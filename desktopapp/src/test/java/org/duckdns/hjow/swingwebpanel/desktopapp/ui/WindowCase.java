package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.FakePrinting;
import org.duckdns.hjow.swingwebpanel.desktopapp.FakePrompts;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The common part of the window tests: an application with one browser window that is not shown, pages to load, prompts that answer
 * without a dialog, a directory for the user data of the case, and helpers for working on the event dispatch thread. The cases need a
 * frame and are skipped in a headless JVM.
 * <p>
 * 창 시험의 공통 부분: 보이지 않는 브라우저 창 하나가 있는 응용 프로그램, 읽을 페이지, 대화 상자 없이 답하는 질문 수단, 사례의 사용자 데이터를 둘
 * 디렉터리, 이벤트 디스패치 스레드에서 일하기 위한 도우미. 사례는 프레임이 필요하므로 headless JVM 에서는 건너뛴다.
 */
public abstract class WindowCase {

    /** The pages of the case. / 사례의 페이지. */
    protected TestPages pages;

    /** The application of the case. / 사례의 응용 프로그램. */
    protected BrowserApp app;

    /** The window of the case. / 사례의 창. */
    protected BrowserWindow window;

    /** The number of times that the program ended. / 프로그램이 끝난 횟수. */
    protected int ended;

    /** The print system of the application of the case: it never reaches a printer. / 사례의 응용 프로그램의 인쇄 시스템: 프린터에 닿지 않는다. */
    protected final FakePrinting printing = new FakePrinting();

    /** Answers the questions of the application. / 응용 프로그램의 질문에 답한다. */
    protected final FakePrompts prompts = new FakePrompts();

    /** The directory of the user data of the case. / 사례의 사용자 데이터 디렉터리. */
    protected File home;

    /** The value of the user data property before the case. / 사례 전의 사용자 데이터 속성 값. */
    private String oldHome;

    /**
     * Creates the common part.
     * <p>
     * 공통 부분을 만든다.
     */
    protected WindowCase() {
    }

    /**
     * Creates the pages and, with a display, a window that is not shown.
     * <p>
     * 페이지를 만들고, 화면이 있으면 보이지 않는 창을 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        System.setProperty(Texts.LANGUAGE_PROPERTY, "en");
        pages = new TestPages();
        home = pages.file("home");
        oldHome = System.getProperty(BrowserApp.HOME_PROPERTY);
        System.setProperty(BrowserApp.HOME_PROPERTY, home.getPath());
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the application and its window. / 응용 프로그램과 창을 만든다. */
            public void run() {
                app = new BrowserApp(new Runnable() {
                    /** Counts the end of the program. / 프로그램 종료를 센다. */
                    public void run() {
                        ended++;
                    }
                });
                app.setPrompts(prompts);
                // 어떤 시험도 실제 프린터에 닿지 않도록 인쇄 시스템을 가짜로 바꾼다.
                app.setPrinting(printing);
                window = app.openWindow(null);
            }
        });
    }

    /**
     * Closes all windows and removes the pages and the user data.
     * <p>
     * 모든 창을 닫고 페이지와 사용자 데이터를 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void tearDown() throws Exception {
        if (app != null) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Closes the windows. / 창을 닫는다. */
                public void run() {
                    // 사례가 연 창을 모두 닫는다.
                    for (BrowserWindow w : app.getWindows()) {
                        w.closeWindow();
                    }
                }
            });
        }
        deleteTree(home);
        pages.close();
        if (oldHome == null) {
            System.clearProperty(BrowserApp.HOME_PROPERTY);
        } else {
            System.setProperty(BrowserApp.HOME_PROPERTY, oldHome);
        }
        System.clearProperty(Texts.LANGUAGE_PROPERTY);
    }

    /**
     * Deletes a file or a directory with everything in it.
     * <p>
     * 파일, 또는 디렉터리와 그 안의 모든 것을 지운다.
     *
     * @param f the file or directory / 파일 또는 디렉터리
     */
    protected static void deleteTree(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        File[] children = f.listFiles();
        // 디렉터리이면 안의 것부터 지운다.
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                deleteTree(children[i]);
            }
        }
        f.delete();
    }

    /**
     * Tells whether the case must be skipped because the JVM has no display.
     * <p>
     * JVM 에 화면이 없어 사례를 건너뛰어야 하는지 알려 준다.
     *
     * @return true if skipped / 건너뛰면 true
     */
    protected boolean skipped() {
        if (window == null) {
            Check.skip("a browser window needs a display");
            return true;
        }
        return false;
    }

    /**
     * Runs work on the event dispatch thread and reports a failed check of the work as it is.
     * <p>
     * 이벤트 디스패치 스레드에서 일을 실행하고, 그 일의 실패한 확인을 그대로 보고한다.
     *
     * @param work the work / 일
     * @throws Exception on failure / 실패 시
     */
    protected static void onEdt(Runnable work) throws Exception {
        try {
            SwingUtilities.invokeAndWait(work);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // 이벤트 디스패치 스레드에서 실패한 확인은 그 실패 그대로 보고한다.
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Waits until a tab of the window of the case shows a page with the given title and is not loading any more.
     * <p>
     * 사례의 창의 탭이 주어진 제목의 페이지를 보이고 더 이상 읽는 중이 아닐 때까지 기다린다.
     *
     * @param index the position of the tab / 탭의 위치
     * @param title the title of the page / 페이지의 제목
     * @throws Exception on failure / 실패 시
     */
    protected void awaitTab(int index, String title) throws Exception {
        awaitTab(window, index, title);
    }

    /**
     * Waits until a tab of a window shows a page with the given title and is not loading any more.
     * <p>
     * 창의 탭이 주어진 제목의 페이지를 보이고 더 이상 읽는 중이 아닐 때까지 기다린다.
     *
     * @param w     the window / 창
     * @param index the position of the tab / 탭의 위치
     * @param title the title of the page / 페이지의 제목
     * @throws Exception on failure / 실패 시
     */
    protected static void awaitTab(final BrowserWindow w, final int index, final String title) throws Exception {
        boolean ok = TestPages.await(new TestPages.Condition() {
            /** Looks at the title and the progress of the tab. / 탭의 제목과 진행률을 본다. */
            public boolean holds() {
                return index < w.getTabs().size() && title.equals(w.getTabs().get(index).getPanel().getTitle())
                        && !w.getTabs().get(index).isLoading();
            }
        });
        final String[] shown = new String[1];
        onEdt(new Runnable() {
            /** Reads the title for the failure message. / 실패 메시지에 쓸 제목을 읽는다. */
            public void run() {
                shown[0] = index >= w.getTabs().size() ? "(no such tab)" : w.getTabs().get(index).getPanel().getTitle() + "' at "
                        + w.getTabs().get(index).getPanel().getPage() + " (progress " + w.getTabs().get(index).getPanel().getProgress() + ", status '"
                        + w.getTabs().get(index).getPanel().getStatusText() + "')";
            }
        });
        Check.isTrue(ok, "expected the page '" + title + "' in tab " + index + " but it shows '" + shown[0]);
    }

    /**
     * Waits for a condition and fails with a message if it does not hold in time.
     * <p>
     * 조건을 기다리고, 제때 성립하지 않으면 메시지와 함께 실패한다.
     *
     * @param condition the condition / 조건
     * @param message   what was expected / 기대한 것
     * @throws Exception on failure / 실패 시
     */
    protected static void await(TestPages.Condition condition, String message) throws Exception {
        Check.isTrue(TestPages.await(condition), message);
    }

    /**
     * Shows the window of the case far off the screen and without taking the keyboard focus; accelerator keys only work in a window
     * that is showing.
     * <p>
     * 사례의 창을 화면에서 멀리 벗어난 곳에, 키보드 포커스를 받지 않게 보인다. 가속 키는 보이는 창에서만 동작한다.
     */
    protected void showOffScreen() {
        // 포커스를 받지 않는 상태로 보여서 사용자가 쓰고 있는 창의 포커스를 빼앗지 않는다.
        window.setFocusableWindowState(false);
        window.setLocation(-4000, -4000);
        window.setVisible(true);
        // Swing 은 포커스를 받을 수 있는 창에만 창 전체의 키 바인딩을 등록한다. 보인 뒤에 상태를 되돌리고 등록을 다시 하게 한다.
        window.setFocusableWindowState(true);
        JMenuBar bar = window.getJMenuBar();
        window.setJMenuBar(null);
        window.setJMenuBar(bar);
        InputMap keys = window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke[] all = keys.keys();
        if (all != null && all.length > 0) {
            keys.put(all[0], keys.get(all[0]));
        }
    }

    /**
     * Presses a key as the keyboard does for a component that has the focus: first the key listeners of the component see the key, then,
     * unless one of them consumed it, the key bindings up to the menu bar of the window. Call it on the event dispatch thread.
     * <p>
     * 포커스를 가진 컴포넌트에 키보드가 하듯이 키를 누른다: 먼저 컴포넌트의 키 리스너가 키를 보고, 그중 누구도 소비하지 않았으면 창의 메뉴 막대까지의
     * 키 바인딩이 본다. 이벤트 디스패치 스레드에서 부른다.
     *
     * @param target    the component that has the focus / 포커스를 가진 컴포넌트
     * @param keyCode   the key code / 키 코드
     * @param modifiers the modifier mask / 보조 키 마스크
     * @return true if a key listener of the component consumed the key / 컴포넌트의 키 리스너가 키를 소비했으면 true
     */
    protected static boolean press(Component target, int keyCode, int modifiers) {
        KeyEvent e = new KeyEvent(target, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
        KeyListener[] listeners = target.getKeyListeners();
        // 컴포넌트의 키 리스너가 먼저 키를 본다.
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].keyPressed(e);
        }
        if (e.isConsumed()) {
            return true;
        }
        SwingUtilities.processKeyBindings(e);
        return false;
    }
}
