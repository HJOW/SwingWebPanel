package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssOperator;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.media.image.svg.PathShapes;

/**
 * The shape of <code>clip-path</code> (plan items P10-08, CSS Masking Level 1 section 5 "Clipping paths",
 * https://www.w3.org/TR/css-masking-1/, Candidate Recommendation Draft 2021-08-05, retrieved 2026-11-17, and CSS Shapes Level 1 section 3 for the
 * basic shapes): the basic shapes <code>inset()</code> (with <code>round</code> and one to four radii per axis like <code>border-radius</code>),
 * <code>circle()</code>, <code>ellipse()</code>, <code>polygon()</code> (with <code>nonzero</code> and <code>evenodd</code>), <code>rect()</code>,
 * <code>xywh()</code> and <code>path()</code> (SVG path data with <code>nonzero</code> or <code>evenodd</code>), with the reference boxes
 * <code>border-box</code> (the default), <code>padding-box</code>, <code>content-box</code> and <code>margin-box</code> (<code>fill-box</code> is the
 * content box, <code>stroke-box</code> and <code>view-box</code> are the border box). A reference box alone clips to the (square) edge of that box.
 * The shape is a {@link Shape} in document coordinates; everything outside of it is neither painted nor hit (CSS Masking 1 section 5.1). A
 * <code>url()</code> reference to an SVG <code>clipPath</code>, <code>shape()</code> and a <code>path()</code> without a drawing segment are not
 * supported: such a declaration clips nothing. A box with <code>clip-path</code> other than <code>none</code> is a stacking context (like
 * <code>opacity</code>) and the containing block of its fixed and absolutely positioned descendants.
 * <p>
 * <code>clip-path</code> 의 모양(계획 P10-08, CSS Masking Level 1 5 절 "Clipping paths", https://www.w3.org/TR/css-masking-1/, Candidate Recommendation Draft 2021-08-05,
 * 조회 2026-11-17, 기본 도형은 CSS Shapes Level 1 3 절): 기본 도형 <code>inset()</code>(<code>round</code> 와 <code>border-radius</code> 처럼 축마다 1~4 개 반지름),
 * <code>circle()</code>, <code>ellipse()</code>, <code>polygon()</code>(<code>nonzero</code>, <code>evenodd</code>), <code>rect()</code>, <code>xywh()</code>, <code>path()</code>(SVG 경로 데이터,
 * <code>nonzero</code> 나 <code>evenodd</code>)과 기준 상자 <code>border-box</code>(기본), <code>padding-box</code>, <code>content-box</code>, <code>margin-box</code>(<code>fill-box</code> 는 content
 * 상자, <code>stroke-box</code> 와 <code>view-box</code> 는 border 상자). 기준 상자만 쓰면 그 상자의 (각진) 가장자리로 자른다. 모양은 문서 좌표의 {@link Shape} 이며 그 밖은
 * 그려지지도 히트되지도 않는다(CSS Masking 1 5.1 절). SVG <code>clipPath</code> 를 가리키는 <code>url()</code>, <code>shape()</code>, 그리는 구간이 없는 <code>path()</code> 는
 * 지원하지 않으며 그런 선언은 아무것도 자르지 않는다. <code>clip-path</code> 가 <code>none</code> 이 아닌 상자는 (<code>opacity</code> 처럼) 쌓임 맥락이며 자신의
 * fixed 와 절대 배치 자손의 포함 블록이다.
 */
public final class ClipPaths {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ClipPaths() {
    }

    /**
     * Tells whether a style has a <code>clip-path</code> other than <code>none</code>.
     * <p>
     * 스타일에 <code>none</code> 이 아닌 <code>clip-path</code> 가 있는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if there is a clip path / 자르는 경로가 있으면 true
     */
    public static boolean has(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        CssValue v = s.get(Prop.CLIP_PATH);
        return v != null && !v.isKeyword("none") && !(v instanceof CssList && ((CssList) v).getItems().isEmpty());
    }

    /**
     * Returns the id of the SVG <code>clipPath</code> element that <code>clip-path: url(#id)</code> refers to (CSS Masking Level 1 section 5.1: a
     * reference to a <code>clipPath</code> element of the same document).
     * <p>
     * <code>clip-path: url(#id)</code> 가 가리키는 SVG <code>clipPath</code> 요소의 id 를 반환한다(CSS Masking Level 1 5.1 절: 같은 문서의 <code>clipPath</code> 요소 참조).
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the id without the <code>#</code>, or null if the value is not such a reference / <code>#</code> 를 뺀 id. 그런 참조가 아니면 null
     */
    public static String reference(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.CLIP_PATH);
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : v == null ? java.util.Collections.<CssValue>emptyList() : java.util.Collections.singletonList(v);
        // 값의 항목 중 같은 문서를 가리키는 url(#id) 를 찾는다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it instanceof org.duckdns.hjow.swingwebpanel.css.values.CssUrl) {
                String u = ((org.duckdns.hjow.swingwebpanel.css.values.CssUrl) it).getUrl();
                return u != null && u.startsWith("#") && u.length() > 1 ? u.substring(1) : null;
            }
        }
        return null;
    }

    /**
     * Returns the shape that clips a box, in document coordinates.
     * <p>
     * 상자를 자르는 모양을 문서 좌표로 반환한다.
     *
     * @param b the box / 상자
     * @return the shape, or null if the value cannot be used (nothing is clipped) / 모양. 값을 쓸 수 없으면 null(아무것도 자르지 않는다)
     */
    public static Shape shapeOf(LayoutBox b) {
        ComputedStyle s = b.getStyle();
        // 참조(url(#id))는 모양이 아니라 SVG clipPath 요소로 래스터에서 자르므로 모양이 없다.
        if (!has(s) || reference(s) != null) {
            return null;
        }
        return shapeFor(b, Prop.CLIP_PATH, "border-box", b.getX(), b.getY());
    }

    /**
     * Returns the shape that a basic-shape property of a box describes (<code>clip-path</code>, <code>shape-outside</code>): the value of
     * the property is read like the value of <code>clip-path</code>, the reference box is the one that the value names or the given default,
     * and the border box of the box is taken to be at the given position (a float is laid out before its place in the document is known).
     * <p>
     * 상자의 기본 도형 속성(<code>clip-path</code>, <code>shape-outside</code>)이 나타내는 모양을 반환한다: 속성 값은 <code>clip-path</code> 의 값처럼 읽고, 기준 상자는
     * 값이 가리키는 것이며 없으면 주어진 기본값이다. 상자의 border box 는 주어진 위치에 있는 것으로 본다(float 는 문서에서의 자리가 정해지기 전에 배치된다).
     *
     * @param b          the box with its size, margins, borders and paddings / 크기, margin, border, padding 가 정해진 상자
     * @param prop       the property id / 속성 id
     * @param defaultBox the reference box when the value names none / 값이 기준 상자를 지정하지 않을 때의 기준 상자
     * @param bx         left of the border box / border box 의 왼쪽
     * @param by         top of the border box / border box 의 위쪽
     * @return the shape or null if the value is none or cannot be used / 모양. 값이 none 이거나 쓸 수 없으면 null
     */
    static Shape shapeFor(LayoutBox b, int prop, String defaultBox, double bx, double by) {
        return shapeFor(b, b.getStyle(), prop, defaultBox, bx, by);
    }

    /**
     * Makes the shape that a property of a style describes against the reference box of another box: the value comes from <code>s</code> and the boxes and the
     * percentages are those of <code>b</code>. <code>offset-path</code> uses it: the path of an element is a shape in the box of its containing block.
     * <p>
     * 어떤 스타일의 속성이 서술하는 모양을 다른 상자의 기준 상자에 대해 만든다: 값은 <code>s</code> 에서 오고 상자와 백분율은 <code>b</code> 의 것이다.
     * <code>offset-path</code> 가 쓴다: 요소의 경로는 포함 블록의 상자 안에 있는 모양이다.
     *
     * @param b          the box that gives the reference boxes / 기준 상자를 주는 상자
     * @param s          the style that holds the value / 값을 가진 스타일
     * @param prop       the property id / 속성 id
     * @param defaultBox the reference box when the value names none / 값이 기준 상자를 지정하지 않을 때의 기준 상자
     * @param bx         left of the border box of <code>b</code> / <code>b</code> 의 border box 왼쪽
     * @param by         top of the border box of <code>b</code> / <code>b</code> 의 border box 위쪽
     * @return the shape or null / 모양. 없으면 null
     */
    static Shape shapeFor(LayoutBox b, ComputedStyle s, int prop, String defaultBox, double bx, double by) {
        CssValue v = s.get(prop);
        if (v == null || v.isKeyword("none")) {
            return null;
        }
        List<CssValue> items = new ArrayList<CssValue>();
        if (v instanceof CssList) {
            items.addAll(((CssList) v).getItems());
        } else {
            items.add(v);
        }
        CssFunctionValue fn = null;
        String box = defaultBox;
        boolean hasBox = false;
        // 값의 항목에서 기본 도형 함수와 기준 상자 키워드를 찾는다.
        for (CssValue it : items) {
            if (it instanceof CssFunctionValue && fn == null) {
                fn = (CssFunctionValue) it;
            } else if (it instanceof CssKeyword) {
                box = ((CssKeyword) it).getName();
                hasBox = true;
            }
        }
        if (fn == null && !hasBox) {
            return null;
        }
        double x = bx;
        double y = by;
        double w = b.getWidth();
        double h = b.getHeight();
        // 마스킹의 기하 상자 이름 중 fill-box 는 content-box, stroke-box 와 view-box 는 border-box 로 본다.
        if ("fill-box".equals(box)) {
            box = "content-box";
        } else if ("stroke-box".equals(box) || "view-box".equals(box)) {
            box = "border-box";
        }
        if ("padding-box".equals(box) || "content-box".equals(box)) {
            x += b.getBorderLeft();
            y += b.getBorderTop();
            w -= b.getBorderLeft() + b.getBorderRight();
            h -= b.getBorderTop() + b.getBorderBottom();
            if ("content-box".equals(box)) {
                x += b.getPaddingLeft();
                y += b.getPaddingTop();
                w -= b.getPaddingLeft() + b.getPaddingRight();
                h -= b.getPaddingTop() + b.getPaddingBottom();
            }
        } else if ("margin-box".equals(box)) {
            x -= b.getMarginLeft();
            y -= b.getMarginTop();
            w += b.getMarginLeft() + b.getMarginRight();
            h += b.getMarginTop() + b.getMarginBottom();
        }
        w = Math.max(0, w);
        h = Math.max(0, h);
        double font = s.fontSizePx();
        // 기준 상자만 있으면 그 상자의 가장자리로 자른다.
        if (fn == null) {
            return new Rectangle2D.Double(x, y, w, h);
        }
        String n = fn.getName();
        if ("inset".equals(n)) {
            return inset(fn, x, y, w, h, font);
        }
        if ("circle".equals(n) || "ellipse".equals(n)) {
            return round(fn, "circle".equals(n), x, y, w, h, font);
        }
        if ("polygon".equals(n)) {
            return polygon(fn, x, y, w, h, font);
        }
        if ("rect".equals(n) || "xywh".equals(n)) {
            return box4(fn, "xywh".equals(n), x, y, w, h, font);
        }
        if ("path".equals(n)) {
            return path(fn, x, y);
        }
        return null;
    }

    /**
     * Flattens the arguments of a function into one list of tokens (the space separated parts of an argument are one list).
     * <p>
     * 함수의 인수를 토큰 목록 하나로 편다(인수의 공백으로 나뉜 부분은 목록 하나이다).
     *
     * @param fn the function / 함수
     * @return the tokens / 토큰
     */
    private static List<CssValue> tokens(CssFunctionValue fn) {
        List<CssValue> out = new ArrayList<CssValue>();
        // 인수마다 목록이면 펼쳐서 더한다.
        for (CssValue a : fn.getArguments()) {
            if (a instanceof CssList) {
                out.addAll(((CssList) a).getItems());
            } else {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * Resolves a length or a percentage to pixels.
     * <p>
     * 길이나 백분율을 픽셀로 푼다.
     *
     * @param v    the value / 값
     * @param base the base of a percentage / 백분율의 기준
     * @param font the font size for <code>em</code> / <code>em</code> 용 글꼴 크기
     * @return pixels, NaN if the value is neither / 픽셀. 둘 다 아니면 NaN
     */
    private static double len(CssValue v, double base, double font) {
        if (v instanceof CssLength) {
            CssLength l = (CssLength) v;
            double px = l.absolutePx();
            if (!Double.isNaN(px)) {
                return px;
            }
            return l.toPx(new LengthContext(font, font, font * 1.2, 800, 600));
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() / 100.0 * base;
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), base);
        }
        return v instanceof CssNumber && ((CssNumber) v).getValue() == 0 ? 0 : Double.NaN;
    }

    /**
     * Makes the shape of <code>inset(top right bottom left round radius)</code>.
     * <p>
     * <code>inset(위 오른쪽 아래 왼쪽 round 반지름)</code> 의 모양을 만든다.
     *
     * @param fn   the function / 함수
     * @param x    left of the reference box / 기준 상자 왼쪽
     * @param y    top / 위쪽
     * @param w    width / 너비
     * @param h    height / 높이
     * @param font the font size / 글꼴 크기
     * @return the shape or null / 모양 또는 null
     */
    private static Shape inset(CssFunctionValue fn, double x, double y, double w, double h, double font) {
        List<CssValue> t = tokens(fn);
        int round = -1;
        // round 키워드의 위치를 찾는다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).isKeyword("round")) {
                round = i;
            }
        }
        int end = round < 0 ? t.size() : round;
        if (end < 1 || end > 4) {
            return null;
        }
        double[] v = new double[4];
        // 값 1~4 개를 위, 오른쪽, 아래, 왼쪽으로 펼친다(CSS 여백 단축 속성처럼).
        for (int i = 0; i < 4; i++) {
            int src = end == 1 ? 0 : (end == 2 ? i % 2 : (end == 3 ? (i == 3 ? 1 : i) : i));
            double base = (i % 2 == 0) ? h : w;
            v[i] = len(t.get(src), base, font);
            if (Double.isNaN(v[i])) {
                return null;
            }
        }
        double left = x + v[3];
        double top = y + v[0];
        double rw = Math.max(0, w - v[1] - v[3]);
        double rh = Math.max(0, h - v[0] - v[2]);
        return withRadii(t, round, left, top, rw, rh, font);
    }

    /**
     * Makes the rectangle of an inset-like function with the optional <code>round</code> radii (the value of <code>border-radius</code>:
     * one to four horizontal radii, then after a slash one to four vertical radii).
     * <p>
     * inset 꼴 함수의 사각형을 선택적인 <code>round</code> 반지름(<code>border-radius</code> 의 값: 가로 반지름 1~4 개, 슬래시 뒤에 세로 반지름 1~4 개)과 함께 만든다.
     *
     * @param t     the flat tokens of the function / 함수의 평평한 토큰
     * @param round the index of the <code>round</code> token, or -1 / <code>round</code> 토큰의 위치. 없으면 -1
     * @param left  left of the rectangle / 사각형의 왼쪽
     * @param top   top / 위쪽
     * @param rw    width / 너비
     * @param rh    height / 높이
     * @param font  the font size / 글꼴 크기
     * @return the shape or null / 모양 또는 null
     */
    private static Shape withRadii(List<CssValue> t, int round, double left, double top, double rw, double rh, double font) {
        if (round < 0) {
            return new Rectangle2D.Double(left, top, rw, rh);
        }
        double[][] axes = new double[2][];
        List<CssValue> cur = new ArrayList<CssValue>();
        int axis = 0;
        // round 뒤의 토큰을 슬래시 앞(가로)과 뒤(세로)로 나눠 반지름 목록을 만든다.
        for (int i = round + 1; i <= t.size(); i++) {
            boolean end = i == t.size();
            if (end || t.get(i) == CssOperator.SLASH) {
                if (cur.isEmpty() || cur.size() > 4 || axis > 1) {
                    return null;
                }
                double[] four = new double[4];
                // 1~4 개 값을 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래로 펼친다.
                for (int c = 0; c < 4; c++) {
                    int src = cur.size() == 1 ? 0 : (cur.size() == 2 ? c % 2 : (cur.size() == 3 ? (c == 3 ? 1 : c) : c));
                    four[c] = len(cur.get(src), axis == 0 ? rw : rh, font);
                    if (Double.isNaN(four[c]) || four[c] < 0) {
                        return null;
                    }
                }
                axes[axis++] = four;
                cur = new ArrayList<CssValue>();
            } else {
                cur.add(t.get(i));
            }
        }
        double[] hr = axes[0];
        double[] vr = axes[1] != null ? axes[1] : axes[0];
        double[] r = { hr[0], vr[0], hr[1], vr[1], hr[2], vr[2], hr[3], vr[3] };
        double f = 1;
        // 변마다 두 반지름의 합이 변의 길이를 넘으면 모두 같은 비율로 줄인다(CSS Backgrounds 3 5.5 절).
        f = Math.min(f, factor(rw, r[0] + r[2]));
        f = Math.min(f, factor(rh, r[3] + r[5]));
        f = Math.min(f, factor(rw, r[4] + r[6]));
        f = Math.min(f, factor(rh, r[7] + r[1]));
        // 모든 반지름에 같은 계수를 곱한다.
        for (int i = 0; i < 8; i++) {
            r[i] *= f;
        }
        return roundedRect(left, top, rw, rh, r);
    }

    /**
     * Returns the factor that makes the two radii of a side fit into it.
     * <p>
     * 변의 두 반지름이 들어맞게 하는 계수를 반환한다.
     *
     * @param side length of the side / 변의 길이
     * @param sum  sum of the two radii / 두 반지름의 합
     * @return the factor, at most 1 / 계수(1 이하)
     */
    private static double factor(double side, double sum) {
        return sum > side && sum > 0 ? Math.max(0, side) / sum : 1;
    }

    /**
     * Makes a rectangle with elliptical corners from the eight radii {top-left h, v, top-right h, v, bottom-right h, v, bottom-left h, v}.
     * <p>
     * 여덟 반지름 {왼쪽 위 가로, 세로, 오른쪽 위 가로, 세로, 오른쪽 아래 가로, 세로, 왼쪽 아래 가로, 세로} 로 타원 모서리가 있는 사각형을 만든다.
     *
     * @param x left / 왼쪽
     * @param y top / 위쪽
     * @param w width / 너비
     * @param h height / 높이
     * @param r the radii / 반지름
     * @return the shape / 모양
     */
    static Shape roundedRect(double x, double y, double w, double h, double[] r) {
        boolean rounded = false;
        // 반지름이 하나라도 양수인지 본다.
        for (int i = 0; i < 8; i++) {
            rounded |= r[i] > 0;
        }
        if (!rounded) {
            return new Rectangle2D.Double(x, y, w, h);
        }
        double k = 0.5522847498307936;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x + r[0], y);
        p.lineTo(x + w - r[2], y);
        p.curveTo(x + w - r[2] + r[2] * k, y, x + w, y + r[3] - r[3] * k, x + w, y + r[3]);
        p.lineTo(x + w, y + h - r[5]);
        p.curveTo(x + w, y + h - r[5] + r[5] * k, x + w - r[4] + r[4] * k, y + h, x + w - r[4], y + h);
        p.lineTo(x + r[6], y + h);
        p.curveTo(x + r[6] - r[6] * k, y + h, x, y + h - r[7] + r[7] * k, x, y + h - r[7]);
        p.lineTo(x, y + r[1]);
        p.curveTo(x, y + r[1] - r[1] * k, x + r[0] - r[0] * k, y, x + r[0], y);
        p.closePath();
        return p;
    }

    /**
     * Makes the shape of <code>rect(top right bottom left round radii)</code> (edges measured from the top-left of the reference box,
     * <code>auto</code> is the edge of the reference box) or <code>xywh(x y width height round radii)</code>.
     * <p>
     * <code>rect(위 오른쪽 아래 왼쪽 round 반지름)</code>(기준 상자의 왼쪽 위에서 잰 가장자리, <code>auto</code> 는 기준 상자의 가장자리)이나
     * <code>xywh(x y 너비 높이 round 반지름)</code> 의 모양을 만든다.
     *
     * @param fn   the function / 함수
     * @param xywh true for <code>xywh()</code> / <code>xywh()</code> 이면 true
     * @param x    left of the reference box / 기준 상자 왼쪽
     * @param y    top / 위쪽
     * @param w    width / 너비
     * @param h    height / 높이
     * @param font the font size / 글꼴 크기
     * @return the shape or null / 모양 또는 null
     */
    private static Shape box4(CssFunctionValue fn, boolean xywh, double x, double y, double w, double h, double font) {
        List<CssValue> t = tokens(fn);
        int round = -1;
        // round 키워드의 위치를 찾는다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).isKeyword("round")) {
                round = i;
            }
        }
        int end = round < 0 ? t.size() : round;
        if (end != 4) {
            return null;
        }
        double[] v = new double[4];
        // 네 값을 읽는다(rect 는 auto 를 허용하며 NaN 으로 둔다).
        for (int i = 0; i < 4; i++) {
            CssValue c = t.get(i);
            if (!xywh && c.isKeyword("auto")) {
                v[i] = Double.NaN;
                continue;
            }
            v[i] = len(c, xywh ? (i % 2 == 0 ? w : h) : (i % 2 == 0 ? h : w), font);
            if (Double.isNaN(v[i]) || (xywh && i >= 2 && v[i] < 0)) {
                return null;
            }
        }
        double left;
        double top;
        double rw;
        double rh;
        if (xywh) {
            left = x + v[0];
            top = y + v[1];
            rw = v[2];
            rh = v[3];
        } else {
            double tp = Double.isNaN(v[0]) ? 0 : v[0];
            double rt = Double.isNaN(v[1]) ? w : v[1];
            double bt = Double.isNaN(v[2]) ? h : v[2];
            double lf = Double.isNaN(v[3]) ? 0 : v[3];
            left = x + lf;
            top = y + tp;
            rw = Math.max(0, rt - lf);
            rh = Math.max(0, bt - tp);
        }
        return withRadii(t, round, left, top, rw, rh, font);
    }

    /**
     * Makes the shape of <code>path(fill-rule, "SVG path data")</code>: the path is placed at the top-left corner of the reference box.
     * <p>
     * <code>path(채움 규칙, "SVG 경로 데이터")</code> 의 모양을 만든다: 경로는 기준 상자의 왼쪽 위 모서리에 놓인다.
     *
     * @param fn the function / 함수
     * @param x  left of the reference box / 기준 상자 왼쪽
     * @param y  top / 위쪽
     * @return the shape, or null if the path data is not usable / 모양. 경로 데이터를 쓸 수 없으면 null
     */
    private static Shape path(CssFunctionValue fn, double x, double y) {
        List<CssValue> a = fn.getArguments();
        boolean evenOdd = false;
        int i = 0;
        if (!a.isEmpty() && a.get(0) instanceof CssKeyword) {
            evenOdd = a.get(0).isKeyword("evenodd");
            i = 1;
        }
        if (a.size() != i + 1 || !(a.get(i) instanceof CssString)) {
            return null;
        }
        Path2D p = PathShapes.parse(((CssString) a.get(i)).getValue(), evenOdd);
        if (p == null) {
            return null;
        }
        p.transform(java.awt.geom.AffineTransform.getTranslateInstance(x, y));
        return p;
    }

    /**
     * Makes the shape of <code>circle(r at x y)</code> or <code>ellipse(rx ry at x y)</code>.
     * <p>
     * <code>circle(r at x y)</code> 나 <code>ellipse(rx ry at x y)</code> 의 모양을 만든다.
     *
     * @param fn     the function / 함수
     * @param circle true for a circle / 원이면 true
     * @param x      left of the reference box / 기준 상자 왼쪽
     * @param y      top / 위쪽
     * @param w      width / 너비
     * @param h      height / 높이
     * @param font   the font size / 글꼴 크기
     * @return the shape or null / 모양 또는 null
     */
    private static Shape round(CssFunctionValue fn, boolean circle, double x, double y, double w, double h, double font) {
        List<CssValue> t = tokens(fn);
        int at = -1;
        // at 키워드의 위치를 찾는다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).isKeyword("at")) {
                at = i;
            }
        }
        int radiusEnd = at < 0 ? t.size() : at;
        List<CssValue> pos = new ArrayList<CssValue>();
        // at 뒤의 토큰은 위치이다.
        for (int i = at + 1; at >= 0 && i < t.size(); i++) {
            pos.add(t.get(i));
        }
        double[] c = org.duckdns.hjow.swingwebpanel.css.anim.Transforms.origin(pos.isEmpty() ? null : new CssList(pos, false), w, h);
        double cx = x + c[0];
        double cy = y + c[1];
        double[] rad = new double[2];
        int count = circle ? 1 : 2;
        if (radiusEnd > count) {
            return null;
        }
        double refDiag = Math.sqrt((w * w + h * h) / 2.0);
        // 반지름마다(원은 하나, 타원은 둘) 값이나 키워드를 푼다.
        for (int i = 0; i < count; i++) {
            CssValue r = i < radiusEnd ? t.get(i) : null;
            double near = circle ? Math.min(Math.min(c[0], w - c[0]), Math.min(c[1], h - c[1])) : (i == 0 ? Math.min(c[0], w - c[0]) : Math.min(c[1], h - c[1]));
            double far = circle ? Math.max(Math.max(c[0], w - c[0]), Math.max(c[1], h - c[1])) : (i == 0 ? Math.max(c[0], w - c[0]) : Math.max(c[1], h - c[1]));
            if (r == null || r.isKeyword("closest-side")) {
                rad[i] = near;
            } else if (r.isKeyword("farthest-side")) {
                rad[i] = far;
            } else {
                rad[i] = len(r, circle ? refDiag : (i == 0 ? w : h), font);
                if (Double.isNaN(rad[i]) || rad[i] < 0) {
                    return null;
                }
            }
        }
        double rx = rad[0];
        double ry = circle ? rad[0] : rad[1];
        return new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    /**
     * Makes the shape of <code>polygon(fill-rule, x y, x y, ...)</code>.
     * <p>
     * <code>polygon(채움 규칙, x y, x y, ...)</code> 의 모양을 만든다.
     *
     * @param fn   the function / 함수
     * @param x    left of the reference box / 기준 상자 왼쪽
     * @param y    top / 위쪽
     * @param w    width / 너비
     * @param h    height / 높이
     * @param font the font size / 글꼴 크기
     * @return the shape or null / 모양 또는 null
     */
    private static Shape polygon(CssFunctionValue fn, double x, double y, double w, double h, double font) {
        List<CssValue> args = fn.getArguments();
        int start = 0;
        int rule = Path2D.WIND_NON_ZERO;
        if (!args.isEmpty() && args.get(0) instanceof CssKeyword) {
            rule = args.get(0).isKeyword("evenodd") ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO;
            start = 1;
        }
        Path2D.Double p = new Path2D.Double(rule);
        int n = 0;
        // 인수마다 "x y" 쌍을 꼭짓점으로 더한다.
        for (int i = start; i < args.size(); i++) {
            CssValue a = args.get(i);
            if (!(a instanceof CssList) || ((CssList) a).getItems().size() != 2) {
                return null;
            }
            double px = len(((CssList) a).getItems().get(0), w, font);
            double py = len(((CssList) a).getItems().get(1), h, font);
            if (Double.isNaN(px) || Double.isNaN(py)) {
                return null;
            }
            if (n == 0) {
                p.moveTo(x + px, y + py);
            } else {
                p.lineTo(x + px, y + py);
            }
            n++;
        }
        if (n < 1) {
            return null;
        }
        p.closePath();
        return p;
    }
}
