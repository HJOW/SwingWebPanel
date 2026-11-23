package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

/**
 * The inverse modified discrete cosine transform of Vorbis for one block size N: <code>y[n] = sum over k of X[k] cos(2 pi / N (n + 1/2 +
 * N/4)(k + 1/2))</code> for n below N and k below N/2, without scaling (the Vorbis window and the encoder's forward scaling of 4/N make the
 * overlapped blocks add up to the signal). It is computed in O(N log N) through a DCT-IV of size N/2 that uses a complex FFT of size N/4.
 * <p>
 * 블록 크기 N 에 대한 Vorbis 의 역 수정 이산 코사인 변환: <code>y[n] = X[k] cos(2 pi / N (n + 1/2 + N/4)(k + 1/2))</code> 을 k 에 대해 합한 값
 * (n 은 N 미만, k 는 N/2 미만). 스케일링은 하지 않는다(Vorbis 창과 인코더의 순방향 4/N 스케일링으로 겹친 블록이 신호가 된다). 크기 N/4 의 복소수
 * FFT 를 쓰는 크기 N/2 의 DCT-IV 로 O(N log N) 에 계산한다.
 */
final class Imdct {

    /**
     * Block size N.
     * <p>
     * 블록 크기 N.
     */
    private final int n;

    /**
     * Number of coefficients M = N/2.
     * <p>
     * 계수 수 M = N/2.
     */
    private final int m;

    /**
     * Size of the FFT, M/2.
     * <p>
     * FFT 의 크기 M/2.
     */
    private final int h;

    /**
     * Cosine of the pre-twiddle.
     * <p>
     * 전처리 회전의 코사인.
     */
    private final double[] preCos;

    /**
     * Sine of the pre-twiddle.
     * <p>
     * 전처리 회전의 사인.
     */
    private final double[] preSin;

    /**
     * Cosine of the post-twiddle.
     * <p>
     * 후처리 회전의 코사인.
     */
    private final double[] postCos;

    /**
     * Sine of the post-twiddle.
     * <p>
     * 후처리 회전의 사인.
     */
    private final double[] postSin;

    /**
     * Cosine of the FFT twiddle factors.
     * <p>
     * FFT 회전 계수의 코사인.
     */
    private final double[] fftCos;

    /**
     * Sine of the FFT twiddle factors.
     * <p>
     * FFT 회전 계수의 사인.
     */
    private final double[] fftSin;

    /**
     * Bit reversal permutation of the FFT.
     * <p>
     * FFT 의 비트 역순 순열.
     */
    private final int[] reverse;

    /**
     * Real parts of the FFT data.
     * <p>
     * FFT 데이터의 실수부.
     */
    private final double[] re;

    /**
     * Imaginary parts of the FFT data.
     * <p>
     * FFT 데이터의 허수부.
     */
    private final double[] im;

    /**
     * Result of the DCT-IV.
     * <p>
     * DCT-IV 의 결과.
     */
    private final double[] u;

    /**
     * Creates the transform.
     * <p>
     * 변환을 만든다.
     *
     * @param n block size, a power of two of at least 16 / 블록 크기. 16 이상의 2의 거듭제곱
     */
    Imdct(int n) {
        this.n = n;
        this.m = n / 2;
        this.h = m / 2;
        preCos = new double[h];
        preSin = new double[h];
        postCos = new double[h];
        postSin = new double[h];
        // 전처리 e^{-i pi j / M} 와 후처리 e^{-i pi (p + 1/4) / M} 계수를 만든다.
        for (int j = 0; j < h; j++) {
            preCos[j] = Math.cos(Math.PI * j / m);
            preSin[j] = -Math.sin(Math.PI * j / m);
            postCos[j] = Math.cos(Math.PI * (j + 0.25) / m);
            postSin[j] = -Math.sin(Math.PI * (j + 0.25) / m);
        }
        fftCos = new double[h / 2 + 1];
        fftSin = new double[h / 2 + 1];
        // FFT 회전 계수 e^{-2 pi i k / H} 를 만든다.
        for (int k = 0; k <= h / 2; k++) {
            fftCos[k] = Math.cos(2 * Math.PI * k / h);
            fftSin[k] = -Math.sin(2 * Math.PI * k / h);
        }
        reverse = new int[h];
        int bits = Integer.numberOfTrailingZeros(h);
        // 비트 역순 순열을 만든다.
        for (int i = 0; i < h; i++) {
            reverse[i] = bits == 0 ? 0 : Integer.reverse(i) >>> (32 - bits);
        }
        re = new double[h];
        im = new double[h];
        u = new double[m];
    }

    /**
     * Computes the inverse transform.
     * <p>
     * 역변환을 계산한다.
     *
     * @param x   N/2 spectrum coefficients / N/2 개의 스펙트럼 계수
     * @param out receives N time samples / N 개의 시간 샘플을 받는다
     */
    void inverse(float[] x, float[] out) {
        // 짝수/홀수 위치의 계수를 복소수로 묶어 전처리 회전을 곱하고 FFT 입력 순서(비트 역순)로 넣는다.
        for (int j = 0; j < h; j++) {
            double a = x[2 * j];
            double b = x[m - 1 - 2 * j];
            int k = reverse[j];
            re[k] = a * preCos[j] - b * preSin[j];
            im[k] = a * preSin[j] + b * preCos[j];
        }
        fft();
        // 후처리 회전을 곱해 DCT-IV 결과를 짝수/홀수 위치에 나눠 놓는다.
        for (int p = 0; p < h; p++) {
            double dr = re[p] * postCos[p] - im[p] * postSin[p];
            double di = re[p] * postSin[p] + im[p] * postCos[p];
            u[2 * p] = dr;
            u[m - 1 - 2 * p] = -di;
        }
        int quarter = m / 2;
        // DCT-IV 결과를 IMDCT 의 N 개 출력으로 펼친다(대칭성으로 부호만 바뀐다).
        for (int i = 0; i < n; i++) {
            double v;
            if (i < quarter) {
                v = u[i + quarter];
            } else if (i < quarter + m) {
                v = -u[3 * quarter - 1 - i];
            } else {
                v = -u[i - 3 * quarter];
            }
            out[i] = (float) v;
        }
    }

    /**
     * In-place radix-2 FFT of the bit-reversed data in {@link #re} and {@link #im}.
     * <p>
     * {@link #re}, {@link #im} 에 비트 역순으로 들어 있는 데이터의 제자리 기수-2 FFT.
     */
    private void fft() {
        // 구간 크기를 2, 4, 8 ... 로 늘리며 나비 연산을 한다.
        for (int size = 2; size <= h; size <<= 1) {
            int half = size >> 1;
            int step = h / size;
            // 항목마다 반복해서 처리한다.
            for (int start = 0; start < h; start += size) {
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < half; k++) {
                    double wr = twiddleCos(k * step);
                    double wi = twiddleSin(k * step);
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

    /**
     * Cosine of the FFT twiddle factor with index <code>i</code> (below H), using the symmetry of the table.
     * <p>
     * 인덱스 <code>i</code>(H 미만)의 FFT 회전 계수의 코사인. 표의 대칭성을 이용한다.
     *
     * @param i index / 인덱스
     * @return the cosine / 코사인
     */
    private double twiddleCos(int i) {
        return i <= h / 2 ? fftCos[i] : fftCos[h - i];
    }

    /**
     * Sine of the FFT twiddle factor with index <code>i</code> (below H), using the symmetry of the table.
     * <p>
     * 인덱스 <code>i</code>(H 미만)의 FFT 회전 계수의 사인. 표의 대칭성을 이용한다.
     *
     * @param i index / 인덱스
     * @return the sine / 사인
     */
    private double twiddleSin(int i) {
        return i <= h / 2 ? fftSin[i] : -fftSin[h - i];
    }
}
