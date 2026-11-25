package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.worker.PortApi;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Messaging of a page (plan items P20-09 and P20-09-b; W3C Web Messaging REC 2015-05-19 and the HTML Standard "Web messaging" of 2026-11-17):
 * <code>window.postMessage(message, targetOrigin [, transfer])</code> and <code>window.postMessage(message, {targetOrigin, transfer})</code>,
 * <code>MessageEvent</code>, and <code>structuredClone(value [, {transfer}])</code>; <code>MessageChannel</code> and <code>MessagePort</code>
 * are installed through {@link PortApi}, because their ports are shared with workers. Messages are copied with the structured clone algorithm
 * ({@link org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone}), may carry ArrayBuffers and <code>MessagePort</code> objects in the transfer list
 * (they are moved, the source is detached), and are delivered later as tasks of the page on the scheduler of the component, never while the
 * sender runs. A <code>postMessage</code> to the window checks the target origin: <code>*</code> sends, <code>/</code> (the default of the
 * options form) sends to the same origin (the only window of this component), a URL sends only if it has the origin of the page, anything else
 * is a <code>SyntaxError</code>; a message to another origin is dropped silently, after it was cloned (so the transferred objects are given up
 * as the specification says). There is one browsing context, so the target of <code>window.postMessage</code> is always the window of the page.
 * <p>
 * 페이지의 메시징(계획 P20-09, P20-09-b. W3C Web Messaging REC 2015-05-19 와 2026-11-17 의 HTML 표준 "Web messaging"):
 * <code>window.postMessage(message, targetOrigin [, transfer])</code>, <code>window.postMessage(message, {targetOrigin, transfer})</code>,
 * <code>MessageEvent</code>, <code>structuredClone(value [, {transfer}])</code>. <code>MessageChannel</code> 과 <code>MessagePort</code> 는 포트를 worker 와
 * 공유하므로 {@link PortApi} 로 설치한다. 메시지는 구조화 복제 알고리즘({@link org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone})으로 복사하며 전달 목록에
 * ArrayBuffer 와 <code>MessagePort</code> 객체를 실을 수 있고(옮겨지고 원본은 분리된다), 보내는 쪽이 실행되는 동안이 아니라 나중에 컴포넌트의 스케줄러에서
 * 페이지의 작업으로 전달된다. window 에 대한 <code>postMessage</code> 는 대상 오리진을 검사한다: <code>*</code> 은 보내고, <code>/</code>(옵션 형식의 기본값)는
 * 같은 오리진에 보내며(이 컴포넌트의 유일한 window), URL 은 페이지의 오리진과 같을 때만 보내고, 그 밖은 <code>SyntaxError</code> 이다. 다른 오리진으로 가는
 * 메시지는 복제한 뒤 조용히 버려진다(그래서 규격대로 전달한 객체는 내놓는다). 브라우징 문맥이 하나뿐이므로 <code>window.postMessage</code> 의 대상은 언제나
 * 페이지의 window 이다.
 */
public final class MessagingApi {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    MessagingApi(PageScripts page) {
        this.page = page;
    }

    /**
     * The channel messaging of the page: the transfer list, the serialization and the ports that come with a message (the interfaces
     * <code>MessageChannel</code> and <code>MessagePort</code> belong to the worker package, because ports are shared with workers).
     * <p>
     * 페이지의 채널 메시징: 전달 목록, 직렬화, 메시지와 함께 오는 포트(<code>MessageChannel</code> 과 <code>MessagePort</code> 인터페이스는 포트를 worker 와
     * 공유하므로 worker 패키지에 있다).
     */
    private PortApi ports;

    /**
     * Reads the transfer list of a call: the array-like argument, or the <code>transfer</code> member of an options object. ArrayBuffers and
     * <code>MessagePort</code> objects can be transferred.
     * <p>
     * 호출의 전달 목록을 읽는다: 배열 형태 인수 또는 옵션 객체의 <code>transfer</code> 멤버. ArrayBuffer 와 <code>MessagePort</code> 객체를 전달할 수 있다.
     *
     * @param v      the argument / 인수
     * @param source unused, kept for the callers that name the port that sends / 쓰지 않는다. 보내는 포트를 적는 호출자를 위해 남겼다
     * @return the objects to transfer, null for none / 전달할 객체. 없으면 null
     */
    private List<JsObject> transferList(JsValue v, Object source) {
        return ports.transferList(v);
    }

    /**
     * Clones a message with the transfer list; a failure is a <code>DataCloneError</code>.
     * <p>
     * 전달 목록과 함께 메시지를 복제한다. 실패는 <code>DataCloneError</code> 이다.
     *
     * @param message  the message / 메시지
     * @param transfer the transfer list or null / 전달 목록. 없으면 null
     * @return the serialized message / 직렬화된 메시지
     */
    private ClonedValue cloneMessage(JsValue message, List<JsObject> transfer) {
        return ports.serialize(message, transfer);
    }

    /**
     * Installs everything.
     * <p>
     * 모두 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        installMessageEvent(bridge, realm);
        ports = PortApi.installInPage(page);
        installWindowMembers(g, realm);
    }

    /**
     * Creates the behaviour of an interface object that is called without <code>new</code>: it throws a TypeError.
     * <p>
     * <code>new</code> 없이 호출한 인터페이스 객체의 동작을 만든다: TypeError 를 던진다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the behaviour / 동작
     */
    private static NativeFunction.Call refuse(final String name) {
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
     * Defines the <code>MessageEvent</code> interface with its constructor (<code>data</code>, <code>origin</code>,
     * <code>lastEventId</code>, <code>source</code>, <code>ports</code> of the init dictionary).
     * <p>
     * 생성자와 함께 <code>MessageEvent</code> 인터페이스를 정의한다(초기화 사전의 <code>data</code>, <code>origin</code>, <code>lastEventId</code>,
     * <code>source</code>, <code>ports</code>).
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    public static void installMessageEvent(final DomBridge bridge, JsRealm realm) {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("MessageEvent", "Event", MessageEvent.class, new String[] { "a:data", "a:origin",
            "a:lastEventId", "a:source", "a:ports" });
        bridge.setConstructor("MessageEvent", new NativeFunction(realm, (JsObject) realm.getGlobalObject().get("Event"), "MessageEvent", 1, refuse("MessageEvent"),
                new NativeFunction.Construct() {
            /**
             * Creates a message event from a type and an init dictionary.
             * <p>
             * 종류와 초기화 사전으로 message 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event wrapper / 이벤트 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'MessageEvent': 1 argument required, but only 0 present.");
                }
                JsValue opts = Js.arg(args, 1);
                MessageEvent e = new MessageEvent(Ops.toString(args[0]), flag(opts, "bubbles"), flag(opts, "cancelable"));
                JsValue data = member(opts, "data");
                JsValue source = member(opts, "source");
                JsValue portsValue = member(opts, "ports");
                List<JsValue> ports = new ArrayList<JsValue>();
                if (!portsValue.isNullOrUndefined()) {
                    JsValue[] items = Ops.createListFromArrayLike(portsValue);
                    // 포트 목록의 항목은 모두 MessagePort 여야 한다.
                    for (int i = 0; i < items.length; i++) {
                        if (!PortApi.isPort(items[i])) {
                            throw JsException.typeError("Failed to construct 'MessageEvent': a member of ports is not a MessagePort.");
                        }
                        ports.add(items[i]);
                    }
                }
                e.setContent(data.isUndefined() ? Js.NULL : data, text(opts, "origin"), text(opts, "lastEventId"),
                        source.isNullOrUndefined() ? null : source, ports);
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Reads a member of an init dictionary (undefined if it is missing).
     * <p>
     * 초기화 사전의 멤버를 읽는다(없으면 undefined).
     *
     * @param options the dictionary / 사전
     * @param name    the member / 멤버
     * @return the value / 값
     */
    private static JsValue member(JsValue options, String name) {
        return options.isObject() ? ((JsObject) options).get(name) : Js.UNDEFINED;
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
        return Ops.toBoolean(member(options, name));
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
        JsValue v = member(options, name);
        return v.isUndefined() ? "" : Ops.toString(v);
    }

    /**
     * Installs <code>postMessage</code> and <code>structuredClone</code> on the global object.
     * <p>
     * 전역 객체에 <code>postMessage</code> 와 <code>structuredClone</code> 을 설치한다.
     *
     * @param g     the global object / 전역 객체
     * @param realm the realm / realm
     */
    private void installWindowMembers(JsObject g, JsRealm realm) {
        g.defineData("postMessage", realm.newFunction("postMessage", 2, new NativeFunction.Call() {
            /**
             * Posts a message to the window of the page.
             * <p>
             * 페이지의 window 로 메시지를 보낸다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: message and either (target origin, optional transfer list) or an options object {targetOrigin, transfer} / 인수: 메시지, 그리고 (대상 오리진, 선택적 전달 목록) 또는 옵션 객체 {targetOrigin, transfer}
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length < 2) {
                    throw JsException.typeError("Failed to execute 'postMessage' on 'Window': 2 arguments required, but only " + args.length
                            + " present.");
                }
                // 둘째 인수가 객체이면 옵션 사전 {targetOrigin, transfer} 형식이다(targetOrigin 의 기본값은 "/"). 아니면 targetOrigin 문자열 형식이다.
                if (args[1].isObject()) {
                    JsValue to = ((JsObject) args[1]).get("targetOrigin");
                    postToWindow(args[0], to.isUndefined() ? "/" : Ops.toString(to), args[1]);
                    return Js.UNDEFINED;
                }
                postToWindow(args[0], Ops.toString(args[1]), Js.arg(args, 2));
                return Js.UNDEFINED;
            }
        }), true, true, true);
        g.defineData("structuredClone", realm.newFunction("structuredClone", 1, new NativeFunction.Call() {
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
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'structuredClone' on 'Window': 1 argument required, but only 0 present.");
                }
                ClonedValue cloned = cloneMessage(args[0], transferList(Js.arg(args, 1), null));
                return ports.deserialize(cloned);
            }
        }), true, true, true);
    }

    /**
     * The algorithm of <code>window.postMessage</code>: checks the target origin, clones the message (with the transfer list), and queues the
     * <code>message</code> event for the window.
     * <p>
     * <code>window.postMessage</code> 의 알고리즘: 대상 오리진을 검사하고, 메시지를 (전달 목록과 함께) 복제하고, window 의 <code>message</code>
     * 이벤트를 큐에 넣는다.
     *
     * @param message      the message / 메시지
     * @param targetOrigin <code>*</code>, <code>/</code> or an absolute URL / <code>*</code>, <code>/</code>, 절대 URL
     * @param transfer     the transfer argument / 전달 인수
     */
    private void postToWindow(JsValue message, String targetOrigin, JsValue transfer) {
        boolean any = "*".equals(targetOrigin);
        boolean same = "/".equals(targetOrigin);
        PageScripts sender = PageScripts.messageSender(page);
        Origin senderOrigin = Origin.of(sender.getDocument().getOriginUrl());
        Origin documentOrigin = Origin.of(page.getDocument().getOriginUrl());
        boolean matches = any || same && (sender == page || senderOrigin.isSameOrigin(documentOrigin));
        if (!any && !same) {
            Origin target;
            try {
                target = UrlUtil.getScheme(targetOrigin) == null ? null : Origin.of(targetOrigin);
                UrlUtil.parse(targetOrigin);
            } catch (RuntimeException e) {
                target = null;
            }
            if (target == null) {
                throw page.getBridge().domError(DomException.syntax("Failed to execute 'postMessage' on 'Window': Invalid target origin '"
                        + targetOrigin + "' in a call to 'postMessage'."));
            }
            matches = !target.isOpaque() && !documentOrigin.isOpaque() && target.isSameOrigin(documentOrigin);
        }
        ClonedValue cloned = sender.messaging().cloneForPost(message, transfer);
        if (!matches) {
            return;
        }
        queue(cloned, senderOrigin.serialize(), sender.getRealm().getGlobalThisValue());
    }

    /**
     * Clones a message of a script of this page that is meant for another window.
     * <p>
     * 이 페이지의 스크립트가 다른 window 로 보내려는 메시지를 복제한다.
     *
     * @param message  the message / 메시지
     * @param transfer the transfer argument / 전달 인수
     * @return the clone / 복제본
     */
    ClonedValue cloneForPost(JsValue message, JsValue transfer) {
        return cloneMessage(message, transferList(transfer, null));
    }

    /**
     * Receives a message that a script of another window posted to the window of this page: the target origin is checked against the
     * document of this page, and the <code>message</code> event with the origin of the sender is queued.
     * <p>
     * 다른 window 의 스크립트가 이 페이지의 window 로 보낸 메시지를 받는다: 대상 오리진을 이 페이지의 문서에 대해 검사하고, 보낸 쪽의 오리진을 가진
     * <code>message</code> 이벤트를 큐에 넣는다.
     *
     * @param targetOrigin    <code>*</code>, <code>/</code> or an absolute URL / <code>*</code>, <code>/</code>, 절대 URL
     * @param cloned          the cloned message / 복제한 메시지
     * @param senderOrigin    the serialized origin of the sender / 보낸 쪽의 직렬화된 오리진
     * @param senderOriginUrl the address that the origin of the sender is computed from, used for <code>/</code> / 보낸 쪽 오리진의 원천 주소. <code>/</code> 에 쓴다
     * @param source          the sending window identity / 보낸 창 객체
     */
    void receiveFromOther(String targetOrigin, ClonedValue cloned, String senderOrigin, String senderOriginUrl, JsObject source) {
        Origin documentOrigin = Origin.of(page.getDocument().getOriginUrl());
        boolean matches;
        // 대상 오리진은 이 페이지의 문서와 비교한다. "/" 는 보낸 쪽과 같은 오리진을 뜻한다.
        if ("*".equals(targetOrigin)) {
            matches = true;
        } else {
            Origin expected = "/".equals(targetOrigin) ? Origin.of(senderOriginUrl == null ? "about:blank" : senderOriginUrl) : Origin.of(targetOrigin);
            matches = !expected.isOpaque() && !documentOrigin.isOpaque() && expected.isSameOrigin(documentOrigin);
        }
        if (matches) {
            queue(cloned, senderOrigin, source);
        }
    }

    /**
     * Queues the <code>message</code> event of a cloned message at the window of this page.
     * <p>
     * 복제한 메시지의 <code>message</code> 이벤트를 이 페이지의 window 에 큐로 넣는다.
     *
     * @param cloned the cloned message / 복제한 메시지
     * @param origin the serialized origin of the sender / 보낸 쪽의 직렬화된 오리진
     * @param source the sending window identity / 보낸 창 객체
     */
    private void queue(final ClonedValue cloned, final String origin, final JsObject source) {
        Scheduler s = page.getEnvironment().getScheduler();
        if (s == null) {
            return;
        }
        s.post(new Runnable() {
            /**
             * Delivers the message under the page lock.
             * <p>
             * 페이지 잠금 아래에서 메시지를 전달한다.
             */
            public void run() {
                page.runTask(new Runnable() {
                    /**
                     * Creates the event and dispatches it at the window.
                     * <p>
                     * 이벤트를 만들어 window 에 전달한다.
                     */
                    public void run() {
                        MessageEvent event = new MessageEvent("message", false, false);
                        ports.fillEvent(event, cloned, origin, source);
                        try {
                            page.getWindow().dispatchTrustedEvent(event);
                        } catch (DomException e) {
                            throw page.getBridge().domError(e);
                        }
                    }
                });
            }
        }, 0);
    }
}
