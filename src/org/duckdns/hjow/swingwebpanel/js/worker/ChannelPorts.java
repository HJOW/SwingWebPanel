package org.duckdns.hjow.swingwebpanel.js.worker;

/**
 * The way for other script contexts (the audio worklet of the Web Audio API, for example) to use the channel messaging of this package: it installs the
 * <code>MessagePort</code> interface into the realm of a {@link ScriptContext} and creates a channel whose two ends belong to two contexts at once. The ports
 * behave exactly like the ports of <code>MessageChannel</code>: a message is serialized with the structured clone algorithm in the realm of the sender and
 * becomes a <code>message</code> event as a task of the context that owns the receiving port (see {@link ScriptContext#post(Runnable)}), the port delivers after
 * <code>start()</code> or after the first assignment of <code>onmessage</code>, and a port can be closed.
 * <p>
 * 다른 스크립트 문맥(예: Web Audio API 의 오디오 worklet)이 이 패키지의 채널 메시징을 쓰는 통로: {@link ScriptContext} 의 realm 에 <code>MessagePort</code> 인터페이스를 설치하고, 두 끝이
 * 두 문맥에 하나씩 속한 채널을 만든다. 포트는 <code>MessageChannel</code> 의 포트와 똑같이 동작한다: 메시지는 보내는 쪽 realm 에서 구조화 복제 알고리즘으로 직렬화되고 받는 포트를
 * 소유한 문맥의 작업({@link ScriptContext#post(Runnable)} 참고)으로 <code>message</code> 이벤트가 되며, 포트는 <code>start()</code> 나 <code>onmessage</code> 의 첫 지정 뒤에 전달하고,
 * 포트를 닫을 수 있다.
 */
public final class ChannelPorts {

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private ChannelPorts() {
    }

    /**
     * Installs the <code>MessagePort</code> interface (<code>postMessage</code>, <code>start</code>, <code>close</code>, <code>onmessage</code>,
     * <code>onmessageerror</code>) into the realm of a context, together with the <code>MessageChannel</code> constructor that the page and the workers have. Call it
     * once, on the thread that owns the realm, after the event interfaces and <code>MessageEvent</code> exist.
     * <p>
     * 문맥의 realm 에 <code>MessagePort</code> 인터페이스(<code>postMessage</code>, <code>start</code>, <code>close</code>, <code>onmessage</code>, <code>onmessageerror</code>)를, 페이지와
     * worker 가 가진 <code>MessageChannel</code> 생성자와 함께 설치한다. realm 을 소유한 스레드에서, 이벤트 인터페이스와 <code>MessageEvent</code> 가 생긴 뒤 한 번 부른다.
     *
     * @param context the context, not null / 문맥(null 불가)
     */
    public static void install(ScriptContext context) {
        PortApi.install(context);
    }

    /**
     * Creates a channel whose ends belong to two contexts: the first port to the first context, the second port to the second one. The ports are not wrapped for
     * scripts yet: the owner wraps its port with the DOM bridge of its realm, inside a task of its context. Callable from any thread.
     * <p>
     * 두 문맥에 끝이 하나씩 속하는 채널을 만든다: 첫 포트는 첫 문맥에, 둘째 포트는 둘째 문맥에 속한다. 포트는 아직 스크립트용으로 감싸지 않았다: 소유자가 자기 문맥의 작업 안에서 자기 realm 의
     * DOM 브리지로 포트를 감싼다. 어느 스레드에서나 부를 수 있다.
     *
     * @param first  the context of the first port, not null / 첫 포트의 문맥(null 불가)
     * @param second the context of the second port, not null / 둘째 포트의 문맥(null 불가)
     * @return the two ports, the first at index 0 / 포트 둘. 첫 포트가 인덱스 0
     */
    public static PortTarget[] newChannel(ScriptContext first, ScriptContext second) {
        PortCore[] pair = PortCore.pair();
        PortTarget a = new PortTarget(first);
        PortTarget b = new PortTarget(second);
        a.bind(pair[0]);
        b.bind(pair[1]);
        return new PortTarget[] {a, b};
    }
}
