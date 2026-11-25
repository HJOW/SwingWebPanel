package org.duckdns.hjow.swingwebpanel.js;

import java.util.ArrayList;
import java.util.List;

/**
 * The iterator abstract operations of ECMA-262 6th edition 7.4 (GetIterator, IteratorNext, IteratorStep, IteratorClose,
 * CreateIterResultObject) and a convenience to drain an iterable into a Java list. Built-ins that consume iterables
 * (Array.from, Map, Set, Promise.all, spread ...) and the interpreter (for-of, destructuring, spread) use these methods so
 * that the protocol, including closing the iterator on early exit, is implemented exactly once.
 * <p>
 * ECMA-262 6판 7.4 의 이터레이터 추상 연산(GetIterator, IteratorNext, IteratorStep, IteratorClose, CreateIterResultObject)과
 * 반복 가능 객체를 Java 목록으로 모으는 편의 메서드. 반복 가능 객체를 소비하는 내장 객체(Array.from, Map, Set, Promise.all, 전개 ...)와
 * 인터프리터(for-of, 구조 분해, 전개)가 이 메서드를 쓰므로, 조기 종료 시 이터레이터를 닫는 것을 포함한 프로토콜이 한 곳에만 구현된다.
 */
public final class IteratorHelper {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private IteratorHelper() {
    }

    /**
     * GetIterator (7.4.1): calls the value's @@iterator method and checks that the result is an object.
     * <p>
     * GetIterator (7.4.1): 값의 @@iterator 메서드를 호출하고 결과가 객체인지 확인한다.
     *
     * @param realm    the realm / realm
     * @param iterable the iterable value / 반복 가능한 값
     * @return the iterator object / 이터레이터 객체
     */
    public static JsObject getIterator(JsRealm realm, JsValue iterable) {
        JsValue method = Ops.getMethod(realm, iterable, WellKnownSymbols.ITERATOR);
        if (method.isUndefined()) {
            throw JsException.typeError(JsException.describe(iterable) + " is not iterable");
        }
        return getIteratorFromMethod(iterable, method);
    }

    /**
     * GetIterator with a known @@iterator method.
     * <p>
     * @@iterator 메서드를 이미 알고 있는 경우의 GetIterator.
     *
     * @param iterable the iterable value / 반복 가능한 값
     * @param method   the @@iterator function / @@iterator 함수
     * @return the iterator object / 이터레이터 객체
     */
    public static JsObject getIteratorFromMethod(JsValue iterable, JsValue method) {
        JsValue it = Ops.call(method, iterable, Js.NO_ARGS);
        if (!(it instanceof JsObject)) {
            throw JsException.typeError("Result of the Symbol.iterator method is not an object");
        }
        return (JsObject) it;
    }

    /**
     * IteratorNext (7.4.2): calls <code>iterator.next(value)</code> and checks that the result is an object.
     * <p>
     * IteratorNext (7.4.2): <code>iterator.next(value)</code> 를 호출하고 결과가 객체인지 확인한다.
     *
     * @param iterator the iterator / 이터레이터
     * @param value    the value sent to next, or null to send no argument / next 에 보낼 값. 인수를 보내지 않으려면 null
     * @return the iterator result object / 이터레이터 결과 객체
     */
    public static JsObject next(JsObject iterator, JsValue value) {
        return next(iterator, iterator.get("next"), value);
    }

    /**
     * Gets the next object with a method captured when the iterator was obtained.
     * <p>
     * 이터레이터를 만든 시점의 메서드로 다음 객체를 얻는다.
     *
     * @param iterator the iterator object / 이터레이터 객체
     * @param nextMethod captured next method / 미리 읽은 next 메서드
     * @param value optional input, null when absent / 선택적 입력. 생략하면 null
     * @return iterator result / 이터레이터 결과
     */
    static JsObject next(JsObject iterator, JsValue nextMethod, JsValue value) {
        JsValue r = Ops.call(nextMethod, iterator, value == null ? Js.NO_ARGS : new JsValue[] { value });
        if (!(r instanceof JsObject)) {
            throw JsException.typeError("Iterator result " + JsException.describe(r) + " is not an object");
        }
        return (JsObject) r;
    }

    /**
     * IteratorComplete (7.4.3): ToBoolean of the result's done property.
     * <p>
     * IteratorComplete (7.4.3): 결과의 done 프로퍼티에 ToBoolean 을 적용한다.
     *
     * @param result the iterator result object / 이터레이터 결과 객체
     * @return true if the iterator is finished / 이터레이터가 끝났으면 true
     */
    public static boolean isDone(JsObject result) {
        return Ops.toBoolean(result.get("done"));
    }

    /**
     * IteratorValue (7.4.4): the result's value property.
     * <p>
     * IteratorValue (7.4.4): 결과의 value 프로퍼티.
     *
     * @param result the iterator result object / 이터레이터 결과 객체
     * @return the value / 값
     */
    public static JsValue value(JsObject result) {
        return result.get("value");
    }

    /**
     * IteratorStep (7.4.5): advances the iterator and returns the result object, or null when the iterator is done.
     * <p>
     * IteratorStep (7.4.5): 이터레이터를 한 걸음 진행하고 결과 객체를 반환한다. 끝났으면 null.
     *
     * @param iterator the iterator / 이터레이터
     * @return the result object or null if done / 결과 객체. 끝났으면 null
     */
    public static JsObject step(JsObject iterator) {
        JsObject r = next(iterator, null);
        return isDone(r) ? null : r;
    }

    /**
     * IteratorClose (7.4.6) for a normal completion: calls the iterator's return method if it has one and checks its result.
     * <p>
     * 정상 완료에 대한 IteratorClose (7.4.6): 이터레이터에 return 메서드가 있으면 호출하고 결과를 확인한다.
     *
     * @param iterator the iterator / 이터레이터
     */
    public static void close(JsObject iterator) {
        JsValue ret = Ops.getMethod(iterator, "return");
        if (ret.isUndefined()) {
            return;
        }
        JsValue r = Ops.call(ret, iterator, Js.NO_ARGS);
        if (!(r instanceof JsObject)) {
            throw JsException.typeError("Iterator return() result is not an object");
        }
    }

    /**
     * IteratorClose (7.4.6) for a throw completion: calls return but ignores any error it raises, so the original exception
     * can be rethrown by the caller.
     * <p>
     * throw 완료에 대한 IteratorClose (7.4.6): return 을 호출하되 그 안에서 난 오류는 무시하여 호출자가 원래 예외를 다시 던질 수 있게 한다.
     *
     * @param iterator the iterator / 이터레이터
     */
    public static void closeQuietly(JsObject iterator) {
        try {
            JsValue ret = Ops.getMethod(iterator, "return");
            if (!ret.isUndefined()) {
                Ops.call(ret, iterator, Js.NO_ARGS);
            }
        } catch (JsException ignored) {
            // throw 완료에서는 return 의 오류를 버리고 원래 예외가 우선한다.
        }
    }

    /**
     * CreateIterResultObject (7.4.7): <code>{ value, done }</code> with both properties enumerable and writable.
     * <p>
     * CreateIterResultObject (7.4.7): <code>{ value, done }</code>. 두 프로퍼티 모두 열거 가능하고 쓸 수 있다.
     *
     * @param realm the realm / realm
     * @param value the value / 값
     * @param done  the done flag / done 플래그
     * @return the result object / 결과 객체
     */
    public static JsObject createResult(JsRealm realm, JsValue value, boolean done) {
        JsObject r = realm.newObject();
        r.createDataProperty("value", value);
        r.createDataProperty("done", JsBoolean.of(done));
        return r;
    }

    /**
     * Drains an iterable into a Java list. If reading fails midway the iterator is closed quietly and the error is rethrown.
     * <p>
     * 반복 가능 객체를 Java 목록으로 모은다. 중간에 읽기가 실패하면 이터레이터를 조용히 닫고 오류를 다시 던진다.
     *
     * @param realm    the realm / realm
     * @param iterable the iterable value / 반복 가능한 값
     * @return the produced values / 생성된 값들
     */
    public static List<JsValue> toList(JsRealm realm, JsValue iterable) {
        JsObject it = getIterator(realm, iterable);
        List<JsValue> out = new ArrayList<JsValue>();
        // 이터레이터가 끝날 때까지 값을 모은다.
        while (true) {
            JsObject r = step(it);
            if (r == null) {
                return out;
            }
            out.add(value(r));
        }
    }
}
