package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the range decoder of RFC 6716 section 4.1: values worked out by hand from the specification, and round trips through
 * a test-only range encoder (random sequences of every kind of symbol that the decoder reads: inverse CDF symbols, bits of
 * unequal probability, uniform integers with and without raw bits, raw bits, Laplace values) including the equality of the final
 * range coder state of both sides, the behavior at the end of the data and the bit counters.
 * <p>
 * RFC 6716 4.1 범위 디코더의 시험: 규격으로 손으로 계산한 값과, 시험 전용 범위 인코더를 거친 왕복(디코더가 읽는 모든 종류의 기호의 무작위
 * 열: 역 CDF 기호, 확률이 다른 비트, 원시 비트가 있거나 없는 균등 정수, 원시 비트, 라플라스 값). 양쪽 최종 범위 디코더 상태의 일치, 데이터 끝에서의
 * 동작, 비트 카운터를 확인한다.
 */
public class OpusRangeDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusRangeDecoderTest() {
    }

    /**
     * After the initialization on all-zero data the state is known: three bytes are consumed (range 2^31, value 2^31 - 1), one bit has been
     * used, and the first bit with probability 1/2 is 0.
     * <p>
     * 모두 0 인 데이터로 초기화하면 상태가 정해진다: 바이트 셋을 소비하고(범위 2^31, 값 2^31 - 1) 1 비트를 썼으며, 확률 1/2 인 첫 비트는 0 이다.
     */
    public void testInitialStateByHand() {
        OpusRangeDecoder rc = new OpusRangeDecoder(new byte[8], 0, 8);
        Check.equal(1, rc.tell(), "tell after the initialization");
        Check.equal(8, rc.tellFrac(), "tell in 1/8 bits after the initialization");
        Check.equal(0, rc.bitLogp(1), "first bit of zero data");
        Check.equal(1L << 30, rc.getFinalRange() & 0xFFFFFFFFL, "range after one bit of probability 1/2");
    }

    /**
     * A table of the RFC (the three-symbol distribution 2, 1, 0 of the energy residual) decodes the hand-worked symbols of known
     * bytes: with an all-zero payload the decoder value starts at the top of the range, which is the first symbol; with an all-ones
     * payload it starts at 0, the bottom of the range, which is the last symbol.
     * <p>
     * RFC 의 표(에너지 잔차의 기호 세 개 분포 2, 1, 0)가 알려진 바이트에서 손으로 계산한 기호를 디코딩한다: 모두 0 인 페이로드에서는 디코더 값이
     * 범위의 꼭대기에서 시작하여 첫 기호가, 모두 1 인 페이로드에서는 0(범위의 맨 아래)에서 시작하여 마지막 기호가 나온다.
     */
    public void testIcdfOnKnownBytes() {
        int[] icdf = { 2, 1, 0 };
        byte[] ff = new byte[8];
        java.util.Arrays.fill(ff, (byte) 0xFF);
        Check.equal(2, new OpusRangeDecoder(ff, 0, 8).icdf(icdf, 2), "all-ones data selects the last symbol");
        byte[] zero = new byte[8];
        Check.equal(0, new OpusRangeDecoder(zero, 0, 8).icdf(icdf, 2), "all-zero data selects the first symbol");
    }

    /**
     * Reading beyond the end returns zero bytes: a decoder over an empty buffer is the same as one over zero bytes, and never fails.
     * <p>
     * 끝을 넘어 읽으면 0 바이트를 돌려준다: 빈 버퍼의 디코더는 0 바이트 버퍼의 디코더와 같으며 실패하지 않는다.
     */
    public void testReadingPastTheEnd() {
        OpusRangeDecoder empty = new OpusRangeDecoder(new byte[0], 0, 0);
        OpusRangeDecoder zeros = new OpusRangeDecoder(new byte[16], 0, 16);
        // 두 디코더가 같은 기호를 내는지 확인한다.
        for (int i = 0; i < 40; i++) {
            Check.equal(zeros.bitLogp(3), empty.bitLogp(3), "bit " + i);
            Check.equal(zeros.readBits(5), empty.readBits(5), "raw bits " + i);
        }
        Check.equal(zeros.getFinalRange(), empty.getFinalRange(), "final range");
    }

    /**
     * Random sequences of all symbol kinds survive a round trip through the test encoder and end in the same range state.
     * <p>
     * 모든 종류의 기호의 무작위 열이 시험용 인코더를 거친 왕복을 견디고 같은 범위 상태로 끝난다.
     */
    public void testRoundTripOfRandomSymbols() {
        Random rnd = new Random(20261117L);
        int[][] tables = { { 2, 1, 0 }, { 224, 192, 160, 128, 96, 64, 32, 0 }, { 25, 23, 2, 0 }, { 126, 124, 119, 109, 87, 41, 19, 9, 4, 2, 0 } };
        int[] ftbs = { 2, 8, 5, 7 };
        // 길이가 다른 여러 열을 시험한다.
        for (int round = 0; round < 200; round++) {
            int count = 1 + rnd.nextInt(60);
            int[] kind = new int[count];
            long[] a = new long[count];
            long[] b = new long[count];
            OpusRangeEncoder enc = new OpusRangeEncoder(400);
            // 기호를 무작위로 고르며 부호화한다.
            for (int i = 0; i < count; i++) {
                kind[i] = rnd.nextInt(6);
                switch (kind[i]) {
                case 0: {
                    int t = rnd.nextInt(tables.length);
                    a[i] = rnd.nextInt(tables[t].length);
                    b[i] = t;
                    enc.icdf((int) a[i], tables[t], ftbs[t]);
                    break;
                }
                case 1:
                    b[i] = 1 + rnd.nextInt(15);
                    a[i] = rnd.nextInt(2);
                    enc.bitLogp((int) a[i], (int) b[i]);
                    break;
                case 2:
                    b[i] = 2 + (rnd.nextBoolean() ? rnd.nextInt(255) : rnd.nextInt(1 << 20));
                    a[i] = (long) (rnd.nextDouble() * b[i]);
                    enc.uint(a[i], b[i]);
                    break;
                case 3:
                    b[i] = 1 + rnd.nextInt(24);
                    a[i] = rnd.nextInt(1 << (int) b[i]);
                    enc.bits(a[i], (int) b[i]);
                    break;
                case 4: {
                    int fs = 6000 + rnd.nextInt(12000);
                    int decay = 6000 + rnd.nextInt(8000);
                    b[i] = ((long) fs << 16) | decay;
                    int v = rnd.nextInt(9) - 4;
                    a[i] = enc.laplace(v, fs, decay);
                    break;
                }
                default:
                    b[i] = 15;
                    a[i] = rnd.nextInt(32768);
                    // 15 비트 균등 분포의 한 값을 구간 [a, a+1) 로 부호화한다.
                    enc.encodeBin(a[i], a[i] + 1, 15);
                    break;
                }
            }
            int expectedRange = enc.getRange();
            byte[] data = enc.done();
            OpusRangeDecoder dec = new OpusRangeDecoder(data, 0, data.length);
            // 같은 순서로 디코딩해 기호가 같은지 확인한다.
            for (int i = 0; i < count; i++) {
                String where = "round " + round + " symbol " + i + " kind " + kind[i];
                switch (kind[i]) {
                case 0:
                    Check.equal(a[i], dec.icdf(tables[(int) b[i]], ftbs[(int) b[i]]), where);
                    break;
                case 1:
                    Check.equal(a[i], dec.bitLogp((int) b[i]), where);
                    break;
                case 2:
                    Check.equal(a[i], dec.readUint(b[i]), where);
                    break;
                case 3:
                    Check.equal(a[i], dec.readBits((int) b[i]), where);
                    break;
                case 4:
                    Check.equal(a[i], dec.laplace((int) (b[i] >> 16), (int) (b[i] & 0xFFFF)), where);
                    break;
                default: {
                    long v = dec.decodeBin(15);
                    Check.equal(a[i], v, where);
                    dec.update(v, v + 1, 1 << 15);
                    break;
                }
                }
            }
            Check.equal(expectedRange, dec.getFinalRange(), "round " + round + " final range");
            Check.isFalse(dec.hasError(), "round " + round + " error flag");
        }
    }

    /**
     * The bit counters grow with the information that was read: reading a likely bit costs little, an unlikely one a lot, raw bits their number.
     * <p>
     * 비트 카운터는 읽은 정보에 따라 늘어난다: 확률이 높은 비트는 적게, 낮은 비트는 많이, 원시 비트는 그 개수만큼 쓴다.
     */
    public void testBitCounters() {
        OpusRangeEncoder enc = new OpusRangeEncoder(64);
        enc.bitLogp(0, 15);
        enc.bitLogp(1, 15);
        enc.bits(0x155, 9);
        byte[] data = enc.done();
        OpusRangeDecoder dec = new OpusRangeDecoder(data, 0, data.length);
        int t0 = dec.tellFrac();
        Check.equal(0, dec.bitLogp(15), "likely bit");
        int t1 = dec.tellFrac();
        Check.isTrue(t1 - t0 <= 1, "a bit of probability 1 - 2^-15 costs almost nothing, cost " + (t1 - t0) + "/8 bit");
        Check.equal(1, dec.bitLogp(15), "unlikely bit");
        int t2 = dec.tellFrac();
        Check.isTrue(t2 - t1 >= 14 * 8, "a bit of probability 2^-15 costs about 15 bits, cost " + (t2 - t1) + "/8 bit");
        Check.equal(0x155L, dec.readBits(9), "raw bits");
        Check.equal(t2 + 9 * 8, dec.tellFrac(), "nine raw bits cost nine bits");
    }

    /**
     * Shortening the data (the removal of redundancy bytes) lowers the storage that the bit budget is based on.
     * <p>
     * 데이터를 줄이면(중복 바이트 제거) 비트 예산의 기준이 되는 크기가 줄어든다.
     */
    public void testShrink() {
        OpusRangeDecoder dec = new OpusRangeDecoder(new byte[20], 0, 20);
        Check.equal(20, dec.getStorage(), "storage");
        dec.shrink(5);
        Check.equal(15, dec.getStorage(), "storage after shrink");
    }
}
