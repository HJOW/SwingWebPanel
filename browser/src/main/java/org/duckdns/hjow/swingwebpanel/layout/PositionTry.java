package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The position options of CSS Anchor Positioning Level 1 section 6 (<a href="https://www.w3.org/TR/css-anchor-position-1/#fallback">
 * https://www.w3.org/TR/css-anchor-position-1/#fallback</a>, W3C Working Draft, Last-Modified 2026-03-27): <code>position-try-fallbacks</code> lists alternative
 * positioning styles for an anchor-positioned box, and when the box overflows its inset-modified containing block the layout tries them in order
 * (<code>position-try-order</code> may sort them by the space that they leave) and takes the first one that does not overflow. An option is one of
 * <ul>
 * <li>a <code>@position-try</code> rule (<code>&lt;dashed-ident&gt;</code>): its declarations replace the inset, margin, size, self-alignment, <code>position-anchor</code>
 * and <code>position-area</code> values of the box;</li>
 * <li>try tactics (<code>flip-block</code>, <code>flip-inline</code>, <code>flip-start</code>, <code>flip-x</code>, <code>flip-y</code>), which mirror those values of the
 * box (or of the named rule) across an axis or the diagonal: the values of the opposite sides are swapped (<code>top</code> with <code>bottom</code>, the margins, the
 * self-alignment <code>start</code> with <code>end</code>), the sides in <code>anchor()</code> follow, a percentage becomes 100% minus itself, the tracks of
 * <code>position-area</code> are mirrored, and for <code>flip-start</code> the block-start side is exchanged with the inline-start side (and the sizes and the two
 * self-alignments with them);</li>
 * <li>a <code>position-area</code> value, which sets only that property.</li>
 * </ul>
 * This class reads the properties, builds the styles of the options and reads <code>position-visibility</code>; the layout and the overflow test are in
 * {@link AbsoluteLayouter}. At most {@link #MAX_OPTIONS} options are used. The specification remembers the last successful option between layouts; this engine lays out
 * from scratch every time, so the first option that fits wins every time.
 * <p>
 * CSS Anchor Positioning Level 1 6 절의 위치 대안(W3C 작업 초안): <code>position-try-fallbacks</code> 가 앵커 위치 상자의 대체 위치 스타일을 나열하고, 상자가 inset-modified
 * containing block 을 넘치면 레이아웃이 차례로 시도하여(<code>position-try-order</code> 는 남기는 공간으로 정렬할 수 있다) 넘치지 않는 첫 대안을 쓴다. 대안은 다음 중 하나이다.
 * <ul>
 * <li><code>@position-try</code> 규칙(<code>&lt;dashed-ident&gt;</code>): 선언이 상자의 오프셋, 마진, 크기, 자기 정렬, <code>position-anchor</code>, <code>position-area</code>
 * 값을 대체한다.</li>
 * <li>전술(<code>flip-block</code>, <code>flip-inline</code>, <code>flip-start</code>, <code>flip-x</code>, <code>flip-y</code>): 상자(또는 이름 있는 규칙)의 그 값을 축이나 대각선에 대해
 * 뒤집는다: 반대편 변의 값을 맞바꾸고(<code>top</code> 과 <code>bottom</code>, 마진, 자기 정렬의 <code>start</code> 와 <code>end</code>), <code>anchor()</code> 안의 변도 따라가며,
 * 백분율은 100% 에서 뺀 값이 되고, <code>position-area</code> 의 트랙은 뒤집히며, <code>flip-start</code> 는 블록 시작 변과 인라인 시작 변을 맞바꾼다(크기와 두 자기 정렬도 함께).</li>
 * <li><code>position-area</code> 값: 그 속성만 정한다.</li>
 * </ul>
 * 이 클래스는 속성을 읽고 대안의 스타일을 만들며 <code>position-visibility</code> 를 읽는다. 배치와 넘침 판정은 {@link AbsoluteLayouter} 에 있다. 대안은 최대
 * {@link #MAX_OPTIONS} 개를 쓴다. 규격은 마지막으로 성공한 대안을 배치 사이에 기억하지만 이 엔진은 매번 처음부터 배치하므로 매번 맞는 첫 대안이 이긴다.
 */
final class PositionTry {

    /** The largest number of options that are tried (the specification requires at least five). 시도하는 대안의 최대 수(규격은 최소 다섯 개를 요구한다). */
    static final int MAX_OPTIONS = 16;

    /** The flag of <code>position-visibility: anchors-valid</code>. <code>position-visibility: anchors-valid</code> 의 플래그. */
    static final int VISIBLE_ANCHORS_VALID = 1;

    /** The flag of <code>position-visibility: anchors-visible</code>. <code>position-visibility: anchors-visible</code> 의 플래그. */
    static final int VISIBLE_ANCHORS_VISIBLE = 2;

    /** The flag of <code>position-visibility: no-overflow</code>. <code>position-visibility: no-overflow</code> 의 플래그. */
    static final int VISIBLE_NO_OVERFLOW = 4;

    /** The physical sides in the order of the properties: top 0, right 1, bottom 2, left 3. 속성 순서의 물리 변: 위 0, 오른쪽 1, 아래 2, 왼쪽 3. */
    private static final String[] SIDE_NAMES = {"top", "right", "bottom", "left"};

    /** The inset properties by side. 변별 오프셋 속성. */
    private static final int[] INSET = {Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT};

    /** The margin properties by side. 변별 마진 속성. */
    private static final int[] MARGIN = {Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT};

    /** The sizing property pairs {horizontal, vertical}: size, minimum, maximum. 크기 속성 쌍 {가로, 세로}: 크기, 최소, 최대. */
    private static final int[][] SIZES = {{Prop.WIDTH, Prop.HEIGHT}, {Prop.MIN_WIDTH, Prop.MIN_HEIGHT}, {Prop.MAX_WIDTH, Prop.MAX_HEIGHT}};

    /** The properties that a position option can change. 위치 대안이 바꿀 수 있는 속성. */
    private static final int[] ACCEPTED = {Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT, Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT,
        Prop.WIDTH, Prop.HEIGHT, Prop.MIN_WIDTH, Prop.MIN_HEIGHT, Prop.MAX_WIDTH, Prop.MAX_HEIGHT, Prop.ALIGN_SELF, Prop.JUSTIFY_SELF, Prop.POSITION_ANCHOR, Prop.POSITION_AREA};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PositionTry() {
    }

    /**
     * One entry of <code>position-try-fallbacks</code>.
     * <p>
     * <code>position-try-fallbacks</code> 의 항목 하나.
     */
    static final class Option {
        /** The name of the <code>@position-try</code> rule, or null. <code>@position-try</code> 규칙의 이름. 없으면 null. */
        final String name;
        /** The try tactics in the order in which they are applied (lower case). 적용하는 순서의 전술(소문자). */
        final List<String> tactics;
        /** The <code>position-area</code> value of an option that consists only of that, else null. 그것만으로 된 대안의 <code>position-area</code> 값. 아니면 null. */
        final CssValue area;

        /**
         * Creates an option.
         * <p>
         * 대안을 만든다.
         *
         * @param name    the rule name or null / 규칙 이름 또는 null
         * @param tactics the tactics / 전술
         * @param area    the position area or null / 위치 영역 또는 null
         */
        Option(String name, List<String> tactics, CssValue area) {
            this.name = name;
            this.tactics = tactics;
            this.area = area;
        }
    }

    /**
     * Reads the options of <code>position-try-fallbacks</code>: the comma separated entries, each a name and/or tactics or a position area; an entry that is not
     * understood is skipped, and at most {@link #MAX_OPTIONS} are returned.
     * <p>
     * <code>position-try-fallbacks</code> 의 대안을 읽는다: 쉼표로 구분한 항목마다 이름과 전술 또는 위치 영역이다. 이해하지 못하는 항목은 건너뛰며 최대 {@link #MAX_OPTIONS} 개를 반환한다.
     *
     * @param s the style of the box / 상자의 스타일
     * @return the options in the order of the property / 속성의 순서대로의 대안
     */
    static List<Option> options(ComputedStyle s) {
        List<Option> out = new ArrayList<Option>();
        CssValue v = s == null ? null : s.get(Prop.POSITION_TRY_FALLBACKS);
        if (v == null || v.isKeyword("none")) {
            return out;
        }
        List<CssValue> items = v instanceof CssList && ((CssList) v).isComma() ? ((CssList) v).getItems() : Collections.singletonList(v);
        // 쉼표로 구분한 항목마다 대안을 하나 만든다.
        for (int i = 0; i < items.size() && out.size() < MAX_OPTIONS; i++) {
            Option o = optionOf(items.get(i));
            if (o != null) {
                out.add(o);
            }
        }
        return out;
    }

    /**
     * Reads one entry: <code>&lt;dashed-ident&gt; || &lt;try-tactic&gt;</code> or a <code>position-area</code> value.
     * <p>
     * 항목 하나를 읽는다: <code>&lt;dashed-ident&gt; || &lt;try-tactic&gt;</code> 또는 <code>position-area</code> 값.
     *
     * @param item the entry / 항목
     * @return the option or null / 대안. 없으면 null
     */
    private static Option optionOf(CssValue item) {
        List<CssValue> words = item instanceof CssList ? ((CssList) item).getItems() : Collections.singletonList(item);
        String name = null;
        List<String> tactics = new ArrayList<String>();
        boolean other = false;
        // 낱말마다 규칙 이름(--로 시작), 전술(flip-), 그 밖의 것(위치 영역 키워드)으로 나눈다.
        for (int i = 0; i < words.size(); i++) {
            if (!(words.get(i) instanceof CssKeyword)) {
                return null;
            }
            String k = ((CssKeyword) words.get(i)).getName();
            if (k.startsWith("--")) {
                name = k;
            } else if (k.toLowerCase(Locale.ENGLISH).startsWith("flip-")) {
                tactics.add(k.toLowerCase(Locale.ENGLISH));
            } else {
                other = true;
            }
        }
        if (other) {
            return name == null && tactics.isEmpty() ? new Option(null, tactics, item) : null;
        }
        return new Option(name, tactics, null);
    }

    /**
     * Returns the value of <code>position-try-order</code>: <code>normal</code>, <code>most-width</code>, <code>most-height</code>, <code>most-block-size</code> or
     * <code>most-inline-size</code>.
     * <p>
     * <code>position-try-order</code> 의 값을 반환한다: <code>normal</code>, <code>most-width</code>, <code>most-height</code>, <code>most-block-size</code>, <code>most-inline-size</code>.
     *
     * @param s the style / 스타일
     * @return the keyword in lower case / 소문자 키워드
     */
    static String order(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.POSITION_TRY_ORDER);
        return v instanceof CssKeyword ? ((CssKeyword) v).getName().toLowerCase(Locale.ENGLISH) : "normal";
    }

    /**
     * Reads <code>position-visibility</code> into flags: <code>always</code> is 0.
     * <p>
     * <code>position-visibility</code> 를 플래그로 읽는다: <code>always</code> 는 0.
     *
     * @param s the style / 스타일
     * @return the bit set of {@link #VISIBLE_ANCHORS_VALID}, {@link #VISIBLE_ANCHORS_VISIBLE} and {@link #VISIBLE_NO_OVERFLOW} / 플래그 집합
     */
    static int visibility(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.POSITION_VISIBILITY);
        if (v == null) {
            return VISIBLE_ANCHORS_VISIBLE;
        }
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : Collections.singletonList(v);
        int flags = 0;
        // 키워드마다 플래그를 켠다(always 는 아무것도 켜지 않는다).
        for (int i = 0; i < items.size(); i++) {
            String k = items.get(i) instanceof CssKeyword ? ((CssKeyword) items.get(i)).getName().toLowerCase(Locale.ENGLISH) : "";
            if (k.equals("anchors-valid")) {
                flags |= VISIBLE_ANCHORS_VALID;
            } else if (k.equals("anchors-visible")) {
                flags |= VISIBLE_ANCHORS_VISIBLE;
            } else if (k.equals("no-overflow")) {
                flags |= VISIBLE_NO_OVERFLOW;
            }
        }
        return flags;
    }

    /**
     * Builds the style of a box with a position option applied (section 6.5.2): the values of the properties that an option can change are taken from the base style,
     * replaced by the declarations of the rule, and mirrored by the tactics in order; an option that is only a <code>position-area</code> changes that property.
     * <p>
     * 위치 대안을 적용한 상자의 스타일을 만든다(6.5.2 절): 대안이 바꿀 수 있는 속성의 값을 기본 스타일에서 가져와 규칙의 선언으로 대체하고 전술로 차례로 뒤집는다.
     * <code>position-area</code> 만으로 된 대안은 그 속성만 바꾼다.
     *
     * @param o    the option / 대안
     * @param base the style of the box without options / 대안 없는 상자의 스타일
     * @param rule the computed declarations of the <code>@position-try</code> rule or null / <code>@position-try</code> 규칙의 계산된 선언. 없으면 null
     * @param wm   the writing mode of the containing block / 포함 블록의 쓰기 모드
     * @param dir  the direction of the containing block / 포함 블록의 방향
     * @return the style / 스타일
     */
    static ComputedStyle apply(Option o, ComputedStyle base, Map<Integer, CssValue> rule, String wm, String dir) {
        if (o.area != null) {
            return base.with(Prop.POSITION_AREA, o.area);
        }
        Map<Integer, CssValue> values = new LinkedHashMap<Integer, CssValue>();
        // 대안이 바꿀 수 있는 속성의 값을 기본 스타일에서 가져온다.
        for (int i = 0; i < ACCEPTED.length; i++) {
            values.put(Integer.valueOf(ACCEPTED[i]), base.get(ACCEPTED[i]));
        }
        // 규칙이 선언한 값으로 대체한다.
        if (rule != null) {
            for (Map.Entry<Integer, CssValue> e : rule.entrySet()) {
                if (values.containsKey(e.getKey()) && e.getValue() != null) {
                    values.put(e.getKey(), e.getValue());
                }
            }
        }
        // 전술을 적은 순서대로 적용한다.
        for (int i = 0; i < o.tactics.size(); i++) {
            values = tactic(values, o.tactics.get(i), wm, dir);
        }
        ComputedStyle s = base;
        // 대안의 값을 스타일에 넣는다.
        for (Map.Entry<Integer, CssValue> e : values.entrySet()) {
            if (e.getValue() != null) {
                s = s.with(e.getKey().intValue(), e.getValue());
            }
        }
        return s;
    }

    /**
     * Tells whether a writing mode has a horizontal block flow (the block axis is vertical).
     * <p>
     * 쓰기 모드의 블록 흐름이 가로쓰기인지(블록 축이 세로) 알려 준다.
     *
     * @param wm the writing mode / 쓰기 모드
     * @return true for the horizontal modes / 가로쓰기이면 true
     */
    private static boolean horizontal(String wm) {
        return wm == null || !(wm.startsWith("vertical") || wm.startsWith("sideways"));
    }

    /**
     * Returns the physical side (0 top, 1 right, 2 bottom, 3 left) that is the start of the block axis.
     * <p>
     * 블록 축의 시작인 물리 변(0 위, 1 오른쪽, 2 아래, 3 왼쪽)을 반환한다.
     *
     * @param wm the writing mode / 쓰기 모드
     * @return the side / 변
     */
    private static int blockStart(String wm) {
        if (horizontal(wm)) {
            return 0;
        }
        return wm.equals("vertical-rl") || wm.equals("sideways-rl") ? 1 : 3;
    }

    /**
     * Returns the physical side that is the start of the inline axis.
     * <p>
     * 인라인 축의 시작인 물리 변을 반환한다.
     *
     * @param wm  the writing mode / 쓰기 모드
     * @param dir the direction / 방향
     * @return the side / 변
     */
    private static int inlineStart(String wm, String dir) {
        boolean rtl = "rtl".equals(dir);
        if (horizontal(wm)) {
            return rtl ? 1 : 3;
        }
        // 세로쓰기에서 글이 위에서 아래로 흐르면(sideways-lr 은 아래에서 위로) ltr 의 인라인 시작은 위이다.
        boolean up = "sideways-lr".equals(wm);
        return (up != rtl) ? 2 : 0;
    }

    /**
     * Returns the tactic in physical terms: <code>flip-block</code> and <code>flip-inline</code> are <code>flip-y</code> or <code>flip-x</code> depending on the
     * writing mode of the containing block; the other tactics stay.
     * <p>
     * 전술을 물리 용어로 반환한다: <code>flip-block</code> 과 <code>flip-inline</code> 은 포함 블록의 쓰기 모드에 따라 <code>flip-y</code> 나 <code>flip-x</code> 이다. 다른 전술은 그대로.
     *
     * @param t  the tactic in lower case / 소문자 전술
     * @param wm the writing mode / 쓰기 모드
     * @return the tactic / 전술
     */
    static String physical(String t, String wm) {
        // 논리 전술은 블록 축과 인라인 축에 해당하는 물리 축의 전술이 된다.
        if (t.equals("flip-block")) {
            return horizontal(wm) ? "flip-y" : "flip-x";
        }
        if (t.equals("flip-inline")) {
            return horizontal(wm) ? "flip-x" : "flip-y";
        }
        return t;
    }

    /**
     * Returns the permutation of the physical sides (0 top, 1 right, 2 bottom, 3 left) of a tactic: the value of side <code>s</code> after the tactic is the value that
     * side <code>perm[s]</code> had before. <code>flip-x</code> exchanges left and right, <code>flip-y</code> top and bottom, <code>flip-start</code> the block-start
     * side with the inline-start side and the block-end side with the inline-end side.
     * <p>
     * 전술의 물리 변(0 위, 1 오른쪽, 2 아래, 3 왼쪽)의 순열을 반환한다: 전술 뒤의 변 <code>s</code> 의 값은 전술 전에 변 <code>perm[s]</code> 가 가진 값이다. <code>flip-x</code> 는 왼쪽과
     * 오른쪽, <code>flip-y</code> 는 위와 아래, <code>flip-start</code> 는 블록 시작 변과 인라인 시작 변, 블록 끝 변과 인라인 끝 변을 맞바꾼다.
     *
     * @param kind the physical tactic (see {@link #physical}) / 물리 전술({@link #physical} 참조)
     * @param wm   the writing mode of the containing block / 포함 블록의 쓰기 모드
     * @param dir  the direction of the containing block / 포함 블록의 방향
     * @return the permutation, or null for a tactic that is not known / 순열. 모르는 전술이면 null
     */
    static int[] permutation(String kind, String wm, String dir) {
        int[] perm = {0, 1, 2, 3};
        if (kind.equals("flip-x")) {
            perm[1] = 3;
            perm[3] = 1;
        } else if (kind.equals("flip-y")) {
            perm[0] = 2;
            perm[2] = 0;
        } else if (kind.equals("flip-start")) {
            // 블록 시작 변과 인라인 시작 변을 맞바꾸고 블록 끝 변과 인라인 끝 변도 맞바꾼다(대각선에 대한 뒤집기).
            int b = blockStart(wm);
            int i = inlineStart(wm, dir);
            perm[b] = i;
            perm[i] = b;
            perm[(b + 2) & 3] = (i + 2) & 3;
            perm[(i + 2) & 3] = (b + 2) & 3;
        } else {
            return null;
        }
        return perm;
    }

    /**
     * Mirrors the values of the properties by one tactic (section 6.5.2).
     * <p>
     * 전술 하나로 속성의 값을 뒤집는다(6.5.2 절).
     *
     * @param v  the values by property id / 속성 id 별 값
     * @param t  the tactic / 전술
     * @param wm the writing mode / 쓰기 모드
     * @param dir the direction / 방향
     * @return the new values / 새 값
     */
    private static Map<Integer, CssValue> tactic(Map<Integer, CssValue> v, String t, String wm, String dir) {
        String kind = physical(t, wm);
        int[] perm = permutation(kind, wm, dir);
        if (perm == null) {
            return v;
        }
        boolean swapAxes = kind.equals("flip-start");
        boolean flipX = kind.equals("flip-x");
        boolean flipY = kind.equals("flip-y");
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>(v);
        // 변마다 반대편(또는 대각선 맞은편)의 값을 가져오며 값 안의 변도 따라 바꾼다.
        for (int s = 0; s < 4; s++) {
            int os = perm[s];
            boolean opposing = os == ((s + 2) & 3);
            out.put(Integer.valueOf(INSET[s]), convert(v.get(Integer.valueOf(INSET[os])), perm, opposing, swapAxes));
            out.put(Integer.valueOf(MARGIN[s]), convert(v.get(Integer.valueOf(MARGIN[os])), perm, opposing, swapAxes));
        }
        // 크기 속성은 축이 바뀔 때만 가로와 세로의 값을 맞바꾼다.
        for (int k = 0; k < SIZES.length; k++) {
            int h = SIZES[k][0];
            int vt = SIZES[k][1];
            if (swapAxes) {
                out.put(Integer.valueOf(h), convert(v.get(Integer.valueOf(vt)), perm, false, true));
                out.put(Integer.valueOf(vt), convert(v.get(Integer.valueOf(h)), perm, false, true));
            } else {
                out.put(Integer.valueOf(h), convert(v.get(Integer.valueOf(h)), perm, false, false));
                out.put(Integer.valueOf(vt), convert(v.get(Integer.valueOf(vt)), perm, false, false));
            }
        }
        // 자기 정렬: 축이 바뀌면 두 값을 맞바꾸고, 같은 축을 뒤집으면 start 와 end 를 맞바꾼다.
        CssValue align = v.get(Integer.valueOf(Prop.ALIGN_SELF));
        CssValue justify = v.get(Integer.valueOf(Prop.JUSTIFY_SELF));
        if (swapAxes) {
            out.put(Integer.valueOf(Prop.ALIGN_SELF), justify);
            out.put(Integer.valueOf(Prop.JUSTIFY_SELF), align);
        } else {
            boolean h = horizontal(wm);
            // 블록 축(align-self)과 인라인 축(justify-self)이 어느 물리 축인지에 따라 뒤집을지 정한다.
            boolean alignReversed = h ? flipY : flipX;
            boolean justifyReversed = h ? flipX : flipY;
            out.put(Integer.valueOf(Prop.ALIGN_SELF), reverseAlignment(align, alignReversed, false));
            out.put(Integer.valueOf(Prop.JUSTIFY_SELF), reverseAlignment(justify, justifyReversed, flipX));
        }
        out.put(Integer.valueOf(Prop.POSITION_AREA), flipArea(v.get(Integer.valueOf(Prop.POSITION_AREA)), perm));
        return out;
    }

    /**
     * Rewrites the sides and the axes that a value names for the move of the value from one side to another (section 6.5.2): the sides in <code>anchor()</code> follow
     * the permutation of the sides, <code>start</code> and <code>end</code> and a percentage change when the value moves to the opposite side, the axes in
     * <code>anchor-size()</code> change when the axes are exchanged.
     * <p>
     * 값이 한 변에서 다른 변으로 옮겨질 때 값이 가리키는 변과 축을 고쳐 쓴다(6.5.2 절): <code>anchor()</code> 안의 변은 변의 순열을 따르고, 값이 반대편 변으로 옮겨지면
     * <code>start</code>/<code>end</code> 와 백분율이 바뀌며, 축을 맞바꾸면 <code>anchor-size()</code> 안의 축이 바뀐다.
     *
     * @param value    the value / 값
     * @param perm     the permutation of the sides / 변의 순열
     * @param opposing true if the value moves to the opposite side of the same axis / 값이 같은 축의 반대편 변으로 옮겨지면 true
     * @param swap     true if the axes are exchanged / 축을 맞바꾸면 true
     * @return the new value / 새 값
     */
    private static CssValue convert(CssValue value, final int[] perm, final boolean opposing, final boolean swap) {
        if (value instanceof CssFunctionValue) {
            return convertFunction((CssFunctionValue) value, perm, opposing, swap);
        }
        if (value instanceof CssCalc && ((CssCalc) value).hasAnchor()) {
            return ((CssCalc) value).mapAnchors(new CssCalc.FunctionMapper() {
                /** Rewrites one function of the expression. 식 안의 함수 하나를 고쳐 쓴다. */
                public CssFunctionValue map(CssFunctionValue fn) {
                    return convertFunction(fn, perm, opposing, swap);
                }
            });
        }
        return value;
    }

    /**
     * Rewrites an <code>anchor()</code> or <code>anchor-size()</code> function (see {@link #convert}).
     * <p>
     * <code>anchor()</code> 나 <code>anchor-size()</code> 함수를 고쳐 쓴다({@link #convert} 참조).
     *
     * @param fn       the function / 함수
     * @param perm     the permutation of the sides / 변의 순열
     * @param opposing true for a move to the opposite side / 반대편 변으로 옮겨지면 true
     * @param swap     true if the axes are exchanged / 축을 맞바꾸면 true
     * @return the new function (the same if it is neither of them) / 새 함수(둘 다 아니면 같은 것)
     */
    private static CssFunctionValue convertFunction(CssFunctionValue fn, int[] perm, boolean opposing, boolean swap) {
        boolean anchor = fn.isNamed("anchor");
        boolean size = fn.isNamed("anchor-size");
        if ((!anchor && !size) || fn.getArguments().isEmpty()) {
            return fn;
        }
        List<CssValue> args = new ArrayList<CssValue>(fn.getArguments());
        CssValue head = args.get(0);
        if (head instanceof CssList && !((CssList) head).isComma()) {
            List<CssValue> items = new ArrayList<CssValue>();
            // 첫 인수의 낱말(앵커 이름, 변, 크기 키워드, 백분율)을 하나씩 고쳐 쓴다.
            for (CssValue w : ((CssList) head).getItems()) {
                items.add(convertWord(w, anchor, perm, opposing, swap));
            }
            args.set(0, new CssList(items, false));
        } else {
            args.set(0, convertWord(head, anchor, perm, opposing, swap));
        }
        return new CssFunctionValue(fn.getName(), args);
    }

    /**
     * Rewrites one word of the first argument of an anchor function.
     * <p>
     * anchor 함수의 첫 인수의 낱말 하나를 고쳐 쓴다.
     *
     * @param w        the word / 낱말
     * @param anchor   true for <code>anchor()</code>, false for <code>anchor-size()</code> / <code>anchor()</code> 이면 true, <code>anchor-size()</code> 이면 false
     * @param perm     the permutation of the sides / 변의 순열
     * @param opposing true for a move to the opposite side / 반대편 변으로 옮겨지면 true
     * @param swap     true if the axes are exchanged / 축을 맞바꾸면 true
     * @return the new word / 새 낱말
     */
    private static CssValue convertWord(CssValue w, boolean anchor, int[] perm, boolean opposing, boolean swap) {
        if (w instanceof CssPercentage) {
            return anchor && opposing ? new CssPercentage(100 - ((CssPercentage) w).getValue()) : w;
        }
        if (!(w instanceof CssKeyword)) {
            return w;
        }
        String k = ((CssKeyword) w).getName();
        if (k.startsWith("--")) {
            return w;
        }
        String l = k.toLowerCase(Locale.ENGLISH);
        if (anchor) {
            // 물리 변은 순열을 따르고, 논리 변 start/end 는 반대편으로 옮겨질 때 맞바뀐다.
            for (int s = 0; s < 4; s++) {
                if (l.equals(SIDE_NAMES[s])) {
                    return CssKeyword.of(SIDE_NAMES[perm[s]]);
                }
            }
            if (opposing) {
                return CssKeyword.of(swapStartEnd(l));
            }
            return w;
        }
        if (swap) {
            if (l.equals("width")) {
                return CssKeyword.of("height");
            }
            if (l.equals("height")) {
                return CssKeyword.of("width");
            }
            if (l.equals("inline")) {
                return CssKeyword.of("block");
            }
            if (l.equals("block")) {
                return CssKeyword.of("inline");
            }
            if (l.equals("self-inline")) {
                return CssKeyword.of("self-block");
            }
            if (l.equals("self-block")) {
                return CssKeyword.of("self-inline");
            }
        }
        return w;
    }

    /**
     * Exchanges <code>start</code> and <code>end</code> (also with <code>self-</code> and <code>flex-</code>) in a keyword.
     * <p>
     * 키워드의 <code>start</code> 와 <code>end</code>(<code>self-</code>, <code>flex-</code> 가 붙은 것도)를 맞바꾼다.
     *
     * @param k the keyword in lower case / 소문자 키워드
     * @return the new keyword (the same if it has neither) / 새 키워드(둘 다 없으면 같은 것)
     */
    private static String swapStartEnd(String k) {
        if (k.equals("start") || k.equals("self-start") || k.equals("flex-start")) {
            return k.substring(0, k.length() - 5) + "end";
        }
        if (k.equals("end") || k.equals("self-end") || k.equals("flex-end")) {
            return k.substring(0, k.length() - 3) + "start";
        }
        return k;
    }

    /**
     * Mirrors a self-alignment value along its axis: the positions <code>start</code> and <code>end</code> are exchanged (and <code>left</code> and <code>right</code> if
     * the horizontal axis is mirrored); <code>center</code>, <code>anchor-center</code>, the baselines and <code>normal</code> stay.
     * <p>
     * 자기 정렬 값을 그 축에서 뒤집는다: 위치 <code>start</code> 와 <code>end</code> 를 맞바꾸고(가로 축을 뒤집으면 <code>left</code> 와 <code>right</code> 도),
     * <code>center</code>, <code>anchor-center</code>, 기준선, <code>normal</code> 은 그대로이다.
     *
     * @param v        the value / 값
     * @param reversed true if the logical axis of the property is mirrored / 속성의 논리 축을 뒤집으면 true
     * @param leftRight true if <code>left</code> and <code>right</code> are exchanged / <code>left</code> 와 <code>right</code> 를 맞바꾸면 true
     * @return the new value / 새 값
     */
    private static CssValue reverseAlignment(CssValue v, boolean reversed, boolean leftRight) {
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName().toLowerCase(Locale.ENGLISH);
            if (reversed && !swapStartEnd(k).equals(k)) {
                return CssKeyword.of(swapStartEnd(k));
            }
            if (leftRight && k.equals("left")) {
                return CssKeyword.of("right");
            }
            if (leftRight && k.equals("right")) {
                return CssKeyword.of("left");
            }
            return v;
        }
        if (v instanceof CssList) {
            List<CssValue> items = new ArrayList<CssValue>();
            // safe/unsafe 가 붙은 값은 낱말마다 뒤집는다.
            for (CssValue w : ((CssList) v).getItems()) {
                items.add(reverseAlignment(w, reversed, leftRight));
            }
            return new CssList(items, ((CssList) v).isComma());
        }
        return v;
    }

    /**
     * Mirrors the tracks of a <code>position-area</code> value by a permutation of the sides: the tracks at the sides move to the tracks of the sides that the
     * permutation gives (the row of the top side becomes the column of the left side for the exchange of the axes), the middle track stays in the middle. The result is
     * written with the physical keywords <code>top</code>, <code>center</code>, <code>bottom</code>, <code>span-top</code>, <code>span-bottom</code>,
     * <code>left</code>, <code>right</code>, <code>span-left</code>, <code>span-right</code> and <code>span-all</code>, the row first.
     * <p>
     * <code>position-area</code> 값의 트랙을 변의 순열로 뒤집는다: 변에 있는 트랙은 순열이 주는 변의 트랙으로 옮겨지고(축을 맞바꾸면 위쪽 변의 행이 왼쪽 변의 열이 된다) 가운데
     * 트랙은 가운데에 남는다. 결과는 행을 앞에 두고 물리 키워드 <code>top</code>, <code>center</code>, <code>bottom</code>, <code>span-top</code>, <code>span-bottom</code>,
     * <code>left</code>, <code>right</code>, <code>span-left</code>, <code>span-right</code>, <code>span-all</code> 로 쓴다.
     *
     * @param area the value (<code>none</code> and what is not understood stay) / 값(<code>none</code> 과 이해하지 못하는 값은 그대로)
     * @param perm the permutation of the sides / 변의 순열
     * @return the mirrored value / 뒤집은 값
     */
    static CssValue flipArea(CssValue area, int[] perm) {
        int[] t = area == null || area.isKeyword("none") ? null : AnchorPositioning.tracksOf(area);
        if (t == null) {
            return area;
        }
        // 가로 축(열)의 시작 변은 왼쪽(3), 끝 변은 오른쪽(1). 세로 축(행)의 시작 변은 위(0), 끝 변은 아래(2).
        int[] cols = mapRange(t[0], t[1], 3, 1, perm);
        int[] rows = mapRange(t[2], t[3], 0, 2, perm);
        // 새 열 범위는 가로 축으로 옮겨진 것, 새 행 범위는 세로 축으로 옮겨진 것이다(축을 맞바꾸면 서로 바뀐다).
        boolean swapped = perm[3] == 0 || perm[3] == 2;
        int[] newCols = swapped ? rows : cols;
        int[] newRows = swapped ? cols : rows;
        List<CssValue> words = new ArrayList<CssValue>();
        words.add(CssKeyword.of(trackWord(newRows[0], newRows[1], true)));
        words.add(CssKeyword.of(trackWord(newCols[0], newCols[1], false)));
        return new CssList(words, false);
    }

    /**
     * Maps a range of tracks of one axis through a permutation of the sides: an end of the range at the start or the end of the axis goes to the track of the side that
     * the permutation gives, the middle track stays.
     * <p>
     * 한 축의 트랙 범위를 변의 순열로 옮긴다: 범위의 끝이 축의 시작이나 끝에 있으면 순열이 주는 변의 트랙으로 가고 가운데 트랙은 그대로이다.
     *
     * @param first      the first track 0 to 2 / 처음 트랙 0~2
     * @param last       the last track / 마지막 트랙
     * @param startSide  the side at the start of the axis / 축의 시작 변
     * @param endSide    the side at the end of the axis / 축의 끝 변
     * @param perm       the permutation / 순열
     * @return {first, last} of the new range in the track numbers of its new axis / 새 축의 트랙 번호로 나타낸 새 범위 {처음, 마지막}
     */
    private static int[] mapRange(int first, int last, int startSide, int endSide, int[] perm) {
        int a = mapTrack(first, startSide, endSide, perm);
        int b = mapTrack(last, startSide, endSide, perm);
        return new int[] {Math.min(a, b), Math.max(a, b)};
    }

    /**
     * Maps one track: the start track and the end track follow the permutation of their sides, the middle track stays.
     * <p>
     * 트랙 하나를 옮긴다: 시작 트랙과 끝 트랙은 그 변의 순열을 따르고 가운데 트랙은 그대로이다.
     *
     * @param track     the track 0 to 2 / 트랙 0~2
     * @param startSide the side at the start of the axis / 축의 시작 변
     * @param endSide   the side at the end of the axis / 축의 끝 변
     * @param perm      the permutation / 순열
     * @return the track in the new axis / 새 축의 트랙
     */
    private static int mapTrack(int track, int startSide, int endSide, int[] perm) {
        if (track == 1) {
            return 1;
        }
        int ns = perm[track == 0 ? startSide : endSide];
        // 새 변이 위나 왼쪽이면 시작 트랙이고 아래나 오른쪽이면 끝 트랙이다.
        return ns == 0 || ns == 3 ? 0 : 2;
    }

    /**
     * Returns the keyword that names a range of tracks of an axis.
     * <p>
     * 한 축의 트랙 범위를 가리키는 키워드를 반환한다.
     *
     * @param first the first track / 처음 트랙
     * @param last  the last track / 마지막 트랙
     * @param rows  true for the vertical axis (rows), false for the horizontal one / 세로 축(행)이면 true, 가로 축이면 false
     * @return the keyword / 키워드
     */
    private static String trackWord(int first, int last, boolean rows) {
        if (first == 0 && last == 2) {
            return "span-all";
        }
        if (first == 1 && last == 1) {
            return "center";
        }
        if (first == last) {
            return first == 0 ? (rows ? "top" : "left") : (rows ? "bottom" : "right");
        }
        return first == 0 ? (rows ? "span-top" : "span-left") : (rows ? "span-bottom" : "span-right");
    }
}
