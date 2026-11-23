package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * Tests of the socket based HTTP/HTTPS connections ({@link SocketHttpURLConnection}, {@link SocketHttpsURLConnection}, plan item P20-08-c):
 * they send the restricted request headers (<code>Origin</code>, <code>Access-Control-Request-*</code>) and any method that the
 * <code>HttpURLConnection</code> of the JRE drops or refuses, read the response framing (content length, chunked, until close, no body for
 * HEAD), keep error bodies, refuse header injection, end blocked reads on <code>disconnect()</code>, and check the TLS certificate chain and
 * the host name ({@link HostnameMatcher}) without touching any JVM wide TLS setting.
 * <p>
 * 소켓 기반 HTTP/HTTPS 연결({@link SocketHttpURLConnection}, {@link SocketHttpsURLConnection}, 계획 P20-08-c)을 시험한다. JRE 의
 * <code>HttpURLConnection</code> 이 버리거나 거부하는 제한 요청 헤더(<code>Origin</code>, <code>Access-Control-Request-*</code>)와 메서드를
 * 보내고, 응답 경계(content-length, chunked, 닫힐 때까지, HEAD 는 본문 없음)를 읽고, 오류 본문을 유지하고, 헤더 주입을 거부하고,
 * <code>disconnect()</code> 로 막힌 읽기를 끝내고, JVM 전역 TLS 설정을 건드리지 않고 TLS 인증서 사슬과 호스트 이름({@link HostnameMatcher})을
 * 확인한다.
 */
public class SocketHttpClientTest {

    /**
     * The plain test server.
     * <p>
     * 평문 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The https test server, or null if the case did not start one.
     * <p>
     * https 시험 서버. 사례가 시작하지 않았으면 null.
     */
    private TestHttpServer secure;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SocketHttpClientTest() {
    }

    /**
     * Creates the plain server (it is started by the cases after their routes are registered).
     * <p>
     * 평문 서버를 만든다(사례가 경로를 등록한 뒤 시작한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
    }

    /**
     * Stops the servers.
     * <p>
     * 서버를 멈춘다.
     */
    public void tearDown() {
        server.stop();
        if (secure != null) {
            secure.stop();
        }
    }

    /**
     * Opens a socket connection to a path of the plain server (the server is started here).
     * <p>
     * 평문 서버의 경로로 소켓 연결을 연다(서버는 여기서 시작한다).
     *
     * @param path the path / 경로
     * @return the connection / 연결
     * @throws IOException on failure / 실패 시
     */
    private HttpURLConnection plain(String path) throws IOException {
        server.start();
        return new SocketHttpURLConnection(new URL(server.url(path)));
    }

    /**
     * Reads a stream to its end as UTF-8 text.
     * <p>
     * 스트림을 끝까지 UTF-8 문자열로 읽는다.
     *
     * @param in the stream / 스트림
     * @return the text / 문자열
     * @throws IOException on failure / 실패 시
     */
    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        // 스트림이 끝날 때까지 읽는다.
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /**
     * The restricted headers <code>Origin</code> and <code>Access-Control-Request-*</code> reach the server, with a method that is no
     * ordinary one, the <code>Host</code> header and <code>Connection: close</code>.
     * <p>
     * 제한 헤더 <code>Origin</code> 과 <code>Access-Control-Request-*</code> 가 평범하지 않은 메서드, <code>Host</code> 헤더,
     * <code>Connection: close</code> 와 함께 서버에 도착한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRestrictedHeadersAreSent() throws Exception {
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "hello"));
        HttpURLConnection c = plain("/x");
        c.setRequestMethod("OPTIONS");
        c.setRequestProperty("Origin", "http://page.example");
        c.setRequestProperty("Access-Control-Request-Method", "PUT");
        c.setRequestProperty("access-control-request-headers", "x-a,x-b");
        c.setRequestProperty("X-Custom", "1");
        Check.equal(200, c.getResponseCode(), "status");
        Check.equal("OK", c.getResponseMessage(), "reason phrase");
        Check.equal("hello", read(c.getInputStream()), "body");
        TestHttpServer.Request r = server.last("/x");
        Check.equal("OPTIONS", r.method, "method");
        Check.equal("http://page.example", r.header("origin"), "Origin");
        Check.equal("PUT", r.header("access-control-request-method"), "request method header");
        Check.equal("x-a,x-b", r.header("access-control-request-headers"), "request headers header");
        Check.equal("1", r.header("x-custom"), "ordinary header");
        Check.equal("localhost:" + server.getPort(), r.header("host"), "Host with the port");
        Check.equal("close", r.header("connection"), "Connection");
        c.disconnect();
    }

    /**
     * A request body is sent with its length (also non ASCII text), and a request header that is set again replaces the old value
     * case-insensitively.
     * <p>
     * 요청 본문은 길이와 함께 간다(ASCII 가 아닌 문자열도). 다시 정한 요청 헤더는 대소문자와 관계없이 이전 값을 대신한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPostBodyIsSentWithItsLength() throws Exception {
        server.route("/echo", new TestHttpServer.Handler() {
            /**
             * Answers with the body of the request.
             * <p>
             * 요청의 본문을 응답으로 돌려준다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.ok("text/plain; charset=UTF-8", "got " + request.bodyText());
            }
        });
        HttpURLConnection c = plain("/echo");
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("X-Twice", "first");
        c.setRequestProperty("x-twice", "second");
        c.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        OutputStream out = c.getOutputStream();
        out.write("한글".getBytes("UTF-8"));
        out.close();
        Check.equal("got 한글", read(c.getInputStream()), "echo");
        TestHttpServer.Request r = server.last("/echo");
        Check.equal("6", r.header("content-length"), "length of the UTF-8 body");
        Check.equal("second", r.header("x-twice"), "the later value replaced the earlier one");
        c.disconnect();
    }

    /**
     * Any method token is accepted (<code>PATCH</code> is refused by the JRE class), a token with a blank is refused, and the method cannot
     * change once the connection was used.
     * <p>
     * 어떤 메서드 토큰이든 받아들이며(<code>PATCH</code> 는 JRE 클래스가 거부한다), 공백이 있는 토큰은 거부하고, 연결을 쓴 뒤에는 메서드를 바꿀 수
     * 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnyMethodTokenAndFrozenMethod() throws Exception {
        server.routeFixed("/m", TestHttpServer.Response.ok("text/plain", "m"));
        HttpURLConnection c = plain("/m");
        try {
            c.setRequestMethod("BAD METHOD");
            Check.fail("a method with a blank must be refused");
        } catch (ProtocolException expected) {
            Check.isTrue(expected.getMessage().indexOf("BAD METHOD") >= 0, "message names the method");
        }
        c.setRequestMethod("PATCH");
        Check.equal("PATCH", c.getRequestMethod(), "the method is reported");
        Check.equal(200, c.getResponseCode(), "status");
        Check.equal("PATCH", server.last("/m").method, "the server saw PATCH");
        try {
            c.setRequestMethod("GET");
            Check.fail("the method of a used connection is frozen");
        } catch (ProtocolException expected) {
            Check.isTrue(true, "refused");
        }
        try {
            c.setRequestProperty("X-Late", "1");
            Check.fail("headers of a used connection are frozen");
        } catch (IllegalStateException expected) {
            Check.isTrue(true, "refused");
        }
        c.disconnect();
    }

    /**
     * The status line and the headers are available by name and by index, the status line has the key null, and a missing header is null.
     * <p>
     * 상태 줄과 헤더는 이름과 번호로 얻을 수 있고, 상태 줄의 키는 null 이며, 없는 헤더는 null 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testResponseHeaders() throws Exception {
        server.routeFixed("/h", TestHttpServer.Response.ok("text/plain", "abc").header("X-One", "1").header("X-Two", "2a").header("x-two", "2b"));
        HttpURLConnection c = plain("/h");
        Check.isTrue(c.getHeaderField(0).startsWith("HTTP/1.1 200"), "status line: " + c.getHeaderField(0));
        Check.isNull(c.getHeaderFieldKey(0), "the status line has no name");
        Check.equal("1", c.getHeaderField("x-one"), "by name, case-insensitively");
        Check.equal("2b", c.getHeaderField("X-Two"), "the last value of a repeated header");
        Check.isNull(c.getHeaderField("X-Missing"), "missing header");
        Check.equal(3, c.getContentLength(), "content length");
        Check.isTrue(c.getHeaderFields().containsKey(null), "the status line is in the map under null");
        Check.equal("abc", read(c.getInputStream()), "body");
        c.disconnect();
    }

    /**
     * Chunked bodies, bodies that end when the connection closes and bodies with a content length are all read in full.
     * <p>
     * chunked 본문, 연결이 닫힐 때 끝나는 본문, content-length 가 있는 본문을 모두 끝까지 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBodyFraming() throws Exception {
        String text = "hello chunked world, with a body that needs many chunks";
        server.routeFixed("/chunked", TestHttpServer.Response.ok("text/plain", text).chunked(7));
        TestHttpServer.Response close = TestHttpServer.Response.ok("text/plain", text);
        close.noLength = true;
        server.routeFixed("/close", close);
        server.routeFixed("/length", TestHttpServer.Response.ok("text/plain", text));
        server.routeFixed("/gzip", TestHttpServer.Response.ok("text/plain", text).gzip());
        Check.equal(text, read(plain("/chunked").getInputStream()), "chunked");
        Check.equal(text, read(new SocketHttpURLConnection(new URL(server.url("/close"))).getInputStream()), "until the connection closes");
        Check.equal(text, read(new SocketHttpURLConnection(new URL(server.url("/length"))).getInputStream()), "content length");
        InputStream zipped = new SocketHttpURLConnection(new URL(server.url("/gzip"))).getInputStream();
        Check.isTrue(zipped.read() == 0x1f, "the body is delivered as it came (decoding is the job of the fetcher)");
    }

    /**
     * A response to HEAD has no body, whatever its headers say.
     * <p>
     * HEAD 에 대한 응답은 헤더가 무엇이라 하든 본문이 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeadHasNoBody() throws Exception {
        server.routeFixed("/head", TestHttpServer.Response.ok("text/plain", "some body"));
        HttpURLConnection c = plain("/head");
        c.setRequestMethod("HEAD");
        Check.equal(200, c.getResponseCode(), "status");
        Check.equal(-1, c.getInputStream().read(), "no body");
        c.disconnect();
    }

    /**
     * An error status keeps its body in the error stream, <code>getInputStream</code> fails (a missing page with a
     * <code>FileNotFoundException</code>) and the error stream of a good response is null.
     * <p>
     * 오류 상태는 본문을 오류 스트림에 유지하고, <code>getInputStream</code> 은 실패하며(없는 페이지는 <code>FileNotFoundException</code>), 정상
     * 응답의 오류 스트림은 null 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorStatusKeepsItsBody() throws Exception {
        server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "no such page"));
        server.routeFixed("/boom", new TestHttpServer.Response(500).text("text/plain", "it broke"));
        server.routeFixed("/fine", TestHttpServer.Response.ok("text/plain", "fine"));
        HttpURLConnection missing = plain("/missing");
        Check.equal(404, missing.getResponseCode(), "status 404");
        try {
            missing.getInputStream();
            Check.fail("getInputStream of a 404 must fail");
        } catch (FileNotFoundException expected) {
            Check.isTrue(true, "FileNotFoundException");
        }
        Check.equal("no such page", read(missing.getErrorStream()), "error body of the 404");
        HttpURLConnection boom = new SocketHttpURLConnection(new URL(server.url("/boom")));
        try {
            boom.getInputStream();
            Check.fail("getInputStream of a 500 must fail");
        } catch (IOException expected) {
            Check.isTrue(expected.getMessage().indexOf("500") >= 0, "message names the status: " + expected.getMessage());
        }
        Check.equal("it broke", read(boom.getErrorStream()), "error body of the 500");
        HttpURLConnection fine = new SocketHttpURLConnection(new URL(server.url("/fine")));
        Check.equal(200, fine.getResponseCode(), "status 200");
        Check.isNull(fine.getErrorStream(), "no error stream for a good response");
    }

    /**
     * A body that is shorter than its content length fails with an end-of-file error instead of looking complete.
     * <p>
     * 선언한 길이보다 짧은 본문은 완전한 것처럼 보이지 않고 파일 끝 오류로 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedBodyFails() throws Exception {
        server.routeFixed("/cut", TestHttpServer.Response.ok("text/plain", "0123456789").truncateAt(4));
        HttpURLConnection c = plain("/cut");
        try {
            read(c.getInputStream());
            Check.fail("a truncated body must fail");
        } catch (EOFException expected) {
            Check.isTrue(true, "premature end of the body");
        }
        c.disconnect();
    }

    /**
     * Header names and values that could split the request are refused before anything is sent.
     * <p>
     * 요청을 쪼갤 수 있는 헤더 이름과 값은 아무것도 보내기 전에 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeaderInjectionIsRefused() throws Exception {
        server.routeFixed("/i", TestHttpServer.Response.ok("text/plain", "i"));
        HttpURLConnection c = plain("/i");
        String[][] bad = { { "X-A", "v\r\nEvil: 1" }, { "X-A", "v\nEvil: 1" }, { "X A", "v" }, { "X-A:", "v" }, { "", "v" } };
        // 위험한 이름과 값을 하나씩 시도하여 모두 거부되는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            try {
                c.setRequestProperty(bad[i][0], bad[i][1]);
                Check.fail("must refuse " + i);
            } catch (IllegalArgumentException expected) {
                Check.isTrue(true, "refused " + i);
            }
        }
        Check.equal(200, c.getResponseCode(), "the connection still works");
        Check.isNull(server.last("/i").header("evil"), "nothing was injected");
        c.disconnect();
    }

    /**
     * <code>disconnect()</code> from another thread ends a read that is blocked on a slow server.
     * <p>
     * 다른 스레드의 <code>disconnect()</code> 가 느린 서버 때문에 막힌 읽기를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisconnectEndsABlockedRead() throws Exception {
        server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(8000));
        final HttpURLConnection c = plain("/slow");
        final IOException[] failure = new IOException[1];
        final boolean[] finished = new boolean[1];
        Thread t = new Thread(new Runnable() {
            /**
             * Reads the response and records how it ended.
             * <p>
             * 응답을 읽고 어떻게 끝났는지 기록한다.
             */
            public void run() {
                try {
                    c.getResponseCode();
                } catch (IOException e) {
                    failure[0] = e;
                }
                finished[0] = true;
            }
        });
        t.start();
        Thread.sleep(300);
        long start = System.currentTimeMillis();
        c.disconnect();
        t.join(5000);
        Check.isTrue(finished[0], "the reader ended");
        Check.notNull(failure[0], "with an error");
        Check.isTrue(System.currentTimeMillis() - start < 4000, "quickly");
    }

    /**
     * A connection to a closed port fails with an <code>IOException</code> and a read timeout ends a silent server with
     * <code>SocketTimeoutException</code>.
     * <p>
     * 닫힌 포트로의 연결은 <code>IOException</code> 으로 실패하고, 읽기 제한 시간은 조용한 서버를 <code>SocketTimeoutException</code> 으로
     * 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConnectFailureAndReadTimeout() throws Exception {
        server.routeFixed("/silent", TestHttpServer.Response.ok("text/plain", "late").delay(3000));
        HttpURLConnection slow = plain("/silent");
        slow.setReadTimeout(300);
        try {
            slow.getResponseCode();
            Check.fail("must time out");
        } catch (java.net.SocketTimeoutException expected) {
            Check.isTrue(true, "read timed out");
        }
        slow.disconnect();
        int port = server.getPort();
        server.stop();
        HttpURLConnection closed = new SocketHttpURLConnection(new URL("http://127.0.0.1:" + port + "/"));
        closed.setConnectTimeout(2000);
        try {
            closed.getResponseCode();
            Check.fail("must fail to connect");
        } catch (IOException expected) {
            Check.isTrue(true, "connection refused: " + expected);
        }
    }

    /**
     * Starts the https server with the test certificate (CN=localhost) and returns a socket connection to a path of it.
     * <p>
     * 시험 인증서(CN=localhost)를 쓰는 https 서버를 시작하고 그 경로로의 소켓 연결을 반환한다.
     *
     * @param host the host name or address to connect to / 연결할 호스트 이름이나 주소
     * @return the connection / 연결
     * @throws Exception on failure / 실패 시
     */
    private SocketHttpsURLConnection https(String host) throws Exception {
        secure = new TestHttpServer(TestSsl.serverContext());
        secure.routeFixed("/s", TestHttpServer.Response.ok("text/plain", "secret"));
        secure.start();
        return new SocketHttpsURLConnection(new URL("https://" + host + ":" + secure.getPort() + "/s"));
    }

    /**
     * An https request over a trusted test certificate works, sends the restricted headers and reports the cipher suite and the
     * certificates of the server.
     * <p>
     * 신뢰하는 시험 인증서로의 https 요청은 동작하고, 제한 헤더를 보내며, 암호 스위트와 서버의 인증서를 보고한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsWithTrustedCertificate() throws Exception {
        SocketHttpsURLConnection c = https("localhost");
        c.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
        c.setRequestProperty("Origin", "https://page.example");
        Check.equal(200, c.getResponseCode(), "status");
        Check.equal("secret", read(c.getInputStream()), "body");
        Check.equal("https://page.example", secure.last("/s").header("origin"), "Origin over TLS");
        Check.notNull(c.getCipherSuite(), "cipher suite");
        Check.isTrue(c.getServerCertificates().length > 0, "server certificates");
        c.disconnect();
    }

    /**
     * A certificate that the default trust store does not know fails the handshake with an <code>SSLException</code>, so that the fetcher
     * can retry over http.
     * <p>
     * 기본 신뢰 저장소가 모르는 인증서는 핸드셰이크를 <code>SSLException</code> 으로 실패시키므로 가져오기 객체가 http 로 재시도할 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsUntrustedCertificateFails() throws Exception {
        SocketHttpsURLConnection c = https("localhost");
        try {
            c.getResponseCode();
            Check.fail("an unknown certificate must be refused");
        } catch (SSLException expected) {
            Check.isTrue(true, "refused: " + expected);
        }
        c.disconnect();
    }

    /**
     * A trusted certificate for another host name is refused with <code>SSLPeerUnverifiedException</code> unless the verifier of the
     * connection accepts it; the default verifier of the JVM is ignored.
     * <p>
     * 다른 호스트 이름을 위한 신뢰하는 인증서는 연결의 검증기가 받아들이지 않는 한 <code>SSLPeerUnverifiedException</code> 으로 거부한다. JVM 의 기본
     * 검증기는 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpsHostNameMismatch() throws Exception {
        SocketHttpsURLConnection c = https("127.0.0.1");
        c.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
        try {
            c.getResponseCode();
            Check.fail("the certificate is for localhost, not for 127.0.0.1");
        } catch (SSLPeerUnverifiedException expected) {
            Check.isTrue(true, "refused: " + expected.getMessage());
        }
        c.disconnect();
        SocketHttpsURLConnection accepted = new SocketHttpsURLConnection(new URL("https://127.0.0.1:" + secure.getPort() + "/s"));
        accepted.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
        accepted.setHostnameVerifier(new HostnameVerifier() {
            /**
             * Accepts every host name.
             * <p>
             * 모든 호스트 이름을 받아들인다.
             *
             * @param hostname the host name / 호스트 이름
             * @param session  the session / 세션
             * @return true / true
             */
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        Check.equal(200, accepted.getResponseCode(), "a verifier of the connection may accept the certificate");
        accepted.disconnect();
    }

    /**
     * The wildcard rules of {@link HostnameMatcher#nameMatches}: a wildcard stands for exactly one whole left-most label.
     * <p>
     * {@link HostnameMatcher#nameMatches} 의 와일드카드 규칙: 와일드카드는 가장 왼쪽 레이블 하나 전체를 대신한다.
     */
    public void testHostnameWildcardRules() {
        Check.isTrue(HostnameMatcher.nameMatches("www.example.com", "*.example.com"), "one label");
        Check.isFalse(HostnameMatcher.nameMatches("example.com", "*.example.com"), "not the bare domain");
        Check.isFalse(HostnameMatcher.nameMatches("a.b.example.com", "*.example.com"), "not two labels");
        Check.isFalse(HostnameMatcher.nameMatches("www.example.com", "w*.example.com"), "no partial wildcard");
        Check.isFalse(HostnameMatcher.nameMatches("www.example.com", "*.com"), "no wildcard for a top level domain");
        Check.isFalse(HostnameMatcher.nameMatches("www.example.com", "*.*.com"), "one wildcard only");
        Check.isTrue(HostnameMatcher.nameMatches("example.com", "example.com"), "plain name");
        Check.isTrue(HostnameMatcher.nameMatches("example.com.", "example.com"), "trailing dot of the host");
        Check.isFalse(HostnameMatcher.nameMatches("other.com", "example.com"), "other name");
        Check.isFalse(HostnameMatcher.nameMatches("", "*.example.com"), "empty host");
    }

    /**
     * The names of the test certificate: <code>localhost</code> in any case matches, an IP address and other names do not.
     * <p>
     * 시험 인증서의 이름: 대소문자와 관계없이 <code>localhost</code> 는 일치하고 IP 주소와 다른 이름은 일치하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHostnameMatcherWithTestCertificate() throws Exception {
        KeyStore ks = TestSsl.loadKeyStore();
        X509Certificate cert = null;
        Enumeration<String> aliases = ks.aliases();
        // 키 저장소에서 X.509 인증서를 하나 찾는다.
        while (aliases.hasMoreElements() && cert == null) {
            java.security.cert.Certificate c = ks.getCertificate(aliases.nextElement());
            if (c instanceof X509Certificate) {
                cert = (X509Certificate) c;
            }
        }
        Check.notNull(cert, "test certificate");
        Check.isTrue(HostnameMatcher.matches("localhost", cert), "localhost");
        Check.isTrue(HostnameMatcher.matches("LocalHost", cert), "case-insensitive");
        Check.isFalse(HostnameMatcher.matches("127.0.0.1", cert), "an IP address is not the name");
        Check.isFalse(HostnameMatcher.matches("example.com", cert), "other name");
        Check.isFalse(HostnameMatcher.matches(null, cert), "no host");
    }

    /**
     * {@link HttpFetcher} carries <code>Origin</code> on a POST through the socket connection, <code>null</code> for a class path page, and
     * no <code>Origin</code> on a plain GET without an initiating origin.
     * <p>
     * {@link HttpFetcher} 는 POST 의 <code>Origin</code> 을 소켓 연결로 싣고, 클래스패스 페이지에는 <code>null</code> 을 싣고, 시작한 오리진이 없는
     * 일반 GET 에는 <code>Origin</code> 을 싣지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetcherSendsOriginOnPost() throws Exception {
        server.routeFixed("/o", TestHttpServer.Response.ok("text/plain", "o"));
        server.start();
        HttpFetcher fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));
        fetcher.fetch(new ResourceRequest(server.url("/o")).setMethod("POST").setBody(new byte[0], null)
                .setInitiator(Origin.of("http://page.example/")));
        Check.equal("http://page.example", server.last("/o").header("origin"), "Origin on POST");
        fetcher.fetch(new ResourceRequest(server.url("/o")).setMethod("POST").setBody(new byte[0], null)
                .setInitiator(Origin.of("classpath://a/b.html")));
        Check.equal("null", server.last("/o").header("origin"), "classpath initiator is hidden");
        fetcher.fetch(new ResourceRequest(server.url("/o")));
        Check.isNull(server.last("/o").header("origin"), "no Origin without an initiating origin");
        fetcher.fetch(new ResourceRequest(server.url("/o")).setMethod("PUT").setBody("x".getBytes("UTF-8"), "text/plain")
                .setInitiator(Origin.of(server.url("/"))));
        Check.equal(Origin.of(server.url("/")).serialize(), server.last("/o").header("origin"), "Origin of the same origin on PUT");
        Check.equal("x", server.last("/o").bodyText(), "body of the PUT");
    }

    /**
     * Counts the calls of the SSL hook and trusts the test certificate on the connection that it gets.
     * <p>
     * SSL 훅의 호출 수를 세고 받은 연결에 시험 인증서에 대한 신뢰를 설치한다.
     */
    private static final class CountingHook implements SslConfigurer {
        /** The number of calls. / 호출 수. */
        volatile int calls;
        /** The kind of the last connection that was passed. / 마지막으로 받은 연결의 클래스 이름. */
        volatile String lastClass;

        /**
         * Counts the call and installs the test trust on the connection.
         * <p>
         * 호출을 세고 연결에 시험 신뢰를 설치한다.
         *
         * @param connection the https connection / https 연결
         * @throws IOException if the test key store cannot be read / 시험 키 저장소를 읽을 수 없으면
         */
        public void configureSsl(javax.net.ssl.HttpsURLConnection connection) throws IOException {
            calls++;
            lastClass = connection.getClass().getName();
            try {
                connection.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
            } catch (Exception e) {
                throw new IOException(e.toString());
            }
        }
    }

    /**
     * A POST with an initiating origin over https goes through the socket connection: the SSL extension point gets that connection (an
     * <code>HttpsURLConnection</code>), the test trust set there is honoured and the <code>Origin</code> header reaches the server.
     * <p>
     * 시작한 오리진이 있는 https POST 는 소켓 연결을 지난다: SSL 확장 지점이 그 연결(<code>HttpsURLConnection</code>)을 받고, 거기서 정한 시험 신뢰가
     * 적용되며, <code>Origin</code> 헤더가 서버에 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetcherHttpsPostUsesTheSslHook() throws Exception {
        secure = new TestHttpServer(TestSsl.serverContext());
        secure.routeFixed("/p", TestHttpServer.Response.ok("text/plain", "posted"));
        secure.start();
        CountingHook hook = new CountingHook();
        HttpFetcher fetcher = new HttpFetcher(hook, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse r = fetcher.fetch(new ResourceRequest(secure.url("/p")).setMethod("POST").setBody("a=1".getBytes("UTF-8"), null)
                .setInitiator(Origin.of("https://page.example/")));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "the request worked: " + r.getFailureMessage());
        Check.equal("posted", new String(r.getBody(), "UTF-8"), "body");
        Check.equal(1, hook.calls, "the SSL hook ran once");
        Check.equal(SocketHttpsURLConnection.class.getName(), hook.lastClass, "the hook got the socket connection");
        Check.equal("https://page.example", secure.last("/p").header("origin"), "Origin reached the server");
        Check.equal("a=1", secure.last("/p").bodyText(), "body reached the server");
    }

    /**
     * A TLS failure of the socket connection (https to a port that speaks only http) is retried once over plain http, with the
     * <code>Origin</code> header, and a certificate for another host name is a TLS failure.
     * <p>
     * 소켓 연결의 TLS 실패(http 만 쓰는 포트로의 https)는 <code>Origin</code> 헤더와 함께 평문 http 로 한 번 재시도하며, 다른 호스트 이름의 인증서는 TLS
     * 실패이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetcherSocketTlsFailureRetriesOverHttp() throws Exception {
        server.routeFixed("/q", TestHttpServer.Response.ok("text/plain", "plain"));
        server.start();
        CountingHook hook = new CountingHook();
        HttpFetcher fetcher = new HttpFetcher(hook, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse r = fetcher.fetch(new ResourceRequest("https://localhost:" + server.getPort() + "/q").setMethod("POST")
                .setBody("a=1".getBytes("UTF-8"), null).setInitiator(Origin.of("https://page.example/")));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "the retry worked: " + r.getFailureMessage());
        Check.isTrue(r.isHttpFallbackUsed(), "the fallback is reported");
        Check.equal(1, server.count("/q"), "one plain request reached the server");
        Check.equal("https://page.example", server.last("/q").header("origin"), "Origin is kept on the retry");
        Check.equal(1, hook.calls, "the hook ran only for the https attempt");
        secure = new TestHttpServer(TestSsl.serverContext());
        secure.routeFixed("/s", TestHttpServer.Response.ok("text/plain", "secret"));
        secure.start();
        HttpFetcher strict = new HttpFetcher(new CountingHook(), FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        strict.setHttpFallbackEnabled(false);
        ResourceResponse mismatch = strict.fetch(new ResourceRequest("https://127.0.0.1:" + secure.getPort() + "/s").setMethod("POST")
                .setBody("x".getBytes("UTF-8"), null).setInitiator(Origin.of("https://page.example/")));
        Check.equal(FailureKind.TLS, mismatch.getFailureKind(), "a certificate for another host name: " + mismatch.getFailureMessage());
        Check.equal(0, secure.totalRequests(), "nothing was served");
    }

    /**
     * A compressed, chunked answer to a request that goes through the socket connection is decoded by the fetcher like any other, and a
     * redirect of a POST becomes a GET that still reaches the target.
     * <p>
     * 소켓 연결을 지나는 요청의 압축되고 chunked 인 응답은 다른 응답처럼 가져오기 객체가 풀며, POST 의 리디렉션은 목표에 닿는 GET 이 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetcherDecodesAndFollowsRedirectsOverTheSocketConnection() throws Exception {
        String text = "compressed and chunked body that is long enough to be split into several chunks";
        server.routeFixed("/zip", TestHttpServer.Response.ok("text/plain", text).gzip().chunked(9));
        server.routeFixed("/moved", TestHttpServer.Response.redirect(302, "/zip"));
        server.start();
        HttpFetcher fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/moved")).setMethod("POST").setBody("a=1".getBytes("UTF-8"), null)
                .setInitiator(Origin.of("http://page.example/")));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "no failure: " + r.getFailureMessage());
        Check.equal(text, new String(r.getBody(), "UTF-8"), "the body was decompressed");
        Check.equal("GET", server.last("/zip").method, "the redirect of a POST is a GET");
        Check.contains(server.last("/zip").header("accept-encoding"), "gzip", "compression was offered");
        Check.equal(1, r.getRedirects().size(), "one redirect");
    }

    /**
     * <code>PATCH</code> (which the JRE class refuses) reaches the server with its body, also without an initiating origin, and a PATCH to
     * another origin is announced with a preflight that names the method.
     * <p>
     * JRE 클래스가 거부하는 <code>PATCH</code> 가 시작한 오리진이 없어도 본문과 함께 서버에 닿고, 다른 오리진으로의 PATCH 는 메서드를 알리는 사전
     * 요청을 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetcherSendsPatch() throws Exception {
        server.routeFixed("/pt", TestHttpServer.Response.ok("text/plain", "patched"));
        server.start();
        HttpFetcher fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/pt")).setMethod("patch").setBody("a=1".getBytes("UTF-8"), "text/plain"));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "no failure: " + r.getFailureMessage());
        Check.equal("patched", new String(r.getBody(), "UTF-8"), "body of the answer");
        TestHttpServer.Request seen = server.last("/pt");
        Check.equal("PATCH", seen.method, "the method reached the server");
        Check.equal("a=1", seen.bodyText(), "the body reached the server");
        Check.equal("3", seen.header("content-length"), "length of the body");
        ResourceResponse cross = fetcher.fetch(new ResourceRequest(server.url("/pt")).setMethod("PATCH").setBody("b".getBytes("UTF-8"), "text/plain")
                .setPurpose(RequestPurpose.XHR).setInitiator(Origin.of("http://page.example/")).setSendCredentials(false));
        Check.equal(FailureKind.CORS, cross.getFailureKind(), "this server does not grant the preflight");
        Check.contains(cross.getFailureMessage(), "preflight", "the reason names the preflight: " + cross.getFailureMessage());
        Check.equal("OPTIONS", server.last("/pt").method, "only the preflight of the cross-origin PATCH was sent");
        Check.equal("PATCH", server.last("/pt").header("access-control-request-method"), "the preflight names the method");
    }
}
