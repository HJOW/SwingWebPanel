package org.duckdns.hjow.swingwebpanel.js;

/**
 * Base class of all function objects (objects with a [[Call]] internal method). Subclasses: {@link NativeFunction}
 * (implemented in Java), {@link BoundFunction} (result of Function.prototype.bind) and the interpreter's script functions.
 * Every function knows the {@link JsRealm} it was created in.
 * <p>
 * 모든 함수 객체([[Call]] 내부 메서드를 가진 객체)의 기반 클래스. 하위 클래스: {@link NativeFunction}(Java 로 구현),
 * {@link BoundFunction}(Function.prototype.bind 의 결과), 인터프리터의 스크립트 함수. 모든 함수는 자신이 만들어진
 * {@link JsRealm} 을 안다.
 */
public abstract class JsFunction extends JsObject {

    /**
     * The realm of this function.
     * <p>
     * 이 함수의 realm.
     */
    protected final JsRealm realm;

    /**
     * Creates a function object.
     * <p>
     * 함수 객체를 만든다.
     *
     * @param realm the realm / realm
     * @param proto the [[Prototype]], normally the realm's Function.prototype / [[Prototype]]. 보통 realm 의 Function.prototype
     */
    protected JsFunction(JsRealm realm, JsObject proto) {
        super(proto);
        this.realm = realm;
    }

    /**
     * Returns the realm this function belongs to.
     * <p>
     * 이 함수가 속한 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public final JsRealm getRealm() {
        return realm;
    }

    /**
     * Functions are callable.
     * <p>
     * 함수는 호출할 수 있다.
     *
     * @return always true / 항상 true
     */
    public boolean isCallable() {
        return true;
    }

    /**
     * Returns "Function".
     * <p>
     * "Function" 을 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Function";
    }

    /**
     * The [[Call]] internal method.
     * <p>
     * [[Call]] 내부 메서드.
     *
     * @param thisValue the this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public abstract JsValue call(JsValue thisValue, JsValue[] args);

    /**
     * Returns the source text shown by Function.prototype.toString. The default is a native-code form.
     * <p>
     * Function.prototype.toString 이 보여 주는 소스 텍스트를 반환한다. 기본값은 네이티브 코드 형태이다.
     *
     * @return the source text / 소스 텍스트
     */
    public String getSourceText() {
        return "function () { [native code] }";
    }

    /**
     * Defines the standard own properties "length" then "name" (non-writable, non-enumerable, configurable).
     * <p>
     * 표준 자체 프로퍼티 "length", "name" 을 이 순서로 정의한다(writable 아님, 열거 불가, configurable).
     *
     * @param name   function name / 함수 이름
     * @param length number of expected arguments / 기대 인수 개수
     */
    protected final void defineNameAndLength(String name, int length) {
        defineOwnProperty("length", PropertyDescriptor.data(JsNumber.of(length), false, false, true));
        defineOwnProperty("name", PropertyDescriptor.data(JsString.of(name), false, false, true));
    }
}
