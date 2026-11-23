package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;

/**
 * The script object of a list of text track cues (<code>TextTrackCueList</code>, HTML 4.8.11.11.5): the list of the cues of a text track or the list
 * of its active cues. The list is live: it is read from the track at each access, in the text track cue order. The class is public because the DOM
 * bridge reads its accessors by reflection.
 * <p>
 * 텍스트 트랙 cue 목록의 스크립트 객체(<code>TextTrackCueList</code>, HTML 4.8.11.11.5): 텍스트 트랙의 cue 목록 또는 활성 cue 목록. 목록은 살아 있다:
 * 접근할 때마다 트랙에서 텍스트 트랙 cue 순서로 읽는다. DOM 브리지가 접근자를 리플렉션으로 읽으므로 클래스는 public 이다.
 */
public final class TextTrackCueListValue {

    /**
     * The track that the list belongs to.
     * <p>
     * 목록이 속한 트랙.
     */
    private final TextTrackValue track;

    /**
     * True for the list of the active cues, false for the list of all cues.
     * <p>
     * 활성 cue 목록이면 true, 모든 cue 목록이면 false.
     */
    private final boolean active;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param track  the track / 트랙
     * @param active true for the active cues / 활성 cue 이면 true
     */
    TextTrackCueListValue(TextTrackValue track, boolean active) {
        this.track = track;
        this.active = active;
    }

    /**
     * Returns the cues of the list now.
     * <p>
     * 지금 목록의 cue 를 반환한다.
     *
     * @return the cues in order / 순서대로의 cue
     */
    private List<TextTrackCueValue> now() {
        return active ? track.activeCuesNow() : track.cuesNow();
    }

    /**
     * Returns the number of cues (<code>length</code>).
     * <p>
     * cue 의 수를 반환한다(<code>length</code>).
     *
     * @return the number / 수
     */
    public int getLength() {
        return now().size();
    }

    /**
     * Returns the cue at an index, or null.
     * <p>
     * 위치의 cue 를 반환한다. 없으면 null.
     *
     * @param index the index / 위치
     * @return the cue or null / cue. 없으면 null
     */
    TextTrackCueValue item(int index) {
        List<TextTrackCueValue> l = now();
        return index >= 0 && index < l.size() ? l.get(index) : null;
    }

    /**
     * Implements <code>getCueById(id)</code>: the first cue (in the cue order) with the identifier; the empty identifier finds nothing.
     * <p>
     * <code>getCueById(id)</code> 를 구현한다: 식별자가 같은 첫 cue(cue 순서). 빈 식별자는 아무것도 찾지 않는다.
     *
     * @param id the identifier / 식별자
     * @return the cue or null / cue. 없으면 null
     */
    public Object getCueById(String id) {
        if (id.length() == 0) {
            return null;
        }
        List<TextTrackCueValue> l = now();
        // cue 를 순서대로 훑어 식별자가 같은 첫 cue 를 찾는다.
        for (int i = 0; i < l.size(); i++) {
            if (id.equals(l.get(i).data.id)) {
                return l.get(i);
            }
        }
        return null;
    }
}
