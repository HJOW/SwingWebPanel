package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The window relations of nested browsing contexts for the scripts of one document (plan item P20-02, HTML5 REC 2014-10-28 5.1-5.2):
 * <code>window.parent</code>, <code>top</code>, <code>frames</code>, <code>length</code>, <code>frames[i]</code> and named frames,
 * <code>frameElement</code>, <code>window.name</code>, and <code>contentWindow</code> and <code>contentDocument</code> of
 * <code>iframe</code> and <code>frame</code> elements. A {@link WindowProxy} routes access to the current same-origin global object or to a
 * {@link CrossOriginWindow} exposing the allowed members. Documents that inherit the origin of their creator
 * (<code>about:blank</code>, <code>srcdoc</code>) are of the same origin as the creator; a sandbox without <code>allow-same-origin</code> makes
 * the origin opaque, so the document cannot reach any other window.
 * <p>
 * 한 문서의 스크립트를 위한 중첩 브라우징 문맥의 window 관계(계획 P20-02, HTML5 REC 2014-10-28 5.1-5.2): <code>window.parent</code>, <code>top</code>,
 * <code>frames</code>, <code>length</code>, <code>frames[i]</code> 와 이름 있는 프레임, <code>frameElement</code>, <code>window.name</code>,
 * <code>iframe</code>/<code>frame</code> 요소의 <code>contentWindow</code>, <code>contentDocument</code>. {@link WindowProxy}는 현재 같은 오리진
 * 전역 객체 또는 허용한 멤버를 노출하는 {@link CrossOriginWindow}로 접근을 연결한다.
 * 생성자의 오리진을 물려받는 문서(<code>about:blank</code>, <code>srcdoc</code>)는 생성자와 같은 오리진이고,
 * <code>allow-same-origin</code> 이 없는 샌드박스는 오리진을 불투명하게 하므로 그 문서는 다른 어떤 window 에도 닿을 수 없다.
 */
final class FrameApi {

    /** The script host of the document. / 문서의 스크립트 호스트. */
    private final PageScripts page;

    /** Restricted views for this caller, including their cached functions. / 캐시된 함수가 들어 있는 이 호출자의 제한 객체. */
    private final Map<BrowsingContext, WeakReference<CrossOriginWindow>> proxies =
            new WeakHashMap<BrowsingContext, WeakReference<CrossOriginWindow>>();

    /** The keys of the frame properties that this api put on the global object. / 이 API 가 전역 객체에 둔 프레임 프로퍼티의 키. */
    private final List<String> childKeys = new ArrayList<String>();

    /** The window name of a top-level document (frames keep theirs in the context). / 최상위 문서의 window 이름(프레임은 문맥에 둔다). */
    private String ownName = "";

    /**
     * Creates the api of a document.
     * <p>
     * 문서의 API 를 만든다.
     *
     * @param page the script host of the document / 문서의 스크립트 호스트
     */
    FrameApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the browsing context of the document.
     * <p>
     * 문서의 브라우징 문맥을 반환한다.
     *
     * @return the context or null if the host has none / 문맥. 호스트에 없으면 null
     */
    private BrowsingContext context() {
        return page.getEnvironment().getBrowsingContext();
    }

    /**
     * Returns the origin of the document that uses this api, serialized for messages (<code>null</code> if opaque).
     * <p>
     * 이 API 를 쓰는 문서의 오리진을 메시지용으로 직렬화해 반환한다(불투명이면 <code>null</code>).
     *
     * @return the serialized origin / 직렬화된 오리진
     */
    String callerOrigin() {
        Origin o = Origin.of(page.getDocument().getOriginUrl());
        return o.isOpaque() ? "null" : o.serialize();
    }

    /**
     * Tells whether a context has the origin of this document (and may be used directly).
     * <p>
     * 문맥이 이 문서와 같은 오리진인지(그래서 직접 쓸 수 있는지) 알려 준다.
     *
     * @param c the context / 문맥
     * @return true if same-origin / 같은 오리진이면 true
     */
    boolean sameOrigin(BrowsingContext c) {
        if (c == null) {
            return false;
        }
        if (c == context()) {
            return true;
        }
        return Origin.of(page.getDocument().getOriginUrl()).isSameOrigin(Origin.of(c.getOriginUrl()));
    }

    /**
     * Returns the stable window identity of a browsing context.
     * <p>
     * 브라우징 문맥의 탐색 중에도 유지되는 창 객체를 반환한다.
     *
     * @param c the context / 문맥
     * @return the window object, or null for a missing context / window 객체. 문맥이 없으면 null
     */
    JsValue windowOf(BrowsingContext c) {
        return c == null ? Js.NULL : WindowProxy.forContext(c);
    }

    /**
     * Resolves the current target for an operation on a window identity.
     * <p>
     * 창 객체에 대한 연산이 닿는 현재 문서를 확인한다.
     *
     * @param c target browsing context / 대상 브라우징 문맥
     * @return the current global object or a restricted view / 현재 전역 객체 또는 제한 객체
     */
    JsObject windowTarget(BrowsingContext c) {
        PageScripts scripts = c.getScripts();
        // 문맥의 현재 문서에 맞춰 오리진을 다시 검사한다.
        if (scripts != null && !scripts.isDisposed() && !c.isClosed() && sameOrigin(c)) {
            return scripts.getRealm().getGlobalObject();
        }
        WeakReference<CrossOriginWindow> ref = proxies.get(c);
        CrossOriginWindow proxy = ref == null ? null : ref.get();
        if (proxy == null) {
            proxy = new CrossOriginWindow(page, this, c);
            proxies.put(c, new WeakReference<CrossOriginWindow>(proxy));
        }
        return proxy;
    }

    /**
     * Wraps an element or document of another document of the page for the scripts of this one: with the bridge of the document that owns
     * it when that document has scripts, otherwise with the bridge of this document.
     * <p>
     * 페이지의 다른 문서의 요소나 문서를 이 문서의 스크립트를 위해 감싼다: 소유 문서에 스크립트가 있으면 그 문서의 브리지로, 없으면 이 문서의 브리지로.
     *
     * @param owner  the context that owns the object / 객체를 소유한 문맥
     * @param object the DOM object / DOM 객체
     * @return the JavaScript object / JavaScript 객체
     */
    private JsValue wrap(BrowsingContext owner, Object object) {
        PageScripts s = owner == null ? null : owner.getScripts();
        DomBridge bridge = s != null && !s.isDisposed() && sameOrigin(owner) ? s.getBridge() : page.getBridge();
        return bridge.toJs(object);
    }

    /**
     * Installs the properties of the window: <code>parent</code>, <code>top</code>, <code>length</code>, <code>frameElement</code>,
     * <code>name</code>, <code>opener</code>, the frames by index and name, and the members of the frame elements.
     * <p>
     * window 의 프로퍼티를 설치한다: <code>parent</code>, <code>top</code>, <code>length</code>, <code>frameElement</code>, <code>name</code>,
     * <code>opener</code>, 인덱스와 이름으로 찾는 프레임, 프레임 요소의 멤버.
     *
     * @param g the global object / 전역 객체
     */
    void install(final JsObject g) {
        final JsRealm realm = page.getRealm();
        replaceable(g, "parent", new NativeFunction.Call() {
            /**
             * Returns the parent window, or the window itself for the top-level document.
             * <p>
             * 부모 window 를 반환한다. 최상위 문서이면 자기 자신.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the window / window
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BrowsingContext c = context();
                BrowsingContext p = c == null ? null : c.getParent();
                return p == null ? r.getGlobalThisValue() : windowOf(p);
            }
        });
        replaceable(g, "top", new NativeFunction.Call() {
            /**
             * Returns the top-level window.
             * <p>
             * 최상위 window 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the window / window
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BrowsingContext c = context();
                return c == null ? r.getGlobalThisValue() : windowOf(c.getTop());
            }
        });
        g.defineAccessor("length", realm.newFunction("get length", 0, new NativeFunction.Call() {
            /**
             * Returns the number of frames of the document.
             * <p>
             * 문서의 프레임 수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number / 개수
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BrowsingContext c = context();
                return Js.num(c == null ? 0 : c.getChildren().size());
            }
        }), null, true, true);
        g.defineAccessor("frameElement", realm.newFunction("get frameElement", 0, new NativeFunction.Call() {
            /**
             * Returns the frame element of the document when the parent has the same origin, otherwise null.
             * <p>
             * 부모가 같은 오리진일 때 문서의 프레임 요소를 반환하고 아니면 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the element or null / 요소 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BrowsingContext c = context();
                BrowsingContext p = c == null ? null : c.getParent();
                Element e = c == null ? null : c.getFrameElement();
                if (p == null || e == null || !sameOrigin(p)) {
                    return Js.NULL;
                }
                return wrap(p, e);
            }
        }), null, true, true);
        g.defineAccessor("name", realm.newFunction("get name", 0, new NativeFunction.Call() {
            /**
             * Returns the name of the browsing context.
             * <p>
             * 브라우징 문맥의 이름을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the name / 이름
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BrowsingContext c = context();
                return Js.str(c == null ? ownName : c.getName());
            }
        }), realm.newFunction("set name", 1, new NativeFunction.Call() {
            /**
             * Sets the name of the browsing context.
             * <p>
             * 브라우징 문맥의 이름을 정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args the name / 이름
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String n = Ops.toString(Js.arg(args, 0));
                BrowsingContext c = context();
                if (c == null) {
                    ownName = n;
                } else {
                    c.setName(n);
                }
                return Js.UNDEFINED;
            }
        }), true, true);
        g.defineData("opener", Js.NULL, true, true, true);
        g.defineData("open", realm.newFunction("open", 0, new NativeFunction.Call() {
            /**
             * Opens a window: the target selects an existing browsing context (a frame of the page, the page itself, its parent or its top),
             * which navigates to the address; a target that would need a new window gives null because a component has no windows.
             * <p>
             * 창을 연다: 대상이 기존 브라우징 문맥(페이지의 프레임, 페이지 자신, 부모, 최상위)을 고르고 그 문맥이 주소로 이동한다. 새 창이 필요한 대상은
             * 컴포넌트에 창이 없으므로 null 을 준다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args address, target and features (ignored) / 주소, 대상, 기능(무시)
             * @return the window or null / window 또는 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                String url = args.length > 0 && args[0] != Js.UNDEFINED ? Ops.toString(args[0]) : "";
                String target = args.length > 1 && args[1] != Js.UNDEFINED ? Ops.toString(args[1]) : "_blank";
                BrowsingContext c = context();
                if (c == null) {
                    return Js.NULL;
                }
                String absolute = "";
                // 주소가 있으면 문서의 기준 주소로 해석한다. 해석할 수 없으면 SyntaxError 이다.
                if (url.length() > 0) {
                    try {
                        absolute = UrlUtil.resolve(page.getDocument().getBaseUri(), url);
                    } catch (RuntimeException e) {
                        throw page.getBridge().domError(DomException.syntax("Failed to execute 'open' on 'Window': Unable to open a window with invalid URL '"
                                + url + "'."));
                    }
                }
                BrowsingContext opened = c.open(absolute, target);
                return opened == null ? Js.NULL : windowOf(opened);
            }
        }), true, false, true);
        installElementMembers();
        refresh();
    }

    /**
     * Defines an accessor that a script may replace by assigning (the <code>Replaceable</code> attributes of the standard): an assignment
     * turns it into an ordinary data property.
     * <p>
     * 스크립트가 대입으로 바꿀 수 있는 접근자를 정의한다(표준의 <code>Replaceable</code> 속성): 대입하면 보통의 데이터 프로퍼티가 된다.
     *
     * @param g    the global object / 전역 객체
     * @param name the name / 이름
     * @param get  the getter / getter
     */
    private void replaceable(final JsObject g, final String name, NativeFunction.Call get) {
        JsRealm realm = page.getRealm();
        g.defineAccessor(name, realm.newFunction("get " + name, 0, get), realm.newFunction("set " + name, 1, new NativeFunction.Call() {
            /**
             * Replaces the property by a data property with the assigned value.
             * <p>
             * 프로퍼티를 대입한 값을 가진 데이터 프로퍼티로 바꾼다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args the value / 값
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                g.delete(name);
                g.defineData(name, Js.arg(args, 0), true, true, true);
                return Js.UNDEFINED;
            }
        }), true, true);
    }

    /**
     * Exposes frame names that do not conflict with global properties. The WindowProxy handles numeric indices.
     * <p>
     * 전역 속성과 충돌하지 않는 프레임 이름을 노출한다. 숫자 인덱스는 WindowProxy에서 처리한다.
     */
    void refresh() {
        JsObject g = page.getRealm().getGlobalObject();
        // 이전에 둔 프레임 프로퍼티를 모두 지운다.
        for (int i = 0; i < childKeys.size(); i++) {
            g.delete(childKeys.get(i));
        }
        childKeys.clear();
        BrowsingContext c = context();
        List<BrowsingContext> kids = c == null ? new ArrayList<BrowsingContext>() : c.getChildren();
        // 프레임 이름이 예약 인덱스나 다른 전역 속성과 충돌하지 않으면 접근자를 정의한다.
        for (int i = 0; i < kids.size(); i++) {
            String n = kids.get(i).getName();
            if (n != null && n.length() > 0 && WindowProxy.index(n) < 0 && !g.hasOwnProperty(n)) {
                define(g, n, kids.get(i));
            }
        }
    }

    /**
     * Defines the accessor of one frame on the global object.
     * <p>
     * 전역 객체에 프레임 하나의 접근자를 정의한다.
     *
     * @param g         the global object / 전역 객체
     * @param key       the property name / 프로퍼티 이름
     * @param child     the context / 문맥
     */
    private void define(JsObject g, String key, final BrowsingContext child) {
        g.defineAccessor(key, page.getRealm().newFunction("get " + key, 0, new NativeFunction.Call() {
            /**
             * Returns the window of the frame.
             * <p>
             * 프레임의 window 를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the window / window
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return windowOf(child);
            }
        }), null, false, true);
        childKeys.add(key);
    }

    /**
     * Adds <code>contentWindow</code> and <code>contentDocument</code> to the interfaces of the frame elements.
     * <p>
     * 프레임 요소의 인터페이스에 <code>contentWindow</code> 와 <code>contentDocument</code> 를 더한다.
     */
    private void installElementMembers() {
        DomBridge bridge = page.getBridge();
        String[] names = { "HTMLIFrameElement", "HTMLFrameElement" };
        // 두 인터페이스의 프로토타입에 같은 접근자를 정의한다.
        for (int i = 0; i < names.length; i++) {
            DomBridge.InterfaceInfo info = bridge.getInterface(names[i]);
            if (info == null) {
                continue;
            }
            JsObject proto = info.getPrototype();
            JsRealm realm = page.getRealm();
            proto.defineAccessor("contentWindow", realm.newFunction("get contentWindow", 0, new NativeFunction.Call() {
                /**
                 * Returns the window of the nested browsing context of the element.
                 * <p>
                 * 요소의 중첩 브라우징 문맥의 window 를 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the window, or null if the element is not in a document / window. 요소가 문서에 없으면 null
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    BrowsingContext child = childOf(element(self));
                    return child == null ? Js.NULL : windowOf(child);
                }
            }), null, true, true);
            proto.defineAccessor("contentDocument", realm.newFunction("get contentDocument", 0, new NativeFunction.Call() {
                /**
                 * Returns the document of the nested browsing context when it has the origin of this document, otherwise null.
                 * <p>
                 * 중첩 브라우징 문맥의 문서를 이 문서와 같은 오리진일 때 반환하고 아니면 null.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the document or null / 문서 또는 null
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    BrowsingContext child = childOf(element(self));
                    Document d = child == null ? null : child.getDocument();
                    if (d == null || !sameOrigin(child)) {
                        return Js.NULL;
                    }
                    return wrap(child, d);
                }
            }), null, true, true);
        }
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
     * Finds the browsing context of a frame element: the child of the context whose document contains the element.
     * <p>
     * 프레임 요소의 브라우징 문맥을 찾는다: 요소를 포함한 문서를 가진 문맥의 자식.
     *
     * @param e the frame element / 프레임 요소
     * @return the context, or null if the element is not part of a document with frames / 문맥. 요소가 프레임을 가진 문서의 일부가 아니면 null
     */
    private BrowsingContext childOf(Element e) {
        BrowsingContext c = context();
        BrowsingContext owner = c == null || e.getOwnerDocument() == null ? null : byDocument(c.getTop(), e.getOwnerDocument(), 0);
        if (owner == null) {
            return null;
        }
        List<BrowsingContext> kids = owner.getChildren();
        // 문서의 프레임 중 이 요소에 속한 것을 찾는다.
        for (int i = 0; i < kids.size(); i++) {
            if (kids.get(i).getFrameElement() == e) {
                return kids.get(i);
            }
        }
        return null;
    }

    /**
     * Finds the context that shows a DOM document, below a context.
     * <p>
     * 한 문맥 아래에서 DOM 문서를 보여 주는 문맥을 찾는다.
     *
     * @param root  where to start / 시작할 문맥
     * @param doc   the document / 문서
     * @param depth the current depth, which limits the search / 현재 깊이. 탐색을 제한한다
     * @return the context or null / 문맥 또는 null
     */
    private static BrowsingContext byDocument(BrowsingContext root, Document doc, int depth) {
        if (root == null || depth > 16) {
            return null;
        }
        if (root.getDocument() == doc) {
            return root;
        }
        List<BrowsingContext> kids = root.getChildren();
        // 자식 문맥에서 깊이 우선으로 찾는다.
        for (int i = 0; i < kids.size(); i++) {
            BrowsingContext found = byDocument(kids.get(i), doc, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Navigates the context of a window that this document reached (an assignment to <code>location</code> of a cross-origin window).
     * <p>
     * 이 문서가 닿은 window 의 문맥을 이동시킨다(다른 오리진 window 의 <code>location</code> 대입).
     *
     * @param target  the context / 문맥
     * @param url     the address, resolved against the document of this page / 주소(이 페이지의 문서를 기준으로 해석)
     * @param replace true to replace the history entry / 이력 항목을 바꾸려면 true
     */
    void navigate(BrowsingContext target, String url, boolean replace) {
        String absolute;
        try {
            absolute = UrlUtil.resolve(page.getDocument().getBaseUri(), url);
        } catch (RuntimeException e) {
            throw page.getBridge().domError(DomException.syntax("Failed to set the 'href' property on 'Location': '" + url + "' is not a valid URL."));
        }
        target.navigate(absolute, replace);
    }

    /**
     * Posts a message of a script of this document to the window of another browsing context (<code>postMessage</code> of a window of
     * another origin): the message is cloned here, the target origin is checked against the document that receives it, and the
     * <code>message</code> event with the origin of this document is queued for that document.
     * <p>
     * 이 문서의 스크립트가 보낸 메시지를 다른 브라우징 문맥의 window 로 보낸다(다른 오리진 window 의 <code>postMessage</code>): 메시지를 여기서 복제하고,
     * 대상 오리진을 받는 문서에 대해 검사하고, 이 문서의 오리진을 가진 <code>message</code> 이벤트를 그 문서의 큐에 넣는다.
     *
     * @param target       the receiving context / 받는 문맥
     * @param message      the message / 메시지
     * @param targetOrigin <code>*</code>, <code>/</code> or an absolute URL / <code>*</code>, <code>/</code>, 절대 URL
     * @param transfer     the transfer argument / 전달 인수
     */
    void postMessageTo(BrowsingContext target, JsValue message, String targetOrigin, JsValue transfer) {
        PageScripts receiver = target.getScripts();
        PageScripts sender = PageScripts.messageSender(page);
        String senderUrl = sender.getDocument().getOriginUrl();
        // 대상 오리진은 받는 문서의 오리진과 비교하며 문법이 틀리면 SyntaxError 이다.
        if (!"*".equals(targetOrigin) && !"/".equals(targetOrigin)) {
            boolean valid;
            try {
                valid = UrlUtil.getScheme(targetOrigin) != null;
                UrlUtil.parse(targetOrigin);
            } catch (RuntimeException e) {
                valid = false;
            }
            if (!valid) {
                throw page.getBridge().domError(DomException.syntax("Failed to execute 'postMessage' on 'Window': Invalid target origin '"
                        + targetOrigin + "' in a call to 'postMessage'."));
            }
        }
        org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue cloned = sender.messaging().cloneForPost(message, transfer);
        if (receiver == null || receiver.isDisposed()) {
            return;
        }
        receiver.messaging().receiveFromOther(targetOrigin, cloned, Origin.of(senderUrl).serialize(), senderUrl,
                sender.getRealm().getGlobalThisValue());
    }
}
