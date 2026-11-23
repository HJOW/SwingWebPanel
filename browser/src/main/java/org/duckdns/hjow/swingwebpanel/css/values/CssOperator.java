package org.duckdns.hjow.swingwebpanel.css.values;

/**
 * A punctuation value inside a structured value: the slash of <code>12px/1.5</code>, <code>16 / 9</code> or <code>a / b</code> and the commas of
 * function arguments that are kept inside a sequence. It carries no meaning of its own; it keeps the structure of the written value so that it
 * can be serialized again and analysed by the consumers.
 * <p>
 * 구조를 가진 값 안의 구두점 값: <code>12px/1.5</code>, <code>16 / 9</code>, <code>a / b</code> 의 슬래시와 시퀀스 안에 남긴 함수 인수의 쉼표. 자체 의미는 없으며 쓰인 값의
 * 구조를 유지해 다시 직렬화하고 사용하는 쪽이 분석할 수 있게 한다.
 */
public final class CssOperator extends CssValue {

    /** The slash operator. / 슬래시 연산자. */
    public static final CssOperator SLASH = new CssOperator('/');

    /** The comma operator. / 쉼표 연산자. */
    public static final CssOperator COMMA = new CssOperator(',');

    /** The punctuation character. / 구두점 문자. */
    private final char ch;

    /**
     * Creates an operator.
     * <p>
     * 연산자를 만든다.
     *
     * @param ch the character / 문자
     */
    private CssOperator(char ch) {
        this.ch = ch;
    }

    /**
     * Returns the character.
     * <p>
     * 문자를 반환한다.
     *
     * @return slash or comma / 슬래시 또는 쉼표
     */
    public char getChar() {
        return ch;
    }

    /**
     * Serializes the character.
     * <p>
     * 문자를 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return String.valueOf(ch);
    }
}
