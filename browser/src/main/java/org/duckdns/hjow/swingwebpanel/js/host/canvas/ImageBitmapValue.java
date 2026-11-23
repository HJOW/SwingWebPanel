package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * An <code>ImageBitmap</code> value (HTML Standard 8.11.2): an immutable picture, a copy of the pixels of its source (an image, a canvas, an
 * <code>ImageData</code>, a <code>Blob</code> or another bitmap) cropped, scaled and flipped as <code>createImageBitmap()</code> asked, together
 * with the origin-clean flag that taints a canvas which draws it. {@link #close()} releases the pixels: a closed bitmap has the size 0 and cannot
 * be drawn, copied or transferred. The pixels are never changed after the bitmap was made, so a copy to another script context shares the array.
 * <p>
 * <code>ImageBitmap</code> 값(HTML 표준 8.11.2): 바뀌지 않는 그림. 원본(이미지, 캔버스, <code>ImageData</code>, <code>Blob</code>, 다른 비트맵)의 화소를
 * <code>createImageBitmap()</code> 이 요청한 대로 자르고 늘이고 뒤집은 복사본이며, 그것을 그리는 캔버스를 오염시키는 origin-clean 표시를 함께 담는다.
 * {@link #close()} 는 화소를 해제한다: 닫힌 비트맵은 크기가 0 이고 그리거나 복사하거나 전달할 수 없다. 화소는 비트맵이 만들어진 뒤 바뀌지 않으므로
 * 다른 스크립트 문맥으로의 복사본은 배열을 공유한다.
 */
public final class ImageBitmapValue {

    /**
     * The pixels, or null once the bitmap is closed.
     * <p>
     * 화소. 비트맵이 닫혔으면 null.
     */
    private volatile CanvasImage image;

    /**
     * Creates a bitmap over pixels.
     * <p>
     * 화소 위의 비트맵을 만든다.
     *
     * @param image the pixels, not null (used, not copied) / 화소. null 불가(복사하지 않고 쓴다)
     */
    public ImageBitmapValue(CanvasImage image) {
        this.image = image;
    }

    /**
     * Returns the width in pixels, 0 for a closed bitmap.
     * <p>
     * 너비(화소)를 반환한다. 닫힌 비트맵은 0.
     *
     * @return the width / 너비
     */
    public int getWidth() {
        CanvasImage i = image;
        return i == null ? 0 : i.getWidth();
    }

    /**
     * Returns the height in pixels, 0 for a closed bitmap.
     * <p>
     * 높이(화소)를 반환한다. 닫힌 비트맵은 0.
     *
     * @return the height / 높이
     */
    public int getHeight() {
        CanvasImage i = image;
        return i == null ? 0 : i.getHeight();
    }

    /**
     * Returns the pixels, or null once the bitmap is closed.
     * <p>
     * 화소를 반환한다. 비트맵이 닫혔으면 null.
     *
     * @return the pixels or null / 화소 또는 null
     */
    CanvasImage image() {
        return image;
    }

    /**
     * Tells whether the bitmap was closed.
     * <p>
     * 비트맵이 닫혔는지 알려 준다.
     *
     * @return true after {@link #close()} or a transfer / {@link #close()} 나 전달 뒤에는 true
     */
    public boolean isClosed() {
        return image == null;
    }

    /**
     * Releases the pixels (<code>ImageBitmap.close()</code>); closing twice does nothing.
     * <p>
     * 화소를 해제한다(<code>ImageBitmap.close()</code>). 두 번 닫아도 아무 일도 없다.
     */
    public void close() {
        image = null;
    }
}
