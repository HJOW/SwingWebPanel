package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * The pixels of an image source that <code>drawImage()</code> and <code>createPattern()</code> read: a decoded <code>img</code>, another canvas
 * or a copy of the canvas itself. It holds the size, the non-premultiplied <code>0xAARRGGBB</code> pixels (read only; not copied unless the
 * caller says so) and whether the pixels are origin-clean. Drawing a source that is not origin-clean taints the canvas.
 * <p>
 * <code>drawImage()</code> 와 <code>createPattern()</code> 이 읽는 이미지 소스의 화소: 디코딩된 <code>img</code>, 다른 캔버스, 또는 캔버스 자신의
 * 복사본. 크기, 알파를 곱하지 않은 <code>0xAARRGGBB</code> 화소(읽기 전용. 호출자가 요구하지 않으면 복사하지 않는다), 화소가 origin-clean 인지 여부를
 * 담는다. origin-clean 이 아닌 소스를 그리면 캔버스가 오염된다.
 */
public final class CanvasImage {

    /**
     * The width in pixels.
     * <p>
     * 너비(화소).
     */
    private final int width;

    /**
     * The height in pixels.
     * <p>
     * 높이(화소).
     */
    private final int height;

    /**
     * The pixels (read only).
     * <p>
     * 화소(읽기 전용).
     */
    private final int[] pixels;

    /**
     * Whether the pixels came from the same origin (or were allowed by CORS).
     * <p>
     * 화소가 같은 오리진에서 왔는지(또는 CORS 로 허용되었는지) 여부.
     */
    private final boolean originClean;

    /**
     * Creates an image source.
     * <p>
     * 이미지 소스를 만든다.
     *
     * @param width       width in pixels / 너비(화소)
     * @param height      height in pixels / 높이(화소)
     * @param pixels      the pixels, <code>width * height</code> of them (the array is used, not copied) / 화소 <code>width * height</code> 개(배열을 복사하지 않고 쓴다)
     * @param originClean true if the pixels may be read back by scripts / 스크립트가 다시 읽어도 되는 화소이면 true
     */
    public CanvasImage(int width, int height, int[] pixels, boolean originClean) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.originClean = originClean;
    }

    /**
     * Returns the width.
     * <p>
     * 너비를 반환한다.
     *
     * @return width in pixels / 너비(화소)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     * <p>
     * 높이를 반환한다.
     *
     * @return height in pixels / 높이(화소)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the pixels (do not modify).
     * <p>
     * 화소를 반환한다(수정하지 말 것).
     *
     * @return the pixels / 화소
     */
    public int[] getPixels() {
        return pixels;
    }

    /**
     * Tells whether the pixels are origin-clean.
     * <p>
     * 화소가 origin-clean 인지 알려 준다.
     *
     * @return true if clean / clean 이면 true
     */
    public boolean isOriginClean() {
        return originClean;
    }

    /**
     * Wraps the pixels in a <code>BufferedImage</code> without copying them.
     * <p>
     * 화소를 복사하지 않고 <code>BufferedImage</code> 로 감싼다.
     *
     * @return the image / 이미지
     */
    BufferedImage toBufferedImage() {
        DataBufferInt db = new DataBufferInt(pixels, pixels.length);
        WritableRaster r = Raster.createPackedRaster(db, width, height, width, new int[] { 0xff0000, 0xff00, 0xff, 0xff000000 }, null);
        ColorModel cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32, 0xff0000, 0xff00, 0xff, 0xff000000, false,
                DataBuffer.TYPE_INT);
        return new BufferedImage(cm, r, false, null);
    }
}
