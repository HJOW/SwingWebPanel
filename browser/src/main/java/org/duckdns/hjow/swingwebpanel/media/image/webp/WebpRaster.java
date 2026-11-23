package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * A decoded WebP bitstream: its size and non-premultiplied ARGB pixels (plan item P18-03). It is the common result of the lossy and the
 * lossless decoders before animation frames are composed.
 * <p>
 * 디코딩된 WebP 비트스트림: 크기와 알파를 미리 곱하지 않은 ARGB 픽셀(계획 P18-03). 애니메이션 프레임을 합성하기 전의, 손실/무손실 디코더의 공통
 * 결과이다.
 */
final class WebpRaster {

    /**
     * Width in pixels.
     * <p>
     * 가로 픽셀 수.
     */
    final int width;

    /**
     * Height in pixels.
     * <p>
     * 세로 픽셀 수.
     */
    final int height;

    /**
     * Pixels, row by row, <code>0xAARRGGBB</code>.
     * <p>
     * 픽셀. 행 순서, <code>0xAARRGGBB</code>.
     */
    final int[] argb;

    /**
     * Whether the bitstream declares an alpha channel: the alpha bit of the VP8L header, or the presence of an <code>ALPH</code> chunk next to
     * a lossy frame.
     * <p>
     * 비트스트림이 알파 채널을 가진다고 선언했는지 여부: VP8L 헤더의 알파 비트, 또는 손실 프레임 옆의 <code>ALPH</code> 청크 존재.
     */
    final boolean hasAlpha;

    /**
     * Creates a raster.
     * <p>
     * 래스터를 만든다.
     *
     * @param width    width / 가로
     * @param height   height / 세로
     * @param argb     pixels (not copied) / 픽셀(복사하지 않음)
     * @param hasAlpha whether an alpha channel is declared / 알파 채널이 선언되었는지 여부
     */
    WebpRaster(int width, int height, int[] argb, boolean hasAlpha) {
        this.width = width;
        this.height = height;
        this.argb = argb;
        this.hasAlpha = hasAlpha;
    }
}
