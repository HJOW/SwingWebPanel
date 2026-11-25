package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;

/**
 * The event handler IDL attributes (<code>onclick</code>, <code>onload</code> ...) of one page. A handler is stored per event target and
 * event type in a {@link HandlerListener}; the listener is registered with the target at the first assignment, so the handler keeps its place
 * among the listeners when it is replaced. The same registry serves event handler content attributes (<code>&lt;a onclick="..."&gt;</code>).
 * <p>
 * 한 페이지의 이벤트 핸들러 IDL 속성(<code>onclick</code>, <code>onload</code> 등). 핸들러는 이벤트 대상과 이벤트 종류마다 {@link HandlerListener} 에
 * 보관한다. 리스너는 첫 대입 때 대상에 등록되므로 핸들러를 바꿔도 리스너 목록에서 자기 위치를 유지한다. 이벤트 핸들러 콘텐츠 속성
 * (<code>&lt;a onclick="..."&gt;</code>)도 같은 등록부를 쓴다.
 */
public final class EventHandlers {

    /**
     * The names of the events that have handler attributes on elements, the document and the window.
     * <p>
     * 요소, 문서, 창에 핸들러 속성이 있는 이벤트의 이름.
     */
    public static final String[] GLOBAL_EVENTS = { "abort", "animationcancel", "animationend", "animationiteration", "animationstart", "blur", "canplay",
        "change", "click", "contextmenu", "dblclick", "drag", "dragend", "dragenter", "dragleave", "dragover", "dragstart", "drop",
        "durationchange", "ended", "error", "focus", "input", "invalid", "keydown", "keypress", "keyup", "load", "loadeddata",
        "loadedmetadata", "mousedown", "mouseenter", "mouseleave", "mousemove", "mouseout", "mouseover", "mouseup", "pause", "play",
        "playing", "reset", "resize", "scroll", "seeked", "seeking", "select", "submit", "timeupdate", "transitioncancel", "transitionend", "transitionrun", "transitionstart", "volumechange",
        "wheel", "auxclick", "beforeinput", "canplaythrough", "cancel", "close", "copy", "cuechange", "cut", "emptied", "formdata", "gotpointercapture",
        "loadstart", "lostpointercapture", "paste", "pointercancel", "pointerdown", "pointerenter", "pointerleave", "pointermove", "pointerout",
        "pointerover", "pointerup", "progress", "ratechange", "readystatechange", "selectionchange", "selectstart", "slotchange", "stalled",
        "suspend", "toggle", "touchcancel", "touchend", "touchmove", "touchstart", "visibilitychange", "waiting", "contextlost", "contextrestored" };

    /**
     * The names of the events with handler attributes on the window only (and on the body element, which forwards them).
     * <p>
     * 창에만(그리고 이를 전달하는 body 요소에) 핸들러 속성이 있는 이벤트의 이름.
     */
    public static final String[] WINDOW_EVENTS = { "afterprint", "beforeprint", "beforeunload", "hashchange", "languagechange", "message", "messageerror",
        "offline", "online", "pagehide", "pageshow", "popstate", "storage", "unload" };

    /**
     * The bridge of the page.
     * <p>
     * 페이지의 브리지.
     */
    private final DomBridge bridge;

    /**
     * Handlers by target and event type.
     * <p>
     * 대상과 이벤트 종류별 핸들러.
     */
    private final Map<EventTarget, Map<String, HandlerListener>> handlers = new WeakHashMap<EventTarget, Map<String, HandlerListener>>();

    /**
     * Creates the registry.
     * <p>
     * 등록부를 만든다.
     *
     * @param bridge the bridge / 브리지
     */
    public EventHandlers(DomBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * Returns the handler of a target and event type.
     * <p>
     * 대상과 이벤트 종류의 핸들러를 반환한다.
     *
     * @param target the event target / 이벤트 대상
     * @param type   the event type / 이벤트 종류
     * @param create true to create and register the handler if it does not exist / 없으면 만들어 등록하려면 true
     * @return the handler or null / 핸들러 또는 null
     */
    public HandlerListener handler(EventTarget target, String type, boolean create) {
        Map<String, HandlerListener> m = handlers.get(target);
        if (m == null) {
            if (!create) {
                return null;
            }
            m = new HashMap<String, HandlerListener>();
            handlers.put(target, m);
        }
        HandlerListener h = m.get(type);
        if (h == null && create) {
            h = new HandlerListener(bridge);
            m.put(type, h);
            target.addEventListener(type, h);
        }
        return h;
    }

    /**
     * Defines the <code>on...</code> accessor properties for a list of event types on a prototype.
     * <p>
     * 프로토타입에 이벤트 종류 목록의 <code>on...</code> 접근자 프로퍼티를 정의한다.
     *
     * @param proto the prototype object / 프로토타입 객체
     * @param cls   the class that the receiver must be an instance of / 수신자가 인스턴스여야 하는 클래스
     * @param types the event types / 이벤트 종류
     */
    public void define(JsObject proto, final Class<?> cls, String[] types) {
        JsRealm realm = bridge.getRealm();
        // 이벤트 종류마다 getter/setter 한 쌍을 정의한다.
        for (int i = 0; i < types.length; i++) {
            final String type = types[i];
            JsObject getter = realm.newFunction("get on" + type, 0, new NativeFunction.Call() {
                /**
                 * Returns the handler function or null.
                 * <p>
                 * 핸들러 함수 또는 null 을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the function or null / 함수 또는 null
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    EventTarget t = (EventTarget) DomBridge.targetOf(self, cls);
                    if (t == null) {
                        return Js.NULL;
                    }
                    HandlerListener h = handler(t, type, false);
                    JsObject cb = h == null ? null : h.getCallback();
                    return cb == null ? Js.NULL : cb;
                }
            });
            JsObject setter = realm.newFunction("set on" + type, 1, new NativeFunction.Call() {
                /**
                 * Sets the handler function; a value that is not an object clears it.
                 * <p>
                 * 핸들러 함수를 지정한다. 객체가 아닌 값은 핸들러를 지운다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    EventTarget t = (EventTarget) DomBridge.targetOf(self, cls);
                    if (t == null) {
                        return Js.UNDEFINED;
                    }
                    JsValue v = Js.arg(args, 0);
                    JsObject cb = v.isObject() ? (JsObject) v : null;
                    if (cb == null && handler(t, type, false) == null) {
                        return Js.UNDEFINED;
                    }
                    handler(t, type, true).setCallback(cb);
                    return Js.UNDEFINED;
                }
            });
            proto.defineAccessor("on" + type, getter, setter, true, true);
        }
    }

    /**
     * Defines the <code>on...</code> accessor properties for a list of event types on an object whose target is fixed (the global object,
     * which stands for the window).
     * <p>
     * 대상이 고정된 객체(window 를 나타내는 전역 객체)에 이벤트 종류 목록의 <code>on...</code> 접근자 프로퍼티를 정의한다.
     *
     * @param object the object that receives the properties / 프로퍼티를 받는 객체
     * @param fixed  the event target behind the object / 객체 뒤의 이벤트 대상
     * @param types  the event types / 이벤트 종류
     */
    public void defineBound(JsObject object, final EventTarget fixed, String[] types) {
        JsRealm realm = bridge.getRealm();
        // 이벤트 종류마다 고정된 대상에 묶인 getter/setter 한 쌍을 정의한다.
        for (int i = 0; i < types.length; i++) {
            final String type = types[i];
            JsObject getter = realm.newFunction("get on" + type, 0, new NativeFunction.Call() {
                /**
                 * Returns the handler function or null.
                 * <p>
                 * 핸들러 함수 또는 null 을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the function or null / 함수 또는 null
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    HandlerListener h = handler(fixed, type, false);
                    JsObject cb = h == null ? null : h.getCallback();
                    return cb == null ? Js.NULL : cb;
                }
            });
            JsObject setter = realm.newFunction("set on" + type, 1, new NativeFunction.Call() {
                /**
                 * Sets the handler function.
                 * <p>
                 * 핸들러 함수를 지정한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    JsValue v = Js.arg(args, 0);
                    JsObject cb = v.isObject() ? (JsObject) v : null;
                    if (cb == null && handler(fixed, type, false) == null) {
                        return Js.UNDEFINED;
                    }
                    handler(fixed, type, true).setCallback(cb);
                    return Js.UNDEFINED;
                }
            });
            object.defineAccessor("on" + type, getter, setter, true, true);
        }
    }
}
