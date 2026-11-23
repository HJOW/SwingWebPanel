package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A matrix that scripts and the canvas may change (<code>DOMMatrix</code>, Geometry Interfaces Module Level 1, section 6): the transformation
 * methods of {@link DomMatrixReadOnly} that change the matrix itself (<code>multiplySelf</code>, <code>translateSelf</code>,
 * <code>scaleSelf</code>, <code>rotateSelf</code>, <code>skewXSelf</code>, <code>invertSelf</code> ...). Every method post-multiplies the
 * transformation (the new transformation is applied to a point first) and returns this matrix, so calls can be chained. The
 * <code>is2D</code> flag turns false as soon as a 3D operation or a 3D element is applied and never comes back (only
 * {@link #setValues} restores it). A rotation or skew by 0 and a scale around the origin do not touch the elements they cannot change, so an
 * infinite element is not turned into NaN by a multiplication with zero.
 * <p>
 * 스크립트와 캔버스가 바꿀 수 있는 행렬(<code>DOMMatrix</code>, Geometry Interfaces Module Level 1, 6 절): {@link DomMatrixReadOnly} 의 변환 메서드 중
 * 행렬 자신을 바꾸는 것들(<code>multiplySelf</code>, <code>translateSelf</code>, <code>scaleSelf</code>, <code>rotateSelf</code>, <code>skewXSelf</code>,
 * <code>invertSelf</code> 등). 모든 메서드는 변환을 뒤에 곱하고(새 변환이 점에 먼저 적용된다) 이 행렬을 반환하므로 호출을 이어 쓸 수 있다.
 * <code>is2D</code> 플래그는 3D 연산이나 3D 요소가 적용되는 즉시 false 가 되며 되돌아오지 않는다({@link #setValues} 만 복원한다).
 * 0 도 회전과 기울이기, 원점 둘레의 확대는 바꿀 수 없는 요소를 건드리지 않으므로 무한대 요소가 0 과의 곱으로 NaN 이 되지 않는다.
 */
public class DomMatrix extends DomMatrixReadOnly {

    /**
     * Creates the identity matrix.
     * <p>
     * 단위 행렬을 만든다.
     */
    public DomMatrix() {
        super();
    }

    /**
     * Creates a copy of a matrix.
     * <p>
     * 행렬의 복사본을 만든다.
     *
     * @param other the matrix to copy / 복사할 행렬
     */
    public DomMatrix(DomMatrixReadOnly other) {
        super(other);
    }

    /**
     * Creates a 2D matrix from the six numbers <code>a</code> to <code>f</code>.
     * <p>
     * 숫자 여섯 개 <code>a</code> ~ <code>f</code> 로 2D 행렬을 만든다.
     *
     * @param a m11 / m11
     * @param b m12 / m12
     * @param c m21 / m21
     * @param d m22 / m22
     * @param e m41 / m41
     * @param f m42 / m42
     */
    public DomMatrix(double a, double b, double c, double d, double e, double f) {
        super(a, b, c, d, e, f);
    }

    /**
     * Creates a 3D matrix from sixteen numbers in column-major order.
     * <p>
     * 열 우선 순서의 숫자 열여섯 개로 3D 행렬을 만든다.
     *
     * @param values sixteen numbers / 숫자 열여섯 개
     */
    public DomMatrix(double[] values) {
        super(values);
    }

    /**
     * Sets one element (the attribute setters <code>m11</code> to <code>m44</code> and <code>a</code> to <code>f</code>). A 3D element
     * (everything except <code>m11</code>, <code>m12</code>, <code>m21</code>, <code>m22</code>, <code>m41</code>, <code>m42</code>) that is set to a
     * value other than the identity value (1 for <code>m33</code> and <code>m44</code>, 0 or -0 for the others) turns <code>is2D</code> false.
     * <p>
     * 요소 하나를 정한다(속성 설정자 <code>m11</code> ~ <code>m44</code>, <code>a</code> ~ <code>f</code>). 3D 요소(<code>m11</code>, <code>m12</code>,
     * <code>m21</code>, <code>m22</code>, <code>m41</code>, <code>m42</code> 를 뺀 모든 요소)를 단위 행렬의 값(<code>m33</code>, <code>m44</code> 는 1, 나머지는
     * 0 또는 -0)이 아닌 값으로 정하면 <code>is2D</code> 가 false 가 된다.
     *
     * @param index the position in column-major order, 0 to 15 / 열 우선 순서의 위치(0 ~ 15)
     * @param value the new value / 새 값
     */
    public void set(int index, double value) {
        v[index] = value;
        // 2D 에 속하지 않는 요소가 단위 행렬의 값이 아니게 되면 3D 행렬이 된다.
        boolean part2D = index == M11 || index == M12 || index == M21 || index == M22 || index == M41 || index == M42;
        if (!part2D) {
            double identity = index == 10 || index == 15 ? 1 : 0;
            if (value != identity) {
                twoD = false;
            }
        }
    }

    /**
     * Replaces the whole matrix (<code>setMatrixValue</code> and the constructors of the scripts): the only way for a matrix to become 2D again.
     * <p>
     * 행렬 전체를 바꾼다(<code>setMatrixValue</code> 와 스크립트 생성자). 행렬이 다시 2D 가 될 수 있는 유일한 방법이다.
     *
     * @param values sixteen numbers in column-major order / 열 우선 순서의 숫자 열여섯 개
     * @param is2D   the new <code>is2D</code> flag / 새 <code>is2D</code> 플래그
     * @return this matrix / 이 행렬
     */
    public DomMatrix setValues(double[] values, boolean is2D) {
        System.arraycopy(values, 0, v, 0, 16);
        twoD = is2D;
        return this;
    }

    /**
     * Post-multiplies a matrix: this = this x other (<code>multiplySelf</code>). The result is 3D if the other matrix is.
     * <p>
     * 행렬을 뒤에 곱한다: this = this x other(<code>multiplySelf</code>). 상대 행렬이 3D 이면 결과도 3D 이다.
     *
     * @param other the other matrix / 상대 행렬
     * @return this matrix / 이 행렬
     */
    public DomMatrix multiplySelf(DomMatrixReadOnly other) {
        multiplyInto(this.v, this.twoD, other.v, other.twoD);
        twoD = twoD && other.twoD;
        return this;
    }

    /**
     * Pre-multiplies a matrix: this = other x this (<code>preMultiplySelf</code>). The result is 3D if the other matrix is.
     * <p>
     * 행렬을 앞에 곱한다: this = other x this(<code>preMultiplySelf</code>). 상대 행렬이 3D 이면 결과도 3D 이다.
     *
     * @param other the other matrix / 상대 행렬
     * @return this matrix / 이 행렬
     */
    public DomMatrix preMultiplySelf(DomMatrixReadOnly other) {
        boolean both2D = twoD && other.twoD;
        double[] left = other.v.clone();
        multiplyInto(left, other.twoD, this.v, this.twoD);
        System.arraycopy(left, 0, v, 0, 16);
        twoD = both2D;
        return this;
    }

    /**
     * Computes <code>left x right</code> into <code>left</code>. Two 2D matrices use only their 2D elements.
     * <p>
     * <code>left x right</code> 를 <code>left</code> 에 계산해 넣는다. 둘 다 2D 이면 2D 요소만 쓴다.
     *
     * @param left    the left factor and the result / 왼쪽 인수이자 결과
     * @param left2D  whether the left factor is 2D / 왼쪽 인수가 2D 인지
     * @param right   the right factor, may be the same array as left / 오른쪽 인수(left 와 같은 배열일 수 있음)
     * @param right2D whether the right factor is 2D / 오른쪽 인수가 2D 인지
     */
    private static void multiplyInto(double[] left, boolean left2D, double[] right, boolean right2D) {
        // 둘 다 2D 이면 a ~ f 여섯 요소만으로 곱한다(무한대와 0 의 곱이 NaN 을 만들지 않는다).
        if (left2D && right2D) {
            double a1 = left[M11];
            double b1 = left[M12];
            double c1 = left[M21];
            double d1 = left[M22];
            double e1 = left[M41];
            double f1 = left[M42];
            double a2 = right[M11];
            double b2 = right[M12];
            double c2 = right[M21];
            double d2 = right[M22];
            double e2 = right[M41];
            double f2 = right[M42];
            left[M11] = a1 * a2 + c1 * b2;
            left[M12] = b1 * a2 + d1 * b2;
            left[M21] = a1 * c2 + c1 * d2;
            left[M22] = b1 * c2 + d1 * d2;
            left[M41] = a1 * e2 + c1 * f2 + e1;
            left[M42] = b1 * e2 + d1 * f2 + f1;
            return;
        }
        double[] a = left.clone();
        double[] b = right == left ? a : right;
        // 열마다, 행마다 왼쪽 행과 오른쪽 열의 내적을 구한다.
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                double sum = 0;
                // 왼쪽 행의 요소와 오른쪽 열의 요소를 차례로 곱해 더한다.
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + r] * b[c * 4 + k];
                }
                left[c * 4 + r] = sum;
            }
        }
    }

    /**
     * Post-multiplies a translation (<code>translateSelf</code>). A <code>tz</code> other than 0 or -0 makes the matrix 3D.
     * <p>
     * 이동을 뒤에 곱한다(<code>translateSelf</code>). 0 이나 -0 이 아닌 <code>tz</code> 는 행렬을 3D 로 만든다.
     *
     * @param tx the x distance / x 거리
     * @param ty the y distance / y 거리
     * @param tz the z distance / z 거리
     * @return this matrix / 이 행렬
     */
    public DomMatrix translateSelf(double tx, double ty, double tz) {
        // 2D 행렬의 2D 이동이면 e 와 f 만 바꾼다.
        if (twoD && tz == 0) {
            v[M41] += v[M11] * tx + v[M21] * ty;
            v[M42] += v[M12] * tx + v[M22] * ty;
            return this;
        }
        if (!(tz == 0)) {
            twoD = false;
        }
        // 네 번째 열에 앞의 세 열을 이동량으로 곱해 더한다.
        for (int r = 0; r < 4; r++) {
            v[12 + r] += v[r] * tx + v[4 + r] * ty + v[8 + r] * tz;
        }
        return this;
    }

    /**
     * Multiplies the first three columns by the scale factors (a post-multiplied scale). A 2D matrix with <code>sz</code> 1 changes only its
     * 2D elements.
     * <p>
     * 앞의 세 열에 확대 비율을 곱한다(뒤에 곱한 확대). <code>sz</code> 가 1 인 2D 행렬은 2D 요소만 바꾼다.
     *
     * @param sx the x factor / x 비율
     * @param sy the y factor / y 비율
     * @param sz the z factor / z 비율
     */
    private void scaleColumns(double sx, double sy, double sz) {
        if (twoD && sz == 1) {
            v[M11] *= sx;
            v[M12] *= sx;
            v[M21] *= sy;
            v[M22] *= sy;
            return;
        }
        // 열 하나의 네 행을 모두 해당 비율로 곱한다.
        for (int r = 0; r < 4; r++) {
            v[r] *= sx;
            v[4 + r] *= sy;
            v[8 + r] *= sz;
        }
    }

    /**
     * Post-multiplies a scale around an origin (<code>scaleSelf</code>). A <code>sz</code> other than 1 makes the matrix 3D.
     * <p>
     * 기준점 둘레의 확대를 뒤에 곱한다(<code>scaleSelf</code>). 1 이 아닌 <code>sz</code> 는 행렬을 3D 로 만든다.
     *
     * @param sx the x factor / x 비율
     * @param sy the y factor / y 비율
     * @param sz the z factor / z 비율
     * @param ox the x of the origin / 기준점 x
     * @param oy the y of the origin / 기준점 y
     * @param oz the z of the origin / 기준점 z
     * @return this matrix / 이 행렬
     */
    public DomMatrix scaleSelf(double sx, double sy, double sz, double ox, double oy, double oz) {
        boolean moved = !(ox == 0 && oy == 0 && oz == 0);
        // 기준점이 원점이 아니면 이동, 확대, 되돌림의 순서로 곱한다.
        if (moved) {
            translateSelf(ox, oy, oz);
        }
        scaleColumns(sx, sy, sz);
        if (moved) {
            translateSelf(-ox, -oy, -oz);
        }
        if (!(sz == 1)) {
            twoD = false;
        }
        return this;
    }

    /**
     * Post-multiplies a uniform 3D scale around an origin (<code>scale3dSelf</code>). A <code>scale</code> other than 1 makes the matrix 3D.
     * <p>
     * 기준점 둘레의 균일한 3D 확대를 뒤에 곱한다(<code>scale3dSelf</code>). 1 이 아닌 <code>scale</code> 은 행렬을 3D 로 만든다.
     *
     * @param scale the factor / 비율
     * @param ox    the x of the origin / 기준점 x
     * @param oy    the y of the origin / 기준점 y
     * @param oz    the z of the origin / 기준점 z
     * @return this matrix / 이 행렬
     */
    public DomMatrix scale3dSelf(double scale, double ox, double oy, double oz) {
        boolean moved = !(ox == 0 && oy == 0 && oz == 0);
        // 기준점이 원점이 아니면 이동, 확대, 되돌림의 순서로 곱한다.
        if (moved) {
            translateSelf(ox, oy, oz);
        }
        scaleColumns(scale, scale, scale);
        if (moved) {
            translateSelf(-ox, -oy, -oz);
        }
        if (!(scale == 1)) {
            twoD = false;
        }
        return this;
    }

    /**
     * Converts degrees to radians.
     * <p>
     * 도를 라디안으로 바꾼다.
     *
     * @param degrees degrees / 도
     * @return radians / 라디안
     */
    private static double radians(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    /**
     * Post-multiplies a rotation around the z axis by an angle in radians. A 2D matrix changes only its 2D elements.
     * <p>
     * z 축 둘레의 회전(라디안)을 뒤에 곱한다. 2D 행렬은 2D 요소만 바꾼다.
     *
     * @param angle the angle in radians / 라디안 각도
     */
    private void rotateZRadians(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        int rows = twoD ? 2 : 4;
        // 첫째 열과 둘째 열을 섞는다(첫째 열 = 첫째 x cos + 둘째 x sin, 둘째 열 = 둘째 x cos - 첫째 x sin).
        for (int r = 0; r < rows; r++) {
            double c1 = v[r];
            double c2 = v[4 + r];
            v[r] = c1 * cos + c2 * sin;
            v[4 + r] = c2 * cos - c1 * sin;
        }
    }

    /**
     * Post-multiplies a rotation around the y axis by an angle in radians.
     * <p>
     * y 축 둘레의 회전(라디안)을 뒤에 곱한다.
     *
     * @param angle the angle in radians / 라디안 각도
     */
    private void rotateYRadians(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // 첫째 열과 셋째 열을 섞는다.
        for (int r = 0; r < 4; r++) {
            double c1 = v[r];
            double c3 = v[8 + r];
            v[r] = c1 * cos - c3 * sin;
            v[8 + r] = c1 * sin + c3 * cos;
        }
    }

    /**
     * Post-multiplies a rotation around the x axis by an angle in radians.
     * <p>
     * x 축 둘레의 회전(라디안)을 뒤에 곱한다.
     *
     * @param angle the angle in radians / 라디안 각도
     */
    private void rotateXRadians(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // 둘째 열과 셋째 열을 섞는다.
        for (int r = 0; r < 4; r++) {
            double c2 = v[4 + r];
            double c3 = v[8 + r];
            v[4 + r] = c2 * cos + c3 * sin;
            v[8 + r] = c3 * cos - c2 * sin;
        }
    }

    /**
     * Post-multiplies the rotations around z, then y, then x (<code>rotateSelf</code>); the angles are in degrees and the caller has already
     * resolved the missing arguments. A rotation about x or y other than 0 or -0 makes the matrix 3D. A rotation by 0 is skipped.
     * <p>
     * z, y, x 축 둘레의 회전을 차례로 뒤에 곱한다(<code>rotateSelf</code>). 각도는 도이며 빠진 인수는 호출자가 이미 정했다. 0 이나 -0 이 아닌 x 나 y 축 회전은
     * 행렬을 3D 로 만든다. 0 도 회전은 건너뛴다.
     *
     * @param rotX the angle around x / x 축 각도
     * @param rotY the angle around y / y 축 각도
     * @param rotZ the angle around z / z 축 각도
     * @return this matrix / 이 행렬
     */
    public DomMatrix rotateSelf(double rotX, double rotY, double rotZ) {
        if (!(rotX == 0) || !(rotY == 0)) {
            twoD = false;
        }
        if (!(rotZ == 0)) {
            rotateZRadians(radians(rotZ));
        }
        if (!(rotY == 0)) {
            rotateYRadians(radians(rotY));
        }
        if (!(rotX == 0)) {
            rotateXRadians(radians(rotX));
        }
        return this;
    }

    /**
     * Post-multiplies the 2D rotation by the angle of the vector (x, y) (<code>rotateFromVectorSelf</code>); the angle of the zero vector is 0.
     * <p>
     * 벡터 (x, y) 의 각도만큼의 2D 회전을 뒤에 곱한다(<code>rotateFromVectorSelf</code>). 영 벡터의 각도는 0 이다.
     *
     * @param x the x of the vector / 벡터 x
     * @param y the y of the vector / 벡터 y
     * @return this matrix / 이 행렬
     */
    public DomMatrix rotateFromVectorSelf(double x, double y) {
        if (x == 0 && y == 0) {
            return this;
        }
        rotateZRadians(Math.atan2(y, x));
        return this;
    }

    /**
     * Post-multiplies a rotation around an axis (<code>rotateAxisAngleSelf</code>); the angle is in degrees. A rotation around an axis whose x
     * or y is not 0 or -0 makes the matrix 3D; the zero axis and the angle 0 change nothing.
     * <p>
     * 축 둘레의 회전을 뒤에 곱한다(<code>rotateAxisAngleSelf</code>). 각도는 도이다. x 나 y 가 0 또는 -0 이 아닌 축 둘레의 회전은 행렬을 3D 로 만든다.
     * 영 축과 0 도는 아무것도 바꾸지 않는다.
     *
     * @param x     the x of the axis / 축 x
     * @param y     the y of the axis / 축 y
     * @param z     the z of the axis / 축 z
     * @param angle the angle in degrees / 도 단위 각도
     * @return this matrix / 이 행렬
     */
    public DomMatrix rotateAxisAngleSelf(double x, double y, double z, double angle) {
        if (!(x == 0) || !(y == 0)) {
            twoD = false;
        }
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0 || angle == 0) {
            return this;
        }
        double nx = x / length;
        double ny = y / length;
        double nz = z / length;
        double alpha = radians(angle);
        // 좌표축이면 cos 와 sin 으로 곧바로 회전한다.
        if (nx == 0 && ny == 0) {
            rotateZRadians(nz > 0 ? alpha : -alpha);
            return this;
        } else if (nx == 0 && nz == 0) {
            rotateYRadians(ny > 0 ? alpha : -alpha);
            return this;
        } else if (ny == 0 && nz == 0) {
            rotateXRadians(nx > 0 ? alpha : -alpha);
            return this;
        }
        double sc = Math.sin(alpha / 2) * Math.cos(alpha / 2);
        double sq = Math.sin(alpha / 2) * Math.sin(alpha / 2);
        double[] r = new double[16];
        r[0] = 1 - 2 * (ny * ny + nz * nz) * sq;
        r[1] = 2 * (nx * ny * sq + nz * sc);
        r[2] = 2 * (nx * nz * sq - ny * sc);
        r[4] = 2 * (nx * ny * sq - nz * sc);
        r[5] = 1 - 2 * (nx * nx + nz * nz) * sq;
        r[6] = 2 * (ny * nz * sq + nx * sc);
        r[8] = 2 * (nx * nz * sq + ny * sc);
        r[9] = 2 * (ny * nz * sq - nx * sc);
        r[10] = 1 - 2 * (nx * nx + ny * ny) * sq;
        r[15] = 1;
        multiplyInto(v, twoD, r, false);
        twoD = false;
        return this;
    }

    /**
     * Post-multiplies a skew along x by an angle in degrees (<code>skewXSelf</code>).
     * <p>
     * x 방향 기울이기(도)를 뒤에 곱한다(<code>skewXSelf</code>).
     *
     * @param sx the angle in degrees / 도 단위 각도
     * @return this matrix / 이 행렬
     */
    public DomMatrix skewXSelf(double sx) {
        if (sx == 0) {
            return this;
        }
        double t = Math.tan(radians(sx));
        int rows = twoD ? 2 : 4;
        // 둘째 열에 첫째 열의 t 배를 더한다.
        for (int r = 0; r < rows; r++) {
            v[4 + r] += v[r] * t;
        }
        return this;
    }

    /**
     * Post-multiplies a skew along y by an angle in degrees (<code>skewYSelf</code>).
     * <p>
     * y 방향 기울이기(도)를 뒤에 곱한다(<code>skewYSelf</code>).
     *
     * @param sy the angle in degrees / 도 단위 각도
     * @return this matrix / 이 행렬
     */
    public DomMatrix skewYSelf(double sy) {
        if (sy == 0) {
            return this;
        }
        double t = Math.tan(radians(sy));
        int rows = twoD ? 2 : 4;
        // 첫째 열에 둘째 열의 t 배를 더한다.
        for (int r = 0; r < rows; r++) {
            v[r] += v[4 + r] * t;
        }
        return this;
    }

    /**
     * Turns the matrix into its inverse (<code>invertSelf</code>). A matrix that cannot be inverted (a determinant that is 0, NaN or infinite)
     * becomes sixteen NaN and is not 2D any more.
     * <p>
     * 행렬을 역행렬로 바꾼다(<code>invertSelf</code>). 뒤집을 수 없는 행렬(행렬식이 0, NaN, 무한대)은 NaN 열여섯 개가 되고 2D 가 아니게 된다.
     *
     * @return this matrix / 이 행렬
     */
    public DomMatrix invertSelf() {
        // 2D 행렬은 3x2 아핀 행렬로 뒤집는다.
        if (twoD) {
            double a = v[M11];
            double b = v[M12];
            double c = v[M21];
            double d = v[M22];
            double e = v[M41];
            double f = v[M42];
            double det = a * d - b * c;
            if (det == 0 || Double.isNaN(det) || Double.isInfinite(det)) {
                makeNaN();
                return this;
            }
            v[M11] = d / det;
            v[M12] = -b / det;
            v[M21] = -c / det;
            v[M22] = a / det;
            v[M41] = (c * f - d * e) / det;
            v[M42] = (b * e - a * f) / det;
            return this;
        }
        double[] m = v;
        double[] inv = new double[16];
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        double det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0 || Double.isNaN(det) || Double.isInfinite(det)) {
            makeNaN();
            return this;
        }
        // 수반 행렬을 행렬식으로 나눠 역행렬을 얻는다.
        for (int i = 0; i < 16; i++) {
            v[i] = inv[i] / det;
        }
        return this;
    }

    /**
     * Sets every element to NaN and the matrix to 3D (the result of inverting a matrix that has no inverse).
     * <p>
     * 모든 요소를 NaN 으로 하고 3D 로 만든다(역행렬이 없는 행렬을 뒤집은 결과).
     */
    private void makeNaN() {
        // 열여섯 요소 모두 NaN 으로 채운다.
        for (int i = 0; i < 16; i++) {
            v[i] = Double.NaN;
        }
        twoD = false;
    }
}
