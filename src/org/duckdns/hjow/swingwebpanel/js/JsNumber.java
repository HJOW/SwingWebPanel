package org.duckdns.hjow.swingwebpanel.js;

/**
 * The ECMAScript Number value: an IEEE 754 double with NaN, Infinity and negative zero preserved.
 * Instances are immutable; small integers are cached by {@link #of(double)}.
 * <p>
 * ECMAScript 의 Number 값: IEEE 754 double 이며 NaN, Infinity, 음의 0 을 보존한다.
 * 인스턴스는 불변이고 작은 정수는 {@link #of(double)} 에서 캐시한다.
 */
public final class JsNumber extends JsValue {

    /**
     * Cache of integer values from -128 to 1023.
     * <p>
     * -128 부터 1023 까지 정수 값의 캐시.
     */
    private static final JsNumber[] CACHE = new JsNumber[1152];

    static {
        // 자주 쓰는 작은 정수를 미리 만들어 둔다.
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new JsNumber(i - 128);
        }
    }

    /**
     * The shared NaN value.
     * <p>
     * 숫자가 아님(NaN)을 나타내는 값.
     */
    public static final JsNumber NAN = new JsNumber(Double.NaN);

    /**
     * The positive zero value.
     * <p>
     * 양의 0 값.
     */
    public static final JsNumber ZERO = CACHE[128];

    /**
     * The double this value stands for.
     * <p>
     * 이 값이 나타내는 double.
     */
    private final double value;

    /**
     * Private constructor: use {@link #of(double)}.
     * <p>
     * 비공개 생성자: {@link #of(double)} 를 사용한다.
     *
     * @param value the double / double 값
     */
    private JsNumber(double value) {
        this.value = value;
    }

    /**
     * Returns a Number value for a double, using the cache for small integers (negative zero is never cached).
     * <p>
     * double 에 해당하는 Number 값을 반환한다. 작은 정수는 캐시를 쓰며 음의 0 은 캐시하지 않는다.
     *
     * @param d the double / double 값
     * @return the Number value / Number 값
     */
    public static JsNumber of(double d) {
        int i = (int) d;
        // 정수이면서 -0 이 아니고 캐시 범위 안이면 공유 인스턴스를 돌려준다.
        if (i == d && i >= -128 && i < 1024 && !(i == 0 && 1 / d < 0)) {
            return CACHE[i + 128];
        }
        return new JsNumber(d);
    }

    /**
     * Returns a Number value for an int.
     * <p>
     * int 에 해당하는 Number 값을 반환한다.
     *
     * @param i the int / int 값
     * @return the Number value / Number 값
     */
    public static JsNumber of(int i) {
        if (i >= -128 && i < 1024) {
            return CACHE[i + 128];
        }
        return new JsNumber(i);
    }

    /**
     * Returns the double.
     * <p>
     * double 을 반환한다.
     *
     * @return the double / double 값
     */
    public double getValue() {
        return value;
    }

    /**
     * Returns {@link JsType#NUMBER}.
     * <p>
     * {@link JsType#NUMBER} 를 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.NUMBER;
    }

    /**
     * Returns the ECMAScript string form (Number::toString with radix 10).
     * <p>
     * ECMAScript 문자열 형태(10진수 Number::toString)를 반환한다.
     *
     * @return text form / 문자열 형태
     */
    public String toString() {
        return NumberText.toString(value);
    }
}
