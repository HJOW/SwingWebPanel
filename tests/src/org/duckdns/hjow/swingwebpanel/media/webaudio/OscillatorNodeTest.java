package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the oscillator (Web Audio API 1.26, plan item P25-04): the sine against its closed form, the exact start and stop times, the
 * computed frequency with detune and with an audio input, the continuity of the phase when the frequency or the type changes, the
 * basic waveforms against their ideal shapes (the unnormalized series is summed independently in the test to find the normalization), the
 * band limiting of high frequencies and the errors of <code>start</code> and <code>stop</code>.
 * <p>
 * 발진기 시험(Web Audio API 1.26, 계획 P25-04): 사인파의 닫힌 식, 정확한 시작/정지 시간, 디튠과 오디오 입력이 있는 계산된 주파수, 주파수나 종류가 바뀔 때
 * 위상의 연속성, 기본 파형의 이상적인 모양(정규화를 구하려고 정규화하지 않은 급수를 시험에서 따로 합한다), 높은 주파수의 대역 제한, <code>start</code> 와
 * <code>stop</code> 의 오류.
 */
public class OscillatorNodeTest {

    /**
     * The sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private static final double SR = 8000.0;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public OscillatorNodeTest() {
    }

    /**
     * Renders an oscillator of a type and a frequency from time 0.
     * <p>
     * 종류와 주파수를 정한 발진기를 시간 0 부터 렌더링한다.
     *
     * @param type   the type / 종류
     * @param hz     the frequency / 주파수
     * @param frames the length / 길이
     * @return the samples / 샘플
     */
    private static float[] render(OscillatorType type, float hz, int frames) {
        OfflineAudioContext ctx = WebAudioTestKit.mono(frames);
        OscillatorNode osc = ctx.createOscillator();
        osc.setType(type);
        osc.getFrequency().setValue(hz);
        osc.connect(ctx.getDestination());
        osc.start(0);
        return WebAudioTestKit.render(ctx);
    }

    /**
     * Sums the full unnormalized series of a basic waveform on a table of 2048 samples and returns its maximum: the factor that the
     * specification divides by (section 1.28.5), computed here without the engine.
     * <p>
     * 기본 파형의 정규화하지 않은 전체 급수를 2048 샘플 표에서 합해 그 최댓값을 반환한다: 규격이 나누는 값(1.28.5절)을 엔진 없이 여기서 계산한다.
     *
     * @param type the type / 종류
     * @return the maximum of the series / 급수의 최댓값
     */
    private static double seriesMax(OscillatorType type) {
        int n = 2048;
        double max = 0;
        // 표의 점마다 부분음 1 ~ 1023 을 더한다.
        for (int i = 0; i < n; i++) {
            double x = 0;
            // 부분음 1 ~ 1023 의 사인 항을 더한다.
            for (int k = 1; k < n / 2; k++) {
                x += coefficient(type, k) * Math.sin(2 * Math.PI * k * i / n);
            }
            max = Math.max(max, Math.abs(x));
        }
        return max;
    }

    /**
     * The sine coefficient of the Fourier series of a basic waveform (section 1.28.6).
     * <p>
     * 기본 파형 푸리에 급수의 사인 계수(1.28.6절).
     *
     * @param type the type / 종류
     * @param k    the term / 항
     * @return the coefficient / 계수
     */
    private static double coefficient(OscillatorType type, int k) {
        switch (type) {
        case SINE:
            return k == 1 ? 1 : 0;
        case SQUARE:
            return 2.0 / (k * Math.PI) * (1 - ((k & 1) == 0 ? 1 : -1));
        case SAWTOOTH:
            return ((k & 1) == 1 ? 1 : -1) * 2.0 / (k * Math.PI);
        default:
            return 8 * Math.sin(k * Math.PI / 2) / (Math.PI * k * Math.PI * k);
        }
    }

    /**
     * A sine of 1000 Hz at 8000 Hz has 8 frames per period and reads the table at whole indices; a sine of 440 Hz is interpolated.
     * Both equal <code>sin(2 pi f n / sr)</code>.
     * <p>
     * 8000 Hz 에서 1000 Hz 사인은 주기당 8프레임이고 표를 정수 인덱스로 읽는다. 440 Hz 사인은 보간한다. 둘 다 <code>sin(2 pi f n / sr)</code> 와 같다.
     */
    public void testSineAgainstClosedForm() {
        float[] a = render(OscillatorType.SINE, 1000f, 1024);
        WebAudioTestKit.assertSignal("1000 Hz", a, 0, 1024, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 1000 * i / SR);
            }
        }, 2e-7);
        float[] b = render(OscillatorType.SINE, 440f, 1024);
        WebAudioTestKit.assertSignal("440 Hz", b, 0, 1024, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 440 * i / SR);
            }
        }, 3e-6);
        Check.near(0.0, a[0], 0, "the phase is zero at the start and the slope is positive");
        Check.isTrue(a[1] > 0, "positive slope");
    }

    /**
     * The start and the stop time are exact: the node plays the frames at or after the start time and before the stop time, and the
     * phase is zero at the exact start time, also between two frames.
     * <p>
     * 시작/정지 시간은 정확하다: 노드는 시작 시간 이후이고 정지 시간 이전인 프레임을 재생하고, 위상은 두 프레임 사이에 있어도 정확한 시작 시간에 0 이다.
     */
    public void testStartAndStopTimes() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(1000f);
        osc.connect(ctx.getDestination());
        osc.start(80.5 / SR);
        osc.stop(160.0 / SR);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("before the start time", out, 0, 81);
        WebAudioTestKit.assertSilent("from the stop time", out, 160, 512);
        // 시작 시간이 프레임 80.5 이므로 첫 프레임 81 의 위상은 0.5 프레임만큼 진행한 값이다.
        WebAudioTestKit.assertSignal("between", out, 81, 160, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 1000 * (i - 80.5) / SR);
            }
        }, 3e-6);
    }

    /**
     * <code>start</code> and <code>stop</code> check the state and the times; <code>stop(0)</code> stops at once; the
     * <code>ended</code> notification comes once.
     * <p>
     * <code>start</code> 와 <code>stop</code> 은 상태와 시간을 검사한다. <code>stop(0)</code> 은 즉시 멈춘다. <code>ended</code> 통지는 한 번 온다.
     */
    public void testStartStopRules() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        final OscillatorNode osc = ctx.createOscillator();
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                osc.stop(1.0);
            }
        }, "stop before start");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                osc.start(-0.5);
            }
        }, "negative start time");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                osc.start(Double.NaN);
            }
        }, "start time NaN");
        Check.isFalse(osc.isStarted(), "failed starts do not start the node");
        final List<String> events = new ArrayList<String>();
        osc.setEndedListener(new Runnable() {
            public void run() {
                events.add("ended");
            }
        });
        osc.connect(ctx.getDestination());
        osc.getFrequency().setValue(1000f);
        osc.start(0);
        Check.isTrue(osc.isStarted(), "started");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                osc.start(0);
            }
        }, "start twice");
        WebAudioTestKit.assertThrows(AudioException.Kind.RANGE, new Runnable() {
            public void run() {
                osc.stop(-1.0);
            }
        }, "negative stop time");
        osc.stop(1.0);
        osc.stop(0.0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("stop(0) stops at once", out, 0, 512);
        Check.equal(1, events.size(), "one ended notification");
    }

    /**
     * The computed frequency is <code>frequency * 2^(detune / 1200)</code>: 1200 cents double it, -1200 cents halve it, a negative
     * frequency inverts the phase, and an audio input is added to the frequency.
     * <p>
     * 계산된 주파수는 <code>frequency * 2^(detune / 1200)</code> 이다: 1200 센트는 두 배, -1200 센트는 절반이고, 음수 주파수는 위상을 뒤집으며, 오디오
     * 입력은 주파수에 더해진다.
     */
    public void testDetuneNegativeFrequencyAndInput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(500f);
        osc.getDetune().setValue(1200f);
        osc.connect(ctx.getDestination());
        osc.start(0);
        WebAudioTestKit.assertSignal("detune up", WebAudioTestKit.render(ctx), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 1000 * i / SR);
            }
        }, 3e-6);
        OfflineAudioContext c2 = WebAudioTestKit.mono(512);
        OscillatorNode o2 = c2.createOscillator();
        o2.getFrequency().setValue(1000f);
        o2.getDetune().setValue(-1200f);
        o2.connect(c2.getDestination());
        o2.start(0);
        WebAudioTestKit.assertSignal("detune down", WebAudioTestKit.render(c2), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 500 * i / SR);
            }
        }, 3e-6);
        OfflineAudioContext c3 = WebAudioTestKit.mono(512);
        OscillatorNode o3 = c3.createOscillator();
        o3.getFrequency().setValue(-1000f);
        o3.connect(c3.getDestination());
        o3.start(0);
        WebAudioTestKit.assertSignal("negative frequency", WebAudioTestKit.render(c3), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return -Math.sin(2 * Math.PI * 1000 * i / SR);
            }
        }, 3e-6);
        OfflineAudioContext c4 = WebAudioTestKit.mono(512);
        OscillatorNode o4 = c4.createOscillator();
        o4.getFrequency().setValue(440f);
        ConstantSourceNode add = c4.createConstantSource();
        add.getOffset().setValue(560f);
        add.connect(o4.getFrequency());
        add.start(0);
        o4.connect(c4.getDestination());
        o4.start(0);
        WebAudioTestKit.assertSignal("frequency input", WebAudioTestKit.render(c4), 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return Math.sin(2 * Math.PI * 1000 * i / SR);
            }
        }, 3e-6);
    }

    /**
     * The frequency is limited to the Nyquist frequency: 20000 Hz at 8000 Hz renders as 4000 Hz.
     * <p>
     * 주파수는 나이퀴스트 주파수로 제한된다: 8000 Hz 에서 20000 Hz 는 4000 Hz 로 렌더링된다.
     */
    public void testFrequencyIsLimitedToNyquist() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(64);
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(20000f);
        osc.connect(ctx.getDestination());
        osc.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 4000 Hz 사인은 정수 프레임에서 0 이다(sin(pi n)).
        for (int i = 0; i < 64; i++) {
            Check.near(0.0, out[i], 1e-5, "frame " + i);
        }
    }

    /**
     * The phase is the integral of the frequency, so an automated frequency continues the phase: the test integrates the frequency
     * itself.
     * <p>
     * 위상은 주파수의 적분이므로 자동화된 주파수는 위상을 이어 간다. 시험이 주파수를 직접 적분한다.
     */
    public void testFrequencyChangeKeepsThePhase() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValueAtTime(300f, 0);
        osc.getFrequency().setValueAtTime(900f, 413 / SR);
        osc.connect(ctx.getDestination());
        osc.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        double phase = 0;
        // 프레임마다 위상을 누적해 기대값을 만든다. 주파수는 프레임 413 에서 바뀐다.
        for (int i = 0; i < 1024; i++) {
            double f = i < 413 ? 300 : 900;
            Check.near(Math.sin(2 * Math.PI * phase), out[i], 5e-6, "frame " + i);
            phase += f / SR;
        }
    }

    /**
     * Changing the type keeps the phase: a sine of 250 Hz (32 frames per period) that becomes a square after 4 quanta keeps the same
     * zero crossings.
     * <p>
     * 종류를 바꿔도 위상은 유지된다: 4 quantum 뒤에 사각파가 되는 250 Hz 사인(주기당 32프레임)은 같은 영점 통과를 유지한다.
     */
    public void testTypeChangeKeepsThePhase() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(128 * 2);
        OscillatorNode osc = ctx.createOscillator();
        osc.getFrequency().setValue(250f);
        osc.connect(ctx.getDestination());
        osc.start(0);
        ctx.renderNextQuantum();
        osc.setType(OscillatorType.SQUARE);
        ctx.renderNextQuantum();
        float[] out = ctx.finishRendering().getChannelData(0);
        // 두 번째 quantum 의 사각파: 위상이 주기의 앞 절반(16 프레임)에서 양수, 뒤 절반에서 음수이다. 경계 근처는 대역 제한 때문에 제외한다.
        for (int i = 128 + 4; i < 256; i++) {
            int inPeriod = i % 32;
            if (inPeriod >= 4 && inPeriod < 12) {
                Check.isTrue(out[i] > 0.5, "positive half at frame " + i + " was " + out[i]);
            } else if (inPeriod >= 20 && inPeriod < 28) {
                Check.isTrue(out[i] < -0.5, "negative half at frame " + i + " was " + out[i]);
            }
        }
        Check.equal(OscillatorType.SQUARE, osc.getType(), "type");
    }

    /**
     * The basic waveforms follow their ideal shapes away from the discontinuities, scaled by the normalization factor that the
     * specification defines (the maximum of the full series): 100 Hz at 8000 Hz.
     * <p>
     * 기본 파형은 불연속점에서 떨어진 곳에서 이상적인 모양을 따르며 규격이 정한 정규화 계수(전체 급수의 최댓값)로 크기가 조정된다: 8000 Hz 에서 100 Hz.
     */
    public void testBasicWaveformsAgainstIdealShapes() {
        final OscillatorType[] types = {OscillatorType.SQUARE, OscillatorType.SAWTOOTH, OscillatorType.TRIANGLE};
        // 종류마다 한 주기(80 프레임)씩 열 주기를 렌더링해 이상적인 파형과 비교한다.
        for (int t = 0; t < types.length; t++) {
            final OscillatorType type = types[t];
            float[] out = render(type, 100f, 800);
            final double norm = 1.0 / seriesMax(type);
            int checked = 0;
            // 프레임마다 이상적인 값과 허용 오차를 비교한다.
            for (int i = 80; i < 800; i++) {
                double p = (i % 80) / 80.0;
                double ideal;
                int toEdge;
                if (type == OscillatorType.SQUARE) {
                    ideal = p < 0.5 ? 1 : -1;
                    toEdge = (int) Math.min(Math.abs(i % 80 - 0), Math.abs(i % 80 - 40));
                    toEdge = Math.min(toEdge, 80 - i % 80);
                } else if (type == OscillatorType.SAWTOOTH) {
                    ideal = p < 0.5 ? 2 * p : 2 * p - 2;
                    toEdge = Math.abs(i % 80 - 40);
                } else {
                    double q = p < 0.5 ? p : p - 0.5;
                    double tri = q < 0.25 ? 4 * q : 1 - 4 * (q - 0.25);
                    ideal = p < 0.5 ? tri : -tri;
                    toEdge = 100;
                }
                if (toEdge >= 10) {
                    // 삼각파는 부분음을 약 29개로 줄이면 꼭짓점 근처에서 최대 0.013 쯤 낮아진다.
                    double tol = type == OscillatorType.TRIANGLE ? 0.02 : 0.06;
                    Check.near(ideal * norm, out[i], tol, type + " frame " + i);
                    checked++;
                }
            }
            Check.isTrue(checked > 300, "enough frames were compared for " + type);
        }
    }

    /**
     * The normalization: the peak of a basic waveform is at most 1 and the amplitude of the fundamental of a sine is 1.
     * <p>
     * 정규화: 기본 파형의 최고점은 1 이하이고 사인파의 기본파 진폭은 1 이다.
     */
    public void testNormalization() {
        float[] sine = render(OscillatorType.SINE, 125f, 640);
        Check.near(1.0, WebAudioTestKit.peak(sine, 0, 640), 1e-4, "sine peak");
        float[] tri = render(OscillatorType.TRIANGLE, 100f, 800);
        Check.near(1.0, WebAudioTestKit.peak(tri, 0, 800), 2e-2, "triangle peak");
        float[] saw = render(OscillatorType.SAWTOOTH, 100f, 800);
        // 톱니파와 사각파는 불연속점의 깁스 오버슈트가 있어 최고점이 정규화한 1 근처이다(전체 급수의 최댓값으로 나누었으므로 1 을 크게 넘지 않는다).
        // 80 점으로 표본화하면 오버슈트의 꼭대기를 정확히 지나지 못하므로 최고점은 1 보다 조금 낮을 수 있다.
        double sawPeak = WebAudioTestKit.peak(saw, 0, 800);
        Check.isTrue(sawPeak > 0.9 && sawPeak < 1.03, "sawtooth peak is near 1 but was " + sawPeak);
        float[] sq = render(OscillatorType.SQUARE, 100f, 800);
        double sqPeak = WebAudioTestKit.peak(sq, 0, 800);
        Check.isTrue(sqPeak > 0.9 && sqPeak < 1.03, "square peak is near 1 but was " + sqPeak);
    }

    /**
     * A high frequency does not alias: a sawtooth of 3000 Hz at 8000 Hz has only its fundamental below the Nyquist frequency, so
     * there is nothing at the frequencies where the harmonics would fold back (6000 Hz to 2000 Hz, 9000 Hz to 1000 Hz); a sine near
     * the Nyquist frequency keeps its amplitude.
     * <p>
     * 높은 주파수는 에일리어싱하지 않는다: 8000 Hz 에서 3000 Hz 톱니파는 나이퀴스트 주파수 아래에 기본파만 있으므로 고조파가 접혀 들어올 주파수(6000 Hz 는
     * 2000 Hz 로, 9000 Hz 는 1000 Hz 로)에는 아무것도 없다. 나이퀴스트 주파수 가까운 사인은 진폭을 유지한다.
     */
    public void testHighFrequencyIsBandLimited() {
        float[] saw = render(OscillatorType.SAWTOOTH, 3000f, 2400);
        double fundamental = WebAudioTestKit.amplitudeAt(saw, 0, 2400, 3000, SR);
        double expected = (2.0 / Math.PI) / seriesMax(OscillatorType.SAWTOOTH);
        Check.near(expected, fundamental, 0.02, "fundamental of the sawtooth");
        Check.isTrue(WebAudioTestKit.amplitudeAt(saw, 0, 2400, 2000, SR) < 0.01, "no alias at 2000 Hz");
        Check.isTrue(WebAudioTestKit.amplitudeAt(saw, 0, 2400, 1000, SR) < 0.01, "no alias at 1000 Hz");
        float[] sine = render(OscillatorType.SINE, 3900f, 4000);
        Check.near(1.0, WebAudioTestKit.amplitudeAt(sine, 0, 4000, 3900, SR), 0.02, "sine near the Nyquist frequency");
    }

    /**
     * A custom waveform from Fourier coefficients: it is normalized to a peak of 1 (the normalization is computed independently by
     * summing the series), or keeps the amplitude of the coefficients when the normalization is disabled; the type becomes
     * <code>custom</code>.
     * <p>
     * 푸리에 계수로 만든 사용자 파형: 최고점이 1 이 되도록 정규화되며(정규화는 급수를 따로 합해 독립적으로 계산한다), 정규화를 끄면 계수의 진폭을 유지한다.
     * 종류는 <code>custom</code> 이 된다.
     */
    public void testCustomPeriodicWave() {
        float[] real = {0f, 0f, 0.5f};
        float[] imag = {0f, 1f, 0f};
        // 정규화 계수: 한 주기를 2048 점으로 나눈 급수 x = 0.5 cos(4 pi t) + sin(2 pi t) 의 최댓값.
        double max = 0;
        for (int i = 0; i < 2048; i++) {
            double t = i / 2048.0;
            max = Math.max(max, Math.abs(0.5 * Math.cos(4 * Math.PI * t) + Math.sin(2 * Math.PI * t)));
        }
        final double norm = max;
        OfflineAudioContext ctx = WebAudioTestKit.mono(640);
        OscillatorNode osc = ctx.createOscillator();
        osc.setPeriodicWave(ctx.createPeriodicWave(real, imag, false));
        osc.getFrequency().setValue(125f);
        osc.connect(ctx.getDestination());
        osc.start(0);
        Check.equal(OscillatorType.CUSTOM, osc.getType(), "type is custom");
        WebAudioTestKit.assertSignal("normalized", WebAudioTestKit.render(ctx), 0, 640, new WebAudioTestKit.Signal() {
            public double at(int i) {
                double t = 125.0 * i / SR;
                return (0.5 * Math.cos(4 * Math.PI * t) + Math.sin(2 * Math.PI * t)) / norm;
            }
        }, 1e-5);
        OfflineAudioContext c2 = WebAudioTestKit.mono(640);
        OscillatorNode o2 = c2.createOscillator();
        o2.setPeriodicWave(c2.createPeriodicWave(real, imag, true));
        o2.getFrequency().setValue(125f);
        o2.connect(c2.getDestination());
        o2.start(0);
        WebAudioTestKit.assertSignal("not normalized", WebAudioTestKit.render(c2), 0, 640, new WebAudioTestKit.Signal() {
            public double at(int i) {
                double t = 125.0 * i / SR;
                return 0.5 * Math.cos(4 * Math.PI * t) + Math.sin(2 * Math.PI * t);
            }
        }, 1e-5);
    }

    /**
     * Setting the type to <code>custom</code> is an <code>InvalidStateError</code>; the default type is <code>sine</code>.
     * <p>
     * 종류를 <code>custom</code> 으로 정하면 <code>InvalidStateError</code> 이다. 기본 종류는 <code>sine</code> 이다.
     */
    public void testTypeRules() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        final OscillatorNode osc = ctx.createOscillator();
        Check.equal(OscillatorType.SINE, osc.getType(), "default type");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                osc.setType(OscillatorType.CUSTOM);
            }
        }, "custom type");
        Check.equal(1, osc.getNumberOfOutputs(), "one output");
        Check.equal(0, osc.getNumberOfInputs(), "no input");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                osc.setPeriodicWave(null);
            }
        }, "null wave");
    }
}
