package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The event of a change of the text track list of a media element (<code>TrackEvent</code>, HTML 4.8.11.15): <code>addtrack</code> and
 * <code>removetrack</code> carry the text track that was added or removed in <code>track</code>. It does not bubble and cannot be cancelled by
 * default; a script can construct it with <code>new TrackEvent(type, {track})</code>. The track is held as an object because the DOM does not
 * know the script interface of text tracks.
 * <p>
 * 미디어 요소의 텍스트 트랙 목록이 바뀐 이벤트(<code>TrackEvent</code>, HTML 4.8.11.15): <code>addtrack</code>, <code>removetrack</code> 은 추가되거나 제거된
 * 텍스트 트랙을 <code>track</code> 에 담는다. 기본으로 버블링하지 않고 취소할 수 없으며 스크립트가 <code>new TrackEvent(type, {track})</code> 으로 만들 수도
 * 있다. DOM 은 텍스트 트랙의 스크립트 인터페이스를 모르므로 트랙은 객체로 보관한다.
 */
public class TrackEvent extends Event {

    /**
     * The track (a text track object, or null).
     * <p>
     * 트랙(텍스트 트랙 객체 또는 null).
     */
    private final Object track;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether the event bubbles / 버블링 여부
     * @param cancelable whether the event can be cancelled / 취소 가능 여부
     * @param track      the track or null / 트랙. 없으면 null
     */
    public TrackEvent(String type, boolean bubbles, boolean cancelable, Object track) {
        super(type, bubbles, cancelable);
        this.track = track;
    }

    /**
     * Returns the track (<code>TrackEvent.track</code>).
     * <p>
     * 트랙을 반환한다(<code>TrackEvent.track</code>).
     *
     * @return the text track object or null / 텍스트 트랙 객체. 없으면 null
     */
    public Object getTrack() {
        return track;
    }
}
