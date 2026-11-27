package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Dialog;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * New tabs, new windows and popups of the browser (plan items P31-04-b and P31-08): where a window that a script opened is shown
 * (popup window, browser window, tab) and that the script keeps control of it, the popup blocker with its notice and its button,
 * links that the user opens in a new tab or window with Ctrl, Shift, the middle button and the context menu, links that ask for a
 * new window, and that closing a tab keeps the local storage of the other tabs. The pages come from a loopback HTTP server.
 * <p>
 * 브라우저의 새 탭, 새 창, 팝업(계획 P31-04-b, P31-08): 스크립트가 연 창을 보이는 곳(팝업 창, 브라우저 창, 탭)과 스크립트가 그 창을 계속
 * 제어하는지, 팝업 차단과 그 알림·단추, 사용자가 Ctrl·Shift·가운데 단추·문맥 메뉴로 새 탭이나 창에서 여는 링크, 새 창을 요청하는 링크,
 * 탭을 닫아도 다른 탭의 localStorage 가 남는지. 페이지는 루프백 HTTP 서버에서 온다.
 */
public class PopupsTest extends WindowCase {

    /** Script text that lets the test run code in a page through a message of the host. / 시험이 호스트의 메시지로 페이지에서 코드를 실행하게 하는 스크립트 글. */
    private static final String RUNNER = "<script>window.addEventListener('message', function (e) {"
            + " if (typeof e.data === 'string' && e.data.indexOf('run:') === 0) { eval(e.data.substring(4)); } });</" + "script>";

    /** The address prefix of the server. / 서버의 주소 앞부분. */
    private String base;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PopupsTest() {
    }

    /**
     * Starts the server and gives it the pages of the cases.
     * <p>
     * 서버를 시작하고 사례의 페이지를 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    private void startPages() throws Exception {
        base = pages.startServer(0);
        pages.serve("/opener", "<!doctype html><title>opener</title><p>opener</p>" + RUNNER);
        pages.serve("/child", "<!doctype html><title>child</title><p>child</p>" + RUNNER);
        pages.serve("/target", "<!doctype html><title>target</title><p>target</p>" + RUNNER);
        pages.serve("/links", "<!doctype html><title>links</title><style>body { margin: 0 } a { display: block; height: 40px; overflow: hidden }</style>"
                + "<a href='/target'>plain link</a><a href='/target' target='_blank'>new window link</a>" + RUNNER);
    }

    /**
     * Runs a script in the page of a component.
     * <p>
     * 컴포넌트의 페이지에서 스크립트를 실행한다.
     *
     * @param panel the component / 컴포넌트
     * @param code  the script / 스크립트
     */
    private static void run(SwingWebPanel panel, String code) {
        panel.postMessage("run:" + code, "*");
    }

    /**
     * Waits until a component shows a page with a title.
     * <p>
     * 컴포넌트가 정해진 제목의 페이지를 보일 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param title the title / 제목
     * @param what  what the title means / 제목이 뜻하는 것
     * @throws Exception on failure / 실패 시
     */
    private static void awaitTitle(final SwingWebPanel panel, final String title, String what) throws Exception {
        boolean ok = TestPages.await(new TestPages.Condition() {
            /** Compares the title. / 제목을 비교한다. */
            public boolean holds() {
                return title.equals(panel.getTitle());
            }
        });
        Check.isTrue(ok, what + ": expected the title '" + title + "' but it is '" + panel.getTitle() + "'");
    }

    /**
     * Sets a setting on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 설정을 정한다.
     *
     * @param key   the key / 키
     * @param value the value / 값
     * @throws Exception on failure / 실패 시
     */
    private void set(final String key, final String value) throws Exception {
        onEdt(new Runnable() {
            /** Changes the setting. / 설정을 바꾼다. */
            public void run() {
                app.getSettings().set(key, value);
            }
        });
    }

    /**
     * A window that a script opened is shown where the settings say: in a popup window (the default), in a tab, or in a browser
     * window. The script that opened it reads and changes its document and closes it wherever it is shown, and the page of the
     * window closes only its own popup, tab or window.
     * <p>
     * 스크립트가 연 창은 설정이 정한 곳에 보인다: 팝업 창(기본), 탭, 브라우저 창. 창을 연 스크립트는 어디에 보이든 그 문서를 읽고 바꾸며 창을
     * 닫고, 그 창의 페이지는 자기 팝업, 탭, 창만 닫는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptWindowsFollowThePlaceSetting() throws Exception {
        if (skipped()) {
            return;
        }
        startPages();
        set(Settings.BLOCK_POPUPS, "false");
        onEdt(new Runnable() {
            /** Opens the page that opens windows. / 창을 여는 페이지를 연다. */
            public void run() {
                window.navigate(base + "/opener");
            }
        });
        awaitTab(0, "opener");
        final SwingWebPanel opener = window.getPanel();
        run(opener, "window.w1 = window.open('/child', 'one', 'width=300,height=200')");
        await(new TestPages.Condition() {
            /** Waits for the popup window. / 팝업 창을 기다린다. */
            public boolean holds() {
                return window.getPopups().size() == 1;
            }
        }, "a script window is shown in a popup window by default");
        final PopupWindow popup = window.getPopups().get(0);
        awaitTitle(popup.getTab().getPanel(), "child", "the popup loads its page");
        onEdt(new Runnable() {
            /** Looks at the popup window. / 팝업 창을 본다. */
            public void run() {
                Check.same(window, popup.getOwner(), "the popup belongs to the browser window");
                Check.equal(Dialog.ModalityType.MODELESS, popup.getModalityType(), "it is modeless");
                Check.equal("child - " + BrowserApp.NAME, popup.getTitle(), "its title bar shows the title of the page");
                Check.equal(base + "/child", popup.getAddressText(), "it shows the address of the page");
                Check.equal(300, popup.getTab().getComponent().getWidth(), "the page has the width that the script asked for");
                Check.equal(200, popup.getTab().getComponent().getHeight(), "and the height");
                Check.equal(1, window.getTabs().size(), "no tab was added");
                Check.isFalse(popup.getTab().getPanel().isPopupBlockingEnabled(), "the popup follows the settings of the application");
            }
        });
        run(opener, "document.title = 'sees:' + w1.document.title + ':' + (w1.opener === window)");
        awaitTitle(opener, "sees:child:true", "the opener reads the document of its popup, and the popup knows its opener");
        run(popup.getTab().getPanel(), "window.close()");
        await(new TestPages.Condition() {
            /** Waits until the popup is gone. / 팝업이 사라질 때까지 기다린다. */
            public boolean holds() {
                return window.getPopups().isEmpty() && popup.isClosed();
            }
        }, "window.close() of the page closes its popup window");
        Check.isTrue(popup.getTab().getPanel().isDisposed(), "and disposes its page");
        run(opener, "document.title = 'closed:' + w1.closed");
        awaitTitle(opener, "closed:true", "the opener sees the popup as closed");

        set(Settings.POPUP_PLACE, Settings.PLACE_TAB);
        run(opener, "window.w2 = window.open('/child')");
        await(new TestPages.Condition() {
            /** Waits for the new tab. / 새 탭을 기다린다. */
            public boolean holds() {
                return window.getTabs().size() == 2;
            }
        }, "with the setting for tabs a script window is shown in a new tab");
        awaitTab(1, "child");
        Check.same(window.getTabs().get(1), window.getActiveTab(), "the tab of a script window becomes the active one");
        Check.isTrue(window.getPopups().isEmpty(), "no popup window was opened");
        run(opener, "w2.document.title = 'changed by opener'");
        awaitTab(1, "changed by opener");
        run(window.getTabs().get(1).getPanel(), "window.close()");
        await(new TestPages.Condition() {
            /** Waits until the tab is gone. / 탭이 사라질 때까지 기다린다. */
            public boolean holds() {
                return window.getTabs().size() == 1;
            }
        }, "window.close() of the page closes its tab");
        Check.isFalse(window.isClosed(), "and not the window with the other tab");
        Check.equal(0, ended, "the program does not end");

        set(Settings.POPUP_PLACE, Settings.PLACE_WINDOW);
        run(opener, "window.w3 = window.open('/child')");
        await(new TestPages.Condition() {
            /** Waits for the new window. / 새 창을 기다린다. */
            public boolean holds() {
                return app.getWindows().size() == 2;
            }
        }, "with the setting for windows a script window is shown in a browser window");
        final BrowserWindow second = app.getWindows().get(1);
        awaitTab(second, 0, "child");
        Check.equal(1, second.getTabs().size(), "the new window has the one tab");
        run(opener, "w3.close()");
        await(new TestPages.Condition() {
            /** Waits until the window is gone. / 창이 사라질 때까지 기다린다. */
            public boolean holds() {
                return app.getWindows().size() == 1 && second.isClosed();
            }
        }, "the opener closes the window that it opened");
        Check.isFalse(window.isClosed(), "the window of the opener stays");
    }

    /**
     * With the popup blocker on, a window that a script asks for without an action of the user is not opened: the status bar tells
     * so and offers a button, which opens the address where popups are shown. The setting reaches every tab.
     * <p>
     * 팝업 차단이 켜져 있으면 스크립트가 사용자의 조작 없이 요청한 창은 열리지 않는다: 상태 표시줄이 그것을 알리고 단추를 제안하며, 단추는 팝업을
     * 보이는 곳에 그 주소를 연다. 설정은 모든 탭에 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedPopupNoticeAndButton() throws Exception {
        if (skipped()) {
            return;
        }
        startPages();
        onEdt(new Runnable() {
            /** Opens the page that opens windows. / 창을 여는 페이지를 연다. */
            public void run() {
                Check.isTrue(window.getPanel().isPopupBlockingEnabled(), "the popup blocker is on by default");
                Check.isFalse(window.getStatusBar().getActionButton().isVisible(), "the status bar offers nothing at first");
                window.navigate(base + "/opener");
            }
        });
        awaitTab(0, "opener");
        final SwingWebPanel opener = window.getPanel();
        run(opener, "var w = window.open('/child'); document.title = 'blocked:' + (w === null)");
        awaitTitle(opener, "blocked:true", "the script gets no window");
        await(new TestPages.Condition() {
            /** Waits for the notice. / 알림을 기다린다. */
            public boolean holds() {
                return window.getStatusBar().getActionButton().isVisible();
            }
        }, "the status bar offers to open the blocked popup");
        onEdt(new Runnable() {
            /** Looks at the notice and presses the button. / 알림을 보고 단추를 누른다. */
            public void run() {
                Check.equal("A popup was blocked: " + base + "/child", window.getStatusBar().getStatusText(), "the status bar names the blocked address");
                Check.equal("Open this popup", window.getStatusBar().getActionButton().getText(), "the text of the button");
                Check.isTrue(window.getPopups().isEmpty() && window.getTabs().size() == 1, "nothing was opened");
                window.getStatusBar().getActionButton().doClick();
                Check.equal(1, window.getPopups().size(), "the button opens the popup");
                Check.isFalse(window.getStatusBar().getActionButton().isVisible(), "and the offer is gone");
            }
        });
        final PopupWindow popup = window.getPopups().get(0);
        awaitTitle(popup.getTab().getPanel(), "child", "the popup loads the blocked address");
        run(popup.getTab().getPanel(), "var seen; try { seen = String(opener.document); } catch (e) { seen = e.name; } document.title = 'opener:' + seen");
        awaitTitle(popup.getTab().getPanel(), "opener:SecurityError", "a popup that the user opened later has no connection to the page");
        set(Settings.BLOCK_POPUPS, "false");
        onEdt(new Runnable() {
            /** Looks at the components. / 컴포넌트를 본다. */
            public void run() {
                Check.isFalse(window.getPanel().isPopupBlockingEnabled(), "the setting reaches the tab");
                Check.isFalse(popup.getTab().getPanel().isPopupBlockingEnabled(), "and the popup window");
                window.closeTab(window.getActiveTab());
            }
        });
        Check.isTrue(popup.isClosed() && window.isClosed(), "closing the browser window closes its popup windows");
        Check.isTrue(popup.getTab().getPanel().isDisposed(), "and disposes their pages");
    }

    /**
     * Sends a mouse event to the page of a tab, at a row of the link page.
     * <p>
     * 링크 페이지의 한 행 위치로, 탭의 페이지에 마우스 이벤트를 보낸다.
     *
     * @param panel     the component / 컴포넌트
     * @param id        the event id / 이벤트 ID
     * @param row       the row of 40 pixels, starting at 0 / 0 부터 시작하는 40픽셀 행
     * @param button    the mouse button / 마우스 단추
     * @param modifiers the modifier mask / 보조 키 마스크
     * @param menu      true if the event asks for the context menu / 이벤트가 문맥 메뉴를 요청하면 true
     * @throws Exception on failure / 실패 시
     */
    private static void mouse(final SwingWebPanel panel, final int id, final int row, final int button, final int modifiers, final boolean menu)
            throws Exception {
        onEdt(new Runnable() {
            /** Sends the event. / 이벤트를 보낸다. */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, id, System.currentTimeMillis(), modifiers, 20, 20 + 40 * row,
                        id == MouseEvent.MOUSE_MOVED ? 0 : 1, menu, button));
            }
        });
    }

    /**
     * Finds an item of a menu by its name.
     * <p>
     * 메뉴의 항목을 이름으로 찾는다.
     *
     * @param menu the menu / 메뉴
     * @param name the name / 이름
     * @return the item, or null / 항목. 없으면 null
     */
    private static JMenuItem item(JPopupMenu menu, String name) {
        // 메뉴의 컴포넌트마다 이름을 비교한다.
        for (int i = 0; i < menu.getComponentCount(); i++) {
            if (menu.getComponent(i) instanceof JMenuItem && name.equals(menu.getComponent(i).getName())) {
                return (JMenuItem) menu.getComponent(i);
            }
        }
        return null;
    }

    /**
     * Waits until the window has a number of tabs.
     * <p>
     * 창의 탭 수가 정해진 수가 될 때까지 기다린다.
     *
     * @param count the number of tabs / 탭 수
     * @param what  what happened / 일어난 일
     * @throws Exception on failure / 실패 시
     */
    private void awaitTabs(final int count, String what) throws Exception {
        await(new TestPages.Condition() {
            /** Counts the tabs. / 탭을 센다. */
            public boolean holds() {
                return window.getTabs().size() == count;
            }
        }, what + " (tabs: " + window.getTabs().size() + ")");
    }

    /**
     * The user opens links elsewhere with real mouse events (plan item P31-04-b): Ctrl+click and the middle button open a new tab,
     * in the background unless the settings select new tabs; Shift+click opens a new window; the context menu of a link offers both.
     * A link that asks for a new window gets a tab or a window as the settings say. The page of the link stays in every case.
     * <p>
     * 사용자가 실제 마우스 이벤트로 링크를 다른 곳에서 연다(계획 P31-04-b): Ctrl+클릭과 가운데 단추는 새 탭을 열고, 설정이 새 탭을 선택하게 하지
     * 않았으면 배경에 연다. Shift+클릭은 새 창을 연다. 링크의 문맥 메뉴가 둘 다 제안한다. 새 창을 요청하는 링크는 설정에 따라 탭이나 창을 얻는다.
     * 어느 경우에도 링크의 페이지는 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLinksOpenInNewTabsAndWindows() throws Exception {
        if (skipped()) {
            return;
        }
        startPages();
        onEdt(new Runnable() {
            /** Shows the window and opens the page with the links. / 창을 보이고 링크가 있는 페이지를 연다. */
            public void run() {
                showOffScreen();
                window.navigate(base + "/links");
            }
        });
        awaitTab(0, "links");
        final SwingWebPanel panel = window.getPanel();
        final BrowserTab first = window.getTabs().get(0);
        // 페이지가 그려져 링크의 위치를 찾을 수 있을 때까지 잠깐 기다린다.
        Thread.sleep(300);
        mouse(panel, MouseEvent.MOUSE_CLICKED, 0, MouseEvent.BUTTON1, InputEvent.CTRL_MASK, false);
        awaitTabs(2, "Ctrl+click opens a new tab");
        awaitTab(1, "target");
        Check.same(first, window.getActiveTab(), "the new tab opens in the background");
        Check.equal("links", panel.getTitle(), "the page of the link stays");
        mouse(panel, MouseEvent.MOUSE_CLICKED, 0, MouseEvent.BUTTON2, 0, false);
        awaitTabs(3, "a click of the middle button opens a new tab");
        awaitTab(2, "target");
        Check.same(first, window.getActiveTab(), "in the background as well");
        set(Settings.SELECT_NEW_TABS, "true");
        mouse(panel, MouseEvent.MOUSE_CLICKED, 0, MouseEvent.BUTTON1, InputEvent.CTRL_MASK, false);
        awaitTabs(4, "Ctrl+click opens another tab");
        awaitTab(3, "target");
        Check.same(window.getTabs().get(3), window.getActiveTab(), "the settings make a new tab the active one");
        onEdt(new Runnable() {
            /** Goes back to the tab with the links. / 링크가 있는 탭으로 돌아간다. */
            public void run() {
                window.selectTab(0);
            }
        });
        mouse(panel, MouseEvent.MOUSE_CLICKED, 0, MouseEvent.BUTTON1, InputEvent.SHIFT_MASK, false);
        await(new TestPages.Condition() {
            /** Waits for the new window. / 새 창을 기다린다. */
            public boolean holds() {
                return app.getWindows().size() == 2;
            }
        }, "Shift+click opens a new window");
        awaitTab(app.getWindows().get(1), 0, "target");
        Check.equal(4, window.getTabs().size(), "and no tab");

        mouse(panel, MouseEvent.MOUSE_CLICKED, 1, MouseEvent.BUTTON1, 0, false);
        awaitTabs(5, "a link that asks for a new window opens a tab");
        awaitTab(4, "target");
        Check.same(window.getTabs().get(4), window.getActiveTab(), "which becomes the active tab");
        Check.equal("links", panel.getTitle(), "the page of the link stays");
        run(window.getTabs().get(4).getPanel(), "window.close()");
        awaitTabs(4, "the page of such a tab may close it");
        set(Settings.LINK_PLACE, Settings.PLACE_WINDOW);
        onEdt(new Runnable() {
            /** Goes back to the tab with the links. / 링크가 있는 탭으로 돌아간다. */
            public void run() {
                window.selectTab(0);
            }
        });
        mouse(panel, MouseEvent.MOUSE_CLICKED, 1, MouseEvent.BUTTON1, 0, false);
        await(new TestPages.Condition() {
            /** Waits for the new window. / 새 창을 기다린다. */
            public boolean holds() {
                return app.getWindows().size() == 3;
            }
        }, "with the setting for windows such a link opens a window");
        awaitTab(app.getWindows().get(2), 0, "target");

        mouse(panel, MouseEvent.MOUSE_MOVED, 0, MouseEvent.NOBUTTON, 0, false);
        await(new TestPages.Condition() {
            /** Waits until the tab knows the link under the mouse. / 탭이 마우스 아래의 링크를 알 때까지 기다린다. */
            public boolean holds() {
                return (base + "/target").equals(first.getHoveredLink());
            }
        }, "the tab knows the link under the mouse");
        mouse(panel, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON3, InputEvent.BUTTON3_MASK, true);
        final JPopupMenu[] menus = new JPopupMenu[1];
        onEdt(new Runnable() {
            /** Uses the context menu of the link. / 링크의 문맥 메뉴를 쓴다. */
            public void run() {
                JPopupMenu menu = window.getLastContextMenu();
                Check.notNull(menu, "the context menu was built");
                menus[0] = menu;
                Check.notNull(item(menu, "open-link-window"), "the menu of a link offers a new window");
                Check.notNull(item(menu, "copy-link"), "and to copy the address");
                Check.isFalse(item(menu, "forward").isEnabled(), "a page without a way forward cannot go forward");
                JMenuItem open = item(menu, "open-link-tab");
                Check.notNull(open, "the menu of a link offers a new tab");
                menu.setVisible(false);
                open.doClick();
            }
        });
        awaitTabs(5, "the context menu opens the link in a new tab");
        awaitTab(4, "target");
        onEdt(new Runnable() {
            /** Opens the link in a new window from the same menu. / 같은 메뉴에서 링크를 새 창에서 연다. */
            public void run() {
                item(menus[0], "open-link-window").doClick();
                Check.equal(4, app.getWindows().size(), "the context menu opens the link in a new window");
                window.selectTab(0);
            }
        });
        mouse(panel, MouseEvent.MOUSE_MOVED, 5, MouseEvent.NOBUTTON, 0, false);
        await(new TestPages.Condition() {
            /** Waits until the mouse left the link. / 마우스가 링크를 떠날 때까지 기다린다. */
            public boolean holds() {
                return first.getHoveredLink() == null;
            }
        }, "the tab forgets the link when the mouse leaves it");
        mouse(panel, MouseEvent.MOUSE_PRESSED, 5, MouseEvent.BUTTON3, InputEvent.BUTTON3_MASK, true);
        onEdt(new Runnable() {
            /** Looks at the context menu of the page. / 페이지의 문맥 메뉴를 본다. */
            public void run() {
                JPopupMenu menu = window.getLastContextMenu();
                Check.isTrue(menu != menus[0], "a new menu was built");
                Check.isTrue(item(menu, "open-link-tab") == null, "away from a link the menu has no link items");
                Check.notNull(item(menu, "reload"), "but the commands of the page");
                menu.setVisible(false);
            }
        });
    }

    /**
     * Closing a tab keeps the local storage that all tabs share.
     * <p>
     * 탭을 닫아도 모든 탭이 함께 쓰는 localStorage 는 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClosingATabKeepsLocalStorage() throws Exception {
        if (skipped()) {
            return;
        }
        startPages();
        pages.serve("/store", "<!doctype html><title>store</title><p>store</p>" + RUNNER
                + "<script>localStorage.setItem('popup-test', 'kept'); document.title = 'stored';</" + "script>");
        onEdt(new Runnable() {
            /** Opens the page that stores and a second tab. / 저장하는 페이지와 둘째 탭을 연다. */
            public void run() {
                window.navigate(base + "/store");
                window.newTab(base + "/child", true);
            }
        });
        awaitTab(0, "stored");
        awaitTab(1, "child");
        onEdt(new Runnable() {
            /** Closes the second tab. / 둘째 탭을 닫는다. */
            public void run() {
                window.closeTab(window.getTabs().get(1));
            }
        });
        run(window.getTabs().get(0).getPanel(), "document.title = 'read:' + localStorage.getItem('popup-test')");
        awaitTab(0, "read:kept");
    }
}
