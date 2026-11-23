package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the CSS part of the SVG decoder (plan item P18-04): declarations, selectors with their specificity, the cascade of presentation
 * attributes, style sheets and style attributes, media queries, validity of values and the computed style with inheritance and custom
 * properties.
 * <p>
 * SVG 디코더의 CSS 부분 시험(계획 P18-04): 선언, 구체성을 가진 선택자, 표현 속성/스타일시트/style 속성의 연쇄, 미디어 질의, 값의 유효성, 상속과
 * 사용자 정의 속성을 가진 계산된 스타일.
 */
public class SvgCssTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgCssTest() {
    }

    /**
     * Parses a document and runs the cascade.
     * <p>
     * 문서를 해석하고 연쇄를 실행한다.
     *
     * @param body the content of an svg root element / svg 루트 요소의 내용
     * @return the root element with its declared properties / 선언된 속성을 가진 루트 요소
     * @throws Exception on a failure / 실패 시
     */
    private static SvgElement cascaded(String body) throws Exception {
        SvgElement root = SvgXmlParser.parse(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">" + body + "</svg>"));
        SvgCss.cascade(root, new SvgCss.MediaEnv(100, 100));
        return root;
    }

    /**
     * Declarations: importance, strings and urls with semicolons, comments, case of names.
     * <p>
     * 선언: 중요도, 세미콜론이 든 문자열과 url, 주석, 이름의 대소문자.
     */
    public void testDeclarations() {
        List<SvgCss.Decl> d = SvgCss.parseDeclarations("FILL: Red !important; stroke:url(\"a;b\") ; /* c: d */ --My-Var : 1px;;font-family: 'a;b', serif;bad;: x;y:");
        Check.equal(4, d.size(), "four valid declarations");
        Check.equal("fill", d.get(0).name, "names are lower case");
        Check.equal("Red", d.get(0).value, "value without !important");
        Check.isTrue(d.get(0).important, "important");
        Check.equal("url(\"a;b\")", d.get(1).value, "semicolon inside a url string");
        Check.equal("--My-Var", d.get(2).name, "custom property names keep their case");
        Check.equal("'a;b', serif", d.get(3).value, "semicolon inside a string");
        Check.isFalse(d.get(1).important, "not important");
    }

    /**
     * Selectors match elements and have the specificity of the specification.
     * <p>
     * 선택자는 요소와 맞으며 규격의 구체성을 가진다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testSelectors() throws Exception {
        SvgElement r = SvgXmlParser.parse(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\"><g id=\"g1\" class=\"a b\"><rect id=\"r1\" class=\"x\" data-k=\"v-1 w\"/>"
                + "<circle/><rect id=\"r2\"/></g><path/></svg>"));
        SvgElement g = r.children.get(0);
        SvgElement r1 = g.children.get(0);
        SvgElement circle = g.children.get(1);
        SvgElement r2 = g.children.get(2);
        String[][] cases = {
                { "rect", "r1", "t" }, { "g rect", "r1", "t" }, { "svg > g > rect", "r1", "t" }, { "svg > rect", "r1", "f" }, { ".x", "r1", "t" },
                { "g.a.b", "g", "t" }, { "#r1.x", "r1", "t" }, { "[data-k]", "r1", "t" }, { "[data-k=\"v-1 w\"]", "r1", "t" },
                { "[data-k~=w]", "r1", "t" }, { "[data-k|=v]", "r1", "t" }, { "[data-k^=v-]", "r1", "t" }, { "[data-k$=w]", "r1", "t" },
                { "[data-k*=\"1 \"]", "r1", "t" }, { "[data-k=V-1 i]", "r1", "f" }, { "rect + circle", "circle", "t" }, { "rect ~ rect", "r2", "t" },
                { "circle + rect", "r2", "t" }, { "rect:first-child", "r1", "t" }, { "rect:last-child", "r2", "t" }, { "rect:nth-child(3)", "r2", "t" },
                { "*:nth-child(2n)", "circle", "t" }, { "rect:nth-of-type(2)", "r2", "t" }, { "rect:only-of-type", "r1", "f" }, { "path:only-child", "path", "f" },
                { ":not(rect)", "circle", "t" }, { ":not(.x, circle)", "r2", "t" }, { ":is(circle, #r2)", "r2", "t" }, { ":root", "svg", "t" },
                { "rect:hover", "r1", "f" }, { "rect:focus", "r1", "f" }, { "*", "circle", "t" }, { "svg|rect", "r1", "t" }, { "g .x", "r1", "t" },
                { "g  >  rect", "r1", "t" } };
        // 선택자마다 대상 요소와 맞는지 확인한다.
        for (int i = 0; i < cases.length; i++) {
            SvgElement e = "svg".equals(cases[i][1]) ? r : ("g".equals(cases[i][1]) ? g : ("r1".equals(cases[i][1]) ? r1
                    : ("r2".equals(cases[i][1]) ? r2 : ("path".equals(cases[i][1]) ? r.children.get(1) : circle))));
            List<SvgCss.Selector> sel = SvgCss.parseSelectorList(cases[i][0]);
            Check.notNull(sel, "selector parses: " + cases[i][0]);
            Check.equal("t".equals(cases[i][2]), SvgCss.matches(sel.get(0), e), "selector " + cases[i][0]);
        }
        Check.isNull(SvgCss.parseSelectorList("rect::before"), "pseudo-elements invalidate the rule");
        Check.isNull(SvgCss.parseSelectorList("rect:bogus"), "unknown pseudo-class");
        Check.isNull(SvgCss.parseSelectorList("> rect"), "leading combinator");
        Check.isNull(SvgCss.parseSelectorList("rect >"), "trailing combinator");
        Check.isNull(SvgCss.parseSelectorList("a, , b"), "empty selector in a list");
        Check.isNull(SvgCss.parseSelectorList("rect[a"), "unterminated attribute selector");
        Check.isNull(SvgCss.parseSelectorList(".1a!"), "bad class");
        Check.equal(2, SvgCss.parseSelectorList("a, b").size(), "selector list");
    }

    /**
     * Specificity values and their order.
     * <p>
     * 구체성 값과 그 순서.
     */
    public void testSpecificity() {
        int star = SvgCss.parseSelectorList("*").get(0).specificity;
        int type = SvgCss.parseSelectorList("rect").get(0).specificity;
        int cls = SvgCss.parseSelectorList(".a").get(0).specificity;
        int id = SvgCss.parseSelectorList("#a").get(0).specificity;
        Check.equal(0, star, "universal");
        Check.equal(1, type, "type");
        Check.equal(1 << 8, cls, "class");
        Check.equal(1 << 16, id, "id");
        Check.equal((1 << 16) | (2 << 8) | 1, SvgCss.parseSelectorList("rect.a.b#c").get(0).specificity, "id, two classes and a type");
        Check.equal(1 << 8, SvgCss.parseSelectorList("[a]").get(0).specificity, "attribute selector counts as a class");
        Check.equal(1 << 8, SvgCss.parseSelectorList(":first-child").get(0).specificity, "pseudo-class");
        Check.equal(1 << 16, SvgCss.parseSelectorList(":not(#a)").get(0).specificity, ":not takes the specificity of its argument");
        Check.equal(0, SvgCss.parseSelectorList(":where(#a)").get(0).specificity, ":where has none");
        Check.equal(1 << 16, SvgCss.parseSelectorList(":is(.a, #b)").get(0).specificity, ":is takes the largest argument");
        Check.isTrue(id > cls && cls > type && type > star, "order");
    }

    /**
     * The cascade: presentation attribute < style sheet < style attribute < important; specificity and source order decide inside a level.
     * <p>
     * 연쇄: 표현 속성 < 스타일시트 < style 속성 < important. 같은 수준에서는 구체성과 소스 순서가 정한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testCascade() throws Exception {
        SvgElement r = cascaded("<style>rect { fill: #111 } .a { fill: #222 } #i { fill: #333 } rect.a { stroke: #444 } rect { stroke: #555 !important }"
                + " .b { opacity: .1 } .b { opacity: .2 }</style>"
                + "<rect id=\"p\" fill=\"#999\" stroke=\"#888\"/><rect class=\"a\" fill=\"#999\"/><rect id=\"i\" class=\"a\" style=\"fill:#666\"/>"
                + "<rect id=\"j\" class=\"a\" style=\"fill:#666 !important\" /><rect class=\"b\" style=\"stroke: #777\"/>");
        Check.equal("#111", r.children.get(1).declared.get("fill"), "style sheet beats the presentation attribute");
        Check.equal("#555", r.children.get(1).declared.get("stroke"), "important beats later normal declarations");
        Check.equal("#222", r.children.get(2).declared.get("fill"), "class beats type");
        Check.equal("#666", r.children.get(3).declared.get("fill"), "style attribute beats the id selector");
        Check.equal("#666", r.children.get(4).declared.get("fill"), "important in the style attribute");
        Check.equal("#555", r.children.get(5).declared.get("stroke"), "a stylesheet important declaration beats the normal style attribute");
        Check.equal(".2", r.children.get(5).declared.get("opacity"), "the later rule wins at equal specificity");
        Check.isNull(r.children.get(0).declared.get("width"), "attributes that are not presentation attributes are not declared");
    }

    /**
     * Values that CSS would drop at parse time are dropped; the earlier valid declaration stays.
     * <p>
     * CSS 가 해석 시점에 버릴 값은 버려지고 앞선 올바른 선언이 남는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testInvalidValuesAreDropped() throws Exception {
        SvgElement r = cascaded("<style>rect { fill: #c00; fill: bogus; stroke-width: -3px; stroke-linecap: sideways; opacity: 2x }</style>"
                + "<rect fill=\"nonsense\" stroke-width=\"-1\" stroke-miterlimit=\"0.5\"/>");
        SvgElement e = r.children.get(1);
        Check.equal("#c00", e.declared.get("fill"), "the invalid fill is ignored, the earlier one stays");
        Check.isNull(e.declared.get("stroke-width"), "negative stroke width");
        Check.isNull(e.declared.get("stroke-linecap"), "unknown keyword");
        Check.isNull(e.declared.get("opacity"), "bad number");
        Check.isNull(e.declared.get("stroke-miterlimit"), "miter limit below 1");
        Check.isTrue(SvgCss.isValidValue("fill", "url(#a) red"), "paint with fallback");
        Check.isTrue(SvgCss.isValidValue("fill", "var(--x)"), "var() is accepted and checked later");
        Check.isTrue(SvgCss.isValidValue("fill", "inherit"), "css-wide keyword");
        Check.isTrue(SvgCss.isValidValue("stroke-dasharray", "1,2 3"), "dash array");
        Check.isFalse(SvgCss.isValidValue("stroke-dasharray", "1,x"), "bad dash array");
        Check.isFalse(SvgCss.isValidValue("fill", ""), "empty");
        Check.isTrue(SvgCss.isValidValue("some-future-property", "anything"), "unknown properties are not checked");
    }

    /**
     * Media queries: types, features, comparisons, lists and negation. The colour scheme is light.
     * <p>
     * 미디어 질의: 유형, 기능, 비교, 목록, 부정. 색 구성표는 밝음이다.
     */
    public void testMediaQueries() {
        SvgCss.MediaEnv env = new SvgCss.MediaEnv(200, 100);
        Check.isTrue(SvgCss.evalMedia("", env), "empty");
        Check.isTrue(SvgCss.evalMedia("all", env), "all");
        Check.isTrue(SvgCss.evalMedia("screen", env), "screen");
        Check.isFalse(SvgCss.evalMedia("print", env), "print");
        Check.isTrue(SvgCss.evalMedia("(min-width: 200px)", env), "min-width equal");
        Check.isFalse(SvgCss.evalMedia("(min-width: 201px)", env), "min-width greater");
        Check.isTrue(SvgCss.evalMedia("(max-width: 200px) and (min-height: 50px)", env), "and");
        Check.isTrue(SvgCss.evalMedia("print, (max-height: 100px)", env), "list");
        Check.isTrue(SvgCss.evalMedia("not print", env), "not");
        Check.isFalse(SvgCss.evalMedia("not screen", env), "not screen");
        Check.isTrue(SvgCss.evalMedia("only screen and (orientation: landscape)", env), "orientation");
        Check.isFalse(SvgCss.evalMedia("(orientation: portrait)", env), "portrait");
        Check.isTrue(SvgCss.evalMedia("(prefers-color-scheme: light)", env), "light scheme");
        Check.isFalse(SvgCss.evalMedia("(prefers-color-scheme: dark)", env), "dark scheme");
        Check.isTrue(SvgCss.evalMedia("(min-resolution: 1dppx)", env), "resolution");
        Check.isFalse(SvgCss.evalMedia("(min-resolution: 2dppx)", env), "high resolution");
        Check.isTrue(SvgCss.evalMedia("(min-width: 10em)", env), "em units");
        Check.isFalse(SvgCss.evalMedia("(unknown-feature: 1)", env), "unknown feature");
        Check.isTrue(SvgCss.evalMedia("(prefers-reduced-motion: no-preference)", env), "motion");
    }

    /**
     * Rules inside <code>@media</code> blocks apply only if the query matches; other at-rules are skipped.
     * <p>
     * <code>@media</code> 블록 안의 규칙은 질의가 맞을 때만 적용되고 다른 at-규칙은 건너뛴다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testAtRules() throws Exception {
        SvgElement r = cascaded("<style>@charset \"utf-8\"; @import url(x.css); @media print { rect { fill: #001 } } @media (min-width: 50px) { rect { fill: #002 } }"
                + " @font-face { font-family: F; src: url(f.woff) } @keyframes k { from { fill: #003 } to { fill: #004 } } rect { stroke: #005 }"
                + " @media (max-width: 10px) { rect { stroke: #006 } }</style><rect/>");
        SvgElement e = r.children.get(1);
        Check.equal("#002", e.declared.get("fill"), "matching media block applies, print does not");
        Check.equal("#005", e.declared.get("stroke"), "the non-matching media block does not apply; keyframes and font-face are skipped");
        SvgElement media = SvgXmlParser.parse(SvgTestKit.utf8("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\"><style media=\"(min-width: 100px)\">rect { fill: red }</style>"
                + "<style type=\"text/plain\">rect { stroke: red }</style><style>rect { opacity: .5 }</style><rect/></svg>"));
        SvgCss.cascade(media, new SvgCss.MediaEnv(20, 20));
        SvgElement rect = media.children.get(3);
        Check.isNull(rect.declared.get("fill"), "style element with a media attribute that does not match");
        Check.isNull(rect.declared.get("stroke"), "style element with another type");
        Check.equal(".5", rect.declared.get("opacity"), "plain style element");
    }

    /**
     * Computed style: inheritance, keywords, custom properties, currentColor and units.
     * <p>
     * 계산된 스타일: 상속, 키워드, 사용자 정의 속성, currentColor, 단위.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testComputedStyle() throws Exception {
        SvgElement r = cascaded("<g color=\"#102030\" fill=\"currentColor\" stroke=\"#456\" stroke-width=\"3\" opacity=\".5\" font-size=\"20px\" style=\"--k: 7px; --m: var(--k)\">"
                + "<rect id=\"a\" font-size=\"2em\" stroke-width=\"var(--m)\" fill=\"var(--none, #abc)\"/>"
                + "<rect id=\"b\" color=\"#fff\" stroke=\"inherit\" opacity=\"inherit\" fill=\"initial\"/>"
                + "<rect id=\"c\" stroke=\"unset\" font-size=\"50%\" stroke-width=\"1em\" style=\"fill: var(--undefined)\"/>"
                + "<rect id=\"d\" font-size=\"larger\" fill=\"url(#x) currentColor\"/></g>");
        SvgStyle base = new SvgStyle(100, 100);
        SvgElement g = r.children.get(0);
        SvgStyle gs = new SvgStyle(base, g, 100, 100);
        SvgStyle a = new SvgStyle(gs, g.children.get(0), 100, 100);
        SvgStyle b = new SvgStyle(gs, g.children.get(1), 100, 100);
        SvgStyle c = new SvgStyle(gs, g.children.get(2), 100, 100);
        SvgStyle d = new SvgStyle(gs, g.children.get(3), 100, 100);
        Check.near(40, a.fontSize, 1e-9, "2em of 20px");
        Check.near(10, c.fontSize, 1e-9, "50% of 20px");
        Check.near(24, d.fontSize, 1e-9, "larger is 1.2 times");
        Check.equal(0xFFAABBCCL, a.paint("fill").argb & 0xFFFFFFFFL, "var() fallback");
        Check.near(7, a.px(a.length("stroke-width"), 2), 1e-9, "a custom property that refers to another one");
        Check.equal(0xFF102030L, gs.paint("fill").argb & 0xFFFFFFFFL, "currentColor takes the color of the element");
        Check.equal(0xFFFFFFFFL, b.color() & 0xFFFFFFFFL, "own color");
        Check.equal(0xFF445566L, b.paint("stroke").argb & 0xFFFFFFFFL, "stroke: inherit");
        Check.near(0.5, b.opacity("opacity", 1), 1e-9, "opacity: inherit takes the parent value although opacity does not inherit");
        Check.near(1, c.opacity("opacity", 1), 1e-9, "opacity is not inherited by default");
        Check.equal(0xFF000000L, b.paint("fill").argb & 0xFFFFFFFFL, "fill: initial is black");
        Check.equal(0xFF445566L, c.paint("stroke").argb & 0xFFFFFFFFL, "stroke: unset inherits");
        Check.near(10, c.px(c.length("stroke-width"), 2), 1e-9, "1em of the computed font size");
        Check.equal(0xFF102030L, c.paint("fill").argb & 0xFFFFFFFFL, "a var() without value and fallback drops the declaration, the inherited fill stays");
        Check.equal(SvgPaint.URL, d.paint("fill").kind, "url paint");
        Check.equal(0xFF102030L, d.paint("fill").fallback.argb & 0xFFFFFFFFL, "currentColor in the fallback");
        Check.isTrue(SvgStyle.isInherited("fill") && !SvgStyle.isInherited("opacity") && SvgStyle.isInherited("--any"), "inheritance table");
        Check.equal("a b", SvgStyle.urlId("url( \"#a b\" )"), "url with quotes and spaces");
        Check.isNull(SvgStyle.urlId("url(http://x/y#z)"), "only fragment references count");
        Check.equal("z", SvgStyle.urlId("URL(#z)"), "case insensitive function name");
    }
}
