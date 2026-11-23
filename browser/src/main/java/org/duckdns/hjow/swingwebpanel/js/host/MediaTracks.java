package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;

/**
 * What a media element keeps about its text tracks (HTML 4.8.11.11.1 and 4.8.11.8): the tracks that <code>addTextTrack()</code> made, the list object
 * of <code>textTracks</code>, the list of newly introduced cues and the state of the time marches on steps (the position at the last run, whether
 * the position jumped, the timer for the next cue boundary) and of the automatic track selection. Only {@link TextTracks} and
 * {@link MediaElements} read and write it, on the thread that owns the page.
 * <p>
 * 미디어 요소가 텍스트 트랙에 대해 보관하는 것(HTML 4.8.11.11.1, 4.8.11.8): <code>addTextTrack()</code> 이 만든 트랙, <code>textTracks</code> 의 목록 객체, 새로
 * 들어온 cue 의 목록, time marches on 단계의 상태(마지막 실행 때의 위치, 위치가 건너뛰었는지, 다음 cue 경계의 타이머), 자동 트랙 선택의 상태.
 * {@link TextTracks} 와 {@link MediaElements} 만 페이지를 소유한 스레드에서 읽고 쓴다.
 */
final class MediaTracks {

    /**
     * The tracks that <code>addTextTrack()</code> made, in the order they were added.
     * <p>
     * <code>addTextTrack()</code> 이 만든 트랙(더한 순서).
     */
    final List<TextTrackValue> scripted = new ArrayList<TextTrackValue>();

    /**
     * The list object of <code>textTracks</code>, made when it is first asked for.
     * <p>
     * <code>textTracks</code> 의 목록 객체. 처음 물을 때 만든다.
     */
    TextTrackListValue list;

    /**
     * The list of newly introduced cues: cues that came into the tracks since the last run of the time marches on steps.
     * <p>
     * 새로 들어온 cue 의 목록: time marches on 단계를 마지막으로 실행한 뒤 트랙에 들어온 cue.
     */
    final List<TextTrackCueValue> newCues = new ArrayList<TextTrackCueValue>();

    /**
     * The playback position at the last run of the time marches on steps.
     * <p>
     * time marches on 단계를 마지막으로 실행했을 때의 재생 위치.
     */
    double last;

    /**
     * True once the time marches on steps ran (so {@link #last} has a value).
     * <p>
     * time marches on 단계를 실행한 적이 있으면 true({@link #last} 에 값이 있다).
     */
    boolean hasLast;

    /**
     * True if the position changed other than by the usual increase of normal playback (a seek or a loop) since the last run.
     * <p>
     * 마지막 실행 뒤로 위치가 정상 재생의 보통 증가가 아닌 방식(탐색, 반복)으로 바뀌었으면 true.
     */
    boolean discontinuity;

    /**
     * True while a <code>change</code> event of the track list waits (the pending text track change notification flag).
     * <p>
     * 트랙 목록의 <code>change</code> 이벤트가 기다리는 동안 true(pending text track change notification flag).
     */
    boolean changePending;

    /**
     * True while a run of the time marches on steps waits in the task queue (a change of cues asked for it).
     * <p>
     * 큐의 변경이 요청한 time marches on 단계의 실행이 작업 줄에서 기다리는 동안 true.
     */
    boolean marchQueued;

    /**
     * True once the automatic text track selection ran for the element (the did-perform-automatic-track-selection flag).
     * <p>
     * 요소의 자동 텍스트 트랙 선택이 실행된 뒤에는 true(did-perform-automatic-track-selection flag).
     */
    boolean selectionDone;

    /**
     * True while the automatic selection waits for the end of the parsing (the element was made by the parser).
     * <p>
     * 요소가 파서가 만든 것이어서 자동 선택이 파싱이 끝나기를 기다리는 동안 true.
     */
    boolean selectionWaits;

    /**
     * The timer that runs the time marches on steps at the next cue boundary, or null.
     * <p>
     * 다음 cue 경계에서 time marches on 단계를 실행하는 타이머. 없으면 null.
     */
    Scheduler.Handle timer;
}
