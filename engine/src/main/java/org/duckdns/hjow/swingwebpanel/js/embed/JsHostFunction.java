package org.duckdns.hjow.swingwebpanel.js.embed;

/**
 * A function that the host application (Java code) gives to scripts. This is the only way for scripts to reach Java code: the engine never
 * exposes Java classes, packages, reflection, files or processes by itself. Register a function with
 * {@link JsEngine#putFunction(String, JsHostFunction)}, or put it as a value into a map or an object that you give to the engine; the engine
 * turns it into a script function. The arguments and the result follow the conversion rules of {@link JsEngine}.
 * <pre>
 * engine.putFunction("add", new JsHostFunction() {
 *     public Object call(Object thisValue, Object[] args) {
 *         return Double.valueOf(((Number) args[0]).doubleValue() + ((Number) args[1]).doubleValue());
 *     }
 * });
 * engine.eval("add(1, 2)");  // 3.0
 * </pre>
 * <p>
 * 호스트 응용(Java 코드)이 스크립트에 주는 함수. 스크립트가 Java 코드에 닿는 유일한 방법이다. 엔진은 Java 클래스, 패키지, 리플렉션, 파일, 프로세스를
 * 스스로 노출하지 않는다. {@link JsEngine#putFunction(String, JsHostFunction)} 로 함수를 등록하거나, 엔진에 주는 맵이나 객체에 값으로 넣으면
 * 엔진이 스크립트 함수로 바꾼다. 인수와 결과는 {@link JsEngine} 의 변환 규칙을 따른다.
 */
public interface JsHostFunction {

    /**
     * Runs the function. It is called on the thread that runs the script, while the engine is locked, so it may call back into the engine of
     * the calling script but must not wait for another thread that uses the same engine.
     * <p>
     * 함수를 실행한다. 스크립트를 실행하는 스레드에서 엔진이 잠긴 채로 호출되므로, 호출한 스크립트의 엔진을 다시 호출해도 되지만 같은 엔진을 쓰는 다른
     * 스레드를 기다려서는 안 된다.
     *
     * @param thisValue the <code>this</code> value of the call, <code>JsEngine.UNDEFINED</code> for a plain call / 호출의 <code>this</code> 값.
     *                  일반 호출이면 <code>JsEngine.UNDEFINED</code>
     * @param args      the arguments converted to Java values / Java 값으로 바꾼 인수
     * @return the result (null for JavaScript <code>null</code>, <code>JsEngine.UNDEFINED</code> for undefined) / 결과(JavaScript
     *         <code>null</code> 은 null, undefined 는 <code>JsEngine.UNDEFINED</code>)
     * @throws Exception any failure; a {@link JsScriptError} is passed on to the script as the JavaScript error that it reports, every other
     *                   exception is thrown into the script as an <code>Error</code> with its text as message / 어떤 실패든 던질 수 있다.
     *                   {@link JsScriptError} 는 그것이 알리는 JavaScript 오류로 스크립트에 전달되고, 다른 예외는 그 문자열을 메시지로 한
     *                   <code>Error</code> 로 스크립트에 던져진다
     */
    Object call(Object thisValue, Object[] args) throws Exception;
}
