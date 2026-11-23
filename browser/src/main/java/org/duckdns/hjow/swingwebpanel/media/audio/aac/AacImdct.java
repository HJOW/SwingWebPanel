package org.duckdns.hjow.swingwebpanel.media.audio.aac;

/**
 * The inverse MDCT of AAC for one transform length N: <code>y[n] = sum over k of X[k] cos(2 pi / N (n + 1/2 + N/4)(k + 1/2))</code> for n
 * below N and k below N/2, without scaling (the caller multiplies by 2/N as ISO/IEC 14496-3 4.6.11 defines). The kernel is the same as the one
 * of the Vorbis IMDCT, so the O(N log N) algorithm (a DCT-IV of size N/2 through a complex FFT of size N/4) is the one verified for Vorbis in
 * <code>org.duckdns.hjow.swingwebpanel.media.audio.vorbis.Imdct</code>, adapted here because that class is package-private.
 * <p>
 * 변환 길이 N 에 대한 AAC 의 역 MDCT: <code>y[n] = X[k] cos(2 pi / N (n + 1/2 + N/4)(k + 1/2))</code> 을 k 에 대해 합한 값(n 은 N 미만, k 는 N/2
 * 미만). 스케일링은 하지 않는다(ISO/IEC 14496-3 4.6.11 의 2/N 은 호출자가 곱한다). 커널이 Vorbis IMDCT 와 같으므로 O(N log N) 알고리즘(크기 N/4
 * 복소수 FFT 로 구하는 크기 N/2 DCT-IV)은 <code>org.duckdns.hjow.swingwebpanel.media.audio.vorbis.Imdct</code> 에서 검증된 것을 쓰며, 그 클래스가 패키지 비공개이므로
 * 여기에 옮겨 적었다.
 */
final class AacImdct {

    /** Transform length N. / 변환 길이 N. */
    private final int n;
    /** Number of coefficients M = N/2. / 계수 수 M = N/2. */
    private final int m;
    /** FFT size M/2. / FFT 크기 M/2. */
    private final int h;
    /** Pre-twiddle cosines. / 전처리 코사인. */
    private final double[] preCos;
    /** Pre-twiddle sines. / 전처리 사인. */
    private final double[] preSin;
    /** Post-twiddle cosines. / 후처리 코사인. */
    private final double[] postCos;
    /** Post-twiddle sines. / 후처리 사인. */
    private final double[] postSin;
    /** FFT twiddle cosines (first half). / FFT 회전 코사인(앞 절반). */
    private final double[] fftCos;
    /** FFT twiddle sines (first half). / FFT 회전 사인(앞 절반). */
    private final double[] fftSin;
    /** Bit reversal permutation. / 비트 역순 순열. */
    private final int[] reverse;
    /** FFT real part. / FFT 실수부. */
    private final double[] re;
    /** FFT imaginary part. / FFT 허수부. */
    private final double[] im;
    /** DCT-IV result. / DCT-IV 결과. */
    private final double[] u;

    /**
     * Creates the transform.
     * <p>
     * 변환을 만든다.
     *
     * @param n transform length (a power of two, at least 16) / 변환 길이(2 의 거듭제곱, 16 이상)
     */
    AacImdct(int n) {
        this.n = n;
        this.m = n / 2;
        this.h = m / 2;
        preCos = new double[h];
        preSin = new double[h];
        postCos = new double[h];
        postSin = new double[h];
        for (int j = 0; j < h; j++) { // 전처리와 후처리 회전 계수를 만든다
            preCos[j] = Math.cos(Math.PI * j / m);
            preSin[j] = -Math.sin(Math.PI * j / m);
            postCos[j] = Math.cos(Math.PI * (j + 0.25) / m);
            postSin[j] = -Math.sin(Math.PI * (j + 0.25) / m);
        }
        fftCos = new double[h / 2 + 1];
        fftSin = new double[h / 2 + 1];
        for (int k = 0; k <= h / 2; k++) { // FFT 회전 계수를 만든다
            fftCos[k] = Math.cos(2 * Math.PI * k / h);
            fftSin[k] = -Math.sin(2 * Math.PI * k / h);
        }
        reverse = new int[h];
        int bits = Integer.numberOfTrailingZeros(h);
        for (int i = 0; i < h; i++) { // 비트 역순 순열을 만든다
            reverse[i] = bits == 0 ? 0 : Integer.reverse(i) >>> (32 - bits);
        }
        re = new double[h];
        im = new double[h];
        u = new double[m];
    }

    /**
     * Computes the unscaled IMDCT.
     * <p>
     * 스케일링 없는 IMDCT 를 계산한다.
     *
     * @param x   N/2 coefficients / 계수 N/2개
     * @param xo  first coefficient / 첫 계수 위치
     * @param out N output samples / 출력 N개
     */
    void inverse(float[] x, int xo, float[] out) {
        for (int j = 0; j < h; j++) { // 짝수/홀수 위치의 계수를 복소수로 묶어 전처리 회전을 곱한다
            double a = x[xo + 2 * j];
            double b = x[xo + m - 1 - 2 * j];
            int k = reverse[j];
            re[k] = a * preCos[j] - b * preSin[j];
            im[k] = a * preSin[j] + b * preCos[j];
        }
        fft();
        for (int p = 0; p < h; p++) { // 후처리 회전을 곱해 DCT-IV 결과를 놓는다
            double dr = re[p] * postCos[p] - im[p] * postSin[p];
            double di = re[p] * postSin[p] + im[p] * postCos[p];
            u[2 * p] = dr;
            u[m - 1 - 2 * p] = -di;
        }
        int quarter = m / 2;
        for (int i = 0; i < n; i++) { // DCT-IV 결과를 IMDCT 의 N 개 출력으로 펼친다
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
     * In-place radix-2 FFT on the bit reversed input. / 비트 역순 입력에 대한 제자리 기수 2 FFT.
     */
    private void fft() {
        for (int size = 2; size <= h; size <<= 1) { // 구간 크기를 2, 4, 8 ... 로 늘리며 나비 연산을 한다
            int half = size >> 1;
            int step = h / size;
            for (int start = 0; start < h; start += size) { // 구간마다 나비 연산을 한다
                for (int k = 0; k < half; k++) { // 구간 안의 쌍마다 계산한다
                    int ti = k * step;
                    double wr = ti <= h / 2 ? fftCos[ti] : fftCos[h - ti];
                    double wi = ti <= h / 2 ? fftSin[ti] : -fftSin[h - ti];
                    int a = start + k;
                    int b = a + half;
                    double tr = re[b] * wr - im[b] * wi;
                    double tm = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - tr;
                    im[b] = im[a] - tm;
                    re[a] += tr;
                    im[a] += tm;
                }
            }
        }
    }
}
