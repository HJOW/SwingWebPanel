package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.List;
import javax.swing.AbstractButton;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.SettingsStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.TextFiles;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The settings of the browser (plan item P31-07): the dialog shows the values and changes nothing until OK, OK applies them to every
 * tab of every window and saves them, the program starts with the home page, the empty page or the tabs of the last run, a script
 * closes its tab only when the settings allow it, and a broken settings file is kept.
 * <p>
 * 브라우저의 설정(계획 P31-07): 대화 상자는 값을 보이고 확인을 누를 때까지 아무것도 바꾸지 않으며, 확인은 값을 모든 창의 모든 탭에 적용하고 저장한다.
 * 프로그램은 홈 페이지, 빈 페이지, 또는 지난번의 탭으로 시작하고, 스크립트는 설정이 허용할 때만 자기 탭을 닫으며, 손상된 설정 파일은 보존한다.
 */
public class SettingsTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SettingsTest() {
    }

    /**
     * Finds a button by its component name.
     * <p>
     * 컴포넌트 이름으로 단추를 찾는다.
     *
     * @param root the container to search / 찾을 컨테이너
     * @param name the component name / 컴포넌트 이름
     * @return the button, or null / 단추. 없으면 null
     */
    private static AbstractButton button(Container root, String name) {
        Component[] children = root.getComponents();
        // 자식마다 이름을 비교하고 컨테이너이면 안을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof AbstractButton && name.equals(children[i].getName())) {
                return (AbstractButton) children[i];
            }
            if (children[i] instanceof Container) {
                AbstractButton inner = button((Container) children[i], name);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Starts a second application on the same user data, as the next run of the program does.
     * <p>
     * 프로그램의 다음 실행처럼 같은 사용자 데이터로 둘째 응용 프로그램을 시작한다.
     *
     * @return the application / 응용 프로그램
     */
    private static BrowserApp restart() {
        return new BrowserApp(new Runnable() {
            /** Does nothing at the end. / 끝날 때 아무것도 하지 않는다. */
            public void run() {
            }
        });
    }

    /**
     * The dialog shows the settings; Cancel changes nothing; OK applies every value to all tabs of all windows (scripts, the
     * User-Agent, the default fonts, the default zoom, the bars) and writes the file that the next start reads.
     * <p>
     * 대화 상자는 설정을 보인다. 취소는 아무것도 바꾸지 않는다. 확인은 모든 값을 모든 창의 모든 탭에 적용하고(스크립트, User-Agent, 기본 글꼴, 기본
     * 확대 비율, 막대) 다음 시작이 읽는 파일을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDialogAppliesToAllTabsAndSaves() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        onEdt(new Runnable() {
            /** Loads a page, opens more tabs and a window. / 페이지를 읽고 탭과 창을 더 연다. */
            public void run() {
                window.navigate(a);
                window.newTab(null, false);
                window.newWindow(null);
            }
        });
        awaitTab(0, "Page A");
        onEdt(new Runnable() {
            /** Uses the dialog. / 대화 상자를 쓴다. */
            public void run() {
                Settings s = app.getSettings();
                SwingWebPanel first = window.getTabs().get(0).getPanel();
                SwingWebPanel second = window.getTabs().get(1).getPanel();
                SwingWebPanel other = app.getWindows().get(1).getPanel();
                String builtInAgent = first.getUserAgent();
                Check.equal(13, first.getDefaultMonospaceFontSize(), "the application starts tabs with its fixed-width size");
                window.getTabs().get(1).getPanel().setZoom(2.0);
                Check.isTrue(window.perform(Commands.SETTINGS), "the command is known");
                SettingsDialog d = window.showSettings();
                Check.isFalse(d.isVisible(), "the dialog of a hidden window is not shown");
                List<String> keys = d.getKeys();
                Check.isTrue(keys.contains(Settings.HOME_PAGE) && keys.contains(Settings.FONT_STANDARD) && keys.contains(Settings.POPUP_PLACE)
                        && keys.contains(Settings.PRINT_HEADERS), "the dialog has controls for the settings: " + keys);
                Check.equal("about:blank", d.getValue(Settings.HOME_PAGE), "the dialog shows the home page");
                Check.equal("16", d.getValue(Settings.FONT_SIZE), "and the font size");
                Check.equal("", d.getValue(Settings.FONT_STANDARD), "the built-in font is the empty value");
                Check.equal(Settings.PLACE_POPUP, d.getValue(Settings.POPUP_PLACE), "popups open in a popup window by default");
                Check.equal("true", d.getValue(Settings.BLOCK_POPUPS), "popups are blocked by default");
                d.setValue(Settings.FONT_SIZE, "20");
                d.setValue(Settings.JAVASCRIPT, "false");
                d.cancel();
                Check.isFalse(d.isDisplayable(), "Cancel closes the dialog");
                Check.equal("16", s.get(Settings.FONT_SIZE), "Cancel changes no setting");
                Check.equal(16, first.getDefaultFontSize(), "and no tab");

                d = window.showSettings();
                button(d, "home-current").doClick(0);
                Check.equal(a, d.getValue(Settings.HOME_PAGE), "Use Current Page takes the address of the active tab");
                button(d, "home-blank").doClick(0);
                Check.equal("about:blank", d.getValue(Settings.HOME_PAGE), "Use Blank Page takes the empty page");
                d.setValue(Settings.HOME_PAGE, a);
                prompts.file = home;
                button(d, "download-browse").doClick(0);
                Check.equal(home.getPath(), d.getValue(Settings.DOWNLOAD_DIRECTORY), "Browse takes the chosen folder");
                d.setValue(Settings.FONT_STANDARD, "Monospaced");
                d.setValue(Settings.FONT_SERIF, "A Font That Is Not Installed");
                d.setValue(Settings.FONT_SIZE, "20");
                d.setValue(Settings.FONT_MONOSPACE_SIZE, "15");
                Check.equal("Monospaced", d.getPreviewFont().getFamily(), "the preview shows the chosen font");
                Check.equal(20, d.getPreviewFont().getSize(), "in the chosen size");
                d.setValue(Settings.JAVASCRIPT, "false");
                d.setValue(Settings.SCRIPTS_CLOSE_WINDOWS, "true");
                d.setValue(Settings.USER_AGENT, "TestAgent/1.0");
                d.setValue(Settings.DEFAULT_ZOOM, "125");
                d.setValue(Settings.SHOW_STATUS_BAR, "false");
                d.setValue(Settings.POPUP_PLACE, Settings.PLACE_TAB);
                d.setValue(Settings.START_PAGE, Settings.START_RESTORE);
                d.setValue(Settings.PRINT_BACKGROUNDS, "true");
                Check.equal("16", s.get(Settings.FONT_SIZE), "nothing changes before OK");
                button(d, "ok").doClick(0);
                Check.isFalse(d.isDisplayable(), "OK closes the dialog");
                Check.equal(a, s.get(Settings.HOME_PAGE), "OK writes the settings");
                Check.equal("A Font That Is Not Installed", s.get(Settings.FONT_SERIF), "a font that is not installed is kept as a setting");
                SwingWebPanel[] all = { first, second, other };
                // 모든 창의 모든 탭에 값이 적용되었는지 본다.
                for (int i = 0; i < all.length; i++) {
                    Check.equal(20, all[i].getDefaultFontSize(), "the default font size reaches tab " + i);
                    Check.equal(15, all[i].getDefaultMonospaceFontSize(), "the fixed-width size reaches tab " + i);
                    Check.equal("Monospaced", all[i].getDefaultFontFamily(SwingWebPanel.FONT_STANDARD), "the standard font reaches tab " + i);
                    Check.isFalse(all[i].isJavascriptEnabled(), "scripts are turned off in tab " + i);
                    Check.isTrue(all[i].isRootWindowClosingEnabled(), "closing by scripts is allowed in tab " + i);
                    Check.equal("TestAgent/1.0", all[i].getUserAgent(), "the User-Agent reaches tab " + i);
                }
                Check.near(1.25, first.getZoom(), 0.001, "a tab at the old default zoom follows the new one");
                Check.near(2.0, second.getZoom(), 0.001, "a tab that the user zoomed keeps its zoom");
                Check.isFalse(window.isStatusBarShown() || app.getWindows().get(1).isStatusBarShown(), "the status bar is hidden in every window");
                Check.equal(20, window.newTab(null, false).getPanel().getDefaultFontSize(), "a new tab starts with the settings");

                d = window.showSettings();
                Check.equal("Monospaced", d.getValue(Settings.FONT_STANDARD), "the dialog shows the new values");
                Check.equal("A Font That Is Not Installed", d.getValue(Settings.FONT_SERIF), "also a font that is not installed");
                d.setValue(Settings.USER_AGENT, "");
                d.setValue(Settings.JAVASCRIPT, "true");
                d.accept();
                Check.equal(builtInAgent, first.getUserAgent(), "an empty User-Agent brings the one of the component back");
                Check.isTrue(first.isJavascriptEnabled(), "scripts are turned on again");
                d = window.showSettings();
                button(d, "defaults").doClick(0);
                Check.equal("16", d.getValue(Settings.FONT_SIZE), "Restore Defaults shows the defaults");
                Check.equal(20, first.getDefaultFontSize(), "but applies nothing before OK");
                d.cancel();
            }
        });
        String text = TextFiles.read(new File(home, SettingsStore.FILE_NAME));
        Check.contains(text, "font.standard=Monospaced\n", "the settings file holds the font");
        Check.contains(text, "popups.place=tab\n", "and the place of popups");
        onEdt(new Runnable() {
            /** Looks at the settings of the next start. / 다음 시작의 설정을 본다. */
            public void run() {
                BrowserApp next = restart();
                Check.equal(app.getSettings().toMap(), next.getSettings().toMap(), "the next start reads the saved settings");
                Check.isNull(next.takeStorageProblem(), "without a problem");
            }
        });
    }

    /**
     * The program starts with the home page, the empty page, the address of the command line or the windows and tabs of the last run,
     * and a window comes back with the size and place that it had.
     * <p>
     * 프로그램은 홈 페이지, 빈 페이지, 명령행의 주소, 또는 지난번의 창과 탭으로 시작하고, 창은 그때의 크기와 위치로 돌아온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStartPagesAndSessionRestore() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String b = pages.page("b.html", "Page B", "<p>second</p>");
        final String h = pages.page("h.html", "Home", "<p>home</p>");
        final BrowserWindow[] started = new BrowserWindow[3];
        onEdt(new Runnable() {
            /** Starts with the home page and with a blank page. / 홈 페이지와 빈 페이지로 시작해 본다. */
            public void run() {
                app.getSettings().set(Settings.HOME_PAGE, h);
                List<BrowserWindow> opened = restart().start(null);
                Check.equal(1, opened.size(), "one window at start");
                started[0] = opened.get(0);
                started[1] = restart().start("  " + b + " ").get(0);
                app.getSettings().set(Settings.START_PAGE, Settings.START_BLANK);
                BrowserWindow blank = restart().start(null).get(0);
                Check.equal(BrowserApp.BLANK, blank.getPanel().getPage(), "the empty page at start");
                blank.closeWindow();
                app.getSettings().set(Settings.START_PAGE, Settings.START_RESTORE);
                started[2] = restart().start(null).get(0);
            }
        });
        awaitTab(started[0], 0, "Home");
        awaitTab(started[1], 0, "Page B");
        awaitTab(started[2], 0, "Home");
        onEdt(new Runnable() {
            /** Opens the tabs of the session. / 세션의 탭을 연다. */
            public void run() {
                started[0].closeWindow();
                started[1].closeWindow();
                started[2].closeWindow();
                window.setBounds(120, 90, 640, 480);
                window.navigate(a);
                window.newTab(null, false);
                window.newTab(b, true);
                BrowserWindow second = window.newWindow(b);
                second.setBounds(200, 150, 500, 400);
            }
        });
        awaitTab(0, "Page A");
        awaitTab(2, "Page B");
        awaitTab(app.getWindows().get(1), 0, "Page B");
        final BrowserWindow[] restored = new BrowserWindow[2];
        onEdt(new Runnable() {
            /** Ends the program and starts it again. / 프로그램을 끝내고 다시 시작한다. */
            public void run() {
                app.getSettings().set(Settings.CONFIRM_CLOSE_TABS, "false");
                Check.isTrue(app.exit(), "the program ends");
                Check.equal(1, ended, "once");
                BrowserApp next = restart();
                List<BrowserWindow> opened = next.start(null);
                Check.equal(2, opened.size(), "both windows come back");
                restored[0] = opened.get(0);
                restored[1] = opened.get(1);
                Check.equal(2, restored[0].getTabs().size(), "the tabs with a page come back; the empty tab does not");
                Check.same(restored[0].getTabs().get(1), restored[0].getActiveTab(), "the active tab is active again");
                Check.equal(640, restored[0].getWidth(), "the window has the width of the window that was closed last");
                Check.equal(480, restored[0].getHeight(), "and its height");
                Check.equal(120, restored[0].getX(), "and its place");
                Check.equal(150, restored[1].getX(), "a further window is put a little beside it");
            }
        });
        awaitTab(restored[0], 0, "Page A");
        awaitTab(restored[0], 1, "Page B");
        awaitTab(restored[1], 0, "Page B");
        onEdt(new Runnable() {
            /** Closes the restored windows one by one. / 복원한 창을 하나씩 닫는다. */
            public void run() {
                restored[1].requestClose();
                restored[0].closeTab(restored[0].getTabs().get(1));
                restored[0].requestClose();
                List<BrowserWindow> again = restart().start(null);
                Check.equal(1, again.size(), "the window that was closed last is the session");
                Check.equal(1, again.get(0).getTabs().size(), "with the tab that it had");
                again.get(0).closeWindow();
            }
        });
    }

    /**
     * A script that closes its window closes its tab when the settings allow it and nothing otherwise; the other tabs stay.
     * <p>
     * 자기 창을 닫는 스크립트는 설정이 허용하면 자기 탭을 닫고, 아니면 아무것도 닫지 않는다. 다른 탭은 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptsCloseTheirTabOnlyWhenAllowed() throws Exception {
        if (skipped()) {
            return;
        }
        final String closing = pages.page("closing.html", "Closing", "<p>closes itself</p><script>"
                + "setTimeout(function () { window.close(); document.title = 'closed:' + window.closed; }, 50);</" + "script>");
        final String a = pages.page("a.html", "Page A", "<p>stays</p>");
        onEdt(new Runnable() {
            /** Opens the page while closing is not allowed. / 닫기가 허용되지 않은 동안 페이지를 연다. */
            public void run() {
                window.navigate(a);
                window.newTab(closing, false);
            }
        });
        awaitTab(0, "Page A");
        awaitTab(1, "closed:false");
        onEdt(new Runnable() {
            /** Allows closing and opens the page again. / 닫기를 허용하고 페이지를 다시 연다. */
            public void run() {
                Check.equal(2, window.getTabs().size(), "the tab stays while the setting is off");
                app.getSettings().set(Settings.SCRIPTS_CLOSE_WINDOWS, "true");
                window.getTabs().get(1).getPanel().refresh();
            }
        });
        await(new TestPages.Condition() {
            /** The tab of the script is gone. / 스크립트의 탭이 사라졌다. */
            public boolean holds() {
                return window.getTabs().size() == 1;
            }
        }, "the script closes its tab when the setting is on");
        onEdt(new Runnable() {
            /** Looks at what is left. / 남은 것을 본다. */
            public void run() {
                Check.isFalse(window.isClosed(), "the window stays open");
                Check.equal("Page A", window.getPanel().getTitle(), "the other tab stays");
                Check.equal(0, ended, "the program goes on");
            }
        });
    }

    /**
     * A settings file that cannot be understood is kept under another name; the program starts with the defaults and tells the user.
     * <p>
     * 해석할 수 없는 설정 파일은 다른 이름으로 보존한다. 프로그램은 기본값으로 시작하고 사용자에게 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenSettingsFile() throws Exception {
        if (skipped()) {
            return;
        }
        final File file = new File(home, SettingsStore.FILE_NAME);
        TextFiles.write(file, "home.page=http://old.format/\n");
        onEdt(new Runnable() {
            /** Starts on the broken file. / 손상된 파일로 시작한다. */
            public void run() {
                BrowserApp other = restart();
                Check.equal("about:blank", other.getSettings().get(Settings.HOME_PAGE), "the defaults are used");
                Check.contains(String.valueOf(other.takeStorageProblem()), "settings.properties.broken", "the problem is remembered for the user");
                other.getSettings().set(Settings.HOME_PAGE, "http://new/");
            }
        });
        Check.equal("home.page=http://old.format/\n", TextFiles.read(new File(home, SettingsStore.FILE_NAME + TextFiles.BROKEN_SUFFIX)), "the broken file is kept");
        Check.contains(TextFiles.read(file), "home.page=http://new/\n", "the next change writes a new file");
    }
}
