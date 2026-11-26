package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;

/**
 * A browser window (plan item P31-03): the navigation toolbar with the address bar at the top, the page in the middle and the status bar
 * at the bottom. The title bar shows the title of the page (its address when it has none) followed by the name of the application.
 * Closing the window disposes its page and tells the application. Use it on the event dispatch thread.
 * <p>
 * 브라우저 창(계획 P31-03): 위에 주소 표시줄이 있는 탐색 도구 모음, 가운데에 페이지, 아래에 상태 표시줄이 있다. 제목 표시줄에는 페이지의 제목(없으면
 * 주소)과 그 뒤에 응용 프로그램의 이름을 보인다. 창을 닫으면 페이지를 폐기하고 응용 프로그램에 알린다. 이벤트 디스패치 스레드에서 쓴다.
 */
public final class BrowserWindow extends JFrame implements NavigationBar.Actions, BrowserTab.Listener {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The application that owns the window. / 창을 소유한 응용 프로그램. */
    private final transient BrowserApp app;

    /** The navigation toolbar. / 탐색 도구 모음. */
    private final NavigationBar navigation;

    /** The status bar. / 상태 표시줄. */
    private final StatusBar status = new StatusBar();

    /** The page of the window. / 창의 페이지. */
    private final transient BrowserTab tab;

    /** True once the window was closed. / 창이 닫힌 뒤 true. */
    private boolean closed;

    /**
     * Creates a window with an empty page.
     * <p>
     * 빈 페이지가 있는 창을 만든다.
     *
     * @param app the application that owns the window / 창을 소유할 응용 프로그램
     */
    public BrowserWindow(BrowserApp app) {
        super(BrowserApp.NAME);
        this.app = app;
        navigation = new NavigationBar(this);
        tab = new BrowserTab(app);
        tab.setListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(navigation, BorderLayout.NORTH);
        getContentPane().add(tab.getComponent(), BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        setSize(1000, 700);
        setLocationByPlatform(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /**
             * Closes the window when the user asks for it.
             * <p>
             * 사용자가 요청하면 창을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        showTab();
    }

    /**
     * Returns the page of the window.
     * <p>
     * 창의 페이지를 반환한다.
     *
     * @return the active tab / 활성 탭
     */
    public BrowserTab getActiveTab() {
        return tab;
    }

    /**
     * Returns the component that shows the page of this window.
     * <p>
     * 이 창의 페이지를 보여 주는 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public SwingWebPanel getPanel() {
        return tab.getPanel();
    }

    /**
     * Starts loading an absolute address in this window.
     * <p>
     * 이 창에서 절대 주소 읽기를 시작한다.
     *
     * @param url an absolute address / 절대 주소
     */
    public void navigate(String url) {
        tab.navigate(url);
    }

    /**
     * Shows the state of the page in the title bar, the toolbar and the status bar.
     * <p>
     * 페이지의 상태를 제목 표시줄, 도구 모음, 상태 표시줄에 보인다.
     */
    private void showTab() {
        String url = tab.getUrl();
        // 빈 페이지는 응용 프로그램의 이름만 보인다.
        if (BrowserApp.BLANK.equals(url) && (tab.getPanel().getTitle() == null || tab.getPanel().getTitle().trim().length() == 0)) {
            setTitle(BrowserApp.NAME);
        } else {
            setTitle(tab.getTitleText() + " - " + BrowserApp.NAME);
        }
        navigation.showPage(tab.getAddressText());
        navigation.setState(tab.canGoBack(), tab.canGoForward(), tab.isLoading());
        status.show(tab.getStatusText(), tab.getPanel().getProgress(), tab.getPanel().getZoom());
    }

    /**
     * Shows the new state of the page of this window.
     * <p>
     * 이 창의 페이지의 새 상태를 보인다.
     *
     * @param changed the tab that changed / 바뀐 탭
     */
    public void tabChanged(BrowserTab changed) {
        if (!closed && changed == tab) {
            showTab();
        }
    }

    /**
     * Goes back in the history of the page.
     * <p>
     * 페이지의 이력에서 뒤로 간다.
     *
     * @param n the number of entries / 칸 수
     */
    public void back(int n) {
        tab.getPanel().back(n);
    }

    /**
     * Goes forward in the history of the page.
     * <p>
     * 페이지의 이력에서 앞으로 간다.
     *
     * @param n the number of entries / 칸 수
     */
    public void forward(int n) {
        tab.getPanel().forward(n);
    }

    /**
     * Reloads the page.
     * <p>
     * 페이지를 다시 읽는다.
     *
     * @param force true to bypass the cache / 캐시를 우회하려면 true
     */
    public void reload(boolean force) {
        tab.getPanel().refresh(force);
    }

    /**
     * Stops loading the page.
     * <p>
     * 페이지 읽기를 멈춘다.
     */
    public void stop() {
        tab.getPanel().stop();
    }

    /**
     * Opens the home page of the settings.
     * <p>
     * 설정의 홈 페이지를 연다.
     */
    public void home() {
        tab.home();
    }

    /**
     * Opens what the user typed into the address bar and gives the keyboard focus to the page.
     * <p>
     * 사용자가 주소 표시줄에 입력한 것을 열고 키보드 포커스를 페이지에 준다.
     *
     * @param text the text of the address bar / 주소 표시줄의 글
     * @return true if loading started / 읽기를 시작했으면 true
     */
    public boolean open(String text) {
        // 읽기를 시작했으면 탭이 읽는 중인 주소를 알려 주므로 주소 표시줄에 그 주소가 보인다.
        if (tab.open(text) != null) {
            tab.getPanel().requestFocusInWindow();
            return true;
        }
        return false;
    }

    /**
     * Returns the addresses of the history entries behind the current one, the nearest first.
     * <p>
     * 현재 항목 뒤쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> backEntries() {
        return tab.backEntries();
    }

    /**
     * Returns the addresses of the history entries ahead of the current one, the nearest first.
     * <p>
     * 현재 항목 앞쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> forwardEntries() {
        return tab.forwardEntries();
    }

    /**
     * Returns the navigation toolbar; for the menus and for tests.
     * <p>
     * 탐색 도구 모음을 반환한다. 메뉴와 시험이 쓴다.
     *
     * @return the toolbar / 도구 모음
     */
    NavigationBar getNavigationBar() {
        return navigation;
    }

    /**
     * Returns the status bar; for tests.
     * <p>
     * 상태 표시줄을 반환한다. 시험용이다.
     *
     * @return the status bar / 상태 표시줄
     */
    StatusBar getStatusBar() {
        return status;
    }

    /**
     * Closes the window: disposes the page and the frame and tells the application. Calling it again does nothing.
     * <p>
     * 창을 닫는다: 페이지와 프레임을 폐기하고 응용 프로그램에 알린다. 다시 불러도 아무 일도 없다.
     */
    public void closeWindow() {
        if (closed) {
            return;
        }
        closed = true;
        tab.close();
        dispose();
        app.windowClosed(this);
    }

    /**
     * Tells whether the window was closed.
     * <p>
     * 창이 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        return closed;
    }
}
