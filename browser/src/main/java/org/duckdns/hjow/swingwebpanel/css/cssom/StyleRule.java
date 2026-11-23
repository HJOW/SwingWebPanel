package org.duckdns.hjow.swingwebpanel.css.cssom;

import org.duckdns.hjow.swingwebpanel.css.selector.NamespaceMap;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;

/**
 * A style rule: a selector list and a declaration block, for example <code>a:hover, a:focus { color: red }</code>. The declaration block is live: a
 * change made through it (the CSSOM <code>style</code> of the rule) reaches the style sheet, which tells the style resolver. The selector text can be
 * replaced ({@link #setSelectorText}); text that is not a valid selector list leaves the rule unchanged, as the CSSOM requires.
 * <p>
 * 스타일 규칙: 선택자 목록과 선언 블록. 예: <code>a:hover, a:focus { color: red }</code>. 선언 블록은 실시간이다: 그것(규칙의 CSSOM <code>style</code>)을 통한 변경은 시트에
 * 닿고 시트가 스타일 리졸버에 알린다. 선택자 문자열은 바꿀 수 있으며({@link #setSelectorText}), 올바른 선택자 목록이 아닌 텍스트는 CSSOM 대로 규칙을 그대로 둔다.
 */
public final class StyleRule extends CssRule {

    /** The parsed selectors. / 파싱한 선택자. */
    private SelectorList selectors;

    /** The declarations. / 선언. */
    private final CssStyleDeclaration style;

    /**
     * Creates a style rule.
     * <p>
     * 스타일 규칙을 만든다.
     *
     * @param selectors the selectors / 선택자
     * @param style     the declarations / 선언
     */
    public StyleRule(SelectorList selectors, CssStyleDeclaration style) {
        this.selectors = selectors;
        this.style = style;
        style.setParentRule(this);
    }

    /**
     * Returns the selectors.
     * <p>
     * 선택자를 반환한다.
     *
     * @return the selector list / 선택자 목록
     */
    public SelectorList getSelectors() {
        return selectors;
    }

    /**
     * Returns the selector text in the canonical CSSOM form.
     * <p>
     * 정규 CSSOM 형태의 선택자 문자열을 반환한다.
     *
     * @return the selector text / 선택자 문자열
     */
    public String getSelectorText() {
        return selectors.toCssText();
    }

    /**
     * Replaces the selectors by the parsed text (CSSOM <code>selectorText</code> setter): the namespace prefixes of the sheet are known, and text that
     * is not a valid selector list is ignored.
     * <p>
     * 선택자를 파싱한 텍스트로 바꾼다(CSSOM <code>selectorText</code> 설정): 시트의 네임스페이스 접두사를 알고 있으며, 올바른 선택자 목록이 아닌 텍스트는 무시한다.
     *
     * @param text the selector list text / 선택자 목록 문자열
     */
    public void setSelectorText(String text) {
        StyleSheet sheet = getParentStyleSheet();
        NamespaceMap ns = sheet == null ? new NamespaceMap() : sheet.getNamespaces();
        SelectorList parsed;
        try {
            parsed = SelectorParser.parse(CssParser.parseComponentValues(text == null ? "" : text), ns);
        } catch (SelectorSyntaxException e) {
            // 올바르지 않은 선택자이면 규칙을 그대로 둔다.
            return;
        }
        this.selectors = parsed;
        if (sheet != null) {
            sheet.markModified();
        }
        changed();
    }

    /**
     * Returns the declaration block.
     * <p>
     * 선언 블록을 반환한다.
     *
     * @return the declarations / 선언
     */
    public CssStyleDeclaration getStyle() {
        return style;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #STYLE_RULE} / STYLE_RULE
     */
    public int getType() {
        return STYLE_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return getSelectorText() + " { " + style.getCssText() + " }";
    }

    /**
     * Puts the rule into a sheet: a change of the declaration block reaches the sheet, and relative urls that a script writes into the block resolve
     * against the URL of the sheet.
     * <p>
     * 규칙을 시트에 넣는다: 선언 블록의 변경이 시트에 닿고, 스크립트가 블록에 쓰는 상대 url 은 시트의 URL 을 기준으로 해석한다.
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
     * Takes the rule out of its sheet: changes of the declaration block no longer reach any sheet.
     * <p>
     * 규칙을 시트에서 뺀다: 선언 블록의 변경은 더 이상 어떤 시트에도 닿지 않는다.
     */
    void detach() {
        super.detach();
        style.setChangeListener(null);
    }
}
