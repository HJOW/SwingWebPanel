package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The basic form of CSS Anchor Positioning Level 1 (<a href="https://www.w3.org/TR/css-anchor-position-1/">https://www.w3.org/TR/css-anchor-position-1/</a>, W3C Working Draft,
 * Last-Modified 2026-03-27): an element that declares <code>anchor-name</code> is an anchor, and an absolutely positioned box that names it (the default anchor of
 * <code>position-anchor</code>, or the name inside the function) takes the position of an edge of the anchor box as the value of an inset (<code>anchor()</code> in
 * <code>top</code>, <code>right</code>, <code>bottom</code>, <code>left</code>) or the size of the anchor box as a length (<code>anchor-size()</code>). Before the box is
 * laid out its style is replaced by one in which these functions are lengths ({@link #resolve}); everything else of the positioning algorithm stays as it is.
 * <p>
 * Which anchor: of the elements with the name that are descendants of the containing block of the positioned box (every box when the containing block is the initial one) and
 * that are laid out before it, the nearest ancestor of the positioned box, else the last one in tree order (2.3). An unresolvable function (no anchor, a side of the other
 * axis, <code>start</code> or <code>end</code> in a writing mode other than left to right horizontal) computes to its fallback value, or if it has none to <code>auto</code>.
 * <p>
 * <code>anchor()</code> and <code>anchor-size()</code> may also be arguments inside <code>calc()</code>, <code>min()</code>, <code>max()</code> and <code>clamp()</code>
 * ({@link CssCalc#withAnchors}): every function becomes its length (or its fallback) and a function that cannot be resolved without a fallback makes the property <code>auto</code>.
 * The default anchor is the element that <code>position-anchor</code> names, the implicit anchor element for <code>auto</code> (and for <code>normal</code> with a
 * <code>position-area</code>), the anchor of the parent for <code>match-parent</code> (2.4); the implicit anchor of a pseudo-element is its originating element (2.4.1).
 * <p>
 * The overflow management of section 6 (<code>position-try-*</code>, <code>position-visibility</code>, see {@link PositionTry} and {@link AbsoluteLayouter}), <code>anchor-scope</code> (2.2) and <code>anchor-center</code> (4.2) are implemented here and there.
 * Not implemented:
 * implicit anchors other than the originating element of a pseudo-element (popover invokers), anchors that are inline boxes or not descendants of the containing block,
 * and the adjustment of an anchor by <code>position: relative</code> offsets or scrolling.
 * <p>
 * CSS Anchor Positioning Level 1 의 기본 형태(W3C 작업 초안): <code>anchor-name</code> 을 선언한 요소가 앵커이고, 그 이름을 쓰는 절대 배치 상자(<code>position-anchor</code> 의 기본
 * 앵커나 함수 안의 이름)는 앵커 상자의 한 가장자리 위치를 오프셋 값(<code>top</code>, <code>right</code>, <code>bottom</code>, <code>left</code> 안의 <code>anchor()</code>)으로,
 * 앵커 상자의 크기를 길이(<code>anchor-size()</code>)로 가진다. 상자를 배치하기 전에 이 함수들이 길이인 스타일로 바꾸고({@link #resolve}) 배치 알고리즘의 나머지는 그대로이다.
 * <p>
 * 앵커 선택: 이름이 같은 요소 중 배치 상자의 포함 블록의 자손이고(포함 블록이 초기 블록이면 모든 상자) 그보다 먼저 배치된 것에서 배치 상자의 가장 가까운 조상, 없으면 트리 순서의
 * 마지막 것(2.3). 풀 수 없는 함수(앵커 없음, 다른 축의 변, 가로 왼쪽→오른쪽이 아닌 쓰기 모드의 <code>start</code>/<code>end</code>)는 대체값으로 계산되고 대체값이 없으면
 * <code>auto</code> 이다.
 * <p>
 * <code>anchor()</code> 와 <code>anchor-size()</code> 는 <code>calc()</code>, <code>min()</code>, <code>max()</code>, <code>clamp()</code> 의 인수로도 쓸 수 있다
 * ({@link CssCalc#withAnchors}): 함수마다 그 길이(또는 대체값)가 되고, 대체값 없이 풀 수 없는 함수가 있으면 속성은 <code>auto</code> 가 된다. 기본 앵커는
 * <code>position-anchor</code> 가 가리키는 요소, <code>auto</code>(와 <code>position-area</code> 가 있는 <code>normal</code>)이면 암묵 앵커 요소, <code>match-parent</code> 이면 부모의
 * 앵커이다(2.4). 의사 요소의 암묵 앵커는 그 원래 요소이다(2.4.1).
 * <p>
 * 6 절의 넘침 관리(<code>position-try-*</code>, <code>position-visibility</code>, {@link PositionTry}, {@link AbsoluteLayouter} 참조), <code>anchor-scope</code>(2.2), <code>anchor-center</code>(4.2)는 여기와 그 클래스들에 구현되어 있다.
 * 구현하지 않음:
 * 의사 요소의 원래 요소 외의 암묵 앵커(popover 호출자), 인라인 상자이거나 포함 블록의 자손이 아닌 앵커, <code>position: relative</code> 오프셋이나 스크롤에 따른 앵커 보정.
 */
final class AnchorPositioning {

    /** The inset properties in the order top, right, bottom, left. 오프셋 속성(위, 오른쪽, 아래, 왼쪽 순서). */
    private static final int[] INSETS = {Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT};

    /** The size properties in which <code>anchor-size()</code> is resolved. <code>anchor-size()</code> 를 푸는 크기 속성. */
    private static final int[] SIZES = {Prop.WIDTH, Prop.HEIGHT, Prop.MIN_WIDTH, Prop.MIN_HEIGHT, Prop.MAX_WIDTH, Prop.MAX_HEIGHT};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private AnchorPositioning() {
    }

    /**
     * Tells whether a style has an <code>anchor()</code> or <code>anchor-size()</code> in one of the properties that {@link #resolve} handles.
     * <p>
     * 스타일에 {@link #resolve} 가 다루는 속성 중 <code>anchor()</code> 나 <code>anchor-size()</code> 를 쓴 것이 있는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if there is such a function / 그런 함수가 있으면 true
     */
    static boolean uses(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        // 오프셋과 크기 속성에서 anchor 함수를 찾는다.
        for (int i = 0; i < INSETS.length; i++) {
            if (isAnchorFunction(s.get(INSETS[i])) || isAnchorCalc(s.get(INSETS[i]))) {
                return true;
            }
        }
        // 크기 속성도 같은 방식으로 찾는다.
        for (int i = 0; i < SIZES.length; i++) {
            if (isAnchorFunction(s.get(SIZES[i])) || isAnchorCalc(s.get(SIZES[i]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a value is an <code>anchor()</code> or <code>anchor-size()</code> function.
     * <p>
     * 값이 <code>anchor()</code> 나 <code>anchor-size()</code> 함수인지 알려 준다.
     *
     * @param v the value, may be null / 값. null 가능
     * @return true for such a function / 그런 함수이면 true
     */
    private static boolean isAnchorFunction(CssValue v) {
        return v instanceof CssFunctionValue && (((CssFunctionValue) v).isNamed("anchor") || ((CssFunctionValue) v).isNamed("anchor-size"));
    }

    /**
     * Tells whether a value is a <code>calc()</code> (or another math function) that contains an <code>anchor()</code> or <code>anchor-size()</code> function.
     * <p>
     * 값이 <code>anchor()</code> 나 <code>anchor-size()</code> 함수를 포함한 <code>calc()</code>(또는 다른 수학 함수)인지 알려 준다.
     *
     * @param v the value, may be null / 값. null 가능
     * @return true for such an expression / 그런 식이면 true
     */
    private static boolean isAnchorCalc(CssValue v) {
        return v instanceof CssCalc && ((CssCalc) v).hasAnchor();
    }

    /**
     * Replaces the anchor functions in the style of a positioned box by the lengths that they resolve to.
     * <p>
     * 배치할 상자의 스타일에서 anchor 함수를 그것이 푸는 길이로 바꾼다.
     *
     * @param a    the absolutely positioned box / 절대 배치 상자
     * @param cb   the box that is the containing block, or null for the initial containing block / 포함 블록인 상자. 초기 포함 블록이면 null
     * @param list the positioned boxes of this containing block in source order / 이 포함 블록의 배치 상자(소스 순서)
     * @param idx  the index of the box in the list (the boxes from there on are not laid out yet) / 목록에서 상자의 번호(거기서부터는 아직 배치되지 않았다)
     * @param bx   left of the padding box of the containing block, in the coordinates of its border box / 포함 블록 padding box 의 왼쪽(border box 좌표)
     * @param by   top of the padding box / padding box 의 위쪽
     * @param bw   width of the padding box / padding box 의 너비
     * @param bh   height of the padding box / padding box 의 높이
     * @return the style with lengths instead of the functions / 함수 대신 길이가 든 스타일
     */
    static ComputedStyle resolve(LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx, double bx, double by, double bw, double bh) {
        ComputedStyle s = a.style;
        Lookup lookup = new Lookup(a, cb, list, idx);
        double[] pad = {bx, by, bw, bh};
        // 오프셋 속성의 anchor() 와 anchor-size() 를 길이로 바꾼다.
        for (int i = 0; i < INSETS.length; i++) {
            CssValue v = s.get(INSETS[i]);
            if (isAnchorFunction(v)) {
                s = s.with(INSETS[i], evaluate((CssFunctionValue) v, INSETS[i], a, lookup, pad));
            } else if (isAnchorCalc(v)) {
                s = s.with(INSETS[i], evaluateCalc((CssCalc) v, INSETS[i], a, lookup, pad));
            }
        }
        // 크기 속성의 anchor-size() 를 길이로 바꾼다.
        for (int i = 0; i < SIZES.length; i++) {
            CssValue v = s.get(SIZES[i]);
            if (isAnchorFunction(v)) {
                s = s.with(SIZES[i], evaluate((CssFunctionValue) v, SIZES[i], a, lookup, pad));
            } else if (isAnchorCalc(v)) {
                s = s.with(SIZES[i], evaluateCalc((CssCalc) v, SIZES[i], a, lookup, pad));
            }
        }
        return s;
    }

    /**
     * Evaluates an expression that has anchor functions in it (CSS Anchor Positioning 1 sections 3.2 and 5.1: the functions can be used in <code>calc()</code>
     * and the other math functions): every function is replaced by its length, and the property is <code>auto</code> (<code>none</code> for a maximum size) if one
     * of them cannot be resolved and has no fallback.
     * <p>
     * anchor 함수가 든 식을 계산한다(CSS Anchor Positioning 1 3.2, 5.1 절: 함수는 <code>calc()</code> 와 다른 수학 함수 안에서 쓸 수 있다): 함수마다 길이로 바꾸며, 하나라도
     * 풀 수 없고 대체값도 없으면 속성은 <code>auto</code>(최대 크기는 <code>none</code>)이다.
     *
     * @param calc   the expression / 식
     * @param prop   the property that it is in / 식이 든 속성
     * @param a      the positioned box / 배치 상자
     * @param lookup the anchor lookup / 앵커 조회
     * @param pad    the padding box of the containing block / 포함 블록의 padding box
     * @return the value to use / 쓸 값
     */
    private static CssValue evaluateCalc(CssCalc calc, final int prop, final LayoutBox a, final Lookup lookup, final double[] pad) {
        CssCalc replaced = calc.withAnchors(new CssCalc.AnchorResolver() {
            /** Resolves one function of the expression. 식 안의 함수 하나를 푼다. */
            public CssValue resolve(CssFunctionValue fn) {
                return valueOf(fn, prop, a, lookup, pad);
            }
        });
        return replaced != null ? replaced : CssKeyword.of(prop == Prop.MAX_WIDTH || prop == Prop.MAX_HEIGHT ? "none" : "auto");
    }

    /**
     * Evaluates one function: its length in pixels, its fallback value, or <code>auto</code> (or <code>none</code> for a maximum size) if it cannot be resolved.
     * <p>
     * 함수 하나를 계산한다: 픽셀 길이, 풀 수 없으면 대체값, 대체값이 없으면 <code>auto</code>(최대 크기이면 <code>none</code>).
     *
     * @param fn     the function / 함수
     * @param prop   the property that it is in / 함수가 든 속성
     * @param a      the positioned box / 배치 상자
     * @param lookup the anchor lookup / 앵커 조회
     * @param pad    the padding box of the containing block {x, y, width, height} / 포함 블록의 padding box
     * @return the value to use / 쓸 값
     */
    private static CssValue evaluate(CssFunctionValue fn, int prop, LayoutBox a, Lookup lookup, double[] pad) {
        CssValue v = valueOf(fn, prop, a, lookup, pad);
        return v != null ? v : CssKeyword.of(prop == Prop.MAX_WIDTH || prop == Prop.MAX_HEIGHT ? "none" : "auto");
    }

    /**
     * Resolves one function: its length in pixels, its fallback value (a length or a percentage), or null if it cannot be resolved and has no usable fallback.
     * <p>
     * 함수 하나를 푼다: 픽셀 길이, 대체값(길이나 백분율). 풀 수 없고 쓸 수 있는 대체값도 없으면 null.
     *
     * @param fn     the function / 함수
     * @param prop   the property that it is in / 함수가 든 속성
     * @param a      the positioned box / 배치 상자
     * @param lookup the anchor lookup / 앵커 조회
     * @param pad    the padding box of the containing block {x, y, width, height} / 포함 블록의 padding box
     * @return the value or null / 값. 없으면 null
     */
    private static CssValue valueOf(CssFunctionValue fn, int prop, LayoutBox a, Lookup lookup, double[] pad) {
        List<CssValue> args = fn.getArguments();
        List<CssValue> head = args.isEmpty() ? Collections.<CssValue>emptyList() : (args.get(0) instanceof CssList ? ((CssList) args.get(0)).getItems() : Collections.singletonList(args.get(0)));
        String name = null;
        String word = null;
        double pct = Double.NaN;
        // 첫 인수의 앵커 이름(--로 시작)과 변, 크기 키워드, 백분율을 모은다.
        for (int i = 0; i < head.size(); i++) {
            CssValue h = head.get(i);
            if (h instanceof CssKeyword) {
                String k = ((CssKeyword) h).getName();
                if (k.startsWith("--")) {
                    name = k;
                } else {
                    word = k.toLowerCase(java.util.Locale.ENGLISH);
                }
            } else if (h instanceof CssPercentage) {
                pct = ((CssPercentage) h).getValue();
            }
        }
        CssValue fallback = args.size() > 1 ? args.get(1) : null;
        boolean size = fn.isNamed("anchor-size");
        double length = Double.NaN;
        boolean isInset = prop == Prop.TOP || prop == Prop.RIGHT || prop == Prop.BOTTOM || prop == Prop.LEFT;
        // anchor() 는 오프셋 속성에서만 풀린다. 이름이 없으면 position-anchor 의 기본 앵커를 쓴다.
        if (size || isInset) {
            // 이름이 없으면 기본 앵커(position-anchor 의 이름, 암묵 앵커, match-parent)를 쓴다.
            double[] r = name != null ? lookup.rect(name) : lookup.defaultRect();
            if (r != null) {
                length = size ? sizeOf(word, r, a.style) : edge(prop, word, pct, r, a.style, pad);
            }
        }
        if (!Double.isNaN(length)) {
            return new CssLength(length, "px");
        }
        // 풀 수 없으면 대체값(길이나 백분율)을 쓴다. 없으면 null 이다.
        if (fallback instanceof CssLength || fallback instanceof CssPercentage) {
            return fallback;
        }
        return null;
    }

    /**
     * Tells whether a style has a <code>position-area</code> other than <code>none</code>.
     * <p>
     * 스타일에 <code>none</code> 이 아닌 <code>position-area</code> 가 있는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if there is a position area / 위치 영역이 있으면 true
     */
    static boolean hasArea(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.POSITION_AREA);
        return v != null && !v.isKeyword("none");
    }

    /**
     * The region of the position-area grid that a box is laid out in (CSS Anchor Positioning Level 1 section 3.1 and 4.1,
     * https://www.w3.org/TR/css-anchor-position-1/#position-area): the 3 by 3 grid is made by the edges of the anchor box and of the containing
     * block (a row or column is as large as the distance between two lines, and zero if the anchor is outside), the keywords select a rectangle of
     * tracks, the box is laid out inside that rectangle and its <code>normal</code> alignment becomes the alignment that puts it toward the anchor: end
     * alignment if the region contains the start track only, start if the end track, centered if only the center track, centered on the anchor if all
     * three tracks. A box that is larger than the region is shifted back into the containing block if it fits there.
     * <p>
     * 위치 지정 상자를 배치할 위치 영역 격자(position-area grid)의 영역(CSS Anchor Positioning Level 1 3.1, 4.1 절): 3x3 격자는 앵커 상자와 포함 블록의 가장자리로
     * 만들고(행과 열의 크기는 두 선 사이의 거리이며 앵커가 밖에 있으면 0), 키워드가 트랙의 사각형을 고르며, 상자는 그 사각형 안에 배치되고 <code>normal</code> 정렬은 앵커 쪽으로
     * 붙이는 정렬이 된다: 영역이 시작 트랙만 포함하면 끝 정렬, 끝 트랙만 포함하면 시작 정렬, 가운데 트랙만이면 가운데, 세 트랙이면 앵커 가운데 정렬. 영역보다 큰 상자는 포함 블록 안에
     * 들어갈 수 있으면 포함 블록 안으로 되돌려 옮긴다.
     */
    static final class Area {
        /** Alignment toward the start of the region. 영역 시작 쪽 정렬. */
        static final int START = 0;
        /** Alignment toward the end of the region. 영역 끝 쪽 정렬. */
        static final int END = 1;
        /** Centered in the region. 영역 가운데 정렬. */
        static final int CENTER = 2;
        /** Centered on the anchor box. 앵커 상자 가운데 정렬. */
        static final int ANCHOR_CENTER = 3;

        /** The region {left, right, top, bottom} in the coordinates of the border box of the containing block. 포함 블록의 border box 좌표로 나타낸 영역. */
        final double[] region;
        /** The anchor box {x, y, w, h}. 앵커 상자. */
        final double[] anchor;
        /** The alignment in the horizontal and in the vertical axis. 가로와 세로 축의 정렬. */
        final int[] align;
        /** The insets of the author {top, right, bottom, left} that count from the region (0 for auto). 영역에서부터 세는 작성자의 오프셋(auto 는 0). */
        final double[] insets;
        /** Whether the region applies to the horizontal and to the vertical axis (a box that only has <code>anchor-center</code> in one axis is laid out normally in the other). 영역이 가로와 세로 축에 적용되는지 여부(한 축에만 <code>anchor-center</code> 가 있는 상자는 다른 축은 평소대로 배치한다). */
        final boolean[] active;

        /**
         * Creates the region.
         * <p>
         * 영역을 만든다.
         *
         * @param region the region / 영역
         * @param anchor the anchor box / 앵커 상자
         * @param align  the alignments / 정렬
         * @param insets the insets of the author / 작성자의 오프셋
         */
        Area(double[] region, double[] anchor, int[] align, double[] insets) {
            this(region, anchor, align, insets, new boolean[] {true, true});
        }

        /**
         * Creates the region for the axes that are marked.
         * <p>
         * 표시한 축에 적용되는 영역을 만든다.
         *
         * @param region the region / 영역
         * @param anchor the anchor box / 앵커 상자
         * @param align  the alignments / 정렬
         * @param insets the insets of the author / 작성자의 오프셋
         * @param active the axes (horizontal, vertical) that the region applies to / 영역이 적용되는 축(가로, 세로)
         */
        Area(double[] region, double[] anchor, int[] align, double[] insets, boolean[] active) {
            this.region = region;
            this.anchor = anchor;
            this.align = align;
            this.insets = insets;
            this.active = active;
        }

        /**
         * Returns the style with the insets that put the start of the box at the start of the region and that limit its size to the region: the
         * box shrinks to fit, as <code>normal</code> alignment does not stretch it, and its position is corrected after the layout.
         * <p>
         * 상자의 시작을 영역의 시작에 두고 크기를 영역으로 제한하는 오프셋이 든 스타일을 반환한다: <code>normal</code> 정렬은 늘이지 않으므로 상자는 shrink-to-fit 이고
         * 위치는 배치 뒤에 바로잡는다.
         *
         * @param s   the style of the box / 상자의 스타일
         * @param pad the padding box {x, y, w, h} of the containing block / 포함 블록의 padding box
         * @return the new style / 새 스타일
         */
        ComputedStyle style(ComputedStyle s, double[] pad) {
            ComputedStyle r = s;
            // 영역이 적용되는 축의 오프셋을 영역의 시작으로 바꾸고 그 축의 auto 마진을 0 으로 푼다(CSS Anchor Positioning 3.1). 지정한 마진은 그대로 둔다.
            if (active[0]) {
                r = r.with(Prop.LEFT, new CssLength(region[0] - pad[0] + insets[3], "px"));
                r = r.with(Prop.RIGHT, CssKeyword.of("auto"));
                int[] margins = { Prop.MARGIN_LEFT, Prop.MARGIN_RIGHT };
                // 가로 auto 마진을 0 으로 푼다.
                for (int i = 0; i < margins.length; i++) {
                    if (r.isAuto(margins[i])) {
                        r = r.with(margins[i], new CssLength(0, "px"));
                    }
                }
                // 너비가 auto 이면 영역 너비로 제한해 shrink-to-fit 이 영역을 넘지 않게 한다.
                if (r.isAuto(Prop.WIDTH)) {
                    r = r.with(Prop.MAX_WIDTH, new CssLength(Math.max(0, region[1] - region[0] - insets[3] - insets[1]), "px"));
                }
            }
            if (active[1]) {
                r = r.with(Prop.TOP, new CssLength(region[2] - pad[1] + insets[0], "px"));
                r = r.with(Prop.BOTTOM, CssKeyword.of("auto"));
                int[] margins = { Prop.MARGIN_TOP, Prop.MARGIN_BOTTOM };
                // 세로 auto 마진을 0 으로 푼다.
                for (int i = 0; i < margins.length; i++) {
                    if (r.isAuto(margins[i])) {
                        r = r.with(margins[i], new CssLength(0, "px"));
                    }
                }
            }
            return r;
        }

        /**
         * Corrects the position of the laid out box for the alignment: the box is moved to the end or the center of the region, or centered on the
         * anchor, and shifted back into the padding box of the containing block if it fits there.
         * <p>
         * 배치한 상자의 위치를 정렬에 맞게 바로잡는다: 상자를 영역의 끝이나 가운데로, 또는 앵커의 가운데로 옮기고, 포함 블록의 padding box 안에 들어갈 수 있으면 그 안으로 되돌린다.
         *
         * @param a   the laid out box (its size is final) / 배치한 상자(크기는 확정)
         * @param pos the position {x, y} relative to the padding box, changed in place / padding box 기준 위치 {x, y}. 제자리에서 바꾼다
         * @param pad the padding box {x, y, w, h} of the containing block / 포함 블록의 padding box
         */
        void place(LayoutBox a, double[] pos, double[] pad) {
            double w = a.width + a.marginLeft + a.marginRight;
            double h = a.height + a.marginTop + a.marginBottom;
            // 영역이 적용되는 축의 위치만 바로잡는다.
            if (active[0]) {
                double left = edge(align[0], region[0] + insets[3], region[1] - insets[1], w, anchor[0] + anchor[2] / 2, pad[0], pad[0] + pad[2]);
                pos[0] = left - pad[0] + a.marginLeft;
            }
            if (active[1]) {
                double top = edge(align[1], region[2] + insets[0], region[3] - insets[2], h, anchor[1] + anchor[3] / 2, pad[1], pad[1] + pad[3]);
                pos[1] = top - pad[1] + a.marginTop;
            }
        }

        /**
         * Returns the start edge of a box of an outer size along one axis.
         * <p>
         * 한 축에서 바깥 크기를 가진 상자의 시작 가장자리를 반환한다.
         *
         * @param kind  the alignment / 정렬
         * @param s     start of the region (with the inset) / 영역의 시작(오프셋 포함)
         * @param e     end of the region / 영역의 끝
         * @param size  the outer size of the box / 상자의 바깥 크기
         * @param ac    the center of the anchor / 앵커의 가운데
         * @param padS  start of the padding box / padding box 의 시작
         * @param padE  end of the padding box / padding box 의 끝
         * @return the start edge / 시작 가장자리
         */
        private static double edge(int kind, double s, double e, double size, double ac, double padS, double padE) {
            double start;
            if (kind == END) {
                start = e - size;
            } else if (kind == CENTER) {
                start = s + ((e - s) - size) / 2;
            } else if (kind == ANCHOR_CENTER) {
                start = ac - size / 2;
            } else {
                start = s;
            }
            // 영역을 넘쳤지만 포함 블록 안에는 들어가면 포함 블록 안으로 되돌린다(CSS Anchor Positioning 4.1).
            if (size <= padE - padS && (start < padS || start + size > padE) && (size > e - s || kind == ANCHOR_CENTER)) {
                start = Math.max(padS, Math.min(start, padE - size));
            }
            return start;
        }
    }

    /**
     * Builds the position area of a box (the region and the alignments) or returns null if it has none or no default anchor box is found.
     * <p>
     * 상자의 위치 영역(영역과 정렬)을 만든다. 영역이 없거나 기본 앵커 상자를 찾지 못하면 null.
     *
     * @param a    the absolutely positioned box / 절대 배치 상자
     * @param cb   the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list the positioned boxes of the containing block / 포함 블록의 배치 상자
     * @param idx  the index of the box in the list / 목록에서 상자의 번호
     * @param bx   left of the padding box of the containing block / 포함 블록 padding box 의 왼쪽
     * @param by   top of the padding box / 위쪽
     * @param bw   width of the padding box / 너비
     * @param bh   height of the padding box / 높이
     * @return the area or null / 영역. 없으면 null
     */
    static Area area(LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx, double bx, double by, double bw, double bh) {
        ComputedStyle s = a.style;
        double[] r = hasArea(s) ? new Lookup(a, cb, list, idx).defaultRect() : null;
        int[] tracks = r == null ? null : tracksOf(s.get(Prop.POSITION_AREA));
        if (tracks == null) {
            return null;
        }
        double[] xs = { Math.min(bx, r[0]), r[0], r[0] + r[2], Math.max(bx + bw, r[0] + r[2]) };
        double[] ys = { Math.min(by, r[1]), r[1], r[1] + r[3], Math.max(by + bh, r[1] + r[3]) };
        double[] region = { xs[tracks[0]], xs[tracks[1] + 1], ys[tracks[2]], ys[tracks[3] + 1] };
        int[] align = { alignmentOf(tracks[0], tracks[1]), alignmentOf(tracks[2], tracks[3]) };
        double t = s.resolve(Prop.TOP, bh);
        double rt = s.resolve(Prop.RIGHT, bw);
        double bt = s.resolve(Prop.BOTTOM, bh);
        double l = s.resolve(Prop.LEFT, bw);
        double[] insets = { Double.isNaN(t) ? 0 : t, Double.isNaN(rt) ? 0 : rt, Double.isNaN(bt) ? 0 : bt, Double.isNaN(l) ? 0 : l };
        return new Area(region, r, align, insets);
    }

    /**
     * Applies <code>anchor-center</code> (CSS Anchor Positioning 4.2): a box whose <code>justify-self</code> or <code>align-self</code> is <code>anchor-center</code> and that
     * has a default anchor box is centered over the anchor in that axis, within its inset-modified containing block (the auto insets and auto margins are 0 there). If
     * the box has a position area, the alignment of that axis of the area changes; else a region of the padding box (reduced by the insets that are not auto) is made for
     * the axes that ask for it, and the other axis is laid out as usual. A box without a default anchor box is not changed (the keyword then behaves as <code>center</code>
     * in the specification; this engine leaves the box where the insets and the static position put it).
     * <p>
     * <code>anchor-center</code> 를 적용한다(CSS Anchor Positioning 4.2 절): <code>justify-self</code> 나 <code>align-self</code> 가 <code>anchor-center</code> 이고 기본 앵커 상자가
     * 있는 상자는 그 축에서 inset-modified containing block 안에서(거기서 auto 오프셋과 auto 마진은 0) 앵커 가운데에 놓인다. 상자에 위치 영역이 있으면 영역의 그 축 정렬을 바꾸고,
     * 없으면 그 축을 요구하는 축에 대해 padding box(auto 가 아닌 오프셋만큼 줄인)의 영역을 만들며 다른 축은 평소대로 배치한다. 기본 앵커 상자가 없는 상자는 바꾸지 않는다
     * (규격에서는 그때 <code>center</code> 처럼 동작하지만 이 엔진은 상자를 오프셋과 정적 위치가 정한 자리에 둔다).
     *
     * @param area the position area of the box or null / 상자의 위치 영역. 없으면 null
     * @param a    the absolutely positioned box (its style is the base style) / 절대 배치 상자(스타일은 기본 스타일)
     * @param cb   the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list the positioned boxes of the containing block / 포함 블록의 배치 상자
     * @param idx  the index of the box in the list / 목록에서 상자의 번호
     * @param bx   left of the padding box of the containing block / 포함 블록 padding box 의 왼쪽
     * @param by   top of the padding box / 위쪽
     * @param bw   width of the padding box / 너비
     * @param bh   height of the padding box / 높이
     * @return the area to use or null / 쓸 영역. 없으면 null
     */
    static Area withAnchorCenter(Area area, LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx, double bx, double by, double bw, double bh) {
        ComputedStyle s = a.style;
        String wm = Css.kw(s, Prop.WRITING_MODE);
        boolean vertical = wm.startsWith("vertical") || wm.startsWith("sideways");
        boolean cx = isAnchorCenter(s.get(vertical ? Prop.ALIGN_SELF : Prop.JUSTIFY_SELF));
        boolean cy = isAnchorCenter(s.get(vertical ? Prop.JUSTIFY_SELF : Prop.ALIGN_SELF));
        if (!cx && !cy) {
            return area;
        }
        if (area != null) {
            // 위치 영역이 있으면 그 영역 안에서 앵커 가운데 정렬로 바꾼다.
            if (cx) {
                area.align[0] = Area.ANCHOR_CENTER;
            }
            if (cy) {
                area.align[1] = Area.ANCHOR_CENTER;
            }
            return area;
        }
        double[] r = new Lookup(a, cb, list, idx).defaultRect();
        if (r == null) {
            return null;
        }
        double[] region = { bx, bx + bw, by, by + bh };
        int[] align = { cx ? Area.ANCHOR_CENTER : Area.START, cy ? Area.ANCHOR_CENTER : Area.START };
        return new Area(region, r, align, insetsOf(s, bw, bh), new boolean[] { cx, cy });
    }

    /**
     * Tells whether a self-alignment value is <code>anchor-center</code>.
     * <p>
     * 자기 정렬 값이 <code>anchor-center</code> 인지 알려 준다.
     *
     * @param v the value, may be null / 값. null 가능
     * @return true for <code>anchor-center</code> / <code>anchor-center</code> 이면 true
     */
    private static boolean isAnchorCenter(CssValue v) {
        return v instanceof CssKeyword && "anchor-center".equals(((CssKeyword) v).getName());
    }

    /**
     * Tells whether a style names a default anchor: <code>position-anchor</code> with an anchor name, <code>auto</code> or <code>match-parent</code>.
     * <p>
     * 스타일이 기본 앵커를 가리키는지 알려 준다: 앵커 이름, <code>auto</code>, <code>match-parent</code> 인 <code>position-anchor</code>.
     *
     * @param s the style / 스타일
     * @return true if there is a default anchor setting / 기본 앵커 설정이 있으면 true
     */
    static boolean hasDefaultAnchor(ComputedStyle s) {
        String k = defaultAnchor(s);
        return k.startsWith("--") || "auto".equals(k) || "match-parent".equals(k);
    }

    /**
     * Returns the insets of a style that count from the edges of a region: {top, right, bottom, left}, zero for <code>auto</code>.
     * <p>
     * 스타일의 오프셋 중 영역의 가장자리에서부터 세는 것을 반환한다: {위, 오른쪽, 아래, 왼쪽}. <code>auto</code> 는 0.
     *
     * @param s  the style / 스타일
     * @param bw width of the padding box (the base of percentages) / padding box 의 너비(백분율의 기준)
     * @param bh height of the padding box / padding box 의 높이
     * @return the insets / 오프셋
     */
    private static double[] insetsOf(ComputedStyle s, double bw, double bh) {
        double t = s.resolve(Prop.TOP, bh);
        double rt = s.resolve(Prop.RIGHT, bw);
        double bt = s.resolve(Prop.BOTTOM, bh);
        double l = s.resolve(Prop.LEFT, bw);
        return new double[] { Double.isNaN(t) ? 0 : t, Double.isNaN(rt) ? 0 : rt, Double.isNaN(bt) ? 0 : bt, Double.isNaN(l) ? 0 : l };
    }

    /**
     * Tells whether the base style of a box has an <code>anchor()</code> or <code>anchor-size()</code> function without a fallback value whose anchor does not exist
     * (the "required anchor references" of <code>position-visibility: anchors-valid</code>, CSS Anchor Positioning 1 section 6.6; the specification leaves the exact
     * definition open, this engine hides the box if any such function has no anchor).
     * <p>
     * 기본 스타일에 대체값이 없는 <code>anchor()</code> 나 <code>anchor-size()</code> 함수가 있고 그 앵커가 없는지 알려 준다(<code>position-visibility: anchors-valid</code> 의
     * "필수 앵커 참조", CSS Anchor Positioning 1 6.6 절. 규격이 정확한 정의를 열어 두었으며 이 엔진은 앵커가 없는 그런 함수가 하나라도 있으면 상자를 숨긴다).
     *
     * @param input the style of the box before the functions were resolved / 함수를 풀기 전의 상자 스타일
     * @param a     the absolutely positioned box / 절대 배치 상자
     * @param cb    the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list  the positioned boxes of the containing block / 포함 블록의 배치 상자
     * @param idx   the index of the box in the list / 목록에서 상자의 번호
     * @return true if a required anchor is missing / 필수 앵커가 없으면 true
     */
    static boolean anchorsMissing(ComputedStyle input, LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx) {
        Lookup lookup = new Lookup(a, cb, list, idx);
        // 오프셋 속성과 크기 속성의 함수를 하나씩 본다.
        for (int k = 0; k < 2; k++) {
            int[] props = k == 0 ? INSETS : SIZES;
            // 속성마다 값이 앵커 함수이고 앵커가 없는지 본다.
            for (int i = 0; i < props.length; i++) {
                CssValue v = input.get(props[i]);
                if (isAnchorFunction(v) && missing((CssFunctionValue) v, lookup)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether an anchor function has no fallback and no anchor.
     * <p>
     * anchor 함수에 대체값도 앵커도 없는지 알려 준다.
     *
     * @param fn     the function / 함수
     * @param lookup the anchor lookup / 앵커 조회
     * @return true if it cannot be resolved for lack of an anchor / 앵커가 없어서 풀 수 없으면 true
     */
    private static boolean missing(CssFunctionValue fn, Lookup lookup) {
        List<CssValue> args = fn.getArguments();
        if (args.size() > 1 && (args.get(1) instanceof CssLength || args.get(1) instanceof CssPercentage)) {
            return false;
        }
        List<CssValue> head = args.isEmpty() ? Collections.<CssValue>emptyList() : (args.get(0) instanceof CssList ? ((CssList) args.get(0)).getItems() : Collections.singletonList(args.get(0)));
        String name = null;
        // 첫 인수에서 앵커 이름(--로 시작)을 찾는다.
        for (int i = 0; i < head.size(); i++) {
            if (head.get(i) instanceof CssKeyword && ((CssKeyword) head.get(i)).getName().startsWith("--")) {
                name = ((CssKeyword) head.get(i)).getName();
            }
        }
        return (name != null ? lookup.rect(name) : lookup.defaultRect()) == null;
    }

    /**
     * Tells whether the default anchor of a box is invisible or clipped (see {@link Lookup#hidden}); a box without a default anchor box is not affected.
     * <p>
     * 상자의 기본 앵커가 보이지 않거나 잘렸는지 알려 준다({@link Lookup#hidden} 참조). 기본 앵커 상자가 없는 상자는 영향을 받지 않는다.
     *
     * @param a    the absolutely positioned box (its style is the style in use) / 절대 배치 상자(스타일은 쓰고 있는 스타일)
     * @param cb   the containing block box or null / 포함 블록 상자. 없으면 null
     * @param list the positioned boxes of the containing block / 포함 블록의 배치 상자
     * @param idx  the index of the box in the list / 목록에서 상자의 번호
     * @return true if the anchor counts as hidden / 앵커가 숨겨진 것으로 치면 true
     */
    static boolean anchorHidden(LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx) {
        Lookup lookup = new Lookup(a, cb, list, idx);
        LayoutBox anchor = lookup.defaultBox();
        return anchor != null && lookup.hidden(anchor);
    }

    /**
     * Returns the alignment that <code>normal</code> resolves to for the tracks of one axis (CSS Anchor Positioning 4.1).
     * <p>
     * 한 축의 트랙에 대해 <code>normal</code> 이 풀리는 정렬을 반환한다(CSS Anchor Positioning 4.1).
     *
     * @param first the first selected track (0 to 2) / 처음 고른 트랙(0~2)
     * @param last  the last selected track / 마지막으로 고른 트랙
     * @return one of the alignment constants of {@link Area} / {@link Area} 의 정렬 상수 중 하나
     */
    private static int alignmentOf(int first, int last) {
        if (first == 0 && last == 2) {
            return Area.ANCHOR_CENTER;
        }
        if (first == 1 && last == 1) {
            return Area.CENTER;
        }
        // 시작 트랙을 포함하면 앵커는 끝 쪽에 있으므로 끝으로, 끝 트랙을 포함하면 시작으로 정렬한다.
        return first == 0 ? Area.END : Area.START;
    }

    /**
     * Reads the keywords of <code>position-area</code> into the tracks that they select: {first column, last column, first row, last row}, each 0 to 2. Logical
     * keywords are taken in the horizontal writing mode, left to right. Returns null for a value that is not understood.
     * <p>
     * <code>position-area</code> 의 키워드를 그것이 고르는 트랙으로 읽는다: {첫 열, 마지막 열, 첫 행, 마지막 행}(각각 0~2). 논리 키워드는 가로쓰기, 왼쪽에서 오른쪽으로
     * 본다. 이해하지 못하는 값이면 null.
     *
     * @param v the value / 값
     * @return the tracks or null / 트랙. 없으면 null
     */
    static int[] tracksOf(CssValue v) {
        List<String> words = new ArrayList<String>();
        // 값의 키워드를 모은다(하나이거나 공백 목록).
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : (v == null ? new ArrayList<CssValue>() : Collections.singletonList(v));
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof CssKeyword) {
                words.add(((CssKeyword) items.get(i)).getName().toLowerCase(java.util.Locale.ENGLISH));
            }
        }
        if (words.isEmpty() || words.size() > 2) {
            return null;
        }
        int[] cols = null;
        int[] rows = null;
        List<String> ambiguous = new ArrayList<String>();
        // 축이 분명한 키워드는 그 축에, 모호한 것은 모아 둔다.
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            int axis = axisOf(w);
            int[] t = trackOf(w);
            if (t == null) {
                return null;
            }
            if (axis == 0 && cols == null) {
                cols = t;
            } else if (axis == 1 && rows == null) {
                rows = t;
            } else if (axis == 2) {
                ambiguous.add(w);
            } else {
                return null;
            }
        }
        int[] all = { 0, 2 };
        // 모호한 키워드를 남은 축에 배정한다: 축이 분명한 키워드가 있으면 반대 축, 둘 다 모호하면 첫째는 세로(블록), 둘째는 가로(인라인).
        if (ambiguous.size() == 1 && words.size() == 1) {
            int[] t = trackOf(ambiguous.get(0));
            cols = t;
            rows = t;
        } else if (ambiguous.size() == 1) {
            int[] t = trackOf(ambiguous.get(0));
            if (cols == null) {
                cols = t;
            } else {
                rows = t;
            }
        } else if (ambiguous.size() == 2) {
            rows = trackOf(ambiguous.get(0));
            cols = trackOf(ambiguous.get(1));
        }
        if (cols == null) {
            cols = all;
        }
        if (rows == null) {
            rows = all;
        }
        return new int[] { cols[0], cols[1], rows[0], rows[1] };
    }

    /**
     * Returns the axis that a <code>position-area</code> keyword names: 0 for horizontal, 1 for vertical, 2 if it is ambiguous.
     * <p>
     * <code>position-area</code> 키워드가 가리키는 축을 반환한다: 가로 0, 세로 1, 모호하면 2.
     *
     * @param w the keyword / 키워드
     * @return the axis / 축
     */
    private static int axisOf(String w) {
        boolean x = w.startsWith("x-") || w.contains("-x-");
        boolean y = w.startsWith("y-") || w.contains("-y-");
        boolean explicit = x || y || w.contains("block") || w.contains("inline");
        // center, span-all 과 축을 밝히지 않은 start/end 계열(self- 포함)은 모호하다.
        if (w.equals("center") || w.equals("span-all") || ((w.endsWith("start") || w.endsWith("end")) && !explicit && !w.equals("left") && !w.equals("right"))) {
            return 2;
        }
        if (w.contains("left") || w.contains("right") || x || w.contains("inline")) {
            return 0;
        }
        if (w.contains("top") || w.contains("bottom") || y || w.contains("block")) {
            return 1;
        }
        return 2;
    }

    /**
     * Returns the tracks {first, last} that a <code>position-area</code> keyword selects in its axis, or null for an unknown keyword.
     * <p>
     * <code>position-area</code> 키워드가 자신의 축에서 고르는 트랙 {처음, 마지막}을 반환한다. 모르는 키워드이면 null.
     *
     * @param w the keyword / 키워드
     * @return the tracks or null / 트랙. 없으면 null
     */
    private static int[] trackOf(String w) {
        if (w.equals("center")) {
            return new int[] { 1, 1 };
        }
        if (w.equals("span-all")) {
            return new int[] { 0, 2 };
        }
        boolean span = w.startsWith("span-");
        String k = span ? w.substring(5) : w;
        boolean start = k.equals("left") || k.equals("top") || k.endsWith("start");
        boolean end = k.equals("right") || k.equals("bottom") || k.endsWith("end");
        if (!start && !end) {
            return null;
        }
        if (span) {
            return start ? new int[] { 0, 1 } : new int[] { 1, 2 };
        }
        return start ? new int[] { 0, 0 } : new int[] { 2, 2 };
    }

    /**
     * Returns the default anchor name of a style (<code>position-anchor</code> with a dashed name), or null.
     * <p>
     * 스타일의 기본 앵커 이름(대시 이름인 <code>position-anchor</code>)을 반환한다. 없으면 null.
     *
     * @param s the style / 스타일
     * @return the name or null / 이름. 없으면 null
     */
    private static String defaultAnchor(ComputedStyle s) {
        CssValue v = s.get(Prop.POSITION_ANCHOR);
        // position-anchor 는 normal(기본), none, auto, match-parent, 앵커 이름 중 하나이다.
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : "normal";
    }

    /**
     * Returns the inset that aligns the edge of the containing block that the property belongs to with an edge of the anchor box (3.2), or NaN if the side cannot be used
     * in this property.
     * <p>
     * 오프셋 속성이 속한 포함 블록의 가장자리를 앵커 상자의 한 가장자리에 맞추는 오프셋을 반환한다(3.2). 그 변을 이 속성에서 쓸 수 없으면 NaN.
     *
     * @param prop the inset property / 오프셋 속성
     * @param word the side keyword / 변 키워드
     * @param pct  the percentage side or NaN / 백분율 변. 없으면 NaN
     * @param r    the anchor box {x, y, width, height} in the border box coordinates of the containing block / 포함 블록의 border box 좌표의 앵커 상자
     * @param s    the style of the positioned box / 배치 상자의 스타일
     * @param pad  the padding box of the containing block / 포함 블록의 padding box
     * @return the inset in pixels or NaN / 픽셀 오프셋. 없으면 NaN
     */
    private static double edge(int prop, String word, double pct, double[] r, ComputedStyle s, double[] pad) {
        boolean horizontal = prop == Prop.LEFT || prop == Prop.RIGHT;
        double start = horizontal ? r[0] : r[1];
        double extent = horizontal ? r[2] : r[3];
        // 속성의 변이 시작쪽(left, top)인지 끝쪽(right, bottom)인지로 inside/outside 와 start/end 를 정한다.
        boolean propStart = prop == Prop.LEFT || prop == Prop.TOP;
        double fraction;
        if (!Double.isNaN(pct)) {
            fraction = pct / 100.0;
        } else if (word == null) {
            return Double.NaN;
        } else if ("center".equals(word)) {
            fraction = 0.5;
        } else if ("left".equals(word)) {
            if (!horizontal) {
                return Double.NaN;
            }
            fraction = 0;
        } else if ("right".equals(word)) {
            if (!horizontal) {
                return Double.NaN;
            }
            fraction = 1;
        } else if ("top".equals(word)) {
            if (horizontal) {
                return Double.NaN;
            }
            fraction = 0;
        } else if ("bottom".equals(word)) {
            if (horizontal) {
                return Double.NaN;
            }
            fraction = 1;
        } else if ("inside".equals(word)) {
            fraction = propStart ? 0 : 1;
        } else if ("outside".equals(word)) {
            fraction = propStart ? 1 : 0;
        } else if ("start".equals(word) || "self-start".equals(word) || "end".equals(word) || "self-end".equals(word)) {
            // 논리 변은 가로쓰기, 왼쪽에서 오른쪽에서만 풀 수 있다(시작은 위/왼쪽, 끝은 아래/오른쪽).
            if (!"horizontal-tb".equals(Css.kw(s, Prop.WRITING_MODE)) && Css.kw(s, Prop.WRITING_MODE).length() > 0) {
                return Double.NaN;
            }
            if ("rtl".equals(Css.kw(s, Prop.DIRECTION))) {
                return Double.NaN;
            }
            fraction = word.endsWith("start") ? 0 : 1;
        } else {
            return Double.NaN;
        }
        double e = start + fraction * extent;
        // 포함 블록 padding box 의 가장자리에서 e 까지의 거리가 오프셋이다.
        if (prop == Prop.LEFT) {
            return e - pad[0];
        }
        if (prop == Prop.RIGHT) {
            return pad[0] + pad[2] - e;
        }
        if (prop == Prop.TOP) {
            return e - pad[1];
        }
        return pad[1] + pad[3] - e;
    }

    /**
     * Returns the size of the anchor box for an <code>anchor-size()</code> keyword (5.1), or NaN.
     * <p>
     * <code>anchor-size()</code> 키워드에 따른 앵커 상자의 크기를 반환한다(5.1). 없으면 NaN.
     *
     * @param word the keyword / 키워드
     * @param r    the anchor box / 앵커 상자
     * @param s    the style of the positioned box / 배치 상자의 스타일
     * @return the size in pixels or NaN / 픽셀 크기. 없으면 NaN
     */
    private static double sizeOf(String word, double[] r, ComputedStyle s) {
        if (word == null) {
            return Double.NaN;
        }
        boolean vertical = Css.kw(s, Prop.WRITING_MODE).startsWith("vertical") || Css.kw(s, Prop.WRITING_MODE).startsWith("sideways");
        if ("width".equals(word)) {
            return r[2];
        }
        if ("height".equals(word)) {
            return r[3];
        }
        if ("inline".equals(word) || "self-inline".equals(word)) {
            return vertical ? r[3] : r[2];
        }
        if ("block".equals(word) || "self-block".equals(word)) {
            return vertical ? r[2] : r[3];
        }
        return Double.NaN;
    }

    /**
     * Finds the target anchor element for a name (2.3) and gives its box in the border box coordinates of the containing block.
     * <p>
     * 이름의 대상 앵커 요소를 찾고(2.3) 포함 블록의 border box 좌표로 그 상자를 알려 준다.
     */
    private static final class Lookup {

        /** The positioned box. 배치 상자. */
        private final LayoutBox a;

        /** The containing block or null for the initial one. 포함 블록. 초기 블록이면 null. */
        private final LayoutBox cb;

        /** The boxes that are not laid out yet (and so cannot be anchors). 아직 배치되지 않은(그래서 앵커가 될 수 없는) 상자. */
        private final Map<LayoutBox, Boolean> pending = new IdentityHashMap<LayoutBox, Boolean>();

        /**
         * Creates the lookup.
         * <p>
         * 조회기를 만든다.
         *
         * @param a    the positioned box / 배치 상자
         * @param cb   the containing block or null / 포함 블록. 없으면 null
         * @param list the positioned boxes of the containing block / 포함 블록의 배치 상자
         * @param idx  the index of the box in the list / 목록에서 상자의 번호
         */
        Lookup(LayoutBox a, LayoutBox cb, List<LayoutBox> list, int idx) {
            this.a = a;
            this.cb = cb;
            // 이 상자와 목록의 뒤쪽 상자는 아직 배치되지 않았다.
            for (int i = idx; i < list.size(); i++) {
                pending.put(list.get(i), Boolean.TRUE);
            }
        }

        /**
         * Returns the anchor box {x, y, width, height} for a name, or null if there is none.
         * <p>
         * 이름의 앵커 상자 {x, y, 너비, 높이}를 반환한다. 없으면 null.
         *
         * @param name the anchor name / 앵커 이름
         * @return the rectangle or null / 사각형. 없으면 null
         */
        double[] rect(String name) {
            LayoutBox anchor = anchorBox(name);
            return anchor == null ? null : boxOf(anchor);
        }

        /**
         * Finds the box of the target anchor element for a name (2.3 and, for the names that an <code>anchor-scope</code> limits, 2.2).
         * <p>
         * 이름의 대상 앵커 요소의 상자를 찾는다(2.3 절, <code>anchor-scope</code> 가 범위를 제한한 이름은 2.2 절).
         *
         * @param name the anchor name / 앵커 이름
         * @return the box or null / 상자. 없으면 null
         */
        LayoutBox anchorBox(String name) {
            LayoutBox anchor = null;
            // 배치 상자의 조상 중 가장 가까운 것을 먼저 찾는다(포함 블록 자신까지).
            for (LayoutBox p = a.parent; p != null && anchor == null; p = p.parent) {
                if (named(p, name) && acceptable(p) && inScope(p, name)) {
                    anchor = p;
                }
                if (p == cb) {
                    break;
                }
            }
            if (anchor == null) {
                LayoutBox root = cb;
                // 초기 포함 블록이면 트리의 맨 위 상자에서 찾는다.
                if (root == null) {
                    root = a;
                    // 맨 위 상자까지 올라간다.
                    while (root.parent != null) {
                        root = root.parent;
                    }
                }
                anchor = last(root, name, null);
            }
            return anchor;
        }

        /**
         * Tells whether the anchor name of a box is visible to the positioned box under the <code>anchor-scope</code> rules (CSS Anchor Positioning 1 section 2.2): if an
         * element at or above the anchor limits the name to its subtree, the positioned box must be in that subtree, and if an element at or above the positioned box
         * limits the name, the anchor must be in that subtree.
         * <p>
         * <code>anchor-scope</code> 규칙에 따라 상자의 앵커 이름이 배치 상자에 보이는지 알려 준다(CSS Anchor Positioning 1 2.2 절): 앵커이거나 그 조상인 요소가 이름을 자신의 서브트리로
         * 제한하면 배치 상자가 그 서브트리 안에 있어야 하고, 배치 상자이거나 그 조상인 요소가 이름을 제한하면 앵커가 그 서브트리 안에 있어야 한다.
         *
         * @param anchor the box that has the name / 이름을 가진 상자
         * @param name   the name / 이름
         * @return true if the name can be used / 이름을 쓸 수 있으면 true
         */
        private boolean inScope(LayoutBox anchor, String name) {
            LayoutBox own = scopeOf(anchor, name);
            if (own != null && !contains(own, a)) {
                return false;
            }
            LayoutBox mine = scopeOf(a, name);
            return mine == null || contains(mine, anchor);
        }

        /**
         * Finds the nearest element at or above a box whose <code>anchor-scope</code> names a name (or is <code>all</code>).
         * <p>
         * 상자이거나 그 조상인 요소 중 <code>anchor-scope</code> 가 이름을 가리키는(또는 <code>all</code> 인) 가장 가까운 것을 찾는다.
         *
         * @param from the box to start at / 시작할 상자
         * @param name the name / 이름
         * @return the box of that element or null / 그 요소의 상자. 없으면 null
         */
        private static LayoutBox scopeOf(LayoutBox from, String name) {
            // 상자에서 위로 올라가며 이름을 제한하는 anchor-scope 가 있는 요소를 찾는다.
            for (LayoutBox q = from; q != null; q = q.parent) {
                if (q.style == null || !(q.node instanceof Element) || q.pseudo != null || q.type == LayoutBox.Type.LINE || q.type == LayoutBox.Type.TEXT) {
                    continue;
                }
                CssValue v = q.style.get(Prop.ANCHOR_SCOPE);
                if (v == null || v.isKeyword("none")) {
                    continue;
                }
                if (v.isKeyword("all")) {
                    return q;
                }
                List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : Collections.singletonList(v);
                // 이름 목록에 같은 이름이 있는지 본다.
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) instanceof CssKeyword && ((CssKeyword) items.get(i)).getName().equals(name)) {
                        return q;
                    }
                }
            }
            return null;
        }

        /**
         * Tells whether a box is at or below another box in the box tree.
         * <p>
         * 상자가 다른 상자 자신이거나 그 아래에 있는지 알려 준다.
         *
         * @param top the upper box / 위쪽 상자
         * @param b   the box / 상자
         * @return true if <code>b</code> is <code>top</code> or a descendant / <code>b</code> 가 <code>top</code> 이거나 자손이면 true
         */
        private static boolean contains(LayoutBox top, LayoutBox b) {
            // b 에서 위로 올라가며 top 을 만나는지 본다.
            for (LayoutBox q = b; q != null; q = q.parent) {
                if (q == top) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the box of an anchor {x, y, width, height} in the border box coordinates of the containing block.
         * <p>
         * 앵커의 상자 {x, y, 너비, 높이}를 포함 블록의 border box 좌표로 반환한다.
         *
         * @param anchor the anchor box / 앵커 상자
         * @return the rectangle / 사각형
         */
        private double[] boxOf(LayoutBox anchor) {
            double x = 0;
            double y = 0;
            // 앵커에서 포함 블록(제외)까지 올라가며 위치를 더한다.
            for (LayoutBox q = anchor; q != null && q != cb; q = q.parent) {
                x += q.lx;
                y += q.ly;
            }
            return new double[] {x, y, anchor.width, anchor.height};
        }

        /**
         * Returns the box of the default anchor of the positioned box (CSS Anchor Positioning 2.4): the element named by <code>position-anchor</code>, the
         * implicit anchor element for <code>auto</code> (and for <code>normal</code> when there is a <code>position-area</code>), the anchor of the parent for
         * <code>match-parent</code>, none for <code>none</code>.
         * <p>
         * 배치 상자의 기본 앵커의 상자를 반환한다(CSS Anchor Positioning 2.4): <code>position-anchor</code> 가 가리키는 요소, <code>auto</code>(와 <code>position-area</code> 가
         * 있을 때의 <code>normal</code>)이면 암묵 앵커 요소, <code>match-parent</code> 이면 부모의 앵커, <code>none</code> 이면 없음.
         *
         * @return the rectangle or null / 사각형. 없으면 null
         */
        double[] defaultRect() {
            LayoutBox b = defaultBox();
            return b == null ? null : boxOf(b);
        }

        /**
         * Returns the box of the default anchor (see {@link #defaultRect()}), or null.
         * <p>
         * 기본 앵커의 상자를 반환한다({@link #defaultRect()} 참조). 없으면 null.
         *
         * @return the box or null / 상자. 없으면 null
         */
        LayoutBox defaultBox() {
            String k = defaultAnchor(a.style);
            // 이름이 있으면 그 이름의 앵커이다.
            if (k.startsWith("--")) {
                return anchorBox(k);
            }
            if ("auto".equals(k) || ("normal".equals(k) && hasArea(a.style))) {
                return implicitBox();
            }
            if (!"match-parent".equals(k)) {
                return null;
            }
            // match-parent: 의사 요소는 원래 요소를, 그 밖의 상자는 부모(가장 가까운 요소 조상)의 기본 앵커 이름을 쓴다.
            if (a.pseudo != null) {
                return implicitBox();
            }
            // 배치 상자는 포함 블록으로 옮겨져 트리의 부모가 DOM 부모가 아니므로 DOM 부모의 주 상자를 트리에서 찾는다(상자가 없는 조상은 건너뛴다).
            for (org.duckdns.hjow.swingwebpanel.dom.Node n = a.node == null ? null : a.node.getParentNode(); n instanceof Element; n = n.getParentNode()) {
                LayoutBox p = principalBoxOf(n);
                if (p != null && p.style != null) {
                    String pk = defaultAnchor(p.style);
                    return pk.startsWith("--") ? anchorBox(pk) : null;
                }
            }
            return null;
        }

        /**
         * Finds the principal box of an element in the whole box tree (the boxes of fragments and lines of the element are not principal boxes).
         * <p>
         * 요소의 주 상자를 전체 상자 트리에서 찾는다(요소의 줄이나 인라인 조각 상자는 주 상자가 아니다).
         *
         * @param n the element / 요소
         * @return the box or null / 상자. 없으면 null
         */
        private LayoutBox principalBoxOf(org.duckdns.hjow.swingwebpanel.dom.Node n) {
            LayoutBox root = a;
            // 트리의 맨 위 상자까지 올라간다.
            while (root.parent != null) {
                root = root.parent;
            }
            return principalIn(root, n);
        }

        /**
         * Searches the principal box of an element below a box, in tree order.
         * <p>
         * 상자 아래에서 요소의 주 상자를 트리 순서로 찾는다.
         *
         * @param b the root of the search / 찾기의 루트
         * @param n the element / 요소
         * @return the box or null / 상자. 없으면 null
         */
        private LayoutBox principalIn(LayoutBox b, org.duckdns.hjow.swingwebpanel.dom.Node n) {
            if (b.node == n && b.pseudo == null && b.type != LayoutBox.Type.LINE && b.type != LayoutBox.Type.TEXT && b.type != LayoutBox.Type.INLINE && b != a) {
                return b;
            }
            // 자식을 차례로 훑으며 처음 찾은 것을 반환한다.
            for (int i = 0; i < b.children.size(); i++) {
                LayoutBox found = principalIn(b.children.get(i), n);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        /**
         * Returns the box of the implicit anchor element (CSS Anchor Positioning 2.4.1: the implicit anchor element of a pseudo-element is its originating
         * element), or null if the positioned box has none or it cannot be an anchor.
         * <p>
         * 암묵 앵커 요소의 상자를 반환한다(CSS Anchor Positioning 2.4.1: 의사 요소의 암묵 앵커 요소는 그것의 원래 요소). 배치 상자에 없거나 앵커가 될 수 없으면 null.
         *
         * @return the rectangle or null / 사각형. 없으면 null
         */
        private double[] implicitRect() {
            LayoutBox b = implicitBox();
            return b == null ? null : boxOf(b);
        }

        /**
         * Returns the box of the implicit anchor element (see {@link #implicitRect()}), or null.
         * <p>
         * 암묵 앵커 요소의 상자를 반환한다({@link #implicitRect()} 참조). 없으면 null.
         *
         * @return the box or null / 상자. 없으면 null
         */
        private LayoutBox implicitBox() {
            if (a.pseudo == null) {
                return null;
            }
            // 의사 요소의 원래 요소의 주 상자를 트리에서 찾는다(절대 배치 상자는 포함 블록으로 옮겨져 있어 조상이 아닐 수 있다).
            LayoutBox origin = principalBoxOf(a.node);
            if (origin == null || !acceptable(origin)) {
                return null;
            }
            // 앵커는 포함 블록 자신이거나 그 안에 있어야 한다(포함 블록이 초기 블록이면 모든 상자).
            boolean inside = cb == null;
            // 앵커에서 위로 올라가며 포함 블록을 만나는지 본다.
            for (LayoutBox q = origin; q != null && !inside; q = q.parent) {
                inside = q == cb;
            }
            return inside ? origin : null;
        }

        /**
         * Tells whether the anchor box is invisible or clipped by an intervening box, so that <code>position-visibility: anchors-visible</code> hides the positioned
         * box (CSS Anchor Positioning 1 section 6.6): the anchor was itself hidden by <code>position-visibility</code>, or an ancestor of the anchor that is a descendant
         * of the containing block clips its overflow and the anchor lies completely outside of the clip rectangle of the padding box. The scroll positions of the
         * ancestors are not known during the layout, so a scroller shows its unscrolled content.
         * <p>
         * 앵커 상자가 보이지 않거나 중간의 상자에 잘려서 <code>position-visibility: anchors-visible</code> 이 배치 상자를 숨기는지 알려 준다(CSS Anchor Positioning 1 6.6 절):
         * 앵커 자신이 <code>position-visibility</code> 로 숨겨졌거나, 앵커의 조상이면서 포함 블록의 자손인 상자가 넘침을 자르고 앵커가 그 padding box 의 자르기 사각형 밖에 완전히
         * 있는 경우. 조상의 스크롤 위치는 레이아웃 중에 알 수 없으므로 스크롤 컨테이너는 스크롤하지 않은 내용을 보인다.
         *
         * @param anchor the default anchor box / 기본 앵커 상자
         * @return true if the anchor counts as hidden / 앵커가 숨겨진 것으로 치면 true
         */
        boolean hidden(LayoutBox anchor) {
            if (anchor.positionHidden) {
                return true;
            }
            double[] r = boxOf(anchor);
            // 넓이가 없는 앵커는 교차 넓이를 판정할 수 없으므로 잘린 것으로 보지 않는다.
            if (r[2] <= 0 || r[3] <= 0) {
                return false;
            }
            double x0 = r[0];
            double y0 = r[1];
            double x1 = r[0] + r[2];
            double y1 = r[1] + r[3];
            // 앵커의 조상 중 포함 블록(제외) 아래의 상자가 넘침을 자르면 그 padding box 와 앵커가 겹치는지 본다.
            for (LayoutBox q = anchor.parent; q != null && q != cb; q = q.parent) {
                if (q.style == null || !(q.node instanceof Element)) {
                    continue;
                }
                boolean cx = clips(q.style, Prop.OVERFLOW_X);
                boolean cy = clips(q.style, Prop.OVERFLOW_Y);
                if (!cx && !cy) {
                    continue;
                }
                double[] qb = boxOf(q);
                double px0 = qb[0] + q.borderLeft;
                double px1 = qb[0] + q.width - q.borderRight;
                double py0 = qb[1] + q.borderTop;
                double py1 = qb[1] + q.height - q.borderBottom;
                if ((cx && (x1 <= px0 || x0 >= px1)) || (cy && (y1 <= py0 || y0 >= py1))) {
                    return true;
                }
                // 겹치는 부분만 남겨 바깥의 자르기와 이어서 판정한다.
                if (cx) {
                    x0 = Math.max(x0, px0);
                    x1 = Math.min(x1, px1);
                }
                if (cy) {
                    y0 = Math.max(y0, py0);
                    y1 = Math.min(y1, py1);
                }
            }
            return false;
        }

        /**
         * Tells whether an overflow value clips (everything but <code>visible</code>).
         * <p>
         * 넘침 값이 자르는지 알려 준다(<code>visible</code> 이외 전부).
         *
         * @param s    the style / 스타일
         * @param prop <code>overflow-x</code> or <code>overflow-y</code> / <code>overflow-x</code> 또는 <code>overflow-y</code>
         * @return true if the box clips / 상자가 자르면 true
         */
        private static boolean clips(ComputedStyle s, int prop) {
            String k = Css.kw(s, prop);
            return k.length() > 0 && !"visible".equals(k);
        }

        /**
         * Returns the last box in tree order that has the name and is an acceptable anchor.
         * <p>
         * 이름이 있고 앵커가 될 수 있는 상자 중 트리 순서의 마지막 것을 반환한다.
         *
         * @param b     the root of the search / 찾기의 루트
         * @param name  the name / 이름
         * @param found the best so far / 지금까지의 결과
         * @return the box or null / 상자. 없으면 null
         */
        private LayoutBox last(LayoutBox b, String name, LayoutBox found) {
            LayoutBox best = found;
            if (b != a && named(b, name) && acceptable(b) && inScope(b, name)) {
                best = b;
            }
            // 자식을 차례로 훑으며 뒤에 있는 것이 이기게 한다.
            for (int i = 0; i < b.children.size(); i++) {
                if (b.children.get(i) != a) {
                    best = last(b.children.get(i), name, best);
                }
            }
            return best;
        }

        /**
         * Tells whether a box can be the anchor: not the positioned box or inside it, not a box that is not laid out yet or inside one.
         * <p>
         * 상자가 앵커가 될 수 있는지 알려 준다: 배치 상자 자신이나 그 안이 아니고, 아직 배치되지 않은 상자나 그 안이 아니어야 한다.
         *
         * @param b the box / 상자
         * @return true if it can be / 될 수 있으면 true
         */
        private boolean acceptable(LayoutBox b) {
            // 위로 올라가며 배치 상자나 아직 배치되지 않은 상자를 지나는지 본다.
            for (LayoutBox q = b; q != null; q = q.parent) {
                if (q == a || pending.containsKey(q)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Tells whether a box is the principal box of an element whose <code>anchor-name</code> has the name.
         * <p>
         * 상자가 <code>anchor-name</code> 에 그 이름이 있는 요소의 주 상자인지 알려 준다.
         *
         * @param b    the box / 상자
         * @param name the name / 이름
         * @return true if it has the name / 이름이 있으면 true
         */
        private static boolean named(LayoutBox b, String name) {
            if (b.style == null || !(b.node instanceof Element) || b.pseudo != null || b.type == LayoutBox.Type.LINE || b.type == LayoutBox.Type.TEXT
                    || b.type == LayoutBox.Type.INLINE) {
                return false;
            }
            CssValue v = b.style.get(Prop.ANCHOR_NAME);
            List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : (v == null ? new ArrayList<CssValue>() : Collections.singletonList(v));
            // 이름 목록에 같은 이름이 있는지 본다.
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof CssKeyword && ((CssKeyword) items.get(i)).getName().equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }
}
