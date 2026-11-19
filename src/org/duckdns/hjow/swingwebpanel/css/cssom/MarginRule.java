package org.duckdns.hjow.swingwebpanel.css.cssom;

/**
 * A page-margin at-rule inside <code>@page</code> (CSS Paged Media Level 3, section 4.3, W3C Working Draft 14 September 2023): one of the sixteen
 * margin boxes <code>@top-left-corner</code>, <code>@top-left</code>, <code>@top-center</code>, <code>@top-right</code>, <code>@top-right-corner</code>,
 * <code>@bottom-*</code>, <code>@left-*</code>, <code>@right-*</code> with a block of declarations (CSSOM <code>CSSMarginRule</code>, legacy type 9). The
 * rule only holds what was written; there is no printing, so the margin boxes are never laid out.
 * <p>
 * <code>@page</code> 안의 쪽 여백 at-규칙(CSS Paged Media Level 3 4.3 절, W3C 작업 초안 2023-09-14): 열여섯 개의 여백 상자 <code>@top-left-corner</code>,
 * <code>@top-left</code>, <code>@top-center</code>, <code>@top-right</code>, <code>@top-right-corner</code>, <code>@bottom-*</code>, <code>@left-*</code>,
 * <code>@right-*</code> 중 하나와 선언 블록(CSSOM <code>CSSMarginRule</code>, 옛 종류 9). 규칙은 쓰인 것만 보관하며 인쇄가 없으므로 여백 상자를 배치하지 않는다.
 */
public final class MarginRule extends CssRule {

    /** The name without the at sign, in lower case, such as <code>top-left</code>. / 골뱅이 없는 소문자 이름(<code>top-left</code> 등). */
    private final String name;

    /** The declarations. / 선언. */
    private final CssStyleDeclaration style;

    /**
     * Creates a margin rule.
     * <p>
     * 여백 규칙을 만든다.
     *
     * @param name  the margin box name in lower case, without the at sign / 골뱅이 없는 소문자 여백 상자 이름
     * @param style the declarations / 선언
     */
    public MarginRule(String name, CssStyleDeclaration style) {
        this.name = name;
        this.style = style;
        style.setParentRule(this);
    }

    /**
     * Returns the name (CSSOM <code>name</code>).
     * <p>
     * 이름을 반환한다(CSSOM <code>name</code>).
     *
     * @return the name such as <code>top-left</code> / <code>top-left</code> 같은 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the declaration block (CSSOM <code>style</code>).
     * <p>
     * 선언 블록을 반환한다(CSSOM <code>style</code>).
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
     * @return {@link #MARGIN_RULE} / MARGIN_RULE
     */
    public int getType() {
        return MARGIN_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return "@" + name + " { " + style.getCssText() + " }";
    }

    /**
     * Puts the rule into a sheet: a change of the declaration block reaches the sheet.
     * <p>
     * 규칙을 시트에 넣는다: 선언 블록의 변경이 시트에 닿는다.
     *
     * @param sheet  the sheet / 시트
     * @param parent the <code>@page</code> rule / <code>@page</code> 규칙
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
