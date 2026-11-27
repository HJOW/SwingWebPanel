package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The bookmark model, its file and the bookmark file format of other browsers (plan item P31-06): changing the tree and what the
 * listeners hear, writing and reading the file with names that contain separators, what happens to a missing and to a broken file, and
 * exporting and importing HTML bookmark files.
 * <p>
 * 북마크 모델, 그 파일, 다른 브라우저의 북마크 파일 형식(계획 P31-06): 트리의 변경과 리스너가 듣는 것, 구분자가 든 이름으로 파일을 쓰고 읽는 일,
 * 없는 파일과 손상된 파일의 처리, HTML 북마크 파일의 내보내기와 가져오기.
 */
public class BookmarkModelTest {

    /** The number of changes that the listener heard. / 리스너가 들은 변경의 수. */
    private int changes;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BookmarkModelTest() {
    }

    /**
     * Creates a model whose changes are counted.
     * <p>
     * 변경을 세는 모델을 만든다.
     *
     * @return the model / 모델
     */
    private BookmarkModel counted() {
        BookmarkModel model = new BookmarkModel();
        model.addListener(new BookmarkModel.Listener() {
            /** Counts a change. / 변경을 센다. */
            public void bookmarksChanged(BookmarkModel m) {
                changes++;
            }
        });
        return model;
    }

    /**
     * Writes the names of the entries of a folder in order, folders with their entries in brackets.
     * <p>
     * 폴더의 항목 이름을 순서대로 적는다. 폴더는 그 항목을 대괄호 안에 적는다.
     *
     * @param folder the folder / 폴더
     * @return the names / 이름들
     */
    private static String shape(BookmarkNode folder) {
        StringBuilder out = new StringBuilder();
        // 항목마다 이름을 적고 폴더이면 안을 이어서 적는다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode n = folder.getChild(i);
            if (i > 0) {
                out.append(' ');
            }
            out.append(n.getName());
            if (n.isFolder()) {
                out.append('[').append(shape(n)).append(']');
            }
        }
        return out.toString();
    }

    /**
     * Entries are added, changed, moved and removed; every real change is announced once, and a change that changes nothing is not.
     * <p>
     * 항목을 더하고 바꾸고 옮기고 지운다. 실제 변경은 한 번씩 알리고, 아무것도 바꾸지 않는 변경은 알리지 않는다.
     */
    public void testTreeChangesAndListeners() {
        final BookmarkModel model = counted();
        BookmarkNode a = BookmarkNode.bookmark("a", "http://a/");
        BookmarkNode b = BookmarkNode.bookmark("b", "http://b/");
        final BookmarkNode f = BookmarkNode.folder("f");
        BookmarkNode c = BookmarkNode.bookmark("c", "http://c/");
        model.add(model.getRoot(), a);
        model.add(model.getRoot(), b);
        model.add(model.getRoot(), f);
        model.add(f, c);
        Check.equal("a b f[c]", shape(model.getRoot()), "entries are appended");
        Check.equal(4, changes, "one announcement per added entry");
        Check.isTrue(model.contains("http://c/") && !model.contains("http://x/"), "an address inside a folder is found");
        Check.same(c, model.find("http://c/"), "the bookmark with the address");
        Check.equal(3, model.countBookmarks(), "folders are not counted");
        model.add(model.getRoot(), BookmarkNode.bookmark("first", "http://1/"), 0);
        Check.equal("first a b f[c]", shape(model.getRoot()), "an entry is added at a position");
        model.edit(a, "A", "http://a2/");
        Check.equal("A", a.getName(), "the name is changed");
        Check.equal("http://a2/", a.getUrl(), "the address is changed");
        model.edit(f, "F", "http://ignored/");
        Check.equal("", f.getUrl(), "a folder has no address");
        changes = 0;
        model.edit(a, "A", "http://a2/");
        Check.equal(0, changes, "a change that changes nothing is not announced");
        Check.isTrue(model.shift(b, -1), "an entry moves up");
        Check.equal("first b A F[c]", shape(model.getRoot()), "after moving up");
        Check.isTrue(model.shift(b, 1), "an entry moves down");
        Check.equal("first A b F[c]", shape(model.getRoot()), "after moving down");
        Check.isFalse(model.shift(model.getRoot().getChild(0), -1), "the first entry cannot move up");
        Check.isFalse(model.shift(f, 1), "the last entry cannot move down");
        Check.isTrue(model.move(b, f, 0), "an entry moves into a folder");
        Check.equal("first A F[b c]", shape(model.getRoot()), "after moving into the folder");
        Check.isTrue(model.move(b, f, 2), "an entry moves to the end of its folder");
        Check.equal("first A F[c b]", shape(model.getRoot()), "after moving inside the folder");
        Check.isFalse(model.move(f, f, 0), "a folder cannot move into itself");
        BookmarkNode inner = BookmarkNode.folder("inner");
        model.add(f, inner);
        Check.isFalse(model.move(f, inner, 0), "a folder cannot move into a folder inside it");
        Check.isTrue(model.move(inner, model.getRoot(), 0), "a folder moves to the top");
        Check.equal("inner[] first A F[c b]", shape(model.getRoot()), "after moving the folder");
        Check.isTrue(model.remove(f), "a folder is removed with its entries");
        Check.isFalse(model.contains("http://c/"), "the entries of a removed folder are gone");
        Check.isFalse(model.remove(f), "an entry outside the model cannot be removed");
        Check.isFalse(model.remove(model.getRoot()), "the root cannot be removed");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Adds an entry that is already in the model. / 이미 모델에 있는 항목을 더한다. */
            public void run() {
                model.add(model.getRoot(), model.getRoot().getChild(0));
            }
        }, "an entry cannot be in two folders");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /** Adds into a folder that is not in the model. / 모델에 없는 폴더에 더한다. */
            public void run() {
                model.add(f, BookmarkNode.bookmark("x", "http://x/"));
            }
        }, "the parent must be in the model");
    }

    /**
     * The file keeps names and addresses with tabs, line breaks and backslashes, nested folders and their order; a missing file gives no
     * bookmarks, and a file that cannot be understood is kept under another name.
     * <p>
     * 파일은 탭, 줄 바꿈, 역슬래시가 든 이름과 주소, 중첩 폴더, 순서를 보존한다. 파일이 없으면 북마크가 없고, 해석할 수 없는 파일은 다른 이름으로
     * 보존한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileRoundTripAndBrokenFile() throws Exception {
        File dir = File.createTempFile("swp-bookmarks", "");
        Check.isTrue(dir.delete() && dir.mkdir(), "a temporary directory");
        try {
            BookmarkStore store = new BookmarkStore(new File(dir, "sub"));
            BookmarkModel model = new BookmarkModel();
            Check.isTrue(store.load(model), "a missing file is not a problem");
            Check.equal(0, model.getRoot().getChildCount(), "a missing file gives no bookmarks");
            BookmarkNode folder = BookmarkNode.folder("한글\t폴더\\이름");
            BookmarkNode deep = BookmarkNode.folder("deep");
            model.add(model.getRoot(), BookmarkNode.bookmark("line\nbreak\rhere", "http://example.com/?a=1&b=\\2"));
            model.add(model.getRoot(), folder);
            model.add(folder, BookmarkNode.bookmark("", "http://no.name/"));
            model.add(folder, deep);
            model.add(deep, BookmarkNode.bookmark("leaf", "file:///C:/a b.html"));
            model.add(folder, BookmarkNode.bookmark("after", "http://after/"));
            model.add(model.getRoot(), BookmarkNode.folder("empty"));
            model.add(model.getRoot(), BookmarkNode.bookmark("last", "http://last/"));
            store.save(model);
            Check.isTrue(store.getFile().isFile(), "the directory is created and the file written");
            BookmarkModel back = new BookmarkModel();
            Check.isTrue(store.load(back), "the file is read");
            Check.equal(BookmarkStore.format(model.getRoot()), BookmarkStore.format(back.getRoot()), "the read bookmarks are the written ones");
            Check.equal("한글\t폴더\\이름", back.getRoot().getChild(1).getName(), "a name with a tab and a backslash");
            Check.equal("line\nbreak\rhere", back.getRoot().getChild(0).getName(), "a name with line breaks");
            Check.equal("leaf", back.getRoot().getChild(1).getChild(1).getChild(0).getName(), "an entry two folders deep");
            Check.equal("after", back.getRoot().getChild(1).getChild(2).getName(), "an entry after a nested folder goes back to its folder");
            Check.equal(0, back.getRoot().getChild(2).getChildCount(), "an empty folder stays empty");
            Check.equal("last", back.getRoot().getChild(3).getName(), "the order is kept");
            TextFiles.write(store.getFile(), "this is not a bookmark file\n");
            Check.isFalse(store.load(back), "a file that cannot be understood is reported");
            Check.equal(0, back.getRoot().getChildCount(), "and gives no bookmarks");
            File kept = new File(store.getFile().getPath() + TextFiles.BROKEN_SUFFIX);
            Check.equal("this is not a bookmark file\n", TextFiles.read(kept), "the broken file is kept under another name");
            Check.isFalse(store.getFile().exists(), "the place of the file is free for a new one");
            Check.isNull(BookmarkStore.parse(BookmarkStore.HEADER + "\nB\t1\tdeep\thttp://x/\n"), "an entry deeper than its folder");
            Check.isNull(BookmarkStore.parse(BookmarkStore.HEADER + "\nX\t0\tname\n"), "an unknown kind");
            Check.isNull(BookmarkStore.parse(BookmarkStore.HEADER + "\nB\t0\tname\n"), "a bookmark without an address");
            Check.isNull(BookmarkStore.parse(BookmarkStore.HEADER + "\nF\tzero\tname\n"), "a depth that is not a number");
            Check.isNull(BookmarkStore.parse(BookmarkStore.HEADER + "\nF\t0\tbad\\escape\n"), "an unknown escape");
            Check.notNull(BookmarkStore.parse(BookmarkStore.HEADER + "\r\nF\t0\tname\r\n\r\nB\t1\tb\thttp://b/\r\n"), "Windows line ends and empty lines");
            deleteTree(dir);
        } finally {
            deleteTree(dir);
        }
    }

    /**
     * Deletes a directory with everything in it.
     * <p>
     * 디렉터리와 그 안의 모든 것을 지운다.
     *
     * @param f the file or directory / 파일 또는 디렉터리
     */
    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        // 디렉터리이면 안의 것부터 지운다.
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                deleteTree(children[i]);
            }
        }
        f.delete();
    }

    /**
     * Bookmarks are exported as an HTML bookmark file with escaped names and read back; a file of another browser with attributes,
     * comments, markup inside names and a script is read as text only.
     * <p>
     * 북마크를 이름이 이스케이프된 HTML 북마크 파일로 내보내고 다시 읽는다. 속성, 주석, 이름 안의 마크업, 스크립트가 든 다른 브라우저의 파일은
     * 텍스트로만 읽는다.
     */
    public void testHtmlExportAndImport() {
        BookmarkModel model = new BookmarkModel();
        BookmarkNode folder = BookmarkNode.folder("Tom & Jerry <b>");
        model.add(model.getRoot(), BookmarkNode.bookmark("\"quoted\" 'name'", "http://example.com/?a=1&b=<2>"));
        model.add(model.getRoot(), folder);
        model.add(folder, BookmarkNode.bookmark("한글 이름", "http://example.com/한글"));
        model.add(folder, BookmarkNode.folder("empty"));
        model.add(model.getRoot(), BookmarkNode.bookmark("last", "http://last/"));
        String html = BookmarkHtml.export(model.getRoot());
        Check.contains(html, "<!DOCTYPE NETSCAPE-Bookmark-file-1>", "the file starts with the type of the format");
        Check.contains(html, "<DT><H3>Tom &amp; Jerry &lt;b&gt;</H3>", "a folder name is escaped");
        Check.contains(html, "<A HREF=\"http://example.com/?a=1&amp;b=&lt;2&gt;\">&quot;quoted&quot; &#39;name&#39;</A>", "an address and a name are escaped");
        BookmarkNode back = BookmarkHtml.parse(html);
        Check.equal(BookmarkStore.format(model.getRoot()), BookmarkStore.format(back), "the imported bookmarks are the exported ones");
        String foreign = "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<!-- <DT><A HREF=\"http://in.a.comment/\">no</A> -->\n"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n<title>Bookmarks</title><h1>Bookmarks</h1>\n"
                + "<script>document.title = 'ran';</" + "script>\n"
                + "<dl><p>\n"
                + "  <dt><h3 add_date=\"1\" personal_toolbar_folder=\"true\">Bar &amp; <i>more</i></h3>\n"
                + "  <dl><p>\n"
                + "    <dt><a href='http://example.com/a>b' add_date=\"2\" icon=\"data:image/png;base64,AAAA\">First\n   link &#44; &#x41;</a>\n"
                + "    <dt><a href=http://example.com/unquoted>Unquoted</a>\n"
                + "    <dd>A description that is not a bookmark\n"
                + "    <dt><a name=\"anchor\">No address</a>\n"
                + "    <dt><h3>Inner</h3>\n    <dl><p>\n      <dt><a HREF=\"http://inner/\">In</a>\n    </dl><p>\n"
                + "  </dl><p>\n"
                + "  <hr>\n"
                + "  <dt><a href=\"http://top/\">Top &unknown; &amp;amp;</a>\n"
                + "</dl><p>\n";
        BookmarkNode read = BookmarkHtml.parse(foreign);
        Check.equal(2, read.getChildCount(), "a folder and a bookmark at the top");
        BookmarkNode bar = read.getChild(0);
        Check.equal("Bar & more", bar.getName(), "markup inside a name is dropped and references are decoded");
        Check.equal(3, bar.getChildCount(), "two links and a folder; the link without an address is left out");
        Check.equal("First link , A", bar.getChild(0).getName(), "white space is collapsed and numeric references are decoded");
        Check.equal("http://example.com/a>b", bar.getChild(0).getUrl(), "an address with an angle bracket in quotes");
        Check.equal("http://example.com/unquoted", bar.getChild(1).getUrl(), "an address without quotes");
        Check.equal("Inner", bar.getChild(2).getName(), "a nested folder");
        Check.equal("http://inner/", bar.getChild(2).getChild(0).getUrl(), "the entry of the nested folder");
        Check.equal("Top &unknown; &amp;", read.getChild(1).getName(), "an unknown reference stays, a known one is decoded once");
        Check.equal(0, BookmarkHtml.parse("<html><body><p>Just a page</p></body></html>").getChildCount(), "a page without links gives nothing");
        Check.equal(0, BookmarkHtml.parse(null).getChildCount(), "no text gives nothing");
        Check.equal(1, BookmarkHtml.parse("<a href=\"http://bare/\">bare").getChildCount(), "a link without a list and an end tag");
    }
}
