package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * Tests of the https behaviour of {@link HttpFetcher} (plan items P04-07 to P04-09 and the https part of P04-12): the SSL
 * hook that is called for every https connection, a subclass-style override of a protected <code>configureSsl</code>
 * method, the one-time http retry after a TLS or connect failure, no retry for HTTP error statuses, Secure cookies and
 * sensitive headers on the downgraded request, and the protocol selection of {@link DefaultSslConfigurer}.
 * <p>
 * {@link HttpFetcher} 의 https 동작 시험(계획 P04-07~P04-09 와 P04-12 의 https 부분): 모든 https 연결에 호출되는 SSL 훅,
 * protected <code>configureSsl</code> 메서드를 하위 클래스에서 재정의하는 방식, TLS/연결 실패 뒤의 일회성 http 재시도,
 * HTTP 오류 상태에는 재시도하지 않음, 내려간 요청에서의 Secure 쿠키와 민감한 헤더, {@link DefaultSslConfigurer} 의 프로토콜 선택.
 */
public class HttpsFetcherTest {

    /**
     * Servers started by a case, stopped in {@link #tearDown()}.
     * <p>
     * 사례가 시작한 서버들. {@link #tearDown()} 에서 멈춘다.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HttpsFetcherTest() {
    }

    /**
     * Stops every server.
     * <p>
     * 모든 서버를 멈춘다.
     */
    public void tearDown() {
        // 시작한 서버를 모두 정리한다.
        for (int i = 0; i < servers.size(); i++) {
            servers.get(i).stop();
        }
        servers.clear();
    }

    /**
     * Starts a server (HTTPS when secure) and remembers it for cleanup.
     * <p>
     * 서버를 시작하고(secure 이면 HTTPS) 정리를 위해 기억해 둔다.
     *
     * @param secure true for https / https 이면 true
     * @return the started server / 시작된 서버
     * @throws Exception on failure / 실패 시
     */
    private TestHttpServer start(boolean secure) throws Exception {
        TestHttpServer s = secure ? new TestHttpServer(TestSsl.serverContext()) : new TestHttpServer();
        servers.add(s);
        return s;
    }

    /**
     * An {@link SslConfigurer} that trusts the test certificate and counts its calls.
     * <p>
     * 시험 인증서를 신뢰하고 호출 횟수를 세는 {@link SslConfigurer}.
     */
    private static final class TrustingConfigurer implements SslConfigurer {
        /**
         * Number of calls.
         * <p>
         * 호출 횟수.
         */
        int calls;
        /**
         * Test client TLS context.
         * <p>
         * 시험 클라이언트 TLS 컨텍스트.
         */
        private final SSLContext context;

        /**
         * Creates the configurer.
         * <p>
         * 설정기를 만든다.
         *
         * @throws Exception when the test key store cannot be read / 시험 키 저장소를 읽을 수 없을 때
         */
        TrustingConfigurer() throws Exception {
            this.context = TestSsl.clientContext();
        }

        /**
         * Counts the call and installs the test trust on this connection only.
         * <p>
         * 호출을 세고 이 연결에만 시험 신뢰를 설치한다.
         *
         * @param connection https connection / https 연결
         */
        public synchronized void configureSsl(HttpsURLConnection connection) {
            calls++;
            connection.setSSLSocketFactory(context.getSocketFactory());
        }
    }

    /**
     * A stand-in for the public component: it has the <code>protected configureSsl</code> method that hosts override, and it
     * adapts it to {@link SslConfigurer} the way SwingWebPanel will.
     * <p>
     * 공개 컴포넌트의 대역: 호스트가 재정의하는 <code>protected configureSsl</code> 메서드를 가지며 SwingWebPanel 이 할 방식으로
     * {@link SslConfigurer} 에 연결한다.
     */
    private static class PanelStub {
        /**
         * The overridable SSL method (default: JRE trust and host name verification).
         * <p>
         * 재정의할 수 있는 SSL 메서드(기본: JRE 신뢰와 호스트 이름 검증).
         *
         * @param connection https connection / https 연결
         * @throws IOException on failure / 실패 시
         */
        protected void configureSsl(HttpsURLConnection connection) throws IOException {
            DefaultSslConfigurer.INSTANCE.configureSsl(connection);
        }

        /**
         * Adapts the protected method to the fetch layer.
         * <p>
         * protected 메서드를 가져오기 계층에 연결한다.
         *
         * @return the adapter / 연결 객체
         */
        SslConfigurer asConfigurer() {
            return new SslConfigurer() {
                /**
                 * Delegates to the (possibly overridden) protected method.
                 * <p>
                 * (재정의되었을 수 있는) protected 메서드에 위임한다.
                 *
                 * @param connection https connection / https 연결
                 * @throws IOException on failure / 실패 시
                 */
                public void configureSsl(HttpsURLConnection connection) throws IOException {
                    PanelStub.this.configureSsl(connection);
                }
            };
        }
    }

    /**
     * https works with the test trust and the SSL hook runs once per connection, including redirect hops and sub-resources.
     * <p>
     * 시험 신뢰로 https 가 동작하고 SSL 훅은 리디렉션 단계와 하위 리소스를 포함해 연결마다 한 번씩 호출된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHookRunsForEveryHttpsConnection() throws Exception {
        TestHttpServer s = start(true);
        s.routeFixed("/page", TestHttpServer.Response.html("secure page"));
        s.routeFixed("/old", TestHttpServer.Response.redirect(302, "/page"));
        s.start();
        TrustingConfigurer cfg = new TrustingConfigurer();
        HttpFetcher f = new HttpFetcher(cfg, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse r = f.fetch(new ResourceRequest(s.url("/page")));
        Check.equal(200, r.getStatus(), "https status");
        Check.equal("secure page", new String(r.getBody(), "UTF-8"), "https body");
        Check.isFalse(r.isHttpFallbackUsed(), "no fallback on success");
        Check.equal(1, cfg.calls, "hook called for the main document");
        f.fetch(new ResourceRequest(s.url("/page")).setPurpose(RequestPurpose.IMAGE));
        Check.equal(2, cfg.calls, "hook called for a sub-resource");
        ResourceResponse red = f.fetch(new ResourceRequest(s.url("/old")));
        Check.equal(4, cfg.calls, "hook called for both hops of a redirect");
        Check.equal(s.url("/page"), red.getFinalUrl(), "final url after an https redirect");
    }

    /**
     * Hosts override the protected method in a subclass; the override is used for every https request and nothing global changes.
     * <p>
     * 호스트가 하위 클래스에서 protected 메서드를 재정의하면 재정의가 모든 https 요청에 쓰이며 전역 상태는 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSubclassOverrideOfProtectedHook() throws Exception {
        TestHttpServer s = start(true);
        s.routeFixed("/page", TestHttpServer.Response.html("x"));
        s.start();
        final int[] calls = new int[1];
        final SSLContext ctx = TestSsl.clientContext();
        PanelStub custom = new PanelStub() {
            /**
             * Test override: trusts the test certificate and counts.
             * <p>
             * 시험용 재정의: 시험 인증서를 신뢰하고 횟수를 센다.
             *
             * @param connection https connection / https 연결
             */
            protected void configureSsl(HttpsURLConnection connection) {
                calls[0]++;
                connection.setSSLSocketFactory(ctx.getSocketFactory());
            }
        };
        javax.net.ssl.SSLSocketFactory globalBefore = HttpsURLConnection.getDefaultSSLSocketFactory();
        javax.net.ssl.HostnameVerifier verifierBefore = HttpsURLConnection.getDefaultHostnameVerifier();
        HttpFetcher f = new HttpFetcher(custom.asConfigurer(), FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        Check.equal(200, f.fetch(new ResourceRequest(s.url("/page"))).getStatus(), "override gives a working connection");
        Check.equal(1, calls[0], "override called");
        // 재정의하지 않은 기본 구현(JRE 신뢰 저장소)은 시험 인증서를 거부하므로 기본 신뢰가 바뀌지 않았음을 알 수 있다.
        HttpFetcher plain = new HttpFetcher(new PanelStub().asConfigurer(), FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        plain.setHttpFallbackEnabled(false);
        ResourceResponse untrusted = plain.fetch(new ResourceRequest(s.url("/page")));
        Check.equal(FailureKind.TLS, untrusted.getFailureKind(), "default implementation keeps the JRE trust store");
        Check.same(globalBefore, HttpsURLConnection.getDefaultSSLSocketFactory(), "global socket factory unchanged");
        Check.same(verifierBefore, HttpsURLConnection.getDefaultHostnameVerifier(), "global host name verifier unchanged");
    }

    /**
     * https to a port that only speaks http fails at TLS level; the fetcher retries once over http with the same port.
     * <p>
     * http 만 쓰는 포트에 https 로 접속하면 TLS 수준에서 실패하고, 가져오기 객체가 같은 포트로 http 재시도를 한 번 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTlsFailureRetriesOverHttp() throws Exception {
        TestHttpServer plain = start(false);
        plain.routeFixed("/page", TestHttpServer.Response.html("plain page"));
        plain.start();
        TrustingConfigurer cfg = new TrustingConfigurer();
        HttpFetcher f = new HttpFetcher(cfg, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse r = f.fetch(new ResourceRequest("https://localhost:" + plain.getPort() + "/page#top"));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "the retry succeeded");
        Check.equal(200, r.getStatus(), "status over http");
        Check.equal("plain page", new String(r.getBody(), "UTF-8"), "body over http");
        Check.isTrue(r.isHttpFallbackUsed(), "fallback is reported");
        Check.equal("http://localhost:" + plain.getPort() + "/page#top", r.getFinalUrl(), "the real http url is reported");
        Check.equal(1, plain.count("/page"), "exactly one http request reached the server");
        Check.equal(1, cfg.calls, "the hook ran only for the https attempt");
    }

    /**
     * Port rules of the http retry: 443 and no port become the http default, other explicit ports stay.
     * <p>
     * http 재시도의 포트 규칙: 443 과 포트 없음은 http 기본값이 되고 다른 명시 포트는 유지한다.
     */
    public void testFallbackPortRules() {
        Check.equal("http://h/x", HttpFetcher.toHttp("https://h/x"), "no port");
        Check.equal("http://h/x", HttpFetcher.toHttp("https://h:443/x"), "explicit 443 becomes the http default");
        Check.equal("http://h:8443/x?q=1#f", HttpFetcher.toHttp("https://h:8443/x?q=1#f"), "other ports stay");
        Check.equal("http://h:80/x", HttpFetcher.toHttp("https://h:80/x"), "an explicit port 80 stays explicit");
    }

    /**
     * An untrusted certificate is a TLS failure; with the retry enabled the plain http request to the TLS-only port fails
     * too and the result stays a failure (no loop, nothing reaches the application layer of the server).
     * <p>
     * 신뢰할 수 없는 인증서는 TLS 실패이다. 재시도를 켜면 TLS 전용 포트로의 평문 http 요청도 실패하여 결과는 실패로 남는다
     * (순환 없음, 서버의 응용 계층에는 아무것도 도달하지 않음).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUntrustedCertificateAndNoLoop() throws Exception {
        TestHttpServer s = start(true);
        s.routeFixed("/page", TestHttpServer.Response.html("secret"));
        s.start();
        HttpFetcher noRetry = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        noRetry.setHttpFallbackEnabled(false);
        ResourceResponse r = noRetry.fetch(new ResourceRequest(s.url("/page")));
        Check.equal(FailureKind.TLS, r.getFailureKind(), "untrusted certificate");
        Check.isFalse(r.isHttpFallbackUsed(), "no retry when disabled");
        HttpFetcher retry = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse r2 = retry.fetch(new ResourceRequest(s.url("/page")));
        Check.isTrue(r2.getFailureKind() != FailureKind.NONE, "the plain request to a TLS port fails as well");
        Check.isTrue(r2.isHttpFallbackUsed(), "the retry was attempted exactly once");
        Check.equal(0, s.totalRequests(), "no request was ever served");
    }

    /**
     * An HTTP error status over a working TLS connection is not a TLS failure and is not retried over http.
     * <p>
     * 정상적인 TLS 연결 위의 HTTP 오류 상태는 TLS 실패가 아니며 http 로 재시도하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHttpErrorStatusIsNotRetried() throws Exception {
        TestHttpServer s = start(true);
        s.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "gone"));
        s.routeFixed("/boom", new TestHttpServer.Response(500).text("text/plain", "bad"));
        s.start();
        TrustingConfigurer cfg = new TrustingConfigurer();
        HttpFetcher f = new HttpFetcher(cfg, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        ResourceResponse a = f.fetch(new ResourceRequest(s.url("/missing")));
        ResourceResponse b = f.fetch(new ResourceRequest(s.url("/boom")));
        Check.equal(404, a.getStatus(), "404 over https");
        Check.equal(500, b.getStatus(), "500 over https");
        Check.isFalse(a.isHttpFallbackUsed() || b.isHttpFallbackUsed(), "no http retry for http errors");
        Check.equal("gone", new String(a.getBody(), "UTF-8"), "error body kept");
        Check.equal(2, s.totalRequests(), "one request each");
    }

    /**
     * After a downgrade, Secure cookies and Authorization-style headers are not sent, ordinary cookies and headers are.
     * <p>
     * 내려간 뒤에는 Secure 쿠키와 인증 계열 헤더를 보내지 않고, 일반 쿠키와 헤더는 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDowngradeDropsSecureCookiesAndAuthorization() throws Exception {
        TestHttpServer secure = start(true);
        secure.routeFixed("/set", TestHttpServer.Response.html("cookies").header("Set-Cookie", "s=1; Path=/; Secure")
                .header("Set-Cookie", "p=2; Path=/"));
        secure.start();
        TestHttpServer plain = start(false);
        plain.routeFixed("/x", TestHttpServer.Response.html("plain"));
        plain.start();
        TrustingConfigurer cfg = new TrustingConfigurer();
        HttpFetcher f = new HttpFetcher(cfg, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        CookieStore store = new CookieStore();
        f.fetch(new ResourceRequest(secure.url("/set")).setCookies(store));
        Check.equal("p=2; s=1", store.getCookies(), "both cookies were stored over https");
        RequestHeaders extra = new RequestHeaders();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("Authorization", "Bearer t");
        m.put("X-Keep", "yes");
        extra.addAll(m);
        ResourceResponse r = f.fetch(new ResourceRequest("https://localhost:" + plain.getPort() + "/x")
                .setCookies(store).setExtraHeaders(extra));
        Check.isTrue(r.isHttpFallbackUsed(), "downgraded");
        TestHttpServer.Request q = plain.last("/x");
        Check.equal("p=2", q.header("cookie"), "the Secure cookie is not sent over plain http");
        Check.isNull(q.header("authorization"), "Authorization is dropped on the plain request");
        Check.equal("yes", q.header("x-keep"), "ordinary headers are kept");
    }

    /**
     * An exception from the hook fails the request as a TLS failure.
     * <p>
     * 훅이 던진 예외는 요청을 TLS 실패로 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHookFailureIsTlsFailure() throws Exception {
        TestHttpServer s = start(true);
        s.routeFixed("/p", TestHttpServer.Response.html("x"));
        s.start();
        SslConfigurer broken = new SslConfigurer() {
            /**
             * Always fails.
             * <p>
             * 항상 실패한다.
             *
             * @param connection connection / 연결
             * @throws IOException always / 항상
             */
            public void configureSsl(HttpsURLConnection connection) throws IOException {
                throw new IOException("no keys");
            }
        };
        HttpFetcher f = new HttpFetcher(broken, FetchLimits.DEFAULT.withTimeouts(8000, 8000));
        f.setHttpFallbackEnabled(false);
        ResourceResponse r = f.fetch(new ResourceRequest(s.url("/p")));
        Check.equal(FailureKind.TLS, r.getFailureKind(), "hook failure");
        Check.contains(r.getFailureMessage(), "no keys", "message is kept");
        Check.equal(0, s.totalRequests(), "nothing was sent");
    }

    /**
     * Protocol selection of the default configurer: newest supported TLS versions first, never SSLv3.
     * <p>
     * 기본 설정기의 프로토콜 선택: 지원되는 최신 TLS 버전 먼저, SSLv3 는 절대 켜지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultProtocolSelection() throws Exception {
        Check.equal("[TLSv1]", java.util.Arrays.toString(DefaultSslConfigurer.select(new String[] { "SSLv2Hello", "SSLv3", "TLSv1" })),
                "a JRE 6 style protocol list");
        Check.equal("[TLSv1.2, TLSv1.1, TLSv1]",
                java.util.Arrays.toString(DefaultSslConfigurer.select(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3" })), "JRE 7/8 style list");
        Check.equal("[TLSv1.3, TLSv1.2]",
                java.util.Arrays.toString(DefaultSslConfigurer.select(new String[] { "TLSv1.2", "TLSv1.3" })), "newest first");
        Check.isNull(DefaultSslConfigurer.select(new String[] { "SSLv3" }), "nothing acceptable");
        javax.net.ssl.SSLSocketFactory sf = SSLContext.getDefault().getSocketFactory();
        javax.net.ssl.SSLSocket sock = (javax.net.ssl.SSLSocket) sf.createSocket();
        try {
            System.out.println("INFO java.version=" + System.getProperty("java.version")
                    + " supported TLS protocols=" + java.util.Arrays.toString(sock.getSupportedProtocols())
                    + " default enabled=" + java.util.Arrays.toString(sock.getEnabledProtocols())
                    + " selected=" + java.util.Arrays.toString(DefaultSslConfigurer.select(sock.getSupportedProtocols())));
        } finally {
            sock.close();
        }
    }
}
