package org.duckdns.hjow.swingwebpanel.core;

import java.awt.datatransfer.Transferable;

/**
 * Where the pasted data of the page comes from (plan item P20-06). When the user pastes (Ctrl+V or Shift+Insert) into an editable place of
 * the page, the component reads the clipboard through this interface; the default reads the system clipboard, a host or a test replaces
 * it. It is called on the event dispatch thread. An exception means that the clipboard could not be used (no screen, access denied): the
 * component shows a status message and pastes nothing.
 * <p>
 * 페이지에 붙여넣는 데이터의 출처(계획 P20-06). 사용자가 페이지의 편집 가능한 곳에 붙여넣을 때(Ctrl+V 또는 Shift+Insert) 컴포넌트는 이 인터페이스로 클립보드를
 * 읽는다. 기본은 시스템 클립보드를 읽으며 호스트나 시험이 바꾼다. 이벤트 디스패치 스레드에서 호출된다. 예외는 클립보드를 쓸 수 없다는 뜻이다(화면 없음, 접근
 * 거부): 컴포넌트는 상태 메시지를 보이고 아무것도 붙여넣지 않는다.
 */
public interface ClipboardSource {

    /**
     * Reads the current contents of the clipboard.
     * <p>
     * 클립보드의 현재 내용을 읽는다.
     *
     * @return the contents, or null if the clipboard is empty / 내용. 클립보드가 비었으면 null
     * @throws Exception if the clipboard cannot be used / 클립보드를 쓸 수 없으면
     */
    Transferable read() throws Exception;
}
