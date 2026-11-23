package org.duckdns.hjow.swingwebpanel.media.image.color;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ColorProfile} (plan item P18-03; FEATURE_MATRIX IMG-01 to IMG-09, colour management): ICC matrix/TRC and gray profiles, the
 * curve types of ICC.1:2010 (identity, gamma, table and the five parametric functions), damaged and unsupported profiles, the code points of
 * ITU-T H.273 (primaries, transfer functions, PQ and HLG), PNG gamma and chromaticities and the calibrated RGB of BMP. The expected values are
 * computed here in double precision from the published definitions with constants that are typed in independently of the product code: the
 * matrices between linear sRGB, Display P3, Adobe RGB (1998) and Rec. 2020 and CIE XYZ of D65 (the fractions of CSS Color Module Level 4,
 * section 10), the sRGB, PQ and HLG functions, and the colorants of the profiles of Display P3 and Adobe RGB (1998) that are
 * adapted to D50. The product converts with its own Bradford adaptation and tables, so a result within one level of the reference confirms
 * the whole chain.
 * <p>
 * {@link ColorProfile} 시험(계획 P18-03; FEATURE_MATRIX IMG-01~IMG-09, 색 관리): ICC 행렬/TRC 와 회색조 프로파일, ICC.1:2010 의 곡선 형식(항등, 감마, 표,
 * 매개변수 함수 다섯 가지), 손상되었거나 지원하지 않는 프로파일, ITU-T H.273 의 코드 값(원색, 전달 함수, PQ, HLG), PNG 감마와 색도 좌표, BMP 의 보정된
 * RGB. 기대 값은 공표된 정의에서 배정밀도로 여기서 계산하며 제품 코드와 독립적으로 입력한 상수를 쓴다: 선형 sRGB, Display P3, Adobe RGB (1998), Rec. 2020
 * 과 D65 의 CIE XYZ 사이의 행렬(CSS Color Module Level 4 10절의 분수), sRGB, PQ, HLG 함수, D50 으로 적응된 Display P3 와 Adobe RGB (1998)
 * 프로파일의 색소. 제품은 자체 Bradford 적응과 표로 변환하므로 기준과 한 단계 이내로 같으면 전체 연쇄가 확인된다.
 */
public class ColorProfileTest {

    /**
     * The matrix from linear sRGB to XYZ (D65).
     * <p>
     * 선형 sRGB 에서 XYZ(D65)로의 행렬.
     */
    private static final double[][] SRGB_TO_XYZ = { { 506752.0 / 1228815, 87881.0 / 245763, 12673.0 / 70218 },
        { 87098.0 / 409605, 175762.0 / 245763, 12673.0 / 175545 }, { 7918.0 / 409605, 87881.0 / 737289, 1001167.0 / 1053270 } };

    /**
     * The matrix from XYZ (D65) to linear sRGB.
     * <p>
     * XYZ(D65)에서 선형 sRGB 로의 행렬.
     */
    private static final double[][] XYZ_TO_SRGB = { { 12831.0 / 3959, -329.0 / 214, -1974.0 / 3959 },
        { -851781.0 / 878810, 1648619.0 / 878810, 36519.0 / 878810 }, { 705.0 / 12673, -2585.0 / 12673, 705.0 / 667 } };

    /**
     * The matrix from linear Display P3 to XYZ (D65).
     * <p>
     * 선형 Display P3 에서 XYZ(D65)로의 행렬.
     */
    private static final double[][] P3_TO_XYZ = { { 608311.0 / 1250200, 189793.0 / 714400, 198249.0 / 1000160 },
        { 35783.0 / 156275, 247089.0 / 357200, 198249.0 / 2500400 }, { 0.0, 32229.0 / 714400, 5220557.0 / 5000800 } };

    /**
     * The matrix from linear Adobe RGB (1998) to XYZ (D65).
     * <p>
     * 선형 Adobe RGB (1998)에서 XYZ(D65)로의 행렬.
     */
    private static final double[][] A98_TO_XYZ = { { 573536.0 / 994567, 263643.0 / 1420810, 187206.0 / 994567 },
        { 591459.0 / 1989134, 6239551.0 / 9945670, 374412.0 / 4972835 }, { 53769.0 / 1989134, 351524.0 / 4972835, 4929758.0 / 4972835 } };

    /**
     * The matrix from linear Rec. 2020 to XYZ (D65).
     * <p>
     * 선형 Rec. 2020 에서 XYZ(D65)로의 행렬.
     */
    private static final double[][] REC2020_TO_XYZ = { { 63426534.0 / 99577255, 20160776.0 / 139408157, 47086771.0 / 278816314 },
        { 26158966.0 / 99577255, 472592308.0 / 697040785, 8267143.0 / 139408157 }, { 0.0, 19567812.0 / 697040785, 295819943.0 / 278816314 } };

    /**
     * Colours that the tests convert (red, green, blue values).
     * <p>
     * 시험에서 변환하는 색(빨강, 초록, 파랑 값).
     */
    private static final int[][] COLORS = { { 200, 100, 50 }, { 10, 200, 100 }, { 255, 255, 255 }, { 0, 0, 0 }, { 128, 128, 128 }, { 30, 60, 90 },
        { 250, 10, 10 }, { 5, 5, 200 }, { 90, 140, 60 }, { 220, 220, 30 } };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ColorProfileTest() {
    }

    /**
     * The sRGB decoding function (IEC 61966-2-1).
     * <p>
     * sRGB 복호 함수(IEC 61966-2-1).
     *
     * @param v encoded value / 부호화된 값
     * @return linear value / 선형 값
     */
    static double srgbToLinear(double v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * Encodes a linear value as an 8-bit sRGB value, clipping the value to 0 to 1 first.
     * <p>
     * 선형 값을 8비트 sRGB 값으로 부호화한다. 먼저 0~1 로 자른다.
     *
     * @param l linear value / 선형 값
     * @return 0 to 255 / 0~255
     */
    static int encode8(double l) {
        double c = Math.max(0.0, Math.min(1.0, l));
        double v = c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
        return (int) Math.round(v * 255.0);
    }

    /**
     * Multiplies a matrix with a vector.
     * <p>
     * 행렬과 벡터를 곱한다.
     *
     * @param m matrix / 행렬
     * @param v vector / 벡터
     * @return product / 곱
     */
    static double[] mul(double[][] m, double[] v) {
        double[] r = new double[3];
        // 행마다 내적을 구한다.
        for (int i = 0; i < 3; i++) {
            r[i] = m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2];
        }
        return r;
    }

    /**
     * Computes the reference result of a conversion: the decoded linear components go through the matrix to XYZ (D65) and the matrix to
     * sRGB, are clipped and encoded.
     * <p>
     * 변환의 기준 결과를 계산한다: 복호한 선형 성분이 XYZ(D65)로의 행렬과 sRGB 로의 행렬을 거쳐 잘린 뒤 부호화된다.
     *
     * @param toXyz  the matrix from the source to XYZ (D65) / 소스에서 XYZ(D65)로의 행렬
     * @param linear the decoded linear components / 복호한 선형 성분
     * @return the 8-bit sRGB components / 8비트 sRGB 성분
     */
    static int[] reference(double[][] toXyz, double[] linear) {
        double[] s = mul(XYZ_TO_SRGB, mul(toXyz, linear));
        return new int[] { encode8(s[0]), encode8(s[1]), encode8(s[2]) };
    }

    /**
     * Checks that a pixel is within a tolerance of the expected components and keeps its alpha.
     * <p>
     * 픽셀이 기대 성분과 허용 오차 이내이고 알파를 유지하는지 검사한다.
     *
     * @param expected the expected {r, g, b} / 기대 {r, g, b}
     * @param actual   the pixel <code>0xAARRGGBB</code> / 픽셀 <code>0xAARRGGBB</code>
     * @param tol      the tolerance per component / 성분별 허용 오차
     * @param what     description / 설명
     */
    static void near(int[] expected, int actual, int tol, String what) {
        int[] got = { (actual >> 16) & 0xFF, (actual >> 8) & 0xFF, actual & 0xFF };
        // 세 성분을 차례로 비교한다.
        for (int i = 0; i < 3; i++) {
            if (Math.abs(expected[i] - got[i]) > tol) {
                Check.fail(what + ": expected (" + expected[0] + "," + expected[1] + "," + expected[2] + ") but was (" + got[0] + "," + got[1] + ","
                        + got[2] + ")");
            }
        }
        Check.equal(0xFF, (actual >>> 24) & 0xFF, what + ": alpha");
    }

    /**
     * Converts the test colours with a profile and compares them with a reference function.
     * <p>
     * 시험 색을 프로파일로 변환해 기준 함수와 비교한다.
     *
     * @param p      the profile / 프로파일
     * @param toXyz  the matrix of the reference / 기준의 행렬
     * @param decode the decoding exponent or -1 for the sRGB function / 복호 지수. sRGB 함수는 -1
     * @param what   description / 설명
     */
    private static void compareColors(ColorProfile p, double[][] toXyz, double decode, String what) {
        // 색마다 기준을 계산해 변환 결과와 비교한다.
        for (int i = 0; i < COLORS.length; i++) {
            double[] lin = new double[3];
            // 세 성분을 차례로 선형 값으로 바꾼다.
            for (int k = 0; k < 3; k++) {
                double v = COLORS[i][k] / 255.0;
                lin[k] = decode < 0 ? srgbToLinear(v) : Math.pow(v, decode);
            }
            int in = 0xFF000000 | (COLORS[i][0] << 16) | (COLORS[i][1] << 8) | COLORS[i][2];
            near(reference(toXyz, lin), p.convertPixel(in), 1, what + " colour " + i);
        }
    }

    /**
     * The sRGB parametric curve of ICC.1:2010 (type 3).
     * <p>
     * ICC.1:2010 의 sRGB 매개변수 곡선(형식 3).
     *
     * @return the tag data / 태그 데이터
     */
    private static byte[] srgbPara() {
        return IccBuilder.para(3, 2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045);
    }

    /**
     * Profiles that are sRGB in fact (a parametric curve, a 1024-entry table like the one of the classic HP profile) are the
     * {@link ColorProfile#SRGB} object, so that images that are already sRGB keep their pixels exactly.
     * <p>
     * 사실상 sRGB 인 프로파일(매개변수 곡선, 고전적인 HP 프로파일 같은 1024 항목 표)은 {@link ColorProfile#SRGB} 객체이므로 이미 sRGB 인 이미지는 픽셀이
     * 그대로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSrgbEquivalentProfilesSkipTheConversion() throws Exception {
        double[] r = { 0.43607, 0.22249, 0.01392 };
        double[] g = { 0.38515, 0.71687, 0.09708 };
        double[] b = { 0.14307, 0.06061, 0.71410 };
        ColorProfile para = ColorProfile.fromIcc(IccBuilder.rgb(4, r, g, b, srgbPara()));
        Check.same(ColorProfile.SRGB, para, "sRGB with the parametric curve");
        int[] table = new int[1024];
        // 1024 항목 표를 sRGB 곡선으로 채운다.
        for (int i = 0; i < table.length; i++) {
            table[i] = (int) Math.round(srgbToLinear(i / 1023.0) * 65535.0);
        }
        ColorProfile tab = ColorProfile.fromIcc(IccBuilder.rgb(2, r, g, b, IccBuilder.curv(table)));
        Check.same(ColorProfile.SRGB, tab, "sRGB with a 1024-entry table (version 2)");
        Check.isTrue(para.isIdentity(), "identity flag");
        Check.equal(0xFF123456, ColorProfile.SRGB.convertPixel(0xFF123456), "no change");
        int[] px = { 0xFF102030, 0x80FFFFFF };
        ColorProfile.SRGB.convert(px);
        Check.equal(0xFF102030, px[0], "array unchanged");
    }

    /**
     * A Display P3 profile (colorants adapted to D50, the sRGB curve) converts to the sRGB values of the CSS Color 4 definition, the
     * components outside the sRGB gamut are clipped, and gray stays gray.
     * <p>
     * Display P3 프로파일(D50 으로 적응된 색소, sRGB 곡선)이 CSS Color 4 정의의 sRGB 값으로 변환되고, sRGB 영역 밖의 성분은 잘리며, 회색은 회색으로
     * 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisplayP3Profile() throws Exception {
        double[] r = { 0.5151, 0.2412, -0.0011 };
        double[] g = { 0.2920, 0.6922, 0.0419 };
        double[] b = { 0.1571, 0.0666, 0.7841 };
        // 두 가지 버전의 같은 프로파일이 같은 결과를 준다.
        for (int version = 2; version <= 4; version += 2) {
            ColorProfile p = ColorProfile.fromIcc(IccBuilder.rgb(version, r, g, b, srgbPara()));
            Check.notNull(p, "the profile is understood (version " + version + ")");
            Check.isFalse(p.isIdentity(), "a wide gamut profile is converted");
            compareColors(p, P3_TO_XYZ, -1, "Display P3 version " + version);
            // 중간 회색은 회색으로 남는다(백색점이 맞게 적응되었는지 확인한다).
            for (int v = 0; v <= 255; v += 15) {
                int out = p.convertPixel(0xFF000000 | (v << 16) | (v << 8) | v);
                int[] gray = { v, v, v };
                near(gray, out, 1, "neutral " + v + " version " + version);
            }
        }
        ColorProfile p = ColorProfile.fromIcc(IccBuilder.rgb(4, r, g, b, srgbPara()));
        // P3 의 순수한 초록은 sRGB 영역 밖이라 선형 값이 음수가 되므로 0 으로 잘린다.
        int green = p.convertPixel(0xFF00FF00);
        Check.equal(0, (green >> 16) & 0xFF, "P3 green is outside sRGB: red component clipped");
        Check.equal(255, (green >> 8) & 0xFF, "P3 green: green component");
        Check.equal(0, green & 0xFF, "P3 green: blue component clipped");
    }

    /**
     * An Adobe RGB (1998) profile with the gamma 563/256 (a <code>curv</code> tag with one u8Fixed8 entry) converts like the reference.
     * <p>
     * 감마 563/256(u8Fixed8 항목 하나의 <code>curv</code> 태그)인 Adobe RGB (1998) 프로파일이 기준과 같게 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAdobeRgbProfile() throws Exception {
        double[] r = { 0.60974, 0.31111, 0.01947 };
        double[] g = { 0.20528, 0.62567, 0.06087 };
        double[] b = { 0.14919, 0.06322, 0.74457 };
        ColorProfile p = ColorProfile.fromIcc(IccBuilder.rgb(2, r, g, b, IccBuilder.curv(563)));
        Check.notNull(p, "the profile is understood");
        compareColors(p, A98_TO_XYZ, 563.0 / 256.0, "Adobe RGB");
    }

    /**
     * A gray profile with a gamma (<code>curv</code> with one entry) converts to the gray of the same luminance.
     * <p>
     * 감마가 있는(항목 하나의 <code>curv</code>) 회색조 프로파일이 같은 휘도의 회색으로 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGrayProfile() throws Exception {
        ColorProfile p = ColorProfile.fromIcc(IccBuilder.gray(IccBuilder.curv(461)));
        Check.notNull(p, "the profile is understood");
        Check.isTrue(p.isGray(), "gray profile");
        double gamma = 461.0 / 256.0;
        // 회색 값마다 선형 값을 구해 sRGB 로 부호화한 기준과 비교한다.
        for (int v = 0; v < 256; v += 5) {
            int e = encode8(Math.pow(v / 255.0, gamma));
            near(new int[] { e, e, e }, p.convertPixel(0xFF000000 | (v << 16) | (v << 8) | v), 1, "gray " + v);
        }
        Check.same(ColorProfile.SRGB, p.forImage(false), "a gray profile does not fit a colour image");
        Check.same(p, p.forImage(true), "a gray profile fits a gray image");
    }

    /**
     * Reference value of a parametric curve of ICC.1:2010 Table 68 with parameters quantised to s15Fixed16.
     * <p>
     * s15Fixed16 으로 양자화한 매개변수의 ICC.1:2010 표 68 매개변수 곡선의 기준 값.
     *
     * @param type the function type / 함수 형식
     * @param q    the parameters g, a, b, c, d, e, f / 매개변수 g, a, b, c, d, e, f
     * @param x    the input / 입력
     * @return the output / 출력
     */
    private static double parametric(int type, double[] q, double x) {
        double g = q[0];
        double a = q.length > 1 ? q[1] : 0;
        double b = q.length > 2 ? q[2] : 0;
        double c = q.length > 3 ? q[3] : 0;
        double d = q.length > 4 ? q[4] : 0;
        double e = q.length > 5 ? q[5] : 0;
        double f = q.length > 6 ? q[6] : 0;
        switch (type) {
        case 0:
            return Math.pow(x, g);
        case 1:
            return x >= -b / a ? Math.pow(a * x + b, g) : 0;
        case 2:
            return x >= -b / a ? Math.pow(a * x + b, g) + c : c;
        case 3:
            return x >= d ? Math.pow(a * x + b, g) : c * x;
        default:
            return x >= d ? Math.pow(a * x + b, g) + e : c * x + f;
        }
    }

    /**
     * All five parametric functions agree with their definition (a gray profile exposes the curve directly).
     * <p>
     * 다섯 가지 매개변수 함수가 모두 정의와 같다(회색조 프로파일은 곡선을 그대로 드러낸다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testParametricCurves() throws Exception {
        double[][] sets = { { 1.8 }, { 2.0, 0.9, 0.1 }, { 2.0, 0.8, 0.2, 0.05 }, { 2.4, 0.947867, 0.052133, 0.0774, 0.04045 },
            { 2.2, 0.9, 0.1, 0.1, 0.2, 0.02, 0.03 }, { 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 } };
        int[] types = { 0, 1, 2, 3, 4, 4 };
        // 형식마다 회색 값 전체를 곡선으로 변환한 결과를 기준과 비교한다.
        for (int s = 0; s < sets.length; s++) {
            ColorProfile p = ColorProfile.fromIcc(IccBuilder.gray(IccBuilder.para(types[s], sets[s])));
            Check.notNull(p, "type " + types[s] + " set " + s + " understood");
            double[] q = new double[sets[s].length];
            // 기준은 s15Fixed16 으로 양자화한 매개변수를 쓴다.
            for (int i = 0; i < q.length; i++) {
                q[i] = Math.round(sets[s][i] * 65536.0) / 65536.0;
            }
            // 회색 값을 건너뛰며 곡선의 결과를 기준과 비교한다.
            for (int v = 0; v < 256; v += 3) {
                int e = encode8(parametric(types[s], q, v / 255.0));
                near(new int[] { e, e, e }, p.convertPixel(0xFF000000 | (v << 16) | (v << 8) | v), 1, "type " + types[s] + " set " + s + " value " + v);
            }
        }
    }

    /**
     * A <code>curv</code> table is interpolated linearly, no entry is the identity, and a curve is applied per channel.
     * <p>
     * <code>curv</code> 표는 선형 보간되고, 항목이 없으면 항등이며, 곡선은 채널마다 적용된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTableCurvesAndPerChannelCurves() throws Exception {
        int[] table = { 0, 4096, 20000, 41000, 65535 };
        ColorProfile p = ColorProfile.fromIcc(IccBuilder.gray(IccBuilder.curv(table)));
        // 표의 구간 사이를 선형 보간한 기준과 비교한다.
        for (int v = 0; v < 256; v += 7) {
            double pos = v / 255.0 * 4;
            int i = Math.min(3, (int) Math.floor(pos));
            double lin = (table[i] + (table[i + 1] - table[i]) * (pos - i)) / 65535.0;
            int e = encode8(lin);
            near(new int[] { e, e, e }, p.convertPixel(0xFF000000 | (v << 16) | (v << 8) | v), 1, "table " + v);
        }
        ColorProfile id = ColorProfile.fromIcc(IccBuilder.gray(IccBuilder.curv()));
        // 회색 값을 건너뛰며 항등 곡선의 결과를 확인한다.
        for (int v = 0; v < 256; v += 17) {
            int e = encode8(v / 255.0);
            near(new int[] { e, e, e }, id.convertPixel(0xFF000000 | (v << 16) | (v << 8) | v), 1, "identity curve " + v);
        }
        // 세 채널에 서로 다른 곡선을 쓴 sRGB 원색 프로파일: 채널마다 곡선이 따로 적용되는지 본다.
        double[] r = { 0.43607, 0.22249, 0.01392 };
        double[] g = { 0.38515, 0.71687, 0.09708 };
        double[] b = { 0.14307, 0.06061, 0.71410 };
        byte[] profile = new IccBuilder().colorants(r, g, b).curves(IccBuilder.curv(256), IccBuilder.curv(), srgbPara()).build();
        ColorProfile mixed = ColorProfile.fromIcc(profile);
        Check.notNull(mixed, "profile with three different curves");
        int out = mixed.convertPixel(0xFF808080);
        double[] lin = { Math.pow(128 / 255.0, 1.0), 128 / 255.0, srgbToLinear(128 / 255.0) };
        near(reference(SRGB_TO_XYZ, lin), out, 1, "per channel curves");
    }

    /**
     * Damaged profiles and profiles of unsupported kinds give null and never throw; the image then stays sRGB.
     * <p>
     * 손상된 프로파일과 지원하지 않는 종류의 프로파일은 null 을 주고 예외를 던지지 않는다. 그러면 이미지는 sRGB 로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsupportedAndDamagedProfiles() throws Exception {
        double[] r = { 0.5151, 0.2412, -0.0011 };
        double[] g = { 0.2920, 0.6922, 0.0419 };
        double[] b = { 0.1571, 0.0666, 0.7841 };
        byte[] good = IccBuilder.rgb(4, r, g, b, srgbPara());
        Check.notNull(ColorProfile.fromIcc(good), "control");
        Check.isNull(ColorProfile.fromIcc(null), "null");
        Check.isNull(ColorProfile.fromIcc(new byte[0]), "empty");
        Check.isNull(ColorProfile.fromIcc(new byte[100]), "shorter than the header");
        byte[] noSig = good.clone();
        noSig[36] = 'x';
        Check.isNull(ColorProfile.fromIcc(noSig), "no acsp signature");
        byte[] lut = new IccBuilder().tag("A2B0", new byte[64]).build();
        Check.isNull(ColorProfile.fromIcc(lut), "a profile with look-up tables only");
        byte[] cmyk = new IccBuilder().spaces("CMYK", "Lab ", "prtr").tag("A2B0", new byte[64]).build();
        Check.isNull(ColorProfile.fromIcc(cmyk), "CMYK");
        byte[] lab = new IccBuilder().spaces("RGB ", "Lab ", "mntr").colorants(r, g, b).curves(srgbPara(), srgbPara(), srgbPara()).build();
        Check.isNull(ColorProfile.fromIcc(lab), "a Lab connection space");
        byte[] link = new IccBuilder().spaces("RGB ", "XYZ ", "link").colorants(r, g, b).curves(srgbPara(), srgbPara(), srgbPara()).build();
        Check.isNull(ColorProfile.fromIcc(link), "a device link profile");
        byte[] noCurve = new IccBuilder().colorants(r, g, b).tag("rTRC", srgbPara()).tag("gTRC", srgbPara()).build();
        Check.isNull(ColorProfile.fromIcc(noCurve), "the blue curve is missing");
        byte[] noColorant = new IccBuilder().tag("rXYZ", IccBuilder.xyz(r)).curves(srgbPara(), srgbPara(), srgbPara()).build();
        Check.isNull(ColorProfile.fromIcc(noColorant), "colorants are missing");
        // 곡선 항목 수를 태그 크기보다 크게(1000 개), 그리고 표 한도(65536)보다 크게(131075 개) 고친다.
        byte[] tooLong = IccBuilder.curv(1, 2, 3);
        tooLong[10] = 0x03;
        tooLong[11] = (byte) 0xE8;
        Check.isNull(ColorProfile.fromIcc(new IccBuilder().colorants(r, g, b).curves(tooLong, tooLong, tooLong).build()),
                "a table that is larger than its tag");
        byte[] beyondLimit = IccBuilder.curv(1, 2, 3);
        beyondLimit[9] = 0x02;
        Check.isNull(ColorProfile.fromIcc(new IccBuilder().colorants(r, g, b).curves(beyondLimit, beyondLimit, beyondLimit).build()),
                "a table over the entry limit");
        // 잘린 파일은 어디서 잘려도 예외 없이 null 이거나 쓸 수 있는 프로파일이다.
        for (int n = 0; n < good.length; n += 7) {
            byte[] cut = new byte[n];
            System.arraycopy(good, 0, cut, 0, n);
            ColorProfile.fromIcc(cut);
        }
        // 바이트 하나씩 망가뜨려도 예외가 나지 않는다.
        for (int i = 0; i < good.length; i++) {
            byte[] bad = good.clone();
            bad[i] = (byte) (bad[i] ^ 0xFF);
            ColorProfile.fromIcc(bad);
        }
    }

    /**
     * Reference result of a conversion with the HDR curves: PQ (SMPTE ST 2084) divides the absolute luminance by 203 cd/m2, HLG (ITU-R
     * BT.2100) applies the system gamma 1.2 for a peak of 1000 cd/m2 and divides by 203 cd/m2.
     * <p>
     * HDR 곡선 변환의 기준 결과: PQ(SMPTE ST 2084)는 절대 휘도를 203 cd/m2 로 나누고 HLG(ITU-R BT.2100)는 최고 1000 cd/m2 에 대한 시스템 감마 1.2 를 적용한 뒤
     * 203 cd/m2 로 나눈다.
     *
     * @param hlg   true for HLG, false for PQ / HLG 이면 true, PQ 이면 false
     * @param toXyz the matrix of the primaries to XYZ (D65) / 원색에서 XYZ(D65)로의 행렬
     * @param rgb   the encoded values 0 to 255 / 부호화된 값 0~255
     * @return the 8-bit sRGB components / 8비트 sRGB 성분
     */
    private static int[] hdr(boolean hlg, double[][] toXyz, int[] rgb) {
        double[] lin = new double[3];
        // 성분마다 부호화된 값을 선형 광으로 바꾼다.
        for (int k = 0; k < 3; k++) {
            double v = rgb[k] / 255.0;
            if (hlg) {
                lin[k] = v <= 0.5 ? v * v / 3.0 : (Math.exp((v - 0.55991073) / 0.17883277) + 0.28466892) / 12.0;
            } else {
                double m1 = 0.1593017578125;
                double m2 = 78.84375;
                double c1 = 0.8359375;
                double c2 = 18.8515625;
                double c3 = 18.6875;
                double p = Math.pow(v, 1.0 / m2);
                lin[k] = Math.pow(Math.max(p - c1, 0.0) / (c2 - c3 * p), 1.0 / m1) * 10000.0 / 203.0;
            }
        }
        if (hlg) {
            double ys = toXyz[1][0] * lin[0] + toXyz[1][1] * lin[1] + toXyz[1][2] * lin[2];
            double gain = ys <= 0 ? 0.0 : Math.pow(ys, 0.2) * 1000.0 / 203.0;
            // 세 성분을 차례로 HDR 선형 광으로 바꾼다.
            for (int k = 0; k < 3; k++) {
                lin[k] *= gain;
            }
        }
        return reference(toXyz, lin);
    }

    /**
     * The code points of ITU-T H.273: sRGB and unspecified values are the identity, other primaries and transfer functions convert like the
     * reference, and logarithmic and unknown ones are refused.
     * <p>
     * ITU-T H.273 의 코드 값: sRGB 와 지정되지 않은 값은 항등이고, 다른 원색과 전달 함수는 기준과 같게 변환되며, 로그와 모르는 값은 거부된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCodePoints() throws Exception {
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(1, 13), "Rec. 709 primaries with the sRGB curve");
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(2, 2), "unspecified");
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(0, 0), "reserved values");
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(1, 2), "unspecified transfer");
        compareColors(ColorProfile.fromCicp(12, 13), P3_TO_XYZ, -1, "Display P3 with the sRGB curve");
        compareColors(ColorProfile.fromCicp(9, 13), REC2020_TO_XYZ, -1, "Rec. 2020 with the sRGB curve");
        compareColors(ColorProfile.fromCicp(1, 8), SRGB_TO_XYZ, 1.0, "linear");
        compareColors(ColorProfile.fromCicp(1, 4), SRGB_TO_XYZ, 2.2, "gamma 2.2");
        compareColors(ColorProfile.fromCicp(1, 5), SRGB_TO_XYZ, 2.8, "gamma 2.8");
        // Rec. 709(1), SMPTE 170M(6), Rec. 2020(14, 15)의 전달 함수는 Chromium 처럼 sRGB 곡선이다.
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(1, 1), "Rec. 709 curve on Rec. 709 primaries is sRGB");
        Check.same(ColorProfile.SRGB, ColorProfile.fromCicp(1, 6), "SMPTE 170M curve");
        compareColors(ColorProfile.fromCicp(9, 14), REC2020_TO_XYZ, -1, "Rec. 2020 10 bit curve is the sRGB curve");
        compareColors(ColorProfile.fromCicp(9, 15), REC2020_TO_XYZ, -1, "Rec. 2020 12 bit curve is the sRGB curve");
        Check.isNull(ColorProfile.fromCicp(1, 9), "a logarithmic transfer function");
        Check.isNull(ColorProfile.fromCicp(1, 17), "SMPTE ST 428");
        Check.isNull(ColorProfile.fromCicp(10, 13), "XYZ primaries");
        // BT.2020 원색의 PQ 와 HLG 는 표준 동적 범위로 내려 맞춘 기준과 비교한다.
        ColorProfile pq = ColorProfile.fromCicp(9, 16);
        ColorProfile hlg = ColorProfile.fromCicp(9, 18);
        int[][] samples = { { 50, 50, 50 }, { 100, 100, 100 }, { 148, 148, 148 }, { 200, 200, 200 }, { 255, 255, 255 }, { 120, 90, 60 }, { 60, 130, 90 },
            { 140, 100, 160 }, { 190, 150, 100 } };
        // 샘플마다 PQ 와 HLG 결과를 기준과 비교한다.
        for (int i = 0; i < samples.length; i++) {
            int in = 0xFF000000 | (samples[i][0] << 16) | (samples[i][1] << 8) | samples[i][2];
            near(hdr(false, REC2020_TO_XYZ, samples[i]), pq.convertPixel(in), 1, "PQ sample " + i);
            near(hdr(true, REC2020_TO_XYZ, samples[i]), hlg.convertPixel(in), 1, "HLG sample " + i);
        }
        // HDR 기준 백색: PQ 약 58 %(203 cd/m2), HLG 75 % 는 표준 동적 범위의 흰색 근처이다.
        int pqWhite = pq.convertPixel(0xFF949494) & 0xFF;
        Check.isTrue(pqWhite >= 250, "PQ reference white (58 percent) shows as white, was " + pqWhite);
        int hlgWhite = hlg.convertPixel(0xFFBFBFBF) & 0xFF;
        Check.isTrue(hlgWhite >= 250, "HLG reference white (75 percent) shows as white, was " + hlgWhite);
        // PQ 의 중간 밝기(10 % 신호)는 흰색이 아니고 어둡다.
        int pqDark = pq.convertPixel(0xFF1A1A1A) & 0xFF;
        Check.isTrue(pqDark < 40, "PQ 10 percent signal is dark, was " + pqDark);
    }

    /**
     * The expected values of a real conversion library: ffmpeg N-125875 with libzimg (<code>zscale</code>) converted the colours of
     * the seven colours of this test from each of the primaries below (with the sRGB transfer function) to sRGB on this machine on 2026-11-18
     * (<code>color=c=...,format=gbrp,setparams=...color_primaries=P:color_trc=iec61966-2-1</code>, then
     * <code>zscale=p=bt709:t=iec61966-2-1:m=gbr:r=pc</code>), and the same colours with linear light on the Rec. 709 primaries. The values are
     * typed in here; libzimg is not needed to run the test. The conversion of the product (its own Bradford adaptation and tables) must be within
     * one level of them.
     * <p>
     * 실제 변환 라이브러리의 기대 값: ffmpeg N-125875 의 libzimg(<code>zscale</code>)가 2026-11-18 에 이 PC 에서 아래 원색(sRGB 전달 함수)마다 the seven colours of this test
     * 의 색을 sRGB 로 바꾼 결과와, Rec. 709 원색의 선형 광에 같은 색을 바꾼 결과. 값을 여기에 적었으므로 시험에 libzimg 는 필요 없다. 제품의 변환(자체
     * Bradford 적응과 표)이 이 값과 한 단계 이내여야 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMatchesLibzimg() throws Exception {
        int[] colors = { 0xC86432, 0x0AC864, 0x3C5A8C, 0xFAFA1E, 0x808080, 0x202020, 0xF01010 };
        // {원색 코드 값, 전달 특성 코드 값, 색마다 기대 R, G, B ...}
        int[][] cases = {
            { 12, 13, 215, 93, 31, 0, 204, 87, 50, 91, 144, 250, 250, 0, 128, 128, 128, 32, 32, 32, 255, 0, 0 },
            { 9, 13, 241, 76, 29, 0, 211, 82, 0, 92, 145, 255, 251, 0, 128, 128, 128, 32, 32, 32, 255, 0, 0 },
            { 11, 13, 211, 93, 32, 0, 204, 87, 53, 91, 144, 250, 250, 0, 128, 128, 128, 32, 32, 32, 255, 0, 0 },
            { 5, 13, 203, 100, 51, 0, 200, 102, 58, 90, 140, 250, 250, 43, 128, 128, 128, 32, 32, 32, 245, 16, 16 },
            { 6, 13, 196, 102, 49, 51, 197, 99, 63, 91, 140, 249, 248, 21, 128, 128, 128, 32, 32, 32, 234, 39, 12 },
            { 1, 8, 229, 168, 122, 56, 229, 168, 133, 160, 195, 253, 253, 96, 188, 188, 188, 99, 99, 99, 248, 71, 71 } };
        // 원색 조합마다 색 일곱 개를 변환해 zscale 의 값과 비교한다.
        for (int c = 0; c < cases.length; c++) {
            ColorProfile p = ColorProfile.fromCicp(cases[c][0], cases[c][1]);
            // 색 일곱 개를 차례로 변환해 기대 값과 비교한다.
            for (int i = 0; i < colors.length; i++) {
                int[] expected = { cases[c][2 + 3 * i], cases[c][3 + 3 * i], cases[c][4 + 3 * i] };
                near(expected, p.convertPixel(0xFF000000 | colors[i]), 1, "libzimg primaries " + cases[c][0] + " transfer " + cases[c][1] + " colour " + i);
            }
        }
    }

    /**
     * The PNG gamma and chromaticities: a gamma of 0.45455 or a missing gamma on sRGB primaries is the identity; other gammas are power
     * curves; the chromaticities give the primaries.
     * <p>
     * PNG 감마와 색도 좌표: 0.45455 감마나 없는 감마(sRGB 원색)는 항등이고, 다른 감마는 거듭제곱 곡선이며, 색도 좌표가 원색을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPngGammaAndChromaticities() throws Exception {
        Check.same(ColorProfile.SRGB, ColorProfile.fromGamma(0.45455, null), "gamma 1/2.2");
        Check.same(ColorProfile.SRGB, ColorProfile.fromGamma(0.45, null), "gamma 0.45");
        compareColors(ColorProfile.fromGamma(1.0, null), SRGB_TO_XYZ, 1.0, "gamma 1.0 (linear)");
        compareColors(ColorProfile.fromGamma(1.0 / 1.8, null), SRGB_TO_XYZ, 1.8, "gamma 1.8");
        double[] p3 = { 0.3127, 0.3290, 0.680, 0.320, 0.265, 0.690, 0.150, 0.060 };
        compareColors(ColorProfile.fromGamma(0.45455, p3), P3_TO_XYZ, 2.2, "gamma 2.2 with P3 primaries");
        double[] srgb = { 0.3127, 0.3290, 0.640, 0.330, 0.300, 0.600, 0.150, 0.060 };
        Check.same(ColorProfile.SRGB, ColorProfile.fromGamma(0, srgb), "sRGB chromaticities without a gamma");
        compareColors(ColorProfile.fromGamma(0, p3), P3_TO_XYZ, -1, "P3 chromaticities without a gamma use the sRGB curve");
        Check.isNull(ColorProfile.fromGamma(0, null), "neither a gamma nor chromaticities");
        Check.isNull(ColorProfile.fromGamma(-1, null), "negative gamma");
        Check.isNull(ColorProfile.fromGamma(0.5, new double[] { 0.3127, 0.3290, 0.680, 0.0, 0.265, 0.690, 0.150, 0.060 }), "a primary with y = 0");
    }

    /**
     * The calibrated RGB of BMP: the XYZ endpoints of the sRGB primaries with the gamma 2.2 convert like a gamma 2.2 sRGB image, and
     * the endpoints of Display P3 like the reference.
     * <p>
     * BMP 의 보정된 RGB: sRGB 원색의 XYZ 끝점과 감마 2.2 는 감마 2.2 sRGB 이미지처럼 변환되고, Display P3 의 끝점은 기준과 같게 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCalibratedRgb() throws Exception {
        double[] srgb = { SRGB_TO_XYZ[0][0], SRGB_TO_XYZ[1][0], SRGB_TO_XYZ[2][0], SRGB_TO_XYZ[0][1], SRGB_TO_XYZ[1][1], SRGB_TO_XYZ[2][1],
            SRGB_TO_XYZ[0][2], SRGB_TO_XYZ[1][2], SRGB_TO_XYZ[2][2] };
        compareColors(ColorProfile.fromCalibrated(srgb, 2.2, 2.2, 2.2), SRGB_TO_XYZ, 2.2, "sRGB endpoints with gamma 2.2");
        double[] p3 = { P3_TO_XYZ[0][0], P3_TO_XYZ[1][0], P3_TO_XYZ[2][0], P3_TO_XYZ[0][1], P3_TO_XYZ[1][1], P3_TO_XYZ[2][1], P3_TO_XYZ[0][2],
            P3_TO_XYZ[1][2], P3_TO_XYZ[2][2] };
        compareColors(ColorProfile.fromCalibrated(p3, 2.2, 2.2, 2.2), P3_TO_XYZ, 2.2, "P3 endpoints with gamma 2.2");
        Check.isNull(ColorProfile.fromCalibrated(null, 2.2, 2.2, 2.2), "no endpoints");
        Check.isNull(ColorProfile.fromCalibrated(srgb, 0, 2.2, 2.2), "gamma 0");
        Check.isNull(ColorProfile.fromCalibrated(new double[9], 2.2, 2.2, 2.2), "all endpoints zero");
    }

    /**
     * Arrays and images: the alpha channel is not changed, repeated colours give the same result, an image is converted frame by frame
     * in place, and a frame object shared by two entries is converted once.
     * <p>
     * 배열과 이미지: 알파 채널은 바뀌지 않고, 반복된 색은 같은 결과를 주며, 이미지는 프레임마다 제자리에서 변환되고, 두 항목이 공유하는 프레임 배열은 한 번만
     * 변환된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testArraysAndImages() throws Exception {
        ColorProfile p = ColorProfile.fromCicp(12, 13);
        int[] px = { 0x80C86432, 0x80C86432, 0x40C86432, 0x00FFFFFF, 0xFF000000 };
        int[] single = { p.convertPixel(0xFFC86432), p.convertPixel(0xFFC86432), p.convertPixel(0xFFC86432), p.convertPixel(0xFFFFFFFF), 0xFF000000 };
        p.convert(px);
        int[] alphas = { 0x80, 0x80, 0x40, 0x00, 0xFF };
        // 알파는 그대로이고 색은 알파 없이 변환한 것과 같다.
        for (int i = 0; i < px.length; i++) {
            Check.equal(alphas[i], (px[i] >>> 24) & 0xFF, "alpha of pixel " + i);
            Check.equal(single[i] & 0xFFFFFF, px[i] & 0xFFFFFF, "colour of pixel " + i);
        }
        int[] a = { 0xFFC86432, 0xFF0A0A0A };
        int[] b = { 0xFFC86432, 0xFF0A0A0A };
        List<DecodedFrame> frames = new ArrayList<DecodedFrame>();
        frames.add(new DecodedFrame(a, 10));
        frames.add(new DecodedFrame(b, 20));
        frames.add(new DecodedFrame(a, 30));
        DecodedImage img = new DecodedImage(2, 1, frames, 0);
        DecodedImage same = p.convert(img);
        Check.same(img, same, "the same image is returned");
        Check.equal(single[0], a[0], "first frame converted");
        Check.equal(single[0], b[0], "second frame converted");
        Check.equal(p.convertPixel(0xFF0A0A0A), a[1], "shared array converted once, not twice");
        Check.equal(2, ColorProfile.SRGB.convert(img).getWidth(), "the sRGB profile returns the image");
    }
}
