package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisBits;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisBook;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Vorbis floor type 0 (line spectral pair floor) against the formulas of the specification, which are evaluated here
 * directly, and of the unused-channel flag.
 * <p>
 * 규격의 공식을 여기서 직접 계산한 값과 비교하는 Vorbis 플로어 유형 0(선 스펙트럼 쌍 플로어) 시험과 사용하지 않는 채널 표시 시험.
 */
public class VorbisFloorTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VorbisFloorTest() {
    }

    /**
     * The Bark scale of the specification.
     * <p>
     * 규격의 Bark 스케일.
     *
     * @param x frequency / 주파수
     * @return Bark value / Bark 값
     */
    private static double bark(double x) {
        return 13.1 * Math.atan(0.00074 * x) + 2.24 * Math.atan(0.0000000185 * x * x) + 0.0001 * x;
    }

    /**
     * Evaluates the floor at one position from the formulas of the specification (products over the coefficient cosines).
     * <p>
     * 규격의 공식(계수 코사인의 곱)으로 한 위치의 플로어 값을 계산한다.
     *
     * @param c      coefficients / 계수
     * @param order  order / 차수
     * @param omega  angle / 각도
     * @param amp    amplitude value / 진폭 값
     * @param bits   amplitude bits / 진폭 비트 수
     * @param offset amplitude offset / 진폭 오프셋
     * @return the linear floor value / 선형 플로어 값
     */
    private static double spec(double[] c, int order, double omega, int amp, int bits, int offset) {
        double cw = Math.cos(omega);
        double p;
        double q;
        if (order % 2 == 1) {
            p = 1 - cw * cw;
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j <= (order - 3) / 2; j++) {
                double t = Math.cos(c[2 * j + 1]) - cw;
                p *= 4 * t * t;
            }
            q = 0.25;
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j <= (order - 1) / 2; j++) {
                double t = Math.cos(c[2 * j]) - cw;
                q *= 4 * t * t;
            }
        } else {
            p = (1 - cw) / 2;
            q = (1 + cw) / 2;
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < order / 2; j++) {
                double t1 = Math.cos(c[2 * j + 1]) - cw;
                double t0 = Math.cos(c[2 * j]) - cw;
                p *= 4 * t1 * t1;
                q *= 4 * t0 * t0;
            }
        }
        double scaled = (double) amp * offset / (((1L << bits) - 1) * Math.sqrt(p + q));
        return Math.exp(0.11512925 * (scaled - offset));
    }

    /**
     * Decodes floors of odd and even order and compares the curves with the formulas of the specification.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCurveMatchesTheSpecification() throws Exception {
        float[][] table = { { 0.3f, 1.2f }, { 0.8f, 2.0f }, { 1.5f, 2.6f }, { 0.2f, 0.9f }, { 0.5f, 1.7f }, { 1.1f, 0.4f }, { 0.05f, 0.3f }, { 0.7f, 0.1f } };
        VorbisBook book = VorbisBook.table(table, null, false);
        int[] orders = { 4, 5, 8, 7 };
        // 차수마다 설정을 쓰고 해석해 패킷의 플로어를 디코딩해 규격의 공식과 비교한다.
        for (int order : orders) {
            int bits = 6;
            int offset = 100;
            int amp = 40;
            VorbisBits cfg = new VorbisBits();
            cfg.put(order, 8);
            cfg.put(44100, 16);
            cfg.put(64, 16);
            cfg.put(bits, 6);
            cfg.put(offset, 8);
            cfg.put(0, 4);
            cfg.put(0, 8);
            VorbisBits bk = new VorbisBits();
            book.write(bk);
            VorbisCodebook[] books = { new VorbisCodebook(new VorbisBitReader(bk.bytes())) };
            VorbisFloor0 floor = new VorbisFloor0(new VorbisBitReader(cfg.bytes()), books);
            VorbisBits packet = new VorbisBits();
            packet.put(amp, bits);
            packet.put(0, 1);
            int vectors = (order + 1) / 2;
            double[] coef = new double[order + 2];
            double last = 0;
            // 벡터 항목을 골라 쓰고 앞 벡터의 마지막 값을 더한 계수를 기대값으로 모은다.
            for (int v = 0; v < vectors; v++) {
                int e = (v * 3 + order) % table.length;
                book.writeEntry(packet, e);
                coef[2 * v] = book.value(e, 0) + last;
                coef[2 * v + 1] = book.value(e, 1) + last;
                last = coef[2 * v + 1];
            }
            int half = 128;
            float[] out = new float[half];
            Check.isTrue(floor.decode(new VorbisBitReader(packet.bytes()), out, half), "the floor is used, order " + order);
            double scale = 64 / bark(0.5 * 44100);
            // 위치마다 Bark 지도를 거친 각도에서 규격의 공식을 계산해 비교한다.
            for (int i = 0; i < half; i += 3) {
                int map = Math.min(63, (int) Math.floor(bark(44100.0 * i / (2.0 * half)) * scale));
                double expected = spec(coef, order, Math.PI * map / 64, amp, bits, offset);
                if (expected > 1e30) {
                    Check.isTrue(out[i] > 1e30f, "a huge floor value stays huge at " + i);
                    continue;
                }
                Check.isTrue(Math.abs(out[i] - expected) <= 1e-4 * Math.abs(expected) + 1e-12, "floor " + i + " of order " + order + ": expected " + expected + " got " + out[i]);
            }
        }
    }

    /**
     * A floor with amplitude zero marks the channel unused.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testZeroAmplitudeMeansUnused() throws Exception {
        VorbisBook book = VorbisBook.table(new float[][] { { 0.3f, 1.2f }, { 0.8f, 2.0f } }, null, false);
        VorbisBits cfg = new VorbisBits();
        cfg.put(4, 8);
        cfg.put(44100, 16);
        cfg.put(64, 16);
        cfg.put(6, 6);
        cfg.put(100, 8);
        cfg.put(0, 4);
        cfg.put(0, 8);
        VorbisBits bk = new VorbisBits();
        book.write(bk);
        VorbisFloor0 floor = new VorbisFloor0(new VorbisBitReader(cfg.bytes()), new VorbisCodebook[] { new VorbisCodebook(new VorbisBitReader(bk.bytes())) });
        VorbisBits packet = new VorbisBits();
        packet.put(0, 6);
        Check.isFalse(floor.decode(new VorbisBitReader(packet.bytes()), new float[64], 64), "amplitude zero");
        Check.isFalse(floor.decode(new VorbisBitReader(new byte[0]), new float[64], 64), "an empty packet");
    }
}
