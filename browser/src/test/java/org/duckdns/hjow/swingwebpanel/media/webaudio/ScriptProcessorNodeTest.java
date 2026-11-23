package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script processor node (plan item P25-07, decision c: the deprecated node is implemented for old pages), in an offline context, in
 * a real-time context whose rendering the test drives itself (so lateness is exact and does not depend on the load of the machine), and with the
 * output driver. What is checked: the delay of two blocks and the <code>playbackTime</code> of every event, the input that the listener sees (the
 * channels, and null without input channels), a generator without an input, a listener on another thread that the offline rendering waits for
 * (the result equals the one of a listener that runs inline) and its time limit, a late listener in a real-time context (silence, the late result
 * is dropped, a task that did not start does not run), the blocks that are recycled and the limit of their number, an exception of the listener, a
 * node that is not connected (no events) and that forgets its state when it is disconnected, the validation of the arguments and the constraints
 * of the channel attributes, and that the listener runs only on the rendering thread (never twice at the same time) when there is no task sink.
 * <p>
 * 스크립트 프로세서 노드의 시험(계획 P25-07, 결정 c: 폐기 예정 노드를 옛 페이지를 위해 구현한다). 오프라인 컨텍스트, 시험이 렌더링을 직접 구동하는 실시간 컨텍스트(그래서
 * 늦음이 정확하고 컴퓨터의 부하에 의존하지 않는다), 출력 드라이버로 한다. 확인하는 것: 두 블록의 지연과 모든 이벤트의 <code>playbackTime</code>, 리스너가 보는 입력(채널,
 * 입력 채널이 없으면 null), 입력 없는 생성기, 다른 스레드의 리스너를 오프라인 렌더링이 기다리는 것(결과가 인라인으로 실행하는 리스너의 결과와 같다)과 그 제한 시간, 실시간
 * 컨텍스트에서 늦은 리스너(무음, 늦은 결과는 버려지고 시작하지 않은 작업은 실행되지 않는다), 재활용하는 블록과 그 수의 제한, 리스너의 예외, 연결되지 않은 노드(이벤트 없음)와
 * 연결이 끊기면 상태를 잊는 것, 인수의 검증과 채널 속성의 제약, 작업 싱크가 없을 때 리스너가 렌더링 스레드에서만(동시에 둘이 아니게) 실행되는 것.
 */
public class ScriptProcessorNodeTest {

    /**
     * The sample rate of the tests.
     * <p>
     * 시험의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * A driver that does nothing: the test calls the renderer itself, so the rendering is exact.
     * <p>
     * 아무것도 하지 않는 드라이버: 시험이 렌더러를 직접 불러 렌더링이 정확하다.
     */
    private static final class ManualDriver implements RealtimeAudioContext.Driver {

        /**
         * The renderer of the context.
         * <p>
         * 컨텍스트의 렌더러.
         */
        RealtimeAudioContext.Renderer renderer;

        /**
         * Keeps the renderer.
         * <p>
         * 렌더러를 보관한다.
         *
         * @param r the renderer / 렌더러
         */
        public void start(RealtimeAudioContext.Renderer r) {
            renderer = r;
        }

        /**
         * Does nothing.
         * <p>
         * 아무것도 하지 않는다.
         */
        public void pause() {
        }

        /**
         * Does nothing.
         * <p>
         * 아무것도 하지 않는다.
         */
        public void close() {
        }
    }

    /**
     * A task sink that keeps the tasks until the test runs or drops them.
     * <p>
     * 시험이 실행하거나 버릴 때까지 작업을 보관하는 작업 싱크.
     */
    private static final class QueueSink implements BaseAudioContext.TaskSink {

        /**
         * The tasks that wait.
         * <p>
         * 기다리는 작업.
         */
        private final ArrayList<Runnable> tasks = new ArrayList<Runnable>();

        /**
         * Keeps a task.
         * <p>
         * 작업을 보관한다.
         *
         * @param task the task / 작업
         */
        public void post(Runnable task) {
            synchronized (tasks) {
                tasks.add(task);
            }
        }

        /**
         * Returns the number of tasks that wait.
         * <p>
         * 기다리는 작업의 수를 반환한다.
         *
         * @return the number / 수
         */
        int size() {
            synchronized (tasks) {
                return tasks.size();
            }
        }

        /**
         * Runs the tasks that wait, in order.
         * <p>
         * 기다리는 작업을 순서대로 실행한다.
         */
        void runAll() {
            ArrayList<Runnable> copy;
            synchronized (tasks) {
                copy = new ArrayList<Runnable>(tasks);
                tasks.clear();
            }
            // 보관한 작업을 순서대로 실행한다.
            for (int i = 0; i < copy.size(); i++) {
                copy.get(i).run();
            }
        }
    }

    /**
     * A task sink with a thread of its own (the control side), which can delay every task.
     * <p>
     * 자기 스레드(제어 쪽)를 가진 작업 싱크. 모든 작업을 지연시킬 수 있다.
     */
    private static final class ThreadSink implements BaseAudioContext.TaskSink, Runnable {

        /**
         * The tasks that wait.
         * <p>
         * 기다리는 작업.
         */
        private final ArrayList<Runnable> tasks = new ArrayList<Runnable>();

        /**
         * The delay of every task in milliseconds.
         * <p>
         * 모든 작업의 지연(밀리초).
         */
        private final long delay;

        /**
         * True if the tasks are thrown away.
         * <p>
         * 작업을 버리면 true.
         */
        private final boolean discard;

        /**
         * The thread.
         * <p>
         * 스레드.
         */
        final Thread thread;

        /**
         * Creates the sink and starts its thread.
         * <p>
         * 싱크를 만들고 스레드를 시작한다.
         *
         * @param delay   the delay of every task in milliseconds / 모든 작업의 지연(밀리초)
         * @param discard true to throw the tasks away / 작업을 버리려면 true
         */
        ThreadSink(long delay, boolean discard) {
            this.delay = delay;
            this.discard = discard;
            thread = new Thread(this, "swingwebpanel-test-control");
            thread.setDaemon(true);
            thread.start();
        }

        /**
         * Queues a task for the thread.
         * <p>
         * 스레드에 작업을 쌓는다.
         *
         * @param task the task / 작업
         */
        public void post(Runnable task) {
            synchronized (tasks) {
                if (!discard) {
                    tasks.add(task);
                }
                tasks.notifyAll();
            }
        }

        /**
         * Runs the tasks in order, each after the delay.
         * <p>
         * 작업을 순서대로, 각각 지연 뒤에 실행한다.
         */
        public void run() {
            // 작업이 올 때마다 지연한 뒤 실행한다.
            for (;;) {
                Runnable task;
                synchronized (tasks) {
                    // 작업이 올 때까지 기다린다.
                    while (tasks.isEmpty()) {
                        try {
                            tasks.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    task = tasks.remove(0);
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
                task.run();
            }
        }
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ScriptProcessorNodeTest() {
    }

    /**
     * Copies the input of a block to the output channel by channel (the output has at most as many channels as the input).
     * <p>
     * 블록의 입력을 채널별로 출력에 복사한다(출력의 채널은 입력보다 많지 않다).
     */
    private static final class Identity implements ScriptProcessorNode.ProcessListener {

        /**
         * The <code>playbackTime</code> of every event.
         * <p>
         * 모든 이벤트의 <code>playbackTime</code>.
         */
        final ArrayList<Double> times = new ArrayList<Double>();

        /**
         * The names of the threads that ran the listener.
         * <p>
         * 리스너를 실행한 스레드의 이름.
         */
        final Set<String> threads = new TreeSet<String>();

        /**
         * Copies the input to the output.
         * <p>
         * 입력을 출력에 복사한다.
         *
         * @param in   the input / 입력
         * @param out  the output / 출력
         * @param time the playback time / 재생 시간
         */
        public void process(AudioBuffer in, AudioBuffer out, double time) {
            synchronized (this) {
                times.add(Double.valueOf(time));
                threads.add(Thread.currentThread().getName());
            }
            // 출력 채널마다 같은 번호의 입력 채널을 복사한다.
            for (int c = 0; c < out.getNumberOfChannels(); c++) {
                System.arraycopy(in.getChannelData(c), 0, out.getChannelData(c), 0, in.getLength());
            }
        }
    }

    /**
     * The delay is exactly two blocks, the output of the first two blocks is silent, and the <code>playbackTime</code> of the event of block
     * <em>k</em> is <em>(256 k + 512) / sampleRate</em>: a ramp that is passed through an identity listener comes out as the same ramp 512 frames
     * later, and 16 events are requested for 4096 frames (one per block).
     * <p>
     * 지연은 정확히 두 블록이고 처음 두 블록의 출력은 무음이며, 블록 <em>k</em> 의 이벤트의 <code>playbackTime</code> 은 <em>(256 k + 512) / 샘플링 주파수</em> 이다: 항등 리스너를 거친 램프는 같은 램프로 512 프레임 뒤에 나오고, 4096 프레임에 이벤트 16개(블록마다 하나)를 요청한다.
     */
    public void testTheDelayIsTwoBlocks() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(4096);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(1, 2000, RATE));
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        Identity id = new Identity();
        spn.setProcessListener(id);
        src.connect(spn);
        spn.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("the first two blocks are silent", out, 0, 512);
        WebAudioTestKit.assertSignal("the ramp, 512 frames later", out, 512, 2512, new WebAudioTestKit.Signal() {
            public double at(int n) {
                return (n - 512) / 1000.0;
            }
        }, 1e-6);
        WebAudioTestKit.assertSilent("the silence after the ramp", out, 2600, 4096);
        Check.equal(16L, (long) spn.getDispatchedBlockCount(), "one event per block");
        Check.equal(16L, (long) id.times.size(), "the listener ran 16 times");
        // 모든 이벤트의 playbackTime 이 (256 k + 512) / 샘플링 주파수인지 확인한다.
        for (int k = 0; k < 16; k++) {
            Check.near((256.0 * k + 512) / RATE, id.times.get(k).doubleValue(), 1e-12, "playbackTime of block " + k);
        }
        Check.equal(0L, (long) spn.getLateBlockCount(), "nothing was late");
        Check.equal(3L, (long) spn.getBlockCount(), "the three blocks are recycled");
        Check.equal(256L, (long) spn.getBufferSize(), "bufferSize");
    }

    /**
     * The listener sees the channels of the input (an input that is mixed to 2 channels, and a mono output made of their sum), and a block that
     * is not a multiple of the quantum (512 is 4 quanta) works the same.
     * <p>
     * 리스너는 입력의 채널(2채널로 섞인 입력과 그 합으로 만든 모노 출력)을 보며, quantum 의 배수가 아닌 블록(512 는 quantum 4개)도 같게 동작한다.
     */
    public void testTheListenerSeesTheInputChannels() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(8192);
        AudioBuffer stereo = new AudioBuffer(2, 8192, RATE);
        java.util.Arrays.fill(stereo.getChannelData(0), 0.25f);
        java.util.Arrays.fill(stereo.getChannelData(1), 0.5f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo);
        ScriptProcessorNode spn = ctx.createScriptProcessor(512, 2, 1);
        final int[] shapes = new int[3];
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double time) {
                shapes[0] = in.getNumberOfChannels();
                shapes[1] = out.getNumberOfChannels();
                shapes[2] = in.getLength();
                // 두 입력 채널의 합을 출력에 쓴다.
                for (int i = 0; i < in.getLength(); i++) {
                    out.getChannelData(0)[i] = in.getChannelData(0)[i] + in.getChannelData(1)[i];
                }
            }
        });
        src.connect(spn);
        spn.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.equal(2L, (long) shapes[0], "two input channels");
        Check.equal(1L, (long) shapes[1], "one output channel");
        Check.equal(512L, (long) shapes[2], "the block length");
        WebAudioTestKit.assertSilent("silent for two blocks", out, 0, 1024);
        WebAudioTestKit.assertSignal("the sum", out, 1024, 8192, new WebAudioTestKit.Signal() {
            public double at(int n) {
                return 0.75;
            }
        }, 1e-7);
    }

    /**
     * A generator without an input (0 input channels): the input buffer of the event is null, the node works although nothing is connected to
     * its input, and a sine that the listener computes from the <code>playbackTime</code> is heard as the sine of the context time from the third
     * block on (so <code>playbackTime</code> is the time at which the output buffer plays).
     * <p>
     * 입력이 없는(입력 채널 0) 생성기: 이벤트의 입력 버퍼는 null 이고, 입력에 아무것도 연결되지 않아도 노드는 동작하며, 리스너가 <code>playbackTime</code> 으로 계산한 사인은 셋째 블록부터 컨텍스트 시간의 사인으로 들린다(그러므로 <code>playbackTime</code> 은 출력 버퍼가 재생되는 시간이다).
     */
    public void testAGeneratorWithoutAnInput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(4096);
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 0, 1);
        final boolean[] inputWasNull = {true};
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double playbackTime) {
                inputWasNull[0] &= in == null;
                // 재생 시간부터 이어지는 440 Hz 사인을 계산한다.
                for (int i = 0; i < out.getLength(); i++) {
                    out.getChannelData(0)[i] = (float) Math.sin(2 * Math.PI * 440.0 * (playbackTime + i / (double) RATE));
                }
            }
        });
        spn.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        Check.isTrue(inputWasNull[0], "the input buffer is null without input channels");
        WebAudioTestKit.assertSilent("the first two blocks", out, 0, 512);
        WebAudioTestKit.assertSignal("the sine of the context time", out, 512, 4096, new WebAudioTestKit.Signal() {
            public double at(int n) {
                return Math.sin(2 * Math.PI * 440.0 * n / RATE);
            }
        }, 2e-6);
    }

    /**
     * A node with an input but no output channels (an analyser): the output buffer is null, the node is processed because its input is
     * connected, and the listener sees the signal.
     * <p>
     * 입력은 있고 출력 채널은 없는 노드(분석기): 출력 버퍼는 null 이고, 입력이 연결되어 있으므로 노드는 처리되며, 리스너는 신호를 본다.
     */
    public void testAnAnalyserWithoutAnOutput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(2048);
        ConstantSourceNode src = ctx.createConstantSource();
        src.getOffset().setValue(0.5f);
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 0);
        final double[] sum = new double[2];
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double playbackTime) {
                sum[1] += out == null ? 1 : 1000;
                // 블록의 평균을 모은다.
                for (int i = 0; i < in.getLength(); i++) {
                    sum[0] += in.getChannelData(0)[i];
                }
            }
        });
        src.connect(spn);
        src.start(0);
        WebAudioTestKit.render(ctx);
        Check.equal(8L, (long) spn.getDispatchedBlockCount(), "8 blocks of 256 in 2048 frames");
        Check.near(0.5 * 2048, sum[0], 1e-3, "the listener saw the signal in every block");
        Check.near(8.0, sum[1], 0, "the output buffer is null");
    }

    /**
     * A listener on the control side (another thread, each task delayed by 3 ms) gives exactly the result of an inline listener, because the
     * offline rendering waits for it; the listener runs on that thread, and nothing is late.
     * <p>
     * 제어 쪽(다른 스레드, 작업마다 3 ms 지연)의 리스너는 인라인 리스너와 정확히 같은 결과를 준다. 오프라인 렌더링이 그것을 기다리기 때문이다. 리스너는 그 스레드에서 실행되며 늦은 것은 없다.
     */
    public void testOfflineRenderingWaitsForAListenerOnAnotherThread() {
        float[] inline = renderRamp(null);
        ThreadSink sink = new ThreadSink(3, false);
        Identity id = new Identity();
        float[] slow = renderRamp(sink, id);
        // 두 결과가 한 프레임도 다르지 않은지 확인한다.
        for (int i = 0; i < inline.length; i++) {
            if (inline[i] != slow[i]) {
                Check.fail("frame " + i + " differs: " + inline[i] + " / " + slow[i]);
            }
        }
        Check.equal(1L, (long) id.threads.size(), "one thread ran the listener");
        Check.isTrue(id.threads.contains("swingwebpanel-test-control"), "the control thread ran it: " + id.threads);
    }

    /**
     * Renders the ramp of the delay test with a task sink (or none) and returns the output.
     * <p>
     * 지연 시험의 램프를 작업 싱크(또는 없이)로 렌더링해 출력을 반환한다.
     *
     * @param sink the sink or null / 싱크 또는 null
     * @return the output / 출력
     */
    private static float[] renderRamp(BaseAudioContext.TaskSink sink) {
        return renderRamp(sink, new Identity());
    }

    /**
     * Renders the ramp with a listener and a sink.
     * <p>
     * 리스너와 싱크로 램프를 렌더링한다.
     *
     * @param sink     the sink or null / 싱크 또는 null
     * @param listener the listener / 리스너
     * @return the output / 출력
     */
    private static float[] renderRamp(BaseAudioContext.TaskSink sink, ScriptProcessorNode.ProcessListener listener) {
        OfflineAudioContext ctx = WebAudioTestKit.mono(2048);
        ctx.setTaskSink(sink);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(1, 2000, RATE));
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        spn.setProcessListener(listener);
        src.connect(spn);
        spn.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.equal(0L, (long) spn.getLateBlockCount(), "nothing was late");
        return out;
    }

    /**
     * A listener that never answers (the sink throws the tasks away) does not hang an offline rendering for ever: with a time limit of 20 ms
     * every block is late after that time, the output is silent, and the rendering ends.
     * <p>
     * 응답하지 않는 리스너(싱크가 작업을 버린다)가 오프라인 렌더링을 영원히 멈추게 하지 않는다: 제한 시간 20 ms 가 지나면 모든 블록이 늦은 것이 되고, 출력은 무음이며, 렌더링은 끝난다.
     */
    public void testOfflineWaitHasATimeLimit() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        ctx.setTaskSink(new ThreadSink(0, true));
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(1, 1024, RATE));
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        spn.setResponseTimeout(20);
        spn.setProcessListener(new Identity());
        src.connect(spn);
        spn.connect(ctx.getDestination());
        src.start(0);
        long begin = System.nanoTime();
        float[] out = WebAudioTestKit.render(ctx);
        double seconds = (System.nanoTime() - begin) / 1e9;
        WebAudioTestKit.assertSilent("everything was late, so silent", out, 0, 1024);
        Check.isTrue(spn.getLateBlockCount() >= 2, "the late blocks: " + spn.getLateBlockCount());
        Check.isTrue(seconds < 5.0, "the rendering ended in " + seconds + " s");
        Check.equal(20L, spn.getResponseTimeout(), "the time limit is kept");
    }

    /**
     * Creates a real-time context that the test renders itself: nothing runs on its own, the tasks wait in the queue sink, and the test calls the
     * renderer, so every block boundary happens when the test says.
     * <p>
     * 시험이 직접 렌더링하는 실시간 컨텍스트를 만든다: 스스로 도는 것이 없고, 작업은 큐 싱크에서 기다리며, 시험이 렌더러를 부르므로 모든 블록 경계는 시험이 정한 때
     * 일어난다.
     *
     * @param sink   the queue sink / 큐 싱크
     * @param driver receives the renderer / 렌더러를 받는다
     * @return the context, running / 컨텍스트(실행 중)
     */
    private static RealtimeAudioContext realtime(QueueSink sink, ManualDriver driver) {
        RealtimeAudioContext ctx = new RealtimeAudioContext(RATE, 0.01, driver);
        ctx.setTaskSink(sink);
        ctx.resume();
        return ctx;
    }

    /**
     * Renders a number of quanta by hand and returns the first channel of the destination of each.
     * <p>
     * 퀀텀 몇 개를 직접 렌더링하고 각각의 목적지 첫 채널을 반환한다.
     *
     * @param driver the driver with the renderer / 렌더러를 가진 드라이버
     * @param quanta the number of quanta / quantum 수
     * @return the samples / 샘플
     */
    private static float[] pump(ManualDriver driver, int quanta) {
        int q = driver.renderer.getRenderQuantumSize();
        float[] out = new float[quanta * q];
        // quantum 을 하나씩 렌더링해 목적지의 첫 채널을 모은다.
        for (int k = 0; k < quanta; k++) {
            driver.renderer.renderQuantum();
            System.arraycopy(driver.renderer.getOutputBus().channel(0), 0, out, k * q, q);
        }
        return out;
    }

    /**
     * Renders a number of blocks of 256 frames by hand and runs the tasks that were requested after each block, so every listener answers in
     * time (the answer of a block is ready before the next boundary). Returns the first channel of the destination.
     * <p>
     * 256 프레임의 블록 몇 개를 직접 렌더링하고 블록마다 요청된 작업을 실행하므로 모든 리스너가 제때 응답한다(블록의 응답이 다음 경계 전에 준비된다). 목적지의 첫
     * 채널을 반환한다.
     *
     * @param driver the driver with the renderer / 렌더러를 가진 드라이버
     * @param sink   the queue sink / 큐 싱크
     * @param blocks the number of blocks / 블록 수
     * @return the samples / 샘플
     */
    private static float[] pumpBlocks(ManualDriver driver, QueueSink sink, int blocks) {
        float[] out = new float[blocks * 256];
        // 블록(quantum 둘)을 렌더링하고 곧바로 요청된 작업을 실행한다.
        for (int b = 0; b < blocks; b++) {
            System.arraycopy(pump(driver, 2), 0, out, b * 256, 256);
            sink.runAll();
        }
        return out;
    }

    /**
     * A late listener in a real-time context is given up: the tasks wait in the queue, so at the second boundary the output of the first block
     * is not ready and that block of output is silent (the task is then run by the test, but it knows that its block was given up and does not
     * call the listener), and the next block plays normally. With blocks of 256 and a constant input of 1: the listener is called only for the blocks
     * that were not given up.
     * <p>
     * 실시간 컨텍스트에서 늦은 리스너는 포기한다: 작업이 큐에서 기다리므로 둘째 경계에서 첫 블록의 출력이 준비되지 않아 그 블록의 출력은 무음이고(시험이 그 작업을 실행해도 작업은 자기 블록이 포기되었음을 알고 리스너를 부르지 않는다), 다음 블록은 정상적으로 재생된다. 256 블록과 상수 입력 1 로, 포기하지 않은 블록에 대해서만 리스너가 불린다.
     */
    public void testALateListenerIsSilentAndItsTaskDoesNotRun() {
        QueueSink sink = new QueueSink();
        ManualDriver driver = new ManualDriver();
        RealtimeAudioContext ctx = realtime(sink, driver);
        ConstantSourceNode one = ctx.createConstantSource();
        one.getOffset().setValue(1f);
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        Identity id = new Identity();
        spn.setProcessListener(id);
        one.connect(spn);
        spn.connect(ctx.getDestination());
        one.start(0);
        // 블록 0, 1 을 렌더링한다(양자 4개). 블록 0 의 작업은 기다리고 있고, 블록 1 의 끝에서 블록 0 의 출력이 필요하지만 준비되지 않았다.
        float[] first = pump(driver, 4);
        Check.equal(2L, (long) sink.size(), "two tasks wait");
        Check.equal(1L, (long) spn.getLateBlockCount(), "block 0 was late at the second boundary");
        sink.runAll();
        Check.equal(1L, (long) id.times.size(), "only the task of block 1 called the listener");
        Check.near((256.0 + 512) / RATE, id.times.get(0).doubleValue(), 1e-12, "it was the task of block 1");
        // 블록 2 와 3 을 렌더링한다. 블록 2 는 (포기한) 블록 0 의 출력이라 무음이고 블록 3 은 블록 1 의 출력(입력 1)이다.
        float[] second = pump(driver, 4);
        sink.runAll();
        WebAudioTestKit.assertSilent("nothing before block 2", first, 0, 512);
        WebAudioTestKit.assertSilent("block 2 is the output of the block that was given up", second, 0, 256);
        WebAudioTestKit.assertSignal("block 3 is the output of block 1", second, 256, 512, new WebAudioTestKit.Signal() {
            public double at(int n) {
                return 1.0;
            }
        }, 1e-7);
        ctx.close();
    }

    /**
     * When the listener never runs (a sink that is stuck) the blocks cannot be recycled, so the node stops after {@value
     * ScriptProcessorNode#MAX_BLOCKS} blocks: the input of the next blocks is dropped and counted, nothing fails, and when the tasks run at last
     * (they find their blocks given up and do not call the listener) the node works again with the same blocks.
     * <p>
     * 리스너가 전혀 실행되지 않으면(멈춘 싱크) 블록을 재활용할 수 없으므로 노드는 {@value ScriptProcessorNode#MAX_BLOCKS} 블록 뒤에 멈춘다: 다음 블록의 입력은 버려지고 세어지며, 아무것도 실패하지 않고, 마침내 작업이 실행되면(자기 블록이 포기된 것을 알고 리스너를 부르지 않는다) 노드는 같은 블록으로 다시 동작한다.
     */
    public void testStuckListenerLimitsTheBlocks() {
        QueueSink sink = new QueueSink();
        ManualDriver driver = new ManualDriver();
        RealtimeAudioContext ctx = realtime(sink, driver);
        ConstantSourceNode one = ctx.createConstantSource();
        one.getOffset().setValue(1f);
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        Identity id = new Identity();
        spn.setProcessListener(id);
        one.connect(spn);
        spn.connect(ctx.getDestination());
        one.start(0);
        pump(driver, 2 * 40);
        Check.equal((long) ScriptProcessorNode.MAX_BLOCKS, (long) spn.getBlockCount(), "no more blocks than the limit");
        Check.isTrue(spn.getDroppedBlockCount() > 20, "the input of the following blocks was dropped: " + spn.getDroppedBlockCount());
        Check.isTrue(sink.size() <= ScriptProcessorNode.MAX_BLOCKS, "no more tasks than blocks: " + sink.size());
        sink.runAll();
        int dropped = spn.getDroppedBlockCount();
        pumpBlocks(driver, sink, 10);
        Check.equal((long) dropped, (long) spn.getDroppedBlockCount(), "after the tasks ran the blocks were recycled and nothing is dropped");
        Check.isTrue(id.times.size() > 0, "the listener runs again");
        ctx.close();
    }

    /**
     * Without a listener no event is requested and the output is silent; a listener that is set later is used from the next block.
     * <p>
     * 리스너가 없으면 이벤트를 요청하지 않고 출력은 무음이다. 나중에 정한 리스너는 다음 블록부터 쓰인다.
     */
    public void testWithoutAListenerThereAreNoEvents() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(2048);
        ConstantSourceNode src = ctx.createConstantSource();
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        src.connect(spn);
        spn.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("silent without a listener", out, 0, 2048);
        Check.equal(0L, (long) spn.getDispatchedBlockCount(), "no events");
        Check.isNull(spn.getProcessListener(), "no listener");
    }

    /**
     * An exception of the listener is contained: the rendering goes on, the exception is kept, and the output is what the listener had written
     * before it threw.
     * <p>
     * 리스너의 예외는 가두어진다: 렌더링은 계속되고, 예외는 보관되며, 출력은 리스너가 던지기 전에 쓴 것이다.
     */
    public void testAnExceptionOfTheListenerIsContained() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        ConstantSourceNode one = ctx.createConstantSource();
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double playbackTime) {
                java.util.Arrays.fill(out.getChannelData(0), 0.5f);
                throw new IllegalStateException("the page failed");
            }
        });
        one.connect(spn);
        spn.connect(ctx.getDestination());
        one.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("what was written before the exception is played", out, 512, 1024, new WebAudioTestKit.Signal() {
            public double at(int n) {
                return 0.5;
            }
        }, 0);
        Check.isTrue(spn.getLastListenerError() instanceof IllegalStateException, "the exception is kept: " + spn.getLastListenerError());
    }

    /**
     * A node that is not connected is not processed (no events), a node with only its input or only its output connected is, and disconnecting
     * both ends makes the node forget the block in progress and the pending output: after it is connected again its first two blocks are silent
     * again.
     * <p>
     * 연결되지 않은 노드는 처리되지 않고(이벤트 없음) 입력만 또는 출력만 연결된 노드는 처리되며, 양쪽을 끊으면 노드가 진행 중인 블록과 대기 중인 출력을 잊는다: 다시 연결하면 처음 두 블록은 다시 무음이다.
     */
    public void testActivityFollowsTheConnections() {
        QueueSink sink = new QueueSink();
        ManualDriver driver = new ManualDriver();
        RealtimeAudioContext ctx = realtime(sink, driver);
        ConstantSourceNode one = ctx.createConstantSource();
        one.getOffset().setValue(1f);
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 1, 1);
        spn.setProcessListener(new Identity());
        one.start(0);
        pumpBlocks(driver, sink, 4);
        Check.equal(0L, (long) spn.getDispatchedBlockCount(), "a node without connections is not processed");
        one.connect(spn);
        pumpBlocks(driver, sink, 4);
        Check.equal(4L, (long) spn.getDispatchedBlockCount(), "an input alone is enough");
        spn.connect(ctx.getDestination());
        float[] heard = pumpBlocks(driver, sink, 6);
        Check.isTrue(heard[heard.length - 1] > 0.99f, "the output is heard after a few blocks: " + heard[heard.length - 1]);
        // 연결을 모두 끊으면 노드는 처리되지 않고, 상태를 잊는다.
        one.disconnect();
        spn.disconnect();
        pumpBlocks(driver, sink, 1);
        int before = spn.getDispatchedBlockCount();
        pumpBlocks(driver, sink, 4);
        Check.equal((long) before, (long) spn.getDispatchedBlockCount(), "no events while disconnected");
        one.connect(spn);
        spn.connect(ctx.getDestination());
        float[] again = pumpBlocks(driver, sink, 4);
        WebAudioTestKit.assertSilent("the first two blocks after reconnecting are silent again", again, 0, 512);
        Check.isTrue(again[600] > 0.99f && again[1000] > 0.99f, "and then the signal is heard: " + again[600] + " / " + again[1000]);
        ctx.close();
    }

    /**
     * The arguments are checked: a buffer size that is 0 (the default of 2048) or a power of two from 256 to 16384, up to 32 channels, not both
     * 0 (an <code>IndexSizeError</code> for every value outside); the channel attributes of the node cannot be changed (the constraints of the
     * specification, with the <code>InvalidStateError</code> of the browsers), but the interpretation can.
     * <p>
     * 인수를 검사한다: 버퍼 크기는 0(기본 2048)이거나 256 ~ 16384 의 2 의 거듭제곱, 채널은 32 개까지이고 둘 다 0 은 안 된다(범위 밖은 모두 <code>IndexSizeError</code>). 노드의 채널 속성은 바꿀 수 없지만(규격의 제약이며 오류는 브라우저의 <code>InvalidStateError</code>) 해석은 바꿀 수 있다.
     */
    public void testValidation() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        Check.equal(2048L, (long) ctx.createScriptProcessor(0, 2, 2).getBufferSize(), "0 is 2048");
        int[] good = {256, 512, 1024, 2048, 4096, 8192, 16384};
        // 허용하는 모든 버퍼 크기를 만들어 본다.
        for (int s : good) {
            Check.equal((long) s, (long) ctx.createScriptProcessor(s, 1, 1).getBufferSize(), "bufferSize " + s);
        }
        int[] bad = {-1, 1, 100, 128, 255, 257, 300, 1000, 32768, 65536};
        // 허용하지 않는 버퍼 크기는 IndexSizeError 이다.
        for (final int s : bad) {
            WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
                public void run() {
                    ctx.createScriptProcessor(s, 2, 2);
                }
            }, "bufferSize " + s);
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createScriptProcessor(256, 0, 0);
            }
        }, "both channel counts 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createScriptProcessor(256, 33, 1);
            }
        }, "33 input channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createScriptProcessor(256, 1, 33);
            }
        }, "33 output channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                ctx.createScriptProcessor(256, -1, 1);
            }
        }, "negative input channels");
        Check.equal(32L, (long) ctx.createScriptProcessor(256, 32, 32).getNumberOfInputChannels(), "32 channels");
        Check.equal(0L, (long) ctx.createScriptProcessor(256, 0, 1).getNumberOfInputChannels(), "no input channels");
        Check.equal(0L, (long) ctx.createScriptProcessor(256, 1, 0).getNumberOfOutputChannels(), "no output channels");
        final ScriptProcessorNode node = ctx.createScriptProcessor(256, 2, 1);
        Check.equal(2L, (long) node.getChannelCount(), "channelCount is the number of input channels");
        Check.equal(ChannelCountMode.EXPLICIT, node.getChannelCountMode(), "explicit");
        Check.equal(ChannelInterpretation.SPEAKERS, node.getChannelInterpretation(), "speakers");
        Check.equal(1L, (long) node.getNumberOfInputs(), "one input");
        Check.equal(1L, (long) node.getNumberOfOutputs(), "one output");
        node.setChannelCount(2);
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                node.setChannelCount(1);
            }
        }, "channelCount cannot change");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                node.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "the mode cannot change");
        node.setChannelCountMode(ChannelCountMode.EXPLICIT);
        node.setChannelInterpretation(ChannelInterpretation.DISCRETE);
        Check.equal(ChannelInterpretation.DISCRETE, node.getChannelInterpretation(), "the interpretation can change");
        Check.equal(1L, (long) ctx.createScriptProcessor(256, 0, 1).getChannelCount(), "a node without input channels reports 1 (an input needs a channel)");
    }

    /**
     * Without a task sink the listener runs on the rendering thread and never twice at the same time, also while the context is paused and
     * resumed in quick succession (the driver creates a new segment each time but keeps one thread): the listener finds nobody else inside,
     * and every call is made by the one rendering thread.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWithoutASinkTheListenerRunsOnTheRenderingThreadOnly() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        final RealtimeAudioContext ctx = OutputRenderDriver.createContext(44100f, 0.01, factory, "swingwebpanel-audio-render-spn");
        final int[] inside = new int[1];
        final boolean[] overlapped = new boolean[1];
        final Set<String> names = new TreeSet<String>();
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 0, 1);
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double playbackTime) {
                synchronized (inside) {
                    inside[0]++;
                    if (inside[0] != 1) {
                        overlapped[0] = true;
                    }
                    names.add(Thread.currentThread().getName());
                }
                java.util.Arrays.fill(out.getChannelData(0), 0.1f);
                synchronized (inside) {
                    inside[0]--;
                }
            }
        });
        spn.connect(ctx.getDestination());
        Random random = new Random(11);
        try {
            // 짧은 무작위 간격으로 일시정지와 재개를 번갈아 부른다.
            for (int i = 0; i < 80; i++) {
                ctx.resume();
                Thread.sleep(random.nextInt(4));
                ctx.suspend();
                Thread.sleep(random.nextInt(2));
            }
            ctx.resume();
            Thread.sleep(100);
        } finally {
            ctx.close();
        }
        Check.isFalse(overlapped[0], "the listener never ran twice at the same time");
        synchronized (inside) {
            Check.equal(1L, (long) names.size(), "one thread ran the listener: " + names);
            Check.isTrue(names.iterator().next().startsWith("swingwebpanel-audio-render-"), "the rendering thread: " + names);
        }
        Check.isTrue(spn.getDispatchedBlockCount() > 0, "events were requested");
    }

    /**
     * A listener that is too slow for real time (it sleeps 30 ms for a block of 5.8 ms) makes the device run dry: the driver counts underruns,
     * keeps working and keeps every frame (the time of the context is whole quanta), and a pause and a close end it promptly.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testASlowListenerCausesUnderrunsButNothingFails() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        factory.setFailure("no device");
        RealtimeAudioContext ctx = OutputRenderDriver.createContext(44100f, 0.01, factory, "swingwebpanel-audio-render-slow");
        ScriptProcessorNode spn = ctx.createScriptProcessor(256, 0, 1);
        spn.setProcessListener(new ScriptProcessorNode.ProcessListener() {
            public void process(AudioBuffer in, AudioBuffer out, double playbackTime) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        spn.connect(ctx.getDestination());
        OutputRenderDriver d = (OutputRenderDriver) ctx.getDriver();
        try {
            ctx.resume();
            Thread.sleep(1500);
            ctx.suspend();
            Thread.sleep(100);
            Check.isTrue(d.getUnderrunCount() > 0, "the device ran dry: " + d.getUnderrunCount());
            Check.isNull(d.getRenderFailure(), "no failure");
            Check.equal(0L, ctx.getCurrentFrame() % ctx.getRenderQuantumSize(), "whole quanta");
            Check.isTrue(ctx.getCurrentTime() > 0.05, "the time advanced: " + ctx.getCurrentTime());
        } finally {
            ctx.close();
        }
    }
}
