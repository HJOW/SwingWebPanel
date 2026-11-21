package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * A set of audio nodes and their connections, and the loop that renders them (Web Audio API 1.1 and 2): the base of the real-time and the
 * offline context. A context has a sample rate, a destination node, the current time and the rendering order of its nodes. The class
 * implements the processing model of section 2:
 * <ul>
 * <li>Control methods (those of the nodes and the parameters) may be called from any thread. They check their arguments at once and
 * queue a <em>control message</em>; {@link #renderQuantum()} applies the messages in the order of their insertion at the start of a
 * render quantum.</li>
 * <li>{@link #renderQuantum()} renders one render quantum (128 frames by default): it applies the messages, orders the nodes when the
 * graph has changed (see {@link RenderGraph}), and renders every node in that order. It is called by one thread at a time, the
 * <em>rendering thread</em>; the offline context calls it from the thread that asks for the rendering.</li>
 * <li>The context keeps a node alive (<em>pinned</em>) while it is started and has not ended, or while it is active or within its tail
 * time, and processes the nodes that can be reached from the pinned nodes and from the destination. A node that is neither connected to
 * a started source nor started itself is not processed, so the value of its parameters does not advance with time.</li>
 * </ul>
 * <p>
 * 오디오 노드의 집합과 그 연결, 그리고 그것을 렌더링하는 루프(Web Audio API 1.1과 2절): 실시간 컨텍스트와 오프라인 컨텍스트의 기반. 컨텍스트는
 * 샘플링 주파수, 목적지 노드, 현재 시간, 노드의 렌더링 순서를 가진다. 이 클래스는 2절의 처리 모델을 구현한다:
 * <ul>
 * <li>제어 메서드(노드와 파라미터의 메서드)는 어느 스레드에서나 부를 수 있다. 인수를 즉시 검사하고 <em>제어 메시지</em>를 쌓으며,
 * {@link #renderQuantum()} 이 render quantum 시작에 메시지를 넣은 순서대로 적용한다.</li>
 * <li>{@link #renderQuantum()} 은 render quantum 하나(기본 128 프레임)를 렌더링한다: 메시지를 적용하고, 그래프가 바뀌었으면 노드의 순서를 정하고
 * ({@link RenderGraph} 참고), 그 순서대로 모든 노드를 렌더링한다. 한 번에 한 스레드(<em>렌더링 스레드</em>)가 부르며 오프라인 컨텍스트는 렌더링을 요청한
 * 스레드에서 부른다.</li>
 * <li>컨텍스트는 노드가 시작되어 끝나지 않았거나 활성이거나 꼬리 시간 안에 있는 동안 노드를 붙잡아 두며(<em>pinned</em>), 붙잡은 노드와 목적지에서 도달할 수
 * 있는 노드를 처리한다. 시작한 원천에도 연결되지 않고 스스로도 시작하지 않은 노드는 처리하지 않으므로 그 파라미터의 값은 시간에 따라 변하지 않는다.</li>
 * </ul>
 */
public abstract class BaseAudioContext {

    /**
     * The sink of the tasks that the rendering thread posts to the control side (the <code>ended</code> event of a source, for example).
     * The script layer implements it to queue the task on the event loop of the page; without a sink the task runs on the rendering thread.
     * <p>
     * 렌더링 스레드가 제어 쪽으로 보내는 작업(예: 원천의 <code>ended</code> 이벤트)의 싱크. 스크립트 계층이 이를 구현해 페이지의 이벤트 루프에 작업을
     * 쌓는다. 싱크가 없으면 작업은 렌더링 스레드에서 실행된다.
     */
    public interface TaskSink {

        /**
         * Queues a task on the control side.
         * <p>
         * 제어 쪽에 작업을 쌓는다.
         *
         * @param task the task / 작업
         */
        void post(Runnable task);
    }

    /**
     * The lock that guards the links of the nodes (the control side of the connections).
     * <p>
     * 노드의 링크(연결의 제어 쪽)를 지키는 잠금.
     */
    final Object controlLock = new Object();

    /**
     * The sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    private final float sampleRate;

    /**
     * The number of sample frames of a render quantum.
     * <p>
     * render quantum 의 샘플 프레임 수.
     */
    private final int renderQuantumSize;

    /**
     * The index of the first frame of the next quantum; <code>currentTime</code> is this divided by the sample rate.
     * <p>
     * 다음 quantum 의 첫 프레임 인덱스. <code>currentTime</code> 은 이를 샘플링 주파수로 나눈 값이다.
     */
    private volatile long currentFrame;

    /**
     * The state of the context.
     * <p>
     * 컨텍스트의 상태.
     */
    private volatile ContextState state = ContextState.SUSPENDED;

    /**
     * The processing latency of the destination in seconds.
     * <p>
     * 목적지의 처리 지연(초).
     */
    private volatile double baseLatency;

    /**
     * The estimated latency of the output device in seconds.
     * <p>
     * 출력 장치의 추정 지연(초).
     */
    private volatile double outputLatency;

    /**
     * The destination node.
     * <p>
     * 목적지 노드.
     */
    private final AudioDestinationNode destination;

    /**
     * The sink of the tasks for the control side.
     * <p>
     * 제어 쪽 작업의 싱크.
     */
    private volatile TaskSink taskSink;

    /**
     * Guards the control message queue.
     * <p>
     * 제어 메시지 큐를 지킨다.
     */
    private final Object queueLock = new Object();

    /**
     * The control messages that wait for the next quantum.
     * <p>
     * 다음 quantum 을 기다리는 제어 메시지.
     */
    private ArrayList<Runnable> queue = new ArrayList<Runnable>();

    /**
     * The empty list that is swapped with the queue.
     * <p>
     * 큐와 맞바꾸는 빈 목록.
     */
    private ArrayList<Runnable> spare = new ArrayList<Runnable>();

    /**
     * Serializes the rendering.
     * <p>
     * 렌더링을 직렬화한다.
     */
    private final Object renderLock = new Object();

    /**
     * The render serial: the number of quanta that were started (rendering thread).
     * <p>
     * 렌더링 일련번호: 시작한 quantum 의 수(렌더링 스레드).
     */
    private long serial;

    /**
     * The nodes that the context holds on to (rendering thread).
     * <p>
     * 컨텍스트가 붙잡고 있는 노드(렌더링 스레드).
     */
    private final LinkedHashSet<AudioNode> pinned = new LinkedHashSet<AudioNode>();

    /**
     * True if the connections or the pinned nodes changed and the rendering order must be computed again (rendering thread).
     * <p>
     * 연결이나 붙잡은 노드가 바뀌어 렌더링 순서를 다시 계산해야 하면 true(렌더링 스레드).
     */
    private boolean dirty = true;

    /**
     * The nodes in rendering order (rendering thread).
     * <p>
     * 렌더링 순서의 노드(렌더링 스레드).
     */
    private AudioNode[] steps = new AudioNode[0];

    /**
     * The phase of every step (rendering thread).
     * <p>
     * 단계마다의 종류(렌더링 스레드).
     */
    private int[] phases = new int[0];

    /**
     * The number of muted nodes of the last rendering order.
     * <p>
     * 마지막 렌더링 순서에서 무음 처리된 노드 수.
     */
    private volatile int mutedNodes;

    /**
     * The parameters that belong to the context and not to a node (the position and the orientation of the listener); the rendering
     * computes them before the nodes in every quantum.
     * <p>
     * 노드가 아니라 컨텍스트가 가진 파라미터(리스너의 위치와 방향). 렌더링은 quantum 마다 노드보다 먼저 이것을 계산한다.
     */
    private final ArrayList<AudioParam> contextParams = new ArrayList<AudioParam>();

    /**
     * The shared basic waves (created when an oscillator needs one).
     * <p>
     * 공유하는 기본 파형(발진기가 필요로 할 때 만든다).
     */
    private final PeriodicWave[] basicWaves = new PeriodicWave[OscillatorType.values().length];

    /**
     * Creates a context.
     * <p>
     * 컨텍스트를 만든다.
     *
     * @param sampleRate        the sample rate in Hz, from 3000 to 768000 / 샘플링 주파수(Hz, 3000 ~ 768000)
     * @param renderQuantumSize the number of frames of a render quantum, from 1 to 6 times the sample rate / render quantum 의 프레임 수(1 ~ 샘플링 주파수의 6배)
     * @param destinationChannels the initial number of channels of the destination / 목적지의 처음 채널 수
     * @param maxChannelCount   the largest number of channels of the destination / 목적지의 최대 채널 수
     * @param offline           true for an offline context / 오프라인 컨텍스트이면 true
     * @throws AudioException <code>NotSupportedError</code> if the sample rate or the quantum size is out of range / 샘플링 주파수나 quantum 크기가 범위 밖이면
     *                        <code>NotSupportedError</code>
     */
    BaseAudioContext(float sampleRate, int renderQuantumSize, int destinationChannels, int maxChannelCount, boolean offline) {
        AudioBuffer.checkSampleRate(sampleRate);
        if (renderQuantumSize < 1 || renderQuantumSize > (int) Math.floor(6.0 * sampleRate)) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "renderQuantumSize " + renderQuantumSize + " is out of range");
        }
        this.sampleRate = sampleRate;
        this.renderQuantumSize = renderQuantumSize;
        this.destination = new AudioDestinationNode(this, destinationChannels, maxChannelCount, offline);
    }

    /**
     * Returns the sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz)를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    public final float getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the number of sample frames of a render quantum.
     * <p>
     * render quantum 의 샘플 프레임 수를 반환한다.
     *
     * @return the quantum size / quantum 크기
     */
    public final int getRenderQuantumSize() {
        return renderQuantumSize;
    }

    /**
     * Returns the index of the first frame of the next quantum, that is the number of frames rendered so far.
     * <p>
     * 다음 quantum 의 첫 프레임 인덱스, 즉 지금까지 렌더링한 프레임 수를 반환한다.
     *
     * @return the current frame / 현재 프레임
     */
    public final long getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Returns the time in seconds of the sample frame that follows the last frame of the last rendered quantum.
     * <p>
     * 마지막으로 렌더링한 quantum 의 마지막 프레임 다음 샘플 프레임의 시간(초)을 반환한다.
     *
     * @return the current time / 현재 시간
     */
    public final double getCurrentTime() {
        return currentFrame / (double) sampleRate;
    }

    /**
     * Returns the destination node.
     * <p>
     * 목적지 노드를 반환한다.
     *
     * @return the destination / 목적지
     */
    public final AudioDestinationNode getDestination() {
        return destination;
    }

    /**
     * Returns the state of the context.
     * <p>
     * 컨텍스트의 상태를 반환한다.
     *
     * @return the state / 상태
     */
    public final ContextState getState() {
        return state;
    }

    /**
     * Sets the state of the context (for the subclasses).
     * <p>
     * 컨텍스트의 상태를 정한다(하위 클래스용).
     *
     * @param newState the state / 상태
     */
    final void setState(ContextState newState) {
        state = newState;
    }

    /**
     * Returns the processing latency of the destination in seconds.
     * <p>
     * 목적지의 처리 지연(초)을 반환한다.
     *
     * @return the base latency / 기본 지연
     */
    public final double getBaseLatency() {
        return baseLatency;
    }

    /**
     * Returns the estimated output latency in seconds.
     * <p>
     * 추정 출력 지연(초)을 반환한다.
     *
     * @return the output latency / 출력 지연
     */
    public final double getOutputLatency() {
        return outputLatency;
    }

    /**
     * Sets the latencies (for the subclasses that output to a device).
     * <p>
     * 지연을 정한다(장치로 출력하는 하위 클래스용).
     *
     * @param base   the processing latency in seconds / 처리 지연(초)
     * @param output the output latency in seconds / 출력 지연(초)
     */
    final void setLatencies(double base, double output) {
        baseLatency = base;
        outputLatency = output;
    }

    /**
     * Returns the number of nodes that were muted by the last rendering order because they are part of a cycle without a delay.
     * <p>
     * 지연 없는 순환의 일부여서 마지막 렌더링 순서에서 무음 처리된 노드 수를 반환한다.
     *
     * @return the number of muted nodes / 무음 처리된 노드 수
     */
    public final int getMutedNodeCount() {
        return mutedNodes;
    }

    /**
     * Returns the number of steps of the last rendering order (a delay node of a cycle counts twice); for tests.
     * <p>
     * 마지막 렌더링 순서의 단계 수를 반환한다(순환의 지연 노드는 두 번 센다). 시험용.
     *
     * @return the number of steps / 단계 수
     */
    public final int getProcessedStepCount() {
        synchronized (renderLock) {
            return steps.length;
        }
    }

    /**
     * Sets the sink of the tasks for the control side.
     * <p>
     * 제어 쪽 작업의 싱크를 정한다.
     *
     * @param sink the sink, or null to run the tasks on the rendering thread / 싱크. 렌더링 스레드에서 실행하려면 null
     */
    public final void setTaskSink(TaskSink sink) {
        this.taskSink = sink;
    }

    /**
     * Posts a task to the control side.
     * <p>
     * 제어 쪽에 작업을 보낸다.
     *
     * @param task the task / 작업
     */
    final void postTask(Runnable task) {
        TaskSink s = taskSink;
        if (s != null) {
            s.post(task);
        } else {
            task.run();
        }
    }

    /**
     * Queues a control message that the rendering thread runs at the start of the next quantum.
     * <p>
     * 렌더링 스레드가 다음 quantum 시작에 실행할 제어 메시지를 쌓는다.
     *
     * @param message the message / 메시지
     */
    final void post(Runnable message) {
        synchronized (queueLock) {
            queue.add(message);
        }
    }

    /**
     * Marks the rendering order as out of date (rendering thread).
     * <p>
     * 렌더링 순서가 낡았다고 표시한다(렌더링 스레드).
     */
    final void markGraphDirty() {
        dirty = true;
    }

    /**
     * Returns the render serial of the quantum that is being rendered (rendering thread).
     * <p>
     * 렌더링 중인 quantum 의 렌더링 일련번호를 반환한다(렌더링 스레드).
     *
     * @return the serial / 일련번호
     */
    final long getRenderSerial() {
        return serial;
    }

    /**
     * Keeps a node alive and processed until it is unpinned (rendering thread).
     * <p>
     * 노드를 붙잡을 때까지 살려 두고 처리한다(렌더링 스레드).
     *
     * @param node the node / 노드
     */
    final void pin(AudioNode node) {
        if (pinned.add(node)) {
            dirty = true;
        }
    }

    /**
     * Releases a node (rendering thread).
     * <p>
     * 노드를 놓아 준다(렌더링 스레드).
     *
     * @param node the node / 노드
     */
    final void unpin(AudioNode node) {
        if (pinned.remove(node)) {
            dirty = true;
        }
    }

    /**
     * Pins a node that has a tail time while it is active and releases it when it is not (rendering thread).
     * <p>
     * 꼬리 시간이 있는 노드를 활성인 동안 붙잡고 활성이 아니면 놓아 준다(렌더링 스레드).
     *
     * @param node   the node / 노드
     * @param active true if the node was active in the quantum / 노드가 quantum 에서 활성이었으면 true
     */
    final void updatePin(AudioNode node, boolean active) {
        if (node.isSource() || node.tailFrames() <= 0) {
            return;
        }
        if (active) {
            pin(node);
        } else {
            unpin(node);
        }
    }

    /**
     * Returns the number of nodes that the context holds on to (for tests).
     * <p>
     * 컨텍스트가 붙잡고 있는 노드 수를 반환한다(시험용).
     *
     * @return the number of pinned nodes / 붙잡은 노드 수
     */
    public final int getPinnedNodeCount() {
        synchronized (renderLock) {
            return pinned.size();
        }
    }

    /**
     * Registers a parameter that belongs to the context (called while the context is built, before the first render).
     * <p>
     * 컨텍스트가 가진 파라미터를 등록한다(컨텍스트를 만드는 동안, 첫 렌더링 전에 부른다).
     *
     * @param p the parameter / 파라미터
     * @return the same parameter / 같은 파라미터
     */
    final AudioParam registerContextParam(AudioParam p) {
        contextParams.add(p);
        return p;
    }

    /**
     * Returns the shared periodic wave of a basic oscillator type.
     * <p>
     * 기본 발진기 종류의 공유 주기파를 반환한다.
     *
     * @param type the type, not custom / 종류(custom 이 아님)
     * @return the wave / 파형
     */
    final PeriodicWave getBasicWave(OscillatorType type) {
        synchronized (basicWaves) {
            PeriodicWave w = basicWaves[type.ordinal()];
            if (w == null) {
                w = PeriodicWave.basic(type, sampleRate);
                basicWaves[type.ordinal()] = w;
            }
            return w;
        }
    }

    /**
     * Renders one render quantum: applies the control messages, orders the nodes if the graph changed, renders every node, and moves
     * the current time forward by one quantum. Only one thread at a time may render.
     * <p>
     * render quantum 하나를 렌더링한다: 제어 메시지를 적용하고, 그래프가 바뀌었으면 노드의 순서를 정하고, 모든 노드를 렌더링하고, 현재 시간을 한 quantum
     * 앞으로 보낸다. 한 번에 한 스레드만 렌더링할 수 있다.
     */
    final void renderQuantum() {
        synchronized (renderLock) {
            ArrayList<Runnable> batch;
            synchronized (queueLock) {
                batch = queue;
                queue = spare;
                spare = batch;
            }
            // 쌓인 제어 메시지를 쌓은 순서대로 적용한다.
            for (int i = 0; i < batch.size(); i++) {
                batch.get(i).run();
            }
            batch.clear();
            if (dirty) {
                rebuild();
            }
            serial++;
            long frame0 = currentFrame;
            // 컨텍스트가 가진 파라미터(리스너의 위치와 방향 등)를 노드보다 먼저 계산한다.
            for (int i = 0; i < contextParams.size(); i++) {
                contextParams.get(i).computeValues(frame0, renderQuantumSize);
            }
            // 렌더링 순서대로 노드를 렌더링한다.
            for (int s = 0; s < steps.length; s++) {
                steps[s].renderStep(frame0, renderQuantumSize, serial, phases[s]);
            }
            currentFrame = frame0 + renderQuantumSize;
        }
    }

    /**
     * Collects the nodes that are processed (the destination, the pinned nodes and everything reachable from them through the
     * connections) and computes their rendering order.
     * <p>
     * 처리할 노드(목적지, 붙잡은 노드, 연결로 그로부터 도달할 수 있는 모든 노드)를 모으고 렌더링 순서를 계산한다.
     */
    private void rebuild() {
        dirty = false;
        ArrayList<AudioNode> list = new ArrayList<AudioNode>();
        HashSet<AudioNode> seen = new HashSet<AudioNode>();
        add(destination, list, seen);
        // 붙잡은 노드도 처리 대상에 넣는다.
        for (AudioNode n : pinned) {
            add(n, list, seen);
        }
        // 목적지와 붙잡은 노드에서 연결을 따라 앞으로 도달할 수 있는 노드를 넓이 우선으로 모은다.
        for (int i = 0; i < list.size() && list.size() <= WebAudioLimits.MAX_ACTIVE_NODES; i++) {
            AudioNode n = list.get(i);
            // 노드의 출력마다 연결된 대상을 살핀다.
            for (int o = 0; o < n.outputs.length; o++) {
                AudioNodeOutput out = n.outputs[o];
                // 이 출력을 읽는 입력의 노드를 더한다.
                for (int k = 0; k < out.inputTargets.size(); k++) {
                    add(out.inputTargets.get(k).node, list, seen);
                }
                // 이 출력이 구동하는 파라미터를 가진 노드를 더한다.
                for (int k = 0; k < out.paramTargets.size(); k++) {
                    AudioNode owner = out.paramTargets.get(k).getOwner();
                    if (owner != null) {
                        add(owner, list, seen);
                    }
                }
            }
        }
        // 상한을 넘는 노드는 처리하지 않는다(무음).
        while (list.size() > WebAudioLimits.MAX_ACTIVE_NODES) {
            list.remove(list.size() - 1);
        }
        RenderGraph.Plan plan = RenderGraph.order(list);
        AudioNode[] previous = steps;
        steps = plan.nodes;
        phases = plan.phases;
        mutedNodes = plan.mutedCount;
        HashSet<AudioNode> now = new HashSet<AudioNode>(java.util.Arrays.asList(steps));
        // 이제 처리하지 않는 노드는 출력을 무음 채널 하나로 되돌리고 내부 상태를 지운다.
        for (int i = 0; i < previous.length; i++) {
            if (!now.contains(previous[i])) {
                previous[i].drop();
            }
        }
    }

    /**
     * Adds a node to the list once.
     * <p>
     * 노드를 목록에 한 번만 더한다.
     *
     * @param n    the node / 노드
     * @param list the list / 목록
     * @param seen the nodes in the list / 목록에 있는 노드
     */
    private static void add(AudioNode n, ArrayList<AudioNode> list, HashSet<AudioNode> seen) {
        if (seen.add(n)) {
            list.add(n);
        }
    }

    /**
     * Creates a gain node.
     * <p>
     * 게인 노드를 만든다.
     *
     * @return the node / 노드
     */
    public final GainNode createGain() {
        return new GainNode(this);
    }

    /**
     * Creates an oscillator.
     * <p>
     * 발진기를 만든다.
     *
     * @return the node / 노드
     */
    public final OscillatorNode createOscillator() {
        return new OscillatorNode(this);
    }

    /**
     * Creates a buffer source.
     * <p>
     * 버퍼 원천을 만든다.
     *
     * @return the node / 노드
     */
    public final AudioBufferSourceNode createBufferSource() {
        return new AudioBufferSourceNode(this);
    }

    /**
     * Creates a constant source.
     * <p>
     * 상수 원천을 만든다.
     *
     * @return the node / 노드
     */
    public final ConstantSourceNode createConstantSource() {
        return new ConstantSourceNode(this);
    }

    /**
     * Creates a delay node.
     * <p>
     * 지연 노드를 만든다.
     *
     * @param maxDelayTime the largest delay in seconds / 가장 긴 지연(초)
     * @return the node / 노드
     */
    public final DelayNode createDelay(double maxDelayTime) {
        return new DelayNode(this, maxDelayTime);
    }

    /**
     * Creates a delay node with a largest delay of 1 second.
     * <p>
     * 가장 긴 지연이 1초인 지연 노드를 만든다.
     *
     * @return the node / 노드
     */
    public final DelayNode createDelay() {
        return new DelayNode(this, 1.0);
    }

    /**
     * Creates a biquad filter.
     * <p>
     * 쌍이차 필터를 만든다.
     *
     * @return the node / 노드
     */
    public final BiquadFilterNode createBiquadFilter() {
        return new BiquadFilterNode(this);
    }

    /**
     * Creates an IIR filter.
     * <p>
     * IIR 필터를 만든다.
     *
     * @param feedforward the numerator coefficients / 분자 계수
     * @param feedback    the denominator coefficients / 분모 계수
     * @return the node / 노드
     */
    public final IIRFilterNode createIIRFilter(double[] feedforward, double[] feedback) {
        return new IIRFilterNode(this, feedforward, feedback);
    }

    /**
     * Creates a stereo panner.
     * <p>
     * 스테레오 패너를 만든다.
     *
     * @return the node / 노드
     */
    public final StereoPannerNode createStereoPanner() {
        return new StereoPannerNode(this);
    }

    /**
     * Creates a channel splitter.
     * <p>
     * 채널 스플리터를 만든다.
     *
     * @param numberOfOutputs the number of outputs / 출력 수
     * @return the node / 노드
     */
    public final ChannelSplitterNode createChannelSplitter(int numberOfOutputs) {
        return new ChannelSplitterNode(this, numberOfOutputs);
    }

    /**
     * Creates a channel merger.
     * <p>
     * 채널 머저를 만든다.
     *
     * @param numberOfInputs the number of inputs / 입력 수
     * @return the node / 노드
     */
    public final ChannelMergerNode createChannelMerger(int numberOfInputs) {
        return new ChannelMergerNode(this, numberOfInputs);
    }

    /**
     * Creates a wave shaper (<code>createWaveShaper</code>).
     * <p>
     * 웨이브셰이퍼를 만든다(<code>createWaveShaper</code>).
     *
     * @return the node / 노드
     */
    public final WaveShaperNode createWaveShaper() {
        return new WaveShaperNode(this);
    }

    /**
     * Creates a convolver (<code>createConvolver</code>).
     * <p>
     * 컨볼버를 만든다(<code>createConvolver</code>).
     *
     * @return the node / 노드
     */
    public final ConvolverNode createConvolver() {
        return new ConvolverNode(this);
    }

    /**
     * Creates a silent buffer.
     * <p>
     * 무음 버퍼를 만든다.
     *
     * @param numberOfChannels the number of channels / 채널 수
     * @param length           the number of frames / 프레임 수
     * @param bufferSampleRate the sample rate of the buffer / 버퍼의 샘플링 주파수
     * @return the buffer / 버퍼
     */
    public final AudioBuffer createBuffer(int numberOfChannels, int length, float bufferSampleRate) {
        return new AudioBuffer(numberOfChannels, length, bufferSampleRate);
    }

    /**
     * Creates a periodic wave from Fourier coefficients (<code>createPeriodicWave</code>). If only one of the two arrays is given, the
     * other is taken as zeros of the same length; if none is given, the wave is a sine.
     * <p>
     * 푸리에 계수로 주기파를 만든다(<code>createPeriodicWave</code>). 두 배열 중 하나만 주면 다른 하나는 같은 길이의 0 으로 보고, 둘 다 없으면 사인파이다.
     *
     * @param real                 the cosine terms, or null / 코사인 항. 없으면 null
     * @param imag                 the sine terms, or null / 사인 항. 없으면 null
     * @param disableNormalization true to keep the amplitude of the coefficients / 계수의 진폭을 그대로 두려면 true
     * @return the wave / 파형
     */
    public final PeriodicWave createPeriodicWave(float[] real, float[] imag, boolean disableNormalization) {
        float[] r = real;
        float[] i = imag;
        if (r == null && i == null) {
            r = new float[] {0f, 0f};
            i = new float[] {0f, 1f};
        } else if (r == null) {
            r = new float[i.length];
        } else if (i == null) {
            i = new float[r.length];
        }
        return new PeriodicWave(sampleRate, r, i, disableNormalization);
    }
}
