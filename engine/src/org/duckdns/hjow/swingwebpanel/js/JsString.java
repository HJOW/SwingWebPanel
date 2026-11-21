package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript String value: an immutable sequence of UTF-16 code units, exactly like {@link java.lang.String}.
 * <code>length</code> and indices count code units, not code points; use {@link #codePointAt(int)} for code point access.
 * <p>
 * ECMAScript 의 String 값: {@link java.lang.String} 과 같은 불변 UTF-16 코드 단위 나열.
 * <code>length</code> 와 인덱스는 코드 포인트가 아니라 코드 단위 기준이며, 코드 포인트 접근에는 {@link #codePointAt(int)} 를 쓴다.
 */
public final class JsString extends JsValue {

    /**
     * The empty string value.
     * <p>
     * 빈 문자열 값.
     */
    public static final JsString EMPTY = new JsString("");

    /**
     * The code units.
     * <p>
     * 코드 단위들.
     */
    private final String value;

    /**
     * Private constructor: use {@link #of(String)}.
     * <p>
     * 비공개 생성자: {@link #of(String)} 를 사용한다.
     *
     * @param value the Java string / Java 문자열
     */
    private JsString(String value) {
        this.value = value;
    }

    /**
     * Returns a String value for a Java string.
     * <p>
     * Java 문자열에 해당하는 String 값을 반환한다.
     *
     * @param s the Java string (not null) / Java 문자열(null 불가)
     * @return the String value / String 값
     */
    public static JsString of(String s) {
        if (s.length() == 0) {
            return EMPTY;
        }
        return new JsString(s);
    }

    /**
     * Returns the Java string.
     * <p>
     * Java 문자열을 반환한다.
     *
     * @return the string / 문자열
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the number of UTF-16 code units.
     * <p>
     * UTF-16 코드 단위 수를 반환한다.
     *
     * @return length in code units / 코드 단위 길이
     */
    public int length() {
        return value.length();
    }

    /**
     * Returns the code unit at an index.
     * <p>
     * 인덱스의 코드 단위를 반환한다.
     *
     * @param i index in code units / 코드 단위 인덱스
     * @return the code unit / 코드 단위
     */
    public char charAt(int i) {
        return value.charAt(i);
    }

    /**
     * Returns the code point starting at an index (combining a valid surrogate pair).
     * <p>
     * 인덱스에서 시작하는 코드 포인트를 반환한다(올바른 서로게이트 쌍이면 합친다).
     *
     * @param i index in code units / 코드 단위 인덱스
     * @return the code point / 코드 포인트
     */
    public int codePointAt(int i) {
        return value.codePointAt(i);
    }

    /**
     * Returns {@link JsType#STRING}.
     * <p>
     * {@link JsType#STRING} 을 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.STRING;
    }

    /**
     * Returns the Java string.
     * <p>
     * Java 문자열을 반환한다.
     *
     * @return the string / 문자열
     */
    public String toString() {
        return value;
    }

    /**
     * Compares by code unit sequence.
     * <p>
     * 코드 단위 나열로 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal strings / 같은 문자열이면 true
     */
    public boolean equals(Object o) {
        return o instanceof JsString && ((JsString) o).value.equals(value);
    }

    /**
     * Hash code of the code units.
     * <p>
     * 코드 단위의 해시 코드.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return value.hashCode();
    }
}
