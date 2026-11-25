package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.LinkedList;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecordHook;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.ModuleGraph;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.host.MessagingApi;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleScriptSource.Credentials;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleSources;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.js.host.structured.SharedMemoryScope;
import org.duckdns.hjow.swingwebpanel.js.worker.ChannelPorts;
import org.duckdns.hjow.swingwebpanel.js.worker.PortTarget;
import org.duckdns.hjow.swingwebpanel.js.worker.ScriptContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WorkletProcessorException;
import org.duckdns.hjow.swingwebpanel.net.Origin;

/**
 * The <code>AudioWorkletGlobalScope</code> of one audio context and everything that runs in it (Web Audio API 1.0, 1.32; HTML Worklets): a script realm of its own with the
 * built-in objects of ECMAScript 2015, the channel messaging and <code>registerProcessor()</code> (see {@link WorkletGlobals}), but no DOM and no window; it loads ES2015
 * modules for <code>audioWorklet.addModule()</code>, creates the <code>AudioWorkletProcessor</code> instance of every <code>AudioWorkletNode</code> and calls its
 * <code>process()</code> method in every render quantum.
 * <p>
 * <b>Threads, and who may touch the realm.</b> A script realm belongs to one thread at a time, so every use of the realm of this scope holds {@link #lock} (a fair lock). Three
 * kinds of code take it:
 * <ul>
 * <li>the <em>worklet thread</em> (<code>swingwebpanel-audio-worklet-N</code>, a daemon thread that starts with the first <code>addModule()</code> and ends with the scope): it
 * evaluates modules and runs the <em>realm tasks</em> (the delivery of a message to a port of a processor). It fetches modules <em>without</em> the lock
 * ({@link ModuleGraphFetcher}), so a slow network never delays the rendering thread;</li>
 * <li>the <em>rendering thread</em> of the audio context: it constructs the processors (a control message at the start of a quantum, so that the result is the same on every run of an
 * offline context) and calls <code>process()</code> once per node per quantum. This is where the specification wants the script to run. In a real-time context it only
 * <em>tries</em> the lock for {@link WorkletLimits#REALTIME_LOCK_WAIT_MILLIS} ms: if the worklet thread is busy the node is silent for that quantum
 * ({@link AudioWorkletNode#SKIPPED}) and the other nodes of that quantum do not wait at all. A real-time context therefore never waits for a script that is not its
 * processor for long; an offline context has no deadline, waits for the lock and, before it processes a quantum, runs the pending realm tasks itself, so that a message posted before
 * <code>startRendering()</code> (or before a quantum) is delivered before that quantum, deterministically;</li>
 * <li>nobody else: the page never touches the realm. What the page needs reaches it as plain data (names, descriptors, serialized messages) through task queues.</li>
 * </ul>
 * A call of <code>process()</code> has a limit of {@link WorkletLimits#PROCESS_STEP_LIMIT} steps and, in a real-time context,
 * {@link WorkletLimits#REALTIME_PROCESS_TIME_LIMIT_MILLIS} ms; a processor that exceeds it fails like a processor that throws (<code>processorerror</code>, silence for good).
 * {@link #dispose()} (the context was closed, the page was replaced, scripts were switched off) cancels the running script, drops the queues and ends the worklet thread without
 * waiting; the nodes of the scope are silent from then on.
 * <p>
 * 오디오 컨텍스트 하나의 <code>AudioWorkletGlobalScope</code> 와 그 안에서 실행되는 모든 것(Web Audio API 1.0, 1.32. HTML Worklets): ECMAScript 2015 의 내장 객체, 채널 메시징,
 * <code>registerProcessor()</code>({@link WorkletGlobals} 참고)가 있지만 DOM 도 window 도 없는 자기만의 스크립트 realm 이다. <code>audioWorklet.addModule()</code> 를 위해 ES2015 모듈을 불러오고,
 * <code>AudioWorkletNode</code> 마다 <code>AudioWorkletProcessor</code> 인스턴스를 만들고, render quantum 마다 그 <code>process()</code> 메서드를 호출한다.
 * <p>
 * <b>스레드와 realm 을 만질 수 있는 쪽.</b> 스크립트 realm 은 한 번에 한 스레드의 것이므로 이 스코프의 realm 을 쓰는 모든 코드는 {@link #lock}(공정한 잠금)을 쥔다. 세 종류의 코드가 쥔다:
 * <ul>
 * <li><em>worklet 스레드</em>(<code>swingwebpanel-audio-worklet-N</code>, 첫 <code>addModule()</code> 에 시작해 스코프와 함께 끝나는 데몬 스레드): 모듈을 평가하고 <em>realm 작업</em>(프로세서 포트로의
 * 메시지 전달)을 실행한다. 모듈은 잠금 <em>없이</em> 가져오므로({@link ModuleGraphFetcher}) 느린 네트워크가 렌더링 스레드를 늦추지 않는다.</li>
 * <li>오디오 컨텍스트의 <em>렌더링 스레드</em>: 프로세서를 만들고(quantum 시작의 제어 메시지이므로 오프라인 컨텍스트의 결과는 실행할 때마다 같다) 노드마다 quantum 마다 한 번
 * <code>process()</code> 를 호출한다. 규격이 스크립트가 실행되기를 바라는 곳이 여기이다. 실시간 컨텍스트에서는 잠금을 {@link WorkletLimits#REALTIME_LOCK_WAIT_MILLIS} ms 동안
 * <em>시도</em>만 한다: worklet 스레드가 바쁘면 노드는 그 quantum 에 무음이고({@link AudioWorkletNode#SKIPPED}) 같은 quantum 의 다른 노드는 전혀 기다리지 않는다. 그러므로 실시간 컨텍스트는
 * 자기 프로세서가 아닌 스크립트를 오래 기다리지 않는다. 오프라인 컨텍스트에는 기한이 없으므로 잠금을 기다리고, quantum 을 처리하기 전에 쌓인 realm 작업을 직접 실행하므로 <code>startRendering()</code>
 * 전(또는 quantum 전)에 보낸 메시지는 그 quantum 전에 결정적으로 전달된다.</li>
 * <li>그 밖에는 아무도 아니다: 페이지는 realm 을 만지지 않는다. 페이지에 필요한 것은 작업 큐를 거쳐 일반 데이터(이름, 설명, 직렬화된 메시지)로 닿는다.</li>
 * </ul>
 * <code>process()</code> 한 번의 호출은 {@link WorkletLimits#PROCESS_STEP_LIMIT} 단계, 실시간 컨텍스트에서는 {@link WorkletLimits#REALTIME_PROCESS_TIME_LIMIT_MILLIS} ms 의 한도가 있다.
 * 넘는 프로세서는 예외를 던지는 프로세서처럼 실패한다(<code>processorerror</code>, 영구 무음). {@link #dispose()}(컨텍스트가 닫혔거나, 페이지가 교체되었거나, 스크립트가 꺼졌다)는 실행 중인 스크립트를
 * 취소하고, 큐를 비우고, 기다리지 않고 worklet 스레드를 끝낸다. 그 뒤 스코프의 노드는 무음이다.
 */
public final class WorkletScope {

    /**
     * Told on the page, as a task of the page, how a call of <code>addModule()</code> ended.
     * <p>
     * <code>addModule()</code> 호출이 어떻게 끝났는지를 페이지의 작업으로 페이지에 알린다.
     */
    public interface Completion {

        /**
         * Called when the module and its imports were evaluated (also when the module threw while it ran: that is reported to the console and the promise resolves), or when the call failed.
         * <p>
         * 모듈과 그것이 import 하는 것을 평가했을 때(모듈이 실행 중 던졌어도 마찬가지: 콘솔에 보고하고 프라미스는 resolve 된다), 또는 호출이 실패했을 때 불린다.
         *
         * @param errorName how the promise is rejected, or null if it resolves: <code>AbortError</code> (a fetch failure: network, scheme, origin policy, MIME type) and
         *                  <code>InvalidStateError</code> are <code>DOMException</code> names; <code>SyntaxError</code> (a syntax error or a module that cannot be linked) and
         *                  <code>TypeError</code> (a specifier that cannot be resolved) are plain error objects of that kind / 프라미스를 어떻게 reject 하는지. resolve 되면 null.
         *                  <code>AbortError</code>(가져오기 실패: 네트워크, 스킴, 오리진 정책, MIME 타입)와 <code>InvalidStateError</code> 는 <code>DOMException</code> 이름이고,
         *                  <code>SyntaxError</code>(구문 오류, 링크할 수 없는 모듈)와 <code>TypeError</code>(해석할 수 없는 지정자)는 그 종류의 일반 오류 객체이다
         * @param message   the message of the error, null on success / 오류 메시지. 성공하면 null
         */
        void finished(String errorName, String message);
    }

    /**
     * A processor class that <code>registerProcessor()</code> registered.
     * <p>
     * <code>registerProcessor()</code> 가 등록한 프로세서 클래스.
     */
    static final class Registration {

        /**
         * The constructor.
         * <p>
         * 생성자.
         */
        final JsObject constructor;

        /**
         * The descriptors of the parameters.
         * <p>
         * 파라미터 설명.
         */
        final List<AudioWorkletNode.ParamDescriptor> descriptors;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param constructor the constructor / 생성자
         * @param descriptors the descriptors / 설명
         */
        Registration(JsObject constructor, List<AudioWorkletNode.ParamDescriptor> descriptors) {
            this.constructor = constructor;
            this.descriptors = Collections.unmodifiableList(new ArrayList<AudioWorkletNode.ParamDescriptor>(descriptors));
        }
    }

    /**
     * The page that owns the scope.
     * <p>
     * 스코프를 소유한 페이지.
     */
    private final WorkletHost host;

    /**
     * The engine context that the scope belongs to.
     * <p>
     * 스코프가 속한 엔진 컨텍스트.
     */
    private final BaseAudioContext engine;

    /**
     * The number in the name of the worklet thread.
     * <p>
     * worklet 스레드 이름의 번호.
     */
    private final int serial;

    /**
     * The lock of the script realm: whoever uses the realm holds it. It is fair, so a thread that waits (the worklet thread, while the rendering thread processes quantum after
     * quantum) is served in its turn.
     * <p>
     * 스크립트 realm 의 잠금: realm 을 쓰는 쪽이 쥔다. 공정한 잠금이므로 기다리는 스레드(렌더링 스레드가 quantum 을 연달아 처리하는 동안의 worklet 스레드)가 차례를 얻는다.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Guards the queues, the thread and the flag that ends them.
     * <p>
     * 큐, 스레드, 그것을 끝내는 표시를 지킨다.
     */
    private final Object queueLock = new Object();

    /**
     * The tasks that run with the lock of the realm held (the deliveries of messages); guarded by {@link #queueLock}.
     * <p>
     * realm 의 잠금을 쥐고 실행되는 작업(메시지 전달). {@link #queueLock} 으로 보호한다.
     */
    private final LinkedList<Runnable> realmTasks = new LinkedList<Runnable>();

    /**
     * The tasks that the worklet thread runs without the lock (the loading of modules, which does network work first); guarded by {@link #queueLock}.
     * <p>
     * 잠금 없이 worklet 스레드가 실행하는 작업(먼저 네트워크 일을 하는 모듈 로드). {@link #queueLock} 으로 보호한다.
     */
    private final LinkedList<Runnable> rawTasks = new LinkedList<Runnable>();

    /**
     * The worklet thread, or null before the first task; guarded by {@link #queueLock}.
     * <p>
     * worklet 스레드. 첫 작업 전에는 null. {@link #queueLock} 으로 보호한다.
     */
    private Thread thread;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * The script realm; created with the lock held, once.
     * <p>
     * 스크립트 realm. 잠금을 쥐고 한 번 만든다.
     */
    private volatile JsRealm realm;

    /**
     * The DOM bridge of the realm (event interfaces and wrappers only).
     * <p>
     * realm 의 DOM 브리지(이벤트 인터페이스와 래퍼만 있다).
     */
    private volatile DomBridge bridge;

    /**
     * <code>Object.freeze</code> of the realm, taken before any module could replace it; guarded by the lock of the realm.
     * <p>
     * realm 의 <code>Object.freeze</code>. 모듈이 바꾸기 전에 가져온다. realm 의 잠금으로 보호한다.
     */
    private JsValue freezeFunction;

    /**
     * The processors that were registered, by name; guarded by the lock of the realm.
     * <p>
     * 등록된 프로세서(이름별). realm 의 잠금으로 보호한다.
     */
    private final Map<String, Registration> registrations = new HashMap<String, Registration>();

    /**
     * The port of the processor that is being constructed (the pending processor construction data of 1.32.2.3); guarded by the lock of the realm.
     * <p>
     * 생성 중인 프로세서의 포트(1.32.2.3 의 pending processor construction data). realm 의 잠금으로 보호한다.
     */
    private PortTarget pendingPort;

    /**
     * The keys of the modules that the realm holds (evaluated or being evaluated), so that a module is fetched and evaluated once.
     * <p>
     * realm 이 가진 모듈의 키(평가했거나 평가 중). 모듈은 한 번만 가져와 평가한다.
     */
    private final Set<ModuleRequest> loadedKeys = Collections.newSetFromMap(new ConcurrentHashMap<ModuleRequest, Boolean>());

    /**
     * The text of the modules that were fetched, by key; what the module loader of the realm reads.
     * <p>
     * 가져온 모듈의 텍스트(키별). realm 의 모듈 로더가 읽는다.
     */
    private final Map<ModuleRequest, String> moduleTexts = new ConcurrentHashMap<ModuleRequest, String>();

    /**
     * The final address of the modules that were fetched (after redirects), by key; relative imports resolve against it.
     * <p>
     * 가져온 모듈의 최종 주소(리디렉션 뒤, 키별). 상대 import 는 이것을 기준으로 해석된다.
     */
    private final Map<String, String> moduleBases = new ConcurrentHashMap<String, String>();

    /**
     * The descriptors of the processors that the page may construct nodes for: the <em>node name to parameter descriptor map</em> of the context (1.32.1). It is filled by
     * tasks of the page, which come before the task that resolves the promise of <code>addModule()</code>.
     * <p>
     * 페이지가 노드를 만들 수 있는 프로세서의 설명: 컨텍스트의 <em>node name to parameter descriptor map</em>(1.32.1). 페이지의 작업이 채우며, 그 작업은
     * <code>addModule()</code> 의 프라미스를 resolve 하는 작업보다 앞선다.
     */
    private final Map<String, List<AudioWorkletNode.ParamDescriptor>> published = new ConcurrentHashMap<String, List<AudioWorkletNode.ParamDescriptor>>();

    /**
     * The script context of the worklet side of the message channels.
     * <p>
     * 메시지 채널의 worklet 쪽 스크립트 문맥.
     */
    private final ScriptContext scriptContext = new WorkletContext();

    /**
     * The address that the console records are attributed to when they have none: the module that was added last.
     * <p>
     * 기록에 주소가 없을 때 콘솔 기록의 출처로 쓰는 주소: 마지막으로 추가한 모듈.
     */
    private volatile String consoleSource = "";

    /**
     * The frame of the last quantum in which the realm was busy for a real-time context; the other nodes of that quantum do not wait.
     * <p>
     * 실시간 컨텍스트에서 realm 이 바빴던 마지막 quantum 의 프레임. 그 quantum 의 다른 노드는 기다리지 않는다.
     */
    private volatile long skipFrame = -1L;

    /**
     * The number of calls of <code>process()</code> that were skipped because the realm was busy.
     * <p>
     * realm 이 바빠서 건너뛴 <code>process()</code> 호출의 수.
     */
    private volatile long skippedCalls;

    /**
     * The number of calls of <code>process()</code>.
     * <p>
     * <code>process()</code> 호출의 수.
     */
    private volatile long calls;

    /**
     * The number of calls of <code>process()</code> in a real-time context that took longer than a render quantum lasts.
     * <p>
     * 실시간 컨텍스트에서 render quantum 이 지속되는 시간보다 오래 걸린 <code>process()</code> 호출의 수.
     */
    private volatile long overrunCalls;

    /**
     * The time of the last warning to the console (nanoseconds), and the counts that it reported; used by the rendering thread only.
     * <p>
     * 콘솔에 마지막으로 경고한 시각(나노초)과 그때 보고한 수. 렌더링 스레드만 쓴다.
     */
    private long lastWarnNanos = System.nanoTime() - 2000000000L;

    /**
     * The number of skipped calls that the last warning reported.
     * <p>
     * 마지막 경고가 보고한 건너뛴 호출의 수.
     */
    private long warnedSkips;

    /**
     * The number of overrun calls that the last warning reported.
     * <p>
     * 마지막 경고가 보고한 느린 호출의 수.
     */
    private long warnedOverruns;

    /**
     * The handler of the failures of listeners (the ports of the processors) in the realm: they go to the console of the page.
     * <p>
     * realm 안의 리스너(프로세서의 포트) 실패를 받는 핸들러: 페이지의 콘솔로 간다.
     */
    private final ListenerErrorHandler listenerErrors = new ListenerErrorHandler() {
        /**
         * Reports the failure to the console of the page.
         * <p>
         * 실패를 페이지의 콘솔에 보고한다.
         *
         * @param target  the target / 대상
         * @param event   the event / 이벤트
         * @param failure the failure / 실패
         */
        public void listenerFailed(EventTarget target, org.duckdns.hjow.swingwebpanel.dom.Event event, Throwable failure) {
            if (failure instanceof RuntimeException) {
                reportException((RuntimeException) failure);
            } else {
                EventTarget.PRINTING_HANDLER.listenerFailed(target, event, failure);
            }
        }
    };

    /**
     * The worklet side of the message channels as a script context: tasks are queued for the worklet thread, which runs them with the lock of the realm held, followed by the
     * microtask checkpoint.
     * <p>
     * 메시지 채널의 worklet 쪽을 스크립트 문맥으로 본 것: 작업은 worklet 스레드를 위해 줄 서고, worklet 스레드가 realm 의 잠금을 쥐고 실행하며 마이크로태스크 검사점이 뒤따른다.
     */
    private final class WorkletContext implements ScriptContext {

        /**
         * Returns the realm.
         * <p>
         * realm 을 반환한다.
         *
         * @return the realm / realm
         */
        public JsRealm getRealm() {
            return realm;
        }

        /**
         * Returns the DOM bridge.
         * <p>
         * DOM 브리지를 반환한다.
         *
         * @return the bridge / 브리지
         */
        public DomBridge getBridge() {
            return bridge;
        }

        /**
         * Queues a task of the realm.
         * <p>
         * realm 의 작업을 줄 세운다.
         *
         * @param task the work / 작업
         */
        public void post(Runnable task) {
            enqueueRealmTask(task);
        }

        /**
         * Tells whether the scope runs scripts.
         * <p>
         * 스코프가 스크립트를 실행하는지 알려 준다.
         *
         * @return true while it does / 그렇다면 true
         */
        public boolean isActive() {
            return !disposed && host.isPageActive();
        }

        /**
         * Returns the origin of the page (a worklet has the origin of its page).
         * <p>
         * 페이지의 오리진을 반환한다(worklet 은 자기 페이지의 오리진을 가진다).
         *
         * @return the origin text, or null for an opaque origin / 오리진 문자열. 불투명 오리진이면 null
         */
        public String getOrigin() {
            String url = host.getPage().getDocument().getUrl();
            Origin o = Origin.of(url == null ? "about:blank" : url);
            return o.isOpaque() ? null : o.serialize();
        }

        /**
         * Returns the handler of the failures of listeners.
         * <p>
         * 리스너 실패의 핸들러를 반환한다.
         *
         * @return the handler / 핸들러
         */
        public ListenerErrorHandler getListenerErrors() {
            return listenerErrors;
        }
    }

    /**
     * Creates the scope of a context; nothing runs until the first {@link #addModule}.
     * <p>
     * 컨텍스트의 스코프를 만든다. 첫 {@link #addModule} 전에는 아무것도 실행되지 않는다.
     *
     * @param host   the page that owns it / 소유한 페이지
     * @param engine the engine context / 엔진 컨텍스트
     * @param serial the number in the name of the worklet thread / worklet 스레드 이름의 번호
     */
    public WorkletScope(WorkletHost host, BaseAudioContext engine, int serial) {
        this.host = host;
        this.engine = engine;
        this.serial = serial;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 페이지가 쓰는 메서드
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the script context of the worklet side, for the message channels of the nodes (<code>ChannelPorts.newChannel</code>).
     * <p>
     * 노드의 메시지 채널을 위한 worklet 쪽 스크립트 문맥을 반환한다(<code>ChannelPorts.newChannel</code>).
     *
     * @return the context / 문맥
     */
    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    /**
     * Loads a module and everything it imports into the global scope, evaluates it (<code>registerProcessor()</code> calls happen here) and tells the page how it ended. A module
     * that the scope has loaded already is not loaded again (the call succeeds). Callable from any thread; the work is done by the worklet thread.
     * <p>
     * 모듈과 그것이 import 하는 모든 것을 전역 범위에 불러와 평가하고(<code>registerProcessor()</code> 호출이 여기서 일어난다) 어떻게 끝났는지 페이지에 알린다. 스코프가 이미 불러온 모듈은 다시
     * 불러오지 않는다(호출은 성공한다). 어느 스레드에서나 부를 수 있으며 일은 worklet 스레드가 한다.
     *
     * @param moduleKey   the absolute address of the module, without a fragment / 모듈의 절대 주소(조각 없음)
     * @param credentials fetch credentials mode / 가져오기 자격 증명 모드
     * @param done        told on the page / 페이지에 알린다
     */
    public void addModule(final String moduleKey, final Credentials credentials, final Completion done) {
        if (disposed) {
            finish(done, "InvalidStateError", "The audio worklet was closed.");
            return;
        }
        consoleSource = moduleKey;
        enqueueRawTask(new Runnable() {
            /**
             * Loads the module on the worklet thread.
             * <p>
             * worklet 스레드에서 모듈을 불러온다.
             */
            public void run() {
                loadModule(moduleKey, credentials, done);
            }
        });
    }

    /**
     * Tells whether the page may construct nodes for a name: the processor was registered and the page was told (the <em>node name to parameter descriptor map</em>).
     * <p>
     * 페이지가 이름에 대해 노드를 만들 수 있는지 알려 준다: 프로세서가 등록되었고 페이지가 통지받았다(<em>node name to parameter descriptor map</em>).
     *
     * @param name the name / 이름
     * @return true if it is known / 알려져 있으면 true
     */
    public boolean hasProcessor(String name) {
        return published.containsKey(name);
    }

    /**
     * Returns the descriptors of the parameters of a processor, as the page knows them.
     * <p>
     * 페이지가 아는 프로세서의 파라미터 설명을 반환한다.
     *
     * @param name the name / 이름
     * @return the descriptors, read only; null if the name is not known / 설명(읽기 전용). 이름을 모르면 null
     */
    public List<AudioWorkletNode.ParamDescriptor> getDescriptors(String name) {
        return published.get(name);
    }

    /**
     * Creates the factory that makes the processor of a node: the control message of the node runs it on the rendering thread at the start of a quantum. The processor is made
     * as the constructor of its class is called with the options and with the end of the message channel that belongs to the processor.
     * <p>
     * 노드의 프로세서를 만드는 팩터리를 만든다: 노드의 제어 메시지가 quantum 시작에 렌더링 스레드에서 실행한다. 프로세서는 그 클래스의 생성자를 옵션과, 프로세서에 속한 메시지 채널의 끝과 함께 호출해
     * 만든다.
     *
     * @param name    the registered name / 등록된 이름
     * @param options the serialized options object (<code>numberOfInputs</code>, <code>processorOptions</code> ...) / 직렬화된 옵션 객체(<code>numberOfInputs</code>,
     *                <code>processorOptions</code> 등)
     * @param port    the end of the message channel that belongs to the processor (made with {@link #getScriptContext()}) / 프로세서에 속한 메시지 채널의 끝
     *                ({@link #getScriptContext()} 로 만든 것)
     * @return the factory / 팩터리
     */
    public AudioWorkletNode.ProcessorFactory newFactory(final String name, final ClonedValue options, final PortTarget port) {
        return new AudioWorkletNode.ProcessorFactory() {
            /**
             * Creates the processor on the rendering thread.
             * <p>
             * 렌더링 스레드에서 프로세서를 만든다.
             *
             * @param node the node / 노드
             * @return the processor, or null if the realm is busy / 프로세서. realm 이 바쁘면 null
             */
            public AudioWorkletNode.Processor create(AudioWorkletNode node) {
                return construct(node, name, options, port);
            }
        };
    }

    /**
     * Tells whether the scope was disposed.
     * <p>
     * 스코프가 폐기되었는지 알려 준다.
     *
     * @return true after {@link #dispose()} / {@link #dispose()} 뒤에는 true
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Tells whether the worklet thread lives (for tests).
     * <p>
     * worklet 스레드가 살아 있는지 알려 준다(시험용).
     *
     * @return true while it lives / 살아 있으면 true
     */
    public boolean isThreadAlive() {
        synchronized (queueLock) {
            return thread != null && thread.isAlive();
        }
    }

    /**
     * Returns the number of calls of <code>process()</code> that were skipped because the realm was busy (for tests and diagnostics).
     * <p>
     * realm 이 바빠서 건너뛴 <code>process()</code> 호출의 수를 반환한다(시험과 진단용).
     *
     * @return the number of calls / 호출 수
     */
    public long getSkippedCallCount() {
        return skippedCalls;
    }

    /**
     * Returns the number of calls of <code>process()</code> that were made (for tests and diagnostics).
     * <p>
     * <code>process()</code> 를 호출한 횟수를 반환한다(시험과 진단용).
     *
     * @return the number of calls / 호출 수
     */
    public long getCallCount() {
        return calls;
    }

    /**
     * Returns the number of calls of <code>process()</code> in a real-time context that took longer than a render quantum lasts (for tests and diagnostics).
     * <p>
     * 실시간 컨텍스트에서 render quantum 이 지속되는 시간보다 오래 걸린 <code>process()</code> 호출의 수를 반환한다(시험과 진단용).
     *
     * @return the number of calls / 호출 수
     */
    public long getOverrunCallCount() {
        return overrunCalls;
    }

    /**
     * Ends the scope at once and from any thread without waiting: the script that runs is cancelled at its next step, the queues are dropped and the worklet thread ends. Nodes
     * whose processors live in the scope are silent afterwards. Calling it again does nothing.
     * <p>
     * 스코프를 기다리지 않고 어느 스레드에서나 즉시 끝낸다: 실행 중인 스크립트는 다음 단계에서 취소되고, 큐가 비워지며, worklet 스레드가 끝난다. 프로세서가 스코프에 있는 노드는 그 뒤 무음이다. 다시
     * 불러도 아무 일도 없다.
     */
    public void dispose() {
        Thread t;
        synchronized (queueLock) {
            if (disposed) {
                return;
            }
            disposed = true;
            realmTasks.clear();
            rawTasks.clear();
            t = thread;
            queueLock.notifyAll();
        }
        JsRealm r = realm;
        if (r != null) {
            r.getLimits().cancel();
        }
        if (t != null) {
            t.interrupt();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // realm 안에서 쓰는 것(패키지 안)
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the script realm; call it with the lock held.
     * <p>
     * 스크립트 realm 을 반환한다. 잠금을 쥔 채 부른다.
     *
     * @return the realm / realm
     */
    JsRealm realm() {
        return realm;
    }

    /**
     * Returns the engine context.
     * <p>
     * 엔진 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    BaseAudioContext engine() {
        return engine;
    }

    /**
     * Creates a <code>DOMException</code> of the realm as an exception to throw.
     * <p>
     * realm 의 <code>DOMException</code> 을 던질 예외로 만든다.
     *
     * @param name    the name / 이름
     * @param message the message / 메시지
     * @return the exception / 예외
     */
    JsException domError(String name, String message) {
        return new JsException(DomExceptionObjects.create(bridge, name, message));
    }

    /**
     * Tells whether a name was registered in the realm; call it with the lock held.
     * <p>
     * 이름이 realm 에 등록되었는지 알려 준다. 잠금을 쥔 채 부른다.
     *
     * @param name the name / 이름
     * @return true if it was registered / 등록되었으면 true
     */
    boolean isRegistered(String name) {
        return registrations.containsKey(name);
    }

    /**
     * Returns the number of registered names; call it with the lock held.
     * <p>
     * 등록된 이름의 수를 반환한다. 잠금을 쥔 채 부른다.
     *
     * @return the number / 수
     */
    int registeredCount() {
        return registrations.size();
    }

    /**
     * Records a registration and tells the page by a task of the page, which comes before the task that resolves the promise of the call of <code>addModule()</code> that is
     * running (tasks of the page keep their order).
     * <p>
     * 등록을 기록하고, 실행 중인 <code>addModule()</code> 호출의 프라미스를 resolve 하는 작업보다 앞서는 페이지의 작업으로 페이지에 알린다(페이지의 작업은 순서를 지킨다).
     *
     * @param name        the name / 이름
     * @param constructor the constructor / 생성자
     * @param descriptors the descriptors / 설명
     */
    void addRegistration(final String name, JsObject constructor, List<AudioWorkletNode.ParamDescriptor> descriptors) {
        final Registration r = new Registration(constructor, descriptors);
        registrations.put(name, r);
        host.postToPage(new Runnable() {
            /**
             * Makes the name known to the page.
             * <p>
             * 이름을 페이지가 알게 한다.
             */
            public void run() {
                published.put(name, r.descriptors);
            }
        });
    }

    /**
     * Takes the port of the processor that is being constructed (<code>AudioWorkletProcessor</code> reads it once); null if no node is being constructed.
     * <p>
     * 생성 중인 프로세서의 포트를 가져간다(<code>AudioWorkletProcessor</code> 가 한 번 읽는다). 생성 중인 노드가 없으면 null.
     *
     * @return the script object of the port, or null / 포트의 스크립트 객체. 없으면 null
     */
    JsObject takePendingPort() {
        PortTarget p = pendingPort;
        pendingPort = null;
        return p == null ? null : bridge.wrap(p);
    }

    /**
     * Freezes an object with the <code>Object.freeze</code> of the realm that was taken when the realm was built.
     * <p>
     * realm 을 만들 때 가져온 realm 의 <code>Object.freeze</code> 로 객체를 얼린다.
     *
     * @param o the object / 객체
     */
    void freeze(JsObject o) {
        Ops.call(freezeFunction, Js.UNDEFINED, new JsValue[] {o});
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 작업 큐와 스레드
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Queues a task of the realm (the delivery of a message): the worklet thread runs it with the lock held, an offline rendering runs it before its next quantum.
     * <p>
     * realm 의 작업(메시지 전달)을 줄 세운다: worklet 스레드가 잠금을 쥐고 실행하고, 오프라인 렌더링은 다음 quantum 전에 실행한다.
     *
     * @param task the work / 작업
     */
    private void enqueueRealmTask(Runnable task) {
        enqueueRealmTask(task, null);
    }

    /**
     * Queues a realm task with a microtask fallback for external completions when the task queue is full.
     * <p>
     * realm 작업을 예약한다. 작업 큐가 가득 찼을 때 외부 완료 작업은 마이크로태스크 큐로 넘긴다.
     *
     * @param task the work / 작업
     * @param externalRealm the realm of an external job, or null for ordinary messages / 외부 작업의 realm. 일반 메시지는 null
     */
    private void enqueueRealmTask(Runnable task, JsRealm externalRealm) {
        synchronized (queueLock) {
            if (disposed) {
                return;
            }
            if (realmTasks.size() >= WorkletLimits.MAX_PENDING_TASKS) {
                // 예약한 외부 완료는 이미 대기 중인 realm 작업의 검사점에 넘겨 잃어버리지 않는다.
                if (externalRealm != null) {
                    externalRealm.enqueueJob(task);
                }
                return;
            }
            realmTasks.add(task);
            startThread();
            queueLock.notifyAll();
        }
    }

    /**
     * Queues a task that the worklet thread runs without the lock.
     * <p>
     * 잠금 없이 worklet 스레드가 실행할 작업을 줄 세운다.
     *
     * @param task the work / 작업
     */
    private void enqueueRawTask(Runnable task) {
        synchronized (queueLock) {
            if (disposed || rawTasks.size() >= WorkletLimits.MAX_PENDING_TASKS) {
                return;
            }
            rawTasks.add(task);
            startThread();
            queueLock.notifyAll();
        }
    }

    /**
     * Starts the worklet thread if it does not run yet; call it with {@link #queueLock} held.
     * <p>
     * worklet 스레드가 아직 없으면 시작한다. {@link #queueLock} 을 쥔 채 부른다.
     */
    private void startThread() {
        if (thread != null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            /**
             * Runs the loop of the worklet thread.
             * <p>
             * worklet 스레드의 루프를 실행한다.
             */
            public void run() {
                loop();
            }
        }, "swingwebpanel-audio-worklet-" + serial);
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    /**
     * The loop of the worklet thread: it waits for a task, runs the loading of a module, or runs the tasks of the realm under the lock; it ends with the scope.
     * <p>
     * worklet 스레드의 루프: 작업을 기다렸다가 모듈 로드를 실행하거나, 잠금 아래에서 realm 의 작업을 실행한다. 스코프와 함께 끝난다.
     */
    private void loop() {
        // 스코프가 끝날 때까지 작업을 기다려 하나씩 실행한다.
        for (;;) {
            Runnable raw = null;
            synchronized (queueLock) {
                // 일이 없으면 기다린다. 폐기되거나 인터럽트되면 루프를 끝낸다.
                while (!disposed && rawTasks.isEmpty() && realmTasks.isEmpty()) {
                    try {
                        queueLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (disposed) {
                    return;
                }
                if (!rawTasks.isEmpty()) {
                    raw = rawTasks.removeFirst();
                }
            }
            if (raw != null) {
                try {
                    raw.run();
                } catch (RuntimeException e) {
                    report(ConsoleRecord.Kind.HOST_ERROR, "Internal error of the audio worklet: " + e, "", 0, 0);
                }
            } else {
                lock.lock();
                try {
                    drainRealmTasks();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Runs the tasks of the realm that wait, in order; call it with the lock held.
     * <p>
     * 기다리는 realm 의 작업을 순서대로 실행한다. 잠금을 쥔 채 부른다.
     */
    private void drainRealmTasks() {
        // 줄 선 작업이 없어질 때까지 하나씩 꺼내 작업으로 실행한다.
        for (;;) {
            Runnable t;
            synchronized (queueLock) {
                t = realmTasks.isEmpty() ? null : realmTasks.removeFirst();
            }
            if (t == null || disposed) {
                return;
            }
            runTask(t);
        }
    }

    /**
     * Runs a task of the realm with the lock held: the limits of a task of the page, the work, and the microtask checkpoint; what the script throws is reported to the console.
     * <p>
     * realm 의 작업을 잠금을 쥔 채 실행한다: 페이지의 작업과 같은 한도, 작업, 마이크로태스크 검사점. 스크립트가 던지는 것은 콘솔에 보고한다.
     *
     * @param body the work / 작업
     */
    private void runTask(Runnable body) {
        ensureRealm();
        JsRealm r = realm;
        ExecutionLimits limits = r.getLimits();
        limits.setMaxSteps(ExecutionLimits.DEFAULT_MAX_STEPS);
        limits.setMaxTimeMillis(host.getPage().getEnvironment().getTaskTimeLimitMillis());
        limits.beginTask();
        try {
            body.run();
            r.runJobs();
        } catch (RuntimeException e) {
            reportException(e);
        } finally {
            limits.endTask();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 모듈
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells the page how a call of <code>addModule()</code> ended, by a task of the page.
     * <p>
     * <code>addModule()</code> 호출이 어떻게 끝났는지를 페이지의 작업으로 페이지에 알린다.
     *
     * @param done      the completion / 완료
     * @param errorName the name of the error, null on success / 오류 이름. 성공하면 null
     * @param message   the message, null on success / 메시지. 성공하면 null
     */
    private void finish(final Completion done, final String errorName, final String message) {
        host.postToPage(new Runnable() {
            /**
             * Tells the completion.
             * <p>
             * 완료에 알린다.
             */
            public void run() {
                done.finished(errorName, message);
            }
        });
    }

    /**
     * Loads and evaluates a module on the worklet thread: fetches the graph without the lock, then loads, links and evaluates it in the realm with the lock held.
     * <p>
     * worklet 스레드에서 모듈을 불러와 평가한다: 그래프를 잠금 없이 가져온 뒤, 잠금을 쥐고 realm 에서 불러오고 링크하고 평가한다.
     *
     * @param key         the key of the entry module / 진입 모듈의 키
     * @param credentials fetch credentials mode / 가져오기 자격 증명 모드
     * @param done        the completion / 완료
     */
    private void loadModule(String key, Credentials credentials, Completion done) {
        if (disposed) {
            return;
        }
        ModuleRequest entry = new ModuleRequest(key);
        // 이미 불러온 JavaScript 진입 모듈이면 다시 가져오지 않고 성공으로 끝낸다.
        if (loadedKeys.contains(entry)) {
            finish(done, null, null);
            return;
        }
        Map<ModuleRequest, String> texts = new HashMap<ModuleRequest, String>();
        Map<String, String> bases = new HashMap<String, String>();
        try {
            ModuleGraphFetcher.fetch(host.getPage().getEnvironment().getScriptSource(), entry, credentials, texts, bases, loadedKeys);
        } catch (IOException e) {
            report(ConsoleRecord.Kind.HOST_ERROR, "Failed to load the audio worklet module " + key + ": " + e.getMessage(), key, 0, 0);
            finish(done, "AbortError", "Unable to load a worklet's module.");
            return;
        } catch (JsException e) {
            // 구문 오류와 해석할 수 없는 지정자는 "error to rethrow" 이다: 그 오류(SyntaxError, TypeError)로 reject 한다.
            reportException(e);
            String name = errorNameOf(e);
            finish(done, name == null ? "AbortError" : name, name == null ? "Unable to load a worklet's module." : messageOf(e));
            return;
        } catch (RuntimeException e) {
            reportException(e);
            finish(done, "AbortError", "Unable to load a worklet's module.");
            return;
        }
        if (disposed) {
            return;
        }
        moduleTexts.putAll(texts);
        moduleBases.putAll(bases);
        lock.lock();
        try {
            if (disposed) {
                return;
            }
            ensureRealm();
            String[] rejection = evaluate(entry);
            // 평가에 이르렀으면(평가 중 예외로 끝났어도) 모듈은 평가한 것으로 친다. 같은 모듈은 다시 평가하지 않는다.
            if (rejection == null) {
                loadedKeys.addAll(texts.keySet());
            }
            finish(done, rejection == null ? null : rejection[0], rejection == null ? null : rejection[1]);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Loads, links and evaluates a module in the realm with the lock held (the HTML Standard step "run a module script" after "fetch a worklet script graph"). What fails <em>before</em> the
     * module runs, a syntax error or a module that cannot be linked (an import of a name that the module does not export), is an <em>error to rethrow</em>: the promise of
     * <code>addModule()</code> is rejected with that error. An exception that the module throws <em>while it runs</em> (a <code>throw</code>, an error of <code>registerProcessor()</code> in the
     * module) does not reject the promise: it is reported to the console as an uncaught exception and the promise resolves (the names that were registered before the exception stay registered).
     * <p>
     * 모듈을 잠금을 쥔 채 realm 에서 불러오고, 링크하고, 평가한다(HTML Standard 의 "fetch a worklet script graph" 뒤 "run a module script" 단계). 모듈이 실행되기 <em>전에</em> 실패하는 것, 곧 구문 오류와
     * 링크할 수 없는 모듈(모듈이 export 하지 않은 이름의 import)은 <em>error to rethrow</em> 이다: <code>addModule()</code> 의 프라미스는 그 오류로 reject 된다. 모듈이 <em>실행되는 동안</em> 던지는 예외
     * (<code>throw</code>, 모듈 안의 <code>registerProcessor()</code> 오류)는 프라미스를 reject 하지 않는다: 처리되지 않은 예외로 콘솔에 보고하고 프라미스는 resolve 된다(예외 전에 등록한 이름은 등록된 채로 남는다).
     *
     * @param entry a resolved JavaScript module request / 해석된 JavaScript 모듈 요청
     * @return null if the promise resolves; otherwise the name of the error that rejects it and its message / 프라미스가 resolve 되면 null. 아니면 reject 하는 오류의 이름과 메시지
     */
    private String[] evaluate(ModuleRequest entry) {
        JsRealm r = realm;
        ExecutionLimits limits = r.getLimits();
        limits.setMaxSteps(ExecutionLimits.DEFAULT_MAX_STEPS);
        limits.setMaxTimeMillis(host.getPage().getEnvironment().getTaskTimeLimitMillis());
        ModuleGraph graph;
        try {
            graph = r.loadModule(entry.getSpecifier(), moduleTexts.get(entry));
        } catch (RuntimeException e) {
            // 불러오는 단계(파싱, 의존 모듈 해석)의 실패는 error to rethrow 이다.
            if (!disposed) {
                reportException(e);
            }
            String name = e instanceof JsException ? errorNameOf((JsException) e) : null;
            return new String[] {name == null ? "AbortError" : name, name == null ? "Unable to load a worklet's module." : messageOf((JsException) e)};
        }
        try {
            graph.evaluate();
            limits.beginTask();
            try {
                r.runJobs();
            } finally {
                limits.endTask();
            }
            return null;
        } catch (RuntimeException e) {
            if (disposed) {
                return null;
            }
            reportException(e);
            // 링크 단계의 실패(import 한 이름을 export 하지 않음)는 error to rethrow 이므로 reject 한다. 모듈이 실행 중 던진 예외는 resolve 한다.
            if (e instanceof JsException && isLinkError((JsException) e)) {
                return new String[] {"SyntaxError", messageOf((JsException) e)};
            }
            return null;
        }
    }

    /**
     * Tells whether an exception is a failure of linking the modules: the engine throws a <code>SyntaxError</code> whose message starts with "The requested module" when an import
     * names something that the imported module does not export (or exports ambiguously). A <code>SyntaxError</code> that the script of a module throws has another message.
     * <p>
     * 예외가 모듈 링크의 실패인지 알려 준다: import 가 가져온 모듈이 export 하지 않는(또는 모호하게 export 하는) 이름을 가리키면 엔진이 "The requested module" 로 시작하는 메시지의
     * <code>SyntaxError</code> 를 던진다. 모듈의 스크립트가 던지는 <code>SyntaxError</code> 는 메시지가 다르다.
     *
     * @param e the exception / 예외
     * @return true for a link error / 링크 오류이면 true
     */
    private static boolean isLinkError(JsException e) {
        String m = e.getErrorMessage();
        return e.getKind() == ErrorKind.SYNTAX_ERROR && m != null && m.startsWith("The requested module '");
    }

    /**
     * Returns the name of the error that rejects the promise for an exception of the loading, or null if the exception is no <code>SyntaxError</code> or <code>TypeError</code>.
     * <p>
     * 불러오기 단계의 예외에 대해 프라미스를 reject 하는 오류의 이름을 반환한다. <code>SyntaxError</code> 나 <code>TypeError</code> 가 아니면 null.
     *
     * @param e the exception / 예외
     * @return <code>SyntaxError</code>, <code>TypeError</code> or null / <code>SyntaxError</code>, <code>TypeError</code>, 또는 null
     */
    private static String errorNameOf(JsException e) {
        ErrorKind k = e.getKind();
        return k == ErrorKind.SYNTAX_ERROR ? "SyntaxError" : k == ErrorKind.TYPE_ERROR ? "TypeError" : null;
    }

    /**
     * Returns the message of an exception without the name of its kind.
     * <p>
     * 예외의 메시지를 종류의 이름 없이 반환한다.
     *
     * @param e the exception / 예외
     * @return the message / 메시지
     */
    private static String messageOf(JsException e) {
        return e.getErrorMessage() != null ? e.getErrorMessage() : String.valueOf(e.getMessage());
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // realm 만들기
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates the realm when it is needed for the first time; call it with the lock held.
     * <p>
     * realm 이 처음 필요할 때 만든다. 잠금을 쥔 채 부른다.
     */
    private void ensureRealm() {
        if (realm != null) {
            return;
        }
        final JsRealm r = JsRealm.create();
        SharedMemoryScope.bind(r, host.getPage().getEnvironment().getSharedMemoryCluster(), Origin.of(host.getPage().getDocument().getOriginUrl()));
        r.setDynamicImportEnabled(false);
        r.setAtomicsWaitAllowed(false);
        r.setExternalJobExecutor(new Executor() {
            public void execute(Runnable job) {
                enqueueRealmTask(job, r);
            }
        });
        DomBridge b = new DomBridge(r);
        realm = r;
        bridge = b;
        r.setConsole(new ConsoleRecordHook() {
            /**
             * Sends a record of the console of the worklet to the console of the page.
             * <p>
             * worklet 콘솔의 기록을 페이지의 콘솔로 보낸다.
             *
             * @param record the record / 기록
             */
            public void record(ConsoleRecord record) {
                if (!disposed) {
                    host.getPage().workerConsole(record, consoleSource, false);
                }
            }

            /**
             * Sends a message in the plain form (a record without a position).
             * <p>
             * 일반 형태의 메시지를 보낸다(위치 없는 기록).
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                record(ConsoleRecord.call(level, level, text, Collections.singletonList(text == null ? "" : text), consoleSource, 0, 0, 0));
            }
        });
        b.setErrorSink(new DomBridge.ErrorSink() {
            /**
             * Reports an error that a listener or callback threw.
             * <p>
             * 리스너나 콜백이 던진 오류를 보고한다.
             *
             * @param error the error / 오류
             */
            public void scriptError(RuntimeException error) {
                reportException(error);
            }
        });
        DomInterfaces.installEventCore(b);
        MessagingApi.installMessageEvent(b, r);
        ChannelPorts.install(scriptContext);
        // AudioWorkletGlobalScope 에는 MessageChannel 생성자가 없다(MessagePort 만 노출한다).
        r.getGlobalObject().delete("MessageChannel");
        freezeFunction = ((JsObject) r.getGlobalObject().get("Object")).get("freeze");
        WorkletGlobals.install(this);
        r.setModuleLoader(new ModuleLoader.WithAttributes() {
            /**
             * Resolves a specifier against the final address of the importing module.
             * <p>
             * 지정자를 가져오는 모듈의 최종 주소를 기준으로 해석한다.
             *
             * @param specifier the specifier / 지정자
             * @param referrer  the key of the importing module / 가져오는 모듈의 키
             * @return the key / 키
             */
            public String resolve(String specifier, String referrer) {
                String known = referrer == null ? null : moduleBases.get(referrer);
                return ModuleSources.resolve(known != null ? known : referrer == null ? host.getPage().getDocument().getBaseUri() : referrer, specifier);
            }

            /**
             * Returns the text of a module that was fetched before the realm asked.
             * <p>
             * realm 이 묻기 전에 가져온 모듈의 텍스트를 반환한다.
             *
             * @param key the key / 키
             * @return the text / 텍스트
             */
            public String fetch(String key) {
                return fetch(new ModuleRequest(key));
            }

            /**
             * Looks up source by the URL and attributes used for prefetching.
             * <p>
             * 선취득에 쓴 URL과 속성으로 소스를 찾는다.
             *
             * @param request resolved module request / 해석된 모듈 요청
             * @return the text / 텍스트
             */
            public String fetch(ModuleRequest request) {
                String text = moduleTexts.get(request);
                if (text == null) {
                    throw JsException.typeError("Failed to fetch module " + request);
                }
                return text;
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 프로세서
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Takes the lock of the realm for the rendering thread: an offline context waits for it (and a cancellation interrupts the wait), a real-time context only tries for a
     * short time, and not at all when another node of the same quantum found the realm busy.
     * <p>
     * 렌더링 스레드를 위해 realm 의 잠금을 쥔다: 오프라인 컨텍스트는 기다리고(취소는 대기를 인터럽트한다), 실시간 컨텍스트는 잠깐 시도만 하며, 같은 quantum 의 다른 노드가 realm 이 바쁨을 봤으면
     * 전혀 시도하지 않는다.
     *
     * @param offline true for an offline context / 오프라인 컨텍스트이면 true
     * @param waitMs  how long a real-time context waits / 실시간 컨텍스트가 기다리는 시간
     * @param frame   the first frame of the quantum, to remember a busy realm, or -1 / 바쁜 realm 을 기억하는 데 쓰는 quantum 의 첫 프레임. 없으면 -1
     * @return true if the lock is held / 잠금을 쥐었으면 true
     */
    private boolean acquire(boolean offline, long waitMs, long frame) {
        try {
            if (offline) {
                lock.lockInterruptibly();
                return true;
            }
            if (frame >= 0 && skipFrame == frame) {
                return false;
            }
            if (lock.tryLock(waitMs, TimeUnit.MILLISECONDS)) {
                return true;
            }
            if (frame >= 0) {
                skipFrame = frame;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Constructs the processor of a node: creates the instance of the registered class with the options, with the port that the node made pending for the constructor of
     * <code>AudioWorkletProcessor</code> (1.32.2.3). Runs on the rendering thread as the control message of the node.
     * <p>
     * 노드의 프로세서를 만든다: 등록된 클래스의 인스턴스를 옵션과, <code>AudioWorkletProcessor</code> 의 생성자를 위해 노드가 대기시킨 포트와 함께 만든다(1.32.2.3). 노드의 제어 메시지로
     * 렌더링 스레드에서 실행된다.
     *
     * @param node    the node / 노드
     * @param name    the name / 이름
     * @param options the serialized options / 직렬화된 옵션
     * @param port    the end of the channel that belongs to the processor / 프로세서에 속한 채널의 끝
     * @return the processor, or null if the realm is busy and the construction has to be tried again / 프로세서. realm 이 바빠서 다시 시도해야 하면 null
     */
    private AudioWorkletNode.Processor construct(AudioWorkletNode node, String name, ClonedValue options, PortTarget port) {
        if (disposed) {
            throw new WorkletProcessorException("The audio worklet was closed.", "", 0, 0);
        }
        boolean offline = node.getContext() instanceof OfflineAudioContext;
        if (!acquire(offline, WorkletLimits.REALTIME_CONSTRUCT_WAIT_MILLIS, -1L)) {
            return null;
        }
        try {
            if (disposed) {
                throw new WorkletProcessorException("The audio worklet was closed.", "", 0, 0);
            }
            ensureRealm();
            if (offline) {
                drainRealmTasks();
            }
            Registration reg = registrations.get(name);
            if (reg == null) {
                throw new WorkletProcessorException("The processor '" + name + "' is not registered.", "", 0, 0);
            }
            JsRealm r = realm;
            ExecutionLimits limits = r.getLimits();
            limits.setMaxSteps(ExecutionLimits.DEFAULT_MAX_STEPS);
            limits.setMaxTimeMillis(host.getPage().getEnvironment().getTaskTimeLimitMillis());
            limits.beginTask();
            try {
                pendingPort = port;
                JsValue optionsValue = StructuredClone.deserializeForMessaging(r, options, null, null);
                JsObject instance = reg.constructor.construct(new JsValue[] {optionsValue}, reg.constructor);
                if (!(instance instanceof ProcessorObject)) {
                    throw new WorkletProcessorException("The constructor of the processor '" + name + "' did not create an AudioWorkletProcessor.", "", 0, 0);
                }
                r.runJobs();
                return new ProcessorRunner(this, instance, node.getParameters());
            } catch (JsException e) {
                throw failure(e);
            } catch (ScriptAbortedException e) {
                throw failure(e);
            } finally {
                pendingPort = null;
                limits.endTask();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Calls <code>process()</code> of a processor for one quantum, on the rendering thread (see the description of the class for the locking and the limits).
     * <p>
     * 프로세서의 <code>process()</code> 를 quantum 하나에 대해 렌더링 스레드에서 호출한다(잠금과 한도는 클래스 설명 참고).
     *
     * @param runner the processor / 프로세서
     * @param node   the node / 노드
     * @param frames the number of frames / 프레임 수
     * @return {@link AudioWorkletNode#ACTIVE}, {@link AudioWorkletNode#INACTIVE} or {@link AudioWorkletNode#SKIPPED} / {@link AudioWorkletNode#ACTIVE},
     *         {@link AudioWorkletNode#INACTIVE}, {@link AudioWorkletNode#SKIPPED} 중 하나
     */
    int runProcessor(ProcessorRunner runner, AudioWorkletNode node, int frames) {
        if (disposed) {
            return AudioWorkletNode.SKIPPED;
        }
        boolean offline = node.getContext() instanceof OfflineAudioContext;
        if (!acquire(offline, WorkletLimits.REALTIME_LOCK_WAIT_MILLIS, node.getContext().getCurrentFrame())) {
            skippedCalls++;
            warnIfTrouble();
            return AudioWorkletNode.SKIPPED;
        }
        try {
            if (disposed) {
                return AudioWorkletNode.SKIPPED;
            }
            // 오프라인 렌더링은 quantum 을 처리하기 전에 쌓인 realm 작업(메시지)을 직접 실행해 결과가 결정적이게 한다.
            if (offline) {
                drainRealmTasks();
            }
            // 건너뛴 quantum 이 있었으면 (초당 한 번까지) 그 수를 알린다.
            if (!offline) {
                warnIfTrouble();
            }
            ExecutionLimits limits = realm.getLimits();
            limits.setMaxSteps(WorkletLimits.PROCESS_STEP_LIMIT);
            limits.setMaxTimeMillis(offline ? 0L : WorkletLimits.REALTIME_PROCESS_TIME_LIMIT_MILLIS);
            limits.beginTask();
            calls++;
            long started = offline ? 0L : System.nanoTime();
            try {
                int result = runner.invoke(node, frames);
                // 실시간에서는 호출이 render quantum 의 길이보다 오래 걸렸는지 센다. 그만큼 오디오가 장치 버퍼보다 늦게 만들어진다.
                if (!offline && System.nanoTime() - started > quantumNanos(node)) {
                    overrunCalls++;
                    warnIfTrouble();
                }
                return result;
            } catch (JsException e) {
                throw failure(e);
            } catch (ScriptAbortedException e) {
                // 스코프를 폐기해서 취소된 것은 실패가 아니다. 그 밖의 중단(한도)은 실패로 친다.
                if (disposed || e.getReason() == ScriptAbortedException.Reason.CANCELLED) {
                    return AudioWorkletNode.SKIPPED;
                }
                throw failure(e);
            } finally {
                limits.endTask();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns how long a render quantum of the context of a node lasts.
     * <p>
     * 노드의 컨텍스트에서 render quantum 하나가 지속되는 시간을 반환한다.
     *
     * @param node the node / 노드
     * @return the time in nanoseconds / 시간(나노초)
     */
    private static long quantumNanos(AudioWorkletNode node) {
        BaseAudioContext c = node.getContext();
        return (long) (1.0e9 * c.getRenderQuantumSize() / c.getSampleRate());
    }

    /**
     * Warns on the console of the page, at most once per second, that the worklet could not keep up with the clock of a real-time context: quanta of a processor that were skipped (the script realm
     * was busy, so the node was silent in them) and calls of <code>process()</code> that took longer than a render quantum. The rendering thread calls it; it only writes to the console when the counts
     * changed since the last warning, so a healthy worklet is never reported.
     * <p>
     * 실시간 컨텍스트의 시계를 worklet 이 따라가지 못했음을 페이지의 콘솔에 초당 한 번까지 경고한다: 건너뛴 프로세서의 quantum(스크립트 realm 이 바빠서 노드가 그 quantum 에 무음이었다)과 render quantum 보다
     * 오래 걸린 <code>process()</code> 호출. 렌더링 스레드가 부르며, 마지막 경고 뒤에 수가 바뀐 경우에만 콘솔에 쓰므로 멀쩡한 worklet 은 보고되지 않는다.
     */
    private void warnIfTrouble() {
        long now = System.nanoTime();
        if (now - lastWarnNanos < 1000000000L || (skippedCalls == warnedSkips && overrunCalls == warnedOverruns)) {
            return;
        }
        lastWarnNanos = now;
        long skips = skippedCalls;
        long overruns = overrunCalls;
        String text = "AudioWorklet: the audio worklet could not keep up with the clock: " + (skips - warnedSkips) + " quanta of processors were silent because the script was busy, and "
                + (overruns - warnedOverruns) + " calls of process() took longer than a render quantum.";
        warnedSkips = skips;
        warnedOverruns = overruns;
        if (!disposed) {
            host.getPage().workerConsole(ConsoleRecord.call("warn", "warn", text, Collections.singletonList(text), consoleSource, 0, 0, 0), consoleSource, false);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 오류와 콘솔
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Describes a failure of the script as the failure of a processor, and writes it to the console of the page.
     * <p>
     * 스크립트의 실패를 프로세서의 실패로 기술하고 페이지의 콘솔에 쓴다.
     *
     * @param e the exception that the script threw, or the abort / 스크립트가 던진 예외 또는 중단
     * @return the failure / 실패
     */
    private WorkletProcessorException failure(RuntimeException e) {
        String message = String.valueOf(e);
        String source = "";
        int line = 0;
        int col = 0;
        if (e instanceof JsException) {
            JsException je = (JsException) e;
            message = describe(je);
            // 던진 위치(런타임 오류)가 있으면 그것을, 없으면 구문 오류의 위치를 쓴다.
            source = sourceOf(je);
            line = je.getThrowLine() > 0 ? je.getThrowLine() : je.getLine();
            col = je.getThrowLine() > 0 ? je.getThrowColumn() : je.getColumn();
        } else if (e instanceof ScriptAbortedException) {
            message = "Script aborted: " + e.getMessage();
        }
        reportException(e);
        return new WorkletProcessorException(message, source, line, col);
    }

    /**
     * Describes an exception of the script as text, as the console shows it: the description of the thrown value (<code>Error: boom</code>), or, for an error that the engine or the host threw
     * without a value (a failed <code>registerProcessor()</code>, a syntax error), the name of its kind and its message (<code>TypeError: ...</code>).
     * <p>
     * 스크립트의 예외를 콘솔이 보여 주는 문자열로 기술한다: 던진 값의 설명(<code>Error: boom</code>), 또는 엔진이나 호스트가 값 없이 던진 오류(실패한 <code>registerProcessor()</code>, 구문 오류)는 종류의 이름과
     * 메시지(<code>TypeError: ...</code>).
     *
     * @param je the exception / 예외
     * @return the text / 문자열
     */
    private String describe(JsException je) {
        try {
            if (je.hasValue()) {
                return JsException.describe(je.getValue(realm));
            }
        } catch (RuntimeException ignored) {
            // realm 이 아직 없거나 값을 만들 수 없으면 아래의 메시지로 대신한다.
        }
        String m = je.getErrorMessage();
        if (m == null) {
            return String.valueOf(je.getMessage());
        }
        return je.getKind() == null ? m : je.getKind().getErrorName() + ": " + m;
    }

    /**
     * Returns the name of the script of an exception: where it was thrown if that is known, otherwise the script of its syntax error.
     * <p>
     * 예외의 스크립트 이름을 반환한다: 던진 곳을 알면 그곳, 아니면 구문 오류의 스크립트.
     *
     * @param e the exception / 예외
     * @return the name, an empty string for none / 이름. 없으면 빈 문자열
     */
    private static String sourceOf(JsException e) {
        String s = e.getThrowLine() > 0 && e.getThrowSourceName() != null ? e.getThrowSourceName() : e.getSourceName();
        return s == null ? "" : s;
    }

    /**
     * Writes an exception of the script to the console of the page.
     * <p>
     * 스크립트의 예외를 페이지의 콘솔에 쓴다.
     *
     * @param e the exception / 예외
     */
    private void reportException(RuntimeException e) {
        if (disposed) {
            return;
        }
        if (e instanceof ScriptAbortedException) {
            report(ConsoleRecord.Kind.SCRIPT_ABORTED, "Script aborted: " + e.getMessage(), "", 0, 0);
        } else if (e instanceof JsException) {
            JsException je = (JsException) e;
            report(ConsoleRecord.Kind.UNCAUGHT_EXCEPTION, "Uncaught " + describe(je), sourceOf(je),
                    je.getThrowLine() > 0 ? je.getThrowLine() : je.getLine(), je.getThrowLine() > 0 ? je.getThrowColumn() : je.getColumn());
        } else {
            report(ConsoleRecord.Kind.HOST_ERROR, "Internal error of the audio worklet: " + e, "", 0, 0);
        }
    }

    /**
     * Writes a report to the console of the page, attributed to the worklet.
     * <p>
     * 페이지의 콘솔에 worklet 의 것으로 보고를 쓴다.
     *
     * @param kind   the kind / 종류
     * @param text   the text / 문자열
     * @param source the script / 스크립트
     * @param line   the line / 줄
     * @param column the column / 열
     */
    private void report(ConsoleRecord.Kind kind, String text, String source, int line, int column) {
        if (!disposed) {
            host.getPage().workerConsole(ConsoleRecord.report(kind, text, source, line, column), consoleSource, false);
        }
    }
}
