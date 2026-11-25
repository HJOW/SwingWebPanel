package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BoundFunction;
import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsFunction;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The Function constructor and Function.prototype (ECMA-262 6th edition 19.2): the constructor that compiles source text
 * (CreateDynamicFunction through the interpreter), <code>call</code>, <code>apply</code>, <code>bind</code>,
 * <code>toString</code> and <code>Symbol.hasInstance</code>, plus the %ThrowTypeError% function used for restricted properties.
 * <p>
 * Function 생성자와 Function.prototype(ECMA-262 6판 19.2): 소스 텍스트를 컴파일하는 생성자(인터프리터를 통한 CreateDynamicFunction),
 * <code>call</code>, <code>apply</code>, <code>bind</code>, <code>toString</code>, <code>Symbol.hasInstance</code>, 그리고 제한된
 * 프로퍼티에 쓰는 %ThrowTypeError% 함수.
 */
final class FunctionBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    FunctionBuiltins() {
    }

    /**
     * Installs Function and Function.prototype.
     * <p>
     * Function 과 Function.prototype 을 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        final JsObject proto = realm.getFunctionPrototype();
        NativeFunction.Call call = new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return r.getInterpreter().createDynamicFunction(args, false);
            }
        };
        NativeFunction ctor = new NativeFunction(realm, "Function", 1, call, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.getInterpreter().createDynamicFunction(args, false, false, newTarget);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("Function", ctor);
        realm.setIntrinsic("%Function%", ctor);

        realm.method(proto, "call", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!thisValue.isCallable()) {
                    throw JsException.typeError("Function.prototype.call called on non-function");
                }
                JsValue[] rest = new JsValue[Math.max(0, args.length - 1)];
                System.arraycopy(args, Math.min(1, args.length), rest, 0, rest.length);
                return ((JsObject) thisValue).call(Js.arg(args, 0), rest);
            }
        });
        realm.method(proto, "apply", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!thisValue.isCallable()) {
                    throw JsException.typeError("Function.prototype.apply called on non-function");
                }
                JsValue list = Js.arg(args, 1);
                JsValue[] argv = list.isNullOrUndefined() ? Js.NO_ARGS : Ops.createListFromArrayLike(list);
                return ((JsObject) thisValue).call(Js.arg(args, 0), argv);
            }
        });
        realm.method(proto, "bind", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!thisValue.isCallable()) {
                    throw JsException.typeError("Bind must be called on a function");
                }
                JsObject target = (JsObject) thisValue;
                JsValue[] bound = new JsValue[Math.max(0, args.length - 1)];
                System.arraycopy(args, Math.min(1, args.length), bound, 0, bound.length);
                BoundFunction bf = new BoundFunction(r, target, Js.arg(args, 0), bound);
                double len = 0;
                if (target.getOwnProperty("length") != null) {
                    JsValue l = target.get("length");
                    if (l instanceof JsNumber) {
                        double d = ((JsNumber) l).getValue();
                        len = Double.isInfinite(d) ? (d > 0 ? d : 0) : Math.max(0, Ops.toInteger(d) - bound.length);
                    }
                }
                bf.defineOwnProperty("length", PropertyDescriptor.data(JsNumber.of(len), false, false, true));
                JsValue n = target.get("name");
                String name = n instanceof JsString ? ((JsString) n).getValue() : "";
                bf.defineOwnProperty("name", PropertyDescriptor.data(JsString.of("bound " + name), false, false, true));
                return bf;
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (thisValue instanceof JsFunction) {
                    return JsString.of(((JsFunction) thisValue).getSourceText());
                }
                if (thisValue.isCallable()) {
                    return JsString.of("function () { [native code] }");
                }
                throw JsException.typeError("Function.prototype.toString requires that 'this' be a Function");
            }
        });
        NativeFunction hasInstance = new NativeFunction(realm, "[Symbol.hasInstance]", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.ordinaryHasInstance(thisValue, Js.arg(args, 0)));
            }
        });
        proto.defineData(WellKnownSymbols.HAS_INSTANCE, hasInstance, false, false, false);

        NativeFunction thrower = realm.newFunction("", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("'caller', 'callee', and 'arguments' properties may not be accessed on strict mode functions or the arguments objects for calls to them");
            }
        });
        // 제17판 §10.2.4.1의 제한 접근자 함수는 name과 length를 바꿀 수 없다.
        thrower.defineData("name", Js.str(""), false, false, false);
        thrower.defineData("length", Js.num(0), false, false, false);
        thrower.preventExtensions();
        realm.setIntrinsic("%ThrowTypeError%", thrower);
        // AddRestrictedFunctionProperties(ECMA-262 6판 9.2.7): Function.prototype 의 caller 와 arguments 는 %ThrowTypeError% 접근자이다.
        proto.defineOwnProperty("caller", PropertyDescriptor.accessor(thrower, thrower, false, true));
        proto.defineOwnProperty("arguments", PropertyDescriptor.accessor(thrower, thrower, false, true));
    }
}
