package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A declaration as produced by the parser: a property name, the component values of its value and the <code>!important</code> flag. The
 * value is not interpreted here; the property table (<code>css.props</code>) does that.
 * <p>
 * 파서가 만든 선언: 속성 이름, 값의 컴포넌트 값들, <code>!important</code> 플래그. 값은 여기서 해석하지 않으며 속성 표(<code>css.props</code>)가 해석한다.
 */
public final class SyntaxDeclaration {

    /** Property name: lower-cased unless it is a custom property. / 속성 이름: 사용자 정의 속성이 아니면 소문자. */
    private final String name;

    /** Value component values (leading and trailing whitespace removed). / 값의 컴포넌트 값(앞뒤 공백 제거). */
    private final List<ComponentValue> value;

    /** True when the declaration was marked important. / 선언에 !important 가 붙었으면 true. */
    private final boolean important;

    /**
     * Creates a declaration.
     * <p>
     * 선언을 만든다.
     *
     * @param name      property name as written / 쓰인 그대로의 속성 이름
     * @param value     value component values / 값의 컴포넌트 값
     * @param important true for !important / !important 이면 true
     */
    public SyntaxDeclaration(String name, List<ComponentValue> value, boolean important) {
        this.name = name.startsWith("--") ? name : asciiLower(name);
        this.value = Collections.unmodifiableList(new ArrayList<ComponentValue>(value));
        this.important = important;
    }

    /**
     * Lower-cases the ASCII letters of a string.
     * <p>
     * 문자열의 ASCII 글자를 소문자로 바꾼다.
     *
     * @param s text / 문자열
     * @return lower-cased text / 소문자로 바꾼 문자열
     */
    static String asciiLower(String s) {
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
     * Returns the property name.
     * <p>
     * 속성 이름을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value component values.
     * <p>
     * 값의 컴포넌트 값을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComponentValue> getValue() {
        return value;
    }

    /**
     * Tells whether the declaration is important.
     * <p>
     * 선언이 important 인지 알려 준다.
     *
     * @return true if !important / !important 이면 true
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * Tells whether this is a custom property (name starting with two hyphens).
     * <p>
     * 사용자 정의 속성(하이픈 두 개로 시작하는 이름)인지 알려 준다.
     *
     * @return true for custom properties / 사용자 정의 속성이면 true
     */
    public boolean isCustomProperty() {
        return name.startsWith("--");
    }

    /**
     * Serializes the value.
     * <p>
     * 값을 직렬화한다.
     *
     * @return value text / 값 문자열
     */
    public String getValueText() {
        return CssBlock.join(value).trim();
    }

    /**
     * Serializes the declaration.
     * <p>
     * 선언을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toString() {
        return name + ": " + getValueText() + (important ? " !important" : "");
    }
}
