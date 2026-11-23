package org.duckdns.hjow.swingwebpanel;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Integration test of the real component with a form that it loads over HTTP (plan items P11-05, P11-06, P11-07, P21): the form is drawn, the
 * text field that has the focus gets a Swing editor over it, what the user types is what the script of the page reads, the Enter key submits
 * the form to the overridable method of the component, the file chooser is the overridable method too, and cleaning and disposing the component
 * remove the editors. The AWT focus needs a screen; the test gives the focus to the page by calling its focus listeners, as AWT does.
 * <p>
 * HTTP 로 읽은 폼을 가진 실제 컴포넌트의 통합 시험(계획 P11-05, P11-06, P11-07, P21): 폼이 그려지고, 포커스를 가진 텍스트 필드 위에 Swing 편집기가 생기고, 사용자가
 * 입력한 것을 페이지의 스크립트가 읽고, Enter 키가 폼을 컴포넌트의 재정의할 수 있는 메서드로 제출하고, 파일 선택기도 재정의할 수 있는 메서드이며, 컴포넌트를 clean
 * 하거나 dispose 하면 편집기가 없어진다. AWT 포커스는 화면이 필요하므로, 시험은 AWT 가 하듯이 페이지의 포커스 리스너를 불러 포커스를 준다.
 */
public class FormComponentIntegrationTest {

    /** The page of the test. 시험의 페이지. */
    private static final String PAGE = "<!doctype html><html><head><style>html,body{margin:0} input{font:12px monospace;display:block;margin:0}</style></head><body>"
            + "<form id=f action='/go' method=get><input id=name name=name value='abc'><input id=pick type=file name=pick><input type=submit value=Send></form>"
            + "<script>var n=document.getElementById('name');"
            + "n.addEventListener('input',function(){localStorage.setItem('typed',n.value);});"
            + "n.addEventListener('change',function(){localStorage.setItem('changed',n.value);});"
            + "document.getElementById('f').addEventListener('submit',function(){localStorage.setItem('submitted',n.value);});"
            + "document.getElementById('pick').addEventListener('change',function(e){localStorage.setItem('picked',String(e.target.files.length)+':'+e.target.files[0].name);});"
            + "window.addEventListener('load',function(){n.focus();});</script></body></html>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormComponentIntegrationTest() {
    }

    /**
     * The component with the overridable methods that the test observes.
     * <p>
     * 시험이 관찰하는, 재정의할 수 있는 메서드를 가진 컴포넌트.
     */
    private static final class Panel extends SwingWebPanel {

        /** The serial version id. 직렬화 버전 식별자. */
        private static final long serialVersionUID = 1L;

        /** The submissions that were received. 받은 제출. */
        final List<FormSubmission> submissions = Collections.synchronizedList(new ArrayList<FormSubmission>());

        /** The files that the chooser returns, or null. 선택기가 돌려주는 파일. 없으면 null. */
        volatile File[] chosen;

        /**
         * Creates the component.
         * <p>
         * 컴포넌트를 만든다.
         */
        Panel() {
        }

        /**
         * Records the submission instead of navigating.
         * <p>
         * 이동하는 대신 제출을 기록한다.
         *
         * @param submission the submission / 제출
         */
        protected void submitForm(FormSubmission submission) {
            submissions.add(submission);
        }

        /**
         * Returns the files that the test chose.
         * <p>
         * 시험이 정한 파일을 반환한다.
         *
         * @param multiple true if several files are accepted / 여러 파일을 받으면 true
         * @param accept   the accept attribute / accept 속성
         * @return the files or null / 파일 또는 null
         */
        protected File[] chooseUploadFiles(boolean multiple, String accept) {
            return chosen;
        }
    }

    /**
     * Returns the value that the page stored under a key of its local storage, or null.
     * <p>
     * 페이지가 local storage 의 키에 저장한 값을 반환한다. 없으면 null.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String stored(Panel panel, String key) {
        // 저장된 항목을 훑어 키가 같은 것의 값을 찾는다.
        for (StorageEntry e : panel.getLocalStorage()) {
            if (e.getKey().equals(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until a condition holds.
     * <p>
     * 조건이 성립할 때까지 기다린다.
     *
     * @param what      what is awaited / 기다리는 것
     * @param condition the condition / 조건
     * @throws Exception on failure / 실패하면
     */
    private static void until(String what, java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        // 조건이 성립하거나 시간이 다할 때까지 짧게 쉬며 EDT 를 비운다.
        while (!condition.call().booleanValue() && System.currentTimeMillis() < end) {
            Thread.sleep(20);
            TestSupport.flushEdt();
        }
        Check.isTrue(condition.call().booleanValue(), "timeout waiting for " + what);
    }

    /**
     * Gives the page the keyboard focus as AWT does when the user clicks the window: the focus listeners of the component are called.
     * <p>
     * 사용자가 창을 누를 때 AWT 가 하듯이 페이지에 키보드 포커스를 준다: 컴포넌트의 포커스 리스너를 부른다.
     *
     * @param panel the component / 컴포넌트
     * @throws Exception on failure / 실패하면
     */
    private static void focusPage(final Panel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                FocusEvent e = new FocusEvent(panel, FocusEvent.FOCUS_GAINED);
                FocusListener[] l = panel.getFocusListeners();
                // 컴포넌트에 달린 포커스 리스너(페이지 키보드)에 포커스 획득을 알린다.
                for (int i = 0; i < l.length; i++) {
                    l[i].focusGained(e);
                }
            }
        });
    }

    /**
     * Loads the page, gives it the focus and waits for the editor over the text field.
     * <p>
     * 페이지를 읽고 포커스를 주고 텍스트 필드 위의 편집기를 기다린다.
     *
     * @param server the server that has the page / 페이지를 가진 서버
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패하면
     */
    private static Panel open(TestHttpServer server) throws Exception {
        final Panel[] holder = new Panel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                holder[0] = new Panel();
                holder[0].setSize(400, 300);
            }
        });
        final Panel panel = holder[0];
        panel.setPage(server.url("/form"));
        until("the page", new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                return Boolean.valueOf(panel.getProgress() == 100 && panel.getPage().endsWith("/form"));
            }
        });
        focusPage(panel);
        until("the editor of the focused field", new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                return Boolean.valueOf(panel.getComponentCount() == 1);
            }
        });
        return panel;
    }

    /**
     * Checks the whole way: the editor, the typing that the script reads, Enter that submits, the file chooser and the cleaning.
     * <p>
     * 전체 흐름을 확인한다: 편집기, 스크립트가 읽는 입력, 제출하는 Enter, 파일 선택기, 정리.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testFormInTheComponent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        server.routeFixed("/form", TestHttpServer.Response.html(PAGE));
        server.start();
        try {
            final Panel panel = open(server);
            final Component child = panel.getComponent(0);
            Check.isTrue(child instanceof JTextField, "the editor of the text field is a Swing text field");
            final JTextField editor = (JTextField) child;
            Check.equal("abc", editor.getText(), "with the value of the field");
            Check.isTrue(editor.getWidth() > 20 && editor.getHeight() > 8, "over the box of the field");
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        editor.getDocument().insertString(3, "Z", null);
                    } catch (javax.swing.text.BadLocationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
            until("the script reads the typed value", new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() {
                    return Boolean.valueOf("abcZ".equals(stored(panel, "typed")));
                }
            });
            // Enter 를 편집기의 키 리스너(페이지 키보드)에 보낸다.
            final KeyEvent enter = new KeyEvent(editor, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n');
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    KeyListener[] l = editor.getKeyListeners();
                    // 편집기의 키 리스너마다 Enter 눌림을 전달한다.
                    for (int i = 0; i < l.length; i++) {
                        l[i].keyPressed(enter);
                    }
                }
            });
            until("the form is submitted", new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() {
                    return Boolean.valueOf(panel.submissions.size() == 1);
                }
            });
            Check.equal("abcZ", stored(panel, "changed"), "the Enter key commits the value (change)");
            Check.equal("abcZ", stored(panel, "submitted"), "the submit event of the script saw the value");
            FormSubmission s = panel.submissions.get(0);
            Check.contains(s.getUrl(), server.url("/go") + "?name=abcZ", "the submission goes to the action with the typed value");
            panel.clean();
            TestSupport.flushEdt();
            Check.equal(0, panel.getComponentCount(), "clean removes the editors");
            panel.dispose();
            TestSupport.flushEdt();
            Check.equal(0, panel.getComponentCount(), "and dispose too");
        } finally {
            server.stop();
        }
    }

    /**
     * Checks that a page that is replaced takes its editor away, and that the file chooser method of the component decides the files.
     * <p>
     * 페이지가 교체되면 편집기가 없어지는지, 그리고 컴포넌트의 파일 선택기 메서드가 파일을 정하는지 확인한다.
     *
     * @throws Exception on failure / 실패하면
     */
    public void testPageReplacementAndFileChooser() throws Exception {
        TestHttpServer server = new TestHttpServer();
        server.routeFixed("/form", TestHttpServer.Response.html(PAGE));
        server.routeFixed("/other", TestHttpServer.Response.html("<!doctype html><p>other</p>"));
        server.start();
        try {
            final Panel panel = open(server);
            final File temp = File.createTempFile("form-b-component", ".txt");
            temp.deleteOnExit();
            java.io.FileOutputStream out = new java.io.FileOutputStream(temp);
            try {
                out.write("data".getBytes("UTF-8"));
            } finally {
                out.close();
            }
            panel.chosen = new File[] { temp };
            // 파일 컨트롤은 이름 필드 바로 아래에 있다: 이름 필드의 편집기 아래쪽에서 조금 내려간 곳을 누른다.
            final JTextField editor = (JTextField) panel.getComponent(0);
            final int y = editor.getY() + editor.getHeight() + 3 + 8;
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    panel.dispatchEvent(new java.awt.event.MouseEvent(panel, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, y, 1,
                            false, java.awt.event.MouseEvent.BUTTON1));
                }
            });
            until("the chosen file is read by the page", new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() {
                    return Boolean.valueOf(("1:" + temp.getName()).equals(stored(panel, "picked")));
                }
            });
            panel.setPage(server.url("/other"));
            until("the other page", new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() {
                    return Boolean.valueOf(panel.getPage().endsWith("/other") && panel.getProgress() == 100);
                }
            });
            TestSupport.flushEdt();
            Check.equal(0, panel.getComponentCount(), "the editor went away with the page");
            panel.dispose();
        } finally {
            server.stop();
        }
    }
}
