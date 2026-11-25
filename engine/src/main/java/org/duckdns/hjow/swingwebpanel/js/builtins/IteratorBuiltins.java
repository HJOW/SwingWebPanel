package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.stringarray.ArrayIteratorObject;
import org.duckdns.hjow.swingwebpanel.js.interp.GeneratorObject;
import org.duckdns.hjow.swingwebpanel.js.interp.AsyncGeneratorObject;

/**
 * The iterator related intrinsics: the array iterator (%ArrayIteratorPrototype%), and the generator machinery
 * %GeneratorFunction%, %GeneratorFunction.prototype% and %GeneratorPrototype% with <code>next</code>, <code>return</code> and
 * <code>throw</code> (ECMA-262 6th edition 25.1, 25.2, 25.3). The generator objects themselves resume without Java threads (see
 * {@link GeneratorObject}).
 * <p>
 * 이터레이터 관련 내장 객체: 배열 이터레이터(%ArrayIteratorPrototype%)와 제너레이터 장치 %GeneratorFunction%,
 * %GeneratorFunction.prototype%, %GeneratorPrototype% 및 <code>next</code>, <code>return</code>, <code>throw</code>
 * (ECMA-262 6판 25.1, 25.2, 25.3). 제너레이터 객체는 Java 스레드 없이 재개된다({@link GeneratorObject} 참조).
 */
public final class IteratorBuiltins implements BuiltinInstaller {

    /**
     * Array iterator kind: keys.
     * <p>
     * 배열 이터레이터 종류: 키.
     */
    public static final int KEYS = 0;

    /**
     * Array iterator kind: values.
     * <p>
     * 배열 이터레이터 종류: 값.
     */
    public static final int VALUES = 1;

    /**
     * Array iterator kind: [key, value] pairs.
     * <p>
     * 배열 이터레이터 종류: [키, 값] 쌍.
     */
    public static final int ENTRIES = 2;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    public IteratorBuiltins() {
    }

    /**
     * Creates an array iterator for an object (CreateArrayIterator, ECMA-262 6th edition 22.1.5.1).
     * <p>
     * 객체에 대한 배열 이터레이터를 만든다(CreateArrayIterator, ECMA-262 6판 22.1.5.1).
     *
     * @param realm  the realm / realm
     * @param target the array or array-like object / 배열 또는 유사 배열 객체
     * @param kind   {@link #KEYS}, {@link #VALUES} or {@link #ENTRIES} / {@link #KEYS}, {@link #VALUES}, {@link #ENTRIES} 중 하나
     * @return the iterator / 이터레이터
     */
    public static JsObject createArrayIterator(JsRealm realm, JsObject target, int kind) {
        return ArrayIteratorObject.create(realm, target, kind);
    }

    /**
     * Installs the iterator intrinsics.
     * <p>
     * 이터레이터 내장 객체를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        IteratorConstructor.install(realm);
        ArrayIteratorObject.install(realm);

        installGenerators(realm);
        installFunctionType(realm, "AsyncFunction", null, false, true);
        installAsyncGenerators(realm);
    }

    /**
     * Installs %GeneratorFunction%, %GeneratorFunction.prototype% and %GeneratorPrototype%.
     * <p>
     * %GeneratorFunction%, %GeneratorFunction.prototype%, %GeneratorPrototype% 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private void installGenerators(JsRealm realm) {
        JsObject genProto = new JsObject(realm.getIteratorPrototype());
        JsObject genFnProto = installFunctionType(realm, "GeneratorFunction", genProto, true, false);
        genProto.defineData("constructor", genFnProto, false, false, true);
        genProto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("Generator"), false, false, true);
        realm.method(genProto, "next", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return generator(thisValue, "next").resume(GeneratorObject.NEXT, Js.arg(args, 0));
            }
        });
        realm.method(genProto, "return", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return generator(thisValue, "return").resume(GeneratorObject.RETURN, Js.arg(args, 0));
            }
        });
        realm.method(genProto, "throw", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return generator(thisValue, "throw").resume(GeneratorObject.THROW, Js.arg(args, 0));
            }
        });
        realm.setIntrinsic("%GeneratorPrototype%", genProto);
    }

    /**
     * Installs a generator or async function constructor and its function-prototype object.
     * <p>
     * 제너레이터 또는 비동기 함수 생성자와 함수 프로토타입 객체를 설치한다.
     *
     * @param realm the receiving realm / 설치할 렐름
     * @param name the constructor's name / 생성자 이름
     * @param resultPrototype generator result prototype, or null / 제너레이터 결과 프로토타입 또는 null
     * @param generator whether the parser uses a generator body / 제너레이터 본문 파싱 여부
     * @param async whether the parser uses an async body / 비동기 본문 파싱 여부
     * @return the function prototype / 함수 프로토타입
     */
    private JsObject installFunctionType(JsRealm realm, String name, JsObject resultPrototype, final boolean generator,
            final boolean async) {
        JsObject prototype = new JsObject(realm.getFunctionPrototype());
        NativeFunction ctor = new NativeFunction(realm, realm.getIntrinsic("%Function%"), name, 1, new NativeFunction.Call() {
            /**
             * Creates a dynamic function using the default prototype.
             * <p>
             * 기본 프로토타입으로 동적 함수를 만든다.
             *
             * @param r the realm / 렐름
             * @param receiver the receiver / 수신자
             * @param args the parameter and body sources / 매개변수와 본문 원문
             * @return the dynamic function / 동적 함수
             */
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return r.getInterpreter().createDynamicFunction(args, generator, async, null);
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a dynamic function using the given new target.
             * <p>
             * 지정한 newTarget으로 동적 함수를 만든다.
             *
             * @param r the realm / 렐름
             * @param args the parameter and body sources / 매개변수와 본문 원문
             * @param newTarget the new target / 새 대상
             * @return the dynamic function / 동적 함수
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.getInterpreter().createDynamicFunction(args, generator, async, newTarget);
            }
        });
        ctor.defineData("prototype", prototype, false, false, false);
        prototype.defineData("constructor", ctor, false, false, true);
        if (resultPrototype != null) {
            prototype.defineData("prototype", resultPrototype, false, false, true);
        }
        prototype.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of(name), false, false, true);
        realm.setIntrinsic("%" + name + "%", ctor);
        realm.setIntrinsic("%" + name + ".prototype%", prototype);
        return prototype;
    }

    /**
     * Installs the shared async-generator prototype and its Promise request methods.
     * <p>
     * 공유 async-generator 프로토타입과 Promise 요청 메서드를 설치한다.
     *
     * @param realm realm / realm
     */
    private void installAsyncGenerators(JsRealm realm) {
        JsObject asyncIterator = new JsObject(realm.getObjectPrototype());
        realm.method(asyncIterator, WellKnownSymbols.ASYNC_ITERATOR, 0, new NativeFunction.Call() {
            /**
             * Returns any receiver unchanged so async iterators are iterable.
             * <p>
             * 비동기 이터레이터를 순회할 수 있도록 수신자를 그대로 반환한다.
             *
             * @param r the realm / 렐름
             * @param receiver the receiver / 수신자
             * @param args ignored arguments / 사용하지 않는 인수
             * @return the receiver / 수신자
             */
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return receiver;
            }
        });
        realm.setIntrinsic("%AsyncIteratorPrototype%", asyncIterator);
        JsObject proto = new JsObject(asyncIterator);
        JsObject functionPrototype = installFunctionType(realm, "AsyncGeneratorFunction", proto, true, true);
        proto.defineData("constructor", functionPrototype, false, false, true);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("AsyncGenerator"), false, false, true);
        realm.method(proto, "next", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return asyncGenerator(thisValue, "next").request(AsyncGeneratorObject.NEXT, Js.arg(args, 0));
            }
        });
        realm.method(proto, "return", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return asyncGenerator(thisValue, "return").request(AsyncGeneratorObject.RETURN, Js.arg(args, 0));
            }
        });
        realm.method(proto, "throw", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return asyncGenerator(thisValue, "throw").request(AsyncGeneratorObject.THROW, Js.arg(args, 0));
            }
        });
        realm.setIntrinsic("%AsyncGeneratorPrototype%", proto);
    }

    /**
     * Validates an async-generator receiver.
     * <p>
     * async-generator 수신자를 검사한다.
     */
    private static AsyncGeneratorObject asyncGenerator(JsValue value, String method) {
        if (!(value instanceof AsyncGeneratorObject)) {
            throw JsException.typeError(method + " method called on incompatible receiver " + JsException.describe(value));
        }
        return (AsyncGeneratorObject) value;
    }

    /**
     * Checks that the receiver is a generator object.
     * <p>
     * this 가 제너레이터 객체인지 확인한다.
     *
     * @param v      the receiver / this 값
     * @param method the method name for the message / 메시지용 메서드 이름
     * @return the generator / 제너레이터
     */
    private static GeneratorObject generator(JsValue v, String method) {
        if (!(v instanceof GeneratorObject)) {
            throw JsException.typeError(method + " method called on incompatible receiver " + JsException.describe(v));
        }
        return (GeneratorObject) v;
    }
}
