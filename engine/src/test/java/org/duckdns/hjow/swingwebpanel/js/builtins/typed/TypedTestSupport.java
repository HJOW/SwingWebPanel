package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the tests of the binary data library: realm creation, construction and method invocation, value conversion and
 * checks for the kind of error that a call throws.
 * <p>
 * 이진 데이터 라이브러리 시험들이 공유하는 도우미: realm 생성, 생성과 메서드 호출, 값 변환, 호출이 던지는 오류 종류 검사.
 */
public final class TypedTestSupport {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedTestSupport() {
    }

    /**
     * Creates a realm that has only the binary data library installed (plus the fundamental objects every realm has).
     * <p>
     * 이진 데이터 라이브러리만 설치된 realm 을 만든다(모든 realm 이 가진 기본 객체는 포함).
     *
     * @return the realm / realm
     */
    public static JsRealm newRealm() {
        JsRealm realm = new JsRealm();
        new TypedLibInstaller().install(realm);
        return realm;
    }

    /**
     * Returns a global constructor by name.
     * <p>
     * 이름으로 전역 생성자를 반환한다.
     *
     * @param realm the realm / realm
     * @param name  the global name / 전역 이름
     * @return the constructor object / 생성자 객체
     */
    public static JsObject ctor(JsRealm realm, String name) {
        JsValue v = realm.getGlobalObject().get(name);
        Check.isTrue(v instanceof JsObject, "global " + name + " must exist");
        return (JsObject) v;
    }

    /**
     * Calls <code>new Name(args)</code>.
     * <p>
     * <code>new Name(args)</code> 를 실행한다.
     *
     * @param realm the realm / realm
     * @param name  the global constructor name / 전역 생성자 이름
     * @param args  the arguments / 인수
     * @return the new object / 새 객체
     */
    public static JsObject make(JsRealm realm, String name, JsValue... args) {
        return Ops.construct(ctor(realm, name), args);
    }

    /**
     * Calls a method of a value: <code>target.method(args)</code>.
     * <p>
     * 값의 메서드를 호출한다: <code>target.method(args)</code>.
     *
     * @param realm  the realm / realm
     * @param target the receiver / 수신자
     * @param method the method name / 메서드 이름
     * @param args   the arguments / 인수
     * @return the result / 결과
     */
    public static JsValue call(JsRealm realm, JsValue target, String method, JsValue... args) {
        return Ops.invoke(realm, target, method, args);
    }

    /**
     * Creates a number value.
     * <p>
     * 숫자 값을 만든다.
     *
     * @param d the number / 숫자
     * @return the value / 값
     */
    public static JsNumber n(double d) {
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
    public static JsString s(String s) {
        return Js.str(s);
    }

    /**
     * Creates an array of numbers.
     * <p>
     * 숫자 배열을 만든다.
     *
     * @param realm the realm / realm
     * @param xs    the numbers / 숫자들
     * @return the array / 배열
     */
    public static JsArray arr(JsRealm realm, double... xs) {
        JsValue[] v = new JsValue[xs.length];
        // 숫자를 JavaScript 값으로 바꿔 배열을 만든다.
        for (int i = 0; i < xs.length; i++) {
            v[i] = Js.num(xs[i]);
        }
        return realm.newArray(v);
    }

    /**
     * Checks that two numbers are the same value (SameValue: NaN equals NaN, +0 differs from -0). Use this instead of
     * <code>Check.equal</code> whenever one side is a double, because mixing int and double there compares boxed objects.
     * <p>
     * 두 숫자가 같은 값인지 검사한다(SameValue: NaN 은 NaN 과 같고 +0 은 -0 과 다르다). 한쪽이 double 이면 <code>Check.equal</code> 대신 이것을 쓴다.
     * 거기서는 int 와 double 을 섞으면 박싱된 객체를 비교하기 때문이다.
     *
     * @param expected the expected number / 기대하는 숫자
     * @param actual   the actual number / 실제 숫자
     * @param message  failure description / 실패 설명
     */
    public static void eq(double expected, double actual, String message) {
        Check.isTrue(Ops.sameValue(Js.num(expected), Js.num(actual)), message + " (expected " + expected + " but was " + actual + ")");
    }

    /**
     * Calls <code>new Name(buffer, numbers...)</code>: a typed array view with byte offset and optional length.
     * <p>
     * <code>new Name(buffer, numbers...)</code> 를 실행한다: 위치와 선택적인 길이를 가진 형식화 배열 뷰.
     *
     * @param realm  the realm / realm
     * @param name   the constructor name / 생성자 이름
     * @param buffer the buffer / 버퍼
     * @param rest   byte offset and optional length / 바이트 위치와 선택적인 길이
     * @return the view / 뷰
     */
    public static JsObject view(JsRealm realm, String name, JsValue buffer, double... rest) {
        JsValue[] args = new JsValue[1 + rest.length];
        args[0] = buffer;
        // 숫자 인수를 JavaScript 값으로 바꿔 뒤에 붙인다.
        for (int i = 0; i < rest.length; i++) {
            args[i + 1] = Js.num(rest[i]);
        }
        return make(realm, name, args);
    }

    /**
     * Creates a typed array of the given kind holding the given numbers: <code>new Name([xs...])</code>.
     * <p>
     * 주어진 숫자를 담은 해당 종류의 형식화 배열을 만든다: <code>new Name([xs...])</code>.
     *
     * @param realm the realm / realm
     * @param name  the constructor name, for example "Uint8Array" / 생성자 이름. 예: "Uint8Array"
     * @param xs    the numbers / 숫자들
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject ta(JsRealm realm, String name, double... xs) {
        return (TypedArrayObject) make(realm, name, arr(realm, xs));
    }

    /**
     * Converts a number value to a Java double.
     * <p>
     * 숫자 값을 Java double 로 바꾼다.
     *
     * @param v the value / 값
     * @return the number / 숫자
     */
    public static double num(JsValue v) {
        Check.isTrue(v instanceof JsNumber, "expected a number but was " + v);
        return ((JsNumber) v).getValue();
    }

    /**
     * Converts a string value to a Java string.
     * <p>
     * 문자열 값을 Java 문자열로 바꾼다.
     *
     * @param v the value / 값
     * @return the text / 문자열
     */
    public static String str(JsValue v) {
        Check.isTrue(v instanceof JsString, "expected a string but was " + v);
        return ((JsString) v).getValue();
    }

    /**
     * Reads the elements of any typed array (or array-like) object as numbers, through the ordinary property protocol.
     * <p>
     * 일반 프로퍼티 규약으로 형식화 배열(또는 유사 배열)의 요소를 숫자 목록으로 읽는다.
     *
     * @param o the object / 객체
     * @return the numbers / 숫자들
     */
    public static double[] elements(JsObject o) {
        int len = (int) Ops.lengthOf(o);
        double[] r = new double[len];
        // 인덱스를 차례로 읽는다.
        for (int i = 0; i < len; i++) {
            r[i] = num(o.get(String.valueOf(i)));
        }
        return r;
    }

    /**
     * Checks that the elements of an array-like object equal the expected numbers (NaN equals NaN, -0 differs from +0).
     * <p>
     * 유사 배열 객체의 요소가 기대한 숫자와 같은지 검사한다(NaN 은 NaN 과 같고 -0 은 +0 과 다르다).
     *
     * @param expected the expected numbers / 기대하는 숫자
     * @param o        the object / 객체
     * @param message  failure description / 실패 설명
     */
    public static void checkElements(double[] expected, JsObject o, String message) {
        double[] actual = elements(o);
        Check.equal(expected.length, actual.length, message + ": length");
        // 요소를 하나씩 SameValue 로 비교한다.
        for (int i = 0; i < expected.length; i++) {
            Check.isTrue(Ops.sameValue(Js.num(expected[i]), Js.num(actual[i])), message + ": element " + i + " expected " + expected[i] + " but was " + actual[i]);
        }
    }

    /**
     * Checks that a block throws a JavaScript error of the given kind.
     * <p>
     * 코드 조각이 주어진 종류의 JavaScript 오류를 던지는지 검사한다.
     *
     * @param kind    the expected error kind / 기대하는 오류 종류
     * @param block   the code / 코드
     * @param message failure description / 실패 설명
     */
    public static void expectError(ErrorKind kind, Block block, String message) {
        try {
            block.run();
        } catch (JsException e) {
            Check.equal(kind, e.getKind(), message + ": error kind (" + e.getMessage() + ")");
            return;
        } catch (Exception e) {
            Check.fail(message + ": unexpected " + e);
        }
        Check.fail(message + ": expected " + kind + " but nothing was thrown");
    }

    /**
     * Creates a native function from a call behavior (for callbacks and custom constructors in tests).
     * <p>
     * 호출 동작으로 네이티브 함수를 만든다(시험의 콜백과 사용자 정의 생성자용).
     *
     * @param realm the realm / realm
     * @param name  the function name / 함수 이름
     * @param len   the length / length
     * @param call  the behavior / 동작
     * @return the function / 함수
     */
    public static NativeFunction fn(JsRealm realm, String name, int len, NativeFunction.Call call) {
        return realm.newFunction(name, len, call);
    }
}
