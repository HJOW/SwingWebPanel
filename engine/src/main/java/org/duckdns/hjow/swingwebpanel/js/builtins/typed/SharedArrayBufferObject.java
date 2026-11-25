package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * A buffer object whose bytes can be read and written by other agents.
 * <p>
 * 다른 에이전트도 바이트를 읽고 쓸 수 있는 버퍼 객체.
 */
final class SharedArrayBufferObject extends ArrayBufferObject {

    /** The storage shared among wrappers. / 여러 래퍼가 공유하는 저장소. */
    private final SharedByteBlock block;

    /** The maximum length, or -1 for fixed length. / 최대 길이. 고정 길이이면 -1. */
    private final int maximum;

    /**
     * Wraps the storage with the given prototype and growth limit.
     * <p>
     * 주어진 프로토타입과 최대 길이로 저장소를 감싼다.
     *
     * @param prototype the object prototype / 객체 프로토타입
     * @param block the shared storage / 공유 저장소
     * @param maximum the maximum length or -1 for fixed length / 최대 길이. 고정 길이이면 -1
     */
    SharedArrayBufferObject(JsObject prototype, SharedByteBlock block, int maximum) {
        super(prototype, null, maximum);
        this.block = block;
        this.maximum = maximum;
    }

    /** Returns the shared buffer class name. / 공유 버퍼 클래스 이름을 반환한다. */
    @Override
    public String className() {
        return "SharedArrayBuffer";
    }

    /** Returns a byte snapshot for the Java caller. / Java 호출자에게 현재 바이트의 복사본을 반환한다. */
    @Override
    public byte[] getData() {
        byte[] data = TypedSupport.allocate(getByteLength());
        copyBytesTo(0, new ArrayBufferObject(null, data), 0, data.length);
        return data;
    }

    /** Returns the published shared length. / 공개된 공유 바이트 길이를 반환한다. */
    @Override
    public int getByteLength() {
        return block.length();
    }

    /** Returns the maximum length of this buffer. / 이 버퍼의 최대 길이를 반환한다. */
    @Override
    public int getMaxByteLength() {
        return isResizable() ? maximum : block.length();
    }

    /** Reports that this storage can be shared. / 이 저장소를 공유할 수 있음을 알려 준다. */
    @Override
    public boolean isShared() {
        return true;
    }

    /** Always reports this buffer as attached. / 항상 분리되지 않은 버퍼로 보고한다. */
    @Override
    public boolean isDetached() {
        return false;
    }

    /** Rejects attempts to detach a shared buffer. / 공유 버퍼를 분리하려는 시도를 거부한다. */
    @Override
    public void detach() {
        throw JsException.typeError("SharedArrayBuffer cannot be detached");
    }

    /** {@inheritDoc} Reads from shared storage. / 공유 저장소에서 읽는다. */
    @Override
    JsValue readValue(ElementType type, int offset, boolean littleEndian) {
        return type.valueFromBits(block.load(offset, type.getSize(), littleEndian));
    }

    /** {@inheritDoc} Reads from shared storage. / 공유 저장소에서 읽는다. */
    @Override
    double readNumber(ElementType type, int offset) {
        return type.numberFromBits(block.load(offset, type.getSize(), true));
    }

    /** {@inheritDoc} Writes to shared storage. / 공유 저장소에 쓴다. */
    @Override
    void writeValue(ElementType type, int offset, JsValue value, boolean littleEndian) {
        block.store(offset, type.getSize(), type.bitsFromValue(value), littleEndian);
    }

    /** {@inheritDoc} Writes to shared storage. / 공유 저장소에 쓴다. */
    @Override
    void writeNumber(ElementType type, int offset, double value) {
        block.store(offset, type.getSize(), type.bitsFromNumber(value), true);
    }

    /** {@inheritDoc} Reads one shared byte. / 공유 바이트 하나를 읽는다. */
    @Override
    int readByte(int offset) {
        return (int) block.load(offset, 1, true);
    }

    /** {@inheritDoc} Writes one shared byte. / 공유 바이트 하나를 쓴다. */
    @Override
    void writeByte(int offset, int value) {
        block.store(offset, 1, value, true);
    }

    /** {@inheritDoc} Returns this object's shared block. / 이 객체의 공유 블록을 반환한다. */
    @Override
    SharedByteBlock sharedBlock() {
        return block;
    }
}
