package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;

/**
 * The pixel store of a canvas: <code>width * height</code> pixels of non-premultiplied <code>0xAARRGGBB</code> in one <code>int[]</code>,
 * a <code>BufferedImage</code> of type <code>TYPE_INT_ARGB</code> that shares that array, and the <code>Graphics2D</code> of the image (created
 * once, configured for antialiasing). It is plain Java2D: nothing uses the GPU or a native surface. A bitmap of width or height 0, or one that
 * exceeds {@link CanvasLimits}, is "empty": it has no pixels, no image and no graphics. All methods must be called while holding the monitor of
 * the owner ({@link CanvasSurface}); the class itself is not synchronized.
 * <p>
 * 캔버스의 화소 저장소: 알파를 미리 곱하지 않은 <code>0xAARRGGBB</code> 형식의 <code>width * height</code> 화소를 담은 <code>int[]</code> 하나,
 * 그 배열을 공유하는 <code>TYPE_INT_ARGB</code> 형식의 <code>BufferedImage</code>, 그 이미지의 <code>Graphics2D</code>(한 번 만들고 안티앨리어싱으로
 * 설정). 순수 Java2D 이며 GPU 나 네이티브 표면을 쓰지 않는다. 너비나 높이가 0 이거나 {@link CanvasLimits} 를 넘는 비트맵은 "비어 있다": 화소,
 * 이미지, 그래픽스가 없다. 모든 메서드는 소유자({@link CanvasSurface})의 모니터를 잡고 불러야 하며 이 클래스는 동기화하지 않는다.
 */
public final class CanvasBitmap {

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
     * The pixels, row by row, or an empty array for an empty bitmap.
     * <p>
     * 행 순서의 화소. 비어 있는 비트맵이면 빈 배열.
     */
    private final int[] pixels;

    /**
     * The image over {@link #pixels}, or null for an empty bitmap.
     * <p>
     * {@link #pixels} 위의 이미지. 비어 있는 비트맵이면 null.
     */
    private final BufferedImage image;

    /**
     * The graphics of {@link #image}, or null.
     * <p>
     * {@link #image} 의 그래픽스. 없으면 null.
     */
    private Graphics2D graphics;

    /**
     * Creates a bitmap whose pixels are transparent black.
     * <p>
     * 화소가 투명한 검정인 비트맵을 만든다.
     *
     * @param width  width in pixels / 너비(화소)
     * @param height height in pixels / 높이(화소)
     */
    public CanvasBitmap(int width, int height) {
        // 크기가 한도 안이고 비어 있지 않을 때만 화소를 할당한다.
        if (width > 0 && height > 0 && CanvasLimits.fits(width, height)) {
            this.width = width;
            this.height = height;
            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        } else {
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
            this.image = null;
            this.pixels = new int[0];
        }
    }

    /**
     * Returns the width of the bitmap that was asked for (also for an empty bitmap).
     * <p>
     * 요청한 비트맵의 너비를 반환한다(비어 있는 비트맵도 마찬가지).
     *
     * @return width in pixels / 너비(화소)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the bitmap that was asked for (also for an empty bitmap).
     * <p>
     * 요청한 비트맵의 높이를 반환한다(비어 있는 비트맵도 마찬가지).
     *
     * @return height in pixels / 높이(화소)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Tells whether the bitmap has no pixels (size 0 or beyond the limits).
     * <p>
     * 비트맵에 화소가 없는지(크기 0 이거나 한도 초과) 알려 준다.
     *
     * @return true if empty / 비어 있으면 true
     */
    public boolean isEmpty() {
        return image == null;
    }

    /**
     * Tells whether the bitmap was refused because it is too large (as opposed to having a zero size).
     * <p>
     * 비트맵이 너무 커서 거부되었는지(크기가 0 인 경우와 구별) 알려 준다.
     *
     * @return true if too large / 너무 크면 true
     */
    public boolean isTooLarge() {
        return image == null && width > 0 && height > 0;
    }

    /**
     * Returns the pixel array itself (not a copy).
     * <p>
     * 화소 배열 자체를 반환한다(복사본이 아님).
     *
     * @return the pixels / 화소
     */
    public int[] pixels() {
        return pixels;
    }

    /**
     * Returns the image that shares the pixels.
     * <p>
     * 화소를 공유하는 이미지를 반환한다.
     *
     * @return the image or null if empty / 이미지. 비어 있으면 null
     */
    public BufferedImage image() {
        return image;
    }

    /**
     * Returns the graphics of the bitmap with default rendering hints: antialiasing on, pure stroking, bilinear interpolation. The caller
     * sets the transform, clip and composite it needs and resets them afterwards.
     * <p>
     * 비트맵의 그래픽스를 기본 렌더링 힌트(안티앨리어싱 켬, 순수 선 긋기, 쌍선형 보간)로 반환한다. 호출자는 필요한 변환, 클립, 합성을 정하고 끝난 뒤
     * 되돌려 놓는다.
     *
     * @return the graphics or null if empty / 그래픽스. 비어 있으면 null
     */
    public Graphics2D graphics() {
        if (image == null) {
            return null;
        }
        if (graphics == null) {
            graphics = image.createGraphics();
            configure(graphics);
        }
        return graphics;
    }

    /**
     * Gives a graphics the rendering hints of the canvas.
     * <p>
     * 그래픽스에 캔버스의 렌더링 힌트를 준다.
     *
     * @param g the graphics / 그래픽스
     */
    public static void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    /**
     * Makes every pixel transparent black.
     * <p>
     * 모든 화소를 투명한 검정으로 만든다.
     */
    public void clear() {
        java.util.Arrays.fill(pixels, 0);
    }

    /**
     * Makes every pixel opaque black (the start of an opaque canvas).
     * <p>
     * 모든 화소를 불투명한 검정으로 만든다(불투명한 캔버스의 시작).
     */
    public void fillOpaqueBlack() {
        java.util.Arrays.fill(pixels, 0xFF000000);
    }

    /**
     * Composites every pixel onto opaque black: the color is scaled by the alpha and the alpha becomes 255.
     * <p>
     * 모든 화소를 불투명한 검정 위에 합친다: 색에 알파를 곱하고 알파를 255 로 만든다.
     */
    public void flattenOverBlack() {
        // 화소마다 알파를 색에 곱한 뒤 알파를 255 로 올린다.
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = p >>> 24;
            if (a == 255) {
                continue;
            }
            int r = ((p >> 16) & 0xFF) * a / 255;
            int g = ((p >> 8) & 0xFF) * a / 255;
            int b = (p & 0xFF) * a / 255;
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Copies the pixels into an immutable image for the renderer (the bitmap keeps changing, the copy never does).
     * <p>
     * 렌더러를 위해 화소를 불변 이미지로 복사한다(비트맵은 계속 바뀌지만 복사본은 바뀌지 않는다).
     *
     * @return the snapshot or null if the bitmap is empty / 스냅샷. 비트맵이 비어 있으면 null
     */
    public DecodedImage snapshot() {
        if (image == null) {
            return null;
        }
        return new DecodedImage(width, height, pixels.clone());
    }

    /**
     * Releases the graphics and drops the pixels' reference to the image; the bitmap must not be used afterwards.
     * <p>
     * 그래픽스를 해제한다. 그 뒤에는 이 비트맵을 쓰면 안 된다.
     */
    public void dispose() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }
}
