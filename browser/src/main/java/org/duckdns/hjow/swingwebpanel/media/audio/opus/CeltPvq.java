package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Pyramid vector quantization of the CELT decoder (RFC 6716 sections 4.3.4.1 to 4.3.4.3): the decoding of the combinatorial pulse
 * index into an integer pulse vector (<code>celt/cwrs.c</code>), the normalization of that vector to the unit sphere and the
 * spreading rotation (<code>celt/vq.c</code>), in the floating-point configuration of the reference decoder. The general
 * row-recurrence method of the reference is used for every vector size; the specialized closed forms for 2 to 4 dimensions of
 * the reference give identical results.
 * <p>
 * CELT 디코더의 피라미드 벡터 양자화(RFC 6716 4.3.4.1 ~ 4.3.4.3): 조합 펄스 색인을 정수 펄스 벡터로 디코딩하고(<code>celt/cwrs.c</code>),
 * 그 벡터를 단위 구로 정규화하며 퍼짐 회전을 한다(<code>celt/vq.c</code>). 참조 디코더의 부동소수점 설정을 따른다. 모든 벡터 크기에 참조의
 * 일반 점화식 방식을 쓰며, 참조의 2~4차원 전용 닫힌 식과 결과가 같다.
 */
final class CeltPvq {

    /**
     * Spreading mode: none.
     * <p>
     * 퍼짐 모드: 없음.
     */
    static final int SPREAD_NONE = 0;

    /**
     * Spreading mode: aggressive.
     * <p>
     * 퍼짐 모드: 강함.
     */
    static final int SPREAD_AGGRESSIVE = 3;

    /**
     * Spreading strength factors of the light, normal and aggressive modes.
     * <p>
     * 약함, 보통, 강함 모드의 퍼짐 세기 계수.
     */
    private static final int[] SPREAD_FACTOR = { 15, 10, 5 };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private CeltPvq() {
    }

    /**
     * Advances a row of the table of U(n, k) numbers to the next dimension.
     * <p>
     * U(n, k) 수 표의 한 행을 다음 차원으로 진행한다.
     *
     * @param u    the row (unsigned 32-bit values in longs) / 행(long 에 담은 부호 없는 32비트 값)
     * @param base index of the first element to advance / 진행할 첫 원소의 위치
     * @param len  number of elements / 원소 수
     * @param ui0v the value entering at the start / 처음에 들어가는 값
     */
    private static void unext(long[] u, int base, int len, long ui0v) {
        long ui0 = ui0v;
        int j = 1;
        // 이웃한 원소의 합으로 다음 차원의 행을 만든다.
        do {
            long ui1 = (u[base + j] + u[base + j - 1] + ui0) & 0xFFFFFFFFL;
            u[base + j - 1] = ui0;
            ui0 = ui1;
            j++;
        } while (j < len);
        u[base + j - 1] = ui0;
    }

    /**
     * Steps a row of U numbers back to the previous dimension.
     * <p>
     * U 수의 행을 이전 차원으로 되돌린다.
     *
     * @param u   the row / 행
     * @param n   number of elements / 원소 수
     * @param ui0v value entering at the start / 처음에 들어가는 값
     */
    private static void uprev(long[] u, int n, long ui0v) {
        long ui0 = ui0v;
        int j = 1;
        // 이웃한 원소의 차로 이전 차원의 행을 복원한다.
        do {
            long ui1 = (u[j] - u[j - 1] - ui0) & 0xFFFFFFFFL;
            u[j - 1] = ui0;
            ui0 = ui1;
            j++;
        } while (j < n);
        u[j - 1] = ui0;
    }

    /**
     * Builds the row of U(n, .) numbers and returns the number of codewords V(n, k).
     * <p>
     * U(n, .) 수의 행을 만들고 부호어의 수 V(n, k) 를 반환한다.
     *
     * @param n the dimension (at least 2) / 차원(2 이상)
     * @param k the number of pulses (at least 1) / 펄스 수(1 이상)
     * @param u receives the row, k + 2 elements / 행을 받는 배열(k + 2 개)
     * @return V(n, k) modulo 2^32 / V(n, k) 의 2^32 로 나눈 나머지
     */
    private static long ncwrsUrow(int n, int k, long[] u) {
        int len = k + 2;
        u[0] = 0;
        u[1] = 1;
        // 2차원 행의 값 2k - 1 을 채운다.
        for (int i = 2; i < len; i++) {
            u[i] = (i << 1) - 1;
        }
        // 차원을 하나씩 올리며 행을 갱신한다.
        for (int d = 2; d < n; d++) {
            unext(u, 1, k + 1, 1);
        }
        return (u[k] + u[k + 1]) & 0xFFFFFFFFL;
    }

    /**
     * Decodes the pulse vector with the given codeword index.
     * <p>
     * 주어진 부호어 색인의 펄스 벡터를 디코딩한다.
     *
     * @param n  dimension / 차원
     * @param kIn number of pulses / 펄스 수
     * @param iIn codeword index / 부호어 색인
     * @param y  receives the pulses / 펄스를 받는 배열
     * @param u  the row built by {@link #ncwrsUrow} / {@link #ncwrsUrow} 가 만든 행
     */
    private static void cwrsi(int n, int kIn, long iIn, int[] y, long[] u) {
        int k = kIn;
        long i = iIn;
        // 차원마다 그 좌표의 크기와 부호를 색인에서 꺼낸다.
        for (int j = 0; j < n; j++) {
            long p = u[k + 1];
            int s = i >= p ? -1 : 0;
            if (s != 0) {
                i -= p;
            }
            int yj = k;
            p = u[k];
            // 색인보다 큰 동안 펄스 수를 줄인다.
            while (p > i) {
                p = u[--k];
            }
            i -= p;
            yj -= k;
            y[j] = (yj + s) ^ s;
            uprev(u, k + 2, 0);
        }
    }

    /**
     * Decodes a vector of K pulses in N dimensions from the range decoder.
     * <p>
     * 범위 디코더에서 N 차원 K 개 펄스의 벡터를 디코딩한다.
     *
     * @param y  receives the pulses, n entries / 펄스를 받는 배열(n 개)
     * @param n  dimension, at least 2 / 차원(2 이상)
     * @param k  number of pulses, at least 1 / 펄스 수(1 이상)
     * @param rc the range decoder / 범위 디코더
     */
    static void decodePulses(int[] y, int n, int k, OpusRangeDecoder rc) {
        long[] u = new long[k + 2];
        long nc = ncwrsUrow(n, k, u);
        // 부호어 수가 2 미만이면 잘못된 데이터이므로 펄스 없이 둔다.
        if (nc < 2) {
            java.util.Arrays.fill(y, 0, n, 0);
            return;
        }
        long idx = rc.readUint(nc);
        cwrsi(n, k, idx, y, u);
    }

    /**
     * One pass of the spreading rotation.
     * <p>
     * 퍼짐 회전의 한 번 통과.
     *
     * @param x      the vector / 벡터
     * @param xo     start of the vector / 벡터의 시작
     * @param len    length / 길이
     * @param stride distance of the rotated pairs / 회전 쌍의 간격
     * @param c      cosine / 코사인
     * @param s      sine / 사인
     */
    private static void expRotation1(float[] x, int xo, int len, int stride, float c, float s) {
        int p = xo;
        // 앞에서 뒤로 인접한 쌍을 회전한다.
        for (int i = 0; i < len - stride; i++) {
            float x1 = x[p];
            float x2 = x[p + stride];
            x[p + stride] = c * x2 + s * x1;
            x[p] = c * x1 - s * x2;
            p++;
        }
        p = xo + len - 2 * stride - 1;
        // 뒤에서 앞으로 한 번 더 회전한다.
        for (int i = len - 2 * stride - 1; i >= 0; i--) {
            float x1 = x[p];
            float x2 = x[p + stride];
            x[p + stride] = c * x2 + s * x1;
            x[p] = c * x1 - s * x2;
            p--;
        }
    }

    /**
     * The spreading rotation of a band (undone by the decoder with dir = -1).
     * <p>
     * 대역의 퍼짐 회전(디코더는 dir = -1 로 되돌린다).
     *
     * @param x      the vector / 벡터
     * @param xo     start / 시작
     * @param len    length / 길이
     * @param dir    direction / 방향
     * @param stride number of interleaved short blocks / 인터리브된 짧은 블록 수
     * @param k      number of pulses / 펄스 수
     * @param spread spreading mode / 퍼짐 모드
     */
    static void expRotation(float[] x, int xo, int len, int dir, int stride, int k, int spread) {
        if (2 * k >= len || spread == SPREAD_NONE) {
            return;
        }
        int factor = SPREAD_FACTOR[spread - 1];
        float gain = (1.0f * len) / (len + factor * k);
        float theta = 0.5f * (gain * gain);
        float c = CeltMath.cosNorm(theta);
        float s = CeltMath.cosNorm(1.0f - theta);
        int stride2 = 0;
        // 블록이 충분히 길면 두 번째 간격을 정한다.
        if (len >= 8 * stride) {
            stride2 = 1;
            // 조건이 만족되는 동안 반복해서 처리한다.
            while ((stride2 * stride2 + stride2) * stride + (stride >> 2) < len) {
                stride2++;
            }
        }
        int l = len / stride;
        // 인터리브된 블록마다 회전한다.
        for (int i = 0; i < stride; i++) {
            if (dir < 0) {
                if (stride2 != 0) {
                    expRotation1(x, xo + i * l, l, stride2, s, c);
                }
                expRotation1(x, xo + i * l, l, 1, c, s);
            } else {
                expRotation1(x, xo + i * l, l, 1, c, -s);
                if (stride2 != 0) {
                    expRotation1(x, xo + i * l, l, stride2, s, -c);
                }
            }
        }
    }

    /**
     * Decodes the shape of a band: pulses, normalization to the given gain, spreading rotation.
     * <p>
     * 대역의 모양을 디코딩한다: 펄스, 주어진 이득으로 정규화, 퍼짐 회전.
     *
     * @param x      receives the unit vector / 단위 벡터를 받는 배열
     * @param xo     start / 시작
     * @param n      dimension, at least 2 / 차원(2 이상)
     * @param k      pulses, at least 1 / 펄스 수(1 이상)
     * @param spread spreading mode / 퍼짐 모드
     * @param b      number of short blocks / 짧은 블록 수
     * @param rc     the range decoder / 범위 디코더
     * @param gain   gain of the vector / 벡터의 이득
     * @return the collapse mask (bit per short block that has pulses) / 붕괴 마스크(펄스가 있는 짧은 블록마다 한 비트)
     */
    static int algUnquant(float[] x, int xo, int n, int k, int spread, int b, OpusRangeDecoder rc, float gain) {
        int[] iy = new int[n];
        decodePulses(iy, n, k, rc);
        float ryy = 0;
        // 펄스 벡터의 에너지를 구한다.
        for (int i = 0; i < n; i++) {
            ryy = ryy + (float) iy[i] * (float) iy[i];
        }
        float g = (CeltMath.rsqrt(ryy)) * gain;
        // 펄스 벡터를 이득에 맞춰 정규화한다.
        for (int i = 0; i < n; i++) {
            x[xo + i] = g * iy[i];
        }
        expRotation(x, xo, n, -1, b, k, spread);
        // 짧은 블록 하나라면 마스크는 1 이다.
        if (b <= 1) {
            return 1;
        }
        int n0 = n / b;
        int mask = 0;
        // 짧은 블록마다 펄스가 있는지 확인한다.
        for (int i = 0; i < b; i++) {
            for (int j = 0; j < n0; j++) {
                if (iy[i * n0 + j] != 0) {
                    mask |= 1 << i;
                }
            }
        }
        return mask;
    }

    /**
     * Scales a vector to a given norm.
     * <p>
     * 벡터를 주어진 크기로 정규화한다.
     *
     * @param x    the vector / 벡터
     * @param xo   start / 시작
     * @param n    length / 길이
     * @param gain target norm / 목표 크기
     */
    static void renormaliseVector(float[] x, int xo, int n, float gain) {
        float e = 1e-15f;
        // 에너지를 합한다.
        for (int i = 0; i < n; i++) {
            e = e + x[xo + i] * x[xo + i];
        }
        float g = CeltMath.rsqrt(e) * gain;
        // 에너지의 제곱근 역수로 크기를 맞춘다.
        for (int i = 0; i < n; i++) {
            x[xo + i] = g * x[xo + i];
        }
    }
}
