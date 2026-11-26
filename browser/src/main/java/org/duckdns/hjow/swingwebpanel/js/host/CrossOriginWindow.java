package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.net.Origin;

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
    private static final String[] READABLE = { "window", "self", "location", "close", "closed", "focus", "blur", "frames",
        "length", "top", "opener", "parent", "postMessage" };

    /** The property keys that resolve to undefined for cross-origin probes. / 교차 오리진에서 undefined를 반환하는 키. */
    private static final Object[] FALLBACK = { "then", WellKnownSymbols.TO_STRING_TAG, WellKnownSymbols.HAS_INSTANCE,
        WellKnownSymbols.IS_CONCAT_SPREADABLE };

    /** The script host of the document that uses the window. / window 를 쓰는 문서의 스크립트 호스트. */
    private final PageScripts caller;

    /** The frame api of that document. / 그 문서의 프레임 API. */
    private final FrameApi api;

    /** The browsing context that the window stands for. / window 가 대신하는 브라우징 문맥. */
    private final BrowsingContext target;

    /** Stable cross-origin functions and accessors for this caller. / 호출자에게 보이는 고정 교차 오리진 함수와 접근자. */
    private final Map<String, Property> properties = new HashMap<String, Property>();
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
     * Finds the first matching live named child of the target when it shares the target's origin.
     * <p>
     * 대상과 같은 오리진에서 현재 이름이 일치하는 첫 하위 창을 찾는다.
     *
     * @param key the proposed child name / 하위 창의 이름
     * @return the child context or null / 하위 문맥 또는 null
     */
    private BrowsingContext namedChild(String key) {
        if (key.length() == 0) {
            return null;
        }
        List<BrowsingContext> children = target.getChildren();
        // 같은 이름이 여러 개면 첫 하위 창에 대해서만 오리진을 검사한다.
        for (int i = 0; i < children.size(); i++) {
            BrowsingContext child = children.get(i);
            if (key.equals(child.getName())) {
                return Origin.of(target.getOriginUrl()).isSameOrigin(Origin.of(child.getOriginUrl())) ? child : null;
            }
        }
        return null;
    }

    /**
     * Reads a regular cross-origin member through its own property slot.
     * <p>
     * 교차 오리진 속성을 자체 속성 슬롯을 통해 읽는다.
     *
     * @param key the property key / 속성 키
     * @param receiver the receiver / 수신자
     * @return the exposed value / 노출되는 값
     */
    public JsValue get(Object key, JsValue receiver) {
        Property slot = getOwnPropertySlot(key);
        return slot.accessor ? Ops.call(slot.getter, receiver, new JsValue[0]) : slot.value;
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
     * Reads the current value of a member in the cross-origin allowlist.
     * <p>
     * 교차 오리진 허용 목록에 있는 속성의 현재 값을 읽는다.
     *
     * @param k the allowlisted name / 허용된 속성 이름
     * @return the value / 값
     */
    private JsValue readMember(String k) {
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
            PageScripts targetPage = target.getScripts();
            return targetPage == null ? Js.NULL : targetPage.crossOriginHostOpener();
        }
        if ("closed".equals(k)) {
            return Js.bool(target.isClosed());
        }
        if ("location".equals(k)) {
            return location(realm);
        }
        if ("postMessage".equals(k)) {
            return realm.newFunction("postMessage", 2, new NativeFunction.Call() {
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
        if ("focus".equals(k) || "blur".equals(k) || "close".equals(k)) {
            return realm.newFunction(k, 0, new NativeFunction.Call() {
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
    public Property getOwnPropertySlot(Object key) {
        long index = WindowProxy.index(key);
        if (index >= 0) {
            List<BrowsingContext> children = target.getChildren();
            if (index < children.size()) {
                return Property.data(api.windowOf(children.get((int) index)), false, true, true);
            }
            throw denied(key);
        }
        // 표에 있는 속성은 호출자별 메서드 또는 접근자 정체성을 보존한다.
        for (int i = 0; i < READABLE.length; i++) {
            if (READABLE[i].equals(key)) {
                return memberSlot(READABLE[i]);
            }
        }
        if (key instanceof String) {
            BrowsingContext child = namedChild((String) key);
            if (child != null) {
                return Property.data(api.windowOf(child), false, false, true);
            }
        }
        // then과 세 well-known symbol만 값이 undefined인 속성으로 보인다.
        for (int i = 0; i < FALLBACK.length; i++) {
            if (FALLBACK[i].equals(key)) {
                return Property.data(Js.UNDEFINED, false, false, true);
            }
        }
        throw denied(key);
    }

    /**
     * Creates a caller-realm slot once, according to the allowlist's accessor and operation types.
     * <p>
     * 허용 목록의 접근자와 연산 유형에 맞춰 호출자 렐름에서 슬롯을 한 번 만든다.
     *
     * @param key allowlisted member name / 허용된 속성 이름
     * @return the slot / 속성 슬롯
     */
    private Property memberSlot(final String key) {
        Property slot = properties.get(key);
        if (slot != null) {
            return slot;
        }
        // 네 연산은 변경 불가 데이터 속성이고 나머지는 현재 창 상태를 읽는 접근자이다.
        if ("postMessage".equals(key) || "focus".equals(key) || "blur".equals(key) || "close".equals(key)) {
            slot = Property.data(readMember(key), false, false, true);
        } else {
            JsRealm realm = caller.getRealm();
            JsObject getter = realm.newFunction("get " + key, 0, new NativeFunction.Call() {
                /** Returns the current window state. / 현재 창 상태를 반환한다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return readMember(key);
                }
            });
            JsObject setter = "location".equals(key) ? realm.newFunction("set location", 1, new NativeFunction.Call() {
                /** Navigates the target window. / 대상 창을 이동시킨다. */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    set(key, Js.arg(args, 0), self);
                    return Js.UNDEFINED;
                }
            }) : null;
            slot = Property.accessor(getter, setter, false, true);
        }
        properties.put(key, slot);
        return slot;
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
        return getOwnPropertySlot(key) != null;
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
        // 스크립트가 관례적으로 조회하는 폴백 속성까지 열거한다.
        for (int i = 0; i < FALLBACK.length; i++) {
            keys.add(FALLBACK[i]);
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
