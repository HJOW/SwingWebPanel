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

/**
 * A <code>CanvasPattern</code>: a snapshot of an image that is repeated in x, in y, in both or in neither direction. Like a gradient it lives in
 * the coordinate space that is current when it is painted. The pixels are copied when the pattern is created, so later changes of the source
 * canvas do not change the pattern. A pattern made from an image that is not origin-clean taints every canvas that paints with it.
 * <p>
 * <code>CanvasPattern</code>: x 방향, y 방향, 양방향 또는 반복 없이 되풀이되는 이미지의 스냅샷. 그라디언트처럼 칠할 때의 좌표계에 놓인다. 화소는 패턴을
 * 만들 때 복사하므로 원본 캔버스가 나중에 바뀌어도 패턴은 바뀌지 않는다. origin-clean 이 아닌 이미지로 만든 패턴은 그것으로 칠하는 모든 캔버스를
 * 오염시킨다.
 */
public final class CanvasPattern {

    /**
     * The width of the tile in pixels.
     * <p>
     * 타일 너비(화소).
     */
    private final int width;

    /**
     * The height of the tile in pixels.
     * <p>
     * 타일 높이(화소).
     */
    private final int height;

    /**
     * The pixels of the tile (non-premultiplied ARGB), a private copy.
     * <p>
     * 타일의 화소(알파를 곱하지 않은 ARGB). 비공개 복사본.
     */
    private final int[] pixels;

    /**
     * Whether the tile repeats horizontally.
     * <p>
     * 타일이 가로로 반복되는지 여부.
     */
    private final boolean repeatX;

    /**
     * Whether the tile repeats vertically.
     * <p>
     * 타일이 세로로 반복되는지 여부.
     */
    private final boolean repeatY;

    /**
     * Whether the source was origin-clean.
     * <p>
     * 원본이 origin-clean 이었는지 여부.
     */
    private final boolean originClean;

    /**
     * The transformation of the pattern itself, applied in the pattern space (the identity until <code>setTransform()</code>).
     * <p>
     * 패턴 자신의 변환. 패턴 좌표계에 적용된다(<code>setTransform()</code> 전에는 단위 행렬).
     */
    private volatile AffineTransform patternTransform = new AffineTransform();

    /**
     * Creates a pattern.
     * <p>
     * 패턴을 만든다.
     *
     * @param width       tile width, positive / 타일 너비(양수)
     * @param height      tile height, positive / 타일 높이(양수)
     * @param pixels      the tile pixels (copied) / 타일 화소(복사한다)
     * @param repeatX     repeat horizontally / 가로 반복
     * @param repeatY     repeat vertically / 세로 반복
     * @param originClean true if the source had no foreign pixels / 원본에 외부 화소가 없었으면 true
     */
    public CanvasPattern(int width, int height, int[] pixels, boolean repeatX, boolean repeatY, boolean originClean) {
        this.width = width;
        this.height = height;
        this.pixels = pixels.clone();
        this.repeatX = repeatX;
        this.repeatY = repeatY;
        this.originClean = originClean;
    }

    /**
     * Tells whether the source image was origin-clean.
     * <p>
     * 원본 이미지가 origin-clean 이었는지 알려 준다.
     *
     * @return true if painting with this pattern does not taint a canvas / 이 패턴으로 칠해도 캔버스가 오염되지 않으면 true
     */
    public boolean isOriginClean() {
        return originClean;
    }

    /**
     * Creates the Java2D paint for the coordinate space of a transformation matrix.
     * <p>
     * 변환 행렬의 좌표계에 맞는 Java2D 페인트를 만든다.
     *
     * @param ctm       the current transformation matrix (pattern space to device) / 현재 변환 행렬(패턴 좌표계에서 장치로)
     * @param smoothing true for bilinear sampling, false for nearest neighbour / 쌍선형 샘플링이면 true, 최근접이면 false
     * @return the paint, or null if the matrix cannot be inverted / 페인트. 행렬을 뒤집을 수 없으면 null
     */
    public Paint toPaint(AffineTransform ctm, boolean smoothing) {
        // 패턴 좌표계에서 장치까지는 (칠할 때의 변환 x 패턴 자신의 변환)이다.
        AffineTransform full = new AffineTransform(ctm);
        full.concatenate(patternTransform);
        double[] f = new double[6];
        full.getMatrix(f);
        // 무한대나 NaN 이 섞인 변환으로는 칠하지 않는다.
        for (int i = 0; i < 6; i++) {
            if (Double.isNaN(f[i]) || Double.isInfinite(f[i])) {
                return null;
            }
        }
        AffineTransform inverse;
        try {
            inverse = full.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
        return new PatternPaint(inverse, smoothing);
    }

    /**
     * Sets the transformation of the pattern itself (<code>CanvasPattern.setTransform()</code>): it is applied to the pattern space before the
     * current transformation matrix of the painting, so it moves, scales or rotates the tiles. The initial value is the identity. A matrix
     * with a NaN or infinite element is stored but nothing is painted with it.
     * <p>
     * 패턴 자신의 변환을 정한다(<code>CanvasPattern.setTransform()</code>): 칠할 때의 현재 변환 행렬보다 먼저 패턴 좌표계에 적용되므로 타일을 옮기고, 키우고,
     * 돌린다. 처음 값은 단위 행렬이다. NaN 이나 무한대 요소가 있는 행렬은 저장하지만 그것으로는 아무것도 칠하지 않는다.
     *
     * @param a m11 / m11
     * @param b m12 / m12
     * @param c m21 / m21
     * @param d m22 / m22
     * @param e m41 / m41
     * @param f m42 / m42
     */
    public void setPatternTransform(double a, double b, double c, double d, double e, double f) {
        patternTransform = new AffineTransform(a, b, c, d, e, f);
    }

    /**
     * Reads a tile pixel, applying the repeat rule: outside a direction that does not repeat the pixel is transparent.
     * <p>
     * 타일 화소를 읽는다. 반복 규칙을 적용하며, 반복하지 않는 방향의 바깥은 투명하다.
     *
     * @param x column (any integer) / 열(임의의 정수)
     * @param y row (any integer) / 행(임의의 정수)
     * @return the ARGB pixel / ARGB 화소
     */
    private int pixel(int x, int y) {
        int px = x;
        int py = y;
        if (repeatX) {
            px = ((x % width) + width) % width;
        } else if (x < 0 || x >= width) {
            return 0;
        }
        if (repeatY) {
            py = ((y % height) + height) % height;
        } else if (y < 0 || y >= height) {
            return 0;
        }
        return pixels[py * width + px];
    }

    /**
     * Samples the tile at a position with bilinear interpolation of premultiplied colors.
     * <p>
     * 타일의 한 위치를 알파를 곱한 색의 쌍선형 보간으로 샘플링한다.
     *
     * @param u horizontal position in tile pixels / 가로 위치(타일 화소)
     * @param v vertical position in tile pixels / 세로 위치(타일 화소)
     * @return the ARGB color / ARGB 색
     */
    private int bilinear(double u, double v) {
        double fu = u - 0.5;
        double fv = v - 0.5;
        int x0 = (int) Math.floor(fu);
        int y0 = (int) Math.floor(fv);
        double tx = fu - x0;
        double ty = fv - y0;
        double a = 0;
        double r = 0;
        double g = 0;
        double b = 0;
        // 둘러싼 네 화소를 가중치대로 알파를 곱한 값으로 더한다.
        for (int k = 0; k < 4; k++) {
            int dx = k & 1;
            int dy = k >> 1;
            double weight = (dx == 0 ? 1 - tx : tx) * (dy == 0 ? 1 - ty : ty);
            if (weight <= 0) {
                continue;
            }
            int p = pixel(x0 + dx, y0 + dy);
            double pa = ((p >>> 24) & 0xFF) / 255.0;
            a += weight * pa;
            r += weight * pa * ((p >> 16) & 0xFF);
            g += weight * pa * ((p >> 8) & 0xFF);
            b += weight * pa * (p & 0xFF);
        }
        if (a <= 0) {
            return 0;
        }
        int ia = (int) Math.round(a * 255);
        int ir = (int) Math.round(r / a);
        int ig = (int) Math.round(g / a);
        int ib = (int) Math.round(b / a);
        return (Math.min(255, ia) << 24) | (Math.min(255, ir) << 16) | (Math.min(255, ig) << 8) | Math.min(255, ib);
    }

    /**
     * The Java2D paint that samples the tile for every device pixel.
     * <p>
     * 장치 화소마다 타일을 샘플링하는 Java2D 페인트.
     */
    private final class PatternPaint implements Paint {
        /**
         * The device-to-pattern transform.
         * <p>
         * 장치에서 패턴 좌표계로의 변환.
         */
        private final AffineTransform inverse;

        /**
         * Bilinear or nearest neighbour.
         * <p>
         * 쌍선형인지 최근접인지.
         */
        private final boolean smoothing;

        /**
         * Creates the paint.
         * <p>
         * 페인트를 만든다.
         *
         * @param inverse   device to pattern space / 장치에서 패턴 좌표계로
         * @param smoothing bilinear sampling / 쌍선형 샘플링
         */
        PatternPaint(AffineTransform inverse, boolean smoothing) {
            this.inverse = inverse;
            this.smoothing = smoothing;
        }

        /**
         * Patterns are translucent in general.
         * <p>
         * 패턴은 일반적으로 반투명이다.
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
                // 각 장치 화소의 중심을 패턴 좌표계로 옮겨 타일을 샘플링한다.
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        pt[0] = x + i + 0.5;
                        pt[1] = y + j + 0.5;
                        inverse.transform(pt, 0, pt, 0, 1);
                        data[j * w + i] = smoothing ? bilinear(pt[0], pt[1]) : pixel((int) Math.floor(pt[0]), (int) Math.floor(pt[1]));
                    }
                }
                return raster;
            }
        }
    }
}
