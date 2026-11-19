package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.await;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitHtml;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitSel;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.drain;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.inputMethod;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.log;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.pageWith;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.paint;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.stored;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.tap;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.type;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of editing by the keyboard and the mouse with the real component and real AWT events (plan item P20-06): a click in an editing host
 * puts the caret there and gives the host the focus, typed characters, Enter, Backspace and Delete change the DOM and send the events in the
 * order of the specification, the arrow keys, Home and End (with Ctrl: by word or to the edge of the host) move the caret, Shift extends
 * the selection, Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+Z and Ctrl+Y format and undo, and what a cancelled <code>keydown</code> or
 * <code>beforeinput</code> prevents. The page reports the HTML of the host, the events and the selection through its
 * <code>sessionStorage</code>.
 * <p>
 * 실제 컴포넌트와 실제 AWT 이벤트로 하는 키보드와 마우스 편집의 시험(계획 P20-06): 편집 호스트 안의 클릭이 캐럿을 거기에 두고 호스트에 포커스를 주며, 입력한
 * 글자, Enter, Backspace, Delete 가 DOM 을 바꾸고 명세의 순서로 이벤트를 보낸다. 화살표 키, Home, End(Ctrl 이면 낱말 단위나 호스트의 가장자리)가 캐럿을 옮기고,
 * Shift 가 선택을 확장하며, Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+Z, Ctrl+Y 가 서식과 되돌리기를 하고, 취소한 <code>keydown</code> 이나 <code>beforeinput</code> 이 막는 것.
 * 페이지는 호스트의 HTML, 이벤트, 선택을 <code>sessionStorage</code> 로 알린다.
 */
public class EditingInputTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EditingInputTest() {
    }

    /**
     * Clicks into the host at a character offset of its first text.
     * <p>
     * 호스트의 첫 텍스트의 글자 오프셋 위치를 클릭한다.
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
     * A click between two characters of the editing host puts the caret there (the page sees it as a collapsed selection), and the typed
     * character is inserted at the caret with the events of a typed key in their order.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickPlacesTheCaretAndTypingInserts() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            type(panel, "x");
            awaitHtml(panel, "hellox world");
            awaitSel(panel, "6,6");
            drain(panel);
            Check.contains(log(panel), "keydown:x keypress:x beforeinput:insertText:x input:insertText:x keyup:x",
                    "the events of a typed key come in the order of the specification");
        } finally {
            close(panel);
        }
    }

    /**
     * Backspace and Delete remove the character before and after the caret, Enter splits the paragraph and Shift+Enter inserts a line break.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackspaceDeleteAndEnter() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_BACK_SPACE, '\b', 0);
            awaitHtml(panel, "hell world");
            awaitSel(panel, "4,4");
            tap(panel, KeyEvent.VK_DELETE, (char) 127, 0);
            awaitHtml(panel, "hellworld");
            awaitSel(panel, "4,4");
            type(panel, "\n");
            awaitHtml(panel, "hell<div>world</div>");
            awaitSel(panel, "4,4");
            drain(panel);
            Check.contains(log(panel), "keydown:Backspace beforeinput:deleteContentBackward:null input:deleteContentBackward:null keyup:Backspace",
                    "Backspace sends its events");
            Check.contains(log(panel), "beforeinput:deleteContentForward:null input:deleteContentForward:null", "Delete sends its events");
            Check.contains(log(panel), "keydown:Enter keypress:Enter beforeinput:insertParagraph:null input:insertParagraph:null keyup:Enter",
                    "Enter breaks the paragraph");
        } finally {
            close(panel);
        }
    }

    /**
     * Shift+Enter inserts a line break.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShiftEnterInsertsALineBreak() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            // Shift+Enter 는 keydown 뒤에 keypress 가 오므로 글자 입력으로 보낸다.
            EditingFixture.keyEvent(panel, KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, '\n', InputEvent.SHIFT_DOWN_MASK);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_TYPED, KeyEvent.VK_ENTER, '\n', InputEvent.SHIFT_DOWN_MASK);
            EditingFixture.keyEvent(panel, KeyEvent.KEY_RELEASED, KeyEvent.VK_ENTER, '\n', InputEvent.SHIFT_DOWN_MASK);
            awaitHtml(panel, "hello<br> world");
            drain(panel);
            Check.contains(log(panel), "beforeinput:insertLineBreak:null input:insertLineBreak:null", "the input type is insertLineBreak");
        } finally {
            close(panel);
        }
    }

    /**
     * The arrow keys, Home and End move the caret, and Ctrl moves by word or to the edge of the host.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCaretKeys() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED, 0);
            awaitSel(panel, "0,0");
            tap(panel, KeyEvent.VK_END, KeyEvent.CHAR_UNDEFINED, 0);
            awaitSel(panel, "11,11");
            tap(panel, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, 0);
            awaitSel(panel, "10,10");
            tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, 0);
            awaitSel(panel, "11,11");
            tap(panel, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitSel(panel, "6,6");
            tap(panel, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitSel(panel, "0,0");
            tap(panel, KeyEvent.VK_END, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitSel(panel, "11,11");
            tap(panel, KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitSel(panel, "0,0");
            drain(panel);
            Check.equal("hello world", stored(panel, "html"), "moving the caret changes nothing");
        } finally {
            close(panel);
        }
    }

    /**
     * Shift with the arrow keys, Home and End extends the selection from the anchor, and Ctrl+A selects the whole host.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShiftExtendsAndSelectAll() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            awaitSel(panel, "5,7");
            tap(panel, KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            awaitSel(panel, "5,0");
            // 선택이 있을 때 입력하면 선택한 내용이 바뀐다.
            type(panel, "j");
            awaitHtml(panel, "j world");
            tap(panel, KeyEvent.VK_A, (char) 1, InputEvent.CTRL_DOWN_MASK);
            awaitSel(panel, "0,7");
        } finally {
            close(panel);
        }
    }

    /**
     * Ctrl+B, Ctrl+I and Ctrl+U format the selection, Ctrl+Z undoes and Ctrl+Y and Ctrl+Shift+Z redo. The platform may report the key
     * character of a Ctrl combination as a control character or as undefined; the keys work either way.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFormatAndUndoShortcuts() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 0);
            // Shift+오른쪽을 다섯 번 눌러 "hello" 를 선택한다.
            for (int i = 0; i < 5; i++) {
                tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            }
            awaitSel(panel, "0,5");
            tap(panel, KeyEvent.VK_B, (char) 2, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "<b>hello</b> world");
            tap(panel, KeyEvent.VK_Z, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "hello world");
            tap(panel, KeyEvent.VK_Y, (char) 25, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "<b>hello</b> world");
            tap(panel, KeyEvent.VK_Z, (char) 26, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "hello world");
            tap(panel, KeyEvent.VK_Z, (char) 26, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
            awaitHtml(panel, "<b>hello</b> world");
            tap(panel, KeyEvent.VK_I, (char) 9, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "<b><i>hello</i></b> world");
            tap(panel, KeyEvent.VK_U, (char) 21, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "<b><i><u>hello</u></i></b> world");
            drain(panel);
            Check.contains(log(panel), "beforeinput:formatBold:null input:formatBold:null", "bold sends the input events");
            Check.contains(log(panel), "beforeinput:historyUndo:null input:historyUndo:null", "undo sends the input events");
        } finally {
            close(panel);
        }
    }

    /**
     * A cancelled <code>keydown</code> stops the character that the key would type; a cancelled <code>beforeinput</code> stops the change.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellingStopsTheEdit() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("ed.addEventListener('keydown', function (e) { if (e.key === 'q') { e.preventDefault(); } });"
                    + " ed.addEventListener('beforeinput', function (e) { if (e.data === 'z') { e.preventDefault(); } });"));
            clickAt(panel, rects, 11);
            type(panel, "q");
            type(panel, "z");
            type(panel, "k");
            awaitHtml(panel, "hello worldk");
            drain(panel);
            String l = log(panel);
            Check.contains(l, "keydown:q keyup:q", "the cancelled keydown gives no keypress and no input");
            Check.contains(l, "beforeinput:insertText:z keyup:z", "the cancelled beforeinput gives no input");
        } finally {
            close(panel);
        }
    }

    /**
     * Keys in a place that cannot be edited change nothing, and a host that the Tab key focuses gets the caret at its start.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNotEditableAndTabFocus() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            click(panel, rects.left(1) + 3, rects.middle(1), 1, false);
            type(panel, "a");
            drain(panel);
            Check.equal("read only", stored(panel, "ro"), "text that is not editable is not changed");
            Check.equal("hello world", stored(panel, "html"), "and the host is not changed");
            // 아무 곳도 클릭하지 않은 상태에서 Tab 키가 호스트로 포커스를 옮기고 입력은 호스트의 처음에 들어간다.
            Panel other = create(400, 300);
            try {
                open(other, page(""));
                tap(other, KeyEvent.VK_TAB, '\t', 0);
                type(other, "a");
                awaitHtml(other, "ahello world");
            } finally {
                close(other);
            }
        } finally {
            close(panel);
        }
    }

    /**
     * Text that an input method composes (Hangul letters become a syllable) is shown in the page as it is composed, with the events of the
     * specification in their order (<code>compositionstart</code>, <code>beforeinput</code>, <code>compositionupdate</code>,
     * <code>input</code> with <code>isComposing</code>, <code>compositionend</code>), and the committed text replaces it. The events are
     * made by hand with the shape that Java gives (committed text followed by composed text); a real input method was not available.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHangulCompositionIsShownAsItIsComposed() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            inputMethod(panel, "", "ㅎ");
            awaitHtml(panel, "helloㅎ world");
            inputMethod(panel, "", "하");
            awaitHtml(panel, "hello하 world");
            inputMethod(panel, "", "한");
            awaitHtml(panel, "hello한 world");
            awaitSel(panel, "6,6");
            // 다음 자음이 앞 음절을 확정하고 새 조합을 시작한다.
            inputMethod(panel, "한", "ㄱ");
            awaitHtml(panel, "hello한ㄱ world");
            inputMethod(panel, "그", "");
            awaitHtml(panel, "hello한그 world");
            awaitSel(panel, "7,7");
            drain(panel);
            String l = log(panel);
            Check.contains(l, "compositionstart: beforeinput:insertCompositionText:ㅎ:c compositionupdate:ㅎ input:insertCompositionText:ㅎ:c",
                    "the first composed letter: the events come in their order and the input is composing");
            Check.contains(l, "compositionupdate:한 input:insertCompositionText:한:c compositionend:한 compositionstart: beforeinput:insertCompositionText:ㄱ:c",
                    "the next letter ends the syllable and starts a new composition");
            Check.contains(l, "compositionupdate:그 input:insertCompositionText:그:c compositionend:그", "the commit ends the composition");
            // 조합 한 번이 되돌리기 한 단계이다: 되돌리면 마지막 조합이 사라지고 앞의 조합은 남는다.
            tap(panel, KeyEvent.VK_Z, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            awaitHtml(panel, "hello한 world");
        } finally {
            close(panel);
        }
    }

    /**
     * The Korean input method of Windows commits a syllable with the Space key in two events that follow each other in the event queue: one that
     * empties the composed text and one with the committed syllable (seen with the real input method in <code>tools/uicheck</code>). The
     * composition ends once with the syllable as data, without an empty composition before it.
     * <p>
     * Windows 의 한국어 입력기는 Space 키로 음절을 확정할 때 이벤트 큐에서 이어지는 두 이벤트를 보낸다: 조합 중인 텍스트를 비우는 것과 확정된 음절(실제 입력기로
     * <code>tools/uicheck</code> 에서 봄). 조합은 앞에 빈 조합 없이 음절을 data 로 한 번 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSpaceCommitInTwoEventsEndsTheCompositionOnce() throws Exception {
        final Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            inputMethod(panel, "", "글");
            awaitHtml(panel, "hello글 world");
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                /** Sends both events in one turn of the event queue. / 두 이벤트를 이벤트 큐 한 바퀴에 보낸다. */
                public void run() {
                    java.awt.event.InputMethodEvent empty = new java.awt.event.InputMethodEvent(panel,
                            java.awt.event.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, null, 0, null, null);
                    java.awt.event.InputMethodEvent commit = new java.awt.event.InputMethodEvent(panel,
                            java.awt.event.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, new java.text.AttributedString("글").getIterator(), 1, null, null);
                    // 등록된 입력기 리스너에 두 이벤트를 차례로 준다.
                    for (java.awt.event.InputMethodListener l : panel.getInputMethodListeners()) {
                        l.inputMethodTextChanged(empty);
                        l.inputMethodTextChanged(commit);
                    }
                }
            });
            awaitHtml(panel, "hello글 world");
            awaitSel(panel, "6,6");
            drain(panel);
            String l = log(panel);
            Check.contains(l, "compositionupdate:글 input:insertCompositionText:글:c", "the syllable was composed");
            Check.isTrue(l.indexOf("compositionend:글") >= 0, "the composition ends with the syllable: " + l);
            Check.isTrue(l.indexOf("compositionupdate: ") < 0 && l.indexOf("compositionend: ") < 0, "no empty composition before the commit: " + l);
        } finally {
            close(panel);
        }
    }

    /**
     * Cancelling the composition (an event without text) removes the composed text and ends the composition, and composed text replaces a
     * selection.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledCompositionAndCompositionOverASelection() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            inputMethod(panel, "", "ㅎ");
            awaitHtml(panel, "helloㅎ world");
            inputMethod(panel, "", "");
            awaitHtml(panel, "hello world");
            awaitSel(panel, "5,5");
            drain(panel);
            Check.contains(log(panel), "compositionupdate: input:insertCompositionText::c compositionend:", "the cancelled composition ends with no text");
            // 선택이 있으면 조합 텍스트가 선택을 대신한다.
            clickAt(panel, rects, 0);
            for (int i = 0; i < 5; i++) {
                tap(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, InputEvent.SHIFT_DOWN_MASK);
            }
            awaitSel(panel, "0,5");
            inputMethod(panel, "", "한");
            awaitHtml(panel, "한 world");
            inputMethod(panel, "한", "");
            awaitSel(panel, "1,1");
        } finally {
            close(panel);
        }
    }

    /**
     * The input methods of the component are on while an editing host has the keyboard focus and off otherwise.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInputMethodsFollowTheEditingFocus() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 5);
            drain(panel);
            Check.equal(Boolean.TRUE, panel.methods.get(panel.methods.size() - 1), "an editing host with the focus turns them on");
            click(panel, rects.left(2) + 5, rects.r[2][1] + 5, 1, false);
            drain(panel);
            Check.equal(Boolean.FALSE, panel.methods.get(panel.methods.size() - 1), "a click outside any editing host turns them off");
        } finally {
            close(panel);
        }
    }

    /**
     * Design mode (<code>document.designMode = "on"</code>) makes the whole page editable: a click puts the caret in the text and typing changes
     * it, although no element has <code>contenteditable</code>; the keys go to the body, which has the focus.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDesignMode() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, pageWith("<p id=ed>hello world</p><p id=ro>read only</p>",
                    "document.designMode = 'on'; document.addEventListener('input', function () { put('html', ed.innerHTML); put('ro', document.getElementById('ro').innerHTML); });"));
            click(panel, rects.left(0) + 5 * rects.advance() + 1, rects.middle(0), 1, false);
            awaitSel(panel, "5,5");
            type(panel, "x");
            awaitHtml(panel, "hellox world");
            // 디자인 모드에서는 페이지의 다른 문단도 편집할 수 있다.
            double[] second = EditingFixture.rect(panel, "ro");
            click(panel, second[0] + 2, second[1] + 12, 1, false);
            type(panel, "y");
            await(panel, "ro", "yread only");
        } finally {
            close(panel);
        }
    }

    /**
     * The caret disappears when the component loses the keyboard focus and appears again when it gets it back.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCaretFollowsTheWindowFocus() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 11);
            int x = (int) Math.round(rects.left(0) + 11 * rects.advance());
            int top = (int) rects.r[0][1];
            Check.isTrue(caretSeen(panel, x, top), "the caret is painted while the component has the focus");
            EditingFixture.loseFocus(panel);
            drain(panel);
            TestSupport.flushEdt();
            Check.isFalse(caretSeen(panel, x, top), "no caret is painted after the component lost the focus");
            EditingFixture.gainFocus(panel);
            drain(panel);
            Check.isTrue(caretSeen(panel, x, top), "and it is painted again when the focus is back");
        } finally {
            close(panel);
        }
    }

    /**
     * Tells whether the caret is painted in a column at some paint within a second (the caret blinks, so one paint may fall in the hidden half).
     * <p>
     * 캐럿이 1 초 안의 어느 그림에서 한 열에 그려지는지 알려 준다(캐럿이 깜박이므로 한 번의 그림은 숨겨진 절반일 수 있다).
     *
     * @param panel the component / 컴포넌트
     * @param x     the column / 열
     * @param top   the top of the line / 줄의 위
     * @return true if painted / 그려지면 true
     * @throws Exception on failure / 실패 시
     */
    private static boolean caretSeen(Panel panel, int x, int top) throws Exception {
        // 깜박임 주기를 넘도록 여러 번 그려 한 번이라도 보이면 true 이다.
        for (int i = 0; i < 20; i++) {
            BufferedImage image = paint(panel);
            int dark = 0;
            // 캐럿 자리의 열에서 줄 높이 안의 어두운 픽셀 수를 센다.
            for (int y = top; y < top + 24; y++) {
                int rgb = image.getRGB(x, y) & 0xffffff;
                if (((rgb >> 16) & 0xff) < 100 && ((rgb >> 8) & 0xff) < 100 && (rgb & 0xff) < 100) {
                    dark++;
                }
            }
            if (dark >= 12) {
                return true;
            }
            Thread.sleep(60);
        }
        return false;
    }

    /**
     * The caret is drawn as a one pixel bar in the line of the editing host where the selection is, and it blinks.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCaretIsPainted() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            clickAt(panel, rects, 11);
            int x = (int) Math.round(rects.left(0) + 11 * rects.advance());
            int top = (int) rects.r[0][1];
            int shown = 0;
            int hidden = 0;
            // 깜박임 주기에 걸쳐 여러 번 그려 보이는 것과 숨겨진 것을 센다.
            for (int i = 0; i < 24; i++) {
                BufferedImage image = paint(panel);
                int dark = 0;
                // 캐럿 자리의 열에서 줄 높이 안의 어두운 픽셀 수를 센다.
                for (int y = top; y < top + 24; y++) {
                    int rgb = image.getRGB(x, y) & 0xffffff;
                    if (((rgb >> 16) & 0xff) < 100 && ((rgb >> 8) & 0xff) < 100 && (rgb & 0xff) < 100) {
                        dark++;
                    }
                }
                if (dark >= 12) {
                    shown++;
                } else if (dark == 0) {
                    hidden++;
                }
                Thread.sleep(60);
            }
            Check.isTrue(shown > 0, "the caret is painted in some of the paints: " + shown);
            Check.isTrue(hidden > 0, "the caret blinks: it is not painted in some of the paints: " + hidden);
        } finally {
            close(panel);
        }
    }
}
