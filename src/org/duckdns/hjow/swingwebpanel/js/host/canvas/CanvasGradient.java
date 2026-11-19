package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

/**
 * A <code>CanvasGradient</code>: a linear gradient along a line, a radial (two-circle, cone) gradient or a conic gradient around a center, with color
 * stops (the conic form is {@link #conic}: the offset is the clockwise angle from the start angle as a fraction of a turn). The gradient lives in
 * the coordinate space that is current when it is painted, not when it was created (so the context passes the current transformation matrix to
 * {@link #toPaint(AffineTransform)}). Colors are interpolated in non-premultiplied RGBA; before the first stop the first color and after the last
 * stop the last color is used; stops with the same offset make a hard edge; a gradient without stops paints transparent black. The radial form
 * follows the algorithm of the canvas specification: for every point the largest <code>w</code> whose circle
 * (<code>x(w)</code>, <code>y(w)</code>, <code>r(w)</code> with <code>r(w) &gt;= 0</code>) contains the point decides the color.
 * <p>
 * <code>CanvasGradient</code>: 직선을 따르는 선형 그라디언트, 두 원(원뿔)의 방사형 그라디언트, 중심 둘레의 원뿔형 그라디언트와 색 정지점(원뿔형은 {@link #conic}:
 * 오프셋은 시작 각도에서 시계 방향으로 잰 각도를 한 바퀴에 대한 비율로 나타낸 것). 그라디언트는 만들 때가 아니라 칠할 때의
 * 좌표계에 놓인다(그래서 컨텍스트가 현재 변환 행렬을 {@link #toPaint(AffineTransform)} 에 넘긴다). 색은 알파를 곱하지 않은 RGBA 로 보간한다. 첫 정지점
 * 앞은 첫 색, 마지막 정지점 뒤는 마지막 색이며, 같은 오프셋의 정지점은 날카로운 경계를 만들고, 정지점이 없으면 투명한 검정을 칠한다. 방사형은 캔버스
 * 규격의 알고리즘을 따른다: 각 점은 그 점을 포함하는 원(<code>x(w)</code>, <code>y(w)</code>, <code>r(w)</code>, <code>r(w) &gt;= 0</code>) 중 가장 큰
 * <code>w</code> 가 색을 정한다.
 */
public final class CanvasGradient {

    /**
     * The kind of the linear gradient.
     * <p>
     * 선형 그라디언트의 종류.
     */
    static final int LINEAR = 0;

    /**
     * The kind of the radial (two-circle) gradient.
     * <p>
     * 방사형(두 원) 그라디언트의 종류.
     */
    static final int RADIAL = 1;

    /**
     * The kind of the conic gradient.
     * <p>
     * 원뿔형(각도) 그라디언트의 종류.
     */
    static final int CONIC = 2;

    /**
     * The kind: {@link #LINEAR}, {@link #RADIAL} or {@link #CONIC}.
     * <p>
     * 종류: {@link #LINEAR}, {@link #RADIAL}, {@link #CONIC}.
     */
    private final int kind;

    /**
     * The geometry: x0, y0, r0, x1, y1, r1 (r0 and r1 are 0 for a linear gradient); for a conic gradient the center x and y and the start angle in
     * radians in the first three places.
     * <p>
     * 기하: x0, y0, r0, x1, y1, r1(선형이면 r0, r1 은 0). 원뿔형이면 앞의 세 칸에 중심 x, y 와 시작 각도(라디안).
     */
    private final double[] geometry;

    /**
     * The offsets of the stops in ascending order (equal offsets keep their insertion order).
     * <p>
     * 오름차순으로 정렬된 정지점의 오프셋(같은 오프셋은 넣은 순서를 유지한다).
     */
    private final List<Double> offsets = new ArrayList<Double>();

    /**
     * The colors of the stops, in the order of {@link #offsets}.
     * <p>
     * {@link #offsets} 와 같은 순서의 정지점 색.
     */
    private final List<Integer> colors = new ArrayList<Integer>();

    /**
     * Creates a gradient without stops (use {@link #linear} or {@link #radial}).
     * <p>
     * 정지점이 없는 그라디언트를 만든다({@link #linear} 또는 {@link #radial} 을 쓴다).
     *
     * @param kind     the kind of the gradient / 그라디언트의 종류
     * @param geometry x0, y0, r0, x1, y1, r1 / x0, y0, r0, x1, y1, r1
     */
    private CanvasGradient(int kind, double[] geometry) {
        this.kind = kind;
        this.geometry = geometry;
    }

    /**
     * Creates a linear gradient.
     * <p>
     * 선형 그라디언트를 만든다.
     *
     * @param x0 start x / 시작 x
     * @param y0 start y / 시작 y
     * @param x1 end x / 끝 x
     * @param y1 end y / 끝 y
     * @return the gradient / 그라디언트
     */
    public static CanvasGradient linear(double x0, double y0, double x1, double y1) {
        return new CanvasGradient(LINEAR, new double[] { x0, y0, 0, x1, y1, 0 });
    }

    /**
     * Creates a conic gradient: the colors go clockwise around a center, the offset 0 is at the start angle and the offset 1 is one full turn
     * later (the rule of CSS <code>conic-gradient(from startAngle + 90deg at x y, ...)</code>, HTML Standard 4.12.5.1.10).
     * <p>
     * 원뿔형 그라디언트를 만든다: 색이 중심 둘레를 시계 방향으로 돌며 오프셋 0 은 시작 각도에, 오프셋 1 은 한 바퀴 뒤에 있다(CSS
     * <code>conic-gradient(from startAngle + 90deg at x y, ...)</code> 의 규칙, HTML 표준 4.12.5.1.10).
     *
     * @param startAngle the angle in radians at which the gradient begins, measured clockwise from the positive x axis / 그라디언트가 시작하는 각도(라디안). 양의 x 축에서
     *                   시계 방향으로 잰다
     * @param x          center x / 중심 x
     * @param y          center y / 중심 y
     * @return the gradient / 그라디언트
     */
    public static CanvasGradient conic(double startAngle, double x, double y) {
        return new CanvasGradient(CONIC, new double[] { x, y, startAngle, 0, 0, 0 });
    }

    /**
     * Creates a radial gradient between two circles.
     * <p>
     * 두 원 사이의 방사형 그라디언트를 만든다.
     *
     * @param x0 start circle x / 시작 원 x
     * @param y0 start circle y / 시작 원 y
     * @param r0 start circle radius, not negative / 시작 원 반지름(음수 불가)
     * @param x1 end circle x / 끝 원 x
     * @param y1 end circle y / 끝 원 y
     * @param r1 end circle radius, not negative / 끝 원 반지름(음수 불가)
     * @return the gradient / 그라디언트
     */
    public static CanvasGradient radial(double x0, double y0, double r0, double x1, double y1, double r1) {
        return new CanvasGradient(RADIAL, new double[] { x0, y0, r0, x1, y1, r1 });
    }

    /**
     * Adds a color stop; a stop is placed after the existing stops with the same or a smaller offset.
     * <p>
     * 색 정지점을 더한다. 정지점은 오프셋이 같거나 작은 기존 정지점 뒤에 놓인다.
     *
     * @param offset the offset 0..1 (the caller checked the range) / 오프셋 0..1(범위는 호출자가 확인한다)
     * @param argb   the color / 색
     */
    public synchronized void addColorStop(double offset, int argb) {
        int at = offsets.size();
        // 뒤에서부터 오프셋이 더 큰 정지점의 앞에 들어갈 위치를 찾는다.
        while (at > 0 && offsets.get(at - 1).doubleValue() > offset) {
            at--;
        }
        offsets.add(at, Double.valueOf(offset));
        colors.add(at, Integer.valueOf(argb));
    }

    /**
     * Returns the number of color stops.
     * <p>
     * 색 정지점의 수를 반환한다.
     *
     * @return the count / 개수
     */
    public synchronized int getStopCount() {
        return offsets.size();
    }

    /**
     * Returns the color at a position of the gradient line (after the pad rule), for tests and diagnostics.
     * <p>
     * 그라디언트 선의 한 위치의 색을 반환한다(패드 규칙 적용 뒤). 시험과 진단용.
     *
     * @param t the position / 위치
     * @return the ARGB color / ARGB 색
     */
    public synchronized int colorAt(double t) {
        return colorAt(t, snapshotOffsets(), snapshotColors());
    }

    /**
     * Copies the offsets into an array.
     * <p>
     * 오프셋을 배열로 복사한다.
     *
     * @return the offsets / 오프셋
     */
    private double[] snapshotOffsets() {
        double[] o = new double[offsets.size()];
        // 오프셋을 하나씩 배열에 옮긴다.
        for (int i = 0; i < o.length; i++) {
            o[i] = offsets.get(i).doubleValue();
        }
        return o;
    }

    /**
     * Copies the colors into an array.
     * <p>
     * 색을 배열로 복사한다.
     *
     * @return the colors / 색
     */
    private int[] snapshotColors() {
        int[] c = new int[colors.size()];
        // 색을 하나씩 배열에 옮긴다.
        for (int i = 0; i < c.length; i++) {
            c[i] = colors.get(i).intValue();
        }
        return c;
    }

    /**
     * Computes the color at a position from the stop arrays.
     * <p>
     * 정지점 배열로 한 위치의 색을 계산한다.
     *
     * @param t   the position / 위치
     * @param off the offsets / 오프셋
     * @param col the colors / 색
     * @return the ARGB color / ARGB 색
     */
    static int colorAt(double t, double[] off, int[] col) {
        int n = off.length;
        if (n == 0) {
            return 0;
        }
        if (t < off[0] || n == 1) {
            return col[0];
        }
        // 위치가 속하는 구간을 찾아 양쪽 정지점 사이를 보간한다.
        for (int i = 0; i < n - 1; i++) {
            if (t < off[i + 1]) {
                double f = (t - off[i]) / (off[i + 1] - off[i]);
                return lerp(col[i], col[i + 1], f);
            }
        }
        return col[n - 1];
    }

    /**
     * Interpolates two colors channel by channel in non-premultiplied RGBA.
     * <p>
     * 두 색을 알파를 곱하지 않은 RGBA 로 채널별 보간한다.
     *
     * @param a the first color / 첫 색
     * @param b the second color / 둘째 색
     * @param f the fraction 0..1 / 비율 0..1
     * @return the color / 색
     */
    static int lerp(int a, int b, double f) {
        int out = 0;
        // 네 채널(알파, 빨강, 초록, 파랑)을 하나씩 보간한다.
        for (int shift = 24; shift >= 0; shift -= 8) {
            double ca = (a >>> shift) & 0xFF;
            double cb = (b >>> shift) & 0xFF;
            int v = (int) Math.round(ca + (cb - ca) * f);
            out |= Math.max(0, Math.min(255, v)) << shift;
        }
        return out;
    }

    /**
     * Creates the Java2D paint for the coordinate space of a transformation matrix.
     * <p>
     * 변환 행렬의 좌표계에 맞는 Java2D 페인트를 만든다.
     *
     * @param ctm the current transformation matrix (gradient space to device) / 현재 변환 행렬(그라디언트 좌표계에서 장치로)
     * @return the paint, or null if the matrix cannot be inverted (nothing is painted) / 페인트. 행렬을 뒤집을 수 없으면 null(아무것도 칠하지 않음)
     */
    public synchronized Paint toPaint(AffineTransform ctm) {
        AffineTransform inverse;
        try {
            inverse = ctm.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new GradientPaint2D(kind, geometry.clone(), snapshotOffsets(), snapshotColors(), inverse);
    }

    /**
     * The Java2D paint that evaluates the gradient for every device pixel.
     * <p>
     * 장치 화소마다 그라디언트를 계산하는 Java2D 페인트.
     */
    private static final class GradientPaint2D implements Paint {
        /**
         * The kind of the gradient.
         * <p>
         * 그라디언트의 종류.
         */
        private final int kind;

        /**
         * The geometry (x0, y0, r0, x1, y1, r1).
         * <p>
         * 기하(x0, y0, r0, x1, y1, r1).
         */
        private final double[] g;

        /**
         * The stop offsets.
         * <p>
         * 정지점 오프셋.
         */
        private final double[] off;

        /**
         * The stop colors.
         * <p>
         * 정지점 색.
         */
        private final int[] col;

        /**
         * The device-to-gradient transform.
         * <p>
         * 장치에서 그라디언트 좌표계로의 변환.
         */
        private final AffineTransform inverse;

        /**
         * Creates the paint.
         * <p>
         * 페인트를 만든다.
         *
         * @param kind    the kind of the gradient / 그라디언트의 종류
         * @param g       the geometry / 기하
         * @param off     the offsets / 오프셋
         * @param col     the colors / 색
         * @param inverse device to gradient space / 장치에서 그라디언트 좌표계로
         */
        GradientPaint2D(int kind, double[] g, double[] off, int[] col, AffineTransform inverse) {
            this.kind = kind;
            this.g = g;
            this.off = off;
            this.col = col;
            this.inverse = inverse;
        }

        /**
         * Gradients are translucent in general.
         * <p>
         * 그라디언트는 일반적으로 반투명이다.
         *
         * @return TRANSLUCENT / TRANSLUCENT
         */
        public int getTransparency() {
            return java.awt.Transparency.TRANSLUCENT;
        }

        /**
         * Creates the context that computes the pixels.
         * <p>
         * 화소를 계산하는 컨텍스트를 만든다.
         *
         * @param cm           the color model of the destination / 대상의 색 모델
         * @param deviceBounds the device bounds / 장치 경계
         * @param userBounds   the user bounds / 사용자 경계
         * @param xform        the transform of the graphics / 그래픽스의 변환
         * @param hints        the hints / 힌트
         * @return the context / 컨텍스트
         */
        public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform,
                RenderingHints hints) {
            return new Context();
        }

        /**
         * The context: it fills a raster of non-premultiplied ARGB.
         * <p>
         * 컨텍스트: 알파를 곱하지 않은 ARGB 래스터를 채운다.
         */
        private final class Context implements PaintContext {
            /**
             * The color model of the rasters.
             * <p>
             * 래스터의 색 모델.
             */
            private final ColorModel model = ColorModel.getRGBdefault();

            /**
             * Nothing to release.
             * <p>
             * 해제할 것이 없다.
             */
            public void dispose() {
                return;
            }

            /**
             * Returns the color model of the rasters.
             * <p>
             * 래스터의 색 모델을 반환한다.
             *
             * @return the model / 모델
             */
            public ColorModel getColorModel() {
                return model;
            }

            /**
             * Fills a raster with the colors of the device pixels in a rectangle.
             * <p>
             * 사각형 안 장치 화소의 색으로 래스터를 채운다.
             *
             * @param x the left device pixel / 왼쪽 장치 화소
             * @param y the top device pixel / 위쪽 장치 화소
             * @param w the width / 너비
             * @param h the height / 높이
             * @return the raster / 래스터
             */
            public Raster getRaster(int x, int y, int w, int h) {
                WritableRaster raster = model.createCompatibleWritableRaster(w, h);
                int[] data = ((DataBufferInt) raster.getDataBuffer()).getData();
                double[] pt = new double[2];
                // 각 장치 화소의 중심을 그라디언트 좌표계로 옮겨 색을 계산한다.
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        pt[0] = x + i + 0.5;
                        pt[1] = y + j + 0.5;
                        inverse.transform(pt, 0, pt, 0, 1);
                        // 종류에 맞는 식으로 이 화소의 색을 계산한다.
                        if (kind == RADIAL) {
                            data[j * w + i] = radialColor(pt[0], pt[1]);
                        } else if (kind == CONIC) {
                            data[j * w + i] = conicColor(pt[0], pt[1]);
                        } else {
                            data[j * w + i] = linearColor(pt[0], pt[1]);
                        }
                    }
                }
                return raster;
            }

            /**
             * Computes the color of a point of a linear gradient.
             * <p>
             * 선형 그라디언트의 한 점의 색을 계산한다.
             *
             * @param px x in gradient space / 그라디언트 좌표계의 x
             * @param py y in gradient space / 그라디언트 좌표계의 y
             * @return the color / 색
             */
            private int linearColor(double px, double py) {
                double dx = g[3] - g[0];
                double dy = g[4] - g[1];
                double len2 = dx * dx + dy * dy;
                // 시작점과 끝점이 같으면 아무것도 칠하지 않는다.
                if (len2 == 0) {
                    return 0;
                }
                double t = ((px - g[0]) * dx + (py - g[1]) * dy) / len2;
                return colorAt(t, off, col);
            }

            /**
             * Computes the color of a point of a conic gradient: the position is the angle of the point around the center, measured clockwise
             * from the start angle, as a fraction of a full turn.
             * <p>
             * 원뿔형 그라디언트의 한 점의 색을 계산한다: 위치는 시작 각도에서 시계 방향으로 잰, 중심 둘레 점의 각도를 한 바퀴에 대한 비율로 나타낸 것이다.
             *
             * @param px x in gradient space / 그라디언트 좌표계의 x
             * @param py y in gradient space / 그라디언트 좌표계의 y
             * @return the color / 색
             */
            private int conicColor(double px, double py) {
                double turn = (Math.atan2(py - g[1], px - g[0]) - g[2]) / (2 * Math.PI);
                // 한 바퀴를 넘는 각도는 [0, 1) 로 접는다.
                double t = turn - Math.floor(turn);
                return colorAt(t, off, col);
            }

            /**
             * Computes the color of a point of a radial gradient with the two-circle rule.
             * <p>
             * 두 원 규칙으로 방사형 그라디언트의 한 점의 색을 계산한다.
             *
             * @param px x in gradient space / 그라디언트 좌표계의 x
             * @param py y in gradient space / 그라디언트 좌표계의 y
             * @return the color / 색
             */
            private int radialColor(double px, double py) {
                double cdx = g[3] - g[0];
                double cdy = g[4] - g[1];
                double dr = g[5] - g[2];
                double qx = px - g[0];
                double qy = py - g[1];
                double a = cdx * cdx + cdy * cdy - dr * dr;
                double b = qx * cdx + qy * cdy + g[2] * dr;
                double c = qx * qx + qy * qy - g[2] * g[2];
                double w;
                if (Math.abs(a) < 1e-12) {
                    // 이차항이 없으면 일차 방정식이다.
                    if (Math.abs(b) < 1e-12) {
                        return 0;
                    }
                    w = c / (2 * b);
                    if (g[2] + w * dr < 0) {
                        return 0;
                    }
                } else {
                    double disc = b * b - a * c;
                    if (disc < 0) {
                        return 0;
                    }
                    double root = Math.sqrt(disc);
                    double w1 = (b + root) / a;
                    double w2 = (b - root) / a;
                    double hi = Math.max(w1, w2);
                    double lo = Math.min(w1, w2);
                    // 반지름이 음수가 아닌 해 중 큰 쪽을 고른다.
                    if (g[2] + hi * dr >= 0) {
                        w = hi;
                    } else if (g[2] + lo * dr >= 0) {
                        w = lo;
                    } else {
                        return 0;
                    }
                }
                return colorAt(w, off, col);
            }
        }
    }
}
