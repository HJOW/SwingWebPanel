package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tabbed browsing with real pages (plan item P31-04): opening, selecting, closing and reopening tabs, what the window shows for the
 * active tab and for a tab in the background, the cookies that the tabs share and the session storage that they do not, the visibility
 * that a page in the background sees, and the question before a window with several tabs is closed. The cases need a frame and are
 * skipped in a headless JVM.
 * <p>
 * 실제 페이지로 확인하는 탭 브라우징(계획 P31-04): 탭을 열고 고르고 닫고 다시 여는 일, 활성 탭과 배경 탭에 대해 창이 보이는 것, 탭이 공유하는
 * 쿠키와 공유하지 않는 sessionStorage, 배경 페이지가 보는 가시성, 탭이 여럿인 창을 닫기 전의 질문. 사례는 프레임이 필요하므로 headless JVM 에서는
 * 건너뛴다.
 */
public class BrowserTabsTest {

    /** The pages of the case. / 사례의 페이지. */
    private TestPages pages;

    /** The application of the case. / 사례의 응용 프로그램. */
    private BrowserApp app;

    /** The window of the case. / 사례의 창. */
    private BrowserWindow window;

    /** The number of times that the program ended. / 프로그램이 끝난 횟수. */
    private int ended;

    /** The answer of the fake confirmation dialog. / 가짜 확인 대화 상자의 답. */
    private boolean answer;

    /** The questions that were asked. / 물어본 질문의 수. */
    private int asked;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrowserTabsTest() {
    }

    /**
     * Creates the pages and, with a display, a window that is not shown.
     * <p>
     * 페이지를 만들고, 화면이 있으면 보이지 않는 창을 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        System.setProperty(Texts.LANGUAGE_PROPERTY, "en");
        pages = new TestPages();
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the application and its window. / 응용 프로그램과 창을 만든다. */
            public void run() {
                app = new BrowserApp(new Runnable() {
                    /** Counts the end of the program. / 프로그램 종료를 센다. */
                    public void run() {
                        ended++;
                    }
                });
                app.setPrompts(new BrowserApp.Prompts() {
                    /** Answers without a dialog. / 대화 상자 없이 답한다. */
                    public boolean confirm(Component parent, String message) {
                        asked++;
                        return answer;
                    }
                });
                window = app.openWindow(null);
            }
        });
    }

    /**
     * Closes the window and removes the pages.
     * <p>
     * 창을 닫고 페이지를 지운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void tearDown() throws Exception {
        if (window != null) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Closes the window. / 창을 닫는다. */
                public void run() {
                    window.closeWindow();
                }
            });
        }
        pages.close();
        System.clearProperty(Texts.LANGUAGE_PROPERTY);
    }

    /**
     * Tells whether the case must be skipped because the JVM has no display.
     * <p>
     * JVM 에 화면이 없어 사례를 건너뛰어야 하는지 알려 준다.
     *
     * @return true if skipped / 건너뛰면 true
     */
    private boolean skipped() {
        if (window == null) {
            Check.skip("a browser window needs a display");
            return true;
        }
        return false;
    }

    /**
     * Runs work on the event dispatch thread and reports a failed check of the work as it is.
     * <p>
     * 이벤트 디스패치 스레드에서 일을 실행하고, 그 일의 실패한 확인을 그대로 보고한다.
     *
     * @param work the work / 일
     * @throws Exception on failure / 실패 시
     */
    private static void onEdt(Runnable work) throws Exception {
        try {
            SwingUtilities.invokeAndWait(work);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // 이벤트 디스패치 스레드에서 실패한 확인은 그 실패 그대로 보고한다.
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Waits until a tab shows a page with the given title and is not loading any more.
     * <p>
     * 탭이 주어진 제목의 페이지를 보이고 더 이상 읽는 중이 아닐 때까지 기다린다.
     *
     * @param index the position of the tab / 탭의 위치
     * @param title the title of the page / 페이지의 제목
     * @throws Exception on failure / 실패 시
     */
    private void awaitTab(final int index, final String title) throws Exception {
        boolean ok = TestPages.await(new TestPages.Condition() {
            /** Looks at the title and the progress of the tab. / 탭의 제목과 진행률을 본다. */
            public boolean holds() {
                return index < window.getTabs().size() && title.equals(window.getTabs().get(index).getPanel().getTitle())
                        && !window.getTabs().get(index).isLoading();
            }
        });
        final String[] shown = new String[1];
        onEdt(new Runnable() {
            /** Reads the title for the failure message. / 실패 메시지에 쓸 제목을 읽는다. */
            public void run() {
                shown[0] = index < window.getTabs().size() ? window.getTabs().get(index).getPanel().getTitle() : "(no such tab)";
            }
        });
        Check.isTrue(ok, "expected the page '" + title + "' in tab " + index + " but it shows '" + shown[0] + "'");
    }

    /**
     * Tabs are opened, selected, closed and opened again; the title bar, the address bar and the buttons follow the active tab, while a
     * change of a background tab only reaches its header in the tab strip. Text that was being edited is dropped when another tab is
     * selected. Closing the last tab closes the window.
     * <p>
     * 탭을 열고 고르고 닫고 다시 연다. 제목 표시줄, 주소 표시줄, 버튼은 활성 탭을 따라가고, 배경 탭의 변경은 탭 줄의 그 탭 머리에만 반영된다.
     * 편집 중이던 글은 다른 탭을 고르면 버린다. 마지막 탭을 닫으면 창을 닫는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpenSelectCloseAndReopen() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String b = pages.page("b.html", "A page with a very long title for the tab strip", "<p>second</p>");
        final NavigationBar bar = window.getNavigationBar();
        onEdt(new Runnable() {
            /** Loads the first tab and opens a second one in the background. / 첫 탭을 읽고 둘째 탭을 배경에 연다. */
            public void run() {
                Check.equal(1, window.getTabs().size(), "a window starts with one tab");
                window.navigate(a);
                window.newTab(b, false);
            }
        });
        awaitTab(0, "Page A");
        awaitTab(1, "A page with a very long title for the tab strip");
        onEdt(new Runnable() {
            /** Checks the window for the active and the background tab, then selects the second tab. / 활성 탭과 배경 탭에 대한 창을 확인한 뒤 둘째 탭을 고른다. */
            public void run() {
                Check.equal(2, window.getTabs().size(), "two tabs");
                Check.same(window.getTabs().get(0), window.getActiveTab(), "a tab opened in the background does not become active");
                Check.equal("Page A - " + BrowserApp.NAME, window.getTitle(), "the title bar shows the active tab");
                Check.equal(a, bar.getAddressText(), "the address bar shows the active tab");
                Check.equal("Page A", window.getTabHeader(0).getShownTitle(), "the header of the first tab");
                Check.equal("A page with a very lo...", window.getTabHeader(1).getShownTitle(), "a long title is cut in the tab strip");
                Check.isFalse(window.getTabHeader(1).isShownLoading(), "a loaded tab is not shown as loading");
                bar.type("typed for the first tab");
                window.selectTab(1);
                Check.same(window.getTabs().get(1), window.getActiveTab(), "the selected tab is active");
                Check.equal("A page with a very long title for the tab strip - " + BrowserApp.NAME, window.getTitle(), "the title bar follows the selection");
                Check.equal(b, bar.getAddressText(), "the address bar follows the selection and drops the edited text");
                Check.isTrue(bar.getButton(Icons.BACK).isEnabled(), "the second tab has a way back to its empty page");
                window.selectNextTab(1);
                Check.same(window.getTabs().get(0), window.getActiveTab(), "next from the last tab goes around to the first");
                window.selectNextTab(-1);
                Check.same(window.getTabs().get(1), window.getActiveTab(), "previous from the first tab goes around to the last");
                bar.getButton(Icons.PLUS).doClick();
                Check.equal(3, window.getTabs().size(), "the new tab button opens a tab");
                Check.same(window.getTabs().get(2), window.getActiveTab(), "the new tab is active");
                Check.equal(BrowserApp.NAME, window.getTitle(), "a new tab shows the empty page");
                Check.equal("", bar.getAddressText(), "the address bar is empty in a new tab");
                Check.equal("Blank page", window.getTabHeader(2).getShownTitle(), "the header of an empty tab");
                window.getTabHeader(1).getCloseButton().doClick();
                Check.equal(2, window.getTabs().size(), "the close button of a tab closes it");
                Check.isTrue(window.getTabHeader(1).getShownTitle().equals("Blank page"), "the tabs behind move up");
            }
        });
        onEdt(new Runnable() {
            /** Opens the closed tab again. / 닫은 탭을 다시 연다. */
            public void run() {
                Check.notNull(window.reopenClosedTab(), "a closed tab can be opened again");
                Check.equal(3, window.getTabs().size(), "the reopened tab is a new tab");
                Check.same(window.getTabs().get(2), window.getActiveTab(), "the reopened tab is active");
            }
        });
        awaitTab(2, "A page with a very long title for the tab strip");
        onEdt(new Runnable() {
            /** Closes the tabs one by one. / 탭을 하나씩 닫는다. */
            public void run() {
                Check.isNull(window.reopenClosedTab(), "nothing more to reopen (an empty tab is not remembered)");
                window.closeTab(window.getTabs().get(2));
                window.closeTab(window.getTabs().get(1));
                Check.equal(1, window.getTabs().size(), "one tab is left");
                Check.isFalse(window.isClosed(), "the window stays open");
                Check.equal("Page A - " + BrowserApp.NAME, window.getTitle(), "the remaining tab is shown");
                window.closeTab(window.getTabs().get(0));
                Check.isTrue(window.isClosed(), "closing the last tab closes the window");
                Check.equal(1, ended, "closing the last window ends the program");
            }
        });
    }

    /**
     * The tabs share their cookies (a login in one tab holds in another) but each tab has a session storage of its own. A new tab can
     * show the home page, as the settings say.
     * <p>
     * 탭은 쿠키를 공유하지만(한 탭의 로그인이 다른 탭에서도 유효하다) sessionStorage 는 탭마다 따로이다. 새 탭은 설정에 따라 홈 페이지를 보일 수
     * 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSharedCookiesSeparateSessionStorageAndNewTabPage() throws Exception {
        if (skipped()) {
            return;
        }
        final String server = pages.startServer(0);
        onEdt(new Runnable() {
            /** Sets a cookie in the first tab. / 첫 탭에서 쿠키를 설정한다. */
            public void run() {
                window.navigate(server + "/set?login");
            }
        });
        awaitTab(0, "set");
        onEdt(new Runnable() {
            /** Opens the echo page in a second tab. / 둘째 탭에 되울림 페이지를 연다. */
            public void run() {
                window.newTab(server + "/echo", true);
            }
        });
        awaitTab(1, "got:login=1");
        onEdt(new Runnable() {
            /** Uses the session storage of both tabs. / 두 탭의 sessionStorage 를 쓴다. */
            public void run() {
                window.getTabs().get(0).navigate(server + "/session");
            }
        });
        awaitTab(0, "session:null");
        onEdt(new Runnable() {
            /** Loads the same page in the first tab again and in the second tab. / 같은 페이지를 첫 탭에서 다시, 그리고 둘째 탭에서 읽는다. */
            public void run() {
                window.getTabs().get(0).getPanel().refresh();
                window.getTabs().get(1).navigate(server + "/session");
            }
        });
        awaitTab(0, "session:set");
        awaitTab(1, "session:null");
        onEdt(new Runnable() {
            /** Makes new tabs show the home page and opens one. / 새 탭이 홈 페이지를 보이게 하고 하나 연다. */
            public void run() {
                app.getSettings().set(Settings.HOME_PAGE, server + "/home");
                app.getSettings().set(Settings.NEW_TAB_PAGE, Settings.NEW_TAB_HOME);
                window.newTab();
            }
        });
        awaitTab(2, "/home");
    }

    /**
     * A page in a background tab sees the document as hidden and hears <code>visibilitychange</code> when its tab is selected again.
     * The window is shown off the screen for this case, because visibility follows what is really showing.
     * <p>
     * 배경 탭의 페이지는 문서를 숨겨진 것으로 보고, 탭이 다시 선택되면 <code>visibilitychange</code> 를 듣는다. 가시성은 실제로 화면에 보이는지를
     * 따르므로 이 사례에서는 창을 화면 밖에 보이게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackgroundTabIsHidden() throws Exception {
        if (skipped()) {
            return;
        }
        final String server = pages.startServer(0);
        onEdt(new Runnable() {
            /** Shows the window off the screen and opens the page. / 창을 화면 밖에 보이게 하고 페이지를 연다. */
            public void run() {
                window.setLocation(-3000, -3000);
                window.setVisible(true);
                window.navigate(server + "/vis");
            }
        });
        awaitTab(0, "vis:visible");
        onEdt(new Runnable() {
            /** Opens a second tab in front. / 둘째 탭을 앞에 연다. */
            public void run() {
                window.newTab(server + "/other", true);
            }
        });
        awaitTab(1, "/other");
        awaitTab(0, "vis:hidden");
        onEdt(new Runnable() {
            /** Checks the header of the background tab and selects it again. / 배경 탭의 머리를 확인하고 다시 고른다. */
            public void run() {
                Check.equal("vis:hidden", window.getTabHeader(0).getShownTitle(), "the header of the background tab follows its title");
                Check.isTrue(window.getTitle().startsWith("/other"), "the title bar keeps showing the active tab: " + window.getTitle());
                window.selectTab(0);
            }
        });
        awaitTab(0, "vis:visible");
    }

    /**
     * Closing a window with several tabs asks first when the settings say so: a refusal keeps the window, an agreement closes it; with
     * the setting off, and for a window with one tab, nothing is asked.
     * <p>
     * 탭이 여럿인 창을 닫을 때 설정이 그렇게 정했으면 먼저 묻는다: 거절하면 창이 남고 동의하면 닫힌다. 설정을 끄면, 그리고 탭이 하나인 창에는 묻지
     * 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClosingAWindowWithSeveralTabsAsks() throws Exception {
        if (skipped()) {
            return;
        }
        onEdt(new Runnable() {
            /** Tries the cases one after the other. / 경우들을 차례로 해 본다. */
            public void run() {
                BrowserWindow single = app.openWindow(null);
                Check.isTrue(single.requestClose(), "a window with one tab closes without a question");
                Check.equal(0, asked, "nothing was asked");
                window.newTab(null, false);
                answer = false;
                Check.isFalse(window.requestClose(), "a refusal keeps the window");
                Check.equal(1, asked, "one question");
                Check.isFalse(window.isClosed(), "the window is still open");
                Check.equal(2, window.getTabs().size(), "the tabs are still there");
                app.getSettings().set(Settings.CONFIRM_CLOSE_TABS, "false");
                BrowserWindow quiet = app.openWindow(null);
                quiet.newTab(null, false);
                Check.isTrue(quiet.requestClose(), "with the setting off the window closes at once");
                Check.equal(1, asked, "no further question");
                app.getSettings().set(Settings.CONFIRM_CLOSE_TABS, "true");
                answer = true;
                Check.isTrue(window.requestClose(), "an agreement closes the window");
                Check.equal(2, asked, "a second question");
                Check.isTrue(window.isClosed(), "the window is closed");
                Check.isTrue(window.getTabs().get(0).getPanel().isDisposed() && window.getTabs().get(1).getPanel().isDisposed(),
                        "closing a window disposes the pages of all its tabs");
                Check.equal(1, ended, "the last window ended the program once");
            }
        });
    }
}
