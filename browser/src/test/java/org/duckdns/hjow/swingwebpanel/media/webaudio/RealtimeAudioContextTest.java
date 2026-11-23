package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the real-time context and its clock driver (plan item P25-07, first part): the state transitions and their errors, a time that advances at
 * the pace of the wall clock and stops while suspended, a graph that is rendered in real time (an oscillator that starts and ends on time and tells it
 * through the task sink, an analyser that sees the signal), the latency rounding, and a driver thread that is a named daemon and ends when the context is
 * closed. The pace is checked with generous margins, because the tests run on a loaded machine; what is checked is that the time moves at about the speed of
 * the wall clock and not faster or slower by a large factor.
 * <p>
 * 실시간 컨텍스트와 그 시계 드라이버의 시험(계획 P25-07 의 첫 부분): 상태 전이와 그 오류, 벽시계의 속도로 흐르고 일시 중단되면 멈추는 시간, 실시간으로
 * 렌더링되는 그래프(제때 시작하고 끝나며 작업 싱크로 알리는 발진기, 신호를 보는 분석기), 지연의 올림, 이름이 있는 데몬이며 컨텍스트를 닫으면 끝나는 드라이버 스레드.
 * 시험은 부하가 있는 기계에서 돌므로 속도는 넉넉한 여유를 두고 확인한다. 확인하는 것은 시간이 벽시계와 비슷한 속도로 움직이고 크게 빠르거나 느리지 않다는 것이다.
 */
public class RealtimeAudioContextTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public RealtimeAudioContextTest() {
    }

    /**
     * Creates a context with a clock driver that has a name like the product's.
     * <p>
     * 제품과 같은 이름 형식의 시계 드라이버를 가진 컨텍스트를 만든다.
     *
     * @param rate the sample rate / 샘플링 주파수
     * @return the context / 컨텍스트
     */
    private static RealtimeAudioContext make(float rate) {
        return new RealtimeAudioContext(rate, RealtimeAudioContext.LATENCY_INTERACTIVE, new ClockRenderDriver("swingwebpanel-audio-render-test"));
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
     * A new context is suspended at time 0; resume runs it, suspend stops it, close ends it; a closed context refuses to resume and suspend and
     * closing again has no effect.
     * <p>
     * 새 컨텍스트는 시간 0 에서 일시 중단되어 있다. resume 이 돌리고, suspend 가 멈추고, close 가 끝낸다. 닫힌 컨텍스트는 resume 과 suspend 를 거부하고 다시
     * 닫아도 효과가 없다.
     */
    public void testStateTransitions() {
        final RealtimeAudioContext ctx = make(44100f);
        Check.equal(ContextState.SUSPENDED, ctx.getState(), "a new context is suspended");
        Check.equal(0.0, ctx.getCurrentTime(), "time 0");
        ctx.resume();
        Check.equal(ContextState.RUNNING, ctx.getState(), "resume");
        ctx.resume();
        Check.equal(ContextState.RUNNING, ctx.getState(), "resume again");
        ctx.suspend();
        Check.equal(ContextState.SUSPENDED, ctx.getState(), "suspend");
        ctx.resume();
        ctx.close();
        Check.equal(ContextState.CLOSED, ctx.getState(), "close");
        Check.isTrue(ctx.isClosed(), "closed");
        ctx.close();
        Check.equal(ContextState.CLOSED, ctx.getState(), "close again");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ctx.resume();
            }
        }, "resume after close");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ctx.suspend();
            }
        }, "suspend after close");
    }

    /**
     * The time advances at the pace of the wall clock while running, does not advance while suspended and continues from the same value after resume.
     * <p>
     * 시간은 도는 동안 벽시계의 속도로 흐르고, 일시 중단된 동안은 흐르지 않으며, resume 뒤에는 같은 값에서 이어진다.
     */
    public void testTimeFollowsTheWallClock() {
        RealtimeAudioContext ctx = make(48000f);
        try {
            long t0 = System.nanoTime();
            ctx.resume();
            sleep(300);
            double wall = (System.nanoTime() - t0) / 1e9;
            double time = ctx.getCurrentTime();
            // 시간은 벽시계의 시간보다 한 quantum 정도 앞서고(여유), 크게 뒤처지지 않는다.
            Check.isTrue(time > wall * 0.7 && time < wall * 1.2 + 0.05, "time " + time + " follows the wall clock " + wall);
            ctx.suspend();
            sleep(30);
            double stopped = ctx.getCurrentTime();
            sleep(200);
            Check.equal(stopped, ctx.getCurrentTime(), "the time does not advance while suspended");
            ctx.resume();
            sleep(150);
            double after = ctx.getCurrentTime();
            Check.isTrue(after > stopped + 0.08 && after < stopped + 0.4, "time continues from the same value: " + stopped + " -> " + after);
            Check.equal(0L, ctx.getCurrentFrame() % ctx.getRenderQuantumSize(), "the time moves in whole quanta");
        } finally {
            ctx.close();
        }
    }

    /**
     * The graph is rendered in real time: a source that stops at 0.1 s tells it through the task sink on the time of the context, and an analyser sees a
     * signal that a constant source produces.
     * <p>
     * 그래프가 실시간으로 렌더링된다: 0.1 초에 멈추는 원천이 컨텍스트의 시간에 작업 싱크로 알리고, 분석기가 상수 원천이 만든 신호를 본다.
     */
    public void testGraphRunsInRealTime() {
        final RealtimeAudioContext ctx = make(44100f);
        try {
            final AtomicInteger sinkCalls = new AtomicInteger();
            ctx.setTaskSink(new BaseAudioContext.TaskSink() {
                public void post(Runnable task) {
                    sinkCalls.incrementAndGet();
                    task.run();
                }
            });
            final OscillatorNode osc = ctx.createOscillator();
            final AtomicInteger ended = new AtomicInteger();
            osc.setEndedListener(new Runnable() {
                public void run() {
                    ended.incrementAndGet();
                }
            });
            ConstantSourceNode constant = ctx.createConstantSource();
            AnalyserNode analyser = ctx.createAnalyser();
            constant.connect(analyser);
            osc.connect(ctx.getDestination());
            constant.start(0);
            osc.start(0);
            osc.stop(0.1);
            ctx.resume();
            long deadline = System.currentTimeMillis() + 3000;
            // 원천이 끝날 때까지(최대 3 초) 기다린다.
            while (ended.get() == 0 && System.currentTimeMillis() < deadline) {
                sleep(10);
            }
            Check.equal(1, ended.get(), "the oscillator ended once");
            Check.isTrue(sinkCalls.get() >= 1, "through the task sink");
            Check.isTrue(ctx.getCurrentTime() >= 0.1, "the oscillator ended after its stop time: " + ctx.getCurrentTime());
            float[] data = new float[analyser.getFftSize()];
            analyser.getFloatTimeDomainData(data);
            double peak = 0;
            // 분석기의 시간 영역 데이터에서 가장 큰 값을 찾는다.
            for (int i = 0; i < data.length; i++) {
                peak = Math.max(peak, Math.abs(data[i]));
            }
            Check.near(1.0, peak, 1e-6, "the analyser sees the constant source");
        } finally {
            ctx.close();
        }
    }

    /**
     * The base latency is the requested latency rounded up to whole quanta and never less than two quanta; the hints map to the documented values.
     * <p>
     * 기본 지연은 요청한 지연을 quantum 의 정수 배로 올림한 값이며 두 quantum 보다 작지 않다. 힌트는 문서의 값으로 대응한다.
     */
    public void testLatency() {
        Check.equal(RealtimeAudioContext.LATENCY_INTERACTIVE, RealtimeAudioContext.resolveLatency("interactive", 0), "interactive");
        Check.equal(RealtimeAudioContext.LATENCY_BALANCED, RealtimeAudioContext.resolveLatency("balanced", 0), "balanced");
        Check.equal(RealtimeAudioContext.LATENCY_PLAYBACK, RealtimeAudioContext.resolveLatency("playback", 0), "playback");
        Check.equal(RealtimeAudioContext.LATENCY_INTERACTIVE, RealtimeAudioContext.resolveLatency(null, 0.0), "a number below the range is clamped");
        Check.equal(RealtimeAudioContext.LATENCY_PLAYBACK, RealtimeAudioContext.resolveLatency(null, 5.0), "a number above the range is clamped");
        Check.equal(0.05, RealtimeAudioContext.resolveLatency(null, 0.05), "a number in the range is kept");
        RealtimeAudioContext ctx = new RealtimeAudioContext(48000f, 0.0, new ClockRenderDriver("swingwebpanel-audio-render-test"));
        Check.near(256.0 / 48000.0, ctx.getBaseLatency(), 1e-12, "at least two quanta");
        RealtimeAudioContext slow = new RealtimeAudioContext(48000f, 0.1, new ClockRenderDriver("swingwebpanel-audio-render-test"));
        double frames = slow.getBaseLatency() * 48000.0;
        Check.near(Math.rint(frames), frames, 1e-6, "a whole number of frames");
        Check.equal(0L, Math.round(frames) % 128, "a whole number of quanta");
        Check.isTrue(slow.getBaseLatency() >= 0.1 && slow.getBaseLatency() < 0.1 + 128.0 / 48000.0 + 1e-9, "rounded up: " + slow.getBaseLatency());
        Check.equal(0.0, slow.getOutputLatency(), "no device, no output latency");
        slow.reportOutputLatency(0.03);
        Check.near(0.03, slow.getOutputLatency(), 0, "a driver reports the output latency");
    }

    /**
     * The driver thread is a daemon with the product's name, is created by the first start only and ends when the context is closed; a context that
     * never ran has no thread.
     * <p>
     * 드라이버 스레드는 제품의 이름을 가진 데몬이고 첫 시작에서만 만들어지며 컨텍스트를 닫으면 끝난다. 한 번도 돌지 않은 컨텍스트는 스레드가 없다.
     */
    public void testDriverThread() {
        ClockRenderDriver driver = new ClockRenderDriver("swingwebpanel-audio-render-7");
        RealtimeAudioContext ctx = new RealtimeAudioContext(44100f, 0.0, driver);
        Check.isNull(driver.getThread(), "no thread before the first start");
        ctx.resume();
        Thread t = driver.getThread();
        Check.notNull(t, "a thread after the start");
        Check.equal("swingwebpanel-audio-render-7", t.getName(), "the name");
        Check.isTrue(t.isDaemon(), "a daemon");
        ctx.suspend();
        ctx.resume();
        Check.same(t, driver.getThread(), "the same thread after a restart");
        ctx.close();
        long deadline = System.currentTimeMillis() + 3000;
        // 스레드가 끝날 때까지(최대 3 초) 기다린다.
        while (t.isAlive() && System.currentTimeMillis() < deadline) {
            sleep(5);
        }
        Check.isFalse(t.isAlive(), "the thread ended with the context");
    }

    /**
     * Many contexts that are created, run and closed leave no thread behind.
     * <p>
     * 만들고 돌리고 닫은 많은 컨텍스트가 스레드를 남기지 않는다.
     */
    public void testNoThreadLeak() {
        Thread[] threads = new Thread[30];
        // 컨텍스트를 30 번 만들어 잠깐 돌리고 닫는다.
        for (int i = 0; i < threads.length; i++) {
            ClockRenderDriver d = new ClockRenderDriver("swingwebpanel-audio-render-" + i);
            RealtimeAudioContext c = new RealtimeAudioContext(44100f, 0.0, d);
            c.resume();
            threads[i] = d.getThread();
            sleep(2);
            c.close();
        }
        long deadline = System.currentTimeMillis() + 5000;
        boolean alive = true;
        // 모든 스레드가 끝날 때까지(최대 5 초) 기다린다.
        while (alive && System.currentTimeMillis() < deadline) {
            alive = false;
            // 스레드마다 아직 살아 있는지 확인한다.
            for (int i = 0; i < threads.length; i++) {
                alive |= threads[i].isAlive();
            }
            sleep(5);
        }
        Check.isFalse(alive, "every driver thread ended");
    }

    /**
     * <code>acquireBuffer</code> gives the node a snapshot that it plays although the buffer of the script changes afterwards, and it can be used more than
     * once (the once-only rule is the one of <code>setBuffer</code>).
     * <p>
     * <code>acquireBuffer</code> 는 노드에 스냅샷을 주어, 스크립트의 버퍼가 나중에 바뀌어도 그것을 재생하게 하며, 여러 번 쓸 수 있다(한 번만 정한다는 규칙은
     * <code>setBuffer</code> 의 것이다).
     */
    public void testAcquireBuffer() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        AudioBuffer content = new AudioBuffer(1, 128, WebAudioTestKit.SR);
        float[] data = content.getChannelData(0);
        // 버퍼를 0.5 로 채운다.
        for (int i = 0; i < data.length; i++) {
            data[i] = 0.5f;
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.acquireBuffer(AudioBuffer.wrap(new float[][] {data.clone()}, WebAudioTestKit.SR));
        // 원본을 바꿔도 스냅샷은 그대로이다.
        for (int i = 0; i < data.length; i++) {
            data[i] = -1f;
        }
        src.acquireBuffer(AudioBuffer.wrap(new float[][] {new float[] {0.25f, 0.25f, 0.25f, 0.25f}}, WebAudioTestKit.SR));
        src.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        Check.near(0.25, out[0], 0, "the last acquired snapshot plays");
        Check.near(0.25, out[3], 0, "its last frame");
        Check.near(0.0, out[4], 0, "then silence");
        Check.notNull(src.getBuffer(), "the node reports the snapshot it plays");
    }
}
