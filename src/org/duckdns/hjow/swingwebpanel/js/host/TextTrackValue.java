package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;

/**
 * The script object of a text track (<code>TextTrack</code>, HTML 4.8.11.11.1 and 4.8.11.11.5) and the state that goes with it: the kind, label, language
 * and identifier, the readiness state, the mode, the list of cues and what the track processing model needs while the file of a
 * <code>track</code> element is fetched. A track either belongs to a <code>track</code> element (its kind, label, language and identifier are read
 * from the element when they are asked for, so they change with the attributes) or was made by <code>addTextTrack()</code> (the values are fixed). The
 * cues are kept in the text track cue order: the start time, then the end time (the later one first), then the order of insertion. <code>cuechange</code>
 * fires at the track. The class is public because the DOM bridge reads its accessors by reflection; everything that changes the state of the page
 * goes through the {@link TextTracks} manager.
 * <p>
 * 텍스트 트랙의 스크립트 객체(<code>TextTrack</code>, HTML 4.8.11.11.1, 4.8.11.11.5)와 거기 딸린 상태: 종류, 레이블, 언어, 식별자, 준비 상태, 모드, cue 목록,
 * <code>track</code> 요소의 파일을 가져오는 동안 트랙 처리 모델이 필요로 하는 것. 트랙은 <code>track</code> 요소에 속하거나(종류, 레이블, 언어, 식별자는
 * 물을 때 요소에서 읽으므로 속성을 따라 바뀐다) <code>addTextTrack()</code> 이 만든 것이다(값이 고정). cue 는 텍스트 트랙 cue 순서로 보관한다: 시작 시각,
 * 끝 시각(늦은 것 먼저), 들어온 순서. <code>cuechange</code> 는 트랙에서 발생한다. DOM 브리지가 접근자를 리플렉션으로 읽으므로 클래스는 public 이며
 * 페이지의 상태를 바꾸는 모든 것은 {@link TextTracks} 관리자를 거친다.
 */
public final class TextTrackValue extends EventTarget {

    /**
     * The readiness state: not loaded.
     * <p>
     * 준비 상태: 로드되지 않음.
     */
    static final int NONE = 0;

    /**
     * The readiness state: loading.
     * <p>
     * 준비 상태: 로드 중.
     */
    static final int LOADING = 1;

    /**
     * The readiness state: loaded.
     * <p>
     * 준비 상태: 로드됨.
     */
    static final int LOADED = 2;

    /**
     * The readiness state: failed to load.
     * <p>
     * 준비 상태: 로드 실패.
     */
    static final int ERROR = 3;

    /**
     * The kinds of text tracks in the order of the specification.
     * <p>
     * 규격 순서의 텍스트 트랙 종류.
     */
    static final String[] KINDS = { "subtitles", "captions", "descriptions", "chapters", "metadata" };

    /**
     * Orders cues as the text track cue order says: by start time, then by end time with the later end first, then by the order of insertion.
     * <p>
     * 텍스트 트랙 cue 순서대로 cue 를 정렬한다: 시작 시각, 끝 시각(늦은 것 먼저), 들어온 순서.
     */
    private static final Comparator<TextTrackCueValue> CUE_ORDER = new Comparator<TextTrackCueValue>() {
        /**
         * Compares two cues.
         * <p>
         * 두 cue 를 비교한다.
         *
         * @param a the first cue / 첫 cue
         * @param b the second cue / 둘째 cue
         * @return the sign of the order / 순서의 부호
         */
        public int compare(TextTrackCueValue a, TextTrackCueValue b) {
            if (a.data.startTime != b.data.startTime) {
                return a.data.startTime < b.data.startTime ? -1 : 1;
            }
            if (a.data.endTime != b.data.endTime) {
                return a.data.endTime > b.data.endTime ? -1 : 1;
            }
            return a.seq < b.seq ? -1 : (a.seq > b.seq ? 1 : 0);
        }
    };

    /**
     * The manager of the page.
     * <p>
     * 페이지의 관리자.
     */
    private final TextTracks owner;

    /**
     * The <code>track</code> element of the track, held weakly, or null for a track that <code>addTextTrack()</code> made.
     * <p>
     * 트랙의 <code>track</code> 요소(약한 참조). <code>addTextTrack()</code> 이 만든 트랙이면 null.
     */
    private final WeakReference<Element> element;

    /**
     * The media element that <code>addTextTrack()</code> added the track to, or null for the track of a <code>track</code> element.
     * <p>
     * <code>addTextTrack()</code> 이 트랙을 더한 미디어 요소. <code>track</code> 요소의 트랙이면 null.
     */
    final MediaElements.Media scripted;

    /**
     * The fixed kind of a scripted track.
     * <p>
     * 스크립트가 만든 트랙의 고정된 종류.
     */
    private final String fixedKind;

    /**
     * The fixed label of a scripted track.
     * <p>
     * 스크립트가 만든 트랙의 고정된 레이블.
     */
    private final String fixedLabel;

    /**
     * The fixed language of a scripted track.
     * <p>
     * 스크립트가 만든 트랙의 고정된 언어.
     */
    private final String fixedLanguage;

    /**
     * The mode: <code>disabled</code>, <code>hidden</code> or <code>showing</code>.
     * <p>
     * 모드: <code>disabled</code>, <code>hidden</code>, <code>showing</code>.
     */
    String mode = "disabled";

    /**
     * The readiness state, one of the constants of this class.
     * <p>
     * 준비 상태(이 클래스의 상수 중 하나).
     */
    int readyState = NONE;

    /**
     * The list of cues in the order of insertion.
     * <p>
     * 들어온 순서의 cue 목록.
     */
    final List<TextTrackCueValue> cues = new ArrayList<TextTrackCueValue>();

    /**
     * The cues in the text track cue order, or null if the order has to be worked out again.
     * <p>
     * 텍스트 트랙 cue 순서의 cue. 순서를 다시 계산해야 하면 null.
     */
    private List<TextTrackCueValue> sorted;

    /**
     * The counter of insertions.
     * <p>
     * 들어온 횟수의 카운터.
     */
    long seq;

    /**
     * The list object of the cues, made when it is first asked for.
     * <p>
     * cue 목록 객체. 처음 물을 때 만든다.
     */
    private TextTrackCueListValue cueList;

    /**
     * The list object of the active cues, made when it is first asked for.
     * <p>
     * 활성 cue 목록 객체. 처음 물을 때 만든다.
     */
    private TextTrackCueListValue activeList;

    /**
     * The URL of the fetch that runs or ran last.
     * <p>
     * 진행 중이거나 마지막으로 실행된 가져오기의 URL.
     */
    String attemptUrl;

    /**
     * The URL of the last fetch that ended (successfully or not), or null: the track is not fetched again while its URL stays the same.
     * <p>
     * 마지막으로 끝난 가져오기(성공이든 아니든)의 URL. 없으면 null. URL 이 같은 동안은 트랙을 다시 가져오지 않는다.
     */
    String loadedUrl;

    /**
     * The number of the current fetch: a result of an older fetch is dropped.
     * <p>
     * 현재 가져오기의 번호. 이전 가져오기의 결과는 버린다.
     */
    int generation;

    /**
     * True while a fetch runs.
     * <p>
     * 가져오기가 진행되는 동안 true.
     */
    boolean fetching;

    /**
     * The running request, or null.
     * <p>
     * 진행 중인 요청. 없으면 null.
     */
    WebRequestSource.Call call;

    /**
     * Creates the track of a <code>track</code> element.
     * <p>
     * <code>track</code> 요소의 트랙을 만든다.
     *
     * @param owner   the manager / 관리자
     * @param element the element / 요소
     */
    TextTrackValue(TextTracks owner, Element element) {
        this.owner = owner;
        this.element = new WeakReference<Element>(element);
        this.scripted = null;
        this.fixedKind = null;
        this.fixedLabel = null;
        this.fixedLanguage = null;
    }

    /**
     * Creates a track that <code>addTextTrack()</code> makes: its mode is <code>hidden</code> and it is loaded.
     * <p>
     * <code>addTextTrack()</code> 이 만드는 트랙을 만든다: 모드는 <code>hidden</code> 이고 로드된 상태이다.
     *
     * @param owner    the manager / 관리자
     * @param media    the media element / 미디어 요소
     * @param kind     the kind / 종류
     * @param label    the label / 레이블
     * @param language the language / 언어
     */
    TextTrackValue(TextTracks owner, MediaElements.Media media, String kind, String label, String language) {
        this.owner = owner;
        this.element = null;
        this.scripted = media;
        this.fixedKind = kind;
        this.fixedLabel = label;
        this.fixedLanguage = language;
        this.mode = "hidden";
        this.readyState = LOADED;
    }

    /**
     * Returns the <code>track</code> element, or null (for a scripted track, or after the element was collected).
     * <p>
     * <code>track</code> 요소를 반환한다. 스크립트가 만든 트랙이거나 요소가 수거되었으면 null.
     *
     * @return the element or null / 요소. 없으면 null
     */
    Element element() {
        return element == null ? null : element.get();
    }

    /**
     * Returns the kind that a <code>kind</code> attribute stands for: the keyword that matches without regard to ASCII case, <code>subtitles</code> if the
     * attribute is missing, and <code>metadata</code> if its value is none of the keywords.
     * <p>
     * <code>kind</code> 속성이 나타내는 종류를 반환한다: ASCII 대소문자를 가리지 않고 일치하는 키워드, 속성이 없으면 <code>subtitles</code>, 값이 어떤
     * 키워드도 아니면 <code>metadata</code>.
     *
     * @param attribute the value of the attribute, or null / 속성 값. 없으면 null
     * @return the kind / 종류
     */
    static String canonicalKind(String attribute) {
        if (attribute == null) {
            return KINDS[0];
        }
        // 키워드와 비교한다(ASCII 대소문자 무시).
        for (int i = 0; i < KINDS.length; i++) {
            if (KINDS[i].equalsIgnoreCase(attribute)) {
                return KINDS[i];
            }
        }
        return KINDS[4];
    }

    /**
     * Returns <code>kind</code>.
     * <p>
     * <code>kind</code> 를 반환한다.
     *
     * @return the kind / 종류
     */
    public String getKind() {
        Element e = element();
        return fixedKind != null ? fixedKind : canonicalKind(e == null ? null : e.getAttribute("kind"));
    }

    /**
     * Returns <code>label</code>.
     * <p>
     * <code>label</code> 을 반환한다.
     *
     * @return the label, empty if there is none / 레이블. 없으면 빈 문자열
     */
    public String getLabel() {
        Element e = element();
        if (fixedLabel != null) {
            return fixedLabel;
        }
        String v = e == null ? null : e.getAttribute("label");
        return v == null ? "" : v;
    }

    /**
     * Returns <code>language</code>.
     * <p>
     * <code>language</code> 를 반환한다.
     *
     * @return the language, empty if there is none / 언어. 없으면 빈 문자열
     */
    public String getLanguage() {
        Element e = element();
        if (fixedLanguage != null) {
            return fixedLanguage;
        }
        String v = e == null ? null : e.getAttribute("srclang");
        return v == null ? "" : v;
    }

    /**
     * Returns <code>id</code>: the <code>id</code> of the <code>track</code> element, empty for a scripted track.
     * <p>
     * <code>id</code> 를 반환한다: <code>track</code> 요소의 <code>id</code>. 스크립트가 만든 트랙은 빈 문자열.
     *
     * @return the identifier / 식별자
     */
    public String getId() {
        Element e = element();
        String v = e == null ? null : e.getAttribute("id");
        return v == null ? "" : v;
    }

    /**
     * Returns <code>inBandMetadataTrackDispatchType</code>: always empty, because no track comes from inside a media resource.
     * <p>
     * <code>inBandMetadataTrackDispatchType</code> 을 반환한다: 미디어 리소스 안에서 오는 트랙이 없으므로 항상 빈 문자열.
     *
     * @return an empty string / 빈 문자열
     */
    public String getInBandMetadataTrackDispatchType() {
        return "";
    }

    /**
     * Returns <code>mode</code>.
     * <p>
     * <code>mode</code> 를 반환한다.
     *
     * @return <code>disabled</code>, <code>hidden</code> or <code>showing</code> / <code>disabled</code>, <code>hidden</code>, <code>showing</code>
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets <code>mode</code>; a value that is none of the keywords is ignored (an enumeration attribute).
     * <p>
     * <code>mode</code> 를 정한다. 키워드가 아닌 값은 무시한다(열거형 속성).
     *
     * @param v the mode / 모드
     */
    public void setMode(String v) {
        if ("disabled".equals(v) || "hidden".equals(v) || "showing".equals(v)) {
            owner.setMode(this, v);
        }
    }

    /**
     * Returns <code>cues</code>: null while the mode is <code>disabled</code>, else the live list of the cues (the same object every time).
     * <p>
     * <code>cues</code> 를 반환한다: 모드가 <code>disabled</code> 인 동안은 null, 아니면 cue 의 살아 있는 목록(매번 같은 객체).
     *
     * @return the list or null / 목록. 없으면 null
     */
    public Object getCues() {
        if ("disabled".equals(mode)) {
            return null;
        }
        if (cueList == null) {
            cueList = new TextTrackCueListValue(this, false);
        }
        return cueList;
    }

    /**
     * Returns <code>activeCues</code>: null while the mode is <code>disabled</code>, else the live list of the cues that are active.
     * <p>
     * <code>activeCues</code> 를 반환한다: 모드가 <code>disabled</code> 인 동안은 null, 아니면 활성인 cue 의 살아 있는 목록.
     *
     * @return the list or null / 목록. 없으면 null
     */
    public Object getActiveCues() {
        if ("disabled".equals(mode)) {
            return null;
        }
        if (activeList == null) {
            activeList = new TextTrackCueListValue(this, true);
        }
        return activeList;
    }

    /**
     * Implements <code>addCue(cue)</code>.
     * <p>
     * <code>addCue(cue)</code> 를 구현한다.
     *
     * @param cue the cue / cue
     */
    public void addCue(Object cue) {
        owner.addCue(this, cue);
    }

    /**
     * Implements <code>removeCue(cue)</code>: a cue that is not in the list is a <code>NotFoundError</code>.
     * <p>
     * <code>removeCue(cue)</code> 를 구현한다: 목록에 없는 cue 는 <code>NotFoundError</code> 이다.
     *
     * @param cue the cue / cue
     */
    public void removeCue(Object cue) {
        owner.removeCue(this, cue);
    }

    /**
     * Marks the order of the cues as stale: it is worked out again at the next use.
     * <p>
     * cue 의 순서를 낡은 것으로 표시한다: 다음에 쓸 때 다시 계산한다.
     */
    void invalidate() {
        sorted = null;
    }

    /**
     * Returns the cues in the text track cue order (a copy that the caller may keep but not change).
     * <p>
     * cue 를 텍스트 트랙 cue 순서로 반환한다(호출자가 보관할 수 있지만 바꾸면 안 되는 복사본).
     *
     * @return the cues / cue
     */
    List<TextTrackCueValue> sortedCues() {
        if (sorted == null) {
            List<TextTrackCueValue> l = new ArrayList<TextTrackCueValue>(cues);
            Collections.sort(l, CUE_ORDER);
            sorted = Collections.unmodifiableList(l);
        }
        return sorted;
    }

    /**
     * Returns the cues of <code>cues</code>: those whose end time is not before the earliest possible position (0), in order.
     * <p>
     * <code>cues</code> 의 cue 를 반환한다: 끝 시각이 가장 이른 위치(0)보다 앞서지 않은 cue 를 순서대로.
     *
     * @return the cues / cue
     */
    List<TextTrackCueValue> cuesNow() {
        List<TextTrackCueValue> all = sortedCues();
        List<TextTrackCueValue> out = new ArrayList<TextTrackCueValue>(all.size());
        // 끝 시각이 0 이상인 cue 만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).data.endTime >= 0) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /**
     * Returns the cues that are active, in order.
     * <p>
     * 활성인 cue 를 순서대로 반환한다.
     *
     * @return the cues / cue
     */
    List<TextTrackCueValue> activeCuesNow() {
        List<TextTrackCueValue> all = sortedCues();
        List<TextTrackCueValue> out = new ArrayList<TextTrackCueValue>();
        // 활성 표시가 있는 cue 만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).active) {
                out.add(all.get(i));
            }
        }
        return out;
    }
}
