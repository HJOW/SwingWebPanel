package uicheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * A small HTTP/1.0 server for the real-window checks of {@link UiCheck}. It serves the check pages from a directory, answers
 * <code>/echo</code> with a page that shows the request (method, target, content type and body) and prints the same request to standard output,
 * and answers <code>/download</code> with an attachment whose name, size and behaviour come from the query (a Korean file name, an unknown size,
 * a slow transfer or a broken one). It only listens on the loop-back address and stops with the tool.
 * <p>
 * {@link UiCheck} 의 실제 창 확인을 위한 작은 HTTP/1.0 서버이다. 디렉터리에서 확인용 페이지를 내주고, <code>/echo</code> 에는 요청(메서드, 대상, 콘텐츠 형식,
 * 본문)을 보여 주는 페이지로 답하며 같은 요청을 표준 출력에 찍는다. <code>/download</code> 에는 질의에 따라 이름·크기·동작(한글 파일 이름, 크기 불명,
 * 느린 전송, 끊기는 전송)이 정해지는 첨부 파일로 답한다. 루프백 주소에서만 듣고 도구와 함께 끝난다.
 * <p>
 * Usage / 사용법: <code>EchoServer s = new EchoServer(new File("tools/uicheck/pages")); s.start(); String base = s.base();</code>
 */
final class EchoServer implements Runnable {

    /** Longest request head in bytes. / 요청 머리의 최대 바이트 수. */
    private static final int MAX_HEAD = 64 * 1024;

    /** Longest request body in bytes. / 요청 본문의 최대 바이트 수. */
    private static final int MAX_BODY = 4 * 1024 * 1024;

    /** The directory of the pages / 페이지 디렉터리. */
    private final File root;

    /** The listening socket / 듣는 소켓. */
    private ServerSocket server;

    /**
     * Creates a server that serves files from a directory. Call {@link #start()} to listen.
     * <p>
     * 디렉터리의 파일을 내주는 서버를 만든다. 들으려면 {@link #start()} 를 부른다.
     *
     * @param root the directory of the pages / 페이지 디렉터리
     */
    EchoServer(File root) {
        this.root = root;
    }

    /**
     * Opens the socket on a free loop-back port and starts the daemon thread that accepts connections.
     * <p>
     * 비어 있는 루프백 포트에 소켓을 열고 연결을 받는 데몬 스레드를 시작한다.
     *
     * @throws IOException if the socket cannot be opened / 소켓을 열 수 없는 경우
     */
    void start() throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        Thread t = new Thread(this, "uicheck-server");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the base address of the server, ending with a slash.
     * <p>
     * 서버의 기준 주소(빗금으로 끝남)를 반환한다.
     *
     * @return the base address / 기준 주소
     */
    String base() {
        return "http://127.0.0.1:" + server.getLocalPort() + "/";
    }

    /**
     * Closes the listening socket.
     * <p>
     * 듣는 소켓을 닫는다.
     */
    void stop() {
        try {
            server.close();
        } catch (IOException e) {
            // 닫기 실패는 도구 종료에 영향이 없으므로 무시한다.
        }
    }

    /**
     * Accepts connections until the socket is closed and answers each on its own daemon thread.
     * <p>
     * 소켓이 닫힐 때까지 연결을 받고 각각을 자기 데몬 스레드에서 처리한다.
     */
    public void run() {
        // 소켓이 닫힐 때까지 연결을 받는다.
        while (!server.isClosed()) {
            final Socket s;
            try {
                s = server.accept();
            } catch (IOException e) {
                return;
            }
            Thread t = new Thread(new Runnable() {
                /** Answers one connection. / 연결 하나에 답한다. */
                public void run() {
                    try {
                        answer(s);
                    } catch (Exception e) {
                        System.out.println("[serve] error " + e);
                    } finally {
                        try {
                            s.close();
                        } catch (IOException e) {
                            // 이미 닫혔으면 할 일이 없다.
                        }
                    }
                }
            }, "uicheck-conn");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Reads one request and writes the answer.
     * <p>
     * 요청 하나를 읽고 답을 쓴다.
     *
     * @param s the connection / 연결
     * @throws IOException if the connection fails / 연결이 실패한 경우
     * @throws InterruptedException if a slow transfer is interrupted / 느린 전송이 중단된 경우
     */
    private void answer(Socket s) throws IOException, InterruptedException {
        InputStream in = s.getInputStream();
        String head = readHead(in);
        if (head == null) {
            return;
        }
        String[] lines = head.split("\r\n");
        String[] first = lines[0].split(" ");
        if (first.length < 2) {
            return;
        }
        String method = first[0];
        String target = first[1];
        Map<String, String> headers = new HashMap<String, String>();
        // 머리 줄을 이름(소문자)과 값으로 나눈다.
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                headers.put(lines[i].substring(0, colon).trim().toLowerCase(), lines[i].substring(colon + 1).trim());
            }
        }
        byte[] body = readBody(in, headers.get("content-length"));
        String path = target;
        String query = "";
        int q = target.indexOf('?');
        if (q >= 0) {
            path = target.substring(0, q);
            query = target.substring(q + 1);
        }
        System.out.println("[serve] " + method + " " + target);
        OutputStream out = s.getOutputStream();
        // 경로에 따라 에코, 다운로드, 파일 응답으로 나눈다.
        if (path.startsWith("/echo")) {
            echo(out, method, target, headers, body);
        } else if (path.equals("/download")) {
            download(out, parseQuery(query));
        } else if (path.equals("/tone.wav")) {
            tone(out, parseQuery(query));
        } else {
            file(out, path);
        }
        out.flush();
    }

    /**
     * Reads the request head up to the empty line.
     * <p>
     * 빈 줄까지 요청 머리를 읽는다.
     *
     * @param in the stream / 스트림
     * @return the head without the empty line, or null at the end of the stream / 빈 줄을 뺀 머리. 스트림이 끝나면 null
     * @throws IOException if reading fails / 읽기가 실패한 경우
     */
    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int state = 0;
        // 바이트를 하나씩 읽으며 CR LF CR LF 를 찾는다.
        while (buf.size() < MAX_HEAD) {
            int b = in.read();
            if (b < 0) {
                return buf.size() == 0 ? null : buf.toString("ISO-8859-1");
            }
            buf.write(b);
            if ((state == 0 || state == 2) && b == '\r') {
                state++;
            } else if ((state == 1 || state == 3) && b == '\n') {
                state++;
                if (state == 4) {
                    String all = buf.toString("ISO-8859-1");
                    return all.substring(0, all.length() - 4);
                }
            } else {
                state = b == '\r' ? 1 : 0;
            }
        }
        throw new IOException("request head too long");
    }

    /**
     * Reads the request body of the length that the <code>Content-Length</code> header gives.
     * <p>
     * <code>Content-Length</code> 머리가 정한 길이만큼 요청 본문을 읽는다.
     *
     * @param in     the stream / 스트림
     * @param length the header value or null / 머리 값 또는 null
     * @return the body (empty without the header) / 본문(머리가 없으면 비어 있음)
     * @throws IOException if reading fails / 읽기가 실패한 경우
     */
    private static byte[] readBody(InputStream in, String length) throws IOException {
        if (length == null) {
            return new byte[0];
        }
        int n = Math.min(Integer.parseInt(length.trim()), MAX_BODY);
        byte[] body = new byte[n];
        int off = 0;
        // 정한 길이를 다 읽을 때까지 반복한다.
        while (off < n) {
            int r = in.read(body, off, n - off);
            if (r < 0) {
                break;
            }
            off += r;
        }
        return body;
    }

    /**
     * Answers <code>/echo</code>: prints the request to standard output and returns a page that shows it.
     * <p>
     * <code>/echo</code> 에 답한다: 요청을 표준 출력에 찍고 그것을 보여 주는 페이지를 돌려준다.
     *
     * @param out     the output / 출력
     * @param method  the method / 메서드
     * @param target  the request target / 요청 대상
     * @param headers the headers / 머리
     * @param body    the body / 본문
     * @throws IOException if writing fails / 쓰기가 실패한 경우
     */
    private static void echo(OutputStream out, String method, String target, Map<String, String> headers, byte[] body) throws IOException {
        String type = headers.get("content-type");
        String text = new String(body, "UTF-8");
        String decodedTarget;
        try {
            decodedTarget = URLDecoder.decode(target, "UTF-8");
        } catch (IllegalArgumentException e) {
            decodedTarget = target;
        }
        System.out.println("[echo] " + method + " " + target);
        System.out.println("[echo] decoded target: " + decodedTarget);
        System.out.println("[echo] content-type: " + type);
        System.out.println("[echo] body (" + body.length + " bytes, UTF-8):");
        String[] bodyLines = text.split("\n", -1);
        // 본문을 줄마다 앞에 표시를 붙여 찍는다.
        for (int i = 0; i < bodyLines.length; i++) {
            System.out.println("[echo] | " + bodyLines[i].replace("\r", "\\r"));
        }
        StringBuilder page = new StringBuilder();
        page.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>echo</title></head><body>");
        page.append("<h1>echo</h1><pre id=\"echo\">").append(escape(method + " " + decodedTarget + "\nContent-Type: " + type + "\n\n" + text)).append("</pre>");
        page.append("<p><a href=\"/form.html\">form</a></p></body></html>");
        send(out, "200 OK", "text/html; charset=utf-8", null, page.toString().getBytes("UTF-8"));
    }

    /**
     * Answers <code>/download</code> with an attachment. Query parameters: <code>name</code> (the file name, may be Korean), <code>size</code> (bytes,
     * default 20000), <code>unknown=1</code> (no <code>Content-Length</code>), <code>slow=1</code> (about 4 seconds), <code>fail=1</code> (the connection
     * is closed after half of the declared length).
     * <p>
     * <code>/download</code> 에 첨부 파일로 답한다. 질의 인수: <code>name</code>(파일 이름, 한글 가능), <code>size</code>(바이트, 기본 20000),
     * <code>unknown=1</code>(<code>Content-Length</code> 없음), <code>slow=1</code>(약 4초), <code>fail=1</code>(선언한 길이의 절반 뒤에 연결을 끊음).
     *
     * @param out   the output / 출력
     * @param query the query parameters / 질의 인수
     * @throws IOException if writing fails / 쓰기가 실패한 경우
     * @throws InterruptedException if a slow transfer is interrupted / 느린 전송이 중단된 경우
     */
    private static void download(OutputStream out, Map<String, String> query) throws IOException, InterruptedException {
        String name = query.containsKey("name") ? query.get("name") : "file.bin";
        int size = query.containsKey("size") ? Integer.parseInt(query.get("size")) : 20000;
        boolean unknown = "1".equals(query.get("unknown"));
        boolean slow = "1".equals(query.get("slow"));
        boolean fail = "1".equals(query.get("fail"));
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.0 200 OK\r\nContent-Type: application/octet-stream\r\nConnection: close\r\n");
        head.append("Content-Disposition: attachment; filename=\"download.bin\"; filename*=UTF-8''").append(percentEncode(name)).append("\r\n");
        if (!unknown) {
            head.append("Content-Length: ").append(size).append("\r\n");
        }
        head.append("\r\n");
        out.write(head.toString().getBytes("ISO-8859-1"));
        int send = fail ? size / 2 : size;
        int chunk = 4096;
        int pause = slow ? Math.max(1, 4000 / Math.max(1, send / chunk)) : 0;
        byte[] data = new byte[chunk];
        // 내용은 'a'~'z' 를 되풀이한다(받은 파일을 확인하기 쉽도록).
        for (int i = 0; i < chunk; i++) {
            data[i] = (byte) ('a' + i % 26);
        }
        int sent = 0;
        // 보낼 길이를 덩어리 단위로 보낸다. 느린 전송이면 덩어리 사이에 쉰다.
        while (sent < send) {
            int n = Math.min(chunk, send - sent);
            out.write(data, 0, n);
            out.flush();
            sent += n;
            if (pause > 0) {
                Thread.sleep(pause);
            }
        }
        System.out.println("[serve] download name=" + name + " sent=" + sent + (fail ? " (broken)" : "") + (unknown ? " (no length)" : ""));
    }

    /**
     * Answers <code>/tone.wav</code> with a sine tone as 16-bit mono PCM WAV at 22050 Hz. Query parameters: <code>sec</code> (length in seconds,
     * default 10), <code>hz</code> (frequency, default 440) and <code>amp</code> (amplitude from 0 to 1, default 0.5). It is the sound source of the
     * real audio check (the peak meter of the system must show the amplitude while the page plays it).
     * <p>
     * <code>/tone.wav</code> 에 22050 Hz 16비트 모노 PCM WAV 사인파로 답한다. 질의 인수: <code>sec</code>(초, 기본 10), <code>hz</code>(주파수, 기본 440),
     * <code>amp</code>(0~1 진폭, 기본 0.5). 실제 오디오 확인의 소리 원천이다(페이지가 재생하는 동안 시스템의 피크 미터가 그 진폭을 보여야 한다).
     *
     * @param out   the output / 출력
     * @param query the query parameters / 질의 인수
     * @throws IOException if writing fails / 쓰기가 실패한 경우
     */
    private static void tone(OutputStream out, Map<String, String> query) throws IOException {
        double sec = query.containsKey("sec") ? Double.parseDouble(query.get("sec")) : 10;
        double hz = query.containsKey("hz") ? Double.parseDouble(query.get("hz")) : 440;
        double amp = query.containsKey("amp") ? Double.parseDouble(query.get("amp")) : 0.5;
        int rate = 22050;
        int frames = (int) Math.round(sec * rate);
        byte[] body = new byte[44 + frames * 2];
        writeAscii(body, 0, "RIFF");
        le32(body, 4, 36 + frames * 2);
        writeAscii(body, 8, "WAVEfmt ");
        le32(body, 16, 16);
        le16(body, 20, 1);
        le16(body, 22, 1);
        le32(body, 24, rate);
        le32(body, 28, rate * 2);
        le16(body, 32, 2);
        le16(body, 34, 16);
        writeAscii(body, 36, "data");
        le32(body, 40, frames * 2);
        // 표본마다 사인값을 16비트 정수로 쓴다.
        for (int i = 0; i < frames; i++) {
            int v = (int) Math.round(Math.sin(2 * Math.PI * hz * i / rate) * amp * 32767);
            le16(body, 44 + i * 2, v);
        }
        send(out, "200 OK", "audio/wav", null, body);
    }

    /**
     * Writes ASCII characters into an array.
     * <p>
     * 배열에 ASCII 문자를 쓴다.
     *
     * @param b    the array / 배열
     * @param at   the position / 위치
     * @param text the characters / 문자
     */
    private static void writeAscii(byte[] b, int at, String text) {
        // 문자마다 한 바이트로 쓴다.
        for (int i = 0; i < text.length(); i++) {
            b[at + i] = (byte) text.charAt(i);
        }
    }

    /**
     * Writes a little-endian 16-bit value.
     * <p>
     * 리틀 엔디언 16비트 값을 쓴다.
     *
     * @param b  the array / 배열
     * @param at the position / 위치
     * @param v  the value / 값
     */
    private static void le16(byte[] b, int at, int v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
    }

    /**
     * Writes a little-endian 32-bit value.
     * <p>
     * 리틀 엔디언 32비트 값을 쓴다.
     *
     * @param b  the array / 배열
     * @param at the position / 위치
     * @param v  the value / 값
     */
    private static void le32(byte[] b, int at, int v) {
        le16(b, at, v);
        le16(b, at + 2, v >> 16);
    }

    /**
     * Answers a page file from the root directory (only plain names, no parent references).
     * <p>
     * 루트 디렉터리의 페이지 파일로 답한다(상위 참조 없는 이름만).
     *
     * @param out  the output / 출력
     * @param path the request path / 요청 경로
     * @throws IOException if writing fails / 쓰기가 실패한 경우
     */
    private void file(OutputStream out, String path) throws IOException {
        String name = path.length() > 1 ? path.substring(1) : "index.html";
        File f = new File(root, name);
        if (name.indexOf("..") >= 0 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || !f.isFile()) {
            send(out, "404 Not Found", "text/html; charset=utf-8", null, "<!DOCTYPE html><title>404</title><p>not found".getBytes("UTF-8"));
            return;
        }
        String type = name.endsWith(".html") ? "text/html; charset=utf-8" : name.endsWith(".css") ? "text/css" : name.endsWith(".js")
                ? "text/javascript" : "application/octet-stream";
        send(out, "200 OK", type, null, readAll(f));
    }

    /**
     * Writes a whole response.
     * <p>
     * 응답 전체를 쓴다.
     *
     * @param out    the output / 출력
     * @param status the status line after the version / 버전 뒤의 상태 줄
     * @param type   the content type / 콘텐츠 형식
     * @param extra  extra header lines ending with CR LF, or null / CR LF 로 끝나는 추가 머리 줄 또는 null
     * @param body   the body / 본문
     * @throws IOException if writing fails / 쓰기가 실패한 경우
     */
    private static void send(OutputStream out, String status, String type, String extra, byte[] body) throws IOException {
        String head = "HTTP/1.0 " + status + "\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length + "\r\nCache-Control: no-store\r\nConnection: close\r\n"
                + (extra == null ? "" : extra) + "\r\n";
        out.write(head.getBytes("ISO-8859-1"));
        out.write(body);
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws IOException if reading fails / 읽기가 실패한 경우
     */
    private static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(b)) > 0) {
                buf.write(b, 0, n);
            }
            return buf.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Splits a query string into decoded parameters.
     * <p>
     * 질의 문자열을 디코딩한 인수로 나눈다.
     *
     * @param query the query / 질의
     * @return the parameters / 인수
     * @throws UnsupportedEncodingException never (UTF-8 always exists) / 일어나지 않음(UTF-8 은 항상 있음)
     */
    private static Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<String, String>();
        String[] parts = query.split("&");
        // 이름=값 쌍마다 디코딩해서 넣는다.
        for (int i = 0; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) {
                map.put(URLDecoder.decode(parts[i].substring(0, eq), "UTF-8"), URLDecoder.decode(parts[i].substring(eq + 1), "UTF-8"));
            }
        }
        return map;
    }

    /**
     * Percent-encodes a text as UTF-8 for <code>filename*</code> (RFC 5987 attribute characters stay as they are).
     * <p>
     * <code>filename*</code> 용으로 글을 UTF-8 퍼센트 인코딩한다(RFC 5987 의 속성 문자는 그대로 둔다).
     *
     * @param text the text / 글
     * @return the encoded text / 인코딩한 글
     * @throws UnsupportedEncodingException never / 일어나지 않음
     */
    private static String percentEncode(String text) throws UnsupportedEncodingException {
        byte[] bytes = text.getBytes("UTF-8");
        StringBuilder sb = new StringBuilder();
        String hex = "0123456789ABCDEF";
        // 바이트마다 속성 문자는 그대로, 나머지는 %XX 로 쓴다.
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || "!#$&+-.^_`|~".indexOf(b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%').append(hex.charAt(b >> 4)).append(hex.charAt(b & 15));
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a text for HTML.
     * <p>
     * 글을 HTML 용으로 이스케이프한다.
     *
     * @param text the text / 글
     * @return the escaped text / 이스케이프한 글
     */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
