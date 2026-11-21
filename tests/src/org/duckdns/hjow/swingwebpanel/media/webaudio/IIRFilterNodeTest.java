package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the IIR filter (Web Audio API 1.21, plan item P25-04): the output against the difference equation that the test evaluates
 * itself (also when <code>feedback[0]</code> is not 1), the frequency response of a one-pole filter against its closed form, the argument
 * rules of <code>createIIRFilter</code>, the tail time of a decaying and of an unstable filter, and stereo input.
 * <p>
 * IIR 필터 시험(Web Audio API 1.21, 계획 P25-04): 시험이 직접 계산한 차분 방정식과의 출력 비교(<code>feedback[0]</code> 이 1 이 아닐 때도), 1극 필터의
 * 주파수 응답과 닫힌 식, <code>createIIRFilter</code> 의 인수 규칙, 감쇠하는 필터와 불안정한 필터의 꼬리 시간, 스테레오 입력.
 */
public class IIRFilterNodeTest {

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
    public IIRFilterNodeTest() {
    }

    /**
     * Filters random noise with an IIR filter and returns the input and the output.
     * <p>
     * 임의의 잡음을 IIR 필터로 필터링해 입력과 출력을 반환한다.
     *
     * @param ff the feedforward coefficients / 피드포워드 계수
     * @param fb the feedback coefficients / 피드백 계수
     * @param n  the number of frames / 프레임 수
     * @return the input at index 0 and the output at index 1 / 입력(0번)과 출력(1번)
     */
    private static float[][] run(double[] ff, double[] fb, int n) {
        AudioBuffer noise = new AudioBuffer(1, n, 8000f);
        java.util.Random r = new java.util.Random(3);
        // 입력은 -1 ~ 1 의 임의 값이다.
        for (int i = 0; i < n; i++) {
            noise.getChannelData(0)[i] = r.nextFloat() * 2 - 1;
        }
        OfflineAudioContext ctx = WebAudioTestKit.mono(n);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(noise);
        IIRFilterNode f = ctx.createIIRFilter(ff, fb);
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        return new float[][] {noise.getChannelData(0), WebAudioTestKit.render(ctx)};
    }

    /**
     * The output equals <code>sum a[k] y(n-k) = sum b[k] x(n-k)</code>, also when the first feedback coefficient is not 1.
     * <p>
     * 출력은 <code>sum a[k] y(n-k) = sum b[k] x(n-k)</code> 와 같다. 첫 피드백 계수가 1 이 아니어도 마찬가지다.
     */
    public void testDifferenceEquation() {
        double[] ff = {0.2, 0.3, 0.1, -0.05};
        double[] fb = {2.0, -0.8, 0.3};
        float[][] io = run(ff, fb, 500);
        // 직접 차분 방정식을 계산한다: y(n) = (sum b x - sum_{k>=1} a y) / a0
        double[] y = new double[500];
        for (int n = 0; n < 500; n++) {
            double acc = 0;
            for (int k = 0; k < ff.length; k++) {
                if (n - k >= 0) {
                    acc += ff[k] * io[0][n - k];
                }
            }
            for (int k = 1; k < fb.length; k++) {
                if (n - k >= 0) {
                    acc -= fb[k] * y[n - k];
                }
            }
            y[n] = acc / fb[0];
            Check.near(y[n], io[1][n], 1e-5, "frame " + n);
        }
    }

    /**
     * A filter of the maximum order (20 coefficients each) works.
     * <p>
     * 최대 차수(각각 계수 20개)의 필터가 동작한다.
     */
    public void testMaximumOrder() {
        double[] ff = new double[20];
        double[] fb = new double[20];
        // 안정한 계수: 피드포워드는 이동 평균, 피드백은 작은 계수.
        for (int i = 0; i < 20; i++) {
            ff[i] = 1.0 / 20;
            fb[i] = i == 0 ? 1.0 : (i % 2 == 0 ? 0.01 : -0.01);
        }
        float[][] io = run(ff, fb, 300);
        double[] y = new double[300];
        // 차분 방정식으로 기대값을 만든다.
        for (int n = 0; n < 300; n++) {
            double acc = 0;
            for (int k = 0; k < 20; k++) {
                if (n - k >= 0) {
                    acc += ff[k] * io[0][n - k];
                    if (k >= 1) {
                        acc -= fb[k] * y[n - k];
                    }
                }
            }
            y[n] = acc;
            Check.near(y[n], io[1][n], 1e-5, "frame " + n);
        }
    }

    /**
     * The response of the one-pole filter <code>y = (1 - a) x + a y1</code> is <code>(1 - a) / (1 - a e^(-j w))</code>: the magnitude
     * and the phase are compared with the closed form; frequencies outside 0 to the Nyquist frequency give NaN.
     * <p>
     * 1극 필터 <code>y = (1 - a) x + a y1</code> 의 응답은 <code>(1 - a) / (1 - a e^(-j w))</code> 이다. 크기와 위상을 닫힌 식과 비교한다. 0 에서 나이퀴스트
     * 주파수까지의 밖은 NaN 을 준다.
     */
    public void testFrequencyResponse() {
        final double a = 0.75;
        OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        IIRFilterNode f = ctx.createIIRFilter(new double[] {1 - a}, new double[] {1, -a});
        float[] hz = {0f, 100f, 1000f, 2500f, 4000f, -5f, 4500f};
        float[] mag = new float[hz.length];
        float[] ph = new float[hz.length];
        f.getFrequencyResponse(hz, mag, ph);
        // 범위 안의 주파수는 닫힌 식과 비교한다: |H|^2 = (1 - a)^2 / (1 - 2 a cos w + a^2), phase = -atan2(a sin w, 1 - a cos w).
        for (int i = 0; i < 5; i++) {
            double w = 2 * Math.PI * hz[i] / SR;
            double expectedMag = (1 - a) / Math.sqrt(1 - 2 * a * Math.cos(w) + a * a);
            double expectedPhase = -Math.atan2(a * Math.sin(w), 1 - a * Math.cos(w));
            Check.near(expectedMag, mag[i], 1e-6, "magnitude at " + hz[i]);
            Check.near(expectedPhase, ph[i], 1e-6, "phase at " + hz[i]);
        }
        Check.isTrue(Float.isNaN(mag[5]) && Float.isNaN(ph[5]), "a negative frequency gives NaN");
        Check.isTrue(Float.isNaN(mag[6]) && Float.isNaN(ph[6]), "a frequency above the Nyquist frequency gives NaN");
        final IIRFilterNode g = f;
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_ACCESS, new Runnable() {
            public void run() {
                g.getFrequencyResponse(new float[2], new float[2], new float[3]);
            }
        }, "arrays of different lengths");
    }

    /**
     * The rules of <code>createIIRFilter</code>: 1 to 20 coefficients, not all feedforward coefficients 0, the first feedback
     * coefficient not 0.
     * <p>
     * <code>createIIRFilter</code> 의 규칙: 계수 1~20개, 피드포워드가 모두 0 이 아님, 첫 피드백 계수가 0 이 아님.
     */
    public void testArgumentRules() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[0], new double[] {1});
            }
        }, "empty feedforward");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[] {1}, new double[0]);
            }
        }, "empty feedback");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[21], new double[] {1});
            }
        }, "21 feedforward coefficients");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[] {1}, new double[21]);
            }
        }, "21 feedback coefficients");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[] {0, 0}, new double[] {1});
            }
        }, "all feedforward coefficients are 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[] {1}, new double[] {0, 1});
            }
        }, "first feedback coefficient is 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.TYPE, new Runnable() {
            public void run() {
                ctx.createIIRFilter(new double[] {Double.NaN}, new double[] {1});
            }
        }, "NaN coefficient");
        double[] ones = new double[20];
        double[] unit = new double[20];
        java.util.Arrays.fill(ones, 0.05);
        unit[0] = 1.0;
        Check.notNull(ctx.createIIRFilter(ones, unit), "20 coefficients each are the maximum and are accepted");
    }

    /**
     * The tail time of a decaying filter lets the filter ring after the input; the engine releases the node afterwards; an unstable
     * filter has the maximum tail time and is not released.
     * <p>
     * 감쇠하는 필터의 꼬리 시간은 입력 뒤에도 필터가 울리게 하고, 엔진은 그 뒤에 노드를 놓아 준다. 불안정한 필터는 최대 꼬리 시간을 가지며 놓이지 않는다.
     */
    public void testTailTime() {
        int n = 6000;
        AudioBuffer impulse = new AudioBuffer(1, 1, 8000f);
        impulse.getChannelData(0)[0] = 1f;
        OfflineAudioContext ctx = WebAudioTestKit.mono(n);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse);
        IIRFilterNode f = ctx.createIIRFilter(new double[] {1}, new double[] {1, -0.99});
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        float[] out = WebAudioTestKit.render(ctx);
        // 임펄스 응답은 0.99^n 이다. 1e-4 보다 큰 동안(약 916 프레임)은 엔진도 같은 값을 내야 한다.
        for (int i = 0; i < 900; i++) {
            Check.near(Math.pow(0.99, i), out[i], 1e-6, "frame " + i);
        }
        Check.isTrue(out[n - 1] == 0f, "silent at the end");
        Check.equal(0, ctx.getPinnedNodeCount(), "released after the tail time");
        OfflineAudioContext u = WebAudioTestKit.mono(n);
        AudioBufferSourceNode s2 = u.createBufferSource();
        s2.setBuffer(impulse);
        IIRFilterNode unstable = u.createIIRFilter(new double[] {1}, new double[] {1, -1.001});
        s2.connect(unstable);
        unstable.connect(u.getDestination());
        s2.start(0);
        float[] o2 = WebAudioTestKit.render(u);
        Check.isTrue(o2[n - 1] > 1f, "the unstable filter keeps growing and is not cut: " + o2[n - 1]);
    }

    /**
     * A stereo input is filtered per channel.
     * <p>
     * 스테레오 입력은 채널마다 필터링된다.
     */
    public void testStereo() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 400, 8000f);
        AudioBuffer stereo = new AudioBuffer(2, 400, 8000f);
        java.util.Arrays.fill(stereo.getChannelData(0), 1f);
        java.util.Arrays.fill(stereo.getChannelData(1), -2f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(stereo);
        IIRFilterNode f = ctx.createIIRFilter(new double[] {0.5}, new double[] {1, -0.5});
        src.connect(f);
        f.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        // 직류에서 이득은 0.5 / (1 - 0.5) = 1 이므로 채널마다 입력 값으로 수렴한다.
        Check.near(1.0, out.getChannelData(0)[399], 1e-5, "left settles at 1");
        Check.near(-2.0, out.getChannelData(1)[399], 1e-5, "right settles at -2");
        Check.near(0.5, out.getChannelData(0)[0], 1e-6, "left step response starts at b0");
        Check.near(-1.0, out.getChannelData(1)[0], 1e-6, "right step response starts at 2 b0");
    }
}
