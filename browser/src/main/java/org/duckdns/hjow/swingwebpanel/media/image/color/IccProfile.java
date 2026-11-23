package org.duckdns.hjow.swingwebpanel.media.image.color;

/**
 * The reader of ICC colour profiles (ICC.1:2010-12, version 4.3, which also covers the version 2 profiles of ICC.1:2001-04 in the parts used
 * here). It understands the two kinds of display profiles that are described by tags alone: <b>matrix/TRC</b> profiles of the colour space
 * <code>RGB </code> (the colorant tags <code>rXYZ gXYZ bXYZ</code> and the curves <code>rTRC gTRC bTRC</code>, ICC.1:2010 section 8.3.4:
 * linearise every channel with its curve, then multiply with the colorant matrix to get XYZ of the D50 profile connection space) and
 * <b>gray TRC</b> profiles of the colour space <code>GRAY</code> (one curve <code>kTRC</code>, section 8.3.3). The curves are the
 * <code>curv</code> type (identity, a u8Fixed8 gamma or a table) and the <code>para</code> type (five parametric functions). Profiles that need
 * look-up tables (the <code>A2B0</code> family, CMYK and Lab profiles) and the other data colour spaces are not converted: {@link #parse(byte[])}
 * gives null for them and the caller keeps treating the samples as sRGB. The white point tag <code>wtpt</code> and the adaptation matrix
 * <code>chad</code> are not used: the colorants of a matrix profile are already adapted to D50 by definition, and the other profile data would
 * only repeat that adaptation (the choice that skcms, the profile reader of Chromium, makes too).
 * <p>
 * ICC 색 프로파일 읽기(ICC.1:2010-12, 버전 4.3. 여기서 쓰는 부분은 ICC.1:2001-04 의 버전 2 프로파일에도 적용된다). 태그만으로 기술되는 두 가지 화면
 * 프로파일을 이해한다: 색 공간 <code>RGB </code> 의 <b>행렬/TRC</b> 프로파일(색소 태그 <code>rXYZ gXYZ bXYZ</code> 와 곡선
 * <code>rTRC gTRC bTRC</code>, ICC.1:2010 8.3.4절: 채널마다 곡선으로 선형화한 뒤 색소 행렬을 곱해 D50 프로파일 연결 공간의 XYZ 를 얻는다)과 색 공간
 * <code>GRAY</code> 의 <b>회색조 TRC</b> 프로파일(곡선 하나 <code>kTRC</code>, 8.3.3절). 곡선은 <code>curv</code> 형식(항등, u8Fixed8 감마, 표)과
 * <code>para</code> 형식(매개변수 함수 다섯 가지)이다. 룩업 테이블이 필요한 프로파일(<code>A2B0</code> 계열, CMYK 와 Lab 프로파일)과 다른 데이터 색
 * 공간은 변환하지 않는다. {@link #parse(byte[])} 가 null 을 주면 호출자는 계속 샘플을 sRGB 로 본다. 백색점 태그 <code>wtpt</code> 와 적응 행렬
 * <code>chad</code> 는 쓰지 않는다. 행렬 프로파일의 색소는 정의상 이미 D50 으로 적응되어 있고 다른 프로파일 데이터는 그 적응을 되풀이할 뿐이다
 * (Chromium 의 프로파일 읽기 skcms 도 같은 선택을 한다).
 */
final class IccProfile {

    /**
     * The size of the profile header.
     * <p>
     * 프로파일 헤더의 크기.
     */
    private static final int HEADER = 128;

    /**
     * The largest number of table entries that a <code>curv</code> tag may have (the 16-bit tables of real profiles have 256 to 4096 entries).
     * <p>
     * <code>curv</code> 태그가 가질 수 있는 표 항목의 최대 수(실제 프로파일의 16비트 표는 256~4096 개이다).
     */
    private static final int MAX_CURVE_ENTRIES = 65536;

    /**
     * The profile bytes.
     * <p>
     * 프로파일 바이트.
     */
    private final byte[] d;

    /**
     * The number of tags.
     * <p>
     * 태그 수.
     */
    private final int tagCount;

    /**
     * Creates a reader over validated profile data.
     * <p>
     * 검증된 프로파일 데이터에 대한 읽기를 만든다.
     *
     * @param d        profile bytes / 프로파일 바이트
     * @param tagCount number of tags in the tag table / 태그 표의 태그 수
     */
    private IccProfile(byte[] d, int tagCount) {
        this.d = d;
        this.tagCount = tagCount;
    }

    /**
     * Reads a profile and builds the conversion to sRGB. It never throws: damaged data and every kind of profile that is not supported give null.
     * <p>
     * 프로파일을 읽어 sRGB 로의 변환을 만든다. 예외를 던지지 않는다. 손상된 데이터와 지원하지 않는 모든 종류의 프로파일은 null 을 준다.
     *
     * @param data the bytes of the profile / 프로파일의 바이트
     * @return the conversion (the sRGB profile when the profile is equivalent to sRGB), or null if the profile cannot be used / 변환(프로파일이 sRGB 와 같으면 sRGB 프로파일). 쓸 수 없으면 null
     */
    static ColorProfile parse(byte[] data) {
        try {
            return parseUnchecked(data);
        } catch (RuntimeException e) {
            // 어긋난 오프셋 등 어떤 문제도 프로파일을 쓰지 못하는 것으로 처리한다(이미지 자체는 sRGB 로 보인다).
            return null;
        }
    }

    /**
     * Does the work of {@link #parse(byte[])}; unexpected data may throw runtime exceptions.
     * <p>
     * {@link #parse(byte[])} 의 실제 작업을 한다. 예상 밖의 데이터는 런타임 예외를 던질 수 있다.
     *
     * @param data the bytes of the profile / 프로파일의 바이트
     * @return the conversion or null / 변환 또는 null
     */
    private static ColorProfile parseUnchecked(byte[] data) {
        if (data == null || data.length < HEADER + 4) {
            return null;
        }
        if (data[36] != 'a' || data[37] != 'c' || data[38] != 's' || data[39] != 'p') {
            return null;
        }
        String cls = ascii(data, 12, 4);
        String space = ascii(data, 16, 4);
        String pcs = ascii(data, 20, 4);
        // 장치 연결, 추상, 이름 있는 색 프로파일과 XYZ 가 아닌 연결 공간은 지원하지 않는다.
        if (!"XYZ ".equals(pcs) || "link".equals(cls) || "abst".equals(cls) || "nmcl".equals(cls)) {
            return null;
        }
        long count = u32(data, HEADER);
        if (count < 1 || count > 1000 || HEADER + 4 + 12 * count > data.length) {
            return null;
        }
        IccProfile p = new IccProfile(data, (int) count);
        if ("RGB ".equals(space)) {
            return p.matrixProfile();
        }
        if ("GRAY".equals(space)) {
            return p.grayProfile();
        }
        return null;
    }

    /**
     * Builds the conversion of a matrix/TRC RGB profile.
     * <p>
     * 행렬/TRC RGB 프로파일의 변환을 만든다.
     *
     * @return the conversion, or null if a tag is missing or damaged / 변환. 태그가 없거나 손상되었으면 null
     */
    private ColorProfile matrixProfile() {
        double[] r = xyz(find("rXYZ"));
        double[] g = xyz(find("gXYZ"));
        double[] b = xyz(find("bXYZ"));
        Transfer tr = curve(find("rTRC"));
        Transfer tg = curve(find("gTRC"));
        Transfer tb = curve(find("bTRC"));
        if (r == null || g == null || b == null || tr == null || tg == null || tb == null) {
            return null;
        }
        // 색소의 XYZ 를 열로 한 행렬이 장치 선형 RGB 를 D50 연결 공간의 XYZ 로 바꾼다.
        double[][] toXyz = { { r[0], g[0], b[0] }, { r[1], g[1], b[1] }, { r[2], g[2], b[2] } };
        double[][] adapt = ColorMath.bradford(ColorMath.D50_ICC_XYZ, ColorMath.D65_XYZ);
        double[][] m = ColorMath.multiply(ColorMath.XYZ_TO_SRGB, ColorMath.multiply(adapt, toXyz));
        return ColorProfile.create(new Transfer[] { tr, tg, tb }, m, false, false, null);
    }

    /**
     * Builds the conversion of a gray TRC profile.
     * <p>
     * 회색조 TRC 프로파일의 변환을 만든다.
     *
     * @return the conversion, or null if the tag is missing or damaged / 변환. 태그가 없거나 손상되었으면 null
     */
    private ColorProfile grayProfile() {
        Transfer k = curve(find("kTRC"));
        if (k == null) {
            return null;
        }
        return ColorProfile.create(new Transfer[] { k }, null, true, false, null);
    }

    /**
     * Finds a tag in the tag table.
     * <p>
     * 태그 표에서 태그를 찾는다.
     *
     * @param sig the four-character signature / 네 글자 시그니처
     * @return {offset, size} of the tag data, or null if there is no such tag or it does not lie inside the profile / 태그 데이터의 {오프셋, 크기}. 없거나 프로파일 밖이면 null
     */
    private int[] find(String sig) {
        // 태그 표의 항목(시그니처, 오프셋, 크기 각 4바이트)을 차례로 본다.
        for (int i = 0; i < tagCount; i++) {
            int e = HEADER + 4 + 12 * i;
            if (sig.equals(ascii(d, e, 4))) {
                long off = u32(d, e + 4);
                long size = u32(d, e + 8);
                if (off < HEADER || size < 8 || off + size > d.length) {
                    return null;
                }
                return new int[] { (int) off, (int) size };
            }
        }
        return null;
    }

    /**
     * Reads an <code>XYZ </code> tag with one XYZ value (ICC.1:2010 section 10.31).
     * <p>
     * XYZ 값 하나가 든 <code>XYZ </code> 태그를 읽는다(ICC.1:2010 10.31절).
     *
     * @param tag {offset, size} or null / {오프셋, 크기}. 또는 null
     * @return {X, Y, Z}, or null if it is not such a tag / {X, Y, Z}. 그런 태그가 아니면 null
     */
    private double[] xyz(int[] tag) {
        if (tag == null || tag[1] < 20 || !"XYZ ".equals(ascii(d, tag[0], 4))) {
            return null;
        }
        return new double[] { s15(tag[0] + 8), s15(tag[0] + 12), s15(tag[0] + 16) };
    }

    /**
     * Reads a curve tag: <code>curv</code> (ICC.1:2010 section 10.6) or <code>para</code> (section 10.18).
     * <p>
     * 곡선 태그를 읽는다: <code>curv</code>(ICC.1:2010 10.6절) 또는 <code>para</code>(10.18절).
     *
     * @param tag {offset, size} or null / {오프셋, 크기}. 또는 null
     * @return the transfer function, or null if the tag is missing or damaged / 전달 함수. 태그가 없거나 손상되었으면 null
     */
    private Transfer curve(int[] tag) {
        if (tag == null || tag[1] < 12) {
            return null;
        }
        int off = tag[0];
        String type = ascii(d, off, 4);
        if ("curv".equals(type)) {
            long n = u32(d, off + 8);
            if (n > MAX_CURVE_ENTRIES || 12 + 2 * n > tag[1]) {
                return null;
            }
            if (n == 0) {
                return Transfer.LINEAR;
            }
            if (n == 1) {
                // 항목 하나는 u8Fixed8 형식의 감마 값이다.
                return Transfer.gamma(u16(d, off + 12) / 256.0);
            }
            double[] samples = new double[(int) n];
            // 16비트 표 값을 0~1 로 바꾼다.
            for (int i = 0; i < n; i++) {
                samples[i] = u16(d, off + 12 + 2 * i) / 65535.0;
            }
            return Transfer.table(samples);
        }
        if ("para".equals(type)) {
            int fn = u16(d, off + 8);
            int[] counts = { 1, 3, 4, 5, 7 };
            if (fn >= counts.length || 12 + 4 * counts[fn] > tag[1]) {
                return null;
            }
            double[] params = new double[counts[fn]];
            // 매개변수는 s15Fixed16 형식이다.
            for (int i = 0; i < params.length; i++) {
                params[i] = s15(off + 12 + 4 * i);
            }
            return Transfer.parametric(fn, params);
        }
        return null;
    }

    /**
     * Reads a signed 15.16 fixed point number.
     * <p>
     * 부호 있는 15.16 고정 소수점 수를 읽는다.
     *
     * @param pos position / 위치
     * @return the value / 값
     */
    private double s15(int pos) {
        return (int) u32(d, pos) / 65536.0;
    }

    /**
     * Reads four bytes as an ASCII string.
     * <p>
     * 네 바이트를 ASCII 문자열로 읽는다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @param len number of bytes / 바이트 수
     * @return the string / 문자열
     */
    private static String ascii(byte[] d, int pos, int len) {
        char[] c = new char[len];
        // 바이트마다 문자 하나로 만든다.
        for (int i = 0; i < len; i++) {
            c[i] = (char) (d[pos + i] & 0xFF);
        }
        return new String(c);
    }

    /**
     * Reads an unsigned big-endian 16-bit value.
     * <p>
     * 부호 없는 빅 엔디언 16비트 값을 읽는다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return the value / 값
     */
    private static int u16(byte[] d, int pos) {
        return ((d[pos] & 0xFF) << 8) | (d[pos + 1] & 0xFF);
    }

    /**
     * Reads an unsigned big-endian 32-bit value.
     * <p>
     * 부호 없는 빅 엔디언 32비트 값을 읽는다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return the value / 값
     */
    private static long u32(byte[] d, int pos) {
        return ((long) u16(d, pos) << 16) | u16(d, pos + 2);
    }
}
