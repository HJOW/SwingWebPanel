package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript undefined value. Only one instance exists: {@link #INSTANCE} (also {@link Js#UNDEFINED}).
 * <p>
 * ECMAScript 의 undefined 값. 인스턴스는 {@link #INSTANCE} 하나뿐이다({@link Js#UNDEFINED} 로도 접근 가능).
 */
public final class JsUndefined extends JsValue {

    /**
     * The only instance.
     * <p>
     * 유일한 인스턴스.
     */
    public static final JsUndefined INSTANCE = new JsUndefined();

    /**
     * Private constructor: use {@link #INSTANCE}.
     * <p>
     * 비공개 생성자: {@link #INSTANCE} 를 사용한다.
     */
    private JsUndefined() {
    }

    /**
     * Returns {@link JsType#UNDEFINED}.
     * <p>
     * {@link JsType#UNDEFINED} 를 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.UNDEFINED;
    }

    /**
     * Returns "undefined".
     * <p>
     * "undefined" 를 반환한다.
     *
     * @return text form / 문자열 형태
     */
    public String toString() {
        return "undefined";
    }
}
