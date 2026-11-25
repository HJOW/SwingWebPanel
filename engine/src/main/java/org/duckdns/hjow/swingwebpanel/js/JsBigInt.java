package org.duckdns.hjow.swingwebpanel.js;

import java.math.BigInteger;

/**
 * An immutable ECMAScript BigInt value backed by {@link BigInteger}.
 * <p>
 * {@link BigInteger} 로 구현한 불변 ECMAScript BigInt 값.
 */
public final class JsBigInt extends JsValue {

    /**
     * The zero value.
     * <p>
     * BigInt 의 영 값.
     */
    public static final JsBigInt ZERO = new JsBigInt(BigInteger.ZERO);
    /**
     * The one value.
     * <p>
     * BigInt 의 일 값.
     */
    public static final JsBigInt ONE = new JsBigInt(BigInteger.ONE);

    /** The exact integer. / 정확한 정수. */
    private final BigInteger value;

    /**
     * Creates a value.
     * <p>
     * 값을 만든다.
     *
     * @param value the integer / 정수
     */
    private JsBigInt(BigInteger value) {
        this.value = value;
    }

    /**
     * Returns a cached or new BigInt value.
     * <p>
     * 캐시를 쓰거나 새 BigInt 값을 반환한다.
     *
     * @param value the integer / 정수
     * @return the BigInt value / BigInt 값
     */
    public static JsBigInt of(BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (value.signum() == 0) {
            return ZERO;
        }
        if (value.equals(BigInteger.ONE)) {
            return ONE;
        }
        return new JsBigInt(value);
    }

    /**
     * Returns the exact integer.
     * <p>
     * 정확한 정수를 반환한다.
     *
     * @return the integer / 정수
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Returns {@link JsType#BIGINT}.
     * <p>
     * {@link JsType#BIGINT} 를 반환한다.
     *
     * @return the type / 형식
     */
    public JsType type() {
        return JsType.BIGINT;
    }

    /**
     * Returns the decimal text without the JavaScript <code>n</code> suffix.
     * <p>
     * JavaScript 의 <code>n</code> 접미사 없이 10진수 문자열을 반환한다.
     *
     * @return decimal text / 10진 문자열
     */
    public String toString() {
        return value.toString();
    }
}
