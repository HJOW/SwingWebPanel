package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.dom.Event;

/**
 * The <code>audioprocess</code> event of a script processor (Web Audio API 1.29.5, <code>AudioProcessingEvent</code>, deprecated): it carries the audio that the
 * node received (<code>inputBuffer</code>), the buffer that the handler fills (<code>outputBuffer</code>) and the time at which the audio will be played
 * (<code>playbackTime</code>). The interface can be constructed by scripts; the node that fires it is {@code ScriptProcessorNode} (see {@code ContextApi}: the event is built over the
 * buffers of a block of the node, and what a handler wrote into <code>outputBuffer</code> is taken back into the block).
 * <p>
 * 스크립트 프로세서의 <code>audioprocess</code> 이벤트(Web Audio API 1.29.5, <code>AudioProcessingEvent</code>, 폐기 예정): 노드가 받은 오디오(<code>inputBuffer</code>),
 * 핸들러가 채우는 버퍼(<code>outputBuffer</code>), 오디오가 재생될 시간(<code>playbackTime</code>)을 싣는다. 인터페이스는 스크립트가 생성할 수 있다. 이것을 발생시키는
 * 노드는 {@code ScriptProcessorNode} 이다({@code ContextApi} 참고: 이벤트는 노드의 블록의 버퍼 위에 만들어지고, 핸들러가 <code>outputBuffer</code> 에 쓴 것은 블록으로 되가져간다).
 */
final class AudioProcessingEvent extends Event {

    /**
     * The time at which the audio will be played, in the time of the context.
     * <p>
     * 오디오가 재생될 시간(컨텍스트의 시간).
     */
    private final double playbackTime;

    /**
     * The input audio.
     * <p>
     * 입력 오디오.
     */
    private final AudioBufferValue inputBuffer;

    /**
     * The buffer that the handler fills.
     * <p>
     * 핸들러가 채우는 버퍼.
     */
    private final AudioBufferValue outputBuffer;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type         the event type / 이벤트 종류
     * @param bubbles      whether it bubbles / 버블링 여부
     * @param cancelable   whether it can be cancelled / 취소 가능 여부
     * @param playbackTime the playback time / 재생 시간
     * @param inputBuffer  the input audio / 입력 오디오
     * @param outputBuffer the output buffer / 출력 버퍼
     */
    AudioProcessingEvent(String type, boolean bubbles, boolean cancelable, double playbackTime, AudioBufferValue inputBuffer, AudioBufferValue outputBuffer) {
        super(type, bubbles, cancelable);
        this.playbackTime = playbackTime;
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
    }

    /**
     * Returns the playback time.
     * <p>
     * 재생 시간을 반환한다.
     *
     * @return the time in seconds / 시간(초)
     */
    double getPlaybackTime() {
        return playbackTime;
    }

    /**
     * Returns the input audio.
     * <p>
     * 입력 오디오를 반환한다.
     *
     * @return the buffer / 버퍼
     */
    AudioBufferValue getInputBuffer() {
        return inputBuffer;
    }

    /**
     * Returns the output buffer.
     * <p>
     * 출력 버퍼를 반환한다.
     *
     * @return the buffer / 버퍼
     */
    AudioBufferValue getOutputBuffer() {
        return outputBuffer;
    }
}
