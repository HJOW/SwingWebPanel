package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.Fft;

/**
 * An arbitrary periodic waveform of an {@link OscillatorNode} (Web Audio API 1.28): the Fourier coefficients
 * <code>x(t) = sum (a[k] cos 2 pi k t + b[k] sin 2 pi k t)</code> turned into band-limited wave tables. The oscillator plays a table by
 * reading it at a phase that advances by the frequency; a table that contains partials above the Nyquist frequency would alias, so the
 * wave keeps several tables (three per octave) and each one has fewer partials than the one before it. A frequency selects the two
 * neighbouring tables and the sample is the interpolation of both. Unless the normalization is disabled the whole waveform is scaled so
 * that the maximum of the table with all partials is 1 (section 1.28.5). The table size depends on the sample rate (2048 up to 24 kHz,
 * 4096 up to 88.2 kHz, 16384 above), as in the reference implementation. The object is immutable and is shared by the oscillators of
 * one context.
 * <p>
 * {@link OscillatorNode} 의 임의 주기 파형(Web Audio API 1.28): 푸리에 계수 <code>x(t) = sum (a[k] cos 2 pi k t + b[k] sin 2 pi k t)</code> 를 대역
 * 제한된 파형 표로 바꾼 것. 발진기는 주파수만큼 나아가는 위상으로 표를 읽어 재생한다. 나이퀴스트 주파수 위의 부분음이 든 표는 에일리어싱이 생기므로
 * 파형은 표를 여러 개(옥타브당 세 개) 가지며 각 표는 앞의 표보다 부분음이 적다. 주파수가 이웃한 두 표를 고르고 표본은 둘의 보간이다. 정규화를 끄지 않는
 * 한 파형 전체는 모든 부분음이 든 표의 최댓값이 1 이 되도록 크기가 조정된다(1.28.5절). 표 크기는 참조 구현처럼 샘플링 주파수에 따라 정해진다(24 kHz
 * 까지 2048, 88.2 kHz 까지 4096, 그 위는 16384). 객체는 불변이며 한 컨텍스트의 발진기들이 공유한다.
 */
public final class PeriodicWave {

    /**
     * The number of tables per octave.
     * <p>
     * 옥타브당 표의 수.
     */
    private static final int BANDS_PER_OCTAVE = 3;

    /**
     * The cents of one octave.
     * <p>
     * 한 옥타브의 센트.
     */
    private static final double CENTS_PER_OCTAVE = 1200.0;

    /**
     * The result of choosing the tables for a frequency; the oscillator keeps one and reuses it while its frequency does not change.
     * <p>
     * 주파수에 맞는 표를 고른 결과. 발진기가 하나를 가지고 주파수가 바뀌지 않는 동안 재사용한다.
     */
    static final class Selection {

        /**
         * The table with fewer partials.
         * <p>
         * 부분음이 더 적은 표.
         */
        float[] lower;

        /**
         * The table with more partials.
         * <p>
         * 부분음이 더 많은 표.
         */
        float[] higher;

        /**
         * The weight of the lower table, from 0 to 1.
         * <p>
         * 부분음이 적은 표의 가중치(0 ~ 1).
         */
        double factor;

        /**
         * The absolute frequency for which the tables were chosen, or NaN if none was chosen yet.
         * <p>
         * 표를 고른 절대 주파수. 아직 고르지 않았으면 NaN.
         */
        double frequency = Double.NaN;
    }

    /**
     * The number of samples of every table, a power of two.
     * <p>
     * 모든 표의 샘플 수(2의 거듭제곱).
     */
    private final int size;

    /**
     * The mask that wraps a table index.
     * <p>
     * 표 인덱스를 되감는 마스크.
     */
    private final int mask;

    /**
     * The band-limited tables, from the one with all partials to the one with none.
     * <p>
     * 대역 제한된 표. 모든 부분음이 든 표부터 하나도 없는 표까지.
     */
    private final float[][] tables;

    /**
     * The fundamental frequency of the whole table, <code>sampleRate / size</code>.
     * <p>
     * 표 전체의 기본 주파수 <code>sampleRate / size</code>.
     */
    private final double lowestFundamental;

    /**
     * The index of the last table that still contains the fundamental (the first partial): <code>3 log2(size / 2)</code>. A frequency
     * below the Nyquist frequency never selects a table beyond it, so a pure sine keeps its amplitude up to the Nyquist frequency; the
     * reference implementation attenuates the fundamental in the last third of an octave.
     * <p>
     * 기본파(첫 부분음)가 아직 들어 있는 마지막 표의 인덱스 <code>3 log2(size / 2)</code>. 나이퀴스트 주파수 아래의 주파수는 그 뒤의 표를 고르지 않으므로
     * 순수한 사인파는 나이퀴스트 주파수까지 진폭을 유지한다(참조 구현은 마지막 1/3 옥타브에서 기본파를 줄인다).
     */
    private final double fundamentalRange;

    /**
     * The factor that turns a frequency in Hz into a table index increment per frame, <code>size / sampleRate</code>.
     * <p>
     * Hz 단위 주파수를 프레임당 표 인덱스 증가량으로 바꾸는 계수 <code>size / sampleRate</code>.
     */
    private final double rateScale;

    /**
     * Creates a periodic wave from Fourier coefficients.
     * <p>
     * 푸리에 계수로 주기파를 만든다.
     *
     * @param sampleRate           the sample rate of the context / 컨텍스트의 샘플링 주파수
     * @param real                 the cosine terms; index 0 (the DC offset) is ignored / 코사인 항. 0번(직류)은 무시한다
     * @param imag                 the sine terms; index 0 is ignored / 사인 항. 0번은 무시한다
     * @param disableNormalization true to keep the amplitude of the coefficients / 계수의 진폭을 그대로 두려면 true
     * @throws AudioException <code>IndexSizeError</code> if the arrays differ in length or have fewer than 2 elements, <code>NotSupportedError</code>
     *                        if they are longer than the supported length, <code>TypeError</code> for a coefficient that is not finite / 배열 길이가 다르거나
     *                        2개 미만이면 <code>IndexSizeError</code>, 지원 길이보다 길면 <code>NotSupportedError</code>, 계수가 유한하지 않으면
     *                        <code>TypeError</code>
     */
    public PeriodicWave(float sampleRate, float[] real, float[] imag, boolean disableNormalization) {
        if (real == null || imag == null) {
            throw new AudioException(AudioException.Kind.TYPE, "real or imag is null");
        }
        if (real.length != imag.length) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "length of real (" + real.length + ") and imag (" + imag.length + ") differ");
        }
        if (real.length < 2) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "the arrays have fewer than 2 elements");
        }
        if (real.length > WebAudioLimits.MAX_PERIODIC_WAVE_LENGTH) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "more than " + WebAudioLimits.MAX_PERIODIC_WAVE_LENGTH + " terms");
        }
        // 계수가 모두 유한한지 확인한다.
        for (int i = 0; i < real.length; i++) {
            if (Float.isNaN(real[i]) || Float.isInfinite(real[i]) || Float.isNaN(imag[i]) || Float.isInfinite(imag[i])) {
                throw new AudioException(AudioException.Kind.TYPE, "coefficient " + i + " is not finite");
            }
        }
        this.size = sampleRate <= 24000f ? 2048 : (sampleRate <= 88200f ? 4096 : 16384);
        this.mask = size - 1;
        int ranges = (int) Math.round(BANDS_PER_OCTAVE * (Math.log(size) / Math.log(2)));
        this.tables = new float[ranges][];
        this.lowestFundamental = sampleRate / (double) size;
        this.rateScale = size / (double) sampleRate;
        this.fundamentalRange = Math.min(ranges - 1, BANDS_PER_OCTAVE * (Math.log(size / 2.0) / Math.log(2)));
        build(real, imag, disableNormalization);
    }

    /**
     * Creates the periodic wave of a basic oscillator type from its coefficients (Web Audio API 1.28.6), always normalized.
     * <p>
     * 기본 발진기 종류의 주기파를 그 계수로 만든다(Web Audio API 1.28.6). 항상 정규화한다.
     *
     * @param type       the type, not custom / 종류(custom 이 아님)
     * @param sampleRate the sample rate of the context / 컨텍스트의 샘플링 주파수
     * @return the wave / 파형
     */
    static PeriodicWave basic(OscillatorType type, float sampleRate) {
        int half = (sampleRate <= 24000f ? 2048 : (sampleRate <= 88200f ? 4096 : 16384)) / 2;
        float[] real = new float[half];
        float[] imag = new float[half];
        // 항 번호 n 마다 종류별 사인 계수를 정한다(규격 1.28.6절). 코사인 항은 모두 0 이다.
        for (int n = 1; n < half; n++) {
            double piFactor = 2.0 / (n * Math.PI);
            double b;
            switch (type) {
            case SINE:
                b = n == 1 ? 1.0 : 0.0;
                break;
            case SQUARE:
                b = (n & 1) != 0 ? 2.0 * piFactor : 0.0;
                break;
            case SAWTOOTH:
                b = piFactor * ((n & 1) != 0 ? 1.0 : -1.0);
                break;
            default:
                // TRIANGLE: b[n] = 8 sin(n pi / 2) / (pi n)^2
                b = (n & 1) != 0 ? 2.0 * piFactor * piFactor * ((((n - 1) >> 1) & 1) != 0 ? -1.0 : 1.0) : 0.0;
                break;
            }
            imag[n] = (float) b;
        }
        return new PeriodicWave(sampleRate, real, imag, false);
    }

    /**
     * Builds the band-limited tables.
     * <p>
     * 대역 제한된 표를 만든다.
     *
     * @param real                 the cosine terms / 코사인 항
     * @param imag                 the sine terms / 사인 항
     * @param disableNormalization true to skip the normalization / 정규화를 건너뛰려면 true
     */
    private void build(float[] real, float[] imag, boolean disableNormalization) {
        int half = size / 2;
        int components = Math.min(real.length, half);
        Fft fft = new Fft(size);
        double[] re = new double[size];
        double[] im = new double[size];
        double scale = 1.0;
        // 표마다 높은 부분음을 잘라 낸 뒤 역 FFT 로 시간 영역 표를 만든다. 첫 표(모든 부분음)의 최댓값으로 정규화 계수를 구한다.
        for (int r = 0; r < tables.length; r++) {
            double culling = Math.pow(2.0, -(r * CENTS_PER_OCTAVE / BANDS_PER_OCTAVE) / CENTS_PER_OCTAVE);
            int partials = (int) (culling * half);
            int keep = Math.min(components, partials + 1);
            java.util.Arrays.fill(re, 0.0);
            java.util.Arrays.fill(im, 0.0);
            // Y[k] = a[k] - i b[k] 의 실수부가 sum(a cos + b sin) 이다. 직류(0번)는 지운다.
            for (int k = 1; k < keep; k++) {
                re[k] = real[k];
                im[k] = -imag[k];
            }
            fft.transform(re, im, true);
            float[] table = new float[size];
            double max = 0.0;
            // 실수부를 표로 옮기고 최댓값을 구한다.
            for (int n = 0; n < size; n++) {
                table[n] = (float) re[n];
                max = Math.max(max, Math.abs(re[n]));
            }
            if (r == 0 && !disableNormalization && max > 0.0) {
                scale = 1.0 / max;
            }
            // 같은 정규화 계수를 모든 표에 곱한다.
            if (scale != 1.0) {
                for (int n = 0; n < size; n++) {
                    table[n] = (float) (table[n] * scale);
                }
            }
            tables[r] = table;
        }
    }

    /**
     * Returns the number of samples of every table.
     * <p>
     * 모든 표의 샘플 수를 반환한다.
     *
     * @return the table size / 표 크기
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the table index increment per frame for a frequency of 1 Hz, <code>size / sampleRate</code>.
     * <p>
     * 주파수 1 Hz 에 대한 프레임당 표 인덱스 증가량 <code>size / sampleRate</code> 를 반환한다.
     *
     * @return the factor / 계수
     */
    public double getRateScale() {
        return rateScale;
    }

    /**
     * Returns the number of tables.
     * <p>
     * 표의 수를 반환한다.
     *
     * @return the number of tables / 표의 수
     */
    int getTableCount() {
        return tables.length;
    }

    /**
     * Chooses the two tables for a frequency and the weight between them.
     * <p>
     * 주파수에 맞는 두 표와 그 사이의 가중치를 고른다.
     *
     * @param absFrequency the absolute fundamental frequency in Hz / 기본 주파수의 절댓값(Hz)
     * @param s            receives the choice / 선택을 받는다
     */
    void select(double absFrequency, Selection s) {
        if (absFrequency == s.frequency) {
            return;
        }
        s.frequency = absFrequency;
        double ratio = absFrequency > 0 ? absFrequency / lowestFundamental : 0.5;
        double cents = Math.log(ratio) / Math.log(2.0) * CENTS_PER_OCTAVE;
        double range = 1 + cents / (CENTS_PER_OCTAVE / BANDS_PER_OCTAVE);
        // 마지막 표들은 부분음이 하나도 없으므로, 기본파가 나이퀴스트 주파수 아래에 있는 동안은 기본파가 남아 있는 마지막 표까지만 쓴다.
        range = Math.max(0.0, Math.min(range, fundamentalRange));
        int i1 = (int) range;
        int i2 = i1 < tables.length - 1 ? i1 + 1 : i1;
        s.higher = tables[i1];
        s.lower = tables[i2];
        s.factor = range - i1;
    }

    /**
     * Returns the interpolated sample at a table index.
     * <p>
     * 표 인덱스에서 보간한 샘플을 반환한다.
     *
     * @param s     the tables chosen for the frequency / 주파수에 맞게 고른 표
     * @param index the table index, from 0 to the table size (exclusive) / 표 인덱스(0 이상 표 크기 미만)
     * @return the sample / 샘플
     */
    float sample(Selection s, double index) {
        int i0 = (int) index;
        double frac = index - i0;
        i0 &= mask;
        int i1 = (i0 + 1) & mask;
        double high = s.higher[i0] + (s.higher[i1] - s.higher[i0]) * frac;
        if (s.factor == 0.0) {
            return (float) high;
        }
        double low = s.lower[i0] + (s.lower[i1] - s.lower[i0]) * frac;
        return (float) ((1.0 - s.factor) * high + s.factor * low);
    }
}
