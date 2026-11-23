package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;

/**
 * A <code>@counter-style</code> rule (CSS Counter Styles Level 3, <a href="https://www.w3.org/TR/css-counter-styles-3/">
 * https://www.w3.org/TR/css-counter-styles-3/</a>): the definition of a named way to write a counter value, with the descriptors <code>system</code>,
 * <code>symbols</code>, <code>additive-symbols</code>, <code>negative</code>, <code>prefix</code>, <code>suffix</code>, <code>range</code>,
 * <code>pad</code>, <code>fallback</code> and <code>speak-as</code> (kept but not used: there is no speech). The rule only holds the descriptors as
 * written; the layout reads them (<code>layout.CounterStyles</code>) and drops a definition that is not valid for its system.
 * <p>
 * <code>@counter-style</code> 규칙(CSS Counter Styles Level 3, <a href="https://www.w3.org/TR/css-counter-styles-3/">
 * https://www.w3.org/TR/css-counter-styles-3/</a>): 카운터 값을 쓰는 이름 있는 방법의 정의. 서술자는 <code>system</code>, <code>symbols</code>,
 * <code>additive-symbols</code>, <code>negative</code>, <code>prefix</code>, <code>suffix</code>, <code>range</code>, <code>pad</code>,
 * <code>fallback</code>, <code>speak-as</code>(보관하지만 쓰지 않는다. 음성이 없다)이다. 규칙은 서술자를 쓰인 그대로 보관하고, 레이아웃이
 * (<code>layout.CounterStyles</code>) 읽어 시스템에 맞지 않는 정의는 버린다.
 */
public final class CounterStyleRule extends CssRule {

    /** The counter style name. / 카운터 스타일 이름. */
    private String name;

    /** The descriptors by lower-case name (changed through the CSSOM setters). / 소문자 이름별 서술자(CSSOM 설정 메서드로 바뀐다). */
    private final Map<String, List<ComponentValue>> descriptors;

    /** The read-only view of the descriptors that {@link #getDescriptors()} hands out (it follows the changes). / {@link #getDescriptors()} 가 내주는 서술자의 읽기 전용 뷰(변경을 따라간다). */
    private final Map<String, List<ComponentValue>> descriptorsView;

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param name        the counter style name / 카운터 스타일 이름
     * @param descriptors the descriptor values by lower-case name (copied) / 소문자 이름별 서술자 값(복사됨)
     */
    public CounterStyleRule(String name, Map<String, List<ComponentValue>> descriptors) {
        this.name = name;
        this.descriptors = new LinkedHashMap<String, List<ComponentValue>>(descriptors);
        this.descriptorsView = Collections.unmodifiableMap(this.descriptors);
    }

    /**
     * Tells whether a name can be defined by an author: a custom identifier that is not <code>none</code>, a CSS-wide keyword, or one of the names
     * that the specification reserves (<code>decimal</code>, <code>disc</code>, <code>circle</code>, <code>square</code>, <code>disclosure-open</code>,
     * <code>disclosure-closed</code>).
     * <p>
     * 이름을 저자가 정의할 수 있는지 알려 준다: <code>none</code>, CSS 전역 키워드, 규격이 예약한 이름(<code>decimal</code>, <code>disc</code>, <code>circle</code>,
     * <code>square</code>, <code>disclosure-open</code>, <code>disclosure-closed</code>)이 아닌 사용자 정의 식별자.
     *
     * @param name the name / 이름
     * @return true if it can be defined / 정의할 수 있으면 true
     */
    public static boolean isDefinable(String name) {
        String l = name.toLowerCase();
        return !(l.equals("none") || l.equals("inherit") || l.equals("initial") || l.equals("unset") || l.equals("default") || l.equals("decimal")
                || l.equals("disc") || l.equals("circle") || l.equals("square") || l.equals("disclosure-open") || l.equals("disclosure-closed"));
    }

    /**
     * Returns the name.
     * <p>
     * 이름을 반환한다.
     *
     * @return the name as written / 쓰인 그대로의 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the descriptors.
     * <p>
     * 서술자를 반환한다.
     *
     * @return the unmodifiable descriptor values by lower-case name / 소문자 이름별 서술자 값(수정할 수 없음)
     */
    public Map<String, List<ComponentValue>> getDescriptors() {
        return descriptorsView;
    }

    /**
     * Returns the text of a descriptor, the CSSOM attribute of the same name (<code>CSSCounterStyleRule.system</code> ...).
     * <p>
     * 서술자의 문자열을 반환한다. 같은 이름의 CSSOM 속성(<code>CSSCounterStyleRule.system</code> 등)이 읽는다.
     *
     * @param key the descriptor name in lower case / 소문자 서술자 이름
     * @return the text, the empty string if the descriptor was not written / 문자열. 쓰이지 않았으면 빈 문자열
     */
    private String text(String key) {
        List<ComponentValue> v = descriptors.get(key);
        return v == null ? "" : CssBlock.join(v).trim();
    }

    /**
     * Replaces a descriptor by the text (the CSSOM setters): an empty text removes it, the others are parsed as a value and replace the old one; the
     * sheet that holds the rule is told so that the counters are formatted again.
     * <p>
     * 서술자를 문자열로 바꾼다(CSSOM 설정 메서드): 빈 문자열은 지우고 그 밖은 값으로 파싱해 이전 것을 대체하며, 규칙을 담은 시트에 알려 카운터를 다시 만든다.
     *
     * @param key  the descriptor name in lower case / 소문자 서술자 이름
     * @param text the new text / 새 문자열
     */
    private void put(String key, String text) {
        String t = text == null ? "" : text.trim();
        // 빈 문자열이면 서술자를 지우고, 아니면 값으로 파싱해 바꾼다.
        if (t.length() == 0) {
            descriptors.remove(key);
        } else {
            descriptors.put(key, new ArrayList<ComponentValue>(CssParser.parseComponentValues(t)));
        }
        changed();
    }

    /** Returns the CSSOM <code>system</code>. / CSSOM <code>system</code> 을 반환한다. @return the text / 문자열 */
    public String getSystem() {
        return text("system");
    }

    /** Sets the CSSOM <code>system</code>. / CSSOM <code>system</code> 을 설정한다. @param v the text / 문자열 */
    public void setSystem(String v) {
        put("system", v);
    }

    /** Returns the CSSOM <code>symbols</code>. / CSSOM <code>symbols</code> 를 반환한다. @return the text / 문자열 */
    public String getSymbols() {
        return text("symbols");
    }

    /** Sets the CSSOM <code>symbols</code>. / CSSOM <code>symbols</code> 를 설정한다. @param v the text / 문자열 */
    public void setSymbols(String v) {
        put("symbols", v);
    }

    /** Returns the CSSOM <code>additiveSymbols</code>. / CSSOM <code>additiveSymbols</code> 를 반환한다. @return the text / 문자열 */
    public String getAdditiveSymbols() {
        return text("additive-symbols");
    }

    /** Sets the CSSOM <code>additiveSymbols</code>. / CSSOM <code>additiveSymbols</code> 를 설정한다. @param v the text / 문자열 */
    public void setAdditiveSymbols(String v) {
        put("additive-symbols", v);
    }

    /** Returns the CSSOM <code>negative</code>. / CSSOM <code>negative</code> 를 반환한다. @return the text / 문자열 */
    public String getNegative() {
        return text("negative");
    }

    /** Sets the CSSOM <code>negative</code>. / CSSOM <code>negative</code> 를 설정한다. @param v the text / 문자열 */
    public void setNegative(String v) {
        put("negative", v);
    }

    /** Returns the CSSOM <code>prefix</code>. / CSSOM <code>prefix</code> 를 반환한다. @return the text / 문자열 */
    public String getPrefix() {
        return text("prefix");
    }

    /** Sets the CSSOM <code>prefix</code>. / CSSOM <code>prefix</code> 를 설정한다. @param v the text / 문자열 */
    public void setPrefix(String v) {
        put("prefix", v);
    }

    /** Returns the CSSOM <code>suffix</code>. / CSSOM <code>suffix</code> 를 반환한다. @return the text / 문자열 */
    public String getSuffix() {
        return text("suffix");
    }

    /** Sets the CSSOM <code>suffix</code>. / CSSOM <code>suffix</code> 를 설정한다. @param v the text / 문자열 */
    public void setSuffix(String v) {
        put("suffix", v);
    }

    /** Returns the CSSOM <code>range</code>. / CSSOM <code>range</code> 를 반환한다. @return the text / 문자열 */
    public String getRange() {
        return text("range");
    }

    /** Sets the CSSOM <code>range</code>. / CSSOM <code>range</code> 를 설정한다. @param v the text / 문자열 */
    public void setRange(String v) {
        put("range", v);
    }

    /** Returns the CSSOM <code>pad</code>. / CSSOM <code>pad</code> 를 반환한다. @return the text / 문자열 */
    public String getPad() {
        return text("pad");
    }

    /** Sets the CSSOM <code>pad</code>. / CSSOM <code>pad</code> 를 설정한다. @param v the text / 문자열 */
    public void setPad(String v) {
        put("pad", v);
    }

    /** Returns the CSSOM <code>speakAs</code>. / CSSOM <code>speakAs</code> 를 반환한다. @return the text / 문자열 */
    public String getSpeakAs() {
        return text("speak-as");
    }

    /** Sets the CSSOM <code>speakAs</code>. / CSSOM <code>speakAs</code> 를 설정한다. @param v the text / 문자열 */
    public void setSpeakAs(String v) {
        put("speak-as", v);
    }

    /** Returns the CSSOM <code>fallback</code>. / CSSOM <code>fallback</code> 를 반환한다. @return the text / 문자열 */
    public String getFallback() {
        return text("fallback");
    }

    /** Sets the CSSOM <code>fallback</code>. / CSSOM <code>fallback</code> 를 설정한다. @param v the text / 문자열 */
    public void setFallback(String v) {
        put("fallback", v);
    }

    /**
     * Sets the counter style name (the CSSOM <code>name</code> setter). A text that is not one custom identifier that an author may define is ignored.
     * <p>
     * 카운터 스타일 이름을 설정한다(CSSOM <code>name</code> 설정 메서드). 저자가 정의할 수 있는 사용자 정의 식별자 하나가 아니면 무시한다.
     *
     * @param v the new name / 새 이름
     */
    public void setName(String v) {
        String t = v == null ? "" : v.trim();
        // 식별자 하나이고 정의할 수 있는 이름일 때만 바꾼다.
        if (t.length() > 0 && t.matches("[A-Za-z_\\-][A-Za-z0-9_\\-]*") && isDefinable(t)) {
            this.name = t;
            changed();
        }
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #COUNTER_STYLE_RULE} / COUNTER_STYLE_RULE
     */
    public int getType() {
        return COUNTER_STYLE_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        StringBuilder sb = new StringBuilder("@counter-style ").append(name).append(" {");
        // 서술자를 쓰인 순서대로 이어 쓴다.
        for (Map.Entry<String, List<ComponentValue>> e : descriptors.entrySet()) {
            sb.append(' ').append(e.getKey()).append(": ").append(CssBlock.join(e.getValue()).trim()).append(';');
        }
        return sb.append(" }").toString();
    }
}
