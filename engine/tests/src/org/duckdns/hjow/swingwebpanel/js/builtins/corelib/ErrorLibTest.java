package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Error and the six native error constructors (plan item P14-01, Error part).
 * <p>
 * Error 와 여섯 가지 기본 오류 생성자의 시험(계획 P14-01 의 Error 부분).
 */
public class ErrorLibTest {

    /**
     * The environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private LibEnv env;

    /**
     * Names of the native error constructors.
     * <p>
     * 기본 오류 생성자의 이름.
     */
    private static final String[] NATIVE = { "EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError", "URIError" };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ErrorLibTest() {
    }

    /**
     * Creates a fresh environment for every case.
     * <p>
     * 사례마다 새 환경을 만든다.
     */
    public void setUp() {
        env = new LibEnv();
    }

    /**
     * Error() works with and without new; the message becomes an own non-enumerable property only when given.
     * <p>
     * Error() 는 new 가 있든 없든 동작하며 message 는 주어졌을 때만 자체 열거 불가 프로퍼티가 된다.
     */
    public void testErrorConstructor() {
        JsObject called = (JsObject) env.call("Error", LibEnv.s("boom"));
        JsObject constructed = env.construct("Error", LibEnv.s("boom"));
        Check.same(env.get("Error.prototype"), called.getPrototypeOf(), "called without new");
        Check.same(env.get("Error.prototype"), constructed.getPrototypeOf(), "constructed");
        Check.equal("boom", LibEnv.str(called.get("message")), "message");
        PropertyDescriptor d = called.getOwnProperty("message");
        Check.isTrue(d.isWritableTrue() && d.isConfigurableTrue() && !d.isEnumerableTrue(), "message attributes");
        Check.isFalse(env.construct("Error").hasOwnProperty("message"), "no message argument: no own property");
        Check.isFalse(env.construct("Error", Js.UNDEFINED).hasOwnProperty("message"), "undefined message: no own property");
        Check.equal("5", LibEnv.str(env.construct("Error", LibEnv.n(5)).get("message")), "message is converted to a string");
        Check.equal("", LibEnv.str(env.construct("Error", LibEnv.s("")).get("message")), "an empty message is an own property");
        Check.isTrue(env.construct("Error", LibEnv.s("")).hasOwnProperty("message"), "empty message exists");
        Check.equal("Error", constructed.className(), "class name for Object.prototype.toString");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.construct("Error", new JsSymbol("s"));
            }
        }, "a symbol message cannot be converted");
        Check.equal(1.0, ((JsNumber) env.get("Error.length")).getValue(), "Error.length");
        Check.same(env.get("Error"), ((JsObject) env.get("Error.prototype")).get("constructor"), "constructor link");
    }

    /**
     * Each native error type has the right prototype chain, name and message defaults.
     * <p>
     * 기본 오류 형식마다 올바른 프로토타입 체인, 이름, 기본 message 가 있다.
     */
    public void testNativeErrors() {
        JsObject errorCtor = env.fn("Error");
        JsObject errorProto = (JsObject) env.get("Error.prototype");
        for (int i = 0; i < NATIVE.length; i++) { // 여섯 기본 오류 형식을 하나씩 확인한다.
            String n = NATIVE[i];
            JsObject ctor = env.fn(n);
            JsObject proto = (JsObject) env.get(n + ".prototype");
            Check.same(errorCtor, ctor.getPrototypeOf(), n + " inherits from the Error constructor");
            Check.same(errorProto, proto.getPrototypeOf(), n + ".prototype inherits from Error.prototype");
            Check.equal(n, LibEnv.str(proto.get("name")), n + ".prototype.name");
            Check.equal("", LibEnv.str(proto.get("message")), n + ".prototype.message");
            Check.same(ctor, proto.get("constructor"), n + ".prototype.constructor");
            Check.isFalse(proto.hasOwnProperty("toString"), n + ".prototype has no own toString");
            Check.equal(1.0, ((JsNumber) ctor.get("length")).getValue(), n + ".length");
            JsObject e = (JsObject) env.call(n, LibEnv.s("m"));
            Check.same(proto, e.getPrototypeOf(), n + " called without new");
            Check.isTrue(Ops.instanceOf(ctor, e) && Ops.instanceOf(errorCtor, e), n + " instance of both");
            Check.equal(n + ": m", LibEnv.str(env.invoke(e, "toString")), n + " toString");
            Check.same(proto, env.realm.getErrorPrototype(ErrorKind.valueOf(toKind(n))), n + " matches the realm prototype");
        }
        Check.equal("Error", LibEnv.str(errorProto.get("name")), "Error.prototype.name");
        Check.equal("", LibEnv.str(errorProto.get("message")), "Error.prototype.message");
        Check.isFalse(errorProto.getPrototypeOf() == null, "Error.prototype inherits from Object.prototype");
        Check.same(env.realm.getObjectPrototype(), errorProto.getPrototypeOf(), "Error.prototype chain");
    }

    /**
     * Converts a native error name into the name of the matching ErrorKind constant.
     * <p>
     * 기본 오류 이름을 대응하는 ErrorKind 상수 이름으로 바꾼다.
     *
     * @param name the JavaScript name / JavaScript 이름
     * @return the constant name / 상수 이름
     */
    private static String toKind(String name) {
        if ("URIError".equals(name)) {
            return "URI_ERROR";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) { // 대문자 앞에 밑줄을 넣어 상수 이름 형태로 만든다.
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /**
     * Error.prototype.toString with every combination of name and message.
     * <p>
     * name 과 message 의 모든 조합에 대한 Error.prototype.toString.
     */
    public void testToString() {
        Check.equal("Error", LibEnv.str(env.callWith("Error.prototype.toString", env.construct("Error"))), "no message");
        Check.equal("Error: m", LibEnv.str(env.callWith("Error.prototype.toString", env.construct("Error", LibEnv.s("m")))), "with message");
        Check.equal("TypeError", LibEnv.str(env.callWith("Error.prototype.toString", env.construct("TypeError"))), "native without message");
        Check.equal("m", LibEnv.str(env.callWith("Error.prototype.toString", env.obj("name", LibEnv.s(""), "message", LibEnv.s("m")))), "empty name");
        Check.equal("N", LibEnv.str(env.callWith("Error.prototype.toString", env.obj("name", LibEnv.s("N"), "message", LibEnv.s("")))), "empty message");
        Check.equal("Error", LibEnv.str(env.callWith("Error.prototype.toString", env.obj("message", Js.UNDEFINED))), "undefined name defaults to Error");
        Check.equal("", LibEnv.str(env.callWith("Error.prototype.toString", env.obj("name", LibEnv.s("")))), "empty name and no message");
        Check.equal("1: 2", LibEnv.str(env.callWith("Error.prototype.toString", env.obj("name", LibEnv.n(1), "message", LibEnv.n(2)))), "values are converted");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Error.prototype.toString", LibEnv.s("x"));
            }
        }, "a primitive receiver");
        LibEnv.throwsKind(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                env.callWith("Error.prototype.toString", Js.UNDEFINED);
            }
        }, "undefined receiver");
    }

    /**
     * Subclass construction through new.target and objects made by the realm for thrown JavaScript errors.
     * <p>
     * new.target 을 통한 하위 클래스 생성과, 던져진 JavaScript 오류용으로 realm 이 만드는 객체.
     */
    public void testNewTargetAndRealmErrors() {
        final JsObject proto = env.realm.newObject();
        NativeFunction sub = new NativeFunction(env.realm, "Sub", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        sub.defineData("prototype", proto, false, false, false);
        JsObject viaSub = env.fn("TypeError").construct(new JsValue[] { LibEnv.s("m") }, sub);
        Check.same(proto, viaSub.getPrototypeOf(), "prototype from new.target");
        Check.equal("m", LibEnv.str(viaSub.get("message")), "message still set");
        JsObject fromRealm = env.realm.createError(ErrorKind.RANGE_ERROR, "r");
        Check.same(env.get("RangeError.prototype"), fromRealm.getPrototypeOf(), "realm.createError uses the installed prototype");
        Check.equal("RangeError: r", LibEnv.str(env.invoke(fromRealm, "toString")), "its string form");
        Check.isTrue(Ops.instanceOf(env.fn("RangeError"), fromRealm), "instanceof with the installed constructor");
    }
}
