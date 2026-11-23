package org.duckdns.hjow.swingwebpanel.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An anonymous FTP client on plain sockets (RFC 959, EPSV from RFC 2428, MLSD from RFC 3659); no JRE internal FTP class is
 * used. One instance serves one control connection and is not thread safe.
 * <ul>
 * <li><b>Login</b>: always <code>USER anonymous</code> with the password {@link #ANONYMOUS_PASSWORD}; user info in a URL is never
 * used. Multi-line replies (<code>220-</code> ... <code>220 </code>) are handled for every command.</li>
 * <li><b>Data connections</b> are passive: <code>EPSV</code> first, <code>PASV</code> when EPSV is refused. The data connection always
 * goes to the address of the control connection (the address in a PASV reply is validated but ignored, which also defeats
 * FTP bounce attacks); ports outside 1-65535 are refused. The completion reply (226/250) of every transfer is always read.</li>
 * <li><b>Listings</b>: <code>MLSD</code> is tried first; if the server does not support it (any 5xx reply) <code>LIST</code> is used.
 * Names are decoded as UTF-8, or as a legacy Korean charset / ISO-8859-1 when the bytes are not valid UTF-8.</li>
 * <li><b>Safety</b>: a path or any command text containing CR, LF or NUL is rejected before anything is sent.</li>
 * <li><b>Time and cancel</b>: connect and read timeouts; {@link CancelToken#cancel()} closes the control and data sockets.</li>
 * </ul>
 * <p>
 * 평문 소켓으로 만든 익명 FTP 클라이언트(RFC 959, RFC 2428 의 EPSV, RFC 3659 의 MLSD). JRE 내부 FTP 클래스는 쓰지 않는다.
 * 인스턴스 하나가 제어 연결 하나를 맡으며 스레드 안전하지 않다.
 * <ul>
 * <li><b>로그인</b>: 항상 <code>USER anonymous</code> 와 비밀번호 {@link #ANONYMOUS_PASSWORD} 를 쓰며 URL 의 사용자 정보는 쓰지 않는다.
 * 모든 명령에서 여러 줄 응답(<code>220-</code> ... <code>220 </code>)을 처리한다.</li>
 * <li><b>데이터 연결</b>은 수동 방식이다. <code>EPSV</code> 를 먼저 쓰고 거부되면 <code>PASV</code> 를 쓴다. 데이터 연결은 항상 제어 연결의
 * 주소로 가며(PASV 응답의 주소는 검증하되 무시하므로 FTP bounce 공격도 막음) 1~65535 밖의 포트는 거부한다. 모든 전송의 완료
 * 응답(226/250)을 반드시 읽는다.</li>
 * <li><b>목록</b>: <code>MLSD</code> 를 먼저 시도하고 서버가 지원하지 않으면(5xx 응답) <code>LIST</code> 를 쓴다. 이름은 UTF-8 로
 * 해독하고, 올바른 UTF-8 이 아니면 옛 한국어 문자셋 또는 ISO-8859-1 로 해독한다.</li>
 * <li><b>안전</b>: CR, LF, NUL 이 들어 있는 경로나 명령 문자열은 아무것도 보내기 전에 거부한다.</li>
 * <li><b>시간과 취소</b>: 연결/읽기 제한 시간이 있으며 {@link CancelToken#cancel()} 이 제어/데이터 소켓을 닫는다.</li>
 * </ul>
 */
public final class FtpClient {

    /**
     * Password sent for the anonymous login.
     * <p>
     * 익명 로그인에 보내는 비밀번호.
     */
    public static final String ANONYMOUS_PASSWORD = "anonymous@";

    /**
     * Maximum length of one reply line.
     * <p>
     * 응답 한 줄의 최대 길이.
     */
    private static final int MAX_LINE = 8192;

    /**
     * Maximum number of lines of one reply.
     * <p>
     * 응답 하나의 최대 줄 수.
     */
    private static final int MAX_REPLY_LINES = 1000;

    /**
     * Maximum size of a directory listing in bytes.
     * <p>
     * 디렉터리 목록의 최대 크기(바이트).
     */
    private static final int MAX_LISTING_BYTES = 16 * 1024 * 1024;

    /**
     * EPSV reply payload such as (|||6446|).
     * <p>
     * (|||6446|) 같은 EPSV 응답 내용.
     */
    private static final Pattern EPSV = Pattern.compile("\\((.)\\1\\1(\\d{1,5})\\1\\)");

    /**
     * PASV reply payload such as (127,0,0,1,25,46).
     * <p>
     * (127,0,0,1,25,46) 같은 PASV 응답 내용.
     */
    private static final Pattern PASV = Pattern.compile("(\\d{1,3}),(\\d{1,3}),(\\d{1,3}),(\\d{1,3}),(\\d{1,3}),(\\d{1,3})");

    /**
     * A parsed server reply.
     * <p>
     * 해석한 서버 응답.
     */
    static final class Reply {
        /**
         * Three digit reply code.
         * <p>
         * 세 자리 응답 코드.
         */
        final int code;
        /**
         * All reply text lines joined with newlines.
         * <p>
         * 개행으로 이은 응답 문자열 전체.
         */
        final String text;

        /**
         * Creates a reply.
         * <p>
         * 응답을 만든다.
         *
         * @param code code / 코드
         * @param text text / 문자열
         */
        Reply(int code, String text) {
            this.code = code;
            this.text = text;
        }
    }

    /**
     * Server host name.
     * <p>
     * 서버 호스트 이름.
     */
    private final String host;
    /**
     * Server port.
     * <p>
     * 서버 포트.
     */
    private final int port;
    /**
     * Connect timeout in milliseconds.
     * <p>
     * 연결 제한 시간(밀리초).
     */
    private final int connectTimeoutMs;
    /**
     * Read timeout in milliseconds.
     * <p>
     * 읽기 제한 시간(밀리초).
     */
    private final int readTimeoutMs;
    /**
     * Cancellation token.
     * <p>
     * 취소 토큰.
     */
    private final CancelToken token;
    /**
     * Messages about things that were ignored or unparsable.
     * <p>
     * 무시했거나 해석하지 못한 것에 대한 메시지.
     */
    private final List<String> diagnostics = new ArrayList<String>();
    /**
     * Control socket.
     * <p>
     * 제어 소켓.
     */
    private Socket control;
    /**
     * Control input.
     * <p>
     * 제어 입력.
     */
    private InputStream in;
    /**
     * Control output.
     * <p>
     * 제어 출력.
     */
    private OutputStream out;
    /**
     * Currently open data socket or null.
     * <p>
     * 지금 열려 있는 데이터 소켓 또는 null.
     */
    private Socket data;
    /**
     * Hook that closes the sockets on cancel.
     * <p>
     * 취소 시 소켓을 닫는 훅.
     */
    private CancelToken.Hook hook;
    /**
     * Charset of names in commands and listings.
     * <p>
     * 명령과 목록에서 쓰는 이름의 문자셋.
     */
    private Charset nameCharset = Charset.forName("UTF-8");

    /**
     * Creates a client (nothing is connected yet).
     * <p>
     * 클라이언트를 만든다(아직 연결하지 않음).
     *
     * @param host             server host / 서버 호스트
     * @param port             server port / 서버 포트
     * @param connectTimeoutMs connect timeout / 연결 제한 시간
     * @param readTimeoutMs    read timeout / 읽기 제한 시간
     * @param token            cancellation token / 취소 토큰
     */
    public FtpClient(String host, int port, int connectTimeoutMs, int readTimeoutMs, CancelToken token) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.token = token == null ? new CancelToken() : token;
    }

    /**
     * Returns the diagnostics collected so far.
     * <p>
     * 지금까지 모은 진단 메시지를 돌려준다.
     *
     * @return messages / 메시지
     */
    public List<String> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Rejects text that could inject commands.
     * <p>
     * 명령을 끼워 넣을 수 있는 문자열을 거부한다.
     *
     * @param text text that becomes part of a command / 명령의 일부가 될 문자열
     * @throws IOException when it contains CR, LF or NUL / CR, LF, NUL 이 있을 때
     */
    static void checkSafe(String text) throws IOException {
        // 줄바꿈이나 NUL 이 하나라도 있으면 명령 주입 위험이 있으므로 거부한다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' || c == '\n' || c == 0) {
                throw new IOException("illegal control character in FTP command text");
            }
        }
    }

    /**
     * Connects, reads the greeting and logs in anonymously.
     * <p>
     * 연결하고 인사말을 읽고 익명으로 로그인한다.
     *
     * @throws IOException on any failure (including {@link FtpException}) / 모든 실패({@link FtpException} 포함)
     */
    public void connect() throws IOException {
        token.throwIfCancelled();
        control = new Socket();
        hook = new CancelToken.Hook() {
            /**
             * Closes control and data sockets.
             * <p>
             * 제어/데이터 소켓을 닫는다.
             */
            public void onCancel() {
                closeSockets();
            }
        };
        token.addHook(hook);
        control.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        control.setSoTimeout(readTimeoutMs);
        in = new BufferedInputStream(control.getInputStream());
        out = control.getOutputStream();
        Reply greeting = readReply();
        if (greeting.code == 120) {
            greeting = readReply();
        }
        if (greeting.code != 220) {
            throw new FtpException(greeting.code, "unexpected greeting: " + greeting.text);
        }
        Reply user = command("USER anonymous");
        if (user.code == 331 || user.code == 332) {
            Reply pass = command("PASS " + ANONYMOUS_PASSWORD);
            if (pass.code != 230 && pass.code != 202) {
                throw new FtpException(pass.code, "anonymous login refused: " + pass.text);
            }
        } else if (user.code != 230) {
            throw new FtpException(user.code, "anonymous login refused: " + user.text);
        }
        command("OPTS UTF8 ON");
        command("TYPE I");
    }

    /**
     * Closes the control and data sockets without sending anything.
     * <p>
     * 아무것도 보내지 않고 제어/데이터 소켓을 닫는다.
     */
    private void closeSockets() {
        closeQuietly(data);
        closeQuietly(control);
    }

    /**
     * Closes a socket ignoring errors.
     * <p>
     * 오류를 무시하고 소켓을 닫는다.
     *
     * @param s socket or null / 소켓 또는 null
     */
    private static void closeQuietly(Socket s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch (IOException e) {
            // 닫기 실패는 무시한다.
        }
    }

    /**
     * Ends the session politely (QUIT) and closes every socket. Safe to call more than once.
     * <p>
     * 세션을 정중히 끝내고(QUIT) 모든 소켓을 닫는다. 여러 번 호출해도 안전하다.
     */
    public void close() {
        if (control != null && !control.isClosed() && !token.isCancelled()) {
            try {
                control.setSoTimeout(1500);
                send("QUIT");
                readReply();
            } catch (IOException e) {
                // 종료 인사 실패는 무시하고 소켓을 닫는다.
            }
        }
        closeSockets();
        if (hook != null) {
            token.removeHook(hook);
            hook = null;
        }
    }

    /**
     * Sends one command line (UTF-8/name charset, CRLF terminated).
     * <p>
     * 명령 한 줄을 보낸다(이름 문자셋, CRLF 종결).
     *
     * @param line command text / 명령 문자열
     * @throws IOException on failure or unsafe text / 실패하거나 안전하지 않은 문자열일 때
     */
    private void send(String line) throws IOException {
        checkSafe(line);
        token.throwIfCancelled();
        out.write((line + "\r\n").getBytes(nameCharset.name()));
        out.flush();
    }

    /**
     * Sends a command and reads its reply.
     * <p>
     * 명령을 보내고 응답을 읽는다.
     *
     * @param line command text / 명령 문자열
     * @return the reply / 응답
     * @throws IOException on failure / 실패 시
     */
    private Reply command(String line) throws IOException {
        send(line);
        return readReply();
    }

    /**
     * Reads one raw line (without CRLF) from the control connection.
     * <p>
     * 제어 연결에서 CRLF 를 뺀 원시 줄 하나를 읽는다.
     *
     * @return line text / 줄 문자열
     * @throws IOException on end of stream, timeout or an over-long line / 스트림 끝, 시간 초과, 너무 긴 줄
     */
    private String readLine() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c;
        // 개행을 만날 때까지 한 바이트씩 읽는다.
        while ((c = in.read()) >= 0) {
            if (c == '\n') {
                break;
            }
            if (c != '\r') {
                bos.write(c);
            }
            if (bos.size() > MAX_LINE) {
                throw new IOException("FTP reply line too long");
            }
        }
        if (c < 0 && bos.size() == 0) {
            token.throwIfCancelled();
            throw new IOException("FTP control connection closed");
        }
        return new String(bos.toByteArray(), nameCharset.name());
    }

    /**
     * Reads a (possibly multi-line) reply.
     * <p>
     * (여러 줄일 수 있는) 응답을 읽는다.
     *
     * @return the reply / 응답
     * @throws IOException on failure / 실패 시
     */
    private Reply readReply() throws IOException {
        String first = readLine();
        if (first.length() < 3 || !Character.isDigit(first.charAt(0)) || !Character.isDigit(first.charAt(1))
                || !Character.isDigit(first.charAt(2))) {
            throw new FtpException(0, "malformed FTP reply: " + first);
        }
        int code = Integer.parseInt(first.substring(0, 3));
        StringBuilder text = new StringBuilder(first.length() > 4 ? first.substring(4) : "");
        if (first.length() > 3 && first.charAt(3) == '-') {
            String prefix = first.substring(0, 3) + " ";
            int lines = 1;
            // 같은 코드와 공백으로 시작하는 줄이 나올 때까지 여러 줄 응답을 모두 읽는다.
            while (true) {
                String l = readLine();
                text.append('\n').append(l.startsWith(prefix) ? l.substring(4) : l);
                if (l.startsWith(prefix)) {
                    break;
                }
                if (++lines > MAX_REPLY_LINES) {
                    throw new IOException("FTP reply has too many lines");
                }
            }
        }
        return new Reply(code, text.toString());
    }

    /**
     * Changes the working directory.
     * <p>
     * 작업 디렉터리를 바꾼다.
     *
     * @param path decoded absolute path / 디코딩된 절대 경로
     * @return true if the server accepted it (the path is a directory) / 서버가 받아들이면(디렉터리이면) true
     * @throws IOException on connection problems / 연결 문제 시
     */
    public boolean changeDirectory(String path) throws IOException {
        Reply r = command("CWD " + path);
        return r.code / 100 == 2;
    }

    /**
     * Returns the size of a file with SIZE, or -1 when unavailable.
     * <p>
     * SIZE 로 파일 크기를 돌려준다. 알 수 없으면 -1.
     *
     * @param path decoded path / 디코딩된 경로
     * @return size or -1 / 크기 또는 -1
     * @throws IOException on connection problems / 연결 문제 시
     */
    public long size(String path) throws IOException {
        Reply r = command("SIZE " + path);
        if (r.code == 213) {
            try {
                return Long.parseLong(r.text.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Opens a passive data connection (EPSV, then PASV).
     * <p>
     * 수동 데이터 연결을 연다(EPSV, 그다음 PASV).
     *
     * @return the connected data socket / 연결된 데이터 소켓
     * @throws IOException on failure / 실패 시
     */
    private Socket openData() throws IOException {
        int dataPort = -1;
        Reply r = command("EPSV");
        if (r.code == 229) {
            Matcher m = EPSV.matcher(r.text);
            if (m.find()) {
                dataPort = Integer.parseInt(m.group(2));
            }
        }
        if (dataPort < 1 || dataPort > 65535) {
            r = command("PASV");
            if (r.code != 227) {
                throw new FtpException(r.code, "passive mode refused: " + r.text);
            }
            Matcher m = PASV.matcher(r.text);
            if (!m.find()) {
                throw new FtpException(r.code, "cannot parse PASV reply: " + r.text);
            }
            int[] n = new int[6];
            // 여섯 숫자가 모두 0~255 인지 검증한다.
            for (int i = 0; i < 6; i++) {
                n[i] = Integer.parseInt(m.group(i + 1));
                if (n[i] > 255) {
                    throw new FtpException(r.code, "invalid number in PASV reply: " + r.text);
                }
            }
            String advertised = n[0] + "." + n[1] + "." + n[2] + "." + n[3];
            dataPort = n[4] * 256 + n[5];
            if (!advertised.equals(control.getInetAddress().getHostAddress())) {
                diagnostics.add("ignored the address " + advertised + " of the PASV reply; using the control connection address");
            }
        }
        if (dataPort < 1 || dataPort > 65535) {
            throw new FtpException(0, "invalid data port " + dataPort);
        }
        Socket s = new Socket();
        data = s;
        if (token.isCancelled()) {
            closeQuietly(s);
            token.throwIfCancelled();
        }
        s.connect(new InetSocketAddress(control.getInetAddress(), dataPort), connectTimeoutMs);
        s.setSoTimeout(readTimeoutMs);
        return s;
    }

    /**
     * Runs a transfer command whose data goes into a stream: opens the data connection, sends the command, copies the data
     * and reads the completion reply.
     * <p>
     * 데이터가 스트림으로 가는 전송 명령을 실행한다. 데이터 연결을 열고 명령을 보내고 데이터를 복사하고 완료 응답을 읽는다.
     *
     * @param command   command such as LIST or RETR path / LIST, RETR 경로 같은 명령
     * @param sink      destination of the data / 데이터의 목적지
     * @param limit     maximum bytes or -1 / 최대 바이트 또는 -1
     * @param progress  progress listener or null / 진행 리스너 또는 null
     * @param total     expected total or -1 / 예상 전체 크기 또는 -1
     * @return number of bytes transferred / 전송한 바이트 수
     * @throws IOException on failure / 실패 시
     */
    private long transfer(String command, OutputStream sink, long limit, ProgressListener progress, long total) throws IOException {
        Socket d = openData();
        long count = 0;
        try {
            Reply start = command(command);
            if (start.code != 125 && start.code != 150) {
                throw new FtpException(start.code, command.split(" ")[0] + " refused: " + start.text);
            }
            InputStream din = d.getInputStream();
            byte[] buf = new byte[8192];
            int n;
            // 데이터 연결이 닫힐 때까지 읽어 목적지로 옮긴다.
            while ((n = din.read(buf)) >= 0) {
                token.throwIfCancelled();
                count += n;
                if (limit >= 0 && count > limit) {
                    throw new IOException("FTP data exceeds " + limit + " bytes");
                }
                sink.write(buf, 0, n);
                if (progress != null) {
                    progress.onProgress(count, total);
                }
            }
        } finally {
            closeQuietly(d);
            data = null;
        }
        Reply done = readReply();
        if (done.code / 100 != 2) {
            throw new FtpException(done.code, "transfer failed: " + done.text);
        }
        return count;
    }

    /**
     * Lists the current working directory: MLSD first, LIST when MLSD is not supported.
     * <p>
     * 현재 작업 디렉터리의 목록을 돌려준다. MLSD 를 먼저 쓰고 지원하지 않으면 LIST 를 쓴다.
     *
     * @return entries (names decoded) / 항목(이름 해독됨)
     * @throws IOException on failure / 실패 시
     */
    public List<FtpEntry> list() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean mlsd = true;
        try {
            transfer("MLSD", bos, MAX_LISTING_BYTES, null, -1);
        } catch (FtpException e) {
            // MLSD 가 지원되지 않으면(5xx) LIST 로 바꾸어 다시 시도한다.
            if (e.getCode() / 100 != 5) {
                throw e;
            }
            diagnostics.add("MLSD not available (" + e.getCode() + "), using LIST");
            mlsd = false;
            bos.reset();
            transfer("LIST", bos, MAX_LISTING_BYTES, null, -1);
        }
        String text = decodeNames(bos.toByteArray());
        List<FtpEntry> entries = new ArrayList<FtpEntry>();
        FtpListParser.parse(text, mlsd, entries, diagnostics);
        return entries;
    }

    /**
     * Downloads a file with RETR.
     * <p>
     * RETR 로 파일을 내려받는다.
     *
     * @param path     decoded path / 디코딩된 경로
     * @param sink     destination / 목적지
     * @param limit    maximum bytes or -1 / 최대 바이트 또는 -1
     * @param progress progress listener or null / 진행 리스너 또는 null
     * @param total    expected size or -1 / 예상 크기 또는 -1
     * @return bytes transferred / 전송한 바이트 수
     * @throws IOException on failure / 실패 시
     */
    public long retrieve(String path, OutputStream sink, long limit, ProgressListener progress, long total) throws IOException {
        return transfer("RETR " + path, sink, limit, progress, total);
    }

    /**
     * Decodes listing bytes: UTF-8 when valid, otherwise a legacy Korean charset or ISO-8859-1. The charset that worked
     * is used for later commands.
     * <p>
     * 목록 바이트를 해독한다. 올바른 UTF-8 이면 UTF-8, 아니면 옛 한국어 문자셋 또는 ISO-8859-1 을 쓴다. 성공한 문자셋은 이후
     * 명령에도 쓴다.
     *
     * @param bytes listing bytes / 목록 바이트
     * @return text / 문자열
     */
    private String decodeNames(byte[] bytes) {
        try {
            return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            String[] candidates = { "x-windows-949", "EUC-KR", "ISO-8859-1" };
            // 사용할 수 있는 첫 번째 대체 문자셋으로 해독한다.
            for (int i = 0; i < candidates.length; i++) {
                if (Charset.isSupported(candidates[i])) {
                    nameCharset = Charset.forName(candidates[i]);
                    diagnostics.add("listing is not valid UTF-8, decoded as " + candidates[i]);
                    return new String(bytes, nameCharset);
                }
            }
            return new String(bytes, Charset.forName("ISO-8859-1"));
        }
    }
}
