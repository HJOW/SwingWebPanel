package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * The FIFO wait lists by byte offset in one shared block.
 * <p>
 * 한 공유 블록에서 바이트 위치별로 유지하는 FIFO 대기열.
 */
final class SharedWaiters {

    /** Status for a wait ended by notification. / 통지로 끝난 대기 상태. */
    private static final String OK = "ok";

    /** Status for a wait whose expected value differs. / 기대한 값이 다를 때의 대기 상태. */
    private static final String NOT_EQUAL = "not-equal";

    /** Status for a wait whose timeout expired. / 제한 시간이 지난 대기 상태. */
    private static final String TIMED_OUT = "timed-out";

    /** Status used internally to release a cancelled wait. / 취소된 대기를 해제하는 내부 상태. */
    private static final String CANCELLED = "cancelled";

    /**
     * Allocates the daemon timer only when an asynchronous timeout is needed.
     * <p>
     * 비동기 타임아웃이 필요할 때만 데몬 타이머를 할당한다.
     */
    private static final class Timeouts {
        /** Timer for finite asynchronous waits. / 유한 비동기 대기의 타이머. */
        static final ScheduledThreadPoolExecutor TIMER = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            /**
             * Creates the timer thread.
             * <p>
             * 타이머 스레드를 만든다.
             *
             * @param task timer worker / 타이머 작업자
             * @return daemon worker / 데몬 작업자
             */
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "swingwebpanel-atomics-timeouts");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * One synchronous or asynchronous waiter. State is protected by the owning wait lists.
     * <p>
     * 동기 또는 비동기 대기자 하나. 상태는 소유 대기열의 잠금으로 보호한다.
     */
    final class Waiter implements Runnable {
        /** Byte offset key for the wait list. / 대기열을 찾을 바이트 위치. */
        final Integer location;

        /** The owning agent's realm. / 대기 에이전트의 realm. */
        final JsRealm realm;

        /** Time at which the agent entered the wait list. / 에이전트가 대기열에 들어간 시각. */
        final long started = System.nanoTime();

        /** Nanoseconds until timeout, or -1 for infinity. / 타임아웃까지의 나노초. 무한이면 -1. */
        final long timeout;

        /** Promise capability for a pending asynchronous call. / 진행 중인 비동기 호출의 Promise capability. */
        PromiseCapability promise;

        /** Timer entry for a finite asynchronous call. / 유한 비동기 호출의 타이머 항목. */
        ScheduledFuture<?> timer;

        /** Final status, or null while pending. / 최종 상태. 아직 대기 중이면 null. */
        String status;

        /**
         * Creates a record for a byte offset and agent.
         * <p>
         * 바이트 위치와 에이전트에 대한 대기 기록을 만든다.
         *
         * @param location byte offset key / 바이트 위치 키
         * @param realm waiting realm / 대기할 realm
         * @param timeout nanoseconds until timeout or -1 for infinity / 타임아웃까지의 나노초. 무한이면 -1
         */
        Waiter(Integer location, JsRealm realm, long timeout) {
            this.location = location;
            this.realm = realm;
            this.timeout = timeout;
        }

        /**
         * Removes this waiter when the host cancels the realm.
         * <p>
         * 호스트가 realm을 취소하면 이 대기자를 제거한다.
         */
        public void run() {
            synchronized (SharedWaiters.this) {
                finish(this, CANCELLED);
            }
        }
    }

    /** Current nonempty queues indexed by byte offset. / 바이트 위치별 현재 비어 있지 않은 대기열. */
    private final Map<Integer, LinkedList<Waiter>> queues = new HashMap<Integer, LinkedList<Waiter>>();

    /**
     * Checks the current value and atomically registers a waiter relative to notifications.
     * <p>
     * 현재 값을 확인하고 통지 연산을 사이에 넣지 않은 채 대기자를 등록한다.
     *
     * @param realm waiting realm / 대기할 realm
     * @param block shared storage / 공유 저장소
     * @param offset byte position / 바이트 위치
     * @param width element width / 요소 너비
     * @param expected expected bits / 기대한 비트
     * @param timeout nanoseconds to wait or -1 for infinity / 대기 시간(나노초). 무한이면 -1
     * @param async whether to return a promise for a registered waiter / 등록된 대기자의 Promise 반환 여부
     * @return the registered or already finished record / 등록했거나 이미 끝난 대기 기록
     */
    synchronized Waiter register(JsRealm realm, SharedByteBlock block, int offset, int width, long expected, long timeout, boolean async) {
        final Waiter waiter = new Waiter(Integer.valueOf(offset), realm, timeout);
        if (block.load(offset, width, true) != (expected & SharedByteBlock.mask(width))) {
            waiter.status = NOT_EQUAL;
            return waiter;
        }
        if (async && timeout == 0) {
            waiter.status = TIMED_OUT;
            return waiter;
        }
        if (async) {
            waiter.promise = PromiseInterop.newCapability(realm);
        }
        ExecutionLimits limits = realm.getLimits();
        if (!limits.addCancellationHook(waiter)) {
            throw cancelled();
        }
        LinkedList<Waiter> queue = queues.get(waiter.location);
        if (queue == null) {
            queue = new LinkedList<Waiter>();
            queues.put(waiter.location, queue);
        }
        queue.addLast(waiter);
        if (async && timeout >= 0) {
            waiter.timer = Timeouts.TIMER.schedule(new Runnable() {
                /**
                 * Enqueues the timeout on the waiting agent.
                 * <p>
                 * 대기 중인 에이전트의 큐에 시간 초과 작업을 넣는다.
                 */
                public void run() {
                    enqueueTimeout(waiter);
                }
            }, timeout, TimeUnit.NANOSECONDS);
        }
        return waiter;
    }

    /**
     * Suspends the current agent until completion, cancellation or timeout.
     * <p>
     * 대기 완료, 취소 또는 시간 초과까지 현재 에이전트를 정지한다.
     *
     * @param waiter the synchronous waiter record / 동기 대기 기록
     * @return the final script-visible status / 스크립트에서 볼 수 있는 최종 상태
     */
    synchronized String await(Waiter waiter) {
        if (waiter.status == NOT_EQUAL) {
            return waiter.status;
        }
        ExecutionLimits limits = waiter.realm.getLimits();
        long timeout = waiter.timeout;
        try {
            // 통지, 취소 또는 기한까지 잠금을 놓고 기다리며 깨어날 때마다 상태를 확인한다.
            while (waiter.status == null) {
                long remaining = timeout < 0 ? Long.MAX_VALUE : timeout - (System.nanoTime() - waiter.started);
                long taskMillis = limits.remainingTaskTimeMillis();
                if (remaining <= 0) {
                    finish(waiter, TIMED_OUT);
                    break;
                }
                if (timeout < 0 && taskMillis < 0) {
                    wait();
                } else {
                    long delay = taskMillis < 0 ? remaining : Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(taskMillis));
                    wait(delay / 1000000L, (int) (delay % 1000000L));
                }
            }
            if (waiter.status == CANCELLED) {
                throw cancelled();
            }
            limits.poll();
            return waiter.status;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw cancelled();
        } finally {
            finish(waiter, CANCELLED);
        }
    }

    /**
     * Selects up to count waiters in enqueue order and wakes or resolves them.
     * <p>
     * 등록 순서로 최대 count명의 대기자를 골라 깨우거나 Promise를 해결한다.
     *
     * @param notifyingRealm realm making the notification / 통지하는 realm
     * @param offset byte position / 바이트 위치
     * @param count maximum number to wake / 깨울 최대 개수
     * @return number removed from the queue / 대기열에서 제거한 수
     */
    int notifyAt(JsRealm notifyingRealm, int offset, double count) {
        List<Waiter> resolve = null;
        int removed;
        synchronized (this) {
            LinkedList<Waiter> queue = queues.get(Integer.valueOf(offset));
            removed = queue == null ? 0 : (int) Math.min(queue.size(), count);
            // 잠금 안에서 앞쪽 대기자를 제거하므로 다른 통지나 시간 초과와 중복되지 않는다.
            for (int i = 0; i < removed; i++) {
                Waiter waiter = queue.getFirst();
                finish(waiter, OK);
                if (waiter.promise != null) {
                    if (resolve == null) {
                        resolve = new ArrayList<Waiter>();
                    }
                    resolve.add(waiter);
                }
            }
        }
        // Promise 해결과 호스트 스케줄링은 잠금 밖에서 선택 순서대로 실행한다.
        if (resolve != null) {
            for (final Waiter waiter : resolve) {
                if (waiter.realm == notifyingRealm) {
                    resolve(waiter, OK);
                } else {
                    waiter.realm.enqueueExternalJob(new Runnable() {
                        /** Resolves the waiter on its agent. / 소유 에이전트에서 대기를 해결한다. */
                        public void run() {
                            resolve(waiter, OK);
                        }
                    });
                }
            }
        }
        return removed;
    }

    /**
     * Submits the timeout job without touching a promise on the timer thread.
     * <p>
     * 타이머 스레드에서 Promise를 직접 다루지 않고 시간 초과 작업을 등록한다.
     *
     * @param waiter the asynchronous waiter / 비동기 대기자
     */
    private void enqueueTimeout(final Waiter waiter) {
        waiter.realm.enqueueExternalJob(new Runnable() {
            /** Ends a wait that is still pending on its agent. / 소유 에이전트에서 아직 진행 중인 대기를 끝낸다. */
            public void run() {
                boolean changed;
                synchronized (SharedWaiters.this) {
                    changed = finish(waiter, TIMED_OUT);
                }
                if (changed) {
                    resolve(waiter, TIMED_OUT);
                }
            }
        });
    }

    /**
     * Records the result and releases associated resources with the wait-list lock held.
     * <p>
     * 대기열 잠금을 쥔 채 결과를 기록하고 연결된 자원을 해제한다.
     *
     * @param waiter the record to release / 해제할 대기 기록
     * @param status final status / 최종 상태
     * @return true if this call ended the wait / 이 호출이 대기를 끝냈으면 true
     */
    private boolean finish(Waiter waiter, String status) {
        if (waiter.status != null) {
            return false;
        }
        waiter.status = status;
        LinkedList<Waiter> queue = queues.get(waiter.location);
        queue.remove(waiter);
        if (queue.isEmpty()) {
            queues.remove(waiter.location);
        }
        waiter.realm.getLimits().removeCancellationHook(waiter);
        if (waiter.timer != null) {
            // Java 6에서는 취소한 타이머 항목을 지연 큐에서도 직접 제거해야 한다.
            waiter.timer.cancel(false);
            Timeouts.TIMER.remove((Runnable) waiter.timer);
            waiter.timer = null;
        }
        if (waiter.promise == null) {
            notifyAll();
        }
        return true;
    }

    /**
     * Resolves the waiter's promise on the receiving agent.
     * <p>
     * 받는 에이전트에서 대기의 Promise를 해결한다.
     *
     * @param waiter asynchronous waiter / 비동기 대기자
     * @param status script-visible final status / 스크립트에서 볼 수 있는 최종 상태
     */
    private static void resolve(Waiter waiter, String status) {
        if (!waiter.realm.getLimits().isCancelled()) {
            waiter.promise.resolve(Js.str(status));
        }
    }

    /**
     * Creates the engine's uncatchable cancellation signal.
     * <p>
     * 스크립트에서 잡을 수 없는 엔진 취소 신호를 만든다.
     *
     * @return cancellation failure / 취소 예외
     */
    private static ScriptAbortedException cancelled() {
        return new ScriptAbortedException(ScriptAbortedException.Reason.CANCELLED, "Script execution was cancelled during Atomics.wait");
    }
}
