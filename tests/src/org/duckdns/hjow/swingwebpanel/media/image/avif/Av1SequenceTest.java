package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the AV1 inter frame decoding with real sequences (plan item P18-03; FEATURE_MATRIX IMG-09): every sequence of
 * <code>/media/avif/seq/index.txt</code> was made by an encoder (libaom) and decoded by libdav1d; the md5 of every decoded frame must be
 * identical, that is, the decoding of the inter frames (reference frame management, motion vector prediction, motion compensation, compound
 * prediction, overlapped and warped motion, the filters of inter frames) is bit exact.
 * <p>
 * 실제 시퀀스로 AV1 인터 프레임 디코딩을 시험한다(계획 P18-03; FEATURE_MATRIX IMG-09): <code>/media/avif/seq/index.txt</code> 의 모든 시퀀스는
 * 인코더(libaom)가 만들고 libdav1d 가 디코딩한 것이며 디코딩한 프레임마다 md5 가 같아야 한다. 즉 인터 프레임 디코딩(참조 프레임 관리, 움직임 벡터
 * 예측, 움직임 보상, 복합 예측, 겹침과 워프 움직임, 인터 프레임의 필터)이 비트 단위로 같다.
 */
public class Av1SequenceTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Av1SequenceTest() {
    }

    /**
     * Every frame of every sequence decodes to exactly the planes of the reference decoder.
     * <p>
     * 모든 시퀀스의 모든 프레임이 기준 디코더와 정확히 같은 평면으로 디코딩된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testBitExactAgainstDav1d() throws Exception {
        List<String> lines = AvifTestKit.indexLines("/media/avif/seq/index.txt");
        int frames = 0;
        // 색인의 시퀀스마다 모든 프레임을 디코딩하여 프레임별 크기와 md5 를 비교한다.
        for (String line : lines) {
            String[] f = line.split("\\|");
            byte[] data = AvifTestKit.resource("/media/avif/seq/" + f[0] + ".obu");
            List<Av1Picture> pics = Av1Decoder.decodeAll(data, 0, data.length, DecodeLimits.DEFAULT, null);
            String[] entries = f[6].split(",");
            Check.equal(Integer.parseInt(f[5]), pics.size(), f[0] + ": number of frames");
            // 프레임마다 크기와 md5 를 비교한다.
            for (int i = 0; i < pics.size(); i++) {
                Av1Picture p = pics.get(i);
                String[] e = entries[i].split(":");
                Check.equal(e[0], p.width + "x" + p.height, f[0] + ": size of frame " + i);
                Check.equal(e[1], AvifTestKit.md5(p), f[0] + ": md5 of frame " + i + " (" + f[7] + ")");
                frames++;
            }
        }
        Check.isTrue(frames >= 1, "at least one frame is checked");
    }

    /**
     * Decodes a sequence and returns how often the decoder met a tool.
     * <p>
     * 시퀀스를 디코딩하고 디코더가 도구를 몇 번 만났는지 반환한다.
     *
     * @param name the name of the sequence / 시퀀스 이름
     * @param tool the counter of {@link Av1ToolStats} / {@link Av1ToolStats} 의 카운터
     * @return the number of uses / 사용 횟수
     * @throws Exception on a failure / 실패 시
     */
    private static int uses(String name, int tool) throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/seq/" + name + ".obu");
        Av1ToolStats.reset();
        Av1Decoder.decodeAll(data, 0, data.length, DecodeLimits.DEFAULT, null);
        return Av1ToolStats.get(tool);
    }

    /**
     * The sequences really use the tools that they are meant to cover (a bit exact result says little if the tool never occurred).
     * <p>
     * 시퀀스가 덮으려는 도구를 실제로 쓴다(도구가 한 번도 나오지 않으면 비트 단위 일치는 의미가 적다).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testSequencesCoverTheTools() throws Exception {
        Check.isTrue(uses("seq_obmc", Av1ToolStats.OBMC) > 20, "overlapped block motion compensation");
        Check.isTrue(uses("seq_warp", Av1ToolStats.LOCAL_WARP) > 0, "local warped motion");
        Check.isTrue(uses("seq_global", Av1ToolStats.GLOBAL_WARP) > 20, "global motion");
        Check.isTrue(uses("seq_interintra", Av1ToolStats.INTERINTRA) > 0, "inter-intra prediction");
        Check.isTrue(uses("seq_interintra", Av1ToolStats.INTERINTRA_WEDGE) > 0, "wedge inter-intra prediction");
        Check.isTrue(uses("seq_masked", Av1ToolStats.COMPOUND_WEDGE) > 0, "wedge compound prediction");
        Check.isTrue(uses("seq_masked", Av1ToolStats.COMPOUND_DIFFWTD) > 0, "difference weighted compound prediction");
        Check.isTrue(uses("seq_jnt", Av1ToolStats.COMPOUND_DISTANCE) > 0, "distance weighted compound prediction");
        Check.isTrue(uses("seq_altref", Av1ToolStats.COMPOUND_AVERAGE) > 20, "averaged compound prediction");
        Check.isTrue(uses("seq_altref", Av1ToolStats.SKIP_MODE) > 20, "skip mode");
        Check.isTrue(uses("seq_refmvs", Av1ToolStats.TEMPORAL_MV) > 100, "temporal motion vector candidates");
        Check.isTrue(uses("seq_resize", Av1ToolStats.SCALED_REFERENCE) > 20, "scaled reference frames");
        Check.isTrue(uses("seq_superres", Av1ToolStats.SCALED_REFERENCE) > 20, "references of super resolution frames");
        Check.isTrue(uses("seq_rav1e", Av1ToolStats.SEGMENT) > 20, "segmentation");
        Check.isTrue(uses("seq_basic8", Av1ToolStats.INTRA_IN_INTER) > 20, "intra blocks in inter frames");
    }

    /**
     * Sequences with damaged bytes either decode (to some pictures) or fail with a {@link MediaException}; they never throw another exception
     * and never run for long.
     * <p>
     * 바이트가 손상된 시퀀스는 (어떤 화면으로) 디코딩되거나 {@link MediaException} 으로 실패한다. 다른 예외를 던지거나 오래 실행되지 않는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedSequences() throws Exception {
        String[] names = { "seq_default", "seq_default_mandel", "seq_resize", "seq_superres", "seq_altref", "seq_obmc", "seq_warp", "seq_masked",
                "seq_rav1e", "seq_svt", "seq_tiles", "seq_444", "seq_10bit", "seq_refmvs" };
        java.util.Random rnd = new java.util.Random(4711);
        long start = System.currentTimeMillis();
        int decoded = 0;
        int refused = 0;
        // 시퀀스마다 바이트 몇 개를 임의로 바꾸어 여러 번 디코딩한다.
        for (int n = 0; n < names.length; n++) {
            byte[] orig = AvifTestKit.resource("/media/avif/seq/" + names[n] + ".obu");
            // 손상시킨 복사본을 여러 번 디코딩한다.
            for (int round = 0; round < 40; round++) {
                byte[] d = orig.clone();
                int flips = 1 + rnd.nextInt(6);
                // 임의 위치의 비트를 뒤집는다.
                for (int k = 0; k < flips; k++) {
                    d[rnd.nextInt(d.length)] ^= (byte) (1 << rnd.nextInt(8));
                }
                try {
                    Av1Decoder.decodeAll(d, 0, d.length, DecodeLimits.DEFAULT, null);
                    decoded++;
                } catch (MediaException e) {
                    Check.notNull(e.getKind(), "a MediaException with a kind");
                    refused++;
                }
            }
        }
        Check.isTrue(System.currentTimeMillis() - start < 120000, "the damaged sequences are rejected or decoded quickly");
        Check.isTrue(decoded + refused == names.length * 40, "every damaged sequence ended");
    }
}
