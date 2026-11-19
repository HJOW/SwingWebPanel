package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.MarginRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.PageRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>@page</code> (plan item P10-08, CSS Paged Media Level 3, https://www.w3.org/TR/css-page-3/, W3C Working Draft 14 September 2023): the
 * parsing of the page selector list and of the rule, the page margin rules, the CSSOM model, the cascade in the page context (specificity of the page
 * selectors, <code>!important</code>) and the computed page box: <code>size</code> with media names, orientation and lengths, percentage margins that
 * are relative to the page size, <code>marks</code> and <code>bleed</code>.
 * <p>
 * <code>@page</code> 시험(계획 P10-08, CSS Paged Media Level 3, https://www.w3.org/TR/css-page-3/, W3C 작업 초안 2023-09-14): 쪽 선택자 목록과 규칙의 파싱, 쪽 여백 규칙,
 * CSSOM 모델, 쪽 문맥의 계단식(쪽 선택자의 구체성, <code>!important</code>)과 계산된 쪽 상자: 용지 이름·방향·길이를 쓰는 <code>size</code>, 쪽 크기 기준의 백분율 여백,
 * <code>marks</code>, <code>bleed</code>.
 */
public class PageRuleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PageRuleTest() {
    }

    /**
     * Parses a page with a style sheet and returns the engine.
     * <p>
     * 스타일시트가 있는 페이지를 해석하고 엔진을 반환한다.
     *
     * @param css the style sheet / 스타일시트
     * @return the engine / 엔진
     */
    private static CssEngine engine(String css) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body><p id=p>x</p></body></html>",
                "http://t.test/page.html", null, true);
        CssEngine e = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        e.loadStyleSheets();
        return e;
    }

    /**
     * Returns the page box of a page.
     * <p>
     * 쪽의 쪽 상자를 반환한다.
     *
     * @param css    the style sheet / 스타일시트
     * @param name   the page name or null / 쪽 이름 또는 null
     * @param number the page number / 쪽 번호
     * @param blank  true for a blank page / 빈 쪽이면 true
     * @param right  true for a right page / 오른쪽 쪽이면 true
     * @return the page box / 쪽 상자
     */
    private static PageStyles.PageBox box(String css, String name, int number, boolean blank, boolean right) {
        return PageStyles.resolve(engine(css).getResolver().getPageRules(), name, number, blank, right);
    }

    /**
     * Returns the first rule of the document style sheet that is a page rule.
     * <p>
     * 문서 스타일 시트의 첫 쪽 규칙을 반환한다.
     *
     * @param e the engine / 엔진
     * @return the rule / 규칙
     */
    private static PageRule firstPage(CssEngine e) {
        List<StyleSheet> sheets = e.getDocumentStyleSheets();
        // 문서의 시트에서 첫 @page 규칙을 찾는다.
        for (int i = 0; i < sheets.size(); i++) {
            List<CssRule> rules = sheets.get(i).getRules();
            // 시트의 규칙을 차례로 살펴 쪽 규칙을 찾는다.
            for (int j = 0; j < rules.size(); j++) {
                if (rules.get(j) instanceof PageRule) {
                    return (PageRule) rules.get(j);
                }
            }
        }
        return null;
    }

    /**
     * The prelude is a page selector list: a name and the pseudo-classes without space, several selectors separated by commas, an empty list for
     * every page; a rule with an unknown pseudo-class or a malformed list is dropped.
     * <p>
     * 전문은 쪽 선택자 목록이다: 공백 없는 이름과 의사 클래스, 쉼표로 구분한 여러 선택자, 모든 쪽에 일치하는 빈 목록. 모르는 의사 클래스나 틀린 목록의 규칙은 버린다.
     */
    public void testSelectorParsing() {
        Check.equal("", firstPage(engine("@page { size: a4 }")).getSelectorText(), "an empty list");
        Check.equal(":left", firstPage(engine("@page :left { size: a4 }")).getSelectorText(), "a pseudo-class");
        Check.equal("chapter:first", firstPage(engine("@page chapter:first { size: a4 }")).getSelectorText(), "a name and a pseudo-class");
        Check.equal(":first, :blank:left", firstPage(engine("@page :first, :blank:left { size: a4 }")).getSelectorText(), "a list");
        Check.equal(":left", firstPage(engine("@page :LEFT { size: a4 }")).getSelectorText(), "pseudo-class names are case-insensitive");
        Check.isNull(firstPage(engine("@page :middle { size: a4 }")), "an unknown pseudo-class drops the rule");
        Check.isNull(firstPage(engine("@page : left { size: a4 }")), "a space inside the pseudo-class drops the rule");
        Check.isNull(firstPage(engine("@page a b { size: a4 }")), "two names drop the rule");
        Check.isNull(firstPage(engine("@page :left,, :right { size: a4 }")), "an empty selector drops the rule");
        Check.isNull(firstPage(engine("@page :first")), "a rule without a block is dropped");
    }

    /**
     * The page margin at-rules inside the rule become margin rules of the CSSOM; other at-rules and nested style rules are ignored, and the
     * sixteen names are known.
     * <p>
     * 규칙 안의 쪽 여백 at-규칙은 CSSOM 의 여백 규칙이 되고, 다른 at-규칙과 중첩된 스타일 규칙은 무시하며, 열여섯 이름을 모두 안다.
     */
    public void testMarginRules() {
        PageRule r = firstPage(engine("@page { margin: 1cm; @top-center { color: red } @bottom-right-corner { color: blue } @foo { color: green } }"));
        Check.equal(2, r.getRules().size(), "the unknown at-rule is dropped");
        Check.equal("top-center", ((MarginRule) r.getRules().get(0)).getName(), "the first margin rule");
        Check.equal("bottom-right-corner", ((MarginRule) r.getRules().get(1)).getName(), "the second margin rule");
        Check.equal(CssRule.MARGIN_RULE, r.getRules().get(0).getType(), "the margin rule type");
        Check.equal(CssRule.PAGE_RULE, r.getType(), "the page rule type");
        Check.equal("red", ((MarginRule) r.getRules().get(0)).getStyle().getPropertyValue("color"), "a declaration of a margin rule");
        String[] names = {"top-left-corner", "top-left", "top-center", "top-right", "top-right-corner", "bottom-left-corner", "bottom-left",
            "bottom-center", "bottom-right", "bottom-right-corner", "left-top", "left-middle", "left-bottom", "right-top", "right-middle", "right-bottom"};
        // 열여섯 쪽 여백 이름이 모두 인식되어야 한다.
        for (int i = 0; i < names.length; i++) {
            Check.isTrue(PageRule.isMarginName(names[i]), names[i] + " is a margin box name");
        }
        Check.isFalse(PageRule.isMarginName("top"), "top is not a margin box name");
        Check.equal(16, names.length, "sixteen names");
        Check.isTrue(r.getCssText().indexOf("@top-center") > 0, "the margin rule is serialized");
    }

    /**
     * The CSSOM model: the declarations (<code>style</code>), <code>selectorText</code> and its setter, the rule list and the serialization.
     * <p>
     * CSSOM 모델: 선언(<code>style</code>), <code>selectorText</code> 와 설정자, 규칙 목록, 직렬화.
     */
    public void testCssomModel() {
        PageRule r = firstPage(engine("@page :first { margin-top: 10cm; color: red }"));
        Check.equal("10cm", r.getStyle().getPropertyValue("margin-top"), "a declaration");
        Check.equal("red", r.getStyle().getPropertyValue("color"), "a declaration of another property");
        r.setSelectorText(":right, chapter");
        Check.equal(":right, chapter", r.getSelectorText(), "the selector text is replaced");
        r.setSelectorText(":nonsense");
        Check.equal(":right, chapter", r.getSelectorText(), "an invalid selector text is ignored");
        Check.equal(0, r.getCssRules().getLength(), "no margin rules");
        Check.contains(r.getCssText(), "@page :right, chapter {", "the serialization");
    }

    /**
     * The specificity of the page selectors: a name is more specific than a pseudo-class and <code>:first</code>/<code>:blank</code> than
     * <code>:left</code>/<code>:right</code>; repeats increase the specificity; the more specific rule wins whatever the order, equal specificities
     * go by the order, and a declaration marked important beats the others.
     * <p>
     * 쪽 선택자의 구체성: 이름이 의사 클래스보다, <code>:first</code>/<code>:blank</code> 가 <code>:left</code>/<code>:right</code> 보다 구체적이고, 반복은 구체성을
     * 올리며, 더 구체적인 규칙이 순서와 상관없이 이기고 같은 구체성은 순서를 따르며 중요 선언은 다른 것을 이긴다.
     */
    public void testCascadeInThePageContext() {
        String css = "@page :left { margin-left: 4cm } @page { margin-left: 3cm }";
        Check.near(4 * 96 / 2.54, box(css, null, 2, false, false).getMargin(3), 0.01, "the pseudo-class beats the order on a left page");
        Check.near(3 * 96 / 2.54, box(css, null, 1, false, true).getMargin(3), 0.01, "a right page takes the rule without a selector");
        css = "@page :first { margin-top: 1in } @page :left { margin-top: 2in } @page { margin-top: 3in }";
        Check.near(96, box(css, null, 1, false, false).getMargin(0), 0.01, ":first is more specific than :left");
        Check.near(192, box(css, null, 2, false, false).getMargin(0), 0.01, ":left on the second page");
        Check.near(288, box(css, null, 3, false, true).getMargin(0), 0.01, "the rule without a selector on the third (right) page");
        css = "@page chapter { margin-top: 1in } @page :first { margin-top: 2in }";
        Check.near(96, box(css, "chapter", 1, false, true).getMargin(0), 0.01, "a name beats :first");
        Check.near(192, box(css, null, 1, false, true).getMargin(0), 0.01, "an unnamed page ignores the named rule");
        css = "@page :first:first { margin-top: 1in } @page :first:left { margin-top: 2in }";
        Check.near(96, box(css, null, 1, false, false).getMargin(0), 0.01, "a repeated pseudo-class counts again");
        css = "@page { margin-top: 1in } @page { margin-top: 2in }";
        Check.near(192, box(css, null, 1, false, true).getMargin(0), 0.01, "the later of equal specificity wins");
        css = "@page { margin-top: 1in !important } @page :first { margin-top: 2in }";
        Check.near(96, box(css, null, 1, false, true).getMargin(0), 0.01, "a declaration that is important wins");
        css = "@page :blank { margin-top: 5in } @page :right { margin-top: 1in }";
        Check.near(480, box(css, null, 2, true, true).getMargin(0), 0.01, ":blank is more specific than :right");
        Check.near(96, box(css, null, 2, false, true).getMargin(0), 0.01, "a page that is not blank");
        Check.near(0, box(css, null, 2, false, false).getMargin(0), 0.01, "a left page that no rule reaches has the initial margin");
    }

    /**
     * <code>size</code>: <code>auto</code> is the default sheet, a media name its size, a media name with an orientation turns it, lengths give
     * the width and the height (one length is a square), and invalid values are dropped.
     * <p>
     * <code>size</code>: <code>auto</code> 는 기본 용지, 용지 이름은 그 크기, 방향이 붙으면 돌리고, 길이는 너비와 높이이며(하나면 정사각형) 올바르지 않은 값은 버린다.
     */
    public void testPageSize() {
        double mm = 96 / 25.4;
        PageStyles.PageBox auto = box("@page { size: auto }", null, 1, false, true);
        Check.near(210 * mm, auto.getWidth(), 0.01, "auto is A4 in this engine: width");
        Check.near(297 * mm, auto.getHeight(), 0.01, "auto is A4: height");
        Check.isFalse(auto.isFixedSize(), "auto is not a fixed size");
        Check.near(210 * mm, box("", null, 1, false, true).getWidth(), 0.01, "without a rule");
        PageStyles.PageBox a3 = box("@page { size: A3 }", null, 1, false, true);
        Check.near(297 * mm, a3.getWidth(), 0.01, "a3 width");
        Check.near(420 * mm, a3.getHeight(), 0.01, "a3 height");
        Check.isTrue(a3.isFixedSize(), "a media name fixes the size");
        PageStyles.PageBox land = box("@page { size: A4 landscape }", null, 1, false, true);
        Check.near(297 * mm, land.getWidth(), 0.01, "A4 landscape: width (the example of the specification)");
        Check.near(210 * mm, land.getHeight(), 0.01, "A4 landscape: height");
        PageStyles.PageBox port = box("@page { size: landscape A4 portrait }", null, 1, false, true);
        Check.near(210 * mm, port.getWidth(), 0.01, "an invalid size (two orientations) is dropped, so the initial auto remains: width");
        PageStyles.PageBox let = box("@page { size: letter }", null, 1, false, true);
        Check.near(8.5 * 96, let.getWidth(), 0.01, "letter width");
        Check.near(11 * 96, let.getHeight(), 0.01, "letter height");
        PageStyles.PageBox ledger = box("@page { size: ledger landscape }", null, 1, false, true);
        Check.near(17 * 96, ledger.getWidth(), 0.01, "ledger landscape width");
        Check.near(11 * 96, ledger.getHeight(), 0.01, "ledger landscape height");
        PageStyles.PageBox only = box("@page { size: landscape }", null, 1, false, true);
        Check.near(297 * mm, only.getWidth(), 0.01, "only an orientation turns the default sheet");
        PageStyles.PageBox len = box("@page { size: 4in 6in }", null, 1, false, true);
        Check.near(384, len.getWidth(), 0.01, "two lengths: width");
        Check.near(576, len.getHeight(), 0.01, "two lengths: height");
        PageStyles.PageBox sq = box("@page { size: 10cm }", null, 1, false, true);
        Check.near(sq.getWidth(), sq.getHeight(), 0.001, "one length is a square");
        Check.near(10 * 96 / 2.54, sq.getWidth(), 0.01, "the square has that side");
        PageStyles.PageBox bad = box("@page { size: -1in }", null, 1, false, true);
        Check.near(210 * mm, bad.getWidth(), 0.01, "a negative length is dropped");
        PageStyles.PageBox bad2 = box("@page { size: 4in a4 }", null, 1, false, true);
        Check.near(210 * mm, bad2.getWidth(), 0.01, "a length with a media name is dropped");
        PageStyles.PageBox em = box("@page { size: 10em 20em }", null, 1, false, true);
        Check.near(160, em.getWidth(), 0.01, "em refers to the page context font (16px here)");
    }

    /**
     * The percentage margin is relative to the page size (the example of the specification: 10% of A4 is 21mm by 29.7mm, leaving a page area of
     * 168mm by 237.6mm once both sides are taken off), lengths are made absolute, and the page area is the page box without the margins.
     * <p>
     * 백분율 여백은 쪽 크기 기준이다(규격 예: A4 의 10% 는 21mm 와 29.7mm 이고 양쪽 여백을 모두 빼면 쪽 영역은 168mm x 237.6mm). 길이는 절대 값이 되며 쪽 영역은 여백을 뺀 쪽 상자이다.
     */
    public void testMargins() {
        double mm = 96 / 25.4;
        PageStyles.PageBox b = box("@page { size: auto; margin: 10% }", null, 1, false, true);
        Check.near(29.7 * mm, b.getMargin(0), 0.01, "the top margin follows the height");
        Check.near(21 * mm, b.getMargin(1), 0.01, "the right margin follows the width");
        Check.near(29.7 * mm, b.getMargin(2), 0.01, "the bottom margin follows the height");
        Check.near(21 * mm, b.getMargin(3), 0.01, "the left margin follows the width");
        Check.near(168 * mm, b.getAreaWidth(), 0.01, "the page area width (the specification text subtracts one margin only, 189mm; both sides give 168mm)");
        Check.near(237.6 * mm, b.getAreaHeight(), 0.01, "the page area height (267.3mm in the specification text, which subtracts one margin only)");
        PageStyles.PageBox m = box("@page { margin: 1cm 2cm 3cm 4cm }", null, 1, false, true);
        Check.near(1 * 96 / 2.54, m.getMargin(0), 0.01, "top");
        Check.near(2 * 96 / 2.54, m.getMargin(1), 0.01, "right");
        Check.near(3 * 96 / 2.54, m.getMargin(2), 0.01, "bottom");
        Check.near(4 * 96 / 2.54, m.getMargin(3), 0.01, "left");
        PageStyles.PageBox calc = box("@page { margin-left: calc(1in + 2px) }", null, 1, false, true);
        Check.near(98, calc.getMargin(3), 0.01, "a calc() margin");
        PageStyles.PageBox big = box("@page { size: 100px 100px; margin: 80px }", null, 1, false, true);
        Check.near(0, big.getAreaWidth(), 0.001, "margins larger than the page leave no page area");
    }

    /**
     * <code>marks</code>, <code>bleed</code> and <code>page-orientation</code>: the computed values, the bleed of <code>auto</code> is 6pt with crop
     * marks and zero otherwise, and a negative bleed is allowed.
     * <p>
     * <code>marks</code>, <code>bleed</code>, <code>page-orientation</code>: 계산 값, <code>auto</code> 의 재단 여유는 자르기 표시가 있으면 6pt 이고 아니면 0 이며 음수도 허용한다.
     */
    public void testMarksBleedAndOrientation() {
        PageStyles.PageBox none = box("@page { size: a4 }", null, 1, false, true);
        Check.equal("none", none.getMarks(), "marks is none by default");
        Check.near(0, none.getBleed(), 0.001, "the bleed without crop marks");
        Check.equal("upright", none.getOrientation(), "the default orientation");
        PageStyles.PageBox crop = box("@page { marks: crop }", null, 1, false, true);
        Check.equal("crop", crop.getMarks(), "crop");
        Check.near(8, crop.getBleed(), 0.001, "6pt is 8px");
        Check.equal("crop cross", box("@page { marks: cross crop }", null, 1, false, true).getMarks(), "both marks in the canonical order");
        Check.equal("cross", box("@page { marks: cross }", null, 1, false, true).getMarks(), "cross");
        Check.near(0, box("@page { marks: cross }", null, 1, false, true).getBleed(), 0.001, "cross marks without crop do not bleed");
        Check.near(16, box("@page { marks: crop; bleed: 1em }", null, 1, false, true).getBleed(), 0.001, "an explicit bleed");
        Check.near(-4, box("@page { bleed: -4px }", null, 1, false, true).getBleed(), 0.001, "a negative bleed");
        Check.equal("rotate-left", box("@page { page-orientation: rotate-left }", null, 1, false, true).getOrientation(), "rotate-left");
        Check.equal("upright", box("@page { page-orientation: sideways }", null, 1, false, true).getOrientation(), "an invalid orientation");
        Check.equal("none", box("@page { marks: crop, cross }", null, 1, false, true).getMarks(), "an invalid marks value is dropped");
    }

    /**
     * The <code>page</code> property on elements is parsed (a name or <code>auto</code>) and is not inherited; the rules in a conditional group
     * apply only when the group does; <code>@page</code> does not change the style of the elements.
     * <p>
     * 요소의 <code>page</code> 속성은 파싱하며(이름 또는 <code>auto</code>) 상속되지 않는다. 조건 그룹 안의 규칙은 그룹이 적용될 때만 적용되며 <code>@page</code> 는
     * 요소의 스타일을 바꾸지 않는다.
     */
    public void testPageProperty() {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>p { page: chapter } #q { page: auto } @page chapter { size: a5 } @media print { @page { size: a3 } } @page { color: red }</style></head><body><p id=p><span id=s>x</span></p><p id=q></p></body></html>",
                "http://t.test/page.html", null, true);
        CssEngine e = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        e.loadStyleSheets();
        Check.equal("chapter", e.getComputedStyle(doc.getElementById("p")).getPropertyValue("page"), "the page property of an element");
        Check.equal("auto", e.getComputedStyle(doc.getElementById("s")).getPropertyValue("page"), "page is not inherited");
        Check.equal("auto", e.getComputedStyle(doc.getElementById("q")).getPropertyValue("page"), "auto");
        Check.equal("rgb(0, 0, 0)", e.getComputedStyle(doc.getElementById("p")).getPropertyValue("color"), "@page does not style the elements");
        List<PageRule> rules = e.getResolver().getPageRules();
        Check.equal(2, rules.size(), "the @page rule of the print group is not collected for the screen");
        PageStyles.PageBox named = PageStyles.resolve(rules, "chapter", 1, false, true);
        Check.near(148 * 96 / 25.4, named.getWidth(), 0.01, "the named page uses the named rule");
        PageStyles.PageBox plain = PageStyles.resolve(rules, null, 1, false, true);
        Check.near(210 * 96 / 25.4, plain.getWidth(), 0.01, "the unnamed page does not");
    }
}
