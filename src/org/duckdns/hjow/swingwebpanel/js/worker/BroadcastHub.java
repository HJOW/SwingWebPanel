package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * The registry of the <code>BroadcastChannel</code> objects that can hear each other: the channels of one origin in the pages of one component,
 * in their dedicated workers and in the shared workers. They are in the same origin by construction (a worker must come from the origin of its
 * page, and a hub belongs to one origin), and the hub is not shared with other components or other processes ({@link WorkerRegistry} keeps it in
 * the component). A message posted to a channel goes to every other open channel of the same name, each as a task of the context that owns that
 * channel. All methods may be called from any thread.
 * <p>
 * 서로 들을 수 있는 <code>BroadcastChannel</code> 객체의 등록부: 한 컴포넌트의 페이지, 그 전용 worker, 공유 worker 에 있는 한 오리진의 채널. 같은 오리진에
 * 있음이 구조로 보장되며(worker 는 페이지의 오리진에서 와야 하고 허브는 한 오리진에 속한다) 허브는 다른 컴포넌트나 다른 프로세스와 공유하지 않는다
 * ({@link WorkerRegistry} 가 컴포넌트 안에 보관한다). 채널에 보낸 메시지는 같은 이름의 다른 모든 열린 채널로, 각각 그 채널을 소유한 문맥의 작업으로 간다.
 * 모든 메서드는 어느 스레드에서나 부를 수 있다.
 */
final class BroadcastHub {

    /**
     * The open channels; guarded by itself.
     * <p>
     * 열린 채널. 자기 자신으로 보호한다.
     */
    private final List<BroadcastChannelTarget> members = new ArrayList<BroadcastChannelTarget>();

    /**
     * Creates an empty hub.
     * <p>
     * 빈 허브를 만든다.
     */
    BroadcastHub() {
    }

    /**
     * Registers a channel.
     * <p>
     * 채널을 등록한다.
     *
     * @param channel the channel / 채널
     * @return false if the hub has too many channels / 허브에 채널이 너무 많으면 false
     */
    boolean add(BroadcastChannelTarget channel) {
        synchronized (members) {
            if (members.size() >= WorkerLimits.MAX_BROADCAST_CHANNELS) {
                return false;
            }
            members.add(channel);
            return true;
        }
    }

    /**
     * Removes a channel.
     * <p>
     * 채널을 제거한다.
     *
     * @param channel the channel / 채널
     */
    void remove(BroadcastChannelTarget channel) {
        synchronized (members) {
            members.remove(channel);
        }
    }

    /**
     * Closes and removes every channel of a context (the context ended).
     * <p>
     * 한 문맥의 모든 채널을 닫고 제거한다(문맥이 끝났다).
     *
     * @param context the context / 문맥
     */
    void removeContext(ScriptContext context) {
        List<BroadcastChannelTarget> mine = new ArrayList<BroadcastChannelTarget>();
        synchronized (members) {
            // 이 문맥이 소유한 채널을 찾아 목록에서 뺀다.
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getContext() == context) {
                    mine.add(members.remove(i));
                }
            }
        }
        // 뺀 채널은 더 이상 받지 않도록 닫는다.
        for (int i = 0; i < mine.size(); i++) {
            mine.get(i).close();
        }
    }

    /**
     * Delivers a message to every other open channel with the name of the sender. Each delivery is posted to the owner of the channel.
     * <p>
     * 보내는 채널의 이름을 가진 다른 모든 열린 채널에 메시지를 전달한다. 전달은 채널의 소유자에게 작업으로 올린다.
     *
     * @param sender  the channel that posted the message / 메시지를 보낸 채널
     * @param message the serialized message / 직렬화된 메시지
     * @param origin  the origin of the sender / 보낸 쪽의 오리진
     */
    void broadcast(BroadcastChannelTarget sender, final ClonedValue message, final String origin) {
        List<BroadcastChannelTarget> targets = new ArrayList<BroadcastChannelTarget>();
        synchronized (members) {
            // 보낸 채널을 뺀 같은 이름의 열린 채널을 모은다.
            for (int i = 0; i < members.size(); i++) {
                BroadcastChannelTarget m = members.get(i);
                if (m != sender && !m.isClosed() && m.getName().equals(sender.getName())) {
                    targets.add(m);
                }
            }
        }
        // 채널마다 소유 문맥의 작업으로 전달을 올린다.
        for (int i = 0; i < targets.size(); i++) {
            final BroadcastChannelTarget target = targets.get(i);
            if (!target.reserve()) {
                continue;
            }
            target.getContext().post(new Runnable() {
                /**
                 * Delivers the message in a task of the owner of the channel.
                 * <p>
                 * 채널 소유자의 작업 안에서 메시지를 전달한다.
                 */
                public void run() {
                    target.deliver(message, origin);
                }
            });
        }
    }
}
