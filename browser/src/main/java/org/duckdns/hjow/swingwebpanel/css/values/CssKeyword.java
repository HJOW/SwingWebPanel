package org.duckdns.hjow.swingwebpanel.css.values;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * An identifier used as a keyword value (<code>auto</code>, <code>none</code>, <code>inherit</code>, a font family name written as an
 * identifier, a custom identifier ...). The name is kept in lower case for the predefined keywords; custom identifiers keep their case.
 * <p>
 * 키워드 값으로 쓰인 식별자(<code>auto</code>, <code>none</code>, <code>inherit</code>, 식별자로 쓴 글꼴 이름, 사용자 정의 식별자 등). 미리 정의된 키워드는
 * 소문자로 보관하고 사용자 정의 식별자는 대소문자를 유지한다.
 */
public final class CssKeyword extends CssValue {

    /** The keyword <code>inherit</code>. / 키워드 <code>inherit</code>. */
    public static final CssKeyword INHERIT = new CssKeyword("inherit");
    /** The keyword <code>initial</code>. / 키워드 <code>initial</code>. */
    public static final CssKeyword INITIAL = new CssKeyword("initial");
    /** The keyword <code>unset</code>. / 키워드 <code>unset</code>. */
    public static final CssKeyword UNSET = new CssKeyword("unset");
    /** The keyword <code>revert</code>. / 키워드 <code>revert</code>. */
    public static final CssKeyword REVERT = new CssKeyword("revert");
    /** The keyword <code>revert-layer</code>. / 키워드 <code>revert-layer</code>. */
    public static final CssKeyword REVERT_LAYER = new CssKeyword("revert-layer");
    /** The keyword <code>auto</code>. / 키워드 <code>auto</code>. */
    public static final CssKeyword AUTO = new CssKeyword("auto");
    /** The keyword <code>none</code>. / 키워드 <code>none</code>. */
    public static final CssKeyword NONE = new CssKeyword("none");
    /** The keyword <code>normal</code>. / 키워드 <code>normal</code>. */
    public static final CssKeyword NORMAL = new CssKeyword("normal");
    /** The keyword <code>currentcolor</code>. / 키워드 <code>currentcolor</code>. */
    public static final CssKeyword CURRENTCOLOR = new CssKeyword("currentcolor");

    /** The identifier text. / 식별자 문자열. */
    private final String name;

    /**
     * Creates a keyword whose name is used as given.
     * <p>
     * 이름을 그대로 쓰는 키워드를 만든다.
     *
     * @param name identifier text / 식별자 문자열
     */
    public CssKeyword(String name) {
        this.name = name;
    }

    /**
     * Creates a keyword with an ASCII lower-cased name; the CSS-wide keywords map to their shared instances.
     * <p>
     * 이름을 ASCII 소문자로 바꾼 키워드를 만든다. CSS 전역 키워드는 공유 인스턴스를 돌려준다.
     *
     * @param name identifier text / 식별자 문자열
     * @return the keyword / 키워드
     */
    public static CssKeyword of(String name) {
        String l = lower(name);
        if (l.equals("inherit")) {
            return INHERIT;
        }
        if (l.equals("initial")) {
            return INITIAL;
        }
        if (l.equals("unset")) {
            return UNSET;
        }
        if (l.equals("revert")) {
            return REVERT;
        }
        if (l.equals("revert-layer")) {
            return REVERT_LAYER;
        }
        if (l.equals("auto")) {
            return AUTO;
        }
        if (l.equals("none")) {
            return NONE;
        }
        if (l.equals("normal")) {
            return NORMAL;
        }
        if (l.equals("currentcolor")) {
            return CURRENTCOLOR;
        }
        return new CssKeyword(l);
    }

    /**
     * Lower-cases the ASCII letters of a string.
     * <p>
     * 문자열의 ASCII 글자를 소문자로 바꾼다.
     *
     * @param s text / 문자열
     * @return lower-cased text / 소문자로 바꾼 문자열
     */
    public static String lower(String s) {
        StringBuilder sb = null;
        // ASCII 대문자가 있을 때만 새 문자열을 만든다.
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
     * Returns the identifier text.
     * <p>
     * 식별자 문자열을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Tells whether this is one of the CSS-wide keywords (<code>inherit initial unset revert revert-layer</code>).
     * <p>
     * CSS 전역 키워드(<code>inherit initial unset revert revert-layer</code>)인지 알려 준다.
     *
     * @return true for CSS-wide keywords / CSS 전역 키워드이면 true
     */
    public boolean isCssWide() {
        return this == INHERIT || this == INITIAL || this == UNSET || this == REVERT || this == REVERT_LAYER;
    }

    /**
     * Tells whether this keyword has the given name (ASCII case-insensitive).
     * <p>
     * 이 키워드가 주어진 이름인지 알려 준다(ASCII 대소문자 무시).
     *
     * @param n name / 이름
     * @return true if equal / 같으면 true
     */
    public boolean isKeyword(String n) {
        return name.equalsIgnoreCase(n);
    }

    /**
     * Serializes the keyword as an identifier.
     * <p>
     * 키워드를 식별자로 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.ident(name);
    }

    /**
     * Compares by name.
     * <p>
     * 이름으로 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssKeyword && ((CssKeyword) o).name.equals(name);
    }

    /**
     * Hash of the name.
     * <p>
     * 이름의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return name.hashCode();
    }
}
