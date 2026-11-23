package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of {@link HttpCache} and its use by {@link ResourceFetcher} against the local test server (plan items P19-03 to P19-06): fresh
 * hits without a request, conditional requests with <code>If-None-Match</code> / <code>If-Modified-Since</code>, merging of 304 responses,
 * <code>no-store</code>, <code>no-cache</code>, <code>Vary</code>, forced reload, history preference, invalidation by unsafe methods,
 * eviction, the body handler on stored responses and the isolation of components. The server counts every request, so each case also checks
 * how many requests really reached the network.
 * <p>
 * {@link HttpCache} 와 {@link ResourceFetcher} 의 사용을 로컬 시험 서버로 확인한다(계획 P19-03~P19-06): 요청 없는 신선한 적중,
 * <code>If-None-Match</code>/<code>If-Modified-Since</code> 조건부 요청, 304 응답 합치기, <code>no-store</code>, <code>no-cache</code>,
 * <code>Vary</code>, 강제 새로고침, 이력 이동의 캐시 우선, 안전하지 않은 메서드에 의한 무효화, 제거, 저장된 응답에 대한 본문 처리기, 컴포넌트 사이의
 * 분리. 서버가 모든 요청을 세므로 각 사례는 네트워크에 실제로 닿은 요청 수도 확인한다.
 */
public class HttpCacheTest {

    /**
     * A clock that only moves when the test says so.
     * <p>
     * 시험이 지시할 때만 움직이는 시계.
     */
    private static final class FakeClock implements HttpCache.Clock {
        /**
         * Current time in milliseconds.
         * <p>
         * 현재 시각(밀리초).
         */
        private final AtomicLong now = new AtomicLong(System.currentTimeMillis());

        /**
         * Returns the fake time.
         * <p>
         * 가짜 시각을 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public long now() {
            return now.get();
        }

        /**
         * Moves the clock forward.
         * <p>
         * 시계를 앞으로 보낸다.
         *
         * @param seconds seconds to add / 더할 초
         */
        void advance(long seconds) {
            now.addAndGet(seconds * 1000L);
        }
    }

    /**
     * Test server of the case.
     * <p>
     * 사례의 시험 서버.
     */
    private TestHttpServer server;

    /**
     * Clock of the case.
     * <p>
     * 사례의 시계.
     */
    private FakeClock clock;

    /**
     * Cache of the case.
     * <p>
     * 사례의 캐시.
     */
    private HttpCache cache;

    /**
     * Fetcher of the case that uses the cache.
     * <p>
     * 캐시를 쓰는 사례의 가져오기 객체.
     */
    private ResourceFetcher fetcher;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HttpCacheTest() {
    }

    /**
     * A response arriving after cancellation and clear cannot refill the cache, even if the transport ignored cancellation.
     * <p>전송기가 취소를 무시해도 취소·삭제 뒤에 도착한 응답이 캐시를 다시 채우지 못한다.
     */
    public void testCancelledLateResponseCannotRefillClearedCache() {
        ResourceRequest request = new ResourceRequest("http://late.test/resource");
        cache.fetch(request, null, new HttpCache.Network() {
            /** Returns a successful response after cancellation and clearing. / 취소·삭제 후 성공 응답을 반환한다.
             * @param request resource request / 리소스 요청
             * @param handler unused body handler / 사용하지 않는 본문 처리기
             * @return late response / 늦은 응답 */
            public ResourceResponse fetch(ResourceRequest request, ResponseHandler handler) {
                request.getCancelToken().cancel();
                cache.clear();
                ResourceResponse response = new ResourceResponse(request.getUrl());
                response.setStatus(200, "OK");
                response.addHeader("Cache-Control", "max-age=60");
                response.setBody(new byte[] {1, 2, 3});
                return response;
            }
        });
        Check.equal(0, cache.getEntryCount(), "cancelled response not stored");
        Check.equal(0, cache.getUsedBytes(), "cache remains empty");
    }

    /**
     * Starts the server and creates an empty cache before each case.
     * <p>
     * 사례마다 서버를 시작하고 빈 캐시를 만든다.
     *
     * @throws IOException if the server cannot start / 서버를 시작할 수 없으면
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
        clock = new FakeClock();
        cache = new HttpCache(HttpCache.DEFAULT_MAX_BYTES, HttpCache.DEFAULT_MAX_ENTRY_BYTES, clock);
        fetcher = new ResourceFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000), null);
        fetcher.setCache(cache);
    }

    /**
     * Stops the server after each case.
     * <p>
     * 사례가 끝나면 서버를 멈춘다.
     */
    public void tearDown() {
        server.stop();
    }

    /**
     * Returns a Date header value for the fake time.
     * <p>
     * 가짜 시각의 Date 헤더 값을 반환한다.
     *
     * @return formatted date / 서식을 적용한 날짜
     */
    private String date() {
        return HttpDates.format(clock.now());
    }

    /**
     * Registers a route that always answers 200 with a Cache-Control value, an ETag and the number of requests so far as its body.
     * <p>
     * 항상 Cache-Control 값과 ETag 로 200 을 답하며 지금까지의 요청 수를 본문으로 주는 경로를 등록한다.
     *
     * @param path         the path / 경로
     * @param cacheControl the Cache-Control value or null / Cache-Control 값 또는 null
     */
    private void countingRoute(final String path, final String cacheControl) {
        server.route(path, new TestHttpServer.Handler() {
            /**
             * Answers with the request count as the body.
             * <p>
             * 요청 수를 본문으로 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                TestHttpServer.Response r = TestHttpServer.Response.ok("text/plain; charset=UTF-8", "n=" + server.count(path));
                r.header("Date", date());
                if (cacheControl != null) {
                    r.header("Cache-Control", cacheControl);
                }
                return r;
            }
        });
    }

    /**
     * Fetches a path with the default cache mode.
     * <p>
     * 기본 캐시 모드로 경로를 가져온다.
     *
     * @param path the path / 경로
     * @return the response / 응답
     */
    private ResourceResponse get(String path) {
        return fetcher.fetch(new ResourceRequest(server.url(path)));
    }

    /**
     * Fetches a path with a cache mode.
     * <p>
     * 캐시 모드를 정해 경로를 가져온다.
     *
     * @param path the path / 경로
     * @param mode the cache mode / 캐시 모드
     * @return the response / 응답
     */
    private ResourceResponse get(String path, ResourceRequest.CacheMode mode) {
        return fetcher.fetch(new ResourceRequest(server.url(path)).setCacheMode(mode));
    }

    /**
     * Returns the body of a response as text.
     * <p>
     * 응답의 본문을 문자열로 반환한다.
     *
     * @param r the response / 응답
     * @return text / 문자열
     * @throws IOException never in practice / 실제로는 발생하지 않음
     */
    private static String text(ResourceResponse r) throws IOException {
        return new String(r.getBody(), "UTF-8");
    }

    /**
     * P19-03/P19-04: a fresh response is answered without a request; after it expires and without a validator the network is used again.
     * <p>
     * P19-03/P19-04: 신선한 응답은 요청 없이 답하고, 만료되고 검증자가 없으면 다시 네트워크를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFreshResponseNeedsNoRequest() throws Exception {
        countingRoute("/a", "max-age=60");
        ResourceResponse r1 = get("/a");
        Check.equal("n=1", text(r1), "first response");
        Check.isFalse(r1.isFromCache(), "the first response comes from the network");
        ResourceResponse r2 = get("/a");
        Check.equal("n=1", text(r2), "the stored body");
        Check.isTrue(r2.isFromCache(), "second response from the cache");
        Check.equal(1, server.count("/a"), "only one request reached the server");
        Check.equal("0", r2.getHeader("Age"), "age of a new copy");
        clock.advance(30);
        Check.isTrue(get("/a").isFromCache(), "still fresh after 30 seconds");
        Check.equal("30", get("/a").getHeader("Age"), "age grows with the clock");
        clock.advance(31);
        ResourceResponse r3 = get("/a");
        Check.isFalse(r3.isFromCache(), "stale and no validator: network again");
        Check.equal("n=2", text(r3), "new body");
        Check.equal(2, server.count("/a"), "two requests in total");
        Check.equal(1, cache.getEntryCount(), "one stored variant");
        Check.isTrue(cache.getHitCount() >= 3, "hits are counted");
    }

    /**
     * P19-04: a stale copy with an ETag is revalidated; a 304 keeps the stored body, merges the new headers and restarts the age.
     * <p>
     * P19-04: ETag 가 있는 오래된 사본은 재검증된다. 304 는 저장된 본문을 유지하고 새 헤더를 합치며 나이를 다시 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConditionalRequestWithEtagAndNotModified() throws Exception {
        server.route("/e", new TestHttpServer.Handler() {
            /**
             * Answers 304 to a matching If-None-Match, otherwise the full response.
             * <p>
             * If-None-Match 가 맞으면 304, 아니면 전체 응답을 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if ("\"v1\"".equals(request.header("If-None-Match"))) {
                    return new TestHttpServer.Response(304).header("ETag", "\"v1\"").header("Cache-Control", "max-age=100")
                            .header("Date", date());
                }
                return TestHttpServer.Response.ok("text/html; charset=UTF-8", "<p>one</p>").header("ETag", "\"v1\"")
                        .header("Cache-Control", "max-age=10").header("Date", date());
            }
        });
        Check.equal("<p>one</p>", text(get("/e")), "first response");
        clock.advance(11);
        ResourceResponse r2 = get("/e");
        Check.equal(200, r2.getStatus(), "the 304 is shown as the stored 200, never as an empty page");
        Check.equal("<p>one</p>", text(r2), "stored body is kept");
        Check.isTrue(r2.isRevalidated(), "confirmed by the server");
        Check.isFalse(r2.isFromCache(), "a request was sent");
        Check.equal("text/html", r2.getMimeType(), "media type of the stored response");
        Check.equal("max-age=100", r2.getHeader("Cache-Control"), "headers of the 304 are merged");
        Check.equal(2, server.count("/e"), "one conditional request");
        Check.equal("\"v1\"", server.last("/e").header("If-None-Match"), "validator sent");
        Check.equal(1, cache.getRevalidationCount(), "revalidation counted");
        Check.equal(1, cache.getNotModifiedCount(), "304 counted");
        clock.advance(50);
        ResourceResponse r3 = get("/e");
        Check.isTrue(r3.isFromCache(), "fresh again for 100 seconds after the 304");
        Check.equal(2, server.count("/e"), "no new request");
        clock.advance(60);
        get("/e");
        Check.equal(3, server.count("/e"), "after 100 seconds it is revalidated again");
    }

    /**
     * P19-04: Last-Modified is used as validator through If-Modified-Since.
     * <p>
     * P19-04: Last-Modified 는 If-Modified-Since 를 통해 검증자로 쓰인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLastModifiedValidator() throws Exception {
        final String lastModified = HttpDates.format(clock.now() - 100000L);
        server.route("/m", new TestHttpServer.Handler() {
            /**
             * Answers 304 to a matching If-Modified-Since.
             * <p>
             * If-Modified-Since 가 맞으면 304 로 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if (lastModified.equals(request.header("If-Modified-Since"))) {
                    return new TestHttpServer.Response(304).header("Date", date());
                }
                return TestHttpServer.Response.ok("text/plain", "body").header("Last-Modified", lastModified)
                        .header("Cache-Control", "max-age=0").header("Date", date());
            }
        });
        get("/m");
        ResourceResponse r = get("/m");
        Check.isTrue(r.isRevalidated(), "revalidated");
        Check.equal("body", text(r), "stored body");
        Check.equal(lastModified, server.last("/m").header("If-Modified-Since"), "validator");
        Check.isNull(server.last("/m").header("If-None-Match"), "no ETag was stored, so none is sent");
    }

    /**
     * P19-04: when the conditional request returns new content the stored copy is replaced.
     * <p>
     * P19-04: 조건부 요청이 새 내용을 돌려주면 저장된 사본이 바뀐다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChangedContentReplacesStoredCopy() throws Exception {
        server.route("/c", new TestHttpServer.Handler() {
            /**
             * Answers version 1 first and version 2 afterwards.
             * <p>
             * 처음에는 1판, 이후에는 2판을 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                int n = server.count("/c");
                return TestHttpServer.Response.ok("text/plain", "version " + n).header("ETag", "\"v" + n + "\"")
                        .header("Cache-Control", "max-age=10").header("Date", date());
            }
        });
        get("/c");
        clock.advance(11);
        ResourceResponse r2 = get("/c");
        Check.equal("version 2", text(r2), "new content");
        Check.isFalse(r2.isRevalidated(), "not a 304");
        Check.equal("\"v1\"", server.last("/c").header("If-None-Match"), "the old validator was sent");
        ResourceResponse r3 = get("/c");
        Check.isTrue(r3.isFromCache(), "the new copy is fresh");
        Check.equal("version 2", text(r3), "and replaced the old one");
        Check.equal(1, cache.getEntryCount(), "one variant");
    }

    /**
     * P19-03: no-store is never stored, no-cache is stored but always revalidated.
     * <p>
     * P19-03: no-store 는 저장하지 않고 no-cache 는 저장하되 항상 재검증한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoStoreAndNoCache() throws Exception {
        countingRoute("/s", "no-store, max-age=60");
        get("/s");
        get("/s");
        Check.equal(2, server.count("/s"), "no-store: every request reaches the server");
        Check.equal(0, cache.getEntryCount(), "nothing stored");
        server.route("/n", new TestHttpServer.Handler() {
            /**
             * Answers with an ETag and no-cache, 304 for the validator.
             * <p>
             * ETag 와 no-cache 로 답하고 검증자가 맞으면 304 를 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if ("\"x\"".equals(request.header("If-None-Match"))) {
                    return new TestHttpServer.Response(304).header("ETag", "\"x\"").header("Date", date());
                }
                return TestHttpServer.Response.ok("text/plain", "body").header("ETag", "\"x\"")
                        .header("Cache-Control", "no-cache, max-age=60").header("Date", date());
            }
        });
        get("/n");
        ResourceResponse r = get("/n");
        Check.isTrue(r.isRevalidated(), "no-cache always revalidates, even within max-age");
        Check.equal(2, server.count("/n"), "the revalidation reached the server");
        Check.equal(1, cache.getEntryCount(), "no-cache responses are stored");
    }

    /**
     * P19-03: Vary selects the stored variant by the request header; Vary: * and Vary: Cookie are not stored.
     * <p>
     * P19-03: Vary 는 요청 헤더로 저장된 변형을 고르고 Vary: * 와 Vary: Cookie 는 저장하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVary() throws Exception {
        server.route("/v", new TestHttpServer.Handler() {
            /**
             * Answers with a body that names the user agent.
             * <p>
             * 사용자 에이전트를 이름으로 담은 본문을 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.ok("text/plain", "for " + request.header("User-Agent"))
                        .header("Cache-Control", "max-age=60").header("Vary", "User-Agent").header("Date", date());
            }
        });
        Check.equal("for A", text(fetcher.fetch(new ResourceRequest(server.url("/v")).setUserAgent("A"))), "variant A");
        Check.equal("for B", text(fetcher.fetch(new ResourceRequest(server.url("/v")).setUserAgent("B"))), "variant B");
        Check.equal(2, server.count("/v"), "two variants fetched");
        ResourceResponse a2 = fetcher.fetch(new ResourceRequest(server.url("/v")).setUserAgent("A"));
        Check.isTrue(a2.isFromCache(), "variant A from the cache");
        Check.equal("for A", text(a2), "and it is the right variant");
        Check.isTrue(fetcher.fetch(new ResourceRequest(server.url("/v")).setUserAgent("B")).isFromCache(), "variant B from the cache");
        Check.equal(2, server.count("/v"), "no further requests");
        Check.equal(2, cache.getEntryCount(), "two variants stored");
        server.routeFixed("/star", TestHttpServer.Response.ok("text/plain", "x").header("Vary", "*").header("Cache-Control", "max-age=60"));
        server.routeFixed("/cookie",
                TestHttpServer.Response.ok("text/plain", "x").header("Vary", "Cookie").header("Cache-Control", "max-age=60"));
        get("/star");
        get("/star");
        get("/cookie");
        get("/cookie");
        Check.equal(2, server.count("/star"), "Vary: * is not stored");
        Check.equal(2, server.count("/cookie"), "Vary: Cookie is not stored");
    }

    /**
     * P19-06: a forced reload skips the stored copy, sends no-cache headers and replaces the copy.
     * <p>
     * P19-06: 강제 새로고침은 저장본을 건너뛰고 no-cache 헤더를 보내며 사본을 바꾼다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testForceReloadBypassesAndReplaces() throws Exception {
        countingRoute("/f", "max-age=60");
        get("/f");
        ResourceResponse forced = get("/f", ResourceRequest.CacheMode.FORCE_RELOAD);
        Check.equal("n=2", text(forced), "the forced reload reached the server");
        Check.isFalse(forced.isFromCache(), "not from the cache");
        Check.equal("no-cache", server.last("/f").header("Cache-Control"), "no-cache request header");
        Check.equal("no-cache", server.last("/f").header("Pragma"), "Pragma header");
        ResourceResponse third = get("/f");
        Check.isTrue(third.isFromCache(), "afterwards the new copy is used");
        Check.equal("n=2", text(third), "the stored copy was replaced");
        Check.equal(2, server.count("/f"), "no additional request");
    }

    /**
     * P19-01/P19-06: history navigation prefers the stored copy even when stale; without a copy the network is used.
     * <p>
     * P19-01/P19-06: 이력 이동은 오래된 저장본이라도 우선 쓰며 저장본이 없으면 네트워크를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPreferCacheForHistory() throws Exception {
        countingRoute("/h", null);
        ResourceResponse none = get("/h", ResourceRequest.CacheMode.PREFER_CACHE);
        Check.isFalse(none.isFromCache(), "no stored copy: network");
        ResourceResponse back = get("/h", ResourceRequest.CacheMode.PREFER_CACHE);
        Check.isTrue(back.isFromCache(), "stale copy used for history navigation");
        Check.equal(1, server.count("/h"), "one request");
        ResourceResponse normal = get("/h");
        Check.isFalse(normal.isFromCache(), "a normal navigation revalidates or refetches the stale copy");
        Check.equal(2, server.count("/h"), "second request");
        countingRoute("/hs", "no-store");
        get("/hs", ResourceRequest.CacheMode.PREFER_CACHE);
        get("/hs", ResourceRequest.CacheMode.PREFER_CACHE);
        Check.equal(2, server.count("/hs"), "no-store responses are never used");
    }

    /**
     * P19-03: freshness from Expires and from the heuristic of 10 percent of the age at Last-Modified.
     * <p>
     * P19-03: Expires 와 Last-Modified 시점 나이의 10 % 휴리스틱에서 오는 신선도.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExpiresAndHeuristicFreshness() throws Exception {
        server.route("/x", new TestHttpServer.Handler() {
            /**
             * Answers with an Expires header 30 seconds ahead.
             * <p>
             * 30초 뒤를 가리키는 Expires 헤더로 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.ok("text/plain", "n" + server.count("/x")).header("Date", date())
                        .header("Expires", HttpDates.format(clock.now() + 30000L));
            }
        });
        get("/x");
        clock.advance(20);
        Check.isTrue(get("/x").isFromCache(), "fresh before Expires");
        clock.advance(11);
        Check.isFalse(get("/x").isFromCache(), "stale after Expires");
        server.routeFixed("/inv", TestHttpServer.Response.ok("text/plain", "x").header("Expires", "0"));
        get("/inv");
        get("/inv");
        Check.equal(2, server.count("/inv"), "an invalid Expires date means already expired");
        final String lm = HttpDates.format(clock.now() - 1000000L);
        server.route("/heur", new TestHttpServer.Handler() {
            /**
             * Answers with Last-Modified 1000 seconds ago and no explicit freshness.
             * <p>
             * 1000초 전의 Last-Modified 를 주고 명시적 신선도는 주지 않는다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if (request.header("If-Modified-Since") != null) {
                    return new TestHttpServer.Response(304).header("Date", date());
                }
                return TestHttpServer.Response.ok("text/plain", "heur").header("Date", date()).header("Last-Modified", lm);
            }
        });
        get("/heur");
        clock.advance(90);
        Check.isTrue(get("/heur").isFromCache(), "heuristic lifetime is 10 percent of 1000 seconds");
        clock.advance(20);
        ResourceResponse stale = get("/heur");
        Check.isTrue(stale.isRevalidated(), "after 100 seconds the copy is revalidated");
    }

    /**
     * P19-07: an unsafe method that succeeds removes the stored copy; a failed one does not.
     * <p>
     * P19-07: 성공한 안전하지 않은 메서드는 저장본을 지우고 실패한 것은 지우지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsafeMethodInvalidates() throws Exception {
        server.route("/r", new TestHttpServer.Handler() {
            /**
             * Answers GET with a cacheable body and POST with a status chosen by the query.
             * <p>
             * GET 에는 캐시할 수 있는 본문을, POST 에는 질의로 고른 상태를 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                if ("POST".equals(request.method)) {
                    return new TestHttpServer.Response("fail".equals(request.query) ? 500 : 200).text("text/plain", "posted");
                }
                return TestHttpServer.Response.ok("text/plain", "page").header("Cache-Control", "max-age=60").header("Date", date());
            }
        });
        get("/r");
        Check.isTrue(get("/r").isFromCache(), "stored");
        ResourceRequest failing = new ResourceRequest(server.url("/r?fail")).setMethod("POST").setBody("x".getBytes("UTF-8"), "text/plain");
        fetcher.fetch(failing);
        Check.isTrue(get("/r").isFromCache(), "a failed POST (500) of another URL keeps the copy");
        ResourceRequest post = new ResourceRequest(server.url("/r")).setMethod("POST").setBody("x".getBytes("UTF-8"), "text/plain");
        Check.equal(200, fetcher.fetch(post).getStatus(), "the POST succeeds");
        Check.equal(0, cache.getEntryCount(), "the copy of the URL was removed");
        ResourceResponse after = get("/r");
        Check.isFalse(after.isFromCache(), "the next GET goes to the server");
        Check.equal(1, countOf("GET"), "request methods are visible on the server");
    }

    /**
     * Counts the recorded requests with a method.
     * <p>
     * 기록된 요청 중 메서드가 같은 것의 수를 센다.
     *
     * @param method the method / 메서드
     * @return count of requests that reached the server after the last POST / 마지막 POST 이후 서버에 닿은 요청 수
     */
    private int countOf(String method) {
        List<TestHttpServer.Request> all = server.requests();
        int n = 0;
        // 마지막 POST 뒤에 도착한 해당 메서드의 요청만 센다.
        for (int i = all.size() - 1; i >= 0; i--) {
            if ("POST".equals(all.get(i).method)) {
                break;
            }
            if (method.equals(all.get(i).method)) {
                n++;
            }
        }
        return n;
    }

    /**
     * P19-03: statuses and responses that must not be stored (500, redirect chains) and those that may (404 with max-age).
     * <p>
     * P19-03: 저장하면 안 되는 상태와 응답(500, 리디렉션 경로)과 저장할 수 있는 것(max-age 가 있는 404).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStorableStatuses() throws Exception {
        server.routeFixed("/err", new TestHttpServer.Response(500).text("text/plain", "oops").header("Cache-Control", "max-age=60"));
        get("/err");
        get("/err");
        Check.equal(2, server.count("/err"), "500 is not stored");
        server.routeFixed("/nf", new TestHttpServer.Response(404).text("text/plain", "none").header("Cache-Control", "max-age=60"));
        get("/nf");
        ResourceResponse nf = get("/nf");
        Check.isTrue(nf.isFromCache(), "404 with explicit freshness is stored");
        Check.equal(404, nf.getStatus(), "status of the stored response");
        server.routeFixed("/target", TestHttpServer.Response.ok("text/plain", "t").header("Cache-Control", "max-age=60"));
        server.routeFixed("/redir", TestHttpServer.Response.redirect(302, "/target"));
        get("/redir");
        ResourceResponse again = get("/redir");
        Check.isFalse(again.isFromCache(), "a response reached through a redirect is not stored");
        Check.equal(2, server.count("/redir"), "the redirect is requested again");
    }

    /**
     * P19-05: the capacity limit evicts the least recently used entry and an oversized body is not stored.
     * <p>
     * P19-05: 용량 제한은 가장 오래 쓰지 않은 항목을 제거하고 너무 큰 본문은 저장하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEvictionAndEntryLimit() throws Exception {
        char[] big = new char[1000];
        java.util.Arrays.fill(big, 'x');
        String body = new String(big);
        // 경로마다 같은 크기의 본문을 가진 고정 응답을 등록한다.
        for (String p : new String[] { "/p1", "/p2", "/p3" }) {
            server.routeFixed(p, TestHttpServer.Response.ok("text/plain", body).header("Cache-Control", "max-age=600"));
        }
        HttpCache small = new HttpCache(4000, 2000, clock);
        fetcher.setCache(small);
        get("/p1");
        get("/p2");
        Check.isTrue(get("/p1").isFromCache(), "p1 is used again, so p2 becomes the least recently used");
        get("/p3");
        Check.isTrue(small.getUsedBytes() <= 4000, "capacity respected: " + small.getUsedBytes());
        Check.equal(2, small.getEntryCount(), "two entries fit");
        Check.isTrue(get("/p1").isFromCache(), "p1 survived");
        Check.isTrue(get("/p3").isFromCache(), "p3 survived");
        Check.equal(3, server.count("/p1") + server.count("/p2") + server.count("/p3"), "so far one request per path");
        Check.isFalse(get("/p2").isFromCache(), "p2 was evicted");
        char[] huge = new char[3000];
        java.util.Arrays.fill(huge, 'y');
        server.routeFixed("/huge", TestHttpServer.Response.ok("text/plain", new String(huge)).header("Cache-Control", "max-age=600"));
        get("/huge");
        Check.isFalse(get("/huge").isFromCache(), "a body over the entry limit is not stored");
    }

    /**
     * P19-03: the body handler is asked about stored responses too: it may leave the body in memory, refuse it (download) or take it.
     * <p>
     * P19-03: 본문 처리기는 저장된 응답에 대해서도 결정한다. 메모리에 남기거나 거부(다운로드)하거나 가져갈 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBodyHandlerOnStoredResponses() throws Exception {
        countingRoute("/d", "max-age=60");
        ResourceRequest req = new ResourceRequest(server.url("/d"));
        ResourceResponse first = fetcher.fetch(req, new ResponseHandler() {
            /**
             * Keeps the body in memory.
             * <p>
             * 본문을 메모리에 둔다.
             *
             * @param head response head / 응답 머리
             * @return null / null
             */
            public OutputStream onResponse(ResourceResponse head) {
                return null;
            }
        });
        Check.equal("n=1", text(first), "in-memory response");
        ResourceResponse refused = fetcher.fetch(new ResourceRequest(server.url("/d")), new ResponseHandler() {
            /**
             * Refuses every response.
             * <p>
             * 모든 응답을 거부한다.
             *
             * @param head response head / 응답 머리
             * @return never / 반환하지 않음
             * @throws IOException always / 항상
             */
            public OutputStream onResponse(ResourceResponse head) throws IOException {
                throw new IOException("download required");
            }
        });
        Check.equal(FailureKind.IO, refused.getFailureKind(), "the handler's refusal is reported for a stored response");
        Check.equal(1, server.count("/d"), "and no request was sent");
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ResourceResponse taken = fetcher.fetch(new ResourceRequest(server.url("/d")), new ResponseHandler() {
            /**
             * Takes the body into a stream.
             * <p>
             * 본문을 스트림으로 가져간다.
             *
             * @param head response head / 응답 머리
             * @return the stream / 스트림
             */
            public OutputStream onResponse(ResourceResponse head) {
                return sink;
            }
        });
        Check.isTrue(taken.isStreamed(), "streamed response");
        Check.equal("n=1", new String(sink.toByteArray(), "UTF-8"), "the stored body went to the stream");
        Check.equal(0, taken.getBody().length, "no body in memory");
        Check.equal(1, server.count("/d"), "still no request");
        ResourceResponse streamedFromNetwork = fetcher.fetch(new ResourceRequest(server.url("/dd")), new ResponseHandler() {
            /**
             * Takes every body into a stream.
             * <p>
             * 모든 본문을 스트림으로 가져간다.
             *
             * @param head response head / 응답 머리
             * @return the stream / 스트림
             */
            public OutputStream onResponse(ResourceResponse head) {
                return new ByteArrayOutputStream();
            }
        });
        Check.equal(404, streamedFromNetwork.getStatus(), "unknown path");
        server.routeFixed("/dl", TestHttpServer.Response.ok("application/octet-stream", "file").header("Cache-Control", "max-age=60"));
        ResourceResponse download = fetcher.fetch(new ResourceRequest(server.url("/dl")), new ResponseHandler() {
            /**
             * Takes every body into a stream.
             * <p>
             * 모든 본문을 스트림으로 가져간다.
             *
             * @param head response head / 응답 머리
             * @return the stream / 스트림
             */
            public OutputStream onResponse(ResourceResponse head) {
                return new ByteArrayOutputStream();
            }
        });
        Check.isTrue(download.isStreamed(), "streamed from the network");
        ResourceResponse next = get("/dl");
        Check.isFalse(next.isFromCache(), "a response that went to a stream is not stored");
    }

    /**
     * P19-03: Set-Cookie is not replayed from the cache, and two components never share a cache.
     * <p>
     * P19-03: Set-Cookie 는 캐시에서 재생되지 않고 두 컴포넌트는 캐시를 공유하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCookiesNotReplayedAndComponentsNotShared() throws Exception {
        server.routeFixed("/ck", TestHttpServer.Response.ok("text/plain", "x").header("Set-Cookie", "a=1").header("Cache-Control", "max-age=60"));
        ResourceResponse first = get("/ck");
        Check.equal("a=1", first.getHeader("Set-Cookie"), "the network response keeps Set-Cookie");
        ResourceResponse hit = get("/ck");
        Check.isTrue(hit.isFromCache(), "stored");
        Check.isNull(hit.getHeader("Set-Cookie"), "Set-Cookie is not stored or replayed");
        ResourceFetcher otherComponent = new ResourceFetcher(null, FetchLimits.DEFAULT.withTimeouts(5000, 5000), null);
        HttpCache otherCache = new HttpCache(HttpCache.DEFAULT_MAX_BYTES, HttpCache.DEFAULT_MAX_ENTRY_BYTES, clock);
        otherComponent.setCache(otherCache);
        ResourceResponse other = otherComponent.fetch(new ResourceRequest(server.url("/ck")));
        Check.isFalse(other.isFromCache(), "the other component has its own cache");
        Check.equal(2, server.count("/ck"), "so the server was asked again");
        Check.equal(1, otherCache.getEntryCount(), "and the other cache holds its own copy");
    }

    /**
     * P19-05: clear() empties the cache; request headers that forbid caching skip the lookup but still store the answer.
     * <p>
     * P19-05: clear() 는 캐시를 비우고 캐시를 금지하는 요청 헤더는 조회를 건너뛰지만 응답은 저장한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClearAndRequestNoCache() throws Exception {
        countingRoute("/z", "max-age=60");
        get("/z");
        RequestHeaders h = new RequestHeaders();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("Cache-Control", "no-cache");
        h.addAll(m);
        ResourceResponse bypass = fetcher.fetch(new ResourceRequest(server.url("/z")).setExtraHeaders(h));
        Check.isFalse(bypass.isFromCache(), "the request's own no-cache skips the stored copy");
        Check.equal(2, server.count("/z"), "network used");
        Check.isTrue(get("/z").isFromCache(), "the answer was stored");
        cache.clear();
        Check.equal(0, cache.getEntryCount(), "cleared");
        Check.equal(0, cache.getUsedBytes(), "no bytes left");
        Check.isFalse(get("/z").isFromCache(), "after clear the network is used");
    }

    /**
     * P19-04: the date and Cache-Control parsers.
     * <p>
     * P19-04: 날짜와 Cache-Control 해석기.
     */
    public void testParsers() {
        Check.equal(784111777000L, HttpDates.parse("Sun, 06 Nov 1994 08:49:37 GMT"), "IMF-fixdate");
        Check.equal(784111777000L, HttpDates.parse("Sunday, 06-Nov-94 08:49:37 GMT"), "RFC 850 date");
        Check.equal(784111777000L, HttpDates.parse("Sun Nov  6 08:49:37 1994"), "asctime date");
        Check.equal(-1L, HttpDates.parse("0"), "invalid date");
        Check.equal(-1L, HttpDates.parse(null), "missing date");
        Check.equal("Sun, 06 Nov 1994 08:49:37 GMT", HttpDates.format(784111777000L), "formatting");
        List<String> lines = new ArrayList<String>();
        lines.add("Max-Age=60, NO-CACHE=\"set-cookie, x\"");
        lines.add("max-age=5, public");
        CacheControl cc = CacheControl.parse(lines);
        Check.equal(60L, cc.getSeconds("max-age"), "the first value of a repeated directive wins, names are case-insensitive");
        Check.equal("set-cookie, x", cc.get("no-cache"), "quoted values may contain commas");
        Check.isTrue(cc.has("public"), "bare directive");
        List<String> bad = new ArrayList<String>();
        bad.add("max-age=-1, s-maxage=abc, x=99999999999999999999");
        CacheControl b = CacheControl.parse(bad);
        Check.equal(-1L, b.getSeconds("max-age"), "negative is invalid");
        Check.equal(-1L, b.getSeconds("s-maxage"), "non-numeric is invalid");
        Check.equal((long) Integer.MAX_VALUE, b.getSeconds("x"), "huge values are capped");
    }
}
