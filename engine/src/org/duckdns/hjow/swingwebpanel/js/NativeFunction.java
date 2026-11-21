package org.duckdns.hjow.swingwebpanel.js;

/**
 * A function implemented in Java. This is the building block of every built-in function and of host (DOM) functions.
 * <p>
 * Define one with an anonymous class:
 * <pre>
 * proto.defineBuiltin("toString", new NativeFunction(realm, "toString", 0, new NativeFunction.Call() {
 *     public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) { return Js.str("x"); }
 * }));
 * </pre>
 * Constructors additionally pass a {@link Construct}; the function object is then a constructor. When a constructor is called
 * without <code>new</code> the {@link Call} part runs, so a built-in constructor normally supplies both.
 * <p>
 * Java 로 구현한 함수. 모든 내장 함수와 호스트(DOM) 함수의 기본 부품이다.
 * <p>
 * 익명 클래스로 정의한다(위 예시 참조). 생성자는 추가로 {@link Construct} 를 넘기며 그러면 함수 객체가 생성자가 된다.
 * 생성자를 <code>new</code> 없이 호출하면 {@link Call} 부분이 실행되므로 내장 생성자는 보통 둘 다 제공한다.
 */
public class NativeFunction extends JsFunction {

    /**
     * The [[Call]] behaviour of a native function.
     * <p>
     * 네이티브 함수의 [[Call]] 동작.
     */
    public interface Call {
        /**
         * Runs the function.
         * <p>
         * 함수를 실행한다.
         *
         * @param realm     the realm of the function / 함수의 realm
         * @param thisValue the this value (not coerced) / this 값(변환되지 않음)
         * @param args      the arguments; use {@link Js#arg(JsValue[], int)} for optional ones / 인수. 선택 인수는 {@link Js#arg(JsValue[], int)} 사용
         * @return the result (never a Java null) / 결과(Java null 불가)
         */
        JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args);
    }

    /**
     * The [[Construct]] behaviour of a native constructor.
     * <p>
     * 네이티브 생성자의 [[Construct]] 동작.
     */
    public interface Construct {
        /**
         * Creates an object.
         * <p>
         * 객체를 만든다.
         *
         * @param realm     the realm of the constructor / 생성자의 realm
         * @param args      the arguments / 인수
         * @param newTarget the new.target; use {@link Ops#getPrototypeFromConstructor(JsRealm, JsObject, JsObject)} to pick the prototype / new.target. 프로토타입은 {@link Ops#getPrototypeFromConstructor(JsRealm, JsObject, JsObject)} 로 고른다
         * @return the new object / 새 객체
         */
        JsObject construct(JsRealm realm, JsValue[] args, JsObject newTarget);
    }

    /**
     * Call behaviour.
     * <p>
     * 호출 동작.
     */
    private final Call callImpl;

    /**
     * Construct behaviour or null for non-constructors.
     * <p>
     * 생성 동작. 생성자가 아니면 null.
     */
    private final Construct constructImpl;

    /**
     * Creates a non-constructor function whose prototype is the realm's Function.prototype.
     * <p>
     * realm 의 Function.prototype 을 프로토타입으로 갖는 비생성자 함수를 만든다.
     *
     * @param realm  the realm / realm
     * @param name   function name / 함수 이름
     * @param length number of expected arguments / 기대 인수 개수
     * @param call   the behaviour / 동작
     */
    public NativeFunction(JsRealm realm, String name, int length, Call call) {
        this(realm, realm.getFunctionPrototype(), name, length, call, null);
    }

    /**
     * Creates a constructor function whose prototype is the realm's Function.prototype.
     * <p>
     * realm 의 Function.prototype 을 프로토타입으로 갖는 생성자 함수를 만든다.
     *
     * @param realm     the realm / realm
     * @param name      function name / 함수 이름
     * @param length    number of expected arguments / 기대 인수 개수
     * @param call      behaviour when called / 호출 동작
     * @param construct behaviour when constructed / 생성 동작
     */
    public NativeFunction(JsRealm realm, String name, int length, Call call, Construct construct) {
        this(realm, realm.getFunctionPrototype(), name, length, call, construct);
    }

    /**
     * Creates a function with an explicit prototype (used for Function.prototype itself and for subclass constructors).
     * <p>
     * 프로토타입을 직접 지정해 함수를 만든다(Function.prototype 자체와 하위 클래스 생성자용).
     *
     * @param realm     the realm / realm
     * @param proto     the [[Prototype]] / [[Prototype]]
     * @param name      function name / 함수 이름
     * @param length    number of expected arguments / 기대 인수 개수
     * @param call      behaviour when called / 호출 동작
     * @param construct behaviour when constructed or null / 생성 동작. 없으면 null
     */
    public NativeFunction(JsRealm realm, JsObject proto, String name, int length, Call call, Construct construct) {
        super(realm, proto);
        this.callImpl = call;
        this.constructImpl = construct;
        defineNameAndLength(name, length);
    }

    /**
     * Returns true if a {@link Construct} behaviour was supplied.
     * <p>
     * {@link Construct} 동작이 있으면 true.
     *
     * @return true for constructors / 생성자이면 true
     */
    public boolean isConstructor() {
        return constructImpl != null;
    }

    /**
     * Runs the call behaviour.
     * <p>
     * 호출 동작을 실행한다.
     *
     * @param thisValue the this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public JsValue call(JsValue thisValue, JsValue[] args) {
        JsValue r = callImpl.call(realm, thisValue, args);
        return r == null ? Js.UNDEFINED : r;
    }

    /**
     * Runs the construct behaviour.
     * <p>
     * 생성 동작을 실행한다.
     *
     * @param args      arguments / 인수
     * @param newTarget the new.target / new.target
     * @return the new object / 새 객체
     */
    public JsObject construct(JsValue[] args, JsObject newTarget) {
        if (constructImpl == null) {
            throw JsException.typeError("not a constructor");
        }
        return constructImpl.construct(realm, args, newTarget == null ? this : newTarget);
    }
}
