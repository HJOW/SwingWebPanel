package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.anim.Transforms;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The offset transform of CSS Motion Path Module Level 1 (<a href="https://www.w3.org/TR/motion-1/">https://www.w3.org/TR/motion-1/</a>, W3C Working Draft 5 November 2024):
 * <code>offset-path</code> (<code>path()</code>, <code>circle()</code>, <code>ellipse()</code>, <code>inset()</code>, <code>rect()</code>, <code>xywh()</code>,
 * <code>polygon()</code>, <code>ray()</code> and a reference box alone), <code>offset-distance</code>, <code>offset-position</code>, <code>offset-anchor</code> and
 * <code>offset-rotate</code>. A box with an offset path is placed so that its anchor point is on the point of the path that <code>offset-distance</code> gives, and it is
 * rotated about that point (2.7 of the specification); the transform takes part in the transform of the box in the order of CSS Transforms Level 2
 * (<code>translate</code>, <code>rotate</code>, <code>scale</code>, the offset transform, <code>transform</code>), so that the box is a stacking context and the
 * containing block of its fixed and absolutely positioned descendants like any transformed box, and hit testing follows the moved box.
 * <p>
 * The path is the equivalent path of the shape (section 3): a circle or ellipse starts at its rightmost point and runs clockwise, a rectangle starts at the left end of the
 * top edge and runs clockwise, a polygon and a <code>path()</code> follow their own points. The reference box is the border box of the box that establishes the containing
 * block. The used distance is the distance modulo the length of a closed path, clamped for an open path, and not limited for a <code>ray()</code> without
 * <code>contain</code> (2.2.1).
 * <p>
 * <code>offset-path</code> 가 있는 상자는 앵커 점이 <code>offset-distance</code> 가 주는 경로 위의 점에 오도록 놓이고 그 점을 중심으로 회전한다(규격 2.7). 변환은 CSS Transforms Level 2 의
 * 순서(<code>translate</code>, <code>rotate</code>, <code>scale</code>, 오프셋 변환, <code>transform</code>)로 상자의 변환에 참여하므로 상자는 다른 변환된 상자처럼 쌓임 맥락이고 fixed 와
 * 절대 배치 자손의 포함 블록이며, 히트 테스트는 옮겨진 상자를 따른다. 경로는 도형의 동등한 경로(3 절)이다: 원과 타원은 가장 오른쪽 점에서 시계 방향으로, 직사각형은 윗변의 왼쪽 끝에서
 * 시계 방향으로 시작하고, 다각형과 <code>path()</code> 는 자신의 점을 따른다. 기준 상자는 포함 블록을 만드는 상자의 border box 이다. 실제 거리는 닫힌 경로에서는 길이로 나눈 나머지, 열린
 * 경로에서는 한정한 값이며 <code>contain</code> 없는 <code>ray()</code> 는 제한이 없다(2.2.1).
 */
public final class MotionPath {

    /** The flattening of curves in pixels. 곡선을 펴는 오차(픽셀). */
    private static final double FLATNESS = 0.1;

    /** The reference boxes that an <code>offset-path</code> may name instead of a shape. <code>offset-path</code> 가 도형 대신 쓸 수 있는 기준 상자. */
    private static final String[] BOXES = {"content-box", "padding-box", "border-box", "margin-box", "fill-box", "stroke-box", "view-box"};

    /** The segments of the path: {x0, y0, x1, y1, length before the segment}. 경로의 구간: {x0, y0, x1, y1, 구간 앞까지의 길이}. */
    private final List<double[]> segments = new ArrayList<double[]>();

    /** The total length of the path. 경로의 전체 길이. */
    private double total;

    /** True for a closed loop. 닫힌 고리이면 true. */
    private boolean closed;
    /** The first point of the path (the whole path when it has no length), or null. 경로의 첫 점(길이가 없는 경로이면 경로 전체). 없으면 null. */
    private double[] point;

    /** True for a ray without <code>contain</code>: the distance is not limited. <code>contain</code> 없는 ray 이면 true: 거리에 제한이 없다. */
    private boolean unbounded;

    /**
     * Creates an empty path.
     * <p>
     * 빈 경로를 만든다.
     */
    private MotionPath() {
    }

    /**
     * Tells whether a style has an offset path that this engine can follow: a shape function, a <code>ray()</code> or a reference box. A <code>url()</code> to an SVG
     * shape, which this engine cannot read, and <code>none</code> have no offset transform.
     * <p>
     * 스타일에 이 엔진이 따라갈 수 있는 오프셋 경로(도형 함수, <code>ray()</code>, 기준 상자)가 있는지 알려 준다. 이 엔진이 읽을 수 없는 SVG 도형을 가리키는 <code>url()</code> 과
     * <code>none</code> 은 오프셋 변환이 없다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box has an offset transform / 상자에 오프셋 변환이 있으면 true
     */
    public static boolean has(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        CssValue v = s.get(Prop.OFFSET_PATH);
        if (v == null || v.isKeyword("none")) {
            return false;
        }
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        // 값의 항목 중 쓸 수 있는 도형 함수나 기준 상자가 하나라도 있는지 본다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it instanceof CssFunctionValue) {
                String n = ((CssFunctionValue) it).getName();
                if (n.equals("path") || n.equals("circle") || n.equals("ellipse") || n.equals("inset") || n.equals("rect") || n.equals("xywh")
                        || n.equals("polygon") || n.equals("ray")) {
                    return true;
                }
            } else if (it instanceof CssKeyword && isBox(((CssKeyword) it).getName())) {
                return true;
            } else if (it instanceof org.duckdns.hjow.swingwebpanel.css.values.CssUrl) {
                // url() 은 SVG 도형 요소의 경로이며, 도형이 아니면 path("m 0 0") 처럼 동작한다(2.1 절). 어느 쪽이든 오프셋 변환이 있다.
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a name is a reference box keyword.
     * <p>
     * 이름이 기준 상자 키워드인지 알려 준다.
     *
     * @param name the keyword / 키워드
     * @return true for a box / 상자이면 true
     */
    private static boolean isBox(String name) {
        // 기준 상자 이름 목록과 하나씩 비교한다.
        for (int i = 0; i < BOXES.length; i++) {
            if (BOXES[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the transform of a box with an offset path in document coordinates: the individual transform properties, the offset transform and the
     * <code>transform</code> property in the order of CSS Transforms Level 2, each about the <code>transform-origin</code> of the box. The geometry of the box
     * and of its containing block must be final.
     * <p>
     * 오프셋 경로가 있는 상자의 문서 좌표 변환을 반환한다: 개별 변환 속성, 오프셋 변환, <code>transform</code> 속성을 CSS Transforms Level 2 의 순서로, 각각 상자의
     * <code>transform-origin</code> 둘레에서. 상자와 그 포함 블록의 기하가 확정되어 있어야 한다.
     *
     * @param b the box / 상자
     * @return the transform, or null if the box has no usable offset path / 변환. 쓸 수 있는 오프셋 경로가 없으면 null
     */
    public static AffineTransform boxTransform(LayoutBox b) {
        ComputedStyle s = b.getStyle();
        AffineTransform off = offsetTransform(b, s);
        if (off == null) {
            return null;
        }
        double w = b.getWidth();
        double h = b.getHeight();
        double[] o = Transforms.origin(s.get(Prop.TRANSFORM_ORIGIN), w, h);
        double ox = b.getX() + o[0];
        double oy = b.getY() + o[1];
        AffineTransform ind = Transforms.individual(s, w, h);
        AffineTransform m = Transforms.matrix(s.get(Prop.TRANSFORM), w, h);
        AffineTransform total = new AffineTransform();
        // translate, rotate, scale 개별 속성은 변환 원점 둘레에서 가장 바깥에 적용한다.
        if (ind != null) {
            total.translate(ox, oy);
            total.concatenate(ind);
            total.translate(-ox, -oy);
        }
        total.concatenate(off);
        // transform 속성은 오프셋 변환 안쪽(먼저 적용)에서 변환 원점 둘레에 적용한다.
        if (m != null) {
            total.translate(ox, oy);
            total.concatenate(m);
            total.translate(-ox, -oy);
        }
        return total;
    }

    /**
     * Returns the offset transform of a box alone (2.7 of the specification) in document coordinates: it moves the anchor point of the box to the point of the path and
     * rotates the box about that point.
     * <p>
     * 상자의 오프셋 변환만(규격 2.7) 문서 좌표로 반환한다: 상자의 앵커 점을 경로 위의 점으로 옮기고 그 점을 중심으로 상자를 회전한다.
     *
     * @param b the box / 상자
     * @param s its style / 스타일
     * @return the transform or null / 변환. 없으면 null
     */
    static AffineTransform offsetTransform(LayoutBox b, ComputedStyle s) {
        double[] ref = referenceBox(b, s);
        MotionPath path = pathOf(b, s, ref);
        if (path == null) {
            return null;
        }
        double distance = s.resolve(Prop.OFFSET_DISTANCE, path.total);
        if (Double.isNaN(distance)) {
            distance = 0;
        }
        double[] at = path.pointAt(distance);
        CssValue anchorValue = s.get(Prop.OFFSET_ANCHOR);
        CssValue anchor = anchorValue == null || anchorValue.isKeyword("auto") ? s.get(Prop.TRANSFORM_ORIGIN) : anchorValue;
        double[] a = Transforms.origin(anchor, b.getWidth(), b.getHeight());
        double ax = b.getX() + a[0];
        double ay = b.getY() + a[1];
        double angle = rotation(s.get(Prop.OFFSET_ROTATE), at[2]);
        AffineTransform t = new AffineTransform();
        t.translate(at[0], at[1]);
        t.rotate(Math.toRadians(angle));
        t.translate(-ax, -ay);
        return t;
    }

    /**
     * Returns the rotation of <code>offset-rotate</code> in degrees: <code>auto</code> turns the box along the direction of the path, <code>reverse</code> adds 180
     * degrees to that, an angle adds itself (and alone it is the whole rotation).
     * <p>
     * <code>offset-rotate</code> 의 회전을 도 단위로 반환한다: <code>auto</code> 는 경로 방향으로 상자를 돌리고, <code>reverse</code> 는 거기에 180도를 더하며, 각도는 그만큼을 더한다(각도만
     * 있으면 그것이 전체 회전이다).
     *
     * @param v         the computed value, null for the initial <code>auto</code> / 계산 값. 없으면 초기값 <code>auto</code>
     * @param direction the direction of the path at the point in degrees / 그 점에서 경로의 방향(도)
     * @return the rotation in degrees / 회전(도)
     */
    static double rotation(CssValue v, double direction) {
        if (v == null) {
            return direction;
        }
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        boolean auto = false;
        double extra = 0;
        boolean any = false;
        // 키워드와 각도를 하나씩 읽는다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it.isKeyword("auto")) {
                auto = true;
                any = true;
            } else if (it.isKeyword("reverse")) {
                auto = true;
                extra += 180;
                any = true;
            } else if (it instanceof CssDimension && ((CssDimension) it).getKind() == CssUnits.ANGLE) {
                extra += ((CssDimension) it).canonical();
                any = true;
            } else if (it instanceof CssNumber) {
                any = true;
            }
        }
        return (auto || !any ? direction : 0) + extra;
    }

    /**
     * Returns the reference box of the <code>&lt;coord-box&gt;</code> of a style (Motion Path 1 section 2.1, CSS Box Model 3): the content, padding, border or
     * margin box of the box that establishes the containing block ({x, y, width, height}; <code>fill-box</code> is the content box, <code>stroke-box</code> and
     * <code>view-box</code> are the border box in CSS contexts); the border box when the value names no box.
     * <p>
     * 스타일의 <code>&lt;coord-box&gt;</code> 기준 상자를 반환한다(Motion Path 1 2.1 절, CSS Box Model 3): 포함 블록을 만드는 상자의 content, padding, border, margin box
     * ({x, y, 너비, 높이}. CSS 맥락에서 <code>fill-box</code> 는 content box, <code>stroke-box</code> 와 <code>view-box</code> 는 border box). 값이 상자를 지정하지 않으면 border box.
     *
     * @param b the box / 상자
     * @param s the style or null / 스타일 또는 null
     * @return the reference box / 기준 상자
     */
    private static double[] referenceBox(LayoutBox b, ComputedStyle s) {
        return boxRect(containingBox(b), coordBox(s));
    }

    /**
     * Returns the box that establishes the containing block of a box; for the initial containing block the root box.
     * <p>
     * 상자의 포함 블록을 만드는 상자를 반환한다. 초기 포함 블록이면 루트 상자.
     *
     * @param b the box / 상자
     * @return the box / 상자
     */
    private static LayoutBox containingBox(LayoutBox b) {
        LayoutBox cb = b.getContainingBlock();
        // 초기 포함 블록이면 위로 올라가 루트 상자를 쓴다.
        if (cb == null) {
            cb = b;
            // 루트 상자까지 부모를 따라 올라간다.
            while (cb.parent != null) {
                cb = cb.parent;
            }
        }
        return cb;
    }

    /**
     * Returns the <code>&lt;coord-box&gt;</code> keyword of the offset path of a style, mapped to a CSS box (<code>border-box</code> if there is none).
     * <p>
     * 스타일의 오프셋 경로의 <code>&lt;coord-box&gt;</code> 키워드를 CSS 상자로 바꿔 반환한다(없으면 <code>border-box</code>).
     *
     * @param s the style or null / 스타일 또는 null
     * @return content-box, padding-box, border-box or margin-box / 상자 키워드
     */
    private static String coordBox(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.OFFSET_PATH);
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        String box = "border-box";
        // 값의 키워드 중 상자 키워드를 찾는다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof CssKeyword && isBox(((CssKeyword) items.get(i)).getName())) {
                box = ((CssKeyword) items.get(i)).getName();
            }
        }
        if ("fill-box".equals(box)) {
            return "content-box";
        }
        return "stroke-box".equals(box) || "view-box".equals(box) ? "border-box" : box;
    }

    /**
     * Returns a box edge of a box as {x, y, width, height}.
     * <p>
     * 상자의 한 가장자리를 {x, y, 너비, 높이} 로 반환한다.
     *
     * @param cb  the box / 상자
     * @param box content-box, padding-box, border-box or margin-box / 상자 키워드
     * @return the rectangle / 사각형
     */
    private static double[] boxRect(LayoutBox cb, String box) {
        double x = cb.getX();
        double y = cb.getY();
        double w = cb.getWidth();
        double h = cb.getHeight();
        if ("padding-box".equals(box) || "content-box".equals(box)) {
            x += cb.getBorderLeft();
            y += cb.getBorderTop();
            w -= cb.getBorderLeft() + cb.getBorderRight();
            h -= cb.getBorderTop() + cb.getBorderBottom();
            if ("content-box".equals(box)) {
                x += cb.getPaddingLeft();
                y += cb.getPaddingTop();
                w -= cb.getPaddingLeft() + cb.getPaddingRight();
                h -= cb.getPaddingTop() + cb.getPaddingBottom();
            }
        } else if ("margin-box".equals(box)) {
            x -= cb.getMarginLeft();
            y -= cb.getMarginTop();
            w += cb.getMarginLeft() + cb.getMarginRight();
            h += cb.getMarginTop() + cb.getMarginBottom();
        }
        return new double[] {x, y, Math.max(0, w), Math.max(0, h)};
    }

    /**
     * Returns the path of a <code>&lt;coord-box&gt;</code> alone: <code>inset(0 round X)</code> where X is the <code>border-radius</code> of the box that
     * establishes the containing block (Motion Path 1 section 2.1), the radii shrunk by the borders and paddings for the inner boxes and grown by the margins for
     * the margin box (CSS Backgrounds 3 section 4.2); the path starts at the left end of the top edge after the corner and runs clockwise (section 3).
     * <p>
     * <code>&lt;coord-box&gt;</code> 만 있을 때의 경로를 반환한다: 포함 블록을 만드는 상자의 <code>border-radius</code> 가 X 인 <code>inset(0 round X)</code>(Motion Path 1
     * 2.1 절). 안쪽 상자는 반지름을 border 와 padding 만큼 줄이고 margin box 는 margin 만큼 늘린다(CSS Backgrounds 3 4.2 절). 경로는 윗변에서 모서리 바로 뒤 왼쪽 끝에서 시작해
     * 시계 방향으로 돈다(3 절).
     *
     * @param cb  the box / 상자
     * @param ref the reference box / 기준 상자
     * @param box the box keyword / 상자 키워드
     * @return the shape / 모양
     */
    private static Shape boxPath(LayoutBox cb, double[] ref, String box) {
        ComputedStyle cs = cb.getStyle();
        double bw = cb.getWidth();
        double bh = cb.getHeight();
        int[] ids = {Prop.BORDER_TOP_LEFT_RADIUS, Prop.BORDER_TOP_RIGHT_RADIUS, Prop.BORDER_BOTTOM_RIGHT_RADIUS, Prop.BORDER_BOTTOM_LEFT_RADIUS};
        double[] r = new double[8];
        // 모서리마다 가로와 세로 반지름을 읽는다(백분율은 border box 기준).
        for (int i = 0; i < 4; i++) {
            double[] hv = cornerRadius(cs == null ? null : cs.get(ids[i]), bw, bh);
            r[2 * i] = hv[0];
            r[2 * i + 1] = hv[1];
        }
        double[] left = {0, 0};
        double[] top = {0, 0};
        double dl = 0;
        double dr = 0;
        double dt = 0;
        double db = 0;
        if ("padding-box".equals(box) || "content-box".equals(box)) {
            dl = -cb.getBorderLeft();
            dr = -cb.getBorderRight();
            dt = -cb.getBorderTop();
            db = -cb.getBorderBottom();
            if ("content-box".equals(box)) {
                dl -= cb.getPaddingLeft();
                dr -= cb.getPaddingRight();
                dt -= cb.getPaddingTop();
                db -= cb.getPaddingBottom();
            }
        } else if ("margin-box".equals(box)) {
            dl = cb.getMarginLeft();
            dr = cb.getMarginRight();
            dt = cb.getMarginTop();
            db = cb.getMarginBottom();
        }
        double[] dh = {dl, dr, dr, dl};
        double[] dv = {dt, dt, db, db};
        // 모서리마다 그 모서리에 닿는 변의 두께만큼 반지름을 줄이거나(안쪽) 늘린다(바깥쪽, 0 인 반지름은 그대로).
        for (int i = 0; i < 4; i++) {
            if (r[2 * i] > 0 || r[2 * i + 1] > 0) {
                r[2 * i] = Math.max(0, r[2 * i] + dh[i]);
                r[2 * i + 1] = Math.max(0, r[2 * i + 1] + dv[i]);
            }
        }
        double w = ref[2];
        double h = ref[3];
        double f = 1;
        f = Math.min(f, fit(w, r[0] + r[2]));
        f = Math.min(f, fit(h, r[3] + r[5]));
        f = Math.min(f, fit(w, r[4] + r[6]));
        f = Math.min(f, fit(h, r[7] + r[1]));
        // 겹치는 반지름은 같은 비율로 줄인다(CSS Backgrounds 3 5.5 절).
        for (int i = 0; i < 8; i++) {
            r[i] *= f;
        }
        return ClipPaths.roundedRect(ref[0], ref[1], w, h, r);
    }

    /**
     * Returns the factor that makes two radii fit into a side.
     * <p>
     * 두 반지름이 변에 들어맞게 하는 계수를 반환한다.
     *
     * @param side the length of the side / 변의 길이
     * @param sum  the sum of the radii / 반지름의 합
     * @return the factor, at most 1 / 계수(1 이하)
     */
    private static double fit(double side, double sum) {
        return sum > side && sum > 0 ? Math.max(0, side) / sum : 1;
    }

    /**
     * Reads the horizontal and the vertical radius of a corner value (a length, a percentage of the border box, or a pair of them).
     * <p>
     * 모서리 값(길이, border box 의 백분율, 또는 그 쌍)의 가로와 세로 반지름을 읽는다.
     *
     * @param v the value or null / 값 또는 null
     * @param w the width of the border box / border box 의 너비
     * @param h the height of the border box / border box 의 높이
     * @return {horizontal, vertical} / {가로, 세로}
     */
    private static double[] cornerRadius(CssValue v, double w, double h) {
        List<CssValue> items = v == null ? java.util.Collections.<CssValue>emptyList() : v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        if (items.isEmpty()) {
            return new double[] {0, 0};
        }
        double rh = lengthOf(items.get(0), w);
        double rv = lengthOf(items.get(items.size() > 1 ? 1 : 0), h);
        return new double[] {Double.isNaN(rh) ? 0 : Math.max(0, rh), Double.isNaN(rv) ? 0 : Math.max(0, rv)};
    }

    /**
     * Resolves a length or a percentage.
     * <p>
     * 길이나 백분율을 푼다.
     *
     * @param v    the value / 값
     * @param base the base of a percentage / 백분율의 기준
     * @return pixels or NaN / 픽셀 또는 NaN
     */
    private static double lengthOf(CssValue v, double base) {
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssLength) {
            double px = ((org.duckdns.hjow.swingwebpanel.css.values.CssLength) v).absolutePx();
            return Double.isNaN(px) && ((org.duckdns.hjow.swingwebpanel.css.values.CssLength) v).isPx() ? ((org.duckdns.hjow.swingwebpanel.css.values.CssLength) v).getValue() : px;
        }
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssPercentage) {
            return ((org.duckdns.hjow.swingwebpanel.css.values.CssPercentage) v).getValue() * base / 100.0;
        }
        return v instanceof CssNumber && ((CssNumber) v).getValue() == 0 ? 0 : Double.NaN;
    }

    /**
     * Builds the path of a style.
     * <p>
     * 스타일의 경로를 만든다.
     *
     * @param b   the box / 상자
     * @param s   the style / 스타일
     * @param ref the reference box / 기준 상자
     * @return the path, or null if it cannot be used / 경로. 쓸 수 없으면 null
     */
    private static MotionPath pathOf(LayoutBox b, ComputedStyle s, double[] ref) {
        CssValue v = s.get(Prop.OFFSET_PATH);
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        CssFunctionValue fn = null;
        // 값에서 도형 함수를 찾는다.
        for (int i = 0; i < items.size() && fn == null; i++) {
            if (items.get(i) instanceof CssFunctionValue) {
                fn = (CssFunctionValue) items.get(i);
            }
        }
        double[] start = startingPosition(b, s, ref);
        MotionPath p = new MotionPath();
        if (fn != null && fn.getName().equals("ray")) {
            return p.ray(fn, ref, start, s.fontSizePx());
        }
        LayoutBox cb = containingBox(b);
        org.duckdns.hjow.swingwebpanel.css.values.CssUrl url = null;
        // 값에서 url() 을 찾는다.
        for (int i = 0; i < items.size() && url == null; i++) {
            if (items.get(i) instanceof org.duckdns.hjow.swingwebpanel.css.values.CssUrl) {
                url = (org.duckdns.hjow.swingwebpanel.css.values.CssUrl) items.get(i);
            }
        }
        Shape shape;
        if (url != null) {
            // SVG 도형 요소의 경로를 좌표 상자의 왼쪽 위를 원점으로 놓는다. 도형이 아니면 path("m 0 0") 이다.
            shape = SvgShapePaths.of(b.node, url.getUrl(), ref);
        } else if (fn == null) {
            shape = boxPath(cb, ref, coordBox(s));
        } else {
            shape = ClipPaths.shapeFor(cb, s, Prop.OFFSET_PATH, "border-box", cb.getX(), cb.getY());
        }
        if (shape == null) {
            return null;
        }
        AffineTransform shift = null;
        // offset-position 이 있으면 path() 는 그 점이 원점이고, at 이 없는 원과 타원은 그 점이 중심이다.
        if (start != null && fn != null && fn.getName().equals("path")) {
            shift = AffineTransform.getTranslateInstance(start[0] - ref[0], start[1] - ref[1]);
        } else if (start != null && fn != null && (fn.getName().equals("circle") || fn.getName().equals("ellipse")) && !hasAt(fn)) {
            shift = AffineTransform.getTranslateInstance(start[0] - (ref[0] + ref[2] / 2), start[1] - (ref[1] + ref[3] / 2));
        }
        p.flatten(shape, shift);
        // 길이가 없는 경로(path("m 0 0") 같은 점 하나)는 그 점에 상자를 놓는다.
        if (p.segments.isEmpty() && p.point != null) {
            return p;
        }
        return p.total >= 0 && !p.segments.isEmpty() ? p : null;
    }

    /**
     * Tells whether a shape function has an <code>at</code> position.
     * <p>
     * 도형 함수에 <code>at</code> 위치가 있는지 알려 준다.
     *
     * @param fn the function / 함수
     * @return true if it has one / 있으면 true
     */
    private static boolean hasAt(CssFunctionValue fn) {
        // 인수를 풀어 헤쳐 at 키워드를 찾는다.
        for (CssValue a : fn.getArguments()) {
            List<CssValue> items = a instanceof CssList ? ((CssList) a).getItems() : java.util.Collections.singletonList(a);
            // 인수 안의 항목에서 at 를 찾는다.
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isKeyword("at")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the offset starting position in document coordinates: <code>auto</code> is the top-left corner of the box itself, a position is placed in the reference
     * box, <code>normal</code> is none (null).
     * <p>
     * 문서 좌표의 오프셋 시작 위치를 반환한다: <code>auto</code> 는 상자 자신의 왼쪽 위 모서리, 위치는 기준 상자 안에 놓은 점, <code>normal</code> 은 없음(null).
     *
     * @param b   the box / 상자
     * @param s   the style / 스타일
     * @param ref the reference box / 기준 상자
     * @return {x, y} or null / {x, y}. 없으면 null
     */
    private static double[] startingPosition(LayoutBox b, ComputedStyle s, double[] ref) {
        CssValue v = s.get(Prop.OFFSET_POSITION);
        if (v == null || v.isKeyword("normal")) {
            return null;
        }
        if (v.isKeyword("auto")) {
            return new double[] {b.getX(), b.getY()};
        }
        double[] o = Transforms.origin(v, ref[2], ref[3]);
        return new double[] {ref[0] + o[0], ref[1] + o[1]};
    }

    /**
     * Flattens a shape into the segments of the path; a subpath that ends with a close makes the path a closed loop if it is the last one.
     * <p>
     * 도형을 경로의 구간으로 편다. 닫는 명령으로 끝나는 하위 경로가 마지막이면 닫힌 고리가 된다.
     *
     * @param shape the shape / 도형
     * @param shift the translation to apply first, or null / 먼저 적용할 이동. 없으면 null
     */
    private void flatten(Shape shape, AffineTransform shift) {
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(shift), FLATNESS);
        double[] c = new double[6];
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        boolean lastClosed = false;
        // 경로의 구간을 하나씩 읽어 직선 구간 목록으로 만든다(moveTo 로 생긴 틈은 길이에 넣지 않는다).
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) {
                sx = c[0];
                sy = c[1];
                if (point == null) {
                    point = new double[] {sx, sy};
                }
                cx = sx;
                cy = sy;
                lastClosed = false;
            } else if (type == PathIterator.SEG_LINETO) {
                add(cx, cy, c[0], c[1]);
                cx = c[0];
                cy = c[1];
                lastClosed = false;
            } else if (type == PathIterator.SEG_CLOSE) {
                add(cx, cy, sx, sy);
                cx = sx;
                cy = sy;
                lastClosed = true;
            }
            it.next();
        }
        closed = lastClosed;
    }

    /**
     * Adds a straight segment to the path.
     * <p>
     * 경로에 직선 구간을 더한다.
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x1 end x / 끝 x
     * @param y1 end y / 끝 y
     */
    private void add(double x0, double y0, double x1, double y1) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        if (len <= 0) {
            return;
        }
        segments.add(new double[] {x0, y0, x1, y1, total});
        total += len;
    }

    /**
     * Makes the path of a <code>ray()</code>: a line from the starting point with the bearing angle (0 degrees is up, clockwise) as long as the size keyword says,
     * measured in the reference box; <code>contain</code> keeps the path an interval, and without it the distance is not limited.
     * <p>
     * <code>ray()</code> 의 경로를 만든다: 시작점에서 방위각(0도는 위, 시계 방향)으로 뻗는 선이며 길이는 크기 키워드가 기준 상자에서 정한다. <code>contain</code> 이 있으면 구간이고 없으면
     * 거리에 제한이 없다.
     *
     * @param fn    the function / 함수
     * @param ref   the reference box / 기준 상자
     * @param start the offset starting position or null / 오프셋 시작 위치. 없으면 null
     * @param font  the font size for <code>em</code> / <code>em</code> 용 글꼴 크기
     * @return the path / 경로
     */
    private MotionPath ray(CssFunctionValue fn, double[] ref, double[] start, double font) {
        List<CssValue> t = new ArrayList<CssValue>();
        // 인수를 하나의 토큰 목록으로 편다.
        for (CssValue a : fn.getArguments()) {
            if (a instanceof CssList) {
                t.addAll(((CssList) a).getItems());
            } else {
                t.add(a);
            }
        }
        double angle = 0;
        String size = "closest-side";
        boolean contain = false;
        double[] origin = start != null ? start : new double[] {ref[0] + ref[2] / 2, ref[1] + ref[3] / 2};
        int at = -1;
        // 각도, 크기 키워드, contain, at 위치를 읽는다.
        for (int i = 0; i < t.size(); i++) {
            CssValue it = t.get(i);
            if (it instanceof CssDimension && ((CssDimension) it).getKind() == CssUnits.ANGLE) {
                angle = ((CssDimension) it).canonical();
            } else if (it.isKeyword("contain")) {
                contain = true;
            } else if (it.isKeyword("at")) {
                at = i + 1;
                break;
            } else if (it instanceof CssKeyword) {
                size = ((CssKeyword) it).getName();
            }
        }
        // at 뒤의 위치는 기준 상자 안에 놓는다.
        if (at > 0 && at < t.size()) {
            List<CssValue> pos = new ArrayList<CssValue>(t.subList(at, t.size()));
            CssValue pv = pos.size() == 1 ? pos.get(0) : new CssList(pos, false);
            double[] o = Transforms.origin(pv, ref[2], ref[3]);
            origin = new double[] {ref[0] + o[0], ref[1] + o[1]};
        }
        double dx = Math.sin(Math.toRadians(angle));
        double dy = -Math.cos(Math.toRadians(angle));
        double length = rayLength(size, origin, ref, dx, dy);
        add(origin[0], origin[1], origin[0] + dx * length, origin[1] + dy * length);
        unbounded = !contain;
        closed = false;
        if (segments.isEmpty()) {
            // 길이 0 의 ray 는 시작점에서 방향만 가진다.
            segments.add(new double[] {origin[0], origin[1], origin[0] + dx, origin[1] + dy, 0});
        }
        return this;
    }

    /**
     * Returns the length of a ray for a size keyword (2.1.1): the distance to the closest or the farthest side or corner of the reference box, or, for
     * <code>sides</code>, to the point where the ray leaves the box (0 if it starts outside).
     * <p>
     * 크기 키워드에 대한 ray 의 길이를 반환한다(2.1.1): 기준 상자의 가장 가까운 또는 가장 먼 변이나 모서리까지의 거리, <code>sides</code> 이면 ray 가 상자를 벗어나는 점까지의
     * 거리(바깥에서 시작하면 0).
     *
     * @param size   the keyword / 키워드
     * @param origin the starting point / 시작점
     * @param ref    the reference box / 기준 상자
     * @param dx     the x of the direction / 방향의 x
     * @param dy     the y of the direction / 방향의 y
     * @return the length / 길이
     */
    private static double rayLength(String size, double[] origin, double[] ref, double dx, double dy) {
        double left = origin[0] - ref[0];
        double right = ref[0] + ref[2] - origin[0];
        double top = origin[1] - ref[1];
        double bottom = ref[1] + ref[3] - origin[1];
        boolean inside = left >= 0 && right >= 0 && top >= 0 && bottom >= 0;
        if (size.equals("sides")) {
            if (!inside) {
                return 0;
            }
            double best = Double.MAX_VALUE;
            // 방향이 향하는 변까지의 거리 중 가장 작은 것이 상자를 벗어나는 점이다.
            if (dx > 1e-12) {
                best = Math.min(best, right / dx);
            } else if (dx < -1e-12) {
                best = Math.min(best, left / -dx);
            }
            if (dy > 1e-12) {
                best = Math.min(best, bottom / dy);
            } else if (dy < -1e-12) {
                best = Math.min(best, top / -dy);
            }
            return best == Double.MAX_VALUE ? 0 : best;
        }
        if (size.equals("farthest-side")) {
            return Math.max(Math.max(Math.abs(left), Math.abs(right)), Math.max(Math.abs(top), Math.abs(bottom)));
        }
        if (size.equals("closest-corner") || size.equals("farthest-corner")) {
            double a = Math.hypot(left, top);
            double b = Math.hypot(right, top);
            double c = Math.hypot(left, bottom);
            double d = Math.hypot(right, bottom);
            return size.equals("closest-corner") ? Math.min(Math.min(a, b), Math.min(c, d)) : Math.max(Math.max(a, b), Math.max(c, d));
        }
        return inside ? Math.min(Math.min(left, right), Math.min(top, bottom)) : 0;
    }

    /**
     * Returns the point of the path at a distance and the direction there: {x, y, direction in degrees} (the direction is the angle of the tangent seen on the
     * screen, 0 degrees to the right). The distance is made the used distance first (2.2.1): modulo the length for a closed loop, clamped for an interval, as it is for
     * a ray without <code>contain</code>.
     * <p>
     * 거리에 해당하는 경로 위의 점과 그 곳의 방향 {x, y, 방향(도)}을 반환한다(방향은 화면에서 본 접선의 각도이며 오른쪽이 0도). 거리는 먼저 실제 거리가 된다(2.2.1): 닫힌 고리는 길이로
     * 나눈 나머지, 구간은 한정한 값, <code>contain</code> 없는 ray 는 그대로.
     *
     * @param distance the offset distance in pixels / 오프셋 거리(픽셀)
     * @return {x, y, direction} / {x, y, 방향}
     */
    double[] pointAt(double distance) {
        if (segments.isEmpty()) {
            return new double[] {point == null ? 0 : point[0], point == null ? 0 : point[1], 0};
        }
        double d = distance;
        if (!unbounded) {
            if (closed) {
                d = total <= 0 ? 0 : ((d % total) + total) % total;
            } else {
                d = Math.max(0, Math.min(total, d));
            }
        }
        double[] seg = segments.get(0);
        // 거리가 놓인 구간을 찾는다(범위 밖이면 처음이나 끝 구간을 연장한다).
        for (int i = 0; i < segments.size(); i++) {
            double[] s = segments.get(i);
            seg = s;
            double len = Math.hypot(s[2] - s[0], s[3] - s[1]);
            if (d <= s[4] + len) {
                break;
            }
        }
        double len = Math.hypot(seg[2] - seg[0], seg[3] - seg[1]);
        double ux = len == 0 ? 1 : (seg[2] - seg[0]) / len;
        double uy = len == 0 ? 0 : (seg[3] - seg[1]) / len;
        double along = d - seg[4];
        return new double[] {seg[0] + ux * along, seg[1] + uy * along, Math.toDegrees(Math.atan2(uy, ux))};
    }

    /**
     * Returns the total length of the path.
     * <p>
     * 경로의 전체 길이를 반환한다.
     *
     * @return the length in pixels / 길이(픽셀)
     */
    double length() {
        return total;
    }

    /**
     * Builds the path of a box for tests and tools: the same path that {@link #boxTransform} follows.
     * <p>
     * 시험과 도구를 위해 상자의 경로를 만든다: {@link #boxTransform} 이 따르는 경로와 같다.
     *
     * @param b the box / 상자
     * @return the path, or null if the box has none / 경로. 없으면 null
     */
    static MotionPath of(LayoutBox b) {
        return pathOf(b, b.getStyle(), referenceBox(b, b.getStyle()));
    }

    /**
     * Tells whether the path is a closed loop.
     * <p>
     * 경로가 닫힌 고리인지 알려 준다.
     *
     * @return true for a closed loop / 닫힌 고리이면 true
     */
    boolean isClosed() {
        return closed;
    }

}
