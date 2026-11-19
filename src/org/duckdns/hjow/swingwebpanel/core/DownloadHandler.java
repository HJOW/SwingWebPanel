package org.duckdns.hjow.swingwebpanel.core;

/**
 * Receives navigations that turned out to be file downloads (API contract section 7). The Swing download popup of a later stage
 * (plan item P11-09) implements this interface. It is called on a background thread: an implementation that shows UI must switch
 * to the event dispatch thread itself. Until a handler is installed the browser only reports the download in the status text.
 * <p>
 * 파일 다운로드로 판명된 이동을 받는다(API 계약 7절). 이후 단계의 Swing 다운로드 팝업(계획 P11-09)이 이 인터페이스를 구현한다.
 * 백그라운드 스레드에서 호출되므로 UI 를 보여 주는 구현은 이벤트 디스패치 스레드로 스스로 넘어가야 한다. 핸들러가 설치되기 전에는
 * 브라우저가 상태 메시지로만 다운로드를 알린다.
 */
public interface DownloadHandler {

    /**
     * Called when a navigation must be downloaded instead of shown.
     * <p>
     * 이동을 보여 주는 대신 내려받아야 할 때 호출된다.
     *
     * @param request the navigation request (user agent, headers) that discovered the download / 다운로드를 알아낸 이동 요청(User-Agent, 헤더)
     * @param download description of the response to download / 내려받을 응답의 설명
     */
    void downloadRequired(NavigationRequest request, DownloadRequiredException download);
}
