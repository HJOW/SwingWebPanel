package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript Boolean values. Only two instances exist: {@link #TRUE} and {@link #FALSE}; compare with <code>==</code>.
 * <p>
 * ECMAScript 의 Boolean 값. 인스턴스는 {@link #TRUE}, {@link #FALSE} 둘뿐이며 <code>==</code> 로 비교할 수 있다.
 */
public final class JsBoolean extends JsValue {

    /**
     * The shared true value.
     * <p>
     * 참(true) 값을 나타내는 싱글턴 상수.
     */
    public static final JsBoolean TRUE = new JsBoolean(true);

    /**
     * The shared false value.
     * <p>
     * 거짓(false) 값을 나타내는 싱글턴 상수.
     */
    public static final JsBoolean FALSE = new JsBoolean(false);

    /**
     * The Java boolean this value stands for.
     * <p>
     * 이 값이 나타내는 Java boolean.
     */
    private final boolean value;

    /**
     * Private constructor: use {@link #TRUE}, {@link #FALSE} or {@link #of(boolean)}.
     * <p>
     * 비공개 생성자: {@link #TRUE}, {@link #FALSE}, {@link #of(boolean)} 를 사용한다.
     *
     * @param value the boolean / 불리언 값
     */
    private JsBoolean(boolean value) {
        this.value = value;
    }

    /**
     * Returns the shared instance for a Java boolean.
     * <p>
     * Java boolean 에 해당하는 공유 인스턴스를 반환한다.
     *
     * @param b the boolean / 불리언 값
     * @return {@link #TRUE} or {@link #FALSE} / {@link #TRUE} 또는 {@link #FALSE}
     */
    public static JsBoolean of(boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Returns the Java boolean.
     * <p>
     * Java boolean 을 반환한다.
     *
     * @return the boolean / 불리언 값
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Returns {@link JsType#BOOLEAN}.
     * <p>
     * {@link JsType#BOOLEAN} 을 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.BOOLEAN;
    }

    /**
     * Returns "true" or "false".
     * <p>
     * "true" 또는 "false" 를 반환한다.
     *
     * @return text form / 문자열 형태
     */
    public String toString() {
        return value ? "true" : "false";
    }
}
