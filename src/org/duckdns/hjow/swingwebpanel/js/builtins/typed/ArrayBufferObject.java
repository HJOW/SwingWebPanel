package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * An ArrayBuffer object: a fixed length block of bytes backed by a Java <code>byte[]</code>. Typed arrays and DataViews are views on the
 * same bytes. A buffer can be <em>detached</em> from Java code (see {@link #detach()}), after which it has length 0 and all views on it
 * become empty; ECMAScript 2015 defines detaching for transferring buffers between realms and workers.
 * <p>
 * ArrayBuffer 객체: Java <code>byte[]</code> 로 뒷받침되는 고정 길이 바이트 덩어리. 형식화 배열과 DataView 는 같은 바이트의 뷰이다.
 * Java 코드에서 버퍼를 <em>분리</em>(detach)할 수 있으며({@link #detach()}) 그러면 길이가 0 이 되고 그 위의 모든 뷰가 비게 된다.
 * ECMAScript 2015 는 realm 이나 worker 사이로 버퍼를 넘길 때 쓰도록 분리를 정의한다.
 */
public class ArrayBufferObject extends JsObject {

    /**
     * Largest byte length that can be allocated (a Java array cannot be larger); bigger requests throw a RangeError.
     * <p>
     * 할당할 수 있는 가장 큰 바이트 길이(Java 배열은 이보다 클 수 없다). 더 크게 요청하면 RangeError 가 난다.
     */
    public static final int MAX_BYTE_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * The bytes, or null after the buffer was detached.
     * <p>
     * 바이트. 버퍼가 분리된 뒤에는 null.
     */
    private byte[] data;

    /**
     * Creates a buffer that uses the given array as its storage (no copy).
     * <p>
     * 주어진 배열을 저장소로 쓰는 버퍼를 만든다(복사하지 않음).
     *
     * @param prototype the [[Prototype]] / [[Prototype]]
     * @param data      the storage, not null / 저장소(null 불가)
     */
    public ArrayBufferObject(JsObject prototype, byte[] data) {
        super(prototype);
        this.data = data;
    }

    /**
     * Returns "ArrayBuffer".
     * <p>
     * "ArrayBuffer" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "ArrayBuffer";
    }

    /**
     * Returns the backing bytes without copying, or null if the buffer is detached. Changes to the array are visible through all views.
     * <p>
     * 뒷받침하는 바이트를 복사 없이 반환한다. 버퍼가 분리되었으면 null. 배열을 바꾸면 모든 뷰에서 보인다.
     *
     * @return the bytes or null / 바이트 또는 null
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the length in bytes (0 if detached).
     * <p>
     * 바이트 길이를 반환한다(분리되었으면 0).
     *
     * @return the length / 길이
     */
    public int getByteLength() {
        return data == null ? 0 : data.length;
    }

    /**
     * Tells whether the buffer was detached.
     * <p>
     * 버퍼가 분리되었는지 알려 준다.
     *
     * @return true if detached / 분리되었으면 true
     */
    public boolean isDetached() {
        return data == null;
    }

    /**
     * Detaches the buffer: the bytes are released, the length becomes 0 and every view on it behaves as empty (typed array methods
     * then throw a TypeError as ECMAScript 2015 requires).
     * <p>
     * 버퍼를 분리한다: 바이트를 놓아 주고 길이는 0 이 되며 그 위의 모든 뷰가 빈 것처럼 동작한다(형식화 배열 메서드는 ECMAScript 2015 가
     * 요구하는 대로 TypeError 를 던진다).
     */
    public void detach() {
        data = null;
    }
}
