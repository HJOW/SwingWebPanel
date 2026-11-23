package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Namespace URIs and the name validation rules of the DOM Standard (validate and extract, XML Name production, ASCII whitespace
 * helpers). The name checks follow the XML 1.0 (fifth edition) <code>Name</code> and <code>QName</code> productions closely enough for
 * HTML and XML documents; characters outside the Basic Multilingual Plane are accepted as name characters.
 * <p>
 * 네임스페이스 URI 와 DOM 표준의 이름 검증 규칙(validate and extract, XML Name 생성 규칙, ASCII 공백 도우미). 이름 검사는 HTML 과 XML 문서에
 * 충분하도록 XML 1.0(제5판)의 <code>Name</code>, <code>QName</code> 생성 규칙을 따르며 기본 다국어 평면 밖의 문자는 이름 문자로 받아들인다.
 */
public final class Namespaces {

    /**
     * The HTML (XHTML) namespace.
     * <p>
     * HTML(XHTML) 네임스페이스.
     */
    public static final String HTML = "http://www.w3.org/1999/xhtml";

    /**
     * The SVG namespace.
     * <p>
     * SVG 네임스페이스.
     */
    public static final String SVG = "http://www.w3.org/2000/svg";

    /**
     * The MathML namespace.
     * <p>
     * MathML 네임스페이스.
     */
    public static final String MATHML = "http://www.w3.org/1998/Math/MathML";

    /**
     * The XML namespace (prefix xml).
     * <p>
     * XML 네임스페이스(접두사 xml).
     */
    public static final String XML = "http://www.w3.org/XML/1998/namespace";

    /**
     * The XMLNS namespace (prefix xmlns).
     * <p>
     * XMLNS 네임스페이스(접두사 xmlns).
     */
    public static final String XMLNS = "http://www.w3.org/2000/xmlns/";

    /**
     * The XLink namespace.
     * <p>
     * XLink 네임스페이스.
     */
    public static final String XLINK = "http://www.w3.org/1999/xlink";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Namespaces() {
    }

    /**
     * Tells whether a character may start an XML name.
     * <p>
     * 문자가 XML 이름의 첫 글자가 될 수 있는지 알려 준다.
     *
     * @param c UTF-16 code unit / UTF-16 코드 단위
     * @return true if allowed / 허용되면 true
     */
    public static boolean isNameStart(char c) {
        return c == ':' || c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= 0xC0 && c <= 0xD6)
                || (c >= 0xD8 && c <= 0xF6) || (c >= 0xF8 && c <= 0x2FF) || (c >= 0x370 && c <= 0x37D) || (c >= 0x37F && c <= 0x1FFF)
                || (c >= 0x200C && c <= 0x200D) || (c >= 0x2070 && c <= 0x218F) || (c >= 0x2C00 && c <= 0x2FEF)
                || (c >= 0x3001 && c <= 0xD7FF) || (c >= 0xD800 && c <= 0xDFFF) || (c >= 0xF900 && c <= 0xFDCF)
                || (c >= 0xFDF0 && c <= 0xFFFD);
    }

    /**
     * Tells whether a character may appear inside an XML name.
     * <p>
     * 문자가 XML 이름 안에 올 수 있는지 알려 준다.
     *
     * @param c UTF-16 code unit / UTF-16 코드 단위
     * @return true if allowed / 허용되면 true
     */
    public static boolean isNameChar(char c) {
        return isNameStart(c) || c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 0xB7 || (c >= 0x300 && c <= 0x36F)
                || (c >= 0x203F && c <= 0x2040);
    }

    /**
     * Tells whether a string is a valid XML <code>Name</code> (colons allowed).
     * <p>
     * 문자열이 올바른 XML <code>Name</code> 인지 알려 준다(콜론 허용).
     *
     * @param s candidate name / 후보 이름
     * @return true if valid / 올바르면 true
     */
    public static boolean isValidName(String s) {
        if (s == null || s.length() == 0 || !isNameStart(s.charAt(0))) {
            return false;
        }
        // 첫 글자 뒤의 모든 글자가 이름 글자인지 확인한다.
        for (int i = 1; i < s.length(); i++) {
            if (!isNameChar(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a string is a valid <code>QName</code>: a Name with at most one colon that is neither first nor last, and with
     * non-colon name characters on both sides.
     * <p>
     * 문자열이 올바른 <code>QName</code> 인지 알려 준다: 콜론이 많아야 하나이며 처음이나 끝이 아니고 양쪽이 콜론 없는 이름인 Name.
     *
     * @param s candidate / 후보
     * @return true if valid / 올바르면 true
     */
    public static boolean isValidQName(String s) {
        if (!isValidName(s)) {
            return false;
        }
        int colon = s.indexOf(':');
        if (colon < 0) {
            return true;
        }
        return colon > 0 && colon < s.length() - 1 && s.indexOf(':', colon + 1) < 0 && isNameStart(s.charAt(colon + 1)) && s.charAt(colon + 1) != ':';
    }

    /**
     * Result of {@link #validateAndExtract(String, String)}.
     * <p>
     * {@link #validateAndExtract(String, String)} 의 결과.
     */
    public static final class QualifiedName {
        /**
         * Namespace URI or null.
         * <p>
         * 네임스페이스 URI 또는 null.
         */
        public final String namespace;

        /**
         * Prefix or null.
         * <p>
         * 접두사 또는 null.
         */
        public final String prefix;

        /**
         * Local name.
         * <p>
         * 로컬 이름.
         */
        public final String localName;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param namespace namespace or null / 네임스페이스 또는 null
         * @param prefix    prefix or null / 접두사 또는 null
         * @param localName local name / 로컬 이름
         */
        QualifiedName(String namespace, String prefix, String localName) {
            this.namespace = namespace;
            this.prefix = prefix;
            this.localName = localName;
        }
    }

    /**
     * The "validate and extract" algorithm of the DOM Standard: checks a namespace and qualified name pair and splits the name.
     * <p>
     * DOM 표준의 "validate and extract" 알고리즘: 네임스페이스와 정규화된 이름의 쌍을 검사하고 이름을 나눈다.
     *
     * @param namespace     namespace URI, null or empty means no namespace / 네임스페이스 URI. null 이나 빈 문자열이면 없음
     * @param qualifiedName qualified name / 정규화된 이름
     * @return the extracted names / 나눈 이름
     * @throws DomException InvalidCharacterError or NamespaceError / InvalidCharacterError 또는 NamespaceError
     */
    public static QualifiedName validateAndExtract(String namespace, String qualifiedName) {
        String ns = namespace != null && namespace.length() == 0 ? null : namespace;
        if (!isValidQName(qualifiedName)) {
            throw DomException.invalidCharacter("invalid qualified name: " + qualifiedName);
        }
        String prefix = null;
        String local = qualifiedName;
        int colon = qualifiedName.indexOf(':');
        if (colon >= 0) {
            prefix = qualifiedName.substring(0, colon);
            local = qualifiedName.substring(colon + 1);
        }
        if (prefix != null && ns == null) {
            throw DomException.namespace("a prefix requires a namespace: " + qualifiedName);
        }
        if ("xml".equals(prefix) && !XML.equals(ns)) {
            throw DomException.namespace("prefix xml requires the XML namespace");
        }
        if (("xmlns".equals(qualifiedName) || "xmlns".equals(prefix)) && !XMLNS.equals(ns)) {
            throw DomException.namespace("xmlns requires the XMLNS namespace");
        }
        if (XMLNS.equals(ns) && !"xmlns".equals(qualifiedName) && !"xmlns".equals(prefix)) {
            throw DomException.namespace("the XMLNS namespace requires the name xmlns or the prefix xmlns");
        }
        return new QualifiedName(ns, prefix, local);
    }

    /**
     * Tells whether a character is ASCII whitespace (tab, LF, FF, CR, space).
     * <p>
     * 문자가 ASCII 공백(탭, LF, FF, CR, 스페이스)인지 알려 준다.
     *
     * @param c character / 문자
     * @return true for ASCII whitespace / ASCII 공백이면 true
     */
    public static boolean isAsciiWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Converts ASCII letters of a string to lower case (other characters are untouched, locale independent).
     * <p>
     * 문자열의 ASCII 영문자를 소문자로 바꾼다(다른 문자는 그대로, 로케일과 무관).
     *
     * @param s string / 문자열
     * @return lower-cased string / 소문자로 바꾼 문자열
     */
    public static String asciiLower(String s) {
        StringBuilder sb = null;
        // 대문자 ASCII 가 있을 때만 새 문자열을 만든다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + 32));
            }
        }
        return sb == null ? s : sb.toString();
    }

    /**
     * Converts ASCII letters of a string to upper case (other characters are untouched, locale independent).
     * <p>
     * 문자열의 ASCII 영문자를 대문자로 바꾼다(다른 문자는 그대로, 로케일과 무관).
     *
     * @param s string / 문자열
     * @return upper-cased string / 대문자로 바꾼 문자열
     */
    public static String asciiUpper(String s) {
        StringBuilder sb = null;
        // 소문자 ASCII 가 있을 때만 새 문자열을 만든다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c - 32));
            }
        }
        return sb == null ? s : sb.toString();
    }
}
