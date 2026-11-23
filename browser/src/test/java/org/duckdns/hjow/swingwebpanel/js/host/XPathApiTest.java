package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests DOM XPath in a page (WHATWG DOM section 8, last modified 15 November 2026, and XML Path Language 1.0, W3C Recommendation 16 November 1999, both fetched
 * 2026-11-18; see <code>docs/progress/webapi2.md</code>): the interfaces, the location paths with all the axes and node tests, the predicates and positions, the
 * operators and their precedence, the comparison rules of node sets, the function library (with the examples of the Recommendation), the result types and
 * conversions, the invalidation of node iterators, resolvers, expression objects, and the errors.
 * <p>
 * 페이지의 DOM XPath 를 시험한다(WHATWG DOM 8절 2026-11-15 수정본과 XML Path Language 1.0 W3C 권고안 1999-11-16, 모두 2026-11-18 조회, <code>docs/progress/webapi2.md</code>
 * 참조): 인터페이스, 모든 축과 노드 테스트가 있는 위치 경로, 술어와 위치, 연산자와 우선순위, 노드 집합의 비교 규칙, 함수 라이브러리(권고안의 예 포함), 결과 종류와 변환, 노드 반복자의
 * 무효화, 리졸버, 식 객체, 오류.
 */
public class XPathApiTest {

    /**
     * The page: two lists, a paragraph with text and an element, a comment, a link, and numbers; the helper functions of the tests (<code>q</code> lists the ids of a
     * node set, <code>s</code>, <code>n</code> and <code>b</code> give a string, a number and a boolean, <code>err</code> gives the name of an error).
     * <p>
     * 페이지: 목록 둘, 텍스트와 요소가 있는 문단, 주석, 링크, 숫자. 시험의 도우미 함수(<code>q</code> 는 노드 집합의 id 를 나열하고, <code>s</code>, <code>n</code>,
     * <code>b</code> 는 문자열, 숫자, 불리언을 주며, <code>err</code> 는 오류의 이름을 준다).
     */
    private static final String PAGE = "<div id=root class=a><ul id=list><li id=l1 class='x'>one</li><li id=l2>two</li><li id=l3 class='x y'>three</li></ul>"
            + "<ul id=list2><li id=m1>a</li><li id=m2>b</li></ul><p id=p lang='en-US'>Hello <b id=bold>big</b> world</p><!-- note --><a href='u' id=lnk>link</a>"
            + "<span class=n id=n1>1</span><span class=n id=n2>2.5</span></div>"
            + "<script>function q(e, c) { var r = document.evaluate(e, c || document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null), a = []; "
            + "for (var i = 0; i < r.snapshotLength; i++) { var x = r.snapshotItem(i); a.push(x.id || x.nodeName || x.name); } return a.join(','); } "
            + "function s(e, c) { return document.evaluate(e, c || document, null, XPathResult.STRING_TYPE, null).stringValue; } "
            + "function n(e, c) { return String(document.evaluate(e, c || document, null, XPathResult.NUMBER_TYPE, null).numberValue); } "
            + "function b(e, c) { return String(document.evaluate(e, c || document, null, XPathResult.BOOLEAN_TYPE, null).booleanValue); } "
            + "function err(f) { try { f(); return 'none'; } catch (e) { return e.name; } }</" + "script>";

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public XPathApiTest() {
    }

    /**
     * Loads the page.
     * <p>
     * 페이지를 읽는다.
     *
     * @return the fixture / 시험 틀
     */
    private static PageFixture load() {
        PageFixture f = new PageFixture().load(PAGE);
        f.env.scheduler.advance(10);
        return f;
    }

    /**
     * Evaluates a script of the page and compares the value as text.
     * <p>
     * 페이지의 스크립트를 평가하고 값을 문자열로 비교한다.
     *
     * @param f        the page / 페이지
     * @param expected the expected value / 기대하는 값
     * @param code     the script / 스크립트
     * @param message  the message / 메시지
     */
    private static void eq(PageFixture f, String expected, String code, String message) {
        Check.equal(expected, f.scripts.evaluate(code, "t"), message);
    }

    /**
     * The interfaces, the constants and the string tags.
     * <p>
     * 인터페이스, 상수, 문자열 태그.
     */
    public void testInterfaces() {
        PageFixture f = load();
        eq(f, "function|function|function|function|function|function", "[typeof XPathResult, typeof XPathExpression, typeof XPathEvaluator, typeof document.evaluate, "
                + "typeof document.createExpression, typeof document.createNSResolver].join('|')", "the interfaces and the methods of Document");
        eq(f, "0,1,2,3,4,5,6,7,8,9|true", "[[XPathResult.ANY_TYPE, XPathResult.NUMBER_TYPE, XPathResult.STRING_TYPE, XPathResult.BOOLEAN_TYPE, "
                + "XPathResult.UNORDERED_NODE_ITERATOR_TYPE, XPathResult.ORDERED_NODE_ITERATOR_TYPE, XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, "
                + "XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, XPathResult.ANY_UNORDERED_NODE_TYPE, XPathResult.FIRST_ORDERED_NODE_TYPE].join(','), "
                + "document.evaluate('1', document).FIRST_ORDERED_NODE_TYPE === 9].join('|')", "the constants are on the interface and on the results");
        eq(f, "[object XPathResult]|[object XPathExpression]|[object XPathEvaluator]|true|true", "var res = document.evaluate('1', document), ex = document.createExpression('1'); "
                + "[Object.prototype.toString.call(res), Object.prototype.toString.call(ex), Object.prototype.toString.call(new XPathEvaluator()), res instanceof XPathResult, "
                + "ex instanceof XPathExpression].join('|')", "tags and instances");
        eq(f, "TypeError|TypeError|TypeError", "[function () { new XPathResult(); }, function () { new XPathExpression(); }, function () { XPathEvaluator(); }].map(err).join('|')",
                "only XPathEvaluator can be constructed, and only with new");
    }

    /**
     * Location paths: absolute, relative, descendants, the position predicates, the attribute tests and the abbreviations.
     * <p>
     * 위치 경로: 절대, 상대, 자손, 위치 술어, 속성 테스트, 약어.
     */
    public void testLocationPaths() {
        PageFixture f = load();
        eq(f, "l1,l2,l3,m1,m2", "q('//li')", "all list items in document order");
        eq(f, "l1,l2,l3,m1,m2", "q('//LI')", "an HTML document matches the names without regard to case");
        eq(f, "l2", "q('/html/body/div/ul[1]/li[2]')", "an absolute path with positions");
        eq(f, "l3,m2", "q('//li[last()]')", "last() counts in each list");
        eq(f, "l1,l2,m1,m2", "q('//li[position() < 3]')", "position() in each list");
        eq(f, "l1,m1", "q('//li[1]')", "//li[1] is the first li of each parent");
        eq(f, "l1", "q('(//li)[1]')", "(//li)[1] is the first of all");
        eq(f, "m2", "q('//ul[2]/li[last()]')", "positions inside a step");
        eq(f, "l1,l3", "q('//li[@class]')", "an attribute test");
        eq(f, "l1", "q(\"//li[@class='x']\")", "an attribute value");
        eq(f, "l1,l3", "q(\"//li[contains(@class, 'x')]\")", "contains");
        eq(f, "list,list2", "q('//li/..')", "the parent step");
        eq(f, "list", "q('//ul/self::ul[@id=\"list\"]')", "self");
        eq(f, "l2", "q('.//li[2]', document.getElementById('list'))", "a relative path from a context node");
        eq(f, "p", "q('../p', document.getElementById('list'))", "parent and then a sibling");
        eq(f, "l1,l2,l3", "q('*', document.getElementById('list'))", "the wildcard name test");
        eq(f, "3", "n('count(*)', document.getElementById('list'))", "count(*) of a context element");
        eq(f, "HTML", "q('/*')", "the root element by its name node (the wildcard at the root)");
        eq(f, "#document", "q('/')", "the root node is the document");
    }

    /**
     * All the axes, in the order that the specification gives them.
     * <p>
     * 모든 축. 규격이 정한 순서로.
     */
    public void testAxes() {
        PageFixture f = load();
        eq(f, "l3", "q(\"//li[@id='l2']/following-sibling::li\")", "following-sibling");
        eq(f, "l1", "q(\"//li[@id='l2']/preceding-sibling::li\")", "preceding-sibling");
        eq(f, "l1,l2", "q(\"//li[@id='l3']/preceding::li\")", "preceding: the result is in document order");
        eq(f, "l2,l3,list2,m1,m2,p,bold,lnk,n1,n2,SCRIPT", "q(\"//li[@id='l1']/following::*\")", "following (the script element after the div included)");
        eq(f, "HTML,BODY,root,p,bold", "q('//b/ancestor-or-self::*')", "ancestor-or-self in document order");
        eq(f, "root", "q('//b/ancestor::div')", "ancestor with a name");
        eq(f, "l1,l2,l3", "q(\"//ul[@id='list']/descendant::li\")", "descendant");
        eq(f, "list,l1,l2,l3", "q(\"//ul[@id='list']/descendant-or-self::*\")", "descendant-or-self");
        eq(f, "id", "q(\"//li[@id='l1']/@id\")", "the attribute axis: the attribute node is listed by its name");
        eq(f, "id,class", "q(\"//li[@id='l1']/@*\")", "all attributes of an element, in the order they were written");
        eq(f, "0", "n(\"count(//li[@id='l1']/namespace::*)\")", "the namespace axis is empty (the DOM has no namespace nodes)");
        eq(f, "bold|class|true", "var a = document.evaluate(\"//li[@id='l1']/@class\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue; "
                + "var p = document.evaluate('ancestor::*[1]', a, null, 9, null).singleNodeValue; [document.evaluate('//b', document, null, 9, null).singleNodeValue.id, a.name, "
                + "a.ownerElement.id === 'l1' && p.id === 'l1'].join('|')", "an attribute as a node: its parent is the owner element for the ancestor axis");
    }

    /**
     * The node tests: <code>text()</code>, <code>comment()</code>, <code>node()</code>.
     * <p>
     * 노드 테스트: <code>text()</code>, <code>comment()</code>, <code>node()</code>.
     */
    public void testNodeTests() {
        PageFixture f = load();
        eq(f, "#text,#text", "q('//p/text()')", "text nodes");
        eq(f, "Hello | world","var r = document.evaluate('//p/text()', document, null, 7, null); [r.snapshotItem(0).data, r.snapshotItem(1).data].join('|')", "the text nodes around the element");
        eq(f, "#comment", "q('//comment()')", "comments");
        eq(f, "3", "n('count(//p/node())')", "node() matches every kind of node");
        eq(f, "0", "n('count(//li/@id/text())')", "an attribute has no children");
        eq(f, "true", "b('//processing-instruction() or true()')", "processing-instruction() is a valid test");
    }

    /**
     * The operators, their precedence and associativity.
     * <p>
     * 연산자, 우선순위, 결합 방향.
     */
    public void testOperators() {
        PageFixture f = load();
        eq(f, "7|9|5|2|2.5|3|-3", "[n('1 + 2 * 3'), n('(1 + 2) * 3'), n('10 - 2 - 3'), n('2 * 3 mod 4'), n('5 div 2'), n('- - 3'), n('-3')].join('|')", "arithmetic: precedence and left associativity");
        eq(f, "1|-1|NaN|Infinity|-Infinity", "[n('5 mod 2'), n('-5 mod 2'), n('0 div 0'), n('1 div 0'), n('-1 div 0')].join('|')", "mod follows the dividend; division by zero");
        eq(f, "true|false|false|true", "[b('1 < 2'), b('2 < 2'), b('3 > 2 > 1'), b('1 < 2 = true()')].join('|')", "comparisons: relational binds tighter than equality; a boolean is a number");
        eq(f, "true|false|true|true", "[b('true() and not(false())'), b('true() and false()'), b('false() or true()'), b('1 = 1 and 2 = 2')].join('|')", "and, or");
        eq(f, "l1,l3", "q(\"//li[@id='l3'] | //li[@id='l1']\")", "a union is in document order");
        eq(f, "l1,m1,m2", "q('//li[1] | //li[last()][../@id=\"list2\"]')", "a union of paths");
        eq(f, "11|10", "[n('count(//li) * 2 + 1'), n('count(//li) div 2 * 4')].join('|')", "a function call as an operand of * and div; both are operators after an operand");
        eq(f, "root", "q(\"//div[@id='root' and @class='a']\")", "and between predicates");
        eq(f, "l1", "q(\"//li[. = 'one']\")", "a node compared with a string");
    }

    /**
     * The comparison rules of node sets (XPath 3.4).
     * <p>
     * 노드 집합의 비교 규칙(XPath 3.4).
     */
    public void testNodeSetComparisons() {
        PageFixture f = load();
        eq(f, "true|true|false", "[b(\"//li = 'two'\"), b(\"//li != 'two'\"), b(\"//li = 'nothing'\")].join('|')", "= and != are true if some node qualifies");
        eq(f, "true|true|false|true", "[b('//span[@class=\"n\"] > 2'), b('//span[@class=\"n\"] = 2.5'), b('//span[@class=\"n\"] > 3'), b('//span[@class=\"n\"] < 2')].join('|')",
                "a node set against a number: the string-values are numbers");
        eq(f, "true|false", "[b('//li = true()'), b('//nothing = true()')].join('|')", "a node set against a boolean: the boolean of the node set");
        eq(f, "true|false", "[b('//li = //li[2]'), b('//li = //span')].join('|')", "two node sets: some pair of string-values");
        eq(f, "false|true", "[b(\"//nothing = ''\"), b(\"not(//nothing = '')\")].join('|')", "an empty node set equals nothing");
    }

    /**
     * The string functions, with the examples of the Recommendation.
     * <p>
     * 문자열 함수. 권고안의 예를 포함한다.
     */
    public void testStringFunctions() {
        PageFixture f = load();
        eq(f, "abc|one|Hello big world", "[s(\"concat('a', 'b', 'c')\"), s('//li'), s('normalize-space(//p)')].join('|')", "concat, the string-value of the first node, normalize-space");
        eq(f, "234|234|12|12345|", "[s(\"substring('12345', 2, 3)\"), s(\"substring('12345', 1.5, 2.6)\"), s(\"substring('12345', 0, 3)\"), s(\"substring('12345', -42, 1 div 0)\"), "
                + "s(\"substring('12345', 0 div 0, 3)\")].join('|')", "substring and its rounding rules");
        eq(f, "1999|04/01|", "[s(\"substring-before('1999/04/01', '/')\"), s(\"substring-after('1999/04/01', '/')\"), s(\"substring-before('abc', 'x')\")].join('|')", "substring-before and substring-after");
        eq(f, "a b|BAr|AAA", "[s(\"normalize-space('  a   b ')\"), s(\"translate('bar', 'abc', 'ABC')\"), s(\"translate('--aaa--', 'abc-', 'ABC')\")].join('|')", "normalize-space and translate");
        eq(f, "5|3|0", "[n(\"string-length('héllo')\"), n('string-length(//li)'), n(\"string-length('')\")].join('|')", "string-length");
        eq(f, "true|false|true|false", "[b(\"starts-with('abc', 'ab')\"), b(\"starts-with('abc', 'bc')\"), b(\"contains('abc', 'bc')\"), b(\"contains('abc', 'x')\")].join('|')", "starts-with, contains");
        eq(f, "li|html|http://www.w3.org/1999/xhtml|id", "[s(\"name(//li[1])\"), s('local-name(/*)'), s('namespace-uri(/*)'), s(\"name(//li[1]/@id)\")].join('|')", "name, local-name, namespace-uri");
    }

    /**
     * The number and boolean functions, and the conversion of numbers to strings.
     * <p>
     * 숫자와 불리언 함수, 숫자를 문자열로 바꾸기.
     */
    public void testNumberAndBooleanFunctions() {
        PageFixture f = load();
        eq(f, "3|3.5|3|3|3|-2", "[n('count(//li[@id=\"l1\"] | //li[@id=\"l2\"] | //li[@id=\"m1\"])'), n('sum(//span)'), n('floor(3.7)'), n('ceiling(2.1)'), n('round(2.5)'), n('round(-2.5)')].join('|')",
                "count, sum, floor, ceiling, round (halves go to positive infinity)");
        eq(f, "12.5|NaN|NaN|NaN|NaN", "[n(\"number(' 12.5 ')\"), n(\"number('abc')\"), n(\"number('1e3')\"), n(\"number('+1')\"), n(\"number('')\")].join('|')", "number() of strings");
        eq(f, "1|0|2.5", "[n('number(true())'), n('number(false())'), n('number(//span[2])')].join('|')", "number() of booleans and of a node set");
        eq(f, "true|false|true|false|true|false", "[b('boolean(1)'), b('boolean(0)'), b(\"boolean('a')\"), b(\"boolean('')\"), b('boolean(//li)'), b('boolean(//nothing)')].join('|')", "boolean()");
        eq(f, "1.5|NaN|Infinity|-Infinity|1000000000000000000000|0.0000001|0|true", "[s('1.5'), s('0 div 0'), s('1 div 0'), s('-1 div 0'), s('1000000000000000000000'), s('0.0000001'), s('-0'), "
                + "s('true()') === 'true'].join('|')", "number to string: no exponent, no trailing zeros, the special values");
        eq(f, "true|true|false", "[b(\"//p[lang('en')]\"), b(\"//b[lang('en-US')]\"), b(\"//p[lang('fr')]\")].join('|')", "lang(): the language is inherited, a prefix before a hyphen matches");
        eq(f, "l2|l1,l3", "[q(\"id('l2')\"), q(\"id('l1 l3')\")].join('|')", "id()");
    }

    /**
     * The result types and the conversions of <code>document.evaluate</code>.
     * <p>
     * <code>document.evaluate</code> 의 결과 종류와 변환.
     */
    public void testResultTypes() {
        PageFixture f = load();
        eq(f, "1|2|3|4", "[document.evaluate('1', document).resultType, document.evaluate('\"a\"', document).resultType, document.evaluate('true()', document).resultType, "
                + "document.evaluate('//li', document).resultType].join('|')", "ANY_TYPE: number, string, boolean, and an unordered node iterator for a node set");
        eq(f, "l1|l2|l3|m1|m2|null", "var it = document.evaluate('//li', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null), a = [], x; while ((x = it.iterateNext())) { a.push(x.id); } "
                + "a.push(String(it.iterateNext())); a.join('|')", "an iterator");
        eq(f, "5|l1|m2||", "var r = document.evaluate('//li', document, null, 7, null); [r.snapshotLength, r.snapshotItem(0).id, r.snapshotItem(4).id, r.snapshotItem(5), r.snapshotItem(-1)].join('|')",
                "a snapshot");
        eq(f, "l1|l1|false", "var r = document.evaluate('//li', document, null, 9, null), r2 = document.evaluate('//li', document, null, 8, null); [r.singleNodeValue.id, r2.singleNodeValue.id, "
                + "r.invalidIteratorState].join('|')", "a single node");
        eq(f, "one|3.5|true|", "[document.evaluate('//li', document, null, 2).stringValue, document.evaluate('sum(//span)', document, null, 1).numberValue, "
                + "document.evaluate('//li', document, null, 3).booleanValue, document.evaluate('//nothing', document, null, 2).stringValue].join('|')", "conversions to the other types");
        eq(f, "TypeError|TypeError|TypeError|TypeError|NotSupportedError", "[function () { document.evaluate('1', document, null, 7); }, function () { document.evaluate('1', document, null, 9); }, "
                + "function () { document.evaluate('//li', document, null, 1).stringValue; }, function () { document.evaluate('//li', document, null, 7).iterateNext(); }, "
                + "function () { document.evaluate('1', document, null, 10); }].map(err).join('|')", "a node type for a value that is not a node set, a getter of another type, a bad type");
        eq(f, "li|2", "var a = document.evaluate(\"//li[@id='l2']/@id\", document, null, 9, null).singleNodeValue; [document.evaluate('..', a, null, 9, null).singleNodeValue.localName, a.name === 'id' ? 2 : 0].join('|')",
                "an attribute is a valid context node");
    }

    /**
     * A node iterator becomes invalid when the document changes; a snapshot does not.
     * <p>
     * 문서가 바뀌면 노드 반복자는 무효가 되고 스냅숏은 그렇지 않다.
     */
    public void testInvalidIterator() {
        PageFixture f = load();
        eq(f, "l1|false|true|InvalidStateError|5|l1", "var it = document.evaluate('//li', document, null, 5, null), sn = document.evaluate('//li', document, null, 7, null); var first = it.iterateNext().id; "
                + "var before = it.invalidIteratorState; document.getElementById('l2').setAttribute('title', 't'); [first, before, it.invalidIteratorState, err(function () { it.iterateNext(); }), sn.snapshotLength, "
                + "sn.snapshotItem(0).id].join('|')", "an iterator is invalid after a change, a snapshot stays");
    }

    /**
     * Expression objects, evaluators and resolvers.
     * <p>
     * 식 객체, 평가기, 리졸버.
     */
    public void testExpressionsEvaluatorsAndResolvers() {
        PageFixture f = load();
        eq(f, "3|3|2", "var ex = document.createExpression('li'); [ex.evaluate(document.getElementById('list'), 7).snapshotLength, ex.evaluate(document.getElementById('list'), 7).snapshotLength, "
                + "ex.evaluate(document.getElementById('list2'), 7).snapshotLength].join('|')", "a compiled expression evaluates for different context nodes");
        eq(f, "5|5", "var ev = new XPathEvaluator(); [ev.evaluate('count(//li)', document, null, 1, null).numberValue === 5 ? 5 : 0, ev.createExpression('//li').evaluate(document, 7).snapshotLength].join('|')",
                "XPathEvaluator has evaluate and createExpression");
        eq(f, "true", "String(document.createNSResolver(document) === document)", "createNSResolver returns its node");
        eq(f, "l1,l2,l3,m1,m2|l1,l2,l3,m1,m2|l1,l2,l3,m1,m2", "var ns = 'http://www.w3.org/1999/xhtml'; function ids(res) { var r = document.evaluate('//h:li', document, res, 7, null), a = []; "
                + "for (var i = 0; i < r.snapshotLength; i++) { a.push(r.snapshotItem(i).id); } return a.join(','); } "
                + "var asFunction = function (p) { return p === 'h' ? ns : null; }, asObject = { lookupNamespaceURI: asFunction }; "
                + "[ids(asFunction), ids(asObject), document.createExpression('//h:li', asFunction).evaluate(document, 7).snapshotLength === 5 ? 'l1,l2,l3,m1,m2' : 'wrong'].join('|')",
                "prefixes through a function, an object with lookupNamespaceURI, and the resolver of createExpression");
    }

    /**
     * An XML document: the names are case sensitive, an unprefixed name has no namespace, and a prefix needs a resolver.
     * <p>
     * XML 문서: 이름은 대소문자를 구분하고 접두사 없는 이름은 네임스페이스가 없으며 접두사는 리졸버가 필요하다.
     */
    public void testXmlDocument() {
        PageFixture f = load();
        eq(f, "x|X|a:x|3", "var d = new DOMParser().parseFromString('<r xmlns:a=\"urn:u\"><a:x>1</a:x><X>2</X><x>3</x></r>', 'application/xml'); "
                + "function ids(e, res) { var r = document.evaluate(e, d, res || null, 7, null), a = []; for (var i = 0; i < r.snapshotLength; i++) { a.push(r.snapshotItem(i).nodeName); } return a.join(','); } "
                + "var ns = function (p) { return p === 'a' ? 'urn:u' : null; }; [ids('/r/x'), ids('/r/X'), ids('//a:x', ns), document.evaluate('count(/r/*)', d, ns, 1, null).numberValue].join('|')",
                "XML: case sensitive, no namespace for a name without a prefix, a prefix through the resolver");
    }

    /**
     * The errors of a bad expression, a variable, a function that does not exist and a prefix that cannot be resolved.
     * <p>
     * 잘못된 식, 변수, 없는 함수, 해석할 수 없는 접두사의 오류.
     */
    public void testErrors() {
        PageFixture f = load();
        eq(f, "SyntaxError|SyntaxError|SyntaxError|SyntaxError|SyntaxError|SyntaxError", "['//li[', '//', '', 'foo()', 'count()', '1 +'].map(function (e) { return err(function () { document.evaluate(e, document, null, 0, null); }); }).join('|')",
                "a text that is not an expression, an unknown function, a wrong number of arguments");
        eq(f, "TypeError|NamespaceError|TypeError|TypeError", "[err(function () { document.evaluate('$x', document); }), err(function () { document.evaluate('//x:y', document); }), "
                + "err(function () { document.evaluate('1'); }), err(function () { document.evaluate('1', {}); })].join('|')", "a variable, a prefix without a resolver, missing and bad arguments");
        eq(f, "TypeError", "err(function () { document.evaluate('count(1)', document); })", "count() needs a node set");
    }
}
