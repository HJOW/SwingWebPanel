package org.duckdns.hjow.swingwebpanel.css;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.selector.AnPlusB;
import org.duckdns.hjow.swingwebpanel.css.selector.ComplexSelector;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.css.selector.NamespaceMap;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorMatcher;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.selector.SimpleElementState;
import org.duckdns.hjow.swingwebpanel.css.selector.ElementState;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.NodeList;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of selector parsing, serialization, specificity and matching, and of the DOM selector engine (plan items P08-03 and P08-09).
 * <p>
 * 선택자 파싱, 직렬화, 구체성, 매칭과 DOM 선택자 엔진 시험(계획 P08-03, P08-09).
 */
public class SelectorTest {

    /** Markup of the shared test document (no whitespace between tags). / 공유 시험 문서의 마크업(태그 사이에 공백 없음). */
    private static final String PAGE = "<html lang=\"en\"><head><title>T</title></head><body>"
            + "<div id=\"main\" class=\"box wide\" data-x=\"Hello World\">"
            + "<p class=\"first\">One</p>"
            + "<p id=\"p2\">Two <span class=\"hl\">hl</span></p>"
            + "<p class=\"last odd\">Three</p>"
            + "<ul><li>1</li><li class=\"sel\">2</li><li>3</li><li>4</li><li>5</li></ul>"
            + "<a href=\"/x\" rel=\"Nofollow\" hreflang=\"en-US\">link</a>"
            + "<span id=\"nolink\">s</span>"
            + "<input id=\"cb\" type=\"checkbox\" checked>"
            + "<input id=\"tx\" type=\"text\" disabled placeholder=\"ph\">"
            + "<form id=\"f\"><button id=\"b1\">go</button><input id=\"rq\" required>"
            + "<fieldset disabled><legend><input id=\"inleg\"></legend><input id=\"infs\"></fieldset></form>"
            + "<div id=\"e1\"></div><div id=\"e2\"> </div><div id=\"e3\"><!-- c --></div>"
            + "</div></body></html>";

    /** The shared document. / 공유 문서. */
    private Document doc;

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public SelectorTest() {
    }

    /**
     * Builds the shared document and installs the engine. 공유 문서를 만들고 엔진을 설치한다.
     */
    public void setUp() {
        doc = MiniHtml.parse(PAGE);
        CssSelectorEngine.install(doc);
    }

    /**
     * Parses a selector and returns its serialization, or the word INVALID. 선택자를 파싱해 직렬화 결과를 반환한다. 올바르지 않으면 INVALID.
     *
     * @param css selector text / 선택자 문자열
     * @return serialized form or INVALID / 직렬화 결과 또는 INVALID
     */
    private static String ser(String css) {
        try {
            return SelectorParser.parse(css, null).toCssText();
        } catch (SelectorSyntaxException e) {
            return "INVALID";
        }
    }

    /**
     * Returns the text contents of the matches of a selector, joined with a bar. 선택자와 일치하는 요소의 텍스트를 막대로 이어 반환한다.
     *
     * @param css selector text / 선택자 문자열
     * @return joined texts / 이어 붙인 텍스트
     */
    private String texts(String css) {
        StringBuilder sb = new StringBuilder();
        List<Node> list = doc.querySelectorAll(css).toList();
        // 결과 요소마다 텍스트를 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(list.get(i).getTextContent());
        }
        return sb.toString();
    }

    /**
     * Returns the ids (or tag names) of the matches of a selector, joined with spaces. 선택자와 일치하는 요소의 id(없으면 태그 이름)를 공백으로 이어 반환한다.
     *
     * @param css selector text / 선택자 문자열
     * @return joined ids / 이어 붙인 id
     */
    private String ids(String css) {
        StringBuilder sb = new StringBuilder();
        List<Node> list = doc.querySelectorAll(css).toList();
        // 결과 요소마다 id 나 태그 이름을 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            Element e = (Element) list.get(i);
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(e.getId().length() > 0 ? e.getId() : e.getLocalName());
        }
        return sb.toString();
    }

    /**
     * Serialization of every selector kind. 모든 선택자 종류의 직렬화.
     */
    public void testSerialization() {
        Check.equal("a", ser("a"), "type");
        Check.equal("*", ser("*"), "universal");
        Check.equal("#id.cls[attr]", ser("#id.cls[attr]"), "compound");
        Check.equal("a > b + c ~ d e", ser("a>b+c~d   e"), "combinators");
        Check.equal(":not(.a, .b)", ser(":not(.a,.b)"), "not");
        Check.equal("[a=\"b\" i]", ser("[a=b I]"), "case flag");
        Check.equal("[a~=\"b\"][c|=\"d\"][e^=\"f\"][g$=\"h\"][i*=\"j\"]", ser("[a~=b][c|=d][e^=f][g$=h][i*=j]"), "operators");
        Check.equal("::before", ser(":before"), "legacy pseudo-element is written with two colons");
        Check.equal("a:hover::after", ser("a:hover::after"), "pseudo-class and pseudo-element");
        Check.equal(":nth-child(3n+2 of .x, p)", ser(":nth-child(3n+2 of .x,p)"), "nth with of");
        Check.equal(":nth-child(odd)", ser(":nth-child(2n+1)"), "odd is canonical");
        Check.equal(":nth-child(even)", ser(":nth-child(2n)"), "even is canonical");
        Check.equal(":lang(en, \"*-CH\")", ser(":lang(en, \"*-CH\")"), "lang");
        Check.equal(":has(> a, + b)", ser(":has(> a, + b)"), "relative selectors keep their leading combinator");
        Check.equal(".a\\.b", ser(".a\\.b"), "escaped class names are re-escaped");
        Check.equal("[id=\"123\"]", ser("[id=\"123\"]"), "string values");
        Check.equal("[a]", ser("[a"), "an unclosed bracket is closed at the end of input");
        Check.equal(":is(a)", ser(":is(a"), "an unclosed function is closed at the end of input");
    }

    /**
     * Invalid selectors are rejected. 올바르지 않은 선택자는 거부된다.
     */
    public void testInvalidSelectors() {
        String[] bad = {"", " ", "a >", "> a", "a,", ",a", ".", "#1a", "[=a]", "[a=]", "[a=1]", "[a b]", "a:unknown", "::unknown",
            ":not()", ":nth-child(foo)", ":nth-child()", "a::before b", "ns|a", ":not(::before)", "a b:", "a$b", "a ~ ~ b", ":lang()",
            ":dir(up)", "a::before:hover", "*|", "a || b", ":nth-of-type(2 of a)"};
        // 모든 잘못된 선택자가 INVALID 인지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            Check.equal("INVALID", ser(bad[i]), "'" + bad[i] + "' must be rejected");
        }
    }

    /**
     * Forgiving lists inside :is() and :where(). :is() 와 :where() 의 관대한 목록.
     */
    public void testForgivingLists() {
        Check.equal(":is(a, b)", ser(":is(a, !!, b)"), "invalid entries are dropped");
        Check.equal(":is()", ser(":is()"), "an empty forgiving list is valid");
        Check.equal(":where()", ser(":where(:bogus)"), "all entries invalid");
        Check.equal("INVALID", ser(":not(a, !!)"), ":not() is not forgiving");
        Check.equal(":matches(a, b)", ser(":matches(a, b)"), "the legacy name is kept as written");
        Check.equal("", texts(":is()"), ":is() matches nothing");
    }

    /**
     * Specificity values. 구체성 값.
     */
    public void testSpecificity() {
        Check.equal(spec(0, 0, 0), specOf("*"), "universal");
        Check.equal(spec(0, 0, 1), specOf("a"), "type");
        Check.equal(spec(1, 1, 1), specOf("a.b#c"), "compound");
        Check.equal(spec(1, 0, 0), specOf(":not(#x)"), "not takes the argument");
        Check.equal(spec(0, 0, 0), specOf(":where(#x)"), "where is zero");
        Check.equal(spec(0, 1, 0), specOf(":is(a, .b)"), "is takes the maximum");
        Check.equal(spec(0, 2, 0), specOf(":nth-child(2 of .a)"), "nth with of");
        Check.equal(spec(0, 0, 2), specOf("a::before"), "pseudo-element counts as a type");
        Check.equal(spec(0, 1, 0), specOf("[x]"), "attribute");
        Check.equal(spec(0, 1, 1), specOf("a:hover"), "pseudo-class");
        Check.equal(spec(0, 2, 2), specOf("ul li.a:first-child"), "mixed");
        Check.equal(spec(0, 1, 1), specOf(":has(> img.x)"), "has takes the argument");
    }

    /**
     * Packs specificity like the engine does. 엔진과 같은 방식으로 구체성을 묶는다.
     *
     * @param a ids / id 개수
     * @param b classes / 클래스 개수
     * @param c types / 타입 개수
     * @return packed value / 묶은 값
     */
    private static Long spec(long a, long b, long c) {
        return Long.valueOf((a << 40) + (b << 20) + c);
    }

    /**
     * Returns the specificity of the first selector of a text. 텍스트의 첫 선택자의 구체성을 반환한다.
     *
     * @param css selector text / 선택자 문자열
     * @return packed specificity / 묶은 구체성
     */
    private static Long specOf(String css) {
        try {
            return Long.valueOf(SelectorParser.parse(css, null).getSelectors().get(0).getSpecificity());
        } catch (SelectorSyntaxException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * The An+B micro-syntax. An+B 미세 문법.
     */
    public void testAnPlusB() {
        String[] valid = {"odd", "even", "5", "-5", "+5", "n", "-n", "+n", "2n", "2n+1", "2n + 1", "2n +1", "2n+ 1", "2n-1", "2n - 1", "2n -1",
            "-n+3", "-n + 3", "n- 2", "n -2", "N+2", "ODD", "-2n-3", "0n+4", "+2n+3"};
        // 올바른 식이 모두 파싱되는지 확인한다.
        for (int i = 0; i < valid.length; i++) {
            Check.notNull(AnPlusB.parse(CssParser.parseComponentValues(valid[i])), "'" + valid[i] + "' is valid");
        }
        String[] invalid = {"", "2 n", "n +", "+ n", "2.5n", "n+1.5", "n--1", "foo", "2n+", "- n", "n+ -1", "2n 1", "1 2", "n n"};
        // 올바르지 않은 식이 모두 거부되는지 확인한다.
        for (int i = 0; i < invalid.length; i++) {
            Check.isNull(AnPlusB.parse(CssParser.parseComponentValues(invalid[i])), "'" + invalid[i] + "' is invalid");
        }
        AnPlusB x = AnPlusB.parse(CssParser.parseComponentValues("-n+3"));
        Check.equal(-1, x.getA(), "a of -n+3");
        Check.equal(3, x.getB(), "b of -n+3");
        Check.isTrue(x.matches(1) && x.matches(3) && !x.matches(4), "positions of -n+3");
        AnPlusB y = AnPlusB.parse(CssParser.parseComponentValues("2n-1"));
        Check.equal(2, y.getA(), "a of 2n-1");
        Check.equal(-1, y.getB(), "b of 2n-1");
        Check.isTrue(y.matches(1) && y.matches(5) && !y.matches(2), "positions of 2n-1");
        AnPlusB z = AnPlusB.parse(CssParser.parseComponentValues("0n+4"));
        Check.isTrue(z.matches(4) && !z.matches(3) && !z.matches(5), "constant");
        Check.equal("-n+3", x.toCssText(), "canonical text");
    }

    /**
     * Combinators and simple selectors on the shared document. 공유 문서에서의 결합자와 단순 선택자.
     */
    public void testBasicMatching() {
        Check.equal("One|Two hl|Three", texts("p"), "type");
        Check.equal("One", texts("p.first"), "class");
        Check.equal("One|Two hl|Three", texts(".box.wide > p"), "child");
        Check.equal("hl", texts("div p span"), "descendant");
        Check.equal("Two hl|Three", texts("p + p"), "next sibling");
        Check.equal("One|Two hl|Three", texts("p ~ p, p.first"), "later sibling and list");
        Check.equal("main", ids("#main"), "id");
        Check.equal("main", ids("DIV#main"), "type names are case-insensitive in HTML");
        Check.equal("One", texts("P.first"), "upper case type with class");
        Check.equal("Three", texts(".last.odd"), "two classes");
        Check.equal("", texts(".last.even"), "missing class");
        Check.equal("html", ids("html"), "root type");
        Check.equal("p2", ids("*#p2"), "universal with id");
        Check.equal("1|2|3|4|5", texts("ul > li"), "children list");
        Check.equal("", texts("body > li"), "child combinator needs the direct parent");
    }

    /**
     * Attribute selectors with every operator and the HTML case rules. 모든 연산자와 HTML 대소문자 규칙을 가진 속성 선택자.
     */
    public void testAttributeSelectors() {
        Check.equal("main", ids("[data-x]"), "presence");
        Check.equal("main", ids("[data-x=\"Hello World\"]"), "equals");
        Check.equal("", ids("[data-x=\"hello world\"]"), "values are case-sensitive for unlisted attributes");
        Check.equal("main", ids("[data-x=\"hello world\" i]"), "i flag");
        Check.equal("", ids("[rel=\"nofollow\" s]"), "s flag");
        Check.equal("a", ids("[rel=nofollow]"), "rel value is case-insensitive in HTML");
        Check.equal("cb", ids("[type=CHECKBOX]"), "type value is case-insensitive in HTML");
        Check.equal("main", ids("[data-x^=Hello]"), "prefix");
        Check.equal("main", ids("[data-x$=World]"), "suffix");
        Check.equal("main", ids("[data-x*=\"o W\"]"), "substring");
        Check.equal("main", ids("[data-x~=World]"), "includes");
        Check.equal("", ids("[data-x~=Wor]"), "includes needs a whole word");
        Check.equal("", ids("[data-x|=Hello]"), "dash match needs a hyphen");
        Check.equal("a", ids("[hreflang|=en]"), "dash match");
        Check.equal("main", ids("[class~=wide]"), "class includes");
        Check.equal("", ids("[data-x^=\"\"]"), "empty prefix matches nothing");
        Check.equal("", ids("[data-x=\"\"]"), "empty value");
        Check.equal("main", ids("[DATA-X]"), "attribute names are case-insensitive in HTML");
        Check.equal("", ids("[data-x~=\"a b\"]"), "includes with whitespace matches nothing");
    }

    /**
     * Structural pseudo-classes. 구조 의사 클래스.
     */
    public void testStructural() {
        Check.equal("1", texts("li:first-child"), "first-child");
        Check.equal("5", texts("li:last-child"), "last-child");
        Check.equal("", texts("li:only-child"), "only-child");
        Check.equal("2|4", texts("li:nth-child(2n)"), "2n");
        Check.equal("1|3|5", texts("li:nth-child(odd)"), "odd");
        Check.equal("1|2", texts("li:nth-child(-n+2)"), "-n+2");
        Check.equal("4|5", texts("li:nth-child(n+4)"), "n+4");
        Check.equal("4", texts("li:nth-last-child(2)"), "nth-last-child");
        Check.equal("1", texts("li:nth-child(1 of :not(.sel))"), "of S, first");
        Check.equal("3", texts("li:nth-child(2 of :not(.sel))"), "of S, second");
        Check.equal("", texts("li:nth-child(2 of :not(.sel)).sel"), "an element outside S is never selected");
        Check.equal("One", texts("p:first-of-type"), "first-of-type");
        Check.equal("Three", texts("p:last-of-type"), "last-of-type");
        Check.equal("Two hl", texts("p:nth-of-type(2)"), "nth-of-type");
        Check.equal("Two hl", texts("p:nth-last-of-type(2)"), "nth-last-of-type");
        Check.equal("hl|s", texts("span:only-of-type"), "only-of-type");
        Check.equal("e1 e3", ids("div:empty"), "empty ignores comments but not white space");
        Check.equal("html", ids(":root"), "root");
        Check.equal("", ids("body:root"), "body is not the root");
        Check.equal("html", ids(":scope"), "scope of a document is the root element");
    }

    /**
     * Logical pseudo-classes. 논리 의사 클래스.
     */
    public void testLogical() {
        Check.equal("One|Two hl|12345", texts("#main > :is(p, ul):not(.last)"), "is and not");
        Check.equal("One|Two hl|Three", texts("#main > :where(p)"), "where");
        Check.equal("main", ids("div:has(> p.first)"), "has with child");
        Check.equal("main", ids("div:has(span.hl)"), "has with descendant");
        Check.equal("p2", ids("p:has(+ p.last)"), "has with next sibling");
        Check.equal("p p2 p", ids("p:has(~ ul)"), "has with later sibling");
        Check.equal("ul", ids("ul:has(li.sel + li)"), "has with a complex relative selector");
        Check.equal("1", texts("li:has(+ .sel)"), "has next sibling");
        Check.equal("", ids("p:has(> div)"), "has with no match");
        Check.equal("p2", ids("p:not(.first):not(.last)"), "chained not");
        Check.equal("", ids("p:not(p)"), "not of itself");
    }

    /**
     * <code>:in-range</code> and <code>:out-of-range</code> follow the live value of the controls that have a range (P11-05); controls without
     * a range match neither. <code>:in-range</code> 와 <code>:out-of-range</code> 는 범위가 있는 컨트롤의 실제 값을 따르고, 범위가 없는 컨트롤은 둘 다
     * 일치하지 않는다.
     */
    public void testRangePseudoClasses() {
        doc = MiniHtml.parse("<html><body><input id=\"in\" type=\"number\" min=\"1\" max=\"5\" value=\"3\">"
                + "<input id=\"out\" type=\"number\" min=\"1\" max=\"5\" value=\"9\"><input id=\"low\" type=\"number\" min=\"1\" value=\"0\">"
                + "<input id=\"none\" type=\"text\" value=\"x\"><input id=\"free\" type=\"number\" value=\"7\"></body></html>");
        CssSelectorEngine.install(doc);
        Check.equal("in", ids(":in-range"), "only the control with a range whose value is inside it");
        Check.equal("out low", ids(":out-of-range"), "above the maximum and below the minimum");
    }

    /**
     * Form and link pseudo-classes derived from attributes. 속성에서 얻는 폼과 링크 의사 클래스.
     */
    public void testFormAndLinkStates() {
        Check.equal("cb", ids(":checked"), "checked");
        Check.equal("tx fieldset infs", ids(":disabled"), "disabled includes the descendants of a disabled fieldset except in its legend");
        Check.equal("cb b1 rq inleg", ids(":enabled"), "enabled");
        Check.equal("rq", ids(":required"), "required");
        Check.equal("cb tx inleg infs", ids(":optional"), "optional");
        Check.equal("tx", ids(":placeholder-shown"), "placeholder-shown");
        Check.equal("a", ids(":link"), "link");
        Check.equal("a", ids(":any-link"), "any-link");
        Check.equal("", ids(":visited"), "visited never matches");
        Check.equal("rq inleg", ids(":read-write"), "read-write");
        Check.equal("cb b1", ids(":default"), "default");
        Check.equal("f rq", ids(":invalid"), "invalid");
        Check.equal("cb b1 fieldset inleg", ids(":valid"), "valid: candidates that satisfy their constraints (a disabled control is barred from constraint validation, a submit button is valid)");
        Check.equal("", ids(":hover"), "no state provider means no hover");
    }

    /**
     * Language and direction pseudo-classes. 언어와 방향 의사 클래스.
     */
    public void testLangAndDir() {
        Document d = MiniHtml.parse("<html lang=\"en-US\" dir=\"rtl\"><body><p id=\"a\" lang=\"fr-CA\">x</p><p id=\"b\" lang=\"\">y</p>"
                + "<p id=\"c\" dir=\"ltr\">z</p><p id=\"d\">w</p></body></html>");
        CssSelectorEngine.install(d);
        Check.isTrue(d.getElementById("a").matches(":lang(fr)"), "prefix range");
        Check.isTrue(d.getElementById("a").matches(":lang(fr-CA)"), "exact range");
        Check.isTrue(d.getElementById("a").matches(":lang(\"*-CA\")"), "wildcard range");
        Check.isFalse(d.getElementById("a").matches(":lang(en)"), "other language");
        Check.isTrue(d.getElementById("b").matches(":lang(\"\")"), "empty language");
        Check.isFalse(d.getElementById("b").matches(":lang(en)"), "empty language matches no range");
        Check.isTrue(d.getElementById("d").matches(":lang(en)"), "inherited from the root");
        Check.isTrue(d.getElementById("d").matches(":lang(de, en)"), "any of several ranges");
        Check.isTrue(d.getElementById("d").matches(":dir(rtl)"), "direction is inherited");
        Check.isTrue(d.getElementById("c").matches(":dir(ltr)"), "dir attribute");
        Check.isFalse(d.getElementById("c").matches(":dir(rtl)"), "dir attribute excludes the other direction");
    }

    /**
     * Quirks mode makes ids and classes case-insensitive. 호환 모드에서는 id 와 클래스가 대소문자를 구분하지 않는다.
     */
    public void testQuirksMode() {
        Document d = MiniHtml.parse("<html><body><p id=\"Ab\" class=\"Cd\">x</p></body></html>");
        CssSelectorEngine.install(d);
        d.setCompatibilityMode(Document.NO_QUIRKS_MODE);
        Check.isFalse(d.getElementById("Ab").matches("#ab"), "id is case-sensitive in standards mode");
        Check.isFalse(d.getElementById("Ab").matches(".cd"), "class is case-sensitive in standards mode");
        d.setCompatibilityMode(Document.QUIRKS_MODE);
        Check.isTrue(d.getElementById("Ab").matches("#ab"), "id is case-insensitive in quirks mode");
        Check.isTrue(d.getElementById("Ab").matches(".cd"), "class is case-insensitive in quirks mode");
    }

    /**
     * Namespaces: prefixes, the default namespace and the three special forms. 네임스페이스: 접두사, 기본 네임스페이스, 세 가지 특수 형태.
     */
    public void testNamespaces() throws Exception {
        Document d = Document.createHtmlDocument();
        Element html = d.createElement("html");
        d.appendChild(html);
        Element svg = d.createElementNS(Namespaces.SVG, "rect");
        Element plain = d.createElement("rect");
        html.appendChild(svg);
        html.appendChild(plain);
        NamespaceMap map = new NamespaceMap();
        map.declare("s", Namespaces.SVG);
        MatchContext ctx = new MatchContext();
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("s|rect", map), svg, ctx), "prefix matches the namespace");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("s|rect", map), plain, ctx), "prefix excludes other namespaces");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("*|rect", map), plain, ctx), "any namespace");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("*|rect", map), svg, ctx), "any namespace, svg");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("|rect", map), svg, ctx), "no namespace excludes svg");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("|rect", map), plain, ctx), "an HTML element is in the HTML namespace, not in none");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("rect", map), plain, ctx), "no default namespace matches any");
        map.declare(null, Namespaces.SVG);
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("rect", map), svg, ctx), "the default namespace applies to type selectors");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("rect", map), plain, ctx), "the default namespace excludes others");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("*", map), svg, ctx), "universal with default namespace");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("*", map), plain, ctx), "universal excludes other namespaces");
        Check.equal("s|rect", SelectorParser.parse("s|rect", map).toCssText(), "prefix is serialized");
        svg.setAttributeNS(Namespaces.XLINK, "xlink:href", "#a");
        map.declare("x", Namespaces.XLINK);
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("[x|href]", map), svg, ctx), "attribute with a namespace");
        Check.isFalse(SelectorMatcher.matches(SelectorParser.parse("[href]", map), svg, ctx), "attribute without a namespace does not see it");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("[*|href]", map), svg, ctx), "attribute in any namespace");
    }

    /**
     * Element state plug-in: hover, focus, focus-within and fallbacks. 요소 상태 플러그인: hover, focus, focus-within, 대체 동작.
     */
    public void testElementState() {
        CssSelectorEngine engine = CssSelectorEngine.install(doc);
        SimpleElementState st = new SimpleElementState();
        engine.setElementState(st);
        Element p2 = doc.getElementById("p2");
        Element span = (Element) p2.getFirstElementChild();
        Check.isFalse(p2.matches(":hover"), "nothing is hovered yet");
        st.setHover(span);
        Check.isTrue(span.matches(":hover"), "hovered element");
        Check.isTrue(p2.matches(":hover"), "ancestors of the hovered element are hovered too");
        Check.isTrue(doc.getElementById("main").matches(":hover"), "all ancestors are hovered");
        Check.isFalse(doc.getElementById("main").matches("p:hover"), "the type selector still applies");
        st.setFocus(span);
        Check.isTrue(span.matches(":focus"), "focus");
        Check.isTrue(p2.matches(":focus-within"), "focus-within");
        Check.isFalse(p2.matches(":focus"), "an ancestor is not focused");
        st.setFocusVisible(false);
        Check.isFalse(span.matches(":focus-visible"), "focus-visible can be off");
        st.setTarget(p2);
        Check.isTrue(p2.matches(":target"), "target");
        // 상태 제공자가 UNKNOWN 이면 속성 기반 기본값을 쓴다.
        Check.isTrue(doc.getElementById("cb").matches(":checked"), "unknown state falls back to the attribute");
        engine.setElementState(new ElementState() {
            public int get(Element element, int id) {
                return ElementState.FALSE;
            }
        });
        Check.isFalse(doc.getElementById("cb").matches(":checked"), "a provider can override the attribute");
        engine.setElementState(null);
    }

    /**
     * Pseudo-elements match only when the context asks for them. 의사 요소는 컨텍스트가 요청할 때만 일치한다.
     */
    public void testPseudoElementMatching() throws Exception {
        SelectorList l = SelectorParser.parse("p::before, .first:is(p)::after, p", null);
        Element first = (Element) doc.querySelector("p.first");
        MatchContext ctx = new MatchContext();
        int plain = 0;
        // 의사 요소가 없는 컨텍스트에서는 마지막 선택자만 일치한다.
        for (int i = 0; i < l.size(); i++) {
            if (SelectorMatcher.matches(l.getSelectors().get(i), first, ctx)) {
                plain++;
            }
        }
        Check.equal(1, plain, "only the selector without a pseudo-element matches the element");
        ctx.setPseudoElement("before");
        Check.isTrue(SelectorMatcher.matches(l, first, ctx), "p::before matches in the before context");
        Check.isFalse(SelectorMatcher.matches(l.getSelectors().get(2), first, ctx), "p does not match in the before context");
        ctx.setPseudoElement("after");
        Check.isTrue(SelectorMatcher.matches(l.getSelectors().get(1), first, ctx), "nested :is() ignores the outer pseudo-element");
        Check.isFalse(SelectorMatcher.matches(l.getSelectors().get(0), first, ctx), "before does not match in the after context");
        Check.equal("before", l.getSelectors().get(0).getPseudoElement(), "pseudo-element name");
        Check.isNull(l.getSelectors().get(2).getPseudoElement(), "no pseudo-element");
        ComplexSelector c = l.getSelectors().get(0);
        Check.equal(1, c.getCompounds().size(), "one compound");
    }

    /**
     * The DOM engine: results, order, static lists, scope, closest and errors. DOM 엔진: 결과, 순서, 고정 목록, 범위, closest, 오류.
     */
    public void testEngine() {
        Element main = doc.getElementById("main");
        NodeList all = doc.querySelectorAll("li");
        Check.equal(5, all.getLength(), "querySelectorAll count");
        Check.equal("1", all.item(0).getTextContent(), "tree order, first");
        Check.equal("5", all.item(4).getTextContent(), "tree order, last");
        Element ul = (Element) doc.querySelector("ul");
        ul.appendChild(doc.createElement("li"));
        Check.equal(5, all.getLength(), "the returned list is static");
        Check.equal(6, doc.querySelectorAll("li").getLength(), "a new query sees the new element");
        Check.equal("One", main.querySelector(":scope > p").getTextContent(), ":scope is the element for element queries");
        Check.isNull(main.querySelector(":scope"), "the element itself is not its own descendant");
        Check.equal(3, main.querySelectorAll(":scope > p").getLength(), "scoped query");
        Check.isTrue(main.matches(":scope"), "matches() has the element as scope");
        Check.isTrue(main.matches("div#main, span"), "matches with a list");
        Check.isFalse(main.matches("span"), "matches false");
        Element li = (Element) doc.querySelector("li.sel");
        Check.same(ul, li.closest("ul"), "closest finds an ancestor");
        Check.same(li, li.closest("li"), "closest includes the element itself");
        Check.isNull(li.closest("table"), "closest without a match");
        Check.same(main, doc.querySelector("body > div"), "first match in tree order");
        Check.isNull(doc.querySelector("table"), "no match");
        Check.equal(0, doc.querySelectorAll("table").getLength(), "empty list");
        Check.equal("hl", ((Element) main.querySelector("p span")).getTextContent(), "descendant selectors may match above the scope");
        Check.isTrue(main.querySelectorAll("div div").getLength() == 3, "ancestors outside the scope take part in matching");
    }

    /**
     * Invalid selectors raise SyntaxError. 올바르지 않은 선택자는 SyntaxError 를 낸다.
     */
    public void testEngineErrors() {
        final Element main = doc.getElementById("main");
        String[] bad = {"", "a >", "!!", ":unknown", "ns|a"};
        // 모든 잘못된 선택자가 모든 진입점에서 SyntaxError 를 내는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            final String sel = bad[i];
            Check.throwsException(DomException.class, new Block() {
                public void run() {
                    doc.querySelector(sel);
                }
            }, "querySelector '" + sel + "'");
            Check.throwsException(DomException.class, new Block() {
                public void run() {
                    main.querySelectorAll(sel);
                }
            }, "querySelectorAll '" + sel + "'");
            Check.throwsException(DomException.class, new Block() {
                public void run() {
                    main.matches(sel);
                }
            }, "matches '" + sel + "'");
            Check.throwsException(DomException.class, new Block() {
                public void run() {
                    main.closest(sel);
                }
            }, "closest '" + sel + "'");
        }
        try {
            doc.querySelector("a >");
            Check.fail("no exception");
        } catch (DomException e) {
            Check.equal("SyntaxError", e.getName(), "exception name");
            Check.equal(12, e.getCode(), "exception code");
        }
    }

    /**
     * A selector with pathological backtracking gives up instead of hanging. 비정상적으로 역추적하는 선택자는 멈추지 않고 포기한다.
     */
    public void testBacktrackingBudget() throws Exception {
        StringBuilder sb = new StringBuilder();
        // 60겹으로 중첩된 div 안에 span 하나를 둔다.
        for (int i = 0; i < 60; i++) {
            sb.append("<div>");
        }
        sb.append("<span>x</span>");
        // 열었던 div 를 같은 수만큼 닫는다.
        for (int i = 0; i < 60; i++) {
            sb.append("</div>");
        }
        Document d = MiniHtml.parse("<html><body>" + sb + "</body></html>");
        SelectorList l = SelectorParser.parse("b div div div div div div div div div div div div div span", null);
        Element span = (Element) d.getElementsByTagName("span").item(0);
        MatchContext ctx = new MatchContext();
        long t0 = System.currentTimeMillis();
        Check.isFalse(SelectorMatcher.matches(l, span, ctx), "no b exists, so nothing matches");
        Check.isTrue(ctx.isExhausted(), "the work budget ran out");
        Check.isTrue(System.currentTimeMillis() - t0 < 20000, "and it returned in bounded time");
        ctx.resetBudget();
        Check.isFalse(ctx.isExhausted(), "resetBudget restores the budget");
        Check.isTrue(SelectorMatcher.matches(SelectorParser.parse("div div div span", null), span, ctx), "ordinary selectors are unaffected");
    }
}
