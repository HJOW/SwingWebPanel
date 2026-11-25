package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
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
     * An Array Iterator instance (CreateArrayIterator): iterates the indices of an array-like object.
     * <p>
     * 배열 이터레이터 인스턴스(CreateArrayIterator): 유사 배열 객체의 인덱스를 순회한다.
     */
    private static final class ArrayIteratorObject extends JsObject {
        /**
         * The iterated object, or null when finished.
         * <p>
         * 순회하는 객체. 끝났으면 null.
         */
        JsObject target;
        /**
         * The next index.
         * <p>
         * 다음 인덱스.
         */
        int index;
        /**
         * One of {@link IteratorBuiltins#KEYS}, {@link IteratorBuiltins#VALUES}, {@link IteratorBuiltins#ENTRIES}.
         * <p>
         * {@link IteratorBuiltins#KEYS}, {@link IteratorBuiltins#VALUES}, {@link IteratorBuiltins#ENTRIES} 중 하나.
         */
        final int kind;

        /**
         * Creates the iterator.
         * <p>
         * 이터레이터를 만든다.
         *
         * @param proto  the prototype / 프로토타입
         * @param target the iterated object / 순회할 객체
         * @param kind   the kind / 종류
         */
        ArrayIteratorObject(JsObject proto, JsObject target, int kind) {
            super(proto);
            this.target = target;
            this.kind = kind;
        }
    }

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
        return new ArrayIteratorObject(realm.getIntrinsic("%ArrayIteratorPrototype%"), target, kind);
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
        JsObject arrayIterProto = new JsObject(realm.getIteratorPrototype());
        realm.setIntrinsic("%ArrayIteratorPrototype%", arrayIterProto);
        realm.method(arrayIterProto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof ArrayIteratorObject)) {
                    throw JsException.typeError("next method called on incompatible receiver " + JsException.describe(thisValue));
                }
                ArrayIteratorObject it = (ArrayIteratorObject) thisValue;
                if (it.target == null) {
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                double len = it.target instanceof JsArray ? ((JsArray) it.target).getLength() : Ops.lengthOf(it.target);
                int i = it.index;
                if (i >= len) {
                    it.target = null;
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                it.index = i + 1;
                if (it.kind == KEYS) {
                    return IteratorHelper.createResult(r, JsNumber.of(i), false);
                }
                JsValue v = it.target.get(PropertyKeys.fromIndex(i));
                if (it.kind == VALUES) {
                    return IteratorHelper.createResult(r, v, false);
                }
                JsArray pair = r.newArray();
                pair.add(JsNumber.of(i));
                pair.add(v);
                return IteratorHelper.createResult(r, pair, false);
            }
        });
        arrayIterProto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("Array Iterator"), false, false, true);

        installGenerators(realm);
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
        JsObject genFnProto = new JsObject(realm.getFunctionPrototype());
        NativeFunction genFnCtor = new NativeFunction(realm, realm.getIntrinsic("%Function%"), "GeneratorFunction", 1,
                new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        return r.getInterpreter().createDynamicFunction(args, true);
                    }
                }, new NativeFunction.Construct() {
                    public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                        return r.getInterpreter().createDynamicFunction(args, true);
                    }
                });
        genFnCtor.defineData("prototype", genFnProto, false, false, false);
        genFnProto.defineData("constructor", genFnCtor, false, false, true);
        genFnProto.defineData("prototype", genProto, false, false, true);
        genFnProto.defineData(WellKnownSymbols.TO_STRING_TAG, JsString.of("GeneratorFunction"), false, false, true);
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
        realm.setIntrinsic("%GeneratorFunction%", genFnCtor);
        realm.setIntrinsic("%GeneratorFunction.prototype%", genFnProto);
        realm.setIntrinsic("%GeneratorPrototype%", genProto);
    }

    /**
     * Installs the shared async-generator prototype and its Promise request methods.
     * <p>
     * 공유 async-generator 프로토타입과 Promise 요청 메서드를 설치한다.
     *
     * @param realm realm / realm
     */
    private void installAsyncGenerators(JsRealm realm) {
        JsObject proto = new JsObject(realm.getIteratorPrototype());
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
        proto.defineData(WellKnownSymbols.ASYNC_ITERATOR, new NativeFunction(realm, "[Symbol.asyncIterator]", 0,
                new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        asyncGenerator(thisValue, "Symbol.asyncIterator");
                        return thisValue;
                    }
                }), false, false, true);
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
