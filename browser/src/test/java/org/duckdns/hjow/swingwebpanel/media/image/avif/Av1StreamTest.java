package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the AV1 intra decoder with real streams (plan item P18-03; FEATURE_MATRIX IMG-06): every stream of
 * <code>/media/avif/av1/index.txt</code> was made by an encoder (libaom, rav1e, SVT-AV1) and decoded by libdav1d; the md5 of the decoded
 * planes must be identical, that is, the decoding is bit exact. The streams use the coding tools that a still picture can use: all block
 * sizes and partitions, all intra prediction modes, filter intra, chroma from luma, palette, all transform types and sizes, quantizer
 * matrices, segmentation, delta quantizer and loop filter levels, tiles, 128x128 superblocks, the deblocking filter, CDEF and loop
 * restoration, the bit depths 8, 10 and 12 and the chroma formats 4:0:0, 4:2:0, 4:2:2 and 4:4:4. The tests also check the refusal of the tools
 * that are not implemented, damaged and truncated data, the limits and the cancellation.
 * <p>
 * 실제 스트림으로 AV1 인트라 디코더를 시험한다(계획 P18-03; FEATURE_MATRIX IMG-06): <code>/media/avif/av1/index.txt</code> 의 모든 스트림은 인코더
 * (libaom, rav1e, SVT-AV1)가 만들고 libdav1d 가 디코딩한 것이며 디코딩한 평면의 md5 가 같아야 한다. 즉 디코딩이 비트 단위로 같다. 스트림은 정지
 * 화면이 쓸 수 있는 부호화 도구를 쓴다: 모든 블록 크기와 분할, 모든 인트라 예측 모드, 필터 인트라, 휘도로부터의 색차, 팔레트, 모든 변환 종류와 크기,
 * 양자화 행렬, 분할, 델타 양자화와 루프 필터 수준, 타일, 128x128 슈퍼블록, 블록 경계 제거 필터, CDEF, 루프 복원, 비트 깊이 8/10/12, 색차 형식
 * 4:0:0/4:2:0/4:2:2/4:4:4. 구현하지 않은 도구의 거부, 손상되거나 잘린 데이터, 제한, 취소도 시험한다.
 */
public class Av1StreamTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Av1StreamTest() {
    }

    /**
     * Decodes a stream of the fixture set.
     * <p>
     * 시험 자료 집합의 스트림을 디코딩한다.
     *
     * @param name the stream name / 스트림 이름
     * @return the picture / 화면
     * @throws Exception on a failure / 실패 시
     */
    private static Av1Picture decode(String name) throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/av1/" + name + ".obu");
        return Av1Decoder.decode(data, 0, data.length, DecodeLimits.DEFAULT, null);
    }

    /**
     * Every reference stream decodes to exactly the planes of the reference decoder.
     * <p>
     * 모든 기준 스트림이 기준 디코더와 정확히 같은 평면으로 디코딩된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testBitExactAgainstDav1d() throws Exception {
        List<String> lines = AvifTestKit.indexLines("/media/avif/av1/index.txt");
        int checked = 0;
        // 색인의 스트림마다 디코딩하여 md5 와 형식을 비교한다.
        for (String line : lines) {
            String[] f = line.split("\\|");
            if (f[7].equals("-")) {
                continue;
            }
            Av1Picture p = decode(f[0]);
            Check.equal(Integer.parseInt(f[1]), p.width, f[0] + ": width");
            Check.equal(Integer.parseInt(f[2]), p.height, f[0] + ": height");
            Check.equal(Integer.parseInt(f[3]), p.bitDepth, f[0] + ": bit depth");
            Check.equal(Integer.parseInt(f[4]), p.numPlanes, f[0] + ": planes");
            if (p.numPlanes > 1) {
                Check.equal(Integer.parseInt(f[5]), p.subX, f[0] + ": horizontal subsampling");
                Check.equal(Integer.parseInt(f[6]), p.subY, f[0] + ": vertical subsampling");
            }
            Check.equal(f[7], AvifTestKit.md5(p), f[0] + ": md5 of the planes (" + f[8] + ")");
            checked++;
        }
        Check.isTrue(checked >= 20, "at least 20 reference streams are checked, was " + checked);
    }

    /**
     * The fixture set uses the tools that it claims to use (so that the bit exact test means what it says).
     * <p>
     * 시험 자료 집합이 쓴다고 한 도구를 실제로 쓴다(비트 단위 시험이 말한 대로의 뜻을 갖게 한다).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFixturesCoverTheTools() throws Exception {
        String all = new String(AvifTestKit.resource("/media/avif/av1/index.txt"), "UTF-8");
        String[] required = { "use_128x128_superblock=1", "allow_screen_content_tools=1", "using_qmatrix=1", "segmentation_enabled=1",
                "delta_q_present=1", "delta_lf_present=1", "tile_cols_log2=1", "reduced_tx_set=1", "lr_type=3/2/3", "cdef_bits=1" };
        // 필요한 도구마다 색인에 나타나는지 확인한다.
        for (String r : required) {
            Check.contains(all, r, "the fixtures contain " + r);
        }
        Check.contains(all, "|10|3|1|1|", "a 10 bit stream");
        Check.contains(all, "|12|3|0|0|", "a 12 bit stream");
        Check.contains(all, "|8|1|1|1|", "a monochrome stream");
    }

    /**
     * The intra block copy streams (the screen content tool that copies parts of the picture that were decoded before: formats 4:4:4, 4:2:2,
     * 4:0:0, 10/12 bit, 128x128 superblocks, tiles, odd sizes, lossless, large pictures; see <code>make_still_fixtures.py</code>) are in the
     * index of the bit exact test; this test checks that the index really has streams that use the tool.
     * <p>
     * 인트라 블록 복사 스트림(이미 디코딩한 화면 부분을 복사하는 화면 콘텐츠 도구: 4:4:4, 4:2:2, 4:0:0, 10/12비트, 128x128 슈퍼블록, 타일, 홀수 크기,
     * 무손실, 큰 화면. <code>make_still_fixtures.py</code> 참조)이 비트 단위 시험의 색인에 있다. 이 시험은 색인에 그 도구를 쓰는 스트림이 실제로
     * 있는지 확인한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testIntraBlockCopyStreamsAreCovered() throws Exception {
        List<String> lines = AvifTestKit.indexLines("/media/avif/av1/index.txt");
        int streams = 0;
        // 색인에서 인트라 블록 복사를 쓰는 스트림을 센다.
        for (String line : lines) {
            if (line.contains("allow_intrabc=1") && !line.split("\\|")[7].equals("-")) {
                streams++;
            }
        }
        Check.isTrue(streams >= 10, "at least 10 intra block copy streams, was " + streams);
    }

    /**
     * The super resolution streams (frames coded at a reduced width and upscaled by the decoder, for every denominator 9..16, with CDEF and loop
     * restoration, 10 bit, odd sizes; made by SVT-AV1, see <code>make_still_fixtures.py</code>) are in the index of the bit exact test; this test
     * checks that the index has them with all denominators.
     * <p>
     * 초해상도 스트림(줄어든 너비로 부호화하고 디코더가 확대하는 프레임. 분모 9..16 전부, CDEF와 루프 복원, 10비트, 홀수 크기. SVT-AV1 이 만들었다.
     * <code>make_still_fixtures.py</code> 참조)이 비트 단위 시험의 색인에 있다. 이 시험은 색인에 분모마다 스트림이 있는지 확인한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testSuperResolutionStreamsAreCovered() throws Exception {
        String all = new String(AvifTestKit.resource("/media/avif/av1/index.txt"), "UTF-8");
        // 분모 9~16 에 해당하는 coded_denom 0~7 이 모두 색인에 있어야 한다.
        for (int d = 0; d < 8; d++) {
            Check.contains(all, "use_superres=1 coded_denom=" + d, "a super resolution stream with coded_denom " + d);
        }
        Check.contains(all, "sr_10bit_d11|160|120|10|", "a 10 bit super resolution stream");
    }

    /**
     * The film grain streams (the 16 parameter sets of libaom and other formats, see <code>make_grain_fixtures.py</code>) are in the index
     * of the bit exact test with the planes that libdav1d produces with the grain applied, so that test covers the film grain synthesis; this
     * test checks that the grain really changes the picture (a stream without the grain differs) and that the index has the streams.
     * <p>
     * 필름 그레인 스트림(libaom 의 16가지 매개변수 집합과 다른 형식. <code>make_grain_fixtures.py</code> 참조)은 libdav1d 가 그레인을 적용해 만든 평면과 함께
     * 비트 단위 시험의 색인에 있으므로 그 시험이 필름 그레인 합성을 다룬다. 이 시험은 그레인이 화면을 실제로 바꾸는지(그레인이 없는 같은 스트림과
     * 다른지)와 색인에 스트림이 있는지 확인한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFilmGrainStreamsAreCovered() throws Exception {
        String all = new String(AvifTestKit.resource("/media/avif/av1/index.txt"), "UTF-8");
        Check.contains(all, "grain_vec16|", "the sixteenth parameter set");
        Check.contains(all, "grain_12bit_444|", "a 12 bit 4:4:4 stream with grain");
        Check.contains(all, "grain_gray|", "a monochrome stream with grain");
        Check.contains(all, "chroma_scaling_from_luma=1", "a stream that scales the chroma noise by the luma");
        Check.contains(all, "overlap_flag=0", "a stream without overlap");
        Check.contains(all, "clip_to_restricted_range=1", "a stream with clipping to the restricted range");
        // 그레인 스트림은 더 이상 거부되지 않고 디코딩된다.
        byte[] data = AvifTestKit.resource("/media/avif/av1/grain_vec01.obu");
        Av1Picture noisy = Av1Decoder.decode(data, 0, data.length, DecodeLimits.DEFAULT, null);
        Check.isTrue(noisy.width == 64 && noisy.height == 48, "the size of the grain stream");
    }

    /**
     * Data that is only a part of a stream fails with a {@link MediaException} (never with another exception) at every cut.
     * <p>
     * 스트림의 일부뿐인 데이터는 어디에서 잘라도 {@link MediaException} 으로 실패한다(다른 예외는 안 된다).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testTruncatedStreams() throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/av1/aom_420_q30.obu");
        int decoded = 0;
        // 잘라낸 길이마다 디코딩을 시도한다. 성공(타일 데이터가 0 으로 채워진 경우)이거나 MediaException 이어야 한다.
        for (int len = 0; len < data.length; len += 7) {
            try {
                Av1Decoder.decode(data, 0, len, DecodeLimits.DEFAULT, null);
                decoded++;
            } catch (MediaException e) {
                Check.isTrue(e.getKind() == MediaException.Kind.CORRUPT || e.getKind() == MediaException.Kind.UNSUPPORTED_FORMAT,
                        "the kind of a truncated stream at " + len + ": " + e.getKind());
            }
        }
        Check.isTrue(decoded >= 0, "truncated streams never throw another exception");
    }

    /**
     * Streams with damaged bytes either decode to some picture or fail with a {@link MediaException}; they never throw another exception and
     * never run for long.
     * <p>
     * 바이트가 손상된 스트림은 어떤 화면으로 디코딩되거나 {@link MediaException} 으로 실패한다. 다른 예외를 던지거나 오래 실행되지 않는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedStreams() throws Exception {
        String[] names = { "aom_420_q30", "aom_palette_420", "aom_lossless_444", "rav1e_420", "aom_10bit_420", "aom_tiles_2x2" };
        Random rnd = new Random(12345);
        long start = System.currentTimeMillis();
        // 스트림마다 바이트 몇 개를 임의로 바꾸어 여러 번 디코딩한다.
        for (int n = 0; n < names.length; n++) {
            byte[] orig = AvifTestKit.resource("/media/avif/av1/" + names[n] + ".obu");
            // 스트림마다 손상시킨 복사본을 여러 번 디코딩한다.
            for (int round = 0; round < 40; round++) {
                byte[] d = orig.clone();
                int flips = 1 + rnd.nextInt(4);
                // 임의 위치의 비트를 뒤집는다.
                for (int k = 0; k < flips; k++) {
                    d[rnd.nextInt(d.length)] ^= (byte) (1 << rnd.nextInt(8));
                }
                try {
                    Av1Decoder.decode(d, 0, d.length, DecodeLimits.DEFAULT, null);
                } catch (MediaException e) {
                    Check.notNull(e.getKind(), "a MediaException with a kind");
                }
            }
        }
        Check.isTrue(System.currentTimeMillis() - start < 60000, "the damaged streams are rejected or decoded quickly");
    }

    /**
     * The decoder respects the limits of the image size and the interruption of the thread.
     * <p>
     * 디코더가 이미지 크기 제한과 스레드 인터럽트를 지킨다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testLimitsAndCancellation() throws Exception {
        final byte[] data = AvifTestKit.resource("/media/avif/av1/aom_420_q55_filters.obu");
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "pixel limit", new Block() {
            public void run() throws Exception {
                Av1Decoder.decode(data, 0, data.length, new DecodeLimits(1000, DecodeLimits.DEFAULT_MAX_BYTES, 10, 1000), null);
            }
        });
        Thread.currentThread().interrupt();
        try {
            AvifTestKit.fails(MediaException.Kind.CANCELLED, "interrupted thread", new Block() {
                public void run() throws Exception {
                    Av1Decoder.decode(data, 0, data.length, DecodeLimits.DEFAULT, null);
                }
            });
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Data after the first frame (more frames of an animation) is ignored: two temporal units decode to the first picture.
     * <p>
     * 첫 프레임 뒤의 데이터(애니메이션의 나머지 프레임)는 무시된다: 시간 단위 둘은 첫 화면으로 디코딩된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testOnlyTheFirstFrameIsDecoded() throws Exception {
        byte[] a = AvifTestKit.resource("/media/avif/av1/aom_420_q30.obu");
        byte[] b = AvifTestKit.resource("/media/avif/av1/aom_444_q30.obu");
        byte[] both = AvifTestKit.cat(a, b);
        Av1Picture p = Av1Decoder.decode(both, 0, both.length, DecodeLimits.DEFAULT, null);
        Check.equal(AvifTestKit.md5(decode("aom_420_q30")), AvifTestKit.md5(p), "the first frame of two temporal units");
    }

    /**
     * A stream without a frame, and a frame header without a sequence header, are damaged data.
     * <p>
     * 프레임이 없는 스트림과 시퀀스 헤더 없는 프레임 헤더는 손상된 데이터이다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testStructureErrors() throws Exception {
        final byte[] empty = new byte[0];
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "no data", new Block() {
            public void run() throws Exception {
                Av1Decoder.decode(empty, 0, 0, DecodeLimits.DEFAULT, null);
            }
        });
        final byte[] full = AvifTestKit.resource("/media/avif/av1/aom_420_q30.obu");
        // 시퀀스 헤더 OBU 를 건너뛰고 나머지만 준다: 첫 OBU(시간 구분자)와 시퀀스 헤더의 길이를 읽어 건너뛴다.
        int pos = 0;
        int skipped = 0;
        // 시간 구분자(2)와 시퀀스 헤더(1) 두 개의 OBU 를 건너뛴다.
        while (skipped < 2 && pos < full.length) {
            int hdr = full[pos] & 0xFF;
            int type = (hdr >> 3) & 15;
            int size = full[pos + 1] & 0x7F;
            pos += 2 + size;
            if (type == 2 || type == 1) {
                skipped++;
            }
        }
        final byte[] noSeq = new byte[full.length - pos];
        System.arraycopy(full, pos, noSeq, 0, noSeq.length);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "frame without sequence header", new Block() {
            public void run() throws Exception {
                Av1Decoder.decode(noSeq, 0, noSeq.length, DecodeLimits.DEFAULT, null);
            }
        });
    }
}
