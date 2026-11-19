package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsChars;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;

/**
 * Shared helpers of the String and Array built-ins: generic element access through the object's internal methods (so that array-likes,
 * holes, accessors and prototype chains behave as the specification says), relative index arithmetic, argument checks and the size
 * limits of this implementation.
 * <p>
 * Limits: strings are limited to {@link #MAX_STRING_LENGTH} UTF-16 code units (the same default as the execution limits; exceeding it is a
 * RangeError "Invalid string length"). Array lengths follow the specification: arrays up to 2<sup>32</sup>-1 elements, other
 * array-likes up to 2<sup>53</sup>-1. Operations that must visit every index take time linear in the length; very long running
 * loops can be stopped by interrupting the thread (see {@link #poll()}).
 * <p>
 * String 과 Array 내장 객체가 공유하는 도우미: 객체의 내부 메서드를 통한 일반 요소 접근(유사 배열, 구멍, 접근자, 프로토타입 체인이 규격대로 동작하게
 * 함), 상대 인덱스 계산, 인수 검사, 이 구현의 크기 제한.
 * <p>
 * 한도: 문자열은 {@link #MAX_STRING_LENGTH} UTF-16 코드 단위로 제한한다(실행 제한의 기본값과 같고, 넘으면 RangeError "Invalid string length").
 * 배열 길이는 규격을 따른다: 배열은 2<sup>32</sup>-1 개까지, 그 밖의 유사 배열은 2<sup>53</sup>-1 까지. 모든 인덱스를 방문해야 하는 연산은 길이에
 * 비례하는 시간이 걸리며, 오래 걸리는 반복은 스레드 인터럽트로 멈출 수 있다({@link #poll()} 참조).
 */
final class Support {

    /**
     * Largest string length produced by the built-ins (UTF-16 code units).
     * <p>
     * 내장 객체가 만드는 문자열의 최대 길이(UTF-16 코드 단위).
     */
    static final int MAX_STRING_LENGTH = ExecutionLimits.DEFAULT_MAX_STRING_LENGTH;

    /**
     * 2<sup>53</sup>-1, the largest exact integer length (ToLength limit).
     * <p>
     * 2<sup>53</sup>-1, 정확히 표현되는 가장 큰 정수 길이(ToLength 의 상한).
     */
    static final double MAX_SAFE_INTEGER = 9007199254740991.0;

    /**
     * 2<sup>32</sup>-1, the largest array length.
     * <p>
     * 2<sup>32</sup>-1, 배열의 최대 길이.
     */
    static final double MAX_ARRAY_LENGTH = 4294967295.0;

    /**
     * Number of loop iterations between two interruption checks.
     * <p>
     * 인터럽트 확인 사이의 반복 횟수.
     */
    private static final int POLL_MASK = 0xFFF;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Support() {
    }

    /**
     * Converts an index to the property key string.
     * <p>
     * 인덱스를 프로퍼티 키 문자열로 바꾼다.
     *
     * @param index non-negative integer valued index / 음수가 아닌 정수 인덱스
     * @return the key / 키
     */
    static String key(double index) {
        return PropertyKeys.fromIndex((long) index);
    }

    /**
     * Get(o, ToString(index)) with a fast path for dense own elements of arrays.
     * <p>
     * Get(o, ToString(index)). 배열의 조밀한 고유 요소는 빠른 경로를 쓴다.
     *
     * @param o     object / 객체
     * @param index index / 인덱스
     * @return the value / 값
     */
    static JsValue getAt(JsObject o, double index) {
        if (o instanceof JsArray && index < Integer.MAX_VALUE) {
            JsValue v = ((JsArray) o).getOwnElement((int) index);
            if (v != null) {
                return v;
            }
        }
        return o.get(key(index));
    }

    /**
     * HasProperty(o, ToString(index)) with a fast path for dense own elements of arrays.
     * <p>
     * HasProperty(o, ToString(index)). 배열의 조밀한 고유 요소는 빠른 경로를 쓴다.
     *
     * @param o     object / 객체
     * @param index index / 인덱스
     * @return true if the property exists / 프로퍼티가 있으면 true
     */
    static boolean hasAt(JsObject o, double index) {
        if (o instanceof JsArray && index < Integer.MAX_VALUE && ((JsArray) o).getOwnElement((int) index) != null) {
            return true;
        }
        return o.hasProperty(key(index));
    }

    /**
     * Set(o, ToString(index), value, true): throws a TypeError when the assignment is rejected.
     * <p>
     * Set(o, ToString(index), value, true): 대입이 거부되면 TypeError 를 던진다.
     *
     * @param o     object / 객체
     * @param index index / 인덱스
     * @param value value / 값
     */
    static void setAt(JsObject o, double index, JsValue value) {
        o.setStrict(key(index), value);
    }

    /**
     * DeletePropertyOrThrow(o, ToString(index)).
     * <p>
     * DeletePropertyOrThrow(o, ToString(index)).
     *
     * @param o     object / 객체
     * @param index index / 인덱스
     */
    static void deleteAt(JsObject o, double index) {
        if (!o.delete(key(index))) {
            throw JsException.typeError("Cannot delete property '" + key(index) + "' of " + Ops.toString(Js.str(o.className())));
        }
    }

    /**
     * CreateDataPropertyOrThrow(o, ToString(index), value).
     * <p>
     * CreateDataPropertyOrThrow(o, ToString(index), value).
     *
     * @param o     object / 객체
     * @param index index / 인덱스
     * @param value value / 값
     */
    static void createAt(JsObject o, double index, JsValue value) {
        o.createDataPropertyOrThrow(key(index), value);
    }

    /**
     * Set(o, "length", length, true).
     * <p>
     * Set(o, "length", length, true).
     *
     * @param o      object / 객체
     * @param length new length / 새 길이
     */
    static void setLength(JsObject o, double length) {
        o.setStrict("length", Js.num(length));
    }

    /**
     * Converts a relative index argument (negative counts from the end) to an absolute position clamped into 0..len.
     * <p>
     * 상대 인덱스 인수(음수는 끝에서부터)를 0..len 으로 제한한 절대 위치로 바꾼다.
     *
     * @param arg      the argument / 인수
     * @param len      length / 길이
     * @param defValue value used when the argument is undefined / 인수가 undefined 일 때의 값
     * @return the absolute position / 절대 위치
     */
    static double relative(JsValue arg, double len, double defValue) {
        if (arg.isUndefined()) {
            return defValue;
        }
        double rel = Ops.toInteger(arg);
        return rel < 0 ? Math.max(len + rel, 0) : Math.min(rel, len);
    }

    /**
     * Removes WhiteSpace and LineTerminator characters of the specification from the start and/or the end of a string. The character
     * classes come from the core's explicit table ({@link JsChars#isSpace(int)}) because the JRE 6 character database classifies U+200B
     * and U+180E as spaces, which no ES2015 engine does.
     * <p>
     * 문자열의 앞쪽 그리고/또는 뒤쪽에서 규격의 WhiteSpace 와 LineTerminator 문자를 제거한다. 문자 종류는 코어의 명시 표({@link JsChars#isSpace(int)})를
     * 쓴다. JRE 6 의 문자 데이터베이스는 U+200B 와 U+180E 를 공백으로 분류하지만 어떤 ES2015 엔진도 그렇게 보지 않기 때문이다.
     *
     * @param s     the string / 문자열
     * @param start true to trim the start / 앞쪽을 다듬으려면 true
     * @param end   true to trim the end / 뒤쪽을 다듬으려면 true
     * @return the trimmed string / 다듬은 문자열
     */
    static String trim(String s, boolean start, boolean end) {
        int from = 0;
        int to = s.length();
        // 앞쪽의 공백류를 건너뛴다.
        while (start && from < to && JsChars.isSpace(s.charAt(from))) {
            from++;
        }
        // 뒤쪽의 공백류를 건너뛴다.
        while (end && to > from && JsChars.isSpace(s.charAt(to - 1))) {
            to--;
        }
        return s.substring(from, to);
    }

    /**
     * Throws a TypeError unless the value is callable.
     * <p>
     * 값이 호출 가능하지 않으면 TypeError 를 던진다.
     *
     * @param f    the value / 값
     * @param what description used in the message / 메시지에 쓸 설명
     */
    static void requireCallable(JsValue f, String what) {
        if (!f.isCallable()) {
            throw JsException.typeError(what + " is not a function");
        }
    }

    /**
     * Creates an array of the given length (ArrayCreate); lengths above 2<sup>32</sup>-1 are a RangeError.
     * <p>
     * 주어진 길이의 배열을 만든다(ArrayCreate). 2<sup>32</sup>-1 보다 크면 RangeError 이다.
     *
     * @param realm  the realm / realm
     * @param length the length / 길이
     * @return the array / 배열
     */
    static JsArray arrayCreate(JsRealm realm, double length) {
        if (length > MAX_ARRAY_LENGTH) {
            throw JsException.rangeError("Invalid array length");
        }
        JsArray a = realm.newArray();
        if (length > 0) {
            a.defineOwnProperty("length", PropertyDescriptor.valueOnly(Js.num(length)));
        }
        return a;
    }

    /**
     * Throws a RangeError when a string length exceeds the limit.
     * <p>
     * 문자열 길이가 제한을 넘으면 RangeError 를 던진다.
     *
     * @param length the length that would result / 만들어질 길이
     */
    static void checkStringLength(double length) {
        if (length > MAX_STRING_LENGTH) {
            throw JsException.rangeError("Invalid string length");
        }
    }

    /**
     * Interruption check for long loops: throws a {@link ScriptAbortedException} when the thread was interrupted (page change,
     * dispose, cancellation). Call it with a loop counter; it only looks at the thread every 4096 iterations.
     * <p>
     * 긴 반복을 위한 인터럽트 확인: 스레드가 인터럽트되었으면(페이지 이동, dispose, 취소) {@link ScriptAbortedException} 을 던진다.
     * 반복 횟수와 함께 호출하며 4096 번에 한 번만 스레드를 확인한다.
     *
     * @param counter loop counter / 반복 횟수
     */
    static void poll(double counter) {
        if (((long) counter & POLL_MASK) == POLL_MASK) {
            poll();
        }
    }

    /**
     * Throws a {@link ScriptAbortedException} if the current thread is interrupted.
     * <p>
     * 현재 스레드가 인터럽트되었으면 {@link ScriptAbortedException} 을 던진다.
     */
    static void poll() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ScriptAbortedException(ScriptAbortedException.Reason.CANCELLED, "script cancelled while running a built-in loop");
        }
    }

    /**
     * Defines a built-in method with the given function behavior and returns its function object.
     * <p>
     * 내장 메서드를 정의하고 함수 객체를 돌려준다.
     *
     * @param realm  the realm / realm
     * @param target object receiving the method / 메서드를 받을 객체
     * @param key    String or symbol key / 문자열 또는 심볼 키
     * @param length the function's length / 함수의 length
     * @param call   behavior / 동작
     * @return the function / 함수
     */
    static NativeFunction def(JsRealm realm, JsObject target, Object key, int length, NativeFunction.Call call) {
        return realm.method(target, key, length, call);
    }
}
