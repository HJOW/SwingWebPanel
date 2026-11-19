package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of {@link HttpFetcher} over plain HTTP with the local {@link TestHttpServer} (plan items P04-03 to P04-06 and
 * P04-12): status codes, redirects, headers, cookies, compression, limits, streaming, truncation, timeouts, cancellation and
 * scheme blocking. The https specific behaviour is covered by {@link HttpsFetcherTest}.
 * <p>
 * 로컬 {@link TestHttpServer} 로 평문 HTTP 위에서 {@link HttpFetcher} 를 시험한다(계획 P04-03~P04-06, P04-12): 상태 코드, 리디렉션,
 * 헤더, 쿠키, 압축, 제한, 스트리밍, 잘림, 시간 초과, 취소, 스킴 차단. https 전용 동작은 {@link HttpsFetcherTest} 가 다룬다.
 */
public class HttpFetcherTest {

    /**
     * First test server.
     * <p>
     * 첫 번째 시험 서버.
     */
    private TestHttpServer server;
    /**
     * Second test server (another origin).
     * <p>
     * 두 번째 시험 서버(다른 오리진).
     */
    private TestHttpServer other;
    /**
     * The fetcher with default limits.
     * <p>
     * 기본 제한을 쓰는 가져오기 객체.
     */
    private final HttpFetcher fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HttpFetcherTest() {
    }

    /**
     * Starts the first server.
     * <p>
     * 첫 번째 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
    }

    /**
     * Stops both servers.
     * <p>
     * 두 서버를 멈춘다.
     */
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (other != null) {
            other.stop();
            other = null;
        }
    }

    /**
     * Starts the first server and returns a GET request for a path.
     * <p>
     * 첫 번째 서버를 시작하고 경로에 대한 GET 요청을 돌려준다.
     *
     * @param path path / 경로
     * @return the request / 요청
     * @throws IOException on failure / 실패 시
     */
    private ResourceRequest start(String path) throws IOException {
        server.start();
        return new ResourceRequest(server.url(path));
    }

    /**
     * Returns the body as UTF-8 text.
     * <p>
     * 본문을 UTF-8 문자열로 돌려준다.
     *
     * @param r response / 응답
     * @return text / 문자열
     * @throws IOException never in practice / 실제로는 발생하지 않음
     */
    private static String text(ResourceResponse r) throws IOException {
        return new String(r.getBody(), "UTF-8");
    }

    /**
     * 200, 404 and 500 responses keep status, headers, media type, charset and body (also for error statuses).
     * <p>
     * 200, 404, 500 응답이 상태, 헤더, 미디어 타입, 문자셋, 본문을 유지한다(오류 상태도 마찬가지).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStatusCodesAndBodies() throws Exception {
        server.routeFixed("/ok", TestHttpServer.Response.html("<p>안녕</p>"));
        server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/html; charset=UTF-8", "<h1>nope</h1>"));
        server.routeFixed("/boom", new TestHttpServer.Response(500).text("text/plain", "server broke").header("X-Why", "test"));
        ResourceResponse ok = fetcher.fetch(start("/ok"));
        Check.equal(200, ok.getStatus(), "status");
        Check.equal(FailureKind.NONE, ok.getFailureKind(), "no failure");
        Check.equal("text/html", ok.getMimeType(), "mime");
        Check.equal("UTF-8", ok.getCharset(), "charset");
        Check.equal("<p>안녕</p>", text(ok), "body");
        Check.isFalse(ok.isError(), "not an error");
        Check.equal(server.url("/ok"), ok.getFinalUrl(), "final url");
        ResourceResponse missing = fetcher.fetch(new ResourceRequest(server.url("/missing")));
        Check.equal(404, missing.getStatus(), "404 status");
        Check.equal("<h1>nope</h1>", text(missing), "404 body is kept");
        Check.isTrue(missing.isHttpError() && missing.isError(), "404 is an http error");
        Check.equal(FailureKind.NONE, missing.getFailureKind(), "an http error is not a transport failure");
        ResourceResponse boom = fetcher.fetch(new ResourceRequest(server.url("/boom")));
        Check.equal(500, boom.getStatus(), "500 status");
        Check.equal("server broke", text(boom), "500 body is kept");
        Check.equal("test", boom.getHeader("x-why"), "header lookup is case-insensitive");
        ResourceResponse unknown = fetcher.fetch(new ResourceRequest(server.url("/not-registered")));
        Check.equal(404, unknown.getStatus(), "unregistered route");
    }

    /**
     * HEAD returns headers without a body and is not reported as truncated.
     * <p>
     * HEAD 는 본문 없이 헤더만 돌려주며 잘림으로 보고되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeadHasNoBody() throws Exception {
        server.routeFixed("/h", TestHttpServer.Response.ok("text/plain", "twelve bytes"));
        ResourceResponse r = fetcher.fetch(start("/h").setMethod("HEAD"));
        Check.equal(200, r.getStatus(), "status");
        Check.equal(0, r.getBody().length, "no body");
        Check.equal(FailureKind.NONE, r.getFailureKind(), "not truncated");
        Check.equal(12, r.getDeclaredLength(), "declared length is reported");
        Check.equal("HEAD", server.last("/h").method, "method on the wire");
    }

    /**
     * The User-Agent and the extra headers go out with every purpose; a User-Agent in the extra headers is ignored;
     * overwriting is case-insensitive; managed headers cannot be set.
     * <p>
     * User-Agent 와 추가 헤더가 모든 목적의 요청에 붙는다. 추가 헤더의 User-Agent 는 무시된다. 덮어쓰기는 대소문자를 구분하지 않고
     * 관리 대상 헤더는 지정할 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeadersOnEveryRequestKind() throws Exception {
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
        RequestHeaders extra = new RequestHeaders();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("X-Test", "first");
        m.put("x-test", "second");
        m.put("User-Agent", "evil/9");
        m.put("Cookie", "forged=1");
        m.put("Accept-Encoding", "br");
        extra.addAll(m);
        RequestPurpose[] purposes = RequestPurpose.values();
        // 모든 요청 목적(주 문서, 이미지, 오디오, CSS, 스크립트 등)에서 같은 헤더가 나가는지 확인한다.
        for (int i = 0; i < purposes.length; i++) {
            // 목적마다 새 서버를 써서 마지막 요청이 확실히 그 목적의 요청이 되게 한다.
            server.stop();
            server = new TestHttpServer();
            server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
            server.start();
            ResourceRequest req = new ResourceRequest(server.url("/x")).setUserAgent("Custom/2.0")
                    .setExtraHeaders(extra).setPurpose(purposes[i]);
            ResourceResponse r = fetcher.fetch(req);
            Check.equal(200, r.getStatus(), "status for " + purposes[i]);
            TestHttpServer.Request q = server.last("/x");
            Check.equal("Custom/2.0", q.header("user-agent"), "user agent for " + purposes[i]);
            Check.equal("second", q.header("x-test"), "overwritten extra header for " + purposes[i]);
            Check.equal("gzip, deflate", q.header("accept-encoding"), "managed Accept-Encoding for " + purposes[i]);
            Check.isNull(q.header("cookie"), "a Cookie extra header is not forwarded for " + purposes[i]);
            Check.notNull(q.header("accept"), "Accept for " + purposes[i]);
        }
    }

    /**
     * The default User-Agent is swingwebpanel/1.0 and the Accept header depends on the purpose.
     * <p>
     * 기본 User-Agent 는 swingwebpanel/1.0 이고 Accept 헤더는 목적에 따라 달라진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultUserAgentAndAccept() throws Exception {
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
        fetcher.fetch(start("/x").setPurpose(RequestPurpose.IMAGE));
        Check.equal("swingwebpanel/1.0", server.last("/x").header("user-agent"), "default user agent");
        Check.contains(server.last("/x").header("accept"), "image/*", "image accept");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setPurpose(RequestPurpose.STYLESHEET));
        Check.contains(server.last("/x").header("accept"), "text/css", "stylesheet accept");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setPurpose(RequestPurpose.DOCUMENT).setUserAgent(""));
        Check.contains(server.last("/x").header("accept"), "text/html", "document accept");
        Check.equal("swingwebpanel/1.0", server.last("/x").header("user-agent"), "empty user agent restores the default");
    }

    /**
     * Referer (http only, no fragment, never https to http), Origin for POST and cache bypass headers.
     * <p>
     * Referer(http 만, 프래그먼트 없음, https 에서 http 로는 안 보냄), POST 의 Origin, 캐시 우회 헤더.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefererOriginAndCacheHeaders() throws Exception {
        server.routeFixed("/x", TestHttpServer.Response.ok("text/plain", "x"));
        fetcher.fetch(start("/x").setReferrer("http://page.example/a/b?q=1#frag"));
        Check.equal("http://page.example/a/b?q=1", server.last("/x").header("referer"), "referer without fragment");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setReferrer("classpath://local/page.html"));
        Check.isNull(server.last("/x").header("referer"), "a classpath referrer is never sent");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setReferrer("https://secure.example/p"));
        Check.isNull(server.last("/x").header("referer"), "https to http sends no referer");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setMethod("POST").setBody(new byte[0], null)
                .setInitiator(Origin.of("http://page.example/")));
        String originOnPost = server.last("/x").header("origin");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setMethod("POST").setBody(new byte[0], null)
                .setInitiator(Origin.of("classpath://a/b.html")));
        String originFromClasspath = server.last("/x").header("origin");
        fetcher.fetch(new ResourceRequest(server.url("/x")).setCacheMode(ResourceRequest.CacheMode.FORCE_RELOAD));
        Check.equal("no-cache", server.last("/x").header("cache-control"), "force reload");
        Check.equal("no-cache", server.last("/x").header("pragma"), "force reload pragma");
        fetcher.fetch(new ResourceRequest(server.url("/x")));
        Check.isNull(server.last("/x").header("cache-control"), "normal mode sends no cache header");
        // JRE 의 HttpURLConnection 은 Origin 을 제한 헤더로 보고 조용히 버리므로 Origin 이 필요한 요청은 소켓 기반 연결(SocketHttpURLConnection)로 보낸다.
        Check.equal("http://page.example", originOnPost, "Origin on POST");
        Check.equal("null", originFromClasspath, "classpath initiator is hidden");
    }

    /**
     * Redirects: relative and absolute Location, chain, final URL, redirect list, fragment inheritance.
     * <p>
     * 리디렉션: 상대/절대 Location, 연쇄, 최종 URL, 리디렉션 목록, 프래그먼트 상속.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectsAndFinalUrl() throws Exception {
        server.routeFixed("/a/start", TestHttpServer.Response.redirect(302, "next?x=1"));
        server.routeFixed("/a/next", TestHttpServer.Response.redirect(301, "../b/final"));
        server.routeFixed("/b/final", TestHttpServer.Response.html("done"));
        server.start();
        ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/a/start#section")));
        Check.equal(200, r.getStatus(), "final status");
        Check.equal(server.url("/b/final") + "#section", r.getFinalUrl(), "final url keeps the fragment");
        Check.equal(2, r.getRedirects().size(), "two hops");
        Check.equal(server.url("/a/start"), r.getRedirects().get(0), "first hop");
        Check.equal("done", text(r), "body of the final page");
        Check.equal(1, server.count("/a/next"), "each hop requested once");
        Check.equal("x=1", server.last("/a/next").query, "query of a relative Location");
    }

    /**
     * POST becomes GET after 302/303 (body dropped); 307 and 308 keep method and body.
     * <p>
     * 302/303 뒤에 POST 는 GET 이 되고(본문 제거) 307/308 은 메서드와 본문을 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectMethodHandling() throws Exception {
        server.routeFixed("/p302", TestHttpServer.Response.redirect(302, "/t302"));
        server.routeFixed("/p303", TestHttpServer.Response.redirect(303, "/t303"));
        server.routeFixed("/p307", TestHttpServer.Response.redirect(307, "/t307"));
        server.routeFixed("/p308", TestHttpServer.Response.redirect(308, "/t308"));
        server.routeFixed("/t*", TestHttpServer.Response.ok("text/plain", "target"));
        server.start();
        byte[] data = "a=1&b=%ED%95%9C".getBytes("ISO-8859-1");
        // 각 리디렉션 코드마다 같은 POST 본문으로 요청하고 도착한 메서드/본문을 확인한다.
        String[] codes = { "302", "303", "307", "308" };
        for (int i = 0; i < codes.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/p" + codes[i])).setMethod("POST").setBody(data, null));
            Check.equal(200, r.getStatus(), "status after " + codes[i]);
            TestHttpServer.Request first = server.last("/p" + codes[i]);
            Check.equal("POST", first.method, "first request is POST");
            Check.equal("application/x-www-form-urlencoded", first.header("content-type"), "default content type");
            Check.equal("a=1&b=%ED%95%9C", first.bodyText(), "post body arrives");
            TestHttpServer.Request second = server.last("/t" + codes[i]);
            boolean keep = "307".equals(codes[i]) || "308".equals(codes[i]);
            Check.equal(keep ? "POST" : "GET", second.method, "method after " + codes[i]);
            Check.equal(keep ? "a=1&b=%ED%95%9C" : "", second.bodyText(), "body after " + codes[i]);
        }
    }

    /**
     * Redirect loops and too many redirects are detected.
     * <p>
     * 리디렉션 순환과 과다 리디렉션을 감지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectLoopAndLimit() throws Exception {
        server.routeFixed("/loopA", TestHttpServer.Response.redirect(302, "/loopB"));
        server.routeFixed("/loopB", TestHttpServer.Response.redirect(302, "/loopA"));
        server.route("/chain/*", new TestHttpServer.Handler() {
            /**
             * Redirects /chain/N to /chain/N+1 forever.
             * <p>
             * /chain/N 을 /chain/N+1 로 끝없이 리디렉션한다.
             *
             * @param request request / 요청
             * @return redirect / 리디렉션
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                int n = Integer.parseInt(request.path.substring("/chain/".length()));
                return TestHttpServer.Response.redirect(302, "/chain/" + (n + 1));
            }
        });
        server.start();
        ResourceResponse loop = fetcher.fetch(new ResourceRequest(server.url("/loopA")));
        Check.equal(FailureKind.REDIRECT_LOOP, loop.getFailureKind(), "loop");
        Check.equal(2, server.count("/loopA") + server.count("/loopB"), "the loop is detected on the first repeat");
        HttpFetcher limited = new HttpFetcher(null, FetchLimits.DEFAULT.withMaxRedirects(5));
        ResourceResponse many = limited.fetch(new ResourceRequest(server.url("/chain/0")));
        Check.equal(FailureKind.TOO_MANY_REDIRECTS, many.getFailureKind(), "limit");
        Check.equal(6, server.requests().size() - 2, "five redirects followed, the sixth refused");
    }

    /**
     * Redirects to forbidden schemes are refused without contacting anything; http may only redirect to http(s).
     * <p>
     * 금지된 스킴으로의 리디렉션은 아무 곳에도 연결하지 않고 거부한다. http 는 http(s) 로만 리디렉션할 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectToForbiddenSchemes() throws Exception {
        String[] targets = { "file:///etc/passwd", "javascript:alert(1)", "ftp://localhost/x", "classpath://secret.html",
            "data:text/html,hi", "mailto:a@b.c", "about:blank" };
        // 위험한 스킴마다 리디렉션 서버를 하나 등록한다.
        for (int i = 0; i < targets.length; i++) {
            server.routeFixed("/r" + i, TestHttpServer.Response.redirect(302, targets[i]));
        }
        server.start();
        // 서버를 시작한 뒤 각 리디렉션 대상이 차단되는지 확인한다.
        for (int i = 0; i < targets.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url("/r" + i)));
            Check.equal(FailureKind.BLOCKED_SCHEME, r.getFailureKind(), "redirect to " + targets[i]);
            Check.equal(0, r.getBody().length, "no body for " + targets[i]);
        }
        Check.equal(targets.length, server.totalRequests(), "only the redirecting requests reached the server");
    }

    /**
     * Forbidden or invalid URLs never open a connection.
     * <p>
     * 금지되었거나 잘못된 URL 은 연결을 열지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedAndInvalidUrls() throws Exception {
        server.start();
        String[] blocked = { "file:///c:/windows/win.ini", "javascript:alert(1)", "mailto:a@b.c", "data:text/plain,x",
            "jar:file:/x.jar!/a", "ws://localhost:" + server.getPort() + "/", "ftp://localhost:1/" };
        // 금지 스킴(그리고 이 클래스가 다루지 않는 ftp)은 모두 거부되어야 한다.
        for (int i = 0; i < blocked.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(blocked[i]));
            Check.equal(FailureKind.BLOCKED_SCHEME, r.getFailureKind(), "blocked: " + blocked[i]);
        }
        String[] invalid = { null, "", "relative/path.html", "http://", "http://h:99999/" };
        // 잘못된 주소는 모두 INVALID_URL 이어야 한다.
        for (int i = 0; i < invalid.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(invalid[i]));
            Check.equal(FailureKind.INVALID_URL, r.getFailureKind(), "invalid: " + invalid[i]);
        }
        Check.equal(0, server.totalRequests(), "the server saw nothing");
    }

    /**
     * Cookies: stored from responses (also from redirect hops), sent back with the right scoping, per component store.
     * <p>
     * 쿠키: 응답(리디렉션 단계 포함)에서 저장하고 올바른 범위로 다시 보내며 컴포넌트별 저장소를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCookiesAreStoredAndSent() throws Exception {
        server.routeFixed("/login", TestHttpServer.Response.redirect(302, "/home").header("Set-Cookie", "sid=abc; Path=/; HttpOnly")
                .header("Set-Cookie", "theme=dark; Path=/home"));
        server.routeFixed("/home", TestHttpServer.Response.ok("text/plain", "home"));
        server.routeFixed("/other", TestHttpServer.Response.ok("text/plain", "other"));
        server.start();
        CookieStore store = new CookieStore();
        fetcher.fetch(new ResourceRequest(server.url("/login")).setCookies(store));
        Check.equal("theme=dark; sid=abc", server.last("/home").header("cookie"), "cookies set by the redirect arrive on the next hop, longer path first");
        fetcher.fetch(new ResourceRequest(server.url("/other")).setCookies(store));
        Check.equal("sid=abc", server.last("/other").header("cookie"), "path scoping");
        Check.equal("sid=abc; theme=dark", store.getCookies(), "getCookies of the component store");
        CookieStore second = new CookieStore();
        fetcher.fetch(new ResourceRequest(server.url("/other")).setCookies(second));
        Check.isNull(server.last("/other").header("cookie"), "another component does not share cookies");
        fetcher.fetch(new ResourceRequest(server.url("/other")).setCookies(store).setSendCredentials(false));
        Check.isNull(server.last("/other").header("cookie"), "credentials off sends no cookies");
        Check.equal("theme=dark", store.getDocumentCookie(server.url("/home")), "HttpOnly cookie is hidden from scripts");
    }

    /**
     * Auth style headers are forwarded to same-origin redirect targets but not to other origins; other headers are.
     * <p>
     * 인증 계열 헤더는 같은 오리진의 리디렉션 대상에는 전달하고 다른 오리진에는 전달하지 않는다. 다른 헤더는 전달한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAuthorizationNotForwardedAcrossOrigins() throws Exception {
        other = new TestHttpServer();
        other.routeFixed("/landing", TestHttpServer.Response.ok("text/plain", "other origin"));
        other.start();
        server.routeFixed("/same", TestHttpServer.Response.redirect(302, "/same-target"));
        server.routeFixed("/same-target", TestHttpServer.Response.ok("text/plain", "ok"));
        server.routeFixed("/cross", TestHttpServer.Response.redirect(302, other.url("/landing")));
        server.start();
        RequestHeaders extra = new RequestHeaders();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("Authorization", "Bearer secret");
        m.put("Proxy-Authorization", "Basic xyz");
        m.put("X-Keep", "kept");
        extra.addAll(m);
        fetcher.fetch(new ResourceRequest(server.url("/same")).setExtraHeaders(extra));
        Check.equal("Bearer secret", server.last("/same-target").header("authorization"), "same origin keeps Authorization");
        fetcher.fetch(new ResourceRequest(server.url("/cross")).setExtraHeaders(extra));
        Check.equal("Bearer secret", server.last("/cross").header("authorization"), "first hop has Authorization");
        TestHttpServer.Request landing = other.last("/landing");
        Check.isNull(landing.header("authorization"), "Authorization is not sent to another origin");
        Check.isNull(landing.header("proxy-authorization"), "Proxy-Authorization is not sent to another origin");
        Check.equal("kept", landing.header("x-keep"), "ordinary extra headers are forwarded");
    }

    /**
     * gzip, zlib deflate and raw deflate bodies are decoded; unsupported encodings are failures.
     * <p>
     * gzip, zlib deflate, raw deflate 본문을 해독하고 지원하지 않는 인코딩은 실패로 처리한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompressionDecoding() throws Exception {
        String t = "compress me, 압축 compress me, 압축";
        server.routeFixed("/gz", TestHttpServer.Response.ok("text/plain; charset=UTF-8", t).gzip());
        server.routeFixed("/zlib", TestHttpServer.Response.ok("text/plain; charset=UTF-8", t).deflate(true));
        server.routeFixed("/raw", TestHttpServer.Response.ok("text/plain; charset=UTF-8", t).deflate(false));
        server.routeFixed("/gzchunk", TestHttpServer.Response.ok("text/plain; charset=UTF-8", t).gzip().chunked(3));
        server.routeFixed("/br", TestHttpServer.Response.ok("text/plain", "x").header("Content-Encoding", "br"));
        server.routeFixed("/emptygz", new TestHttpServer.Response(200).header("Content-Encoding", "gzip"));
        server.routeFixed("/badgz", TestHttpServer.Response.ok("text/plain", "this is not gzip").header("Content-Encoding", "gzip"));
        server.start();
        String[] ok = { "/gz", "/zlib", "/raw", "/gzchunk" };
        // 압축 방식마다 원문과 같게 풀리는지 확인한다.
        for (int i = 0; i < ok.length; i++) {
            ResourceResponse r = fetcher.fetch(new ResourceRequest(server.url(ok[i])));
            Check.equal(t, text(r), "decoded body of " + ok[i]);
            Check.equal("gzip, deflate", server.last(ok[i]).header("accept-encoding"), "only decodable encodings are advertised");
        }
        Check.equal(FailureKind.IO, fetcher.fetch(new ResourceRequest(server.url("/br"))).getFailureKind(), "br is unsupported");
        Check.equal(0, fetcher.fetch(new ResourceRequest(server.url("/emptygz"))).getBody().length, "empty gzip body");
        Check.isTrue(fetcher.fetch(new ResourceRequest(server.url("/badgz"))).getFailureKind() != FailureKind.NONE, "damaged gzip is a failure");
    }

    /**
     * Memory limits: declared length, decoded length (zip bomb) and chunked bodies are limited; streaming has no limit.
     * <p>
     * 메모리 제한: 선언된 길이, 해독 후 길이(압축 폭탄), chunked 본문을 제한하고 스트리밍에는 제한이 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeLimitsAndStreaming() throws Exception {
        StringBuilder sb = new StringBuilder();
        // 천 바이트짜리 본문을 만든다.
        for (int i = 0; i < 100; i++) {
            sb.append("0123456789");
        }
        server.routeFixed("/big", TestHttpServer.Response.ok("application/octet-stream", sb.toString()));
        server.routeFixed("/bigchunk", TestHttpServer.Response.ok("application/octet-stream", sb.toString()).chunked(50));
        server.routeFixed("/biggz", TestHttpServer.Response.ok("application/octet-stream", sb.toString()).gzip());
        server.start();
        HttpFetcher small = new HttpFetcher(null, FetchLimits.DEFAULT.withMaxMemoryBytes(200));
        Check.equal(FailureKind.TOO_LARGE, small.fetch(new ResourceRequest(server.url("/big"))).getFailureKind(), "declared length over the limit");
        Check.equal(FailureKind.TOO_LARGE, small.fetch(new ResourceRequest(server.url("/bigchunk"))).getFailureKind(), "chunked body over the limit");
        Check.equal(FailureKind.TOO_LARGE, small.fetch(new ResourceRequest(server.url("/biggz"))).getFailureKind(), "decoded body over the limit");
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        final List<String> seen = new ArrayList<String>();
        ResponseHandler h = new ResponseHandler() {
            /**
             * Streams every successful body into the sink.
             * <p>
             * 성공한 본문을 모두 sink 로 보낸다.
             *
             * @param head response head / 응답 머리
             * @return the sink / sink
             */
            public OutputStream onResponse(ResourceResponse head) {
                seen.add(head.getStatus() + " " + head.getMimeType());
                return sink;
            }
        };
        ResourceResponse streamed = small.fetch(new ResourceRequest(server.url("/big")), h);
        Check.equal(FailureKind.NONE, streamed.getFailureKind(), "streaming ignores the memory limit");
        Check.isTrue(streamed.isStreamed(), "streamed flag");
        Check.equal(0, streamed.getBody().length, "no body in memory");
        Check.equal(1000, streamed.getBodyLength(), "streamed byte count");
        Check.equal(sb.toString(), new String(sink.toByteArray(), "ISO-8859-1"), "streamed bytes");
        Check.equal("[200 application/octet-stream]", seen.toString(), "handler saw the response head");
        sink.reset();
        small.fetch(new ResourceRequest(server.url("/biggz")), h);
        Check.equal(sb.toString(), new String(sink.toByteArray(), "ISO-8859-1"), "streamed bytes are decoded");
        ResourceResponse notFound = small.fetch(new ResourceRequest(server.url("/nothing")), h);
        Check.equal(404, notFound.getStatus(), "error status");
        Check.isFalse(notFound.isStreamed(), "error bodies are kept in memory, not streamed");
        Check.equal(2, seen.size(), "the handler is not asked for error statuses");
    }

    /**
     * Truncated bodies (short Content-Length, chunked without the last chunk) are failures.
     * <p>
     * 잘린 본문(짧은 Content-Length, 마지막 조각이 없는 chunked)은 실패이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedResponses() throws Exception {
        TestHttpServer.Response aborted = TestHttpServer.Response.ok("text/plain", "0123456789").chunked(4);
        aborted.abortChunked = true;
        server.routeFixed("/short", TestHttpServer.Response.ok("text/plain", "0123456789").truncateAt(4));
        server.routeFixed("/abort", aborted);
        TestHttpServer.Response dropped = new TestHttpServer.Response(200);
        dropped.dropWithoutResponse = true;
        server.routeFixed("/drop", dropped);
        server.start();
        ResourceResponse s = fetcher.fetch(new ResourceRequest(server.url("/short")));
        Check.equal(FailureKind.TRUNCATED, s.getFailureKind(), "short content-length");
        Check.equal(200, s.getStatus(), "the head was received");
        Check.equal(FailureKind.TRUNCATED, fetcher.fetch(new ResourceRequest(server.url("/abort"))).getFailureKind(), "chunked without terminator");
        ResourceResponse d = fetcher.fetch(new ResourceRequest(server.url("/drop")));
        Check.isTrue(d.getFailureKind() != FailureKind.NONE, "a dropped connection is a failure");
        Check.equal(0, d.getStatus(), "no status");
    }

    /**
     * Read timeouts and refused connections are reported as failures; DNS failures too when the resolver behaves.
     * <p>
     * 읽기 제한 시간 초과와 연결 거부를 실패로 보고한다. 해석기가 정상이면 DNS 실패도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTimeoutAndConnectionFailures() throws Exception {
        server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(3000));
        server.start();
        HttpFetcher quick = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(8000, 300));
        long t0 = System.currentTimeMillis();
        ResourceResponse slow = quick.fetch(new ResourceRequest(server.url("/slow")));
        Check.equal(FailureKind.TIMEOUT, slow.getFailureKind(), "read timeout");
        Check.isTrue(System.currentTimeMillis() - t0 < 2500, "the timeout ended the wait");
        ServerSocket probe = new ServerSocket(0);
        int freePort = probe.getLocalPort();
        probe.close();
        ResourceResponse refused = quick.fetch(new ResourceRequest("http://localhost:" + freePort + "/"));
        Check.equal(FailureKind.CONNECT, refused.getFailureKind(), "connection refused");
        ResourceResponse dns = quick.fetch(new ResourceRequest("http://no-such-host.invalid/"));
        if (dns.getFailureKind() != FailureKind.DNS) {
            Check.skip("this machine's resolver does not fail for .invalid names (got " + dns.getFailureKind() + ")");
        }
    }

    /**
     * Cancelling during a slow response ends the fetch promptly with CANCELLED; a cancelled token never connects.
     * <p>
     * 느린 응답 중에 취소하면 가져오기가 곧바로 CANCELLED 로 끝나고, 이미 취소된 토큰은 연결하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(8000));
        server.routeFixed("/quick", TestHttpServer.Response.ok("text/plain", "q"));
        server.start();
        final CancelToken token = new CancelToken();
        final ResourceResponse[] holder = new ResourceResponse[1];
        final HttpFetcher f = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 20000));
        final ResourceRequest req = new ResourceRequest(server.url("/slow")).setCancelToken(token);
        Thread t = new Thread(new Runnable() {
            /**
             * Runs the slow fetch.
             * <p>
             * 느린 가져오기를 실행한다.
             */
            public void run() {
                holder[0] = f.fetch(req);
            }
        });
        t.start();
        Check.isTrue(server.awaitRequests(1, 5000), "the request reached the server");
        long t0 = System.currentTimeMillis();
        token.cancel();
        t.join(5000);
        Check.isFalse(t.isAlive(), "the fetch thread ended after cancel");
        Check.isTrue(System.currentTimeMillis() - t0 < 4000, "cancel was prompt");
        Check.equal(FailureKind.CANCELLED, holder[0].getFailureKind(), "cancelled");
        CancelToken pre = new CancelToken();
        pre.cancel();
        int before = server.totalRequests();
        ResourceResponse r = f.fetch(new ResourceRequest(server.url("/quick")).setCancelToken(pre));
        Check.equal(FailureKind.CANCELLED, r.getFailureKind(), "pre-cancelled");
        Check.equal(before, server.totalRequests(), "no request was sent");
    }

    /**
     * Progress is reported with the known total, or -1 for an encoded or length-less body.
     * <p>
     * 진행 상황은 알려진 전체 크기와 함께 보고되며 압축되었거나 길이를 모르는 본문은 -1 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testProgressReporting() throws Exception {
        server.routeFixed("/p", TestHttpServer.Response.ok("text/plain", "0123456789012345678901234567890123456789"));
        TestHttpServer.Response noLength = TestHttpServer.Response.ok("text/plain", "abcdefghij");
        noLength.noLength = true;
        server.routeFixed("/nolen", noLength);
        server.start();
        final List<long[]> calls = new ArrayList<long[]>();
        ProgressListener l = new ProgressListener() {
            /**
             * Records the call.
             * <p>
             * 호출을 기록한다.
             *
             * @param received bytes so far / 지금까지의 바이트
             * @param total    total or -1 / 전체 또는 -1
             */
            public void onProgress(long received, long total) {
                calls.add(new long[] { received, total });
            }
        };
        fetcher.fetch(new ResourceRequest(server.url("/p")).setProgressListener(l));
        Check.equal(0, calls.get(0)[0], "starts at zero");
        Check.equal(40, calls.get(0)[1], "total from Content-Length");
        Check.equal(40, calls.get(calls.size() - 1)[0], "ends with everything received");
        calls.clear();
        fetcher.fetch(new ResourceRequest(server.url("/nolen")).setProgressListener(l));
        Check.equal(-1, calls.get(0)[1], "unknown length is indeterminate (-1)");
        Check.equal(10, calls.get(calls.size() - 1)[0], "all bytes counted");
    }

    /**
     * Fragments never go on the wire.
     * <p>
     * 프래그먼트는 전송되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmentIsNotSent() throws Exception {
        server.routeFixed("/doc", TestHttpServer.Response.html("d"));
        server.start();
        fetcher.fetch(new ResourceRequest(server.url("/doc?x=1#top")));
        Check.equal("/doc?x=1", server.requests().get(0).target, "request target has no fragment");
    }

    /**
     * An unsupported or unknown HTTP method name is refused when building the request.
     * <p>
     * 지원하지 않는 HTTP 메서드 이름은 요청을 만들 때 거부된다.
     */
    public void testUnsupportedMethodIsRejected() {
        try {
            new ResourceRequest("http://h/").setMethod("CONNECT");
            Check.fail("CONNECT must be rejected");
        } catch (IllegalArgumentException expected) {
            Check.contains(expected.getMessage(), "CONNECT", "message names the method");
        }
    }
}
