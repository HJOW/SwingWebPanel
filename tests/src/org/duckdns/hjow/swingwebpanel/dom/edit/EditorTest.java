package org.duckdns.hjow.swingwebpanel.dom.edit;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.InputEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Selection;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the editing engine without layout (plan item P20-06): typing and the white space of typed text, deleting by character, word and
 * across blocks, line and paragraph breaks, formats, undo and redo, the <code>beforeinput</code>/<code>input</code> events with their
 * <code>inputType</code>, cancelling, pasting sanitized HTML, the <code>execCommand</code> table, and what is editable (nested
 * <code>contenteditable="false"</code>, design mode).
 * <p>
 * 레이아웃 없이 하는 편집 엔진의 시험(계획 P20-06): 글자 입력과 입력한 텍스트의 공백, 글자·낱말·블록 단위 지우기, 줄과 문단 나누기, 서식, 되돌리기와 다시 하기,
 * <code>inputType</code> 이 있는 <code>beforeinput</code>/<code>input</code> 이벤트, 취소, 정리한 HTML 붙여넣기, <code>execCommand</code> 표, 편집 가능한 곳(중첩된
 * <code>contenteditable="false"</code>, 디자인 모드).
 */
public class EditorTest {

    /**
     * The document of the case.
     * <p>
     * 사례의 문서.
     */
    private Document doc;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EditorTest() {
    }

    /**
     * Parses a page into a fresh document.
     * <p>
     * 페이지를 새 문서로 해석한다.
     *
     * @param body the markup of the body / body 의 마크업
     */
    private void load(String body) {
        doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        new HtmlParser(doc, "<!doctype html><body>" + body).run();
    }

    /**
     * Returns the editor.
     * <p>
     * 편집기를 반환한다.
     *
     * @return the editor / 편집기
     */
    private Editor ed() {
        return doc.getEditor();
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
     * Puts the caret in a node.
     * <p>
     * 캐럿을 노드 안에 둔다.
     *
     * @param n the node / 노드
     * @param o the offset / 오프셋
     */
    private void caret(Node n, int o) {
        doc.getSelection().collapse(n, o);
    }

    /**
     * Returns the inner HTML of the editing host with the no-break spaces written as <code>~</code>, so that failures are readable.
     * <p>
     * 편집 호스트의 inner HTML 을 반환한다. 실패 메시지가 읽기 쉽도록 NBSP 는 <code>~</code> 로 쓴다.
     *
     * @return the markup / 마크업
     */
    private String html() {
        return el("h").getInnerHtml().replace("&nbsp;", "~").replace('\u00A0', '~');
    }

    /**
     * Returns the position of the caret as <code>text|offset</code> for a text node, else the node name and offset.
     * <p>
     * 캐럿의 위치를 텍스트 노드이면 <code>텍스트|오프셋</code>, 아니면 노드 이름과 오프셋으로 반환한다.
     *
     * @return the description / 설명
     */
    private String where() {
        Selection s = doc.getSelection();
        Node n = s.getFocusNode();
        String name = n instanceof Text ? "'" + ((Text) n).getData().replace('\u00A0', '~') + "'" : n instanceof Element ? ((Element) n).getLocalName() : n.getNodeName();
        return name + "@" + s.getFocusOffset();
    }

    /**
     * Typing into text: the characters are inserted at the caret, the caret moves after them, a selection is replaced, and an empty host
     * gets a text node.
     * <p>
     * 텍스트에 입력: 글자가 캐럿에 들어가고 캐럿이 그 뒤로 가며, 선택은 바뀌고, 빈 호스트에는 텍스트 노드가 생긴다.
     */
    public void testTyping() {
        load("<div id=h contenteditable>abc</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 3);
        Check.isTrue(ed().insertText("d"), "typed");
        Check.equal("abcd", html(), "appended");
        Check.equal("'abcd'@4", where(), "the caret is after the typed text");
        caret(t, 1);
        ed().insertText("XY");
        Check.equal("aXYbcd", html(), "inserted in the middle");
        doc.getSelection().setBaseAndExtent(t, 1, t, 3);
        ed().insertText("-");
        Check.equal("a-bcd", html(), "a selection is replaced");
        Check.equal("'a-bcd'@2", where(), "the caret is after the replacement");
        load("<div id=h contenteditable></div>");
        caret(el("h"), 0);
        ed().insertText("hi");
        Check.equal("hi", html(), "an empty host gets a text node");
        Check.equal("'hi'@2", where(), "and the caret is in it");
    }

    /**
     * Typed spaces stay visible: a space at the end or after another space is a no-break space, and the no-break spaces turn into plain
     * ones when text follows (HTML collapses plain spaces, so the editor rebalances them).
     * <p>
     * 입력한 공백은 보인다: 끝이나 다른 공백 뒤의 공백은 NBSP 이고, 뒤에 텍스트가 오면 NBSP 가 일반 공백이 된다(HTML 은 일반 공백을 접으므로 편집기가 재조정한다).
     */
    public void testTypedSpacesStayVisible() {
        load("<div id=h contenteditable>ab</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 2);
        ed().insertText(" ");
        Check.equal("ab~", html(), "a trailing space is a no-break space");
        ed().insertText(" ");
        Check.equal("ab~~", html(), "two trailing spaces are no-break spaces");
        ed().insertText("c");
        Check.equal("ab~ c", html(), "text after the spaces: no-break space, then a plain space");
        caret(t, 0);
        ed().insertText(" ");
        Check.equal("~ab~ c", html(), "a space at the start of the line is a no-break space");
    }

    /**
     * Backspace and Delete remove one character; at the start of a block Backspace joins it to the block before, Delete at the end joins the
     * next block; a line break is removed as one unit.
     * <p>
     * Backspace 와 Delete 는 글자 하나를 지운다. 블록의 시작에서 Backspace 는 앞 블록과 합치고, 끝에서 Delete 는 다음 블록과 합친다. 줄 바꿈은 한 단위로 지운다.
     */
    public void testDeleteCharactersAndJoinBlocks() {
        load("<div id=h contenteditable>abc</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 3);
        ed().delete(true, false);
        Check.equal("ab", html(), "Backspace removes the character before the caret");
        caret(t, 0);
        ed().delete(false, false);
        Check.equal("b", html(), "Delete removes the character after the caret");
        load("<div id=h contenteditable><p>ab</p><p>cd</p></div>");
        caret(el("h").getLastChild().getFirstChild(), 0);
        Check.isTrue(ed().delete(true, false), "Backspace at the start of a block");
        Check.equal("<p>abcd</p>", html(), "the blocks are joined");
        Check.equal("'ab'@2", where(), "the caret is where the two met");
        load("<div id=h contenteditable><p>ab</p><p>cd</p></div>");
        caret(el("h").getFirstChild().getFirstChild(), 2);
        ed().delete(false, false);
        Check.equal("<p>abcd</p>", html(), "Delete at the end of a block joins the next block");
        load("<div id=h contenteditable>ab<br>cd</div>");
        caret(el("h").getLastChild(), 0);
        ed().delete(true, false);
        Check.equal("abcd", html(), "Backspace after a line break removes the break");
        load("<div id=h contenteditable>x</div>");
        caret(el("h").getFirstChild(), 0);
        Check.isFalse(ed().delete(true, false), "nothing before the start of the host: no event, no change");
        Check.equal("x", html(), "unchanged");
    }

    /**
     * Ctrl+Backspace and Ctrl+Delete remove a word with the spaces around it, and a character unit (a surrogate pair, a letter with a
     * combining mark) is removed whole.
     * <p>
     * Ctrl+Backspace 와 Ctrl+Delete 는 낱말과 그 둘레의 공백을 지우고, 글자 단위(서로게이트 쌍, 결합 문자가 붙은 글자)는 통째로 지운다.
     */
    public void testDeleteWordsAndCharacterUnits() {
        load("<div id=h contenteditable>one two three</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 7);
        ed().delete(true, true);
        Check.equal("one  three", html().replace("~", " "), "the word before the caret goes (the plain space stays)");
        load("<div id=h contenteditable>one two three</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 4);
        ed().delete(false, true);
        Check.equal("one three", html().replace("~", " "), "the word after the caret and its space go");
        String pair = new String(Character.toChars(0x1F600));
        load("<div id=h contenteditable>a" + pair + "b</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 3);
        ed().delete(true, false);
        Check.equal("ab", html(), "a surrogate pair is removed whole");
        load("<div id=h contenteditable>aéz</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 3);
        ed().delete(true, false);
        Check.equal("az", html(), "a letter with its combining mark is removed whole");
    }

    /**
     * Deleting a selection that spans blocks leaves one block with the start of the first and the end of the last, and removes the empty
     * inline elements that it leaves.
     * <p>
     * 블록에 걸친 선택을 지우면 첫 블록의 앞부분과 마지막 블록의 뒷부분을 가진 블록 하나가 남고, 남는 빈 인라인 요소는 제거된다.
     */
    public void testDeleteSelectionAcrossBlocks() {
        load("<div id=h contenteditable><p>ab<b>cd</b></p><p>mid</p><p>efgh</p></div>");
        Node a = el("h").getFirstChild().getFirstChild();
        Node c = el("h").getLastChild().getFirstChild();
        doc.getSelection().setBaseAndExtent(a, 1, c, 2);
        Check.isTrue(ed().delete(true, false), "deleted");
        Check.equal("<p>agh</p>", html(), "one block with the start of the first and the end of the last");
        Check.equal("'a'@1", where(), "the caret is at the place of the deletion");
        load("<div id=h contenteditable>x<b>yz</b>w</div>");
        Node b = el("h").getFirstChild().getNextSibling().getFirstChild();
        doc.getSelection().setBaseAndExtent(b, 0, b, 2);
        ed().delete(true, false);
        Check.equal("xw", html(), "the emptied bold element is removed");
    }

    /**
     * A line break inserts a <code>br</code>, and a second one at the end of the block so that the new line shows.
     * <p>
     * 줄 바꿈은 <code>br</code> 을 넣고, 블록의 끝에서는 새 줄이 보이도록 하나 더 넣는다.
     */
    public void testLineBreak() {
        load("<div id=h contenteditable>abcd</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 2);
        Check.isTrue(ed().insertLineBreak(), "inserted");
        Check.equal("ab<br>cd", html(), "a break in the middle");
        load("<div id=h contenteditable>abcd</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 4);
        ed().insertLineBreak();
        Check.equal("abcd<br><br>", html(), "at the end of the block a second br shows the new line");
        Check.equal("div@2", where(), "the caret is between the two breaks");
    }

    /**
     * A paragraph break splits the block into two blocks of the same kind, a heading is followed by a plain paragraph, text directly in the
     * host goes into a <code>div</code>, and the new block gets a placeholder when it is empty.
     * <p>
     * 문단 나누기는 블록을 같은 종류의 두 블록으로 쪼개고, 제목 뒤에는 일반 단락이 오며, 호스트 바로 아래의 텍스트는 <code>div</code> 에 들어가고, 새 블록이 비면
     * 자리표시를 가진다.
     */
    public void testParagraphBreak() {
        load("<div id=h contenteditable><p>abcd</p></div>");
        Text t = (Text) el("h").getFirstChild().getFirstChild();
        caret(t, 2);
        Check.isTrue(ed().insertParagraph(), "done");
        Check.equal("<p>ab</p><p>cd</p>", html(), "the block is split");
        Check.equal("'cd'@0", where(), "the caret is at the start of the new block");
        load("<div id=h contenteditable><p>abcd</p></div>");
        t = (Text) el("h").getFirstChild().getFirstChild();
        caret(t, 4);
        ed().insertParagraph();
        Check.equal("<p>abcd</p><p><br></p>", html(), "at the end the new block has a placeholder");
        load("<div id=h contenteditable><h2>Title</h2></div>");
        t = (Text) el("h").getFirstChild().getFirstChild();
        caret(t, 5);
        ed().insertParagraph();
        Check.equal("<h2>Title</h2><div><br></div>", html(), "after the end of a heading comes a plain paragraph");
        load("<div id=h contenteditable>abcd</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 2);
        ed().insertParagraph();
        Check.equal("ab<div>cd</div>", html(), "text directly in the host: the rest goes into a div");
        load("<div id=h contenteditable><p><b>bold|text</b></p></div>".replace("|", ""));
        t = (Text) el("h").getFirstChild().getFirstChild().getFirstChild();
        caret(t, 4);
        ed().insertParagraph();
        Check.equal("<p><b>bold</b></p><p><b>text</b></p>", html(), "the inline elements are split with the block");
    }

    /**
     * Enter in a list item makes a new item, and in an empty item it ends the list.
     * <p>
     * 목록 항목에서 Enter 는 새 항목을 만들고 빈 항목에서는 목록을 끝낸다.
     */
    public void testListItems() {
        load("<div id=h contenteditable><ul><li>one</li><li>two</li></ul></div>");
        Text t = (Text) el("h").getFirstChild().getFirstChild().getFirstChild();
        caret(t, 3);
        ed().insertParagraph();
        Check.equal("<ul><li>one</li><li><br></li><li>two</li></ul>", html(), "a new item after the first");
        ed().insertParagraph();
        Check.equal("<ul><li>one</li></ul><div><br></div><ul><li>two</li></ul>", html(), "in the empty item the list ends and the rest forms a new list");
    }

    /**
     * Formats: a selection gets the element, the same command again takes it off, a part inside a format splits it, adjacent elements
     * merge, a caret remembers the format for the next typed text, and the state follows.
     * <p>
     * 서식: 선택에 요소가 생기고, 같은 명령을 다시 하면 없어지며, 서식 안의 일부는 서식을 쪼개고, 이웃한 요소는 합쳐지고, 캐럿은 다음 입력에 쓸 서식을 기억하며, 상태가 따라온다.
     */
    public void testFormats() {
        load("<div id=h contenteditable>abcdef</div>");
        Text t = (Text) el("h").getFirstChild();
        doc.getSelection().setBaseAndExtent(t, 1, t, 4);
        Check.isTrue(ed().execCommand("bold", null), "bold");
        Check.equal("a<b>bcd</b>ef", html(), "the selected text is in a bold element");
        Check.isTrue(ed().queryCommandState("bold"), "the state is on");
        Check.equal("bcd", doc.getSelection().toString(), "the selection still covers the text");
        Node bold = el("h").getFirstChild().getNextSibling().getFirstChild();
        doc.getSelection().setBaseAndExtent(bold, 1, bold, 2);
        ed().execCommand("bold", null);
        Check.equal("a<b>b</b>c<b>d</b>ef", html(), "a part inside the format is taken out and splits it");
        doc.getSelection().setBaseAndExtent(el("h").getFirstChild(), 0, el("h").getLastChild(), 2);
        Check.isFalse(ed().queryCommandState("bold"), "the whole selection is not bold");
        ed().execCommand("bold", null);
        Check.equal("<b>abcdef</b>", html(), "bold on mixed text makes all of it bold and merges the neighbours");
        ed().execCommand("italic", null);
        Check.equal("<b><i>abcdef</i></b>", html(), "formats nest");
        load("<div id=h contenteditable>ab</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 2);
        ed().execCommand("underline", null);
        Check.equal("ab", html(), "a collapsed caret changes nothing in the DOM");
        ed().insertText("c");
        Check.equal("ab<u>c</u>", html(), "the next typed text gets the format");
        Check.isTrue(ed().queryCommandState("underline"), "and the caret is in it");
    }

    /**
     * Undo and redo restore the DOM and the selection exactly, typing is undone by the run, and a new edit empties the redo list.
     * <p>
     * 되돌리기와 다시 하기는 DOM 과 선택을 정확히 되살리고, 입력은 연속 단위로 되돌려지며, 새 편집은 다시 하기 목록을 비운다.
     */
    public void testUndoRedo() {
        load("<div id=h contenteditable>abc</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 3);
        ed().insertText("d");
        ed().insertText("e");
        Check.equal("abcde", html(), "typed");
        Check.isTrue(ed().undo(), "undo");
        Check.equal("abc", html(), "the typed run is undone as one step");
        Check.equal("'abc'@3", where(), "with the selection from before");
        Check.isTrue(ed().redo(), "redo");
        Check.equal("abcde", html(), "redone");
        Check.equal("'abcde'@5", where(), "with the selection from after");
        ed().undo();
        ed().insertText("X");
        Check.isFalse(ed().redo(), "a new edit empties the redo list");
        load("<div id=h contenteditable><p>ab</p><p>cd</p></div>");
        caret(el("h").getLastChild().getFirstChild(), 0);
        ed().delete(true, false);
        Check.equal("<p>abcd</p>", html(), "joined");
        ed().undo();
        Check.equal("<p>ab</p><p>cd</p>", html(), "undo of a join restores both blocks");
        Check.same(el("h").getLastChild().getFirstChild(), doc.getSelection().getFocusNode(), "and the same nodes, with the caret in the second one");
        ed().redo();
        Check.equal("<p>abcd</p>", html(), "redo of the join");
        el("h").normalize();
        doc.getSelection().setBaseAndExtent(el("h").getFirstChild().getFirstChild(), 1, el("h").getFirstChild().getFirstChild(), 3);
        ed().execCommand("bold", null);
        Check.equal("<p>a<b>bc</b>d</p>", html(), "bold");
        ed().undo();
        Check.equal("<p>abcd</p>", html(), "bold undone");
    }

    /**
     * The events: <code>beforeinput</code> is cancelable with the <code>inputType</code>, the data and the target range; cancelling leaves
     * the DOM alone and sends no <code>input</code>; <code>input</code> follows a change with the same type.
     * <p>
     * 이벤트: <code>beforeinput</code> 은 취소할 수 있고 <code>inputType</code>, 데이터, 대상 범위를 가지며, 취소하면 DOM 이 그대로이고 <code>input</code> 도 없으며,
     * <code>input</code> 은 같은 종류로 변경 뒤에 온다.
     */
    public void testInputEvents() {
        load("<div id=h contenteditable>abc</div>");
        final List<String> log = new ArrayList<String>();
        final boolean[] cancel = new boolean[1];
        el("h").addEventListener("beforeinput", new EventListener() {
            /**
             * Records the event, and cancels it when asked.
             * <p>
             * 이벤트를 기록하고, 요청이 있으면 취소한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                InputEvent ie = (InputEvent) e;
                String ranges = ie.getTargetRanges().isEmpty() ? "none" : ie.getTargetRanges().get(0).getStartOffset() + "-" + ie.getTargetRanges().get(0).getEndOffset();
                log.add("before:" + ie.getInputType() + ":" + ie.getData() + ":" + ranges + ":" + ie.isCancelable());
                if (cancel[0]) {
                    e.preventDefault();
                }
            }
        });
        el("h").addEventListener("input", new EventListener() {
            /**
             * Records the event.
             * <p>
             * 이벤트를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void handleEvent(Event e) {
                InputEvent ie = (InputEvent) e;
                log.add("input:" + ie.getInputType() + ":" + ie.getData() + ":" + el("h").getInnerHtml());
            }
        });
        Text t = (Text) el("h").getFirstChild();
        doc.getSelection().setBaseAndExtent(t, 1, t, 2);
        ed().insertText("X");
        Check.equal("[before:insertText:X:1-2:true, input:insertText:X:aXc]", log.toString(), "the events with the type, the data and the target range");
        cancel[0] = true;
        log.clear();
        Check.isFalse(ed().insertText("Y"), "cancelled");
        Check.equal("aXc", html(), "the DOM is unchanged");
        Check.equal(1, log.size(), "and no input event follows");
        cancel[0] = false;
        log.clear();
        caret(el("h").getFirstChild(), 1);
        ed().delete(true, false);
        Check.contains(log.get(0), "before:deleteContentBackward:null:0-1:true", "a deletion reports the range it will remove");
        log.clear();
        ed().undo();
        Check.equal("before:historyUndo:null:none:true", log.get(0), "undo sends historyUndo with no range");
        Check.contains(log.get(1), "input:historyUndo", "and input");
    }

    /**
     * Pasting HTML inserts it without scripts, event handlers, styles and unsafe addresses; the plain text of plaintext-only hosts and
     * <code>text/plain</code>-only data is inserted as text.
     * <p>
     * HTML 붙여넣기는 스크립트, 이벤트 핸들러, 스타일, 안전하지 않은 주소 없이 넣고, plaintext-only 호스트와 <code>text/plain</code> 만 있는 데이터는 텍스트로 넣는다.
     */
    public void testPaste() {
        load("<div id=h contenteditable>ab</div>");
        Text t = (Text) el("h").getFirstChild();
        caret(t, 1);
        ClipboardData data = new ClipboardData();
        data.put("text/plain", "ignored");
        data.put("text/html", "<b onclick='x()'>B</b><script>evil()</script><a href='javascript:evil()'>l</a><!-- c --><i>i</i>");
        Check.isTrue(ed().insertFromPaste(data), "pasted");
        Check.equal("a<b>B</b><a>l</a><i>i</i>b", html(), "the markup without script, handler, unsafe address and comment");
        load("<div id=h contenteditable=plaintext-only>ab</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 1);
        ed().insertFromPaste(data);
        Check.equal("aignoredb", html(), "a plaintext-only host takes the plain text");
        load("<div id=h contenteditable>ab</div>");
        t = (Text) el("h").getFirstChild();
        caret(t, 2);
        ClipboardData plain = new ClipboardData();
        plain.put("text/plain", "x\ny");
        ed().insertFromPaste(plain);
        Check.equal("abx<br>y", html(), "plain text with a line feed gets a line break");
    }

    /**
     * What is editable: only inside an editing host, not inside a nested <code>contenteditable="false"</code>; typing in a non-editable
     * place does nothing; design mode makes the whole document the host.
     * <p>
     * 편집 가능한 곳: 편집 호스트 안이며 중첩된 <code>contenteditable="false"</code> 안은 아니다. 편집할 수 없는 곳의 입력은 아무 일도 없다. 디자인 모드는 문서
     * 전체를 호스트로 만든다.
     */
    public void testEditableRegions() {
        load("<div id=h contenteditable>ab<span id=lock contenteditable=false>cd</span><span id=n>ef</span></div><p id=out>xy</p>");
        Check.same(el("h"), EditingHosts.hostOf(el("n")), "an element inside a host");
        Check.isNull(EditingHosts.hostOf(el("lock")), "the false element is not editable");
        Check.isNull(EditingHosts.hostOf(el("lock").getFirstChild()), "nor its text");
        Check.isNull(EditingHosts.hostOf(el("out")), "text outside a host is not editable");
        caret(el("out").getFirstChild(), 1);
        Check.isFalse(ed().insertText("Z"), "typing outside does nothing");
        Check.equal("xy", el("out").getInnerHtml(), "the text is unchanged");
        Check.isFalse(ed().execCommand("bold", null), "and commands are not enabled");
        doc.getSelection().setBaseAndExtent(el("h").getFirstChild(), 1, el("out").getFirstChild(), 1);
        Check.isNull(ed().getSelectionHost(), "a selection that leaves the host is not edited");
        Check.isFalse(EditingHosts.isEditingHost(el("out")), "not a host");
        doc.setDesignMode(true);
        Check.isTrue(EditingHosts.isEditable(el("out").getFirstChild()), "design mode makes the whole document editable");
        caret(el("out").getFirstChild(), 1);
        Check.isTrue(ed().insertText("Z"), "typing works");
        Check.equal("xZy", el("out").getInnerHtml(), "in the paragraph");
        Check.isNull(EditingHosts.hostOf(el("lock")), "contenteditable=false still protects its content in design mode");
        doc.setDesignMode(false);
        Check.isFalse(EditingHosts.isEditable(el("out")), "design mode off");
    }

    /**
     * <code>execCommand</code>: the supported names, the values, HTML insertion, links, block formats and lists.
     * <p>
     * <code>execCommand</code>: 지원하는 이름, 값, HTML 삽입, 링크, 블록 서식, 목록.
     */
    public void testExecCommand() {
        load("<div id=h contenteditable><p>abcd</p></div>");
        Text t = (Text) el("h").getFirstChild().getFirstChild();
        caret(t, 2);
        Check.isTrue(ed().isCommandSupported("BOLD"), "names are case insensitive");
        Check.isFalse(ed().isCommandSupported("foreColor"), "unsupported commands are reported");
        Check.isTrue(ed().execCommand("insertHTML", "<i>x</i>"), "insertHTML");
        Check.equal("<p>ab<i>x</i>cd</p>", html(), "the markup is inserted at the caret");
        doc.getSelection().setBaseAndExtent(el("h").getFirstChild().getFirstChild(), 0, el("h").getFirstChild().getFirstChild(), 2);
        Check.isTrue(ed().execCommand("createLink", "http://example.com/"), "createLink");
        Check.equal("<p><a href=\"http://example.com/\">ab</a><i>x</i>cd</p>", html(), "the selected text is in a link");
        Check.isTrue(ed().execCommand("unlink", null), "unlink");
        Check.equal("<p>ab<i>x</i>cd</p>", html(), "and out of it");
        caret(el("h").getFirstChild().getFirstChild(), 1);
        Check.isTrue(ed().execCommand("formatBlock", "<h1>"), "formatBlock");
        Check.equal("<h1>ab<i>x</i>cd</h1>", html(), "the block is renamed");
        Check.equal("h1", ed().queryCommandValue("formatBlock"), "and the value is the block");
        Check.isTrue(ed().execCommand("insertUnorderedList", null), "list");
        Check.equal("<ul><li>ab<i>x</i>cd</li></ul>", html(), "a list item");
        Check.isTrue(ed().queryCommandState("insertUnorderedList"), "the state of the list");
        Check.isTrue(ed().execCommand("insertOrderedList", null), "the other list kind replaces the list");
        Check.equal("<ol><li>ab<i>x</i>cd</li></ol>", html(), "switched to an ordered list");
        Check.isTrue(ed().execCommand("selectAll", null), "selectAll");
        Check.equal("abxcd", doc.getSelection().toString().replace("\n", ""), "selects the content of the host");
    }
}
