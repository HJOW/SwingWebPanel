package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Receives notifications from a {@link StyleResolver} when cached computed styles became stale: after a DOM change, a change of the dynamic element
 * state, new style sheets or a new media environment. The layout uses it to schedule a relayout and a repaint of the affected part of the tree.
 * The callback runs on the thread that made the change; it must not call back into the resolver for long computations.
 * <p>
 * 캐시된 계산 스타일이 낡았을 때 {@link StyleResolver} 가 보내는 통지를 받는다: DOM 변경, 동적 요소 상태 변경, 새 스타일 시트, 새 미디어 환경 뒤. 레이아웃이 이것으로
 * 트리의 영향받은 부분의 재배치와 다시 그리기를 예약한다. 콜백은 변경을 일으킨 스레드에서 실행되며 리졸버로 되돌아가 긴 계산을 해서는 안 된다.
 */
public interface StyleChangeListener {

    /**
     * Called when the styles of a subtree (or of the whole document) must be recomputed.
     * <p>
     * 서브트리(또는 문서 전체)의 스타일을 다시 계산해야 할 때 호출된다.
     *
     * @param root the root of the stale subtree, or null if the whole document is stale / 낡은 서브트리의 루트. 문서 전체가 낡았으면 null
     */
    void stylesInvalidated(Element root);
}
