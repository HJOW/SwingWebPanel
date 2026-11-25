package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * A realm-independent reference to shared bytes for a host that has checked its agent-cluster policy.
 * The receiving host can create a fresh JavaScript wrapper with its realm's intrinsic prototype.
 * <p>
 * 에이전트 클러스터 정책을 검사한 호스트가 쓰는 realm 독립 공유 바이트 참조.
 * 받는 호스트는 자기 realm의 내장 프로토타입을 사용하는 새 JavaScript 래퍼를 만들 수 있다.
 */
public final class SharedBufferReference {

    /** Bytes and waiter lists shared across wrappers. / 여러 래퍼가 공유하는 바이트와 대기열. */
    private final SharedByteBlock block;

    /** Maximum byte length, or -1 for fixed storage. / 최대 바이트 길이. 고정 저장소이면 -1. */
    private final int maximum;

    /**
     * Retains the shared data of a buffer without retaining the wrapper or its realm.
     * <p>
     * 래퍼나 realm을 유지하지 않고 버퍼의 공유 데이터만 유지한다.
     *
     * @param source a SharedArrayBuffer from this engine / 이 엔진의 SharedArrayBuffer
     * @throws IllegalArgumentException if the buffer does not contain shared storage / 공유 저장소가 없으면
     */
    public SharedBufferReference(ArrayBufferObject source) {
        if (!(source instanceof SharedArrayBufferObject)) {
            throw new IllegalArgumentException("Expected a SharedArrayBuffer");
        }
        this.block = source.sharedBlock();
        this.maximum = source.isResizable() ? source.getMaxByteLength() : -1;
    }

    /**
     * Makes a wrapper that accesses the same shared data.
     * <p>
     * 같은 공유 데이터에 접근하는 래퍼를 만든다.
     *
     * @param realm realm that receives the wrapper / 래퍼를 받을 realm
     * @return the realm's new SharedArrayBuffer wrapper / 받는 realm의 새 SharedArrayBuffer 래퍼
     */
    public ArrayBufferObject wrap(JsRealm realm) {
        return new SharedArrayBufferObject(realm.getIntrinsic("%SharedArrayBufferPrototype%"), block, maximum);
    }
}
