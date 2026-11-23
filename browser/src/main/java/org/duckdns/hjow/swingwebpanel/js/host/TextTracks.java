package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.TrackEvent;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttCue;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttCueText;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttFile;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttNode;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttParser;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttRegion;

/**
 * The text tracks of the media elements of one page (plan item P20-13; HTML 4.8.11.11 "Timed text tracks" of the HTML Standard as of 2026-11-17 and WebVTT
 * of 2026-05-20, see <code>docs/progress/webapi2.md</code>): the list of text tracks of a media element, the text tracks of the <code>track</code>
 * children and of <code>addTextTrack()</code>, the cues of a track, the track processing model that fetches the WebVTT file of a <code>track</code>
 * element through the request service of the component (the same-origin fallback of a <code>track</code> request, the <code>crossorigin</code> attribute
 * of the media element), the automatic selection of a <code>default</code> track, and the time marches on steps: whenever the playback position changes
 * (a time update, a seek, a loop, the end) or a cue or a track changes, the cues that are active are worked out again and the events
 * <code>enter</code>, <code>exit</code> (in the order of the specification) and <code>cuechange</code> are queued as tasks of the media element; a cue
 * with the pause-on-exit flag pauses the element when playback leaves it. While audio plays, a timer runs the steps at the next boundary of a cue, so an
 * event comes close to its place on the timeline and not only at the time updates.
 * <p>
 * Differences from the specification: the show poster flag is not modelled (the steps run as soon as the metadata is known); the text tracks of an element
 * are not ready-checked for the <code>load</code> event; the user's preferences for the language of tracks do not exist, so only the <code>default</code>
 * attribute selects a track; a fetch ends when the whole file arrived (the specification parses it incrementally); the content type of the file is not
 * checked; a track removed from its media element during a fetch aborts the fetch and becomes not loaded; the cues are not rendered (only
 * <code>audio</code> elements are supported, and an audio element has no area that shows text).
 * <p>
 * 한 페이지의 미디어 요소의 텍스트 트랙(계획 P20-13. 2026-11-17 의 HTML Standard 4.8.11.11 "Timed text tracks"와 2026-05-20 의 WebVTT,
 * <code>docs/progress/webapi2.md</code> 참조): 미디어 요소의 텍스트 트랙 목록, <code>track</code> 자식과 <code>addTextTrack()</code> 의 텍스트 트랙, 트랙의
 * cue, 컴포넌트의 요청 서비스로 <code>track</code> 요소의 WebVTT 파일을 가져오는 트랙 처리 모델(<code>track</code> 요청의 same-origin 대체, 미디어 요소의
 * <code>crossorigin</code> 속성), <code>default</code> 트랙의 자동 선택, time marches on 단계: 재생 위치가 바뀌거나(시간 갱신, 탐색, 반복, 끝) cue 나 트랙이 바뀔
 * 때마다 활성 cue 를 다시 계산하고 <code>enter</code>, <code>exit</code>(규격의 순서)와 <code>cuechange</code> 이벤트를 미디어 요소의 작업으로 올린다.
 * pause-on-exit 표시가 있는 cue 는 재생이 그 cue 를 벗어날 때 요소를 일시정지한다. 오디오가 재생되는 동안 타이머가 다음 cue 경계에서 단계를 실행하므로
 * 이벤트는 시간 갱신 때만이 아니라 타임라인의 자리에 가깝게 발생한다.
 * <p>
 * 규격과 다른 점: show poster 표시를 모델링하지 않는다(메타데이터를 알면 곧바로 단계를 실행한다). 요소의 텍스트 트랙이 준비되었는지를 <code>load</code>
 * 이벤트에 반영하지 않는다. 트랙 언어에 대한 사용자 선호가 없으므로 <code>default</code> 속성만 트랙을 고른다. 가져오기는 파일 전체가 도착해야 끝난다(규격은
 * 점진적으로 해석한다). 파일의 콘텐츠 타입은 검사하지 않는다. 가져오는 중에 미디어 요소에서 제거된 트랙은 가져오기를 중단하고 로드되지 않은 상태가 된다.
 * cue 를 그리지 않는다(<code>audio</code> 요소만 지원하며 audio 요소에는 텍스트를 보일 영역이 없다).
 */
final class TextTracks {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The media elements of the page.
     * <p>
     * 페이지의 미디어 요소.
     */
    private final MediaElements media;

    /**
     * The tracks of the <code>track</code> elements; an element that is no longer referenced disappears with its track.
     * <p>
     * <code>track</code> 요소의 트랙. 더 이상 참조되지 않는 요소는 트랙과 함께 사라진다.
     */
    private final Map<Element, TextTrackValue> byElement = new WeakHashMap<Element, TextTrackValue>();

    /**
     * The media elements whose automatic track selection waits for the end of the parsing.
     * <p>
     * 자동 트랙 선택이 파싱의 끝을 기다리는 미디어 요소.
     */
    private final List<MediaElements.Media> waiting = new ArrayList<MediaElements.Media>();

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * One event of the time marches on steps that is to be queued.
     * <p>
     * 큐에 올릴 time marches on 단계의 이벤트 하나.
     */
    private static final class CueEvent {
        /**
         * The cue.
         * <p>
         * 이벤트의 cue 객체.
         */
        final TextTrackCueValue cue;

        /**
         * The time that the event belongs to.
         * <p>
         * 이벤트가 속한 시각.
         */
        final double time;

        /**
         * True for <code>exit</code>, false for <code>enter</code>.
         * <p>
         * <code>exit</code> 이면 true, <code>enter</code> 이면 false.
         */
        final boolean exit;

        /**
         * Creates an event.
         * <p>
         * 이벤트를 만든다.
         *
         * @param cue  the cue / cue
         * @param time the time / 시각
         * @param exit true for exit / exit 이면 true
         */
        CueEvent(TextTrackCueValue cue, double time, boolean exit) {
            this.cue = cue;
            this.time = time;
            this.exit = exit;
        }
    }

    /**
     * Orders the events: by time, then by the text track cue order, then <code>enter</code> before <code>exit</code>.
     * <p>
     * 이벤트를 정렬한다: 시각, 텍스트 트랙 cue 순서, <code>enter</code> 가 <code>exit</code> 보다 먼저.
     */
    private static final Comparator<CueEvent> EVENT_ORDER = new Comparator<CueEvent>() {
        /**
         * Compares two events.
         * <p>
         * 두 이벤트를 비교한다.
         *
         * @param a the first event / 첫 이벤트
         * @param b the second event / 둘째 이벤트
         * @return the sign of the order / 순서의 부호
         */
        public int compare(CueEvent a, CueEvent b) {
            if (a.time != b.time) {
                return a.time < b.time ? -1 : 1;
            }
            if (a.cue.order != b.cue.order) {
                return a.cue.order < b.cue.order ? -1 : 1;
            }
            if (a.exit != b.exit) {
                return a.exit ? 1 : -1;
            }
            return 0;
        }
    };

    /**
     * Creates the manager of a page.
     * <p>
     * 페이지의 관리자를 만든다.
     *
     * @param page  the page / 페이지
     * @param media the media elements / 미디어 요소
     */
    TextTracks(PageScripts page, MediaElements media) {
        this.page = page;
        this.media = media;
    }

    /**
     * Tells whether a node is a <code>track</code> element of the HTML namespace.
     * <p>
     * 노드가 HTML 네임스페이스의 <code>track</code> 요소인지 알려 준다.
     *
     * @param n the node / 노드
     * @return true for a track element / track 요소이면 true
     */
    static boolean isTrack(Node n) {
        return n instanceof Element && Namespaces.HTML.equals(((Element) n).getNamespaceURI()) && "track".equals(((Element) n).getLocalName());
    }

    /**
     * Returns the text track of a <code>track</code> element (made at the first use, the same object afterwards).
     * <p>
     * <code>track</code> 요소의 텍스트 트랙을 반환한다(처음 쓸 때 만들고 그 뒤로는 같은 객체).
     *
     * @param e the element / 요소
     * @return the track / 트랙
     */
    TextTrackValue trackFor(Element e) {
        TextTrackValue t = byElement.get(e);
        if (t == null) {
            t = new TextTrackValue(this, e);
            byElement.put(e, t);
        }
        return t;
    }

    /**
     * Returns the media element that a track belongs to now: the element of <code>addTextTrack()</code>, or the parent of the
     * <code>track</code> element if it is a media element; null if there is none.
     * <p>
     * 트랙이 지금 속한 미디어 요소를 반환한다: <code>addTextTrack()</code> 의 요소, 또는 <code>track</code> 요소의 부모가 미디어 요소이면 그것. 없으면 null.
     *
     * @param t the track / 트랙
     * @return the state of the media element or null / 미디어 요소의 상태. 없으면 null
     */
    MediaElements.Media mediaOf(TextTrackValue t) {
        if (t.scripted != null) {
            return t.scripted;
        }
        Element e = t.element();
        if (e == null) {
            return null;
        }
        Node p = e.getParentNode();
        if (p instanceof Element && MediaElements.isMedia((Element) p)) {
            return media.state((Element) p);
        }
        return null;
    }

    /**
     * Returns the list of the text tracks of a media element in the order of the specification: the tracks of the <code>track</code> children in
     * tree order, then the tracks of <code>addTextTrack()</code> in the order they were added.
     * <p>
     * 미디어 요소의 텍스트 트랙 목록을 규격의 순서로 반환한다: <code>track</code> 자식의 트랙(트리 순서), 그 뒤에 <code>addTextTrack()</code> 의 트랙(더한 순서).
     *
     * @param m the state of the media element / 미디어 요소의 상태
     * @return the tracks (a new list) / 트랙(새 목록)
     */
    List<TextTrackValue> listOf(MediaElements.Media m) {
        List<TextTrackValue> out = new ArrayList<TextTrackValue>();
        Element e = m.element();
        if (e != null) {
            // 자식을 순서대로 훑으며 track 요소의 트랙을 모은다.
            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (isTrack(n)) {
                    out.add(trackFor((Element) n));
                }
            }
        }
        if (m.tracks != null) {
            out.addAll(m.tracks.scripted);
        }
        return out;
    }

    /**
     * Returns the list object of <code>textTracks</code> of a media element (the same object every time).
     * <p>
     * 미디어 요소의 <code>textTracks</code> 목록 객체를 반환한다(매번 같은 객체).
     *
     * @param m the state of the media element / 미디어 요소의 상태
     * @return the list object / 목록 객체
     */
    TextTrackListValue listValue(MediaElements.Media m) {
        MediaTracks mt = media.tracksOf(m);
        if (mt.list == null) {
            mt.list = new TextTrackListValue(this, m);
        }
        return mt.list;
    }

    /**
     * Implements <code>addTextTrack(kind, label, language)</code> of a media element: the new track is <code>hidden</code> and loaded, joins the list
     * and <code>addtrack</code> is queued.
     * <p>
     * 미디어 요소의 <code>addTextTrack(kind, label, language)</code> 을 구현한다: 새 트랙은 <code>hidden</code> 이고 로드된 상태이며 목록에 들어가고
     * <code>addtrack</code> 이 올라간다.
     *
     * @param m        the state of the media element / 미디어 요소의 상태
     * @param kind     the kind (checked by the caller) / 종류(호출자가 확인)
     * @param label    the label / 레이블
     * @param language the language / 언어
     * @return the track / 트랙
     */
    TextTrackValue addTextTrack(MediaElements.Media m, String kind, String label, String language) {
        TextTrackValue t = new TextTrackValue(this, m, kind, label, language);
        media.tracksOf(m).scripted.add(t);
        queueTrackEvent(m, "addtrack", t);
        return t;
    }

    /**
     * Returns the script object of a region (the same object for the same region: it is kept in the region itself, so nothing holds a region that
     * nobody uses).
     * <p>
     * region 의 스크립트 객체를 반환한다(같은 region 이면 같은 객체. region 자신이 가지고 있으므로 아무도 쓰지 않는 region 을 붙잡는 것이 없다).
     *
     * @param r the region or null / region. 없으면 null
     * @return the object or null / 객체. 없으면 null
     */
    VttRegionValue regionValue(VttRegion r) {
        if (r == null) {
            return null;
        }
        if (!(r.host instanceof VttRegionValue)) {
            r.host = new VttRegionValue(r);
        }
        return (VttRegionValue) r.host;
    }

    /**
     * Makes a script object for a cue.
     * <p>
     * cue 의 스크립트 객체를 만든다.
     *
     * @param data the cue data / cue 데이터
     * @return the object / 객체
     */
    TextTrackCueValue newCue(VttCue data) {
        return new TextTrackCueValue(this, data);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 이벤트 대기열
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Queues the event <code>addtrack</code> or <code>removetrack</code> at the track list of a media element (a task of the media element; nothing
     * happens if no script ever asked for the list).
     * <p>
     * 미디어 요소의 트랙 목록에 <code>addtrack</code> 또는 <code>removetrack</code> 이벤트를 올린다(미디어 요소의 작업. 스크립트가 목록을 물은 적이 없으면
     * 아무 일도 없다).
     *
     * @param m    the state of the media element / 미디어 요소의 상태
     * @param type the event type / 이벤트 종류
     * @param t    the track / 트랙
     */
    private void queueTrackEvent(final MediaElements.Media m, final String type, final TextTrackValue t) {
        media.queueTask(m, new Runnable() {
            /**
             * Fires the event.
             * <p>
             * 이벤트를 발생시킨다.
             */
            public void run() {
                if (m.tracks != null && m.tracks.list != null) {
                    page.dispatchTrustedEvent(m.tracks.list, new TrackEvent(type, false, false, t));
                }
            }
        }, false);
    }

    /**
     * Queues the <code>change</code> event of the track list after a change of a mode (several changes before the task runs make one event), and
     * runs the time marches on steps in that task.
     * <p>
     * 모드가 바뀐 뒤 트랙 목록의 <code>change</code> 이벤트를 올린다(작업이 실행되기 전의 여러 변경은 이벤트 하나가 된다). 그 작업에서 time marches on 단계도
     * 실행한다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    private void queueChange(final MediaElements.Media m) {
        final MediaTracks mt = media.tracksOf(m);
        if (mt.changePending) {
            return;
        }
        mt.changePending = true;
        media.queueTask(m, new Runnable() {
            /**
             * Fires <code>change</code> and runs the time marches on steps.
             * <p>
             * <code>change</code> 를 발생시키고 time marches on 단계를 실행한다.
             */
            public void run() {
                mt.changePending = false;
                if (mt.list != null) {
                    page.dispatchTrustedEvent(mt.list, new Event("change", false, false));
                }
                timeMarchesOn(m);
            }
        }, false);
    }

    /**
     * Asks for a run of the time marches on steps in a task of the media element (a change of cues asks for it; several asks before the task runs
     * make one run). The active cues therefore change after the script that changed the cues, as the specification's "when the script started" says.
     * <p>
     * 미디어 요소의 작업에서 time marches on 단계를 한 번 실행하도록 요청한다(cue 의 변경이 요청한다. 작업이 실행되기 전의 여러 요청은 한 번의 실행이 된다).
     * 그래서 활성 cue 는 cue 를 바꾼 스크립트가 끝난 뒤에 바뀌며, 이는 규격의 "when the script started" 와 맞는다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void requestMarch(final MediaElements.Media m) {
        final MediaTracks mt = media.tracksOf(m);
        if (mt.marchQueued || disposed) {
            return;
        }
        mt.marchQueued = true;
        media.queueTask(m, new Runnable() {
            /**
             * Runs the steps.
             * <p>
             * 단계를 실행한다.
             */
            public void run() {
                mt.marchQueued = false;
                timeMarchesOn(m);
            }
        }, false);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 모드, cue
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Changes the mode of a track: disabling it unsets the active flags at once, a change queues <code>change</code>, and the track processing model
     * starts for a track of a <code>track</code> element that became <code>hidden</code> or <code>showing</code>.
     * <p>
     * 트랙의 모드를 바꾼다: 비활성으로 바꾸면 활성 표시를 곧바로 지우고, 바뀌면 <code>change</code> 가 올라가며, <code>hidden</code>, <code>showing</code> 이 된
     * <code>track</code> 요소의 트랙은 트랙 처리 모델이 시작된다.
     *
     * @param t the track / 트랙
     * @param v the new mode / 새 모드
     */
    void setMode(TextTrackValue t, String v) {
        if (t.mode.equals(v)) {
            return;
        }
        t.mode = v;
        if ("disabled".equals(v)) {
            deactivate(t);
        }
        MediaElements.Media m = mediaOf(t);
        if (m != null) {
            queueChange(m);
        }
        startProcessing(t);
    }

    /**
     * Unsets the active flag of every cue of a track.
     * <p>
     * 트랙의 모든 cue 의 활성 표시를 지운다.
     *
     * @param t the track / 트랙
     */
    private void deactivate(TextTrackValue t) {
        // 트랙의 cue 마다 활성 표시를 지운다.
        for (int i = 0; i < t.cues.size(); i++) {
            t.cues.get(i).active = false;
        }
    }

    /**
     * Puts a cue into the list of cues of a track (a cue that is in another list leaves it first).
     * <p>
     * cue 를 트랙의 cue 목록에 넣는다(다른 목록에 있던 cue 는 먼저 그곳을 떠난다).
     *
     * @param t   the track / 트랙
     * @param cue the cue (a script value) / cue(스크립트 값)
     */
    void addCue(TextTrackValue t, Object cue) {
        if (!(cue instanceof TextTrackCueValue)) {
            throw JsException.typeError("Failed to execute 'addCue' on 'TextTrack': parameter 1 is not of type 'TextTrackCue'.");
        }
        TextTrackCueValue c = (TextTrackCueValue) cue;
        if (c.track != null) {
            detach(c.track, c);
        }
        attach(t, c);
    }

    /**
     * Takes a cue out of the list of cues of a track: a cue that is not in the list is a <code>NotFoundError</code>.
     * <p>
     * 트랙의 cue 목록에서 cue 를 뺀다: 목록에 없는 cue 는 <code>NotFoundError</code> 이다.
     *
     * @param t   the track / 트랙
     * @param cue the cue (a script value) / cue(스크립트 값)
     */
    void removeCue(TextTrackValue t, Object cue) {
        if (!(cue instanceof TextTrackCueValue)) {
            throw JsException.typeError("Failed to execute 'removeCue' on 'TextTrack': parameter 1 is not of type 'TextTrackCue'.");
        }
        TextTrackCueValue c = (TextTrackCueValue) cue;
        if (c.track != t) {
            throw DomException.notFound("Failed to execute 'removeCue' on 'TextTrack': The specified cue is not listed in the TextTrack's list of cues.");
        }
        detach(t, c);
    }

    /**
     * Adds a cue to a track without a check (a newly introduced cue of the media element, if the track is in its list).
     * <p>
     * 확인 없이 트랙에 cue 를 더한다(트랙이 미디어 요소의 목록에 있으면 그 요소의 새로 들어온 cue).
     *
     * @param t the track / 트랙
     * @param c the cue / cue
     */
    private void attach(TextTrackValue t, TextTrackCueValue c) {
        c.track = t;
        t.seq++;
        c.seq = t.seq;
        t.cues.add(c);
        t.invalidate();
        MediaElements.Media m = mediaOf(t);
        if (m != null) {
            media.tracksOf(m).newCues.add(c);
            requestMarch(m);
        }
    }

    /**
     * Takes a cue out of a track without a check.
     * <p>
     * 확인 없이 트랙에서 cue 를 뺀다.
     *
     * @param t the track / 트랙
     * @param c the cue / cue
     */
    private void detach(TextTrackValue t, TextTrackCueValue c) {
        t.cues.remove(c);
        c.track = null;
        c.active = false;
        t.invalidate();
        MediaElements.Media m = mediaOf(t);
        if (m != null) {
            media.tracksOf(m).newCues.remove(c);
            requestMarch(m);
        }
    }

    /**
     * Tells that the start or end time of a cue changed: the order of the cues of its track is stale and the active cues are worked out again.
     * <p>
     * cue 의 시작/끝 시각이 바뀌었음을 알린다: 트랙의 cue 순서는 낡았고 활성 cue 를 다시 계산한다.
     *
     * @param c the cue / cue
     */
    void cueTimesChanged(TextTrackCueValue c) {
        if (c.track != null) {
            c.track.invalidate();
            MediaElements.Media m = mediaOf(c.track);
            if (m != null) {
                requestMarch(m);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // track 요소의 변화
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * A <code>track</code> element became a child of a media element: its track joins the list (<code>addtrack</code>), its cues are newly
     * introduced, the automatic selection is asked for and the track processing model starts if the mode asks for it.
     * <p>
     * <code>track</code> 요소가 미디어 요소의 자식이 되었다: 트랙이 목록에 들어가고(<code>addtrack</code>), 그 cue 는 새로 들어온 것이 되며, 자동 선택을
     * 요청하고, 모드가 요구하면 트랙 처리 모델을 시작한다.
     *
     * @param m  the state of the media element / 미디어 요소의 상태
     * @param el the <code>track</code> element / <code>track</code> 요소
     */
    void trackInserted(MediaElements.Media m, Element el) {
        if (disposed) {
            return;
        }
        TextTrackValue t = trackFor(el);
        queueTrackEvent(m, "addtrack", t);
        MediaTracks mt = media.tracksOf(m);
        // 트랙에 이미 있는 cue 는 모두 새로 들어온 cue 이다.
        for (int i = 0; i < t.cues.size(); i++) {
            mt.newCues.add(t.cues.get(i));
        }
        if (!t.cues.isEmpty()) {
            requestMarch(m);
        }
        queueSelection(m);
        startProcessing(t);
    }

    /**
     * A <code>track</code> element is no longer a child of a media element: its track leaves the list (<code>removetrack</code>), a fetch is aborted,
     * the active flags of its cues are unset, and the time marches on steps run again.
     * <p>
     * <code>track</code> 요소가 더 이상 미디어 요소의 자식이 아니다: 트랙이 목록을 떠나고(<code>removetrack</code>), 가져오기가 중단되고, cue 의 활성 표시가
     * 지워지며, time marches on 단계가 다시 실행된다.
     *
     * @param m  the state of the media element it left / 요소가 떠난 미디어 요소의 상태
     * @param el the <code>track</code> element / <code>track</code> 요소
     */
    void trackRemoved(MediaElements.Media m, Element el) {
        TextTrackValue t = byElement.get(el);
        if (t == null || disposed) {
            return;
        }
        abortFetch(t);
        // 가져오는 중이던 트랙은 로드되지 않은 상태로 돌아가며 다시 넣으면 처음부터 시작한다.
        if (t.readyState == TextTrackValue.LOADING) {
            t.readyState = TextTrackValue.NONE;
            t.loadedUrl = null;
        }
        deactivate(t);
        MediaTracks mt = media.tracksOf(m);
        // 이 트랙의 cue 는 새로 들어온 cue 가 아니게 된다.
        for (int i = 0; i < t.cues.size(); i++) {
            mt.newCues.remove(t.cues.get(i));
        }
        queueTrackEvent(m, "removetrack", t);
        requestMarch(m);
    }

    /**
     * The <code>src</code> attribute of a <code>track</code> element was set, changed or removed: the list of cues is emptied at once, a fetch for
     * the old URL is aborted (with <code>error</code>), and the track processing model starts again if the mode asks for it.
     * <p>
     * <code>track</code> 요소의 <code>src</code> 속성이 설정, 변경, 제거되었다: cue 목록을 곧바로 비우고, 이전 URL 의 가져오기는 중단하며(<code>error</code> 와
     * 함께), 모드가 요구하면 트랙 처리 모델을 다시 시작한다.
     *
     * @param el the <code>track</code> element / <code>track</code> 요소
     */
    void trackSourceChanged(Element el) {
        TextTrackValue t = byElement.get(el);
        if (t == null || disposed) {
            return;
        }
        // 목록의 cue 를 모두 비운다.
        while (!t.cues.isEmpty()) {
            detach(t, t.cues.get(t.cues.size() - 1));
        }
        if (t.fetching) {
            abortFetch(t);
            t.readyState = TextTrackValue.ERROR;
            t.loadedUrl = t.attemptUrl;
            MediaElements.Media m = mediaOf(t);
            if (m != null) {
                queueElementEvent(m, el, "error");
            }
        }
        startProcessing(t);
    }

    /**
     * Cancels the fetch of a track and forgets its result.
     * <p>
     * 트랙의 가져오기를 취소하고 결과를 잊는다.
     *
     * @param t the track / 트랙
     */
    private void abortFetch(TextTrackValue t) {
        t.generation++;
        t.fetching = false;
        WebRequestSource.Call c = t.call;
        t.call = null;
        if (c != null) {
            c.cancel();
        }
    }

    /**
     * Queues a simple event at an element as a task of a media element (the events of a track element: <code>load</code>, <code>error</code>).
     * <p>
     * 미디어 요소의 작업으로 요소에 단순 이벤트를 올린다(track 요소의 이벤트: <code>load</code>, <code>error</code>).
     *
     * @param m    the state of the media element / 미디어 요소의 상태
     * @param el   the element / 요소
     * @param type the event type / 이벤트 종류
     */
    private void queueElementEvent(MediaElements.Media m, final Element el, final String type) {
        media.queueTask(m, new Runnable() {
            /**
             * Fires the event.
             * <p>
             * 이벤트를 발생시킨다.
             */
            public void run() {
                page.fireMediaEvent(el, type);
            }
        }, true);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 트랙 처리 모델(가져오기)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the track URL of a <code>track</code> element: the absolute <code>src</code>, empty if there is none or it cannot be resolved.
     * <p>
     * <code>track</code> 요소의 트랙 URL 을 반환한다: 절대 <code>src</code>. 없거나 해석할 수 없으면 빈 문자열.
     *
     * @param el the element / 요소
     * @return the URL or the empty string / URL 또는 빈 문자열
     */
    static String trackUrl(Element el) {
        String raw = el.getAttribute("src");
        if (raw == null || raw.trim().length() == 0) {
            return "";
        }
        String abs = MediaElements.absolute(el, raw.trim());
        return abs == null ? "" : abs;
    }

    /**
     * Starts the track processing model for a track (HTML 4.8.11.11.3): if the track belongs to a <code>track</code> element that is a child of a
     * media element, its mode is <code>hidden</code> or <code>showing</code>, and no fetch runs or finished for the same URL, the readiness state
     * becomes loading and the file is fetched through the request service. The request is a CORS request in the same-origin fallback mode: the
     * file must come from the same origin unless the media element has a <code>crossorigin</code> attribute.
     * <p>
     * 트랙의 트랙 처리 모델을 시작한다(HTML 4.8.11.11.3): 트랙이 미디어 요소의 자식인 <code>track</code> 요소의 것이고 모드가 <code>hidden</code> 또는
     * <code>showing</code> 이며 같은 URL 에 대해 진행 중이거나 끝난 가져오기가 없으면 준비 상태가 loading 이 되고 요청 서비스로 파일을 가져온다. 요청은
     * same-origin 대체 모드의 CORS 요청이다: 미디어 요소에 <code>crossorigin</code> 속성이 없으면 파일은 같은 오리진에서 와야 한다.
     *
     * @param t the track / 트랙
     */
    void startProcessing(final TextTrackValue t) {
        final Element el = t.element();
        if (el == null || disposed || (!"hidden".equals(t.mode) && !"showing".equals(t.mode)) || t.fetching) {
            return;
        }
        final MediaElements.Media m = mediaOf(t);
        if (m == null) {
            return;
        }
        String url = trackUrl(el);
        if (url.equals(t.loadedUrl)) {
            return;
        }
        t.generation++;
        final int gen = t.generation;
        t.attemptUrl = url;
        t.fetching = true;
        t.readyState = TextTrackValue.LOADING;
        WebRequestSource source = url.length() == 0 ? null : page.getEnvironment().getWebRequestSource();
        if (source == null) {
            // 주소가 없거나 요청 서비스가 없으면 곧바로 실패로 끝낸다.
            queueResult(m, t, gen, WebResponse.error("the track has no address or no request service", false));
            return;
        }
        WebRequest req = new WebRequest(url);
        Element mediaEl = m.element();
        String cors = mediaEl == null ? null : mediaEl.getAttribute("crossorigin");
        if (cors == null) {
            req.setMode(WebRequest.MODE_SAME_ORIGIN);
        } else {
            req.setMode(WebRequest.MODE_CORS);
            if ("use-credentials".equalsIgnoreCase(cors.trim())) {
                req.setCredentials(WebRequest.CREDENTIALS_INCLUDE);
            }
        }
        t.call = source.start(req, new WebRequestSource.Callback() {
            /**
             * Does nothing: the headers are looked at with the whole response.
             * <p>
             * 아무것도 하지 않는다: 헤더는 응답 전체와 함께 본다.
             *
             * @param head the response head / 응답 머리
             */
            public void onHeaders(WebResponse head) {
                // 응답 전체를 기다린다.
            }

            /**
             * Does nothing: the file is parsed when it arrived.
             * <p>
             * 아무것도 하지 않는다: 파일은 도착한 뒤에 해석한다.
             *
             * @param loaded the bytes so far / 지금까지의 바이트
             * @param total  the total or -1 / 전체 또는 -1
             */
            public void onProgress(long loaded, long total) {
                // 진행은 쓰지 않는다.
            }

            /**
             * Queues the response for the thread that owns the page.
             * <p>
             * 페이지를 소유한 스레드에 응답을 올린다.
             *
             * @param response the response / 응답
             */
            public void onComplete(WebResponse response) {
                queueResult(m, t, gen, response);
            }
        });
    }

    /**
     * Queues the result of a fetch for the thread that owns the page (callable from any thread; a new load of the media element does not drop it).
     * <p>
     * 가져오기의 결과를 페이지를 소유한 스레드에 올린다(어느 스레드에서나 부를 수 있다. 미디어 요소의 새 로드는 이것을 버리지 않는다).
     *
     * @param m        the state of the media element / 미디어 요소의 상태
     * @param t        the track / 트랙
     * @param gen      the number of the fetch / 가져오기 번호
     * @param response the response / 응답
     */
    private void queueResult(MediaElements.Media m, final TextTrackValue t, final int gen, final WebResponse response) {
        media.queueTask(m, new Runnable() {
            /**
             * Applies the result.
             * <p>
             * 결과를 적용한다.
             */
            public void run() {
                trackFetched(t, gen, response);
            }
        }, true);
    }

    /**
     * The fetch ended: an error, a status that is not a success or a file without the WebVTT signature fails the track (readiness state failed
     * to load, then <code>error</code> at the element); otherwise the cues join the track, the state becomes loaded and <code>load</code> fires.
     * <p>
     * 가져오기가 끝났다: 오류, 성공이 아닌 상태, WebVTT 서명이 없는 파일은 트랙을 실패시키고(준비 상태 failed to load, 이어 요소에 <code>error</code>),
     * 그렇지 않으면 cue 가 트랙에 들어가고 상태가 loaded 가 되며 <code>load</code> 가 발생한다.
     *
     * @param t        the track / 트랙
     * @param gen      the number of the fetch / 가져오기 번호
     * @param response the response / 응답
     */
    private void trackFetched(TextTrackValue t, int gen, WebResponse response) {
        if (disposed || gen != t.generation || !t.fetching) {
            return;
        }
        t.fetching = false;
        t.call = null;
        t.loadedUrl = t.attemptUrl;
        Element el = t.element();
        int status = response.getStatus();
        VttFile file = null;
        if (!response.isError() && status >= 200 && status < 300) {
            file = VttParser.parse(response.getBody());
        }
        if (file == null || !file.valid) {
            t.readyState = TextTrackValue.ERROR;
            if (el != null) {
                page.fireMediaEvent(el, "error");
            }
            return;
        }
        // 파일의 cue 를 순서대로 트랙에 넣는다.
        for (int i = 0; i < file.cues.size(); i++) {
            attach(t, newCue(file.cues.get(i)));
        }
        t.readyState = TextTrackValue.LOADED;
        if (el != null) {
            page.fireMediaEvent(el, "load");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 자동 트랙 선택
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Asks for the automatic text track selection of an element (once per element; it waits for the end of the parsing if the element came from the
     * parser, so that all its <code>track</code> children are there).
     * <p>
     * 요소의 자동 텍스트 트랙 선택을 요청한다(요소마다 한 번. 요소가 파서가 만든 것이면 <code>track</code> 자식이 모두 있도록 파싱이 끝나기를 기다린다).
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    private void queueSelection(final MediaElements.Media m) {
        final MediaTracks mt = media.tracksOf(m);
        if (mt.selectionDone || mt.selectionWaits) {
            return;
        }
        media.queueTask(m, new Runnable() {
            /**
             * Selects now, or when the parsing ended.
             * <p>
             * 지금 또는 파싱이 끝났을 때 선택한다.
             */
            public void run() {
                if (mt.selectionDone || mt.selectionWaits) {
                    return;
                }
                if (page.isParsingDocument()) {
                    mt.selectionWaits = true;
                    waiting.add(m);
                } else {
                    select(m);
                }
            }
        }, false);
    }

    /**
     * The parsing of the document ended: the automatic selection of the elements that waited for it runs.
     * <p>
     * 문서의 파싱이 끝났다: 그것을 기다리던 요소의 자동 선택을 실행한다.
     */
    void parsingFinished() {
        List<MediaElements.Media> list = new ArrayList<MediaElements.Media>(waiting);
        waiting.clear();
        // 기다리던 요소마다 선택을 작업으로 실행한다.
        for (int i = 0; i < list.size(); i++) {
            final MediaElements.Media m = list.get(i);
            media.queueTask(m, new Runnable() {
                /**
                 * Selects the tracks.
                 * <p>
                 * 트랙을 선택한다.
                 */
                public void run() {
                    m.tracks.selectionWaits = false;
                    select(m);
                }
            }, false);
        }
    }

    /**
     * Honors the preferences for the automatic text track selection (HTML 4.8.11.11.3): of the tracks of subtitles and captions, then of the tracks of
     * descriptions, the first track of a <code>track</code> element with the <code>default</code> attribute becomes <code>showing</code> unless a
     * track of these kinds is already showing; a track of chapters or metadata with <code>default</code> becomes <code>hidden</code>.
     * <p>
     * 자동 텍스트 트랙 선택의 선호를 반영한다(HTML 4.8.11.11.3): 자막/캡션 종류의 트랙, 이어서 설명 종류의 트랙 중, 해당 종류의 트랙이 이미 showing 이 아니면
     * <code>default</code> 속성이 있는 첫 <code>track</code> 요소의 트랙이 <code>showing</code> 이 된다. <code>default</code> 가 있는 chapters/metadata 트랙은
     * <code>hidden</code> 이 된다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    private void select(MediaElements.Media m) {
        MediaTracks mt = media.tracksOf(m);
        if (mt.selectionDone || disposed) {
            return;
        }
        mt.selectionDone = true;
        List<TextTrackValue> all = listOf(m);
        selectKinds(all, "subtitles", "captions");
        selectKinds(all, "descriptions", null);
        // chapters 와 metadata 트랙 중 default 가 있고 꺼져 있는 것은 hidden 으로 한다.
        for (int i = 0; i < all.size(); i++) {
            TextTrackValue t = all.get(i);
            Element el = t.element();
            String kind = t.getKind();
            if (el != null && el.hasAttribute("default") && "disabled".equals(t.mode) && ("chapters".equals(kind) || "metadata".equals(kind))) {
                setMode(t, "hidden");
            }
        }
    }

    /**
     * Performs the automatic selection for one or two kinds of tracks.
     * <p>
     * 한두 종류의 트랙에 대해 자동 선택을 수행한다.
     *
     * @param all   the tracks in order / 순서대로의 트랙
     * @param kind1 the first kind / 첫 종류
     * @param kind2 the second kind or null / 둘째 종류. 없으면 null
     */
    private void selectKinds(List<TextTrackValue> all, String kind1, String kind2) {
        List<TextTrackValue> candidates = new ArrayList<TextTrackValue>();
        // 해당 종류의 트랙을 순서대로 모은다.
        for (int i = 0; i < all.size(); i++) {
            String k = all.get(i).getKind();
            if (k.equals(kind1) || k.equals(kind2)) {
                candidates.add(all.get(i));
            }
        }
        // 이미 showing 인 트랙이 있으면 아무것도 하지 않는다.
        for (int i = 0; i < candidates.size(); i++) {
            if ("showing".equals(candidates.get(i).mode)) {
                return;
            }
        }
        // default 속성이 있고 꺼져 있는 첫 track 요소의 트랙을 showing 으로 한다.
        for (int i = 0; i < candidates.size(); i++) {
            TextTrackValue t = candidates.get(i);
            Element el = t.element();
            if (el != null && el.hasAttribute("default") && "disabled".equals(t.mode)) {
                setMode(t, "showing");
                return;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // time marches on
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Notes that the playback position of an element jumped (a seek or a loop): the next run of the time marches on steps does not look for missed
     * cues.
     * <p>
     * 요소의 재생 위치가 건너뛰었음을 기록한다(탐색 또는 반복): 다음 time marches on 단계는 놓친 cue 를 찾지 않는다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void positionJumped(MediaElements.Media m) {
        if (m.tracks != null) {
            m.tracks.discontinuity = true;
        }
    }

    /**
     * The media element was reset (the load algorithm): the active flags of all cues are unset without events and the state of the steps starts
     * over. The text tracks themselves and their cues stay.
     * <p>
     * 미디어 요소가 초기화되었다(로드 알고리즘): 모든 cue 의 활성 표시를 이벤트 없이 지우고 단계의 상태를 처음으로 되돌린다. 텍스트 트랙과 cue 는 그대로 있다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void mediaReset(MediaElements.Media m) {
        MediaTracks mt = m.tracks;
        if (mt == null) {
            return;
        }
        cancelTimer(mt);
        mt.hasLast = false;
        mt.discontinuity = false;
        mt.marchQueued = false;
        mt.changePending = false;
        List<TextTrackValue> list = listOf(m);
        // 모든 트랙의 cue 활성 표시를 지운다.
        for (int i = 0; i < list.size(); i++) {
            deactivate(list.get(i));
        }
    }

    /**
     * Stops the timer of an element.
     * <p>
     * 요소의 타이머를 멈춘다.
     *
     * @param mt the text track state of the element / 요소의 텍스트 트랙 상태
     */
    private static void cancelTimer(MediaTracks mt) {
        Scheduler.Handle h = mt.timer;
        mt.timer = null;
        if (h != null) {
            h.cancel();
        }
    }

    /**
     * Runs the time marches on steps of HTML 4.8.11.8 for a media element: works out the cues that are current at the playback position, the cues
     * that playback skipped, and queues the <code>enter</code> and <code>exit</code> events of the cues that became active or inactive in the
     * order of the specification (by time, by the text track cue order, <code>enter</code> first), then <code>cuechange</code> at each affected track
     * (and its <code>track</code> element); a cue with the pause-on-exit flag that playback left pauses the element. Then the timer for the next
     * cue boundary is set. Nothing happens until the metadata of the media is known.
     * <p>
     * 미디어 요소에 대해 HTML 4.8.11.8 의 time marches on 단계를 실행한다: 재생 위치에서 현재인 cue 와 재생이 건너뛴 cue 를 구하고, 활성 또는 비활성이 된 cue 의
     * <code>enter</code>, <code>exit</code> 이벤트를 규격의 순서(시각, 텍스트 트랙 cue 순서, <code>enter</code> 먼저)로 올리고, 영향받은 트랙(과 그
     * <code>track</code> 요소)마다 <code>cuechange</code> 를 올린다. 재생이 벗어난 pause-on-exit cue 는 요소를 일시정지한다. 이어서 다음 cue 경계의 타이머를
     * 설정한다. 미디어의 메타데이터를 알기 전에는 아무 일도 없다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void timeMarchesOn(MediaElements.Media m) {
        MediaTracks mt = m.tracks;
        if (mt == null || disposed) {
            return;
        }
        cancelTimer(mt);
        if (m.readyState < MediaElements.HAVE_METADATA) {
            return;
        }
        double pos = media.position(m);
        boolean normal = !mt.discontinuity;
        mt.discontinuity = false;
        List<TextTrackValue> list = listOf(m);
        List<TextTrackCueValue> current = new ArrayList<TextTrackCueValue>();
        List<TextTrackCueValue> other = new ArrayList<TextTrackCueValue>();
        int order = 0;
        // 트랙마다 cue 를 텍스트 트랙 cue 순서로 번호 매기고, 꺼져 있지 않은 트랙의 cue 를 현재 cue 와 그 밖의 cue 로 나눈다.
        for (int i = 0; i < list.size(); i++) {
            TextTrackValue t = list.get(i);
            boolean on = !"disabled".equals(t.mode);
            List<TextTrackCueValue> cues = t.sortedCues();
            // 트랙의 cue 를 순서대로 훑는다.
            for (int j = 0; j < cues.size(); j++) {
                TextTrackCueValue c = cues.get(j);
                c.order = order++;
                if (!on) {
                    continue;
                }
                if (c.data.startTime <= pos && c.data.endTime > pos) {
                    current.add(c);
                } else {
                    other.add(c);
                }
            }
        }
        // 정상 재생에서 건너뛴 cue: 마지막 실행 위치와 지금 위치 사이에서 시작하고 끝난 cue. 이미 활성인 cue 는 enter 를 낸 것이므로 놓친 것이 아니다
        // (규격 문구대로면 마지막 실행 위치가 cue 의 시작 시각과 정확히 같을 때 enter 가 두 번 나간다).
        Set<TextTrackCueValue> missed = new LinkedHashSet<TextTrackCueValue>();
        if (normal && mt.hasLast) {
            // 그 밖의 cue 중 건너뛴 것을 찾는다.
            for (int i = 0; i < other.size(); i++) {
                TextTrackCueValue c = other.get(i);
                if (!c.active && c.data.startTime >= mt.last && c.data.endTime <= pos) {
                    missed.add(c);
                }
            }
        }
        // 새로 들어온 cue 는 놓친 cue 가 아니다.
        missed.removeAll(new HashSet<TextTrackCueValue>(mt.newCues));
        mt.newCues.clear();
        mt.last = pos;
        mt.hasLast = true;
        boolean settled = missed.isEmpty();
        // 현재 cue 는 모두 활성이고 그 밖의 cue 는 하나도 활성이 아니면 할 일이 없다.
        for (int i = 0; i < current.size() && settled; i++) {
            if (!current.get(i).active) {
                settled = false;
            }
        }
        // 그 밖의 cue 가 하나라도 활성이면 정리할 것이 있다.
        for (int i = 0; i < other.size() && settled; i++) {
            if (other.get(i).active) {
                settled = false;
            }
        }
        if (!settled) {
            marchChanged(m, mt, list, current, other, missed, normal);
        }
        rearm(m, pos);
    }

    /**
     * The second half of the time marches on steps: some cue became active or inactive or was missed.
     * <p>
     * time marches on 단계의 뒷부분: 어떤 cue 가 활성/비활성이 되었거나 놓쳐졌다.
     *
     * @param m       the state of the media element / 미디어 요소의 상태
     * @param mt      its text track state / 그 텍스트 트랙 상태
     * @param list    the tracks in order / 순서대로의 트랙
     * @param current the cues that are current / 현재인 cue
     * @param other   the other cues of the tracks that are not disabled / 꺼져 있지 않은 트랙의 그 밖의 cue
     * @param missed  the cues that playback skipped / 재생이 건너뛴 cue
     * @param normal  true if the position moved only by normal playback / 위치가 정상 재생으로만 움직였으면 true
     */
    private void marchChanged(MediaElements.Media m, MediaTracks mt, List<TextTrackValue> list, List<TextTrackCueValue> current,
            List<TextTrackCueValue> other, Set<TextTrackCueValue> missed, boolean normal) {
        // 정상 재생에서 pause-on-exit 가 있는 cue 를 벗어났거나 건너뛰었으면 요소를 곧바로 일시정지한다.
        if (normal) {
            for (int i = 0; i < other.size(); i++) {
                TextTrackCueValue c = other.get(i);
                if (c.data.pauseOnExit && (c.active || missed.contains(c))) {
                    media.pauseNow(m);
                    break;
                }
            }
        }
        List<CueEvent> events = new ArrayList<CueEvent>();
        Set<TextTrackValue> affected = new HashSet<TextTrackValue>();
        // 건너뛴 cue 는 시작 시각의 enter 를 낸다.
        for (TextTrackCueValue c : missed) {
            events.add(new CueEvent(c, c.data.startTime, false));
            affected.add(c.track);
        }
        // 활성이었거나 건너뛴 cue 는 끝 시각(시작보다 이르지 않게)의 exit 를 낸다.
        for (int i = 0; i < other.size(); i++) {
            TextTrackCueValue c = other.get(i);
            if (c.active || missed.contains(c)) {
                events.add(new CueEvent(c, Math.max(c.data.endTime, c.data.startTime), true));
                affected.add(c.track);
            }
        }
        // 새로 활성이 되는 cue 는 시작 시각의 enter 를 낸다.
        for (int i = 0; i < current.size(); i++) {
            TextTrackCueValue c = current.get(i);
            if (!c.active) {
                events.add(new CueEvent(c, c.data.startTime, false));
                affected.add(c.track);
            }
        }
        Collections.sort(events, EVENT_ORDER);
        // 이벤트마다 미디어 요소의 작업 하나를 올린다.
        for (int i = 0; i < events.size(); i++) {
            queueCueEvent(m, events.get(i).cue, events.get(i).exit ? "exit" : "enter");
        }
        // 영향받은 트랙을 목록 순서로 훑으며 cuechange 를 올린다.
        for (int i = 0; i < list.size(); i++) {
            if (affected.contains(list.get(i))) {
                queueCueChange(m, list.get(i));
            }
        }
        // 활성 표시를 새로 정한다.
        for (int i = 0; i < current.size(); i++) {
            current.get(i).active = true;
        }
        // 그 밖의 cue 는 모두 비활성으로 한다.
        for (int i = 0; i < other.size(); i++) {
            other.get(i).active = false;
        }
    }

    /**
     * Queues <code>enter</code> or <code>exit</code> at a cue.
     * <p>
     * cue 에 <code>enter</code> 또는 <code>exit</code> 를 올린다.
     *
     * @param m    the state of the media element / 미디어 요소의 상태
     * @param cue  the cue / cue
     * @param type the event type / 이벤트 종류
     */
    private void queueCueEvent(MediaElements.Media m, final TextTrackCueValue cue, final String type) {
        media.queueTask(m, new Runnable() {
            /**
             * Fires the event at the cue.
             * <p>
             * cue 에 이벤트를 발생시킨다.
             */
            public void run() {
                page.dispatchTrustedEvent(cue, new Event(type, false, false));
            }
        }, false);
    }

    /**
     * Queues <code>cuechange</code> at a track and at its <code>track</code> element.
     * <p>
     * 트랙과 그 <code>track</code> 요소에 <code>cuechange</code> 를 올린다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     * @param t the track / 트랙
     */
    private void queueCueChange(MediaElements.Media m, final TextTrackValue t) {
        media.queueTask(m, new Runnable() {
            /**
             * Fires the event at the track, then at the element.
             * <p>
             * 트랙에, 이어서 요소에 이벤트를 발생시킨다.
             */
            public void run() {
                page.dispatchTrustedEvent(t, new Event("cuechange", false, false));
                Element el = t.element();
                if (el != null) {
                    page.fireMediaEvent(el, "cuechange");
                }
            }
        }, false);
    }

    /**
     * Sets the timer that runs the steps at the next boundary of a cue, while the element plays: the earliest start or end time of the cues of the
     * tracks that are not disabled after the position, at the speed of the playback (at most a second ahead, so that a change of the rate or a
     * pause is noticed).
     * <p>
     * 요소가 재생되는 동안 다음 cue 경계에서 단계를 실행하는 타이머를 설정한다: 꺼져 있지 않은 트랙의 cue 중 위치 뒤의 가장 이른 시작/끝 시각을 재생 속도로
     * 환산한다(속도 변경이나 일시정지를 알아채도록 최대 1초 앞까지).
     *
     * @param m   the state of the media element / 미디어 요소의 상태
     * @param pos the position now / 지금의 위치
     */
    private void rearm(final MediaElements.Media m, double pos) {
        final MediaTracks mt = m.tracks;
        cancelTimer(mt);
        if (disposed || m.paused || m.player == null) {
            return;
        }
        Scheduler scheduler = page.getEnvironment().getScheduler();
        if (scheduler == null) {
            return;
        }
        double next = Double.POSITIVE_INFINITY;
        List<TextTrackValue> list = listOf(m);
        // 꺼져 있지 않은 트랙의 모든 cue 에서 위치 뒤의 가장 이른 경계를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            TextTrackValue t = list.get(i);
            if ("disabled".equals(t.mode)) {
                continue;
            }
            // 트랙의 cue 마다 위치 뒤의 시작/끝 시각 중 가장 이른 것을 찾는다.
            for (int j = 0; j < t.cues.size(); j++) {
                VttCue d = t.cues.get(j).data;
                if (d.startTime > pos && d.startTime < next) {
                    next = d.startTime;
                }
                if (d.endTime > pos && d.endTime < next) {
                    next = d.endTime;
                }
            }
        }
        if (Double.isInfinite(next)) {
            return;
        }
        double rate = m.playbackRate > 0 ? m.playbackRate : 1;
        long delay = (long) Math.ceil((next - pos) * 1000.0 / rate);
        delay = Math.max(1, Math.min(1000, delay));
        final int epoch = m.epoch;
        try {
            mt.timer = scheduler.post(new Runnable() {
                /**
                 * Runs the steps as a page task if nothing changed meanwhile.
                 * <p>
                 * 그동안 바뀐 것이 없으면 단계를 페이지 작업으로 실행한다.
                 */
                public void run() {
                    page.runTask(new Runnable() {
                        /**
                         * Runs the steps under the page lock.
                         * <p>
                         * 페이지 잠금 아래에서 단계를 실행한다.
                         */
                        public void run() {
                            if (!disposed && epoch == m.epoch) {
                                mt.timer = null;
                                timeMarchesOn(m);
                            }
                        }
                    });
                }
            }, delay);
        } catch (RuntimeException e) {
            mt.timer = null;
        }
    }

    /**
     * Sets the timer again after a change of the state of playback (play, pause, rate), without running the steps.
     * <p>
     * 재생 상태가 바뀐 뒤(재생, 일시정지, 속도) 단계를 실행하지 않고 타이머를 다시 설정한다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void playbackChanged(MediaElements.Media m) {
        if (m.tracks != null && !disposed && m.readyState >= MediaElements.HAVE_METADATA) {
            rearm(m, media.position(m));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // getCueAsHTML
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Implements <code>getCueAsHTML()</code> (WebVTT 6.5): the cue text parsed by the cue text parsing rules becomes a document fragment: a class span
     * is a <code>span</code>, italic <code>i</code>, bold <code>b</code>, underline <code>u</code>, ruby <code>ruby</code>, ruby text <code>rt</code>,
     * a voice a <code>span</code> with a <code>title</code>, a language a <code>span</code> with a <code>lang</code>, a text a text node and a timestamp a
     * processing instruction <code>timestamp</code>; the classes become the <code>class</code> attribute.
     * <p>
     * <code>getCueAsHTML()</code> 을 구현한다(WebVTT 6.5): cue 텍스트 파싱 규칙으로 해석한 cue 텍스트가 문서 조각이 된다: 클래스 span 은 <code>span</code>, 기울임은
     * <code>i</code>, 굵게는 <code>b</code>, 밑줄은 <code>u</code>, 루비는 <code>ruby</code>, 루비 텍스트는 <code>rt</code>, 목소리는 <code>title</code> 이 있는
     * <code>span</code>, 언어는 <code>lang</code> 이 있는 <code>span</code>, 텍스트는 텍스트 노드, 타임스탬프는 처리 명령 <code>timestamp</code>. 클래스는
     * <code>class</code> 속성이 된다.
     *
     * @param cue the cue / cue
     * @return the fragment / 문서 조각
     */
    DocumentFragment cueAsHtml(TextTrackCueValue cue) {
        Document doc = page.getDocument();
        DocumentFragment frag = doc.createDocumentFragment();
        VttNode root = VttCueText.parse(cue.data.text, null);
        appendChildren(doc, frag, root);
        return frag;
    }

    /**
     * Appends the DOM nodes of the children of a cue text node.
     * <p>
     * cue 텍스트 노드의 자식에 해당하는 DOM 노드를 덧붙인다.
     *
     * @param doc    the document that owns the nodes / 노드를 소유하는 문서
     * @param parent the DOM parent / DOM 부모
     * @param node   the cue text node / cue 텍스트 노드
     */
    private void appendChildren(Document doc, Node parent, VttNode node) {
        // 자식을 순서대로 DOM 노드로 바꿔 부모에 붙인다.
        for (int i = 0; i < node.children.size(); i++) {
            VttNode c = node.children.get(i);
            if (c.kind == VttNode.TEXT) {
                parent.appendChild(doc.createTextNode(c.value));
            } else if (c.kind == VttNode.TIMESTAMP) {
                parent.appendChild(doc.createProcessingInstruction("timestamp", VttCueText.formatTimestamp(c.time)));
            } else {
                String name;
                switch (c.kind) {
                case VttNode.ITALIC:
                    name = "i";
                    break;
                case VttNode.BOLD:
                    name = "b";
                    break;
                case VttNode.UNDERLINE:
                    name = "u";
                    break;
                case VttNode.RUBY:
                    name = "ruby";
                    break;
                case VttNode.RUBY_TEXT:
                    name = "rt";
                    break;
                default:
                    name = "span";
                    break;
                }
                Element e = doc.createElement(name);
                if (c.kind == VttNode.VOICE) {
                    e.setAttribute("title", c.value);
                } else if (c.kind == VttNode.LANGUAGE) {
                    e.setAttribute("lang", c.language == null ? "" : c.language);
                }
                if (!c.classes.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    // 클래스를 스페이스 하나로 이어 class 속성으로 만든다.
                    for (int k = 0; k < c.classes.size(); k++) {
                        if (k > 0) {
                            sb.append(' ');
                        }
                        sb.append(c.classes.get(k));
                    }
                    e.setAttribute("class", sb.toString());
                }
                parent.appendChild(e);
                appendChildren(doc, e, c);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 해제
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Releases everything: running fetches and timers are cancelled and results that arrive later are ignored.
     * <p>
     * 모든 것을 해제한다: 진행 중인 가져오기와 타이머를 취소하고 나중에 도착하는 결과는 무시한다.
     */
    void dispose() {
        disposed = true;
        List<TextTrackValue> all = new ArrayList<TextTrackValue>(byElement.values());
        // 가져오는 중인 트랙의 요청을 취소한다.
        for (int i = 0; i < all.size(); i++) {
            abortFetch(all.get(i));
        }
        waiting.clear();
    }

    /**
     * Stops what runs for one media element when the page is disposed (the timer).
     * <p>
     * 페이지를 폐기할 때 미디어 요소 하나에서 실행되는 것(타이머)을 멈춘다.
     *
     * @param m the state of the media element / 미디어 요소의 상태
     */
    void disposeMedia(MediaElements.Media m) {
        if (m.tracks != null) {
            cancelTimer(m.tracks);
        }
    }
}
