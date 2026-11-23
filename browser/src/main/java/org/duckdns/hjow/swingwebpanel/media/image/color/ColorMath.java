package org.duckdns.hjow.swingwebpanel.media.image.color;

/**
 * The small linear algebra and colorimetry of the colour conversion (plan item P18-03): 3 x 3 matrices, the matrix that turns the chromaticities of
 * RGB primaries and a white point into CIE XYZ, the Bradford chromatic adaptation (ICC.1:2010 Annex E.3, the same method that ICC profiles use to
 * relate the D50 profile connection space to other white points) and the constants of sRGB (IEC 61966-2-1): its primaries, the D65 white point
 * and the matrices between linear sRGB and XYZ. All numbers are computed in double precision from the chromaticities, so a source whose white
 * point is D65 and whose primaries are those of sRGB gives exactly the identity matrix.
 * <p>
 * 색 변환의 작은 선형대수와 색채학(계획 P18-03): 3 x 3 행렬, RGB 원색과 백색점의 색도 좌표를 CIE XYZ 로 바꾸는 행렬, Bradford 색 적응(ICC.1:2010
 * 부록 E.3. ICC 프로파일이 D50 프로파일 연결 공간을 다른 백색점과 잇는 방법과 같다), sRGB(IEC 61966-2-1)의 상수: 원색, D65 백색점, 선형 sRGB 와
 * XYZ 사이의 행렬. 모든 수는 색도 좌표에서 배정밀도로 계산하므로 백색점이 D65 이고 원색이 sRGB 의 것인 소스는 정확히 단위 행렬을 준다.
 */
final class ColorMath {

    /**
     * The chromaticity (x, y) of the D65 white point that sRGB uses.
     * <p>
     * sRGB 가 쓰는 D65 백색점의 색도 좌표 (x, y).
     */
    static final double[] D65_XY = { 0.3127, 0.3290 };

    /**
     * The chromaticities {x, y} of the red, green and blue primaries of sRGB and Rec. 709.
     * <p>
     * sRGB 와 Rec. 709 의 빨강, 초록, 파랑 원색의 색도 좌표 {x, y}.
     */
    static final double[][] SRGB_PRIMARIES = { { 0.640, 0.330 }, { 0.300, 0.600 }, { 0.150, 0.060 } };

    /**
     * The D50 illuminant of the profile connection space of ICC profiles (ICC.1:2010 section 7.2.16: X 0.9642, Y 1.0, Z 0.8249).
     * <p>
     * ICC 프로파일의 프로파일 연결 공간 D50 조명(ICC.1:2010 7.2.16절: X 0.9642, Y 1.0, Z 0.8249).
     */
    static final double[] D50_ICC_XYZ = { 0.9642, 1.0, 0.8249 };

    /**
     * The XYZ of the D65 white point (with Y = 1).
     * <p>
     * D65 백색점의 XYZ(Y = 1).
     */
    static final double[] D65_XYZ = whiteXyz(D65_XY[0], D65_XY[1]);

    /**
     * The matrix from linear sRGB to XYZ relative to D65.
     * <p>
     * 선형 sRGB 에서 D65 기준 XYZ 로의 행렬.
     */
    static final double[][] SRGB_TO_XYZ = primariesToXyz(SRGB_PRIMARIES, D65_XY);

    /**
     * The matrix from XYZ relative to D65 to linear sRGB.
     * <p>
     * D65 기준 XYZ 에서 선형 sRGB 로의 행렬.
     */
    static final double[][] XYZ_TO_SRGB = invert(SRGB_TO_XYZ);

    /**
     * The Bradford matrix that moves XYZ into the cone response space (Lindbloom, from ICC.1:2010 Annex E.3).
     * <p>
     * XYZ 를 원추 반응 공간으로 옮기는 Bradford 행렬(Lindbloom, ICC.1:2010 부록 E.3).
     */
    private static final double[][] BRADFORD = { { 0.8951, 0.2664, -0.1614 }, { -0.7502, 1.7135, 0.0367 }, { 0.0389, -0.0685, 1.0296 } };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ColorMath() {
    }

    /**
     * Returns the XYZ of a white point given by its chromaticity, scaled to Y = 1.
     * <p>
     * 색도 좌표로 주어진 백색점의 XYZ 를 Y = 1 로 맞춰 반환한다.
     *
     * @param x chromaticity x / 색도 x
     * @param y chromaticity y / 색도 y
     * @return {X, Y, Z} / {X, Y, Z}
     */
    static double[] whiteXyz(double x, double y) {
        return new double[] { x / y, 1.0, (1.0 - x - y) / y };
    }

    /**
     * Multiplies two 3 x 3 matrices.
     * <p>
     * 3 x 3 행렬 둘을 곱한다.
     *
     * @param a left matrix / 왼쪽 행렬
     * @param b right matrix / 오른쪽 행렬
     * @return a times b / a 곱하기 b
     */
    static double[][] multiply(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        // 행과 열마다 내적을 구한다.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }

    /**
     * Multiplies a 3 x 3 matrix with a vector.
     * <p>
     * 3 x 3 행렬과 벡터를 곱한다.
     *
     * @param m matrix / 행렬
     * @param v vector / 벡터
     * @return m times v / m 곱하기 v
     */
    static double[] multiply(double[][] m, double[] v) {
        return new double[] { m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2] };
    }

    /**
     * Inverts a 3 x 3 matrix with the adjugate; a singular matrix gives null.
     * <p>
     * 3 x 3 행렬의 역행렬을 수반 행렬로 구한다. 특이 행렬이면 null 을 반환한다.
     *
     * @param m matrix / 행렬
     * @return the inverse, or null if it does not exist / 역행렬. 없으면 null
     */
    static double[][] invert(double[][] m) {
        double a = m[0][0];
        double b = m[0][1];
        double c = m[0][2];
        double d = m[1][0];
        double e = m[1][1];
        double f = m[1][2];
        double g = m[2][0];
        double h = m[2][1];
        double i = m[2][2];
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-12 || Double.isNaN(det)) {
            return null;
        }
        double s = 1.0 / det;
        return new double[][] { { (e * i - f * h) * s, (c * h - b * i) * s, (b * f - c * e) * s },
            { (f * g - d * i) * s, (a * i - c * g) * s, (c * d - a * f) * s }, { (d * h - e * g) * s, (b * g - a * h) * s, (a * e - b * d) * s } };
    }

    /**
     * Builds the matrix that turns linear RGB of the given primaries into XYZ relative to the given white point: the columns are the XYZ of the
     * primaries, scaled so that RGB (1, 1, 1) gives the white point with Y = 1.
     * <p>
     * 주어진 원색의 선형 RGB 를 주어진 백색점 기준의 XYZ 로 바꾸는 행렬을 만든다. 열은 원색의 XYZ 이며 RGB (1, 1, 1) 이 Y = 1 인 백색점이 되도록
     * 크기를 맞춘다.
     *
     * @param primaries the chromaticities {x, y} of red, green and blue / 빨강, 초록, 파랑의 색도 좌표 {x, y}
     * @param whiteXy   the chromaticity {x, y} of the white point / 백색점의 색도 좌표 {x, y}
     * @return the matrix, or null if the primaries are degenerate / 행렬. 원색이 퇴화했으면 null
     */
    static double[][] primariesToXyz(double[][] primaries, double[] whiteXy) {
        double[][] p = new double[3][3];
        // 원색마다 Y = 1 로 정규화한 XYZ 를 열로 놓는다.
        for (int k = 0; k < 3; k++) {
            double x = primaries[k][0];
            double y = primaries[k][1];
            if (y <= 0) {
                return null;
            }
            p[0][k] = x / y;
            p[1][k] = 1.0;
            p[2][k] = (1.0 - x - y) / y;
        }
        double[][] inverse = invert(p);
        if (inverse == null) {
            return null;
        }
        double[] s = multiply(inverse, whiteXyz(whiteXy[0], whiteXy[1]));
        double[][] m = new double[3][3];
        // 각 열에 백색점을 맞추는 계수를 곱한다.
        for (int k = 0; k < 3; k++) {
            for (int r = 0; r < 3; r++) {
                m[r][k] = p[r][k] * s[k];
            }
        }
        return m;
    }

    /**
     * Builds the Bradford matrix that adapts XYZ values from one white point to another.
     * <p>
     * 한 백색점의 XYZ 값을 다른 백색점으로 적응시키는 Bradford 행렬을 만든다.
     *
     * @param from the XYZ of the source white point / 원래 백색점의 XYZ
     * @param to   the XYZ of the destination white point / 목표 백색점의 XYZ
     * @return the adaptation matrix / 적응 행렬
     */
    static double[][] bradford(double[] from, double[] to) {
        double[] src = multiply(BRADFORD, from);
        double[] dst = multiply(BRADFORD, to);
        double[][] scale = { { dst[0] / src[0], 0, 0 }, { 0, dst[1] / src[1], 0 }, { 0, 0, dst[2] / src[2] } };
        return multiply(invert(BRADFORD), multiply(scale, BRADFORD));
    }

    /**
     * Builds the matrix from linear RGB of the given primaries and white point to linear sRGB: the primaries give XYZ relative to their own
     * white, Bradford adaptation moves that white to D65 and the inverse of the sRGB matrix gives the sRGB components.
     * <p>
     * 주어진 원색과 백색점의 선형 RGB 에서 선형 sRGB 로의 행렬을 만든다. 원색이 자기 백색점 기준 XYZ 를 주고, Bradford 적응이 그 백색점을 D65 로 옮기며,
     * sRGB 행렬의 역행렬이 sRGB 성분을 준다.
     *
     * @param primaries the chromaticities of red, green and blue / 빨강, 초록, 파랑의 색도 좌표
     * @param whiteXy   the chromaticity of the white point / 백색점의 색도 좌표
     * @return the matrix, or null if the primaries are degenerate / 행렬. 원색이 퇴화했으면 null
     */
    static double[][] toSrgb(double[][] primaries, double[] whiteXy) {
        double[][] toXyz = primariesToXyz(primaries, whiteXy);
        if (toXyz == null) {
            return null;
        }
        return multiply(XYZ_TO_SRGB, multiply(bradford(whiteXyz(whiteXy[0], whiteXy[1]), D65_XYZ), toXyz));
    }
}
