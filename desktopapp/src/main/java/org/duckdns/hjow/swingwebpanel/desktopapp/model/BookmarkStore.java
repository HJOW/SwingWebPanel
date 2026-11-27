package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.IOException;

/**
 * The file of the bookmarks (plan item P31-06): <code>bookmarks.txt</code> in the directory of the user data, UTF-8 text with one entry
 * per line. The first line names the format. An entry line has fields separated by tabs: the kind (<code>F</code> for a folder,
 * <code>B</code> for a bookmark), the depth (0 for the entries of the root), the name and, for a bookmark, the address. Inside a field
 * a tab is written as <code>\t</code>, a line feed as <code>\n</code>, a carriage return as <code>\r</code> and a backslash as
 * <code>\\</code>. A missing file means no bookmarks; a file that cannot be understood is kept as <code>bookmarks.txt.broken</code> and
 * the application starts without bookmarks.
 * <p>
 * 북마크의 파일(계획 P31-06): 사용자 데이터 디렉터리의 <code>bookmarks.txt</code>. 한 줄에 항목 하나인 UTF-8 텍스트이다. 첫 줄은 형식의 이름이다.
 * 항목 줄은 탭으로 구분한 필드를 가진다: 종류(폴더는 <code>F</code>, 북마크는 <code>B</code>), 깊이(루트의 항목이 0), 이름, 그리고 북마크이면 주소.
 * 필드 안에서 탭은 <code>\t</code>, 줄 바꿈은 <code>\n</code>, 캐리지 리턴은 <code>\r</code>, 역슬래시는 <code>\\</code> 로 적는다. 파일이 없으면
 * 북마크가 없는 것이고, 해석할 수 없는 파일은 <code>bookmarks.txt.broken</code> 으로 보존한 뒤 북마크 없이 시작한다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * BookmarkStore store = new BookmarkStore(BrowserApp.homeDirectory());
 * store.load(model);
 * store.save(model);
 * </pre>
 */
public final class BookmarkStore {

    /** The name of the file. / 파일의 이름. */
    public static final String FILE_NAME = "bookmarks.txt";

    /** The first line of the file. / 파일의 첫 줄. */
    static final String HEADER = "swingwebpanel-bookmarks 1";

    /** The greatest depth of folders that is read; deeper files are taken as broken. / 읽는 폴더의 최대 깊이. 더 깊은 파일은 손상된 것으로 본다. */
    private static final int MAX_DEPTH = 64;

    /** The file. / 파일. */
    private final File file;

    /**
     * Creates the store for a directory.
     * <p>
     * 디렉터리에 대한 저장소를 만든다.
     *
     * @param directory the directory of the user data / 사용자 데이터 디렉터리
     */
    public BookmarkStore(File directory) {
        file = new File(directory, FILE_NAME);
    }

    /**
     * Returns the file of the bookmarks.
     * <p>
     * 북마크 파일을 반환한다.
     *
     * @return the file / 파일
     */
    public File getFile() {
        return file;
    }

    /**
     * Reads the file into a model, replacing what the model holds. A missing file gives empty bookmarks. A file that cannot be read or
     * understood is kept under the name <code>bookmarks.txt.broken</code> and the model is left empty.
     * <p>
     * 파일을 모델로 읽어 모델의 내용을 바꾼다. 파일이 없으면 빈 북마크가 된다. 읽거나 해석할 수 없는 파일은 <code>bookmarks.txt.broken</code> 이라는
     * 이름으로 보존하고 모델은 비워 둔다.
     *
     * @param model the model to fill / 채울 모델
     * @return false if a file was there but could not be used / 파일이 있었지만 쓸 수 없었으면 false
     */
    public boolean load(BookmarkModel model) {
        BookmarkNode parsed = null;
        boolean missing = false;
        try {
            String text = TextFiles.read(file);
            if (text == null) {
                missing = true;
            } else {
                parsed = parse(text);
            }
        } catch (IOException e) {
            parsed = null;
        }
        if (missing) {
            model.replaceAll(BookmarkNode.folder(""));
            return true;
        }
        // 해석하지 못한 파일은 사용자가 볼 수 있게 남겨 두고 빈 상태로 시작한다.
        if (parsed == null) {
            TextFiles.keepBroken(file);
            model.replaceAll(BookmarkNode.folder(""));
            return false;
        }
        model.replaceAll(parsed);
        return true;
    }

    /**
     * Writes the bookmarks of a model into the file.
     * <p>
     * 모델의 북마크를 파일에 쓴다.
     *
     * @param model the model / 모델
     * @throws IOException if the file cannot be written / 파일을 쓸 수 없으면
     */
    public void save(BookmarkModel model) throws IOException {
        TextFiles.write(file, format(model.getRoot()));
    }

    /**
     * Writes the entries of a root folder as the text of the file.
     * <p>
     * 루트 폴더의 항목을 파일의 텍스트로 적는다.
     *
     * @param root the root folder / 루트 폴더
     * @return the text / 텍스트
     */
    public static String format(BookmarkNode root) {
        StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        formatChildren(root, 0, out);
        return out.toString();
    }

    /**
     * Writes the entries of a folder, each folder followed by its own entries one level deeper.
     * <p>
     * 폴더의 항목을 적는다. 폴더 뒤에는 한 단계 깊게 그 폴더의 항목이 온다.
     *
     * @param folder the folder / 폴더
     * @param depth  the depth of its entries / 그 항목들의 깊이
     * @param out    where the lines go / 줄을 쓸 곳
     */
    private static void formatChildren(BookmarkNode folder, int depth, StringBuilder out) {
        // 항목마다 한 줄을 쓰고 폴더이면 그 안을 이어서 쓴다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode n = folder.getChild(i);
            out.append(n.isFolder() ? 'F' : 'B').append('\t').append(depth).append('\t').append(escape(n.getName()));
            if (!n.isFolder()) {
                out.append('\t').append(escape(n.getUrl()));
            }
            out.append('\n');
            if (n.isFolder()) {
                formatChildren(n, depth + 1, out);
            }
        }
    }

    /**
     * Reads the text of the file.
     * <p>
     * 파일의 텍스트를 읽는다.
     *
     * @param text the text / 텍스트
     * @return a root folder with the entries, or null if the text cannot be understood / 항목이 든 루트 폴더. 텍스트를 해석할 수 없으면 null
     */
    static BookmarkNode parse(String text) {
        String[] lines = text.split("\n", -1);
        if (lines.length == 0 || !HEADER.equals(strip(lines[0]))) {
            return null;
        }
        BookmarkNode root = BookmarkNode.folder("");
        // 깊이별로 지금 채우는 폴더를 기억한다. open[d] 는 깊이 d 의 항목이 들어갈 폴더이다.
        BookmarkNode[] open = new BookmarkNode[MAX_DEPTH + 2];
        open[0] = root;
        int deepest = 0;
        // 줄마다 항목 하나를 만든다. 빈 줄은 건너뛴다.
        for (int i = 1; i < lines.length; i++) {
            String line = strip(lines[i]);
            if (line.length() == 0) {
                continue;
            }
            String[] f = line.split("\t", -1);
            boolean folder = "F".equals(f[0]);
            if ((!folder && !"B".equals(f[0])) || f.length != (folder ? 3 : 4)) {
                return null;
            }
            int depth;
            try {
                depth = Integer.parseInt(f[1]);
            } catch (NumberFormatException e) {
                return null;
            }
            // 깊이는 바로 앞에 열린 폴더보다 깊을 수 없다.
            if (depth < 0 || depth > deepest || depth > MAX_DEPTH) {
                return null;
            }
            String name = unescape(f[2]);
            String url = folder ? "" : unescape(f[3]);
            if (name == null || url == null) {
                return null;
            }
            BookmarkNode node = folder ? BookmarkNode.folder(name) : BookmarkNode.bookmark(name, url);
            open[depth].insert(node, open[depth].getChildCount());
            if (folder) {
                open[depth + 1] = node;
                deepest = depth + 1;
            } else {
                deepest = depth;
            }
        }
        return root;
    }

    /**
     * Removes a carriage return at the end of a line (a file edited on Windows).
     * <p>
     * 줄 끝의 캐리지 리턴을 뗀다(Windows 에서 편집한 파일).
     *
     * @param line the line / 줄
     * @return the line without it / 그것을 뗀 줄
     */
    private static String strip(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    /**
     * Escapes a field.
     * <p>
     * 필드를 이스케이프한다.
     *
     * @param value the value / 값
     * @return the escaped text / 이스케이프한 텍스트
     */
    static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        // 문자마다 구분자와 역슬래시를 이스케이프한다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Reverses {@link #escape(String)}.
     * <p>
     * {@link #escape(String)} 을 되돌린다.
     *
     * @param text the escaped text / 이스케이프한 텍스트
     * @return the value, or null for an unknown or incomplete escape / 값. 모르는 이스케이프이거나 끝이 잘렸으면 null
     */
    static String unescape(String text) {
        if (text.indexOf('\\') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        // 문자마다 역슬래시 뒤의 글자를 원래 문자로 바꾼다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i + 1 >= text.length()) {
                return null;
            }
            char e = text.charAt(++i);
            if (e == '\\') {
                out.append('\\');
            } else if (e == 't') {
                out.append('\t');
            } else if (e == 'n') {
                out.append('\n');
            } else if (e == 'r') {
                out.append('\r');
            } else {
                return null;
            }
        }
        return out.toString();
    }
}
