package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.js.BoundFunction;
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
 * Installs the methods of <code>Function.prototype</code> (ECMA-262 6th edition 19.2.3): <code>apply</code>, <code>bind</code>,
 * <code>call</code>, <code>toString</code> and <code>[Symbol.hasInstance]</code>. The <code>Function</code> constructor itself and
 * <code>eval</code> need the interpreter and belong to the engine core, so they are not created here.
 * <p>
 * <code>Function.prototype</code> 의 메서드(ECMA-262 6판 19.2.3)를 설치한다: <code>apply</code>, <code>bind</code>, <code>call</code>,
 * <code>toString</code>, <code>[Symbol.hasInstance]</code>. <code>Function</code> 생성자와 <code>eval</code> 은 인터프리터가 필요하므로
 * 엔진 코어의 몫이며 여기서 만들지 않는다.
 */
final class FunctionLib {

    /**
     * Text returned by Function.prototype.toString for functions without source text.
     * <p>
     * 소스 텍스트가 없는 함수에 대해 Function.prototype.toString 이 돌려주는 문자열.
     */
    private static final String NATIVE_TEXT = "function () { [native code] }";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FunctionLib() {
    }

    /**
     * Installs the Function.prototype methods.
     * <p>
     * Function.prototype 메서드를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject fp = realm.getFunctionPrototype();
        realm.method(fp, "apply", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                requireCallable(thisValue, "apply");
                JsValue arr = Js.arg(args, 1);
                JsValue[] list = arr.isNullOrUndefined() ? Js.NO_ARGS : Ops.createListFromArrayLike(arr);
                return ((JsObject) thisValue).call(Js.arg(args, 0), list);
            }
        });
        realm.method(fp, "bind", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                requireCallable(thisValue, "bind");
                return bind(r, (JsObject) thisValue, Js.arg(args, 0), args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : Js.NO_ARGS);
            }
        });
        realm.method(fp, "call", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                requireCallable(thisValue, "call");
                JsValue[] rest = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : Js.NO_ARGS;
                return ((JsObject) thisValue).call(Js.arg(args, 0), rest);
            }
        });
        realm.method(fp, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!thisValue.isCallable()) {
                    throw JsException.typeError("Function.prototype.toString requires that 'this' be a Function");
                }
                if (thisValue instanceof BoundFunction || !(thisValue instanceof JsFunction)) {
                    return Js.str(NATIVE_TEXT);
                }
                return Js.str(((JsFunction) thisValue).getSourceText());
            }
        });
        NativeFunction hasInstance = new NativeFunction(realm, "[Symbol.hasInstance]", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.ordinaryHasInstance(thisValue, Js.arg(args, 0)));
            }
        });
        fp.defineData(WellKnownSymbols.HAS_INSTANCE, hasInstance, false, false, false);
    }

    /**
     * Throws a TypeError unless the receiver is callable.
     * <p>
     * 수신 값이 호출 가능하지 않으면 TypeError 를 던진다.
     *
     * @param thisValue the receiver / 수신 값
     * @param method    the method name for the message / 메시지용 메서드 이름
     */
    private static void requireCallable(JsValue thisValue, String method) {
        if (!thisValue.isCallable()) {
            throw JsException.typeError("Function.prototype." + method + " called on non-function");
        }
    }

    /**
     * Function.prototype.bind (19.2.3.2): creates a bound function with the adjusted "length" and the name "bound " plus the target name.
     * <p>
     * Function.prototype.bind (19.2.3.2): 조정된 "length" 와 "bound " 다음에 대상 이름이 붙은 이름을 가진 바운드 함수를 만든다.
     *
     * @param realm     the realm / realm
     * @param target    the function to bind / 바인딩할 함수
     * @param boundThis the bound this value / 고정할 this 값
     * @param bound     the bound leading arguments / 고정할 앞쪽 인수
     * @return the bound function / 바운드 함수
     */
    private static JsValue bind(JsRealm realm, JsObject target, JsValue boundThis, JsValue[] bound) {
        BoundFunction f = new BoundFunction(realm, target, boundThis, bound);
        double length = 0;
        if (target.hasOwnProperty("length")) {
            JsValue tl = target.get("length");
            if (tl instanceof JsNumber) {
                double d = ((JsNumber) tl).getValue();
                if (d == Double.POSITIVE_INFINITY) {
                    length = d;
                } else if (d != Double.NEGATIVE_INFINITY) {
                    length = Math.max(0, Ops.toInteger(d) - bound.length);
                }
            }
        }
        f.definePropertyOrThrow("length", PropertyDescriptor.data(Js.num(length), false, false, true));
        JsValue tn = target.get("name");
        String name = tn instanceof JsString ? ((JsString) tn).getValue() : "";
        f.definePropertyOrThrow("name", PropertyDescriptor.data(Js.str("bound " + name), false, false, true));
        return f;
    }
}
