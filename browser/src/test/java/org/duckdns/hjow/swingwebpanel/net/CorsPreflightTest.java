package org.duckdns.hjow.swingwebpanel.net;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of the CORS preflight of {@link HttpFetcher} (plan item P20-08-c) with two local servers: the page lives at
 * <code>http://page.example</code> and the API server is another origin. A cross-origin request with a method or a page header that is not
 * safelisted is announced with an <code>OPTIONS</code> request that carries <code>Origin</code>, <code>Access-Control-Request-Method</code>
 * and <code>Access-Control-Request-Headers</code> but no cookies; only an answer that grants the request lets the real request go out, a
 * grant is reused for <code>Access-Control-Max-Age</code>, and simple requests, same-origin requests, module scripts and the additional
 * headers of the component never cause a preflight.
 * <p>
 * 두 개의 로컬 서버로 {@link HttpFetcher} 의 CORS 사전 요청(계획 P20-08-c)을 시험한다. 페이지는 <code>http://page.example</code> 에 있고 API 서버는
 * 다른 오리진이다. 안전 목록에 없는 메서드나 페이지 헤더를 가진 교차 오리진 요청은 <code>Origin</code>, <code>Access-Control-Request-Method</code>,
 * <code>Access-Control-Request-Headers</code> 를 싣고 쿠키는 싣지 않는 <code>OPTIONS</code> 요청으로 미리 알린다. 요청을 허락하는 응답만 본 요청을
 * 내보내고, 허락은 <code>Access-Control-Max-Age</code> 동안 다시 쓰며, 단순 요청, 같은 오리진 요청, 모듈 스크립트, 컴포넌트의 추가 헤더는 사전 요청을
 * 일으키지 않는다.
 */
public class CorsPreflightTest {

    /**
     * The origin of the page that makes the requests.
     * <p>
     * 요청을 만드는 페이지의 오리진.
     */
    private static final Origin PAGE = Origin.of("http://page.example/");

    /**
     * What the API server answers to a preflight; the fields are changed by the cases.
     * <p>
     * API 서버가 사전 요청에 답하는 내용. 사례가 필드를 바꾼다.
     */
    private static final class Api implements TestHttpServer.Handler {
        /** The status of the answer to the preflight. / 사전 요청 응답의 상태. */
        volatile int preflightStatus = 204;
        /** The Access-Control-Allow-Origin of the preflight, null for none. / 사전 요청의 Access-Control-Allow-Origin. 없으면 null. */
        volatile String allowOrigin = "http://page.example";
        /** The Access-Control-Allow-Methods of the preflight, null for none. / 사전 요청의 Access-Control-Allow-Methods. 없으면 null. */
        volatile String allowMethods = "GET, POST, PUT, DELETE";
        /** The Access-Control-Allow-Headers of the preflight, null for none. / 사전 요청의 Access-Control-Allow-Headers. 없으면 null. */
        volatile String allowHeaders = "content-type, x-token";
        /** The Access-Control-Allow-Credentials of the preflight, null for none. / 사전 요청의 Access-Control-Allow-Credentials. 없으면 null. */
        volatile String allowCredentials;
        /** The Access-Control-Max-Age of the preflight, null for none. / 사전 요청의 Access-Control-Max-Age. 없으면 null. */
        volatile String maxAge;
        /** The delay of the answer to the preflight in milliseconds. / 사전 요청 응답의 지연(밀리초). */
        volatile long preflightDelay;

        /**
         * Answers a preflight with the configured grant and every other request with a small page.
         * <p>
         * 사전 요청에는 설정한 허락으로, 다른 요청에는 작은 페이지로 답한다.
         *
         * @param request the request / 요청
         * @return the response / 응답
         */
        public TestHttpServer.Response handle(TestHttpServer.Request request) {
            if ("OPTIONS".equals(request.method)) {
                TestHttpServer.Response r = new TestHttpServer.Response(preflightStatus).delay(preflightDelay);
                if (allowOrigin != null) {
                    r.header("Access-Control-Allow-Origin", allowOrigin);
                }
                if (allowMethods != null) {
                    r.header("Access-Control-Allow-Methods", allowMethods);
                }
                if (allowHeaders != null) {
                    r.header("Access-Control-Allow-Headers", allowHeaders);
                }
                if (allowCredentials != null) {
                    r.header("Access-Control-Allow-Credentials", allowCredentials);
                }
                if (maxAge != null) {
                    r.header("Access-Control-Max-Age", maxAge);
                }
                return r;
            }
            return TestHttpServer.Response.ok("text/plain", "done " + request.method).header("Access-Control-Allow-Origin", "http://page.example");
        }
    }

    /**
     * The API server (the other origin).
     * <p>
     * API 서버(다른 오리진).
     */
    private TestHttpServer api;

    /**
     * What the API server answers.
     * <p>
     * API 서버가 답하는 내용.
     */
    private Api answers;

    /**
     * The fetcher.
     * <p>
     * 가져오기 객체.
     */
    private HttpFetcher fetcher;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CorsPreflightTest() {
    }

    /**
     * Starts the API server and creates a fresh fetcher (so that no grant is remembered from an earlier case).
     * <p>
     * API 서버를 시작하고 새 가져오기 객체를 만든다(앞선 사례의 허락이 남지 않도록).
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        api = new TestHttpServer();
        answers = new Api();
        api.route("/data*", answers);
        api.start();
        fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));
    }

    /**
     * Stops the server.
     * <p>
     * 서버를 멈춘다.
     */
    public void tearDown() {
        api.stop();
    }

    /**
     * Creates an XHR request of the page without cookies.
     * <p>
     * 쿠키 없는 페이지의 XHR 요청을 만든다.
     *
     * @param path   the path on the API server / API 서버의 경로
     * @param method the method / 메서드
     * @return the request / 요청
     */
    private ResourceRequest xhr(String path, String method) {
        ResourceRequest r = new ResourceRequest(api.url(path)).setMethod(method).setPurpose(RequestPurpose.XHR).setInitiator(PAGE)
                .setSendCredentials(false);
        if ("PUT".equals(method) || "POST".equals(method)) {
            r.setBody(utf8("x"), "text/plain");
        }
        return r;
    }

    /**
     * Encodes a text as UTF-8.
     * <p>
     * 문자열을 UTF-8 로 인코딩한다.
     *
     * @param text the text / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Counts the requests with a method that the API server logged.
     * <p>
     * API 서버가 기록한 요청 중 메서드가 같은 것을 센다.
     *
     * @param method the method / 메서드
     * @return the count / 개수
     */
    private int count(String method) {
        int n = 0;
        // 기록된 요청 중 메서드가 같은 것을 센다.
        for (TestHttpServer.Request r : api.requests()) {
            if (r.method.equals(method)) {
                n++;
            }
        }
        return n;
    }

    /**
     * A PUT to another origin is announced with an OPTIONS request that carries the origin and the wanted method, then the PUT follows
     * with the origin as well.
     * <p>
     * 다른 오리진으로의 PUT 은 오리진과 원하는 메서드를 실은 OPTIONS 요청으로 미리 알리고, 그 뒤에 PUT 이 오리진과 함께 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAllowedPutIsPreflighted() throws Exception {
        ResourceResponse r = fetcher.fetch(xhr("/data", "PUT"));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "no failure: " + r.getFailureMessage());
        Check.equal(200, r.getStatus(), "status of the PUT");
        Check.equal(1, count("OPTIONS"), "one preflight");
        Check.equal(1, count("PUT"), "one PUT");
        TestHttpServer.Request pre = api.requests().get(0);
        Check.equal("OPTIONS", pre.method, "the preflight comes first");
        Check.equal("http://page.example", pre.header("origin"), "Origin of the preflight");
        Check.equal("PUT", pre.header("access-control-request-method"), "requested method");
        Check.isNull(pre.header("access-control-request-headers"), "no page header, nothing announced");
        Check.isNull(pre.header("cookie"), "a preflight has no cookies");
        Check.equal("swingwebpanel/1.0", pre.header("user-agent"), "User-Agent of the preflight");
        Check.equal("http://page.example", api.last("/data").header("origin"), "Origin of the actual request");
    }

    /**
     * The headers of the page (and the content type of a JSON body) are announced sorted and in lower case, and a server that does not
     * allow one of them stops the request before it is sent.
     * <p>
     * 페이지의 헤더(와 JSON 본문의 content type)는 정렬하고 소문자로 알리며, 그중 하나를 허용하지 않는 서버는 요청을 보내기 전에 막는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageHeadersAreAnnouncedAndChecked() throws Exception {
        Map<String, String> author = new LinkedHashMap<String, String>();
        author.put("X-Token", "1");
        author.put("Accept", "*/*");
        ResourceRequest json = xhr("/data", "POST").setBody(utf8("{}"), "application/json").setCorsAuthorHeaders(author);
        ResourceResponse ok = fetcher.fetch(json);
        Check.equal(FailureKind.NONE, ok.getFailureKind(), "granted: " + ok.getFailureMessage());
        TestHttpServer.Request pre = api.requests().get(0);
        Check.equal("content-type,x-token", pre.header("access-control-request-headers"), "sorted unsafe names, Accept is safelisted");
        Check.equal("POST", pre.header("access-control-request-method"), "a POST with a JSON body needs the preflight, too");
        answers.allowHeaders = "content-type";
        ResourceResponse denied = fetcher.fetch(xhr("/data2", "POST").setBody(utf8("{}"), "application/json").setCorsAuthorHeaders(author));
        Check.equal(FailureKind.CORS, denied.getFailureKind(), "x-token is not allowed");
        Check.contains(denied.getFailureMessage(), "does not allow", "the reason: " + denied.getFailureMessage());
        Check.equal(1, count("POST"), "the second POST was never sent");
    }

    /**
     * A preflight that is not answered with a grant ends the request with a CORS failure and the real request is never sent: an answer
     * without <code>Access-Control-Allow-Origin</code>, for another origin, with a status outside 2xx, without the wanted method.
     * <p>
     * 허락으로 답하지 않은 사전 요청은 요청을 CORS 실패로 끝내고 본 요청은 보내지 않는다: <code>Access-Control-Allow-Origin</code> 이 없는 답, 다른 오리진을
     * 위한 답, 2xx 가 아닌 상태, 원하는 메서드가 없는 답.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDeniedPreflightStopsTheRequest() throws Exception {
        answers.allowOrigin = null;
        ResourceResponse none = fetcher.fetch(xhr("/data", "PUT"));
        Check.equal(FailureKind.CORS, none.getFailureKind(), "no Access-Control-Allow-Origin");
        answers.allowOrigin = "http://other.example";
        Check.equal(FailureKind.CORS, fetcher.fetch(xhr("/data", "PUT")).getFailureKind(), "another origin");
        answers.allowOrigin = "http://page.example";
        answers.preflightStatus = 403;
        ResourceResponse forbidden = fetcher.fetch(xhr("/data", "PUT"));
        Check.equal(FailureKind.CORS, forbidden.getFailureKind(), "status 403");
        Check.contains(forbidden.getFailureMessage(), "403", "the status is named: " + forbidden.getFailureMessage());
        answers.preflightStatus = 204;
        answers.allowMethods = "GET, POST";
        ResourceResponse method = fetcher.fetch(xhr("/data", "PUT"));
        Check.equal(FailureKind.CORS, method.getFailureKind(), "PUT is not allowed");
        Check.contains(method.getFailureMessage(), "PUT", "the method is named: " + method.getFailureMessage());
        Check.equal(0, count("PUT"), "no PUT was ever sent");
        Check.equal(4, count("OPTIONS"), "each denied request made its own preflight");
    }

    /**
     * With credentials, <code>*</code> grants nothing and the answer needs the exact origin and
     * <code>Access-Control-Allow-Credentials: true</code>; the preflight has no cookies but the real request has.
     * <p>
     * 자격 증명이 있으면 <code>*</code> 은 아무것도 허락하지 않으며 답에는 정확한 오리진과 <code>Access-Control-Allow-Credentials: true</code> 가 필요하다.
     * 사전 요청에는 쿠키가 없지만 본 요청에는 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCredentialsNeedTheExactOriginAndTheCredentialsHeader() throws Exception {
        CookieStore store = new CookieStore();
        store.storeFromResponse(api.url("/"), Collections.singletonList("sid=42; Path=/"));
        answers.allowOrigin = "*";
        answers.allowMethods = "*";
        ResourceRequest r1 = xhr("/data", "PUT").setCookies(store).setSendCredentials(true);
        Check.equal(FailureKind.CORS, fetcher.fetch(r1).getFailureKind(), "* is no grant for credentials");
        answers.allowOrigin = "http://page.example";
        answers.allowMethods = "PUT";
        ResourceRequest r2 = xhr("/data", "PUT").setCookies(store).setSendCredentials(true);
        Check.equal(FailureKind.CORS, fetcher.fetch(r2).getFailureKind(), "the origin alone is no grant for credentials");
        answers.allowCredentials = "true";
        ResourceRequest r3 = xhr("/data", "PUT").setCookies(store).setSendCredentials(true);
        ResourceResponse ok = fetcher.fetch(r3);
        Check.equal(FailureKind.NONE, ok.getFailureKind(), "granted with credentials: " + ok.getFailureMessage());
        List<TestHttpServer.Request> all = api.requests();
        TestHttpServer.Request put = all.get(all.size() - 1);
        TestHttpServer.Request pre = all.get(all.size() - 2);
        Check.equal("PUT", put.method, "the PUT is last");
        Check.equal("OPTIONS", pre.method, "the preflight is before it");
        Check.isNull(pre.header("cookie"), "the preflight has no cookies");
        Check.equal("sid=42", put.header("cookie"), "the actual request has the cookie");
    }

    /**
     * Without credentials, <code>*</code> grants any method and any header except <code>Authorization</code>, which must be named.
     * <p>
     * 자격 증명이 없으면 <code>*</code> 은 어떤 메서드와 헤더든 허락하지만 <code>Authorization</code> 은 이름을 적어야 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWildcardsExceptAuthorization() throws Exception {
        answers.allowOrigin = "*";
        answers.allowMethods = "*";
        answers.allowHeaders = "*";
        Map<String, String> custom = new LinkedHashMap<String, String>();
        custom.put("X-Anything", "1");
        ResourceResponse ok = fetcher.fetch(xhr("/data", "DELETE").setCorsAuthorHeaders(custom));
        Check.equal(FailureKind.NONE, ok.getFailureKind(), "wildcards grant: " + ok.getFailureMessage());
        Map<String, String> auth = new LinkedHashMap<String, String>();
        auth.put("Authorization", "Bearer t");
        ResourceResponse denied = fetcher.fetch(xhr("/data3", "GET").setCorsAuthorHeaders(auth));
        Check.equal(FailureKind.CORS, denied.getFailureKind(), "* does not cover Authorization");
        answers.allowHeaders = "*, authorization";
        ResourceResponse named = fetcher.fetch(xhr("/data4", "GET").setCorsAuthorHeaders(auth));
        Check.equal(FailureKind.NONE, named.getFailureKind(), "Authorization named: " + named.getFailureMessage());
    }

    /**
     * A grant is reused for <code>Access-Control-Max-Age</code> seconds for the same origin, URL and credentials; a grant without a max age
     * is reused only for the short default, and a max age of 0 is never reused; another URL needs its own preflight.
     * <p>
     * 허락은 같은 오리진, URL, 자격 증명에 대해 <code>Access-Control-Max-Age</code> 초 동안 다시 쓴다. max age 가 없는 허락은 짧은 기본 시간 동안만,
     * max age 가 0 이면 다시 쓰지 않으며, 다른 URL 은 자신의 사전 요청이 필요하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGrantIsReusedForTheMaxAge() throws Exception {
        answers.maxAge = "60";
        fetcher.fetch(xhr("/data", "PUT"));
        fetcher.fetch(xhr("/data", "PUT"));
        fetcher.fetch(xhr("/data", "PUT"));
        Check.equal(1, count("OPTIONS"), "one preflight for three requests");
        Check.equal(3, count("PUT"), "all three PUT were sent");
        fetcher.fetch(xhr("/data5", "PUT"));
        Check.equal(2, count("OPTIONS"), "another URL has its own preflight");
        answers.maxAge = "0";
        fetcher.fetch(xhr("/data6", "PUT"));
        fetcher.fetch(xhr("/data6", "PUT"));
        Check.equal(4, count("OPTIONS"), "a max age of 0 is not reused");
        answers.maxAge = null;
        fetcher.fetch(xhr("/data7", "PUT"));
        fetcher.fetch(xhr("/data7", "PUT"));
        Check.equal(5, count("OPTIONS"), "the default age is reused briefly");
    }

    /**
     * Simple requests, same-origin requests, module scripts and the additional headers of the component never cause a preflight, but the
     * additional headers are sent.
     * <p>
     * 단순 요청, 같은 오리진 요청, 모듈 스크립트, 컴포넌트의 추가 헤더는 사전 요청을 일으키지 않지만 추가 헤더는 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRequestsThatNeedNoPreflight() throws Exception {
        Map<String, String> simple = new LinkedHashMap<String, String>();
        simple.put("Accept", "application/json");
        simple.put("Content-Type", "application/x-www-form-urlencoded");
        Check.equal(FailureKind.NONE, fetcher.fetch(xhr("/data", "GET").setCorsAuthorHeaders(simple)).getFailureKind(), "simple GET");
        Check.equal(FailureKind.NONE, fetcher.fetch(xhr("/data", "POST").setBody(utf8("a=1"), "application/x-www-form-urlencoded")).getFailureKind(),
                "simple POST");
        ResourceRequest same = new ResourceRequest(api.url("/data")).setMethod("PUT").setBody(utf8("x"), "application/json")
                .setPurpose(RequestPurpose.XHR).setInitiator(Origin.of(api.url("/")));
        Check.equal(FailureKind.NONE, fetcher.fetch(same).getFailureKind(), "same-origin PUT");
        RequestHeaders extra = new RequestHeaders();
        extra.addAll(Collections.singletonMap("X-Host-Header", "kept"));
        ResourceRequest withExtra = xhr("/data", "GET").setExtraHeaders(extra);
        Check.equal(FailureKind.NONE, fetcher.fetch(withExtra).getFailureKind(), "component header");
        Check.equal("kept", api.last("/data").header("x-host-header"), "the component header was sent");
        ResourceRequest module = xhr("/data", "GET").setPurpose(RequestPurpose.MODULE).setExtraHeaders(extra);
        Check.equal(FailureKind.NONE, fetcher.fetch(module).getFailureKind(), "module script");
        Check.equal(0, count("OPTIONS"), "none of these made a preflight");
    }

    /**
     * A redirect to another URL is preflighted again for the new hop, and a failed preflight of the new hop ends the request.
     * <p>
     * 다른 URL 로의 리디렉션은 새 구간에 대해 사전 요청을 다시 하고, 새 구간의 사전 요청이 실패하면 요청이 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectIsPreflightedAgain() throws Exception {
        api.route("/hop", new TestHttpServer.Handler() {
            /**
             * Redirects every request except the preflight to the data path.
             * <p>
             * 사전 요청을 뺀 모든 요청을 데이터 경로로 리디렉션한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if ("OPTIONS".equals(request.method)) {
                    return new TestHttpServer.Response(204).header("Access-Control-Allow-Origin", "http://page.example")
                            .header("Access-Control-Allow-Methods", "PUT");
                }
                return TestHttpServer.Response.redirect(307, "/data-final");
            }
        });
        ResourceResponse r = fetcher.fetch(xhr("/hop", "PUT"));
        Check.equal(FailureKind.NONE, r.getFailureKind(), "followed: " + r.getFailureMessage());
        Check.equal(200, r.getStatus(), "final status");
        Check.equal(2, count("OPTIONS"), "a preflight for each hop");
        answers.allowMethods = "GET";
        fetcher = new HttpFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000));
        ResourceResponse denied = fetcher.fetch(xhr("/hop", "PUT"));
        Check.equal(FailureKind.CORS, denied.getFailureKind(), "the second hop does not allow PUT");
    }

    /**
     * Cancelling while the preflight is waiting for the server ends the request at once, and an unreachable server is a connection
     * failure, not a CORS failure.
     * <p>
     * 서버를 기다리는 동안 사전 요청을 취소하면 요청이 바로 끝나고, 닿을 수 없는 서버는 CORS 실패가 아니라 연결 실패이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelAndNetworkFailureOfThePreflight() throws Exception {
        answers.preflightDelay = 8000;
        final CancelToken token = new CancelToken();
        Thread canceller = new Thread(new Runnable() {
            /**
             * Cancels the request shortly after it started.
             * <p>
             * 요청이 시작된 직후에 취소한다.
             */
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    return;
                }
                token.cancel();
            }
        });
        canceller.start();
        long start = System.currentTimeMillis();
        ResourceResponse cancelled = fetcher.fetch(xhr("/data", "PUT").setCancelToken(token));
        Check.equal(FailureKind.CANCELLED, cancelled.getFailureKind(), "cancelled");
        Check.isTrue(System.currentTimeMillis() - start < 5000, "at once");
        canceller.join();
        answers.preflightDelay = 0;
        int port = api.getPort();
        api.stop();
        ResourceResponse down = fetcher.fetch(new ResourceRequest("http://127.0.0.1:" + port + "/data").setMethod("PUT").setBody(utf8("x"), "text/plain")
                .setPurpose(RequestPurpose.XHR).setInitiator(PAGE).setSendCredentials(false));
        Check.equal(FailureKind.CONNECT, down.getFailureKind(), "the server is down: " + down.getFailureMessage());
    }
}
