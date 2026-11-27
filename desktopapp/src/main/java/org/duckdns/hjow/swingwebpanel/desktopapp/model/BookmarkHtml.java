package org.duckdns.hjow.swingwebpanel.desktopapp.model;

/**
 * Bookmarks in the Netscape bookmark file format, the HTML file that browsers write and read to exchange bookmarks (plan item P31-06).
 * Exporting writes the names and addresses with their special characters escaped. Importing reads the file as text: it looks for the
 * folder headings (<code>H3</code>), the links (<code>A</code> with <code>HREF</code>) and the lists (<code>DL</code>) and takes nothing
 * else from the file; the file is never shown as a page and nothing in it is run.
 * <p>
 * Netscape 북마크 파일 형식의 북마크. 브라우저들이 북마크를 주고받으려고 쓰고 읽는 HTML 파일이다(계획 P31-06). 내보내기는 이름과 주소의 특수 문자를
 * 이스케이프하여 쓴다. 가져오기는 파일을 텍스트로 읽는다: 폴더 제목(<code>H3</code>), 링크(<code>HREF</code> 가 있는 <code>A</code>), 목록
 * (<code>DL</code>)을 찾고 그 밖의 것은 파일에서 가져오지 않는다. 파일을 페이지로 보이는 일은 없고 그 안의 어떤 것도 실행하지 않는다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * String html = BookmarkHtml.export(model.getRoot());
 * BookmarkNode imported = BookmarkHtml.parse(html);
 * model.addAll(model.getRoot(), imported);
 * </pre>
 */
public final class BookmarkHtml {

    /** The greatest depth of folders that is read; deeper lists are put into the deepest folder. / 읽는 폴더의 최대 깊이. 더 깊은 목록은 가장 깊은 폴더에 넣는다. */
    private static final int MAX_DEPTH = 64;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private BookmarkHtml() {
    }

    /**
     * Writes the entries of a folder as a bookmark file.
     * <p>
     * 폴더의 항목을 북마크 파일로 적는다.
     *
     * @param root the folder whose entries are written / 항목을 적을 폴더
     * @return the text of the file / 파일의 텍스트
     */
    public static String export(BookmarkNode root) {
        StringBuilder out = new StringBuilder();
        out.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n");
        out.append("<!-- This is an automatically generated file. -->\n");
        out.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
        out.append("<TITLE>Bookmarks</TITLE>\n");
        out.append("<H1>Bookmarks</H1>\n");
        exportList(root, 0, out);
        return out.toString();
    }

    /**
     * Writes the list of a folder.
     * <p>
     * 폴더의 목록을 적는다.
     *
     * @param folder the folder / 폴더
     * @param depth  the depth, for the indentation / 들여쓰기에 쓰는 깊이
     * @param out    where the text goes / 텍스트를 쓸 곳
     */
    private static void exportList(BookmarkNode folder, int depth, StringBuilder out) {
        String pad = indent(depth);
        out.append(pad).append("<DL><p>\n");
        // 항목마다 폴더는 제목과 그 목록을, 북마크는 링크를 적는다.
        for (int i = 0; i < folder.getChildCount(); i++) {
            BookmarkNode n = folder.getChild(i);
            if (n.isFolder()) {
                out.append(pad).append("    <DT><H3>").append(escape(n.getName())).append("</H3>\n");
                exportList(n, depth + 1, out);
            } else {
                out.append(pad).append("    <DT><A HREF=\"").append(escape(n.getUrl())).append("\">").append(escape(n.getName())).append("</A>\n");
            }
        }
        out.append(pad).append("</DL><p>\n");
    }

    /**
     * Returns the indentation of a depth.
     * <p>
     * 깊이에 맞는 들여쓰기를 반환한다.
     *
     * @param depth the depth / 깊이
     * @return four spaces per level / 단계마다 공백 네 개
     */
    private static String indent(int depth) {
        StringBuilder pad = new StringBuilder();
        // 단계마다 공백 네 개를 더한다.
        for (int i = 0; i < depth; i++) {
            pad.append("    ");
        }
        return pad.toString();
    }

    /**
     * Escapes the characters that have a meaning in HTML text and attribute values.
     * <p>
     * HTML 의 텍스트와 속성 값에서 의미가 있는 문자를 이스케이프한다.
     *
     * @param text the text / 텍스트
     * @return the escaped text / 이스케이프한 텍스트
     */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        // 문자마다 HTML 특수 문자를 문자 참조로 바꾼다. 줄 바꿈은 공백으로 적는다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '\'') {
                out.append("&#39;");
            } else if (c == '\n' || c == '\r') {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Reads a bookmark file.
     * <p>
     * 북마크 파일을 읽는다.
     *
     * @param html the text of the file / 파일의 텍스트
     * @return a folder outside any model that holds the entries of the file; empty if the file has none / 파일의 항목을 담은, 모델 밖의
     *         폴더. 파일에 항목이 없으면 비어 있다
     */
    public static BookmarkNode parse(String html) {
        BookmarkNode root = BookmarkNode.folder("");
        if (html == null) {
            return root;
        }
        BookmarkNode[] stack = new BookmarkNode[MAX_DEPTH + 1];
        int depth = 0;
        stack[0] = root;
        // 마지막에 읽은 폴더 제목. 다음 <DL> 이 이 폴더의 목록을 연다.
        BookmarkNode pending = null;
        boolean firstList = true;
        int at = 0;
        // 태그를 차례로 찾아 폴더 제목, 링크, 목록의 시작과 끝만 처리한다.
        while (at < html.length()) {
            int lt = html.indexOf('<', at);
            if (lt < 0) {
                break;
            }
            // 주석은 통째로 건너뛴다.
            if (html.startsWith("<!--", lt)) {
                int end = html.indexOf("-->", lt + 4);
                at = end < 0 ? html.length() : end + 3;
                continue;
            }
            int gt = tagEnd(html, lt);
            if (gt < 0) {
                break;
            }
            String tag = html.substring(lt + 1, gt);
            String name = tagName(tag);
            at = gt + 1;
            if ("dl".equals(name)) {
                // 파일의 첫 목록은 루트의 목록이다. 그 뒤의 목록은 바로 앞 폴더 제목의 것이다.
                if (firstList) {
                    firstList = false;
                } else if (depth < MAX_DEPTH) {
                    BookmarkNode opened = pending != null ? pending : stack[depth];
                    stack[++depth] = opened;
                }
                pending = null;
            } else if ("/dl".equals(name)) {
                if (depth > 0) {
                    depth--;
                }
                pending = null;
            } else if ("h3".equals(name)) {
                int close = indexOfIgnoreCase(html, "</h3", at);
                String text = close < 0 ? "" : html.substring(at, close);
                pending = BookmarkNode.folder(clean(text));
                stack[depth].insert(pending, stack[depth].getChildCount());
                at = close < 0 ? at : close;
            } else if ("a".equals(name)) {
                int close = indexOfIgnoreCase(html, "</a", at);
                String text = close < 0 ? "" : html.substring(at, close);
                String href = attribute(tag, "href");
                // 주소가 없는 링크는 북마크가 아니다.
                if (href != null && href.trim().length() > 0) {
                    stack[depth].insert(BookmarkNode.bookmark(clean(text), unescape(href).trim()), stack[depth].getChildCount());
                }
                at = close < 0 ? at : close;
            }
        }
        return root;
    }

    /**
     * Finds the end of a tag, skipping over quoted attribute values (an address may contain <code>&gt;</code>).
     * <p>
     * 따옴표로 묶인 속성 값을 건너뛰며 태그의 끝을 찾는다(주소에 <code>&gt;</code> 가 들어 있을 수 있다).
     *
     * @param html the text / 텍스트
     * @param lt   the position of the opening angle bracket / 여는 꺾쇠의 위치
     * @return the position of the closing angle bracket, or -1 / 닫는 꺾쇠의 위치. 없으면 -1
     */
    private static int tagEnd(String html, int lt) {
        char quote = 0;
        // 따옴표 안이 아닌 곳의 첫 닫는 꺾쇠를 찾는다.
        for (int i = lt + 1; i < html.length(); i++) {
            char c = html.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the name of a tag in lower case, with the slash of an end tag.
     * <p>
     * 태그의 이름을 소문자로 반환한다. 끝 태그이면 슬래시를 포함한다.
     *
     * @param tag the text between the angle brackets / 꺾쇠 사이의 글
     * @return the name / 이름
     */
    private static String tagName(String tag) {
        int end = 0;
        // 이름은 공백이나 슬래시(자체 닫기) 앞까지이다. 맨 앞의 슬래시는 끝 태그의 표시이다.
        while (end < tag.length() && !Character.isWhitespace(tag.charAt(end)) && !(end > 0 && tag.charAt(end) == '/')) {
            end++;
        }
        return tag.substring(0, end).toLowerCase();
    }

    /**
     * Returns the value of an attribute of a start tag.
     * <p>
     * 시작 태그의 속성 값을 반환한다.
     *
     * @param tag  the text between the angle brackets / 꺾쇠 사이의 글
     * @param name the attribute name in lower case / 소문자로 된 속성 이름
     * @return the raw value, or null if the tag has no such attribute / 원래 값. 그런 속성이 없으면 null
     */
    static String attribute(String tag, String name) {
        int i = 0;
        // 태그 이름을 건너뛴다.
        while (i < tag.length() && !Character.isWhitespace(tag.charAt(i))) {
            i++;
        }
        // 속성을 하나씩 읽어 이름이 같은 것을 찾는다.
        while (i < tag.length()) {
            // 속성 사이의 공백을 건너뛴다.
            while (i < tag.length() && (Character.isWhitespace(tag.charAt(i)) || tag.charAt(i) == '/')) {
                i++;
            }
            int start = i;
            // 속성 이름은 공백이나 등호 앞까지이다.
            while (i < tag.length() && !Character.isWhitespace(tag.charAt(i)) && tag.charAt(i) != '=') {
                i++;
            }
            String attr = tag.substring(start, i).toLowerCase();
            // 등호 앞의 공백을 건너뛴다.
            while (i < tag.length() && Character.isWhitespace(tag.charAt(i))) {
                i++;
            }
            String value = "";
            if (i < tag.length() && tag.charAt(i) == '=') {
                i++;
                // 등호 뒤의 공백을 건너뛴다.
                while (i < tag.length() && Character.isWhitespace(tag.charAt(i))) {
                    i++;
                }
                if (i < tag.length() && (tag.charAt(i) == '"' || tag.charAt(i) == '\'')) {
                    char quote = tag.charAt(i++);
                    int end = tag.indexOf(quote, i);
                    end = end < 0 ? tag.length() : end;
                    value = tag.substring(i, end);
                    i = Math.min(tag.length(), end + 1);
                } else {
                    int from = i;
                    // 따옴표 없는 값은 공백 앞까지이다.
                    while (i < tag.length() && !Character.isWhitespace(tag.charAt(i))) {
                        i++;
                    }
                    value = tag.substring(from, i);
                }
            }
            if (attr.length() == 0 && value.length() == 0) {
                break;
            }
            if (name.equals(attr)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Finds text without regard to the case of ASCII letters.
     * <p>
     * ASCII 글자의 대소문자를 가리지 않고 글을 찾는다.
     *
     * @param text   the text to search in / 찾을 대상
     * @param needle the text to find, in lower case / 찾는 글(소문자)
     * @param from   the position to start at / 시작 위치
     * @return the position, or -1 / 위치. 없으면 -1
     */
    private static int indexOfIgnoreCase(String text, String needle, int from) {
        int last = text.length() - needle.length();
        // 위치마다 대소문자를 무시하고 비교한다.
        for (int i = Math.max(0, from); i <= last; i++) {
            if (text.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Turns the content of a heading or a link into a name: tags inside it are dropped, character references are decoded and white
     * space is collapsed.
     * <p>
     * 제목이나 링크의 내용을 이름으로 바꾼다: 안의 태그를 빼고, 문자 참조를 풀고, 공백을 하나로 줄인다.
     *
     * @param content the content between the start and the end tag / 시작 태그와 끝 태그 사이의 내용
     * @return the name / 이름
     */
    private static String clean(String content) {
        StringBuilder plain = new StringBuilder(content.length());
        boolean inTag = false;
        // 꺾쇠 사이의 글(태그)을 빼고 나머지를 모은다.
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inTag) {
                inTag = c != '>';
            } else if (c == '<') {
                inTag = true;
            } else {
                plain.append(c);
            }
        }
        String decoded = unescape(plain.toString());
        StringBuilder out = new StringBuilder(decoded.length());
        boolean space = false;
        // 이어진 공백과 줄 바꿈을 공백 하나로 줄인다.
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (Character.isWhitespace(c)) {
                space = out.length() > 0;
            } else {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Decodes the character references that bookmark files use: the five named ones and the numeric ones.
     * <p>
     * 북마크 파일이 쓰는 문자 참조를 푼다: 이름 있는 것 다섯 개와 숫자 참조.
     *
     * @param text the text / 텍스트
     * @return the decoded text / 푼 텍스트
     */
    static String unescape(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        // 앰퍼샌드마다 세미콜론까지를 참조로 보고 풀어 본다. 모르는 참조는 그대로 둔다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int semi = c == '&' ? text.indexOf(';', i) : -1;
            String decoded = semi > i && semi - i <= 10 ? reference(text.substring(i + 1, semi)) : null;
            if (decoded == null) {
                out.append(c);
            } else {
                out.append(decoded);
                i = semi;
            }
        }
        return out.toString();
    }

    /**
     * Decodes one character reference.
     * <p>
     * 문자 참조 하나를 푼다.
     *
     * @param name the text between the ampersand and the semicolon / 앰퍼샌드와 세미콜론 사이의 글
     * @return the character, or null for an unknown reference / 문자. 모르는 참조이면 null
     */
    private static String reference(String name) {
        if ("amp".equals(name)) {
            return "&";
        }
        if ("lt".equals(name)) {
            return "<";
        }
        if ("gt".equals(name)) {
            return ">";
        }
        if ("quot".equals(name)) {
            return "\"";
        }
        if ("apos".equals(name)) {
            return "'";
        }
        if (name.length() < 2 || name.charAt(0) != '#') {
            return null;
        }
        try {
            boolean hex = name.charAt(1) == 'x' || name.charAt(1) == 'X';
            int code = hex ? Integer.parseInt(name.substring(2), 16) : Integer.parseInt(name.substring(1));
            // 쓸 수 없는 코드 포인트는 참조로 보지 않는다.
            if (code <= 0 || code > 0x10FFFF || (code >= 0xD800 && code <= 0xDFFF)) {
                return null;
            }
            return new String(Character.toChars(code));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
