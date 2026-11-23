package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of decoding an image: its natural size, one or more composed frames and the animation loop count. A still image has exactly
 * one frame. Orientation (for example EXIF) is already applied to the pixels and to the size.
 * <p>
 * 이미지 디코딩 결과: 자연 크기, 합성된 프레임 하나 이상, 애니메이션 반복 횟수. 정지 이미지는 프레임이 정확히 하나이다. 방향 정보(예: EXIF)는
 * 이미 픽셀과 크기에 적용되어 있다.
 */
public final class DecodedImage {

    /**
     * Width in pixels.
     * <p>
     * 가로 픽셀 수.
     */
    private final int width;

    /**
     * Height in pixels.
     * <p>
     * 세로 픽셀 수.
     */
    private final int height;

    /**
     * Frames (unmodifiable).
     * <p>
     * 프레임(수정 불가).
     */
    private final List<DecodedFrame> frames;

    /**
     * Number of times the animation plays: 0 means forever, 1 means once.
     * <p>
     * 애니메이션 재생 횟수. 0 이면 무한, 1 이면 한 번.
     */
    private final int loopCount;

    /**
     * The EXIF orientation (1 to 8) that was applied to the pixels, 1 if none. Kept so that <code>image-orientation: none</code> can show the
     * pixels in the order they are stored in the file.
     * <p>
     * 픽셀에 적용한 EXIF 방향(1~8). 적용하지 않았으면 1. <code>image-orientation: none</code> 이 파일에 저장된 순서대로 보여 줄 수 있도록 남긴다.
     */
    private final int appliedOrientation;

    /**
     * The same image with the applied orientation undone (created on first use), or null.
     * <p>
     * 적용한 방향을 되돌린 같은 이미지(처음 쓸 때 만든다). 없으면 null.
     */
    private volatile DecodedImage unorientedCache;

    /**
     * Creates a still image.
     * <p>
     * 정지 이미지를 만든다.
     *
     * @param width  width / 가로
     * @param height height / 세로
     * @param argb   pixels (<code>0xAARRGGBB</code>, not premultiplied) / 픽셀(<code>0xAARRGGBB</code>, 알파를 미리 곱하지 않음)
     */
    public DecodedImage(int width, int height, int[] argb) {
        this(width, height, Collections.singletonList(new DecodedFrame(argb, 0)), 1);
    }

    /**
     * Creates an image with several frames.
     * <p>
     * 프레임이 여럿인 이미지를 만든다.
     *
     * @param width     width / 가로
     * @param height    height / 세로
     * @param frames    frames, at least one, each of width times height pixels / 프레임. 하나 이상이며 각각 가로 × 세로 픽셀
     * @param loopCount number of plays, 0 for forever / 재생 횟수. 0 이면 무한
     */
    public DecodedImage(int width, int height, List<DecodedFrame> frames, int loopCount) {
        this(width, height, frames, loopCount, 1);
    }

    /**
     * Creates a still image whose pixels already have an EXIF orientation applied.
     * <p>
     * 픽셀에 EXIF 방향을 이미 적용한 정지 이미지를 만든다.
     *
     * @param width              width after the orientation / 방향 적용 뒤의 가로
     * @param height             height after the orientation / 방향 적용 뒤의 세로
     * @param argb               pixels after the orientation / 방향 적용 뒤의 픽셀
     * @param appliedOrientation the orientation 1 to 8 that was applied (1 for none) / 적용한 방향 1~8(없으면 1)
     */
    public DecodedImage(int width, int height, int[] argb, int appliedOrientation) {
        this(width, height, Collections.singletonList(new DecodedFrame(argb, 0)), 1, appliedOrientation);
    }

    /**
     * Creates an image with several frames whose pixels already have an EXIF orientation applied.
     * <p>
     * 픽셀에 EXIF 방향을 이미 적용한 프레임이 여럿인 이미지를 만든다.
     *
     * @param width              width after the orientation / 방향 적용 뒤의 가로
     * @param height             height after the orientation / 방향 적용 뒤의 세로
     * @param frames             frames after the orientation / 방향 적용 뒤의 프레임
     * @param loopCount          number of plays, 0 for forever / 재생 횟수. 0 이면 무한
     * @param appliedOrientation the orientation 1 to 8 that was applied (1 for none) / 적용한 방향 1~8(없으면 1)
     */
    public DecodedImage(int width, int height, List<DecodedFrame> frames, int loopCount, int appliedOrientation) {
        if (width <= 0 || height <= 0 || frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("invalid image");
        }
        // 모든 프레임의 크기가 이미지 크기와 같은지 확인한다.
        for (DecodedFrame f : frames) {
            if (f.getArgb().length != width * height) {
                throw new IllegalArgumentException("frame size does not match the image size");
            }
        }
        this.width = width;
        this.height = height;
        this.frames = Collections.unmodifiableList(new ArrayList<DecodedFrame>(frames));
        this.loopCount = loopCount < 0 ? 0 : loopCount;
        this.appliedOrientation = appliedOrientation >= 1 && appliedOrientation <= 8 ? appliedOrientation : 1;
    }

    /**
     * Returns the EXIF orientation that was applied to the pixels (1 if none: the image is shown as stored).
     * <p>
     * 픽셀에 적용한 EXIF 방향을 반환한다(없으면 1: 저장된 그대로 보여 준다).
     *
     * @return orientation 1 to 8 / 방향 1~8
     */
    public int getAppliedOrientation() {
        return appliedOrientation;
    }

    /**
     * Returns the image as it is stored in the file, that is with the applied EXIF orientation undone. This is what the style
     * <code>image-orientation: none</code> shows (CSS Images 3, section 5.2). An image without an applied orientation returns itself; the
     * other one is created on the first call and kept. Never throws: the work does not look at the interrupt flag, because it runs while the
     * page is painted.
     * <p>
     * 파일에 저장된 그대로의 이미지, 즉 적용한 EXIF 방향을 되돌린 이미지를 반환한다. 스타일 <code>image-orientation: none</code> 이 보여 주는
     * 것이다(CSS Images 3, 5.2절). 적용한 방향이 없는 이미지는 자기 자신을 반환하고, 있는 이미지는 처음 부를 때 만들어 보관한다. 예외를 던지지
     * 않는다. 페이지를 그리는 동안 도는 작업이라 인터럽트 표시를 보지 않는다.
     *
     * @return the image in stored orientation / 저장된 방향의 이미지
     */
    public DecodedImage unoriented() {
        if (appliedOrientation == 1) {
            return this;
        }
        DecodedImage cached = unorientedCache;
        if (cached != null) {
            return cached;
        }
        int inverse = ExifOrientation.inverse(appliedOrientation);
        int[] size = new int[2];
        List<DecodedFrame> out = new ArrayList<DecodedFrame>(frames.size());
        try {
            // 프레임마다 적용한 방향의 역을 적용한다.
            for (DecodedFrame f : frames) {
                int[] pixels = PixelOps.orient(f.getArgb(), width, height, inverse, size, false);
                out.add(new DecodedFrame(pixels, f.getDelayMillis()));
            }
        } catch (org.duckdns.hjow.swingwebpanel.media.MediaException e) {
            // 인터럽트를 보지 않으므로 이 예외는 오지 않는다. 오더라도 적용한 상태 그대로 돌려준다.
            return this;
        }
        cached = new DecodedImage(size[0], size[1], out, loopCount, 1);
        unorientedCache = cached;
        return cached;
    }

    /**
     * Returns the width in pixels.
     * <p>
     * 가로 픽셀 수를 반환한다.
     *
     * @return width / 가로
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in pixels.
     * <p>
     * 세로 픽셀 수를 반환한다.
     *
     * @return height / 세로
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the frames (unmodifiable, never empty).
     * <p>
     * 프레임을 반환한다(수정 불가, 비어 있지 않음).
     *
     * @return frames / 프레임
     */
    public List<DecodedFrame> getFrames() {
        return frames;
    }

    /**
     * Returns the number of times the animation plays (0 forever).
     * <p>
     * 애니메이션 재생 횟수를 반환한다(0 은 무한).
     *
     * @return loop count / 재생 횟수
     */
    public int getLoopCount() {
        return loopCount;
    }

    /**
     * Tells whether this image has more than one frame.
     * <p>
     * 이 이미지의 프레임이 둘 이상인지 알려 준다.
     *
     * @return true for an animation / 애니메이션이면 true
     */
    public boolean isAnimated() {
        return frames.size() > 1;
    }
}
