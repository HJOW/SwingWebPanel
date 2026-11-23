package org.duckdns.hjow.swingwebpanel.media;

/**
 * Limits that every decoder must respect so that damaged or hostile data cannot exhaust memory or time (plan items P18-10, P22-09).
 * A decoder checks the limits <em>before</em> it allocates buffers from sizes found in the data, and reports
 * {@link MediaException.Kind#TOO_LARGE} when one is exceeded.
 * <p>
 * 손상되었거나 악의적인 데이터가 메모리나 시간을 고갈시키지 못하도록 모든 디코더가 지켜야 하는 제한(계획 P18-10, P22-09). 디코더는 데이터에서
 * 읽은 크기로 버퍼를 할당하기 <em>전에</em> 제한을 확인하며 초과하면 {@link MediaException.Kind#TOO_LARGE} 를 보고한다.
 */
public final class DecodeLimits {

    /**
     * Default maximum number of pixels of one image (width times height): 64 million.
     * <p>
     * 이미지 하나의 기본 최대 픽셀 수(가로 × 세로): 6,400만.
     */
    public static final long DEFAULT_MAX_PIXELS = 64L * 1000 * 1000;

    /**
     * Default maximum size of the encoded data in bytes: 64 MiB.
     * <p>
     * 인코딩된 데이터의 기본 최대 크기(바이트): 64 MiB.
     */
    public static final long DEFAULT_MAX_BYTES = 64L * 1024 * 1024;

    /**
     * Default maximum number of frames of an animation: 4096.
     * <p>
     * 애니메이션의 기본 최대 프레임 수: 4096.
     */
    public static final int DEFAULT_MAX_FRAMES = 4096;

    /**
     * Default maximum total number of decoded pixels over all frames of an animation: 256 million.
     * <p>
     * 애니메이션의 모든 프레임에 걸친 디코딩된 픽셀 수의 기본 최대 합계: 2억 5,600만.
     */
    public static final long DEFAULT_MAX_TOTAL_PIXELS = 256L * 1000 * 1000;

    /**
     * The default limits.
     * <p>
     * 기본 제한.
     */
    public static final DecodeLimits DEFAULT = new DecodeLimits(DEFAULT_MAX_PIXELS, DEFAULT_MAX_BYTES, DEFAULT_MAX_FRAMES,
            DEFAULT_MAX_TOTAL_PIXELS);

    /**
     * Maximum pixels of one image.
     * <p>
     * 이미지 하나의 최대 픽셀 수.
     */
    private final long maxPixels;

    /**
     * Maximum size of the encoded data.
     * <p>
     * 인코딩된 데이터의 최대 크기.
     */
    private final long maxBytes;

    /**
     * Maximum number of frames.
     * <p>
     * 최대 프레임 수.
     */
    private final int maxFrames;

    /**
     * Maximum total pixels over all frames.
     * <p>
     * 모든 프레임의 최대 픽셀 합계.
     */
    private final long maxTotalPixels;

    /**
     * Creates limits.
     * <p>
     * 제한을 만든다.
     *
     * @param maxPixels      maximum pixels of one image / 이미지 하나의 최대 픽셀 수
     * @param maxBytes       maximum encoded size in bytes / 인코딩 데이터의 최대 바이트 수
     * @param maxFrames      maximum number of frames / 최대 프레임 수
     * @param maxTotalPixels maximum pixels over all frames / 모든 프레임의 최대 픽셀 합계
     */
    public DecodeLimits(long maxPixels, long maxBytes, int maxFrames, long maxTotalPixels) {
        this.maxPixels = maxPixels;
        this.maxBytes = maxBytes;
        this.maxFrames = maxFrames;
        this.maxTotalPixels = maxTotalPixels;
    }

    /**
     * Returns the maximum pixels of one image.
     * <p>
     * 이미지 하나의 최대 픽셀 수를 반환한다.
     *
     * @return pixels / 픽셀 수
     */
    public long getMaxPixels() {
        return maxPixels;
    }

    /**
     * Returns the maximum encoded size.
     * <p>
     * 인코딩 데이터의 최대 크기를 반환한다.
     *
     * @return bytes / 바이트 수
     */
    public long getMaxBytes() {
        return maxBytes;
    }

    /**
     * Returns the maximum number of frames.
     * <p>
     * 최대 프레임 수를 반환한다.
     *
     * @return frames / 프레임 수
     */
    public int getMaxFrames() {
        return maxFrames;
    }

    /**
     * Returns the maximum total pixels over all frames.
     * <p>
     * 모든 프레임의 최대 픽셀 합계를 반환한다.
     *
     * @return pixels / 픽셀 수
     */
    public long getMaxTotalPixels() {
        return maxTotalPixels;
    }

    /**
     * Checks the size of an image against the limits and fails before any buffer is allocated.
     * <p>
     * 이미지 크기를 제한과 대조하며 버퍼를 할당하기 전에 실패한다.
     *
     * @param width  width in pixels / 가로 픽셀 수
     * @param height height in pixels / 세로 픽셀 수
     * @throws MediaException CORRUPT for a non-positive size, TOO_LARGE for a size over the limit / 크기가 0 이하이면 CORRUPT, 제한을 넘으면 TOO_LARGE
     */
    public void checkImageSize(long width, long height) throws MediaException {
        if (width <= 0 || height <= 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid image size " + width + "x" + height);
        }
        if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE || width * height > maxPixels) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "image is too large: " + width + "x" + height);
        }
    }
}
