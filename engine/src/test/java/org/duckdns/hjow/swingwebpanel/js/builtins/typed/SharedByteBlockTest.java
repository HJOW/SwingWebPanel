package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises shared byte storage across threads, word widths and growth boundaries.
 * <p>
 * 스레드, 워드 너비, 성장 경계에 걸친 공유 바이트 저장소를 검증한다.
 */
public class SharedByteBlockTest {

    /**
     * Checks shared DataView, typed array and encoding methods over the same bytes.
     * <p>
     * 같은 바이트에 접근하는 공유 DataView, 형식화 배열, 인코딩 메서드를 검증한다.
     */
    public void testViewsAndEncoding() {
        JsRealm realm = JsRealm.create();
        SharedByteBlock block = new SharedByteBlock(12);
        SharedArrayBufferObject buffer = new SharedArrayBufferObject(realm.getObjectPrototype(), block, 24);
        realm.getGlobalObject().defineBuiltin("shared", buffer);
        realm.getGlobalObject().defineBuiltin("alias", new SharedArrayBufferObject(realm.getObjectPrototype(), block, 24));
        String source = "var u=new Uint8Array(shared); var a=new Uint8Array(alias); var fixed=new Uint8Array(shared,0,2);"
                + "u.setFromHex('01020304'); a.copyWithin(1,0,4); u.set(a.subarray(1,5), 2);"
                + "const copy = new Uint8Array(u.subarray(0,6));"
                + "a.setFromBase64('AAEC/w==');"
                + "var d=new DataView(shared); d.setBigInt64(3,0x102030405060708n);"
                + "[copy.toHex(),u.slice(0,3).toBase64(),d.getBigInt64(3).toString(16),d.getUint16(3,true)].join('|')";
        Check.equal("010101020304|AAEC|102030405060708|513", Ops.toString(realm.evaluateScript(source, "shared-views.js")),
                "overlap with shared storage, decoding and copying preserve the observed bytes");
        Check.equal("01ff", Ops.toString(realm.evaluateScript("u.setFromHex('0102ff03');"
                + "new Uint8Array(shared,2,2).set(new Uint16Array(alias,0,2)); u.subarray(2,4).toHex()", "shared-set.js")),
                "different element types snapshot the overlapping shared byte range");
        block.grow(16);
        Check.equal("16,16,2,16,0", Ops.toString(realm.evaluateScript("[u.length,a.length,fixed.length,d.byteLength,u[15]].join()",
                "shared-grow.js")), "all automatic views reflect growth and the explicit length is preserved");
        byte[] snapshot = buffer.getData();
        snapshot[1] = 19;
        Check.equal(2, buffer.readByte(1), "mutating a host copy cannot change shared bytes");
        Check.isNull(TypedArrays.bytesOf(buffer), "a shared buffer has no live Java byte window");
        Check.isNull(TypedArrays.bytesOf(realm.getGlobalObject().get("u")), "a shared view has no live Java byte window");
    }

    /**
     * An unaligned read and write can span pages while preserving the requested byte order.
     * <p>
     * 페이지를 넘는 비정렬 읽기와 쓰기에서도 요청한 바이트 순서를 유지한다.
     */
    public void testUnalignedAcrossPages() {
        final SharedByteBlock block = new SharedByteBlock(4095);
        block.store(4093, 2, 0xabcd, false);
        block.grow(4098);
        Check.equal(0xabcdL, block.load(4093, 2, false), "growth retains the end of the old page");
        Check.equal(0L, block.load(4095, 3, true), "new bytes start at zero");
        block.store(4094, 4, 0x12345678L, false);
        Check.equal(0x12345678L, block.load(4094, 4, false), "read back across both words and pages");
        Check.equal(0x78563412L, block.load(4094, 4, true), "opposite endian view of the same bytes");
        Check.equal(0xabL, block.load(4093, 1, true), "an adjacent byte is unchanged");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                block.grow(4097);
            }
        }, "growth cannot shrink the directory");
    }

    /**
     * Updates two overlapping word fields from multiple threads while growing the accessible byte range.
     * <p>
     * 접근할 수 있는 범위를 넓히는 도중 여러 스레드가 같은 워드의 필드를 갱신한다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testConcurrentAtomicFieldsAndGrowth() throws Exception {
        final SharedByteBlock block = new SharedByteBlock(8);
        final CountDownLatch ready = new CountDownLatch(3);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final int iterations = 20000;
        block.store(6, 2, 0xbeef, true);
        Thread[] writers = new Thread[3];
        // 두 스레드는 낮은 4바이트를 함께 증가시키고 하나는 같은 워드의 다른 2바이트를 갱신한다.
        for (int i = 0; i < writers.length; i++) {
            final int offset = i == 2 ? 4 : 0;
            final int width = i == 2 ? 2 : 4;
            writers[i] = new Thread(new Runnable() {
                public void run() {
                    ready.countDown();
                    try {
                        start.await();
                        // 동일한 필드를 두 스레드가 읽어도 성공한 한 곳의 갱신만 반영한다.
                        for (int n = 0; n < iterations; n++) {
                            long previous;
                            do {
                                previous = block.load(offset, width, true);
                            } while (block.compareExchange(offset, width, previous, previous + 1) != previous); // 경쟁으로 바뀌었으면 다시 읽는다.
                        }
                    } catch (Throwable failure) {
                        error.compareAndSet(null, failure);
                    }
                }
            }, "shared-byte-block-test-" + i);
            writers[i].setDaemon(true);
            writers[i].start();
        }
        Check.isTrue(ready.await(5, TimeUnit.SECONDS), "all writers reached the barrier");
        start.countDown();
        block.grow(8193);
        // 세 스레드가 제한 시간 안에 종료하는지 확인한다.
        for (Thread writer : writers) {
            writer.join(5000);
            Check.isTrue(!writer.isAlive(), "writer completed the atomic operations");
        }
        Check.isNull(error.get(), "writers did not throw");
        Check.equal(2L * iterations, block.load(0, 4, true), "same field increments are never lost");
        Check.equal((long) iterations, block.load(4, 2, true), "the adjacent counter is preserved");
        Check.equal(0xbeefL, block.load(6, 2, true), "neither counter overwrote the high bytes");
        Check.equal(0L, block.load(8192, 1, true), "growth zeroed the final new page");
    }
}
