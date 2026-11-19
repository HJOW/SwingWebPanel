package org.duckdns.hjow.swingwebpanel.core;

/**
 * The live objects that belong to one confirmed page (DOM, style sheets, layout, script realm, timers, media).
 * A {@link PageSnapshot} may carry one. The browser session calls {@link #dispose()} exactly when the page is replaced,
 * cleaned or disposed, so every resource of the page is released at that moment.
 * <p>
 * 확정된 페이지 하나에 속한 살아 있는 객체들(DOM, 스타일시트, 레이아웃, 스크립트 영역, 타이머, 미디어).
 * {@link PageSnapshot} 이 하나를 가질 수 있다. 페이지가 교체되거나 clean/dispose 될 때 브라우저 세션이 정확히 한 번
 * {@link #dispose()} 를 호출하며, 그 시점에 페이지의 모든 자원이 해제되어야 한다.
 */
public interface PageDocument {

    /**
     * Releases every resource of the page: cancels timers and script work, stops media, closes frames and requests.
     * It must be safe to call more than once and from any thread.
     * <p>
     * 페이지의 모든 자원을 해제한다: 타이머와 스크립트 작업을 취소하고 미디어를 멈추며 프레임과 요청을 닫는다.
     * 여러 번, 어느 스레드에서 호출해도 안전해야 한다.
     */
    void dispose();
}
