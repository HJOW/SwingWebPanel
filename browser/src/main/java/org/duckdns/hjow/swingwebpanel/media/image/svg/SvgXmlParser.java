package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A small non-validating XML parser written for SVG documents (plan item P18-04). It never opens a file or a network connection: external
 * DTDs and external entities are not loaded (an external entity reads as empty), only the general entities declared with a literal value in
 * the internal DTD subset are expanded, with a strict budget so that nested entity definitions cannot explode. It resolves namespaces,
 * decodes character references, handles CDATA sections, comments and processing instructions, and reports every well-formedness error
 * (mismatched tags, duplicate attributes, undefined entities, an unfinished document) as CORRUPT, as browsers show an error for such an
 * image. Limits: nesting depth {@link #MAX_DEPTH}, number of elements {@link #MAX_ELEMENTS}, expanded entity text {@link #MAX_ENTITY_CHARS}.
 * <p>
 * SVG 문서를 위한 작은 비검증 XML 파서(계획 P18-04). 파일이나 네트워크 연결을 열지 않는다. 외부 DTD 와 외부 엔티티는 읽지 않으며(외부 엔티티는
 * 빈 문자열), 내부 DTD 부분집합에 리터럴 값으로 선언된 일반 엔티티만 엄격한 예산 안에서 확장하므로 중첩된 엔티티 정의가 폭발하지 못한다.
 * 네임스페이스를 해석하고 문자 참조를 디코딩하며 CDATA 구역, 주석, 처리 명령을 다루고, 모든 문서 형식 오류(짝이 맞지 않는 태그, 중복 속성,
 * 정의되지 않은 엔티티, 끝나지 않은 문서)를 CORRUPT 로 보고한다(브라우저도 이런 이미지는 오류로 보여 준다). 제한: 중첩 깊이
 * {@link #MAX_DEPTH}, 요소 수 {@link #MAX_ELEMENTS}, 확장된 엔티티 문자 수 {@link #MAX_ENTITY_CHARS}.
 */
final class SvgXmlParser {

    /**
     * The SVG namespace URI.
     * <p>
     * SVG 네임스페이스 URI.
     */
    static final String SVG_NS = "http://www.w3.org/2000/svg";

    /**
     * The XLink namespace URI.
     * <p>
     * XLink 네임스페이스 URI.
     */
    static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    /**
     * The XML namespace URI (the <code>xml</code> prefix).
     * <p>
     * XML 네임스페이스 URI(<code>xml</code> 접두사).
     */
    static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    /**
     * The namespace declaration namespace (<code>xmlns</code> attributes).
     * <p>
     * 네임스페이스 선언용 네임스페이스(<code>xmlns</code> 속성).
     */
    private static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    /**
     * Maximum nesting depth of elements.
     * <p>
     * 요소의 최대 중첩 깊이.
     */
    static final int MAX_DEPTH = 256;

    /**
     * Maximum number of elements in a document.
     * <p>
     * 문서의 최대 요소 수.
     */
    static final int MAX_ELEMENTS = 250000;

    /**
     * Maximum number of characters produced by entity expansion in one document.
     * <p>
     * 한 문서에서 엔티티 확장이 만들 수 있는 최대 문자 수.
     */
    static final int MAX_ENTITY_CHARS = 1000000;

    /**
     * Maximum nesting of entity references inside entity values.
     * <p>
     * 엔티티 값 안에서 엔티티 참조의 최대 중첩.
     */
    private static final int MAX_ENTITY_DEPTH = 8;

    /**
     * The text being parsed.
     * <p>
     * 해석 중인 텍스트.
     */
    private final String s;

    /**
     * The current position in {@link #s}.
     * <p>
     * {@link #s} 안의 현재 위치.
     */
    private int p;

    /**
     * The number of elements created so far.
     * <p>
     * 지금까지 만든 요소 수.
     */
    private int elementCount;

    /**
     * The number of characters produced by entity expansion so far.
     * <p>
     * 지금까지 엔티티 확장이 만든 문자 수.
     */
    private long entityChars;

    /**
     * Declared internal general entities (name to replacement text); an external entity maps to the empty string.
     * <p>
     * 선언된 내부 일반 엔티티(이름 → 대체 텍스트). 외부 엔티티는 빈 문자열로 둔다.
     */
    private final Map<String, String> entities = new HashMap<String, String>();

    /**
     * The root element found, or null.
     * <p>
     * 찾은 루트 요소. 없으면 null.
     */
    private SvgElement root;

    /**
     * Creates a parser for a text.
     * <p>
     * 텍스트를 위한 파서를 만든다.
     *
     * @param text the document text / 문서 텍스트
     */
    private SvgXmlParser(String text) {
        this.s = text;
    }

    /**
     * Parses an XML document given as bytes: the encoding is detected from the byte order mark, the first bytes or the XML declaration.
     * <p>
     * 바이트로 주어진 XML 문서를 해석한다. 인코딩은 바이트 순서 표시, 첫 바이트, XML 선언에서 알아낸다.
     *
     * @param data the document bytes / 문서 바이트
     * @return the root element / 루트 요소
     * @throws MediaException CORRUPT for malformed XML, TOO_LARGE over a limit / 형식이 틀린 XML 은 CORRUPT, 제한 초과는 TOO_LARGE
     */
    static SvgElement parse(byte[] data) throws MediaException {
        SvgXmlParser x = new SvgXmlParser(decode(data));
        x.parseDocument();
        return x.root;
    }

    /**
     * Decodes the document bytes to text using the detected encoding (UTF-8 by default); the byte order mark is removed.
     * <p>
     * 감지한 인코딩(기본 UTF-8)으로 문서 바이트를 텍스트로 디코딩한다. 바이트 순서 표시는 제거한다.
     *
     * @param d the bytes / 바이트
     * @return the text / 텍스트
     */
    static String decode(byte[] d) {
        int off = 0;
        String cs = null;
        int n = d.length;
        int b0 = n > 0 ? d[0] & 0xFF : -1;
        int b1 = n > 1 ? d[1] & 0xFF : -1;
        int b2 = n > 2 ? d[2] & 0xFF : -1;
        int b3 = n > 3 ? d[3] & 0xFF : -1;
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            off = 3;
            cs = "UTF-8";
        } else if (b0 == 0xFE && b1 == 0xFF) {
            off = 2;
            cs = "UTF-16BE";
        } else if (b0 == 0xFF && b1 == 0xFE) {
            off = 2;
            cs = "UTF-16LE";
        } else if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 > 0) {
            // '<' 로 시작하는 BOM 없는 UTF-16 문서(상위 바이트가 0)를 알아본다.
            cs = "UTF-16BE";
        } else if (b0 == 0x3C && b1 == 0x00 && b2 > 0 && b3 == 0x00) {
            cs = "UTF-16LE";
        }
        if (cs == null) {
            cs = "UTF-8";
            // XML 선언의 encoding 값을 첫 부분(ASCII 호환)에서 찾는다.
            int limit = Math.min(n, 200);
            String head = new String(d, 0, limit, Charset.forName("ISO-8859-1"));
            if (head.startsWith("<?xml")) {
                int end = head.indexOf("?>");
                String decl = end > 0 ? head.substring(0, end) : head;
                int e = decl.indexOf("encoding");
                if (e > 0) {
                    int q = e + 8;
                    // 등호와 따옴표 앞의 공백을 건너뛴다.
                    while (q < decl.length() && (decl.charAt(q) == ' ' || decl.charAt(q) == '=')) {
                        q++;
                    }
                    if (q < decl.length() && (decl.charAt(q) == '"' || decl.charAt(q) == '\'')) {
                        char quote = decl.charAt(q);
                        int q2 = decl.indexOf(quote, q + 1);
                        if (q2 > q) {
                            cs = decl.substring(q + 1, q2).trim();
                        }
                    }
                }
            }
        }
        Charset charset;
        try {
            charset = Charset.forName(cs);
        } catch (RuntimeException e) {
            // 알 수 없는 인코딩 이름이면 UTF-8 로 읽는다.
            charset = Charset.forName("UTF-8");
        }
        return new String(d, off, d.length - off, charset);
    }

    /**
     * Creates the CORRUPT error for a well-formedness problem.
     * <p>
     * 문서 형식 문제에 대한 CORRUPT 오류를 만든다.
     *
     * @param msg what is wrong / 무엇이 잘못되었는지
     * @return the exception / 예외
     */
    private MediaException error(String msg) {
        int line = 1;
        int limit = Math.min(p, s.length());
        // 오류 위치의 줄 번호를 센다.
        for (int i = 0; i < limit; i++) {
            if (s.charAt(i) == '\n') {
                line++;
            }
        }
        return new MediaException(MediaException.Kind.CORRUPT, "invalid SVG XML at line " + line + ": " + msg);
    }

    /**
     * Parses the whole document: prolog, the root element and the trailing misc items.
     * <p>
     * 문서 전체(머리말, 루트 요소, 뒤따르는 기타 항목)를 해석한다.
     *
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseDocument() throws MediaException {
        int n = s.length();
        // 루트 요소가 나올 때까지 머리말 항목을 처리한다.
        while (p < n) {
            skipSpace();
            if (p >= n) {
                break;
            }
            if (s.startsWith("<?", p)) {
                skipUntil("?>");
            } else if (s.startsWith("<!--", p)) {
                skipUntil("-->");
            } else if (s.startsWith("<!DOCTYPE", p)) {
                parseDoctype();
            } else if (s.charAt(p) == '<') {
                if (root != null) {
                    throw error("more than one root element");
                }
                root = parseElement(null, new NsScope(null), 0);
            } else {
                throw error("text outside the root element");
            }
        }
        if (root == null) {
            throw error("no root element");
        }
    }

    /**
     * Skips white space.
     * <p>
     * 공백을 건너뛴다.
     */
    private void skipSpace() {
        // 공백 문자가 이어지는 동안 건너뛴다.
        while (p < s.length() && isSpace(s.charAt(p))) {
            p++;
        }
    }

    /**
     * Tells whether a character is XML white space.
     * <p>
     * 문자가 XML 공백인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for space, tab, CR, LF / 공백, 탭, CR, LF 이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /**
     * Moves behind the next occurrence of a terminator, or fails if there is none.
     * <p>
     * 다음 종결 문자열 뒤로 이동하며 없으면 실패한다.
     *
     * @param end the terminator / 종결 문자열
     * @throws MediaException if the terminator is missing / 종결 문자열이 없을 때
     */
    private void skipUntil(String end) throws MediaException {
        int i = s.indexOf(end, p);
        if (i < 0) {
            throw error("unterminated construct, expected " + end);
        }
        p = i + end.length();
    }

    /**
     * Parses a DOCTYPE declaration: the internal subset is scanned for entity declarations, the external identifiers are ignored.
     * <p>
     * DOCTYPE 선언을 해석한다. 내부 부분집합에서 엔티티 선언을 찾고 외부 식별자는 무시한다.
     *
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseDoctype() throws MediaException {
        p += "<!DOCTYPE".length();
        int n = s.length();
        // 내부 부분집합 '[' 또는 선언 끝 '>' 까지 훑는다(따옴표 안은 건너뜀).
        while (p < n && s.charAt(p) != '[' && s.charAt(p) != '>') {
            char c = s.charAt(p);
            if (c == '"' || c == '\'') {
                int q = s.indexOf(c, p + 1);
                if (q < 0) {
                    throw error("unterminated quote in DOCTYPE");
                }
                p = q + 1;
            } else {
                p++;
            }
        }
        if (p >= n) {
            throw error("unterminated DOCTYPE");
        }
        if (s.charAt(p) == '[') {
            p++;
            parseInternalSubset();
            skipSpace();
        }
        if (p >= n || s.charAt(p) != '>') {
            throw error("expected > at the end of DOCTYPE");
        }
        p++;
    }

    /**
     * Scans the internal DTD subset up to the closing bracket and records the general entity declarations.
     * <p>
     * 내부 DTD 부분집합을 닫는 대괄호까지 훑으며 일반 엔티티 선언을 기록한다.
     *
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseInternalSubset() throws MediaException {
        int n = s.length();
        // 닫는 ']' 가 나올 때까지 선언을 하나씩 처리한다.
        while (true) {
            skipSpace();
            if (p >= n) {
                throw error("unterminated DOCTYPE internal subset");
            }
            if (s.charAt(p) == ']') {
                p++;
                return;
            }
            if (s.startsWith("<!--", p)) {
                skipUntil("-->");
            } else if (s.startsWith("<?", p)) {
                skipUntil("?>");
            } else if (s.startsWith("<!ENTITY", p)) {
                parseEntityDecl();
            } else if (s.startsWith("<!", p)) {
                skipDeclaration();
            } else if (s.charAt(p) == '%') {
                // 매개변수 엔티티 참조는 확장하지 않는다.
                skipUntil(";");
            } else {
                throw error("unexpected content in DOCTYPE");
            }
        }
    }

    /**
     * Skips a markup declaration (ELEMENT, ATTLIST, NOTATION) up to its closing bracket, ignoring brackets in quotes.
     * <p>
     * 마크업 선언(ELEMENT, ATTLIST, NOTATION)을 닫는 꺾쇠까지 건너뛴다. 따옴표 안의 꺾쇠는 무시한다.
     *
     * @throws MediaException if the declaration is not finished / 선언이 끝나지 않았을 때
     */
    private void skipDeclaration() throws MediaException {
        int n = s.length();
        // 따옴표 밖의 첫 '>' 를 찾는다.
        while (p < n) {
            char c = s.charAt(p);
            if (c == '"' || c == '\'') {
                int q = s.indexOf(c, p + 1);
                if (q < 0) {
                    throw error("unterminated quote in declaration");
                }
                p = q + 1;
            } else if (c == '>') {
                p++;
                return;
            } else {
                p++;
            }
        }
        throw error("unterminated declaration");
    }

    /**
     * Parses an ENTITY declaration. A general entity with a literal value is recorded, an external entity is recorded as empty and a parameter
     * entity is ignored.
     * <p>
     * ENTITY 선언을 해석한다. 리터럴 값의 일반 엔티티는 기록하고, 외부 엔티티는 빈 값으로 기록하며, 매개변수 엔티티는 무시한다.
     *
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseEntityDecl() throws MediaException {
        p += "<!ENTITY".length();
        skipSpace();
        boolean parameter = false;
        if (p < s.length() && s.charAt(p) == '%') {
            parameter = true;
            p++;
            skipSpace();
        }
        int start = p;
        // 엔티티 이름의 끝을 찾는다.
        while (p < s.length() && !isSpace(s.charAt(p)) && s.charAt(p) != '>') {
            p++;
        }
        String name = s.substring(start, p);
        skipSpace();
        if (p < s.length() && (s.charAt(p) == '"' || s.charAt(p) == '\'')) {
            char q = s.charAt(p);
            int e = s.indexOf(q, p + 1);
            if (e < 0) {
                throw error("unterminated entity value");
            }
            String raw = s.substring(p + 1, e);
            p = e + 1;
            if (!parameter && name.length() > 0 && !entities.containsKey(name)) {
                entities.put(name, expandCharRefs(raw));
            }
        } else if (!parameter && name.length() > 0 && !entities.containsKey(name)) {
            // SYSTEM/PUBLIC 외부 엔티티는 읽지 않고 빈 값으로 둔다.
            entities.put(name, "");
        }
        skipDeclaration();
    }

    /**
     * Expands only the character references of an entity literal (general entity references stay for the use site).
     * <p>
     * 엔티티 리터럴의 문자 참조만 확장한다(일반 엔티티 참조는 사용하는 곳에서 확장하도록 남긴다).
     *
     * @param raw the literal text / 리터럴 텍스트
     * @return text with character references replaced / 문자 참조를 바꾼 텍스트
     * @throws MediaException for an invalid character reference / 올바르지 않은 문자 참조일 때
     */
    private String expandCharRefs(String raw) throws MediaException {
        if (raw.indexOf("&#") < 0) {
            return raw;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        // 문자 참조 '&#...;' 를 찾아 문자로 바꾼다.
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length() && raw.charAt(i + 1) == '#') {
                int semi = raw.indexOf(';', i);
                if (semi < 0) {
                    throw error("unterminated character reference");
                }
                appendCharRef(sb, raw.substring(i + 2, semi));
                i = semi + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Appends the character of a numeric character reference body (such as <code>x41</code> or <code>65</code>).
     * <p>
     * 숫자 문자 참조 본문(<code>x41</code>, <code>65</code> 등)이 가리키는 문자를 덧붙인다.
     *
     * @param sb   target / 대상
     * @param body the text between <code>&amp;#</code> and the semicolon / <code>&amp;#</code> 와 세미콜론 사이의 텍스트
     * @throws MediaException for an invalid number / 올바르지 않은 수일 때
     */
    private void appendCharRef(StringBuilder sb, String body) throws MediaException {
        int cp;
        try {
            if (body.length() > 1 && (body.charAt(0) == 'x' || body.charAt(0) == 'X')) {
                cp = Integer.parseInt(body.substring(1), 16);
            } else {
                cp = Integer.parseInt(body, 10);
            }
        } catch (NumberFormatException e) {
            throw error("invalid character reference &#" + body + ";");
        }
        if (cp <= 0 || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            throw error("character reference out of range &#" + body + ";");
        }
        sb.appendCodePoint(cp);
    }

    /**
     * The namespace prefix declarations of one element and its ancestors.
     * <p>
     * 요소 하나와 그 조상의 네임스페이스 접두사 선언.
     */
    private static final class NsScope {

        /**
         * The enclosing scope or null.
         * <p>
         * 바깥 범위. 없으면 null.
         */
        final NsScope parent;

        /**
         * The prefixes declared on this element (the empty prefix is the default namespace).
         * <p>
         * 이 요소에 선언된 접두사(빈 접두사는 기본 네임스페이스).
         */
        Map<String, String> map;

        /**
         * Creates a scope.
         * <p>
         * 범위를 만든다.
         *
         * @param parent the enclosing scope / 바깥 범위
         */
        NsScope(NsScope parent) {
            this.parent = parent;
        }

        /**
         * Finds the namespace URI of a prefix.
         * <p>
         * 접두사의 네임스페이스 URI 를 찾는다.
         *
         * @param prefix the prefix, empty for the default namespace / 접두사. 기본 네임스페이스는 빈 문자열
         * @return the URI or null if undeclared / URI. 선언이 없으면 null
         */
        String lookup(String prefix) {
            // 안쪽 범위부터 바깥으로 올라가며 찾는다.
            for (NsScope sc = this; sc != null; sc = sc.parent) {
                if (sc.map != null && sc.map.containsKey(prefix)) {
                    return sc.map.get(prefix);
                }
            }
            if ("xml".equals(prefix)) {
                return XML_NS;
            }
            return null;
        }
    }

    /**
     * Reads an XML name (letters, digits, and the usual punctuation) at the current position.
     * <p>
     * 현재 위치의 XML 이름(문자, 숫자, 일반 구두점)을 읽는다.
     *
     * @return the name / 이름
     * @throws MediaException if there is no name / 이름이 없을 때
     */
    private String readName() throws MediaException {
        int start = p;
        // 이름 문자가 이어지는 동안 읽는다.
        while (p < s.length()) {
            char c = s.charAt(p);
            if (c == '=' || c == '>' || c == '/' || c == '<' || c == '"' || c == '\'' || isSpace(c)) {
                break;
            }
            p++;
        }
        if (p == start) {
            throw error("a name was expected");
        }
        return s.substring(start, p);
    }

    /**
     * Parses an element starting at <code>&lt;</code>, including its content up to the matching end tag.
     * <p>
     * <code>&lt;</code> 에서 시작하는 요소를 짝이 되는 종료 태그까지의 내용과 함께 해석한다.
     *
     * @param parent the parent element or null / 부모 요소 또는 null
     * @param outer  the namespace scope of the parent / 부모의 네임스페이스 범위
     * @param depth  the nesting depth / 중첩 깊이
     * @return the element / 요소
     * @throws MediaException on malformed input or over a limit / 잘못된 입력이거나 제한을 넘을 때
     */
    private SvgElement parseElement(SvgElement parent, NsScope outer, int depth) throws MediaException {
        if (depth > MAX_DEPTH) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "SVG elements are nested too deeply");
        }
        if (++elementCount > MAX_ELEMENTS) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "SVG has too many elements");
        }
        p++; // '<'
        String qname = readName();
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        boolean empty = false;
        // 속성을 끝 '>' 또는 '/>' 까지 읽는다.
        while (true) {
            skipSpace();
            if (p >= s.length()) {
                throw error("unterminated start tag <" + qname);
            }
            char c = s.charAt(p);
            if (c == '>') {
                p++;
                break;
            }
            if (c == '/') {
                if (p + 1 < s.length() && s.charAt(p + 1) == '>') {
                    p += 2;
                    empty = true;
                    break;
                }
                throw error("unexpected / in tag <" + qname);
            }
            String an = readName();
            skipSpace();
            if (p >= s.length() || s.charAt(p) != '=') {
                throw error("attribute " + an + " has no value");
            }
            p++;
            skipSpace();
            if (p >= s.length() || (s.charAt(p) != '"' && s.charAt(p) != '\'')) {
                throw error("attribute " + an + " value is not quoted");
            }
            char q = s.charAt(p);
            int e = s.indexOf(q, p + 1);
            if (e < 0) {
                throw error("unterminated attribute value");
            }
            String raw = s.substring(p + 1, e);
            p = e + 1;
            if (raw.indexOf('<') >= 0) {
                throw error("< in an attribute value");
            }
            if (names.contains(an)) {
                throw error("duplicate attribute " + an);
            }
            names.add(an);
            values.add(expandAttribute(raw, 0));
        }
        NsScope scope = new NsScope(outer);
        // 네임스페이스 선언(xmlns, xmlns:prefix)을 먼저 모은다.
        for (int i = 0; i < names.size(); i++) {
            String an = names.get(i);
            if ("xmlns".equals(an)) {
                if (scope.map == null) {
                    scope.map = new HashMap<String, String>();
                }
                scope.map.put("", values.get(i));
            } else if (an.startsWith("xmlns:")) {
                if (scope.map == null) {
                    scope.map = new HashMap<String, String>();
                }
                scope.map.put(an.substring(6), values.get(i));
            }
        }
        String prefix = "";
        String local = qname;
        int colon = qname.indexOf(':');
        if (colon > 0) {
            prefix = qname.substring(0, colon);
            local = qname.substring(colon + 1);
        }
        String ns = scope.lookup(prefix);
        if (ns == null) {
            if (colon > 0) {
                throw error("undeclared namespace prefix " + prefix);
            }
            ns = "";
        }
        SvgElement el = new SvgElement(local, ns);
        el.parent = parent;
        // 일반 속성을 이름 공간에 맞는 키로 저장한다.
        for (int i = 0; i < names.size(); i++) {
            String an = names.get(i);
            if ("xmlns".equals(an) || an.startsWith("xmlns:")) {
                continue;
            }
            int ac = an.indexOf(':');
            String key;
            if (ac <= 0) {
                key = an;
            } else {
                String ap = an.substring(0, ac);
                String al = an.substring(ac + 1);
                String auri = scope.lookup(ap);
                if (auri == null) {
                    throw error("undeclared namespace prefix " + ap);
                }
                if (XLINK_NS.equals(auri)) {
                    key = "xlink:" + al;
                } else if (XML_NS.equals(auri)) {
                    key = "xml:" + al;
                } else if (SVG_NS.equals(auri)) {
                    key = al;
                } else if (XMLNS_NS.equals(auri)) {
                    continue;
                } else {
                    key = "{" + auri + "}" + al;
                }
            }
            el.attrs.put(key, values.get(i));
        }
        if (!empty) {
            parseContent(el, scope, depth, qname);
        }
        return el;
    }

    /**
     * Parses the content of an element up to its end tag and consumes the end tag.
     * <p>
     * 요소의 내용을 종료 태그까지 해석하고 종료 태그를 소비한다.
     *
     * @param el    the element receiving the content / 내용을 받는 요소
     * @param scope the namespace scope of the element / 요소의 네임스페이스 범위
     * @param depth the nesting depth of the element / 요소의 중첩 깊이
     * @param qname the qualified name used by the start tag / 시작 태그에 쓴 이름
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseContent(SvgElement el, NsScope scope, int depth, String qname) throws MediaException {
        StringBuilder text = new StringBuilder();
        int n = s.length();
        // 종료 태그를 만날 때까지 텍스트와 하위 구조를 처리한다.
        while (true) {
            if (p >= n) {
                throw error("unexpected end of document inside <" + qname + ">");
            }
            char c = s.charAt(p);
            if (c == '<') {
                if (s.startsWith("</", p)) {
                    flushText(el, text);
                    p += 2;
                    String end = readName();
                    skipSpace();
                    if (p >= n || s.charAt(p) != '>') {
                        throw error("malformed end tag </" + end);
                    }
                    p++;
                    if (!end.equals(qname)) {
                        throw error("end tag </" + end + "> does not match <" + qname + ">");
                    }
                    return;
                } else if (s.startsWith("<!--", p)) {
                    skipUntil("-->");
                } else if (s.startsWith("<![CDATA[", p)) {
                    int e = s.indexOf("]]>", p + 9);
                    if (e < 0) {
                        throw error("unterminated CDATA section");
                    }
                    text.append(s, p + 9, e);
                    p = e + 3;
                } else if (s.startsWith("<?", p)) {
                    skipUntil("?>");
                } else if (s.startsWith("<!", p)) {
                    throw error("unexpected markup declaration inside an element");
                } else {
                    flushText(el, text);
                    SvgElement child = parseElement(el, scope, depth + 1);
                    el.children.add(child);
                    el.content.add(child);
                }
            } else if (c == '&') {
                parseReference(el, scope, depth, text);
            } else {
                // 다음 '<' 또는 '&' 까지의 글자를 한꺼번에 옮긴다.
                int e = p;
                while (e < n && s.charAt(e) != '<' && s.charAt(e) != '&') {
                    e++;
                }
                text.append(s, p, e);
                p = e;
            }
        }
    }

    /**
     * Adds pending text to the content of the element.
     * <p>
     * 쌓인 텍스트를 요소의 내용에 추가한다.
     *
     * @param el   the element / 요소
     * @param text the pending text, emptied by the call / 쌓인 텍스트. 호출 후 비워진다
     */
    private static void flushText(SvgElement el, StringBuilder text) {
        if (text.length() > 0) {
            el.content.add(text.toString());
            text.setLength(0);
        }
    }

    /**
     * Handles an entity or character reference inside element content.
     * <p>
     * 요소 내용 안의 엔티티 참조나 문자 참조를 처리한다.
     *
     * @param el    the element receiving content / 내용을 받는 요소
     * @param scope the namespace scope / 네임스페이스 범위
     * @param depth the nesting depth / 중첩 깊이
     * @param text  the pending text of the element / 요소의 쌓인 텍스트
     * @throws MediaException for an undefined entity or an invalid reference / 정의되지 않은 엔티티나 잘못된 참조일 때
     */
    private void parseReference(SvgElement el, NsScope scope, int depth, StringBuilder text) throws MediaException {
        int semi = s.indexOf(';', p);
        if (semi < 0 || semi - p > 64) {
            throw error("malformed reference");
        }
        String name = s.substring(p + 1, semi);
        p = semi + 1;
        if (name.startsWith("#")) {
            appendCharRef(text, name.substring(1));
            return;
        }
        String predefined = predefined(name);
        if (predefined != null) {
            text.append(predefined);
            return;
        }
        String value = entities.get(name);
        if (value == null) {
            throw error("undefined entity &" + name + ";");
        }
        chargeEntity(value.length());
        if (value.indexOf('<') < 0) {
            // 마크업이 없는 값은 안의 엔티티까지 확장해 텍스트로 붙인다.
            text.append(expandAttribute(value, 1));
            return;
        }
        // 마크업이 든 값은 별도 파서로 요소 내용으로 해석한다.
        flushText(el, text);
        SvgXmlParser sub = new SvgXmlParser(value);
        sub.entities.putAll(entities);
        sub.entityChars = entityChars;
        sub.elementCount = elementCount;
        SvgElement holder = new SvgElement("#entity", "");
        sub.parseEntityContent(holder, scope, depth);
        entityChars = sub.entityChars;
        elementCount = sub.elementCount;
        // 대체 내용의 요소와 텍스트를 현재 요소의 내용으로 옮긴다.
        for (int i = 0; i < holder.content.size(); i++) {
            Object o = holder.content.get(i);
            if (o instanceof SvgElement) {
                SvgElement c = (SvgElement) o;
                c.parent = el;
                el.children.add(c);
            }
            el.content.add(o);
        }
    }

    /**
     * Parses the replacement text of an entity as element content (used when the text contains markup).
     * <p>
     * 엔티티의 대체 텍스트를 요소 내용으로 해석한다(텍스트에 마크업이 있을 때 쓴다).
     *
     * @param holder collects the parsed content / 해석한 내용을 받는다
     * @param scope  the namespace scope / 네임스페이스 범위
     * @param depth  the nesting depth / 중첩 깊이
     * @throws MediaException on malformed input / 잘못된 입력일 때
     */
    private void parseEntityContent(SvgElement holder, NsScope scope, int depth) throws MediaException {
        // 대체 텍스트 끝까지 종료 태그 없이 내용을 읽는다.
        StringBuilder text = new StringBuilder();
        int n = s.length();
        // 다음 종결 위치까지 문자를 읽는다.
        while (p < n) {
            char c = s.charAt(p);
            if (c == '<') {
                if (s.startsWith("<!--", p)) {
                    skipUntil("-->");
                } else if (s.startsWith("</", p) || s.startsWith("<!", p) || s.startsWith("<?", p)) {
                    throw error("unsupported markup in an entity value");
                } else {
                    flushText(holder, text);
                    SvgElement child = parseElement(holder, scope, depth + 1);
                    holder.children.add(child);
                    holder.content.add(child);
                }
            } else if (c == '&') {
                parseReference(holder, scope, depth, text);
            } else {
                text.append(c);
                p++;
            }
        }
        flushText(holder, text);
    }

    /**
     * Counts characters produced by entity expansion against the budget.
     * <p>
     * 엔티티 확장이 만든 문자 수를 예산에 반영한다.
     *
     * @param n characters added / 추가된 문자 수
     * @throws MediaException TOO_LARGE if the budget is used up / 예산을 넘으면 TOO_LARGE
     */
    private void chargeEntity(int n) throws MediaException {
        entityChars += n;
        if (entityChars > MAX_ENTITY_CHARS) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "SVG entity expansion is too large");
        }
    }

    /**
     * Returns the text of a predefined entity, or null.
     * <p>
     * 미리 정의된 엔티티의 텍스트를 반환하며 없으면 null 이다.
     *
     * @param name entity name without ampersand and semicolon / 앰퍼샌드와 세미콜론이 없는 엔티티 이름
     * @return text or null / 텍스트 또는 null
     */
    private static String predefined(String name) {
        if ("lt".equals(name)) {
            return "<";
        }
        if ("gt".equals(name)) {
            return ">";
        }
        if ("amp".equals(name)) {
            return "&";
        }
        if ("quot".equals(name)) {
            return "\"";
        }
        if ("apos".equals(name)) {
            return "'";
        }
        return null;
    }

    /**
     * Expands references in an attribute value (or an entity value) and normalizes white space characters to spaces.
     * <p>
     * 속성 값(또는 엔티티 값)의 참조를 확장하고 공백 문자를 스페이스로 정규화한다.
     *
     * @param raw   the text between the quotes / 따옴표 사이의 텍스트
     * @param level the nesting of entity values / 엔티티 값의 중첩 수준
     * @return the expanded text / 확장된 텍스트
     * @throws MediaException for an undefined entity or an invalid reference / 정의되지 않은 엔티티나 잘못된 참조일 때
     */
    private String expandAttribute(String raw, int level) throws MediaException {
        if (raw.indexOf('&') < 0 && raw.indexOf('\n') < 0 && raw.indexOf('\t') < 0 && raw.indexOf('\r') < 0) {
            return raw;
        }
        if (level > MAX_ENTITY_DEPTH) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "SVG entity references are nested too deeply");
        }
        StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        // 참조는 해석하고 줄바꿈/탭은 스페이스로 바꾸며 한 글자씩 옮긴다.
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&') {
                int semi = raw.indexOf(';', i);
                if (semi < 0) {
                    throw error("malformed reference in an attribute");
                }
                String name = raw.substring(i + 1, semi);
                i = semi + 1;
                if (name.startsWith("#")) {
                    appendCharRef(sb, name.substring(1));
                } else if (predefined(name) != null) {
                    sb.append(predefined(name));
                } else {
                    String v = entities.get(name);
                    if (v == null) {
                        throw error("undefined entity &" + name + ";");
                    }
                    chargeEntity(v.length());
                    sb.append(expandAttribute(v, level + 1));
                }
            } else if (c == '\n' || c == '\t' || c == '\r') {
                sb.append(' ');
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
