package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.FakeLine;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.FakeLines;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.JavaSoundOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the driver that plays a real-time context through a sound output (plan item P25-07), without a sound device (the machine that runs
 * the tests has none): the output is a {@link ManualOutput} that records every frame as a float and plays only when the test says so, or a
 * {@link JavaSoundOutput} on a fake line. What is checked: the audio that reaches the output is <em>exactly</em> what the same graph renders in
 * an {@link OfflineAudioContext} (and, through the 16-bit conversion, that rounded to 16 bits), a pause releases the device and drops the queued
 * audio at once and a resume opens a new one, a device that has stalled does not hold a pause or a close, nothing is lost when the device
 * consumes at once, a computer without a device plays silently with the time at real speed and uses a device that appears later, the sizes of
 * the chunk and the buffer and the latency that the context reports, the mixing to a device of another number of channels, the thread (a named
 * daemon thread of a high priority that ends on close), and that 100 contexts that are created and closed leave no thread and no open output. The
 * state transitions of the context itself are those of {@link RealtimeAudioContextTest}. That the sound is heard is not checked (plan item
 * P22-10).
 * <p>
 * 실시간 컨텍스트를 소리 출력으로 재생하는 드라이버의 시험(계획 P25-07). 소리 장치 없이 한다(시험을 돌리는 컴퓨터에는 없다): 출력은 모든 프레임을 float 으로 기록하고
 * 시험이 시킬 때만 재생하는 {@link ManualOutput} 이거나 가짜 라인 위의 {@link JavaSoundOutput} 이다. 확인하는 것: 출력에 닿는 오디오가 같은 그래프를
 * {@link OfflineAudioContext} 로 렌더링한 것과 <em>정확히</em> 같고(16비트 변환을 거치면 그것을 16비트로 반올림한 것과 같고), 일시정지는 장치를 놓고 큐의 오디오를 곧바로
 * 버리며 재개는 새 장치를 열고, 멈춰 버린 장치가 일시정지나 닫기를 붙잡지 않고, 장치가 곧바로 소비할 때 아무것도 잃지 않고, 장치가 없는 컴퓨터는 시간을 실제 속도로 두고
 * 소리 없이 재생하며 나중에 나타난 장치를 쓰고, 청크와 버퍼의 크기와 컨텍스트가 보고하는 지연, 채널 수가 다른 장치로의 믹스, 스레드(높은 우선순위의 이름 있는 데몬
 * 스레드이며 닫으면 끝난다), 100개의 컨텍스트를 만들고 닫아도 스레드와 열린 출력이 남지 않는 것. 컨텍스트 자체의 상태 전이는 {@link RealtimeAudioContextTest} 의
 * 것이다. 소리가 들리는지는 확인하지 않는다(계획 P22-10).
 */
public class OutputRenderDriverTest {

    /**
     * The sample rate of the tests.
     * <p>
     * 시험의 샘플링 주파수.
     */
    private static final float RATE = 44100f;

    /**
     * A condition that a test waits for.
     * <p>
     * 시험이 기다리는 조건.
     */
    private interface Condition {

        /**
         * Tells whether the condition holds.
         * <p>
         * 조건이 성립하는지 알려 준다.
         *
         * @return true if it holds / 성립하면 true
         */
        boolean holds();
    }

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public OutputRenderDriverTest() {
    }

    /**
     * Waits until a condition holds.
     * <p>
     * 조건이 성립할 때까지 기다린다.
     *
     * @param c      the condition / 조건
     * @param millis the longest wait / 가장 오래 기다릴 시간(밀리초)
     * @return true if the condition held in time / 제때 성립했으면 true
     */
    private static boolean await(Condition c, long millis) {
        long end = System.currentTimeMillis() + millis;
        // 시간이 지날 때까지 조건을 짧게 쉬며 확인한다.
        while (System.currentTimeMillis() < end) {
            if (c.holds()) {
                return true;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Check.fail("interrupted");
            }
        }
        return c.holds();
    }

    /**
     * Sleeps for some milliseconds.
     * <p>
     * 몇 밀리초 잔다.
     *
     * @param ms the milliseconds / 밀리초
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Check.fail("interrupted");
        }
    }

    /**
     * Creates a context that is played by an output driver.
     * <p>
     * 출력 드라이버가 재생하는 컨텍스트를 만든다.
     *
     * @param factory the factory of the outputs / 출력의 팩터리
     * @param latency the latency in seconds / 지연(초)
     * @return the context / 컨텍스트
     */
    private static RealtimeAudioContext make(OutputFactory factory, double latency) {
        return OutputRenderDriver.createContext(RATE, latency, factory, "swingwebpanel-audio-render-test");
    }

    /**
     * Returns the driver of a context.
     * <p>
     * 컨텍스트의 드라이버를 반환한다.
     *
     * @param ctx the context / 컨텍스트
     * @return the driver / 드라이버
     */
    private static OutputRenderDriver driver(RealtimeAudioContext ctx) {
        return (OutputRenderDriver) ctx.getDriver();
    }

    /**
     * Counts the rendering threads of the product that are alive.
     * <p>
     * 살아 있는 제품의 렌더링 스레드를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    private static int liveRenderThreads() {
        int n = 0;
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        // 이름이 렌더링 스레드 접두사로 시작하는 살아 있는 스레드를 센다.
        for (Thread t : all.keySet()) {
            if (t.isAlive() && t.getName().startsWith(OutputRenderDriver.THREAD_NAME_PREFIX)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Builds the graph of the tests: a sawtooth through a low-pass filter whose frequency ramps, a gain that ramps, and a stereo panner that moves
     * from the left to the right, plus a quiet square wave through a delay that starts at 0.2 s. The peak stays below 0.95, so nothing clips.
     * <p>
     * 시험의 그래프를 만든다: 주파수가 램프하는 저역 통과 필터를 거친 톱니파, 램프하는 게인, 왼쪽에서 오른쪽으로 움직이는 스테레오 패너, 그리고 0.2 초에 시작해 지연을
     * 거치는 작은 사각파. 최고점은 0.95 아래이므로 잘리는 것이 없다.
     *
     * @param c the context / 컨텍스트
     */
    private static void buildGraph(BaseAudioContext c) {
        OscillatorNode saw = c.createOscillator();
        saw.setType(OscillatorType.SAWTOOTH);
        saw.getFrequency().setValue(330f);
        BiquadFilterNode lowpass = c.createBiquadFilter();
        lowpass.setType(BiquadFilterType.LOWPASS);
        lowpass.getFrequency().setValueAtTime(800f, 0);
        lowpass.getFrequency().linearRampToValueAtTime(3000f, 0.5);
        GainNode gain = c.createGain();
        gain.getGain().setValueAtTime(0.2f, 0);
        gain.getGain().linearRampToValueAtTime(0.6f, 1.0);
        StereoPannerNode pan = c.createStereoPanner();
        pan.getPan().setValueAtTime(-0.5f, 0);
        pan.getPan().linearRampToValueAtTime(0.5f, 1.0);
        saw.connect(lowpass);
        lowpass.connect(gain);
        gain.connect(pan);
        pan.connect(c.getDestination());
        saw.start(0);
        OscillatorNode square = c.createOscillator();
        square.setType(OscillatorType.SQUARE);
        square.getFrequency().setValue(220f);
        GainNode quiet = c.createGain();
        quiet.getGain().setValue(0.1f);
        DelayNode delay = c.createDelay();
        delay.getDelayTime().setValue(0.01f);
        square.connect(quiet);
        quiet.connect(delay);
        delay.connect(c.getDestination());
        square.start(0.2);
    }

    /**
     * The audio that reaches the output is exactly what an offline context renders for the same graph: every float of both channels of the
     * first 300 quanta is equal (the same engine renders both, and the driver only copies the quanta into the chunk, so nothing may differ by a
     * single bit).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAudioEqualsTheOfflineRendering() throws Exception {
        final int frames = 128 * 300;
        OfflineAudioContext offline = new OfflineAudioContext(2, frames, RATE);
        buildGraph(offline);
        AudioBuffer expected = offline.startRendering();
        ManualOutputFactory factory = new ManualOutputFactory();
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            buildGraph(ctx);
            ctx.resume();
            final ManualOutput out = factory.waitFor(1, 10000);
            Check.notNull(out, "the output was opened");
            // 시험이 장치를 대신해 큐를 비워 주면서 충분히 쓰일 때까지 기다린다.
            boolean enough = await(new Condition() {
                public boolean holds() {
                    out.consume(1024);
                    return out.getWrittenFrames() >= frames + 4096;
                }
            }, 20000);
            Check.isTrue(enough, "enough frames were written: " + out.getWrittenFrames());
            ctx.suspend();
            // 두 채널의 모든 프레임이 오프라인 렌더링과 한 비트도 다르지 않은지 확인한다.
            for (int c = 0; c < 2; c++) {
                float[] got = out.copyChannel(c);
                float[] want = expected.getChannelData(c);
                // 프레임마다 두 값이 같은지 비교한다.
                for (int i = 0; i < frames; i++) {
                    if (got[i] != want[i]) {
                        Check.fail("channel " + c + " frame " + i + " is " + got[i] + " but the offline rendering has " + want[i]);
                    }
                }
            }
        } finally {
            ctx.close();
        }
    }

    /**
     * Through the Java Sound output the audio is the offline rendering rounded to 16 bits: every sample that arrives at the (fake) line is the
     * integer that rounds the offline sample times 32768 to the nearest (the error is at most half a step, 1.53e-5, which is checked as the
     * exact integer), and the order of the channels is the interleaved left, right.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSixteenBitPathIsTheRoundedOfflineRendering() throws Exception {
        final int frames = 128 * 200;
        OfflineAudioContext offline = new OfflineAudioContext(2, frames, RATE);
        buildGraph(offline);
        AudioBuffer expected = offline.startRendering();
        double peak = 0;
        // 최고점이 0.98 아래여야 자르기가 시험의 기대값에 끼어들지 않는다.
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < frames; i++) {
                peak = Math.max(peak, Math.abs(expected.getChannelData(c)[i]));
            }
        }
        Check.isTrue(peak < 0.98 && peak > 0.2, "the peak of the graph is " + peak);
        final FakeLines lines = new FakeLines();
        OutputFactory factory = new OutputFactory() {
            public AudioOutput open(int sampleRate, int channels, int bufferFrames) throws IOException {
                return JavaSoundOutput.open(lines, sampleRate, channels, bufferFrames);
            }
        };
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            buildGraph(ctx);
            ctx.resume();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return lines.count() > 0;
                }
            }, 10000), "a line was opened");
            final FakeLine line = lines.last();
            final byte[][] bytes = new byte[1][];
            // 시험이 장치를 대신해 라인의 바이트를 재생해 주면서 충분히 쓰일 때까지 기다린다.
            boolean enough = await(new Condition() {
                public boolean holds() {
                    line.consume(4096);
                    bytes[0] = line.getRecorded();
                    return bytes[0].length >= (frames + 2048) * 4;
                }
            }, 20000);
            Check.isTrue(enough, "enough bytes were written: " + bytes[0].length);
            ctx.suspend();
            float[][] heard = FakeLine.decode16(bytes[0], 2);
            // 모든 프레임이 오프라인 값을 가장 가까운 16비트로 반올림한 것과 같은 정수인지 확인한다.
            for (int c = 0; c < 2; c++) {
                for (int i = 0; i < frames; i++) {
                    long want = (long) Math.floor(expected.getChannelData(c)[i] * 32768.0 + 0.5);
                    long got = Math.round(heard[c][i] * 32768f);
                    if (want != got) {
                        Check.fail("channel " + c + " frame " + i + " is " + got + " (16 bit) but the rounded offline sample is " + want);
                    }
                }
            }
        } finally {
            ctx.close();
        }
    }

    /**
     * A pause releases the device: the output is stopped, flushed and closed (the sound stops at once, the queued audio is dropped), the time
     * stands still, and a resume opens a second output, so a suspended context holds no device. The thread stays (it is the same thread
     * for both segments).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseReleasesTheDeviceAndResumeOpensANewOne() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            ctx.resume();
            final ManualOutput first = factory.waitFor(1, 10000);
            Check.notNull(first, "the first output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return first.isStarted();
                }
            }, 10000), "the output was filled and started");
            first.consume(512);
            Thread thread = driver(ctx).getThread();
            ctx.suspend();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return first.isClosed();
                }
            }, 10000), "a pause closes the output");
            Check.isTrue(first.getStopCount() >= 1, "it was stopped");
            Check.isTrue(first.getFlushCount() >= 1, "its queue was dropped");
            Check.equal(0L, first.getQueuedFrames(), "nothing is left to play");
            sleep(30);
            double stopped = ctx.getCurrentTime();
            sleep(200);
            Check.equal(stopped, ctx.getCurrentTime(), "the time stands still while suspended");
            Check.equal(0L, ctx.getCurrentFrame() % ctx.getRenderQuantumSize(), "in whole quanta");
            Check.equal(0.0, ctx.getOutputLatency(), "no output latency without a device");
            Check.isTrue(thread.isAlive(), "the thread waits for the next segment");
            Check.equal(1L, factory.count(), "no second output yet");
            ctx.resume();
            final ManualOutput second = factory.waitFor(2, 10000);
            Check.notNull(second, "a resume opens a new output");
            Check.isTrue(second != first, "a different output");
            Check.same(thread, driver(ctx).getThread(), "the same thread continues");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return second.getWrittenFrames() > 0;
                }
            }, 10000), "the new output receives audio");
            Check.isTrue(ctx.getCurrentTime() >= stopped, "the time continues from where it stopped");
            Check.equal(2L, driver(ctx).getSegmentCount(), "two segments");
        } finally {
            ctx.close();
        }
    }

    /**
     * A device that has stalled (the output never plays anything, so the rendering thread waits for room in the output) does not hold a
     * pause or a close: the pause closes the output within seconds, the next resume works, and the close ends the thread.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStalledDeviceDoesNotHoldAPauseOrAClose() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            ctx.resume();
            final ManualOutput stalled = factory.waitFor(1, 10000);
            Check.notNull(stalled, "the output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return stalled.isStarted();
                }
            }, 10000), "started");
            sleep(100);
            long frames = stalled.getWrittenFrames();
            sleep(100);
            Check.equal(frames, stalled.getWrittenFrames(), "the writer waits, a stalled device takes no more audio");
            ctx.suspend();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return stalled.isClosed();
                }
            }, 5000), "a pause ends the wait and releases the stalled output");
            ctx.resume();
            final ManualOutput again = factory.waitFor(2, 10000);
            Check.notNull(again, "a resume works after that");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return again.isStarted();
                }
            }, 10000), "the second segment started");
            final Thread thread = driver(ctx).getThread();
            ctx.close();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return !thread.isAlive();
                }
            }, 5000), "a close ends the thread although the device stalls");
            Check.isTrue(again.isClosed(), "and releases the output");
        } finally {
            ctx.close();
        }
    }

    /**
     * The thread is a daemon thread with the name of the product's rendering threads and a priority above normal; it is created at the first
     * resume (not by the construction), and a close ends it and closes the output.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThreadIsANamedDaemonAndEndsOnClose() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        RealtimeAudioContext ctx = make(factory, 0.01);
        Check.isNull(driver(ctx).getThread(), "no thread before the first start");
        Check.equal(0L, factory.count(), "no output before the first start");
        ctx.resume();
        final Thread thread = driver(ctx).getThread();
        Check.notNull(thread, "the thread exists");
        Check.isTrue(thread.isDaemon(), "a daemon thread");
        Check.isTrue(thread.getName().startsWith("swingwebpanel-audio-render-"), "the name " + thread.getName());
        Check.isTrue(thread.getPriority() > Thread.NORM_PRIORITY, "a priority above normal: " + thread.getPriority());
        ManualOutput out = factory.waitFor(1, 10000);
        Check.notNull(out, "the output");
        ctx.close();
        Check.isTrue(await(new Condition() {
            public boolean holds() {
                return !thread.isAlive();
            }
        }, 5000), "the thread ended");
        Check.isTrue(out.isClosed(), "the output was closed");
        OutputRenderDriver plain = new OutputRenderDriver("plain", factory, 0.01);
        plain.close();
        Check.isNull(plain.getThread(), "a driver that was closed before a start never creates a thread");
    }

    /**
     * A computer without a device plays silently: the factory fails, the context still runs, the time advances at about the speed of the wall
     * clock (the output makes the writes wait; the margins are generous because the machine is loaded), the output is the silent one and the
     * reason is kept. The default factory (the real sound system) runs without an error whether the machine has a device or not.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoDeviceIsSilentButTheTimeRunsAtRealSpeed() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        factory.setFailure("no sound card here");
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            buildGraph(ctx);
            ctx.resume();
            sleep(250);
            OutputRenderDriver d = driver(ctx);
            Check.isFalse(d.isOutputAudible(), "the output is silent");
            Check.contains(d.getOutputDescription(), "silent", "the description");
            Check.contains(d.getOutputProblem(), "no sound card here", "the reason is kept");
            double t0 = ctx.getCurrentTime();
            long w0 = System.nanoTime();
            sleep(600);
            double t1 = ctx.getCurrentTime();
            double wall = (System.nanoTime() - w0) / 1e9;
            Check.isTrue(t1 - t0 > wall * 0.6 && t1 - t0 < wall * 1.4 + 0.15, "the time advanced " + (t1 - t0) + " s in " + wall + " s of the wall clock");
            Check.isNull(d.getRenderFailure(), "no failure");
        } finally {
            ctx.close();
        }
        RealtimeAudioContext system = OutputRenderDriver.createContext(RATE, 0.01, null, "swingwebpanel-audio-render-system");
        try {
            system.resume();
            sleep(300);
            Check.isTrue(system.getCurrentTime() > 0.05, "the default factory runs: " + system.getCurrentTime());
            Check.notNull(driver(system).getOutputDescription(), "an output was opened");
            Check.isNull(driver(system).getRenderFailure(), "no failure with the default factory");
            if (!driver(system).isOutputAudible()) {
                Check.notNull(driver(system).getOutputProblem(), "a silent output says why");
            }
        } finally {
            system.close();
        }
    }

    /**
     * A device that appears later is used: a segment that could not open one plays silently, and after a pause the next segment tries again
     * and opens it.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDeviceThatAppearsLaterIsUsedByTheNextSegment() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        factory.setFailure("busy");
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            ctx.resume();
            final OutputRenderDriver d = driver(ctx);
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return d.getOutputDescription() != null;
                }
            }, 10000), "a segment was played");
            Check.isFalse(d.isOutputAudible(), "silent while the device is busy");
            ctx.suspend();
            factory.setFailure(null);
            ctx.resume();
            final ManualOutput out = factory.waitFor(1, 10000);
            Check.notNull(out, "the device is used by the next segment");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return d.isOutputAudible() && d.getOutputProblem() == null;
                }
            }, 10000), "audible, and the problem is gone");
        } finally {
            ctx.close();
        }
    }

    /**
     * The chunk is half of the latency in render quanta (from 1 to 16) and the buffer is the larger of 50 ms and four times the latency, as a
     * whole number of chunks: 10 ms gives chunks of 128 frames and a buffer of 2304 (52 ms), 20 ms gives 384 and 3840, and 100 ms gives 2048
     * and 18432 (418 ms) at 44100 Hz. The output is opened with that buffer.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizesFollowTheLatency() throws Exception {
        double[] latencies = {0.010, 0.020, 0.100};
        int[] chunks = {128, 384, 2048};
        int[] buffers = {2304, 3840, 18432};
        // 지연마다 컨텍스트를 만들어 청크와 버퍼의 크기와 출력이 연 버퍼를 확인한다.
        for (int i = 0; i < latencies.length; i++) {
            ManualOutputFactory factory = new ManualOutputFactory();
            RealtimeAudioContext ctx = make(factory, latencies[i]);
            try {
                ctx.resume();
                ManualOutput out = factory.waitFor(1, 10000);
                Check.notNull(out, "the output of " + latencies[i]);
                final OutputRenderDriver d = driver(ctx);
                Check.equal((long) chunks[i], (long) d.getChunkFrames(), "the chunk for " + latencies[i]);
                Check.equal((long) buffers[i], (long) d.getBufferFrames(), "the buffer for " + latencies[i]);
                Check.equal((long) buffers[i], (long) out.getBufferFrames(), "the output was opened with that buffer for " + latencies[i]);
                Check.equal(0L, (long) (d.getBufferFrames() % d.getChunkFrames()), "a whole number of chunks");
            } finally {
                ctx.close();
            }
        }
    }

    /**
     * The output latency that the context reports is the audio that waits in the output: with a device that never plays it is the whole buffer
     * (the driver fills it to the last chunk before it starts), it is reported in seconds of the context, and it is 0 again after a pause.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputLatencyIsTheQueuedAudio() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        final RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            Check.equal(0.0, ctx.getOutputLatency(), "0 before the start");
            ctx.resume();
            final ManualOutput out = factory.waitFor(1, 10000);
            Check.notNull(out, "the output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return out.isStarted() && ctx.getOutputLatency() > 0;
                }
            }, 10000), "a latency was reported");
            sleep(100);
            Check.near(2304.0 / RATE, ctx.getOutputLatency(), 1e-9, "the full buffer of 2304 frames");
            Check.isTrue(ctx.getBaseLatency() > 0, "the base latency of the context is untouched");
            ctx.suspend();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return ctx.getOutputLatency() == 0.0;
                }
            }, 10000), "0 after a pause");
        } finally {
            ctx.close();
        }
    }

    /**
     * Nothing is lost when the device plays everything at once: after the close the frames that the output received are exactly the frames
     * that the context rendered (every chunk that is rendered is written, also the one in progress when the close comes), and every write
     * found the output empty, which is counted as an underrun.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNothingIsLostWhenTheDevicePlaysAtOnce() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        factory.setAutoConsume(true);
        RealtimeAudioContext ctx = make(factory, 0.01);
        buildGraph(ctx);
        ctx.resume();
        final ManualOutput out = factory.waitFor(1, 10000);
        Check.notNull(out, "the output");
        Check.isTrue(await(new Condition() {
            public boolean holds() {
                return out.getWrittenFrames() > 128 * 500;
            }
        }, 10000), "audio flows");
        final Thread thread = driver(ctx).getThread();
        ctx.close();
        Check.isTrue(await(new Condition() {
            public boolean holds() {
                return !thread.isAlive();
            }
        }, 5000), "the thread ended");
        Check.equal(ctx.getCurrentFrame(), out.getWrittenFrames(), "the output got every frame that was rendered");
        Check.isTrue(driver(ctx).getUnderrunCount() > 100, "the device was empty at every write after the start: " + driver(ctx).getUnderrunCount());
        Check.isNull(driver(ctx).getRenderFailure(), "no failure");
    }

    /**
     * The destination is mixed to the channels of the device with the rules of speakers: a source panned fully to the right
     * (the left channel is 0 and the right channel 1) gives 0.5 on a mono device (half of the sum), a mono source of 0.25 gives 0.25 on
     * both channels of a stereo device when the destination has one channel (<code>channelCount</code> set to 1, which changes the bus
     * while the context runs), and a 6-channel device gets the front left and right channels and silence in the others.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMixesToTheChannelsOfTheDevice() throws Exception {
        // 모노 장치: 패너로 완전히 오른쪽에 둔 1 은 (0 + 1) / 2 = 0.5 가 된다.
        ManualOutputFactory mono = new ManualOutputFactory();
        mono.setChannelsOverride(1);
        RealtimeAudioContext c1 = make(mono, 0.01);
        try {
            ConstantSourceNode one = c1.createConstantSource();
            one.getOffset().setValue(1f);
            StereoPannerNode pan = c1.createStereoPanner();
            pan.getPan().setValue(1f);
            one.connect(pan);
            pan.connect(c1.getDestination());
            one.start(0);
            c1.resume();
            final ManualOutput out = mono.waitFor(1, 10000);
            Check.notNull(out, "the mono output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return out.getWrittenFrames() > 1024;
                }
            }, 10000), "audio flows");
            c1.suspend();
            float[] heard = out.copyChannel(0);
            // 처음 양자(첫 render quantum 에는 연결이 적용된다) 이후의 모든 프레임이 0.5 인지 확인한다.
            for (int i = 128; i < 1024; i++) {
                if (Math.abs(heard[i] - 0.5f) > 1e-6f) {
                    Check.fail("frame " + i + " of the mono device is " + heard[i]);
                }
            }
        } finally {
            c1.close();
        }
        // 스테레오 장치와 채널이 1개인 목적지: 같은 값이 양쪽 채널에 간다.
        ManualOutputFactory stereo = new ManualOutputFactory();
        RealtimeAudioContext c2 = make(stereo, 0.01);
        try {
            c2.getDestination().setChannelCount(1);
            ConstantSourceNode quarter = c2.createConstantSource();
            quarter.getOffset().setValue(0.25f);
            quarter.connect(c2.getDestination());
            quarter.start(0);
            c2.resume();
            final ManualOutput out = stereo.waitFor(1, 10000);
            Check.notNull(out, "the stereo output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return out.getWrittenFrames() > 1024;
                }
            }, 10000), "audio flows");
            c2.suspend();
            float[] left = out.copyChannel(0);
            float[] right = out.copyChannel(1);
            // 두 채널이 모두 0.25 인지 확인한다.
            for (int i = 128; i < 1024; i++) {
                if (left[i] != 0.25f || right[i] != 0.25f) {
                    Check.fail("frame " + i + " of the stereo device is " + left[i] + " / " + right[i]);
                }
            }
        } finally {
            c2.close();
        }
        // 6채널 장치: 앞 왼쪽과 오른쪽에 스테레오가 가고 나머지는 무음이다.
        ManualOutputFactory six = new ManualOutputFactory();
        six.setChannelsOverride(6);
        RealtimeAudioContext c3 = make(six, 0.01);
        try {
            ConstantSourceNode half = c3.createConstantSource();
            half.getOffset().setValue(0.5f);
            StereoPannerNode pan = c3.createStereoPanner();
            pan.getPan().setValue(1f);
            half.connect(pan);
            pan.connect(c3.getDestination());
            half.start(0);
            c3.resume();
            final ManualOutput out = six.waitFor(1, 10000);
            Check.notNull(out, "the 6 channel output");
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return out.getWrittenFrames() > 1024;
                }
            }, 10000), "audio flows");
            c3.suspend();
            Check.equal(6L, (long) out.getChannels(), "six channels");
            // 앞 왼쪽은 0, 앞 오른쪽은 0.5, 나머지 채널은 무음인지 확인한다.
            for (int i = 128; i < 1024; i++) {
                if (Math.abs(out.copyChannel(0)[i]) > 1e-6f || Math.abs(out.copyChannel(1)[i] - 0.5f) > 1e-6f) {
                    Check.fail("frame " + i + " of the 6 channel device: " + out.copyChannel(0)[i] + " / " + out.copyChannel(1)[i]);
                }
                // 나머지 채널이 무음인지 확인한다.
                for (int c = 2; c < 6; c++) {
                    if (out.copyChannel(c)[i] != 0f) {
                        Check.fail("channel " + c + " should be silent at frame " + i);
                    }
                }
            }
        } finally {
            c3.close();
        }
    }

    /**
     * The estimate of the audio that is heard now stays between the rendered time minus the buffer (and some slack) and the rendered time, runs at the speed
     * of the wall clock from the start of the sound, and is the rendered time when nothing plays. A silent output is used, because its virtual
     * device plays at real speed like a device does.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputTimestamp() throws Exception {
        ManualOutputFactory factory = new ManualOutputFactory();
        factory.setFailure("no device");
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            OutputRenderDriver d = driver(ctx);
            OutputRenderDriver.Timestamp idle = d.getOutputTimestamp();
            Check.equal(0.0, idle.getContextTime(), "0 before the start");
            long w0 = System.nanoTime();
            ctx.resume();
            sleep(500);
            double wall = (System.nanoTime() - w0) / 1e9;
            OutputRenderDriver.Timestamp ts = d.getOutputTimestamp();
            double rendered = ctx.getCurrentTime();
            double bufferSeconds = d.getBufferFrames() / (double) RATE;
            Check.isTrue(ts.getContextTime() <= rendered + 1e-9, "never beyond the rendered time: " + ts.getContextTime() + " > " + rendered);
            Check.isTrue(ts.getContextTime() >= rendered - bufferSeconds - 0.1, "not more than the buffer behind: " + ts.getContextTime() + " vs " + rendered);
            Check.isTrue(ts.getContextTime() > wall - 0.35 && ts.getContextTime() < wall + 0.05, "heard " + ts.getContextTime() + " s after " + wall + " s of the wall clock");
            Check.isTrue(ts.getNanoTime() > w0, "a reading of the clock");
            ctx.suspend();
            sleep(100);
            Check.near(ctx.getCurrentTime(), d.getOutputTimestamp().getContextTime(), 1e-9, "the rendered time when nothing plays");
        } finally {
            ctx.close();
        }
    }

    /**
     * Pauses and resumes in a quick random sequence (with devices that stall) never fail and never render in two threads: the number of
     * rendered frames stays a whole number of quanta, the driver records no failure, every output that was opened is closed, and the segments
     * match the resumes that were not merged by a missing pause.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQuickPausesAndResumes() throws Exception {
        final ManualOutputFactory factory = new ManualOutputFactory();
        RealtimeAudioContext ctx = make(factory, 0.01);
        try {
            buildGraph(ctx);
            Random random = new Random(7);
            // 짧은 무작위 간격으로 일시정지와 재개를 번갈아 부른다.
            for (int i = 0; i < 150; i++) {
                ctx.resume();
                sleep(random.nextInt(4));
                if (random.nextInt(4) != 0) {
                    ctx.suspend();
                    sleep(random.nextInt(3));
                }
            }
            ctx.suspend();
            final OutputRenderDriver d = driver(ctx);
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    // 구간마다 출력을 정확히 하나 열고 구간이 끝나면 닫으므로, 안정되면 모든 출력이 닫혀 있고 수가 구간 수와 같다.
                    for (int i = 0; i < factory.count(); i++) {
                        if (!factory.get(i).isClosed()) {
                            return false;
                        }
                    }
                    return d.getSegmentCount() == factory.count();
                }
            }, 10000), "every segment opened one output and closed it");
            sleep(100);
            Check.equal(0L, ctx.getCurrentFrame() % ctx.getRenderQuantumSize(), "whole quanta only");
            Check.isNull(d.getRenderFailure(), "no failure: " + d.getRenderFailure());
            final Thread thread = d.getThread();
            ctx.close();
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return !thread.isAlive();
                }
            }, 5000), "the thread ended");
            // 연 출력이 모두 닫혔는지 확인한다.
            for (int i = 0; i < factory.count(); i++) {
                Check.isTrue(factory.get(i).isClosed(), "output " + i + " is closed");
            }
        } finally {
            ctx.close();
        }
    }

    /**
     * Creating and closing 100 contexts (half of them closed before the device was opened, half after) leaves no rendering thread of the
     * product alive and no output open: the numbers return to what they were before the loop.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHundredContextsLeaveNothingBehind() throws Exception {
        final int before = liveRenderThreads();
        final ManualOutputFactory factory = new ManualOutputFactory();
        // 컨텍스트를 만들고 시작해서 (절반은 곧바로, 절반은 장치가 열린 뒤에) 닫는다.
        for (int i = 0; i < 100; i++) {
            final int opened = factory.count();
            RealtimeAudioContext ctx = OutputRenderDriver.createContext(8000f, 0.01, factory, "swingwebpanel-audio-render-loop-" + i);
            ctx.resume();
            if (i % 2 == 1) {
                Check.isTrue(await(new Condition() {
                    public boolean holds() {
                        return factory.count() > opened;
                    }
                }, 10000), "the output of context " + i);
            }
            ctx.close();
        }
        Check.isTrue(await(new Condition() {
            public boolean holds() {
                return liveRenderThreads() <= before;
            }
        }, 10000), "the rendering threads ended: " + liveRenderThreads() + " alive, " + before + " before");
        // 열린 출력이 모두 닫혔는지 확인한다(장치를 열기 전에 닫힌 컨텍스트는 출력을 열지 않았을 수 있다).
        for (int i = 0; i < factory.count(); i++) {
            final ManualOutput out = factory.get(i);
            Check.isTrue(await(new Condition() {
                public boolean holds() {
                    return out.isClosed();
                }
            }, 10000), "output " + i + " is closed");
        }
        Check.isTrue(factory.count() >= 50, "outputs were opened: " + factory.count());
    }
}
