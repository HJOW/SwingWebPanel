package org.duckdns.hjow.swingwebpanel.layout;

/**
 * The alpha channel of an image that gives the float area of a float whose <code>shape-outside</code> is an image (CSS Shapes Module Level 1
 * section 4 "Shapes from Image", https://www.w3.org/TR/css-shapes-1/#shapes-from-image): the width and the height in image pixels and one
 * alpha value from 0 (transparent) to 255 (opaque) for every pixel in row order. The layout reads only this mask, never the colors, and the
 * mask is immutable once it was built.
 * <p>
 * <code>shape-outside</code> 가 이미지인 float 의 float 영역을 주는 이미지의 알파 채널(CSS Shapes Module Level 1 4 절 "Shapes from Image",
 * https://www.w3.org/TR/css-shapes-1/#shapes-from-image): 이미지 픽셀 단위의 너비와 높이, 행 순서로 놓인 픽셀마다의 알파 값(0 투명 ~ 255 불투명). 레이아웃은 이
 * 마스크만 읽고 색은 읽지 않으며, 만든 뒤에는 바뀌지 않는다.
 */
public final class ShapeImage {

    /** The width in pixels. 너비(픽셀). */
    private final int width;
    /** The height in pixels. 높이(픽셀). */
    private final int height;
    /** The alpha values, row by row. 행 순서의 알파 값. */
    private final byte[] alpha;

    /**
     * Creates the mask (the array is used as it is, so the caller must not change it afterwards).
     * <p>
     * 마스크를 만든다(배열을 그대로 쓰므로 호출자는 이후에 바꾸면 안 된다).
     *
     * @param width  the width in pixels (at least 1) / 너비(픽셀, 1 이상)
     * @param height the height in pixels (at least 1) / 높이(픽셀, 1 이상)
     * @param alpha  <code>width * height</code> alpha values, row by row / 행 순서의 <code>width * height</code> 개의 알파 값
     * @throws IllegalArgumentException if the size is not positive or the array has another length / 크기가 양수가 아니거나 배열 길이가 다르면
     */
    public ShapeImage(int width, int height, byte[] alpha) {
        if (width < 1 || height < 1 || alpha == null || alpha.length != (long) width * height) {
            throw new IllegalArgumentException("invalid shape image " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.alpha = alpha;
    }

    /**
     * Returns the width.
     * <p>
     * 너비를 반환한다.
     *
     * @return pixels / 픽셀
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * <p>
     * 높이를 반환한다.
     *
     * @return pixels / 픽셀
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the alpha of a pixel.
     * <p>
     * 픽셀의 알파를 반환한다.
     *
     * @param x the column / 열
     * @param y the row / 행
     * @return the alpha from 0 to 255 / 0~255 의 알파
     */
    public int alphaAt(int x, int y) {
        return alpha[y * width + x] & 0xFF;
    }
}
