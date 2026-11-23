package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Observer of the changes of a document. The style, layout and drawing code uses it to know what must be recalculated, and the JavaScript
 * layer builds <code>MutationObserver</code> on top of it. It is called synchronously on the document thread after the change was made, with
 * a flag that tells whether the changed node is part of the document tree (changes inside detached subtrees need no layout work). An
 * implementation must be fast and must not change the DOM.
 * <p>
 * 문서의 변경을 관찰하는 객체. 스타일, 레이아웃, 그리기 코드가 무엇을 다시 계산해야 하는지 알기 위해 쓰고, JavaScript 계층은 이것을 바탕으로
 * <code>MutationObserver</code> 를 만든다. 변경이 이루어진 직후 문서 스레드에서 동기적으로 호출되며, 변경된 노드가 문서 트리에 속하는지 알려 주는
 * 플래그를 함께 준다(분리된 하위 트리 안의 변경은 레이아웃 작업이 필요 없다). 구현은 빨라야 하며 DOM 을 바꾸면 안 된다.
 */
public interface DomMutationListener {

    /**
     * Called after a change.
     * <p>
     * 변경 뒤에 호출된다.
     *
     * @param record    description of the change / 변경 설명
     * @param connected true if the changed node is connected to the document tree / 변경된 노드가 문서 트리에 연결되어 있으면 true
     */
    void mutated(MutationRecord record, boolean connected);
}
