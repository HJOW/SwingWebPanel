package org.duckdns.hjow.swingwebpanel.js.host;

import java.net.URI;
import java.net.URISyntaxException;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.dom.EventHandlers;
import org.duckdns.hjow.swingwebpanel.js.host.web.ClipboardApi;
import org.duckdns.hjow.swingwebpanel.js.host.web.WebApis;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.storage.StorageEvent;
import org.duckdns.hjow.swingwebpanel.storage.StorageListener;
import org.duckdns.hjow.swingwebpanel.storage.WebStorage;

/**
 * Installs the browser objects of one page into its realm (plan items P15-01, P15-08 (hooks), P15-09, P16-01, P16-08): the
 * <code>window</code> properties of the global object, <code>location</code>, <code>navigator</code>, <code>history</code>,
 * <code>screen</code>, <code>performance</code>, <code>atob</code>/<code>btoa</code>, the dialog functions, the document members that depend on
 * the page (<code>cookie</code>, <code>readyState</code>, <code>write</code> ...) and <code>localStorage</code>/<code>sessionStorage</code> with
 * their <code>storage</code> events. Navigation requests go to the {@link HostEnvironment}; the page never navigates by itself. Nothing of
 * the Java side is reachable, and <code>navigator.gpu</code>, WebGL and WebGPU objects do not exist (project requirement).
 * <p>
 * 한 페이지의 브라우저 객체를 realm 에 설치한다(계획 P15-01, P15-08(훅), P15-09, P16-01, P16-08): 전역 객체의 <code>window</code> 프로퍼티,
 * <code>location</code>, <code>navigator</code>, <code>history</code>, <code>screen</code>, <code>performance</code>,
 * <code>atob</code>/<code>btoa</code>, 대화 상자 함수, 페이지에 의존하는 document 멤버(<code>cookie</code>, <code>readyState</code>,
 * <code>write</code> 등), <code>localStorage</code>/<code>sessionStorage</code> 와 <code>storage</code> 이벤트. 이동 요청은
 * {@link HostEnvironment} 로 가며 페이지가 스스로 이동하지 않는다. Java 쪽은 전혀 닿지 않으며 <code>navigator.gpu</code>, WebGL, WebGPU 객체는
 * 존재하지 않는다(프로젝트 요구).
 */
final class WindowGlobals {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The handler of <code>window.onerror</code>, or null.
     * <p>
     * <code>window.onerror</code> 핸들러. 없으면 null.
     */
    private JsObject onerror;

    /**
     * The <code>location</code> object.
     * <p>
     * <code>location</code> 객체.
     */
    private JsObject location;

    /**
     * The <code>history</code> object (HTML5 2014 REC 5.5.2).
     * <p>
     * <code>history</code> 객체(HTML5 2014 REC 5.5.2).
     */
    private HistoryApi history;

    /**
     * The storage views of this document (created at first use so that the same object is returned every time).
     * <p>
     * 이 문서의 저장소 뷰(처음 쓸 때 만들어 같은 객체를 매번 반환한다).
     */
    private WebStorage localView;

    /**
     * The sessionStorage view of this document.
     * <p>
     * 이 문서의 sessionStorage 뷰.
     */
    private WebStorage sessionView;

    /**
     * The listener for localStorage changes of other documents.
     * <p>
     * 다른 문서의 localStorage 변경을 듣는 리스너.
     */
    private StorageListener localListener;

    /**
     * The web request APIs of the page (XMLHttpRequest, fetch and their helpers), installed with the window.
     * <p>
     * 페이지의 웹 요청 API(XMLHttpRequest, fetch와 그 도우미). window 와 함께 설치된다.
     */
    private WebApis web;

    /**
     * The media query lists of the page (<code>window.matchMedia</code>), installed with the window.
     * <p>
     * 페이지의 미디어 질의 목록(<code>window.matchMedia</code>). window 와 함께 설치된다.
     */
    private MediaQueries mediaQueries;

    /**
     * The members of the elements with a state (dialog, details, track, object), installed with the window.
     * <p>
     * 상태가 있는 요소(dialog, details, track, object)의 멤버. window 와 함께 설치된다.
     */
    private ElementStates elementStates;

    /**
     * Passes a change of the document to the parts that react to it (the <code>toggle</code> event of <code>details</code>).
     * <p>
     * 문서의 변경을 그에 반응하는 부분(<code>details</code> 의 <code>toggle</code> 이벤트)에 전달한다.
     *
     * @param record    the change / 변경
     * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
     */
    void mutated(org.duckdns.hjow.swingwebpanel.dom.MutationRecord record, boolean connected) {
        ElementStates s = elementStates;
        if (s != null) {
            s.mutated(record, connected);
        }
    }

    /**
     * The workers of the page (<code>Worker</code>, <code>BroadcastChannel</code>), installed with the window and terminated with the page.
     * <p>
     * 페이지의 worker(<code>Worker</code>, <code>BroadcastChannel</code>). window 와 함께 설치되고 페이지와 함께 종료된다.
     */
    private volatile org.duckdns.hjow.swingwebpanel.js.worker.WorkerApi workers;

    /**
     * The Web Animations of the page (<code>element.animate()</code>, P10-08-h), stopped when the scripts of the page stop.
     * <p>
     * 페이지의 Web Animations(<code>element.animate()</code>, P10-08-h). 페이지의 스크립트가 멈추면 함께 멈춘다.
     */
    private volatile org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi animations;

    /**
     * The Web Audio API of the page (<code>AudioContext</code> and the nodes, P25), closed when the scripts of the page stop.
     * <p>
     * 페이지의 Web Audio API(<code>AudioContext</code> 와 노드, P25). 페이지의 스크립트가 멈추면 함께 닫힌다.
     */
    private volatile org.duckdns.hjow.swingwebpanel.js.host.webaudio.WebAudioApi webAudio;

    /**
     * The listener for sessionStorage changes of other documents of the component.
     * <p>
     * 컴포넌트의 다른 문서의 sessionStorage 변경을 듣는 리스너.
     */
    private StorageListener sessionListener;

    /**
     * The messaging api of the page (<code>postMessage</code>, channels), kept so that other windows can post messages to this one.
     * <p>
     * 페이지의 메시징 API(<code>postMessage</code>, 채널). 다른 window 가 이 window 로 메시지를 보낼 수 있도록 보관한다.
     */
    private MessagingApi messaging;

    /**
     * The window relations of the page (parent, top, frames).
     * <p>
     * 페이지의 window 관계(parent, top, frames).
     */
    private FrameApi frames;

    /**
     * Returns the messaging api of the page.
     * <p>
     * 페이지의 메시징 API 를 반환한다.
     *
     * @return the api, null before the window was installed / API. window 를 설치하기 전에는 null
     */
    MessagingApi messaging() {
        return messaging;
    }

    /**
     * Returns the window relations of the page.
     * <p>
     * 페이지의 window 관계를 반환한다.
     *
     * @return the api, null before the window was installed / API. window 를 설치하기 전에는 null
     */
    FrameApi frameApi() {
        return frames;
    }

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    WindowGlobals(PageScripts page) {
        this.page = page;
    }

    /**
     * Defines a method on an object.
     * <p>
     * 객체에 메서드를 정의한다.
     *
     * @param o      the object / 객체
     * @param name   the name / 이름
     * @param length the declared length / 선언된 길이
     * @param call   the behaviour / 동작
     */
    private void method(JsObject o, String name, int length, NativeFunction.Call call) {
        o.defineData(name, page.getRealm().newFunction(name, length, call), true, true, true);
    }

    /**
     * Defines a read-only accessor.
     * <p>
     * 읽기 전용 접근자를 정의한다.
     *
     * @param o    the object / 객체
     * @param name the name / 이름
     * @param get  the getter / getter
     */
    private void getter(JsObject o, String name, NativeFunction.Call get) {
        o.defineAccessor(name, page.getRealm().newFunction("get " + name, 0, get), null, true, true);
    }

    /**
     * Defines a read-write accessor.
     * <p>
     * 읽기/쓰기 접근자를 정의한다.
     *
     * @param o    the object / 객체
     * @param name the name / 이름
     * @param get  the getter / getter
     * @param set  the setter / setter
     */
    private void accessor(JsObject o, String name, NativeFunction.Call get, NativeFunction.Call set) {
        JsRealm r = page.getRealm();
        o.defineAccessor(name, r.newFunction("get " + name, 0, get), r.newFunction("set " + name, 1, set), true, true);
    }

    /**
     * Installs everything.
     * <p>
     * 모두 설치한다.
     */
    void install() {
        final JsRealm realm = page.getRealm();
        final DomBridge bridge = page.getBridge();
        final JsObject g = realm.getGlobalObject();
        JsObject window = realm.getGlobalThisValue();
        bridge.alias(page.getWindow(), window);
        g.defineData("window", window, true, true, true);
        g.defineData("self", window, true, true, true);
        g.defineData("frames", window, true, true, true);
        g.defineData("document", bridge.toJs(page.getDocument()), true, true, true);
        g.defineData("closed", Js.FALSE, true, true, true);
        g.defineData("devicePixelRatio", Js.num(1), true, true, true);
        // 문서의 요소 id 와 일부 요소의 name 은 전역 이름으로도 보인다(HTML5 2014 §5.1.4). 있는 프로퍼티는 가리지 않는다.
        realm.setNamedGlobals(new WindowNames(page));
        installEventTarget(g);
        bridge.getEventHandlers().defineBound(g, page.getWindow(), EventHandlers.GLOBAL_EVENTS);
        bridge.getEventHandlers().defineBound(g, page.getWindow(), EventHandlers.WINDOW_EVENTS);
        accessor(g, "onerror", new NativeFunction.Call() {
            /**
             * Returns the error handler.
             * <p>
             * 오류 핸들러를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the handler or null / 핸들러 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return onerror == null ? Js.NULL : onerror;
            }
        }, new NativeFunction.Call() {
            /**
             * Sets the error handler.
             * <p>
             * 오류 핸들러를 지정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                onerror = v.isObject() ? (JsObject) v : null;
                return Js.UNDEFINED;
            }
        });
        installSizes(g);
        installLocation(g);
        installNavigator(g);
        history = new HistoryApi(page);
        history.install(g);
        installScreenAndPerformance(g);
        // 이미 있는 객체에 인터페이스 이름(Navigator, Location, History, Screen)을 준다: instanceof 와 Object.prototype.toString 이 맞는다.
        InterfaceNames.adopt(page, g, "Navigator", g.get("navigator"));
        InterfaceNames.adopt(page, g, "Location", g.get("location"));
        InterfaceNames.adopt(page, g, "History", g.get("history"));
        InterfaceNames.adopt(page, g, "Screen", g.get("screen"));
        installWindowBasics(g);
        installDialogs(g);
        installBase64(g);
        DomInterfaces.installStorage(bridge);
        installStorage(g);
        installDocumentMembers();
        page.getTimers().install(g);
        page.getObservers().install(g);
        page.getRendering().install(g);
        page.getXPath().install(g);
        new ElementExtras(page).install(g);
        new PointerApi(page).install();
        page.getLifecycle().install();
        elementStates = new ElementStates(page);
        elementStates.install();
        new LegacyWebApi(page).install(g);
        new MicrodataApi(page).install();
        // CSSOM(document.styleSheets, 규칙 객체, CSS 네임스페이스)은 CSSStyleDeclaration 인터페이스가 생긴 뒤에 설치한다.
        new CssomBindings(page).install(g);
        // Web Animations(Animation, KeyframeEffect, element.animate, document.timeline)는 Element 와 Document 인터페이스가 생긴 뒤에 설치한다.
        animations = AnimationHostImpl.install(page);
        mediaQueries = new MediaQueries(page);
        mediaQueries.install(g);
        new RangeGeometry(page).install();
        new DomParsing(page).install();
        messaging = new MessagingApi(page);
        messaging.install(g);
        // window.parent/top/length/frameElement/name 과 프레임 접근은 중첩 브라우징 문맥을 아는 API 가 설치한다.
        frames = new FrameApi(page);
        frames.install(g);
        web = WebApis.install(page);
        workers = new org.duckdns.hjow.swingwebpanel.js.worker.WorkerApi(page);
        workers.install();
        // Web Audio(AudioContext 등)는 DOM 인터페이스(EventTarget, Event)가 생긴 뒤에 설치한다(P25).
        webAudio = org.duckdns.hjow.swingwebpanel.js.host.webaudio.WebAudioApi.install(page);
    }

    /**
     * Tells the media query lists of the page that the viewport changed (the lists whose result changed fire a <code>change</code> event).
     * Call it as a page task.
     * <p>
     * 뷰포트가 바뀌었음을 페이지의 미디어 질의 목록에 알린다(결과가 바뀐 목록은 <code>change</code> 이벤트를 발생시킨다). 페이지 작업으로 호출한다.
     */
    void viewportChanged() {
        MediaQueries m = mediaQueries;
        if (m != null) {
            m.viewportChanged();
        }
    }

    /**
     * Cancels the running web requests of the page without waiting (called when the page is asked to stop; safe from any thread).
     * <p>
     * 페이지의 실행 중인 웹 요청을 기다리지 않고 취소한다(페이지에 중지를 요청할 때 호출하며 어느 스레드에서나 안전하다).
     */
    void cancelRequests() {
        WebApis w = web;
        if (w != null) {
            w.cancelAll();
        }
        // 페이지가 끝나거나 스크립트가 꺼지면 이 페이지의 worker 도 기다리지 않고 모두 종료한다(P20-09-b).
        org.duckdns.hjow.swingwebpanel.js.worker.WorkerApi wk = workers;
        if (wk != null) {
            wk.terminateAll();
        }
        // 스크립트 애니메이션도 기다리지 않고 멈춘다(효과는 문서 스레드의 다음 렌더링에서 사라진다).
        org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi an = animations;
        if (an != null) {
            an.dispose();
        }
        // Web Audio 컨텍스트도 기다리지 않고 닫는다(렌더링 스레드와 오프라인 렌더링, 디코딩 서비스가 끝난다).
        org.duckdns.hjow.swingwebpanel.js.host.webaudio.WebAudioApi wa = webAudio;
        if (wa != null) {
            wa.dispose();
        }
    }

    /**
     * Installs the small window members that need no layout: <code>origin</code>, <code>isSecureContext</code>, <code>status</code>,
     * <code>defaultStatus</code> and <code>reportError</code>.
     * <p>
     * 레이아웃이 필요 없는 작은 window 멤버를 설치한다: <code>origin</code>, <code>isSecureContext</code>, <code>status</code>,
     * <code>defaultStatus</code>, <code>reportError</code>.
     *
     * @param g the global object / 전역 객체
     */
    private void installWindowBasics(JsObject g) {
        getter(g, "origin", new NativeFunction.Call() {
            /**
             * Returns the serialized origin of the document (<code>null</code> for an opaque origin).
             * <p>
             * 문서 오리진의 직렬화를 반환한다(불투명 오리진은 <code>null</code>).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the origin / 오리진
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Origin o = origin();
                return Js.str(o.isOpaque() ? "null" : o.serialize());
            }
        });
        getter(g, "isSecureContext", new NativeFunction.Call() {
            /**
             * Tells whether the page is in a secure context: an <code>https</code> page, or an <code>http</code> page on a loopback host.
             * <p>
             * 페이지가 보안 컨텍스트에 있는지 알려 준다: <code>https</code> 페이지이거나 루프백 호스트의 <code>http</code> 페이지.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the flag / 플래그
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(isSecureContext());
            }
        });
        getter(g, "crossOriginIsolated", new NativeFunction.Call() {
            /** Returns the document's isolation capability. / 문서의 격리 권한을 반환한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(page.getEnvironment().isCrossOriginIsolated());
            }
        });
        g.defineData("status", Js.str(""), true, true, true);
        g.defineData("defaultStatus", Js.str(""), true, true, true);
        method(g, "reportError", 1, new NativeFunction.Call() {
            /**
             * Reports a value as an uncaught error: <code>window.onerror</code>, the <code>error</code> event of the window and the console.
             * <p>
             * 값을 잡히지 않은 오류로 보고한다: <code>window.onerror</code>, window 의 <code>error</code> 이벤트, 콘솔.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'reportError' on 'Window': 1 argument required, but only 0 present.");
                }
                page.reportError(new JsException(args[0]));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>addEventListener</code>, <code>removeEventListener</code> and <code>dispatchEvent</code> on the global object; they work
     * on the window event target whatever <code>this</code> is.
     * <p>
     * 전역 객체에 <code>addEventListener</code>, <code>removeEventListener</code>, <code>dispatchEvent</code> 를 설치한다. <code>this</code> 와
     * 무관하게 window 이벤트 대상에 작용한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installEventTarget(JsObject g) {
        final DomBridge bridge = page.getBridge();
        method(g, "addEventListener", 2, new NativeFunction.Call() {
            /**
             * Registers a listener on the window.
             * <p>
             * window 에 리스너를 등록한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.addListener(bridge, page.getWindow(), args);
                return Js.UNDEFINED;
            }
        });
        method(g, "removeEventListener", 2, new NativeFunction.Call() {
            /**
             * Removes a listener of the window.
             * <p>
             * window 의 리스너를 제거한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.removeListener(bridge, page.getWindow(), args);
                return Js.UNDEFINED;
            }
        });
        method(g, "dispatchEvent", 1, new NativeFunction.Call() {
            /**
             * Dispatches an event at the window.
             * <p>
             * window 에 이벤트를 전달한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false if cancelled / 취소되었으면 false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return DomInterfaces.dispatch(bridge, page.getWindow(), args);
            }
        });
    }

    /**
     * Installs the viewport size properties.
     * <p>
     * 뷰포트 크기 프로퍼티를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installSizes(JsObject g) {
        final HostEnvironment env = page.getEnvironment();
        getter(g, "innerWidth", new NativeFunction.Call() {
            /**
             * Returns the viewport width.
             * <p>
             * 뷰포트 폭을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width / 폭
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportWidth());
            }
        });
        getter(g, "innerHeight", new NativeFunction.Call() {
            /**
             * Returns the viewport height.
             * <p>
             * 뷰포트 높이를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the height / 높이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportHeight());
            }
        });
        getter(g, "outerWidth", new NativeFunction.Call() {
            /**
             * Returns the outer width (equal to the viewport).
             * <p>
             * 바깥 폭을 반환한다(뷰포트와 같다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width / 폭
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportWidth());
            }
        });
        getter(g, "outerHeight", new NativeFunction.Call() {
            /**
             * Returns the outer height (equal to the viewport).
             * <p>
             * 바깥 높이를 반환한다(뷰포트와 같다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the height / 높이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportHeight());
            }
        });
        String[] zero = { "screenX", "screenY", "screenLeft", "screenTop" };
        // 화면 위치는 이 컴포넌트가 알 수 없으므로 0 이다.
        for (int i = 0; i < zero.length; i++) {
            g.defineData(zero[i], Js.num(0), true, true, true);
        }
        String[] scrollXNames = { "scrollX", "pageXOffset" };
        // 가로 스크롤 위치는 환경(스크롤하는 뷰포트)에서 읽는다.
        for (int i = 0; i < scrollXNames.length; i++) {
            getter(g, scrollXNames[i], new NativeFunction.Call() {
                /**
                 * Returns the horizontal scroll position.
                 * <p>
                 * 가로 스크롤 위치를 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the position / 위치
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.num(env.getScrollX());
                }
            });
        }
        String[] scrollYNames = { "scrollY", "pageYOffset" };
        // 세로 스크롤 위치도 환경에서 읽는다.
        for (int i = 0; i < scrollYNames.length; i++) {
            getter(g, scrollYNames[i], new NativeFunction.Call() {
                /**
                 * Returns the vertical scroll position.
                 * <p>
                 * 세로 스크롤 위치를 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the position / 위치
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.num(env.getScrollY());
                }
            });
        }
        NativeFunction.Call scrollAbsolute = new NativeFunction.Call() {
            /**
             * <code>scroll(x, y)</code> and <code>scrollTo(x, y)</code> (or with <code>{left, top}</code>): scrolls to a position.
             * <p>
             * <code>scroll(x, y)</code> 와 <code>scrollTo(x, y)</code>(또는 <code>{left, top}</code>): 위치로 스크롤한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                scrollByArguments(args, false);
                return Js.UNDEFINED;
            }
        };
        NativeFunction.Call scrollRelative = new NativeFunction.Call() {
            /**
             * <code>scrollBy(dx, dy)</code> (or with <code>{left, top}</code>): scrolls relative to the current position.
             * <p>
             * <code>scrollBy(dx, dy)</code>(또는 <code>{left, top}</code>): 현재 위치에서 상대적으로 스크롤한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                scrollByArguments(args, true);
                return Js.UNDEFINED;
            }
        };
        method(g, "scroll", 2, scrollAbsolute);
        method(g, "scrollTo", 2, scrollAbsolute);
        method(g, "scrollBy", 2, scrollRelative);
        NativeFunction.Call noop = new NativeFunction.Call() {
            /**
             * Does nothing (scrolling is handled by the layout layer later).
             * <p>
             * 아무것도 하지 않는다(스크롤은 이후 레이아웃 계층이 처리한다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.UNDEFINED;
            }
        };
        String[] noops = { "focus", "blur", "close", "stop", "print" };
        // 화면 없는 동작은 빈 함수로 둔다.
        for (int i = 0; i < noops.length; i++) {
            method(g, noops[i], 0, noop);
        }
        method(g, "open", 0, new NativeFunction.Call() {
            /**
             * Popups are not supported: returns null.
             * <p>
             * 팝업은 지원하지 않는다: null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return null / null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.NULL;
            }
        });
    }

    /**
     * Scrolls the viewport for the arguments of <code>scroll</code>, <code>scrollTo</code> and <code>scrollBy</code>: either a pair of numbers
     * <code>(x, y)</code> or one object with <code>left</code> and <code>top</code> (a member that is missing keeps that axis). A number that is
     * not finite counts as 0, as in the specification.
     * <p>
     * <code>scroll</code>, <code>scrollTo</code>, <code>scrollBy</code> 의 인수로 뷰포트를 스크롤한다: 숫자 쌍 <code>(x, y)</code> 또는
     * <code>left</code>, <code>top</code> 이 있는 객체 하나(없는 멤버는 그 축을 유지한다). 유한하지 않은 숫자는 규격대로 0 으로 본다.
     *
     * @param args     the script arguments / 스크립트 인수
     * @param relative true for <code>scrollBy</code> / <code>scrollBy</code> 이면 true
     */
    private void scrollByArguments(JsValue[] args, boolean relative) {
        if (args.length == 0) {
            return;
        }
        double x;
        double y;
        // 객체 하나이면 left/top 을, 아니면 숫자 쌍을 읽는다.
        if (args.length == 1 && args[0].isObject()) {
            JsObject o = (JsObject) args[0];
            x = scrollNumber(o.get("left"), Double.NaN);
            y = scrollNumber(o.get("top"), Double.NaN);
        } else {
            x = scrollNumber(args[0], 0);
            y = args.length > 1 ? scrollNumber(args[1], 0) : 0;
        }
        HostEnvironment env = page.getEnvironment();
        double nx = Double.isNaN(x) ? env.getScrollX() : (relative ? env.getScrollX() + x : x);
        double ny = Double.isNaN(y) ? env.getScrollY() : (relative ? env.getScrollY() + y : y);
        env.scrollTo(nx, ny);
    }

    /**
     * Converts a scroll argument to a number: a missing value gives the fallback, a value that is not finite gives 0.
     * <p>
     * 스크롤 인수를 숫자로 바꾼다: 값이 없으면 대체값, 유한하지 않으면 0.
     *
     * @param v        the script value / 스크립트 값
     * @param fallback the result for undefined / undefined 일 때의 결과
     * @return the number / 숫자
     */
    private static double scrollNumber(JsValue v, double fallback) {
        if (v.isUndefined()) {
            return fallback;
        }
        double d = Ops.toNumber(v);
        return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
    }

    /**
     * Returns the part of a URL that a <code>location</code> property names.
     * <p>
     * <code>location</code> 프로퍼티가 가리키는 URL 의 부분을 반환한다.
     *
     * @param name the property name / 프로퍼티 이름
     * @param url  the document URL / 문서 URL
     * @return the value / 값
     */
    private static String locationPart(String name, String url) {
        if ("href".equals(name)) {
            return url;
        }
        if ("origin".equals(name)) {
            Origin o = Origin.of(url);
            return o.isOpaque() ? "null" : o.serialize();
        }
        URI u;
        try {
            u = new URI(url);
        } catch (URISyntaxException e) {
            return "";
        }
        if ("protocol".equals(name)) {
            return u.getScheme() == null ? "" : u.getScheme() + ":";
        }
        String host = u.getHost() == null ? "" : u.getHost();
        int port = u.getPort();
        if ("hostname".equals(name)) {
            return host;
        } else if ("port".equals(name)) {
            return port < 0 ? "" : Integer.toString(port);
        } else if ("host".equals(name)) {
            return port < 0 ? host : host + ":" + port;
        } else if ("pathname".equals(name)) {
            return u.getRawPath() == null ? "" : u.getRawPath();
        } else if ("search".equals(name)) {
            return u.getRawQuery() == null || u.getRawQuery().length() == 0 ? "" : "?" + u.getRawQuery();
        } else if ("hash".equals(name)) {
            return u.getRawFragment() == null || u.getRawFragment().length() == 0 ? "" : "#" + u.getRawFragment();
        }
        return "";
    }

    /**
     * Resolves a navigation target against the document base and asks the environment to navigate.
     * <p>
     * 이동 대상을 문서 기준 URL 로 해석하고 환경에 이동을 요청한다.
     *
     * @param target  the target text / 대상 텍스트
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    private void navigate(String target, boolean replace) {
        String abs;
        try {
            abs = UrlUtil.resolve(page.getDocument().getBaseUri(), target);
        } catch (RuntimeException e) {
            throw page.getBridge().domError(DomException.syntax("Failed to parse URL: " + target));
        }
        page.getEnvironment().navigate(abs, replace);
    }

    /**
     * Installs <code>location</code> (and <code>document.location</code> through the document members).
     * <p>
     * <code>location</code> 을 설치한다(<code>document.location</code> 은 document 멤버 설치에서 연결한다).
     *
     * @param g the global object / 전역 객체
     */
    private void installLocation(JsObject g) {
        JsRealm realm = page.getRealm();
        location = new LocationObject(realm);
        String[] parts = { "href", "origin", "protocol", "host", "hostname", "port", "pathname", "search", "hash" };
        // 위치의 각 부분을 문서 URL 에서 읽는 접근자로 정의한다. href 와 hash 는 쓰면 이동한다.
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            NativeFunction.Call get = new NativeFunction.Call() {
                /**
                 * Reads the part from the current document URL.
                 * <p>
                 * 현재 문서 URL 에서 부분을 읽는다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the text / 문자열
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.str(locationPart(part, page.getDocument().getUrl()));
                }
            };
            if ("href".equals(part) || "hash".equals(part)) {
                accessor(location, part, get, new NativeFunction.Call() {
                    /**
                     * Navigates to the new address.
                     * <p>
                     * 새 주소로 이동한다.
                     *
                     * @param r    the realm / realm
                     * @param self this value / this 값
                     * @param args arguments / 인수
                     * @return undefined / undefined
                     */
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        String v = Ops.toString(Js.arg(args, 0));
                        if ("hash".equals(part)) {
                            String base = page.getDocument().getUrl();
                            int h = base.indexOf('#');
                            v = (h >= 0 ? base.substring(0, h) : base) + (v.startsWith("#") ? v : "#" + v);
                        }
                        navigate(v, false);
                        return Js.UNDEFINED;
                    }
                });
            } else {
                getter(location, part, get);
            }
        }
        method(location, "assign", 1, new NativeFunction.Call() {
            /**
             * Navigates to an address.
             * <p>
             * 주소로 이동한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                navigate(Ops.toString(Js.arg(args, 0)), false);
                return Js.UNDEFINED;
            }
        });
        method(location, "replace", 1, new NativeFunction.Call() {
            /**
             * Navigates and replaces the history entry.
             * <p>
             * 이동하며 이력 항목을 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                navigate(Ops.toString(Js.arg(args, 0)), true);
                return Js.UNDEFINED;
            }
        });
        method(location, "reload", 0, new NativeFunction.Call() {
            /**
             * Reloads the page.
             * <p>
             * 페이지를 새로고침한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                page.getEnvironment().reload();
                return Js.UNDEFINED;
            }
        });
        method(location, "toString", 0, new NativeFunction.Call() {
            /**
             * Returns the URL.
             * <p>
             * URL 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(page.getDocument().getUrl());
            }
        });
        accessor(g, "location", new NativeFunction.Call() {
            /**
             * Returns the location object.
             * <p>
             * location 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return location;
            }
        }, new NativeFunction.Call() {
            /**
             * Assigning to <code>window.location</code> navigates.
             * <p>
             * <code>window.location</code> 에 대입하면 이동한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                navigate(Ops.toString(Js.arg(args, 0)), false);
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Installs <code>navigator</code>.
     * <p>
     * <code>navigator</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installNavigator(JsObject g) {
        final JsRealm realm = page.getRealm();
        final HostEnvironment env = page.getEnvironment();
        JsObject nav = realm.newObject();
        getter(nav, "userAgent", new NativeFunction.Call() {
            /**
             * Returns the user agent of the component.
             * <p>
             * 컴포넌트의 User-Agent 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the user agent / User-Agent
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(env.getUserAgent());
            }
        });
        getter(nav, "cookieEnabled", new NativeFunction.Call() {
            /**
             * Tells whether cookies work.
             * <p>
             * 쿠키가 동작하는지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the flag / 플래그
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(env.getCookies() != null);
            }
        });
        String[][] strings = { { "appCodeName", "Mozilla" }, { "appName", "Netscape" }, { "appVersion", "5.0" }, { "platform", "Java" },
            { "product", "Gecko" }, { "productSub", "20030107" }, { "vendor", "" }, { "vendorSub", "" }, { "language", "en-US" } };
        // 고정 문자열 프로퍼티를 정의한다.
        for (int i = 0; i < strings.length; i++) {
            nav.defineData(strings[i][0], Js.str(strings[i][1]), false, true, true);
        }
        nav.defineData("languages", realm.newArray(new JsValue[] { Js.str("en-US") }), false, true, true);
        getter(nav, "onLine", new NativeFunction.Call() {
            /**
             * Tells whether the host has a network connection (the host reports a change with the <code>online</code> and <code>offline</code> events).
             * <p>
             * 호스트에 네트워크 연결이 있는지 알려 준다(호스트는 변화를 <code>online</code>, <code>offline</code> 이벤트로 알린다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(env.isOnline());
            }
        });
        nav.defineData("hardwareConcurrency", Js.num(1), false, true, true);
        nav.defineData("maxTouchPoints", Js.num(0), false, true, true);
        nav.defineData("plugins", realm.newArray(), false, true, true);
        nav.defineData("mimeTypes", realm.newArray(), false, true, true);
        // 클립보드 인터페이스는 보안 컨텍스트의 Window에만 설치한다.
        if (isSecureContext()) {
            ClipboardApi.install(page, nav);
        }
        method(nav, "javaEnabled", 0, new NativeFunction.Call() {
            /**
             * Java applets are not supported.
             * <p>
             * 자바 애플릿은 지원하지 않는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false / false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.FALSE;
            }
        });
        g.defineData("navigator", nav, true, true, true);
    }

    /**
     * Returns the <code>history</code> object of the page (its state and the session history operations).
     * <p>
     * 페이지의 <code>history</code> 객체(상태와 세션 이력 동작)를 반환한다.
     *
     * @return the history object / history 객체
     */
    HistoryApi history() {
        return history;
    }

    /**
     * Installs <code>screen</code> and <code>performance</code>.
     * <p>
     * <code>screen</code> 과 <code>performance</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installScreenAndPerformance(JsObject g) {
        JsRealm realm = page.getRealm();
        JsObject s = realm.newObject();
        final HostEnvironment env = page.getEnvironment();
        getter(s, "width", new NativeFunction.Call() {
            /**
             * Returns the screen width (the viewport width).
             * <p>
             * 화면 폭을 반환한다(뷰포트 폭).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width / 폭
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportWidth());
            }
        });
        getter(s, "height", new NativeFunction.Call() {
            /**
             * Returns the screen height (the viewport height).
             * <p>
             * 화면 높이를 반환한다(뷰포트 높이).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the height / 높이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportHeight());
            }
        });
        s.defineData("colorDepth", Js.num(24), false, true, true);
        s.defineData("pixelDepth", Js.num(24), false, true, true);
        s.defineData("availLeft", Js.num(0), false, true, true);
        s.defineData("availTop", Js.num(0), false, true, true);
        getter(s, "availWidth", new NativeFunction.Call() {
            /**
             * Returns the available width (the viewport width: the component has no screen furniture).
             * <p>
             * 사용할 수 있는 폭을 반환한다(뷰포트 폭: 컴포넌트에는 화면 장식이 없다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width / 폭
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportWidth());
            }
        });
        getter(s, "availHeight", new NativeFunction.Call() {
            /**
             * Returns the available height (the viewport height).
             * <p>
             * 사용할 수 있는 높이를 반환한다(뷰포트 높이).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the height / 높이
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(env.getViewportHeight());
            }
        });
        JsObject orientation = realm.newObject();
        getter(orientation, "type", new NativeFunction.Call() {
            /**
             * Returns <code>landscape-primary</code> for a viewport that is at least as wide as high, else <code>portrait-primary</code>.
             * <p>
             * 뷰포트가 높이 이상으로 넓으면 <code>landscape-primary</code>, 아니면 <code>portrait-primary</code> 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the type / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(env.getViewportWidth() >= env.getViewportHeight() ? "landscape-primary" : "portrait-primary");
            }
        });
        orientation.defineData("angle", Js.num(0), false, true, true);
        s.defineData("orientation", orientation, false, true, true);
        g.defineData("screen", s, true, true, true);
        // performance 는 User Timing 과 Navigation Timing 을 가진 Performance 인터페이스의 객체이다.
        page.getPerformance().install(g);
    }

    /**
     * Installs <code>alert</code>, <code>confirm</code> and <code>prompt</code>; they only write to the console (no dialog).
     * <p>
     * <code>alert</code>, <code>confirm</code>, <code>prompt</code> 를 설치한다. 콘솔에만 쓰며 대화 상자는 없다.
     *
     * @param g the global object / 전역 객체
     */
    private void installDialogs(JsObject g) {
        method(g, "alert", 0, new NativeFunction.Call() {
            /**
             * Writes the message to the console.
             * <p>
             * 메시지를 콘솔에 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ConsoleRecord.deliver(r.getConsole(), ConsoleRecord.dialog("alert", args.length == 0 ? "" : Ops.toString(args[0]), r.getCallSourceName(), r.getCallLine(), r.getCallColumn()));
                return Js.UNDEFINED;
            }
        });
        method(g, "confirm", 0, new NativeFunction.Call() {
            /**
             * Writes the message to the console and answers false.
             * <p>
             * 메시지를 콘솔에 쓰고 false 로 답한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false / false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ConsoleRecord.deliver(r.getConsole(), ConsoleRecord.dialog("confirm", args.length == 0 ? "" : Ops.toString(args[0]), r.getCallSourceName(), r.getCallLine(), r.getCallColumn()));
                return Js.FALSE;
            }
        });
        method(g, "prompt", 0, new NativeFunction.Call() {
            /**
             * Writes the message to the console and answers null.
             * <p>
             * 메시지를 콘솔에 쓰고 null 로 답한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return null / null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ConsoleRecord.deliver(r.getConsole(), ConsoleRecord.dialog("prompt", args.length == 0 ? "" : Ops.toString(args[0]), r.getCallSourceName(), r.getCallLine(), r.getCallColumn()));
                return Js.NULL;
            }
        });
    }

    /**
     * The characters of the base64 alphabet.
     * <p>
     * base64 알파벳 문자.
     */
    private static final String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Installs <code>btoa</code> and <code>atob</code>.
     * <p>
     * <code>btoa</code> 와 <code>atob</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installBase64(JsObject g) {
        final DomBridge bridge = page.getBridge();
        method(g, "btoa", 1, new NativeFunction.Call() {
            /**
             * Encodes a Latin-1 string as base64.
             * <p>
             * Latin-1 문자열을 base64 로 인코딩한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the encoded text / 인코딩한 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String s = Ops.toString(Js.arg(args, 0));
                StringBuilder sb = new StringBuilder();
                // 3 바이트씩 묶어 4 글자로 바꾼다.
                for (int i = 0; i < s.length(); i += 3) {
                    int b0 = latin1(s.charAt(i), bridge);
                    int b1 = i + 1 < s.length() ? latin1(s.charAt(i + 1), bridge) : -1;
                    int b2 = i + 2 < s.length() ? latin1(s.charAt(i + 2), bridge) : -1;
                    sb.append(B64.charAt(b0 >> 2));
                    sb.append(B64.charAt(((b0 & 3) << 4) | (b1 < 0 ? 0 : b1 >> 4)));
                    sb.append(b1 < 0 ? '=' : B64.charAt(((b1 & 15) << 2) | (b2 < 0 ? 0 : b2 >> 6)));
                    sb.append(b2 < 0 ? '=' : B64.charAt(b2 & 63));
                }
                return Js.str(sb.toString());
            }
        });
        method(g, "atob", 1, new NativeFunction.Call() {
            /**
             * Decodes base64 into a Latin-1 string.
             * <p>
             * base64 를 Latin-1 문자열로 디코딩한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the decoded text / 디코딩한 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String s = Ops.toString(Js.arg(args, 0));
                StringBuilder clean = new StringBuilder();
                // 공백을 건너뛰고 알파벳 문자만 모은다.
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r') {
                        continue;
                    }
                    clean.append(c);
                }
                String d = clean.toString();
                if (d.length() % 4 == 0 && d.endsWith("==")) {
                    d = d.substring(0, d.length() - 2);
                } else if (d.length() % 4 == 0 && d.endsWith("=")) {
                    d = d.substring(0, d.length() - 1);
                }
                if (d.length() % 4 == 1) {
                    throw bridge.domError(DomException.invalidCharacter("The string to be decoded is not correctly encoded."));
                }
                StringBuilder out = new StringBuilder();
                int bits = 0;
                int acc = 0;
                // 6 비트씩 모아 8 비트가 되면 한 글자를 내보낸다.
                for (int i = 0; i < d.length(); i++) {
                    int v = B64.indexOf(d.charAt(i));
                    if (v < 0) {
                        throw bridge.domError(DomException.invalidCharacter("The string to be decoded is not correctly encoded."));
                    }
                    acc = (acc << 6) | v;
                    bits += 6;
                    if (bits >= 8) {
                        bits -= 8;
                        out.append((char) ((acc >> bits) & 255));
                    }
                }
                return Js.str(out.toString());
            }
        });
    }

    /**
     * Checks that a character fits one byte.
     * <p>
     * 문자가 한 바이트에 들어가는지 확인한다.
     *
     * @param c      the character / 문자
     * @param bridge the bridge / 브리지
     * @return its code / 코드
     */
    private static int latin1(char c, DomBridge bridge) {
        if (c > 255) {
            throw bridge.domError(DomException.invalidCharacter("The string to be encoded contains characters outside of the Latin1 range."));
        }
        return c;
    }

    /**
     * Refuses the cookies to a document of a nested browsing context that has an opaque origin (a sandbox without
     * <code>allow-same-origin</code>): reading or writing <code>document.cookie</code> throws a <code>SecurityError</code>. The top-level
     * document keeps the plain rule (a document without a network address simply has no cookies).
     * <p>
     * 불투명 오리진(<code>allow-same-origin</code> 이 없는 샌드박스)을 가진 중첩 브라우징 문맥의 문서에는 쿠키를 주지 않는다: <code>document.cookie</code> 를
     * 읽거나 쓰면 <code>SecurityError</code> 를 던진다. 최상위 문서는 평소 규칙을 따른다(네트워크 주소가 없는 문서는 그냥 쿠키가 없다).
     *
     * @param what <code>read</code> or <code>set</code> for the message / 메시지에 쓸 <code>read</code> 또는 <code>set</code>
     */
    private void requireCookieAccess(String what) {
        BrowsingContext c = page.getEnvironment().getBrowsingContext();
        if (c != null && c.getParent() != null && origin().isOpaque()) {
            throw page.getBridge().domError(DomException.security("Failed to " + what + " the 'cookie' property from 'Document': The document is sandboxed"
                    + " and lacks the 'allow-same-origin' flag."));
        }
    }

    /**
     * Returns the origin of the document.
     * <p>
     * 문서의 오리진을 반환한다.
     *
     * @return the origin / 오리진
     */
    private Origin origin() {
        return Origin.of(page.getDocument().getOriginUrl());
    }

    /**
     * Checks the trustworthiness of this document and every containing document.
     * <p>
     * 현재 문서와 모든 컨테이너 문서의 신뢰 가능 여부를 확인한다.
     *
     * @return true for a secure document context / 문서가 보안 컨텍스트이면 true
     */
    private boolean isSecureContext() {
        if (!isSecureDocumentOrigin(origin())) {
            return false;
        }
        BrowsingContext current = page.getEnvironment().getBrowsingContext();
        // 자기 오리진이 안전해도 상위 문서가 안전하지 않으면 보안 컨텍스트가 아니다.
        for (BrowsingContext parent = current == null ? null : current.getParent(); parent != null; parent = parent.getParent()) {
            if (!isSecureDocumentOrigin(Origin.of(parent.getOriginUrl()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Includes application resources installed by the host in the secure Window context policy.
     * <p>
     * 호스트가 설치한 애플리케이션 리소스를 보안 Window 문맥 정책에 포함한다.
     *
     * @param origin a document origin / 문서 오리진
     * @return true for trustworthy network and classpath origins / 신뢰 가능한 네트워크·클래스패스 오리진이면 true
     */
    private static boolean isSecureDocumentOrigin(Origin origin) {
        return UrlUtil.SCHEME_CLASSPATH.equals(origin.getScheme()) || origin.isPotentiallyTrustworthy();
    }

    /**
     * Creates the listener that turns a change of a storage area by another document into a <code>storage</code> event at the window.
     * <p>
     * 다른 문서가 저장소를 바꾼 것을 window 의 <code>storage</code> 이벤트로 바꾸는 리스너를 만든다.
     *
     * @param local true for localStorage, false for sessionStorage / localStorage 이면 true, sessionStorage 이면 false
     * @return the listener / 리스너
     */
    private StorageListener storageListener(final boolean local) {
        final String originText = origin().serialize();
        return new StorageListener() {
            /**
             * Returns the origin this listener belongs to.
             * <p>
             * 이 리스너가 속한 오리진을 반환한다.
             *
             * @return the serialized origin / 직렬화된 오리진
             */
            public String getOrigin() {
                return originText;
            }

            /**
             * Queues the <code>storage</code> event for the window; another thread may call this.
             * <p>
             * window 의 <code>storage</code> 이벤트를 대기열에 넣는다. 다른 스레드가 부를 수 있다.
             *
             * @param key      the changed key or null for clear / 바뀐 키. clear 이면 null
             * @param oldValue the old value / 이전 값
             * @param newValue the new value / 새 값
             * @param source   the source document / 출처 문서
             */
            public void storageChanged(final String key, final String oldValue, final String newValue, Object source) {
                Scheduler s = page.getEnvironment().getScheduler();
                Runnable task = new Runnable() {
                    /**
                     * Dispatches the event as a page task.
                     * <p>
                     * 이벤트를 페이지 작업으로 전달한다.
                     */
                    public void run() {
                        page.runTask(new Runnable() {
                            /**
                             * Dispatches the event at the window.
                             * <p>
                             * window 에 이벤트를 전달한다.
                             */
                            public void run() {
                                WebStorage area = local ? localView : sessionView;
                                if (area == null) {
                                    return;
                                }
                                StorageEvent ev = new StorageEvent(key, oldValue, newValue, page.getDocument().getUrl(), area);
                                page.getWindow().dispatchEvent(ev);
                            }
                        });
                    }
                };
                if (s != null) {
                    s.post(task, 0);
                } else {
                    task.run();
                }
            }
        };
    }

    /**
     * Installs <code>localStorage</code> and <code>sessionStorage</code> as accessors that throw a SecurityError for opaque origins.
     * <p>
     * <code>localStorage</code> 와 <code>sessionStorage</code> 를 접근자로 설치한다. 불투명 오리진에서는 SecurityError 를 던진다.
     *
     * @param g the global object / 전역 객체
     */
    private void installStorage(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final HostEnvironment env = page.getEnvironment();
        getter(g, "localStorage", new NativeFunction.Call() {
            /**
             * Returns the localStorage of the document's origin.
             * <p>
             * 문서 오리진의 localStorage 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the Storage object / Storage 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (localView == null) {
                    if (env.getLocalStorage() == null) {
                        throw bridge.domError(DomException.security("localStorage is not available"));
                    }
                    try {
                        localListener = storageListener(true);
                        localView = env.getLocalStorage().forOrigin(origin(), localListener);
                    } catch (DomException e) {
                        throw bridge.domError(e);
                    }
                    env.getLocalStorage().addListener(localListener);
                }
                return bridge.toJs(localView);
            }
        });
        getter(g, "sessionStorage", new NativeFunction.Call() {
            /**
             * Returns the sessionStorage of the document's origin.
             * <p>
             * 문서 오리진의 sessionStorage 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the Storage object / Storage 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (sessionView == null) {
                    if (env.getSessionStorage() == null) {
                        throw bridge.domError(DomException.security("sessionStorage is not available"));
                    }
                    try {
                        sessionListener = storageListener(false);
                        sessionView = env.getSessionStorage().forOrigin(origin(), sessionListener);
                    } catch (DomException e) {
                        throw bridge.domError(e);
                    }
                    env.getSessionStorage().addListener(sessionListener);
                }
                return bridge.toJs(sessionView);
            }
        });
    }

    /**
     * Installs the members of <code>Document</code> that depend on the page host.
     * <p>
     * 페이지 호스트에 의존하는 <code>Document</code> 멤버를 설치한다.
     */
    private void installDocumentMembers() {
        final DomBridge bridge = page.getBridge();
        final HostEnvironment env = page.getEnvironment();
        JsObject d = bridge.getInterface("Document").getPrototype();
        getter(d, "scrollingElement", new NativeFunction.Call() {
            /**
             * Returns the element that scrolls the viewport: the root element in standards mode, the <code>body</code> element in quirks
             * mode (null if there is none).
             * <p>
             * 뷰포트를 스크롤하는 요소를 반환한다: 표준 모드에서는 루트 요소, 호환(quirks) 모드에서는 <code>body</code> 요소(없으면 null).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Document doc = page.getDocument();
                Element root = doc.getDocumentElement();
                if (root == null || !"BackCompat".equals(doc.getCompatMode())) {
                    return root == null ? Js.NULL : bridge.toJs(root);
                }
                // 호환 모드에서는 루트의 첫 body 자식을 찾는다.
                for (Element c = root.getFirstElementChild(); c != null; c = c.getNextElementSibling()) {
                    if ("body".equals(c.getLocalName())) {
                        return bridge.toJs(c);
                    }
                }
                return Js.NULL;
            }
        });
        method(d, "elementFromPoint", 2, new NativeFunction.Call() {
            /**
             * Returns the topmost element at a point of the viewport, or null when the point is outside the viewport.
             * <p>
             * 뷰포트의 한 점에 있는 최상위 요소를 반환한다. 점이 뷰포트 밖이면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (x, y) / 인수(x, y)
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'elementFromPoint' on 'Document': 2 arguments required, but only " + args.length
                        + " present.");
                }
                double x = Ops.toNumber(args[0]);
                double y = Ops.toNumber(args[1]);
                // 유한하지 않은 값은 WebIDL 의 double 변환에서 TypeError 이다.
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    throw JsException.typeError("Failed to execute 'elementFromPoint' on 'Document': The provided double value is non-finite.");
                }
                Element hit = env.getElementAt(x, y);
                return hit == null ? Js.NULL : bridge.toJs(hit);
            }
        });
        accessor(d, "cookie", new NativeFunction.Call() {
            /**
             * Returns the cookies of the document URL without the HttpOnly ones.
             * <p>
             * 문서 URL 의 쿠키를 반환한다(HttpOnly 제외).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the cookie text / 쿠키 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireCookieAccess("read");
                CookieStore c = env.getCookies();
                return Js.str(c == null ? "" : c.getDocumentCookie(page.getDocument().getUrl()));
            }
        }, new NativeFunction.Call() {
            /**
             * Stores one cookie from script (<code>document.cookie = "a=b; path=/"</code>).
             * <p>
             * 스크립트가 쿠키 하나를 저장한다(<code>document.cookie = "a=b; path=/"</code>).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireCookieAccess("set");
                CookieStore c = env.getCookies();
                if (c != null) {
                    c.setFromScript(page.getDocument().getUrl(), Ops.toString(Js.arg(args, 0)));
                }
                return Js.UNDEFINED;
            }
        });
        getter(d, "referrer", new NativeFunction.Call() {
            /**
             * Returns the referring page.
             * <p>
             * 참조 페이지를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(env.getReferrer());
            }
        });
        getter(d, "readyState", new NativeFunction.Call() {
            /**
             * Returns the ready state.
             * <p>
             * 준비 상태를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return loading, interactive or complete / loading, interactive, complete
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(page.getReadyState());
            }
        });
        accessor(d, "location", new NativeFunction.Call() {
            /**
             * Returns the location object.
             * <p>
             * location 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return location;
            }
        }, new NativeFunction.Call() {
            /**
             * Assigning navigates.
             * <p>
             * 대입하면 이동한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                navigate(Ops.toString(Js.arg(args, 0)), false);
                return Js.UNDEFINED;
            }
        });
        getter(d, "defaultView", new NativeFunction.Call() {
            /**
             * Returns the browsing context's window identity.
             * <p>
             * 브라우징 문맥을 가리키는 창 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the window identity / 창 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return r.getGlobalThisValue();
            }
        });
        getter(d, "currentScript", new NativeFunction.Call() {
            /**
             * Returns the classic script that is executing, or null.
             * <p>
             * 실행 중인 일반 스크립트를 반환한다. 없으면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(page.getCurrentScript());
            }
        });
        getter(d, "hidden", new NativeFunction.Call() {
            /**
             * The document is hidden while the component that shows it is not on the screen (Page Visibility Level 2).
             * <p>
             * 문서를 보여 주는 컴포넌트가 화면에 없으면 문서는 숨겨진 것이다(Page Visibility Level 2).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return true if hidden / 숨겨졌으면 true
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(!page.getLifecycle().isDocumentVisible());
            }
        });
        getter(d, "visibilityState", new NativeFunction.Call() {
            /**
             * The visibility state: <code>visible</code> or <code>hidden</code> (Page Visibility Level 2).
             * <p>
             * 가시성 상태: <code>visible</code> 또는 <code>hidden</code>(Page Visibility Level 2).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(page.getLifecycle().isDocumentVisible() ? "visible" : "hidden");
            }
        });
        getter(d, "domain", new NativeFunction.Call() {
            /**
             * Returns the host name of the document.
             * <p>
             * 문서의 호스트 이름을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the host name / 호스트 이름
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(locationPart("hostname", page.getDocument().getUrl()));
            }
        });
        NativeFunction.Call write = new NativeFunction.Call() {
            /**
             * Implements document.write (all arguments are joined).
             * <p>
             * document.write 를 구현한다(모든 인수를 이어 붙인다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireHtmlDocument("write");
                StringBuilder sb = new StringBuilder();
                // 모든 인수를 문자열로 바꿔 이어 붙인다.
                for (int i = 0; i < args.length; i++) {
                    sb.append(Ops.toString(args[i]));
                }
                page.write(sb.toString());
                return Js.UNDEFINED;
            }
        };
        method(d, "write", 1, write);
        method(d, "writeln", 1, new NativeFunction.Call() {
            /**
             * Implements document.writeln (adds a line feed).
             * <p>
             * document.writeln 을 구현한다(줄바꿈을 더한다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireHtmlDocument("writeln");
                StringBuilder sb = new StringBuilder();
                // 모든 인수를 문자열로 바꿔 이어 붙인 뒤 줄바꿈을 더한다.
                for (int i = 0; i < args.length; i++) {
                    sb.append(Ops.toString(args[i]));
                }
                page.write(sb.append('\n').toString());
                return Js.UNDEFINED;
            }
        });
        method(d, "open", 0, new NativeFunction.Call() {
            /**
             * Implements document.open.
             * <p>
             * document.open 을 구현한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the document / 문서
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireHtmlDocument("open");
                page.open();
                return self;
            }
        });
        method(d, "close", 0, new NativeFunction.Call() {
            /**
             * Implements document.close.
             * <p>
             * document.close 를 구현한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireHtmlDocument("close");
                page.close();
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Throws an <code>InvalidStateError</code> if the document is not an HTML document: <code>document.write</code>, <code>writeln</code>,
     * <code>open</code> and <code>close</code> are for the HTML parser only and an XML document (XHTML) refuses them (HTML5 REC
     * 2014-10-28, the methods of the Document interface: "If the Document object is not flagged as an HTML document, throw an
     * InvalidStateError").
     * <p>
     * 문서가 HTML 문서가 아니면 <code>InvalidStateError</code> 를 던진다: <code>document.write</code>, <code>writeln</code>, <code>open</code>,
     * <code>close</code> 는 HTML 파서 전용이며 XML 문서(XHTML)는 거부한다.
     *
     * @param method the name of the method for the message / 메시지에 쓸 메서드 이름
     */
    private void requireHtmlDocument(String method) {
        if (!page.getDocument().isHtmlDocument()) {
            throw page.getBridge().domError(DomException.invalidState("Failed to execute '" + method + "' on 'Document': This method is not supported for XML documents."));
        }
    }

    /**
     * Calls <code>window.onerror</code> if it is set.
     * <p>
     * 설정되어 있으면 <code>window.onerror</code> 를 부른다.
     *
     * @param message the message / 메시지
     * @param source  the script name / 스크립트 이름
     * @param line    the line / 줄
     * @param col     the column / 열
     * @param error   the error / 오류
     * @return true if the handler returned true (the error is handled) / 핸들러가 true 를 반환했으면(오류를 처리했으면) true
     */
    boolean callOnError(String message, String source, int line, int col, RuntimeException error) {
        if (onerror == null || !onerror.isCallable()) {
            return false;
        }
        JsValue errValue = error instanceof JsException && ((JsException) error).hasValue()
                ? ((JsException) error).getValue(page.getRealm()) : Js.NULL;
        try {
            JsValue r = Ops.call(onerror, page.getRealm().getGlobalThisValue(), new JsValue[] { Js.str(message), Js.str(source), Js.num(line),
                Js.num(col), errValue });
            return r == Js.TRUE;
        } catch (JsException e) {
            return false;
        }
    }

    /**
     * Removes the storage listeners of this page.
     * <p>
     * 이 페이지의 저장소 리스너를 제거한다.
     */
    void dispose() {
        HostEnvironment env = page.getEnvironment();
        if (localListener != null && env.getLocalStorage() != null) {
            env.getLocalStorage().removeListener(localListener);
        }
        if (sessionListener != null && env.getSessionStorage() != null) {
            env.getSessionStorage().removeListener(sessionListener);
        }
        localView = null;
        sessionView = null;
        if (web != null) {
            web.dispose();
        }
        if (workers != null) {
            workers.dispose();
        }
    }
}
