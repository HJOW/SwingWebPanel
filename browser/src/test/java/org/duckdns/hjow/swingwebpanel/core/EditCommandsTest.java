package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.await;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitHtml;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitSel;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.drain;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.log;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.pageWith;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Clip;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the edit commands that a host runs on the component (plan item P31-05, contract 5.26): <code>copy()</code>,
 * <code>cut()</code>, <code>paste()</code> and <code>selectAll()</code> of {@link org.duckdns.hjow.swingwebpanel.SwingWebPanel} do what
 * the matching keys do in an editing host, in a text control of a form and on a plain page, send the clipboard events but no key events
 * to the page, can be called from any thread, and are refused after the component was disposed. The clipboard is the variable of the
 * test component, which stands for the overridable clipboard methods.
 * <p>
 * 호스트가 컴포넌트에서 실행하는 편집 명령의 시험(계획 P31-05, 계약 5.26): {@link org.duckdns.hjow.swingwebpanel.SwingWebPanel} 의 <code>copy()</code>,
 * <code>cut()</code>, <code>paste()</code>, <code>selectAll()</code> 이 편집 호스트, 폼의 텍스트 컨트롤, 보통 페이지에서 해당 키와 같은 일을 하고, 페이지에 클립보드
 * 이벤트는 보내되 키 이벤트는 보내지 않으며, 어느 스레드에서나 부를 수 있고, 컴포넌트를 폐기한 뒤에는 거부되는지 확인한다. 클립보드는 재정의할 수 있는
 * 클립보드 메서드를 대신하는 시험 컴포넌트의 변수이다.
 */
public class EditCommandsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EditCommandsTest() {
    }

    /**
     * Waits until the component recorded a number of copies.
     * <p>
     * 컴포넌트가 주어진 수의 복사를 기록할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param count the expected number / 기대하는 수
     * @throws Exception on failure / 실패 시
     */
    private static void awaitCopies(Panel panel, int count) throws Exception {
        long deadline = System.currentTimeMillis() + EditingFixture.WAIT_MS;
        // 복사가 EDT 에서 기록될 때까지 기다린다.
        while (panel.texts.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(count, panel.texts.size(), "the number of copies");
    }

    /**
     * Runs a command of the component on the EDT, as a menu item does.
     * <p>
     * 메뉴 항목이 하듯 컴포넌트의 명령을 EDT 에서 실행한다.
     *
     * @param panel   the component / 컴포넌트
     * @param command <code>copy</code>, <code>cut</code>, <code>paste</code> or <code>selectAll</code> / 명령
     * @throws Exception on failure / 실패 시
     */
    private static void run(final Panel panel, final String command) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Calls the method of the command. / 명령의 메서드를 부른다. */
            public void run() {
                // 명령 이름에 해당하는 공개 메서드를 부른다.
                if ("copy".equals(command)) {
                    panel.copy();
                } else if ("cut".equals(command)) {
                    panel.cut();
                } else if ("paste".equals(command)) {
                    panel.paste();
                } else {
                    panel.selectAll();
                }
            }
        });
    }

    /**
     * In an editing host the commands select all its text, copy it, cut it and paste the clipboard, with the clipboard events and the
     * input events of the keys but without any key event.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_EDITCMD_1_EditingHost() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            click(panel, rects.left(0) + 1, rects.middle(0), 1, false);
            awaitSel(panel, "0,0");
            run(panel, "selectAll");
            awaitSel(panel, "0,11");
            run(panel, "copy");
            awaitCopies(panel, 1);
            Check.equal("hello world", panel.texts.get(0), "copy writes the selected text");
            run(panel, "cut");
            awaitHtml(panel, "");
            awaitCopies(panel, 2);
            Check.equal("hello world", panel.texts.get(1), "cut writes the selected text and removes it");
            panel.clipboard = new Clip("XY", null);
            run(panel, "paste");
            awaitHtml(panel, "XY");
            drain(panel);
            String log = log(panel);
            Check.contains(log, "copy:hello world cut:hello world beforeinput:deleteByCut:null input:deleteByCut:null paste:XY"
                    + " beforeinput:insertFromPaste:null input:insertFromPaste:null", "the clipboard and input events of the commands");
            Check.isTrue(log.indexOf("keydown:c") < 0 && log.indexOf("keydown:x") < 0 && log.indexOf("keydown:v") < 0 && log.indexOf("keydown:a") < 0,
                    "the commands send no key events: " + log);
        } finally {
            close(panel);
        }
    }

    /**
     * In a focused text control of a form the commands act on the text of the control, and the page sees the clipboard events.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_EDITCMD_2_TextControl() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, pageWith("<input id=ed value='abc def' style='width:200px'>",
                    "ed.addEventListener('input', function () { put('value', ed.value); });"));
            click(panel, rects.left(0) + 5, rects.middle(0), 1, false);
            long deadline = System.currentTimeMillis() + EditingFixture.WAIT_MS;
            // 컨트롤이 포커스를 얻어 편집기가 열릴 때까지 기다린다.
            while (panel.getComponentCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.isTrue(panel.getComponentCount() > 0, "an editor should open over the control");
            run(panel, "selectAll");
            run(panel, "copy");
            awaitCopies(panel, 1);
            Check.equal("abc def", panel.texts.get(0), "copy writes the text of the control");
            run(panel, "cut");
            awaitCopies(panel, 2);
            await(panel, "value", "");
            panel.clipboard = new Clip("new", null);
            run(panel, "paste");
            await(panel, "value", "new");
            drain(panel);
            Check.contains(log(panel), "copy:abc def", "the page saw the copy event of the control");
            Check.contains(log(panel), "paste:new", "and the paste event");
        } finally {
            close(panel);
        }
    }

    /**
     * On a page where nothing is edited, select all selects the content of the page and copy writes it; cut and paste change nothing
     * (paste still sends its event). The methods may be called from a thread that is not the event dispatch thread.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_EDITCMD_3_PlainPageFromAnotherThread() throws Exception {
        Panel panel = create(400, 300);
        try {
            open(panel, pageWith("<div id=ed>plain text</div><div id=ro>second line</div>",
                    "document.addEventListener('paste', function (e) { put('pasted', e.clipboardData.getData('text/plain')); });"));
            // 시험 스레드(EDT 가 아니다)에서 바로 부른다.
            panel.selectAll();
            panel.copy();
            awaitCopies(panel, 1);
            Check.contains(panel.texts.get(0), "plain text", "the whole page is copied");
            Check.contains(panel.texts.get(0), "second line", "including the second block");
            panel.clipboard = new Clip("P", null);
            panel.cut();
            panel.paste();
            await(panel, "pasted", "P");
            drain(panel);
            Check.equal("plain text", EditingFixture.stored(panel, "html"), "a page that cannot be edited is not changed");
            Check.equal(1, panel.texts.size(), "cut writes nothing where nothing can be edited");
        } finally {
            close(panel);
        }
    }

    /**
     * Presses a key on the component and tells whether the component consumed the key event.
     * <p>
     * 컴포넌트에서 키를 누르고 컴포넌트가 그 키 이벤트를 소비했는지 알려 준다.
     *
     * @param panel     the component / 컴포넌트
     * @param keyCode   the AWT key code / AWT 키 코드
     * @param modifiers the extended modifiers / 확장 보조 값
     * @return true if the event was consumed / 이벤트가 소비되었으면 true
     * @throws Exception on failure / 실패 시
     */
    private static boolean consumes(final Panel panel, final int keyCode, final int modifiers) throws Exception {
        final boolean[] consumed = new boolean[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Sends the key press and the release to the key listeners. / 키 누름과 뗌을 키 리스너에 보낸다. */
            public void run() {
                KeyEvent press = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
                KeyEvent release = new KeyEvent(panel, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
                // 등록된 키 리스너를 직접 부른다(창에 없는 컴포넌트는 키보드 포커스를 가질 수 없다).
                for (KeyListener l : panel.getKeyListeners()) {
                    l.keyPressed(press);
                    l.keyReleased(release);
                }
                consumed[0] = press.isConsumed();
            }
        });
        return consumed[0];
    }

    /**
     * The component consumes the key events that it handles as commands itself, so that a key binding of the host window (a menu
     * accelerator) does not run for the same key: the copy, select-all, paste and zoom keys everywhere, and the editing keys while the
     * focus is in an editable place. Other shortcuts stay free for the host.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_EDITCMD_5_CommandKeysAreConsumed() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            int ctrl = InputEvent.CTRL_DOWN_MASK;
            Check.isTrue(consumes(panel, KeyEvent.VK_C, ctrl), "Ctrl+C is the copy of the page");
            Check.isTrue(consumes(panel, KeyEvent.VK_A, ctrl), "Ctrl+A selects all");
            Check.isTrue(consumes(panel, KeyEvent.VK_V, ctrl), "Ctrl+V is the paste of the page");
            Check.isTrue(consumes(panel, KeyEvent.VK_EQUALS, ctrl), "Ctrl+= zooms");
            Check.isTrue(consumes(panel, KeyEvent.VK_0, ctrl), "Ctrl+0 restores the zoom");
            Check.isTrue(!consumes(panel, KeyEvent.VK_U, ctrl), "Ctrl+U is free for the host while nothing is edited");
            Check.isTrue(!consumes(panel, KeyEvent.VK_X, ctrl), "and so is Ctrl+X");
            Check.isTrue(!consumes(panel, KeyEvent.VK_T, ctrl), "Ctrl+T is never a key of the page");
            Check.isTrue(!consumes(panel, KeyEvent.VK_F5, 0), "nor is F5");
            Check.isTrue(!consumes(panel, KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "nor Alt+Left");
            click(panel, rects.left(0) + 1, rects.middle(0), 1, false);
            awaitSel(panel, "0,0");
            drain(panel);
            Check.isTrue(consumes(panel, KeyEvent.VK_U, ctrl), "in an editable place Ctrl+U underlines");
            Check.isTrue(consumes(panel, KeyEvent.VK_X, ctrl), "Ctrl+X cuts");
            Check.isTrue(consumes(panel, KeyEvent.VK_Z, ctrl), "Ctrl+Z undoes");
            Check.isTrue(!consumes(panel, KeyEvent.VK_T, ctrl), "Ctrl+T stays free for the host");
        } finally {
            close(panel);
        }
    }

    /**
     * After the component was disposed the commands are refused.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_EDITCMD_4_DisposedComponent() throws Exception {
        final Panel panel = create(400, 300);
        close(panel);
        String[] commands = {"copy", "cut", "paste", "selectAll"};
        // 명령마다 IllegalStateException 을 확인한다.
        for (int i = 0; i < commands.length; i++) {
            boolean refused = false;
            try {
                if (i == 0) {
                    panel.copy();
                } else if (i == 1) {
                    panel.cut();
                } else if (i == 2) {
                    panel.paste();
                } else {
                    panel.selectAll();
                }
            } catch (IllegalStateException e) {
                refused = true;
            }
            Check.isTrue(refused, commands[i] + " is refused after dispose");
        }
    }
}
