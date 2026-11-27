package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import javax.swing.JMenuItem;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The menu bar, the commands and the keys of a browser window (plan item P31-05): menu items run their commands, the accelerator keys
 * and the keys without a menu item reach their commands from the page and from the address bar, and the keys that the page or a text
 * field handles itself do not run a command of the window a second time.
 * <p>
 * 브라우저 창의 메뉴 막대, 명령, 키(계획 P31-05): 메뉴 항목이 그 명령을 실행하고, 가속 키와 메뉴 항목 없는 키가 페이지와 주소 표시줄에서 그 명령에
 * 닿으며, 페이지나 텍스트 필드가 스스로 처리하는 키는 창의 명령을 한 번 더 실행하지 않는다.
 */
public class BrowserMenuTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrowserMenuTest() {
    }

    /**
     * Clicks the menu item of a command.
     * <p>
     * 명령의 메뉴 항목을 누른다.
     *
     * @param command the command name / 명령 이름
     */
    private void click(String command) {
        JMenuItem item = window.getBrowserMenu().getItem(command);
        Check.notNull(item, "the menu has an item for " + command);
        Check.equal(command, item.getName(), "the item carries the command name");
        item.doClick(0);
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return its bytes / 그 바이트
     * @throws Exception on failure / 실패 시
     */
    private static byte[] read(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * The menu items for tabs, windows, the bars, zoom and the about text run their commands; an item that cannot do anything is
     * disabled only while its menu is open.
     * <p>
     * 탭, 창, 막대, 확대, 정보에 관한 메뉴 항목이 그 명령을 실행한다. 할 수 있는 일이 없는 항목은 그 메뉴가 열려 있는 동안에만 비활성화된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMenuItemsRunTheirCommands() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        onEdt(new Runnable() {
            /** Uses the tab and bar items. / 탭과 막대 항목을 쓴다. */
            public void run() {
                List<String> commands = window.getBrowserMenu().getCommands();
                Check.isTrue(commands.contains(Commands.NEW_TAB) && commands.contains(Commands.ABOUT), "the menu lists its commands: " + commands);
                window.navigate(a);
                click(Commands.NEW_TAB);
                Check.equal(2, window.getTabs().size(), "New Tab opens a tab");
                click(Commands.PREVIOUS_TAB);
                Check.same(window.getTabs().get(0), window.getActiveTab(), "Previous Tab selects the tab before");
                click(Commands.NEXT_TAB);
                Check.same(window.getTabs().get(1), window.getActiveTab(), "Next Tab selects the tab behind");
                window.getBrowserMenu().refreshItems(null);
                Check.isFalse(window.getBrowserMenu().getItem(Commands.REOPEN_TAB).isEnabled(), "nothing to reopen while the menu is open");
                Check.isFalse(window.getBrowserMenu().getItem(Commands.BACK).isEnabled(), "an empty tab has no way back");
                window.getBrowserMenu().enableAll();
                Check.isTrue(window.getBrowserMenu().getItem(Commands.BACK).isEnabled(), "a closed menu blocks no accelerator");
                click(Commands.CLOSE_TAB);
                Check.equal(1, window.getTabs().size(), "Close Tab closes the active tab");
                Check.isTrue(window.isToolbarShown() && window.isStatusBarShown(), "both bars are shown at first");
                click(Commands.TOGGLE_TOOLBAR);
                click(Commands.TOGGLE_STATUS_BAR);
                Check.isFalse(window.isToolbarShown() || window.isStatusBarShown(), "the items hide the bars");
                Check.equal("false", app.getSettings().get(Settings.SHOW_TOOLBAR), "the choice is a setting");
                window.getBrowserMenu().refreshItems(null);
                Check.isFalse(window.getBrowserMenu().getItem(Commands.TOGGLE_TOOLBAR).isSelected(), "the check mark follows the bar");
                window.getBrowserMenu().enableAll();
                click(Commands.OPEN_LOCATION);
                Check.isTrue(window.isToolbarShown(), "Open Location brings the toolbar back");
                click(Commands.TOGGLE_STATUS_BAR);
                Check.isTrue(window.isStatusBarShown(), "the item shows the status bar again");
                click(Commands.ABOUT);
                Check.contains(prompts.messages.get(prompts.messages.size() - 1), BrowserApp.NAME, "About shows the application");
                Check.contains(prompts.messages.get(prompts.messages.size() - 1), "Java " + System.getProperty("java.version"), "About shows Java");
                Check.isFalse(window.perform("no-such-command"), "an unknown command is refused");
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Zooms with the items. / 항목으로 확대한다. */
            public void run() {
                click(Commands.ZOOM_IN);
                Check.near(1.1, window.getPanel().getZoom(), 0.001, "Zoom In goes one step up");
                click(Commands.ZOOM_IN);
                click(Commands.ZOOM_OUT);
                click(Commands.ZOOM_OUT);
                click(Commands.ZOOM_OUT);
                Check.near(0.9, window.getPanel().getZoom(), 0.001, "Zoom Out goes one step down");
                Check.equal("90%", window.getStatusBar().getZoomText(), "the status bar shows the zoom");
                click(Commands.ZOOM_RESET);
                Check.near(1.0, window.getPanel().getZoom(), 0.001, "Actual Size goes back to the default zoom");
                app.getSettings().set(Settings.DEFAULT_ZOOM, "125");
                click(Commands.ZOOM_RESET);
                Check.near(1.25, window.getPanel().getZoom(), 0.001, "the default zoom is a setting");
                Check.near(1.25, window.newTab(null, false).getPanel().getZoom(), 0.001, "a new tab starts with the default zoom");
                window.closeTab(window.getTabs().get(1));
                window.getBrowserMenu().refreshItems(null);
                Check.isTrue(window.getBrowserMenu().getItem(Commands.BACK).isEnabled(), "the loaded tab has a way back");
                Check.isFalse(window.getBrowserMenu().getItem(Commands.FORWARD).isEnabled(), "but no way forward");
                window.getBrowserMenu().enableAll();
                click(Commands.BACK);
            }
        });
        await(new TestPages.Condition() {
            /** The empty page is shown again. / 빈 페이지가 다시 보인다. */
            public boolean holds() {
                return BrowserApp.BLANK.equals(window.getPanel().getPage());
            }
        }, "Back goes to the empty page");
        onEdt(new Runnable() {
            /** Goes forward again. / 다시 앞으로 간다. */
            public void run() {
                click(Commands.FORWARD);
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Opens and closes windows. / 창을 열고 닫는다. */
            public void run() {
                click(Commands.NEW_WINDOW);
                Check.equal(2, app.getWindows().size(), "New Window opens a window");
                BrowserWindow second = app.getWindows().get(1);
                Check.isFalse(second.isVisible(), "the window of a hidden window is not shown");
                second.perform(Commands.CLOSE_WINDOW);
                Check.isTrue(second.isClosed(), "Close Window closes the window");
                Check.equal(0, ended, "another window is still open");
                click(Commands.NEW_WINDOW);
                click(Commands.EXIT);
                Check.equal(0, app.getWindows().size(), "Exit closes every window");
                Check.equal(1, ended, "Exit ends the program once");
            }
        });
    }

    /**
     * Save Page writes the received text as UTF-8 with a byte order mark under the suggested name, Open File opens a chosen file, and
     * the editing commands reach the page.
     * <p>
     * 페이지 저장은 수신 원문을 제안한 이름으로, 바이트 순서 표시가 붙은 UTF-8 로 쓴다. 파일 열기는 고른 파일을 열고, 편집 명령은 페이지에 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSaveOpenAndEditCommands() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("saved page.html", "Page A", "<p>한글 text</p>");
        final File target = pages.file("out.html");
        pages.page("out.html", "placeholder", "");
        onEdt(new Runnable() {
            /** Loads the page. / 페이지를 읽는다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Saves the page. / 페이지를 저장한다. */
            public void run() {
                prompts.file = null;
                click(Commands.SAVE_PAGE);
                Check.equal("saved page.html", prompts.suggested, "the name comes from the address");
                prompts.file = target;
                click(Commands.SAVE_PAGE);
                Check.contains(window.getStatusBar().getStatusText(), "Saved", "the status bar tells that the page was saved");
            }
        });
        byte[] bytes = read(target);
        Check.isTrue(bytes.length > 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF,
                "the saved HTML starts with a byte order mark");
        final String saved = new String(bytes, 3, bytes.length - 3, "UTF-8");
        Check.contains(saved, "<p>한글 text</p>", "the saved file holds the received text");
        final String other = pages.page("other.html", "Other", "<p id=t>some text to select</p><script>"
                + "document.addEventListener('selectionchange', function () { document.title = 'sel:' + String(getSelection()).length; });"
                + "document.addEventListener('paste', function () { document.title = 'pasted'; });</" + "script>");
        onEdt(new Runnable() {
            /** Opens another file through the menu. / 메뉴로 다른 파일을 연다. */
            public void run() {
                Check.equal(saved, window.getPanel().getContent(), "what was saved is what the component received");
                prompts.file = pages.file("other.html");
                click(Commands.OPEN_FILE);
            }
        });
        awaitTab(0, "Other");
        onEdt(new Runnable() {
            /** Selects everything in the page. / 페이지에서 모두 선택한다. */
            public void run() {
                Check.equal(other, window.getPanel().getPage(), "Open File opens the chosen file");
                click(Commands.SELECT_ALL);
            }
        });
        awaitTab(0, "sel:19");
        onEdt(new Runnable() {
            /** Pastes into the page. / 페이지에 붙여넣는다. */
            public void run() {
                click(Commands.PASTE);
            }
        });
        awaitTab(0, "pasted");
    }

    /**
     * The keys of the window work wherever the focus is: accelerators of menu items and keys without a menu item run their commands
     * from the page and from the address bar. A key that the page handles itself (zoom, select all) is consumed by the page and does
     * not run the command of the window a second time, and Escape in the address bar reverts the address instead of stopping.
     * <p>
     * 창의 키는 포커스가 어디에 있든 동작한다: 메뉴 항목의 가속 키와 메뉴 항목 없는 키가 페이지에서도 주소 표시줄에서도 그 명령을 실행한다. 페이지가
     * 스스로 처리하는 키(확대, 모두 선택)는 페이지가 소비하여 창의 명령을 한 번 더 실행하지 않고, 주소 표시줄의 Esc 는 멈춤 대신 주소를 되돌린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testKeysAndTheirConflicts() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String home = pages.page("home.html", "Home", "<p>home</p>");
        final int ctrl = InputEvent.CTRL_MASK;
        final int shift = InputEvent.SHIFT_MASK;
        onEdt(new Runnable() {
            /** Shows the window and loads a page. / 창을 보이고 페이지를 읽는다. */
            public void run() {
                showOffScreen();
                app.getSettings().set(Settings.HOME_PAGE, home);
                window.navigate(a);
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Presses the tab keys in the page. / 페이지에서 탭 키를 누른다. */
            public void run() {
                SwingWebPanel page = window.getPanel();
                Check.isFalse(press(page, KeyEvent.VK_T, ctrl), "Ctrl+T is not a key of the page");
                Check.equal(2, window.getTabs().size(), "Ctrl+T opens a tab");
                press(window.getPanel(), KeyEvent.VK_T, ctrl);
                Check.equal(3, window.getTabs().size(), "Ctrl+T opens another tab from the new tab");
                press(window.getPanel(), KeyEvent.VK_TAB, ctrl);
                Check.same(window.getTabs().get(0), window.getActiveTab(), "Ctrl+Tab goes around to the first tab");
                press(window.getPanel(), KeyEvent.VK_TAB, ctrl | shift);
                Check.same(window.getTabs().get(2), window.getActiveTab(), "Ctrl+Shift+Tab goes back to the last tab");
                press(window.getPanel(), KeyEvent.VK_PAGE_UP, ctrl);
                Check.same(window.getTabs().get(1), window.getActiveTab(), "Ctrl+PageUp selects the tab before");
                press(window.getPanel(), KeyEvent.VK_PAGE_DOWN, ctrl);
                Check.same(window.getTabs().get(2), window.getActiveTab(), "Ctrl+PageDown selects the tab behind");
                press(window.getPanel(), KeyEvent.VK_1, ctrl);
                Check.same(window.getTabs().get(0), window.getActiveTab(), "Ctrl+1 selects the first tab");
                press(window.getPanel(), KeyEvent.VK_9, ctrl);
                Check.same(window.getTabs().get(2), window.getActiveTab(), "Ctrl+9 selects the last tab");
                press(window.getPanel(), KeyEvent.VK_2, ctrl);
                Check.same(window.getTabs().get(1), window.getActiveTab(), "Ctrl+2 selects the second tab");
                press(window.getPanel(), KeyEvent.VK_W, ctrl);
                Check.equal(2, window.getTabs().size(), "Ctrl+W closes the active tab");
                press(window.getPanel(), KeyEvent.VK_F4, ctrl);
                Check.equal(1, window.getTabs().size(), "Ctrl+F4 closes the active tab");
                press(window.getPanel(), KeyEvent.VK_N, ctrl);
                Check.equal(2, app.getWindows().size(), "Ctrl+N opens a window");
                app.getWindows().get(1).closeWindow();
            }
        });
        onEdt(new Runnable() {
            /** Presses keys that the page handles itself. / 페이지가 스스로 처리하는 키를 누른다. */
            public void run() {
                SwingWebPanel page = window.getPanel();
                Check.isTrue(press(page, KeyEvent.VK_EQUALS, ctrl), "the page consumes its zoom key");
                Check.near(1.1, page.getZoom(), 0.001, "the zoom key of the page zooms one step, not two");
                Check.isTrue(press(page, KeyEvent.VK_0, ctrl), "the page consumes Ctrl+0");
                Check.near(1.0, page.getZoom(), 0.001, "Ctrl+0 in the page resets the zoom");
                Check.isTrue(press(page, KeyEvent.VK_A, ctrl), "the page consumes Ctrl+A");
                Check.isFalse(press(window.getNavigationBar().getAddressField(), KeyEvent.VK_EQUALS, ctrl), "the address bar does not use Ctrl+=");
                Check.near(1.1, page.getZoom(), 0.001, "Ctrl+= from the address bar zooms the page through the menu");
                press(window.getNavigationBar().getAddressField(), KeyEvent.VK_SUBTRACT, ctrl);
                Check.near(1.0, page.getZoom(), 0.001, "Ctrl+numpad minus zooms out");
                window.getNavigationBar().type("typed text");
                press(window.getNavigationBar().getAddressField(), KeyEvent.VK_ESCAPE, 0);
                Check.equal(a, window.getNavigationBar().getAddressText(), "Escape in the address bar brings the address back");
                press(page, KeyEvent.VK_HOME, InputEvent.ALT_MASK);
            }
        });
        awaitTab(0, "Home");
        onEdt(new Runnable() {
            /** Goes back with the keyboard. / 키보드로 뒤로 간다. */
            public void run() {
                press(window.getPanel(), KeyEvent.VK_LEFT, InputEvent.ALT_MASK);
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Goes forward with the keyboard. / 키보드로 앞으로 간다. */
            public void run() {
                press(window.getPanel(), KeyEvent.VK_RIGHT, InputEvent.ALT_MASK);
            }
        });
        awaitTab(0, "Home");
        final String changed = pages.page("home.html", "Home again", "<p>changed</p>");
        onEdt(new Runnable() {
            /** Reloads with the keyboard. / 키보드로 다시 읽는다. */
            public void run() {
                Check.equal(home, changed, "the same address");
                press(window.getPanel(), KeyEvent.VK_F5, 0);
            }
        });
        awaitTab(0, "Home again");
        pages.page("home.html", "Home third", "<p>changed again</p>");
        onEdt(new Runnable() {
            /** Reloads with another key. / 다른 키로 다시 읽는다. */
            public void run() {
                press(window.getPanel(), KeyEvent.VK_R, ctrl | shift);
            }
        });
        awaitTab(0, "Home third");
    }
}
