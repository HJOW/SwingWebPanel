package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A functional value that is kept structurally: <code>translate(10px, 5px)</code>, <code>linear-gradient(...)</code>,
 * <code>cubic-bezier(...)</code>, <code>attr(...)</code>, <code>counter(...)</code> and so on. Each comma separated argument is one value
 * (a list when it has several components).
 * <p>
 * 구조를 유지하는 함수형 값: <code>translate(10px, 5px)</code>, <code>linear-gradient(...)</code>, <code>cubic-bezier(...)</code>, <code>attr(...)</code>,
 * <code>counter(...)</code> 등. 쉼표로 구분한 인수 각각이 값 하나이다(구성 요소가 여럿이면 목록).
 */
public final class CssFunctionValue extends CssValue {

    /** Function name in lower case. / 소문자 함수 이름. */
    private final String name;

    /** The comma separated arguments. / 쉼표로 구분한 인수. */
    private final List<CssValue> args;

    /**
     * Creates a functional value.
     * <p>
     * 함수형 값을 만든다.
     *
     * @param name function name (lower-cased here) / 함수 이름(여기서 소문자로 바뀜)
     * @param args arguments (copied) / 인수(복사됨)
     */
    public CssFunctionValue(String name, List<CssValue> args) {
        this.name = CssKeyword.lower(name);
        this.args = Collections.unmodifiableList(new ArrayList<CssValue>(args));
    }

    /**
     * Returns the function name.
     * <p>
     * 함수 이름을 반환한다.
     *
     * @return lower-case name / 소문자 이름
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
    public List<CssValue> getArguments() {
        return args;
    }

    /**
     * Tells whether the function has a name.
     * <p>
     * 함수가 주어진 이름인지 알려 준다.
     *
     * @param n lower-case name / 소문자 이름
     * @return true if equal / 같으면 true
     */
    public boolean isNamed(String n) {
        return name.equals(n);
    }

    /**
     * Serializes the function.
     * <p>
     * 함수를 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder(CssEscape.ident(name)).append('(');
        // 인수를 쉼표와 공백으로 이어 쓴다.
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args.get(i).toCssText());
        }
        return sb.append(')').toString();
    }

    /**
     * Compares name and arguments.
     * <p>
     * 이름과 인수를 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssFunctionValue && ((CssFunctionValue) o).name.equals(name) && ((CssFunctionValue) o).args.equals(args);
    }

    /**
     * Hash of name and arguments.
     * <p>
     * 이름과 인수의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return name.hashCode() * 31 + args.hashCode();
    }
}
