package org.duckdns.hjow.swingwebpanel.desktopapp.model;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * Turns what the user typed into the address bar into an absolute address that the component can load (plan item P31-03). The rules, in
 * this order:
 * <ol>
 * <li>Empty input gives nothing.</li>
 * <li>A local file path (a Windows drive path, a UNC path, or an existing absolute path) becomes a <code>file:</code> address.</li>
 * <li>Input with a scheme is used as it is. The component itself decides whether the scheme is allowed and shows its error page for a
 * blocked one. <code>host:port</code> is not a scheme.</li>
 * <li>Text that looks like a search (it has white space, or it is one word without a dot that is not <code>localhost</code>) goes to the
 * search address template of the settings, with <code>%s</code> replaced by the encoded text. Without a template, one word is treated
 * as a host name and text with white space cannot be opened.</li>
 * <li>Everything else is a host or an address without scheme and gets <code>https://</code> in front (the component retries with HTTP
 * when the secure connection fails).</li>
 * </ol>
 * <p>
 * 사용자가 주소 표시줄에 입력한 것을 컴포넌트가 읽을 수 있는 절대 주소로 바꾼다(계획 P31-03). 규칙은 이 순서대로이다:
 * <ol>
 * <li>빈 입력은 아무것도 주지 않는다.</li>
 * <li>로컬 파일 경로(Windows 드라이브 경로, UNC 경로, 존재하는 절대 경로)는 <code>file:</code> 주소가 된다.</li>
 * <li>스킴이 있는 입력은 그대로 쓴다. 스킴을 허용할지는 컴포넌트가 판정하고 차단된 스킴에는 컴포넌트의 오류 화면이 보인다. <code>호스트:포트</code> 는
 * 스킴이 아니다.</li>
 * <li>검색처럼 보이는 글(공백이 있거나, 점이 없는 한 낱말이면서 <code>localhost</code> 가 아닌 것)은 설정의 검색 주소 서식으로 간다. 서식의
 * <code>%s</code> 를 인코딩한 글로 바꾼다. 서식이 없으면 한 낱말은 호스트 이름으로 보고, 공백이 있는 글은 열 수 없다.</li>
 * <li>그 밖의 것은 스킴 없는 호스트나 주소이며 앞에 <code>https://</code> 를 붙인다(보안 연결이 실패하면 컴포넌트가 HTTP 로 다시 시도한다).</li>
 * </ol>
 * <p>
 * 사용 예 / Example:
 * <pre>
 * AddressInput.Result r = AddressInput.resolve("example.com/a b", "");
 * if (r.getUrl() != null) { panel.setPage(r.getUrl()); }      // https://example.com/a b
 * </pre>
 */
public final class AddressInput {

    /**
     * What an input was understood as.
     * <p>
     * 입력을 무엇으로 이해했는지.
     */
    public enum Kind {
        /** Nothing was typed. / 아무것도 입력하지 않았다. */
        EMPTY,
        /** An address with a scheme, used as typed. / 스킴이 있는 주소. 입력한 그대로 쓴다. */
        ADDRESS,
        /** A host or address without scheme; <code>https://</code> was added. / 스킴 없는 호스트나 주소. <code>https://</code> 를 붙였다. */
        HOST,
        /** A local file path, turned into a <code>file:</code> address. / 로컬 파일 경로. <code>file:</code> 주소로 바꿨다. */
        FILE,
        /** A search, turned into an address with the search template. / 검색. 검색 서식으로 주소를 만들었다. */
        SEARCH,
        /** A search, but the settings have no search address template. / 검색이지만 설정에 검색 주소 서식이 없다. */
        NO_SEARCH
    }

    /**
     * The result of resolving an input: what it was understood as and the address to load.
     * <p>
     * 입력을 해석한 결과: 무엇으로 이해했는지와 읽을 주소.
     */
    public static final class Result {

        /** What the input was understood as. / 입력을 무엇으로 이해했는지. */
        private final Kind kind;

        /** The address to load, or null. / 읽을 주소. 없으면 null. */
        private final String url;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param kind what the input was understood as / 입력을 무엇으로 이해했는지
         * @param url  the address to load, or null / 읽을 주소. 없으면 null
         */
        Result(Kind kind, String url) {
            this.kind = kind;
            this.url = url;
        }

        /**
         * Returns what the input was understood as.
         * <p>
         * 입력을 무엇으로 이해했는지 반환한다.
         *
         * @return the kind / 종류
         */
        public Kind getKind() {
            return kind;
        }

        /**
         * Returns the absolute address to load; null for {@link Kind#EMPTY} and {@link Kind#NO_SEARCH}.
         * <p>
         * 읽을 절대 주소를 반환한다. {@link Kind#EMPTY} 와 {@link Kind#NO_SEARCH} 이면 null 이다.
         *
         * @return the address or null / 주소 또는 null
         */
        public String getUrl() {
            return url;
        }
    }

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private AddressInput() {
    }

    /**
     * Resolves what the user typed.
     * <p>
     * 사용자가 입력한 것을 해석한다.
     *
     * @param input          the text of the address bar, or null / 주소 표시줄의 글. 없으면 null
     * @param searchTemplate the search address with <code>%s</code> for the query, or null or empty when there is none / 질의 자리에
     *                       <code>%s</code> 가 있는 검색 주소. 없으면 null 이나 빈 문자열
     * @return the result; never null / 결과. null 이 아니다
     */
    public static Result resolve(String input, String searchTemplate) {
        String text = input == null ? "" : input.trim();
        if (text.length() == 0) {
            return new Result(Kind.EMPTY, null);
        }
        if (isFilePath(text)) {
            return new Result(Kind.FILE, fileUrl(text));
        }
        if (schemeLength(text) > 0) {
            return new Result(Kind.ADDRESS, text);
        }
        boolean spaced = hasWhiteSpace(text);
        if (spaced || isSingleWord(text)) {
            String template = searchTemplate == null ? "" : searchTemplate.trim();
            // 검색 서식이 있으면 검색 주소를 만든다.
            if (template.length() > 0) {
                return new Result(Kind.SEARCH, search(template, text));
            }
            // 서식이 없으면 공백 있는 글은 열 수 없고, 한 낱말은 호스트 이름으로 본다.
            if (spaced) {
                return new Result(Kind.NO_SEARCH, null);
            }
        }
        return new Result(Kind.HOST, "https://" + text);
    }

    /**
     * Tells whether a text is a local file path: a Windows drive path, a UNC path, or an absolute path that exists.
     * <p>
     * 글이 로컬 파일 경로인지 알려 준다: Windows 드라이브 경로, UNC 경로, 또는 존재하는 절대 경로.
     *
     * @param text the trimmed input / 앞뒤 공백을 뗀 입력
     * @return true for a file path / 파일 경로이면 true
     */
    private static boolean isFilePath(String text) {
        // "C:\..." 나 "C:/..." 는 드라이브 경로이다.
        if (text.length() >= 3 && isLetter(text.charAt(0)) && text.charAt(1) == ':' && (text.charAt(2) == '\\' || text.charAt(2) == '/')) {
            return true;
        }
        if (text.startsWith("\\\\")) {
            return true;
        }
        // "/" 로 시작하는 글은 실제로 있는 경로일 때만 파일로 본다.
        return text.startsWith("/") && !text.startsWith("//") && new File(text).exists();
    }

    /**
     * Builds the <code>file:</code> address of a path without asking the file system (a UNC path would otherwise wait for the network):
     * <code>C:/dir/a b.html</code> becomes <code>file:///C:/dir/a%20b.html</code>.
     * <p>
     * 파일 시스템에 묻지 않고 경로의 <code>file:</code> 주소를 만든다(그러지 않으면 UNC 경로는 네트워크를 기다린다):
     * <code>C:/dir/a b.html</code> 은 <code>file:///C:/dir/a%20b.html</code> 이 된다.
     *
     * @param path a local path / 로컬 경로
     * @return the address / 주소
     */
    static String fileUrl(String path) {
        String p = path.replace('\\', '/');
        // 드라이브 경로는 앞에 슬래시를 붙여 URI 의 절대 경로로 만든다.
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        try {
            return new URI("file", "", p, null).toString();
        } catch (URISyntaxException e) {
            // 경로로 URI 를 만들 수 없는 드문 경우에는 플랫폼의 변환에 맡긴다.
            return new File(path).toURI().toString();
        }
    }

    /**
     * Returns the length of the scheme at the start of a text (without the colon), or 0 when the text has none. A name followed by a
     * port number (<code>localhost:8080</code>, <code>example.com:8080/path</code>) is not a scheme.
     * <p>
     * 글 앞의 스킴 길이(콜론 제외)를 반환한다. 스킴이 없으면 0 이다. 이름 뒤에 포트 번호가 오는 경우(<code>localhost:8080</code>,
     * <code>example.com:8080/path</code>)는 스킴이 아니다.
     *
     * @param text the trimmed input / 앞뒤 공백을 뗀 입력
     * @return the scheme length or 0 / 스킴 길이 또는 0
     */
    static int schemeLength(String text) {
        int colon = text.indexOf(':');
        if (colon <= 0 || !isLetter(text.charAt(0))) {
            return 0;
        }
        // 스킴은 글자, 숫자, '+', '-', '.' 로만 이루어진다.
        for (int i = 1; i < colon; i++) {
            char c = text.charAt(i);
            if (!isLetter(c) && !(c >= '0' && c <= '9') && c != '+' && c != '-' && c != '.') {
                return 0;
            }
        }
        // 콜론 뒤가 숫자로만 이어지다가 끝나거나 '/', '?', '#' 을 만나면 포트 번호이다.
        int i = colon + 1;
        while (i < text.length() && text.charAt(i) >= '0' && text.charAt(i) <= '9') {
            i++;
        }
        boolean digits = i > colon + 1;
        boolean ended = i == text.length() || text.charAt(i) == '/' || text.charAt(i) == '?' || text.charAt(i) == '#';
        return digits && ended ? 0 : colon;
    }

    /**
     * Tells whether a character is an ASCII letter.
     * <p>
     * 문자가 ASCII 글자인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for a letter / 글자이면 true
     */
    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Tells whether a text contains white space.
     * <p>
     * 글에 공백이 있는지 알려 준다.
     *
     * @param text the text / 글
     * @return true if it has white space / 공백이 있으면 true
     */
    private static boolean hasWhiteSpace(String text) {
        // 글자마다 공백인지 본다.
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a text is one word that does not look like a host: no dot, no colon, no slash, and not <code>localhost</code>.
     * <p>
     * 글이 호스트처럼 보이지 않는 한 낱말인지 알려 준다: 점, 콜론, 슬래시가 없고 <code>localhost</code> 가 아니다.
     *
     * @param text the text without white space / 공백 없는 글
     * @return true for such a word / 그런 낱말이면 true
     */
    private static boolean isSingleWord(String text) {
        return text.indexOf('.') < 0 && text.indexOf(':') < 0 && text.indexOf('/') < 0 && !"localhost".equalsIgnoreCase(text);
    }

    /**
     * Builds the search address: every <code>%s</code> of the template becomes the query encoded for a URL (UTF-8).
     * <p>
     * 검색 주소를 만든다: 서식의 모든 <code>%s</code> 가 URL 용으로 인코딩한(UTF-8) 질의가 된다.
     *
     * @param template the search address template / 검색 주소 서식
     * @param query    the text to search / 검색할 글
     * @return the address / 주소
     */
    private static String search(String template, String query) {
        String encoded;
        try {
            encoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 은 모든 JRE 에 있으므로 일어나지 않는다.
            throw new IllegalStateException(e);
        }
        // 서식에 자리 표시가 없으면 질의를 끝에 붙인다.
        if (template.indexOf("%s") < 0) {
            return template + encoded;
        }
        StringBuilder out = new StringBuilder();
        int from = 0;
        int at = template.indexOf("%s");
        // 자리 표시마다 인코딩한 질의로 바꾼다.
        while (at >= 0) {
            out.append(template, from, at).append(encoded);
            from = at + 2;
            at = template.indexOf("%s", from);
        }
        return out.append(template.substring(from)).toString();
    }
}
