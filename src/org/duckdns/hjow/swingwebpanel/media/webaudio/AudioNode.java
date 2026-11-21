package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;

/**
 * The base of all audio nodes (Web Audio API 1.5). A node has a number of inputs and outputs, the channel attributes
 * <code>channelCount</code>, <code>channelCountMode</code> and <code>channelInterpretation</code> that decide how the inputs are mixed, and
 * zero or more {@link AudioParam}s. The control methods (<code>connect</code>, <code>disconnect</code>, the channel attributes) may be
 * called from any thread: they validate at once, keep the connections of the node in a list for the later validation, and queue a control
 * message that the rendering thread applies at the next render quantum boundary. The rendering thread calls
 * {@link #renderStep(long, int, long, int)} once per quantum: the parameters are computed, the inputs are pulled and mixed, and the node
 * processes if it is actively processing (a source that plays, a node with an active input, or a node inside its tail time); otherwise its
 * outputs are a single channel of silence. The hierarchy is closed: the abstract hooks are package private.
 * <p>
 * 모든 오디오 노드의 기반(Web Audio API 1.5). 노드는 여러 입력과 출력, 입력을 섞는 방식을 정하는 채널 속성 <code>channelCount</code>,
 * <code>channelCountMode</code>, <code>channelInterpretation</code>, 그리고 0개 이상의 {@link AudioParam} 을 가진다. 제어 메서드(<code>connect</code>,
 * <code>disconnect</code>, 채널 속성)는 어느 스레드에서나 부를 수 있다: 즉시 검증하고, 이후 검증을 위해 노드의 연결을 목록에 두고, 렌더링 스레드가
 * 다음 render quantum 경계에서 적용하는 제어 메시지를 쌓는다. 렌더링 스레드는 quantum 마다 {@link #renderStep(long, int, long, int)} 를 한 번 부른다:
 * 파라미터를 계산하고, 입력을 끌어와 섞고, 노드가 활성(재생 중인 원천, 활성 입력이 있는 노드, 꼬리 시간 안의 노드)이면 처리하며, 아니면 출력은 무음
 * 채널 하나이다. 계층은 닫혀 있다: 추상 훅은 package-private 이다.
 */
public abstract class AudioNode {

    /**
     * Render phase: the whole node is processed at its place in the order.
     * <p>
     * 렌더링 단계: 노드 전체를 순서 안의 자리에서 처리한다.
     */
    static final int PHASE_FULL = 0;

    /**
     * Render phase: only the output side of a delay node that breaks a cycle (it reads the delay line).
     * <p>
     * 렌더링 단계: 순환을 끊는 지연 노드의 출력 쪽만(지연선을 읽는다).
     */
    static final int PHASE_READ = 1;

    /**
     * Render phase: only the input side of a delay node that breaks a cycle (it writes the delay line).
     * <p>
     * 렌더링 단계: 순환을 끊는 지연 노드의 입력 쪽만(지연선에 쓴다).
     */
    static final int PHASE_WRITE = 2;

    /**
     * A connection from an output of this node, for the validation of <code>connect</code> and <code>disconnect</code> on the control
     * thread.
     * <p>
     * 제어 스레드에서 <code>connect</code> 와 <code>disconnect</code> 를 검증하기 위한, 이 노드의 출력에서 나가는 연결.
     */
    private static final class Link {

        /**
         * The destination node, or null if the destination is a parameter.
         * <p>
         * 대상 노드. 대상이 파라미터이면 null.
         */
        final AudioNode dstNode;

        /**
         * The destination parameter, or null if the destination is a node.
         * <p>
         * 대상 파라미터. 대상이 노드이면 null.
         */
        final AudioParam dstParam;

        /**
         * The output index of the source.
         * <p>
         * 원본의 출력 인덱스.
         */
        final int output;

        /**
         * The input index of the destination node (0 for a parameter).
         * <p>
         * 대상 노드의 입력 인덱스(파라미터이면 0).
         */
        final int input;

        /**
         * Creates a link.
         * <p>
         * 링크를 만든다.
         *
         * @param dstNode  the destination node or null / 대상 노드 또는 null
         * @param dstParam the destination parameter or null / 대상 파라미터 또는 null
         * @param output   the output index / 출력 인덱스
         * @param input    the input index / 입력 인덱스
         */
        Link(AudioNode dstNode, AudioParam dstParam, int output, int input) {
            this.dstNode = dstNode;
            this.dstParam = dstParam;
            this.output = output;
            this.input = input;
        }
    }

    /**
     * The context that owns the node.
     * <p>
     * 노드를 가진 컨텍스트.
     */
    private final BaseAudioContext context;

    /**
     * The number of inputs.
     * <p>
     * 입력 수.
     */
    private final int numberOfInputs;

    /**
     * The number of outputs.
     * <p>
     * 출력 수.
     */
    private final int numberOfOutputs;

    /**
     * The <code>channelCount</code> attribute; the rendering thread reads it at every quantum.
     * <p>
     * <code>channelCount</code> 속성. 렌더링 스레드가 quantum 마다 읽는다.
     */
    private volatile int channelCount;

    /**
     * The <code>channelCountMode</code> attribute.
     * <p>
     * <code>channelCountMode</code> 속성.
     */
    private volatile ChannelCountMode channelCountMode;

    /**
     * The <code>channelInterpretation</code> attribute.
     * <p>
     * <code>channelInterpretation</code> 속성.
     */
    private volatile ChannelInterpretation channelInterpretation;

    /**
     * The inputs (rendering side).
     * <p>
     * 입력(렌더링 쪽).
     */
    final AudioNodeInput[] inputs;

    /**
     * The outputs (rendering side).
     * <p>
     * 출력(렌더링 쪽).
     */
    final AudioNodeOutput[] outputs;

    /**
     * The parameters of the node, in the order of registration.
     * <p>
     * 노드의 파라미터(등록 순서).
     */
    private final ArrayList<AudioParam> params = new ArrayList<AudioParam>();

    /**
     * The connections that leave this node (control side); guarded by the control lock of the context.
     * <p>
     * 이 노드에서 나가는 연결(제어 쪽). 컨텍스트의 제어 잠금으로 보호한다.
     */
    private final ArrayList<Link> links = new ArrayList<Link>();

    /**
     * The frame at which the last input that was active ended; the tail time counts from here.
     * <p>
     * 활성이던 마지막 입력이 끝난 프레임. 꼬리 시간은 여기서부터 센다.
     */
    long lastInputActiveEnd = Long.MIN_VALUE / 2;

    /**
     * True if the node was actively processing in the previous quantum.
     * <p>
     * 노드가 이전 quantum 에 활성으로 처리했으면 true.
     */
    boolean wasActive;

    /**
     * The index of the first frame of the quantum that is rendered (rendering thread).
     * <p>
     * 렌더링 중인 quantum 의 첫 프레임 인덱스(렌더링 스레드).
     */
    long renderFrame0;

    /**
     * True if the node is part of a cycle without a delay and is muted by the rendering order.
     * <p>
     * 노드가 지연 없는 순환의 일부여서 렌더링 순서에서 무음 처리되면 true.
     */
    boolean muted;

    /**
     * Creates a node.
     * <p>
     * 노드를 만든다.
     *
     * @param context        the context / 컨텍스트
     * @param numberOfInputs  the number of inputs / 입력 수
     * @param numberOfOutputs the number of outputs / 출력 수
     * @param channelCount    the default <code>channelCount</code> / <code>channelCount</code> 기본 값
     * @param mode            the default <code>channelCountMode</code> / <code>channelCountMode</code> 기본 값
     * @param interpretation  the default <code>channelInterpretation</code> / <code>channelInterpretation</code> 기본 값
     */
    AudioNode(BaseAudioContext context, int numberOfInputs, int numberOfOutputs, int channelCount, ChannelCountMode mode, ChannelInterpretation interpretation) {
        if (context == null) {
            throw new AudioException(AudioException.Kind.TYPE, "context is null");
        }
        this.context = context;
        this.numberOfInputs = numberOfInputs;
        this.numberOfOutputs = numberOfOutputs;
        this.channelCount = channelCount;
        this.channelCountMode = mode;
        this.channelInterpretation = interpretation;
        int q = context.getRenderQuantumSize();
        this.inputs = new AudioNodeInput[numberOfInputs];
        this.outputs = new AudioNodeOutput[numberOfOutputs];
        // 입력과 출력 객체를 미리 만들어 둔다.
        for (int i = 0; i < numberOfInputs; i++) {
            inputs[i] = new AudioNodeInput(this, i, q);
        }
        for (int i = 0; i < numberOfOutputs; i++) {
            outputs[i] = new AudioNodeOutput(this, i, q);
        }
    }

    /**
     * Registers a parameter so that the rendering loop computes it before the node processes.
     * <p>
     * 렌더링 루프가 노드를 처리하기 전에 파라미터를 계산하도록 등록한다.
     *
     * @param p the parameter / 파라미터
     * @return the same parameter / 같은 파라미터
     */
    final AudioParam registerParam(AudioParam p) {
        params.add(p);
        return p;
    }

    /**
     * Returns the parameters of the node for the rendering order (rendering thread).
     * <p>
     * 렌더링 순서를 정하기 위해 노드의 파라미터를 반환한다(렌더링 스레드).
     *
     * @return the live list of parameters / 파라미터의 살아 있는 목록
     */
    final ArrayList<AudioParam> paramsForGraph() {
        return params;
    }

    /**
     * Returns the context that owns this node.
     * <p>
     * 이 노드를 가진 컨텍스트를 반환한다.
     *
     * @return the context / 컨텍스트
     */
    public final BaseAudioContext getContext() {
        return context;
    }

    /**
     * Returns the number of inputs.
     * <p>
     * 입력 수를 반환한다.
     *
     * @return the number of inputs / 입력 수
     */
    public final int getNumberOfInputs() {
        return numberOfInputs;
    }

    /**
     * Returns the number of outputs.
     * <p>
     * 출력 수를 반환한다.
     *
     * @return the number of outputs / 출력 수
     */
    public final int getNumberOfOutputs() {
        return numberOfOutputs;
    }

    /**
     * Returns the <code>channelCount</code> attribute.
     * <p>
     * <code>channelCount</code> 속성을 반환한다.
     *
     * @return the number of channels used when mixing / 섞을 때 쓰는 채널 수
     */
    public final int getChannelCount() {
        return channelCount;
    }

    /**
     * Returns the <code>channelCountMode</code> attribute.
     * <p>
     * <code>channelCountMode</code> 속성을 반환한다.
     *
     * @return the mode / 모드
     */
    public final ChannelCountMode getChannelCountMode() {
        return channelCountMode;
    }

    /**
     * Returns the <code>channelInterpretation</code> attribute.
     * <p>
     * <code>channelInterpretation</code> 속성을 반환한다.
     *
     * @return the interpretation / 해석
     */
    public final ChannelInterpretation getChannelInterpretation() {
        return channelInterpretation;
    }

    /**
     * Sets the <code>channelCount</code> attribute; the rendering thread uses it from the next quantum.
     * <p>
     * <code>channelCount</code> 속성을 정한다. 렌더링 스레드는 다음 quantum 부터 쓴다.
     *
     * @param count the number of channels / 채널 수
     * @throws AudioException <code>NotSupportedError</code> for 0 or more than the supported channels, or the error of the node type / 0 이거나 지원 채널 수를
     *                        넘으면 <code>NotSupportedError</code>, 또는 노드 종류에 따른 오류
     */
    public final void setChannelCount(int count) {
        checkChannelCount(count);
        if (count < 1 || count > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "channelCount " + count + " is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        channelCount = count;
    }

    /**
     * Sets the <code>channelCountMode</code> attribute.
     * <p>
     * <code>channelCountMode</code> 속성을 정한다.
     *
     * @param mode the mode / 모드
     * @throws AudioException the error of the node type if the mode is not allowed / 노드 종류가 허용하지 않는 모드이면 그에 따른 오류
     */
    public final void setChannelCountMode(ChannelCountMode mode) {
        if (mode == null) {
            throw new AudioException(AudioException.Kind.TYPE, "channelCountMode is null");
        }
        checkChannelCountMode(mode);
        channelCountMode = mode;
    }

    /**
     * Sets the <code>channelInterpretation</code> attribute.
     * <p>
     * <code>channelInterpretation</code> 속성을 정한다.
     *
     * @param interpretation the interpretation / 해석
     * @throws AudioException the error of the node type if the interpretation is not allowed / 노드 종류가 허용하지 않는 해석이면 그에 따른 오류
     */
    public final void setChannelInterpretation(ChannelInterpretation interpretation) {
        if (interpretation == null) {
            throw new AudioException(AudioException.Kind.TYPE, "channelInterpretation is null");
        }
        checkChannelInterpretation(interpretation);
        channelInterpretation = interpretation;
    }

    /**
     * Checks a new <code>channelCount</code> against the constraints of the node type; the default accepts everything.
     * <p>
     * 새 <code>channelCount</code> 를 노드 종류의 제약에 비추어 검사한다. 기본은 모두 받아들인다.
     *
     * @param count the new value; the general range check follows after this hook / 새 값. 일반 범위 검사는 이 훅 다음에 한다
     */
    void checkChannelCount(int count) {
    }

    /**
     * Checks a new <code>channelCountMode</code> against the constraints of the node type; the default accepts everything.
     * <p>
     * 새 <code>channelCountMode</code> 를 노드 종류의 제약에 비추어 검사한다. 기본은 모두 받아들인다.
     *
     * @param mode the new value / 새 값
     */
    void checkChannelCountMode(ChannelCountMode mode) {
    }

    /**
     * Checks a new <code>channelInterpretation</code> against the constraints of the node type; the default accepts everything.
     * <p>
     * 새 <code>channelInterpretation</code> 을 노드 종류의 제약에 비추어 검사한다. 기본은 모두 받아들인다.
     *
     * @param interpretation the new value / 새 값
     */
    void checkChannelInterpretation(ChannelInterpretation interpretation) {
    }

    /**
     * Returns the audio of an input in a node hook (rendering thread).
     * <p>
     * 노드 훅에서 입력의 오디오를 반환한다(렌더링 스레드).
     *
     * @param i the input index / 입력 인덱스
     * @return the mixed audio of the input / 입력의 섞인 오디오
     */
    final AudioBus inputBus(int i) {
        return inputs[i].bus;
    }

    /**
     * Returns the audio of an output in a node hook (rendering thread).
     * <p>
     * 노드 훅에서 출력의 오디오를 반환한다(렌더링 스레드).
     *
     * @param i the output index / 출력 인덱스
     * @return the audio of the output / 출력의 오디오
     */
    final AudioBus outputBus(int i) {
        return outputs[i].bus;
    }

    /**
     * Connects an output of this node to an input of another node. A second connection with the same ends is ignored.
     * <p>
     * 이 노드의 출력을 다른 노드의 입력에 연결한다. 끝이 같은 두 번째 연결은 무시한다.
     *
     * @param destination the destination node / 대상 노드
     * @param output      the output index of this node / 이 노드의 출력 인덱스
     * @param input       the input index of the destination / 대상의 입력 인덱스
     * @return the destination node / 대상 노드
     * @throws AudioException <code>InvalidAccessError</code> for a node of another context, <code>IndexSizeError</code> for an index out of range /
     *                        다른 컨텍스트의 노드이면 <code>InvalidAccessError</code>, 인덱스가 범위를 벗어나면 <code>IndexSizeError</code>
     */
    public final AudioNode connect(AudioNode destination, int output, int input) {
        if (destination == null) {
            throw new AudioException(AudioException.Kind.TYPE, "destination is null");
        }
        if (destination.context != context) {
            throw new AudioException(AudioException.Kind.INVALID_ACCESS, "the nodes belong to different contexts");
        }
        checkOutput(output);
        if (input < 0 || input >= destination.numberOfInputs) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "input " + input + " is out of range");
        }
        final Link link = new Link(destination, null, output, input);
        synchronized (context.controlLock) {
            // 끝이 같은 연결이 이미 있으면 무시한다.
            for (int i = 0; i < links.size(); i++) {
                Link l = links.get(i);
                if (l.dstNode == destination && l.output == output && l.input == input) {
                    return destination;
                }
            }
            links.add(link);
        }
        context.post(new Runnable() {
            public void run() {
                AudioNodeOutput out = outputs[link.output];
                AudioNodeInput in = link.dstNode.inputs[link.input];
                out.inputTargets.add(in);
                in.sources.add(out);
                context.markGraphDirty();
            }
        });
        return destination;
    }

    /**
     * Connects output 0 of this node to input 0 of another node.
     * <p>
     * 이 노드의 출력 0 을 다른 노드의 입력 0 에 연결한다.
     *
     * @param destination the destination node / 대상 노드
     * @return the destination node / 대상 노드
     */
    public final AudioNode connect(AudioNode destination) {
        return connect(destination, 0, 0);
    }

    /**
     * Connects an output of this node to a parameter; the output is mixed down to mono and added to the value of the parameter.
     * <p>
     * 이 노드의 출력을 파라미터에 연결한다. 출력은 모노로 다운믹스되어 파라미터의 값에 더해진다.
     *
     * @param destination the destination parameter / 대상 파라미터
     * @param output      the output index of this node / 이 노드의 출력 인덱스
     * @throws AudioException <code>InvalidAccessError</code> for a parameter of another context, <code>IndexSizeError</code> for an output out of range /
     *                        다른 컨텍스트의 파라미터이면 <code>InvalidAccessError</code>, 출력이 범위를 벗어나면 <code>IndexSizeError</code>
     */
    public final void connect(AudioParam destination, int output) {
        if (destination == null) {
            throw new AudioException(AudioException.Kind.TYPE, "destination is null");
        }
        if (destination.getContext() != context) {
            throw new AudioException(AudioException.Kind.INVALID_ACCESS, "the node and the parameter belong to different contexts");
        }
        checkOutput(output);
        final Link link = new Link(null, destination, output, 0);
        synchronized (context.controlLock) {
            // 끝이 같은 연결이 이미 있으면 무시한다.
            for (int i = 0; i < links.size(); i++) {
                Link l = links.get(i);
                if (l.dstParam == destination && l.output == output) {
                    return;
                }
            }
            links.add(link);
        }
        context.post(new Runnable() {
            public void run() {
                AudioNodeOutput out = outputs[link.output];
                out.paramTargets.add(link.dstParam);
                link.dstParam.sources.add(out);
                context.markGraphDirty();
            }
        });
    }

    /**
     * Disconnects every outgoing connection.
     * <p>
     * 나가는 모든 연결을 끊는다.
     */
    public final void disconnect() {
        removeLinks(-1, null, null, -1, false);
    }

    /**
     * Disconnects every connection of one output.
     * <p>
     * 출력 하나의 모든 연결을 끊는다.
     *
     * @param output the output index / 출력 인덱스
     * @throws AudioException <code>IndexSizeError</code> if the output is out of range / 출력이 범위를 벗어나면 <code>IndexSizeError</code>
     */
    public final void disconnect(int output) {
        checkOutput(output);
        removeLinks(output, null, null, -1, false);
    }

    /**
     * Disconnects every connection to a node.
     * <p>
     * 노드로의 모든 연결을 끊는다.
     *
     * @param destination the destination node / 대상 노드
     * @throws AudioException <code>InvalidAccessError</code> if there is no such connection / 그런 연결이 없으면 <code>InvalidAccessError</code>
     */
    public final void disconnect(AudioNode destination) {
        removeLinks(-1, destination, null, -1, true);
    }

    /**
     * Disconnects the connections from an output to a node.
     * <p>
     * 출력에서 노드로의 연결을 끊는다.
     *
     * @param destination the destination node / 대상 노드
     * @param output      the output index / 출력 인덱스
     * @throws AudioException <code>IndexSizeError</code> for an output out of range, <code>InvalidAccessError</code> if there is no such connection /
     *                        출력이 범위를 벗어나면 <code>IndexSizeError</code>, 그런 연결이 없으면 <code>InvalidAccessError</code>
     */
    public final void disconnect(AudioNode destination, int output) {
        checkOutput(output);
        removeLinks(output, destination, null, -1, true);
    }

    /**
     * Disconnects the connection from an output to an input of a node.
     * <p>
     * 출력에서 노드의 입력으로의 연결을 끊는다.
     *
     * @param destination the destination node / 대상 노드
     * @param output      the output index / 출력 인덱스
     * @param input       the input index / 입력 인덱스
     * @throws AudioException <code>IndexSizeError</code> for an index out of range, <code>InvalidAccessError</code> if there is no such connection /
     *                        인덱스가 범위를 벗어나면 <code>IndexSizeError</code>, 그런 연결이 없으면 <code>InvalidAccessError</code>
     */
    public final void disconnect(AudioNode destination, int output, int input) {
        checkOutput(output);
        if (destination == null) {
            throw new AudioException(AudioException.Kind.TYPE, "destination is null");
        }
        if (input < 0 || input >= destination.numberOfInputs) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "input " + input + " is out of range");
        }
        removeLinks(output, destination, null, input, true);
    }

    /**
     * Disconnects every connection to a parameter.
     * <p>
     * 파라미터로의 모든 연결을 끊는다.
     *
     * @param destination the destination parameter / 대상 파라미터
     * @throws AudioException <code>InvalidAccessError</code> if there is no such connection / 그런 연결이 없으면 <code>InvalidAccessError</code>
     */
    public final void disconnect(AudioParam destination) {
        removeLinks(-1, null, destination, -1, true);
    }

    /**
     * Disconnects the connection from an output to a parameter.
     * <p>
     * 출력에서 파라미터로의 연결을 끊는다.
     *
     * @param destination the destination parameter / 대상 파라미터
     * @param output      the output index / 출력 인덱스
     * @throws AudioException <code>IndexSizeError</code> for an output out of range, <code>InvalidAccessError</code> if there is no such connection /
     *                        출력이 범위를 벗어나면 <code>IndexSizeError</code>, 그런 연결이 없으면 <code>InvalidAccessError</code>
     */
    public final void disconnect(AudioParam destination, int output) {
        checkOutput(output);
        removeLinks(output, null, destination, -1, true);
    }

    /**
     * Throws an <code>IndexSizeError</code> if the output index does not exist.
     * <p>
     * 출력 인덱스가 없으면 <code>IndexSizeError</code> 를 던진다.
     *
     * @param output the output index / 출력 인덱스
     */
    private void checkOutput(int output) {
        if (output < 0 || output >= numberOfOutputs) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "output " + output + " is out of range");
        }
    }

    /**
     * Removes the links that match the filter and queues the control message that removes the connections of the rendering side.
     * <p>
     * 필터에 맞는 링크를 지우고 렌더링 쪽의 연결을 지우는 제어 메시지를 쌓는다.
     *
     * @param output      the output index, or -1 for any / 출력 인덱스. 아무것이나이면 -1
     * @param node        the destination node, or null / 대상 노드 또는 null
     * @param param       the destination parameter, or null / 대상 파라미터 또는 null
     * @param input       the input index, or -1 for any / 입력 인덱스. 아무것이나이면 -1
     * @param mustMatch   true if an <code>InvalidAccessError</code> is thrown when nothing matches / 맞는 것이 없을 때 <code>InvalidAccessError</code> 를 던지면 true
     */
    private void removeLinks(int output, AudioNode node, AudioParam param, int input, boolean mustMatch) {
        if (mustMatch && node == null && param == null) {
            throw new AudioException(AudioException.Kind.TYPE, "destination is null");
        }
        final ArrayList<Link> removed = new ArrayList<Link>();
        synchronized (context.controlLock) {
            // 필터에 맞는 링크를 모아 목록에서 지운다.
            for (int i = links.size() - 1; i >= 0; i--) {
                Link l = links.get(i);
                boolean match = (output < 0 || l.output == output) && (node == null || l.dstNode == node) && (param == null || l.dstParam == param)
                        && (input < 0 || l.input == input);
                if (match) {
                    removed.add(links.remove(i));
                }
            }
        }
        if (removed.isEmpty()) {
            if (mustMatch) {
                throw new AudioException(AudioException.Kind.INVALID_ACCESS, "there is no such connection");
            }
            return;
        }
        context.post(new Runnable() {
            public void run() {
                // 렌더링 쪽 연결을 지운다.
                for (int i = 0; i < removed.size(); i++) {
                    Link l = removed.get(i);
                    AudioNodeOutput out = outputs[l.output];
                    if (l.dstNode != null) {
                        AudioNodeInput in = l.dstNode.inputs[l.input];
                        out.inputTargets.remove(in);
                        in.sources.remove(out);
                    } else {
                        out.paramTargets.remove(l.dstParam);
                        l.dstParam.sources.remove(out);
                    }
                }
                context.markGraphDirty();
            }
        });
    }

    /**
     * Tells whether this node produces audio by itself (a source) instead of by processing its inputs.
     * <p>
     * 이 노드가 입력을 처리하는 대신 스스로 오디오를 만드는지(원천인지) 알려 준다.
     *
     * @return true for a source / 원천이면 true
     */
    boolean isSource() {
        return false;
    }

    /**
     * Tells whether a source plays in at least a part of the quantum; a source overrides it and may finish itself here.
     * <p>
     * 원천이 quantum 의 일부라도 재생하는지 알려 준다. 원천이 재정의하며 여기서 스스로 끝낼 수 있다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @return true if the source plays / 원천이 재생하면 true
     */
    boolean sourceActive(long frame0, int frames) {
        return false;
    }

    /**
     * Returns the tail time in frames: how long the output stays non-silent after the input became silent. 0 for a node without memory.
     * <p>
     * 꼬리 시간(프레임)을 반환한다: 입력이 무음이 된 뒤 출력이 얼마나 오래 무음이 아닌지. 기억이 없는 노드는 0.
     *
     * @return the tail time in frames / 꼬리 시간(프레임)
     */
    long tailFrames() {
        return 0;
    }

    /**
     * Called when a node that was active becomes inactive: it clears its internal state (filter memory, delay line).
     * <p>
     * 활성이던 노드가 비활성이 될 때 불린다: 내부 상태(필터 기억, 지연선)를 지운다.
     */
    void resetState() {
    }

    /**
     * Called after the quantum was rendered, so that a source can finish.
     * <p>
     * quantum 을 렌더링한 뒤 불려서 원천이 끝날 수 있게 한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param active true if the node was active / 노드가 활성이었으면 true
     */
    void afterRender(long frame0, int frames, boolean active) {
    }

    /**
     * Processes one quantum: reads the audio of the inputs and the values of the parameters and writes the audio of the outputs.
     * Called only when the node is active. The node sets the number of channels of its outputs.
     * <p>
     * quantum 하나를 처리한다: 입력의 오디오와 파라미터의 값을 읽고 출력의 오디오를 쓴다. 노드가 활성일 때만 불린다. 노드가 출력의 채널 수를 정한다.
     *
     * @param frames the number of frames of the quantum / quantum 의 프레임 수
     */
    abstract void process(int frames);

    /**
     * Renders one step of the rendering order. Only a delay node that breaks a cycle is rendered in two steps; every other node in one.
     * <p>
     * 렌더링 순서의 한 단계를 렌더링한다. 순환을 끊는 지연 노드만 두 단계로, 나머지 노드는 한 단계로 렌더링한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param serial the render serial of the quantum / quantum 의 렌더링 일련번호
     * @param phase  {@link #PHASE_FULL}, {@link #PHASE_READ} or {@link #PHASE_WRITE} / 단계
     */
    final void renderStep(long frame0, int frames, long serial, int phase) {
        renderFrame0 = frame0;
        if (phase == PHASE_FULL) {
            renderFull(frame0, frames, serial);
        } else {
            renderSplit(frame0, frames, serial, phase);
        }
    }

    /**
     * Renders one step of a node that is split into two steps; only a delay node supports it.
     * <p>
     * 두 단계로 나뉜 노드의 한 단계를 렌더링한다. 지연 노드만 지원한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param serial the render serial of the quantum / quantum 의 렌더링 일련번호
     * @param phase  {@link #PHASE_READ} or {@link #PHASE_WRITE} / 단계
     */
    void renderSplit(long frame0, int frames, long serial, int phase) {
        throw new IllegalStateException("this node cannot be split");
    }

    /**
     * Pulls the inputs and decides whether the node is active.
     * <p>
     * 입력을 끌어오고 노드가 활성인지 정한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param serial the render serial of the quantum / quantum 의 렌더링 일련번호
     * @return true if the node is active / 노드가 활성이면 true
     */
    final boolean pullAndDecide(long frame0, int frames, long serial) {
        boolean inputActive = false;
        // 입력을 모두 끌어와 섞는다.
        for (int i = 0; i < inputs.length; i++) {
            inputs[i].pull(serial);
            inputActive |= inputs[i].anyActive;
        }
        if (isSource()) {
            return sourceActive(frame0, frames);
        }
        if (inputActive) {
            lastInputActiveEnd = frame0 + frames;
        }
        return inputActive || frame0 < lastInputActiveEnd + tailFrames();
    }

    /**
     * Computes the parameters of the node for the quantum.
     * <p>
     * 노드의 파라미터를 quantum 에 맞게 계산한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     */
    final void computeParams(long frame0, int frames) {
        // 노드의 모든 파라미터를 계산한다.
        for (int i = 0; i < params.size(); i++) {
            params.get(i).computeValues(frame0, frames);
        }
    }

    /**
     * Marks the outputs as produced in this quantum.
     * <p>
     * 출력이 이번 quantum 에 만들어졌다고 표시한다.
     *
     * @param serial the render serial / 렌더링 일련번호
     * @param active the active flag of the outputs / 출력의 활성 표시
     */
    final void stampOutputs(long serial, boolean active) {
        // 모든 출력에 도장과 활성 표시를 한다.
        for (int i = 0; i < outputs.length; i++) {
            outputs[i].stamp = serial;
            outputs[i].active = active;
        }
    }

    /**
     * Renders the whole node: parameters, inputs, processing or silence.
     * <p>
     * 노드 전체를 렌더링한다: 파라미터, 입력, 처리 또는 무음.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param serial the render serial of the quantum / quantum 의 렌더링 일련번호
     */
    private void renderFull(long frame0, int frames, long serial) {
        computeParams(frame0, frames);
        boolean active = pullAndDecide(frame0, frames, serial);
        stampOutputs(serial, active);
        if (active) {
            process(frames);
        } else {
            // 활성이 아니면 출력은 무음 채널 하나이다.
            for (int i = 0; i < outputs.length; i++) {
                outputs[i].bus.makeSilentMono();
            }
            if (wasActive) {
                resetState();
            }
        }
        context.updatePin(this, active);
        afterRender(frame0, frames, active);
        wasActive = active;
    }
}
