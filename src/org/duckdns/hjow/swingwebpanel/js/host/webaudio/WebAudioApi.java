package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioListener;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioParam;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.PeriodicWave;

/**
 * The Web Audio API of one page (plan item P25; W3C Web Audio API 1.0 Recommendation of 2021-06-17): the script interfaces <code>AudioContext</code>,
 * <code>OfflineAudioContext</code>, <code>AudioNode</code> and the nodes, <code>AudioParam</code>, <code>AudioBuffer</code>, <code>AudioListener</code>,
 * <code>PeriodicWave</code> and the events, over the pure Java engine of {@code org.duckdns.hjow.swingwebpanel.media.webaudio}. This object installs them on the global object of the page
 * and owns what they share: the delivery of what other threads produce (the end of a source, the end of an offline rendering, a decoded buffer) to the thread that owns the
 * script realm as tasks of the page, the list of the contexts that are open, the limits of the page, the conversion of the engine's errors to script exceptions, and the
 * disposal of everything when the page is replaced, its scripts are switched off or it is disposed.
 * <p>
 * <b>Threads.</b> The script realm belongs to the page thread: every interface call, every event and every settled promise runs there, as a task of the page
 * ({@link PageScripts#runTask(Runnable)}), through the {@link Scheduler} of the component. The engine has its own threads (the rendering thread of a real-time context, the thread that
 * renders an offline context, the decoding threads); they never call the script. What they have to tell the script goes through {@link #postTask(Runnable)}, which
 * drops it silently after the page was disposed, so a late result never touches a new page. Control calls go the other way directly: the engine accepts them from any
 * thread (see {@code BaseAudioContext}).
 * <p>
 * <b>Limits.</b> {@link WebAudioHostLimits} lists them: the number of open contexts of a page, the memory that its offline contexts may hold, and the number of
 * pending decodings (the decoding service of the engine). Exceeding a limit is a <code>NotSupportedError</code>, as the specification allows an implementation-defined
 * maximum number of contexts.
 * <p>
 * 한 페이지의 Web Audio API(계획 P25. W3C Web Audio API 1.0 권고안, 2021-06-17): <code>AudioContext</code>, <code>OfflineAudioContext</code>, <code>AudioNode</code> 와 노드,
 * <code>AudioParam</code>, <code>AudioBuffer</code>, <code>AudioListener</code>, <code>PeriodicWave</code>, 이벤트의 스크립트 인터페이스를
 * {@code org.duckdns.hjow.swingwebpanel.media.webaudio} 의 순수 Java 엔진 위에 만든다. 이 객체가 그것들을 페이지의 전역 객체에 설치하고 공유하는 것을 소유한다: 다른 스레드가 만든
 * 결과(원천의 끝, 오프라인 렌더링의 끝, 디코딩된 버퍼)를 스크립트 realm 을 소유한 스레드에 페이지의 작업으로 전달하는 일, 열려 있는 컨텍스트의 목록, 페이지의 한도,
 * 엔진의 오류를 스크립트 예외로 바꾸는 일, 페이지가 교체되거나 스크립트가 꺼지거나 폐기될 때 모든 것을 정리하는 일.
 * <p>
 * <b>스레드.</b> 스크립트 realm 은 페이지 스레드의 것이다: 모든 인터페이스 호출, 모든 이벤트, 확정되는 모든 프라미스가 컴포넌트의 {@link Scheduler} 를 거쳐 페이지의 작업
 * ({@link PageScripts#runTask(Runnable)})으로 거기서 실행된다. 엔진에는 자기 스레드(실시간 컨텍스트의 렌더링 스레드, 오프라인 컨텍스트를 렌더링하는 스레드, 디코딩 스레드)가 있으며
 * 이들은 스크립트를 부르지 않는다. 스크립트에 알려야 하는 것은 {@link #postTask(Runnable)} 로 가며, 페이지가 폐기된 뒤에는 조용히 버려지므로 늦은 결과가 새 페이지를 건드리지
 * 않는다. 제어 호출은 반대 방향으로 직접 간다: 엔진은 어느 스레드에서나 받는다({@code BaseAudioContext} 참고).
 * <p>
 * <b>한도.</b> {@link WebAudioHostLimits} 에 있다: 페이지의 열린 컨텍스트 수, 오프라인 컨텍스트가 쥘 수 있는 메모리, 대기 중인 디코딩 수(엔진의 디코딩 서비스). 한도를 넘으면
 * <code>NotSupportedError</code> 이다. 규격이 구현이 정하는 컨텍스트 최대 수를 허용한다.
 */
public final class WebAudioApi {

    /**
     * One script-visible operation: the body of a method or of an accessor. {@link WebAudioApi#function} wraps it, so an engine exception that it throws becomes the
     * script exception that the specification names.
     * <p>
     * 스크립트에 보이는 연산 하나: 메서드나 접근자의 본문. {@link WebAudioApi#function} 이 감싸므로 본문이 던지는 엔진 예외는 규격이 정한 스크립트 예외가 된다.
     */
    interface Op {

        /**
         * Runs the operation.
         * <p>
         * 연산을 실행한다.
         *
         * @param self this value / this 값
         * @param args the arguments / 인수
         * @return the result, undefined for none / 결과. 없으면 undefined
         */
        JsValue call(JsValue self, JsValue[] args);
    }

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The script realm of the page.
     * <p>
     * 페이지의 스크립트 realm.
     */
    private final JsRealm realm;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * Guards the lists and the counters below.
     * <p>
     * 아래 목록과 계수기를 지킨다.
     */
    private final Object lock = new Object();

    /**
     * The contexts that are open, in the order of their creation.
     * <p>
     * 열려 있는 컨텍스트(만든 순서).
     */
    private final List<ContextValue> contexts = new ArrayList<ContextValue>();

    /**
     * The number of samples of the memory budget that the offline contexts of the page hold.
     * <p>
     * 페이지의 오프라인 컨텍스트가 쥔 메모리 예산의 표본 수.
     */
    private long offlineSamples;

    /**
     * The number of rendering threads that were named so far (the number in the name of the next one).
     * <p>
     * 지금까지 이름을 붙인 렌더링 스레드 수(다음 스레드 이름의 번호).
     */
    private int renderSerial;

    /**
     * The number of offline rendering threads that were named so far.
     * <p>
     * 지금까지 이름을 붙인 오프라인 렌더링 스레드 수.
     */
    private int offlineSerial;

    /**
     * The installer of the members of the interfaces.
     * <p>
     * 인터페이스 멤버의 설치기.
     */
    private final Members members;

    /**
     * The nodes (<code>AudioNode</code> and its subinterfaces).
     * <p>
     * 노드(<code>AudioNode</code> 와 그 하위 인터페이스).
     */
    private NodeApi nodes;

    /**
     * The contexts.
     * <p>
     * 컨텍스트.
     */
    private ContextApi contextApi;

    /**
     * Creates the installer; {@link #install(PageScripts)} is the way in.
     * <p>
     * 설치기를 만든다. 진입점은 {@link #install(PageScripts)} 이다.
     *
     * @param page the page / 페이지
     */
    private WebAudioApi(PageScripts page) {
        this.page = page;
        this.bridge = page.getBridge();
        this.realm = page.getRealm();
        this.members = new Members(this);
    }

    /**
     * Installs the Web Audio interfaces on the global object of a page. Call it while the page is created, on the thread that creates it, after the DOM interfaces
     * (<code>EventTarget</code>, <code>Event</code>) exist.
     * <p>
     * 페이지의 전역 객체에 Web Audio 인터페이스를 설치한다. 페이지를 만드는 동안 만드는 스레드에서, DOM 인터페이스(<code>EventTarget</code>, <code>Event</code>)가 생긴 뒤에 부른다.
     *
     * @param page the page, not null / 페이지(null 불가)
     * @return the installed API, to be disposed with the page / 설치한 API. 페이지와 함께 폐기해야 한다
     */
    public static WebAudioApi install(PageScripts page) {
        WebAudioApi api = new WebAudioApi(page);
        api.installAll();
        return api;
    }

    /**
     * Installs every interface.
     * <p>
     * 모든 인터페이스를 설치한다.
     */
    private void installAll() {
        JsObject g = realm.getGlobalObject();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Names the interface of the Web Audio objects.
             * <p>
             * Web Audio 객체의 인터페이스 이름을 정한다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름. 모르면 null
             */
            public String classify(Object target) {
                String n = nodes.classify(target);
                if (n == null) {
                    n = contextApi.classify(target);
                }
                if (n != null) {
                    return n;
                } else if (target instanceof AudioBufferValue) {
                    return "AudioBuffer";
                } else if (target instanceof AudioParam) {
                    return "AudioParam";
                } else if (target instanceof AudioListener) {
                    return "AudioListener";
                }
                return target instanceof PeriodicWave ? "PeriodicWave" : null;
            }
        });
        nodes = new NodeApi(this, members);
        contextApi = new ContextApi(this, members, nodes);
        new ValueApi(this, members).install(g);
        nodes.install(g);
        contextApi.install(g);
    }

    /**
     * Returns the page.
     * <p>
     * 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    PageScripts page() {
        return page;
    }

    /**
     * Returns the DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    DomBridge bridge() {
        return bridge;
    }

    /**
     * Returns the script realm of the page.
     * <p>
     * 페이지의 스크립트 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    JsRealm realm() {
        return realm;
    }

    /**
     * Tells whether the page was disposed (the scripts of the page do not run any more; this is also the "document is not fully active" of the specification).
     * <p>
     * 페이지가 폐기되었는지 알려 준다(페이지의 스크립트가 더는 실행되지 않는다. 규격의 "document is not fully active" 이기도 하다).
     *
     * @return true after the disposal / 폐기 뒤에는 true
     */
    boolean isDisposed() {
        return disposed;
    }

    /**
     * Returns the installer of the nodes.
     * <p>
     * 노드의 설치기를 반환한다.
     *
     * @return the nodes / 노드
     */
    NodeApi nodes() {
        return nodes;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 함수와 오류
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a script function for an operation; an engine exception that the operation throws becomes the script exception of the specification.
     * <p>
     * 연산을 위한 스크립트 함수를 만든다. 연산이 던지는 엔진 예외는 규격이 정한 스크립트 예외가 된다.
     *
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param op     the operation / 연산
     * @return the function / 함수
     */
    NativeFunction function(String name, int length, final Op op) {
        return realm.newFunction(name, length, new NativeFunction.Call() {
            /**
             * Runs the operation and translates its engine exceptions.
             * <p>
             * 연산을 실행하고 엔진 예외를 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                try {
                    return op.call(self, args);
                } catch (AudioException e) {
                    throw error(e);
                }
            }
        });
    }

    /**
     * Defines a method (writable, enumerable and configurable, as WebIDL says) on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다(WebIDL 대로 쓰기 가능, 열거 가능, 설정 가능).
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param op     the operation / 연산
     */
    void method(JsObject proto, String name, int length, Op op) {
        proto.defineData(name, function(name, length, op), true, true, true);
    }

    /**
     * Defines an attribute (an accessor property, enumerable and configurable) on a prototype.
     * <p>
     * 프로토타입에 속성(접근자 프로퍼티, 열거 가능, 설정 가능)을 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter / getter
     * @param set   the setter, or null for a read-only attribute / setter. 읽기 전용이면 null
     */
    void accessor(JsObject proto, String name, Op get, Op set) {
        proto.defineAccessor(name, function("get " + name, 0, get), set == null ? null : function("set " + name, 1, set), true, true);
    }

    /**
     * Turns an engine exception into the script exception that the specification names: a <code>RangeError</code> or a <code>TypeError</code>, or a
     * <code>DOMException</code> with the name of the error (<code>IndexSizeError</code>, <code>NotSupportedError</code> ...).
     * <p>
     * 엔진 예외를 규격이 정한 스크립트 예외로 바꾼다: <code>RangeError</code>, <code>TypeError</code>, 또는 오류 이름(<code>IndexSizeError</code>,
     * <code>NotSupportedError</code> 등)을 가진 <code>DOMException</code>.
     *
     * @param e the engine exception / 엔진 예외
     * @return the exception to throw / 던질 예외
     */
    JsException error(AudioException e) {
        AudioException.Kind k = e.getKind();
        if (k == AudioException.Kind.RANGE) {
            return JsException.rangeError(e.getMessage());
        } else if (k == AudioException.Kind.TYPE) {
            return JsException.typeError(e.getMessage());
        }
        return domError(k.getExceptionName(), e.getMessage());
    }

    /**
     * Creates the exception of a <code>DOMException</code> with a name.
     * <p>
     * 이름이 있는 <code>DOMException</code> 의 예외를 만든다.
     *
     * @param name    the name, for example <code>InvalidStateError</code> / 이름. 예: <code>InvalidStateError</code>
     * @param message the message / 메시지
     * @return the exception to throw / 던질 예외
     */
    JsException domError(String name, String message) {
        return new JsException(DomExceptionObjects.create(bridge, name, message));
    }

    /**
     * Returns the value that a script sees for a failure of a promise: the script exception of an engine exception, as an object.
     * <p>
     * 프라미스의 실패에서 스크립트가 보는 값을 반환한다: 엔진 예외의 스크립트 예외를 객체로 준 것.
     *
     * @param e the engine exception / 엔진 예외
     * @return the value / 값
     */
    JsValue errorValue(AudioException e) {
        return error(e).getValue(realm);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 작업 전달
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Hands a task to the thread that owns the script realm: it runs as a task of the page, followed by the microtask checkpoint. Any thread may call it; after the
     * page was disposed the task is dropped, so a result that comes late never runs.
     * <p>
     * 스크립트 realm 을 소유한 스레드에 작업을 넘긴다: 페이지의 작업으로 실행되고 마이크로태스크 검사점이 뒤따른다. 어느 스레드에서나 부를 수 있다. 페이지가 폐기된 뒤에는
     * 작업을 버리므로 늦게 온 결과는 실행되지 않는다.
     *
     * @param task the task / 작업
     */
    void postTask(final Runnable task) {
        if (disposed) {
            return;
        }
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null) {
            return;
        }
        s.post(new Runnable() {
            /**
             * Runs the task as a task of the page unless the page was disposed meanwhile.
             * <p>
             * 그동안 페이지가 폐기되지 않았다면 작업을 페이지의 작업으로 실행한다.
             */
            public void run() {
                if (!disposed) {
                    page.runTask(task);
                }
            }
        }, 0);
    }

    /**
     * Returns the sink that carries what the rendering thread of a context has to tell the script to the page (the <code>ended</code> event of a source, for example).
     * <p>
     * 컨텍스트의 렌더링 스레드가 스크립트에 알려야 하는 것(예: 원천의 <code>ended</code> 이벤트)을 페이지로 나르는 싱크를 반환한다.
     *
     * @return the sink / 싱크
     */
    BaseAudioContext.TaskSink sink() {
        return new BaseAudioContext.TaskSink() {
            /**
             * Posts the task to the page.
             * <p>
             * 작업을 페이지로 보낸다.
             *
             * @param task the task / 작업
             */
            public void post(Runnable task) {
                postTask(task);
            }
        };
    }

    /**
     * Dispatches a plain event of a type at a target (call it inside a task of the page): a <code>statechange</code> or <code>ended</code> event, that does not bubble and
     * cannot be cancelled.
     * <p>
     * 대상에 종류만 있는 이벤트를 전달한다(페이지의 작업 안에서 부른다): 버블링하지 않고 취소할 수 없는 <code>statechange</code>, <code>ended</code> 이벤트.
     *
     * @param target the target / 대상
     * @param type   the event type / 이벤트 종류
     */
    void fire(EventTarget target, String type) {
        fire(target, new Event(type, false, false));
    }

    /**
     * Dispatches an event at a target (call it inside a task of the page); the listeners are the script's, so a listener that throws is reported to the page by
     * the event machinery and does not stop the others.
     * <p>
     * 대상에 이벤트를 전달한다(페이지의 작업 안에서 부른다). 리스너는 스크립트의 것이므로 던지는 리스너는 이벤트 처리 장치가 페이지에 보고하며 다른 리스너를 멈추지 않는다.
     *
     * @param target the target / 대상
     * @param event  the event / 이벤트
     */
    void fire(EventTarget target, Event event) {
        try {
            target.dispatchTrustedEvent(event);
        } catch (DomException e) {
            throw bridge.domError(e);
        }
    }

    /**
     * Creates the script object of an event of this API with the prototype of its interface, so that a listener sees an object of that interface although the DOM
     * bridge names every event class that it does not know <code>Event</code>.
     * <p>
     * 이 API 의 이벤트에 인터페이스의 프로토타입을 가진 스크립트 객체를 만든다. DOM 브리지는 모르는 이벤트 클래스를 모두 <code>Event</code> 로 부르므로, 이렇게 해야 리스너가 그
     * 인터페이스의 객체를 본다.
     *
     * @param event the event / 이벤트
     * @param proto the prototype of the interface / 인터페이스의 프로토타입
     * @return the event / 이벤트
     */
    Event adopt(Event event, JsObject proto) {
        HostObject w = bridge.wrap(event);
        w.setPrototypeOf(proto);
        return event;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 컨텍스트 목록과 한도
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Enters a new context into the list of the open contexts, checking the limits of the page. When a real-time context finds the limit reached, the page first gives up the real-time
     * contexts that nobody can use any more: those that are suspended and whose script objects (and those of their nodes) were collected (see {@link ContextValue#isAbandoned()}); their
     * rendering threads and outputs end. A running context is never given up, as it can make sound (a browser keeps it alive until it is closed, too), so a script that creates a context
     * per sound and never closes it still hits the limit, as in a browser.
     * <p>
     * 새 컨텍스트를 열린 컨텍스트 목록에 넣으며 페이지의 한도를 검사한다. 실시간 컨텍스트가 한도에 닿은 것을 보면 페이지는 먼저 아무도 더는 쓸 수 없는 실시간 컨텍스트를 포기한다: 멈춰 있고
     * 스크립트 객체(와 노드의 스크립트 객체)가 수거된 것이다({@link ContextValue#isAbandoned()} 참고). 그 렌더링 스레드와 출력이 끝난다. 실행 중인 컨텍스트는 소리를 낼 수 있으므로 포기하지
     * 않는다(브라우저도 닫힐 때까지 살려 둔다). 그래서 소리마다 컨텍스트를 만들고 닫지 않는 스크립트는 브라우저에서처럼 여전히 한도에 닿는다.
     *
     * @param c the context / 컨텍스트
     * @throws AudioException <code>NotSupportedError</code> if the page has too many open contexts / 열린 컨텍스트가 너무 많으면 <code>NotSupportedError</code>
     */
    void register(ContextValue c) {
        List<ContextValue> abandoned = null;
        synchronized (lock) {
            int same = countOpen(c.isOffline());
            int max = c.isOffline() ? WebAudioHostLimits.MAX_OFFLINE_CONTEXTS : WebAudioHostLimits.MAX_REALTIME_CONTEXTS;
            // 한도에 닿으면 어떤 스크립트도 더는 닿을 수 없고 멈춰 있는 실시간 컨텍스트를 먼저 놓아 자리를 만든다(실행 중인 컨텍스트는 소리를 낼 수 있으므로 닫을 때까지 붙잡는다).
            if (same >= max && !c.isOffline()) {
                abandoned = new ArrayList<ContextValue>();
                // 목록의 뒤에서부터 포기해도 되는 컨텍스트를 골라 목록에서 뺀다.
                for (int i = contexts.size() - 1; i >= 0; i--) {
                    ContextValue open = contexts.get(i);
                    if (!open.isOffline() && open.isAbandoned()) {
                        contexts.remove(i);
                        abandoned.add(open);
                    }
                }
                same = countOpen(false);
            }
            if (same >= max) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "The number of hardware contexts provided (" + max
                        + ") is greater than or equal to the maximum bound (" + max + ").");
            }
            contexts.add(c);
        }
        // 놓은 컨텍스트의 스레드와 자원은 잠금 밖에서 닫는다.
        if (abandoned != null) {
            for (int i = 0; i < abandoned.size(); i++) {
                abandoned.get(i).closeAbandoned();
            }
        }
    }

    /**
     * Counts the open contexts of a kind (call it with the lock held).
     * <p>
     * 한 종류의 열린 컨텍스트를 센다(잠금을 쥐고 부른다).
     *
     * @param offline true to count offline contexts / 오프라인 컨텍스트를 세려면 true
     * @return the number / 개수
     */
    private int countOpen(boolean offline) {
        int n = 0;
        // 같은 종류의 열린 컨텍스트를 센다.
        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).isOffline() == offline) {
                n++;
            }
        }
        return n;
    }

    /**
     * Removes a context from the list of the open contexts and gives its memory back.
     * <p>
     * 열린 컨텍스트 목록에서 컨텍스트를 빼고 메모리를 돌려준다.
     *
     * @param c the context / 컨텍스트
     */
    void unregister(ContextValue c) {
        synchronized (lock) {
            contexts.remove(c);
            offlineSamples -= c.reservedSamples;
            c.reservedSamples = 0;
        }
    }

    /**
     * Reserves memory of the budget of the page for an offline context (the engine allocates the whole result when the context is created).
     * <p>
     * 오프라인 컨텍스트를 위해 페이지 예산의 메모리를 예약한다(엔진은 컨텍스트를 만들 때 결과 전체를 할당한다).
     *
     * @param samples the number of samples (all channels) / 표본 수(모든 채널)
     * @return true if the budget allows it / 예산이 허용하면 true
     */
    boolean reserveOffline(long samples) {
        synchronized (lock) {
            if (offlineSamples + samples > WebAudioHostLimits.MAX_OFFLINE_SAMPLES) {
                return false;
            }
            offlineSamples += samples;
            return true;
        }
    }

    /**
     * Gives back memory that {@link #reserveOffline(long)} reserved, before the context entered the list.
     * <p>
     * 컨텍스트가 목록에 들어가기 전에 {@link #reserveOffline(long)} 이 예약한 메모리를 돌려준다.
     *
     * @param samples the number of samples / 표본 수
     */
    void releaseOffline(long samples) {
        synchronized (lock) {
            offlineSamples -= samples;
        }
    }

    /**
     * Returns the number in the name of the next rendering thread of a real-time context.
     * <p>
     * 다음 실시간 컨텍스트 렌더링 스레드의 이름에 쓸 번호를 반환한다.
     *
     * @return the number / 번호
     */
    int nextRenderSerial() {
        synchronized (lock) {
            return ++renderSerial;
        }
    }

    /**
     * Returns the number in the name of the next thread that renders an offline context.
     * <p>
     * 다음 오프라인 컨텍스트 렌더링 스레드의 이름에 쓸 번호를 반환한다.
     *
     * @return the number / 번호
     */
    int nextOfflineSerial() {
        synchronized (lock) {
            return ++offlineSerial;
        }
    }

    /**
     * Returns the number of open contexts (for tests).
     * <p>
     * 열린 컨텍스트 수를 반환한다(시험용).
     *
     * @return the number / 개수
     */
    public int getOpenContextCount() {
        synchronized (lock) {
            return contexts.size();
        }
    }

    /**
     * Returns the number of samples of the offline budget that is in use (for tests).
     * <p>
     * 쓰고 있는 오프라인 예산의 표본 수를 반환한다(시험용).
     *
     * @return the number of samples / 표본 수
     */
    public long getOfflineSamplesInUse() {
        synchronized (lock) {
            return offlineSamples;
        }
    }

    /**
     * Disposes everything that the page owns here: every real-time context is closed (its rendering thread ends), every offline rendering is cancelled, every
     * decoding service is closed (the requests that wait are dropped without a callback), and nothing is delivered to the page any more. Any thread may call it, without the
     * lock of the page, and calling it again has no effect. The script objects that the page still holds keep working as objects but no longer produce sound or events.
     * <p>
     * 페이지가 여기서 가진 모든 것을 폐기한다: 모든 실시간 컨텍스트를 닫고(렌더링 스레드가 끝난다), 모든 오프라인 렌더링을 취소하고, 모든 디코딩 서비스를 닫고(기다리는 요청은 콜백 없이
     * 버려진다), 이후 페이지에 아무것도 전달하지 않는다. 페이지의 잠금 없이 어느 스레드에서나 부를 수 있으며 다시 불러도 효과가 없다. 페이지가 아직 쥐고 있는 스크립트 객체는
     * 객체로는 계속 동작하나 더는 소리도 이벤트도 만들지 않는다.
     */
    public void dispose() {
        disposed = true;
        List<ContextValue> copy;
        synchronized (lock) {
            copy = new ArrayList<ContextValue>(contexts);
            contexts.clear();
            offlineSamples = 0;
        }
        // 열린 컨텍스트마다 렌더링을 멈추고 디코딩 서비스를 닫는다.
        for (int i = 0; i < copy.size(); i++) {
            ContextValue c = copy.get(i);
            c.closeRequested = true;
            if (c.realtime() != null) {
                c.realtime().close();
            }
            OfflineRender r = c.renderer;
            if (r != null) {
                r.cancel();
            }
            c.closeDecoder();
        }
    }

    /**
     * Reads an optional numeric argument with a default: the default for an absent or undefined argument, otherwise a finite <code>double</code> (a
     * non-finite value is a <code>TypeError</code>, as WebIDL says).
     * <p>
     * 기본값이 있는 선택적 숫자 인수를 읽는다: 인수가 없거나 undefined 이면 기본값을, 아니면 유한한 double 을 반환한다(유한하지 않은 값은 WebIDL 대로 <code>TypeError</code>).
     *
     * @param args     the arguments / 인수
     * @param index    the position / 위치
     * @param fallback the value for an absent or undefined argument / 없거나 undefined 인 인수의 값
     * @return the number / 수
     */
    static double optionalDouble(JsValue[] args, int index, double fallback) {
        return index < args.length && !args[index].isUndefined() ? Idl.dbl(args[index]) : fallback;
    }

}
