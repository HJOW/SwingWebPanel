package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script interfaces of ranges, selections and traversal (plan item P20-03): <code>document.createRange()</code>,
 * <code>new Range()</code>, the content operations as scripts use them, <code>getSelection()</code>, <code>createTreeWalker()</code> and
 * <code>createNodeIterator()</code> with function and object filters, the errors as DOMExceptions and TypeErrors, and the rectangles of a
 * range from the layout hook of the environment.
 * <p>
 * 범위, 선택, 탐색의 스크립트 인터페이스 시험(계획 P20-03): <code>document.createRange()</code>, <code>new Range()</code>, 스크립트가 쓰는 방식의 내용 연산,
 * <code>getSelection()</code>, 함수 필터와 객체 필터가 있는 <code>createTreeWalker()</code>, <code>createNodeIterator()</code>, DOMException 과 TypeError
 * 오류, 환경의 레이아웃 훅에서 오는 범위의 사각형.
 */
public class RangeApiTest {

    /**
     * An environment whose layout hook answers with a box per element id: <code>a</code> at (0,0,100,20), <code>b</code> at (0,20,100,30),
     * <code>c</code> at (10,60,40,10); other elements are empty.
     * <p>
     * 레이아웃 훅이 요소 id 마다 상자로 답하는 환경: <code>a</code> 는 (0,0,100,20), <code>b</code> 는 (0,20,100,30), <code>c</code> 는 (10,60,40,10),
     * 다른 요소는 비어 있다.
     */
    private static final class Boxes extends HostEnvironment {
        /**
         * The manual scheduler.
         * <p>
         * 수동 스케줄러.
         */
        final PageFixture.ManualScheduler scheduler = new PageFixture.ManualScheduler();

        /**
         * Returns the scheduler.
         * <p>
         * 스케줄러를 반환한다.
         *
         * @return the scheduler / 스케줄러
         */
        public Scheduler getScheduler() {
            return scheduler;
        }

        /**
         * Answers with the box of the element id.
         * <p>
         * 요소 id 의 상자로 답한다.
         *
         * @param element the element / 요소
         * @return the box / 상자
         */
        public double[] getElementBounds(Element element) {
            String id = element.getId();
            if ("a".equals(id)) {
                return new double[] { 0, 0, 100, 20 };
            } else if ("b".equals(id)) {
                return new double[] { 0, 20, 100, 30 };
            } else if ("c".equals(id)) {
                return new double[] { 10, 60, 40, 10 };
            }
            return new double[4];
        }
    }

    /**
     * The scripts of the page of the case.
     * <p>
     * 사례 페이지의 스크립트.
     */
    private PageScripts scripts;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RangeApiTest() {
    }

    /**
     * Loads a page with a small tree before each case.
     * <p>
     * 사례마다 작은 트리가 있는 페이지를 로드한다.
     */
    public void setUp() {
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setUrl("http://example.test/page.html");
        scripts = new PageScripts(doc, new Boxes());
        HtmlParser parser = new HtmlParser(doc, "<!doctype html><body><div id=root><p id=a>Hello <b id=bold>big</b> world</p><p id=b>Second</p>"
                + "<p id=c>Third</p></div>");
        parser.setScripting(true);
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
    }

    /**
     * Evaluates a script that must not throw and returns its completion value as text.
     * <p>
     * 던지지 않아야 하는 스크립트를 실행해 완료 값을 문자열로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the text / 문자열
     */
    private String run(String source) {
        String result = scripts.evaluate(source, "test");
        Check.notNull(result, "the script threw: " + source);
        return result;
    }

    /**
     * Returns the text of the error that a statement throws (name, code, whether it is a DOMException).
     * <p>
     * 문장이 던지는 오류의 내용(이름, 코드, DOMException 인지)을 반환한다.
     *
     * @param statement the statement / 문장
     * @return <code>name:code:isDomException</code>, or "no error" / <code>이름:코드:DOMException 여부</code>. 오류가 없으면 "no error"
     */
    private String errorOf(String statement) {
        return run("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name + ':' + e.code + ':' + (e instanceof DOMException); } })()");
    }

    /**
     * The interfaces and constants exist; ranges are made by document.createRange() and new Range().
     * <p>
     * 인터페이스와 상수가 있으며 범위는 document.createRange() 와 new Range() 로 만든다.
     */
    public void testInterfacesAndConstructors() {
        Check.equal("true", run("String(document.createRange() instanceof Range)"), "createRange");
        Check.equal("true", run("String(new Range() instanceof Range)"), "new Range");
        Check.equal("0:1:2:3", run("[Range.START_TO_START, Range.START_TO_END, Range.END_TO_END, Range.END_TO_START].join(':')"), "constants");
        Check.equal("[object Range]", run("Object.prototype.toString.call(new Range())"), "toStringTag");
        Check.equal("true:true", run("var q = new Range(); (q.startContainer === document) + ':' + q.collapsed"), "a new range is collapsed in the document");
        Check.equal("1:2:3:4294967295", run("[NodeFilter.FILTER_ACCEPT, NodeFilter.FILTER_REJECT, NodeFilter.FILTER_SKIP, NodeFilter.SHOW_ALL].join(':')"),
                "NodeFilter constants");
        Check.equal("1:4:128", run("[NodeFilter.SHOW_ELEMENT, NodeFilter.SHOW_TEXT, NodeFilter.SHOW_COMMENT].join(':')"), "show constants");
        Check.equal("TypeError", run("(function () { try { Range(); } catch (e) { return e.name; } })()"), "needs new");
        Check.equal("true", run("String(window.getSelection() === document.getSelection())"), "one selection");
        Check.equal("[object Selection]", run("Object.prototype.toString.call(getSelection())"), "selection tag");
    }

    /**
     * Boundary points through the script API keep node identity and raise DOMException and TypeError like the Standard says.
     * <p>
     * 스크립트 API 로 다루는 경계점은 노드 동일성을 유지하고 표준대로 DOMException 과 TypeError 를 낸다.
     */
    public void testBoundariesAndErrors() {
        Check.equal("true:2:true:2:false:true", run("var a = document.getElementById('a'); var t = a.firstChild; var r = document.createRange();"
                + " r.setStart(t, 2); r.setEnd(a, 2); (r.startContainer === t) + ':' + r.startOffset + ':' + (r.endContainer === a) + ':' + r.endOffset + ':'"
                + " + r.collapsed + ':' + (r.commonAncestorContainer === a)"), "boundary points");
        Check.equal("IndexSizeError:1:true", errorOf("document.createRange().setStart(document.body, 99)"), "offset too large");
        Check.equal("InvalidNodeTypeError:24:true", errorOf("document.createRange().setStart(document.firstChild, 0)"), "a document type");
        Check.equal("TypeError:undefined:false", errorOf("document.createRange().setStart({}, 0)"), "not a node");
        Check.equal("TypeError:undefined:false", errorOf("document.createRange().compareBoundaryPoints(0, {})"), "not a range");
        Check.equal("NotSupportedError:9:true", errorOf("var q = document.createRange(); q.compareBoundaryPoints(9, q)"), "unknown mode");
        Check.equal("WrongDocumentError:4:true", errorOf("var q2 = document.createRange(); var o = document.createRange(); o.setStart(document.createElement('x'), 0);"
                + " q2.compareBoundaryPoints(0, o)"), "different trees");
        Check.equal("0:-1:1", run("var x = document.createRange(), y = document.createRange(); x.selectNodeContents(document.body); y.selectNodeContents(document.body);"
                + " [x.compareBoundaryPoints(Range.START_TO_START, y), x.compareBoundaryPoints(Range.END_TO_START, y), x.compareBoundaryPoints(Range.START_TO_END, y)].join(':')"),
                "compare");
    }

    /**
     * The content operations return DocumentFragment objects, change the tree like the Standard says, and leave the ranges collapsed where the
     * content was.
     * <p>
     * 내용 연산은 DocumentFragment 를 반환하고 표준대로 트리를 바꾸며 범위를 내용이 있던 자리로 접는다.
     */
    public void testContentOperations() {
        Check.equal("true:3:llo big world", run("var a = document.getElementById('a'), b = document.getElementById('b'); var r = document.createRange();"
                + " r.setStart(a.firstChild, 2); r.setEnd(a, 3); var f = r.cloneContents();"
                + " (f instanceof DocumentFragment) + ':' + f.childNodes.length + ':' + f.textContent"), "cloneContents");
        Check.equal("Hello big world|Second", run("a.textContent + '|' + b.textContent"), "the tree did not change");
        Check.equal("llo big world:He:true:1", run("var r2 = document.createRange(); r2.setStart(a.firstChild, 2); r2.setEnd(a, 3); var g = r2.extractContents();"
                + " g.textContent + ':' + a.textContent + ':' + r2.collapsed + ':' + r2.startOffset"), "extractContents");
        run("var r3 = document.createRange(); r3.selectNodeContents(b); r3.deleteContents();");
        Check.equal("", run("b.textContent"), "deleteContents");
        run("var em = document.createElement('em'); em.textContent = 'E'; var r4 = document.createRange(); r4.setStart(a.firstChild, 1); r4.setEnd(a.firstChild, 1);"
                + " r4.insertNode(em);");
        Check.equal("H<em>E</em>e", run("a.innerHTML"), "insertNode splits the text");
        run("var mark = document.createElement('mark'); var r5 = document.createRange(); r5.setStart(a.firstChild, 0); r5.setEnd(a.firstChild, 1);"
                + " r5.surroundContents(mark);");
        Check.equal("<mark>H</mark><em>E</em>e", run("a.innerHTML"), "surroundContents");
        Check.equal("InvalidStateError:11:true", errorOf("var r6 = document.createRange(); r6.setStart(a.getElementsByTagName('mark')[0].firstChild, 0); r6.setEnd(a.lastChild, 1);"
                + " r6.surroundContents(document.createElement('s'))"), "partially contained element");
        Check.equal("true:2:b", run("var f2 = document.createRange(); f2.selectNodeContents(document.getElementById('c')); var cf = f2.createContextualFragment('<b>x</b><i>y</i>');"
                + " (cf instanceof DocumentFragment) + ':' + cf.childNodes.length + ':' + cf.firstChild.localName"), "createContextualFragment");
    }

    /**
     * A live range moves with the tree when scripts change it.
     * <p>
     * 살아 있는 범위는 스크립트가 트리를 바꿀 때 트리와 함께 움직인다.
     */
    public void testLiveRangeInScripts() {
        Check.equal("true:1", run("var root = document.getElementById('root'); var lr = document.createRange(); lr.setStart(root, 2); lr.setEnd(root, 3);"
                + " root.removeChild(root.firstChild); (lr.startContainer === root) + ':' + lr.startOffset"), "a removal before the range");
        Check.equal("2", run("root.insertBefore(document.createElement('hr'), root.firstChild); String(lr.startOffset)"), "an insertion before the range");
        Check.equal("true:1", run("var inner = document.getElementById('b'); var lr2 = document.createRange(); lr2.setStart(inner.firstChild, 3);"
                + " root.removeChild(inner); (lr2.startContainer === root) + ':' + lr2.startOffset"), "a boundary inside a removed node");
        Check.equal("5:7", run("var tn = document.getElementById('c').firstChild; var lr3 = document.createRange(); lr3.setStart(tn, 3); lr3.setEnd(tn, 5);"
                + " tn.insertData(0, 'XY'); lr3.startOffset + ':' + lr3.endOffset"), "character data inserted before the range");
    }

    /**
     * Selection through scripts: collapse, extend, direction, identity of the range, containsNode, toString and removeAllRanges.
     * <p>
     * 스크립트로 다루는 선택: collapse, extend, 방향, 범위의 동일성, containsNode, toString, removeAllRanges.
     */
    public void testSelection() {
        Check.equal("0:None:true:null", run("var s = getSelection(); s.rangeCount + ':' + s.type + ':' + s.isCollapsed + ':' + s.anchorNode"), "empty selection");
        Check.equal("1:Caret:true:true:3", run("var a = document.getElementById('a'); s.collapse(a.firstChild, 3);"
                + " s.rangeCount + ':' + s.type + ':' + s.isCollapsed + ':' + (s.anchorNode === a.firstChild) + ':' + s.focusOffset"), "collapse");
        Check.equal("Range:forward:lo big wo", run("s.extend(a.lastChild, 3); s.type + ':' + s.direction + ':' + s.toString()"), "extend");
        Check.equal("true:true", run("var rg = s.getRangeAt(0); (rg === s.getRangeAt(0)) + ':' + (rg.startContainer === a.firstChild)"), "the range object is stable");
        run("s.removeAllRanges(); var mine = document.createRange(); mine.selectNodeContents(document.getElementById('b')); s.addRange(mine);");
        Check.equal("Second:true:true:false", run("s.toString() + ':' + (s.getRangeAt(0) === mine) + ':' + s.containsNode(document.getElementById('b'), false)"
                + " + ':' + s.containsNode(document.getElementById('c'), true)"), "addRange and containsNode");
        Check.equal("IndexSizeError:1:true", errorOf("s.getRangeAt(3)"), "getRangeAt out of range");
        Check.equal("Third", run("s.selectAllChildren(document.getElementById('c')); s.toString()"), "selectAllChildren");
        Check.equal("backward", run("s.setBaseAndExtent(document.getElementById('c').firstChild, 5, a.firstChild, 1); s.direction"), "setBaseAndExtent backwards");
        Check.equal("H|null", run("s.deleteFromDocument(); a.textContent + '|' + document.getElementById('b')"), "deleteFromDocument");
        Check.equal("InvalidStateError:11:true", errorOf("getSelection().removeAllRanges(); getSelection().extend(document.body, 0)"), "extend with no range");
    }

    /**
     * Tree walkers and node iterators from scripts: whatToShow, function and object filters, the filter attribute, results of callbacks,
     * errors thrown by the callback, and the live iterator.
     * <p>
     * 스크립트의 트리 걷기 도구와 노드 반복자: whatToShow, 함수와 객체 필터, filter 속성, 콜백의 결과, 콜백이 던진 오류, 살아 있는 반복자.
     */
    public void testTraversalFromScripts() {
        Check.equal("root a bold b c", run("var seen = []; var it = document.createNodeIterator(document.getElementById('root'), NodeFilter.SHOW_ELEMENT);"
                + " var n; while ((n = it.nextNode())) { seen.push(n.id); } seen.join(' ')"), "node iterator over the elements");
        Check.equal("Hello |big| world|Second|Third", run("var texts = []; var it2 = document.createNodeIterator(document.getElementById('root'), NodeFilter.SHOW_TEXT);"
                + " var t; while ((t = it2.nextNode())) { texts.push(t.data); } texts.join('|')"), "text only");
        Check.equal("a b c", run("var ids = []; var w = document.createTreeWalker(document.getElementById('root'), NodeFilter.SHOW_ELEMENT, function (node) {"
                + " return node.id === 'bold' ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT; });"
                + " var x; while ((x = w.nextNode())) { ids.push(x.id); } ids.join(' ')"), "a function filter that rejects a subtree");
        Check.equal("a|b", run("var obj = { count: 0, acceptNode: function (nd) { this.count++; return nd.id === 'c' ? NodeFilter.FILTER_SKIP : NodeFilter.FILTER_ACCEPT; } };"
                + " var w2 = document.createTreeWalker(document.getElementById('root'), NodeFilter.SHOW_ELEMENT, obj);"
                + " w2.firstChild() ? w2.currentNode.id + '|' + w2.nextSibling().id : 'none'"), "an object filter with acceptNode");
        Check.equal("true:true:4294967295:true", run("[w2.filter === obj, w.filter !== null, document.createTreeWalker(document.body).whatToShow,"
                + " document.createNodeIterator(document.body).filter === null].join(':')"), "attributes");
        Check.equal("0", run("var nothing = document.createNodeIterator(document.body, null); var cnt = 0; while (nothing.nextNode()) { cnt++; } String(cnt)"),
                "null whatToShow shows nothing");
        Check.equal("boom", run("(function () { try { document.createTreeWalker(document.body, NodeFilter.SHOW_ALL, function () { throw new Error('boom'); }).nextNode(); }"
                + " catch (e) { return e.message; } })()"), "an error of the callback goes through");
        Check.equal("InvalidStateError:11:true", errorOf("var re = document.createNodeIterator(document.body, NodeFilter.SHOW_ALL, function () { re.nextNode(); return 1; });"
                + " re.nextNode()"), "the filter cannot re-enter the traversal");
        Check.equal("TypeError:undefined:false", errorOf("document.createTreeWalker(5)"), "the root must be a node");
        Check.equal("TypeError:undefined:false", errorOf("document.createNodeIterator(document.body, 4, 5)"), "the filter must be an object");
        Check.equal("true:b:c", run("var live = document.createNodeIterator(document.getElementById('root'), NodeFilter.SHOW_ELEMENT); live.nextNode(); live.nextNode();"
                + " document.getElementById('root').removeChild(document.getElementById('a'));"
                + " (live.referenceNode === document.getElementById('root')) + ':' + live.nextNode().id + ':' + live.nextNode().id"),
                "the live iterator after a removal");
    }

    /**
     * The rectangles of a range come from the layout hook: covered elements give their boxes, covered text the box of its element, and a
     * collapsed range gives nothing.
     * <p>
     * 범위의 사각형은 레이아웃 훅에서 온다: 덮인 요소는 자기 상자를, 덮인 텍스트는 그 요소의 상자를 주고 접힌 범위는 아무것도 주지 않는다.
     */
    public void testRangeRectangles() {
        Check.equal("2:0,20,100,30:10,60,40,10", run("var root = document.getElementById('root'); var r = document.createRange(); r.setStart(root, 1); r.setEnd(root, 3);"
                + " var rs = r.getClientRects(); rs.length + ':' + [rs[0], rs[1]].map(function (q) { return [q.x, q.y, q.width, q.height].join(','); }).join(':')"),
                "two covered elements");
        Check.equal("0,20,100,70", run("var bb = r.getBoundingClientRect(); [bb.left, bb.top, bb.right, bb.bottom].join(',')"), "the bounding rectangle is the union");
        Check.equal("1:0,0,100,20", run("var tr = document.createRange(); tr.setStart(document.getElementById('a').firstChild, 1); tr.setEnd(document.getElementById('a').firstChild, 3);"
                + " var ts = tr.getClientRects(); ts.length + ':' + [ts[0].x, ts[0].y, ts[0].width, ts[0].height].join(',')"), "text inside an element uses its box");
        Check.equal("0:0,0,0,0", run("var cr = document.createRange(); cr.setStart(root, 1); cr.collapse(true); var cb = cr.getBoundingClientRect();"
                + " cr.getClientRects().length + ':' + [cb.x, cb.y, cb.width, cb.height].join(',')"), "a collapsed range has no rectangle");
    }
}
