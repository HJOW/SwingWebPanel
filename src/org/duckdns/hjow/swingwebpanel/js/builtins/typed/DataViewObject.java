package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * A DataView object: a window of a given byte offset and length on an {@link ArrayBufferObject} with explicit-endianness accessors.
 * <p>
 * DataView 객체: {@link ArrayBufferObject} 위의 주어진 바이트 위치와 길이의 창이며 엔디언을 직접 지정하는 접근자를 가진다.
 */
public class DataViewObject extends JsObject {

    /**
     * The viewed buffer.
     * <p>
     * 보고 있는 버퍼.
     */
    private final ArrayBufferObject buffer;

    /**
     * Offset of the window in bytes.
     * <p>
     * 창의 바이트 위치.
     */
    private final int byteOffset;

    /**
     * Length of the window in bytes.
     * <p>
     * 창의 바이트 길이.
     */
    private final int byteLength;

    /**
     * Creates a view; the arguments must already be validated against the buffer length.
     * <p>
     * 뷰를 만든다. 인수는 이미 버퍼 길이에 맞게 검증되어 있어야 한다.
     *
     * @param prototype  the [[Prototype]] / [[Prototype]]
     * @param buffer     the buffer / 버퍼
     * @param byteOffset offset in bytes / 바이트 위치
     * @param byteLength length in bytes / 바이트 길이
     */
    public DataViewObject(JsObject prototype, ArrayBufferObject buffer, int byteOffset, int byteLength) {
        super(prototype);
        this.buffer = buffer;
        this.byteOffset = byteOffset;
        this.byteLength = byteLength;
    }

    /**
     * Returns "DataView".
     * <p>
     * "DataView" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "DataView";
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
     * Returns the offset of the window in bytes.
     * <p>
     * 창의 바이트 위치를 반환한다.
     *
     * @return the offset / 위치
     */
    public int getByteOffset() {
        return byteOffset;
    }

    /**
     * Returns the length of the window in bytes.
     * <p>
     * 창의 바이트 길이를 반환한다.
     *
     * @return the length / 길이
     */
    public int getByteLength() {
        return byteLength;
    }
}
