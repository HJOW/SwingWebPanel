package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * Tests of the HTTP/HTTPS test servers themselves (plan item P02-06) using raw sockets and the JRE client, so that
 * later product tests can trust the servers.
 * <p>
 * HTTP/HTTPS 시험 서버 자체의 시험(계획 P02-06). raw 소켓과 JRE 클라이언트를 써서 이후의 제품 시험이 서버를 믿을 수 있게 한다.
 */
public class TestServersTest {

    /**
     * Server under test, stopped in {@link #tearDown()}.
     * <p>
     * 시험 대상 서버. {@link #tearDown()} 에서 멈춘다.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TestServersTest() {
    }

    /**
     * Stops the server after each case.
     * <p>
     * 사례마다 끝난 뒤 서버를 멈춘다.
     */
    public void tearDown() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Sends raw bytes to the server and reads the whole answer until the server closes the connection.
     * <p>
     * 서버에 원시 바이트를 보내고 서버가 연결을 닫을 때까지 응답 전체를 읽는다.
     *
     * @param port    server port / 서버 포트
     * @param request request text sent as ISO-8859-1 / ISO-8859-1 로 보낼 요청 문자열
     * @return response bytes / 응답 바이트
     * @throws IOException on failure / 실패 시
     */
    private static byte[] raw(int port, String request) throws IOException {
        Socket s = new Socket("127.0.0.1", port);
        try {
            s.setSoTimeout(10000);
            OutputStream out = s.getOutputStream();
            out.write(request.getBytes("ISO-8859-1"));
            out.flush();
            InputStream in = s.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 서버가 닫을 때까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            s.close();
        }
    }

    /**
     * Converts bytes to a string with ISO-8859-1.
     * <p>
     * 바이트를 ISO-8859-1 문자열로 바꾼다.
     *
     * @param b bytes / 바이트
     * @return text / 문자열
     * @throws IOException never in practice / 실제로는 발생하지 않음
     */
    private static String latin1(byte[] b) throws IOException {
        return new String(b, "ISO-8859-1");
    }

    /**
     * A GET gets the registered body and is logged with its headers.
     * <p>
     * GET 이 등록한 본문을 받고 헤더와 함께 기록된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGetAndRequestLog() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/hello", TestHttpServer.Response.html("<p>hi</p>"));
        server.start();
        String r = latin1(raw(server.getPort(), "GET /hello?x=1 HTTP/1.1\r\nHost: t\r\nX-Test: A\r\n\r\n"));
        Check.isTrue(r.startsWith("HTTP/1.1 200 OK\r\n"), "status line: " + r);
        Check.contains(r, "Connection: close", "close header");
        Check.isTrue(r.endsWith("<p>hi</p>"), "body");
        Check.equal(1, server.count("/hello"), "request count");
        TestHttpServer.Request q = server.last("/hello");
        Check.equal("x=1", q.query, "query");
        Check.equal("A", q.header("x-test"), "custom header lookup is case-insensitive");
    }

    /**
     * Unknown paths give 404; prefix routes match; handler exceptions give 500.
     * <p>
     * 알 수 없는 경로는 404, 접두사 경로는 일치, 처리기 예외는 500.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRoutingAndErrors() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/files/*", TestHttpServer.Response.ok("text/plain", "prefix"));
        server.route("/boom", new TestHttpServer.Handler() {
            /**
             * Always fails.
             * <p>
             * 항상 실패한다.
             *
             * @param request request / 요청
             * @return never / 반환하지 않음
             * @throws Exception always / 항상
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                throw new IllegalStateException("boom");
            }
        });
        server.start();
        Check.contains(latin1(raw(server.getPort(), "GET /nothing HTTP/1.1\r\n\r\n")), " 404 ", "404");
        Check.contains(latin1(raw(server.getPort(), "GET /files/a/b HTTP/1.1\r\n\r\n")), "prefix", "prefix route");
        Check.contains(latin1(raw(server.getPort(), "GET /boom HTTP/1.1\r\n\r\n")), " 500 ", "500");
    }

    /**
     * POST bodies are parsed with Content-Length and with chunked transfer encoding.
     * <p>
     * POST 본문을 Content-Length 와 chunked 전송 인코딩으로 모두 해석한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPostBodies() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/p", TestHttpServer.Response.ok("text/plain", "ok"));
        server.start();
        raw(server.getPort(), "POST /p HTTP/1.1\r\nContent-Length: 5\r\n\r\nhello");
        Check.equal("hello", server.requests().get(0).bodyText(), "content-length body");
        raw(server.getPort(), "POST /p HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n");
        Check.equal("abcde", server.requests().get(1).bodyText(), "chunked body");
    }

    /**
     * Chunked, truncated and dropped responses behave as specified.
     * <p>
     * chunked, 잘린 응답, 끊긴 응답이 명세대로 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChunkedTruncatedAndDropped() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/chunk", TestHttpServer.Response.ok("text/plain", "0123456789").chunked(4));
        server.routeFixed("/trunc", TestHttpServer.Response.ok("text/plain", "0123456789").truncateAt(4));
        server.routeFixed("/drop", dropResponse());
        server.start();
        String c = latin1(raw(server.getPort(), "GET /chunk HTTP/1.1\r\n\r\n"));
        Check.contains(c, "Transfer-Encoding: chunked", "chunked header");
        Check.isTrue(c.endsWith("2\r\n89\r\n0\r\n\r\n"), "terminating chunk: " + c);
        String t = latin1(raw(server.getPort(), "GET /trunc HTTP/1.1\r\n\r\n"));
        Check.contains(t, "Content-Length: 10", "declared length");
        Check.isTrue(t.endsWith("\r\n\r\n0123"), "only four body bytes: " + t);
        Check.equal(0, raw(server.getPort(), "GET /drop HTTP/1.1\r\n\r\n").length, "dropped connection sends nothing");
    }

    /**
     * Builds a response that makes the server close without answering.
     * <p>
     * 서버가 응답 없이 닫도록 하는 응답을 만든다.
     *
     * @return the response / 응답
     */
    private static TestHttpServer.Response dropResponse() {
        TestHttpServer.Response r = new TestHttpServer.Response(200);
        r.dropWithoutResponse = true;
        return r;
    }

    /**
     * Compressed bodies decode with the JRE gzip and inflater classes.
     * <p>
     * 압축 본문을 JRE 의 gzip/inflater 클래스로 풀 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompressedResponses() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/gz", TestHttpServer.Response.ok("text/plain", "compress me compress me").gzip());
        server.routeFixed("/df", TestHttpServer.Response.ok("text/plain", "compress me compress me").deflate(true));
        server.start();
        HttpURLConnection c = (HttpURLConnection) new URL(server.url("/gz")).openConnection();
        Check.equal("gzip", c.getHeaderField("Content-Encoding"), "encoding header");
        java.util.zip.GZIPInputStream gz = new java.util.zip.GZIPInputStream(c.getInputStream());
        Check.equal("compress me compress me", readAll(gz), "gunzip");
        HttpURLConnection d = (HttpURLConnection) new URL(server.url("/df")).openConnection();
        java.util.zip.InflaterInputStream inf = new java.util.zip.InflaterInputStream(d.getInputStream());
        Check.equal("compress me compress me", readAll(inf), "inflate");
    }

    /**
     * Reads a stream fully as UTF-8 text.
     * <p>
     * 스트림 전체를 UTF-8 문자열로 읽는다.
     *
     * @param in stream / 스트림
     * @return text / 문자열
     * @throws IOException on failure / 실패 시
     */
    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        // 스트림 끝까지 버퍼 단위로 읽어 모은다.
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    /**
     * A delayed response arrives late, and stop() closes everything so that new connections fail.
     * <p>
     * 지연된 응답은 늦게 도착하고, stop() 이 모든 것을 닫아 새 연결이 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDelayAndStop() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(300));
        server.start();
        long t0 = System.currentTimeMillis();
        String r = latin1(raw(server.getPort(), "GET /slow HTTP/1.1\r\n\r\n"));
        Check.isTrue(System.currentTimeMillis() - t0 >= 250, "response was delayed");
        Check.isTrue(r.endsWith("late"), "body arrived");
        final int port = server.getPort();
        server.stop();
        server = null;
        Check.throwsException(IOException.class, new Block() {
            /**
             * Connects to the stopped server.
             * <p>
             * 멈춘 서버에 접속한다.
             *
             * @throws Exception expected connection failure / 기대한 접속 실패
             */
            public void run() throws Exception {
                new Socket("127.0.0.1", port).close();
            }
        }, "connect after stop must fail");
    }

    /**
     * After stop() no server thread remains alive (sockets and threads are cleaned up).
     * <p>
     * stop() 뒤에는 서버 스레드가 하나도 남지 않는다(소켓과 스레드 정리).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStopLeavesNoThreads() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
        server.start();
        raw(server.getPort(), "GET /x HTTP/1.1\r\n\r\n");
        // 연결을 열어 둔 채(요청을 보내지 않음) 정지해도 연결 스레드가 정리되어야 한다.
        Socket idle = new Socket("127.0.0.1", server.getPort());
        server.stop();
        server = null;
        idle.close();
        int alive = countServerThreads();
        long end = System.currentTimeMillis() + 15000;
        // CPU 가 바쁠 때 스레드가 끝나는 데 시간이 걸리므로 제한 시간까지 기다리며 다시 센다.
        while (alive > 0 && System.currentTimeMillis() < end) {
            Thread.sleep(50);
            alive = countServerThreads();
        }
        Check.equal(0, alive, "no TestHttpServer thread may survive stop()");
    }

    /**
     * Counts the live threads that belong to a test server.
     * <p>
     * 시험 서버에 속한 살아 있는 스레드의 수를 센다.
     *
     * @return number of threads / 스레드 수
     */
    private static int countServerThreads() {
        int alive = 0;
        // 모든 스레드를 훑어 서버 이름을 가진 살아 있는 스레드를 센다.
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("TestHttpServer-")) {
                alive++;
            }
        }
        return alive;
    }

    /**
     * HTTPS works for a client that trusts the test certificate and fails for the default trust store,
     * proving that no global trust was changed.
     * <p>
     * 시험 인증서를 신뢰하는 클라이언트에서는 HTTPS 가 동작하고 기본 신뢰 저장소로는 실패한다. 전역 신뢰가 바뀌지 않았다는 증거이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsWithTestTrustOnly() throws Exception {
        server = new TestHttpServer(TestSsl.serverContext());
        server.routeFixed("/s", TestHttpServer.Response.ok("text/plain", "secure"));
        server.start();
        HttpsURLConnection ok = (HttpsURLConnection) new URL(server.url("/s")).openConnection();
        ok.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
        Check.equal("secure", readAll(ok.getInputStream()), "https body with test trust");

        final HttpsURLConnection bad = (HttpsURLConnection) new URL(server.url("/s")).openConnection();
        Check.throwsException(SSLException.class, new Block() {
            /**
             * Connects without the test trust.
             * <p>
             * 시험 신뢰 없이 접속한다.
             *
             * @throws Exception expected TLS failure / 기대한 TLS 실패
             */
            public void run() throws Exception {
                bad.getInputStream();
            }
        }, "default trust must reject the test certificate");
    }

    /**
     * A TLS client talking to the plain HTTP server fails quickly with an SSL exception instead of hanging.
     * <p>
     * 평문 HTTP 서버에 TLS 로 접속하면 멈추지 않고 곧바로 SSL 예외로 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTlsClientAgainstPlainServerFailsFast() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
        server.start();
        final HttpsURLConnection c = (HttpsURLConnection) new URL("https://localhost:" + server.getPort() + "/x").openConnection();
        c.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
        c.setReadTimeout(8000);
        long t0 = System.currentTimeMillis();
        Check.throwsException(SSLException.class, new Block() {
            /**
             * Starts a TLS handshake against the plain server.
             * <p>
             * 평문 서버에 TLS 핸드셰이크를 시작한다.
             *
             * @throws Exception expected SSL failure / 기대한 SSL 실패
             */
            public void run() throws Exception {
                c.getInputStream();
            }
        }, "plain server must make the TLS handshake fail");
        Check.isTrue(System.currentTimeMillis() - t0 < 6000, "failure must be fast");
    }
}
