package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * A DataView object: a fixed-length or length-tracking window on an {@link ArrayBufferObject} with explicit-endianness accessors.
 * <p>
 * DataView 객체: {@link ArrayBufferObject} 위의 고정 길이 또는 길이를 추적하는 창이며 엔디언을 직접 지정하는 접근자를 가진다.
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
     * Fixed byte length, or -1 to track the buffer length.
     * <p>
     * 고정 바이트 길이. 버퍼 길이를 추적하면 -1.
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
     * @param byteLength byte length or -1 to track a resizable buffer / 바이트 길이. 가변 버퍼를 추적하면 -1
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
     * Reports whether the view's length follows changes to the buffer's length.
     * <p>
     * 뷰의 길이가 버퍼의 길이 변화를 따르는지 알려 준다.
     *
     * @return true if the length is automatic / 자동 길이이면 true
     */
    public boolean isLengthTracking() {
        return byteLength < 0;
    }

    /**
     * Checks the view's current bounds.
     * <p>
     * 뷰의 현재 경계를 확인한다.
     *
     * @return true if detached or out of bounds / 분리되었거나 범위를 벗어났으면 true
     */
    public boolean isOutOfBounds() {
        if (buffer.isDetached()) {
            return true;
        }
        int remaining = buffer.getByteLength() - byteOffset;
        return remaining < 0 || (byteLength >= 0 && byteLength > remaining);
    }

    /**
     * Returns the accessible window length, or zero if out of bounds.
     * <p>
     * 접근할 수 있는 창의 길이를 반환한다. 범위를 벗어나면 0이다.
     *
     * @return current length in bytes / 현재 바이트 길이
     */
    public int getByteLength() {
        if (isOutOfBounds()) {
            return 0;
        }
        return byteLength < 0 ? buffer.getByteLength() - byteOffset : byteLength;
    }
}
