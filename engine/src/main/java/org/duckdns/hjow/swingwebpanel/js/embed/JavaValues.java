package org.duckdns.hjow.swingwebpanel.js.embed;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
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
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;

/**
 * The conversions between Java values and script values that {@link JsEngine} documents. Only the types listed there can cross the border;
 * any other Java object is refused with an <code>IllegalArgumentException</code>, so a script can never get hold of an arbitrary Java object,
 * its class or its methods. The callers lock the engine.
 * <p>
 * {@link JsEngine} 이 문서화한 Java 값과 스크립트 값 사이의 변환. 거기 적힌 타입만 경계를 넘을 수 있고, 다른 Java 객체는
 * <code>IllegalArgumentException</code> 으로 거부하므로 스크립트가 임의의 Java 객체나 그 클래스, 메서드를 얻을 수 없다. 호출자가 엔진을 잠근다.
 */
final class JavaValues {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private JavaValues() {
    }

    /**
     * Converts a Java value to a script value.
     * <p>
     * Java 값을 스크립트 값으로 바꾼다.
     *
     * @param engine the engine that receives the value / 값을 받는 엔진
     * @param value  the Java value / Java 값
     * @return the script value / 스크립트 값
     * @throws IllegalArgumentException if the type cannot cross the border / 경계를 넘을 수 없는 타입이면
     */
    static JsValue toJs(JsEngine engine, Object value) {
        return toJs(engine, value, "", new IdentityHashMap<Object, JsObject>());
    }

    /**
     * Converts a Java value to a script value, naming a host function after the key it is stored under.
     * <p>
     * Java 값을 스크립트 값으로 바꾼다. 호스트 함수는 저장되는 키를 이름으로 삼는다.
     *
     * @param engine   the engine that receives the value / 값을 받는 엔진
     * @param value    the Java value / Java 값
     * @param funcName the name for a host function / 호스트 함수에 줄 이름
     * @return the script value / 스크립트 값
     */
    static JsValue toJs(JsEngine engine, Object value, String funcName) {
        return toJs(engine, value, funcName, new IdentityHashMap<Object, JsObject>());
    }

    /**
     * Converts a Java value to a script value; containers that were already converted in the same call are reused, so cyclic structures keep
     * their shape.
     * <p>
     * Java 값을 스크립트 값으로 바꾼다. 같은 호출에서 이미 바꾼 컨테이너는 재사용하므로 순환 구조도 모양을 유지한다.
     *
     * @param engine   the engine / 엔진
     * @param v        the Java value / Java 값
     * @param funcName the name for a host function / 호스트 함수에 줄 이름
     * @param seen     the containers converted so far / 지금까지 바꾼 컨테이너
     * @return the script value / 스크립트 값
     */
    private static JsValue toJs(JsEngine engine, Object v, String funcName, IdentityHashMap<Object, JsObject> seen) {
        if (v == null) {
            return Js.NULL;
        }
        if (v == JsEngine.UNDEFINED) {
            return Js.UNDEFINED;
        }
        if (v instanceof JsValue) {
            return (JsValue) v;
        }
        if (v instanceof JsHandle) {
            return ((JsHandle) v).getJsObject();
        }
        if (v instanceof Boolean) {
            return Js.bool(((Boolean) v).booleanValue());
        }
        if (v instanceof Character || v instanceof CharSequence || v instanceof Enum) {
            return Js.str(v instanceof Enum ? ((Enum<?>) v).name() : v.toString());
        }
        if (v instanceof BigInteger) {
            return JsBigInt.of((BigInteger) v);
        }
        if (v instanceof Number) {
            return Js.num(((Number) v).doubleValue());
        }
        if (v instanceof JsHostFunction) {
            return hostFunction(engine, funcName, (JsHostFunction) v);
        }
        JsObject known = seen.get(v);
        if (known != null) {
            return known;
        }
        JsRealm realm = engine.getRealm();
        if (v instanceof Map) {
            JsObject o = realm.newObject();
            seen.put(v, o);
            // 맵의 항목마다 키를 문자열로 바꿔 프로퍼티로 만든다.
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                String key = String.valueOf(e.getKey());
                o.createDataPropertyOrThrow(key, toJs(engine, e.getValue(), key, seen));
            }
            return o;
        }
        if (v instanceof Collection) {
            return arrayOf(engine, ((Collection<?>) v).toArray(), seen, v);
        }
        if (v.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(v);
            Object[] items = new Object[n];
            // 배열(기본형 배열 포함)의 요소를 하나씩 꺼내 Object 배열로 옮긴다.
            for (int i = 0; i < n; i++) {
                items[i] = java.lang.reflect.Array.get(v, i);
            }
            return arrayOf(engine, items, seen, v);
        }
        throw new IllegalArgumentException("a Java object of type " + v.getClass().getName()
                + " cannot be given to a script (only null, JsEngine.UNDEFINED, Boolean, Number (BigInteger is BigInt), CharSequence, Character, Enum, Map, Collection, arrays,"
                + " JsHostFunction, JsHandle and JsValue can cross to a script)");
    }

    /**
     * Creates a script array from the items of a Java collection or array.
     * <p>
     * Java 컬렉션이나 배열의 요소로 스크립트 배열을 만든다.
     *
     * @param engine the engine / 엔진
     * @param items  the items / 요소
     * @param seen   the containers converted so far / 지금까지 바꾼 컨테이너
     * @param origin the Java container that is registered as converted / 바꾼 것으로 등록할 Java 컨테이너
     * @return the script array / 스크립트 배열
     */
    private static JsObject arrayOf(JsEngine engine, Object[] items, IdentityHashMap<Object, JsObject> seen, Object origin) {
        JsObject array = engine.getRealm().newArray();
        seen.put(origin, array);
        // 요소를 하나씩 변환해 배열 끝에 붙인다.
        for (int i = 0; i < items.length; i++) {
            array.createDataPropertyOrThrow(PropertyKeys.fromIndex(i), toJs(engine, items[i], "", seen));
        }
        return array;
    }

    /**
     * Wraps a host function as a script function.
     * <p>
     * 호스트 함수를 스크립트 함수로 감싼다.
     *
     * @param engine the engine / 엔진
     * @param name   the name of the function / 함수 이름
     * @param fn     the host function / 호스트 함수
     * @return the script function / 스크립트 함수
     */
    static NativeFunction hostFunction(final JsEngine engine, String name, final JsHostFunction fn) {
        return engine.getRealm().newFunction(name == null ? "" : name, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                Object[] javaArgs = new Object[args.length];
                // 스크립트가 넘긴 인수를 Java 값으로 바꾼다.
                for (int i = 0; i < args.length; i++) {
                    javaArgs[i] = toJava(engine, args[i]);
                }
                Object result;
                try {
                    result = fn.call(toJava(engine, thisValue), javaArgs);
                } catch (JsScriptError e) {
                    throw e.getJsException() != null ? e.getJsException() : new JsException(ErrorKind.ERROR, e.getMessage());
                } catch (JsException e) {
                    throw e;
                } catch (ScriptAbortedException e) {
                    throw e;
                } catch (Exception e) {
                    throw new JsException(ErrorKind.ERROR, e.toString()).withHostCause(e);
                }
                try {
                    return toJs(engine, result);
                } catch (IllegalArgumentException e) {
                    throw JsException.typeError(e.getMessage());
                }
            }
        });
    }

    /**
     * Converts a script value to a Java value.
     * <p>
     * 스크립트 값을 Java 값으로 바꾼다.
     *
     * @param engine the engine that owns the value / 값을 가진 엔진
     * @param v      the script value / 스크립트 값
     * @return the Java value / Java 값
     */
    static Object toJava(JsEngine engine, JsValue v) {
        if (v == null) {
            return null;
        }
        switch (v.type()) {
        case UNDEFINED:
            return JsEngine.UNDEFINED;
        case NULL:
            return null;
        case BOOLEAN:
            return Boolean.valueOf(((JsBoolean) v).getValue());
        case NUMBER:
            return Double.valueOf(((JsNumber) v).getValue());
        case BIGINT:
            return ((JsBigInt) v).getValue();
        case STRING:
            return ((JsString) v).getValue();
        case SYMBOL:
            return v.toString();
        default:
            return handle(engine, (JsObject) v);
        }
    }

    /**
     * Creates the handle that fits a script object: a function handle, an array handle or an object handle.
     * <p>
     * 스크립트 객체에 맞는 핸들(함수, 배열, 객체)을 만든다.
     *
     * @param engine the engine / 엔진
     * @param o      the script object / 스크립트 객체
     * @return the handle / 핸들
     */
    static JsHandle handle(JsEngine engine, JsObject o) {
        if (o.isCallable()) {
            return new JsFunctionHandle(engine, o);
        }
        if (Ops.isArray(o)) {
            return new JsArrayHandle(engine, o);
        }
        return new JsObjectHandle(engine, o);
    }

    /**
     * Converts a script value to a Java value like {@link #toJava(JsEngine, JsValue)} but copies arrays and objects deeply into
     * <code>List</code> and <code>Map</code> objects (functions stay handles).
     * <p>
     * 스크립트 값을 {@link #toJava(JsEngine, JsValue)} 처럼 바꾸되 배열과 객체는 <code>List</code> 와 <code>Map</code> 으로 깊게 복사한다(함수는 핸들로 남는다).
     *
     * @param engine the engine / 엔진
     * @param v      the script value / 스크립트 값
     * @param seen   the objects copied so far / 지금까지 복사한 객체
     * @return the plain Java value / 순수 Java 값
     */
    static Object toPlain(JsEngine engine, JsValue v, IdentityHashMap<JsObject, Object> seen) {
        if (!(v instanceof JsObject) || v.isCallable()) {
            return toJava(engine, v);
        }
        JsObject o = (JsObject) v;
        Object known = seen.get(o);
        if (known != null) {
            return known;
        }
        if (Ops.isArray(o)) {
            List<Object> list = new ArrayList<Object>();
            seen.put(o, list);
            int n = (int) Ops.lengthOf(o);
            // 배열 요소를 하나씩 깊게 복사한다.
            for (int i = 0; i < n; i++) {
                list.add(toPlain(engine, o.get(PropertyKeys.fromIndex(i)), seen));
            }
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        seen.put(o, map);
        List<String> keys = Ops.enumerableOwnKeys(o);
        // 열거 가능한 고유 문자열 키마다 값을 깊게 복사한다.
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), toPlain(engine, o.get(keys.get(i)), seen));
        }
        return map;
    }

    /**
     * Converts a script value to the Java type that a Java interface method declares as its result type.
     * <p>
     * 스크립트 값을 Java 인터페이스 메서드가 결과 타입으로 선언한 Java 타입으로 바꾼다.
     *
     * @param engine the engine / 엔진
     * @param v      the script value / 스크립트 값
     * @param type   the wanted type / 원하는 타입
     * @return the converted value / 바뀐 값
     * @throws ClassCastException if the value cannot be converted / 바꿀 수 없으면
     */
    static Object coerce(JsEngine engine, JsValue v, Class<?> type) {
        if (type == void.class || type == Void.class) {
            return null;
        }
        if (type == Object.class) {
            return toJava(engine, v);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(Ops.toBoolean(v));
        }
        if (type == String.class || type == CharSequence.class) {
            return v.isNullOrUndefined() ? null : Ops.toString(v);
        }
        if (type == char.class || type == Character.class) {
            String s = Ops.toString(v);
            return Character.valueOf(s.length() == 0 ? (char) 0 : s.charAt(0));
        }
        if (type == double.class || type == Double.class || type == Number.class) {
            return Double.valueOf(Ops.toNumber(v));
        }
        if (type == BigInteger.class) {
            return Ops.toBigInt(v);
        }
        if (type == float.class || type == Float.class) {
            return Float.valueOf((float) Ops.toNumber(v));
        }
        if (type == long.class || type == Long.class) {
            return Long.valueOf((long) Ops.toInteger(v));
        }
        if (type == int.class || type == Integer.class) {
            return Integer.valueOf(Ops.toInt32(v));
        }
        if (type == short.class || type == Short.class) {
            return Short.valueOf((short) Ops.toInt32(v));
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.valueOf((byte) Ops.toInt32(v));
        }
        if (v.isNullOrUndefined() && !type.isPrimitive()) {
            return null;
        }
        Object j = toJava(engine, v);
        if (type.isInstance(j)) {
            return j;
        }
        // 값이 객체이고 원하는 타입이 인터페이스이면 객체의 프로퍼티(또는 함수 자신)로 인터페이스를 구현하는 프록시를 만든다.
        if (type.isInterface() && v instanceof JsObject) {
            return InterfaceProxy.create(engine, (JsObject) v, type, false);
        }
        throw new ClassCastException("cannot convert the script value " + describe(v) + " to " + type.getName());
    }

    /**
     * Describes a script value for messages without running script code.
     * <p>
     * 스크립트 코드를 실행하지 않고 메시지에 쓸 스크립트 값의 설명을 만든다.
     *
     * @param v the value / 값
     * @return the description / 설명
     */
    static String describe(JsValue v) {
        return v instanceof JsObject ? JsException.describe(v) : String.valueOf(v);
    }
}
