package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the notifications of the selection of a document (plan item P11-08): every change of the selection reaches the
 * {@link SelectionListener} that {@link Document#setSelectionListener(SelectionListener)} installed, whoever makes it (the Selection methods,
 * a script that changes the live range object, or a change of the tree under the range), and nothing is reported for a range that the
 * selection does not use or for a change that moves no boundary point. Also the helper that orders two boundary points, and the clipboard
 * data of the <code>copy</code> event.
 * <p>
 * 문서의 선택 통지 시험(계획 P11-08): 선택이 바뀔 때마다(Selection 메서드, 살아 있는 범위 객체를 바꾸는 스크립트, 범위 아래의 트리 변경 중 누가 바꾸든)
 * {@link Document#setSelectionListener(SelectionListener)} 가 설치한 {@link SelectionListener} 에 전달되며, 선택이 쓰지 않는 범위나 경계점을 옮기지 않는 변경은
 * 알리지 않는다. 두 경계점의 순서를 정하는 도우미와 <code>copy</code> 이벤트의 클립보드 데이터도 시험한다.
 */
public class SelectionListenerTest {

    /**
     * The document of the case.
     * <p>
     * 사례의 문서.
     */
    private Document doc;

    /**
     * The changes that the listener heard of.
     * <p>
     * 리스너가 들은 변경.
     */
    private List<String> heard;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SelectionListenerTest() {
    }

    /**
     * Creates a document with a listener before each case.
     * <p>
     * 사례마다 리스너가 있는 문서를 만든다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, "<!doctype html><div id=d><p id=p>Hello <b id=b>big</b> world</p><p id=q>Second</p></div>").run();
        heard = new ArrayList<String>();
        doc.setSelectionListener(new SelectionListener() {
            /**
             * Records the change with the selected text.
             * <p>
             * 변경을 선택된 텍스트와 함께 기록한다.
             *
             * @param selection the selection / 선택
             */
            public void selectionChanged(Selection selection) {
                heard.add(selection.toString());
            }
        });
    }

    /**
     * Returns an element by id.
     * <p>
     * id 로 요소를 반환한다.
     *
     * @param id the id / id
     * @return the element / 요소
     */
    private Element el(String id) {
        return doc.getElementById(id);
    }

    /**
     * Every method of the selection that changes it tells the listener; emptying an empty selection does not.
     * <p>
     * 선택을 바꾸는 선택의 모든 메서드가 리스너에 알린다. 빈 선택을 비우는 것은 알리지 않는다.
     */
    public void testSelectionMethodsNotify() {
        Selection sel = doc.getSelection();
        sel.removeAllRanges();
        Check.equal(0, heard.size(), "emptying an empty selection is not a change");
        sel.selectAllChildren(el("p"));
        Check.equal(1, heard.size(), "selectAllChildren");
        Check.equal("Hello big world", heard.get(0), "the listener can read the new selection");
        sel.collapse(el("p").getFirstChild(), 2);
        Check.equal(2, heard.size(), "collapse");
        sel.extend(el("p").getFirstChild(), 5);
        Check.equal(3, heard.size(), "extend");
        Check.equal("llo", heard.get(2), "the extended text");
        sel.setBaseAndExtent(el("q").getFirstChild(), 0, el("q").getFirstChild(), 3);
        Check.equal(4, heard.size(), "setBaseAndExtent");
        sel.removeAllRanges();
        Check.equal(5, heard.size(), "emptying");
        Range r = new Range(doc);
        r.selectNodeContents(el("q"));
        sel.addRange(r);
        Check.equal(6, heard.size(), "addRange");
        sel.removeRange(r);
        Check.equal(7, heard.size(), "removeRange");
        sel.collapse(null, 0);
        Check.equal(7, heard.size(), "collapse to nothing on an empty selection is no change");
    }

    /**
     * A script that changes the live range object that it got from the selection is heard, a range that the selection does not use is not.
     * <p>
     * 선택에서 얻은 살아 있는 범위 객체를 바꾸는 스크립트는 들리고, 선택이 쓰지 않는 범위는 들리지 않는다.
     */
    public void testChangesOfTheLiveRangeAreHeard() {
        Selection sel = doc.getSelection();
        sel.selectAllChildren(el("q"));
        heard.clear();
        Range live = sel.getRangeAt(0);
        live.setEnd(el("q").getFirstChild(), 3);
        Check.equal(1, heard.size(), "setEnd on the range of the selection");
        Check.equal("Sec", heard.get(0), "the selection shows the new end");
        live.collapse(true);
        Check.equal(2, heard.size(), "collapse of the range of the selection");
        Range other = new Range(doc);
        other.selectNodeContents(el("p"));
        other.setEnd(el("p").getFirstChild(), 1);
        Check.equal(2, heard.size(), "a range of the script that is not the selection tells nobody");
        sel.removeAllRanges();
        heard.clear();
        live.setStart(el("q").getFirstChild(), 1);
        Check.equal(0, heard.size(), "a range that was taken out of the selection tells nobody");
    }

    /**
     * A change of the tree under the range that moves a boundary point is heard; a change elsewhere is not.
     * <p>
     * 경계점을 옮기는 범위 아래의 트리 변경은 들리고, 다른 곳의 변경은 들리지 않는다.
     */
    public void testTreeChangesUnderTheRangeAreHeard() {
        Selection sel = doc.getSelection();
        sel.setBaseAndExtent(el("p").getFirstChild(), 2, el("q").getFirstChild(), 3);
        heard.clear();
        Node q = el("q").getFirstChild();
        ((Text) q).replaceData(4, 1, "XY");
        Check.equal(0, heard.size(), "text replaced after the end of the range moves nothing");
        ((Text) el("p").getFirstChild()).replaceData(0, 1, "");
        Check.equal(1, heard.size(), "text removed before the start moves the start offset");
        el("d").appendChild(doc.createElement("hr"));
        Check.equal(1, heard.size(), "a node appended elsewhere moves nothing");
        el("d").removeChild(el("q"));
        Check.equal(2, heard.size(), "removing the node of the end moves the end");
        Check.equal(1, sel.getRangeCount(), "the selection still has its range");
    }

    /**
     * Without a listener nothing is reported, and removing the listener stops the reports.
     * <p>
     * 리스너가 없으면 아무것도 알리지 않으며, 리스너를 제거하면 통지가 멈춘다.
     */
    public void testListenerCanBeRemoved() {
        Selection sel = doc.getSelection();
        doc.setSelectionListener(null);
        sel.selectAllChildren(el("p"));
        Check.equal(0, heard.size(), "no listener, no report");
        Document fresh = Document.createHtmlDocument();
        fresh.setSelectionListener(new SelectionListener() {
            /**
             * Records that the listener of a document that had no selection yet works.
             * <p>
             * 아직 선택이 없던 문서의 리스너가 동작함을 기록한다.
             *
             * @param selection the selection / 선택
             */
            public void selectionChanged(Selection selection) {
                heard.add("fresh");
            }
        });
        fresh.getSelection().collapse(fresh, 0);
        Check.equal(1, heard.size(), "the listener of a document is given to the selection that is created later");
    }

    /**
     * Two boundary points are ordered in tree order.
     * <p>
     * 두 경계점은 트리 순서로 정렬된다.
     */
    public void testComparePointOrder() {
        Node a = el("p").getFirstChild();
        Node b = el("q").getFirstChild();
        Check.isTrue(Range.comparePointOrder(a, 1, b, 0) < 0, "an earlier node is before");
        Check.isTrue(Range.comparePointOrder(b, 0, a, 5) > 0, "a later node is after");
        Check.equal(0, Range.comparePointOrder(a, 3, a, 3), "the same point");
        Check.isTrue(Range.comparePointOrder(a, 3, a, 4) < 0, "offsets in one node");
    }

    /**
     * The data of the clipboard: types are lower case and the legacy names are mapped, data replaces, clearData removes, and the change by
     * a script is told apart from what the component put in.
     * <p>
     * 클립보드의 데이터: 종류는 소문자이고 옛 이름은 바뀌며, 데이터는 대체되고, clearData 는 지우며, 스크립트의 변경은 컴포넌트가 넣은 것과 구별된다.
     */
    public void testClipboardData() {
        ClipboardData data = new ClipboardData();
        data.put("TEXT/Plain", "hello");
        Check.equal("hello", data.getData("text"), "the legacy name text is text/plain");
        Check.isFalse(data.isChanged(), "what the component put in is not a change by a script");
        data.setData("text/html", "<b>x</b>");
        Check.isTrue(data.isChanged(), "setData is a change by a script");
        Check.equal("text/plain,text/html", data.getTypes().get(0) + "," + data.getTypes().get(1), "types in the order they were added");
        data.setData("Text/Plain", "again");
        Check.equal("again", data.getData("text/plain"), "replaced");
        Check.equal(2, data.getTypes().size(), "no duplicate type");
        data.clearData("text/html");
        Check.isFalse(data.hasData("text/html"), "cleared");
        Check.equal("", data.getData("text/html"), "no data gives an empty string");
        data.clearData(null);
        Check.equal(0, data.getTypes().size(), "everything cleared");
        ClipboardEvent event = new ClipboardEvent("copy", true, true, data);
        Check.same(data, event.getClipboardData(), "the event carries the data");
        Check.isTrue(event.isCancelable() && event.isBubbles(), "copy bubbles and can be cancelled");
    }
}
