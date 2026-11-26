package org.duckdns.hjow.swingwebpanel.core;

/**
 * Receives the request to close the window of a component after a script called <code>window.close()</code> and the component accepted
 * the call (API contract 5.23). Register it with {@link BrowserSession#setWindowCloseHandler(WindowCloseHandler)}; it is called once per
 * accepted request, on the event dispatch thread, after the unloading events of the page ran.
 * <p>
 * 스크립트가 <code>window.close()</code> 를 부르고 컴포넌트가 그 호출을 받아들인 뒤, 컴포넌트의 창을 닫으라는 요청을 받는다(API 계약 5.23).
 * {@link BrowserSession#setWindowCloseHandler(WindowCloseHandler)} 로 등록한다. 받아들인 요청마다 한 번, 페이지의 언로드 이벤트가 실행된 뒤
 * 이벤트 디스패치 스레드에서 호출된다.
 */
public interface WindowCloseHandler {

    /**
     * Closes the window. A window that the host created is closed by the host policy (by default the program ends); a window that web
     * content created is closed alone and must never end the program.
     * <p>
     * 창을 닫는다. 호스트가 만든 창은 호스트의 정책대로 닫고(기본은 프로그램 종료), 웹 콘텐츠가 만든 창은 그 창만 닫으며 프로그램을 끝내면 안 된다.
     *
     * @param createdByWebContent true if web content created the window (a popup or a window that a link opened) / 웹 콘텐츠가 창을
     *                            만들었으면 true(팝업이나 링크가 연 창)
     */
    void closeWindow(boolean createdByWebContent);
}
