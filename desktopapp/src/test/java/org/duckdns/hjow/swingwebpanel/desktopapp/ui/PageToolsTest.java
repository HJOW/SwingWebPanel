package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ConsoleLog;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The helper features of a browser window around a page (plan item P31-09): the find bar, the source window, the console window, the
 * history window and the clearing of the private data.
 * <p>
 * 브라우저 창의 페이지 보조 기능(계획 P31-09): 찾기 막대, 원본 창, 콘솔 창, 방문 기록 창, 개인 정보 삭제.
 */
public class PageToolsTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PageToolsTest() {
    }

    /**
     * Waits until the find bar shows a count text.
     * <p>
     * 찾기 막대가 개수 글을 보일 때까지 기다린다.
     *
     * @param expected the expected text / 기대하는 글
     * @param message  what it means / 그것이 뜻하는 것
     * @throws Exception on failure / 실패 시
     */
    private void awaitCount(final String expected, String message) throws Exception {
        boolean ok = TestPages.await(new TestPages.Condition() {
            /** The bar shows the text. / 막대가 그 글을 보인다. */
            public boolean holds() {
                return expected.equals(window.getFindBar().getCountText());
            }
        });
        Check.isTrue(ok, message + ": expected '" + expected + "' but the bar shows '" + window.getFindBar().getCountText() + "'");
    }

    /**
     * The find bar opens with its command, searches while the user types, steps with the buttons and the commands, follows the active
     * tab, and ends the search when it is closed.
     * <p>
     * 찾기 막대는 명령으로 열리고, 사용자가 입력하는 동안 찾으며, 단추와 명령으로 일치를 옮겨 다니고, 활성 탭을 따라가며, 닫으면 찾기를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFindBar() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("find-a.html", "Find A", "<p>one fish, two fish</p><p>red Fish, blue fish</p>");
        final String b = pages.page("find-b.html", "Find B", "<p>a single fish</p>");
        onEdt(new Runnable() {
            /** Opens the two pages. / 두 페이지를 연다. */
            public void run() {
                Check.isFalse(window.getFindBar().isOpen(), "the find bar is closed at first");
                window.navigate(a);
                window.newTab(b, false);
            }
        });
        awaitTab(0, "Find A");
        awaitTab(1, "Find B");
        onEdt(new Runnable() {
            /** Opens the bar and types. / 막대를 열고 입력한다. */
            public void run() {
                Check.isTrue(window.perform(Commands.FIND), "the find command is known");
                Check.isTrue(window.getFindBar().isOpen(), "the command opens the find bar");
                window.getFindBar().getField().setText("fish");
            }
        });
        awaitCount("1 / 4", "typing searches the page of the active tab");
        onEdt(new Runnable() {
            /** Steps with the commands. / 명령으로 옮겨 다닌다. */
            public void run() {
                window.perform(Commands.FIND_NEXT);
            }
        });
        awaitCount("2 / 4", "Find Next goes to the next match");
        onEdt(new Runnable() {
            /** Steps back twice. / 두 번 뒤로 간다. */
            public void run() {
                window.perform(Commands.FIND_PREVIOUS);
                window.perform(Commands.FIND_PREVIOUS);
            }
        });
        awaitCount("4 / 4", "Find Previous goes around at the start");
        onEdt(new Runnable() {
            /** Turns match case on. / 대소문자 구분을 켠다. */
            public void run() {
                window.getFindBar().getMatchCaseBox().doClick(0);
            }
        });
        awaitCount("3 / 3", "match case leaves out the capitalized word and the current match stays where it was");
        onEdt(new Runnable() {
            /** Types a text that is not there. / 없는 글을 입력한다. */
            public void run() {
                window.getFindBar().getMatchCaseBox().doClick(0);
                window.getFindBar().getField().setText("whale");
            }
        });
        awaitCount("No matches", "a text that is not on the page is reported");
        onEdt(new Runnable() {
            /** Switches to the other tab. / 다른 탭으로 바꾼다. */
            public void run() {
                window.getFindBar().getField().setText("fish");
                window.selectTab(1);
            }
        });
        awaitCount("1 / 1", "the search follows the active tab");
        await(new TestPages.Condition() {
            /** The first tab has no search any more. / 첫 탭에는 더 이상 찾기가 없다. */
            public boolean holds() {
                return !window.getTabs().get(0).getPanel().getFindResult().isActive();
            }
        }, "the search of the tab that was left ended");
        onEdt(new Runnable() {
            /** Closes the bar. / 막대를 닫는다. */
            public void run() {
                window.getFindBar().close();
                Check.isFalse(window.getFindBar().isOpen(), "the bar is closed");
            }
        });
        await(new TestPages.Condition() {
            /** The search of the active tab ended. / 활성 탭의 찾기가 끝났다. */
            public boolean holds() {
                return !window.getPanel().getFindResult().isActive();
            }
        }, "closing the bar ends the search");
        onEdt(new Runnable() {
            /** Opens the bar again with Find Next. / 다음 찾기로 막대를 다시 연다. */
            public void run() {
                window.perform(Commands.FIND_NEXT);
                Check.isTrue(window.getFindBar().isOpen(), "Find Next opens a closed bar");
                Check.equal("fish", window.getFindBar().getText(), "the text of the last search is still there");
            }
        });
        awaitCount("1 / 1", "and the text is searched for again");
    }

    /**
     * The source window shows the received text as plain text, and it does not follow the page afterwards.
     * <p>
     * 원본 창은 수신한 글을 평문으로 보이고, 그 뒤에는 페이지를 따라가지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourceWindow() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("source.html", "Source", "<p id=p>before <b>bold</b> &amp; after</p>"
                + "<script>document.getElementById('p').textContent = 'changed by script';</" + "script>");
        onEdt(new Runnable() {
            /** Opens the page. / 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitTab(0, "Source");
        onEdt(new Runnable() {
            /** Opens the source window. / 원본 창을 연다. */
            public void run() {
                Check.isTrue(window.perform(Commands.VIEW_SOURCE), "the source command is known");
                SourceWindow w = window.getLastSourceWindow();
                Check.notNull(w, "a source window was created");
                Check.equal(window.getPanel().getContent(), w.getSourceText(), "the window shows exactly the received text");
                Check.contains(w.getSourceText(), "<b>bold</b> &amp; after", "markup and character references are shown as text");
                Check.isFalse(w.getSourceText().indexOf("changed by script'") < 0, "the script is part of the source");
                Check.isFalse(w.getArea().isEditable(), "the text cannot be edited");
                Check.isTrue(w.getArea().getFont().getFamily().toLowerCase().indexOf("mono") >= 0 || "Monospaced".equals(w.getArea().getFont().getName()),
                        "the font is the fixed-width one: " + w.getArea().getFont());
                Check.contains(w.getInfoText(), "text/html", "the type is shown");
                Check.contains(w.getTitle(), "source.html", "the title names the page");
                w.dispose();
            }
        });
    }

    /**
     * The console window shows the messages of the active tab with their levels, also those that arrived before it was opened, filters
     * by level, follows the active tab and empties the log.
     * <p>
     * 콘솔 창은 활성 탭의 메시지를 수준과 함께 보이고, 열리기 전에 도착한 메시지도 보이며, 수준으로 거르고, 활성 탭을 따라가고, 로그를 비운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConsoleWindow() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("console-a.html", "Console A", "<p>a</p><script>console.log('plain message');"
                + "console.warn('careful <b>now</b>'); console.error('went wrong');</" + "script>");
        final String b = pages.page("console-b.html", "Console B", "<p>b</p><script>console.info('other tab');</" + "script>");
        onEdt(new Runnable() {
            /** Opens the two pages. / 두 페이지를 연다. */
            public void run() {
                window.navigate(a);
                window.newTab(b, false);
            }
        });
        awaitTab(0, "Console A");
        awaitTab(1, "Console B");
        await(new TestPages.Condition() {
            /** The messages of the first tab arrived. / 첫 탭의 메시지가 도착했다. */
            public boolean holds() {
                return window.getTabs().get(0).getConsole().getEntries().size() >= 3 && window.getTabs().get(1).getConsole().getEntries().size() >= 1;
            }
        }, "the tabs keep their console messages");
        onEdt(new Runnable() {
            /** Opens the console and uses it. / 콘솔을 열어 쓴다. */
            public void run() {
                Check.isTrue(window.perform(Commands.CONSOLE), "the console command is known");
                ConsoleWindow c = window.showConsole();
                String shown = c.getShownText();
                Check.contains(shown, "[log] plain message", "a message that arrived before the window opened is shown with its level");
                Check.contains(shown, "[warn] careful <b>now</b>", "markup in a message is shown as text");
                Check.contains(shown, "[error] went wrong", "errors are shown");
                Check.contains(shown, "console-a.html", "the place of a message is shown");
                Check.isFalse(shown.indexOf("other tab") >= 0, "messages of another tab are not mixed in");
                Check.equal("Console A", c.getOwnerText(), "the window names the page");
                c.getLevelBox().setSelectedIndex(ConsoleLog.WARN);
                Check.isFalse(c.getShownText().indexOf("plain message") >= 0, "the level list hides lower levels");
                Check.contains(c.getShownText(), "[warn]", "warnings stay");
                c.getLevelBox().setSelectedIndex(ConsoleLog.ERROR);
                Check.isFalse(c.getShownText().indexOf("[warn]") >= 0, "only errors are left");
                Check.contains(c.getShownText(), "[error] went wrong", "the error stays");
                c.getLevelBox().setSelectedIndex(0);
                window.selectTab(1);
                Check.contains(c.getShownText(), "[info] other tab", "the window follows the active tab");
                Check.isFalse(c.getShownText().indexOf("went wrong") >= 0, "and leaves the other tab behind");
                window.selectTab(0);
                window.getTabs().get(0).getConsole().clear();
                Check.equal("", c.getShownText().trim(), "clearing the log empties the window");
                Check.equal(1, window.getTabs().get(1).getConsole().getEntries().size(), "the log of the other tab is untouched");
            }
        });
    }

    /**
     * The history window lists the session history of the active tab, marks the entry that is shown, and goes to a chosen entry.
     * <p>
     * 방문 기록 창은 활성 탭의 세션 이력을 나열하고, 지금 보이는 항목을 표시하며, 고른 항목으로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHistoryWindow() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("h1.html", "First", "<p>1</p>");
        final String b = pages.page("h2.html", "Second", "<p>2</p>");
        final String c = pages.page("h3.html", "Third", "<p>3</p>");
        onEdt(new Runnable() {
            /** Opens the first page. / 첫 페이지를 연다. */
            public void run() {
                window.navigate(a);
            }
        });
        awaitTab(0, "First");
        onEdt(new Runnable() {
            /** Opens the second page. / 둘째 페이지를 연다. */
            public void run() {
                window.navigate(b);
            }
        });
        awaitTab(0, "Second");
        onEdt(new Runnable() {
            /** Opens the third page. / 셋째 페이지를 연다. */
            public void run() {
                window.navigate(c);
            }
        });
        awaitTab(0, "Third");
        final HistoryDialog[] dialog = new HistoryDialog[1];
        onEdt(new Runnable() {
            /** Opens the history and goes to the second entry. / 방문 기록을 열고 둘째 항목으로 간다. */
            public void run() {
                Check.isTrue(window.perform(Commands.HISTORY), "the history command is known");
                dialog[0] = window.showHistory();
                String[] lines = dialog[0].getLines();
                Check.equal(4, lines.length, "the empty first page and three pages");
                Check.isTrue(lines[3].startsWith("> ") && lines[3].endsWith("h3.html"), "the entry that is shown is marked: " + lines[3]);
                Check.isTrue(lines[1].startsWith("  ") && lines[1].endsWith("h1.html"), "the others are not: " + lines[1]);
                Check.equal(3, dialog[0].getList().getSelectedIndex(), "the entry that is shown is selected");
                dialog[0].getList().setSelectedIndex(1);
                dialog[0].go();
            }
        });
        awaitTab(0, "First");
        onEdt(new Runnable() {
            /** Goes forward through the list. / 목록으로 앞으로 간다. */
            public void run() {
                String[] lines = dialog[0].getLines();
                Check.isTrue(lines[1].startsWith("> "), "the list follows the tab: " + lines[1]);
                Check.equal(4, lines.length, "going back keeps the entries ahead");
                dialog[0].getList().setSelectedIndex(3);
                dialog[0].go();
            }
        });
        awaitTab(0, "Third");
        onEdt(new Runnable() {
            /** Checks the entries that cannot be gone to. / 갈 수 없는 항목을 확인한다. */
            public void run() {
                Check.isFalse(window.getActiveTab().goToHistory(3), "the entry that is shown starts no navigation");
                Check.isFalse(window.getActiveTab().goToHistory(9), "an entry outside the history starts none either");
            }
        });
    }

    /**
     * Clearing the private data asks first, then empties every tab of every window, forgets the closed tabs and the console messages,
     * and removes the cookies and the session storage; bookmarks and settings stay.
     * <p>
     * 개인 정보 삭제는 먼저 묻고, 그런 다음 모든 창의 모든 탭을 비우고, 닫은 탭과 콘솔 메시지를 잊으며, 쿠키와 sessionStorage 를 지운다. 북마크와
     * 설정은 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClearPrivateData() throws Exception {
        if (skipped()) {
            return;
        }
        final String base = pages.startServer(0);
        final BrowserWindow[] second = new BrowserWindow[1];
        onEdt(new Runnable() {
            /** Sets a cookie and session storage in two windows. / 두 창에서 쿠키와 sessionStorage 를 정한다. */
            public void run() {
                window.navigate(base + "/set?kept");
                window.newTab(base + "/session", false);
                window.newTab(base + "/closing", false);
                second[0] = window.newWindow(base + "/session");
            }
        });
        awaitTab(0, "set");
        awaitTab(1, "session:null");
        awaitTab(2, "/closing");
        awaitTab(second[0], 0, "session:null");
        onEdt(new Runnable() {
            /** Closes a tab, declines once, then clears. / 탭을 닫고, 한 번 거절한 뒤 지운다. */
            public void run() {
                window.closeTab(window.getTabs().get(2));
                Check.isTrue(window.hasClosedTabs(), "a closed tab is remembered");
                Check.contains(app.getCookieJar().getCookies(), "kept=1", "the cookie is in the shared jar");
                app.getBookmarks().add(app.getBookmarks().getRoot(), org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkNode.bookmark("kept", base + "/k"));
                prompts.answer = false;
                Check.isTrue(window.perform(Commands.CLEAR_PRIVATE_DATA), "the command is known");
                Check.contains(prompts.messages.get(prompts.messages.size() - 1), "local storage", "the question tells what is shared");
                Check.contains(app.getCookieJar().getCookies(), "kept=1", "declining keeps everything");
                Check.equal(base + "/set?kept", window.getTabs().get(0).getUrl(), "the pages stay as well");
                prompts.answer = true;
                window.perform(Commands.CLEAR_PRIVATE_DATA);
                Check.equal("", app.getCookieJar().getCookies(), "the cookies are removed");
                Check.isFalse(window.hasClosedTabs(), "the closed tabs are forgotten");
                Check.equal(1, app.getBookmarks().getRoot().getChildCount(), "bookmarks are kept");
            }
        });
        await(new TestPages.Condition() {
            /** Every tab of both windows shows the empty page. / 두 창의 모든 탭이 빈 페이지를 보인다. */
            public boolean holds() {
                List<BrowserTab> tabs = window.getTabs();
                // 첫 창의 탭마다 빈 페이지인지 본다.
                for (int i = 0; i < tabs.size(); i++) {
                    if (!BrowserApp.BLANK.equals(tabs.get(i).getUrl()) || tabs.get(i).getPanel().getHistories().size() != 1) {
                        return false;
                    }
                }
                return BrowserApp.BLANK.equals(second[0].getTabs().get(0).getUrl());
            }
        }, "every tab of every window is emptied with its history");
        onEdt(new Runnable() {
            /** Looks at the storage and opens the session page again. / 저장소를 보고 세션 페이지를 다시 연다. */
            public void run() {
                Check.equal(0, window.getTabs().get(1).getPanel().getSessionStorage().size(), "the session storage of the tab is empty");
                Check.equal(0, window.getTabs().get(0).getConsole().getEntries().size(), "the console messages are gone");
                Check.equal(2, window.getTabs().size(), "the tabs themselves stay open");
                Check.contains(window.getActiveTab().getStatusText(), "cleared", "the status bar tells what happened");
                window.getTabs().get(1).navigate(base + "/session");
            }
        });
        awaitTab(1, "session:null");
        onEdt(new Runnable() {
            /** The window can be used as before. / 창을 전처럼 쓸 수 있다. */
            public void run() {
                window.getTabs().get(0).navigate(base + "/echo");
            }
        });
        awaitTab(0, "got:");
    }
}
