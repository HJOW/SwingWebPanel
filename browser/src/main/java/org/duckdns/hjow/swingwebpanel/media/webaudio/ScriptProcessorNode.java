package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;

/**
 * The deprecated node that lets a script generate or process audio directly (Web Audio API 1.0, 1.29 <code>ScriptProcessorNode</code> and
 * 1.12 <code>AudioProcessingEvent</code>), implemented for old pages. The class is pure Java: it collects the input in blocks of
 * <code>bufferSize</code> frames, asks the control side (through the {@link BaseAudioContext.TaskSink} of the context, the event loop of the
 * page) to run a {@link ProcessListener} for each block, and plays what the listener wrote into the output buffer one block later.
 * <ul>
 * <li><strong>Blocks and delay.</strong> The input of block <em>k</em> (frames <em>kB</em> .. <em>(k+1)B-1</em>) is complete at the end of the block, when
 * the event for it is requested. The listener has one whole block of time to answer; the output that it wrote is played during block
 * <em>k+2</em>. So the audio is delayed by <em>2 bufferSize</em> frames, like the double buffering of Chromium (the output of the first two blocks
 * is silent). The <code>playbackTime</code> of the event is the context time at which the output buffer starts to play,
 * <em>(kB + 2B) / sampleRate</em>. A single block of delay would require the listener to answer before the next quantum is rendered, which a
 * listener on another thread can never do.</li>
 * <li><strong>Real-time and offline.</strong> In a real-time context a listener that is late (it has not returned when its output is due) is
 * abandoned: that block of output is silent and the late result is dropped ({@link #getLateBlockCount()}); a listener that did not start yet is
 * not run at all. In an offline context there is no real time to keep, so the rendering waits for the listener (at most
 * {@link #RESPONSE_TIMEOUT_MILLIS} milliseconds, which is then a late block): the result is the same on every run. The wait must not happen
 * on the thread that runs the listener (the rendering thread of an offline context is not that thread).</li>
 * <li><strong>The listener</strong> receives an input buffer (a copy of the block, valid only during the call) and an output buffer (silent at
 * first); what the listener leaves in the output buffer when it returns is played. It runs on the control side if the context has a task sink,
 * on the rendering thread if not. An exception of the listener is caught; the output stays as the listener left it
 * ({@link #getLastListenerError()}). Without a listener the output is silent and no event is requested.</li>
 * <li><strong>Activity.</strong> The node is processed only while it has an input or an output connection (the specification: it is actively
 * processing then), whether or not the other side is connected, so a generator without an input works. Becoming inactive (disconnected) forgets the
 * block in progress and the pending output. The node stays in the context for as long as the context lives.</li>
 * <li><strong>Channels.</strong> <code>numberOfInputChannels</code> and <code>numberOfOutputChannels</code> are from 0 to 32 (not both 0). The
 * input is mixed to <code>numberOfInputChannels</code> channels (<code>channelCount</code>, mode <code>explicit</code>, which cannot be changed, as
 * in the browsers an <code>InvalidStateError</code> where the specification says <code>NotSupportedError</code>). With 0 input channels the input is
 * ignored and the input buffer of the event is null; with 0 output channels the output is a silent channel and the output buffer is null.</li>
 * <li><strong>Memory.</strong> The node keeps three blocks (an input and an output buffer each) and creates at most {@value #MAX_BLOCKS} while the
 * listener is slow; if all are in use the input of that block is dropped ({@link #getDroppedBlockCount()}) and no event is requested.</li>
 * </ul>
 * <p>
 * 스크립트가 오디오를 직접 만들거나 처리하게 하는 폐기 예정 노드(Web Audio API 1.0, 1.29 <code>ScriptProcessorNode</code> 와 1.12
 * <code>AudioProcessingEvent</code>)를 옛 페이지를 위해 구현한 것이다. 클래스는 순수 Java 이다: 입력을 <code>bufferSize</code> 프레임의 블록으로 모으고, 블록마다
 * 컨텍스트의 {@link BaseAudioContext.TaskSink}(페이지의 이벤트 루프)를 통해 제어 쪽에 {@link ProcessListener} 실행을 요청하고, 리스너가 출력 버퍼에 쓴 것을 한 블록 뒤에
 * 재생한다.
 * <ul>
 * <li><strong>블록과 지연.</strong> 블록 <em>k</em>(프레임 <em>kB</em> .. <em>(k+1)B-1</em>)의 입력은 블록이 끝날 때 완성되며 그때 그 이벤트를 요청한다. 리스너에게는
 * 블록 하나만큼의 시간이 응답 시간으로 주어지고, 리스너가 쓴 출력은 블록 <em>k+2</em> 동안 재생된다. 그래서 오디오는 <em>2 bufferSize</em> 프레임 지연되며 Chromium 의
 * 이중 버퍼링과 같다(처음 두 블록의 출력은 무음). 이벤트의 <code>playbackTime</code> 은 출력 버퍼가 재생되기 시작하는 컨텍스트 시간 <em>(kB + 2B) / sampleRate</em> 이다.
 * 지연이 한 블록이라면 다음 quantum 을 렌더링하기 전에 리스너가 응답해야 하는데, 다른 스레드의 리스너는 그럴 수 없다.</li>
 * <li><strong>실시간과 오프라인.</strong> 실시간 컨텍스트에서는 늦은 리스너(출력이 필요한 때까지 돌아오지 않음)를 포기한다: 그 블록의 출력은 무음이고 늦은 결과는 버려지며
 * ({@link #getLateBlockCount()}), 아직 시작하지 않은 리스너는 실행되지 않는다. 오프라인 컨텍스트에는 지켜야 할 실시간이 없으므로 렌더링이 리스너를 기다린다
 * (최대 {@link #RESPONSE_TIMEOUT_MILLIS} 밀리초이며, 넘으면 늦은 블록이다). 결과는 실행할 때마다 같다. 기다림이 리스너를 실행하는 스레드에서 일어나면 안 된다(오프라인
 * 컨텍스트의 렌더링 스레드는 그 스레드가 아니다).</li>
 * <li><strong>리스너</strong>는 입력 버퍼(블록의 복사본이며 호출 동안만 유효)와 출력 버퍼(처음에는 무음)를 받으며, 리스너가 돌아올 때 출력 버퍼에 남긴 것이 재생된다.
 * 컨텍스트에 작업 싱크가 있으면 제어 쪽에서, 없으면 렌더링 스레드에서 실행된다. 리스너의 예외는 잡는다. 출력은 리스너가 남긴 대로이다({@link #getLastListenerError()}).
 * 리스너가 없으면 출력은 무음이고 이벤트를 요청하지 않는다.</li>
 * <li><strong>활성.</strong> 노드는 입력이나 출력 연결이 있는 동안에만 처리된다(규격: 그때 활성으로 처리한다). 반대쪽이 연결되어 있는지는 상관없으므로 입력 없는 생성기도 동작한다.
 * 비활성이 되면(연결이 끊기면) 진행 중인 블록과 대기 중인 출력을 잊는다. 노드는 컨텍스트가 사는 동안 컨텍스트에 남는다.</li>
 * <li><strong>채널.</strong> <code>numberOfInputChannels</code> 와 <code>numberOfOutputChannels</code> 는 0 ~ 32 이다(둘 다 0 은 안 된다). 입력은
 * <code>numberOfInputChannels</code> 채널로 섞이며(<code>channelCount</code>, 모드 <code>explicit</code>, 바꿀 수 없다. 규격은 <code>NotSupportedError</code> 라 하지만 브라우저처럼
 * <code>InvalidStateError</code>), 입력 채널이 0 이면 입력을 무시하고 이벤트의 입력 버퍼는 null 이다. 출력 채널이 0 이면 출력은 무음 채널 하나이고 출력 버퍼는 null 이다.</li>
 * <li><strong>메모리.</strong> 노드는 블록 셋(각각 입력과 출력 버퍼)을 두고 리스너가 느릴 때 최대 {@value #MAX_BLOCKS} 개까지 만든다. 모두 쓰고 있으면 그 블록의 입력을 버리고
 * ({@link #getDroppedBlockCount()}) 이벤트를 요청하지 않는다.</li>
 * </ul>
 */
public final class ScriptProcessorNode extends AudioNode {

    /**
     * The code that the control side runs for a block (the <code>audioprocess</code> event).
     * <p>
     * 제어 쪽이 블록마다 실행하는 코드(<code>audioprocess</code> 이벤트).
     */
    public interface ProcessListener {

        /**
         * Processes one block: reads the input and writes the output; what the output buffer holds when the method returns is played.
         * <p>
         * 블록 하나를 처리한다: 입력을 읽고 출력을 쓴다. 메서드가 돌아올 때 출력 버퍼에 있는 것이 재생된다.
         *
         * @param inputBuffer  the block of the input, with <code>numberOfInputChannels</code> channels, or null if the node has none; valid only
         *                     during the call / 입력의 블록(<code>numberOfInputChannels</code> 채널). 없으면 null. 호출 동안만 유효
         * @param outputBuffer the buffer to fill, silent at first, with <code>numberOfOutputChannels</code> channels, or null if the node has none /
         *                     채울 버퍼. 처음에는 무음이며 <code>numberOfOutputChannels</code> 채널. 없으면 null
         * @param playbackTime the context time in seconds at which the output buffer starts to play / 출력 버퍼가 재생되기 시작하는 컨텍스트 시간(초)
         */
        void process(AudioBuffer inputBuffer, AudioBuffer outputBuffer, double playbackTime);
    }

    /**
     * The smallest <code>bufferSize</code>.
     * <p>
     * <code>bufferSize</code> 의 최솟값.
     */
    public static final int MIN_BUFFER_SIZE = 256;

    /**
     * The largest <code>bufferSize</code>.
     * <p>
     * <code>bufferSize</code> 의 최댓값.
     */
    public static final int MAX_BUFFER_SIZE = 16384;

    /**
     * The <code>bufferSize</code> that the implementation picks for 0 (46 ms at 44100 Hz).
     * <p>
     * 0 일 때 구현이 고르는 <code>bufferSize</code>(44100 Hz 에서 46 ms).
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    /**
     * The longest time in milliseconds that an offline rendering waits for a listener.
     * <p>
     * 오프라인 렌더링이 리스너를 기다리는 가장 긴 시간(밀리초).
     */
    public static final long RESPONSE_TIMEOUT_MILLIS = 10000L;

    /**
     * The largest number of blocks that exist at the same time.
     * <p>
     * 동시에 존재하는 블록의 최대 수.
     */
    public static final int MAX_BLOCKS = 8;

    /**
     * The number of blocks that are created with the node.
     * <p>
     * 노드와 함께 만드는 블록 수.
     */
    private static final int INITIAL_BLOCKS = 3;

    /**
     * One block: the input that is collected, and the output that the listener fills. The block is recycled when the rendering side and the
     * listener have both released it.
     * <p>
     * 블록 하나: 모으는 입력과 리스너가 채우는 출력. 렌더링 쪽과 리스너가 모두 놓아 주면 재활용한다.
     */
    private static final class Block {

        /**
         * The input of the block, or null without input channels.
         * <p>
         * 블록의 입력. 입력 채널이 없으면 null.
         */
        final AudioBuffer in;

        /**
         * The output of the block, or null without output channels.
         * <p>
         * 블록의 출력. 출력 채널이 없으면 null.
         */
        final AudioBuffer out;

        /**
         * The first frame of the block (rendering thread).
         * <p>
         * 블록의 첫 프레임(렌더링 스레드).
         */
        long startFrame;

        /**
         * The number of parties that still use the block: the rendering side and the listener (guarded by the pool lock).
         * <p>
         * 블록을 아직 쓰는 쪽의 수: 렌더링 쪽과 리스너(풀 잠금으로 보호).
         */
        int refs;

        /**
         * True when the listener has returned, or no listener will run (guarded by the block).
         * <p>
         * 리스너가 돌아왔거나 실행될 리스너가 없으면 true(블록으로 보호).
         */
        boolean done;

        /**
         * True when the rendering side gave up on the block: a listener that did not start yet must not run (guarded by the block).
         * <p>
         * 렌더링 쪽이 블록을 포기했으면 true. 아직 시작하지 않은 리스너는 실행하면 안 된다(블록으로 보호).
         */
        boolean abandoned;

        /**
         * Creates a block.
         * <p>
         * 블록을 만든다.
         *
         * @param in  the input buffer or null / 입력 버퍼 또는 null
         * @param out the output buffer or null / 출력 버퍼 또는 null
         */
        Block(AudioBuffer in, AudioBuffer out) {
            this.in = in;
            this.out = out;
        }
    }

    /**
     * The number of frames of a block.
     * <p>
     * 블록의 프레임 수.
     */
    private final int bufferSize;

    /**
     * The number of input channels (0 for none).
     * <p>
     * 입력 채널 수(없으면 0).
     */
    private final int inputChannels;

    /**
     * The number of output channels (0 for none).
     * <p>
     * 출력 채널 수(없으면 0).
     */
    private final int outputChannels;

    /**
     * The longest time in milliseconds that the rendering waits for a listener.
     * <p>
     * 렌더링이 리스너를 기다리는 가장 긴 시간(밀리초).
     */
    private volatile long responseTimeout = RESPONSE_TIMEOUT_MILLIS;

    /**
     * True if the rendering waits for the listener (an offline context).
     * <p>
     * 렌더링이 리스너를 기다리면(오프라인 컨텍스트) true.
     */
    private final boolean waitForResponse;

    /**
     * The listener of the blocks.
     * <p>
     * 블록의 리스너.
     */
    private volatile ProcessListener listener;

    /**
     * Guards the pool of blocks and the counts of the blocks.
     * <p>
     * 블록의 풀과 블록 수를 지킨다.
     */
    private final Object poolLock = new Object();

    /**
     * The blocks that nobody uses (guarded by the pool lock).
     * <p>
     * 아무도 쓰지 않는 블록(풀 잠금으로 보호).
     */
    private final ArrayList<Block> free = new ArrayList<Block>();

    /**
     * The number of blocks that were created (guarded by the pool lock).
     * <p>
     * 만든 블록의 수(풀 잠금으로 보호).
     */
    private int created;

    /**
     * The block that collects the input (rendering thread).
     * <p>
     * 입력을 모으는 블록(렌더링 스레드).
     */
    private Block collecting;

    /**
     * True while a block is open: from its first frame to its last (rendering thread).
     * <p>
     * 블록이 열려 있는 동안 true: 첫 프레임부터 마지막 프레임까지(렌더링 스레드).
     */
    private boolean blockOpen;

    /**
     * The number of frames of the open block that were collected (rendering thread).
     * <p>
     * 열린 블록에서 모은 프레임 수(렌더링 스레드).
     */
    private int index;

    /**
     * The block whose event was requested at the last block boundary; its output plays from the next boundary (rendering thread).
     * <p>
     * 지난 블록 경계에서 이벤트를 요청한 블록. 그 출력은 다음 경계부터 재생된다(렌더링 스레드).
     */
    private Block inFlight;

    /**
     * The block whose output is playing now (rendering thread).
     * <p>
     * 지금 출력이 재생되는 블록(렌더링 스레드).
     */
    private Block playing;

    /**
     * The number of blocks that were late.
     * <p>
     * 늦은 블록의 수.
     */
    private volatile int lateBlocks;

    /**
     * The number of blocks whose input was dropped because no block was free.
     * <p>
     * 빈 블록이 없어 입력을 버린 블록의 수.
     */
    private volatile int droppedBlocks;

    /**
     * The number of events that were requested.
     * <p>
     * 요청한 이벤트의 수.
     */
    private volatile int dispatchedBlocks;

    /**
     * The last exception of the listener.
     * <p>
     * 리스너의 마지막 예외.
     */
    private volatile Throwable lastListenerError;

    /**
     * Creates a script processor.
     * <p>
     * 스크립트 프로세서를 만든다.
     *
     * @param context             the context / 컨텍스트
     * @param bufferSize          0 to let the implementation pick, or a power of two from 256 to 16384 / 0 이면 구현이 고르고, 아니면 256 ~ 16384 의 2 의 거듭제곱
     * @param numberOfInputChannels  the number of input channels, from 0 to 32 / 입력 채널 수(0 ~ 32)
     * @param numberOfOutputChannels the number of output channels, from 0 to 32; not both 0 / 출력 채널 수(0 ~ 32). 둘 다 0 은 안 된다
     * @throws AudioException <code>IndexSizeError</code> if a value is out of range / 값이 범위 밖이면 <code>IndexSizeError</code>
     */
    public ScriptProcessorNode(BaseAudioContext context, int bufferSize, int numberOfInputChannels, int numberOfOutputChannels) {
        super(context, 1, 1, Math.max(1, checkChannels(numberOfInputChannels, numberOfOutputChannels)), ChannelCountMode.EXPLICIT, ChannelInterpretation.SPEAKERS);
        this.bufferSize = checkBufferSize(bufferSize);
        this.inputChannels = numberOfInputChannels;
        this.outputChannels = numberOfOutputChannels;
        this.waitForResponse = context instanceof OfflineAudioContext;
        // 블록 셋을 미리 만들어 렌더링 중에 할당하지 않게 한다.
        for (int i = 0; i < INITIAL_BLOCKS; i++) {
            free.add(newBlock());
        }
        created = INITIAL_BLOCKS;
        // 컨텍스트가 이 노드를 계속 처리 대상에 넣게(입력 없는 생성기도 도달할 수 있게) 붙잡는다.
        context.post(new Runnable() {
            public void run() {
                getContext().pin(ScriptProcessorNode.this);
            }
        });
    }

    /**
     * Checks the numbers of channels and returns the number of input channels.
     * <p>
     * 채널 수를 검사하고 입력 채널 수를 반환한다.
     *
     * @param in  the input channels / 입력 채널 수
     * @param out the output channels / 출력 채널 수
     * @return the number of input channels / 입력 채널 수
     */
    private static int checkChannels(int in, int out) {
        if (in < 0 || in > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "numberOfInputChannels " + in + " is not in 0.." + WebAudioLimits.MAX_CHANNELS);
        }
        if (out < 0 || out > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "numberOfOutputChannels " + out + " is not in 0.." + WebAudioLimits.MAX_CHANNELS);
        }
        if (in == 0 && out == 0) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "numberOfInputChannels and numberOfOutputChannels cannot both be 0");
        }
        return in;
    }

    /**
     * Checks the buffer size.
     * <p>
     * 버퍼 크기를 검사한다.
     *
     * @param size the buffer size / 버퍼 크기
     * @return the size, {@link #DEFAULT_BUFFER_SIZE} for 0 / 크기. 0 이면 {@link #DEFAULT_BUFFER_SIZE}
     */
    private static int checkBufferSize(int size) {
        if (size == 0) {
            return DEFAULT_BUFFER_SIZE;
        }
        if (size < MIN_BUFFER_SIZE || size > MAX_BUFFER_SIZE || (size & (size - 1)) != 0) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "bufferSize " + size + " is not 0 or a power of two from " + MIN_BUFFER_SIZE + " to " + MAX_BUFFER_SIZE);
        }
        return size;
    }

    /**
     * Creates a block with the buffers of this node.
     * <p>
     * 이 노드의 버퍼를 가진 블록을 만든다.
     *
     * @return the block / 블록
     */
    private Block newBlock() {
        float rate = getContext().getSampleRate();
        return new Block(inputChannels > 0 ? new AudioBuffer(inputChannels, bufferSize, rate) : null,
                outputChannels > 0 ? new AudioBuffer(outputChannels, bufferSize, rate) : null);
    }

    /**
     * Returns the number of frames of a block (<code>bufferSize</code>).
     * <p>
     * 블록의 프레임 수(<code>bufferSize</code>)를 반환한다.
     *
     * @return the buffer size / 버퍼 크기
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Returns the number of input channels the node was created with (0 for none).
     * <p>
     * 노드를 만들 때 정한 입력 채널 수를 반환한다(없으면 0).
     *
     * @return the number of channels / 채널 수
     */
    public int getNumberOfInputChannels() {
        return inputChannels;
    }

    /**
     * Returns the number of output channels the node was created with (0 for none).
     * <p>
     * 노드를 만들 때 정한 출력 채널 수를 반환한다(없으면 0).
     *
     * @return the number of channels / 채널 수
     */
    public int getNumberOfOutputChannels() {
        return outputChannels;
    }

    /**
     * Sets the listener of the blocks (the <code>onaudioprocess</code> handler); null stops the events and the output is silent.
     * <p>
     * 블록의 리스너(<code>onaudioprocess</code> 핸들러)를 정한다. null 이면 이벤트가 멈추고 출력은 무음이다.
     *
     * @param newListener the listener or null / 리스너 또는 null
     */
    public void setProcessListener(ProcessListener newListener) {
        this.listener = newListener;
    }

    /**
     * Returns the listener of the blocks.
     * <p>
     * 블록의 리스너를 반환한다.
     *
     * @return the listener or null / 리스너 또는 null
     */
    public ProcessListener getProcessListener() {
        return listener;
    }

    /**
     * Sets the longest time in milliseconds that an offline rendering waits for a listener (the default is {@link #RESPONSE_TIMEOUT_MILLIS}).
     * <p>
     * 오프라인 렌더링이 리스너를 기다리는 가장 긴 시간(밀리초)을 정한다(기본값은 {@link #RESPONSE_TIMEOUT_MILLIS}).
     *
     * @param millis the time in milliseconds, at least 1 / 시간(밀리초, 1 이상)
     */
    public void setResponseTimeout(long millis) {
        this.responseTimeout = Math.max(1L, millis);
    }

    /**
     * Returns the longest time in milliseconds that an offline rendering waits for a listener.
     * <p>
     * 오프라인 렌더링이 리스너를 기다리는 가장 긴 시간(밀리초)을 반환한다.
     *
     * @return the time in milliseconds / 시간(밀리초)
     */
    public long getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * Returns the number of blocks that exist (the blocks that are created with the node, and those that were added while the listener was slow).
     * <p>
     * 존재하는 블록의 수를 반환한다(노드와 함께 만든 블록과 리스너가 느린 동안 더한 블록).
     *
     * @return the number of blocks / 블록 수
     */
    public int getBlockCount() {
        synchronized (poolLock) {
            return created;
        }
    }

    /**
     * Returns the number of blocks whose output was late (silent, the result dropped).
     * <p>
     * 출력이 늦었던(무음이고 결과를 버린) 블록의 수를 반환한다.
     *
     * @return the count / 수
     */
    public int getLateBlockCount() {
        return lateBlocks;
    }

    /**
     * Returns the number of blocks whose input was dropped because the listener was so slow that no block was free.
     * <p>
     * 리스너가 너무 느려 빈 블록이 없어서 입력을 버린 블록의 수를 반환한다.
     *
     * @return the count / 수
     */
    public int getDroppedBlockCount() {
        return droppedBlocks;
    }

    /**
     * Returns the number of events that were requested (one per block with a listener).
     * <p>
     * 요청한 이벤트의 수를 반환한다(리스너가 있는 블록마다 하나).
     *
     * @return the count / 수
     */
    public int getDispatchedBlockCount() {
        return dispatchedBlocks;
    }

    /**
     * Returns the last exception that the listener threw, or null.
     * <p>
     * 리스너가 마지막으로 던진 예외를 반환한다. 없으면 null.
     *
     * @return the exception or null / 예외 또는 null
     */
    public Throwable getLastListenerError() {
        return lastListenerError;
    }

    /**
     * Rejects a <code>channelCount</code> that is not the number of input channels.
     * <p>
     * 입력 채널 수가 아닌 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count != getChannelCount()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCount of a ScriptProcessorNode cannot be changed");
        }
    }

    /**
     * Rejects a mode other than <code>explicit</code>.
     * <p>
     * <code>explicit</code> 가 아닌 모드를 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode != ChannelCountMode.EXPLICIT) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "the channelCountMode of a ScriptProcessorNode cannot be changed");
        }
    }

    /**
     * Tells that the node is processed while its input or its output is connected.
     * <p>
     * 입력이나 출력이 연결되어 있는 동안 노드를 처리한다고 알려 준다.
     *
     * @return true if a connection exists / 연결이 있으면 true
     */
    @Override
    boolean alwaysActive() {
        AudioNodeOutput out = outputs[0];
        return !inputs[0].sources.isEmpty() || !out.inputTargets.isEmpty() || !out.paramTargets.isEmpty();
    }

    /**
     * Takes a block from the pool, or creates one if fewer than {@link #MAX_BLOCKS} exist; null if all are in use. The block is
     * used by the rendering side and by the listener.
     * <p>
     * 풀에서 블록을 꺼낸다. {@link #MAX_BLOCKS} 개 미만이면 새로 만든다. 모두 쓰고 있으면 null. 블록은 렌더링 쪽과 리스너가 쓴다.
     *
     * @return the block or null / 블록 또는 null
     */
    private Block acquire() {
        Block b = null;
        boolean make = false;
        synchronized (poolLock) {
            if (!free.isEmpty()) {
                b = free.remove(free.size() - 1);
            } else if (created < MAX_BLOCKS) {
                created++;
                make = true;
            }
            if (b != null) {
                b.refs = 2;
            }
        }
        if (make) {
            b = newBlock();
            synchronized (poolLock) {
                b.refs = 2;
            }
        }
        if (b == null) {
            return null;
        }
        b.done = false;
        b.abandoned = false;
        // 출력은 처음에 무음이다.
        if (b.out != null) {
            for (int c = 0; c < outputChannels; c++) {
                java.util.Arrays.fill(b.out.getChannelData(c), 0f);
            }
        }
        return b;
    }

    /**
     * Releases one use of a block; a block that nobody uses returns to the pool.
     * <p>
     * 블록의 사용 하나를 놓는다. 아무도 쓰지 않는 블록은 풀로 돌아간다.
     *
     * @param b the block or null / 블록 또는 null
     */
    private void release(Block b) {
        if (b == null) {
            return;
        }
        synchronized (poolLock) {
            b.refs--;
            if (b.refs == 0) {
                free.add(b);
            }
        }
    }

    /**
     * Processes one render quantum: collects the input into the open block, copies the playing output, and at the end of a block requests the
     * event and moves the blocks on.
     * <p>
     * render quantum 하나를 처리한다: 입력을 열린 블록에 모으고, 재생 중인 출력을 복사하고, 블록의 끝에서 이벤트를 요청하고 블록을 넘긴다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        out.setNumberOfChannels(Math.max(1, outputChannels));
        out.zero();
        int pos = 0;
        // quantum 의 프레임을 블록 경계까지 나누어 처리한다.
        while (pos < frames) {
            // 새 블록이 시작되면 블록 하나를 얻는다(없으면 이 블록의 입력은 버린다).
            if (!blockOpen) {
                blockOpen = true;
                index = 0;
                collecting = acquire();
                if (collecting == null) {
                    droppedBlocks++;
                } else {
                    collecting.startFrame = renderFrame0 + pos;
                }
            }
            int n = Math.min(frames - pos, bufferSize - index);
            // 입력 채널을 블록에 모은다.
            if (collecting != null && inputChannels > 0) {
                for (int c = 0; c < inputChannels; c++) {
                    System.arraycopy(in.channel(c), pos, collecting.in.getChannelData(c), index, n);
                }
            }
            // 재생 중인 블록의 출력을 노드의 출력으로 복사한다.
            if (playing != null && outputChannels > 0) {
                for (int c = 0; c < outputChannels; c++) {
                    System.arraycopy(playing.out.getChannelData(c), index, out.channel(c), pos, n);
                }
            }
            index += n;
            pos += n;
            if (index == bufferSize) {
                endBlock();
                blockOpen = false;
            }
        }
    }

    /**
     * Ends the open block: requests its event, and decides what plays during the next block, which is the output of the block whose event was
     * requested one boundary ago.
     * <p>
     * 열린 블록을 끝낸다: 그 이벤트를 요청하고, 다음 블록 동안 재생할 것을 정한다. 그것은 한 경계 전에 이벤트를 요청한 블록의 출력이다.
     */
    private void endBlock() {
        Block finished = collecting;
        collecting = null;
        Block next = inFlight;
        Block previous = playing;
        if (finished != null) {
            dispatch(finished);
        }
        inFlight = finished;
        Block toPlay = null;
        // 한 경계 전에 요청한 블록의 응답이 왔으면 재생하고, 오지 않았으면 포기하고 무음으로 둔다.
        if (next != null) {
            if (awaitReady(next)) {
                toPlay = next;
            } else {
                synchronized (next) {
                    next.abandoned = true;
                }
                lateBlocks++;
                release(next);
            }
        }
        release(previous);
        playing = toPlay;
    }

    /**
     * Requests the event of a finished block on the control side.
     * <p>
     * 끝난 블록의 이벤트를 제어 쪽에 요청한다.
     *
     * @param block the block / 블록
     */
    private void dispatch(final Block block) {
        final ProcessListener l = listener;
        // 리스너가 없으면 이벤트를 요청하지 않으며 출력은 무음이다.
        if (l == null) {
            synchronized (block) {
                block.done = true;
            }
            release(block);
            return;
        }
        dispatchedBlocks++;
        final double playbackTime = (block.startFrame + 2L * bufferSize) / (double) getContext().getSampleRate();
        getContext().postTask(new Runnable() {
            /**
             * Runs the listener for the block unless the block was given up.
             * <p>
             * 블록을 포기하지 않았으면 리스너를 실행한다.
             */
            public void run() {
                boolean go;
                synchronized (block) {
                    go = !block.abandoned;
                }
                if (go) {
                    try {
                        l.process(block.in, block.out, playbackTime);
                    } catch (RuntimeException e) {
                        // 리스너의 예외는 렌더링에 영향을 주지 않는다. 출력은 리스너가 남긴 대로이다.
                        lastListenerError = e;
                    }
                }
                synchronized (block) {
                    block.done = true;
                    block.notifyAll();
                }
                release(block);
            }
        });
    }

    /**
     * Tells whether the listener of a block has returned; an offline rendering waits for it.
     * <p>
     * 블록의 리스너가 돌아왔는지 알려 준다. 오프라인 렌더링은 기다린다.
     *
     * @param b the block / 블록
     * @return true if the output of the block is ready / 블록의 출력이 준비되었으면 true
     */
    private boolean awaitReady(Block b) {
        synchronized (b) {
            if (b.done) {
                return true;
            }
            // 실시간에서는 기다리지 않는다. 늦었으면 무음이다.
            if (!waitForResponse) {
                return false;
            }
            long end = System.nanoTime() + responseTimeout * 1000000L;
            // 오프라인에서는 리스너가 끝날 때까지(최대 제한 시간) 기다린다.
            while (!b.done) {
                long left = end - System.nanoTime();
                if (left <= 0) {
                    return false;
                }
                try {
                    b.wait(left / 1000000L + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Forgets the block in progress and the pending output (the node became inactive).
     * <p>
     * 진행 중인 블록과 대기 중인 출력을 잊는다(노드가 비활성이 되었다).
     */
    @Override
    void resetState() {
        // 이벤트를 요청하지 않은 블록은 리스너의 몫까지 두 번 놓는다.
        if (collecting != null) {
            synchronized (collecting) {
                collecting.done = true;
            }
            release(collecting);
            release(collecting);
        }
        // 요청했지만 재생하지 않은 블록은 포기하고 놓는다.
        if (inFlight != null) {
            synchronized (inFlight) {
                inFlight.abandoned = true;
            }
            release(inFlight);
        }
        release(playing);
        collecting = null;
        inFlight = null;
        playing = null;
        blockOpen = false;
        index = 0;
    }
}
