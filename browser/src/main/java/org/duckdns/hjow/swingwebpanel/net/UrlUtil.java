package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL helper implementing RFC 3986 parsing, relative reference resolution and dot-segment removal on plain strings,
 * plus the project specific <code>classpath://</code> canonical form. It never decodes percent escapes while resolving
 * (no double decoding) and never treats <code>+</code> in a path as a space; decoding is an explicit single step
 * ({@link #percentDecode(String)}) used only where a protocol needs the decoded path (FTP and classpath).
 * <p>
 * The <code>classpath://a/b.html</code> form means: everything after <code>classpath:</code> and any slashes is the resource path
 * <code>a/b.html</code> (never an authority), so <code>classpath://a/b.html</code> resolved with <code>c.css</code> is
 * <code>classpath://a/c.css</code>; <code>..</code> cannot climb above the class path root.
 * <p>
 * Use: <code>UrlUtil.resolve("http://h/a/b.html", "../c.css")</code> gives <code>http://h/c.css</code>.
 * <p>
 * RFC 3986 의 해석, 상대 참조 결합, 점 세그먼트 제거를 문자열 위에서 구현한 URL 도우미이며 프로젝트 전용 <code>classpath://</code>
 * 정규 형식도 다룬다. 결합 중에는 퍼센트 인코딩을 디코딩하지 않으며(중복 디코딩 없음) 경로의 <code>+</code> 를 공백으로
 * 보지 않는다. 디코딩은 프로토콜이 필요로 하는 곳(FTP, 클래스패스)에서만 {@link #percentDecode(String)} 로 한 번 명시적으로 수행한다.
 * <p>
 * <code>classpath://a/b.html</code> 형식은 <code>classpath:</code> 와 이어지는 슬래시 뒤의 전부가 리소스 경로 <code>a/b.html</code> 이라는
 * 뜻이다(authority 가 아님). 이 주소에 <code>c.css</code> 를 결합하면 <code>classpath://a/c.css</code> 이고, <code>..</code> 는
 * 클래스패스 루트 위로 올라갈 수 없다.
 * <p>
 * 사용: <code>UrlUtil.resolve("http://h/a/b.html", "../c.css")</code> 의 결과는 <code>http://h/c.css</code> 이다.
 */
public final class UrlUtil {

    /**
     * Scheme name of class path resources.
     * <p>
     * 클래스패스 리소스의 스킴 이름.
     */
    public static final String SCHEME_CLASSPATH = "classpath";

    /**
     * RFC 3986 appendix B style splitter with an optional scheme, authority, path, query and fragment.
     * <p>
     * 스킴, authority, 경로, 질의, 프래그먼트를 선택적으로 가진 RFC 3986 부록 B 형식의 분해 패턴.
     */
    private static final Pattern SPLIT = Pattern.compile(
            "^(?:([A-Za-z][A-Za-z0-9+.\\-]*):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?$", Pattern.DOTALL);

    /**
     * Characters (besides letters and digits) that may appear unescaped in a URL.
     * <p>
     * URL 에 이스케이프 없이 나타날 수 있는 문자(영문자와 숫자 제외).
     */
    private static final String URL_SAFE = "-._~:/?#[]@!$&'()*+,;=%";

    /**
     * Characters (besides letters and digits) left unescaped by {@link #percentEncodeComponent(String)}.
     * <p>
     * {@link #percentEncodeComponent(String)} 가 이스케이프하지 않는 문자(영문자와 숫자 제외).
     */
    private static final String UNRESERVED = "-._~";

    /**
     * Hexadecimal digits in upper case.
     * <p>
     * 대문자 16진수 숫자.
     */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * The parsed pieces of a URL reference. Instances are immutable.
     * <p>
     * URL 참조를 분해한 조각. 인스턴스는 불변이다.
     */
    public static final class Parts {
        /**
         * Scheme in lower case, or null for a relative reference.
         * <p>
         * 소문자 스킴. 상대 참조면 null.
         */
        public final String scheme;
        /**
         * True when the reference has an <code>//authority</code> part.
         * <p>
         * <code>//authority</code> 부분이 있으면 true.
         */
        public final boolean hasAuthority;
        /**
         * User info before '@' or null.
         * <p>
         * '@' 앞의 사용자 정보. 없으면 null.
         */
        public final String userInfo;
        /**
         * Host in lower case (IPv6 literals keep their brackets), or null.
         * <p>
         * 소문자 호스트(IPv6 주소는 대괄호 유지). 없으면 null.
         */
        public final String host;
        /**
         * Explicit port or -1.
         * <p>
         * 명시된 포트. 없으면 -1.
         */
        public final int port;
        /**
         * Path (never null, possibly empty); for class path URLs it always starts with '/'.
         * <p>
         * 경로(null 아님, 비어 있을 수 있음). 클래스패스 URL 은 항상 '/' 로 시작한다.
         */
        public final String path;
        /**
         * Query without '?' or null.
         * <p>
         * '?' 를 뺀 질의. 없으면 null.
         */
        public final String query;
        /**
         * Fragment without '#' or null.
         * <p>
         * '#' 를 뺀 프래그먼트. 없으면 null.
         */
        public final String fragment;

        /**
         * Creates a parts record.
         * <p>
         * 조각 기록을 만든다.
         *
         * @param scheme       scheme / 스킴
         * @param hasAuthority whether an authority exists / authority 유무
         * @param userInfo     user info / 사용자 정보
         * @param host         host / 호스트
         * @param port         port / 포트
         * @param path         path / 경로
         * @param query        query / 질의
         * @param fragment     fragment / 프래그먼트
         */
        public Parts(String scheme, boolean hasAuthority, String userInfo, String host, int port,
                String path, String query, String fragment) {
            this.scheme = scheme;
            this.hasAuthority = hasAuthority;
            this.userInfo = userInfo;
            this.host = host;
            this.port = port;
            this.path = path == null ? "" : path;
            this.query = query;
            this.fragment = fragment;
        }

        /**
         * Returns a copy with another path.
         * <p>
         * 경로만 바꾼 복사본을 돌려준다.
         *
         * @param newPath new path / 새 경로
         * @return the copy / 복사본
         */
        public Parts withPath(String newPath) {
            return new Parts(scheme, hasAuthority, userInfo, host, port, newPath, query, fragment);
        }

        /**
         * Returns a copy with another fragment.
         * <p>
         * 프래그먼트만 바꾼 복사본을 돌려준다.
         *
         * @param newFragment new fragment or null / 새 프래그먼트 또는 null
         * @return the copy / 복사본
         */
        public Parts withFragment(String newFragment) {
            return new Parts(scheme, hasAuthority, userInfo, host, port, path, query, newFragment);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private UrlUtil() {
    }

    /**
     * Returns the lower-case scheme of a URL string, or null when it has none (relative reference).
     * <p>
     * URL 문자열의 소문자 스킴을 돌려준다. 스킴이 없으면(상대 참조) null.
     *
     * @param url URL text / URL 문자열
     * @return scheme or null / 스킴 또는 null
     */
    public static String getScheme(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9+.\\-]*):").matcher(url);
        return m.find() ? m.group(1).toLowerCase() : null;
    }

    /**
     * Returns the default port of a scheme or -1.
     * <p>
     * 스킴의 기본 포트를 돌려준다. 없으면 -1.
     *
     * @param scheme scheme in lower case / 소문자 스킴
     * @return default port or -1 / 기본 포트 또는 -1
     */
    public static int defaultPort(String scheme) {
        if ("http".equals(scheme)) {
            return 80;
        }
        if ("https".equals(scheme)) {
            return 443;
        }
        if ("ftp".equals(scheme)) {
            return 21;
        }
        return -1;
    }

    /**
     * Returns the effective port: the explicit one or the scheme default.
     * <p>
     * 실효 포트를 돌려준다. 명시한 포트이거나 스킴 기본값이다.
     *
     * @param p parsed URL / 분해된 URL
     * @return port or -1 / 포트 또는 -1
     */
    public static int effectivePort(Parts p) {
        return p.port >= 0 ? p.port : defaultPort(p.scheme);
    }

    /**
     * Makes user typed text safe for parsing: trims, removes tab/CR/LF, turns '\' into '/' in the path of special
     * schemes, and percent-encodes spaces, control characters, non-ASCII characters (as UTF-8) and other characters
     * that are not allowed in URLs. A '%' that is not followed by two hex digits becomes %25. Existing valid escapes stay.
     * <p>
     * 사용자가 입력한 문자열을 해석하기 안전하게 만든다. 앞뒤 공백과 탭/CR/LF 를 지우고, 특수 스킴에서는 경로의 '\' 를 '/' 로
     * 바꾸며, 공백/제어 문자/비 ASCII 문자(UTF-8)와 URL 에 허용되지 않는 문자를 퍼센트 인코딩한다. 16진수 두 자리가 뒤따르지
     * 않는 '%' 는 %25 가 된다. 이미 올바른 이스케이프는 그대로 둔다.
     *
     * @param input raw text / 원본 문자열
     * @return a string that contains only URL characters / URL 문자만 가진 문자열
     */
    public static String encodeIllegal(String input) {
        String s = input.trim();
        StringBuilder cleaned = new StringBuilder();
        // 탭, 줄바꿈은 브라우저처럼 모두 제거한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\t' && c != '\r' && c != '\n') {
                cleaned.append(c);
            }
        }
        s = cleaned.toString();
        String scheme = getScheme(s);
        boolean special = "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme);
        StringBuilder sb = new StringBuilder();
        boolean inPath = true;
        int i = 0;
        // 코드 포인트 단위로 훑으며 허용되지 않은 문자만 UTF-8 퍼센트 인코딩한다.
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);
            if (cp == '?' || cp == '#') {
                inPath = false;
            }
            if (cp == '\\' && special && inPath) {
                sb.append('/');
            } else if (cp == '%') {
                boolean ok = i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2));
                sb.append(ok ? "%" : "%25");
            } else if (cp < 0x80 && (Character.isLetterOrDigit(cp) || URL_SAFE.indexOf(cp) >= 0)) {
                sb.append((char) cp);
            } else {
                appendUtf8Escaped(sb, new String(Character.toChars(cp)));
            }
            i += len;
        }
        return sb.toString();
    }

    /**
     * Tells whether a character is a hexadecimal digit.
     * <p>
     * 문자가 16진수 숫자인지 알려 준다.
     *
     * @param c character / 문자
     * @return true for 0-9a-fA-F / 0-9a-fA-F 이면 true
     */
    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Appends the UTF-8 bytes of text as %XX escapes.
     * <p>
     * 문자열의 UTF-8 바이트를 %XX 로 덧붙인다.
     *
     * @param sb   target / 대상
     * @param text text to escape / 이스케이프할 문자열
     */
    private static void appendUtf8Escaped(StringBuilder sb, String text) {
        byte[] bytes;
        try {
            bytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        // 바이트마다 %XX 두 자리 16진수로 바꿔 덧붙인다.
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            sb.append('%').append(HEX[b >> 4]).append(HEX[b & 15]);
        }
    }

    /**
     * Parses a URL or a relative reference after {@link #encodeIllegal(String)}.
     * <p>
     * {@link #encodeIllegal(String)} 를 적용한 뒤 URL 또는 상대 참조를 분해한다.
     *
     * @param input URL text / URL 문자열
     * @return the parts / 분해 결과
     * @throws IllegalArgumentException for null, empty text or an invalid port / null, 빈 문자열, 잘못된 포트
     */
    public static Parts parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("url is null");
        }
        String s = encodeIllegal(input);
        if (s.length() == 0) {
            throw new IllegalArgumentException("url is empty");
        }
        String scheme = getScheme(s);
        // 클래스패스 주소는 authority 없이 "classpath:" 뒤 전체를 경로로 본다.
        if (SCHEME_CLASSPATH.equals(scheme)) {
            return parseClasspath(s);
        }
        Matcher m = SPLIT.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("cannot parse url: " + input);
        }
        String sch = m.group(1) == null ? null : m.group(1).toLowerCase();
        String authority = m.group(2);
        String userInfo = null;
        String host = null;
        int port = -1;
        if (authority != null) {
            String hostPort = authority;
            int at = authority.lastIndexOf('@');
            if (at >= 0) {
                userInfo = authority.substring(0, at);
                hostPort = authority.substring(at + 1);
            }
            int colon;
            if (hostPort.startsWith("[")) {
                int close = hostPort.indexOf(']');
                if (close < 0) {
                    throw new IllegalArgumentException("bad IPv6 host in " + input);
                }
                colon = hostPort.indexOf(':', close);
            } else {
                colon = hostPort.lastIndexOf(':');
            }
            if (colon >= 0) {
                String ps = hostPort.substring(colon + 1);
                hostPort = hostPort.substring(0, colon);
                if (ps.length() > 0) {
                    port = parsePort(ps, input);
                }
            }
            host = hostPort.toLowerCase();
        }
        return new Parts(sch, authority != null, userInfo, host, port, m.group(3), m.group(4), m.group(5));
    }

    /**
     * Parses a port number.
     * <p>
     * 포트 번호를 해석한다.
     *
     * @param ps    port digits / 포트 숫자 문자열
     * @param input whole URL for messages / 메시지용 전체 URL
     * @return port 0-65535 / 0~65535 포트
     */
    private static int parsePort(String ps, String input) {
        // 숫자만 허용하고 범위를 검사한다.
        for (int i = 0; i < ps.length(); i++) {
            if (ps.charAt(i) < '0' || ps.charAt(i) > '9') {
                throw new IllegalArgumentException("bad port in " + input);
            }
        }
        if (ps.length() > 5) {
            throw new IllegalArgumentException("bad port in " + input);
        }
        int port = Integer.parseInt(ps);
        if (port > 65535) {
            throw new IllegalArgumentException("bad port in " + input);
        }
        return port;
    }

    /**
     * Parses the class path form: the resource path follows <code>classpath:</code> and any number of slashes.
     * <p>
     * 클래스패스 형식을 해석한다. 리소스 경로는 <code>classpath:</code> 와 슬래시들 뒤에 온다.
     *
     * @param s encoded text starting with the classpath scheme / 클래스패스 스킴으로 시작하는 인코딩된 문자열
     * @return parts / 분해 결과
     */
    private static Parts parseClasspath(String s) {
        String rest = s.substring(s.indexOf(':') + 1);
        String fragment = null;
        int h = rest.indexOf('#');
        if (h >= 0) {
            fragment = rest.substring(h + 1);
            rest = rest.substring(0, h);
        }
        String query = null;
        int q = rest.indexOf('?');
        if (q >= 0) {
            query = rest.substring(q + 1);
            rest = rest.substring(0, q);
        }
        int k = 0;
        // 앞쪽 슬래시는 모두 버리고 항상 '/' 하나로 시작하는 경로로 보관한다.
        while (k < rest.length() && rest.charAt(k) == '/') {
            k++;
        }
        return new Parts(SCHEME_CLASSPATH, false, null, null, -1, "/" + rest.substring(k), query, fragment);
    }

    /**
     * Converts parts back to text. Class path URLs use the canonical form <code>classpath://path</code>.
     * <p>
     * 분해 결과를 다시 문자열로 만든다. 클래스패스 URL 은 정규 형식 <code>classpath://경로</code> 를 쓴다.
     *
     * @param p parts / 분해 결과
     * @return URL text / URL 문자열
     */
    public static String toString(Parts p) {
        StringBuilder sb = new StringBuilder();
        if (SCHEME_CLASSPATH.equals(p.scheme)) {
            sb.append("classpath://");
            String path = p.path;
            sb.append(path.startsWith("/") ? path.substring(1) : path);
        } else {
            if (p.scheme != null) {
                sb.append(p.scheme).append(':');
            }
            if (p.hasAuthority) {
                sb.append("//");
                if (p.userInfo != null) {
                    sb.append(p.userInfo).append('@');
                }
                if (p.host != null) {
                    sb.append(p.host);
                }
                if (p.port >= 0) {
                    sb.append(':').append(p.port);
                }
            }
            sb.append(p.path);
        }
        if (p.query != null) {
            sb.append('?').append(p.query);
        }
        if (p.fragment != null) {
            sb.append('#').append(p.fragment);
        }
        return sb.toString();
    }

    /**
     * Normalizes an absolute URL: lower-case scheme and host, default port removed, dot segments removed,
     * empty path of a hierarchical URL turned into '/', escape hex digits upper-cased.
     * <p>
     * 절대 URL 을 정규화한다. 스킴/호스트 소문자, 기본 포트 제거, 점 세그먼트 제거, 계층 URL 의 빈 경로를 '/' 로,
     * 이스케이프의 16진수를 대문자로 바꾼다.
     *
     * @param url absolute URL / 절대 URL
     * @return normalized URL / 정규화된 URL
     * @throws IllegalArgumentException when the text is not an absolute URL / 절대 URL 이 아닐 때
     */
    public static String normalize(String url) {
        Parts p = parse(url);
        if (p.scheme == null) {
            throw new IllegalArgumentException("not an absolute url: " + url);
        }
        return toString(normalizeParts(p));
    }

    /**
     * Normalizes parts (see {@link #normalize(String)}).
     * <p>
     * 분해 결과를 정규화한다({@link #normalize(String)} 참조).
     *
     * @param p parts / 분해 결과
     * @return normalized parts / 정규화된 분해 결과
     */
    private static Parts normalizeParts(Parts p) {
        // blob 경로는 불투명하므로 내부 바이트 식별자의 경로 세그먼트를 바꾸지 않는다.
        String path = upperCaseEscapes("blob".equals(p.scheme) ? p.path : removeDotSegments(p.path));
        int port = p.port;
        if (port >= 0 && port == defaultPort(p.scheme)) {
            port = -1;
        }
        if (p.hasAuthority && path.length() == 0) {
            path = "/";
        }
        String query = p.query == null ? null : upperCaseEscapes(p.query);
        String fragment = p.fragment == null ? null : upperCaseEscapes(p.fragment);
        return new Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, port, path, query, fragment);
    }

    /**
     * Upper-cases the hex digits of every percent escape.
     * <p>
     * 모든 퍼센트 이스케이프의 16진수를 대문자로 바꾼다.
     *
     * @param s text / 문자열
     * @return converted text / 변환된 문자열
     */
    private static String upperCaseEscapes(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        // '%' 다음 두 글자만 대문자로 바꾸고 나머지는 그대로 둔다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c);
            if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                sb.append(Character.toUpperCase(s.charAt(i + 1))).append(Character.toUpperCase(s.charAt(i + 2)));
                i += 2;
            }
        }
        return sb.toString();
    }

    /**
     * Resolves a reference against a base URL following RFC 3986 section 5.2 and returns a normalized absolute URL.
     * A relative reference needs a hierarchical base (http, https, ftp, classpath).
     * <p>
     * RFC 3986 5.2 절에 따라 참조를 기준 URL 에 결합하고 정규화된 절대 URL 을 돌려준다.
     * 상대 참조에는 계층형 기준(http, https, ftp, classpath)이 필요하다.
     *
     * @param base base URL, may be null when the reference is absolute / 기준 URL. 참조가 절대면 null 가능
     * @param ref  reference text / 참조 문자열
     * @return absolute URL / 절대 URL
     * @throws IllegalArgumentException when a relative reference cannot be resolved / 상대 참조를 결합할 수 없을 때
     */
    public static String resolve(String base, String ref) {
        // 빈 참조는 기준 URL 자체(프래그먼트 제외)를 뜻한다.
        if (ref != null && ref.trim().length() == 0 && base != null) {
            return toString(normalizeParts(parse(base).withFragment(null)));
        }
        Parts r = parse(ref);
        Parts t;
        if (r.scheme != null) {
            t = r;
        } else {
            if (base == null) {
                throw new IllegalArgumentException("relative url without base: " + ref);
            }
            Parts b = parse(base);
            // blob URL은 같은 문서의 프래그먼트 참조만 기준으로 해석할 수 있다.
            if ("blob".equals(b.scheme) && !r.hasAuthority && r.path.length() == 0 && r.query == null && r.fragment != null) {
                return toString(normalizeParts(b.withFragment(r.fragment)));
            }
            if (b.scheme == null || !(b.hasAuthority || SCHEME_CLASSPATH.equals(b.scheme))) {
                throw new IllegalArgumentException("cannot resolve relative url against " + base);
            }
            if (r.hasAuthority) {
                t = new Parts(b.scheme, true, r.userInfo, r.host, r.port, r.path, r.query, r.fragment);
            } else if (r.path.length() == 0) {
                t = new Parts(b.scheme, b.hasAuthority, b.userInfo, b.host, b.port, b.path,
                        r.query != null ? r.query : b.query, r.fragment);
            } else if (r.path.startsWith("/")) {
                t = new Parts(b.scheme, b.hasAuthority, b.userInfo, b.host, b.port, r.path, r.query, r.fragment);
            } else {
                t = new Parts(b.scheme, b.hasAuthority, b.userInfo, b.host, b.port, merge(b, r.path), r.query, r.fragment);
            }
        }
        return toString(normalizeParts(t));
    }

    /**
     * Merges a relative path with the base path (RFC 3986 5.2.3).
     * <p>
     * 상대 경로를 기준 경로와 합친다(RFC 3986 5.2.3).
     *
     * @param base base parts / 기준 분해 결과
     * @param rel  relative path / 상대 경로
     * @return merged path / 합친 경로
     */
    private static String merge(Parts base, String rel) {
        if (base.hasAuthority && base.path.length() == 0) {
            return "/" + rel;
        }
        int slash = base.path.lastIndexOf('/');
        return slash < 0 ? rel : base.path.substring(0, slash + 1) + rel;
    }

    /**
     * Removes "." and ".." segments (RFC 3986 5.2.4); ".." can never go above the root.
     * <p>
     * "." 와 ".." 세그먼트를 제거한다(RFC 3986 5.2.4). ".." 는 루트 위로 올라갈 수 없다.
     *
     * @param path path / 경로
     * @return path without dot segments / 점 세그먼트가 없는 경로
     */
    public static String removeDotSegments(String path) {
        if (path.indexOf('.') < 0) {
            return path;
        }
        String input = path;
        StringBuilder out = new StringBuilder();
        // 입력의 앞부분을 규칙에 따라 하나씩 소비하며 출력 버퍼를 만든다.
        while (input.length() > 0) {
            if (input.startsWith("../")) {
                input = input.substring(3);
            } else if (input.startsWith("./")) {
                input = input.substring(2);
            } else if (input.startsWith("/./")) {
                input = input.substring(2);
            } else if (input.equals("/.")) {
                input = "/";
            } else if (input.startsWith("/../")) {
                input = input.substring(3);
                removeLastSegment(out);
            } else if (input.equals("/..")) {
                input = "/";
                removeLastSegment(out);
            } else if (input.equals(".") || input.equals("..")) {
                input = "";
            } else {
                int next = input.indexOf('/', input.startsWith("/") ? 1 : 0);
                if (next < 0) {
                    out.append(input);
                    input = "";
                } else {
                    out.append(input, 0, next);
                    input = input.substring(next);
                }
            }
        }
        return out.toString();
    }

    /**
     * Removes the last segment (and its leading '/') from a buffer.
     * <p>
     * 버퍼에서 마지막 세그먼트(앞의 '/' 포함)를 제거한다.
     *
     * @param out path buffer / 경로 버퍼
     */
    private static void removeLastSegment(StringBuilder out) {
        int idx = out.lastIndexOf("/");
        out.setLength(idx < 0 ? 0 : idx);
    }

    /**
     * Returns the URL without its fragment.
     * <p>
     * 프래그먼트를 뺀 URL 을 돌려준다.
     *
     * @param url URL text / URL 문자열
     * @return URL without '#...' / '#...' 가 없는 URL
     */
    public static String stripFragment(String url) {
        int h = url.indexOf('#');
        return h < 0 ? url : url.substring(0, h);
    }

    /**
     * Returns the fragment (text after '#') or null.
     * <p>
     * 프래그먼트('#' 뒤 문자열)를 돌려준다. 없으면 null.
     *
     * @param url URL text / URL 문자열
     * @return fragment or null / 프래그먼트 또는 null
     */
    public static String getFragment(String url) {
        int h = url.indexOf('#');
        return h < 0 ? null : url.substring(h + 1);
    }

    /**
     * Decodes %XX escapes exactly once as UTF-8 (invalid sequences become U+FFFD). '+' is left unchanged.
     * <p>
     * %XX 이스케이프를 UTF-8 로 정확히 한 번 디코딩한다(잘못된 순서는 U+FFFD). '+' 는 그대로 둔다.
     *
     * @param s encoded text / 인코딩된 문자열
     * @return decoded text / 디코딩된 문자열
     */
    public static String percentDecode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 일반 문자는 UTF-8 로 그대로, %XX 는 바이트 하나로 모은 뒤 마지막에 한꺼번에 디코딩한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                bos.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
                if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                    try {
                        b = s.substring(i, i + 2).getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                    i++;
                }
                bos.write(b, 0, b.length);
            }
        }
        return decodeUtf8Lenient(bos.toByteArray());
    }

    /**
     * Decodes UTF-8 replacing malformed input.
     * <p>
     * 잘못된 입력을 대체 문자로 바꾸며 UTF-8 을 디코딩한다.
     *
     * @param bytes bytes / 바이트
     * @return text / 문자열
     */
    private static String decodeUtf8Lenient(byte[] bytes) {
        CharsetDecoder d = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return d.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Percent-encodes every character except letters, digits and <code>-._~</code> (UTF-8 bytes). Suitable for one
     * path segment such as an FTP file name.
     * <p>
     * 영문자, 숫자, <code>-._~</code> 를 제외한 모든 문자를 퍼센트 인코딩한다(UTF-8 바이트). FTP 파일 이름 같은 경로 세그먼트 하나에 알맞다.
     *
     * @param s text / 문자열
     * @return encoded text / 인코딩된 문자열
     */
    public static String percentEncodeComponent(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        // 코드 포인트마다 안전한 ASCII 는 그대로, 나머지는 UTF-8 바이트로 인코딩한다.
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);
            if (cp < 0x80 && (Character.isLetterOrDigit(cp) || UNRESERVED.indexOf(cp) >= 0)) {
                sb.append((char) cp);
            } else {
                appendUtf8Escaped(sb, new String(Character.toChars(cp)));
            }
            i += len;
        }
        return sb.toString();
    }

    /**
     * Converts an http or https URL text to a {@link URL} object.
     * <p>
     * http 또는 https URL 문자열을 {@link URL} 객체로 바꾼다.
     *
     * @param url absolute URL / 절대 URL
     * @return URL object / URL 객체
     * @throws MalformedURLException when the JRE rejects it / JRE 가 거부할 때
     */
    public static URL toJavaUrl(String url) throws MalformedURLException {
        return new URL(url);
    }
}
