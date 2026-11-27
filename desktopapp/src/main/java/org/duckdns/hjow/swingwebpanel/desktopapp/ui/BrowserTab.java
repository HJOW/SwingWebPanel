package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WindowRequest;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.AddressInput;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ConsoleLog;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ZoomSteps;

/**
 * One page of the browser: a {@link SwingWebPanel} in a scroll pane together with what the window needs to know about it (plan items
 * P31-03 and P31-04). A window shows the state of its active tab in the title bar, the navigation toolbar and the status bar; the tab
 * tells its listener whenever that state changes. Use it on the event dispatch thread.
 * <p>
 * 브라우저의 페이지 하나: 스크롤 창 안의 {@link SwingWebPanel} 과, 창이 그 페이지에 대해 알아야 하는 것들(계획 P31-03, P31-04). 창은 활성 탭의
 * 상태를 제목 표시줄, 탐색 도구 모음, 상태 표시줄에 보인다. 탭은 그 상태가 바뀔 때마다 리스너에 알린다. 이벤트 디스패치 스레드에서 쓴다.
 */
public final class BrowserTab {

    /**
     * Receives the changes of a tab.
     * <p>
     * 탭의 변경을 받는다.
     */
    public interface Listener {

        /**
         * Called on the event dispatch thread when the address, title, progress, status text or zoom of the tab changed.
         * <p>
         * 탭의 주소, 제목, 진행률, 상태 문구, 확대 비율이 바뀌면 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param tab the tab / 탭
         */
        void tabChanged(BrowserTab tab);

        /**
         * Called on the event dispatch thread when a script of the page of the tab closed its window and the settings allow it.
         * <p>
         * 탭의 페이지의 스크립트가 자기 창을 닫았고 설정이 그것을 허용하면 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param tab the tab / 탭
         */
        void tabCloseRequested(BrowserTab tab);

        /**
         * Called on the event dispatch thread when a script of the page of the tab opened a window: the receiver shows the component
         * of the request somewhere.
         * <p>
         * 탭의 페이지의 스크립트가 창을 열었을 때 이벤트 디스패치 스레드에서 호출된다: 받는 쪽이 요청의 컴포넌트를 어딘가에 보인다.
         *
         * @param tab     the tab whose page opened the window / 페이지가 창을 연 탭
         * @param request the window / 창
         * @return true if the window is shown; false declines it / 창을 보였으면 true. false 이면 거절한다
         */
        boolean popupOpened(BrowserTab tab, WindowRequest request);

        /**
         * Called on the event dispatch thread when a link or a form of the page asks for a new window, or the user chose to open a
         * link in a new tab or window.
         * <p>
         * 페이지의 링크나 폼이 새 창을 요청했거나, 사용자가 링크를 새 탭이나 창에서 열기로 골랐을 때 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param tab     the tab of the page / 페이지의 탭
         * @param request what was asked for / 요청한 것
         * @return true if the receiver opened it / 받는 쪽이 열었으면 true
         */
        boolean windowRequested(BrowserTab tab, WindowRequest request);

        /**
         * Called on the event dispatch thread when the user asks for the context menu of the page.
         * <p>
         * 사용자가 페이지의 문맥 메뉴를 요청하면 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param tab  the tab / 탭
         * @param link the address of the link under the mouse, or null / 마우스 아래 링크의 주소. 없으면 null
         * @param x    the horizontal position in the component of the page / 페이지 컴포넌트 안의 가로 위치
         * @param y    the vertical position in the component of the page / 페이지 컴포넌트 안의 세로 위치
         */
        void contextMenuRequested(BrowserTab tab, String link, int x, int y);

        /**
         * Called on the event dispatch thread when a script of the page of the tab asks to print the page (<code>window.print()</code>).
         * <p>
         * 탭의 페이지의 스크립트가 페이지를 인쇄해 달라고 하면(<code>window.print()</code>) 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param tab the tab / 탭
         */
        void printRequested(BrowserTab tab);
    }

    /** The application. / 응용 프로그램. */
    private final BrowserApp app;

    /** The component that shows the page. / 페이지를 보여 주는 컴포넌트. */
    private final SwingWebPanel panel;

    /** The scroll pane around the component. / 컴포넌트를 감싼 스크롤 창. */
    private final JScrollPane scroll;

    /** The receiver of the changes, or null. / 변경을 받는 쪽. 없으면 null. */
    private Listener listener;

    /** The User-Agent of the component before the settings changed it. / 설정이 바꾸기 전의 컴포넌트 User-Agent. */
    private final String builtInUserAgent;

    /** The default zoom of the settings that this tab follows, as a factor. / 이 탭이 따르는 설정의 기본 확대 비율(배율). */
    private double followedZoom = 1.0;

    /** The User-Agent that a new component has before any setting changes it. / 설정이 바꾸기 전에 새 컴포넌트가 가지는 User-Agent. */
    private static volatile String defaultUserAgent;

    /** The address of the link under the mouse, or null. / 마우스 아래 링크의 주소. 없으면 null. */
    private String hoveredLink;

    /** The message that the status bar shows after the private data was cleared, until the tab opens a page again; or null. / 개인 정보를 지운 뒤 탭이 다시 페이지를 열 때까지 상태 표시줄에 보이는 메시지. 없으면 null. */
    private String clearedNotice;

    /** The console messages of the pages of this tab. / 이 탭의 페이지들의 콘솔 메시지. */
    private final ConsoleLog console = new ConsoleLog();

    /** The address of the popup that was blocked last on this page, or null. / 이 페이지에서 마지막으로 차단된 팝업의 주소. 없으면 null. */
    private String blockedPopup;

    /** The address that the user opened and that is still loading (shown in the address bar before the page arrives), or null. / 사용자가 열었고 아직 읽는 중인 주소(페이지가 도착하기 전에 주소 표시줄에 보인다). 없으면 null. */
    private String pendingUrl;

    /** A message of the application for the status bar that replaces the status text of the page until the page changes, or null. / 페이지가 바뀔 때까지 페이지의 상태 문구 대신 상태 표시줄에 보일 응용 프로그램의 메시지. 없으면 null. */
    private String notice;

    /**
     * Creates a tab with an empty page.
     * <p>
     * 빈 페이지가 있는 탭을 만든다.
     *
     * @param app the application / 응용 프로그램
     */
    public BrowserTab(BrowserApp app) {
        this(app, null);
    }

    /**
     * Creates a tab for the component of a window that a page opened (see {@link WindowRequest#getPanel()}); the tab shows the page
     * that the component already has or is loading.
     * <p>
     * 페이지가 연 창의 컴포넌트를 위한 탭을 만든다({@link WindowRequest#getPanel()} 참고). 탭은 컴포넌트가 이미 가졌거나 읽는 중인 페이지를
     * 보인다.
     *
     * @param app     the application / 응용 프로그램
     * @param adopted the component of the window, created by a tab of this application; null creates a tab with an empty page /
     *                이 응용 프로그램의 탭이 만든 창의 컴포넌트. null 이면 빈 페이지가 있는 탭을 만든다
     * @throws IllegalArgumentException if the component was not created by a tab of this application / 컴포넌트가 이 응용 프로그램의
     *                                  탭이 만든 것이 아니면
     */
    public BrowserTab(BrowserApp app, SwingWebPanel adopted) {
        this.app = app;
        if (adopted == null) {
            // 응용 프로그램의 모든 탭은 쿠키 저장소 하나를 함께 쓴다.
            panel = new TabPanel(app);
            if (defaultUserAgent == null) {
                defaultUserAgent = panel.getUserAgent();
            }
        } else {
            if (!(adopted instanceof TabPanel) || !((TabPanel) adopted).belongsTo(app)) {
                throw new IllegalArgumentException("the component was not created by a tab of this application");
            }
            panel = adopted;
        }
        ((TabPanel) panel).bind(this);
        // 페이지가 연 창의 컴포넌트는 여는 창의 User-Agent 를 이어받았으므로, 설정을 비웠을 때 돌아갈 값은 새 컴포넌트의 값이다.
        builtInUserAgent = adopted != null && defaultUserAgent != null ? defaultUserAgent : panel.getUserAgent();
        scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        // 스크롤 창의 Ctrl+PageUp/PageDown(가로 쪽 넘김)은 탭 전환에 쓰도록 끈다.
        unbind(scroll, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK));
        unbind(scroll, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK));
        applySettings();
        PropertyChangeListener changes = new PropertyChangeListener() {
            /**
             * Tells the window that the state of the page changed.
             * <p>
             * 페이지의 상태가 바뀌었음을 창에 알린다.
             *
             * @param e the change / 변경
             */
            public void propertyChange(PropertyChangeEvent e) {
                // 페이지가 바뀌거나 페이지가 새 상태 문구를 내면 응용 프로그램의 메시지는 물러난다.
                if (SwingWebPanel.PROPERTY_PAGE.equals(e.getPropertyName()) || SwingWebPanel.PROPERTY_STATUS_TEXT.equals(e.getPropertyName())) {
                    notice = null;
                }
                // 페이지가 바뀌면 이전 페이지에서 차단된 팝업과 가리키던 링크는 잊는다.
                if (SwingWebPanel.PROPERTY_PAGE.equals(e.getPropertyName())) {
                    blockedPopup = null;
                    hoveredLink = null;
                    // 빈 페이지가 아닌 페이지가 도착하면 개인 정보를 지웠다는 알림도 물러난다.
                    if (!BrowserApp.BLANK.equals(e.getNewValue())) {
                        clearedNotice = null;
                    }
                }
                // 페이지가 도착했거나 읽기가 끝났으면(멈춤 포함) 읽는 중이던 주소는 더 이상 보이지 않는다.
                if (SwingWebPanel.PROPERTY_PAGE.equals(e.getPropertyName())
                        || (SwingWebPanel.PROPERTY_PROGRESS.equals(e.getPropertyName()) && Integer.valueOf(100).equals(e.getNewValue()))) {
                    pendingUrl = null;
                }
                fireChanged();
            }
        };
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_PAGE, changes);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_TITLE, changes);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_PROGRESS, changes);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_STATUS_TEXT, changes);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_ZOOM, changes);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_FIND_RESULT, changes);
        panel.addConsoleListener(new ConsoleListener() {
            /**
             * Keeps the message for the console window.
             * <p>
             * 콘솔 창을 위해 메시지를 보관한다.
             *
             * @param e the message / 메시지
             */
            public void consoleMessage(ConsoleEvent e) {
                String where = e.getScriptUrl() == null || e.getScriptUrl().length() == 0 ? "" : e.getScriptUrl()
                        + (e.getLineNumber() > 0 ? ":" + e.getLineNumber() : "");
                console.add(e.getLevel().name(), e.getMessage(), where);
            }
        });
        panel.addHyperlinkListener(new HyperlinkListener() {
            /**
             * Remembers the link under the mouse for the context menu.
             * <p>
             * 문맥 메뉴를 위해 마우스 아래의 링크를 기억한다.
             *
             * @param e the event / 이벤트
             */
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    hoveredLink = e.getURL() != null ? e.getURL().toString() : e.getDescription();
                } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    hoveredLink = null;
                }
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            /**
             * Asks for the context menu where the platform shows it on a press.
             * <p>
             * 플랫폼이 누를 때 문맥 메뉴를 보이면 그때 문맥 메뉴를 요청한다.
             *
             * @param e the event / 이벤트
             */
            public void mousePressed(MouseEvent e) {
                askForMenu(e);
            }

            /**
             * Asks for the context menu where the platform shows it on a release.
             * <p>
             * 플랫폼이 뗄 때 문맥 메뉴를 보이면 그때 문맥 메뉴를 요청한다.
             *
             * @param e the event / 이벤트
             */
            public void mouseReleased(MouseEvent e) {
                askForMenu(e);
            }
        });
    }

    /**
     * Passes a mouse event that asks for the context menu to the window.
     * <p>
     * 문맥 메뉴를 요청하는 마우스 이벤트를 창에 넘긴다.
     *
     * @param e the event / 이벤트
     */
    private void askForMenu(MouseEvent e) {
        if (e.isPopupTrigger() && listener != null) {
            listener.contextMenuRequested(this, hoveredLink, e.getX(), e.getY());
        }
    }

    /**
     * Returns the address of the link under the mouse.
     * <p>
     * 마우스 아래 링크의 주소를 반환한다.
     *
     * @return the address, or null / 주소. 없으면 null
     */
    public String getHoveredLink() {
        return hoveredLink;
    }

    /**
     * Passes a window that a script of the page opened to the window of the tab.
     * <p>
     * 페이지의 스크립트가 연 창을 탭의 창에 넘긴다.
     *
     * @param request the window / 창
     * @return true if it is shown / 보였으면 true
     */
    boolean popupOpened(WindowRequest request) {
        return listener != null && listener.popupOpened(this, request);
    }

    /**
     * Passes what a link, a form or the user asks to open elsewhere to the window of the tab.
     * <p>
     * 링크, 폼, 사용자가 다른 곳에서 열어 달라고 한 것을 탭의 창에 넘긴다.
     *
     * @param request what was asked for / 요청한 것
     * @return true if it was opened / 열었으면 true
     */
    boolean windowRequested(WindowRequest request) {
        return listener != null && listener.windowRequested(this, request);
    }

    /**
     * Notes that the popup blocker refused a window of the page: the status bar tells so and offers to open it.
     * <p>
     * 팝업 차단이 페이지의 창을 거부했음을 기록한다: 상태 표시줄이 그것을 알리고 열기를 제안한다.
     *
     * @param request the blocked window / 차단된 창
     */
    void popupBlocked(WindowRequest request) {
        String url = request.getUrl();
        blockedPopup = url == null ? "" : url;
        notice = Texts.of("A popup was blocked", "팝업을 차단했습니다") + (blockedPopup.length() == 0 ? "" : ": " + blockedPopup);
        fireChanged();
    }

    /**
     * Returns the address of the popup that was blocked last on this page, for the button that opens it.
     * <p>
     * 이 페이지에서 마지막으로 차단된 팝업의 주소를 반환한다. 그것을 여는 단추에 쓴다.
     *
     * @return the address (empty for a popup without one), or null if none was blocked / 주소(주소 없는 팝업이면 빈 문자열). 차단된 것이
     *         없으면 null
     */
    public String getBlockedPopup() {
        return blockedPopup;
    }

    /**
     * Forgets the blocked popup, after the user opened it.
     * <p>
     * 사용자가 연 뒤에 차단된 팝업을 잊는다.
     */
    void clearBlockedPopup() {
        blockedPopup = null;
        notice = null;
        fireChanged();
    }

    /**
     * Asks the window of the tab to print the page, as a script of the page asked.
     * <p>
     * 페이지의 스크립트가 요청한 대로, 탭의 창에 페이지를 인쇄해 달라고 요청한다.
     */
    void requestPrint() {
        if (listener != null) {
            listener.printRequested(this);
        }
    }

    /**
     * Asks the window of the tab to close this tab, as a script of its page asked.
     * <p>
     * 페이지의 스크립트가 요청한 대로, 탭의 창에 이 탭을 닫아 달라고 요청한다.
     */
    void requestClose() {
        if (listener != null) {
            listener.tabCloseRequested(this);
        }
    }

    /**
     * Takes a key away from a component that handles it for the components inside it, so that the key reaches the bindings of the
     * window.
     * <p>
     * 안쪽 컴포넌트를 위해 키를 처리하는 컴포넌트에서 그 키를 떼어, 키가 창의 바인딩에 닿게 한다.
     *
     * @param component the component / 컴포넌트
     * @param key       the key / 키
     */
    static void unbind(JComponent component, KeyStroke key) {
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, "none");
    }

    /**
     * Applies the settings of the application to the component of this tab: scripts, closing by scripts, the User-Agent, the default
     * fonts and the default zoom. Only what differs is changed, so a call with unchanged settings does nothing. A tab whose zoom the
     * user changed keeps it when the default zoom changes.
     * <p>
     * 응용 프로그램의 설정을 이 탭의 컴포넌트에 적용한다: 스크립트, 스크립트의 닫기, User-Agent, 기본 글꼴, 기본 확대 비율. 다른 것만 바꾸므로 설정이
     * 그대로이면 아무 일도 하지 않는다. 사용자가 확대 비율을 바꾼 탭은 기본 확대 비율이 바뀌어도 그대로 둔다.
     */
    void applySettings() {
        if (panel.isDisposed()) {
            return;
        }
        Settings s = app.getSettings();
        boolean scripts = s.getBoolean(Settings.JAVASCRIPT);
        if (panel.isJavascriptEnabled() != scripts) {
            panel.setJavascriptEnabled(scripts);
        }
        boolean closing = s.getBoolean(Settings.SCRIPTS_CLOSE_WINDOWS);
        if (panel.isRootWindowClosingEnabled() != closing) {
            panel.setRootWindowClosingEnabled(closing);
        }
        boolean blocking = s.getBoolean(Settings.BLOCK_POPUPS);
        if (panel.isPopupBlockingEnabled() != blocking) {
            panel.setPopupBlockingEnabled(blocking);
        }
        String agent = s.get(Settings.USER_AGENT).trim();
        String wanted = agent.length() == 0 ? builtInUserAgent : agent;
        if (!wanted.equals(panel.getUserAgent())) {
            try {
                panel.setUserAgent(wanted);
            } catch (IllegalArgumentException e) {
                // 줄 바꿈이 든 값처럼 컴포넌트가 받지 않는 글은 건너뛰고 지금의 값을 둔다.
                agent = "";
            }
        }
        String[][] fonts = { { Settings.FONT_STANDARD, SwingWebPanel.FONT_STANDARD }, { Settings.FONT_SERIF, SwingWebPanel.FONT_SERIF },
                { Settings.FONT_SANS_SERIF, SwingWebPanel.FONT_SANS_SERIF }, { Settings.FONT_MONOSPACE, SwingWebPanel.FONT_MONOSPACE } };
        // 종류마다 달라진 글꼴 패밀리를 적용한다.
        for (int i = 0; i < fonts.length; i++) {
            String family = s.get(fonts[i][0]).trim();
            if (!family.equals(panel.getDefaultFontFamily(fonts[i][1]))) {
                try {
                    panel.setDefaultFontFamily(fonts[i][1], family);
                } catch (IllegalArgumentException e) {
                    // 글꼴 이름이 아닌 글은 건너뛴다.
                    family = "";
                }
            }
        }
        int size = s.getInt(Settings.FONT_SIZE, 6, 72);
        if (size != panel.getDefaultFontSize()) {
            panel.setDefaultFontSize(size);
        }
        int mono = s.getInt(Settings.FONT_MONOSPACE_SIZE, 0, 72);
        // 0 은 "주변 글의 크기"이고 그 밖에는 6 이상이어야 한다.
        if (mono != 0 && mono < 6) {
            mono = 0;
        }
        if (mono != panel.getDefaultMonospaceFontSize()) {
            panel.setDefaultMonospaceFontSize(mono);
        }
        double zoom = ZoomSteps.parsePercent(s.get(Settings.DEFAULT_ZOOM), 1.0);
        // 기본 확대 비율이 바뀌었고 탭이 이전 기본값을 그대로 쓰고 있었으면 새 기본값을 따른다.
        if (Math.abs(zoom - followedZoom) > 0.0005) {
            if (Math.abs(panel.getZoom() - followedZoom) < 0.0005) {
                panel.setZoom(zoom);
            }
            followedZoom = zoom;
        }
    }

    /**
     * Sets the receiver of the changes of this tab.
     * <p>
     * 이 탭의 변경을 받을 쪽을 정한다.
     *
     * @param listener the receiver or null / 받는 쪽 또는 null
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Tells the listener that the state changed.
     * <p>
     * 상태가 바뀌었음을 리스너에 알린다.
     */
    private void fireChanged() {
        if (listener != null) {
            listener.tabChanged(this);
        }
    }

    /**
     * Returns the component that shows the page.
     * <p>
     * 페이지를 보여 주는 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public SwingWebPanel getPanel() {
        return panel;
    }

    /**
     * Returns the Swing component of the tab (the scroll pane with the page).
     * <p>
     * 탭의 Swing 컴포넌트(페이지가 든 스크롤 창)를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public JComponent getComponent() {
        return scroll;
    }

    /**
     * Returns the address of the page that is shown.
     * <p>
     * 보이는 페이지의 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public String getUrl() {
        return panel.getPage();
    }

    /**
     * Returns the address for the address bar: the address that is being loaded if the user opened one and it has not arrived yet,
     * otherwise the address of the page that is shown.
     * <p>
     * 주소 표시줄에 보일 주소를 반환한다: 사용자가 연 주소가 아직 도착하지 않았으면 읽는 중인 그 주소, 아니면 보이는 페이지의 주소.
     *
     * @return the address / 주소
     */
    public String getAddressText() {
        return pendingUrl != null ? pendingUrl : panel.getPage();
    }

    /**
     * Returns the text that names the page: its title, or its address when it has no title, or a fixed text for the empty page.
     * <p>
     * 페이지를 부르는 글을 반환한다: 제목, 제목이 없으면 주소, 빈 페이지이면 정해진 글.
     *
     * @return the text / 글
     */
    public String getTitleText() {
        String title = panel.getTitle();
        if (title != null && title.trim().length() > 0) {
            return title.trim();
        }
        String url = panel.getPage();
        // 제목 없는 빈 페이지는 정해진 이름으로 부른다.
        if (url == null || url.length() == 0 || BrowserApp.BLANK.equals(url)) {
            return Texts.of("Blank page", "빈 페이지");
        }
        return url;
    }

    /**
     * Tells whether the page is loading.
     * <p>
     * 페이지를 읽는 중인지 알려 준다.
     *
     * @return true while loading / 읽는 동안 true
     */
    public boolean isLoading() {
        return panel.getProgress() != 100;
    }

    /**
     * Tells whether the history has an entry behind the current one.
     * <p>
     * 이력에서 현재 항목 뒤에 항목이 있는지 알려 준다.
     *
     * @return true if going back is possible / 뒤로 갈 수 있으면 true
     */
    public boolean canGoBack() {
        return panel.getHistoryIndex() > 0;
    }

    /**
     * Tells whether the history has an entry ahead of the current one.
     * <p>
     * 이력에서 현재 항목 앞에 항목이 있는지 알려 준다.
     *
     * @return true if going forward is possible / 앞으로 갈 수 있으면 true
     */
    public boolean canGoForward() {
        return panel.getHistoryIndex() < panel.getHistories().size() - 1;
    }

    /**
     * Returns the addresses of the history entries behind the current one, the nearest first.
     * <p>
     * 현재 항목 뒤쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> backEntries() {
        Vector<String> all = panel.getHistories();
        List<String> out = new ArrayList<String>();
        // 현재 항목 바로 뒤부터 처음 항목까지 거슬러 올라간다.
        for (int i = Math.min(panel.getHistoryIndex(), all.size()) - 1; i >= 0; i--) {
            out.add(all.get(i));
        }
        return out;
    }

    /**
     * Returns the addresses of the history entries ahead of the current one, the nearest first.
     * <p>
     * 현재 항목 앞쪽의 이력 주소를 가까운 것부터 반환한다.
     *
     * @return the addresses / 주소
     */
    public List<String> forwardEntries() {
        Vector<String> all = panel.getHistories();
        List<String> out = new ArrayList<String>();
        // 현재 항목 바로 앞부터 마지막 항목까지 간다.
        for (int i = panel.getHistoryIndex() + 1; i < all.size(); i++) {
            out.add(all.get(i));
        }
        return out;
    }

    /**
     * Opens what the user typed: an address, a host, a file path or a search (see {@link AddressInput}). Text that cannot be opened
     * leaves the page as it is and shows a message in the status bar.
     * <p>
     * 사용자가 입력한 것을 연다: 주소, 호스트, 파일 경로, 검색({@link AddressInput} 참고). 열 수 없는 글이면 페이지를 그대로 두고 상태 표시줄에
     * 메시지를 보인다.
     *
     * @param text the typed text / 입력한 글
     * @return the address that is being loaded, or null if nothing was opened / 읽기 시작한 주소. 아무것도 열지 않았으면 null
     */
    public String open(String text) {
        AddressInput.Result r = AddressInput.resolve(text, app.getSettings().get(Settings.SEARCH_TEMPLATE));
        // 검색 서식이 없어 검색할 수 없는 글은 메시지로 알린다.
        if (r.getKind() == AddressInput.Kind.NO_SEARCH) {
            showNotice(Texts.of("This is not an address, and no search address is set in the settings.",
                    "주소가 아니며, 설정에 검색 주소가 없습니다."));
            return null;
        }
        if (r.getUrl() == null) {
            return null;
        }
        return navigate(r.getUrl()) ? r.getUrl() : null;
    }

    /**
     * Starts loading an absolute address. An address that the component cannot take leaves the page as it is and shows a message in the
     * status bar.
     * <p>
     * 절대 주소 읽기를 시작한다. 컴포넌트가 받을 수 없는 주소이면 페이지를 그대로 두고 상태 표시줄에 메시지를 보인다.
     *
     * @param url the absolute address / 절대 주소
     * @return true if loading started / 읽기를 시작했으면 true
     */
    public boolean navigate(String url) {
        try {
            panel.setPage(url);
            pendingUrl = url;
            clearedNotice = null;
            fireChanged();
            return true;
        } catch (IllegalArgumentException e) {
            showNotice(Texts.of("This address cannot be opened: ", "열 수 없는 주소입니다: ") + url);
            return false;
        }
    }

    /**
     * Returns the console messages of the pages of this tab.
     * <p>
     * 이 탭의 페이지들의 콘솔 메시지를 반환한다.
     *
     * @return the log / 로그
     */
    public ConsoleLog getConsole() {
        return console;
    }

    /**
     * Goes to an entry of the session history of this tab by going back or forward as many entries as it is away.
     * <p>
     * 이 탭의 세션 이력의 한 항목으로 간다. 떨어진 칸 수만큼 뒤로 또는 앞으로 간다.
     *
     * @param index the position of the entry in <code>getHistories()</code>; the current one or a position outside does nothing /
     *              <code>getHistories()</code> 안에서 항목의 위치. 지금의 항목이거나 범위 밖이면 아무것도 하지 않는다
     * @return true if a navigation was started / 이동을 시작했으면 true
     */
    public boolean goToHistory(int index) {
        int current = panel.getHistoryIndex();
        if (index < 0 || index >= panel.getHistories().size() || index == current) {
            return false;
        }
        if (index < current) {
            panel.back(current - index);
        } else {
            panel.forward(index - current);
        }
        return true;
    }

    /**
     * Removes what this tab remembers of the pages it showed (plan item P31-09): the page, the history, the cache and the session
     * storage of the component (its <code>clean()</code>), and the console messages. The shared local storage is emptied by the same
     * call; the cookies are removed by the application.
     * <p>
     * 이 탭이 보였던 페이지에 대해 기억하는 것을 지운다(계획 P31-09): 컴포넌트의 페이지, 이력, 캐시, sessionStorage(<code>clean()</code>)와 콘솔
     * 메시지. 공유 localStorage 도 같은 호출로 비워진다. 쿠키는 응용 프로그램이 지운다.
     */
    public void clearPrivateData() {
        if (panel.isDisposed()) {
            return;
        }
        pendingUrl = null;
        blockedPopup = null;
        hoveredLink = null;
        notice = null;
        clearedNotice = Texts.of("The private data was cleared.", "개인 정보를 지웠습니다.");
        panel.clean();
        console.clear();
        fireChanged();
    }

    /**
     * Opens the home page of the settings.
     * <p>
     * 설정의 홈 페이지를 연다.
     */
    public void home() {
        String home = app.getSettings().get(Settings.HOME_PAGE);
        open(home.length() == 0 ? BrowserApp.BLANK : home);
    }

    /**
     * Shows a message of the application in the status bar until the page changes.
     * <p>
     * 페이지가 바뀔 때까지 응용 프로그램의 메시지를 상태 표시줄에 보인다.
     *
     * @param message the message / 메시지
     */
    public void showNotice(String message) {
        notice = message;
        fireChanged();
    }

    /**
     * Returns the text for the status bar: the message of the application if there is one, otherwise the status text of the page.
     * <p>
     * 상태 표시줄에 보일 글을 반환한다: 응용 프로그램의 메시지가 있으면 그것을, 없으면 페이지의 상태 문구를 반환한다.
     *
     * @return the text / 글
     */
    public String getStatusText() {
        if (notice != null) {
            return notice;
        }
        return clearedNotice != null ? clearedNotice : panel.getStatusText();
    }

    /**
     * Closes the tab: disposes its component. The local storage that all tabs share stays. Calling it again does nothing.
     * <p>
     * 탭을 닫는다: 그 컴포넌트를 폐기한다. 모든 탭이 함께 쓰는 localStorage 는 남는다. 다시 불러도 아무 일도 없다.
     */
    public void close() {
        listener = null;
        ((TabPanel) panel).bind(null);
        panel.disposeKeepingStorage();
    }
}
