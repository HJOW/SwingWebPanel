package org.duckdns.hjow.swingwebpanel.js;

/**
 * Helpers for property keys. A property key is either a {@link java.lang.String} or a {@link JsSymbol}; engine code
 * stores keys as plain Java <code>Object</code> references holding one of those two types.
 * <p>
 * 프로퍼티 키 도우미. 프로퍼티 키는 {@link java.lang.String} 또는 {@link JsSymbol} 이며, 엔진 코드는 키를
 * 이 두 형식 중 하나를 담은 Java <code>Object</code> 참조로 저장한다.
 */
public final class PropertyKeys {

    /**
     * The largest array index plus one (2^32 - 1), also the maximum array length.
     * <p>
     * 가장 큰 배열 인덱스 + 1 (2^32 - 1). 배열 길이의 최댓값이기도 하다.
     */
    public static final long MAX_ARRAY_LENGTH = 4294967295L;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PropertyKeys() {
    }

    /**
     * Returns the array index for a canonical numeric string ("0", "1", ... "4294967294"), or -1 if the string is not one
     * (leading zeros, signs, decimals and values of 2^32 - 1 or more are not array indices).
     * <p>
     * 정규 숫자 문자열("0", "1", ... "4294967294")의 배열 인덱스를 반환한다. 그렇지 않으면 -1
     * (앞의 0, 부호, 소수, 2^32 - 1 이상의 값은 배열 인덱스가 아니다).
     *
     * @param s candidate string / 후보 문자열
     * @return the index or -1 / 인덱스 또는 -1
     */
    public static long arrayIndex(String s) {
        int n = s.length();
        if (n == 0 || n > 10) {
            return -1;
        }
        char first = s.charAt(0);
        if (first < '0' || first > '9') {
            return -1;
        }
        if (first == '0') {
            return n == 1 ? 0 : -1;
        }
        long v = 0;
        // 모든 문자가 숫자인지 확인하며 값을 계산한다.
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v < MAX_ARRAY_LENGTH ? v : -1;
    }

    /**
     * Returns the property key for an integer index.
     * <p>
     * 정수 인덱스의 프로퍼티 키를 반환한다.
     *
     * @param index non-negative index / 0 이상의 인덱스
     * @return the key / 키
     */
    public static String fromIndex(long index) {
        return Long.toString(index);
    }

    /**
     * Describes a key for messages: strings as is, symbols as their descriptive string.
     * <p>
     * 메시지용으로 키를 설명한다: 문자열은 그대로, 심볼은 설명 문자열.
     *
     * @param key a property key / 프로퍼티 키
     * @return text / 문자열
     */
    public static String describe(Object key) {
        return key instanceof JsSymbol ? ((JsSymbol) key).toString() : String.valueOf(key);
    }

    /**
     * Returns the value form of a key (String value or Symbol value).
     * <p>
     * 키를 값 형태(String 값 또는 Symbol 값)로 바꾼다.
     *
     * @param key a property key / 프로퍼티 키
     * @return the JavaScript value / JavaScript 값
     */
    public static JsValue toValue(Object key) {
        return key instanceof JsSymbol ? (JsSymbol) key : JsString.of((String) key);
    }
}
