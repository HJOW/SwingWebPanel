package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Integration test with the complete standard realm created by <code>JsRealm.create()</code>: the engine core installs its minimal
 * built-ins first and then loads {@link CoreLibInstaller} by name, so this checks that the replaced constructors, prototypes and global
 * functions still work together with the interpreter (error objects thrown by the engine, <code>instanceof</code>, class inheritance,
 * <code>constructor</code> links and intrinsic registrations).
 * <p>
 * <code>JsRealm.create()</code> 가 만드는 완전한 표준 realm 과의 통합 시험: 엔진 코어가 최소 내장 객체를 먼저 설치한 뒤 {@link CoreLibInstaller} 를
 * 이름으로 불러 오므로, 교체된 생성자, 프로토타입, 전역 함수가 인터프리터와 함께 계속 동작하는지(엔진이 던지는 오류 객체, <code>instanceof</code>,
 * class 상속, <code>constructor</code> 연결, 내장 객체 등록) 확인한다.
 */
public class StandardRealmTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StandardRealmTest() {
    }

    /**
     * Evaluates one script in a fresh standard realm and returns the string form of the result.
     * <p>
     * 새 표준 realm 에서 스크립트 하나를 실행하고 결과의 문자열 형태를 돌려준다.
     *
     * @param source the script / 스크립트
     * @return the result text or "THROWS name: message" / 결과 문자열 또는 "THROWS 이름: 메시지"
     */
    private static String eval(String source) {
        JsRealm realm = JsRealm.create();
        try {
            JsValue v = realm.evaluateScript(source, "standard");
            return Ops.toString(v);
        } catch (JsException e) {
            return "THROWS " + e.getMessage();
        }
    }

    /**
     * The replaced constructors are the ones that the global object, the intrinsics and the prototypes refer to.
     * <p>
     * 교체된 생성자를 전역 객체, 내장 객체 등록, 프로토타입이 모두 가리킨다.
     */
    public void testConstructorLinks() {
        JsRealm realm = JsRealm.create();
        JsObject global = realm.getGlobalObject();
        String[] names = { "Object", "Boolean", "Symbol", "Error", "EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError",
            "URIError", "Number" };
        for (int i = 0; i < names.length; i++) { // 생성자마다 전역, 내장 객체 이름, prototype.constructor 가 같은 객체인지 확인한다.
            JsObject ctor = (JsObject) global.get(names[i]);
            Check.same(ctor, realm.getIntrinsic("%" + names[i] + "%"), names[i] + " intrinsic");
            JsObject proto = (JsObject) ctor.get("prototype");
            Check.same(ctor, proto.get("constructor"), names[i] + ".prototype.constructor");
        }
        Check.same(realm.getObjectPrototype(), ((JsObject) global.get("Object")).get("prototype"), "Object.prototype is the realm's");
        Check.same(realm.getErrorPrototype(org.duckdns.hjow.swingwebpanel.js.ErrorKind.TYPE_ERROR), ((JsObject) global.get("TypeError")).get("prototype"),
                "TypeError.prototype is the realm's");
    }

    /**
     * Scripts that exercise the library together with the interpreter and the engine's own objects.
     * <p>
     * 라이브러리를 인터프리터와 엔진 자신의 객체와 함께 쓰는 스크립트.
     */
    public void testScriptsInTheStandardRealm() {
        String[][] cases = {
            { "Object.keys({a: 1, b: 2}).length", "2" },
            { "({}).constructor === Object", "true" },
            { "Object.getPrototypeOf(function() {}) === Function.prototype", "true" },
            { "(function() { try { null.x; } catch (e) { return (e instanceof TypeError) + ',' + (e.constructor === TypeError) + ',' + (e.name); } })()",
                "true,true,TypeError" },
            { "(function() { try { undefinedName; } catch (e) { return e instanceof ReferenceError; } })()", "true" },
            { "class MyErr extends RangeError {} var e = new MyErr('m'); (e instanceof RangeError) + ',' + (e instanceof Error) + ',' + e.message", "true,true,m" },
            { "var s = Symbol('q'); var o = {[s]: 1}; Object.getOwnPropertySymbols(o)[0] === s", "true" },
            { "Object.prototype.toString.call([]) + Object.prototype.toString.call(null) + Object.prototype.toString.call(function() {})",
                "[object Array][object Null][object Function]" },
            { "(5).toFixed(2) + ',' + Math.max(1, 2) + ',' + parseInt('42px') + ',' + isNaN('x')", "5.00,2,42,true" },
            { "typeof Symbol.iterator + ',' + (Symbol.for('a') === Symbol.for('a'))", "symbol,true" },
            { "var o = Object.freeze({a: 1}); Object.isFrozen(o) + ',' + Object.isFrozen({})", "true,false" },
            { "function f() { return this.v; } f.bind({v: 7})()", "7" },
        };
        List<String> failures = new ArrayList<String>();
        for (int i = 0; i < cases.length; i++) { // 사례마다 새 표준 realm 에서 실행해 결과를 비교한다.
            String actual = eval(cases[i][0]);
            if (!cases[i][1].equals(actual)) {
                failures.add("[" + cases[i][0] + "] expected <" + cases[i][1] + "> but was <" + actual + ">");
            }
        }
        Check.isTrue(failures.isEmpty(), failures.size() + " case(s) failed: " + failures);
    }
}
