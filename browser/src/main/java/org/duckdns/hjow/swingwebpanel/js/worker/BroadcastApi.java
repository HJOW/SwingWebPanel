package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
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

/**
 * Installs the <code>BroadcastChannel</code> interface (HTML Standard "Broadcasting to other browsing contexts") into the realm of a script
 * context: <code>new BroadcastChannel(name)</code>, <code>name</code>, <code>postMessage(message)</code>, <code>close()</code>,
 * <code>onmessage</code>, <code>onmessageerror</code> and the <code>message</code> event. Channels of the same name in the contexts of one
 * {@link BroadcastHub} (a page and its workers) hear each other; the sender does not hear itself.
 * <p>
 * 스크립트 문맥의 realm 에 <code>BroadcastChannel</code> 인터페이스(HTML 표준 "Broadcasting to other browsing contexts")를 설치한다:
 * <code>new BroadcastChannel(name)</code>, <code>name</code>, <code>postMessage(message)</code>, <code>close()</code>, <code>onmessage</code>,
 * <code>onmessageerror</code>, <code>message</code> 이벤트. 하나의 {@link BroadcastHub} 에 속한 문맥(페이지와 그 worker)의 같은 이름 채널은 서로
 * 듣는다. 보내는 쪽은 자기 메시지를 듣지 않는다.
 */
final class BroadcastApi {

    /**
     * Private constructor: static installer.
     * <p>
     * 비공개 생성자: 정적 설치기.
     */
    private BroadcastApi() {
    }

    /**
     * Installs the interface.
     * <p>
     * 인터페이스를 설치한다.
     *
     * @param ctx the context that receives the interface / 인터페이스를 받는 문맥
     * @param hub the hub that the channels of the context join / 문맥의 채널이 속하는 허브
     */
    static void install(final ScriptContext ctx, final BroadcastHub hub) {
        final DomBridge bridge = ctx.getBridge();
        final JsRealm realm = ctx.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a channel.
             * <p>
             * 채널의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                return o instanceof BroadcastChannelTarget ? "BroadcastChannel" : null;
            }
        });
        final DomBridge.InterfaceInfo info = bridge.defineInterface("BroadcastChannel", "EventTarget", BroadcastChannelTarget.class,
                new String[] { "a:name", "m:close()" });
        bridge.getEventHandlers().define(info.getPrototype(), BroadcastChannelTarget.class, new String[] { "message", "messageerror" });
        info.getPrototype().defineData("postMessage", realm.newFunction("postMessage", 1, new NativeFunction.Call() {
            /**
             * Posts a message to the other channels of the same name.
             * <p>
             * 같은 이름의 다른 채널에 메시지를 보낸다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the message / 인수: 메시지
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                BroadcastChannelTarget ch = (BroadcastChannelTarget) DomBridge.targetOf(self, BroadcastChannelTarget.class);
                if (ch == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                ScriptUtil.requireArgs(args, 1, "execute 'postMessage' on 'BroadcastChannel'");
                if (ch.isClosed()) {
                    throw bridge.domError(DomException.invalidState("Failed to execute 'postMessage' on 'BroadcastChannel': Channel is closed."));
                }
                ClonedValue cloned = Messages.serialize(ctx, args[0], Js.UNDEFINED);
                String origin = ctx.getOrigin();
                hub.broadcast(ch, cloned, origin == null ? "null" : origin);
                return Js.UNDEFINED;
            }
        }), true, true, true);
        bridge.setConstructor("BroadcastChannel", new NativeFunction(realm, realm.getFunctionPrototype(), "BroadcastChannel", 1,
                ScriptUtil.refuse("BroadcastChannel"), new NativeFunction.Construct() {
            /**
             * Creates a channel and joins the hub.
             * <p>
             * 채널을 만들고 허브에 들어간다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the channel name / 인수: 채널 이름
             * @param newTarget the new target / new 대상
             * @return the channel wrapper / 채널 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                ScriptUtil.requireArgs(args, 1, "construct 'BroadcastChannel'");
                // 끝난 문맥(이동하거나 폐기된 페이지, 종료된 worker)에서는 새 채널을 만들 수 없다.
                if (!ctx.isActive()) {
                    throw bridge.domError(DomException.invalidState("Failed to construct 'BroadcastChannel': the context has ended."));
                }
                BroadcastChannelTarget ch = new BroadcastChannelTarget(Ops.toString(args[0]), ctx, hub);
                if (!hub.add(ch)) {
                    throw bridge.domError(new DomException("QuotaExceededError", DomException.QUOTA_EXCEEDED_ERR,
                            "Failed to construct 'BroadcastChannel': too many open channels (limit " + WorkerLimits.MAX_BROADCAST_CHANNELS + ")."));
                }
                HostObject w = bridge.wrap(ch);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }
}
