package org.duckdns.hjow.swingwebpanel.media.image.color;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The colour management of the image decoders (plan item P18-03; FEATURE_MATRIX IMG-01 to IMG-09): the conversion of the pixels of an image
 * from the colour space that the file describes into sRGB, the colour space of the screen. A browser does the same once per image, after
 * decoding; the work here is also done once, over the pixel array that the decoder has just made, with 256-entry tables for the linearisation
 * of the 8-bit channels and one table for the encoding of the result, so that the cost is a few table look-ups and nine multiplications per
 * pixel.
 * <p>
 * The colour spaces that can be described are: <b>ICC profiles</b> of the matrix/TRC and gray TRC kinds ({@link #fromIcc(byte[])}), <b>PNG
 * <code>gAMA</code> and <code>cHRM</code></b> ({@link #fromGamma(double, double[])}), <b>coding-independent code points</b> of ITU-T H.273
 * (<code>cICP</code> of PNG, <code>nclx</code> of AVIF; {@link #fromCicp(int, int)}) with the primaries of Rec. 709, Rec. 470, SMPTE 170M and
 * 240M, generic film, Rec. 2020, DCI-P3, Display P3 and EBU 3213 and the transfer functions of sRGB (which Rec. 709, SMPTE 170M and Rec. 2020 use too, as in Chromium), gamma 2.2 and 2.8, linear,
 * SMPTE 240M, PQ and HLG, and the <b>calibrated RGB endpoints</b> of a BMP ({@link #fromCalibrated(double[], double, double, double)}). The
 * conversion is relative colorimetric with Bradford adaptation to the D65 white of sRGB, and components that fall outside the sRGB gamut are
 * clipped. The HDR curves are mapped to the standard dynamic range with the HDR reference white of 203 cd/m2 of ITU-R BT.2408: a PQ image has
 * its absolute luminance divided by 203 cd/m2, an HLG image goes through the system gamma 1.2 of ITU-R BT.2100 for a nominal peak of
 * 1000 cd/m2 and the same division, and everything above the white of the display is clipped. LUT-based profiles (the <code>A2B0</code>
 * family, CMYK, Lab) are not converted: they give null and the image stays as sRGB.
 * <p>
 * A profile that is equivalent to sRGB (every channel value changes by at most one level) is {@link #SRGB} and skips the conversion, so that
 * images that are sRGB in fact keep their pixels exactly.
 * <p>
 * 이미지 디코더의 색 관리(계획 P18-03; FEATURE_MATRIX IMG-01~IMG-09): 이미지의 픽셀을 파일이 기술하는 색 공간에서 화면의 색 공간인 sRGB 로 바꾼다.
 * 브라우저도 이미지마다 디코딩 뒤에 한 번 같은 일을 한다. 여기서도 디코더가 방금 만든 픽셀 배열에 대해 한 번만 하며, 8비트 채널의 선형화를 256 항목
 * 표로, 결과의 부호화를 표 하나로 하므로 픽셀당 비용은 표 조회 몇 번과 곱셈 아홉 번이다.
 * <p>
 * 기술할 수 있는 색 공간: 행렬/TRC 와 회색조 TRC 종류의 <b>ICC 프로파일</b>({@link #fromIcc(byte[])}), <b>PNG <code>gAMA</code>, <code>cHRM</code></b>
 * ({@link #fromGamma(double, double[])}), ITU-T H.273 의 <b>코딩 독립 코드 값</b>(PNG 의 <code>cICP</code>, AVIF 의 <code>nclx</code>.
 * {@link #fromCicp(int, int)}. 원색은 Rec. 709, Rec. 470, SMPTE 170M/240M, 일반 필름, Rec. 2020, DCI-P3, Display P3, EBU 3213, 전달 함수는 sRGB,
 * (Rec. 709, SMPTE 170M, Rec. 2020 도 Chromium 처럼 sRGB 곡선), 감마 2.2/2.8, 선형, SMPTE 240M, PQ, HLG), BMP 의 <b>보정된 RGB 끝점</b>({@link #fromCalibrated(double[], double, double, double)}).
 * 변환은 sRGB 의 D65 백색에 대한 Bradford 적응을 쓰는 상대 색도 측정 방식이며 sRGB 영역 밖의 성분은 자른다. HDR 곡선은 ITU-R BT.2408 의 HDR
 * 기준 백색 203 cd/m2 로 표준 동적 범위에 맞춘다: PQ 이미지는 절대 휘도를 203 cd/m2 로 나누고, HLG 이미지는 공칭 최고 1000 cd/m2 에 대한 ITU-R BT.2100
 * 의 시스템 감마 1.2 를 거친 뒤 같은 나눗셈을 하며, 화면의 흰색을 넘는 값은 자른다. 룩업 테이블 기반 프로파일(<code>A2B0</code> 계열, CMYK, Lab)은 변환하지
 * 않는다. null 을 주고 이미지는 sRGB 로 남는다.
 * <p>
 * sRGB 와 같은 프로파일(모든 채널 값이 최대 한 단계만 바뀜)은 {@link #SRGB} 이며 변환을 건너뛰므로, 실제로 sRGB 인 이미지는 픽셀이 그대로 남는다.
 */
public final class ColorProfile {

    /**
     * The sRGB colour space: no conversion.
     * <p>
     * sRGB 색 공간: 변환하지 않는다.
     */
    public static final ColorProfile SRGB = new ColorProfile();

    /**
     * The number of entries of the table that encodes linear light as an sRGB value (the index is the linear value times this number).
     * <p>
     * 선형 광을 sRGB 값으로 부호화하는 표의 항목 수(색인은 선형 값에 이 수를 곱한 것이다).
     */
    private static final int ENCODE_SIZE = 16384;

    /**
     * The encoding table: the 8-bit sRGB value for a linear value in 0 to 1, in steps of 1/16384 (IEC 61966-2-1: 12.92 times the value up to
     * 0.0031308, then 1.055 times its 1/2.4 power minus 0.055).
     * <p>
     * 부호화 표: 0~1 의 선형 값(1/16384 간격)에 대한 8비트 sRGB 값(IEC 61966-2-1: 0.0031308 까지는 12.92 배, 그 위는 1/2.4 거듭제곱의 1.055 배 빼기 0.055).
     */
    private static final byte[] ENCODE = buildEncode();

    /**
     * The gain of the HLG system gamma (ITU-R BT.2100: the display light is the scene light times 1000 cd/m2 times the luminance of the scene
     * light to the power of 0.2) divided by the HDR reference white, for the scene luminance in steps of 1/16384; the table spares a power
     * function for every pixel.
     * <p>
     * HLG 시스템 감마의 이득(ITU-R BT.2100: 화면 광은 장면 광에 1000 cd/m2 와 장면 광 휘도의 0.2 거듭제곱을 곱한 것)을 HDR 기준 백색으로 나눈 값. 장면 휘도
     * 1/16384 간격의 표이며 픽셀마다 거듭제곱 함수를 부르지 않게 해 준다.
     */
    private static final float[] HLG_GAIN = buildHlgGain();

    /**
     * The linear values of the 8-bit values of every channel: three tables for colour, one table for gray profiles, or null for sRGB.
     * <p>
     * 모든 채널의 8비트 값에 대한 선형 값: 컬러는 표 세 개, 회색조 프로파일은 표 하나, sRGB 는 null.
     */
    private final float[][] linear;

    /**
     * The matrix from the linear device values to linear sRGB, row by row (nine numbers), or null for gray profiles and sRGB.
     * <p>
     * 선형 장치 값에서 선형 sRGB 로의 행렬(행 순서로 아홉 개의 수). 회색조 프로파일과 sRGB 는 null.
     */
    private final float[] matrix;

    /**
     * True if the profile describes gray values (one channel, copied to red, green and blue).
     * <p>
     * 프로파일이 회색 값(채널 하나를 빨강, 초록, 파랑에 복사)을 기술하면 true.
     */
    private final boolean gray;

    /**
     * True for an HLG source: the scene light goes through the system gamma of ITU-R BT.2100 before the matrix.
     * <p>
     * HLG 소스이면 true: 장면 광이 행렬 앞에서 ITU-R BT.2100 의 시스템 감마를 거친다.
     */
    private final boolean hlg;

    /**
     * The luminance coefficients of the source primaries (the second row of their RGB to XYZ matrix), used by the system gamma of HLG.
     * <p>
     * 소스 원색의 휘도 계수(RGB 에서 XYZ 로 가는 행렬의 둘째 행). HLG 의 시스템 감마가 쓴다.
     */
    private final float[] luma;

    /**
     * True if the conversion leaves every pixel (nearly) unchanged and is skipped.
     * <p>
     * 변환이 모든 픽셀을 (거의) 바꾸지 않아 건너뛰면 true.
     */
    private final boolean identity;

    /**
     * Creates the sRGB profile (no tables).
     * <p>
     * sRGB 프로파일(표 없음)을 만든다.
     */
    private ColorProfile() {
        this.linear = null;
        this.matrix = null;
        this.gray = false;
        this.hlg = false;
        this.luma = null;
        this.identity = true;
    }

    /**
     * Creates a profile from transfer functions and a matrix and decides whether it is equivalent to sRGB.
     * <p>
     * 전달 함수와 행렬로 프로파일을 만들고 sRGB 와 같은지 판정한다.
     *
     * @param transfers the transfer functions of red, green and blue, or the one of a gray profile / 빨강, 초록, 파랑의 전달 함수. 회색조 프로파일은 하나
     * @param m         the matrix to linear sRGB (null for gray) / 선형 sRGB 로의 행렬(회색조는 null)
     * @param gray      true for a gray profile / 회색조 프로파일이면 true
     * @param hlg       true for HLG / HLG 이면 true
     * @param luma      the luminance coefficients for HLG or null / HLG 의 휘도 계수 또는 null
     * @param plain     unused marker that keeps this signature apart from the sRGB constructor / sRGB 생성자와 구별하는 쓰이지 않는 표시
     */
    private ColorProfile(Transfer[] transfers, double[][] m, boolean gray, boolean hlg, double[] luma, boolean plain) {
        this.linear = new float[transfers.length][256];
        // 채널마다 8비트 값 256 개의 선형 값을 미리 계산한다.
        for (int c = 0; c < transfers.length; c++) {
            for (int i = 0; i < 256; i++) {
                this.linear[c][i] = (float) transfers[c].toLinear(i / 255.0);
            }
        }
        this.gray = gray;
        this.hlg = hlg;
        if (m == null) {
            this.matrix = null;
        } else {
            this.matrix = new float[9];
            // 행렬을 행 순서로 펼친다.
            for (int r = 0; r < 3; r++) {
                for (int k = 0; k < 3; k++) {
                    this.matrix[r * 3 + k] = (float) m[r][k];
                }
            }
        }
        this.luma = luma == null ? null : new float[] { (float) luma[0], (float) luma[1], (float) luma[2] };
        this.identity = !hlg && looksLikeSrgb(this);
    }

    /**
     * Creates a profile (for the classes of this package).
     * <p>
     * 프로파일을 만든다(이 패키지의 클래스용).
     *
     * @param transfers the transfer functions of red, green and blue, or the one of a gray profile / 빨강, 초록, 파랑의 전달 함수. 회색조 프로파일은 하나
     * @param m         the matrix to linear sRGB (null for gray) / 선형 sRGB 로의 행렬(회색조는 null)
     * @param gray      true for a gray profile / 회색조 프로파일이면 true
     * @param hlg       true for HLG / HLG 이면 true
     * @param luma      the luminance coefficients for HLG or null / HLG 의 휘도 계수 또는 null
     * @return the profile, or {@link #SRGB} if the conversion would change nothing / 프로파일. 변환이 아무것도 바꾸지 않으면 {@link #SRGB}
     */
    static ColorProfile create(Transfer[] transfers, double[][] m, boolean gray, boolean hlg, double[] luma) {
        ColorProfile p = new ColorProfile(transfers, m, gray, hlg, luma, true);
        return p.identity ? SRGB : p;
    }

    /**
     * Tells whether a profile changes no value of an 8-bit channel by more than one level, on the three channel ramps and on the gray ramp.
     * <p>
     * 프로파일이 세 채널의 경사와 회색 경사에서 8비트 채널 값을 한 단계보다 많이 바꾸지 않는지 알려 준다.
     *
     * @param p the profile to test (its tables are already built) / 시험할 프로파일(표는 이미 만들어져 있다)
     * @return true if the profile is equivalent to sRGB / 프로파일이 sRGB 와 같으면 true
     */
    private static boolean looksLikeSrgb(ColorProfile p) {
        // 값 0~255 마다 한 채널만 있는 색과 회색을 변환해 입력과 같은지 본다.
        for (int v = 0; v < 256; v++) {
            int[] probes = { v << 16, v << 8, v, (v << 16) | (v << 8) | v };
            // 빨강, 초록, 파랑 경사와 회색 경사를 차례로 시험한다.
            for (int i = 0; i < probes.length; i++) {
                int in = 0xFF000000 | probes[i];
                int out = p.convertPixel(in);
                if (Math.abs(((in >> 16) & 0xFF) - ((out >> 16) & 0xFF)) > 1 || Math.abs(((in >> 8) & 0xFF) - ((out >> 8) & 0xFF)) > 1
                        || Math.abs((in & 0xFF) - (out & 0xFF)) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Builds the table of the HLG system gamma gain.
     * <p>
     * HLG 시스템 감마 이득의 표를 만든다.
     *
     * @return the table / 표
     */
    private static float[] buildHlgGain() {
        float[] t = new float[ENCODE_SIZE + 1];
        // 장면 휘도마다 이득(휘도 ^ 0.2 * 1000 / 203)을 계산한다.
        for (int i = 0; i <= ENCODE_SIZE; i++) {
            t[i] = (float) (Math.pow(i / (double) ENCODE_SIZE, 0.2) * 1000.0 / Transfer.HDR_REFERENCE_WHITE);
        }
        return t;
    }

    /**
     * Builds the table that encodes linear light as an 8-bit sRGB value.
     * <p>
     * 선형 광을 8비트 sRGB 값으로 부호화하는 표를 만든다.
     *
     * @return the table / 표
     */
    private static byte[] buildEncode() {
        byte[] t = new byte[ENCODE_SIZE + 1];
        // 선형 값마다 sRGB 값을 구해 가장 가까운 8비트 정수로 반올림한다.
        for (int i = 0; i <= ENCODE_SIZE; i++) {
            double l = i / (double) ENCODE_SIZE;
            double v = l <= 0.0031308 ? 12.92 * l : 1.055 * Math.pow(l, 1.0 / 2.4) - 0.055;
            t[i] = (byte) (int) Math.round(Math.max(0.0, Math.min(1.0, v)) * 255.0);
        }
        return t;
    }

    /**
     * Tells whether this profile leaves the pixels unchanged (so {@link #convert(int[])} does nothing).
     * <p>
     * 이 프로파일이 픽셀을 바꾸지 않는지(그래서 {@link #convert(int[])} 가 아무것도 하지 않는지) 알려 준다.
     *
     * @return true for sRGB and the profiles that are equivalent to it / sRGB 와 그와 같은 프로파일이면 true
     */
    public boolean isIdentity() {
        return identity;
    }

    /**
     * Tells whether the profile describes gray values (a gray TRC profile): it fits a gray image, and a colour image has to ignore it.
     * <p>
     * 프로파일이 회색 값을 기술하는지(회색조 TRC 프로파일) 알려 준다. 회색 이미지에 맞고 컬러 이미지는 무시해야 한다.
     *
     * @return true for a gray profile / 회색조 프로파일이면 true
     */
    public boolean isGray() {
        return gray;
    }

    /**
     * Returns the profile to use for an image: this profile if its kind fits the image (a gray profile only fits a gray image), else
     * {@link #SRGB}. An image whose file describes the wrong kind of profile is shown as sRGB.
     * <p>
     * 이미지에 쓸 프로파일을 반환한다: 종류가 이미지에 맞으면 이 프로파일(회색조 프로파일은 회색 이미지에만 맞다), 아니면 {@link #SRGB}. 파일이 잘못된
     * 종류의 프로파일을 기술하는 이미지는 sRGB 로 보인다.
     *
     * @param grayImage true if the image has gray samples only (one colour channel) / 이미지가 회색 샘플만(색 채널 하나) 가지면 true
     * @return this profile or {@link #SRGB} / 이 프로파일 또는 {@link #SRGB}
     */
    public ColorProfile forImage(boolean grayImage) {
        return gray && !grayImage ? SRGB : this;
    }

    /**
     * Converts one non-premultiplied ARGB pixel to sRGB; the alpha is kept.
     * <p>
     * 알파를 미리 곱하지 않은 ARGB 픽셀 하나를 sRGB 로 바꾼다. 알파는 유지한다.
     *
     * @param argb the pixel <code>0xAARRGGBB</code> / 픽셀 <code>0xAARRGGBB</code>
     * @return the converted pixel / 변환된 픽셀
     */
    public int convertPixel(int argb) {
        if (linear == null) {
            return argb;
        }
        int a = argb & 0xFF000000;
        float r;
        float g;
        float b;
        if (gray) {
            // 회색조 프로파일은 선형 회색 값을 세 성분에 똑같이 쓴다.
            r = linear[0][(argb >> 16) & 0xFF];
            g = r;
            b = r;
        } else {
            float e0 = linear[0][(argb >> 16) & 0xFF];
            float e1 = linear[1][(argb >> 8) & 0xFF];
            float e2 = linear[2][argb & 0xFF];
            if (hlg) {
                // HLG: 장면 광에 시스템 감마(휘도 ^ 0.2)와 최고 1000 cd/m2 를 곱한 뒤 기준 백색 203 cd/m2 로 나눈다.
                float ys = luma[0] * e0 + luma[1] * e1 + luma[2] * e2;
                float gain = ys <= 0 ? 0f : HLG_GAIN[ys >= 1f ? ENCODE_SIZE : (int) (ys * ENCODE_SIZE + 0.5f)];
                e0 *= gain;
                e1 *= gain;
                e2 *= gain;
            }
            r = matrix[0] * e0 + matrix[1] * e1 + matrix[2] * e2;
            g = matrix[3] * e0 + matrix[4] * e1 + matrix[5] * e2;
            b = matrix[6] * e0 + matrix[7] * e1 + matrix[8] * e2;
        }
        return a | (encode(r) << 16) | (encode(g) << 8) | encode(b);
    }

    /**
     * Encodes a linear value as an 8-bit sRGB value, clipping what lies outside 0 to 1.
     * <p>
     * 선형 값을 8비트 sRGB 값으로 부호화하며 0~1 밖의 값은 자른다.
     *
     * @param v linear value / 선형 값
     * @return value 0 to 255 / 값 0~255
     */
    private static int encode(float v) {
        if (!(v > 0f)) {
            return 0;
        }
        if (v >= 1f) {
            return 255;
        }
        return ENCODE[(int) (v * ENCODE_SIZE + 0.5f)] & 0xFF;
    }

    /**
     * Converts the pixels of an array to sRGB in place. The pixels are non-premultiplied ARGB; the alpha is not changed. Nothing happens for
     * {@link #SRGB}.
     * <p>
     * 배열의 픽셀을 제자리에서 sRGB 로 바꾼다. 픽셀은 알파를 미리 곱하지 않은 ARGB 이며 알파는 바꾸지 않는다. {@link #SRGB} 는 아무 일도 없다.
     *
     * @param argb the pixels / 픽셀
     */
    public void convert(int[] argb) {
        if (identity) {
            return;
        }
        // 픽셀을 하나씩 변환한다. 같은 색이 이어지면 앞의 결과를 다시 쓴다.
        int lastIn = 0;
        int lastOut = 0;
        boolean have = false;
        // 픽셀 배열의 모든 위치를 하나씩 변환한다.
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            if (have && ((p ^ lastIn) & 0x00FFFFFF) == 0) {
                argb[i] = (p & 0xFF000000) | (lastOut & 0x00FFFFFF);
                continue;
            }
            int out = convertPixel(p);
            lastIn = p;
            lastOut = out;
            have = true;
            argb[i] = out;
        }
    }

    /**
     * Converts all frames of a freshly decoded image to sRGB in place (the pixel arrays of the frames are changed) and returns the image.
     * Only decoders call this, right after they made the image and before anybody else has seen it.
     * <p>
     * 방금 디코딩한 이미지의 모든 프레임을 제자리에서 sRGB 로 바꾸고(프레임의 픽셀 배열이 바뀐다) 이미지를 반환한다. 디코더만 이미지를 만든 직후,
     * 다른 누군가가 보기 전에 부른다.
     *
     * @param image the image / 이미지
     * @return the same image / 같은 이미지
     */
    public DecodedImage convert(DecodedImage image) {
        if (identity) {
            return image;
        }
        // 프레임마다 픽셀을 바꾼다(같은 배열을 공유하는 프레임이 있어도 한 번씩만 바꾼다).
        List<int[]> done = new ArrayList<int[]>();
        for (DecodedFrame f : image.getFrames()) {
            boolean seen = false;
            // 이미 바꾼 배열인지 목록에서 찾는다.
            for (int[] d : done) {
                if (d == f.getArgb()) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                convert(f.getArgb());
                done.add(f.getArgb());
            }
        }
        return image;
    }

    /**
     * Builds the conversion of an ICC profile (see {@link IccProfile} for what is understood).
     * <p>
     * ICC 프로파일의 변환을 만든다(이해하는 범위는 {@link IccProfile} 참조).
     *
     * @param icc the bytes of the profile / 프로파일의 바이트
     * @return the conversion, {@link #SRGB} if the profile is equivalent to sRGB, or null if the profile is damaged or of a kind that is not supported / 변환. 프로파일이 sRGB 와 같으면 {@link #SRGB}. 손상되었거나 지원하지 않는 종류이면 null
     */
    public static ColorProfile fromIcc(byte[] icc) {
        return IccProfile.parse(icc);
    }

    /**
     * Builds the conversion for the <code>gAMA</code> and <code>cHRM</code> chunks of a PNG file: the samples are the intensity raised to the
     * file gamma, so the linear value is the sample to the power of 1/gamma; the chromaticities give the primaries and the white point (sRGB
     * and D65 if the file has no <code>cHRM</code> chunk). A gamma within 0.1 of 1/2.2 on sRGB primaries counts as sRGB.
     * <p>
     * PNG 파일의 <code>gAMA</code> 와 <code>cHRM</code> 청크의 변환을 만든다: 샘플은 강도의 파일 감마 거듭제곱이므로 선형 값은 샘플의 1/감마 거듭제곱이다.
     * 색도 좌표는 원색과 백색점을 준다(<code>cHRM</code> 청크가 없으면 sRGB 와 D65). sRGB 원색에서 1/2.2 와 0.1 이내인 감마는 sRGB 로 센다.
     *
     * @param fileGamma the file gamma, the value of <code>gAMA</code> divided by 100000 (for example 0.45455), or 0 if there is no gamma / 파일 감마, <code>gAMA</code> 의 값을 100000 으로 나눈 것(예: 0.45455). 감마가 없으면 0
     * @param chrm      {white x, white y, red x, red y, green x, green y, blue x, blue y} or null / {백색 x, 백색 y, 빨강 x, 빨강 y, 초록 x, 초록 y, 파랑 x, 파랑 y}. 없으면 null
     * @return the conversion, {@link #SRGB} if it is equivalent to sRGB, or null if the values are unusable / 변환. sRGB 와 같으면 {@link #SRGB}. 값을 쓸 수 없으면 null
     */
    public static ColorProfile fromGamma(double fileGamma, double[] chrm) {
        if (fileGamma < 0 || Double.isNaN(fileGamma) || (fileGamma == 0 && chrm == null)) {
            return null;
        }
        double[][] primaries = ColorMath.SRGB_PRIMARIES;
        double[] white = ColorMath.D65_XY;
        if (chrm != null) {
            if (chrm.length < 8) {
                return null;
            }
            white = new double[] { chrm[0], chrm[1] };
            primaries = new double[][] { { chrm[2], chrm[3] }, { chrm[4], chrm[5] }, { chrm[6], chrm[7] } };
        }
        // 감마가 2.2 근처이고 원색이 sRGB 의 것이면 이 파일은 sRGB 이다(많은 도구가 sRGB 이미지에 gAMA 45455 를 붙인다).
        boolean nearSrgbGamma = fileGamma > 0 && Math.abs(1.0 / fileGamma - 2.2) <= 0.1;
        if ((fileGamma == 0 || nearSrgbGamma) && isSrgbPrimaries(primaries, white)) {
            return SRGB;
        }
        // 감마가 없는 파일(cHRM 만 있음)은 sRGB 곡선을 쓴다. 있으면 그 감마의 거듭제곱 곡선이다.
        Transfer t = fileGamma == 0 ? Transfer.SRGB : Transfer.gamma(1.0 / fileGamma);
        double[][] m = ColorMath.toSrgb(primaries, white);
        if (m == null) {
            return null;
        }
        return create(new Transfer[] { t, t, t }, m, false, false, null);
    }

    /**
     * Tells whether primaries and a white point are those of sRGB (each chromaticity within 0.01).
     * <p>
     * 원색과 백색점이 sRGB 의 것인지(색도 좌표마다 0.01 이내) 알려 준다.
     *
     * @param primaries the chromaticities of red, green and blue / 빨강, 초록, 파랑의 색도 좌표
     * @param white     the chromaticity of the white point / 백색점의 색도 좌표
     * @return true if they are the sRGB values / sRGB 의 값이면 true
     */
    private static boolean isSrgbPrimaries(double[][] primaries, double[] white) {
        // 백색점과 원색 셋을 sRGB 의 값과 하나씩 비교한다.
        if (Math.abs(white[0] - ColorMath.D65_XY[0]) > 0.01 || Math.abs(white[1] - ColorMath.D65_XY[1]) > 0.01) {
            return false;
        }
        // 프로파일 안의 세 색소 행마다 같은 일을 한다.
        for (int k = 0; k < 3; k++) {
            if (Math.abs(primaries[k][0] - ColorMath.SRGB_PRIMARIES[k][0]) > 0.01 || Math.abs(primaries[k][1] - ColorMath.SRGB_PRIMARIES[k][1]) > 0.01) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the conversion for coding-independent code points of ITU-T H.273 (the <code>cICP</code> chunk of PNG, the <code>nclx</code>
     * colour description of AVIF). Unspecified or unknown primaries are taken as those of sRGB and an unspecified transfer function as the
     * sRGB curve, as browsers do; the logarithmic transfer functions and SMPTE ST 428 are not supported.
     * <p>
     * ITU-T H.273 의 코딩 독립 코드 값의 변환을 만든다(PNG 의 <code>cICP</code> 청크, AVIF 의 <code>nclx</code> 색 설명). 지정되지 않았거나 모르는
     * 원색은 브라우저처럼 sRGB 의 것으로, 지정되지 않은 전달 함수는 sRGB 곡선으로 본다. 로그 전달 함수와 SMPTE ST 428 은 지원하지 않는다.
     *
     * @param primaries the colour primaries code point (1 Rec. 709, 4 Rec. 470 M, 5 Rec. 470 BG, 6 SMPTE 170M, 7 SMPTE 240M, 8 generic film, 9 Rec. 2020, 11 DCI-P3, 12 Display P3, 22 EBU 3213) / 색 원색 코드 값
     * @param transfer  the transfer characteristics code point (1, 6, 11, 12, 13, 14, 15 the sRGB curve, 4 gamma 2.2, 5 gamma 2.8, 7 SMPTE 240M, 8 linear, 16 PQ, 18 HLG) / 전달 특성 코드 값
     * @return the conversion, {@link #SRGB} if it is equivalent to sRGB, or null if a code point is not supported / 변환. sRGB 와 같으면 {@link #SRGB}. 지원하지 않는 코드 값이면 null
     */
    public static ColorProfile fromCicp(int primaries, int transfer) {
        Transfer t = cicpTransfer(transfer);
        double[][] prim = cicpPrimaries(primaries);
        if (t == null || prim == null) {
            return null;
        }
        // prim 은 {빨강 x, y, 초록 x, y, 파랑 x, y, 백색 x, y} 순서의 한 줄이다.
        double[][] p = { { prim[0][0], prim[0][1] }, { prim[0][2], prim[0][3] }, { prim[0][4], prim[0][5] } };
        double[] white = { prim[0][6], prim[0][7] };
        double[][] m = ColorMath.toSrgb(p, white);
        if (m == null) {
            return null;
        }
        boolean hlg = transfer == 18;
        double[] luma = null;
        if (hlg) {
            // HLG 의 시스템 감마는 소스 원색의 휘도(RGB 에서 XYZ 로 가는 행렬의 Y 행)로 계산한다.
            luma = ColorMath.primariesToXyz(p, white)[1];
        }
        return create(new Transfer[] { t, t, t }, m, false, hlg, luma);
    }

    /**
     * Returns the transfer function of a code point of ITU-T H.273.
     * <p>
     * ITU-T H.273 코드 값의 전달 함수를 반환한다.
     *
     * @param code the code point / 코드 값
     * @return the function, or null if it is not supported / 함수. 지원하지 않으면 null
     */
    private static Transfer cicpTransfer(int code) {
        switch (code) {
        case 4:
            return Transfer.gamma(2.2);
        case 5:
            return Transfer.gamma(2.8);
        case 7:
            return Transfer.SMPTE240M;
        case 8:
            return Transfer.LINEAR;
        case 16:
            return Transfer.PQ;
        case 18:
            return Transfer.HLG_SCENE;
        case 9:
        case 10:
        case 17:
            return null;
        default:
            // sRGB(13)와 같은 곡선을 쓰는 값: Rec. 709(1), SMPTE 170M(6), Rec. 2020(14, 15)은 Chromium 처럼 sRGB 곡선을 쓰고
            // (Chromium 은 화면에서 이 값들을 sRGB 와 같게 다룬다), IEC 61966-2-4(11)와 BT.1361(12)도 가장 가까운 sRGB 곡선으로 본다.
            // 0, 2, 3 과 모르는 값은 지정되지 않은 것으로 sRGB 곡선을 쓴다.
            return Transfer.SRGB;
        }
    }

    /**
     * Returns the primaries and white point of a code point of ITU-T H.273 (Table 2), as one row {red x, y, green x, y, blue x, y, white x, y}.
     * <p>
     * ITU-T H.273 코드 값(표 2)의 원색과 백색점을 한 줄 {빨강 x, y, 초록 x, y, 파랑 x, y, 백색 x, y} 로 반환한다.
     *
     * @param code the code point / 코드 값
     * @return a one-row array, never null for the known and unspecified values / 한 줄짜리 배열. 아는 값과 지정되지 않은 값에는 null 이 아니다
     */
    private static double[][] cicpPrimaries(int code) {
        double[] d65 = ColorMath.D65_XY;
        double[] c = { 0.310, 0.316 };
        switch (code) {
        case 4:
            return new double[][] { { 0.67, 0.33, 0.21, 0.71, 0.14, 0.08, c[0], c[1] } };
        case 5:
            return new double[][] { { 0.64, 0.33, 0.29, 0.60, 0.15, 0.06, d65[0], d65[1] } };
        case 6:
        case 7:
            return new double[][] { { 0.630, 0.340, 0.310, 0.595, 0.155, 0.070, d65[0], d65[1] } };
        case 8:
            return new double[][] { { 0.681, 0.319, 0.243, 0.692, 0.145, 0.049, c[0], c[1] } };
        case 9:
            return new double[][] { { 0.708, 0.292, 0.170, 0.797, 0.131, 0.046, d65[0], d65[1] } };
        case 10:
            return null;
        case 11:
            return new double[][] { { 0.680, 0.320, 0.265, 0.690, 0.150, 0.060, 0.314, 0.351 } };
        case 12:
            return new double[][] { { 0.680, 0.320, 0.265, 0.690, 0.150, 0.060, d65[0], d65[1] } };
        case 22:
            return new double[][] { { 0.630, 0.340, 0.295, 0.605, 0.155, 0.077, d65[0], d65[1] } };
        default:
            // 1(Rec. 709/sRGB), 지정되지 않음(0, 2), 예약된 값은 sRGB 의 원색이다.
            return new double[][] { { 0.640, 0.330, 0.300, 0.600, 0.150, 0.060, d65[0], d65[1] } };
        }
    }

    /**
     * Builds the conversion for the calibrated RGB colour space of a BMP file (<code>LCS_CALIBRATED_RGB</code>): the CIE XYZ endpoints of
     * the three primaries and a gamma for every channel.
     * <p>
     * BMP 파일의 보정된 RGB 색 공간(<code>LCS_CALIBRATED_RGB</code>)의 변환을 만든다: 세 원색의 CIE XYZ 끝점과 채널별 감마.
     *
     * @param endpoints the XYZ of the red, green and blue endpoints {Xr, Yr, Zr, Xg, Yg, Zg, Xb, Yb, Zb}; their sum is the white point / 빨강, 초록, 파랑 끝점의 XYZ {Xr, Yr, Zr, Xg, Yg, Zg, Xb, Yb, Zb}. 합이 백색점이다
     * @param gammaR    the gamma of red (the sample to this power is the linear value) / 빨강의 감마(샘플의 이 거듭제곱이 선형 값)
     * @param gammaG    the gamma of green / 초록의 감마
     * @param gammaB    the gamma of blue / 파랑의 감마
     * @return the conversion, {@link #SRGB} if it is equivalent to sRGB, or null if the values are unusable / 변환. sRGB 와 같으면 {@link #SRGB}. 값을 쓸 수 없으면 null
     */
    public static ColorProfile fromCalibrated(double[] endpoints, double gammaR, double gammaG, double gammaB) {
        if (endpoints == null || endpoints.length < 9 || !(gammaR > 0) || !(gammaG > 0) || !(gammaB > 0)) {
            return null;
        }
        double[][] toXyz = { { endpoints[0], endpoints[3], endpoints[6] }, { endpoints[1], endpoints[4], endpoints[7] },
            { endpoints[2], endpoints[5], endpoints[8] } };
        double[] white = { endpoints[0] + endpoints[3] + endpoints[6], endpoints[1] + endpoints[4] + endpoints[7],
            endpoints[2] + endpoints[5] + endpoints[8] };
        if (!(white[0] > 0) || !(white[1] > 0) || !(white[2] > 0)) {
            return null;
        }
        // 백색점을 Y = 1 로 맞춘 뒤 D65 로 적응시켜 선형 sRGB 로 바꾸는 행렬을 만든다.
        double[] w1 = { white[0] / white[1], 1.0, white[2] / white[1] };
        double[][] scaled = new double[3][3];
        // 3 x 3 행렬의 행마다 값을 나눠 쓴다.
        for (int r = 0; r < 3; r++) {
            // 행 안의 열마다 값을 옮긴다.
            for (int k = 0; k < 3; k++) {
                scaled[r][k] = toXyz[r][k] / white[1];
            }
        }
        double[][] m = ColorMath.multiply(ColorMath.XYZ_TO_SRGB, ColorMath.multiply(ColorMath.bradford(w1, ColorMath.D65_XYZ), scaled));
        return create(new Transfer[] { Transfer.gamma(gammaR), Transfer.gamma(gammaG), Transfer.gamma(gammaB) }, m, false, false, null);
    }
}
