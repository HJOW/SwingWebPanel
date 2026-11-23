package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Receives the notification that the selection of a document changed: another range was chosen, a boundary point of the range moved (by a
 * script, by the user, or because the tree changed under a live range), or the selection was emptied. The component of the page uses it to
 * redraw the highlight of the selection and to fire the <code>selectionchange</code> event; a document has at most one listener, which is
 * installed with {@link Document#setSelectionListener(SelectionListener)}. The call happens on the thread that changed the selection (the
 * document thread), possibly several times for one operation, so a listener should only note the change and do its work later.
 * <p>
 * 문서의 선택이 바뀌었다는 통지를 받는다: 다른 범위가 선택되었거나, 범위의 경계점이 움직였거나(스크립트, 사용자, 또는 살아 있는 범위 아래의 트리 변경),
 * 선택이 비워졌다. 페이지의 컴포넌트가 이것으로 선택 강조를 다시 그리고 <code>selectionchange</code> 이벤트를 보낸다. 문서에는 리스너가 최대 하나이며
 * {@link Document#setSelectionListener(SelectionListener)} 로 설치한다. 선택을 바꾼 스레드(문서 스레드)에서 호출되고 한 연산에 여러 번 호출될 수 있으므로,
 * 리스너는 변경을 기록만 하고 일은 나중에 해야 한다.
 */
public interface SelectionListener {

    /**
     * Called after the selection changed.
     * <p>
     * 선택이 바뀐 뒤에 호출된다.
     *
     * @param selection the selection that changed / 바뀐 선택
     */
    void selectionChanged(Selection selection);
}
