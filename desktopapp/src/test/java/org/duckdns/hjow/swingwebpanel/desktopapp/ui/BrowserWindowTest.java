package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.util.List;
import javax.swing.JPopupMenu;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The main window and its navigation toolbar with real pages (plan item P31-03): the title bar, the address bar and its editing rules,
 * the back, forward, home, stop and reload buttons, and the status bar. The pages are local files and answers of a small loopback server.
 * The buttons are pressed with <code>doClick()</code>; real mouse and keyboard input is checked separately with <code>tools/uicheck</code>.
 * The cases need a frame and are skipped in a headless JVM.
 * <p>
 * 실제 페이지로 확인하는 주 창과 탐색 도구 모음(계획 P31-03): 제목 표시줄, 주소 표시줄과 그 편집 규칙, 뒤로·앞으로·홈·멈춤·새로고침 버튼, 상태
 * 표시줄. 페이지는 로컬 파일과 작은 루프백 서버의 응답이다. 버튼은 <code>doClick()</code> 으로 누른다. 실제 마우스·키보드 입력은
 * <code>tools/uicheck</code> 로 따로 확인한다. 사례는 프레임이 필요하므로 headless JVM 에서는 건너뛴다.
 */
public class BrowserWindowTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrowserWindowTest() {
    }

    /**
     * Waits until the window shows a page with the given title and is not loading any more.
     * <p>
     * 창이 주어진 제목의 페이지를 보이고 더 이상 읽는 중이 아닐 때까지 기다린다.
     *
     * @param title the title of the page / 페이지의 제목
     * @throws Exception on failure / 실패 시
     */
    private void awaitPage(final String title) throws Exception {
        boolean ok = TestPages.await(new TestPages.Condition() {
            /** Looks at the title bar and the progress. / 제목 표시줄과 진행률을 본다. */
            public boolean holds() {
                return (title + " - " + BrowserApp.NAME).equals(window.getTitle()) && !window.getActiveTab().isLoading();
            }
        });
        Check.isTrue(ok, "expected the page '" + title + "' but the window shows '" + window.getTitle() + "'");
    }

    /**
     * The title bar shows the title of the page and the name of the application, the address bar its address, and the status bar its
     * status, the hidden progress bar of a loaded page and the zoom factor.
     * <p>
     * 제목 표시줄은 페이지의 제목과 응용 프로그램의 이름을, 주소 표시줄은 그 주소를, 상태 표시줄은 그 상태와 다 읽은 페이지의 숨은 진행 막대와 확대
     * 비율을 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTitleAddressAndStatusBar() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String untitled = pages.page("untitled.html", "", "<p>no title</p>");
        Check.equal(BrowserApp.NAME, window.getTitle(), "an empty page shows the application name only");
        Check.equal("", window.getNavigationBar().getAddressText(), "the address bar is empty for the empty page");
        onEdt(new Runnable() {
            /** Opens the first page. / 첫 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitPage("Page A");
        onEdt(new Runnable() {
            /** Checks the bars and changes the zoom. / 표시줄을 확인하고 확대 비율을 바꾼다. */
            public void run() {
                Check.equal(window.getPanel().getPage(), window.getNavigationBar().getAddressText(), "the address bar shows the address of the page");
                Check.isFalse(window.getStatusBar().isProgressVisible(), "no progress bar for a loaded page");
                Check.equal(window.getPanel().getStatusText(), window.getStatusBar().getStatusText(), "the status bar shows the status text of the page");
                Check.equal("100%", window.getStatusBar().getZoomText(), "the zoom factor");
                window.getPanel().setZoom(1.5);
            }
        });
        boolean zoomed = TestPages.await(new TestPages.Condition() {
            /** Looks at the zoom text. / 확대 비율의 글을 본다. */
            public boolean holds() {
                return "150%".equals(window.getStatusBar().getZoomText());
            }
        });
        Check.isTrue(zoomed, "the status bar follows the zoom factor");
        onEdt(new Runnable() {
            /** Opens the page without a title. / 제목 없는 페이지를 연다. */
            public void run() {
                window.navigate(untitled);
            }
        });
        boolean shown = TestPages.await(new TestPages.Condition() {
            /** Looks at the title bar. / 제목 표시줄을 본다. */
            public boolean holds() {
                return (window.getPanel().getPage() + " - " + BrowserApp.NAME).equals(window.getTitle()) && window.getPanel().getPage().endsWith("untitled.html");
            }
        });
        Check.isTrue(shown, "a page without a title shows its address in the title bar: " + window.getTitle());
    }

    /**
     * The back and forward buttons follow the history and move in it, their menus list the entries, the home button opens the home page
     * of the settings and the reload button loads the page again.
     * <p>
     * 뒤로·앞으로 버튼은 이력을 따라 활성화되고 이력에서 이동하며, 그 메뉴는 항목을 나열하고, 홈 버튼은 설정의 홈 페이지를 열고, 새로고침 버튼은
     * 페이지를 다시 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackForwardHomeAndReload() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String b = pages.page("b.html", "Page B", "<p>second</p>");
        final String home = pages.page("home.html", "Home", "<p>home</p>");
        final NavigationBar bar = window.getNavigationBar();
        Check.isFalse(bar.getButton(Icons.BACK).isEnabled(), "no way back at first");
        Check.isFalse(bar.getButton(Icons.FORWARD).isEnabled(), "no way forward at first");
        Check.isFalse(bar.getButton(Icons.STOP).isEnabled(), "nothing to stop at first");
        onEdt(new Runnable() {
            /** Opens the first page. / 첫 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitPage("Page A");
        onEdt(new Runnable() {
            /** Opens the second page. / 둘째 페이지를 연다. */
            public void run() {
                window.navigate(b);
            }
        });
        awaitPage("Page B");
        onEdt(new Runnable() {
            /** Checks the buttons and the menu, then goes back. / 버튼과 메뉴를 확인한 뒤 뒤로 간다. */
            public void run() {
                Check.isTrue(bar.getButton(Icons.BACK).isEnabled(), "back is possible on the second page");
                Check.isFalse(bar.getButton(Icons.FORWARD).isEnabled(), "forward is not possible on the newest page");
                List<String> behind = window.backEntries();
                Check.equal(2, behind.size(), "two entries behind: the first page and the empty page");
                Check.equal(a, behind.get(0), "the nearest entry first");
                JPopupMenu menu = bar.historyMenu(true);
                Check.equal(2, menu.getComponentCount(), "the menu of the back button lists the entries");
                Check.equal(0, bar.historyMenu(false).getComponentCount(), "the menu of the forward button is empty");
                bar.getButton(Icons.BACK).doClick();
            }
        });
        awaitPage("Page A");
        onEdt(new Runnable() {
            /** Checks the buttons, then goes forward. / 버튼을 확인한 뒤 앞으로 간다. */
            public void run() {
                Check.isTrue(bar.getButton(Icons.FORWARD).isEnabled(), "forward is possible after going back");
                Check.equal(a, bar.getAddressText(), "the address bar follows the history move");
                bar.getButton(Icons.FORWARD).doClick();
            }
        });
        awaitPage("Page B");
        onEdt(new Runnable() {
            /** Sets the home page and presses the home button. / 홈 페이지를 정하고 홈 버튼을 누른다. */
            public void run() {
                app.getSettings().set(Settings.HOME_PAGE, home);
                bar.getButton(Icons.HOME).doClick();
            }
        });
        awaitPage("Home");
        // 파일의 내용을 바꾼 뒤 새로고침하면 새 제목이 보여야 한다.
        pages.page("home.html", "Home again", "<p>changed</p>");
        onEdt(new Runnable() {
            /** Presses the reload button. / 새로고침 버튼을 누른다. */
            public void run() {
                bar.getButton(Icons.RELOAD).doClick();
            }
        });
        awaitPage("Home again");
        onEdt(new Runnable() {
            /** Moves two entries back through the menu of the back button. / 뒤로 버튼의 메뉴로 두 칸 뒤로 간다. */
            public void run() {
                Check.equal(home, bar.getAddressText(), "a reload keeps the address");
                ((javax.swing.JMenuItem) bar.historyMenu(true).getComponent(1)).doClick();
            }
        });
        awaitPage("Page A");
    }

    /**
     * The address bar keeps text that the user is editing when the page changes, Escape brings the address of the page back, Enter
     * opens the text (a file path here), and text that cannot be opened shows a message in the status bar.
     * <p>
     * 주소 표시줄은 페이지가 바뀌어도 사용자가 편집 중인 글을 유지하고, Esc 는 페이지의 주소로 되돌리며, Enter 는 글(여기서는 파일 경로)을 열고,
     * 열 수 없는 글은 상태 표시줄에 메시지를 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAddressBarEditing() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        pages.page("b.html", "Page B", "<p>second</p>");
        final NavigationBar bar = window.getNavigationBar();
        onEdt(new Runnable() {
            /** Types into the address bar, then the page changes. / 주소 표시줄에 입력한 뒤 페이지가 바뀐다. */
            public void run() {
                bar.type("half typed");
                window.navigate(a);
            }
        });
        awaitPage("Page A");
        onEdt(new Runnable() {
            /** Checks the kept text, reverts, then opens a file path. / 유지된 글을 확인하고 되돌린 뒤 파일 경로를 연다. */
            public void run() {
                Check.equal("half typed", bar.getAddressText(), "the page change does not overwrite text that is being edited");
                bar.revert();
                Check.equal(a, bar.getAddressText(), "Escape brings the address of the page back");
                bar.type(pages.file("b.html").getAbsolutePath());
                bar.submit();
            }
        });
        awaitPage("Page B");
        onEdt(new Runnable() {
            /** Checks the address and types text that cannot be opened. / 주소를 확인하고 열 수 없는 글을 입력한다. */
            public void run() {
                Check.equal(TestPages.url(pages.file("b.html")), bar.getAddressText(), "a typed file path becomes a file address");
                bar.type("two words");
                bar.submit();
                Check.contains(window.getStatusBar().getStatusText(), "no search address", "text that cannot be opened shows a message");
                Check.equal("two words", bar.getAddressText(), "the text stays in the address bar");
                Check.isTrue(window.getPanel().getPage().endsWith("b.html"), "the page stays as it is");
            }
        });
    }

    /**
     * While a page loads the stop button is enabled and the progress bar is visible; the stop button ends the loading and the old page
     * stays.
     * <p>
     * 페이지를 읽는 동안 멈춤 버튼이 활성화되고 진행 막대가 보인다. 멈춤 버튼은 읽기를 끝내고 이전 페이지가 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStopWhileLoading() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String slow = pages.startServer(5000) + "/slow";
        final NavigationBar bar = window.getNavigationBar();
        onEdt(new Runnable() {
            /** Opens the first page. / 첫 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitPage("Page A");
        onEdt(new Runnable() {
            /** Types the slow address and opens it. / 느린 주소를 입력하고 연다. */
            public void run() {
                bar.type(slow);
                bar.submit();
            }
        });
        boolean loading = TestPages.await(new TestPages.Condition() {
            /** Looks at the stop button and the progress bar. / 멈춤 버튼과 진행 막대를 본다. */
            public boolean holds() {
                return bar.getButton(Icons.STOP).isEnabled() && window.getStatusBar().isProgressVisible();
            }
        });
        Check.isTrue(loading, "the stop button is enabled and the progress bar is visible while the page loads");
        onEdt(new Runnable() {
            /** Checks the pending address and presses stop. / 읽는 중인 주소를 확인하고 멈춤을 누른다. */
            public void run() {
                Check.equal(slow, bar.getAddressText(), "the address bar shows the address that is being loaded");
                Check.equal("Page A - " + BrowserApp.NAME, window.getTitle(), "the old page is shown until the new one arrives");
                bar.getButton(Icons.STOP).doClick();
            }
        });
        boolean stopped = TestPages.await(new TestPages.Condition() {
            /** Looks at the stop button and the progress bar. / 멈춤 버튼과 진행 막대를 본다. */
            public boolean holds() {
                return !bar.getButton(Icons.STOP).isEnabled() && !window.getStatusBar().isProgressVisible();
            }
        });
        Check.isTrue(stopped, "stop ends the loading");
        Check.equal("Page A - " + BrowserApp.NAME, window.getTitle(), "the old page stays after stop");
    }
}
