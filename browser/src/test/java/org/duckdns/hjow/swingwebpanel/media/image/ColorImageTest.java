package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;
import org.duckdns.hjow.swingwebpanel.media.image.color.IccBuilder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the colour management in the PNG, JPEG and BMP decoders (plan item P18-03; FEATURE_MATRIX IMG-01, IMG-02, IMG-04, IMG-05): the
 * colour chunks of PNG and the order of precedence of the specification (<code>cICP</code>, <code>iCCP</code>, <code>sRGB</code>,
 * <code>gAMA</code> with <code>cHRM</code>), a damaged or unsupported profile that falls back to the next chunk, gray profiles, palette and
 * animated PNG, the pieces of an ICC profile in several JPEG APP2 segments, and the calibrated RGB and the embedded profile of a BMP V4 or V5
 * header. The conversion itself is checked against independent references in
 * {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest}; here the expected pixels come from the {@link ColorProfile} that the file
 * should select, so the tests check that the decoders find the colour description in the file, pick the right one and apply it to every pixel.
 * <p>
 * PNG, JPEG, BMP 디코더의 색 관리 시험(계획 P18-03; FEATURE_MATRIX IMG-01, IMG-02, IMG-04, IMG-05): PNG 의 색 청크와 규격의 우선순위
 * (<code>cICP</code>, <code>iCCP</code>, <code>sRGB</code>, <code>gAMA</code> 와 <code>cHRM</code>), 다음 청크로 넘어가는 손상되거나 지원하지 않는
 * 프로파일, 회색조 프로파일, 팔레트와 애니메이션 PNG, 여러 JPEG APP2 세그먼트에 나뉜 ICC 프로파일 조각, BMP V4/V5 헤더의 보정된 RGB 와 내장 프로파일.
 * 변환 자체는 {@code org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfileTest} 가 독립된 기준과 대조하고, 여기서는 기대 픽셀을 파일이 고르는
 * {@link ColorProfile} 에서 얻는다. 디코더가 파일에서 색 설명을 찾고, 맞는 것을 고르고, 모든 픽셀에 적용하는지 확인한다.
 */
public class ColorImageTest {

    /**
     * Width of the PNG test image.
     * <p>
     * PNG 시험 이미지의 가로.
     */
    private static final int W = 4;

    /**
     * Height of the PNG test image.
     * <p>
     * PNG 시험 이미지의 세로.
     */
    private static final int H = 2;

    /**
     * The colours of the PNG test image (opaque, with strong and dark colours that a conversion changes).
     * <p>
     * PNG 시험 이미지의 색(불투명이며 변환이 바꾸는 강하고 어두운 색).
     */
    private static final int[] PIXELS = { 0xFFC86432, 0xFF0AC864, 0xFF3C5A8C, 0xFFFAFA1E, 0xFF808080, 0xFF101010, 0xFFF01010, 0xFF2020F0 };

    /**
     * The colorants of Display P3 relative to D50.
     * <p>
     * D50 기준 Display P3 색소.
     */
    private static final double[][] P3 = { { 0.5151, 0.2412, -0.0011 }, { 0.2920, 0.6922, 0.0419 }, { 0.1571, 0.0666, 0.7841 } };

    /**
     * The colorants of Adobe RGB (1998) relative to D50.
     * <p>
     * D50 기준 Adobe RGB (1998) 색소.
     */
    private static final double[][] A98 = { { 0.60974, 0.31111, 0.01947 }, { 0.20528, 0.62567, 0.06087 }, { 0.14919, 0.06322, 0.74457 } };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ColorImageTest() {
    }

    /**
     * Builds the ICC profile of Display P3 (the sRGB curve).
     * <p>
     * Display P3 의 ICC 프로파일(sRGB 곡선)을 만든다.
     *
     * @return the profile bytes / 프로파일 바이트
     */
    static byte[] p3Profile() {
        return IccBuilder.rgb(4, P3[0], P3[1], P3[2], IccBuilder.para(3, 2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045));
    }

    /**
     * Builds the ICC profile of Adobe RGB (1998).
     * <p>
     * Adobe RGB (1998)의 ICC 프로파일을 만든다.
     *
     * @return the profile bytes / 프로파일 바이트
     */
    static byte[] adobeProfile() {
        return IccBuilder.rgb(2, A98[0], A98[1], A98[2], IccBuilder.curv(563));
    }

    /**
     * Compresses data with zlib (the form of an <code>iCCP</code> chunk).
     * <p>
     * 데이터를 zlib 으로 압축한다(<code>iCCP</code> 청크의 형태).
     *
     * @param data the data / 데이터
     * @return the zlib stream / zlib 스트림
     */
    private static byte[] zlib(byte[] data) {
        return PngBuilder.deflate(data);
    }

    /**
     * Builds an <code>iCCP</code> chunk.
     * <p>
     * <code>iCCP</code> 청크를 만든다.
     *
     * @param profile the profile bytes / 프로파일 바이트
     * @return the complete chunk / 완전한 청크
     */
    private static byte[] iccp(byte[] profile) {
        byte[] z = zlib(profile);
        Bytes b = new Bytes().ascii("test profile").u8(0).u8(0).put(z);
        return PngBuilder.chunk("iCCP", b.toBytes());
    }

    /**
     * Builds a PNG of the test pixels with colour chunks.
     * <p>
     * 색 청크가 있는 시험 픽셀의 PNG 를 만든다.
     *
     * @param chunks the colour chunks (complete chunks) written before the image data / 이미지 데이터 앞에 쓰는 색 청크(완전한 청크)
     * @return the file / 파일
     */
    private static byte[] png(byte[]... chunks) {
        PngBuilder.Spec s = new PngBuilder.Spec(W, H);
        s.colorType = 6;
        s.depth = 8;
        s.samples = new int[W * H * 4];
        // 픽셀마다 R, G, B, A 샘플로 푼다.
        for (int i = 0; i < PIXELS.length; i++) {
            s.samples[i * 4] = (PIXELS[i] >> 16) & 0xFF;
            s.samples[i * 4 + 1] = (PIXELS[i] >> 8) & 0xFF;
            s.samples[i * 4 + 2] = PIXELS[i] & 0xFF;
            s.samples[i * 4 + 3] = (PIXELS[i] >>> 24) & 0xFF;
        }
        // 받은 청크를 모두 이미지 데이터 앞에 둔다.
        for (byte[] c : chunks) {
            s.extraChunks.add(c);
        }
        return PngBuilder.encode(s);
    }

    /**
     * Decodes a PNG and returns its pixels.
     * <p>
     * PNG 를 디코딩해 픽셀을 반환한다.
     *
     * @param file the file / 파일
     * @return the pixels of the first frame / 첫 프레임의 픽셀
     * @throws Exception on failure / 실패 시
     */
    private static int[] decode(byte[] file) throws Exception {
        return new PngDecoder().decode(file, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
    }

    /**
     * Returns the test pixels converted with a profile.
     * <p>
     * 시험 픽셀을 프로파일로 변환한 결과를 반환한다.
     *
     * @param p the profile / 프로파일
     * @return the converted pixels / 변환된 픽셀
     */
    private static int[] converted(ColorProfile p) {
        int[] r = PIXELS.clone();
        p.convert(r);
        return r;
    }

    /**
     * Checks the pixels of a decode against the expected ones.
     * <p>
     * 디코딩 결과의 픽셀을 기대 픽셀과 대조한다.
     *
     * @param expected the expected pixels / 기대 픽셀
     * @param actual   the pixels of the decode / 디코딩 결과의 픽셀
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
     * Checks that the test pixels were changed by the profile (so that a test of a conversion can tell it from no conversion).
     * <p>
     * 프로파일이 시험 픽셀을 바꾸는지 확인한다(변환을 안 한 것과 구별할 수 있도록).
     *
     * @param expected the converted pixels / 변환된 픽셀
     * @param what     description / 설명
     */
    private static void differs(int[] expected, String what) {
        int changed = 0;
        // 바뀐 픽셀의 수를 센다.
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != PIXELS[i]) {
                changed++;
            }
        }
        Check.isTrue(changed >= 4, what + ": the conversion has to change most pixels, changed " + changed);
    }

    /**
     * The chunks <code>sRGB</code> and a plain <code>gAMA</code> of 45455 leave the pixels as they are; a PNG without colour chunks too.
     * <p>
     * <code>sRGB</code> 청크와 45455 의 <code>gAMA</code> 만 있는 경우, 그리고 색 청크가 없는 PNG 는 픽셀을 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSrgbCasesLeaveThePixels() throws Exception {
        same(PIXELS, decode(png()), "no colour chunks");
        same(PIXELS, decode(png(PngBuilder.chunk("sRGB", new byte[] { 0 }))), "sRGB chunk");
        same(PIXELS, decode(png(PngBuilder.chunk("gAMA", new Bytes().be32(45455).toBytes()))), "gAMA 45455");
        byte[] chrm = new Bytes().be32(31270).be32(32900).be32(64000).be32(33000).be32(30000).be32(60000).be32(15000).be32(6000).toBytes();
        same(PIXELS, decode(png(PngBuilder.chunk("gAMA", new Bytes().be32(45455).toBytes()), PngBuilder.chunk("cHRM", chrm))), "gAMA with the sRGB cHRM");
        // cICP 가 sRGB(원색 1, 전달 특성 13)이면 그대로이다.
        same(PIXELS, decode(png(PngBuilder.chunk("cICP", new byte[] { 1, 13, 0, 1 }))), "cICP sRGB");
        // sRGB 와 같은 ICC 프로파일도 그대로이다.
        byte[] srgb = IccBuilder.rgb(4, new double[] { 0.43607, 0.22249, 0.01392 }, new double[] { 0.38515, 0.71687, 0.09708 },
                new double[] { 0.14307, 0.06061, 0.71410 }, IccBuilder.para(3, 2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045));
        same(PIXELS, decode(png(iccp(srgb))), "sRGB ICC profile");
    }

    /**
     * An <code>iCCP</code> chunk with the Display P3 profile converts every pixel; the alpha is kept; an Adobe RGB profile converts too.
     * <p>
     * Display P3 프로파일의 <code>iCCP</code> 청크가 모든 픽셀을 변환한다. 알파는 유지된다. Adobe RGB 프로파일도 변환한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testIccProfileChunk() throws Exception {
        int[] expected = converted(ColorProfile.fromIcc(p3Profile()));
        differs(expected, "Display P3");
        same(expected, decode(png(iccp(p3Profile()))), "iCCP Display P3");
        int[] adobe = converted(ColorProfile.fromIcc(adobeProfile()));
        differs(adobe, "Adobe RGB");
        same(adobe, decode(png(iccp(adobeProfile()))), "iCCP Adobe RGB");
        // 알파가 있는 픽셀은 알파를 유지한다.
        PngBuilder.Spec s = new PngBuilder.Spec(2, 1);
        s.colorType = 6;
        s.depth = 8;
        s.samples = new int[] { 200, 100, 50, 128, 10, 200, 100, 0 };
        s.extraChunks.add(iccp(p3Profile()));
        int[] px = decode(PngBuilder.encode(s));
        Check.equal(0x80, px[0] >>> 24, "alpha of pixel 0");
        Check.equal(0x00, px[1] >>> 24, "alpha of pixel 1");
        Check.equal(ColorProfile.fromIcc(p3Profile()).convertPixel(0xFFC86432) & 0xFFFFFF, px[0] & 0xFFFFFF, "colour of pixel 0");
    }

    /**
     * The order of precedence: <code>cICP</code> over <code>iCCP</code> over <code>sRGB</code> over <code>gAMA</code>; a chunk that cannot
     * be used (a damaged stream, a profile that is not supported, an unknown code point) is passed over to the next one.
     * <p>
     * 우선순위: <code>cICP</code> &gt; <code>iCCP</code> &gt; <code>sRGB</code> &gt; <code>gAMA</code>. 쓸 수 없는 청크(손상된 스트림, 지원하지 않는
     * 프로파일, 모르는 코드 값)는 다음 청크로 넘어간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPrecedenceAndFallback() throws Exception {
        byte[] gama = PngBuilder.chunk("gAMA", new Bytes().be32(100000).toBytes());
        int[] linear = converted(ColorProfile.fromGamma(1.0, null));
        differs(linear, "gamma 1.0");
        same(linear, decode(png(gama)), "gAMA alone");
        int[] p3 = converted(ColorProfile.fromIcc(p3Profile()));
        same(p3, decode(png(gama, iccp(p3Profile()))), "iCCP over gAMA");
        same(p3, decode(png(iccp(p3Profile()), PngBuilder.chunk("sRGB", new byte[] { 0 }), gama)), "iCCP over sRGB and gAMA");
        same(PIXELS, decode(png(PngBuilder.chunk("sRGB", new byte[] { 0 }), gama)), "sRGB over gAMA");
        int[] cicp = converted(ColorProfile.fromCicp(12, 13));
        differs(cicp, "cICP Display P3");
        same(cicp, decode(png(iccp(adobeProfile()), PngBuilder.chunk("cICP", new byte[] { 12, 13, 0, 1 }), gama)), "cICP over iCCP");
        // 지원하지 않는 프로파일(룩업 테이블만 있음)은 건너뛰고 sRGB 청크, 그 다음 gAMA 를 쓴다.
        byte[] lut = new IccBuilder().tag("A2B0", new byte[64]).build();
        same(PIXELS, decode(png(iccp(lut), PngBuilder.chunk("sRGB", new byte[] { 0 }))), "unsupported profile falls back to sRGB");
        same(linear, decode(png(iccp(lut), gama)), "unsupported profile falls back to gAMA");
        // 손상된 iCCP(zlib 스트림이 아님, 이름 없음, 압축 방식 오류)도 건너뛴다.
        byte[] bad1 = PngBuilder.chunk("iCCP", new Bytes().ascii("x").u8(0).u8(0).put(new byte[] { 1, 2, 3, 4, 5 }).toBytes());
        byte[] bad2 = PngBuilder.chunk("iCCP", new Bytes().u8(0).u8(0).put(zlib(p3Profile())).toBytes());
        byte[] bad3 = PngBuilder.chunk("iCCP", new Bytes().ascii("x").u8(0).u8(1).put(zlib(p3Profile())).toBytes());
        same(linear, decode(png(bad1, gama)), "damaged zlib stream");
        same(linear, decode(png(bad2, gama)), "profile without a name");
        same(linear, decode(png(bad3, gama)), "unknown compression method");
        // 모르는 cICP 코드(로그 전달 함수)나 RGB 가 아닌 행렬 계수는 건너뛴다.
        same(linear, decode(png(PngBuilder.chunk("cICP", new byte[] { 1, 9, 0, 1 }), gama)), "unsupported cICP code point");
        same(linear, decode(png(PngBuilder.chunk("cICP", new byte[] { 12, 13, 1, 1 }), gama)), "cICP with a YCbCr matrix");
    }

    /**
     * The chromaticities and the gamma convert like {@link ColorProfile#fromGamma(double, double[])}; a wrong length of a chunk and colour
     * chunks that come behind the image data are ignored.
     * <p>
     * 색도 좌표와 감마가 {@link ColorProfile#fromGamma(double, double[])} 처럼 변환한다. 길이가 틀린 청크와 이미지 데이터 뒤에 오는 색 청크는 무시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGammaChromaticitiesAndPlacement() throws Exception {
        byte[] gama = PngBuilder.chunk("gAMA", new Bytes().be32(45455).toBytes());
        byte[] chrm = PngBuilder.chunk("cHRM",
                new Bytes().be32(31270).be32(32900).be32(68000).be32(32000).be32(26500).be32(69000).be32(15000).be32(6000).toBytes());
        double[] values = { 0.3127, 0.3290, 0.680, 0.320, 0.265, 0.690, 0.150, 0.060 };
        int[] expected = converted(ColorProfile.fromGamma(0.45455, values));
        differs(expected, "gamma 2.2 with P3 primaries");
        same(expected, decode(png(gama, chrm)), "gAMA with cHRM");
        // cHRM 만 있으면 sRGB 곡선과 그 원색을 쓴다.
        int[] only = converted(ColorProfile.fromGamma(0, values));
        same(only, decode(png(chrm)), "cHRM alone");
        // 길이가 틀린 청크는 무시한다.
        same(PIXELS, decode(png(PngBuilder.chunk("gAMA", new Bytes().be32(100000).u8(0).toBytes()))), "gAMA of 5 bytes");
        same(PIXELS, decode(png(PngBuilder.chunk("cHRM", new byte[31]))), "cHRM of 31 bytes");
        // 이미지 데이터 뒤의 색 청크는 이미지에 영향을 주지 않는다(규격은 IDAT 앞에 있어야 한다고 정한다).
        byte[] plain = png();
        byte[] late = new byte[plain.length + gama.length];
        int at = plain.length - 12;
        System.arraycopy(plain, 0, late, 0, at);
        System.arraycopy(PngBuilder.chunk("gAMA", new Bytes().be32(100000).toBytes()), 0, late, at, gama.length);
        System.arraycopy(plain, at, late, at + gama.length, 12);
        same(PIXELS, decode(late), "gAMA behind the image data");
    }

    /**
     * Gray PNG with a gray profile (a gray TRC) converts through the curve; a gray profile on a colour image and a colour profile on a gray
     * image are handled as the kind of image requires; palette images convert their palette colours.
     * <p>
     * 회색조 프로파일(회색 TRC)이 있는 회색 PNG 는 곡선을 거쳐 변환된다. 컬러 이미지의 회색조 프로파일과 회색 이미지의 컬러 프로파일은 이미지 종류에 맞게
     * 처리된다. 팔레트 이미지는 팔레트 색을 변환한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGrayAndPaletteImages() throws Exception {
        byte[] gray18 = IccBuilder.gray(IccBuilder.curv(461));
        PngBuilder.Spec g = new PngBuilder.Spec(4, 1);
        g.colorType = 0;
        g.depth = 8;
        g.samples = new int[] { 0, 64, 128, 255 };
        g.extraChunks.add(iccp(gray18));
        int[] px = decode(PngBuilder.encode(g));
        ColorProfile gp = ColorProfile.fromIcc(gray18);
        // 회색 값마다 프로파일 변환과 같은지 본다.
        for (int i = 0; i < 4; i++) {
            Check.equal(gp.convertPixel(0xFF000000 | (g.samples[i] * 0x010101)), px[i], "gray value " + g.samples[i]);
        }
        Check.isTrue((px[1] & 0xFF) != 64, "the gray profile changes the value 64");
        // 컬러 이미지에 회색조 프로파일이 있으면 무시한다.
        PngBuilder.Spec c = new PngBuilder.Spec(W, H);
        c.colorType = 6;
        c.depth = 8;
        c.samples = new int[W * H * 4];
        // 픽셀마다 R, G, B, A 샘플로 푼다.
        for (int i = 0; i < PIXELS.length; i++) {
            c.samples[i * 4] = (PIXELS[i] >> 16) & 0xFF;
            c.samples[i * 4 + 1] = (PIXELS[i] >> 8) & 0xFF;
            c.samples[i * 4 + 2] = PIXELS[i] & 0xFF;
            c.samples[i * 4 + 3] = 255;
        }
        c.extraChunks.add(iccp(gray18));
        same(PIXELS, decode(PngBuilder.encode(c)), "a gray profile on a colour image");
        // 회색 이미지에 RGB 프로파일이 있으면 회색 값을 세 채널에 넣어 변환한다.
        PngBuilder.Spec g2 = new PngBuilder.Spec(2, 1);
        g2.colorType = 0;
        g2.depth = 8;
        g2.samples = new int[] { 100, 200 };
        g2.extraChunks.add(iccp(adobeProfile()));
        int[] gray2 = decode(PngBuilder.encode(g2));
        Check.equal(ColorProfile.fromIcc(adobeProfile()).convertPixel(0xFF646464), gray2[0], "gray value with an RGB profile");
        // 팔레트 이미지: 팔레트 색이 변환된다.
        PngBuilder.Spec pal = new PngBuilder.Spec(2, 1);
        pal.colorType = 3;
        pal.depth = 8;
        pal.samples = new int[] { 0, 1 };
        pal.plte = new byte[] { (byte) 200, 100, 50, 10, (byte) 200, 100 };
        // 팔레트 앞(IHDR 바로 뒤)에 iCCP 청크를 둔다.
        pal.extraChunks.add(iccp(p3Profile()));
        int[] pp = decode(PngBuilder.encode(pal));
        Check.equal(ColorProfile.fromIcc(p3Profile()).convertPixel(0xFFC86432), pp[0], "palette colour 0");
        Check.equal(ColorProfile.fromIcc(p3Profile()).convertPixel(0xFF0AC864), pp[1], "palette colour 1");
    }

    /**
     * Every frame of an animated PNG is converted.
     * <p>
     * 애니메이션 PNG 의 모든 프레임이 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimatedPng() throws Exception {
        PngBuilder.Spec s = new PngBuilder.Spec(2, 1);
        s.colorType = 6;
        s.depth = 8;
        s.samples = new int[8];
        s.extraChunks.add(PngBuilder.chunk("cICP", new byte[] { 12, 13, 0, 1 }));
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        frames.add(new PngBuilder.FrameSpec(2, 1, 0, 0, new int[] { 200, 100, 50, 255, 10, 200, 100, 255 }));
        frames.add(new PngBuilder.FrameSpec(2, 1, 0, 0, new int[] { 20, 30, 240, 255, 240, 240, 20, 255 }));
        DecodedImage img = new PngDecoder().decode(PngBuilder.encodeApng(s, 0, true, frames, false), DecodeLimits.DEFAULT);
        Check.equal(2, img.getFrames().size(), "two frames");
        ColorProfile p = ColorProfile.fromCicp(12, 13);
        Check.equal(p.convertPixel(0xFFC86432), img.getFrames().get(0).getArgb()[0], "frame 0 pixel 0");
        Check.equal(p.convertPixel(0xFF0AC864), img.getFrames().get(0).getArgb()[1], "frame 0 pixel 1");
        Check.equal(p.convertPixel(0xFF141EF0), img.getFrames().get(1).getArgb()[0], "frame 1 pixel 0");
        Check.equal(p.convertPixel(0xFFF0F014), img.getFrames().get(1).getArgb()[1], "frame 1 pixel 1");
    }

    /**
     * Writes an image as JPEG with the JRE's writer.
     * <p>
     * JRE 의 작성기로 이미지를 JPEG 로 쓴다.
     *
     * @param img the image / 이미지
     * @return file bytes / 파일 바이트
     * @throws Exception on failure / 실패 시
     */
    private static byte[] jpeg(BufferedImage img) throws Exception {
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(bos);
        w.setOutput(ios);
        w.write(null, new IIOImage(img, null, null), p);
        ios.close();
        w.dispose();
        return bos.toByteArray();
    }

    /**
     * Builds an APP2 segment with a piece of an ICC profile.
     * <p>
     * ICC 프로파일 조각이 든 APP2 세그먼트를 만든다.
     *
     * @param number the sequence number / 순서 번호
     * @param count  the number of pieces / 조각 수
     * @param data   the piece / 조각
     * @return the segment including its marker / 마커를 포함한 세그먼트
     */
    private static byte[] app2(int number, int count, byte[] data) {
        return new Bytes().u8(0xFF).u8(0xE2).be16(2 + 12 + 2 + data.length).ascii("ICC_PROFILE").u8(0).u8(number).u8(count).put(data).toBytes();
    }

    /**
     * Inserts segments right after the start-of-image marker.
     * <p>
     * 이미지 시작 마커 바로 뒤에 세그먼트를 넣는다.
     *
     * @param d        file / 파일
     * @param segments the segments / 세그먼트
     * @return the new file / 새 파일
     */
    private static byte[] insert(byte[] d, byte[]... segments) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(d, 0, 2);
        // 세그먼트를 차례로 쓴다.
        for (byte[] s : segments) {
            out.write(s, 0, s.length);
        }
        out.write(d, 2, d.length - 2);
        return out.toByteArray();
    }

    /**
     * Returns the pixels of a JPEG decode.
     * <p>
     * JPEG 디코딩의 픽셀을 반환한다.
     *
     * @param file the file / 파일
     * @return pixels / 픽셀
     * @throws Exception on failure / 실패 시
     */
    private static int[] decodeJpeg(byte[] file) throws Exception {
        return new JpegDecoder().decode(file, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
    }

    /**
     * Cuts an array into two pieces at a position.
     * <p>
     * 배열을 한 위치에서 두 조각으로 자른다.
     *
     * @param d  the array / 배열
     * @param at the position / 위치
     * @return the two pieces / 두 조각
     */
    private static byte[][] split(byte[] d, int at) {
        byte[] a = new byte[at];
        byte[] b = new byte[d.length - at];
        System.arraycopy(d, 0, a, 0, at);
        System.arraycopy(d, at, b, 0, b.length);
        return new byte[][] { a, b };
    }

    /**
     * A JPEG with an ICC profile in one APP2 segment, in two pieces and with the pieces in reverse order is converted; a missing piece, a
     * wrong count and a profile that is not supported leave the pixels alone; a CMYK-like unsupported profile type does not matter for an RGB
     * file.
     * <p>
     * APP2 세그먼트 하나, 둘로 나뉜 조각, 거꾸로 놓인 조각의 ICC 프로파일이 있는 JPEG 는 변환된다. 빠진 조각, 틀린 개수, 지원하지 않는 프로파일은
     * 픽셀을 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJpegProfilePieces() throws Exception {
        BufferedImage img = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        // 여러 색의 구역으로 칠한다.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                img.setRGB(x, y, PIXELS[((y / 8) * 4 + x / 8) % PIXELS.length] & 0xFFFFFF);
            }
        }
        byte[] plain = jpeg(img);
        int[] base = decodeJpeg(plain);
        byte[] icc = p3Profile();
        ColorProfile p3 = ColorProfile.fromIcc(icc);
        int[] expected = base.clone();
        p3.convert(expected);
        // 변환이 실제로 일어났는지 확인할 수 있어야 한다.
        int changed = 0;
        for (int i = 0; i < base.length; i++) {
            if (base[i] != expected[i]) {
                changed++;
            }
        }
        Check.isTrue(changed > base.length / 2, "the profile changes most pixels, changed " + changed);
        same(expected, decodeJpeg(insert(plain, app2(1, 1, icc))), "one piece");
        byte[][] two = split(icc, icc.length / 2);
        same(expected, decodeJpeg(insert(plain, app2(1, 2, two[0]), app2(2, 2, two[1]))), "two pieces");
        same(expected, decodeJpeg(insert(plain, app2(2, 2, two[1]), app2(1, 2, two[0]))), "two pieces in reverse order");
        same(base, decodeJpeg(insert(plain, app2(1, 2, two[0]))), "the second piece is missing");
        same(base, decodeJpeg(insert(plain, app2(1, 2, two[0]), app2(2, 3, two[1]))), "pieces that disagree about the count");
        same(base, decodeJpeg(insert(plain, app2(1, 2, two[0]), app2(1, 2, two[0]))), "the same piece twice");
        same(base, decodeJpeg(insert(plain, app2(0, 1, icc))), "sequence number 0");
        byte[] lut = new IccBuilder().tag("A2B0", new byte[64]).build();
        same(base, decodeJpeg(insert(plain, app2(1, 1, lut))), "a profile that is not supported");
        same(base, decodeJpeg(insert(plain, app2(1, 1, new byte[] { 1, 2, 3 }))), "junk instead of a profile");
        // 잘못된 식별자의 APP2 는 ICC 로 보지 않는다.
        byte[] fake = new Bytes().u8(0xFF).u8(0xE2).be16(2 + 12 + 2 + icc.length).ascii("ICC_PROFILX").u8(0).u8(1).u8(1).put(icc).toBytes();
        same(base, decodeJpeg(insert(plain, fake)), "another identifier");
    }

    /**
     * A gray JPEG is converted with a gray profile and with an RGB profile; a gray profile on an RGB JPEG is ignored.
     * <p>
     * 회색 JPEG 는 회색조 프로파일과 RGB 프로파일로 변환되고, RGB JPEG 의 회색조 프로파일은 무시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJpegGrayAndRgbProfiles() throws Exception {
        BufferedImage gray = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
        // 가로로 밝기가 변하는 회색 그림을 만든다.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int v = x * 16;
                gray.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        byte[] plain = jpeg(gray);
        int[] base = decodeJpeg(plain);
        byte[] gp = IccBuilder.gray(IccBuilder.curv(461));
        int[] expected = base.clone();
        ColorProfile.fromIcc(gp).convert(expected);
        same(expected, decodeJpeg(insert(plain, app2(1, 1, gp))), "gray image with a gray profile");
        int[] viaRgb = base.clone();
        ColorProfile.fromIcc(adobeProfile()).convert(viaRgb);
        same(viaRgb, decodeJpeg(insert(plain, app2(1, 1, adobeProfile()))), "gray image with an RGB profile");
        BufferedImage rgb = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        // 세로 방향 위치를 차례로 본다.
        for (int y = 0; y < 16; y++) {
            // 가로 방향 위치마다 값을 정한다.
            for (int x = 0; x < 16; x++) {
                rgb.setRGB(x, y, PIXELS[x % PIXELS.length] & 0xFFFFFF);
            }
        }
        byte[] plainRgb = jpeg(rgb);
        same(decodeJpeg(plainRgb), decodeJpeg(insert(plainRgb, app2(1, 1, gp))), "a gray profile on an RGB image is ignored");
    }

    /**
     * Builds a BMP file of 2 x 2 pixels with 24 bits and a header of the given size, with room for the colour space fields.
     * <p>
     * 24비트 2 x 2 픽셀 BMP 파일을 주어진 크기의 헤더로 만든다. 색 공간 필드가 들어갈 자리가 있다.
     *
     * @param headerSize 108 or 124 / 108 또는 124
     * @return the file / 파일
     */
    private static byte[] bmp(int headerSize) {
        int[] values = { 0xC86432, 0x0AC864, 0x3C5A8C, 0xFAFA1E };
        byte[] pixels = BmpBuilder.pack(2, 2, 24, values, false);
        return BmpBuilder.build(true, headerSize, 2, 2, 24, 0, 0, null, null, pixels);
    }

    /**
     * Writes a little-endian 32-bit value into an array.
     * <p>
     * 배열에 리틀 엔디언 32비트 값을 쓴다.
     *
     * @param d   the array / 배열
     * @param pos the position / 위치
     * @param v   the value / 값
     */
    private static void put32(byte[] d, int pos, long v) {
        d[pos] = (byte) v;
        d[pos + 1] = (byte) (v >> 8);
        d[pos + 2] = (byte) (v >> 16);
        d[pos + 3] = (byte) (v >> 24);
    }

    /**
     * The calibrated RGB of a BMP V4 header (the endpoints of Display P3 and the gamma 2.2) and the embedded profile of a V5 header convert the
     * pixels; the colour space types sRGB and Windows, a linked profile and damaged values leave them alone.
     * <p>
     * BMP V4 헤더의 보정된 RGB(Display P3 끝점과 감마 2.2)와 V5 헤더의 내장 프로파일이 픽셀을 변환한다. sRGB 와 Windows 색 공간 종류, 연결된 프로파일,
     * 손상된 값은 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBmpColorSpaces() throws Exception {
        byte[] plain = bmp(108);
        int[] base = new BmpDecoder().decode(plain, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
        // P3 의 선형 P3 에서 XYZ(D65)로의 행렬(각 열이 원색의 XYZ)을 2.30 고정 소수점으로 쓴다.
        double[][] toXyz = { { 608311.0 / 1250200, 189793.0 / 714400, 198249.0 / 1000160 }, { 35783.0 / 156275, 247089.0 / 357200, 198249.0 / 2500400 },
            { 0.0, 32229.0 / 714400, 5220557.0 / 5000800 } };
        double[] ends = { toXyz[0][0], toXyz[1][0], toXyz[2][0], toXyz[0][1], toXyz[1][1], toXyz[2][1], toXyz[0][2], toXyz[1][2], toXyz[2][2] };
        byte[] calibrated = plain.clone();
        put32(calibrated, 14 + 56, 0);
        // 끝점 아홉 값(2.30 고정 소수점)과 세 감마(16.16 고정 소수점)를 쓴다.
        for (int i = 0; i < 9; i++) {
            put32(calibrated, 14 + 60 + 4 * i, Math.round(ends[i] * 1073741824.0));
        }
        // 세 채널의 감마를 쓴다.
        for (int i = 0; i < 3; i++) {
            put32(calibrated, 14 + 96 + 4 * i, Math.round(2.2 * 65536.0));
        }
        int[] expected = base.clone();
        ColorProfile.fromCalibrated(ends, 2.2, 2.2, 2.2).convert(expected);
        Check.isTrue(expected[0] != base[0], "the endpoints change the colour");
        same(expected, new BmpDecoder().decode(calibrated, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), "calibrated RGB");
        // 끝점이 모두 0 이면(많은 작성기가 그렇게 둔다) 변환하지 않는다.
        byte[] zero = calibrated.clone();
        for (int i = 0; i < 9; i++) {
            put32(zero, 14 + 60 + 4 * i, 0);
        }
        same(base, new BmpDecoder().decode(zero, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), "calibrated RGB with zero endpoints");
        // sRGB('sRGB'), Windows('Win '), 연결('LINK') 색 공간은 그대로이다.
        long[] others = { 0x73524742L, 0x57696E20L, 0x4C494E4BL };
        for (int i = 0; i < others.length; i++) {
            byte[] variant = calibrated.clone();
            put32(variant, 14 + 56, others[i]);
            same(base, new BmpDecoder().decode(variant, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), "colour space type " + Long.toHexString(others[i]));
        }
        // V5: 파일 끝에 내장 프로파일을 붙이고 오프셋(헤더 시작 기준)과 크기를 쓴다.
        byte[] v5 = bmp(124);
        byte[] icc = p3Profile();
        byte[] withProfile = new byte[v5.length + icc.length];
        System.arraycopy(v5, 0, withProfile, 0, v5.length);
        System.arraycopy(icc, 0, withProfile, v5.length, icc.length);
        put32(withProfile, 14 + 56, 0x4D424544L);
        put32(withProfile, 14 + 112, v5.length - 14);
        put32(withProfile, 14 + 116, icc.length);
        int[] v5base = new BmpDecoder().decode(v5, DecodeLimits.DEFAULT).getFrames().get(0).getArgb();
        int[] v5expected = v5base.clone();
        ColorProfile.fromIcc(icc).convert(v5expected);
        same(v5expected, new BmpDecoder().decode(withProfile, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), "embedded profile");
        // 프로파일 위치가 파일 밖이면 무시한다.
        byte[] outside = withProfile.clone();
        put32(outside, 14 + 116, 100000);
        same(v5base, new BmpDecoder().decode(outside, DecodeLimits.DEFAULT).getFrames().get(0).getArgb(), "profile outside of the file");
    }
}
