package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The menu bar of a browser window (plan item P31-05). Every item runs one command of {@link Commands} through
 * {@link BrowserWindow#perform(String)} and has the name of the command as its component name. The accelerator keys of the items work
 * while the window is showing, wherever the keyboard focus is, unless the focused component consumed the key (a page consumes the keys
 * that it handles itself, a text field its editing keys).
 * <p>
 * Items that cannot do anything right now (going back without history, reopening a tab when none was closed) are disabled only while
 * their menu is open; a closed menu keeps all items enabled so that an accelerator is never blocked by an outdated state.
 * <p>
 * 브라우저 창의 메뉴 막대(계획 P31-05). 모든 항목은 {@link Commands} 의 명령 하나를 {@link BrowserWindow#perform(String)} 으로 실행하고, 명령
 * 이름을 컴포넌트 이름으로 가진다. 항목의 가속 키는 창이 보이는 동안 키보드 포커스가 어디에 있든 동작한다. 단, 포커스를 가진 컴포넌트가 그 키를
 * 소비하면 동작하지 않는다(페이지는 스스로 처리하는 키를, 텍스트 필드는 자기 편집 키를 소비한다).
 * <p>
 * 지금 할 수 있는 일이 없는 항목(이력 없이 뒤로 가기, 닫은 탭이 없을 때 다시 열기)은 그 메뉴가 열려 있는 동안에만 비활성화한다. 닫힌 메뉴는 모든
 * 항목을 활성 상태로 두어 가속 키가 낡은 상태 때문에 막히지 않게 한다.
 */
final class BrowserMenu extends JMenuBar {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The window that runs the commands. / 명령을 실행하는 창. */
    private final transient BrowserWindow window;

    /** The items by command name. / 명령 이름별 항목. */
    private final transient Map<String, JMenuItem> items = new LinkedHashMap<String, JMenuItem>();

    /** The bookmark menu, filled each time it opens. / 열릴 때마다 채우는 북마크 메뉴. */
    private final JMenu bookmarks;

    /** The number of fixed items at the top of the bookmark menu. / 북마크 메뉴 위쪽의 고정 항목 수. */
    private final int fixedBookmarkItems;

    /**
     * Creates the menu bar of a window.
     * <p>
     * 창의 메뉴 막대를 만든다.
     *
     * @param window the window that runs the commands / 명령을 실행할 창
     */
    BrowserMenu(BrowserWindow window) {
        this.window = window;
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        int shift = InputEvent.SHIFT_MASK;

        JMenu file = menu(Texts.of("File", "파일(F)"), 'F');
        item(file, Commands.NEW_TAB, Texts.of("New Tab", "새 탭"), KeyStroke.getKeyStroke(KeyEvent.VK_T, menuKey));
        item(file, Commands.NEW_WINDOW, Texts.of("New Window", "새 창"), KeyStroke.getKeyStroke(KeyEvent.VK_N, menuKey));
        file.addSeparator();
        item(file, Commands.OPEN_LOCATION, Texts.of("Open Location...", "위치 열기..."), KeyStroke.getKeyStroke(KeyEvent.VK_L, menuKey));
        item(file, Commands.OPEN_FILE, Texts.of("Open File...", "파일 열기..."), KeyStroke.getKeyStroke(KeyEvent.VK_O, menuKey));
        item(file, Commands.SAVE_PAGE, Texts.of("Save Page As...", "페이지 저장..."), KeyStroke.getKeyStroke(KeyEvent.VK_S, menuKey));
        file.addSeparator();
        item(file, Commands.PAGE_SETUP, Texts.of("Page Setup...", "페이지 설정..."), null);
        item(file, Commands.PRINT_PREVIEW, Texts.of("Print Preview", "인쇄 미리 보기"), null);
        item(file, Commands.PRINT, Texts.of("Print...", "인쇄..."), KeyStroke.getKeyStroke(KeyEvent.VK_P, menuKey));
        file.addSeparator();
        item(file, Commands.CLOSE_TAB, Texts.of("Close Tab", "탭 닫기"), KeyStroke.getKeyStroke(KeyEvent.VK_W, menuKey));
        item(file, Commands.CLOSE_WINDOW, Texts.of("Close Window", "창 닫기"), KeyStroke.getKeyStroke(KeyEvent.VK_W, menuKey | shift));
        item(file, Commands.EXIT, Texts.of("Exit", "끝내기"), null);

        JMenu edit = menu(Texts.of("Edit", "편집(E)"), 'E');
        item(edit, Commands.CUT, Texts.of("Cut", "잘라내기"), KeyStroke.getKeyStroke(KeyEvent.VK_X, menuKey));
        item(edit, Commands.COPY, Texts.of("Copy", "복사"), KeyStroke.getKeyStroke(KeyEvent.VK_C, menuKey));
        item(edit, Commands.PASTE, Texts.of("Paste", "붙여넣기"), KeyStroke.getKeyStroke(KeyEvent.VK_V, menuKey));
        item(edit, Commands.SELECT_ALL, Texts.of("Select All", "모두 선택"), KeyStroke.getKeyStroke(KeyEvent.VK_A, menuKey));
        edit.addSeparator();
        item(edit, Commands.FIND, Texts.of("Find in Page...", "페이지에서 찾기..."), KeyStroke.getKeyStroke(KeyEvent.VK_F, menuKey));
        item(edit, Commands.FIND_NEXT, Texts.of("Find Next", "다음 찾기"), KeyStroke.getKeyStroke(KeyEvent.VK_G, menuKey));
        item(edit, Commands.FIND_PREVIOUS, Texts.of("Find Previous", "이전 찾기"), KeyStroke.getKeyStroke(KeyEvent.VK_G, menuKey | shift));

        JMenu view = menu(Texts.of("View", "보기(V)"), 'V');
        check(view, Commands.TOGGLE_TOOLBAR, Texts.of("Toolbar", "도구 모음"));
        check(view, Commands.TOGGLE_STATUS_BAR, Texts.of("Status Bar", "상태 표시줄"));
        view.addSeparator();
        item(view, Commands.ZOOM_IN, Texts.of("Zoom In", "확대"), KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuKey));
        item(view, Commands.ZOOM_OUT, Texts.of("Zoom Out", "축소"), KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuKey));
        item(view, Commands.ZOOM_RESET, Texts.of("Actual Size", "원래 크기"), KeyStroke.getKeyStroke(KeyEvent.VK_0, menuKey));
        view.addSeparator();
        item(view, Commands.VIEW_SOURCE, Texts.of("Page Source", "페이지 원본"), KeyStroke.getKeyStroke(KeyEvent.VK_U, menuKey));
        item(view, Commands.CONSOLE, Texts.of("Console", "콘솔"), KeyStroke.getKeyStroke(KeyEvent.VK_J, menuKey | shift));

        JMenu go = menu(Texts.of("Go", "이동(G)"), 'G');
        item(go, Commands.BACK, Texts.of("Back", "뒤로"), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK));
        item(go, Commands.FORWARD, Texts.of("Forward", "앞으로"), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK));
        item(go, Commands.HOME, Texts.of("Home", "홈"), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.ALT_MASK));
        go.addSeparator();
        item(go, Commands.RELOAD, Texts.of("Reload", "새로고침"), KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        item(go, Commands.RELOAD_HARD, Texts.of("Reload Without Cache", "캐시 무시 새로고침"), KeyStroke.getKeyStroke(KeyEvent.VK_F5, menuKey));
        item(go, Commands.STOP, Texts.of("Stop", "멈춤"), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        go.addSeparator();
        item(go, Commands.HISTORY, Texts.of("History", "방문 기록"), KeyStroke.getKeyStroke(KeyEvent.VK_H, menuKey));

        JMenu tabs = menu(Texts.of("Tabs", "탭(T)"), 'T');
        item(tabs, Commands.NEXT_TAB, Texts.of("Next Tab", "다음 탭"), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK));
        item(tabs, Commands.PREVIOUS_TAB, Texts.of("Previous Tab", "이전 탭"), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK | shift));
        tabs.addSeparator();
        item(tabs, Commands.REOPEN_TAB, Texts.of("Reopen Closed Tab", "닫은 탭 다시 열기"), KeyStroke.getKeyStroke(KeyEvent.VK_T, menuKey | shift));

        bookmarks = menu(Texts.of("Bookmarks", "북마크(B)"), 'B');
        item(bookmarks, Commands.ADD_BOOKMARK, Texts.of("Bookmark This Page", "이 페이지를 북마크에 추가"), KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey));
        item(bookmarks, Commands.BOOKMARK_ALL_TABS, Texts.of("Bookmark All Tabs...", "모든 탭을 북마크에 추가..."),
                KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey | shift));
        item(bookmarks, Commands.MANAGE_BOOKMARKS, Texts.of("Manage Bookmarks...", "북마크 관리..."), KeyStroke.getKeyStroke(KeyEvent.VK_B, menuKey | shift));
        fixedBookmarkItems = bookmarks.getMenuComponentCount();

        JMenu tools = menu(Texts.of("Tools", "도구(O)"), 'O');
        item(tools, Commands.SETTINGS, Texts.of("Settings...", "설정..."), null);
        item(tools, Commands.CLEAR_PRIVATE_DATA, Texts.of("Clear Private Data...", "개인 정보 삭제..."),
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, menuKey | shift));

        JMenu help = menu(Texts.of("Help", "도움말(H)"), 'H');
        item(help, Commands.ABOUT, Texts.of("About", "정보"), null);

        MenuListener states = new MenuListener() {
            /**
             * Brings the items of the opened menu up to date.
             * <p>
             * 열린 메뉴의 항목을 최신 상태로 만든다.
             *
             * @param e the event / 이벤트
             */
            public void menuSelected(MenuEvent e) {
                refreshItems(e.getSource());
            }

            /**
             * Enables all items again so that no accelerator is blocked.
             * <p>
             * 가속 키가 막히지 않도록 모든 항목을 다시 활성화한다.
             *
             * @param e the event / 이벤트
             */
            public void menuDeselected(MenuEvent e) {
                enableAll();
            }

            /**
             * Enables all items again so that no accelerator is blocked.
             * <p>
             * 가속 키가 막히지 않도록 모든 항목을 다시 활성화한다.
             *
             * @param e the event / 이벤트
             */
            public void menuCanceled(MenuEvent e) {
                enableAll();
            }
        };
        // 메뉴마다 상태 갱신을 건다.
        for (int i = 0; i < getMenuCount(); i++) {
            getMenu(i).addMenuListener(states);
        }
    }

    /**
     * Creates a menu and adds it to the bar.
     * <p>
     * 메뉴를 만들어 막대에 더한다.
     *
     * @param text     the text of the menu / 메뉴의 글
     * @param mnemonic the letter that opens the menu together with Alt / Alt 와 함께 눌러 메뉴를 여는 글자
     * @return the menu / 메뉴
     */
    private JMenu menu(String text, char mnemonic) {
        JMenu m = new JMenu(text);
        m.setMnemonic(mnemonic);
        add(m);
        return m;
    }

    /**
     * Adds an item that runs a command.
     * <p>
     * 명령을 실행하는 항목을 더한다.
     *
     * @param menu    the menu / 메뉴
     * @param command the command name / 명령 이름
     * @param text    the text of the item / 항목의 글
     * @param key     the accelerator key, or null / 가속 키. 없으면 null
     * @return the item / 항목
     */
    private JMenuItem item(JMenu menu, String command, String text, KeyStroke key) {
        JMenuItem it = new JMenuItem(text);
        wire(it, command, key);
        menu.add(it);
        return it;
    }

    /**
     * Adds a check item that runs a command.
     * <p>
     * 명령을 실행하는 체크 항목을 더한다.
     *
     * @param menu    the menu / 메뉴
     * @param command the command name / 명령 이름
     * @param text    the text of the item / 항목의 글
     * @return the item / 항목
     */
    private JMenuItem check(JMenu menu, String command, String text) {
        JMenuItem it = new JCheckBoxMenuItem(text);
        wire(it, command, null);
        menu.add(it);
        return it;
    }

    /**
     * Gives an item its name, accelerator and action.
     * <p>
     * 항목에 이름, 가속 키, 동작을 준다.
     *
     * @param it      the item / 항목
     * @param command the command name / 명령 이름
     * @param key     the accelerator key, or null / 가속 키. 없으면 null
     */
    private void wire(JMenuItem it, final String command, KeyStroke key) {
        it.setName(command);
        if (key != null) {
            it.setAccelerator(key);
        }
        it.addActionListener(new ActionListener() {
            /** Runs the command of the item. / 항목의 명령을 실행한다. */
            public void actionPerformed(ActionEvent e) {
                window.perform(command);
            }
        });
        items.put(command, it);
    }

    /**
     * Returns the item of a command; for tests.
     * <p>
     * 명령의 항목을 반환한다. 시험용이다.
     *
     * @param command the command name / 명령 이름
     * @return the item, or null if the menu has none for the command / 항목. 그 명령의 항목이 없으면 null
     */
    JMenuItem getItem(String command) {
        return items.get(command);
    }

    /**
     * Returns the names of all commands that have a menu item; for tests.
     * <p>
     * 메뉴 항목이 있는 모든 명령의 이름을 반환한다. 시험용이다.
     *
     * @return the names in menu order / 메뉴 순서대로의 이름
     */
    List<String> getCommands() {
        return new ArrayList<String>(items.keySet());
    }

    /**
     * Returns the bookmark menu; for tests.
     * <p>
     * 북마크 메뉴를 반환한다. 시험용이다.
     *
     * @return the menu / 메뉴
     */
    JMenu getBookmarkMenu() {
        return bookmarks;
    }

    /**
     * Brings the items up to date for the state of the window, as it is done when a menu opens.
     * <p>
     * 메뉴가 열릴 때 하듯이 항목을 창의 상태에 맞게 갱신한다.
     *
     * @param menu the menu that opens / 열리는 메뉴
     */
    void refreshItems(Object menu) {
        BrowserTab tab = window.getActiveTab();
        items.get(Commands.BACK).setEnabled(tab.canGoBack());
        items.get(Commands.FORWARD).setEnabled(tab.canGoForward());
        items.get(Commands.STOP).setEnabled(tab.isLoading());
        items.get(Commands.REOPEN_TAB).setEnabled(window.hasClosedTabs());
        items.get(Commands.NEXT_TAB).setEnabled(window.getTabs().size() > 1);
        items.get(Commands.PREVIOUS_TAB).setEnabled(window.getTabs().size() > 1);
        items.get(Commands.TOGGLE_TOOLBAR).setSelected(window.isToolbarShown());
        items.get(Commands.TOGGLE_STATUS_BAR).setSelected(window.isStatusBarShown());
        // 북마크 메뉴는 열릴 때마다 지금의 북마크로 다시 채운다.
        if (menu == bookmarks) {
            // 고정 항목 뒤의 것을 모두 지운 뒤 다시 만든다.
            while (bookmarks.getMenuComponentCount() > fixedBookmarkItems) {
                bookmarks.remove(bookmarks.getMenuComponentCount() - 1);
            }
            window.fillBookmarkMenu(bookmarks);
        }
    }

    /**
     * Enables every item, as it is done when a menu closes.
     * <p>
     * 메뉴가 닫힐 때 하듯이 모든 항목을 활성화한다.
     */
    void enableAll() {
        // 항목마다 활성 상태로 되돌린다.
        for (JMenuItem it : items.values()) {
            it.setEnabled(true);
        }
    }
}
