package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.PageTransitionEvent;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.HashChangeEvent;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.PopStateEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.html.ParserControl;
import org.duckdns.hjow.swingwebpanel.html.ParserListener;
import org.duckdns.hjow.swingwebpanel.html.ParserListenerAdapter;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseJobs;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseRejectionTracker;
import org.duckdns.hjow.swingwebpanel.js.ModuleGraph;
import org.duckdns.hjow.swingwebpanel.js.ModuleLoader;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.dom.HandlerListener;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The JavaScript side of one page (plan items P15-01, P15-04 to P15-07, P15-09, P15-11): it owns the realm, the DOM bridge, the
 * <code>window</code>, the event loop (tasks, timers, microtasks, mutation observers) and the script execution model. The component builds
 * one instance per page like this:
 * <ol>
 * <li><code>new PageScripts(document, environment)</code> before parsing;</li>
 * <li>parse with <code>new HtmlParser(document, text)</code> after <code>parser.setListener(scripts.createParserListener())</code>:
 * classic scripts run while the parser runs, <code>document.write</code> feeds the parser;</li>
 * <li>{@link #completeParsing()} after the parser finished: deferred and module scripts, <code>DOMContentLoaded</code>, async scripts,
 * <code>load</code>;</li>
 * <li>later tasks (timers, host events) run through {@link #runTask(Runnable)} and {@link #dispatchEvent(EventTarget, Event)};</li>
 * <li>{@link #dispose()} when the page is replaced or the component is disposed: every pending task is cancelled and callbacks that were
 * already scheduled never touch the new page.</li>
 * </ol>
 * All JavaScript of the page is serialized by one lock, so the loader thread, the document thread and the scheduler thread may call in
 * turn. A script that is aborted by a limit or by {@link #dispose()} stops without affecting other pages. When
 * {@link HostEnvironment#isScriptingEnabled()} is false no script, event handler attribute or timer runs (the plan item P15-10 behaviour);
 * HTML and CSS are not affected, and switching it on again does not run scripts of an already loaded page.
 * <p>
 * 한 페이지의 JavaScript 쪽(계획 P15-01, P15-04~07, P15-09, P15-11): realm, DOM 브리지, <code>window</code>, 이벤트 루프(작업, 타이머, 마이크로태스크,
 * 변경 관찰자), 스크립트 실행 모델을 가진다. 컴포넌트는 페이지마다 인스턴스 하나를 이렇게 만든다: ① 해석 전에
 * <code>new PageScripts(document, environment)</code>, ② <code>parser.setListener(scripts.createParserListener())</code> 뒤에 해석하면 파서가 도는
 * 동안 일반 스크립트가 실행되고 <code>document.write</code> 가 파서에 입력을 준다. ③ 파서가 끝나면 {@link #completeParsing()}: defer/module 스크립트,
 * <code>DOMContentLoaded</code>, async 스크립트, <code>load</code>. ④ 이후 작업(타이머, 호스트 이벤트)은 {@link #runTask(Runnable)} 과
 * {@link #dispatchEvent(EventTarget, Event)} 로 실행. ⑤ 페이지가 교체되거나 컴포넌트가 폐기되면 {@link #dispose()}: 대기 중인 모든 작업이 취소되고
 * 이미 예약된 콜백도 새 페이지를 건드리지 못한다. 페이지의 모든 JavaScript 는 잠금 하나로 직렬화되므로 로더 스레드, 문서 스레드, 스케줄러 스레드가 번갈아
 * 호출해도 된다. 한도나 {@link #dispose()} 로 중단된 스크립트는 다른 페이지에 영향을 주지 않는다. {@link HostEnvironment#isScriptingEnabled()} 가
 * false 이면 스크립트, 이벤트 핸들러 속성, 타이머가 실행되지 않는다(계획 P15-10 동작). HTML 과 CSS 는 영향받지 않으며 다시 켜도 이미 로드된
 * 페이지의 스크립트는 자동으로 다시 실행되지 않는다.
 */
public final class PageScripts {

    /**
     * MIME types of classic scripts (lower case).
     * <p>
     * 일반 스크립트의 MIME 타입(소문자).
     */
    private static final String[] JS_TYPES = { "application/ecmascript", "application/javascript", "application/x-ecmascript", "application/x-javascript",
        "text/ecmascript", "text/javascript", "text/javascript1.0", "text/javascript1.1", "text/javascript1.2", "text/javascript1.3",
        "text/javascript1.4", "text/javascript1.5", "text/jscript", "text/livescript", "text/x-ecmascript", "text/x-javascript" };

    /**
     * A script that waits for the end of parsing.
     * <p>
     * 해석이 끝나기를 기다리는 스크립트.
     */
    private static final class Pending {
        /**
         * The script element.
         * <p>
         * script 요소.
         */
        final Element element;

        /**
         * The script URL, or null for an inline script.
         * <p>
         * 스크립트 URL. 인라인이면 null.
         */
        final String url;

        /**
         * The inline text, or null.
         * <p>
         * 인라인 텍스트. 없으면 null.
         */
        final String text;

        /**
         * True for module scripts.
         * <p>
         * 모듈 스크립트이면 true.
         */
        final boolean module;

        /**
         * The fetch of an external script (started when the parser found it), or null for an inline script.
         * <p>
         * 외부 스크립트의 가져오기(파서가 발굴할 때 시작). 인라인 스크립트이면 null.
         */
        ScriptLoads.Load load;

        /**
         * True for a dynamically inserted script that must run in the order of insertion ("in order as soon as possible").
         * <p>
         * 삽입한 순서대로 실행해야 하는 동적으로 삽입한 스크립트이면 true("in order as soon as possible").
         */
        boolean ordered;

        /**
         * True once the fetch of an ordered script finished, so that it may run when it reaches the head of the ordered list.
         * <p>
         * 순서 있는 스크립트의 가져오기가 끝났으면 true. 순서 목록의 맨 앞에 오면 실행할 수 있다.
         */
        boolean ready;

        /**
         * Creates the entry.
         * <p>
         * 항목을 만든다.
         *
         * @param element the element / 요소
         * @param url     the URL or null / URL 또는 null
         * @param text    the inline text or null / 인라인 텍스트 또는 null
         * @param module  module flag / 모듈 여부
         */
        Pending(Element element, String url, String text, boolean module) {
            this.element = element;
            this.url = url;
            this.text = text;
            this.module = module;
        }
    }

    /**
     * The document.
     * <p>
     * 문서.
     */
    private final Document document;

    /**
     * The environment.
     * <p>
     * 환경.
     */
    private final HostEnvironment env;

    /**
     * The realm.
     * <p>
     * realm(JavaScript 실행 영역).
     */
    private final JsRealm realm;

    /**
     * The DOM bridge.
     * <p>
     * DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The window.
     * <p>
     * window(창 이벤트 대상).
     */
    private final Window window;

    /**
     * The lock that serializes all script execution of the page.
     * <p>
     * 페이지의 모든 스크립트 실행을 직렬화하는 잠금.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Timers and animation frames.
     * <p>
     * 타이머와 애니메이션 프레임.
     */
    private final Timers timers;

    /**
     * Mutation observers.
     * <p>
     * 변경 관찰자.
     */
    private final MutationObservers observers;

    /**
     * The geometry observers (<code>IntersectionObserver</code>, <code>ResizeObserver</code>) and their rendering step.
     * <p>
     * 기하 관찰자(<code>IntersectionObserver</code>, <code>ResizeObserver</code>)와 그 렌더링 단계.
     */
    private final RenderObservers rendering;

    /**
     * The times at which the page reached the stages of its loading (Navigation Timing).
     * <p>
     * 페이지가 로딩 단계에 이른 시각(Navigation Timing).
     */
    private final PageTimeline timeline = new PageTimeline();

    /**
     * <code>window.performance</code> of the page.
     * <p>
     * 페이지의 <code>window.performance</code>.
     */
    private final PagePerformance performance = new PagePerformance(this);

    /**
     * DOM XPath of the page (<code>document.evaluate</code> and its interfaces).
     * <p>
     * 페이지의 DOM XPath(<code>document.evaluate</code> 와 그 인터페이스).
     */
    private final XPathApi xpath = new XPathApi(this);

    /**
     * Returns DOM XPath of the page.
     * <p>
     * 페이지의 DOM XPath 를 반환한다.
     *
     * @return the XPath helper / XPath 도우미
     */
    XPathApi getXPath() {
        return xpath;
    }

    /**
     * The window globals (location, navigator, storage ...).
     * <p>
     * window 전역 객체들(location, navigator, 저장소 등).
     */
    private final WindowGlobals globals;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * True once the page fired <code>pageshow</code> (a page that was shown can be unloaded with the events of the unloading sequence).
     * <p>
     * 페이지가 <code>pageshow</code> 를 발생시킨 뒤이면 true(보인 페이지만 언로드 순서의 이벤트로 언로드할 수 있다).
     */
    private volatile boolean pageShown;

    /**
     * The life cycle events that the host reports for this page.
     * <p>
     * 호스트가 이 페이지에 대해 알리는 수명 이벤트.
     */
    private final PageLifecycle lifecycle = new PageLifecycle(this);

    /**
     * The document ready state: loading, interactive or complete.
     * <p>
     * 문서 준비 상태: loading, interactive, complete.
     */
    private String readyState = "loading";

    /**
     * The script that is executing now (null for modules and outside scripts).
     * <p>
     * 지금 실행 중인 스크립트(모듈이거나 스크립트 밖이면 null).
     */
    private Element currentScript;

    /**
     * The parser control during a parser-blocking script.
     * <p>
     * 파서를 막는 스크립트 실행 중의 파서 제어.
     */
    private ParserControl currentControl;

    /**
     * Whether the parser is running (scripts may call document.write).
     * <p>
     * 파서가 도는 중인지 여부(스크립트가 document.write 를 부를 수 있다).
     */
    private boolean parsing;

    /**
     * Scripts that wait for the end of parsing (defer and module).
     * <p>
     * 해석이 끝나기를 기다리는 스크립트(defer 와 module).
     */
    private final List<Pending> deferred = new ArrayList<Pending>();

    /**
     * Async scripts, run after parsing.
     * <p>
     * 해석 뒤에 실행하는 async 스크립트.
     */
    private final List<Pending> asyncScripts = new ArrayList<Pending>();

    /**
     * Dynamically inserted external scripts that run in the order of insertion once each one has been fetched.
     * <p>
     * 삽입한 순서대로, 각각 가져온 뒤에 실행하는 동적으로 삽입한 외부 스크립트.
     */
    private final List<Pending> orderedScripts = new ArrayList<Pending>();

    /**
     * True once the document stage after <code>DOMContentLoaded</code> has been reached (the load event may then fire).
     * <p>
     * <code>DOMContentLoaded</code> 뒤의 문서 단계에 이르렀으면 true(그러면 load 이벤트가 발생할 수 있다).
     */
    private volatile boolean loadStage;

    /**
     * True after the load event fired.
     * <p>
     * load 이벤트가 발생한 뒤 true.
     */
    private volatile boolean loadFired;

    /**
     * The base URL of each module by module key: the URL that finally served it (after redirects) for external modules and the document
     * base URL for inline modules. Relative imports of a module resolve against it. Guarded by the page lock.
     * <p>
     * 모듈 키별 모듈의 기준 URL: 외부 모듈은 최종적으로 제공한 URL(리디렉션 뒤), 인라인 모듈은 문서 기준 URL. 모듈의 상대 import 는 이 URL을
     * 기준으로 해석한다. 페이지 잠금으로 보호한다.
     */
    private final Map<String, String> moduleBases = new HashMap<String, String>();

    /**
     * The source text of the modules that were already fetched, by module key, so that a module that a script element and an import both ask
     * for is requested once. Guarded by the page lock.
     * <p>
     * 이미 가져온 모듈의 원문(모듈 키별). script 요소와 import 가 함께 요청한 모듈을 한 번만 요청하게 한다. 페이지 잠금으로 보호한다.
     */
    private final Map<String, String> moduleTexts = new HashMap<String, String>();

    /**
     * Whether the module graph that is being loaded sends cookies to every origin (the top-level script has
     * <code>crossorigin="use-credentials"</code>); its imports inherit the setting. Guarded by the page lock.
     * <p>
     * 불러오는 중인 모듈 그래프가 모든 오리진에 쿠키를 보내는지 여부(최상위 스크립트에 <code>crossorigin="use-credentials"</code> 가 있음).
     * import 한 모듈이 이 설정을 이어받는다. 페이지 잠금으로 보호한다.
     */
    private boolean moduleCredentials;

    /**
     * The number of inline module scripts so far; each one is a module of its own with a key of its own.
     * <p>
     * 지금까지의 인라인 모듈 스크립트 수. 각각이 자기 키를 가진 별개의 모듈이다.
     */
    private int inlineModules;

    /**
     * The nesting depth of script executions and tasks of the page (guarded by the page lock): the microtask checkpoint belongs to the
     * outermost one only, because the JavaScript stack is not empty while an inner script runs.
     * <p>
     * 페이지의 스크립트 실행과 작업의 중첩 깊이(페이지 잠금으로 보호). 안쪽 스크립트가 실행되는 동안에는 JavaScript 호출 스택이 비어 있지
     * 않으므로 마이크로태스크 검사점은 가장 바깥 것만 갖는다.
     */
    private int depth;

    /**
     * The parallel fetches of external scripts. Their completion reaches the thread that owns the page's DOM only through this set.
     * <p>
     * 외부 스크립트의 병렬 가져오기. 완료는 이 모음을 거쳐서만 페이지 DOM 의 소유 스레드에 도달한다.
     */
    private final ScriptLoads loads = new ScriptLoads();

    /**
     * The image loading of the <code>img</code> elements; its results arrive through {@link #loads}.
     * <p>
     * <code>img</code> 요소의 이미지 로딩. 결과는 {@link #loads} 로 도착한다.
     */
    private final ImageLoads images = new ImageLoads(this, loads);

    /**
     * The state machine of the <code>audio</code> and <code>video</code> elements; its tasks arrive through {@link #loads}.
     * <p>
     * <code>audio</code>, <code>video</code> 요소의 상태 기계. 작업은 {@link #loads} 로 도착한다.
     */
    private final MediaElements media = new MediaElements(this, loads);

    /**
     * The canvases of the page: the script interface of <code>canvas</code> and its 2D context, and the pictures that the renderer paints.
     * <p>
     * 페이지의 캔버스: <code>canvas</code> 와 2D 컨텍스트의 스크립트 인터페이스, 렌더러가 그리는 그림.
     */
    private final org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasBindings canvases = new org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasBindings(new CanvasHostImpl(this));

    /**
     * The time the page started (scheduler time), the origin of <code>performance.now()</code>.
     * <p>
     * 페이지가 시작된 시각(스케줄러 시각). <code>performance.now()</code> 의 기준.
     */
    private final long start;

    /**
     * Returns the start time of the page.
     * <p>
     * 페이지의 시작 시각을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    long startTime() {
        return start;
    }

    /**
     * The listener on the document changes.
     * <p>
     * 문서 변경에 대한 리스너.
     */
    private final DomMutationListener mutationListener;

    /**
     * Text written by the executing script while the parser is running; handed to the parser when the script ends.
     * <p>
     * 파서가 도는 동안 실행 중인 스크립트가 쓴 텍스트. 스크립트가 끝날 때 파서에 넘긴다.
     */
    private StringBuilder scriptWrites;

    /**
     * Text written by <code>document.write</code> after loading (flushed by <code>close()</code> or at the end of the task).
     * <p>
     * 로딩이 끝난 뒤 <code>document.write</code> 가 쓴 텍스트(<code>close()</code> 나 작업 끝에 처리한다).
     */
    private StringBuilder writeBuffer;

    /**
     * Promises that were rejected without a handler and still have none, with their reasons, in rejection order. The microtask checkpoint
     * reports what is left in it to the console and empties it.
     * <p>
     * 처리기 없이 거부되었고 아직 처리기가 없는 promise 와 그 이유(거부된 순서). 마이크로태스크 검사점이 남은 것을 콘솔에 보고하고 비운다.
     */
    private final Map<JsObject, JsValue> unhandledRejections = new LinkedHashMap<JsObject, JsValue>();

    /**
     * Creates the scripts of a page and installs the window, the DOM interfaces and the event loop.
     * <p>
     * 페이지의 스크립트 환경을 만들고 window, DOM 인터페이스, 이벤트 루프를 설치한다.
     *
     * @param document the page's document, not null / 페이지의 문서(null 불가)
     * @param env      the environment, not null / 환경(null 불가)
     */
    public PageScripts(Document document, HostEnvironment env) {
        if (document == null || env == null) {
            throw new IllegalArgumentException("document and environment must not be null");
        }
        this.document = document;
        this.env = env;
        this.realm = JsRealm.create();
        this.bridge = new DomBridge(realm);
        this.window = new Window();
        realm.getLimits().setMaxTimeMillis(env.getTaskTimeLimitMillis());
        // 처리기 없이 거부된 promise 를 기억해 두었다가 마이크로태스크 검사점이 끝날 때 콘솔에 보고한다.
        PromiseJobs.setRejectionTracker(realm, new PromiseRejectionTracker() {
            /**
             * Remembers a promise that was rejected without a handler.
             * <p>
             * 처리기 없이 거부된 promise 를 기억한다.
             *
             * @param r       the realm / realm
             * @param promise the promise / promise
             * @param reason  the reason / 이유
             */
            public void rejectedWithoutHandler(JsRealm r, JsObject promise, JsValue reason) {
                unhandledRejections.put(promise, reason);
            }

            /**
             * Forgets a promise that got a handler after it was rejected.
             * <p>
             * 거부된 뒤에 처리기를 얻은 promise 를 잊는다.
             *
             * @param r       the realm / realm
             * @param promise the promise / promise
             */
            public void handlerAddedToRejectedPromise(JsRealm r, JsObject promise) {
                unhandledRejections.remove(promise);
            }
        });
        this.start = env.getScheduler() == null ? System.currentTimeMillis() : env.getScheduler().now();
        bridge.setErrorSink(new DomBridge.ErrorSink() {
            /**
             * Reports an error that a listener or callback threw.
             * <p>
             * 리스너나 콜백이 던진 오류를 보고한다.
             *
             * @param error the error / 오류
             */
            public void scriptError(RuntimeException error) {
                reportError(error);
            }
        });
        DomInterfaces.install(bridge);
        document.setEventWindow(window);
        realm.setModuleLoader(new ModuleLoader() {
            /**
             * Resolves a module specifier against the importing module or the document URL.
             * <p>
             * 모듈 지정자를 가져오는 모듈이나 문서 URL 기준으로 해석한다.
             *
             * @param specifier the specifier / 지정자
             * @param referrer  the importing module key or null / 가져오는 모듈 키 또는 null
             * @return the module key (an absolute URL) / 모듈 키(절대 URL)
             */
            public String resolve(String specifier, String referrer) {
                // 상대 지정자는 가져오는 모듈의 기준 URL(리디렉션 뒤의 최종 URL, 인라인 모듈이면 문서 기준 URL)에서 해석한다.
                String base = PageScripts.this.document.getBaseUri();
                if (referrer != null && referrer.length() > 0) {
                    String known = moduleBases.get(referrer);
                    base = known != null ? known : referrer;
                }
                try {
                    return UrlUtil.stripFragment(UrlUtil.resolve(base, specifier));
                } catch (RuntimeException e) {
                    throw JsException.typeError("Failed to resolve module specifier '" + specifier + "'");
                }
            }

            /**
             * Fetches the source of a module through the page's script source, with the module rules when the source knows them (CORS,
             * JavaScript MIME type), and remembers the final URL that the module's own relative imports resolve against.
             * <p>
             * 페이지의 스크립트 공급원으로 모듈 소스를 가져온다. 공급원이 알면 모듈 규칙(CORS, JavaScript MIME 타입)으로 가져오며, 모듈 자신의
             * 상대 import 가 해석될 기준인 최종 URL을 기억한다.
             *
             * @param key the module key / 모듈 키
             * @return the source / 소스
             */
            public String fetch(String key) {
                ScriptSource src = PageScripts.this.env.getScriptSource();
                if (src == null) {
                    throw JsException.typeError("Failed to fetch module " + key);
                }
                String cached = moduleTexts.get(key);
                if (cached != null) {
                    return cached;
                }
                try {
                    ScriptText result = src instanceof ModuleScriptSource ? ((ModuleScriptSource) src).fetchModule(key, moduleCredentials)
                            : new ScriptText(src.fetch(key), key);
                    moduleBases.put(key, result.getUrl());
                    moduleTexts.put(key, result.getText());
                    return result.getText();
                } catch (IOException e) {
                    throw JsException.typeError("Failed to fetch module " + key + ": " + e.getMessage());
                }
            }
        });
        this.timers = new Timers(this);
        this.observers = new MutationObservers(this);
        this.rendering = new RenderObservers(this);
        this.globals = new WindowGlobals(this);
        globals.install();
        this.mutationListener = new DomMutationListener() {
            /**
             * Feeds the mutation observers, activates handler attributes and tells the component that the document changed.
             * <p>
             * 변경 관찰자에 전달하고, 핸들러 속성을 활성화하며, 문서가 바뀌었음을 컴포넌트에 알린다.
             *
             * @param record    the change / 변경
             * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
             */
            public void mutated(MutationRecord record, boolean connected) {
                observers.record(record);
                rendering.mutated(record, connected);
                xpath.mutated(record);
                handlerAttributes(record);
                images.mutated(record, connected);
                media.mutated(record, connected);
                globals.mutated(record, connected);
                if (connected) {
                    // 문서에 연결된 곳에 삽입된 script 요소는 삽입 즉시 준비한다.
                    if (record.getType() == MutationRecord.CHILD_LIST) {
                        List<Node> added = record.getAddedNodes();
                        // 추가된 노드마다 하위 트리의 script 요소를 찾아 준비한다.
                        for (int i = 0; i < added.size(); i++) {
                            scriptsInserted(added.get(i));
                            // 문서가 이미 해석된 뒤 삽입된 meta refresh 도 시작한다.
                            if (!parsing) {
                                MetaRefresh.scan(PageScripts.this, added.get(i));
                            }
                        }
                    }
                    PageScripts.this.env.documentChanged();
                }
            }
        };
        document.addMutationListener(mutationListener);
    }

    /**
     * Returns the image loading of the page.
     * <p>
     * 페이지의 이미지 로딩을 반환한다.
     *
     * @return the image loads / 이미지 로딩
     */
    ImageLoads images() {
        return images;
    }

    /**
     * Returns the media elements of the page.
     * <p>
     * 페이지의 미디어 요소를 반환한다.
     *
     * @return the media elements / 미디어 요소
     */
    MediaElements media() {
        return media;
    }

    /**
     * Tells whether the parser still works on the document (the media elements that the parser made wait for its end before their text tracks are
     * selected automatically).
     * <p>
     * 파서가 아직 문서를 처리하는 중인지 알려 준다(파서가 만든 미디어 요소는 텍스트 트랙을 자동으로 선택하기 전에 파서가 끝나기를 기다린다).
     *
     * @return true while parsing / 파싱 중이면 true
     */
    boolean isParsingDocument() {
        return parsing;
    }

    /**
     * Returns the canvases of the page, to install their script interface.
     * <p>
     * 페이지의 캔버스를 반환한다. 스크립트 인터페이스를 설치할 때 쓴다.
     *
     * @return the canvas bindings / 캔버스 연결
     */
    org.duckdns.hjow.swingwebpanel.js.host.canvas.CanvasBindings canvases() {
        return canvases;
    }

    /**
     * Returns the source of the canvas pictures that the renderer paints.
     * <p>
     * 렌더러가 그리는 캔버스 그림의 공급원을 반환한다.
     *
     * @return the source / 공급원
     */
    public org.duckdns.hjow.swingwebpanel.render.CanvasSource canvasSource() {
        return canvases;
    }

    /**
     * Returns the source of the control bar states of the <code>audio</code> elements that the renderer paints.
     * <p>
     * 렌더러가 그리는 <code>audio</code> 요소의 컨트롤 막대 상태의 공급원을 반환한다.
     *
     * @return the source / 공급원
     */
    public org.duckdns.hjow.swingwebpanel.render.MediaControlSource mediaControlSource() {
        return media;
    }

    /**
     * Applies an action of the user on the control bar of an <code>audio</code> element, on the thread that owns the page: a click on the play
     * button plays or pauses, a click or drag on the seek bar moves the position, a click on the mute button toggles the sound, and a click
     * or drag on the volume slider sets the volume. The element changes exactly as if a script had called <code>play()</code>,
     * <code>pause()</code> or set <code>currentTime</code>, <code>muted</code> or <code>volume</code>, so the events fire as usual.
     * <p>
     * 사용자가 <code>audio</code> 요소의 컨트롤 막대에서 한 동작을 페이지를 소유한 스레드에서 적용한다: 재생 버튼을 누르면 재생하거나 일시정지하고,
     * 탐색 막대를 누르거나 끌면 위치를 옮기고, 음소거 버튼을 누르면 소리를 켜고 끄며, 음량 슬라이더를 누르거나 끌면 음량을 정한다. 요소는 스크립트가
     * <code>play()</code>, <code>pause()</code> 를 부르거나 <code>currentTime</code>, <code>muted</code>, <code>volume</code> 을 정한 것과 똑같이
     * 바뀌므로 이벤트도 평소처럼 발생한다.
     *
     * @param target   the <code>audio</code> element / <code>audio</code> 요소
     * @param part     the part of the bar, one of the <code>PART_</code> constants of {@link org.duckdns.hjow.swingwebpanel.render.MediaControls} /
     *                 막대의 부분. {@link org.duckdns.hjow.swingwebpanel.render.MediaControls} 의 <code>PART_</code> 상수 중 하나
     * @param fraction the position on the seek bar or the volume from 0 to 1 (ignored for the buttons) / 탐색 막대의 위치 또는 음량(0에서 1).
     *                 버튼에서는 무시한다
     */
    public void controlMedia(final Element target, final int part, final double fraction) {
        runTask(new Runnable() {
            /**
             * Applies the action under the page lock.
             * <p>
             * 페이지 잠금 아래에서 동작을 적용한다.
             */
            public void run() {
                media.control(target, part, fraction);
            }
        });
    }

    /**
     * Claims the declarative refresh of the document: true for the first claim, false afterwards (only the first
     * <code>meta http-equiv=refresh</code> of a document counts).
     * <p>
     * 문서의 선언적 새로고침을 차지한다: 첫 요청은 true, 그 뒤는 false(문서의 첫 <code>meta http-equiv=refresh</code> 만 센다).
     *
     * @return true if this is the first claim / 첫 요청이면 true
     */
    boolean claimDeclarativeRefresh() {
        if (declarativeRefresh) {
            return false;
        }
        declarativeRefresh = true;
        return true;
    }

    /**
     * Whether the document already started a declarative refresh (<code>meta http-equiv=refresh</code>); only the first counts.
     * <p>
     * 문서가 이미 선언적 새로고침(<code>meta http-equiv=refresh</code>)을 시작했는지 여부. 첫 것만 센다.
     */
    private boolean declarativeRefresh;

    /**
     * Tells whether the page was disposed or its disposal was requested.
     * <p>
     * 페이지가 폐기되었거나 폐기가 요청되었는지 알려 준다.
     *
     * @return true if disposed / 폐기되었으면 true
     */
    boolean isDisposedPage() {
        return disposed;
    }

    /**
     * Tells whether the window load event was fired.
     * <p>
     * window load 이벤트가 발생했는지 알려 준다.
     *
     * @return true after load / load 뒤에는 true
     */
    boolean isLoadFired() {
        return loadFired;
    }

    /**
     * Tells whether the window <code>load</code> event of the page has fired. Callable from any thread.
     * <p>
     * 페이지의 window <code>load</code> 이벤트가 발생했는지 알려 준다. 어느 스레드에서나 부를 수 있다.
     *
     * @return true after the load event / load 이벤트 뒤에는 true
     */
    public boolean isLoadComplete() {
        return loadFired;
    }

    /**
     * Reports a change of the viewport of the component as page events in the order of "update the rendering": <code>resize</code> is fired at
     * the window when the size changed, <code>scroll</code> at the document (it bubbles to the window) when the scroll position changed, and
     * then the media query lists are evaluated again (their <code>change</code> events). Call it on the thread that owns the page's DOM.
     * <p>
     * 컴포넌트의 뷰포트가 바뀌었음을 "렌더링 갱신" 순서대로 페이지 이벤트로 알린다: 크기가 바뀌었으면 window 에 <code>resize</code>, 스크롤 위치가
     * 바뀌었으면 문서에 <code>scroll</code>(window 로 버블링)을 발생시키고, 그 뒤 미디어 질의 목록을 다시 평가한다(<code>change</code> 이벤트).
     * 페이지 DOM 의 소유 스레드에서 부른다.
     *
     * @param resized  true if the viewport size changed / 뷰포트 크기가 바뀌었으면 true
     * @param scrolled true if the scroll position changed / 스크롤 위치가 바뀌었으면 true
     */
    public void viewportChanged(final boolean resized, final boolean scrolled) {
        runTask(new Runnable() {
            /**
             * Fires the events under the page lock.
             * <p>
             * 페이지 잠금 아래에서 이벤트를 발생시킨다.
             */
            public void run() {
                // 렌더링 갱신 순서대로 resize, scroll 을 먼저 보내고 마지막에 미디어 질의 변화를 알린다.
                if (resized) {
                    fire(window, new Event("resize", false, false));
                }
                if (scrolled) {
                    fire(document, new Event("scroll", true, false));
                }
                if (resized) {
                    globals.viewportChanged();
                }
                // 뷰포트 크기나 스크롤 위치가 바뀌면 교차·크기 관찰자의 렌더링 단계가 필요하다.
                rendering.requestUpdate();
            }
        });
    }

    /**
     * Fires the <code>load</code> or <code>error</code> event of an image at the element (it does not bubble). Call it on the thread that owns
     * the page's DOM.
     * <p>
     * 이미지의 <code>load</code> 또는 <code>error</code> 이벤트를 요소에 발생시킨다(버블링하지 않는다). 페이지 DOM 의 소유 스레드에서 부른다.
     *
     * @param image the element / 요소
     * @param type  <code>load</code> or <code>error</code> / <code>load</code> 또는 <code>error</code>
     */
    void fireImageEvent(Element image, String type) {
        // 이미지 핸들러가 만든 프라미스 작업과 변경 관찰자 전달도 이 작업의 체크포인트에서 실행되도록 페이지 잠금 아래에서 보낸다.
        fire(image, new Event(type, false, false));
    }

    /**
     * Fires a simple event of a media element (<code>play</code>, <code>timeupdate</code>, <code>error</code> ...) at an element: it does not
     * bubble and cannot be cancelled. Call it on the thread that owns the page's DOM.
     * <p>
     * 미디어 요소의 단순 이벤트(<code>play</code>, <code>timeupdate</code>, <code>error</code> 등)를 요소에 발생시킨다: 버블링하지 않고 취소할 수
     * 없다. 페이지 DOM 의 소유 스레드에서 부른다.
     *
     * @param target the element (the media element, or a <code>source</code> element for an <code>error</code>) / 요소(미디어 요소, 또는
     *               <code>error</code> 의 <code>source</code> 요소)
     * @param type   the event type / 이벤트 종류
     */
    void fireMediaEvent(Element target, String type) {
        fire(target, new Event(type, false, false));
    }

    /**
     * The receivers that are called at the end of every outermost task, after its microtask checkpoint (under the page lock).
     * <p>
     * 가장 바깥 작업마다 마이크로태스크 검사점 뒤에 불리는 받는 쪽(페이지 잠금 아래).
     */
    private final List<Runnable> taskEndHooks = new java.util.concurrent.CopyOnWriteArrayList<Runnable>();

    /**
     * Adds a receiver that is called at the end of every outermost task, after its microtask checkpoint; the CSS Custom Highlight API uses it to paint
     * again when a script moved the boundary of a highlighted range.
     * <p>
     * 가장 바깥 작업마다 마이크로태스크 검사점 뒤에 불리는 받는 쪽을 더한다. CSS Custom Highlight API 가 스크립트가 하이라이트된 범위의 경계를 옮겼을 때 다시 그리는 데 쓴다.
     *
     * @param hook the receiver / 받는 쪽
     */
    void onTaskEnd(Runnable hook) {
        taskEndHooks.add(hook);
    }

    /**
     * Returns the realm (use only while holding the page lock, for example inside {@link #runTask(Runnable)}).
     * <p>
     * realm 을 반환한다(페이지 잠금을 잡은 동안, 예를 들어 {@link #runTask(Runnable)} 안에서만 쓴다).
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
     * Returns the window target.
     * <p>
     * window 대상을 반환한다.
     *
     * @return the window / window
     */
    public Window getWindow() {
        return window;
    }

    /**
     * Returns the document.
     * <p>
     * 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the environment.
     * <p>
     * 환경을 반환한다.
     *
     * @return the environment / 환경
     */
    public HostEnvironment getEnvironment() {
        return env;
    }

    /**
     * Returns the messaging api of the page, through which other windows post messages to this window.
     * <p>
     * 페이지의 메시징 API 를 반환한다. 다른 window 가 이 window 로 메시지를 보내는 통로이다.
     *
     * @return the api / API
     */
    MessagingApi messaging() {
        return globals.messaging();
    }

    /**
     * Tells the page that the frames of its document were created, removed or renamed, so that <code>window[0]</code>, <code>window[name]</code>
     * and <code>length</code> follow. Call it on the thread that owns the DOM; it takes the page lock.
     * <p>
     * 문서의 프레임이 생성, 제거, 이름 변경되었음을 페이지에 알려 <code>window[0]</code>, <code>window[name]</code>, <code>length</code> 가 따라가게 한다.
     * DOM 을 소유한 스레드에서 부르며 페이지 잠금을 잡는다.
     */
    public void framesChanged() {
        final FrameApi api = globals.frameApi();
        if (api != null && !disposed) {
            runTask(new Runnable() {
                /** Redefines the frame properties of the window. / window 의 프레임 프로퍼티를 다시 정의한다. */
                public void run() {
                    api.refresh();
                }
            });
        }
    }

    /**
     * Returns the event loop helper for timers.
     * <p>
     * 타이머용 이벤트 루프 도우미를 반환한다.
     *
     * @return the timers / 타이머
     */
    Timers getTimers() {
        return timers;
    }

    /**
     * Returns the mutation observers helper.
     * <p>
     * 변경 관찰자 도우미를 반환한다.
     *
     * @return the observers / 관찰자
     */
    MutationObservers getObservers() {
        return observers;
    }

    /**
     * Returns the geometry observers of the page.
     * <p>
     * 페이지의 기하 관찰자를 반환한다.
     *
     * @return the observers / 관찰자
     */
    RenderObservers getRendering() {
        return rendering;
    }

    /**
     * Returns the times of the loading stages of the page.
     * <p>
     * 페이지의 로딩 단계 시각을 반환한다.
     *
     * @return the timeline / 타임라인
     */
    PageTimeline getTimeline() {
        return timeline;
    }

    /**
     * Returns <code>window.performance</code> of the page.
     * <p>
     * 페이지의 <code>window.performance</code> 를 반환한다.
     *
     * @return the performance object / performance 객체
     */
    PagePerformance getPerformance() {
        return performance;
    }

    /**
     * Records that a loading stage was reached now.
     * <p>
     * 로딩 단계에 지금 이르렀음을 기록한다.
     *
     * @param stage the stage of {@link PageTimeline} / {@link PageTimeline} 의 단계
     */
    private void reached(int stage) {
        Scheduler s = env.getScheduler();
        timeline.reached(stage, s == null ? System.currentTimeMillis() : s.now());
    }

    /**
     * Tells the page that the component published a new layout (the geometry that the geometry observers watch may have changed): the next rendering
     * step runs if something is observed. It only posts to the scheduler and may be called from any thread.
     * <p>
     * 컴포넌트가 새 레이아웃을 게시했음을 페이지에 알린다(기하 관찰자가 지켜보는 기하가 바뀌었을 수 있다). 관찰하는 것이 있으면 다음 렌더링 단계가 실행된다.
     * 스케줄러에 올리기만 하며 어느 스레드에서나 부를 수 있다.
     */
    public void layoutPublished() {
        rendering.requestUpdate();
    }

    /**
     * Tells whether the page was disposed.
     * <p>
     * 페이지가 폐기되었는지 알려 준다.
     *
     * @return true after dispose / 폐기 뒤에는 true
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Tells whether the page fired <code>pageshow</code>, which is when it counts as shown to the user.
     * <p>
     * 페이지가 <code>pageshow</code> 를 발생시켰는지 알려 준다. 그때부터 사용자에게 보인 것으로 센다.
     *
     * @return true after <code>pageshow</code> / <code>pageshow</code> 뒤에는 true
     */
    public boolean isPageShown() {
        return pageShown;
    }

    /**
     * Returns the life cycle events of this page (unload, visibility, window focus, network state).
     * <p>
     * 이 페이지의 수명 이벤트(언로드, 가시성, window 포커스, 네트워크 상태)를 반환한다.
     *
     * @return the life cycle / 수명 이벤트
     */
    public PageLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Returns the document ready state.
     * <p>
     * 문서 준비 상태를 반환한다.
     *
     * @return loading, interactive or complete / loading, interactive, complete
     */
    String getReadyState() {
        return readyState;
    }

    /**
     * Returns the executing classic script or null.
     * <p>
     * 실행 중인 일반 스크립트를 반환한다. 없으면 null.
     *
     * @return the script element or null / script 요소 또는 null
     */
    Element getCurrentScript() {
        return currentScript;
    }

    /**
     * Returns the scripting flag of the environment.
     * <p>
     * 환경의 스크립트 사용 플래그를 반환한다.
     *
     * @return true if scripts may run / 스크립트를 실행할 수 있으면 true
     */
    boolean scripting() {
        return !disposed && env.isScriptingEnabled();
    }

    /**
     * Runs a task of the page under the page lock: the task body, then the microtask checkpoint, the mutation observer delivery and the
     * flush of text written after loading. Errors are reported to <code>window.onerror</code> and the console; a task that is aborted by
     * a limit ends quietly. Nothing runs after {@link #dispose()}.
     * <p>
     * 페이지의 작업을 페이지 잠금 아래에서 실행한다: 작업 본문, 마이크로태스크 검사점, 변경 관찰자 전달, 로딩 뒤에 쓴 텍스트의 처리. 오류는
     * <code>window.onerror</code> 와 콘솔에 보고하며 한도로 중단된 작업은 조용히 끝난다. {@link #dispose()} 뒤에는 아무것도 실행되지 않는다.
     *
     * @param body the task / 작업
     */
    public void runTask(Runnable body) {
        lock.lock();
        boolean outermost = markEntry();
        try {
            depth++;
            if (disposed) {
                return;
            }
            realm.getLimits().beginTask();
            try {
                body.run();
                // 다른 작업 안에 중첩된 작업은 바깥 작업이 끝날 때의 검사점에 맡긴다.
                if (depth == 1) {
                    checkpoint();
                }
            } catch (JsException e) {
                reportError(e);
            } catch (ScriptAbortedException e) {
                reportAbort(e);
            } finally {
                realm.getLimits().endTask();
            }
        } finally {
            depth--;
            clearEntry(outermost);
            lock.unlock();
        }
    }

    /**
     * Records this page as the entry of the current thread if no page runs on it yet. A page that a script of another page calls into stays
     * under the page that started the work, so that the sandbox rules for navigation see who really asked.
     * <p>
     * 현재 스레드에서 아직 실행 중인 페이지가 없으면 이 페이지를 entry 로 기록한다. 다른 페이지의 스크립트가 부르는 페이지는 작업을 시작한 페이지 아래에 두어,
     * 이동에 대한 샌드박스 규칙이 실제로 누가 요청했는지 볼 수 있게 한다.
     *
     * @return true if this call recorded the page (and {@link #clearEntry} must follow) / 이 호출이 기록했으면 true({@link #clearEntry} 를 이어 불러야 한다)
     */
    private boolean markEntry() {
        if (ENTRY.get() != null) {
            return false;
        }
        ENTRY.set(this);
        return true;
    }

    /**
     * Ends what {@link #markEntry} began.
     * <p>
     * {@link #markEntry} 가 시작한 것을 끝낸다.
     *
     * @param marked the result of {@link #markEntry} / {@link #markEntry} 의 결과
     */
    private static void clearEntry(boolean marked) {
        if (marked) {
            ENTRY.remove();
        }
    }

    /**
     * The page whose task runs outermost on the current thread (the "entry" of the HTML Standard): scripts of other documents that it calls
     * run inside its task.
     * <p>
     * 현재 스레드에서 가장 바깥에서 작업을 실행 중인 페이지(HTML 표준의 "entry"). 그것이 부르는 다른 문서의 스크립트는 그 작업 안에서 실행된다.
     */
    private static final ThreadLocal<PageScripts> ENTRY = new ThreadLocal<PageScripts>();

    /**
     * Returns the browsing context of the script that is running on this thread, the one that started the outermost task (a script that
     * calls into another document is still the caller there). Used to apply the sandbox rules to navigations that scripts start.
     * <p>
     * 이 스레드에서 실행 중인 스크립트의 브라우징 문맥을 반환한다. 가장 바깥 작업을 시작한 문맥이다(다른 문서로 들어가 부르는 스크립트도 거기서는 여전히
     * 호출자이다). 스크립트가 시작하는 이동에 샌드박스 규칙을 적용하는 데 쓴다.
     *
     * @return the context, or null when no script runs on this thread / 문맥. 이 스레드에서 실행 중인 스크립트가 없으면 null
     */
    public static BrowsingContext entryContext() {
        PageScripts p = ENTRY.get();
        return p == null || p.env == null ? null : p.env.getBrowsingContext();
    }

    /**
     * The microtask checkpoint: runs the promise jobs, then delivers mutation records, then flushes document.write text; repeats while
     * there is more work.
     * <p>
     * 마이크로태스크 검사점: 프라미스 작업을 실행하고 변경 기록을 전달하며 document.write 텍스트를 처리한다. 일이 남아 있으면 되풀이한다.
     */
    private void checkpoint() {
        // 마이크로태스크와 변경 관찰자 전달이 서로 일을 만들 수 있으므로 더 이상 일이 없을 때까지 되풀이한다.
        for (int i = 0; i < 1000; i++) {
            realm.runJobs();
            boolean more = observers.deliver();
            if (writeBuffer != null && parsing == false && readyState.equals("loading") == false) {
                // 로딩이 끝난 뒤 쓴 텍스트는 작업이 끝날 때 문서로 만든다.
                more = flushWrite() || more;
            }
            if (!more) {
                break;
            }
        }
        reportUnhandledRejections();
        // 작업이 끝날 때 알려 받는 쪽(예: 하이라이트의 범위 변경 확인)을 부른다.
        for (int i = 0; i < taskEndHooks.size(); i++) {
            taskEndHooks.get(i).run();
        }
    }

    /**
     * Gives the document that was loaded again for a history entry the state object of that entry as its first <code>history.state</code>
     * (HTML5 2014 REC 5.6.10: the state of a state object entry is restored when the entry is traversed to). Call before any script ran.
     * <p>
     * 이력 항목을 위해 다시 읽은 문서에, 그 항목의 상태 객체를 첫 <code>history.state</code> 로 준다(HTML5 2014 REC 5.6.10: 상태 객체 항목으로
     * 이동하면 그 상태를 복원한다). 어떤 스크립트도 실행되기 전에 호출한다.
     *
     * @param state the serialized state of the entry, or null / 항목의 직렬화된 상태. 없으면 null
     */
    public void setInitialHistoryState(ClonedValue state) {
        globals.history().setState(state);
    }

    /**
     * Makes <code>history.state</code> null at once, for a navigation to a fragment of the same document: its new entry has no state object,
     * and a script that reads <code>history.state</code> right after changing <code>location.hash</code> must not see the old one.
     * <p>
     * <code>history.state</code> 를 곧바로 null 로 만든다. 같은 문서의 프래그먼트로 가는 이동의 새 항목은 상태 객체가 없고,
     * <code>location.hash</code> 를 바꾼 직후 <code>history.state</code> 를 읽는 스크립트가 옛 값을 보면 안 되기 때문이다.
     */
    public void clearHistoryState() {
        globals.history().setState(null);
    }

    /**
     * Tells the page that its document moved to another entry of the session history (a traversal between the entries that
     * <code>history.pushState</code> made, or a navigation that only changed the fragment): the state object of the entry becomes
     * <code>history.state</code>, <code>popstate</code> is fired at the window (HTML5 2014 REC 5.6.10), and <code>hashchange</code> follows
     * when the fragment differs. It runs as a page task, so listeners and microtasks follow the usual order.
     * <p>
     * 문서가 세션 이력의 다른 항목으로 옮겨 갔음을 페이지에 알린다(<code>history.pushState</code> 가 만든 항목 사이의 이동이나 프래그먼트만 바꾼
     * 이동). 항목의 상태 객체가 <code>history.state</code> 가 되고, window 에 <code>popstate</code> 가 발생하며(HTML5 2014 REC 5.6.10), 프래그먼트가
     * 다르면 이어서 <code>hashchange</code> 가 발생한다. 페이지 작업으로 실행되므로 리스너와 마이크로태스크는 평소의 순서를 따른다.
     *
     * @param state       the serialized state of the entry moved to, or null / 옮겨 간 항목의 직렬화된 상태. 없으면 null
     * @param popState    true to fire <code>popstate</code> / <code>popstate</code> 를 보내려면 true
     * @param oldUrl      the URL before / 이전 URL
     * @param newUrl      the URL after / 이후 URL
     * @param hashChanged true to fire <code>hashchange</code> / <code>hashchange</code> 를 보내려면 true
     */
    public void historyTraversed(final ClonedValue state, final boolean popState, final String oldUrl, final String newUrl,
            final boolean hashChanged) {
        runTask(new Runnable() {
            /**
             * Sets the state, then creates and dispatches the events.
             * <p>
             * 상태를 지정한 뒤 이벤트를 만들어 전달한다.
             */
            public void run() {
                HistoryApi h = globals.history();
                h.setState(state);
                if (popState) {
                    PopStateEvent pop = new PopStateEvent("popstate", true, false);
                    pop.setState(h.stateValue());
                    window.dispatchEvent(pop);
                }
                if (hashChanged) {
                    HashChangeEvent hash = new HashChangeEvent("hashchange", true, false);
                    hash.setUrls(oldUrl, newUrl);
                    window.dispatchEvent(hash);
                }
            }
        });
    }

    /**
     * Dispatches an event from the host (for example a click from the input layer) as a page task and returns whether it was not cancelled.
     * <p>
     * 호스트가 만든 이벤트(예: 입력 계층의 클릭)를 페이지 작업으로 전달하고 취소되지 않았는지 반환한다.
     *
     * @param target the target / 대상
     * @param event  the event / 이벤트
     * @return true if no listener cancelled it / 어떤 리스너도 취소하지 않았으면 true
     */
    public boolean dispatchEvent(final EventTarget target, final Event event) {
        return dispatchEvent(target, event, false);
    }

    /**
     * Dispatches actual host input with isTrusted preserved and a task checkpoint. After script cancellation, Java DOM
     * listeners still receive the input while the bridge's permanent callback gate prevents JavaScript reentry.
     * <p>실제 호스트 입력을 신뢰 표시와 작업 체크포인트를 유지해 전달한다. 스크립트 취소 뒤에도 Java DOM 리스너에 입력을 보내되
     * 연결 계층의 영구 콜백 차단으로 JavaScript 재진입을 막는다.
     * @param target event target / 이벤트 대상
     * @param event host-created input / 호스트가 만든 입력
     * @return true if not cancelled / 취소되지 않았으면 true
     */
    public boolean dispatchTrustedEvent(EventTarget target, Event event) {
        if (disposed) {
            return target.dispatchTrustedEvent(event);
        }
        return dispatchEvent(target, event, true);
    }

    /** Dispatches inside one page task without changing the origin of the event. / 이벤트의 출처를 바꾸지 않고 페이지 작업 하나 안에서 전달한다.
     * @param target event target / 이벤트 대상
     * @param event dispatched event / 전달할 이벤트
     * @param trusted host input flag / 호스트 입력 표시
     * @return cancellation result / 취소 결과 */
    private boolean dispatchEvent(final EventTarget target, final Event event, final boolean trusted) {
        final boolean[] result = { true };
        runTask(new Runnable() {
            /**
             * Dispatches the event.
             * <p>
             * 이벤트를 전달한다.
             */
            public void run() {
                try {
                    result[0] = trusted ? target.dispatchTrustedEvent(event) : target.dispatchEvent(event);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        });
        return result[0];
    }

    /**
     * Evaluates a classic script text as a page task (used by tests and by the host); the completion value is returned as a string, null
     * when the script threw.
     * <p>
     * 일반 스크립트 텍스트를 페이지 작업으로 평가한다(시험과 호스트가 쓴다). 완료 값을 문자열로 반환하며 스크립트가 던졌으면 null.
     *
     * @param source the script / 스크립트
     * @param name   the script name for error messages / 오류 메시지용 스크립트 이름
     * @return the completion value as text, or null / 완료 값의 문자열. 던졌으면 null
     */
    public String evaluate(final String source, final String name) {
        final String[] out = new String[1];
        lock.lock();
        boolean outermost = markEntry();
        try {
            depth++;
            if (disposed || !env.isScriptingEnabled()) {
                return null;
            }
            realm.getLimits().beginTask();
            try {
                JsValue v = realm.evaluateScript(source, name);
                out[0] = Ops.toString(v);
                if (depth == 1) {
                    checkpoint();
                }
            } catch (JsException e) {
                reportError(e);
            } catch (ScriptAbortedException e) {
                reportAbort(e);
            } finally {
                realm.getLimits().endTask();
            }
        } finally {
            depth--;
            clearEntry(outermost);
            lock.unlock();
        }
        return out[0];
    }

    /**
     * Reports an error thrown by script code: to <code>window.onerror</code>, to the <code>error</code> listeners of the window and, if
     * nobody handled it, to the console. Never throws.
     * <p>
     * 스크립트 코드가 던진 오류를 보고한다: <code>window.onerror</code>, window 의 <code>error</code> 리스너, 아무도 처리하지 않으면 콘솔. 예외를
     * 던지지 않는다.
     *
     * @param error the error / 오류
     */
    void reportError(RuntimeException error) {
        if (error instanceof ScriptAbortedException) {
            reportAbort((ScriptAbortedException) error);
            return;
        }
        String message = String.valueOf(error);
        if (error instanceof JsException) {
            JsException je = (JsException) error;
            message = je.hasValue() ? JsException.describe(je.getValue(realm)) : je.getErrorMessage();
            if (message == null) {
                message = String.valueOf(je.getMessage());
            }
        }
        String source = error instanceof JsException ? ((JsException) error).getSourceName() : "";
        int line = error instanceof JsException ? ((JsException) error).getLine() : 0;
        int col = error instanceof JsException ? ((JsException) error).getColumn() : 0;
        try {
            if (globals.callOnError(message, source == null ? "" : source, line, col, error)) {
                return;
            }
            Event ev = new Event("error", false, true);
            HostObject w = bridge.wrap(ev);
            w.defineData("message", Js.str(message), true, true, true);
            w.defineData("filename", Js.str(source == null ? "" : source), true, true, true);
            w.defineData("lineno", Js.num(line), true, true, true);
            w.defineData("colno", Js.num(col), true, true, true);
            if (error instanceof JsException && ((JsException) error).hasValue()) {
                w.defineData("error", ((JsException) error).getValue(realm), true, true, true);
            }
            window.dispatchEvent(ev);
            if (!ev.isDefaultPrevented()) {
                report(ConsoleRecord.Kind.UNCAUGHT_EXCEPTION, "Uncaught " + message, source, line, col);
            }
        } catch (RuntimeException e) {
            report(ConsoleRecord.Kind.UNCAUGHT_EXCEPTION, "Uncaught " + message, source, line, col);
        }
    }

    /**
     * Sends a report of the host about the page's scripts to the console, as a record that tells what kind of report it is.
     * <p>
     * 페이지 스크립트에 대한 호스트의 보고를, 어떤 종류의 보고인지 알리는 기록으로 콘솔에 보낸다.
     *
     * @param kind   what is reported / 보고하는 내용
     * @param text   the message / 메시지
     * @param source the script name or null / 스크립트 이름 또는 null
     * @param line   the line or 0 / 줄 또는 0
     * @param column the column or 0 / 열 또는 0
     */
    void report(ConsoleRecord.Kind kind, String text, String source, int line, int column) {
        ConsoleRecord.deliver(realm.getConsole(), ConsoleRecord.report(kind, text, source, line, column));
    }

    /**
     * Reports the promises that were rejected without a handler and still have none, the way a browser does after the microtask
     * checkpoint ("Uncaught (in promise)"), and forgets them. Never throws.
     * <p>
     * 처리기 없이 거부되었고 아직 처리기가 없는 promise 를 브라우저처럼 마이크로태스크 검사점이 끝난 뒤 보고하고("Uncaught (in promise)") 잊는다.
     * 예외를 던지지 않는다.
     */
    private void reportUnhandledRejections() {
        if (unhandledRejections.isEmpty()) {
            return;
        }
        List<JsValue> reasons = new ArrayList<JsValue>(unhandledRejections.values());
        unhandledRejections.clear();
        // 이유를 거부된 순서대로 한 줄씩 보고한다.
        for (int i = 0; i < reasons.size(); i++) {
            try {
                report(ConsoleRecord.Kind.UNHANDLED_REJECTION, "Uncaught (in promise) " + JsException.describe(reasons.get(i)), "", 0, 0);
            } catch (RuntimeException e) {
                report(ConsoleRecord.Kind.UNHANDLED_REJECTION, "Uncaught (in promise)", "", 0, 0);
            }
        }
    }

    /**
     * Forwards the console record of a worker of this page to the console of the page, which marks it as written by a worker.
     * <p>
     * 이 페이지의 worker 가 쓴 콘솔 기록을, worker 가 쓴 것임을 표시하는 페이지의 콘솔로 전달한다.
     *
     * @param record    the record / 기록
     * @param workerUrl the script URL of the worker / worker 의 스크립트 URL
     * @param shared    true for a shared worker / 공유 worker 이면 true
     */
    public void workerConsole(ConsoleRecord record, String workerUrl, boolean shared) {
        ConsoleHook hook = realm.getConsole();
        // 출처를 아는 페이지 콘솔이면 worker 의 기록으로, 일반 연결이면 기록을 그대로 보낸다.
        if (hook instanceof HostConsole) {
            ((HostConsole) hook).workerRecord(record, workerUrl, shared);
        } else {
            ConsoleRecord.deliver(hook, record);
        }
    }

    /**
     * Reports an aborted script (limit, time or cancellation) to the console unless the page was disposed.
     * <p>
     * 중단된 스크립트(한도, 시간, 취소)를 콘솔에 보고한다. 페이지가 폐기되었으면 보고하지 않는다.
     *
     * @param e the abort / 중단
     */
    private void reportAbort(ScriptAbortedException e) {
        if (!disposed) {
            report(ConsoleRecord.Kind.SCRIPT_ABORTED, "Script aborted: " + e.getMessage(), "", 0, 0);
            realm.getLimits().reset();
        }
    }

    /**
     * Creates the parser listener that runs scripts while the page is parsed. Wrap it if other parts of the component (style sheet loading,
     * image loading) also listen: it forwards nothing, the component's own listener must call it.
     * <p>
     * 페이지를 해석하는 동안 스크립트를 실행하는 파서 리스너를 만든다. 컴포넌트의 다른 부분(스타일시트 로딩, 이미지 로딩)도 듣는다면 이 리스너를 감싸
     * 쓴다. 아무것도 전달하지 않으므로 컴포넌트의 리스너가 이것을 불러야 한다.
     *
     * @return the listener / 리스너
     */
    public ParserListener createParserListener() {
        return new ParserListenerAdapter() {
            /**
             * Runs or schedules a script element that the parser finished.
             * <p>
             * 파서가 끝낸 script 요소를 실행하거나 예약한다.
             *
             * @param script  the script element / script 요소
             * @param control the parser control / 파서 제어
             */
            public void scriptReady(Element script, ParserControl control) {
                scriptFromParser(script, control);
            }
        };
    }

    /**
     * Marks the start of parsing; scripts may now call <code>document.write</code>.
     * <p>
     * 해석의 시작을 표시한다. 이제 스크립트가 <code>document.write</code> 를 부를 수 있다.
     */
    public void parsingStarted() {
        parsing = true;
    }

    /**
     * Decides what to do with a script element that the parser reports.
     * <p>
     * 파서가 알린 script 요소를 어떻게 할지 정한다.
     *
     * @param script  the element / 요소
     * @param control the parser control / 파서 제어
     */
    private void scriptFromParser(Element script, ParserControl control) {
        if (!scripting()) {
            return;
        }
        // 파서가 스크립트 경계에 온 이 시점에, 이미 가져오기가 끝난 async 스크립트를 먼저 실행한다.
        runReadyAsync();
        if (!scripting()) {
            return;
        }
        ParserNodeData.ScriptFlags flags = ParserNodeData.scriptFlags(script);
        if (flags.isAlreadyStarted()) {
            return;
        }
        String type = script.getAttribute("type");
        String lang = script.getAttribute("language");
        boolean module = type != null && "module".equals(type.trim().toLowerCase());
        if (!module && !isJavaScriptType(type, lang)) {
            return;
        }
        // 모듈을 지원하므로 nomodule 이 붙은 일반 스크립트는 실행하지 않는다(모듈을 모르는 브라우저를 위한 대체 스크립트).
        if (!module && script.hasAttribute("nomodule")) {
            return;
        }
        // for 와 event 속성이 있는 스크립트는 window 와 onload 일 때만 실행한다(HTML5 2014 §4.11.1.1).
        if (!allowedByEventAttributes(script)) {
            flags.setAlreadyStarted(true);
            return;
        }
        String src = script.getAttribute("src");
        if (src != null && src.trim().length() > 0) {
            String url;
            try {
                url = UrlUtil.resolve(document.getBaseUri(), src.trim());
            } catch (RuntimeException e) {
                flags.setAlreadyStarted(true);
                fireScriptEvent(script, "error");
                return;
            }
            flags.setAlreadyStarted(true);
            Pending p = new Pending(script, url, null, module);
            boolean credentials = useCredentials(script);
            if (script.hasAttribute("async")) {
                // async 스크립트는 발굴 즉시 가져오기를 시작하고, 가져오기가 끝나면 파서의 다음 경계나 대기 중에 실행된다.
                p.load = loads.start(env.getScriptSource(), url, p, true, true, module, credentials);
                asyncScripts.add(p);
            } else if (module || script.hasAttribute("defer")) {
                // defer 와 module 스크립트도 발굴 즉시 가져오기를 시작하지만 파싱이 끝난 뒤 문서 순서로 실행한다.
                p.load = loads.start(env.getScriptSource(), url, p, false, true, module, credentials);
                deferred.add(p);
            } else {
                // 파서를 막는 일반 스크립트는 가져오는 동안 파서를 세운 채 기다리고, 그 사이 끝난 async 스크립트는 실행한다.
                p.load = loads.start(env.getScriptSource(), url, p, false, false, false, false);
                if (awaitLoad(p.load)) {
                    runPending(p, control);
                }
            }
            return;
        }
        String text = script.getTextContent();
        if (text.length() == 0) {
            return;
        }
        flags.setAlreadyStarted(true);
        if (module) {
            deferred.add(new Pending(script, null, text, true));
        } else {
            execute(script, text, scriptName(script), false, control);
        }
    }

    /**
     * Returns a name for error messages of an inline script.
     * <p>
     * 인라인 스크립트의 오류 메시지용 이름을 반환한다.
     *
     * @param script the element / 요소
     * @return the name / 이름
     */
    private String scriptName(Element script) {
        return document.getUrl() + "#inline";
    }

    /**
     * Tells whether a MIME type (without parameters or with them) is a JavaScript MIME type. Module scripts must be served with one
     * (classic scripts are not checked).
     * <p>
     * MIME 타입(매개변수는 있어도 없어도 된다)이 JavaScript MIME 타입인지 알려 준다. 모듈 스크립트는 이 타입으로 제공되어야 한다(일반 스크립트는
     * 검사하지 않는다).
     *
     * @param mime the MIME type or null / MIME 타입 또는 null
     * @return true for a JavaScript MIME type / JavaScript MIME 타입이면 true
     */
    public static boolean isJavaScriptMimeType(String mime) {
        if (mime == null) {
            return false;
        }
        String t = mime.trim().toLowerCase();
        int semi = t.indexOf(';');
        if (semi >= 0) {
            t = t.substring(0, semi).trim();
        }
        // 알려진 JavaScript MIME 타입 목록과 비교한다.
        for (int i = 0; i < JS_TYPES.length; i++) {
            if (JS_TYPES[i].equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a script type or language attribute names JavaScript (empty means JavaScript).
     * <p>
     * script 의 type 또는 language 속성이 JavaScript 를 가리키는지 알려 준다(비어 있으면 JavaScript).
     *
     * @param type the type attribute or null / type 속성 또는 null
     * @param lang the language attribute or null / language 속성 또는 null
     * @return true for JavaScript / JavaScript 이면 true
     */
    private static boolean isJavaScriptType(String type, String lang) {
        String t;
        // 스크립트 블록의 타입: 빈 type 은 JavaScript, 비어 있지 않은 type, 없으면 "text/" 를 붙인 language, 둘 다 없으면 JavaScript.
        if (type != null) {
            t = type.trim().length() == 0 ? "text/javascript" : type.trim();
            int semi = t.indexOf(';');
            // 현행 표준과 브라우저처럼 MIME 매개변수(charset 등)는 비교에서 뺀다(HTML5 2014 는 매개변수까지 비교하라고 한다).
            if (semi >= 0) {
                t = t.substring(0, semi).trim();
            }
        } else if (lang != null && lang.trim().length() > 0) {
            t = "text/" + lang.trim();
        } else {
            t = "text/javascript";
        }
        t = t.toLowerCase(java.util.Locale.ENGLISH);
        // 알려진 JavaScript MIME 타입 목록과 비교한다.
        for (int i = 0; i < JS_TYPES.length; i++) {
            if (JS_TYPES[i].equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the <code>for</code> and <code>event</code> attributes of a script element let it run (HTML5 REC 2014-10-28 section
     * 4.11.1.1, https://www.w3.org/TR/2014/REC-html5-20141028/scripting-1.html#prepare-a-script, retrieved 2026-11-17): if the element has
     * both attributes, the script is the old form of an event handler of Internet Explorer and runs only if the <code>for</code>
     * attribute is <code>window</code> and the <code>event</code> attribute is <code>onload</code> or <code>onload()</code>, compared
     * without regard to case after the white space around them is removed. A script with only one of them is an ordinary script.
     * <p>
     * script 요소의 <code>for</code> 와 <code>event</code> 속성이 실행을 허용하는지 알려 준다(HTML5 REC 2014-10-28 §4.11.1.1): 두 속성이 모두 있으면 옛 Internet Explorer 의
     * 이벤트 핸들러 형태이므로 <code>for</code> 가 <code>window</code> 이고 <code>event</code> 가 <code>onload</code> 또는 <code>onload()</code> 일 때만 실행한다. 앞뒤 공백을
     * 없앤 뒤 대소문자를 구분하지 않고 비교한다. 둘 중 하나만 있는 스크립트는 일반 스크립트이다.
     *
     * @param script the script element / script 요소
     * @return true if the script may run / 실행해도 되면 true
     */
    static boolean allowedByEventAttributes(Element script) {
        if (!script.hasAttribute("for") || !script.hasAttribute("event")) {
            return true;
        }
        String target = script.getAttribute("for").trim();
        String event = script.getAttribute("event").trim();
        return "window".equalsIgnoreCase(target) && ("onload".equalsIgnoreCase(event) || "onload()".equalsIgnoreCase(event));
    }

    /**
     * Runs an external script whose fetch has finished: a failure fires <code>error</code> on the element, success runs the text and fires
     * <code>load</code>. Call it on the thread that owns the page's DOM only after {@link #awaitLoad(ScriptLoads.Load)} said true.
     * <p>
     * 가져오기가 끝난 외부 스크립트를 실행한다. 실패하면 요소에 <code>error</code>, 성공하면 텍스트를 실행하고 <code>load</code> 를 보낸다.
     * {@link #awaitLoad(ScriptLoads.Load)} 가 true 를 돌려준 뒤에 페이지 DOM 의 소유 스레드에서만 부른다.
     *
     * @param p       the finished external script / 가져오기가 끝난 외부 스크립트
     * @param control the parser control or null / 파서 제어 또는 null
     */
    private void runFetched(Pending p, ParserControl control) {
        ScriptText fetched;
        try {
            fetched = loads.result(p.load);
        } catch (IOException e) {
            if (!scripting()) {
                return;
            }
            report(ConsoleRecord.Kind.HOST_ERROR, "Failed to load script " + p.url + ": " + e.getMessage(), p.url, 0, 0);
            fireScriptEvent(p.element, "error");
            return;
        }
        if (scripting()) {
            // 모듈의 상대 import 는 요청한 URL이 아니라 리디렉션 뒤의 최종 URL을 기준으로 해석한다.
            if (p.module) {
                moduleBases.put(p.url, fetched.getUrl());
                moduleTexts.put(p.url, fetched.getText());
            }
            boolean ok = execute(p.element, fetched.getText(), p.url, p.module, control);
            if (scripting()) {
                // 모듈 그래프를 가져오거나 파싱하지 못했으면 load 가 아니라 error 이다. 평가 중의 오류는 보고만 하고 load 를 보낸다.
                fireScriptEvent(p.element, ok ? "load" : "error");
            }
        }
    }

    /**
     * Waits for a fetch to finish. While it waits, async scripts that finished in the meantime run (they may, since the parser is idle), so a
     * slow parser-blocking script never holds back a ready async script. Returns false if scripts were disabled or the page was disposed
     * while waiting.
     * <p>
     * 가져오기가 끝나기를 기다린다. 기다리는 동안 그 사이 끝난 async 스크립트를 실행한다(파서가 쉬는 중이므로 가능하다). 따라서 느린 파서
     * 차단 스크립트가 준비된 async 스크립트를 붙잡지 않는다. 기다리는 동안 스크립트가 비활성화되거나 페이지가 폐기되면 false 를 반환한다.
     *
     * @param load the fetch / 가져오기
     * @return true if the fetch is done and scripts may still run / 가져오기가 끝났고 스크립트를 계속 실행할 수 있으면 true
     */
    private boolean awaitLoad(ScriptLoads.Load load) {
        // 가져오기가 끝나거나 스크립트를 더 실행할 수 없을 때까지 준비된 async 를 실행하며 기다린다.
        while (scripting()) {
            runReadyAsync();
            if (loads.isDone(load)) {
                return scripting();
            }
            if (!waitForProgress(load)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Waits a short time for progress of the loads (a completion, a ready async script, or the end of the page). A bounded wait keeps the
     * thread responsive to flags that change without a notification.
     * <p>
     * 로드의 진행(완료, 준비된 async 스크립트, 페이지의 끝)을 잠시 기다린다. 대기 시간에 상한을 두어 알림 없이 바뀌는 플래그에도 스레드가 반응한다.
     *
     * @param load the load to wait for, or null / 기다릴 로드. 없으면 null
     * @return false if the thread was interrupted / 스레드가 인터럽트되었으면 false
     */
    private boolean waitForProgress(ScriptLoads.Load load) {
        try {
            loads.awaitProgress(load, 100);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Runs the async scripts whose fetch finished, in the order of their completion. Call it on the thread that owns the page's DOM at a
     * task boundary only (the parser between two scripts, a wait, or the end of parsing), never while another script of the page runs.
     * <p>
     * 가져오기가 끝난 async 스크립트를 완료 순서대로 실행한다. 페이지 DOM 의 소유 스레드에서 작업 경계(두 스크립트 사이의 파서, 대기, 파싱 끝)에서만
     * 부르며, 같은 페이지의 다른 스크립트가 실행되는 동안에는 부르지 않는다.
     */
    private void runReadyAsync() {
        ScriptLoads.Load ready;
        // 끝난 async 로드를 하나씩 꺼내 실행한다. 실행 중 다른 async 가 끝날 수 있어 비어 있을 때까지 되풀이한다.
        while ((ready = loads.pollReady()) != null) {
            // 이미지 로드의 결과는 요소의 load/error 이벤트로 전달한다.
            if (ready.tag instanceof ImageLoads.Task) {
                images.deliver((ImageLoads.Task) ready.tag);
                continue;
            }
            // 미디어 요소의 가져오기 결과, 재생기 보고, 올려 둔 이벤트는 미디어 상태 기계가 처리한다.
            if (ready.tag instanceof MediaElements.Task) {
                media.deliver((MediaElements.Task) ready.tag);
                continue;
            }
            Pending p = (Pending) ready.tag;
            if (p.ordered) {
                // 순서 있는 스크립트는 가져오기가 끝났다고 표시만 하고, 맨 앞부터 연속으로 끝난 것을 삽입 순서대로 실행한다.
                p.ready = true;
                runOrderedHeads();
            } else {
                asyncScripts.remove(p);
                runPending(p, null);
            }
        }
    }

    /**
     * Runs the dynamically inserted ordered scripts from the head of the list as long as the head has been fetched.
     * <p>
     * 동적으로 삽입한 순서 있는 스크립트를, 맨 앞 스크립트의 가져오기가 끝나 있는 동안 목록의 맨 앞부터 실행한다.
     */
    private void runOrderedHeads() {
        // 맨 앞 스크립트가 준비되어 있는 동안 하나씩 꺼내 실행한다. 아직 가져오는 중이면 뒤의 것이 끝났어도 기다린다.
        while (!orderedScripts.isEmpty() && orderedScripts.get(0).ready) {
            runPending(orderedScripts.remove(0), null);
        }
    }

    /**
     * Prepares the script elements of an inserted subtree ("prepare a script" for scripts that were not created by the parser): an inline
     * script runs at once, an external script is fetched at once and runs as soon as it has been fetched (async), or after the scripts
     * inserted before it (when <code>script.async = false</code> was assigned). Scripts that the parser created or that have already
     * started never run twice.
     * <p>
     * 삽입된 하위 트리의 script 요소를 준비한다(파서가 만들지 않은 스크립트의 "prepare a script"): 인라인 스크립트는 즉시 실행하고, 외부
     * 스크립트는 즉시 가져오기를 시작하여 가져오는 대로(async) 또는 먼저 삽입된 스크립트 뒤에(<code>script.async = false</code> 를 지정한 경우)
     * 실행한다. 파서가 만들었거나 이미 시작한 스크립트는 두 번 실행되지 않는다.
     *
     * @param root the root of the inserted subtree / 삽입된 하위 트리의 루트
     */
    private void scriptsInserted(Node root) {
        // 삽입된 하위 트리를 깊이 우선으로 돌며 script 요소를 찾아 준비한다.
        for (Node n = root; n != null; n = n.nextInTree(n, root)) {
            if (n instanceof Element && "script".equals(((Element) n).getLocalName())
                    && Namespaces.HTML.equals(((Element) n).getNamespaceURI())) {
                prepareDynamic((Element) n);
            }
        }
    }

    /**
     * Prepares one script element that was inserted into the connected document.
     * <p>
     * 연결된 문서에 삽입된 script 요소 하나를 준비한다.
     *
     * @param script the element / 요소
     */
    private void prepareDynamic(Element script) {
        if (!scripting()) {
            return;
        }
        ParserNodeData.ScriptFlags flags = ParserNodeData.scriptFlags(script);
        if (flags.isAlreadyStarted() || flags.isParserInserted()) {
            return;
        }
        String type = script.getAttribute("type");
        boolean module = type != null && "module".equals(type.trim().toLowerCase());
        if (!module && !isJavaScriptType(type, script.getAttribute("language"))) {
            return;
        }
        if (!module && script.hasAttribute("nomodule")) {
            return;
        }
        // for 와 event 속성이 있는 스크립트는 window 와 onload 일 때만 실행한다(HTML5 2014 §4.11.1.1).
        if (!allowedByEventAttributes(script)) {
            flags.setAlreadyStarted(true);
            return;
        }
        String src = script.getAttribute("src");
        if (src != null && src.trim().length() > 0) {
            String url;
            try {
                url = UrlUtil.resolve(document.getBaseUri(), src.trim());
            } catch (RuntimeException e) {
                flags.setAlreadyStarted(true);
                fireScriptEvent(script, "error");
                return;
            }
            flags.setAlreadyStarted(true);
            Pending p = new Pending(script, url, null, module);
            p.load = loads.start(env.getScriptSource(), url, p, true, true, module, useCredentials(script));
            if (script.hasAttribute("async") || flags.isForceAsync()) {
                // async 이거나 스크립트가 만든 스크립트는 가져오는 대로 실행한다.
                asyncScripts.add(p);
            } else {
                // script.async = false 로 순서를 요청한 스크립트는 삽입한 순서를 지켜 실행한다.
                p.ordered = true;
                orderedScripts.add(p);
            }
            return;
        }
        String text = script.getTextContent();
        if (text.length() == 0) {
            return;
        }
        flags.setAlreadyStarted(true);
        executeInline(script, text, module, null);
    }

    /**
     * Executes the text of an inline script. An inline module is a module of its own, with a key of its own and the document base URL
     * as the base of its imports.
     * <p>
     * 인라인 스크립트의 텍스트를 실행한다. 인라인 모듈은 자기 키를 가진 별개의 모듈이며 import 의 기준은 문서 기준 URL이다.
     *
     * @param script  the element / 요소
     * @param text    the script text / 스크립트 텍스트
     * @param module  true for a module / 모듈이면 true
     * @param control the parser control or null / 파서 제어 또는 null
     * @return false if the module graph could not be loaded / 모듈 그래프를 불러올 수 없었으면 false
     */
    private boolean executeInline(Element script, String text, boolean module, ParserControl control) {
        if (!module) {
            return execute(script, text, scriptName(script), false, control);
        }
        String key = document.getUrl() + "#inline-module-" + (++inlineModules);
        moduleBases.put(key, document.getBaseUri());
        boolean ok = execute(script, text, key, true, control);
        if (!ok && scripting()) {
            fireScriptEvent(script, "error");
        }
        return ok;
    }

    /**
     * Tells whether a script asks for cookies on every origin: <code>crossorigin="use-credentials"</code>.
     * <p>
     * 스크립트가 모든 오리진에 대한 쿠키를 요청하는지 알려 준다: <code>crossorigin="use-credentials"</code>.
     *
     * @param script the element / 요소
     * @return true for use-credentials / use-credentials 이면 true
     */
    private static boolean useCredentials(Element script) {
        String value = script.getAttribute("crossorigin");
        return value != null && "use-credentials".equals(value.trim().toLowerCase());
    }

    /**
     * Fires a simple event at a script element (load or error), outside the page lock rules of the parser callbacks.
     * <p>
     * script 요소에 단순 이벤트(load 또는 error)를 보낸다.
     *
     * @param script the element / 요소
     * @param type   the event type / 이벤트 종류
     */
    private void fireScriptEvent(Element script, String type) {
        try {
            script.dispatchEvent(new Event(type, false, false));
        } catch (RuntimeException e) {
            reportError(e);
        }
    }

    /**
     * Executes a script text under the page lock with <code>document.currentScript</code> set.
     * <p>
     * 페이지 잠금 아래에서 <code>document.currentScript</code> 를 지정해 스크립트 텍스트를 실행한다.
     *
     * @param script  the element / 요소
     * @param code    the text / 텍스트
     * @param name    the name for messages (the URL for modules) / 메시지용 이름(모듈이면 URL)
     * @param module  module flag / 모듈 여부
     * @param control the parser control or null / 파서 제어 또는 null
     * @return false if a module graph could not be fetched or parsed (nothing ran), true otherwise (also when the script threw) / 모듈 그래프를
     *         가져오거나 파싱하지 못해 아무것도 실행되지 않았으면 false, 그 밖에는(스크립트가 예외를 던져도) true
     */
    private boolean execute(Element script, String code, String name, boolean module, ParserControl control) {
        boolean ok = true;
        String src = script.getAttribute("src");
        boolean external = src != null && src.trim().length() > 0;
        // 파서 생성 일반·defer 스크립트만 선행 CSS를 기다리며 네트워크 대기는 실행 시간 한도에 포함하지 않는다.
        if (!module && ParserNodeData.scriptFlags(script).isParserInserted()
                && (!external || !script.hasAttribute("async")) && scripting()) {
            env.prepareParserScript(script);
        }
        lock.lock();
        boolean outermost = markEntry();
        Element prevScript = currentScript;
        ParserControl prevControl = currentControl;
        try {
            depth++;
            if (disposed) {
                return true;
            }
            currentScript = module ? null : script;
            // 다른 스크립트 안에서 중첩 실행되는 스크립트(동적으로 삽입한 인라인 스크립트)는 바깥 스크립트의 파서 제어를 이어받는다.
            if (control != null) {
                currentControl = control;
            }
            realm.getLimits().beginTask();
            try {
                if (module) {
                    // 그래프를 먼저 가져와 파싱한 뒤 평가한다. 가져오기나 구문 오류는 error 이벤트이고 평가 중의 오류는 보고만 한다.
                    moduleCredentials = useCredentials(script);
                    ModuleGraph graph = null;
                    try {
                        graph = realm.loadModule(name, code);
                    } catch (JsException e) {
                        ok = false;
                        reportError(e);
                    }
                    if (graph != null) {
                        graph.evaluate();
                    }
                } else {
                    realm.evaluateScript(code, name);
                }
            } catch (JsException e) {
                reportError(e);
            } catch (ScriptAbortedException e) {
                reportAbort(e);
            } finally {
                realm.getLimits().endTask();
                currentScript = prevScript;
                currentControl = prevControl;
            }
            // 파서를 막은 스크립트가 끝날 때 한 번에 파서에 넣는다. 중첩 실행한 스크립트는 바깥 스크립트가 처리하도록 남겨 둔다.
            if (control != null && scriptWrites != null) {
                String written = scriptWrites.toString();
                scriptWrites = null;
                control.insertAtInsertionPoint(written);
            }
            // 가장 바깥 스크립트가 끝날 때만 마이크로태스크 검사점을 둔다. 안쪽 스크립트가 도는 동안은 바깥 스크립트가 호출 스택에 있다.
            if (depth == 1) {
                try {
                    realm.getLimits().beginTask();
                    realm.runJobs();
                    observers.deliver();
                } catch (JsException e) {
                    reportError(e);
                } catch (ScriptAbortedException e) {
                    reportAbort(e);
                } finally {
                    realm.getLimits().endTask();
                }
                // 이 검사점이 끝날 때까지 처리기를 얻지 못한 거부된 promise 를 보고한다.
                reportUnhandledRejections();
            }
        } finally {
            depth--;
            clearEntry(outermost);
            lock.unlock();
        }
        return ok;
    }

    /**
     * Finishes loading after the parser ended: <code>readyState</code> becomes interactive, deferred and module scripts run in order,
     * <code>DOMContentLoaded</code> is fired, async scripts run, then <code>readyState</code> becomes complete and <code>load</code> is fired
     * at the window. Call it on the thread that parsed the page.
     * <p>
     * 파서가 끝난 뒤 로딩을 마무리한다: <code>readyState</code> 가 interactive 가 되고 defer 와 module 스크립트가 순서대로 실행되며
     * <code>DOMContentLoaded</code> 가 발생하고 async 스크립트가 실행된 뒤 <code>readyState</code> 가 complete 가 되고 window 에
     * <code>load</code> 가 발생한다. 페이지를 해석한 스레드에서 부른다.
     */
    public void completeParsing() {
        parsing = false;
        if (disposed) {
            return;
        }
        // 파싱이 끝나기를 기다리던 미디어 요소의 자동 텍스트 트랙 선택을 시작한다.
        media.textTracks().parsingFinished();
        setReadyState("interactive");
        // 해석 중인 문서의 meta http-equiv=refresh 는 파서가 알리고 HtmlPageDocument 가 시작한다(core.MetaRefresh, 스크립트와 무관). 여기서는 해석 뒤에 삽입된 것만 다룬다.
        List<Pending> list = new ArrayList<Pending>(deferred);
        deferred.clear();
        // defer 와 module 스크립트를 문서 순서대로 실행한다. 각 스크립트의 가져오기는 이미 병렬로 진행 중이다.
        for (Pending p : list) {
            if (p.load != null && !awaitLoad(p.load)) {
                continue;
            }
            runPending(p, null);
        }
        reached(PageTimeline.DCL_START);
        fire(document, new Event("DOMContentLoaded", true, false));
        reached(PageTimeline.DCL_END);
        loadStage = true;
        Scheduler queue = env.getScheduler();
        if (queue == null) {
            // 작업 큐가 없는 호스트(시험)는 이 스레드가 남은 스크립트를 기다린 뒤 load 를 보낸다.
            while (pendingExternal() && scripting()) {
                runReadyAsync();
                if (pendingExternal() && scripting() && !waitForProgress(null)) {
                    break;
                }
            }
        } else {
            // DOMContentLoaded 는 async 스크립트를 기다리지 않았고 load 도 이 스레드를 붙잡지 않는다. 남은 스크립트의 전달은 문서 작업 큐로
            // 넘기고, 페이지는 먼저 확정되어 화면에 나타난다. 가져오기가 끝나는 대로 큐에서 실행하고 마지막 것이 끝나면 load 를 보낸다.
            loads.setNotifier(new Runnable() {
                /**
                 * Posts a drain task to the document queue (runs on the fetch thread and only posts).
                 * <p>
                 * 문서 큐에 전달 작업을 올린다(가져오기 스레드에서 실행되며 올리기만 한다).
                 */
                public void run() {
                    postDrain();
                }
            });
        }
        if (disposed) {
            return;
        }
        maybeFinishLoad();
    }

    /**
     * Tells whether an external script that was started is still waiting to run (async, dynamically inserted or in-order scripts).
     * <p>
     * 시작한 외부 스크립트가 아직 실행을 기다리는지 알려 준다(async, 동적으로 삽입한 스크립트, 순서 있는 스크립트).
     *
     * @return true if one is waiting / 기다리는 것이 있으면 true
     */
    private boolean pendingExternal() {
        // 컴포넌트가 시작한 스타일시트와 이미지도 load 이벤트를 늦춘다(성공하든 실패하든 끝나면 풀린다).
        return !asyncScripts.isEmpty() || !orderedScripts.isEmpty() || images.isDelayingLoad() || env.hasPendingResources();
    }

    /**
     * Fires the load event once everything that the page waits for has finished: no script is waiting any more and the document stage after
     * <code>DOMContentLoaded</code> has been reached. It makes <code>readyState</code> complete first. Runs at most once.
     * <p>
     * 페이지가 기다리는 모든 것이 끝나면 load 이벤트를 한 번 보낸다: 기다리는 스크립트가 없고 <code>DOMContentLoaded</code> 뒤의 단계에 이르렀을 때.
     * 먼저 <code>readyState</code> 를 complete 로 만든다. 많아야 한 번 실행된다.
     */
    private void maybeFinishLoad() {
        if (!loadStage || loadFired || pendingExternal() || media.isDelayingLoad() || disposed) {
            return;
        }
        loadFired = true;
        setReadyState("complete");
        reached(PageTimeline.LOAD_START);
        fire(window, new Event("load", false, false));
        reached(PageTimeline.LOAD_END);
        // load 뒤에 pageshow 를 보낸다(뒤로/앞으로 이동 캐시가 없으므로 persisted 는 항상 false).
        pageShown = true;
        fire(window, new PageTransitionEvent("pageshow", false, false));
        // 로딩 중에 호스트의 가시성이나 네트워크 상태가 바뀌었으면(그때는 알릴 수 없었다) 이제 알린다.
        lifecycle.visibilityChanged();
        lifecycle.onlineChanged();
        // load 이벤트의 핸들러까지 끝난 뒤 컴포넌트가 로딩 진행률을 마무리하게 한다.
        env.pageLoaded();
    }

    /**
     * Tells the page that a sub-resource of the component (a stylesheet or an image) ended, so that the window <code>load</code> event can
     * fire when it was the last one that the page waited for. It only posts a task to the document queue and may be called from any thread.
     * <p>
     * 컴포넌트의 하위 리소스(스타일시트나 이미지)가 끝났음을 페이지에 알려, 그것이 페이지가 기다리던 마지막이었다면 window <code>load</code>
     * 이벤트가 발생할 수 있게 한다. 문서 큐에 작업만 올리며 어느 스레드에서나 부를 수 있다.
     */
    public void resourcesChanged() {
        // 문서를 아직 파싱 중이면 파서가 끝날 때 확인하므로 확정 단계 이후에만 전달 작업을 올린다.
        if (loadStage && !loadFired) {
            postDrain();
        }
    }

    /**
     * Posts a task to the document queue that runs the scripts which finished loading and then the load event if nothing more is awaited.
     * Callable from any thread.
     * <p>
     * 가져오기가 끝난 스크립트를 실행하고 기다리는 것이 더 없으면 load 이벤트를 보내는 작업을 문서 큐에 올린다. 어느 스레드에서나 부를 수 있다.
     */
    private void postDrain() {
        Scheduler queue = env.getScheduler();
        if (queue == null || disposed) {
            return;
        }
        try {
            queue.post(new Runnable() {
                /**
                 * Runs the finished scripts on the document queue.
                 * <p>
                 * 끝난 스크립트를 문서 큐에서 실행한다.
                 */
                public void run() {
                    if (scripting()) {
                        runReadyAsync();
                        maybeFinishLoad();
                    }
                }
            }, 0);
        } catch (RuntimeException e) {
            // 대기 작업 한도를 넘었거나 큐가 닫혔으면 더 올릴 수 없다. 이 페이지의 대기 스크립트는 실행되지 않는다.
            report(ConsoleRecord.Kind.HOST_ERROR, "Cannot schedule script: " + e.getMessage(), "", 0, 0);
        }
    }

    /**
     * Runs one script whose text is available: an inline script, or an external script whose fetch has finished.
     * <p>
     * 텍스트를 쓸 수 있는 스크립트 하나를 실행한다: 인라인 스크립트나 가져오기가 끝난 외부 스크립트.
     *
     * @param p       the entry / 항목
     * @param control the parser control or null / 파서 제어 또는 null
     */
    private void runPending(Pending p, ParserControl control) {
        if (!scripting()) {
            return;
        }
        if (p.url != null) {
            runFetched(p, control);
        } else {
            executeInline(p.element, p.text, p.module, control);
        }
    }

    /**
     * Changes the ready state and fires <code>readystatechange</code>.
     * <p>
     * 준비 상태를 바꾸고 <code>readystatechange</code> 를 발생시킨다.
     *
     * @param state the new state / 새 상태
     */
    private void setReadyState(String state) {
        readyState = state;
        // 문서가 해석되었거나(interactive) 모두 끝났을 때(complete)의 시각을 기록한다.
        if ("interactive".equals(state)) {
            reached(PageTimeline.DOM_INTERACTIVE);
        } else if ("complete".equals(state)) {
            reached(PageTimeline.DOM_COMPLETE);
        }
        fire(document, new Event("readystatechange", false, false));
    }

    /**
     * Dispatches an event under the page lock without a microtask checkpoint of its own beyond the normal one.
     * <p>
     * 페이지 잠금 아래에서 이벤트를 전달한다.
     *
     * @param target the target / 대상
     * @param event  the event / 이벤트
     */
    private void fire(final EventTarget target, final Event event) {
        lock.lock();
        boolean outermost = markEntry();
        try {
            depth++;
            if (disposed) {
                return;
            }
            realm.getLimits().beginTask();
            try {
                target.dispatchEvent(event);
                if (depth == 1) {
                    realm.runJobs();
                    observers.deliver();
                }
            } catch (JsException e) {
                reportError(e);
            } catch (ScriptAbortedException e) {
                reportAbort(e);
            } finally {
                realm.getLimits().endTask();
            }
        } finally {
            depth--;
            clearEntry(outermost);
            lock.unlock();
        }
    }

    /**
     * Implements <code>document.write</code>: while the parser runs the text goes to the parser at its insertion point, afterwards the
     * document is reopened (implicit <code>document.open()</code>) and the text is collected until <code>close()</code> or the end of the task.
     * <p>
     * <code>document.write</code> 를 구현한다: 파서가 도는 동안은 텍스트가 파서의 삽입 지점으로 가고, 그 뒤에는 문서를 다시 열고(암묵적
     * <code>document.open()</code>) <code>close()</code> 나 작업이 끝날 때까지 텍스트를 모은다.
     *
     * @param text the text / 텍스트
     */
    void write(String text) {
        if (parsing && currentControl != null) {
            // 스크립트가 끝날 때 한 번에 파서에 넣어야 여러 번 쓴 텍스트가 쓴 순서대로 놓인다.
            if (scriptWrites == null) {
                scriptWrites = new StringBuilder();
            }
            scriptWrites.append(text);
            return;
        }
        if (parsing) {
            return;
        }
        if (writeBuffer == null) {
            open();
        }
        writeBuffer.append(text);
    }

    /**
     * Implements <code>document.open()</code>: removes the content of the document and starts collecting written text.
     * <p>
     * <code>document.open()</code> 을 구현한다: 문서의 내용을 지우고 쓰인 텍스트를 모으기 시작한다.
     */
    void open() {
        if (parsing) {
            return;
        }
        // 문서의 모든 자식을 지워 빈 문서로 만든다.
        while (document.getFirstChild() != null) {
            document.removeChild(document.getFirstChild());
        }
        writeBuffer = new StringBuilder();
        readyState = "loading";
    }

    /**
     * Implements <code>document.close()</code>: parses the collected text into the document.
     * <p>
     * <code>document.close()</code> 를 구현한다: 모은 텍스트를 문서로 해석한다.
     */
    void close() {
        flushWrite();
    }

    /**
     * Parses the text collected by <code>document.write</code> into the (emptied) document.
     * <p>
     * <code>document.write</code> 로 모은 텍스트를 (비운) 문서로 해석한다.
     *
     * @return true if text was parsed / 텍스트를 해석했으면 true
     */
    private boolean flushWrite() {
        if (writeBuffer == null) {
            return false;
        }
        String text = writeBuffer.toString();
        writeBuffer = null;
        HtmlParser parser = new HtmlParser(document, text);
        parser.setScripting(scripting());
        parser.setListener(createParserListener());
        parsing = true;
        try {
            parser.run();
        } finally {
            parsing = false;
        }
        readyState = "interactive";
        readyState = "complete";
        return true;
    }

    /**
     * Activates the event handler content attributes that a mutation introduced (<code>onclick="..."</code>).
     * <p>
     * 변경이 도입한 이벤트 핸들러 콘텐츠 속성(<code>onclick="..."</code>)을 활성화한다.
     *
     * @param r the change / 변경
     */
    private void handlerAttributes(MutationRecord r) {
        if (r.getType() == MutationRecord.ATTRIBUTES && r.getTarget() instanceof Element) {
            String name = r.getAttributeName();
            if (name != null && name.startsWith("on")) {
                activateHandler((Element) r.getTarget(), name);
            }
        } else if (r.getType() == MutationRecord.CHILD_LIST) {
            List<Node> added = r.getAddedNodes();
            // 추가된 노드의 하위 트리에 있는 핸들러 속성을 찾아 활성화한다.
            for (int i = 0; i < added.size(); i++) {
                scanHandlers(added.get(i));
            }
        }
    }

    /**
     * Activates the handler attributes in a subtree.
     * <p>
     * 하위 트리의 핸들러 속성을 활성화한다.
     *
     * @param root the root of the subtree / 하위 트리의 루트
     */
    public void scanHandlers(Node root) {
        // 하위 트리를 깊이 우선으로 돌며 요소의 on... 속성을 활성화한다.
        for (Node n = root; n != null; n = n.nextInTree(n, root)) {
            if (n instanceof Element) {
                Element e = (Element) n;
                List<String> names = e.getAttributeNames();
                // 요소의 속성 이름 중 on 으로 시작하는 것을 모두 활성화한다.
                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).startsWith("on")) {
                        activateHandler(e, names.get(i));
                    }
                }
            }
        }
    }

    /**
     * Activates or clears one event handler content attribute. The function is compiled the first time the event arrives, with the scope
     * chain element, form owner, document, global (as the HTML standard requires). On the body element the window-level events are handled
     * by the window.
     * <p>
     * 이벤트 핸들러 콘텐츠 속성 하나를 활성화하거나 지운다. 함수는 이벤트가 처음 올 때 요소, 폼 소유자, 문서, 전역의 범위 사슬로 컴파일한다
     * (HTML 표준 요구). body 요소의 창 수준 이벤트는 window 가 처리한다.
     *
     * @param element the element / 요소
     * @param attr    the attribute name such as <code>onclick</code> / <code>onclick</code> 같은 속성 이름
     */
    private void activateHandler(final Element element, String attr) {
        final String type = attr.substring(2);
        if (type.length() == 0) {
            return;
        }
        EventTarget target = element;
        String local = element.getLocalName();
        boolean windowLevel = ("body".equals(local) || "frameset".equals(local)) && isWindowEvent(type);
        if (windowLevel) {
            target = window;
        }
        final String code = element.getAttribute(attr);
        HandlerListener h = bridge.getEventHandlers().handler(target, type, code != null);
        if (h == null) {
            return;
        }
        if (code == null) {
            h.setCallback(null);
            return;
        }
        final String source = code;
        h.setCompiler(new HandlerListener.Compiler() {
            /**
             * Compiles the attribute text into a function.
             * <p>
             * 속성 텍스트를 함수로 컴파일한다.
             *
             * @return the function or null / 함수 또는 null
             */
            public JsObject compile() {
                return compileHandler(element, source);
            }
        });
    }

    /**
     * Tells whether an event type belongs to the window-level handlers of the body element.
     * <p>
     * 이벤트 종류가 body 요소의 창 수준 핸들러에 속하는지 알려 준다.
     *
     * @param type the event type / 이벤트 종류
     * @return true for window-level events / 창 수준 이벤트이면 true
     */
    private static boolean isWindowEvent(String type) {
        String[] extra = { "load", "blur", "focus", "resize", "scroll", "error" };
        // 창에만 있는 이벤트 목록과 body 가 전달하는 목록을 차례로 확인한다.
        for (int i = 0; i < org.duckdns.hjow.swingwebpanel.js.dom.EventHandlers.WINDOW_EVENTS.length; i++) {
            if (org.duckdns.hjow.swingwebpanel.js.dom.EventHandlers.WINDOW_EVENTS[i].equals(type)) {
                return true;
            }
        }
        // 이벤트 종류가 body 가 전달하는 추가 목록에 있는지 확인한다.
        for (int i = 0; i < extra.length; i++) {
            if (extra[i].equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compiles the text of an event handler attribute with the scope chain of the HTML standard.
     * <p>
     * 이벤트 핸들러 속성의 텍스트를 HTML 표준의 범위 사슬로 컴파일한다.
     *
     * @param element the element / 요소
     * @param body    the attribute text / 속성 텍스트
     * @return the function or null if the text does not compile / 함수. 컴파일되지 않으면 null
     */
    private JsObject compileHandler(Element element, String body) {
        if (!scripting()) {
            return null;
        }
        lock.lock();
        try {
            realm.getLimits().beginTask();
            try {
                JsValue factory = realm.evaluateScript("(function(__d,__f,__e){return function(event){with(__d){with(__f){with(__e){\n" + body
                        + "\n}}}};})", "handler");
                Element form = ParserNodeData.getFormOwner(element);
                JsValue fn = Ops.call(factory, Js.UNDEFINED, new JsValue[] { bridge.toJs(document),
                    form == null ? realm.newObject() : bridge.toJs(form), bridge.toJs(element) });
                return fn.isObject() ? (JsObject) fn : null;
            } catch (JsException e) {
                reportError(e);
                return null;
            } catch (ScriptAbortedException e) {
                reportAbort(e);
                return null;
            } finally {
                realm.getLimits().endTask();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Requests disposal without taking the page lock: running code notices cancellation and no new page task starts.
     * The host can call this from the EDT, then call {@link #dispose()} on a cleanup worker to release timers, listeners
     * and wrappers. This method alone is not full resource cleanup. Repeated requests are safe.
     * <p>
     * 페이지 잠금을 얻지 않고 폐기를 요청한다. 실행 중인 코드는 취소를 감지하며 새 페이지 작업은 시작되지 않는다. 호스트는 EDT에서 요청한 뒤
     * 정리 작업자에서 {@link #dispose()}를 호출하여 타이머·리스너·래퍼를 해제한다. 이 메서드만으로 전체 자원 정리가 끝나지는 않는다. 반복 요청은 안전하다.
     */
    public void requestDisposal() {
        disposed = true;
        loads.close();
        bridge.stopEventExecution();
        realm.getLimits().cancel();
        globals.cancelRequests();
        // 페이지가 교체되거나 닫히면 소리부터 즉시 끈다(재생기의 일시정지는 기다리지 않는다).
        media.stopSoundNow();
    }

    /**
     * Disposes the page's scripts: every timer and frame callback is cancelled, running scripts are aborted, observers and wrappers are
     * dropped and storage listeners are removed. Afterwards nothing runs, even tasks that were already handed to the scheduler.
     * <p>
     * 페이지의 스크립트를 폐기한다: 모든 타이머와 프레임 콜백을 취소하고 실행 중인 스크립트를 중단하며 관찰자와 래퍼를 버리고 저장소 리스너를 제거한다.
     * 그 뒤에는 이미 스케줄러에 넘긴 작업도 실행되지 않는다.
     */
    public void dispose() {
        requestDisposal();
        lock.lock();
        try {
            timers.dispose();
            observers.dispose();
            rendering.dispose();
            globals.dispose();
            deferred.clear();
            asyncScripts.clear();
            orderedScripts.clear();
            images.dispose();
            media.dispose();
            canvases.dispose();
            document.removeMutationListener(mutationListener);
            bridge.dispose();
        } finally {
            lock.unlock();
        }
    }
}
