package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the state and the limits of the engine (plan item P25-02): the <code>AudioBuffer</code> rules, the exact mapping of times to
 * frames, the <code>[[current value]]</code> of a parameter that follows the rendering, a parameter that drives its own node (a cycle that
 * is muted), the sine at the supported sample rates (3000 to 768000 Hz, the three table sizes), and a control thread that connects, disconnects
 * and automates while another thread renders (no exception, no deadlock, finite output).
 * <p>
 * 엔진의 상태와 제한 시험(계획 P25-02): <code>AudioBuffer</code> 규칙, 시간에서 프레임으로의 정확한 대응, 렌더링을 따라가는 파라미터의
 * <code>[[current value]]</code>, 자기 노드를 구동하는 파라미터(무음 처리되는 순환), 지원하는 샘플링 주파수(3000~768000 Hz, 세 가지 표 크기)에서의 사인파,
 * 다른 스레드가 렌더링하는 동안 연결하고 끊고 자동화하는 제어 스레드(예외 없음, 교착 없음, 유한한 출력).
 */
public class EngineStateTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public EngineStateTest() {
    }

    /**
     * The constructor of the buffer checks all arguments; the copy methods honour the offsets.
     * <p>
     * 버퍼의 생성자는 모든 인수를 검사한다. 복사 메서드는 시작 위치를 따른다.
     */
    public void testAudioBufferRules() {
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(0, 10, 8000f);
            }
        }, "0 channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(33, 10, 8000f);
            }
        }, "33 channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(1, 0, 8000f);
            }
        }, "length 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(1, 10, 2999f);
            }
        }, "sample rate 2999");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(1, 10, 768001f);
            }
        }, "sample rate 768001");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(1, 10, Float.NaN);
            }
        }, "sample rate NaN");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(32, (WebAudioLimits.MAX_BUFFER_SAMPLES / 32) + 1, 8000f);
            }
        }, "more samples than the limit");
        final AudioBuffer b = new AudioBuffer(2, 100, 4000f);
        Check.equal(2, b.getNumberOfChannels(), "channels");
        Check.equal(100, b.getLength(), "length");
        Check.near(0.025, b.getDuration(), 1e-12, "duration");
        Check.near(4000.0, b.getSampleRate(), 0, "sample rate");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.getChannelData(2);
            }
        }, "channel out of range");
        float[] src = {1f, 2f, 3f};
        b.copyToChannel(src, 1, 98);
        Check.near(1.0, b.getChannelData(1)[98], 0, "copied from the offset");
        Check.near(2.0, b.getChannelData(1)[99], 0, "copy stops at the end of the channel");
        float[] dst = new float[5];
        b.copyFromChannel(dst, 1, 97);
        Check.near(0.0, dst[0], 0, "copied from the offset of the channel");
        Check.near(1.0, dst[1], 0, "second");
        Check.near(2.0, dst[2], 0, "third");
        Check.near(0.0, dst[3], 0, "the rest of the destination is untouched");
        // 규격 1.4.3: 시작이 길이와 같거나 뒤여도 오류가 아니며 아무것도 복사하지 않는다(P25-05 에서 바로잡았다).
        float[] untouched = {7f};
        b.copyFromChannel(untouched, 0, 100);
        Check.near(7.0, untouched[0], 0, "an offset at the length copies nothing and is not an error");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.copyToChannel(new float[1], 5, 0);
            }
        }, "channel out of range for a copy");
    }

    /**
     * A time maps to the first frame whose time is at or after it, exactly: every frame time <code>k / sampleRate</code> maps to
     * <code>k</code> and the next representable time maps to <code>k + 1</code>, for several sample rates.
     * <p>
     * 시간은 그 시간 이후(같은 시간 포함)인 첫 프레임에 정확히 대응한다: 모든 프레임 시간 <code>k / sampleRate</code> 는 <code>k</code> 에, 그 다음 표현
     * 가능한 시간은 <code>k + 1</code> 에 대응한다. 여러 샘플링 주파수에서.
     */
    public void testTimeToFrameMapping() {
        double[] rates = {3000.0, 8000.0, 44100.0, 48000.0, 96000.0, 768000.0};
        // 샘플링 주파수마다 프레임 경계의 시간을 확인한다.
        for (int r = 0; r < rates.length; r++) {
            for (long k = 0; k < 200000; k += 997) {
                double t = k / rates[r];
                Check.equal(k, AudioScheduledSourceNode.firstFrameAtOrAfter(t, rates[r]), "frame time " + k + " at " + rates[r]);
                Check.equal(k + 1, AudioScheduledSourceNode.firstFrameAtOrAfter(Math.nextUp(t), rates[r]), "just after frame " + k + " at " + rates[r]);
            }
        }
        Check.equal(0L, AudioScheduledSourceNode.firstFrameAtOrAfter(0.0, 8000.0), "time 0");
        Check.equal(0L, AudioScheduledSourceNode.firstFrameAtOrAfter(-5.0, 8000.0), "negative time");
        Check.equal(Long.MAX_VALUE, AudioScheduledSourceNode.firstFrameAtOrAfter(Double.POSITIVE_INFINITY, 8000.0), "infinite time");
        Check.equal(Long.MAX_VALUE, AudioScheduledSourceNode.firstFrameAtOrAfter(1e300, 8000.0), "a time that is too large");
    }

    /**
     * The value of a parameter that is rendered is the intrinsic value at the beginning of the last quantum: a ramp from 0 to 1 over
     * two quanta reads 0, 0.5 and 1 after the first, second and third quantum.
     * <p>
     * 렌더링되는 파라미터의 값은 마지막 quantum 시작에서의 고유 값이다: 두 quantum 에 걸친 0 에서 1 로의 램프는 첫째, 둘째, 셋째 quantum 뒤에 0, 0.5, 1 이다.
     */
    public void testParamValueFollowsTheRendering() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 128 * 5, 8000f);
        GainNode g = ctx.createGain();
        ConstantSourceNode c = ctx.createConstantSource();
        c.connect(g);
        g.connect(ctx.getDestination());
        c.start(0);
        g.getGain().setValueAtTime(0f, 0);
        g.getGain().linearRampToValueAtTime(1f, 256 / 8000.0);
        ctx.renderNextQuantum();
        Check.near(0.0, g.getGain().getValue(), 1e-7, "after the first quantum");
        ctx.renderNextQuantum();
        Check.near(0.5, g.getGain().getValue(), 1e-6, "after the second quantum");
        ctx.renderNextQuantum();
        Check.near(1.0, g.getGain().getValue(), 1e-7, "after the third quantum");
    }

    /**
     * A node whose own output drives one of its parameters is a cycle without a delay and is muted.
     * <p>
     * 자기 출력이 자기 파라미터를 구동하는 노드는 지연 없는 순환이며 무음 처리된다.
     */
    public void testParamCycleIsMuted() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        OscillatorNode osc = ctx.createOscillator();
        osc.connect(osc.getFrequency());
        osc.connect(ctx.getDestination());
        osc.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("a muted oscillator", out, 0, 256);
        Check.equal(1, ctx.getMutedNodeCount(), "one node is muted");
        OfflineAudioContext c2 = WebAudioTestKit.mono(256);
        OscillatorNode lfo = c2.createOscillator();
        lfo.getFrequency().setValue(1f);
        GainNode depth = c2.createGain();
        depth.getGain().setValue(100f);
        OscillatorNode carrier = c2.createOscillator();
        lfo.connect(depth);
        depth.connect(carrier.getFrequency());
        carrier.connect(c2.getDestination());
        lfo.start(0);
        carrier.start(0);
        float[] fm = WebAudioTestKit.render(c2);
        Check.equal(0, c2.getMutedNodeCount(), "a modulation chain is not a cycle");
        Check.isTrue(WebAudioTestKit.peak(fm, 0, 256) > 0.5, "the carrier is heard");
    }

    /**
     * A sine of 1000 Hz equals its closed form at the supported sample rates: 3000 Hz (2048 table), 44100 and 48000 Hz (4096 table),
     * 96000 and 768000 Hz (16384 table).
     * <p>
     * 1000 Hz 사인은 지원하는 샘플링 주파수에서 닫힌 식과 같다: 3000 Hz(표 2048), 44100 Hz 와 48000 Hz(표 4096), 96000 Hz 와 768000 Hz(표 16384).
     */
    public void testSineAtSupportedSampleRates() {
        float[] rates = {3000f, 44100f, 48000f, 96000f, 768000f};
        // 샘플링 주파수마다 주파수 437 Hz 사인을 렌더링해 닫힌 식과 비교한다(주파수는 나이퀴스트 아래).
        for (int r = 0; r < rates.length; r++) {
            final double sr = rates[r];
            final double hz = r == 0 ? 437.0 : 1000.0;
            OfflineAudioContext ctx = new OfflineAudioContext(1, 1024, rates[r]);
            OscillatorNode osc = ctx.createOscillator();
            osc.getFrequency().setValue((float) hz);
            osc.connect(ctx.getDestination());
            osc.start(0);
            WebAudioTestKit.assertSignal("sine at " + sr, WebAudioTestKit.render(ctx), 0, 1024, new WebAudioTestKit.Signal() {
                public double at(int i) {
                    return Math.sin(2 * Math.PI * hz * i / sr);
                }
            }, 3e-6);
        }
    }

    /**
     * A control thread that connects, disconnects, creates nodes and automates parameters while another thread renders: nothing throws
     * but the expected argument errors, nothing deadlocks and the output is finite. The test also checks that the render thread
     * applied the control messages (the rendering produced sound).
     * <p>
     * 다른 스레드가 렌더링하는 동안 연결하고, 끊고, 노드를 만들고, 파라미터를 자동화하는 제어 스레드: 예상한 인수 오류 말고는 던지지 않고, 교착이 없고, 출력은
     * 유한하다. 렌더링 스레드가 제어 메시지를 적용했는지(소리가 났는지)도 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testControlThreadRacesTheRenderingThread() throws Exception {
        final OfflineAudioContext ctx = new OfflineAudioContext(2, 128 * 1500, 8000f);
        final OscillatorNode osc = ctx.createOscillator();
        final GainNode gain = ctx.createGain();
        final StereoPannerNode pan = ctx.createStereoPanner();
        final DelayNode delay = ctx.createDelay(0.5);
        final BiquadFilterNode filter = ctx.createBiquadFilter();
        osc.connect(gain);
        gain.connect(pan);
        pan.connect(ctx.getDestination());
        osc.start(0);
        final Throwable[] failure = new Throwable[1];
        Thread renderer = new Thread(new Runnable() {
            public void run() {
                try {
                    // 렌더링 스레드는 전체 길이를 채울 때까지 quantum 을 하나씩 렌더링한다.
                    while (!ctx.isComplete()) {
                        ctx.renderNextQuantum();
                    }
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        renderer.start();
        Random random = new Random(42);
        int ops = 0;
        int extras = 0;
        // 렌더링이 끝날 때까지 제어 스레드가 무작위 작업을 계속한다. 렌더링 스레드와 번갈아 돌도록 가끔 잠시 쉰다.
        while (renderer.isAlive() && ops < 30000) {
            ops++;
            if ((ops & 15) == 0) {
                Thread.sleep(1);
            }
            double now = ctx.getCurrentTime();
            try {
                switch (random.nextInt(10)) {
                case 0:
                    gain.connect(delay);
                    delay.connect(pan);
                    break;
                case 1:
                    gain.disconnect(delay);
                    break;
                case 2:
                    gain.getGain().setValueAtTime(random.nextFloat(), now + random.nextDouble() * 0.05);
                    break;
                case 3:
                    gain.getGain().linearRampToValueAtTime(random.nextFloat(), now + random.nextDouble() * 0.1);
                    break;
                case 4:
                    osc.getFrequency().setValue(100 + random.nextInt(2000));
                    break;
                case 5:
                    gain.getGain().cancelScheduledValues(now);
                    break;
                case 6:
                    osc.connect(filter);
                    filter.connect(pan);
                    filter.getFrequency().setTargetAtTime(random.nextInt(3000), now, 0.01f);
                    break;
                case 7:
                    osc.disconnect(filter);
                    break;
                case 8:
                    // 연결된 게인이 너무 많아지지 않도록 200개까지만 만든다.
                    if (extras < 200) {
                        GainNode extra = ctx.createGain();
                        extras++;
                        osc.connect(extra);
                        extra.connect(ctx.getDestination());
                    }
                    break;
                default:
                    pan.getPan().cancelAndHoldAtTime(now);
                    pan.getPan().setValueAtTime(random.nextFloat() * 2 - 1, now);
                    break;
                }
            } catch (AudioException expected) {
                // 연결이 없는 노드를 끊는 것 등은 InvalidAccessError 로 정상이다.
                Check.isTrue(expected.getKind() == AudioException.Kind.INVALID_ACCESS || expected.getKind() == AudioException.Kind.NOT_SUPPORTED,
                        "unexpected " + expected);
            }
        }
        renderer.join(30000);
        Check.isFalse(renderer.isAlive(), "the rendering thread finished (no deadlock)");
        Check.isNull(failure[0], "the rendering thread threw " + failure[0]);
        AudioBuffer out = ctx.finishRendering();
        double peak = 0;
        // 출력이 모두 유한한 값인지 확인하고 최고점을 구한다.
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < out.getLength(); i++) {
                float v = out.getChannelData(c)[i];
                Check.isTrue(!Float.isNaN(v) && !Float.isInfinite(v), "finite output at " + c + "/" + i);
                peak = Math.max(peak, Math.abs(v));
            }
        }
        Check.isTrue(peak > 0.01, "the rendering produced sound, the control messages were applied (peak " + peak + ")");
    }
}
