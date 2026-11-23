package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlTextAreaElement;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Integration tests of the form controls of a page with the Swing parts (plan items P11-05, P11-06, P11-07, P11-02): the editors that sit over
 * text fields and text areas and mirror their text and selection to the DOM and back, the events that the edits fire (<code>input</code>,
 * <code>change</code>, composition), implicit submission, the validation bubble, the file chooser, the redrawing after a click on a check box,
 * and the removal of everything when the page is replaced. The Swing components are real; the AWT focus is not (it needs a screen), so the
 * tests give the DOM focus and edit the document of the editor, as the user's typing does. A real keyboard, a real IME and real file dialogs are
 * not tested here.
 * <p>
 * Swing 부분을 포함한 페이지의 폼 컨트롤 통합 시험(계획 P11-05, P11-06, P11-07, P11-02): 텍스트 필드와 텍스트 영역 위에 놓여 글과 선택을 DOM 과 서로 비추는
 * 편집기, 편집이 발생시키는 이벤트(<code>input</code>, <code>change</code>, 조합), 암묵적 제출, 검증 말풍선, 파일 선택기, 체크박스 클릭 뒤 다시 그리기,
 * 페이지 교체 때 모든 것의 제거. Swing 컴포넌트는 실제이고 AWT 포커스는 실제가 아니므로(화면이 필요) 시험은 DOM 포커스를 주고 사용자의 입력처럼 편집기의
 * 문서를 편집한다. 실제 키보드, 실제 IME, 실제 파일 대화상자는 여기서 시험하지 않는다.
 */
public class FormControlIntegrationTest {

    /** A page with a form of text controls. 텍스트 컨트롤이 있는 폼 페이지. */
    private static final String PAGE = "<!doctype html><base href='http://forms.test/dir/'><style>html,body{margin:0} input,textarea,select{font:12px monospace;margin:0;display:block}"
            + "#cb{margin:0}</style><form id=f action='/go' method=get>"
            + "<input id=name name=name value='abc'>"
            + "<input id=pw name=pw type=password value='secret'>"
            + "<textarea id=ta name=ta cols=20 rows=3>line1\nline2</textarea>"
            + "<input id=req name=req required>"
            + "<input id=cb name=cb type=checkbox>"
            + "<input id=file name=file type=file>"
            + "<input id=ro name=ro value='fixed' readonly>"
            + "<input id=ml name=ml maxlength=5>"
            + "<input id=ok type=submit value=Send></form>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormControlIntegrationTest() {
    }

    /**
     * The page, the overlays and the recorders that a test needs.
     * <p>
     * 시험에 필요한 페이지, 오버레이, 기록기.
     */
    static final class Fixture implements PageView.Host {
        /** The page component. 페이지 컴포넌트. */
        final JPanel component = new JPanel(null);
        /** The loader. 로더. */
        final FakeLoader loader = new FakeLoader();
        /** The cookies. 쿠키. */
        final org.duckdns.hjow.swingwebpanel.net.CookieStore cookies = new org.duckdns.hjow.swingwebpanel.net.CookieStore();
        /** The fetcher. 가져오기 객체. */
        final org.duckdns.hjow.swingwebpanel.net.ResourceFetcher fetcher = new org.duckdns.hjow.swingwebpanel.net.ResourceFetcher(org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer.INSTANCE,
                org.duckdns.hjow.swingwebpanel.net.FetchLimits.DEFAULT, getClass().getClassLoader());
        /** The parsing loader. 파싱 로더. */
        final ParsingPageLoader parsing = new ParsingPageLoader(loader);
        /** The session. 세션. */
        final BrowserSession session = new BrowserSession(parsing, null, cookies);
        /** The view that shows the page. 페이지를 보여 주는 뷰. */
        final PageView view = new PageView(session, new ViewMetrics(), this, fetcher, cookies);
        /** The pointer input. 포인터 입력. */
        final PageInput input = new PageInput(component, session, view, new HyperlinkDispatcher());
        /** The keyboard input. 키보드 입력. */
        final PageKeyboard keyboard = new PageKeyboard(component, session, view, input);
        /** The overlays. 오버레이. */
        final ControlOverlays overlays = new ControlOverlays(component);
        /** The submissions that the sink received. 싱크가 받은 제출. */
        final List<FormSubmission> submissions = Collections.synchronizedList(new ArrayList<FormSubmission>());
        /** The DOM events that were recorded. 기록한 DOM 이벤트. */
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());
        /** The files that the chooser returns, or null. 선택기가 돌려주는 파일. 없으면 null. */
        volatile File[] chosen;
        /** The number of times the chooser was asked. 선택기에 물은 횟수. */
        volatile int asked;
        /** The color that the color chooser returns, or null. 색 선택기가 돌려주는 색. 없으면 null. */
        volatile Integer pickedColor;

        /**
         * Wires the pieces like the component does.
         * <p>
         * 컴포넌트처럼 부품을 잇는다.
         */
        Fixture() {
            component.setSize(500, 400);
            parsing.bindScripts(session, fetcher, cookies, view);
            component.addMouseListener(input);
            component.addMouseMotionListener(input);
            component.addKeyListener(keyboard);
            component.addFocusListener(keyboard);
            overlays.setKeyboard(keyboard);
            view.setControlOverlays(overlays);
            view.setFormSubmitSink(new FormSubmitSink() {
                public void submit(FormSubmission submission) {
                    submissions.add(submission);
                }
            });
            overlays.setUploadChooser(new UploadChooser() {
                public File[] choose(boolean multiple, String accept) {
                    asked++;
                    return chosen;
                }
            });
            overlays.setColorChooser(new ColorChooser() {
                public Integer choose(int rgb) {
                    return pickedColor;
                }
            });
        }

        /**
         * Publishes a new rendering to the overlays, as the component does.
         * <p>
         * 컴포넌트처럼 새 렌더링을 오버레이에 게시한다.
         */
        public void viewChanged() {
            overlays.layout(view.getRenderedPage());
        }

        /**
         * Loads a page and waits until it is drawn.
         * <p>
         * 페이지를 읽고 그려질 때까지 기다린다.
         *
         * @param html the page / 페이지
         * @throws Exception on failure / 실패하면
         */
        void open(String html) throws Exception {
            view.setViewport(500, 400);
            session.setContent(html, "text/html");
            TestSupport.settle(session);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    view.pageConfirmed(session.getSnapshot());
                }
            });
            TestSupport.settle(session);
            until("the page is rendered", new Condition() {
                public boolean holds() {
                    return view.getRenderedPage() != null;
                }
            });
            TestSupport.flushEdt();
            String[] types = { "input", "change", "select", "invalid", "focusin", "focusout", "submit", "compositionstart", "compositionupdate",
                    "compositionend", "click" };
            onDocument(new Runnable() {
                public void run() {
                    Document d = document();
                    d.setWindowFocused(true);
                    String[] kinds = { "input", "change", "select", "invalid", "focusin", "focusout", "submit", "compositionstart", "compositionupdate",
                            "compositionend", "click" };
                    // 관심 있는 이벤트를 모두 캡처 단계에서 기록한다.
                    for (int i = 0; i < kinds.length; i++) {
                        d.addEventListener(kinds[i], new EventListener() {
                            public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                                events.add(event.getType() + ":" + id(event.getTarget()));
                            }
                        }, true);
                    }
                }
            });
            Check.equal(types.length, 11, "(types)");
        }

        /**
         * Returns the document.
         * <p>
         * 문서를 반환한다.
         *
         * @return the document / 문서
         */
        Document document() {
            return ((HtmlPageDocument) session.getSnapshot().getDocument()).getDocument();
        }

        /**
         * Returns an element by id.
         * <p>
         * id 로 요소를 반환한다.
         *
         * @param id the id / id
         * @return the element / 요소
         */
        Element el(String id) {
            Element e = document().getElementById(id);
            Check.notNull(e, "element #" + id);
            return e;
        }

        /**
         * Runs work on the document thread and waits until it and the work that it caused are done, including the EDT.
         * <p>
         * 작업을 문서 스레드에서 실행하고 그 작업과 그로 인한 작업(EDT 포함)이 끝날 때까지 기다린다.
         *
         * @param work the work / 작업
         * @throws Exception on failure / 실패하면
         */
        void onDocument(Runnable work) throws Exception {
            Check.isTrue(session.submitDocumentTask(work), "the document queue takes the work");
            TestSupport.settle(session);
            TestSupport.flushEdt();
            TestSupport.settle(session);
        }

        /**
         * Gives an element the DOM focus (the window of the document has the focus).
         * <p>
         * 요소에 DOM 포커스를 준다(문서의 창은 포커스를 가진다).
         *
         * @param id the id / id
         * @throws Exception on failure / 실패하면
         */
        void focus(final String id) throws Exception {
            onDocument(new Runnable() {
                public void run() {
                    FocusManager.focus(el(id), true);
                }
            });
        }

        /**
         * Returns the id of an event target, or its tag name.
         * <p>
         * 이벤트 대상의 id 를 반환한다. 없으면 태그 이름.
         *
         * @param t the target / 대상
         * @return the id / id
         */
        String id(org.duckdns.hjow.swingwebpanel.dom.EventTarget t) {
            if (t instanceof Element) {
                String v = ((Element) t).getAttribute("id");
                return v == null || v.length() == 0 ? ((Element) t).getLocalName() : v;
            }
            return String.valueOf(t);
        }

        /**
         * Returns the editor over an element.
         * <p>
         * 요소 위의 편집기를 반환한다.
         *
         * @param id the id / id
         * @return the text component / 텍스트 컴포넌트
         */
        JTextComponent editor(String id) {
            return overlays.getEditor(el(id));
        }

        /**
         * Removes the recorded events that are before now.
         * <p>
         * 지금까지 기록한 이벤트를 지운다.
         */
        void forgetEvents() {
            events.clear();
        }

        /**
         * Counts the recorded events of a kind and target.
         * <p>
         * 종류와 대상이 같은 기록된 이벤트 수를 센다.
         *
         * @param what the event, for example <code>input:name</code> / 이벤트(예: <code>input:name</code>)
         * @return the number / 수
         */
        int count(String what) {
            int n = 0;
            synchronized (events) {
                // 기록된 이벤트를 훑으며 같은 것을 센다.
                for (String e : events) {
                    if (e.equals(what)) {
                        n++;
                    }
                }
            }
            return n;
        }
    }

    /**
     * A condition that a test waits for.
     * <p>
     * 시험이 기다리는 조건.
     */
    interface Condition {
        /**
         * Tells whether the condition holds.
         * <p>
         * 조건이 성립하는지 알려 준다.
         *
         * @return true if it holds / 성립하면 true
         */
        boolean holds();
    }

    /**
     * Waits until a condition holds, with a limit.
     * <p>
     * 조건이 성립할 때까지 한도 안에서 기다린다.
     *
     * @param what      what is awaited / 기다리는 것
     * @param condition the condition / 조건
     * @throws Exception on failure / 실패하면
     */
    static void until(String what, Condition condition) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        // 조건이 성립하거나 시간이 다할 때까지 짧게 쉬며 EDT 를 비운다.
        while (!condition.holds() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            TestSupport.flushEdt();
        }
        Check.isTrue(condition.holds(), "timeout waiting for " + what);
    }

    /**
     * Edits the document of an editor as the user's typing does (on the EDT), and waits until the DOM has the result.
     * <p>
     * 사용자의 입력처럼 편집기의 문서를 (EDT 에서) 편집하고 DOM 이 결과를 가질 때까지 기다린다.
     *
     * @param f    the fixture / 고정 설정
     * @param text the text component / 텍스트 컴포넌트
     * @param at   the offset / 위치
     * @param s    the text to insert / 삽입할 글
     * @throws Exception on failure / 실패하면
     */
    static void type(Fixture f, final JTextComponent text, final int at, final String s) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    text.getDocument().insertString(at, s, null);
                } catch (javax.swing.text.BadLocationException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
    }

    /**
     * Checks that the focus opens an editor over the text field with its text, and that the control is drawn at the place of the editor.
     * <p>
     * 포커스가 텍스트 필드 위에 글이 있는 편집기를 여는지, 컨트롤이 편집기가 있는 자리에 그려지는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testFocusOpensAnEditorOverTheField() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        Check.equal(0, f.overlays.getEditorCount(), "no editor before the focus");
        f.focus("name");
        JTextComponent e = f.editor("name");
        Check.notNull(e, "an editor opens over the focused field");
        Check.equal("abc", e.getText(), "with the value of the field");
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("name"));
        Check.notNull(c, "the control was drawn");
        Rectangle b = e.getParent() == f.component ? e.getBounds() : null;
        Check.notNull(b, "the editor is a child of the page component");
        Check.equal((int) Math.round(c.getX()), b.x, "at the x of the content box");
        Check.equal((int) Math.round(c.getY()), b.y, "at the y of the content box");
        Check.equal((int) Math.round(c.getWidth()), b.width, "with its width");
        Check.equal(c.getFont(), e.getFont(), "and its font");
        Check.equal(1, f.count("focusin:name"), "the focus event was fired once");
        f.focus("cb");
        Check.equal(0, f.overlays.getEditorCount(), "the editor closes when the focus moves to a check box");
        f.focus("ta");
        Check.equal("line1\nline2", f.editor("ta").getText(), "a text area has a multi-line editor");
        f.focus("pw");
        Check.isTrue(f.editor("pw") instanceof javax.swing.JPasswordField, "a password field has a password editor");
        Check.equal("secret", f.editor("pw").getText(), "that holds the real value");
    }

    /**
     * Checks that typing changes the value of the DOM and fires one <code>input</code> event per edit and, at the blur, one
     * <code>change</code> event.
     * <p>
     * 입력이 DOM 의 값을 바꾸고 편집마다 <code>input</code> 이벤트 하나, blur 때 <code>change</code> 이벤트 하나를 발생시키는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testTypingChangesTheValueAndFiresEvents() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("name");
        f.forgetEvents();
        type(f, f.editor("name"), 3, "d");
        Check.equal("abcd", ((HtmlInputElement) f.el("name")).getValue(), "the value follows the typing");
        Check.equal(1, f.count("input:name"), "one input event");
        type(f, f.editor("name"), 4, "e");
        Check.equal("abcde", ((HtmlInputElement) f.el("name")).getValue(), "the value follows the typing");
        Check.equal(2, f.count("input:name"), "two input events");
        Check.equal(0, f.count("change:name"), "no change event while the field has the focus");
        f.focus("cb");
        Check.equal(1, f.count("change:name"), "one change event when the field loses the focus");
        Check.equal(0, f.overlays.getEditorCount(), "and the editor is gone");
        f.focus("name");
        f.focus("cb");
        Check.equal(1, f.count("change:name"), "no change event if the value was not edited");
    }

    /**
     * Checks that the value of a text area with several lines is set from the editor, and the selection of the user goes to the DOM.
     * <p>
     * 여러 줄 텍스트 영역의 값이 편집기에서 정해지고 사용자의 선택이 DOM 으로 가는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testTextAreaAndSelection() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("ta");
        type(f, f.editor("ta"), 5, "X");
        Check.equal("line1X\nline2", ((HtmlTextAreaElement) f.el("ta")).getValue(), "the text area value");
        final JTextComponent text = f.editor("ta");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                text.select(1, 4);
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        HtmlTextAreaElement ta = (HtmlTextAreaElement) f.el("ta");
        Check.equal(1, ta.getSelectionStart(), "the selection start of the DOM");
        Check.equal(4, ta.getSelectionEnd(), "the selection end of the DOM");
        Check.equal(1, f.count("select:ta"), "a select event for a selection of text");
    }

    /**
     * Checks that a change made by script reaches the editor, and the selection that script sets too.
     * <p>
     * 스크립트가 한 변경이 편집기에 전달되는지, 스크립트가 지정한 선택도 전달되는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testScriptChangesReachTheEditor() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("name");
        f.onDocument(new Runnable() {
            public void run() {
                ((HtmlInputElement) f.el("name")).setValue("changed by script");
            }
        });
        Check.equal("changed by script", f.editor("name").getText(), "the editor shows the new value");
        f.onDocument(new Runnable() {
            public void run() {
                ((HtmlInputElement) f.el("name")).setSelectionRange(2, 5, "forward");
            }
        });
        Check.equal(2, f.editor("name").getSelectionStart(), "the editor selection start follows script");
        Check.equal(5, f.editor("name").getSelectionEnd(), "and the end");
        f.onDocument(new Runnable() {
            public void run() {
                f.el("name").setAttribute("readonly", "");
            }
        });
        Check.isFalse(f.editor("name").isEditable(), "a field that became read-only cannot be edited");
        f.onDocument(new Runnable() {
            public void run() {
                f.el("name").removeAttribute("readonly");
                f.el("name").setAttribute("maxlength", "3");
                ((HtmlInputElement) f.el("name")).setValue("xyz");
            }
        });
        Check.isTrue(f.editor("name").isEditable(), "and editable again");
    }

    /**
     * Checks that the value that the user typed is not replaced by the DOM while the editor and the DOM agree.
     * <p>
     * 편집기와 DOM 이 일치하는 동안 사용자가 입력한 값이 DOM 의 값으로 덮어써지지 않는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testRapidTypingIsNotOverwritten() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("name");
        final JTextComponent e = f.editor("name");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    // 편집기에서 연달아 입력한다(DOM 이 따라잡기 전에 다음 입력이 들어온다).
                    for (int i = 0; i < 20; i++) {
                        e.getDocument().insertString(e.getDocument().getLength(), "" + (char) ('a' + i), null);
                    }
                } catch (javax.swing.text.BadLocationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        Check.equal("abcabcdefghijklmnopqrst", e.getText(), "the editor keeps all the characters");
        Check.equal("abcabcdefghijklmnopqrst", ((HtmlInputElement) f.el("name")).getValue(), "and the DOM has them all");
    }

    /**
     * Checks the input method path: composed text that the input method puts into the editor reaches the DOM value, the composition events
     * of the DOM are fired in order, and the committed text stays. An input method event is made by hand here (a real Korean input method
     * needs a keyboard and a screen; that is not tested).
     * <p>
     * 입력기 경로를 확인한다: 입력기가 편집기에 넣은 조합 중인 글이 DOM 값에 닿고, DOM 의 조합 이벤트가 차례로 발생하며, 확정된 글이 남는다. 입력기 이벤트는 여기서
     * 직접 만든다(실제 한글 입력기는 키보드와 화면이 필요하므로 시험하지 않는다).
     *
     * @throws Exception on failure / 실패하면
     */
    public void testInputMethodComposition() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("name");
        f.forgetEvents();
        final JTextComponent e = f.editor("name");
        final String hangul = "" + (char) 0xD55C;
        final java.awt.Component source = e;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                java.text.AttributedString composed = new java.text.AttributedString(hangul);
                e.setCaretPosition(3);
                e.dispatchEvent(new java.awt.event.InputMethodEvent(source, java.awt.event.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                        composed.getIterator(), 0, null, null));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        Check.equal("abc" + hangul, ((HtmlInputElement) f.el("name")).getValue(), "the composed text is in the value while it is composed");
        Check.equal(1, f.count("compositionstart:name"), "compositionstart was fired once");
        Check.isTrue(f.count("compositionupdate:name") >= 1, "compositionupdate was fired");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                java.text.AttributedString done = new java.text.AttributedString(hangul);
                e.dispatchEvent(new java.awt.event.InputMethodEvent(source, java.awt.event.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                        done.getIterator(), 1, null, null));
            }
        });
        TestSupport.flushEdt();
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        Check.equal(1, f.count("compositionend:name"), "compositionend was fired");
        Check.equal("abc" + hangul, ((HtmlInputElement) f.el("name")).getValue(), "the committed text stays in the value");
    }

    /**
     * Checks maxlength, a read-only field and a field that became disabled.
     * <p>
     * maxlength, 읽기 전용 필드, 비활성이 된 필드를 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testMaxLengthAndReadOnly() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("ml");
        type(f, f.editor("ml"), 0, "12345678");
        Check.equal("12345", f.editor("ml").getText(), "the editor takes at most maxlength characters");
        Check.equal("12345", ((HtmlInputElement) f.el("ml")).getValue(), "and so does the DOM");
        f.focus("ro");
        Check.isFalse(f.editor("ro").isEditable(), "a read-only field has a read-only editor");
        Check.equal("fixed", f.editor("ro").getText(), "that shows the value");
    }

    /**
     * Checks that the Enter key in a text field fires <code>change</code> and submits the form with the values of the fields.
     * <p>
     * 텍스트 필드에서 Enter 키가 <code>change</code> 를 발생시키고 폼을 필드 값과 함께 제출하는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testEnterSubmitsTheForm() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE.replace(" required", ""));
        f.focus("name");
        type(f, f.editor("name"), 3, "Z");
        f.forgetEvents();
        final JTextComponent e = f.editor("name");
        // 편집기에 달린 키 리스너(페이지 키보드)에 Enter 를 보낸다. AWT 는 포커스가 없는 컴포넌트의 키 이벤트를 돌려보내므로 리스너를 직접 부른다.
        final KeyEvent press = new KeyEvent(e, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                java.awt.event.KeyListener[] listeners = e.getKeyListeners();
                // 편집기의 모든 키 리스너에 눌림 이벤트를 전달한다.
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].keyPressed(press);
                }
            }
        });
        TestSupport.settle(f.session);
        TestSupport.flushEdt();
        Check.equal(1, f.count("change:name"), "the Enter key commits the value");
        Check.equal(1, f.count("submit:f"), "and submits the form");
        Check.equal(1, f.submissions.size(), "the sink received one submission");
        FormSubmission s = f.submissions.get(0);
        Check.equal("GET", s.getMethod(), "the method");
        Check.contains(s.getUrl(), "name=abcZ", "the URL has the value that the user typed");
        Check.contains(s.getUrl(), "pw=secret", "and the other fields");
        Check.isFalse(s.getUrl().indexOf("cb=") >= 0, "and not the unchecked check box");
    }

    /**
     * Checks that a form with an invalid field is not submitted and that the message of the field is shown in a bubble.
     * <p>
     * 올바르지 않은 필드가 있는 폼이 제출되지 않고 그 필드의 메시지가 말풍선에 보이는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testInvalidFieldShowsABubble() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.onDocument(new Runnable() {
            public void run() {
                ((HtmlFormElement) f.el("f")).requestSubmit(f.el("ok"));
            }
        });
        Check.equal(0, f.submissions.size(), "an invalid form is not submitted");
        Check.equal(1, f.count("invalid:req"), "the invalid event of the field");
        Check.notNull(f.overlays.getMessage(), "a message is shown");
        Check.isTrue(f.overlays.getMessage().length() > 0, "that is not empty");
        Check.notNull(f.editor("req"), "the invalid field has the focus and its editor");
        type(f, f.editor("req"), 0, "x");
        Check.isNull(f.overlays.getMessage(), "typing hides the message");
    }

    /**
     * Checks that a click on a file upload control asks the chooser, that the chosen file is read into the control and that a cancelled
     * dialog gives a <code>cancel</code> event.
     * <p>
     * 파일 업로드 컨트롤을 누르면 선택기에 묻고, 고른 파일이 컨트롤에 읽히고, 취소하면 <code>cancel</code> 이벤트가 발생하는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testFileChooser() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        File temp = File.createTempFile("form-b", ".txt");
        temp.deleteOnExit();
        FileOutputStream out = new FileOutputStream(temp);
        try {
            out.write("hello file".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        f.chosen = null;
        f.onDocument(new Runnable() {
            public void run() {
                f.el("file").dispatchTrustedEvent(new org.duckdns.hjow.swingwebpanel.dom.MouseEvent("click", true, true));
            }
        });
        Check.equal(1, f.asked, "the chooser was asked");
        Check.equal(0, ((HtmlInputElement) f.el("file")).getSelectedFiles().size(), "nothing was chosen");
        f.chosen = new File[] { temp };
        f.onDocument(new Runnable() {
            public void run() {
                f.el("file").dispatchTrustedEvent(new org.duckdns.hjow.swingwebpanel.dom.MouseEvent("click", true, true));
            }
        });
        Check.equal(2, f.asked, "the chooser was asked again");
        HtmlInputElement file = (HtmlInputElement) f.el("file");
        Check.equal(1, file.getSelectedFiles().size(), "one file was chosen");
        Check.equal(temp.getName(), file.getSelectedFiles().get(0).getName(), "with its name");
        Check.equal(10, file.getSelectedFiles().get(0).getSize(), "and its size");
        Check.equal(1, f.count("change:file"), "a change event");
        RenderedPage page = f.view.getRenderedPage();
        until("the file name is drawn", new Condition() {
            public boolean holds() {
                ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("file"));
                return c != null && c.getFileText().equals(f.el("file") == null ? "" : ((HtmlInputElement) f.el("file")).getSelectedFiles().get(0).getName());
            }
        });
        Check.notNull(page, "the page");
    }

    /**
     * Checks that a click on a check box with the mouse toggles it and that the drawing follows the new state.
     * <p>
     * 마우스로 체크박스를 누르면 토글되고 그림이 새 상태를 따르는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testClickTogglesTheCheckBoxAndRedraws() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        ControlSnapshot c = f.view.getRenderedPage().getControl(f.el("cb"));
        Check.isFalse(c.isChecked(), "unchecked at first");
        final int x = (int) (c.getX() + c.getWidth() / 2);
        final int y = (int) (c.getY() + c.getHeight() / 2);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.input.mouseClicked(new MouseEvent(f.component, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false,
                        MouseEvent.BUTTON1));
            }
        });
        TestSupport.settle(f.session);
        Check.isTrue(((HtmlInputElement) f.el("cb")).isChecked(), "the check box is checked");
        until("the new state is drawn", new Condition() {
            public boolean holds() {
                RenderedPage p = f.view.getRenderedPage();
                return p != null && p.getControl(f.el("cb")).isChecked();
            }
        });
        Check.equal(1, f.count("click:cb"), "one click event (the click is not handled twice)");
    }

    /**
     * Checks that the editor moves with the field when the page is laid out again, and that it is removed when the page is replaced.
     * <p>
     * 페이지를 다시 배치하면 편집기가 필드와 함께 움직이고, 페이지가 교체되면 편집기가 없어지는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testEditorFollowsTheLayoutAndIsRemovedWithThePage() throws Exception {
        final Fixture f = new Fixture();
        f.open(PAGE);
        f.focus("name");
        final int y0 = f.editor("name").getParent().getBounds().y + f.editor("name").getY();
        f.onDocument(new Runnable() {
            public void run() {
                f.el("f").setAttribute("style", "margin-top:50px");
            }
        });
        until("the editor moved", new Condition() {
            public boolean holds() {
                return f.editor("name").getY() >= y0 + 49;
            }
        });
        f.open("<!doctype html><p>another page</p>");
        until("the editor is removed", new Condition() {
            public boolean holds() {
                return f.overlays.getEditorCount() == 0;
            }
        });
        Check.equal(0, f.component.getComponentCount(), "the page component has no children any more");
    }

    /**
     * Checks that a control that has the focus before it was drawn gets its editor after the first rendering (script focus at load time).
     * <p>
     * 그려지기 전에 포커스를 가진 컨트롤이 첫 렌더링 뒤에 편집기를 얻는지 확인한다(로드 때 스크립트의 포커스).
     *
     * @throws Exception on failure / 실패하면
     */
    public void testAutofocusGetsAnEditorAfterTheFirstRendering() throws Exception {
        final Fixture f = new Fixture();
        f.view.setViewport(500, 400);
        f.session.setContent("<!doctype html><input id=a value=hi><script>document.getElementById('a').focus()</script>", "text/html");
        TestSupport.settle(f.session);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.view.pageConfirmed(f.session.getSnapshot());
            }
        });
        TestSupport.settle(f.session);
        f.onDocument(new Runnable() {
            public void run() {
                f.document().setWindowFocused(true);
                org.duckdns.hjow.swingwebpanel.dom.FocusManager.windowFocusChanged(f.document(), true);
            }
        });

        until("the editor opens", new Condition() {
            public boolean holds() {
                return f.overlays.getEditor(f.el("a")) != null;
            }
        });
        Check.equal("hi", f.overlays.getEditor(f.el("a")).getText(), "with the value");
    }
}
