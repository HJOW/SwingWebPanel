package org.duckdns.hjow.swingwebpanel.js.worker;

/**
 * The Java side of a <code>MessageChannel</code> object (HTML Standard "Message channels"): it only holds the two entangled
 * <code>MessagePort</code> objects <code>port1</code> and <code>port2</code> that were created together in one context.
 * <p>
 * <code>MessageChannel</code> 객체의 Java 쪽(HTML 표준 "Message channels"): 한 문맥에서 함께 만든, 서로 얽힌 <code>MessagePort</code> 객체
 * <code>port1</code> 과 <code>port2</code> 만 가진다.
 */
public final class ChannelTarget {

    /**
     * The first port.
     * <p>
     * 첫째 포트.
     */
    private final PortTarget port1;

    /**
     * The second port.
     * <p>
     * 둘째 포트.
     */
    private final PortTarget port2;

    /**
     * Creates the channel of a context: two ports that are entangled and owned by the context.
     * <p>
     * 문맥의 채널을 만든다: 서로 얽혀 있고 문맥이 소유하는 포트 둘.
     *
     * @param context the context that creates the channel / 채널을 만드는 문맥
     */
    ChannelTarget(ScriptContext context) {
        PortCore[] ends = PortCore.pair();
        port1 = new PortTarget(context);
        port2 = new PortTarget(context);
        port1.bind(ends[0]);
        port2.bind(ends[1]);
    }

    /**
     * Returns the first port (<code>port1</code>).
     * <p>
     * 첫째 포트(<code>port1</code>)를 반환한다.
     *
     * @return the port / 포트
     */
    public PortTarget getPort1() {
        return port1;
    }

    /**
     * Returns the second port (<code>port2</code>).
     * <p>
     * 둘째 포트(<code>port2</code>)를 반환한다.
     *
     * @return the port / 포트
     */
    public PortTarget getPort2() {
        return port2;
    }
}
