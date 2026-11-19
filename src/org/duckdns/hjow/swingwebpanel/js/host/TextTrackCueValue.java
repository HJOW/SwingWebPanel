package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttCue;

/**
 * The script object of a text track cue (<code>TextTrackCue</code> and <code>VTTCue</code>, HTML 4.8.11.11.5 and WebVTT 9.1): the getters and
 * setters read and write the {@link VttCue} that it stands for, and the changes that matter to the playback (the start and end time, the
 * pause-on-exit flag) tell the text track manager so that the cues that are active are worked out again. Every cue of this component is a WebVTT cue
 * (a script cannot construct another kind). The cue is an <code>EventTarget</code>: <code>enter</code> and <code>exit</code> fire at it. The
 * members <code>region</code>, <code>line</code> and <code>position</code> (which take objects and the keyword <code>auto</code>) are defined by
 * {@link TextTrackBindings} itself. The class is public because the DOM bridge reads its accessors by reflection.
 * <p>
 * 텍스트 트랙 cue 의 스크립트 객체(<code>TextTrackCue</code>, <code>VTTCue</code>. HTML 4.8.11.11.5 와 WebVTT 9.1): getter 와 setter 가 그것이 나타내는
 * {@link VttCue} 를 읽고 쓰며, 재생에 영향을 주는 변경(시작/끝 시각, pause-on-exit 표시)은 활성 cue 를 다시 계산하도록 텍스트 트랙 관리자에 알린다. 이
 * 컴포넌트의 모든 cue 는 WebVTT cue 이다(스크립트는 다른 종류를 만들 수 없다). cue 는 <code>EventTarget</code> 이며 <code>enter</code>,
 * <code>exit</code> 가 그 위에서 발생한다. 객체와 키워드 <code>auto</code> 를 받는 <code>region</code>, <code>line</code>, <code>position</code> 은
 * {@link TextTrackBindings} 가 직접 정의한다. DOM 브리지가 접근자를 리플렉션으로 읽으므로 클래스는 public 이다.
 */
public final class TextTrackCueValue extends EventTarget {

    /**
     * The cue data.
     * <p>
     * cue 데이터.
     */
    final VttCue data;

    /**
     * The manager of the page.
     * <p>
     * 페이지의 관리자.
     */
    private final TextTracks owner;

    /**
     * The text track whose list of cues has this cue, or null.
     * <p>
     * 이 cue 가 cue 목록에 들어 있는 텍스트 트랙. 없으면 null.
     */
    TextTrackValue track;

    /**
     * The active flag (HTML 4.8.11.11.1): true from the <code>enter</code> to the <code>exit</code> of the cue.
     * <p>
     * 활성 표시(HTML 4.8.11.11.1): cue 의 <code>enter</code> 부터 <code>exit</code> 까지 true.
     */
    boolean active;

    /**
     * The order of insertion into the track, which breaks ties of the text track cue order.
     * <p>
     * 트랙에 들어간 순서. 텍스트 트랙 cue 순서의 동점을 가른다.
     */
    long seq;

    /**
     * The place in the text track cue order of one run of the time marches on steps (scratch).
     * <p>
     * time marches on 단계를 한 번 실행하는 동안의 텍스트 트랙 cue 순서의 자리(임시).
     */
    int order;

    /**
     * Creates the object of a cue.
     * <p>
     * cue 의 객체를 만든다.
     *
     * @param owner the manager / 관리자
     * @param data  the cue data / cue 데이터
     */
    TextTrackCueValue(TextTracks owner, VttCue data) {
        this.owner = owner;
        this.data = data;
    }

    /**
     * Checks a number that must be finite.
     * <p>
     * 유한해야 하는 수를 확인한다.
     *
     * @param v    the number / 수
     * @param name the attribute / 속성
     * @return the number / 수
     */
    static double finite(double v, String name) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw JsException.typeError("Failed to set the '" + name + "' property on 'VTTCue': The provided double value is non-finite.");
        }
        return v;
    }

    /**
     * Returns <code>track</code>.
     * <p>
     * <code>track</code> 을 반환한다.
     *
     * @return the text track or null / 텍스트 트랙. 없으면 null
     */
    public Object getTrack() {
        return track;
    }

    /**
     * Returns <code>id</code>.
     * <p>
     * <code>id</code> 를 반환한다.
     *
     * @return the identifier / 식별자
     */
    public String getId() {
        return data.id;
    }

    /**
     * Sets <code>id</code>.
     * <p>
     * <code>id</code> 를 정한다.
     *
     * @param v the identifier / 식별자
     */
    public void setId(String v) {
        data.id = v;
    }

    /**
     * Returns <code>startTime</code>.
     * <p>
     * <code>startTime</code> 을 반환한다.
     *
     * @return the start in seconds / 시작(초)
     */
    public double getStartTime() {
        return data.startTime;
    }

    /**
     * Sets <code>startTime</code>; the active cues are worked out again.
     * <p>
     * <code>startTime</code> 을 정한다. 활성 cue 를 다시 계산한다.
     *
     * @param v the start in seconds / 시작(초)
     */
    public void setStartTime(double v) {
        data.startTime = finite(v, "startTime");
        owner.cueTimesChanged(this);
    }

    /**
     * Returns <code>endTime</code>.
     * <p>
     * <code>endTime</code> 을 반환한다.
     *
     * @return the end in seconds, infinity for an unbounded cue / 끝(초). 끝이 없는 cue 는 무한대
     */
    public double getEndTime() {
        return data.endTime;
    }

    /**
     * Sets <code>endTime</code>: not-a-number and negative infinity are a <code>TypeError</code>; the active cues are worked out again.
     * <p>
     * <code>endTime</code> 을 정한다: NaN 과 음의 무한대는 <code>TypeError</code> 이다. 활성 cue 를 다시 계산한다.
     *
     * @param v the end in seconds / 끝(초)
     */
    public void setEndTime(double v) {
        if (Double.isNaN(v) || v == Double.NEGATIVE_INFINITY) {
            throw JsException.typeError("Failed to set the 'endTime' property on 'TextTrackCue': The provided double value is non-finite.");
        }
        data.endTime = v;
        owner.cueTimesChanged(this);
    }

    /**
     * Returns <code>pauseOnExit</code>.
     * <p>
     * <code>pauseOnExit</code> 를 반환한다.
     *
     * @return the flag / 표시
     */
    public boolean isPauseOnExit() {
        return data.pauseOnExit;
    }

    /**
     * Sets <code>pauseOnExit</code>.
     * <p>
     * <code>pauseOnExit</code> 를 정한다.
     *
     * @param v the flag / 표시
     */
    public void setPauseOnExit(boolean v) {
        data.pauseOnExit = v;
    }

    /**
     * Returns <code>vertical</code>.
     * <p>
     * <code>vertical</code> 을 반환한다.
     *
     * @return the empty string, <code>rl</code> or <code>lr</code> / 빈 문자열, <code>rl</code>, <code>lr</code>
     */
    public String getVertical() {
        return data.vertical;
    }

    /**
     * Sets <code>vertical</code>; a value that is not one of the keywords is ignored (an enumeration attribute).
     * <p>
     * <code>vertical</code> 을 정한다. 키워드가 아닌 값은 무시한다(열거형 속성).
     *
     * @param v the value / 값
     */
    public void setVertical(String v) {
        if ("".equals(v) || "rl".equals(v) || "lr".equals(v)) {
            data.vertical = v;
        }
    }

    /**
     * Returns <code>snapToLines</code>.
     * <p>
     * <code>snapToLines</code> 를 반환한다.
     *
     * @return the flag / 표시
     */
    public boolean isSnapToLines() {
        return data.snapToLines;
    }

    /**
     * Sets <code>snapToLines</code>.
     * <p>
     * <code>snapToLines</code> 를 정한다.
     *
     * @param v the flag / 표시
     */
    public void setSnapToLines(boolean v) {
        data.snapToLines = v;
    }

    /**
     * Returns <code>lineAlign</code>.
     * <p>
     * <code>lineAlign</code> 을 반환한다.
     *
     * @return the alignment / 정렬
     */
    public String getLineAlign() {
        return data.lineAlign;
    }

    /**
     * Sets <code>lineAlign</code>; a value that is not one of the keywords is ignored.
     * <p>
     * <code>lineAlign</code> 을 정한다. 키워드가 아닌 값은 무시한다.
     *
     * @param v the value / 값
     */
    public void setLineAlign(String v) {
        if ("start".equals(v) || "center".equals(v) || "end".equals(v)) {
            data.lineAlign = v;
        }
    }

    /**
     * Returns <code>positionAlign</code>.
     * <p>
     * <code>positionAlign</code> 을 반환한다.
     *
     * @return the alignment / 정렬
     */
    public String getPositionAlign() {
        return data.positionAlign;
    }

    /**
     * Sets <code>positionAlign</code>; a value that is not one of the keywords is ignored.
     * <p>
     * <code>positionAlign</code> 을 정한다. 키워드가 아닌 값은 무시한다.
     *
     * @param v the value / 값
     */
    public void setPositionAlign(String v) {
        if ("line-left".equals(v) || "center".equals(v) || "line-right".equals(v) || "auto".equals(v)) {
            data.positionAlign = v;
        }
    }

    /**
     * Returns <code>size</code>.
     * <p>
     * <code>size</code> 를 반환한다.
     *
     * @return the size in percent / 크기(백분율)
     */
    public double getSize() {
        return data.size;
    }

    /**
     * Sets <code>size</code>: a value outside 0 to 100 is an <code>IndexSizeError</code>.
     * <p>
     * <code>size</code> 를 정한다. 0~100 밖의 값은 <code>IndexSizeError</code> 이다.
     *
     * @param v the size in percent / 크기(백분율)
     */
    public void setSize(double v) {
        finite(v, "size");
        if (v < 0 || v > 100) {
            throw DomException.indexSize("Failed to set the 'size' property on 'VTTCue': The value provided (" + v + ") is outside the range (0, 100).");
        }
        data.size = v;
    }

    /**
     * Returns <code>align</code>.
     * <p>
     * <code>align</code> 을 반환한다.
     *
     * @return the alignment / 정렬
     */
    public String getAlign() {
        return data.align;
    }

    /**
     * Sets <code>align</code>; a value that is not one of the keywords is ignored.
     * <p>
     * <code>align</code> 을 정한다. 키워드가 아닌 값은 무시한다.
     *
     * @param v the value / 값
     */
    public void setAlign(String v) {
        if ("start".equals(v) || "center".equals(v) || "end".equals(v) || "left".equals(v) || "right".equals(v)) {
            data.align = v;
        }
    }

    /**
     * Returns <code>text</code>.
     * <p>
     * <code>text</code> 를 반환한다.
     *
     * @return the raw cue text / 원문 cue 텍스트
     */
    public String getText() {
        return data.text;
    }

    /**
     * Sets <code>text</code>.
     * <p>
     * <code>text</code> 를 정한다.
     *
     * @param v the raw cue text / 원문 cue 텍스트
     */
    public void setText(String v) {
        data.text = v;
    }

    /**
     * Implements <code>getCueAsHTML()</code>: the cue text as a document fragment of HTML elements (WebVTT 6.5).
     * <p>
     * <code>getCueAsHTML()</code> 를 구현한다: cue 텍스트를 HTML 요소의 문서 조각으로(WebVTT 6.5).
     *
     * @return the fragment / 문서 조각
     */
    public Object getCueAsHTML() {
        return owner.cueAsHtml(this);
    }
}
