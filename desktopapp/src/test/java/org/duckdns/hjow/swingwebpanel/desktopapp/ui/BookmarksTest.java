package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkModel;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkNode;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.BookmarkStore;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.TextFiles;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Bookmarks in the browser window (plan item P31-06): adding the page of a tab and of all tabs, the bookmark menu that opens entries in
 * the tab, in a new tab or in a new window, the bookmarks that all windows share and that are saved for the next start, and the
 * bookmark manager.
 * <p>
 * 브라우저 창의 북마크(계획 P31-06): 탭의 페이지와 모든 탭의 페이지를 더하는 일, 항목을 그 탭·새 탭·새 창에서 여는 북마크 메뉴, 모든 창이 공유하며
 * 다음 시작을 위해 저장되는 북마크, 북마크 관리 창.
 */
public class BookmarksTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BookmarksTest() {
    }

    /**
     * Fills the bookmark menu as it is done when the menu opens, and returns it.
     * <p>
     * 메뉴가 열릴 때 하듯이 북마크 메뉴를 채우고 반환한다.
     *
     * @param w the window / 창
     * @return the menu / 메뉴
     */
    private static JMenu openMenu(BrowserWindow w) {
        JMenu menu = w.getBrowserMenu().getBookmarkMenu();
        w.getBrowserMenu().refreshItems(menu);
        w.getBrowserMenu().enableAll();
        return menu;
    }

    /**
     * Finds an item of a menu by its text.
     * <p>
     * 메뉴에서 글로 항목을 찾는다.
     *
     * @param menu the menu / 메뉴
     * @param text the text of the item / 항목의 글
     * @return the item, or null / 항목. 없으면 null
     */
    private static JMenuItem item(JMenu menu, String text) {
        // 메뉴의 항목마다 글을 비교한다(구분선은 건너뛴다).
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem it = menu.getItem(i);
            if (it != null && text.equals(it.getText())) {
                return it;
            }
        }
        return null;
    }

    /**
     * Chooses a menu item as a click with modifier keys does.
     * <p>
     * 보조 키를 누른 클릭처럼 메뉴 항목을 고른다.
     *
     * @param it        the item / 항목
     * @param modifiers the modifier mask of the action event / 동작 이벤트의 보조 키 마스크
     */
    private static void choose(JMenuItem it, int modifiers) {
        ActionListener[] listeners = it.getActionListeners();
        // 항목의 리스너마다 동작 이벤트를 보낸다.
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].actionPerformed(new ActionEvent(it, ActionEvent.ACTION_PERFORMED, it.getActionCommand(), modifiers));
        }
    }

    /**
     * The page of a tab is bookmarked (a second time only after a question), all tabs go into a new folder, the menu lists the entries
     * and opens them where the modifier keys say, a second window sees the same bookmarks, and a new start reads them from the file.
     * <p>
     * 탭의 페이지를 북마크에 더하고(두 번째는 물어본 뒤에만), 모든 탭을 새 폴더에 넣고, 메뉴가 항목을 나열하며 보조 키가 정한 곳에서 열고, 둘째
     * 창이 같은 북마크를 보고, 새로 시작하면 파일에서 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAddOpenShareAndSave() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final String b = pages.page("b.html", "Page B", "<p>second</p>");
        onEdt(new Runnable() {
            /** Tries to bookmark the empty page, then loads two tabs. / 빈 페이지를 북마크해 본 뒤 두 탭을 읽는다. */
            public void run() {
                Check.isTrue(window.perform(Commands.ADD_BOOKMARK), "the command is known");
                Check.equal(0, app.getBookmarks().countBookmarks(), "the empty page is not bookmarked");
                Check.contains(window.getStatusBar().getStatusText(), "empty page", "the status bar says why");
                window.navigate(a);
                window.newTab(b, false);
            }
        });
        awaitTab(0, "Page A");
        awaitTab(1, "Page B");
        onEdt(new Runnable() {
            /** Bookmarks the page and all tabs and uses the menu. / 페이지와 모든 탭을 북마크에 더하고 메뉴를 쓴다. */
            public void run() {
                BookmarkModel model = app.getBookmarks();
                window.perform(Commands.ADD_BOOKMARK);
                Check.equal(1, model.countBookmarks(), "the page is bookmarked");
                Check.equal("Page A", model.getRoot().getChild(0).getName(), "the name is the title");
                Check.equal(a, model.getRoot().getChild(0).getUrl(), "the address is the address of the page");
                Check.contains(window.getStatusBar().getStatusText(), "Bookmarked: Page A", "the status bar confirms");
                prompts.answer = false;
                window.perform(Commands.ADD_BOOKMARK);
                Check.equal(1, prompts.asked, "bookmarking the same page again asks first");
                Check.equal(1, model.countBookmarks(), "a refusal adds nothing");
                prompts.answer = true;
                window.perform(Commands.ADD_BOOKMARK);
                Check.equal(2, model.countBookmarks(), "an agreement adds the page again");
                model.remove(model.getRoot().getChild(1));
                prompts.text = null;
                window.perform(Commands.BOOKMARK_ALL_TABS);
                Check.equal(1, model.getRoot().getChildCount(), "a cancelled folder name adds nothing");
                prompts.text = "  Both tabs ";
                window.perform(Commands.BOOKMARK_ALL_TABS);
                BookmarkNode folder = model.getRoot().getChild(1);
                Check.isTrue(folder.isFolder() && "Both tabs".equals(folder.getName()), "a folder with the trimmed name");
                Check.equal(2, folder.getChildCount(), "one bookmark per tab");
                Check.equal(b, folder.getChild(1).getUrl(), "the second tab");
                JMenu menu = openMenu(window);
                Check.notNull(item(menu, "Page A"), "the menu lists the bookmark");
                JMenu sub = (JMenu) item(menu, "Both tabs");
                Check.notNull(sub, "the menu lists the folder as a submenu");
                Check.notNull(item(sub, "Page B"), "the submenu lists the entries of the folder");
                Check.equal(a, item(menu, "Page A").getToolTipText(), "the tool tip of an item is the address");
                window.selectTab(1);
                choose(item(sub, "Page A"), 0);
            }
        });
        awaitTab(1, "Page A");
        onEdt(new Runnable() {
            /** Opens bookmarks in a new tab, in a new window and a whole folder. / 북마크를 새 탭, 새 창에서 열고 폴더 전체를 연다. */
            public void run() {
                JMenu menu = openMenu(window);
                choose(item(menu, "Page A"), ActionEvent.CTRL_MASK);
                Check.equal(3, window.getTabs().size(), "Ctrl opens the bookmark in a new tab");
                Check.same(window.getTabs().get(2), window.getActiveTab(), "which becomes the active tab");
                choose(item((JMenu) item(menu, "Both tabs"), "Page B"), ActionEvent.SHIFT_MASK);
                Check.equal(2, app.getWindows().size(), "Shift opens the bookmark in a new window");
                choose(item((JMenu) item(menu, "Both tabs"), "Open All in Tabs"), 0);
                Check.equal(5, window.getTabs().size(), "the folder opens each of its bookmarks in a tab");
            }
        });
        awaitTab(2, "Page A");
        awaitTab(app.getWindows().get(1), 0, "Page B");
        awaitTab(3, "Page A");
        awaitTab(4, "Page B");
        onEdt(new Runnable() {
            /** Looks at the second window and at a new start. / 둘째 창과 새 시작을 본다. */
            public void run() {
                BrowserWindow second = app.getWindows().get(1);
                Check.same(window.getTabs().get(3), window.getActiveTab(), "the first tab of a folder comes to the front");
                app.getBookmarks().add(app.getBookmarks().getRoot(), BookmarkNode.bookmark("Added later", "http://later/"));
                Check.notNull(item(openMenu(second), "Added later"), "all windows share the bookmarks");
                BrowserApp restarted = new BrowserApp(new Runnable() {
                    /** Does nothing at the end of this second application. / 이 둘째 응용 프로그램이 끝날 때 아무것도 하지 않는다. */
                    public void run() {
                    }
                });
                Check.equal(BookmarkStore.format(app.getBookmarks().getRoot()), BookmarkStore.format(restarted.getBookmarks().getRoot()),
                        "a new start reads the saved bookmarks");
                Check.equal(4, restarted.getBookmarks().countBookmarks(), "four bookmarks were saved");
            }
        });
    }

    /**
     * The bookmark manager shows the selected entry, changes its name and address, adds folders, moves entries up, down and into
     * folders, deletes them (a folder with entries only after a question), opens them, and imports and exports bookmark files.
     * <p>
     * 북마크 관리 창은 선택한 항목을 보이고, 이름과 주소를 바꾸고, 폴더를 더하고, 항목을 위·아래·폴더 안으로 옮기고, 지우고(항목이 든 폴더는 물어본
     * 뒤에만), 열고, 북마크 파일을 가져오고 내보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBookmarkManager() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("a.html", "Page A", "<p>first</p>");
        final File exported = pages.file("exported.html");
        pages.page("exported.html", "placeholder", "");
        final BookmarkDialog[] dialog = new BookmarkDialog[1];
        onEdt(new Runnable() {
            /** Works through the buttons of the manager. / 관리 창의 단추들을 차례로 써 본다. */
            public void run() {
                BookmarkModel model = app.getBookmarks();
                BookmarkNode one = BookmarkNode.bookmark("One", a);
                BookmarkNode two = BookmarkNode.bookmark("Two", "http://two/");
                model.add(model.getRoot(), one);
                model.add(model.getRoot(), two);
                window.perform(Commands.MANAGE_BOOKMARKS);
                BookmarkDialog d = window.showBookmarkManager();
                dialog[0] = d;
                Check.isFalse(d.isVisible(), "the manager of a hidden window is not shown");
                Check.isNull(d.getSelected(), "nothing is selected at first");
                d.select(two);
                Check.same(two, d.getSelected(), "an entry is selected");
                d.type("  Second ", " http://second/ ");
                d.apply();
                Check.equal("Second", two.getName(), "Save Changes writes the trimmed name");
                Check.equal("http://second/", two.getUrl(), "and the trimmed address");
                Check.same(two, d.getSelected(), "the entry stays selected after the change");
                d.shift(-1);
                Check.same(two, model.getRoot().getChild(0), "Up moves the entry");
                d.shift(1);
                Check.same(two, model.getRoot().getChild(1), "Down moves it back");
                prompts.text = "Folder";
                d.select(null);
                d.newFolder();
                BookmarkNode folder = model.getRoot().getChild(2);
                Check.isTrue(folder.isFolder() && "Folder".equals(folder.getName()), "New Folder adds a folder at the top level");
                Check.same(folder, d.getSelected(), "the new folder is selected");
                prompts.text = "Inner";
                d.newFolder();
                Check.equal("Inner", folder.getChild(0).getName(), "a new folder goes into the selected folder");
                d.select(two);
                Check.isTrue(d.moveTo(folder), "an entry moves into a folder");
                Check.same(folder, two.getParent(), "the entry is in the folder now");
                Check.same(two, d.getSelected(), "and stays selected");
                d.select(folder);
                Check.isFalse(d.moveTo(folder.getChild(0)), "a folder does not move into a folder inside it");
                prompts.answer = false;
                d.delete();
                Check.equal(1, prompts.asked, "deleting a folder with entries asks first");
                Check.same(model.getRoot(), folder.getParent(), "a refusal keeps the folder");
                d.select(one);
                d.open(BookmarkDialog.NEW_TAB);
                Check.equal(2, window.getTabs().size(), "Open in New Tab opens a tab");
                d.open(BookmarkDialog.NEW_WINDOW);
                Check.equal(2, app.getWindows().size(), "Open in New Window opens a window");
                d.open(BookmarkDialog.HERE);
                prompts.file = exported;
                d.exportFile();
            }
        });
        awaitTab(1, "Page A");
        final String html = TextFiles.read(exported);
        Check.contains(html, "<DT><H3>Folder</H3>", "the exported file holds the folder");
        Check.contains(html, "<A HREF=\"http://second/\">Second</A>", "and the bookmarks");
        onEdt(new Runnable() {
            /** Deletes everything and imports the exported file. / 모두 지우고 내보낸 파일을 가져온다. */
            public void run() {
                BookmarkModel model = app.getBookmarks();
                BookmarkDialog d = dialog[0];
                prompts.answer = true;
                d.select(model.getRoot().getChild(1));
                d.delete();
                d.select(model.getRoot().getChild(0));
                d.delete();
                Check.equal(0, model.getRoot().getChildCount(), "Delete removes the entries");
                Check.isNull(d.getSelected(), "nothing is selected after the selected entry was deleted");
                prompts.file = exported;
                d.importFile();
                BookmarkNode imported = model.getRoot().getChild(0);
                Check.equal("Imported from exported.html", imported.getName(), "the imported entries go into a folder named after the file");
                Check.equal("One", imported.getChild(0).getName(), "the first imported bookmark");
                Check.equal("Second", imported.getChild(1).getChild(1).getName(), "an imported bookmark inside its folder");
                Check.same(imported, d.getSelected(), "the imported folder is selected");
                prompts.file = pages.file("a.html");
                int before = prompts.messages.size();
                d.importFile();
                Check.equal(1, model.getRoot().getChildCount(), "a file without bookmarks adds nothing");
                Check.contains(prompts.messages.get(prompts.messages.size() - 1), "No bookmarks", "and the user is told");
                Check.equal(before + 1, prompts.messages.size(), "one message");
                d.close();
                Check.isFalse(d.isDisplayable(), "Close disposes the manager");
                model.remove(imported);
                Check.notNull(window.showBookmarkManager(), "the manager can be opened again");
                Check.isTrue(window.showBookmarkManager() != d, "as a new dialog");
            }
        });
    }

    /**
     * A bookmark file that cannot be understood is kept under another name and the application starts without bookmarks; the next
     * change writes a new file.
     * <p>
     * 해석할 수 없는 북마크 파일은 다른 이름으로 보존하고 응용 프로그램은 북마크 없이 시작한다. 다음 변경이 새 파일을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenBookmarkFile() throws Exception {
        if (skipped()) {
            return;
        }
        final File file = new File(home, BookmarkStore.FILE_NAME);
        TextFiles.write(file, "broken\n");
        onEdt(new Runnable() {
            /** Starts an application on the broken file. / 손상된 파일로 응용 프로그램을 시작한다. */
            public void run() {
                BrowserApp other = new BrowserApp(new Runnable() {
                    /** Does nothing at the end. / 끝날 때 아무것도 하지 않는다. */
                    public void run() {
                    }
                });
                Check.equal(0, other.getBookmarks().getRoot().getChildCount(), "no bookmarks");
                Check.contains(String.valueOf(other.takeStorageProblem()), "bookmarks.txt.broken", "the problem is remembered for the user");
                Check.isNull(other.takeStorageProblem(), "and told once");
                other.getBookmarks().add(other.getBookmarks().getRoot(), BookmarkNode.bookmark("New", "http://new/"));
            }
        });
        Check.equal("broken\n", TextFiles.read(new File(home, BookmarkStore.FILE_NAME + TextFiles.BROKEN_SUFFIX)), "the broken file is kept");
        Check.contains(TextFiles.read(file), "B\t0\tNew\thttp://new/", "the next change writes a new file");
    }
}
