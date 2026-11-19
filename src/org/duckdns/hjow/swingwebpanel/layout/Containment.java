package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The containment of a box (CSS Containment Module Level 2, <a href="https://www.w3.org/TR/css-contain-2/">https://www.w3.org/TR/css-contain-2/</a>
 * and the container types of Level 3): <code>contain</code> (<code>size</code>, <code>inline-size</code>, <code>layout</code>, <code>style</code>,
 * <code>paint</code>, <code>strict</code>, <code>content</code>), <code>container-type</code> (<code>size</code> and <code>inline-size</code> contain the
 * size, the layout and the style) and <code>content-visibility</code> (<code>hidden</code> skips the contents and contains everything,
 * <code>auto</code> contains the layout, the style and the paint). The effects this engine has: size containment makes the box lay out as if it had no
 * contents (an automatic size is zero, or <code>contain-intrinsic-width</code>/<code>-height</code>; the contents still lay out and overflow), layout
 * and paint containment make the box a block formatting context, a stacking context and the containing block of absolutely and fixed positioned
 * descendants, paint containment also clips the contents to the box, and a skipped content builds no boxes. Style containment (counter scope) has no
 * effect here.
 * <p>
 * 상자의 격리(CSS Containment Module Level 2, <a href="https://www.w3.org/TR/css-contain-2/">https://www.w3.org/TR/css-contain-2/</a> 와 Level 3 의 컨테이너 종류):
 * <code>contain</code>(<code>size</code>, <code>inline-size</code>, <code>layout</code>, <code>style</code>, <code>paint</code>, <code>strict</code>,
 * <code>content</code>), <code>container-type</code>(<code>size</code> 와 <code>inline-size</code> 는 크기, 레이아웃, 스타일을 격리한다),
 * <code>content-visibility</code>(<code>hidden</code> 은 내용을 건너뛰고 모든 것을 격리하며, <code>auto</code> 는 레이아웃, 스타일, 그리기를 격리한다). 이 엔진에서의 효과:
 * 크기 격리는 상자가 내용이 없는 것처럼 배치되게 하고(자동 크기는 0 이거나 <code>contain-intrinsic-width</code>/<code>-height</code>. 내용은 그대로 배치되어 넘친다),
 * 레이아웃과 그리기 격리는 상자를 블록 서식 문맥, 쌓임 맥락, 절대 배치와 fixed 자손의 포함 블록으로 만들며, 그리기 격리는 내용을 상자로 자르고, 건너뛴 내용은 상자를 만들지
 * 않는다. 스타일 격리(카운터 범위)는 여기서 효과가 없다.
 */
final class Containment {

    /** Size containment. / 크기 격리. */
    static final int SIZE = 1;
    /** Inline-size containment. / 인라인 크기 격리. */
    static final int INLINE_SIZE = 2;
    /** Layout containment. / 레이아웃 격리. */
    static final int LAYOUT = 4;
    /** Style containment. / 스타일 격리. */
    static final int STYLE = 8;
    /** Paint containment. / 그리기 격리. */
    static final int PAINT = 16;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Containment() {
    }

    /**
     * Returns the kinds of containment a style asks for, from <code>contain</code>, <code>container-type</code> and <code>content-visibility</code>.
     * <p>
     * 스타일이 요구하는 격리의 종류를 <code>contain</code>, <code>container-type</code>, <code>content-visibility</code> 에서 구한다.
     *
     * @param s the style, may be null / 스타일. null 일 수 있다
     * @return the bits {@link #SIZE}, {@link #INLINE_SIZE}, {@link #LAYOUT}, {@link #STYLE}, {@link #PAINT} / 격리 비트
     */
    static int flags(ComputedStyle s) {
        if (s == null) {
            return 0;
        }
        int f = 0;
        CssValue c = s.get(Prop.CONTAIN);
        // 거의 모든 요소는 contain: none 이므로 문자열로 풀어 보기 전에 키워드 하나인 경우를 빠르게 거른다.
        if (c instanceof org.duckdns.hjow.swingwebpanel.css.values.CssKeyword && "none".equals(((org.duckdns.hjow.swingwebpanel.css.values.CssKeyword) c).getName())) {
            c = null;
        }
        if (c != null) {
            String[] t = c.toCssText().toLowerCase().split("\\s+");
            // contain 의 키워드를 하나씩 비트로 옮긴다.
            for (int i = 0; i < t.length; i++) {
                if (t[i].equals("strict")) {
                    f |= SIZE | LAYOUT | STYLE | PAINT;
                } else if (t[i].equals("content")) {
                    f |= LAYOUT | STYLE | PAINT;
                } else if (t[i].equals("size")) {
                    f |= SIZE;
                } else if (t[i].equals("inline-size")) {
                    f |= INLINE_SIZE;
                } else if (t[i].equals("layout")) {
                    f |= LAYOUT;
                } else if (t[i].equals("style")) {
                    f |= STYLE;
                } else if (t[i].equals("paint")) {
                    f |= PAINT;
                }
            }
        }
        String type = Css.kw(s, Prop.CONTAINER_TYPE);
        if ("size".equals(type)) {
            f |= SIZE | LAYOUT | STYLE;
        } else if ("inline-size".equals(type)) {
            f |= INLINE_SIZE | LAYOUT | STYLE;
        }
        String cv = Css.kw(s, Prop.CONTENT_VISIBILITY);
        if ("hidden".equals(cv)) {
            f |= SIZE | LAYOUT | STYLE | PAINT;
        } else if ("auto".equals(cv)) {
            f |= LAYOUT | STYLE | PAINT;
        }
        return f;
    }

    /**
     * Tells whether a style has size containment in both axes.
     * <p>
     * 스타일이 두 축 모두에서 크기 격리를 가졌는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true for size containment / 크기 격리이면 true
     */
    static boolean size(ComputedStyle s) {
        return (flags(s) & SIZE) != 0;
    }

    /**
     * Tells whether a style has size containment at least in the inline axis.
     * <p>
     * 스타일이 적어도 인라인 축에서 크기 격리를 가졌는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true for inline-size containment / 인라인 크기 격리이면 true
     */
    static boolean inlineSize(ComputedStyle s) {
        return (flags(s) & (SIZE | INLINE_SIZE)) != 0;
    }

    /**
     * Tells whether a style has layout or paint containment, which makes a block formatting context, a stacking context and a containing block.
     * <p>
     * 스타일이 블록 서식 문맥, 쌓임 맥락, 포함 블록을 만드는 레이아웃 또는 그리기 격리를 가졌는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true if so / 그렇다면 true
     */
    static boolean layoutOrPaint(ComputedStyle s) {
        return (flags(s) & (LAYOUT | PAINT)) != 0;
    }

    /**
     * Tells whether a style has paint containment, which clips the contents.
     * <p>
     * 스타일이 내용을 자르는 그리기 격리를 가졌는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true for paint containment / 그리기 격리이면 true
     */
    static boolean paint(ComputedStyle s) {
        return (flags(s) & PAINT) != 0;
    }

    /**
     * Tells whether the contents of a box are skipped (<code>content-visibility: hidden</code>).
     * <p>
     * 상자의 내용을 건너뛰는지(<code>content-visibility: hidden</code>) 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 일 수 있다
     * @return true if no boxes are built for the contents / 내용의 상자를 만들지 않으면 true
     */
    static boolean skipsContents(ComputedStyle s) {
        return s != null && "hidden".equals(Css.kw(s, Prop.CONTENT_VISIBILITY));
    }

    /**
     * Tells whether a style makes its element a query container.
     * <p>
     * 스타일이 요소를 질의 컨테이너로 만드는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 일 수 있다
     * @return true for <code>container-type</code> other than <code>normal</code> / <code>normal</code> 이 아닌 <code>container-type</code> 이면 true
     */
    static boolean isContainer(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        String t = Css.kw(s, Prop.CONTAINER_TYPE);
        return t.length() > 0 && !"normal".equals(t);
    }

    /**
     * Returns the width a box with inline-size containment has instead of the width of its contents: <code>contain-intrinsic-width</code> or 0.
     * <p>
     * 인라인 크기 격리가 있는 상자가 내용의 너비 대신 가지는 너비를 반환한다: <code>contain-intrinsic-width</code> 또는 0.
     *
     * @param s the style / 스타일
     * @return the width in px / 너비(px)
     */
    static double intrinsicWidth(ComputedStyle s) {
        return intrinsic(s, Prop.CONTAIN_INTRINSIC_WIDTH);
    }

    /**
     * Returns the explicit intrinsic inner width of a box with size containment (CSS Box Sizing 4 section 5.2): the last remembered width when the value
     * has <code>auto</code>, the element has a last remembered size and skips its contents, else the length of <code>contain-intrinsic-width</code> or 0.
     * <p>
     * 크기 격리가 있는 상자의 명시적 내재 안쪽 너비를 반환한다(CSS Box Sizing 4 5.2 절): 값에 <code>auto</code> 가 있고 요소에 마지막으로 기억한 크기가 있으며 내용을 건너뛰면 기억한
     * 너비, 아니면 <code>contain-intrinsic-width</code> 의 길이. 없으면 0.
     *
     * @param b the box / 상자
     * @return the width in px / 너비(px)
     */
    static double intrinsicWidth(LayoutBox b) {
        double[] r = remembered(b, Prop.CONTAIN_INTRINSIC_WIDTH);
        return r != null ? r[0] : intrinsic(b.style, Prop.CONTAIN_INTRINSIC_WIDTH);
    }

    /**
     * Returns the explicit intrinsic inner height of a box with size containment, like {@link #intrinsicWidth(LayoutBox)}.
     * <p>
     * {@link #intrinsicWidth(LayoutBox)} 처럼 크기 격리가 있는 상자의 명시적 내재 안쪽 높이를 반환한다.
     *
     * @param b the box / 상자
     * @return the height in px / 높이(px)
     */
    static double intrinsicHeight(LayoutBox b) {
        double[] r = remembered(b, Prop.CONTAIN_INTRINSIC_HEIGHT);
        return r != null ? r[1] : intrinsic(b.style, Prop.CONTAIN_INTRINSIC_HEIGHT);
    }

    /**
     * Returns the last remembered size of the element of a box when it applies to an axis: the value of the axis has <code>auto</code> and the box skips its
     * contents.
     * <p>
     * 상자의 요소의 마지막으로 기억한 크기가 축에 적용되면 반환한다: 그 축의 값에 <code>auto</code> 가 있고 상자가 내용을 건너뛸 때.
     *
     * @param b  the box / 상자
     * @param id the property of the axis / 축의 속성
     * @return {width, height} or null / {너비, 높이} 또는 null
     */
    private static double[] remembered(LayoutBox b, int id) {
        if (!(b.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element) || !hasAuto(b.style, id) || !skipsContents(b.style)) {
            return null;
        }
        org.duckdns.hjow.swingwebpanel.dom.Element e = (org.duckdns.hjow.swingwebpanel.dom.Element) b.node;
        return e.getOwnerDocument() == null ? null : e.getOwnerDocument().getLastRememberedSize(e);
    }

    /**
     * Tells whether a contain-intrinsic value starts with <code>auto</code>.
     * <p>
     * contain-intrinsic 값이 <code>auto</code> 로 시작하는지 알려 준다.
     *
     * @param s  the style / 스타일
     * @param id the property / 속성
     * @return true for <code>auto ...</code> / <code>auto ...</code> 이면 true
     */
    private static boolean hasAuto(ComputedStyle s, int id) {
        CssValue v = s == null ? null : s.get(id);
        return v != null && (v.toCssText().trim() + " ").startsWith("auto ");
    }

    /**
     * Records the last remembered sizes after a layout (CSS Box Sizing 4 section 5.2.1): an element whose <code>contain-intrinsic-width</code> or
     * <code>-height</code> has <code>auto</code> and that has no size containment remembers the inner width and height of its principal box; an element with
     * a remembered size that no longer has <code>auto</code> forgets it. Elements without a box keep what they remembered.
     * <p>
     * 배치 뒤 마지막으로 기억한 크기를 기록한다(CSS Box Sizing 4 5.2.1 절): <code>contain-intrinsic-width</code> 나 <code>-height</code> 에 <code>auto</code> 가 있고 크기 격리가 없는
     * 요소는 주 상자의 안쪽 너비와 높이를 기억하고, 기억한 크기가 있는데 더는 <code>auto</code> 가 없는 요소는 잊는다. 상자가 없는 요소는 기억을 지킨다.
     *
     * @param doc    the document / 문서
     * @param result the layout / 레이아웃
     */
    static void rememberSizes(org.duckdns.hjow.swingwebpanel.dom.Document doc, LayoutResult result) {
        if (doc == null || result == null || result.getRoot() == null) {
            return;
        }
        java.util.ArrayList<LayoutBox> stack = new java.util.ArrayList<LayoutBox>();
        stack.add(result.getRoot());
        java.util.Set<org.duckdns.hjow.swingwebpanel.dom.Node> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<org.duckdns.hjow.swingwebpanel.dom.Node, Boolean>());
        // 명시적 스택으로 상자를 훑으며 요소마다 주 상자(처음 만나는 상자) 하나만 본다.
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            if (b.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && b.pseudo == null && b.style != null && seen.add(b.node)) {
                org.duckdns.hjow.swingwebpanel.dom.Element e = (org.duckdns.hjow.swingwebpanel.dom.Element) b.node;
                boolean auto = hasAuto(b.style, Prop.CONTAIN_INTRINSIC_WIDTH) || hasAuto(b.style, Prop.CONTAIN_INTRINSIC_HEIGHT);
                if (auto && !size(b.style) && !inlineSize(b.style)) {
                    doc.setLastRememberedSize(e, new double[] { b.getContentWidth(), b.getContentHeight() });
                } else if (!auto) {
                    doc.setLastRememberedSize(e, null);
                }
            }
            stack.addAll(b.children);
        }
    }

    /**
     * Returns the height a box with size containment has instead of the height of its contents: <code>contain-intrinsic-height</code> or 0.
     * <p>
     * 크기 격리가 있는 상자가 내용의 높이 대신 가지는 높이를 반환한다: <code>contain-intrinsic-height</code> 또는 0.
     *
     * @param s the style / 스타일
     * @return the height in px / 높이(px)
     */
    static double intrinsicHeight(ComputedStyle s) {
        return intrinsic(s, Prop.CONTAIN_INTRINSIC_HEIGHT);
    }

    /**
     * Reads a contain-intrinsic size: the last length of <code>none | &lt;length&gt; | auto &lt;length&gt;</code>, or 0.
     * <p>
     * contain-intrinsic 크기를 읽는다: <code>none | &lt;length&gt; | auto &lt;length&gt;</code> 의 마지막 길이. 없으면 0.
     *
     * @param s  the style / 스타일
     * @param id the property id / 속성 id
     * @return the size in px / 크기(px)
     */
    private static double intrinsic(ComputedStyle s, int id) {
        CssValue v = s == null ? null : s.get(id);
        if (v == null) {
            return 0;
        }
        String[] t = v.toCssText().trim().split("\\s+");
        String last = t[t.length - 1];
        if (!last.endsWith("px")) {
            return 0;
        }
        try {
            return Math.max(0, Double.parseDouble(last.substring(0, last.length() - 2)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
