package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The inverse MDCT of the CELT decoder (RFC 6716 section 4.3.7): a mixed-radix (2, 3, 4, 5) inverse FFT of N/4 complex points with
 * pre- and post-rotation, followed by the windowed overlap-add of the Vorbis power-complementary window. It is a translation of the
 * KISS-FFT based routine of the reference decoder, in single-precision floating point like the reference "float" build. The
 * twiddle factors are computed at construction time with double precision and rounded to float.
 * <p>
 * CELT 디코더의 역 MDCT(RFC 6716 4.3.7): N/4 개 복소수 점에 대한 혼합 기수(2, 3, 4, 5) 역 FFT 에 앞뒤 회전을 더하고, Vorbis 전력 상보 창으로
 * 겹쳐 더한다. 참조 디코더의 KISS-FFT 기반 루틴을 참조 "float" 빌드처럼 단정밀도 부동소수점으로 옮긴 것이다. 트위들 계수는 생성 시 배정밀도로
 * 계산해 float 로 반올림한다.
 */
final class CeltMdct {

    /**
     * Largest number of factors of an FFT size.
     * <p>
     * FFT 크기의 인수 개수 상한.
     */
    private static final int MAX_FACTORS = 8;

    /**
     * Single precision pi as the reference decoder defines it.
     * <p>
     * 참조 디코더가 정의한 단정밀도 원주율.
     */
    private static final float PI = 3.141592653f;

    /**
     * Size of the MDCT window in samples (twice the number of coefficients of the longest block).
     * <p>
     * MDCT 창의 크기(가장 긴 블록의 계수 개수의 두 배).
     */
    private final int n;

    /**
     * Cosine table cos(2*pi*i/n) for i = 0..n/4.
     * <p>
     * 코사인 표 cos(2*pi*i/n) (i = 0..n/4).
     */
    private final float[] trig;

    /**
     * FFT states of the sizes n/4, n/8, ... (one per shift).
     * <p>
     * 크기 n/4, n/8, ... 의 FFT 상태(shift 마다 하나).
     */
    private final Fft[] ffts;

    /**
     * Scratch buffers of the transform.
     * <p>
     * 변환의 작업 버퍼.
     */
    private final float[] bufF;

    /**
     * Second scratch buffer.
     * <p>
     * 두 번째 작업 버퍼.
     */
    private final float[] bufF2;

    /**
     * Creates the transform for a window size.
     * <p>
     * 창 크기에 대한 변환을 만든다.
     *
     * @param n        window size in samples (1920 for CELT at 48 kHz) / 창 크기(48 kHz 의 CELT 는 1920)
     * @param maxShift number of additional halvings supported (3 for CELT) / 지원하는 추가 절반 분할 횟수(CELT 는 3)
     */
    CeltMdct(int n, int maxShift) {
        this.n = n;
        int n4 = n >> 2;
        this.ffts = new Fft[maxShift + 1];
        // 기본 크기와 그 절반 크기들의 FFT 상태를 만든다(트위들은 기본 크기의 것을 공유한다).
        for (int i = 0; i <= maxShift; i++) {
            ffts[i] = new Fft(n4 >> i, i == 0 ? null : ffts[0]);
        }
        this.trig = new float[n4 + 1];
        // 코사인 표를 배정밀도로 계산해 float 로 반올림한다.
        for (int i = 0; i <= n4; i++) {
            trig[i] = (float) Math.cos(2 * Math.PI * i / n);
        }
        this.bufF = new float[n >> 1];
        this.bufF2 = new float[n >> 1];
    }

    /**
     * Inverse MDCT of one block with windowed overlap-add into the output.
     * <p>
     * 블록 하나의 역 MDCT 를 하고 출력에 창을 씌워 겹쳐 더한다.
     *
     * @param in      coefficients (interleaved by <code>stride</code>) / 계수(<code>stride</code> 간격으로 인터리브)
     * @param inPos   index of the first coefficient / 첫 계수의 위치
     * @param out     output samples / 출력 샘플
     * @param outPos  index of the output block / 출력 블록의 위치
     * @param window  the overlap window / 겹침 창
     * @param overlap number of overlapping samples / 겹치는 샘플 수
     * @param shift   halving count (0 for the longest block) / 절반 분할 횟수(가장 긴 블록은 0)
     * @param stride  distance of consecutive coefficients in <code>in</code> / 연속한 계수 사이의 간격
     */
    void backward(float[] in, int inPos, float[] out, int outPos, float[] window, int overlap, int shift, int stride) {
        int nn = n >> shift;
        int n2 = nn >> 1;
        int n4 = nn >> 2;
        float[] f = bufF;
        float[] f2 = bufF2;
        float sine = ((2.0f * PI) * 0.125f) / nn;
        int xp1 = inPos;
        int xp2 = inPos + stride * (n2 - 1);
        // 계수를 앞뒤 쌍으로 묶어 회전한다(사전 회전).
        for (int i = 0; i < n4; i++) {
            float yr = -(in[xp2] * trig[i << shift]) + in[xp1] * trig[(n4 - i) << shift];
            float yi = -(in[xp2] * trig[(n4 - i) << shift]) - in[xp1] * trig[i << shift];
            f2[2 * i] = yr - yi * sine;
            f2[2 * i + 1] = yi + yr * sine;
            xp1 += 2 * stride;
            xp2 -= 2 * stride;
        }
        ffts[shift].ifft(f2, f);
        // 역 FFT 결과를 다시 회전한다(사후 회전).
        for (int i = 0; i < n4; i++) {
            float re = f[2 * i];
            float im = f[2 * i + 1];
            float yr = re * trig[i << shift] - im * trig[(n4 - i) << shift];
            float yi = im * trig[i << shift] + re * trig[(n4 - i) << shift];
            f[2 * i] = yr - yi * sine;
            f[2 * i + 1] = yi + yr * sine;
        }
        // 시간 영역 순서로 재배열한다.
        for (int i = 0; i < n4; i++) {
            f2[2 * i] = -f[2 * i];
            f2[2 * i + 1] = f[n2 - 1 - 2 * i];
        }
        int base = outPos - ((n2 - overlap) >> 1);
        int fp1 = n4 - 1;
        int xo1 = base + n2 - 1;
        int yo1 = base + n4 - overlap / 2;
        int wp1 = 0;
        int wp2 = overlap - 1;
        int i;
        // 창의 영향을 받지 않는 앞쪽 절반의 샘플을 그대로 복사한다.
        for (i = 0; i < n4 - overlap / 2; i++) {
            out[xo1] = f2[fp1];
            xo1--;
            fp1--;
        }
        // 겹침 구간에서는 창을 곱해 기존 값에 더한다.
        for (; i < n4; i++) {
            float x1 = f2[fp1--];
            out[yo1] += -(window[wp1] * x1);
            yo1++;
            out[xo1] += window[wp2] * x1;
            xo1--;
            wp1++;
            wp2--;
        }
        int fp2 = n4;
        int xo2 = base + n2;
        int yo2 = base + nn - 1 - (n4 - overlap / 2);
        wp1 = 0;
        wp2 = overlap - 1;
        // 뒤쪽 절반의 겹침 없는 샘플을 그대로 복사한다.
        for (i = 0; i < n4 - overlap / 2; i++) {
            out[xo2] = f2[fp2];
            xo2++;
            fp2++;
        }
        // 뒤쪽 겹침 구간은 창을 곱해 대입한다(다음 블록이 더해진다).
        for (; i < n4; i++) {
            float x2 = f2[fp2++];
            out[yo2] = window[wp1] * x2;
            yo2--;
            out[xo2] = window[wp2] * x2;
            xo2++;
            wp1++;
            wp2--;
        }
    }

    /**
     * Mixed-radix inverse FFT state: factors, bit reversal table and a view of the twiddle table.
     * <p>
     * 혼합 기수 역 FFT 의 상태: 인수, 비트 반전 표, 트위들 표에 대한 참조.
     */
    private static final class Fft {

        /**
         * Number of points.
         * <p>
         * 점의 개수.
         */
        private final int nfft;

        /**
         * Shift of the twiddle stride relative to the base FFT (0 for the base).
         * <p>
         * 기본 FFT 에 대한 트위들 간격의 shift(기본은 0).
         */
        private final int shift;

        /**
         * Factor pairs (radix, remaining size).
         * <p>
         * 인수 쌍(기수, 남은 크기).
         */
        private final int[] factors = new int[2 * MAX_FACTORS];

        /**
         * Bit reversal permutation.
         * <p>
         * 비트 반전 순열.
         */
        private final int[] bitrev;

        /**
         * Twiddles (cos, sin interleaved) of the base FFT, shared by the smaller sizes.
         * <p>
         * 기본 FFT 의 트위들(cos, sin 인터리브). 작은 크기가 공유한다.
         */
        private final float[] twiddles;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param nfft number of points / 점의 개수
         * @param base the base FFT whose twiddles are shared, or null to make own twiddles / 트위들을 공유할 기본 FFT. 없으면 null
         */
        Fft(int nfft, Fft base) {
            this.nfft = nfft;
            if (base != null) {
                this.twiddles = base.twiddles;
                int s = 0;
                // 기본 크기가 되도록 몇 번 두 배 해야 하는지 센다.
                while ((nfft << s) != base.nfft && s < 32) {
                    s++;
                }
                this.shift = s;
            } else {
                this.twiddles = new float[2 * nfft];
                // 단위원 위의 트위들 계수를 배정밀도로 계산한다.
                for (int i = 0; i < nfft; i++) {
                    double phase = (-2 * Math.PI / nfft) * i;
                    twiddles[2 * i] = (float) Math.cos(phase);
                    twiddles[2 * i + 1] = (float) Math.sin(phase);
                }
                this.shift = -1;
            }
            if (!factor(nfft, factors)) {
                throw new IllegalArgumentException("unsupported FFT size " + nfft);
            }
            this.bitrev = new int[nfft];
            bitrevTable(0, 0, 1, 1, 0);
        }

        /**
         * Factors a size into radices 4, 2, 3, 5.
         * <p>
         * 크기를 기수 4, 2, 3, 5 로 인수분해한다.
         *
         * @param size   the size / 크기
         * @param facbuf receives the pairs / 쌍을 받는 배열
         * @return false if the size has another prime factor / 다른 소인수가 있으면 false
         */
        private static boolean factor(int size, int[] facbuf) {
            int nn = size;
            int p = 4;
            int k = 0;
            // 나누어떨어지는 기수를 찾아 인수 쌍에 기록하며 1 이 될 때까지 반복한다.
            do {
                // 현재 기수로 나누어떨어지지 않으면 다음 기수를 시도한다.
                while (nn % p != 0) {
                    switch (p) {
                    case 4:
                        p = 2;
                        break;
                    case 2:
                        p = 3;
                        break;
                    default:
                        p += 2;
                        break;
                    }
                    if (p > 32000 || p * p > nn) {
                        p = nn;
                    }
                }
                nn /= p;
                if (p > 5 || k >= 2 * MAX_FACTORS) {
                    return false;
                }
                facbuf[k++] = p;
                facbuf[k++] = nn;
            } while (nn > 1);
            return true;
        }

        /**
         * Fills the bit reversal table recursively.
         * <p>
         * 비트 반전 표를 재귀적으로 채운다.
         *
         * @param fout     first output index of this level / 이 단계의 첫 출력 위치
         * @param f        index in the table / 표의 위치
         * @param fstride  stride of the level / 단계의 간격
         * @param inStride input stride / 입력 간격
         * @param level    index of the factor pair / 인수 쌍의 번호
         */
        private void bitrevTable(int fout, int f, int fstride, int inStride, int level) {
            int p = factors[2 * level];
            int m = factors[2 * level + 1];
            int fo = fout;
            int fi = f;
            if (m == 1) {
                // 마지막 단계에서는 출력 위치를 그대로 기록한다.
                for (int j = 0; j < p; j++) {
                    bitrev[fi] = fo + j;
                    fi += fstride * inStride;
                }
            } else {
                // 아래 단계로 재귀하며 출력 위치를 m 만큼씩 옮긴다.
                for (int j = 0; j < p; j++) {
                    bitrevTable(fo, fi, fstride * p, inStride, level + 1);
                    fi += fstride * inStride;
                    fo += m;
                }
            }
        }

        /**
         * Inverse FFT of <code>nfft</code> complex points (no scaling).
         * <p>
         * 복소수 <code>nfft</code> 개의 역 FFT(스케일링 없음).
         *
         * @param fin  input (re, im interleaved) / 입력(re, im 인터리브)
         * @param fout output (re, im interleaved), a different array / 출력(re, im 인터리브). 다른 배열
         */
        void ifft(float[] fin, float[] fout) {
            int[] fstride = new int[MAX_FACTORS + 1];
            int sh = shift > 0 ? shift : 0;
            // 입력을 비트 반전 순서로 복사한다.
            for (int i = 0; i < nfft; i++) {
                int d = 2 * bitrev[i];
                fout[d] = fin[2 * i];
                fout[d + 1] = fin[2 * i + 1];
            }
            fstride[0] = 1;
            int l = 0;
            int m;
            // 인수 쌍을 따라 단계별 간격을 계산한다.
            do {
                int p = factors[2 * l];
                m = factors[2 * l + 1];
                fstride[l + 1] = fstride[l] * p;
                l++;
            } while (m != 1);
            m = factors[2 * l - 1];
            // 마지막 단계부터 첫 단계까지 나비 연산을 적용한다.
            for (int i = l - 1; i >= 0; i--) {
                int m2 = i != 0 ? factors[2 * i - 1] : 1;
                switch (factors[2 * i]) {
                case 2:
                    bfly2(fout, fstride[i] << sh, m, fstride[i], m2);
                    break;
                case 4:
                    bfly4(fout, fstride[i] << sh, m, fstride[i], m2);
                    break;
                case 3:
                    bfly3(fout, fstride[i] << sh, m, fstride[i], m2);
                    break;
                case 5:
                    bfly5(fout, fstride[i] << sh, m, fstride[i], m2);
                    break;
                default:
                    break;
                }
                m = m2;
            }
        }

        /**
         * Radix-2 inverse butterfly.
         * <p>
         * 기수 2 역 나비 연산.
         *
         * @param f       the data / 데이터
         * @param fstride twiddle stride / 트위들 간격
         * @param m       sub-transform size / 부분 변환 크기
         * @param count   number of groups / 그룹 수
         * @param mm      group distance / 그룹 간 거리
         */
        private void bfly2(float[] f, int fstride, int m, int count, int mm) {
            // 그룹마다 처리한다.
            for (int i = 0; i < count; i++) {
                int o1 = 2 * (i * mm);
                int o2 = o1 + 2 * m;
                int tw = 0;
                // 그룹 안의 점마다 처리한다.
                for (int j = 0; j < m; j++) {
                    float tr = f[o2] * twiddles[tw] + f[o2 + 1] * twiddles[tw + 1];
                    float ti = f[o2 + 1] * twiddles[tw] - f[o2] * twiddles[tw + 1];
                    tw += 2 * fstride;
                    f[o2] = f[o1] - tr;
                    f[o2 + 1] = f[o1 + 1] - ti;
                    f[o1] += tr;
                    f[o1 + 1] += ti;
                    o1 += 2;
                    o2 += 2;
                }
            }
        }

        /**
         * Radix-4 inverse butterfly.
         * <p>
         * 기수 4 역 나비 연산.
         *
         * @param f       the data / 데이터
         * @param fstride twiddle stride / 트위들 간격
         * @param m       sub-transform size / 부분 변환 크기
         * @param count   number of groups / 그룹 수
         * @param mm      group distance / 그룹 간 거리
         */
        private void bfly4(float[] f, int fstride, int m, int count, int mm) {
            int m2 = 2 * m;
            int m3 = 3 * m;
            // 그룹마다 처리한다.
            for (int i = 0; i < count; i++) {
                int o = 2 * (i * mm);
                int t1 = 0;
                int t2 = 0;
                int t3 = 0;
                // 그룹 안의 점마다 처리한다.
                for (int j = 0; j < m; j++) {
                    int a = o + 2 * m;
                    int b = o + 2 * m2;
                    int c = o + 2 * m3;
                    float s0r = f[a] * twiddles[t1] + f[a + 1] * twiddles[t1 + 1];
                    float s0i = f[a + 1] * twiddles[t1] - f[a] * twiddles[t1 + 1];
                    float s1r = f[b] * twiddles[t2] + f[b + 1] * twiddles[t2 + 1];
                    float s1i = f[b + 1] * twiddles[t2] - f[b] * twiddles[t2 + 1];
                    float s2r = f[c] * twiddles[t3] + f[c + 1] * twiddles[t3 + 1];
                    float s2i = f[c + 1] * twiddles[t3] - f[c] * twiddles[t3 + 1];
                    float s5r = f[o] - s1r;
                    float s5i = f[o + 1] - s1i;
                    f[o] += s1r;
                    f[o + 1] += s1i;
                    float s3r = s0r + s2r;
                    float s3i = s0i + s2i;
                    float s4r = s0r - s2r;
                    float s4i = s0i - s2i;
                    f[b] = f[o] - s3r;
                    f[b + 1] = f[o + 1] - s3i;
                    t1 += 2 * fstride;
                    t2 += 2 * fstride * 2;
                    t3 += 2 * fstride * 3;
                    f[o] += s3r;
                    f[o + 1] += s3i;
                    f[a] = s5r - s4i;
                    f[a + 1] = s5i + s4r;
                    f[c] = s5r + s4i;
                    f[c + 1] = s5i - s4r;
                    o += 2;
                }
            }
        }

        /**
         * Radix-3 inverse butterfly.
         * <p>
         * 기수 3 역 나비 연산.
         *
         * @param f       the data / 데이터
         * @param fstride twiddle stride / 트위들 간격
         * @param m       sub-transform size / 부분 변환 크기
         * @param count   number of groups / 그룹 수
         * @param mm      group distance / 그룹 간 거리
         */
        private void bfly3(float[] f, int fstride, int m, int count, int mm) {
            int m2 = 2 * m;
            float epi3i = twiddles[2 * (fstride * m) + 1];
            // 그룹마다 처리한다.
            for (int i = 0; i < count; i++) {
                int o = 2 * (i * mm);
                int t1 = 0;
                int t2 = 0;
                // 그룹 안의 점마다 처리한다.
                for (int k = 0; k < m; k++) {
                    int a = o + 2 * m;
                    int b = o + 2 * m2;
                    float s1r = f[a] * twiddles[t1] + f[a + 1] * twiddles[t1 + 1];
                    float s1i = f[a + 1] * twiddles[t1] - f[a] * twiddles[t1 + 1];
                    float s2r = f[b] * twiddles[t2] + f[b + 1] * twiddles[t2 + 1];
                    float s2i = f[b + 1] * twiddles[t2] - f[b] * twiddles[t2 + 1];
                    float s3r = s1r + s2r;
                    float s3i = s1i + s2i;
                    float s0r = s1r - s2r;
                    float s0i = s1i - s2i;
                    t1 += 2 * fstride;
                    t2 += 2 * fstride * 2;
                    f[a] = f[o] - s3r * 0.5f;
                    f[a + 1] = f[o + 1] - s3i * 0.5f;
                    s0r *= -epi3i;
                    s0i *= -epi3i;
                    f[o] += s3r;
                    f[o + 1] += s3i;
                    f[b] = f[a] + s0i;
                    f[b + 1] = f[a + 1] - s0r;
                    f[a] -= s0i;
                    f[a + 1] += s0r;
                    o += 2;
                }
            }
        }

        /**
         * Radix-5 inverse butterfly.
         * <p>
         * 기수 5 역 나비 연산.
         *
         * @param f       the data / 데이터
         * @param fstride twiddle stride / 트위들 간격
         * @param m       sub-transform size / 부분 변환 크기
         * @param count   number of groups / 그룹 수
         * @param mm      group distance / 그룹 간 거리
         */
        private void bfly5(float[] f, int fstride, int m, int count, int mm) {
            float yar = twiddles[2 * (fstride * m)];
            float yai = twiddles[2 * (fstride * m) + 1];
            float ybr = twiddles[2 * (fstride * 2 * m)];
            float ybi = twiddles[2 * (fstride * 2 * m) + 1];
            // 그룹마다 처리한다.
            for (int i = 0; i < count; i++) {
                int o0 = 2 * (i * mm);
                int o1 = o0 + 2 * m;
                int o2 = o0 + 4 * m;
                int o3 = o0 + 6 * m;
                int o4 = o0 + 8 * m;
                // 그룹 안의 점마다 처리한다.
                for (int u = 0; u < m; u++) {
                    float s0r = f[o0];
                    float s0i = f[o0 + 1];
                    int w1 = 2 * (u * fstride);
                    int w2 = 2 * (2 * u * fstride);
                    int w3 = 2 * (3 * u * fstride);
                    int w4 = 2 * (4 * u * fstride);
                    float s1r = f[o1] * twiddles[w1] + f[o1 + 1] * twiddles[w1 + 1];
                    float s1i = f[o1 + 1] * twiddles[w1] - f[o1] * twiddles[w1 + 1];
                    float s2r = f[o2] * twiddles[w2] + f[o2 + 1] * twiddles[w2 + 1];
                    float s2i = f[o2 + 1] * twiddles[w2] - f[o2] * twiddles[w2 + 1];
                    float s3r = f[o3] * twiddles[w3] + f[o3 + 1] * twiddles[w3 + 1];
                    float s3i = f[o3 + 1] * twiddles[w3] - f[o3] * twiddles[w3 + 1];
                    float s4r = f[o4] * twiddles[w4] + f[o4 + 1] * twiddles[w4 + 1];
                    float s4i = f[o4 + 1] * twiddles[w4] - f[o4] * twiddles[w4 + 1];
                    float s7r = s1r + s4r;
                    float s7i = s1i + s4i;
                    float s10r = s1r - s4r;
                    float s10i = s1i - s4i;
                    float s8r = s2r + s3r;
                    float s8i = s2i + s3i;
                    float s9r = s2r - s3r;
                    float s9i = s2i - s3i;
                    f[o0] += s7r + s8r;
                    f[o0 + 1] += s7i + s8i;
                    float s5r = s0r + s7r * yar + s8r * ybr;
                    float s5i = s0i + s7i * yar + s8i * ybr;
                    float s6r = -(s10i * yai) - s9i * ybi;
                    float s6i = s10r * yai + s9r * ybi;
                    f[o1] = s5r - s6r;
                    f[o1 + 1] = s5i - s6i;
                    f[o4] = s5r + s6r;
                    f[o4 + 1] = s5i + s6i;
                    float s11r = s0r + s7r * ybr + s8r * yar;
                    float s11i = s0i + s7i * ybr + s8i * yar;
                    float s12r = s10i * ybi - s9i * yai;
                    float s12i = -(s10r * ybi) + s9r * yai;
                    f[o2] = s11r + s12r;
                    f[o2 + 1] = s11i + s12i;
                    f[o3] = s11r - s12r;
                    f[o3 + 1] = s11i - s12i;
                    o0 += 2;
                    o1 += 2;
                    o2 += 2;
                    o3 += 2;
                    o4 += 2;
                }
            }
        }
    }
}
