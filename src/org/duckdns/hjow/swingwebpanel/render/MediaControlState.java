package org.duckdns.hjow.swingwebpanel.render;

/**
 * An immutable snapshot of what the control bar of an <code>audio</code> element with the <code>controls</code> attribute shows (plan item
 * P18-08): whether a resource is loaded, whether the element is paused or ended, the position and the length in seconds, the volume and
 * the mute flag. The script host takes the snapshot on the document thread and the renderer paints it, so painting never reads the live
 * state of the element.
 * <p>
 * <code>controls</code> 속성이 있는 <code>audio</code> 요소의 컨트롤 막대가 보여 주는 것의 불변 스냅샷(계획 P18-08): 리소스가 로드되었는지, 요소가
 * 일시정지되었거나 끝났는지, 초 단위의 위치와 길이, 음량과 음소거 플래그. 스크립트 호스트가 문서 스레드에서 스냅샷을 떠고 렌더러가 그것을 그리므로
 * 그리기는 요소의 실시간 상태를 읽지 않는다.
 */
public final class MediaControlState {

    /**
     * The state of an element that has no loaded resource yet: paused at 0:00, volume 1, not muted.
     * <p>
     * 아직 로드된 리소스가 없는 요소의 상태: 0:00 에서 일시정지, 음량 1, 음소거 아님.
     */
    public static final MediaControlState IDLE = new MediaControlState(false, true, false, 0, Double.NaN, 1.0, false);

    /**
     * True once the metadata of a resource is known.
     * <p>
     * 리소스의 메타데이터를 알게 되었으면 true.
     */
    private final boolean loaded;

    /**
     * True while the element is paused (the play button shows the play symbol).
     * <p>
     * 요소가 일시정지된 동안 true(재생 버튼이 재생 기호를 보여 준다).
     */
    private final boolean paused;

    /**
     * True when the playback reached the end.
     * <p>
     * 재생이 끝에 닿았으면 true.
     */
    private final boolean ended;

    /**
     * The position in seconds.
     * <p>
     * 초 단위의 위치.
     */
    private final double currentTime;

    /**
     * The length in seconds: NaN before the metadata, infinity when the length is not known.
     * <p>
     * 초 단위의 길이. 메타데이터 전에는 NaN, 길이를 모르면 무한대.
     */
    private final double duration;

    /**
     * The volume from 0 to 1.
     * <p>
     * 0 에서 1 까지의 음량.
     */
    private final double volume;

    /**
     * True if the sound is muted.
     * <p>
     * 소리가 꺼져 있으면 true.
     */
    private final boolean muted;

    /**
     * Creates a snapshot.
     * <p>
     * 스냅샷을 만든다.
     *
     * @param loaded      true once the metadata is known / 메타데이터를 알면 true
     * @param paused      true while paused / 일시정지된 동안 true
     * @param ended       true at the end / 끝에 있으면 true
     * @param currentTime the position in seconds / 초 단위의 위치
     * @param duration    the length in seconds (NaN, infinity) / 초 단위의 길이(NaN, 무한대)
     * @param volume      the volume, 0 to 1 / 음량(0에서 1)
     * @param muted       true if muted / 음소거이면 true
     */
    public MediaControlState(boolean loaded, boolean paused, boolean ended, double currentTime, double duration, double volume, boolean muted) {
        this.loaded = loaded;
        this.paused = paused;
        this.ended = ended;
        this.currentTime = currentTime;
        this.duration = duration;
        this.volume = volume;
        this.muted = muted;
    }

    /**
     * Tells whether the metadata of a resource is known.
     * <p>
     * 리소스의 메타데이터를 아는지 알려 준다.
     *
     * @return true if loaded / 로드되었으면 true
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Tells whether the element is paused.
     * <p>
     * 요소가 일시정지되었는지 알려 준다.
     *
     * @return true if paused / 일시정지되었으면 true
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Tells whether the playback reached the end.
     * <p>
     * 재생이 끝에 닿았는지 알려 준다.
     *
     * @return true at the end / 끝에 있으면 true
     */
    public boolean isEnded() {
        return ended;
    }

    /**
     * Returns the position in seconds.
     * <p>
     * 초 단위의 위치를 반환한다.
     *
     * @return the position / 위치
     */
    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Returns the length in seconds: NaN before the metadata, infinity when it is not known.
     * <p>
     * 초 단위의 길이를 반환한다. 메타데이터 전에는 NaN, 모르면 무한대.
     *
     * @return the length / 길이
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Returns the volume.
     * <p>
     * 음량을 반환한다.
     *
     * @return the volume, 0 to 1 / 음량(0에서 1)
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Tells whether the sound is muted.
     * <p>
     * 소리가 꺼져 있는지 알려 준다.
     *
     * @return true if muted / 꺼져 있으면 true
     */
    public boolean isMuted() {
        return muted;
    }
}
