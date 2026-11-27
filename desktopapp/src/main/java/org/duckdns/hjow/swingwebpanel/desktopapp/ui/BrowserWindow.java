package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WindowRequest;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkModel;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkNode;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.PageFiles;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ZoomSteps;

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
public final class BrowserWindow extends JFrame implements NavigationBar.Actions, BrowserTab.Listener, Settings.Listener, FindBar.Actions {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The largest number of closed tabs that can be opened again. / 다시 열 수 있는 닫은 탭의 최대 개수. */
    private static final int CLOSED_TABS_KEPT = 20;

    /** The application that owns the window. / 창을 소유한 응용 프로그램. */
    private final transient BrowserApp app;

    /** The navigation toolbar. / 탐색 도구 모음. */
    private final NavigationBar navigation;

    /** The menu bar. / 메뉴 막대. */
    private final BrowserMenu menu;

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

    /** The bookmark manager of this window while it is open, or null. / 열려 있는 동안 이 창의 북마크 관리 창. 없으면 null. */
    private transient BookmarkDialog bookmarkDialog;

    /** The open popup windows that belong to this window. / 이 창에 속한 열린 팝업 창. */
    private final transient List<PopupWindow> popups = new ArrayList<PopupWindow>();

    /** The find bar, above the status bar. / 상태 표시줄 위의 찾기 막대. */
    private final FindBar findBar = new FindBar(this);

    /** The console window of this window while it exists, or null. / 이 창의 콘솔 창. 없으면 null. */
    private transient ConsoleWindow consoleWindow;

    /** The history window of this window while it exists, or null. / 이 창의 방문 기록 창. 없으면 null. */
    private transient HistoryDialog historyDialog;

    /** The source window that was opened last, kept for the tests. / 마지막으로 연 원본 창. 시험을 위해 보관한다. */
    private transient SourceWindow lastSourceWindow;

    /** The context menu that was built last, kept for the tests. / 마지막으로 만든 문맥 메뉴. 시험을 위해 보관한다. */
    private transient JPopupMenu lastContextMenu;

    /** The largest number of bookmarks that a folder opens in tabs without asking. / 묻지 않고 폴더가 탭으로 여는 북마크의 최대 개수. */
    private static final int OPEN_ALL_WITHOUT_ASKING = 10;

    /**
     * Creates a window with one tab that shows the empty page.
     * <p>
     * 빈 페이지를 보이는 탭 하나가 있는 창을 만든다.
     *
     * @param app the application that owns the window / 창을 소유할 응용 프로그램
     */
    public BrowserWindow(BrowserApp app) {
        this(app, null);
    }

    /**
     * Creates a window with one tab: a tab for the component of a window that a page opened, or a tab with the empty page.
     * <p>
     * 탭 하나가 있는 창을 만든다: 페이지가 연 창의 컴포넌트를 위한 탭, 또는 빈 페이지가 있는 탭.
     *
     * @param app   the application that owns the window / 창을 소유할 응용 프로그램
     * @param first the component of a window that a page opened (created by a tab of this application), or null for the empty page /
     *              페이지가 연 창의 컴포넌트(이 응용 프로그램의 탭이 만든 것). 빈 페이지이면 null
     */
    public BrowserWindow(BrowserApp app, SwingWebPanel first) {
        super(BrowserApp.NAME);
        this.app = app;
        navigation = new NavigationBar(this);
        menu = new BrowserMenu(this);
        setJMenuBar(menu);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(navigation, BorderLayout.NORTH);
        getContentPane().add(tabPane, BorderLayout.CENTER);
        javax.swing.JPanel south = new javax.swing.JPanel(new BorderLayout());
        south.add(findBar, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);
        getContentPane().add(south, BorderLayout.SOUTH);
        placeFromSettings();
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
        installKeys();
        app.getSettings().addListener(this);
        if (first == null) {
            newTab(null, true);
        } else {
            addTab(new BrowserTab(app, first), true);
        }
        settingsChanged(app.getSettings());
    }

    /** The bounds of the window while it was not maximized, remembered for the next start. / 최대화되지 않았을 때의 창 경계. 다음 시작을 위해 기억한다. */
    private java.awt.Rectangle normalBounds;

    /**
     * Gives the window the size and the place of the settings (the window of the last run); a place that is not on a screen any more is
     * left to the platform, and a further window is put a little beside the windows that are already open.
     * <p>
     * 창에 설정의 크기와 위치(지난번의 창)를 준다. 더 이상 화면에 없는 위치는 플랫폼에 맡기고, 추가로 여는 창은 이미 열린 창 옆에 조금 비켜 둔다.
     */
    private void placeFromSettings() {
        Settings s = app.getSettings();
        int w = s.getInt(Settings.WINDOW_WIDTH, 300, 10000);
        int h = s.getInt(Settings.WINDOW_HEIGHT, 200, 10000);
        setSize(w, h);
        int others = app.getWindows().size();
        boolean placed = false;
        try {
            int x = Integer.parseInt(s.get(Settings.WINDOW_X).trim()) + others * 30;
            int y = Integer.parseInt(s.get(Settings.WINDOW_Y).trim()) + others * 30;
            java.awt.Rectangle wanted = new java.awt.Rectangle(x, y, w, h);
            java.awt.GraphicsDevice[] screens = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            // 창의 제목 줄이 어느 화면에든 걸쳐 있어야 그 위치를 쓴다.
            for (int i = 0; i < screens.length && !placed; i++) {
                java.awt.Rectangle screen = screens[i].getDefaultConfiguration().getBounds();
                placed = screen.intersects(new java.awt.Rectangle(wanted.x, wanted.y, wanted.width, 40));
            }
            if (placed) {
                setLocation(x, y);
            }
        } catch (NumberFormatException e) {
            placed = false;
        } catch (java.awt.HeadlessException e) {
            placed = false;
        }
        if (!placed) {
            setLocationByPlatform(true);
        }
        normalBounds = getBounds();
        if (s.getBoolean(Settings.WINDOW_MAXIMIZED)) {
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        }
        addComponentListener(new java.awt.event.ComponentAdapter() {
            /**
             * Remembers the bounds while the window is not maximized.
             * <p>
             * 창이 최대화되지 않은 동안의 경계를 기억한다.
             *
             * @param e the event / 이벤트
             */
            public void componentResized(java.awt.event.ComponentEvent e) {
                rememberBounds();
            }

            /**
             * Remembers the bounds while the window is not maximized.
             * <p>
             * 창이 최대화되지 않은 동안의 경계를 기억한다.
             *
             * @param e the event / 이벤트
             */
            public void componentMoved(java.awt.event.ComponentEvent e) {
                rememberBounds();
            }
        });
    }

    /**
     * Remembers the bounds of the window if it is not maximized.
     * <p>
     * 창이 최대화되어 있지 않으면 그 경계를 기억한다.
     */
    private void rememberBounds() {
        if (!closed && (getExtendedState() & MAXIMIZED_BOTH) == 0) {
            normalBounds = getBounds();
        }
    }

    /**
     * Writes the place and the size of the window into the settings, for the next start.
     * <p>
     * 창의 위치와 크기를 다음 시작을 위해 설정에 적는다.
     */
    private void savePlace() {
        java.awt.Rectangle b = normalBounds != null ? normalBounds : getBounds();
        java.util.Map<String, String> values = new java.util.LinkedHashMap<String, String>();
        values.put(Settings.WINDOW_X, String.valueOf(b.x));
        values.put(Settings.WINDOW_Y, String.valueOf(b.y));
        values.put(Settings.WINDOW_WIDTH, String.valueOf(b.width));
        values.put(Settings.WINDOW_HEIGHT, String.valueOf(b.height));
        values.put(Settings.WINDOW_MAXIMIZED, String.valueOf((getExtendedState() & MAXIMIZED_BOTH) != 0));
        app.getSettings().setAll(values);
    }

    /**
     * Returns the addresses of the tabs that show a page (not the empty page), for the session that is restored at the next start.
     * <p>
     * 페이지를 보이는 탭(빈 페이지 제외)의 주소를 반환한다. 다음에 시작할 때 복원하는 세션에 쓴다.
     *
     * @return the addresses in the order of the tab strip / 탭 줄 순서대로의 주소
     */
    public List<String> getTabUrls() {
        List<String> urls = new ArrayList<String>();
        // 탭마다 빈 페이지가 아니면 주소를 모은다.
        for (int i = 0; i < tabs.size(); i++) {
            String url = tabs.get(i).getUrl();
            if (url != null && url.length() > 0 && !BrowserApp.BLANK.equals(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * Returns the position of the active tab among the tabs that {@link #getTabUrls()} lists.
     * <p>
     * {@link #getTabUrls()} 가 나열하는 탭 가운데 활성 탭의 위치를 반환한다.
     *
     * @return the position, 0 if the active tab shows the empty page / 위치. 활성 탭이 빈 페이지를 보이면 0
     */
    public int getActiveTabPosition() {
        int position = 0;
        BrowserTab active = getActiveTab();
        // 활성 탭 앞에 있는, 주소가 있는 탭의 수가 그 위치이다.
        for (int i = 0; i < tabs.size(); i++) {
            String url = tabs.get(i).getUrl();
            boolean listed = url != null && url.length() > 0 && !BrowserApp.BLANK.equals(url);
            if (tabs.get(i) == active) {
                return listed ? position : 0;
            }
            if (listed) {
                position++;
            }
        }
        return 0;
    }

    /**
     * Closes a tab because a script of its page closed the window (the settings allowed it).
     * <p>
     * 페이지의 스크립트가 창을 닫았으므로(설정이 허용했다) 그 탭을 닫는다.
     *
     * @param tab the tab / 탭
     */
    public void tabCloseRequested(BrowserTab tab) {
        closeTab(tab);
    }

    /**
     * Returns the place that a setting chooses.
     * <p>
     * 설정이 고른 곳을 반환한다.
     *
     * @param key the key of the setting / 설정의 키
     * @return {@link Settings#PLACE_POPUP}, {@link Settings#PLACE_WINDOW} or {@link Settings#PLACE_TAB} / {@link Settings#PLACE_POPUP},
     *         {@link Settings#PLACE_WINDOW}, {@link Settings#PLACE_TAB} 중 하나
     */
    private String placeOf(String key) {
        return app.getSettings().getChoice(key, Settings.LINK_PLACE.equals(key) ? new String[] { Settings.PLACE_TAB, Settings.PLACE_WINDOW }
                : new String[] { Settings.PLACE_POPUP, Settings.PLACE_WINDOW, Settings.PLACE_TAB });
    }

    /**
     * Shows a window that a script opened where the settings say (plan item P31-08-g): in a popup window that belongs to this
     * window, in a browser window of its own, or in a new tab of this window that becomes the active one. The script keeps its
     * connection to the window wherever it is shown.
     * <p>
     * 스크립트가 연 창을 설정이 정한 곳에 보인다(계획 P31-08-g): 이 창에 속한 팝업 창, 독립된 브라우저 창, 또는 활성 탭이 되는 이 창의 새 탭.
     * 어디에 보이든 스크립트와 그 창의 관계는 그대로이다.
     *
     * @param opener  the tab whose page opened the window / 페이지가 창을 연 탭
     * @param request the window / 창
     * @return true if the window is shown / 창을 보였으면 true
     */
    public boolean popupOpened(BrowserTab opener, WindowRequest request) {
        SwingWebPanel panel = request.getPanel();
        // 그 사이 스크립트가 닫았거나 이 창이 닫혔으면 보일 것이 없다.
        if (closed || panel == null || panel.isDisposed()) {
            return false;
        }
        String place = placeOf(Settings.POPUP_PLACE);
        if (Settings.PLACE_TAB.equals(place)) {
            addTab(new BrowserTab(app, panel), true);
        } else if (Settings.PLACE_WINDOW.equals(place)) {
            showBeside(app.adoptWindow(panel));
        } else {
            showPopup(new BrowserTab(app, panel), request.getWidth(), request.getHeight(), request.getLeft(), request.getTop());
        }
        return true;
    }

    /**
     * Opens what a link, a form or the user asks to open elsewhere (plan items P31-04-b and P31-08). A link or a form that asks for
     * a new window gets a tab that becomes the active one, or a window, as the settings say. A link that the user opens with Ctrl or
     * the middle button gets a new tab, which becomes the active one only if the settings say so; with Shift it gets a new window.
     * <p>
     * 링크, 폼, 사용자가 다른 곳에서 열어 달라고 한 것을 연다(계획 P31-04-b, P31-08). 새 창을 요청하는 링크나 폼은 설정에 따라 활성 탭이 되는 새
     * 탭이나 새 창을 얻는다. 사용자가 Ctrl 이나 가운데 단추로 연 링크는 새 탭을 얻고, 그 탭은 설정이 그렇게 정했을 때만 활성 탭이 된다. Shift
     * 로 열면 새 창을 얻는다.
     *
     * @param source  the tab of the page / 페이지의 탭
     * @param request what was asked for / 요청한 것
     * @return true if it was opened (or blocked) here / 여기서 열었으면(또는 차단했으면) true
     */
    public boolean windowRequested(BrowserTab source, WindowRequest request) {
        if (closed) {
            return false;
        }
        if (request.getKind() == WindowRequest.USER) {
            String url = request.getUrl();
            if (url == null || url.length() == 0) {
                return false;
            }
            if (WindowRequest.PLACE_WINDOW.equals(request.getPlace())) {
                newWindow(url);
            } else {
                newTab(url, app.getSettings().getBoolean(Settings.SELECT_NEW_TABS));
            }
            return true;
        }
        SwingWebPanel panel = request.getPanel();
        // 팝업 차단이 막았으면 알림은 이미 갔다. 컴포넌트를 만들 수 없었으면 링크는 제자리에서 열린다.
        if (panel == null) {
            return request.isBlocked();
        }
        if (Settings.PLACE_WINDOW.equals(placeOf(Settings.LINK_PLACE))) {
            showBeside(app.adoptWindow(panel));
        } else {
            addTab(new BrowserTab(app, panel), true);
        }
        return true;
    }

    /**
     * Shows a popup window for a page.
     * <p>
     * 페이지를 위한 팝업 창을 보인다.
     *
     * @param tab    the page / 페이지
     * @param width  the width of the page that was asked for, or -1 / 요청한 페이지 폭. 없으면 -1
     * @param height the height of the page that was asked for, or -1 / 요청한 페이지 높이. 없으면 -1
     * @param left   the left edge on the screen, or -1 / 화면의 왼쪽 가장자리. 없으면 -1
     * @param top    the top edge on the screen, or -1 / 화면의 위쪽 가장자리. 없으면 -1
     * @return the popup window; it is visible if this window is / 팝업 창. 이 창이 보이면 팝업도 보인다
     */
    private PopupWindow showPopup(BrowserTab tab, int width, int height, int left, int top) {
        PopupWindow popup = new PopupWindow(this, tab, width, height, left, top);
        popups.add(popup);
        if (isVisible()) {
            popup.setVisible(true);
        }
        return popup;
    }

    /**
     * Forgets a popup window that was closed; called by the popup itself.
     * <p>
     * 닫힌 팝업 창을 목록에서 뺀다. 팝업 자신이 부른다.
     *
     * @param popup the closed popup / 닫힌 팝업
     */
    void popupClosed(PopupWindow popup) {
        popups.remove(popup);
    }

    /**
     * Returns the open popup windows that belong to this window.
     * <p>
     * 이 창에 속한 열린 팝업 창들을 반환한다.
     *
     * @return a copy of the list / 목록의 복사본
     */
    List<PopupWindow> getPopups() {
        return new ArrayList<PopupWindow>(popups);
    }

    /**
     * Shows the button that opens the popup that was blocked last on a page, or hides it when there is none (plan item P31-08-f).
     * <p>
     * 페이지에서 마지막으로 차단된 팝업을 여는 단추를 보인다. 없으면 숨긴다(계획 P31-08-f).
     *
     * @param bar the status bar that shows the page / 페이지를 보이는 상태 표시줄
     * @param tab the page / 페이지
     */
    void offerBlockedPopup(StatusBar bar, final BrowserTab tab) {
        String url = tab.getBlockedPopup();
        // 주소 없는 팝업은 다시 열 것이 없으므로 알림만 보인다.
        if (url == null || url.length() == 0) {
            bar.offer(null, null);
            return;
        }
        bar.offer(Texts.of("Open this popup", "이 팝업 열기"), new Runnable() {
            /** Opens the blocked popup. / 차단된 팝업을 연다. */
            public void run() {
                openBlockedPopup(tab);
            }
        });
    }

    /**
     * Opens the popup that was blocked last on a page, where the settings show popups. The script that asked for it has finished, so
     * the new window has no connection to the page, like a window opened with <code>noopener</code>.
     * <p>
     * 페이지에서 마지막으로 차단된 팝업을 설정이 팝업을 보이는 곳에 연다. 그것을 요청한 스크립트는 이미 끝났으므로, 새 창은
     * <code>noopener</code> 로 연 창처럼 그 페이지와 관계가 없다.
     *
     * @param tab the page whose popup was blocked / 팝업이 차단된 페이지
     */
    void openBlockedPopup(BrowserTab tab) {
        String url = tab.getBlockedPopup();
        if (closed || url == null || url.length() == 0) {
            return;
        }
        tab.clearBlockedPopup();
        String place = placeOf(Settings.POPUP_PLACE);
        if (Settings.PLACE_TAB.equals(place)) {
            newTab(url, true);
        } else if (Settings.PLACE_WINDOW.equals(place)) {
            newWindow(url);
        } else {
            BrowserTab fresh = new BrowserTab(app);
            showPopup(fresh, -1, -1, -1, -1);
            fresh.navigate(url);
        }
    }

    /**
     * Builds the context menu of a page and shows it at the mouse (plan item P31-04-b). Over a link it offers to open the link in a
     * new tab or a new window and to copy its address; then come the commands of the page.
     * <p>
     * 페이지의 문맥 메뉴를 만들어 마우스 위치에 보인다(계획 P31-04-b). 링크 위에서는 링크를 새 탭이나 새 창에서 열기, 주소 복사를 제안하고, 이어서
     * 페이지의 명령이 온다.
     *
     * @param tab  the tab of the page / 페이지의 탭
     * @param link the address of the link under the mouse, or null / 마우스 아래 링크의 주소. 없으면 null
     * @param x    the horizontal position in the component of the page / 페이지 컴포넌트 안의 가로 위치
     * @param y    the vertical position in the component of the page / 페이지 컴포넌트 안의 세로 위치
     */
    public void contextMenuRequested(BrowserTab tab, String link, int x, int y) {
        if (closed) {
            return;
        }
        JPopupMenu menu = contextMenu(tab, link);
        lastContextMenu = menu;
        // 보이지 않는 창(시험)에서는 메뉴를 만들기만 한다.
        if (tab.getPanel().isShowing()) {
            menu.show(tab.getPanel(), x, y);
        }
    }

    /**
     * Returns the context menu that was built last.
     * <p>
     * 마지막으로 만든 문맥 메뉴를 반환한다.
     *
     * @return the menu, or null if none was asked for yet / 메뉴. 아직 요청이 없었으면 null
     */
    JPopupMenu getLastContextMenu() {
        return lastContextMenu;
    }

    /**
     * Builds the context menu of a page.
     * <p>
     * 페이지의 문맥 메뉴를 만든다.
     *
     * @param tab  the tab of the page / 페이지의 탭
     * @param link the address of the link under the mouse, or null / 마우스 아래 링크의 주소. 없으면 null
     * @return the menu; its items are named <code>open-link-tab</code>, <code>open-link-window</code>, <code>copy-link</code>,
     *         <code>back</code>, <code>forward</code>, <code>reload</code>, <code>copy</code> and <code>select-all</code> / 메뉴. 항목의
     *         이름은 <code>open-link-tab</code>, <code>open-link-window</code>, <code>copy-link</code>, <code>back</code>,
     *         <code>forward</code>, <code>reload</code>, <code>copy</code>, <code>select-all</code> 이다
     */
    JPopupMenu contextMenu(final BrowserTab tab, final String link) {
        JPopupMenu menu = new JPopupMenu();
        final SwingWebPanel panel = tab.getPanel();
        if (link != null && link.length() > 0) {
            menu.add(contextItem("open-link-tab", Texts.of("Open link in new tab", "새 탭에서 링크 열기"), true, new Runnable() {
                /** Opens the link in a new tab. / 링크를 새 탭에서 연다. */
                public void run() {
                    newTab(link, app.getSettings().getBoolean(Settings.SELECT_NEW_TABS));
                }
            }));
            menu.add(contextItem("open-link-window", Texts.of("Open link in new window", "새 창에서 링크 열기"), true, new Runnable() {
                /** Opens the link in a new window. / 링크를 새 창에서 연다. */
                public void run() {
                    newWindow(link);
                }
            }));
            menu.add(contextItem("copy-link", Texts.of("Copy link address", "링크 주소 복사"), true, new Runnable() {
                /** Puts the address of the link on the clipboard. / 링크의 주소를 클립보드에 넣는다. */
                public void run() {
                    copyText(link);
                }
            }));
            menu.addSeparator();
        }
        menu.add(contextItem("back", Texts.of("Back", "뒤로"), tab.canGoBack(), new Runnable() {
            /** Goes back in the history of the page. / 페이지의 이력에서 뒤로 간다. */
            public void run() {
                panel.back();
            }
        }));
        menu.add(contextItem("forward", Texts.of("Forward", "앞으로"), tab.canGoForward(), new Runnable() {
            /** Goes forward in the history of the page. / 페이지의 이력에서 앞으로 간다. */
            public void run() {
                panel.forward();
            }
        }));
        menu.add(contextItem("reload", Texts.of("Reload", "새로고침"), true, new Runnable() {
            /** Loads the page again. / 페이지를 다시 읽는다. */
            public void run() {
                panel.refresh();
            }
        }));
        menu.addSeparator();
        menu.add(contextItem("copy", Texts.of("Copy", "복사"), true, new Runnable() {
            /** Copies the selection of the page. / 페이지의 선택을 복사한다. */
            public void run() {
                panel.copy();
            }
        }));
        menu.add(contextItem("select-all", Texts.of("Select all", "모두 선택"), true, new Runnable() {
            /** Selects everything on the page. / 페이지의 모든 것을 선택한다. */
            public void run() {
                panel.selectAll();
            }
        }));
        return menu;
    }

    /**
     * Creates an item of the context menu.
     * <p>
     * 문맥 메뉴의 항목을 만든다.
     *
     * @param name    the name of the item for the tests / 시험을 위한 항목 이름
     * @param text    the text / 글
     * @param enabled whether the item can be chosen / 항목을 고를 수 있는지
     * @param work    what the item does / 항목이 하는 일
     * @return the item / 항목
     */
    private static JMenuItem contextItem(String name, String text, boolean enabled, final Runnable work) {
        JMenuItem item = new JMenuItem(text);
        item.setName(name);
        item.setEnabled(enabled);
        item.addActionListener(new ActionListener() {
            /**
             * Does the work of the item.
             * <p>
             * 항목의 일을 한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                work.run();
            }
        });
        return item;
    }

    /**
     * Puts text on the system clipboard; a clipboard that cannot be used leaves a message in the status bar.
     * <p>
     * 글을 시스템 클립보드에 넣는다. 클립보드를 쓸 수 없으면 상태 표시줄에 메시지를 남긴다.
     *
     * @param text the text / 글
     */
    private void copyText(String text) {
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
        } catch (RuntimeException e) {
            getActiveTab().showNotice(Texts.of("The clipboard cannot be used.", "클립보드를 쓸 수 없습니다."));
        }
    }

    /**
     * Puts another window a little beside this one, with the size of this one, and shows it if this window is shown.
     * <p>
     * 다른 창을 이 창과 같은 크기로 조금 비켜 두고, 이 창이 보이면 그 창도 보인다.
     *
     * @param w the other window / 다른 창
     * @return the other window / 그 창
     */
    private BrowserWindow showBeside(BrowserWindow w) {
        w.setSize(getSize());
        w.setLocation(getX() + 30, getY() + 30);
        if (isVisible()) {
            w.setVisible(true);
        }
        return w;
    }

    /**
     * Installs the keys of the window (plan item P31-05): Ctrl+Tab is taken out of the focus traversal keys so that it can switch tabs,
     * the tab strip gives its own Ctrl+PageUp/PageDown handling to the window, and the keys that have no menu item are bound to their
     * commands for the whole window.
     * <p>
     * 창의 키를 설치한다(계획 P31-05): Ctrl+Tab 이 탭을 바꿀 수 있도록 포커스 이동 키에서 빼고, 탭 줄이 자체 처리하던 Ctrl+PageUp/PageDown 을
     * 창에 넘기며, 메뉴 항목이 없는 키를 창 전체에서 그 명령에 묶는다.
     */
    private void installKeys() {
        Set<AWTKeyStroke> forward = new HashSet<AWTKeyStroke>();
        forward.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forward);
        Set<AWTKeyStroke> backward = new HashSet<AWTKeyStroke>();
        backward.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backward);
        int ctrl = InputEvent.CTRL_MASK;
        int shift = InputEvent.SHIFT_MASK;
        BrowserTab.unbind(tabPane, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, ctrl));
        BrowserTab.unbind(tabPane, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, ctrl));
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ctrl), Commands.ZOOM_IN);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ctrl), Commands.ZOOM_IN);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrl | shift), Commands.ZOOM_IN);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrl), Commands.ZOOM_OUT);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, ctrl), Commands.ZOOM_RESET);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, ctrl), Commands.RELOAD);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, ctrl | shift), Commands.RELOAD_HARD);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, ctrl), Commands.NEXT_TAB);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, ctrl), Commands.PREVIOUS_TAB);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ctrl), Commands.CLOSE_TAB);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), Commands.OPEN_LOCATION);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_MASK), Commands.OPEN_LOCATION);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), Commands.FIND_NEXT);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, shift), Commands.FIND_PREVIOUS);
        // Ctrl+1 ~ Ctrl+8 은 그 번호의 탭, Ctrl+9 는 마지막 탭이다.
        for (int n = 1; n <= 8; n++) {
            bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_0 + n, ctrl), Commands.SELECT_TAB + n);
            bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + n, ctrl), Commands.SELECT_TAB + n);
        }
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_9, ctrl), Commands.LAST_TAB);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, ctrl), Commands.LAST_TAB);
    }

    /**
     * Binds a key to a command for the whole window.
     * <p>
     * 키를 창 전체에서 명령에 묶는다.
     *
     * @param key     the key / 키
     * @param command the command name / 명령 이름
     */
    private void bindKey(KeyStroke key, final String command) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            /** Serialization version. / 직렬화 버전. */
            private static final long serialVersionUID = 1L;

            /** Runs the command. / 명령을 실행한다. */
            public void actionPerformed(ActionEvent e) {
                perform(command);
            }
        });
    }

    /**
     * Runs a command of the window (plan item P31-05); menu items, key bindings and tests all come through here.
     * <p>
     * 창의 명령을 실행한다(계획 P31-05). 메뉴 항목, 키 바인딩, 시험이 모두 여기를 거친다.
     *
     * @param command one of the names of {@link Commands} / {@link Commands} 의 이름 가운데 하나
     * @return true if the command is known and was run / 아는 명령이고 실행했으면 true
     */
    public boolean perform(String command) {
        if (closed || command == null) {
            return false;
        }
        BrowserTab tab = getActiveTab();
        // 명령 이름에 따라 해당하는 일을 한다.
        if (Commands.NEW_TAB.equals(command)) {
            newTab();
        } else if (Commands.NEW_WINDOW.equals(command)) {
            newWindow(null);
        } else if (Commands.OPEN_LOCATION.equals(command)) {
            // 도구 모음을 숨겨 두었으면 주소를 입력할 수 있게 다시 보인다.
            app.getSettings().set(Settings.SHOW_TOOLBAR, "true");
            navigation.focusAddress();
        } else if (Commands.OPEN_FILE.equals(command)) {
            openFile();
        } else if (Commands.SAVE_PAGE.equals(command)) {
            savePage();
        } else if (Commands.CLOSE_TAB.equals(command)) {
            closeTab(tab);
        } else if (Commands.CLOSE_WINDOW.equals(command)) {
            requestClose();
        } else if (Commands.EXIT.equals(command)) {
            app.exit();
        } else if (Commands.CUT.equals(command) || Commands.COPY.equals(command) || Commands.PASTE.equals(command)
                || Commands.SELECT_ALL.equals(command)) {
            edit(command);
        } else if (Commands.TOGGLE_TOOLBAR.equals(command)) {
            app.getSettings().set(Settings.SHOW_TOOLBAR, String.valueOf(!isToolbarShown()));
        } else if (Commands.TOGGLE_STATUS_BAR.equals(command)) {
            app.getSettings().set(Settings.SHOW_STATUS_BAR, String.valueOf(!isStatusBarShown()));
        } else if (Commands.ZOOM_IN.equals(command)) {
            tab.getPanel().setZoom(ZoomSteps.larger(tab.getPanel().getZoom()));
        } else if (Commands.ZOOM_OUT.equals(command)) {
            tab.getPanel().setZoom(ZoomSteps.smaller(tab.getPanel().getZoom()));
        } else if (Commands.ZOOM_RESET.equals(command)) {
            tab.getPanel().setZoom(ZoomSteps.parsePercent(app.getSettings().get(Settings.DEFAULT_ZOOM), 1.0));
        } else if (Commands.BACK.equals(command)) {
            back(1);
        } else if (Commands.FORWARD.equals(command)) {
            forward(1);
        } else if (Commands.HOME.equals(command)) {
            home();
        } else if (Commands.RELOAD.equals(command)) {
            reload(false);
        } else if (Commands.RELOAD_HARD.equals(command)) {
            reload(true);
        } else if (Commands.STOP.equals(command)) {
            stop();
        } else if (Commands.NEXT_TAB.equals(command)) {
            selectNextTab(1);
        } else if (Commands.PREVIOUS_TAB.equals(command)) {
            selectNextTab(-1);
        } else if (Commands.REOPEN_TAB.equals(command)) {
            reopenClosedTab();
        } else if (Commands.LAST_TAB.equals(command)) {
            selectTab(tabs.size() - 1);
        } else if (command.startsWith(Commands.SELECT_TAB)) {
            return selectTabByNumber(command.substring(Commands.SELECT_TAB.length()));
        } else if (Commands.ABOUT.equals(command)) {
            app.getPrompts().inform(this, BrowserApp.aboutText());
        } else {
            return performMore(command, tab);
        }
        return true;
    }

    /**
     * Runs the commands of the features that are added after the basic ones (bookmarks, settings, find, print and the helper windows).
     * <p>
     * 기본 명령 뒤에 더한 기능(북마크, 설정, 찾기, 인쇄, 보조 창)의 명령을 실행한다.
     *
     * @param command the command name / 명령 이름
     * @param tab     the active tab / 활성 탭
     * @return true if the command is known and was run / 아는 명령이고 실행했으면 true
     */
    private boolean performMore(String command, BrowserTab tab) {
        if (Commands.ADD_BOOKMARK.equals(command)) {
            addBookmark(tab);
            return true;
        }
        if (Commands.BOOKMARK_ALL_TABS.equals(command)) {
            bookmarkAllTabs();
            return true;
        }
        if (Commands.MANAGE_BOOKMARKS.equals(command)) {
            showBookmarkManager();
            return true;
        }
        if (Commands.SETTINGS.equals(command)) {
            showSettings();
            return true;
        }
        if (Commands.FIND.equals(command)) {
            findBar.open();
            return true;
        }
        if (Commands.FIND_NEXT.equals(command) || Commands.FIND_PREVIOUS.equals(command)) {
            // 닫힌 막대는 다시 열고(남아 있던 글로 다시 찾는다), 열린 막대는 그 방향으로 간다.
            if (!findBar.isOpen() || findBar.getText().length() == 0) {
                findBar.open();
            } else {
                findBar.search(Commands.FIND_NEXT.equals(command));
            }
            return true;
        }
        if (Commands.VIEW_SOURCE.equals(command)) {
            showSource();
            return true;
        }
        if (Commands.CONSOLE.equals(command)) {
            showConsole();
            return true;
        }
        if (Commands.HISTORY.equals(command)) {
            showHistory();
            return true;
        }
        if (Commands.CLEAR_PRIVATE_DATA.equals(command)) {
            clearPrivateData();
            return true;
        }
        if (Commands.PAGE_SETUP.equals(command)) {
            pageSetup();
            return true;
        }
        if (Commands.PRINT_PREVIEW.equals(command)) {
            printTab(tab, true);
            return true;
        }
        if (Commands.PRINT.equals(command)) {
            printTab(tab, false);
            return true;
        }
        tab.showNotice(Texts.of("This command is not available yet: ", "아직 쓸 수 없는 명령입니다: ") + command);
        return false;
    }

    /** The print preview that was opened last, kept for the tests. / 마지막으로 연 인쇄 미리 보기. 시험을 위해 보관한다. */
    private transient PrintPreview lastPreview;

    /**
     * Lets the user change the paper of the page setup (plan item P31-10) and keeps the choice in the settings.
     * <p>
     * 사용자가 페이지 설정의 용지를 바꾸게 하고(계획 P31-10) 고른 것을 설정에 보관한다.
     */
    void pageSetup() {
        java.awt.print.PageFormat chosen = app.getPrinting().pageSetup(this, app.getPaper());
        if (chosen != null) {
            app.setPaper(chosen);
            getActiveTab().showNotice(Texts.of("Page setup: ", "페이지 설정: ")
                    + org.duckdns.hjow.swingwebpanel.desktopapp.model.PaperFormat.describe(chosen));
        }
    }

    /**
     * Prints the page of a tab (plan item P31-10): the component lays the page out for the paper of the page setup, the settings
     * decide whether backgrounds are printed and whether headers and footers are added, and then the preview is shown or the print
     * system is asked to print. The layout is made in the background; the status bar tells while it is made.
     * <p>
     * 탭의 페이지를 인쇄한다(계획 P31-10): 컴포넌트가 페이지 설정의 용지에 맞춰 페이지를 배치하고, 설정이 배경을 인쇄할지와 머리글·바닥글을
     * 더할지를 정하며, 그런 다음 미리 보기를 보이거나 인쇄 시스템에 인쇄를 요청한다. 배치는 배경에서 만들어지고, 그동안 상태 표시줄이 알린다.
     *
     * @param tab     the tab whose page is printed / 페이지를 인쇄할 탭
     * @param preview true to show the preview instead of printing / 인쇄하는 대신 미리 보기를 보이려면 true
     */
    void printTab(final BrowserTab tab, final boolean preview) {
        if (closed || tab.getPanel().isDisposed()) {
            return;
        }
        final boolean headers = app.getSettings().getBoolean(Settings.PRINT_HEADERS);
        boolean backgrounds = app.getSettings().getBoolean(Settings.PRINT_BACKGROUNDS);
        final String date = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(new Date());
        tab.showNotice(Texts.of("Preparing the pages...", "쪽을 준비하는 중..."));
        tab.getPanel().createPrintout(app.getPaper(), backgrounds, new org.duckdns.hjow.swingwebpanel.PrintoutListener() {
            /**
             * Shows the preview or starts printing when the pages are ready.
             * <p>
             * 쪽이 준비되면 미리 보기를 보이거나 인쇄를 시작한다.
             *
             * @param printout the pages, or null / 쪽들 또는 null
             */
            public void printoutReady(org.duckdns.hjow.swingwebpanel.PagePrintout printout) {
                if (closed) {
                    return;
                }
                if (printout == null) {
                    tab.showNotice(Texts.of("The page could not be prepared for printing.", "페이지를 인쇄용으로 준비할 수 없습니다."));
                    return;
                }
                String title = printout.getTitle().length() > 0 ? printout.getTitle() : printout.getUrl();
                org.duckdns.hjow.swingwebpanel.desktopapp.model.PrintDocument doc = new org.duckdns.hjow.swingwebpanel.desktopapp.model.PrintDocument(
                        printout, headers, title, printout.getUrl(), date);
                tab.showNotice(Texts.of(doc.getNumberOfPages() + " page(s) ready to print", "인쇄할 쪽 " + doc.getNumberOfPages() + "개 준비됨"));
                if (preview) {
                    showPreview(doc, title);
                } else {
                    startPrint(doc, title);
                }
            }
        });
    }

    /**
     * Shows the print preview of a document; it is shown if this window is shown.
     * <p>
     * 문서의 인쇄 미리 보기를 보인다. 이 창이 보이면 미리 보기도 보인다.
     *
     * @param doc   the document / 문서
     * @param title what names it / 그것을 부르는 글
     * @return the preview / 미리 보기
     */
    PrintPreview showPreview(final java.awt.print.Pageable doc, final String title) {
        PrintPreview p = new PrintPreview(this, doc, title, new Runnable() {
            /** Prints the document that the preview shows. / 미리 보기가 보이는 문서를 인쇄한다. */
            public void run() {
                startPrint(doc, title);
            }
        });
        lastPreview = p;
        if (isVisible()) {
            p.setVisible(true);
        }
        return p;
    }

    /**
     * Hands a document to the print system, which asks for the printer and prints.
     * <p>
     * 문서를 인쇄 시스템에 넘긴다. 인쇄 시스템이 프린터를 묻고 인쇄한다.
     *
     * @param doc   the document / 문서
     * @param title the name of the print job / 인쇄 작업의 이름
     * @return true if printing started / 인쇄를 시작했으면 true
     */
    boolean startPrint(java.awt.print.Pageable doc, String title) {
        return !closed && app.getPrinting().print(this, doc, title);
    }

    /**
     * Prints the page of a tab because a script of the page asked for it (<code>window.print()</code>): the same as the print command.
     * <p>
     * 페이지의 스크립트가 요청했으므로(<code>window.print()</code>) 탭의 페이지를 인쇄한다: 인쇄 명령과 같다.
     *
     * @param tab the tab / 탭
     */
    public void printRequested(BrowserTab tab) {
        printTab(tab, false);
    }

    /**
     * Returns the print preview that was opened last; for tests.
     * <p>
     * 마지막으로 연 인쇄 미리 보기를 반환한다. 시험용이다.
     *
     * @return the preview, or null if none was opened / 미리 보기. 연 적이 없으면 null
     */
    PrintPreview getLastPreview() {
        return lastPreview;
    }

    /**
     * Searches the page of the active tab for the text of the find bar (plan item P31-09); an empty text ends the search.
     * <p>
     * 활성 탭의 페이지에서 찾기 막대의 글을 찾는다(계획 P31-09). 빈 글은 찾기를 끝낸다.
     *
     * @param text      the text of the find bar / 찾기 막대의 글
     * @param matchCase true to tell upper and lower case apart / 대소문자를 구분하려면 true
     * @param forward   true for the next match / 다음 일치이면 true
     */
    public void find(String text, boolean matchCase, boolean forward) {
        if (closed) {
            return;
        }
        SwingWebPanel p = getPanel();
        if (text == null || text.trim().length() == 0) {
            p.clearFind();
        } else {
            p.find(text, matchCase, forward);
        }
    }

    /**
     * Ends the search after the find bar was closed and gives the keyboard focus back to the page.
     * <p>
     * 찾기 막대가 닫힌 뒤 찾기를 끝내고 키보드 포커스를 페이지에 돌려준다.
     */
    public void findClosed() {
        if (closed) {
            return;
        }
        getPanel().clearFind();
        getPanel().requestFocusInWindow();
    }

    /**
     * Returns the find bar; for tests.
     * <p>
     * 찾기 막대를 반환한다. 시험용이다.
     *
     * @return the find bar / 찾기 막대
     */
    FindBar getFindBar() {
        return findBar;
    }

    /**
     * Opens a window with the source of the page of the active tab (plan item P31-09): the text that was received, shown as plain
     * text. It is shown if this window is shown.
     * <p>
     * 활성 탭 페이지의 원본을 보이는 창을 연다(계획 P31-09): 수신한 글을 평문으로 보인다. 이 창이 보이면 그 창도 보인다.
     *
     * @return the source window / 원본 창
     */
    SourceWindow showSource() {
        SwingWebPanel p = getPanel();
        SourceWindow w = new SourceWindow(this, p.getPage(), p.getMimeType(), p.getContent());
        lastSourceWindow = w;
        if (isVisible()) {
            w.setVisible(true);
        }
        return w;
    }

    /**
     * Returns the source window that was opened last; for tests.
     * <p>
     * 마지막으로 연 원본 창을 반환한다. 시험용이다.
     *
     * @return the window, or null if none was opened / 창. 연 적이 없으면 null
     */
    SourceWindow getLastSourceWindow() {
        return lastSourceWindow;
    }

    /**
     * Shows the console window of this window with the messages of the active tab (plan item P31-09), creating it if needed.
     * <p>
     * 활성 탭의 메시지가 든 이 창의 콘솔 창을 보인다(계획 P31-09). 없으면 만든다.
     *
     * @return the console window / 콘솔 창
     */
    ConsoleWindow showConsole() {
        if (consoleWindow == null) {
            consoleWindow = new ConsoleWindow(this);
        }
        BrowserTab tab = getActiveTab();
        consoleWindow.showLog(tab.getConsole(), tab.getTitleText());
        if (isVisible()) {
            consoleWindow.setVisible(true);
            consoleWindow.toFront();
        }
        return consoleWindow;
    }

    /**
     * Shows the history window of this window with the session history of the active tab (plan item P31-09), creating it if needed.
     * <p>
     * 활성 탭의 세션 이력이 든 이 창의 방문 기록 창을 보인다(계획 P31-09). 없으면 만든다.
     *
     * @return the history window / 방문 기록 창
     */
    HistoryDialog showHistory() {
        if (historyDialog == null) {
            historyDialog = new HistoryDialog(this);
        }
        historyDialog.showTab(getActiveTab());
        if (isVisible()) {
            historyDialog.setVisible(true);
            historyDialog.toFront();
        }
        return historyDialog;
    }

    /**
     * Asks the user and then removes the private data of the whole application (plan item P31-09): every tab and popup of every
     * window is emptied, and the cookies and the local storage that they share are removed.
     * <p>
     * 사용자에게 물은 뒤 응용 프로그램 전체의 개인 정보를 지운다(계획 P31-09): 모든 창의 모든 탭과 팝업을 비우고, 그것들이 함께 쓰는 쿠키와
     * localStorage 를 지운다.
     *
     * @return true if the data was removed / 지웠으면 true
     */
    boolean clearPrivateData() {
        String question = Texts.of("Clear the private data?\n\n"
                + "All tabs and popup windows of all windows will show the empty page, and their history, cache and session storage are removed.\n"
                + "The cookies and the local storage, which all tabs and windows share, are removed too.\n"
                + "Bookmarks and settings are kept.",
                "개인 정보를 지울까요?\n\n"
                + "모든 창의 모든 탭과 팝업 창이 빈 페이지가 되고, 그 방문 기록, 캐시, sessionStorage 가 지워집니다.\n"
                + "모든 탭과 창이 함께 쓰는 쿠키와 localStorage 도 지워집니다.\n"
                + "북마크와 설정은 그대로 남습니다.");
        if (!app.getPrompts().confirm(this, question)) {
            return false;
        }
        app.clearPrivateData();
        return true;
    }

    /**
     * Empties the pages of this window for the clearing of the private data: every tab and popup, and the list of closed tabs.
     * <p>
     * 개인 정보를 지우기 위해 이 창의 페이지를 비운다: 모든 탭과 팝업, 그리고 닫은 탭의 목록.
     */
    public void clearPages() {
        if (closed) {
            return;
        }
        findBar.close();
        closedTabs.clear();
        // 탭마다 페이지와 그 기록을 지운다.
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).clearPrivateData();
        }
        // 팝업 창의 페이지도 지운다.
        for (int i = 0; i < popups.size(); i++) {
            popups.get(i).getTab().clearPrivateData();
        }
    }

    /**
     * Selects the tab with a number as the keys Ctrl+1 to Ctrl+8 do.
     * <p>
     * Ctrl+1 ~ Ctrl+8 키처럼 번호로 탭을 고른다.
     *
     * @param number the number of the tab, starting at 1, as text / 1 부터 시작하는 탭 번호(글)
     * @return true if the text is a number / 글이 수이면 true
     */
    private boolean selectTabByNumber(String number) {
        try {
            selectTab(Integer.parseInt(number) - 1);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Opens a new window next to this one; it is shown if this window is shown.
     * <p>
     * 이 창 옆에 새 창을 연다. 이 창이 보이면 새 창도 보인다.
     *
     * @param url what the window opens, or null for the page that the settings choose for new tabs / 창이 열 것. null 이면 설정이 새 탭에
     *            정한 페이지
     * @return the new window / 새 창
     */
    public BrowserWindow newWindow(String url) {
        String first = url;
        // 주소를 주지 않았으면 새 탭과 같은 페이지로 시작한다.
        if (first == null && Settings.NEW_TAB_HOME.equals(app.getSettings().get(Settings.NEW_TAB_PAGE))) {
            first = app.getSettings().get(Settings.HOME_PAGE);
        }
        return showBeside(app.openWindow(first));
    }

    /**
     * Asks for a file and opens it in the active tab.
     * <p>
     * 파일을 물어 활성 탭에서 연다.
     */
    private void openFile() {
        File f = app.getPrompts().chooseFile(this, false, null);
        if (f != null) {
            getActiveTab().open(f.getAbsolutePath());
        }
    }

    /**
     * Asks for a file and saves the received text of the page of the active tab into it.
     * <p>
     * 파일을 물어 활성 탭 페이지의 수신 원문을 그 파일에 저장한다.
     */
    private void savePage() {
        BrowserTab tab = getActiveTab();
        SwingWebPanel p = tab.getPanel();
        File f = app.getPrompts().chooseFile(this, true, PageFiles.suggestName(p.getPage(), p.getMimeType()));
        if (f == null) {
            return;
        }
        try {
            PageFiles.write(f, p.getContent(), p.getMimeType());
            tab.showNotice(Texts.of("Saved: ", "저장했습니다: ") + f.getPath());
        } catch (IOException e) {
            app.getPrompts().inform(this, Texts.of("The page could not be saved: ", "페이지를 저장할 수 없습니다: ") + e.getMessage());
        }
    }

    /**
     * Runs an editing command where the user is working: in a text field of the window itself (the address bar, the find bar) when it
     * has the keyboard focus, otherwise in the page of the active tab.
     * <p>
     * 사용자가 작업 중인 곳에서 편집 명령을 실행한다: 창 자체의 텍스트 필드(주소 표시줄, 찾기 막대)가 키보드 포커스를 가지면 그 필드에서, 아니면
     * 활성 탭의 페이지에서 실행한다.
     *
     * @param command {@link Commands#CUT}, {@link Commands#COPY}, {@link Commands#PASTE} or {@link Commands#SELECT_ALL} /
     *                {@link Commands#CUT}, {@link Commands#COPY}, {@link Commands#PASTE}, {@link Commands#SELECT_ALL} 중 하나
     */
    private void edit(String command) {
        SwingWebPanel p = getPanel();
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        // 페이지 안의 편집기(폼 컨트롤)는 컴포넌트가 스스로 다루므로 창 자체의 필드만 여기서 처리한다.
        if (owner instanceof JTextComponent && SwingUtilities.isDescendingFrom(owner, this) && !SwingUtilities.isDescendingFrom(owner, p)) {
            JTextComponent field = (JTextComponent) owner;
            if (Commands.CUT.equals(command)) {
                field.cut();
            } else if (Commands.COPY.equals(command)) {
                field.copy();
            } else if (Commands.PASTE.equals(command)) {
                field.paste();
            } else {
                field.selectAll();
            }
            return;
        }
        if (Commands.CUT.equals(command)) {
            p.cut();
        } else if (Commands.COPY.equals(command)) {
            p.copy();
        } else if (Commands.PASTE.equals(command)) {
            p.paste();
        } else {
            p.selectAll();
        }
    }

    /**
     * Applies the settings that concern the window: which bars are shown.
     * <p>
     * 창에 관한 설정을 적용한다: 어느 막대를 보이는지.
     *
     * @param settings the settings / 설정
     */
    public void settingsChanged(Settings settings) {
        if (closed) {
            return;
        }
        boolean toolbar = settings.getBoolean(Settings.SHOW_TOOLBAR);
        boolean statusBar = settings.getBoolean(Settings.SHOW_STATUS_BAR);
        // 달라진 것이 있을 때만 다시 배치한다.
        if (navigation.isVisible() != toolbar || status.isVisible() != statusBar) {
            navigation.setVisible(toolbar);
            status.setVisible(statusBar);
            getContentPane().validate();
        }
        // 탭마다 페이지에 관한 설정(스크립트, 글꼴, 확대 비율 등)을 적용한다.
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).applySettings();
        }
        // 팝업 창의 페이지에도 적용한다.
        for (int i = 0; i < popups.size(); i++) {
            popups.get(i).getTab().applySettings();
        }
    }

    /**
     * Tells whether the navigation toolbar is shown.
     * <p>
     * 탐색 도구 모음을 보이는지 알려 준다.
     *
     * @return true if shown / 보이면 true
     */
    public boolean isToolbarShown() {
        return navigation.isVisible();
    }

    /**
     * Tells whether the status bar is shown.
     * <p>
     * 상태 표시줄을 보이는지 알려 준다.
     *
     * @return true if shown / 보이면 true
     */
    public boolean isStatusBarShown() {
        return status.isVisible();
    }

    /**
     * Tells whether a closed tab can be opened again.
     * <p>
     * 다시 열 수 있는 닫은 탭이 있는지 알려 준다.
     *
     * @return true if at least one closed tab is remembered / 기억한 닫은 탭이 하나라도 있으면 true
     */
    public boolean hasClosedTabs() {
        return !closedTabs.isEmpty();
    }

    /**
     * Fills the bookmark menu with the bookmarks; called each time the menu opens.
     * <p>
     * 북마크 메뉴를 북마크로 채운다. 메뉴가 열릴 때마다 호출된다.
     *
     * @param target the menu to add the items to / 항목을 더할 메뉴
     */
    void fillBookmarkMenu(JMenu target) {
        BookmarkNode root = app.getBookmarks().getRoot();
        if (root.getChildCount() > 0) {
            target.addSeparator();
        }
        fillBookmarkItems(target, root);
    }

    /**
     * Adds the entries of a bookmark folder to a menu: a bookmark becomes an item, a folder a submenu.
     * <p>
     * 북마크 폴더의 항목을 메뉴에 더한다: 북마크는 항목이, 폴더는 하위 메뉴가 된다.
     *
     * @param target the menu / 메뉴
     * @param folder the bookmark folder / 북마크 폴더
     */
    private void fillBookmarkItems(JMenu target, final BookmarkNode folder) {
        // 항목마다 메뉴 항목이나 하위 메뉴를 만든다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            final BookmarkNode n = folder.getChild(i);
            if (n.isFolder()) {
                JMenu sub = new JMenu(menuText(n.getName()));
                JMenuItem all = new JMenuItem(Texts.of("Open All in Tabs", "모두 탭으로 열기"));
                all.setEnabled(n.getChildCount() > 0);
                all.addActionListener(new ActionListener() {
                    /** Opens every bookmark of the folder in a tab. / 폴더의 모든 북마크를 탭으로 연다. */
                    public void actionPerformed(ActionEvent e) {
                        openBookmarkFolder(n);
                    }
                });
                sub.add(all);
                if (n.getChildCount() > 0) {
                    sub.addSeparator();
                }
                fillBookmarkItems(sub, n);
                target.add(sub);
            } else {
                JMenuItem item = new JMenuItem(menuText(n.toString()));
                item.setToolTipText(n.getUrl());
                item.addActionListener(new ActionListener() {
                    /** Opens the bookmark: with Ctrl in a new tab, with Shift in a new window. / 북마크를 연다: Ctrl 이면 새 탭, Shift 이면 새 창. */
                    public void actionPerformed(ActionEvent e) {
                        int where = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0 ? BookmarkDialog.NEW_TAB
                                : (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0 ? BookmarkDialog.NEW_WINDOW : BookmarkDialog.HERE;
                        openBookmark(n.getUrl(), where);
                    }
                });
                target.add(item);
            }
        }
    }

    /**
     * Shortens a long name for a menu item; an empty name gets a placeholder.
     * <p>
     * 메뉴 항목에 쓸 긴 이름을 줄인다. 빈 이름은 대신할 글을 쓴다.
     *
     * @param name the name / 이름
     * @return the text of the menu item / 메뉴 항목의 글
     */
    private static String menuText(String name) {
        if (name.length() == 0) {
            return Texts.of("(no name)", "(이름 없음)");
        }
        return name.length() <= 60 ? name : name.substring(0, 57) + "...";
    }

    /**
     * Opens the address of a bookmark in the active tab, in a new tab or in a new window.
     * <p>
     * 북마크의 주소를 활성 탭, 새 탭, 또는 새 창에서 연다.
     *
     * @param url   the address / 주소
     * @param where {@link BookmarkDialog#HERE}, {@link BookmarkDialog#NEW_TAB} or {@link BookmarkDialog#NEW_WINDOW} /
     *              {@link BookmarkDialog#HERE}, {@link BookmarkDialog#NEW_TAB}, {@link BookmarkDialog#NEW_WINDOW} 중 하나
     */
    void openBookmark(String url, int where) {
        if (closed || url == null || url.trim().length() == 0) {
            return;
        }
        if (where == BookmarkDialog.NEW_TAB) {
            newTab(url, true);
        } else if (where == BookmarkDialog.NEW_WINDOW) {
            newWindow(url);
        } else {
            getActiveTab().open(url);
        }
    }

    /**
     * Opens every bookmark that lies directly in a folder in a new tab; many bookmarks are opened only after the user agreed.
     * <p>
     * 폴더 바로 안의 모든 북마크를 새 탭으로 연다. 북마크가 많으면 사용자가 동의한 뒤에만 연다.
     *
     * @param folder the bookmark folder / 북마크 폴더
     */
    void openBookmarkFolder(BookmarkNode folder) {
        List<String> urls = new ArrayList<String>();
        // 폴더 바로 안의 북마크만 모은다(하위 폴더는 열지 않는다).
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode n = folder.getChild(i);
            if (!n.isFolder() && n.getUrl().trim().length() > 0) {
                urls.add(n.getUrl());
            }
        }
        if (urls.size() > OPEN_ALL_WITHOUT_ASKING
                && !app.getPrompts().confirm(this, Texts.of("Open " + urls.size() + " tabs?", "탭 " + urls.size() + "개를 열까요?"))) {
            return;
        }
        // 주소마다 새 탭을 연다. 첫 탭만 앞에 보인다.
        for (int i = 0; i < urls.size(); i++) {
            newTab(urls.get(i), i == 0);
        }
    }

    /**
     * Adds the page of a tab to the bookmarks; a page that is already bookmarked is added again only after the user agreed.
     * <p>
     * 탭의 페이지를 북마크에 더한다. 이미 북마크된 페이지는 사용자가 동의한 뒤에만 다시 더한다.
     *
     * @param tab the tab / 탭
     */
    private void addBookmark(BrowserTab tab) {
        String url = tab.getUrl();
        if (url == null || url.length() == 0 || BrowserApp.BLANK.equals(url)) {
            tab.showNotice(Texts.of("The empty page cannot be bookmarked.", "빈 페이지는 북마크에 추가할 수 없습니다."));
            return;
        }
        BookmarkModel bookmarks = app.getBookmarks();
        if (bookmarks.contains(url)
                && !app.getPrompts().confirm(this, Texts.of("This page is already bookmarked. Add it again?", "이미 북마크된 페이지입니다. 다시 추가할까요?"))) {
            return;
        }
        bookmarks.add(bookmarks.getRoot(), BookmarkNode.bookmark(tab.getTitleText(), url));
        showStorageResult(tab, Texts.of("Bookmarked: ", "북마크에 추가했습니다: ") + tab.getTitleText());
    }

    /**
     * Asks for a folder name and adds the pages of all tabs of the window to a new bookmark folder with that name.
     * <p>
     * 폴더 이름을 물어 창의 모든 탭의 페이지를 그 이름의 새 북마크 폴더에 더한다.
     */
    private void bookmarkAllTabs() {
        String suggested = Texts.of("Tabs ", "탭 ") + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String name = app.getPrompts().ask(this, Texts.of("Name of the bookmark folder for all tabs:", "모든 탭을 담을 북마크 폴더의 이름:"), suggested);
        if (name == null || name.trim().length() == 0) {
            return;
        }
        BookmarkModel bookmarks = app.getBookmarks();
        BookmarkNode folder = BookmarkNode.folder(name.trim());
        bookmarks.add(bookmarks.getRoot(), folder);
        int added = 0;
        // 탭마다 빈 페이지가 아니면 북마크를 더한다.
        for (int i = 0; i < tabs.size(); i++) {
            String url = tabs.get(i).getUrl();
            if (url != null && url.length() > 0 && !BrowserApp.BLANK.equals(url)) {
                bookmarks.add(folder, BookmarkNode.bookmark(tabs.get(i).getTitleText(), url));
                added++;
            }
        }
        showStorageResult(getActiveTab(), Texts.of("Bookmarked " + added + " tabs in: ", "탭 " + added + "개를 북마크에 추가했습니다: ") + name.trim());
    }

    /**
     * Shows the result of a change of the user data in the status bar: the message of success, or what went wrong when it was saved.
     * <p>
     * 사용자 데이터 변경의 결과를 상태 표시줄에 보인다: 성공 메시지, 또는 저장할 때 잘못된 것.
     *
     * @param tab     the tab whose status bar shows the message / 메시지를 보일 탭
     * @param success the message of success / 성공 메시지
     */
    private void showStorageResult(BrowserTab tab, String success) {
        String problem = app.takeStorageProblem();
        tab.showNotice(problem != null ? problem : success);
    }

    /**
     * Creates the settings dialog of this window and shows it if the window is shown. The dialog is modal: with a visible window this
     * method returns after the user closed it.
     * <p>
     * 이 창의 설정 대화 상자를 만들고, 창이 보이면 대화 상자도 보인다. 대화 상자는 모달이다: 창이 보이면 사용자가 대화 상자를 닫은 뒤에 이 메서드가
     * 돌아온다.
     *
     * @return the dialog / 대화 상자
     */
    SettingsDialog showSettings() {
        SettingsDialog dialog = new SettingsDialog(this, app);
        if (isVisible()) {
            dialog.setVisible(true);
        }
        return dialog;
    }

    /**
     * Shows the bookmark manager of this window, creating it if it is not open.
     * <p>
     * 이 창의 북마크 관리 창을 보인다. 열려 있지 않으면 만든다.
     *
     * @return the manager / 관리 창
     */
    BookmarkDialog showBookmarkManager() {
        // 닫은 관리 창은 버리고 새로 만든다.
        if (bookmarkDialog == null || !bookmarkDialog.isDisplayable()) {
            bookmarkDialog = new BookmarkDialog(this, app);
        }
        if (isVisible()) {
            bookmarkDialog.setVisible(true);
            bookmarkDialog.toFront();
        }
        return bookmarkDialog;
    }

    /**
     * Returns the menu bar; for tests.
     * <p>
     * 메뉴 막대를 반환한다. 시험용이다.
     *
     * @return the menu bar / 메뉴 막대
     */
    BrowserMenu getBrowserMenu() {
        return menu;
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
        BrowserTab tab = new BrowserTab(app);
        // 빈 페이지는 컴포넌트의 처음 상태이므로 다시 읽지 않는다.
        if (url != null && url.trim().length() > 0 && !BrowserApp.BLANK.equals(url.trim())) {
            tab.open(url);
        }
        return addTab(tab, select);
    }

    /**
     * Puts a tab at the end of the tab strip.
     * <p>
     * 탭을 탭 줄 끝에 둔다.
     *
     * @param tab    the tab / 탭
     * @param select true to make it the active tab / 활성 탭으로 만들려면 true
     * @return the tab / 그 탭
     */
    private BrowserTab addTab(final BrowserTab tab, boolean select) {
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
            BrowserTab before = shownTab;
            shownTab = tab;
            navigation.switchPage(tab.getAddressText());
            // 찾기는 활성 탭에서만 한다: 이전 탭의 찾기를 끝내고, 막대가 열려 있으면 새 탭에서 같은 글을 찾는다.
            if (before != null && !before.getPanel().isDisposed()) {
                before.getPanel().clearFind();
            }
            if (findBar.isOpen()) {
                find(findBar.getText(), findBar.isMatchCase(), true);
            }
        } else {
            navigation.showPage(tab.getAddressText());
        }
        navigation.setState(tab.canGoBack(), tab.canGoForward(), tab.isLoading());
        status.show(tab.getStatusText(), tab.getPanel().getProgress(), tab.getPanel().getZoom());
        offerBlockedPopup(status, tab);
        findBar.showResult(tab.getPanel().getFindResult());
        // 열려 있는 보조 창은 활성 탭을 따라간다.
        if (consoleWindow != null) {
            consoleWindow.showLog(tab.getConsole(), tab.getTitleText());
        }
        if (historyDialog != null) {
            historyDialog.showTab(tab);
        }
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
        // 마지막 창을 닫는 것은 프로그램을 끝내는 것이므로, 다음 시작을 위해 지금의 탭들을 기억해 둔다.
        boolean last = app.getWindows().size() == 1;
        // 탭이 여럿이면 설정에 따라 닫기 전에 확인한다.
        if (tabs.size() > 1 && app.getSettings().getBoolean(Settings.CONFIRM_CLOSE_TABS)) {
            String question = Texts.of("Close this window with its " + tabs.size() + " tabs?", "탭 " + tabs.size() + "개가 있는 이 창을 닫을까요?");
            if (!app.getPrompts().confirm(this, question)) {
                return false;
            }
        }
        if (last) {
            app.lastWindowClosing();
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
        savePlace();
        closed = true;
        app.getSettings().removeListener(this);
        if (bookmarkDialog != null) {
            bookmarkDialog.dispose();
            bookmarkDialog = null;
        }
        if (consoleWindow != null) {
            consoleWindow.close();
            consoleWindow = null;
        }
        if (historyDialog != null) {
            historyDialog.showTab(null);
            historyDialog.dispose();
            historyDialog = null;
        }
        // 이 창에 속한 팝업 창을 먼저 닫는다.
        List<PopupWindow> open = new ArrayList<PopupWindow>(popups);
        for (int i = 0; i < open.size(); i++) {
            open.get(i).close();
        }
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
