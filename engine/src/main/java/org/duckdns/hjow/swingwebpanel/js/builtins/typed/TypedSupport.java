package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Abstract operations shared by the ArrayBuffer, DataView and typed array built-ins: argument conversions (ToIndex, relative indices),
 * argument validation (ValidateTypedArray), SpeciesConstructor, TypedArrayCreate / TypedArraySpeciesCreate and buffer allocation.
 * <p>
 * ArrayBuffer, DataView, 형식화 배열 내장 객체가 함께 쓰는 추상 연산: 인수 변환(ToIndex, 상대 인덱스), 인수 검증(ValidateTypedArray),
 * SpeciesConstructor, TypedArrayCreate / TypedArraySpeciesCreate, 버퍼 할당.
 */
final class TypedSupport {

    /**
     * 2^53 - 1, the largest integer index.
     * <p>
     * 가장 큰 정수 인덱스 2^53 - 1.
     */
    private static final double MAX_SAFE = 9007199254740991.0;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedSupport() {
    }

    /**
     * ToIndex: converts a value to a non-negative integer. A fractional part is dropped and undefined/NaN give 0 (ECMAScript 2017 and
     * browsers; ECMAScript 2015 would reject fractions and NaN with a RangeError). Negative values and values beyond 2^53-1 throw a RangeError.
     * <p>
     * ToIndex: 값을 음이 아닌 정수로 바꾼다. 소수부는 버리고 undefined/NaN 은 0 이 된다(ECMAScript 2017 과 브라우저. ECMAScript 2015 는 소수와 NaN 을
     * RangeError 로 거부한다). 음수와 2^53-1 보다 큰 값은 RangeError.
     *
     * @param v     the value / 값
     * @param what  description for the error message / 오류 메시지용 설명
     * @return the integer / 정수
     */
    static double toIndex(JsValue v, String what) {
        if (v.isUndefined()) {
            return 0;
        }
        double integer = Ops.toInteger(v);
        if (integer < 0 || integer > MAX_SAFE) {
            throw JsException.rangeError("Invalid " + what);
        }
        return integer;
    }

    /**
     * Reads the maximum byte length option on a buffer constructor.
     * <p>
     * 버퍼 생성자에 전달된 최대 바이트 길이 옵션을 읽는다.
     *
     * @param options the constructor's options argument / 생성자 옵션 인수
     * @return a maximum length or -1 if it was omitted / 최대 길이. 생략하면 -1
     */
    static double maxByteLengthOption(JsValue options) {
        JsValue maximum = options instanceof JsObject ? ((JsObject) options).get("maxByteLength") : Js.UNDEFINED;
        return maximum.isUndefined() ? -1 : toIndex(maximum, "maximum array buffer length");
    }

    /**
     * Converts a relative index argument (negative counts from the end) into an absolute position clamped to 0..len.
     * <p>
     * 상대 인덱스 인수(음수는 끝에서부터 센다)를 0..len 으로 제한한 절대 위치로 바꾼다.
     *
     * @param v          the argument / 인수
     * @param len        the length / 길이
     * @param ifUndefined result when the argument is undefined / 인수가 undefined 일 때의 결과
     * @return the position / 위치
     */
    static int relative(JsValue v, int len, int ifUndefined) {
        if (v.isUndefined()) {
            return ifUndefined;
        }
        double r = Ops.toInteger(v);
        if (r < 0) {
            double p = len + r;
            return p < 0 ? 0 : (int) p;
        }
        return r > len ? len : (int) r;
    }

    /**
     * Validates a typed array and its current buffer bounds (ValidateTypedArray).
     * <p>
     * 형식화 배열과 버퍼의 현재 경계를 검증한다(ValidateTypedArray).
     *
     * @param v the value, usually the this value / 값. 보통 this 값
     * @return the typed array / 형식화 배열
     */
    static TypedArrayObject validate(JsValue v) {
        if (!(v instanceof TypedArrayObject)) {
            throw JsException.typeError("this is not a typed array");
        }
        TypedArrayObject t = (TypedArrayObject) v;
        if (t.isOutOfBounds()) {
            throw JsException.typeError("TypedArray view is out of bounds");
        }
        return t;
    }

    /**
     * Returns the callable argument or throws a TypeError.
     * <p>
     * 호출 가능한 인수를 반환하거나 TypeError 를 던진다.
     *
     * @param v the argument / 인수
     * @return the function / 함수
     */
    static JsObject callable(JsValue v) {
        if (!v.isCallable() || !(v instanceof JsObject)) {
            throw JsException.typeError(JsException.describe(v) + " is not a function");
        }
        return (JsObject) v;
    }

    /**
     * SpeciesConstructor (7.3.20).
     * <p>
     * SpeciesConstructor (7.3.20).
     *
     * @param o           the exemplar object / 기준 객체
     * @param defaultCtor the constructor to use if there is no species / species 가 없을 때 쓸 생성자
     * @return the constructor / 생성자
     */
    static JsObject speciesConstructor(JsObject o, JsObject defaultCtor) {
        JsValue c = o.get("constructor");
        if (c.isUndefined()) {
            return defaultCtor;
        }
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("object.constructor is not an object");
        }
        JsValue s = ((JsObject) c).get(WellKnownSymbols.SPECIES);
        if (s.isNullOrUndefined()) {
            return defaultCtor;
        }
        if (Ops.isConstructor(s)) {
            return (JsObject) s;
        }
        throw JsException.typeError("object.constructor[Symbol.species] is not a constructor");
    }

    /**
     * TypedArrayCreate (22.2.4.6): constructs a typed array with a constructor and validates the result.
     * <p>
     * TypedArrayCreate (22.2.4.6): 생성자로 형식화 배열을 만들고 결과를 검증한다.
     *
     * @param ctor the constructor / 생성자
     * @param args the constructor arguments / 생성자 인수
     * @return the new typed array / 새 형식화 배열
     */
    static TypedArrayObject create(JsObject ctor, JsValue[] args) {
        JsObject o = Ops.construct(ctor, args);
        TypedArrayObject t = validate(o);
        if (args.length == 1 && args[0] instanceof JsNumber && t.getLength() < ((JsNumber) args[0]).getValue()) {
            throw JsException.typeError("the typed array constructor returned an array that is too short");
        }
        return t;
    }

    /**
     * TypedArraySpeciesCreate (22.2.4.7): creates a typed array like an exemplar, honoring its species constructor.
     * <p>
     * TypedArraySpeciesCreate (22.2.4.7): 기준 배열의 species 생성자를 존중하며 같은 종류의 형식화 배열을 만든다.
     *
     * @param realm    the realm / realm
     * @param exemplar the exemplar typed array / 기준 형식화 배열
     * @param args     the constructor arguments / 생성자 인수
     * @return the new typed array / 새 형식화 배열
     */
    static TypedArrayObject speciesCreate(JsRealm realm, TypedArrayObject exemplar, JsValue[] args) {
        JsObject def = realm.getIntrinsic("%" + exemplar.getType().getConstructorName() + "%");
        TypedArrayObject result = create(speciesConstructor(exemplar, def), args);
        if (result.getType().isBigInt() != exemplar.getType().isBigInt()) {
            throw JsException.typeError("The species constructor changed the TypedArray content type");
        }
        return result;
    }

    /**
     * Allocates a zeroed byte array, mapping size limits and memory exhaustion to a RangeError.
     * <p>
     * 0 으로 채워진 바이트 배열을 할당한다. 크기 제한과 메모리 부족은 RangeError 로 바꾼다.
     *
     * @param n the number of bytes / 바이트 수
     * @return the array / 배열
     */
    static byte[] allocate(double n) {
        if (n > ArrayBufferObject.MAX_BYTE_LENGTH) {
            throw JsException.rangeError("Array buffer allocation failed");
        }
        try {
            return new byte[(int) n];
        } catch (OutOfMemoryError e) {
            throw JsException.rangeError("Array buffer allocation failed");
        }
    }

    /**
     * Creates an ArrayBuffer object with the realm's ArrayBuffer.prototype.
     * <p>
     * realm 의 ArrayBuffer.prototype 을 쓰는 ArrayBuffer 객체를 만든다.
     *
     * @param realm the realm / realm
     * @param data  the storage (not copied) / 저장소(복사하지 않음)
     * @return the buffer object / 버퍼 객체
     */
    static ArrayBufferObject newBuffer(JsRealm realm, byte[] data) {
        JsObject proto = realm.getIntrinsic("%ArrayBufferPrototype%");
        if (proto == null) {
            throw new IllegalStateException("TypedLibInstaller has not been installed in this realm");
        }
        return new ArrayBufferObject(proto, data);
    }

    /**
     * IterableToList: collects all values of an iterable with a known @@iterator method.
     * <p>
     * IterableToList: @@iterator 메서드를 아는 반복 가능 객체의 모든 값을 모은다.
     *
     * @param iterable the iterable / 반복 가능 객체
     * @param method   its @@iterator method / @@iterator 메서드
     * @return the values / 값 목록
     */
    static List<JsValue> iterableToList(JsValue iterable, JsValue method) {
        JsObject it = IteratorHelper.getIteratorFromMethod(iterable, method);
        List<JsValue> list = new ArrayList<JsValue>();
        // 이터레이터가 끝날 때까지 값을 모은다.
        while (true) {
            JsObject r = IteratorHelper.step(it);
            if (r == null) {
                return list;
            }
            list.add(IteratorHelper.value(r));
        }
    }
}
