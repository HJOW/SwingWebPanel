package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A context that renders a graph as fast as possible into an {@link AudioBuffer} instead of to a device (Web Audio API 1.3). The
 * destination has the number of channels that the constructor was given and the sample rate is the one given too. {@link #startRendering()}
 * renders the whole length on the calling thread: before the rendering starts and between the quanta the control methods of the nodes
 * may be called, and the control messages are applied at the start of every quantum, so the result is the same as for a real-time
 * context that renders without delays. The length is rounded up to a whole number of quanta while rendering and the result is cut to the
 * length. The script layer wraps the call in a promise and fires <code>oncomplete</code>; suspending and resuming a rendering is
 * provided by {@link #renderNextQuantum()} and {@link #finishRendering()}.
 * <p>
 * 장치가 아니라 {@link AudioBuffer} 로 그래프를 가능한 한 빠르게 렌더링하는 컨텍스트(Web Audio API 1.3). 목적지는 생성자에 준 채널 수를 가지며
 * 샘플링 주파수도 생성자에 준 값이다. {@link #startRendering()} 은 호출한 스레드에서 전체 길이를 렌더링한다: 렌더링이 시작되기 전과 quantum 사이에
 * 노드의 제어 메서드를 부를 수 있고, 제어 메시지는 quantum 마다 시작에 적용되므로 결과는 지연 없이 렌더링하는 실시간 컨텍스트와 같다. 길이는 렌더링 중에
 * quantum 의 정수 배로 올림하고 결과는 길이로 자른다. 스크립트 계층은 이 호출을 promise 로 감싸고 <code>oncomplete</code> 를 발생시킨다. 렌더링의 일시
 * 중단과 재개는 {@link #renderNextQuantum()} 과 {@link #finishRendering()} 으로 제공한다.
 */
public final class OfflineAudioContext extends BaseAudioContext {

    /**
     * The length of the rendering in frames.
     * <p>
     * 렌더링 길이(프레임).
     */
    private final int length;

    /**
     * The rendered audio; its length is a whole number of quanta.
     * <p>
     * 렌더링한 오디오. 길이는 quantum 의 정수 배이다.
     */
    private final float[][] rendered;

    /**
     * The number of frames of the result that are filled.
     * <p>
     * 결과 중 채운 프레임 수.
     */
    private int filled;

    /**
     * True after the rendering was started.
     * <p>
     * 렌더링을 시작한 뒤 true.
     */
    private boolean started;

    /**
     * Creates an offline context.
     * <p>
     * 오프라인 컨텍스트를 만든다.
     *
     * @param numberOfChannels the number of channels, from 1 to the maximum / 채널 수(1 ~ 최대)
     * @param length           the length in sample frames, at least 1 / 길이(샘플 프레임, 1 이상)
     * @param sampleRate       the sample rate in Hz, from 3000 to 768000 / 샘플링 주파수(Hz, 3000 ~ 768000)
     * @throws AudioException <code>NotSupportedError</code> if an argument is out of range / 인수가 범위 밖이면 <code>NotSupportedError</code>
     */
    public OfflineAudioContext(int numberOfChannels, int length, float sampleRate) {
        this(numberOfChannels, length, sampleRate, WebAudioLimits.RENDER_QUANTUM_SIZE);
    }

    /**
     * Creates an offline context with a render quantum size.
     * <p>
     * render quantum 크기를 정해 오프라인 컨텍스트를 만든다.
     *
     * @param numberOfChannels the number of channels / 채널 수
     * @param length           the length in sample frames / 길이(샘플 프레임)
     * @param sampleRate       the sample rate in Hz / 샘플링 주파수(Hz)
     * @param renderQuantumSize the number of frames of a render quantum / render quantum 의 프레임 수
     */
    public OfflineAudioContext(int numberOfChannels, int length, float sampleRate, int renderQuantumSize) {
        super(sampleRate, renderQuantumSize, checkChannels(numberOfChannels), checkChannels(numberOfChannels), true);
        if (length < 1) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "length " + length + " is less than 1");
        }
        long rounded = ((long) length + renderQuantumSize - 1) / renderQuantumSize * renderQuantumSize;
        if (rounded * numberOfChannels > WebAudioLimits.MAX_BUFFER_SAMPLES) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the rendering needs more than " + WebAudioLimits.MAX_BUFFER_SAMPLES + " samples");
        }
        this.length = length;
        this.rendered = new float[numberOfChannels][(int) rounded];
    }

    /**
     * Checks the number of channels.
     * <p>
     * 채널 수를 검사한다.
     *
     * @param n the number of channels / 채널 수
     * @return the same number / 같은 수
     */
    private static int checkChannels(int n) {
        if (n < 1 || n > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "numberOfChannels " + n + " is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        return n;
    }

    /**
     * Returns the length of the rendering in frames.
     * <p>
     * 렌더링 길이(프레임)를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the number of frames that have been rendered (a whole number of quanta).
     * <p>
     * 렌더링한 프레임 수(quantum 의 정수 배)를 반환한다.
     *
     * @return the rendered frames / 렌더링한 프레임 수
     */
    public int getRenderedFrames() {
        return filled;
    }

    /**
     * Tells whether all frames of the length have been rendered.
     * <p>
     * 길이만큼의 모든 프레임을 렌더링했는지 알려 준다.
     *
     * @return true if the rendering is complete / 렌더링이 끝났으면 true
     */
    public boolean isComplete() {
        return filled >= length;
    }

    /**
     * Renders one quantum and appends it to the result.
     * <p>
     * quantum 하나를 렌더링해 결과에 덧붙인다.
     *
     * @throws AudioException <code>InvalidStateError</code> if the rendering is complete already / 이미 렌더링이 끝났으면 <code>InvalidStateError</code>
     */
    public void renderNextQuantum() {
        if (isComplete()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the rendering is complete");
        }
        if (!started) {
            started = true;
            setState(ContextState.RUNNING);
        }
        renderQuantum();
        AudioBus bus = getDestination().getRenderedBus();
        int quantum = getRenderQuantumSize();
        // 목적지가 받은 오디오를 채널별로 결과에 복사한다.
        for (int c = 0; c < rendered.length; c++) {
            System.arraycopy(bus.channel(c), 0, rendered[c], filled, quantum);
        }
        filled += quantum;
    }

    /**
     * Renders the whole length and returns the result.
     * <p>
     * 전체 길이를 렌더링해 결과를 반환한다.
     *
     * @return the rendered audio / 렌더링한 오디오
     * @throws AudioException <code>InvalidStateError</code> if the rendering was started before / 이미 렌더링을 시작했으면 <code>InvalidStateError</code>
     */
    public AudioBuffer startRendering() {
        if (started) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "startRendering() was called more than once");
        }
        // 길이를 채울 때까지 quantum 을 하나씩 렌더링한다.
        while (!isComplete()) {
            renderNextQuantum();
        }
        return finishRendering();
    }

    /**
     * Ends the rendering: the context is closed and the rendered audio is returned as a buffer of the length of the context.
     * <p>
     * 렌더링을 끝낸다: 컨텍스트는 닫히고 렌더링한 오디오가 컨텍스트 길이의 버퍼로 반환된다.
     *
     * @return the rendered audio / 렌더링한 오디오
     * @throws AudioException <code>InvalidStateError</code> if the rendering is not complete / 렌더링이 끝나지 않았으면 <code>InvalidStateError</code>
     */
    public AudioBuffer finishRendering() {
        if (!isComplete()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the rendering is not complete");
        }
        AudioBuffer result = new AudioBuffer(rendered.length, length, getSampleRate());
        // 채널마다 길이만큼 잘라 결과 버퍼에 옮긴다.
        for (int c = 0; c < rendered.length; c++) {
            System.arraycopy(rendered[c], 0, result.getChannelData(c), 0, length);
        }
        setState(ContextState.CLOSED);
        return result;
    }
}
