package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.Fft;

/**
 * Real-time frequency and time-domain analysis (Web Audio API 1.8). The audio passes unchanged from the input to the output (the output
 * may stay unconnected, and the node is processed as long as a started source feeds it). The node keeps the last 32768 frames of the
 * input, down-mixed to mono with the rules of the <code>speakers</code> interpretation whatever the settings of the node are, and the
 * reading methods work on the most recent <code>fftSize</code> frames of it.
 * <p>
 * Frequency data (<code>getFloatFrequencyData</code>, <code>getByteFrequencyData</code>): the time-domain data is windowed with the
 * Blackman window <code>w[n] = a0 - a1 cos(2 pi n / N) + a2 cos(4 pi n / N)</code> (<code>alpha = 0.16</code>,
 * <code>a0 = (1 - alpha) / 2</code>, <code>a1 = 1 / 2</code>, <code>a2 = alpha / 2</code>), transformed with the discrete Fourier
 * transform and scaled by <code>1 / N</code>; the magnitudes <code>|X[k]|</code> are smoothed over time,
 * <code>X'[k] = tau X'_prev[k] + (1 - tau) |X[k]|</code> with <code>tau = smoothingTimeConstant</code>, and converted to decibels,
 * <code>Y[k] = 20 log10 X'[k]</code> (minus infinity for a magnitude of 0). The bytes are <code>floor(255 (Y - min) / (max - min))</code>
 * clipped to 0..255 and, for the time domain, <code>floor(128 (1 + x))</code> clipped to 0..255; a byte is an unsigned value (use
 * <code>b &amp; 0xFF</code>). The frequency data is computed again only when the render time has advanced since the last call, so
 * several calls within one render quantum return the same data and smooth only once.
 * <p>
 * A reading method may be called from any thread while the context renders: the rendering thread writes the history in short critical
 * sections and the readers copy what they need.
 * <p>
 * 실시간 주파수와 시간 영역 분석(Web Audio API 1.8). 오디오는 입력에서 출력으로 그대로 지나간다(출력은 연결하지 않아도 되고, 시작한 원천이 노드를 먹이는 동안
 * 처리된다). 노드는 입력의 마지막 32768 프레임을 보관하는데 노드의 설정과 무관하게 <code>speakers</code> 해석의 규칙으로 모노로 다운믹스한 것이며, 읽기
 * 메서드는 그중 가장 최근 <code>fftSize</code> 프레임을 쓴다.
 * <p>
 * 주파수 데이터(<code>getFloatFrequencyData</code>, <code>getByteFrequencyData</code>): 시간 영역 데이터에 Blackman 창
 * <code>w[n] = a0 - a1 cos(2 pi n / N) + a2 cos(4 pi n / N)</code>(<code>alpha = 0.16</code>, <code>a0 = (1 - alpha) / 2</code>, <code>a1 = 1 / 2</code>,
 * <code>a2 = alpha / 2</code>)을 곱하고 이산 푸리에 변환한 뒤 <code>1 / N</code> 로 크기를 맞춘다. 크기 <code>|X[k]|</code> 를 시간에 따라
 * 평활(<code>X'[k] = tau X'_prev[k] + (1 - tau) |X[k]|</code>, <code>tau = smoothingTimeConstant</code>)하고 데시벨로 바꾼다(<code>Y[k] = 20 log10 X'[k]</code>,
 * 크기가 0 이면 음의 무한대). 바이트는 <code>floor(255 (Y - min) / (max - min))</code> 을 0~255 로 자른 값이고 시간 영역은 <code>floor(128 (1 + x))</code> 을
 * 0~255 로 자른 값이다. 바이트는 부호 없는 값이다(<code>b &amp; 0xFF</code> 를 쓴다). 주파수 데이터는 마지막 호출 뒤 렌더링 시간이 흘렀을 때만 다시 계산하므로
 * 한 render quantum 안의 여러 호출은 같은 데이터를 돌려주고 평활도 한 번만 한다.
 * <p>
 * 읽기 메서드는 컨텍스트가 렌더링하는 동안 어느 스레드에서든 부를 수 있다. 렌더링 스레드는 짧은 임계 구역에서 이력을 쓰고 읽는 쪽은 필요한 만큼 복사한다.
 */
public final class AnalyserNode extends AudioNode {

    /**
     * The smallest <code>fftSize</code>.
     * <p>
     * <code>fftSize</code> 의 최솟값.
     */
    public static final int MIN_FFT_SIZE = 32;

    /**
     * The largest <code>fftSize</code>, which is also the number of frames of history that the node keeps.
     * <p>
     * <code>fftSize</code> 의 최댓값. 노드가 보관하는 이력의 프레임 수이기도 하다.
     */
    public static final int MAX_FFT_SIZE = 32768;

    /**
     * The coefficient alpha of the Blackman window.
     * <p>
     * Blackman 창의 계수 alpha.
     */
    private static final double BLACKMAN_ALPHA = 0.16;

    /**
     * The history of the down-mixed input: twice the largest FFT size, as a circular buffer (guarded by {@link #lock}).
     * <p>
     * 다운믹스한 입력의 이력: 최대 FFT 크기의 두 배인 원형 버퍼({@link #lock} 으로 보호).
     */
    private final float[] history = new float[MAX_FFT_SIZE * 2];

    /**
     * The index at which the next frame is written (guarded by {@link #lock}).
     * <p>
     * 다음 프레임을 쓸 인덱스({@link #lock} 으로 보호).
     */
    private int writeIndex;

    /**
     * The lock of the history.
     * <p>
     * 이력의 잠금.
     */
    private final Object lock = new Object();

    /**
     * The mono bus into which the input is down-mixed (rendering thread).
     * <p>
     * 입력을 다운믹스하는 모노 버스(렌더링 스레드).
     */
    private final AudioBus monoBus;

    /**
     * The size of the FFT (guarded by {@link #lock}).
     * <p>
     * FFT 크기({@link #lock} 으로 보호).
     */
    private int fftSize = 2048;

    /**
     * The minimum of the range of the byte conversion in decibels (guarded by {@link #lock}).
     * <p>
     * 바이트 변환 범위의 최소(dB)({@link #lock} 으로 보호).
     */
    private double minDecibels = -100.0;

    /**
     * The maximum of the range of the byte conversion in decibels (guarded by {@link #lock}).
     * <p>
     * 바이트 변환 범위의 최대(dB)({@link #lock} 으로 보호).
     */
    private double maxDecibels = -30.0;

    /**
     * The smoothing constant (guarded by {@link #lock}).
     * <p>
     * 평활 상수({@link #lock} 으로 보호).
     */
    private double smoothingTimeConstant = 0.8;

    /**
     * The smoothed magnitudes of the last analysis, <code>fftSize / 2</code> of them (guarded by {@link #lock}).
     * <p>
     * 마지막 분석의 평활한 크기. <code>fftSize / 2</code> 개({@link #lock} 으로 보호).
     */
    private double[] magnitudes = new double[1024];

    /**
     * The render time (the frame index of the context) of the last analysis, or -1 (guarded by {@link #lock}).
     * <p>
     * 마지막 분석의 렌더링 시간(컨텍스트의 프레임 인덱스). 없으면 -1({@link #lock} 으로 보호).
     */
    private long lastAnalysisFrame = -1;

    /**
     * The FFT object of the current size (guarded by {@link #lock}).
     * <p>
     * 현재 크기의 FFT 객체({@link #lock} 으로 보호).
     */
    private Fft fft = new Fft(2048);

    /**
     * The Blackman window of the current size (guarded by {@link #lock}).
     * <p>
     * 현재 크기의 Blackman 창({@link #lock} 으로 보호).
     */
    private double[] window = blackman(2048);

    /**
     * Creates an analyser with the default settings: <code>fftSize</code> 2048, <code>minDecibels</code> -100, <code>maxDecibels</code>
     * -30, <code>smoothingTimeConstant</code> 0.8.
     * <p>
     * 기본 설정의 분석기를 만든다: <code>fftSize</code> 2048, <code>minDecibels</code> -100, <code>maxDecibels</code> -30,
     * <code>smoothingTimeConstant</code> 0.8.
     *
     * @param context the context / 컨텍스트
     */
    public AnalyserNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.MAX, ChannelInterpretation.SPEAKERS);
        monoBus = new AudioBus(1, context.getRenderQuantumSize());
    }

    /**
     * Computes the Blackman window of a size.
     * <p>
     * 크기에 맞는 Blackman 창을 계산한다.
     *
     * @param n the size / 크기
     * @return the window / 창
     */
    private static double[] blackman(int n) {
        double a0 = 0.5 * (1 - BLACKMAN_ALPHA);
        double a1 = 0.5;
        double a2 = 0.5 * BLACKMAN_ALPHA;
        double[] w = new double[n];
        // 창의 각 점을 식대로 계산한다.
        for (int i = 0; i < n; i++) {
            w[i] = a0 - a1 * Math.cos(2.0 * Math.PI * i / n) + a2 * Math.cos(4.0 * Math.PI * i / n);
        }
        return w;
    }

    /**
     * Returns the size of the FFT.
     * <p>
     * FFT 크기를 반환한다.
     *
     * @return the size / 크기
     */
    public int getFftSize() {
        synchronized (lock) {
            return fftSize;
        }
    }

    /**
     * Sets the size of the FFT, a power of two from 32 to 32768. A change resets the smoothing of the frequency data.
     * <p>
     * FFT 크기를 설정한다. 32 ~ 32768 의 2 의 거듭제곱이다. 바뀌면 주파수 데이터의 평활이 초기화된다.
     *
     * @param size the size / 크기
     * @throws AudioException <code>IndexSizeError</code> if the size is not a power of two in the range / 크기가 범위의 2 의 거듭제곱이 아니면
     *                        <code>IndexSizeError</code>
     */
    public void setFftSize(int size) {
        if (size < MIN_FFT_SIZE || size > MAX_FFT_SIZE || (size & (size - 1)) != 0) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "fftSize " + size + " is not a power of two from 32 to 32768");
        }
        synchronized (lock) {
            if (size != fftSize) {
                fftSize = size;
                fft = new Fft(size);
                window = blackman(size);
                magnitudes = new double[size / 2];
                lastAnalysisFrame = -1;
            }
        }
    }

    /**
     * Returns the number of frequency bins, half the FFT size.
     * <p>
     * 주파수 빈의 수(FFT 크기의 절반)를 반환한다.
     *
     * @return the number of bins / 빈 수
     */
    public int getFrequencyBinCount() {
        return getFftSize() / 2;
    }

    /**
     * Returns the minimum of the byte conversion range in decibels.
     * <p>
     * 바이트 변환 범위의 최소(dB)를 반환한다.
     *
     * @return the minimum / 최솟값
     */
    public double getMinDecibels() {
        synchronized (lock) {
            return minDecibels;
        }
    }

    /**
     * Sets the minimum of the byte conversion range in decibels.
     * <p>
     * 바이트 변환 범위의 최소(dB)를 설정한다.
     *
     * @param value the minimum, less than <code>maxDecibels</code> / 최솟값. <code>maxDecibels</code> 보다 작아야 한다
     * @throws AudioException <code>IndexSizeError</code> if the value is not less than the maximum / 값이 최댓값보다 작지 않으면
     *                        <code>IndexSizeError</code>
     */
    public void setMinDecibels(double value) {
        synchronized (lock) {
            if (!(value < maxDecibels)) {
                throw new AudioException(AudioException.Kind.INDEX_SIZE, "minDecibels must be less than maxDecibels");
            }
            minDecibels = value;
        }
    }

    /**
     * Returns the maximum of the byte conversion range in decibels.
     * <p>
     * 바이트 변환 범위의 최대(dB)를 반환한다.
     *
     * @return the maximum / 최댓값
     */
    public double getMaxDecibels() {
        synchronized (lock) {
            return maxDecibels;
        }
    }

    /**
     * Sets the maximum of the byte conversion range in decibels.
     * <p>
     * 바이트 변환 범위의 최대(dB)를 설정한다.
     *
     * @param value the maximum, greater than <code>minDecibels</code> / 최댓값. <code>minDecibels</code> 보다 커야 한다
     * @throws AudioException <code>IndexSizeError</code> if the value is not greater than the minimum / 값이 최솟값보다 크지 않으면
     *                        <code>IndexSizeError</code>
     */
    public void setMaxDecibels(double value) {
        synchronized (lock) {
            if (!(value > minDecibels)) {
                throw new AudioException(AudioException.Kind.INDEX_SIZE, "maxDecibels must be greater than minDecibels");
            }
            maxDecibels = value;
        }
    }

    /**
     * Returns the smoothing constant.
     * <p>
     * 평활 상수를 반환한다.
     *
     * @return the constant / 상수
     */
    public double getSmoothingTimeConstant() {
        synchronized (lock) {
            return smoothingTimeConstant;
        }
    }

    /**
     * Sets the smoothing constant: 0 means no averaging with the last analysis.
     * <p>
     * 평활 상수를 설정한다. 0 은 마지막 분석과 평균하지 않는다는 뜻이다.
     *
     * @param value the constant from 0 to 1 / 0 ~ 1 의 상수
     * @throws AudioException <code>IndexSizeError</code> if the value is outside 0..1 / 값이 0~1 밖이면 <code>IndexSizeError</code>
     */
    public void setSmoothingTimeConstant(double value) {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "smoothingTimeConstant " + value + " is outside 0..1");
        }
        synchronized (lock) {
            smoothingTimeConstant = value;
        }
    }

    /**
     * Copies the most recent <code>fftSize</code> frames of the history, oldest first, into an array (guarded by {@link #lock}).
     * <p>
     * 이력의 가장 최근 <code>fftSize</code> 프레임을 오래된 것부터 배열에 복사한다({@link #lock} 으로 보호).
     *
     * @param dest the destination with at least <code>fftSize</code> elements / <code>fftSize</code> 요소 이상의 대상
     */
    private void copyRecent(double[] dest) {
        int mask = history.length - 1;
        int start = writeIndex - fftSize;
        // 가장 오래된 프레임부터 순서대로 복사한다.
        for (int i = 0; i < fftSize; i++) {
            dest[i] = history[(start + i) & mask];
        }
    }

    /**
     * Updates the smoothed magnitudes if the render time has advanced since the last analysis (guarded by {@link #lock}).
     * <p>
     * 마지막 분석 뒤 렌더링 시간이 흘렀으면 평활한 크기를 갱신한다({@link #lock} 으로 보호).
     */
    private void analyse() {
        long now = getContext().getCurrentFrame();
        if (now <= lastAnalysisFrame) {
            return;
        }
        lastAnalysisFrame = now;
        int n = fftSize;
        double[] re = new double[n];
        double[] im = new double[n];
        copyRecent(re);
        // 시간 영역 데이터에 Blackman 창을 곱한다.
        for (int i = 0; i < n; i++) {
            re[i] *= window[i];
        }
        fft.transform(re, im, false);
        double tau = smoothingTimeConstant;
        // 크기를 1/N 로 맞추고 이전 결과와 평활한다.
        for (int k = 0; k < n / 2; k++) {
            double mag = Math.sqrt(re[k] * re[k] + im[k] * im[k]) / n;
            double s = tau * magnitudes[k] + (1.0 - tau) * mag;
            magnitudes[k] = (Double.isNaN(s) || Double.isInfinite(s)) ? 0.0 : s;
        }
    }

    /**
     * Copies the current frequency data in decibels into an array: as many values as the array holds, at most
     * <code>frequencyBinCount</code>.
     * <p>
     * 현재 주파수 데이터를 데시벨로 배열에 복사한다. 배열에 들어가는 만큼, 최대 <code>frequencyBinCount</code> 개이다.
     *
     * @param array the destination / 대상
     */
    public void getFloatFrequencyData(float[] array) {
        synchronized (lock) {
            analyse();
            int len = Math.min(array.length, magnitudes.length);
            // 크기를 데시벨로 바꿔 복사한다(크기 0 은 음의 무한대).
            for (int i = 0; i < len; i++) {
                array[i] = (float) (20.0 * Math.log10(magnitudes[i]));
            }
        }
    }

    /**
     * Copies the current frequency data into an array of unsigned bytes: the decibels scaled so that <code>minDecibels</code> is 0 and
     * <code>maxDecibels</code> is 255, clipped to 0..255.
     * <p>
     * 현재 주파수 데이터를 부호 없는 바이트 배열에 복사한다. <code>minDecibels</code> 가 0, <code>maxDecibels</code> 가 255 가 되도록 데시벨을 맞추고
     * 0~255 로 자른다.
     *
     * @param array the destination (bytes are unsigned) / 대상(바이트는 부호 없음)
     */
    public void getByteFrequencyData(byte[] array) {
        synchronized (lock) {
            analyse();
            int len = Math.min(array.length, magnitudes.length);
            double range = maxDecibels - minDecibels;
            // 크기를 데시벨로 바꾸고 범위에 맞춰 0 ~ 255 의 바이트로 만든다.
            for (int i = 0; i < len; i++) {
                double db = 20.0 * Math.log10(magnitudes[i]);
                double scaled = Math.floor(255.0 / range * (db - minDecibels));
                array[i] = (byte) (scaled < 0 || Double.isNaN(scaled) ? 0 : (scaled > 255 ? 255 : (int) scaled));
            }
        }
    }

    /**
     * Copies the most recent <code>fftSize</code> frames of the down-mixed input into an array: as many values as the array holds.
     * <p>
     * 다운믹스한 입력의 가장 최근 <code>fftSize</code> 프레임을 배열에 복사한다. 배열에 들어가는 만큼이다.
     *
     * @param array the destination / 대상
     */
    public void getFloatTimeDomainData(float[] array) {
        synchronized (lock) {
            int mask = history.length - 1;
            int len = Math.min(array.length, fftSize);
            int start = writeIndex - fftSize;
            // 가장 오래된 프레임부터 복사한다.
            for (int i = 0; i < len; i++) {
                array[i] = history[(start + i) & mask];
            }
        }
    }

    /**
     * Copies the most recent <code>fftSize</code> frames into an array of unsigned bytes: <code>floor(128 (1 + x))</code> clipped to
     * 0..255.
     * <p>
     * 가장 최근 <code>fftSize</code> 프레임을 부호 없는 바이트 배열에 복사한다. <code>floor(128 (1 + x))</code> 을 0~255 로 자른 값이다.
     *
     * @param array the destination (bytes are unsigned) / 대상(바이트는 부호 없음)
     */
    public void getByteTimeDomainData(byte[] array) {
        synchronized (lock) {
            int mask = history.length - 1;
            int len = Math.min(array.length, fftSize);
            int start = writeIndex - fftSize;
            // 가장 오래된 프레임부터 바이트로 바꿔 복사한다.
            for (int i = 0; i < len; i++) {
                double scaled = Math.floor(128.0 * (1.0 + history[(start + i) & mask]));
                array[i] = (byte) (scaled < 0 || Double.isNaN(scaled) ? 0 : (scaled > 255 ? 255 : (int) scaled));
            }
        }
    }

    /**
     * Returns the tail time: the number of frames that the analysis window holds, so that the history is flushed with silence after the
     * input ended. The node produces no sound of its own, so this does not make the output audible.
     * <p>
     * 꼬리 시간을 반환한다: 분석 창이 담는 프레임 수. 입력이 끝난 뒤 이력이 무음으로 채워지도록 한다. 노드는 스스로 소리를 내지 않으므로 출력이 들리게
     * 만들지는 않는다.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    @Override
    long tailFrames() {
        return getFftSize();
    }

    /**
     * Passes the input to the output and appends it, down-mixed to mono, to the history.
     * <p>
     * 입력을 출력으로 보내고 모노로 다운믹스해 이력에 덧붙인다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        out.copyFrom(in);
        // 입력을 speakers 규칙으로 모노로 다운믹스한다(노드의 해석 설정과 무관).
        monoBus.zero();
        ChannelMixing.mix(in, monoBus, ChannelInterpretation.SPEAKERS, true);
        float[] mono = monoBus.channel(0);
        synchronized (lock) {
            int mask = history.length - 1;
            // 다운믹스한 프레임을 이력의 쓰기 위치부터 덧붙인다.
            for (int i = 0; i < frames; i++) {
                history[(writeIndex + i) & mask] = mono[i];
            }
            writeIndex = (writeIndex + frames) & mask;
        }
    }
}
