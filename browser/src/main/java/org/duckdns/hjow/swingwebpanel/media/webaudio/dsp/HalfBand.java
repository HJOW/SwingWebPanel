package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The shared kernel design of the half-band stages that double or halve the sample rate (the oversampling of the wave shaper): the taps of
 * a half-sample interpolator, which are the odd taps of a half-band low-pass filter. The even taps of a half-band filter are zero except
 * for the centre, so an interpolator needs only the odd taps and the decimator is its transpose.
 * <p>
 * The design is an ideal band-limited interpolation at the half sample positions <code>+-(m + 0.5)</code>, <code>m = 0 .. P - 1</code>
 * (the values of <code>sinc</code> there are <code>(-1)^m / (pi (m + 0.5))</code>), tapered by a Kaiser window of the half width
 * <code>P</code> and scaled so that a constant signal is interpolated to the same constant. The filter is linear phase.
 * <p>
 * 샘플링 주파수를 두 배로 하거나 절반으로 하는 반대역 단계(웨이브셰이퍼의 오버샘플링)의 공통 커널 설계: 반 샘플 보간기의 탭. 이 탭은 반대역 저역 통과
 * 필터의 홀수 탭이다. 반대역 필터의 짝수 탭은 중심을 빼고 0 이므로 보간기에는 홀수 탭만 필요하고 데시메이터는 그것의 전치이다.
 * <p>
 * 설계는 반 샘플 위치 <code>+-(m + 0.5)</code>(<code>m = 0 .. P - 1</code>)에서의 이상적인 대역 제한 보간이다(그 위치의 <code>sinc</code> 값은
 * <code>(-1)^m / (pi (m + 0.5))</code>). 반폭 <code>P</code> 의 Kaiser 창으로 다듬고 상수 신호가 같은 상수로 보간되도록 크기를 맞춘다. 필터는 선형 위상이다.
 */
final class HalfBand {

    /**
     * The shape parameter of the Kaiser window: about 80 dB of stop band attenuation.
     * <p>
     * Kaiser 창의 모양 매개변수: 저지 대역 감쇠 약 80 dB.
     */
    static final double KAISER_BETA = 8.0;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private HalfBand() {
    }

    /**
     * Returns the modified Bessel function of the first kind and order zero, summed from its power series.
     * <p>
     * 0 차 제1종 변형 베셀 함수를 멱급수 합으로 구해 반환한다.
     *
     * @param x the argument / 인수
     * @return <code>I0(x)</code>
     */
    static double bessel0(double x) {
        double sum = 1.0;
        double term = 1.0;
        double q = x * x / 4.0;
        // 항이 합에 비해 무시할 만큼 작아질 때까지 급수를 더한다.
        for (int k = 1; k < 200; k++) {
            term *= q / ((double) k * k);
            sum += term;
            if (term < sum * 1e-17) {
                break;
            }
        }
        return sum;
    }

    /**
     * Designs the taps <code>g[0 .. P-1]</code> of the half-sample interpolator: <code>g[m]</code> is the weight of the two samples that lie
     * at the distance <code>m + 0.5</code> on both sides of the interpolated point. The taps sum to one half, so that the interpolation of
     * a constant is the constant.
     * <p>
     * 반 샘플 보간기의 탭 <code>g[0 .. P-1]</code> 을 설계한다. <code>g[m]</code> 은 보간하는 점의 양쪽에서 거리 <code>m + 0.5</code> 에 있는 두 샘플의
     * 가중치이다. 탭의 합은 1/2 이므로 상수의 보간은 그 상수이다.
     *
     * @param p the half length, the number of taps on each side / 반 길이, 한쪽의 탭 수
     * @return the taps / 탭
     */
    static double[] design(int p) {
        double[] g = new double[p];
        double norm = bessel0(KAISER_BETA);
        double sum = 0.0;
        // 반 샘플 위치마다 sinc 값에 Kaiser 창을 곱한다.
        for (int m = 0; m < p; m++) {
            double x = m + 0.5;
            double sinc = ((m & 1) == 0 ? 1.0 : -1.0) / (Math.PI * x);
            double r = x / p;
            double w = bessel0(KAISER_BETA * Math.sqrt(Math.max(0.0, 1.0 - r * r))) / norm;
            g[m] = sinc * w;
            sum += g[m];
        }
        // 양쪽 탭의 합이 1 이 되도록(한쪽 합이 1/2) 크기를 맞춘다.
        for (int m = 0; m < p; m++) {
            g[m] = g[m] * 0.5 / sum;
        }
        return g;
    }
}
