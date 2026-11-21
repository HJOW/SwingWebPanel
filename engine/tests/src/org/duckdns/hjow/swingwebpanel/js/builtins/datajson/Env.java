package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Test environment: a realm with the data library installed and short helpers to read properties by dotted path, call methods,
 * construct objects, build iterables and expect JavaScript errors.
 * <p>
 * 시험 환경: 데이터 라이브러리를 설치한 realm 과 점으로 구분한 경로로 프로퍼티를 읽고, 메서드를 호출하고, 객체를 만들고, 이터러블을 만들고,
 * JavaScript 오류를 기대하는 짧은 도우미.
 */
final class Env {

    /**
     * The realm under test.
     * <p>
     * 시험 대상 realm.
     */
    final JsRealm realm;

    /**
     * Creates an environment with a fresh realm.
     * <p>
     * 새 realm 으로 환경을 만든다.
     */
    Env() {
        realm = new JsRealm();
        new DataLibInstaller().install(realm);
    }

    /**
     * Reads a property by dotted path starting at the global object ("Date.prototype.getTime").
     * <p>
     * 전역 객체에서 시작하는 점 경로로 프로퍼티를 읽는다("Date.prototype.getTime").
     *
     * @param path the path / 경로
     * @return the value / 값
     */
    JsValue get(String path) {
        JsValue cur = realm.getGlobalObject();
        String[] parts = path.split("\\.");
        // 경로의 조각마다 한 단계씩 내려간다.
        for (int i = 0; i < parts.length; i++) {
            cur = ((JsObject) cur).get(parts[i]);
        }
        return cur;
    }

    /**
     * Reads an object by dotted path.
     * <p>
     * 점 경로로 객체를 읽는다.
     *
     * @param path the path / 경로
     * @return the object / 객체
     */
    JsObject obj(String path) {
        return (JsObject) get(path);
    }

    /**
     * Constructs an object with a global constructor.
     * <p>
     * 전역 생성자로 객체를 만든다.
     *
     * @param ctor global constructor name / 전역 생성자 이름
     * @param args arguments / 인수
     * @return the new object / 새 객체
     */
    JsObject construct(String ctor, JsValue... args) {
        return Ops.construct(get(ctor), args);
    }

    /**
     * Calls a method of a value.
     * <p>
     * 값의 메서드를 호출한다.
     *
     * @param target receiver / 수신 객체
     * @param method method name / 메서드 이름
     * @param args   arguments / 인수
     * @return the result / 결과
     */
    JsValue invoke(JsValue target, String method, JsValue... args) {
        return Ops.invoke(realm, target, method, args);
    }

    /**
     * Calls a function by path with a receiver.
     * <p>
     * 경로로 찾은 함수를 수신 객체와 함께 호출한다.
     *
     * @param path     function path / 함수 경로
     * @param receiver this value / this 값
     * @param args     arguments / 인수
     * @return the result / 결과
     */
    JsValue call(String path, JsValue receiver, JsValue... args) {
        return Ops.call(get(path), receiver, args);
    }

    /**
     * Makes a number value.
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
     * Makes a string value.
     * <p>
     * 문자열 값을 만든다.
     *
     * @param s the text / 문자열
     * @return the value / 값
     */
    static JsValue s(String s) {
        return Js.str(s);
    }

    /**
     * Converts a value to a Java double.
     * <p>
     * 값을 Java double 로 바꾼다.
     *
     * @param v the value / 값
     * @return the number / 숫자
     */
    static double d(JsValue v) {
        return ((JsNumber) v).getValue();
    }

    /**
     * Converts a value to a Java string.
     * <p>
     * 값을 Java 문자열로 바꾼다.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    static String str(JsValue v) {
        return ((JsString) v).getValue();
    }

    /**
     * Checks that code throws a JavaScript error of a kind.
     * <p>
     * 코드가 주어진 종류의 JavaScript 오류를 던지는지 확인한다.
     *
     * @param kind    expected kind / 기대하는 종류
     * @param block   the code / 코드
     * @param message failure text / 실패 메시지
     * @return the thrown exception / 던져진 예외
     */
    static JsException expect(final ErrorKind kind, Block block, String message) {
        JsException e = (JsException) Check.throwsException(JsException.class, block, message);
        Check.equal(kind, e.getKind(), message + " (error kind)");
        return e;
    }

    /**
     * Creates an array-like iterable of the given values: an object with a [Symbol.iterator] method that returns a fresh iterator over a copy.
     * <p>
     * 주어진 값들의 이터러블을 만든다: 복사본을 도는 새 이터레이터를 돌려주는 [Symbol.iterator] 메서드를 가진 객체.
     *
     * @param values the values / 값들
     * @return the iterable / 이터러블
     */
    JsObject iterable(final JsValue... values) {
        JsObject it = realm.newObject();
        realm.method(it, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over the values.
             * <p>
             * 값들을 도는 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue receiver / 수신 객체
             * @param args      arguments / 인수
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                final int[] pos = new int[1];
                JsObject iterator = r.newObject();
                r.method(iterator, "next", 0, new NativeFunction.Call() {
                    /**
                     * Returns the next iterator result.
                     * <p>
                     * 다음 이터레이터 결과를 반환한다.
                     *
                     * @param r2         realm / realm
                     * @param thisValue2 receiver / 수신 객체
                     * @param args2      arguments / 인수
                     * @return the result object / 결과 객체
                     */
                    public JsValue call(JsRealm r2, JsValue thisValue2, JsValue[] args2) {
                        JsObject result = r2.newObject();
                        boolean done = pos[0] >= values.length;
                        result.createDataProperty("value", done ? Js.UNDEFINED : values[pos[0]++]);
                        result.createDataProperty("done", Js.bool(done));
                        return result;
                    }
                });
                return iterator;
            }
        });
        return it;
    }


    /**
     * Calls a symbol-keyed method of an object.
     * <p>
     * 객체의 심볼 키 메서드를 호출한다.
     *
     * @param target receiver / 수신 객체
     * @param key    the symbol key / 심볼 키
     * @param args   arguments / 인수
     * @return the result / 결과
     */
    JsValue invokeSymbol(JsObject target, Object key, JsValue... args) {
        return Ops.call(target.get(key), target, args);
    }

    /**
     * Reads an array-like value as a list of strings.
     * <p>
     * 배열 유사 값을 문자열 목록으로 읽는다.
     *
     * @param arrayLike the array-like object / 배열 유사 객체
     * @return strings (undefined elements become the text "undefined") / 문자열 목록(undefined 요소는 "undefined" 문자열)
     */
    static java.util.List<String> list(JsValue arrayLike) {
        JsObject o = (JsObject) arrayLike;
        java.util.List<String> out = new java.util.ArrayList<String>();
        int n = (int) Ops.lengthOf(o);
        // 요소를 하나씩 문자열로 바꿔 목록에 담는다.
        for (int i = 0; i < n; i++) {
            JsValue v = o.get(String.valueOf(i));
            out.add(v.isUndefined() ? "undefined" : Ops.toString(v));
        }
        return out;
    }
    /**
     * Creates a two-element array-like object [a, b] (a pair for Map constructor tests).
     * <p>
     * 요소가 둘인 배열 유사 객체 [a, b]를 만든다(Map 생성자 시험용 쌍).
     *
     * @param a first element / 첫 요소
     * @param b second element / 둘째 요소
     * @return the pair / 쌍
     */
    JsObject pair(JsValue a, JsValue b) {
        return realm.newArray(new JsValue[] { a, b });
    }
}
