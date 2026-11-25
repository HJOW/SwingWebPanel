package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * An ArrayBuffer object with a fixed length or a specified maximum length. Typed arrays and DataViews refer to its current byte array.
 * A detached buffer has length zero and releases its storage.
 * <p>
 * 고정 길이 또는 최대 길이를 지정한 ArrayBuffer 객체. 형식화 배열과 DataView는 현재 바이트 배열을 참조한다.
 * 분리된 버퍼는 길이가 0이 되고 저장소를 놓아 준다.
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

    /** The resizable limit, or -1 for a fixed-length buffer. / 가변 길이의 상한. 고정 버퍼면 -1. */
    private final int maximum;

    /**
     * Creates a buffer that uses the given array as its storage (no copy).
     * <p>
     * 주어진 배열을 저장소로 쓰는 버퍼를 만든다(복사하지 않음).
     *
     * @param prototype the [[Prototype]] / [[Prototype]]
     * @param data      the storage, not null / 저장소(null 불가)
     */
    public ArrayBufferObject(JsObject prototype, byte[] data) {
        this(prototype, data, -1);
    }

    /**
     * Creates a buffer with the provided backing data and maximum length.
     * <p>
     * 받은 바이트 배열과 최대 길이로 버퍼를 만든다.
     *
     * @param prototype the [[Prototype]] / [[Prototype]]
     * @param data the backing bytes / 저장 바이트
     * @param maximum maximum length or -1 for fixed length / 최대 길이. 고정 길이이면 -1
     */
    ArrayBufferObject(JsObject prototype, byte[] data, int maximum) {
        super(prototype);
        this.data = data;
        this.maximum = maximum;
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
     * Returns the current backing bytes or null if detached. Resize replaces the backing array.
     * <p>
     * 현재 저장 바이트를 반환한다. 분리되었으면 null이고, 크기를 바꾸면 새 바이트 배열을 쓴다.
     *
     * @return the bytes or null / 바이트 또는 null
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Reports whether other agents can access the same storage.
     * <p>
     * 다른 에이전트가 같은 저장소에 접근할 수 있는지 알려 준다.
     *
     * @return true for shared storage / 공유 저장소이면 true
     */
    public boolean isShared() {
        return false;
    }

    /**
     * Reads an element from this buffer.
     * <p>
     * 이 버퍼에서 요소를 읽는다.
     *
     * @param type element type / 요소 형식
     * @param offset byte position / 바이트 위치
     * @param littleEndian whether byte order is little endian / 리틀 엔디언 사용 여부
     * @return the element value / 요소 값
     */
    JsValue readValue(ElementType type, int offset, boolean littleEndian) {
        return type.readValue(data, offset, littleEndian);
    }

    /**
     * Reads an element for a Java caller expecting a double.
     * <p>
     * double을 받는 Java 호출자를 위해 요소를 읽는다.
     *
     * @param type element type / 요소 형식
     * @param offset byte position / 바이트 위치
     * @return the numeric value / 숫자 값
     */
    double readNumber(ElementType type, int offset) {
        return type.read(data, offset, true);
    }

    /**
     * Writes an element to this buffer.
     * <p>
     * 이 버퍼에 요소를 쓴다.
     *
     * @param type element type / 요소 형식
     * @param offset byte position / 바이트 위치
     * @param value value to write / 쓸 값
     * @param littleEndian whether byte order is little endian / 리틀 엔디언 사용 여부
     */
    void writeValue(ElementType type, int offset, JsValue value, boolean littleEndian) {
        type.writeValue(data, offset, value, littleEndian);
    }

    /**
     * Writes a double supplied by a Java caller.
     * <p>
     * Java 호출자가 넘긴 double을 쓴다.
     *
     * @param type element type / 요소 형식
     * @param offset byte position / 바이트 위치
     * @param value numeric value / 숫자 값
     */
    void writeNumber(ElementType type, int offset, double value) {
        type.write(data, offset, value, true);
    }

    /**
     * Reads an unsigned byte.
     * <p>
     * 부호 없는 바이트를 읽는다.
     *
     * @param offset byte position / 바이트 위치
     * @return a value between 0 and 255 / 0에서 255까지의 값
     */
    int readByte(int offset) {
        return data[offset] & 0xff;
    }

    /**
     * Writes the low eight bits of a value.
     * <p>
     * 값의 하위 8비트를 쓴다.
     *
     * @param offset byte position / 바이트 위치
     * @param value bits to write / 쓸 비트
     */
    void writeByte(int offset, int value) {
        data[offset] = (byte) value;
    }

    /**
     * Copies bytes to another range, keeping the byte order intact even if the ranges overlap.
     * <p>
     * 다른 구간으로 바이트를 복사한다. 두 구간이 겹쳐도 바이트 순서를 유지한다.
     *
     * @param from source offset / 원본 시작 위치
     * @param target destination buffer / 대상 버퍼
     * @param to destination offset / 대상 시작 위치
     * @param count number of bytes / 바이트 수
     */
    void copyBytesTo(int from, ArrayBufferObject target, int to, int count) {
        if (!isShared() && !target.isShared()) {
            System.arraycopy(data, from, target.data, to, count);
            return;
        }
        if (sharesStorageWith(target) && from < to && to - from < count) {
            // 대상이 원본의 뒤쪽과 겹치면 끝에서부터 복사한다.
            for (int i = count - 1; i >= 0; i--) {
                target.writeByte(to + i, readByte(from + i));
            }
        } else {
            // 겹치지 않거나 대상이 앞에 있으면 처음부터 복사한다.
            for (int i = 0; i < count; i++) {
                target.writeByte(to + i, readByte(from + i));
            }
        }
    }

    /**
     * Returns the storage identity for a buffer shared with other agents.
     * <p>
     * 다른 에이전트와 공유하는 버퍼의 저장소 식별 객체를 반환한다.
     *
     * @return the shared block or null for ordinary storage / 공유 블록. 일반 저장소이면 null
     */
    SharedByteBlock sharedBlock() {
        return null;
    }

    /**
     * Tests whether these buffers refer to identical storage.
     * <p>
     * 두 버퍼가 같은 저장소를 가리키는지 검사한다.
     *
     * @param other buffer to compare / 비교할 버퍼
     * @return true if the backing storage is the same / 같은 저장소를 가리키면 true
     */
    boolean sharesStorageWith(ArrayBufferObject other) {
        SharedByteBlock block = sharedBlock();
        return block != null ? block == other.sharedBlock() : data != null && data == other.data;
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
     * Returns the maximum byte length, or zero for a detached buffer.
     * <p>
     * 최대 바이트 길이를 반환한다. 버퍼가 분리되었으면 0이다.
     *
     * @return maximum byte length / 최대 바이트 길이
     */
    public int getMaxByteLength() {
        return data == null ? 0 : isResizable() ? maximum : data.length;
    }

    /**
     * Tells whether the buffer was created with a maximum length.
     * <p>
     * 버퍼를 만들 때 최대 길이를 지정했는지 알려 준다.
     *
     * @return true for a resizable buffer / 가변 버퍼이면 true
     */
    public boolean isResizable() {
        return maximum >= 0;
    }

    /**
     * Changes the length, keeping the existing prefix and zero-filling new bytes.
     * <p>
     * 기존 앞부분을 보존하고 새 바이트를 0으로 채우면서 길이를 변경한다.
     *
     * @param length new byte length / 새 바이트 길이
     */
    void resizeTo(double length) {
        if (data == null || isShared() || !isResizable()) {
            throw JsException.typeError("ArrayBuffer must be attached and resizable");
        }
        if (length < 0 || length > maximum || length != Math.floor(length)) {
            throw JsException.rangeError("New ArrayBuffer length exceeds its maximum");
        }
        if (length != data.length) {
            byte[] resized = TypedSupport.allocate(length);
            System.arraycopy(data, 0, resized, 0, Math.min(resized.length, data.length));
            data = resized;
        }
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
