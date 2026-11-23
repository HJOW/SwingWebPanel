package org.duckdns.hjow.swingwebpanel.js;

/**
 * The seven ECMAScript language types (ECMA-262 6th edition, clause 6.1).
 * Use {@link JsValue#type()} to read the type of a value.
 * <p>
 * ECMAScript 언어의 일곱 가지 자료형(ECMA-262 6판 6.1절).
 * 값의 형식은 {@link JsValue#type()} 으로 읽는다.
 */
public enum JsType {
    /**
     * The Undefined type (single value undefined).
     * <p>
     * Undefined 형식(값은 undefined 하나).
     */
    UNDEFINED,
    /**
     * The Null type (single value null).
     * <p>
     * Null 형식(값은 null 하나).
     */
    NULL,
    /**
     * The Boolean type (true and false).
     * <p>
     * Boolean 형식(true, false).
     */
    BOOLEAN,
    /**
     * The Number type (IEEE 754 double precision).
     * <p>
     * Number 형식(IEEE 754 배정밀도).
     */
    NUMBER,
    /**
     * The String type (sequence of UTF-16 code units).
     * <p>
     * String 형식(UTF-16 코드 단위의 나열).
     */
    STRING,
    /**
     * The Symbol type (unique values).
     * <p>
     * Symbol 형식(고유한 값).
     */
    SYMBOL,
    /**
     * The Object type (all objects including functions).
     * <p>
     * Object 형식(함수를 포함한 모든 객체).
     */
    OBJECT
}
