package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Timers and animation frames of one page (plan items P15-06, P15-07): <code>setTimeout</code>, <code>setInterval</code>, their
 * <code>clear...</code> functions, <code>requestAnimationFrame</code>, <code>cancelAnimationFrame</code> and <code>queueMicrotask</code>.
 * Every callback runs as a page task ({@link PageScripts#runTask(Runnable)}), so it is serialized with all other script code, followed by a
 * microtask checkpoint, and it never runs after the page was disposed even if the scheduler had already queued it. Timers that are nested
 * five levels deep are clamped to 4 ms; a timer whose callback throws is reported and an interval keeps running.
 * <p>
 * 한 페이지의 타이머와 애니메이션 프레임(계획 P15-06, P15-07): <code>setTimeout</code>, <code>setInterval</code>, <code>clear...</code> 함수,
 * <code>requestAnimationFrame</code>, <code>cancelAnimationFrame</code>, <code>queueMicrotask</code>. 모든 콜백은 페이지 작업
 * ({@link PageScripts#runTask(Runnable)})으로 실행되므로 다른 스크립트 코드와 직렬화되고 뒤이어 마이크로태스크 검사점이 오며, 스케줄러가 이미 대기열에
 * 넣었더라도 페이지가 폐기된 뒤에는 실행되지 않는다. 5단계 중첩된 타이머는 4 ms 로 제한하며 콜백이 던진 타이머는 보고하되 interval 은 계속 돈다.
 */
final class Timers {

    /**
     * Maximum number of pending timers (protects the memory of the process from runaway scripts).
     * <p>
     * 대기 중인 타이머의 최대 수(폭주하는 스크립트로부터 프로세스 메모리를 보호한다).
     */
    private static final int MAX_TIMERS = 100000;

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
         * The extra arguments.
         * <p>
         * 추가 인수.
         */
        JsValue[] args;
        /**
         * True for intervals.
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
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Pending timers by id.
     * <p>
     * id 별 대기 중인 타이머.
     */
    private final Map<Integer, Timer> timers = new HashMap<Integer, Timer>();

    /**
     * Pending animation frame callbacks by id (in registration order).
     * <p>
     * id 별 대기 중인 애니메이션 프레임 콜백(등록 순서).
     */
    private final Map<Integer, JsObject> frames = new LinkedHashMap<Integer, JsObject>();

    /**
     * The next timer id.
     * <p>
     * 다음 타이머 id.
     */
    private int nextId = 1;

    /**
     * The next animation frame id.
     * <p>
     * 다음 애니메이션 프레임 id.
     */
    private int nextFrameId = 1;

    /**
     * The nesting level of the timer callback that is running.
     * <p>
     * 실행 중인 타이머 콜백의 중첩 수준.
     */
    private int currentNesting;

    /**
     * The task of the next animation frame, or null.
     * <p>
     * 다음 애니메이션 프레임의 작업. 없으면 null.
     */
    private Scheduler.Handle frameHandle;

    /**
     * The receiver of the Web Animations part of each animation frame (P10-08-h), or null: it runs before the animation frame callbacks of the frame.
     * <p>
     * 각 애니메이션 프레임의 Web Animations 부분을 받는 쪽(P10-08-h). 없으면 null. 프레임의 애니메이션 프레임 콜백보다 먼저 실행된다.
     */
    private org.duckdns.hjow.swingwebpanel.js.host.anim.AnimationPage.FrameHook animationHook;

    /**
     * The idle callbacks that wait for their turn, by id (<code>requestIdleCallback</code>).
     * <p>
     * 차례를 기다리는 유휴 콜백(<code>requestIdleCallback</code>). id 별.
     */
    private final Map<Integer, Scheduler.Handle> idle = new HashMap<Integer, Scheduler.Handle>();

    /**
     * The id of the next idle callback.
     * <p>
     * 다음 유휴 콜백의 id.
     */
    private int nextIdleId = 1;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * Creates the timers of a page.
     * <p>
     * 페이지의 타이머를 만든다.
     *
     * @param page the page / 페이지
     */
    Timers(PageScripts page) {
        this.page = page;
    }

    /**
     * Installs the functions on the global object.
     * <p>
     * 전역 객체에 함수를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
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
        g.defineBuiltin("requestAnimationFrame", realm.newFunction("requestAnimationFrame", 1, new NativeFunction.Call() {
            /**
             * Registers an animation frame callback.
             * <p>
             * 애니메이션 프레임 콜백을 등록한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the id / id
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'requestAnimationFrame' on 'Window': The callback provided as parameter 1 "
                            + "is not a function.");
                }
                return Js.num(addFrame((JsObject) cb));
            }
        }));
        g.defineBuiltin("cancelAnimationFrame", realm.newFunction("cancelAnimationFrame", 1, new NativeFunction.Call() {
            /**
             * Cancels an animation frame callback.
             * <p>
             * 애니메이션 프레임 콜백을 취소한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                frames.remove(Integer.valueOf((int) Ops.toInteger(Js.arg(args, 0))));
                return Js.UNDEFINED;
            }
        }));
        g.defineBuiltin("requestIdleCallback", realm.newFunction("requestIdleCallback", 1, new NativeFunction.Call() {
            /**
             * Registers a callback for the idle time (HTML Standard "Idle Callbacks"): the page has no idle detection, so the callback runs as
             * a task soon after the current one (after 1 ms, or after the <code>timeout</code> of the options if that is shorter). Its
             * deadline object reports <code>didTimeout</code> false and a <code>timeRemaining()</code> of up to 50 ms.
             * <p>
             * 유휴 시간용 콜백을 등록한다(HTML Standard "Idle Callbacks"): 페이지에는 유휴 감지가 없으므로 콜백은 현재 작업 직후(1ms 뒤, 옵션의
             * <code>timeout</code> 이 더 짧으면 그 뒤)에 작업으로 실행된다. 마감 객체는 <code>didTimeout</code> false 와 최대 50ms 의
             * <code>timeRemaining()</code> 을 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the callback and the options / 인수: 콜백과 옵션
             * @return the id / id
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'requestIdleCallback' on 'Window': The callback provided as parameter 1 "
                            + "is not a function.");
                }
                return Js.num(addIdle((JsObject) cb));
            }
        }));
        g.defineBuiltin("cancelIdleCallback", realm.newFunction("cancelIdleCallback", 1, new NativeFunction.Call() {
            /**
             * Cancels an idle callback.
             * <p>
             * 유휴 콜백을 취소한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Scheduler.Handle h = idle.remove(Integer.valueOf((int) Ops.toInteger(Js.arg(args, 0))));
                if (h != null) {
                    h.cancel();
                }
                return Js.UNDEFINED;
            }
        }));
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
                    throw JsException.typeError("Failed to execute 'queueMicrotask' on 'Window': The callback provided as parameter 1 is not "
                            + "a function.");
                }
                page.getRealm().enqueueJob(new Runnable() {
                    /**
                     * Calls the function; its error is reported.
                     * <p>
                     * 함수를 부른다. 오류는 보고한다.
                     */
                    public void run() {
                        try {
                            Ops.call(fn, Js.UNDEFINED, new JsValue[0]);
                        } catch (JsException e) {
                            page.reportError(e);
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
        if (timers.size() >= MAX_TIMERS) {
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
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null || disposed) {
            return;
        }
        long delay = t.delay;
        if (t.nesting >= 5 && delay < 4) {
            delay = 4;
        }
        if (t.repeat && delay < 1) {
            delay = 1;
        }
        t.handle = s.post(new Runnable() {
            /**
             * Runs the timer as a page task.
             * <p>
             * 타이머를 페이지 작업으로 실행한다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Runs the callback if the timer is still pending.
                     * <p>
                     * 타이머가 아직 대기 중이면 콜백을 실행한다.
                     */
                    public void run() {
                        fire(t);
                    }
                });
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
        if (disposed || timers.get(Integer.valueOf(t.id)) != t || !page.scripting()) {
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
                Ops.call(t.callback, page.getRealm().getGlobalObject(), t.args);
            } else {
                page.getRealm().evaluateScript(Ops.toString(t.callback), "timer");
            }
        } catch (JsException e) {
            page.reportError(e);
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

    /**
     * Registers an animation frame callback and schedules the frame if none is pending.
     * <p>
     * 애니메이션 프레임 콜백을 등록하고 대기 중인 프레임이 없으면 프레임을 예약한다.
     *
     * @param cb the callback / 콜백
     * @return the id / id
     */
    private int addFrame(JsObject cb) {
        int id = nextFrameId++;
        frames.put(Integer.valueOf(id), cb);
        scheduleFrame();
        return id;
    }

    /**
     * Installs the receiver of the Web Animations part of the animation frames.
     * <p>
     * 애니메이션 프레임의 Web Animations 부분을 받는 쪽을 설치한다.
     *
     * @param hook the receiver or null / 받는 쪽 또는 null
     */
    void setAnimationHook(org.duckdns.hjow.swingwebpanel.js.host.anim.AnimationPage.FrameHook hook) {
        animationHook = hook;
    }

    /**
     * Schedules the next animation frame if none is pending (the frame of <code>requestAnimationFrame</code>, also used by Web Animations).
     * <p>
     * 대기 중인 애니메이션 프레임이 없으면 다음 프레임을 예약한다(<code>requestAnimationFrame</code> 의 프레임이며 Web Animations 도 쓴다).
     */
    void scheduleFrame() {
        Scheduler s = page.getEnvironment().getScheduler();
        if (s != null && frameHandle == null && !disposed) {
            frameHandle = s.post(new Runnable() {
                /**
                 * Runs the frame as a page task.
                 * <p>
                 * 프레임을 페이지 작업으로 실행한다.
                 */
                public void run() {
                    page.runTask(new Runnable() {
                        /**
                         * Runs the frame callbacks.
                         * <p>
                         * 프레임 콜백을 실행한다.
                         */
                        public void run() {
                            frame();
                        }
                    });
                }
            }, 16);
        }
    }

    /**
     * Schedules an idle callback as a page task.
     * <p>
     * 유휴 콜백을 페이지 작업으로 예약한다.
     *
     * @param cb the callback / 콜백
     * @return the id / id
     */
    private int addIdle(final JsObject cb) {
        final int id = nextIdleId++;
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null || disposed) {
            return id;
        }
        final double started = s.now();
        idle.put(Integer.valueOf(id), s.post(new Runnable() {
            /**
             * Runs the callback as a page task.
             * <p>
             * 콜백을 페이지 작업으로 실행한다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Calls the callback with the deadline object.
                     * <p>
                     * 마감 객체와 함께 콜백을 부른다.
                     */
                    public void run() {
                        // 취소되었거나 페이지가 폐기되었으면 아무것도 하지 않는다.
                        if (disposed || idle.remove(Integer.valueOf(id)) == null || !page.scripting()) {
                            return;
                        }
                        final JsRealm realm = page.getRealm();
                        JsObject deadline = realm.newObject();
                        deadline.createDataProperty("didTimeout", Js.bool(false));
                        deadline.createDataProperty("timeRemaining", realm.newFunction("timeRemaining", 0, new NativeFunction.Call() {
                            /**
                             * Returns the time that is left of the idle period (at most 50 ms).
                             * <p>
                             * 유휴 구간의 남은 시간을 반환한다(최대 50ms).
                             *
                             * @param r    the realm / realm
                             * @param self this value / this 값
                             * @param args arguments / 인수
                             * @return the milliseconds / 밀리초
                             */
                            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                                Scheduler now = page.getEnvironment().getScheduler();
                                double used = now == null ? 0 : now.now() - started;
                                return Js.num(Math.max(0, 50 - used));
                            }
                        }));
                        try {
                            Ops.call(cb, Js.UNDEFINED, new JsValue[] { deadline });
                        } catch (JsException e) {
                            page.reportError(e);
                        }
                    }
                });
            }
        }, 1));
        return id;
    }

    /**
     * Calls the callbacks that were registered before this frame started; callbacks that register new ones get the next frame.
     * <p>
     * 이 프레임이 시작되기 전에 등록된 콜백을 부른다. 새로 등록하는 콜백은 다음 프레임에 받는다.
     */
    private void frame() {
        frameHandle = null;
        if (disposed || !page.scripting()) {
            return;
        }
        double now = page.getEnvironment().getScheduler() == null ? 0 : page.getEnvironment().getScheduler().now() - page.startTime();
        // 같은 시각으로 Web Animations 의 "update animations and send events" 를 애니메이션 프레임 콜백보다 먼저 실행한다(HTML "update the rendering").
        org.duckdns.hjow.swingwebpanel.js.host.anim.AnimationPage.FrameHook hook = animationHook;
        if (hook != null) {
            try {
                hook.frame(now);
            } catch (JsException e) {
                page.reportError(e);
            }
        }
        List<JsObject> list = new ArrayList<JsObject>(frames.values());
        frames.clear();
        // 이 프레임에 등록되어 있던 콜백을 등록 순서대로 부른다.
        for (int i = 0; i < list.size(); i++) {
            try {
                Ops.call(list.get(i), Js.UNDEFINED, new JsValue[] { Js.num(now) });
            } catch (JsException e) {
                page.reportError(e);
            }
        }
    }

    /**
     * Cancels everything (the page is disposed).
     * <p>
     * 모두 취소한다(페이지가 폐기됨).
     */
    void dispose() {
        disposed = true;
        // 대기 중인 타이머의 예약 작업을 모두 취소한다.
        for (Timer t : new ArrayList<Timer>(timers.values())) {
            if (t.handle != null) {
                t.handle.cancel();
            }
        }
        timers.clear();
        frames.clear();
        if (frameHandle != null) {
            frameHandle.cancel();
        }
        // 대기 중인 유휴 콜백도 모두 취소한다.
        for (Scheduler.Handle h : new ArrayList<Scheduler.Handle>(idle.values())) {
            h.cancel();
        }
        idle.clear();
    }
}
