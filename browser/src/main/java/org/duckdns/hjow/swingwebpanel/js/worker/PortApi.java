package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * Channel messaging of a script context (HTML Standard "Channel messaging", plan item P20-09-b): installs <code>MessageChannel</code> and
 * <code>MessagePort</code> (<code>postMessage</code>, <code>start</code>, <code>close</code>, <code>onmessage</code>,
 * <code>onmessageerror</code>, the <code>message</code> event) into the realm of a page or of a worker, and offers the steps that
 * <code>window.postMessage</code> and <code>structuredClone</code> of the page need to send ports along with a message. A port is the end of a
 * channel ({@link PortCore}) that any context can own: a port in the transfer list of <code>postMessage</code> (of a window, a port, a
 * <code>Worker</code> or the global scope of a worker) is given to the receiver with its queue and its entanglement, so a page and a worker
 * (or two workers, or a shared worker and its clients) can talk through a channel whose ends were created in other places. A port starts to
 * deliver when <code>start()</code> is called or <code>onmessage</code> is set for the first time; what arrives earlier waits in its queue (at
 * most {@link PortCore#MAX_QUEUED} messages). Messages are delivered as tasks of the owner of the receiving port, never while the sender
 * runs.
 * <p>
 * 스크립트 문맥의 채널 메시징(HTML 표준 "Channel messaging", 계획 P20-09-b): 페이지나 worker 의 realm 에 <code>MessageChannel</code> 과
 * <code>MessagePort</code>(<code>postMessage</code>, <code>start</code>, <code>close</code>, <code>onmessage</code>, <code>onmessageerror</code>,
 * <code>message</code> 이벤트)를 설치하고, 페이지의 <code>window.postMessage</code> 와 <code>structuredClone</code> 이 메시지와 함께 포트를 보내는 데 필요한
 * 단계를 제공한다. 포트는 어느 문맥이나 소유할 수 있는 채널의 끝({@link PortCore})이다: (window, 포트, <code>Worker</code>, worker 전역 범위의)
 * <code>postMessage</code> 전달 목록에 든 포트는 큐와 얽힘을 가진 채 받는 쪽에 주어지므로, 페이지와 worker(또는 worker 둘, 공유 worker 와 그 클라이언트)가 다른
 * 곳에서 만든 끝을 가진 채널로 대화할 수 있다. 포트는 <code>start()</code> 를 부르거나 <code>onmessage</code> 를 처음 지정할 때 전달을 시작하며 그전에 도착한
 * 것은 큐에서 기다린다(최대 {@link PortCore#MAX_QUEUED} 개). 메시지는 받는 포트의 소유자의 작업으로 전달되며 보내는 쪽이 실행되는 동안에는 절대 전달되지 않는다.
 */
public final class PortApi {

    /**
     * The context that the interfaces were installed into (set for the page facade).
     * <p>
     * 인터페이스를 설치한 문맥(페이지 정면 객체에서 쓴다).
     */
    private final ScriptContext context;

    /**
     * Creates the facade of a context.
     * <p>
     * 문맥의 정면 객체를 만든다.
     *
     * @param context the context / 문맥
     */
    private PortApi(ScriptContext context) {
        this.context = context;
    }

    /**
     * Installs <code>MessageChannel</code> and <code>MessagePort</code> into the realm of a page and returns the steps that the messaging of
     * the window needs. Call it once, on the thread that creates the page, after the event interfaces and <code>MessageEvent</code> exist.
     * <p>
     * 페이지의 realm 에 <code>MessageChannel</code> 과 <code>MessagePort</code> 를 설치하고, window 메시징에 필요한 단계를 반환한다. 이벤트 인터페이스와
     * <code>MessageEvent</code> 가 생긴 뒤 페이지를 만드는 스레드에서 한 번 부른다.
     *
     * @param page the page / 페이지
     * @return the facade of the page / 페이지의 정면 객체
     */
    public static PortApi installInPage(PageScripts page) {
        ScriptContext ctx = new PageContext(page);
        install(ctx);
        return new PortApi(ctx);
    }

    /**
     * Reads the transfer list of a <code>postMessage</code> call of the window (ArrayBuffers and ports).
     * <p>
     * window 의 <code>postMessage</code> 호출의 전달 목록(ArrayBuffer 와 포트)을 읽는다.
     *
     * @param arg the argument, an array or <code>{transfer}</code> (may be undefined) / 인수. 배열 또는 <code>{transfer}</code>(undefined 일 수 있다)
     * @return the objects to transfer, null for none / 전달할 객체. 없으면 null
     */
    public List<JsObject> transferList(JsValue arg) {
        return Messages.transferList(context, arg);
    }

    /**
     * Serializes a message of the page; a failure is a <code>DataCloneError</code>, the transferred objects are given up on success.
     * <p>
     * 페이지의 메시지를 직렬화한다. 실패는 <code>DataCloneError</code> 이며 성공하면 전달한 객체를 내놓는다.
     *
     * @param message  the message / 메시지
     * @param transfer the transfer list or null / 전달 목록. 없으면 null
     * @return the serialized message / 직렬화된 메시지
     */
    public ClonedValue serialize(JsValue message, List<JsObject> transfer) {
        return Messages.serializeWith(context, message, transfer);
    }

    /**
     * Creates the value of a serialized message in the realm of the page (the ports that it carries become ports of the page).
     * <p>
     * 직렬화된 메시지의 값을 페이지의 realm 에 만든다(함께 온 포트는 페이지의 포트가 된다).
     *
     * @param data the serialized message / 직렬화된 메시지
     * @return the value / 값
     */
    public JsValue deserialize(ClonedValue data) {
        return Messages.deserialize(context, data, null);
    }

    /**
     * Fills the <code>message</code> event that a window receives: data, origin, source and the ports that came with the message.
     * <p>
     * window 가 받는 <code>message</code> 이벤트를 채운다: 데이터, 오리진, 소스, 메시지와 함께 온 포트.
     *
     * @param event  the event / 이벤트
     * @param data   the serialized message / 직렬화된 메시지
     * @param origin the origin of the sender / 보낸 쪽의 오리진
     * @param source the source object of the event / 이벤트의 소스 객체
     */
    public void fillEvent(MessageEvent event, ClonedValue data, String origin, Object source) {
        Messages.fill(context, event, data, origin, source);
    }

    /**
     * Tells whether a value is a <code>MessagePort</code> object.
     * <p>
     * 값이 <code>MessagePort</code> 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a port / 포트이면 true
     */
    public static boolean isPort(JsValue v) {
        return DomBridge.targetOf(v, PortTarget.class) != null;
    }

    /**
     * Installs <code>MessageChannel</code> and <code>MessagePort</code> into the realm of a context.
     * <p>
     * 문맥의 realm 에 <code>MessageChannel</code> 과 <code>MessagePort</code> 를 설치한다.
     *
     * @param ctx the context / 문맥
     */
    static void install(final ScriptContext ctx) {
        final DomBridge bridge = ctx.getBridge();
        final JsRealm realm = ctx.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a port or a channel.
             * <p>
             * 포트와 채널의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof PortTarget) {
                    return "MessagePort";
                } else if (o instanceof ChannelTarget) {
                    return "MessageChannel";
                }
                return null;
            }
        });
        final DomBridge.InterfaceInfo port = bridge.defineInterface("MessagePort", "EventTarget", PortTarget.class, new String[] { "m:start()", "m:close()" });
        bridge.getEventHandlers().define(port.getPrototype(), PortTarget.class, new String[] { "message", "messageerror" });
        startOnFirstHandler(port.getPrototype(), realm);
        port.getPrototype().defineData("postMessage", realm.newFunction("postMessage", 1, new NativeFunction.Call() {
            /**
             * Posts a message to the port at the other end of the channel.
             * <p>
             * 채널의 반대쪽 끝의 포트로 메시지를 보낸다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: message and optional transfer list / 인수: 메시지와 선택적 전달 목록
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                PortTarget p = (PortTarget) DomBridge.targetOf(self, PortTarget.class);
                if (p == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                ScriptUtil.requireArgs(args, 1, "execute 'postMessage' on 'MessagePort'");
                p.post(args[0], Js.arg(args, 1));
                return Js.UNDEFINED;
            }
        }), true, true, true);
        final DomBridge.InterfaceInfo channel = bridge.defineInterface("MessageChannel", null, ChannelTarget.class, new String[] { "a:port1", "a:port2" });
        bridge.setConstructor("MessageChannel", new NativeFunction(realm, realm.getFunctionPrototype(), "MessageChannel", 0, ScriptUtil.refuse("MessageChannel"),
                new NativeFunction.Construct() {
            /**
             * Creates a channel with its two ports.
             * <p>
             * 포트 둘이 있는 채널을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the channel wrapper / 채널 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(new ChannelTarget(ctx));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, channel.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Makes the first assignment of <code>onmessage</code> start the port, wrapping the accessor that the event handler registry defined.
     * <p>
     * 이벤트 핸들러 등록부가 정의한 접근자를 감싸 <code>onmessage</code> 의 첫 지정이 포트를 시작하게 한다.
     *
     * @param proto the prototype of <code>MessagePort</code> / <code>MessagePort</code> 의 프로토타입
     * @param realm the realm / realm
     */
    private static void startOnFirstHandler(JsObject proto, JsRealm realm) {
        PropertyDescriptor d = proto.getOwnProperty("onmessage");
        if (d == null || !d.isAccessor() || !(d.set instanceof JsObject) || !(d.get instanceof JsObject)) {
            return;
        }
        final JsValue original = d.set;
        proto.defineAccessor("onmessage", (JsObject) d.get, realm.newFunction("set onmessage", 1, new NativeFunction.Call() {
            /**
             * Sets the handler and starts the port.
             * <p>
             * 핸들러를 지정하고 포트를 시작한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Ops.call(original, self, args);
                PortTarget p = (PortTarget) DomBridge.targetOf(self, PortTarget.class);
                if (p != null) {
                    p.start();
                }
                return Js.UNDEFINED;
            }
        }), true, true);
    }
}
