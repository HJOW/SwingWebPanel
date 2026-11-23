package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitCopies;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitReport;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitSelection;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.count;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.drag;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.key;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.mouse;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.paint;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.reported;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the selection of text and the copy with the real component and real AWT events (plan item P11-08): a press, a drag and a release
 * on the component select text through the DOM selection that scripts see; double and triple clicks, Shift+click, the Shift+arrow keys,
 * Select All, <code>user-select</code>, the highlight in the pixels of the painted component, and Ctrl+C with the <code>copy</code> event
 * (a script that puts its own data on the clipboard). The case that uses the system clipboard skips itself when there is none.
 * <p>
 * 실제 컴포넌트와 실제 AWT 이벤트로 하는 텍스트 선택과 복사의 시험(계획 P11-08): 컴포넌트 위의 누름, 드래그, 뗌이 스크립트가 보는 DOM 선택을 통해 텍스트를
 * 선택한다. 더블/트리플 클릭, Shift+클릭, Shift+화살표 키, 전체 선택, <code>user-select</code>, 그려진 컴포넌트의 픽셀에 나타나는 강조, <code>copy</code> 이벤트와
 * 함께하는 Ctrl+C(스크립트가 자기 데이터를 클립보드에 넣는다). 시스템 클립보드를 쓰는 사례는 클립보드가 없으면 스스로 건너뛴다.
 */
public class TextSelectionIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextSelectionIntegrationTest() {
    }

    /**
     * Makes line feeds and tabs of a text visible for the messages of failed checks.
     * <p>
     * 실패한 검사의 메시지를 위해 텍스트의 줄바꿈과 탭이 보이게 한다.
     *
     * @param s the text / 텍스트
     * @return the text with escapes / 이스케이프한 텍스트
     */
    private static String visible(String s) {
        return s.replace("\n", "\\n").replace("\t", "\\t");
    }

    /**
     * Dragging over text selects it through the selection that scripts see (<code>selectionchange</code> is fired and the text is what
     * <code>getSelection().toString()</code> gives), the highlight is painted over exactly the selected characters, and a click without a drag
     * empties the highlight again.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragSelectsTextAndPaintsTheHighlight() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(0) + 5 * adv + 1, rects.middle(0));
            awaitSelection(panel, "hello");
            TestSupport.flushEdt();
            BufferedImage image = paint(panel);
            int y = (int) rects.bottom(0);
            int[] inside = count(image, (int) (rects.left(0) + 0.5 * adv), (int) (rects.left(0) + 4.5 * adv), y);
            int[] outside = count(image, (int) (rects.left(0) + 6 * adv), (int) (rects.left(0) + 10 * adv), y);
            Check.isTrue(inside[0] > 3 * adv, "the selected characters are highlighted: " + inside[0]);
            Check.equal(0, outside[0], "the rest of the line is not highlighted");
            Check.isTrue(outside[1] > 3 * adv, "and stays white");
            click(panel, rects.left(1) + 2, rects.middle(1), 1, false);
            awaitSelection(panel, "");
            TestSupport.flushEdt();
            int[] after = count(paint(panel), (int) (rects.left(0) + 0.5 * adv), (int) (rects.left(0) + 4.5 * adv), y);
            Check.equal(0, after[0], "a click empties the highlight");
        } finally {
            close(panel);
        }
    }

    /**
     * A drag from one block to the end of the next selects across the blocks in the DOM; Ctrl+C copies the rendered text (with the blocks
     * separated) and the markup, through the overridable method of the component, and Ctrl+Insert does the same.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragAcrossBlocksAndCopy() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(1) + rects.r[1][2] + 30, rects.middle(1));
            awaitSelection(panel, "hello world\n\nsecond line");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            awaitCopies(panel, 1);
            Check.equal("hello world\\n\\nsecond line", visible(panel.texts.get(0)), "the copied text has the paragraphs separated by an empty line");
            Check.notNull(panel.markups.get(0), "the markup is copied as well");
            Check.contains(panel.markups.get(0), "second line", "the markup holds the selected text");
            key(panel, KeyEvent.VK_INSERT, InputEvent.CTRL_DOWN_MASK);
            awaitCopies(panel, 2);
            Check.equal("hello world\\n\\nsecond line", visible(panel.texts.get(1)), "Ctrl+Insert copies as well");
        } finally {
            close(panel);
        }
    }

    /**
     * A double click selects a word, a triple click the paragraph, a drag after the double click goes on by words, and Shift+click extends
     * the selection from its anchor.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWordParagraphAndShiftClick() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            double x = rects.left(0) + 8 * adv;
            click(panel, x, rects.middle(0), 2, false);
            awaitSelection(panel, "world");
            click(panel, x, rects.middle(0), 3, false);
            awaitSelection(panel, "hello world");
            // 더블 클릭으로 낱말을 고르고 두 번째 줄의 가운데까지 끌면 낱말 단위로 확장된다.
            double x2 = rects.left(1) + 2 * rects.advance(1, 11);
            mouse(panel, MouseEvent.MOUSE_PRESSED, rects.left(0) + 1, rects.middle(0), 2, InputEvent.BUTTON1_DOWN_MASK);
            mouse(panel, MouseEvent.MOUSE_DRAGGED, x2, rects.middle(1), 2, InputEvent.BUTTON1_DOWN_MASK);
            mouse(panel, MouseEvent.MOUSE_RELEASED, x2, rects.middle(1), 2, 0);
            awaitSelection(panel, "hello world\n\nsecond");
            // 한 번 누른 뒤 Shift+클릭하면 anchor 는 그대로이고 focus 만 옮겨진다.
            click(panel, rects.left(0) + 1, rects.middle(0), 1, false);
            click(panel, rects.left(0) + 5 * adv + 1, rects.middle(0), 1, true);
            awaitSelection(panel, "hello");
        } finally {
            close(panel);
        }
    }

    /**
     * Shift+arrow keys move the focus of the selection (one character, one word with Ctrl, one line, the start or the end of the line) while
     * the anchor stays; nothing happens without a selection.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShiftArrowKeysExtendTheSelection() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            int shift = InputEvent.SHIFT_DOWN_MASK;
            key(panel, KeyEvent.VK_RIGHT, shift);
            TestSupport.flushEdt();
            Thread.sleep(150);
            Check.equal("", reported(panel), "nothing happens without a selection");
            click(panel, rects.left(0) + 2 * adv + 1, rects.middle(0), 1, false);
            awaitSelection(panel, "");
            key(panel, KeyEvent.VK_RIGHT, shift);
            awaitSelection(panel, "l");
            key(panel, KeyEvent.VK_RIGHT, shift);
            awaitSelection(panel, "ll");
            key(panel, KeyEvent.VK_LEFT, shift);
            awaitSelection(panel, "l");
            key(panel, KeyEvent.VK_END, shift);
            awaitSelection(panel, "llo world");
            key(panel, KeyEvent.VK_HOME, shift);
            awaitSelection(panel, "he");
            // 한 줄 아래로 가면 같은 가로 위치의 다음 줄 글자까지, 다시 위로 가면 원래 자리로 돌아온다.
            click(panel, rects.left(0) + 2 * adv + 1, rects.middle(0), 1, false);
            key(panel, KeyEvent.VK_DOWN, shift);
            awaitSelection(panel, "llo world\n\nse");
            key(panel, KeyEvent.VK_UP, shift);
            awaitSelection(panel, "");
            // Ctrl 과 함께이면 낱말 단위로 간다.
            click(panel, rects.left(0) + 1, rects.middle(0), 1, false);
            key(panel, KeyEvent.VK_RIGHT, shift | InputEvent.CTRL_DOWN_MASK);
            awaitSelection(panel, "hello");
            key(panel, KeyEvent.VK_RIGHT, shift | InputEvent.CTRL_DOWN_MASK);
            awaitSelection(panel, "hello world");
            key(panel, KeyEvent.VK_LEFT, shift | InputEvent.CTRL_DOWN_MASK);
            awaitSelection(panel, "hello ");
        } finally {
            close(panel);
        }
    }

    /**
     * Ctrl+A selects everything in the body; text with <code>user-select: none</code> does not start a selection, is not highlighted and is
     * not copied.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSelectAllAndUserSelectNone() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(0) + 5 * adv + 1, rects.middle(0));
            awaitSelection(panel, "hello");
            // 선택할 수 없는 텍스트를 누르고 끌어도 기존 선택은 바뀌지 않는다.
            drag(panel, rects.left(2) + 1, rects.middle(2), rects.left(2) + 40, rects.middle(2));
            TestSupport.flushEdt();
            Thread.sleep(150);
            Check.isTrue(reported(panel).startsWith("sel:hello|"), "the selection stays: " + reported(panel));
            key(panel, KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
            // 전체 선택의 Selection.toString() 은 복사 때와 같은 렌더링 텍스트이므로 선택할 수 없는 텍스트와 스크립트 텍스트가 없다.
            awaitReport(panel, "sel:hello world\n\nsecond line|");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            awaitCopies(panel, 1);
            Check.equal("hello world\\n\\nsecond line", visible(panel.texts.get(0)), "the copy leaves out the text that cannot be selected and the script");
            int[] locked = count(paint(panel), (int) rects.left(2) + 2, (int) (rects.left(2) + 8 * adv), (int) rects.bottom(2));
            Check.equal(0, locked[0], "the locked text is not highlighted");
        } finally {
            close(panel);
        }
    }

    /**
     * A script that selects text (<code>selectAllChildren</code>) makes the highlight appear, and the user's selection is what the script
     * reads: both are the one DOM selection.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptSelectionIsDrawn() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page("window.getSelection().selectAllChildren(document.getElementById('b'));"));
            awaitSelection(panel, "second line");
            TestSupport.flushEdt();
            double adv = rects.advance(1, 11);
            int[] inside = count(paint(panel), (int) (rects.left(1) + 0.5 * adv), (int) (rects.left(1) + 10 * adv), (int) rects.bottom(1));
            Check.isTrue(inside[0] > 8 * adv, "the text that the script selected is highlighted: " + inside[0]);
        } finally {
            close(panel);
        }
    }

    /**
     * A listener of the <code>copy</code> event can read the selected text from <code>clipboardData</code> and put its own data on the
     * clipboard by cancelling the event; without cancelling, the selection is copied as it is.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCopyEventCanReplaceTheData() throws Exception {
        Panel panel = create(400, 300);
        try {
            String script = "document.addEventListener('copy', function (e) {"
                    + " var d = e.clipboardData; var before = d.getData('text/plain');"
                    + " d.setData('text/plain', 'custom:' + before + ':' + d.types.join('+'));"
                    + " if (window.getSelection().toString() === 'hello') { e.preventDefault(); } });";
            Rects rects = open(panel, page(script));
            double adv = rects.advance(0, 11);
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(0) + 5 * adv + 1, rects.middle(0));
            awaitSelection(panel, "hello");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            awaitCopies(panel, 1);
            Check.equal("custom:hello:text/plain+text/html", panel.texts.get(0), "the cancelled event puts the data of the listener on the clipboard");
            drag(panel, rects.left(1) + 1, rects.middle(1), rects.left(1) + 6 * rects.advance(1, 11) + 1, rects.middle(1));
            awaitSelection(panel, "second");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            awaitCopies(panel, 2);
            Check.equal("second", panel.texts.get(1), "an event that is not cancelled copies the selection itself");
            click(panel, rects.left(0) + 1, rects.middle(0), 1, false);
            awaitSelection(panel, "");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            TestSupport.flushEdt();
            Thread.sleep(150);
            Check.equal(2, panel.texts.size(), "nothing is copied without a selection");
        } finally {
            close(panel);
        }
    }

    /**
     * The default clipboard of the component is the system clipboard: the text and the markup of the selection arrive there. The case puts
     * back what the clipboard had, and skips itself without a screen or when the clipboard cannot be used.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSystemClipboard() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            Check.skip("no screen: the system clipboard is not available");
        }
        Clipboard clipboard;
        String old = null;
        try {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                old = (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            Check.skip("the system clipboard cannot be read: " + e);
            return;
        } catch (java.awt.AWTError e) {
            Check.skip("the system clipboard cannot be read: " + e);
            return;
        }
        Panel panel = create(400, 300);
        panel.recording = false;
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(0) + 5 * adv + 1, rects.middle(0));
            awaitSelection(panel, "hello");
            key(panel, KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
            String got = null;
            long deadline = System.currentTimeMillis() + 5000;
            // 복사가 EDT 로 넘어가 시스템 클립보드에 쓰일 때까지 기다린다.
            while (!"hello".equals(got) && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
                try {
                    Transferable t = clipboard.getContents(null);
                    got = t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor) ? (String) t.getTransferData(DataFlavor.stringFlavor) : null;
                } catch (Exception e) {
                    got = null;
                }
            }
            if (!"hello".equals(got) && panel.getStatusText().startsWith("Copy failed")) {
                Check.skip("the clipboard of this environment cannot be written: " + panel.getStatusText());
            }
            Check.equal("hello", got, "the selected text is on the system clipboard");
        } finally {
            close(panel);
            // 시험 전의 클립보드 내용을 되돌려 놓는다.
            if (old != null) {
                clipboard.setContents(new StringSelection(old), null);
            }
        }
    }

}
