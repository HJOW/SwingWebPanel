package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * A streaming interpolator that doubles the sample rate of one channel. The even output frames repeat the input delayed by <code>P</code>
 * frames and the odd output frames interpolate half way between two input frames with the half-band taps of {@link HalfBand}, so the
 * output frame <code>2t</code> is the input <code>x[t - P]</code> and the output frame <code>2t + 1</code> is the band-limited value at
 * <code>t - P + 0.5</code>. The delay is <code>P</code> input frames (<code>2P</code> output frames). The object keeps the history
 * between blocks, so a signal can be fed in blocks of any size.
 * <p>
 * 한 채널의 샘플링 주파수를 두 배로 만드는 스트리밍 보간기. 짝수 출력 프레임은 입력을 <code>P</code> 프레임 지연해서 되풀이하고 홀수 출력 프레임은
 * {@link HalfBand} 의 반대역 탭으로 두 입력 프레임의 중간 값을 보간한다. 그래서 출력 프레임 <code>2t</code> 는 입력 <code>x[t - P]</code> 이고 출력
 * 프레임 <code>2t + 1</code> 은 <code>t - P + 0.5</code> 에서의 대역 제한 값이다. 지연은 입력 <code>P</code> 프레임(출력 <code>2P</code> 프레임)이다.
 * 객체가 블록 사이의 이력을 가지므로 신호를 어떤 크기의 블록으로든 넣을 수 있다.
 */
final class HalfBandInterpolator {

    /**
     * The half length: the number of taps on each side of the interpolated point.
     * <p>
     * 반 길이: 보간하는 점 양쪽의 탭 수.
     */
    private final int p;

    /**
     * The taps <code>g[0 .. P-1]</code>.
     * <p>
     * 반 샘플 보간기의 탭 값들 <code>g[0 .. P-1]</code>.
     */
    private final double[] g;

    /**
     * The work array: the <code>2P - 1</code> frames of history followed by the current block.
     * <p>
     * 작업 배열: <code>2P - 1</code> 프레임의 이력과 그 뒤에 이어지는 현재 블록.
     */
    private float[] buf;

    /**
     * Creates an interpolator with an empty history.
     * <p>
     * 이력이 빈 보간기를 만든다.
     *
     * @param p the half length (the delay in input frames) / 반 길이(입력 프레임 단위 지연)
     */
    HalfBandInterpolator(int p) {
        this.p = p;
        this.g = HalfBand.design(p);
        this.buf = new float[2 * p - 1 + 128];
    }

    /**
     * Returns the delay in input frames.
     * <p>
     * 입력 프레임 단위의 지연을 반환한다.
     *
     * @return the delay / 지연
     */
    int getDelay() {
        return p;
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
     * Doubles the sample rate of a block.
     * <p>
     * 블록의 샘플링 주파수를 두 배로 만든다.
     *
     * @param in     the input / 입력
     * @param frames the number of input frames / 입력 프레임 수
     * @param out    the output with room for <code>2 * frames</code> frames / <code>2 * frames</code> 프레임이 들어갈 출력
     */
    void process(float[] in, int frames, float[] out) {
        int h = 2 * p - 1;
        if (buf.length < h + frames) {
            float[] bigger = new float[h + frames];
            System.arraycopy(buf, 0, bigger, 0, h);
            buf = bigger;
        }
        System.arraycopy(in, 0, buf, h, frames);
        // 프레임마다 짝수 출력은 지연한 입력을, 홀수 출력은 양쪽 이웃의 가중 합을 쓴다.
        for (int t = 0; t < frames; t++) {
            int c = h + t - p;
            out[2 * t] = buf[c];
            double s = 0.0;
            // 중심에서 반 샘플씩 멀어지는 탭마다 양쪽 샘플의 합에 가중치를 곱해 더한다.
            for (int m = 0; m < p; m++) {
                s += g[m] * (buf[c - m] + buf[c + 1 + m]);
            }
            out[2 * t + 1] = (float) s;
        }
        System.arraycopy(buf, frames, buf, 0, h);
    }
}
