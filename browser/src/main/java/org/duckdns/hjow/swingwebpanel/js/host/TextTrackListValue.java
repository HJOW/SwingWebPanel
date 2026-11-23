package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * The script object of the list of the text tracks of a media element (<code>TextTrackList</code>, HTML 4.8.11.11.5): <code>media.textTracks</code>.
 * The list is live and has the order of the specification (the tracks of the <code>track</code> children in tree order, then the tracks that
 * <code>addTextTrack()</code> made). <code>change</code>, <code>addtrack</code> and <code>removetrack</code> fire at it. The class is public
 * because the DOM bridge reads its accessors by reflection.
 * <p>
 * 미디어 요소의 텍스트 트랙 목록의 스크립트 객체(<code>TextTrackList</code>, HTML 4.8.11.11.5): <code>media.textTracks</code>. 목록은 살아 있고
 * 규격의 순서를 가진다(<code>track</code> 자식의 트랙이 트리 순서로, 그 뒤에 <code>addTextTrack()</code> 이 만든 트랙). <code>change</code>,
 * <code>addtrack</code>, <code>removetrack</code> 이 그 위에서 발생한다. DOM 브리지가 접근자를 리플렉션으로 읽으므로 클래스는 public 이다.
 */
public final class TextTrackListValue extends EventTarget {

    /**
     * The manager of the page.
     * <p>
     * 페이지의 관리자.
     */
    private final TextTracks owner;

    /**
     * The media element that the list belongs to.
     * <p>
     * 목록이 속한 미디어 요소.
     */
    final MediaElements.Media media;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param owner the manager / 관리자
     * @param media the state of the media element / 미디어 요소의 상태
     */
    TextTrackListValue(TextTracks owner, MediaElements.Media media) {
        this.owner = owner;
        this.media = media;
    }

    /**
     * Returns the number of tracks (<code>length</code>).
     * <p>
     * 트랙의 수를 반환한다(<code>length</code>).
     *
     * @return the number / 수
     */
    public int getLength() {
        return owner.listOf(media).size();
    }

    /**
     * Returns the track at an index, or null.
     * <p>
     * 위치의 트랙을 반환한다. 없으면 null.
     *
     * @param index the index / 위치
     * @return the track or null / 트랙. 없으면 null
     */
    TextTrackValue item(int index) {
        List<TextTrackValue> l = owner.listOf(media);
        return index >= 0 && index < l.size() ? l.get(index) : null;
    }

    /**
     * Implements <code>getTrackById(id)</code>: the first track whose <code>id</code> is the argument, or null.
     * <p>
     * <code>getTrackById(id)</code> 를 구현한다: <code>id</code> 가 인수와 같은 첫 트랙. 없으면 null.
     *
     * @param id the identifier / 식별자
     * @return the track or null / 트랙. 없으면 null
     */
    public Object getTrackById(String id) {
        List<TextTrackValue> l = owner.listOf(media);
        // 트랙을 순서대로 훑어 id 가 같은 첫 트랙을 찾는다.
        for (int i = 0; i < l.size(); i++) {
            if (id.equals(l.get(i).getId())) {
                return l.get(i);
            }
        }
        return null;
    }
}
