package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioDestinationNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.RealtimeAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.AudioDecodeService;

/**
 * The script side of a <code>BaseAudioContext</code> (Web Audio API 1.1 to 1.3): an <code>EventTarget</code> (<code>statechange</code>, and
 * <code>complete</code> of an offline context are dispatched at it) that holds the engine context, either a {@link RealtimeAudioContext} for
 * <code>AudioContext</code> or an {@link OfflineAudioContext} for <code>OfflineAudioContext</code>, and everything that exists only in the script interface: the
 * <em>visible</em> state (the <code>state</code> attribute that changes in a task of the page, after the engine did what was asked, with a
 * <code>statechange</code> event), the flags that make later calls fail (a closed context refuses <code>resume()</code>), the decoding service of the context, the thread
 * that renders an offline context, and the part of the memory budget of the page that the context holds.
 * <p>
 * <code>BaseAudioContext</code> 의 스크립트 쪽(Web Audio API 1.1~1.3): <code>EventTarget</code>(<code>statechange</code>, 오프라인 컨텍스트의 <code>complete</code> 가
 * 이것에 전달된다)이며 엔진 컨텍스트(<code>AudioContext</code> 는 {@link RealtimeAudioContext}, <code>OfflineAudioContext</code> 는 {@link OfflineAudioContext})와
 * 스크립트 인터페이스에만 있는 모든 것을 가진다: <em>보이는</em> 상태(엔진이 요청을 처리한 뒤 페이지의 작업에서 <code>statechange</code> 이벤트와 함께 바뀌는
 * <code>state</code> 속성), 이후의 호출을 실패시키는 표시(닫힌 컨텍스트는 <code>resume()</code> 을 거부한다), 컨텍스트의 디코딩 서비스, 오프라인 컨텍스트를 렌더링하는 스레드,
 * 컨텍스트가 쥔 페이지 메모리 예산의 몫.
 */
final class ContextValue extends EventTarget {

    /**
     * The owner of the script interface of the page.
     * <p>
     * 페이지의 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The engine context.
     * <p>
     * 엔진 컨텍스트.
     */
    private final BaseAudioContext engine;

    /**
     * The visible state: <code>suspended</code>, <code>running</code> or <code>closed</code> (the page thread changes it).
     * <p>
     * 보이는 상태: <code>suspended</code>, <code>running</code>, <code>closed</code>(페이지 스레드가 바꾼다).
     */
    private String state = "suspended";

    /**
     * True after <code>close()</code> was called (a real-time context): the control thread state of the specification, which makes the later calls fail at once.
     * <p>
     * <code>close()</code> 를 부른 뒤 true(실시간 컨텍스트): 이후의 호출이 즉시 실패하게 하는 규격의 제어 스레드 상태.
     */
    volatile boolean closeRequested;

    /**
     * True after <code>startRendering()</code> was called (an offline context): the slot <code>[[rendering started]]</code>.
     * <p>
     * <code>startRendering()</code> 을 부른 뒤 true(오프라인 컨텍스트): <code>[[rendering started]]</code> 슬롯.
     */
    boolean renderingStarted;

    /**
     * The destination node, created with the context.
     * <p>
     * 컨텍스트와 함께 만든 목적지 노드.
     */
    NodeValue destination;

    /**
     * The decoding service of the context, created by the first <code>decodeAudioData()</code>.
     * <p>
     * 컨텍스트의 디코딩 서비스. 처음 <code>decodeAudioData()</code> 가 만든다.
     */
    private volatile AudioDecodeService decoder;

    /**
     * The renderer of an offline context, created by <code>startRendering()</code> or by the first <code>suspend()</code>.
     * <p>
     * 오프라인 컨텍스트의 렌더러. <code>startRendering()</code> 이나 첫 <code>suspend()</code> 가 만든다.
     */
    volatile OfflineRender renderer;

    /**
     * The number of samples of the memory budget of the page that the context holds (an offline context), 0 for none.
     * <p>
     * 컨텍스트가 쥔 페이지 메모리 예산의 표본 수(오프라인 컨텍스트). 없으면 0.
     */
    long reservedSamples;

    /**
     * True when the output was switched off by <code>setSinkId({type: 'none'})</code>.
     * <p>
     * <code>setSinkId({type: 'none'})</code> 로 출력을 껐으면 true.
     */
    boolean sinkNone;

    /**
     * Creates the value of a context.
     * <p>
     * 컨텍스트의 값을 만든다.
     *
     * @param api    the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param engine the engine context / 엔진 컨텍스트
     */
    ContextValue(WebAudioApi api, BaseAudioContext engine) {
        this.api = api;
        this.engine = engine;
    }

    /**
     * Returns the owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자를 반환한다.
     *
     * @return the owner / 소유자
     */
    WebAudioApi api() {
        return api;
    }

    /**
     * Returns the engine context.
     * <p>
     * 엔진 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    BaseAudioContext engine() {
        return engine;
    }

    /**
     * Returns the engine context as a real-time context, or null for an offline one.
     * <p>
     * 엔진 컨텍스트를 실시간 컨텍스트로 반환한다. 오프라인이면 null.
     *
     * @return the real-time context or null / 실시간 컨텍스트. 없으면 null
     */
    RealtimeAudioContext realtime() {
        return engine instanceof RealtimeAudioContext ? (RealtimeAudioContext) engine : null;
    }

    /**
     * Returns the engine context as an offline context, or null for a real-time one.
     * <p>
     * 엔진 컨텍스트를 오프라인 컨텍스트로 반환한다. 실시간이면 null.
     *
     * @return the offline context or null / 오프라인 컨텍스트. 없으면 null
     */
    OfflineAudioContext offline() {
        return engine instanceof OfflineAudioContext ? (OfflineAudioContext) engine : null;
    }

    /**
     * Tells whether this is an offline context.
     * <p>
     * 오프라인 컨텍스트인지 알려 준다.
     *
     * @return true for <code>OfflineAudioContext</code> / <code>OfflineAudioContext</code> 이면 true
     */
    boolean isOffline() {
        return engine instanceof OfflineAudioContext;
    }

    /**
     * Returns the visible state.
     * <p>
     * 보이는 상태를 반환한다.
     *
     * @return the state / 상태
     */
    String getState() {
        return state;
    }

    /**
     * Changes the visible state.
     * <p>
     * 보이는 상태를 바꾼다.
     *
     * @param newState the new state / 새 상태
     * @return true if the state changed (a <code>statechange</code> event is due) / 상태가 바뀌었으면 true(<code>statechange</code> 이벤트를 발생시켜야 한다)
     */
    boolean setState(String newState) {
        if (state.equals(newState)) {
            return false;
        }
        state = newState;
        return true;
    }

    /**
     * Returns the destination of the engine context (used to link the node value).
     * <p>
     * 엔진 컨텍스트의 목적지를 반환한다(노드 값을 잇는 데 쓴다).
     *
     * @return the engine destination / 엔진 목적지
     */
    AudioDestinationNode engineDestination() {
        return engine.getDestination();
    }

    /**
     * Returns the decoding service of the context, creating it at the first call.
     * <p>
     * 컨텍스트의 디코딩 서비스를 반환한다. 처음 부를 때 만든다.
     *
     * @return the service / 서비스
     */
    AudioDecodeService decoder() {
        if (decoder == null) {
            decoder = new AudioDecodeService();
        }
        return decoder;
    }

    /**
     * Closes the decoding service if there is one (requests that wait are cancelled without a callback).
     * <p>
     * 디코딩 서비스가 있으면 닫는다(기다리는 요청은 콜백 없이 취소된다).
     */
    void closeDecoder() {
        AudioDecodeService d = decoder;
        if (d != null) {
            d.close();
        }
    }
}
