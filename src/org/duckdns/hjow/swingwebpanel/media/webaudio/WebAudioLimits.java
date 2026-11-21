package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The limits and fixed sizes of the Web Audio engine (plan item P25-02). Every limit has a name so that the exception for a value outside
 * it can say which limit it was, and so that a test can read the value. The limits protect the memory of the host application: a page
 * cannot ask for more than these sizes.
 * <p>
 * Web Audio 엔진의 제한과 고정 크기(계획 P25-02). 모든 제한에 이름이 있어 범위를 벗어난 값의 예외가 어느 제한인지 말할 수 있고 시험이 값을 읽을 수
 * 있다. 제한은 호스트 애플리케이션의 메모리를 지킨다: 페이지는 이 크기를 넘게 요청할 수 없다.
 */
public final class WebAudioLimits {

    /**
     * The default number of sample frames of a render quantum (Web Audio API 2.6).
     * <p>
     * render quantum 의 기본 샘플 프레임 수(Web Audio API 2.6).
     */
    public static final int RENDER_QUANTUM_SIZE = 128;

    /**
     * The largest number of channels of a bus, a buffer, a splitter or a merger (the specification requires at least 32).
     * <p>
     * 버스, 버퍼, 스플리터, 머저의 채널 수 상한(규격은 최소 32 를 요구한다).
     */
    public static final int MAX_CHANNELS = 32;

    /**
     * The lowest supported sample rate in Hz (Web Audio API 2.4).
     * <p>
     * 지원하는 가장 낮은 샘플링 주파수(Hz, Web Audio API 2.4).
     */
    public static final float MIN_SAMPLE_RATE = 3000f;

    /**
     * The highest supported sample rate in Hz (Web Audio API 2.4).
     * <p>
     * 지원하는 가장 높은 샘플링 주파수(Hz, Web Audio API 2.4).
     */
    public static final float MAX_SAMPLE_RATE = 768000f;

    /**
     * The largest number of float samples (all channels together) of one buffer, delay line or other allocation of the engine:
     * 32 Mi samples, which is 128 MiB.
     * <p>
     * 버퍼 하나, 지연선 하나, 그 밖의 엔진 할당 하나의 float 샘플 수 상한(모든 채널 합계): 32 Mi 샘플, 즉 128 MiB.
     */
    public static final int MAX_BUFFER_SAMPLES = 1 << 25;

    /**
     * The largest number of nodes that one context processes at the same time (nodes that are started, active, within their tail time or
     * reachable from them); nodes beyond the limit are muted.
     * <p>
     * 컨텍스트 하나가 동시에 처리하는 노드 수 상한(시작된 노드, 활성 노드, 꼬리 시간 안의 노드, 그로부터 도달 가능한 노드). 상한을 넘는 노드는 무음이 된다.
     */
    public static final int MAX_ACTIVE_NODES = 20000;

    /**
     * The largest number of automation events that one parameter keeps (events that lie in the past are folded into the base value).
     * <p>
     * 파라미터 하나가 가지는 자동화 이벤트 수 상한(과거의 이벤트는 기준 값으로 접힌다).
     */
    public static final int MAX_AUTOMATION_EVENTS = 65536;

    /**
     * The largest delay time in seconds (exclusive): <code>createDelay</code> requires less than three minutes.
     * <p>
     * 지연 시간의 상한(초, 미포함): <code>createDelay</code> 는 3분 미만을 요구한다.
     */
    public static final double MAX_DELAY_SECONDS = 180.0;

    /**
     * The largest number of coefficients of the feedforward or the feedback array of an IIR filter (Web Audio API 1.1.2).
     * <p>
     * IIR 필터의 피드포워드 또는 피드백 배열의 계수 수 상한(Web Audio API 1.1.2).
     */
    public static final int MAX_IIR_COEFFICIENTS = 20;

    /**
     * The largest number of terms of a periodic wave (the specification requires at least 8192).
     * <p>
     * 주기파의 항 수 상한(규격은 최소 8192 를 요구한다).
     */
    public static final int MAX_PERIODIC_WAVE_LENGTH = 16384;

    /**
     * The longest tail time in seconds that the engine estimates for a filter that does not decay (an unstable or resonant one).
     * <p>
     * 감쇠하지 않는 필터(불안정하거나 공진이 큰 필터)에 대해 엔진이 추정하는 가장 긴 꼬리 시간(초).
     */
    public static final double MAX_TAIL_SECONDS = 30.0;

    /**
     * The magnitude under which a sample counts as silent when the engine decides that an in-cycle delay is no longer active: the
     * smallest normal float, <code>2^-126</code> (Web Audio API 1.5.3).
     * <p>
     * 순환 안의 지연 노드가 더 이상 활성이 아니라고 판단할 때 샘플을 무음으로 보는 크기: 가장 작은 정규 float, <code>2^-126</code>
     * (Web Audio API 1.5.3).
     */
    public static final float SILENCE_THRESHOLD = Float.MIN_NORMAL;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private WebAudioLimits() {
    }
}
