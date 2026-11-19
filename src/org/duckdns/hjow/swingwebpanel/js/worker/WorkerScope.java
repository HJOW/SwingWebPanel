package org.duckdns.hjow.swingwebpanel.js.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.ErrorEvent;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.MessagingApi;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.web.WebApis;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * One dedicated worker (HTML Standard "Web workers"): a script context of its own that runs on a daemon thread of its own
 * (<code>swingwebpanel-worker-N</code>). It owns a {@link JsRealm} with the built-in objects, a DOM bridge that has only the event interfaces
 * (no window, document, node or storage), the global scope object, the timers, the request APIs and the event loop. The realm is touched by
 * its own thread only: every task (a message, a timer, a request result) is queued on the single thread executor of the worker and runs there,
 * followed by the microtask checkpoint, so the order of messages is kept and microtasks run before the next message.
 * <p>
 * Life cycle: the page creates the object and calls {@link #start()}; the thread fetches the script through the script source of the
 * component (same user agent, headers, cookies, scheme limits and cancellation as the scripts of the page), builds the realm and runs the
 * script, and from then on delivers the queued messages. {@link #terminate()} (the page, <code>Worker.terminate()</code>, page navigation,
 * <code>clean()</code>, <code>dispose()</code> or switching JavaScript off) stops everything at once: the running script is cancelled at its
 * next step, the pending requests are cancelled, the queued tasks are dropped and the thread ends; nothing runs afterwards. A worker that calls
 * <code>self.close()</code> ends after its current task. The script limits of the realm (steps, time, call depth, string length) are those of
 * the page; a task that exceeds them is aborted and reported to the page as an <code>error</code> event.
 * <p>
 * 전용 worker 하나(HTML 표준 "Web workers"): 자기 데몬 스레드(<code>swingwebpanel-worker-N</code>)에서 실행되는 자기만의 스크립트 문맥. 내장 객체가 있는
 * {@link JsRealm}, 이벤트 인터페이스만 있는 DOM 브리지(window, document, 노드, 저장소 없음), 전역 범위 객체, 타이머, 요청 API, 이벤트 루프를 소유한다.
 * realm 은 자기 스레드만 만진다. 모든 작업(메시지, 타이머, 요청 결과)은 worker 의 단일 스레드 실행기에 줄 서서 그곳에서 실행되고 마이크로태스크
 * 검사점이 뒤따르므로 메시지 순서가 유지되고 마이크로태스크가 다음 메시지보다 먼저 실행된다.
 * <p>
 * 수명: 페이지가 객체를 만들고 {@link #start()} 를 부르면, 스레드가 컴포넌트의 스크립트 공급원으로 스크립트를 가져오고(페이지 스크립트와 같은
 * User-Agent, 헤더, 쿠키, 스킴 제한, 취소) realm 을 만들어 스크립트를 실행한 뒤 줄 서 있던 메시지를 전달한다. {@link #terminate()}(페이지,
 * <code>Worker.terminate()</code>, 페이지 이동, <code>clean()</code>, <code>dispose()</code>, JavaScript 끄기)는 모든 것을 즉시 멈춘다: 실행 중인
 * 스크립트는 다음 단계에서 취소되고, 대기 중인 요청은 취소되며, 줄 선 작업은 버려지고, 스레드가 끝난다. 그 뒤에는 아무것도 실행되지 않는다.
 * <code>self.close()</code> 를 부른 worker 는 현재 작업이 끝난 뒤 끝난다. realm 의 스크립트 한도(단계 수, 시간, 호출 깊이, 문자열 길이)는 페이지와
 * 같으며, 한도를 넘은 작업은 중단되고 페이지에 <code>error</code> 이벤트로 보고된다.
 */
final class WorkerScope implements ScriptContext {

    /**
     * The counter of the worker thread names.
     * <p>
     * worker 스레드 이름의 번호 계수기.
     */
    private static final AtomicInteger IDS = new AtomicInteger();

    /**
     * A handle of a task that was never scheduled.
     * <p>
     * 예약되지 않은 작업의 핸들.
     */
    private static final Scheduler.Handle NO_HANDLE = new Scheduler.Handle() {
        /**
         * Nothing to cancel.
         * <p>
         * 취소할 것이 없다.
         */
        public void cancel() {
            return;
        }
    };

    /**
     * The environment of the page: user agent, script source, request service and limits (read only, thread safe).
     * <p>
     * 페이지의 환경: User-Agent, 스크립트 공급원, 요청 서비스, 한도(읽기 전용, 스레드 안전).
     */
    private final HostEnvironment env;

    /**
     * The page side of the worker.
     * <p>
     * worker 의 페이지 쪽.
     */
    private final WorkerParent parent;

    /**
     * The hub of the broadcast channels of the page and its workers.
     * <p>
     * 페이지와 그 worker 의 브로드캐스트 채널 허브.
     */
    private final BroadcastHub hub;

    /**
     * The absolute URL of the worker script (the <code>location</code> of the worker).
     * <p>
     * worker 스크립트의 절대 URL(worker 의 <code>location</code>).
     */
    private final String scriptUrl;

    /**
     * The name of the worker (<code>self.name</code>).
     * <p>
     * worker 의 이름(<code>self.name</code>).
     */
    private final String name;

    /**
     * The event loop: one daemon thread that runs the tasks in order, and the timers.
     * <p>
     * 이벤트 루프: 작업을 순서대로, 그리고 타이머를 실행하는 데몬 스레드 하나.
     */
    private final ScheduledThreadPoolExecutor executor;

    /**
     * Guards {@link #started} and {@link #early}.
     * <p>
     * {@link #started} 와 {@link #early} 를 보호한다.
     */
    private final Object lock = new Object();

    /**
     * The messages that arrived before the script ran; guarded by {@link #lock}.
     * <p>
     * 스크립트가 실행되기 전에 도착한 메시지. {@link #lock} 으로 보호한다.
     */
    private final LinkedList<ClonedValue> early = new LinkedList<ClonedValue>();

    /**
     * The connections (inside ports) that arrived before the script ran, for a shared worker; guarded by {@link #lock}.
     * <p>
     * 스크립트가 실행되기 전에 도착한 연결(안쪽 포트). 공유 worker 용이다. {@link #lock} 으로 보호한다.
     */
    private final LinkedList<PortCore> earlyConnects = new LinkedList<PortCore>();

    /**
     * True for a shared worker (<code>SharedWorkerGlobalScope</code>), false for a dedicated worker.
     * <p>
     * 공유 worker(<code>SharedWorkerGlobalScope</code>)이면 true, 전용 worker 이면 false.
     */
    private final boolean shared;

    /**
     * The number of messages from the page that wait for delivery (a flood limit).
     * <p>
     * 전달을 기다리는 페이지의 메시지 수(폭주 한도).
     */
    private final AtomicInteger pendingIn = new AtomicInteger();

    /**
     * True once the worker was terminated; nothing runs afterwards.
     * <p>
     * worker 가 종료되었으면 true. 그 뒤에는 아무것도 실행되지 않는다.
     */
    private volatile boolean terminated;

    /**
     * True after <code>self.close()</code>: the worker ends when its current task is done.
     * <p>
     * <code>self.close()</code> 뒤에는 true. worker 는 현재 작업이 끝나면 끝난다.
     */
    private volatile boolean closing;

    /**
     * True once the script has run and messages are delivered; guarded by {@link #lock}.
     * <p>
     * 스크립트가 실행되어 메시지를 전달하기 시작했으면 true. {@link #lock} 으로 보호한다.
     */
    private boolean started;

    /**
     * The realm; created by the worker thread (visible to {@link #terminate()} for the cancellation).
     * <p>
     * realm. worker 스레드가 만든다({@link #terminate()} 가 취소하려고 볼 수 있다).
     */
    private volatile JsRealm realm;

    /**
     * The DOM bridge of the realm (event interfaces only).
     * <p>
     * realm 의 DOM 브리지(이벤트 인터페이스만 있다).
     */
    private volatile DomBridge bridge;

    /**
     * The event target behind <code>self</code>.
     * <p>
     * <code>self</code> 뒤의 이벤트 대상.
     */
    private WorkerGlobal global;

    /**
     * The timers of the worker.
     * <p>
     * worker 의 타이머.
     */
    private WorkerTimers timers;

    /**
     * The request APIs of the worker (<code>fetch</code>, <code>XMLHttpRequest</code> ...); volatile because {@link #terminate()} cancels them.
     * <p>
     * worker 의 요청 API(<code>fetch</code>, <code>XMLHttpRequest</code> 등). {@link #terminate()} 가 취소하므로 volatile 이다.
     */
    private volatile WebApis web;

    /**
     * The nesting depth of tasks that run on the worker thread (the microtask checkpoint belongs to the outermost one).
     * <p>
     * worker 스레드에서 실행 중인 작업의 중첩 깊이(마이크로태스크 검사점은 가장 바깥 작업의 몫이다).
     */
    private int depth;

    /**
     * The nesting depth of <code>importScripts()</code>.
     * <p>
     * <code>importScripts()</code> 의 중첩 깊이.
     */
    private int importDepth;

    /**
     * True while an error is being reported (an error of a listener of the error event must not report itself again).
     * <p>
     * 오류를 보고하는 중이면 true(오류 이벤트 리스너의 오류가 다시 자기 자신을 보고하면 안 된다).
     */
    private boolean reporting;

    /**
     * The <code>self.onerror</code> handler, or null.
     * <p>
     * <code>self.onerror</code> 핸들러. 없으면 null.
     */
    private JsObject onerror;

    /**
     * The scheduler that the request APIs deliver their results with: raw tasks on the worker thread.
     * <p>
     * 요청 API가 결과를 전달하는 스케줄러: worker 스레드의 원시 작업.
     */
    private final Scheduler loop = new Scheduler() {
        /**
         * Returns the system time.
         * <p>
         * 시스템 시각을 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public long now() {
            return System.currentTimeMillis();
        }

        /**
         * Schedules a raw task on the worker thread.
         * <p>
         * worker 스레드에 원시 작업을 예약한다.
         *
         * @param task    the task / 작업
         * @param delayMs the delay / 지연
         * @return the handle / 핸들
         */
        public Scheduler.Handle post(Runnable task, long delayMs) {
            return rawSchedule(task, delayMs);
        }
    };

    /**
     * The handler of the failures of the listeners of the event targets of the worker: an abort of a running listener (a limit of the script
     * task, a cancellation) and a defect are reported like the errors of the worker, so that the page learns about an endless loop in a
     * listener too.
     * <p>
     * worker 의 이벤트 대상의 리스너 실패를 받는 핸들러: 실행 중이던 리스너의 중단(스크립트 작업의 제한, 취소)과 결함을 worker 의 오류처럼 보고하므로, 페이지는
     * 리스너 안의 무한 반복도 알게 된다.
     */
    private final ListenerErrorHandler listenerErrors = new ListenerErrorHandler() {
        /**
         * Reports the failure as an error of the worker.
         * <p>
         * 실패를 worker 의 오류로 보고한다.
         *
         * @param target  the target / 대상
         * @param event   the event / 이벤트
         * @param failure the failure / 실패
         */
        public void listenerFailed(EventTarget target, Event event, Throwable failure) {
            if (failure instanceof RuntimeException) {
                reportError((RuntimeException) failure);
            } else {
                EventTarget.PRINTING_HANDLER.listenerFailed(target, event, failure);
            }
        }
    };

    /**
     * Creates a worker that has not started.
     * <p>
     * 시작하지 않은 worker 를 만든다.
     *
     * @param env       the environment of the page / 페이지의 환경
     * @param parent    the page side / 페이지 쪽
     * @param hub       the broadcast hub of the page / 페이지의 브로드캐스트 허브
     * @param scriptUrl the absolute URL of the worker script / worker 스크립트의 절대 URL
     * @param name      the name of the worker / worker 의 이름
     * @param shared    true for a shared worker / 공유 worker 이면 true
     */
    WorkerScope(HostEnvironment env, WorkerParent parent, BroadcastHub hub, String scriptUrl, String name, boolean shared) {
        this.env = env;
        this.parent = parent;
        this.hub = hub;
        this.scriptUrl = scriptUrl;
        this.name = name;
        this.shared = shared;
        this.executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            /**
             * Creates the daemon thread of the worker.
             * <p>
             * worker 의 데몬 스레드를 만든다.
             *
             * @param r the work / 작업
             * @return the thread / 스레드
             */
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "swingwebpanel-worker-" + IDS.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * Starts the worker: the thread fetches the script, builds the realm, runs the script and then delivers the messages.
     * <p>
     * worker 를 시작한다: 스레드가 스크립트를 가져오고 realm 을 만들어 스크립트를 실행한 뒤 메시지를 전달한다.
     */
    void start() {
        rawSchedule(new Runnable() {
            /**
             * Boots the worker on its thread.
             * <p>
             * worker 의 스레드에서 worker 를 부팅한다.
             */
            public void run() {
                boot();
            }
        }, 0);
    }

    /**
     * Fetches the script, builds the realm and runs the script; runs on the worker thread.
     * <p>
     * 스크립트를 가져오고 realm 을 만들어 스크립트를 실행한다. worker 스레드에서 실행된다.
     */
    private void boot() {
        String source;
        try {
            source = fetchScript(scriptUrl);
        } catch (IOException e) {
            failLoad("Failed to load the worker script " + scriptUrl + ": " + e.getMessage());
            return;
        }
        if (terminated) {
            return;
        }
        try {
            buildRealm();
        } catch (RuntimeException e) {
            failLoad("Failed to create the worker: " + e);
            return;
        }
        final String text = source;
        runTask(new Runnable() {
            /**
             * Runs the worker script as a classic script.
             * <p>
             * worker 스크립트를 일반 스크립트로 실행한다.
             */
            public void run() {
                realm.evaluateScript(text, scriptUrl);
            }
        });
        // 스크립트가 실행되면 기다리던 메시지를 도착한 순서대로 전달하기 시작한다.
        synchronized (lock) {
            if (terminated) {
                return;
            }
            started = true;
            // 줄 서 있던 메시지를 도착한 순서대로 하나씩 전달 작업으로 올린다.
            while (!early.isEmpty()) {
                postDelivery(early.removeFirst());
            }
            // 공유 worker 는 스크립트가 실행되기 전에 도착한 연결마다 connect 이벤트를 올린다.
            while (!earlyConnects.isEmpty()) {
                postConnect(earlyConnects.removeFirst());
            }
        }
    }

    /**
     * Fetches the text of a script through the script source of the component.
     * <p>
     * 컴포넌트의 스크립트 공급원으로 스크립트 텍스트를 가져온다.
     *
     * @param url the absolute URL / 절대 URL
     * @return the text / 텍스트
     * @throws IOException if it cannot be fetched / 가져올 수 없으면
     */
    private String fetchScript(String url) throws IOException {
        ScriptSource source = env.getScriptSource();
        if (source == null) {
            throw new IOException("scripts cannot be fetched");
        }
        return source.fetch(url);
    }

    /**
     * Ends a worker that cannot start and tells the page.
     * <p>
     * 시작할 수 없는 worker 를 끝내고 페이지에 알린다.
     *
     * @param reason the reason / 이유
     */
    private void failLoad(String reason) {
        boolean wasTerminated = terminated;
        terminate();
        if (!wasTerminated) {
            parent.loadFailed(reason);
        }
    }

    /**
     * Builds the realm of the worker on the worker thread: the built-in objects, the event interfaces, the global scope, the request APIs and
     * the broadcast channel.
     * <p>
     * worker 스레드에서 worker 의 realm 을 만든다: 내장 객체, 이벤트 인터페이스, 전역 범위, 요청 API, 브로드캐스트 채널.
     */
    private void buildRealm() {
        JsRealm r = JsRealm.create();
        realm = r;
        r.getLimits().setMaxTimeMillis(env.getTaskTimeLimitMillis());
        r.setConsole(new ConsoleHook() {
            /**
             * Sends the output of the worker console to the console of the page.
             * <p>
             * worker 콘솔의 출력을 페이지의 콘솔로 보낸다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                if (!terminated) {
                    parent.console(level, text);
                }
            }
        });
        DomBridge b = new DomBridge(r);
        bridge = b;
        b.setErrorSink(new DomBridge.ErrorSink() {
            /**
             * Reports an error that a listener or callback threw as an error of the worker.
             * <p>
             * 리스너나 콜백이 던진 오류를 worker 의 오류로 보고한다.
             *
             * @param error the error / 오류
             */
            public void scriptError(RuntimeException error) {
                reportError(error);
            }
        });
        DomInterfaces.installEventCore(b);
        MessagingApi.installMessageEvent(b, r);
        PortApi.install(this);
        global = new WorkerGlobal(this);
        timers = new WorkerTimers(this);
        WorkerGlobals.install(this);
        web = WebApis.install(new WorkerWebHost(this, env, loop));
        BroadcastApi.install(this, hub);
    }

    // ------------------------------------------------------------------ messages

    /**
     * Delivers a message of the page to the worker: it waits until the script ran, then it is dispatched as a <code>message</code> event in a
     * task of the worker. Callable from any thread.
     * <p>
     * 페이지의 메시지를 worker 에 전달한다: 스크립트가 실행될 때까지 기다렸다가 worker 의 작업으로 <code>message</code> 이벤트로 전달된다. 어느
     * 스레드에서나 부를 수 있다.
     *
     * @param message the serialized message / 직렬화된 메시지
     */
    void deliver(ClonedValue message) {
        if (pendingIn.incrementAndGet() > WorkerLimits.MAX_PENDING_MESSAGES) {
            pendingIn.decrementAndGet();
            return;
        }
        synchronized (lock) {
            if (terminated || closing) {
                pendingIn.decrementAndGet();
                return;
            }
            if (!started) {
                early.add(message);
                return;
            }
            postDelivery(message);
        }
    }

    /**
     * Tells whether this is a shared worker.
     * <p>
     * 공유 worker 인지 알려 준다.
     *
     * @return true for a shared worker / 공유 worker 이면 true
     */
    boolean isShared() {
        return shared;
    }

    /**
     * Connects a client to a shared worker: a <code>connect</code> event with the inside port in <code>ports</code> follows as a task of the
     * worker once its script ran. Callable from any thread.
     * <p>
     * 공유 worker 에 클라이언트를 연결한다: worker 의 스크립트가 실행된 뒤 <code>ports</code> 에 안쪽 포트를 싣은 <code>connect</code> 이벤트가 worker 의
     * 작업으로 뒤따른다. 어느 스레드에서나 부를 수 있다.
     *
     * @param inside the end of the channel that the worker owns / worker 가 소유하는 채널의 끝
     */
    void connect(PortCore inside) {
        synchronized (lock) {
            if (terminated || closing) {
                return;
            }
            if (!started) {
                earlyConnects.add(inside);
                return;
            }
            postConnect(inside);
        }
    }

    /**
     * Queues the <code>connect</code> event of one connection on the worker thread.
     * <p>
     * worker 스레드에 연결 하나의 <code>connect</code> 이벤트를 줄 세운다.
     *
     * @param inside the end of the channel that the worker owns / worker 가 소유하는 채널의 끝
     */
    private void postConnect(final PortCore inside) {
        schedule(new Runnable() {
            /**
             * Gives the end of the channel to the worker as a port and dispatches the event.
             * <p>
             * 채널의 끝을 포트로 worker 에 주고 이벤트를 전달한다.
             */
            public void run() {
                PortTarget port = new PortTarget(WorkerScope.this);
                port.bind(inside);
                JsObject wrapper = bridge.wrap(port);
                List<JsObject> ports = new ArrayList<JsObject>();
                ports.add(wrapper);
                MessageEvent event = new MessageEvent("connect", false, false);
                event.setContent(Js.str(""), "", "", wrapper, ports);
                try {
                    global.dispatchTrustedEvent(event);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        }, 0);
    }

    /**
     * Queues the dispatch of one message on the worker thread.
     * <p>
     * worker 스레드에 메시지 하나의 전달을 줄 세운다.
     *
     * @param message the serialized message / 직렬화된 메시지
     */
    private void postDelivery(final ClonedValue message) {
        schedule(new Runnable() {
            /**
             * Dispatches the message at the global scope.
             * <p>
             * 메시지를 전역 범위에 전달한다.
             */
            public void run() {
                pendingIn.decrementAndGet();
                MessageEvent event = Messages.event(WorkerScope.this, message, "");
                try {
                    global.dispatchTrustedEvent(event);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        }, 0);
    }

    /**
     * Sends a message of the worker to the page (<code>self.postMessage</code>); it is dropped after the worker ended.
     * <p>
     * worker 의 메시지를 페이지로 보낸다(<code>self.postMessage</code>). worker 가 끝난 뒤에는 버린다.
     *
     * @param message the serialized message / 직렬화된 메시지
     */
    void postToParent(ClonedValue message) {
        if (!terminated) {
            parent.messageFromWorker(message);
        }
    }

    // ------------------------------------------------------------------ event loop

    /**
     * Schedules a raw task on the worker thread; it is skipped after the worker was terminated or closed. An unexpected exception of the task is
     * written to the console instead of being lost.
     * <p>
     * worker 스레드에 원시 작업을 예약한다. worker 가 종료되거나 닫힌 뒤에는 건너뛴다. 작업이 던진 예상 밖의 예외는 사라지지 않고 콘솔에 기록한다.
     *
     * @param task    the task / 작업
     * @param delayMs the delay in milliseconds / 지연(밀리초)
     * @return the handle / 핸들
     */
    Scheduler.Handle rawSchedule(final Runnable task, long delayMs) {
        try {
            final ScheduledFuture<?> f = executor.schedule(new Runnable() {
                /**
                 * Runs the task unless the worker ended.
                 * <p>
                 * worker 가 끝나지 않았으면 작업을 실행한다.
                 */
                public void run() {
                    if (terminated || closing) {
                        return;
                    }
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        internalError(e);
                    }
                }
            }, delayMs, TimeUnit.MILLISECONDS);
            return new Scheduler.Handle() {
                /**
                 * Cancels the task if it has not run yet.
                 * <p>
                 * 아직 실행되지 않았으면 작업을 취소한다.
                 */
                public void cancel() {
                    f.cancel(false);
                }
            };
        } catch (RejectedExecutionException e) {
            return NO_HANDLE;
        }
    }

    /**
     * Schedules a task of the worker: it runs on the worker thread as a task (limits, error reporting and the microtask checkpoint).
     * <p>
     * worker 의 작업을 예약한다: worker 스레드에서 작업(한도, 오류 보고, 마이크로태스크 검사점 포함)으로 실행된다.
     *
     * @param body    the work / 작업
     * @param delayMs the delay in milliseconds / 지연(밀리초)
     * @return the handle / 핸들
     */
    Scheduler.Handle schedule(final Runnable body, long delayMs) {
        return rawSchedule(new Runnable() {
            /**
             * Runs the body as a task.
             * <p>
             * 본문을 작업으로 실행한다.
             */
            public void run() {
                runTask(body);
            }
        }, delayMs);
    }

    /**
     * Runs a task of the worker on the worker thread: the body, then the microtask checkpoint; the errors of the body are reported as errors of
     * the worker, and a task that is aborted by a limit ends and is reported. Nothing runs after the worker ended.
     * <p>
     * worker 스레드에서 worker 의 작업을 실행한다: 본문, 이어서 마이크로태스크 검사점. 본문의 오류는 worker 의 오류로 보고하고, 한도로 중단된 작업은
     * 끝나고 보고된다. worker 가 끝난 뒤에는 아무것도 실행되지 않는다.
     *
     * @param body the work / 작업
     */
    void runTask(Runnable body) {
        if (terminated) {
            return;
        }
        depth++;
        ExecutionLimits limits = realm.getLimits();
        limits.beginTask();
        try {
            body.run();
            // 다른 작업 안에 중첩된 작업은 바깥 작업이 끝날 때의 검사점에 맡긴다.
            if (depth == 1) {
                realm.runJobs();
            }
        } catch (JsException e) {
            reportError(e);
        } catch (ScriptAbortedException e) {
            reportAbort(e);
        } finally {
            limits.endTask();
            depth--;
        }
        // self.close() 를 부른 worker 는 현재 작업이 끝나면 끝난다.
        if (depth == 0 && closing) {
            terminate();
        }
    }

    /**
     * Reports an unexpected exception (a defect, not a script error) to the console of the page.
     * <p>
     * 예상 밖의 예외(스크립트 오류가 아닌 결함)를 페이지의 콘솔에 보고한다.
     *
     * @param e the exception / 예외
     */
    private void internalError(RuntimeException e) {
        if (!terminated) {
            parent.console("error", "Internal error of the worker " + scriptUrl + ": " + e);
        }
    }

    // ------------------------------------------------------------------ errors

    /**
     * Reports an uncaught error of the worker: first to <code>self.onerror</code> and the <code>error</code> listeners of the global scope
     * (the error event can be cancelled there), and, if nobody handled it, to the page, which fires an <code>error</code> event at the
     * <code>Worker</code> object. Never throws.
     * <p>
     * worker 의 잡히지 않은 오류를 보고한다: 먼저 <code>self.onerror</code> 와 전역 범위의 <code>error</code> 리스너(여기서 오류 이벤트를 취소할 수
     * 있다)에, 아무도 처리하지 않았으면 페이지에 보고하고 페이지는 <code>Worker</code> 객체에 <code>error</code> 이벤트를 발생시킨다. 예외를 던지지 않는다.
     *
     * @param error the error / 오류
     */
    void reportError(RuntimeException error) {
        if (terminated) {
            return;
        }
        if (error instanceof ScriptAbortedException) {
            reportAbort((ScriptAbortedException) error);
            return;
        }
        String message = String.valueOf(error);
        String source = "";
        int line = 0;
        int col = 0;
        JsValue thrown = null;
        if (error instanceof JsException) {
            JsException je = (JsException) error;
            message = je.hasValue() ? JsException.describe(je.getValue(realm)) : je.getErrorMessage();
            if (message == null) {
                message = String.valueOf(je.getMessage());
            }
            source = je.getSourceName() == null ? "" : je.getSourceName();
            line = je.getLine();
            col = je.getColumn();
            thrown = je.hasValue() ? je.getValue(realm) : null;
        }
        if (source.length() == 0) {
            source = scriptUrl;
        }
        boolean handled = false;
        if (!reporting) {
            reporting = true;
            try {
                handled = callOnError(message, source, line, col, thrown);
                if (!handled) {
                    ErrorEvent event = new ErrorEvent("error", false, true);
                    event.setContent(message, source, line, col, thrown);
                    global.dispatchTrustedEvent(event);
                    handled = event.isDefaultPrevented();
                }
            } catch (RuntimeException e) {
                // 오류를 보고하다 생긴 오류는 더 보고하지 않는다(무한 되풀이 방지).
                handled = false;
            } finally {
                reporting = false;
            }
        }
        if (!handled && !terminated) {
            parent.errorFromWorker(message, source, line, col);
        }
    }

    /**
     * Calls <code>self.onerror</code> with the five arguments of the error event handler.
     * <p>
     * <code>self.onerror</code> 를 오류 이벤트 핸들러의 다섯 인수로 호출한다.
     *
     * @param message the message / 메시지
     * @param source  the script URL / 스크립트 URL
     * @param line    the line / 줄
     * @param col     the column / 열
     * @param thrown  the thrown value or null / 던져진 값. 없으면 null
     * @return true if the handler returned true (the error is handled) / 핸들러가 true 를 반환했으면(오류가 처리되었으면) true
     */
    private boolean callOnError(String message, String source, int line, int col, JsValue thrown) {
        if (onerror == null || !onerror.isCallable()) {
            return false;
        }
        try {
            JsValue r = Ops.call(onerror, realm.getGlobalObject(), new JsValue[] { Js.str(message), Js.str(source), Js.num(line), Js.num(col),
                thrown == null ? Js.NULL : thrown });
            return r == Js.TRUE;
        } catch (JsException e) {
            return false;
        }
    }

    /**
     * Reports a task that a limit aborted (time, steps, ...) to the page; a cancellation by {@link #terminate()} is silent.
     * <p>
     * 한도(시간, 단계 수 등)로 중단된 작업을 페이지에 보고한다. {@link #terminate()} 에 의한 취소는 조용하다.
     *
     * @param e the abort / 중단
     */
    private void reportAbort(ScriptAbortedException e) {
        if (terminated) {
            return;
        }
        String message = "Script aborted: " + e.getMessage();
        realm.getLimits().reset();
        parent.console("error", message);
        parent.errorFromWorker(message, scriptUrl, 0, 0);
    }

    // ------------------------------------------------------------------ importScripts

    /**
     * Fetches and runs one script of <code>importScripts()</code> (synchronously, on the worker thread). The fetch goes through the script source
     * of the component, so user agent, headers, cookies, scheme limits and cancellation apply; the script runs in the global scope.
     * <p>
     * <code>importScripts()</code> 의 스크립트 하나를 가져와 실행한다(worker 스레드에서 동기적으로). 가져오기는 컴포넌트의 스크립트 공급원을 거치므로
     * User-Agent, 헤더, 쿠키, 스킴 제한, 취소가 적용되고, 스크립트는 전역 범위에서 실행된다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     */
    void importScript(String absoluteUrl) {
        if (importDepth >= WorkerLimits.MAX_IMPORT_DEPTH) {
            throw JsException.rangeError("importScripts() is nested too deeply (limit " + WorkerLimits.MAX_IMPORT_DEPTH + ")");
        }
        String text;
        try {
            text = fetchScript(absoluteUrl);
        } catch (IOException e) {
            throw bridge.domError(new DomException("NetworkError", DomException.NETWORK_ERR,
                    "Failed to execute 'importScripts' on 'WorkerGlobalScope': The script at '" + absoluteUrl + "' failed to load (" + e.getMessage() + ")."));
        }
        // 가져오는 동안 worker 가 종료되었으면 취소로 중단한다.
        realm.getLimits().poll();
        importDepth++;
        try {
            realm.evaluateScript(text, absoluteUrl);
        } finally {
            importDepth--;
        }
    }

    // ------------------------------------------------------------------ termination

    /**
     * Terminates the worker at once and from any thread without waiting: a running script is cancelled at its next step, the running requests
     * are cancelled, the queued tasks and messages are dropped, the channels leave the hub and the thread ends. Nothing runs afterwards.
     * Calling it again does nothing.
     * <p>
     * worker 를 기다리지 않고 어느 스레드에서나 즉시 종료한다: 실행 중인 스크립트는 다음 단계에서 취소되고, 실행 중인 요청은 취소되며, 줄 선 작업과
     * 메시지는 버려지고, 채널은 허브에서 빠지며, 스레드가 끝난다. 그 뒤에는 아무것도 실행되지 않는다. 다시 불러도 아무 일도 없다.
     */
    void terminate() {
        synchronized (lock) {
            if (terminated) {
                return;
            }
            terminated = true;
            early.clear();
            earlyConnects.clear();
        }
        JsRealm r = realm;
        if (r != null) {
            r.getLimits().cancel();
        }
        WebApis w = web;
        if (w != null) {
            w.cancelAll();
        }
        hub.removeContext(this);
        executor.shutdownNow();
    }

    /**
     * Closes the worker after the current task (<code>self.close()</code>): queued tasks are dropped and no message is accepted any more, but the
     * running script finishes.
     * <p>
     * 현재 작업이 끝난 뒤 worker 를 닫는다(<code>self.close()</code>). 줄 선 작업은 버려지고 더 이상 메시지를 받지 않지만 실행 중인 스크립트는 끝까지
     * 실행된다.
     */
    void close() {
        closing = true;
        synchronized (lock) {
            early.clear();
            earlyConnects.clear();
        }
        executor.getQueue().clear();
    }

    /**
     * Tells whether the worker was terminated.
     * <p>
     * worker 가 종료되었는지 알려 준다.
     *
     * @return true after {@link #terminate()} / {@link #terminate()} 뒤에는 true
     */
    boolean isTerminated() {
        return terminated;
    }

    // ------------------------------------------------------------------ accessors

    /**
     * Returns the realm; call it only on the worker thread.
     * <p>
     * realm 을 반환한다. worker 스레드에서만 부른다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return realm;
    }

    /**
     * Returns the DOM bridge; call it only on the worker thread.
     * <p>
     * DOM 브리지를 반환한다. worker 스레드에서만 부른다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge getBridge() {
        return bridge;
    }

    /**
     * Posts a task of this worker (a delivery of a broadcast message): it runs later on the worker thread.
     * <p>
     * 이 worker 의 작업(브로드캐스트 메시지 전달)을 올린다. 나중에 worker 스레드에서 실행된다.
     *
     * @param task the work / 작업
     */
    public void post(Runnable task) {
        schedule(task, 0);
    }

    /**
     * Tells whether the worker still runs scripts.
     * <p>
     * worker 가 아직 스크립트를 실행하는지 알려 준다.
     *
     * @return true while it runs / 실행 중이면 true
     */
    public boolean isActive() {
        return !terminated && !closing;
    }

    /**
     * Returns the handler of the failures of the listeners of the event targets of the worker.
     * <p>
     * worker 의 이벤트 대상의 리스너 실패를 받는 핸들러를 반환한다.
     *
     * @return the handler / 핸들러
     */
    public ListenerErrorHandler getListenerErrors() {
        return listenerErrors;
    }

    /**
     * Returns the origin of the worker (the origin of its script).
     * <p>
     * worker 의 오리진(스크립트의 오리진)을 반환한다.
     *
     * @return the origin text, or null for an opaque origin / 오리진 문자열. 불투명 오리진이면 null
     */
    public String getOrigin() {
        Origin o = Origin.of(scriptUrl);
        return o.isOpaque() ? null : o.serialize();
    }

    /**
     * Returns the absolute URL of the worker script.
     * <p>
     * worker 스크립트의 절대 URL을 반환한다.
     *
     * @return the URL / URL
     */
    String getScriptUrl() {
        return scriptUrl;
    }

    /**
     * Returns the name of the worker.
     * <p>
     * worker 의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    String getName() {
        return name;
    }

    /**
     * Returns the environment of the page.
     * <p>
     * 페이지의 환경을 반환한다.
     *
     * @return the environment / 환경
     */
    HostEnvironment getEnvironment() {
        return env;
    }

    /**
     * Returns the event target behind <code>self</code>.
     * <p>
     * <code>self</code> 뒤의 이벤트 대상을 반환한다.
     *
     * @return the target / 대상
     */
    WorkerGlobal getGlobal() {
        return global;
    }

    /**
     * Returns the timers of the worker.
     * <p>
     * worker 의 타이머를 반환한다.
     *
     * @return the timers / 타이머
     */
    WorkerTimers getTimers() {
        return timers;
    }

    /**
     * Returns the <code>self.onerror</code> handler.
     * <p>
     * <code>self.onerror</code> 핸들러를 반환한다.
     *
     * @return the handler or null / 핸들러. 없으면 null
     */
    JsObject getOnerror() {
        return onerror;
    }

    /**
     * Sets the <code>self.onerror</code> handler.
     * <p>
     * <code>self.onerror</code> 핸들러를 지정한다.
     *
     * @param handler the handler or null / 핸들러. 없으면 null
     */
    void setOnerror(JsObject handler) {
        this.onerror = handler;
    }
}
