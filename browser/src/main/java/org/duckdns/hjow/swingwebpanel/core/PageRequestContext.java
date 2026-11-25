package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;

/**
 * Immutable request settings for subresources of one navigation. It deliberately retains no NavigationRequest, progress
 * sink, session or previous snapshot, so repeated refreshes cannot retain a chain of old pages.
 * <p>한 이동의 하위 리소스용 불변 요청 설정. 이동 요청·진행 콜백·세션·이전 스냅샷을 보관하지 않아 새로고침으로 과거 페이지가 연결되어 남지 않는다.
 */
final class PageRequestContext {
    /** User agent captured at navigation start. / 이동 시작 때의 User-Agent. */
    final String userAgent;
    /** Private copy of request headers. / 요청 헤더의 독립 복사본. */
    private final RequestHeaders headers;
    /** Bypasses caches for every resource in this navigation. / 이 이동의 모든 리소스에서 캐시를 우회한다. */
    final boolean force;
    /** The policies of the document issuing subresource requests. / 하위 리소스를 요청하는 문서의 정책. */
    final CrossOriginPolicy crossOriginPolicy;

    /**
     * Captures settings, using defaults for documents attached by standalone tests or custom loaders.
     * <p>설정을 복사하며 독립 시험이나 사용자 로더로 붙인 문서는 기본값을 쓴다.
     * @param request navigation request, or null / 이동 요청 또는 null
     */
    PageRequestContext(NavigationRequest request) {
        this(request, CrossOriginPolicy.NONE);
    }

    /**
     * Captures request settings and the committed response policies.
     * <p>
     * 요청 설정과 확정 응답의 정책을 복사한다.
     *
     * @param request navigation request or null / 이동 요청 또는 null
     * @param policy the parsed response policy / 응답에서 읽은 정책
     */
    PageRequestContext(NavigationRequest request, CrossOriginPolicy policy) {
        userAgent = request == null ? BrowserSession.DEFAULT_USER_AGENT : request.getUserAgent();
        headers = request == null ? new RequestHeaders() : request.getHeaders().copy();
        force = request != null && request.isForce();
        crossOriginPolicy = policy;
    }

    /** Returns a copy so request construction cannot mutate this context. / 요청 구성이 문맥을 바꾸지 않도록 복사본을 반환한다.
     * @return header copy / 헤더 복사본 */
    RequestHeaders headers() {
        return headers.copy();
    }
}
