package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * The geometry of the <code>resize</code> property (CSS Basic User Interface Module Level 4, https://www.w3.org/TR/css-ui-4/#resize, section 6.1
 * "resize: Resizability of elements"): which boxes the user may resize, in which directions, and where their resize handle is. A box is
 * resizable when <code>resize</code> is not <code>none</code>, when it is a block container (a block or an inline block) and when its
 * <code>overflow</code> is neither <code>visible</code> nor <code>clip</code> in at least one axis (the property "applies to elements with
 * overflow other than visible"). The handle is a square of {@link #HANDLE_SIZE} pixels at the bottom right corner of the padding box (the corner
 * where a scrollbar corner is); a box whose padding box is smaller than the handle has none. <code>block</code> and <code>inline</code> are
 * resolved against the writing mode of the box. The input handling that changes the size is in <code>org.duckdns.hjow.swingwebpanel.core.PageResize</code> and
 * the picture of the handle in <code>org.duckdns.hjow.swingwebpanel.render</code>.
 * <p>
 * <code>resize</code> 속성의 기하(CSS Basic User Interface Module Level 4, https://www.w3.org/TR/css-ui-4/#resize, 6.1 절 "resize: Resizability of
 * elements"): 사용자가 크기를 바꿀 수 있는 상자, 방향, 크기 조절 핸들의 위치. <code>resize</code> 가 <code>none</code> 이 아니고, 블록 컨테이너(블록 또는
 * inline-block)이며, 적어도 한 축에서 <code>overflow</code> 가 <code>visible</code> 도 <code>clip</code> 도 아니면 크기를 바꿀 수 있는 상자이다(속성은 "overflow 가
 * visible 이 아닌 요소에 적용된다"). 핸들은 padding box 의 오른쪽 아래 모서리(스크롤바 모서리가 있는 곳)에 놓이는 {@link #HANDLE_SIZE} 픽셀의 정사각형이며,
 * padding box 가 핸들보다 작은 상자는 핸들이 없다. <code>block</code> 과 <code>inline</code> 은 상자의 쓰기 모드에 따라 푼다. 크기를 바꾸는 입력 처리는
 * <code>org.duckdns.hjow.swingwebpanel.core.PageResize</code>, 핸들 그림은 <code>org.duckdns.hjow.swingwebpanel.render</code> 에 있다.
 */
public final class Resizing {

    /** The resize direction: not resizable. 크기 조절 방향: 크기를 바꿀 수 없음. */
    public static final int NONE = 0;
    /** The resize direction: the width can be changed. 크기 조절 방향: 너비를 바꿀 수 있음. */
    public static final int HORIZONTAL = 1;
    /** The resize direction: the height can be changed. 크기 조절 방향: 높이를 바꿀 수 있음. */
    public static final int VERTICAL = 2;
    /** The resize direction: both can be changed (the bits of the two directions). 크기 조절 방향: 둘 다 바꿀 수 있음(두 방향의 비트). */
    public static final int BOTH = 3;

    /** The side of the square resize handle in CSS pixels. 정사각형 크기 조절 핸들의 한 변(CSS 픽셀). */
    public static final double HANDLE_SIZE = 15;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Resizing() {
    }

    /**
     * Returns the directions in which the user may resize a box: the value of <code>resize</code> resolved against the writing mode, or
     * {@link #NONE} if the box is not resizable.
     * <p>
     * 사용자가 상자의 크기를 바꿀 수 있는 방향을 반환한다: 쓰기 모드에 따라 푼 <code>resize</code> 의 값. 크기를 바꿀 수 없으면 {@link #NONE}.
     *
     * @param b the box / 상자
     * @return {@link #NONE}, {@link #HORIZONTAL}, {@link #VERTICAL} or {@link #BOTH} / 방향
     */
    public static int directions(LayoutBox b) {
        ComputedStyle s = b.getStyle();
        LayoutBox.Type t = b.getType();
        // 블록 컨테이너만 크기를 바꿀 수 있고, 스타일과 DOM 요소가 있어야 한다.
        if (s == null || b.getNode() == null || (t != LayoutBox.Type.BLOCK && t != LayoutBox.Type.INLINE_BLOCK) || !b.isClipsContent()) {
            return NONE;
        }
        String r = s.keyword(Prop.RESIZE);
        if (r == null || "none".equals(r)) {
            return NONE;
        }
        // overflow 가 visible 이 아니고 clip 도 아닌 축이 하나라도 있어야 한다.
        if (!scrolls(s.keyword(Prop.OVERFLOW_X)) && !scrolls(s.keyword(Prop.OVERFLOW_Y))) {
            return NONE;
        }
        if ("both".equals(r)) {
            return BOTH;
        }
        if ("horizontal".equals(r)) {
            return HORIZONTAL;
        }
        if ("vertical".equals(r)) {
            return VERTICAL;
        }
        // block 과 inline 은 쓰기 모드에 따라 세로와 가로 중 하나가 된다.
        boolean inline = "inline".equals(r);
        return inline == b.isVertical() ? VERTICAL : HORIZONTAL;
    }

    /**
     * Tells whether an <code>overflow</code> value makes a scroll container.
     * <p>
     * <code>overflow</code> 값이 스크롤 컨테이너를 만드는지 알려 준다.
     *
     * @param v the keyword / 키워드
     * @return true for <code>hidden</code>, <code>scroll</code>, <code>auto</code> / <code>hidden</code>, <code>scroll</code>, <code>auto</code> 이면 true
     */
    private static boolean scrolls(String v) {
        return v != null && !"visible".equals(v) && !"clip".equals(v) && v.length() > 0;
    }

    /**
     * Returns the resize handle of a box.
     * <p>
     * 상자의 크기 조절 핸들을 반환한다.
     *
     * @param b the box / 상자
     * @return {x, y, width, height} in the document coordinates of the box, or null if the box has no handle / 상자의 문서 좌표로 나타낸 {x, y, 너비, 높이}.
     *         핸들이 없으면 null
     */
    public static double[] handle(LayoutBox b) {
        if (directions(b) == NONE) {
            return null;
        }
        double pw = b.getWidth() - b.getBorderLeft() - b.getBorderRight();
        double ph = b.getHeight() - b.getBorderTop() - b.getBorderBottom();
        // 핸들보다 작은 padding box 에는 핸들을 만들지 않는다.
        if (pw < HANDLE_SIZE || ph < HANDLE_SIZE) {
            return null;
        }
        return new double[] { b.getX() + b.getWidth() - b.getBorderRight() - HANDLE_SIZE, b.getY() + b.getHeight() - b.getBorderBottom() - HANDLE_SIZE,
            HANDLE_SIZE, HANDLE_SIZE };
    }

    /**
     * Finds the box whose resize handle is at a point: the box that was hit or its closest ancestor with a handle that contains the point.
     * The point is mapped back through the transform of the box, so that a handle that was moved by <code>transform</code> is found where it is
     * seen.
     * <p>
     * 한 점에 있는 크기 조절 핸들의 상자를 찾는다: 맞은 상자 또는 그 점을 포함한 핸들을 가진 가장 가까운 조상. 점은 상자의 변환을 거꾸로 적용해 옮기므로
     * <code>transform</code> 으로 옮겨진 핸들도 보이는 자리에서 찾는다.
     *
     * @param hit the box that was hit, may be null / 맞은 상자. null 가능
     * @param x   the x of the point in document coordinates / 점의 x(문서 좌표)
     * @param y   the y of the point in document coordinates / 점의 y(문서 좌표)
     * @return the box or null / 상자. 없으면 null
     */
    public static LayoutBox find(LayoutBox hit, double x, double y) {
        // 맞은 상자에서 조상으로 올라가며 점을 포함한 핸들을 가진 상자를 찾는다.
        for (LayoutBox b = hit; b != null; b = b.getParent()) {
            double[] h = handle(b);
            if (h == null) {
                continue;
            }
            Point2D p = new Point2D.Double(x, y);
            AffineTransform t = b.getFullTransform();
            if (t != null) {
                try {
                    p = t.inverseTransform(p, null);
                } catch (NoninvertibleTransformException e) {
                    continue;
                }
            }
            if (p.getX() >= h[0] && p.getX() < h[0] + h[2] && p.getY() >= h[1] && p.getY() < h[1] + h[3]) {
                return b;
            }
        }
        return null;
    }

    /**
     * Returns how much larger the border box is than the value of <code>width</code> (or <code>height</code>) of a box: the padding and the
     * border along the axis for <code>box-sizing: content-box</code> and zero for <code>border-box</code>. The new value of the property
     * that gives the box the border box size that the user dragged it to is that size minus this amount (not below zero).
     * <p>
     * 상자의 border box 가 <code>width</code>(또는 <code>height</code>) 값보다 얼마나 큰지 반환한다: <code>box-sizing: content-box</code> 이면 축을 따른 padding 과 border,
     * <code>border-box</code> 이면 0. 사용자가 끌어 정한 border box 크기를 상자에 주는 속성의 새 값은 그 크기에서 이 값을 뺀 것이다(0 미만이 아님).
     *
     * @param b     the box / 상자
     * @param width true for the width, false for the height / 너비이면 true, 높이이면 false
     * @return the amount in CSS pixels / 크기(CSS 픽셀)
     */
    public static double insets(LayoutBox b, boolean width) {
        ComputedStyle s = b.getStyle();
        String sizing = s == null ? null : s.keyword(Prop.BOX_SIZING);
        if ("border-box".equals(sizing)) {
            return 0;
        }
        return width ? b.getPaddingLeft() + b.getPaddingRight() + b.getBorderLeft() + b.getBorderRight()
                : b.getPaddingTop() + b.getPaddingBottom() + b.getBorderTop() + b.getBorderBottom();
    }
}
