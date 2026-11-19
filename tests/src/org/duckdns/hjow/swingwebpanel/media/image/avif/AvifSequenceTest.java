package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the animated AVIF files (image sequences, plan item P18-03; FEATURE_MATRIX IMG-09): the files of
 * <code>/media/avif/avis</code> wrap AV1 sequences that are checked bit for bit against libdav1d (see {@link Av1SequenceTest}) in a movie box
 * with a track; the decoder has to read the samples, the durations and the repeat count from the container and to decode the inter frames in
 * order. The expected pictures are those of the raw AV1 sequence (converted like a still image), the expected durations and plays were read
 * from the container by the generation script, not by the decoder under test.
 * <p>
 * 애니메이션 AVIF 파일(이미지 시퀀스, 계획 P18-03; FEATURE_MATRIX IMG-09)의 시험: <code>/media/avif/avis</code> 의 파일은 libdav1d 와 비트 단위로
 * 비교한({@link Av1SequenceTest} 참조) AV1 시퀀스를 트랙이 있는 무비 상자로 감싼 것이다. 디코더는 컨테이너에서 샘플, 지속 시간, 반복 횟수를 읽고
 * 인터 프레임을 순서대로 디코딩해야 한다. 기대하는 화면은 원본 AV1 시퀀스의 화면(정지 이미지처럼 변환)이고 기대하는 지속 시간과 재생 횟수는
 * 시험 대상 디코더가 아니라 생성 스크립트가 컨테이너에서 읽은 것이다.
 */
public class AvifSequenceTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AvifSequenceTest() {
    }

    /**
     * Decodes a file of the animated fixtures.
     * <p>
     * 애니메이션 시험 자료의 파일을 디코딩한다.
     *
     * @param name the name without extension / 확장자 없는 이름
     * @return the image / 이미지
     * @throws Exception on a failure / 실패 시
     */
    private static DecodedImage decode(String name) throws Exception {
        byte[] data = AvifTestKit.resource("/media/avif/avis/" + name + ".avif");
        return new AvifDecoder().decode(data, DecodeLimits.DEFAULT);
    }

    /**
     * Converts the pictures of a raw AV1 sequence to the pixels that the decoder must produce.
     * <p>
     * 원본 AV1 시퀀스의 화면을 디코더가 만들어야 하는 픽셀로 바꾼다.
     *
     * @param color the name of the colour sequence / 색 시퀀스 이름
     * @param alpha the name of the alpha sequence or "-" / 알파 시퀀스 이름 또는 "-"
     * @return the pixels of every frame / 프레임별 픽셀
     * @throws Exception on a failure / 실패 시
     */
    private static int[][] expectedFrames(String color, String alpha) throws Exception {
        byte[] c = AvifTestKit.resource("/media/avif/seq/" + color + ".obu");
        List<Av1Picture> pics = Av1Decoder.decodeAll(c, 0, c.length, DecodeLimits.DEFAULT, null);
        List<Av1Picture> alphas = null;
        if (!alpha.equals("-")) {
            byte[] a = AvifTestKit.resource("/media/avif/seq/" + alpha + ".obu");
            alphas = Av1Decoder.decodeAll(a, 0, a.length, DecodeLimits.DEFAULT, null);
        }
        int[][] out = new int[pics.size()][];
        // 화면마다 알파가 있으면 함께 ARGB 로 바꾼다(색 설명이 지정되지 않은 시퀀스이므로 sRGB 변환은 없다).
        for (int i = 0; i < out.length; i++) {
            out[i] = AvifColor.toArgb(pics.get(i), alphas == null ? null : alphas.get(i), false);
        }
        return out;
    }

    /**
     * Every file decodes to the frames, the sizes, the durations and the repeat count that the container says, and every frame has exactly the
     * pixels of the raw sequence (which matches libdav1d bit for bit).
     * <p>
     * 모든 파일이 컨테이너가 말하는 프레임, 크기, 지속 시간, 반복 횟수로 디코딩되며 모든 프레임이 (libdav1d 와 비트 단위로 일치하는) 원본 시퀀스의
     * 픽셀과 정확히 같다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testAnimatedFilesMatchTheRawSequences() throws Exception {
        List<String> lines = AvifTestKit.indexLines("/media/avif/avis/index.txt");
        int checked = 0;
        // 색인의 파일마다 디코딩하여 프레임 수, 크기, 지속 시간, 재생 횟수, 픽셀을 대조한다.
        for (String line : lines) {
            String[] f = line.split("\\|");
            String name = f[0];
            if (name.equals("avis_varsize")) {
                continue;
            }
            DecodedImage img = decode(name);
            int frames = Integer.parseInt(f[3]);
            String[] size = f[4].split("x");
            Check.equal(frames, img.getFrames().size(), name + ": number of frames");
            Check.equal(Integer.parseInt(size[0]), img.getWidth(), name + ": width");
            Check.equal(Integer.parseInt(size[1]), img.getHeight(), name + ": height");
            Check.equal(Integer.parseInt(f[6]), img.getLoopCount(), name + ": number of plays");
            Check.isTrue(img.isAnimated(), name + ": animated");
            String[] delays = f[5].split(",");
            int[][] expected = expectedFrames(f[1], f[2]);
            // 프레임마다 지속 시간과 픽셀을 비교한다.
            for (int i = 0; i < frames; i++) {
                DecodedFrame fr = img.getFrames().get(i);
                Check.equal(Integer.parseInt(delays[i]), fr.getDelayMillis(), name + ": delay of frame " + i);
                Check.isTrue(java.util.Arrays.equals(expected[i], fr.getArgb()), name + ": pixels of frame " + i);
                checked++;
            }
        }
        Check.isTrue(checked >= 50, "enough frames were compared: " + checked);
    }

    /**
     * The alpha track gives the alpha values of the frames (the monochrome sequence is the alpha plane, in the full range of the 8 bit sample).
     * <p>
     * 알파 트랙이 프레임의 알파 값을 준다(단색 시퀀스가 알파 평면이다).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testAlphaTrack() throws Exception {
        DecodedImage img = decode("avis_alpha");
        int distinct = 0;
        int last = -1;
        // 알파가 프레임마다 실제로 투명도를 담고 있는지(모두 불투명이 아닌지) 확인한다.
        for (DecodedFrame fr : img.getFrames()) {
            int[] px = fr.getArgb();
            // 7 픽셀마다 알파 값이 앞 값과 다른 횟수를 센다.
            for (int i = 0; i < px.length; i += 7) {
                int a = px[i] >>> 24;
                if (a != last) {
                    distinct++;
                    last = a;
                }
            }
        }
        Check.isTrue(distinct > 10, "the alpha values vary: " + distinct);
    }

    /**
     * The first frame of the animation is the picture of the primary item (the same AV1 data decoded by the still image path).
     * <p>
     * 애니메이션의 첫 프레임은 기본 항목의 화면과 같다(같은 AV1 데이터를 정지 이미지 경로로 디코딩한 것).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFirstFrameEqualsThePrimaryItem() throws Exception {
        String[] names = { "avis_default", "avis_10bit", "avis_tiles", "avis_odd" };
        // 파일마다 moov 상자를 free 로 바꾸어 기본 항목만 있는 파일로 만들고 정지 이미지 경로의 결과와 비교한다.
        for (String name : names) {
            byte[] data = AvifTestKit.resource("/media/avif/avis/" + name + ".avif");
            DecodedImage anim = new AvifDecoder().decode(data, DecodeLimits.DEFAULT);
            byte[] still = data.clone();
            int pos = indexOf(still, "moov");
            Check.isTrue(pos > 0, name + ": has a movie box");
            still[pos] = 'f';
            still[pos + 1] = 'r';
            still[pos + 2] = 'e';
            still[pos + 3] = 'e';
            DecodedImage one = new AvifDecoder().decode(still, DecodeLimits.DEFAULT);
            Check.isFalse(one.isAnimated(), name + ": the primary item is a still image");
            Check.isTrue(java.util.Arrays.equals(one.getFrames().get(0).getArgb(), anim.getFrames().get(0).getArgb()), name + ": first frame");
        }
    }

    /**
     * Finds a four character type in a file.
     * <p>
     * 파일에서 네 글자 종류를 찾는다.
     *
     * @param d    the data / 데이터
     * @param type the type / 종류
     * @return the position or -1 / 위치 또는 -1
     */
    private static int indexOf(byte[] d, String type) {
        // 모든 위치에서 네 바이트가 같은지 본다.
        for (int i = 0; i + 4 <= d.length; i++) {
            if (d[i] == type.charAt(0) && d[i + 1] == type.charAt(1) && d[i + 2] == type.charAt(2) && d[i + 3] == type.charAt(3)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A file without a primary item decodes from its track; a sequence whose frames change their size is not supported as a sequence, and the
     * decoder shows the primary item instead.
     * <p>
     * 기본 항목이 없는 파일은 트랙에서 디코딩된다. 프레임의 크기가 바뀌는 시퀀스는 시퀀스로는 지원하지 않으며 디코더가 대신 기본 항목을 보여 준다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFallbackAndMissingPrimaryItem() throws Exception {
        DecodedImage nometa = decode("avis_nometa");
        Check.equal(8, nometa.getFrames().size(), "a file without meta box: frames");
        DecodedImage varsize = decode("avis_varsize");
        Check.isFalse(varsize.isAnimated(), "frames of different sizes: shown as a still image");
        Check.equal(160, varsize.getWidth(), "the still image has the size of the first frame");
        int[][] expected = expectedFrames("seq_resize_fixed", "-");
        Check.isTrue(java.util.Arrays.equals(expected[0], varsize.getFrames().get(0).getArgb()), "the first frame");
        // 기본 항목도 없고 크기도 바뀌는 파일은 UNSUPPORTED_FORMAT 으로 실패한다.
        final byte[] data = AvifTestKit.resource("/media/avif/avis/avis_varsize.avif").clone();
        int meta = indexOf(data, "meta");
        data[meta] = 'f';
        data[meta + 1] = 'r';
        data[meta + 2] = 'e';
        data[meta + 3] = 'e';
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "different frame sizes without a primary item", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(data, DecodeLimits.DEFAULT);
            }
        });
    }

    /**
     * The registry decodes animated files and the limits of the frame count and of the total number of pixels apply.
     * <p>
     * 등록부가 애니메이션 파일을 디코딩하며 프레임 수와 전체 픽셀 수의 제한이 적용된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testRegistryAndLimits() throws Exception {
        final byte[] data = AvifTestKit.resource("/media/avif/avis/avis_default.avif");
        DecodedImage img = ImageDecoders.decode(data, "image/avif", null);
        Check.equal(12, img.getFrames().size(), "decoded by the registry");
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "frame limit", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(data, new DecodeLimits(DecodeLimits.DEFAULT_MAX_PIXELS, DecodeLimits.DEFAULT_MAX_BYTES, 5,
                        DecodeLimits.DEFAULT_MAX_TOTAL_PIXELS));
            }
        });
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "total pixel limit", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(data, new DecodeLimits(DecodeLimits.DEFAULT_MAX_PIXELS, DecodeLimits.DEFAULT_MAX_BYTES, 100, 160 * 120 * 3));
            }
        });
        Thread.currentThread().interrupt();
        try {
            AvifTestKit.fails(MediaException.Kind.CANCELLED, "interrupted thread", new Block() {
                public void run() throws Exception {
                    new AvifDecoder().decode(data, DecodeLimits.DEFAULT);
                }
            });
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Damaged sample tables fail with CORRUPT: samples that lie outside of the file, a chunk table that does not hold all samples.
     * <p>
     * 손상된 샘플 표는 CORRUPT 로 실패한다: 파일 밖에 있는 샘플, 모든 샘플을 담지 못하는 청크 표.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedSampleTables() throws Exception {
        final byte[] orig = AvifTestKit.resource("/media/avif/avis/avis_default.avif");
        // 샘플 크기 표의 첫 크기를 파일보다 크게 고친다.
        final byte[] bigSample = orig.clone();
        int stsz = indexOf(bigSample, "stsz");
        bigSample[stsz + 16] = 0x7F;
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "a sample larger than the file", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(bigSample, DecodeLimits.DEFAULT);
            }
        });
        // 청크 오프셋을 파일 끝 근처로 고친다.
        final byte[] farChunk = orig.clone();
        int stco = indexOf(farChunk, "stco");
        farChunk[stco + 12] = 0x00;
        farChunk[stco + 13] = 0x00;
        farChunk[stco + 14] = (byte) 0xFF;
        farChunk[stco + 15] = (byte) 0xFF;
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "a chunk outside of the file", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(farChunk, DecodeLimits.DEFAULT);
            }
        });
        // 샘플 개수를 늘려 표가 잘리게 한다.
        final byte[] manySamples = orig.clone();
        stsz = indexOf(manySamples, "stsz");
        manySamples[stsz + 12] = 0x00;
        manySamples[stsz + 13] = 0x00;
        manySamples[stsz + 14] = 0x10;
        manySamples[stsz + 15] = 0x00;
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "more samples than sizes", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(manySamples, DecodeLimits.DEFAULT);
            }
        });
    }

    /**
     * Cut and damaged animated files fail with a {@link MediaException} or decode to some frames; nothing else is thrown.
     * <p>
     * 잘리거나 손상된 애니메이션 파일은 {@link MediaException} 으로 실패하거나 어떤 프레임으로 디코딩된다. 그 밖의 것은 던지지 않는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedFiles() throws Exception {
        String[] names = { "avis_default", "avis_alpha", "avis_altref", "avis_nometa" };
        Random rnd = new Random(2468);
        int ended = 0;
        // 파일마다 잘라낸 길이와 임의로 바꾼 바이트로 여러 번 디코딩한다.
        for (String name : names) {
            byte[] orig = AvifTestKit.resource("/media/avif/avis/" + name + ".avif");
            // 잘라낸 길이마다 등록부로 디코딩을 시도한다.
            for (int len = 1; len < orig.length; len += 97) {
                byte[] cut = new byte[len];
                System.arraycopy(orig, 0, cut, 0, len);
                try {
                    ImageDecoders.decode(cut, "image/avif", null);
                } catch (MediaException e) {
                    Check.notNull(e.getKind(), "a kind");
                }
                ended++;
            }
            // 바이트를 임의로 바꾼 파일을 디코딩한다.
            for (int round = 0; round < 60; round++) {
                byte[] d = orig.clone();
                int flips = 1 + rnd.nextInt(4);
                // 임의 위치의 비트를 뒤집는다(앞쪽 상자 구조와 뒤쪽 AV1 데이터 모두).
                for (int k = 0; k < flips; k++) {
                    int p = round % 2 == 0 ? rnd.nextInt(d.length) : rnd.nextInt(Math.min(d.length, 1000));
                    d[p] ^= (byte) (1 << rnd.nextInt(8));
                }
                try {
                    ImageDecoders.decode(d, "image/avif", null);
                } catch (MediaException e) {
                    Check.notNull(e.getKind(), "a kind");
                }
                ended++;
            }
        }
        Check.isTrue(ended > 200, "every damaged file ended: " + ended);
    }
}
