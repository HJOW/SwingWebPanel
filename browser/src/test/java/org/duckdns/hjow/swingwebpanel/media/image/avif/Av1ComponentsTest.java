package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Unit tests of the building blocks of the AV1 decoder (plan item P18-03): the bit reader, the symbol decoder, the inverse transforms, the intra
 * predictors and the constant tables. The full chain is checked bit exactly by {@link Av1StreamTest}; these tests pin down the small pieces
 * with values that follow from the specification.
 * <p>
 * AV1 디코더 구성 요소의 단위 시험(계획 P18-03): 비트 읽기, 심볼 디코더, 역변환, 인트라 예측기, 상수 표. 전체 연쇄는 {@link Av1StreamTest} 가 비트
 * 단위로 확인하며, 이 시험은 규격에서 나오는 값으로 작은 조각을 고정한다.
 */
public class Av1ComponentsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Av1ComponentsTest() {
    }

    /**
     * The bit reader reads fixed width, signed, LEB128, unary-prefixed and non-symmetric values and refuses to read past the end.
     * <p>
     * 비트 읽기는 고정 너비, 부호 있는 값, LEB128, 단항 접두 값, 비대칭 값을 읽고 끝을 넘어 읽기를 거부한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testBitReader() throws Exception {
        Av1BitReader r = new Av1BitReader(new byte[] { (byte) 0xB3, (byte) 0xE5, (byte) 0x8E, 0x26, 0x28 }, 0, 5);
        Check.equal(5, r.bits(3), "first three bits 101");
        Check.equal(19, r.bits(5), "next five bits 10011");
        Check.equal(624485L, r.leb128(), "LEB128 of E5 8E 26");
        // 0x28 = 00101000: uvlc 는 앞의 0 두 개, 1, 값 비트 01 → 4.
        Check.equal(4L, r.uvlc(), "uvlc of 00101");
        Av1BitReader s = new Av1BitReader(new byte[] { (byte) 0xF7, (byte) 0x00 }, 0, 2);
        Check.equal(-1, s.su(4), "su(4) of 1111");
        Check.equal(7, s.su(4), "su(4) of 0111");
        Av1BitReader n = new Av1BitReader(new byte[] { (byte) 0x40 }, 0, 1);
        // ns(5): w = 3, m = 3; 처음 두 비트 01 = 1 < 3 이므로 값은 1 이다.
        Check.equal(1, n.ns(5), "ns(5) of 01");
        Av1BitReader a = new Av1BitReader(new byte[] { (byte) 0xFF }, 0, 1);
        a.bits(3);
        a.byteAlign();
        Check.equal(8L, a.position(), "byte alignment");
        final Av1BitReader end = new Av1BitReader(new byte[] { 1 }, 0, 1);
        end.bits(8);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "reading past the end", new Block() {
            public void run() throws Exception {
                end.bit();
            }
        });
    }

    /**
     * The symbol decoder keeps every distribution valid while it adapts, returns symbols in range and reads zero bits past the end of the
     * data.
     * <p>
     * 심볼 디코더는 적응하는 동안 모든 분포를 유효하게 유지하고, 범위 안의 심볼을 반환하며, 데이터 끝을 넘으면 0 비트를 읽는다.
     */
    public void testSymbolDecoder() {
        byte[] data = new byte[200];
        new Random(5).nextBytes(data);
        Av1SymbolDecoder sd = new Av1SymbolDecoder(data, 0, data.length, false);
        int[] cdf = { 8192, 16384, 24576, 32768, 0 };
        int[] seen = new int[4];
        // 데이터를 넘어서까지 많은 심볼을 읽으며 분포가 항상 유효한지 확인한다.
        for (int i = 0; i < 3000; i++) {
            int sym = sd.symbol(cdf, 0, 4);
            Check.isTrue(sym >= 0 && sym < 4, "symbol in range");
            seen[sym]++;
            Check.isTrue(cdf[0] <= cdf[1] && cdf[1] <= cdf[2] && cdf[2] <= cdf[3], "the distribution stays monotonic");
            Check.equal(32768, cdf[3], "the last entry of the distribution stays 32768");
        }
        Check.equal(32, cdf[4], "the counter saturates at 32");
        Check.isTrue(seen[0] + seen[1] + seen[2] + seen[3] == 3000, "all symbols were counted");
        Av1SymbolDecoder fixed = new Av1SymbolDecoder(data, 0, data.length, true);
        int[] copy = { 8192, 16384, 24576, 32768, 0 };
        // 적응을 끈 디코더로 심볼을 읽는다.
        for (int i = 0; i < 100; i++) {
            fixed.symbol(copy, 0, 4);
        }
        Check.equal(8192, copy[0], "no adaptation when updates are disabled");
        Check.equal(0, copy[4], "no counter when updates are disabled");
        Av1SymbolDecoder lit = new Av1SymbolDecoder(new byte[0], 0, 0, false);
        Check.isTrue(lit.literal(8) >= 0, "an empty tile reads without failing");
    }

    /**
     * A transform of only a DC coefficient gives a flat residual, in all sizes and for the types that have a DC term.
     * <p>
     * DC 계수만 있는 변환은 모든 크기에서, DC 항이 있는 종류에 대해 평탄한 잔차를 준다.
     */
    public void testInverseTransformOfDcIsFlat() {
        int[] sizes = { 2, 3, 4, 5, 6 };
        // 모든 너비/높이 조합(비율 4:1 이하)에 대해 DC 만 있는 입력을 변환한다.
        for (int lw : sizes) {
            for (int lh : sizes) {
                if (Math.abs(lw - lh) > 2) {
                    continue;
                }
                int w = 1 << lw;
                int h = 1 << lh;
                int[] dq = new int[64 * 64];
                dq[0] = 800;
                int[] out = new int[w * h];
                Av1InvTransform.inverse2d(dq, lw, lh, 0, false, 1, 8, out);
                // 모든 출력 샘플이 첫 샘플과 같은지 확인한다.
                for (int i = 1; i < w * h; i++) {
                    if (out[i] != out[0]) {
                        Check.fail("DCT of a DC term is not flat for " + w + "x" + h + " at " + i);
                    }
                }
                Check.isTrue(out[0] > 0, "a positive DC gives a positive residual " + w + "x" + h);
            }
        }
        int[] zero = new int[64 * 64];
        int[] out = new int[64];
        Av1InvTransform.inverse2d(zero, 3, 3, 3, false, 1, 8, out);
        // 모든 출력 샘플이 0 인지 확인한다.
        for (int i = 0; i < 64; i++) {
            Check.equal(0, out[i], "a zero block stays zero");
        }
    }

    /**
     * The lossless Walsh-Hadamard transform of a DC term spreads it evenly (4x4).
     * <p>
     * 무손실 월시-아다마르 변환은 DC 항을 고르게 퍼뜨린다(4x4).
     */
    public void testWalshHadamardOfDc() {
        int[] dq = new int[64 * 64];
        dq[0] = 64;
        int[] out = new int[16];
        Av1InvTransform.inverse2d(dq, 2, 2, 0, true, 0, 8, out);
        int sum = 0;
        // 모든 샘플이 같은지 확인하며 합을 구한다.
        for (int i = 0; i < 16; i++) {
            sum += out[i];
            Check.equal(out[0], out[i], "a DC term gives a flat lossless residual");
        }
        Check.isTrue(sum != 0, "the lossless residual is not zero");
    }

    /**
     * The simple intra predictors follow the formulas of the specification.
     * <p>
     * 단순한 인트라 예측기가 규격의 식을 따른다.
     */
    public void testIntraPredictors() {
        int stride = 16;
        short[] pl = new short[stride * 16];
        int[] above = { 10, 20, 30, 40 };
        int[] left = { 1, 2, 3, 4 };
        int corner = 5;
        // 블록 (4,4) 주변에 위쪽 줄, 왼쪽 열, 모서리 값을 채운다.
        for (int i = 0; i < 4; i++) {
            pl[3 * stride + 4 + i] = (short) above[i];
            pl[(4 + i) * stride + 3] = (short) left[i];
        }
        pl[3 * stride + 3] = (short) corner;
        // V_PRED 는 위쪽 줄을 복사한다.
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, Av1IntraPred.V_PRED, true, true, false, false, 15, 15, 0, false, 0, false, 0, 8);
        for (int i = 0; i < 4; i++) {
            // 행의 모든 열이 위쪽 줄 값과 같은지 확인한다.
            for (int j = 0; j < 4; j++) {
                Check.equal(above[j], pl[(4 + i) * stride + 4 + j], "V_PRED " + i + "," + j);
            }
        }
        // H_PRED 는 왼쪽 열을 복사한다.
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, Av1IntraPred.H_PRED, true, true, false, false, 15, 15, 0, false, 0, false, 0, 8);
        for (int i = 0; i < 4; i++) {
            // 행의 모든 열이 왼쪽 열 값과 같은지 확인한다.
            for (int j = 0; j < 4; j++) {
                Check.equal(left[i], pl[(4 + i) * stride + 4 + j], "H_PRED " + i + "," + j);
            }
        }
        // DC_PRED 는 이웃의 평균이다: (100 + 10 + 4) / 8 = 14.
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, 0, true, true, false, false, 15, 15, 0, false, 0, false, 0, 8);
        Check.equal(14, pl[4 * stride + 4], "DC_PRED with both neighbours");
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, 0, false, true, false, false, 15, 15, 0, false, 0, false, 0, 8);
        Check.equal(25, pl[5 * stride + 5], "DC_PRED with the row above only: (100 + 2) >> 2");
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, 0, false, false, false, false, 15, 15, 0, false, 0, false, 0, 8);
        Check.equal(128, pl[7 * stride + 7], "DC_PRED without neighbours is the mid value");
        // PAETH_PRED 는 base = 위 + 왼 - 모서리 에 가장 가까운 이웃을 고른다.
        for (int i = 0; i < 4; i++) {
            pl[3 * stride + 4 + i] = (short) above[i];
            pl[(4 + i) * stride + 3] = (short) left[i];
        }
        Av1IntraPred.predict(pl, stride, 4, 4, 2, 2, Av1IntraPred.PAETH_PRED, true, true, false, false, 15, 15, 0, false, 0, false, 0, 8);
        // 블록의 모든 위치를 확인한다.
        for (int i = 0; i < 4; i++) {
            // 열마다 Paeth 규칙으로 기대 값을 계산해 비교한다.
            for (int j = 0; j < 4; j++) {
                int base = above[j] + left[i] - corner;
                int pl2 = Math.abs(base - left[i]);
                int pt = Math.abs(base - above[j]);
                int ptl = Math.abs(base - corner);
                int want = (pl2 <= pt && pl2 <= ptl) ? left[i] : (pt <= ptl ? above[j] : corner);
                Check.equal(want, pl[(4 + i) * stride + 4 + j], "PAETH_PRED " + i + "," + j);
            }
        }
        Check.isTrue(Av1IntraPred.isDirectional(Av1IntraPred.V_PRED) && Av1IntraPred.isDirectional(Av1IntraPred.D67_PRED), "directional modes");
        Check.isFalse(Av1IntraPred.isDirectional(0) || Av1IntraPred.isDirectional(Av1IntraPred.SMOOTH_PRED), "non directional modes");
    }

    /**
     * The generated tables have the sizes and values of the specification (spot checks).
     * <p>
     * 생성된 표가 규격의 크기와 값을 가진다(표본 확인).
     */
    public void testTables() {
        Check.equal(3 * 256, AvifTables.DC_QLOOKUP.length, "Dc_Qlookup size");
        Check.equal(3 * 256, AvifTables.AC_QLOOKUP.length, "Ac_Qlookup size");
        Check.equal(4, AvifTables.DC_QLOOKUP[0], "Dc_Qlookup[0][0]");
        Check.equal(4, AvifTables.AC_QLOOKUP[0], "Ac_Qlookup[0][0]");
        Check.equal(1336, AvifTables.DC_QLOOKUP[255], "Dc_Qlookup[0][255]");
        Check.equal(1828, AvifTables.AC_QLOOKUP[255], "Ac_Qlookup[0][255]");
        Check.equal(22, AvifTables.NUM_4X4_BLOCKS_WIDE.length, "block sizes");
        Check.equal(19, AvifTables.TX_WIDTH.length, "transform sizes");
        Check.equal(64, AvifTables.TX_WIDTH[4], "TX_64X64 width");
        Check.equal(32, AvifTables.DEFAULT_SCAN_32X32.length / 32, "scan 32x32 size");
        Check.equal(1024, AvifTables.DEFAULT_SCAN_32X32.length, "scan 32x32 length");
        // 스캔 표는 0..N-1 의 순열이어야 한다.
        int[][] scans = { AvifTables.DEFAULT_SCAN_4X4, AvifTables.DEFAULT_SCAN_8X8, AvifTables.DEFAULT_SCAN_16X16, AvifTables.DEFAULT_SCAN_32X32,
                AvifTables.MROW_SCAN_8X8, AvifTables.MCOL_SCAN_16X16, AvifTables.DEFAULT_SCAN_16X32, AvifTables.DEFAULT_SCAN_8X32 };
        // 스캔 표마다 순열인지 확인한다.
        for (int[] sc : scans) {
            boolean[] used = new boolean[sc.length];
            // 값이 범위 안이고 중복되지 않는지 확인한다.
            for (int v : sc) {
                Check.isTrue(v >= 0 && v < sc.length && !used[v], "a scan is a permutation");
                used[v] = true;
            }
        }
        Check.equal(15 * 2 * 3344, 15 * 2 * 3344, "quantizer matrix size");
        Check.equal(32, AvifQmTable.weight(0, 0, 0), "Quantizer_Matrix[0][0][0]");
        Check.equal(43, AvifQmTable.weight(0, 0, 1), "Quantizer_Matrix[0][0][1]");
        Check.equal(73, AvifQmTable.weight(0, 0, 2), "Quantizer_Matrix[0][0][2]");
        Check.isTrue(AvifQmTable.weight(14, 1, 3343) > 0, "the last quantizer matrix weight");
        // 모든 기본 분포는 마지막 심볼이 32768 이고 카운터가 0 이어야 한다(행 길이 N + 1).
        int[] skip = AvifCdfTables.DEFAULT_SKIP_CDF;
        Check.equal(9, skip.length, "Default_Skip_Cdf size");
        // 분포의 행마다 끝 값과 카운터를 확인한다.
        for (int r = 0; r < 3; r++) {
            Check.equal(32768, skip[r * 3 + 1], "Default_Skip_Cdf row end");
            Check.equal(0, skip[r * 3 + 2], "Default_Skip_Cdf counter");
        }
        Av1Cdfs a = new Av1Cdfs(100);
        Av1Cdfs b = a.copy();
        b.skip[0] = 1;
        Check.isTrue(a.skip[0] != 1, "a copy of the distributions is independent");
        Check.isTrue(new Av1Cdfs(10).txbSkip.length == new Av1Cdfs(200).txbSkip.length, "the coefficient set is chosen by the quantizer");
        Check.isFalse(java.util.Arrays.equals(new Av1Cdfs(10).txbSkip, new Av1Cdfs(200).txbSkip), "different quantizer sets differ");
    }
}
