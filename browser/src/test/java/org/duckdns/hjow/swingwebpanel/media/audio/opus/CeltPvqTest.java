package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the pyramid vector quantizer of RFC 6716 section 4.3.4: the pulse vectors of a band are coded as an index in the
 * enumeration of all vectors with K pulses in N dimensions; the tests encode random vectors with a test-only enumeration
 * (<code>icwrs</code> of the reference source) through the test range encoder and require that the decoder returns the same
 * vectors, check the number of codewords against the known closed forms and test the normalization of the decoded shape.
 * <p>
 * RFC 6716 4.3.4 피라미드 벡터 양자화기의 시험: 대역의 펄스 벡터는 N 차원에서 펄스 K 개를 가진 모든 벡터를 나열한 순서의 색인으로 부호화된다.
 * 시험은 무작위 벡터를 시험 전용 열거(참조 소스의 <code>icwrs</code>)로 시험용 범위 인코더에 부호화해 디코더가 같은 벡터를 돌려주는지 확인하고,
 * 부호어 수를 알려진 닫힌 식과 대조하며, 디코딩한 모양의 정규화를 시험한다.
 */
public class CeltPvqTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CeltPvqTest() {
    }

    /**
     * Index of a pulse vector and the number of codewords, as <code>icwrs</code> of the reference source computes them.
     * <p>
     * 참조 소스의 <code>icwrs</code> 가 계산하는 펄스 벡터의 색인과 부호어 수.
     *
     * @param n    dimension / 차원
     * @param k    pulses / 펄스 수
     * @param y    the vector / 벡터
     * @param nc   receives the number of codewords at index 0 / 인덱스 0 에 부호어 수를 받는 배열
     * @return the index / 색인
     */
    private static long index(int n, int k, int[] y, long[] nc) {
        long[] u = new long[k + 2];
        u[0] = 0;
        // 2차원 행(2k - 1)을 만든다.
        for (int i = 1; i <= k + 1; i++) {
            u[i] = (i << 1) - 1;
        }
        int kk = Math.abs(y[n - 1]);
        long idx = y[n - 1] < 0 ? 1 : 0;
        int j = n - 2;
        idx += u[kk];
        kk += Math.abs(y[j]);
        if (y[j] < 0) {
            idx += u[kk + 1];
        }
        // 차원을 하나씩 올리며 색인에 더한다.
        while (j-- > 0) {
            unext(u);
            idx += u[kk];
            kk += Math.abs(y[j]);
            if (y[j] < 0) {
                idx += u[kk + 1];
            }
        }
        nc[0] = u[kk] + u[kk + 1];
        return idx & 0xFFFFFFFFL;
    }

    /**
     * Steps the row of U numbers to the next dimension.
     * <p>
     * U 수의 행을 다음 차원으로 진행한다.
     *
     * @param u the row / 행
     */
    private static void unext(long[] u) {
        long ui0 = 0;
        // 이웃한 원소를 더해 다음 차원의 행을 만든다.
        for (int j = 1; j < u.length; j++) {
            long ui1 = (u[j] + u[j - 1] + ui0) & 0xFFFFFFFFL;
            u[j - 1] = ui0;
            ui0 = ui1;
        }
        u[u.length - 1] = ui0;
    }

    /**
     * Builds a random vector of n values with a sum of absolute values of k.
     * <p>
     * 절댓값의 합이 k 인 n 개 값의 무작위 벡터를 만든다.
     *
     * @param rnd random source / 난수원
     * @param n   dimension / 차원
     * @param k   pulses / 펄스 수
     * @return the vector / 벡터
     */
    private static int[] randomVector(Random rnd, int n, int k) {
        int[] y = new int[n];
        // 펄스를 하나씩 임의의 위치에 임의의 부호로 놓는다.
        for (int i = 0; i < k; i++) {
            y[rnd.nextInt(n)] += rnd.nextBoolean() ? 1 : -1;
        }
        // 같은 위치의 +1 과 -1 이 상쇄되면 합이 줄어드므로 다시 센다.
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += Math.abs(y[i]);
        }
        // 조건이 만족되는 동안 반복해서 처리한다.
        while (sum < k) {
            int p = rnd.nextInt(n);
            y[p] += y[p] < 0 ? -1 : 1;
            sum++;
        }
        return y;
    }

    /**
     * Vectors of many shapes and sizes survive the round trip through the range coder.
     * <p>
     * 여러 모양과 크기의 벡터가 범위 부호화기를 거친 왕복을 견딘다.
     */
    public void testRoundTrip() {
        Random rnd = new Random(7);
        long[][] count = new long[200][130];
        // 부호어 수 V(N, K) 를 큰 정수 범위에서 점화식으로 계산한다(32비트를 넘는 모양은 부호화할 수 없다).
        for (int n = 1; n < 200; n++) {
            count[n][0] = 1;
            // k 를 1 부터 130 까지 늘려 가며 항목마다 처리한다.
            for (int k = 1; k < 130; k++) {
                count[n][k] = n == 1 ? 2 : Math.min(Long.MAX_VALUE / 4, count[n - 1][k] + count[n][k - 1] + count[n - 1][k - 1]);
            }
        }
        int[][] shapes = { { 2, 1 }, { 2, 7 }, { 3, 5 }, { 4, 4 }, { 5, 9 }, { 8, 3 }, { 8, 16 }, { 16, 8 }, { 20, 5 }, { 32, 4 }, { 96, 3 }, { 176, 2 }, { 12, 40 },
                { 6, 128 }, { 3, 128 } };
        // 모양마다 벡터를 여러 개 시험한다.
        for (int s = 0; s < shapes.length; s++) {
            int n = shapes[s][0];
            int k = shapes[s][1];
            if (count[n][k] >= (1L << 32)) {
                continue;
            }
            // rep 를 0 부터 25 까지 늘려 가며 항목마다 처리한다.
            for (int rep = 0; rep < 25; rep++) {
                int[] y = randomVector(rnd, n, k);
                long[] nc = new long[1];
                long idx = index(n, k, y, nc);
                Check.equal(count[n][k], nc[0], "V(" + n + "," + k + ") from the enumeration");
                OpusRangeEncoder enc = new OpusRangeEncoder(64);
                enc.uint(idx, nc[0]);
                byte[] data = enc.done();
                int[] got = new int[n];
                CeltPvq.decodePulses(got, n, k, new OpusRangeDecoder(data, 0, data.length));
                Check.isTrue(java.util.Arrays.equals(y, got), "n=" + n + " k=" + k + " rep " + rep + " expected " + java.util.Arrays.toString(y) + " got "
                        + java.util.Arrays.toString(got));
            }
        }
    }

    /**
     * Every vector of a small space is returned by exactly one index: the decoder enumerates the whole space without repeats.
     * <p>
     * 작은 공간의 모든 벡터는 색인 하나에 정확히 대응한다: 디코더는 같은 값을 반복하지 않고 공간 전체를 나열한다.
     */
    public void testEnumerationIsABijection() {
        int n = 4;
        int k = 3;
        long[] nc = new long[1];
        index(n, k, new int[] { 3, 0, 0, 0 }, nc);
        Check.equal(((k * k + 2) * k) / 3 << 3, nc[0], "number of codewords V(4,3)");
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        // 가능한 모든 색인을 디코딩해 서로 다른 유효 벡터인지 확인한다.
        for (long i = 0; i < nc[0]; i++) {
            OpusRangeEncoder enc = new OpusRangeEncoder(16);
            enc.uint(i, nc[0]);
            byte[] data = enc.done();
            int[] y = new int[n];
            CeltPvq.decodePulses(y, n, k, new OpusRangeDecoder(data, 0, data.length));
            int sum = 0;
            // j 를 0 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int j = 0; j < n; j++) {
                sum += Math.abs(y[j]);
            }
            Check.equal(k, sum, "pulses of index " + i);
            Check.isTrue(seen.add(java.util.Arrays.toString(y)), "index " + i + " repeats a vector");
        }
        Check.equal(nc[0], seen.size(), "all codewords distinct");
    }

    /**
     * The number of codewords agrees with the closed forms for 2, 3 and 4 dimensions and with the recurrence V(N,K) = V(N-1,K) + V(N,K-1) + V(N-1,K-1).
     * <p>
     * 부호어 수는 2, 3, 4 차원의 닫힌 식과, 점화식 V(N,K) = V(N-1,K) + V(N,K-1) + V(N-1,K-1) 와 일치한다.
     */
    public void testCodewordCounts() {
        long[][] v = new long[6][13];
        // 점화식으로 작은 표를 만든다(V(N,0) = 1, V(1,K) = 2).
        for (int n = 1; n < 6; n++) {
            v[n][0] = 1;
        }
        // k 를 1 부터 13 까지 늘려 가며 항목마다 처리한다.
        for (int k = 1; k < 13; k++) {
            v[1][k] = 2;
        }
        // n 를 2 부터 6 까지 늘려 가며 항목마다 처리한다.
        for (int n = 2; n < 6; n++) {
            // k 를 1 부터 13 까지 늘려 가며 항목마다 처리한다.
            for (int k = 1; k < 13; k++) {
                v[n][k] = v[n - 1][k] + v[n][k - 1] + v[n - 1][k - 1];
            }
        }
        Random rnd = new Random(3);
        // 몇 개의 (n, k) 에서 열거 쪽 부호어 수를 대조한다.
        for (int n = 2; n < 6; n++) {
            for (int k = 1; k < 13; k++) {
                long[] nc = new long[1];
                index(n, k, randomVector(rnd, n, k), nc);
                Check.equal(v[n][k], nc[0], "V(" + n + "," + k + ")");
            }
        }
        Check.equal(4L * 5, v[2][5], "V(2,5) = 4K");
        Check.equal(2L * (2 * 25 + 1), v[3][5], "V(3,5) = 2(2K^2 + 1)");
    }

    /**
     * The decoded shape has unit norm times the gain (before the spreading rotation, which preserves the norm), whatever the pulses.
     * <p>
     * 디코딩한 모양은 펄스와 상관없이 크기가 이득과 같다(크기를 보존하는 퍼짐 회전 전후 동일).
     */
    public void testNormalization() {
        Random rnd = new Random(11);
        // 몇 가지 크기와 퍼짐 모드에서 정규화를 확인한다.
        for (int spread = 0; spread <= 3; spread++) {
            int n = 16;
            int k = 5;
            int[] y = randomVector(rnd, n, k);
            long[] nc = new long[1];
            long idx = index(n, k, y, nc);
            OpusRangeEncoder enc = new OpusRangeEncoder(32);
            enc.uint(idx, nc[0]);
            byte[] data = enc.done();
            float[] x = new float[n];
            CeltPvq.algUnquant(x, 0, n, k, spread, 1, new OpusRangeDecoder(data, 0, data.length), 0.5f);
            double e = 0;
            // i 를 0 부터 n 까지 늘려 가며 항목마다 처리한다.
            for (int i = 0; i < n; i++) {
                e += (double) x[i] * x[i];
            }
            Check.near(0.25, e, 1e-4, "energy with spread " + spread);
        }
    }
}
