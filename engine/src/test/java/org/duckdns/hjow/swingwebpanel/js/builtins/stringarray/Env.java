package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsBoolean;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Test environment of the String/Array built-in tests: a realm with the {@link StringArrayInstaller} installed and short helpers to build values,
 * call methods and check JavaScript errors from Java.
 * <p>
 * String/Array 내장 객체 시험의 시험 환경: {@link StringArrayInstaller} 가 설치된 realm 과, 값을 만들고 메서드를 호출하고 JavaScript 오류를 Java 에서
 * 확인하는 짧은 도우미.
 */
public final class Env {

    /**
     * Marker for a hole when building arrays with {@link #array(Object...)}.
     * <p>
     * {@link #array(Object...)} 로 배열을 만들 때 구멍을 나타내는 표식.
     */
    public static final Object HOLE = new Object();

    /**
     * The realm.
     * <p>
     * 시험에 쓰는 realm 객체.
     */
    public final JsRealm realm;

    /**
     * Creates a realm with String and Array installed.
     * <p>
     * String 과 Array 가 설치된 realm 을 만든다.
     */
    public Env() {
        realm = new JsRealm();
        new StringArrayInstaller().install(realm);
    }

    /**
     * Converts a Java value to a JavaScript value: Strings, Numbers, Booleans, JsValues, and null for JavaScript undefined.
     * <p>
     * Java 값을 JavaScript 값으로 바꾼다: String, Number, Boolean, JsValue, 그리고 null 은 JavaScript undefined.
     *
     * @param o Java value / Java 값
     * @return JavaScript value / JavaScript 값
     */
    public JsValue v(Object o) {
        if (o == null) {
            return Js.UNDEFINED;
        }
        if (o instanceof JsValue) {
            return (JsValue) o;
        }
        if (o instanceof String) {
            return Js.str((String) o);
        }
        if (o instanceof Number) {
            return Js.num(((Number) o).doubleValue());
        }
        if (o instanceof Boolean) {
            return Js.bool(((Boolean) o).booleanValue());
        }
        throw new IllegalArgumentException("cannot convert " + o);
    }

    /**
     * Converts Java values to JavaScript values.
     * <p>
     * Java 값들을 JavaScript 값들로 바꾼다.
     *
     * @param items Java values / Java 값
     * @return JavaScript values / JavaScript 값
     */
    public JsValue[] vs(Object... items) {
        JsValue[] r = new JsValue[items.length];
        // 값을 하나씩 변환한다.
        for (int i = 0; i < items.length; i++) {
            r[i] = v(items[i]);
        }
        return r;
    }

    /**
     * Creates an array; {@link #HOLE} entries stay holes.
     * <p>
     * 배열을 만든다. {@link #HOLE} 항목은 구멍으로 남는다.
     *
     * @param items elements / 요소
     * @return the array / 배열
     */
    public JsArray array(Object... items) {
        JsArray a = realm.newArray();
        a.defineOwnProperty("length", org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.valueOnly(Js.num(items.length)));
        // 구멍이 아닌 항목만 인덱스 프로퍼티로 정의한다.
        for (int i = 0; i < items.length; i++) {
            if (items[i] != HOLE) {
                a.createDataPropertyOrThrow(PropertyKeys.fromIndex(i), v(items[i]));
            }
        }
        return a;
    }

    /**
     * Creates an array-like plain object <code>{length: n, 0: ..., 1: ...}</code>; {@link #HOLE} entries are left out.
     * <p>
     * 유사 배열 일반 객체 <code>{length: n, 0: ..., 1: ...}</code> 를 만든다. {@link #HOLE} 항목은 빼 둔다.
     *
     * @param items elements / 요소
     * @return the object / 객체
     */
    public JsObject arrayLike(Object... items) {
        JsObject o = realm.newObject();
        // 인덱스 프로퍼티와 length 를 일반 객체에 정의한다.
        for (int i = 0; i < items.length; i++) {
            if (items[i] != HOLE) {
                o.createDataPropertyOrThrow(PropertyKeys.fromIndex(i), v(items[i]));
            }
        }
        o.createDataPropertyOrThrow("length", Js.num(items.length));
        return o;
    }

    /**
     * Calls a method of a value: <code>receiver[name](...args)</code>.
     * <p>
     * 값의 메서드를 호출한다: <code>receiver[name](...args)</code>.
     *
     * @param receiver the receiver / 수신자
     * @param name     method name / 메서드 이름
     * @param args     arguments / 인수
     * @return the result / 결과
     */
    public JsValue call(Object receiver, String name, Object... args) {
        return Ops.invoke(realm, v(receiver), name, vs(args));
    }

    /**
     * Calls a prototype method with an explicit this value: <code>proto[name].call(thisValue, ...args)</code>.
     * <p>
     * 명시한 this 값으로 프로토타입 메서드를 호출한다: <code>proto[name].call(thisValue, ...args)</code>.
     *
     * @param proto     String.prototype or Array.prototype / String.prototype 또는 Array.prototype
     * @param name      method name / 메서드 이름
     * @param thisValue this value / this 값
     * @param args      arguments / 인수
     * @return the result / 결과
     */
    public JsValue callOn(JsObject proto, String name, Object thisValue, Object... args) {
        return Ops.call(proto.get(name), v(thisValue), vs(args));
    }

    /**
     * Returns the global object property <code>name</code>.
     * <p>
     * 전역 객체의 프로퍼티 <code>name</code> 을 돌려준다.
     *
     * @param name property name / 프로퍼티 이름
     * @return the object / 객체
     */
    public JsObject global(String name) {
        return (JsObject) realm.getGlobalObject().get(name);
    }

    /**
     * Creates a JavaScript function from a Java callback.
     * <p>
     * Java 콜백으로 JavaScript 함수를 만든다.
     *
     * @param c the behavior / 동작
     * @return the function / 함수
     */
    public NativeFunction fn(NativeFunction.Call c) {
        return realm.newFunction("f", 0, c);
    }


    /**
     * Converts a number value to a Java long for exact integer assertions.
     * <p>
     * 정수 단언을 위해 숫자 값을 Java long 으로 바꾼다.
     *
     * @param value the value / 값
     * @return the integer part / 정수 부분
     */
    public static long i(JsValue value) {
        return (long) Ops.toNumber(value);
    }
    /**
     * Converts the result to a Java string with ToString.
     * <p>
     * 결과를 ToString 으로 Java 문자열로 바꾼다.
     *
     * @param value the value / 값
     * @return the string / 문자열
     */
    public static String str(JsValue value) {
        return Ops.toString(value);
    }

    /**
     * Returns the elements of an array-like as strings (undefined as "undefined", holes as "&lt;hole&gt;").
     * <p>
     * 유사 배열의 요소를 문자열 목록으로 돌려준다(undefined 는 "undefined", 구멍은 "&lt;hole&gt;").
     *
     * @param value the array / 배열
     * @return element strings / 요소 문자열
     */
    public static List<String> list(JsValue value) {
        JsObject o = (JsObject) value;
        double len = Ops.lengthOf(o);
        List<String> out = new ArrayList<String>();
        // 인덱스마다 구멍인지 확인하고 요소를 문자열로 바꿔 모은다.
        for (double k = 0; k < len; k++) {
            String key = PropertyKeys.fromIndex((long) k);
            out.add(o.hasProperty(key) ? describe(o.get(key)) : "<hole>");
        }
        return out;
    }

    /**
     * Short text of a value for assertions: strings are returned as they are, other primitives with ToString.
     * <p>
     * 단언용 짧은 문자열: 문자열은 그대로, 다른 원시 값은 ToString.
     *
     * @param value the value / 값
     * @return the text / 문자열
     */
    public static String describe(JsValue value) {
        if (value instanceof JsObject) {
            return JsException.describe(value);
        }
        if (value instanceof JsString || value instanceof JsNumber || value instanceof JsBoolean) {
            return Ops.toString(value);
        }
        return value.isUndefined() ? "undefined" : (value.isNull() ? "null" : value.toString());
    }

    /**
     * Runs a block and returns the kind of the JavaScript error it throws, failing if it does not throw a {@link JsException}.
     * <p>
     * 블록을 실행하고 그것이 던지는 JavaScript 오류의 종류를 돌려준다. {@link JsException} 을 던지지 않으면 실패한다.
     *
     * @param block code to run / 실행할 코드
     * @return the error kind / 오류 종류
     */
    public static ErrorKind errorOf(Block block) {
        try {
            block.run();
        } catch (JsException e) {
            return e.getKind();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Check.fail("expected a JavaScript error but nothing was thrown");
        return null;
    }
}
