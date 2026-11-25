package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.duckdns.hjow.swingwebpanel.js.JobQueue;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.SimpleJobQueue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests Atomics waits, timed jobs and cancellation in independent realms.
 * <p>
 * 독립 realm에서 Atomics 대기, 예약 작업과 취소를 시험한다.
 */
public class Es2026AtomicsWaitTest {

    /**
     * Checks immediate results and the ordering of arguments and promise reactions.
     * <p>
     * 즉시 반환값, 인수 변환 순서 및 Promise 후속 작업 순서를 확인한다.
     */
    public void testImmediateAndSameAgentResults() {
        JsRealm r = JsRealm.create();
        String src = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "var arr=new Int32Array(new SharedArrayBuffer(8)); var big=new BigInt64Array(arr.buffer);"
                + "var mismatch=Atomics.waitAsync(arr,0,2), expired=Atomics.waitAsync(arr,0,0,-Infinity);"
                + "var order=''; var waiting=Atomics.waitAsync(big,0,'0');"
                + "waiting.value.then(v=>order+=v); var count=Atomics.notify(arr,0);"
                + "Promise.resolve().then(()=>order+='later');"
                + "let trace=''; const index={valueOf(){trace+='i';return 0;}};"
                + "let plain=new Int32Array(1); const nonshared=fail(()=>Atomics.wait(plain,index,0,0));"
                + "const notifyPlain=Atomics.notify(plain,index,{valueOf(){trace+='c';return 1;}});"
                + "[Object.keys(mismatch).join('/'),mismatch.async,mismatch.value,expired.async,expired.value,"
                + "waiting.async,waiting.value instanceof Promise,count,Atomics.wait(arr,1,0,0),Atomics.wait(big,0,2n),"
                + "nonshared,notifyPlain,trace,fail(()=>Atomics.wait(new BigUint64Array(arr.buffer),0,0n)),"
                + "Atomics.wait.length,Atomics.notify.length,Atomics.waitAsync.length].join('|')";
        Check.equal("async/value|false|not-equal|false|timed-out|true|true|1|timed-out|not-equal|"
                + "TypeError|0|ic|TypeError|4|3|4", evaluate(r, src), "immediate results, argument order and descriptors");
        r.runJobs();
        Check.equal("oklater", evaluate(r, "order"), "notifying on the same agent queues reactions immediately");
    }

    /**
     * Checks that a nonblocking agent rejects synchronous waits after coercion and can use waitAsync.
     * <p>
     * 차단 금지 에이전트가 인수 변환 뒤 동기 대기를 거부하고 waitAsync를 사용할 수 있는지 확인한다.
     */
    public void testAgentMayDeclineToSuspend() {
        JsRealm r = JsRealm.create();
        r.setAtomicsWaitAllowed(false);
        String src = "var a=new Int32Array(new SharedArrayBuffer(4)); let trace=''; let caught;"
                + "try{Atomics.wait(a,{valueOf(){trace+='i';return 0;}},"
                + "{valueOf(){trace+='v';return 1;}},{valueOf(){trace+='t';return 0;}});}"
                + "catch(e){caught=e.name;} [trace,caught,Atomics.waitAsync(a,0,0,0).value].join('|')";
        Check.equal("ivt|TypeError|timed-out", evaluate(r, src), "suspension policy follows argument conversion");
    }

    /**
     * Wakes several realms in FIFO order using the byte position across distinct array element widths.
     * <p>
     * 요소 너비가 다른 배열에서 같은 바이트 위치를 사용해 여러 realm을 FIFO 순서로 깨운다.
     */
    public void testFifoAndDistinctLocations() {
        SharedByteBlock block = new SharedByteBlock(16);
        JsRealm first = realmWithBuffer(block);
        JsRealm second = realmWithBuffer(block);
        JsRealm third = realmWithBuffer(block);
        JsRealm notifying = realmWithBuffer(block);
        evaluate(first, "var result='pending'; Atomics.waitAsync(new BigInt64Array(b),1,0n).value.then(v=>result=v)");
        evaluate(second, "var result='pending'; Atomics.waitAsync(new Int32Array(b),2,0).value.then(v=>result=v)");
        evaluate(third, "var result='pending'; Atomics.waitAsync(new Int32Array(b),1,0).value.then(v=>result=v)");
        Check.equal("0,0,1", evaluate(notifying, "var a=new Int32Array(b);"
                + "[Atomics.notify(a,2,-1),Atomics.notify(a,2,NaN),Atomics.notify(a,2,1.9)].join()"), "only the first waiter is selected");
        first.runJobs();
        second.runJobs();
        third.runJobs();
        Check.equal("ok", evaluate(first, "result"), "first at the location resolved");
        Check.equal("pending", evaluate(second, "result"), "second at the same location remains");
        Check.equal("pending", evaluate(third, "result"), "the other location remains");
        Check.equal("1,1,0", evaluate(notifying, "[Atomics.notify(a,2),Atomics.notify(a,1),Atomics.notify(a,2)].join()"),
                "default count wakes everyone at the specified location");
        second.runJobs();
        third.runJobs();
        Check.equal("ok", evaluate(second, "result"), "second waiter resolved");
        Check.equal("ok", evaluate(third, "result"), "different location resolved");
    }

    /**
     * Notifies a synchronous waiter between its registration and suspension.
     * <p>
     * 동기 대기자가 등록된 뒤 스레드가 멈추기 전에 통지한다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testNotificationBeforeSuspension() throws Exception {
        final SharedByteBlock block = new SharedByteBlock(4);
        final JsRealm waiting = realmWithBuffer(block);
        final CountDownLatch ready = new CountDownLatch(1);
        final CountDownLatch suspend = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<String>();
        Thread agent = new Thread(new Runnable() {
            public void run() {
                try {
                    SharedWaiters waiters = block.waiters();
                    SharedWaiters.Waiter waiter = waiters.register(waiting, block, 0, 4, 0, -1, false);
                    ready.countDown();
                    suspend.await();
                    result.set(waiters.await(waiter));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "atomics-notify-test");
        agent.setDaemon(true);
        agent.start();
        boolean registered = ready.await(5, TimeUnit.SECONDS);
        try {
            Check.isTrue(registered, "synchronous waiter has reached the wait list");
            JsRealm notifying = realmWithBuffer(block);
            Check.equal("1", evaluate(notifying, "Atomics.notify(new Int32Array(b),0,1)"), "one synchronous waiter is removed");
        } finally {
            suspend.countDown();
        }
        agent.join(5000);
        Check.isTrue(!agent.isAlive(), "the early wakeup was retained");
        Check.equal("ok", result.get(), "the synchronous agent sees the notification result");
    }

    /**
     * Checks that an asynchronous timeout removes its waiter when the timeout job runs in the owning realm.
     * <p>
     * 소유 realm에서 타임아웃 작업이 실행될 때 비동기 대기자가 제거되는 것을 확인한다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testAsyncTimeoutRunsOnOwner() throws Exception {
        SharedByteBlock block = new SharedByteBlock(4);
        JsRealm r = realmWithBuffer(block);
        final SimpleJobQueue queue = new SimpleJobQueue();
        final CountDownLatch ready = new CountDownLatch(1);
        r.setJobQueue(new JobQueue() {
            /**
             * Captures the job and signals the waiting test.
             * <p>
             * 작업을 보관하고 시험 스레드에게 알린다.
             *
             * @param job wait timeout or promise job / 대기 타임아웃 또는 Promise 작업
             */
            public void enqueue(Runnable job) {
                queue.enqueue(job);
                ready.countDown();
            }
        });
        evaluate(r, "var result='pending'; Atomics.waitAsync(new Int32Array(b),0,0,0.25).value.then(v=>result=v)");
        Check.isTrue(ready.await(5, TimeUnit.SECONDS), "timeout reached the owning queue");
        Check.equal("pending", evaluate(r, "result"), "promise is unchanged until the owner runs the timeout job");
        queue.runAll();
        Check.equal("timed-out", evaluate(r, "result"), "the timeout job resolved the promise");
        Check.equal("0", evaluate(r, "Atomics.notify(new Int32Array(b),0)"), "the timed-out waiter is gone");
    }

    /**
     * Cancels an infinite synchronous wait and a pending async timeout while another realm keeps the shared block alive.
     * <p>
     * 다른 realm이 공유 블록을 유지하는 동안 무한 동기 대기와 예약한 비동기 타임아웃을 취소한다.
     *
     * @throws Exception if the test thread is interrupted / 시험 스레드가 인터럽트되면
     */
    public void testCancellationRemovesBothKindsOfWaiters() throws Exception {
        final SharedByteBlock block = new SharedByteBlock(4);
        final JsRealm blocked = realmWithBuffer(block);
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    SharedWaiters waiters = block.waiters();
                    SharedWaiters.Waiter waiter = waiters.register(blocked, block, 0, 4, 0, -1, false);
                    ready.countDown();
                    waiters.await(waiter);
                    result.set(new AssertionError("Cancelled wait returned a JavaScript status"));
                } catch (Throwable e) {
                    result.set(e);
                }
            }
        }, "atomics-wait-test");
        thread.setDaemon(true);
        thread.start();
        JsRealm async = realmWithBuffer(block);
        evaluate(async, "Atomics.waitAsync(new Int32Array(b),0,0,1000000)");
        Check.isTrue(ready.await(5, TimeUnit.SECONDS), "synchronous wait registered");
        blocked.getLimits().cancel();
        async.getLimits().cancel();
        thread.join(5000);
        Check.isTrue(!thread.isAlive(), "cancellation woke the blocked agent");
        Check.isTrue(result.get() instanceof ScriptAbortedException, "the wait raised the engine cancellation signal");
        Check.equal(ScriptAbortedException.Reason.CANCELLED, ((ScriptAbortedException) result.get()).getReason(), "cancellation reason");
        Check.equal("0", evaluate(realmWithBuffer(block), "Atomics.notify(new Int32Array(b),0)"), "both cancelled records were removed");
    }

    /**
     * Confirms that an infinite JavaScript wait still obeys the host's execution deadline.
     * <p>
     * 무한 JavaScript 대기도 호스트의 실행 시간 한도를 따르는지 확인한다.
     */
    public void testInfiniteWaitObeysTaskLimit() {
        SharedByteBlock block = new SharedByteBlock(4);
        JsRealm r = realmWithBuffer(block);
        r.getLimits().setMaxTimeMillis(80);
        try {
            evaluate(r, "var a=new Int32Array(b); Atomics.wait(a,0,0)");
            throw new AssertionError("Infinite wait returned without a notification");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.TIME_LIMIT, e.getReason(), "waiting respects the task deadline");
        }
        Check.equal("0", evaluate(realmWithBuffer(block), "Atomics.notify(new Int32Array(b),0)"), "the limit removed the waiter");
    }

    /**
     * Creates a realm and exposes a wrapper for the test's shared block.
     * <p>
     * realm을 만들고 시험용 공유 블록의 래퍼를 노출한다.
     *
     * @param block the shared block / 공유 블록
     * @return initialized realm / 초기화한 realm
     */
    private static JsRealm realmWithBuffer(SharedByteBlock block) {
        JsRealm realm = JsRealm.create();
        realm.getGlobalObject().defineBuiltin("b", new SharedArrayBufferObject(realm.getIntrinsic("%SharedArrayBufferPrototype%"), block, -1));
        return realm;
    }

    /**
     * Evaluates a script and joins its result into the form used by the assertions.
     * <p>
     * 스크립트를 실행하고 결과를 검증에 사용하는 형태로 변환한다.
     *
     * @param realm script realm / 스크립트 realm
     * @param source test script / 시험 스크립트
     * @return result as a string / 문자열로 변환한 결과
     */
    private static String evaluate(JsRealm realm, String source) {
        return Ops.toString(realm.evaluateScript(source, "atomics-wait.js"));
    }
}
