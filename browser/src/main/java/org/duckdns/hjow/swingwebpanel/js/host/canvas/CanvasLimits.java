package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * The size limits of canvas bitmaps and the default size of a <code>canvas</code> element. A bitmap larger than {@link #MAX_DIMENSION} on a
 * side, or with more than {@link #MAX_PIXELS} pixels, is not allocated: such a canvas still has a 2D context but drawing on it is ignored,
 * <code>getImageData</code> throws <code>InvalidStateError</code> and <code>toDataURL</code> answers <code>"data:,"</code>. The layout still
 * uses the width and height of the attributes.
 * <p>
 * 캔버스 비트맵의 크기 한도와 <code>canvas</code> 요소의 기본 크기. 한 변이 {@link #MAX_DIMENSION} 보다 크거나 화소가 {@link #MAX_PIXELS} 보다
 * 많은 비트맵은 할당하지 않는다: 이런 캔버스에도 2D 컨텍스트는 있지만 그리기는 무시되고, <code>getImageData</code> 는
 * <code>InvalidStateError</code> 를 던지며, <code>toDataURL</code> 은 <code>"data:,"</code> 로 답한다. 레이아웃은 속성의 너비와 높이를 그대로 쓴다.
 */
public final class CanvasLimits {

    /**
     * The default width of a canvas in pixels.
     * <p>
     * 캔버스의 기본 너비(화소).
     */
    public static final int DEFAULT_WIDTH = 300;

    /**
     * The default height of a canvas in pixels.
     * <p>
     * 캔버스의 기본 높이(화소).
     */
    public static final int DEFAULT_HEIGHT = 150;

    /**
     * The largest width or height of a bitmap in pixels.
     * <p>
     * 비트맵 너비 또는 높이의 최댓값(화소).
     */
    public static final int MAX_DIMENSION = 16384;

    /**
     * The largest number of pixels of a bitmap (4096 x 4096; 4 bytes per pixel, so 64 MiB for the bitmap and the same again for a snapshot).
     * <p>
     * 비트맵 화소 수의 최댓값(4096 x 4096. 화소당 4바이트이므로 비트맵 64 MiB 와 스냅샷 하나에 같은 양이 든다).
     */
    public static final long MAX_PIXELS = 4096L * 4096L;

    /**
     * The largest number of pixels of an <code>ImageData</code> that a script creates (the same as a bitmap).
     * <p>
     * 스크립트가 만드는 <code>ImageData</code> 화소 수의 최댓값(비트맵과 같다).
     */
    public static final long MAX_IMAGE_DATA_PIXELS = MAX_PIXELS;

    /**
     * The largest blur that a shadow uses; larger values are clamped to keep the cost bounded.
     * <p>
     * 그림자가 쓰는 최대 번짐. 비용을 제한하려고 더 큰 값은 이 값으로 줄인다.
     */
    public static final double MAX_SHADOW_BLUR = 200;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private CanvasLimits() {
    }

    /**
     * Tells whether a bitmap of a size may be allocated.
     * <p>
     * 어떤 크기의 비트맵을 할당할 수 있는지 알려 준다.
     *
     * @param width  width in pixels / 너비(화소)
     * @param height height in pixels / 높이(화소)
     * @return true if both are within {@link #MAX_DIMENSION} and the product within {@link #MAX_PIXELS} / 둘 다 {@link #MAX_DIMENSION} 이하이고 곱이 {@link #MAX_PIXELS} 이하이면 true
     */
    public static boolean fits(long width, long height) {
        return width >= 0 && height >= 0 && width <= MAX_DIMENSION && height <= MAX_DIMENSION && width * height <= MAX_PIXELS;
    }
}
