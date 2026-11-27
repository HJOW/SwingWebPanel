package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The navigation toolbar of a browser window (plan item P31-03): the back, forward, reload, stop and home buttons and the address bar.
 * The toolbar does not know the page; the window tells it what to show ({@link #showPage(String)}, {@link #setState}) and receives the
 * requests of the user through {@link Actions}.
 * <ul>
 * <li>The address bar shows the address of the page, but never overwrites text that the user is editing. Enter opens the text, Escape
 * brings the address of the page back.</li>
 * <li>A right click on the back or forward button shows the history entries in that direction.</li>
 * <li>A click on the reload button with Shift held reloads without the cache.</li>
 * </ul>
 * <p>
 * 브라우저 창의 탐색 도구 모음(계획 P31-03): 뒤로, 앞으로, 새로고침, 멈춤, 홈 버튼과 주소 표시줄. 도구 모음은 페이지를 모른다. 창이 보일 내용을
 * 알려 주고({@link #showPage(String)}, {@link #setState}) 사용자의 요청은 {@link Actions} 로 받는다.
 * <ul>
 * <li>주소 표시줄은 페이지의 주소를 보이지만 사용자가 편집 중인 글은 덮어쓰지 않는다. Enter 는 그 글을 열고 Esc 는 페이지의 주소로 되돌린다.</li>
 * <li>뒤로·앞으로 버튼을 오른쪽 클릭하면 그 방향의 이력 항목을 보인다.</li>
 * <li>Shift 를 누른 채 새로고침 버튼을 누르면 캐시 없이 다시 읽는다.</li>
 * </ul>
 */
final class NavigationBar extends JPanel {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The largest number of history entries in the menu of a button. / 버튼의 메뉴에 보이는 이력 항목의 최대 개수. */
    private static final int HISTORY_MENU_SIZE = 15;

    /**
     * What the toolbar asks the window to do.
     * <p>
     * 도구 모음이 창에 요청하는 일.
     */
    interface Actions {

        /**
         * Goes back in the history.
         * <p>
         * 이력에서 뒤로 간다.
         *
         * @param n the number of entries / 칸 수
         */
        void back(int n);

        /**
         * Goes forward in the history.
         * <p>
         * 이력에서 앞으로 간다.
         *
         * @param n the number of entries / 칸 수
         */
        void forward(int n);

        /**
         * Reloads the page.
         * <p>
         * 페이지를 다시 읽는다.
         *
         * @param force true to bypass the cache / 캐시를 우회하려면 true
         */
        void reload(boolean force);

        /**
         * Stops loading.
         * <p>
         * 읽기를 멈춘다.
         */
        void stop();

        /**
         * Opens the home page.
         * <p>
         * 홈 페이지를 연다.
         */
        void home();

        /**
         * Opens what the user typed into the address bar.
         * <p>
         * 사용자가 주소 표시줄에 입력한 것을 연다.
         *
         * @param text the text of the address bar / 주소 표시줄의 글
         * @return true if loading started, false if the text could not be opened / 읽기를 시작했으면 true, 글을 열 수 없었으면 false
         */
        boolean open(String text);

        /**
         * Opens a new tab.
         * <p>
         * 새 탭을 연다.
         */
        void newTab();

        /**
         * Returns the addresses of the history entries behind the current one, the nearest first.
         * <p>
         * 현재 항목 뒤쪽의 이력 주소를 가까운 것부터 반환한다.
         *
         * @return the addresses / 주소
         */
        List<String> backEntries();

        /**
         * Returns the addresses of the history entries ahead of the current one, the nearest first.
         * <p>
         * 현재 항목 앞쪽의 이력 주소를 가까운 것부터 반환한다.
         *
         * @return the addresses / 주소
         */
        List<String> forwardEntries();
    }

    /** The receiver of the requests. / 요청을 받는 쪽. */
    private final transient Actions actions;

    /** The back button. / 뒤로 버튼. */
    private final JButton back = button(Icons.BACK, Texts.of("Back (Alt+Left). Right-click for the history", "뒤로 (Alt+←). 오른쪽 클릭: 이력"));

    /** The forward button. / 앞으로 버튼. */
    private final JButton forward = button(Icons.FORWARD,
            Texts.of("Forward (Alt+Right). Right-click for the history", "앞으로 (Alt+→). 오른쪽 클릭: 이력"));

    /** The reload button. / 새로고침 버튼. */
    private final JButton reload = button(Icons.RELOAD, Texts.of("Reload (F5). Shift-click: without the cache", "새로고침 (F5). Shift+클릭: 캐시 없이"));

    /** The stop button. / 멈춤 버튼. */
    private final JButton stop = button(Icons.STOP, Texts.of("Stop loading (Esc)", "읽기 중지 (Esc)"));

    /** The home button. / 홈 버튼. */
    private final JButton home = button(Icons.HOME, Texts.of("Home (Alt+Home)", "홈 (Alt+Home)"));

    /** The new tab button. / 새 탭 버튼. */
    private final JButton newTab = button(Icons.PLUS, Texts.of("New tab (Ctrl+T)", "새 탭 (Ctrl+T)"));

    /** The address bar. / 주소 표시줄. */
    private final JTextField address = new JTextField();

    /** The address of the page that the window told last. / 창이 마지막으로 알려 준 페이지의 주소. */
    private String pageUrl = "";

    /** True while the user has changed the text of the address bar and not opened or reverted it. / 사용자가 주소 표시줄의 글을 바꾸고 아직 열거나 되돌리지 않은 동안 true. */
    private boolean edited;

    /** True while the toolbar itself sets the text of the address bar. / 도구 모음 자신이 주소 표시줄의 글을 정하는 동안 true. */
    private boolean programmatic;

    /**
     * Creates the toolbar.
     * <p>
     * 도구 모음을 만든다.
     *
     * @param actions the receiver of the requests of the user / 사용자의 요청을 받는 쪽
     */
    NavigationBar(Actions actions) {
        super(new BorderLayout(4, 0));
        this.actions = actions;
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 4));
        JToolBar buttons = new JToolBar() {
            /** Serialization version. / 직렬화 버전. */
            private static final long serialVersionUID = 1L;

            /**
             * Restores the Nimbus toolbar style after the ancestor notification.
             * <p>
             * 상위 컴포넌트 변경 알림 뒤 Nimbus 도구 모음 스타일을 복원한다.
             */
            public void addNotify() {
                super.addNotify();
                // Java 6 Nimbus는 상위 컴포넌트 변경 알림에서 끌기 창의 스타일로 도구 모음의 여백과 불투명도를 덮어쓴다.
                if ("Nimbus".equals(UIManager.getLookAndFeel().getID())) {
                    updateUI();
                }
            }
        };
        buttons.setFloatable(false);
        buttons.setRollover(true);
        buttons.add(back);
        buttons.add(forward);
        buttons.add(reload);
        buttons.add(stop);
        buttons.add(home);
        buttons.add(newTab);
        add(buttons, BorderLayout.WEST);
        add(address, BorderLayout.CENTER);
        address.setToolTipText(Texts.of("Address (Ctrl+L)", "주소 (Ctrl+L)"));
        wireButtons();
        wireAddress();
        setState(false, false, false);
    }

    /**
     * Creates a toolbar button with an icon and a tool tip.
     * <p>
     * 아이콘과 도구 설명이 있는 도구 모음 버튼을 만든다.
     *
     * @param icon the kind of the icon / 아이콘의 종류
     * @param tip  the tool tip / 도구 설명
     * @return the button / 버튼
     */
    private static JButton button(int icon, String tip) {
        JButton b = new JButton(Icons.of(icon));
        b.setToolTipText(tip);
        b.setFocusable(false);
        return b;
    }

    /**
     * Connects the buttons to the actions.
     * <p>
     * 버튼을 동작에 연결한다.
     */
    private void wireButtons() {
        back.addActionListener(new ActionListener() {
            /** Goes back one entry. / 한 칸 뒤로 간다. */
            public void actionPerformed(ActionEvent e) {
                actions.back(1);
            }
        });
        forward.addActionListener(new ActionListener() {
            /** Goes forward one entry. / 한 칸 앞으로 간다. */
            public void actionPerformed(ActionEvent e) {
                actions.forward(1);
            }
        });
        reload.addActionListener(new ActionListener() {
            /** Reloads; with Shift held, without the cache. / 다시 읽는다. Shift 를 누르고 있으면 캐시 없이 읽는다. */
            public void actionPerformed(ActionEvent e) {
                actions.reload((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
            }
        });
        stop.addActionListener(new ActionListener() {
            /** Stops loading. / 읽기를 멈춘다. */
            public void actionPerformed(ActionEvent e) {
                actions.stop();
            }
        });
        home.addActionListener(new ActionListener() {
            /** Opens the home page. / 홈 페이지를 연다. */
            public void actionPerformed(ActionEvent e) {
                actions.home();
            }
        });
        newTab.addActionListener(new ActionListener() {
            /** Opens a new tab. / 새 탭을 연다. */
            public void actionPerformed(ActionEvent e) {
                actions.newTab();
            }
        });
        back.addMouseListener(new HistoryMenu(true));
        forward.addMouseListener(new HistoryMenu(false));
    }

    /**
     * Shows the history entries of one direction when the button is right-clicked.
     * <p>
     * 버튼을 오른쪽 클릭하면 한 방향의 이력 항목을 보인다.
     */
    private final class HistoryMenu extends MouseAdapter {

        /** True for the back button. / 뒤로 버튼이면 true. */
        private final boolean backward;

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         *
         * @param backward true for the back button / 뒤로 버튼이면 true
         */
        HistoryMenu(boolean backward) {
            this.backward = backward;
        }

        /**
         * Shows the menu if the press is the popup trigger of the platform.
         * <p>
         * 누름이 플랫폼의 팝업 트리거이면 메뉴를 보인다.
         *
         * @param e the event / 이벤트
         */
        public void mousePressed(MouseEvent e) {
            maybeShow(e);
        }

        /**
         * Shows the menu if the release is the popup trigger of the platform.
         * <p>
         * 뗌이 플랫폼의 팝업 트리거이면 메뉴를 보인다.
         *
         * @param e the event / 이벤트
         */
        public void mouseReleased(MouseEvent e) {
            maybeShow(e);
        }

        /**
         * Shows the menu for a popup trigger.
         * <p>
         * 팝업 트리거이면 메뉴를 보인다.
         *
         * @param e the event / 이벤트
         */
        private void maybeShow(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                return;
            }
            JPopupMenu menu = historyMenu(backward);
            if (menu.getComponentCount() > 0) {
                menu.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        }
    }

    /**
     * Builds the menu of the history entries of one direction; choosing an entry moves there.
     * <p>
     * 한 방향의 이력 항목 메뉴를 만든다. 항목을 고르면 그곳으로 이동한다.
     *
     * @param backward true for the entries behind the current one / 현재 항목 뒤쪽이면 true
     * @return the menu (empty when there are no entries) / 메뉴(항목이 없으면 비어 있다)
     */
    JPopupMenu historyMenu(final boolean backward) {
        JPopupMenu menu = new JPopupMenu();
        List<String> entries = backward ? actions.backEntries() : actions.forwardEntries();
        // 가까운 항목부터 정해진 개수까지 메뉴 항목을 만든다.
        for (int i = 0; i < entries.size() && i < HISTORY_MENU_SIZE; i++) {
            final int steps = i + 1;
            JMenuItem item = new JMenuItem(shorten(entries.get(i)));
            item.addActionListener(new ActionListener() {
                /** Moves to the chosen entry. / 고른 항목으로 이동한다. */
                public void actionPerformed(ActionEvent e) {
                    if (backward) {
                        actions.back(steps);
                    } else {
                        actions.forward(steps);
                    }
                }
            });
            menu.add(item);
        }
        return menu;
    }

    /**
     * Shortens a long address for a menu item.
     * <p>
     * 메뉴 항목에 쓸 긴 주소를 줄인다.
     *
     * @param url the address / 주소
     * @return the address, cut with an ellipsis when it is long / 주소. 길면 줄임표로 자른다
     */
    private static String shorten(String url) {
        return url.length() <= 80 ? url : url.substring(0, 77) + "...";
    }

    /**
     * Connects the address bar: typing marks it as edited, Enter opens the text and Escape reverts it.
     * <p>
     * 주소 표시줄을 연결한다: 입력하면 편집 중으로 표시하고, Enter 는 글을 열며, Esc 는 되돌린다.
     */
    private void wireAddress() {
        address.getDocument().addDocumentListener(new DocumentListener() {
            /** Marks the text as edited by the user. / 사용자가 글을 편집했음을 표시한다. */
            public void insertUpdate(DocumentEvent e) {
                touched();
            }

            /** Marks the text as edited by the user. / 사용자가 글을 편집했음을 표시한다. */
            public void removeUpdate(DocumentEvent e) {
                touched();
            }

            /** Attribute changes do not change the text. / 속성 변경은 글을 바꾸지 않는다. */
            public void changedUpdate(DocumentEvent e) {
            }
        });
        address.addActionListener(new ActionListener() {
            /** Opens the typed text. / 입력한 글을 연다. */
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        address.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "revert-address");
        BrowserTab.releaseCommandKeys(address);
        address.getActionMap().put("revert-address", new AbstractAction() {
            /** Serialization version. / 직렬화 버전. */
            private static final long serialVersionUID = 1L;

            /** Brings the address of the page back. / 페이지의 주소로 되돌린다. */
            public void actionPerformed(ActionEvent e) {
                revert();
            }
        });
    }

    /**
     * Remembers that the user changed the text, unless the toolbar itself is setting it.
     * <p>
     * 도구 모음 자신이 글을 정하는 중이 아니면, 사용자가 글을 바꿨음을 기억한다.
     */
    private void touched() {
        if (!programmatic) {
            edited = true;
        }
    }

    /**
     * Sets the text of the address bar without marking it as edited by the user.
     * <p>
     * 사용자가 편집한 것으로 표시하지 않고 주소 표시줄의 글을 정한다.
     *
     * @param text the text / 글
     */
    private void setAddressText(String text) {
        programmatic = true;
        try {
            address.setText(text);
            address.setCaretPosition(0);
        } finally {
            programmatic = false;
        }
    }

    /**
     * Opens the text of the address bar, as Enter does. When loading started, the bar shows the address that is being loaded; text
     * that could not be opened stays as it is.
     * <p>
     * Enter 와 같이 주소 표시줄의 글을 연다. 읽기를 시작했으면 표시줄에 읽는 중인 주소를 보이고, 열 수 없는 글은 그대로 둔다.
     */
    void submit() {
        // 열 수 없는 글은 사용자가 고칠 수 있도록 편집 중인 글로 남긴다.
        if (actions.open(address.getText())) {
            edited = false;
            setAddressText(display(pageUrl));
        }
    }

    /**
     * Brings the address of the page back into the address bar, as Escape does.
     * <p>
     * Esc 와 같이 페이지의 주소를 주소 표시줄에 되돌린다.
     */
    void revert() {
        edited = false;
        setAddressText(display(pageUrl));
    }

    /**
     * Tells the toolbar the address that belongs to the page now: the address of the page that is shown, or the address that is being
     * loaded. The address bar takes it over unless the user is editing its text.
     * <p>
     * 지금 페이지에 해당하는 주소를 도구 모음에 알린다: 보이는 페이지의 주소, 또는 읽는 중인 주소. 사용자가 글을 편집 중이 아니면 주소 표시줄이
     * 그 주소를 보인다.
     *
     * @param url the address / 주소
     */
    void showPage(String url) {
        pageUrl = url == null ? "" : url;
        if (!edited) {
            setAddressText(display(pageUrl));
        }
    }

    /**
     * Tells the toolbar that another page became the one it serves (another tab was selected): text that the user was editing for the
     * old page is dropped.
     * <p>
     * 도구 모음이 다루는 페이지가 바뀌었음을 알린다(다른 탭을 골랐다): 사용자가 이전 페이지를 두고 편집하던 글은 버린다.
     *
     * @param url the address of the new page / 새 페이지의 주소
     */
    void switchPage(String url) {
        edited = false;
        showPage(url);
    }

    /**
     * Returns the text that the address bar shows for an address: nothing for the empty page.
     * <p>
     * 주소에 대해 주소 표시줄이 보일 글을 반환한다: 빈 페이지이면 아무것도 보이지 않는다.
     *
     * @param url the address / 주소
     * @return the text / 글
     */
    private static String display(String url) {
        return BrowserApp.BLANK.equals(url) ? "" : url;
    }

    /**
     * Enables the buttons for the state of the page.
     * <p>
     * 페이지의 상태에 맞게 버튼을 활성화한다.
     *
     * @param canBack    true if there is an entry behind the current one / 현재 항목 뒤에 항목이 있으면 true
     * @param canForward true if there is an entry ahead of the current one / 현재 항목 앞에 항목이 있으면 true
     * @param loading    true while the page loads / 페이지를 읽는 동안 true
     */
    void setState(boolean canBack, boolean canForward, boolean loading) {
        back.setEnabled(canBack);
        forward.setEnabled(canForward);
        stop.setEnabled(loading);
    }

    /**
     * Moves the keyboard focus to the address bar and selects its text.
     * <p>
     * 키보드 포커스를 주소 표시줄로 옮기고 그 글을 선택한다.
     */
    void focusAddress() {
        address.requestFocusInWindow();
        address.selectAll();
    }

    /**
     * Returns the text of the address bar.
     * <p>
     * 주소 표시줄의 글을 반환한다.
     *
     * @return the text / 글
     */
    String getAddressText() {
        return address.getText();
    }

    /**
     * Returns the text field of the address bar; for the window and for tests.
     * <p>
     * 주소 표시줄의 텍스트 필드를 반환한다. 창과 시험이 쓴다.
     *
     * @return the field / 필드
     */
    JTextField getAddressField() {
        return address;
    }

    /**
     * Types into the address bar as the user does (the text counts as edited); for tests and for commands that fill the bar.
     * <p>
     * 사용자가 하듯이 주소 표시줄에 입력한다(편집한 글로 친다). 시험과, 표시줄을 채우는 명령이 쓴다.
     *
     * @param text the text / 글
     */
    void type(String text) {
        address.setText(text);
    }

    /**
     * Returns a button of the toolbar by the kind of its icon; for tests.
     * <p>
     * 아이콘의 종류로 도구 모음의 버튼을 반환한다. 시험용이다.
     *
     * @param icon one of {@link Icons#BACK}, {@link Icons#FORWARD}, {@link Icons#RELOAD}, {@link Icons#STOP}, {@link Icons#HOME},
     *             {@link Icons#PLUS} / {@link Icons#BACK}, {@link Icons#FORWARD}, {@link Icons#RELOAD}, {@link Icons#STOP},
     *             {@link Icons#HOME}, {@link Icons#PLUS} 중 하나
     * @return the button / 버튼
     */
    JButton getButton(int icon) {
        // 종류에 맞는 버튼을 고른다.
        switch (icon) {
        case Icons.BACK:
            return back;
        case Icons.FORWARD:
            return forward;
        case Icons.RELOAD:
            return reload;
        case Icons.STOP:
            return stop;
        case Icons.PLUS:
            return newTab;
        default:
            return home;
        }
    }
}
