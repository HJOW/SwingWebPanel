package org.duckdns.hjow.swingwebpanel.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A small local FTP server for tests (plan item P05-07), written on plain sockets. It serves an in-memory file tree to
 * anonymous clients on 127.0.0.1 with an ephemeral control port and passive data connections (PASV and EPSV).
 * Supported commands: USER, PASS, SYST, FEAT, OPTS, TYPE, PWD, CWD, CDUP, PASV, EPSV, LIST (UNIX or DOS style), MLSD
 * (optional), RETR, SIZE, NOOP, QUIT. File names are UTF-8 (Hangul and spaces are fine). Failure injection switches:
 * unsupported MLSD/EPSV, multi-line greeting, failing login/CWD/LIST/RETR, delayed or truncated data, a PASV reply that
 * advertises a wrong address, raw (unparsable) listing lines. Every command line received is logged.
 * Call {@link #stop()} to close sockets and join threads.
 * <p>
 * 평문 소켓으로 만든 시험용 소형 로컬 FTP 서버(계획 P05-07). 127.0.0.1 의 임시 제어 포트에서 메모리 파일 트리를 익명 클라이언트에게
 * 제공하며 수동(PASV, EPSV) 데이터 연결을 쓴다. 지원 명령: USER, PASS, SYST, FEAT, OPTS, TYPE, PWD, CWD, CDUP, PASV, EPSV,
 * LIST(UNIX 또는 DOS 형식), MLSD(선택), RETR, SIZE, NOOP, QUIT. 파일 이름은 UTF-8 이다(한글과 공백 가능). 실패 흉내 스위치:
 * MLSD/EPSV 미지원, 여러 줄 인사말, 로그인/CWD/LIST/RETR 실패, 지연되거나 잘린 데이터, 잘못된 주소를 알리는 PASV 응답,
 * 해석할 수 없는 원문 목록 줄. 받은 명령 줄은 모두 기록한다. 소켓과 스레드를 정리하려면 {@link #stop()} 을 호출한다.
 */
public class TestFtpServer {

    /**
     * Directory listing style of the LIST command.
     * <p>
     * LIST 명령의 디렉터리 목록 형식.
     */
    public enum ListStyle {
        /**
         * UNIX <code>ls -l</code> lines.
         * <p>
         * UNIX <code>ls -l</code> 형식의 줄.
         */
        UNIX,
        /**
         * MS-DOS / IIS style lines.
         * <p>
         * MS-DOS / IIS 형식의 줄.
         */
        DOS
    }

    /**
     * Directories (absolute paths without trailing slash, "/" implied).
     * <p>
     * 디렉터리(끝 슬래시 없는 절대 경로, "/" 는 항상 있음).
     */
    private final Set<String> dirs = new TreeSet<String>();
    /**
     * Files by absolute path.
     * <p>
     * 절대 경로별 파일.
     */
    private final Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
    /**
     * Symbolic links: path to target text.
     * <p>
     * 심볼릭 링크: 경로에서 대상 문자열로.
     */
    private final Map<String, String> links = new LinkedHashMap<String, String>();
    /**
     * Raw lines added to the LIST output of a directory.
     * <p>
     * 디렉터리의 LIST 출력에 덧붙이는 원문 줄.
     */
    private final Map<String, List<String>> rawLines = new LinkedHashMap<String, List<String>>();
    /**
     * Paths for which CWD fails.
     * <p>
     * CWD 가 실패하는 경로.
     */
    private final Set<String> failCwd = new HashSet<String>();
    /**
     * Log of received command lines.
     * <p>
     * 받은 명령 줄 기록.
     */
    private final List<String> commands = new ArrayList<String>();
    /**
     * Open sockets closed by stop().
     * <p>
     * stop() 이 닫는 열린 소켓.
     */
    private final List<Socket> sockets = new ArrayList<Socket>();
    /**
     * Passive listeners closed by stop().
     * <p>
     * stop() 이 닫는 수동 대기 소켓.
     */
    private final List<ServerSocket> listeners = new ArrayList<ServerSocket>();
    /**
     * Started threads.
     * <p>
     * 시작한 스레드.
     */
    private final List<Thread> threads = new ArrayList<Thread>();

    /**
     * Control listener.
     * <p>
     * 제어 대기 소켓.
     */
    private ServerSocket control;
    /**
     * Whether the server runs.
     * <p>
     * 서버가 동작 중인지 여부.
     */
    private volatile boolean running;
    /**
     * LIST style.
     * <p>
     * LIST 형식.
     */
    private volatile ListStyle listStyle = ListStyle.UNIX;
    /**
     * Whether MLSD works.
     * <p>
     * MLSD 가 동작하는지 여부.
     */
    private volatile boolean mlsdEnabled = true;
    /**
     * Whether EPSV works.
     * <p>
     * EPSV 가 동작하는지 여부.
     */
    private volatile boolean epsvEnabled = true;
    /**
     * Whether the greeting has several lines.
     * <p>
     * 인사말이 여러 줄인지 여부.
     */
    private volatile boolean multilineGreeting;
    /**
     * Whether PASS is rejected.
     * <p>
     * PASS 를 거부하는지 여부.
     */
    private volatile boolean failLogin;
    /**
     * Whether LIST and MLSD answer 550.
     * <p>
     * LIST 와 MLSD 가 550 을 답하는지 여부.
     */
    private volatile boolean failList;
    /**
     * Delay before data is sent, in milliseconds.
     * <p>
     * 데이터를 보내기 전의 지연(밀리초).
     */
    private volatile long dataDelayMs;
    /**
     * If not negative, RETR sends only this many bytes and then closes the data connection (but still answers 226).
     * <p>
     * 음수가 아니면 RETR 이 이만큼의 바이트만 보내고 데이터 연결을 닫는다(그래도 226 을 답함).
     */
    private volatile int truncateRetrAt = -1;
    /**
     * Address that PASV advertises instead of 127.0.0.1 (null = real address).
     * <p>
     * PASV 가 127.0.0.1 대신 알리는 주소(null 이면 실제 주소).
     */
    private volatile String advertisedPasvAddress;
    /**
     * Number of data connections accepted.
     * <p>
     * 수락한 데이터 연결 수.
     */
    private volatile int dataConnections;
    /**
     * Number of control connections accepted.
     * <p>
     * 수락한 제어 연결 수.
     */
    private volatile int controlConnections;

    /**
     * Creates a server with an empty tree.
     * <p>
     * 빈 트리를 가진 서버를 만든다.
     */
    public TestFtpServer() {
    }

    /**
     * Adds a directory and its parents.
     * <p>
     * 디렉터리와 그 상위 디렉터리를 추가한다.
     *
     * @param path absolute path / 절대 경로
     * @return this / 자기 자신
     */
    public synchronized TestFtpServer addDir(String path) {
        String p = normalize("/", path);
        // 루트부터 한 단계씩 만들어 올라간다.
        while (!p.equals("/")) {
            dirs.add(p);
            p = parentOf(p);
        }
        return this;
    }

    /**
     * Adds a file (and its parent directories).
     * <p>
     * 파일(과 상위 디렉터리)을 추가한다.
     *
     * @param path absolute path / 절대 경로
     * @param data content / 내용
     * @return this / 자기 자신
     */
    public synchronized TestFtpServer addFile(String path, byte[] data) {
        String p = normalize("/", path);
        addDir(parentOf(p));
        files.put(p, data);
        return this;
    }

    /**
     * Adds a symbolic link entry.
     * <p>
     * 심볼릭 링크 항목을 추가한다.
     *
     * @param path   link path / 링크 경로
     * @param target target text / 대상 문자열
     * @return this / 자기 자신
     */
    public synchronized TestFtpServer addSymlink(String path, String target) {
        String p = normalize("/", path);
        addDir(parentOf(p));
        links.put(p, target);
        return this;
    }

    /**
     * Adds a raw line to the LIST output of a directory (for unparsable formats).
     * <p>
     * 디렉터리의 LIST 출력에 원문 줄을 덧붙인다(해석할 수 없는 형식 시험용).
     *
     * @param dir  directory / 디렉터리
     * @param line raw line / 원문 줄
     * @return this / 자기 자신
     */
    public synchronized TestFtpServer addRawListLine(String dir, String line) {
        String p = normalize("/", dir);
        List<String> l = rawLines.get(p);
        if (l == null) {
            l = new ArrayList<String>();
            rawLines.put(p, l);
        }
        l.add(line);
        return this;
    }

    /**
     * Makes CWD fail for a path.
     * <p>
     * 경로에 대한 CWD 가 실패하게 한다.
     *
     * @param path absolute path / 절대 경로
     * @return this / 자기 자신
     */
    public synchronized TestFtpServer failCwdFor(String path) {
        failCwd.add(normalize("/", path));
        return this;
    }

    /**
     * Sets the LIST style.
     * <p>
     * LIST 형식을 정한다.
     *
     * @param style style / 형식
     * @return this / 자기 자신
     */
    public TestFtpServer setListStyle(ListStyle style) {
        this.listStyle = style;
        return this;
    }

    /**
     * Enables or disables MLSD (disabled answers 500 unknown command).
     * <p>
     * MLSD 를 켜거나 끈다(끄면 500 알 수 없는 명령을 답한다).
     *
     * @param on true to support MLSD / MLSD 를 지원하려면 true
     * @return this / 자기 자신
     */
    public TestFtpServer setMlsdEnabled(boolean on) {
        this.mlsdEnabled = on;
        return this;
    }

    /**
     * Enables or disables EPSV (disabled answers 500).
     * <p>
     * EPSV 를 켜거나 끈다(끄면 500 을 답한다).
     *
     * @param on true to support EPSV / EPSV 를 지원하려면 true
     * @return this / 자기 자신
     */
    public TestFtpServer setEpsvEnabled(boolean on) {
        this.epsvEnabled = on;
        return this;
    }

    /**
     * Uses a multi-line greeting.
     * <p>
     * 여러 줄 인사말을 쓴다.
     *
     * @param on true for several lines / 여러 줄이면 true
     * @return this / 자기 자신
     */
    public TestFtpServer setMultilineGreeting(boolean on) {
        this.multilineGreeting = on;
        return this;
    }

    /**
     * Rejects the login.
     * <p>
     * 로그인을 거부한다.
     *
     * @param on true to reject / 거부하려면 true
     * @return this / 자기 자신
     */
    public TestFtpServer setFailLogin(boolean on) {
        this.failLogin = on;
        return this;
    }

    /**
     * Makes LIST and MLSD answer 550.
     * <p>
     * LIST 와 MLSD 가 550 을 답하게 한다.
     *
     * @param on true to fail / 실패시키려면 true
     * @return this / 자기 자신
     */
    public TestFtpServer setFailList(boolean on) {
        this.failList = on;
        return this;
    }

    /**
     * Delays data transfers.
     * <p>
     * 데이터 전송을 지연시킨다.
     *
     * @param ms delay in milliseconds / 지연(밀리초)
     * @return this / 자기 자신
     */
    public TestFtpServer setDataDelayMs(long ms) {
        this.dataDelayMs = ms;
        return this;
    }

    /**
     * Truncates RETR data.
     * <p>
     * RETR 데이터를 자른다.
     *
     * @param bytes bytes to send, or -1 for all / 보낼 바이트 수. 전부 보내려면 -1
     * @return this / 자기 자신
     */
    public TestFtpServer setTruncateRetrAt(int bytes) {
        this.truncateRetrAt = bytes;
        return this;
    }

    /**
     * Makes PASV advertise another IPv4 address.
     * <p>
     * PASV 가 다른 IPv4 주소를 알리게 한다.
     *
     * @param dotted address like 10.0.0.9, or null for the real one / 10.0.0.9 같은 주소. 실제 주소면 null
     * @return this / 자기 자신
     */
    public TestFtpServer setAdvertisedPasvAddress(String dotted) {
        this.advertisedPasvAddress = dotted;
        return this;
    }

    /**
     * Starts the server.
     * <p>
     * 서버를 시작한다.
     *
     * @throws IOException when the socket cannot be opened / 소켓을 열 수 없을 때
     */
    public synchronized void start() throws IOException {
        control = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        running = true;
        Thread t = new Thread(new Runnable() {
            /**
             * Accepts control connections until stopped.
             * <p>
             * 중지될 때까지 제어 연결을 수락한다.
             */
            public void run() {
                acceptLoop();
            }
        }, "TestFtpServer-accept");
        t.setDaemon(true);
        threads.add(t);
        t.start();
    }

    /**
     * Returns the control port.
     * <p>
     * 제어 포트를 돌려준다.
     *
     * @return port / 포트
     */
    public int getPort() {
        return control.getLocalPort();
    }

    /**
     * Builds an FTP URL for a path (already percent-encoded where needed).
     * <p>
     * 경로에 대한 FTP URL 을 만든다(필요하면 이미 퍼센트 인코딩된 경로).
     *
     * @param path path starting with '/' / '/' 로 시작하는 경로
     * @return URL / URL
     */
    public String url(String path) {
        return "ftp://localhost:" + getPort() + path;
    }

    /**
     * Returns a copy of the received command lines.
     * <p>
     * 받은 명령 줄의 복사본을 돌려준다.
     *
     * @return commands in order / 순서대로의 명령
     */
    public synchronized List<String> commands() {
        return new ArrayList<String>(commands);
    }

    /**
     * Returns the number of accepted data connections.
     * <p>
     * 수락한 데이터 연결 수를 돌려준다.
     *
     * @return count / 개수
     */
    public int getDataConnections() {
        return dataConnections;
    }

    /**
     * Returns the number of accepted control connections.
     * <p>
     * 수락한 제어 연결 수를 돌려준다.
     *
     * @return count / 개수
     */
    public int getControlConnections() {
        return controlConnections;
    }

    /**
     * Stops the server: closes all sockets and joins all threads.
     * <p>
     * 서버를 멈춘다. 모든 소켓을 닫고 모든 스레드를 합류시킨다.
     */
    public void stop() {
        List<Socket> s;
        List<ServerSocket> l;
        List<Thread> t;
        synchronized (this) {
            running = false;
            s = new ArrayList<Socket>(sockets);
            l = new ArrayList<ServerSocket>(listeners);
            t = new ArrayList<Thread>(threads);
        }
        try {
            if (control != null) {
                control.close();
            }
        } catch (IOException e) {
            // 이미 닫힌 경우는 무시한다.
        }
        // 열린 수동 대기 소켓과 연결을 모두 닫아 막힌 스레드를 깨운다.
        for (int i = 0; i < l.size(); i++) {
            try {
                l.get(i).close();
            } catch (IOException e) {
                // 무시한다.
            }
        }
        // 닫을 소켓을 차례로 닫는다.
        for (int i = 0; i < s.size(); i++) {
            try {
                s.get(i).close();
            } catch (IOException e) {
                // 무시한다.
            }
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int i = 0; i < t.size(); i++) {
            try {
                t.get(i).join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Accept loop for control connections.
     * <p>
     * 제어 연결 수락 반복.
     */
    private void acceptLoop() {
        // 서버가 동작하는 동안 연결마다 세션 스레드를 시작한다.
        while (running) {
            final Socket client;
            try {
                client = control.accept();
            } catch (IOException e) {
                return;
            }
            controlConnections++;
            Thread th = new Thread(new Runnable() {
                /**
                 * Runs one FTP session.
                 * <p>
                 * FTP 세션 하나를 실행한다.
                 */
                public void run() {
                    session(client);
                }
            }, "TestFtpServer-session");
            th.setDaemon(true);
            synchronized (this) {
                sockets.add(client);
                threads.add(th);
            }
            th.start();
        }
    }

    /**
     * Normalizes a path against a base directory; ".." cannot climb above the root.
     * <p>
     * 기준 디렉터리에 대해 경로를 정규화한다. ".." 는 루트 위로 올라갈 수 없다.
     *
     * @param cwd  current directory / 현재 디렉터리
     * @param path absolute or relative path / 절대 또는 상대 경로
     * @return absolute normalized path without trailing slash / 끝 슬래시 없는 절대 정규화 경로
     */
    private static String normalize(String cwd, String path) {
        String full = path.startsWith("/") ? path : cwd + "/" + path;
        List<String> parts = new ArrayList<String>();
        String[] segs = full.split("/");
        // 세그먼트를 훑으며 '.' 은 건너뛰고 '..' 은 한 단계 올라간다.
        for (int i = 0; i < segs.length; i++) {
            if (segs[i].length() == 0 || ".".equals(segs[i])) {
                continue;
            }
            if ("..".equals(segs[i])) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            } else {
                parts.add(segs[i]);
            }
        }
        StringBuilder sb = new StringBuilder();
        // 남은 경로 조각을 이어 붙여 정규화된 경로를 만든다.
        for (int i = 0; i < parts.size(); i++) {
            sb.append('/').append(parts.get(i));
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    /**
     * Returns the parent directory of an absolute path.
     * <p>
     * 절대 경로의 상위 디렉터리를 돌려준다.
     *
     * @param p absolute path / 절대 경로
     * @return parent path / 상위 경로
     */
    private static String parentOf(String p) {
        int i = p.lastIndexOf('/');
        return i <= 0 ? "/" : p.substring(0, i);
    }

    /**
     * Writes a control reply (UTF-8).
     * <p>
     * 제어 응답을 쓴다(UTF-8).
     *
     * @param out  control output / 제어 출력
     * @param text full reply including CRLF / CRLF 를 포함한 응답 전체
     * @throws IOException on failure / 실패 시
     */
    private static void reply(OutputStream out, String text) throws IOException {
        out.write(text.getBytes("UTF-8"));
        out.flush();
    }

    /**
     * One FTP control session.
     * <p>
     * FTP 제어 세션 하나.
     *
     * @param client control socket / 제어 소켓
     */
    private void session(Socket client) {
        ServerSocket pasv = null;
        try {
            client.setSoTimeout(30000);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            OutputStream out = client.getOutputStream();
            if (multilineGreeting) {
                reply(out, "220-Welcome to the test server\r\n220-second line\r\n220 ready\r\n");
            } else {
                reply(out, "220 TestFtpServer ready\r\n");
            }
            String cwd = "/";
            String line;
            // 클라이언트 명령을 한 줄씩 읽어 처리한다.
            while (running && (line = in.readLine()) != null) {
                synchronized (this) {
                    commands.add(line);
                }
                int sp = line.indexOf(' ');
                String cmd = (sp < 0 ? line : line.substring(0, sp)).toUpperCase();
                String arg = sp < 0 ? "" : line.substring(sp + 1);
                if ("USER".equals(cmd)) {
                    reply(out, "anonymous".equalsIgnoreCase(arg) || "ftp".equalsIgnoreCase(arg)
                            ? "331 Send e-mail address as password\r\n" : "530 Only anonymous access\r\n");
                } else if ("PASS".equals(cmd)) {
                    reply(out, failLogin ? "530 Login incorrect\r\n" : "230 Anonymous access granted\r\n");
                } else if ("SYST".equals(cmd)) {
                    reply(out, "215 UNIX Type: L8\r\n");
                } else if ("FEAT".equals(cmd)) {
                    reply(out, "211-Features:\r\n UTF8\r\n" + (mlsdEnabled ? " MLST type*;size*;modify*;\r\n MLSD\r\n" : "")
                            + " SIZE\r\n211 End\r\n");
                } else if ("OPTS".equals(cmd)) {
                    reply(out, "200 OK\r\n");
                } else if ("TYPE".equals(cmd) || "NOOP".equals(cmd)) {
                    reply(out, "200 OK\r\n");
                } else if ("PWD".equals(cmd)) {
                    reply(out, "257 \"" + cwd + "\" is the current directory\r\n");
                } else if ("CWD".equals(cmd) || "CDUP".equals(cmd)) {
                    String target = normalize(cwd, "CDUP".equals(cmd) ? ".." : arg);
                    boolean ok;
                    synchronized (this) {
                        ok = (target.equals("/") || dirs.contains(target)) && !failCwd.contains(target);
                    }
                    if (ok) {
                        cwd = target;
                        reply(out, "250 Directory successfully changed\r\n");
                    } else {
                        reply(out, "550 Failed to change directory\r\n");
                    }
                } else if ("PASV".equals(cmd) || "EPSV".equals(cmd)) {
                    if ("EPSV".equals(cmd) && !epsvEnabled) {
                        reply(out, "500 Unknown command EPSV\r\n");
                        continue;
                    }
                    if (pasv != null) {
                        pasv.close();
                    }
                    pasv = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
                    synchronized (this) {
                        listeners.add(pasv);
                    }
                    int port = pasv.getLocalPort();
                    if ("EPSV".equals(cmd)) {
                        reply(out, "229 Entering Extended Passive Mode (|||" + port + "|)\r\n");
                    } else {
                        String addr = advertisedPasvAddress != null ? advertisedPasvAddress : "127.0.0.1";
                        reply(out, "227 Entering Passive Mode (" + addr.replace('.', ',') + "," + (port / 256) + "," + (port % 256) + ")\r\n");
                    }
                } else if ("LIST".equals(cmd) || "MLSD".equals(cmd)) {
                    if ("MLSD".equals(cmd) && !mlsdEnabled) {
                        reply(out, "500 Unknown command MLSD\r\n");
                    } else if (failList) {
                        reply(out, "550 Permission denied\r\n");
                    } else if (pasv == null) {
                        reply(out, "425 Use PASV first\r\n");
                    } else {
                        String dir = arg.length() == 0 || arg.startsWith("-") ? cwd : normalize(cwd, arg);
                        sendData(out, pasv, listing(dir, "MLSD".equals(cmd)).getBytes("UTF-8"), -1);
                        pasv = null;
                    }
                } else if ("RETR".equals(cmd)) {
                    String path = normalize(cwd, arg);
                    byte[] data;
                    synchronized (this) {
                        data = files.get(path);
                    }
                    if (data == null) {
                        reply(out, "550 Failed to open file\r\n");
                    } else if (pasv == null) {
                        reply(out, "425 Use PASV first\r\n");
                    } else {
                        sendData(out, pasv, data, truncateRetrAt);
                        pasv = null;
                    }
                } else if ("SIZE".equals(cmd)) {
                    byte[] data;
                    synchronized (this) {
                        data = files.get(normalize(cwd, arg));
                    }
                    reply(out, data == null ? "550 Could not get file size\r\n" : "213 " + data.length + "\r\n");
                } else if ("QUIT".equals(cmd)) {
                    reply(out, "221 Goodbye\r\n");
                    break;
                } else {
                    reply(out, "500 Unknown command\r\n");
                }
            }
        } catch (IOException e) {
            // 연결이 끊기면 세션을 끝낸다.
        } finally {
            try {
                if (pasv != null) {
                    pasv.close();
                }
            } catch (IOException e) {
                // 무시한다.
            }
            try {
                client.close();
            } catch (IOException e) {
                // 무시한다.
            }
        }
    }

    /**
     * Sends data over the passive connection and the 150/226 replies on the control connection.
     * <p>
     * 수동 연결로 데이터를 보내고 제어 연결에 150/226 응답을 쓴다.
     *
     * @param out      control output / 제어 출력
     * @param listener passive listener / 수동 대기 소켓
     * @param data     data bytes / 데이터 바이트
     * @param truncate bytes to send or -1 for all / 보낼 바이트 수. 전부면 -1
     * @throws IOException on failure / 실패 시
     */
    private void sendData(OutputStream out, ServerSocket listener, byte[] data, int truncate) throws IOException {
        reply(out, "150 Opening data connection\r\n");
        Socket d = null;
        try {
            listener.setSoTimeout(10000);
            d = listener.accept();
            dataConnections++;
            synchronized (this) {
                sockets.add(d);
            }
            long end = System.currentTimeMillis() + dataDelayMs;
            // 지연이 설정되어 있으면 서버가 멈추기 전까지 조금씩 쉰다.
            while (running && System.currentTimeMillis() < end) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            OutputStream dout = d.getOutputStream();
            int n = truncate >= 0 ? Math.min(truncate, data.length) : data.length;
            dout.write(data, 0, n);
            dout.flush();
        } finally {
            if (d != null) {
                d.close();
            }
            listener.close();
        }
        reply(out, "226 Transfer complete\r\n");
    }

    /**
     * Builds the listing text of a directory in the configured style.
     * <p>
     * 디렉터리의 목록 문자열을 설정된 형식으로 만든다.
     *
     * @param dir  absolute directory / 절대 디렉터리
     * @param mlsd true for MLSD facts format / MLSD 사실 형식이면 true
     * @return listing text with CRLF line ends / CRLF 로 끝나는 목록 문자열
     */
    private synchronized String listing(String dir, boolean mlsd) {
        StringBuilder sb = new StringBuilder();
        if (mlsd) {
            sb.append("type=cdir;modify=20200101101500; .\r\n");
            sb.append("type=pdir;modify=20200101101500; ..\r\n");
        } else if (listStyle == ListStyle.UNIX) {
            sb.append("total 0\r\n");
        }
        Set<String> names = new TreeSet<String>();
        // 디렉터리 중 이 디렉터리의 직계 항목을 모은다.
        for (String d : dirs) {
            if (!d.equals(dir) && parentOf(d).equals(dir)) {
                names.add(d);
            }
        }
        // 파일 중 이 디렉터리의 직계 항목을 모은다.
        for (String f : files.keySet()) {
            if (parentOf(f).equals(dir)) {
                names.add(f);
            }
        }
        // 심볼릭 링크 중 이 디렉터리의 직계 항목을 모은다.
        for (String l : links.keySet()) {
            if (parentOf(l).equals(dir)) {
                names.add(l);
            }
        }
        // 이 디렉터리의 직계 항목을 이름순으로 한 줄씩 만든다.
        for (String path : names) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (dirs.contains(path)) {
                sb.append(mlsd ? "type=dir;modify=20200101101500; " + name
                        : (listStyle == ListStyle.UNIX ? "drwxr-xr-x   2 ftp      ftp          4096 Jan  1  2020 " + name
                                : "01-01-20  10:15AM       <DIR>          " + name)).append("\r\n");
            } else if (links.containsKey(path)) {
                sb.append(mlsd ? "type=OS.unix=slink:" + links.get(path) + ";modify=20200101101500; " + name
                        : "lrwxrwxrwx   1 ftp      ftp             " + links.get(path).length() + " Jan  1  2020 " + name + " -> " + links.get(path))
                        .append("\r\n");
            } else {
                int size = files.get(path).length;
                sb.append(mlsd ? "type=file;size=" + size + ";modify=20200101101500; " + name
                        : (listStyle == ListStyle.UNIX ? "-rw-r--r--   1 ftp      ftp      " + pad(size) + " Jan  1  2020 " + name
                                : "01-01-20  10:15AM " + pad(size) + " " + name)).append("\r\n");
            }
        }
        List<String> raw = rawLines.get(dir);
        if (raw != null && !mlsd) {
            // 원문 줄을 차례로 덧붙인다.
            for (int i = 0; i < raw.size(); i++) {
                sb.append(raw.get(i)).append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Right-aligns a number in a 10 character field.
     * <p>
     * 숫자를 10 글자 칸에 오른쪽 정렬한다.
     *
     * @param n number / 숫자
     * @return padded text / 채워 넣은 문자열
     */
    private static String pad(int n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        // 앞쪽을 공백으로 채워 10 글자로 맞춘다.
        for (int i = s.length(); i < 10; i++) {
            sb.append(' ');
        }
        return sb.append(s).toString();
    }
}
