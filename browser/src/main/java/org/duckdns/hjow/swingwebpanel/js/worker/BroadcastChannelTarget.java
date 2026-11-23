package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;
import org.duckdns.hjow.swingwebpanel.dom.MessageEvent;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * The Java side of one <code>BroadcastChannel</code> object (HTML Standard "Broadcasting to other browsing contexts"): a named channel that
 * belongs to one script context. A message that another channel of the same name posts reaches it as a <code>message</code> event, as a task
 * of the context that owns the channel, never while the sender runs. A closed channel receives nothing.
 * <p>
 * <code>BroadcastChannel</code> 객체 하나의 Java 쪽(HTML 표준 "Broadcasting to other browsing contexts"): 스크립트 문맥 하나에 속한 이름 있는 채널.
 * 같은 이름의 다른 채널이 보낸 메시지는 채널을 소유한 문맥의 작업으로, 보내는 쪽이 실행되는 동안이 아닐 때 <code>message</code> 이벤트로 도착한다.
 * 닫힌 채널은 아무것도 받지 않는다.
 */
public final class BroadcastChannelTarget extends EventTarget {

    /**
     * The name of the channel.
     * <p>
     * 채널의 이름.
     */
    private final String name;

    /**
     * The context that owns the channel.
     * <p>
     * 채널을 소유한 문맥.
     */
    private final ScriptContext context;

    /**
     * True once the channel is closed.
     * <p>
     * 채널이 닫혔으면 true.
     */
    private volatile boolean closed;

    /**
     * The number of messages that were posted to this channel and are not delivered yet (a flood limit).
     * <p>
     * 이 채널로 보내졌으나 아직 전달되지 않은 메시지 수(폭주 한도).
     */
    private final AtomicInteger pending = new AtomicInteger();

    /**
     * The hub that the channel belongs to.
     * <p>
     * 채널이 속한 허브.
     */
    private final BroadcastHub hub;

    /**
     * Creates a channel and registers it with the hub.
     * <p>
     * 채널을 만들고 허브에 등록한다.
     *
     * @param name    the channel name / 채널 이름
     * @param context the owning context / 소유 문맥
     * @param hub     the hub / 허브
     */
    BroadcastChannelTarget(String name, ScriptContext context, BroadcastHub hub) {
        this.name = name;
        this.context = context;
        this.hub = hub;
    }

    /**
     * Returns the name of the channel (<code>BroadcastChannel.name</code>).
     * <p>
     * 채널의 이름을 반환한다(<code>BroadcastChannel.name</code>).
     *
     * @return the name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the context that owns the channel.
     * <p>
     * 채널을 소유한 문맥을 반환한다.
     *
     * @return the context / 문맥
     */
    ScriptContext getContext() {
        return context;
    }

    /**
     * Tells whether the channel was closed.
     * <p>
     * 채널이 닫혔는지 알려 준다.
     *
     * @return true after <code>close()</code> / <code>close()</code> 뒤에는 true
     */
    boolean isClosed() {
        return closed;
    }

    /**
     * Closes the channel (<code>BroadcastChannel.close()</code>): it leaves the hub and receives nothing more. Closing twice does nothing.
     * <p>
     * 채널을 닫는다(<code>BroadcastChannel.close()</code>). 허브에서 빠지고 더 이상 받지 않는다. 두 번 닫아도 아무 일도 없다.
     */
    public void close() {
        closed = true;
        hub.remove(this);
    }

    /**
     * Reserves a place for one more undelivered message.
     * <p>
     * 전달되지 않은 메시지 하나를 더 받을 자리를 잡는다.
     *
     * @return false if the channel has too many undelivered messages (the message is dropped) / 전달되지 않은 메시지가 너무 많으면 false(메시지를 버린다)
     */
    boolean reserve() {
        if (pending.incrementAndGet() > WorkerLimits.MAX_PENDING_MESSAGES) {
            pending.decrementAndGet();
            return false;
        }
        return true;
    }

    /**
     * Delivers a message as a <code>message</code> event; call it inside a task of the owning context.
     * <p>
     * 메시지를 <code>message</code> 이벤트로 전달한다. 소유 문맥의 작업 안에서 부른다.
     *
     * @param message the serialized message / 직렬화된 메시지
     * @param origin  the origin of the sender / 보낸 쪽의 오리진
     */
    void deliver(ClonedValue message, String origin) {
        pending.decrementAndGet();
        if (closed) {
            return;
        }
        MessageEvent event = Messages.event(context, message, origin);
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
