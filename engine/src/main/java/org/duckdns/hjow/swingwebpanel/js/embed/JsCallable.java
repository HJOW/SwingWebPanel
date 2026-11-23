package org.duckdns.hjow.swingwebpanel.js.embed;

/**
 * A script function that Java code can call. {@link JsEngine} returns it (as a {@link JsFunctionHandle}) wherever a script value is a
 * function. Arguments and results follow the conversion rules of {@link JsEngine}; a script error is thrown as {@link JsScriptError}.
 * <p>
 * Java 코드가 호출할 수 있는 스크립트 함수. 스크립트 값이 함수이면 {@link JsEngine} 이 이것({@link JsFunctionHandle})을 돌려준다. 인수와 결과는
 * {@link JsEngine} 의 변환 규칙을 따르며, 스크립트 오류는 {@link JsScriptError} 로 던져진다.
 */
public interface JsCallable {

    /**
     * Calls the function with <code>this</code> undefined.
     * <p>
     * <code>this</code> 가 undefined 인 채로 함수를 호출한다.
     *
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    Object call(Object... args);

    /**
     * Calls the function with an explicit <code>this</code> value.
     * <p>
     * <code>this</code> 값을 지정해 함수를 호출한다.
     *
     * @param thisValue the <code>this</code> value / <code>this</code> 값
     * @param args      the arguments / 인수
     * @return the result / 결과
     */
    Object callWithThis(Object thisValue, Object... args);

    /**
     * Calls the function as a constructor (<code>new f(args)</code>).
     * <p>
     * 함수를 생성자로 호출한다(<code>new f(args)</code>).
     *
     * @param args the arguments / 인수
     * @return the new object / 새 객체
     * @throws JsScriptError if the function is not a constructor or throws / 함수가 생성자가 아니거나 예외를 던질 때
     */
    Object construct(Object... args);
}
