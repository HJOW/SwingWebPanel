package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.HashChangeEvent;
import org.duckdns.hjow.swingwebpanel.dom.PageTransitionEvent;
import org.duckdns.hjow.swingwebpanel.dom.PopStateEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The JavaScript interfaces of the two events that session history moves fire at the window (HTML5 2014 REC 5.6.10.1):
 * <code>PopStateEvent</code> (<code>state</code>) and <code>HashChangeEvent</code> (<code>oldURL</code>, <code>newURL</code>), with their
 * constructors and the init dictionaries of the specification.
 * <p>
 * 세션 이력 이동이 window 에 발생시키는 두 이벤트의 JavaScript 인터페이스(HTML5 2014 REC 5.6.10.1): <code>PopStateEvent</code>(<code>state</code>)와
 * <code>HashChangeEvent</code>(<code>oldURL</code>, <code>newURL</code>). 생성자와 규격의 초기화 사전도 함께 설치한다.
 */
public final class HistoryBindings {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private HistoryBindings() {
    }

    /**
     * Installs <code>PopStateEvent</code> and <code>HashChangeEvent</code> (call after the Event interface exists).
     * <p>
     * <code>PopStateEvent</code> 와 <code>HashChangeEvent</code> 를 설치한다(Event 인터페이스가 생긴 뒤에 호출한다).
     *
     * @param bridge the bridge / 브리지
     */
    public static void install(final DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        final JsObject eventCtor = (JsObject) realm.getGlobalObject().get("Event");
        final DomBridge.InterfaceInfo pop = bridge.defineInterface("PopStateEvent", "Event", PopStateEvent.class, new String[] { "a:state" });
        bridge.setConstructor("PopStateEvent", new NativeFunction(realm, eventCtor, "PopStateEvent", 1, notCallable("PopStateEvent"),
                new NativeFunction.Construct() {
            /**
             * Creates a popstate event with the <code>state</code> of the init dictionary (the value as it is, not a copy).
             * <p>
             * 초기화 사전의 <code>state</code> 로 popstate 이벤트를 만든다(복사하지 않고 값 그대로).
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'PopStateEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                PopStateEvent e = new PopStateEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"));
                JsValue state = opts.isObject() ? ((JsObject) opts).get("state") : Js.NULL;
                e.setState(state.isUndefined() ? Js.NULL : state);
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, pop.getPrototype()));
                return w;
            }
        }));
        final DomBridge.InterfaceInfo transition = bridge.defineInterface("PageTransitionEvent", "Event", PageTransitionEvent.class,
                new String[] { "a:persisted" });
        bridge.setConstructor("PageTransitionEvent", new NativeFunction(realm, eventCtor, "PageTransitionEvent", 1, notCallable("PageTransitionEvent"),
                new NativeFunction.Construct() {
            /**
             * Creates a pageshow or pagehide event with the <code>persisted</code> flag of the init dictionary.
             * <p>
             * 초기화 사전의 <code>persisted</code> 플래그로 pageshow 또는 pagehide 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'PageTransitionEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                PageTransitionEvent e = new PageTransitionEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "persisted"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, transition.getPrototype()));
                return w;
            }
        }));
        final DomBridge.InterfaceInfo hash = bridge.defineInterface("HashChangeEvent", "Event", HashChangeEvent.class,
                new String[] { "a:oldURL@OldUrl", "a:newURL@NewUrl" });
        bridge.setConstructor("HashChangeEvent", new NativeFunction(realm, eventCtor, "HashChangeEvent", 1, notCallable("HashChangeEvent"),
                new NativeFunction.Construct() {
            /**
             * Creates a hashchange event with the URLs of the init dictionary.
             * <p>
             * 초기화 사전의 URL로 hashchange 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'HashChangeEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                HashChangeEvent e = new HashChangeEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"));
                e.setUrls(text(opts, "oldURL"), text(opts, "newURL"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, hash.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Creates the behaviour of an interface object that is called without <code>new</code>: it throws a TypeError.
     * <p>
     * <code>new</code> 없이 호출한 인터페이스 객체의 동작을 만든다: TypeError 를 던진다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the behaviour / 동작
     */
    private static NativeFunction.Call notCallable(final String name) {
        return new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        };
    }

    /**
     * Reads a boolean member of an init dictionary.
     * <p>
     * 초기화 사전의 불리언 멤버를 읽는다.
     *
     * @param options the dictionary / 사전
     * @param name    the member / 멤버
     * @return the flag / 플래그
     */
    private static boolean flag(JsValue options, String name) {
        return options.isObject() && Ops.toBoolean(((JsObject) options).get(name));
    }

    /**
     * Reads a string member of an init dictionary (empty if missing).
     * <p>
     * 초기화 사전의 문자열 멤버를 읽는다(없으면 빈 문자열).
     *
     * @param options the dictionary / 사전
     * @param name    the member / 멤버
     * @return the text / 문자열
     */
    private static String text(JsValue options, String name) {
        if (!options.isObject()) {
            return "";
        }
        JsValue v = ((JsObject) options).get(name);
        return v.isUndefined() ? "" : Ops.toString(v);
    }
}
