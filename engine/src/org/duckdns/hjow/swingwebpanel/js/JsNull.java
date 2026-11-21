package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript null value. Only one instance exists: {@link #INSTANCE} (also {@link Js#NULL}).
 * <p>
 * ECMAScript 의 null 값. 인스턴스는 {@link #INSTANCE} 하나뿐이다({@link Js#NULL} 로도 접근 가능).
 */
public final class JsNull extends JsValue {

    /**
     * The only instance.
     * <p>
     * 유일한 인스턴스.
     */
    public static final JsNull INSTANCE = new JsNull();

    /**
     * Private constructor: use {@link #INSTANCE}.
     * <p>
     * 비공개 생성자: {@link #INSTANCE} 를 사용한다.
     */
    private JsNull() {
    }

    /**
     * Returns {@link JsType#NULL}.
     * <p>
     * {@link JsType#NULL} 을 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.NULL;
    }

    /**
     * Returns "null".
     * <p>
     * "null" 을 반환한다.
     *
     * @return text form / 문자열 형태
     */
    public String toString() {
        return "null";
    }
}
