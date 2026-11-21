package org.duckdns.hjow.swingwebpanel.js.embed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * A live reference from Java code to an object of a script, implemented by {@link JsObjectHandle} (any object, seen as a
 * <code>Map</code>), {@link JsArrayHandle} (an array, seen as a <code>List</code>) and {@link JsFunctionHandle} (a function). Reading and
 * writing through a handle reads and writes the script object itself; nothing is copied. All operations lock the engine, so a handle may
 * be used from any thread, but a script that is running on another thread makes the caller wait.
 * <p>
 * Java 코드에서 스크립트 객체로 가는 살아 있는 참조. {@link JsObjectHandle}(모든 객체, <code>Map</code> 으로 보임), {@link JsArrayHandle}(배열,
 * <code>List</code> 로 보임), {@link JsFunctionHandle}(함수)이 구현한다. 핸들로 읽고 쓰면 스크립트 객체 자체를 읽고 쓰며 아무것도 복사하지 않는다.
 * 모든 연산은 엔진을 잠그므로 핸들은 어느 스레드에서나 쓸 수 있지만, 다른 스레드에서 스크립트가 실행 중이면 호출자가 기다린다.
 */
public interface JsHandle {

    /**
     * Returns the engine that owns the script object.
     * <p>
     * 스크립트 객체를 가진 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    JsEngine getEngine();

    /**
     * Returns the script object itself (an advanced hook for code that works with the classes of the engine directly; such code is
     * responsible for locking the engine).
     * <p>
     * 스크립트 객체 자체를 반환한다(엔진 클래스를 직접 다루는 코드를 위한 고급 연결 지점. 그런 코드가 엔진 잠금을 책임진다).
     *
     * @return the object / 객체
     */
    JsObject getJsObject();

    /**
     * Tells whether the object is a function that can be called.
     * <p>
     * 객체가 호출할 수 있는 함수인지 알려 준다.
     *
     * @return true for a function / 함수이면 true
     */
    boolean isCallable();

    /**
     * Calls a method of the object: <code>object[name](args)</code>.
     * <p>
     * 객체의 메서드를 호출한다: <code>object[name](args)</code>.
     *
     * @param name the property name of the method / 메서드의 프로퍼티 이름
     * @param args the arguments / 인수
     * @return the result / 결과
     * @throws JsScriptError if the property is not a function or the call throws / 프로퍼티가 함수가 아니거나 호출이 예외를 던질 때
     */
    Object callMember(String name, Object... args);
}
