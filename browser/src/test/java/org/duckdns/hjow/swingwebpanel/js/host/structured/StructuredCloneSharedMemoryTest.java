package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests structured clone of shared storage and the cluster boundary before deserialization.
 * <p>
 * 공유 저장소의 구조화 복제와 역직렬화 전 클러스터 경계를 검사한다.
 */
public class StructuredCloneSharedMemoryTest {

    /**
     * Preserves shared storage, wrapper identity within a graph and the bounds mode of each view.
     * <p>
     * 공유 저장소, 그래프 내부 래퍼 식별자와 뷰 각각의 경계 방식을 유지한다.
     */
    public void testSharedStorageAndLengthTracking() {
        Object cluster = new Object();
        JsRealm from = bound(cluster, "http://example.test/source");
        JsRealm to = bound(cluster, "http://example.test:80/target");
        JsValue data = from.evaluateScript("var shared=new SharedArrayBuffer(8,{maxByteLength:16});"
                + "new Uint8Array(shared).setFromHex('010203');"
                + "var g={b:shared,duplicate:shared,typed:new Uint16Array(shared,2),fixed:new Uint8Array(shared,1,2),"
                + "view:new DataView(shared,1)};g.self=g;g", "clone-source.js");
        ClonedValue cloned = StructuredClone.serializeForMessaging(from, data, 1024, null, null);
        from.evaluateScript("shared.grow(14); new Uint8Array(shared)[3]=99", "grow-before-receive.js");
        JsObject result = (JsObject) StructuredClone.deserializeForMessaging(to, cloned, null, null);
        Check.isTrue(result.get("b") != from.getGlobalObject().get("shared"), "each realm has a distinct shared buffer wrapper");
        to.getGlobalObject().defineBuiltin("g", result);
        String check = "const b=g.b; const t=g.typed; const d=g.view; const f=g.fixed;"
                + "const before=[b.byteLength,t.length,d.byteLength,f.length];"
                + "b.grow(16); f[1]=42;"
                + "[g.self===g,b===g.duplicate,b===t.buffer,b===d.buffer,b===f.buffer,"
                + "Object.getPrototypeOf(b)===SharedArrayBuffer.prototype,b.growable,b.maxByteLength,before,t.length,"
                + "d.byteLength,f.length,d.getUint8(2),d.getUint8(14)].join('|')";
        Check.equal("true|true|true|true|true|true|true|16|14,6,13,2|7|15|2|99|0",
                Ops.toString(to.evaluateScript(check, "clone-target.js")), "views use the recipient realm and keep observing growth");
        Check.equal("16,42", Ops.toString(from.evaluateScript("[shared.byteLength,new Uint8Array(shared)[2]].join()", "clone-observe.js")),
                "updates made by the receiving realm are visible to the sender");
    }

    /**
     * Denies a different origin, different host cluster, opaque peer, and the generic host/storage path.
     * <p>
     * 다른 오리진, 다른 호스트 클러스터, 불투명한 상대, 일반 호스트/저장소 경로를 거부한다.
     */
    public void testOnlyMessagingWithinSharedScopeMayRestore() {
        Object cluster = new Object();
        final JsRealm from = bound(cluster, "https://example.test/doc");
        final JsValue data = from.evaluateScript("({list:[new DataView(new SharedArrayBuffer(2))]})", "clone-boundary.js");
        final ClonedValue shared = StructuredClone.serializeForMessaging(from, data, 1024, null, null);
        rejectReceiver(bound(cluster, "https://example.test:8443/other"), shared, "a different port changes the origin");
        rejectReceiver(bound(new Object(), "https://example.test/other"), shared, "the same origin in a separate host cluster is denied");
        rejectReceiver(JsRealm.create(), shared, "an unbound realm cannot receive shared memory");
        Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                StructuredClone.serialize(from, data);
            }
        }, "storage/Java serialization rejects a nested shared view");
        Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                StructuredClone.deserialize(from, shared);
            }
        }, "storage/Java deserialization cannot restore a messaging payload with shared memory");
        JsRealm opaque = bound(cluster, "about:blank");
        ClonedValue local = StructuredClone.serializeForMessaging(opaque,
                opaque.evaluateScript("new SharedArrayBuffer(1)", "opaque-local.js"), 1024, null, null);
        Check.isTrue(((ArrayBufferObject) StructuredClone.deserializeForMessaging(opaque, local, null, null)).isShared(), "opaque realm may clone locally");
        rejectReceiver(bound(cluster, "about:blank"), local, "a different opaque realm has a different origin");
    }

    /**
     * A bad transfer item is rejected before any ordinary buffer is detached.
     * <p>
     * 잘못된 전송 항목은 일반 버퍼를 분리하기 전에 거부한다.
     */
    public void testSharedBufferCannotBeTransferred() {
        final JsRealm from = bound(new Object(), "https://example.test/");
        final JsObject ordinary = (JsObject) from.evaluateScript("new ArrayBuffer(4)", "transfer-ordinary.js");
        final JsObject shared = (JsObject) from.evaluateScript("new SharedArrayBuffer(4)", "transfer-shared.js");
        Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                StructuredClone.serializeForMessaging(from, ordinary, 1024, Arrays.asList(ordinary, shared), null);
            }
        }, "SharedArrayBuffer is cloneable but never transferable");
        Check.isTrue(!((ArrayBufferObject) ordinary).isDetached(), "ordinary entries are unchanged by a transfer-list error");
        Check.isTrue(!((ArrayBufferObject) shared).isDetached(), "shared bytes are still present");
    }

    /**
     * Creates a realm and assigns the browser's scope.
     * <p>
     * realm을 만들고 브라우저 범위를 지정한다.
     *
     * @param cluster host cluster identity / 호스트 클러스터 식별자
     * @param url URL that supplies the origin / 오리진을 제공하는 URL
     * @return new bound realm / 범위를 지정한 새 realm
     */
    private static JsRealm bound(Object cluster, String url) {
        JsRealm realm = JsRealm.create();
        SharedMemoryScope.bind(realm, cluster, Origin.of(url), true);
        return realm;
    }

    /**
     * Checks that the given scope cannot reconstitute a shared message.
     * <p>
     * 주어진 범위에서 공유 메시지를 복원할 수 없는지 검사한다.
     *
     * @param realm receiving realm / 받는 realm
     * @param data serialized value / 직렬화한 값
     * @param reason assertion description / 검증 설명
     */
    private static void rejectReceiver(final JsRealm realm, final ClonedValue data, String reason) {
        Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                StructuredClone.deserializeForMessaging(realm, data, null, null);
            }
        }, reason);
    }
}
