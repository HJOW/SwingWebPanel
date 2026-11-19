package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;

/**
 * The timers of a worker: <code>setTimeout</code>, <code>setInterval</code>, <code>clearTimeout</code>, <code>clearInterval</code> and
 * <code>queueMicrotask</code>. A callback runs as a task of the worker on the worker's own thread, followed by the microtask checkpoint, and
 * never after the worker ended. Timers nested five levels deep are clamped to 4 ms like in a page; a callback that throws is reported as an
 * error of the worker and an interval keeps running. At most {@link WorkerLimits#MAX_TIMERS} timers can be pending.
 * <p>
 * worker 의 타이머: <code>setTimeout</code>, <code>setInterval</code>, <code>clearTimeout</code>, <code>clearInterval</code>, <code>queueMicrotask</code>.
 * 콜백은 worker 자신의 스레드에서 worker 의 작업으로 실행되고 마이크로태스크 검사점이 뒤따르며 worker 가 끝난 뒤에는 실행되지 않는다. 페이지처럼
 * 5단계 중첩된 타이머는 4 ms 로 제한한다. 콜백이 던진 오류는 worker 의 오류로 보고하며 interval 은 계속 돈다. 대기 중인 타이머는 최대
 * {@link WorkerLimits#MAX_TIMERS} 개이다.
 */
final class WorkerTimers {

    /**
     * One timer.
     * <p>
     * 타이머 하나.
     */
    private static final class Timer {
        /**
         * The timer id.
         * <p>
         * 타이머 id.
         */
        int id;

        /**
         * The callback function or script text.
         * <p>
         * 콜백 함수 또는 스크립트 텍스트.
         */
        JsValue callback;

        /**
         * The extra arguments of the callback.
         * <p>
         * 콜백의 추가 인수.
         */
        JsValue[] args;

        /**
         * True for an interval.
         * <p>
         * interval 이면 true.
         */
        boolean repeat;

        /**
         * The delay in milliseconds.
         * <p>
         * 지연(밀리초).
         */
        long delay;

        /**
         * The nesting level of the timer.
         * <p>
         * 타이머의 중첩 수준.
         */
        int nesting;

        /**
         * The scheduled task.
         * <p>
         * 예약된 작업.
         */
        Scheduler.Handle handle;
    }

    /**
     * The worker.
     * <p>
     * 이 타이머가 속한 worker.
     */
    private final WorkerScope scope;

    /**
     * The pending timers by id; touched only on the thread of the worker.
     * <p>
     * id 별 대기 중인 타이머. worker 의 스레드에서만 만진다.
     */
    private final Map<Integer, Timer> timers = new HashMap<Integer, Timer>();

    /**
     * The next timer id.
     * <p>
     * 다음 타이머 id.
     */
    private int nextId = 1;

    /**
     * The nesting level of the timer callback that is running.
     * <p>
     * 실행 중인 타이머 콜백의 중첩 수준.
     */
    private int currentNesting;

    /**
     * Creates the timers of a worker.
     * <p>
     * worker 의 타이머를 만든다.
     *
     * @param scope the worker / worker
     */
    WorkerTimers(WorkerScope scope) {
        this.scope = scope;
    }

    /**
     * Installs the functions on the global object of the worker.
     * <p>
     * worker 의 전역 객체에 함수를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = scope.getRealm();
        g.defineBuiltin("setTimeout", realm.newFunction("setTimeout", 1, new NativeFunction.Call() {
            /**
             * Schedules a one-shot timer.
             * <p>
             * 한 번 실행하는 타이머를 예약한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the timer id / 타이머 id
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(add(args, false));
            }
        }));
        g.defineBuiltin("setInterval", realm.newFunction("setInterval", 1, new NativeFunction.Call() {
            /**
             * Schedules a repeating timer.
             * <p>
             * 반복 타이머를 예약한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the timer id / 타이머 id
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(add(args, true));
            }
        }));
        NativeFunction.Call clear = new NativeFunction.Call() {
            /**
             * Cancels a timer.
             * <p>
             * 타이머를 취소한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                clear((int) Ops.toInteger(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        };
        g.defineBuiltin("clearTimeout", realm.newFunction("clearTimeout", 1, clear));
        g.defineBuiltin("clearInterval", realm.newFunction("clearInterval", 1, clear));
        g.defineBuiltin("queueMicrotask", realm.newFunction("queueMicrotask", 1, new NativeFunction.Call() {
            /**
             * Queues a microtask.
             * <p>
             * 마이크로태스크를 대기열에 넣는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                final JsValue fn = Js.arg(args, 0);
                if (!fn.isCallable()) {
                    throw JsException.typeError("Failed to execute 'queueMicrotask' on 'WorkerGlobalScope': The callback provided as parameter 1 "
                            + "is not a function.");
                }
                scope.getRealm().enqueueJob(new Runnable() {
                    /**
                     * Calls the function; its error is reported as an error of the worker.
                     * <p>
                     * 함수를 부른다. 오류는 worker 의 오류로 보고한다.
                     */
                    public void run() {
                        try {
                            Ops.call(fn, Js.UNDEFINED, new JsValue[0]);
                        } catch (JsException e) {
                            scope.reportError(e);
                        }
                    }
                });
                return Js.UNDEFINED;
            }
        }));
    }

    /**
     * Adds a timer from the script arguments.
     * <p>
     * 스크립트 인수로 타이머를 추가한다.
     *
     * @param args   the script arguments / 스크립트 인수
     * @param repeat true for setInterval / setInterval 이면 true
     * @return the id / id
     */
    private int add(JsValue[] args, boolean repeat) {
        JsValue cb = Js.arg(args, 0);
        double d = Ops.toNumber(Js.arg(args, 1));
        long delay = Double.isNaN(d) || d < 0 ? 0 : (long) Math.min(d, 2147483647.0);
        if (timers.size() >= WorkerLimits.MAX_TIMERS) {
            throw JsException.rangeError("Too many timers");
        }
        Timer t = new Timer();
        t.id = nextId++;
        t.callback = cb;
        t.args = args.length > 2 ? java.util.Arrays.copyOfRange(args, 2, args.length) : new JsValue[0];
        t.repeat = repeat;
        t.nesting = currentNesting + 1;
        t.delay = delay;
        timers.put(Integer.valueOf(t.id), t);
        schedule(t);
        return t.id;
    }

    /**
     * Schedules the next run of a timer.
     * <p>
     * 타이머의 다음 실행을 예약한다.
     *
     * @param t the timer / 타이머
     */
    private void schedule(final Timer t) {
        long delay = t.delay;
        if (t.nesting >= 5 && delay < 4) {
            delay = 4;
        }
        if (t.repeat && delay < 1) {
            delay = 1;
        }
        t.handle = scope.schedule(new Runnable() {
            /**
             * Runs the callback if the timer is still pending.
             * <p>
             * 타이머가 아직 대기 중이면 콜백을 실행한다.
             */
            public void run() {
                fire(t);
            }
        }, delay);
    }

    /**
     * Runs a timer callback.
     * <p>
     * 타이머 콜백을 실행한다.
     *
     * @param t the timer / 타이머
     */
    private void fire(Timer t) {
        if (timers.get(Integer.valueOf(t.id)) != t) {
            return;
        }
        if (t.repeat) {
            t.nesting++;
            schedule(t);
        } else {
            timers.remove(Integer.valueOf(t.id));
        }
        int prev = currentNesting;
        currentNesting = t.nesting;
        try {
            if (t.callback.isCallable()) {
                Ops.call(t.callback, scope.getRealm().getGlobalObject(), t.args);
            } else {
                scope.getRealm().evaluateScript(Ops.toString(t.callback), "timer");
            }
        } catch (JsException e) {
            scope.reportError(e);
        } finally {
            currentNesting = prev;
        }
    }

    /**
     * Cancels a timer.
     * <p>
     * 타이머를 취소한다.
     *
     * @param id the id / id
     */
    private void clear(int id) {
        Timer t = timers.remove(Integer.valueOf(id));
        if (t != null && t.handle != null) {
            t.handle.cancel();
        }
    }

}
