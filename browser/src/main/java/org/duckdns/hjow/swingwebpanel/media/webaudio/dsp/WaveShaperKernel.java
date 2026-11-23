package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The processing of a wave shaper for one channel (Web Audio API 1.31): the shaping curve with linear interpolation and the optional
 * oversampling. With the oversampling factor 1 the curve is applied to the input frames. With the factor 2 or 4 the signal is up-sampled
 * by half-band interpolation stages, shaped at the high rate and down-sampled again by the matching half-band decimation stages, so the
 * harmonics that the curve creates above the original Nyquist frequency are filtered out instead of folding back as aliasing.
 * <p>
 * Filters: the factor 2 uses one interpolator and one decimator of the half length 32 (a delay of 63 frames), the factor 4 adds an inner
 * stage pair of the half length 12 at the doubled rate (a delay of 74.5 frames). The filters are linear phase with about 80 dB of stop
 * band attenuation; the identity processing is flat within 0.01 dB up to 0.46 of the sample rate (about 20 kHz at 44.1 kHz, measured
 * with sine tones, see the tests). The delay is the price of the filters and is reported by {@link #getLatency()}; the specification leaves the filters and
 * their latency to the implementation.
 * <p>
 * 웨이브셰이퍼 한 채널의 처리(Web Audio API 1.31): 선형 보간하는 변환 곡선과 선택 사항인 오버샘플링. 오버샘플링 배수가 1 이면 곡선을 입력 프레임에
 * 바로 적용한다. 배수가 2 나 4 이면 신호를 반대역 보간 단계로 업샘플링하고 높은 주파수에서 곡선을 적용한 뒤 짝이 되는 반대역 데시메이션 단계로 다시
 * 다운샘플링하므로, 곡선이 원래 나이퀴스트 주파수 위에 만든 고조파는 앨리어싱으로 접히지 않고 걸러진다.
 * <p>
 * 필터: 배수 2 는 반 길이 32 의 보간기와 데시메이터 하나씩(지연 63 프레임)을 쓰고, 배수 4 는 두 배 속도에서 반 길이 12 의 안쪽 단계 쌍을 더한다(지연
 * 74.5 프레임). 필터는 선형 위상이며 저지 대역 감쇠가 약 80 dB 이다. 반 길이 32 는 샘플링 주파수의 0.46 배(44.1 kHz 에서 약 20 kHz)까지 항등 처리의 이득이 0.01 dB 안에서 평탄하다(사인으로 잰 값, 시험 참고). 지연은 필터의 대가이고 {@link #getLatency()} 가 알려 준다. 규격은 필터와 그 지연을 구현에 맡긴다.
 */
public final class WaveShaperKernel {

    /**
     * The half length of the outer stages that run between the original rate and the doubled rate.
     * <p>
     * 원래 속도와 두 배 속도 사이에서 동작하는 바깥 단계의 반 길이.
     */
    static final int OUTER_HALF_LENGTH = 32;

    /**
     * The half length of the inner stages that run between the doubled rate and the quadrupled rate.
     * <p>
     * 두 배 속도와 네 배 속도 사이에서 동작하는 안쪽 단계의 반 길이.
     */
    static final int INNER_HALF_LENGTH = 12;

    /**
     * The oversampling factor, 1, 2 or 4.
     * <p>
     * 오버샘플링 배수(1, 2, 4).
     */
    private final int factor;

    /**
     * The interpolator from the original rate (null for the factor 1).
     * <p>
     * 원래 속도에서의 보간기(배수 1 이면 null).
     */
    private final HalfBandInterpolator up1;

    /**
     * The interpolator from the doubled rate to the quadrupled rate (null unless the factor is 4).
     * <p>
     * 두 배 속도에서 네 배 속도로의 보간기(배수가 4 가 아니면 null).
     */
    private final HalfBandInterpolator up2;

    /**
     * The decimator from the quadrupled rate to the doubled rate (null unless the factor is 4).
     * <p>
     * 네 배 속도에서 두 배 속도로의 데시메이터(배수가 4 가 아니면 null).
     */
    private final HalfBandDecimator down2;

    /**
     * The decimator back to the original rate (null for the factor 1).
     * <p>
     * 원래 속도로 돌아오는 데시메이터(배수 1 이면 null).
     */
    private final HalfBandDecimator down1;

    /**
     * The work array at the doubled rate.
     * <p>
     * 두 배 속도의 작업 배열.
     */
    private float[] w2 = new float[0];

    /**
     * The work array at the quadrupled rate.
     * <p>
     * 네 배 속도의 작업 배열.
     */
    private float[] w4 = new float[0];

    /**
     * Creates a kernel.
     * <p>
     * 커널을 만든다.
     *
     * @param factor the oversampling factor: 1, 2 or 4 / 오버샘플링 배수: 1, 2, 4
     * @throws IllegalArgumentException if the factor is not 1, 2 or 4 / 배수가 1, 2, 4 가 아니면
     */
    public WaveShaperKernel(int factor) {
        if (factor != 1 && factor != 2 && factor != 4) {
            throw new IllegalArgumentException("factor " + factor);
        }
        this.factor = factor;
        this.up1 = factor > 1 ? new HalfBandInterpolator(OUTER_HALF_LENGTH) : null;
        this.down1 = factor > 1 ? new HalfBandDecimator(OUTER_HALF_LENGTH) : null;
        this.up2 = factor == 4 ? new HalfBandInterpolator(INNER_HALF_LENGTH) : null;
        this.down2 = factor == 4 ? new HalfBandDecimator(INNER_HALF_LENGTH) : null;
    }

    /**
     * Returns the oversampling factor.
     * <p>
     * 오버샘플링 배수를 반환한다.
     *
     * @return 1, 2 or 4 / 1, 2, 4
     */
    public int getFactor() {
        return factor;
    }

    /**
     * Returns the delay of the filters in frames of the original rate: 0 for the factor 1, 63 for 2 and 74.5 for 4.
     * <p>
     * 필터의 지연을 원래 속도의 프레임 단위로 반환한다. 배수 1 은 0, 2 는 63, 4 는 74.5.
     *
     * @return the latency in frames / 지연(프레임)
     */
    public double getLatency() {
        return latencyOf(factor);
    }

    /**
     * Returns the delay of the filters of a factor in frames of the original rate.
     * <p>
     * 배수의 필터 지연을 원래 속도의 프레임 단위로 반환한다.
     *
     * @param factor the oversampling factor: 1, 2 or 4 / 오버샘플링 배수: 1, 2, 4
     * @return the latency in frames / 지연(프레임)
     */
    public static double latencyOf(int factor) {
        if (factor == 1) {
            return 0.0;
        }
        double d = OUTER_HALF_LENGTH + (OUTER_HALF_LENGTH - 1);
        if (factor == 4) {
            // 안쪽 단계의 지연은 두 배 속도의 프레임 단위이므로 절반으로 환산한다.
            d += (INNER_HALF_LENGTH + (INNER_HALF_LENGTH - 1)) / 2.0;
        }
        return d;
    }

    /**
     * Returns a number of frames after which the output of a silent input is silent again (twice the latency plus a margin): the
     * tail time of the node. 0 for the factor 1.
     * <p>
     * 입력이 무음이 된 뒤 출력이 다시 무음이 되기까지의 프레임 수(지연의 두 배에 여유를 더한 값): 노드의 꼬리 시간. 배수 1 이면 0.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    public int getTailFrames() {
        return tailFramesOf(factor);
    }

    /**
     * Returns the tail of a factor (see {@link #getTailFrames()}).
     * <p>
     * 배수의 꼬리를 반환한다({@link #getTailFrames()} 참고).
     *
     * @param factor the oversampling factor: 1, 2 or 4 / 오버샘플링 배수: 1, 2, 4
     * @return the tail in frames / 꼬리(프레임)
     */
    public static int tailFramesOf(int factor) {
        if (factor == 1) {
            return 0;
        }
        return (int) Math.ceil(latencyOf(factor) * 2.0) + 8;
    }

    /**
     * Clears the memory of the filters.
     * <p>
     * 필터의 기억을 지운다.
     */
    public void reset() {
        if (up1 != null) {
            up1.reset();
            down1.reset();
        }
        if (up2 != null) {
            up2.reset();
            down2.reset();
        }
    }

    /**
     * Shapes a block. The input and the output may be the same array when the factor is 1.
     * <p>
     * 블록을 변환한다. 배수가 1 이면 입력과 출력이 같은 배열이어도 된다.
     *
     * @param in     the input / 입력
     * @param out    the output / 출력
     * @param frames the number of frames / 프레임 수
     * @param curve  the shaping curve with at least 2 values / 값이 둘 이상인 변환 곡선
     */
    public void process(float[] in, float[] out, int frames, float[] curve) {
        if (factor == 1) {
            // 오버샘플링이 없으면 입력 프레임에 곡선을 바로 적용한다.
            for (int i = 0; i < frames; i++) {
                out[i] = (float) shape(in[i], curve);
            }
            return;
        }
        if (w2.length < 2 * frames) {
            w2 = new float[2 * frames];
        }
        up1.process(in, frames, w2);
        if (factor == 2) {
            // 두 배 속도에서 곡선을 적용하고 원래 속도로 줄인다.
            for (int i = 0; i < 2 * frames; i++) {
                w2[i] = (float) shape(w2[i], curve);
            }
            down1.process(w2, frames, out);
            return;
        }
        if (w4.length < 4 * frames) {
            w4 = new float[4 * frames];
        }
        up2.process(w2, 2 * frames, w4);
        // 네 배 속도에서 곡선을 적용한다.
        for (int i = 0; i < 4 * frames; i++) {
            w4[i] = (float) shape(w4[i], curve);
        }
        down2.process(w4, 2 * frames, w2);
        down1.process(w2, frames, out);
    }

    /**
     * Applies the shaping curve to one value with the linear interpolation of the specification: with <code>N</code> curve values,
     * <code>v = (N - 1) / 2 * (x + 1)</code>, the result is the first value for <code>v &lt; 0</code>, the last value for
     * <code>v &gt;= N - 1</code> and otherwise the interpolation of the values at <code>floor(v)</code> and <code>floor(v) + 1</code>.
     * A NaN input is treated as 0.
     * <p>
     * 규격의 선형 보간으로 변환 곡선을 값 하나에 적용한다. 곡선 값이 <code>N</code> 개이면 <code>v = (N - 1) / 2 * (x + 1)</code> 이고, 결과는
     * <code>v &lt; 0</code> 이면 첫 값, <code>v &gt;= N - 1</code> 이면 마지막 값이며 그 밖에는 <code>floor(v)</code> 와 <code>floor(v) + 1</code> 의 값을
     * 보간한 것이다. NaN 입력은 0 으로 본다.
     *
     * @param x     the input sample / 입력 샘플
     * @param curve the curve / 곡선
     * @return the output sample / 출력 샘플
     */
    public static double shape(double x, float[] curve) {
        int n = curve.length;
        double input = x != x ? 0.0 : x;
        double v = (n - 1) * 0.5 * (input + 1.0);
        if (v < 0) {
            return curve[0];
        }
        if (v >= n - 1) {
            return curve[n - 1];
        }
        int k = (int) v;
        double f = v - k;
        return (1.0 - f) * curve[k] + f * curve[k + 1];
    }
}
