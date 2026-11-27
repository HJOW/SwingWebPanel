package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Helpers of the window tests: pages in temporary files (loaded with <code>file:</code> addresses), a very small HTTP server on the
 * loopback interface for pages that must arrive slowly, and waiting for a condition while the event dispatch thread keeps running.
 * <p>
 * 창 시험의 도우미: 임시 파일에 둔 페이지(<code>file:</code> 주소로 읽는다), 천천히 도착해야 하는 페이지를 위한 루프백의 아주 작은 HTTP 서버,
 * 그리고 이벤트 디스패치 스레드가 계속 도는 동안 조건을 기다리는 일.
 */
final class TestPages {

    /**
     * A condition that a test waits for.
     * <p>
     * 시험이 기다리는 조건.
     */
    interface Condition {

        /**
         * Tells whether the condition holds; called on the event dispatch thread.
         * <p>
         * 조건이 성립하는지 알려 준다. 이벤트 디스패치 스레드에서 호출된다.
         *
         * @return true once it holds / 성립하면 true
         */
        boolean holds();
    }

    /** The directory of the page files. / 페이지 파일의 디렉터리. */
    private final File dir;

    /** The files written so far. / 지금까지 쓴 파일. */
    private final List<File> files = new ArrayList<File>();

    /** The listening socket of the server, or null. / 서버의 수신 소켓. 없으면 null. */
    private ServerSocket server;

    /** The delay of the server before it answers, in milliseconds. / 서버가 응답하기 전의 지연(밀리초). */
    private volatile long delayMs;

    /**
     * Creates the helper with a new temporary directory.
     * <p>
     * 새 임시 디렉터리로 도우미를 만든다.
     *
     * @throws IOException if the directory cannot be created / 디렉터리를 만들 수 없으면
     */
    TestPages() throws IOException {
        File t = File.createTempFile("swp-desktopapp", "");
        if (!t.delete() || !t.mkdir()) {
            throw new IOException("cannot create " + t);
        }
        dir = t;
    }

    /**
     * Writes an HTML page with a title into a file and returns its <code>file:</code> address.
     * <p>
     * 제목이 있는 HTML 페이지를 파일에 쓰고 그 <code>file:</code> 주소를 반환한다.
     *
     * @param name  the file name / 파일 이름
     * @param title the title of the page / 페이지의 제목
     * @param body  the markup of the body / 본문 마크업
     * @return the address of the file / 파일의 주소
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    String page(String name, String title, String body) throws IOException {
        File f = new File(dir, name);
        Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            out.write("<!doctype html>\n<meta charset=\"utf-8\">\n<title>" + title + "</title>\n<body>" + body + "</body>\n");
        } finally {
            out.close();
        }
        if (!files.contains(f)) {
            files.add(f);
        }
        return url(f);
    }

    /**
     * Returns the address of a file in the form in which the component reports it (<code>file:///C:/...</code>).
     * <p>
     * 컴포넌트가 알려 주는 형태(<code>file:///C:/...</code>)로 파일의 주소를 반환한다.
     *
     * @param f the file / 파일
     * @return the address / 주소
     */
    static String url(File f) {
        String uri = f.toURI().toString();
        // File.toURI 는 "file:/C:/..." 을 주고 컴포넌트는 슬래시 세 개인 형태로 정규화한다.
        return uri.startsWith("file:/") && !uri.startsWith("file://") ? "file:///" + uri.substring("file:/".length()) : uri;
    }

    /**
     * Returns the file of a page that was written.
     * <p>
     * 쓴 페이지의 파일을 반환한다.
     *
     * @param name the file name / 파일 이름
     * @return the file / 파일
     */
    File file(String name) {
        return new File(dir, name);
    }

    /** The pages that a test gave for paths of the server. / 시험이 서버의 경로에 준 페이지. */
    private final java.util.Map<String, String> served = java.util.Collections.synchronizedMap(new java.util.HashMap<String, String>());

    /**
     * Gives the HTTP server the page for a path.
     * <p>
     * HTTP 서버에 경로의 페이지를 준다.
     *
     * @param path the request path, for example <code>/page</code> / 요청 경로. 예: <code>/page</code>
     * @param html the document / 문서
     */
    void serve(String path, String html) {
        served.put(path, html);
    }

    /**
     * Starts the HTTP server: every request is answered, after the delay, with a small HTML page whose title is the request path.
     * Some paths give special pages: <code>/set?name</code> sets a cookie, <code>/echo</code> shows the received cookies as
     * <code>got:...</code>, <code>/vis</code> shows <code>vis:</code> and the visibility state of the document and follows its changes,
     * <code>/session</code> shows <code>session:</code> and the stored value of this tab and then stores one.
     * <p>
     * HTTP 서버를 시작한다: 모든 요청에 지연 뒤, 요청 경로를 제목으로 하는 작은 HTML 페이지로 응답한다. 몇 경로는 특별한 페이지를 준다:
     * <code>/set?이름</code> 은 쿠키를 설정하고, <code>/echo</code> 는 받은 쿠키를 <code>got:...</code> 으로 보이며, <code>/vis</code> 는
     * <code>vis:</code> 와 문서의 가시성 상태를 보이고 그 변화를 따라가며, <code>/session</code> 은 <code>session:</code> 과 이 탭에 저장된 값을
     * 보인 뒤 값을 저장한다.
     *
     * @param delay the delay before each answer in milliseconds / 응답 전 지연(밀리초)
     * @return the address prefix of the server, for example <code>http://127.0.0.1:1234</code> / 서버의 주소 앞부분. 예:
     *         <code>http://127.0.0.1:1234</code>
     * @throws IOException if the server cannot listen / 서버가 수신할 수 없으면
     */
    String startServer(long delay) throws IOException {
        delayMs = delay;
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        final ServerSocket listening = server;
        Thread accept = new Thread(new Runnable() {
            /** Accepts connections until the socket is closed. / 소켓이 닫힐 때까지 연결을 받는다. */
            public void run() {
                // 연결마다 별도 스레드에서 응답한다.
                while (!listening.isClosed()) {
                    try {
                        final Socket s = listening.accept();
                        Thread t = new Thread(new Runnable() {
                            /** Answers one request. / 요청 하나에 응답한다. */
                            public void run() {
                                answer(s);
                            }
                        }, "desktopapp-test-http");
                        t.setDaemon(true);
                        t.start();
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }, "desktopapp-test-http-accept");
        accept.setDaemon(true);
        accept.start();
        return "http://127.0.0.1:" + server.getLocalPort();
    }

    /**
     * Changes the delay of the server.
     * <p>
     * 서버의 지연을 바꾼다.
     *
     * @param delay the delay in milliseconds / 지연(밀리초)
     */
    void setDelay(long delay) {
        delayMs = delay;
    }

    /**
     * Reads one request and writes the answer.
     * <p>
     * 요청 하나를 읽고 응답을 쓴다.
     *
     * @param s the connection / 연결
     */
    private void answer(Socket s) {
        try {
            try {
                InputStream in = s.getInputStream();
                ByteArrayOutputStream head = new ByteArrayOutputStream();
                int state = 0;
                // 머리의 끝(빈 줄)까지 읽는다.
                while (state < 4) {
                    int c = in.read();
                    if (c < 0) {
                        return;
                    }
                    head.write(c);
                    state = (c == '\r' && (state == 0 || state == 2)) || (c == '\n' && (state == 1 || state == 3)) ? state + 1 : (c == '\r' ? 1 : 0);
                }
                String request = head.toString("ISO-8859-1");
                int a = request.indexOf(' ');
                int b = request.indexOf(' ', a + 1);
                String path = a > 0 && b > a ? request.substring(a + 1, b) : "/";
                long wait = delayMs;
                if (wait > 0) {
                    Thread.sleep(wait);
                }
                String extra = "";
                String html = "<!doctype html><title>" + path + "</title><p>served " + path + "</p>";
                String given = served.get(path);
                // 시험이 준 페이지가 먼저이고, 몇 경로는 탭 시험을 위한 특별한 페이지를 준다.
                if (given != null) {
                    html = given;
                } else if (path.startsWith("/set?")) {
                    extra = "Set-Cookie: " + path.substring("/set?".length()) + "=1; Path=/\r\n";
                    html = "<!doctype html><title>set</title><p>cookie set</p>";
                } else if (path.equals("/echo")) {
                    html = "<!doctype html><title>got:" + header(request, "Cookie") + "</title><p>cookies</p>";
                } else if (path.equals("/vis")) {
                    html = "<!doctype html><title>vis</title><p>visibility</p><script>"
                            + "function show() { document.title = 'vis:' + document.visibilityState; }"
                            + "document.addEventListener('visibilitychange', show); show();</" + "script>";
                } else if (path.equals("/session")) {
                    html = "<!doctype html><title>session</title><p>session storage</p><script>"
                            + "document.title = 'session:' + sessionStorage.getItem('k'); sessionStorage.setItem('k', 'set');</" + "script>";
                }
                byte[] body = html.getBytes("UTF-8");
                OutputStream out = s.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" + extra + "Content-Length: " + body.length
                        + "\r\nConnection: close\r\n\r\n").getBytes("ISO-8859-1"));
                out.write(body);
                out.flush();
            } finally {
                s.close();
            }
        } catch (IOException e) {
            // 시험이 요청을 취소하면 연결이 끊긴다. 서버 쪽에서는 무시한다.
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the value of a header of a request head, or the empty string.
     * <p>
     * 요청 머리에서 헤더의 값을 반환한다. 없으면 빈 문자열이다.
     *
     * @param request the request head / 요청 머리
     * @param name    the header name / 헤더 이름
     * @return the value or the empty string / 값 또는 빈 문자열
     */
    private static String header(String request, String name) {
        String[] lines = request.split("\r\n");
        // 줄마다 이름이 같은 헤더인지 본다(대소문자 무시).
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0 && lines[i].substring(0, colon).trim().equalsIgnoreCase(name)) {
                return lines[i].substring(colon + 1).trim();
            }
        }
        return "";
    }

    /**
     * Waits until a condition holds, checking it on the event dispatch thread, and tells whether it held in time.
     * <p>
     * 조건을 이벤트 디스패치 스레드에서 확인하며 성립할 때까지 기다리고, 제때 성립했는지 알려 준다.
     *
     * @param condition the condition / 조건
     * @return true if it held within the time limit / 제한 시간 안에 성립했으면 true
     * @throws Exception if interrupted / 인터럽트되면
     */
    static boolean await(final Condition condition) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        final boolean[] held = new boolean[1];
        // 성립하거나 시간이 다할 때까지 짧게 쉬며 확인한다.
        for (;;) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Checks the condition on the EDT. / EDT 에서 조건을 확인한다. */
                public void run() {
                    held[0] = condition.holds();
                }
            });
            if (held[0] || System.currentTimeMillis() >= end) {
                return held[0];
            }
            Thread.sleep(20);
        }
    }

    /**
     * Stops the server and deletes the page files.
     * <p>
     * 서버를 멈추고 페이지 파일을 지운다.
     */
    void close() {
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                // 닫는 중의 오류는 시험 결과와 무관하다.
                server = null;
            }
        }
        // 쓴 파일과 디렉터리를 지운다.
        for (int i = 0; i < files.size(); i++) {
            files.get(i).delete();
        }
        dir.delete();
    }
}
