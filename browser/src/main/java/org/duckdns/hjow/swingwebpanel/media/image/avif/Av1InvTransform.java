package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The inverse transforms of AV1 (plan item P18-03; AV1 specification section 7.13): the inverse DCT of 4 to 64 points, the inverse ADST of 4,
 * 8 and 16 points, the identity transforms, the Walsh-Hadamard transform of lossless blocks, and the 2D process with its row and column
 * shifts and intermediate clamping. The butterfly structure follows the specification step by step, in integer arithmetic.
 * <p>
 * AV1 의 역변환(계획 P18-03; AV1 규격 7.13절): 4~64점 역 DCT, 4/8/16점 역 ADST, 항등 변환, 무손실 블록의 월시-아다마르 변환, 행/열 시프트와 중간 값
 * 제한을 가진 2차원 처리. 나비 구조는 규격을 한 단계씩 따라 정수 연산으로 구현했다.
 */
final class Av1InvTransform {

    /**
     * Row transform kind: DCT.
     * <p>
     * 행 변환 종류: DCT.
     */
    private static final int K_DCT = 0;

    /**
     * Row transform kind: ADST.
     * <p>
     * 행 변환 종류: ADST.
     */
    private static final int K_ADST = 1;

    /**
     * Row transform kind: identity.
     * <p>
     * 행 변환 종류: 항등.
     */
    private static final int K_IDENTITY = 2;

    /**
     * The horizontal (row) transform kind of each of the 16 transform types.
     * <p>
     * 변환 종류 16가지 각각의 가로(행) 변환 종류.
     */
    private static final int[] ROW_KIND = { K_DCT, K_DCT, K_ADST, K_ADST, K_DCT, K_ADST, K_ADST, K_ADST, K_ADST, K_IDENTITY, K_IDENTITY, K_DCT,
            K_IDENTITY, K_ADST, K_IDENTITY, K_ADST };

    /**
     * The vertical (column) transform kind of each of the 16 transform types.
     * <p>
     * 변환 종류 16가지 각각의 세로(열) 변환 종류.
     */
    private static final int[] COL_KIND = { K_DCT, K_ADST, K_DCT, K_ADST, K_ADST, K_DCT, K_ADST, K_ADST, K_ADST, K_IDENTITY, K_DCT, K_IDENTITY,
            K_ADST, K_IDENTITY, K_ADST, K_IDENTITY };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Av1InvTransform() {
    }

    /**
     * Rounds a value to the right by n bits (the specification function Round2).
     * <p>
     * 값을 오른쪽으로 n 비트 반올림한다(규격의 Round2 함수).
     *
     * @param x the value / 값
     * @param n the shift / 시프트
     * @return the rounded value / 반올림한 값
     */
    private static int round2(int x, int n) {
        if (n == 0) {
            return x;
        }
        return (x + (1 << (n - 1))) >> n;
    }

    /**
     * Rounds a long value to the right by n bits.
     * <p>
     * long 값을 오른쪽으로 n 비트 반올림한다.
     *
     * @param x the value / 값
     * @param n the shift / 시프트
     * @return the rounded value / 반올림한 값
     */
    private static int round2(long x, int n) {
        return (int) ((x + (1L << (n - 1))) >> n);
    }

    /**
     * Reverses the lowest numBits bits of a number.
     * <p>
     * 수의 낮은 numBits 비트를 뒤집는다.
     *
     * @param numBits the number of bits / 비트 수
     * @param x       the number / 수
     * @return the reversed number / 뒤집은 수
     */
    private static int brev(int numBits, int x) {
        int t = 0;
        // 비트를 하나씩 반대 위치로 옮긴다.
        for (int i = 0; i < numBits; i++) {
            int bit = (x >> i) & 1;
            t += bit << (numBits - 1 - i);
        }
        return t;
    }

    /**
     * The function cos128: 4096 times the cosine of angle * pi / 128.
     * <p>
     * 함수 cos128: angle * pi / 128 의 코사인에 4096 을 곱한 값.
     *
     * @param angle the angle in units of pi / 128 / pi / 128 단위의 각도
     * @return the value / 값
     */
    private static int cos128(int angle) {
        int a = angle & 255;
        if (a <= 64) {
            return AvifTables.COS128_LOOKUP[a];
        }
        if (a <= 128) {
            return -AvifTables.COS128_LOOKUP[128 - a];
        }
        if (a <= 192) {
            return -AvifTables.COS128_LOOKUP[a - 128];
        }
        return AvifTables.COS128_LOOKUP[256 - a];
    }

    /**
     * The function sin128.
     * <p>
     * 함수 sin128.
     *
     * @param angle the angle / 각도
     * @return the value / 값
     */
    private static int sin128(int angle) {
        return cos128(angle - 64);
    }

    /**
     * The butterfly rotation B( a, b, angle, flip ).
     * <p>
     * 나비 회전 B( a, b, angle, flip ).
     *
     * @param t     the array / 배열
     * @param a     index a / 인덱스 a
     * @param b     index b / 인덱스 b
     * @param angle the angle / 각도
     * @param flip  true to exchange the results / 결과를 맞바꾸려면 true
     */
    private static void bfly(int[] t, int a, int b, int angle, boolean flip) {
        long x = (long) t[a] * cos128(angle) - (long) t[b] * sin128(angle);
        long y = (long) t[a] * sin128(angle) + (long) t[b] * cos128(angle);
        int xa = round2(x, 12);
        int yb = round2(y, 12);
        if (flip) {
            t[a] = yb;
            t[b] = xa;
        } else {
            t[a] = xa;
            t[b] = yb;
        }
    }

    /**
     * The Hadamard rotation H( a, b, flip, r ).
     * <p>
     * 아다마르 회전 H( a, b, flip, r ).
     *
     * @param t    the array / 배열
     * @param a    index a / 인덱스 a
     * @param b    index b / 인덱스 b
     * @param flip true to use the flipped indices / 인덱스를 뒤집으려면 true
     * @param r    the clamping range in bits / 제한 범위(비트)
     */
    private static void had(int[] t, int a, int b, boolean flip, int r) {
        int ia = flip ? b : a;
        int ib = flip ? a : b;
        int x = t[ia];
        int y = t[ib];
        int lo = -(1 << (r - 1));
        int hi = (1 << (r - 1)) - 1;
        int s = x + y;
        int d = x - y;
        t[ia] = s < lo ? lo : (s > hi ? hi : s);
        t[ib] = d < lo ? lo : (d > hi ? hi : d);
    }

    /**
     * The in-place inverse DCT of 2^n points.
     * <p>
     * 2^n 점의 제자리 역 DCT.
     *
     * @param t the array / 배열
     * @param n the base 2 logarithm of the length (2 to 6) / 길이의 밑 2 로그(2~6)
     * @param r the clamping range / 제한 범위
     */
    private static void idct(int[] t, int n, int r) {
        int n0 = 1 << n;
        int[] copy = new int[n0];
        System.arraycopy(t, 0, copy, 0, n0);
        // 입력 순서를 비트 반전 순서로 바꾼다.
        for (int i = 0; i < n0; i++) {
            t[i] = copy[brev(n, i)];
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 16; i++) {
                bfly(t, 32 + i, 63 - i, 63 - 4 * brev(4, i), false);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                bfly(t, 16 + i, 31 - i, 6 + (brev(3, 7 - i) << 3), false);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 16; i++) {
                had(t, 32 + i * 2, 33 + i * 2, (i & 1) != 0, r);
            }
        }
        if (n >= 4) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                bfly(t, 8 + i, 15 - i, 12 + (brev(2, 3 - i) << 4), false);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                had(t, 16 + 2 * i, 17 + 2 * i, (i & 1) != 0, r);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    bfly(t, 62 - i * 4 - j, 33 + i * 4 + j, 60 - 16 * brev(2, i) + 64 * j, true);
                }
            }
        }
        if (n >= 3) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                bfly(t, 4 + i, 7 - i, 56 - 32 * i, false);
            }
        }
        if (n >= 4) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                had(t, 8 + 2 * i, 9 + 2 * i, (i & 1) != 0, r);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    bfly(t, 30 - 4 * i - j, 17 + 4 * i + j, 24 + (j << 6) + ((1 - i) << 5), true);
                }
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    had(t, 32 + i * 4 + j, 35 + i * 4 - j, (i & 1) != 0, r);
                }
            }
        }
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 2; i++) {
            bfly(t, 2 * i, 2 * i + 1, 32 + 16 * i, i == 0);
        }
        if (n >= 3) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                had(t, 4 + 2 * i, 5 + 2 * i, i != 0, r);
            }
        }
        if (n >= 4) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                bfly(t, 14 - i, 9 + i, 48 + 64 * i, true);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    had(t, 16 + 4 * i + j, 19 + 4 * i - j, (i & 1) != 0, r);
                }
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 4; j++) {
                    bfly(t, 61 - i * 8 - j, 34 + i * 8 + j, 56 - i * 32 + (j >> 1) * 64, true);
                }
            }
        }
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 2; i++) {
            had(t, i, 3 - i, false, r);
        }
        if (n >= 3) {
            bfly(t, 6, 5, 32, true);
        }
        if (n >= 4) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    had(t, 8 + 4 * i + j, 11 + 4 * i - j, i != 0, r);
                }
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                bfly(t, 29 - i, 18 + i, 48 + (i >> 1) * 64, true);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 4; j++) {
                    had(t, 32 + 8 * i + j, 39 + 8 * i - j, (i & 1) != 0, r);
                }
            }
        }
        if (n >= 3) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                had(t, i, 7 - i, false, r);
            }
        }
        if (n >= 4) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                bfly(t, 13 - i, 10 + i, 32, true);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 4; j++) {
                    had(t, 16 + i * 8 + j, 23 + i * 8 - j, i != 0, r);
                }
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                bfly(t, 59 - i, 36 + i, i < 4 ? 48 : 112, true);
            }
        }
        if (n >= 4) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                had(t, i, 15 - i, false, r);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                bfly(t, 27 - i, 20 + i, 32, true);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                had(t, 32 + i, 47 - i, false, r);
                had(t, 48 + i, 63 - i, true, r);
            }
        }
        if (n >= 5) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 16; i++) {
                had(t, i, 31 - i, false, r);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 8; i++) {
                bfly(t, 55 - i, 40 + i, 32, true);
            }
        }
        if (n == 6) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 32; i++) {
                had(t, i, 63 - i, false, r);
            }
        }
    }

    /**
     * The inverse ADST input permutation.
     * <p>
     * 역 ADST 의 입력 순서 치환.
     *
     * @param t the array / 배열
     * @param n the base 2 logarithm of the length / 길이의 밑 2 로그
     */
    private static void adstIn(int[] t, int n) {
        int n0 = 1 << n;
        int[] copy = new int[n0];
        System.arraycopy(t, 0, copy, 0, n0);
        // 홀수 위치는 앞 원소를, 짝수 위치는 뒤에서부터 가져온다.
        for (int i = 0; i < n0; i++) {
            int idx = (i & 1) != 0 ? (i - 1) : (n0 - i - 1);
            t[i] = copy[idx];
        }
    }

    /**
     * The inverse ADST output permutation.
     * <p>
     * 역 ADST 의 출력 순서 치환.
     *
     * @param t the array / 배열
     * @param n the base 2 logarithm of the length / 길이의 밑 2 로그
     */
    private static void adstOut(int[] t, int n) {
        int n0 = 1 << n;
        int[] copy = new int[n0];
        System.arraycopy(t, 0, copy, 0, n0);
        // 비트 연산으로 정해진 위치의 값을 가져오고 홀수 위치는 부호를 바꾼다.
        for (int i = 0; i < n0; i++) {
            int a = (i >> 3) & 1;
            int b = ((i >> 2) & 1) ^ ((i >> 3) & 1);
            int c = ((i >> 1) & 1) ^ ((i >> 2) & 1);
            int d = (i & 1) ^ ((i >> 1) & 1);
            int idx = ((d << 3) | (c << 2) | (b << 1) | a) >> (4 - n);
            t[i] = (i & 1) != 0 ? -copy[idx] : copy[idx];
        }
    }

    /**
     * The in-place inverse ADST of 4, 8 or 16 points.
     * <p>
     * 4, 8, 16점의 제자리 역 ADST.
     *
     * @param t the array / 배열
     * @param n the base 2 logarithm of the length (2 to 4) / 길이의 밑 2 로그(2~4)
     * @param r the clamping range / 제한 범위
     */
    private static void iadst(int[] t, int n, int r) {
        if (n == 2) {
            final int s1 = 1321;
            final int s2 = 2482;
            final int s3 = 3344;
            final int s4 = 3803;
            long[] s = new long[7];
            s[0] = (long) s1 * t[0];
            s[1] = (long) s2 * t[0];
            s[2] = (long) s3 * t[1];
            s[3] = (long) s4 * t[2];
            s[4] = (long) s1 * t[2];
            s[5] = (long) s2 * t[3];
            s[6] = (long) s4 * t[3];
            long a7 = (long) t[0] - t[2];
            long b7 = a7 + t[3];
            s[0] = s[0] + s[3];
            s[1] = s[1] - s[4];
            s[3] = s[2];
            s[2] = s3 * b7;
            s[0] = s[0] + s[5];
            s[1] = s[1] - s[6];
            long x0 = s[0] + s[3];
            long x1 = s[1] + s[3];
            long x2 = s[2];
            long x3 = s[0] + s[1];
            x3 = x3 - s[3];
            t[0] = round2(x0, 12);
            t[1] = round2(x1, 12);
            t[2] = round2(x2, 12);
            t[3] = round2(x3, 12);
            return;
        }
        if (n == 3) {
            adstIn(t, 3);
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                bfly(t, 2 * i, 2 * i + 1, 60 - 16 * i, true);
            }
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 4; i++) {
                had(t, i, 4 + i, false, r);
            }
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                bfly(t, 4 + 3 * i, 5 + i, 48 - 32 * i, true);
            }
            // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
                for (int j = 0; j < 2; j++) {
                    had(t, 4 * j + i, 2 + 4 * j + i, false, r);
                }
            }
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int i = 0; i < 2; i++) {
                bfly(t, 2 + 4 * i, 3 + 4 * i, 32, true);
            }
            adstOut(t, 3);
            return;
        }
        adstIn(t, 4);
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 8; i++) {
            bfly(t, 2 * i, 2 * i + 1, 62 - 8 * i, true);
        }
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 8; i++) {
            had(t, i, 8 + i, false, r);
        }
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 2; i++) {
            bfly(t, 8 + 2 * i, 9 + 2 * i, 56 - 32 * i, true);
            bfly(t, 13 + 2 * i, 12 + 2 * i, 8 + 32 * i, true);
        }
        // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
        for (int i = 0; i < 4; i++) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int j = 0; j < 2; j++) {
                had(t, 8 * j + i, 4 + 8 * j + i, false, r);
            }
        }
        // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
        for (int i = 0; i < 2; i++) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int j = 0; j < 2; j++) {
                bfly(t, 4 + 8 * j + 3 * i, 5 + 8 * j + i, 48 - 32 * i, true);
            }
        }
        // 이 단계의 인덱스마다 같은 변환 연산을 적용한다.
        for (int i = 0; i < 2; i++) {
            // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
            for (int j = 0; j < 4; j++) {
                had(t, 4 * j + i, 2 + 4 * j + i, false, r);
            }
        }
        // 이 단계의 인덱스 쌍마다 같은 버터플라이 연산을 적용한다.
        for (int i = 0; i < 4; i++) {
            bfly(t, 2 + 4 * i, 3 + 4 * i, 32, true);
        }
        adstOut(t, 4);
    }

    /**
     * The in-place identity transform of 4, 8, 16 or 32 points.
     * <p>
     * 4, 8, 16, 32점의 제자리 항등 변환.
     *
     * @param t the array / 배열
     * @param n the base 2 logarithm of the length (2 to 5) / 길이의 밑 2 로그(2~5)
     */
    private static void iidentity(int[] t, int n) {
        int len = 1 << n;
        // 길이마다 정해진 배율을 곱한다.
        for (int i = 0; i < len; i++) {
            if (n == 2) {
                t[i] = round2((long) t[i] * 5793, 12);
            } else if (n == 3) {
                t[i] = t[i] * 2;
            } else if (n == 4) {
                t[i] = round2((long) t[i] * 11586, 12);
            } else {
                t[i] = t[i] * 4;
            }
        }
    }

    /**
     * The inverse Walsh-Hadamard transform of 4 points.
     * <p>
     * 4점 역 월시-아다마르 변환.
     *
     * @param t     the array / 배열
     * @param shift the pre-scaling shift / 사전 크기 조정 시프트
     */
    private static void iwht(int[] t, int shift) {
        int a = t[0] >> shift;
        int c = t[1] >> shift;
        int d = t[2] >> shift;
        int b = t[3] >> shift;
        a += c;
        d -= b;
        int e = (a - d) >> 1;
        b = e - b;
        c = e - c;
        a -= b;
        d += c;
        t[0] = a;
        t[1] = b;
        t[2] = c;
        t[3] = d;
    }

    /**
     * Applies a 1D transform of the given kind.
     * <p>
     * 지정한 종류의 1차원 변환을 적용한다.
     *
     * @param t    the array / 배열
     * @param kind the kind (DCT, ADST or identity) / 종류(DCT, ADST, 항등)
     * @param n    the base 2 logarithm of the length / 길이의 밑 2 로그
     * @param r    the clamping range / 제한 범위
     */
    private static void transform1d(int[] t, int kind, int n, int r) {
        if (kind == K_DCT) {
            idct(t, n, r);
        } else if (kind == K_ADST) {
            iadst(t, n, r);
        } else {
            iidentity(t, n);
        }
    }

    /**
     * The 2D inverse transform process (section 7.13.3): transforms the dequantized coefficients into the residual.
     * <p>
     * 2차원 역변환 처리(7.13.3절): 역양자화된 계수를 잔차로 변환한다.
     *
     * @param dequant  the dequantized coefficients, row i and column j at <code>i * 64 + j</code>, only rows and columns below 32 are read / 역양자화된 계수. 행 i, 열 j 는 <code>i * 64 + j</code>, 32 미만의 행과 열만 읽는다
     * @param log2W    the base 2 logarithm of the width / 너비의 밑 2 로그
     * @param log2H    the base 2 logarithm of the height / 높이의 밑 2 로그
     * @param txType   the transform type (0..15) / 변환 종류(0..15)
     * @param lossless true for the lossless Walsh-Hadamard transform / 무손실 월시-아다마르 변환이면 true
     * @param rowShift the row shift (Transform_Row_Shift of the size) / 행 시프트(크기의 Transform_Row_Shift)
     * @param bitDepth the bit depth / 비트 깊이
     * @param out      receives the residual, row i and column j at <code>i * w + j</code> / 잔차를 받는다. 행 i, 열 j 는 <code>i * w + j</code>
     */
    static void inverse2d(int[] dequant, int log2W, int log2H, int txType, boolean lossless, int rowShift, int bitDepth, int[] out) {
        int w = 1 << log2W;
        int h = 1 << log2H;
        int colShift = lossless ? 0 : 4;
        int rowClamp = bitDepth + 8;
        int colClamp = Math.max(bitDepth + 6, 16);
        int rs = lossless ? 0 : rowShift;
        int rowKind = ROW_KIND[txType];
        int colKind = COL_KIND[txType];
        int[] t = new int[64];
        int lo = -(1 << (colClamp - 1));
        int hi = (1 << (colClamp - 1)) - 1;
        // 행 변환: 앞쪽 32행만 계수가 있으며 나머지 행은 0 이다.
        for (int i = 0; i < h; i++) {
            if (i >= 32) {
                java.util.Arrays.fill(out, i * w, i * w + w, 0);
                continue;
            }
            // 행의 계수를 임시 배열로 옮긴다(32 열 이상은 0).
            for (int j = 0; j < w; j++) {
                t[j] = j < 32 ? dequant[i * 64 + j] : 0;
            }
            if (Math.abs(log2W - log2H) == 1) {
                // 정사각형이 아닌 변환은 값에 1/√2 배율을 곱한다.
                for (int j = 0; j < w; j++) {
                    t[j] = round2((long) t[j] * 2896, 12);
                }
            }
            if (lossless) {
                iwht(t, 2);
            } else {
                transform1d(t, rowKind, log2W, rowClamp);
            }
            // 행 변환 결과를 반올림하여 중간 배열에 쓴다.
            for (int j = 0; j < w; j++) {
                int v = round2(t[j], rs);
                out[i * w + j] = v < lo ? lo : (v > hi ? hi : v);
            }
        }
        // 열 변환: 열마다 위에서 아래로 변환한 뒤 열 시프트를 적용한다.
        for (int j = 0; j < w; j++) {
            for (int i = 0; i < h; i++) {
                t[i] = out[i * w + j];
            }
            if (lossless) {
                iwht(t, 0);
            } else {
                transform1d(t, colKind, log2H, colClamp);
            }
            // 열 변환 결과를 반올림하여 잔차 배열의 한 열에 쓴다.
            for (int i = 0; i < h; i++) {
                out[i * w + j] = round2(t[i], colShift);
            }
        }
    }
}
