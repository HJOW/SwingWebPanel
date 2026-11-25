package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * A typed array: an integer-indexed exotic object (ECMA-262 6th edition, 9.4.5) that shows a window of an {@link ArrayBufferObject} as a
 * list of numbers of one {@link ElementType}. Properties whose key is a <em>canonical numeric string</em> ("0", "12", "-0", "1.5", "NaN",
 * "Infinity", ...) never reach the ordinary property table: valid integer indices read and write the buffer, every other numeric key is
 * simply absent (reads give undefined, writes are ignored) and the prototype chain is not consulted for them. The element properties are
 * writable, enumerable and not configurable. Writes to invalid indices are ignored without error (the behavior of ECMAScript 2017 and later
 * and of browsers; the 2015 text would make a strict mode assignment fail). A detached buffer makes the array behave as empty.
 * <p>
 * 형식화 배열: {@link ArrayBufferObject} 의 한 구간을 한 {@link ElementType} 숫자 목록으로 보여 주는 정수 인덱스 이국적 객체
 * (ECMA-262 6판, 9.4.5). 키가 <em>정규 숫자 문자열</em>("0", "12", "-0", "1.5", "NaN", "Infinity" 등)인 프로퍼티는 일반 프로퍼티 표에 들어가지 않는다:
 * 올바른 정수 인덱스는 버퍼를 읽고 쓰며, 그 밖의 숫자 키는 아예 없는 것으로 취급되어(읽으면 undefined, 쓰기는 무시) 프로토타입 체인도 보지 않는다.
 * 요소 프로퍼티는 writable, 열거 가능, configurable 아님이다. 잘못된 인덱스에 쓰는 것은 오류 없이 무시한다(ECMAScript 2017 이후와 브라우저의 동작이며,
 * 2015 본문은 엄격 모드 대입을 실패시킨다). 버퍼가 분리되면 배열은 빈 것처럼 동작한다.
 */
public class TypedArrayObject extends JsObject {

    /**
     * The element type.
     * <p>
     * 요소 형식.
     */
    private final ElementType type;

    /**
     * The viewed buffer.
     * <p>
     * 보고 있는 버퍼.
     */
    private final ArrayBufferObject buffer;

    /**
     * Offset of the first element in bytes.
     * <p>
     * 첫 요소의 바이트 위치.
     */
    private final int byteOffset;

    /**
     * Number of elements (fixed at creation).
     * <p>
     * 요소 수(생성 시 고정).
     */
    private final int length;

    /**
     * Creates a typed array; the arguments must already be validated (alignment and range against the buffer).
     * <p>
     * 형식화 배열을 만든다. 인수는 이미 검증되어 있어야 한다(정렬과 버퍼에 대한 범위).
     *
     * @param prototype  the [[Prototype]] / [[Prototype]]
     * @param type       element type / 요소 형식
     * @param buffer     the buffer / 버퍼
     * @param byteOffset offset of the first element in bytes / 첫 요소의 바이트 위치
     * @param length     number of elements or -1 to track a resizable buffer / 요소 수. 가변 버퍼를 추적하면 -1
     */
    public TypedArrayObject(JsObject prototype, ElementType type, ArrayBufferObject buffer, int byteOffset, int length) {
        super(prototype);
        this.type = type;
        this.buffer = buffer;
        this.byteOffset = byteOffset;
        this.length = length;
    }

    /**
     * Returns the constructor name, for example "Int8Array".
     * <p>
     * 생성자 이름을 반환한다. 예: "Int8Array".
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return type.getConstructorName();
    }

    /**
     * Returns the element type.
     * <p>
     * 요소 형식을 반환한다.
     *
     * @return the type / 형식
     */
    public ElementType getType() {
        return type;
    }

    /**
     * Returns the viewed buffer.
     * <p>
     * 보고 있는 버퍼를 반환한다.
     *
     * @return the buffer / 버퍼
     */
    public ArrayBufferObject getBuffer() {
        return buffer;
    }

    /**
     * Returns the visible byte offset, or zero if the view is out of bounds.
     * <p>
     * 접근 가능한 뷰의 바이트 위치를 반환한다. 경계를 벗어나면 0이다.
     *
     * @return the offset / 위치
     */
    public int getByteOffset() {
        return isOutOfBounds() ? 0 : byteOffset;
    }

    /**
     * Returns the original offset for operations such as subarray.
     * <p>
     * subarray 같은 연산에 필요한 원래 위치를 반환한다.
     *
     * @return the original byte offset / 원래 바이트 위치
     */
    int getRawByteOffset() {
        return byteOffset;
    }

    /**
     * Tells whether the array length is derived from its current buffer size.
     * <p>
     * 배열 길이를 버퍼의 현재 크기로 계산하는지 알려 준다.
     *
     * @return true for a length-tracking view / 길이를 자동 추적하는 뷰이면 true
     */
    public boolean isLengthTracking() {
        return length < 0;
    }

    /**
     * Checks the array's range against the current byte length.
     * <p>
     * 버퍼의 현재 바이트 길이와 배열이 차지하는 범위를 대조한다.
     *
     * @return true for a detached buffer or a view beyond its bounds / 버퍼가 분리되었거나 뷰가 범위를 벗어났으면 true
     */
    public boolean isOutOfBounds() {
        if (buffer.isDetached()) {
            return true;
        }
        int remaining = buffer.getByteLength() - byteOffset;
        return remaining < 0 || (!isLengthTracking() && length > remaining / type.getSize());
    }

    /**
     * Returns the visible number of elements, or zero if the view is out of bounds.
     * <p>
     * 접근 가능한 뷰의 요소 수를 반환한다. 경계를 벗어나면 0이다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        if (isOutOfBounds()) {
            return 0;
        }
        return isLengthTracking() ? (buffer.getByteLength() - byteOffset) / type.getSize() : length;
    }

    /**
     * Returns the visible byte length, or zero if the view is out of bounds.
     * <p>
     * 접근 가능한 바이트 길이를 반환한다. 경계를 벗어나면 0이다.
     *
     * @return the length in bytes / 바이트 길이
     */
    public int getByteLength() {
        return getLength() * type.getSize();
    }

    /**
     * Reads the element at a valid index.
     * <p>
     * 올바른 인덱스의 요소를 읽는다.
     *
     * @param index an index from 0 to length-1 / 0 ~ length-1 의 인덱스
     * @return the element as a number / 숫자로 바꾼 요소
     */
    public double getElement(int index) {
        return buffer.readNumber(type, byteOffset + index * type.getSize());
    }

    /**
     * Reads an element without losing BigInt precision.
     * <p>
     * BigInt 정밀도를 잃지 않고 요소를 읽는다.
     *
     * @param index index / 인덱스
     * @return the Number or BigInt value / Number 또는 BigInt 값
     */
    public JsValue getElementValue(int index) {
        return buffer.readValue(type, byteOffset + index * type.getSize(), true);
    }

    /**
     * Converts a number to the element type and stores it at a valid index.
     * <p>
     * 숫자를 요소 형식으로 바꾸어 올바른 인덱스에 저장한다.
     *
     * @param index an index from 0 to length-1 / 0 ~ length-1 의 인덱스
     * @param value the number to store / 저장할 숫자
     */
    public void setElement(int index, double value) {
        buffer.writeNumber(type, byteOffset + index * type.getSize(), value);
    }

    /**
     * Converts a value and writes it if the index is still in range.
     * <p>
     * 값을 변환한 뒤 인덱스가 여전히 범위 안이면 쓴다.
     *
     * @param index index / 인덱스
     * @param value value / 값
     */
    public void setElementValue(int index, JsValue value) {
        setIndexed(index, value);
    }

    /**
     * Converts a value once for repeated writes such as typed-array fill.
     * <p>
     * 형식화 배열 fill 처럼 반복해서 쓰는 경우에 값을 한 번만 변환한다.
     *
     * @param value the value / 값
     * @return Number or BigInt primitive / Number 또는 BigInt 원시값
     */
    public JsValue convertElement(JsValue value) {
        return type.isBigInt() ? JsBigInt.of(Ops.toBigInt(value)) : Js.num(Ops.toNumber(value));
    }

    /**
     * Tells whether a number is a valid element index of this array: an integer, not -0, from 0 up to length-1.
     * <p>
     * 숫자가 이 배열의 올바른 요소 인덱스인지 알려 준다: -0 이 아닌 정수이며 0 이상 length-1 이하.
     *
     * @param d the numeric index / 숫자 인덱스
     * @return true if valid / 올바르면 true
     */
    public boolean isValidIndex(double d) {
        if (d != d || Double.isInfinite(d) || d != Math.floor(d)) {
            return false;
        }
        if (d == 0 && 1 / d < 0) {
            return false;
        }
        return d >= 0 && d < getLength();
    }

    /**
     * CanonicalNumericIndexString (7.1.16): returns the number a key stands for if the key is the canonical string form of a number
     * ("-0" is the one special case), otherwise null.
     * <p>
     * CanonicalNumericIndexString (7.1.16): 키가 숫자의 정규 문자열 형태이면 그 숫자를 반환하고("-0" 이 유일한 특례) 아니면 null 을 반환한다.
     *
     * @param s the property key / 프로퍼티 키
     * @return the number or null / 숫자 또는 null
     */
    public static Double canonicalNumericIndex(String s) {
        int n = s.length();
        if (n == 0) {
            return null;
        }
        char c = s.charAt(0);
        // 숫자 모양이 될 수 있는 첫 글자(숫자, '-', Infinity 의 'I', NaN 의 'N')가 아니면 바로 제외한다.
        if (!((c >= '0' && c <= '9') || c == '-' || c == 'I' || c == 'N')) {
            return null;
        }
        long idx = PropertyKeys.arrayIndex(s);
        if (idx >= 0) {
            return Double.valueOf((double) idx);
        }
        if ("-0".equals(s)) {
            return Double.valueOf(-0.0);
        }
        double d = Ops.toNumber(Js.str(s));
        return Ops.toString(Js.num(d)).equals(s) ? Double.valueOf(d) : null;
    }

    /**
     * IntegerIndexedElementGet: reads the element of a numeric index, undefined for invalid indices.
     * <p>
     * IntegerIndexedElementGet: 숫자 인덱스의 요소를 읽는다. 올바르지 않은 인덱스이면 undefined.
     *
     * @param index the numeric index / 숫자 인덱스
     * @return the number or undefined / 숫자 또는 undefined
     */
    public JsValue getIndexed(double index) {
        if (!isValidIndex(index)) {
            return Js.UNDEFINED;
        }
        return getElementValue((int) index);
    }

    /**
     * IntegerIndexedElementSet: converts the value with ToNumber (always, so side effects happen even for invalid indices) and stores it
     * if the index is valid.
     * <p>
     * IntegerIndexedElementSet: 값을 ToNumber 로 바꾸고(올바르지 않은 인덱스여도 부수 효과가 일어나도록 항상 수행) 인덱스가 올바르면 저장한다.
     *
     * @param index the numeric index / 숫자 인덱스
     * @param value the value to store / 저장할 값
     * @return true if the element was stored / 저장되었으면 true
     */
    public boolean setIndexed(double index, JsValue value) {
        JsValue converted = convertElement(value);
        if (!isValidIndex(index)) {
            return false;
        }
        buffer.writeValue(type, byteOffset + (int) index * type.getSize(), converted, true);
        return true;
    }

    /**
     * The [[GetOwnProperty]] view of an element: a temporary data property (writable, enumerable, not configurable).
     * <p>
     * 요소의 [[GetOwnProperty]] 모습: 임시 데이터 프로퍼티(writable, 열거 가능, configurable 아님).
     *
     * @param key the property key / 프로퍼티 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (key instanceof String) {
            Double n = canonicalNumericIndex((String) key);
            if (n != null) {
                if (!isValidIndex(n.doubleValue())) {
                    return null;
                }
                return Property.data(getElementValue((int) n.doubleValue()), true, true, false);
            }
        }
        return super.getOwnPropertySlot(key);
    }

    /**
     * [[DefineOwnProperty]] (9.4.5.3): only data descriptors that keep the element writable, enumerable and not configurable are accepted
     * for valid indices; invalid numeric indices are rejected.
     * <p>
     * [[DefineOwnProperty]] (9.4.5.3): 올바른 인덱스에는 요소를 writable, 열거 가능, configurable 아님으로 유지하는 데이터 기술자만 받아들이고
     * 올바르지 않은 숫자 인덱스는 거부한다.
     *
     * @param key  the property key / 프로퍼티 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        if (key instanceof String) {
            Double n = canonicalNumericIndex((String) key);
            if (n != null) {
                if (!isValidIndex(n.doubleValue())) {
                    return false;
                }
                if (desc.configurable != null && desc.configurable.booleanValue()) {
                    return false;
                }
                if (desc.enumerable != null && !desc.enumerable.booleanValue()) {
                    return false;
                }
                if (desc.isAccessor()) {
                    return false;
                }
                if (desc.writable != null && !desc.writable.booleanValue()) {
                    return false;
                }
                if (desc.value != null) {
                    setIndexed(n.doubleValue(), desc.value);
                }
                return true;
            }
        }
        return super.defineOwnProperty(key, desc);
    }

    /**
     * [[HasProperty]] (9.4.5.2): numeric keys are present exactly when they are valid indices; the prototype chain is not consulted.
     * <p>
     * [[HasProperty]] (9.4.5.2): 숫자 키는 올바른 인덱스일 때만 존재하며 프로토타입 체인은 보지 않는다.
     *
     * @param key the property key / 프로퍼티 키
     * @return true if present / 있으면 true
     */
    public boolean hasProperty(Object key) {
        if (key instanceof String) {
            Double n = canonicalNumericIndex((String) key);
            if (n != null) {
                return isValidIndex(n.doubleValue());
            }
        }
        return super.hasProperty(key);
    }

    /**
     * [[Get]] (9.4.5.4): numeric keys read the buffer and never reach the prototype chain.
     * <p>
     * [[Get]] (9.4.5.4): 숫자 키는 버퍼를 읽으며 프로토타입 체인에 닿지 않는다.
     *
     * @param key      the property key / 프로퍼티 키
     * @param receiver the receiver / receiver
     * @return the value / 값
     */
    public JsValue get(Object key, JsValue receiver) {
        if (key instanceof String) {
            Double n = canonicalNumericIndex((String) key);
            if (n != null) {
                return getIndexed(n.doubleValue());
            }
        }
        return super.get(key, receiver);
    }

    /**
     * [[Set]] (9.4.5.5): numeric keys write the buffer (invalid indices are ignored) and the assignment counts as successful.
     * <p>
     * [[Set]] (9.4.5.5): 숫자 키는 버퍼에 쓰며(올바르지 않은 인덱스는 무시) 대입은 성공한 것으로 본다.
     *
     * @param key      the property key / 프로퍼티 키
     * @param value    the value / 값
     * @param receiver the receiver / receiver
     * @return true for numeric keys, otherwise the ordinary result / 숫자 키이면 true, 아니면 일반 결과
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        if (key instanceof String) {
            Double n = canonicalNumericIndex((String) key);
            if (n != null) {
                setIndexed(n.doubleValue(), value);
                return true;
            }
        }
        return super.set(key, value, receiver);
    }

    /**
     * [[OwnPropertyKeys]] (9.4.5.6): the integer indices first, then the ordinary string keys and symbols.
     * <p>
     * [[OwnPropertyKeys]] (9.4.5.6): 정수 인덱스가 먼저, 그다음 일반 문자열 키와 심볼.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        int len = getLength();
        List<Object> keys = new ArrayList<Object>(len + 4);
        // 요소 인덱스를 오름차순 문자열 키로 먼저 나열한다.
        for (int i = 0; i < len; i++) {
            keys.add(PropertyKeys.fromIndex(i));
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
