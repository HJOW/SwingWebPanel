package org.duckdns.hjow.swingwebpanel;

import java.util.EventListener;

/**
 * Receives the printout that {@link SwingWebPanel#createPrintout(java.awt.print.PageFormat, boolean, PrintoutListener)} makes (extension,
 * API contract 5.30). The layout for printing is made on the document thread of the component, so the result arrives later.
 * <p>
 * {@link SwingWebPanel#createPrintout(java.awt.print.PageFormat, boolean, PrintoutListener)} 가 만드는 인쇄물을 받는다(확장, API 계약 5.30).
 * 인쇄용 배치는 컴포넌트의 문서 스레드에서 만들어지므로 결과는 나중에 도착한다.
 */
public interface PrintoutListener extends EventListener {

    /**
     * Called once, on the event dispatch thread, when the printout is ready.
     * <p>
     * 인쇄물이 준비되면 이벤트 디스패치 스레드에서 한 번 호출된다.
     *
     * @param printout the printout, or null if none could be made (the page was replaced or the component was disposed while the layout
     *                 was made, or the layout failed) / 인쇄물. 만들 수 없었으면 null(배치하는 동안 페이지가 교체되었거나 컴포넌트가
     *                 폐기되었거나 배치에 실패했다)
     */
    void printoutReady(PagePrintout printout);
}
