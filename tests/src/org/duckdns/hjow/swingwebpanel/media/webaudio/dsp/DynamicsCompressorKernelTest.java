package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the dynamics compressor kernel (Web Audio API 1.19.4, plan item P25-04): the constraints that the specification puts on the
 * compression curve (the identity up to the threshold, monotonic, continuous, the slope <code>1 / ratio</code> after the knee), the
 * makeup gain <code>(1 / curve(1)) ^ 0.6</code> against a hard knee curve written out in the test (a straight line in decibels), the
 * steady state output for constant inputs against the same line, the metered reduction, the look-ahead of 6 ms (the first frame at
 * which a step is heard), the linking of the channels, and the dependence of the attack and release time on the parameters (the time
 * to cross the half way point is proportional to the parameter).
 * <p>
 * 다이내믹 컴프레서 커널 시험(Web Audio API 1.19.4, 계획 P25-04): 규격이 압축 곡선에 두는 제약(임계값까지 항등, 단조, 연속, 무릎 뒤 기울기
 * <code>1 / ratio</code>), 메이크업 게인 <code>(1 / curve(1)) ^ 0.6</code> 을 시험에 직접 적은 하드 니 곡선(데시벨의 직선)과 비교, 일정한 입력의 정상 상태
 * 출력을 같은 직선과 비교, 측정한 감소량, 6 ms 선행 지연(스텝이 처음 들리는 프레임), 채널 연결, 어택과 릴리즈 시간이 파라미터에 따라 바뀌는 것(중간 지점을
 * 지나는 시간이 파라미터에 비례)을 확인한다.
 */
public class DynamicsCompressorKernelTest {

    /**
     * The sample rate of the tests.
     * <p>
     * 시험의 샘플링 주파수.
     */
    private static final double RATE = 44100.0;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public DynamicsCompressorKernelTest() {
    }

    /**
     * Converts decibels to a linear amplitude.
     * <p>
     * 데시벨을 선형 진폭으로 바꾼다.
     *
     * @param db the decibels / 데시벨
     * @return the amplitude / 진폭
     */
    private static double lin(double db) {
        return Math.pow(10.0, db / 20.0);
    }

    /**
     * Converts a linear amplitude to decibels.
     * <p>
     * 선형 진폭을 데시벨로 바꾼다.
     *
     * @param x the amplitude / 진폭
     * @return the decibels / 데시벨
     */
    private static double db(double x) {
        return 20.0 * Math.log10(x);
    }

    /**
     * The hard knee curve in decibels: the identity up to the threshold and a line with the slope <code>1 / ratio</code> after it.
     * <p>
     * 하드 니 곡선(데시벨): 임계값까지는 항등이고 그 뒤는 기울기 <code>1 / ratio</code> 인 직선.
     *
     * @param xDb       the input in decibels / 입력(dB)
     * @param thrDb     the threshold in decibels / 임계값(dB)
     * @param ratio     the ratio / 비율
     * @return the output in decibels / 출력(dB)
     */
    private static double hard(double xDb, double thrDb, double ratio) {
        return xDb <= thrDb ? xDb : thrDb + (xDb - thrDb) / ratio;
    }

    /**
     * Runs a mono signal through a kernel in blocks of 128 frames and returns the output.
     * <p>
     * 모노 신호를 커널에 128 프레임 블록으로 통과시켜 출력을 반환한다.
     *
     * @param k       the kernel / 커널
     * @param x       the input, whose length is a multiple of 128 / 입력. 길이는 128 의 배수
     * @param thr     the threshold / 임계값
     * @param knee    the knee / 무릎
     * @param ratio   the ratio / 비율
     * @param attack  the attack time / 어택 시간
     * @param release the release time / 릴리즈 시간
     * @return the output / 출력
     */
    private static float[] run(DynamicsCompressorKernel k, float[] x, double thr, double knee, double ratio, double attack, double release) {
        float[] y = new float[x.length];
        float[] in = new float[128];
        float[] out = new float[128];
        float[][] inA = {in};
        float[][] outA = {out};
        // 128 프레임 블록마다 처리한다.
        for (int s = 0; s < x.length; s += 128) {
            System.arraycopy(x, s, in, 0, 128);
            k.process(inA, outA, 1, 128, thr, knee, ratio, attack, release);
            System.arraycopy(out, 0, y, s, 128);
        }
        return y;
    }

    /**
     * Creates a constant signal.
     * <p>
     * 일정한 신호를 만든다.
     *
     * @param n     the length / 길이
     * @param value the value / 값
     * @return the signal / 신호
     */
    private static float[] constant(int n, double value) {
        float[] x = new float[n];
        java.util.Arrays.fill(x, (float) value);
        return x;
    }

    /**
     * The constraints of the specification on the compression curve with the default parameters: the identity up to the threshold, a
     * monotonically increasing and continuous curve, and after the end of the knee a line of the slope <code>1 / ratio</code> in
     * decibels; the slope just before the end of the knee is close to that slope too (the knee is smooth).
     * <p>
     * 기본 파라미터에서 압축 곡선에 대한 규격의 제약: 임계값까지 항등이고, 단조 증가하며 연속이고, 무릎 끝 뒤에는 데시벨에서 기울기 <code>1 / ratio</code> 인
     * 직선이다. 무릎 끝 직전의 기울기도 그 기울기에 가깝다(무릎이 부드럽다).
     */
    public void testCurveConstraints() {
        DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 2);
        k.setStaticCurve(-24, 30, 12);
        double prev = Double.NEGATIVE_INFINITY;
        // 입력 -100 ~ +20 dB 를 0.25 dB 간격으로 훑는다.
        for (double x = -100; x <= 20; x += 0.25) {
            double y = db(k.curve(lin(x)));
            if (x < -24) {
                Check.near(x, y, 1e-9, "the identity below the threshold at " + x + " dB");
            }
            Check.isTrue(y >= prev - 1e-9, "the curve is monotonic at " + x + " dB");
            Check.isTrue(y - prev < 0.3 || prev == Double.NEGATIVE_INFINITY, "the curve is continuous at " + x + " dB: step " + (y - prev));
            if (x >= 6.0) {
                // 무릎 끝(-24 + 30 = +6 dB) 뒤에는 기울기 1/12 인 직선이다.
                Check.near(db(k.curve(lin(6.0))) + (x - 6.0) / 12.0, y, 1e-9, "the line after the knee at " + x + " dB");
            }
            prev = y;
        }
        double slopeBeforeKnee = (db(k.curve(lin(5.9))) - db(k.curve(lin(5.8)))) / 0.1;
        Check.near(1.0 / 12.0, slopeBeforeKnee, 0.02, "the slope just before the end of the knee");
        double slopeAtThreshold = (db(k.curve(lin(-23.9))) - db(k.curve(lin(-24.1)))) / 0.2;
        Check.near(1.0, slopeAtThreshold, 0.03, "the slope at the threshold is 1: the curve leaves the identity smoothly");
    }

    /**
     * With a knee of 0 the curve is a hard knee: the closed form line in decibels, for several ratios, within 0.1 dB; and the makeup gain
     * is <code>(1 / curve(1)) ^ 0.6</code>, which for the hard knee is <code>(10 ^ ((24 - 24 / ratio) / 20)) ^ 0.6</code> at the threshold
     * -24 dB.
     * <p>
     * 무릎이 0 이면 곡선은 하드 니이다: 여러 비율에서 데시벨의 닫힌 식 직선과 0.1 dB 안에서 같다. 메이크업 게인은 <code>(1 / curve(1)) ^ 0.6</code> 이며
     * 하드 니에서 임계값 -24 dB 이면 <code>(10 ^ ((24 - 24 / ratio) / 20)) ^ 0.6</code> 이다.
     */
    public void testHardKneeAndMakeupGain() {
        double[] ratios = {2, 4, 12, 20};
        // 비율마다 하드 니 직선과 메이크업 게인을 비교한다.
        for (int i = 0; i < ratios.length; i++) {
            DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 2);
            k.setStaticCurve(-24, 0, ratios[i]);
            // 입력 -60 ~ +12 dB 를 훑는다.
            for (double x = -60; x <= 12; x += 1.0) {
                Check.near(hard(x, -24, ratios[i]), db(k.curve(lin(x))), 0.1, "ratio " + ratios[i] + " at " + x + " dB");
            }
            double expectedMakeup = Math.pow(1.0 / lin(hard(0, -24, ratios[i])), 0.6);
            Check.near(expectedMakeup, k.getMakeupGain(), expectedMakeup * 0.01, "makeup gain of ratio " + ratios[i]);
        }
    }

    /**
     * The steady state of a constant input: the compressor gain settles at the attenuation of the curve, so the output is the
     * makeup gain times the curve of the input amplitude (for amplitudes below the threshold the identity). The expectation is the hard
     * knee line of the test.
     * <p>
     * 일정한 입력의 정상 상태: 컴프레서 게인이 곡선의 감쇠로 수렴하므로 출력은 메이크업 게인에 입력 진폭의 곡선을 곱한 값이다(임계값 아래 진폭은 항등).
     * 기대값은 시험의 하드 니 직선이다.
     */
    public void testSteadyStateOfConstantInputs() {
        double[] levels = {0.001, 0.05, 0.2, 0.5, 1.0};
        // 진폭마다 0.6 초 동안 일정한 입력을 넣고 끝의 출력을 직선과 비교한다.
        for (int i = 0; i < levels.length; i++) {
            DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 1);
            float[] y = run(k, constant(26496, levels[i]), -24, 0, 4, 0.003, 0.25);
            double makeup = Math.pow(1.0 / lin(hard(0, -24, 4)), 0.6);
            double expected = makeup * lin(hard(db(levels[i]), -24, 4));
            Check.near(expected, y[26495], expected * 0.015, "output for the input " + levels[i]);
        }
    }

    /**
     * The metered reduction is 0 without a signal and after the first quantum of a quiet signal, and for a constant input above the
     * threshold it settles at the decibel difference between the curve and the input (the attenuation that the compressor applies
     * before the makeup gain): -13.48 dB for 0.5 at the threshold -24 dB and the ratio 4.
     * <p>
     * 측정한 감소량은 신호가 없을 때와 조용한 신호의 첫 quantum 뒤에 0 이고, 임계값 위의 일정한 입력에서는 곡선과 입력의 데시벨 차이(메이크업 게인 앞에서
     * 컴프레서가 적용하는 감쇠)로 수렴한다. 임계값 -24 dB, 비율 4 에서 0.5 는 -13.48 dB 이다.
     */
    public void testMeteredReduction() {
        DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 1);
        Check.near(0.0, k.getReduction(), 0.0, "the reduction starts at 0");
        // 조용한 신호(임계값 아래)를 3 초 넣으면 시작할 때의 짧은 감쇠(검출기가 0 에서 올라오는 동안)가 측정 시간 상수의 아홉 배 동안 회복해 0 에 가까워진다.
        run(k, constant(132352, 0.01), -24, 0, 4, 0.003, 0.25);
        Check.near(0.0, k.getReduction(), 0.01, "a quiet signal: no reduction once the start-up dip has recovered");
        k = new DynamicsCompressorKernel(RATE, 1);
        run(k, constant(66048, 0.5), -24, 0, 4, 0.003, 0.25);
        double expected = hard(db(0.5), -24, 4) - db(0.5);
        Check.near(expected, k.getReduction(), 0.15, "reduction for 0.5");
        Check.isTrue(k.getReduction() < 0, "the reduction is negative decibels");
        k.reset();
        Check.near(0.0, k.getReduction(), 0.0, "reset clears the reduction");
    }

    /**
     * The look-ahead: a step of a quiet constant input at the frame 1000 is first heard at the frame 1000 + 264 (6 ms at 44100 Hz); the
     * frames before it are exactly silent.
     * <p>
     * 선행 지연: 프레임 1000 에서 시작하는 조용한 일정 입력의 스텝은 프레임 1000 + 264(44100 Hz 에서 6 ms)에서 처음 들린다. 그 앞 프레임은 정확히 무음이다.
     */
    public void testLookAhead() {
        DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 1);
        Check.equal(264L, (long) k.getPreDelayFrames(), "6 ms at 44100 Hz");
        float[] x = new float[2048];
        // 프레임 1000 부터 일정한 입력을 둔다.
        for (int i = 1000; i < 2048; i++) {
            x[i] = 0.001f;
        }
        float[] y = run(k, x, -24, 30, 12, 0.003, 0.25);
        // 스텝 앞과 선행 지연 동안은 무음이다.
        for (int i = 0; i < 1264; i++) {
            Check.near(0.0, y[i], 0.0, "frame " + i + " is silent");
        }
        Check.isTrue(y[1264] != 0f, "the step is heard at 1264");
        Check.isTrue(k.getTailFrames() > 264 + 5 * 0.325 * RATE - 1, "the tail covers the look-ahead and the metering");
    }

    /**
     * The channels are linked: a stereo input with different levels gets the same gain on both channels (the ratio of the outputs
     * equals the ratio of the inputs), which follows the louder channel; the output has as many channels as the input.
     * <p>
     * 채널이 묶여 있다: 레벨이 다른 스테레오 입력은 두 채널이 같은 게인을 받고(출력의 비는 입력의 비와 같다) 그 게인은 더 큰 채널을 따른다. 출력은 입력과 같은
     * 채널 수이다.
     */
    public void testChannelsAreLinked() {
        DynamicsCompressorKernel k = new DynamicsCompressorKernel(RATE, 2);
        float[] l = new float[128];
        float[] r = new float[128];
        float[] ol = new float[128];
        float[] or = new float[128];
        java.util.Arrays.fill(l, 0.5f);
        java.util.Arrays.fill(r, 0.1f);
        float[][] in = {l, r};
        float[][] out = {ol, or};
        // 0.4 초 동안 같은 블록을 반복해 정상 상태까지 보낸다.
        for (int b = 0; b < 140; b++) {
            k.process(in, out, 2, 128, -24, 0, 4, 0.003, 0.25);
        }
        Check.near(5.0, ol[100] / or[100], 1e-4, "the ratio of the outputs is the ratio of the inputs");
        double gainOfLouder = ol[100] / 0.5;
        // 더 큰 채널(0.5)만으로 정한 단일 채널 컴프레서와 같은 게인이다.
        DynamicsCompressorKernel mono = new DynamicsCompressorKernel(RATE, 1);
        float[] one = new float[128];
        java.util.Arrays.fill(one, 0.5f);
        float[] oone = new float[128];
        float[][] inOne = {one};
        float[][] outOne = {oone};
        // 모노 컴프레서도 같은 횟수만큼 처리한다.
        for (int b = 0; b < 140; b++) {
            mono.process(inOne, outOne, 1, 128, -24, 0, 4, 0.003, 0.25);
        }
        Check.near(oone[100] / 0.5, gainOfLouder, 1e-5, "the gain follows the louder channel");
    }

    /**
     * Returns the number of frames after the step of the input at which the output gain has gone a fraction of the way from its initial
     * to its final value (the output of the step is heard after the look-ahead of 264 frames, so the search starts there).
     * <p>
     * 입력의 스텝 뒤 출력 게인이 처음 값에서 마지막 값까지 일정 비율을 지난 프레임 수를 반환한다(스텝의 출력은 선행 지연 264 프레임 뒤에 들리므로 거기서부터 찾는다).
     *
     * @param before  the level before the step / 스텝 전 레벨
     * @param after   the level after the step / 스텝 후 레벨
     * @param attack  the attack time / 어택 시간
     * @param release  the release time / 릴리즈 시간
     * @param fraction the fraction of the way, between 0 and 1 / 지난 비율(0 ~ 1)
     * @return the number of frames to that point / 그 지점까지의 프레임 수
     */
    private static int crossFrames(double before, double after, double attack, double release, double fraction) {
        int settle = 66048;
        int total = settle + 352768;
        float[] x = new float[total];
        // 스텝 전 레벨과 후 레벨을 채운다.
        for (int i = 0; i < total; i++) {
            x[i] = (float) (i < settle ? before : after);
        }
        float[] y = run(new DynamicsCompressorKernel(RATE, 1), x, -24, 30, 12, attack, release);
        double g0 = y[settle - 1] / before;
        double g1 = y[total - 1] / after;
        double half = g0 + (g1 - g0) * fraction;
        // 스텝 뒤 게인이 그 지점을 처음 지나는 프레임을 찾는다.
        for (int i = settle + 264; i < total; i++) {
            double g = y[i] / after;
            if ((g0 > g1 && g <= half) || (g0 < g1 && g >= half)) {
                return i - settle;
            }
        }
        return -1;
    }

    /**
     * The time that the gain needs to go half way grows in proportion to the attack parameter for a step to a loud input: ten times the
     * attack gives about ten times the time. For a step to a quiet input the time to go 90 percent of the way grows about with the
     * release parameter: the release rate (the slew of the gain, a polynomial of the compression difference times the release time) is
     * proportional to it, while the detector, which rises with a fixed time constant of 2.5 ms, adds a part that does not scale, so
     * twice the release gives between 1.6 and 2.1 times the time.
     * <p>
     * 큰 입력으로의 스텝에서 게인이 절반 지점까지 가는 시간은 attack 파라미터에 비례해 늘어난다: attack 이 열 배이면 시간이 약 열 배이다. 조용한 입력으로의
     * 스텝에서 90 퍼센트까지 가는 시간은 대체로 release 파라미터에 따라 늘어난다. 릴리즈 속도(게인의 변화율, 압축 차이의 다항식에 릴리즈 시간을 곱한 값)는
     * 파라미터에 비례하지만 고정 시간 상수 2.5 ms 로 올라오는 검출기가 비례하지 않는 부분을 더하므로, release 가 두 배이면 시간이 1.6 ~ 2.1 배이다.
     */
    public void testAttackAndReleaseScaleWithTheParameters() {
        int a1 = crossFrames(0.001, 1.0, 0.1, 0.25, 0.5);
        int a2 = crossFrames(0.001, 1.0, 1.0, 0.25, 0.5);
        Check.isTrue(a1 > 0 && a2 > 0, "the attack reaches the half way point: " + a1 + ", " + a2);
        double attackRatio = (double) a2 / a1;
        Check.isTrue(attackRatio > 8.0 && attackRatio < 12.0, "ten times the attack time takes " + attackRatio + " times as long");
        int r1 = crossFrames(1.0, 0.001, 0.003, 0.25, 0.9);
        int r2 = crossFrames(1.0, 0.001, 0.003, 0.5, 0.9);
        int r3 = crossFrames(1.0, 0.001, 0.003, 1.0, 0.9);
        Check.isTrue(r1 > 0 && r2 > 0 && r3 > 0, "the release reaches the point: " + r1 + ", " + r2 + ", " + r3);
        double releaseRatio = (double) r2 / r1;
        Check.isTrue(releaseRatio > 1.6 && releaseRatio < 2.1, "twice the release time takes " + releaseRatio + " times as long");
        double releaseRatio2 = (double) r3 / r2;
        Check.isTrue(releaseRatio2 > 1.6 && releaseRatio2 < 2.1, "twice the release time takes " + releaseRatio2 + " times as long");
    }
}
