package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * The HTTP cache of one browser component (plan items P19-03 to P19-06), a private cache in the sense of RFC 9111: it keeps successful
 * <code>GET</code> responses of the main document and of sub-resources in memory, answers repeated requests without contacting the server
 * while they are fresh, revalidates stale copies with a conditional request (<code>If-None-Match</code>, <code>If-Modified-Since</code>) and
 * merges the headers of a <code>304 Not Modified</code> into the stored copy, so a 304 never shows up as an empty page. Every component has
 * its own cache object, so responses are never shared between components; the cache holds no reference to a component.
 * <p>
 * Rules (summary): only <code>GET</code> responses with status 200, 203, 204, 300, 404, 405, 410, 414 or 501 that arrive without redirect, without
 * failure, in memory (not streamed) and below the entry size limit are stored; <code>Cache-Control: no-store</code>, <code>Vary: *</code> and
 * <code>Vary: Cookie</code> prevent storing; <code>Set-Cookie</code> headers are never stored (cookies are handled once, when the response arrives).
 * Freshness comes from <code>max-age</code>, then <code>Expires</code>, then the heuristic of 10 % of the time since
 * <code>Last-Modified</code> (at most 24 hours); <code>no-cache</code> always revalidates; <code>Vary</code> selects the stored variant by the
 * request headers that this layer controls (User-Agent, extra headers, Referer). A successful unsafe request (POST, PUT, DELETE, PATCH)
 * removes the stored copies of its URL and of the URLs in <code>Location</code> and <code>Content-Location</code>. Request modes:
 * {@link ResourceRequest.CacheMode#DEFAULT} follows the rules above, {@link ResourceRequest.CacheMode#FORCE_RELOAD} skips the stored copy
 * and replaces it with the new response, {@link ResourceRequest.CacheMode#PREFER_CACHE} (history navigation) uses a stored copy even when it
 * is stale. A response that was reached through a redirect is not stored because the redirect response itself is not visible here.
 * Capacity is limited in bytes with least-recently-used eviction; a copy that a response object already returned stays valid after eviction.
 * <p>
 * 브라우저 컴포넌트 하나의 HTTP 캐시(계획 P19-03~P19-06)로 RFC 9111 의 사설 캐시에 해당한다. 주 문서와 하위 리소스의 성공한 <code>GET</code>
 * 응답을 메모리에 보관하고, 신선한 동안에는 서버에 접속하지 않고 같은 요청에 답하며, 오래된 사본은 조건부 요청(<code>If-None-Match</code>,
 * <code>If-Modified-Since</code>)으로 재검증하고 <code>304 Not Modified</code> 의 헤더를 저장본에 합치므로 304 가 빈 페이지로 보이지 않는다.
 * 컴포넌트마다 자신의 캐시 객체를 가지므로 응답이 컴포넌트 사이에 공유되지 않으며, 캐시는 컴포넌트를 참조하지 않는다.
 * <p>
 * 규칙(요약): 리디렉션/실패/스트리밍 없이 메모리로 도착했고 항목 크기 제한 이하인 상태 200, 203, 204, 300, 404, 405, 410, 414, 501 의
 * <code>GET</code> 응답만 저장한다. <code>Cache-Control: no-store</code>, <code>Vary: *</code>, <code>Vary: Cookie</code> 는 저장을 막고
 * <code>Set-Cookie</code> 헤더는 저장하지 않는다(쿠키는 응답이 도착할 때 한 번만 처리). 신선도는 <code>max-age</code>, <code>Expires</code>,
 * <code>Last-Modified</code> 이후 시간의 10 %(최대 24시간) 휴리스틱 순으로 정한다. <code>no-cache</code> 는 항상 재검증하고 <code>Vary</code> 는
 * 이 계층이 제어하는 요청 헤더(User-Agent, 추가 헤더, Referer)로 저장된 변형을 고른다. 성공한 안전하지 않은 요청(POST, PUT, DELETE, PATCH)은 그
 * URL 과 <code>Location</code>/<code>Content-Location</code> 의 URL 의 저장본을 지운다. 요청 모드: {@link ResourceRequest.CacheMode#DEFAULT} 는
 * 위 규칙, {@link ResourceRequest.CacheMode#FORCE_RELOAD} 는 저장본을 건너뛰고 새 응답으로 바꾸며, {@link ResourceRequest.CacheMode#PREFER_CACHE}
 * (이력 이동)는 오래된 저장본도 쓴다. 리디렉션을 거친 응답은 리디렉션 응답 자체가 보이지 않으므로 저장하지 않는다. 용량은 바이트로 제한하며 가장
 * 오래 쓰지 않은 항목부터 제거한다. 이미 응답 객체가 돌려준 사본은 제거된 뒤에도 유효하다.
 * <p>
 * Use: <code>fetcher.setCache(new HttpCache())</code>; the {@link ResourceFetcher} then consults the cache for every http/https GET.
 */
public final class HttpCache {

    /**
     * The clock of the cache (replaceable in tests).
     * <p>
     * 캐시가 쓰는 시계(시험에서 바꿀 수 있다).
     */
    public interface Clock {
        /**
         * Returns the current time.
         * <p>
         * 현재 시각을 반환한다.
         *
         * @return milliseconds since 1970-01-01T00:00:00Z / 1970-01-01T00:00:00Z 이후 밀리초
         */
        long now();
    }

    /**
     * The network access that the cache wraps.
     * <p>
     * 캐시가 감싸는 네트워크 접근.
     */
    interface Network {
        /**
         * Fetches a request from the network.
         * <p>
         * 요청을 네트워크에서 가져온다.
         *
         * @param request the request / 요청
         * @param handler body handler or null / 본문 처리기 또는 null
         * @return the response / 응답
         */
        ResourceResponse fetch(ResourceRequest request, ResponseHandler handler);
    }

    /**
     * Default capacity of the cache in bytes: 32 MiB.
     * <p>
     * 캐시의 기본 용량(바이트): 32 MiB.
     */
    public static final long DEFAULT_MAX_BYTES = 32L * 1024 * 1024;

    /**
     * Default maximum size of one stored body in bytes: 8 MiB.
     * <p>
     * 저장하는 본문 하나의 기본 최대 크기(바이트): 8 MiB.
     */
    public static final long DEFAULT_MAX_ENTRY_BYTES = 8L * 1024 * 1024;

    /**
     * Upper limit of the heuristic freshness lifetime: 24 hours in milliseconds.
     * <p>
     * 휴리스틱 신선 기간의 상한: 24시간(밀리초).
     */
    private static final long HEURISTIC_CAP_MS = 24L * 60 * 60 * 1000;

    /**
     * Header names that are never stored or never updated by a 304 (lower case).
     * <p>
     * 저장하지 않거나 304 로 갱신하지 않는 헤더 이름(소문자).
     */
    private static final String[] NOT_UPDATED = { "content-length", "content-encoding", "content-range", "content-type", "transfer-encoding",
        "connection", "keep-alive", "set-cookie", "set-cookie2" };

    /**
     * Statuses that may be stored without explicit freshness information (RFC 9110 section 15.1 minus partial content and redirects).
     * <p>
     * 명시적 신선도 정보 없이 저장할 수 있는 상태 코드(RFC 9110 15.1 에서 부분 콘텐츠와 리디렉션을 뺀 것).
     */
    private static final int[] STORABLE_STATUS = { 200, 203, 204, 300, 404, 405, 410, 414, 501 };

    /**
     * One stored response variant.
     * <p>
     * 저장된 응답 변형 하나.
     */
    private static final class Entry {
        /**
         * Cache key (URL without fragment).
         * <p>
         * 캐시 키(조각이 없는 URL).
         */
        final String key;
        /**
         * Final URL of the response.
         * <p>
         * 응답의 최종 URL.
         */
        final String finalUrl;
        /**
         * Status code.
         * <p>
         * 상태 코드.
         */
        final int status;
        /**
         * Reason phrase.
         * <p>
         * 이유 문구.
         */
        final String reason;
        /**
         * Stored headers by lower-case name.
         * <p>
         * 소문자 이름별 저장된 헤더.
         */
        final Map<String, List<String>> headers;
        /**
         * Decoded body (shared with returned responses, never modified).
         * <p>
         * 해독된 본문(반환된 응답과 공유하며 수정하지 않는다).
         */
        final byte[] body;
        /**
         * Media type.
         * <p>
         * 미디어 타입.
         */
        final String mimeType;
        /**
         * Charset or null.
         * <p>
         * 문자셋 또는 null.
         */
        final String charset;
        /**
         * Names of the request headers selected by Vary (lower case).
         * <p>
         * Vary 가 고른 요청 헤더 이름(소문자).
         */
        final List<String> varyNames;
        /**
         * Values of those request headers at the time of the request (null for absent).
         * <p>
         * 요청 당시 그 요청 헤더의 값(없으면 null).
         */
        final List<String> varyValues;
        /** Credentialless partition selected by the initiating origin. / 시작 오리진이 선택한 credentialless 캐시 분기. */
        final String credentialPartition;
        /**
         * Time the request was sent.
         * <p>
         * 요청을 보낸 시각.
         */
        long requestTime;
        /**
         * Time the response was received.
         * <p>
         * 응답을 받은 시각.
         */
        long responseTime;
        /**
         * Parsed Cache-Control of the stored headers.
         * <p>
         * 저장된 헤더의 해석된 Cache-Control.
         */
        CacheControl cc;

        /**
         * Creates an entry.
         * <p>
         * 항목을 만든다.
         *
         * @param key        cache key / 캐시 키
         * @param response   the response to copy / 복사할 응답
         * @param varyNames  Vary header names / Vary 헤더 이름
         * @param varyValues Vary header values / Vary 헤더 값
         * @param credentialsPartition the credentialless partition / credentialless 캐시 분기
         * @param start      request time / 요청 시각
         * @param end        response time / 응답 시각
         */
        Entry(String key, ResourceResponse response, List<String> varyNames, List<String> varyValues, String credentialsPartition,
                long start, long end) {
            this.key = key;
            this.finalUrl = response.getFinalUrl();
            this.status = response.getStatus();
            this.reason = response.getReason();
            this.headers = new LinkedHashMap<String, List<String>>();
            // 응답 헤더를 복사하되 쿠키 관련 헤더는 저장하지 않는다.
            for (String name : response.getHeaderNames()) {
                if (!"set-cookie".equals(name) && !"set-cookie2".equals(name)) {
                    headers.put(name, new ArrayList<String>(response.getHeaders(name)));
                }
            }
            this.body = response.getBody();
            this.mimeType = response.getMimeType();
            this.charset = response.getCharset();
            this.varyNames = varyNames;
            this.varyValues = varyValues;
            this.credentialPartition = credentialsPartition;
            this.requestTime = start;
            this.responseTime = end;
            this.cc = CacheControl.parse(headers.get("cache-control"));
        }

        /**
         * Returns the first value of a stored header.
         * <p>
         * 저장된 헤더의 첫 값을 반환한다.
         *
         * @param name lower-case header name / 소문자 헤더 이름
         * @return value or null / 값 또는 null
         */
        String header(String name) {
            List<String> v = headers.get(name);
            return v == null || v.isEmpty() ? null : v.get(0);
        }

        /**
         * Returns the memory used by the entry (body plus an allowance for headers and bookkeeping).
         * <p>
         * 항목이 쓰는 메모리를 반환한다(본문과 헤더/관리 정보의 여유분).
         *
         * @return bytes / 바이트
         */
        long size() {
            long n = body.length + 512;
            // 헤더 문자열의 길이를 더한다.
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                n += e.getKey().length();
                // 헤더 값 문자열의 길이를 더한다.
                for (String v : e.getValue()) {
                    n += v.length();
                }
            }
            return n;
        }
    }

    /**
     * Clock used by this cache.
     * <p>
     * 이 캐시가 쓰는 시계.
     */
    private final Clock clock;
    /**
     * Capacity in bytes.
     * <p>
     * 용량(바이트).
     */
    private final long maxBytes;
    /**
     * Maximum size of one entry in bytes.
     * <p>
     * 항목 하나의 최대 크기(바이트).
     */
    private final long maxEntryBytes;
    /**
     * Stored variants by cache key.
     * <p>
     * 캐시 키별 저장된 변형.
     */
    private final Map<String, List<Entry>> byKey = new HashMap<String, List<Entry>>();
    /**
     * All entries in access order (the eldest is evicted first).
     * <p>
     * 접근 순서로 둔 모든 항목(가장 오래된 것을 먼저 제거한다).
     */
    private final LinkedHashMap<Entry, Boolean> lru = new LinkedHashMap<Entry, Boolean>(16, 0.75f, true);
    /**
     * Bytes in use.
     * <p>
     * 사용 중인 바이트.
     */
    private long usedBytes;
    /**
     * Number of responses served without contacting the server.
     * <p>
     * 서버에 접속하지 않고 응답한 횟수.
     */
    private long hits;
    /**
     * Number of conditional requests sent.
     * <p>
     * 보낸 조건부 요청 수.
     */
    private long revalidations;
    /**
     * Number of conditional requests that the server answered with 304.
     * <p>
     * 서버가 304 로 답한 조건부 요청 수.
     */
    private long notModified;
    /**
     * Number of responses stored.
     * <p>
     * 저장한 응답 수.
     */
    private long stores;

    /**
     * Creates a cache with the default capacity and the system clock.
     * <p>
     * 기본 용량과 시스템 시계로 캐시를 만든다.
     */
    public HttpCache() {
        this(DEFAULT_MAX_BYTES, DEFAULT_MAX_ENTRY_BYTES, null);
    }

    /**
     * Creates a cache.
     * <p>
     * 캐시를 만든다.
     *
     * @param maxBytes      capacity in bytes (at least 1) / 용량(바이트, 1 이상)
     * @param maxEntryBytes largest body that is stored (at least 0) / 저장하는 가장 큰 본문(0 이상)
     * @param clock         the clock, null for the system clock / 시계. null 이면 시스템 시계
     */
    public HttpCache(long maxBytes, long maxEntryBytes, Clock clock) {
        if (maxBytes < 1 || maxEntryBytes < 0) {
            throw new IllegalArgumentException("invalid cache size");
        }
        this.maxBytes = maxBytes;
        this.maxEntryBytes = maxEntryBytes;
        this.clock = clock != null ? clock : new Clock() {
            /**
             * Returns the system time.
             * <p>
             * 시스템 시각을 반환한다.
             *
             * @return current time / 현재 시각
             */
            public long now() {
                return System.currentTimeMillis();
            }
        };
    }

    /**
     * Removes everything from the cache (used by clean and dispose of the component).
     * <p>
     * 캐시를 모두 비운다(컴포넌트의 clean 과 dispose 가 쓴다).
     */
    public synchronized void clear() {
        byKey.clear();
        lru.clear();
        usedBytes = 0;
    }

    /**
     * Returns the number of stored variants.
     * <p>
     * 저장된 변형의 수를 반환한다.
     *
     * @return entries / 항목 수
     */
    public synchronized int getEntryCount() {
        return lru.size();
    }

    /**
     * Returns the bytes in use.
     * <p>
     * 사용 중인 바이트를 반환한다.
     *
     * @return bytes / 바이트
     */
    public synchronized long getUsedBytes() {
        return usedBytes;
    }

    /**
     * Returns how many responses were served from the cache without contacting the server.
     * <p>
     * 서버에 접속하지 않고 캐시에서 응답한 횟수를 반환한다.
     *
     * @return hits / 적중 횟수
     */
    public synchronized long getHitCount() {
        return hits;
    }

    /**
     * Returns how many conditional requests were sent.
     * <p>
     * 보낸 조건부 요청 수를 반환한다.
     *
     * @return revalidations / 재검증 횟수
     */
    public synchronized long getRevalidationCount() {
        return revalidations;
    }

    /**
     * Returns how many conditional requests were confirmed with 304.
     * <p>
     * 304 로 확인된 조건부 요청 수를 반환한다.
     *
     * @return confirmations / 확인 횟수
     */
    public synchronized long getNotModifiedCount() {
        return notModified;
    }

    /**
     * Returns how many responses were stored.
     * <p>
     * 저장한 응답 수를 반환한다.
     *
     * @return stores / 저장 횟수
     */
    public synchronized long getStoreCount() {
        return stores;
    }

    /**
     * Removes every stored variant of a URL.
     * <p>
     * URL 의 저장된 변형을 모두 제거한다.
     *
     * @param url the URL (a fragment is ignored) / URL(조각은 무시)
     */
    public synchronized void invalidate(String url) {
        List<Entry> list = byKey.remove(keyOf(url));
        if (list == null) {
            return;
        }
        // 이 URL 의 모든 변형을 용량 계산에서 뺀다.
        for (Entry e : list) {
            lru.remove(e);
            usedBytes -= e.size();
        }
    }

    /**
     * Applies the invalidation rules after an unsafe request (RFC 9111 section 4.4): when the response is not an error, the copies of the
     * request URL and of the same-origin URLs in <code>Location</code> and <code>Content-Location</code> are removed.
     * <p>
     * 안전하지 않은 요청 뒤에 무효화 규칙을 적용한다(RFC 9111 4.4). 응답이 오류가 아니면 요청 URL 과 <code>Location</code>,
     * <code>Content-Location</code> 의 같은 오리진 URL 의 사본을 제거한다.
     *
     * @param request  the unsafe request / 안전하지 않은 요청
     * @param response its response / 그 응답
     */
    void invalidateAfterUnsafe(ResourceRequest request, ResourceResponse response) {
        if (response.getFailureKind() != FailureKind.NONE || response.getStatus() >= 400) {
            return;
        }
        invalidate(request.getUrl());
        Origin base = Origin.of(request.getUrl());
        String[] names = { "location", "content-location" };
        // 두 헤더가 가리키는 같은 오리진 URL 의 사본도 제거한다.
        for (int i = 0; i < names.length; i++) {
            String v = response.getHeader(names[i]);
            if (v == null || v.length() == 0) {
                continue;
            }
            try {
                String target = UrlUtil.resolve(request.getUrl(), v);
                if (base.isSameOrigin(Origin.of(target))) {
                    invalidate(target);
                }
            } catch (IllegalArgumentException e) {
                // 해석할 수 없는 주소는 무시한다.
                continue;
            }
        }
    }

    /**
     * Fetches a GET request through the cache.
     * <p>
     * GET 요청을 캐시를 거쳐 가져온다.
     *
     * @param req     the request (method GET) / 요청(메서드 GET)
     * @param handler body handler that decides where bodies go (also asked for stored responses), or null / 본문의 목적지를 정하는 처리기(저장된 응답에도 묻는다). 없으면 null
     * @param net     the network access / 네트워크 접근
     * @return the response; {@link ResourceResponse#isFromCache()} and {@link ResourceResponse#isRevalidated()} tell where it came from / 응답. 출처는 {@link ResourceResponse#isFromCache()}, {@link ResourceResponse#isRevalidated()} 로 알 수 있다
     */
    ResourceResponse fetch(ResourceRequest req, ResponseHandler handler, Network net) {
        String key = keyOf(req.getUrl());
        ResourceRequest.CacheMode mode = req.getCacheMode();
        boolean bypass = mode == ResourceRequest.CacheMode.FORCE_RELOAD || requestForbidsCache(req);
        Entry entry = bypass ? null : find(key, req);
        if (entry != null) {
            ResourceResponse hit = tryServe(entry, req, handler, mode, net, key);
            if (hit != null) {
                return hit;
            }
        }
        long start = clock.now();
        ResourceResponse response = net.fetch(req, handler);
        long end = clock.now();
        store(key, req, response, start, end);
        return response;
    }

    /**
     * Lets the body handler decide about a response that is already in memory (a stored copy, or a changed response to a conditional
     * request): the handler may refuse it (the failure is reported like a refusal during the transfer), take the body into a stream or
     * leave it in memory.
     * <p>
     * 이미 메모리에 있는 응답(저장본, 또는 조건부 요청에 대한 바뀐 응답)에 대해 본문 처리기가 결정하게 한다. 처리기는 거부할 수 있고(전송 중
     * 거부와 같은 실패로 보고), 본문을 스트림으로 가져가거나 메모리에 남겨 둘 수 있다.
     *
     * @param req the request used for policy checks / 정책을 검사할 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @param r       the response / 응답
     * @return the response to return to the caller / 호출자에게 돌려줄 응답
     */
    private static ResourceResponse applyHandler(ResourceRequest req, ResponseHandler handler, ResourceResponse r) {
        // 다른 정책에서 저장한 응답도 현재 문서의 정책을 통과해야 한다.
        if (r.getFailureKind() == FailureKind.NONE && !CrossOriginPolicy.permits(req, r)) {
            return CrossOriginPolicy.blocked(r);
        }
        boolean observe = handler instanceof ResponseHandler.BodyObserver;
        if (handler == null || r.getFailureKind() != FailureKind.NONE || (r.getStatus() >= 400 && !observe)) {
            return r;
        }
        OutputStream sink;
        try {
            sink = handler.onResponse(r);
        } catch (IOException e) {
            return ResourceResponse.failure(r.getRequestUrl(), FailureKind.IO, e.getMessage());
        }
        if (sink == null) {
            return r;
        }
        try {
            try {
                sink.write(r.getBody());
                sink.flush();
            } finally {
                sink.close();
            }
        } catch (IOException e) {
            return ResourceResponse.failure(r.getRequestUrl(), FailureKind.IO, e.getMessage());
        }
        // 본문 관찰자에게 사본을 전달한 경우 저장 응답의 기존 정보와 바이트를 유지한다.
        if (observe) {
            return r;
        }
        // 본문을 스트림으로 보냈으므로 메모리 본문이 없는 같은 응답을 새로 만든다.
        ResourceResponse streamed = new ResourceResponse(r.getRequestUrl());
        streamed.setFinalUrl(r.getFinalUrl());
        streamed.setStatus(r.getStatus(), r.getReason());
        // 스트림으로 보낸 응답에 원래 헤더를 모두 복사한다.
        for (String name : r.getHeaderNames()) {
            // 같은 이름의 헤더 값을 하나씩 복사한다.
            for (String v : r.getHeaders(name)) {
                streamed.addHeader(name, v);
            }
        }
        streamed.setContentType(r.getMimeType(), r.getCharset());
        streamed.setDeclaredLength(r.getDeclaredLength());
        streamed.setStreamed(r.getBody().length);
        if (r.isRevalidated()) {
            streamed.markFromCache(true);
        } else if (r.isFromCache()) {
            streamed.markFromCache(false);
        }
        return streamed;
    }

    /**
     * Serves a request from a stored entry if the rules allow: a fresh entry (or any entry in prefer-cache mode) is returned directly, a
     * stale entry with a validator is revalidated with a conditional request.
     * <p>
     * 규칙이 허용하면 저장된 항목으로 요청에 응답한다. 신선한 항목(또는 캐시 우선 모드의 모든 항목)은 바로 반환하고 검증자가 있는 오래된 항목은 조건부
     * 요청으로 재검증한다.
     *
     * @param entry   the stored variant / 저장된 변형
     * @param req     the request / 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @param mode    the cache mode / 캐시 모드
     * @param net     the network / 네트워크
     * @param key     the cache key / 캐시 키
     * @return the response, or null if the request must go to the network without a validator / 응답. 검증자 없이 네트워크로 가야 하면 null
     */
    private ResourceResponse tryServe(Entry entry, ResourceRequest req, ResponseHandler handler, ResourceRequest.CacheMode mode,
            Network net, String key) {
        long now = clock.now();
        boolean fresh;
        synchronized (this) {
            fresh = isFresh(entry, now);
        }
        if (fresh || mode == ResourceRequest.CacheMode.PREFER_CACHE) {
            ResourceResponse hit;
            synchronized (this) {
                hits++;
                hit = toResponse(entry, req.getUrl(), now, false);
            }
            return applyHandler(req, handler, hit);
        }
        String etag;
        String lastModified;
        synchronized (this) {
            etag = entry.header("etag");
            lastModified = HttpDates.parse(entry.header("last-modified")) >= 0 ? entry.header("last-modified") : null;
        }
        if (etag == null && lastModified == null) {
            return null;
        }
        ResourceRequest conditional = conditionalCopy(req, etag, lastModified);
        if (conditional == null) {
            return null;
        }
        synchronized (this) {
            revalidations++;
        }
        long start = clock.now();
        // 조건부 요청의 본문은 메모리로 받고, 처리기의 결정은 응답이 정해진 뒤에 적용한다.
        ResourceResponse response = net.fetch(conditional, null);
        long end = clock.now();
        if (response.getFailureKind() == FailureKind.NONE && response.getStatus() == 304) {
            ResourceResponse confirmed;
            synchronized (this) {
                notModified++;
                merge(entry, response, start, end);
                confirmed = toResponse(entry, req.getUrl(), end, true);
            }
            return applyHandler(req, handler, confirmed);
        }
        store(key, req, response, start, end);
        return applyHandler(req, handler, response);
    }

    /**
     * Creates the conditional copy of a request, or null if the caller already sent conditional headers or the validators cannot be sent.
     * <p>
     * 요청의 조건부 복사본을 만든다. 호출자가 이미 조건부 헤더를 보냈거나 검증자를 보낼 수 없으면 null.
     *
     * @param req          original request / 원래 요청
     * @param etag         the stored ETag or null / 저장된 ETag 또는 null
     * @param lastModified the stored Last-Modified or null / 저장된 Last-Modified 또는 null
     * @return the copy or null / 복사본 또는 null
     */
    private static ResourceRequest conditionalCopy(ResourceRequest req, String etag, String lastModified) {
        ResourceRequest c = req.copy();
        RequestHeaders h = c.getExtraHeaders() == null ? new RequestHeaders() : c.getExtraHeaders();
        if (h.get("If-None-Match") != null || h.get("If-Modified-Since") != null) {
            return null;
        }
        Map<String, String> add = new LinkedHashMap<String, String>();
        if (etag != null) {
            add.put("If-None-Match", etag);
        }
        if (lastModified != null) {
            add.put("If-Modified-Since", lastModified);
        }
        try {
            h.addAll(add);
        } catch (IllegalArgumentException e) {
            return null;
        }
        c.setExtraHeaders(h);
        return c;
    }

    /**
     * Tells whether the request itself asks to bypass stored copies (<code>Cache-Control: no-cache</code>, <code>no-store</code>,
     * <code>max-age=0</code> or <code>Pragma: no-cache</code> in the component's extra headers).
     * <p>
     * 요청 자체가 저장본을 우회하라고 요구하는지 알려 준다(컴포넌트의 추가 헤더에 <code>Cache-Control: no-cache</code>, <code>no-store</code>,
     * <code>max-age=0</code>, <code>Pragma: no-cache</code> 가 있는 경우).
     *
     * @param req the request / 요청
     * @return true to skip the lookup / 조회를 건너뛰려면 true
     */
    private static boolean requestForbidsCache(ResourceRequest req) {
        RequestHeaders h = req.getExtraHeaders();
        if (h == null) {
            return false;
        }
        String cc = h.get("Cache-Control");
        if (cc != null) {
            List<String> lines = new ArrayList<String>();
            lines.add(cc);
            CacheControl parsed = CacheControl.parse(lines);
            if (parsed.has("no-cache") || parsed.has("no-store") || parsed.getSeconds("max-age") == 0) {
                return true;
            }
        }
        String pragma = h.get("Pragma");
        return pragma != null && pragma.toLowerCase(Locale.ENGLISH).indexOf("no-cache") >= 0;
    }

    /**
     * Finds the stored variant that matches the request's Vary headers and marks it as recently used.
     * <p>
     * 요청의 Vary 헤더와 맞는 저장된 변형을 찾고 최근에 쓴 것으로 표시한다.
     *
     * @param key cache key / 캐시 키
     * @param req the request / 요청
     * @return the entry or null / 항목 또는 null
     */
    private synchronized Entry find(String key, ResourceRequest req) {
        List<Entry> list = byKey.get(key);
        if (list == null) {
            return null;
        }
        String partition = CrossOriginPolicy.cachePartition(req);
        // 변형마다 Vary 로 고른 요청 헤더가 지금 요청과 같은지 비교한다.
        for (Entry e : list) {
            boolean same = partition.equals(e.credentialPartition);
            // Vary 로 고른 헤더가 모두 같은지 차례로 비교한다.
            for (int i = 0; i < e.varyNames.size() && same; i++) {
                String now = requestHeader(req, e.varyNames.get(i));
                String then = e.varyValues.get(i);
                same = now == null ? then == null : now.equals(then);
            }
            if (same) {
                lru.get(e);
                return e;
            }
        }
        return null;
    }

    /**
     * Returns the value of a request header as this layer would send it, for matching Vary.
     * <p>
     * 이 계층이 보낼 요청 헤더의 값을 Vary 비교용으로 반환한다.
     *
     * @param req  the request / 요청
     * @param name lower-case header name / 소문자 헤더 이름
     * @return the value, null if the header is not sent / 값. 보내지 않으면 null
     */
    private static String requestHeader(ResourceRequest req, String name) {
        if ("user-agent".equals(name)) {
            return req.getUserAgent();
        }
        if ("accept-encoding".equals(name)) {
            return "gzip, deflate";
        }
        if ("referer".equals(name)) {
            return req.getReferrer();
        }
        RequestHeaders h = req.getExtraHeaders();
        return h == null ? null : h.get(name);
    }

    /**
     * Stores a response if the rules allow it.
     * <p>
     * 규칙이 허용하면 응답을 저장한다.
     *
     * @param key  cache key / 캐시 키
     * @param req  the request / 요청
     * @param resp the response / 응답
     * @param start time the request was sent / 요청을 보낸 시각
     * @param end   time the response arrived / 응답이 도착한 시각
     */
    private void store(String key, ResourceRequest req, ResourceResponse resp, long start, long end) {
        if (!isStorable(req, resp)) {
            return;
        }
        List<String> varyNames = new ArrayList<String>();
        List<String> varyValues = new ArrayList<String>();
        // Vary 에 나열된 헤더마다 이 요청의 값을 기록한다.
        for (String line : resp.getHeaders("vary")) {
            String[] parts = line.split(",");
            // Vary 에 나열된 이름을 하나씩 기록한다.
            for (int i = 0; i < parts.length; i++) {
                String n = parts[i].trim().toLowerCase(Locale.ENGLISH);
                if (n.length() > 0 && !varyNames.contains(n)) {
                    varyNames.add(n);
                    varyValues.add(requestHeader(req, n));
                }
            }
        }
        String partition = CrossOriginPolicy.cachePartition(req);
        Entry e = new Entry(key, resp, varyNames, varyValues, partition, start, end);
        synchronized (this) {
            // clean의 취소 후 도착한 응답이 비운 캐시를 다시 채우지 못하게 한다.
            if (req.getCancelToken().isCancelled()) {
                return;
            }
            List<Entry> list = byKey.get(key);
            if (list == null) {
                list = new ArrayList<Entry>();
                byKey.put(key, list);
            }
            // 같은 Vary 값을 가진 이전 변형은 새 응답으로 바꾼다.
            for (Iterator<Entry> it = list.iterator(); it.hasNext();) {
                Entry old = it.next();
                if (partition.equals(old.credentialPartition) && old.varyNames.equals(varyNames) && old.varyValues.equals(varyValues)) {
                    it.remove();
                    lru.remove(old);
                    usedBytes -= old.size();
                }
            }
            list.add(e);
            lru.put(e, Boolean.TRUE);
            usedBytes += e.size();
            stores++;
            evict();
        }
    }

    /**
     * Removes the least recently used entries until the cache fits its capacity.
     * <p>
     * 캐시가 용량에 맞을 때까지 가장 오래 쓰지 않은 항목을 제거한다. 호출자는 잠금을 잡고 있어야 한다.
     */
    private void evict() {
        // 용량을 넘는 동안 가장 오래된 항목부터 제거한다.
        while (usedBytes > maxBytes && !lru.isEmpty()) {
            Entry eldest = lru.keySet().iterator().next();
            lru.remove(eldest);
            usedBytes -= eldest.size();
            List<Entry> list = byKey.get(eldest.key);
            if (list != null) {
                list.remove(eldest);
                if (list.isEmpty()) {
                    byKey.remove(eldest.key);
                }
            }
        }
    }

    /**
     * Tells whether a response may be stored.
     * <p>
     * 응답을 저장해도 되는지 알려 준다.
     *
     * @param req  the request / 요청
     * @param resp the response / 응답
     * @return true if storable / 저장할 수 있으면 true
     */
    private boolean isStorable(ResourceRequest req, ResourceResponse resp) {
        if (!"GET".equals(req.getMethod()) || resp.getFailureKind() != FailureKind.NONE || resp.isStreamed()) {
            return false;
        }
        if (!resp.getRedirects().isEmpty() || resp.isHttpFallbackUsed() || resp.getBody().length > maxEntryBytes) {
            return false;
        }
        boolean statusOk = false;
        // 저장할 수 있는 상태 코드 목록에 있는지 확인한다.
        for (int i = 0; i < STORABLE_STATUS.length; i++) {
            if (STORABLE_STATUS[i] == resp.getStatus()) {
                statusOk = true;
            }
        }
        if (!statusOk) {
            return false;
        }
        CacheControl cc = CacheControl.parse(resp.getHeaders("cache-control"));
        if (cc.has("no-store")) {
            return false;
        }
        // Vary: * 와 Vary: Cookie 는 이 캐시가 일치 여부를 알 수 없으므로 저장하지 않는다.
        for (String line : resp.getHeaders("vary")) {
            String[] parts = line.split(",");
            // Vary 에 나열된 이름을 하나씩 검사한다.
            for (int i = 0; i < parts.length; i++) {
                String n = parts[i].trim().toLowerCase(Locale.ENGLISH);
                if ("*".equals(n) || "cookie".equals(n)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether a stored entry is fresh at a time.
     * <p>
     * 저장된 항목이 어떤 시각에 신선한지 알려 준다. 호출자는 잠금을 잡고 있어야 한다.
     *
     * @param e   the entry / 항목
     * @param now the time / 시각
     * @return true if it may be used without revalidation / 재검증 없이 쓸 수 있으면 true
     */
    private boolean isFresh(Entry e, long now) {
        // 값이 없는 no-cache 는 항상 재검증하게 한다(값이 있는 한정형은 무시한다).
        if (e.cc.has("no-cache") && "".equals(e.cc.get("no-cache"))) {
            return false;
        }
        return freshnessLifetime(e) > currentAge(e, now);
    }

    /**
     * Computes the freshness lifetime of an entry in milliseconds: max-age, else Expires minus Date, else the heuristic.
     * <p>
     * 항목의 신선 기간(밀리초)을 계산한다: max-age, 없으면 Expires 에서 Date 를 뺀 값, 없으면 휴리스틱.
     *
     * @param e the entry / 항목
     * @return lifetime in milliseconds / 신선 기간(밀리초)
     */
    private static long freshnessLifetime(Entry e) {
        long maxAge = e.cc.getSeconds("max-age");
        if (maxAge >= 0) {
            return maxAge * 1000L;
        }
        long date = HttpDates.parse(e.header("date"));
        if (date < 0) {
            date = e.responseTime;
        }
        String expires = e.header("expires");
        if (expires != null) {
            long exp = HttpDates.parse(expires);
            return exp < 0 ? 0 : Math.max(0, exp - date);
        }
        long lastModified = HttpDates.parse(e.header("last-modified"));
        if (lastModified >= 0 && date > lastModified) {
            return Math.min((date - lastModified) / 10, HEURISTIC_CAP_MS);
        }
        return 0;
    }

    /**
     * Computes the current age of an entry in milliseconds (RFC 9111 section 4.2.3).
     * <p>
     * 항목의 현재 나이(밀리초)를 계산한다(RFC 9111 4.2.3).
     *
     * @param e   the entry / 항목
     * @param now the time / 시각
     * @return age in milliseconds / 나이(밀리초)
     */
    private static long currentAge(Entry e, long now) {
        long date = HttpDates.parse(e.header("date"));
        if (date < 0) {
            date = e.responseTime;
        }
        long apparentAge = Math.max(0, e.responseTime - date);
        long ageValue = 0;
        String ageHeader = e.header("age");
        if (ageHeader != null) {
            try {
                ageValue = Math.max(0, Long.parseLong(ageHeader.trim())) * 1000L;
            } catch (NumberFormatException ex) {
                ageValue = 0;
            }
        }
        long responseDelay = Math.max(0, e.responseTime - e.requestTime);
        long correctedInitialAge = Math.max(apparentAge, ageValue + responseDelay);
        return correctedInitialAge + Math.max(0, now - e.responseTime);
    }

    /**
     * Merges the headers of a 304 response into a stored entry and restarts its age (RFC 9111 section 4.3.4).
     * <p>
     * 304 응답의 헤더를 저장된 항목에 합치고 나이를 다시 시작한다(RFC 9111 4.3.4). 호출자는 잠금을 잡고 있어야 한다.
     *
     * @param e     the stored entry / 저장된 항목
     * @param r     the 304 response / 304 응답
     * @param start time the conditional request was sent / 조건부 요청을 보낸 시각
     * @param end   time the 304 arrived / 304 가 도착한 시각
     */
    private void merge(Entry e, ResourceResponse r, long start, long end) {
        // 304 가 알려 준 헤더로 같은 이름의 저장된 헤더를 바꾼다(갱신하면 안 되는 헤더는 제외).
        for (String name : r.getHeaderNames()) {
            boolean skip = false;
            // 이 헤더가 갱신하면 안 되는 목록에 있는지 확인한다.
            for (int i = 0; i < NOT_UPDATED.length; i++) {
                if (NOT_UPDATED[i].equals(name)) {
                    skip = true;
                }
            }
            if (!skip) {
                e.headers.put(name, new ArrayList<String>(r.getHeaders(name)));
            }
        }
        e.cc = CacheControl.parse(e.headers.get("cache-control"));
        e.requestTime = start;
        e.responseTime = end;
        lru.get(e);
    }

    /**
     * Builds the response object for a stored entry; the <code>Age</code> header is replaced by the current age.
     * <p>
     * 저장된 항목의 응답 객체를 만든다. <code>Age</code> 헤더는 현재 나이로 바꾼다. 호출자는 잠금을 잡고 있어야 한다.
     *
     * @param e           the entry / 항목
     * @param requestUrl  the URL of the request / 요청의 URL
     * @param now         the time / 시각
     * @param revalidated true if the server confirmed the copy / 서버가 사본을 확인했으면 true
     * @return the response / 응답
     */
    private ResourceResponse toResponse(Entry e, String requestUrl, long now, boolean revalidated) {
        ResourceResponse r = new ResourceResponse(requestUrl);
        r.setFinalUrl(e.finalUrl);
        r.setStatus(e.status, e.reason);
        // 저장된 헤더를 모두 옮기되 Age 는 현재 값으로 다시 쓴다.
        for (Map.Entry<String, List<String>> h : e.headers.entrySet()) {
            if ("age".equals(h.getKey())) {
                continue;
            }
            // 헤더 값을 하나씩 응답에 옮긴다.
            for (String v : h.getValue()) {
                r.addHeader(h.getKey(), v);
            }
        }
        r.addHeader("Age", Long.toString(currentAge(e, now) / 1000L));
        r.setBody(e.body);
        r.setContentType(e.mimeType, e.charset);
        r.setDeclaredLength(e.body.length);
        r.markFromCache(revalidated);
        lru.get(e);
        return r;
    }

    /**
     * Computes the cache key of a URL: the URL without its fragment.
     * <p>
     * URL 의 캐시 키를 계산한다: 조각을 뺀 URL.
     *
     * @param url the URL / URL
     * @return the key / 키
     */
    private static String keyOf(String url) {
        return UrlUtil.stripFragment(url);
    }
}
