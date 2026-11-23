package org.duckdns.hjow.swingwebpanel.css.cssom;

/**
 * Base class of the rules of a style sheet. The numeric types follow the CSSOM (<code>CSSRule.type</code>) where an equivalent exists.
 * <p>
 * 스타일 시트 규칙의 기반 클래스. 숫자 종류는 대응하는 것이 있으면 CSSOM(<code>CSSRule.type</code>)을 따른다.
 */
public abstract class CssRule {

    /** Type: style rule. / 종류: 스타일 규칙. */
    public static final int STYLE_RULE = 1;
    /** Type constant of the import rule. / 종류: <code>@import</code>. */
    public static final int IMPORT_RULE = 3;
    /** Type constant of the media rule. / 종류: <code>@media</code>. */
    public static final int MEDIA_RULE = 4;
    /** Type constant of the font face rule. / 종류: <code>@font-face</code>. */
    public static final int FONT_FACE_RULE = 5;
    /** Type constant of the keyframes rule. / 종류: <code>@keyframes</code>. */
    public static final int KEYFRAMES_RULE = 7;
    /** Type: one keyframe. / 종류: 키프레임 하나. */
    public static final int KEYFRAME_RULE = 8;
    /** Type constant of the namespace rule. / 종류: <code>@namespace</code>. */
    public static final int NAMESPACE_RULE = 10;
    /** Type constant of the supports rule. / 종류: <code>@supports</code>. */
    public static final int SUPPORTS_RULE = 12;
    /** Type constant of the layer rule. / 종류: <code>@layer</code>. */
    public static final int LAYER_RULE = 100;
    /** Type constant of the container rule. / 종류: <code>@container</code>. */
    public static final int CONTAINER_RULE = 101;
    /** Type constant of the scope rule. / 종류: <code>@scope</code>. */
    public static final int SCOPE_RULE = 102;
    /** Type constant of the registered custom property rule. / 종류: <code>@property</code>. */
    public static final int PROPERTY_RULE = 103;
    /** Type constant of the counter style rule. / 종류: <code>@counter-style</code>. */
    public static final int COUNTER_STYLE_RULE = 104;
    /** Type constant of the starting style rule. / 종류: <code>@starting-style</code>. */
    public static final int STARTING_STYLE_RULE = 105;
    /** Type constant of the page rule (<code>@page</code>; the legacy CSSOM number is 6). / 종류: <code>@page</code>(옛 CSSOM 번호는 6). */
    public static final int PAGE_RULE = 6;
    /** Rule type of <code>@position-try</code> (CSS Anchor Positioning 1); the CSSOM reports 0. <code>@position-try</code> 의 규칙 종류(CSS Anchor Positioning 1). CSSOM 은 0 을 알린다. */
    public static final int POSITION_TRY_RULE = 106;
    /** Type of <code>@font-feature-values</code> (CSSOM reports 14, <code>CSSRule.FONT_FEATURE_VALUES_RULE</code>). / <code>@font-feature-values</code> 의 종류(CSSOM 은 14). */
    public static final int FONT_FEATURE_VALUES_RULE = 107;
    /** Type constant of a margin rule inside <code>@page</code> (the legacy CSSOM number is 9). / 종류: <code>@page</code> 안의 여백 규칙(옛 CSSOM 번호는 9). */
    public static final int MARGIN_RULE = 9;
    /** Type: a rule that is kept but not interpreted (<code>@page</code>, <code>@property</code> ...). / 종류: 보관하지만 해석하지 않는 규칙(<code>@page</code>, <code>@property</code> 등). */
    public static final int UNKNOWN_RULE = 0;

    /** The rule that contains this one (a group rule or a keyframes rule), or null for a rule at the top level of a sheet. / 이 규칙을 담은 규칙(그룹 규칙이나 키프레임 규칙). 시트의 최상위 규칙이면 null. */
    private CssRule parentRule;

    /** The sheet that contains this rule (also for a nested rule), or null while the rule is not in a sheet. / 이 규칙을 담은 시트(중첩된 규칙도 마찬가지). 시트에 들어 있지 않으면 null. */
    private StyleSheet parentSheet;

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return one of the type constants / 종류 상수 중 하나
     */
    public abstract int getType();

    /**
     * Returns the number that the CSSOM <code>CSSRule.type</code> attribute reports: the type constant for the rule kinds that have a legacy number
     * and 0 for the others (<code>@layer</code>, <code>@property</code> ...). Scripts use this, not {@link #getType()}, which also numbers the kinds
     * that the CSSOM does not. <code>@counter-style</code> reports 11 (<code>CSSRule.COUNTER_STYLE_RULE</code>).
     * <p>
     * CSSOM <code>CSSRule.type</code> 속성이 알려 주는 숫자를 반환한다: 옛 번호가 있는 규칙 종류는 종류 상수, 그 밖의 것(<code>@layer</code>,
     * <code>@property</code> 등)은 0. 스크립트는 CSSOM 에 없는 종류에도 번호를 붙인 {@link #getType()} 이 아니라 이것을 쓴다.
     * 단 <code>@counter-style</code> 는 옛 번호 11(<code>CSSRule.COUNTER_STYLE_RULE</code>)을 가진다.
     *
     * @return the CSSOM type number / CSSOM 종류 번호
     */
    public int getCssomType() {
        int t = getType();
        // @counter-style 은 CSS Counter Styles 규격이 CSSRule 에 11 번을 주었다.
        if (t == COUNTER_STYLE_RULE) {
            return 11;
        }
        // @font-feature-values 는 CSS Fonts 4 가 CSSRule 에 14 번을 주었다.
        if (t == FONT_FEATURE_VALUES_RULE) {
            return 14;
        }
        // 100 이상은 이 엔진만 쓰는 내부 번호(@layer 등)이므로 CSSOM 에서는 0 이다.
        return t >= LAYER_RULE ? 0 : t;
    }

    /**
     * Returns the rule that contains this one.
     * <p>
     * 이 규칙을 담은 규칙을 반환한다.
     *
     * @return the group rule or keyframes rule, or null at the top level / 그룹 규칙이나 키프레임 규칙. 최상위이면 null
     */
    public CssRule getParentRule() {
        return parentRule;
    }

    /**
     * Returns the style sheet that contains this rule (for a nested rule the sheet of the outermost rule).
     * <p>
     * 이 규칙을 담은 스타일 시트를 반환한다(중첩된 규칙이면 가장 바깥 규칙의 시트).
     *
     * @return the sheet, or null if the rule is in no sheet / 시트. 시트에 들어 있지 않으면 null
     */
    public StyleSheet getParentStyleSheet() {
        return parentSheet;
    }

    /**
     * Puts this rule (and the rules inside it) into a sheet: records the container links that the CSSOM exposes and makes changes of the rule
     * reach the sheet. A rule kind that contains other rules overrides it and attaches them too.
     * <p>
     * 이 규칙(과 그 안의 규칙)을 시트에 넣는다: CSSOM 이 보여 주는 컨테이너 링크를 기록하고 규칙의 변경이 시트에 닿게 한다. 다른 규칙을 담는 규칙 종류는
     * 이것을 재정의하여 안의 규칙도 붙인다.
     *
     * @param sheet  the sheet that contains the rule / 규칙을 담은 시트
     * @param parent the rule that contains this one, or null / 이 규칙을 담은 규칙. 없으면 null
     */
    void attach(StyleSheet sheet, CssRule parent) {
        this.parentSheet = sheet;
        this.parentRule = parent;
    }

    /**
     * Takes this rule out of its sheet (the links are cleared; a removed rule no longer changes any sheet).
     * <p>
     * 이 규칙을 시트에서 뺀다(링크를 지우며, 뺀 규칙은 더 이상 어떤 시트도 바꾸지 않는다).
     */
    void detach() {
        this.parentSheet = null;
        this.parentRule = null;
    }

    /**
     * Tells the sheet that contains this rule that something in it changed (the style resolver then drops what it computed from the sheet).
     * <p>
     * 이 규칙을 담은 시트에 그 안의 무언가가 바뀌었다고 알린다(그러면 스타일 리졸버가 그 시트로 계산한 것을 버린다).
     */
    void changed() {
        StyleSheet s = parentSheet;
        if (s != null) {
            s.changed();
        }
    }

    /**
     * The CSSOM <code>cssText</code> setter, which does nothing: the text of a rule cannot be replaced through its <code>cssText</code> attribute.
     * <p>
     * CSSOM <code>cssText</code> 설정 메서드로 아무것도 하지 않는다: 규칙의 텍스트는 <code>cssText</code> 속성으로 바꿀 수 없다.
     *
     * @param ignored the text, which is ignored / 무시되는 텍스트
     */
    public void setCssText(String ignored) {
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public abstract String getCssText();

    /**
     * Returns the serialized rule.
     * <p>
     * 직렬화한 규칙을 반환한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toString() {
        return getCssText();
    }
}
