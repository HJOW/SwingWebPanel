package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The behavior of the nine concrete typed array constructors (ECMA-262 6th edition, 22.2.4): no arguments, a length, another typed array, an
 * ArrayBuffer with offset and length, and an iterable or array-like object.
 * <p>
 * 아홉 개 구체 형식화 배열 생성자의 동작(ECMA-262 6판, 22.2.4): 인수 없음, 길이, 다른 형식화 배열, 위치와 길이를 가진 ArrayBuffer,
 * 반복 가능 객체 또는 유사 배열 객체.
 */
final class TypedArrayFactory {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedArrayFactory() {
    }

    /**
     * Implements <code>new Int8Array(...)</code> and its siblings.
     * <p>
     * <code>new Int8Array(...)</code> 과 그 형제들을 구현한다.
     *
     * @param realm     the realm / realm
     * @param type      the element type / 요소 형식
     * @param args      the constructor arguments / 생성자 인수
     * @param newTarget new.target / new.target
     * @return the new typed array / 새 형식화 배열
     */
    static JsObject construct(JsRealm realm, ElementType type, JsValue[] args, JsObject newTarget) {
        JsObject proto = Ops.getPrototypeFromConstructor(realm, newTarget, realm.getIntrinsic("%" + type.getConstructorName() + "Prototype%"));
        JsValue first = Js.arg(args, 0);
        if (args.length == 0 || !(first instanceof JsObject)) {
            double length = args.length == 0 ? 0 : TypedSupport.toIndex(first, "typed array length");
            return allocate(realm, type, proto, length);
        }
        if (first instanceof TypedArrayObject) {
            return fromTypedArray(realm, type, proto, (TypedArrayObject) first);
        }
        if (first instanceof ArrayBufferObject) {
            return fromBuffer(type, proto, (ArrayBufferObject) first, Js.arg(args, 1), Js.arg(args, 2));
        }
        return fromObject(realm, type, proto, (JsObject) first);
    }

    /**
     * Allocates a zero filled typed array of a given length on a new buffer.
     * <p>
     * 새 버퍼 위에 주어진 길이의 0 으로 채운 형식화 배열을 할당한다.
     *
     * @param realm  the realm / realm
     * @param type   the element type / 요소 형식
     * @param proto  the prototype / 프로토타입
     * @param length the number of elements / 요소 수
     * @return the typed array / 형식화 배열
     */
    static TypedArrayObject allocate(JsRealm realm, ElementType type, JsObject proto, double length) {
        double bytes = length * type.getSize();
        if (bytes > ArrayBufferObject.MAX_BYTE_LENGTH) {
            throw JsException.rangeError("Invalid typed array length: " + (long) length);
        }
        return new TypedArrayObject(proto, type, TypedSupport.newBuffer(realm, TypedSupport.allocate(bytes)), 0, (int) length);
    }

    /**
     * 22.2.4.3: copies the elements of another typed array, converting between element types if needed.
     * <p>
     * 22.2.4.3: 다른 형식화 배열의 요소를 복사하며 필요하면 요소 형식을 변환한다.
     *
     * @param realm  the realm / realm
     * @param type   the element type / 요소 형식
     * @param proto  the prototype / 프로토타입
     * @param source the source array / 원본 배열
     * @return the new typed array / 새 형식화 배열
     */
    private static JsObject fromTypedArray(JsRealm realm, ElementType type, JsObject proto, TypedArrayObject source) {
        if (source.getBuffer().isDetached()) {
            throw JsException.typeError("the source typed array buffer is detached");
        }
        int length = source.getLength();
        double byteLength = (double) length * type.getSize();
        JsObject bufferCtor = TypedSupport.speciesConstructor(source.getBuffer(), realm.getIntrinsic("%ArrayBuffer%"));
        ArrayBufferObject buffer = TypedSupport.allocateBuffer(realm, bufferCtor, byteLength);
        TypedArrayObject result = new TypedArrayObject(proto, type, buffer, 0, length);
        if (source.getBuffer().isDetached()) {
            throw JsException.typeError("the source typed array buffer is detached");
        }
        if (source.getType() == type) {
            System.arraycopy(source.getBuffer().getData(), source.getByteOffset(), buffer.getData(), 0, (int) byteLength);
        } else {
            // 요소 형식이 다르면 요소마다 숫자로 읽어 새 형식으로 바꿔 쓴다.
            for (int i = 0; i < length; i++) {
                result.setElement(i, source.getElement(i));
            }
        }
        return result;
    }

    /**
     * 22.2.4.5: creates a view on an existing buffer.
     * <p>
     * 22.2.4.5: 기존 버퍼 위의 뷰를 만든다.
     *
     * @param type       the element type / 요소 형식
     * @param proto      the prototype / 프로토타입
     * @param buffer     the buffer / 버퍼
     * @param byteOffset the byteOffset argument / byteOffset 인수
     * @param length     the length argument / length 인수
     * @return the new typed array / 새 형식화 배열
     */
    private static JsObject fromBuffer(ElementType type, JsObject proto, ArrayBufferObject buffer, JsValue byteOffset, JsValue length) {
        int size = type.getSize();
        double offset = TypedSupport.toIndex(byteOffset, "typed array offset");
        if (offset % size != 0) {
            throw JsException.rangeError("start offset of " + type.getConstructorName() + " should be a multiple of " + size);
        }
        double newLength;
        if (!length.isUndefined()) {
            newLength = TypedSupport.toIndex(length, "typed array length");
        } else {
            newLength = -1;
        }
        if (buffer.isDetached()) {
            throw JsException.typeError("the buffer is detached");
        }
        double bufferLength = buffer.getByteLength();
        double newByteLength;
        if (newLength < 0) {
            if (bufferLength % size != 0) {
                throw JsException.rangeError("byte length of " + type.getConstructorName() + " should be a multiple of " + size);
            }
            newByteLength = bufferLength - offset;
            if (newByteLength < 0) {
                throw JsException.rangeError("Start offset " + (long) offset + " is outside the bounds of the buffer");
            }
        } else {
            newByteLength = newLength * size;
            if (offset + newByteLength > bufferLength) {
                throw JsException.rangeError("Invalid typed array length: " + (long) newLength);
            }
        }
        return new TypedArrayObject(proto, type, buffer, (int) offset, (int) (newByteLength / size));
    }

    /**
     * 22.2.4.4: creates a typed array from an iterable (preferred) or an array-like object.
     * <p>
     * 22.2.4.4: 반복 가능 객체(우선) 또는 유사 배열 객체로 형식화 배열을 만든다.
     *
     * @param realm  the realm / realm
     * @param type   the element type / 요소 형식
     * @param proto  the prototype / 프로토타입
     * @param source the source object / 원본 객체
     * @return the new typed array / 새 형식화 배열
     */
    private static JsObject fromObject(JsRealm realm, ElementType type, JsObject proto, JsObject source) {
        JsValue usingIterator = Ops.getMethod(realm, source, WellKnownSymbols.ITERATOR);
        if (!usingIterator.isUndefined()) {
            List<JsValue> values = TypedSupport.iterableToList(source, usingIterator);
            TypedArrayObject result = allocate(realm, type, proto, values.size());
            // 이터레이터가 낸 값을 차례로 숫자로 바꿔 저장한다.
            for (int k = 0; k < values.size(); k++) {
                result.setIndexed(k, values.get(k));
            }
            return result;
        }
        double length = Ops.lengthOf(source);
        TypedArrayObject result = allocate(realm, type, proto, length);
        // 유사 배열의 요소를 인덱스 순서로 읽어 저장한다.
        for (int k = 0; k < (int) length; k++) {
            result.setIndexed(k, source.get(PropertyKeys.fromIndex(k)));
        }
        return result;
    }
}
