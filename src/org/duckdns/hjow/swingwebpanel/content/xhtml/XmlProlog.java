package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.util.HashMap;
import java.util.Map;

/**
 * A lightweight, never-failing scan of the beginning of an XML text: the XML declaration, the DOCTYPE declaration (name, public and system
 * identifier, internal subset and its position in the text) and the start tag of the root element (its name and namespace declarations).
 * It does not validate anything; a text that does not follow the XML syntax simply ends the scan early, and the real parser reports the
 * error. Two users: the XHTML parser rewrites the DOCTYPE with it (so that no external DTD can be named, see {@link XhtmlParser}) and the
 * content classifier looks at the root element to find out whether a <code>text/xml</code> or <code>application/xml</code> document is XHTML.
 * <p>
 * XML 텍스트의 앞부분을 훑는 가볍고 실패하지 않는 검사: XML 선언, DOCTYPE 선언(이름, 공개/시스템 식별자, 내부 서브셋과 텍스트 안의 위치), 루트 요소의
 * 시작 태그(이름과 네임스페이스 선언). 아무것도 검증하지 않는다. XML 구문을 따르지 않는 텍스트는 검사가 일찍 끝날 뿐이며 오류는 실제 파서가 보고한다.
 * 쓰는 곳은 둘이다: XHTML 파서는 이것으로 DOCTYPE 을 다시 써서 외부 DTD 를 지정할 수 없게 하고({@link XhtmlParser} 참조), 콘텐츠 분류기는 루트 요소를
 * 보고 <code>text/xml</code>, <code>application/xml</code> 문서가 XHTML 인지 판단한다.
 */
public final class XmlProlog {

    /**
     * The XHTML namespace URI.
     * <p>
     * XHTML 네임스페이스 URI.
     */
    public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    /**
     * Value of the <code>version</code> pseudo attribute of the XML declaration, or null.
     * <p>
     * XML 선언의 <code>version</code> 값. 없으면 null.
     */
    private String xmlVersion;

    /**
     * Value of the <code>encoding</code> pseudo attribute of the XML declaration, or null.
     * <p>
     * XML 선언의 <code>encoding</code> 값. 없으면 null.
     */
    private String xmlEncoding;

    /**
     * True if a DOCTYPE declaration was found and is complete.
     * <p>
     * DOCTYPE 선언을 찾았고 완전하면 true.
     */
    private boolean doctypeFound;

    /**
     * DOCTYPE name, or null.
     * <p>
     * DOCTYPE 이름. 없으면 null.
     */
    private String doctypeName;

    /**
     * DOCTYPE public identifier, or null.
     * <p>
     * DOCTYPE 공개 식별자. 없으면 null.
     */
    private String publicId;

    /**
     * DOCTYPE system identifier, or null.
     * <p>
     * DOCTYPE 시스템 식별자. 없으면 null.
     */
    private String systemId;

    /**
     * Text of the internal subset between the brackets, or null if there are no brackets.
     * <p>
     * 대괄호 사이의 내부 서브셋 텍스트. 대괄호가 없으면 null.
     */
    private String internalSubset;

    /**
     * True if a DOCTYPE declaration starts in the prolog but is not complete or not well formed (the scan stopped there).
     * <p>
     * 프롤로그에서 DOCTYPE 선언이 시작되지만 완전하지 않거나 형식이 틀리면(검사가 거기서 멈췄다) true.
     */
    private boolean doctypeMalformed;

    /**
     * Index of the <code>&lt;</code> of the DOCTYPE declaration in the scanned text, or -1.
     * <p>
     * 검사한 텍스트에서 DOCTYPE 선언의 <code>&lt;</code> 위치. 없으면 -1.
     */
    private int doctypeStart = -1;

    /**
     * Index just after the <code>&gt;</code> of the DOCTYPE declaration, or -1.
     * <p>
     * DOCTYPE 선언의 <code>&gt;</code> 바로 다음 위치. 없으면 -1.
     */
    private int doctypeEnd = -1;

    /**
     * Qualified name of the root element, or null if the scan did not reach it.
     * <p>
     * 루트 요소의 정규화된 이름. 검사가 거기까지 가지 못했으면 null.
     */
    private String rootName;

    /**
     * Namespace of the root element as far as the start tag declares it, or null.
     * <p>
     * 루트 요소의 시작 태그가 선언한 범위에서의 루트 요소 네임스페이스. 없으면 null.
     */
    private String rootNamespace;

    /**
     * Private constructor: use {@link #scan(String)}.
     * <p>
     * 비공개 생성자: {@link #scan(String)} 을 쓴다.
     */
    private XmlProlog() {
    }

    /**
     * Scans the beginning of an XML text. A leading byte order mark character (U+FEFF) is skipped. The method never throws.
     * <p>
     * XML 텍스트의 앞부분을 훑는다. 앞의 바이트 순서 표시 문자(U+FEFF)는 건너뛴다. 예외를 던지지 않는다.
     *
     * @param text the XML text, null is treated as empty / XML 텍스트(null 은 빈 문자열)
     * @return what was found / 찾아낸 것
     */
    public static XmlProlog scan(String text) {
        XmlProlog p = new XmlProlog();
        if (text == null) {
            return p;
        }
        int len = text.length();
        int pos = len > 0 && text.charAt(0) == (char) 0xFEFF ? 1 : 0;
        if (text.startsWith("<?xml", pos) && pos + 5 < len && isSpace(text.charAt(pos + 5))) {
            int end = text.indexOf("?>", pos);
            if (end < 0) {
                return p;
            }
            p.parseDeclaration(text.substring(pos + 5, end));
            pos = end + 2;
        }
        // 주석, 처리 명령, DOCTYPE 을 건너뛰며 루트 요소의 시작 태그를 찾는다.
        while (pos < len) {
            while (pos < len && isSpace(text.charAt(pos))) {
                pos++;
            }
            if (text.startsWith("<!--", pos)) {
                int end = text.indexOf("-->", pos + 4);
                if (end < 0) {
                    return p;
                }
                pos = end + 3;
            } else if (text.startsWith("<?", pos)) {
                int end = text.indexOf("?>", pos + 2);
                if (end < 0) {
                    return p;
                }
                pos = end + 2;
            } else if (text.startsWith("<!DOCTYPE", pos)) {
                int next = p.parseDoctype(text, pos);
                if (next < 0) {
                    p.doctypeMalformed = true;
                    p.doctypeStart = pos;
                    return p;
                }
                pos = next;
            } else if (pos + 1 < len && text.charAt(pos) == '<' && isNameStart(text.charAt(pos + 1))) {
                p.parseRoot(text, pos);
                return p;
            } else {
                return p;
            }
        }
        return p;
    }

    /**
     * Reads the pseudo attributes of the XML declaration.
     * <p>
     * XML 선언의 의사 속성을 읽는다.
     *
     * @param body text between <code>&lt;?xml</code> and <code>?&gt;</code> / <code>&lt;?xml</code> 과 <code>?&gt;</code> 사이의 텍스트
     */
    private void parseDeclaration(String body) {
        int i = 0;
        // 이름="값" 쌍을 차례로 읽는다.
        while (i < body.length()) {
            while (i < body.length() && isSpace(body.charAt(i))) {
                i++;
            }
            int nameStart = i;
            // 이름은 '=' 이나 공백 앞에서 끝난다.
            while (i < body.length() && body.charAt(i) != '=' && !isSpace(body.charAt(i))) {
                i++;
            }
            String name = body.substring(nameStart, i);
            // '=' 앞의 공백을 건너뛴다.
            while (i < body.length() && isSpace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length() || body.charAt(i) != '=') {
                return;
            }
            i++;
            // '=' 뒤의 공백을 건너뛴다.
            while (i < body.length() && isSpace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length() || body.charAt(i) != '"' && body.charAt(i) != '\'') {
                return;
            }
            char quote = body.charAt(i);
            int close = body.indexOf(quote, i + 1);
            if (close < 0) {
                return;
            }
            String value = body.substring(i + 1, close);
            if ("version".equals(name)) {
                xmlVersion = value;
            } else if ("encoding".equals(name)) {
                xmlEncoding = value;
            }
            i = close + 1;
        }
    }

    /**
     * Parses a DOCTYPE declaration that starts at an index.
     * <p>
     * 어떤 위치에서 시작하는 DOCTYPE 선언을 해석한다.
     *
     * @param text the text / 텍스트
     * @param at   index of <code>&lt;!DOCTYPE</code> / <code>&lt;!DOCTYPE</code> 의 위치
     * @return the index after the declaration, or -1 if it is not complete or not well formed / 선언 다음 위치. 완전하지 않거나 형식이 틀리면 -1
     */
    private int parseDoctype(String text, int at) {
        int len = text.length();
        int p = at + 9;
        int afterKeyword = p;
        // 공백이 아닌 문자가 나올 때까지 건너뛴다.
        while (p < len && isSpace(text.charAt(p))) {
            p++;
        }
        if (p == afterKeyword) {
            return -1;
        }
        int nameStart = p;
        // 이름은 이름에 쓸 수 있는 문자가 끝나는 곳에서 끝난다.
        while (p < len && isNameChar(text.charAt(p))) {
            p++;
        }
        String name = text.substring(nameStart, p);
        if (name.length() == 0) {
            return -1;
        }
        p = skipSpace(text, p);
        String pub = null;
        String sys = null;
        if (text.startsWith("SYSTEM", p)) {
            p = skipSpace(text, p + 6);
            int[] end = new int[1];
            sys = readLiteral(text, p, end);
            if (sys == null) {
                return -1;
            }
            p = skipSpace(text, end[0]);
        } else if (text.startsWith("PUBLIC", p)) {
            p = skipSpace(text, p + 6);
            int[] end = new int[1];
            pub = readLiteral(text, p, end);
            if (pub == null) {
                return -1;
            }
            p = skipSpace(text, end[0]);
            sys = readLiteral(text, p, end);
            if (sys == null) {
                return -1;
            }
            p = skipSpace(text, end[0]);
        }
        String subset = null;
        if (p < len && text.charAt(p) == '[') {
            int q = skipInternalSubset(text, p + 1);
            if (q < 0) {
                return -1;
            }
            subset = text.substring(p + 1, q);
            p = skipSpace(text, q + 1);
        }
        if (p >= len || text.charAt(p) != '>') {
            return -1;
        }
        doctypeFound = true;
        doctypeName = name;
        publicId = pub;
        systemId = sys;
        internalSubset = subset;
        doctypeStart = at;
        doctypeEnd = p + 1;
        return p + 1;
    }

    /**
     * Finds the closing bracket of an internal subset, skipping quoted strings, comments and processing instructions.
     * <p>
     * 내부 서브셋의 닫는 대괄호를 찾는다. 따옴표 문자열, 주석, 처리 명령 안은 건너뛴다.
     *
     * @param text  the text / 텍스트
     * @param start index just after the opening bracket / 여는 대괄호 바로 다음 위치
     * @return index of the closing bracket, or -1 / 닫는 대괄호의 위치. 없으면 -1
     */
    private static int skipInternalSubset(String text, int start) {
        int len = text.length();
        int p = start;
        // 닫는 ']' 가 나올 때까지 훑는다.
        while (p < len) {
            char c = text.charAt(p);
            if (c == '"' || c == '\'') {
                int close = text.indexOf(c, p + 1);
                if (close < 0) {
                    return -1;
                }
                p = close + 1;
            } else if (text.startsWith("<!--", p)) {
                int close = text.indexOf("-->", p + 4);
                if (close < 0) {
                    return -1;
                }
                p = close + 3;
            } else if (text.startsWith("<?", p)) {
                int close = text.indexOf("?>", p + 2);
                if (close < 0) {
                    return -1;
                }
                p = close + 2;
            } else if (c == ']') {
                return p;
            } else {
                p++;
            }
        }
        return -1;
    }

    /**
     * Reads a quoted literal.
     * <p>
     * 따옴표 리터럴을 읽는다.
     *
     * @param text the text / 텍스트
     * @param p    index of the opening quote / 여는 따옴표의 위치
     * @param end  receives the index after the closing quote / 닫는 따옴표 다음 위치를 받는다
     * @return the content, or null if there is no complete literal / 내용. 완전한 리터럴이 없으면 null
     */
    private static String readLiteral(String text, int p, int[] end) {
        if (p >= text.length()) {
            return null;
        }
        char q = text.charAt(p);
        if (q != '"' && q != '\'') {
            return null;
        }
        int close = text.indexOf(q, p + 1);
        if (close < 0) {
            return null;
        }
        end[0] = close + 1;
        return text.substring(p + 1, close);
    }

    /**
     * Parses the start tag of the root element: its name and the namespace declarations that decide its own namespace.
     * <p>
     * 루트 요소의 시작 태그를 해석한다: 이름과 루트 요소 자신의 네임스페이스를 정하는 네임스페이스 선언.
     *
     * @param text the text / 텍스트
     * @param at   index of the <code>&lt;</code> / <code>&lt;</code> 의 위치
     */
    private void parseRoot(String text, int at) {
        int len = text.length();
        int p = at + 1;
        int nameStart = p;
        // 이름은 공백, '>', '/' 앞에서 끝난다.
        while (p < len && !isSpace(text.charAt(p)) && text.charAt(p) != '>' && text.charAt(p) != '/') {
            p++;
        }
        rootName = text.substring(nameStart, p);
        Map<String, String> declared = new HashMap<String, String>();
        // 속성을 차례로 읽으며 xmlns 선언을 모은다. 시작 태그가 끝나거나 형식이 틀리면 멈춘다.
        while (p < len) {
            p = skipSpace(text, p);
            if (p >= len || text.charAt(p) == '>' || text.charAt(p) == '/') {
                break;
            }
            int attrStart = p;
            // 속성 이름은 '=', 공백, 태그 끝 앞에서 끝난다.
            while (p < len && text.charAt(p) != '=' && !isSpace(text.charAt(p)) && text.charAt(p) != '>' && text.charAt(p) != '/') {
                p++;
            }
            String attr = text.substring(attrStart, p);
            p = skipSpace(text, p);
            if (p >= len || text.charAt(p) != '=') {
                break;
            }
            p = skipSpace(text, p + 1);
            int[] end = new int[1];
            String value = readLiteral(text, p, end);
            if (value == null) {
                break;
            }
            p = end[0];
            if ("xmlns".equals(attr)) {
                declared.put("", value);
            } else if (attr.startsWith("xmlns:")) {
                declared.put(attr.substring(6), value);
            }
        }
        int colon = rootName.indexOf(':');
        String prefix = colon < 0 ? "" : rootName.substring(0, colon);
        rootNamespace = declared.get(prefix);
    }

    /**
     * Skips XML white space.
     * <p>
     * XML 공백을 건너뛴다.
     *
     * @param text the text / 텍스트
     * @param p    start index / 시작 위치
     * @return the index of the first non-space character / 공백이 아닌 첫 문자의 위치
     */
    private static int skipSpace(String text, int p) {
        int i = p;
        // 공백이 아닌 문자가 나올 때까지 간다.
        while (i < text.length() && isSpace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Tells whether a character is XML white space.
     * <p>
     * 문자가 XML 공백인지 알려 준다.
     *
     * @param c character / 문자
     * @return true for space, tab, line feed, carriage return / 스페이스, 탭, 줄 바꿈, 캐리지 리턴이면 true
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Tells whether a character can start an XML name (a rough test that accepts all non-ASCII characters).
     * <p>
     * 문자가 XML 이름의 시작이 될 수 있는지 알려 준다(ASCII 가 아닌 문자를 모두 받아들이는 대략적인 검사).
     *
     * @param c character / 문자
     * @return true if it can start a name / 이름의 시작이 될 수 있으면 true
     */
    private static boolean isNameStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == ':' || c >= 0x80;
    }

    /**
     * Tells whether a character can be part of an XML name (a rough test that accepts all non-ASCII characters).
     * <p>
     * 문자가 XML 이름의 일부가 될 수 있는지 알려 준다(ASCII 가 아닌 문자를 모두 받아들이는 대략적인 검사).
     *
     * @param c character / 문자
     * @return true if it can be part of a name / 이름의 일부가 될 수 있으면 true
     */
    private static boolean isNameChar(char c) {
        return isNameStart(c) || c >= '0' && c <= '9' || c == '-' || c == '.';
    }

    /**
     * Returns the XML version from the declaration, or null.
     * <p>
     * XML 선언의 버전을 반환한다. 없으면 null.
     *
     * @return the version or null / 버전 또는 null
     */
    public String getXmlVersion() {
        return xmlVersion;
    }

    /**
     * Returns the encoding name from the XML declaration, or null.
     * <p>
     * XML 선언의 인코딩 이름을 반환한다. 없으면 null.
     *
     * @return the declared encoding or null / 선언된 인코딩 또는 null
     */
    public String getXmlEncoding() {
        return xmlEncoding;
    }

    /**
     * Tells whether a complete DOCTYPE declaration was found before the root element.
     * <p>
     * 루트 요소 앞에서 완전한 DOCTYPE 선언을 찾았는지 알려 준다.
     *
     * @return true if found / 찾았으면 true
     */
    public boolean hasDoctype() {
        return doctypeFound;
    }

    /**
     * Tells whether a DOCTYPE declaration starts in the prolog but is incomplete or malformed; {@link #getDoctypeStart()} then is the
     * index where it starts.
     * <p>
     * 프롤로그에서 DOCTYPE 선언이 시작되지만 불완전하거나 형식이 틀렸는지 알려 준다. 이때 {@link #getDoctypeStart()} 가 시작 위치이다.
     *
     * @return true if malformed / 틀렸으면 true
     */
    public boolean isDoctypeMalformed() {
        return doctypeMalformed;
    }

    /**
     * Returns the DOCTYPE name, or null.
     * <p>
     * DOCTYPE 이름을 반환한다. 없으면 null.
     *
     * @return the name or null / 이름 또는 null
     */
    public String getDoctypeName() {
        return doctypeName;
    }

    /**
     * Returns the public identifier of the DOCTYPE, or null.
     * <p>
     * DOCTYPE 의 공개 식별자를 반환한다. 없으면 null.
     *
     * @return the public identifier or null / 공개 식별자 또는 null
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Returns the system identifier of the DOCTYPE, or null.
     * <p>
     * DOCTYPE 의 시스템 식별자를 반환한다. 없으면 null.
     *
     * @return the system identifier or null / 시스템 식별자 또는 null
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the text of the internal subset, or null if the DOCTYPE has no brackets.
     * <p>
     * 내부 서브셋의 텍스트를 반환한다. DOCTYPE 에 대괄호가 없으면 null.
     *
     * @return the internal subset or null / 내부 서브셋 또는 null
     */
    public String getInternalSubset() {
        return internalSubset;
    }

    /**
     * Returns the index of the DOCTYPE declaration in the scanned text, or -1.
     * <p>
     * 검사한 텍스트에서 DOCTYPE 선언의 시작 위치를 반환한다. 없으면 -1.
     *
     * @return start index or -1 / 시작 위치 또는 -1
     */
    public int getDoctypeStart() {
        return doctypeStart;
    }

    /**
     * Returns the index just after the DOCTYPE declaration, or -1.
     * <p>
     * DOCTYPE 선언 바로 다음 위치를 반환한다. 없으면 -1.
     *
     * @return end index or -1 / 끝 위치 또는 -1
     */
    public int getDoctypeEnd() {
        return doctypeEnd;
    }

    /**
     * Tells whether the DOCTYPE names an external subset (a public or system identifier).
     * <p>
     * DOCTYPE 이 외부 서브셋(공개 또는 시스템 식별자)을 지정하는지 알려 준다.
     *
     * @return true if there is an external identifier / 외부 식별자가 있으면 true
     */
    public boolean hasExternalId() {
        return publicId != null || systemId != null;
    }

    /**
     * Returns the qualified name of the root element, or null if the scan did not get there.
     * <p>
     * 루트 요소의 정규화된 이름을 반환한다. 검사가 거기까지 가지 못했으면 null.
     *
     * @return the name or null / 이름 또는 null
     */
    public String getRootName() {
        return rootName;
    }

    /**
     * Returns the namespace that the start tag of the root element gives to the root element, or null.
     * <p>
     * 루트 요소의 시작 태그가 루트 요소에 주는 네임스페이스를 반환한다. 없으면 null.
     *
     * @return the namespace or null / 네임스페이스 또는 null
     */
    public String getRootNamespace() {
        return rootNamespace;
    }

    /**
     * Tells whether the root element is <code>html</code> in the XHTML namespace.
     * <p>
     * 루트 요소가 XHTML 네임스페이스의 <code>html</code> 인지 알려 준다.
     *
     * @return true for an XHTML document / XHTML 문서이면 true
     */
    public boolean isXhtmlRoot() {
        if (rootName == null || !XHTML_NAMESPACE.equals(rootNamespace)) {
            return false;
        }
        int colon = rootName.indexOf(':');
        String local = colon < 0 ? rootName : rootName.substring(colon + 1);
        return "html".equals(local);
    }
}
