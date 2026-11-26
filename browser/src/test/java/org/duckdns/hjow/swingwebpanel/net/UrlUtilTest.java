package org.duckdns.hjow.swingwebpanel.net;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link UrlUtil} and {@link SchemePolicy} (plan items P04-01 and P04-02): RFC 3986 resolution, normalization,
 * percent-encoding rules, the classpath canonical form and scheme classification.
 * <p>
 * {@link UrlUtil} 과 {@link SchemePolicy} 시험(계획 P04-01, P04-02): RFC 3986 결합, 정규화, 퍼센트 인코딩 규칙, 클래스패스
 * 정규 형식, 스킴 분류.
 */
public class UrlUtilTest {

    /**
     * Base URL of the RFC 3986 section 5.4 examples.
     * <p>
     * RFC 3986 5.4 절 예제의 기준 URL.
     */
    private static final String BASE = "http://a/b/c/d;p?q";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public UrlUtilTest() {
    }

    /**
     * Checks one resolution result.
     * <p>
     * 결합 결과 하나를 확인한다.
     *
     * @param ref      reference / 참조
     * @param expected expected absolute URL / 기대하는 절대 URL
     */
    private static void resolves(String ref, String expected) {
        Check.equal(expected, UrlUtil.resolve(BASE, ref), "resolve(\"" + ref + "\")");
    }

    /**
     * RFC 3986 normal examples (section 5.4.1).
     * <p>
     * RFC 3986 의 일반 예제(5.4.1 절).
     */
    public void testRfc3986NormalExamples() {
        resolves("g:h", "g:h");
        resolves("g", "http://a/b/c/g");
        resolves("./g", "http://a/b/c/g");
        resolves("g/", "http://a/b/c/g/");
        resolves("/g", "http://a/g");
        resolves("//g", "http://g/");
        resolves("?y", "http://a/b/c/d;p?y");
        resolves("g?y", "http://a/b/c/g?y");
        resolves("#s", "http://a/b/c/d;p?q#s");
        resolves("g#s", "http://a/b/c/g#s");
        resolves("g?y#s", "http://a/b/c/g?y#s");
        resolves(";x", "http://a/b/c/;x");
        resolves("g;x", "http://a/b/c/g;x");
        resolves("g;x?y#s", "http://a/b/c/g;x?y#s");
        resolves("", "http://a/b/c/d;p?q");
        resolves(".", "http://a/b/c/");
        resolves("./", "http://a/b/c/");
        resolves("..", "http://a/b/");
        resolves("../", "http://a/b/");
        resolves("../g", "http://a/b/g");
        resolves("../..", "http://a/");
        resolves("../../", "http://a/");
        resolves("../../g", "http://a/g");
    }

    /**
     * RFC 3986 abnormal examples (section 5.4.2): ".." never climbs above the root.
     * <p>
     * RFC 3986 의 비정상 예제(5.4.2 절). ".." 는 루트 위로 올라가지 못한다.
     */
    public void testRfc3986AbnormalExamples() {
        resolves("../../../g", "http://a/g");
        resolves("../../../../g", "http://a/g");
        resolves("/./g", "http://a/g");
        resolves("/../g", "http://a/g");
        resolves("g.", "http://a/b/c/g.");
        resolves(".g", "http://a/b/c/.g");
        resolves("g..", "http://a/b/c/g..");
        resolves("..g", "http://a/b/c/..g");
        resolves("./../g", "http://a/b/g");
        resolves("./g/.", "http://a/b/c/g/");
        resolves("g/./h", "http://a/b/c/g/h");
        resolves("g/../h", "http://a/b/c/h");
        resolves("g;x=1/./y", "http://a/b/c/g;x=1/y");
        resolves("g;x=1/../y", "http://a/b/c/y");
        resolves("g?y/./x", "http://a/b/c/g?y/./x");
        resolves("g?y/../x", "http://a/b/c/g?y/../x");
        resolves("g#s/./x", "http://a/b/c/g#s/./x");
        resolves("http:g", "http:g");
    }

    /**
     * Percent escapes are not decoded twice and '+' stays '+' in paths.
     * <p>
     * 퍼센트 이스케이프는 두 번 디코딩하지 않으며 경로의 '+' 는 그대로 둔다.
     */
    public void testNoDoubleDecodingAndPlusStays() {
        Check.equal("http://h/c", UrlUtil.resolve("http://h/x", "a%2Fb/../c"), "an escaped slash is not a separator");
        Check.equal("http://h/a%252Fb", UrlUtil.resolve("http://h/", "a%252Fb"), "%25 stays");
        Check.equal("http://h/a+b/c+d", UrlUtil.resolve("http://h/a+b/", "c+d"), "plus is not a space");
        Check.equal("http://h/%7E", UrlUtil.normalize("http://h/%7e"), "escape digits are upper-cased only");
    }

    /**
     * User typed text with spaces, Hangul, backslashes and stray percent signs becomes a valid URL.
     * <p>
     * 공백, 한글, 역슬래시, 짝 없는 퍼센트 기호가 있는 입력이 올바른 URL 이 된다.
     */
    public void testEncodeIllegal() {
        Check.equal("http://h/%ED%95%9C%EA%B8%80%20%EA%B2%BD%EB%A1%9C?x=%EA%B0%92#%ED%94%84",
                UrlUtil.encodeIllegal("  http://h/한글 경로?x=값#프  "), "hangul and space");
        Check.equal("http://h/a/b", UrlUtil.encodeIllegal("http://h/a\\b"), "backslash in the path of http becomes slash");
        Check.equal("http://h/100%25", UrlUtil.encodeIllegal("http://h/100%"), "stray percent");
        Check.equal("http://h/%41", UrlUtil.encodeIllegal("http://h/%41"), "valid escape stays");
        Check.equal("http://h/ab", UrlUtil.encodeIllegal("http://h/a\tb"), "tab is removed");
        Check.equal("http://h/%F0%9F%98%80", UrlUtil.encodeIllegal("http://h/" + new String(Character.toChars(0x1F600))), "surrogate pair is one code point");
    }

    /**
     * Normalization of scheme, host, default port, empty path and dot segments.
     * <p>
     * 스킴, 호스트, 기본 포트, 빈 경로, 점 세그먼트의 정규화.
     */
    public void testNormalize() {
        Check.equal("http://example.com/a/c", UrlUtil.normalize("HTTP://EXAMPLE.com:80/a/./b/../c"), "mixed case and port");
        Check.equal("https://h/", UrlUtil.normalize("https://h:443"), "empty path and default https port");
        Check.equal("http://h:8080/", UrlUtil.normalize("http://h:8080"), "other port stays");
        Check.equal("ftp://h/", UrlUtil.normalize("ftp://h:21"), "default ftp port");
        Check.equal("http://[::1]:80/".replace(":80/", "/"), UrlUtil.normalize("http://[::1]:80/"), "ipv6 host keeps brackets");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Normalizes a relative reference.
             * <p>
             * 상대 참조를 정규화한다.
             */
            public void run() {
                UrlUtil.normalize("a/b");
            }
        }, "relative reference cannot be normalized");
    }

    /**
     * Invalid input is rejected.
     * <p>
     * 잘못된 입력은 거부된다.
     */
    public void testInvalidInput() {
        final String[] bad = { null, "", "   ", "http://h:99999/", "http://h:ab/", "http://[::1/" };
        // 잘못된 입력마다 IllegalArgumentException 이 나는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            final String s = bad[i];
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Parses the bad input.
                 * <p>
                 * 잘못된 입력을 해석한다.
                 */
                public void run() {
                    UrlUtil.parse(s);
                }
            }, "parse(" + s + ")");
        }
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Resolves a relative reference against a non hierarchical base.
             * <p>
             * 계층형이 아닌 기준에 상대 참조를 결합한다.
             */
            public void run() {
                UrlUtil.resolve("about:blank", "x.html");
            }
        }, "no relative resolution against about:blank");
    }

    /**
     * The classpath canonical form: text after the scheme is the resource path.
     * <p>
     * 클래스패스 정규 형식: 스킴 뒤의 문자열이 리소스 경로이다.
     */
    public void testClasspathForm() {
        UrlUtil.Parts p = UrlUtil.parse("classpath://a/b.html");
        Check.equal("/a/b.html", p.path, "path of classpath://a/b.html");
        Check.isFalse(p.hasAuthority, "no authority in classpath urls");
        Check.equal("classpath://a/b.html", UrlUtil.toString(p), "canonical form");
        Check.equal("classpath://x/y", UrlUtil.normalize("classpath:///x/y"), "extra slashes are dropped");
        Check.equal("classpath://x/y", UrlUtil.normalize("classpath:x/y"), "no slashes");
        Check.equal("classpath://a/c.css", UrlUtil.resolve("classpath://a/b.html", "c.css"), "relative resource");
        Check.equal("classpath://c", UrlUtil.resolve("classpath://a/b.html", "../../../c"), "cannot climb above the class path root");
        Check.equal("classpath://a/b.html#top", UrlUtil.resolve("classpath://a/b.html", "#top"), "fragment on classpath");
        Check.equal("http://h/x", UrlUtil.resolve("classpath://a/b.html", "http://h/x"), "absolute http from classpath page");
    }

    /**
     * Single percent decoding for FTP and classpath paths, and component encoding.
     * <p>
     * FTP/클래스패스 경로를 위한 한 번의 퍼센트 디코딩과 구성 요소 인코딩.
     */
    public void testPercentDecodeAndEncode() {
        Check.equal("한", UrlUtil.percentDecode("%ED%95%9C"), "utf-8 decode");
        Check.equal("a+b c", UrlUtil.percentDecode("a+b%20c"), "plus unchanged, %20 decoded");
        Check.equal("%zz", UrlUtil.percentDecode("%zz"), "invalid escape unchanged");
        Check.equal("%41", UrlUtil.percentDecode("%2541"), "decoded exactly once");
        Check.equal(String.valueOf((char) 0xFFFD), UrlUtil.percentDecode("%FF"), "malformed utf-8 gives the replacement character");
        Check.equal("%ED%95%9C%20a%3Ab%2F-._~", UrlUtil.percentEncodeComponent("한 a:b/-._~"), "component encoding");
    }

    /**
     * Fragment helpers and scheme/port helpers.
     * <p>
     * 프래그먼트 도우미와 스킴/포트 도우미.
     */
    public void testFragmentAndSchemeHelpers() {
        Check.equal("http://h/a", UrlUtil.stripFragment("http://h/a#x"), "strip");
        Check.equal("x", UrlUtil.getFragment("http://h/a#x"), "get");
        Check.isNull(UrlUtil.getFragment("http://h/a"), "no fragment");
        Check.equal("https", UrlUtil.getScheme("  HTTPS://h"), "scheme lower-cased");
        Check.isNull(UrlUtil.getScheme("a/b:c"), "no scheme in a relative path");
        Check.equal(443, UrlUtil.effectivePort(UrlUtil.parse("https://h/")), "https default port");
        Check.equal(8080, UrlUtil.effectivePort(UrlUtil.parse("http://h:8080/")), "explicit port");
    }

    /**
     * Scheme classification for network, class path, file, object and blocked addresses.
     * <p>
     * 네트워크·클래스패스·파일·객체·차단 주소의 스킴 분류.
     */
    public void testSchemePolicy() {
        Check.equal(SchemePolicy.Kind.HTTP, SchemePolicy.classify("http://h/"), "http");
        Check.equal(SchemePolicy.Kind.HTTPS, SchemePolicy.classify("HTTPS://h"), "https");
        Check.equal(SchemePolicy.Kind.FTP, SchemePolicy.classify("ftp://h/x"), "ftp");
        Check.equal(SchemePolicy.Kind.CLASSPATH, SchemePolicy.classify("classpath://a.html"), "classpath");
        Check.equal(SchemePolicy.Kind.FILE, SchemePolicy.classify("file:///c:/x"), "absolute file");
        Check.equal("file:///C:/folder/a%20b.txt", UrlUtil.resolve("file:/C:/folder/index.html", "a b.txt"), "file relative address");
        Check.equal("file:///C:/x", UrlUtil.resolve("file://localhost/C:/dir/p.html", "/../x"), "file drive root");
        Check.equal("file:///home/index.html", UrlUtil.normalize("file://localhost/home/./index.html"), "file local authority");
        Check.equal(SchemePolicy.Kind.BLOB, SchemePolicy.classify("blob:http://h/x"), "blob lookup is handled by its store");
        Check.equal(SchemePolicy.Kind.ABOUT_BLANK, SchemePolicy.classify("about:blank"), "about:blank");
        Check.equal(SchemePolicy.Kind.ABOUT_TEXT, SchemePolicy.classify("about:text"), "about:text");
        final String[] blocked = { "file://nonlocal.invalid/c:/x", "jar:file:/x.jar!/a", "mailto:a@b.c", "javascript:alert(1)",
            "data:text/html,x", "ws://h/", "wss://h/", "about:srcdoc", "c:\\x", "gopher://h/" };
        // 금지 스킴은 모두 BLOCKED 로 분류되어야 한다.
        for (int i = 0; i < blocked.length; i++) {
            Check.equal(SchemePolicy.Kind.BLOCKED, SchemePolicy.classify(blocked[i]), "blocked: " + blocked[i]);
            Check.isFalse(SchemePolicy.isFetchAllowed(blocked[i]), "not fetchable: " + blocked[i]);
        }
        final String[] invalid = { null, "", "  ", "foo.html", "//h/x", "http://", "http:///x", "ftp://", "blob:", "blob://h/x" };
        // 절대 URL 이 아닌 입력은 INVALID 이다.
        for (int i = 0; i < invalid.length; i++) {
            Check.equal(SchemePolicy.Kind.INVALID, SchemePolicy.classify(invalid[i]), "invalid: " + invalid[i]);
        }
        Check.isTrue(SchemePolicy.isNavigationAllowed("about:blank"), "about:blank is a legal navigation target");
        Check.isFalse(SchemePolicy.isFetchAllowed("about:blank"), "about:blank is not fetched");
        Check.isFalse(SchemePolicy.isNavigationAllowed("about:text"), "about:text is not a navigation target");
    }
}
