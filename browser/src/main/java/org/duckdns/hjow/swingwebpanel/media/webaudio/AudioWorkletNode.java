package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The engine side of an <code>AudioWorkletNode</code> (Web Audio API 1.0, 1.32): a node with a configurable number of inputs and outputs and a set of
 * named {@link AudioParam}s whose audio is produced by a {@link Processor}, code that lives outside the engine (the script layer runs a JavaScript
 * <code>AudioWorkletProcessor</code> there). The class is plain Java: it knows nothing about scripts, it only runs the processor on the rendering thread once
 * per render quantum and keeps the rules of the specification around it.
 * <ul>
 * <li><strong>Channels.</strong> Defaults are <code>channelCount</code> 2, mode <code>max</code>, interpretation <code>speakers</code>. The number of channels of an
 * output is the matching <code>outputChannelCount</code> entry; without the list it is 1, except when the node has exactly one input and one output, when it follows the
 * number of channels of the input (<code>computedNumberOfChannels</code>). The constructor refuses a node without inputs and outputs and an
 * <code>outputChannelCount</code> list that does not fit (1.32.3.3.2).</li>
 * <li><strong>The processor.</strong> It is created by a {@link ProcessorFactory} that runs on the rendering thread as a control message, at the start of the next
 * render quantum (the specification queues "a control message to invoke the constructor"). A factory that is not ready may return <code>null</code>; it is asked again
 * at the next quantum, up to {@value #MAX_CONSTRUCTION_ATTEMPTS} times. Until the processor exists, and after it failed, the output is silence.</li>
 * <li><strong>Processing.</strong> In every quantum in which the node is actively processing, {@link Processor#process} reads the active inputs with
 * {@link #getInput(int)} (<code>null</code> while nothing active is connected to the input), writes the outputs ({@link #getOutput(int)}, zero filled with the right
 * number of channels) and reads the parameter values ({@link #copyParameter(int, float[])}: one value when the block is constant, a value per frame otherwise). Its
 * result is the <em>active source flag</em>: {@link #ACTIVE} keeps the node processing without active inputs (the node is retained by the context, 1.32.3),
 * {@link #INACTIVE} makes the node process only while an input is active, {@link #SKIPPED} means the processor could not run in time: silence for this quantum and
 * nothing else changes.</li>
 * <li><strong>Failure.</strong> A <code>RuntimeException</code> of the processor (a {@link WorkletProcessorException} carries the report of a script error) ends it for
 * good: the active source flag becomes false, the output of that quantum and of every later one is silence, and the {@link ErrorListener} is told on the control side
 * (the <code>processorerror</code> event, 1.32.3.2).</li>
 * </ul>
 * <p>
 * <code>AudioWorkletNode</code> 의 엔진 쪽(Web Audio API 1.0, 1.32): 입력과 출력의 수를 정할 수 있고 이름 있는 {@link AudioParam} 을 가진 노드이며, 오디오는 엔진 밖에 있는
 * 코드인 {@link Processor} 가 만든다(스크립트 계층이 그 자리에서 JavaScript <code>AudioWorkletProcessor</code> 를 실행한다). 클래스는 순수 Java 이다: 스크립트를 모르고, render
 * quantum 마다 렌더링 스레드에서 프로세서를 실행하며 그 둘레의 규격 규칙만 지킨다.
 * <ul>
 * <li><strong>채널.</strong> 기본값은 <code>channelCount</code> 2, 모드 <code>max</code>, 해석 <code>speakers</code> 이다. 출력의 채널 수는 <code>outputChannelCount</code> 의 해당 항목이다.
 * 목록이 없으면 1 이고, 입력과 출력이 정확히 하나씩일 때만 입력의 채널 수(<code>computedNumberOfChannels</code>)를 따른다. 생성자는 입력과 출력이 모두 없는 노드와 맞지 않는
 * <code>outputChannelCount</code> 목록을 거부한다(1.32.3.3.2).</li>
 * <li><strong>프로세서.</strong> {@link ProcessorFactory} 가 만들며, 렌더링 스레드에서 제어 메시지로 다음 render quantum 시작에 실행된다(규격은 "생성자를 호출하는 제어 메시지"를 쌓는다).
 * 준비되지 않은 팩터리는 <code>null</code> 을 반환할 수 있고 다음 quantum 에서 다시 부르며, 최대 {@value #MAX_CONSTRUCTION_ATTEMPTS} 번 시도한다. 프로세서가 생기기 전과 실패한 뒤의 출력은 무음이다.</li>
 * <li><strong>처리.</strong> 노드가 활성으로 처리하는 quantum 마다 {@link Processor#process} 가 {@link #getInput(int)} 으로 활성 입력을 읽고(입력에 활성인 것이 연결되어 있지 않으면
 * <code>null</code>), 출력({@link #getOutput(int)}, 알맞은 채널 수로 0 을 채움)을 쓰고, 파라미터 값({@link #copyParameter(int, float[])}: 블록이 일정하면 값 하나, 아니면 프레임마다 하나)을
 * 읽는다. 결과는 <em>active source flag</em> 이다: {@link #ACTIVE} 는 활성 입력이 없어도 노드가 계속 처리하게 하고(컨텍스트가 노드를 붙잡는다. 1.32.3), {@link #INACTIVE} 는 입력이 활성인 동안만
 * 처리하게 하며, {@link #SKIPPED} 는 프로세서가 제때 실행하지 못했다는 뜻이다(이번 quantum 은 무음이고 다른 것은 바뀌지 않는다).</li>
 * <li><strong>실패.</strong> 프로세서의 <code>RuntimeException</code>({@link WorkletProcessorException} 이 스크립트 오류의 보고를 담는다)은 프로세서를 영구히 끝낸다: active source flag 가 false 가
 * 되고, 그 quantum 과 이후 모든 quantum 의 출력은 무음이며, 제어 쪽에서 {@link ErrorListener} 가 통지받는다(<code>processorerror</code> 이벤트, 1.32.3.2).</li>
 * </ul>
 */
public final class AudioWorkletNode extends AudioNode {

    /**
     * The result of {@link Processor#process}: the processor returned true (the active source flag becomes true).
     * <p>
     * {@link Processor#process} 의 결과: 프로세서가 true 를 반환했다(active source flag 가 true 가 된다).
     */
    public static final int ACTIVE = 1;

    /**
     * The result of {@link Processor#process}: the processor returned false (the active source flag becomes false).
     * <p>
     * {@link Processor#process} 의 결과: 프로세서가 false 를 반환했다(active source flag 가 false 가 된다).
     */
    public static final int INACTIVE = 0;

    /**
     * The result of {@link Processor#process}: the processor could not run in time; the output of the quantum is silence and the active source flag stays.
     * <p>
     * {@link Processor#process} 의 결과: 프로세서가 제때 실행하지 못했다. quantum 의 출력은 무음이고 active source flag 는 그대로이다.
     */
    public static final int SKIPPED = 2;

    /**
     * How many quanta a {@link ProcessorFactory} that answers <code>null</code> (not ready) is asked before the construction counts as failed.
     * <p>
     * 준비되지 않았다며 <code>null</code> 로 답하는 {@link ProcessorFactory} 에게 몇 quantum 동안 다시 물은 뒤 생성을 실패로 칠지.
     */
    public static final int MAX_CONSTRUCTION_ATTEMPTS = 2000;

    /**
     * The description of a parameter of the node: what <code>AudioParamDescriptor</code> gives (1.32.4.4). The values are checked here too.
     * <p>
     * 노드의 파라미터 설명: <code>AudioParamDescriptor</code> 가 주는 것(1.32.4.4). 값은 여기서도 검사한다.
     */
    public static final class ParamDescriptor {

        /**
         * The name.
         * <p>
         * 이름.
         */
        private final String name;

        /**
         * The default value.
         * <p>
         * 기본 값.
         */
        private final float defaultValue;

        /**
         * The nominal minimum.
         * <p>
         * 명목 최솟값.
         */
        private final float minValue;

        /**
         * The nominal maximum.
         * <p>
         * 명목 최댓값.
         */
        private final float maxValue;

        /**
         * The automation rate.
         * <p>
         * 자동화 속도.
         */
        private final AutomationRate rate;

        /**
         * Creates a description.
         * <p>
         * 설명을 만든다.
         *
         * @param name         the name, not null / 이름(null 불가)
         * @param defaultValue the default value / 기본 값
         * @param minValue     the nominal minimum / 명목 최솟값
         * @param maxValue     the nominal maximum / 명목 최댓값
         * @param rate         the automation rate, not null / 자동화 속도(null 불가)
         * @throws AudioException <code>InvalidStateError</code> unless <code>minValue &lt;= defaultValue &lt;= maxValue</code> / <code>minValue &lt;= defaultValue &lt;= maxValue</code>
         *                        가 아니면 <code>InvalidStateError</code>
         */
        public ParamDescriptor(String name, float defaultValue, float minValue, float maxValue, AutomationRate rate) {
            if (name == null || rate == null) {
                throw new AudioException(AudioException.Kind.TYPE, "the name and the automation rate of a parameter are required");
            }
            if (!(minValue <= defaultValue && defaultValue <= maxValue)) {
                throw new AudioException(AudioException.Kind.INVALID_STATE, "The defaultValue " + defaultValue + " of '" + name + "' is not in [" + minValue + ", " + maxValue + "].");
            }
            this.name = name;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.rate = rate;
        }

        /**
         * Returns the name.
         * <p>
         * 이름을 반환한다.
         *
         * @return the name / 이름
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the default value.
         * <p>
         * 기본 값을 반환한다.
         *
         * @return the default value / 기본 값
         */
        public float getDefaultValue() {
            return defaultValue;
        }

        /**
         * Returns the nominal minimum.
         * <p>
         * 명목 최솟값을 반환한다.
         *
         * @return the minimum / 최솟값
         */
        public float getMinValue() {
            return minValue;
        }

        /**
         * Returns the nominal maximum.
         * <p>
         * 명목 최댓값을 반환한다.
         *
         * @return the maximum / 최댓값
         */
        public float getMaxValue() {
            return maxValue;
        }

        /**
         * Returns the automation rate.
         * <p>
         * 자동화 속도를 반환한다.
         *
         * @return the rate / 속도
         */
        public AutomationRate getAutomationRate() {
            return rate;
        }
    }

    /**
     * The code that produces the audio of the node, once per render quantum on the rendering thread.
     * <p>
     * 노드의 오디오를 만드는 코드. render quantum 마다 렌더링 스레드에서 한 번 실행된다.
     */
    public interface Processor {

        /**
         * Processes one quantum: reads the inputs and the parameters and fills the outputs of the node.
         * <p>
         * quantum 하나를 처리한다: 입력과 파라미터를 읽고 노드의 출력을 채운다.
         *
         * @param node   the node; only its accessors for the rendering thread may be used / 노드. 렌더링 스레드용 접근자만 쓸 수 있다
         * @param frames the number of frames of the quantum / quantum 의 프레임 수
         * @return {@link AudioWorkletNode#ACTIVE}, {@link AudioWorkletNode#INACTIVE} or {@link AudioWorkletNode#SKIPPED} / {@link AudioWorkletNode#ACTIVE},
         *         {@link AudioWorkletNode#INACTIVE}, {@link AudioWorkletNode#SKIPPED} 중 하나
         * @throws RuntimeException the failure of the processor; it ends the processor / 프로세서의 실패. 프로세서를 끝낸다
         */
        int process(AudioWorkletNode node, int frames);
    }

    /**
     * Creates the processor of a node, on the rendering thread at the start of a render quantum.
     * <p>
     * 노드의 프로세서를 렌더링 스레드에서 render quantum 시작에 만든다.
     */
    public interface ProcessorFactory {

        /**
         * Creates the processor.
         * <p>
         * 프로세서를 만든다.
         *
         * @param node the node / 노드
         * @return the processor, or null if it cannot be created just now (it is asked again at the next quantum) / 프로세서. 지금 만들 수 없으면 null(다음 quantum 에 다시 묻는다)
         * @throws RuntimeException the failure of the construction (a {@link WorkletProcessorException} for a script error); the node fails like a processor that failed
         *                          while processing / 생성의 실패(스크립트 오류는 {@link WorkletProcessorException}). 노드는 처리 중 실패한 프로세서처럼 실패한다
         */
        Processor create(AudioWorkletNode node);
    }

    /**
     * Told on the control side that the processor failed (the <code>processorerror</code> event).
     * <p>
     * 프로세서가 실패했음을 제어 쪽에서 통지받는다(<code>processorerror</code> 이벤트).
     */
    public interface ErrorListener {

        /**
         * Called through the task sink of the context, once per node.
         * <p>
         * 컨텍스트의 작업 싱크를 거쳐 호출된다. 노드마다 한 번.
         *
         * @param failure the failure / 실패
         */
        void processorError(RuntimeException failure);
    }

    /**
     * The parameters, in the order of the descriptors.
     * <p>
     * 파라미터(설명의 순서).
     */
    private final List<AudioParam> paramList = new ArrayList<AudioParam>();

    /**
     * The number of channels of each output that the options fixed, or null.
     * <p>
     * 옵션이 정한 출력별 채널 수. 없으면 null.
     */
    private final int[] outputChannelCount;

    /**
     * The processor, set by the control message that runs the factory (rendering thread).
     * <p>
     * 프로세서. 팩터리를 실행하는 제어 메시지가 정한다(렌더링 스레드).
     */
    private volatile Processor processor;

    /**
     * The failure that ended the processor, or null.
     * <p>
     * 프로세서를 끝낸 실패. 없으면 null.
     */
    private volatile RuntimeException failure;

    /**
     * The active source flag (rendering thread).
     * <p>
     * active source flag(렌더링 스레드).
     */
    private volatile boolean activeSource = true;

    /**
     * True while the context keeps the node alive because of the active source flag (rendering thread).
     * <p>
     * active source flag 때문에 컨텍스트가 노드를 붙잡고 있는 동안 true(렌더링 스레드).
     */
    private boolean pinnedByFlag;

    /**
     * The listener of the failure of the processor.
     * <p>
     * 프로세서 실패의 리스너.
     */
    private volatile ErrorListener errorListener;

    /**
     * The number of quanta in which the processor ran.
     * <p>
     * 프로세서가 실행된 quantum 수.
     */
    private volatile long processCount;

    /**
     * The number of quanta that the processor skipped.
     * <p>
     * 프로세서가 건너뛴 quantum 수.
     */
    private volatile long skippedCount;

    /**
     * Creates the node.
     * <p>
     * 노드를 만든다.
     *
     * @param context            the context / 컨텍스트
     * @param numberOfInputs     the number of inputs, 0 to 32 / 입력 수(0 ~ 32)
     * @param numberOfOutputs    the number of outputs, 0 to 32 / 출력 수(0 ~ 32)
     * @param outputChannelCount the number of channels of every output, or null for none given / 모든 출력의 채널 수. 주어지지 않았으면 null
     * @param descriptors        the descriptors of the parameters, in order, not null / 파라미터 설명(순서대로, null 불가)
     * @throws AudioException <code>NotSupportedError</code> if both numbers are 0, a number or a channel count is out of range, or two parameters have the same name;
     *                        <code>IndexSizeError</code> if the length of <code>outputChannelCount</code> differs from <code>numberOfOutputs</code> / 두 수가 모두 0 이거나, 수나 채널 수가
     *                        범위 밖이거나, 파라미터 이름이 같으면 <code>NotSupportedError</code>. <code>outputChannelCount</code> 의 길이가 <code>numberOfOutputs</code> 와 다르면
     *                        <code>IndexSizeError</code>
     */
    public AudioWorkletNode(BaseAudioContext context, int numberOfInputs, int numberOfOutputs, int[] outputChannelCount, List<ParamDescriptor> descriptors) {
        super(context, checkCount(numberOfInputs, "numberOfInputs"), checkCount(numberOfOutputs, "numberOfOutputs"), 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        // 입력과 출력이 모두 없는 노드는 만들 수 없다.
        if (numberOfInputs == 0 && numberOfOutputs == 0) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "Cannot create an AudioWorkletNode with no inputs and no outputs.");
        }
        int[] fixed = null;
        // outputChannelCount 가 주어졌으면 길이와 각 값을 검사해 복사한다.
        if (outputChannelCount != null) {
            if (outputChannelCount.length != numberOfOutputs) {
                throw new AudioException(AudioException.Kind.INDEX_SIZE, "Length of the outputChannelCount array (" + outputChannelCount.length
                        + ") does not match the number of outputs (" + numberOfOutputs + ").");
            }
            fixed = new int[outputChannelCount.length];
            // 값마다 1 이상 최대 채널 수 이하인지 확인한다.
            for (int i = 0; i < outputChannelCount.length; i++) {
                if (outputChannelCount[i] < 1 || outputChannelCount[i] > WebAudioLimits.MAX_CHANNELS) {
                    throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "Channel count of " + outputChannelCount[i] + " in outputChannelCount[" + i
                            + "] is out of range [1, " + WebAudioLimits.MAX_CHANNELS + "].");
                }
                fixed[i] = outputChannelCount[i];
            }
        }
        this.outputChannelCount = fixed;
        // 설명마다 파라미터를 만들고 이름이 겹치면 거부한다.
        for (int i = 0; i < descriptors.size(); i++) {
            ParamDescriptor d = descriptors.get(i);
            // 이미 만든 파라미터와 이름이 같은지 확인한다.
            for (int j = 0; j < paramList.size(); j++) {
                if (paramList.get(j).getName().equals(d.getName())) {
                    throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "Two parameters have the same name '" + d.getName() + "'.");
                }
            }
            paramList.add(registerParam(new AudioParam(context, this, d.getName(), d.getDefaultValue(), d.getMinValue(), d.getMaxValue(), d.getAutomationRate(), false)));
        }
    }

    /**
     * Checks the number of inputs or outputs.
     * <p>
     * 입력이나 출력의 수를 검사한다.
     *
     * @param n    the number / 수
     * @param what the name for the message / 메시지에 쓸 이름
     * @return the number / 수
     */
    private static int checkCount(int n, String what) {
        if (n < 0 || n > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "The " + what + " " + n + " is outside the range [0, " + WebAudioLimits.MAX_CHANNELS + "].");
        }
        return n;
    }

    /**
     * Returns the parameters in the order of the descriptors.
     * <p>
     * 설명의 순서대로 파라미터를 반환한다.
     *
     * @return the parameters, read only / 파라미터(읽기 전용)
     */
    public List<AudioParam> getParameters() {
        return Collections.unmodifiableList(paramList);
    }

    /**
     * Returns the parameter with a name.
     * <p>
     * 이름이 있는 파라미터를 반환한다.
     *
     * @param name the name / 이름
     * @return the parameter, or null if there is none / 파라미터. 없으면 null
     */
    public AudioParam getParameter(String name) {
        // 이름이 같은 파라미터를 찾는다.
        for (int i = 0; i < paramList.size(); i++) {
            if (paramList.get(i).getName().equals(name)) {
                return paramList.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the number of channels that an output has when the options fixed it, or 0 when it follows the input or is 1.
     * <p>
     * 옵션이 출력의 채널 수를 정했으면 그 값을, 입력을 따르거나 1 이면 0 을 반환한다.
     *
     * @param output the output index / 출력 인덱스
     * @return the fixed number of channels, 0 if the options did not fix it / 정해진 채널 수. 옵션이 정하지 않았으면 0
     */
    public int getFixedOutputChannels(int output) {
        return outputChannelCount == null ? 0 : outputChannelCount[output];
    }

    /**
     * Sets the factory of the processor: queues the control message that runs it at the start of the next render quantum. Call it once, right after the node was created.
     * <p>
     * 프로세서의 팩터리를 정한다: 다음 render quantum 시작에 팩터리를 실행하는 제어 메시지를 쌓는다. 노드를 만든 직후 한 번 부른다.
     *
     * @param factory the factory, not null / 팩터리(null 불가)
     */
    public void setProcessorFactory(final ProcessorFactory factory) {
        if (factory == null) {
            throw new AudioException(AudioException.Kind.TYPE, "factory is null");
        }
        final int[] attempts = new int[1];
        getContext().post(new Runnable() {
            /**
             * Runs the factory on the rendering thread; asks again at the next quantum if it is not ready.
             * <p>
             * 렌더링 스레드에서 팩터리를 실행한다. 준비되지 않았으면 다음 quantum 에 다시 묻는다.
             */
            public void run() {
                if (failure != null) {
                    return;
                }
                Processor created;
                try {
                    created = factory.create(AudioWorkletNode.this);
                } catch (RuntimeException e) {
                    fail(e);
                    return;
                }
                // 준비되지 않았으면 다음 quantum 에 다시 시도하되, 너무 오래 걸리면 실패로 친다.
                if (created == null) {
                    if (++attempts[0] >= MAX_CONSTRUCTION_ATTEMPTS) {
                        fail(new WorkletProcessorException("The processor could not be created in time.", "", 0, 0));
                    } else {
                        getContext().post(this);
                    }
                    return;
                }
                processor = created;
                activeSource = true;
                syncPin();
            }
        });
    }

    /**
     * Sets the listener that is told when the processor fails.
     * <p>
     * 프로세서가 실패하면 통지받는 리스너를 정한다.
     *
     * @param listener the listener, or null for none / 리스너. 없으면 null
     */
    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Returns the failure that ended the processor.
     * <p>
     * 프로세서를 끝낸 실패를 반환한다.
     *
     * @return the failure, or null while the processor did not fail / 실패. 프로세서가 실패하지 않았으면 null
     */
    public RuntimeException getFailure() {
        return failure;
    }

    /**
     * Returns the processor, if the factory created it.
     * <p>
     * 팩터리가 만들었으면 프로세서를 반환한다.
     *
     * @return the processor or null / 프로세서 또는 null
     */
    public Processor getProcessor() {
        return processor;
    }

    /**
     * Returns the active source flag.
     * <p>
     * active source flag 를 반환한다.
     *
     * @return the flag / 표시
     */
    public boolean isActiveSource() {
        return activeSource;
    }

    /**
     * Returns the number of quanta in which the processor ran (also the quanta that it skipped).
     * <p>
     * 프로세서가 실행된 quantum 수를 반환한다(건너뛴 quantum 도 센다).
     *
     * @return the number of quanta / quantum 수
     */
    public long getProcessCount() {
        return processCount;
    }

    /**
     * Returns the number of quanta that the processor skipped (it could not run in time).
     * <p>
     * 프로세서가 건너뛴 quantum 수(제때 실행하지 못함)를 반환한다.
     *
     * @return the number of quanta / quantum 수
     */
    public long getSkippedCount() {
        return skippedCount;
    }

    /**
     * Returns the audio of an input for the quantum that the processor is called for (rendering thread).
     * <p>
     * 프로세서가 호출된 quantum 의 입력 오디오를 반환한다(렌더링 스레드).
     *
     * @param input the input index / 입력 인덱스
     * @return the mixed audio of the input, or null if no actively processing node is connected to it (zero channels, 1.32.4.3.1) / 입력의 섞인 오디오. 활성으로 처리하는 노드가
     *         연결되어 있지 않으면 null(채널 0 개. 1.32.4.3.1)
     */
    public AudioBus getInput(int input) {
        return inputs[input].anyActive ? inputBus(input) : null;
    }

    /**
     * Returns the buffer of an output that the processor has to fill (rendering thread): it has the right number of channels and is filled with zero.
     * <p>
     * 프로세서가 채워야 하는 출력의 버퍼를 반환한다(렌더링 스레드). 채널 수는 알맞고 0 으로 채워져 있다.
     *
     * @param output the output index / 출력 인덱스
     * @return the buffer / 버퍼
     */
    public AudioBus getOutput(int output) {
        return outputBus(output);
    }

    /**
     * Copies the computed values of a parameter for the quantum (rendering thread): one value if the parameter is constant over the quantum (always so for a k-rate
     * parameter), otherwise one value per frame (1.32.4.3.1).
     * <p>
     * 파라미터가 quantum 에 대해 계산된 값을 복사한다(렌더링 스레드): 파라미터가 quantum 동안 일정하면(k-rate 는 항상) 값 하나, 아니면 프레임마다 하나(1.32.4.3.1).
     *
     * @param index  the index of the parameter, in the order of the descriptors / 파라미터 인덱스(설명의 순서)
     * @param target the array that receives the values, at least as long as a quantum / 값을 받는 배열(quantum 길이 이상)
     * @return the number of values that were copied, 1 or the number of frames / 복사한 값의 수. 1 또는 프레임 수
     */
    public int copyParameter(int index, float[] target) {
        AudioParam p = paramList.get(index);
        // 블록이 일정하면 값 하나만, 아니면 프레임마다 값을 복사한다.
        if (p.isConstantBlock()) {
            target[0] = p.valueAt(0);
            return 1;
        }
        int frames = getContext().getRenderQuantumSize();
        System.arraycopy(p.getBlockValues(), 0, target, 0, frames);
        return frames;
    }

    /**
     * The node is processed while the active source flag is true, besides while an input is active.
     * <p>
     * 노드는 입력이 활성인 동안에 더해 active source flag 가 true 인 동안에도 처리된다.
     *
     * @return true while the processor exists, did not fail and the flag is true / 프로세서가 있고, 실패하지 않았고, 표시가 true 인 동안 true
     */
    @Override
    boolean alwaysActive() {
        return processor != null && failure == null && activeSource;
    }

    /**
     * Pins the node in the context while the active source flag holds, and releases it when it does not (rendering thread).
     * <p>
     * active source flag 가 true 인 동안 노드를 컨텍스트에 붙잡아 두고 아니면 놓아 준다(렌더링 스레드).
     */
    private void syncPin() {
        boolean want = alwaysActive();
        if (want && !pinnedByFlag) {
            getContext().pin(this);
            pinnedByFlag = true;
        } else if (!want && pinnedByFlag) {
            getContext().unpin(this);
            pinnedByFlag = false;
        }
    }

    /**
     * Fills the outputs with silence of the right number of channels.
     * <p>
     * 출력을 알맞은 채널 수의 무음으로 채운다.
     */
    private void prepareOutputs() {
        int outs = getNumberOfOutputs();
        // 출력마다 채널 수를 정하고 0 으로 채운다. 입력과 출력이 하나씩이고 채널 수가 정해지지 않았으면 입력의 채널 수를 따른다.
        for (int j = 0; j < outs; j++) {
            int n;
            if (outputChannelCount != null) {
                n = outputChannelCount[j];
            } else if (getNumberOfInputs() == 1 && outs == 1) {
                n = Math.max(1, inputBus(0).getNumberOfChannels());
            } else {
                n = 1;
            }
            AudioBus b = outputBus(j);
            b.setNumberOfChannels(n);
            b.zero();
        }
    }

    /**
     * Runs the processor for one quantum.
     * <p>
     * quantum 하나에 대해 프로세서를 실행한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        prepareOutputs();
        Processor p = processor;
        // 프로세서가 아직 없거나 실패했으면 출력은 무음이다.
        if (p == null || failure != null) {
            return;
        }
        int result;
        processCount++;
        try {
            result = p.process(this, frames);
        } catch (RuntimeException e) {
            // 실패하면 이 quantum 의 출력도 무음으로 되돌린다.
            prepareOutputs();
            fail(e);
            return;
        }
        if (result == SKIPPED) {
            skippedCount++;
            prepareOutputs();
        } else {
            activeSource = result == ACTIVE;
        }
    }

    /**
     * Ends the processor for good and tells the control side.
     * <p>
     * 프로세서를 영구히 끝내고 제어 쪽에 알린다.
     *
     * @param e the failure / 실패
     */
    private void fail(final RuntimeException e) {
        if (failure != null) {
            return;
        }
        failure = e;
        activeSource = false;
        syncPin();
        final ErrorListener l = errorListener;
        // 리스너가 있으면 컨텍스트의 작업 싱크로 제어 쪽에 알린다.
        if (l != null) {
            getContext().postTask(new Runnable() {
                /**
                 * Tells the listener.
                 * <p>
                 * 리스너에게 알린다.
                 */
                public void run() {
                    l.processorError(e);
                }
            });
        }
    }

    /**
     * Updates the retention of the node after a quantum.
     * <p>
     * quantum 이 끝난 뒤 노드의 붙잡기를 갱신한다.
     *
     * @param frame0 the index of the first frame of the quantum / quantum 첫 프레임의 인덱스
     * @param frames the number of frames / 프레임 수
     * @param active true if the node was active / 노드가 활성이었으면 true
     */
    @Override
    void afterRender(long frame0, int frames, boolean active) {
        syncPin();
    }
}
