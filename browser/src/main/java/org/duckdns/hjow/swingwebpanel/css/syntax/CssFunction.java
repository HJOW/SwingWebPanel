package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A function: a name and the component values between its parentheses.
 * <p>
 * 함수: 이름과 소괄호 사이의 컴포넌트 값들.
 */
public final class CssFunction extends ComponentValue {

    /** Function name (original case). / 함수 이름(원래 대소문자). */
    private final String name;

    /** Arguments as component values. / 인수 컴포넌트 값. */
    private final List<ComponentValue> arguments;

    /**
     * Creates a function.
     * <p>
     * 함수를 만든다.
     *
     * @param name      function name / 함수 이름
     * @param arguments argument component values / 인수 컴포넌트 값
     */
    public CssFunction(String name, List<ComponentValue> arguments) {
        this.name = name;
        this.arguments = Collections.unmodifiableList(new ArrayList<ComponentValue>(arguments));
    }

    /**
     * Returns the function name.
     * <p>
     * 함수 이름을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the arguments.
     * <p>
     * 인수를 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComponentValue> getArguments() {
        return arguments;
    }

    /**
     * Tells whether the function has the given name (ASCII case-insensitive).
     * <p>
     * 함수가 주어진 이름인지 알려 준다(ASCII 대소문자 무시).
     *
     * @param n name / 이름
     * @return true if it matches / 일치하면 true
     */
    public boolean isNamed(String n) {
        return name.equalsIgnoreCase(n);
    }

    /**
     * Serializes the function.
     * <p>
     * 함수를 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssEscape.ident(name) + "(" + CssBlock.join(arguments) + ")";
    }
}
