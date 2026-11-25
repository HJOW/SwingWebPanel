package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsErrorObject;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * The Error constructor and the native error types EvalError, RangeError, ReferenceError, SyntaxError, TypeError and URIError
 * (ECMA-262 6th edition 19.5) with their prototypes, <code>name</code>, <code>message</code> and <code>Error.prototype.toString</code>.
 * Error instances also receive a non-standard <code>stack</code> string (the error text) that scripts commonly read.
 * <p>
 * Error 생성자와 기본 오류 형식 EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError(ECMA-262 6판 19.5)와 각각의
 * 프로토타입, <code>name</code>, <code>message</code>, <code>Error.prototype.toString</code>. Error 인스턴스는 스크립트가 흔히 읽는
 * 비표준 <code>stack</code> 문자열(오류 텍스트)도 받는다.
 */
final class ErrorBuiltins implements BuiltinInstaller {

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     */
    ErrorBuiltins() {
    }

    /**
     * Installs all error constructors.
     * <p>
     * 모든 오류 생성자를 설치한다.
     *
     * @param realm the realm / realm
     */
    public void install(final JsRealm realm) {
        ErrorKind[] kinds = ErrorKind.values();
        NativeFunction errorCtor = null;
        // 기본 Error 를 먼저 만들고, 나머지 오류 생성자는 Error 를 프로토타입으로 삼는다.
        for (int i = 0; i < kinds.length; i++) {
            final ErrorKind kind = kinds[i];
            JsObject ctorProto = kind == ErrorKind.ERROR ? realm.getFunctionPrototype() : errorCtor;
            final NativeFunction[] self = new NativeFunction[1];
            NativeFunction ctor = new NativeFunction(realm, ctorProto, kind.getErrorName(), 1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    return make(r, kind, r.getErrorPrototype(kind), Js.arg(args, 0));
                }
            }, new NativeFunction.Construct() {
                public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                    JsObject proto = Ops.getPrototypeFromConstructor(r, newTarget, r.getErrorPrototype(kind));
                    return make(r, kind, proto, Js.arg(args, 0));
                }
            });
            self[0] = ctor;
            JsObject proto = realm.getErrorPrototype(kind);
            ctor.defineData("prototype", proto, false, false, false);
            proto.defineBuiltin("constructor", ctor);
            proto.defineBuiltin("name", JsString.of(kind.getErrorName()));
            proto.defineBuiltin("message", JsString.EMPTY);
            realm.getGlobalObject().defineBuiltin(kind.getErrorName(), ctor);
            realm.setIntrinsic("%" + kind.getErrorName() + "%", ctor);
            if (kind == ErrorKind.ERROR) {
                errorCtor = ctor;
            }
        }
        realm.method(errorCtor, "isError", 1, new NativeFunction.Call() {
            /** Tests the internal error data brand. / 오류 데이터의 내부 브랜드를 검사한다. */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Js.arg(args, 0) instanceof JsErrorObject);
            }
        });
        realm.method(realm.getErrorPrototype(ErrorKind.ERROR), "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof JsObject)) {
                    throw JsException.typeError("Error.prototype.toString requires that 'this' be an Object");
                }
                JsObject o = (JsObject) thisValue;
                JsValue n = o.get("name");
                String name = n.isUndefined() ? "Error" : Ops.toString(n);
                JsValue m = o.get("message");
                String msg = m.isUndefined() ? "" : Ops.toString(m);
                if (name.length() == 0) {
                    return JsString.of(msg);
                }
                if (msg.length() == 0) {
                    return JsString.of(name);
                }
                return JsString.of(name + ": " + msg);
            }
        });
    }

    /**
     * Creates an error instance with an optional message.
     * <p>
     * 선택적 메시지를 가진 오류 인스턴스를 만든다.
     *
     * @param r     the realm / realm
     * @param kind  the error kind / 오류 종류
     * @param proto the prototype of the instance / 인스턴스의 프로토타입
     * @param msg   the message argument or undefined / 메시지 인수 또는 undefined
     * @return the error object / 오류 객체
     */
    private static JsObject make(JsRealm r, ErrorKind kind, JsObject proto, JsValue msg) {
        JsErrorObject e = new JsErrorObject(proto);
        String text = "";
        if (!msg.isUndefined()) {
            text = Ops.toString(msg);
            e.defineOwnProperty("message", PropertyDescriptor.data(JsString.of(text), true, false, true));
        }
        String head = kind.getErrorName() + (text.length() == 0 ? "" : ": " + text);
        e.defineOwnProperty("stack", PropertyDescriptor.data(JsString.of(head + "\n    at <anonymous>"), true, false, true));
        return e;
    }
}
