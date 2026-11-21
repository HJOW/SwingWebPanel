package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioDestinationNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ContextState;
import org.duckdns.hjow.swingwebpanel.media.webaudio.MediaElementTap;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OutputRenderDriver;
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
     * The taps of the media elements that were connected to this context with <code>createMediaElementSource</code>, which are released when the context is closed (the elements stay
     * bound to their nodes for good and go on playing silently). Any thread may read the list.
     * <p>
     * 이 컨텍스트에 <code>createMediaElementSource</code> 로 연결한 미디어 요소의 탭. 컨텍스트를 닫을 때 놓는다(요소는 영구히 자기 노드에 묶인 채 소리 없이 계속 재생된다). 어느 스레드든
     * 목록을 읽을 수 있다.
     */
    private final List<MediaElementTap> taps = new CopyOnWriteArrayList<MediaElementTap>();

    /**
     * The script object of this context, held weakly: the page list recognizes a context that no script can reach any more (see {@link WebAudioApi#register}).
     * <p>
     * 이 컨텍스트의 스크립트 객체(약한 참조). 페이지 목록은 어떤 스크립트도 더는 닿을 수 없는 컨텍스트를 이것으로 알아본다({@link WebAudioApi#register} 참고).
     */
    private volatile WeakReference<JsObject> wrapper;

    /**
     * The script objects of the nodes of this context, held weakly (guarded by itself).
     * <p>
     * 이 컨텍스트의 노드의 스크립트 객체(약한 참조. 자기 자신으로 보호).
     */
    private final List<WeakReference<JsObject>> nodeWrappers = new ArrayList<WeakReference<JsObject>>();

    /**
     * The size of {@link #nodeWrappers} at which the collected references are removed next (guarded by {@link #nodeWrappers}).
     * <p>
     * 수거된 참조를 다음에 지우는 {@link #nodeWrappers} 의 크기({@link #nodeWrappers} 로 보호).
     */
    private int nextPrune = 1024;

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
     * Returns the driver that plays a real-time context to an output, or null for an offline context.
     * <p>
     * 실시간 컨텍스트를 출력으로 재생하는 드라이버를 반환한다. 오프라인 컨텍스트면 null.
     *
     * @return the driver or null / 드라이버. 없으면 null
     */
    OutputRenderDriver outputDriver() {
        RealtimeAudioContext r = realtime();
        return r != null && r.getDriver() instanceof OutputRenderDriver ? (OutputRenderDriver) r.getDriver() : null;
    }

    /**
     * Remembers the script object of this context (weakly).
     * <p>
     * 이 컨텍스트의 스크립트 객체를 기억한다(약하게).
     *
     * @param w the script object / 스크립트 객체
     */
    void bindWrapper(JsObject w) {
        this.wrapper = new WeakReference<JsObject>(w);
    }

    /**
     * Remembers the script object of a node of this context (weakly): a node object gives a script the way back to its context (<code>node.context</code>), so the context is out of
     * reach only when the objects of all its nodes are gone as well.
     * <p>
     * 이 컨텍스트의 노드의 스크립트 객체를 기억한다(약하게): 노드 객체는 스크립트에 자기 컨텍스트로 가는 길(<code>node.context</code>)을 주므로, 컨텍스트는 모든 노드의 객체도 사라졌을 때에만
     * 닿을 수 없다.
     *
     * @param w the script object of a node / 노드의 스크립트 객체
     */
    void trackNodeWrapper(JsObject w) {
        synchronized (nodeWrappers) {
            // 많이 쌓이면 이미 수거된 것을 먼저 지워 목록이 노드 수만큼 계속 자라지 않게 한다.
            if (nodeWrappers.size() >= nextPrune) {
                for (int i = nodeWrappers.size() - 1; i >= 0; i--) {
                    if (nodeWrappers.get(i).get() == null) {
                        nodeWrappers.remove(i);
                    }
                }
                nextPrune = Math.max(1024, nodeWrappers.size() * 2);
            }
            nodeWrappers.add(new WeakReference<JsObject>(w));
        }
    }

    /**
     * Tells whether no script can reach this context any more: the script object of the context and the script objects of all its nodes were collected (see
     * {@link WebAudioApi#register} for what the page does with such a context).
     * <p>
     * 어떤 스크립트도 더는 이 컨텍스트에 닿을 수 없는지 알려 준다: 컨텍스트의 스크립트 객체와 모든 노드의 스크립트 객체가 수거되었다(페이지가 그런 컨텍스트로 무엇을 하는지는
     * {@link WebAudioApi#register} 참고).
     *
     * @return true if the context is out of reach of the scripts / 스크립트가 닿을 수 없으면 true
     */
    boolean isUnreachable() {
        WeakReference<JsObject> w = wrapper;
        if (w == null || w.get() != null) {
            return false;
        }
        synchronized (nodeWrappers) {
            // 노드의 스크립트 객체가 하나라도 남아 있으면 스크립트가 컨텍스트에 닿을 수 있다.
            for (int i = 0; i < nodeWrappers.size(); i++) {
                if (nodeWrappers.get(i).get() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tells whether the page may give up this context to make room: it is a real-time context that was not closed, that is suspended (it plays nothing and nothing is scheduled on it that
     * a script could see) and that no script can reach any more (nobody can resume it). A running context is never given up: it can make sound and events, and a browser keeps it alive
     * until it is closed too.
     * <p>
     * 페이지가 자리를 만들려고 이 컨텍스트를 포기해도 되는지 알려 준다: 닫지 않은 실시간 컨텍스트이고, 멈춰 있으며(아무것도 재생하지 않고 스크립트가 볼 수 있는 예약된 것이 없다), 어떤
     * 스크립트도 더는 닿을 수 없다(아무도 재개할 수 없다). 실행 중인 컨텍스트는 포기하지 않는다. 소리와 이벤트를 만들 수 있고 브라우저도 닫힐 때까지 살려 둔다.
     *
     * @return true if the page may give it up / 포기해도 되면 true
     */
    boolean isAbandoned() {
        RealtimeAudioContext r = realtime();
        return r != null && !closeRequested && r.getState() == ContextState.SUSPENDED && isUnreachable();
    }

    /**
     * Closes a context that the page gave up: the engine stops (the thread and the output of its driver end), the decoding service and the taps of media elements are released.
     * <p>
     * 페이지가 포기한 컨텍스트를 닫는다: 엔진이 멈추고(드라이버의 스레드와 출력이 끝난다) 디코딩 서비스와 미디어 요소의 탭을 놓는다.
     */
    void closeAbandoned() {
        closeRequested = true;
        RealtimeAudioContext r = realtime();
        if (r != null) {
            r.close();
        }
        closeDecoder();
        releaseTaps();
    }

    /**
     * Adds the tap of a media element that was connected to this context.
     * <p>
     * 이 컨텍스트에 연결한 미디어 요소의 탭을 더한다.
     *
     * @param t the tap / 탭
     */
    void addTap(MediaElementTap t) {
        taps.add(t);
    }

    /**
     * Releases the taps of the media elements that were connected to this context (called when the context is closed or the page ends): the elements go on, silently.
     * <p>
     * 이 컨텍스트에 연결한 미디어 요소의 탭을 놓는다(컨텍스트를 닫거나 페이지가 끝날 때 부른다): 요소는 소리 없이 계속 진행한다.
     */
    void releaseTaps() {
        // 연결된 요소의 탭을 하나씩 놓는다.
        for (MediaElementTap t : taps) {
            t.release();
        }
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
