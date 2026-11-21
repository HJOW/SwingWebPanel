package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the half-band stages and the wave shaper kernel (plan item P25-04): the design of the taps (their sum, the closed form of the
 * ideal values), the interpolation of a band-limited sine at the half sample positions against its closed form, the stop band of the
 * decimator, the flat gain of the whole oversampling chain measured with sine tones, the independence of the result from the block size
 * (the streaming state), and the curve lookup of the kernel including a NaN input.
 * <p>
 * 반대역 단계와 웨이브셰이퍼 커널 시험(계획 P25-04): 탭의 설계(합, 이상적인 값의 닫힌 식), 대역 제한 사인을 반 샘플 위치에서 보간한 결과와 닫힌 식의
 * 비교, 데시메이터의 저지 대역, 오버샘플링 전체 사슬의 평탄한 이득(사인으로 측정), 블록 크기와 무관한 결과(스트리밍 상태), NaN 입력을 포함한 커널의
 * 곡선 조회를 확인한다.
 */
public class HalfBandTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HalfBandTest() {
    }

    /**
     * The taps sum to one half, alternate in sign and start near the ideal value <code>2 / pi</code> of the sinc at half a sample.
     * <p>
     * 탭의 합은 1/2 이고 부호가 번갈아 바뀌며 반 샘플에서의 sinc 의 이상적인 값 <code>2 / pi</code> 근처에서 시작한다.
     */
    public void testTapDesign() {
        double[] g = HalfBand.design(32);
        double sum = 0;
        // 탭의 합을 구하고 부호가 번갈아 바뀌는지 본다.
        for (int m = 0; m < g.length; m++) {
            sum += g[m];
            Check.isTrue((m % 2 == 0) == (g[m] > 0), "the sign of tap " + m + " alternates");
        }
        Check.near(0.5, sum, 1e-12, "the taps sum to 1/2");
        Check.near(2 / Math.PI, g[0], 0.01, "the first tap is sinc(0.5) = 2 / pi");
        Check.near(-2 / (3 * Math.PI), g[1], 0.01, "the second tap is sinc(1.5) = -2 / (3 pi)");
        Check.near(0.0, HalfBand.bessel0(0.0) - 1.0, 0.0, "I0(0) = 1");
        Check.near(2.2795853023360673, HalfBand.bessel0(2.0), 1e-12, "I0(2)");
    }

    /**
     * The interpolator returns, for a sine below the pass band edge, the closed form: the even output frames are the input delayed by
     * <code>P</code> and the odd frames are the sine at the half sample position <code>n - P + 0.5</code>.
     * <p>
     * 보간기는 통과 대역 가장자리 아래의 사인에 대해 닫힌 식을 돌려준다. 짝수 출력 프레임은 입력을 <code>P</code> 지연한 것이고 홀수 프레임은 반 샘플 위치
     * <code>n - P + 0.5</code> 에서의 사인이다.
     */
    public void testInterpolatorOfASine() {
        HalfBandInterpolator up = new HalfBandInterpolator(32);
        int n = 512;
        double f = 0.17;
        float[] x = new float[n];
        // 정규화 주파수 0.17 의 사인을 만든다.
        for (int i = 0; i < n; i++) {
            x[i] = (float) Math.sin(2 * Math.PI * f * i);
        }
        float[] y = new float[2 * n];
        up.process(x, n, y);
        // 필터 이력이 찬 뒤 짝수와 홀수 출력을 닫힌 식과 비교한다.
        for (int t = 100; t < n; t++) {
            Check.near(Math.sin(2 * Math.PI * f * (t - 32)), y[2 * t], 1e-6, "even frame " + t);
            Check.near(Math.sin(2 * Math.PI * f * (t - 32 + 0.5)), y[2 * t + 1], 1e-4, "odd frame " + t);
        }
        Check.equal(32L, up.getDelay(), "the delay of the interpolator");
    }

    /**
     * A constant is interpolated to the same constant and decimated to the same constant (the gain at zero frequency is exactly 1).
     * <p>
     * 상수는 같은 상수로 보간되고 같은 상수로 데시메이트된다(0 주파수의 이득이 정확히 1).
     */
    public void testConstantKeepsItsValue() {
        HalfBandInterpolator up = new HalfBandInterpolator(12);
        HalfBandDecimator down = new HalfBandDecimator(12);
        float[] x = new float[256];
        java.util.Arrays.fill(x, 0.7f);
        float[] mid = new float[512];
        float[] y = new float[256];
        up.process(x, 256, mid);
        down.process(mid, 256, y);
        // 필터 이력이 찬 뒤에는 상수가 그대로 나온다.
        for (int i = 60; i < 256; i++) {
            Check.near(0.7, mid[2 * i + 1], 1e-6, "interpolated constant " + i);
            Check.near(0.7, y[i], 1e-6, "decimated constant " + i);
        }
        Check.equal(11L, down.getDelay(), "the delay of the decimator");
    }

    /**
     * The stop band of the decimator: a tone at 0.375 of the high rate (which would fold to 0.25 of the low rate) is attenuated by
     * more than 70 dB.
     * <p>
     * 데시메이터의 저지 대역: 높은 속도의 0.375 배 톤(낮은 속도의 0.25 배로 접힐 것)이 70 dB 넘게 감쇠된다.
     */
    public void testDecimatorStopBand() {
        HalfBandDecimator down = new HalfBandDecimator(32);
        int n = 2048;
        float[] v = new float[2 * n];
        // 높은 속도의 0.375 배 사인을 만든다.
        for (int i = 0; i < 2 * n; i++) {
            v[i] = (float) Math.sin(2 * Math.PI * 0.375 * i);
        }
        float[] y = new float[n];
        down.process(v, n, y);
        double peak = 0;
        // 필터 이력이 찬 뒤 출력의 최댓값을 구한다.
        for (int i = 200; i < n; i++) {
            peak = Math.max(peak, Math.abs(y[i]));
        }
        Check.isTrue(peak < 3e-4, "the stop band leaks " + peak);
    }

    /**
     * The gain of the whole chain for the identity curve is flat within 0.02 dB from 0.02 to 0.46 of the sample rate, for 2x and 4x,
     * measured with sine tones (the amplitude by correlation with the tone).
     * <p>
     * 항등 곡선에서 전체 사슬의 이득은 2x 와 4x 모두 샘플링 주파수의 0.02~0.46 배에서 0.02 dB 안으로 평탄하다(톤과의 상관으로 진폭을 측정).
     */
    public void testChainGainIsFlat() {
        float[] identity = {-1f, 1f};
        int[] factors = {2, 4};
        // 오버샘플링 배수마다 여러 주파수의 사인 이득을 잰다.
        for (int fi = 0; fi < factors.length; fi++) {
            for (int k = 0; k < 12; k++) {
                double f = 0.02 + 0.04 * k;
                WaveShaperKernel kernel = new WaveShaperKernel(factors[fi]);
                int n = 4096;
                float[] s = new float[n];
                float[] o = new float[n];
                // 정규화 주파수 f 의 사인을 만든다.
                for (int i = 0; i < n; i++) {
                    s[i] = (float) Math.sin(2 * Math.PI * f * i);
                }
                kernel.process(s, o, n, identity);
                double re = 0;
                double im = 0;
                // 필터 이력이 찬 뒤 사인과 코사인에 대한 상관을 구한다.
                for (int i = 1000; i < n; i++) {
                    re += o[i] * Math.sin(2 * Math.PI * f * i);
                    im += o[i] * Math.cos(2 * Math.PI * f * i);
                }
                double amp = 2 * Math.sqrt(re * re + im * im) / (n - 1000);
                double db = 20 * Math.log10(amp);
                Check.isTrue(Math.abs(db) < 0.02, "factor " + factors[fi] + " at " + f + " of the rate: " + db + " dB");
            }
        }
    }

    /**
     * The result does not depend on how the signal is cut into blocks (the streaming state is kept correctly), for 1, 2 and 4 times
     * oversampling with a non-linear curve.
     * <p>
     * 결과는 신호를 블록으로 자르는 방식과 무관하다(스트리밍 상태가 올바르게 유지된다). 1, 2, 4 배 오버샘플링에 비선형 곡선을 쓴다.
     */
    public void testBlockSizeDoesNotMatter() {
        float[] curve = {-0.9f, -0.2f, 0.1f, 0.8f, 0.3f};
        int n = 1200;
        float[] x = new float[n];
        // 알려진 시드의 의사 난수로 입력을 만든다.
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < n; i++) {
            x[i] = (float) (rnd.nextDouble() * 2 - 1);
        }
        int[] factors = {1, 2, 4};
        int[] blocks = {1, 7, 128, 300};
        // 배수마다 한 번에 처리한 결과를 여러 블록 크기의 결과와 비교한다.
        for (int fi = 0; fi < factors.length; fi++) {
            float[] whole = new float[n];
            new WaveShaperKernel(factors[fi]).process(x, whole, n, curve);
            // 블록 크기마다 입력을 잘라 처리하고 결과를 모은다.
            for (int bi = 0; bi < blocks.length; bi++) {
                WaveShaperKernel kernel = new WaveShaperKernel(factors[fi]);
                float[] part = new float[n];
                float[] in = new float[blocks[bi]];
                float[] out = new float[blocks[bi]];
                // 블록 크기만큼씩 입력을 잘라 처리한다.
                for (int start = 0; start < n; start += blocks[bi]) {
                    int len = Math.min(blocks[bi], n - start);
                    System.arraycopy(x, start, in, 0, len);
                    kernel.process(in, out, len, curve);
                    System.arraycopy(out, 0, part, start, len);
                }
                // 같은 프레임의 값이 일치해야 한다.
                for (int i = 0; i < n; i++) {
                    Check.near(whole[i], part[i], 1e-6, "factor " + factors[fi] + " block " + blocks[bi] + " frame " + i);
                }
            }
        }
    }

    /**
     * The curve lookup of the kernel: the equation of the specification, the end values, and a NaN input that is treated as zero.
     * <p>
     * 커널의 곡선 조회: 규격의 식, 끝 값, 0 으로 취급하는 NaN 입력.
     */
    public void testShapeLookup() {
        float[] curve = {1f, 3f, 2f};
        Check.near(1.0, WaveShaperKernel.shape(-1.0, curve), 0.0, "first value");
        Check.near(3.0, WaveShaperKernel.shape(0.0, curve), 1e-12, "centre value");
        Check.near(2.0, WaveShaperKernel.shape(1.0, curve), 0.0, "last value");
        Check.near(2.0, WaveShaperKernel.shape(-0.5, curve), 1e-12, "between the first and the centre: (1 + 3) / 2");
        Check.near(2.5, WaveShaperKernel.shape(0.5, curve), 1e-12, "between the centre and the last: (3 + 2) / 2");
        Check.near(1.0, WaveShaperKernel.shape(-9.0, curve), 0.0, "below the range");
        Check.near(2.0, WaveShaperKernel.shape(9.0, curve), 0.0, "above the range");
        Check.near(3.0, WaveShaperKernel.shape(Double.NaN, curve), 1e-12, "NaN is treated as zero");
        Check.near(2.0, WaveShaperKernel.shape(Double.POSITIVE_INFINITY, curve), 0.0, "infinity");
        Check.near(63.0, WaveShaperKernel.latencyOf(2), 0.0, "latency of 2x");
        Check.near(74.5, WaveShaperKernel.latencyOf(4), 0.0, "latency of 4x");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new WaveShaperKernel(3);
            }
        }, "a factor of 3");
    }
}
