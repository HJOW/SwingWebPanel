package org.duckdns.hjow.swingwebpanel.js.worker;

import java.net.URI;
import java.net.URISyntaxException;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomInterfaces;
import org.duckdns.hjow.swingwebpanel.js.dom.EventHandlers;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Installs the members of the global scope of a dedicated worker (<code>DedicatedWorkerGlobalScope</code>, HTML Standard "Web workers"):
 * <code>self</code>, <code>name</code>, the event target methods, <code>onmessage</code>, <code>onmessageerror</code>, <code>onerror</code>,
 * <code>postMessage</code>, <code>close</code>, <code>importScripts</code>, the read-only <code>location</code>, the <code>navigator</code>
 * subset (<code>userAgent</code>, <code>hardwareConcurrency</code>, <code>onLine</code> and a few constants), the timers,
 * <code>structuredClone</code> and <code>performance.now()</code>. Nothing else of a page is here: there is no <code>window</code>,
 * <code>document</code>, <code>localStorage</code>, <code>sessionStorage</code>, <code>history</code>, dialog function or <code>Worker</code>,
 * and no Java object can be reached.
 * <p>
 * 전용 worker 의 전역 범위(<code>DedicatedWorkerGlobalScope</code>, HTML 표준 "Web workers")의 멤버를 설치한다: <code>self</code>, <code>name</code>,
 * 이벤트 대상 메서드, <code>onmessage</code>, <code>onmessageerror</code>, <code>onerror</code>, <code>postMessage</code>, <code>close</code>,
 * <code>importScripts</code>, 읽기 전용 <code>location</code>, <code>navigator</code> 일부(<code>userAgent</code>, <code>hardwareConcurrency</code>,
 * <code>onLine</code>과 몇몇 상수), 타이머, <code>structuredClone</code>, <code>performance.now()</code>. 페이지의 나머지는 없다:
 * <code>window</code>, <code>document</code>, <code>localStorage</code>, <code>sessionStorage</code>, <code>history</code>, 대화 상자 함수,
 * <code>Worker</code> 가 없고 어떤 Java 객체에도 닿을 수 없다.
 */
final class WorkerGlobals {

    /**
     * Private constructor: static installer.
     * <p>
     * 비공개 생성자: 정적 설치기.
     */
    private WorkerGlobals() {
    }

    /**
     * Defines a method on an object.
     * <p>
     * 객체에 메서드를 정의한다.
     *
     * @param realm  the realm / realm
     * @param o      the object / 객체
     * @param name   the name / 이름
     * @param length the declared length / 선언된 길이
     * @param call   the behaviour / 동작
     */
    private static void method(JsRealm realm, JsObject o, String name, int length, NativeFunction.Call call) {
        o.defineData(name, realm.newFunction(name, length, call), true, true, true);
    }

    /**
     * Defines a read-only accessor.
     * <p>
     * 읽기 전용 접근자를 정의한다.
     *
     * @param realm the realm / realm
     * @param o     the object / 객체
     * @param name  the name / 이름
     * @param get   the getter / getter
     */
    private static void getter(JsRealm realm, JsObject o, String name, NativeFunction.Call get) {
        o.defineAccessor(name, realm.newFunction("get " + name, 0, get), null, true, true);
    }

    /**
     * Installs the members of the global scope.
     * <p>
     * 전역 범위의 멤버를 설치한다.
     *
     * @param scope the worker / worker
     */
    static void install(final WorkerScope scope) {
        final JsRealm realm = scope.getRealm();
        final DomBridge bridge = scope.getBridge();
        final JsObject g = realm.getGlobalObject();
        final WorkerGlobal global = scope.getGlobal();
        bridge.alias(global, g);
        // self 는 DedicatedWorkerGlobalScope(또는 공유 worker 의 SharedWorkerGlobalScope)의 인스턴스이다(WorkerGlobalScope, EventTarget 을 거쳐 Object 까지
        // 프로토타입 사슬이 이어진다). 전용 worker 는 message 이벤트를, 공유 worker 는 connect 이벤트를 받는다.
        bridge.defineInterface("WorkerGlobalScope", "EventTarget", WorkerGlobal.class, new String[0]);
        DomBridge.InterfaceInfo kind = bridge.defineInterface(scope.isShared() ? "SharedWorkerGlobalScope" : "DedicatedWorkerGlobalScope",
                "WorkerGlobalScope", WorkerGlobal.class, new String[0]);
        g.setPrototypeOf(kind.getPrototype());
        g.defineData("self", g, true, true, true);
        g.defineData("name", Js.str(scope.getName()), true, true, true);
        installEventTarget(realm, bridge, g, global);
        bridge.getEventHandlers().defineBound(g, global, scope.isShared() ? new String[] { "connect" } : new String[] { "message", "messageerror" });
        installOnError(scope, realm, g);
        // 공유 worker 에는 페이지로 가는 암묵 포트가 없으므로 postMessage 가 없다(메시지는 connect 이벤트로 받은 포트로 오간다).
        if (!scope.isShared()) {
            method(realm, g, "postMessage", 1, new NativeFunction.Call() {
                /**
                 * Posts a message to the page that created the worker.
                 * <p>
                 * worker 를 만든 페이지에 메시지를 보낸다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments: the message and the optional transfer list / 인수: 메시지와 선택적 전달 목록
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    ScriptUtil.requireArgs(args, 1, "execute 'postMessage' on 'DedicatedWorkerGlobalScope'");
                    scope.postToParent(Messages.serialize(scope, args[0], Js.arg(args, 1)));
                    return Js.UNDEFINED;
                }
            });
        }
        method(realm, g, "close", 0, new NativeFunction.Call() {
            /**
             * Closes the worker after the current task.
             * <p>
             * 현재 작업이 끝난 뒤 worker 를 닫는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                scope.close();
                return Js.UNDEFINED;
            }
        });
        method(realm, g, "importScripts", 0, new NativeFunction.Call() {
            /**
             * Fetches and runs the scripts in order (synchronously).
             * <p>
             * 스크립트를 순서대로 동기적으로 가져와 실행한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: URLs relative to the worker script / 인수: worker 스크립트 기준의 URL
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (scope.isModule()) {
                    throw JsException.typeError("importScripts() is unavailable in a module worker");
                }
                String[] urls = new String[args.length];
                // 가져오기 전에 모든 URL을 먼저 해석한다. 하나라도 잘못되었으면 아무것도 가져오지 않고 SyntaxError 이다.
                for (int i = 0; i < args.length; i++) {
                    String text = Ops.toString(args[i]);
                    try {
                        urls[i] = UrlUtil.stripFragment(UrlUtil.resolve(scope.getScriptUrl(), text));
                    } catch (RuntimeException e) {
                        throw bridge.domError(DomException.syntax("Failed to execute 'importScripts' on 'WorkerGlobalScope': The URL '" + text
                                + "' is invalid."));
                    }
                }
                // 해석한 URL을 순서대로 가져와 전역 범위에서 실행한다.
                for (int i = 0; i < urls.length; i++) {
                    scope.importScript(urls[i]);
                }
                return Js.UNDEFINED;
            }
        });
        method(realm, g, "structuredClone", 1, new NativeFunction.Call() {
            /**
             * Returns a structured clone of a value.
             * <p>
             * 값의 구조화 복제본을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the value and optional options with a transfer list / 인수: 값과 전달 목록이 든 선택적 옵션
             * @return the copy / 복사본
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                ScriptUtil.requireArgs(args, 1, "execute 'structuredClone' on 'WorkerGlobalScope'");
                ClonedValue cloned = Messages.serialize(scope, args[0], Js.arg(args, 1));
                return StructuredClone.deserialize(r, cloned);
            }
        });
        installLocation(scope, realm, g);
        installNavigator(scope, realm, g);
        installPerformance(realm, g);
        scope.getTimers().install(g);
    }

    /**
     * Installs <code>addEventListener</code>, <code>removeEventListener</code> and <code>dispatchEvent</code> on the global object; they work
     * on the event target of the global scope whatever <code>this</code> is.
     * <p>
     * 전역 객체에 <code>addEventListener</code>, <code>removeEventListener</code>, <code>dispatchEvent</code> 를 설치한다. <code>this</code> 와 무관하게
     * 전역 범위의 이벤트 대상에 작용한다.
     *
     * @param realm  the realm / realm
     * @param bridge the bridge / 브리지
     * @param g      the global object / 전역 객체
     * @param global the event target of the global scope / 전역 범위의 이벤트 대상
     */
    private static void installEventTarget(JsRealm realm, final DomBridge bridge, JsObject g, final WorkerGlobal global) {
        method(realm, g, "addEventListener", 2, new NativeFunction.Call() {
            /**
             * Registers a listener.
             * <p>
             * 리스너를 등록한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.addListener(bridge, global, args);
                return Js.UNDEFINED;
            }
        });
        method(realm, g, "removeEventListener", 2, new NativeFunction.Call() {
            /**
             * Removes a listener.
             * <p>
             * 리스너를 제거한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                DomInterfaces.removeListener(bridge, global, args);
                return Js.UNDEFINED;
            }
        });
        method(realm, g, "dispatchEvent", 1, new NativeFunction.Call() {
            /**
             * Dispatches an event at the global scope.
             * <p>
             * 전역 범위에 이벤트를 전달한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false if cancelled / 취소되었으면 false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return DomInterfaces.dispatch(bridge, global, args);
            }
        });
    }

    /**
     * Installs the <code>onerror</code> property: the handler gets the five arguments of the error event handler (message, source, line, column
     * and the thrown value), and it handles the error when it returns true.
     * <p>
     * <code>onerror</code> 프로퍼티를 설치한다: 핸들러는 오류 이벤트 핸들러의 다섯 인수(메시지, 소스, 줄, 열, 던져진 값)를 받고 true 를 반환하면 오류를
     * 처리한 것이다.
     *
     * @param scope the worker / worker
     * @param realm the realm / realm
     * @param g     the global object / 전역 객체
     */
    private static void installOnError(final WorkerScope scope, JsRealm realm, JsObject g) {
        g.defineAccessor("onerror", realm.newFunction("get onerror", 0, new NativeFunction.Call() {
            /**
             * Returns the error handler.
             * <p>
             * 오류 핸들러를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the handler or null / 핸들러. 없으면 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                JsObject h = scope.getOnerror();
                return h == null ? Js.NULL : h;
            }
        }), realm.newFunction("set onerror", 1, new NativeFunction.Call() {
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
                scope.setOnerror(v.isObject() ? (JsObject) v : null);
                return Js.UNDEFINED;
            }
        }), true, true);
    }

    /**
     * Returns the part of a URL that a <code>location</code> property names.
     * <p>
     * <code>location</code> 프로퍼티가 가리키는 URL 의 부분을 반환한다.
     *
     * @param name the property name / 프로퍼티 이름
     * @param url  the URL / URL
     * @return the text / 문자열
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
     * Installs the read-only <code>location</code> (<code>WorkerLocation</code>): the URL of the worker script.
     * <p>
     * 읽기 전용 <code>location</code>(<code>WorkerLocation</code>)을 설치한다: worker 스크립트의 URL.
     *
     * @param scope the worker / worker
     * @param realm the realm / realm
     * @param g     the global object / 전역 객체
     */
    private static void installLocation(final WorkerScope scope, JsRealm realm, JsObject g) {
        JsObject loc = realm.newObject();
        String[] names = { "href", "origin", "protocol", "host", "hostname", "port", "pathname", "search", "hash" };
        // 위치의 각 부분을 읽기 전용 접근자로 정의한다.
        for (int i = 0; i < names.length; i++) {
            final String part = names[i];
            getter(realm, loc, part, new NativeFunction.Call() {
                /**
                 * Returns the part of the script URL.
                 * <p>
                 * 스크립트 URL의 부분을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the text / 문자열
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.str(locationPart(part, scope.getScriptUrl()));
                }
            });
        }
        method(realm, loc, "toString", 0, new NativeFunction.Call() {
            /**
             * Returns the URL.
             * <p>
             * URL을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the URL / URL
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(scope.getScriptUrl());
            }
        });
        loc.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("WorkerLocation"), false, false, true);
        g.defineData("location", loc, true, true, true);
    }

    /**
     * Installs <code>navigator</code> (<code>WorkerNavigator</code>): the user agent of the component, <code>hardwareConcurrency</code> 1,
     * <code>onLine</code> true and a few constants.
     * <p>
     * <code>navigator</code>(<code>WorkerNavigator</code>)를 설치한다: 컴포넌트의 User-Agent, <code>hardwareConcurrency</code> 1,
     * <code>onLine</code> true, 몇몇 상수.
     *
     * @param scope the worker / worker
     * @param realm the realm / realm
     * @param g     the global object / 전역 객체
     */
    private static void installNavigator(final WorkerScope scope, JsRealm realm, JsObject g) {
        JsObject nav = realm.newObject();
        getter(realm, nav, "userAgent", new NativeFunction.Call() {
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
                return Js.str(scope.getEnvironment().getUserAgent());
            }
        });
        String[][] strings = { { "appCodeName", "Mozilla" }, { "appName", "Netscape" }, { "appVersion", "5.0" }, { "platform", "Java" },
            { "product", "Gecko" }, { "productSub", "20030107" }, { "vendor", "" }, { "vendorSub", "" }, { "language", "en-US" } };
        // 고정 문자열 프로퍼티를 정의한다.
        for (int i = 0; i < strings.length; i++) {
            nav.defineData(strings[i][0], Js.str(strings[i][1]), false, true, true);
        }
        nav.defineData("languages", realm.newArray(new JsValue[] { Js.str("en-US") }), false, true, true);
        nav.defineData("onLine", Js.TRUE, false, true, true);
        nav.defineData("hardwareConcurrency", Js.num(1), false, true, true);
        nav.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("WorkerNavigator"), false, false, true);
        g.defineData("navigator", nav, true, true, true);
    }

    /**
     * Installs the part of <code>performance</code> that workers use: <code>now()</code> and <code>timeOrigin</code>.
     * <p>
     * worker 가 쓰는 <code>performance</code> 의 일부를 설치한다: <code>now()</code>, <code>timeOrigin</code>.
     *
     * @param realm the realm / realm
     * @param g     the global object / 전역 객체
     */
    private static void installPerformance(JsRealm realm, JsObject g) {
        final long origin = System.currentTimeMillis();
        JsObject perf = realm.newObject();
        method(realm, perf, "now", 0, new NativeFunction.Call() {
            /**
             * Returns the milliseconds since the worker started.
             * <p>
             * worker 가 시작된 뒤의 밀리초를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time / 시간
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(System.currentTimeMillis() - origin);
            }
        });
        perf.defineData("timeOrigin", Js.num(origin), false, true, true);
        g.defineData("performance", perf, true, true, true);
    }
}
