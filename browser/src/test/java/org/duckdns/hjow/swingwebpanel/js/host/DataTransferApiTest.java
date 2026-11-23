package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.DataTransferItem;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.DragEvent;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the interfaces that scripts use in drag and drop and clipboard events (plan item P20-06): <code>DataTransfer</code>
 * (<code>effectAllowed</code>, <code>dropEffect</code>, <code>types</code>, <code>getData</code>, <code>setData</code>, <code>clearData</code>,
 * <code>items</code>, <code>files</code>, <code>setDragImage</code>) with the rules of its modes (read/write, read-only, protected, disabled),
 * <code>DataTransferItemList</code>, <code>DataTransferItem</code> and <code>DragEvent</code> (the constructor with the init dictionary, the
 * inheritance from <code>MouseEvent</code>, <code>draggable</code> and the <code>on...</code> handlers of the drag events). The modes of a
 * data transfer that the component hands to its drag events are set from Java here, because only the component makes such events.
 * <p>
 * 끌어다 놓기와 클립보드 이벤트에서 스크립트가 쓰는 인터페이스의 시험(계획 P20-06): <code>DataTransfer</code>(<code>effectAllowed</code>, <code>dropEffect</code>,
 * <code>types</code>, <code>getData</code>, <code>setData</code>, <code>clearData</code>, <code>items</code>, <code>files</code>, <code>setDragImage</code>)와 모드(읽기/쓰기, 읽기
 * 전용, 보호, 무효화)의 규칙, <code>DataTransferItemList</code>, <code>DataTransferItem</code>, <code>DragEvent</code>(초기화 사전을 받는 생성자, <code>MouseEvent</code>
 * 상속, <code>draggable</code>, 끌기 이벤트의 <code>on...</code> 핸들러). 컴포넌트가 끌기 이벤트에 주는 데이터 전달 객체의 모드는 컴포넌트만 그런 이벤트를 만들기
 * 때문에 여기서는 Java 에서 정한다.
 */
public class DataTransferApiTest {

    /**
     * The scripts of the page of the case.
     * <p>
     * 사례 페이지의 스크립트.
     */
    private PageScripts scripts;

    /**
     * The document of the page.
     * <p>
     * 페이지의 문서.
     */
    private Document doc;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DataTransferApiTest() {
    }

    /**
     * Loads a page with a few elements before each case.
     * <p>
     * 사례마다 요소 몇 개가 있는 페이지를 로드한다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setUrl("http://example.test/page.html");
        scripts = new PageScripts(doc, new HostEnvironment() {
            /**
             * The manual scheduler.
             * <p>
             * 수동 스케줄러.
             */
            private final PageFixture.ManualScheduler scheduler = new PageFixture.ManualScheduler();

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
        });
        HtmlParser parser = new HtmlParser(doc, "<!doctype html><body><p id=p>text</p><img id=img src=a.png><a id=a href=x>link</a>"
                + "<span id=s draggable=true>span</span><span id=n draggable=false>no</span>");
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
     * A script can make a data transfer: both effects start as <code>none</code>; <code>effectAllowed</code> and <code>dropEffect</code> accept
     * their own values only (anything else is ignored); the old names <code>text</code> and <code>url</code> are mapped; the formats are
     * lower-cased; <code>types</code> lists them in the order of addition; <code>getData</code> of an unknown format is empty; <code>clearData</code>
     * removes one format or all.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConstructedDataTransfer() throws Exception {
        Check.equal("none:none:", run("var d = new DataTransfer(); [d.effectAllowed, d.dropEffect, d.types.join()].join(':')"), "the start state");
        Check.equal("copyMove|move|none", run("var d = new DataTransfer(); d.effectAllowed = 'copyMove'; d.dropEffect = 'move'; d.effectAllowed = 'bogus';"
                + " var a = d.effectAllowed; var b = d.dropEffect; d.dropEffect = 'all'; var c = d.dropEffect; d.dropEffect = 'none'; [a, b, d.dropEffect].join('|')"),
                "only the allowed values are accepted (dropEffect 'all' is not one)");
        Check.equal("text/plain,text/uri-list,x/y|a|A|http://a/|", run("var d = new DataTransfer(); d.setData('text', 'a'); d.setData('URL', 'http://a/\\r\\nhttp://b/');"
                + " d.setData('X/Y', 'v'); var t = d.types.join(); d.setData('X/Y', 'A'); [t, d.getData('text/plain'), d.getData('x/y'), d.getData('url'),"
                + " d.getData('nothing')].join('|')"), "legacy names, lower case, the first address of url, replacing and unknown formats");
        Check.equal("text/plain,text/uri-list|0", run("var d = new DataTransfer(); d.setData('text/plain', 'a'); d.setData('text/uri-list', 'u'); d.setData('x', 'y');"
                + " d.clearData('x'); var one = d.types.join(); d.clearData(); [one, d.types.length].join('|')"), "clearData with and without a format");
    }

    /**
     * <code>items</code> is the same list every time and reflects the data: <code>length</code>, indexed access with <code>kind</code> and
     * <code>type</code>, <code>add</code>, <code>remove</code>, <code>clear</code>, <code>getAsString</code> (the callback runs after the script), and a
     * second <code>add</code> for a type that exists throws <code>NotSupportedError</code>; <code>files</code> is an empty <code>FileList</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testItems() throws Exception {
        Check.equal("true:2:string:text/plain:x/y", run("var d = new DataTransfer(); var same = d.items === d.items; d.items.add('hello', 'TEXT/Plain');"
                + " d.setData('x/y', 'v'); [same, d.items.length, d.items[0].kind, d.items[0].type, d.items[1].type].join(':')"), "the list follows the data");
        Check.equal("NotSupportedError:string:1", run("var d = new DataTransfer(); d.items.add('a', 'text/plain'); var name = '';"
                + " try { d.items.add('b', 'TEXT/PLAIN'); } catch (e) { name = e.name; } [name, d.items[0].kind, d.items.length].join(':')"),
                "a type that exists cannot be added again");
        run("var d = new DataTransfer(); d.items.add('hello', 'text/plain'); window.__out = []; var f = d.items[0].getAsFile();"
                + " d.items[0].getAsString(function (s) { window.__out.push(s); }); window.__out.push('before:' + f);");
        Check.equal("before:null,hello", run("window.__out.join()"), "getAsString calls the callback after the current script; getAsFile of text is null");
        Check.equal("1:0:0:true:0", run("var d = new DataTransfer(); d.items.add('a', 'text/plain'); d.items.add('b', 'x/y'); var it = d.items[0]; d.items.remove(0);"
                + " var a = d.items.length; var detached = it.type === ''; d.items.remove(5); d.items.clear(); [a, d.items.length, d.types.length, detached, d.files.length].join(':')"),
                "remove and clear; a removed item reads as disabled; files is empty");
        Check.equal("FileList:0", run("var d = new DataTransfer(); [d.files instanceof FileList ? 'FileList' : 'other', d.files.length].join(':')"), "files is an empty FileList");
    }

    /**
     * <code>setDragImage</code> accepts an element with the hot spot and needs an element; <code>draggable</code> is true by the attribute and by
     * default for images and links, and the <code>on...</code> handler properties of the drag events exist.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragImageAndDraggable() throws Exception {
        Check.equal("true:false:false:true:true", run("var d = new DataTransfer(); d.setDragImage(document.getElementById('p'), 3, 4);"
                + " [document.getElementById('s').draggable, document.getElementById('n').draggable, document.getElementById('p').draggable,"
                + " document.getElementById('img').draggable, document.getElementById('a').draggable].join(':')"), "draggable by the attribute and by default");
        Check.equal("object:object:object:object:object:object:object", run("var p = document.getElementById('p'); ['ondragstart', 'ondrag', 'ondragenter', 'ondragover',"
                + " 'ondragleave', 'ondrop', 'ondragend'].map(function (n) { return typeof p[n]; }).join(':')"),
                "the handler properties of the drag events are null (an object type) until they are set");
        Check.equal("TypeError", run("(function () { try { new DataTransfer().setDragImage('x', 1, 2); } catch (e) { return e.name; } })()"),
                "the first argument must be an element");
    }

    /**
     * <code>DragEvent</code> is a <code>MouseEvent</code> with <code>dataTransfer</code>; the constructor takes the coordinates, the buttons and the
     * <code>dataTransfer</code> of its init dictionary, and the event is delivered like any other event.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragEvent() throws Exception {
        Check.equal("true:true:true:7:9:true:drop:true", run("var d = new DataTransfer(); var e = new DragEvent('drop', { bubbles: true, cancelable: true, clientX: 7, clientY: 9, dataTransfer: d });"
                + " [e instanceof DragEvent, e instanceof MouseEvent, e instanceof Event, e.clientX, e.clientY, e.dataTransfer === d, e.type, e.bubbles].join(':')"),
                "the interface and the members");
        Check.equal("null:true", run("var e = new DragEvent('dragend'); [String(e.dataTransfer), e instanceof UIEvent].join(':')"), "without an init dictionary there is no data transfer");
        Check.equal("drop:copy:abc", run("var log = []; var p = document.getElementById('p'); p.addEventListener('drop', function (e) { log.push(e.type, e.dataTransfer.dropEffect, e.dataTransfer.getData('text')); });"
                + " var d = new DataTransfer(); d.setData('text', 'abc'); d.dropEffect = 'copy'; p.dispatchEvent(new DragEvent('drop', { dataTransfer: d })); log.join(':')"),
                "a script can dispatch a drag event, and the listener sees the data transfer");
        Check.equal("ondrop", run("var p = document.getElementById('n'); var got = ''; p.ondrop = function (e) { got = 'ondrop'; }; p.dispatchEvent(new DragEvent('drop')); got"),
                "the on... handler of a drag event runs");
    }

    /**
     * The rules of the modes, checked from Java where the component sets them for the events: in the read/write mode (<code>dragstart</code>)
     * data can be set and <code>effectAllowed</code> can be changed; in the read-only mode (<code>drop</code>) data can be read but nothing can be
     * changed; in the protected mode (the other events) the types can be listed but the data cannot be read; in the disabled mode (after the
     * event) nothing is visible; <code>duplicate</code> makes an independent copy.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testModes() throws Exception {
        ClipboardData d = new ClipboardData();
        d.setData("text/plain", "one");
        d.setEffectAllowed("copyMove");
        Check.equal("copyMove", d.getEffectAllowed(), "read/write: the effect can be set");
        ClipboardData copy = d.duplicate();
        d.setMode(ClipboardData.READ_ONLY);
        d.setData("x/y", "two");
        d.setEffectAllowed("all");
        d.clearData(null);
        Check.equal("one", d.getData("text/plain"), "read-only: the data is readable");
        Check.equal("copyMove", d.getEffectAllowed(), "read-only: nothing can be set or cleared");
        Check.equal(1, d.getTypes().size(), "read-only: no type was added");
        Check.isNull(d.getItems().add("z", "z/z"), "read-only: items.add gives null");
        try {
            d.getItems().remove(0);
            Check.fail("items.remove outside the read/write mode must throw");
        } catch (DomException e) {
            Check.equal("InvalidStateError", e.getName(), "items.remove outside the read/write mode is an InvalidStateError");
        }
        d.setMode(ClipboardData.PROTECTED);
        Check.equal("", d.getData("text/plain"), "protected: the data is not readable");
        Check.equal("one", d.peek("text/plain"), "protected: the component can still read it");
        Check.equal("text/plain", d.getTypes().get(0), "protected: the types are listed");
        Check.equal(1, d.getItems().getLength(), "protected: the items are listed");
        DataTransferItem item = d.getItems().item(0);
        Check.equal("string", item.getKind(), "protected: the kind is visible");
        Check.isNull(item.getAsString(), "protected: getAsString gives nothing");
        d.disable();
        Check.equal(0, d.getTypes().size(), "disabled: no types");
        Check.equal(0, d.getItems().getLength(), "disabled: no items");
        Check.equal("", item.getKind(), "disabled: an item has an empty kind");
        Check.equal("one", copy.getData("text/plain"), "the copy made before is independent and in the read/write mode");
        Check.equal("copyMove", copy.getEffectAllowed(), "and has the effects");
    }

    /**
     * Makes a drag event with the DOM classes only (no scripts): the type, the data transfer and the inheritance of the mouse members.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragEventClass() throws Exception {
        DragEvent e = new DragEvent("dragstart", true, true);
        ClipboardData d = new ClipboardData();
        e.setDataTransfer(d);
        e.setCoordinates(1, 2, 3, 4, 5, 6, 7, 8);
        Check.same(d, e.getDataTransfer(), "the data transfer");
        Check.equal(3, (long) e.getClientX(), "the mouse members are inherited");
        Element p = doc.getElementById("p");
        final boolean[] seen = new boolean[1];
        p.addEventListener("dragstart", new org.duckdns.hjow.swingwebpanel.dom.EventListener() {
            /** Records the event. / 이벤트를 기록한다. */
            public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                seen[0] = event instanceof DragEvent && ((DragEvent) event).getDataTransfer() != null;
            }
        }, false);
        Check.isTrue(p.dispatchTrustedEvent(e), "the event was not cancelled");
        Check.isTrue(seen[0], "the listener got the drag event with its data transfer");
    }
}
