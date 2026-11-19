package org.duckdns.hjow.swingwebpanel.js;

/**
 * Constants and small factories for ECMAScript values, meant to be used by built-in authors:
 * <code>Js.UNDEFINED</code>, <code>Js.num(1.5)</code>, <code>Js.str("a")</code>, <code>Js.bool(true)</code>.
 * <p>
 * ECMAScript 값의 상수와 간단한 생성 도구. 내장 객체 작성자가 쓰도록 만들었다:
 * <code>Js.UNDEFINED</code>, <code>Js.num(1.5)</code>, <code>Js.str("a")</code>, <code>Js.bool(true)</code>.
 */
public final class Js {

    /**
     * The undefined value constant.
     * <p>
     * undefined 값을 나타내는 상수.
     */
    public static final JsValue UNDEFINED = JsUndefined.INSTANCE;

    /**
     * The null value constant.
     * <p>
     * null 값을 나타내는 상수.
     */
    public static final JsValue NULL = JsNull.INSTANCE;

    /**
     * The true value constant.
     * <p>
     * true 값을 나타내는 상수.
     */
    public static final JsValue TRUE = JsBoolean.TRUE;

    /**
     * The false value constant.
     * <p>
     * false 값을 나타내는 상수.
     */
    public static final JsValue FALSE = JsBoolean.FALSE;

    /**
     * Shared empty argument list.
     * <p>
     * 공유되는 빈 인수 목록.
     */
    public static final JsValue[] NO_ARGS = new JsValue[0];

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Js() {
    }

    /**
     * Creates a Number value.
     * <p>
     * Number 값을 만든다.
     *
     * @param d the double / double 값
     * @return the value / 값
     */
    public static JsNumber num(double d) {
        return JsNumber.of(d);
    }

    /**
     * Creates a String value.
     * <p>
     * String 값을 만든다.
     *
     * @param s the Java string / Java 문자열
     * @return the value / 값
     */
    public static JsString str(String s) {
        return JsString.of(s);
    }

    /**
     * Creates a Boolean value.
     * <p>
     * Boolean 값을 만든다.
     *
     * @param b the boolean / 불리언 값
     * @return the value / 값
     */
    public static JsBoolean bool(boolean b) {
        return JsBoolean.of(b);
    }

    /**
     * Returns the argument at an index or undefined when there are fewer arguments.
     * <p>
     * 인덱스의 인수를 반환한다. 인수가 모자라면 undefined.
     *
     * @param args the argument list / 인수 목록
     * @param i    the index / 인덱스
     * @return the argument or undefined / 인수 또는 undefined
     */
    public static JsValue arg(JsValue[] args, int i) {
        return i < args.length ? args[i] : UNDEFINED;
    }
}
