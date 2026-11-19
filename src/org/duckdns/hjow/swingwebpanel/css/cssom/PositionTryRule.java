package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.HashSet;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A <code>@position-try</code> rule (CSS Anchor Positioning Level 1 section 6.4, <a href="https://www.w3.org/TR/css-anchor-position-1/#position-try-rule">
 * https://www.w3.org/TR/css-anchor-position-1/#position-try-rule</a>, W3C Working Draft, Last-Modified 2026-03-27): a named position option, a block of positioning
 * declarations that <code>position-try-fallbacks</code> can apply to an anchor-positioned box when the box overflows. The block takes the inset properties, the margin
 * properties, the sizing properties, the self-alignment properties, <code>position-anchor</code> and <code>position-area</code> only; any other declaration is
 * dropped, and <code>!important</code> makes a declaration invalid. Several rules of one name cascade like <code>@keyframes</code>: the last one wins. The rule is
 * the CSSOM <code>CSSPositionTryRule</code>: <code>name</code> and <code>style</code>.
 * <p>
 * <code>@position-try</code> 규칙(CSS Anchor Positioning Level 1 6.4 절, W3C 작업 초안): 이름 있는 위치 대안으로, <code>position-try-fallbacks</code> 가 앵커 위치 상자가 넘칠 때 적용할 수 있는
 * 위치 선언 블록이다. 블록은 오프셋 속성, 마진 속성, 크기 속성, 자기 정렬 속성, <code>position-anchor</code>, <code>position-area</code> 만 받고 다른 선언은 버리며
 * <code>!important</code> 는 선언을 무효로 만든다. 이름이 같은 여러 규칙은 <code>@keyframes</code> 처럼 캐스케이드되어 마지막 것이 이긴다. 규칙은 CSSOM
 * <code>CSSPositionTryRule</code>(<code>name</code>, <code>style</code>)이다.
 */
public final class PositionTryRule extends CssRule {

    /** The names of the properties that the block accepts (the descriptors of <code>CSSPositionTryDescriptors</code>). 블록이 받는 속성의 이름(<code>CSSPositionTryDescriptors</code> 의 서술자). */
    private static final String[] ACCEPTED = { "margin", "margin-top", "margin-right", "margin-bottom", "margin-left", "margin-block", "margin-block-start",
        "margin-block-end", "margin-inline", "margin-inline-start", "margin-inline-end", "inset", "top", "left", "right", "bottom", "inset-block", "inset-block-start",
        "inset-block-end", "inset-inline", "inset-inline-start", "inset-inline-end", "width", "min-width", "max-width", "height", "min-height", "max-height",
        "block-size", "min-block-size", "max-block-size", "inline-size", "min-inline-size", "max-inline-size", "place-self", "align-self", "justify-self",
        "position-anchor", "position-area" };

    /** The name of the option (a dashed identifier). 대안의 이름(대시 식별자). */
    private final String name;

    /** The declarations. 선언. */
    private final CssStyleDeclaration style;

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param name  the name, a dashed identifier / 이름. 대시 식별자
     * @param style the declarations; the block is restricted to the accepted properties / 선언. 블록은 받는 속성으로 제한된다
     */
    PositionTryRule(String name, CssStyleDeclaration style) {
        this.name = name;
        this.style = style;
        style.restrictTo(acceptedLonghands());
        style.setParentRule(this);
    }

    /**
     * Returns the longhand ids that the block of the rule accepts: the longhands of the inset, margin, sizing and self-alignment properties,
     * <code>position-anchor</code> and <code>position-area</code> (CSS Anchor Positioning 1 section 6.4).
     * <p>
     * 규칙의 블록이 받는 개별 속성 id 를 반환한다: 오프셋, 마진, 크기, 자기 정렬 속성과 <code>position-anchor</code>, <code>position-area</code> 의 개별 속성(CSS Anchor Positioning 1 6.4 절).
     *
     * @return the ids (do not modify) / id(수정하지 말 것)
     */
    public static Set<Integer> acceptedLonghands() {
        return Accepted.IDS;
    }

    /**
     * Holds the accepted longhand ids, made when first needed (an immutable set of numbers that refers to nothing).
     * <p>
     * 받는 개별 속성 id 를 가진다. 처음 필요할 때 만든다(아무것도 참조하지 않는 변하지 않는 숫자 집합).
     */
    private static final class Accepted {
        /** The ids of the accepted longhands. 받는 개별 속성의 id. */
        static final Set<Integer> IDS = make();

        /**
         * Collects the longhands of the accepted names.
         * <p>
         * 받는 이름의 개별 속성을 모은다.
         *
         * @return the unmodifiable set / 수정할 수 없는 집합
         */
        private static Set<Integer> make() {
            Set<Integer> ids = new HashSet<Integer>();
            PropertyRegistry reg = PropertyRegistry.getInstance();
            // 받는 이름마다 그 이름이 정하는 개별 속성(단축 속성이면 모두)을 모은다.
            for (int i = 0; i < ACCEPTED.length; i++) {
                int[] l = reg.longhandsOf(ACCEPTED[i]);
                // 이름이 정하는 개별 속성을 집합에 넣는다.
                for (int k = 0; l != null && k < l.length; k++) {
                    ids.add(Integer.valueOf(l[k]));
                }
            }
            return java.util.Collections.unmodifiableSet(ids);
        }
    }

    /**
     * Returns the name of the option.
     * <p>
     * 대안의 이름을 반환한다.
     *
     * @return the dashed identifier / 대시 식별자
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the declarations (the CSSOM <code>style</code>).
     * <p>
     * 선언을 반환한다(CSSOM <code>style</code>).
     *
     * @return the block / 블록
     */
    public CssStyleDeclaration getStyle() {
        return style;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #POSITION_TRY_RULE} / POSITION_TRY_RULE
     */
    public int getType() {
        return POSITION_TRY_RULE;
    }

    /**
     * Puts the rule into a sheet: changes of its declaration block reach the sheet, so that the layout uses the new option.
     * <p>
     * 규칙을 시트에 넣는다: 선언 블록의 변경이 시트에 닿으므로 레이아웃은 새 대안을 쓴다.
     *
     * @param sheet  the sheet / 시트
     * @param parent the containing rule or null / 담은 규칙 또는 null
     */
    void attach(StyleSheet sheet, CssRule parent) {
        super.attach(sheet, parent);
        style.setBaseUrl(sheet == null ? null : sheet.getUrl());
        style.setChangeListener(new Runnable() {
            /**
             * Tells the sheet that the declarations changed.
             * <p>
             * 선언이 바뀌었다고 시트에 알린다.
             */
            public void run() {
                StyleSheet s = getParentStyleSheet();
                if (s != null) {
                    s.markModified();
                }
                changed();
            }
        });
    }

    /**
     * Takes the rule out of its sheet.
     * <p>
     * 규칙을 시트에서 뺀다.
     */
    void detach() {
        super.detach();
        style.setChangeListener(null);
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return "@position-try " + CssEscape.ident(name) + " { " + style.getCssText() + " }";
    }
}
