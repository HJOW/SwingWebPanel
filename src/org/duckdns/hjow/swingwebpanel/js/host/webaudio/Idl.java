package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;

/**
 * The WebIDL conversions that the Web Audio interfaces need: the numeric types (<code>float</code>, <code>double</code> and their unrestricted
 * forms, <code>unsigned long</code>), <code>sequence&lt;float&gt;</code> and <code>sequence&lt;double&gt;</code>, dictionaries, enumerations (strict for
 * arguments and dictionary members, tolerant for attribute assignment, as WebIDL says) and the typed arrays that carry audio data. Every conversion error is a
 * <code>TypeError</code> with a message in the style of the browsers. The class holds no state.
 * <p>
 * Web Audio 인터페이스가 필요로 하는 WebIDL 변환: 숫자 형식(<code>float</code>, <code>double</code>과 그 제한 없는 형식, <code>unsigned long</code>),
 * <code>sequence&lt;float&gt;</code>, <code>sequence&lt;double&gt;</code>, 사전, 열거형(WebIDL 대로 인수와 사전 멤버에는 엄격하고 속성 대입에는 관대함),
 * 오디오 데이터를 나르는 형식화 배열. 모든 변환 오류는 브라우저 형식의 메시지를 가진 <code>TypeError</code> 이다. 이 클래스는 상태가 없다.
 */
final class Idl {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Idl() {
    }

    /**
     * Throws the TypeError for too few arguments, like the browsers do.
     * <p>
     * 브라우저처럼 인수가 모자라면 TypeError 를 던진다.
     *
     * @param args   the arguments / 인수
     * @param needed the number of required arguments / 필수 인수 수
     * @param method the name of the operation / 연산 이름
     * @param iface  the name of the interface / 인터페이스 이름
     */
    static void requireArgs(JsValue[] args, int needed, String method, String iface) {
        if (args.length < needed) {
            throw JsException.typeError("Failed to execute '" + method + "' on '" + iface + "': " + needed + " argument" + (needed == 1 ? "" : "s")
                    + " required, but only " + args.length + " present.");
        }
    }

    /**
     * Throws the TypeError for too few arguments of a constructor.
     * <p>
     * 생성자의 인수가 모자라면 TypeError 를 던진다.
     *
     * @param args   the arguments / 인수
     * @param needed the number of required arguments / 필수 인수 수
     * @param iface  the name of the interface / 인터페이스 이름
     */
    static void requireCtorArgs(JsValue[] args, int needed, String iface) {
        if (args.length < needed) {
            throw JsException.typeError("Failed to construct '" + iface + "': " + needed + " argument" + (needed == 1 ? "" : "s") + " required, but only "
                    + args.length + " present.");
        }
    }

    /**
     * Converts a value to a WebIDL <code>double</code>: ToNumber, and a non-finite result is a TypeError.
     * <p>
     * 값을 WebIDL <code>double</code> 로 바꾼다: ToNumber 이며 유한하지 않은 결과는 TypeError 이다.
     *
     * @param v the value / 값
     * @return the number / 수
     */
    static double dbl(JsValue v) {
        double d = Ops.toNumber(v);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError("The provided double value is non-finite.");
        }
        return d;
    }

    /**
     * Converts a value to a WebIDL <code>float</code>: ToNumber rounded to a single precision number, and a non-finite result (also an overflow) is a
     * TypeError.
     * <p>
     * 값을 WebIDL <code>float</code> 로 바꾼다: 단정밀도로 반올림한 ToNumber 이며, 유한하지 않은 결과(넘침 포함)는 TypeError 이다.
     *
     * @param v the value / 값
     * @return the number / 수
     */
    static float flt(JsValue v) {
        float f = (float) Ops.toNumber(v);
        if (Float.isNaN(f) || Float.isInfinite(f)) {
            throw JsException.typeError("The provided float value is non-finite.");
        }
        return f;
    }

    /**
     * Converts a value to a WebIDL <code>unrestricted float</code>.
     * <p>
     * 값을 WebIDL <code>unrestricted float</code> 로 바꾼다.
     *
     * @param v the value / 값
     * @return the number / 수
     */
    static float uflt(JsValue v) {
        return (float) Ops.toNumber(v);
    }

    /**
     * Converts a value to a WebIDL <code>unsigned long</code> (modulo 2^32) and limits it to the largest <code>int</code>, because every value from
     * 2^31 up is far out of range for every use of it here (a channel, a length, a number of nodes), so the engine rejects them all the same way.
     * <p>
     * 값을 WebIDL <code>unsigned long</code>(2^32 로 나눈 나머지)으로 바꾸고 가장 큰 <code>int</code> 로 제한한다. 2^31 이상의 값은 여기서의 모든 쓰임(채널, 길이,
     * 노드 수)에서 범위를 훨씬 벗어나므로 엔진이 모두 같은 방식으로 거부한다.
     *
     * @param v the value / 값
     * @return the number / 수
     */
    static int ulong(JsValue v) {
        long u = Ops.toUint32(v);
        return u > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) u;
    }

    /**
     * Reads a dictionary argument: undefined and null mean an empty dictionary (null is returned), an object is the dictionary, anything else is a TypeError.
     * <p>
     * 사전 인수를 읽는다: undefined 와 null 은 빈 사전(null 반환), 객체는 사전, 그 밖은 TypeError 이다.
     *
     * @param v    the argument / 인수
     * @param what the description for the message / 메시지에 쓸 설명
     * @return the dictionary, or null for an empty one / 사전. 빈 것이면 null
     */
    static JsObject dictionary(JsValue v, String what) {
        if (v == null || v.isNullOrUndefined()) {
            return null;
        }
        if (!v.isObject()) {
            throw JsException.typeError(what + " is not of type 'dictionary'.");
        }
        return (JsObject) v;
    }

    /**
     * Reads a member of a dictionary (undefined if the dictionary is empty or lacks the member).
     * <p>
     * 사전의 멤버를 읽는다(사전이 비었거나 멤버가 없으면 undefined).
     *
     * @param dict the dictionary or null / 사전. 없으면 null
     * @param name the member / 멤버
     * @return the value / 값
     */
    static JsValue member(JsObject dict, String name) {
        return dict == null ? Js.UNDEFINED : dict.get(name);
    }

    /**
     * Returns the script name of an enumeration constant (the engine enumerations all have <code>getScriptName()</code>).
     * <p>
     * 열거형 상수의 스크립트 이름을 반환한다(엔진의 열거형은 모두 <code>getScriptName()</code> 을 가진다).
     *
     * @param e the constant / 상수
     * @return the name / 이름
     */
    static String enumName(Enum<?> e) {
        try {
            return (String) e.getClass().getMethod("getScriptName").invoke(e);
        } catch (NoSuchMethodException x) {
            throw new IllegalStateException(x);
        } catch (IllegalAccessException x) {
            throw new IllegalStateException(x);
        } catch (InvocationTargetException x) {
            throw new IllegalStateException(x);
        }
    }

    /**
     * Finds the constant of an engine enumeration by its script name.
     * <p>
     * 엔진 열거형의 상수를 스크립트 이름으로 찾는다.
     *
     * @param cls  the enumeration class / 열거형 클래스
     * @param name the script name / 스크립트 이름
     * @return the constant, or null if the name is not one of them / 상수. 이름이 그 중 하나가 아니면 null
     */
    static Object enumByName(Class<?> cls, String name) {
        Object[] all = cls.getEnumConstants();
        // 상수마다 스크립트 이름을 비교한다.
        for (int i = 0; i < all.length; i++) {
            if (enumName((Enum<?>) all[i]).equals(name)) {
                return all[i];
            }
        }
        return null;
    }

    /**
     * Converts a value to a constant of an engine enumeration for an argument or a dictionary member: a value that is not one of the names is a TypeError.
     * <p>
     * 값을 인수나 사전 멤버를 위해 엔진 열거형의 상수로 바꾼다: 이름이 아닌 값은 TypeError 이다.
     *
     * @param cls      the enumeration class / 열거형 클래스
     * @param v        the value / 값
     * @param typeName the WebIDL name of the enumeration for the message / 메시지에 쓸 열거형의 WebIDL 이름
     * @return the constant / 상수
     */
    static Object enumStrict(Class<?> cls, JsValue v, String typeName) {
        String s = Ops.toString(v);
        Object e = enumByName(cls, s);
        if (e == null) {
            throw JsException.typeError("The provided value '" + s + "' is not a valid enum value of type " + typeName + ".");
        }
        return e;
    }

    /**
     * Checks that a string is one of the values of a WebIDL enumeration that the engine does not model (a TypeError otherwise).
     * <p>
     * 문자열이 엔진이 모형으로 두지 않는 WebIDL 열거형의 값 중 하나인지 확인한다(아니면 TypeError).
     *
     * @param value    the string / 문자열
     * @param allowed  the values of the enumeration / 열거형의 값
     * @param typeName the WebIDL name of the enumeration / 열거형의 WebIDL 이름
     * @return the same string / 같은 문자열
     */
    static String stringEnum(String value, String[] allowed, String typeName) {
        // 허용하는 값을 하나씩 비교한다.
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(value)) {
                return value;
            }
        }
        throw JsException.typeError("The provided value '" + value + "' is not a valid enum value of type " + typeName + ".");
    }

    /**
     * Converts a <code>sequence&lt;double&gt;</code>: an array, a typed array or any iterable object; each member must be finite.
     * <p>
     * <code>sequence&lt;double&gt;</code> 를 변환한다: 배열, 형식화 배열 또는 반복 가능한 객체. 모든 멤버는 유한해야 한다.
     *
     * @param realm the realm / realm
     * @param v     the value / 값
     * @param what  the description for the message / 메시지에 쓸 설명
     * @return the numbers / 수
     */
    static double[] doubleSequence(JsRealm realm, JsValue v, String what) {
        JsValue[] items = sequence(realm, v, what);
        double[] out = new double[items.length];
        // 항목마다 유한한 double 로 바꾼다.
        for (int i = 0; i < items.length; i++) {
            out[i] = dbl(items[i]);
        }
        return out;
    }

    /**
     * Converts a <code>sequence&lt;float&gt;</code>: an array, a typed array or any iterable object; each member must be finite as a float.
     * <p>
     * <code>sequence&lt;float&gt;</code> 를 변환한다: 배열, 형식화 배열 또는 반복 가능한 객체. 모든 멤버는 float 로 유한해야 한다.
     *
     * @param realm the realm / realm
     * @param v     the value / 값
     * @param what  the description for the message / 메시지에 쓸 설명
     * @return the numbers / 수
     */
    static float[] floatSequence(JsRealm realm, JsValue v, String what) {
        if (v instanceof TypedArrayObject && !((TypedArrayObject) v).getBuffer().isDetached()) {
            TypedArrayObject t = (TypedArrayObject) v;
            float[] out = new float[t.getLength()];
            // 형식화 배열의 요소를 float 로 읽는다.
            for (int i = 0; i < out.length; i++) {
                float f = (float) t.getElement(i);
                if (Float.isNaN(f) || Float.isInfinite(f)) {
                    throw JsException.typeError("The provided float value is non-finite.");
                }
                out[i] = f;
            }
            return out;
        }
        JsValue[] items = sequence(realm, v, what);
        float[] out = new float[items.length];
        // 항목마다 유한한 float 로 바꾼다.
        for (int i = 0; i < items.length; i++) {
            out[i] = flt(items[i]);
        }
        return out;
    }

    /**
     * Reads the items of a WebIDL sequence argument.
     * <p>
     * WebIDL 시퀀스 인수의 항목을 읽는다.
     *
     * @param realm the realm / realm
     * @param v     the value / 값
     * @param what  the description for the message / 메시지에 쓸 설명
     * @return the items / 항목
     */
    private static JsValue[] sequence(JsRealm realm, JsValue v, String what) {
        if (!v.isObject()) {
            throw JsException.typeError(what + " is not a sequence.");
        }
        JsObject o = (JsObject) v;
        // 배열과 형식화 배열은 길이로 읽고, 그 밖의 객체는 반복자로 읽는다.
        if (Ops.isArray(o) || o instanceof TypedArrayObject) {
            return Ops.createListFromArrayLike(o);
        }
        JsValue method = Ops.getMethod(o, WellKnownSymbols.ITERATOR);
        if (method == null || method.isNullOrUndefined()) {
            throw JsException.typeError(what + " is not iterable.");
        }
        List<JsValue> list = IteratorHelper.toList(realm, o);
        return list.toArray(new JsValue[list.size()]);
    }

    /**
     * Requires a typed array of an element type (a <code>Float32Array</code> argument is such a thing, a plain array is not) whose buffer is not detached.
     * <p>
     * 요소 형식이 정해진 형식화 배열을 요구한다(<code>Float32Array</code> 인수가 그런 것이며 일반 배열은 아니다). 버퍼는 분리되지 않은 것이어야 한다.
     *
     * @param v    the value / 값
     * @param type the element type / 요소 형식
     * @param what the description for the message / 메시지에 쓸 설명
     * @return the array / 배열
     */
    static TypedArrayObject typedArray(JsValue v, ElementType type, String what) {
        if (!(v instanceof TypedArrayObject) || ((TypedArrayObject) v).getType() != type) {
            throw JsException.typeError(what + " is not of type '" + type.getConstructorName() + "'.");
        }
        TypedArrayObject t = (TypedArrayObject) v;
        if (t.getBuffer().isDetached()) {
            throw JsException.typeError(what + " is detached.");
        }
        return t;
    }

    /**
     * Reads the elements of a <code>Float32Array</code> into a new <code>float[]</code>.
     * <p>
     * <code>Float32Array</code> 의 요소를 새 <code>float[]</code> 로 읽는다.
     *
     * @param t the array / 배열
     * @return the copy / 복사본
     */
    static float[] floatsOf(TypedArrayObject t) {
        float[] out = new float[t.getLength()];
        byte[] data = t.getBuffer().getData();
        int base = t.getByteOffset();
        // 요소마다 리틀 엔디언 바이트에서 float 를 읽는다.
        for (int i = 0; i < out.length; i++) {
            out[i] = Float.intBitsToFloat(readInt(data, base + i * 4));
        }
        return out;
    }

    /**
     * Writes floats into the start of a <code>Float32Array</code> (as many as fit).
     * <p>
     * <code>Float32Array</code> 의 앞부분에 float 를 쓴다(들어가는 만큼).
     *
     * @param t     the array / 배열
     * @param src   the values / 값
     * @param count the number of values to write at most / 쓸 최대 개수
     */
    static void storeFloats(TypedArrayObject t, float[] src, int count) {
        byte[] data = t.getBuffer().getData();
        int base = t.getByteOffset();
        int n = Math.min(Math.min(count, src.length), t.getLength());
        // 값마다 리틀 엔디언 바이트로 쓴다.
        for (int i = 0; i < n; i++) {
            writeInt(data, base + i * 4, Float.floatToRawIntBits(src[i]));
        }
    }

    /**
     * Reads a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 읽는다.
     *
     * @param b the bytes / 바이트
     * @param o the offset / 위치
     * @return the integer / 정수
     */
    static int readInt(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
    }

    /**
     * Writes a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 쓴다.
     *
     * @param b the bytes / 바이트
     * @param o the offset / 위치
     * @param v the integer / 정수
     */
    static void writeInt(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >>> 8);
        b[o + 2] = (byte) (v >>> 16);
        b[o + 3] = (byte) (v >>> 24);
    }

    /**
     * Converts <code>float[]</code> channel data to the little endian bytes that a <code>Float32Array</code> shows.
     * <p>
     * <code>float[]</code> 채널 데이터를 <code>Float32Array</code> 가 보여 주는 리틀 엔디언 바이트로 바꾼다.
     *
     * @param src the samples / 표본
     * @return the bytes / 바이트
     */
    static byte[] toBytes(float[] src) {
        byte[] out = new byte[src.length * 4];
        // 표본마다 네 바이트로 쓴다.
        for (int i = 0; i < src.length; i++) {
            writeInt(out, i * 4, Float.floatToRawIntBits(src[i]));
        }
        return out;
    }

    /**
     * Converts the little endian bytes of a <code>Float32Array</code> to a new <code>float[]</code>.
     * <p>
     * <code>Float32Array</code> 의 리틀 엔디언 바이트를 새 <code>float[]</code> 로 바꾼다.
     *
     * @param src the bytes / 바이트
     * @return the samples / 표본
     */
    static float[] fromBytes(byte[] src) {
        float[] out = new float[src.length / 4];
        // 네 바이트마다 표본 하나를 읽는다.
        for (int i = 0; i < out.length; i++) {
            out[i] = Float.intBitsToFloat(readInt(src, i * 4));
        }
        return out;
    }

    /**
     * Returns the Java method of an engine class by name, taking the only public method with that name (and the number of parameters if given).
     * <p>
     * 엔진 클래스의 Java 메서드를 이름으로 찾는다. 그 이름의 공개 메서드가 하나뿐이어야 하며 매개변수 수가 주어지면 그것도 맞아야 한다.
     *
     * @param cls   the class / 클래스
     * @param name  the method name / 메서드 이름
     * @param arity the number of parameters, or -1 for any / 매개변수 수. 상관없으면 -1
     * @return the method / 메서드
     */
    static Method find(Class<?> cls, String name, int arity) {
        Method found = null;
        Method[] all = cls.getMethods();
        // 이름과 매개변수 수가 맞는 공개 메서드를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equals(name) && (arity < 0 || all[i].getParameterTypes().length == arity)) {
                if (found != null) {
                    throw new IllegalArgumentException("ambiguous method " + name + " in " + cls.getName());
                }
                found = all[i];
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("no method " + name + "/" + arity + " in " + cls.getName());
        }
        return found;
    }
}
