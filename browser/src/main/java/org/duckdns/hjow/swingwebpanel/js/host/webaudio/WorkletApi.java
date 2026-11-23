package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.ErrorEvent;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredCloneException;
import org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet.WorkletHost;
import org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet.WorkletScope;
import org.duckdns.hjow.swingwebpanel.js.worker.ChannelPorts;
import org.duckdns.hjow.swingwebpanel.js.worker.PortTarget;
import org.duckdns.hjow.swingwebpanel.js.worker.ScriptContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioParam;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ChannelCountMode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ChannelInterpretation;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WorkletProcessorException;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The page side of the audio worklet (plan item P25-06; Web Audio API 1.0, 1.32): <code>BaseAudioContext.audioWorklet</code> and the interfaces <code>Worklet</code> and
 * <code>AudioWorklet</code> with <code>addModule()</code>, <code>AudioWorkletNode</code> with <code>parameters</code> (<code>AudioParamMap</code>), <code>port</code> and
 * <code>onprocessorerror</code>. The audio worklet global scope with the processors lives in {@link WorkletScope} (see there for the threads); this class is the part that runs on the
 * thread that owns the script realm of the page and talks to the scope only through plain data and task queues.
 * <ul>
 * <li><strong><code>audioWorklet.addModule(url, options)</code>.</strong> The address is resolved against the base address of the document (a failure is a rejected promise with
 * <code>SyntaxError</code>); <code>options.credentials</code> is <code>omit</code>, <code>same-origin</code> (the default) or <code>include</code>. The scope fetches the module and
 * everything it imports through the request layer of the component (user agent, headers, cookies, scheme limits, CORS and the MIME type of modules), evaluates it in the worklet
 * realm, and the promise is resolved after the names that the module registered are known to the page. It is rejected with an <code>AbortError</code> if a module cannot be fetched
 * (network, scheme, origin policy, MIME type) and with the error itself (a <code>SyntaxError</code>, an error object and no <code>DOMException</code>) if a module has a syntax error or cannot
 * be linked; an exception that a module throws while it runs, an error of <code>registerProcessor()</code> included, does not reject: it is reported to the console and the promise resolves
 * (HTML Standard "Worklets"; this is what Chromium does). A module that was added before is not evaluated again. The context of a closed <code>AudioContext</code> rejects with <code>InvalidStateError</code>.</li>
 * <li><strong><code>new AudioWorkletNode(context, name, options)</code>.</strong> The options dictionary is converted as WebIDL says; the name must be registered
 * (<code>InvalidStateError</code> otherwise); the node is configured by <code>numberOfInputs</code>, <code>numberOfOutputs</code> and <code>outputChannelCount</code> (see
 * {@link AudioWorkletNode}); the <code>parameters</code> map is made from the descriptors of the processor and <code>parameterData</code> sets the initial values; the options are
 * serialized with the structured clone algorithm (<code>DataCloneError</code> if they cannot be) for the constructor of the processor; a message channel is made whose one end
 * is <code>node.port</code> and whose other end belongs to the processor; the construction of the processor is queued as a control message of the node. A processor that throws
 * makes the node fire <code>processorerror</code> (an <code>ErrorEvent</code> with the message, the file, the line and the column) and the node is silent from then on.</li>
 * </ul>
 * <p>
 * 오디오 worklet 의 페이지 쪽(계획 P25-06. Web Audio API 1.0, 1.32): <code>BaseAudioContext.audioWorklet</code> 과 인터페이스 <code>Worklet</code>, <code>AudioWorklet</code>(<code>addModule()</code>),
 * <code>AudioWorkletNode</code>(<code>parameters</code>(<code>AudioParamMap</code>), <code>port</code>, <code>onprocessorerror</code>). 프로세서가 있는 오디오 worklet 전역 범위는 {@link WorkletScope} 에 있다(스레드는
 * 거기를 참고). 이 클래스는 페이지의 스크립트 realm 을 소유한 스레드에서 실행되며 스코프와는 일반 데이터와 작업 큐로만 이야기하는 부분이다.
 * <ul>
 * <li><strong><code>audioWorklet.addModule(url, options)</code>.</strong> 주소는 문서의 기준 주소를 기준으로 해석한다(실패는 <code>SyntaxError</code> 로 거부된 프라미스). <code>options.credentials</code> 는
 * <code>omit</code>, <code>same-origin</code>(기본), <code>include</code> 이다. 스코프가 컴포넌트의 요청 계층(User-Agent, 헤더, 쿠키, 스킴 제한, CORS 와 모듈의 MIME 타입)으로 모듈과 그것이 import 하는
 * 모든 것을 가져와 worklet realm 에서 평가하며, 모듈이 등록한 이름을 페이지가 알게 된 뒤에 프라미스가 resolve 된다. 모듈을 가져올 수 없으면(네트워크, 스킴, 오리진 정책, MIME 타입) <code>AbortError</code> 로, 모듈에 구문 오류가 있거나 링크할 수 없으면 그 오류 자체(<code>DOMException</code> 이 아닌 <code>SyntaxError</code> 오류 객체)로 거부된다. 모듈이 실행 중 던지는 예외(<code>registerProcessor()</code> 의 오류 포함)는 거부하지 않고 콘솔에 보고한 채 프라미스가 resolve 된다(HTML Standard "Worklets", Chromium 의 동작). 전에 추가한 모듈은 다시
 * 평가하지 않는다. 닫힌 <code>AudioContext</code> 는 <code>InvalidStateError</code> 로 거부한다.</li>
 * <li><strong><code>new AudioWorkletNode(context, name, options)</code>.</strong> 옵션 사전은 WebIDL 대로 변환한다. 이름은 등록되어 있어야 한다(아니면 <code>InvalidStateError</code>). 노드는
 * <code>numberOfInputs</code>, <code>numberOfOutputs</code>, <code>outputChannelCount</code>(={@link AudioWorkletNode} 참고)로 구성된다. <code>parameters</code> 맵은 프로세서의 설명으로 만들고
 * <code>parameterData</code> 가 초기 값을 정한다. 옵션은 프로세서의 생성자를 위해 구조화 복제 알고리즘으로 직렬화한다(할 수 없으면 <code>DataCloneError</code>). 한쪽 끝이 <code>node.port</code> 이고 다른 쪽 끝이
 * 프로세서에 속하는 메시지 채널을 만든다. 프로세서의 생성은 노드의 제어 메시지로 줄 선다. 예외를 던지는 프로세서는 노드가 <code>processorerror</code>(메시지, 파일, 줄, 열이 있는 <code>ErrorEvent</code>)를
 * 발생시키게 하며, 노드는 그 뒤 무음이다.</li>
 * </ul>
 */
final class WorkletApi implements WorkletHost {

    /**
     * The script side of the <code>AudioWorklet</code> of a context.
     * <p>
     * 컨텍스트의 <code>AudioWorklet</code> 의 스크립트 쪽.
     */
    static final class WorkletObject {

        /**
         * The context that owns the worklet.
         * <p>
         * worklet 을 소유한 컨텍스트.
         */
        final ContextValue context;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param context the context / 컨텍스트
         */
        WorkletObject(ContextValue context) {
            this.context = context;
        }
    }

    /**
     * The script side of an <code>AudioParamMap</code>: a read-only map from the name to the parameter, in the order of the descriptors.
     * <p>
     * <code>AudioParamMap</code> 의 스크립트 쪽: 이름에서 파라미터로의 읽기 전용 맵(설명의 순서).
     */
    static final class ParamMapObject {

        /**
         * The parameters by name.
         * <p>
         * 이름별 파라미터.
         */
        final Map<String, AudioParam> params;

        /**
         * Creates the map.
         * <p>
         * 맵을 만든다.
         *
         * @param params the parameters, in order / 파라미터(순서대로)
         */
        ParamMapObject(Map<String, AudioParam> params) {
            this.params = params;
        }
    }

    /**
     * What a context has of the audio worklet: its script object and, once a module was added, the scope.
     * <p>
     * 컨텍스트가 가진 오디오 worklet: 스크립트 객체와, 모듈을 추가한 뒤에는 스코프.
     */
    private static final class State {

        /**
         * The <code>audioWorklet</code> object (always the same one).
         * <p>
         * <code>audioWorklet</code> 객체(언제나 같은 것).
         */
        final WorkletObject object;

        /**
         * The scope, null before the first <code>addModule()</code>.
         * <p>
         * 스코프. 첫 <code>addModule()</code> 전에는 null.
         */
        WorkletScope scope;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param object the object / 객체
         */
        State(WorkletObject object) {
            this.object = object;
        }
    }

    /**
     * What an <code>AudioWorkletNode</code> shows to the script beyond the engine node: its port and its parameter map.
     * <p>
     * <code>AudioWorkletNode</code> 가 엔진 노드 외에 스크립트에 보여 주는 것: 포트와 파라미터 맵.
     */
    private static final class NodeState {

        /**
         * The end of the channel that belongs to the page.
         * <p>
         * 페이지에 속한 채널의 끝.
         */
        final PortTarget port;

        /**
         * The parameter map.
         * <p>
         * 파라미터 맵.
         */
        final ParamMapObject parameters;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param port       the port / 포트
         * @param parameters the parameter map / 파라미터 맵
         */
        NodeState(PortTarget port, ParamMapObject parameters) {
            this.port = port;
            this.parameters = parameters;
        }
    }

    /**
     * The script context of the page for the message channels of the nodes: tasks go to the page as tasks of the page.
     * <p>
     * 노드의 메시지 채널을 위한 페이지의 스크립트 문맥: 작업은 페이지의 작업으로 페이지에 간다.
     */
    private final class PageSide implements ScriptContext {

        /**
         * Returns the realm of the page.
         * <p>
         * 페이지의 realm 을 반환한다.
         *
         * @return the realm / realm
         */
        public JsRealm getRealm() {
            return realm;
        }

        /**
         * Returns the DOM bridge of the page.
         * <p>
         * 페이지의 DOM 브리지를 반환한다.
         *
         * @return the bridge / 브리지
         */
        public DomBridge getBridge() {
            return bridge;
        }

        /**
         * Posts a task to the page.
         * <p>
         * 페이지에 작업을 올린다.
         *
         * @param task the work / 작업
         */
        public void post(Runnable task) {
            api.postTask(task);
        }

        /**
         * Tells whether the page lives and runs scripts.
         * <p>
         * 페이지가 살아 있고 스크립트를 실행하는지 알려 준다.
         *
         * @return true while it does / 그렇다면 true
         */
        public boolean isActive() {
            return isPageActive();
        }

        /**
         * Returns the origin of the document of the page.
         * <p>
         * 페이지 문서의 오리진을 반환한다.
         *
         * @return the origin text, or null for an opaque origin / 오리진 문자열. 불투명 오리진이면 null
         */
        public String getOrigin() {
            String url = api.page().getDocument().getUrl();
            Origin o = Origin.of(url == null ? "about:blank" : url);
            return o.isOpaque() ? null : o.serialize();
        }

        /**
         * A page prints the failures of listeners that are not script errors.
         * <p>
         * 페이지는 스크립트 오류가 아닌 리스너 실패를 출력한다.
         *
         * @return the printing handler / 출력 핸들러
         */
        public ListenerErrorHandler getListenerErrors() {
            return EventTarget.PRINTING_HANDLER;
        }
    }

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The installer of the nodes.
     * <p>
     * 노드의 설치기.
     */
    private final NodeApi nodes;

    /**
     * The DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm of the page.
     * <p>
     * 페이지의 realm.
     */
    private final JsRealm realm;

    /**
     * The audio worklets by context; guarded by itself.
     * <p>
     * 컨텍스트별 오디오 worklet. 자기 자신으로 보호한다.
     */
    private final Map<ContextValue, State> states = new HashMap<ContextValue, State>();

    /**
     * What the <code>AudioWorkletNode</code> objects show beyond the engine node; the page thread only.
     * <p>
     * <code>AudioWorkletNode</code> 객체가 엔진 노드 외에 보여 주는 것. 페이지 스레드만 쓴다.
     */
    private final Map<NodeValue, NodeState> nodeStates = new WeakHashMap<NodeValue, NodeState>();

    /**
     * The script context of the page side of the message channels.
     * <p>
     * 메시지 채널의 페이지 쪽 스크립트 문맥.
     */
    private final ScriptContext pageSide = new PageSide();

    /**
     * The number of scopes that were created (the number in the name of the next worklet thread); guarded by {@link #states}.
     * <p>
     * 만든 스코프의 수(다음 worklet 스레드 이름의 번호). {@link #states} 로 보호한다.
     */
    private int serial;

    /**
     * True after {@link #dispose()}; guarded by {@link #states}.
     * <p>
     * {@link #dispose()} 뒤에는 true. {@link #states} 로 보호한다.
     */
    private boolean disposed;

    /**
     * The prototype of <code>AudioWorkletNode</code>.
     * <p>
     * <code>AudioWorkletNode</code> 의 프로토타입.
     */
    private JsObject nodeProto;

    /**
     * Creates the installer; {@link #install(JsObject)} is the way in.
     * <p>
     * 설치기를 만든다. 진입점은 {@link #install(JsObject)} 이다.
     *
     * @param api   the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param nodes the installer of the nodes / 노드의 설치기
     */
    WorkletApi(WebAudioApi api, NodeApi nodes) {
        this.api = api;
        this.nodes = nodes;
        this.bridge = api.bridge();
        this.realm = api.realm();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // WorkletHost
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the page.
     * <p>
     * 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    public PageScripts getPage() {
        return api.page();
    }

    /**
     * Posts a task to the page.
     * <p>
     * 페이지에 작업을 올린다.
     *
     * @param task the work / 작업
     */
    public void postToPage(Runnable task) {
        api.postTask(task);
    }

    /**
     * Tells whether the page lives and runs scripts.
     * <p>
     * 페이지가 살아 있고 스크립트를 실행하는지 알려 준다.
     *
     * @return true while it does / 그렇다면 true
     */
    public boolean isPageActive() {
        return !api.isDisposed() && api.page().getEnvironment().isScriptingEnabled();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 수명
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Ends the audio worklet of a context that was closed (or whose offline rendering ended): its scope is disposed, so the processors are silent and the worklet thread ends.
     * <p>
     * 닫힌(또는 오프라인 렌더링이 끝난) 컨텍스트의 오디오 worklet 을 끝낸다: 스코프를 폐기하므로 프로세서는 무음이고 worklet 스레드가 끝난다.
     *
     * @param c the context / 컨텍스트
     */
    void contextClosed(ContextValue c) {
        State s;
        synchronized (states) {
            s = states.remove(c);
        }
        if (s != null && s.scope != null) {
            s.scope.dispose();
        }
    }

    /**
     * Disposes every scope of the page: any thread, without the lock of the page, and calling it again has no effect.
     * <p>
     * 페이지의 모든 스코프를 폐기한다: 어느 스레드에서나, 페이지의 잠금 없이, 다시 불러도 효과가 없다.
     */
    void dispose() {
        List<State> all;
        synchronized (states) {
            disposed = true;
            all = new ArrayList<State>(states.values());
            states.clear();
        }
        // 스코프마다 폐기한다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).scope != null) {
                all.get(i).scope.dispose();
            }
        }
    }

    /**
     * Returns the scope of a context, if one exists (for tests).
     * <p>
     * 컨텍스트의 스코프가 있으면 반환한다(시험용).
     *
     * @param c the context / 컨텍스트
     * @return the scope or null / 스코프 또는 null
     */
    WorkletScope scopeOf(ContextValue c) {
        synchronized (states) {
            State s = states.get(c);
            return s == null ? null : s.scope;
        }
    }

    /**
     * Returns the state of a context, created at the first request; null after the page was disposed.
     * <p>
     * 컨텍스트의 상태를 반환한다. 첫 요청에 만든다. 페이지가 폐기된 뒤에는 null.
     *
     * @param c the context / 컨텍스트
     * @return the state or null / 상태 또는 null
     */
    private State stateOf(ContextValue c) {
        synchronized (states) {
            if (disposed) {
                return null;
            }
            State s = states.get(c);
            if (s == null) {
                s = new State(new WorkletObject(c));
                states.put(c, s);
            }
            return s;
        }
    }

    /**
     * Returns the scope of a state, creating it at the first request; null after the page was disposed.
     * <p>
     * 상태의 스코프를 반환한다. 첫 요청에 만든다. 페이지가 폐기된 뒤에는 null.
     *
     * @param c the context / 컨텍스트
     * @return the scope or null / 스코프 또는 null
     */
    private WorkletScope scopeFor(ContextValue c) {
        synchronized (states) {
            State s = stateOf(c);
            if (s == null) {
                return null;
            }
            if (s.scope == null) {
                s.scope = new WorkletScope(this, c.engine(), ++serial);
            }
            return s.scope;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 설치
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Installs the interfaces and <code>BaseAudioContext.audioWorklet</code>; call it after the nodes and the contexts were installed.
     * <p>
     * 인터페이스와 <code>BaseAudioContext.audioWorklet</code> 을 설치한다. 노드와 컨텍스트를 설치한 뒤 부른다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Names the interface of the worklet objects.
             * <p>
             * worklet 객체의 인터페이스 이름을 정한다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름. 모르면 null
             */
            public String classify(Object target) {
                if (target instanceof WorkletObject) {
                    return "AudioWorklet";
                } else if (target instanceof ParamMapObject) {
                    return "AudioParamMap";
                } else if (target instanceof NodeValue && ((NodeValue) target).node() instanceof AudioWorkletNode) {
                    return "AudioWorkletNode";
                }
                return null;
            }
        });
        installWorklet();
        installParamMap();
        installNode();
        api.accessor(bridge.getInterface("BaseAudioContext").getPrototype(), "audioWorklet", new WebAudioApi.Op() {
            /**
             * Returns the audio worklet of the context (always the same object).
             * <p>
             * 컨텍스트의 오디오 worklet 을 반환한다(언제나 같은 객체).
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the worklet / worklet
             */
            public JsValue call(JsValue self, JsValue[] args) {
                State s = stateOf(ContextApi.contextOf(self));
                if (s == null) {
                    throw api.domError("InvalidStateError", "The document is not fully active.");
                }
                return bridge.toJs(s.object);
            }
        }, null);
    }

    /**
     * Installs <code>Worklet</code> and <code>AudioWorklet</code>.
     * <p>
     * <code>Worklet</code> 과 <code>AudioWorklet</code> 을 설치한다.
     */
    private void installWorklet() {
        DomBridge.InterfaceInfo worklet = bridge.defineInterface("Worklet", null, WorkletObject.class, new String[0]);
        bridge.defineInterface("AudioWorklet", "Worklet", WorkletObject.class, new String[0]);
        api.method(worklet.getPrototype(), "addModule", 1, new WebAudioApi.Op() {
            /**
             * Loads a module into the global scope of the worklet and returns a promise for the end of the loading.
             * <p>
             * 모듈을 worklet 의 전역 범위에 불러오고 로딩의 끝을 알리는 프라미스를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments: the address of the module and the options / 인수: 모듈의 주소와 옵션
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return addModule(self, args);
            }
        });
    }

    /**
     * The steps of <code>addModule(moduleURL, options)</code>: every failure that happens before the work is given to the scope is a rejected promise, as WebIDL says for an
     * operation that returns a promise.
     * <p>
     * <code>addModule(moduleURL, options)</code> 의 단계: 일을 스코프에 맡기기 전에 생기는 모든 실패는 거부된 프라미스이다. 프라미스를 반환하는 연산에 대한 WebIDL 규칙이다.
     *
     * @param self this value / this 값
     * @param args the arguments / 인수
     * @return the promise / 프라미스
     */
    private JsValue addModule(JsValue self, JsValue[] args) {
        try {
            WorkletObject w = (WorkletObject) DomBridge.targetOf(self, WorkletObject.class);
            if (w == null) {
                throw JsException.typeError("Illegal invocation");
            }
            Idl.requireArgs(args, 1, "addModule", "Worklet");
            String raw = Ops.toString(args[0]);
            boolean credentials = credentials(Idl.dictionary(Js.arg(args, 1), "Failed to execute 'addModule' on 'Worklet': The provided value"));
            if (api.isDisposed()) {
                throw api.domError("InvalidStateError", "The document is not fully active.");
            }
            String key;
            // 주소를 문서의 기준 주소로 해석한다. 해석할 수 없으면 SyntaxError 이다.
            try {
                key = UrlUtil.stripFragment(UrlUtil.resolve(api.page().getDocument().getBaseUri(), raw));
            } catch (RuntimeException e) {
                throw api.domError("SyntaxError", "The module URL '" + raw + "' is invalid.");
            }
            if (w.context.closeRequested || w.context.getState().equals("closed")) {
                throw api.domError("InvalidStateError", "The audio context is closed.");
            }
            WorkletScope scope = scopeFor(w.context);
            if (scope == null) {
                throw api.domError("InvalidStateError", "The document is not fully active.");
            }
            final Promises.Deferred d = Promises.create(realm);
            scope.addModule(key, credentials, new WorkletScope.Completion() {
                /**
                 * Resolves or rejects the promise, as a task of the page.
                 * <p>
                 * 페이지의 작업으로 프라미스를 resolve 하거나 reject 한다.
                 *
                 * @param errorName the name of the error or null / 오류 이름 또는 null
                 * @param message   the message or null / 메시지 또는 null
                 */
                public void finished(String errorName, String message) {
                    if (errorName == null) {
                        d.resolve(Js.UNDEFINED);
                    } else if ("SyntaxError".equals(errorName)) {
                        // 구문 오류와 링크 오류는 error to rethrow 이므로 DOMException 이 아닌 그 오류 객체로 reject 한다.
                        d.reject(JsException.syntaxError(message).getValue(realm));
                    } else if ("TypeError".equals(errorName)) {
                        d.reject(JsException.typeError(message).getValue(realm));
                    } else {
                        d.reject(api.domError(errorName, message).getValue(realm));
                    }
                }
            });
            return d.promise;
        } catch (JsException e) {
            return Promises.rejected(realm, e.getValue(realm));
        }
    }

    /**
     * Reads <code>options.credentials</code> of <code>addModule()</code>: <code>include</code> sends cookies to every origin; <code>omit</code> and <code>same-origin</code> only to the
     * origin of the page (the request layer has no mode that sends none, so <code>omit</code> is the same as <code>same-origin</code>).
     * <p>
     * <code>addModule()</code> 의 <code>options.credentials</code> 를 읽는다: <code>include</code> 는 모든 오리진에 쿠키를 보내고, <code>omit</code> 과 <code>same-origin</code> 은 페이지의 오리진에만 보낸다
     * (요청 계층에는 쿠키를 전혀 보내지 않는 모드가 없으므로 <code>omit</code> 은 <code>same-origin</code> 과 같다).
     *
     * @param options the options, or null / 옵션. 없으면 null
     * @return true to send cookies to every origin / 모든 오리진에 쿠키를 보내려면 true
     */
    private static boolean credentials(JsObject options) {
        JsValue v = Idl.member(options, "credentials");
        if (v.isUndefined()) {
            return false;
        }
        String s = Ops.toString(v);
        if (!"omit".equals(s) && !"same-origin".equals(s) && !"include".equals(s)) {
            throw JsException.typeError("The provided value '" + s + "' is not a valid enum value of type RequestCredentials.");
        }
        return "include".equals(s);
    }

    /**
     * Installs <code>AudioParamMap</code>: a read-only maplike (<code>size</code>, <code>get</code>, <code>has</code>, <code>keys</code>, <code>values</code>, <code>entries</code>,
     * <code>forEach</code> and the iterator), in the order of the descriptors.
     * <p>
     * <code>AudioParamMap</code> 을 설치한다: 읽기 전용 맵 형태(<code>size</code>, <code>get</code>, <code>has</code>, <code>keys</code>, <code>values</code>, <code>entries</code>, <code>forEach</code>, 반복자).
     * 순서는 설명의 순서이다.
     */
    private void installParamMap() {
        JsObject proto = bridge.defineInterface("AudioParamMap", null, ParamMapObject.class, new String[0]).getPrototype();
        api.accessor(proto, "size", new WebAudioApi.Op() {
            /**
             * Returns the number of parameters.
             * <p>
             * 파라미터의 수를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number / 수
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(mapOf(self).params.size());
            }
        }, null);
        api.method(proto, "get", 1, new WebAudioApi.Op() {
            /**
             * Returns the parameter with a name, or undefined.
             * <p>
             * 이름이 있는 파라미터를 반환한다. 없으면 undefined.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the parameter / 파라미터
             */
            public JsValue call(JsValue self, JsValue[] args) {
                Idl.requireArgs(args, 1, "get", "AudioParamMap");
                AudioParam p = mapOf(self).params.get(Ops.toString(args[0]));
                return p == null ? Js.UNDEFINED : bridge.toJs(p);
            }
        });
        api.method(proto, "has", 1, new WebAudioApi.Op() {
            /**
             * Tells whether a parameter with the name exists.
             * <p>
             * 이름이 있는 파라미터가 있는지 알려 준다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return a boolean / 불리언
             */
            public JsValue call(JsValue self, JsValue[] args) {
                Idl.requireArgs(args, 1, "has", "AudioParamMap");
                return Js.bool(mapOf(self).params.containsKey(Ops.toString(args[0])));
            }
        });
        api.method(proto, "keys", 0, iteratorOp(0));
        api.method(proto, "values", 0, iteratorOp(1));
        WebAudioApi.Op entries = iteratorOp(2);
        api.method(proto, "entries", 0, entries);
        proto.defineData(WellKnownSymbols.ITERATOR, proto.get("entries"), true, false, true);
        api.method(proto, "forEach", 1, new WebAudioApi.Op() {
            /**
             * Calls a function for every entry: <code>callback.call(thisArg, value, key, map)</code>.
             * <p>
             * 모든 항목에 대해 함수를 호출한다: <code>callback.call(thisArg, value, key, map)</code>.
             *
             * @param self this value / this 값
             * @param args arguments: the callback and the receiver / 인수: 콜백과 수신자
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ParamMapObject m = mapOf(self);
                Idl.requireArgs(args, 1, "forEach", "AudioParamMap");
                if (!(args[0] instanceof JsObject) || !((JsObject) args[0]).isCallable()) {
                    throw JsException.typeError("Failed to execute 'forEach' on 'AudioParamMap': The callback provided as parameter 1 is not a function.");
                }
                JsValue thisArg = Js.arg(args, 1);
                // 항목마다 (값, 키, 맵) 으로 콜백을 호출한다. 반복 중 변경을 막으려고 항목을 복사해 둔다.
                List<Map.Entry<String, AudioParam>> items = new ArrayList<Map.Entry<String, AudioParam>>(m.params.entrySet());
                for (int i = 0; i < items.size(); i++) {
                    Ops.call(args[0], thisArg, new JsValue[] {bridge.toJs(items.get(i).getValue()), Js.str(items.get(i).getKey()), self});
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Creates the operation that returns an iterator over the keys (0), the values (1) or the entries (2) of the map: an array iterator over a snapshot.
     * <p>
     * 맵의 키(0), 값(1), 항목(2)에 대한 반복자를 반환하는 연산을 만든다: 스냅샷에 대한 배열 반복자.
     *
     * @param what 0 for the keys, 1 for the values, 2 for the entries / 키이면 0, 값이면 1, 항목이면 2
     * @return the operation / 연산
     */
    private WebAudioApi.Op iteratorOp(final int what) {
        return new WebAudioApi.Op() {
            /**
             * Returns the iterator.
             * <p>
             * 반복자를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the iterator / 반복자
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ParamMapObject m = mapOf(self);
                List<JsValue> items = new ArrayList<JsValue>();
                // 항목마다 키, 값, 또는 [키, 값] 쌍을 모은다.
                for (Map.Entry<String, AudioParam> e : m.params.entrySet()) {
                    JsValue key = Js.str(e.getKey());
                    JsValue value = bridge.toJs(e.getValue());
                    items.add(what == 0 ? key : what == 1 ? value : realm.newArray(new JsValue[] {key, value}));
                }
                JsObject array = realm.newArray(items);
                return Ops.invoke(realm, array, WellKnownSymbols.ITERATOR, new JsValue[0]);
            }
        };
    }

    /**
     * Returns the map of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 맵을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the map / 맵
     */
    private static ParamMapObject mapOf(JsValue self) {
        ParamMapObject m = (ParamMapObject) DomBridge.targetOf(self, ParamMapObject.class);
        if (m == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return m;
    }

    /**
     * Installs <code>AudioWorkletNode</code>: the constructor, <code>parameters</code>, <code>port</code> and <code>onprocessorerror</code>.
     * <p>
     * <code>AudioWorkletNode</code> 를 설치한다: 생성자, <code>parameters</code>, <code>port</code>, <code>onprocessorerror</code>.
     */
    private void installNode() {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("AudioWorkletNode", "AudioNode", NodeValue.class, new String[0]);
        nodeProto = info.getPrototype();
        bridge.getEventHandlers().define(nodeProto, NodeValue.class, new String[] {"processorerror"});
        api.accessor(nodeProto, "parameters", new WebAudioApi.Op() {
            /**
             * Returns the parameter map of the node.
             * <p>
             * 노드의 파라미터 맵을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the map / 맵
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(stateOfNode(self).parameters);
            }
        }, null);
        api.accessor(nodeProto, "port", new WebAudioApi.Op() {
            /**
             * Returns the port of the node.
             * <p>
             * 노드의 포트를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the port / 포트
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(stateOfNode(self).port);
            }
        }, null);
        bridge.setConstructor("AudioWorkletNode", new NativeFunction(realm, bridge.getInterface("AudioNode").getConstructor(), "AudioWorkletNode", 2,
                new NativeFunction.Call() {
            /**
             * Calling the constructor without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 생성자를 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'AudioWorkletNode': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates the node.
             * <p>
             * 노드를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the context, the name and the options / 인수: 컨텍스트, 이름, 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 2, "AudioWorkletNode");
                ContextValue c = ContextApi.contextOf(args[0], "AudioWorkletNode");
                String name = Ops.toString(args[1]);
                JsObject dict = Idl.dictionary(Js.arg(args, 2), "Failed to construct 'AudioWorkletNode': The provided value");
                NodeValue v;
                try {
                    v = createNode(c, name, dict);
                } catch (AudioException e) {
                    throw api.error(e);
                }
                HostObject w = bridge.wrap(v);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Returns what a node shows beyond the engine node, or throws the standard TypeError for a receiver that is no <code>AudioWorkletNode</code>.
     * <p>
     * 노드가 엔진 노드 외에 보여 주는 것을 반환한다. <code>AudioWorkletNode</code> 가 아닌 수신자이면 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the state / 상태
     */
    private NodeState stateOfNode(JsValue self) {
        NodeValue v = (NodeValue) DomBridge.targetOf(self, NodeValue.class);
        NodeState s = v == null ? null : nodeStates.get(v);
        if (s == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return s;
    }

    /**
     * The steps of the constructor (1.32.3.1): converts the options, checks the name, creates the engine node, the parameter map, the message channel and the control message that
     * constructs the processor.
     * <p>
     * 생성자의 단계(1.32.3.1): 옵션을 변환하고, 이름을 검사하고, 엔진 노드, 파라미터 맵, 메시지 채널, 프로세서를 만드는 제어 메시지를 만든다.
     *
     * @param c    the context / 컨텍스트
     * @param name the name of the processor / 프로세서의 이름
     * @param dict the options dictionary, or null / 옵션 사전. 없으면 null
     * @return the node / 노드
     */
    private NodeValue createNode(ContextValue c, String name, JsObject dict) {
        // WebIDL 사전 변환: 멤버를 읽고 형식을 검사한다(AudioNodeOptions 가 먼저, 그 다음 알파벳 순).
        JsValue channelCount = Idl.member(dict, "channelCount");
        JsValue channelCountMode = Idl.member(dict, "channelCountMode");
        JsValue channelInterpretation = Idl.member(dict, "channelInterpretation");
        JsValue numberOfInputsValue = Idl.member(dict, "numberOfInputs");
        JsValue numberOfOutputsValue = Idl.member(dict, "numberOfOutputs");
        int numberOfInputs = numberOfInputsValue.isUndefined() ? 1 : Idl.ulong(numberOfInputsValue);
        int numberOfOutputs = numberOfOutputsValue.isUndefined() ? 1 : Idl.ulong(numberOfOutputsValue);
        JsValue outputChannelCountValue = Idl.member(dict, "outputChannelCount");
        int[] outputChannelCount = outputChannelCountValue.isUndefined() ? null : counts(outputChannelCountValue);
        Map<String, Float> parameterData = parameterData(Idl.member(dict, "parameterData"));
        JsValue processorOptions = Idl.member(dict, "processorOptions");
        if (!processorOptions.isUndefined() && !(processorOptions instanceof JsObject)) {
            throw JsException.typeError("Failed to construct 'AudioWorkletNode': The provided value for 'processorOptions' is not an object.");
        }
        WorkletScope scope;
        synchronized (states) {
            State s = states.get(c);
            scope = s == null ? null : s.scope;
        }
        List<AudioWorkletNode.ParamDescriptor> descriptors = scope == null ? null : scope.getDescriptors(name);
        if (descriptors == null) {
            throw api.domError("InvalidStateError", "AudioWorkletNode cannot be created: The node name '" + name + "' is not defined in AudioWorkletGlobalScope.");
        }
        AudioWorkletNode engine = new AudioWorkletNode(c.engine(), numberOfInputs, numberOfOutputs, outputChannelCount, descriptors);
        // 옵션이 정한 채널 속성을 노드에 적용한다.
        if (!channelCount.isUndefined()) {
            engine.setChannelCount(Idl.ulong(channelCount));
        }
        if (!channelCountMode.isUndefined()) {
            engine.setChannelCountMode((ChannelCountMode) Idl.enumStrict(ChannelCountMode.class, channelCountMode, "ChannelCountMode"));
        }
        if (!channelInterpretation.isUndefined()) {
            engine.setChannelInterpretation((ChannelInterpretation) Idl.enumStrict(ChannelInterpretation.class, channelInterpretation, "ChannelInterpretation"));
        }
        // parameterData 의 값을 해당 이름의 파라미터의 초기 값으로 정한다.
        for (Map.Entry<String, Float> e : parameterData.entrySet()) {
            AudioParam p = engine.getParameter(e.getKey());
            if (p != null) {
                p.setValue(e.getValue().floatValue());
            }
        }
        // 프로세서의 생성자가 받을 옵션 객체를 만들어 직렬화한다.
        JsObject optionsObject = realm.newObject();
        optionsObject.createDataProperty("numberOfInputs", Js.num(numberOfInputs));
        optionsObject.createDataProperty("numberOfOutputs", Js.num(numberOfOutputs));
        if (!outputChannelCountValue.isUndefined()) {
            optionsObject.createDataProperty("outputChannelCount", outputChannelCountValue);
        }
        JsValue parameterDataValue = Idl.member(dict, "parameterData");
        if (!parameterDataValue.isUndefined()) {
            optionsObject.createDataProperty("parameterData", parameterDataValue);
        }
        if (!processorOptions.isUndefined()) {
            optionsObject.createDataProperty("processorOptions", processorOptions);
        }
        if (!channelCount.isUndefined()) {
            optionsObject.createDataProperty("channelCount", channelCount);
        }
        if (!channelCountMode.isUndefined()) {
            optionsObject.createDataProperty("channelCountMode", channelCountMode);
        }
        if (!channelInterpretation.isUndefined()) {
            optionsObject.createDataProperty("channelInterpretation", channelInterpretation);
        }
        ClonedValue serialized;
        try {
            serialized = StructuredClone.serialize(realm, optionsObject);
        } catch (StructuredCloneException e) {
            throw api.domError("DataCloneError", e.getMessage());
        }
        PortTarget[] ends = ChannelPorts.newChannel(pageSide, scope.getScriptContext());
        Map<String, AudioParam> map = new LinkedHashMap<String, AudioParam>();
        // 설명의 순서대로 파라미터 맵을 채운다.
        for (int i = 0; i < engine.getParameters().size(); i++) {
            AudioParam p = engine.getParameters().get(i);
            map.put(p.getName(), p);
        }
        final NodeValue v = nodes.wrap(c, engine);
        nodeStates.put(v, new NodeState(ends[0], new ParamMapObject(map)));
        engine.setErrorListener(new AudioWorkletNode.ErrorListener() {
            /**
             * Fires <code>processorerror</code> at the node, as a task of the page.
             * <p>
             * 페이지의 작업으로 노드에 <code>processorerror</code> 를 발생시킨다.
             *
             * @param failure the failure of the processor / 프로세서의 실패
             */
            public void processorError(RuntimeException failure) {
                ErrorEvent event = new ErrorEvent("processorerror", false, false);
                // 스크립트 오류는 메시지, 파일, 줄, 열을 그대로 싣고, 그 밖의 실패는 문자열로 알린다.
                if (failure instanceof WorkletProcessorException) {
                    WorkletProcessorException w = (WorkletProcessorException) failure;
                    event.setContent(w.getMessage(), w.getFilename(), w.getLine(), w.getColumn(), null);
                } else {
                    event.setContent(String.valueOf(failure), "", 0, 0, null);
                }
                api.fire(v, event);
            }
        });
        engine.setProcessorFactory(scope.newFactory(name, serialized, ends[1]));
        return v;
    }

    /**
     * Converts a <code>sequence&lt;unsigned long&gt;</code> (the <code>outputChannelCount</code>): an array or an iterable.
     * <p>
     * <code>sequence&lt;unsigned long&gt;</code>(<code>outputChannelCount</code>)를 변환한다: 배열 또는 이터러블.
     *
     * @param v the value / 값
     * @return the numbers / 수
     */
    private int[] counts(JsValue v) {
        if (!(v instanceof JsObject)) {
            throw JsException.typeError("Failed to construct 'AudioWorkletNode': The provided value for 'outputChannelCount' is not a sequence.");
        }
        JsObject o = (JsObject) v;
        JsValue[] items;
        // 배열과 형식화 배열은 길이로 읽고, 그 밖의 객체는 반복자로 읽는다.
        if (Ops.isArray(o) || o instanceof TypedArrayObject) {
            items = Ops.createListFromArrayLike(o);
        } else {
            JsValue method = Ops.getMethod(o, WellKnownSymbols.ITERATOR);
            if (method == null || method.isNullOrUndefined()) {
                throw JsException.typeError("Failed to construct 'AudioWorkletNode': The provided value for 'outputChannelCount' is not iterable.");
            }
            List<JsValue> all = IteratorHelper.toList(realm, o);
            items = all.toArray(new JsValue[all.size()]);
        }
        int[] out = new int[items.length];
        // 항목마다 unsigned long 으로 변환한다.
        for (int i = 0; i < out.length; i++) {
            out[i] = Idl.ulong(items[i]);
        }
        return out;
    }

    /**
     * Converts <code>record&lt;DOMString, double&gt;</code> (the <code>parameterData</code>): the own enumerable string keys in order, each value to a finite number.
     * <p>
     * <code>record&lt;DOMString, double&gt;</code>(<code>parameterData</code>)를 변환한다: 순서대로 자기 열거 가능 문자열 키, 값은 각각 유한한 수로.
     *
     * @param v the value, undefined for none / 값. 없으면 undefined
     * @return the values by name (as float, the type of the value of a parameter) / 이름별 값(파라미터 값의 형식인 float 으로)
     */
    private Map<String, Float> parameterData(JsValue v) {
        Map<String, Float> out = new LinkedHashMap<String, Float>();
        if (v.isUndefined()) {
            return out;
        }
        if (!(v instanceof JsObject)) {
            throw JsException.typeError("Failed to construct 'AudioWorkletNode': The provided value for 'parameterData' is not a record.");
        }
        JsObject o = (JsObject) v;
        List<Object> keys = o.ownKeys();
        // 자기 열거 가능 문자열 키마다 값을 숫자로 바꾼다.
        for (int i = 0; i < keys.size(); i++) {
            if (!(keys.get(i) instanceof String)) {
                continue;
            }
            PropertyDescriptor d = o.getOwnProperty(keys.get(i));
            if (d == null || !d.isEnumerableTrue()) {
                continue;
            }
            out.put((String) keys.get(i), Float.valueOf((float) Idl.dbl(o.get(keys.get(i)))));
        }
        return out;
    }
}
