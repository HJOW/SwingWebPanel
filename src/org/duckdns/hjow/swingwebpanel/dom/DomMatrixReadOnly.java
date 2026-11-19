package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A 4x4 matrix of the geometry interfaces (<code>DOMMatrixReadOnly</code>, Geometry Interfaces Module Level 1, section 6): the sixteen
 * elements <code>m11</code> to <code>m44</code> and the flag <code>is2D</code>. The elements are stored in column-major order (the first
 * four numbers are <code>m11</code>, <code>m12</code>, <code>m13</code>, <code>m14</code>, the first column; <code>m41</code>,
 * <code>m42</code>, <code>m43</code> are the translation), the way the specification, <code>matrix3d()</code> and <code>toFloat64Array()</code>
 * list them; <code>a</code> to <code>f</code> are the aliases <code>m11</code>, <code>m12</code>, <code>m21</code>, <code>m22</code>,
 * <code>m41</code>, <code>m42</code>. Points are column vectors, a matrix transforms a point as <code>M x p</code>, and the multiplication
 * methods post-multiply (<code>A.multiplySelf(B)</code> is <code>A x B</code>, so B is applied to a point first). This class only reads; the
 * variant that scripts and the canvas may change is {@link DomMatrix}. There is no 3D rendering: 3D values are stored, multiplied and
 * inverted, and the canvas uses only the 2D part (see the canvas notes in <code>docs/progress/canvas.md</code>).
 * <p>
 * 기하 인터페이스의 4x4 행렬(<code>DOMMatrixReadOnly</code>, Geometry Interfaces Module Level 1, 6 절): 열여섯 요소 <code>m11</code> ~ <code>m44</code> 와
 * 플래그 <code>is2D</code>. 요소는 열 우선 순서로 저장한다(처음 네 숫자가 첫 열인 <code>m11</code>, <code>m12</code>, <code>m13</code>, <code>m14</code> 이고
 * <code>m41</code>, <code>m42</code>, <code>m43</code> 이 이동이다). 규격과 <code>matrix3d()</code>, <code>toFloat64Array()</code> 가 나열하는 순서와 같다.
 * <code>a</code> ~ <code>f</code> 는 <code>m11</code>, <code>m12</code>, <code>m21</code>, <code>m22</code>, <code>m41</code>, <code>m42</code> 의 별칭이다.
 * 점은 열 벡터이고 행렬은 점을 <code>M x p</code> 로 변환하며, 곱셈 메서드는 뒤에 곱한다(<code>A.multiplySelf(B)</code> 는 <code>A x B</code> 이므로 점에는 B 가
 * 먼저 적용된다). 이 클래스는 읽기만 하며 스크립트와 캔버스가 바꿀 수 있는 변종은 {@link DomMatrix} 이다. 3D 그리기는 없다: 3D 값은 저장, 곱셈, 역행렬만 하고
 * 캔버스는 2D 부분만 쓴다(<code>docs/progress/canvas.md</code> 의 캔버스 메모 참고).
 */
public class DomMatrixReadOnly {

    /**
     * The index of <code>m11</code> (alias <code>a</code>).
     * <p>
     * <code>m11</code>(별칭 <code>a</code>)의 위치.
     */
    public static final int M11 = 0;

    /**
     * The index of <code>m12</code> (alias <code>b</code>).
     * <p>
     * <code>m12</code>(별칭 <code>b</code>)의 위치.
     */
    public static final int M12 = 1;

    /**
     * The index of <code>m21</code> (alias <code>c</code>).
     * <p>
     * <code>m21</code>(별칭 <code>c</code>)의 위치.
     */
    public static final int M21 = 4;

    /**
     * The index of <code>m22</code> (alias <code>d</code>).
     * <p>
     * <code>m22</code>(별칭 <code>d</code>)의 위치.
     */
    public static final int M22 = 5;

    /**
     * The index of <code>m41</code> (alias <code>e</code>).
     * <p>
     * <code>m41</code>(별칭 <code>e</code>)의 위치.
     */
    public static final int M41 = 12;

    /**
     * The index of <code>m42</code> (alias <code>f</code>).
     * <p>
     * <code>m42</code>(별칭 <code>f</code>)의 위치.
     */
    public static final int M42 = 13;

    /**
     * The sixteen elements in column-major order; the 3D elements of a 2D matrix are those of the identity.
     * <p>
     * 열 우선 순서의 열여섯 요소. 2D 행렬의 3D 요소는 단위 행렬의 값이다.
     */
    final double[] v = new double[16];

    /**
     * The <code>is2D</code> flag: the matrix was made as a 2D matrix and only 2D operations were applied.
     * <p>
     * <code>is2D</code> 플래그: 2D 행렬로 만들어졌고 2D 연산만 적용되었다.
     */
    boolean twoD;

    /**
     * Creates the identity matrix (<code>is2D</code> is true).
     * <p>
     * 단위 행렬을 만든다(<code>is2D</code> 는 true).
     */
    public DomMatrixReadOnly() {
        v[0] = 1;
        v[5] = 1;
        v[10] = 1;
        v[15] = 1;
        twoD = true;
    }

    /**
     * Creates a copy of a matrix.
     * <p>
     * 행렬의 복사본을 만든다.
     *
     * @param other the matrix to copy / 복사할 행렬
     */
    public DomMatrixReadOnly(DomMatrixReadOnly other) {
        System.arraycopy(other.v, 0, v, 0, 16);
        twoD = other.twoD;
    }

    /**
     * Creates a 2D matrix from the six numbers <code>a</code> to <code>f</code> (<code>is2D</code> is true).
     * <p>
     * 숫자 여섯 개 <code>a</code> ~ <code>f</code> 로 2D 행렬을 만든다(<code>is2D</code> 는 true).
     *
     * @param a m11 / m11
     * @param b m12 / m12
     * @param c m21 / m21
     * @param d m22 / m22
     * @param e m41 / m41
     * @param f m42 / m42
     */
    public DomMatrixReadOnly(double a, double b, double c, double d, double e, double f) {
        this();
        v[M11] = a;
        v[M12] = b;
        v[M21] = c;
        v[M22] = d;
        v[M41] = e;
        v[M42] = f;
    }

    /**
     * Creates a 3D matrix from sixteen numbers in column-major order (<code>is2D</code> is false).
     * <p>
     * 열 우선 순서의 숫자 열여섯 개로 3D 행렬을 만든다(<code>is2D</code> 는 false).
     *
     * @param values sixteen numbers m11, m12, m13, m14, m21 ... m44 / 숫자 열여섯 개 m11, m12, m13, m14, m21 ... m44
     */
    public DomMatrixReadOnly(double[] values) {
        if (values == null || values.length != 16) {
            throw new IllegalArgumentException("sixteen values are required");
        }
        System.arraycopy(values, 0, v, 0, 16);
        twoD = false;
    }

    /**
     * Returns one element by its index in column-major order (0 is <code>m11</code>, 1 is <code>m12</code>, 4 is <code>m21</code>, 15 is
     * <code>m44</code>).
     * <p>
     * 열 우선 순서의 위치로 요소 하나를 반환한다(0 은 <code>m11</code>, 1 은 <code>m12</code>, 4 는 <code>m21</code>, 15 는 <code>m44</code>).
     *
     * @param index the position, 0 to 15 / 위치(0 ~ 15)
     * @return the element / 요소
     */
    public double get(int index) {
        return v[index];
    }

    /**
     * Returns a copy of the sixteen elements in column-major order.
     * <p>
     * 열여섯 요소의 복사본을 열 우선 순서로 반환한다.
     *
     * @return sixteen numbers / 숫자 열여섯 개
     */
    public double[] toArray() {
        return v.clone();
    }

    /**
     * Returns the six numbers of the 2D part: a, b, c, d, e, f.
     * <p>
     * 2D 부분의 숫자 여섯 개를 반환한다: a, b, c, d, e, f.
     *
     * @return six numbers / 숫자 여섯 개
     */
    public double[] to2DArray() {
        return new double[] { v[M11], v[M12], v[M21], v[M22], v[M41], v[M42] };
    }

    /**
     * Tells whether the matrix is a 2D matrix (the <code>is2D</code> attribute).
     * <p>
     * 행렬이 2D 행렬인지 알려 준다(<code>is2D</code> 속성).
     *
     * @return true for a 2D matrix / 2D 행렬이면 true
     */
    public boolean is2D() {
        return twoD;
    }

    /**
     * Tells whether the matrix is the identity (the <code>isIdentity</code> attribute): the diagonal is 1 and every other element is 0 or -0.
     * <p>
     * 행렬이 단위 행렬인지 알려 준다(<code>isIdentity</code> 속성): 대각선이 1 이고 다른 요소는 모두 0 또는 -0 이다.
     *
     * @return true for the identity / 단위 행렬이면 true
     */
    public boolean isIdentity() {
        // 열여섯 요소를 모두 단위 행렬의 값과 비교한다(-0 은 0 과 같다).
        for (int i = 0; i < 16; i++) {
            double expected = i % 5 == 0 ? 1 : 0;
            if (v[i] != expected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether all sixteen elements are finite numbers (the stringifier throws otherwise).
     * <p>
     * 열여섯 요소가 모두 유한한 수인지 알려 준다(아니면 문자열 변환이 예외를 던진다).
     *
     * @return true if no element is NaN or infinite / NaN 이나 무한대인 요소가 없으면 true
     */
    public boolean isFinite() {
        // 요소를 하나씩 보며 NaN 과 무한대를 찾는다.
        for (int i = 0; i < 16; i++) {
            if (Double.isNaN(v[i]) || Double.isInfinite(v[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Transforms a point (section 2.1 of the module): the point as a column vector (x, y, z, w) is pre-multiplied by this matrix. A 2D matrix
     * with a point whose z is 0 and w is 1 uses only the 2D part, so the other elements never turn an infinite coordinate into NaN.
     * <p>
     * 점을 변환한다(모듈 2.1 절): 열 벡터 (x, y, z, w) 인 점에 이 행렬을 앞에서 곱한다. 2D 행렬이고 점의 z 가 0, w 가 1 이면 2D 부분만 쓰므로 다른 요소가
     * 무한대 좌표를 NaN 으로 만들지 않는다.
     *
     * @param p the point / 점
     * @return a new point / 새 점
     */
    public DomPoint transformPoint(DomPointReadOnly p) {
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        double w = p.getW();
        // 2D 행렬과 2D 점의 변환이면 a ~ f 만으로 계산한다.
        if (twoD && z == 0 && w == 1) {
            return new DomPoint(v[M11] * x + v[M21] * y + v[M41], v[M12] * x + v[M22] * y + v[M42], z, w);
        }
        return new DomPoint(v[0] * x + v[4] * y + v[8] * z + v[12] * w, v[1] * x + v[5] * y + v[9] * z + v[13] * w,
                v[2] * x + v[6] * y + v[10] * z + v[14] * w, v[3] * x + v[7] * y + v[11] * z + v[15] * w);
    }
}
