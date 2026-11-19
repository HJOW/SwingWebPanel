package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * The pixel work of <code>createImageBitmap()</code> (HTML Standard 8.11.2 "crop bitmap data to the source rectangle with formatting"): the
 * source picture is placed on an infinite transparent grid, the source rectangle is cut out of it (pixels that the picture does not have are
 * transparent black), the cut is scaled to the output size with the interpolation that the <code>resizeQuality</code> asks for, and it is flipped
 * vertically when <code>imageOrientation</code> is <code>flipY</code>. The output size is computed as the specification says: the
 * <code>resizeWidth</code> and <code>resizeHeight</code> of the options, one of them derived from the other by the aspect ratio of the source
 * rectangle (rounded up) when only one is given, and the size of the source rectangle otherwise. Java2D public API only.
 * <p>
 * <code>createImageBitmap()</code> 의 화소 작업(HTML 표준 8.11.2 "crop bitmap data to the source rectangle with formatting"): 원본 그림을 무한한 투명 격자에
 * 놓고, 거기서 원본 사각형을 잘라 내고(그림에 없는 화소는 투명한 검정), 잘라 낸 것을 <code>resizeQuality</code> 가 요구하는 보간으로 출력 크기에 맞게 늘이거나
 * 줄이며, <code>imageOrientation</code> 이 <code>flipY</code> 이면 세로로 뒤집는다. 출력 크기는 규격대로 계산한다: 옵션의 <code>resizeWidth</code>,
 * <code>resizeHeight</code>, 하나만 주어지면 원본 사각형의 가로세로 비율로 다른 하나를 구하고(올림), 둘 다 없으면 원본 사각형의 크기이다. Java2D 공개 API 만 쓴다.
 */
final class BitmapOps {

    /**
     * The quality <code>pixelated</code>: nearest neighbour.
     * <p>
     * 품질 <code>pixelated</code>: 최근접.
     */
    static final int PIXELATED = 0;

    /**
     * The quality <code>low</code>: bilinear.
     * <p>
     * 품질 <code>low</code>: 쌍선형.
     */
    static final int LOW = 1;

    /**
     * The quality <code>medium</code>: bilinear.
     * <p>
     * 품질 <code>medium</code>: 쌍선형.
     */
    static final int MEDIUM = 2;

    /**
     * The quality <code>high</code>: bicubic.
     * <p>
     * 품질 <code>high</code>: 쌍삼차.
     */
    static final int HIGH = 3;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BitmapOps() {
    }

    /**
     * Returns the number that the <code>resizeQuality</code> enumeration value stands for.
     * <p>
     * <code>resizeQuality</code> 열거 값이 뜻하는 숫자를 반환한다.
     *
     * @param name the enumeration value / 열거 값
     * @return {@link #PIXELATED}, {@link #LOW}, {@link #MEDIUM} or {@link #HIGH}, or -1 if the value is not one of them / 열거 값이 아니면 -1
     */
    static int quality(String name) {
        if ("pixelated".equals(name)) {
            return PIXELATED;
        } else if ("low".equals(name)) {
            return LOW;
        } else if ("medium".equals(name)) {
            return MEDIUM;
        }
        return "high".equals(name) ? HIGH : -1;
    }

    /**
     * Computes the output width: the <code>resizeWidth</code> if there is one, else derived from the <code>resizeHeight</code> by the aspect ratio
     * of the source rectangle and rounded up, else the width of the source rectangle.
     * <p>
     * 출력 너비를 계산한다: <code>resizeWidth</code> 가 있으면 그것, 없고 <code>resizeHeight</code> 가 있으면 원본 사각형의 가로세로 비율로 구해 올림한 값,
     * 둘 다 없으면 원본 사각형의 너비.
     *
     * @param sw            the width of the source rectangle / 원본 사각형의 너비
     * @param sh            the height of the source rectangle / 원본 사각형의 높이
     * @param resizeWidth   the requested width, or -1 / 요청한 너비. 없으면 -1
     * @param resizeHeight  the requested height, or -1 / 요청한 높이. 없으면 -1
     * @return the width in pixels / 너비(화소)
     */
    static long outputWidth(double sw, double sh, long resizeWidth, long resizeHeight) {
        if (resizeWidth >= 0) {
            return resizeWidth;
        }
        if (resizeHeight >= 0) {
            return (long) Math.ceil(sw * resizeHeight / sh);
        }
        return (long) Math.ceil(sw);
    }

    /**
     * Computes the output height (the mirror image of {@link #outputWidth}).
     * <p>
     * 출력 높이를 계산한다({@link #outputWidth} 의 짝).
     *
     * @param sw            the width of the source rectangle / 원본 사각형의 너비
     * @param sh            the height of the source rectangle / 원본 사각형의 높이
     * @param resizeWidth   the requested width, or -1 / 요청한 너비. 없으면 -1
     * @param resizeHeight  the requested height, or -1 / 요청한 높이. 없으면 -1
     * @return the height in pixels / 높이(화소)
     */
    static long outputHeight(double sw, double sh, long resizeWidth, long resizeHeight) {
        if (resizeHeight >= 0) {
            return resizeHeight;
        }
        if (resizeWidth >= 0) {
            return (long) Math.ceil(sh * resizeWidth / sw);
        }
        return (long) Math.ceil(sh);
    }

    /**
     * Cuts a rectangle out of a picture and scales it to a size.
     * <p>
     * 그림에서 사각형을 잘라 내 크기에 맞게 늘이거나 줄인다.
     *
     * @param src     the source picture / 원본 그림
     * @param sx      the left of the source rectangle (may lie outside the picture) / 원본 사각형의 왼쪽(그림 밖일 수 있음)
     * @param sy      the top of the source rectangle / 원본 사각형의 위쪽
     * @param sw      the width of the source rectangle, positive / 원본 사각형의 너비. 양수
     * @param sh      the height of the source rectangle, positive / 원본 사각형의 높이. 양수
     * @param outW    the output width, at least 1 / 출력 너비. 1 이상
     * @param outH    the output height, at least 1 / 출력 높이. 1 이상
     * @param flipY   true to flip the output vertically / 출력을 세로로 뒤집으면 true
     * @param quality the interpolation, one of the quality constants / 보간. 품질 상수 중 하나
     * @return the pixels of the output, non-premultiplied ARGB / 출력 화소. 알파를 곱하지 않은 ARGB
     */
    static int[] render(CanvasImage src, double sx, double sy, double sw, double sh, int outW, int outH, boolean flipY, int quality) {
        BufferedImage dst = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        int[] out = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        Graphics2D g = dst.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            Object hint;
            // 품질에 맞는 보간 방법을 고른다.
            if (quality == PIXELATED) {
                hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            } else if (quality == HIGH) {
                hint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
            } else {
                hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            AffineTransform t = new AffineTransform();
            // flipY 이면 출력의 세로 방향을 뒤집는다.
            if (flipY) {
                t.translate(0, outH);
                t.scale(1, -1);
            }
            t.scale(outW / sw, outH / sh);
            t.translate(-sx, -sy);
            g.drawImage(src.toBufferedImage(), t, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
