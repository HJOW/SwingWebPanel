package org.duckdns.hjow.swingwebpanel.core;

/**
 * Where the copied text of the page goes (plan item P11-08). The component of the page passes what the user copied (Ctrl+C) to it; the
 * default writes it to the system clipboard, a host or a test replaces it. It is called on the event dispatch thread. An exception means
 * that the clipboard could not be used (no screen, access denied): the component shows a status message and goes on.
 * <p>
 * 페이지에서 복사한 텍스트가 가는 곳(계획 P11-08). 페이지의 컴포넌트는 사용자가 복사한(Ctrl+C) 것을 이것에 넘긴다. 기본은 시스템 클립보드에 쓰며
 * 호스트나 시험이 바꾼다. 이벤트 디스패치 스레드에서 호출된다. 예외는 클립보드를 쓸 수 없다는 뜻이다(화면 없음, 접근 거부): 컴포넌트는 상태 메시지를 보이고
 * 계속한다.
 */
public interface ClipboardSink {

    /**
     * Writes the copied data.
     * <p>
     * 복사한 데이터를 쓴다.
     *
     * @param plainText    the text, as the <code>text/plain</code> data / <code>text/plain</code> 데이터인 텍스트
     * @param htmlFragment the markup of the selection for <code>text/html</code>, or null if there is none / <code>text/html</code> 을 위한 선택의 마크업.
     *                     없으면 null
     * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면
     */
    void write(String plainText, String htmlFragment) throws Exception;
}
