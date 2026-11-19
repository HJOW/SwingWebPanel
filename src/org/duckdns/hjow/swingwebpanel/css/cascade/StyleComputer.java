package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * The computation of values: it turns the cascaded values of an element (the winning declarations, already expanded to longhands) into the computed
 * style. It applies <code>inherit</code>, <code>initial</code> and <code>unset</code>, inherits the inherited properties from the parent, substitutes
 * <code>var()</code>, resolves relative lengths (<code>em</code>, <code>rem</code>, viewport units) to pixels, computes the font size first because other
 * lengths depend on it, and applies the computed-value rules of the individual properties (font keywords, border widths, display adjustments, ...).
 * <p>
 * 값의 계산: 요소의 캐스케이드 값(이미 개별 속성으로 전개된 승리한 선언)을 계산 스타일로 바꾼다. <code>inherit</code>, <code>initial</code>, <code>unset</code> 을 적용하고,
 * 상속 속성을 부모에서 물려받고, <code>var()</code> 를 치환하고, 상대 길이(<code>em</code>, <code>rem</code>, 뷰포트 단위)를 픽셀로 바꾸고, 다른 길이가 의존하므로 글꼴 크기를
 * 먼저 계산하며, 개별 속성의 계산 값 규칙(글꼴 키워드, 테두리 두께, display 조정 등)을 적용한다.
 */
final class StyleComputer {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StyleComputer() {
    }

    /**
     * Computes the style of an element.
     * <p>
     * 요소의 스타일을 계산한다.
     *
     * @param cascaded       the winning specified value of each longhand by id, null where nothing was declared / 개별 속성별 승리한 지정 값(id 로 접근). 선언이 없으면 null
     * @param customDeclared the winning custom property declarations / 승리한 사용자 정의 속성 선언
     * @param parent         the parent's computed style or null for the root / 부모의 계산 스타일. 루트이면 null
     * @param base           viewport size and default font sizes / 뷰포트 크기와 기본 글꼴 크기
     * @param root           true for the root element / 루트 요소이면 true
     * @return the computed style / 계산 스타일
     */
    static ComputedStyle compute(CssValue[] cascaded, Map<String, List<ComponentValue>> customDeclared, ComputedStyle parent, LengthContext base,
            boolean root) {
        return compute(cascaded, customDeclared, parent, base, root, null);
    }

    /**
     * Computes the style of an element with registered custom properties (<code>@property</code>): the values of the registered properties are computed
     * against their syntax with the font size of the element, which is found by a first computation without them (a font size that depends on a
     * registered property is therefore computed from the untyped value), and the declarations that use them with <code>var()</code> see the computed
     * values.
     * <p>
     * 등록된 사용자 정의 속성(<code>@property</code>)이 있는 요소의 스타일을 계산한다: 등록된 속성의 값은 문법에 맞춰 요소의 글꼴 크기로 계산하는데, 글꼴 크기는 그것들 없이
     * 한 번 계산해서 구한다(그래서 등록된 속성에 의존하는 글꼴 크기는 형 없는 값으로 계산된다). <code>var()</code> 로 그것을 쓰는 선언은 계산된 값을 본다.
     *
     * @param cascaded       the winning specified value of each longhand by id / 개별 속성별 승리한 지정 값
     * @param customDeclared the winning custom property declarations / 승리한 사용자 정의 속성 선언
     * @param parent         the parent's computed style or null / 부모의 계산 스타일. 없으면 null
     * @param base           viewport size and default font sizes / 뷰포트 크기와 기본 글꼴 크기
     * @param root           true for the root element / 루트 요소이면 true
     * @param registered     the registered custom properties by name, or null / 이름별 등록된 사용자 정의 속성. 없으면 null
     * @return the computed style / 계산 스타일
     */
    static ComputedStyle compute(CssValue[] cascaded, Map<String, List<ComponentValue>> customDeclared, ComputedStyle parent, LengthContext base,
            boolean root, Map<String, RegisteredProperty> registered) {
        Map<String, CssRaw> inheritedCustom = parent == null ? new LinkedHashMap<String, CssRaw>() : parent.customProperties();
        Map<String, CssRaw> custom = VarResolver.resolveCustom(inheritedCustom, customDeclared);
        if (registered != null && !registered.isEmpty()) {
            ComputedStyle trial = computeCore(cascaded, customDeclared, parent, base, root, custom);
            double fs = trial.fontSizePx();
            LengthContext lc = new LengthContext(fs, trial.rootFontSizePx(), fs * 1.2, base.getViewportWidth(), base.getViewportHeight()).withPrefersDark(base.isPrefersDark()).withContainer(base.getContainerWidth(), base.getContainerHeight());
            custom = RegisteredProperty.apply(custom, inheritedCustom, customDeclared, registered, lc);
        }
        return computeCore(cascaded, customDeclared, parent, base, root, custom);
    }

    /**
     * Computes the style of an element from the resolved custom properties.
     * <p>
     * 해석한 사용자 정의 속성으로 요소의 스타일을 계산한다.
     *
     * @param cascaded       the winning specified value of each longhand by id / 개별 속성별 승리한 지정 값
     * @param customDeclared the winning custom property declarations / 승리한 사용자 정의 속성 선언
     * @param parent         the parent's computed style or null / 부모의 계산 스타일. 없으면 null
     * @param base           viewport size and default font sizes / 뷰포트 크기와 기본 글꼴 크기
     * @param root           true for the root element / 루트 요소이면 true
     * @param custom         the custom properties of the element / 요소의 사용자 정의 속성
     * @return the computed style / 계산 스타일
     */
    private static ComputedStyle computeCore(CssValue[] cascaded, Map<String, List<ComponentValue>> customDeclared, ComputedStyle parent,
            LengthContext base, boolean root, Map<String, CssRaw> custom) {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        CssValue[] computed = new CssValue[Prop.COUNT + 1];
        CssValue[] spec = new CssValue[Prop.COUNT + 1];
        IdentityHashMap<CssPending, Map<Integer, CssValue>> cache = new IdentityHashMap<CssPending, Map<Integer, CssValue>>();
        // 논리 속성의 var() 값을 물리 속성으로 옮기려면 요소의 쓰기 모드와 방향이 필요하다.
        String[] modes = modes(cascaded[Prop.WRITING_MODE], cascaded[Prop.DIRECTION], parent);
        // 모든 개별 속성의 지정 값을 정한다: 치환, inherit/initial/unset 적용, 상속.
        for (int id = 1; id <= Prop.COUNT; id++) {
            Property p = reg.byId(id);
            CssValue raw = cascaded[id];
            if (raw instanceof CssPending) {
                raw = VarResolver.resolvePending((CssPending) raw, id, custom, reg, cache, modes);
            }
            if (raw == null || raw.isKeyword("unset") || raw.isKeyword("revert") || raw.isKeyword("revert-layer")) {
                raw = p.isInherited() ? CssKeyword.INHERIT : CssKeyword.INITIAL;
            }
            if (id == Prop.TEXT_ALIGN && raw.isKeyword("-internal-center")) {
                // th 의 UA 규칙(HTML5 2014 10.3.9): 부모의 text-align 이 초기값(start)일 때만 가운데이고 아니면 부모의 값을 상속한다.
                if (parent != null && !parent.isKeyword(Prop.TEXT_ALIGN, "start")) {
                    computed[id] = parent.get(id);
                } else {
                    spec[id] = new CssKeyword("center");
                }
            } else if (raw.isKeyword("inherit")) {
                if (parent != null) {
                    computed[id] = parent.get(id);
                } else {
                    spec[id] = p.getInitial();
                }
            } else if (raw.isKeyword("initial")) {
                spec[id] = p.getInitial();
            } else {
                spec[id] = raw;
            }
        }
        double parentFs = parent != null ? parent.fontSizePx() : LengthContext.DEFAULT_FONT_SIZE;
        double rootFs = parent != null ? parent.rootFontSizePx() : LengthContext.DEFAULT_FONT_SIZE;
        if (computed[Prop.FONT_SIZE] == null) {
            computed[Prop.FONT_SIZE] = computeFontSize(spec[Prop.FONT_SIZE], parentFs, root ? LengthContext.DEFAULT_FONT_SIZE : rootFs, base);
        }
        double fs = ((CssLength) computed[Prop.FONT_SIZE]).getValue();
        if (root) {
            rootFs = fs;
        }
        // 요소가 쓰는 색 구성표(color-scheme)를 먼저 정해 시스템 색과 light-dark() 를 풀 때 쓴다(CSS Color Adjust 1).
        CssValue schemeValue = computed[Prop.COLOR_SCHEME] != null ? computed[Prop.COLOR_SCHEME] : spec[Prop.COLOR_SCHEME];
        boolean dark = usesDark(schemeValue, base.isPrefersDark());
        // 어두운 구성표의 루트 요소는 선언이 없으면 글자색이 CanvasText(밝은 색)이다.
        if (root && dark && cascaded[Prop.COLOR] == null && computed[Prop.COLOR] == null) {
            spec[Prop.COLOR] = CssColor.parse("canvastext");
        }
        LengthContext lc = new LengthContext(fs, rootFs, fs * 1.2, base.getViewportWidth(), base.getViewportHeight()).withPrefersDark(base.isPrefersDark())
                .withDark(dark).withContainer(base.getContainerWidth(), base.getContainerHeight());
        if (computed[Prop.LINE_HEIGHT] == null) {
            computed[Prop.LINE_HEIGHT] = computeLineHeight(spec[Prop.LINE_HEIGHT], lc);
        }
        lc = lc.withLineHeight(lineHeightPx(computed[Prop.LINE_HEIGHT], fs));
        // 나머지 속성의 계산 값을 구한다.
        for (int id = 1; id <= Prop.COUNT; id++) {
            if (computed[id] == null) {
                computed[id] = computeOne(id, spec[id], lc, parent, fs);
            }
        }
        adjust(computed, root);
        return new ComputedStyle(computed, custom, rootFs, dark);
    }

    /**
     * Tells whether an element uses the dark color scheme (CSS Color Adjustment Module Level 1, <code>color-scheme</code>): the list names
     * <code>dark</code> and not <code>light</code>, or names both and the user agent prefers dark; <code>normal</code> and everything else is the light
     * scheme.
     * <p>
     * 요소가 어두운 색 구성표를 쓰는지 알려 준다(CSS Color Adjustment Module Level 1, <code>color-scheme</code>): 목록에 <code>dark</code> 가 있고 <code>light</code> 가 없거나,
     * 둘 다 있고 사용자 에이전트가 어둡게를 선호하는 경우. <code>normal</code> 과 그 밖의 모든 것은 밝은 구성표이다.
     *
     * @param v           the specified or computed value of <code>color-scheme</code>, may be null / <code>color-scheme</code> 의 지정 값이나 계산 값. null 가능
     * @param prefersDark true if the user agent prefers a dark scheme / 사용자 에이전트가 어두운 구성표를 선호하면 true
     * @return true for the dark scheme / 어두운 구성표이면 true
     */
    static boolean usesDark(CssValue v, boolean prefersDark) {
        if (v == null) {
            return false;
        }
        // 키워드 하나(normal, light, dark)는 문자열로 풀지 않고 바로 판정한다(거의 모든 요소가 이 경우이다).
        if (v instanceof CssKeyword) {
            return "dark".equals(((CssKeyword) v).getName());
        }
        String[] t = v.toCssText().toLowerCase().split("\\s+");
        boolean dark = false;
        boolean light = false;
        // 목록에서 light 와 dark 키워드를 찾는다.
        for (int i = 0; i < t.length; i++) {
            dark |= t[i].equals("dark");
            light |= t[i].equals("light");
        }
        return dark && (!light || prefersDark);
    }

    /**
     * Returns the effective <code>writing-mode</code> and <code>direction</code> keywords of an element from their winning declarations, which
     * the mapping of the logical properties to the physical ones needs (CSS Logical Properties 1 section 2): an explicit keyword is used,
     * <code>initial</code> is the initial value, and an absent, inherited or unresolved (<code>var()</code>) value is the value of the parent.
     * <p>
     * 논리 속성을 물리 속성으로 옮기는 데 필요한, 요소의 <code>writing-mode</code> 와 <code>direction</code> 키워드를 승리한 선언에서 구한다(CSS Logical Properties 1 2 절):
     * 명시한 키워드는 그대로, <code>initial</code> 은 초기값, 선언이 없거나 상속하거나 풀지 못한(<code>var()</code>) 값은 부모의 값이다.
     *
     * @param wm     the winning <code>writing-mode</code> declaration or null / 승리한 <code>writing-mode</code> 선언. 없으면 null
     * @param dir    the winning <code>direction</code> declaration or null / 승리한 <code>direction</code> 선언. 없으면 null
     * @param parent the parent's style or null / 부모의 스타일. 없으면 null
     * @return {writing-mode, direction} / {writing-mode, direction}
     */
    static String[] modes(CssValue wm, CssValue dir, ComputedStyle parent) {
        return new String[] {mode(wm, parent, Prop.WRITING_MODE, "horizontal-tb"), mode(dir, parent, Prop.DIRECTION, "ltr")};
    }

    /**
     * Returns the effective keyword of one of the two properties of {@link #modes}.
     * <p>
     * {@link #modes} 가 다루는 두 속성 중 하나의 실제 키워드를 반환한다.
     *
     * @param v       the winning declaration or null / 승리한 선언. 없으면 null
     * @param parent  the parent's style or null / 부모의 스타일. 없으면 null
     * @param id      the property id / 속성 id
     * @param initial the initial keyword / 초기 키워드
     * @return the keyword / 키워드
     */
    private static String mode(CssValue v, ComputedStyle parent, int id, String initial) {
        if (v instanceof CssKeyword && !((CssKeyword) v).isCssWide()) {
            return ((CssKeyword) v).getName();
        }
        if (v != null && v.isKeyword("initial")) {
            return initial;
        }
        String inherited = parent == null ? null : parent.keyword(id);
        return inherited == null ? initial : inherited;
    }

    /**
     * Returns the line height in pixels for the length context of lh units.
     * <p>
     * lh 단위의 길이 컨텍스트에 쓸 줄 높이를 픽셀로 반환한다.
     *
     * @param lh the computed line height / 계산된 줄 높이
     * @param fs the font size / 글꼴 크기
     * @return pixels / 픽셀
     */
    private static double lineHeightPx(CssValue lh, double fs) {
        if (lh instanceof CssNumber) {
            return ((CssNumber) lh).getValue() * fs;
        }
        return lh instanceof CssLength ? ((CssLength) lh).getValue() : fs * 1.2;
    }

    /**
     * Computes the font size.
     * <p>
     * 글꼴 크기를 계산한다.
     *
     * @param v        the specified value / 지정 값
     * @param parentFs the parent's font size / 부모의 글꼴 크기
     * @param rootFs   the root font size for rem / rem 용 루트 글꼴 크기
     * @param base     viewport and defaults / 뷰포트와 기본값
     * @return the font size as a pixel length / 픽셀 길이로 나타낸 글꼴 크기
     */
    private static CssValue computeFontSize(CssValue v, double parentFs, double rootFs, LengthContext base) {
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            String[] names = {"xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large", "xxx-large"};
            double[] sizes = {9, 10, 13, 16, 18, 24, 32, 48};
            // 절대 크기 키워드를 표에서 찾는다.
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(k)) {
                    return CssLength.px(sizes[i]);
                }
            }
            if (k.equals("larger")) {
                return CssLength.px(parentFs * 1.2);
            }
            if (k.equals("smaller")) {
                return CssLength.px(parentFs / 1.2);
            }
            return CssLength.px(parentFs);
        }
        LengthContext lc = new LengthContext(parentFs, rootFs, parentFs * 1.2, base.getViewportWidth(), base.getViewportHeight())
                .withContainer(base.getContainerWidth(), base.getContainerHeight());
        if (v instanceof CssPercentage) {
            return CssLength.px(parentFs * ((CssPercentage) v).getValue() / 100.0);
        }
        if (v instanceof CssCalc) {
            return CssLength.px(Math.max(0, ((CssCalc) v).resolve(lc, parentFs)));
        }
        if (v instanceof CssLength) {
            double px = ((CssLength) v).toPx(lc);
            return CssLength.px(Double.isNaN(px) ? parentFs : px);
        }
        return CssLength.px(parentFs);
    }

    /**
     * Computes the line height: numbers and <code>normal</code> stay, lengths and percentages become pixels.
     * <p>
     * 줄 높이를 계산한다: 숫자와 <code>normal</code> 은 그대로이고 길이와 백분율은 픽셀이 된다.
     *
     * @param v  the specified value / 지정 값
     * @param lc the length context with the element's font size / 요소의 글꼴 크기가 든 길이 컨텍스트
     * @return the computed value / 계산 값
     */
    private static CssValue computeLineHeight(CssValue v, LengthContext lc) {
        if (v instanceof CssPercentage) {
            return CssLength.px(lc.getFontSize() * ((CssPercentage) v).getValue() / 100.0);
        }
        if (v instanceof CssCalc) {
            return CssLength.px(Math.max(0, ((CssCalc) v).resolve(lc, lc.getFontSize())));
        }
        return map(v, lc);
    }

    /**
     * Computes one property.
     * <p>
     * 속성 하나를 계산한다.
     *
     * @param id     the property id / 속성 id
     * @param v      the specified value / 지정 값
     * @param lc     the length context of the element / 요소의 길이 컨텍스트
     * @param parent the parent's style or null / 부모의 스타일 또는 null
     * @param fs     the element's font size / 요소의 글꼴 크기
     * @return the computed value / 계산 값
     */
    private static CssValue computeOne(int id, CssValue v, LengthContext lc, ComputedStyle parent, double fs) {
        switch (id) {
        case Prop.FONT_WEIGHT:
            return fontWeight(v, parent == null ? 400 : parent.fontWeight());
        case Prop.BORDER_TOP_WIDTH:
        case Prop.BORDER_RIGHT_WIDTH:
        case Prop.BORDER_BOTTOM_WIDTH:
        case Prop.BORDER_LEFT_WIDTH:
        case Prop.OUTLINE_WIDTH:
        case Prop.COLUMN_RULE_WIDTH:
            return lineWidth(v, lc);
        case Prop.COLOR:
            if (v.isKeyword("currentcolor")) {
                return parent != null ? parent.get(Prop.COLOR) : CssColor.BLACK.plain();
            }
            return map(v, lc);
        case Prop.SHAPE_IMAGE_THRESHOLD:
        case Prop.OPACITY: {
            double o = v instanceof CssPercentage ? ((CssPercentage) v).getValue() / 100.0 : v instanceof CssNumber ? ((CssNumber) v).getValue() : 1.0;
            if (v instanceof CssCalc) {
                CssValue s = ((CssCalc) v).simplify(lc);
                o = s instanceof CssNumber ? ((CssNumber) s).getValue() : 1.0;
            }
            return new CssNumber(Math.max(0, Math.min(1, o)), false);
        }
        default:
            return map(v, lc);
        }
    }

    /**
     * Computes <code>font-weight</code>: keywords become numbers, <code>bolder</code> and <code>lighter</code> depend on the parent weight.
     * <p>
     * <code>font-weight</code> 를 계산한다: 키워드는 숫자가 되고 <code>bolder</code> 와 <code>lighter</code> 는 부모의 두께에 따라 정해진다.
     *
     * @param v      the specified value / 지정 값
     * @param parent the parent's weight / 부모의 두께
     * @return a number / 숫자
     */
    private static CssValue fontWeight(CssValue v, int parent) {
        if (v instanceof CssNumber) {
            return v;
        }
        String k = v instanceof CssKeyword ? ((CssKeyword) v).getName() : "normal";
        if (k.equals("bold")) {
            return new CssNumber(700, true);
        }
        if (k.equals("bolder")) {
            return new CssNumber(parent < 350 ? 400 : parent < 550 ? 700 : 900, true);
        }
        if (k.equals("lighter")) {
            return new CssNumber(parent < 550 ? 100 : parent < 750 ? 400 : 700, true);
        }
        return new CssNumber(400, true);
    }

    /**
     * Computes a border-like width: keywords become 1, 3 and 5 pixels.
     * <p>
     * 테두리 같은 두께를 계산한다: 키워드는 1, 3, 5 픽셀이 된다.
     *
     * @param v  the specified value / 지정 값
     * @param lc the length context / 길이 컨텍스트
     * @return a pixel length / 픽셀 길이
     */
    private static CssValue lineWidth(CssValue v, LengthContext lc) {
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            return CssLength.px(k.equals("thin") ? 1 : k.equals("thick") ? 5 : 3);
        }
        return map(v, lc);
    }

    /**
     * Maps a value structurally: relative lengths become pixels, calc expressions are simplified, colors lose their keyword spelling.
     * <p>
     * 값을 구조적으로 바꾼다: 상대 길이는 픽셀이 되고 calc 식은 단순화되며 색은 키워드 표기를 잃는다.
     *
     * @param v  the value / 값
     * @param lc the length context / 길이 컨텍스트
     * @return the computed value / 계산 값
     */
    static CssValue map(CssValue v, LengthContext lc) {
        if (v instanceof CssLength) {
            CssLength l = (CssLength) v;
            if (l.isPx()) {
                return v;
            }
            double px = l.toPx(lc);
            return Double.isNaN(px) ? v : CssLength.px(px);
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).simplify(lc);
        }
        if (v instanceof CssColor) {
            // 시스템 색과 light-dark() 는 요소가 쓰는 색 구성표의 값이 된다.
            return ((CssColor) v).resolve(lc.isDark());
        }
        if (v instanceof CssList) {
            List<CssValue> items = new ArrayList<CssValue>();
            // 목록의 항목을 하나씩 계산한다.
            for (int i = 0; i < ((CssList) v).size(); i++) {
                items.add(map(((CssList) v).get(i), lc));
            }
            return new CssList(items, ((CssList) v).isComma());
        }
        if (v instanceof CssFunctionValue) {
            List<CssValue> args = new ArrayList<CssValue>();
            // 함수 인수를 하나씩 계산한다.
            for (int i = 0; i < ((CssFunctionValue) v).getArguments().size(); i++) {
                args.add(map(((CssFunctionValue) v).getArguments().get(i), lc));
            }
            return new CssFunctionValue(((CssFunctionValue) v).getName(), args);
        }
        return v;
    }

    /**
     * Applies the computed-value rules that involve several properties: a zero border width where the style is none or hidden, the
     * blockification of display for floats, absolutely positioned elements and the root, and the overflow combination.
     * <p>
     * 여러 속성이 얽힌 계산 값 규칙을 적용한다: 스타일이 none 이나 hidden 이면 테두리 두께 0, float 와 절대 배치 요소와 루트의 display 블록화, overflow 조합.
     *
     * @param c    the computed values (modified in place) / 계산 값(제자리에서 바뀜)
     * @param root true for the root element / 루트 요소이면 true
     */
    private static void adjust(CssValue[] c, boolean root) {
        int[][] pairs = {{Prop.BORDER_TOP_WIDTH, Prop.BORDER_TOP_STYLE}, {Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_RIGHT_STYLE},
            {Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_BOTTOM_STYLE}, {Prop.BORDER_LEFT_WIDTH, Prop.BORDER_LEFT_STYLE},
            {Prop.OUTLINE_WIDTH, Prop.OUTLINE_STYLE}, {Prop.COLUMN_RULE_WIDTH, Prop.COLUMN_RULE_STYLE}};
        // 스타일이 none 또는 hidden 인 선의 두께는 0 이다.
        for (int i = 0; i < pairs.length; i++) {
            if (c[pairs[i][1]].isKeyword("none") || c[pairs[i][1]].isKeyword("hidden")) {
                c[pairs[i][0]] = CssLength.ZERO;
            }
        }
        boolean abs = c[Prop.POSITION].isKeyword("absolute") || c[Prop.POSITION].isKeyword("fixed");
        if (abs && !c[Prop.FLOAT].isKeyword("none")) {
            c[Prop.FLOAT] = CssKeyword.NONE;
        }
        boolean floated = !c[Prop.FLOAT].isKeyword("none");
        if ((abs || floated || root) && c[Prop.DISPLAY] instanceof CssKeyword) {
            c[Prop.DISPLAY] = blockify((CssKeyword) c[Prop.DISPLAY]);
        }
        boolean xv = isVisibleOverflow(c[Prop.OVERFLOW_X]);
        boolean yv = isVisibleOverflow(c[Prop.OVERFLOW_Y]);
        if (xv != yv) {
            // 한쪽만 visible/clip 이 아니면 다른 쪽의 visible 은 auto, clip 은 hidden 으로 계산된다.
            if (xv) {
                c[Prop.OVERFLOW_X] = c[Prop.OVERFLOW_X].isKeyword("clip") ? CssKeyword.of("hidden") : CssKeyword.AUTO;
            } else {
                c[Prop.OVERFLOW_Y] = c[Prop.OVERFLOW_Y].isKeyword("clip") ? CssKeyword.of("hidden") : CssKeyword.AUTO;
            }
        }
    }

    /**
     * Tells whether an overflow value is visible or clip (the values that do not create a scroll container).
     * <p>
     * overflow 값이 visible 이나 clip(스크롤 컨테이너를 만들지 않는 값)인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for visible and clip / visible 과 clip 이면 true
     */
    private static boolean isVisibleOverflow(CssValue v) {
        return v.isKeyword("visible") || v.isKeyword("clip");
    }

    /**
     * Returns the block-level counterpart of a display value.
     * <p>
     * display 값의 블록 수준 대응 값을 반환한다.
     *
     * @param d the display keyword / display 키워드
     * @return the blockified keyword / 블록화한 키워드
     */
    private static CssKeyword blockify(CssKeyword d) {
        String k = d.getName();
        if (k.equals("inline") || k.equals("inline-block") || k.equals("run-in") || k.equals("table-row-group") || k.equals("table-header-group")
                || k.equals("table-footer-group") || k.equals("table-row") || k.equals("table-column-group") || k.equals("table-column")
                || k.equals("table-cell") || k.equals("table-caption") || k.equals("inline-list-item")) {
            return CssKeyword.of("block");
        }
        if (k.equals("inline-table")) {
            return CssKeyword.of("table");
        }
        if (k.equals("inline-flex") || k.equals("-webkit-inline-flex")) {
            return CssKeyword.of("flex");
        }
        if (k.equals("inline-grid")) {
            return CssKeyword.of("grid");
        }
        if (k.equals("-webkit-inline-box")) {
            return CssKeyword.of("-webkit-box");
        }
        return d;
    }
}
