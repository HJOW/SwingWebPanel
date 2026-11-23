package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Small helpers that read layout-relevant values from a {@link ComputedStyle}: keywords, border widths, overflow, text decoration and the
 * classification of <code>display</code> values.
 * <p>
 * {@link ComputedStyle} 에서 레이아웃에 필요한 값을 읽는 작은 도우미: 키워드, border 너비, overflow, 텍스트 장식, <code>display</code> 값의 분류.
 */
final class Css {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Css() {
    }

    /** Writing mode (frame) of horizontal text: <code>horizontal-tb</code>. 가로쓰기 프레임: <code>horizontal-tb</code>. */
    static final int FRAME_H = 0;
    /** Frame of <code>vertical-rl</code> (and <code>sideways-rl</code>): lines run top to bottom and stack from right to left. <code>vertical-rl</code>(과 <code>sideways-rl</code>) 프레임: 줄은 위에서 아래로 가고 오른쪽에서 왼쪽으로 쌓인다. */
    static final int FRAME_VRL = 1;
    /** Frame of <code>vertical-lr</code>: lines run top to bottom and stack from left to right. <code>vertical-lr</code> 프레임: 줄은 위에서 아래로 가고 왼쪽에서 오른쪽으로 쌓인다. */
    static final int FRAME_VLR = 2;
    /** Frame of <code>sideways-lr</code>: lines run bottom to top and stack from left to right. <code>sideways-lr</code> 프레임: 줄은 아래에서 위로 가고 왼쪽에서 오른쪽으로 쌓인다. */
    static final int FRAME_SLR = 3;

    /** Padding properties by physical side (top, right, bottom, left). 물리적 변(위, 오른쪽, 아래, 왼쪽)별 padding 속성. */
    static final int[] PADDING = { Prop.PADDING_TOP, Prop.PADDING_RIGHT, Prop.PADDING_BOTTOM, Prop.PADDING_LEFT };
    /** Margin properties by physical side. 물리적 변별 margin 속성. */
    static final int[] MARGIN = { Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT };
    /** Border width properties by physical side. 물리적 변별 border 너비 속성. */
    static final int[] BORDER_WIDTH = { Prop.BORDER_TOP_WIDTH, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_LEFT_WIDTH };
    /** Border style properties by physical side. 물리적 변별 border 스타일 속성. */
    static final int[] BORDER_STYLE = { Prop.BORDER_TOP_STYLE, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_LEFT_STYLE };
    /** Offset properties (<code>top</code>, <code>right</code>, <code>bottom</code>, <code>left</code>) by physical side. 물리적 변별 오프셋 속성. */
    static final int[] OFFSET = { Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT };

    /**
     * Returns the frame of a style: the writing mode as one of the <code>FRAME_*</code> constants.
     * <p>
     * 스타일의 프레임을 반환한다: 쓰기 모드를 <code>FRAME_*</code> 상수로 나타낸 값.
     *
     * @param s the style / 스타일
     * @return the frame / 프레임
     */
    static int frameOf(ComputedStyle s) {
        if (s == null) {
            return FRAME_H;
        }
        String w = kw(s, Prop.WRITING_MODE);
        if ("vertical-rl".equals(w) || "sideways-rl".equals(w)) {
            return FRAME_VRL;
        }
        if ("vertical-lr".equals(w)) {
            return FRAME_VLR;
        }
        if ("sideways-lr".equals(w)) {
            return FRAME_SLR;
        }
        return FRAME_H;
    }

    /**
     * Returns the physical side (0 top, 1 right, 2 bottom, 3 left) that a logical side of a frame is: the logical sides are those of the
     * horizontal layout (0 block-start, 1 inline-end, 2 block-end, 3 inline-start) that the layout code works with.
     * <p>
     * 프레임의 논리적 변이 가리키는 물리적 변(0 위, 1 오른쪽, 2 아래, 3 왼쪽)을 반환한다. 논리적 변은 레이아웃 코드가 쓰는 가로 레이아웃의 변(0 블록 시작, 1 인라인 끝,
     * 2 블록 끝, 3 인라인 시작)이다.
     *
     * @param frame   the frame / 프레임
     * @param logical the logical side / 논리적 변
     * @return the physical side / 물리적 변
     */
    static int physSide(int frame, int logical) {
        if (frame == FRAME_VRL) {
            return (logical + 1) & 3;
        }
        if (frame == FRAME_VLR) {
            return 3 - logical;
        }
        if (frame == FRAME_SLR) {
            return (logical == 0 ? 3 : (logical == 1 ? 0 : (logical == 2 ? 1 : 2)));
        }
        return logical;
    }

    /**
     * Returns the property of a family (padding, margin, border width, border style, offset) for a logical side of a frame.
     * <p>
     * 프레임의 논리적 변에 해당하는 속성 계열(padding, margin, border 너비, border 스타일, 오프셋)의 속성을 반환한다.
     *
     * @param family the properties by physical side / 물리적 변별 속성
     * @param frame  the frame / 프레임
     * @param logical the logical side / 논리적 변
     * @return the property id / 속성 id
     */
    static int side(int[] family, int frame, int logical) {
        return family[physSide(frame, logical)];
    }

    /**
     * Returns the property that gives the logical inline size (<code>width</code> in horizontal frames, <code>height</code> in vertical
     * ones): <code>shift</code> 0 for the preferred size, 1 for the minimum and 2 for the maximum.
     * <p>
     * 논리적 인라인 크기를 주는 속성을 반환한다(가로 프레임은 <code>width</code>, 세로 프레임은 <code>height</code>). <code>shift</code> 가 0 이면 지정 크기, 1 이면 최소, 2 이면 최대이다.
     *
     * @param frame the frame / 프레임
     * @param kind  0 size, 1 minimum, 2 maximum / 0 크기, 1 최소, 2 최대
     * @return the property id / 속성 id
     */
    static int inlineSizeProp(int frame, int kind) {
        int[] w = { Prop.WIDTH, Prop.MIN_WIDTH, Prop.MAX_WIDTH };
        int[] h = { Prop.HEIGHT, Prop.MIN_HEIGHT, Prop.MAX_HEIGHT };
        return frame == FRAME_H ? w[kind] : h[kind];
    }

    /**
     * Returns the property that gives the logical block size (<code>height</code> in horizontal frames, <code>width</code> in vertical
     * ones).
     * <p>
     * 논리적 블록 크기를 주는 속성을 반환한다(가로 프레임은 <code>height</code>, 세로 프레임은 <code>width</code>).
     *
     * @param frame the frame / 프레임
     * @param kind  0 size, 1 minimum, 2 maximum / 0 크기, 1 최소, 2 최대
     * @return the property id / 속성 id
     */
    static int blockSizeProp(int frame, int kind) {
        return inlineSizeProp(frame == FRAME_H ? FRAME_VRL : FRAME_H, kind);
    }

    /**
     * Returns the keyword value of a property, or "" if the value is not a keyword.
     * <p>
     * 속성의 키워드 값을 반환한다. 키워드가 아니면 "".
     *
     * @param s the style / 스타일
     * @param p property id / 속성 id
     * @return keyword or "" / 키워드 또는 ""
     */
    static String kw(ComputedStyle s, int p) {
        String k = s.keyword(p);
        return k == null ? "" : k;
    }

    /**
     * Returns a border width in pixels: zero if the border style is <code>none</code> or <code>hidden</code>.
     * <p>
     * border 너비(픽셀)를 반환한다. border 스타일이 <code>none</code> 또는 <code>hidden</code> 이면 0.
     *
     * @param s     the style / 스타일
     * @param width width property id / 너비 속성 id
     * @param style style property id / 스타일 속성 id
     * @return width / 너비
     */
    static double border(ComputedStyle s, int width, int style) {
        String st = kw(s, style);
        if ("none".equals(st) || "hidden".equals(st) || st.length() == 0) {
            return 0;
        }
        double w = s.px(width);
        return Double.isNaN(w) || w < 0 ? 0 : w;
    }

    /**
     * Tells whether the box clips its content: <code>overflow</code> other than <code>visible</code>.
     * <p>
     * 상자가 콘텐츠를 자르는지 알려 준다: <code>overflow</code> 가 <code>visible</code> 이 아닌 경우.
     *
     * @param s the style / 스타일
     * @return true if clipped / 자르면 true
     */
    static boolean clips(ComputedStyle s) {
        String x = kw(s, Prop.OVERFLOW_X);
        String y = kw(s, Prop.OVERFLOW_Y);
        return !("visible".equals(x) || x.length() == 0) || !("visible".equals(y) || y.length() == 0) || Containment.paint(s);
    }

    /**
     * Returns the text decoration bits of a style (own declaration only).
     * <p>
     * 스타일의 텍스트 장식 비트를 반환한다(자신의 선언만).
     *
     * @param s the style / 스타일
     * @return bits of the <code>LayoutBox.DECORATION_*</code> constants / <code>LayoutBox.DECORATION_*</code> 상수의 비트
     */
    static int decoration(ComputedStyle s) {
        CssValue v = s.get(Prop.TEXT_DECORATION_LINE);
        String t = v == null ? "" : v.toCssText();
        int d = 0;
        if (t.indexOf("underline") >= 0) {
            d |= LayoutBox.DECORATION_UNDERLINE;
        }
        if (t.indexOf("overline") >= 0) {
            d |= LayoutBox.DECORATION_OVERLINE;
        }
        if (t.indexOf("line-through") >= 0) {
            d |= LayoutBox.DECORATION_LINE_THROUGH;
        }
        return d;
    }

    /**
     * Tells whether a <code>display</code> value makes an atomic inline-level box (<code>inline-block</code> and its relatives).
     * <p>
     * <code>display</code> 값이 원자적 인라인 수준 상자(<code>inline-block</code> 과 그 친척)를 만드는지 알려 준다.
     *
     * @param d the display value / display 값
     * @return true for atomic inlines / 원자적 인라인이면 true
     */
    static boolean isAtomicInline(String d) {
        return "inline-block".equals(d) || "inline-table".equals(d) || "inline-flex".equals(d) || "inline-grid".equals(d);
    }

    /**
     * Tells whether a <code>display</code> value makes an ordinary inline box.
     * <p>
     * <code>display</code> 값이 보통의 인라인 상자를 만드는지 알려 준다.
     *
     * @param d the display value / display 값
     * @return true for inline / 인라인이면 true
     */
    static boolean isInline(String d) {
        return "inline".equals(d) || d.startsWith("ruby") || "run-in".equals(d) || d.length() == 0;
    }

    /**
     * Returns the effective <code>white-space</code> keyword of a style: with <code>text-wrap-mode: nowrap</code> (CSS Text Level 4, where
     * <code>white-space</code> is a shorthand of <code>white-space-collapse</code> and <code>text-wrap-mode</code>) <code>normal</code> becomes
     * <code>nowrap</code> and <code>pre-wrap</code> and <code>break-spaces</code> become <code>pre</code>; the other values keep their meaning
     * (there is no keyword for <code>pre-line</code> that does not wrap).
     * <p>
     * 스타일의 실효 <code>white-space</code> 키워드를 반환한다: <code>text-wrap-mode: nowrap</code> 이면(CSS Text Level 4 에서 <code>white-space</code> 는
     * <code>white-space-collapse</code> 와 <code>text-wrap-mode</code> 의 단축 속성이다) <code>normal</code> 은 <code>nowrap</code> 이 되고 <code>pre-wrap</code>,
     * <code>break-spaces</code> 는 <code>pre</code> 가 되며, 다른 값은 그대로이다(줄바꿈 없는 <code>pre-line</code> 에 해당하는 키워드는 없다).
     *
     * @param s the style / 스타일
     * @return the keyword / 키워드
     */
    static String whiteSpace(ComputedStyle s) {
        String w = kw(s, Prop.WHITE_SPACE);
        if ("nowrap".equals(kw(s, Prop.TEXT_WRAP_MODE))) {
            if ("normal".equals(w) || w.length() == 0) {
                return "nowrap";
            }
            if ("pre-wrap".equals(w) || "break-spaces".equals(w)) {
                return "pre";
            }
        }
        return w;
    }

    /**
     * Tells whether the white space of the style collapses (<code>normal</code>, <code>nowrap</code>, <code>pre-line</code>).
     * <p>
     * 스타일의 공백이 접히는지(<code>normal</code>, <code>nowrap</code>, <code>pre-line</code>) 알려 준다.
     *
     * @param s the style / 스타일
     * @return true if collapsible / 접히면 true
     */
    static boolean collapsesSpaces(ComputedStyle s) {
        String w = whiteSpace(s);
        return !("pre".equals(w) || "pre-wrap".equals(w) || "break-spaces".equals(w));
    }

    /**
     * Tells whether a style has a transform (<code>transform</code> other than <code>none</code>): such a box is a stacking context and
     * the containing block of its absolutely positioned and fixed descendants (CSS Transforms 1 section 2).
     * <p>
     * 스타일에 변환(<code>none</code> 이 아닌 <code>transform</code>)이 있는지 알려 준다: 그런 상자는 쌓임 맥락이며 자신의 절대 배치와 fixed 자손의 포함 블록이다
     * (CSS Transforms 1 2 절).
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box is transformed / 변환되었으면 true
     */
    static boolean hasTransform(ComputedStyle s) {
        return s != null && (!org.duckdns.hjow.swingwebpanel.css.anim.Transforms.isNone(s.get(Prop.TRANSFORM)) || !org.duckdns.hjow.swingwebpanel.css.anim.Transforms.isNone(s.get(Prop.TRANSLATE))
                || !org.duckdns.hjow.swingwebpanel.css.anim.Transforms.isNone(s.get(Prop.ROTATE)) || !org.duckdns.hjow.swingwebpanel.css.anim.Transforms.isNone(s.get(Prop.SCALE))
                || MotionPath.has(s));
    }
}
