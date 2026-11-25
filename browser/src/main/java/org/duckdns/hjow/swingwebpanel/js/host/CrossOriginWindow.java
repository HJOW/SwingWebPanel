package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;

/**
 * The window of a browsing context that a script reaches from a document of another origin (<code>window.parent</code>, <code>top</code>,
 * <code>frames[i]</code>, <code>iframe.contentWindow</code>). The HTML Standard lets such a window show only a few members: the references to
 * other windows (<code>window</code>, <code>self</code>, <code>parent</code>, <code>top</code>, <code>frames</code>, <code>length</code>,
 * <code>opener</code>, <code>closed</code>, indexed frames), <code>postMessage</code>, <code>focus</code>, <code>blur</code> and an assignment to
 * <code>location</code>. Every other access throws a <code>SecurityError</code> <code>DOMException</code>. The object is a proxy for a browsing
 * context, so it stays valid when the document of the context is replaced by a navigation.
 * <p>
 * 스크립트가 다른 오리진의 문서에서 닿는 브라우징 문맥의 window(<code>window.parent</code>, <code>top</code>, <code>frames[i]</code>,
 * <code>iframe.contentWindow</code>). HTML 표준은 그런 window 가 몇 가지 멤버만 보여 주게 한다: 다른 window 의 참조(<code>window</code>,
 * <code>self</code>, <code>parent</code>, <code>top</code>, <code>frames</code>, <code>length</code>, <code>opener</code>, <code>closed</code>, 인덱스로 찾는
 * 프레임), <code>postMessage</code>, <code>focus</code>, <code>blur</code>, <code>location</code> 대입. 그 밖의 모든 접근은 <code>SecurityError</code>
 * <code>DOMException</code> 을 던진다. 이 객체는 브라우징 문맥의 프록시이므로 문맥의 문서가 이동으로 바뀌어도 유효하다.
 */
final class CrossOriginWindow extends JsObject {

    /** The names that may be read; the others throw. / 읽을 수 있는 이름. 나머지는 예외를 던진다. */
    private static final String[] READABLE = { "window", "self", "parent", "top", "frames", "length", "opener", "closed", "location",
        "postMessage", "focus", "blur" };

    /** The script host of the document that uses the window. / window 를 쓰는 문서의 스크립트 호스트. */
    private final PageScripts caller;

    /** The frame api of that document. / 그 문서의 프레임 API. */
    private final FrameApi api;

    /** The browsing context that the window stands for. / window 가 대신하는 브라우징 문맥. */
    private final BrowsingContext target;

    /** The cached methods and the location object. / 캐시한 메서드와 location 객체. */
    private JsValue postMessage;
    /** The cached focus function. / 캐시한 focus 함수. */
    private JsValue focus;
    /** The cached blur function. / 캐시한 blur 함수. */
    private JsValue blur;
    /** The cached location object. / 캐시한 location 객체. */
    private JsObject location;

    /**
     * Creates the proxy.
     * <p>
     * 프록시를 만든다.
     *
     * @param caller the script host of the document that reaches the window / window 에 닿는 문서의 스크립트 호스트
     * @param api    the frame api of that document / 그 문서의 프레임 API
     * @param target the browsing context / 브라우징 문맥
     */
    CrossOriginWindow(PageScripts caller, FrameApi api, BrowsingContext target) {
        super((JsObject) null);
        this.caller = caller;
        this.api = api;
        this.target = target;
    }

    /**
     * Returns the class name that scripts see in <code>Object.prototype.toString</code>.
     * <p>
     * <code>Object.prototype.toString</code> 에서 스크립트가 보는 클래스 이름을 반환한다.
     *
     * @return <code>Object</code> / <code>Object</code>
     */
    public String className() {
        return "Object";
    }

    /**
     * Tells whether a name may be read.
     * <p>
     * 이름을 읽을 수 있는지 알려 준다.
     *
     * @param key the key / 키
     * @return true if allowed / 허용이면 true
     */
    private boolean allowed(Object key) {
        // 허용 목록에서 이름을 찾는다.
        for (int i = 0; key instanceof String && i < READABLE.length; i++) {
            if (READABLE[i].equals(key)) {
                return true;
            }
        }
        return index(key) >= 0 && index(key) < target.getChildren().size();
    }

    /**
     * Returns the number that an integer key stands for, or -1.
     * <p>
     * 정수 키가 뜻하는 숫자를 반환한다. 아니면 -1.
     *
     * @param key the key / 키
     * @return the index or -1 / 인덱스 또는 -1
     */
    private static int index(Object key) {
        if (!(key instanceof String)) {
            return -1;
        }
        String s = (String) key;
        if (s.length() == 0 || s.length() > 9) {
            return -1;
        }
        // 숫자로만 이루어진 키만 인덱스이다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return -1;
            }
        }
        return Integer.parseInt(s);
    }

    /**
     * Builds the SecurityError that a forbidden access throws.
     * <p>
     * 금지된 접근이 던지는 SecurityError 를 만든다.
     *
     * @param what the member that was accessed / 접근한 멤버
     * @return the exception / 예외
     */
    private JsException denied(Object what) {
        String origin = api.callerOrigin();
        return caller.getBridge().domError(DomException.security("Blocked a frame with origin \"" + origin
                + "\" from accessing a cross-origin frame. (property '" + String.valueOf(what) + "')"));
    }

    /**
     * Reads a member: the allowed ones, or throws.
     * <p>
     * 멤버를 읽는다: 허용된 것이거나 예외를 던진다.
     *
     * @param key      the key / 키
     * @param receiver the receiver / 수신자
     * @return the value / 값
     */
    public JsValue get(Object key, JsValue receiver) {
        // 심볼과 then 은 undefined 로 응답한다(프라미스 해결과 도구가 던지지 않고 확인할 수 있게 한다).
        if (!(key instanceof String) || "then".equals(key)) {
            return Js.UNDEFINED;
        }
        String k = (String) key;
        JsRealm realm = caller.getRealm();
        if ("window".equals(k) || "self".equals(k) || "frames".equals(k)) {
            return api.windowOf(target);
        }
        if ("parent".equals(k)) {
            BrowsingContext p = target.getParent();
            return api.windowOf(p == null ? target : p);
        }
        if ("top".equals(k)) {
            return api.windowOf(target.getTop());
        }
        if ("length".equals(k)) {
            return Js.num(target.getChildren().size());
        }
        if ("opener".equals(k)) {
            return Js.NULL;
        }
        if ("closed".equals(k)) {
            return Js.bool(target.isClosed());
        }
        if ("location".equals(k)) {
            return location(realm);
        }
        if ("postMessage".equals(k)) {
            if (postMessage == null) {
                postMessage = realm.newFunction("postMessage", 2, new NativeFunction.Call() {
                    /**
                     * Posts a message to the window of the other origin.
                     * <p>
                     * 다른 오리진의 window 로 메시지를 보낸다.
                     *
                     * @param r    the realm / realm
                     * @param self this value / this 값
                     * @param args message, target origin and optional transfer list / 메시지, 대상 오리진, 선택적 전달 목록
                     * @return undefined / undefined
                     */
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        if (args.length < 2) {
                            throw JsException.typeError("Failed to execute 'postMessage' on 'Window': 2 arguments required, but only " + args.length
                                    + " present.");
                        }
                        api.postMessageTo(target, args[0], Ops.toString(args[1]), Js.arg(args, 2));
                        return Js.UNDEFINED;
                    }
                });
            }
            return postMessage;
        }
        if ("focus".equals(k) || "blur".equals(k)) {
            if (focus == null) {
                focus = realm.newFunction("focus", 0, new NativeFunction.Call() {
                    /**
                     * Does nothing (a component has no window focus to move between frames).
                     * <p>
                     * 아무것도 하지 않는다(컴포넌트에는 프레임 사이에서 옮길 창 포커스가 없다).
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
                blur = focus;
            }
            return focus;
        }
        int i = index(k);
        List<BrowsingContext> kids = target.getChildren();
        if (i >= 0 && i < kids.size()) {
            return api.windowOf(kids.get(i));
        }
        throw denied(k);
    }

    /**
     * Returns the restricted <code>location</code> object: the address cannot be read, only assigned.
     * <p>
     * 제한된 <code>location</code> 객체를 반환한다: 주소는 읽을 수 없고 대입만 할 수 있다.
     *
     * @param realm the realm of the caller / 호출자의 realm
     * @return the object / 객체
     */
    private JsObject location(JsRealm realm) {
        if (location == null) {
            JsObject l = new LocationObject(realm);
            final JsObject self = l;
            l.defineAccessor("href", realm.newFunction("get href", 0, new NativeFunction.Call() {
                /**
                 * Refuses to read the address of another origin.
                 * <p>
                 * 다른 오리진의 주소를 읽지 않는다.
                 *
                 * @param r    the realm / realm
                 * @param s    this value / this 값
                 * @param args arguments / 인수
                 * @return never / 반환하지 않음
                 */
                public JsValue call(JsRealm r, JsValue s, JsValue[] args) {
                    throw denied("location.href");
                }
            }), realm.newFunction("set href", 1, new NativeFunction.Call() {
                /**
                 * Navigates the context that the window stands for.
                 * <p>
                 * window 가 대신하는 문맥을 이동시킨다.
                 *
                 * @param r    the realm / realm
                 * @param s    this value / this 값
                 * @param args the new address / 새 주소
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue s, JsValue[] args) {
                    api.navigate(target, Ops.toString(Js.arg(args, 0)), false);
                    return Js.UNDEFINED;
                }
            }), true, true);
            l.defineData("replace", realm.newFunction("replace", 1, new NativeFunction.Call() {
                /**
                 * Navigates the context and replaces its history entry.
                 * <p>
                 * 문맥을 이동시키고 이력 항목을 바꾼다.
                 *
                 * @param r    the realm / realm
                 * @param s    this value / this 값
                 * @param args the new address / 새 주소
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue s, JsValue[] args) {
                    api.navigate(target, Ops.toString(Js.arg(args, 0)), true);
                    return Js.UNDEFINED;
                }
            }), true, true, true);
            location = self;
        }
        return location;
    }

    /**
     * Writes a member: only an assignment to <code>location</code> is allowed.
     * <p>
     * 멤버를 쓴다: <code>location</code> 대입만 허용된다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / 수신자
     * @return true if it was written / 썼으면 true
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        if ("location".equals(key)) {
            api.navigate(target, Ops.toString(value), false);
            return true;
        }
        throw denied(key);
    }

    /**
     * Returns the descriptor of an allowed member and throws for the others.
     * <p>
     * 허용된 멤버의 설명자를 반환하고 나머지는 예외를 던진다.
     *
     * @param key the key / 키
     * @return the descriptor / 설명자
     */
    public PropertyDescriptor getOwnProperty(Object key) {
        if (!allowed(key)) {
            if (key instanceof String) {
                throw denied(key);
            }
            return null;
        }
        return PropertyDescriptor.data(get(key, this), false, false, true);
    }

    /**
     * Tells whether an allowed member exists and throws for the others.
     * <p>
     * 허용된 멤버가 있는지 알려 주고 나머지는 예외를 던진다.
     *
     * @param key the key / 키
     * @return true for an allowed member / 허용된 멤버이면 true
     */
    public boolean hasProperty(Object key) {
        if (!allowed(key)) {
            if (key instanceof String) {
                throw denied(key);
            }
            return false;
        }
        return true;
    }

    /**
     * Tells whether an allowed member exists and throws for the others.
     * <p>
     * 허용된 멤버가 있는지 알려 주고 나머지는 예외를 던진다.
     *
     * @param key the key / 키
     * @return true for an allowed member / 허용된 멤버이면 true
     */
    public boolean hasOwnProperty(Object key) {
        return hasProperty(key);
    }

    /**
     * Refuses to define members.
     * <p>
     * 멤버 정의를 거부한다.
     *
     * @param key  the key / 키
     * @param desc the descriptor / 설명자
     * @return never / 반환하지 않음
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        throw denied(key);
    }

    /**
     * Refuses to delete members.
     * <p>
     * 멤버 삭제를 거부한다.
     *
     * @param key the key / 키
     * @return never / 반환하지 않음
     */
    public boolean delete(Object key) {
        throw denied(key);
    }

    /**
     * Returns the names of the members that may be seen.
     * <p>
     * 볼 수 있는 멤버의 이름을 반환한다.
     *
     * @return the keys / 키
     */
    public List<Object> ownKeys() {
        List<Object> keys = new ArrayList<Object>();
        int count = target.getChildren().size();
        // 인덱스로 찾는 프레임 이름을 먼저 나열한다.
        for (int i = 0; i < count; i++) {
            keys.add(String.valueOf(i));
        }
        // 이어서 읽을 수 있는 이름을 나열한다.
        for (int i = 0; i < READABLE.length; i++) {
            keys.add(READABLE[i]);
        }
        return keys;
    }

    /**
     * The prototype of a cross-origin window is hidden.
     * <p>
     * 다른 오리진 window 의 프로토타입은 숨긴다.
     *
     * @return null / null
     */
    public JsObject getPrototypeOf() {
        return null;
    }

    /**
     * The prototype of a cross-origin window cannot be changed.
     * <p>
     * 다른 오리진 window 의 프로토타입은 바꿀 수 없다.
     *
     * @param proto the new prototype / 새 프로토타입
     * @return false / false
     */
    public boolean setPrototypeOf(JsObject proto) {
        return false;
    }

    /**
     * A cross-origin window cannot be extended.
     * <p>
     * 다른 오리진 window 는 확장할 수 없다.
     *
     * @return false / false
     */
    public boolean isExtensible() {
        return false;
    }
}
