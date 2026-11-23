package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;
import org.duckdns.hjow.swingwebpanel.media.image.color.IccBuilder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The colour management of AVIF (plan item P18-03; FEATURE_MATRIX IMG-09): the <code>colr</code> property of the type <code>nclx</code> (colour
 * primaries and transfer characteristics of ITU-T H.273, including the HDR curves PQ and HLG) and of the types <code>prof</code> and
 * <code>rICC</code> (an ICC profile) convert the pixels to sRGB, the profile wins over the code points, a profile that cannot be used falls back
 * to the code points, and the values of sRGB leave the pixels alone. The layers are real AV1 streams (the fixtures of the decoder tests) wrapped by
 * the test HEIF writer; the expected pixels are those of the same file with sRGB code points, converted by {@link ColorProfile} (which is checked
 * against independent references in {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest}).
 * <p>
 * AVIF 의 색 관리(계획 P18-03; FEATURE_MATRIX IMG-09): <code>nclx</code> 형식의 <code>colr</code> 속성(ITU-T H.273 의 색 원색과 전달 특성. HDR 곡선 PQ,
 * HLG 포함)과 <code>prof</code>, <code>rICC</code> 형식(ICC 프로파일)이 픽셀을 sRGB 로 바꾸고, 프로파일이 코드 값보다 우선하며, 쓸 수 없는 프로파일은 코드
 * 값으로 넘어가고, sRGB 의 값은 픽셀을 그대로 둔다. 계층은 시험용 HEIF 작성기로 감싼 실제 AV1 스트림(디코더 시험의 자료)이며 기대 픽셀은 같은 파일의
 * sRGB 코드 값 판을 {@link ColorProfile} 로 변환한 것이다(변환 자체는 {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest} 가 독립된 기준과 대조한다).
 */
public class AvifColorTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AvifColorTest() {
    }

    /**
     * Builds an <code>nclx</code> colour description with the matrix coefficients of Rec. 709 and the full range.
     * <p>
     * Rec. 709 행렬 계수와 전체 범위의 <code>nclx</code> 색 설명을 만든다.
     *
     * @param primaries the colour primaries code point / 색 원색 코드 값
     * @param transfer  the transfer characteristics code point / 전달 특성 코드 값
     * @return the <code>colr</code> box / <code>colr</code> 상자
     */
    private static byte[] nclx(int primaries, int transfer) {
        return AvifTestKit.box("colr", AvifTestKit.ascii("nclx"), AvifTestKit.be(primaries, 2), AvifTestKit.be(transfer, 2), AvifTestKit.be(1, 2),
                new byte[] { (byte) 0x80 });
    }

    /**
     * Builds an ICC <code>colr</code> box.
     * <p>
     * ICC <code>colr</code> 상자를 만든다.
     *
     * @param type    <code>prof</code> or <code>rICC</code> / <code>prof</code> 또는 <code>rICC</code>
     * @param profile the profile bytes / 프로파일 바이트
     * @return the box / 상자
     */
    private static byte[] icc(String type, byte[] profile) {
        return AvifTestKit.box("colr", AvifTestKit.ascii(type), profile);
    }

    /**
     * Builds an AVIF file of one fixture stream with colour description boxes.
     * <p>
     * 자료 스트림 하나와 색 설명 상자로 AVIF 파일을 만든다.
     *
     * @param colrs the <code>colr</code> boxes in the order of the file / 파일 순서의 <code>colr</code> 상자
     * @return the file / 파일
     * @throws Exception on a failure / 실패 시
     */
    private static byte[] avif(byte[]... colrs) throws Exception {
        AvifTestKit.Builder b = new AvifTestKit.Builder();
        b.primary = 1;
        b.item(1, "av01", AvifTestKit.resource("/media/avif/av1/rav1e_420.obu"));
        b.assoc(1, b.prop(AvifTestKit.ispe(96, 64)), false).assoc(1, b.prop(AvifTestKit.av1C()), true);
        // 색 설명 상자를 파일의 순서대로 항목에 연결한다.
        for (byte[] c : colrs) {
            b.assoc(1, b.prop(c), false);
        }
        return b.build();
    }

    /**
     * Decodes a file and returns the pixels.
     * <p>
     * 파일을 디코딩해 픽셀을 반환한다.
     *
     * @param file the file / 파일
     * @return the pixels / 픽셀
     * @throws Exception on a failure / 실패 시
     */
    private static int[] pixels(byte[] file) throws Exception {
        DecodedImage img = new AvifDecoder().decode(file, DecodeLimits.DEFAULT);
        return img.getFrames().get(0).getArgb();
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
     * Counts the pixels that differ.
     * <p>
     * 다른 픽셀의 수를 센다.
     *
     * @param a one array / 한 배열
     * @param b the other array / 다른 배열
     * @return the number of differing pixels / 다른 픽셀의 수
     */
    private static int differing(int[] a, int[] b) {
        int n = 0;
        // 위치마다 값이 다른 픽셀을 센다.
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the pixels of the file with the sRGB code points converted with a profile.
     * <p>
     * sRGB 코드 값 판의 픽셀을 프로파일로 변환해 반환한다.
     *
     * @param base the pixels with sRGB code points / sRGB 코드 값 판의 픽셀
     * @param p    the profile / 프로파일
     * @return the converted pixels / 변환된 픽셀
     */
    private static int[] converted(int[] base, ColorProfile p) {
        int[] r = base.clone();
        p.convert(r);
        return r;
    }

    /**
     * Code points: sRGB (and the unspecified values and the Rec. 709 curve, which is the sRGB curve as in Chromium) leave the pixels alone;
     * Display P3, Rec. 2020 with PQ and with HLG, and the gamma 2.2 curve of Rec. 470 M convert every pixel like
     * {@link ColorProfile#fromCicp(int, int)}.
     * <p>
     * 코드 값: sRGB(와 지정되지 않은 값, Chromium 처럼 sRGB 곡선인 Rec. 709 곡선)는 픽셀을 그대로 두고, Display P3, PQ 와 HLG 의 Rec. 2020, Rec. 470 M 의
     * 감마 2.2 곡선은 모든 픽셀을
     * {@link ColorProfile#fromCicp(int, int)} 처럼 바꾼다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testCodePoints() throws Exception {
        int[] base = pixels(avif(nclx(1, 13)));
        same(base, pixels(avif(nclx(2, 2))), "unspecified code points");
        same(base, pixels(avif(nclx(1, 13))), "again, the decode is deterministic");
        // Rec. 709 곡선(1)은 Chromium 처럼 sRGB 곡선이므로 원색이 같으면 픽셀을 바꾸지 않는다.
        same(base, pixels(avif(nclx(1, 1))), "the Rec. 709 curve is the sRGB curve");
        int[][] cases = { { 12, 13 }, { 9, 16 }, { 9, 18 }, { 9, 1 }, { 11, 13 }, { 4, 4 } };
        // 코드 값 조합마다 변환 결과와 대조한다.
        for (int i = 0; i < cases.length; i++) {
            int[] expected = converted(base, ColorProfile.fromCicp(cases[i][0], cases[i][1]));
            Check.isTrue(differing(base, expected) > base.length / 8, "primaries " + cases[i][0] + " transfer " + cases[i][1] + " change the pixels");
            same(expected, pixels(avif(nclx(cases[i][0], cases[i][1]))), "primaries " + cases[i][0] + " transfer " + cases[i][1]);
        }
        // 지원하지 않는 전달 함수(로그)는 sRGB 로 남는다.
        same(base, pixels(avif(nclx(1, 9))), "logarithmic transfer function");
    }

    /**
     * ICC profiles (<code>prof</code> and <code>rICC</code>) convert the pixels; the profile wins over the code points; a profile that cannot be
     * used (look-up tables, damaged bytes) falls back to the code points; an <code>nclx</code> box behind a profile still sets the matrix.
     * <p>
     * ICC 프로파일(<code>prof</code>, <code>rICC</code>)이 픽셀을 바꾼다. 프로파일이 코드 값보다 우선한다. 쓸 수 없는 프로파일(룩업 테이블, 손상된 바이트)은 코드
     * 값으로 넘어간다. 프로파일 뒤의 <code>nclx</code> 상자도 행렬을 정한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testIccProfiles() throws Exception {
        int[] base = pixels(avif(nclx(1, 13)));
        byte[] p3 = IccBuilder.rgb(4, new double[] { 0.5151, 0.2412, -0.0011 }, new double[] { 0.2920, 0.6922, 0.0419 },
                new double[] { 0.1571, 0.0666, 0.7841 }, IccBuilder.para(3, 2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045));
        int[] expected = converted(base, ColorProfile.fromIcc(p3));
        Check.isTrue(differing(base, expected) > base.length / 8, "the profile changes the pixels");
        // nclx 상자가 먼저 오든 뒤에 오든 행렬은 같고 프로파일이 색 공간을 정한다.
        same(expected, pixels(avif(nclx(1, 13), icc("prof", p3))), "profile behind the nclx box");
        same(expected, pixels(avif(icc("prof", p3), nclx(1, 13))), "profile in front of the nclx box");
        same(expected, pixels(avif(icc("rICC", p3), nclx(1, 13))), "restricted profile");
        same(expected, pixels(avif(icc("prof", p3), nclx(9, 16))), "the profile wins over the code points");
        // 쓸 수 없는 프로파일이면 코드 값을 쓴다.
        int[] byCode = converted(base, ColorProfile.fromCicp(12, 13));
        byte[] lut = new IccBuilder().tag("A2B0", new byte[64]).build();
        same(byCode, pixels(avif(icc("prof", lut), nclx(12, 13))), "an unsupported profile falls back to the code points");
        same(byCode, pixels(avif(icc("prof", new byte[] { 1, 2, 3, 4, 5 }), nclx(12, 13))), "damaged profile bytes fall back to the code points");
    }
}
