package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * Shared helpers of the presentational hints of HTML: the attribute parsing rules of HTML5 2014 section 2.4 (non-negative integers,
 * dimension values), the keyword matching of enumerated attributes (an ASCII case-insensitive match of the whole value, as the
 * <code>[align=left i]</code> selectors of section 10 do), the declaration emitters that the hint classes use, and the tree tests that
 * find the table of a cell or a row by the structure that the rendering rules name (<code>table &gt; tr &gt; td</code> and
 * <code>table &gt; thead|tbody|tfoot &gt; tr &gt; td</code>). Sources: https://www.w3.org/TR/2014/REC-html5-20141028/common-microsyntaxes.html
 * (section 2.4.4) and https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html (retrieved 2026-11-17).
 * <p>
 * HTML 표현 힌트의 공용 도우미: HTML5 2014 §2.4의 속성 해석 규칙(음이 아닌 정수, 차원 값), 열거형 속성의 키워드 맞춤(10장의
 * <code>[align=left i]</code> 선택자처럼 값 전체의 ASCII 대소문자 무시 일치), 힌트 클래스가 쓰는 선언 출력 도우미, 렌더링 규칙이 이름 붙인 구조
 * (<code>table &gt; tr &gt; td</code>, <code>table &gt; thead|tbody|tfoot &gt; tr &gt; td</code>)로 셀이나 행의 표를 찾는 트리 검사.
 */
final class LegacyAttributes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private LegacyAttributes() {
    }

    // ------------------------------------------------------------------------------------------ 속성 값 해석

    /**
     * Parses a "non-negative integer" (HTML5 2014 section 2.4.4.1): ASCII whitespace, an optional sign and the longest run of ASCII digits;
     * trailing text is ignored. Text without digits and a negative number other than zero are errors (negative zero is zero).
     * <p>
     * "음이 아닌 정수"를 해석한다(HTML5 2014 §2.4.4.1): ASCII 공백, 선택적 부호, 가장 긴 ASCII 숫자열이며 뒤따르는 글자는 무시한다. 숫자가 없는 글과 0 이 아닌
     * 음수는 오류이다(음의 0 은 0).
     *
     * @param raw the attribute value or null / 속성 값 또는 null
     * @return the number, or -1 for an error or a missing attribute / 숫자. 오류이거나 속성이 없으면 -1
     */
    static double nonNegativeInteger(String raw) {
        if (raw == null) {
            return -1;
        }
        int position = 0;
        // HTML 이 정한 ASCII 공백만 건너뛰며 NBSP 등은 숫자 접두사로 허용하지 않는다.
        while (position < raw.length() && Namespaces.isAsciiWhitespace(raw.charAt(position))) {
            position++;
        }
        boolean negative = position < raw.length() && raw.charAt(position) == '-';
        if (position < raw.length() && (negative || raw.charAt(position) == '+')) {
            position++;
        }
        int start = position;
        double value = 0;
        // 선행 0 도 모두 읽어 고정 자릿수 잘림으로 숫자가 달라지지 않게 한다.
        while (position < raw.length() && raw.charAt(position) >= '0' && raw.charAt(position) <= '9') {
            value = value * 10 + (raw.charAt(position++) - '0');
        }
        if (position == start || Double.isInfinite(value) || (negative && value != 0)) {
            return -1;
        }
        return value;
    }

    /**
     * Formats a non-negative whole number as a CSS pixel length.
     * <p>
     * 음이 아닌 정수를 CSS 픽셀 길이로 만든다.
     *
     * @param value the number / 숫자
     * @return for example <code>12px</code> / 예: <code>12px</code>
     */
    static String px(double value) {
        // 정수 범위 안의 값은 소수점 없이 쓴다.
        if (value < 1e15 && value == Math.floor(value)) {
            return ((long) value) + "px";
        }
        return Double.toString(value) + "px";
    }

    /**
     * Parses an attribute as a pixel length through the non-negative integer rules.
     * <p>
     * 속성을 음이 아닌 정수 규칙으로 픽셀 길이로 해석한다.
     *
     * @param raw the attribute value or null / 속성 값 또는 null
     * @return <code>Npx</code> or null on an error / <code>Npx</code>. 오류이면 null
     */
    static String pixels(String raw) {
        double v = nonNegativeInteger(raw);
        return v < 0 ? null : px(v);
    }

    /**
     * Parses a "dimension value" (HTML5 2014 section 2.4.4.4): ASCII whitespace, a non-negative number with an optional fraction and an
     * optional percent sign; anything after that is ignored. The result is a pixel length or a percentage.
     * <p>
     * "차원 값"을 해석한다(HTML5 2014 §2.4.4.4): ASCII 공백, 선택적 소수부가 있는 음이 아닌 숫자, 선택적 퍼센트 기호이며 그 뒤는 무시한다. 결과는 픽셀 길이 또는 백분율이다.
     *
     * @param s the attribute value or null / 속성 값 또는 null
     * @return <code>Npx</code> or <code>N%</code>, or null if invalid / <code>Npx</code> 또는 <code>N%</code>. 올바르지 않으면 null
     */
    static String dimension(String s) {
        if (s == null) {
            return null;
        }
        int i = 0;
        int n = s.length();
        // 앞쪽 ASCII 공백을 건너뛴다.
        while (i < n && Namespaces.isAsciiWhitespace(s.charAt(i))) {
            i++;
        }
        int start = i;
        // 정수부의 숫자를 읽는다.
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }
        if (i == start) {
            return null;
        }
        if (i < n && s.charAt(i) == '.') {
            int f = i + 1;
            // 소수부의 숫자를 읽는다.
            while (f < n && s.charAt(f) >= '0' && s.charAt(f) <= '9') {
                f++;
            }
            i = f;
        }
        String num = s.substring(start, i);
        if (num.endsWith(".")) {
            num = num.substring(0, num.length() - 1);
        }
        return i < n && s.charAt(i) == '%' ? num + "%" : num + "px";
    }

    /**
     * Tells whether a dimension value is a length (not an error and not a percentage).
     * <p>
     * 차원 값이 길이(오류도 백분율도 아님)인지 알려 준다.
     *
     * @param s the attribute value or null / 속성 값 또는 null
     * @return true for a length / 길이이면 true
     */
    static boolean isLength(String s) {
        String d = dimension(s);
        return d != null && !d.endsWith("%");
    }

    /**
     * Matches the value of an enumerated attribute against keywords as the selectors <code>[name=keyword i]</code> do: the whole value, with
     * no trimming, ASCII case-insensitive.
     * <p>
     * 열거형 속성의 값을 <code>[name=keyword i]</code> 선택자처럼 키워드와 맞춘다: 값 전체를 다듬지 않고 ASCII 대소문자를 무시하여 비교한다.
     *
     * @param e        the element / 요소
     * @param name     the attribute name / 속성 이름
     * @param keywords the lower-case keywords / 소문자 키워드
     * @return the matching keyword or null / 일치한 키워드. 없으면 null
     */
    static String keyword(Element e, String name, String... keywords) {
        String v = e.getAttribute(name);
        if (v == null) {
            return null;
        }
        String l = Namespaces.asciiLower(v);
        // 키워드를 하나씩 비교한다.
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].equals(l)) {
                return keywords[i];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ 선언 출력

    /**
     * Appends a declaration.
     * <p>
     * 선언을 덧붙인다.
     *
     * @param sb       the output / 출력
     * @param property property name / 속성 이름
     * @param value    value text / 값 문자열
     */
    static void add(StringBuilder sb, String property, String value) {
        sb.append(property).append(": ").append(value).append("; ");
    }

    /**
     * Adds a color declaration from an attribute through the legacy color rules.
     * <p>
     * 속성에서 이전 색 규칙으로 색 선언을 더한다.
     *
     * @param e    the element / 요소
     * @param attr the attribute / 속성
     * @param prop the property / 속성
     * @param sb   the output / 출력
     */
    static void color(Element e, String attr, String prop, StringBuilder sb) {
        String c = HtmlPresentationalHints.legacyColor(e.getAttribute(attr));
        if (c != null) {
            add(sb, prop, c);
        }
    }

    /**
     * Adds a dimension declaration (a length or a percentage) from an attribute.
     * <p>
     * 속성에서 차원 선언(길이 또는 백분율)을 더한다.
     *
     * @param e    the element / 요소
     * @param attr the attribute / 속성
     * @param prop the property / 속성
     * @param sb   the output / 출력
     */
    static void dimension(Element e, String attr, String prop, StringBuilder sb) {
        String d = dimension(e.getAttribute(attr));
        if (d != null) {
            add(sb, prop, d);
        }
    }

    /**
     * Adds a background image declaration from the <code>background</code> attribute: a non-empty value is resolved against the document
     * by the CSS parse environment.
     * <p>
     * <code>background</code> 속성에서 배경 이미지 선언을 더한다: 비어 있지 않은 값을 CSS 파싱 환경이 문서 기준으로 해석한다.
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    static void background(Element e, StringBuilder sb) {
        String b = e.getAttribute("background");
        if (b != null) {
            b = b.trim();
        }
        if (b != null && b.length() > 0) {
            add(sb, "background-image", "url(" + CssEscape.string(b) + ")");
        }
    }

    /**
     * Adds the <code>vertical-align</code> of a <code>valign</code> attribute (<code>top</code>, <code>middle</code>, <code>bottom</code>,
     * <code>baseline</code>).
     * <p>
     * <code>valign</code> 속성의 <code>vertical-align</code> 을 더한다(<code>top</code>, <code>middle</code>, <code>bottom</code>, <code>baseline</code>).
     *
     * @param e  the element / 요소
     * @param sb the output / 출력
     */
    static void valign(Element e, StringBuilder sb) {
        String v = keyword(e, "valign", "top", "middle", "bottom", "baseline");
        if (v != null) {
            add(sb, "vertical-align", v);
        }
    }

    /**
     * Adds the <code>text-align</code> hint of an <code>align</code> attribute.
     * <p>
     * <code>align</code> 속성의 <code>text-align</code> 힌트를 더한다.
     *
     * @param e          the element / 요소
     * @param sb         the output / 출력
     * @param middle     true if <code>middle</code> centers the text as <code>center</code> does / <code>middle</code> 이 <code>center</code> 처럼 글을 가운데 정렬하면 true
     * @param absmiddle  true if <code>absmiddle</code> centers the text (table parts) / <code>absmiddle</code> 이 글을 가운데 정렬하면(표 부품) true
     * @return the keyword that was applied, or null / 적용한 키워드. 없으면 null
     */
    static String textAlign(Element e, StringBuilder sb, boolean middle, boolean absmiddle) {
        String a = keyword(e, "align", "left", "right", "justify", "center", "middle", "absmiddle");
        if (a == null) {
            return null;
        }
        if (a.equals("left") || a.equals("right") || a.equals("justify") || a.equals("center")) {
            add(sb, "text-align", a);
            return a;
        }
        if ((a.equals("middle") && middle) || (a.equals("absmiddle") && absmiddle)) {
            add(sb, "text-align", "center");
            return a;
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ 구조 검사

    /**
     * Tells whether an element is an HTML element with the given lower-case name.
     * <p>
     * 요소가 주어진 소문자 이름의 HTML 요소인지 알려 준다.
     *
     * @param e    the element or null / 요소 또는 null
     * @param name the name / 이름
     * @return true if it is / 맞으면 true
     */
    static boolean is(Element e, String name) {
        return e != null && Namespaces.HTML.equals(e.getNamespaceURI()) && name.equals(Namespaces.asciiLower(e.getLocalName()));
    }

    /**
     * Tells whether an element is a <code>thead</code>, <code>tbody</code> or <code>tfoot</code>.
     * <p>
     * 요소가 <code>thead</code>, <code>tbody</code>, <code>tfoot</code> 중 하나인지 알려 준다.
     *
     * @param e the element or null / 요소 또는 null
     * @return true for a row group / 행 그룹이면 true
     */
    static boolean isSection(Element e) {
        return is(e, "thead") || is(e, "tbody") || is(e, "tfoot");
    }

    /**
     * Returns the table that holds a row: the parent of the row, or the parent of its row group.
     * <p>
     * 행을 담은 표를 반환한다: 행의 부모 또는 행 그룹의 부모.
     *
     * @param row the <code>tr</code> element / <code>tr</code> 요소
     * @return the table or null if the row is not in a table by structure / 표. 구조상 표 안에 있지 않으면 null
     */
    static Element tableOfRow(Element row) {
        Element p = row.getParentElement();
        if (isSection(p)) {
            p = p.getParentElement();
        }
        return is(p, "table") ? p : null;
    }

    /**
     * Returns the table that holds a cell (<code>table &gt; tr &gt; td</code> or <code>table &gt; thead|tbody|tfoot &gt; tr &gt; td</code>).
     * <p>
     * 셀을 담은 표를 반환한다(<code>table &gt; tr &gt; td</code> 또는 <code>table &gt; thead|tbody|tfoot &gt; tr &gt; td</code>).
     *
     * @param cell the <code>td</code> or <code>th</code> element / <code>td</code> 또는 <code>th</code> 요소
     * @return the table or null / 표. 없으면 null
     */
    static Element tableOfCell(Element cell) {
        Element row = cell.getParentElement();
        return is(row, "tr") ? tableOfRow(row) : null;
    }

    /**
     * Returns the table whose direct child a row group or a column group is.
     * <p>
     * 행 그룹이나 열 그룹이 직접 자식인 표를 반환한다.
     *
     * @param part the group element / 그룹 요소
     * @return the table or null / 표. 없으면 null
     */
    static Element tableOfGroup(Element part) {
        Element p = part.getParentElement();
        return is(p, "table") ? p : null;
    }
}
