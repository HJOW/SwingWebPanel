package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ExifBlocks;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>EXIF</code> chunk of WebP (plan item P18-03; FEATURE_MATRIX IMG-08): the orientation of the chunk is applied to still
 * lossy and lossless images and to every frame of an animation, with the chunk in either byte order, with the optional prefix and in front
 * of or behind the image data; an invalid value and a damaged trailing chunk leave the image alone. The pixels come from files made by
 * libwebp (the fixtures of {@link WebpFixtures}) that are wrapped into an extended file, and the expected result is the stored picture moved
 * by the definition of the eight orientations ({@link ExifBlocks#forward(int, int, int, int, int)}).
 * <p>
 * WebP <code>EXIF</code> 청크 시험(계획 P18-03; FEATURE_MATRIX IMG-08): 청크의 방향이 손실/무손실 정지 이미지와 애니메이션의 모든 프레임에 적용된다.
 * 청크는 어느 바이트 순서든, 선택적 머리말이 있든, 이미지 데이터 앞이든 뒤든 상관없고, 잘못된 값과 손상된 뒤쪽 청크는 이미지를 그대로 둔다. 픽셀은
 * libwebp 가 만든 파일({@link WebpFixtures} 의 자료)을 확장 파일로 감싼 것이고 기대 결과는 저장된 그림을 여덟 방향의 정의
 * ({@link ExifBlocks#forward(int, int, int, int, int)})대로 옮긴 것이다.
 */
public class WebpOrientationTest {

    /**
     * Directory of the libwebp files on the class path.
     * <p>
     * 클래스패스에서 libwebp 파일의 디렉터리.
     */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/testdata/webp/libwebp/";

    /**
     * Width of the container fixtures.
     * <p>
     * 컨테이너 자료의 가로.
     */
    private static final int W = 40;

    /**
     * Height of the container fixtures.
     * <p>
     * 컨테이너 자료의 세로.
     */
    private static final int H = 30;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebpOrientationTest() {
    }

    /**
     * Reads a libwebp file.
     * <p>
     * libwebp 파일을 읽는다.
     *
     * @param name name without the extension / 확장자를 뺀 이름
     * @return the file / 파일
     * @throws Exception if it cannot be read / 읽을 수 없을 때
     */
    private static byte[] fixture(String name) throws Exception {
        return WebpFixtures.readResource(DIR + name + ".webp");
    }

    /**
     * Wraps the bitstream chunk of a simple file into an extended file with an <code>EXIF</code> chunk.
     * <p>
     * 단순 파일의 비트스트림 청크를 <code>EXIF</code> 청크가 있는 확장 파일로 감싼다.
     *
     * @param simple the simple file / 단순 파일
     * @param type   the chunk type of its bitstream (<code>VP8 </code> or <code>VP8L</code>) / 비트스트림의 청크 종류(<code>VP8 </code> 또는 <code>VP8L</code>)
     * @param exif   the data of the <code>EXIF</code> chunk / <code>EXIF</code> 청크의 데이터
     * @param before true to put the <code>EXIF</code> chunk in front of the image data / <code>EXIF</code> 청크를 이미지 데이터 앞에 두려면 true
     * @return the extended file / 확장 파일
     */
    private static byte[] withExif(byte[] simple, String type, byte[] exif, boolean before) {
        byte[] image = WebpBuilder.extract(simple, type);
        byte[] meta = WebpBuilder.chunk("EXIF", exif);
        byte[] head = WebpBuilder.vp8x(0x08, W, H);
        return before ? WebpBuilder.file(head, meta, image) : WebpBuilder.file(head, image, meta);
    }

    /**
     * Returns the pixels of the first frame.
     * <p>
     * 첫 프레임의 픽셀을 반환한다.
     *
     * @param img the image / 이미지
     * @return pixels / 픽셀
     */
    private static int[] pixels(DecodedImage img) {
        return img.getFrames().get(0).getArgb();
    }

    /**
     * Still images, lossy and lossless: all eight orientations in both byte orders and with the prefix equal the definition.
     * <p>
     * 손실/무손실 정지 이미지에서 여덟 방향 모두가 두 바이트 순서와 머리말에서 정의와 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStillImagesFollowTheDefinition() throws Exception {
        String[] names = { "container_simple_lossless", "container_simple_lossy" };
        String[] types = { "VP8L", "VP8 " };
        // 무손실과 손실 파일을 차례로 시험한다.
        for (int n = 0; n < names.length; n++) {
            byte[] simple = fixture(names[n]);
            DecodedImage base = new WebpDecoder().decode(simple, DecodeLimits.DEFAULT);
            Check.equal(W, base.getWidth(), names[n] + ": width");
            Check.equal(H, base.getHeight(), names[n] + ": height");
            int[] src = pixels(base);
            // 방향 1~8 과 변형(빅 엔디언, 리틀 엔디언, 머리말)마다 확인한다.
            for (int o = 1; o <= 8; o++) {
                for (int v = 0; v < 3; v++) {
                    byte[] block = v == 0 ? ExifBlocks.tiff(o, true) : v == 1 ? ExifBlocks.tiff(o, false) : ExifBlocks.prefixed(o, false);
                    DecodedImage img = new WebpDecoder().decode(withExif(simple, types[n], block, false), DecodeLimits.DEFAULT);
                    int ow = o >= 5 ? H : W;
                    int oh = o >= 5 ? W : H;
                    String what = names[n] + " orientation " + o + " variant " + v;
                    Check.equal(ow, img.getWidth(), what + ": width");
                    Check.equal(oh, img.getHeight(), what + ": height");
                    Check.equal(o, img.getAppliedOrientation(), what + ": applied value");
                    int[] expected = ExifBlocks.reference(src, W, H, o);
                    int[] actual = pixels(img);
                    Check.equal(expected.length, actual.length, what + ": pixel count");
                    // 모든 픽셀이 정의와 같은지 확인한다(다르면 첫 위치를 알린다).
                    for (int i = 0; i < expected.length; i++) {
                        if (expected[i] != actual[i]) {
                            Check.fail(what + ": pixel " + i + " expected " + Integer.toHexString(expected[i]) + " but was " + Integer.toHexString(actual[i]));
                        }
                    }
                }
            }
        }
    }

    /**
     * The chunk also counts in front of the image data; only the first chunk counts; an invalid value leaves the image upright.
     * <p>
     * 청크는 이미지 데이터 앞에 있어도 인정되고, 첫 청크만 인정하며, 잘못된 값은 이미지를 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChunkPositionFirstChunkAndInvalidValue() throws Exception {
        byte[] simple = fixture("container_simple_lossless");
        int[] src = pixels(new WebpDecoder().decode(simple, DecodeLimits.DEFAULT));
        DecodedImage early = new WebpDecoder().decode(withExif(simple, "VP8L", ExifBlocks.tiff(6, true), true), DecodeLimits.DEFAULT);
        Check.equal(6, early.getAppliedOrientation(), "EXIF in front of the image data");
        Check.equal(H, early.getWidth(), "width after a quarter turn");
        // 첫 EXIF(3)만 인정하고 둘째(6)는 무시한다.
        byte[] image = WebpBuilder.extract(simple, "VP8L");
        byte[] two = WebpBuilder.file(WebpBuilder.vp8x(0x08, W, H), image, WebpBuilder.chunk("EXIF", ExifBlocks.tiff(3, true)),
                WebpBuilder.chunk("EXIF", ExifBlocks.tiff(6, true)));
        DecodedImage first = new WebpDecoder().decode(two, DecodeLimits.DEFAULT);
        Check.equal(3, first.getAppliedOrientation(), "the first EXIF chunk counts");
        // 잘못된 값과 알아볼 수 없는 데이터는 이미지를 돌리지 않는다.
        DecodedImage invalid = new WebpDecoder().decode(withExif(simple, "VP8L", ExifBlocks.tiff(9, true), false), DecodeLimits.DEFAULT);
        Check.equal(1, invalid.getAppliedOrientation(), "invalid value: nothing applied");
        Check.equal(W, invalid.getWidth(), "invalid value: width");
        Check.equal(src[7], pixels(invalid)[7], "invalid value: pixels unchanged");
        DecodedImage junk = new WebpDecoder().decode(withExif(simple, "VP8L", new byte[] { 1, 2, 3 }, false), DecodeLimits.DEFAULT);
        Check.equal(1, junk.getAppliedOrientation(), "junk data: nothing applied");
        // 이미지가 없는 파일에서는 EXIF 가 있어도 CORRUPT 이다.
        byte[] none = WebpBuilder.file(WebpBuilder.vp8x(0x08, W, H), WebpBuilder.chunk("EXIF", ExifBlocks.tiff(6, true)));
        try {
            new WebpDecoder().decode(none, DecodeLimits.DEFAULT);
            Check.fail("a file without image data must fail");
        } catch (org.duckdns.hjow.swingwebpanel.media.MediaException e) {
            Check.equal(org.duckdns.hjow.swingwebpanel.media.MediaException.Kind.CORRUPT, e.getKind(), "no image data");
        }
    }

    /**
     * The orientation applies to every frame of an animation, and the delays and the loop count stay.
     * <p>
     * 방향은 애니메이션의 모든 프레임에 적용되고 지연과 반복 횟수는 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationFrames() throws Exception {
        byte[] lossless = WebpBuilder.extract(fixture("container_simple_lossless"), "VP8L");
        byte[] lossy = WebpBuilder.extract(fixture("container_simple_lossy"), "VP8 ");
        byte[] head = WebpBuilder.vp8x(0x1A, W, H);
        byte[] anim = WebpBuilder.anim(0, 4);
        byte[] f1 = WebpBuilder.anmf(0, 0, W, H, 50, 2, lossless);
        byte[] f2 = WebpBuilder.anmf(0, 0, W, H, 80, 2, lossy);
        byte[] plain = WebpBuilder.file(WebpBuilder.vp8x(0x12, W, H), anim, f1, f2);
        DecodedImage base = new WebpDecoder().decode(plain, DecodeLimits.DEFAULT);
        Check.equal(2, base.getFrames().size(), "frames of the animation without EXIF");
        byte[] oriented = WebpBuilder.file(head, anim, f1, f2, WebpBuilder.chunk("EXIF", ExifBlocks.tiff(8, false)));
        DecodedImage img = new WebpDecoder().decode(oriented, DecodeLimits.DEFAULT);
        Check.equal(H, img.getWidth(), "width after a quarter turn");
        Check.equal(W, img.getHeight(), "height after a quarter turn");
        Check.equal(4, img.getLoopCount(), "loop count");
        List<DecodedFrame> frames = img.getFrames();
        Check.equal(2, frames.size(), "frame count");
        Check.equal(50, frames.get(0).getDelayMillis(), "first delay");
        Check.equal(80, frames.get(1).getDelayMillis(), "second delay");
        // 프레임마다 정의와 같은지 확인한다.
        for (int f = 0; f < 2; f++) {
            int[] expected = ExifBlocks.reference(base.getFrames().get(f).getArgb(), W, H, 8);
            int[] actual = frames.get(f).getArgb();
            Check.equal(expected.length, actual.length, "frame " + f + ": pixel count");
            // 모든 픽셀을 확인한다.
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != actual[i]) {
                    Check.fail("frame " + f + ": pixel " + i + " expected " + Integer.toHexString(expected[i]) + " but was " + Integer.toHexString(actual[i]));
                }
            }
        }
    }

    /**
     * A damaged chunk behind the image data of a still image does not hurt the image: it is still decoded, just without orientation.
     * <p>
     * 정지 이미지의 이미지 데이터 뒤에 있는 손상된 청크는 이미지를 해치지 않는다. 방향 없이 그대로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedTrailingChunkLeavesTheImage() throws Exception {
        byte[] simple = fixture("container_simple_lossless");
        byte[] image = WebpBuilder.extract(simple, "VP8L");
        // 청크 크기가 파일을 넘어가도록 크기 필드를 키운 EXIF 청크를 붙인다.
        byte[] meta = WebpBuilder.chunk("EXIF", ExifBlocks.tiff(6, true));
        meta[4] = (byte) 0xF0;
        meta[5] = (byte) 0x7F;
        byte[] head = WebpBuilder.vp8x(0x08, W, H);
        // file() 은 RIFF 크기를 실제 길이로 쓰므로 청크 하나만 자기 크기를 넘어선다.
        byte[] file = WebpBuilder.file(head, image, meta);
        DecodedImage img = new WebpDecoder().decode(file, DecodeLimits.DEFAULT);
        Check.equal(W, img.getWidth(), "width");
        Check.equal(H, img.getHeight(), "height");
        Check.equal(1, img.getAppliedOrientation(), "nothing applied");
    }
}
