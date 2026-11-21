package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * A general IIR filter of the Web Audio API (1.21): the transfer function <code>H(z) = sum b[m] z^-m / sum a[n] z^-n</code> with at most
 * 20 coefficients in each array. The coefficients are divided by <code>a[0]</code> once; the processing is the direct form I in double
 * precision with a circular history of the inputs and the outputs of every channel. The coefficients cannot change after the filter is
 * created.
 * <p>
 * Web Audio API 의 일반 IIR 필터(1.21): 전달 함수 <code>H(z) = sum b[m] z^-m / sum a[n] z^-n</code> 이며 각 배열의 계수는 최대 20개이다. 계수는
 * <code>a[0]</code> 로 한 번 나누고, 처리는 채널마다 입력과 출력의 순환 이력을 가진 배정밀도 직접형 I 이다. 필터를 만든 뒤에는 계수를 바꿀 수 없다.
 */
public final class IirFilter {

    /**
     * The size of the circular histories, a power of two above the largest number of coefficients.
     * <p>
     * 순환 이력의 크기. 계수 수의 최댓값보다 큰 2의 거듭제곱.
     */
    private static final int HISTORY = 32;

    /**
     * The feedforward coefficients divided by <code>a[0]</code>.
     * <p>
     * <code>a[0]</code> 로 나눈 피드포워드 계수.
     */
    private final double[] b;

    /**
     * The feedback coefficients divided by <code>a[0]</code>; <code>a[0]</code> itself is 1.
     * <p>
     * <code>a[0]</code> 로 나눈 피드백 계수. <code>a[0]</code> 자체는 1 이다.
     */
    private final double[] a;

    /**
     * The history of the inputs of every channel.
     * <p>
     * 채널마다의 입력 이력.
     */
    private double[][] xHist;

    /**
     * The history of the outputs of every channel.
     * <p>
     * 채널마다의 출력 이력.
     */
    private double[][] yHist;

    /**
     * The write position of the circular histories of every channel.
     * <p>
     * 채널마다의 순환 이력의 쓰기 위치.
     */
    private int[] pos;

    /**
     * Creates a filter.
     * <p>
     * 필터를 만든다.
     *
     * @param feedforward the numerator coefficients (1 to 20, not all 0) / 분자 계수(1~20개, 모두 0 은 아님)
     * @param feedback    the denominator coefficients (1 to 20, the first one not 0) / 분모 계수(1~20개, 첫 계수는 0 이 아님)
     * @param channels    the initial number of channels / 처음 채널 수
     */
    public IirFilter(double[] feedforward, double[] feedback, int channels) {
        double a0 = feedback[0];
        b = new double[feedforward.length];
        a = new double[feedback.length];
        // 모든 계수를 a[0] 로 나눈다.
        for (int i = 0; i < b.length; i++) {
            b[i] = feedforward[i] / a0;
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = feedback[i] / a0;
        }
        setChannels(channels, true);
    }

    /**
     * Allocates the histories for a number of channels.
     * <p>
     * 채널 수에 맞게 이력을 할당한다.
     *
     * @param channels the number of channels / 채널 수
     * @param fresh    true to clear the histories that stay / 남는 이력도 지우려면 true
     */
    private void setChannels(int channels, boolean fresh) {
        double[][] nx = new double[channels][HISTORY];
        double[][] ny = new double[channels][HISTORY];
        int[] np = new int[channels];
        if (!fresh && xHist != null) {
            int keep = Math.min(channels, xHist.length);
            // 남는 채널의 이력은 그대로 옮긴다.
            for (int c = 0; c < keep; c++) {
                nx[c] = xHist[c];
                ny[c] = yHist[c];
                np[c] = pos[c];
            }
        }
        xHist = nx;
        yHist = ny;
        pos = np;
    }

    /**
     * Changes the number of channels. The state of the channels that stay is kept and the state of new channels is zero.
     * <p>
     * 채널 수를 바꾼다. 남는 채널의 상태는 유지하고 새 채널의 상태는 0 이다.
     *
     * @param channels the new number of channels / 새 채널 수
     */
    public void setChannels(int channels) {
        if (channels != xHist.length) {
            setChannels(channels, false);
        }
    }

    /**
     * Clears the state of all channels.
     * <p>
     * 모든 채널의 상태를 지운다.
     */
    public void reset() {
        setChannels(xHist.length, true);
    }

    /**
     * Filters one channel of a block.
     * <p>
     * 블록의 채널 하나를 필터링한다.
     *
     * @param channel the channel index / 채널 인덱스
     * @param in      the input samples / 입력 샘플
     * @param out     the output samples (may be the input array) / 출력 샘플(입력 배열이어도 된다)
     * @param frames  the number of frames / 프레임 수
     */
    public void process(int channel, float[] in, float[] out, int frames) {
        double[] xh = xHist[channel];
        double[] yh = yHist[channel];
        int p = pos[channel];
        int mask = HISTORY - 1;
        // 프레임마다 입력 이력에서 b 항을, 출력 이력에서 a 항을 계산한다.
        for (int i = 0; i < frames; i++) {
            double x = in[i];
            xh[p & mask] = x;
            double acc = 0.0;
            for (int m = 0; m < b.length; m++) {
                acc += b[m] * xh[(p - m) & mask];
            }
            for (int n = 1; n < a.length; n++) {
                acc -= a[n] * yh[(p - n) & mask];
            }
            if (Math.abs(acc) < 1e-300) {
                acc = 0.0;
            }
            yh[p & mask] = acc;
            out[i] = (float) acc;
            p++;
        }
        pos[channel] = p & (Integer.MAX_VALUE >> 1);
    }

    /**
     * Computes the frequency response at frequencies in Hz. A frequency outside <code>[0, sampleRate/2]</code> gives NaN.
     * <p>
     * Hz 단위 주파수에서 주파수 응답을 계산한다. <code>[0, sampleRate/2]</code> 밖의 주파수는 NaN 을 준다.
     *
     * @param frequencyHz the frequencies / 주파수
     * @param magnitude   receives the linear magnitude / 선형 크기를 받는다
     * @param phase       receives the phase in radians / 위상(라디안)을 받는다
     * @param sampleRate  the sample rate in Hz / 샘플링 주파수(Hz)
     */
    public void frequencyResponse(float[] frequencyHz, float[] magnitude, float[] phase, double sampleRate) {
        int n = Math.min(frequencyHz.length, Math.min(magnitude.length, phase.length));
        // 주파수마다 분자와 분모의 다항식을 단위원 위에서 계산한다.
        for (int k = 0; k < n; k++) {
            double f = frequencyHz[k];
            if (!(f >= 0 && f <= sampleRate / 2)) {
                magnitude[k] = Float.NaN;
                phase[k] = Float.NaN;
                continue;
            }
            double omega = 2 * Math.PI * f / sampleRate;
            double numR = 0;
            double numI = 0;
            for (int m = 0; m < b.length; m++) {
                numR += b[m] * Math.cos(omega * m);
                numI -= b[m] * Math.sin(omega * m);
            }
            double denR = 0;
            double denI = 0;
            for (int j = 0; j < a.length; j++) {
                denR += a[j] * Math.cos(omega * j);
                denI -= a[j] * Math.sin(omega * j);
            }
            double scale = denR * denR + denI * denI;
            double re = (numR * denR + numI * denI) / scale;
            double im = (numI * denR - numR * denI) / scale;
            magnitude[k] = (float) Math.sqrt(re * re + im * im);
            phase[k] = (float) Math.atan2(im, re);
        }
    }

    /**
     * Estimates the tail time of the filter in frames by running its impulse response until it stays below the threshold of 16 bit audio
     * for a while, but not longer than the given maximum (a filter that does not decay has the maximum).
     * <p>
     * 필터의 임펄스 응답을 16비트 오디오의 문턱 아래로 한동안 머물 때까지 돌려 꼬리 시간(프레임)을 추정한다. 단 주어진 최댓값을 넘지 않는다(감쇠하지
     * 않는 필터는 최댓값을 가진다).
     *
     * @param maxFrames the largest value to return / 반환할 최댓값
     * @return the tail time in frames / 꼬리 시간(프레임)
     */
    public long estimateTailFrames(long maxFrames) {
        double[] xh = new double[HISTORY];
        double[] yh = new double[HISTORY];
        int mask = HISTORY - 1;
        double threshold = 1.0 / 32768.0;
        long last = 0;
        long quiet = 0;
        // 임펄스 응답을 계산하며 문턱을 넘은 마지막 위치를 기록한다. 문턱 아래가 충분히 이어지면 멈춘다.
        for (long i = 0; i < maxFrames; i++) {
            int p = (int) (i & 0x7fffffff);
            xh[p & mask] = i == 0 ? 1.0 : 0.0;
            double acc = 0.0;
            for (int m = 0; m < b.length; m++) {
                acc += b[m] * xh[(p - m) & mask];
            }
            for (int n = 1; n < a.length; n++) {
                acc -= a[n] * yh[(p - n) & mask];
            }
            if (Double.isNaN(acc) || Double.isInfinite(acc)) {
                return maxFrames;
            }
            yh[p & mask] = acc;
            if (Math.abs(acc) > threshold) {
                last = i + 1;
                quiet = 0;
            } else if (++quiet > 4096) {
                break;
            }
        }
        return last >= maxFrames ? maxFrames : last + 1;
    }
}
