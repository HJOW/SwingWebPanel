package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * An in-place complex FFT of a power-of-two size (radix 2, iterative). Neither direction is normalized: the forward transform computes
 * <code>X[k] = sum x[n] e^(-i 2 pi k n / N)</code> and the inverse transform computes <code>x[n] = sum X[k] e^(+i 2 pi k n / N)</code>,
 * so the caller divides by <code>N</code> when it needs a round trip. The instance holds only the tables of its size and can be used
 * from one thread at a time.
 * <p>
 * 2의 거듭제곱 크기의 제자리 복소 FFT(기수 2, 반복형). 어느 방향도 정규화하지 않는다: 순방향은 <code>X[k] = sum x[n] e^(-i 2 pi k n / N)</code> 를,
 * 역방향은 <code>x[n] = sum X[k] e^(+i 2 pi k n / N)</code> 를 계산하므로 왕복이 필요하면 호출자가 <code>N</code> 으로 나눈다. 인스턴스는 자기 크기의
 * 표만 가지며 한 번에 한 스레드에서 쓸 수 있다.
 */
public final class Fft {

    /**
     * The size of the transform.
     * <p>
     * 변환 크기.
     */
    private final int n;

    /**
     * The cosines of the twiddle factors, <code>cos(2 pi k / N)</code> for <code>k</code> below <code>N/2</code>.
     * <p>
     * 회전 인자의 코사인 <code>cos(2 pi k / N)</code> (<code>k</code> 는 <code>N/2</code> 미만).
     */
    private final double[] cos;

    /**
     * The sines of the twiddle factors, <code>sin(2 pi k / N)</code> for <code>k</code> below <code>N/2</code>.
     * <p>
     * 회전 인자의 사인 <code>sin(2 pi k / N)</code> (<code>k</code> 는 <code>N/2</code> 미만).
     */
    private final double[] sin;

    /**
     * The bit reversed index of every position.
     * <p>
     * 위치마다의 비트 반전 인덱스.
     */
    private final int[] reversed;

    /**
     * Creates a transform of a size.
     * <p>
     * 크기를 정해 변환을 만든다.
     *
     * @param size the size, a power of two of at least 2 / 크기(2 이상의 2의 거듭제곱)
     */
    public Fft(int size) {
        if (size < 2 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("size " + size + " is not a power of two");
        }
        this.n = size;
        this.cos = new double[size / 2];
        this.sin = new double[size / 2];
        // 회전 인자 표를 만든다.
        for (int k = 0; k < size / 2; k++) {
            cos[k] = Math.cos(2 * Math.PI * k / size);
            sin[k] = Math.sin(2 * Math.PI * k / size);
        }
        this.reversed = new int[size];
        int bits = Integer.numberOfTrailingZeros(size);
        // 위치마다 비트를 뒤집은 인덱스를 구한다.
        for (int i = 0; i < size; i++) {
            reversed[i] = Integer.reverse(i) >>> (32 - bits);
        }
    }

    /**
     * Returns the size of the transform.
     * <p>
     * 변환 크기를 반환한다.
     *
     * @return the size / 크기
     */
    public int getSize() {
        return n;
    }

    /**
     * Transforms the arrays in place.
     * <p>
     * 배열을 제자리에서 변환한다.
     *
     * @param re      the real parts, at least <code>N</code> long / 실수부(<code>N</code> 이상)
     * @param im      the imaginary parts, at least <code>N</code> long / 허수부(<code>N</code> 이상)
     * @param inverse true for the inverse transform (the sign of the exponent is positive) / 역변환이면 true(지수의 부호가 양수)
     */
    public void transform(double[] re, double[] im, boolean inverse) {
        // 비트 반전 순서로 재배열한다.
        for (int i = 0; i < n; i++) {
            int j = reversed[i];
            if (j > i) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        double sign = inverse ? 1.0 : -1.0;
        // 길이 2, 4, 8, ... 의 나비 연산을 차례로 한다.
        for (int len = 2; len <= n; len <<= 1) {
            int half = len >> 1;
            int step = n / len;
            // 구간 안의 나비 연산을 계산한다.
            for (int start = 0; start < n; start += len) {
                // 나비 연산의 두 점을 회전 인자로 계산한다.
                for (int k = 0; k < half; k++) {
                    double wr = cos[k * step];
                    double wi = sign * sin[k * step];
                    int a = start + k;
                    int b = a + half;
                    double tr = re[b] * wr - im[b] * wi;
                    double ti = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - ti;
                    re[a] += tr;
                    im[a] += ti;
                }
            }
        }
    }
}
