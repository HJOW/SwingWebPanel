package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises shared buffers and views through the JavaScript built-ins.
 * <p>
 * JavaScript 내장 객체를 통해 공유 버퍼와 뷰를 검증한다.
 */
public class Es2026SharedMemoryTest {

    /**
     * Checks constructor properties, growable lengths, views and integer coercion.
     * <p>
     * 생성자 속성, 성장 가능 길이, 뷰, 정수 변환을 검증한다.
     */
    public void testSharedArrayBufferGrowthAndBrand() {
        String source = "const p=SharedArrayBuffer.prototype;"
                + "const fixed=new SharedArrayBuffer(7); const b=new SharedArrayBuffer(3,{maxByteLength:16});"
                + "const u=new Uint16Array(b,2), d=new DataView(b,2);"
                + "const first=[u.length,d.byteLength];"
                + "new Uint8Array(b).set([1,2,3]); const result=b.grow(8.9); b.grow(8);"
                + "const desc=Object.getOwnPropertyDescriptor(p,Symbol.toStringTag);"
                + "const g=Object.getOwnPropertyDescriptor(globalThis,'SharedArrayBuffer');"
                + "[SharedArrayBuffer.name,SharedArrayBuffer.length,p.slice.length,p.grow.length,"
                + "p.constructor===SharedArrayBuffer,SharedArrayBuffer[Symbol.species]===SharedArrayBuffer,"
                + "Object.prototype.toString.call(b),desc.writable,desc.enumerable,desc.configurable,"
                + "g.writable,g.enumerable,g.configurable,fixed.byteLength,fixed.maxByteLength,fixed.growable,"
                + "first,u.length,d.byteLength,b.byteLength,b.maxByteLength,b.growable,result===undefined,"
                + "new Uint8Array(b).toHex(),ArrayBuffer.isView(b),ArrayBuffer.isView(d)].join('|')";
        Check.equal("SharedArrayBuffer|1|2|1|true|true|[object SharedArrayBuffer]|false|false|true|true|false|true|"
                + "7|7|false|0,1|3|6|8|16|true|true|0102030000000000|false|true",
                eval(JsRealm.create(), source), "built-ins, lengths and retained bytes follow the shared-buffer contract");
    }

    /**
     * Checks the observable order of initial arguments and invalid length errors.
     * <p>
     * 초기 인수의 관찰 가능한 변환 순서와 잘못된 길이 오류를 검증한다.
     */
    public void testSharedConstructionAndGrowErrors() {
        String source = "function failure(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "let log=''; const fixed=new SharedArrayBuffer(0);"
                + "const length={valueOf(){log+='length,';return 4;}};"
                + "const op={get maxByteLength(){log+='get,';return {valueOf(){log+='max';return 8;}};}};"
                + "const b=new SharedArrayBuffer(length,op);"
                + "const f=failure(()=>fixed.grow({valueOf(){log+='bad';return 1;}}));"
                + "const shrink=failure(()=>b.grow({valueOf(){b.grow(8);return 6;}}));"
                + "[log,f,shrink,b.byteLength,failure(()=>SharedArrayBuffer()),"
                + "failure(()=>new SharedArrayBuffer(3,{maxByteLength:2})),"
                + "failure(()=>b.grow(9)),failure(()=>b.grow(-1)),"
                + "failure(()=>new SharedArrayBuffer(2147483640)),"
                + "failure(()=>Object.getOwnPropertyDescriptor(SharedArrayBuffer.prototype,'byteLength').get.call(new ArrayBuffer(1))),"
                + "failure(()=>ArrayBuffer.prototype.slice.call(b)),"
                + "failure(()=>ArrayBuffer.prototype.transfer.call(b)),"
                + "failure(()=>ArrayBuffer.prototype.resize.call(b,8))].join('|')";
        Check.equal("length,get,max|TypeError|RangeError|8|TypeError|RangeError|RangeError|RangeError|RangeError|"
                + "TypeError|TypeError|TypeError|TypeError", eval(JsRealm.create(), source), "type tests and conversion order");
    }

    /**
     * Checks slice length capture, distinct storage, and species returning a second wrapper of the source.
     * <p>
     * slice 길이 확정, 분리된 저장소, species가 원본 저장소의 두 번째 래퍼를 반환하는 경우를 확인한다.
     */
    public void testSharedSliceSpecies() {
        JsRealm realm = JsRealm.create();
        SharedArrayBufferObject source = (SharedArrayBufferObject) realm.evaluateScript("new SharedArrayBuffer(5,{maxByteLength:12})", "shared.js");
        realm.getGlobalObject().defineBuiltin("source", source);
        realm.getGlobalObject().defineBuiltin("alias", new SharedArrayBufferObject(realm.getIntrinsic("%SharedArrayBufferPrototype%"),
                source.sharedBlock(), 12));
        String script = "function failure(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const u=new Uint8Array(source); u.set([1,2,3,4,5]);"
                + "class S extends SharedArrayBuffer {}"
                + "source.constructor={ [Symbol.species]:S };"
                + "const s=source.slice({valueOf(){source.grow(10);return -3;}});"
                + "u[2]=9;"
                + "source.constructor={ [Symbol.species]:function(){return alias;} };"
                + "const same=failure(()=>source.slice(0,1));"
                + "source.constructor={ [Symbol.species]:function(){return new SharedArrayBuffer(0);} };"
                + "const small=failure(()=>source.slice(0,1));"
                + "source.constructor={ [Symbol.species]:ArrayBuffer };"
                + "const wrong=failure(()=>source.slice(0,1));"
                + "const ordinary=new ArrayBuffer(3);ordinary.constructor={ [Symbol.species]:SharedArrayBuffer };"
                + "[s instanceof S,s.byteLength,s.growable,new Uint8Array(s).toHex(),u[2],same,small,wrong,"
                + "failure(()=>ordinary.slice(0,1))].join('|')";
        Check.equal("true|3|false|030405|9|TypeError|TypeError|TypeError|TypeError", eval(realm, script),
                "species checks the backing identity, brand and length");
    }

    /**
     * Grows a shared buffer from two JavaScript realms on separate agents.
     * <p>
     * 별도 에이전트의 두 JavaScript realm에서 공유 버퍼를 늘린다.
     *
     * @throws Exception if the coordinating thread is interrupted / 조정 스레드가 인터럽트되면
     */
    public void testRacingGrowCalls() throws Exception {
        final SharedByteBlock block = new SharedByteBlock(4);
        block.store(0, 4, 42, true);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread[] agents = new Thread[2];
        // 작은 성장 호출은 먼저 실행되거나 큰 성장에 뒤따라 축소 오류를 낼 수 있다.
        for (int i = 0; i < agents.length; i++) {
            final int length = i == 0 ? 8192 : 16384;
            agents[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        JsRealm r = JsRealm.create();
                        r.getGlobalObject().defineBuiltin("buffer", new SharedArrayBufferObject(r.getIntrinsic("%SharedArrayBufferPrototype%"),
                                block, 16384));
                        ready.countDown();
                        start.await();
                        eval(r, "buffer.grow(" + length + ")");
                    } catch (JsException e) {
                        if (length != 8192 || e.getKind() != ErrorKind.RANGE_ERROR || block.length() != 16384) {
                            failure.compareAndSet(null, e);
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                }
            }, "shared-grow-test-" + i);
            agents[i].setDaemon(true);
            agents[i].start();
        }
        boolean started = ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        Check.isTrue(started, "both agents are ready");
        // 두 에이전트가 끝났는지 확인하고 최종 바이트를 검증한다.
        for (Thread agent : agents) {
            agent.join(5000);
            Check.isTrue(!agent.isAlive(), "grow call completed");
        }
        Check.isNull(failure.get(), "both grow calls followed the ordering rules");
        Check.equal(16384, block.length(), "larger requested length is published");
        Check.equal(42L, block.load(0, 4, true), "growth preserves the old bytes");
        Check.equal(0L, block.load(16380, 4, true), "the final word is initially zero");
    }

    /**
     * Checks integer arithmetic, bitwise operations and wrapped storage on both buffer kinds.
     * <p>
     * 두 버퍼 종류에서 정수 산술, 비트 연산과 저장 범위에 맞춘 변환을 검증한다.
     */
    public void testAtomicsIntegerOperations() {
        String source = "function run(buffer){"
                + "const a=new Int8Array(buffer,2,1); const u=new Uint8Array(buffer,2,1);"
                + "const results=[Atomics.store(a,0,255.9),Atomics.load(a,0),Atomics.add(a,0,2),Atomics.sub(a,0,2),"
                + "Atomics.and(a,0,15),Atomics.or(a,0,0x40),Atomics.xor(a,0,0x10),Atomics.exchange(a,0,255),"
                + "Atomics.compareExchange(a,0,255,128),Atomics.compareExchange(a,0,127,0),Atomics.load(u,0)];"
                + "const b=new BigInt64Array(buffer,8,1), ub=new BigUint64Array(buffer,8,1);"
                + "const wide=18446744073709551615n;"
                + "results.push(Atomics.store(b,0,wide),Atomics.load(b,0),Atomics.load(ub,0),Atomics.add(ub,0,2n),"
                + "Atomics.compareExchange(b,0,'1',true),Atomics.xor(b,0,-1n),Atomics.load(ub,0),"
                + "Object.is(Atomics.store(a,0,-0.5),0),Atomics.store(a,0,Infinity),Atomics.load(a,0));"
                + "return results.join(',');}"
                + "const s=run(new SharedArrayBuffer(16)), n=run(new ArrayBuffer(16)); [s,s===n].join('|')";
        Check.equal("255,-1,-1,1,-1,15,79,95,-1,-128,128,18446744073709551615,-1,18446744073709551615,"
                + "18446744073709551615,1,1,18446744073709551614,true,Infinity,0|true",
                eval(JsRealm.create(), source), "Atomics returns the old value or the unwrapped store argument");
    }

    /**
     * Checks argument ordering, buffer witness revalidation and built-in descriptors.
     * <p>
     * 인수 변환 순서, 버퍼 경계 재검사와 내장 객체 기술자를 검증한다.
     */
    public void testAtomicsArgumentOrdering() {
        String script = "function failure(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "let trace=''; const invalid={valueOf(){trace+='bad';return 0;}};"
                + "const wrong=failure(()=>Atomics.load(new Float16Array(1),invalid));"
                + "const clamped=failure(()=>Atomics.xor(new Uint8ClampedArray(1),invalid,1));"
                + "const s=new SharedArrayBuffer(4,{maxByteLength:8}), gs=new Int32Array(s);"
                + "const captured=failure(()=>Atomics.load(gs,{valueOf(){s.grow(8);return 1;}}));"
                + "const b=new ArrayBuffer(8,{maxByteLength:16}), view=new Int32Array(b);"
                + "const index={valueOf(){trace+='i';return 1;}}, expected={valueOf(){trace+='e';b.resize(5);return 0;}},"
                + "replacement={valueOf(){trace+='r';return 7;}};"
                + "const partial=failure(()=>Atomics.compareExchange(view,index,expected,replacement));"
                + "const fixed=new Int32Array(b,0,1);"
                + "const detached=failure(()=>Atomics.store(fixed,0,{valueOf(){b.transfer();return 1;}}));"
                + "const mixed=failure(()=>Atomics.add(new BigInt64Array(1),0,1));"
                + "const desc=Object.getOwnPropertyDescriptor(Atomics,Symbol.toStringTag);"
                + "[typeof Atomics,Atomics.compareExchange.length,Atomics.load.length,Atomics.store.length,"
                + "Object.prototype.toString.call(Atomics),desc.writable,desc.enumerable,desc.configurable,"
                + "Atomics.isLockFree(4.8),Atomics.isLockFree(3),Atomics.load(gs,1),trace,wrong,clamped,captured,partial,detached,mixed].join('|')";
        Check.equal("object|4|2|3|[object Atomics]|false|false|true|true|false|0|ier|"
                + "TypeError|TypeError|RangeError|RangeError|TypeError|TypeError", eval(JsRealm.create(), script),
                "type check, captured length and post-conversion byte bounds");
    }

    /**
     * Runs Atomics operations from two concurrent JavaScript agents sharing adjacent fields.
     * <p>
     * 이웃 필드를 공유하는 두 JavaScript 에이전트에서 원자 연산을 동시에 실행한다.
     *
     * @throws Exception if the coordinating thread is interrupted / 조정 스레드가 인터럽트되면
     */
    public void testConcurrentAtomics() throws Exception {
        final SharedByteBlock block = new SharedByteBlock(16);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread[] agents = new Thread[2];
        // 두 에이전트가 4바이트 카운터를 함께 증가시키고 나머지 독립 필드를 갱신한다.
        for (int i = 0; i < agents.length; i++) {
            final int index = i;
            agents[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        JsRealm r = JsRealm.create();
                        r.getGlobalObject().defineBuiltin("buffer", new SharedArrayBufferObject(r.getIntrinsic("%SharedArrayBufferPrototype%"),
                                block, -1));
                        ready.countDown();
                        start.await();
                        eval(r, "const both=new Uint32Array(buffer,0,1), own=new Uint16Array(buffer,4,2), "
                                + "big=new BigUint64Array(buffer,8,1);"
                                + "for(var i=0;i<5000;i++){Atomics.add(both,0,1);Atomics.add(own," + index
                                + ",2);Atomics.add(big,0,1n);}");
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                }
            }, "atomics-test-" + i);
            agents[i].setDaemon(true);
            agents[i].start();
        }
        boolean started = ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        Check.isTrue(started, "both agents are ready");
        // 모든 원자 연산이 끝난 뒤 각 필드를 확인한다.
        for (Thread agent : agents) {
            agent.join(10000);
            Check.isTrue(!agent.isAlive(), "atomic operations completed");
        }
        Check.isNull(failure.get(), "agents executed successfully");
        Check.equal(10000L, block.load(0, 4, true), "competing Uint32Array updates are never lost");
        Check.equal(10000L, block.load(4, 2, true), "adjacent Uint16Array field of the first agent");
        Check.equal(10000L, block.load(6, 2, true), "adjacent Uint16Array field of the second agent");
        Check.equal(10000L, block.load(8, 8, true), "competing BigUint64Array updates are never lost");
    }

    /**
     * Evaluates a test script and converts its result to a string for comparison.
     * <p>
     * 시험 스크립트를 평가한 뒤 결과를 비교할 문자열로 변환한다.
     *
     * @param realm realm for evaluation / 평가할 realm
     * @param script JavaScript source / JavaScript 소스
     * @return string result / 문자열 결과
     */
    private static String eval(JsRealm realm, String script) {
        return Ops.toString(realm.evaluateScript(script, "shared-memory.js"));
    }
}
