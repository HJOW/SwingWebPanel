package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * An instance of a class that extends <code>AudioWorkletProcessor</code> (Web Audio API 1.32.4): an ordinary script object whose prototype comes from the class that
 * <code>new</code> was applied to, with the one thing that the interface adds, the <code>port</code> (the end of the message channel that belongs to the processor). It is created by
 * the constructor of <code>AudioWorkletProcessor</code>, which a subclass reaches through <code>super()</code>.
 * <p>
 * <code>AudioWorkletProcessor</code> 를 확장한 클래스의 인스턴스(Web Audio API 1.32.4): 프로토타입이 <code>new</code> 를 적용한 클래스에서 오는 일반 스크립트 객체이며, 인터페이스가 더하는 것 하나,
 * <code>port</code>(프로세서에 속한 메시지 채널의 끝)를 가진다. 하위 클래스가 <code>super()</code> 로 닿는 <code>AudioWorkletProcessor</code> 의 생성자가 만든다.
 */
final class ProcessorObject extends JsObject {

    /**
     * The script object of the port (the <code>MessagePort</code> of the worklet side of the channel).
     * <p>
     * 포트의 스크립트 객체(채널의 worklet 쪽 <code>MessagePort</code>).
     */
    private final JsObject port;

    /**
     * Creates the instance.
     * <p>
     * 인스턴스를 만든다.
     *
     * @param prototype the prototype of the instance / 인스턴스의 프로토타입
     * @param port      the script object of the port / 포트의 스크립트 객체
     */
    ProcessorObject(JsObject prototype, JsObject port) {
        super(prototype);
        this.port = port;
    }

    /**
     * Returns the port.
     * <p>
     * 포트를 반환한다.
     *
     * @return the <code>MessagePort</code> object / <code>MessagePort</code> 객체
     */
    JsObject getPort() {
        return port;
    }
}
