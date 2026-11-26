package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;

/**
 * A browser window (plan items P31-03 and P31-04): the navigation toolbar with the address bar at the top, the tabs with their pages in
 * the middle and the status bar at the bottom. The title bar, the toolbar and the status bar show the state of the active tab. Every tab
 * has a page of its own; closing a tab disposes its page, closing the last tab closes the window, and closing the window disposes all
 * its pages and tells the application. Use it on the event dispatch thread.
 * <p>
 * 브라우저 창(계획 P31-03, P31-04): 위에 주소 표시줄이 있는 탐색 도구 모음, 가운데에 탭과 그 페이지, 아래에 상태 표시줄이 있다. 제목 표시줄, 도구
 * 모음, 상태 표시줄은 활성 탭의 상태를 보인다. 탭마다 자기 페이지가 있다. 탭을 닫으면 그 페이지를 폐기하고, 마지막 탭을 닫으면 창을 닫으며, 창을
 * 닫으면 모든 페이지를 폐기하고 응용 프로그램에 알린다. 이벤트 디스패치 스레드에서 쓴다.
 */
public final class BrowserWindow extends JFrame implements NavigationBar.Actions, BrowserTab.Listener {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The largest number of closed tabs that can be opened again. / 다시 열 수 있는 닫은 탭의 최대 개수. */
    private static final int CLOSED_TABS_KEPT = 20;

    /** The application that owns the window. / 창을 소유한 응용 프로그램. */
    private final transient BrowserApp app;

    /** The navigation toolbar. / 탐색 도구 모음. */
    private final NavigationBar navigation;

    /** The tab strip with the pages. / 페이지가 든 탭 줄. */
    private final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    /** The status bar. / 상태 표시줄. */
    private final StatusBar status = new StatusBar();

    /** The tabs in the order of the tab strip. / 탭 줄의 순서대로의 탭. */
    private final transient List<BrowserTab> tabs = new ArrayList<BrowserTab>();

    /** The addresses of the tabs that were closed, the last closed at the end. / 닫은 탭의 주소. 마지막에 닫은 것이 끝에 있다. */
    private final transient List<String> closedTabs = new ArrayList<String>();

    /** The tab that the toolbar served last, so that a tab switch is told apart from a change of the same tab. / 도구 모음이 마지막으로 다룬 탭. 탭 전환과 같은 탭의 변경을 구분한다. */
    private transient BrowserTab shownTab;

    /** True once the window was closed. / 창이 닫힌 뒤 true. */
    private boolean closed;

    /**
     * Creates a window with one tab that shows the empty page.
     * <p>
     * 빈 페이지를 보이는 탭 하나가 있는 창을 만든다.
     *
     * @param app the application that owns the window / 창을 소유할 응용 프로그램
     */
    public BrowserWindow(BrowserApp app) {
        super(BrowserApp.NAME);
        this.app = app;
        navigation = new NavigationBar(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(navigation, BorderLayout.NORTH);
        getContentPane().add(tabPane, BorderLayout.CENTER);
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
                requestClose();
            }
        });
        tabPane.addChangeListener(new ChangeListener() {
            /**
             * Shows the tab that was selected.
             * <p>
             * 선택된 탭을 보인다.
             *
             * @param e the event / 이벤트
             */
            public void stateChanged(ChangeEvent e) {
                showTab();
            }
        });
        tabPane.addMouseListener(new MouseAdapter() {
            /**
             * Closes the tab that was clicked with the middle button.
             * <p>
             * 가운데 단추로 누른 탭을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void mouseClicked(MouseEvent e) {
                int index = tabPane.indexAtLocation(e.getX(), e.getY());
                if (SwingUtilities.isMiddleMouseButton(e) && index >= 0 && index < tabs.size()) {
                    closeTab(tabs.get(index));
                }
            }
        });
        newTab(null, true);
    }

    /**
     * Opens a new tab at the end of the tab strip.
     * <p>
     * 탭 줄 끝에 새 탭을 연다.
     *
     * @param url    what the tab opens (an address or text as typed into the address bar), or null for the empty page / 탭이 열 것(주소,
     *               또는 주소 표시줄에 입력하는 글). 빈 페이지이면 null
     * @param select true to make the new tab the active one / 새 탭을 활성 탭으로 만들려면 true
     * @return the new tab / 새 탭
     */
    public BrowserTab newTab(String url, boolean select) {
        final BrowserTab tab = new BrowserTab(app);
        tab.setListener(this);
        tabs.add(tab);
        tabPane.addTab(null, tab.getComponent());
        int index = tabs.size() - 1;
        tabPane.setTabComponentAt(index, new TabHeader(new Runnable() {
            /** Closes this tab. / 이 탭을 닫는다. */
            public void run() {
                closeTab(tab);
            }
        }));
        // 빈 페이지는 컴포넌트의 처음 상태이므로 다시 읽지 않는다.
        if (url != null && url.trim().length() > 0 && !BrowserApp.BLANK.equals(url.trim())) {
            tab.open(url);
        }
        showHeader(tab);
        if (select) {
            tabPane.setSelectedIndex(index);
        }
        // 첫 탭은 선택이 바뀌지 않아도 창에 반영해야 한다.
        showTab();
        return tab;
    }

    /**
     * Opens a new tab as the user asks for one: it shows the page that the settings choose for new tabs, becomes the active tab, and the
     * address bar gets the keyboard focus.
     * <p>
     * 사용자가 요청한 대로 새 탭을 연다: 설정이 새 탭에 정한 페이지를 보이고, 활성 탭이 되며, 주소 표시줄이 키보드 포커스를 받는다.
     */
    public void newTab() {
        boolean home = Settings.NEW_TAB_HOME.equals(app.getSettings().get(Settings.NEW_TAB_PAGE));
        newTab(home ? app.getSettings().get(Settings.HOME_PAGE) : null, true);
        navigation.focusAddress();
    }

    /**
     * Closes a tab: its page is disposed and its address is remembered so that the tab can be opened again. Closing the last tab closes
     * the window.
     * <p>
     * 탭을 닫는다: 그 페이지를 폐기하고, 탭을 다시 열 수 있도록 주소를 기억한다. 마지막 탭을 닫으면 창을 닫는다.
     *
     * @param tab a tab of this window / 이 창의 탭
     */
    public void closeTab(BrowserTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0 || closed) {
            return;
        }
        // 마지막 탭을 닫는 것은 창을 닫는 것이다.
        if (tabs.size() == 1) {
            closeWindow();
            return;
        }
        remember(tab.getUrl());
        tabs.remove(index);
        tabPane.removeTabAt(index);
        tab.close();
        showTab();
    }

    /**
     * Remembers the address of a tab that is being closed, unless it shows the empty page.
     * <p>
     * 닫는 탭의 주소를 기억한다. 빈 페이지를 보이던 탭은 기억하지 않는다.
     *
     * @param url the address of the tab / 탭의 주소
     */
    private void remember(String url) {
        if (url == null || url.length() == 0 || BrowserApp.BLANK.equals(url)) {
            return;
        }
        closedTabs.add(url);
        // 정해진 개수만 남기고 오래된 것부터 버린다.
        while (closedTabs.size() > CLOSED_TABS_KEPT) {
            closedTabs.remove(0);
        }
    }

    /**
     * Opens the tab that was closed last again, in a new tab that becomes the active one.
     * <p>
     * 마지막에 닫은 탭을 새 탭으로 다시 열고 그 탭을 활성 탭으로 만든다.
     *
     * @return the new tab, or null if no closed tab is remembered / 새 탭. 기억한 닫은 탭이 없으면 null
     */
    public BrowserTab reopenClosedTab() {
        if (closedTabs.isEmpty()) {
            return null;
        }
        return newTab(closedTabs.remove(closedTabs.size() - 1), true);
    }

    /**
     * Returns the tabs of the window in the order of the tab strip.
     * <p>
     * 창의 탭을 탭 줄의 순서대로 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    public List<BrowserTab> getTabs() {
        return new ArrayList<BrowserTab>(tabs);
    }

    /**
     * Returns the active tab.
     * <p>
     * 활성 탭을 반환한다.
     *
     * @return the active tab / 활성 탭
     */
    public BrowserTab getActiveTab() {
        int index = tabPane.getSelectedIndex();
        return tabs.get(index < 0 || index >= tabs.size() ? 0 : index);
    }

    /**
     * Makes a tab the active one.
     * <p>
     * 탭을 활성 탭으로 만든다.
     *
     * @param index the position in the tab strip; a value outside it does nothing / 탭 줄에서의 위치. 범위 밖의 값이면 아무것도 하지 않는다
     */
    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabPane.setSelectedIndex(index);
        }
    }

    /**
     * Makes the next or the previous tab the active one, going around at the ends.
     * <p>
     * 다음 또는 이전 탭을 활성 탭으로 만든다. 끝에서는 반대쪽 끝으로 돈다.
     *
     * @param step 1 for the next tab, -1 for the previous one / 다음 탭이면 1, 이전 탭이면 -1
     */
    public void selectNextTab(int step) {
        int count = tabs.size();
        tabPane.setSelectedIndex(((tabPane.getSelectedIndex() + step) % count + count) % count);
    }

    /**
     * Returns the component that shows the page of the active tab.
     * <p>
     * 활성 탭의 페이지를 보여 주는 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public SwingWebPanel getPanel() {
        return getActiveTab().getPanel();
    }

    /**
     * Starts loading an absolute address in the active tab.
     * <p>
     * 활성 탭에서 절대 주소 읽기를 시작한다.
     *
     * @param url an absolute address / 절대 주소
     */
    public void navigate(String url) {
        getActiveTab().navigate(url);
    }

    /**
     * Shows the title and the loading state of a tab in the tab strip.
     * <p>
     * 탭의 제목과 읽는 중 상태를 탭 줄에 보인다.
     *
     * @param tab a tab of this window / 이 창의 탭
     */
    private void showHeader(BrowserTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) {
            return;
        }
        String title = tab.getTitleText();
        ((TabHeader) tabPane.getTabComponentAt(index)).show(title, tab.isLoading());
        tabPane.setToolTipTextAt(index, title);
    }

    /**
     * Shows the state of the active tab in the title bar, the toolbar and the status bar.
     * <p>
     * 활성 탭의 상태를 제목 표시줄, 도구 모음, 상태 표시줄에 보인다.
     */
    private void showTab() {
        if (closed || tabs.isEmpty()) {
            return;
        }
        BrowserTab tab = getActiveTab();
        String url = tab.getUrl();
        // 빈 페이지는 응용 프로그램의 이름만 보인다.
        if (BrowserApp.BLANK.equals(url) && (tab.getPanel().getTitle() == null || tab.getPanel().getTitle().trim().length() == 0)) {
            setTitle(BrowserApp.NAME);
        } else {
            setTitle(tab.getTitleText() + " - " + BrowserApp.NAME);
        }
        // 다른 탭으로 바뀌었으면 이전 탭을 두고 편집하던 글은 버린다.
        if (tab != shownTab) {
            shownTab = tab;
            navigation.switchPage(tab.getAddressText());
        } else {
            navigation.showPage(tab.getAddressText());
        }
        navigation.setState(tab.canGoBack(), tab.canGoForward(), tab.isLoading());
        status.show(tab.getStatusText(), tab.getPanel().getProgress(), tab.getPanel().getZoom());
    }

    /**
     * Shows the new state of a tab: always in the tab strip, and in the rest of the window if it is the active tab.
     * <p>
     * 탭의 새 상태를 보인다: 탭 줄에는 항상, 활성 탭이면 창의 나머지에도 보인다.
     *
     * @param changed the tab that changed / 바뀐 탭
     */
    public void tabChanged(BrowserTab changed) {
        if (closed || !tabs.contains(changed)) {
            return;
        }
        showHeader(changed);
        if (changed == getActiveTab()) {
            showTab();
        }
    }

    /**
     * Goes back in the history of the active tab.
     * <p>
     * 활성 탭의 이력에서 뒤로 간다.
     *
     * @param n the number of entries / 칸 수
     */
    public void back(int n) {
        getPanel().back(n);
    }

    /**
     * Goes forward in the history of the active tab.
     * <p>
     * 활성 탭의 이력에서 앞으로 간다.
     *
     * @param n the number of entries / 칸 수
     */
    public void forward(int n) {
        getPanel().forward(n);
    }

    /**
     * Reloads the page of the active tab.
     * <p>
     * 활성 탭의 페이지를 다시 읽는다.
     *
     * @param force true to bypass the cache / 캐시를 우회하려면 true
     */
    public void reload(boolean force) {
        getPanel().refresh(force);
    }

    /**
     * Stops loading the page of the active tab.
     * <p>
     * 활성 탭의 페이지 읽기를 멈춘다.
     */
    public void stop() {
        getPanel().stop();
    }

    /**
     * Opens the home page of the settings in the active tab.
     * <p>
     * 활성 탭에서 설정의 홈 페이지를 연다.
     */
    public void home() {
        getActiveTab().home();
    }

    /**
     * Opens what the user typed into the address bar in the active tab and gives the keyboard focus to the page.
     * <p>
     * 사용자가 주소 표시줄에 입력한 것을 활성 탭에서 열고 키보드 포커스를 페이지에 준다.
     *
     * @param text the text of the address bar / 주소 표시줄의 글
     * @return true if loading started / 읽기를 시작했으면 true
     */
    public boolean open(String text) {
        BrowserTab tab = getActiveTab();
        // 읽기를 시작했으면 탭이 읽는 중인 주소를 알려 주므로 주소 표시줄에 그 주소가 보인다.
        if (tab.open(text) != null) {
            tab.getPanel().requestFocusInWindow();
            return true;
        }
        return false;
    }

    /**
     * Returns the addresses of the history entries behind the current one of the active tab, the nearest first.
     * <p>
     * 활성 탭에서 현재 항목 뒤쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> backEntries() {
        return getActiveTab().backEntries();
    }

    /**
     * Returns the addresses of the history entries ahead of the current one of the active tab, the nearest first.
     * <p>
     * 활성 탭에서 현재 항목 앞쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> forwardEntries() {
        return getActiveTab().forwardEntries();
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
     * Returns what a tab shows in the tab strip; for tests.
     * <p>
     * 탭이 탭 줄에 보이는 것을 반환한다. 시험용이다.
     *
     * @param index the position in the tab strip / 탭 줄에서의 위치
     * @return the header of the tab / 탭 머리
     */
    TabHeader getTabHeader(int index) {
        return (TabHeader) tabPane.getTabComponentAt(index);
    }

    /**
     * Closes the window as the user asks for it: when it has several tabs and the settings say so, the user is asked first.
     * <p>
     * 사용자가 요청한 대로 창을 닫는다: 탭이 여럿이고 설정이 그렇게 정했으면 먼저 사용자에게 묻는다.
     *
     * @return true if the window was closed / 창을 닫았으면 true
     */
    public boolean requestClose() {
        if (closed) {
            return true;
        }
        // 탭이 여럿이면 설정에 따라 닫기 전에 확인한다.
        if (tabs.size() > 1 && app.getSettings().getBoolean(Settings.CONFIRM_CLOSE_TABS)) {
            String question = Texts.of("Close this window with its " + tabs.size() + " tabs?", "탭 " + tabs.size() + "개가 있는 이 창을 닫을까요?");
            if (!app.getPrompts().confirm(this, question)) {
                return false;
            }
        }
        closeWindow();
        return true;
    }

    /**
     * Closes the window without asking: disposes the pages of all tabs and the frame and tells the application. Calling it again does
     * nothing.
     * <p>
     * 묻지 않고 창을 닫는다: 모든 탭의 페이지와 프레임을 폐기하고 응용 프로그램에 알린다. 다시 불러도 아무 일도 없다.
     */
    public void closeWindow() {
        if (closed) {
            return;
        }
        closed = true;
        // 탭마다 페이지를 폐기한다.
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).close();
        }
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
