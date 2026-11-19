package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * The Java side of one <code>MessagePort</code> object of a script context (HTML Standard "Message ports"): the event target at which the
 * <code>message</code> events of the port are dispatched, bound to the {@link PortCore} (the end of the channel) that it shows to the script.
 * A port that was transferred to another context is detached: it loses its core, so it does nothing any more, while the same end of the
 * channel lives on as a new port object of the receiving context. Every method must be called from a task of the context that owns the
 * object, except {@link #core()}.
 * <p>
 * 스크립트 문맥의 <code>MessagePort</code> 객체 하나의 Java 쪽(HTML 표준 "Message ports"): 포트의 <code>message</code> 이벤트가 전달되는 이벤트 대상이며,
 * 스크립트에 보여 주는 채널의 끝 {@link PortCore} 에 묶여 있다. 다른 문맥으로 전달된 포트는 분리되어 코어를 잃고 더 이상 아무 일도 하지 않으며, 채널의 같은
 * 끝은 받는 문맥의 새 포트 객체로 계속 산다. {@link #core()} 를 뺀 모든 메서드는 객체를 소유한 문맥의 작업에서 불러야 한다.
 */
public final class PortTarget extends EventTarget {

    /**
     * The context that owns this object.
     * <p>
     * 이 객체를 소유한 문맥.
     */
    private final ScriptContext context;

    /**
     * The end of the channel that this port shows, or null once the port was transferred away.
     * <p>
     * 이 포트가 보여 주는 채널의 끝. 포트가 전달되어 나갔으면 null.
     */
    private volatile PortCore core;

    /**
     * Creates a port that has no core yet.
     * <p>
     * 아직 코어가 없는 포트를 만든다.
     *
     * @param context the context that owns the port / 포트를 소유하는 문맥
     */
    PortTarget(ScriptContext context) {
        this.context = context;
    }

    /**
     * Binds the port to the end of a channel and makes this context its owner.
     * <p>
     * 포트를 채널의 끝에 묶고 이 문맥을 소유자로 삼는다.
     *
     * @param c the end of the channel / 채널의 끝
     */
    void bind(PortCore c) {
        this.core = c;
        c.attach(context, this);
    }

    /**
     * Returns the end of the channel that this port shows.
     * <p>
     * 이 포트가 보여 주는 채널의 끝을 반환한다.
     *
     * @return the end, or null for a transferred port / 끝. 전달된 포트이면 null
     */
    PortCore core() {
        return core;
    }

    /**
     * Cuts the port off after it was transferred: it shows no channel any more.
     * <p>
     * 포트가 전달된 뒤 끊는다: 더 이상 채널을 보여 주지 않는다.
     */
    void neuter() {
        this.core = null;
    }

    /**
     * Starts the delivery of the messages of the port (<code>start()</code>); the first assignment of <code>onmessage</code> does it too.
     * <p>
     * 포트의 메시지 전달을 시작한다(<code>start()</code>). <code>onmessage</code> 의 첫 지정도 같은 일을 한다.
     */
    public void start() {
        PortCore c = core;
        if (c != null) {
            c.start();
        }
    }

    /**
     * Closes the port (<code>close()</code>): it is disentangled and delivers nothing any more. Closing twice does nothing.
     * <p>
     * 포트를 닫는다(<code>close()</code>): 얽힘이 풀리고 더 이상 아무것도 전달하지 않는다. 두 번 닫아도 아무 일도 없다.
     */
    public void close() {
        PortCore c = core;
        if (c != null) {
            c.close();
        }
    }

    /**
     * The steps of <code>port.postMessage(message, transfer)</code>: reads the transfer list, refuses a list that holds the port itself
     * (<code>DataCloneError</code>), serializes the message with the structured clone algorithm (a failure is a <code>DataCloneError</code>),
     * and posts the serialized message to the other end of the channel. The objects of the transfer list are given up once the serialization
     * succeeded, also if the message is not delivered because the port is closed or because the other end of the channel itself was in the
     * transfer list (the channel is lost then, as the specification says).
     * <p>
     * <code>port.postMessage(message, transfer)</code> 의 단계: 전달 목록을 읽고, 포트 자신이 든 목록은 거부하고(<code>DataCloneError</code>), 구조화 복제
     * 알고리즘으로 메시지를 직렬화하고(실패는 <code>DataCloneError</code>), 직렬화된 메시지를 채널의 반대쪽 끝으로 보낸다. 전달 목록의 객체는 직렬화에
     * 성공하면 내놓으며, 포트가 닫혔거나 채널의 반대쪽 끝 자신이 전달 목록에 있어서 메시지가 전달되지 않을 때도 마찬가지이다(그때는 규격대로 채널을 잃는다).
     *
     * @param message     the message / 메시지
     * @param transferArg the transfer argument (an array or <code>{transfer}</code>) / 전달 인수(배열 또는 <code>{transfer}</code>)
     */
    void post(JsValue message, JsValue transferArg) {
        List<JsObject> transfer = Messages.transferList(context, transferArg);
        PortCore mine = core;
        PortCore other = mine == null ? null : mine.getPeer();
        boolean doomed = false;
        // 전달 목록에 이 포트 자신이 있으면 오류이고, 채널의 반대쪽 끝이 있으면 메시지는 갈 곳이 없어 사라진다.
        if (transfer != null) {
            for (int i = 0; i < transfer.size(); i++) {
                PortTarget t = (PortTarget) DomBridge.targetOf(transfer.get(i), PortTarget.class);
                if (t == this) {
                    throw Messages.cloneError(context, "Transfer list contains source port.");
                }
                if (t == null || t.core == null) {
                    continue;
                }
                if (t.core == mine) {
                    throw Messages.cloneError(context, "Transfer list contains source port.");
                }
                if (t.core == other) {
                    doomed = true;
                }
            }
        }
        ClonedValue cloned = Messages.serializeWith(context, message, transfer);
        if (mine == null || doomed) {
            return;
        }
        mine.postToPeer(cloned);
    }

    /**
     * Dispatches one message of this port as a <code>message</code> event (a task of the owner).
     * <p>
     * 이 포트의 메시지 하나를 <code>message</code> 이벤트로 전달한다(소유자의 작업).
     *
     * @param data the serialized message / 직렬화된 메시지
     */
    void deliver(ClonedValue data) {
        MessageEvent event = Messages.event(context, data, "");
        try {
            dispatchTrustedEvent(event);
        } catch (DomException e) {
            throw context.getBridge().domError(e);
        }
    }

    /**
     * The failures of the listeners of this port go to the handler of the context that owns it.
     * <p>
     * 이 대상의 리스너 실패는 이것을 소유한 문맥의 핸들러로 간다.
     *
     * @return the handler of the context / 문맥의 핸들러
     */
    protected ListenerErrorHandler getListenerErrorHandler() {
        return context.getListenerErrors();
    }
}
