package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitHtml;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitSel;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.drain;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.log;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.stored;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.tap;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Clip;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of cut and paste in an editing host with real AWT key events (plan item P20-06): Ctrl+V and Shift+Insert paste the clipboard
 * (text, or sanitized markup) after the cancelable <code>paste</code> event, Ctrl+X and Shift+Delete cut the selection after the
 * <code>cut</code> event (the clipboard gets the text, the selection is removed with <code>deleteByCut</code>), a cancelled event stops the
 * default action, a place that cannot be edited only gets the event, and a clipboard that cannot be read pastes nothing. The clipboard of
 * the test component is a variable, which stands for the overridable <code>readClipboard()</code> of the component; the real system
 * clipboard is not used.
 * <p>
 * 편집 호스트의 잘라내기와 붙여넣기를 실제 AWT 키 이벤트로 하는 시험(계획 P20-06): Ctrl+V 와 Shift+Insert 는 취소할 수 있는 <code>paste</code> 이벤트 뒤에
 * 클립보드(텍스트 또는 정리한 마크업)를 붙여넣고, Ctrl+X 와 Shift+Delete 는 <code>cut</code> 이벤트 뒤에 선택을 잘라낸다(클립보드는 텍스트를 받고 선택은
 * <code>deleteByCut</code> 으로 지워진다). 취소한 이벤트는 기본 동작을 막고, 편집할 수 없는 곳은 이벤트만 받으며, 읽을 수 없는 클립보드는 아무것도 붙여넣지 않는다.
 * 시험 컴포넌트의 클립보드는 변수이며 컴포넌트의 재정의할 수 있는 <code>readClipboard()</code> 를 대신한다. 실제 시스템 클립보드는 쓰지 않는다.
 */
public class EditingClipboardTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EditingClipboardTest() {
    }

    /**
     * Clicks into the host at a character offset of its first text and waits for the caret.
     * <p>
     * 호스트의 첫 텍스트의 글자 오프셋 위치를 클릭하고 캐럿을 기다린다.
     *
     * @param panel  the component / 컴포넌트
     * @param rects  the rectangles / 사각형
     * @param offset the character offset / 글자 오프셋
     * @throws Exception on failure / 실패 시
     */
    private static void clickAt(Panel panel, Rects rects, int offset) throws Exception {
        click(panel, rects.left(0) + offset * rects.advance() + 1, rects.middle(0), 1, false);
        awaitSel(panel, offset + "," + offset);
    }

    /**
     * Selects the first five characters (<code>hello</code>) with Shift and the arrow keys.
     * <p>
     * Shift 와 화살표 키로 처음 다섯 글자(<code>hello</code>)를 선택한다.
     *
     * @param panel the component / 컴포넌트
     * @param rects the rectangles / 사각형
     * @throws Exception on failure / 실패 시
     */
    private static void selectHello(Panel panel, Rects rects) throws Exception {
        clickAt(panel, rects, 0);
        // Shift+오른쪽을 다섯 번 눌러 "hello" 를 선택한다.
        for (int i = 0; i < 5; i++) {
            tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
        }
        awaitSel(panel, "0,5");
    }

    /**
     * Ctrl+V pastes the text of the clipboard at the caret after the <code>paste</code> event that carries it, Shift+Insert does the same,
     * and the text replaces a selection.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteInsertsTheClipboardText() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            panel.clipboard = new Clip("XY", null);
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "helloXY world");
            awaitSel(panel, "7,7");
            tap(panel, KeyEvent.VK_INSERT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            awaitHtml(panel, "helloXYXY world");
            selectHello(panel, rects);
            tap(panel, KeyEvent.VK_V, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "XYXYXY world");
            drain(panel);
            Check.contains(log(panel), "keydown:v paste:XY beforeinput:insertFromPaste:null input:insertFromPaste:null keyup:v",
                    "the paste event comes first and carries the text, then the input events");
        } finally {
            close(panel);
        }
    }

    /**
     * Markup on the clipboard is pasted as markup (with the harmful parts removed) and the plain text only when the markup is missing.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteMarkupIsSanitized() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            panel.clipboard = new Clip("B text", "<b>B</b> <script>alert(1)</script>text<img src=x onerror=alert(2)>");
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            String html = stored(panel, "html");
            Check.contains(html, "<b>B</b>", "the markup is pasted as markup");
            Check.isTrue(html.indexOf("<script") < 0 && html.indexOf("onerror") < 0, "scripts and event handler attributes are removed: " + html);
        } finally {
            close(panel);
        }
    }

    /**
     * A script that cancels <code>paste</code> stops the default action; a clipboard that cannot be read pastes nothing and says so in
     * the status text; an empty clipboard pastes nothing.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledAndBrokenPaste() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("ed.addEventListener('paste', function (e) { if (e.clipboardData.getData('text/plain') === 'no') { e.preventDefault(); } });"));
            clickAt(panel, rects, 5);
            panel.clipboard = new Clip("no", null);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            Check.equal("hello world", stored(panel, "html"), "the cancelled paste changes nothing");
            Check.contains(log(panel), "paste:no keyup:v", "and sends no input events");
            panel.clipboard = null;
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            Check.equal("hello world", stored(panel, "html"), "an empty clipboard pastes nothing");
            panel.clipboardBroken = true;
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            Check.equal("hello world", stored(panel, "html"), "a clipboard that cannot be read pastes nothing");
            Check.contains(panel.getStatusText(), "Paste failed", "the status text tells it");
        } finally {
            close(panel);
        }
    }

    /**
     * The <code>clipboardData</code> of <code>paste</code> is read-only: a script that calls <code>setData</code> in its listener changes nothing, and
     * what is pasted is what the clipboard held.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteDataIsReadOnly() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("ed.addEventListener('paste', function (e) { e.clipboardData.setData('text/plain', 'HACK'); put('seen', e.clipboardData.getData('text/plain')); });"));
            panel.clipboard = new Clip("XY", null);
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "helloXY world");
            Check.equal("XY", stored(panel, "seen"), "the listener still sees the clipboard text");
        } finally {
            close(panel);
        }
    }

    /**
     * Ctrl+X and Shift+Delete put the selected text on the clipboard after the <code>cut</code> event and remove the selection with the input
     * type <code>deleteByCut</code>; a cancelled <code>cut</code> removes nothing; a script can put its own data on the clipboard.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCutRemovesTheSelectionAndFillsTheClipboard() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("ed.addEventListener('cut', function (e) { if (window.holdCut) { e.clipboardData.setData('text/plain', 'mine'); e.preventDefault(); } });"));
            selectHello(panel, rects);
            tap(panel, KeyEvent.VK_X, (char) 24, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, " world");
            drain(panel);
            Check.equal("hello", panel.texts.get(0), "the clipboard got the selected text");
            Check.contains(log(panel), "keydown:x cut:hello beforeinput:deleteByCut:null input:deleteByCut:null keyup:x", "the cut event comes first");
            // 붙여넣으면 잘라낸 것이 되돌아온다(시험의 클립보드는 변수이므로 직접 옮긴다).
            panel.clipboard = new Clip(panel.texts.get(0), null);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "hello world");
            // Shift+Delete 도 잘라내기이다.
            selectHello(panel, rects);
            tap(panel, KeyEvent.VK_DELETE, (char) 127, InputEvent.SHIFT_DOWN_MASK);
            awaitHtml(panel, " world");
            Check.equal(2, panel.texts.size(), "Shift+Delete copied again");
        } finally {
            close(panel);
        }
    }

    /**
     * A script that cancels <code>cut</code> and sets its own data stops the removal and the default clipboard text.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledCutKeepsTheContent() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("ed.addEventListener('cut', function (e) { e.clipboardData.setData('text/plain', 'mine'); e.preventDefault(); });"));
            selectHello(panel, rects);
            tap(panel, KeyEvent.VK_X, (char) 24, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            Check.equal("hello world", stored(panel, "html"), "the cancelled cut removes nothing");
            Check.equal("mine", panel.texts.get(0), "the data of the script is what the clipboard gets");
        } finally {
            close(panel);
        }
    }

    /**
     * Keys in a place that cannot be edited: the <code>paste</code> event is still sent to the focused element (the body), nothing changes.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteOutsideAnEditingHostOnlySendsTheEvent() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("document.addEventListener('paste', function () { put('pasted', 'yes'); });"));
            panel.clipboard = new Clip("zzz", null);
            click(panel, rects.left(1) + 3, rects.middle(1), 1, false);
            tap(panel, KeyEvent.VK_V, (char) 22, InputEvent.CTRL_DOWN_MASK);
            drain(panel);
            Check.equal("yes", stored(panel, "pasted"), "the document saw the paste event");
            Check.equal("hello world", stored(panel, "html"), "the host is unchanged");
            Check.equal("read only", stored(panel, "ro"), "and so is the text that cannot be edited");
        } finally {
            close(panel);
        }
    }
}
