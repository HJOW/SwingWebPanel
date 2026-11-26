package org.duckdns.hjow.swingwebpanel.net;

/**
 * Decides which URL schemes the component may fetch. Only <code>http</code>, <code>https</code> and <code>ftp</code> are
 * network schemes; <code>classpath://</code>, <code>blob:</code>, <code>about:blank</code> and the internal <code>about:text</code> are special
 * addresses. Everything else (<code>file</code>, <code>jar</code>, <code>mailto</code>, <code>javascript</code>, <code>data</code>,
 * <code>ws</code>, <code>wss</code>, other <code>about:</code> pages, ...) is blocked: the fetch layer must
 * refuse it without connecting anywhere. The check is applied to the first URL, every redirect target and every sub-resource.
 * <p>
 * 구성 요소가 가져올 수 있는 URL 스킴을 판정한다. 네트워크 스킴은 <code>http</code>, <code>https</code>, <code>ftp</code> 뿐이고
 * <code>classpath://</code>, <code>blob:</code>, <code>about:blank</code>, 내부용 <code>about:text</code> 는 특수 주소이다. 그 밖의 모든 스킴
 * (<code>file</code>, <code>jar</code>, <code>mailto</code>, <code>javascript</code>, <code>data</code>,
 * <code>ws</code>, <code>wss</code>, 다른 <code>about:</code> 페이지 등)은 차단하며, 가져오기 계층은 어디에도 연결하지 않고
 * 거부해야 한다. 이 검사는 최초 주소, 모든 리디렉션 대상, 모든 하위 리소스에 적용한다.
 */
public final class SchemePolicy {

    /**
     * The special blank page address.
     * <p>
     * 빈 페이지 특수 주소.
     */
    public static final String ABOUT_BLANK = "about:blank";

    /**
     * The internal address of content set with setContent.
     * <p>
     * setContent 로 채운 콘텐츠의 내부 주소.
     */
    public static final String ABOUT_TEXT = "about:text";

    /**
     * Classification of an address.
     * <p>
     * 주소의 분류.
     */
    public enum Kind {
        /**
         * An http URL.
         * <p>
         * http 주소(URL).
         */
        HTTP,
        /**
         * An https URL.
         * <p>
         * https 주소(URL).
         */
        HTTPS,
        /**
         * An ftp URL.
         * <p>
         * ftp 주소(URL).
         */
        FTP,
        /**
         * A class path resource URL.
         * <p>
         * 클래스패스 리소스 URL.
         */
        CLASSPATH,
        /** An object URL owned by a browser component. / 브라우저 컴포넌트가 보유한 객체 URL. */
        BLOB,
        /**
         * The blank page.
         * <p>
         * 빈 페이지.
         */
        ABOUT_BLANK,
        /**
         * The internal text content address.
         * <p>
         * 내부 텍스트 콘텐츠 주소.
         */
        ABOUT_TEXT,
        /**
         * Well formed but a forbidden scheme.
         * <p>
         * 형식은 올바르지만 금지된 스킴.
         */
        BLOCKED,
        /**
         * Not an absolute URL (null, empty, relative or unparsable).
         * <p>
         * 절대 URL 이 아님(null, 빈 문자열, 상대 참조, 해석 불가).
         */
        INVALID
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SchemePolicy() {
    }

    /**
     * Classifies an address. The text is trimmed; scheme names are case-insensitive.
     * <p>
     * 주소를 분류한다. 문자열은 앞뒤 공백을 지우며 스킴 이름은 대소문자를 구분하지 않는다.
     *
     * @param url address text / 주소 문자열
     * @return the classification / 분류
     */
    public static Kind classify(String url) {
        if (url == null || url.trim().length() == 0) {
            return Kind.INVALID;
        }
        String scheme = UrlUtil.getScheme(url);
        if (scheme == null) {
            return Kind.INVALID;
        }
        String trimmed = url.trim().toLowerCase();
        if ("about".equals(scheme)) {
            if (ABOUT_BLANK.equals(trimmed)) {
                return Kind.ABOUT_BLANK;
            }
            if (ABOUT_TEXT.equals(trimmed)) {
                return Kind.ABOUT_TEXT;
            }
            return Kind.BLOCKED;
        }
        if ("blob".equals(scheme)) {
            try {
                UrlUtil.Parts blob = UrlUtil.parse(url);
                return !blob.hasAuthority && blob.path.length() != 0 ? Kind.BLOB : Kind.INVALID;
            } catch (IllegalArgumentException invalid) {
                return Kind.INVALID;
            }
        }
        boolean network = "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme);
        if (network || UrlUtil.SCHEME_CLASSPATH.equals(scheme)) {
            UrlUtil.Parts p;
            try {
                p = UrlUtil.parse(url);
            } catch (IllegalArgumentException e) {
                return Kind.INVALID;
            }
            // 네트워크 스킴은 호스트가 있어야 한다.
            if (network && (p.host == null || p.host.length() == 0)) {
                return Kind.INVALID;
            }
            if ("http".equals(scheme)) {
                return Kind.HTTP;
            }
            if ("https".equals(scheme)) {
                return Kind.HTTPS;
            }
            if ("ftp".equals(scheme)) {
                return Kind.FTP;
            }
            return Kind.CLASSPATH;
        }
        return Kind.BLOCKED;
    }

    /**
     * Tells whether the fetch layer may load the address (http, https, ftp, classpath or a registered blob URL).
     * <p>
     * 가져오기 계층이 주소를 읽어도 되는지 알려 준다(http, https, ftp, classpath 및 등록된 blob URL).
     *
     * @param url address / 주소
     * @return true if loadable / 읽을 수 있으면 true
     */
    public static boolean isFetchAllowed(String url) {
        Kind k = classify(url);
        return k == Kind.HTTP || k == Kind.HTTPS || k == Kind.FTP || k == Kind.CLASSPATH || k == Kind.BLOB;
    }

    /**
     * Tells whether the address is a legal target of main document navigation (fetchable or about:blank).
     * <p>
     * 주소가 주 문서 이동의 올바른 대상인지 알려 준다(가져올 수 있거나 about:blank).
     *
     * @param url address / 주소
     * @return true if allowed / 허용되면 true
     */
    public static boolean isNavigationAllowed(String url) {
        return isFetchAllowed(url) || classify(url) == Kind.ABOUT_BLANK;
    }
}
