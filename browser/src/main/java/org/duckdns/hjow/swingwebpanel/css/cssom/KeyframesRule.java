package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxRule;
import org.duckdns.hjow.swingwebpanel.dom.DomException;

/**
 * A <code>@keyframes</code> rule: the animation name and its keyframes in source order. Each keyframe has one or more offsets (0 to 1) and a
 * declaration block; the animation engine (plan item P10-07) interpolates between them. The CSSOM can rename the rule, append and delete keyframes and
 * find one by its key text, and the keyframes are rules of their own (<code>CSSKeyframeRule</code>) whose key text and declarations can be changed.
 * <p>
 * <code>@keyframes</code> 규칙: 애니메이션 이름과 소스 순서의 키프레임. 키프레임마다 오프셋(0 에서 1) 하나 이상과 선언 블록이 있으며, 애니메이션 엔진(계획 P10-07)이
 * 그 사이를 보간한다. CSSOM 은 규칙의 이름을 바꾸고, 키프레임을 덧붙이고 지우고, 키 텍스트로 찾을 수 있으며, 키프레임은 키 텍스트와 선언을 바꿀 수 있는 독립된 규칙
 * (<code>CSSKeyframeRule</code>)이다.
 */
public final class KeyframesRule extends CssRule {

    /**
     * One keyframe: offsets and declarations; a rule of its own (the CSSOM <code>CSSKeyframeRule</code>).
     * <p>
     * 키프레임 하나: 오프셋과 선언. 독립된 규칙이다(CSSOM <code>CSSKeyframeRule</code>).
     */
    public static final class Keyframe extends CssRule {
        /** The offsets between 0 and 1. / 0 과 1 사이의 오프셋. */
        private List<Double> offsets;
        /** The declarations. / 선언. */
        private final CssStyleDeclaration style;

        /**
         * Creates a keyframe.
         * <p>
         * 키프레임을 만든다.
         *
         * @param offsets offsets / 오프셋
         * @param style   declarations / 선언
         */
        Keyframe(List<Double> offsets, CssStyleDeclaration style) {
            this.offsets = Collections.unmodifiableList(new ArrayList<Double>(offsets));
            this.style = style;
            style.setParentRule(this);
        }

        /**
         * Returns the offsets.
         * <p>
         * 오프셋을 반환한다.
         *
         * @return values from 0 to 1 / 0 에서 1 까지의 값
         */
        public List<Double> getOffsets() {
            return offsets;
        }

        /**
         * Returns the declarations.
         * <p>
         * 선언을 반환한다.
         *
         * @return the block / 블록
         */
        public CssStyleDeclaration getStyle() {
            return style;
        }

        /**
         * Returns the key text (CSSOM <code>keyText</code>): the offsets as percentages separated by a comma and a space.
         * <p>
         * 키 텍스트를 반환한다(CSSOM <code>keyText</code>): 쉼표와 공백으로 구분한 백분율 오프셋.
         *
         * @return text such as <code>0%, 50%</code> / <code>0%, 50%</code> 같은 문자열
         */
        public String getKeyText() {
            StringBuilder sel = new StringBuilder();
            // 오프셋을 백분율로 이어 쓴다.
            for (int j = 0; j < offsets.size(); j++) {
                if (j > 0) {
                    sel.append(", ");
                }
                sel.append(CssEscape.number(offsets.get(j).doubleValue() * 100)).append('%');
            }
            return sel.toString();
        }

        /**
         * Replaces the offsets by the parsed key text (CSSOM <code>keyText</code> setter).
         * <p>
         * 오프셋을 파싱한 키 텍스트로 바꾼다(CSSOM <code>keyText</code> 설정).
         *
         * @param text <code>from</code>, <code>to</code> or percentages separated by commas / <code>from</code>, <code>to</code> 또는 쉼표로 구분한 백분율
         * @throws DomException <code>SyntaxError</code> if the text is not a valid key text / 올바른 키 텍스트가 아니면 <code>SyntaxError</code>
         */
        public void setKeyText(String text) {
            List<Double> parsed = StyleSheetBuilder.offsets(CssParser.parseComponentValues(text == null ? "" : text));
            if (parsed == null || parsed.isEmpty()) {
                throw DomException.syntax("The key text '" + text + "' is not valid.");
            }
            this.offsets = Collections.unmodifiableList(new ArrayList<Double>(parsed));
            StyleSheet s = getParentStyleSheet();
            if (s != null) {
                s.markModified();
            }
            changed();
        }

        /**
         * Returns the rule type.
         * <p>
         * 규칙 종류를 반환한다.
         *
         * @return {@link #KEYFRAME_RULE} / KEYFRAME_RULE
         */
        public int getType() {
            return KEYFRAME_RULE;
        }

        /**
         * Serializes the keyframe.
         * <p>
         * 키프레임을 직렬화한다.
         *
         * @return CSS text / CSS 문자열
         */
        public String getCssText() {
            return getKeyText() + " { " + style.getCssText() + " }";
        }

        /**
         * Puts the keyframe into a sheet: changes of its declaration block reach the sheet.
         * <p>
         * 키프레임을 시트에 넣는다: 선언 블록의 변경이 시트에 닿는다.
         *
         * @param sheet  the sheet / 시트
         * @param parent the keyframes rule / 키프레임 규칙
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
         * Takes the keyframe out of its sheet.
         * <p>
         * 키프레임을 시트에서 뺀다.
         */
        void detach() {
            super.detach();
            style.setChangeListener(null);
        }
    }

    /** The animation name. / 애니메이션 이름. */
    private String name;

    /** The keyframes (changed through {@link #appendRule} and {@link #deleteRule}). / 키프레임({@link #appendRule}, {@link #deleteRule} 로 바뀐다). */
    private final List<Keyframe> keyframes;

    /** The read-only view of the keyframes that {@link #getKeyframes()} hands out (it follows the changes). / {@link #getKeyframes()} 가 내주는 키프레임의 읽기 전용 뷰(변경을 따라간다). */
    private final List<Keyframe> keyframesView;

    /** The rule list object of the CSSOM, created when it is first asked for. / CSSOM 의 규칙 목록 객체. 처음 요청할 때 만든다. */
    private CssRuleList ruleList;

    /**
     * Creates a keyframes rule.
     * <p>
     * 키프레임 규칙을 만든다.
     *
     * @param name      the name / 이름
     * @param keyframes the keyframes / 키프레임
     */
    public KeyframesRule(String name, List<Keyframe> keyframes) {
        this.name = name;
        this.keyframes = new ArrayList<Keyframe>(keyframes);
        this.keyframesView = Collections.unmodifiableList(this.keyframes);
    }

    /**
     * Returns the animation name.
     * <p>
     * 애니메이션 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Renames the animation (CSSOM <code>name</code> setter).
     * <p>
     * 애니메이션의 이름을 바꾼다(CSSOM <code>name</code> 설정).
     *
     * @param n the new name / 새 이름
     */
    public void setName(String n) {
        this.name = n == null ? "" : n;
        StyleSheet s = getParentStyleSheet();
        if (s != null) {
            s.markModified();
        }
        changed();
    }

    /**
     * Returns the keyframes in source order. The list is a read-only view that follows later insertions and deletions.
     * <p>
     * 소스 순서의 키프레임을 반환한다. 목록은 이후의 삽입과 삭제를 따라가는 읽기 전용 뷰이다.
     *
     * @return unmodifiable view / 수정할 수 없는 뷰
     */
    public List<Keyframe> getKeyframes() {
        return keyframesView;
    }

    /**
     * Returns the rule list object of the CSSOM, <code>cssRules</code> (the same object every time).
     * <p>
     * CSSOM 의 규칙 목록 객체 <code>cssRules</code> 를 반환한다(매번 같은 객체).
     *
     * @return the rule list of the keyframes / 키프레임의 규칙 목록
     */
    public CssRuleList getCssRules() {
        if (ruleList == null) {
            ruleList = new CssRuleList(keyframesView);
        }
        return ruleList;
    }

    /**
     * Appends a keyframe given as text (CSSOM <code>appendRule</code>), for example <code>50% { color: red }</code>; text that is not a keyframe rule is
     * ignored.
     * <p>
     * 텍스트로 주어진 키프레임을 덧붙인다(CSSOM <code>appendRule</code>). 예: <code>50% { color: red }</code>. 키프레임 규칙이 아닌 텍스트는 무시한다.
     *
     * @param text one keyframe rule / 키프레임 규칙 하나
     */
    public void appendRule(String text) {
        List<SyntaxRule> syn = CssParser.parseStylesheet(text == null ? "" : text, null);
        if (syn.size() != 1 || syn.get(0).isAtRule()) {
            return;
        }
        Keyframe k = StyleSheetBuilder.buildKeyframe(syn.get(0), name, null, getParentStyleSheet() == null ? null : getParentStyleSheet().getUrl());
        if (k == null) {
            return;
        }
        keyframes.add(k);
        k.attach(getParentStyleSheet(), this);
        StyleSheet s = getParentStyleSheet();
        if (s != null) {
            s.markModified();
        }
        changed();
    }

    /**
     * Normalizes a key text so that two spellings of the same offsets compare equal (<code>from</code> is <code>0%</code>).
     * <p>
     * 같은 오프셋의 서로 다른 표기가 같게 비교되도록 키 텍스트를 정규화한다(<code>from</code> 은 <code>0%</code>).
     *
     * @param text the key text / 키 텍스트
     * @return the canonical text, or null if it is not a valid key text / 정규화한 문자열. 올바른 키 텍스트가 아니면 null
     */
    private static String canonical(String text) {
        List<Double> o = StyleSheetBuilder.offsets(CssParser.parseComponentValues(text == null ? "" : text));
        if (o == null || o.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 오프셋을 백분율로 이어 쓴다.
        for (int i = 0; i < o.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(CssEscape.number(o.get(i).doubleValue() * 100)).append('%');
        }
        return sb.toString();
    }

    /**
     * Finds the last keyframe whose key text matches (CSSOM <code>findRule</code>).
     * <p>
     * 키 텍스트가 일치하는 마지막 키프레임을 찾는다(CSSOM <code>findRule</code>).
     *
     * @param select the key text / 키 텍스트
     * @return the keyframe or null / 키프레임 또는 null
     */
    public Keyframe findRule(String select) {
        String want = canonical(select);
        // 뒤에서부터 훑어 처음 일치하는 키프레임이 마지막 일치이다.
        for (int i = keyframes.size() - 1; want != null && i >= 0; i--) {
            if (want.equals(keyframes.get(i).getKeyText())) {
                return keyframes.get(i);
            }
        }
        return null;
    }

    /**
     * Deletes the last keyframe whose key text matches (CSSOM <code>deleteRule</code>); nothing happens if there is none.
     * <p>
     * 키 텍스트가 일치하는 마지막 키프레임을 지운다(CSSOM <code>deleteRule</code>). 없으면 아무 일도 없다.
     *
     * @param select the key text / 키 텍스트
     */
    public void deleteRule(String select) {
        Keyframe k = findRule(select);
        if (k == null) {
            return;
        }
        keyframes.remove(k);
        k.detach();
        StyleSheet s = getParentStyleSheet();
        if (s != null) {
            s.markModified();
        }
        changed();
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #KEYFRAMES_RULE} / KEYFRAMES_RULE
     */
    public int getType() {
        return KEYFRAMES_RULE;
    }

    /**
     * Puts the rule and its keyframes into a sheet.
     * <p>
     * 규칙과 그 키프레임을 시트에 넣는다.
     *
     * @param sheet  the sheet / 시트
     * @param parent the containing rule or null / 담은 규칙 또는 null
     */
    void attach(StyleSheet sheet, CssRule parent) {
        super.attach(sheet, parent);
        // 키프레임마다 이 규칙을 부모로 하여 같은 시트에 붙인다.
        for (int i = 0; i < keyframes.size(); i++) {
            keyframes.get(i).attach(sheet, this);
        }
    }

    /**
     * Takes the rule and its keyframes out of their sheet.
     * <p>
     * 규칙과 그 키프레임을 시트에서 뺀다.
     */
    void detach() {
        super.detach();
        // 키프레임도 시트에서 뗀다.
        for (int i = 0; i < keyframes.size(); i++) {
            keyframes.get(i).detach();
        }
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        StringBuilder sb = new StringBuilder("@keyframes ").append(CssEscape.ident(name)).append(" {");
        // 키프레임을 차례로 이어 쓴다.
        for (int i = 0; i < keyframes.size(); i++) {
            Keyframe k = keyframes.get(i);
            sb.append("\n  ").append(k.getKeyText()).append(" { ").append(k.style.getCssText()).append(" }");
        }
        return sb.append("\n}").toString();
    }
}
