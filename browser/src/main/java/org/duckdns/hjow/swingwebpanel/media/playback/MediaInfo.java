package org.duckdns.hjow.swingwebpanel.media.playback;

/**
 * The facts about a loaded media resource: sample rate, channels and length (plan item P18-08). Immutable.
 * <p>
 * 로드한 미디어 리소스에 대한 정보: 샘플링 주파수, 채널 수, 길이(계획 P18-08). 불변이다.
 */
public final class MediaInfo {

    /**
     * Frames per second.
     * <p>
     * 초당 프레임 수.
     */
    private final int sampleRate;

    /**
     * Channels (1 or 2).
     * <p>
     * 채널 수(1 또는 2).
     */
    private final int channels;

    /**
     * Total frames or -1.
     * <p>
     * 전체 프레임 수 또는 -1.
     */
    private final long frameCount;

    /**
     * Whether the source can seek.
     * <p>
     * 소스가 탐색할 수 있는지 여부.
     */
    private final boolean seekable;

    /**
     * Creates the record.
     * <p>
     * 기록을 만든다.
     *
     * @param sampleRate the frames per second / 초당 프레임 수
     * @param channels   the channels / 채널 수
     * @param frameCount the total frames or -1 / 전체 프레임 수 또는 -1
     * @param seekable   whether seeking works / 탐색이 되는지 여부
     */
    public MediaInfo(int sampleRate, int channels, long frameCount, boolean seekable) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.frameCount = frameCount;
        this.seekable = seekable;
    }

    /**
     * Returns the frames per second.
     * <p>
     * 초당 프레임 수를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the channels of the decoded PCM.
     * <p>
     * 디코딩된 PCM 의 채널 수를 반환한다.
     *
     * @return the channels / 채널 수
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Returns the total number of frames, -1 if the source could not tell it in advance.
     * <p>
     * 전체 프레임 수를 반환한다. 소스가 미리 알려 주지 못했으면 -1.
     *
     * @return the frames / 프레임 수
     */
    public long getFrameCount() {
        return frameCount;
    }

    /**
     * Returns the duration in seconds, positive infinity when the length is not known in advance.
     * <p>
     * 길이(초)를 반환한다. 미리 알 수 없으면 양의 무한대.
     *
     * @return the duration / 길이
     */
    public double getDuration() {
        return frameCount < 0 ? Double.POSITIVE_INFINITY : (double) frameCount / sampleRate;
    }

    /**
     * Tells whether the position can be moved.
     * <p>
     * 위치를 옮길 수 있는지 알려 준다.
     *
     * @return true if seekable / 탐색할 수 있으면 true
     */
    public boolean isSeekable() {
        return seekable;
    }
}
