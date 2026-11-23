/*
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The signal processing of a dynamics compressor (Web Audio API 1.19, section 1.19.4): a look-ahead pre-delay of 6 ms, a peak detector
 * on the undelayed input that is linked across the channels, the static compression curve with a soft knee, an envelope with an attack
 * and an adaptive release, a fixed makeup gain, and the metering of the gain reduction.
 * <p>
 * The specification fixes the structure and the constraints of the curves and leaves their shape to the implementation. This class
 * follows the design of the Blink implementation (Chromium <code>third_party/blink/renderer/platform/audio/dynamics_compressor.cc</code>,
 * commit 8d101571406d3ef5bda7446b34568192ad99b3b5, read 2026-11-22, BSD license; the notice above is kept because the structure, the
 * constants of the release polynomial and the equations are carried over): an exponential knee whose parameter <code>k</code> is chosen
 * so that the slope after the knee is <code>1 / ratio</code>, a linear segment in decibels after the knee, an attack rate that
 * depends on the largest gain difference of the attack, a release rate from a fourth-order polynomial of the gain difference (the
 * adaptive release zones), the pre-warp by <code>asin</code> and <code>sin</code>, and the makeup gain
 * <code>(1 / curve(1)) ^ 0.6</code>.
 * <p>
 * 다이내믹 컴프레서의 신호 처리(Web Audio API 1.19, 1.19.4절): 6 ms 의 선행 지연, 채널을 묶은 지연 전 입력의 피크 검출기, 부드러운 무릎이 있는 정적 압축
 * 곡선, 어택과 적응형 릴리즈가 있는 엔벨로프, 고정 메이크업 게인, 이득 감소 측정.
 * <p>
 * 규격은 구조와 곡선의 제약만 정하고 곡선 모양은 구현에 맡긴다. 이 클래스는 Blink 구현(Chromium
 * <code>third_party/blink/renderer/platform/audio/dynamics_compressor.cc</code>, 커밋 8d101571406d3ef5bda7446b34568192ad99b3b5, 2026-11-22 열람, BSD
 * 라이선스. 구조, 릴리즈 다항식의 상수, 식을 가져왔으므로 위의 고지를 유지한다)의 설계를 따른다. 무릎 뒤 기울기가 <code>1 / ratio</code> 가 되도록 매개변수
 * <code>k</code> 를 고르는 지수 무릎, 무릎 뒤의 데시벨 직선, 어택의 최대 이득 차이에 따른 어택 속도, 이득 차이의 4차 다항식으로 구하는 릴리즈 속도(적응형 릴리즈
 * 구간), <code>asin</code> 과 <code>sin</code> 으로 하는 사전 왜곡, 메이크업 게인 <code>(1 / curve(1)) ^ 0.6</code> 을 따른다.
 */
public final class DynamicsCompressorKernel {

    /**
     * The look-ahead delay in seconds.
     * <p>
     * 선행 지연(초).
     */
    public static final double PRE_DELAY_SECONDS = 0.006;

    /**
     * The time constant of the metering release in seconds: the metered reduction hits peaks at once and recovers with this constant.
     * <p>
     * 측정 릴리즈의 시간 상수(초): 측정한 감소량은 피크를 즉시 따라가고 이 상수로 회복한다.
     */
    public static final double METERING_RELEASE_TIME_CONSTANT = 0.325;

    /**
     * The release time of the detector in seconds.
     * <p>
     * 검출기의 릴리즈 시간(초).
     */
    private static final double SAT_RELEASE_TIME = 0.0025;

    /**
     * The first release zone (the position of the polynomial at a compression difference of -12 dB).
     * <p>
     * 첫째 릴리즈 구간(압축 차이 -12 dB 에서의 다항식 값).
     */
    private static final double RELEASE_ZONE_1 = 0.09;

    /**
     * The second release zone (-8 dB).
     * <p>
     * 둘째 릴리즈 구간(-8 dB).
     */
    private static final double RELEASE_ZONE_2 = 0.16;

    /**
     * The third release zone (-4 dB).
     * <p>
     * 셋째 릴리즈 구간(-4 dB).
     */
    private static final double RELEASE_ZONE_3 = 0.42;

    /**
     * The fourth release zone (0 dB).
     * <p>
     * 넷째 릴리즈 구간(0 dB).
     */
    private static final double RELEASE_ZONE_4 = 0.98;

    /**
     * The constant term of the release polynomial through the four zones.
     * <p>
     * 네 구간을 지나는 릴리즈 다항식의 상수항.
     */
    private static final double A_BASE = 0.9999999999999998 * RELEASE_ZONE_1 + 1.8432219684323923e-16 * RELEASE_ZONE_2
            - 1.9373394351676423e-16 * RELEASE_ZONE_3 + 8.824516011816245e-18 * RELEASE_ZONE_4;

    /**
     * The coefficient of <code>x</code> of the release polynomial.
     * <p>
     * 릴리즈 다항식의 <code>x</code> 계수.
     */
    private static final double B_BASE = -1.5788320352845888 * RELEASE_ZONE_1 + 2.3305837032074286 * RELEASE_ZONE_2
            - 0.9141194204840429 * RELEASE_ZONE_3 + 0.1623677525612032 * RELEASE_ZONE_4;

    /**
     * The coefficient of <code>x^2</code> of the release polynomial.
     * <p>
     * 릴리즈 다항식의 <code>x^2</code> 계수.
     */
    private static final double C_BASE = 0.5334142869106424 * RELEASE_ZONE_1 - 1.272736789213631 * RELEASE_ZONE_2
            + 0.9258856042207512 * RELEASE_ZONE_3 - 0.18656310191776226 * RELEASE_ZONE_4;

    /**
     * The coefficient of <code>x^3</code> of the release polynomial.
     * <p>
     * 릴리즈 다항식의 <code>x^3</code> 계수.
     */
    private static final double D_BASE = 0.08783463138207234 * RELEASE_ZONE_1 - 0.1694162967925622 * RELEASE_ZONE_2
            + 0.08588057951595272 * RELEASE_ZONE_3 - 0.00429891410546283 * RELEASE_ZONE_4;

    /**
     * The coefficient of <code>x^4</code> of the release polynomial.
     * <p>
     * 릴리즈 다항식의 <code>x^4</code> 계수.
     */
    private static final double E_BASE = -0.042416883008123074 * RELEASE_ZONE_1 + 0.1115693827987602 * RELEASE_ZONE_2
            - 0.09764676325265872 * RELEASE_ZONE_3 + 0.028494263462021576 * RELEASE_ZONE_4;

    /**
     * The number of frames after which the envelope parameters are computed again.
     * <p>
     * 엔벨로프 매개변수를 다시 계산하는 프레임 수.
     */
    private static final int DIVISION_FRAMES = 32;

    /**
     * The sample rate.
     * <p>
     * 샘플링 주파수.
     */
    private final double sampleRate;

    /**
     * The number of frames of the pre-delay.
     * <p>
     * 선행 지연의 프레임 수.
     */
    private final int preDelayFrames;

    /**
     * The pre-delay lines of the channels (circular, a power of two long).
     * <p>
     * 채널의 선행 지연선(원형, 길이는 2 의 거듭제곱).
     */
    private final float[][] preDelay;

    /**
     * The mask of the indices of the pre-delay lines.
     * <p>
     * 선행 지연선 인덱스의 마스크.
     */
    private final int mask;

    /**
     * The index at which the delayed samples are read.
     * <p>
     * 지연된 샘플을 읽는 인덱스.
     */
    private int readIndex;

    /**
     * The index at which the new samples are written.
     * <p>
     * 새 샘플을 쓰는 인덱스.
     */
    private int writeIndex;

    /**
     * The detector average: the smoothed attenuation that the static curve asks for (1 means no compression).
     * <p>
     * 검출기 평균: 정적 곡선이 요구하는 감쇠를 평활한 값(1 은 압축 없음).
     */
    private double detectorAverage;

    /**
     * The compressor gain in the warped domain.
     * <p>
     * 사전 왜곡한 영역에서의 컴프레서 게인.
     */
    private double compressorGain = 1.0;

    /**
     * The metered gain in decibels.
     * <p>
     * 측정한 게인(dB).
     */
    private double meteringGain;

    /**
     * The largest compression difference in decibels of the current attack, or -1 while not attacking.
     * <p>
     * 현재 어택에서 가장 큰 압축 차이(dB). 어택 중이 아니면 -1.
     */
    private double maxAttackCompressionDiff = -1;

    /**
     * The coefficient of the metering recovery per frame.
     * <p>
     * 프레임당 측정 회복 계수.
     */
    private final double meteringReleaseK;

    /**
     * The last reduction in decibels, at the end of the last block.
     * <p>
     * 마지막 블록 끝에서의 마지막 감소량(dB).
     */
    private double reduction;

    /**
     * The static curve: the linear threshold.
     * <p>
     * 정적 곡선: 선형 단위의 임계값.
     */
    private double linearThreshold = -1;

    /**
     * The static curve: the threshold in decibels the curve was built for.
     * <p>
     * 정적 곡선: 곡선을 만든 임계값(dB).
     */
    private double dbThreshold = Double.NaN;

    /**
     * The static curve: the knee in decibels the curve was built for.
     * <p>
     * 정적 곡선: 곡선을 만든 무릎(dB).
     */
    private double dbKnee = Double.NaN;

    /**
     * The static curve: the ratio the curve was built for.
     * <p>
     * 정적 곡선: 곡선을 만든 비율.
     */
    private double ratio = Double.NaN;

    /**
     * The static curve: the slope in decibels after the knee, <code>1 / ratio</code>.
     * <p>
     * 정적 곡선: 무릎 뒤 데시벨 기울기 <code>1 / ratio</code>.
     */
    private double slope;

    /**
     * The static curve: the end of the knee in linear units.
     * <p>
     * 정적 곡선: 선형 단위의 무릎 끝.
     */
    private double kneeThreshold;

    /**
     * The static curve: the end of the knee in decibels.
     * <p>
     * 정적 곡선: 무릎 끝(dB).
     */
    private double dbKneeThreshold;

    /**
     * The static curve: the output at the end of the knee in decibels.
     * <p>
     * 정적 곡선: 무릎 끝에서의 출력(dB).
     */
    private double dbYKneeThreshold;

    /**
     * The static curve: the exponent of the knee curve.
     * <p>
     * 정적 곡선: 무릎 곡선의 지수.
     */
    private double kneeK;

    /**
     * Creates a compressor with an empty state.
     * <p>
     * 상태가 빈 컴프레서를 만든다.
     *
     * @param sampleRate the sample rate in Hz / 샘플링 주파수(Hz)
     * @param channels   the largest number of channels, 1 or 2 / 최대 채널 수(1 또는 2)
     */
    public DynamicsCompressorKernel(double sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.preDelayFrames = (int) (PRE_DELAY_SECONDS * sampleRate);
        int size = 1;
        // 선행 지연 프레임 수보다 큰 2 의 거듭제곱을 고른다.
        while (size <= preDelayFrames) {
            size <<= 1;
        }
        this.preDelay = new float[channels][size];
        this.mask = size - 1;
        this.meteringReleaseK = 1.0 - Math.exp(-1.0 / (METERING_RELEASE_TIME_CONSTANT * sampleRate));
        reset();
    }

    /**
     * Returns the number of frames of the look-ahead delay (the latency of the node).
     * <p>
     * 선행 지연의 프레임 수(노드의 지연)를 반환한다.
     *
     * @return the pre-delay in frames / 선행 지연(프레임)
     */
    public int getPreDelayFrames() {
        return preDelayFrames;
    }

    /**
     * Returns the number of frames after which the metered reduction has fallen to about 0.7 percent of its last value (five time
     * constants of the metering): the tail time of the node together with the pre-delay.
     * <p>
     * 측정한 감소량이 마지막 값의 약 0.7 퍼센트로 줄어드는 프레임 수(측정 시간 상수의 다섯 배)를 반환한다. 선행 지연과 함께 노드의 꼬리 시간이다.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    public int getTailFrames() {
        return preDelayFrames + (int) Math.ceil(5.0 * METERING_RELEASE_TIME_CONSTANT * sampleRate);
    }

    /**
     * Returns the gain reduction in decibels that was metered at the end of the last block (0 or less).
     * <p>
     * 마지막 블록 끝에서 측정한 이득 감소량(dB, 0 이하)을 반환한다.
     *
     * @return the reduction / 감소량
     */
    public double getReduction() {
        return reduction;
    }

    /**
     * Clears the state: the pre-delay lines, the detector, the gain and the metering.
     * <p>
     * 상태를 지운다: 선행 지연선, 검출기, 게인, 측정.
     */
    public void reset() {
        detectorAverage = 0.0;
        compressorGain = 1.0;
        meteringGain = 0.0;
        reduction = 0.0;
        maxAttackCompressionDiff = -1;
        // 채널마다 선행 지연선을 지운다.
        for (int c = 0; c < preDelay.length; c++) {
            java.util.Arrays.fill(preDelay[c], 0f);
        }
        readIndex = 0;
        writeIndex = preDelayFrames;
    }

    /**
     * Converts a linear amplitude to decibels; 0 is -1000 dB.
     * <p>
     * 선형 진폭을 데시벨로 바꾼다. 0 은 -1000 dB 이다.
     *
     * @param linear the amplitude / 진폭
     * @return the decibels / 데시벨
     */
    static double linearToDecibels(double linear) {
        return linear != 0.0 ? 20.0 * Math.log10(linear) : -1000.0;
    }

    /**
     * Converts decibels to a linear amplitude.
     * <p>
     * 데시벨을 선형 진폭으로 바꾼다.
     *
     * @param db the decibels / 데시벨
     * @return the amplitude / 진폭
     */
    static double decibelsToLinear(double db) {
        return Math.pow(10.0, db / 20.0);
    }

    /**
     * The exponential knee: the identity up to the linear threshold, then <code>lt + (1 - exp(-k (x - lt))) / k</code>, which matches
     * the slope 1 at the threshold and approaches <code>lt + 1 / k</code>.
     * <p>
     * 지수 무릎: 선형 임계값까지는 항등이고 그 뒤는 <code>lt + (1 - exp(-k (x - lt))) / k</code> 이다. 임계값에서 기울기 1 이 맞고
     * <code>lt + 1 / k</code> 에 점근한다.
     *
     * @param x the input / 입력
     * @param k the exponent / 지수
     * @return the output / 출력
     */
    private double kneeCurve(double x, double k) {
        if (x < linearThreshold) {
            return x;
        }
        return linearThreshold + (1.0 - Math.exp(-k * (x - linearThreshold))) / k;
    }

    /**
     * The whole compression curve for a linear amplitude: the knee up to the end of the knee, then a line in decibels with the slope
     * <code>1 / ratio</code>.
     * <p>
     * 선형 진폭에 대한 전체 압축 곡선: 무릎 끝까지는 무릎 곡선이고 그 뒤는 기울기 <code>1 / ratio</code> 인 데시벨 직선이다.
     *
     * @param x the input amplitude / 입력 진폭
     * @param k the exponent of the knee / 무릎의 지수
     * @return the output amplitude / 출력 진폭
     */
    private double saturate(double x, double k) {
        if (x < kneeThreshold) {
            return kneeCurve(x, k);
        }
        double dbX = linearToDecibels(x);
        double dbY = dbYKneeThreshold + slope * (dbX - dbKneeThreshold);
        return decibelsToLinear(dbY);
    }

    /**
     * Finds the exponent of the knee for which the slope of the curve in decibels at the end of the knee is the wanted slope, by a
     * geometric bisection of 15 steps between 0.1 and 10000.
     * <p>
     * 무릎 끝에서의 데시벨 기울기가 원하는 기울기가 되는 무릎 지수를 0.1 ~ 10000 사이의 15단계 기하 이분법으로 찾는다.
     *
     * @param desiredSlope the slope / 기울기
     * @return the exponent / 지수
     */
    private double kAtSlope(double desiredSlope) {
        double dbX = dbThreshold + dbKnee;
        double x = decibelsToLinear(dbX);
        double x2 = 1.0;
        double dbX2 = 0.0;
        if (!(x < linearThreshold)) {
            x2 = x * 1.001;
            dbX2 = linearToDecibels(x2);
        }
        double minK = 0.1;
        double maxK = 10000.0;
        double k = 5.0;
        double s = 1.0;
        // 기울기가 원하는 값보다 작으면 k 가 너무 크고, 크면 너무 작다. 기하 평균으로 범위를 좁힌다.
        for (int i = 0; i < 15; i++) {
            if (!(x < linearThreshold)) {
                double dbY = linearToDecibels(kneeCurve(x, k));
                double dbY2 = linearToDecibels(kneeCurve(x2, k));
                s = (dbY2 - dbY) / (dbX2 - dbX);
            }
            if (s < desiredSlope) {
                maxK = k;
            } else {
                minK = k;
            }
            k = Math.sqrt(minK * maxK);
        }
        return k;
    }

    /**
     * Builds the static curve for a threshold, a knee and a ratio (only when one of them changed) and returns the exponent of the knee.
     * <p>
     * 임계값, 무릎, 비율에 대한 정적 곡선을 만들고(그중 하나가 바뀐 경우에만) 무릎 지수를 반환한다.
     *
     * @param thresholdDb the threshold in decibels / 임계값(dB)
     * @param kneeDb      the knee in decibels / 무릎(dB)
     * @param newRatio    the ratio / 비율
     * @return the exponent of the knee / 무릎 지수
     */
    public double setStaticCurve(double thresholdDb, double kneeDb, double newRatio) {
        if (thresholdDb != dbThreshold || kneeDb != dbKnee || newRatio != ratio) {
            dbThreshold = thresholdDb;
            linearThreshold = decibelsToLinear(thresholdDb);
            dbKnee = kneeDb;
            ratio = newRatio;
            slope = 1.0 / newRatio;
            double k = kAtSlope(slope);
            dbKneeThreshold = thresholdDb + kneeDb;
            kneeThreshold = decibelsToLinear(dbKneeThreshold);
            dbYKneeThreshold = linearToDecibels(kneeCurve(kneeThreshold, k));
            kneeK = k;
        }
        return kneeK;
    }

    /**
     * Applies the static compression curve (as built by {@link #setStaticCurve}) to a linear amplitude.
     * <p>
     * ({@link #setStaticCurve} 로 만든) 정적 압축 곡선을 선형 진폭에 적용한다.
     *
     * @param x the input amplitude, 0 or more / 입력 진폭(0 이상)
     * @return the output amplitude / 출력 진폭
     */
    public double curve(double x) {
        return saturate(x, kneeK);
    }

    /**
     * Returns the makeup gain of the current static curve: <code>(1 / curve(1)) ^ 0.6</code>.
     * <p>
     * 현재 정적 곡선의 메이크업 게인 <code>(1 / curve(1)) ^ 0.6</code> 을 반환한다.
     *
     * @return the gain / 게인
     */
    public double getMakeupGain() {
        return Math.pow(1.0 / saturate(1.0, kneeK), 0.6);
    }

    /**
     * Returns a value if it is finite and otherwise a default.
     * <p>
     * 값이 유한하면 그 값을, 아니면 기본값을 반환한다.
     *
     * @param x            the value / 값
     * @param defaultValue the default / 기본값
     * @return the value or the default / 값 또는 기본값
     */
    private static double ensureFinite(double x, double defaultValue) {
        return (Double.isNaN(x) || Double.isInfinite(x)) ? defaultValue : x;
    }

    /**
     * Compresses a block. The channels of the input are read for every frame (the detector uses the largest absolute value of all
     * channels, so every channel gets the same gain), and the output has the same number of channels.
     * <p>
     * 블록을 압축한다. 프레임마다 입력의 채널을 읽고(검출기는 모든 채널의 절댓값 최댓값을 쓰므로 모든 채널이 같은 게인을 받는다) 출력은 같은 채널 수이다.
     *
     * @param in          the input channels / 입력 채널
     * @param out         the output channels, as many as the input / 출력 채널(입력과 같은 수)
     * @param channels    the number of channels, at most the number given to the constructor / 채널 수(생성자에 준 수 이하)
     * @param frames      the number of frames / 프레임 수
     * @param thresholdDb the threshold in decibels / 임계값(dB)
     * @param kneeDb      the knee in decibels / 무릎(dB)
     * @param newRatio    the ratio / 비율
     * @param attackTime  the attack time in seconds / 어택 시간(초)
     * @param releaseTime the release time in seconds / 릴리즈 시간(초)
     */
    public void process(float[][] in, float[][] out, int channels, int frames, double thresholdDb, double kneeDb, double newRatio,
            double attackTime, double releaseTime) {
        double k = setStaticCurve(thresholdDb, kneeDb, newRatio);
        // 메이크업 게인은 실험적으로 맞춘 값이다.
        double linearPostGain = Math.pow(1.0 / saturate(1.0, k), 0.6);
        double attackFrames = Math.max(0.001, attackTime) * sampleRate;
        double releaseFrames = sampleRate * releaseTime;
        double satReleaseFrames = SAT_RELEASE_TIME * sampleRate;
        double a = releaseFrames * A_BASE;
        double b = releaseFrames * B_BASE;
        double c = releaseFrames * C_BASE;
        double d = releaseFrames * D_BASE;
        double e = releaseFrames * E_BASE;
        int frameIndex = 0;
        // 32 프레임씩 나눠 엔벨로프 속도를 다시 계산하고 그 구간의 프레임을 처리한다.
        while (frameIndex < frames) {
            detectorAverage = ensureFinite(detectorAverage, 1.0);
            double desiredGain = Math.min(1.0, detectorAverage);
            // 사인으로 사전 왜곡하기 전에 원하는 게인을 asin 으로 미리 왜곡한다.
            double scaledDesiredGain = Math.asin(desiredGain) / (Math.PI / 2.0);
            boolean releasing = scaledDesiredGain > compressorGain;
            double dbCompressionDiff;
            if (scaledDesiredGain == 0) {
                dbCompressionDiff = releasing ? -1 : 1;
            } else {
                dbCompressionDiff = linearToDecibels(compressorGain / scaledDesiredGain);
            }
            double envelopeRate;
            if (releasing) {
                // 릴리즈: 압축이 깊을수록 빠르게 풀리도록 압축 차이의 4차 다항식으로 릴리즈 프레임을 구한다.
                maxAttackCompressionDiff = -1;
                dbCompressionDiff = ensureFinite(dbCompressionDiff, -1);
                double x = Math.max(-12.0, Math.min(0.0, dbCompressionDiff));
                x = 0.25 * (x + 12.0);
                double x2 = x * x;
                double calcReleaseFrames = a + b * x + c * x2 + d * x2 * x + e * x2 * x2;
                double dbPerFrame = 5.0 / calcReleaseFrames;
                envelopeRate = decibelsToLinear(dbPerFrame);
            } else {
                // 어택: 어택 동안 만난 가장 큰 압축 차이로 속도를 정한다.
                dbCompressionDiff = ensureFinite(dbCompressionDiff, 1);
                if (maxAttackCompressionDiff == -1 || maxAttackCompressionDiff < dbCompressionDiff) {
                    maxAttackCompressionDiff = dbCompressionDiff;
                }
                double dbEffAttenDiff = Math.max(0.5, maxAttackCompressionDiff);
                double x = 0.25 / dbEffAttenDiff;
                envelopeRate = 1.0 - Math.pow(x, 1.0 / attackFrames);
            }
            int loopFrames = Math.min(DIVISION_FRAMES, frames - frameIndex);
            // 구간의 프레임마다 지연선에 쓰고, 지연 전 입력으로 검출하고, 지연된 입력에 게인을 곱한다.
            for (int division = 0; division < loopFrames; division++) {
                double compressorInput = 0.0;
                // 채널마다 지연선에 쓰고 지연 전 입력의 절댓값 최댓값을 구한다.
                for (int j = 0; j < channels; j++) {
                    float undelayed = in[j][frameIndex];
                    preDelay[j][writeIndex] = undelayed;
                    double abs = undelayed > 0 ? undelayed : -undelayed;
                    if (compressorInput < abs) {
                        compressorInput = abs;
                    }
                }
                double shapedInput = saturate(compressorInput, k);
                double attenuation = compressorInput <= 0.0001 ? 1.0 : shapedInput / compressorInput;
                double dbAttenuation = Math.max(2.0, -linearToDecibels(attenuation));
                double dbPerFrame = dbAttenuation / satReleaseFrames;
                double satReleaseRate = decibelsToLinear(dbPerFrame) - 1.0;
                boolean isRelease = attenuation > detectorAverage;
                double rate = isRelease ? satReleaseRate : 1.0;
                detectorAverage += (attenuation - detectorAverage) * rate;
                detectorAverage = Math.min(1.0, detectorAverage);
                detectorAverage = ensureFinite(detectorAverage, 1.0);
                if (envelopeRate < 1.0) {
                    // 어택: 원하는 게인으로 지수적으로 줄인다.
                    compressorGain += (scaledDesiredGain - compressorGain) * envelopeRate;
                } else {
                    // 릴리즈: 1.0 으로 지수적으로 늘린다.
                    compressorGain *= envelopeRate;
                    compressorGain = Math.min(1.0, compressorGain);
                }
                double postWarpCompressorGain = Math.sin(Math.PI / 2.0 * compressorGain);
                double totalGain = linearPostGain * postWarpCompressorGain;
                double dbRealGain = linearToDecibels(postWarpCompressorGain);
                // 측정은 피크를 즉시 따라가고 천천히 회복한다.
                if (dbRealGain < meteringGain) {
                    meteringGain = dbRealGain;
                } else {
                    meteringGain += (dbRealGain - meteringGain) * meteringReleaseK;
                }
                // 지연된 입력에 최종 게인을 곱해 출력한다.
                for (int j = 0; j < channels; j++) {
                    out[j][frameIndex] = (float) (preDelay[j][readIndex] * totalGain);
                }
                frameIndex++;
                readIndex = (readIndex + 1) & mask;
                writeIndex = (writeIndex + 1) & mask;
            }
        }
        reduction = meteringGain;
    }
}
