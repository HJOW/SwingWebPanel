package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.AffineTransform;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The start command of a transform group (plan item P10-06, CSS Transforms Level 1): everything up to the matching end command is
 * painted with the 2D transform, which maps the local (laid-out) document coordinates of the content to the coordinates of the
 * surrounding group. The bounds of the command are the bounds of the transformed content in the coordinates of the surrounding group,
 * so the painter can skip the whole group when it is outside the visible area; the painter maps the visible area back with the inverse
 * transform for the commands inside. A transform that cannot be inverted (a scale of 0) makes the content invisible: the painter skips
 * the group.
 * <p>
 * 변환 그룹의 시작 명령(계획 P10-06, CSS Transforms Level 1): 짝이 되는 끝 명령까지의 모든 것을 2D 변환을 적용해 그리며, 변환은 내용의 지역(배치된) 문서 좌표를 둘러싼
 * 그룹의 좌표로 옮긴다. 명령의 영역은 둘러싼 그룹의 좌표로 나타낸 변환된 내용의 영역이므로 그리는 쪽은 보이는 영역 밖이면 그룹 전체를 건너뛸 수 있고, 안쪽 명령을 위해
 * 보이는 영역을 역변환으로 되돌려 준다. 역변환할 수 없는 변환(배율 0)은 내용을 보이지 않게 하므로 그리는 쪽이 그룹을 건너뛴다.
 */
final class TransformGroup extends DisplayCommand {

    /** The transform of the group (a private copy). 그룹의 변환(비공개 사본). */
    private final AffineTransform transform;

    /**
     * Creates the command.
     * <p>
     * 명령을 만든다.
     *
     * @param x         left of the transformed bounds / 변환된 영역의 왼쪽
     * @param y         top / 위쪽
     * @param w         width / 너비
     * @param h         height / 높이
     * @param transform the transform of the content / 내용의 변환
     * @param node      the DOM node or null / DOM 노드 또는 null
     */
    TransformGroup(double x, double y, double w, double h, AffineTransform transform, Node node) {
        super(Type.TRANSFORM_START, x, y, w, h, node);
        this.transform = new AffineTransform(transform);
    }

    /**
     * Returns a copy of the transform of the group.
     * <p>
     * 그룹 변환의 사본을 반환한다.
     *
     * @return the transform / 변환
     */
    AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    /**
     * Paints nothing (the painter handles groups).
     * <p>
     * 아무것도 그리지 않는다(그룹은 그리는 쪽이 처리한다).
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        // 그룹 명령은 PagePainter 가 구조적으로 처리하므로 여기서 할 일이 없다.
    }
}
