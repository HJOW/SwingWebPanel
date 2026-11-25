package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.NamespaceLookup;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * The small and legacy members of the window, the document and the elements that need the page host (HTML5 2014 REC chapters 3.1, 6 and 11
 * and CSSOM View):
 * <ul>
 * <li>window: <code>event</code> (the current event while a listener runs, set by {@link DomBridge#enterEvent(JsValue)}), <code>external</code>
 * (<code>AddSearchProvider</code> does nothing and <code>IsSearchProviderInstalled</code> answers 0), <code>moveTo/moveBy/resizeTo/resizeBy</code>
 * (a component cannot move its window: they do nothing) and the bars <code>locationbar menubar personalbar scrollbars statusbar toolbar</code>
 * (<code>visible</code> is false);</li>
 * <li>document: <code>elementsFromPoint</code> (the topmost element at the point and its ancestors: elements that only lie below it are not
 * listed), <code>applets</code>, <code>clear()</code>, <code>captureEvents()</code>, <code>releaseEvents()</code> (obsolete, do nothing) and the
 * obsolete colour attributes <code>fgColor bgColor linkColor vlinkColor alinkColor</code> that reflect the <code>text bgcolor link vlink
 * alink</code> attributes of the body;</li>
 * <li>node: <code>lookupNamespaceURI</code>, <code>lookupPrefix</code>, <code>isDefaultNamespace</code> ({@link NamespaceLookup});</li>
 * <li>window: <code>crypto.getRandomValues</code> and <code>crypto.randomUUID</code> (no <code>crypto.subtle</code>); <code>window.applicationCache</code> (never cached); <code>navigator.registerProtocolHandler</code> and the other 2014 REC <code>Navigator</code> members (checked and without effect);</li>
 * <li>element: <code>clientTop</code>, <code>clientLeft</code> (the border widths), <code>offsetParent</code> (CSSOM View "offsetParent") and the
 * non-standard <code>scrollIntoViewIfNeeded</code> (the same as <code>scrollIntoView</code>).</li>
 * </ul>
 * Use: <code>new LegacyWebApi(page).install(globalObject)</code> once per page after the interfaces and {@link ElementExtras} exist.
 * <p>
 * 창, 문서, 요소의 작은 멤버와 과거 멤버 중 페이지 호스트가 필요한 것(HTML5 2014 REC 3.1, 6, 11장과 CSSOM View):
 * <ul>
 * <li>window: <code>event</code>(리스너가 도는 동안의 현재 이벤트, {@link DomBridge#enterEvent(JsValue)} 가 지정), <code>external</code>
 * (<code>AddSearchProvider</code> 는 아무것도 하지 않고 <code>IsSearchProviderInstalled</code> 는 0), <code>moveTo/moveBy/resizeTo/resizeBy</code>
 * (컴포넌트는 창을 옮길 수 없으므로 아무것도 하지 않음), 막대 <code>locationbar menubar personalbar scrollbars statusbar toolbar</code>
 * (<code>visible</code> 은 false);</li>
 * <li>document: <code>elementsFromPoint</code>(그 점의 최상위 요소와 조상: 그 아래에만 놓인 요소는 나열하지 않음), <code>applets</code>,
 * <code>clear()</code>, <code>captureEvents()</code>, <code>releaseEvents()</code>(폐기됨, 아무것도 하지 않음)와 body 의 <code>text bgcolor link
 * vlink alink</code> 속성을 반영하는 폐기된 색 속성 <code>fgColor bgColor linkColor vlinkColor alinkColor</code>;</li>
 * <li>node: <code>lookupNamespaceURI</code>, <code>lookupPrefix</code>, <code>isDefaultNamespace</code>({@link NamespaceLookup});</li>
 * <li>window: <code>crypto.getRandomValues</code>, <code>crypto.randomUUID</code>(<code>crypto.subtle</code> 은 없음), <code>window.applicationCache</code>(캐시된 적 없음), <code>navigator.registerProtocolHandler</code> 와 2014 REC <code>Navigator</code> 의 나머지 멤버(검사하고 효과는 없음);</li>
 * <li>element: <code>clientTop</code>, <code>clientLeft</code>(테두리 두께), <code>offsetParent</code>(CSSOM View "offsetParent")와 비표준
 * <code>scrollIntoViewIfNeeded</code>(<code>scrollIntoView</code> 와 같음).</li>
 * </ul>
 * 사용: 인터페이스와 {@link ElementExtras} 가 만들어진 뒤 페이지마다 한 번 <code>new LegacyWebApi(page).install(globalObject)</code>.
 */
final class LegacyWebApi {

    /**
     * The document colour attributes and the body attribute each one reflects.
     * <p>
     * 문서의 색 속성과 각각이 반영하는 body 속성.
     */
    private static final String[][] COLORS = { { "fgColor", "text" }, { "bgColor", "bgcolor" }, { "linkColor", "link" },
        { "vlinkColor", "vlink" }, { "alinkColor", "alink" } };

    /**
     * The names of the bars of the window.
     * <p>
     * 창의 막대 이름.
     */
    private static final String[] BARS = { "locationbar", "menubar", "personalbar", "scrollbars", "statusbar", "toolbar" };

    /**
     * The page whose realm gets the members.
     * <p>
     * 멤버를 받는 realm 이 속한 페이지.
     */
    private final PageScripts page;

    /**
     * The collection returned by <code>document.applets</code> (the same object every time), or null before the first request.
     * <p>
     * <code>document.applets</code> 가 돌려주는 컬렉션(매번 같은 객체). 첫 요청 전에는 null.
     */
    private JsValue applets;

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    LegacyWebApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the element of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 요소를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element element(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Defines an accessor on an object.
     * <p>
     * 객체에 접근자를 정의한다.
     *
     * @param target the object / 객체
     * @param name   the name / 이름
     * @param get    the getter / 읽기
     * @param set    the setter or null / 쓰기. 없으면 null
     */
    private void accessor(JsObject target, String name, NativeFunction.Call get, NativeFunction.Call set) {
        JsRealm r = page.getRealm();
        target.defineAccessor(name, r.newFunction("get " + name, 0, get), set == null ? null : r.newFunction("set " + name, 1, set), true, true);
    }

    /**
     * Defines a method on an object.
     * <p>
     * 객체에 메서드를 정의한다.
     *
     * @param target the object / 객체
     * @param name   the name / 이름
     * @param arity  the declared number of arguments / 선언한 인수 개수
     * @param body   the work / 하는 일
     */
    private void method(JsObject target, String name, int arity, NativeFunction.Call body) {
        target.defineBuiltin(name, page.getRealm().newFunction(name, arity, body));
    }

    /**
     * Returns the computed style of an element, or the initial style when the page has no style engine.
     * <p>
     * 요소의 계산 스타일을 반환한다. 페이지에 스타일 엔진이 없으면 초기 스타일을 반환한다.
     *
     * @param e the element / 요소
     * @return the style / 스타일
     */
    private ComputedStyle styleOf(Element e) {
        StyleResolver sr = page.getEnvironment().getStyleResolver();
        return sr == null ? ComputedStyle.initial() : sr.getComputedStyle(e);
    }

    /**
     * Installs all the members.
     * <p>
     * 모든 멤버를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        installWindow(g);
        installCrypto(g);
        installNavigator(g);
        installApplicationCache(g);
        installDocument();
        installNode();
        installElement();
    }

    /**
     * The source of random numbers of <code>crypto</code> (created at the first use, because seeding it can take a moment).
     * <p>
     * <code>crypto</code> 의 난수 원천(씨앗을 만드는 데 시간이 걸릴 수 있으므로 처음 쓸 때 만든다).
     */
    private java.security.SecureRandom random;

    /**
     * Returns the random source.
     * <p>
     * 난수 원천을 반환한다.
     *
     * @return the source / 원천
     */
    private synchronized java.security.SecureRandom random() {
        if (random == null) {
            random = new java.security.SecureRandom();
        }
        return random;
    }

    /**
     * Installs <code>window.crypto</code> with the two functions that pages use without the rest of Web Crypto: <code>getRandomValues(typedArray)</code>
     * (an integer typed array of at most 65536 bytes is filled with random values from <code>java.security.SecureRandom</code> and returned;
     * a typed array of floating point numbers gives <code>TypeMismatchError</code> and a larger one <code>QuotaExceededError</code>) and
     * <code>randomUUID()</code> (a version 4 UUID). <code>crypto.subtle</code> does not exist: the cryptographic operations of Web Crypto are
     * not implemented.
     * <p>
     * <code>window.crypto</code> 를 설치한다. Web Crypto 의 나머지 없이 페이지가 쓰는 두 함수만 있다: <code>getRandomValues(typedArray)</code>(65536 바이트 이하의
     * 정수 형식 배열을 <code>java.security.SecureRandom</code> 의 난수로 채워 반환한다. 부동 소수점 형식 배열은 <code>TypeMismatchError</code>, 더 큰 배열은
     * <code>QuotaExceededError</code>)와 <code>randomUUID()</code>(버전 4 UUID). <code>crypto.subtle</code> 은 없다: Web Crypto 의 암호 연산은 구현하지 않았다.
     *
     * @param g the global object / 전역 객체
     */
    private void installCrypto(JsObject g) {
        final DomBridge bridge = page.getBridge();
        JsObject crypto = page.getRealm().newObject();
        method(crypto, "getRandomValues", 1, new NativeFunction.Call() {
            /**
             * Fills an integer typed array with random values.
             * <p>
             * 정수 형식 배열을 난수로 채운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (the typed array) / 인수(형식 배열)
             * @return the same array / 같은 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'getRandomValues' on 'Crypto': 1 argument required, but only 0 present.");
                }
                JsValue a = args[0];
                if (TypedArrays.isTypedArray(a) || TypedArrays.isDataView(a)) {
                    ArrayBufferObject buffer = TypedArrays.bufferOf(a);
                    if (buffer.isShared() || buffer.isResizable()) {
                        throw JsException.typeError("Failed to execute 'getRandomValues' on 'Crypto': the view must have fixed, non-shared storage.");
                    }
                }
                ElementType type = a instanceof TypedArrayObject ? ((TypedArrayObject) a).getType() : null;
                boolean integers = type != null && type != ElementType.FLOAT16 && type != ElementType.FLOAT32 && type != ElementType.FLOAT64;
                if (!integers) {
                    throw bridge.domError(new DomException("TypeMismatchError", (short) 17, "The data argument must be an integer-type TypedArray."));
                }
                TypedArrays.ByteView view = TypedArrays.bytesOf(a);
                if (view == null) {
                    return a;
                }
                if (view.length > 65536) {
                    throw bridge.domError(new DomException("QuotaExceededError", (short) 22, "The ArrayBufferView's byte length (" + view.length
                        + ") exceeds the number of bytes of entropy available via this API (65536)."));
                }
                byte[] bytes = new byte[view.length];
                random().nextBytes(bytes);
                System.arraycopy(bytes, 0, view.array, view.offset, view.length);
                return a;
            }
        });
        method(crypto, "randomUUID", 0, new NativeFunction.Call() {
            /**
             * Returns a random version 4 UUID in lower case.
             * <p>
             * 소문자의 무작위 버전 4 UUID 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the UUID / UUID
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                byte[] b = new byte[16];
                random().nextBytes(b);
                b[6] = (byte) ((b[6] & 0x0f) | 0x40);
                b[8] = (byte) ((b[8] & 0x3f) | 0x80);
                StringBuilder sb = new StringBuilder(36);
                // 16 바이트를 8-4-4-4-12 자리의 16진수로 쓴다.
                for (int i = 0; i < 16; i++) {
                    if (i == 4 || i == 6 || i == 8 || i == 10) {
                        sb.append('-');
                    }
                    sb.append(Character.forDigit((b[i] >> 4) & 0xf, 16)).append(Character.forDigit(b[i] & 0xf, 16));
                }
                return Js.str(sb.toString());
            }
        });
        g.defineData("crypto", crypto, true, true, true);
    }

    /**
     * The object of <code>window.applicationCache</code> (HTML5 2014 REC section 5.7, "Offline Web applications"). The component keeps no
     * application cache: the manifest attribute of the <code>html</code> element is ignored, so the status is always
     * <code>UNCACHED</code> (0), <code>update()</code> and <code>swapCache()</code> throw <code>InvalidStateError</code> as the specification
     * says for a document without a cache, and <code>abort()</code> does nothing. The event handler attributes exist but are never fired.
     * <p>
     * <code>window.applicationCache</code> 의 객체(HTML5 2014 REC 5.7절 "Offline Web applications"). 컴포넌트는 응용 프로그램 캐시를 두지 않는다:
     * <code>html</code> 요소의 manifest 속성은 무시하므로 상태는 항상 <code>UNCACHED</code>(0)이고, 캐시가 없는 문서에 대해 규격이 정한 대로
     * <code>update()</code> 와 <code>swapCache()</code> 는 <code>InvalidStateError</code> 를 던지며 <code>abort()</code> 는 아무것도 하지 않는다. 이벤트
     * 핸들러 속성은 있지만 발생시키지 않는다.
     */
    public static final class AppCache extends EventTarget {
        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         */
        AppCache() {
        }

        /**
         * Returns the status of the cache: always <code>UNCACHED</code>.
         * <p>
         * 캐시의 상태를 반환한다: 항상 <code>UNCACHED</code>.
         *
         * @return 0 / 0
         */
        public int getStatus() {
            return 0;
        }
    }

    /**
     * Installs <code>window.applicationCache</code>.
     * <p>
     * <code>window.applicationCache</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installApplicationCache(JsObject g) {
        final DomBridge bridge = page.getBridge();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of the application cache object.
             * <p>
             * 응용 프로그램 캐시 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return o instanceof AppCache ? "ApplicationCache" : null;
            }
        });
        DomBridge.InterfaceInfo info = bridge.defineInterface("ApplicationCache", "EventTarget", AppCache.class, new String[] { "c:UNCACHED=0",
            "c:IDLE=1", "c:CHECKING=2", "c:DOWNLOADING=3", "c:UPDATEREADY=4", "c:OBSOLETE=5", "a:status" });
        bridge.getEventHandlers().define(info.getPrototype(), AppCache.class, new String[] { "checking", "error", "noupdate", "downloading",
            "progress", "updateready", "cached", "obsolete" });
        NativeFunction.Call invalid = new NativeFunction.Call() {
            /**
             * Throws <code>InvalidStateError</code>: there is no application cache to update or swap.
             * <p>
             * <code>InvalidStateError</code> 를 던진다: 갱신하거나 바꿀 응용 프로그램 캐시가 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw bridge.domError(DomException.invalidState("there is no application cache for this document"));
            }
        };
        method(info.getPrototype(), "update", 0, invalid);
        method(info.getPrototype(), "swapCache", 0, invalid);
        method(info.getPrototype(), "abort", 0, new NativeFunction.Call() {
            /**
             * Does nothing: no cache update is running.
             * <p>
             * 아무것도 하지 않는다: 진행 중인 캐시 갱신이 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        final AppCache cache = new AppCache();
        accessor(g, "applicationCache", new NativeFunction.Call() {
            /**
             * Returns the application cache object of the window.
             * <p>
             * 창의 응용 프로그램 캐시 객체를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the object / 객체
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(cache);
            }
        }, null);
    }

    /**
     * The schemes that <code>registerProtocolHandler</code> accepts (HTML5 2014 REC section 6.6.1.3, the whitelisted schemes).
     * <p>
     * <code>registerProtocolHandler</code> 가 받는 스킴(HTML5 2014 REC 6.6.1.3절, 허용 목록).
     */
    private static final String[] PROTOCOL_WHITELIST = { "bitcoin", "geo", "im", "irc", "ircs", "magnet", "mailto", "mms", "news", "nntp", "sip",
        "sms", "smsto", "ssh", "tel", "urn", "webcal", "wtai", "xmpp" };

    /**
     * The MIME types that <code>registerContentHandler</code> refuses (the type blacklist of the same section).
     * <p>
     * <code>registerContentHandler</code> 가 거부하는 MIME 타입(같은 절의 타입 블랙리스트).
     */
    private static final String[] CONTENT_BLACKLIST = { "application/x-www-form-urlencoded", "application/xhtml+xml", "application/xml",
        "image/gif", "image/jpeg", "image/png", "image/svg+xml", "multipart/x-mixed-replace", "text/cache-manifest", "text/css", "text/html",
        "text/ping", "text/plain", "text/xml" };

    /**
     * Adds the members of the 2014 REC <code>Navigator</code> that were missing: <code>taintEnabled()</code>,
     * <code>registerProtocolHandler()</code>, <code>registerContentHandler()</code>, their <code>unregister</code> counterparts and
     * <code>yieldForStorageUpdates()</code>, plus <code>webdriver</code> (false). The handler methods check their arguments as the
     * specification says (whitelisted scheme or <code>web+</code> custom scheme, a <code>%s</code> in the URL, same origin as the page,
     * MIME type not blacklisted) and then do nothing: a component has no user interface to ask for the consent of the user, and the
     * schemes it can navigate to are limited to http, https and ftp.
     * <p>
     * 2014 REC <code>Navigator</code> 에서 빠져 있던 멤버를 더한다: <code>taintEnabled()</code>, <code>registerProtocolHandler()</code>,
     * <code>registerContentHandler()</code>, 그 <code>unregister</code> 짝, <code>yieldForStorageUpdates()</code> 와 <code>webdriver</code>(false). 핸들러
     * 메서드는 규격대로 인수를 검사(허용 스킴이나 <code>web+</code> 사용자 정의 스킴, URL 의 <code>%s</code>, 페이지와 같은 오리진, 블랙리스트에 없는
     * MIME 타입)한 뒤 아무것도 하지 않는다: 컴포넌트에는 사용자의 동의를 구할 화면이 없고 이동할 수 있는 스킴은 http, https, ftp 뿐이다.
     *
     * @param g the global object / 전역 객체
     */
    private void installNavigator(JsObject g) {
        JsValue nv = g.get("navigator");
        if (!(nv instanceof JsObject)) {
            return;
        }
        JsObject nav = (JsObject) nv;
        final DomBridge bridge = page.getBridge();
        nav.defineData("webdriver", Js.bool(false), false, true, true);
        method(nav, "taintEnabled", 0, new NativeFunction.Call() {
            /**
             * Answers false (the specification says it is a constant).
             * <p>
             * false 를 답한다(규격에서 상수이다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return false / false
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.bool(false);
            }
        });
        method(nav, "yieldForStorageUpdates", 0, new NativeFunction.Call() {
            /**
             * Does nothing: the storage of the component is always up to date.
             * <p>
             * 아무것도 하지 않는다: 컴포넌트의 저장소는 항상 최신이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        method(nav, "registerProtocolHandler", 3, new NativeFunction.Call() {
            /**
             * Checks the arguments of the registration and accepts it without effect.
             * <p>
             * 등록의 인수를 검사하고 효과 없이 받아들인다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (scheme, url, title) / 인수(scheme, url, title)
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireArguments(args, 3, "registerProtocolHandler");
                String scheme = Ops.toString(args[0]);
                String url = Ops.toString(args[1]);
                Ops.toString(args[2]);
                checkHandlerUrl(bridge, url);
                String lower = scheme.toLowerCase(java.util.Locale.ENGLISH);
                boolean allowed = lower.startsWith("web+") && lower.length() > 4 && lower.equals(scheme) && lettersOnly(lower.substring(4));
                // 허용 목록의 스킴도 받는다.
                for (int i = 0; i < PROTOCOL_WHITELIST.length && !allowed; i++) {
                    allowed = PROTOCOL_WHITELIST[i].equals(scheme);
                }
                if (!allowed) {
                    throw bridge.domError(DomException.security("the scheme '" + scheme + "' cannot be registered"));
                }
                return Js.UNDEFINED;
            }
        });
        method(nav, "registerContentHandler", 3, new NativeFunction.Call() {
            /**
             * Checks the arguments of the registration and accepts it without effect.
             * <p>
             * 등록의 인수를 검사하고 효과 없이 받아들인다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (mimeType, url, title) / 인수(mimeType, url, title)
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireArguments(args, 3, "registerContentHandler");
                String type = Ops.toString(args[0]).toLowerCase(java.util.Locale.ENGLISH);
                String url = Ops.toString(args[1]);
                Ops.toString(args[2]);
                checkHandlerUrl(bridge, url);
                // 블랙리스트에 있는 타입은 등록할 수 없다.
                for (int i = 0; i < CONTENT_BLACKLIST.length; i++) {
                    if (CONTENT_BLACKLIST[i].equals(type)) {
                        throw bridge.domError(DomException.security("the MIME type '" + type + "' cannot be registered"));
                    }
                }
                return Js.UNDEFINED;
            }
        });
        NativeFunction.Call unregister = new NativeFunction.Call() {
            /**
             * Does nothing: no handler is ever registered.
             * <p>
             * 아무것도 하지 않는다: 등록된 핸들러가 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                requireArguments(args, 2, "unregisterHandler");
                return Js.UNDEFINED;
            }
        };
        method(nav, "unregisterProtocolHandler", 2, unregister);
        method(nav, "unregisterContentHandler", 2, unregister);
    }

    /**
     * Throws the TypeError of WebIDL when too few arguments are passed.
     * <p>
     * 인수가 모자라면 WebIDL 의 TypeError 를 던진다.
     *
     * @param args   the arguments / 인수
     * @param needed the number of required arguments / 필요한 인수 개수
     * @param name   the method name / 메서드 이름
     */
    private static void requireArguments(JsValue[] args, int needed, String name) {
        if (args.length < needed) {
            throw JsException.typeError("Failed to execute '" + name + "' on 'Navigator': " + needed + " arguments required, but only " + args.length
                + " present.");
        }
    }

    /**
     * Tells whether a string consists only of the ASCII letters a to z.
     * <p>
     * 문자열이 ASCII 소문자 a~z 로만 이루어졌는지 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if only lower-case letters / 소문자뿐이면 true
     */
    private static boolean lettersOnly(String s) {
        // 글자마다 a~z 범위를 확인한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the URL template of a handler registration: it must contain <code>%s</code>, be a valid URL, and have the origin of the page.
     * <p>
     * 핸들러 등록의 URL 템플릿을 검사한다: <code>%s</code> 가 있어야 하고 올바른 URL 이어야 하며 페이지의 오리진이어야 한다.
     *
     * @param bridge the bridge / 브리지
     * @param url    the URL template / URL 템플릿
     */
    private void checkHandlerUrl(DomBridge bridge, String url) {
        if (url.indexOf("%s") < 0) {
            throw bridge.domError(DomException.syntax("the URL of a handler must contain %s"));
        }
        String resolved;
        try {
            resolved = UrlUtil.resolve(page.getDocument().getBaseUri(), url);
        } catch (RuntimeException e) {
            throw bridge.domError(DomException.syntax("the URL of the handler is not valid: " + url));
        }
        Origin own = Origin.of(page.getDocument().getOriginUrl());
        // 페이지가 불투명 오리진이거나 http(s) 가 아니면, 또는 다른 오리진의 URL 이면 SecurityError 이다.
        if (own.isOpaque() || !own.isSameOrigin(Origin.of(resolved))) {
            throw bridge.domError(DomException.security("the handler URL must have the origin of the page"));
        }
    }

    /**
     * Installs the window members.
     * <p>
     * 창 멤버를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    private void installWindow(JsObject g) {
        JsRealm realm = page.getRealm();
        // window.event 는 처음에는 undefined 이고 리스너가 도는 동안 DomBridge 가 현재 이벤트로 바꾼다.
        g.defineData("event", Js.UNDEFINED, true, true, true);
        JsObject external = realm.newObject();
        method(external, "AddSearchProvider", 1, new NativeFunction.Call() {
            /**
             * Does nothing: a component has no search provider list.
             * <p>
             * 아무것도 하지 않는다: 컴포넌트에는 검색 제공자 목록이 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        method(external, "IsSearchProviderInstalled", 1, new NativeFunction.Call() {
            /**
             * Answers 0: no search provider is installed.
             * <p>
             * 0 을 답한다: 설치된 검색 제공자가 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return 0 / 0
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(0);
            }
        });
        g.defineData("external", external, true, true, true);
        String[] noops = { "moveTo", "moveBy", "resizeTo", "resizeBy" };
        NativeFunction.Call nothing = new NativeFunction.Call() {
            /**
             * Does nothing: the window of the host cannot be moved or resized by a page.
             * <p>
             * 아무것도 하지 않는다: 호스트의 창은 페이지가 옮기거나 크기를 바꿀 수 없다.
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
        // 창 이동과 크기 조절 메서드는 모두 아무것도 하지 않는다.
        for (int i = 0; i < noops.length; i++) {
            method(g, noops[i], 2, nothing);
        }
        // 창의 막대는 보이지 않는 것으로 답한다(컴포넌트에는 주소 표시줄, 메뉴, 도구 모음이 없다).
        for (int i = 0; i < BARS.length; i++) {
            JsObject bar = realm.newObject();
            accessor(bar, "visible", new NativeFunction.Call() {
                /**
                 * Answers false.
                 * <p>
                 * false 를 답한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return false / false
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return Js.bool(false);
                }
            }, null);
            g.defineData(BARS[i], bar, true, true, true);
        }
    }

    /**
     * Installs the document members.
     * <p>
     * 문서 멤버를 설치한다.
     */
    private void installDocument() {
        final DomBridge bridge = page.getBridge();
        final HostEnvironment env = page.getEnvironment();
        JsObject d = bridge.getInterface("Document").getPrototype();
        method(d, "elementsFromPoint", 2, new NativeFunction.Call() {
            /**
             * Returns the elements at a point of the viewport, the topmost first: the topmost element and its ancestors up to the root element.
             * <p>
             * 뷰포트의 한 점에 있는 요소를 맨 위부터 반환한다: 최상위 요소와 루트 요소까지의 조상.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (x, y) / 인수(x, y)
             * @return the array of elements / 요소 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'elementsFromPoint' on 'Document': 2 arguments required, but only " + args.length
                        + " present.");
                }
                double x = Ops.toNumber(args[0]);
                double y = Ops.toNumber(args[1]);
                // 유한하지 않은 값은 WebIDL 의 double 변환에서 TypeError 이다.
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    throw JsException.typeError("Failed to execute 'elementsFromPoint' on 'Document': The provided double value is non-finite.");
                }
                java.util.List<JsValue> out = new java.util.ArrayList<JsValue>();
                // 최상위 요소에서 루트 쪽으로 올라가며 모은다.
                for (Element e = env.getElementAt(x, y); e != null; e = e.getParentElement()) {
                    out.add(bridge.toJs(e));
                }
                return page.getRealm().newArray(out);
            }
        });
        accessor(d, "applets", new NativeFunction.Call() {
            /**
             * Returns the live collection of the <code>applet</code> elements (obsolete; they are never run).
             * <p>
             * <code>applet</code> 요소의 라이브 컬렉션을 반환한다(폐기됨. 실행하지 않는다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the collection / 컬렉션
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                // [SameObject]: 처음 요청할 때 만든 컬렉션을 매번 돌려준다.
                if (applets == null) {
                    applets = bridge.toJs(page.getDocument().getElementsByTagName("applet"));
                }
                return applets;
            }
        }, null);
        NativeFunction.Call nothing = new NativeFunction.Call() {
            /**
             * Does nothing (an obsolete method of <code>Document</code>).
             * <p>
             * 아무것도 하지 않는다(<code>Document</code> 의 폐기된 메서드).
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
        method(d, "clear", 0, nothing);
        method(d, "captureEvents", 0, nothing);
        method(d, "releaseEvents", 0, nothing);
        // 문서의 색 속성마다 body 의 속성을 읽고 쓰는 접근자를 정의한다.
        for (int i = 0; i < COLORS.length; i++) {
            final String attribute = COLORS[i][1];
            accessor(d, COLORS[i][0], new NativeFunction.Call() {
                /**
                 * Reads the attribute of the body (empty if there is no body or no attribute).
                 * <p>
                 * body 의 속성을 읽는다(body 나 속성이 없으면 빈 문자열).
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the value / 값
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    Element body = bodyOf(page.getDocument());
                    String v = body == null ? null : body.getAttribute(attribute);
                    return Js.str(v == null ? "" : v);
                }
            }, new NativeFunction.Call() {
                /**
                 * Writes the attribute of the body (nothing happens if there is no body).
                 * <p>
                 * body 의 속성을 쓴다(body 가 없으면 아무것도 하지 않는다).
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    Element body = bodyOf(page.getDocument());
                    if (body != null) {
                        JsValue v = Js.arg(args, 0);
                        body.setAttribute(attribute, v.isNull() ? "" : Ops.toString(v));
                    }
                    return Js.UNDEFINED;
                }
            });
        }
    }

    /**
     * Finds the <code>body</code> element (the first <code>body</code> child of the root element).
     * <p>
     * <code>body</code> 요소(루트 요소의 첫 <code>body</code> 자식)를 찾는다.
     *
     * @param doc the document / 문서
     * @return the body or null / body 또는 null
     */
    private static Element bodyOf(Document doc) {
        Element root = doc.getDocumentElement();
        // 루트의 자식 요소 중 첫 body 를 찾는다.
        for (Element c = root == null ? null : root.getFirstElementChild(); c != null; c = c.getNextElementSibling()) {
            if ("body".equals(c.getLocalName())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Installs the namespace lookups of <code>Node</code>.
     * <p>
     * <code>Node</code> 의 네임스페이스 조회를 설치한다.
     */
    private void installNode() {
        JsObject n = page.getBridge().getInterface("Node").getPrototype();
        method(n, "lookupNamespaceURI", 1, new NativeFunction.Call() {
            /**
             * Returns the namespace that a prefix is bound to (null for none).
             * <p>
             * 접두사가 묶인 네임스페이스를 반환한다(없으면 null).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (prefix or null) / 인수(접두사 또는 null)
             * @return the namespace or null / 네임스페이스 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Node node = node(self);
                JsValue p = Js.arg(args, 0);
                String prefix = p.isNullOrUndefined() ? null : Ops.toString(p);
                String ns = NamespaceLookup.locateNamespace(node, prefix == null || prefix.length() == 0 ? null : prefix);
                return ns == null ? Js.NULL : Js.str(ns);
            }
        });
        method(n, "lookupPrefix", 1, new NativeFunction.Call() {
            /**
             * Returns a prefix bound to a namespace (null for none).
             * <p>
             * 네임스페이스에 묶인 접두사를 반환한다(없으면 null).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (namespace or null) / 인수(네임스페이스 또는 null)
             * @return the prefix or null / 접두사 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Node node = node(self);
                JsValue p = Js.arg(args, 0);
                String ns = p.isNullOrUndefined() ? null : Ops.toString(p);
                String prefix = ns == null || ns.length() == 0 ? null : NamespaceLookup.locatePrefix(node, ns);
                return prefix == null ? Js.NULL : Js.str(prefix);
            }
        });
        method(n, "isDefaultNamespace", 1, new NativeFunction.Call() {
            /**
             * Tells whether a namespace is the default namespace as seen from the node.
             * <p>
             * 네임스페이스가 노드에서 본 기본 네임스페이스인지 알려 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (namespace or null) / 인수(네임스페이스 또는 null)
             * @return a boolean / 불리언
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Node node = node(self);
                JsValue p = Js.arg(args, 0);
                String ns = p.isNullOrUndefined() ? null : Ops.toString(p);
                String dflt = NamespaceLookup.locateNamespace(node, null);
                // 빈 문자열은 null 과 같이 "네임스페이스 없음" 이다.
                String wanted = ns == null || ns.length() == 0 ? null : ns;
                return Js.bool(wanted == null ? dflt == null : wanted.equals(dflt));
            }
        });
    }

    /**
     * Returns the node of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 노드를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the node / 노드
     */
    private static Node node(JsValue self) {
        Node n = (Node) DomBridge.targetOf(self, Node.class);
        if (n == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return n;
    }

    /**
     * Installs the element members.
     * <p>
     * 요소 멤버를 설치한다.
     */
    private void installElement() {
        JsObject el = page.getBridge().getInterface("Element").getPrototype();
        accessor(el, "clientTop", new NativeFunction.Call() {
            /**
             * Reads the width of the top border (0 when the element has no box or is inline).
             * <p>
             * 위쪽 테두리 두께를 읽는다(요소에 상자가 없거나 인라인이면 0).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width in pixels / 픽셀 단위 두께
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(borderWidth(element(self), Prop.BORDER_TOP_WIDTH));
            }
        }, null);
        accessor(el, "clientLeft", new NativeFunction.Call() {
            /**
             * Reads the width of the left border (0 when the element has no box or is inline).
             * <p>
             * 왼쪽 테두리 두께를 읽는다(요소에 상자가 없거나 인라인이면 0).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the width in pixels / 픽셀 단위 두께
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(borderWidth(element(self), Prop.BORDER_LEFT_WIDTH));
            }
        }, null);
        accessor(el, "offsetParent", new NativeFunction.Call() {
            /**
             * Returns the nearest positioned ancestor (CSSOM View "offsetParent"), or null.
             * <p>
             * 가장 가까운 위치 지정 조상을 반환한다(CSSOM View "offsetParent"). 없으면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element p = offsetParent(element(self));
                return p == null ? Js.NULL : page.getBridge().toJs(p);
            }
        }, null);
        method(el, "scrollIntoViewIfNeeded", 0, new NativeFunction.Call() {
            /**
             * Calls <code>scrollIntoView()</code> (the non-standard method scrolls only when the element is outside the viewport; this
             * implementation scrolls like <code>scrollIntoView</code>).
             * <p>
             * <code>scrollIntoView()</code> 를 호출한다(비표준 메서드는 요소가 뷰포트 밖일 때만 스크롤하지만 이 구현은
             * <code>scrollIntoView</code> 처럼 스크롤한다).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                element(self);
                JsValue f = ((JsObject) self).get("scrollIntoView");
                if (f.isCallable()) {
                    Ops.call(f, self, new JsValue[0]);
                }
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * The rounded border width of an element in pixels, or 0 for an element without a box or an inline element (CSSOM View
     * <code>clientTop</code>).
     * <p>
     * 요소의 테두리 두께를 반올림한 픽셀 값이며, 상자가 없거나 인라인인 요소는 0(CSSOM View <code>clientTop</code>).
     *
     * @param e    the element / 요소
     * @param prop the id of the border width property / 테두리 두께 프로퍼티 id
     * @return the width / 두께
     */
    private double borderWidth(Element e, int prop) {
        ComputedStyle cs = styleOf(e);
        String display = cs.display();
        // 상자가 없거나(none, contents) 인라인 상자이면 0 이다.
        if ("none".equals(display) || "contents".equals(display) || "inline".equals(display)) {
            return 0;
        }
        return Math.round(cs.px(prop));
    }

    /**
     * Finds the offset parent of an element (CSSOM View): null for the root element, the body, an element with <code>position: fixed</code> and
     * an element without a box; otherwise the nearest ancestor that is positioned, or a <code>td</code>, <code>th</code> or <code>table</code>
     * when the element itself is static, or else the body.
     * <p>
     * 요소의 offsetParent 를 찾는다(CSSOM View): 루트 요소, body, <code>position: fixed</code> 인 요소, 상자가 없는 요소는 null. 그 밖에는 위치가
     * 지정된 가장 가까운 조상이며, 요소 자신이 static 이면 <code>td</code>, <code>th</code>, <code>table</code> 도 해당하고, 없으면 body 이다.
     *
     * @param e the element / 요소
     * @return the offset parent or null / offsetParent 또는 null
     */
    private Element offsetParent(Element e) {
        Element root = e.getOwnerDocument().getDocumentElement();
        ComputedStyle own = styleOf(e);
        // 루트, body, fixed 요소, 상자 없는 요소는 offsetParent 가 없다.
        if (e == root || "body".equals(e.getLocalName()) || "fixed".equals(own.position()) || "none".equals(own.display())) {
            return null;
        }
        boolean isStatic = "static".equals(own.position());
        // 조상을 위로 올라가며 body, 위치가 지정된 요소, (자신이 static 일 때) 표 요소를 찾는다.
        for (Element a = e.getParentElement(); a != null; a = a.getParentElement()) {
            String name = a.getLocalName();
            if ("body".equals(name)) {
                return a;
            }
            if (!"static".equals(styleOf(a).position())) {
                return a;
            }
            if (isStatic && ("td".equals(name) || "th".equals(name) || "table".equals(name))) {
                return a;
            }
        }
        return null;
    }
}
