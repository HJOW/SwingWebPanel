package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.await;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.drain;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.log;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.pageWith;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.stored;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Clip;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of cut, copy and paste in the Swing editor that sits over a focused text control (plan item P20-06): the Swing actions of the
 * editor (what Ctrl+X, Ctrl+C and Ctrl+V do) send the cancelable <code>cut</code>, <code>copy</code> and <code>paste</code> events to the
 * control first; the clipboard of the component (the variable of the test component, which stands for its overridable methods) is where
 * the text goes and where it comes from; a cancelled event stops the action, and a script that cancels a copy and sets its own data puts
 * that on the clipboard. The Swing actions are invoked directly because the test component is not shown in a window and cannot have the
 * keyboard focus; the key bindings of Swing are not what is tested.
 * <p>
 * 포커스를 가진 텍스트 컨트롤 위에 놓이는 Swing 편집기의 잘라내기, 복사, 붙여넣기 시험(계획 P20-06): 편집기의 Swing 동작(Ctrl+X, Ctrl+C, Ctrl+V 가 하는 일)이
 * 취소할 수 있는 <code>cut</code>, <code>copy</code>, <code>paste</code> 이벤트를 컨트롤에 먼저 보낸다. 컴포넌트의 클립보드(컴포넌트의 재정의할 수 있는 메서드를 대신하는
 * 시험 컴포넌트의 변수)가 글이 가는 곳이자 오는 곳이며, 취소한 이벤트는 동작을 막고, 복사를 취소하고 자기 데이터를 정한 스크립트는 그것을 클립보드에 넣는다.
 * 시험 컴포넌트는 창에 보이지 않아 키보드 포커스를 가질 수 없으므로 Swing 동작을 직접 부른다. Swing 의 키 바인딩은 시험 대상이 아니다.
 */
public class FormClipboardTest {

    /**
     * The body of the page: a text field (the element that the page script listens to) and a text area.
     * <p>
     * 페이지의 본문: 텍스트 필드(페이지 스크립트가 듣는 요소)와 텍스트 영역.
     */
    private static final String BODY = "<input id=ed value='abc def' style='width:200px'><textarea id=area style='width:200px;height:60px'>one two</textarea>";

    /**
     * The script of the page: it reports the value of the field after every <code>input</code> event, and it cancels the events of the text
     * <code>no</code> and answers a copy with its own text when the text is <code>mine</code>.
     * <p>
     * 페이지의 스크립트: <code>input</code> 이벤트마다 필드의 값을 알리고, 텍스트가 <code>no</code> 이면 이벤트를 취소하며, <code>mine</code> 이면 복사에 자기 텍스트로 답한다.
     */
    private static final String SCRIPT = "ed.addEventListener('input', function () { put('value', ed.value); });"
            + " ed.addEventListener('paste', function (e) { if (e.clipboardData.getData('text/plain') === 'no') { e.preventDefault(); } });"
            + " ed.addEventListener('copy', function (e) { if (ed.value.indexOf('mine') === 0) { e.clipboardData.setData('text/plain', 'from script'); e.preventDefault(); } });"
            + " ed.addEventListener('cut', function (e) { if (ed.value.indexOf('keep') === 0) { e.preventDefault(); } });";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormClipboardTest() {
    }

    /**
     * Finds the Swing text component of the editor that is open over a control of the component.
     * <p>
     * 컴포넌트의 컨트롤 위에 열려 있는 편집기의 Swing 텍스트 컴포넌트를 찾는다.
     *
     * @param panel the component / 컴포넌트
     * @return the text component / 텍스트 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private static JTextComponent editor(final Panel panel) throws Exception {
        long deadline = System.currentTimeMillis() + EditingFixture.WAIT_MS;
        // 컨트롤이 포커스를 얻어 편집기가 열릴 때까지 컴포넌트의 자식을 확인한다.
        while (System.currentTimeMillis() < deadline) {
            final JTextComponent[] found = new JTextComponent[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Looks for the editor among the children. / 자식 중에서 편집기를 찾는다. */
                public void run() {
                    // 편집기는 텍스트 컴포넌트이거나 그것을 담은 스크롤 창이다.
                    for (Component c : panel.getComponents()) {
                        if (c instanceof JTextComponent) {
                            found[0] = (JTextComponent) c;
                        } else if (c instanceof JScrollPane && ((JScrollPane) c).getViewport().getView() instanceof JTextComponent) {
                            found[0] = (JTextComponent) ((JScrollPane) c).getViewport().getView();
                        }
                    }
                }
            });
            if (found[0] != null) {
                return found[0];
            }
            Thread.sleep(10);
        }
        Check.fail("an editor should open over the control");
        return null;
    }

    /**
     * Calls a Swing action of an editor on the EDT.
     * <p>
     * 편집기의 Swing 동작을 EDT 에서 부른다.
     *
     * @param text   the editor / 편집기
     * @param action <code>cut</code>, <code>copy</code>, <code>paste</code> or <code>selectAll</code> / 동작
     * @throws Exception on failure / 실패 시
     */
    private static void act(final JTextComponent text, final String action) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Runs the action. / 동작을 실행한다. */
            public void run() {
                if ("cut".equals(action)) {
                    text.cut();
                } else if ("copy".equals(action)) {
                    text.copy();
                } else if ("paste".equals(action)) {
                    text.paste();
                } else {
                    text.selectAll();
                }
            }
        });
    }

    /**
     * Cut sends <code>cut</code> and then writes the selected text to the clipboard of the component and removes it (the value follows with
     * an <code>input</code> event); a cancelled cut does neither; copy sends <code>copy</code> and writes the text without removing it, and
     * a script that cancels it and sets its own data puts that on the clipboard.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCutAndCopyOfATextField() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, pageWith(BODY, SCRIPT));
            click(panel, rects.left(0) + 5, rects.middle(0), 1, false);
            JTextComponent text = editor(panel);
            act(text, "selectAll");
            act(text, "copy");
            awaitCopies(panel, 1);
            Check.equal("abc def", panel.texts.get(0), "copy writes the selected text");
            Check.equal("abc def", text.getText(), "and removes nothing");
            act(text, "cut");
            awaitCopies(panel, 2);
            Check.equal("abc def", panel.texts.get(1), "cut writes the selected text");
            await(panel, "value", "");
            Check.equal("", text.getText(), "and removes it");
            drain(panel);
            Check.contains(log(panel), "copy:abc def", "the page saw the copy event with the text");
            Check.contains(log(panel), "cut:abc def", "and the cut event");
        } finally {
            close(panel);
        }
    }

    /**
     * A script that cancels <code>cut</code> keeps the text; one that cancels <code>copy</code> and sets its own data gives the clipboard
     * that data.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledCutAndCopy() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, pageWith(BODY.replace("value='abc def'", "value='keep this'"), SCRIPT));
            click(panel, rects.left(0) + 5, rects.middle(0), 1, false);
            JTextComponent text = editor(panel);
            act(text, "selectAll");
            act(text, "cut");
            drain(panel);
            TestSupport.flushEdt();
            Check.equal("keep this", text.getText(), "the cancelled cut keeps the text");
            Check.equal(0, panel.texts.size(), "and writes nothing to the clipboard");
            final JTextComponent t = text;
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Sets a value that the script answers specially. / 스크립트가 특별히 답하는 값을 넣는다. */
                public void run() {
                    t.setText("mine!");
                    t.selectAll();
                }
            });
            await(panel, "value", "mine!");
            act(text, "copy");
            awaitCopies(panel, 1);
            Check.equal("from script", panel.texts.get(0), "a script that cancels the copy and sets data puts its data on the clipboard");
        } finally {
            close(panel);
        }
    }

    /**
     * Paste sends <code>paste</code> with the text of the clipboard of the component, and replaces the selection with it unless a listener
     * cancelled the event; a clipboard that cannot be read, or an empty one, pastes nothing.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteIntoATextField() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, pageWith(BODY, SCRIPT));
            click(panel, rects.left(0) + 5, rects.middle(0), 1, false);
            JTextComponent text = editor(panel);
            panel.clipboard = new Clip("XYZ", null);
            act(text, "selectAll");
            act(text, "paste");
            await(panel, "value", "XYZ");
            Check.equal("XYZ", text.getText(), "the paste replaced the selection");
            drain(panel);
            Check.contains(log(panel), "paste:XYZ", "the page saw the paste event with the text of the clipboard");
            panel.clipboard = new Clip("no", null);
            act(text, "paste");
            drain(panel);
            TestSupport.flushEdt();
            Check.equal("XYZ", text.getText(), "a cancelled paste inserts nothing");
            panel.clipboard = null;
            act(text, "paste");
            panel.clipboardBroken = true;
            act(text, "paste");
            drain(panel);
            TestSupport.flushEdt();
            Check.equal("XYZ", text.getText(), "an empty clipboard and one that cannot be read paste nothing");
            Check.equal("XYZ", stored(panel, "value"), "and the DOM value is unchanged");
        } finally {
            close(panel);
        }
    }

    /**
     * A paste into a text field shows the HTML of the clipboard to the <code>paste</code> event as well (its fragment, without the document
     * frame of the platform), while the field takes the text (found in the real-window check: the event only had <code>text/plain</code>).
     * <p>
     * 텍스트 필드에 붙여넣으면 <code>paste</code> 이벤트가 클립보드의 HTML 도 본다(플랫폼의 문서 틀을 뺀 조각). 필드에는 글이 들어간다(실제 창 확인에서 찾음:
     * 이벤트에 <code>text/plain</code> 만 있었다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPasteShowsTheHtmlOfTheClipboard() throws Exception {
        Panel panel = create(400, 300);
        try {
            String script = SCRIPT + " ed.addEventListener('paste', function (e) { put('types', Array.prototype.join.call(e.clipboardData.types, ','));"
                    + " put('pastehtml', e.clipboardData.getData('text/html')); });";
            Rects rects = open(panel, pageWith(BODY, script));
            click(panel, rects.left(0) + 5, rects.middle(0), 1, false);
            JTextComponent text = editor(panel);
            panel.clipboard = new Clip("rich text", "<html><body><!--StartFragment--><b>rich</b> text<!--EndFragment--></body></html>");
            act(text, "selectAll");
            act(text, "paste");
            await(panel, "value", "rich text");
            Check.equal("text/plain,text/html", stored(panel, "types"), "the event offers both formats");
            Check.equal("<b>rich</b> text", stored(panel, "pastehtml"), "the HTML is the fragment");
        } finally {
            close(panel);
        }
    }

    /**
     * Waits until a number of copies were recorded by the component.
     * <p>
     * 컴포넌트가 복사를 주어진 수만큼 기록할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param count the number of copies / 복사 수
     * @throws Exception on interruption / 인터럽트 시
     */
    private static void awaitCopies(Panel panel, int count) throws Exception {
        long deadline = System.currentTimeMillis() + EditingFixture.WAIT_MS;
        // 복사가 EDT 에서 기록될 때까지 기다린다.
        while (panel.texts.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(count, panel.texts.size(), "the number of copies");
    }
}
