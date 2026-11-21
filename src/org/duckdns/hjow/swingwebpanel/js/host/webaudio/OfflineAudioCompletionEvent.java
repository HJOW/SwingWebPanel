package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The <code>complete</code> event of an offline context (Web Audio API 1.3.5, <code>OfflineAudioCompletionEvent</code>): it carries the buffer that the rendering
 * produced and is the last event of the context, fired after the promise of <code>startRendering()</code> was resolved (the interface exists for legacy reasons).
 * <p>
 * 오프라인 컨텍스트의 <code>complete</code> 이벤트(Web Audio API 1.3.5, <code>OfflineAudioCompletionEvent</code>): 렌더링이 만든 버퍼를 싣고 있으며 컨텍스트의 마지막
 * 이벤트이다. <code>startRendering()</code> 의 프라미스를 resolve 한 뒤에 발생한다(인터페이스는 옛 호환을 위해 있다).
 */
final class OfflineAudioCompletionEvent extends Event {

    /**
     * The rendered buffer.
     * <p>
     * 렌더링한 버퍼.
     */
    private final AudioBufferValue renderedBuffer;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type           the event type, <code>complete</code> / 이벤트 종류. <code>complete</code>
     * @param bubbles        whether it bubbles / 버블링 여부
     * @param cancelable     whether it can be cancelled / 취소 가능 여부
     * @param renderedBuffer the rendered buffer / 렌더링한 버퍼
     */
    OfflineAudioCompletionEvent(String type, boolean bubbles, boolean cancelable, AudioBufferValue renderedBuffer) {
        super(type, bubbles, cancelable);
        this.renderedBuffer = renderedBuffer;
    }

    /**
     * Returns the rendered buffer.
     * <p>
     * 렌더링한 버퍼를 반환한다.
     *
     * @return the buffer / 버퍼
     */
    AudioBufferValue getRenderedBuffer() {
        return renderedBuffer;
    }
}
