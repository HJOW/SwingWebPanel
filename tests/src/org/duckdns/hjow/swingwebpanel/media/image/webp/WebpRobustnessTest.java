package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the safety of {@link WebpDecoder} (plan items P18-01, P18-10, P22-09): damaged, truncated and hostile files, the decoding limits,
 * the detection by file signature and the cancellation of a decode. A failure must always be a {@link MediaException} of the right kind, never
 * another exception, never a hang and never a huge allocation.
 * <p>
 * {@link WebpDecoder} 의 안전성 시험(계획 P18-01, P18-10, P22-09): 손상되거나 잘리거나 악의적인 파일, 디코딩 제한, 파일 시그니처로 하는 판별,
 * 디코딩 취소. 실패는 항상 올바른 종류의 {@link MediaException} 이어야 하며 다른 예외도, 멈춤도, 거대한 할당도 없어야 한다.
 */
public class WebpRobustnessTest {

    /**
     * Directory of the libwebp fixtures.
     * <p>
     * libwebp 자료 디렉터리.
     */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/testdata/webp/libwebp/";

    /**
     * Tight limits that keep the memory of every test small.
     * <p>
     * 모든 시험의 메모리를 작게 유지하는 빡빡한 제한.
     */
    private static final DecodeLimits SMALL = new DecodeLimits(1000000L, 1 << 20, 64, 4000000L);

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebpRobustnessTest() {
    }

    /**
     * Reads a fixture file.
     * <p>
     * 자료 파일을 읽는다.
     *
     * @param name file name / 파일 이름
     * @return the bytes / 바이트
     * @throws Exception if it cannot be read / 읽을 수 없을 때
     */
    private static byte[] fixture(String name) throws Exception {
        return WebpFixtures.readResource(DIR + name + ".webp");
    }

    /**
     * Decodes and returns the kind of the failure, or null if the decode succeeded; any other throwable fails the test.
     * <p>
     * 디코딩하고 실패의 종류를 반환한다. 성공하면 null 이다. 다른 예외는 시험을 실패시킨다.
     *
     * @param data   data / 데이터
     * @param limits limits / 제한
     * @return the failure kind or null / 실패 종류 또는 null
     */
    private static MediaException.Kind kind(byte[] data, DecodeLimits limits) {
        try {
            new WebpDecoder().decode(data, limits);
            return null;
        } catch (MediaException e) {
            return e.getKind();
        }
    }

    /**
     * Every prefix of a file (cut at every length) fails cleanly with CORRUPT: lossy, lossy with alpha, lossless and an animation.
     * <p>
     * 파일의 모든 앞부분(모든 길이에서 자름)이 깨끗이 CORRUPT 로 실패한다: 손실, 알파가 있는 손실, 무손실, 애니메이션.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryTruncationIsCorrupt() throws Exception {
        String[] names = { "lossy_photo_1x1", "lossy_alpha_one", "lossless_photo_2x3", "lossless_palette_4_w3", "anim_hand_odd_offsets" };
        // 파일마다 길이를 하나씩 줄여 가며 디코딩한다.
        for (int n = 0; n < names.length; n++) {
            byte[] full = fixture(names[n]);
            Check.isTrue(kind(full, SMALL) == null, names[n] + " decodes completely");
            // 길이를 하나씩 줄여 가며 디코딩한다.
            for (int len = 0; len < full.length; len++) {
                byte[] cut = new byte[len];
                System.arraycopy(full, 0, cut, 0, len);
                MediaException.Kind k = kind(cut, SMALL);
                Check.isTrue(k == MediaException.Kind.CORRUPT || k == MediaException.Kind.UNSUPPORTED_FORMAT, names[n] + " cut at " + len + " gave " + k);
            }
        }
    }

    /**
     * A simple file whose bitstream chunk was shortened (the container is rebuilt consistently, so only the bitstream is damaged) is detected
     * as damaged: a cut of 16 bytes or more always fails with CORRUPT, a smaller cut fails or still decodes (the last bytes of a bitstream may
     * be padding).
     * <p>
     * 비트스트림 청크를 줄인 단순 파일(컨테이너는 일관되게 다시 만들므로 비트스트림만 손상됨)이 손상으로 판별된다. 16바이트 이상 자르면 항상
     * CORRUPT 로 실패하고 그보다 작게 자르면 실패하거나 그대로 디코딩된다(비트스트림의 마지막 바이트는 패딩일 수 있다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCutBitstreamWithValidContainer() throws Exception {
        String[] names = { "container_simple_lossy", "container_simple_lossless" };
        // 파일마다 비트스트림 뒤쪽을 여러 길이로 잘라 컨테이너를 다시 만든다.
        for (int n = 0; n < names.length; n++) {
            byte[] full = fixture(names[n]);
            String type = new String(full, 12, 4, "US-ASCII");
            int size = (full[16] & 0xff) | ((full[17] & 0xff) << 8) | ((full[18] & 0xff) << 16);
            // 잘라 낼 길이를 달리하며 시험한다.
            for (int cut = 1; cut < size - 20; cut += 1 + size / 40) {
                byte[] payload = new byte[size - cut];
                System.arraycopy(full, 20, payload, 0, payload.length);
                MediaException.Kind k = kind(WebpBuilder.file(WebpBuilder.chunk(type, payload)), SMALL);
                if (cut >= 16) {
                    Check.equal(MediaException.Kind.CORRUPT, k, names[n] + " cut by " + cut);
                } else {
                    Check.isTrue(k == null || k == MediaException.Kind.CORRUPT, names[n] + " cut by " + cut + " gave " + k);
                }
            }
        }
    }

    /**
     * Random byte changes (seeded) never lead to another failure than {@link MediaException}; a lossy, a lossy alpha, a lossless and an animated
     * file are damaged at 1 to 4 places 400 times each.
     * <p>
     * 무작위 바이트 변경(씨앗 고정)이 {@link MediaException} 이외의 실패로 이어지지 않는다. 손실, 알파가 있는 손실, 무손실, 애니메이션 파일을 각각
     * 400 번씩 1~4 곳 손상시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRandomDamageOnlyFailsWithMediaException() throws Exception {
        String[] names = { "lossy_photo_33x17_q100", "lossy_alpha_a100", "lossless_alpha_photo_50x40", "anim_hand_lossy_alpha" };
        Random rnd = new Random(20261116L);
        int undetected = 0;
        // 파일마다 여러 번 무작위로 망가뜨려 디코딩한다.
        for (int n = 0; n < names.length; n++) {
            byte[] full = fixture(names[n]);
            // 바뀐 위치마다 값을 무작위로 정한다.
            for (int round = 0; round < 400; round++) {
                byte[] d = full.clone();
                int changes = 1 + rnd.nextInt(4);
                // 바뀐 위치마다 값을 무작위로 정한다(RIFF 헤더 12바이트 뒤).
                for (int c = 0; c < changes; c++) {
                    d[12 + rnd.nextInt(d.length - 12)] = (byte) rnd.nextInt(256);
                }
                try {
                    new WebpDecoder().decode(d, SMALL);
                    undetected++;
                } catch (MediaException e) {
                    Check.notNull(e.getKind(), "kind");
                } catch (RuntimeException e) {
                    Check.fail(names[n] + " round " + round + ": leaked " + e);
                }
            }
        }
        Check.isTrue(undetected < 4 * 400, "some damage must be detected");
    }

    /**
     * Garbage after the first bytes, empty data and a wrong signature are reported with the right kinds.
     * <p>
     * 처음 몇 바이트 뒤의 쓰레기, 빈 데이터, 잘못된 시그니처가 올바른 종류로 보고된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGarbage() throws Exception {
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, kind(new byte[0], SMALL), "empty data");
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, kind("RIFX....WEBPVP8 ".getBytes("US-ASCII"), SMALL), "wrong signature");
        byte[] header = WebpBuilder.file(WebpBuilder.chunk("ABCD", new byte[4]));
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, kind(header, SMALL), "unknown first chunk");
        byte[] noChunks = WebpBuilder.file();
        Check.equal(MediaException.Kind.CORRUPT, kind(noChunks, SMALL), "no chunks");
        byte[] extendedOnly = WebpBuilder.file(WebpBuilder.vp8x(0, 10, 10));
        Check.equal(MediaException.Kind.CORRUPT, kind(extendedOnly, SMALL), "header without image");
    }

    /**
     * The image size limit is checked before anything is allocated: a canvas of 16 million pixels in the header of a tiny file is refused as
     * TOO_LARGE, for the extended header and for both bitstream types.
     * <p>
     * 이미지 크기 제한은 아무것도 할당하기 전에 확인한다: 작은 파일의 헤더에 적힌 1,600만 픽셀 캔버스는 확장 헤더와 두 비트스트림 종류 모두에서
     * TOO_LARGE 로 거부된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeLimits() throws Exception {
        byte[] lossless = WebpBuilder.extract(fixture("container_simple_lossless"), "VP8L");
        byte[] bomb = WebpBuilder.file(WebpBuilder.vp8x(0, 4096, 4096), lossless);
        Check.equal(MediaException.Kind.TOO_LARGE, kind(bomb, SMALL), "canvas over the limit");
        // 40 x 30 = 1200 픽셀 영상을 1000 픽셀 제한으로 디코딩한다.
        DecodeLimits tiny = new DecodeLimits(1000L, 1 << 20, 64, 4000000L);
        Check.equal(MediaException.Kind.TOO_LARGE, kind(fixture("container_simple_lossless"), tiny), "lossless over the pixel limit");
        Check.equal(MediaException.Kind.TOO_LARGE, kind(fixture("container_simple_lossy"), tiny), "lossy over the pixel limit");
        // 비트스트림 헤더만 4096 x 4096 으로 고친 손실/무손실 단순 파일도 거부된다.
        byte[] huge = fixture("container_simple_lossless");
        huge[21] = (byte) 0xff;
        huge[22] = (byte) 0xff;
        huge[23] = (byte) 0xff;
        huge[24] = (byte) 0x0f;
        Check.equal(MediaException.Kind.TOO_LARGE, kind(huge, SMALL), "lossless header with a huge size");
        byte[] hugeLossy = fixture("container_simple_lossy");
        hugeLossy[26] = (byte) 0xff;
        hugeLossy[27] = (byte) 0x0f;
        hugeLossy[28] = (byte) 0xff;
        hugeLossy[29] = (byte) 0x0f;
        Check.equal(MediaException.Kind.TOO_LARGE, kind(hugeLossy, SMALL), "lossy header with a huge size");
    }

    /**
     * An animation is limited by the number of frames and by the total number of pixels over all frames.
     * <p>
     * 애니메이션은 프레임 수와 모든 프레임에 걸친 총 픽셀 수로 제한된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationLimits() throws Exception {
        byte[] frame = WebpBuilder.extract(fixture("container_simple_lossless"), "VP8L");
        byte[][] chunks = new byte[2 + 6][];
        chunks[0] = WebpBuilder.vp8x(0x12, 40, 30);
        chunks[1] = WebpBuilder.anim(0, 0);
        // 같은 프레임 6 개를 이어 붙인 애니메이션을 만든다.
        for (int i = 0; i < 6; i++) {
            chunks[2 + i] = WebpBuilder.anmf(0, 0, 40, 30, 50, 0, frame);
        }
        byte[] anim = WebpBuilder.file(chunks);
        DecodedImage ok = new WebpDecoder().decode(anim, SMALL);
        Check.equal(6, ok.getFrames().size(), "six frames");
        Check.equal(MediaException.Kind.TOO_LARGE, kind(anim, new DecodeLimits(1000000L, 1 << 20, 5, 4000000L)), "too many frames");
        Check.equal(MediaException.Kind.TOO_LARGE, kind(anim, new DecodeLimits(1000000L, 1 << 20, 64, 5L * 40 * 30)), "too many total pixels");
    }

    /**
     * Animation structure errors are CORRUPT: a frame outside the canvas, a frame whose size differs from its bitstream, a frame without image
     * data, a missing header chunk, and a frame before the <code>ANIM</code> chunk.
     * <p>
     * 애니메이션 구조 오류는 CORRUPT 이다: 캔버스 밖의 프레임, 비트스트림과 크기가 다른 프레임, 이미지 데이터가 없는 프레임, 없는 헤더 청크,
     * <code>ANIM</code> 청크 앞의 프레임.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationStructureErrors() throws Exception {
        byte[] frame = WebpBuilder.extract(fixture("container_simple_lossless"), "VP8L");
        byte[] vp8x = WebpBuilder.vp8x(0x12, 40, 30);
        byte[] anim = WebpBuilder.anim(0, 0);
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(vp8x, anim, WebpBuilder.anmf(2, 0, 40, 30, 10, 0, frame)), SMALL), "frame outside");
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(vp8x, anim, WebpBuilder.anmf(0, 0, 39, 30, 10, 0, frame)), SMALL), "frame size differs");
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(vp8x, anim, WebpBuilder.anmf(0, 0, 40, 30, 10, 0)), SMALL), "no image data");
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(vp8x, WebpBuilder.anmf(0, 0, 40, 30, 10, 0, frame)), SMALL), "frame before ANIM");
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(vp8x, anim), SMALL), "no frames");
        Check.equal(MediaException.Kind.CORRUPT, kind(WebpBuilder.file(WebpBuilder.vp8x(0, 41, 30), frame), SMALL), "canvas differs from the image");
    }

    /**
     * The registry finds WebP data by its signature whatever the MIME type says, lists the type, and a wrong MIME hint does not matter.
     * <p>
     * 등록부는 MIME 타입이 무엇이든 시그니처로 WebP 데이터를 찾고, 이 타입을 목록에 올리며, 잘못된 MIME 힌트는 상관없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRegistryIntegration() throws Exception {
        byte[] data = fixture("lossy_photo_33x17_q100");
        Check.isTrue(ImageDecoders.isSupportedMime("image/webp"), "image/webp is supported");
        Check.isTrue(ImageDecoders.isSupportedMime("IMAGE/WEBP; charset=x"), "case and parameters are ignored");
        Check.isTrue(ImageDecoders.getSupportedMimeTypes().contains("image/webp"), "listed");
        Check.contains(ImageDecoders.getAcceptHeader(), "image/webp", "Accept header");
        Check.isTrue(ImageDecoders.find(data) instanceof WebpDecoder, "found by signature");
        DecodedImage a = ImageDecoders.decode(data, "image/png", null);
        DecodedImage b = ImageDecoders.decode(data, null, null);
        Check.equal(33, a.getWidth(), "width through the registry");
        Check.equal(17, a.getHeight(), "height through the registry");
        Check.isTrue(java.util.Arrays.equals(a.getFrames().get(0).getArgb(), b.getFrames().get(0).getArgb()), "the MIME hint does not change the result");
        MediaException e = (MediaException) Check.throwsException(MediaException.class, new Block() {
            public void run() throws Exception {
                ImageDecoders.decode(new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ' }, "image/webp", null);
            }
        }, "a header without data fails");
        Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "kind");
    }

    /**
     * A decode of a thread whose interrupt flag is set ends with CANCELLED, and the flag stays set.
     * <p>
     * 인터럽트 플래그가 켜진 스레드의 디코딩은 CANCELLED 로 끝나며 플래그는 켜진 채로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        String[] names = { "lossy_photo_96x64_q75_m4", "lossless_photo_96x64_m6", "anim_lossless_boxes" };
        // 형식마다 인터럽트된 스레드에서 디코딩한다.
        for (int i = 0; i < names.length; i++) {
            byte[] data = fixture(names[i]);
            Thread.currentThread().interrupt();
            try {
                Check.equal(MediaException.Kind.CANCELLED, kind(data, DecodeLimits.DEFAULT), names[i] + " is cancelled");
                Check.isTrue(Thread.currentThread().isInterrupted(), "the flag is kept");
            } finally {
                Thread.interrupted();
            }
        }
    }

    /**
     * The comparison of the other tests really detects differences: the pixels of a lossy file do not match the reference of another image.
     * <p>
     * 다른 시험의 비교가 실제로 차이를 찾아낸다: 손실 파일의 픽셀은 다른 영상의 기준과 맞지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testComparisonDetectsDifferences() throws Exception {
        WebpFixtures.Fixture lossy = WebpFixtures.get("lossy_flat_20x20_q50");
        WebpFixtures.Fixture other = WebpFixtures.get("lossless_flat_20x20");
        DecodedImage img = new WebpDecoder().decode(lossy.data, SMALL);
        long[] got = WebpFixtures.rowHashes(img.getFrames().get(0), 20, 20);
        boolean same = true;
        // 모든 행의 해시를 다른 영상의 기준과 비교한다.
        for (int y = 0; y < 20; y++) {
            same &= got[y] == other.rowHashes[y];
        }
        Check.isFalse(same, "different pictures must have different hashes");
        Check.isTrue(java.util.Arrays.equals(got, lossy.rowHashes), "the true reference matches");
    }
}
