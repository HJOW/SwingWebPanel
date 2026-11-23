package org.duckdns.hjow.swingwebpanel.core;

/**
 * Strategy that turns a {@link NavigationRequest} into a confirmed {@link PageSnapshot}. It runs on the component's
 * document thread, never on the EDT. An implementation must poll {@link NavigationRequest#getCancelToken()} during long
 * work, close every stream on every path and report failures as an error page snapshot rather than by throwing, except for
 * cancellation which is signalled by {@link org.duckdns.hjow.swingwebpanel.net.CancelledException}. Later stages of the project replace the
 * default loader with the real network, content and rendering pipeline.
 * <p>
 * {@link NavigationRequest} 를 확정된 {@link PageSnapshot} 으로 바꾸는 전략. 컴포넌트의 문서 스레드에서 실행되며 EDT 에서는 실행되지 않는다.
 * 구현은 긴 작업 중 {@link NavigationRequest#getCancelToken()} 을 확인하고, 모든 경로에서 스트림을 닫으며, 실패는 던지지 않고 오류 페이지
 * 스냅샷으로 보고해야 한다. 취소만은 {@link org.duckdns.hjow.swingwebpanel.net.CancelledException} 으로 알린다. 프로젝트 후반 단계에서 기본 로더를
 * 실제 네트워크, 콘텐츠, 렌더링 파이프라인으로 교체한다.
 */
public interface PageLoader {

    /**
     * Loads one page.
     * <p>
     * 페이지 하나를 읽는다.
     *
     * @param request what to load and how / 읽을 대상과 방법
     * @return the confirmed page / 확정된 페이지
     * @throws Exception on cancellation or an unexpected failure (the session turns unexpected failures into an error page) / 취소 또는
     *                   예상치 못한 실패(세션이 예상치 못한 실패를 오류 페이지로 바꾼다)
     */
    PageSnapshot load(NavigationRequest request) throws Exception;
}
