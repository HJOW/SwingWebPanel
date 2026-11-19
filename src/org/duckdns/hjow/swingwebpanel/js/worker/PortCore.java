package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.LinkedList;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * One end of a message channel, independent of any script realm (HTML Standard "Channel messaging"): the entanglement with the other end,
 * the queue of the messages that wait for delivery, the switches <em>enabled</em> and <em>closed</em>, and the context that owns the port at
 * the moment. Both ends of a channel share one lock, so every method may be called from any thread. A message waits in the queue as the
 * immutable serialized tree ({@link ClonedValue}) and is turned into a <code>message</code> event, as a task of the owning context, when it
 * is delivered; the realm of the receiver is therefore the realm of the context that owns the port at that time. When a port is transferred
 * ({@link #detach()}, {@link #attach(ScriptContext, PortTarget)}) the core stays the same object: it keeps its queue and its entanglement,
 * only the owner changes, and the port starts disabled again at the receiver, as the specification says.
 * <p>
 * 메시지 채널의 한쪽 끝으로 어떤 스크립트 realm 에도 속하지 않는다(HTML 표준 "Channel messaging"): 반대쪽 끝과의 얽힘, 전달을 기다리는 메시지의 큐,
 * <em>enabled</em> 와 <em>closed</em> 스위치, 그리고 지금 포트를 소유한 문맥. 채널의 두 끝은 잠금 하나를 공유하므로 모든 메서드를 어느 스레드에서나 부를 수
 * 있다. 메시지는 불변의 직렬화된 트리({@link ClonedValue})로 큐에서 기다리다가, 전달될 때 포트를 소유한 문맥의 작업으로 <code>message</code> 이벤트가 된다.
 * 그러므로 받는 쪽 realm 은 그때 포트를 소유한 문맥의 realm 이다. 포트를 전달({@link #detach()}, {@link #attach(ScriptContext, PortTarget)})해도 코어는
 * 같은 객체로 남는다: 큐와 얽힘을 유지한 채 소유자만 바뀌며, 규격대로 받는 쪽에서는 포트가 다시 비활성 상태로 시작한다.
 */
final class PortCore {

    /**
     * The largest number of messages that wait in the queue of one port; further messages are dropped.
     * <p>
     * 포트 하나의 큐에서 기다릴 수 있는 메시지의 최대 수. 더 오는 메시지는 버린다.
     */
    static final int MAX_QUEUED = 10000;

    /**
     * The lock that both ends of the channel share.
     * <p>
     * 채널의 두 끝이 공유하는 잠금.
     */
    private final Object lock;

    /**
     * The other end, or null when the channel was disentangled; guarded by {@link #lock}.
     * <p>
     * 반대쪽 끝. 채널의 얽힘이 풀렸으면 null. {@link #lock} 으로 보호한다.
     */
    private PortCore peer;

    /**
     * The messages that wait for delivery; guarded by {@link #lock}.
     * <p>
     * 전달을 기다리는 메시지. {@link #lock} 으로 보호한다.
     */
    private final LinkedList<ClonedValue> queue = new LinkedList<ClonedValue>();

    /**
     * True once the port delivers its messages; guarded by {@link #lock}.
     * <p>
     * 포트가 메시지를 전달하기 시작했으면 true. {@link #lock} 으로 보호한다.
     */
    private boolean enabled;

    /**
     * True once the port was closed; guarded by {@link #lock}.
     * <p>
     * 포트가 닫혔으면 true. {@link #lock} 으로 보호한다.
     */
    private boolean closed;

    /**
     * The context that owns the port now, or null while the port is on its way (transferred and not yet received); guarded by {@link #lock}.
     * <p>
     * 지금 포트를 소유한 문맥. 포트가 가는 중(전달되었고 아직 받지 않음)이면 null. {@link #lock} 으로 보호한다.
     */
    private ScriptContext owner;

    /**
     * The script visible port of the owner, or null; guarded by {@link #lock}.
     * <p>
     * 소유자에게 보이는 스크립트 포트. 없으면 null. {@link #lock} 으로 보호한다.
     */
    private PortTarget target;

    /**
     * Creates an end that is not entangled yet.
     * <p>
     * 아직 얽히지 않은 끝을 만든다.
     *
     * @param lock the lock of the channel / 채널의 잠금
     */
    private PortCore(Object lock) {
        this.lock = lock;
    }

    /**
     * Creates the two entangled ends of a new channel.
     * <p>
     * 새 채널의 서로 얽힌 두 끝을 만든다.
     *
     * @return the two ends / 두 끝
     */
    static PortCore[] pair() {
        Object lock = new Object();
        PortCore a = new PortCore(lock);
        PortCore b = new PortCore(lock);
        a.peer = b;
        b.peer = a;
        return new PortCore[] { a, b };
    }

    /**
     * Gives the port to a context (a new port, or a transferred port that arrived): it is disabled and keeps what waits in its queue.
     * <p>
     * 포트를 문맥에 준다(새 포트이거나 도착한 전달 포트): 비활성이며 큐에서 기다리던 것은 유지한다.
     *
     * @param context the owner / 소유자
     * @param port    its script visible port / 소유자에게 보이는 스크립트 포트
     */
    void attach(ScriptContext context, PortTarget port) {
        synchronized (lock) {
            owner = context;
            target = port;
            enabled = false;
        }
    }

    /**
     * Takes the port away from its owner (it is being transferred): nothing is delivered until another context received it.
     * <p>
     * 포트를 소유자에게서 거둔다(전달 중). 다른 문맥이 받을 때까지 아무것도 전달되지 않는다.
     */
    void detach() {
        synchronized (lock) {
            owner = null;
            target = null;
            enabled = false;
        }
    }

    /**
     * Tells whether the port was closed.
     * <p>
     * 포트가 닫혔는지 알려 준다.
     *
     * @return true after {@link #close()} / {@link #close()} 뒤에는 true
     */
    boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /**
     * Returns the other end.
     * <p>
     * 반대쪽 끝을 반환한다.
     *
     * @return the other end, or null if the channel is disentangled / 반대쪽 끝. 얽힘이 풀렸으면 null
     */
    PortCore getPeer() {
        synchronized (lock) {
            return peer;
        }
    }

    /**
     * Starts the delivery (<code>start()</code>, or the first assignment of <code>onmessage</code>): what already waits is delivered in
     * order, as one task of the owner per message.
     * <p>
     * 전달을 시작한다(<code>start()</code> 또는 <code>onmessage</code> 의 첫 지정). 이미 기다리던 것은 메시지마다 소유자의 작업 하나로 순서대로 전달된다.
     */
    void start() {
        ScriptContext o;
        int n;
        synchronized (lock) {
            if (enabled || closed || owner == null) {
                return;
            }
            enabled = true;
            o = owner;
            n = queue.size();
        }
        // 기다리던 메시지마다 전달 작업을 소유자에게 올린다.
        for (int i = 0; i < n; i++) {
            schedule(o);
        }
    }

    /**
     * Posts a message to the other end (the message was serialized by the caller); it is dropped if the channel is disentangled.
     * <p>
     * 반대쪽 끝으로 메시지를 보낸다(메시지는 호출자가 직렬화했다). 채널의 얽힘이 풀렸으면 버린다.
     *
     * @param message the serialized message / 직렬화된 메시지
     */
    void postToPeer(ClonedValue message) {
        ScriptContext o = null;
        PortCore p;
        synchronized (lock) {
            p = peer;
            if (p == null || closed || p.closed || p.queue.size() >= MAX_QUEUED) {
                return;
            }
            p.queue.add(message);
            if (p.enabled) {
                o = p.owner;
            }
        }
        // 전달 작업은 메시지를 받는 쪽 끝의 것이다.
        if (o != null) {
            p.schedule(o);
        }
    }

    /**
     * Asks a context to deliver one message of this port.
     * <p>
     * 문맥에 이 포트의 메시지 하나를 전달하라고 요청한다.
     *
     * @param context the owner at the time of the request / 요청 시점의 소유자
     */
    private void schedule(final ScriptContext context) {
        context.post(new Runnable() {
            /**
             * Delivers one message as a task of the context.
             * <p>
             * 문맥의 작업으로 메시지 하나를 전달한다.
             */
            public void run() {
                deliverOne(context);
            }
        });
    }

    /**
     * Delivers the oldest message as a <code>message</code> event (a task of the owner). Nothing happens if the port was closed, was moved to
     * another context in the meantime, or has nothing waiting.
     * <p>
     * 가장 오래된 메시지를 <code>message</code> 이벤트로 전달한다(소유자의 작업). 포트가 닫혔거나 그 사이 다른 문맥으로 옮겨졌거나 기다리는 것이 없으면
     * 아무 일도 없다.
     *
     * @param context the context that runs the task / 작업을 실행하는 문맥
     */
    private void deliverOne(ScriptContext context) {
        ClonedValue message;
        PortTarget t;
        synchronized (lock) {
            if (closed || !enabled || owner != context || queue.isEmpty()) {
                return;
            }
            message = queue.removeFirst();
            t = target;
        }
        if (t != null) {
            t.deliver(message);
        }
    }

    /**
     * Closes the port (<code>close()</code>): it is disentangled from the other end, what waits in its queue is dropped and nothing more is
     * delivered. What was posted to the other end earlier stays there.
     * <p>
     * 포트를 닫는다(<code>close()</code>): 반대쪽 끝과의 얽힘이 풀리고, 큐에서 기다리던 것은 버려지며, 더는 아무것도 전달되지 않는다. 이전에 반대쪽 끝으로 보낸
     * 것은 그쪽에 남는다.
     */
    void close() {
        synchronized (lock) {
            closed = true;
            queue.clear();
            owner = null;
            target = null;
            // 반대쪽 끝도 얽힘이 풀려 더는 이쪽으로 보내지 못한다.
            if (peer != null) {
                peer.peer = null;
                peer = null;
            }
        }
    }
}
