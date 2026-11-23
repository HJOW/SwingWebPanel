package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the biquad filter (Web Audio API 1.13, plan item P25-04). The frequency response is checked against the analytic values that
 * the design of the filters of the specification guarantees: at the centre frequency the low-pass and the high-pass have the gain
 * <code>10^(Q/20)</code>, the band-pass has the gain 1, the notch 0, the all-pass has the gain 1 everywhere and the phase of -pi, the
 * peaking filter has <code>A^2</code>, the shelves have <code>A^2</code> at the one end and 1 at the other (with <code>A = 10^(gain/40)</code>);
 * the processing is checked against the sine response and against the difference equation that the test evaluates itself with the
 * coefficients of the specification; the automation of the parameters is applied per frame; the tail time keeps a resonant filter
 * ringing after its input ended and then the node becomes inactive.
 * <p>
 * 쌍이차 필터 시험(Web Audio API 1.13, 계획 P25-04). 주파수 응답은 규격 필터 설계가 보장하는 해석적 값과 비교한다: 중심 주파수에서 저역/고역 통과는 이득
 * <code>10^(Q/20)</code>, 대역 통과는 이득 1, 노치는 0, 전역 통과는 어디서나 이득 1 이고 위상 -pi, 피킹 필터는 <code>A^2</code>, 선반은 한쪽 끝에서
 * <code>A^2</code> 다른 쪽에서 1 이다(<code>A = 10^(gain/40)</code>). 처리는 사인 응답과, 시험이 규격의 계수로 직접 계산한 차분 방정식과 비교한다.
 * 파라미터 자동화는 프레임마다 적용되고, 꼬리 시간은 공진이 큰 필터가 입력이 끝난 뒤에도 울리게 한 뒤 노드가 비활성이 된다.
 */
public class BiquadFilterNodeTest {

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
    public BiquadFilterNodeTest() {
    }

    /**
     * Calculates the response of a filter at a frequency.
     * <p>
     * 필터의 한 주파수에서의 응답을 계산한다.
     *
     * @param type  the type / 종류
     * @param hz    the centre frequency / 중심 주파수
     * @param q     the Q / Q
     * @param gain  the gain in dB / 이득(dB)
     * @param at    the frequency of the response / 응답을 구할 주파수
     * @return the magnitude at index 0 and the phase at index 1 / 크기(0번)와 위상(1번)
     */
    private static double[] response(BiquadFilterType type, float hz, float q, float gain, float at) {
        OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        BiquadFilterNode f = ctx.createBiquadFilter();
        f.setType(type);
        f.getFrequency().setValue(hz);
        f.getQ().setValue(q);
        f.getGain().setValue(gain);
        float[] mag = new float[1];
        float[] ph = new float[1];
        f.getFrequencyResponse(new float[] {at}, mag, ph);
        return new double[] {mag[0], ph[0]};
    }

    /**
     * The low-pass filter has the gain 1 at 0 Hz, <code>10^(Q/20)</code> at the cutoff and 0 at the Nyquist frequency; the high-pass
     * is the mirror image.
     * <p>
     * 저역 통과 필터는 0 Hz 에서 이득 1, 차단 주파수에서 <code>10^(Q/20)</code>, 나이퀴스트 주파수에서 0 이다. 고역 통과는 그 대칭이다.
     */
    public void testLowpassAndHighpassResponse() {
        double qLin = Math.pow(10, 6.0 / 20);
        Check.near(1.0, response(BiquadFilterType.LOWPASS, 1000f, 6f, 0f, 0f)[0], 1e-5, "lowpass DC");
        Check.near(qLin, response(BiquadFilterType.LOWPASS, 1000f, 6f, 0f, 1000f)[0], 1e-4, "lowpass at the cutoff");
        Check.near(0.0, response(BiquadFilterType.LOWPASS, 1000f, 6f, 0f, 4000f)[0], 1e-5, "lowpass at the Nyquist frequency");
        Check.near(0.0, response(BiquadFilterType.LOWPASS, 1000f, 6f, 0f, 0f)[1], 1e-6, "lowpass phase at DC");
        Check.near(0.0, response(BiquadFilterType.HIGHPASS, 1000f, 6f, 0f, 0f)[0], 1e-5, "highpass DC");
        Check.near(qLin, response(BiquadFilterType.HIGHPASS, 1000f, 6f, 0f, 1000f)[0], 1e-4, "highpass at the cutoff");
        Check.near(1.0, response(BiquadFilterType.HIGHPASS, 1000f, 6f, 0f, 4000f)[0], 1e-5, "highpass at the Nyquist frequency");
        // Q = 0 dB 이면 Q' = 1 이므로 차단 주파수의 이득이 정확히 1/sqrt(2) 가 아니라 1 이다.
        Check.near(1.0, response(BiquadFilterType.LOWPASS, 1000f, 0f, 0f, 1000f)[0], 1e-4, "Q of 0 dB gives a gain of 1 at the cutoff");
    }

    /**
     * Band-pass: gain 1 at the centre and 0 at both ends; notch: 0 at the centre and 1 at both ends; all-pass: gain 1 everywhere and
     * a phase of pi (in magnitude) at the centre.
     * <p>
     * 대역 통과: 중심에서 이득 1, 양 끝에서 0. 노치: 중심에서 0, 양 끝에서 1. 전역 통과: 어디서나 이득 1 이고 중심에서 위상의 크기가 pi.
     */
    public void testBandpassNotchAllpassResponse() {
        Check.near(1.0, response(BiquadFilterType.BANDPASS, 1500f, 5f, 0f, 1500f)[0], 1e-5, "bandpass at the centre");
        Check.near(0.0, response(BiquadFilterType.BANDPASS, 1500f, 5f, 0f, 1500f)[1], 1e-5, "bandpass phase at the centre");
        Check.near(0.0, response(BiquadFilterType.BANDPASS, 1500f, 5f, 0f, 0f)[0], 1e-6, "bandpass at DC");
        Check.near(0.0, response(BiquadFilterType.BANDPASS, 1500f, 5f, 0f, 4000f)[0], 1e-5, "bandpass at the Nyquist frequency");
        Check.near(0.0, response(BiquadFilterType.NOTCH, 1500f, 5f, 0f, 1500f)[0], 1e-5, "notch at the centre");
        Check.near(1.0, response(BiquadFilterType.NOTCH, 1500f, 5f, 0f, 0f)[0], 1e-5, "notch at DC");
        Check.near(1.0, response(BiquadFilterType.NOTCH, 1500f, 5f, 0f, 4000f)[0], 1e-5, "notch at the Nyquist frequency");
        float[] freqs = {0f, 300f, 1000f, 1500f, 2500f, 4000f};
        // 전역 통과의 이득은 모든 주파수에서 1 이다.
        for (int i = 0; i < freqs.length; i++) {
            Check.near(1.0, response(BiquadFilterType.ALLPASS, 1500f, 5f, 0f, freqs[i])[0], 1e-5, "allpass gain at " + freqs[i]);
        }
        Check.near(Math.PI, Math.abs(response(BiquadFilterType.ALLPASS, 1500f, 5f, 0f, 1500f)[1]), 1e-4, "allpass phase at the centre");
    }

    /**
     * The peaking filter has the gain <code>A^2</code> at the centre and 1 elsewhere far away; the shelves have <code>A^2</code> at
     * one end, 1 at the other and <code>A</code> at the corner frequency.
     * <p>
     * 피킹 필터는 중심에서 이득 <code>A^2</code> 이고 멀리서는 1 이다. 선반은 한쪽 끝에서 <code>A^2</code>, 다른 쪽에서 1, 모서리 주파수에서 <code>A</code> 이다.
     */
    public void testPeakingAndShelfResponse() {
        double a2 = Math.pow(10, 12.0 / 20);
        double a1 = Math.pow(10, 12.0 / 40);
        Check.near(a2, response(BiquadFilterType.PEAKING, 1500f, 2f, 12f, 1500f)[0], 1e-4, "peaking centre");
        Check.near(1.0, response(BiquadFilterType.PEAKING, 1500f, 2f, 12f, 0f)[0], 1e-5, "peaking DC");
        Check.near(1.0, response(BiquadFilterType.PEAKING, 1500f, 2f, 12f, 4000f)[0], 1e-5, "peaking Nyquist");
        Check.near(a2, response(BiquadFilterType.LOWSHELF, 1000f, 1f, 12f, 0f)[0], 1e-4, "lowshelf DC");
        Check.near(1.0, response(BiquadFilterType.LOWSHELF, 1000f, 1f, 12f, 4000f)[0], 1e-4, "lowshelf Nyquist");
        Check.near(a1, response(BiquadFilterType.LOWSHELF, 1000f, 1f, 12f, 1000f)[0], 1e-4, "lowshelf corner");
        Check.near(1.0, response(BiquadFilterType.HIGHSHELF, 1000f, 1f, 12f, 0f)[0], 1e-4, "highshelf DC");
        Check.near(a2, response(BiquadFilterType.HIGHSHELF, 1000f, 1f, 12f, 4000f)[0], 1e-4, "highshelf Nyquist");
        Check.near(a1, response(BiquadFilterType.HIGHSHELF, 1000f, 1f, 12f, 1000f)[0], 1e-4, "highshelf corner");
        Check.near(1.0 / a2, response(BiquadFilterType.PEAKING, 1500f, 2f, -12f, 1500f)[0], 1e-4, "negative gain cuts");
    }

    /**
     * <code>detune</code> shifts the frequency of the response (1200 cents are an octave); frequencies outside 0 to the Nyquist
     * frequency give NaN; arrays of different lengths are an <code>InvalidAccessError</code>.
     * <p>
     * <code>detune</code> 은 응답의 주파수를 옮긴다(1200 센트는 한 옥타브). 0 에서 나이퀴스트 주파수까지의 밖은 NaN 을 주고, 길이가 다른 배열은
     * <code>InvalidAccessError</code> 이다.
     */
    public void testResponseDetuneAndRanges() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        final BiquadFilterNode f = ctx.createBiquadFilter();
        f.setType(BiquadFilterType.BANDPASS);
        f.getFrequency().setValue(500f);
        f.getDetune().setValue(1200f);
        f.getQ().setValue(5f);
        float[] mag = new float[3];
        float[] ph = new float[3];
        f.getFrequencyResponse(new float[] {1000f, -1f, 4001f}, mag, ph);
        Check.near(1.0, mag[0], 1e-5, "the centre is 500 Hz detuned by an octave");
        Check.isTrue(Float.isNaN(mag[1]) && Float.isNaN(ph[1]), "a negative frequency gives NaN");
        Check.isTrue(Float.isNaN(mag[2]) && Float.isNaN(ph[2]), "a frequency above the Nyquist frequency gives NaN");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                f.getFrequencyResponse(new float[3], new float[2], new float[3]);
            }
        }, "different lengths");
        Check.equal(BiquadFilterType.LOWPASS, ctx.createBiquadFilter().getType(), "the default type is lowpass");
    }

    /**
     * A filtered sine has the amplitude of the response at its frequency, for every type (the response is measured independently by
     * the Fourier sum over whole periods after the filter has settled).
     * <p>
     * 필터링한 사인은 모든 종류에서 그 주파수의 응답 크기를 진폭으로 가진다(응답은 필터가 자리 잡은 뒤 주기의 정수 배 구간의 푸리에 합으로 독립적으로
     * 측정한다).
     */
    public void testSteadyStateMatchesTheResponse() {
        BiquadFilterType[] types = BiquadFilterType.values();
        // 종류마다 1000 Hz 사인을 필터링해 진폭을 응답과 비교한다.
        for (int t = 0; t < types.length; t++) {
            OfflineAudioContext ctx = WebAudioTestKit.mono(8000);
            OscillatorNode osc = ctx.createOscillator();
            osc.getFrequency().setValue(1000f);
            BiquadFilterNode f = ctx.createBiquadFilter();
            f.setType(types[t]);
            f.getFrequency().setValue(1200f);
            f.getQ().setValue(3f);
            f.getGain().setValue(9f);
            osc.connect(f);
            f.connect(ctx.getDestination());
            osc.start(0);
            float[] out = WebAudioTestKit.render(ctx);
            float[] mag = new float[1];
            float[] ph = new float[1];
            f.getFrequencyResponse(new float[] {1000f}, mag, ph);
            double measured = WebAudioTestKit.amplitudeAt(out, 4000, 8000, 1000, SR);
            Check.near(mag[0], measured, 2e-3, types[t].getScriptName() + " steady state gain");
        }
    }

    /**
     * The output equals the difference equation with the coefficients of the specification (low-pass, written out in the test)
     * for an input that has all frequencies.
     * <p>
     * 모든 주파수를 가진 입력에 대해 출력은 규격의 계수(저역 통과, 시험에 직접 적었다)를 쓴 차분 방정식과 같다.
     */
    public void testDifferenceEquation() {
        final int n = 600;
        AudioBuffer noise = new AudioBuffer(1, n, 8000f);
        java.util.Random r = new java.util.Random(11);
        // 입력은 -1 ~ 1 의 임의 값이다.
        for (int i = 0; i < n; i++) {
            noise.getChannelData(0)[i] = r.nextFloat() * 2 - 1;
        }
        OfflineAudioContext ctx = WebAudioTestKit.mono(n);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(noise);
        BiquadFilterNode f = ctx.createBiquadFilter();
        f.getFrequency().setValue(900f);
        f.getQ().setValue(4f);
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 규격의 저역 통과 계수: w0 = 2 pi f / Fs, alphaQ(dB) = sin(w0) / (2 10^(Q/20)).
        double w0 = 2 * Math.PI * 900 / SR;
        double alpha = Math.sin(w0) / (2 * Math.pow(10, 4.0 / 20));
        double b0 = (1 - Math.cos(w0)) / 2;
        double b1 = 1 - Math.cos(w0);
        double b2 = b0;
        double a0 = 1 + alpha;
        double a1 = -2 * Math.cos(w0);
        double a2 = 1 - alpha;
        double x1 = 0;
        double x2 = 0;
        double y1 = 0;
        double y2 = 0;
        // a0 y(n) + a1 y(n-1) + a2 y(n-2) = b0 x(n) + b1 x(n-1) + b2 x(n-2)
        // 프레임마다 차분 방정식을 풀어 비교한다.
        for (int i = 0; i < n; i++) {
            double x = noise.getChannelData(0)[i];
            double y = (b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0;
            Check.near(y, out[i], 1e-5, "frame " + i);
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
        }
    }

    /**
     * The parameters are a-rate: a ramp of the frequency changes the coefficients for every frame; the test evaluates the difference
     * equation with the coefficients of every frame.
     * <p>
     * 파라미터는 a-rate 이다: 주파수 램프는 프레임마다 계수를 바꾼다. 시험은 프레임마다의 계수로 차분 방정식을 계산한다.
     */
    public void testFrequencyAutomationIsAppliedPerFrame() {
        final int n = 512;
        AudioBuffer noise = new AudioBuffer(1, n, 8000f);
        java.util.Random r = new java.util.Random(5);
        // 입력은 임의 값이다.
        for (int i = 0; i < n; i++) {
            noise.getChannelData(0)[i] = r.nextFloat() * 2 - 1;
        }
        OfflineAudioContext ctx = WebAudioTestKit.mono(n);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(noise);
        BiquadFilterNode f = ctx.createBiquadFilter();
        f.getQ().setValue(2f);
        f.getFrequency().setValueAtTime(200f, 0);
        f.getFrequency().linearRampToValueAtTime(2000f, n / SR);
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        double x1 = 0;
        double x2 = 0;
        double y1 = 0;
        double y2 = 0;
        // 프레임마다 주파수 f(i) = 200 + 1800 i / n 의 저역 통과 계수로 차분 방정식을 계산한다.
        for (int i = 0; i < n; i++) {
            double fr = 200 + 1800.0 * i / n;
            double w0 = 2 * Math.PI * fr / SR;
            double alpha = Math.sin(w0) / (2 * Math.pow(10, 2.0 / 20));
            double b0 = (1 - Math.cos(w0)) / 2;
            double a0 = 1 + alpha;
            double a1 = -2 * Math.cos(w0);
            double a2 = 1 - alpha;
            double x = noise.getChannelData(0)[i];
            double y = (b0 * x + 2 * b0 * x1 + b0 * x2 - a1 * y1 - a2 * y2) / a0;
            Check.near(y, out[i], 2e-5, "frame " + i);
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
        }
    }

    /**
     * Edge values: a low-pass of 0 Hz passes nothing, a low-pass at the Nyquist frequency or above passes everything, a band-pass with
     * Q 0 passes everything, and no value produces a NaN.
     * <p>
     * 경계값: 0 Hz 저역 통과는 아무것도 통과시키지 않고, 나이퀴스트 주파수 이상의 저역 통과는 모두 통과시키며, Q 가 0 인 대역 통과는 모두 통과시키고,
     * 어떤 값에서도 NaN 이 생기지 않는다.
     */
    public void testEdgeValues() {
        OfflineAudioContext c0 = WebAudioTestKit.mono(256);
        BiquadFilterNode zero = c0.createBiquadFilter();
        zero.getFrequency().setValue(0f);
        constantThrough(c0, zero, 1f);
        WebAudioTestKit.assertSilent("lowpass of 0 Hz", WebAudioTestKit.render(c0), 0, 256);
        OfflineAudioContext c1 = WebAudioTestKit.mono(256);
        BiquadFilterNode top = c1.createBiquadFilter();
        top.getFrequency().setValue(4000f);
        constantThrough(c1, top, 0.5f);
        float[] pass = WebAudioTestKit.render(c1);
        Check.near(0.5, pass[100], 1e-6, "lowpass at the Nyquist frequency passes the signal");
        OfflineAudioContext c2 = WebAudioTestKit.mono(256);
        BiquadFilterNode bp = c2.createBiquadFilter();
        bp.setType(BiquadFilterType.BANDPASS);
        bp.getQ().setValue(0f);
        constantThrough(c2, bp, 0.5f);
        Check.near(0.5, WebAudioTestKit.render(c2)[100], 1e-6, "bandpass with Q 0 passes the signal");
        BiquadFilterType[] types = BiquadFilterType.values();
        float[][] settings = {{0f, 0f}, {4000f, 0f}, {10f, -3f}, {2000f, 0f}, {3999f, 100f}};
        // 모든 종류와 극단적인 설정에서 출력이 유한한지 확인한다.
        for (int t = 0; t < types.length; t++) {
            for (int s = 0; s < settings.length; s++) {
                OfflineAudioContext c = WebAudioTestKit.mono(256);
                BiquadFilterNode f = c.createBiquadFilter();
                f.setType(types[t]);
                f.getFrequency().setValue(settings[s][0]);
                f.getQ().setValue(settings[s][1]);
                f.getGain().setValue(20f);
                constantThrough(c, f, 1f);
                float[] o = WebAudioTestKit.render(c);
                // 출력의 모든 프레임이 유한한 값인지 확인한다.
                for (int i = 0; i < 256; i++) {
                    Check.isTrue(!Float.isNaN(o[i]) && !Float.isInfinite(o[i]), types[t] + " " + settings[s][0] + "/" + settings[s][1] + " frame " + i);
                }
            }
        }
    }

    /**
     * Connects a constant source through a node to the destination.
     * <p>
     * 상수 원천을 노드를 거쳐 목적지에 연결한다.
     *
     * @param ctx   the context / 컨텍스트
     * @param node  the node / 노드
     * @param value the constant / 상수
     */
    private static void constantThrough(OfflineAudioContext ctx, AudioNode node, float value) {
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(value);
        c.connect(node);
        node.connect(ctx.getDestination());
        c.start(0);
    }

    /**
     * A resonant filter keeps ringing after its input has ended (the tail time) and the engine releases the node afterwards: the
     * output equals the difference equation until it has decayed, and the node is inactive at the end.
     * <p>
     * 공진이 큰 필터는 입력이 끝난 뒤에도 울리고(꼬리 시간) 엔진은 그 뒤에 노드를 놓아 준다: 출력은 감쇠할 때까지 차분 방정식과 같고 끝에서 노드는 활성이
     * 아니다.
     */
    public void testTailTime() {
        int n = 8000;
        AudioBuffer impulse = new AudioBuffer(1, 1, 8000f);
        impulse.getChannelData(0)[0] = 1f;
        OfflineAudioContext ctx = WebAudioTestKit.mono(n);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse);
        BiquadFilterNode f = ctx.createBiquadFilter();
        f.setType(BiquadFilterType.BANDPASS);
        f.getFrequency().setValue(1000f);
        f.getQ().setValue(30f);
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 규격의 대역 통과 계수로 임펄스 응답을 계산한다.
        double w0 = 2 * Math.PI * 1000 / SR;
        double alpha = Math.sin(w0) / (2 * 30);
        double b0 = alpha;
        double b2 = -alpha;
        double a0 = 1 + alpha;
        double a1 = -2 * Math.cos(w0);
        double a2 = 1 - alpha;
        double x1 = 0;
        double x2 = 0;
        double y1 = 0;
        double y2 = 0;
        int lastBig = 0;
        // 임펄스 응답이 1e-5 보다 큰 동안은 엔진도 같은 값을 내야 한다(꼬리 시간이 끝나기 전에 끊으면 안 된다).
        for (int i = 0; i < n; i++) {
            double x = i == 0 ? 1.0 : 0.0;
            double y = (b0 * x + b2 * x2 - a1 * y1 - a2 * y2) / a0;
            if (Math.abs(y) > 1e-5) {
                Check.near(y, out[i], 1e-6, "ringing at frame " + i);
                lastBig = i;
            }
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
        }
        Check.isTrue(lastBig > 400, "the filter rings for more than 3 quanta (last big sample " + lastBig + ")");
        Check.isTrue(out[n - 1] == 0f, "the output is silent at the end");
        Check.equal(0, ctx.getPinnedNodeCount(), "the engine released the filter after its tail time");
        Check.isFalse(f.outputs[0].active, "the filter is not active any more");
    }

    /**
     * A stereo input is filtered per channel with the same coefficients and the output has the channels of the input.
     * <p>
     * 스테레오 입력은 채널마다 같은 계수로 필터링되고 출력은 입력의 채널을 가진다.
     */
    public void testStereoInput() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 8000, 8000f);
        AudioBuffer stereo = new AudioBuffer(2, 8000, 8000f);
        // 왼쪽은 1000 Hz, 오른쪽은 3000 Hz 사인이다.
        for (int i = 0; i < 8000; i++) {
            stereo.getChannelData(0)[i] = (float) Math.sin(2 * Math.PI * 1000 * i / SR);
            stereo.getChannelData(1)[i] = (float) Math.sin(2 * Math.PI * 3000 * i / SR);
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo);
        BiquadFilterNode f = ctx.createBiquadFilter();
        f.getFrequency().setValue(2000f);
        f.getQ().setValue(0f);
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        float[] mag = new float[2];
        float[] ph = new float[2];
        f.getFrequencyResponse(new float[] {1000f, 3000f}, mag, ph);
        Check.near(mag[0], WebAudioTestKit.amplitudeAt(out.getChannelData(0), 4000, 8000, 1000, SR), 2e-3, "left channel gain at 1000 Hz");
        Check.near(mag[1], WebAudioTestKit.amplitudeAt(out.getChannelData(1), 4000, 8000, 3000, SR), 2e-3, "right channel gain at 3000 Hz");
        Check.isTrue(mag[0] > 0.9 && mag[1] < 0.5, "the low-pass passes 1000 Hz and attenuates 3000 Hz");
    }
}
