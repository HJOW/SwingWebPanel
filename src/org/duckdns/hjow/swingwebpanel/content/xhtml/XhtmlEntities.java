package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.duckdns.hjow.swingwebpanel.html.NamedCharacterReferences;

/**
 * The policy for named character references such as <code>&amp;nbsp;</code> in XHTML documents (plan item P17-05).
 * <p>
 * In XML only <code>amp</code>, <code>lt</code>, <code>gt</code>, <code>quot</code> and <code>apos</code> are predefined; every other
 * name must be declared. XHTML documents get <code>&amp;nbsp;</code> and the rest from the external DTD that their DOCTYPE names
 * (<code>-//W3C//DTD XHTML 1.0 Strict//EN</code> and so on). This implementation never fetches a DTD, so it replaces that mechanism by
 * the following fixed policy, which is what browsers do as well (they ship the XHTML entity files):
 * <ul>
 * <li>if the DOCTYPE names a well-known W3C DTD of the HTML, XHTML, MathML or SVG family ({@link #isKnownDtd(String, String)}), the
 * named references of the HTML character reference table ({@link NamedCharacterReferences}, a superset of the three entity files
 * of XHTML 1.0) that occur in the document are declared as internal entities (the replacement text is written with numeric character
 * references; an entity that the document itself declares in its internal subset keeps the document's own definition);</li>
 * <li>in every other document (no DOCTYPE, an HTML5 <code>&lt;!DOCTYPE html&gt;</code>, an unknown DTD) any name other than the five
 * predefined ones is an undeclared entity, which is a well-formedness error.</li>
 * </ul>
 * <p>
 * XHTML 문서의 <code>&amp;nbsp;</code> 같은 이름 있는 문자 참조에 대한 정책(계획 P17-05).
 * <p>
 * XML 에서 미리 정의된 것은 <code>amp</code>, <code>lt</code>, <code>gt</code>, <code>quot</code>, <code>apos</code> 뿐이며 나머지 이름은 선언해야
 * 한다. XHTML 문서는 DOCTYPE 이 지정하는 외부 DTD(<code>-//W3C//DTD XHTML 1.0 Strict//EN</code> 등)에서 <code>&amp;nbsp;</code> 등을 얻는다.
 * 이 구현은 DTD 를 절대 가져오지 않으므로 그 메커니즘을 다음의 고정 정책으로 대신한다. 브라우저도 XHTML 엔티티 파일을 내장하여 같은 일을 한다.
 * <ul>
 * <li>DOCTYPE 이 HTML, XHTML, MathML, SVG 계열의 잘 알려진 W3C DTD 를 지정하면({@link #isKnownDtd(String, String)}) 문서에 나오는
 * HTML 문자 참조 표({@link NamedCharacterReferences}. XHTML 1.0 의 엔티티 파일 셋의 상위 집합)의 이름 있는 참조를 내부 엔티티로 선언한다(치환 텍스트는
 * 숫자 문자 참조로 쓴다. 문서가 내부 서브셋에서 직접 선언한 엔티티는 문서의 정의를 유지한다);</li>
 * <li>그 밖의 모든 문서(DOCTYPE 없음, HTML5 <code>&lt;!DOCTYPE html&gt;</code>, 알 수 없는 DTD)에서는 미리 정의된 다섯 개 외의 이름이 선언되지 않은
 * 엔티티이며 이는 well-formed 오류이다.</li>
 * </ul>
 */
final class XhtmlEntities {

    /**
     * Maximum length of a name that is looked for.
     * <p>
     * 찾는 이름의 최대 길이.
     */
    private static final int MAX_NAME = 32;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private XhtmlEntities() {
    }

    /**
     * Tells whether a DOCTYPE names a well-known W3C DTD whose entity files this implementation replaces: public identifiers that start
     * with <code>-//W3C//DTD XHTML</code>, <code>-//W3C//DTD HTML</code>, <code>-//W3C//DTD MathML</code> or <code>-//W3C//DTD SVG</code>
     * (compared as written, XML identifiers are case sensitive), and system identifiers on <code>www.w3.org</code> whose last path
     * segment starts with <code>xhtml</code>, <code>html</code>, <code>mathml</code> or <code>svg</code>.
     * <p>
     * DOCTYPE 이 이 구현이 엔티티 파일을 대신하는 잘 알려진 W3C DTD 를 지정하는지 알려 준다: 공개 식별자가 <code>-//W3C//DTD XHTML</code>,
     * <code>-//W3C//DTD HTML</code>, <code>-//W3C//DTD MathML</code>, <code>-//W3C//DTD SVG</code> 로 시작하는 경우(쓰인 그대로 비교한다. XML 식별자는
     * 대소문자를 구분한다), 그리고 <code>www.w3.org</code> 의 시스템 식별자 중 마지막 경로 구간이 <code>xhtml</code>, <code>html</code>,
     * <code>mathml</code>, <code>svg</code> 로 시작하는 경우.
     *
     * @param publicId public identifier or null / 공개 식별자 또는 null
     * @param systemId system identifier or null / 시스템 식별자 또는 null
     * @return true for a known DTD / 알려진 DTD 이면 true
     */
    static boolean isKnownDtd(String publicId, String systemId) {
        if (publicId != null) {
            String p = collapse(publicId);
            if (p.startsWith("-//W3C//DTD XHTML") || p.startsWith("-//W3C//DTD HTML") || p.startsWith("-//W3C//DTD MathML")
                    || p.startsWith("-//W3C//DTD SVG")) {
                return true;
            }
        }
        if (systemId != null) {
            String s = systemId.trim();
            boolean w3 = s.startsWith("http://www.w3.org/") || s.startsWith("https://www.w3.org/");
            if (w3) {
                String last = s.substring(s.lastIndexOf('/') + 1).toLowerCase();
                return last.startsWith("xhtml") || last.startsWith("html") || last.startsWith("mathml") || last.startsWith("svg");
            }
        }
        return false;
    }

    /**
     * Collapses runs of white space to one space and trims, as XML does for public identifiers.
     * <p>
     * XML 이 공개 식별자에 하듯이 연속된 공백을 하나로 줄이고 앞뒤를 자른다.
     *
     * @param s the identifier / 식별자
     * @return the normalized identifier / 정규화한 식별자
     */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean space = false;
        // 공백 문자를 만나면 하나의 공백으로 줄인다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                space = sb.length() > 0;
            } else {
                if (space) {
                    sb.append(' ');
                    space = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Builds the entity declarations for the named references that occur in a text.
     * <p>
     * 텍스트에 나오는 이름 있는 참조에 대한 엔티티 선언을 만든다.
     *
     * @param text       the document text / 문서 텍스트
     * @param ownSubset  the internal subset of the document or null; names that it declares are skipped / 문서의 내부 서브셋 또는 null. 거기서 선언한 이름은 건너뛴다
     * @return declarations like <code>&lt;!ENTITY nbsp "&amp;#xA0;"&gt;</code>, one after another, or "" / 선언들. 없으면 ""
     */
    static String declarationsFor(String text, String ownSubset) {
        Set<String> names = new TreeSet<String>();
        int n = text.length();
        int amp = text.indexOf('&');
        // 앰퍼샌드마다 뒤따르는 이름과 세미콜론을 확인하여 이름 집합에 모은다.
        while (amp >= 0) {
            int j = amp + 1;
            // 이름 글자가 이어지는 동안 읽는다(길이 상한 포함).
            while (j < n && j - amp <= MAX_NAME && isNameChar(text.charAt(j))) {
                j++;
            }
            if (j < n && j > amp + 1 && text.charAt(j) == ';' && j - amp <= MAX_NAME) {
                names.add(text.substring(amp + 1, j));
            }
            amp = text.indexOf('&', amp + 1);
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = names.iterator();
        // 모은 이름 중 미리 정의되지 않았고 표에 있으며 문서가 직접 선언하지 않은 것만 선언한다.
        while (it.hasNext()) {
            String name = it.next();
            if (isPredefined(name) || ownSubset != null && declaresEntity(ownSubset, name)) {
                continue;
            }
            String value = NamedCharacterReferences.lookup(name + ";");
            if (value == null) {
                continue;
            }
            sb.append("<!ENTITY ").append(name).append(" \"");
            int i = 0;
            // 치환 문자열의 코드 포인트마다 숫자 문자 참조로 쓴다. 앰퍼샌드와 작은 부등호는 한 번 더 이스케이프한다.
            while (i < value.length()) {
                int cp = value.codePointAt(i);
                i += Character.charCount(cp);
                if (cp == '&') {
                    sb.append("&#38;#38;");
                } else if (cp == '<') {
                    sb.append("&#38;#60;");
                } else {
                    sb.append("&#x").append(Integer.toHexString(cp)).append(';');
                }
            }
            sb.append("\">");
        }
        return sb.toString();
    }

    /**
     * Tells whether a name is one of the five entities that XML predefines.
     * <p>
     * 이름이 XML 이 미리 정의하는 다섯 엔티티 중 하나인지 알려 준다.
     *
     * @param name entity name / 엔티티 이름
     * @return true if predefined / 미리 정의되었으면 true
     */
    private static boolean isPredefined(String name) {
        return "amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "quot".equals(name) || "apos".equals(name);
    }

    /**
     * Tells whether an internal subset declares a general entity with a name (a plain text search for the declaration keyword followed by
     * the name; it may find a declaration inside a comment, which only means that the document's own spelling is respected).
     * <p>
     * 내부 서브셋이 이름의 일반 엔티티를 선언하는지 알려 준다(선언 키워드와 이름을 문자열로 찾는다. 주석 안의 선언을 찾을 수도 있는데, 그러면 문서가
     * 쓴 것을 존중하게 될 뿐이다).
     *
     * @param subset the internal subset / 내부 서브셋
     * @param name   entity name / 엔티티 이름
     * @return true if declared / 선언되었으면 true
     */
    private static boolean declaresEntity(String subset, String name) {
        int at = subset.indexOf("<!ENTITY");
        // 서브셋의 <!ENTITY 키워드마다 뒤의 이름을 비교한다.
        while (at >= 0) {
            int p = at + 8;
            // 선언 키워드 뒤의 공백을 건너뛴다.
            while (p < subset.length() && Character.isWhitespace(subset.charAt(p))) {
                p++;
            }
            if (subset.startsWith(name, p)) {
                int end = p + name.length();
                if (end >= subset.length() || !isNameChar(subset.charAt(end))) {
                    return true;
                }
            }
            at = subset.indexOf("<!ENTITY", at + 8);
        }
        return false;
    }

    /**
     * Tells whether a character can be part of a named character reference.
     * <p>
     * 문자가 이름 있는 문자 참조의 일부가 될 수 있는지 알려 준다.
     *
     * @param c character / 문자
     * @return true for letters and digits / 글자나 숫자이면 true
     */
    private static boolean isNameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }
}
