package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * A streaming decimator that halves the sample rate of one channel with the half-band low-pass filter that belongs to
 * {@link HalfBandInterpolator} of the same half length <code>P</code>: the filter has the weight <code>1/2</code> at the centre and
 * <code>g[m] / 2</code> at the odd distances <code>2m + 1</code>, and only every second output of the filter is computed. The output frame
 * <code>t</code> is the filtered value at the input frame <code>2 (t - P + 1)</code>, so the delay is <code>P - 1</code> output frames.
 * Chained after an interpolator of the same half length an identity processing returns the input delayed by <code>2P - 1</code> frames.
 * <p>
 * {@link HalfBandInterpolator} 와 짝인 같은 반 길이 <code>P</code> 의 반대역 저역 통과 필터로 한 채널의 샘플링 주파수를 절반으로 줄이는 스트리밍
 * 데시메이터. 필터는 중심의 가중치가 <code>1/2</code> 이고 홀수 거리 <code>2m + 1</code> 에서 <code>g[m] / 2</code> 이며, 필터 출력 중 하나 건너 하나만
 * 계산한다. 출력 프레임 <code>t</code> 는 입력 프레임 <code>2 (t - P + 1)</code> 에서의 필터링한 값이므로 지연은 출력 <code>P - 1</code> 프레임이다.
 * 같은 반 길이의 보간기 뒤에 이으면 항등 처리가 입력을 <code>2P - 1</code> 프레임 지연해서 돌려준다.
 */
final class HalfBandDecimator {

    /**
     * The half length.
     * <p>
     * 반 길이.
     */
    private final int p;

    /**
     * The odd taps of the half-band filter, <code>g[m] / 2</code>.
     * <p>
     * 반대역 필터의 홀수 탭 <code>g[m] / 2</code>.
     */
    private final double[] h;

    /**
     * The work array: the <code>4P - 3</code> frames of history followed by the current block.
     * <p>
     * 작업 배열: <code>4P - 3</code> 프레임의 이력과 그 뒤에 이어지는 현재 블록.
     */
    private float[] buf;

    /**
     * Creates a decimator with an empty history.
     * <p>
     * 이력이 빈 데시메이터를 만든다.
     *
     * @param p the half length / 반 길이
     */
    HalfBandDecimator(int p) {
        this.p = p;
        double[] g = HalfBand.design(p);
        this.h = new double[p];
        // 보간기의 탭을 절반으로 줄여 저역 통과 필터의 홀수 탭으로 쓴다.
        for (int m = 0; m < p; m++) {
            h[m] = g[m] * 0.5;
        }
        this.buf = new float[4 * p - 3 + 256];
    }

    /**
     * Returns the delay in output frames.
     * <p>
     * 출력 프레임 단위의 지연을 반환한다.
     *
     * @return the delay / 지연
     */
    int getDelay() {
        return p - 1;
    }

    /**
     * Clears the history.
     * <p>
     * 이력을 지운다.
     */
    void reset() {
        java.util.Arrays.fill(buf, 0f);
    }

    /**
     * Halves the sample rate of a block.
     * <p>
     * 블록의 샘플링 주파수를 절반으로 줄인다.
     *
     * @param in     the input with <code>2 * frames</code> frames / <code>2 * frames</code> 프레임의 입력
     * @param frames the number of output frames / 출력 프레임 수
     * @param out    the output / 출력
     */
    void process(float[] in, int frames, float[] out) {
        int hist = 4 * p - 3;
        if (buf.length < hist + 2 * frames) {
            float[] bigger = new float[hist + 2 * frames];
            System.arraycopy(buf, 0, bigger, 0, hist);
            buf = bigger;
        }
        System.arraycopy(in, 0, buf, hist, 2 * frames);
        // 출력 프레임마다 짝수 위치의 중심과 홀수 거리의 이웃을 가중해 더한다.
        for (int t = 0; t < frames; t++) {
            int c = hist + 2 * t - 2 * p + 2;
            double s = 0.5 * buf[c];
            // 중심에서 홀수 거리만큼 떨어진 양쪽 샘플의 합에 가중치를 곱해 더한다.
            for (int m = 0; m < p; m++) {
                s += h[m] * (buf[c - 2 * m - 1] + buf[c + 2 * m + 1]);
            }
            out[t] = (float) s;
        }
        System.arraycopy(buf, 2 * frames, buf, 0, hist);
    }
}
