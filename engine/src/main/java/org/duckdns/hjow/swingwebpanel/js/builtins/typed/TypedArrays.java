package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * Stable Java API for the rest of the library (canvas <code>ImageData</code>, XMLHttpRequest and Fetch bodies, <code>Blob</code>, media decoders) to
 * create and read the binary objects of a realm without going through script calls. The realm must have been prepared with
 * {@link TypedLibInstaller}. Buffers can wrap an existing <code>byte[]</code> without copying; views never copy; arguments are checked and
 * wrong ones throw {@link IllegalArgumentException}.
 * <p>
 * 라이브러리의 다른 부분(캔버스 <code>ImageData</code>, XMLHttpRequest 와 Fetch 본문, <code>Blob</code>, 미디어 디코더)이 스크립트 호출 없이
 * realm 의 이진 객체를 만들고 읽을 수 있게 하는 안정적인 Java API. realm 은 {@link TypedLibInstaller} 로 준비되어 있어야 한다. 버퍼는 기존
 * <code>byte[]</code> 를 복사 없이 감쌀 수 있고, 뷰는 절대 복사하지 않으며, 인수를 검사해 잘못된 것은 {@link IllegalArgumentException} 을 던진다.
 */
public final class TypedArrays {

    /**
     * A window on a byte array: the bytes <code>array[offset .. offset+length-1]</code>. The array is shared with the script object, so writes
     * through it are visible to scripts.
     * <p>
     * 바이트 배열의 구간: 바이트 <code>array[offset .. offset+length-1]</code>. 배열은 스크립트 객체와 공유되므로 이 배열에 쓰면 스크립트에서 보인다.
     */
    public static final class ByteView {
        /**
         * The shared backing array.
         * <p>
         * 공유되는 뒷받침 배열.
         */
        public final byte[] array;

        /**
         * Index of the first byte of the window.
         * <p>
         * 구간의 첫 바이트 위치.
         */
        public final int offset;

        /**
         * Number of bytes of the window.
         * <p>
         * 구간의 바이트 수.
         */
        public final int length;

        /**
         * Creates a window.
         * <p>
         * 구간을 만든다.
         *
         * @param array  the backing array / 뒷받침 배열
         * @param offset first byte / 첫 바이트
         * @param length number of bytes / 바이트 수
         */
        ByteView(byte[] array, int offset, int length) {
            this.array = array;
            this.offset = offset;
            this.length = length;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedArrays() {
    }

    /**
     * Tells whether a value is an ArrayBuffer object.
     * <p>
     * 값이 ArrayBuffer 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for ArrayBuffers / ArrayBuffer 이면 true
     */
    public static boolean isArrayBuffer(JsValue v) {
        return v instanceof ArrayBufferObject;
    }

    /**
     * Tells whether a value is a typed array (any of the nine kinds).
     * <p>
     * 값이 형식화 배열(아홉 종류 중 하나)인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for typed arrays / 형식화 배열이면 true
     */
    public static boolean isTypedArray(JsValue v) {
        return v instanceof TypedArrayObject;
    }

    /**
     * Tells whether a value is a DataView.
     * <p>
     * 값이 DataView 인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for DataViews / DataView 이면 true
     */
    public static boolean isDataView(JsValue v) {
        return v instanceof DataViewObject;
    }

    /**
     * Creates an ArrayBuffer that uses the given array as its storage (no copy): changes on either side are visible on the other.
     * <p>
     * 주어진 배열을 저장소로 쓰는 ArrayBuffer 를 만든다(복사 없음): 어느 쪽의 변경도 다른 쪽에서 보인다.
     *
     * @param realm the realm / realm
     * @param data  the bytes, not null / 바이트(null 불가)
     * @return the buffer object / 버퍼 객체
     */
    public static ArrayBufferObject newArrayBuffer(JsRealm realm, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        return TypedSupport.newBuffer(realm, data);
    }

    /**
     * Creates an ArrayBuffer holding a copy of a byte range.
     * <p>
     * 바이트 구간의 복사본을 담은 ArrayBuffer 를 만든다.
     *
     * @param realm  the realm / realm
     * @param data   the source bytes / 원본 바이트
     * @param offset the first byte to copy / 복사할 첫 바이트
     * @param length the number of bytes / 바이트 수
     * @return the buffer object / 버퍼 객체
     */
    public static ArrayBufferObject newArrayBufferCopy(JsRealm realm, byte[] data, int offset, int length) {
        if (data == null || offset < 0 || length < 0 || offset > data.length - length) {
            throw new IllegalArgumentException("invalid byte range");
        }
        byte[] copy = new byte[length];
        System.arraycopy(data, offset, copy, 0, length);
        return TypedSupport.newBuffer(realm, copy);
    }

    /**
     * Creates a typed array of any kind over a buffer, checking alignment and range.
     * <p>
     * 버퍼 위에 어떤 종류의 형식화 배열이든 만든다. 정렬과 범위를 검사한다.
     *
     * @param realm      the realm / realm
     * @param type       the element type / 요소 형식
     * @param buffer     the buffer, not detached / 버퍼(분리되지 않은 것)
     * @param byteOffset offset of the first element, a multiple of the element size / 첫 요소의 위치(요소 크기의 배수)
     * @param length     number of elements or -1 to track the buffer's length / 요소 수. 버퍼의 길이를 추적하면 -1
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject newTypedArray(JsRealm realm, ElementType type, ArrayBufferObject buffer, int byteOffset, int length) {
        if (type == null || buffer == null || buffer.isDetached()) {
            throw new IllegalArgumentException("a type and an attached buffer are required");
        }
        if (byteOffset < 0 || length < -1 || (length == -1 && !buffer.isResizable()) || byteOffset % type.getSize() != 0
                || (long) byteOffset + (long) Math.max(0, length) * type.getSize() > buffer.getByteLength()) {
            throw new IllegalArgumentException("offset/length do not fit the buffer or are not aligned");
        }
        JsObject proto = realm.getIntrinsic("%" + type.getConstructorName() + "Prototype%");
        if (proto == null) {
            throw new IllegalStateException("TypedLibInstaller has not been installed in this realm");
        }
        return new TypedArrayObject(proto, type, buffer, byteOffset, length);
    }

    /**
     * Creates a Uint8Array over a buffer.
     * <p>
     * 버퍼 위의 Uint8Array 를 만든다.
     *
     * @param realm      the realm / realm
     * @param buffer     the buffer / 버퍼
     * @param byteOffset offset of the first element / 첫 요소의 위치
     * @param length     number of elements / 요소 수
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject newUint8Array(JsRealm realm, ArrayBufferObject buffer, int byteOffset, int length) {
        return newTypedArray(realm, ElementType.UINT8, buffer, byteOffset, length);
    }

    /**
     * Creates a Uint8Array that shows a whole <code>byte[]</code> (the array is wrapped without copying).
     * <p>
     * <code>byte[]</code> 전체를 보여 주는 Uint8Array 를 만든다(배열을 복사 없이 감싼다).
     *
     * @param realm the realm / realm
     * @param data  the bytes / 바이트
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject newUint8Array(JsRealm realm, byte[] data) {
        ArrayBufferObject b = newArrayBuffer(realm, data);
        return newTypedArray(realm, ElementType.UINT8, b, 0, data.length);
    }

    /**
     * Creates a Uint8ClampedArray over a buffer (the type of canvas pixel data).
     * <p>
     * 버퍼 위의 Uint8ClampedArray 를 만든다(캔버스 픽셀 데이터의 형식).
     *
     * @param realm      the realm / realm
     * @param buffer     the buffer / 버퍼
     * @param byteOffset offset of the first element / 첫 요소의 위치
     * @param length     number of elements / 요소 수
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject newUint8ClampedArray(JsRealm realm, ArrayBufferObject buffer, int byteOffset, int length) {
        return newTypedArray(realm, ElementType.UINT8_CLAMPED, buffer, byteOffset, length);
    }

    /**
     * Creates a Float32Array over a buffer.
     * <p>
     * 버퍼 위의 Float32Array 를 만든다.
     *
     * @param realm      the realm / realm
     * @param buffer     the buffer / 버퍼
     * @param byteOffset offset of the first element (multiple of 4) / 첫 요소의 위치(4 의 배수)
     * @param length     number of elements / 요소 수
     * @return the typed array / 형식화 배열
     */
    public static TypedArrayObject newFloat32Array(JsRealm realm, ArrayBufferObject buffer, int byteOffset, int length) {
        return newTypedArray(realm, ElementType.FLOAT32, buffer, byteOffset, length);
    }

    /**
     * Returns a live byte window for a non-shared ArrayBuffer, typed array or DataView.
     * <p>
     * 공유하지 않는 ArrayBuffer, 형식화 배열, DataView의 바이트 구간을 복사 없이 반환한다.
     *
     * @param v the value / 값
     * @return the byte window, or null for other values or shared or detached buffers /
     * 바이트 구간. 이런 객체가 아니거나 버퍼가 공유 또는 분리되었으면 null
     */
    public static ByteView bytesOf(JsValue v) {
        if (v instanceof ArrayBufferObject) {
            ArrayBufferObject b = (ArrayBufferObject) v;
            return b.isShared() || b.isDetached() ? null : new ByteView(b.getData(), 0, b.getByteLength());
        }
        if (v instanceof TypedArrayObject) {
            TypedArrayObject t = (TypedArrayObject) v;
            return t.getBuffer().isShared() || t.isOutOfBounds() ? null : new ByteView(t.getBuffer().getData(), t.getByteOffset(), t.getByteLength());
        }
        if (v instanceof DataViewObject) {
            DataViewObject d = (DataViewObject) v;
            return d.getBuffer().isShared() || d.isOutOfBounds() ? null : new ByteView(d.getBuffer().getData(), d.getByteOffset(), d.getByteLength());
        }
        return null;
    }
}
