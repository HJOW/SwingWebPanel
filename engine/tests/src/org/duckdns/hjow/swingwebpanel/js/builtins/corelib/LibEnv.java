package org.duckdns.hjow.swingwebpanel.js.builtins.corelib;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.testing.AssertionFailure;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Test environment for the fundamental built-ins: a realm with {@link CoreLibInstaller} installed, plus short helpers to look up
 * properties by dotted path, call installed natives through {@link Ops}, build values and check thrown JavaScript errors.
 * <p>
 * 기본 내장 객체 시험 환경: {@link CoreLibInstaller} 를 설치한 realm 과, 점으로 이은 경로로 프로퍼티를 찾고 {@link Ops} 로 설치된
 * 네이티브를 호출하며 값을 만들고 던져진 JavaScript 오류를 확인하는 짧은 도우미.
 */
final class LibEnv {

    /**
     * The realm under test.
     * <p>
     * 시험 대상 realm.
     */
    final JsRealm realm;

    /**
     * Creates an environment with a fresh realm and the core library installed.
     * <p>
     * 새 realm 에 핵심 라이브러리를 설치한 환경을 만든다.
     */
    LibEnv() {
        realm = new JsRealm();
        new CoreLibInstaller().install(realm);
        // Function 생성자는 엔진 코어의 몫이므로, 시험에서 경로로 찾을 수 있도록 자리표시 생성자만 만들어 둔다.
        NativeFunction function = new NativeFunction(realm, "Function", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("test placeholder");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("test placeholder");
            }
        });
        function.defineData("prototype", realm.getFunctionPrototype(), false, false, false);
        realm.getFunctionPrototype().defineBuiltin("constructor", function);
        realm.getGlobalObject().defineBuiltin("Function", function);
    }

    /**
     * Looks up a value by a dotted path starting at the global object, for example <code>Math.PI</code>.
     * <p>
     * 전역 객체에서 시작하는 점으로 이은 경로로 값을 찾는다. 예: <code>Math.PI</code>.
     *
     * @param path the path / 경로
     * @return the value / 값
     */
    JsValue get(String path) {
        JsValue cur = realm.getGlobalObject();
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) { // 경로의 조각마다 프로퍼티를 읽어 내려간다.
            cur = ((JsObject) cur).get(parts[i]);
        }
        return cur;
    }

    /**
     * Looks up a function by dotted path.
     * <p>
     * 점으로 이은 경로로 함수를 찾는다.
     *
     * @param path the path / 경로
     * @return the function object / 함수 객체
     */
    JsObject fn(String path) {
        JsValue v = get(path);
        Check.isTrue(v instanceof JsObject && v.isCallable(), path + " must be a function but is " + v);
        return (JsObject) v;
    }

    /**
     * Calls the function at a path with the owner object as this value (the global object for top-level functions).
     * <p>
     * 경로의 함수를 소유 객체를 this 로 하여 호출한다(최상위 함수는 전역 객체).
     *
     * @param path the path of the function / 함수의 경로
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    JsValue call(String path, JsValue... args) {
        int dot = path.lastIndexOf('.');
        JsValue owner = dot < 0 ? realm.getGlobalObject() : get(path.substring(0, dot));
        return fn(path).call(owner, args);
    }

    /**
     * Calls the function at a path with an explicit this value.
     * <p>
     * 경로의 함수를 명시한 this 값으로 호출한다.
     *
     * @param path      the path of the function / 함수의 경로
     * @param thisValue the this value / this 값
     * @param args      the arguments / 인수
     * @return the result / 결과
     */
    JsValue callWith(String path, JsValue thisValue, JsValue... args) {
        return fn(path).call(thisValue, args);
    }

    /**
     * Calls a method of a value (through the prototype chain, with ToObject for primitives).
     * <p>
     * 값의 메서드를 호출한다(프로토타입 체인을 통하며 원시 값은 ToObject 를 거친다).
     *
     * @param target the receiver / 수신 값
     * @param method the method name / 메서드 이름
     * @param args   the arguments / 인수
     * @return the result / 결과
     */
    JsValue invoke(JsValue target, String method, JsValue... args) {
        return Ops.invoke(realm, target, method, args);
    }

    /**
     * Constructs an object with a constructor found by path.
     * <p>
     * 경로로 찾은 생성자로 객체를 만든다.
     *
     * @param path the path of the constructor / 생성자의 경로
     * @param args the arguments / 인수
     * @return the new object / 새 객체
     */
    JsObject construct(String path, JsValue... args) {
        return Ops.construct(get(path), args);
    }

    /**
     * Creates a number value.
     * <p>
     * 숫자 값을 만든다.
     *
     * @param d the number / 숫자
     * @return the value / 값
     */
    static JsValue n(double d) {
        return Js.num(d);
    }

    /**
     * Creates a string value.
     * <p>
     * 문자열 값을 만든다.
     *
     * @param s the text / 문자열
     * @return the value / 값
     */
    static JsValue s(String s) {
        return JsString.of(s);
    }

    /**
     * Creates a boolean value.
     * <p>
     * 불리언 값을 만든다.
     *
     * @param b the boolean / 불리언
     * @return the value / 값
     */
    static JsValue b(boolean b) {
        return Js.bool(b);
    }

    /**
     * Converts a value to a Java string with ToString.
     * <p>
     * 값을 ToString 으로 Java 문자열로 바꾼다.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    static String str(JsValue v) {
        return Ops.toString(v);
    }

    /**
     * Creates an ordinary object from alternating names and values.
     * <p>
     * 이름과 값을 번갈아 준 목록으로 일반 객체를 만든다.
     *
     * @param kv names (String) and values (JsValue) alternately / 이름(String)과 값(JsValue)을 번갈아
     * @return the object / 객체
     */
    JsObject obj(Object... kv) {
        JsObject o = realm.newObject();
        for (int i = 0; i + 1 < kv.length; i += 2) { // 이름/값 쌍마다 열거 가능한 데이터 프로퍼티를 만든다.
            o.createDataProperty(kv[i], (JsValue) kv[i + 1]);
        }
        return o;
    }

    /**
     * Joins the string keys of an array-like value with commas for compact checks.
     * <p>
     * 간단히 비교하도록 유사 배열 값의 요소를 쉼표로 이어 붙인다.
     *
     * @param arrayLike the array-like value / 유사 배열 값
     * @return the joined text / 이어 붙인 문자열
     */
    static String join(JsValue arrayLike) {
        JsObject a = (JsObject) arrayLike;
        int len = (int) Ops.lengthOf(a);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) { // 요소를 앞에서부터 문자열로 바꿔 이어 붙인다.
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Ops.toString(a.get(Integer.toString(i))));
        }
        return sb.toString();
    }

    /**
     * Checks that a block throws a JavaScript error of the given kind.
     * <p>
     * 코드 조각이 주어진 종류의 JavaScript 오류를 던지는지 확인한다.
     *
     * @param kind    the expected error kind / 기대하는 오류 종류
     * @param block   the code / 코드
     * @param message the failure description / 실패 설명
     */
    static void throwsKind(ErrorKind kind, Block block, String message) {
        try {
            block.run();
        } catch (JsException e) {
            Check.equal(kind, e.getKind(), message + " (error kind)");
            return;
        } catch (Throwable t) {
            throw new AssertionFailure(message + " (expected " + kind + " but " + t + " was thrown)");
        }
        throw new AssertionFailure(message + " (expected " + kind + " but nothing was thrown)");
    }

    /**
     * Returns the own property descriptor of an object.
     * <p>
     * 객체의 자체 프로퍼티 기술자를 돌려준다.
     *
     * @param o   the object / 객체
     * @param key the key / 키
     * @return the descriptor or null / 기술자 또는 null
     */
    static PropertyDescriptor desc(JsObject o, Object key) {
        return o.getOwnProperty(key);
    }
}
