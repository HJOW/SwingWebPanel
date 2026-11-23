package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The processing of a biquad filter (direct form I, double precision): for every channel it keeps the last two inputs and the last two
 * outputs and applies the coefficients that are given for the block. The coefficients may change from frame to frame (an a-rate parameter
 * that is automated); the caller passes the coefficient arrays of the block and a flag that says whether only the first element is used.
 * <p>
 * 쌍이차 필터의 처리(직접형 I, 배정밀도): 채널마다 마지막 두 입력과 마지막 두 출력을 기억하고 블록에 주어진 계수를 적용한다. 계수는 프레임마다 바뀔 수
 * 있다(자동화되는 a-rate 파라미터). 호출자는 블록의 계수 배열과 첫 요소만 쓰는지 알리는 표시를 넘긴다.
 */
public final class BiquadFilter {

    /**
     * The last input of every channel, <code>x[n-1]</code>.
     * <p>
     * 채널마다 마지막 입력 <code>x[n-1]</code>.
     */
    private double[] x1;

    /**
     * The input before the last one of every channel, <code>x[n-2]</code>.
     * <p>
     * 채널마다 그 앞의 입력 <code>x[n-2]</code>.
     */
    private double[] x2;

    /**
     * The last output of every channel, <code>y[n-1]</code>.
     * <p>
     * 채널마다 마지막 출력 <code>y[n-1]</code>.
     */
    private double[] y1;

    /**
     * The output before the last one of every channel, <code>y[n-2]</code>.
     * <p>
     * 채널마다 그 앞의 출력 <code>y[n-2]</code>.
     */
    private double[] y2;

    /**
     * Creates a filter with a number of channels and a zero state.
     * <p>
     * 채널 수를 정해 상태가 0 인 필터를 만든다.
     *
     * @param channels the number of channels / 채널 수
     */
    public BiquadFilter(int channels) {
        x1 = new double[channels];
        x2 = new double[channels];
        y1 = new double[channels];
        y2 = new double[channels];
    }

    /**
     * Changes the number of channels. The state of the channels that stay is kept and the state of new channels is zero.
     * <p>
     * 채널 수를 바꾼다. 남는 채널의 상태는 유지하고 새 채널의 상태는 0 이다.
     *
     * @param channels the new number of channels / 새 채널 수
     */
    public void setChannels(int channels) {
        if (channels == x1.length) {
            return;
        }
        x1 = java.util.Arrays.copyOf(x1, channels);
        x2 = java.util.Arrays.copyOf(x2, channels);
        y1 = java.util.Arrays.copyOf(y1, channels);
        y2 = java.util.Arrays.copyOf(y2, channels);
    }

    /**
     * Returns the number of channels.
     * <p>
     * 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    public int getChannels() {
        return x1.length;
    }

    /**
     * Clears the state of all channels.
     * <p>
     * 모든 채널의 상태를 지운다.
     */
    public void reset() {
        java.util.Arrays.fill(x1, 0.0);
        java.util.Arrays.fill(x2, 0.0);
        java.util.Arrays.fill(y1, 0.0);
        java.util.Arrays.fill(y2, 0.0);
    }

    /**
     * Filters one channel of a block.
     * <p>
     * 블록의 채널 하나를 필터링한다.
     *
     * @param channel  the channel index / 채널 인덱스
     * @param in       the input samples / 입력 샘플
     * @param out      the output samples (may be the input array) / 출력 샘플(입력 배열이어도 된다)
     * @param frames   the number of frames / 프레임 수
     * @param b0       the coefficients <code>b0</code> of the frames / 프레임별 계수 <code>b0</code>
     * @param b1       the coefficients <code>b1</code> / 계수 <code>b1</code>
     * @param b2       the coefficients <code>b2</code> / 계수 <code>b2</code>
     * @param a1       the coefficients <code>a1</code> / 계수 <code>a1</code>
     * @param a2       the coefficients <code>a2</code> / 계수 <code>a2</code>
     * @param constant true if only the element 0 of the coefficient arrays is used for all frames / 모든 프레임에 계수 배열의 0번 요소만 쓰면 true
     */
    public void process(int channel, float[] in, float[] out, int frames, double[] b0, double[] b1, double[] b2, double[] a1, double[] a2, boolean constant) {
        double px1 = x1[channel];
        double px2 = x2[channel];
        double py1 = y1[channel];
        double py2 = y2[channel];
        // 프레임마다 직접형 I 점화식을 계산한다.
        for (int i = 0; i < frames; i++) {
            int k = constant ? 0 : i;
            double x = in[i];
            double y = b0[k] * x + b1[k] * px1 + b2[k] * px2 - a1[k] * py1 - a2[k] * py2;
            px2 = px1;
            px1 = x;
            py2 = py1;
            py1 = y;
            out[i] = (float) y;
        }
        // 부동소수점 비정규 수가 상태에 남지 않게 아주 작은 값은 0 으로 둔다.
        x1[channel] = flush(px1);
        x2[channel] = flush(px2);
        y1[channel] = flush(py1);
        y2[channel] = flush(py2);
    }

    /**
     * Replaces a denormal number by zero.
     * <p>
     * 비정규 수를 0 으로 바꾼다.
     *
     * @param v the number / 수
     * @return the number, or 0 if it is denormal / 수. 비정규이면 0
     */
    private static double flush(double v) {
        return Math.abs(v) < 1e-300 ? 0.0 : v;
    }
}
