package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

/**
 * A streaming sample rate converter for one channel of floating point audio (plan item P25-05: the resampling of
 * <code>decodeAudioData</code> to the sample rate of the context). It is a windowed sinc interpolator in the polyphase form: every output
 * sample is the sum of the input samples around its position, weighted by a Kaiser windowed sinc low-pass kernel whose cut-off sits at the
 * Nyquist frequency of the <em>lower</em> of the two rates (so an up-conversion removes the images and a down-conversion removes what
 * would alias).
 * <p>
 * <b>Design and quality.</b> The kernel is designed for {@link #STOPBAND_ATTENUATION_DB} (100 dB, better than the 16 bit
 * resolution of the decoders that feed it) with a transition band of {@link #TRANSITION_FRACTION} of the lower Nyquist frequency: the pass
 * band is flat (the Kaiser ripple is about 10<sup>-5</sup>, 0.0001 dB) up to 90 % of that Nyquist frequency and everything at or above it is
 * attenuated by the stop band attenuation. The Kaiser window gives the specified attenuation with the fewest taps of the common window
 * families (the formula of Kaiser: <code>N = (A - 7.95) / (14.36 * df)</code> with <code>df</code> the transition width in cycles per
 * input sample); that is about 130 taps per output sample at the same rate and proportionally more when the output rate is lower
 * (the kernel is stretched by the ratio). Each phase of the kernel is normalised to a gain of exactly 1 at DC.
 * <p>
 * <b>Phases.</b> When both rates are whole numbers the ratio is reduced to <code>up / down</code> and, if <code>up</code> is at most
 * {@link #MAX_EXACT_PHASES} and the table is small enough, every output sample uses one of <code>up</code> exact precomputed phases
 * (no interpolation error at all, e.g. 44100 to 48000 uses 160 phases). Otherwise (large <code>up</code>, non-integer rates) the table has
 * {@link #INTERPOLATED_PHASES} rows (fewer for a very wide kernel) and the two nearest rows are interpolated linearly; the error of that
 * is below -110 dB for the 1024 rows (it shrinks with the square of the ratio of the sample rates for fewer rows).
 * <p>
 * <b>Position and length.</b> Output sample <code>n</code> is at the time <code>n / outputRate</code>, i.e. at the input position
 * <code>n * inputRate / outputRate</code>; the filter is symmetric around that position, so there is no delay. The input before the first
 * sample and after the last one is taken as zero. The output has <code>ceil(inputFrames * outputRate / inputRate)</code> samples (see
 * {@link #outputLength(long)}): every output sample whose time lies inside the duration of the input is produced, so the duration is
 * preserved to less than one output sample and no tail of the input is cut off. The result near the two ends is the response of the filter
 * to the zero extension, as with any finite signal.
 * <p>
 * <b>Streaming.</b> Samples are given in chunks of any size with {@link #process(float[], int, int, float[], int)}, then the tail is
 * flushed with {@link #finish(float[], int)}; the result does not depend on how the input is split (it is bit for bit the same). Only a
 * window of the input of the kernel length is kept, so memory does not grow with the length of the signal. One instance converts one
 * channel; use one instance per channel. An instance is not thread-safe (no sharing between threads without a lock), but instances share
 * nothing, so different instances may run in different threads.
 * <p>
 * 부동 소수점 오디오 한 채널용 스트리밍 표본율 변환기(계획 P25-05: <code>decodeAudioData</code> 를 컨텍스트의 표본율로 바꾸기). 다상(polyphase)
 * 형태의 창 함수를 씌운 sinc 보간기이다. 출력 표본마다 자기 위치 둘레의 입력 표본을 Kaiser 창을 씌운 sinc 저역 통과 커널로 가중해 더한다. 커널의 차단
 * 주파수는 두 표본율 중 <em>낮은 쪽</em>의 나이퀴스트 주파수이다(그래서 올릴 때는 이미지를 없애고 내릴 때는 앨리어싱될 성분을 없앤다).
 * <p>
 * <b>설계와 품질.</b> 커널은 {@link #STOPBAND_ATTENUATION_DB}(100 dB, 입력을 만드는 디코더의 16비트 해상도보다 좋다)에 낮은 쪽 나이퀴스트 주파수의
 * {@link #TRANSITION_FRACTION} 만큼의 전이 대역으로 설계한다. 통과 대역은 그 나이퀴스트 주파수의 90 % 까지 평탄하고(Kaiser 리플은 약 10<sup>-5</sup>,
 * 0.0001 dB) 그 이상은 정지 대역 감쇠만큼 줄어든다. Kaiser 창은 흔한 창 함수 중 정해진 감쇠를 가장 적은 탭으로 얻는다(Kaiser 의 식
 * <code>N = (A - 7.95) / (14.36 * df)</code>, <code>df</code> 는 입력 표본당 주기로 잰 전이 폭). 같은 표본율에서는 출력 표본당 약 130 탭이고
 * 출력 표본율이 낮으면 그 비율만큼 늘어난다(커널을 비율만큼 늘린다). 커널의 각 위상은 DC 이득이 정확히 1 이 되도록 정규화한다.
 * <p>
 * <b>위상.</b> 두 표본율이 모두 정수이면 비율을 <code>up / down</code> 으로 약분하고, <code>up</code> 이 {@link #MAX_EXACT_PHASES} 이하이며 표가 충분히
 * 작으면 출력 표본마다 미리 계산한 <code>up</code> 개의 정확한 위상 중 하나를 쓴다(보간 오차가 전혀 없다. 예: 44100 에서 48000 은 160 위상). 그렇지
 * 않으면(큰 <code>up</code>, 정수가 아닌 표본율) 표는 {@link #INTERPOLATED_PHASES} 행(아주 넓은 커널이면 더 적게)이고 가장 가까운 두 행을 선형 보간한다.
 * 그 오차는 1024 행에서 -110 dB 아래이다(행이 적을 때는 표본율 비율의 제곱에 비례해 줄어든다).
 * <p>
 * <b>위치와 길이.</b> 출력 표본 <code>n</code> 은 시간 <code>n / outputRate</code>, 곧 입력 위치 <code>n * inputRate / outputRate</code> 에 있다. 필터는
 * 그 위치에 대칭이므로 지연이 없다. 첫 표본 앞과 마지막 표본 뒤의 입력은 0 으로 본다. 출력은
 * <code>ceil(inputFrames * outputRate / inputRate)</code> 개이다({@link #outputLength(long)} 참조). 입력의 재생 시간 안에 시간이 드는 출력 표본을 모두
 * 만들므로 재생 시간이 출력 표본 하나보다 작은 오차로 보존되고 입력의 끝이 잘리지 않는다. 양 끝 근처의 결과는 유한한 신호가 늘 그렇듯 0 으로 연장한
 * 신호에 대한 필터의 응답이다.
 * <p>
 * <b>스트리밍.</b> 표본은 어떤 크기의 덩어리로든 {@link #process(float[], int, int, float[], int)} 에 주고 마지막에 {@link #finish(float[], int)} 로
 * 꼬리를 비운다. 결과는 입력을 어떻게 나누었는지와 무관하다(비트 단위로 같다). 커널 길이만큼의 입력 창만 보관하므로 신호가 길어도 메모리가 늘지 않는다.
 * 인스턴스 하나가 채널 하나를 변환한다. 채널마다 인스턴스를 쓴다. 인스턴스는 스레드 안전하지 않지만(잠금 없이 스레드 사이에 공유하지 말 것) 인스턴스끼리는
 * 아무것도 공유하지 않으므로 서로 다른 인스턴스는 서로 다른 스레드에서 돌려도 된다.
 */
public final class Resampler {

    /**
     * The stop band attenuation the kernel is designed for, in dB.
     * <p>
     * 커널을 설계하는 정지 대역 감쇠(dB).
     */
    public static final double STOPBAND_ATTENUATION_DB = 100.0;

    /**
     * The width of the transition band as a fraction of the Nyquist frequency of the lower rate: the pass band ends at
     * <code>1 - TRANSITION_FRACTION</code> and the stop band begins at 1 of that Nyquist frequency.
     * <p>
     * 전이 대역의 폭을 낮은 표본율의 나이퀴스트 주파수에 대한 비율로 나타낸 값: 통과 대역은 <code>1 - TRANSITION_FRACTION</code> 에서 끝나고 정지 대역은
     * 그 나이퀴스트 주파수의 1 에서 시작한다.
     */
    public static final double TRANSITION_FRACTION = 0.10;

    /**
     * The largest ratio between the two rates (in either direction) that the converter accepts.
     * <p>
     * 변환기가 받는 두 표본율 비율의 최댓값(어느 방향이든).
     */
    public static final double MAX_RATIO = 1024.0;

    /**
     * The largest number of exact phases of the reduced ratio that is tabulated without interpolation.
     * <p>
     * 보간 없이 표로 만드는 약분한 비율의 정확한 위상 수의 최댓값.
     */
    public static final int MAX_EXACT_PHASES = 1024;

    /**
     * The number of table rows that are interpolated when the ratio has no small exact form.
     * <p>
     * 비율에 작은 정확한 형태가 없을 때 보간하는 표의 행 수.
     */
    public static final int INTERPOLATED_PHASES = 1024;

    /**
     * The most floats that the kernel table may hold (24 MB), which limits the rows of a very wide kernel.
     * <p>
     * 커널 표가 담을 수 있는 float 의 최대 수(24 MB). 아주 넓은 커널의 행 수를 제한한다.
     */
    private static final int MAX_TABLE_FLOATS = 6 * 1024 * 1024;

    /**
     * The fewest rows of an interpolated table.
     * <p>
     * 보간하는 표의 최소 행 수.
     */
    private static final int MIN_INTERPOLATED_PHASES = 64;

    /**
     * The input sample rate in Hz.
     * <p>
     * 입력 표본율(Hz).
     */
    private final double inputRate;

    /**
     * The output sample rate in Hz.
     * <p>
     * 출력 표본율(Hz).
     */
    private final double outputRate;

    /**
     * True if the rates are equal: samples pass through unchanged.
     * <p>
     * 두 표본율이 같으면 true: 표본이 그대로 통과한다.
     */
    private final boolean identity;

    /**
     * True if both rates are whole numbers, so positions are exact fractions (<code>up / down</code>).
     * <p>
     * 두 표본율이 모두 정수이면 true: 위치가 정확한 분수(<code>up / down</code>)이다.
     */
    private final boolean rational;

    /**
     * The numerator of the reduced output / input ratio (valid if {@link #rational}).
     * <p>
     * 약분한 출력/입력 비율의 분자({@link #rational} 일 때 유효).
     */
    private final long up;

    /**
     * The denominator of the reduced output / input ratio (valid if {@link #rational}).
     * <p>
     * 약분한 출력/입력 비율의 분모({@link #rational} 일 때 유효).
     */
    private final long down;

    /**
     * The input samples per output sample (<code>inputRate / outputRate</code>).
     * <p>
     * 출력 표본당 입력 표본 수(<code>inputRate / outputRate</code>).
     */
    private final double step;

    /**
     * Half the kernel length in input samples: the kernel reaches <code>half - 1</code> samples before and <code>half</code> after the
     * integer part of the position.
     * <p>
     * 입력 표본 단위로 커널 길이의 절반: 커널은 위치의 정수 부분 앞으로 <code>half - 1</code>, 뒤로 <code>half</code> 표본에 닿는다.
     */
    private final int half;

    /**
     * The number of taps of a phase (<code>2 * half</code>).
     * <p>
     * 위상 하나의 탭 수(<code>2 * half</code>).
     */
    private final int taps;

    /**
     * The number of phases in the table (without the closing row of an interpolated table).
     * <p>
     * 표의 위상 수(보간하는 표의 마지막 닫는 행은 제외).
     */
    private final int rows;

    /**
     * True if the table is interpolated (inexact phases), false if every phase is exact.
     * <p>
     * 표를 보간하면(정확하지 않은 위상) true, 모든 위상이 정확하면 false.
     */
    private final boolean interpolated;

    /**
     * The kernel table: one row per phase, <code>taps</code> coefficients each (an interpolated table has one more row, for the phase 1).
     * <p>
     * 커널 표: 위상마다 한 행에 <code>taps</code> 개의 계수(보간하는 표는 위상 1 을 위한 행이 하나 더 있다).
     */
    private final float[][] bank;

    /**
     * The window of the input that the next outputs need.
     * <p>
     * 다음 출력이 필요로 하는 입력 창.
     */
    private float[] window;

    /**
     * The absolute index (0 is the first input sample) of <code>window[0]</code>.
     * <p>
     * <code>window[0]</code> 의 절대 인덱스(0 이 첫 입력 표본).
     */
    private long windowStart;

    /**
     * The number of valid samples in the window.
     * <p>
     * 창의 유효한 표본 수.
     */
    private int windowLength;

    /**
     * The number of input samples received.
     * <p>
     * 받은 입력 표본 수.
     */
    private long inputCount;

    /**
     * The number of output samples produced.
     * <p>
     * 만든 출력 표본 수.
     */
    private long outputCount;

    /**
     * True once the zero tail has been appended by {@link #finish(float[], int)}.
     * <p>
     * {@link #finish(float[], int)} 가 0 꼬리를 덧붙였으면 true.
     */
    private boolean padded;

    /**
     * Creates a converter for one channel.
     * <p>
     * 채널 하나를 위한 변환기를 만든다.
     *
     * @param inputRate  the input sample rate in Hz, positive and finite / 입력 표본율(Hz). 양수이고 유한해야 한다
     * @param outputRate the output sample rate in Hz, positive and finite / 출력 표본율(Hz). 양수이고 유한해야 한다
     * @throws IllegalArgumentException if a rate is not positive and finite, or the ratio is above {@link #MAX_RATIO} in either direction /
     *                                  표본율이 양의 유한한 값이 아니거나 비율이 어느 방향이든 {@link #MAX_RATIO} 를 넘으면
     */
    public Resampler(double inputRate, double outputRate) {
        if (!(inputRate > 0) || !(outputRate > 0) || Double.isInfinite(inputRate) || Double.isInfinite(outputRate)) {
            throw new IllegalArgumentException("the sample rates must be positive and finite: " + inputRate + " -> " + outputRate);
        }
        double ratio = outputRate / inputRate;
        if (ratio > MAX_RATIO || ratio < 1.0 / MAX_RATIO) {
            throw new IllegalArgumentException("the ratio of the sample rates is out of range: " + inputRate + " -> " + outputRate);
        }
        this.inputRate = inputRate;
        this.outputRate = outputRate;
        this.step = inputRate / outputRate;
        this.identity = inputRate == outputRate;
        boolean whole = isWhole(inputRate) && isWhole(outputRate);
        this.rational = whole;
        if (whole) {
            long a = Math.round(inputRate);
            long b = Math.round(outputRate);
            long g = gcd(a, b);
            this.up = b / g;
            this.down = a / g;
        } else {
            this.up = 0;
            this.down = 0;
        }
        if (identity) {
            this.half = 0;
            this.taps = 0;
            this.rows = 0;
            this.interpolated = false;
            this.bank = new float[0][];
            this.window = new float[0];
            return;
        }
        // 낮은 쪽 나이퀴스트 주파수(입력 표본당 주기)와 그에 맞춘 Kaiser 설계 값을 구한다.
        double scale = Math.min(1.0, outputRate / inputRate);
        double nyquist = 0.5 * scale;
        double cutoff = nyquist * (1.0 - TRANSITION_FRACTION / 2.0);
        double transition = nyquist * TRANSITION_FRACTION;
        double totalTaps = (STOPBAND_ATTENUATION_DB - 7.95) / (14.36 * transition);
        this.half = (int) Math.ceil(totalTaps / 2.0);
        this.taps = 2 * half;
        double beta = 0.1102 * (STOPBAND_ATTENUATION_DB - 8.7);
        // 정확한 위상 표가 작으면 위상마다 한 행을 쓰고 아니면 보간하는 표를 쓴다.
        if (whole && up <= MAX_EXACT_PHASES && up * taps <= MAX_TABLE_FLOATS) {
            this.rows = (int) up;
            this.interpolated = false;
        } else {
            this.rows = Math.max(MIN_INTERPOLATED_PHASES, Math.min(INTERPOLATED_PHASES, MAX_TABLE_FLOATS / taps - 1));
            this.interpolated = true;
        }
        this.bank = buildBank(cutoff, beta);
        reset();
    }

    /**
     * Tells whether a number is a whole number (within the rounding of a rate that was given as a float).
     * <p>
     * 수가 정수인지 알려 준다(float 로 준 표본율의 반올림 오차 안에서).
     *
     * @param x the number / 수
     * @return true if whole / 정수이면 true
     */
    private static boolean isWhole(double x) {
        return Math.abs(x - Math.rint(x)) < 1e-9 * Math.max(1.0, x);
    }

    /**
     * Returns the greatest common divisor.
     * <p>
     * 최대공약수를 반환한다.
     *
     * @param a the first number / 첫째 수
     * @param b the second number / 둘째 수
     * @return the divisor / 약수
     */
    private static long gcd(long a, long b) {
        // 유클리드 호제법으로 나머지가 0 이 될 때까지 줄인다.
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /**
     * The modified Bessel function of the first kind of order 0 (the window function of Kaiser), by its power series.
     * <p>
     * 0 차 제1종 변형 베셀 함수(Kaiser 의 창 함수). 멱급수로 구한다.
     *
     * @param x the argument / 인수
     * @return the value / 값
     */
    private static double besselI0(double x) {
        double sum = 1.0;
        double term = 1.0;
        double q = x * x / 4.0;
        // 항이 합에 비해 무시할 만큼 작아질 때까지 더한다.
        for (int k = 1; k < 500; k++) {
            term *= q / ((double) k * k);
            sum += term;
            if (term < sum * 1e-20) {
                break;
            }
        }
        return sum;
    }

    /**
     * The value of the windowed sinc kernel at a distance from the output position.
     * <p>
     * 출력 위치에서 떨어진 거리에서 창을 씌운 sinc 커널의 값.
     *
     * @param x      the distance in input samples / 입력 표본 단위의 거리
     * @param cutoff the cut-off in cycles per input sample / 입력 표본당 주기로 나타낸 차단 주파수
     * @param beta   the shape parameter of the Kaiser window / Kaiser 창의 모양 매개변수
     * @param i0Beta the Bessel function at <code>beta</code>, the normalisation of the window / <code>beta</code> 에서의 베셀 함수 값. 창의 정규화 값
     * @return the weight / 가중치
     */
    private double kernel(double x, double cutoff, double beta, double i0Beta) {
        double r = x / half;
        // 창의 폭 밖은 0 이다.
        if (r >= 1.0 || r <= -1.0) {
            return 0.0;
        }
        double w = besselI0(beta * Math.sqrt(1.0 - r * r)) / i0Beta;
        double u = 2.0 * cutoff * x;
        double sinc = u == 0.0 ? 1.0 : Math.sin(Math.PI * u) / (Math.PI * u);
        return 2.0 * cutoff * sinc * w;
    }

    /**
     * Builds the kernel table: one normalised row per phase.
     * <p>
     * 커널 표를 만든다: 위상마다 정규화한 행 하나.
     *
     * @param cutoff the cut-off in cycles per input sample / 입력 표본당 주기로 나타낸 차단 주파수
     * @param beta   the shape parameter of the Kaiser window / Kaiser 창의 모양 매개변수
     * @return the table / 표
     */
    private float[][] buildBank(double cutoff, double beta) {
        int count = interpolated ? rows + 1 : rows;
        float[][] table = new float[count][taps];
        double[] c = new double[taps];
        double i0Beta = besselI0(beta);
        // 위상마다 탭의 가중치를 계산하고 합이 1 이 되도록 정규화한다.
        for (int r = 0; r < count; r++) {
            double phi = (double) r / rows;
            double sum = 0.0;
            // 출력 위치로부터 각 탭까지의 거리에서 커널을 계산한다.
            for (int j = 0; j < taps; j++) {
                c[j] = kernel((-half + 1 + j) - phi, cutoff, beta, i0Beta);
                sum += c[j];
            }
            // 정규화하여 float 로 저장한다.
            for (int j = 0; j < taps; j++) {
                table[r][j] = (float) (c[j] / sum);
            }
        }
        return table;
    }

    /**
     * Starts again from the beginning of a signal: forgets the input received so far.
     * <p>
     * 신호의 처음부터 다시 시작한다: 지금까지 받은 입력을 잊는다.
     */
    public void reset() {
        windowStart = -half;
        windowLength = half;
        if (window == null || window.length < taps + 1024) {
            window = new float[taps + 8192];
        }
        // 첫 표본 앞의 입력은 0 이다.
        java.util.Arrays.fill(window, 0, windowLength, 0f);
        inputCount = 0;
        outputCount = 0;
        padded = false;
    }

    /**
     * Tells whether the two rates are equal, so that samples pass through unchanged.
     * <p>
     * 두 표본율이 같아서 표본이 그대로 통과하는지 알려 준다.
     *
     * @return true if the rates are equal / 표본율이 같으면 true
     */
    public boolean isIdentity() {
        return identity;
    }

    /**
     * Returns the input sample rate.
     * <p>
     * 입력 표본율을 반환한다.
     *
     * @return the rate in Hz / 표본율(Hz)
     */
    public double getInputRate() {
        return inputRate;
    }

    /**
     * Returns the output sample rate.
     * <p>
     * 출력 표본율을 반환한다.
     *
     * @return the rate in Hz / 표본율(Hz)
     */
    public double getOutputRate() {
        return outputRate;
    }

    /**
     * Returns the number of taps that every output sample sums (0 if the rates are equal).
     * <p>
     * 출력 표본마다 더하는 탭 수를 반환한다(표본율이 같으면 0).
     *
     * @return the number of taps / 탭 수
     */
    public int getTaps() {
        return taps;
    }

    /**
     * Returns the number of phases of the kernel table (0 if the rates are equal).
     * <p>
     * 커널 표의 위상 수를 반환한다(표본율이 같으면 0).
     *
     * @return the number of phases / 위상 수
     */
    public int getPhases() {
        return rows;
    }

    /**
     * Tells whether the table has exact phases (no interpolation between rows).
     * <p>
     * 표가 정확한 위상을 가졌는지(행 사이의 보간이 없는지) 알려 준다.
     *
     * @return true if every phase is exact / 모든 위상이 정확하면 true
     */
    public boolean isExact() {
        return !interpolated;
    }

    /**
     * Returns the number of output samples for a number of input samples: <code>ceil(inputFrames * outputRate / inputRate)</code> (computed
     * exactly in integers when both rates are whole numbers).
     * <p>
     * 입력 표본 수에 대한 출력 표본 수를 반환한다: <code>ceil(inputFrames * outputRate / inputRate)</code>(두 표본율이 모두 정수이면 정수로 정확히
     * 계산한다).
     *
     * @param inputFrames the number of input samples, 0 or more / 입력 표본 수(0 이상)
     * @return the number of output samples / 출력 표본 수
     */
    public long outputLength(long inputFrames) {
        if (identity) {
            return inputFrames;
        }
        if (rational) {
            return (inputFrames * up + down - 1) / down;
        }
        return (long) Math.ceil(inputFrames * outputRate / inputRate - 1e-9);
    }

    /**
     * Returns the largest number of output samples that one call of {@link #process} with this many input samples can produce.
     * <p>
     * 입력 표본이 이만큼인 {@link #process} 호출 한 번이 만들 수 있는 출력 표본 수의 최댓값을 반환한다.
     *
     * @param inputLength the number of input samples of the call / 호출의 입력 표본 수
     * @return the capacity that an output array needs / 출력 배열에 필요한 용량
     */
    public int maxOutputFrames(int inputLength) {
        if (identity) {
            return inputLength;
        }
        return (int) Math.ceil((inputLength + 2.0) * outputRate / inputRate) + 3;
    }

    /**
     * Returns the largest number of output samples that {@link #finish} can produce.
     * <p>
     * {@link #finish} 가 만들 수 있는 출력 표본 수의 최댓값을 반환한다.
     *
     * @return the capacity that an output array needs / 출력 배열에 필요한 용량
     */
    public int maxFinishFrames() {
        if (identity) {
            return 0;
        }
        return (int) Math.ceil((half + 3.0) * outputRate / inputRate) + 3;
    }

    /**
     * Feeds input samples and writes the output samples that can be computed now. Output samples that need input that has not arrived
     * yet are produced by a later call. If the output array has no room for all of them, the rest are kept and written by the next call.
     * <p>
     * 입력 표본을 주고 지금 계산할 수 있는 출력 표본을 쓴다. 아직 오지 않은 입력이 필요한 출력 표본은 이후의 호출이 만든다. 출력 배열에 모두 쓸 자리가
     * 없으면 나머지는 남겨 두었다가 다음 호출이 쓴다.
     *
     * @param in        the input samples / 입력 표본
     * @param inOffset  the index of the first input sample / 첫 입력 표본의 인덱스
     * @param inLength  the number of input samples (the array must hold {@link #maxOutputFrames(int)} for this length to be certain that
     *                  nothing is kept) / 입력 표본 수(아무것도 남지 않는다고 확신하려면 출력 배열이 이 길이에 대한 {@link #maxOutputFrames(int)} 를 담아야 한다)
     * @param out       the array that receives the output samples / 출력 표본을 받는 배열
     * @param outOffset the index of the first output sample / 첫 출력 표본의 인덱스
     * @return the number of output samples written / 쓴 출력 표본 수
     * @throws IllegalStateException if {@link #finish} has been called (call {@link #reset()} first) / {@link #finish} 를 이미 불렀으면({@link #reset()} 을 먼저 부를 것)
     */
    public int process(float[] in, int inOffset, int inLength, float[] out, int outOffset) {
        if (padded) {
            throw new IllegalStateException("the stream is finished");
        }
        if (identity) {
            // 같은 표본율에서는 입력을 그대로 복사하므로 출력 배열에 입력 전체가 들어가야 한다.
            if (out.length - outOffset < inLength) {
                throw new IllegalArgumentException("the output array is too small: " + (out.length - outOffset) + " < " + inLength);
            }
            System.arraycopy(in, inOffset, out, outOffset, inLength);
            inputCount += inLength;
            outputCount += inLength;
            return inLength;
        }
        append(in, inOffset, inLength);
        return generate(out, outOffset, false);
    }

    /**
     * Ends the signal: the input after the last sample is taken as zero and the remaining output samples (up to
     * {@link #outputLength(long) outputLength(total input)}) are written. If the output array has no room for all of them, the rest is
     * written by the next call of this method, which can be repeated until it returns 0.
     * <p>
     * 신호를 끝낸다: 마지막 표본 뒤의 입력은 0 으로 보고 남은 출력 표본({@link #outputLength(long) outputLength(전체 입력)} 까지)을 쓴다. 출력 배열에
     * 모두 쓸 자리가 없으면 나머지는 이 메서드의 다음 호출이 쓴다. 0 을 반환할 때까지 되풀이할 수 있다.
     *
     * @param out       the array that receives the output samples / 출력 표본을 받는 배열
     * @param outOffset the index of the first output sample / 첫 출력 표본의 인덱스
     * @return the number of output samples written / 쓴 출력 표본 수
     */
    public int finish(float[] out, int outOffset) {
        if (identity) {
            return 0;
        }
        // 처음 부를 때 꼬리를 0 으로 채워 마지막 출력 표본이 필요로 하는 입력을 갖춘다.
        if (!padded) {
            ensureCapacity(windowLength + half + 1);
            java.util.Arrays.fill(window, windowLength, windowLength + half + 1, 0f);
            windowLength += half + 1;
            padded = true;
        }
        return generate(out, outOffset, true);
    }

    /**
     * Appends input samples to the window.
     * <p>
     * 입력 표본을 창에 덧붙인다.
     *
     * @param in       the samples / 표본
     * @param offset   the first index / 첫 인덱스
     * @param length   the number of samples / 표본 수
     */
    private void append(float[] in, int offset, int length) {
        ensureCapacity(windowLength + length);
        System.arraycopy(in, offset, window, windowLength, length);
        windowLength += length;
        inputCount += length;
    }

    /**
     * Makes the window array large enough.
     * <p>
     * 창 배열을 충분히 크게 만든다.
     *
     * @param needed the number of samples that must fit / 들어가야 하는 표본 수
     */
    private void ensureCapacity(int needed) {
        if (needed > window.length) {
            float[] bigger = new float[Math.max(needed, window.length * 2)];
            System.arraycopy(window, 0, bigger, 0, windowLength);
            window = bigger;
        }
    }

    /**
     * Returns the integer part of the input position of an output sample.
     * <p>
     * 출력 표본의 입력 위치의 정수 부분을 반환한다.
     *
     * @param n the index of the output sample / 출력 표본 인덱스
     * @return the index of the input sample at or before the position / 위치와 같거나 그 앞인 입력 표본의 인덱스
     */
    private long positionOf(long n) {
        if (rational) {
            return (n * down) / up;
        }
        return (long) Math.floor(n * step);
    }

    /**
     * Computes the output samples that the window allows and drops the input that is no longer needed.
     * <p>
     * 창이 허용하는 출력 표본을 계산하고 더 필요하지 않은 입력을 버린다.
     *
     * @param out    the output array / 출력 배열
     * @param outOff the first index / 첫 인덱스
     * @param last   true after {@link #finish}: stop at the end of the signal / {@link #finish} 뒤에는 true: 신호의 끝에서 멈춘다
     * @return the number of samples written / 쓴 표본 수
     */
    private int generate(float[] out, int outOff, boolean last) {
        long total = last ? outputLength(inputCount) : Long.MAX_VALUE;
        long available = windowStart + windowLength;
        int capacity = out.length - outOff;
        int produced = 0;
        // 창이 허용하는 만큼 출력 표본을 하나씩 계산한다.
        while (outputCount < total && produced < capacity) {
            long ip;
            int row;
            double t = 0.0;
            if (rational) {
                long num = outputCount * down;
                ip = num / up;
                long rem = num - ip * up;
                if (interpolated) {
                    double scaled = (double) rem * rows / up;
                    row = (int) scaled;
                    t = scaled - row;
                } else {
                    row = (int) rem;
                }
            } else {
                double p = outputCount * step;
                ip = (long) Math.floor(p);
                double scaled = (p - ip) * rows;
                row = (int) scaled;
                t = scaled - row;
            }
            // 커널이 닿는 마지막 입력이 아직 오지 않았으면 멈춘다.
            if (ip + half >= available) {
                break;
            }
            if (row >= rows) {
                row = rows - 1;
            }
            int base = (int) (ip - half + 1 - windowStart);
            out[outOff + produced] = interpolated ? interpolate(base, row, t) : exact(base, row);
            produced++;
            outputCount++;
        }
        compact();
        return produced;
    }

    /**
     * The output sample of an exact phase: the sum of the taps.
     * <p>
     * 정확한 위상의 출력 표본: 탭의 합.
     *
     * @param base the window index of the first tap / 첫 탭의 창 인덱스
     * @param row  the phase / 위상
     * @return the sample / 표본
     */
    private float exact(int base, int row) {
        float[] c = bank[row];
        float[] w = window;
        double acc = 0.0;
        // 탭마다 입력 표본과 계수를 곱해 더한다.
        for (int j = 0; j < taps; j++) {
            acc += (double) w[base + j] * c[j];
        }
        return (float) acc;
    }

    /**
     * The output sample between two rows of the table: both rows are summed and the sums are interpolated linearly, which equals the
     * interpolation of the coefficients.
     * <p>
     * 표의 두 행 사이의 출력 표본: 두 행으로 각각 합을 구하고 그 합을 선형 보간한다. 계수를 보간하는 것과 같다.
     *
     * @param base the window index of the first tap / 첫 탭의 창 인덱스
     * @param row  the lower row / 아래 행
     * @param t    the fraction between the rows, from 0 up to 1 / 행 사이의 비율(0 이상 1 미만)
     * @return the sample / 표본
     */
    private float interpolate(int base, int row, double t) {
        float[] c0 = bank[row];
        float[] c1 = bank[row + 1];
        float[] w = window;
        double a0 = 0.0;
        double a1 = 0.0;
        // 탭마다 두 행의 계수를 따로 곱해 더한다.
        for (int j = 0; j < taps; j++) {
            double x = w[base + j];
            a0 += x * c0[j];
            a1 += x * c1[j];
        }
        return (float) (a0 + t * (a1 - a0));
    }

    /**
     * Drops the input that the following outputs do not need any more.
     * <p>
     * 이후의 출력이 더는 필요로 하지 않는 입력을 버린다.
     */
    private void compact() {
        long keepFrom = positionOf(outputCount) - half + 1;
        long drop = keepFrom - windowStart;
        // 버릴 입력이 있으면 앞으로 당긴다.
        if (drop > 0) {
            int d = (int) Math.min(drop, (long) windowLength);
            System.arraycopy(window, d, window, 0, windowLength - d);
            windowLength -= d;
            windowStart += d;
        }
    }

    /**
     * Converts a whole signal of one channel in one call (a convenience over the streaming interface).
     * <p>
     * 한 채널의 신호 전체를 한 번에 변환한다(스트리밍 인터페이스 위의 편의 메서드).
     *
     * @param in         the input samples / 입력 표본
     * @param inputRate  the input sample rate in Hz / 입력 표본율(Hz)
     * @param outputRate the output sample rate in Hz / 출력 표본율(Hz)
     * @return the converted samples, <code>ceil(length * outputRate / inputRate)</code> of them / 변환한 표본.
     *         <code>ceil(length * outputRate / inputRate)</code> 개
     * @throws IllegalArgumentException for rates that the constructor rejects / 생성자가 거부하는 표본율이면
     */
    public static float[] resample(float[] in, double inputRate, double outputRate) {
        Resampler r = new Resampler(inputRate, outputRate);
        long wanted = r.outputLength(in.length);
        float[] out = new float[(int) wanted];
        int written = 0;
        int chunk = 8192;
        // 입력을 덩어리로 나누어 주고 만든 출력을 이어 붙인다.
        for (int pos = 0; pos < in.length; pos += chunk) {
            int n = Math.min(chunk, in.length - pos);
            float[] tmp = new float[r.maxOutputFrames(n)];
            int w = r.process(in, pos, n, tmp, 0);
            System.arraycopy(tmp, 0, out, written, w);
            written += w;
        }
        float[] tail = new float[r.maxFinishFrames()];
        int w = r.finish(tail, 0);
        System.arraycopy(tail, 0, out, written, w);
        written += w;
        if (written != out.length) {
            throw new IllegalStateException("the converter produced " + written + " samples instead of " + out.length);
        }
        return out;
    }
}
