package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The coefficients of a biquad filter (Web Audio API 1.13.5, "Filters Characteristics"). The transfer function is
 * <code>H(z) = (b0 + b1 z^-1 + b2 z^-2) / (1 + a1 z^-1 + a2 z^-2)</code> where the coefficients are already divided by <code>a0</code>.
 * The formulas of the eight filter types are those of the specification (the Audio EQ Cookbook). Where the formulas are not defined
 * (a frequency of 0 or of the Nyquist frequency, a Q of 0) the limit of the transfer function is used, so that automating a parameter
 * through such a value does not produce a NaN: this is the behavior of the reference implementation (Chromium, <code>biquad.cc</code>).
 * <p>
 * 쌍이차 필터의 계수(Web Audio API 1.13.5, "Filters Characteristics"). 전달 함수는 <code>H(z) = (b0 + b1 z^-1 + b2 z^-2) / (1 + a1 z^-1 + a2 z^-2)</code>
 * 이며 계수는 이미 <code>a0</code> 로 나눈 값이다. 여덟 가지 필터 종류의 식은 규격(Audio EQ Cookbook)의 식이다. 식이 정의되지 않는 곳(주파수 0 또는
 * 나이퀴스트 주파수, Q 0)에서는 전달 함수의 극한을 써서 파라미터를 그 값 위로 자동화해도 NaN 이 생기지 않게 한다: 참조 구현(Chromium,
 * <code>biquad.cc</code>)의 동작이다.
 */
public final class BiquadCoefficients {

    /**
     * Filter type: low-pass (the <code>Q</code> is in dB).
     * <p>
     * 필터 종류: 저역 통과(<code>Q</code> 는 dB).
     */
    public static final int LOWPASS = 0;

    /**
     * Filter type: high-pass (the <code>Q</code> is in dB).
     * <p>
     * 필터 종류: 고역 통과(<code>Q</code> 는 dB).
     */
    public static final int HIGHPASS = 1;

    /**
     * Filter type: band-pass.
     * <p>
     * 필터 종류: 대역 통과.
     */
    public static final int BANDPASS = 2;

    /**
     * Filter type: low shelf.
     * <p>
     * 필터 종류: 저역 선반.
     */
    public static final int LOWSHELF = 3;

    /**
     * Filter type: high shelf.
     * <p>
     * 필터 종류: 고역 선반.
     */
    public static final int HIGHSHELF = 4;

    /**
     * Filter type: peaking.
     * <p>
     * 필터 종류: 피킹.
     */
    public static final int PEAKING = 5;

    /**
     * Filter type: notch.
     * <p>
     * 필터 종류: 노치.
     */
    public static final int NOTCH = 6;

    /**
     * Filter type: all-pass.
     * <p>
     * 필터 종류: 전역 통과.
     */
    public static final int ALLPASS = 7;

    /**
     * The coefficient <code>b0</code> divided by <code>a0</code>.
     * <p>
     * <code>a0</code> 로 나눈 계수 <code>b0</code>.
     */
    public double b0;

    /**
     * The coefficient <code>b1</code> divided by <code>a0</code>.
     * <p>
     * <code>a0</code> 로 나눈 계수 <code>b1</code>.
     */
    public double b1;

    /**
     * The coefficient <code>b2</code> divided by <code>a0</code>.
     * <p>
     * <code>a0</code> 로 나눈 계수 <code>b2</code>.
     */
    public double b2;

    /**
     * The coefficient <code>a1</code> divided by <code>a0</code>.
     * <p>
     * <code>a0</code> 로 나눈 계수 <code>a1</code>.
     */
    public double a1;

    /**
     * The coefficient <code>a2</code> divided by <code>a0</code>.
     * <p>
     * <code>a0</code> 로 나눈 계수 <code>a2</code>.
     */
    public double a2;

    /**
     * Creates the coefficients of a filter that passes the signal unchanged.
     * <p>
     * 신호를 그대로 통과시키는 필터의 계수를 만든다.
     */
    public BiquadCoefficients() {
        set(1, 0, 0, 1, 0, 0);
    }

    /**
     * Sets the six coefficients and divides them by <code>a0</code>.
     * <p>
     * 여섯 계수를 정하고 <code>a0</code> 로 나눈다.
     *
     * @param nb0 the numerator coefficient 0 / 분자 계수 0
     * @param nb1 the numerator coefficient 1 / 분자 계수 1
     * @param nb2 the numerator coefficient 2 / 분자 계수 2
     * @param na0 the denominator coefficient 0 / 분모 계수 0
     * @param na1 the denominator coefficient 1 / 분모 계수 1
     * @param na2 the denominator coefficient 2 / 분모 계수 2
     */
    private void set(double nb0, double nb1, double nb2, double na0, double na1, double na2) {
        double inv = 1.0 / na0;
        b0 = nb0 * inv;
        b1 = nb1 * inv;
        b2 = nb2 * inv;
        a1 = na1 * inv;
        a2 = na2 * inv;
    }

    /**
     * Copies the coefficients of another filter.
     * <p>
     * 다른 필터의 계수를 복사한다.
     *
     * @param other the other coefficients / 다른 계수
     */
    public void copyFrom(BiquadCoefficients other) {
        b0 = other.b0;
        b1 = other.b1;
        b2 = other.b2;
        a1 = other.a1;
        a2 = other.a2;
    }

    /**
     * Computes the coefficients of a filter.
     * <p>
     * 필터의 계수를 계산한다.
     *
     * @param type   the filter type, one of the type constants / 필터 종류(종류 상수 중 하나)
     * @param freq   the frequency divided by the Nyquist frequency (1 is the Nyquist frequency), already including the detune / 나이퀴스트 주파수로 나눈
     *               주파수(1 이 나이퀴스트 주파수). 디튠이 이미 반영된 값
     * @param q      the Q value (in dB for low-pass and high-pass, linear for the others) / Q 값(저역/고역 통과는 dB, 나머지는 선형)
     * @param gainDb the gain in dB (used by the shelves and the peaking filter) / 이득(dB). 선반과 피킹 필터가 쓴다
     */
    public void compute(int type, double freq, double q, double gainDb) {
        // 주파수는 0 ~ 1 로 제한한다. 대역 통과는 음수만 막는다.
        double f = freq;
        if (type != BANDPASS) {
            f = Math.max(0.0, Math.min(1.0, f));
        } else {
            f = Math.max(0.0, f);
        }
        double a = Math.pow(10.0, gainDb / 40.0);
        double w0 = Math.PI * f;
        double cosw = Math.cos(w0);
        double sinw = Math.sin(w0);
        switch (type) {
        case LOWPASS: {
            if (f == 1.0) {
                set(1, 0, 0, 1, 0, 0);
            } else if (f > 0.0) {
                double alpha = sinw / (2.0 * Math.pow(10.0, q / 20.0));
                set((1 - cosw) / 2, 1 - cosw, (1 - cosw) / 2, 1 + alpha, -2 * cosw, 1 - alpha);
            } else {
                set(0, 0, 0, 1, 0, 0);
            }
            break;
        }
        case HIGHPASS: {
            if (f == 1.0) {
                set(0, 0, 0, 1, 0, 0);
            } else if (f > 0.0) {
                double alpha = sinw / (2.0 * Math.pow(10.0, q / 20.0));
                set((1 + cosw) / 2, -(1 + cosw), (1 + cosw) / 2, 1 + alpha, -2 * cosw, 1 - alpha);
            } else {
                set(1, 0, 0, 1, 0, 0);
            }
            break;
        }
        case BANDPASS: {
            double qq = Math.max(0.0, q);
            if (f > 0.0 && f < 1.0) {
                if (qq > 0.0) {
                    double alpha = sinw / (2.0 * qq);
                    set(alpha, 0, -alpha, 1 + alpha, -2 * cosw, 1 - alpha);
                } else {
                    set(1, 0, 0, 1, 0, 0);
                }
            } else {
                set(0, 0, 0, 1, 0, 0);
            }
            break;
        }
        case LOWSHELF: {
            if (f == 1.0) {
                set(a * a, 0, 0, 1, 0, 0);
            } else if (f > 0.0) {
                double alphaS = 0.5 * sinw * Math.sqrt(2.0);
                double k2 = 2 * Math.sqrt(a) * alphaS;
                set(a * ((a + 1) - (a - 1) * cosw + k2), 2 * a * ((a - 1) - (a + 1) * cosw), a * ((a + 1) - (a - 1) * cosw - k2),
                        (a + 1) + (a - 1) * cosw + k2, -2 * ((a - 1) + (a + 1) * cosw), (a + 1) + (a - 1) * cosw - k2);
            } else {
                set(1, 0, 0, 1, 0, 0);
            }
            break;
        }
        case HIGHSHELF: {
            if (f == 1.0) {
                set(1, 0, 0, 1, 0, 0);
            } else if (f > 0.0) {
                double alphaS = 0.5 * sinw * Math.sqrt(2.0);
                double k2 = 2 * Math.sqrt(a) * alphaS;
                set(a * ((a + 1) + (a - 1) * cosw + k2), -2 * a * ((a - 1) + (a + 1) * cosw), a * ((a + 1) + (a - 1) * cosw - k2),
                        (a + 1) - (a - 1) * cosw + k2, 2 * ((a - 1) - (a + 1) * cosw), (a + 1) - (a - 1) * cosw - k2);
            } else {
                set(a * a, 0, 0, 1, 0, 0);
            }
            break;
        }
        case PEAKING: {
            double qq = Math.max(0.0, q);
            if (f > 0.0 && f < 1.0) {
                if (qq > 0.0) {
                    double alpha = sinw / (2.0 * qq);
                    set(1 + alpha * a, -2 * cosw, 1 - alpha * a, 1 + alpha / a, -2 * cosw, 1 - alpha / a);
                } else {
                    set(a * a, 0, 0, 1, 0, 0);
                }
            } else {
                set(1, 0, 0, 1, 0, 0);
            }
            break;
        }
        case NOTCH: {
            double qq = Math.max(0.0, q);
            if (f > 0.0 && f < 1.0) {
                if (qq > 0.0) {
                    double alpha = sinw / (2.0 * qq);
                    set(1, -2 * cosw, 1, 1 + alpha, -2 * cosw, 1 - alpha);
                } else {
                    set(0, 0, 0, 1, 0, 0);
                }
            } else {
                set(1, 0, 0, 1, 0, 0);
            }
            break;
        }
        default: {
            // ALLPASS
            double qq = Math.max(0.0, q);
            if (f > 0.0 && f < 1.0) {
                if (qq > 0.0) {
                    double alpha = sinw / (2.0 * qq);
                    set(1 - alpha, -2 * cosw, 1 + alpha, 1 + alpha, -2 * cosw, 1 - alpha);
                } else {
                    set(-1, 0, 0, 1, 0, 0);
                }
            } else {
                set(1, 0, 0, 1, 0, 0);
            }
            break;
        }
        }
    }

    /**
     * Computes the complex frequency response at a normalized frequency.
     * <p>
     * 정규화된 주파수에서의 복소 주파수 응답을 계산한다.
     *
     * @param normalizedFrequency the frequency divided by the Nyquist frequency, from 0 to 1 / 나이퀴스트 주파수로 나눈 주파수(0 ~ 1)
     * @param out                 receives the real and the imaginary part of the response / 응답의 실수부와 허수부를 받는다
     */
    public void response(double normalizedFrequency, double[] out) {
        double omega = -Math.PI * normalizedFrequency;
        double zr = Math.cos(omega);
        double zi = Math.sin(omega);
        // 분자 b0 + (b1 + b2 z) z 와 분모 1 + (a1 + a2 z) z 를 복소수로 계산한다.
        double nr = b1 + b2 * zr;
        double ni = b2 * zi;
        double tr = nr * zr - ni * zi;
        double ti = nr * zi + ni * zr;
        double numR = b0 + tr;
        double numI = ti;
        double dr = a1 + a2 * zr;
        double di = a2 * zi;
        double ur = dr * zr - di * zi;
        double ui = dr * zi + di * zr;
        double denR = 1 + ur;
        double denI = ui;
        double scale = denR * denR + denI * denI;
        out[0] = (numR * denR + numI * denI) / scale;
        out[1] = (numI * denR - numR * denI) / scale;
    }

    /**
     * Estimates the number of frames after which the response of the filter to a silent input has decayed below the threshold of 16 bit
     * audio, from the radius of the poles. The estimate is limited to the given maximum (a filter that does not decay, because it is
     * unstable or has a pole on the unit circle, has the maximum).
     * <p>
     * 필터가 무음 입력에 대해 응답이 16비트 오디오의 문턱 아래로 줄어들기까지의 프레임 수를 극의 반지름으로 추정한다. 추정값은 주어진 최댓값으로
     * 제한된다(불안정하거나 단위원 위에 극이 있어 감쇠하지 않는 필터는 최댓값을 가진다).
     *
     * @param maxFrames the largest value to return / 반환할 최댓값
     * @return the tail time in frames / 꼬리 시간(프레임)
     */
    public long tailFrames(long maxFrames) {
        if (!isFinite(b0) || !isFinite(b1) || !isFinite(b2) || !isFinite(a1) || !isFinite(a2)) {
            return 0;
        }
        if (b0 == 0 && b1 == 0 && b2 == 0) {
            return 0;
        }
        double disc = a1 * a1 - 4 * a2;
        double radius;
        if (disc < 0) {
            radius = Math.sqrt(Math.abs(a2));
        } else {
            double s = Math.sqrt(disc);
            radius = Math.max(Math.abs((-a1 + s) / 2), Math.abs((-a1 - s) / 2));
        }
        if (radius >= 1.0) {
            return maxFrames;
        }
        if (radius < 1e-6) {
            return 2;
        }
        double frames = Math.log(1.0 / 32768.0) / Math.log(radius);
        return Math.min(maxFrames, (long) Math.ceil(frames) + 2);
    }

    /**
     * Tells whether a number is neither NaN nor infinite.
     * <p>
     * 수가 NaN 도 무한대도 아닌지 알려 준다.
     *
     * @param v the number / 수
     * @return true if finite / 유한하면 true
     */
    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
