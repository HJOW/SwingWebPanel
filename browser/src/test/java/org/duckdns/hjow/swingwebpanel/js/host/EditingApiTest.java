package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the editing interfaces that scripts use (plan item P20-06): <code>contentEditable</code>, <code>isContentEditable</code>,
 * <code>designMode</code>, <code>document.execCommand()</code> and the <code>queryCommand*</code> methods, the <code>beforeinput</code> and
 * <code>input</code> events that editing sends (with <code>inputType</code>, <code>data</code>, <code>getTargetRanges()</code>,
 * cancelling), <code>StaticRange</code>, and the construction of <code>InputEvent</code> from script.
 * <p>
 * 스크립트가 쓰는 편집 인터페이스 시험(계획 P20-06): <code>contentEditable</code>, <code>isContentEditable</code>, <code>designMode</code>,
 * <code>document.execCommand()</code> 와 <code>queryCommand*</code> 메서드, 편집이 보내는 <code>beforeinput</code>, <code>input</code> 이벤트(<code>inputType</code>,
 * <code>data</code>, <code>getTargetRanges()</code>, 취소), <code>StaticRange</code>, 스크립트의 <code>InputEvent</code> 생성.
 */
public class EditingApiTest {

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
    public EditingApiTest() {
    }

    /**
     * Loads a page with an editing host and a plain paragraph before each case.
     * <p>
     * 사례마다 편집 호스트와 일반 문단이 있는 페이지를 로드한다.
     */
    public void setUp() {
        Document doc = Document.createHtmlDocument();
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
        HtmlParser parser = new HtmlParser(doc, "<!doctype html><body><div id=ed contenteditable=true>abcdef</div><p id=plain>plain</p>"
                + "<div id=lock contenteditable=true>x<span id=ro contenteditable=false>ro</span></div>");
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
     * <code>isContentEditable</code> follows the editing hosts and the false islands; <code>contentEditable</code> reads and writes the
     * state; <code>designMode</code> switches the whole document.
     * <p>
     * <code>isContentEditable</code> 은 편집 호스트와 false 섬을 따르고, <code>contentEditable</code> 은 상태를 읽고 쓰며, <code>designMode</code> 는 문서 전체를
     * 바꾼다.
     */
    public void testEditableState() {
        Check.equal("true:true:false:false:inherit:true",
                run("[document.getElementById('ed').isContentEditable, document.getElementById('ed').firstChild.parentNode.isContentEditable,"
                        + " document.getElementById('plain').isContentEditable, document.getElementById('ro').isContentEditable,"
                        + " document.getElementById('plain').contentEditable, document.getElementById('lock').isContentEditable].join(':')"),
                "editable state of the elements");
        Check.equal("on:true:true:false", run("var p = document.getElementById('plain'); document.designMode = 'ON'; var on = document.designMode;"
                + " var ed = p.isContentEditable; var ro = document.getElementById('ro').isContentEditable; document.designMode = 'off';"
                + " [on, ed, !ro, p.isContentEditable].join(':')"), "design mode makes the document editable, and the false island stays");
        Check.equal("off", run("document.designMode = 'maybe'; document.designMode"), "another value is ignored");
        Check.equal("true:plaintext-only:false", run("var e = document.getElementById('plain'); e.contentEditable = 'TRUE'; var a = e.isContentEditable;"
                + " e.contentEditable = 'plaintext-only'; var b = e.contentEditable; e.contentEditable = 'inherit'; [a, b, e.isContentEditable].join(':')"),
                "the setter and the states");
    }

    /**
     * <code>execCommand</code> edits the selection of the document; the query methods report support, enabled state and format state.
     * <p>
     * <code>execCommand</code> 는 문서의 선택을 편집하고, 질의 메서드는 지원, 사용 가능, 서식 상태를 알려 준다.
     */
    public void testExecCommandAndQueries() {
        Check.equal("true:true:false:false", run("[document.queryCommandSupported('bold'), document.queryCommandSupported('INSERTTEXT'),"
                + " document.queryCommandSupported('nonsense'), document.queryCommandEnabled('bold')].join(':')"), "support, and not enabled without an editing selection");
        Check.equal("true:ab<b>cd</b>ef:true:true", run("var t = document.getElementById('ed').firstChild; getSelection().setBaseAndExtent(t, 2, t, 4);"
                + " var ok = document.execCommand('bold'); var h = document.getElementById('ed').innerHTML;"
                + " [ok, h, document.queryCommandState('bold'), document.queryCommandEnabled('bold')].join(':')"), "bold the selection");
        Check.equal("true:ab<b>cd</b>XYef:false", run("var b = document.getElementById('ed').firstChild.nextSibling; getSelection().collapse(b.nextSibling, 0);"
                + " var ok = document.execCommand('insertText', false, 'XY'); [ok, document.getElementById('ed').innerHTML, document.queryCommandState('bold')].join(':')"),
                "insertText with the value");
        Check.equal("true:ab<b>cd</b>ef:abcdef:true:false", run("var ed = document.getElementById('ed'); var a = document.execCommand('undo'); var h1 = ed.innerHTML;"
                + " var b = document.execCommand('undo'); var h2 = ed.innerHTML; var c = document.execCommand('redo'); var d = document.execCommand('undo');"
                + " [a, h1, 'x'].join(':').replace(':x', '') + ':' + h2 + ':' + c + ':' + document.queryCommandEnabled('undo')"), "undo twice, redo once, undo again");
        Check.equal("true:false", run("[document.execCommand('selectAll'), document.execCommand('doesNotExist')].join(':')"), "selectAll, and an unknown command");
        Check.equal("TypeError", run("(function () { try { document.execCommand(); } catch (e) { return e.name; } })()"), "needs the name");
    }

    /**
     * The events that editing sends: <code>beforeinput</code> first (cancelable, with <code>inputType</code>, <code>data</code>, the target
     * ranges as <code>StaticRange</code> objects), then the change, then <code>input</code>; cancelling stops the change.
     * <p>
     * 편집이 보내는 이벤트: 먼저 <code>beforeinput</code>(취소할 수 있고, <code>inputType</code>, <code>data</code>, <code>StaticRange</code> 객체인 대상 범위),
     * 그 다음 변경, 그 다음 <code>input</code>. 취소하면 변경이 멈춘다.
     */
    public void testInputEvents() {
        Check.equal("before:insertText:Q:true:2-4:true:abcdef|input:insertText:Q:abQef", run("var ed = document.getElementById('ed'); var log = [];"
                + " ed.addEventListener('beforeinput', function (e) { var r = e.getTargetRanges()[0];"
                + " log.push('before:' + e.inputType + ':' + e.data + ':' + e.cancelable + ':' + r.startOffset + '-' + r.endOffset + ':' + (r instanceof StaticRange)"
                + " + ':' + ed.innerHTML); });"
                + " ed.addEventListener('input', function (e) { log.push('input:' + e.inputType + ':' + e.data + ':' + ed.innerHTML); });"
                + " var t = ed.firstChild; getSelection().setBaseAndExtent(t, 2, t, 4); document.execCommand('insertText', false, 'Q'); log.join('|')"),
                "beforeinput sees the document as it was, input as it is");
        Check.equal("abQef:1", run("var ed = document.getElementById('ed'); var n = 0;"
                + " var cancel = true; ed.addEventListener('beforeinput', function (e) { n++; if (cancel) { e.preventDefault(); } });"
                + " getSelection().collapse(ed.firstChild, 0); document.execCommand('insertText', false, 'Z'); cancel = false; [ed.innerHTML, n].join(':')"),
                "a cancelled beforeinput leaves the document alone");
        Check.equal("deleteContentBackward:true", run("var ed = document.getElementById('ed'); var types = [];"
                + " ed.addEventListener('input', function (e) { types.push(e.inputType); });"
                + " getSelection().collapse(ed.firstChild, 3); var ok = document.execCommand('delete'); [types[types.length - 1], ok].join(':')"),
                "delete is deleteContentBackward");
    }

    /**
     * <code>StaticRange</code> and <code>InputEvent</code> can be made by scripts: the init dictionaries, the validation, and the target
     * ranges of events that script made (they are not computed).
     * <p>
     * <code>StaticRange</code> 와 <code>InputEvent</code> 는 스크립트가 만들 수 있다: 초기화 사전, 검사, 스크립트가 만든 이벤트의 대상 범위(계산되지 않는다).
     */
    public void testStaticRangeAndInputEventConstruction() {
        Check.equal("true:2:4:false:true", run("var t = document.getElementById('ed').firstChild; var s = new StaticRange({ startContainer: t, startOffset: 2,"
                + " endContainer: t, endOffset: 4 }); [s instanceof StaticRange, s.startOffset, s.endOffset, s.collapsed, s.startContainer === t].join(':')"),
                "a static range");
        Check.equal("TypeError", run("(function () { try { new StaticRange({ startOffset: 0 }); } catch (e) { return e.name; } })()"), "required members");
        Check.equal("InvalidNodeTypeError", run("(function () { try { new StaticRange({ startContainer: document.doctype, startOffset: 0,"
                + " endContainer: document.body, endOffset: 0 }); } catch (e) { return e.name; } })()"), "a document type is rejected");
        Check.equal("insertText:q:true:0:1", run("var t = document.getElementById('ed').firstChild; var sr = new StaticRange({ startContainer: t, startOffset: 0,"
                + " endContainer: t, endOffset: 1 }); var e = new InputEvent('beforeinput', { inputType: 'insertText', data: 'q', cancelable: true,"
                + " targetRanges: [sr] }); [e.inputType, e.data, e.cancelable, e.getTargetRanges()[0].startOffset, e.getTargetRanges()[0].endOffset].join(':')"),
                "an InputEvent made by script keeps its target ranges");
        Check.equal("0:true", run("var e = new InputEvent('input', { inputType: 'insertText' }); [e.getTargetRanges().length, e.dataTransfer === null].join(':')"),
                "an InputEvent without ranges and data transfer");
    }

    /**
     * Text inside a <code>contenteditable="false"</code> island and outside the hosts cannot be changed by editing commands.
     * <p>
     * <code>contenteditable="false"</code> 섬 안과 호스트 밖의 텍스트는 편집 명령으로 바꿀 수 없다.
     */
    public void testNotEditable() {
        Check.equal("false:plain", run("var p = document.getElementById('plain').firstChild; getSelection().collapse(p, 2);"
                + " [document.execCommand('insertText', false, 'X'), document.getElementById('plain').innerHTML].join(':')"), "outside a host");
        Check.equal("false:ro", run("var r = document.getElementById('ro').firstChild; getSelection().collapse(r, 1);"
                + " [document.execCommand('insertText', false, 'X'), document.getElementById('ro').innerHTML].join(':')"), "inside a false island");
    }
}
