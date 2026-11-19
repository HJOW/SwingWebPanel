package org.duckdns.hjow.swingwebpanel.media.image.webp;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ExifBlocks;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;
import org.duckdns.hjow.swingwebpanel.media.image.color.IccBuilder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the colour profile of WebP (plan item P18-03; FEATURE_MATRIX IMG-08): the <code>ICCP</code> chunk of a matrix/TRC profile converts the
 * pixels of lossy, lossless and animated files to sRGB; a profile that is not supported, damaged bytes and a gray profile on a colour image leave
 * the pixels alone; the colour profile and the EXIF orientation work together. The expected pixels are the pixels of the same file without the chunk,
 * converted by {@link ColorProfile} (whose conversion is checked against independent references in
 * {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest}).
 * <p>
 * WebP 색 프로파일 시험(계획 P18-03; FEATURE_MATRIX IMG-08): 행렬/TRC 프로파일의 <code>ICCP</code> 청크가 손실, 무손실, 애니메이션 파일의 픽셀을 sRGB 로
 * 바꾼다. 지원하지 않는 프로파일, 손상된 바이트, 컬러 이미지의 회색조 프로파일은 픽셀을 그대로 둔다. 색 프로파일과 EXIF 방향은 함께 동작한다. 기대 픽셀은 같은
 * 파일에서 청크를 뺀 픽셀을 {@link ColorProfile} 로 변환한 것이다(변환 자체는 {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest} 가 독립된
 * 기준과 대조한다).
 */
public class WebpColorTest {

    /**
     * Directory of the libwebp files on the class path.
     * <p>
     * 클래스패스에서 libwebp 파일의 디렉터리.
     */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/testdata/webp/libwebp/";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebpColorTest() {
    }

    /**
     * Builds the ICC profile of Display P3 (the sRGB curve).
     * <p>
     * Display P3 의 ICC 프로파일(sRGB 곡선)을 만든다.
     *
     * @return the profile bytes / 프로파일 바이트
     */
    private static byte[] p3() {
        return IccBuilder.rgb(4, new double[] { 0.5151, 0.2412, -0.0011 }, new double[] { 0.2920, 0.6922, 0.0419 }, new double[] { 0.1571, 0.0666, 0.7841 },
                IccBuilder.para(3, 2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045));
    }

    /**
     * Compares the pixels of two arrays.
     * <p>
     * 두 배열의 픽셀을 비교한다.
     *
     * @param expected expected pixels / 기대 픽셀
     * @param actual   actual pixels / 실제 픽셀
     * @param what     description / 설명
     */
    private static void same(int[] expected, int[] actual, String what) {
        Check.equal(expected.length, actual.length, what + ": pixel count");
        // 모든 픽셀을 비교한다.
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                Check.fail(what + ": pixel " + i + " expected " + Integer.toHexString(expected[i]) + " but was " + Integer.toHexString(actual[i]));
            }
        }
    }

    /**
     * Still lossless and lossy images with an <code>ICCP</code> chunk in front of the image data are converted, with the orientation of an
     * <code>EXIF</code> chunk behind it applied after the conversion.
     * <p>
     * 이미지 데이터 앞에 <code>ICCP</code> 청크가 있는 무손실/손실 정지 이미지가 변환되고, 뒤의 <code>EXIF</code> 청크의 방향은 변환 뒤에 적용된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStillImages() throws Exception {
        String[] names = { "container_simple_lossless", "container_simple_lossy" };
        String[] types = { "VP8L", "VP8 " };
        ColorProfile p = ColorProfile.fromIcc(p3());
        // 무손실과 손실 파일을 차례로 시험한다.
        for (int n = 0; n < names.length; n++) {
            byte[] simple = WebpFixtures.readResource(DIR + names[n] + ".webp");
            int[] base = new WebpDecoder().decode(simple, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
            int[] expected = base.clone();
            p.convert(expected);
            int changed = 0;
            // 바뀐 픽셀의 수를 센다.
            for (int i = 0; i < base.length; i++) {
                if (base[i] != expected[i]) {
                    changed++;
                }
            }
            Check.isTrue(changed > base.length / 4, names[n] + ": the profile changes the pixels, changed " + changed);
            byte[] image = WebpBuilder.extract(simple, types[n]);
            byte[] file = WebpBuilder.file(WebpBuilder.vp8x(0x20, 40, 30), WebpBuilder.chunk("ICCP", p3()), image);
            same(expected, new WebpDecoder().decode(file, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), names[n] + " with a profile");
            // 방향이 6 인 EXIF 청크를 더하면 변환된 픽셀을 그 방향으로 돌린 것과 같다.
            byte[] both = WebpBuilder.file(WebpBuilder.vp8x(0x28, 40, 30), WebpBuilder.chunk("ICCP", p3()), image,
                    WebpBuilder.chunk("EXIF", ExifBlocks.tiff(6, false)));
            DecodedImage turned = new WebpDecoder().decode(both, DecodeLimits.DEFAULT);
            same(ExifBlocks.reference(expected, 40, 30, 6), turned.getFrames().get(0).getArgb(), names[n] + " with a profile and an orientation");
        }
    }

    /**
     * Every frame of an animation is converted.
     * <p>
     * 애니메이션의 모든 프레임이 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimation() throws Exception {
        byte[] lossless = WebpBuilder.extract(WebpFixtures.readResource(DIR + "container_simple_lossless.webp"), "VP8L");
        byte[] lossy = WebpBuilder.extract(WebpFixtures.readResource(DIR + "container_simple_lossy.webp"), "VP8 ");
        byte[] anim = WebpBuilder.anim(0, 2);
        byte[] f1 = WebpBuilder.anmf(0, 0, 40, 30, 50, 2, lossless);
        byte[] f2 = WebpBuilder.anmf(0, 0, 40, 30, 80, 2, lossy);
        DecodedImage base = new WebpDecoder().decode(WebpBuilder.file(WebpBuilder.vp8x(0x12, 40, 30), anim, f1, f2), DecodeLimits.DEFAULT);
        DecodedImage withIcc = new WebpDecoder().decode(
                WebpBuilder.file(WebpBuilder.vp8x(0x32, 40, 30), WebpBuilder.chunk("ICCP", p3()), anim, f1, f2), DecodeLimits.DEFAULT);
        ColorProfile p = ColorProfile.fromIcc(p3());
        Check.equal(2, withIcc.getFrames().size(), "frame count");
        // 프레임마다 프로파일 변환과 같은지 확인한다.
        for (int f = 0; f < 2; f++) {
            int[] expected = base.getFrames().get(f).getArgb().clone();
            p.convert(expected);
            same(expected, withIcc.getFrames().get(f).getArgb(), "frame " + f);
            Check.equal(base.getFrames().get(f).getDelayMillis(), withIcc.getFrames().get(f).getDelayMillis(), "delay of frame " + f);
        }
    }

    /**
     * A profile that is not supported, damaged bytes and a gray profile on a colour image leave the pixels as they are.
     * <p>
     * 지원하지 않는 프로파일, 손상된 바이트, 컬러 이미지의 회색조 프로파일은 픽셀을 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnusableProfilesLeaveThePixels() throws Exception {
        byte[] simple = WebpFixtures.readResource(DIR + "container_simple_lossless.webp");
        int[] base = new WebpDecoder().decode(simple, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
        byte[] image = WebpBuilder.extract(simple, "VP8L");
        byte[][] profiles = { new IccBuilder().tag("A2B0", new byte[64]).build(), new byte[] { 1, 2, 3, 4 }, IccBuilder.gray(IccBuilder.curv(461)) };
        String[] what = { "a profile with look-up tables", "damaged bytes", "a gray profile" };
        // 쓸 수 없는 프로파일마다 픽셀이 그대로인지 본다.
        for (int i = 0; i < profiles.length; i++) {
            byte[] file = WebpBuilder.file(WebpBuilder.vp8x(0x20, 40, 30), WebpBuilder.chunk("ICCP", profiles[i]), image);
            same(base, new WebpDecoder().decode(file, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), what[i]);
        }
    }
}
