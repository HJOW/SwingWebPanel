package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.StickyConstraint;

/**
 * The start command of the group of a <code>position:sticky</code> box. The group paints its commands shifted by the amount that the
 * {@link StickyConstraint} gives for the viewport of the paint pass, so the box stays at the edge of the viewport while the page scrolls
 * and stops at the edge of its containing block. The bounds of the command are those of the unshifted content; {@link PagePainter} shifts
 * the visible area back by the amount before it compares them.
 * <p>
 * <code>position:sticky</code> 상자 그룹의 시작 명령. 그룹은 그리기 단계의 뷰포트에 대해 {@link StickyConstraint} 가 정하는 만큼 옮겨 명령을 그리므로, 상자는 페이지가
 * 스크롤되는 동안 뷰포트의 가장자리에 머물다가 포함 블록의 가장자리에서 멈춘다. 명령의 영역은 옮기기 전 내용의 영역이며 {@link PagePainter} 가 비교하기 전에 보이는
 * 영역을 그만큼 되돌린다.
 */
final class StickyGroup extends DisplayCommand {

    /** The constraint. 제약. */
    private final StickyConstraint constraint;

    /**
     * Creates the start command.
     * <p>
     * 시작 명령을 만든다.
     *
     * @param x          left of the unshifted content / 옮기기 전 내용의 왼쪽
     * @param y          top / 위쪽
     * @param w          width / 너비
     * @param h          height / 높이
     * @param constraint the constraint / 제약
     * @param node       the DOM node or null / DOM 노드 또는 null
     */
    StickyGroup(double x, double y, double w, double h, StickyConstraint constraint, Node node) {
        super(Type.STICKY_START, x, y, w, h, node);
        this.constraint = constraint;
    }

    /**
     * Returns the constraint.
     * <p>
     * 제약을 반환한다.
     *
     * @return the constraint / 제약
     */
    StickyConstraint getConstraint() {
        return constraint;
    }

    /**
     * Does nothing: the group is handled structurally by {@link PagePainter}.
     * <p>
     * 아무것도 하지 않는다: 그룹은 {@link PagePainter} 가 구조적으로 처리한다.
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        // 그룹 명령은 PagePainter 가 구조적으로 처리하므로 여기서 할 일이 없다.
    }
}
