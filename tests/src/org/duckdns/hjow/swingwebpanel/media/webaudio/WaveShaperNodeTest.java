package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.WaveShaperKernel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the wave shaper (Web Audio API 1.31, plan item P25-04): the curve lookup with the linear interpolation against the equation
 * of the specification written out in the test, the clamping outside <code>[-1, 1]</code>, the pass-through without a curve, the errors
 * of <code>curve</code>, a curve that maps zero to a constant (the node is active without input), the channel rule, and the
 * oversampling: with the identity curve the output is the input delayed by the latency of the filters (checked against a closed-form
 * sine), the impulse response of the filters, the tail time, and the reduction of aliasing of a hard clipper measured with the amplitude
 * of a frequency that is not a harmonic of the input.
 * <p>
 * 웨이브셰이퍼 시험(Web Audio API 1.31, 계획 P25-04): 곡선 조회와 선형 보간을 규격의 식(시험에 직접 적었다)과 비교하고, <code>[-1, 1]</code> 밖의 제한,
 * 곡선이 없을 때의 통과, <code>curve</code> 의 오류, 0 을 상수로 옮기는 곡선(입력이 없어도 노드가 활성), 채널 규칙, 오버샘플링을 확인한다. 오버샘플링은
 * 항등 곡선이면 출력이 입력을 필터의 지연만큼 늦춘 것이라는 점(닫힌 식 사인과 비교), 필터의 임펄스 응답, 꼬리 시간, 입력의 고조파가 아닌 주파수의 진폭으로
 * 잰 하드 클리퍼의 앨리어싱 감소를 본다.
 */
public class WaveShaperNodeTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public WaveShaperNodeTest() {
    }

    /**
     * Renders a constant input through a wave shaper and returns the value of the last frame.
     * <p>
     * 일정한 입력을 웨이브셰이퍼에 통과시켜 렌더링하고 마지막 프레임의 값을 반환한다.
     *
     * @param curve the curve, or null / 곡선. 없으면 null
     * @param level the constant input / 일정한 입력
     * @return the output / 출력
     */
    private static double shapeConstant(float[] curve, float level) {
        OfflineAudioContext ctx = WebAudioTestKit.mono(512);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(level);
        WaveShaperNode ws = ctx.createWaveShaper();
        if (curve != null) {
            ws.setCurve(curve);
        }
        c.connect(ws);
        ws.connect(ctx.getDestination());
        c.start(0);
        return ctx.startRendering().getChannelData(0)[511];
    }

    /**
     * The expectation of the specification for an input and a curve: <code>v = (N - 1) / 2 (x + 1)</code>, <code>k = floor(v)</code>,
     * <code>f = v - k</code>, the first value below 0, the last value from <code>N - 1</code>, otherwise <code>(1 - f) c[k] + f
     * c[k + 1]</code>.
     * <p>
     * 입력과 곡선에 대한 규격의 기대값: <code>v = (N - 1) / 2 (x + 1)</code>, <code>k = floor(v)</code>, <code>f = v - k</code>, 0 보다 작으면 첫 값,
     * <code>N - 1</code> 이상이면 마지막 값, 그 밖에는 <code>(1 - f) c[k] + f c[k + 1]</code>.
     *
     * @param x the input / 입력
     * @param c the curve / 곡선
     * @return the expected output / 기대하는 출력
     */
    private static double expected(double x, float[] c) {
        int n = c.length;
        double v = (n - 1) / 2.0 * (x + 1);
        if (v < 0) {
            return c[0];
        }
        if (v >= n - 1) {
            return c[n - 1];
        }
        int k = (int) Math.floor(v);
        double f = v - k;
        return (1 - f) * c[k] + f * c[k + 1];
    }

    /**
     * The curve is looked up with linear interpolation, and inputs outside <code>[-1, 1]</code> give the end values.
     * <p>
     * 곡선은 선형 보간으로 조회하고 <code>[-1, 1]</code> 밖의 입력은 끝 값을 준다.
     */
    public void testCurveInterpolation() {
        float[] curve = {-0.5f, 0.25f, 1.0f, 0.5f, -1.0f};
        float[] inputs = {-1f, -0.75f, -0.3f, 0f, 0.1f, 0.5f, 0.99f, 1f, 1.5f, -2f, 7f};
        // 입력마다 규격의 식과 비교한다.
        for (int i = 0; i < inputs.length; i++) {
            Check.near(expected(inputs[i], curve), shapeConstant(curve, inputs[i]), 1e-6, "curve at input " + inputs[i]);
        }
        Check.near(-0.5, shapeConstant(curve, -3f), 1e-6, "below -1 gives the first value");
        Check.near(-1.0, shapeConstant(curve, 3f), 1e-6, "above 1 gives the last value");
        Check.near(1.0, shapeConstant(curve, 0f), 1e-6, "an odd curve has a value at zero: the centre");
    }

    /**
     * A curve with an even number of values has no value at zero: zero lies between the two centre values.
     * <p>
     * 값이 짝수 개인 곡선은 0 에 값이 없다: 0 은 가운데 두 값 사이에 놓인다.
     */
    public void testEvenCurveInterpolatesBetweenCentreValues() {
        float[] curve = {0f, 0.2f, 0.6f, 1f};
        Check.near(0.4, shapeConstant(curve, 0f), 1e-6, "zero is the mean of the two centre values");
        Check.near(expected(0.5, curve), shapeConstant(curve, 0.5f), 1e-6, "0.5");
        float[] two = {-1f, 1f};
        Check.near(0.5, shapeConstant(two, 0.5f), 1e-6, "a curve of two values is the identity in between");
    }

    /**
     * Without a curve the input passes unchanged, also with an oversampling (no filter delay), and a stereo input stays stereo.
     * <p>
     * 곡선이 없으면 입력이 그대로 지나간다(오버샘플링이 있어도 필터 지연이 없다). 스테레오 입력은 스테레오로 남는다.
     */
    public void testPassThroughWithoutCurve() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 512, 8000f);
        AudioBuffer in = new AudioBuffer(2, 512, 8000f);
        // 채널마다 다른 사인을 넣는다.
        for (int i = 0; i < 512; i++) {
            in.getChannelData(0)[i] = (float) (0.5 * Math.sin(2 * Math.PI * 440 * i / 8000.0));
            in.getChannelData(1)[i] = (float) (0.25 * Math.cos(2 * Math.PI * 1000 * i / 8000.0));
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        WaveShaperNode ws = ctx.createWaveShaper();
        ws.setOversample(OverSampleType.FOUR_X);
        src.connect(ws);
        ws.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        // 두 채널이 입력과 같은지 본다.
        for (int i = 0; i < 512; i++) {
            Check.near(in.getChannelData(0)[i], out.getChannelData(0)[i], 1e-7, "left " + i);
            Check.near(in.getChannelData(1)[i], out.getChannelData(1)[i], 1e-7, "right " + i);
        }
    }

    /**
     * The errors of <code>curve</code>: fewer than two values and a second non-null curve are <code>InvalidStateError</code>; null is
     * always allowed; the node keeps a copy, and <code>getCurve</code> returns a copy.
     * <p>
     * <code>curve</code> 의 오류: 값이 둘보다 적거나 두 번째 null 이 아닌 곡선은 <code>InvalidStateError</code>, null 은 언제나 허용, 노드는 복사본을 보관하고
     * <code>getCurve</code> 는 복사본을 반환한다.
     */
    public void testCurveErrors() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        final WaveShaperNode ws = ctx.createWaveShaper();
        Check.isNull(ws.getCurve(), "the curve starts as null");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ws.setCurve(new float[] {1f});
            }
        }, "a curve of one value");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ws.setCurve(new float[0]);
            }
        }, "an empty curve");
        float[] original = {-1f, 0f, 1f};
        ws.setCurve(original);
        original[1] = 5f;
        Check.near(0.0, ws.getCurve()[1], 0.0, "the node keeps a copy of the curve");
        ws.getCurve()[2] = 9f;
        Check.near(1.0, ws.getCurve()[2], 0.0, "getCurve returns a copy");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ws.setCurve(new float[] {1f, 2f});
            }
        }, "a second curve");
        ws.setCurve(null);
        Check.isNull(ws.getCurve(), "null is always allowed");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ws.setCurve(new float[] {1f, 2f});
            }
        }, "[[curve set]] stays true after null");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                ws.setOversample(null);
            }
        }, "a null oversample");
        Check.equal(OverSampleType.NONE, ws.getOversample(), "the default oversample");
        Check.equal("2x", OverSampleType.TWO_X.getScriptName(), "script name 2x");
        Check.same(OverSampleType.FOUR_X, OverSampleType.fromScriptName("4x"), "from script name");
        Check.isNull(OverSampleType.fromScriptName("8x"), "unknown name");
    }

    /**
     * A curve that maps zero to a non-zero value makes the node produce a constant signal without any input; a curve that maps zero to
     * zero stays silent.
     * <p>
     * 0 을 0 이 아닌 값으로 옮기는 곡선은 입력이 없어도 노드가 일정한 신호를 만들게 한다. 0 을 0 으로 옮기는 곡선은 무음이다.
     */
    public void testConstantCurveIsActiveWithoutInput() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(1024);
        WaveShaperNode ws = ctx.createWaveShaper();
        ws.setCurve(new float[] {0.3f, 0.3f, 0.3f});
        ws.connect(ctx.getDestination());
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("constant", out, 0, 1024, new WebAudioTestKit.Signal() {
            public double at(int frame) {
                return 0.3;
            }
        }, 1e-6);
        OfflineAudioContext ctx2 = WebAudioTestKit.mono(1024);
        WaveShaperNode ws2 = ctx2.createWaveShaper();
        ws2.setCurve(new float[] {-1f, 0f, 1f});
        ws2.connect(ctx2.getDestination());
        WebAudioTestKit.assertSilent("zero maps to zero", WebAudioTestKit.render(ctx2), 0, 1024);
    }

    /**
     * The output has as many channels as the input, and every channel is shaped on its own.
     * <p>
     * 출력은 입력과 같은 채널 수이고 채널마다 따로 변환된다.
     */
    public void testChannelsAreShapedSeparately() {
        float[] curve = {-1f, -0.2f, 0.9f};
        OfflineAudioContext ctx = new OfflineAudioContext(2, 256, 8000f);
        AudioBuffer in = new AudioBuffer(2, 256, 8000f);
        java.util.Arrays.fill(in.getChannelData(0), 0.5f);
        java.util.Arrays.fill(in.getChannelData(1), -0.6f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        WaveShaperNode ws = ctx.createWaveShaper();
        ws.setCurve(curve);
        src.connect(ws);
        ws.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(expected(0.5, curve), out.getChannelData(0)[100], 1e-6, "left is shaped");
        Check.near(expected(-0.6, curve), out.getChannelData(1)[100], 1e-6, "right is shaped on its own");
    }

    /**
     * Renders a signal through a wave shaper with an oversampling and returns the output.
     * <p>
     * 신호를 오버샘플링을 가진 웨이브셰이퍼에 통과시켜 출력을 반환한다.
     *
     * @param signal   the input samples / 입력 샘플
     * @param rate     the sample rate / 샘플링 주파수
     * @param curve    the curve / 곡선
     * @param type     the oversampling / 오버샘플링
     * @return the output / 출력
     */
    private static float[] through(float[] signal, float rate, float[] curve, OverSampleType type) {
        OfflineAudioContext ctx = new OfflineAudioContext(1, signal.length, rate);
        AudioBuffer in = new AudioBuffer(1, signal.length, rate);
        System.arraycopy(signal, 0, in.getChannelData(0), 0, signal.length);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        WaveShaperNode ws = ctx.createWaveShaper();
        ws.setCurve(curve);
        ws.setOversample(type);
        src.connect(ws);
        ws.connect(ctx.getDestination());
        src.start(0);
        return ctx.startRendering().getChannelData(0);
    }

    /**
     * With the identity curve and an oversampling the output is the input delayed by the latency of the filters: for a sine of a frequency
     * in the pass band the closed form <code>a sin(2 pi f (n - D) / rate)</code> with the delay <code>D</code> of 63 frames (2x) or
     * 74.5 frames (4x) holds.
     * <p>
     * 항등 곡선과 오버샘플링에서 출력은 입력을 필터의 지연만큼 늦춘 것이다. 통과 대역의 사인에 대해 지연 <code>D</code>(2x 는 63 프레임, 4x 는 74.5
     * 프레임)로 닫힌 식 <code>a sin(2 pi f (n - D) / rate)</code> 가 성립한다.
     */
    public void testOversampledIdentityIsADelayedInput() {
        float rate = 48000f;
        float[] identity = {-1f, 1f};
        double[] freqs = {300.0, 3000.0, 9000.0, 16000.0};
        OverSampleType[] types = {OverSampleType.TWO_X, OverSampleType.FOUR_X};
        double[] delays = {63.0, 74.5};
        // 오버샘플링 종류와 주파수마다 닫힌 식의 지연된 사인과 비교한다.
        for (int t = 0; t < types.length; t++) {
            for (int fi = 0; fi < freqs.length; fi++) {
                final double f = freqs[fi];
                float[] x = new float[2048];
                // 입력 사인을 만든다.
                for (int i = 0; i < x.length; i++) {
                    x[i] = (float) (0.7 * Math.sin(2 * Math.PI * f * i / rate));
                }
                float[] y = through(x, rate, identity, types[t]);
                final double d = delays[t];
                // 필터의 이력이 찬 뒤(300 프레임 이후)부터 지연된 사인과 비교한다.
                for (int n = 300; n < 2048; n++) {
                    double e = 0.7 * Math.sin(2 * Math.PI * f * (n - d) / rate);
                    Check.near(e, y[n], 2e-3, types[t] + " " + f + " Hz frame " + n);
                }
            }
        }
    }

    /**
     * The impulse response of the filters with the identity curve. The cascade of an interpolator and a decimator is a band-limited
     * delay: 2x delays by 63 whole frames, so the response is a pulse of about 0.985 at frame 63 whose neighbours are below 0.02; 4x
     * delays by 74.5 frames, so the response is the band-limited half-frame delay <code>sinc(n - 74.5)</code> (<code>2/pi</code> at
     * the frames 74 and 75, <code>-2/(3 pi)</code> at 73 and 76). The gain at zero frequency is exactly 1 (the sum of the response), and
     * the output is silent after the tail time that the node reports.
     * <p>
     * 항등 곡선에서 필터의 임펄스 응답. 보간기와 데시메이터를 이은 것은 대역 제한 지연이다. 2x 는 정수 63 프레임을 지연하므로 응답은 63 프레임에서 약 0.985
     * 인 펄스이고 이웃은 0.02 보다 작다. 4x 는 74.5 프레임을 지연하므로 응답은 대역 제한 반 프레임 지연 <code>sinc(n - 74.5)</code> 이다(74, 75 프레임에서
     * <code>2/pi</code>, 73, 76 에서 <code>-2/(3 pi)</code>). 0 주파수의 이득은 정확히 1(응답의 합)이고 노드가 알리는 꼬리 시간 뒤에는 무음이다.
     */
    public void testImpulseResponseAndTail() {
        float rate = 48000f;
        float[] identity = {-1f, 1f};
        float[] x = new float[1024];
        x[0] = 1f;
        float[] y2 = through(x, rate, identity, OverSampleType.TWO_X);
        Check.near(1.0, y2[63], 0.02, "the 2x chain delays an impulse by 63 frames");
        int last = 0;
        double sum = 0;
        // 응답의 합과 0 이 아닌 마지막 프레임을 찾고 이웃의 크기를 확인한다.
        for (int i = 0; i < y2.length; i++) {
            sum += y2[i];
            if (Math.abs(y2[i]) > 1e-9) {
                last = i;
            }
            if (i != 63) {
                Check.isTrue(Math.abs(y2[i]) < 0.02, "2x residue at frame " + i + " is " + y2[i]);
            }
        }
        Check.near(1.0, sum, 1e-5, "the gain at zero frequency of 2x");
        Check.isTrue(last < WaveShaperKernel.tailFramesOf(2), "the response of 2x ends at " + last + " before the tail " + WaveShaperKernel.tailFramesOf(2));
        float[] y4 = through(x, rate, identity, OverSampleType.FOUR_X);
        // 74.5 프레임 지연은 대역 제한 반 프레임 지연 sinc(n - 74.5) 이다.
        Check.near(2 / Math.PI, y4[74], 0.01, "4x frame 74");
        Check.near(2 / Math.PI, y4[75], 0.01, "4x frame 75");
        Check.near(-2 / (3 * Math.PI), y4[73], 0.01, "4x frame 73");
        Check.near(-2 / (3 * Math.PI), y4[76], 0.01, "4x frame 76");
        last = 0;
        sum = 0;
        // 4x 의 응답의 합과 0 이 아닌 마지막 프레임을 찾는다.
        for (int i = 0; i < y4.length; i++) {
            sum += y4[i];
            if (Math.abs(y4[i]) > 1e-9) {
                last = i;
            }
        }
        Check.near(1.0, sum, 1e-5, "the gain at zero frequency of 4x");
        Check.isTrue(last < WaveShaperKernel.tailFramesOf(4), "the response of 4x ends at " + last + " before the tail " + WaveShaperKernel.tailFramesOf(4));
        Check.equal(0L, WaveShaperKernel.tailFramesOf(1), "no tail without oversampling");
    }

    /**
     * The amplitude of the fundamental of a hard clipper in closed form: the clipped sine <code>clamp(g a sin t)</code> has the
     * fundamental <code>(1/pi) integral over 0..2pi of clamp(g a sin t) sin t dt</code> (a numerical integration with a fine step that is
     * independent of the engine).
     * <p>
     * 하드 클리퍼의 기본파 진폭의 닫힌 식: 잘린 사인 <code>clamp(g a sin t)</code> 의 기본파는 <code>(1/pi) 0..2pi 적분 clamp(g a sin t) sin t dt</code>
     * 이다(엔진과 무관한 촘촘한 간격의 수치 적분).
     *
     * @param gain      the gain before the clipper / 클리퍼 앞의 이득
     * @param amplitude the amplitude of the sine / 사인의 진폭
     * @return the amplitude of the fundamental / 기본파 진폭
     */
    private static double clipperFundamental(double gain, double amplitude) {
        int n = 400000;
        double sum = 0;
        // 한 주기를 촘촘히 나눠 적분한다.
        for (int i = 0; i < n; i++) {
            double t = 2 * Math.PI * (i + 0.5) / n;
            double v = Math.max(-1.0, Math.min(1.0, gain * amplitude * Math.sin(t)));
            sum += v * Math.sin(t);
        }
        return sum * (2 * Math.PI / n) / Math.PI;
    }

    /**
     * Returns what an ideal oversampled shaper gives: the sine is sampled at <code>rate * factor</code> without any filter, the curve is
     * applied with the equation of the specification, and the amplitude of a frequency is measured in that high rate signal. An ideal
     * low-pass filter before the decimation leaves the components below the original Nyquist frequency unchanged, so the amplitude of
     * such a frequency is the alias-free expectation of the shaper with that oversampling (the components that the curve creates above
     * the high rate still fold, as they do in every implementation).
     * <p>
     * 이상적인 오버샘플링 웨이브셰이퍼가 주는 값을 반환한다: 사인을 필터 없이 <code>rate * factor</code> 로 표본화하고, 규격의 식으로 곡선을 적용하고, 그
     * 높은 속도의 신호에서 주파수의 진폭을 잰다. 데시메이션 앞의 이상적인 저역 통과 필터는 원래 나이퀴스트 주파수 아래 성분을 바꾸지 않으므로, 그런 주파수의
     * 진폭이 그 오버샘플링에서의 앨리어싱 없는 기대값이다(곡선이 높은 속도 위에 만든 성분은 모든 구현에서처럼 여전히 접힌다).
     *
     * @param rate    the sample rate / 샘플링 주파수
     * @param factor  the oversampling factor / 오버샘플링 배수
     * @param curve   the curve / 곡선
     * @param probeHz the frequency to measure / 잴 주파수
     * @return the amplitude / 진폭
     */
    private static double idealAmplitude(float rate, int factor, float[] curve, double probeHz) {
        double hi = rate * (double) factor;
        int from = 400 * factor;
        int to = 2800 * factor;
        float[] y = new float[to];
        // 높은 속도로 표본화한 사인에 곡선을 적용한다.
        for (int i = 0; i < to; i++) {
            y[i] = (float) expected(0.6 * Math.sin(2 * Math.PI * 7000.0 * i / hi), curve);
        }
        return WebAudioTestKit.amplitudeAt(y, from, to, probeHz, hi);
    }

    /**
     * Oversampling reduces the aliasing of a hard clipper: a sine of 7000 Hz at 48000 Hz that is clipped has odd harmonics. The
     * frequency 13000 Hz is no harmonic, so an alias-free shaper has no amplitude there; without oversampling the 35 kHz harmonic folds
     * to 13 kHz with a large amplitude, and with 2x or 4x only the much weaker harmonics above the high rate fold there. For every
     * oversampling the engine matches the ideal oversampled shaper of {@link #idealAmplitude} (the amplitudes at 13000 Hz and 7000 Hz
     * within a small margin) and the fundamental matches the closed form of the clipper.
     * <p>
     * 오버샘플링은 하드 클리퍼의 앨리어싱을 줄인다: 48000 Hz 에서 7000 Hz 사인을 자르면 홀수 고조파가 생긴다. 13000 Hz 는 고조파가 아니므로 앨리어싱이 없는
     * 웨이브셰이퍼에서는 진폭이 없다. 오버샘플링이 없으면 35 kHz 고조파가 큰 진폭으로 13 kHz 에 접히고, 2x 나 4x 에서는 높은 속도 위의 훨씬 약한 고조파만
     * 거기에 접힌다. 모든 오버샘플링에서 엔진은 {@link #idealAmplitude} 의 이상적인 오버샘플링 웨이브셰이퍼와(13000 Hz 와 7000 Hz 의 진폭이 작은 여유 안에서)
     * 맞고, 기본파는 클리퍼의 닫힌 식과 맞는다.
     */
    public void testOversamplingReducesAliasing() {
        float rate = 48000f;
        // 곡선: 기울기 4 로 올라가다 +-1 에서 잘리는 하드 클리퍼를 [-1, 1] 에서 조밀하게 표본화한다.
        float[] curve = new float[2001];
        for (int i = 0; i < curve.length; i++) {
            double x = -1.0 + 2.0 * i / 2000.0;
            curve[i] = (float) Math.max(-1.0, Math.min(1.0, 4.0 * x));
        }
        int n = 4096;
        float[] x = new float[n];
        // 진폭 0.6 의 7000 Hz 사인을 만든다.
        for (int i = 0; i < n; i++) {
            x[i] = (float) (0.6 * Math.sin(2 * Math.PI * 7000.0 * i / rate));
        }
        double refFundamental = clipperFundamental(4.0, 0.6);
        double aliasNone = 0;
        OverSampleType[] types = {OverSampleType.NONE, OverSampleType.TWO_X, OverSampleType.FOUR_X};
        // 오버샘플링 종류마다 13000 Hz 와 7000 Hz 의 진폭을 이상적인 기대값과 비교한다.
        for (int t = 0; t < types.length; t++) {
            float[] y = through(x, rate, curve, types[t]);
            int factor = types[t].getFactor();
            // 2400 프레임은 7000 Hz 로 350 주기, 13000 Hz 로 650 주기이므로 진폭 측정이 정확하다(지연은 진폭에 영향이 없다).
            double alias = WebAudioTestKit.amplitudeAt(y, 400, 2800, 13000.0, rate);
            double fund = WebAudioTestKit.amplitudeAt(y, 400, 2800, 7000.0, rate);
            double idealAlias = idealAmplitude(rate, factor, curve, 13000.0);
            double idealFund = idealAmplitude(rate, factor, curve, 7000.0);
            Check.near(idealAlias, alias, idealAlias * 0.15 + 2e-4, types[t] + " alias at 13 kHz against the ideal shaper");
            Check.near(idealFund, fund, idealFund * 0.01 + 2e-4, types[t] + " fundamental against the ideal shaper");
            if (t == 0) {
                aliasNone = alias;
                Check.isTrue(alias > 0.05, "without oversampling the alias at 13 kHz is large: " + alias);
            } else {
                Check.isTrue(alias < aliasNone / 100.0, types[t] + " alias " + alias + " is not 40 dB below " + aliasNone);
                Check.near(refFundamental, fund, refFundamental * 0.01, types[t] + " fundamental against the closed form");
            }
        }
    }

    /**
     * The tail time: with an oversampling and a curve the node reports the tail of the filters; without them it reports none, so a
     * source that ended releases the node at once.
     * <p>
     * 꼬리 시간: 오버샘플링과 곡선이 있으면 노드가 필터의 꼬리를 알리고, 없으면 알리지 않으므로 원천이 끝나면 노드가 바로 풀린다.
     */
    public void testTailTimeFollowsOversample() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(2048);
        ConstantSourceNode c = ctx.createConstantSource();
        WaveShaperNode ws = ctx.createWaveShaper();
        ws.setCurve(new float[] {-1f, 1f});
        ws.setOversample(OverSampleType.TWO_X);
        c.connect(ws);
        ws.connect(ctx.getDestination());
        c.start(0);
        c.stop(0.05);
        ctx.startRendering();
        Check.equal(0L, ctx.getPinnedNodeCount(), "after the tail the shaper is released again");
    }
}
