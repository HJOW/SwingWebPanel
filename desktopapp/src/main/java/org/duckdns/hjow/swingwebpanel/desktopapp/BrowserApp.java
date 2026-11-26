package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.BrowserWindow;

/**
 * The state of the whole browser program: the settings, the open browser windows and what happens when the last one is closed. One
 * instance exists per process. Create it and call its methods on the event dispatch thread.
 * <p>
 * 브라우저 프로그램 전체의 상태: 설정, 열린 브라우저 창들, 마지막 창이 닫힐 때 일어나는 일. 프로세스에 인스턴스가 하나 있다. 이벤트 디스패치
 * 스레드에서 만들고 그 메서드를 부른다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * BrowserApp app = new BrowserApp(null);          // null: end the JVM when the last window closes
 * app.openWindow("https://example.com/").setVisible(true);
 * </pre>
 */
public final class BrowserApp {

    /** The name of the application, shown in window titles. / 창 제목에 보이는 응용 프로그램의 이름. */
    public static final String NAME = "SwingWebPanel Browser";

    /** The address of the empty page. / 빈 페이지의 주소. */
    public static final String BLANK = "about:blank";

    /** The settings shared by all windows. / 모든 창이 공유하는 설정. */
    private final Settings settings = new Settings();

    /** The open windows in the order they were opened. / 열린 순서대로의 창. */
    private final List<BrowserWindow> windows = new ArrayList<BrowserWindow>();

    /** Runs when the last window was closed. / 마지막 창이 닫혔을 때 실행된다. */
    private final Runnable afterLastWindow;

    /**
     * Creates the application state.
     * <p>
     * 응용 프로그램의 상태를 만든다.
     *
     * @param afterLastWindow what to do when the last window was closed, or null to end the program with <code>System.exit(0)</code>
     *                        (tests pass their own action) / 마지막 창이 닫혔을 때 할 일. null 이면 <code>System.exit(0)</code> 으로 프로그램을
     *                        끝낸다(시험은 자기 동작을 넘긴다)
     */
    public BrowserApp(Runnable afterLastWindow) {
        this.afterLastWindow = afterLastWindow != null ? afterLastWindow : new Runnable() {
            /** Ends the program. / 프로그램을 끝낸다. */
            public void run() {
                System.exit(0);
            }
        };
    }

    /**
     * Returns the settings of the application.
     * <p>
     * 응용 프로그램의 설정을 반환한다.
     *
     * @return the settings shared by all windows / 모든 창이 공유하는 설정
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Opens a new browser window that shows an address. The window is created but not shown; the caller makes it visible.
     * <p>
     * 주소를 보여 주는 새 브라우저 창을 연다. 창은 만들기만 하고 보이게 하지는 않는다. 보이게 하는 것은 호출자이다.
     *
     * @param url the address to show, or null for the empty page / 보여 줄 주소. 빈 페이지이면 null
     * @return the new window / 새 창
     */
    public BrowserWindow openWindow(String url) {
        BrowserWindow window = new BrowserWindow(this);
        windows.add(window);
        // 빈 페이지는 컴포넌트의 처음 상태이므로 다시 읽지 않는다.
        if (url != null && !BLANK.equals(url)) {
            window.getActiveTab().open(url);
        }
        return window;
    }

    /**
     * Returns the open windows.
     * <p>
     * 열린 창들을 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<BrowserWindow> getWindows() {
        return new ArrayList<BrowserWindow>(windows);
    }

    /**
     * Forgets a window that was closed; called by the window itself. When it was the last one, the action for the end of the program runs.
     * <p>
     * 닫힌 창을 목록에서 뺀다. 창 자신이 부른다. 마지막 창이었으면 프로그램 종료 동작이 실행된다.
     *
     * @param window the closed window / 닫힌 창
     */
    public void windowClosed(BrowserWindow window) {
        // 목록에 있던 창이 마지막 창이었을 때만 종료 동작을 실행한다.
        if (windows.remove(window) && windows.isEmpty()) {
            afterLastWindow.run();
        }
    }
}
