package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

/**
 * The limits of the audio worklet. The script of a worklet runs on the rendering thread of an audio context, so its time is scarce: a render quantum of 128 frames is about 2.7 ms
 * at 48 kHz, and the interpreter needs about a quarter of a millisecond for a simple processor (measured: 0.2 to 0.3 ms for a loop over the 128 frames). The limits below keep a
 * runaway script from holding the rendering thread, and keep a real-time context responsive when the worklet is busy.
 * <p>
 * 오디오 worklet 의 한도. worklet 의 스크립트는 오디오 컨텍스트의 렌더링 스레드에서 실행되므로 시간이 귀하다: 128 프레임의 render quantum 은 48 kHz 에서 약 2.7 ms 이며, 인터프리터는 단순한
 * 프로세서 하나에 약 0.25 ms 가 든다(측정: 128 프레임을 도는 반복문 하나에 0.2 ~ 0.3 ms). 아래 한도는 폭주하는 스크립트가 렌더링 스레드를 붙잡지 못하게 하고, worklet 이 바쁠 때도 실시간
 * 컨텍스트가 응답하게 한다.
 */
public final class WorkletLimits {

    /**
     * The step limit of one call of <code>process()</code> (the engine counts statements, loop iterations and calls). A processor that exceeds it fails like one that throws
     * (the <code>processorerror</code> event, silence for good). A processor that loops over 128 frames with a few statements needs a few thousand steps.
     * <p>
     * <code>process()</code> 호출 하나의 단계 수 한도(엔진은 문장, 반복 한 번, 호출을 센다). 넘으면 예외를 던진 것처럼 실패한다(<code>processorerror</code> 이벤트, 영구 무음). 128 프레임을 몇 문장으로 도는
     * 프로세서는 수천 단계가 든다.
     */
    public static final long PROCESS_STEP_LIMIT = 4000000L;

    /**
     * The time limit of one call of <code>process()</code> in a real-time context, in milliseconds: a call that takes longer fails like one that throws. It is about
     * 37 render quanta at 48 kHz; a processor that needs this long cannot be played anyway.
     * <p>
     * 실시간 컨텍스트에서 <code>process()</code> 호출 하나의 시간 한도(밀리초): 더 오래 걸리는 호출은 예외를 던진 것처럼 실패한다. 48 kHz 에서 render quantum 약 37 개이며, 이만큼 걸리는
     * 프로세서는 어차피 재생할 수 없다.
     */
    public static final long REALTIME_PROCESS_TIME_LIMIT_MILLIS = 100L;

    /**
     * How long the rendering thread of a real-time context waits for the script realm when the worklet thread is busy (a module is being evaluated, a message handler runs), in
     * milliseconds. If the realm is still busy, the node outputs silence for that quantum ({@link org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode#SKIPPED}) and
     * every other node of the same quantum gives up at once, so that the rendering thread loses this time once per quantum and not once per node.
     * <p>
     * worklet 스레드가 바쁠 때(모듈을 평가하거나 메시지 핸들러가 실행 중일 때) 실시간 컨텍스트의 렌더링 스레드가 스크립트 realm 을 기다리는 시간(밀리초). 그래도 realm 이 바쁘면 노드는 그
     * quantum 의 출력을 무음으로 하고({@link org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode#SKIPPED}) 같은 quantum 의 다른 노드는 곧바로 포기하므로, 렌더링 스레드가 이
     * 시간을 노드마다가 아니라 quantum 마다 한 번만 잃는다.
     */
    public static final long REALTIME_LOCK_WAIT_MILLIS = 2L;

    /**
     * How long the control message that creates a processor waits for the script realm in a real-time context, in milliseconds; if the realm is still busy the construction is
     * tried again at the next quantum.
     * <p>
     * 실시간 컨텍스트에서 프로세서를 만드는 제어 메시지가 스크립트 realm 을 기다리는 시간(밀리초). 그래도 바쁘면 다음 quantum 에 다시 시도한다.
     */
    public static final long REALTIME_CONSTRUCT_WAIT_MILLIS = 5L;

    /**
     * The largest number of tasks (messages, module loads, constructions) that wait for the worklet thread; further tasks are dropped. A page that floods the processor port
     * cannot grow the queue without bound.
     * <p>
     * worklet 스레드를 기다릴 수 있는 작업(메시지, 모듈 로드, 생성)의 최대 수. 더 오는 작업은 버린다. 프로세서 포트를 범람시키는 페이지가 큐를 끝없이 키울 수 없다.
     */
    public static final int MAX_PENDING_TASKS = 10000;

    /**
     * The largest number of modules (source texts) that one <code>addModule()</code> loads (the module and what it imports, directly or not); a larger graph is refused.
     * <p>
     * <code>addModule()</code> 한 번이 불러오는 모듈(소스 텍스트)의 최대 수(모듈과 그것이 직간접으로 import 하는 것). 더 큰 그래프는 거부한다.
     */
    public static final int MAX_MODULES_PER_LOAD = 256;

    /**
     * The largest number of processor names that one worklet global scope can register.
     * <p>
     * worklet 전역 범위 하나가 등록할 수 있는 프로세서 이름의 최대 수.
     */
    public static final int MAX_PROCESSORS = 1024;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private WorkletLimits() {
    }
}
