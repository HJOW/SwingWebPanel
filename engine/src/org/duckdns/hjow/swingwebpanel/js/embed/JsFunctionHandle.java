package org.duckdns.hjow.swingwebpanel.js.embed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * A live reference to a script function. It is a {@link JsObjectHandle} (a function is an object and may have properties) that Java code can
 * call through {@link JsCallable}. The engine lock rules and the conversion rules of {@link JsEngine} apply to every call.
 * <p>
 * 스크립트 함수에 대한 살아 있는 참조. {@link JsObjectHandle}(함수도 객체이며 프로퍼티를 가질 수 있다)이며 Java 코드가 {@link JsCallable} 로 호출할 수 있다.
 * 모든 호출에 엔진 잠금 규칙과 {@link JsEngine} 의 변환 규칙이 적용된다.
 */
public final class JsFunctionHandle extends JsObjectHandle implements JsCallable {

    /**
     * Creates a handle (convert a script value that is a function to get one).
     * <p>
     * 핸들을 만든다(함수인 스크립트 값을 바꿔 얻는다).
     *
     * @param engine   the engine / 엔진
     * @param function the script function / 스크립트 함수
     */
    JsFunctionHandle(JsEngine engine, JsObject function) {
        super(engine, function);
    }

    /**
     * Calls the function with <code>this</code> undefined.
     * <p>
     * <code>this</code> 가 undefined 인 채로 함수를 호출한다.
     *
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    public Object call(Object... args) {
        return getEngine().callFunction(this, JsEngine.UNDEFINED, args);
    }

    /**
     * Calls the function with an explicit <code>this</code> value.
     * <p>
     * <code>this</code> 값을 지정해 함수를 호출한다.
     *
     * @param thisValue the <code>this</code> value / <code>this</code> 값
     * @param args      the arguments / 인수
     * @return the result / 결과
     */
    public Object callWithThis(Object thisValue, Object... args) {
        return getEngine().callFunction(this, thisValue, args);
    }

    /**
     * Calls the function as a constructor.
     * <p>
     * 함수를 생성자로 호출한다.
     *
     * @param args the arguments / 인수
     * @return the new object / 새 객체
     */
    public Object construct(Object... args) {
        return getEngine().construct(this, args);
    }
}
