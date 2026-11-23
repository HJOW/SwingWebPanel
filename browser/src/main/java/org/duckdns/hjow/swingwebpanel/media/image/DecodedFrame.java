package org.duckdns.hjow.swingwebpanel.media.image;

/**
 * One fully composed frame of a decoded image: the complete canvas (not a sub-rectangle) as non-premultiplied ARGB pixels. Animation
 * decoders apply disposal and blend rules themselves, so a consumer only has to show the frames one after another for their delay.
 * <p>
 * 디코딩된 이미지의 완전히 합성된 프레임 하나: 부분 사각형이 아니라 캔버스 전체를 알파 미리 곱하지 않은 ARGB 픽셀로 가진다. 애니메이션 디코더가
 * 처리 방법(disposal)과 합성 규칙을 스스로 적용하므로 사용하는 쪽은 프레임을 지연 시간만큼씩 차례로 보여 주기만 하면 된다.
 */
public final class DecodedFrame {

    /**
     * Pixels, row by row, <code>0xAARRGGBB</code>, not premultiplied; length is width times height of the image.
     * <p>
     * 픽셀. 행 순서, <code>0xAARRGGBB</code>, 알파를 미리 곱하지 않음. 길이는 이미지의 가로 × 세로.
     */
    private final int[] argb;

    /**
     * Display time in milliseconds (0 for a still image).
     * <p>
     * 표시 시간(밀리초). 정지 이미지는 0.
     */
    private final int delayMillis;

    /**
     * Creates a frame. The array is not copied; the caller must not change it afterwards.
     * <p>
     * 프레임을 만든다. 배열은 복사하지 않으므로 호출자는 이후 바꾸면 안 된다.
     *
     * @param argb        pixels / 픽셀
     * @param delayMillis display time in milliseconds / 표시 시간(밀리초)
     */
    public DecodedFrame(int[] argb, int delayMillis) {
        if (argb == null) {
            throw new IllegalArgumentException("argb must not be null");
        }
        this.argb = argb;
        this.delayMillis = delayMillis < 0 ? 0 : delayMillis;
    }

    /**
     * Returns the pixels (shared, do not modify).
     * <p>
     * 픽셀을 반환한다(공유되므로 수정하지 않는다).
     *
     * @return pixels / 픽셀
     */
    public int[] getArgb() {
        return argb;
    }

    /**
     * Returns the display time in milliseconds.
     * <p>
     * 표시 시간(밀리초)을 반환한다.
     *
     * @return delay / 지연 시간
     */
    public int getDelayMillis() {
        return delayMillis;
    }
}
