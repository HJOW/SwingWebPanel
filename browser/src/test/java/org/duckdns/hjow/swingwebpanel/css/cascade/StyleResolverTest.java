package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssDiagnostics;
import org.duckdns.hjow.swingwebpanel.css.MiniHtml;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.selector.SimpleElementState;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the style resolver (plan items P08-04, P08-06, P08-07): the cascade (origin, importance, specificity, order, inline style, presentational hints),
 * pseudo-element styles, the user agent defaults, media dependent rules, dynamic state, DOM mutation invalidation, quirks mode, disposal and robustness
 * on large and deep trees.
 * <p>
 * 스타일 리졸버 시험(계획 P08-04, P08-06, P08-07): 캐스케이드(출처, 중요도, 구체성, 순서, 인라인 스타일, 표현 힌트), 의사 요소 스타일, 사용자 에이전트 기본값,
 * 미디어에 의존하는 규칙, 동적 상태, DOM 변경에 의한 무효화, 호환 모드, 폐기, 크고 깊은 트리에서의 견고성.
 */
public class StyleResolverTest {

    /** The document of the current case. / 현재 사례의 문서. */
    private Document doc;

    /** The resolver of the current case. / 현재 사례의 리졸버. */
    private StyleResolver r;

    /** The environment of the current case. / 현재 사례의 환경. */
    private MediaEnvironment env;

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public StyleResolverTest() {
    }

    /**
     * Frees the resolver after each case. 사례마다 리졸버를 해제한다.
     */
    public void tearDown() {
        if (r != null) {
            r.dispose();
        }
    }

    /**
     * Creates the document and the resolver of a case. 사례의 문서와 리졸버를 만든다.
     *
     * @param body   the markup inside body / body 안의 마크업
     * @param sheets author style sheets / 작성자 스타일 시트
     */
    private void init(String body, String... sheets) {
        doc = MiniHtml.parse("<html><head></head><body>" + body + "</body></html>");
        env = new MediaEnvironment(800, 600);
        r = new StyleResolver(doc, env);
        List<StyleSheet> list = new ArrayList<StyleSheet>();
        // 문자열마다 작성자 스타일 시트를 만든다.
        for (int i = 0; i < sheets.length; i++) {
            list.add(StyleSheet.parse(sheets[i], "http://example.com/s" + i + ".css", StyleSheet.ORIGIN_AUTHOR, null, null));
        }
        r.setStyleSheets(list);
    }

    /**
     * Returns the computed style of the element with an id. id 가 있는 요소의 계산 스타일을 반환한다.
     *
     * @param id the id / id
     * @return the style / 스타일
     */
    private ComputedStyle s(String id) {
        return r.getComputedStyle(doc.getElementById(id));
    }

    /**
     * Returns the color of an element as text. 요소의 색을 문자열로 반환한다.
     *
     * @param id the id / id
     * @return the rgb text / rgb 문자열
     */
    private String color(String id) {
        return s(id).getPropertyValue("color");
    }

    /**
     * Specificity and source order. 구체성과 소스 순서.
     */
    public void testSpecificityAndOrder() {
        init("<p id=\"a\" class=\"c\">x</p><p id=\"b\" class=\"c\">y</p>", "p { color: red } .c { color: green } #a { color: blue }");
        Check.equal("rgb(0, 0, 255)", color("a"), "an id beats a class and a type");
        Check.equal("rgb(0, 128, 0)", color("b"), "a class beats a type");
        tearDown();
        init("<p id=\"a\">x</p>", "p { color: red } p { color: blue }");
        Check.equal("rgb(0, 0, 255)", color("a"), "later wins at equal specificity");
        tearDown();
        init("<p id=\"a\">x</p>", "p { color: red }", "p { color: blue }");
        Check.equal("rgb(0, 0, 255)", color("a"), "later sheet wins at equal specificity");
        tearDown();
        init("<div id=\"d\"><p id=\"a\">x</p></div>", "div p { color: red } p { color: blue }");
        Check.equal("rgb(255, 0, 0)", color("a"), "two types beat one type regardless of order");
        tearDown();
        init("<p id=\"a\" class=\"x y\">x</p>", ".x.y { color: red } .x { color: blue } [class] { color: green }");
        Check.equal("rgb(255, 0, 0)", color("a"), "two classes beat one class");
        tearDown();
        init("<p id=\"a\" class=\"x\">x</p>", ":is(#nothing, .x) { color: red } p.x { color: blue }");
        Check.equal("rgb(255, 0, 0)", color("a"), ":is() takes the specificity of its most specific argument, even one that does not match");
        tearDown();
        init("<p id=\"a\" class=\"x\">x</p>", ":where(#a) { color: red } p { color: blue }");
        Check.equal("rgb(0, 0, 255)", color("a"), ":where() has no specificity");
    }

    /**
     * Importance and the origins. 중요도와 출처.
     */
    public void testImportanceAndOrigins() {
        init("<div id=\"a\" style=\"color: red\">x</div>", "#a { color: blue }");
        Check.equal("rgb(255, 0, 0)", color("a"), "the style attribute beats any selector");
        tearDown();
        init("<div id=\"a\" style=\"color: red\">x</div>", "#a { color: blue !important }");
        Check.equal("rgb(0, 0, 255)", color("a"), "!important in a sheet beats a normal inline declaration");
        tearDown();
        init("<div id=\"a\" style=\"color: red !important\">x</div>", "#a { color: blue !important }");
        Check.equal("rgb(255, 0, 0)", color("a"), "inline !important beats sheet !important");
        tearDown();
        init("<div id=\"a\">x</div>", "div { display: inline }");
        Check.equal("inline", s("a").display(), "an author rule beats the user agent rule");
        tearDown();
        init("<div id=\"a\">x</div>");
        Check.equal("block", s("a").display(), "the user agent sheet makes div a block");
        tearDown();
        init("<input id=\"a\" type=\"hidden\">", "input { display: block !important }");
        Check.equal("none", s("a").display(), "a user agent !important rule beats author !important");
        tearDown();
        init("<p id=\"a\">x</p>", "p { color: red !important; color: blue }");
        Check.equal("rgb(255, 0, 0)", color("a"), "within a block the important declaration wins regardless of order");
        tearDown();
        init("<p id=\"a\">x</p>");
        r.setUserStyleSheets(Arrays.asList(StyleSheet.parse("p { color: green; margin-top: 3px !important }", null, StyleSheet.ORIGIN_USER, null, null)));
        Check.equal("rgb(0, 128, 0)", color("a"), "a user rule beats the user agent");
        r.setStyleSheets(Arrays.asList(StyleSheet.parse("p { color: red; margin-top: 9px !important }", null, StyleSheet.ORIGIN_AUTHOR, null, null)));
        Check.equal("rgb(255, 0, 0)", color("a"), "an author normal rule beats a user normal rule");
        Check.near(3, s("a").px(Prop.MARGIN_TOP), 1e-9, "a user !important rule beats an author !important rule");
        tearDown();
        init("<p id=\"a\">x</p>", "p { color: red; color: revert } ");
        Check.equal("rgb(0, 0, 0)", color("a"), "revert falls back to the user agent value (the initial color)");
        tearDown();
        init("<a id=\"a\" href=\"/x\">x</a>", "a { color: revert }");
        Check.equal("rgb(0, 0, 238)", color("a"), "revert on a link gives the user agent link color");
        tearDown();
        init("<div id=\"a\" style=\"color: red; margin: 5px\">x</div>", "div { all: initial }");
        Check.equal("inline", s("a").display(), "all: initial resets display");
        Check.equal("rgb(255, 0, 0)", color("a"), "but the inline style still applies");
    }

    /**
     * Presentational hints are the weakest author declarations. 표현 힌트는 가장 약한 작성자 선언이다.
     */
    public void testPresentationalHints() {
        init("<table id=\"t\" border=\"2\" bgcolor=\"#ff0000\" cellpadding=\"4\" cellspacing=\"6\" width=\"50%\"><tbody><tr><td id=\"c\" align=\"right\" valign=\"top\""
                + " nowrap width=\"30\">x</td></tr></tbody></table>");
        Check.near(2, s("t").px(Prop.BORDER_TOP_WIDTH), 1e-9, "table border");
        Check.equal("outset", s("t").keyword(Prop.BORDER_TOP_STYLE), "table border style");
        Check.equal("rgb(255, 0, 0)", s("t").getPropertyValue("background-color"), "bgcolor");
        Check.equal("50%", s("t").get(Prop.WIDTH).toCssText(), "width in percent");
        Check.equal("6px", s("t").get(Prop.BORDER_SPACING).toCssText(), "border-spacing is set");
        Check.near(4, s("c").px(Prop.PADDING_LEFT), 1e-9, "cellpadding reaches the cells");
        Check.near(1, s("c").px(Prop.BORDER_LEFT_WIDTH), 1e-9, "a bordered table gives its cells a border");
        Check.equal("right", s("c").keyword(Prop.TEXT_ALIGN), "align");
        Check.equal("top", s("c").keyword(Prop.VERTICAL_ALIGN), "valign");
        Check.equal("nowrap", s("c").keyword(Prop.WHITE_SPACE), "nowrap");
        Check.near(30, s("c").px(Prop.WIDTH), 1e-9, "a plain number is pixels");
        tearDown();
        init("<table id=\"t\" bgcolor=\"red\"></table>", "table { background-color: blue }");
        Check.equal("rgb(0, 0, 255)", s("t").getPropertyValue("background-color"), "any author rule beats a presentational hint");
        tearDown();
        init("<font id=\"f\" color=\"red\" face=\"Arial, sans-serif\" size=\"5\">x</font><font id=\"g\" size=\"+2\">y</font>");
        Check.equal("rgb(255, 0, 0)", color("f"), "font color");
        Check.equal("Arial", s("f").fontFamilies().get(0), "font face");
        Check.near(24, s("f").fontSizePx(), 1e-9, "font size 5 is x-large");
        Check.near(24, s("g").fontSizePx(), 1e-9, "font size +2 is level 5");
        tearDown();
        init("<img id=\"i\" width=\"100\" height=\"50\" hspace=\"3\" vspace=\"4\" border=\"1\" align=\"left\">");
        Element bodyElement = doc.getDocumentElement().getLastElementChild();
        bodyElement.setAttribute("bgcolor", "yellow");
        bodyElement.setAttribute("text", "blue");
        bodyElement.setAttribute("leftmargin", "0");
        ComputedStyle bs = r.getComputedStyle(bodyElement);
        Check.equal("rgb(255, 255, 0)", bs.getPropertyValue("background-color"), "body bgcolor");
        Check.equal("rgb(0, 0, 255)", bs.getPropertyValue("color"), "body text");
        Check.near(0, bs.px(Prop.MARGIN_LEFT), 1e-9, "body leftmargin overrides the user agent margin");
        Check.near(8, bs.px(Prop.MARGIN_TOP), 1e-9, "the other margins keep the default");
        Check.near(100, s("i").px(Prop.WIDTH), 1e-9, "image width");
        Check.near(3, s("i").px(Prop.MARGIN_LEFT), 1e-9, "hspace");
        Check.near(4, s("i").px(Prop.MARGIN_TOP), 1e-9, "vspace");
        Check.equal("left", s("i").keyword(Prop.FLOAT), "align left floats the image");
        tearDown();
        init("<p id=\"p\">x</p><ol id=\"o\" type=\"a\"><li id=\"l\">1</li></ol><hr id=\"h\" width=\"50\" noshade><br id=\"br\" clear=\"all\">");
        Check.equal("lower-alpha", s("o").keyword(Prop.LIST_STYLE_TYPE), "ol type");
        Check.equal("both", s("br").keyword(Prop.CLEAR), "br clear");
        Check.near(50, s("h").px(Prop.WIDTH), 1e-9, "hr width");
        Check.equal("#112233", HtmlPresentationalHints.legacyColor("#123"), "three digit colors");
        Check.equal("#ff0000", HtmlPresentationalHints.legacyColor("ff0000"), "colors without a hash");
        Check.equal("#00ff00", HtmlPresentationalHints.legacyColor("00ff00"), "plain hex");
        Check.equal("red", HtmlPresentationalHints.legacyColor("RED"), "named colors");
        Check.isNull(HtmlPresentationalHints.legacyColor("transparent"), "transparent has no effect");
        Check.isNull(HtmlPresentationalHints.legacyColor(""), "empty");
        Check.equal("#ff0000", HtmlPresentationalHints.legacyColor("#ff0000"), "seven characters");
        Check.equal("50%", LegacyAttributes.dimension(" 50% "), "percent dimension");
        Check.equal("12px", LegacyAttributes.dimension("12.abc"), "a dimension ignores trailing garbage");
        Check.isNull(LegacyAttributes.dimension("abc"), "not a dimension");
    }

    /**
     * The defaults of the user agent sheet. 사용자 에이전트 시트의 기본값.
     */
    public void testUserAgentDefaults() {
        init("<h1 id=\"h1\">t</h1><p id=\"p\">x</p><ul id=\"ul\"><li id=\"li\">a<ul id=\"ul2\"><li id=\"li2\">b</li></ul></li></ul><b id=\"b\">b</b>"
                + "<a id=\"a\" href=\"/x\">a</a><a id=\"n\">n</a><input id=\"in\"><hr id=\"hr\"><pre id=\"pre\">p</pre><code id=\"code\">c</code><small id=\"sm\">s</small>");
        Check.near(32, s("h1").fontSizePx(), 1e-9, "h1 is 2em");
        Check.equal(700, s("h1").fontWeight(), "headings are bold");
        Check.near(21.44, s("h1").px(Prop.MARGIN_TOP), 0.01, "h1 margin is 0.67em of 32px");
        Check.equal("block", s("p").display(), "p is a block");
        Check.near(16, s("p").px(Prop.MARGIN_TOP), 1e-9, "p has a one em margin");
        Check.near(8, r.getComputedStyle(doc.getDocumentElement().getLastElementChild()).px(Prop.MARGIN_LEFT), 1e-9, "body has an 8px margin");
        Check.equal("list-item", s("li").display(), "li is a list item");
        Check.equal("disc", s("ul").keyword(Prop.LIST_STYLE_TYPE), "ul has discs");
        Check.equal("circle", s("ul2").keyword(Prop.LIST_STYLE_TYPE), "a nested list has circles");
        Check.near(0, s("ul2").px(Prop.MARGIN_TOP), 1e-9, "a nested list has no vertical margin");
        Check.near(40, s("ul").px(Prop.PADDING_LEFT), 1e-9, "list indent");
        Check.equal(700, s("b").fontWeight(), "b is bolder than the normal parent weight");
        Check.equal("rgb(0, 0, 238)", color("a"), "an unvisited link is blue");
        Check.equal("underline", s("a").keyword(Prop.TEXT_DECORATION_LINE), "links are underlined");
        Check.equal("rgb(0, 0, 0)", color("n"), "an anchor without href is not a link");
        Check.equal("inline-block", s("in").display(), "inputs are inline blocks");
        Check.equal("pre", s("pre").keyword(Prop.WHITE_SPACE), "pre keeps white space");
        Check.equal("monospace", s("code").fontFamilies().get(0), "code is monospace");
        Check.near(16 / 1.2, s("sm").fontSizePx(), 1e-9, "small is smaller");
        Check.equal("none", r.getComputedStyle(doc.getDocumentElement().getFirstElementChild()).display(), "head is hidden");
        Check.equal("block", r.getComputedStyle(doc.getDocumentElement()).display(), "html is a block");
        Check.equal("inset", s("hr").keyword(Prop.BORDER_TOP_STYLE), "hr has an inset border");
    }

    /**
     * Pseudo-element styles. 의사 요소 스타일.
     */
    public void testPseudoElements() {
        init("<p id=\"a\" style=\"color: red\">x</p><q id=\"q\">q</q>", "p::before { content: \"[\"; font-weight: bold } p:first-child::after { content: \"]\" }"
                + " :is(p)::selection { color: blue } p::placeholder { color: green }");
        Element a = doc.getElementById("a");
        ComputedStyle before = r.getPseudoStyle(a, "before");
        Check.notNull(before, "the before style exists");
        Check.equal("\"[\"", before.get(Prop.CONTENT).toCssText(), "content of the pseudo-element");
        Check.equal("rgb(255, 0, 0)", before.getPropertyValue("color"), "a pseudo-element inherits from its element");
        Check.equal(700, before.fontWeight(), "own declarations apply");
        Check.notNull(r.getPseudoStyle(a, "AFTER"), "names are case-insensitive and structural selectors work");
        Check.notNull(r.getPseudoStyle(a, "selection"), "selection");
        Check.equal("rgb(0, 0, 255)", r.getPseudoStyle(a, "selection").getPropertyValue("color"), "selection color");
        Check.isNull(r.getPseudoStyle(a, "first-line"), "no rule targets first-line");
        Check.isNull(r.getPseudoStyle(doc.getElementById("q"), "selection"), "p rules do not apply to q");
        Check.notNull(r.getPseudoStyle(doc.getElementById("q"), "before"), "the user agent sheet gives q a before style");
        Check.equal("open-quote", r.getPseudoStyle(doc.getElementById("q"), "before").get(Prop.CONTENT).toCssText(), "q::before content");
        Check.equal("normal", s("a").get(Prop.CONTENT).toCssText(), "the element itself is not affected by pseudo-element rules");
        Check.same(before, r.getPseudoStyle(a, "before"), "pseudo styles are cached");
    }

    /**
     * Media dependent rules and environment changes. 미디어에 의존하는 규칙과 환경 변경.
     */
    public void testMedia() {
        init("<p id=\"a\">x</p>", "@media (min-width: 600px) { p { color: red } } @media print { p { color: blue } } p { margin-top: 1px }");
        Check.equal("rgb(255, 0, 0)", color("a"), "the media rule applies in a wide viewport");
        MediaEnvironment narrow = new MediaEnvironment(400, 600);
        r.setMediaEnvironment(narrow);
        Check.equal("rgb(0, 0, 0)", color("a"), "and not in a narrow one");
        Check.near(1, s("a").px(Prop.MARGIN_TOP), 1e-9, "unconditional rules still apply");
        MediaEnvironment print = new MediaEnvironment(800, 600);
        print.setMediaType("print");
        r.setMediaEnvironment(print);
        Check.equal("rgb(0, 0, 255)", color("a"), "a print rule applies for print");
        tearDown();
        init("<p id=\"a\">x</p>", "p { width: 10vw; height: 50vh }");
        Check.near(80, s("a").px(Prop.WIDTH), 1e-9, "vw units follow the viewport");
        r.setMediaEnvironment(new MediaEnvironment(1000, 400));
        Check.near(100, s("a").px(Prop.WIDTH), 1e-9, "after resizing");
        Check.near(200, s("a").px(Prop.HEIGHT), 1e-9, "vh after resizing");
        tearDown();
        init("<p id=\"a\">x</p>", "p { color: red }");
        StyleSheet disabled = StyleSheet.parse("p { color: blue }", null, StyleSheet.ORIGIN_AUTHOR, null, null);
        StyleSheet printOnly = StyleSheet.parse("p { margin-top: 7px }", null, StyleSheet.ORIGIN_AUTHOR, null, null);
        printOnly.setMedia(org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList.parse("print"));
        disabled.setDisabled(true);
        r.setStyleSheets(Arrays.asList(StyleSheet.parse("p { color: red }", null, StyleSheet.ORIGIN_AUTHOR, null, null), disabled, printOnly));
        Check.equal("rgb(255, 0, 0)", color("a"), "a disabled sheet is ignored");
        Check.near(16, s("a").px(Prop.MARGIN_TOP), 1e-9, "a sheet whose media do not match is ignored");
    }

    /**
     * Dynamic state: only the styles that consulted it are recomputed. 동적 상태: 그것을 조회한 스타일만 다시 계산한다.
     */
    public void testDynamicState() {
        init("<div id=\"d\"><a id=\"a\" href=\"/x\">a</a><span id=\"s\">s</span></div>", "a:hover { color: red } a { color: blue } div:hover span { color: green }");
        SimpleElementState st = new SimpleElementState();
        r.setElementState(st);
        final List<Element> notified = new ArrayList<Element>();
        r.addChangeListener(new StyleChangeListener() {
            public void stylesInvalidated(Element root) {
                notified.add(root);
            }
        });
        ComputedStyle before = s("a");
        Check.equal("rgb(0, 0, 255)", color("a"), "not hovered");
        Check.equal("rgb(0, 0, 0)", color("s"), "the span is not green yet");
        notified.clear();
        st.setHover(doc.getElementById("a"));
        r.stateChanged(doc.getElementById("a"));
        Check.isFalse(notified.isEmpty(), "the listener was told");
        Check.equal("rgb(255, 0, 0)", color("a"), "hovered");
        Check.equal("rgb(0, 128, 0)", color("s"), "a descendant selector with a hovered ancestor");
        Check.isFalse(before.sameValues(s("a")), "a new style object with other values");
        int v = r.getStyleVersion();
        st.setHover(null);
        r.stateChanged(null);
        Check.isTrue(r.getStyleVersion() > v, "the version grows");
        Check.equal("rgb(0, 0, 255)", color("a"), "not hovered again");
    }

    /**
     * DOM mutations invalidate exactly what they can affect. DOM 변경은 영향을 줄 수 있는 것만 무효화한다.
     */
    public void testMutations() {
        init("<div id=\"d\" class=\"x\"><p id=\"p\">1</p><p id=\"q\">2</p></div>", ".x p { color: red } .y p { color: blue } .x { margin-top: 3px }");
        Check.equal("rgb(255, 0, 0)", color("p"), "class selector");
        Element d = doc.getElementById("d");
        ComputedStyle p0 = s("p");
        d.setAttribute("class", "y");
        Check.equal("rgb(0, 0, 255)", color("p"), "a class change reaches the descendants");
        Check.near(0, s("d").px(Prop.MARGIN_TOP), 1e-9, "and the element itself");
        Check.isFalse(p0 == s("p"), "the style object was replaced");
        doc.getElementById("p").setAttribute("style", "color: green");
        Check.equal("rgb(0, 128, 0)", color("p"), "a style attribute change");
        doc.getElementById("p").removeAttribute("style");
        Check.equal("rgb(0, 0, 255)", color("p"), "removing the style attribute");
        Element e = doc.createElement("p");
        e.setAttribute("id", "n");
        e.appendChild(doc.createTextNode("3"));
        d.appendChild(e);
        Check.equal("rgb(0, 0, 255)", color("n"), "a new element is styled on demand");
        d.removeChild(e);
        Check.notNull(r.getComputedStyle(e), "a detached element can still be styled");
        tearDown();
        init("<div id=\"d\"><p id=\"p1\">1</p><p id=\"p2\">2</p></div>", "p:first-child { color: red } p + p { margin-top: 20px } div:empty { height: 5px }");
        Check.equal("rgb(255, 0, 0)", color("p1"), "first-child");
        Check.equal("rgb(0, 0, 0)", color("p2"), "not first-child");
        Element d2 = doc.getElementById("d");
        Element first = doc.createElement("p");
        first.setAttribute("id", "p0");
        d2.insertBefore(first, d2.getFirstChild());
        Check.equal("rgb(0, 0, 0)", color("p1"), "inserting a first child changes :first-child");
        Check.equal("rgb(255, 0, 0)", color("p0"), "the new first child");
        Check.near(20, s("p1").px(Prop.MARGIN_TOP), 1e-9, "the sibling combinator sees the new sibling");
        d2.removeChild(first);
        Check.equal("rgb(255, 0, 0)", color("p1"), "removing the first child restores :first-child");
        Check.isTrue(Double.isNaN(s("d").px(Prop.HEIGHT)), "div:empty does not match a div with children");
        // 항목 의 항목을 하나씩 처리한다.
        while (d2.getFirstChild() != null) {
            d2.removeChild(d2.getFirstChild());
        }
        Check.near(5, s("d").px(Prop.HEIGHT), 1e-9, "div:empty matches once the children are gone");
        tearDown();
        init("<ul id=\"u\"><li id=\"a\">a</li></ul>", "ul:has(> li.on) { color: red }");
        Check.equal("rgb(0, 0, 0)", color("u"), ":has() without a match");
        doc.getElementById("a").setAttribute("class", "on");
        Check.equal("rgb(255, 0, 0)", color("u"), "a descendant change reaches the ancestor through :has()");
    }

    /**
     * Style sheet sources are reported. 스타일 시트 출처의 변경이 보고된다.
     */
    public void testSheetSourceChanges() {
        init("<style id=\"st\">p { color: red }</style><p id=\"p\">x</p>");
        Check.isFalse(r.consumeSheetSourceChange(), "nothing changed yet");
        Element st = doc.getElementById("st");
        s("p");
        st.setTextContent("p { color: blue }");
        Check.isTrue(r.consumeSheetSourceChange(), "the text of a style element changed");
        Check.isFalse(r.consumeSheetSourceChange(), "the flag is cleared when read");
        Element link = doc.createElement("link");
        doc.getDocumentElement().appendChild(link);
        Check.isTrue(r.consumeSheetSourceChange(), "a link element was added");
    }

    /**
     * Quirks mode adds rules. 호환 모드는 규칙을 더한다.
     */
    public void testQuirks() {
        init("<div id=\"w\" style=\"font-size: 20px\"><table id=\"t\"><tbody><tr><td id=\"c\">x</td></tr></tbody></table></div>");
        doc.setCompatibilityMode(Document.NO_QUIRKS_MODE);
        r.invalidate();
        Check.near(20, s("t").fontSizePx(), 1e-9, "a table inherits the font size in standards mode");
        doc.setCompatibilityMode(Document.QUIRKS_MODE);
        r.invalidate();
        Check.near(16, s("t").fontSizePx(), 1e-9, "a table resets the font size in quirks mode");
    }

    /**
     * Root element and rem. 루트 요소와 rem.
     */
    public void testRootAndRem() {
        init("<p id=\"p\" style=\"width: 2rem\">x</p>", "html { font-size: 10px }");
        Check.near(10, r.getComputedStyle(doc.getDocumentElement()).fontSizePx(), 1e-9, "root font size");
        Check.near(20, s("p").px(Prop.WIDTH), 1e-9, "rem follows the root font size");
        Check.near(10, s("p").fontSizePx(), 1e-9, "the root font size is inherited");
    }

    /**
     * Disposal and misuse. 폐기와 잘못된 사용.
     */
    public void testDisposal() {
        init("<p id=\"p\">x</p>", "p { color: red }");
        final Element p = doc.getElementById("p");
        Check.notNull(r.getComputedStyle(p), "usable before disposal");
        r.dispose();
        r.dispose();
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() {
                r.getComputedStyle(p);
            }
        }, "a disposed resolver cannot be used");
        tearDown();
        init("<p id=\"p\">x</p>");
        final Document other = MiniHtml.parse("<html><body><p id=\"o\">x</p></body></html>");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                r.getComputedStyle(other.getElementById("o"));
            }
        }, "an element of another document is rejected");
        final int[] calls = {0};
        r.addChangeListener(new StyleChangeListener() {
            public void stylesInvalidated(Element root) {
                calls[0]++;
                throw new IllegalStateException("a listener must not break the resolver");
            }
        });
        r.invalidate();
        Check.equal(1, calls[0], "a throwing listener is tolerated");
        CssDiagnostics d = new CssDiagnostics();
        r.setDiagnostics(d);
        Check.equal(0, d.count(CssDiagnostics.INVALID), "diagnostics are collected into the given object");
    }

    /**
     * Large and deep trees. 크고 깊은 트리.
     */
    public void testLargeAndDeepTrees() {
        StringBuilder wide = new StringBuilder();
        // 형제 3000개를 만든다.
        for (int i = 0; i < 3000; i++) {
            wide.append("<p class=\"c").append(i % 10).append("\">x</p>");
        }
        init(wide.toString(), ".c3 { color: red } .c4 { color: blue } p { margin-top: 1px }");
        Element body = doc.getDocumentElement().getLastElementChild();
        int red = 0;
        long t0 = System.currentTimeMillis();
        // 모든 형제의 스타일을 계산해 빨간 것을 센다.
        for (Element c = body.getFirstElementChild(); c != null; c = c.getNextElementSibling()) {
            if (r.getComputedStyle(c).color(Prop.COLOR) == 0xFFFF0000) {
                red++;
            }
        }
        Check.equal(300, red, "a tenth of the siblings are red");
        Check.isTrue(System.currentTimeMillis() - t0 < 60000, "3000 elements are styled in bounded time");
        tearDown();
        StringBuilder deep = new StringBuilder();
        // 5000겹의 div 를 만든다.
        for (int i = 0; i < 5000; i++) {
            deep.append("<div>");
        }
        deep.append("<span id=\"leaf\">x</span>");
        // 항목 의 항목을 하나씩 처리한다.
        for (int i = 0; i < 5000; i++) {
            deep.append("</div>");
        }
        init(deep.toString(), "div { color: red }");
        Check.equal("rgb(255, 0, 0)", color("leaf"), "the style of a very deep element is computed without recursion");
    }

    /**
     * Rules with a selector list use the highest specificity of the matching selector. 선택자 목록이 있는 규칙은 일치하는 선택자의 가장 높은 구체성을 쓴다.
     */
    public void testSelectorListSpecificity() {
        init("<p id=\"a\" class=\"c\">x</p>", "p, #zzz { color: red } .c { color: blue }");
        Check.equal("rgb(0, 0, 255)", color("a"), "only the matching selector counts: the type selector, not the unmatched id");
        tearDown();
        init("<p id=\"a\" class=\"c\">x</p>", "p, #a { color: red } .c { color: blue }");
        Check.equal("rgb(255, 0, 0)", color("a"), "the id selector of the list matches and is the most specific");
    }
}
