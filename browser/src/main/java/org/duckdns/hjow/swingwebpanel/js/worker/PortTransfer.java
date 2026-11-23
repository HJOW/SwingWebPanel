package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredCloneException;

/**
 * The transfer handler of the structured clone for <code>MessagePort</code> objects (HTML Standard "Message ports", transfer steps and
 * transfer-receiving steps) of one context: a port of the transfer list is checked (<code>prepare</code>), given up once the whole message
 * was serialized (<code>commit</code>: the port object loses its end of the channel and the end loses its owner), and, wherever the message is
 * received, turned into a new port object of the receiving context that owns the same end of the channel with its queue and entanglement
 * (<code>receive</code>), disabled until it is started.
 * <p>
 * 구조화 복제가 <code>MessagePort</code> 객체(HTML 표준 "Message ports"의 전달 단계와 전달 수신 단계)를 다루는 한 문맥의 전달 처리기: 전달 목록의 포트를
 * 검사하고(<code>prepare</code>), 메시지 전체를 직렬화한 뒤 내놓으며(<code>commit</code>: 포트 객체는 채널의 끝을 잃고 끝은 소유자를 잃는다), 메시지가 어디서
 * 받아지든 받는 문맥의 새 포트 객체로 바꾼다(<code>receive</code>). 새 포트 객체는 같은 채널의 끝을 큐와 얽힘 그대로 소유하며 시작하기 전까지 비활성이다.
 */
final class PortTransfer implements StructuredClone.TransferHandler {

    /**
     * The context that serializes or receives.
     * <p>
     * 직렬화하거나 받는 문맥.
     */
    private final ScriptContext context;

    /**
     * Creates the handler of a context.
     * <p>
     * 문맥의 처리기를 만든다.
     *
     * @param context the context / 문맥
     */
    PortTransfer(ScriptContext context) {
        this.context = context;
    }

    /**
     * Checks that an object of the transfer list is a live port of this context and returns its end of the channel.
     * <p>
     * 전달 목록의 객체가 이 문맥의 살아 있는 포트인지 검사하고 채널의 끝을 반환한다.
     *
     * @param object the object of the transfer list / 전달 목록의 객체
     * @return the end of the channel, or null if the object is not a port / 채널의 끝. 포트가 아니면 null
     */
    public Object prepare(JsObject object) {
        PortTarget port = (PortTarget) DomBridge.targetOf(object, PortTarget.class);
        if (port == null) {
            return null;
        }
        PortCore core = port.core();
        // 이미 전달되었거나 닫힌 포트는 다시 전달할 수 없다.
        if (core == null || core.isClosed()) {
            throw new StructuredCloneException("A MessagePort is detached and could not be transferred.");
        }
        return core;
    }

    /**
     * Gives up the port after a successful serialization.
     * <p>
     * 직렬화에 성공한 뒤 포트를 내놓는다.
     *
     * @param object  the port object / 포트 객체
     * @param payload the end of the channel / 채널의 끝
     */
    public void commit(JsObject object, Object payload) {
        PortTarget port = (PortTarget) DomBridge.targetOf(object, PortTarget.class);
        if (port != null) {
            port.neuter();
        }
        ((PortCore) payload).detach();
    }

    /**
     * Creates the port object of a received end of a channel in this context.
     * <p>
     * 받은 채널의 끝에 대한 포트 객체를 이 문맥에 만든다.
     *
     * @param realm   the realm of the receiver / 받는 쪽 realm
     * @param payload the end of the channel / 채널의 끝
     * @return the wrapper of the new port / 새 포트의 래퍼
     */
    public JsObject receive(JsRealm realm, Object payload) {
        PortTarget port = new PortTarget(context);
        port.bind((PortCore) payload);
        return context.getBridge().wrap(port);
    }
}
