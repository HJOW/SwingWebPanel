package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Hand-written recognizers for the HTML constructs of CommonMark: open and closing tags (the grammar of the "Raw HTML" section) and the
 * tag names of HTML block start condition 6. Regular expressions are deliberately not used because the Java regex engine recurses on
 * repeated groups and could overflow the stack on long input.
 * <p>
 * CommonMark 의 HTML 구성 요소를 인식하는 직접 작성한 인식기: 여는/닫는 태그("Raw HTML" 절의 문법)와 HTML 블록 시작 조건 6 의 태그 이름.
 * Java 정규식 엔진이 반복 그룹에서 재귀하여 긴 입력에서 스택이 넘칠 수 있으므로 정규식은 일부러 쓰지 않는다.
 */
final class HtmlTags {

    /** Tag names of HTML block start condition 6 (lower case). HTML 블록 시작 조건 6 의 태그 이름(소문자). */
    private static final Set<String> BLOCK_NAMES = new HashSet<String>(Arrays.asList("address", "article", "aside", "base", "basefont",
            "blockquote", "body", "caption", "center", "col", "colgroup", "dd", "details", "dialog", "dir", "div", "dl", "dt", "fieldset",
            "figcaption", "figure", "footer", "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr",
            "html", "iframe", "legend", "li", "link", "main", "menu", "menuitem", "nav", "noframes", "ol", "optgroup", "option", "p",
            "param", "search", "section", "summary", "table", "tbody", "td", "tfoot", "th", "thead", "title", "tr", "track", "ul"));

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private HtmlTags() {
    }

    /**
     * Tells whether a name is one of the block-level tag names of start condition 6.
     * <p>
     * 이름이 시작 조건 6 의 블록 수준 태그 이름인지 알려 준다.
     *
     * @param lowerName the name in lower case / 소문자 이름
     * @return true if it is in the list / 목록에 있으면 true
     */
    static boolean isBlockName(String lowerName) {
        return BLOCK_NAMES.contains(lowerName);
    }

    /**
     * Matches an open tag or a closing tag that starts at a position.
     * <p>
     * 위치에서 시작하는 여는 태그나 닫는 태그를 찾는다.
     *
     * @param s the text / 텍스트
     * @param i index of the less-than sign / 꺾쇠 여는 기호의 위치
     * @return index after the closing greater-than sign, or -1 if there is no tag / 닫는 꺾쇠 다음 위치. 태그가 아니면 -1
     */
    static int matchTag(String s, int i) {
        int n = s.length();
        int p = i + 1;
        if (p >= n) {
            return -1;
        }
        if (s.charAt(p) == '/') {
            p++;
            if (p >= n || !MdText.isAsciiLetter(s.charAt(p))) {
                return -1;
            }
            p = skipTagName(s, p);
            p = skipSpace(s, p);
            return p < n && s.charAt(p) == '>' ? p + 1 : -1;
        }
        if (!MdText.isAsciiLetter(s.charAt(p))) {
            return -1;
        }
        p = skipTagName(s, p);
        // 속성을 하나씩 읽는다. 속성 앞에는 공백이 하나 이상 있어야 한다.
        while (true) {
            int afterSpace = skipSpace(s, p);
            boolean hadSpace = afterSpace > p;
            p = afterSpace;
            if (p >= n) {
                return -1;
            }
            char c = s.charAt(p);
            if (c == '>') {
                return p + 1;
            }
            if (c == '/') {
                return p + 1 < n && s.charAt(p + 1) == '>' ? p + 2 : -1;
            }
            if (!hadSpace || !isAttrNameStart(c)) {
                return -1;
            }
            p++;
            // 속성 이름의 나머지 문자를 읽는다.
            while (p < n && isAttrNameChar(s.charAt(p))) {
                p++;
            }
            int q = skipSpace(s, p);
            if (q < n && s.charAt(q) == '=') {
                q = skipSpace(s, q + 1);
                if (q >= n) {
                    return -1;
                }
                char v = s.charAt(q);
                if (v == '"' || v == '\'') {
                    int close = s.indexOf(v, q + 1);
                    if (close < 0) {
                        return -1;
                    }
                    p = close + 1;
                } else {
                    int start = q;
                    // 따옴표 없는 값은 공백, 따옴표, =, <, >, 백틱이 나오기 전까지이다.
                    while (q < n && isUnquotedValueChar(s.charAt(q))) {
                        q++;
                    }
                    if (q == start) {
                        return -1;
                    }
                    p = q;
                }
            }
        }
    }

    /**
     * Skips an HTML tag name (letters, digits, hyphens).
     * <p>
     * HTML 태그 이름(영문자, 숫자, 하이픈)을 건너뛴다.
     *
     * @param s the text / 텍스트
     * @param p start index / 시작 위치
     * @return index after the name / 이름 다음 위치
     */
    private static int skipTagName(String s, int p) {
        int n = s.length();
        // 이름에 쓸 수 있는 문자가 이어지는 동안 건너뛴다.
        while (p < n && (MdText.isAsciiAlnum(s.charAt(p)) || s.charAt(p) == '-')) {
            p++;
        }
        return p;
    }

    /**
     * Skips HTML whitespace (including line endings).
     * <p>
     * HTML 공백(줄 끝 포함)을 건너뛴다.
     *
     * @param s the text / 텍스트
     * @param p start index / 시작 위치
     * @return index of the first non-whitespace character / 공백이 아닌 첫 문자의 위치
     */
    private static int skipSpace(String s, int p) {
        int n = s.length();
        // 공백 문자가 이어지는 동안 건너뛴다.
        while (p < n && MdText.isAsciiWhitespace(s.charAt(p))) {
            p++;
        }
        return p;
    }

    /**
     * Tells whether a character can start an attribute name.
     * <p>
     * 문자가 속성 이름의 첫 글자가 될 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for a letter, underscore or colon / 영문자, 밑줄, 콜론이면 true
     */
    private static boolean isAttrNameStart(char c) {
        return MdText.isAsciiLetter(c) || c == '_' || c == ':';
    }

    /**
     * Tells whether a character can continue an attribute name.
     * <p>
     * 문자가 속성 이름의 이어지는 글자가 될 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for a letter, digit, underscore, colon, period or hyphen / 영문자, 숫자, 밑줄, 콜론, 마침표, 하이픈이면 true
     */
    private static boolean isAttrNameChar(char c) {
        return MdText.isAsciiAlnum(c) || c == '_' || c == ':' || c == '.' || c == '-';
    }

    /**
     * Tells whether a character can be part of an unquoted attribute value.
     * <p>
     * 문자가 따옴표 없는 속성값의 일부가 될 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if allowed / 허용되면 true
     */
    private static boolean isUnquotedValueChar(char c) {
        return c > ' ' && c != '"' && c != '\'' && c != '=' && c != '<' && c != '>' && c != '`';
    }
}
