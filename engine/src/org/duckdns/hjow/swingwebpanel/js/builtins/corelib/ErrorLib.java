package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsErrorObject;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * Installs <code>Error</code> and the six native error constructors <code>EvalError</code>, <code>RangeError</code>,
 * <code>ReferenceError</code>, <code>SyntaxError</code>, <code>TypeError</code>, <code>URIError</code> with their prototypes
 * (ECMA-262 6th edition 19.5). Every constructor works with and without <code>new</code>; the prototypes carry the <code>name</code> and
 * <code>message</code> properties and <code>Error.prototype.toString</code>.
 * <p>
 * <code>Error</code> 와 여섯 가지 기본 오류 생성자 <code>EvalError</code>, <code>RangeError</code>, <code>ReferenceError</code>,
 * <code>SyntaxError</code>, <code>TypeError</code>, <code>URIError</code> 를 프로토타입과 함께 설치한다(ECMA-262 6판 19.5).
 * 모든 생성자는 <code>new</code> 가 있든 없든 동작하며, 프로토타입은 <code>name</code>, <code>message</code> 프로퍼티와
 * <code>Error.prototype.toString</code> 을 가진다.
 */
final class ErrorLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ErrorLib() {
    }

    /**
     * Installs the Error family into the realm.
     * <p>
     * realm 에 Error 계열을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        NativeFunction errorCtor = installOne(realm, ErrorKind.ERROR, null);
        JsObject errorProto = realm.getErrorPrototype(ErrorKind.ERROR);
        realm.method(errorProto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = LibSupport.requireObject(thisValue, "Error.prototype.toString requires that 'this' be an Object");
                JsValue n = o.get("name");
                String name = n.isUndefined() ? "Error" : Ops.toString(n);
                JsValue m = o.get("message");
                String msg = m.isUndefined() ? "" : Ops.toString(m);
                if (name.length() == 0) {
                    return Js.str(msg);
                }
                if (msg.length() == 0) {
                    return Js.str(name);
                }
                return Js.str(name + ": " + msg);
            }
        });
        ErrorKind[] kinds = ErrorKind.values();
        // Error 를 뺀 여섯 기본 오류 형식의 생성자는 Error 생성자를 [[Prototype]] 으로 가진다.
        for (int i = 0; i < kinds.length; i++) {
            if (kinds[i] != ErrorKind.ERROR) {
                installOne(realm, kinds[i], errorCtor);
            }
        }
    }

    /**
     * Installs one error constructor and fills its prototype with <code>message</code> and <code>name</code>.
     * <p>
     * 오류 생성자 하나를 설치하고 프로토타입에 <code>message</code> 와 <code>name</code> 을 채운다.
     *
     * @param realm  the realm / realm
     * @param kind   the error kind / 오류 종류
     * @param parent the [[Prototype]] of the constructor (the Error constructor for native errors) or null / 생성자의 [[Prototype]](기본 오류는 Error 생성자). 없으면 null
     * @return the constructor / 생성자
     */
    private static NativeFunction installOne(JsRealm realm, final ErrorKind kind, JsObject parent) {
        final JsObject proto = realm.getErrorPrototype(kind);
        NativeFunction ctor = LibSupport.defineConstructor(realm, kind.getErrorName(), 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return create(r, kind, args, null);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return create(r, kind, args, newTarget);
            }
        }, proto, parent);
        proto.defineBuiltin("message", JsString.EMPTY);
        proto.defineBuiltin("name", JsString.of(kind.getErrorName()));
        return ctor;
    }

    /**
     * Creates an error object (19.5.1.1, 19.5.6.1.1): the prototype comes from new.target if given, and a non-undefined message becomes
     * an own non-enumerable <code>message</code> property.
     * <p>
     * 오류 객체를 만든다(19.5.1.1, 19.5.6.1.1): new.target 이 있으면 그 프로토타입을 쓰고, undefined 가 아닌 메시지는 자체 열거 불가
     * <code>message</code> 프로퍼티가 된다.
     *
     * @param realm     the realm / realm
     * @param kind      the error kind / 오류 종류
     * @param args      the constructor arguments / 생성자 인수
     * @param newTarget new.target or null / new.target 또는 null
     * @return the error object / 오류 객체
     */
    private static JsObject create(JsRealm realm, ErrorKind kind, JsValue[] args, JsObject newTarget) {
        JsObject proto = Ops.getPrototypeFromConstructor(realm, newTarget, realm.getErrorPrototype(kind));
        JsErrorObject e = new JsErrorObject(proto);
        JsValue message = Js.arg(args, 0);
        if (!message.isUndefined()) {
            e.defineOwnProperty("message", PropertyDescriptor.data(JsString.of(Ops.toString(message)), true, false, true));
        }
        return e;
    }
}
