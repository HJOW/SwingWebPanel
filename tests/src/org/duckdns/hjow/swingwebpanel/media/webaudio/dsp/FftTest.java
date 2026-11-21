package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the FFT (plan item P25-02): the transform of an impulse, of a cosine and of a sine against their closed forms, the round trip
 * with the division by the size, the linearity, and the argument check.
 * <p>
 * FFT 시험(계획 P25-02): 임펄스, 코사인, 사인의 변환을 닫힌 식과 비교하고, 크기로 나누는 왕복, 선형성, 인수 검사를 확인한다.
 */
public class FftTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public FftTest() {
    }

    /**
     * The forward transform of an impulse at 0 is 1 at every bin.
     * <p>
     * 0 에 있는 임펄스의 순방향 변환은 모든 빈에서 1 이다.
     */
    public void testImpulse() {
        Fft fft = new Fft(16);
        double[] re = new double[16];
        double[] im = new double[16];
        re[0] = 1;
        fft.transform(re, im, false);
        // 모든 빈이 1 + 0i 이다.
        for (int k = 0; k < 16; k++) {
            Check.near(1.0, re[k], 1e-12, "re " + k);
            Check.near(0.0, im[k], 1e-12, "im " + k);
        }
    }

    /**
     * The transform of a cosine of bin 3 and a sine of bin 5 has the closed form: <code>N/2</code> at the bins and their mirrors, and
     * <code>-i N/2</code> at the sine bin.
     * <p>
     * 3번 빈의 코사인과 5번 빈의 사인의 변환은 닫힌 식을 가진다: 빈과 그 대칭 빈에서 <code>N/2</code>, 사인 빈에서 <code>-i N/2</code>.
     */
    public void testCosineAndSine() {
        int n = 64;
        Fft fft = new Fft(n);
        double[] re = new double[n];
        double[] im = new double[n];
        // x[i] = cos(2 pi 3 i / N) + sin(2 pi 5 i / N)
        for (int i = 0; i < n; i++) {
            re[i] = Math.cos(2 * Math.PI * 3 * i / n) + Math.sin(2 * Math.PI * 5 * i / n);
        }
        fft.transform(re, im, false);
        Check.near(n / 2.0, re[3], 1e-9, "cosine bin real");
        Check.near(n / 2.0, re[n - 3], 1e-9, "cosine mirror real");
        Check.near(0.0, im[3], 1e-9, "cosine bin imaginary");
        Check.near(0.0, re[5], 1e-9, "sine bin real");
        Check.near(-n / 2.0, im[5], 1e-9, "sine bin imaginary");
        Check.near(n / 2.0, im[n - 5], 1e-9, "sine mirror imaginary");
        Check.near(0.0, re[7], 1e-9, "other bin");
        Check.near(0.0, im[7], 1e-9, "other bin imaginary");
    }

    /**
     * The inverse transform divided by the size restores the input.
     * <p>
     * 크기로 나눈 역변환은 입력을 복원한다.
     */
    public void testRoundTrip() {
        int n = 1024;
        Fft fft = new Fft(n);
        double[] re = new double[n];
        double[] im = new double[n];
        double[] orig = new double[n];
        java.util.Random random = new java.util.Random(7);
        // 임의의 실수 신호를 만든다.
        for (int i = 0; i < n; i++) {
            orig[i] = random.nextDouble() * 2 - 1;
            re[i] = orig[i];
        }
        fft.transform(re, im, false);
        fft.transform(re, im, true);
        // 정규화한 뒤 원래 신호와 비교한다.
        for (int i = 0; i < n; i++) {
            Check.near(orig[i], re[i] / n, 1e-12, "sample " + i);
            Check.near(0.0, im[i] / n, 1e-12, "imaginary " + i);
        }
    }

    /**
     * Sizes that are not a power of two are rejected.
     * <p>
     * 2의 거듭제곱이 아닌 크기는 거부된다.
     */
    public void testSizeCheck() {
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new Fft(12);
            }
        }, "size 12");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new Fft(1);
            }
        }, "size 1");
        Check.equal(8, new Fft(8).getSize(), "size");
    }
}
