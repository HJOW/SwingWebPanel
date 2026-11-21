package org.duckdns.hjow.swingwebpanel.js;

/**
 * Base class of every ECMAScript value. The hierarchy is {@link JsUndefined}, {@link JsNull}, {@link JsBoolean},
 * {@link JsNumber}, {@link JsString}, {@link JsSymbol} and {@link JsObject}. JavaScript <code>null</code> and
 * <code>undefined</code> are distinct singleton objects; a Java <code>null</code> reference is never a JavaScript value
 * (it means "absent" inside the engine only). Use {@link Js} for constants and factories and {@link Ops} for conversions.
 * <p>
 * 모든 ECMAScript 값의 기반 클래스. 계층은 {@link JsUndefined}, {@link JsNull}, {@link JsBoolean}, {@link JsNumber},
 * {@link JsString}, {@link JsSymbol}, {@link JsObject} 이다. JavaScript 의 <code>null</code> 과 <code>undefined</code> 는
 * 서로 다른 싱글턴 객체이며, Java 의 <code>null</code> 참조는 JavaScript 값이 아니다(엔진 내부에서 "없음"을 뜻할 때만 쓴다).
 * 상수와 생성 도구는 {@link Js}, 형 변환은 {@link Ops} 를 사용한다.
 */
public abstract class JsValue {

    /**
     * Subclass constructor.
     * <p>
     * 하위 클래스용 생성자.
     */
    protected JsValue() {
    }

    /**
     * Returns the ECMAScript language type of this value.
     * <p>
     * 이 값의 ECMAScript 언어 형식을 반환한다.
     *
     * @return the type / 형식
     */
    public abstract JsType type();

    /**
     * Returns true for the undefined value.
     * <p>
     * undefined 값이면 true.
     *
     * @return true if undefined / undefined 이면 true
     */
    public final boolean isUndefined() {
        return this instanceof JsUndefined;
    }

    /**
     * Returns true for the null value.
     * <p>
     * null 값이면 true.
     *
     * @return true if null / null 이면 true
     */
    public final boolean isNull() {
        return this instanceof JsNull;
    }

    /**
     * Returns true for null or undefined.
     * <p>
     * null 또는 undefined 이면 true.
     *
     * @return true if null or undefined / null 또는 undefined 이면 true
     */
    public final boolean isNullOrUndefined() {
        return this instanceof JsUndefined || this instanceof JsNull;
    }

    /**
     * Returns true for any object (including functions).
     * <p>
     * 객체(함수 포함)이면 true.
     *
     * @return true if an object / 객체이면 true
     */
    public final boolean isObject() {
        return this instanceof JsObject;
    }

    /**
     * Returns true if this value is an object with a [[Call]] internal method.
     * <p>
     * [[Call]] 내부 메서드를 가진 객체이면 true.
     *
     * @return true if callable / 호출 가능하면 true
     */
    public boolean isCallable() {
        return false;
    }

    /**
     * Returns the result of the <code>typeof</code> operator for this value.
     * <p>
     * 이 값에 <code>typeof</code> 연산자를 적용한 결과를 반환한다.
     *
     * @return one of "undefined", "object", "boolean", "number", "string", "symbol", "function" / typeof 결과 문자열
     */
    public String typeOf() {
        switch (type()) {
        case UNDEFINED:
            return "undefined";
        case NULL:
            return "object";
        case BOOLEAN:
            return "boolean";
        case NUMBER:
            return "number";
        case STRING:
            return "string";
        case SYMBOL:
            return "symbol";
        default:
            return isCallable() ? "function" : "object";
        }
    }
}
