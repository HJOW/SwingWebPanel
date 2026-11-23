package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the partitioned FFT convolution (plan item P25-04): the result against the direct convolution sum that the test computes
 * itself, for impulse responses whose length is on both sides of every boundary between the levels (512, 2048, 8192), for other render
 * quantum sizes and for a scale; the absence of latency (an impulse at the first frame gives the response from the first frame); a long
 * sparse response whose taps lie in every level, so that the large partitions are checked without an expensive direct sum; the quantum
 * argument check; and the reset.
 * <p>
 * 분할 FFT 합성곱 시험(계획 P25-04): 시험이 직접 계산한 합성곱 합과 비교한다. 임펄스 응답 길이는 레벨 사이의 모든 경계(512, 2048, 8192) 양쪽에 두고, 다른
 * render quantum 크기와 배율도 확인한다. 지연이 없음(첫 프레임의 임펄스가 첫 프레임부터 응답을 준다), 모든 레벨에 탭이 있는 긴 성긴 응답(큰 파티션을 비싼
 * 직접 합 없이 확인), quantum 인수 검사, 초기화도 본다.
 */
public class PartitionedConvolverTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public PartitionedConvolverTest() {
    }

    /**
     * Fills an array with a repeatable noise in <code>[-1, 1)</code>.
     * <p>
     * 배열을 <code>[-1, 1)</code> 의 반복 가능한 잡음으로 채운다.
     *
     * @param n    the length / 길이
     * @param seed the seed / 시드
     * @return the noise / 잡음
     */
    private static float[] noise(int n, long seed) {
        java.util.Random r = new java.util.Random(seed);
        float[] a = new float[n];
        // 샘플마다 난수를 채운다.
        for (int i = 0; i < n; i++) {
            a[i] = (float) (r.nextDouble() * 2 - 1);
        }
        return a;
    }

    /**
     * The direct convolution sum <code>y[n] = scale sum h[k] x[n - k]</code>, computed in double precision.
     * <p>
     * 직접 합성곱 합 <code>y[n] = scale sum h[k] x[n - k]</code> 을 배정밀도로 계산한다.
     *
     * @param x     the input / 입력
     * @param h     the response / 응답
     * @param scale the gain / 이득
     * @return the output with the length of the input / 입력 길이의 출력
     */
    private static double[] direct(float[] x, float[] h, double scale) {
        double[] y = new double[x.length];
        // 출력 프레임마다 응답과 입력의 곱을 더한다.
        for (int n = 0; n < x.length; n++) {
            double s = 0;
            int kmax = Math.min(h.length - 1, n);
            // 응답의 탭마다 지연된 입력과 곱한다.
            for (int k = 0; k <= kmax; k++) {
                s += (double) h[k] * x[n - k];
            }
            y[n] = s * scale;
        }
        return y;
    }

    /**
     * Runs a convolver over a signal in blocks of a quantum.
     * <p>
     * 컨볼버로 신호를 quantum 블록 단위로 처리한다.
     *
     * @param c       the convolver / 컨볼버
     * @param x       the input, whose length is a multiple of the quantum / 입력. 길이는 quantum 의 배수
     * @param quantum the quantum size / quantum 크기
     * @return the output / 출력
     */
    private static float[] run(PartitionedConvolver c, float[] x, int quantum) {
        float[] y = new float[x.length];
        float[] in = new float[quantum];
        float[] out = new float[quantum];
        // quantum 블록마다 처리한다.
        for (int s = 0; s < x.length; s += quantum) {
            System.arraycopy(x, s, in, 0, quantum);
            c.process(in, out, quantum);
            System.arraycopy(out, 0, y, s, quantum);
        }
        return y;
    }

    /**
     * Returns the largest absolute difference between the convolver output and the direct sum.
     * <p>
     * 컨볼버 출력과 직접 합의 최대 절대 차이를 반환한다.
     *
     * @param n       the length of the response / 응답의 길이
     * @param quantum the quantum size / quantum 크기
     * @param scale   the gain / 이득
     * @return the largest difference / 최대 차이
     */
    private static double maxError(int n, int quantum, double scale) {
        float[] h = noise(n, 11L + n);
        int len = ((n + 700 + quantum - 1) / quantum) * quantum;
        float[] x = noise(len, 5L + n);
        float[] y = run(new PartitionedConvolver(new ConvolutionKernel(h, n, scale, quantum)), x, quantum);
        double[] e = direct(x, h, scale);
        double worst = 0;
        // 프레임마다 직접 합과의 차이를 구한다.
        for (int i = 0; i < len; i++) {
            worst = Math.max(worst, Math.abs(e[i] - y[i]));
        }
        return worst;
    }

    /**
     * The output equals the direct convolution sum for lengths on both sides of the boundaries between the levels of the quantum 128
     * (512, 2048 and 8192 taps), with an absolute error far below the resolution of the audio (the spectra are stored as floats, so the
     * error grows with the square root of the length: below <code>5e-5</code> for noise of unit amplitude).
     * <p>
     * 출력은 quantum 128 의 레벨 사이 경계(512, 2048, 8192 탭) 양쪽의 길이에서 직접 합성곱 합과 같다. 절대 오차는 오디오의 해상도보다 훨씬 작다(스펙트럼을
     * float 로 저장하므로 오차가 길이의 제곱근에 따라 커진다. 진폭 1 의 잡음에서 <code>5e-5</code> 미만).
     */
    public void testMatchesDirectSumAtLevelBoundaries() {
        int[] lengths = {1, 2, 127, 128, 129, 511, 512, 513, 2047, 2048, 2049, 8191, 8192, 8193, 12000};
        // 길이마다 직접 합과 비교한다.
        for (int i = 0; i < lengths.length; i++) {
            double err = maxError(lengths[i], 128, 1.0);
            Check.isTrue(err < 5e-5, "length " + lengths[i] + " differs from the direct sum by " + err);
        }
    }

    /**
     * The layout has the number of levels that the boundaries imply: up to 512 taps one level, up to 2048 two, up to 8192 three, and
     * more taps four (the last level takes the rest).
     * <p>
     * 배치는 경계에서 정해지는 레벨 수를 가진다: 512 탭까지 하나, 2048 까지 둘, 8192 까지 셋, 그보다 많으면 넷(마지막 레벨이 나머지를 맡는다).
     */
    public void testLevelLayout() {
        int[] lengths = {1, 512, 513, 2048, 2049, 8192, 8193, 100000};
        int[] levels = {1, 1, 2, 2, 3, 3, 4, 4};
        // 길이마다 레벨 수를 확인한다.
        for (int i = 0; i < lengths.length; i++) {
            ConvolutionKernel k = new ConvolutionKernel(new float[lengths[i]], lengths[i], 1.0, 128);
            Check.equal((long) levels[i], (long) k.getLevelCount(), "levels of " + lengths[i] + " taps");
            Check.equal((long) lengths[i], (long) k.getLength(), "length");
        }
    }

    /**
     * Other render quantum sizes work too (64 and 256 frames), and the scale multiplies the result.
     * <p>
     * 다른 render quantum 크기(64, 256 프레임)도 동작하고 배율은 결과에 곱해진다.
     */
    public void testOtherQuantumSizesAndScale() {
        Check.isTrue(maxError(3000, 64, 1.0) < 5e-5, "quantum 64");
        Check.isTrue(maxError(3000, 256, 1.0) < 5e-5, "quantum 256");
        Check.isTrue(maxError(3000, 128, 0.25) < 5e-5, "a scale of 0.25");
        Check.isTrue(maxError(700, 1, 1.0) < 5e-5, "quantum 1 (a block of one frame)");
    }

    /**
     * An impulse at the first frame gives the response from the first frame (no latency), exactly the taps of the response.
     * <p>
     * 첫 프레임의 임펄스는 첫 프레임부터 응답을 준다(지연 없음). 응답의 탭 그대로이다.
     */
    public void testNoLatency() {
        float[] h = noise(3000, 99L);
        float[] x = new float[3200];
        x[0] = 1f;
        float[] y = run(new PartitionedConvolver(new ConvolutionKernel(h, 3000, 1.0, 128)), x, 128);
        // 응답의 탭이 첫 프레임부터 그대로 나온다.
        for (int i = 0; i < 3000; i++) {
            Check.near(h[i], y[i], 1e-6, "tap " + i);
        }
        // 응답 뒤는 무음이다.
        for (int i = 3000; i < 3200; i++) {
            Check.near(0.0, y[i], 1e-6, "after the response " + i);
        }
    }

    /**
     * A long sparse response with taps in every level (an impulse at 0, 127, 128, 511, 512, 2047, 2048, 8191, 8192, 40000 and 99999) is
     * convolved correctly with a noise input: the expectation is the weighted sum of delayed inputs.
     * <p>
     * 모든 레벨에 탭이 있는 긴 성긴 응답(0, 127, 128, 511, 512, 2047, 2048, 8191, 8192, 40000, 99999 위치의 임펄스)이 잡음 입력과 올바르게 합성곱된다.
     * 기대값은 지연된 입력의 가중 합이다.
     */
    public void testLongSparseResponse() {
        int[] taps = {0, 127, 128, 511, 512, 2047, 2048, 8191, 8192, 40000, 99999};
        float[] h = new float[100000];
        // 탭 위치마다 서로 다른 가중치를 둔다.
        for (int i = 0; i < taps.length; i++) {
            h[taps[i]] = 0.5f + 0.1f * i;
        }
        int len = 102400;
        float[] x = noise(len, 3L);
        float[] y = run(new PartitionedConvolver(new ConvolutionKernel(h, 100000, 1.0, 128)), x, 128);
        double worst = 0;
        // 프레임마다 가중 합을 직접 계산해 비교한다.
        for (int n = 0; n < len; n++) {
            double e = 0;
            // 탭마다 지연된 입력에 가중치를 곱한다.
            for (int i = 0; i < taps.length; i++) {
                if (n - taps[i] >= 0) {
                    e += (0.5 + 0.1 * i) * x[n - taps[i]];
                }
            }
            worst = Math.max(worst, Math.abs(e - y[n]));
        }
        Check.isTrue(worst < 1e-5, "the sparse long response differs by " + worst);
    }

    /**
     * The argument checks, and the reset that clears the history so that the output of the next run equals the output of the first run.
     * <p>
     * 인수 검사와, 이력을 지워 다음 실행의 출력이 첫 실행의 출력과 같아지게 하는 초기화.
     */
    public void testArgumentsAndReset() {
        final float[] h = noise(1000, 4L);
        final PartitionedConvolver c = new PartitionedConvolver(new ConvolutionKernel(h, 1000, 1.0, 128));
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                c.process(new float[100], new float[100], 100);
            }
        }, "a block that is not a quantum");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new ConvolutionKernel(h, 1000, 1.0, 100);
            }
        }, "a quantum that is not a power of two");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new ConvolutionKernel(h, 0, 1.0, 128);
            }
        }, "an empty response");
        float[] x = noise(1024, 8L);
        float[] first = run(c, x, 128);
        c.reset();
        float[] second = run(c, x, 128);
        // 초기화 뒤의 출력은 첫 출력과 같다.
        for (int i = 0; i < x.length; i++) {
            Check.near(first[i], second[i], 0.0, "frame " + i + " after the reset");
        }
    }
}
