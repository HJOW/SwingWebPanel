package org.duckdns.hjow.swingwebpanel.js.host;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutput;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaInfo;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaPlayer;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaRanges;
import org.duckdns.hjow.swingwebpanel.media.playback.PlaybackListener;
import org.duckdns.hjow.swingwebpanel.media.webaudio.MediaElementTap;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.MediaControlSource;
import org.duckdns.hjow.swingwebpanel.render.MediaControlState;
import org.duckdns.hjow.swingwebpanel.render.MediaControls;

/**
 * The state machine of the <code>audio</code> and <code>video</code> elements of one page (plan items P18-05-c, P18-08, P18-09), after
 * HTML5 (W3C Recommendation 28 October 2014) section 4.7.10 "Media elements": the media element load algorithm, the resource selection
 * algorithm with <code>src</code> and <code>source</code> children, the network and ready states, the playing, pausing and seeking
 * algorithms with their events, the volume and the time ranges. The bytes of the resource come from the component through the
 * {@link MediaRequestSource}, the sound is played by a {@link MediaPlayer} per loaded resource, and the sound output comes from the
 * {@link HostEnvironment}.
 * <p>
 * Every algorithm runs on the thread that owns the DOM of the page. What happens on other threads (the end of a fetch, the reports of the
 * player) only becomes a {@link Task} in the result queue of the page ({@link ScriptLoads}), so it reaches the DOM owner at the same safe
 * points as an async script, and "queue a task to fire an event" really is a task. A task carries the load generation of its element
 * ({@link Media#epoch}); a new load removes the tasks of the old one, as the specification requires, and a closed page delivers nothing.
 * An element delays the window <code>load</code> event from the start of its resource selection until its first frame of data is ready (or it
 * failed); a media element that plays keeps itself reachable (the specification lets a playing element play without any reference), any other
 * element is held weakly, and its player ends when the element is collected.
 * <p>
 * Differences from the Recommendation (the more recent HTML standard in these points): <code>play()</code> returns a promise; a media resource
 * is fetched as a whole (progress is reported once, at the end, and <code>preload=none</code> defers the fetch until <code>play()</code>);
 * <code>video</code> elements never load: their resource selection fails with <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> (plan item P18-09);
 * the text tracks (<code>track</code> elements, <code>addTextTrack()</code>, WebVTT cues and their events) are kept by {@link TextTracks}, which
 * this class tells about every change of the playback position; there are no audio or video tracks and no media controllers. The promise of
 * <code>play()</code> is fulfilled in the task that fires <code>playing</code> (WHATWG "notify about playing the media element"), never by the
 * sound device. A playback failure after loading is the fatal decoding error of 4.7.10.5 (error <code>MEDIA_ERR_DECODE</code>, the element is not
 * paused; a machine without a sound device is reported the same way).
 * <p>
 * 한 페이지의 <code>audio</code>, <code>video</code> 요소의 상태 기계(계획 P18-05-c, P18-08, P18-09). HTML5(W3C 권고안 2014-10-28) 4.7.10
 * "미디어 요소"를 따른다: 미디어 요소 로드 알고리즘, <code>src</code> 와 <code>source</code> 자식의 리소스 선택 알고리즘, 네트워크/준비 상태, 이벤트가
 * 있는 재생/일시정지/탐색 알고리즘, 음량, 시간 범위. 리소스 바이트는 {@link MediaRequestSource} 로 컴포넌트에서 오고, 소리는 로드한 리소스마다 있는
 * {@link MediaPlayer} 가 재생하며, 소리 출력은 {@link HostEnvironment} 에서 온다.
 * <p>
 * 모든 알고리즘은 페이지의 DOM 을 소유한 스레드에서 실행된다. 다른 스레드에서 일어나는 일(가져오기의 끝, 재생기의 보고)은 페이지의 결과 줄
 * ({@link ScriptLoads})에 {@link Task} 가 될 뿐이므로 async 스크립트와 같은 안전한 지점에서 DOM 소유자에게 도달하며, "이벤트를 발생시키는 작업을
 * 올린다"는 정말 작업이다. 작업은 요소의 로드 세대({@link Media#epoch})를 지닌다. 명세대로 새 로드는 이전 로드의 작업을 없애며, 닫힌 페이지는
 * 아무것도 전달하지 않는다. 요소는 리소스 선택을 시작할 때부터 첫 데이터가 준비될 때까지(또는 실패할 때까지) window <code>load</code> 이벤트를
 * 늦춘다. 재생 중인 미디어 요소는 스스로 도달 가능하게 유지하고(명세는 참조가 없어도 재생을 허용한다) 그 밖의 요소는 약하게 잡으며, 요소가
 * 수거되면 재생기가 끝난다.
 * <p>
 * 권고안과 다른 점(이 부분의 더 최근 HTML 표준을 따름): <code>play()</code> 는 프라미스를 반환한다. 미디어 리소스는 통째로 가져온다(진행 보고는 끝에서
 * 한 번이며 <code>preload=none</code> 은 <code>play()</code> 까지 가져오기를 미룬다). <code>video</code> 요소는 로드하지 않는다: 리소스 선택은
 * <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 로 실패한다(계획 P18-09). 1.0 이 아닌 재생 속도는 <code>NotSupportedError</code> 로 거절한다. 텍스트 트랙(<code>track</code> 요소,
 * <code>addTextTrack()</code>, WebVTT cue 와 그 이벤트)은 {@link TextTracks} 가 관리하며 이 클래스는 재생 위치가 바뀔 때마다 그것에 알린다. 오디오/비디오 트랙과
 * 미디어 컨트롤러는 없다. <code>play()</code> 의 프라미스는 소리 장치가 아니라 <code>playing</code> 을 내는 작업에서 이행된다(WHATWG "notify about playing the media
 * element"). 로드 뒤의 재생 실패는 4.7.10.5 의 치명적 디코딩 오류이다(오류 <code>MEDIA_ERR_DECODE</code>, 요소는 일시정지되지 않는다. 소리 장치가 없는 컴퓨터도
 * 같게 알린다).
 */
final class MediaElements implements MediaControlSource {

    /**
     * <code>networkState</code>: the element is not initialized.
     * <p>
     * <code>networkState</code>: 요소가 초기화되지 않았다.
     */
    static final int NETWORK_EMPTY = 0;

    /**
     * <code>networkState</code>: a resource was selected, but the network is not used right now.
     * <p>
     * <code>networkState</code>: 리소스를 골랐지만 지금은 네트워크를 쓰지 않는다.
     */
    static final int NETWORK_IDLE = 1;

    /**
     * <code>networkState</code>: the resource is being fetched.
     * <p>
     * <code>networkState</code>: 리소스를 가져오는 중이다.
     */
    static final int NETWORK_LOADING = 2;

    /**
     * <code>networkState</code>: no usable resource was found.
     * <p>
     * <code>networkState</code>: 쓸 수 있는 리소스를 찾지 못했다.
     */
    static final int NETWORK_NO_SOURCE = 3;

    /**
     * <code>readyState</code>: no information about the resource.
     * <p>
     * <code>readyState</code>: 리소스에 대한 정보가 없다.
     */
    static final int HAVE_NOTHING = 0;

    /**
     * <code>readyState</code>: the duration is known, no data for the current position.
     * <p>
     * <code>readyState</code>: 길이를 알지만 현재 위치의 데이터는 없다.
     */
    static final int HAVE_METADATA = 1;

    /**
     * <code>readyState</code>: data for the current position, not enough to go on.
     * <p>
     * <code>readyState</code>: 현재 위치의 데이터는 있지만 계속하기에는 부족하다.
     */
    static final int HAVE_CURRENT_DATA = 2;

    /**
     * <code>readyState</code>: data to advance a little.
     * <p>
     * <code>readyState</code>: 조금 진행할 데이터가 있다.
     */
    static final int HAVE_FUTURE_DATA = 3;

    /**
     * <code>readyState</code>: enough data to play to the end.
     * <p>
     * <code>readyState</code>: 끝까지 재생할 데이터가 충분하다.
     */
    static final int HAVE_ENOUGH_DATA = 4;

    /**
     * The code of <code>MEDIA_ERR_ABORTED</code>.
     * <p>
     * <code>MEDIA_ERR_ABORTED</code> 의 코드.
     */
    static final int MEDIA_ERR_ABORTED = 1;

    /**
     * The code of <code>MEDIA_ERR_DECODE</code>.
     * <p>
     * <code>MEDIA_ERR_DECODE</code> 의 코드.
     */
    static final int MEDIA_ERR_DECODE = 3;

    /**
     * The code of <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code>.
     * <p>
     * <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 의 코드.
     */
    static final int MEDIA_ERR_SRC_NOT_SUPPORTED = 4;

    /**
     * Task kind: fire the event of the task at its target.
     * <p>
     * 작업 종류: 작업의 이벤트를 대상에 발생시킨다.
     */
    private static final int K_EVENT = 0;

    /**
     * Task kind: the synchronous section of the resource selection algorithm (after a stable state).
     * <p>
     * 작업 종류: 리소스 선택 알고리즘의 동기 구간(안정 상태 뒤).
     */
    private static final int K_SELECT = 1;

    /**
     * Task kind: look for the next candidate <code>source</code>.
     * <p>
     * 작업 종류: 다음 후보 <code>source</code> 를 찾는다.
     */
    private static final int K_ADVANCE = 2;

    /**
     * Task kind: the fetch of the resource ended.
     * <p>
     * 작업 종류: 리소스 가져오기가 끝났다.
     */
    private static final int K_FETCHED = 3;

    /**
     * Task kind: the player opened the data.
     * <p>
     * 작업 종류: 재생기가 데이터를 열었다.
     */
    private static final int K_LOADED = 4;

    /**
     * Task kind: the player could not open the data.
     * <p>
     * 작업 종류: 재생기가 데이터를 열지 못했다.
     */
    private static final int K_LOAD_FAILED = 5;

    /**
     * Task kind: the player finished a seek.
     * <p>
     * 작업 종류: 재생기가 탐색을 끝냈다.
     */
    private static final int K_SEEKED = 6;

    /**
     * Task kind: the player wrapped around because of the loop attribute.
     * <p>
     * 작업 종류: 재생기가 loop 속성 때문에 처음으로 되돌아갔다.
     */
    private static final int K_LOOPED = 7;

    /**
     * Task kind: the player reached the end of the media.
     * <p>
     * 작업 종류: 재생기가 미디어의 끝에 닿았다.
     */
    private static final int K_ENDED = 8;

    /**
     * Task kind: the duration became known.
     * <p>
     * 작업 종류: 길이를 알게 되었다.
     */
    private static final int K_DURATION = 9;

    /**
     * Task kind: the playback position moved.
     * <p>
     * 작업 종류: 재생 위치가 움직였다.
     */
    private static final int K_TIME = 10;

    /**
     * Task kind: the playback failed.
     * <p>
     * 작업 종류: 재생이 실패했다.
     */
    private static final int K_FAILED = 11;

    /**
     * Task kind: the sound began.
     * <p>
     * 작업 종류: 소리가 시작되었다.
     */
    private static final int K_STARTED = 14;

    /**
     * Task kind: stop delaying the window load event.
     * <p>
     * 작업 종류: window load 이벤트를 그만 늦춘다.
     */
    private static final int K_RELEASE = 12;

    /**
     * Task kind: pause the element if it is not in the document any more.
     * <p>
     * 작업 종류: 요소가 문서에 더 없으면 일시정지한다.
     */
    private static final int K_PAUSE_CHECK = 13;

    /**
     * Task kind: more bytes of the resource arrived (fires <code>progress</code>).
     * <p>
     * 작업 종류: 리소스의 바이트가 더 도착했다(<code>progress</code> 를 발생시킨다).
     */
    private static final int K_PROGRESS = 15;

    /**
     * Task kind: run the code that a text track operation queued (an event of a text track, a result of the fetch of a text track).
     * <p>
     * 작업 종류: 텍스트 트랙 동작이 올린 코드를 실행한다(텍스트 트랙의 이벤트, 텍스트 트랙 가져오기의 결과).
     */
    private static final int K_RUN = 16;

    /**
     * Task kind: resolve the <code>play()</code> promises that were taken from the pending list when the task was queued (HTML "take pending
     * play promises" and "resolve pending play promises" for a <code>play()</code> call on an element that already plays).
     * <p>
     * 작업 종류: 작업을 줄 세울 때 대기 목록에서 가져온 <code>play()</code> 프라미스를 이행한다(이미 재생 중인 요소에 대한 <code>play()</code> 호출의 HTML
     * "take pending play promises" 와 "resolve pending play promises").
     */
    private static final int K_RESOLVE = 17;

    /**
     * How long the transfer may give no data before <code>stalled</code> fires, in milliseconds (HTML5 4.7.10.5: three seconds).
     * <p>
     * 전송이 데이터를 주지 않아도 <code>stalled</code> 가 발생하기 전까지의 시간(밀리초; HTML5 4.7.10.5: 3초).
     */
    static final long STALL_MILLIS = 3000;

    /**
     * One task for the owner of the DOM: the result of a fetch, a report of a player, or an event that was queued.
     * <p>
     * DOM 소유자를 위한 작업 하나: 가져오기의 결과, 재생기의 보고, 또는 올려 둔 이벤트.
     */
    static final class Task {
        /**
         * The element state that the task belongs to.
         * <p>
         * 작업이 속한 요소 상태.
         */
        final Media media;

        /**
         * The load generation of the element when the task was queued; an older task is dropped.
         * <p>
         * 작업을 올릴 때의 요소의 로드 세대. 오래된 작업은 버린다.
         */
        final int epoch;

        /**
         * What to do.
         * <p>
         * 할 일.
         */
        final int kind;

        /**
         * The name of the event.
         * <p>
         * 이벤트 이름.
         */
        String name;

        /**
         * The target of the event, if it is not the media element itself (a <code>source</code> element).
         * <p>
         * 이벤트의 대상(미디어 요소 자신이 아닐 때. <code>source</code> 요소).
         */
        Element target;

        /**
         * The facts of a loaded media, or the exception of a failure.
         * <p>
         * 로드된 미디어의 정보 또는 실패의 예외.
         */
        Object arg;

        /**
         * The fetched bytes.
         * <p>
         * 가져온 바이트.
         */
        byte[] data;

        /**
         * The MIME type of the fetched resource.
         * <p>
         * 가져온 리소스의 MIME 타입.
         */
        String mime;

        /**
         * The failure of a fetch, or null.
         * <p>
         * 가져오기의 실패 원인. 없으면 null.
         */
        String error;

        /**
         * A number, the duration of a duration task.
         * <p>
         * 숫자. 길이 작업의 길이.
         */
        double number;

        /**
         * True if a new load of the element does not drop the task (the result of the fetch of a text track, which does not depend on the load of the
         * media resource).
         * <p>
         * 요소의 새 로드가 작업을 버리지 않으면 true(미디어 리소스의 로드와 무관한 텍스트 트랙 가져오기의 결과).
         */
        boolean sticky;

        /**
         * The <code>play()</code> promises that this task resolves after its event (taken from the pending list when the task was queued), or
         * null.
         * <p>
         * 이 작업이 이벤트 뒤에 이행하는 <code>play()</code> 프라미스(작업을 줄 세울 때 대기 목록에서 가져온 것). 없으면 null.
         */
        List<Deferred> promises;

        /**
         * Creates a task.
         * <p>
         * 작업을 만든다.
         *
         * @param media the element state / 요소 상태
         * @param epoch the load generation / 로드 세대
         * @param kind  what to do / 할 일
         */
        Task(Media media, int epoch, int kind) {
            this.media = media;
            this.epoch = epoch;
            this.kind = kind;
        }
    }

    /**
     * A promise together with its resolving functions (the promise of <code>play()</code>).
     * <p>
     * 프라미스와 그 확정 함수(<code>play()</code> 의 프라미스).
     */
    static final class Deferred {
        /**
         * The promise object.
         * <p>
         * 프라미스 객체.
         */
        final JsObject promise;

        /**
         * The resolve function.
         * <p>
         * resolve 함수.
         */
        private final JsValue resolveFn;

        /**
         * The reject function.
         * <p>
         * reject 함수.
         */
        private final JsValue rejectFn;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param promise the promise / 프라미스
         * @param resolve the resolve function / resolve 함수
         * @param reject  the reject function / reject 함수
         */
        Deferred(JsObject promise, JsValue resolve, JsValue reject) {
            this.promise = promise;
            this.resolveFn = resolve;
            this.rejectFn = reject;
        }

        /**
         * Resolves the promise with undefined.
         * <p>
         * 프라미스를 undefined 로 resolve 한다.
         */
        void resolve() {
            Ops.call(resolveFn, Js.UNDEFINED, new JsValue[] { Js.UNDEFINED });
        }

        /**
         * Rejects the promise.
         * <p>
         * 프라미스를 reject 한다.
         *
         * @param reason the reason / 이유
         */
        void reject(JsValue reason) {
            Ops.call(rejectFn, Js.UNDEFINED, new JsValue[] { reason });
        }
    }

    /**
     * The state of one media element.
     * <p>
     * 미디어 요소 하나의 상태.
     */
    final class Media {
        /**
         * The element, held weakly (a playing element is held by {@link MediaElements#playing}).
         * <p>
         * 요소(약한 참조. 재생 중인 요소는 {@link MediaElements#playing} 이 붙잡는다).
         */
        final WeakReference<Element> element;

        /**
         * True for a <code>video</code> element.
         * <p>
         * <code>video</code> 요소이면 true.
         */
        final boolean video;

        /**
         * The load generation; every media element load algorithm starts a new one.
         * <p>
         * 로드 세대. 미디어 요소 로드 알고리즘이 시작될 때마다 새 세대가 된다.
         */
        int epoch;

        /**
         * The value of <code>networkState</code>, one of the <code>NETWORK_</code> constants.
         * <p>
         * <code>networkState</code> 의 값(<code>NETWORK_</code> 상수 중 하나).
         */
        int networkState = NETWORK_EMPTY;

        /**
         * The value of <code>readyState</code>, one of the <code>HAVE_</code> constants.
         * <p>
         * <code>readyState</code> 의 값(<code>HAVE_</code> 상수 중 하나).
         */
        int readyState = HAVE_NOTHING;

        /**
         * The current error, or null.
         * <p>
         * 현재의 오류. 없으면 null.
         */
        MediaErrorValue error;

        /**
         * The value of <code>currentSrc</code>, the absolute URL of the resource that is used (empty if none).
         * <p>
         * <code>currentSrc</code> 의 값. 사용 중인 리소스의 절대 URL(없으면 빈 문자열).
         */
        String currentSrc = "";

        /**
         * The value of <code>paused</code>: true until playback was requested.
         * <p>
         * <code>paused</code> 의 값. 재생을 요청하기 전까지 true.
         */
        boolean paused = true;

        /**
         * The value of <code>seeking</code>: true while a seek is under way.
         * <p>
         * <code>seeking</code> 의 값. 탐색이 진행되는 동안 true.
         */
        boolean seeking;

        /**
         * The autoplaying flag: true until a script plays or pauses the element.
         * <p>
         * autoplaying 플래그: 스크립트가 재생하거나 일시정지하기 전까지 true.
         */
        boolean autoplaying = true;

        /**
         * True from the report that the sound began until the element is paused, ended or reloaded.
         * <p>
         * 소리가 시작되었다는 보고부터 요소가 일시정지되거나 끝나거나 다시 로드될 때까지 true.
         */
        boolean started;

        /**
         * True while the element delays the window load event.
         * <p>
         * 요소가 window load 이벤트를 늦추는 동안 true.
         */
        boolean delaying;

        /**
         * True once a load algorithm ran for the element.
         * <p>
         * 요소에 대해 로드 알고리즘이 실행된 적이 있으면 true.
         */
        boolean everLoaded;

        /**
         * The default playback start position: the value of <code>currentTime</code> that was set before the metadata.
         * <p>
         * 기본 재생 시작 위치: 메타데이터 전에 설정된 <code>currentTime</code> 값.
         */
        double defaultStart;

        /**
         * The volume factor, 0 to 1.
         * <p>
         * 음량 배율(0에서 1).
         */
        double volume = 1;

        /**
         * True if muted.
         * <p>
         * 음소거이면 true.
         */
        boolean muted;

        /**
         * The value of <code>defaultPlaybackRate</code>: the rate that a new load starts with (1.0 normal speed).
         * <p>
         * <code>defaultPlaybackRate</code> 의 값: 새 로드가 시작하는 속도(1.0 이 보통 속도).
         */
        double defaultRate = 1.0;

        /**
         * The value of <code>playbackRate</code>: the speed at which the resource plays now.
         * <p>
         * <code>playbackRate</code> 의 값: 리소스가 지금 재생되는 속도.
         */
        double playbackRate = 1.0;

        /**
         * The value of <code>preservesPitch</code>: true keeps the pitch when the rate is not 1.0.
         * <p>
         * <code>preservesPitch</code> 의 값: true 이면 속도가 1.0 이 아니어도 음높이를 유지한다.
         */
        boolean preservesPitch = true;

        /**
         * The value of <code>duration</code> in seconds: NaN before the metadata, infinity if the length is not known.
         * <p>
         * <code>duration</code> 의 값(초). 메타데이터 전에는 NaN, 길이를 모르면 무한대.
         */
        double duration = Double.NaN;

        /**
         * The facts about the loaded media, or null.
         * <p>
         * 로드된 미디어의 정보. 없으면 null.
         */
        MediaInfo info;

        /**
         * The player of the loaded resource, or null.
         * <p>
         * 로드한 리소스의 재생기. 없으면 null.
         */
        MediaPlayer player;

        /**
         * The receiver of the reports of the player (strongly held, the player holds it weakly).
         * <p>
         * 재생기의 보고를 받는 쪽(강하게 잡고, 재생기는 약하게 잡는다).
         */
        PlayerLink link;

        /**
         * The tap of the audio graph that the element is connected to (<code>createMediaElementSource</code>), or null; once set it stays for good. The playback thread reads it when
         * it opens an output.
         * <p>
         * 요소가 연결된 오디오 그래프의 탭(<code>createMediaElementSource</code>). 없으면 null. 한 번 정해지면 영구히 남는다. 재생 스레드가 출력을 열 때 읽는다.
         */
        volatile MediaElementTap tap;

        /**
         * The running fetch, or null.
         * <p>
         * 진행 중인 가져오기. 없으면 null.
         */
        MediaRequestSource.Request fetch;

        /**
         * The candidate <code>source</code> element whose resource is tried, or null in the attribute mode.
         * <p>
         * 리소스를 시도 중인 후보 <code>source</code> 요소. 속성 모드이면 null.
         */
        Element candidate;

        /**
         * The last <code>source</code> element that was looked at (the node before the pointer of the algorithm).
         * <p>
         * 마지막으로 살펴본 <code>source</code> 요소(알고리즘의 포인터 앞의 노드).
         */
        Element pointer;

        /**
         * The candidates that were tried already in this selection.
         * <p>
         * 이번 선택에서 이미 시도한 후보.
         */
        final List<Element> tried = new ArrayList<Element>();

        /**
         * True while the algorithm waits for a <code>source</code> element to be inserted.
         * <p>
         * 알고리즘이 <code>source</code> 요소가 삽입되기를 기다리는 동안 true.
         */
        boolean waitingForSource;

        /**
         * The address whose fetch is deferred (<code>preload=none</code>), or null.
         * <p>
         * 가져오기를 미룬(<code>preload=none</code>) 주소. 없으면 null.
         */
        String deferredUrl;

        /**
         * The candidate that belongs to {@link #deferredUrl}.
         * <p>
         * {@link #deferredUrl} 에 속한 후보.
         */
        Element deferredCandidate;

        /**
         * True once the error event of the unsupported video was queued for the current selection.
         * <p>
         * 현재 선택에서 지원하지 않는 비디오의 error 이벤트를 올렸으면 true.
         */
        boolean videoErrorQueued;

        /**
         * True while a time update task of the player waits for the owner of the DOM (at most one at a time).
         * <p>
         * 재생기의 시간 갱신 작업이 DOM 소유자를 기다리는 동안 true(한 번에 하나만).
         */
        final AtomicBoolean timePending = new AtomicBoolean();

        /**
         * True while a progress task of the fetch waits for the owner of the DOM (at most one at a time, so a fast transfer does not flood
         * the queue).
         * <p>
         * 가져오기의 진행 작업이 DOM 소유자를 기다리는 동안 true(한 번에 하나만이므로 빠른 전송이 줄을 넘치게 하지 않는다).
         */
        final AtomicBoolean progressPending = new AtomicBoolean();

        /**
         * The timer that fires <code>stalled</code> when the transfer gives no data for three seconds, or null.
         * <p>
         * 전송이 3초 동안 데이터를 주지 않으면 <code>stalled</code> 를 발생시키는 타이머. 없으면 null.
         */
        Scheduler.Handle stall;

        /**
         * True while the element stays at <code>HAVE_METADATA</code> because of <code>preload="metadata"</code> and wants no more data until
         * it is played.
         * <p>
         * <code>preload="metadata"</code> 때문에 요소가 <code>HAVE_METADATA</code> 에 머물며 재생될 때까지 더 이상의 데이터를 원하지 않는 동안 true.
         */
        boolean metadataOnly;

        /**
         * The promises of the <code>play()</code> calls that wait for playback to start.
         * <p>
         * 재생이 시작되기를 기다리는 <code>play()</code> 호출의 프라미스.
         */
        final List<Deferred> playPromises = new ArrayList<Deferred>();

        /**
         * The batches of <code>play()</code> promises that were taken from the pending list into a queued task that has not run yet, in the order
         * of the tasks; a new load resolves them at once (HTML: the load algorithm settles the promises of the tasks it removes).
         * <p>
         * 대기 목록에서 가져와 아직 실행되지 않은 작업에 실은 <code>play()</code> 프라미스 묶음(작업 순서대로). 새 로드는 이것을 곧바로 이행한다(HTML: 로드
         * 알고리즘은 없애는 작업의 프라미스를 확정한다).
         */
        final List<List<Deferred>> promisesInFlight = new ArrayList<List<Deferred>>();

        /**
         * What the element keeps about its text tracks, made at the first use, or null.
         * <p>
         * 요소가 텍스트 트랙에 대해 보관하는 것. 처음 쓸 때 만든다. 없으면 null.
         */
        MediaTracks tracks;

        /**
         * Creates the state of an element.
         * <p>
         * 요소의 상태를 만든다.
         *
         * @param e the element / 요소
         */
        Media(Element e) {
            this.element = new WeakReference<Element>(e);
            this.video = "video".equals(e.getLocalName());
            this.muted = e.hasAttribute("muted");
        }

        /**
         * Returns the element, or null if it was collected.
         * <p>
         * 요소를 반환한다. 수거되었으면 null.
         *
         * @return the element or null / 요소 또는 null
         */
        Element element() {
            return element.get();
        }
    }

    /**
     * Receives the reports of a player and queues them for the owner of the DOM.
     * <p>
     * 재생기의 보고를 받아 DOM 소유자를 위해 줄 세운다.
     */
    private final class PlayerLink implements PlaybackListener {
        /**
         * The element state.
         * <p>
         * 요소 상태.
         */
        private final Media media;

        /**
         * The load generation of the player.
         * <p>
         * 재생기의 로드 세대.
         */
        private final int epoch;

        /**
         * Creates the link.
         * <p>
         * 연결을 만든다.
         *
         * @param media the element state / 요소 상태
         * @param epoch the load generation / 로드 세대
         */
        PlayerLink(Media media, int epoch) {
            this.media = media;
            this.epoch = epoch;
        }

        /**
         * Queues the metadata.
         * <p>
         * 메타데이터를 줄 세운다.
         *
         * @param info the facts / 정보
         */
        public void loaded(MediaInfo info) {
            Task t = new Task(media, epoch, K_LOADED);
            t.arg = info;
            post(t);
        }

        /**
         * Queues a failed load.
         * <p>
         * 로드 실패를 줄 세운다.
         *
         * @param error the reason / 이유
         */
        public void loadFailed(MediaException error) {
            Task t = new Task(media, epoch, K_LOAD_FAILED);
            t.arg = error;
            post(t);
        }

        /**
         * Queues the start of the sound.
         * <p>
         * 소리의 시작을 줄 세운다.
         */
        public void started() {
            post(new Task(media, epoch, K_STARTED));
        }

        /**
         * Queues a finished seek.
         * <p>
         * 끝난 탐색을 줄 세운다.
         */
        public void seeked() {
            post(new Task(media, epoch, K_SEEKED));
        }

        /**
         * Queues a loop wrap-around.
         * <p>
         * 반복으로 되돌아감을 줄 세운다.
         */
        public void looped() {
            post(new Task(media, epoch, K_LOOPED));
        }

        /**
         * Queues the end.
         * <p>
         * 끝을 줄 세운다.
         */
        public void ended() {
            post(new Task(media, epoch, K_ENDED));
        }

        /**
         * Queues a duration that became known.
         * <p>
         * 알게 된 길이를 줄 세운다.
         *
         * @param seconds the duration / 길이
         */
        public void durationKnown(double seconds) {
            Task t = new Task(media, epoch, K_DURATION);
            t.number = seconds;
            post(t);
        }

        /**
         * Queues a time update; at most one is waiting at a time.
         * <p>
         * 시간 갱신을 줄 세운다. 한 번에 하나만 기다린다.
         */
        public void timeUpdate() {
            if (media.timePending.compareAndSet(false, true)) {
                post(new Task(media, epoch, K_TIME));
            }
        }

        /**
         * Queues a playback failure.
         * <p>
         * 재생 실패를 줄 세운다.
         *
         * @param error the reason / 이유
         */
        public void failed(MediaException error) {
            Task t = new Task(media, epoch, K_FAILED);
            t.arg = error;
            post(t);
        }
    }

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /** Host for a document without a script realm. / 스크립트 렐름이 없는 문서의 호스트. */
    private final HostEnvironment unscripted;

    /**
     * The result queue of the page.
     * <p>
     * 페이지의 결과 줄.
     */
    private final ScriptLoads loads;

    /**
     * The states by element; elements that are no longer referenced disappear (the state holds its element weakly).
     * <p>
     * 요소별 상태. 더 이상 참조되지 않는 요소는 사라진다(상태는 요소를 약하게 잡는다).
     */
    private final Map<Element, Media> states = new WeakHashMap<Element, Media>();

    /**
     * The elements that play now; they stay reachable until they are paused or ended.
     * <p>
     * 지금 재생 중인 요소. 일시정지되거나 끝날 때까지 도달 가능하게 유지된다.
     */
    private final Set<Element> playing = new HashSet<Element>();

    /**
     * Every player that is not closed yet, for silencing a page at once (callable from any thread).
     * <p>
     * 아직 닫히지 않은 모든 재생기. 페이지의 소리를 즉시 끄기 위한 것이다(어느 스레드에서나 접근 가능).
     */
    private final List<MediaPlayer> live = new CopyOnWriteArrayList<MediaPlayer>();

    /**
     * The number of elements that delay the load event.
     * <p>
     * load 이벤트를 늦추는 요소의 수.
     */
    private volatile int delaying;

    /**
     * True after {@link #dispose()}.
     * <p>
     * {@link #dispose()} 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * The text tracks of the media elements.
     * <p>
     * 미디어 요소의 텍스트 트랙.
     */
    private final TextTracks textTracks;

    /**
     * Creates the media elements of a page.
     * <p>
     * 페이지의 미디어 요소를 만든다.
     *
     * @param page  the page / 페이지
     * @param loads the result queue of the page / 페이지의 결과 줄
     */
    MediaElements(PageScripts page, ScriptLoads loads) {
        this(page, null, loads);
    }

    /**
     * Creates media for a document without author scripts. / 작성자 스크립트가 없는 문서의 미디어를 만든다.
     * @param host the media services / 미디어 서비스
     * @param loads the document's result queue / 문서의 결과 큐
     */
    MediaElements(HostEnvironment host, ScriptLoads loads) {
        this(null, host, loads);
    }

    /**
     * Stores the one host of the media elements. / 미디어 요소의 호스트 하나를 보관한다.
     * @param page the script host or null / 스크립트 호스트 또는 null
     * @param unscripted the host without scripts or null / 스크립트 없는 호스트 또는 null
     * @param loads the document's result queue / 문서 결과 큐
     */
    private MediaElements(PageScripts page, HostEnvironment unscripted, ScriptLoads loads) {
        this.page = page;
        this.unscripted = unscripted;
        this.loads = loads;
        // 스크립트 없는 호스트는 파싱한 오디오만 설치하므로 TextTracks에는 스크립트나 트랙이 등록되지 않는다.
        this.textTracks = new TextTracks(page, this);
    }

    /** Returns the hosting services. / 호스트 서비스를 반환한다. */
    private HostEnvironment host() {
        return page == null ? unscripted : page.getEnvironment();
    }

    /**
     * Runs a media task with the document's lifetime guard. / 문서 수명에 맞춰 미디어 작업을 실행한다.
     * @param task the work on the document thread / 문서 스레드의 작업
     */
    private void runMediaTask(Runnable task) {
        if (page != null) {
            page.runTask(task);
        } else {
            // 스크립트 없는 문서에서는 같은 잠금으로 작업과 다른 스레드의 페이지 폐기를 조정한다.
            synchronized (this) {
                if (!disposed) {
                    task.run();
                }
            }
        }
    }

    /**
     * Returns the text tracks of the media elements.
     * <p>
     * 미디어 요소의 텍스트 트랙을 반환한다.
     *
     * @return the text tracks / 텍스트 트랙
     */
    TextTracks textTracks() {
        return textTracks;
    }

    /**
     * Returns what an element keeps about its text tracks (it is made at the first use).
     * <p>
     * 요소가 텍스트 트랙에 대해 보관하는 것을 반환한다(처음 쓸 때 만든다).
     *
     * @param m the element state / 요소 상태
     * @return the text track state / 텍스트 트랙 상태
     */
    MediaTracks tracksOf(Media m) {
        if (m.tracks == null) {
            m.tracks = new MediaTracks();
        }
        return m.tracks;
    }

    /**
     * Queues code as a task of a media element (callable from any thread). A task that is not sticky is dropped when the element starts a new load,
     * as the specification says of the tasks that are queued for a media element.
     * <p>
     * 코드를 미디어 요소의 작업으로 올린다(어느 스레드에서나 부를 수 있다). 고정이 아닌 작업은 요소가 새 로드를 시작하면 버려진다(규격이 미디어 요소를 위해
     * 올린 작업에 대해 말하는 대로).
     *
     * @param m      the element state / 요소 상태
     * @param code   the code to run on the thread that owns the page / 페이지를 소유한 스레드에서 실행할 코드
     * @param sticky true if a new load does not drop the task / 새 로드가 작업을 버리지 않으면 true
     */
    void queueTask(Media m, Runnable code, boolean sticky) {
        Task t = new Task(m, m.epoch, K_RUN);
        t.arg = code;
        t.sticky = sticky;
        post(t);
    }

    /**
     * Pauses an element at once with the pause algorithm (used when playback leaves a cue that has the pause-on-exit flag).
     * <p>
     * 일시정지 알고리즘으로 요소를 곧바로 일시정지한다(재생이 pause-on-exit 표시가 있는 cue 를 벗어날 때 쓴다).
     *
     * @param m the element state / 요소 상태
     */
    void pauseNow(Media m) {
        pauseAlgorithm(m);
    }

    /**
     * Tells whether an element is an <code>audio</code> or <code>video</code> element of the HTML namespace.
     * <p>
     * 요소가 HTML 네임스페이스의 <code>audio</code> 또는 <code>video</code> 요소인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a media element / 미디어 요소이면 true
     */
    static boolean isMedia(Element e) {
        return e != null && Namespaces.HTML.equals(e.getNamespaceURI()) && ("audio".equals(e.getLocalName()) || "video".equals(e.getLocalName()));
    }

    /**
     * Tells whether an element is a <code>source</code> element of the HTML namespace.
     * <p>
     * 요소가 HTML 네임스페이스의 <code>source</code> 요소인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a source element / source 요소이면 true
     */
    private static boolean isSource(Node e) {
        return e instanceof Element && Namespaces.HTML.equals(((Element) e).getNamespaceURI()) && "source".equals(((Element) e).getLocalName());
    }

    /**
     * Returns the state of a media element (it is created at the first use).
     * <p>
     * 미디어 요소의 상태를 반환한다(처음 쓸 때 만든다).
     *
     * @param e the element / 요소
     * @return the state / 상태
     */
    Media state(Element e) {
        Media m = states.get(e);
        if (m == null) {
            m = new Media(e);
            states.put(e, m);
        }
        return m;
    }

    /**
     * Tells whether a media element delays the window load event.
     * <p>
     * 미디어 요소가 window load 이벤트를 늦추고 있는지 알려 준다.
     *
     * @return true if one does / 늦추는 것이 있으면 true
     */
    boolean isDelayingLoad() {
        return delaying > 0;
    }

    /**
     * Sets the delaying-the-load-event flag of an element.
     * <p>
     * 요소의 load 이벤트 지연 플래그를 정한다.
     *
     * @param m  the element state / 요소 상태
     * @param on the new value / 새 값
     */
    private void setDelaying(Media m, boolean on) {
        if (m.delaying != on) {
            m.delaying = on;
            delaying += on ? 1 : -1;
        }
    }

    /**
     * Queues a task for the owner of the DOM (callable from any thread).
     * <p>
     * DOM 소유자를 위한 작업을 줄 세운다(어느 스레드에서나 부를 수 있다).
     *
     * @param t the task / 작업
     */
    private void post(Task t) {
        if (disposed) {
            return;
        }
        ScriptLoads.Load load = loads.beginExternal(t);
        loads.finishExternal(load, null);
    }

    /**
     * Queues an event at the media element.
     * <p>
     * 미디어 요소에 올리는 이벤트를 줄 세운다.
     *
     * @param m    the element state / 요소 상태
     * @param name the event name / 이벤트 이름
     */
    private void queue(Media m, String name) {
        Task t = new Task(m, m.epoch, K_EVENT);
        t.name = name;
        post(t);
    }

    /**
     * Notifies about playing the media element (HTML "notify about playing the media element"): takes the pending <code>play()</code> promises
     * and queues one task that fires <code>playing</code> and then resolves exactly those promises. The promises therefore follow the event and
     * not the sound device, whose first output may come seconds later on a slow line.
     * <p>
     * 미디어 요소의 재생을 알린다(HTML "notify about playing the media element"): 대기 중인 <code>play()</code> 프라미스를 가져오고
     * <code>playing</code> 을 낸 뒤 바로 그 프라미스들을 이행하는 작업 하나를 줄 세운다. 그래서 프라미스는 소리 장치가 아니라 이벤트를 따른다(느린 라인에서는
     * 장치의 첫 출력이 몇 초 늦을 수 있다).
     *
     * @param m the element state / 요소 상태
     */
    private void notifyPlaying(Media m) {
        Task t = new Task(m, m.epoch, K_EVENT);
        t.name = "playing";
        t.promises = takePlayPromises(m);
        post(t);
    }

    /**
     * Takes the pending <code>play()</code> promises into a batch that a queued task settles later (HTML "take pending play promises"); the
     * batch is remembered so that a new load can settle it at once.
     * <p>
     * 대기 중인 <code>play()</code> 프라미스를 나중에 줄 세운 작업이 확정할 묶음으로 가져온다(HTML "take pending play promises"). 새 로드가 곧바로
     * 확정할 수 있도록 묶음을 기억한다.
     *
     * @param m the element state / 요소 상태
     * @return the batch, or null when nothing was pending / 묶음. 대기 중인 것이 없으면 null
     */
    private List<Deferred> takePlayPromises(Media m) {
        if (m.playPromises.isEmpty()) {
            return null;
        }
        List<Deferred> batch = new ArrayList<Deferred>(m.playPromises);
        m.playPromises.clear();
        m.promisesInFlight.add(batch);
        return batch;
    }

    /**
     * Resolves a batch of taken promises, unless a new load already settled it.
     * <p>
     * 가져온 프라미스 묶음을 이행한다. 새 로드가 이미 확정했으면 하지 않는다.
     *
     * @param m     the element state / 요소 상태
     * @param batch the batch or null / 묶음 또는 null
     */
    private void resolveTaken(Media m, List<Deferred> batch) {
        if (batch == null || !m.promisesInFlight.remove(batch)) {
            return;
        }
        // 묶음의 프라미스를 하나씩 이행한다.
        for (Deferred d : batch) {
            d.resolve();
        }
    }

    /**
     * Queues an event at another element (the <code>source</code> candidate).
     * <p>
     * 다른 요소(<code>source</code> 후보)에 올리는 이벤트를 줄 세운다.
     *
     * @param m      the element state / 요소 상태
     * @param target the target / 대상
     * @param name   the event name / 이벤트 이름
     */
    private void queueAt(Media m, Element target, String name) {
        Task t = new Task(m, m.epoch, K_EVENT);
        t.name = name;
        t.target = target;
        post(t);
    }

    /**
     * Delivers a task to the owner of the DOM. A task of an older load generation, and everything after the disposal, is dropped.
     * <p>
     * DOM 소유자에게 작업을 전달한다. 이전 로드 세대의 작업과 폐기 뒤의 모든 작업은 버린다.
     *
     * @param t the task / 작업
     */
    void deliver(final Task t) {
        if (disposed || (!t.sticky && t.epoch != t.media.epoch)) {
            return;
        }
        runMediaTask(new Runnable() {
            /**
             * Handles the task under the page lock, with the microtask checkpoint of a task.
             * <p>
             * 페이지 잠금 아래에서 작업을 처리한다. 작업의 마이크로태스크 검사점이 따른다.
             */
            public void run() {
                handle(t);
            }
        });
    }

    /**
     * Handles one task.
     * <p>
     * 작업 하나를 처리한다.
     *
     * @param t the task / 작업
     */
    private void handle(Task t) {
        Media m = t.media;
        if (disposed || (!t.sticky && t.epoch != m.epoch)) {
            return;
        }
        switch (t.kind) {
        case K_EVENT:
            fireEvent(m, t.target != null ? t.target : m.element(), t.name);
            // "notify about playing": playing 이벤트를 낸 같은 작업에서 그때 가져온 play() 프라미스를 이행한다.
            resolveTaken(m, t.promises);
            break;
        case K_RESOLVE:
            resolveTaken(m, t.promises);
            break;
        case K_SELECT:
            select(m);
            break;
        case K_ADVANCE:
            advance(m);
            break;
        case K_FETCHED:
            fetched(m, t);
            break;
        case K_LOADED:
            loaded(m, (MediaInfo) t.arg);
            break;
        case K_LOAD_FAILED:
            resourceFailed(m, ((MediaException) t.arg).getMessage());
            break;
        case K_SEEKED:
            m.seeking = false;
            textTracks.timeMarchesOn(m);
            queue(m, "timeupdate");
            queue(m, "seeked");
            break;
        case K_LOOPED:
            queue(m, "seeking");
            textTracks.positionJumped(m);
            textTracks.timeMarchesOn(m);
            queue(m, "timeupdate");
            queue(m, "seeked");
            break;
        case K_ENDED:
            ended(m);
            break;
        case K_DURATION:
            m.duration = t.number;
            queue(m, "durationchange");
            break;
        case K_TIME:
            m.timePending.set(false);
            if (!m.paused) {
                fireEvent(m, m.element(), "timeupdate");
                textTracks.timeMarchesOn(m);
            }
            break;
        case K_FAILED:
            playbackFailed(m, (MediaException) t.arg);
            break;
        case K_STARTED:
            // 소리가 장치에 닿기 시작했다는 기록일 뿐이다. play() 프라미스는 장치 지연과 무관하게 playing 이벤트와 함께 이행한다(HTML "notify about playing").
            if (!m.paused) {
                m.started = true;
            }
            break;
        case K_RELEASE:
            setDelaying(m, false);
            break;
        case K_PROGRESS:
            m.progressPending.set(false);
            // 아직 가져오는 중일 때만 진행 이벤트를 내고, 다음 데이터를 기다리는 stalled 타이머를 다시 시작한다.
            if (m.fetch != null && m.networkState == NETWORK_LOADING) {
                fireEvent(m, m.element(), "progress");
                armStall(m);
            }
            break;
        case K_RUN:
            ((Runnable) t.arg).run();
            break;
        case K_PAUSE_CHECK:
            if (m.element() != null && !m.element().isConnected() && !m.paused) {
                pauseAlgorithm(m);
            }
            break;
        default:
            break;
        }
    }

    /**
     * Fires a simple event at an element on the thread that owns the DOM.
     * <p>
     * DOM 을 소유한 스레드에서 요소에 단순 이벤트를 발생시킨다.
     *
     * @param m      the element state / 요소 상태
     * @param target the target / 대상
     * @param name   the event name / 이벤트 이름
     */
    private void fireEvent(Media m, Element target, String name) {
        if (target == null) {
            return;
        }
        if (page == null) {
            target.dispatchTrustedEvent(new Event(name, false, false));
        } else {
            page.fireMediaEvent(target, name);
        }
        // 컨트롤 막대가 있는 요소는 상태가 바뀔 때마다(이벤트는 상태 변화를 알린다) 막대를 다시 그리게 한다.
        if (target == m.element() && target.hasAttribute("controls")) {
            host().mediaControlsChanged(target);
        }
    }

    /**
     * Returns what the control bar of an element shows now (the renderer asks on the document thread, so the state is read without a lock).
     * <p>
     * 요소의 컨트롤 막대가 지금 보여 주는 것을 반환한다(렌더러가 문서 스레드에서 묻는 것이므로 상태는 잠금 없이 읽는다).
     *
     * @param e the <code>audio</code> element / <code>audio</code> 요소
     * @return the snapshot / 스냅샷
     */
    public MediaControlState getMediaControls(Element e) {
        if (disposed || !isMedia(e)) {
            return null;
        }
        Media m = states.get(e);
        // 아직 아무것도 로드하지 않은 요소는 처음 상태를 보여 준다.
        if (m == null) {
            return MediaControlState.IDLE;
        }
        return new MediaControlState(m.readyState >= HAVE_METADATA && m.error == null, m.paused, isEnded(m), position(m), m.duration, m.volume, m.muted);
    }

    /**
     * Applies an action of the user on the control bar (see {@link PageScripts#controlMedia(Element, int, double)}): exactly what a script
     * would do with <code>play()</code>, <code>pause()</code>, <code>currentTime</code>, <code>muted</code> and <code>volume</code>. The
     * control starts playback directly without creating a script promise.
     * <p>
     * 사용자가 컨트롤 막대에서 한 동작을 적용한다({@link PageScripts#controlMedia(Element, int, double)} 참고): 스크립트가 <code>play()</code>,
     * <code>pause()</code>, <code>currentTime</code>, <code>muted</code>, <code>volume</code> 으로 하는 것과 똑같다. 컨트롤은 스크립트 프라미스를
     * 만들지 않고 직접 재생을 시작한다.
     *
     * @param e        the <code>audio</code> element / <code>audio</code> 요소
     * @param part     the part of the bar (<code>MediaControls.PART_</code>) / 막대의 부분(<code>MediaControls.PART_</code>)
     * @param fraction the position on the seek bar or the volume, 0 to 1 / 탐색 막대의 위치 또는 음량(0에서 1)
     */
    void control(Element e, int part, double fraction) {
        if (disposed || !isMedia(e) || "video".equals(e.getLocalName())) {
            return;
        }
        Media m = state(e);
        double f = Double.isNaN(fraction) ? 0 : Math.max(0, Math.min(1, fraction));
        // 눌린 부분에 따라 스크립트가 하는 것과 같은 동작을 한다.
        switch (part) {
        case MediaControls.PART_PLAY:
            if (m.paused) {
                requestPlayback(m, e, null);
            } else {
                pause(m);
            }
            break;
        case MediaControls.PART_SEEK:
            // 길이를 아는 로드된 요소만 탐색한다.
            if (m.readyState >= HAVE_METADATA && !Double.isNaN(m.duration) && !Double.isInfinite(m.duration)) {
                setPosition(m, f * m.duration);
            }
            break;
        case MediaControls.PART_MUTE:
            setMuted(m, !m.muted);
            break;
        case MediaControls.PART_VOLUME:
            // 음량을 올리면 음소거도 풀린다.
            if (m.muted && f > 0) {
                setMuted(m, false);
            }
            setVolume(m, f);
            break;
        default:
            break;
        }
    }

    /**
     * Settles the promises of the pending <code>play()</code> calls: fulfils them, or rejects them with a <code>DOMException</code>.
     * <p>
     * 기다리는 <code>play()</code> 호출의 프라미스를 확정한다: 이행하거나 <code>DOMException</code> 으로 거부한다.
     *
     * @param m       the element state / 요소 상태
     * @param name    the name of the exception, or null to fulfil / 예외 이름. 이행하려면 null
     * @param message the message / 메시지
     */
    private void settlePlayPromises(Media m, String name, String message) {
        if (m.playPromises.isEmpty()) {
            return;
        }
        List<Deferred> list = new ArrayList<Deferred>(m.playPromises);
        m.playPromises.clear();
        // 기다리던 프라미스를 하나씩 이행하거나 거부한다.
        for (Deferred d : list) {
            if (name == null) {
                d.resolve();
            } else {
                d.reject(DomExceptionObjects.create(page.getBridge(), name, message));
            }
        }
    }

    /**
     * The media element load algorithm (HTML5 4.7.10.5): aborts what runs, resets the element and starts the resource selection.
     * <p>
     * 미디어 요소 로드 알고리즘(HTML5 4.7.10.5): 실행 중인 것을 중단하고, 요소를 초기화하고, 리소스 선택을 시작한다.
     *
     * @param m the element state / 요소 상태
     */
    void load(Media m) {
        double before = position(m);
        // 이전 로드의 작업을 없애려고 세대를 올린다. 이후에 올리는 이벤트가 새 세대의 것이다.
        m.epoch++;
        textTracks.mediaReset(m);
        m.everLoaded = true;
        m.metadataOnly = false;
        m.progressPending.set(false);
        m.timePending.set(false);
        cancelFetch(m);
        releasePlayer(m);
        // 없어지는 작업이 이행하려던 프라미스는 작업 순서대로 곧바로 이행한다(HTML 로드 알고리즘).
        List<List<Deferred>> taken = new ArrayList<List<Deferred>>(m.promisesInFlight);
        m.promisesInFlight.clear();
        for (List<Deferred> batch : taken) { // 묶음을 줄 세운 순서대로 처리한다
            for (Deferred d : batch) { // 묶음 안의 프라미스를 하나씩 이행한다
                d.resolve();
            }
        }
        settlePlayPromises(m, "AbortError", "The play() request was interrupted by a new load request.");
        if (m.networkState == NETWORK_LOADING || m.networkState == NETWORK_IDLE) {
            queue(m, "abort");
        }
        if (m.networkState != NETWORK_EMPTY) {
            queue(m, "emptied");
            m.networkState = NETWORK_EMPTY;
        }
        m.readyState = HAVE_NOTHING;
        m.paused = true;
        m.started = false;
        removePlaying(m);
        m.seeking = false;
        if (before != 0) {
            queue(m, "timeupdate");
        }
        // 새 로드는 playbackRate 를 defaultPlaybackRate 로 되돌린다(바뀌었으면 ratechange).
        if (m.playbackRate != m.defaultRate) {
            m.playbackRate = m.defaultRate;
            queue(m, "ratechange");
        }
        m.duration = Double.NaN;
        m.info = null;
        m.error = null;
        m.autoplaying = true;
        m.currentSrc = "";
        m.candidate = null;
        m.pointer = null;
        m.tried.clear();
        m.waitingForSource = false;
        m.deferredUrl = null;
        m.deferredCandidate = null;
        m.videoErrorQueued = false;
        startSelection(m);
    }

    /**
     * Invokes the resource selection algorithm: the network state becomes <code>NETWORK_NO_SOURCE</code>, the element delays the load event,
     * and the synchronous section runs as a task (after the script that changed the element finished).
     * <p>
     * 리소스 선택 알고리즘을 호출한다: 네트워크 상태가 <code>NETWORK_NO_SOURCE</code> 가 되고, 요소가 load 이벤트를 늦추며, 동기 구간은 작업으로
     * 실행된다(요소를 바꾼 스크립트가 끝난 뒤).
     *
     * @param m the element state / 요소 상태
     */
    private void startSelection(Media m) {
        m.everLoaded = true;
        m.networkState = NETWORK_NO_SOURCE;
        setDelaying(m, true);
        post(new Task(m, m.epoch, K_SELECT));
    }

    /**
     * The synchronous section of the resource selection algorithm: decides between the <code>src</code> attribute and the <code>source</code>
     * children, and starts with the first resource.
     * <p>
     * 리소스 선택 알고리즘의 동기 구간: <code>src</code> 속성과 <code>source</code> 자식 중 어느 쪽을 쓸지 정하고 첫 리소스로 시작한다.
     *
     * @param m the element state / 요소 상태
     */
    private void select(Media m) {
        Element e = m.element();
        if (e == null) {
            return;
        }
        String src = e.getAttribute("src");
        Element first = null;
        // src 속성이 없으면 첫 source 자식을 후보로 삼는다.
        if (src == null) {
            first = nextSource(m, null);
            if (first == null) {
                m.networkState = NETWORK_EMPTY;
                setDelaying(m, false);
                if (m.video) {
                    videoUnsupported(m, "video playback is not supported");
                }
                return;
            }
        }
        m.networkState = NETWORK_LOADING;
        queue(m, "loadstart");
        if (src != null) {
            String raw = src.trim();
            String url = raw.length() == 0 ? null : absolute(e, raw);
            if (url == null) {
                failedWithAttribute(m, raw.length() == 0 ? "the src attribute is empty" : "the src attribute cannot be resolved");
                return;
            }
            m.currentSrc = url;
            if (m.video) {
                failedWithAttribute(m, "video playback is not supported");
                return;
            }
            startFetch(m, url, null);
        } else {
            processCandidate(m, first);
        }
    }

    /**
     * Resolves the URL of a media resource against the base URL of the document.
     * <p>
     * 미디어 리소스의 URL 을 문서의 기준 URL 로 해석한다.
     *
     * @param e   the element that has the URL / URL 이 있는 요소
     * @param raw the URL as written / 적힌 URL
     * @return the absolute URL, or null if it cannot be resolved / 절대 URL. 해석할 수 없으면 null
     */
    static String absolute(Element e, String raw) {
        String base = e.getOwnerDocument() == null ? null : e.getOwnerDocument().getBaseUri();
        if (base == null || base.length() == 0 || "about:blank".equals(base) || "about:text".equals(base)) {
            return raw;
        }
        try {
            return UrlUtil.resolve(base, raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Finds the next <code>source</code> child after the last one that was looked at, skipping the candidates that were tried.
     * <p>
     * 마지막으로 살펴본 것 다음의 <code>source</code> 자식을 찾는다. 이미 시도한 후보는 건너뛴다.
     *
     * @param m    the element state / 요소 상태
     * @param from the node after which to look, null for the start of the children / 이 노드 다음부터 찾는다. 자식의 처음부터이면 null
     * @return the next candidate or null / 다음 후보. 없으면 null
     */
    private Element nextSource(Media m, Element from) {
        Element e = m.element();
        if (e == null) {
            return null;
        }
        Node start = from != null && from.getParentNode() == e ? from.getNextSibling() : e.getFirstChild();
        // 자식을 순서대로 훑으며 아직 시도하지 않은 source 요소를 찾는다.
        for (Node n = start; n != null; n = n.getNextSibling()) {
            if (isSource(n) && !m.tried.contains(n)) {
                m.pointer = (Element) n;
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Examines a candidate <code>source</code> element: its <code>src</code>, its <code>type</code> and its <code>media</code> query; a
     * usable candidate is fetched, any other fails and the next one is tried.
     * <p>
     * 후보 <code>source</code> 요소를 검사한다: <code>src</code>, <code>type</code>, <code>media</code> 질의. 쓸 수 있는 후보는 가져오고 그 밖의
     * 후보는 실패하여 다음 후보를 시도한다.
     *
     * @param m the element state / 요소 상태
     * @param c the candidate / 후보
     */
    private void processCandidate(Media m, Element c) {
        m.tried.add(c);
        String src = c.getAttribute("src");
        String raw = src == null ? "" : src.trim();
        String url = raw.length() == 0 ? null : absolute(c, raw);
        String type = c.getAttribute("type");
        String media = c.getAttribute("media");
        if (url == null) {
            failedWithElements(m, c);
        } else if (m.video || (type != null && knowsCannotRender(type))) {
            failedWithElements(m, c);
        } else if (media != null && media.trim().length() > 0 && !MediaQueryList.parse(media).matches(environment())) {
            failedWithElements(m, c);
        } else {
            m.currentSrc = url;
            startFetch(m, url, c);
        }
    }

    /**
     * Returns the media environment of the viewport, for the <code>media</code> attribute of a <code>source</code>.
     * <p>
     * <code>source</code> 의 <code>media</code> 속성을 위해 뷰포트의 미디어 환경을 반환한다.
     *
     * @return the environment / 환경
     */
    private MediaEnvironment environment() {
        HostEnvironment env = host();
        return new MediaEnvironment(env.getViewportWidth(), env.getViewportHeight());
    }

    /**
     * Tells whether a type attribute names a type that this component knows it cannot render: it has a type that is not supported. An empty
     * type, and <code>application/octet-stream</code> without parameters, are never known to be unrenderable.
     * <p>
     * type 속성이 이 컴포넌트가 렌더링할 수 없다고 아는 타입인지 알려 준다: 지원하지 않는 타입이다. 빈 타입과 매개변수 없는
     * <code>application/octet-stream</code> 은 렌더링할 수 없다고 알려진 적이 없다.
     *
     * @param type the value of the attribute / 속성 값
     * @return true if the type is known to be unrenderable / 렌더링할 수 없다고 알면 true
     */
    static boolean knowsCannotRender(String type) {
        String t = type.trim();
        if (t.length() == 0 || "application/octet-stream".equalsIgnoreCase(t)) {
            return false;
        }
        return AudioDecoders.canPlayType(t).length() == 0;
    }

    /**
     * "Failed with attribute": the resource of the <code>src</code> attribute cannot be used. The error becomes
     * <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code>, the network state <code>NETWORK_NO_SOURCE</code>, the <code>error</code> event is queued
     * and the element stops delaying the load event.
     * <p>
     * "속성으로 실패": <code>src</code> 속성의 리소스를 쓸 수 없다. 오류는 <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code>, 네트워크 상태는
     * <code>NETWORK_NO_SOURCE</code> 가 되고 <code>error</code> 이벤트를 올리며 요소는 load 이벤트를 그만 늦춘다.
     *
     * @param m   the element state / 요소 상태
     * @param why the reason, for the message of the error / 이유(오류의 메시지용)
     */
    private void failedWithAttribute(Media m, String why) {
        reportState(m, why);
        releasePlayer(m);
        m.error = new MediaErrorValue(MEDIA_ERR_SRC_NOT_SUPPORTED, why);
        m.networkState = NETWORK_NO_SOURCE;
        m.readyState = HAVE_NOTHING;
        queue(m, "error");
        setDelaying(m, false);
        settlePlayPromises(m, "NotSupportedError", "The element has no supported sources: " + why);
    }

    /**
     * "Failed with elements": the candidate cannot be used. An <code>error</code> event is queued at the candidate and the next candidate is
     * looked for.
     * <p>
     * "요소로 실패": 후보를 쓸 수 없다. 후보에 <code>error</code> 이벤트를 올리고 다음 후보를 찾는다.
     *
     * @param m the element state / 요소 상태
     * @param c the failed candidate / 실패한 후보
     */
    private void failedWithElements(Media m, Element c) {
        releasePlayer(m);
        queueAt(m, c, "error");
        advance(m);
    }

    /**
     * Looks for the next candidate; without one the element waits for a <code>source</code> to be inserted ("waiting": no source, no
     * delay of the load event). A <code>video</code> element reports its failure once.
     * <p>
     * 다음 후보를 찾는다. 후보가 없으면 요소는 <code>source</code> 가 삽입되기를 기다린다("waiting": 소스 없음, load 이벤트 지연 없음).
     * <code>video</code> 요소는 실패를 한 번 알린다.
     *
     * @param m the element state / 요소 상태
     */
    private void advance(Media m) {
        Element next = nextSource(m, m.pointer);
        if (next != null) {
            processCandidate(m, next);
            return;
        }
        m.networkState = NETWORK_NO_SOURCE;
        setDelaying(m, false);
        m.waitingForSource = true;
        reportState(m, "no usable source");
        if (m.video) {
            videoUnsupported(m, "video playback is not supported");
        }
    }

    /**
     * The explicit result of a <code>video</code> element, which this component cannot play: <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> and one
     * <code>error</code> event per selection (plan item P18-09).
     * <p>
     * 이 컴포넌트가 재생할 수 없는 <code>video</code> 요소의 명시적인 결과: <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 와 선택마다 한 번의
     * <code>error</code> 이벤트(계획 P18-09).
     *
     * @param m   the element state / 요소 상태
     * @param why the message / 메시지
     */
    private void videoUnsupported(Media m, String why) {
        if (m.videoErrorQueued) {
            return;
        }
        m.videoErrorQueued = true;
        m.error = new MediaErrorValue(MEDIA_ERR_SRC_NOT_SUPPORTED, why);
        if (m.networkState == NETWORK_EMPTY) {
            m.networkState = NETWORK_NO_SOURCE;
        }
        queue(m, "error");
        settlePlayPromises(m, "NotSupportedError", why);
    }

    /**
     * Starts the fetch of the resource, or defers it for <code>preload="none"</code> (the network is idle and a <code>suspend</code> event is
     * queued) until the element is played.
     * <p>
     * 리소스 가져오기를 시작한다. <code>preload="none"</code> 이면 요소가 재생될 때까지 미룬다(네트워크는 놀고 <code>suspend</code> 이벤트를 올린다).
     *
     * @param m   the element state / 요소 상태
     * @param url the absolute URL / 절대 URL
     * @param c   the candidate, or null in the attribute mode / 후보. 속성 모드이면 null
     */
    private void startFetch(Media m, String url, Element c) {
        m.candidate = c;
        Element e = m.element();
        String preload = e == null ? null : e.getAttribute("preload");
        // preload=none 이고 자동 재생도 재생 요청도 없으면 사용자가 재생할 때까지 가져오기를 미룬다.
        if (e != null && preload != null && "none".equalsIgnoreCase(preload.trim()) && !wantsAutoplay(e) && m.paused) {
            m.deferredUrl = url;
            m.deferredCandidate = c;
            m.networkState = NETWORK_IDLE;
            queue(m, "suspend");
            setDelaying(m, false);
            return;
        }
        beginFetch(m, url);
    }

    /**
     * Fetches the resource through the request service of the page.
     * <p>
     * 페이지의 요청 서비스로 리소스를 가져온다.
     *
     * @param m   the element state / 요소 상태
     * @param url the absolute URL / 절대 URL
     */
    private void beginFetch(final Media m, String url) {
        m.networkState = NETWORK_LOADING;
        final int epoch = m.epoch;
        MediaRequestSource source = host().getMediaRequestSource();
        if (source == null) {
            Task t = new Task(m, epoch, K_FETCHED);
            t.error = "this page cannot load media";
            post(t);
            return;
        }
        m.fetch = source.request(url, new MediaRequestSource.ProgressCallback() {
            /**
             * Queues a progress report for the owner of the DOM; at most one is waiting at a time (may run on any thread).
             * <p>
             * 진행 보고를 DOM 소유자에게 줄 세운다. 한 번에 하나만 기다린다(어느 스레드에서나 실행될 수 있다).
             *
             * @param received the bytes received / 받은 바이트
             * @param total    the expected total or -1 / 예상 전체 크기. 모르면 -1
             */
            public void progress(long received, long total) {
                // 이미 기다리는 진행 작업이 없을 때만 새로 올린다.
                if (received > 0 && m.progressPending.compareAndSet(false, true)) {
                    post(new Task(m, epoch, K_PROGRESS));
                }
            }

            /**
             * Queues the result for the owner of the DOM (may run on any thread).
             * <p>
             * 결과를 DOM 소유자에게 줄 세운다(어느 스레드에서나 실행될 수 있다).
             *
             * @param data     the bytes / 바이트
             * @param mimeType the MIME type / MIME 타입
             * @param error    the failure or null / 실패 원인 또는 null
             */
            public void done(byte[] data, String mimeType, String error) {
                Task t = new Task(m, epoch, K_FETCHED);
                t.data = data;
                t.mime = mimeType;
                t.error = error;
                post(t);
            }
        });
        armStall(m);
    }

    /**
     * Checks both the HTML attribute and the document's automatic-features policy.
     * <p>
     * HTML 속성과 문서의 자동 기능 정책을 모두 확인한다.
     * @param element the media element / 미디어 요소
     * @return true if the element requests and may use autoplay / 요소가 자동 재생을 요청했고 문서가 허용하면 true
     */
    private boolean wantsAutoplay(Element element) {
        return element.hasAttribute("autoplay") && host().allowsMediaAutoplay();
    }

    /**
     * Starts the timer of the <code>stalled</code> event: when the transfer gives no data for {@link #STALL_MILLIS}, the event fires once
     * (the next progress report starts the timer again). Without a scheduler in the host nothing is started.
     * <p>
     * <code>stalled</code> 이벤트의 타이머를 시작한다: 전송이 {@link #STALL_MILLIS} 동안 데이터를 주지 않으면 이벤트가 한 번 발생한다(다음 진행 보고가
     * 타이머를 다시 시작한다). 호스트에 스케줄러가 없으면 아무것도 시작하지 않는다.
     *
     * @param m the element state / 요소 상태
     */
    private void armStall(final Media m) {
        cancelStall(m);
        Scheduler scheduler = host().getScheduler();
        if (scheduler == null || disposed) {
            return;
        }
        final int epoch = m.epoch;
        m.stall = scheduler.post(new Runnable() {
            /**
             * Fires <code>stalled</code> if the same transfer still waits for data.
             * <p>
             * 같은 전송이 아직 데이터를 기다리면 <code>stalled</code> 를 발생시킨다.
             */
            public void run() {
                runMediaTask(new Runnable() {
                    /** Fires the event under the page lock. / 페이지 잠금 아래에서 이벤트를 발생시킨다. */
                    public void run() {
                        if (!disposed && epoch == m.epoch && m.fetch != null && m.networkState == NETWORK_LOADING) {
                            m.stall = null;
                            fireEvent(m, m.element(), "stalled");
                        }
                    }
                });
            }
        }, STALL_MILLIS);
    }

    /**
     * Stops the timer of the <code>stalled</code> event, if one runs.
     * <p>
     * <code>stalled</code> 이벤트의 타이머가 돌고 있으면 멈춘다.
     *
     * @param m the element state / 요소 상태
     */
    private void cancelStall(Media m) {
        Scheduler.Handle h = m.stall;
        m.stall = null;
        if (h != null) {
            h.cancel();
        }
    }

    /**
     * Cancels the fetch that runs, if any.
     * <p>
     * 진행 중인 가져오기가 있으면 취소한다.
     *
     * @param m the element state / 요소 상태
     */
    private void cancelFetch(Media m) {
        cancelStall(m);
        MediaRequestSource.Request r = m.fetch;
        m.fetch = null;
        if (r != null) {
            r.cancel();
        }
    }

    /**
     * The fetch ended: a failure fails the resource, otherwise a player opens the data.
     * <p>
     * 가져오기가 끝났다: 실패하면 리소스를 실패 처리하고, 아니면 재생기가 데이터를 연다.
     *
     * @param m the element state / 요소 상태
     * @param t the result / 결과
     */
    private void fetched(Media m, Task t) {
        cancelStall(m);
        m.fetch = null;
        if (t.error != null || t.data == null) {
            resourceFailed(m, t.error == null ? "the resource could not be fetched" : t.error);
            return;
        }
        PlayerLink link = new PlayerLink(m, m.epoch);
        MediaPlayer p = new MediaPlayer(new ElementOutputs(m, host().getAudioOutputFactory()), link);
        m.link = link;
        m.player = p;
        // 그래프에 연결된 요소는 새 리소스가 페이지에 읽혀도 되는 것인지(같은 오리진인지) 다시 정한다.
        MediaElementTap connected = m.tap;
        if (connected != null) {
            connected.setOutputAllowed(mayReadAudio(m));
        }
        pruneLive();
        live.add(p);
        Element e = m.element();
        p.setVolume(m.volume);
        p.setMuted(m.muted);
        p.setPreservesPitch(m.preservesPitch);
        p.setRate(m.playbackRate);
        p.setLoop(e != null && e.hasAttribute("loop"));
        p.load(t.data, t.mime);
        t.data = null;
    }

    /**
     * Opens the outputs of the player of one element: the pipe of the audio graph that the element is connected to (see {@link #connectToGraph}) if it is, the sound output of the
     * component otherwise. The choice is made each time the player opens an output, so a script that connects the element later redirects the sound (the player is asked to reopen
     * its output then).
     * <p>
     * 요소 하나의 재생기의 출력을 연다: 요소가 연결된 오디오 그래프의 파이프({@link #connectToGraph} 참고)가 있으면 그것을, 없으면 컴포넌트의 소리 출력을 연다. 재생기가 출력을
     * 열 때마다 고르므로 스크립트가 나중에 요소를 연결하면 소리가 돌려진다(그때 재생기에 출력을 다시 열도록 요청한다).
     */
    private static final class ElementOutputs implements AudioOutputFactory {

        /**
         * The element state.
         * <p>
         * 요소 상태.
         */
        private final Media media;

        /**
         * The sound output of the component, or null for none.
         * <p>
         * 컴포넌트의 소리 출력. 없으면 null.
         */
        private final AudioOutputFactory direct;

        /**
         * Creates the factory of an element.
         * <p>
         * 요소의 팩터리를 만든다.
         *
         * @param media  the element state / 요소 상태
         * @param direct the sound output of the component, or null / 컴포넌트의 소리 출력. 없으면 null
         */
        ElementOutputs(Media media, AudioOutputFactory direct) {
            this.media = media;
            this.direct = direct;
        }

        /**
         * Opens an output on the playback thread.
         * <p>
         * 재생 스레드에서 출력을 연다.
         *
         * @param sampleRate the sample rate of the media / 미디어의 샘플링 주파수
         * @param channels   the number of channels / 채널 수
         * @return the output / 출력
         * @throws MediaException <code>ACCESS_FAILED</code> without a sound output / 소리 출력이 없으면 <code>ACCESS_FAILED</code>
         */
        public AudioOutput open(int sampleRate, int channels) throws MediaException {
            MediaElementTap tap = media.tap;
            // 오디오 그래프에 연결된 요소는 소리가 스피커가 아니라 그래프로 간다.
            if (tap != null) {
                return tap.open(sampleRate, channels);
            }
            if (direct == null) {
                throw new MediaException(MediaException.Kind.ACCESS_FAILED, "this component has no audio output");
            }
            return direct.open(sampleRate, channels);
        }
    }

    /**
     * Connects an <code>audio</code> element to an audio graph (Web Audio <code>createMediaElementSource</code>, plan item P25-04): from now on the sound of the element is not
     * played to the speakers but offered to the returned {@linkplain MediaElementTap tap}, which the graph pulls from; the element keeps playing, pausing, seeking, looping and
     * reporting its events as before, with the audio graph as its clock (see {@link MediaElementTap}). A player that has an output open for the speakers is asked to reopen it as a
     * pipe of the graph at the position that is heard now. An element can be connected once, to one node of one context, for good. Call it on the thread that owns the page.
     * <p>
     * <code>audio</code> 요소를 오디오 그래프에 연결한다(Web Audio <code>createMediaElementSource</code>, 계획 P25-04): 이제부터 요소의 소리는 스피커로 재생되지 않고 반환하는
     * {@linkplain MediaElementTap 탭}에 제공되며 그래프가 거기서 가져간다. 요소는 전처럼 재생, 일시정지, 탐색, 반복하고 이벤트를 알리며 오디오 그래프가 시계가 된다({@link MediaElementTap}
     * 참고). 스피커용 출력을 열어 둔 재생기는 지금 들리는 위치에서 그것을 그래프의 파이프로 다시 열도록 요청받는다. 요소는 한 번만, 한 컨텍스트의 한 노드에, 영구히 연결할 수 있다.
     * 페이지를 소유한 스레드에서 부른다.
     *
     * @param e                 the element / 요소
     * @param contextSampleRate the sample rate of the context in Hz / 컨텍스트의 샘플링 주파수(Hz)
     * @return the tap / 탭
     * @throws IllegalArgumentException if the element is no <code>audio</code> element of this page (a <code>video</code> element is not supported) / 요소가 이 페이지의
     *                                  <code>audio</code> 요소가 아니면(<code>video</code> 요소는 지원하지 않는다)
     * @throws IllegalStateException    if the element was connected before / 요소를 전에 연결했으면
     */
    MediaElementTap connectToGraph(Element e, float contextSampleRate) {
        if (!isMedia(e) || "video".equals(e.getLocalName())) {
            throw new IllegalArgumentException("not an audio element");
        }
        if (e.getOwnerDocument() != page.getDocument()) {
            throw new IllegalArgumentException("the element belongs to another document");
        }
        Media m = state(e);
        if (m.tap != null) {
            throw new IllegalStateException("the element is connected to an audio node already");
        }
        MediaElementTap tap = new MediaElementTap(contextSampleRate);
        m.tap = tap;
        tap.setOutputAllowed(mayReadAudio(m));
        MediaPlayer p = m.player;
        // 스피커용 출력을 이미 열어 둔 재생기는 그래프의 파이프로 다시 열도록 요청한다.
        if (p != null) {
            p.reopenOutput();
        }
        return tap;
    }

    /**
     * Tells whether the page may read the audio of the resource that the element plays: the resource comes from the origin of the document of the element (the fetch of media
     * does not use the CORS mode yet, so the response of another origin never carries the permission of a server; the web audio specification makes such media silent in the
     * graph). An element without a resource is allowed (nothing is heard).
     * <p>
     * 페이지가 요소가 재생하는 리소스의 오디오를 읽어도 되는지 알려 준다: 리소스가 요소의 문서와 같은 오리진에서 온 것이어야 한다(미디어 가져오기는 아직 CORS 모드를 쓰지 않으므로 다른
     * 오리진의 응답에는 서버의 허가가 실려 있지 않다. Web Audio 규격은 그런 미디어를 그래프에서 무음으로 만든다). 리소스가 없는 요소는 허용한다(들리는 것이 없다).
     *
     * @param m the element state / 요소 상태
     * @return true if the audio may reach the page / 오디오가 페이지에 닿아도 되면 true
     */
    private boolean mayReadAudio(Media m) {
        Element e = m.element();
        if (e == null || m.currentSrc.length() == 0) {
            return true;
        }
        Origin document = Origin.of(e.getOwnerDocument().getOriginUrl());
        return document.isSameOrigin(Origin.of(m.currentSrc));
    }

    /**
     * Removes the players that were closed from the list of live players.
     * <p>
     * 닫힌 재생기를 살아 있는 재생기 목록에서 지운다.
     */
    private void pruneLive() {
        // 닫힌 재생기를 목록에서 지운다.
        for (MediaPlayer p : live) {
            if (p.isClosed()) {
                live.remove(p);
            }
        }
    }

    /**
     * The resource cannot be used (network error, unsupported format, damaged headers): in the attribute mode the element fails, in the
     * children mode the candidate fails and the next one is tried.
     * <p>
     * 리소스를 쓸 수 없다(네트워크 오류, 지원하지 않는 형식, 손상된 헤더): 속성 모드에서는 요소가 실패하고, 자식 모드에서는 후보가 실패하여 다음
     * 후보를 시도한다.
     *
     * @param m   the element state / 요소 상태
     * @param why the reason / 이유
     */
    private void resourceFailed(Media m, String why) {
        Element c = m.candidate;
        if (c == null) {
            failedWithAttribute(m, why);
        } else {
            failedWithElements(m, c);
        }
    }

    /**
     * Tells the component whether the element has a resource that can be played, for the renderer: the state is recorded for the address that
     * the renderer looks at, which is the <code>src</code> attribute or, without it, the <code>src</code> of the first <code>source</code>
     * child. A resource that was found unplayable is therefore not shown as failed while another candidate of the element plays, and an
     * element that found nothing to play is shown with its unsupported address and its alternative text.
     * <p>
     * 요소에 재생할 수 있는 리소스가 있는지 렌더러를 위해 컴포넌트에 알린다: 상태는 렌더러가 보는 주소, 즉 <code>src</code> 속성 또는 그것이 없으면 첫
     * <code>source</code> 자식의 <code>src</code> 에 기록한다. 그러므로 재생할 수 없다고 알아낸 리소스도 요소의 다른 후보가 재생되는 동안에는 실패로
     * 표시되지 않고, 재생할 것을 찾지 못한 요소는 미지원 주소와 대체 텍스트와 함께 표시된다.
     *
     * @param m     the element state / 요소 상태
     * @param error null if it can be played, else the reason / 재생할 수 있으면 null, 아니면 이유
     */
    private void reportState(Media m, String error) {
        Element e = m.element();
        if (e == null) {
            return;
        }
        String src = e.getAttribute("src");
        // src 속성이 없으면 렌더러처럼 src 가 있는 첫 source 자식의 주소를 쓴다.
        if (src == null || src.trim().length() == 0) {
            src = null;
            // 자식을 순서대로 훑으며 src 속성이 있는 첫 source 를 찾는다.
            for (Node n = e.getFirstChild(); n != null && src == null; n = n.getNextSibling()) {
                if (isSource(n) && ((Element) n).getAttribute("src") != null) {
                    src = ((Element) n).getAttribute("src");
                }
            }
        }
        if (src == null || src.trim().length() == 0) {
            return;
        }
        String url = absolute(e, src.trim());
        if (url != null) {
            host().mediaStateChanged(url, error);
        }
    }

    /**
     * The player opened the data: the metadata is known. Sets the duration and the ready state, and queues the events of HTML5 4.7.10.7 in
     * their order: durationchange, loadedmetadata, loadeddata, progress, suspend, canplay, (playing), (play, playing for autoplay),
     * canplaythrough. All the data is in memory, so the state jumps from <code>HAVE_METADATA</code> to <code>HAVE_ENOUGH_DATA</code>.
     * <p>
     * 재생기가 데이터를 열었다: 메타데이터를 안다. 길이와 준비 상태를 정하고 HTML5 4.7.10.7 의 이벤트를 순서대로 올린다: durationchange,
     * loadedmetadata, loadeddata, progress, suspend, canplay, (playing), (autoplay 이면 play, playing), canplaythrough. 모든 데이터가 메모리에
     * 있으므로 상태는 <code>HAVE_METADATA</code> 에서 <code>HAVE_ENOUGH_DATA</code> 로 건너뛴다.
     *
     * @param m    the element state / 요소 상태
     * @param info the facts about the media / 미디어 정보
     */
    private void loaded(Media m, MediaInfo info) {
        Element e = m.element();
        if (e == null) {
            return;
        }
        m.info = info;
        m.duration = info.getDuration();
        reportState(m, null);
        queue(m, "durationchange");
        m.readyState = HAVE_METADATA;
        queue(m, "loadedmetadata");
        // 메타데이터 전에 정한 currentTime 이 있으면 그 위치로 건너뛴다.
        if (m.defaultStart > 0 && m.player != null) {
            m.seeking = true;
            queue(m, "seeking");
            m.player.seek(m.defaultStart);
        }
        m.defaultStart = 0;
        // 메타데이터를 알았으므로 재생 위치에서 활성인 텍스트 트랙 cue 를 계산한다.
        textTracks.timeMarchesOn(m);
        // preload="metadata": 재생이 요청되지 않았고 autoplay 도 없으면 메타데이터까지만 알고 멈춘다(네트워크는 놀고, 재생할 때 나머지 상태로 올라간다).
        String preload = e.getAttribute("preload");
        boolean autoplay = wantsAutoplay(e);
        if (preload != null && "metadata".equalsIgnoreCase(preload.trim()) && !autoplay && m.paused) {
            m.metadataOnly = true;
            post(new Task(m, m.epoch, K_RELEASE));
            queue(m, "progress");
            m.networkState = NETWORK_IDLE;
            queue(m, "suspend");
            return;
        }
        m.readyState = HAVE_ENOUGH_DATA;
        queue(m, "loadeddata");
        post(new Task(m, m.epoch, K_RELEASE));
        queue(m, "progress");
        m.networkState = NETWORK_IDLE;
        queue(m, "suspend");
        queue(m, "canplay");
        if (!m.paused) {
            notifyPlaying(m);
        }
        // 자동 재생: 스크립트가 재생이나 일시정지를 부르지 않았고 autoplay 속성이 있으면 재생을 시작한다.
        if (m.autoplaying && m.paused && autoplay) {
            m.paused = false;
            addPlaying(m);
            queue(m, "play");
            notifyPlaying(m);
        }
        queue(m, "canplaythrough");
        startPlayback(m);
    }

    /**
     * Tells the player to play if the element is not paused.
     * <p>
     * 요소가 일시정지가 아니면 재생기에 재생을 시킨다.
     *
     * @param m the element state / 요소 상태
     */
    private void startPlayback(Media m) {
        // 치명적 디코딩 오류로 죽은 리소스는 새 로드 전까지 다시 재생하지 않는다.
        if (m.player != null && m.info != null && !m.paused && m.error == null) {
            m.player.play();
        }
    }

    /**
     * The player reached the end of the media: queues <code>timeupdate</code>, then <code>pause</code> (the element becomes paused) and
     * <code>ended</code>. A report that no longer matches the state of the player (a seek came after it) is ignored.
     * <p>
     * 재생기가 미디어의 끝에 닿았다: <code>timeupdate</code>, 그다음 <code>pause</code>(요소가 일시정지가 된다)와 <code>ended</code> 를 올린다.
     * 재생기의 상태와 더 이상 맞지 않는 보고(그 뒤에 탐색이 왔다)는 무시한다.
     *
     * @param m the element state / 요소 상태
     */
    private void ended(Media m) {
        if (m.player == null || !m.player.isEnded()) {
            return;
        }
        textTracks.timeMarchesOn(m);
        queue(m, "timeupdate");
        m.started = false;
        if (!m.paused) {
            m.paused = true;
            removePlaying(m);
            queue(m, "pause");
            settlePlayPromises(m, "AbortError", "The play() request was interrupted because the media ended.");
        }
        queue(m, "ended");
    }

    /**
     * Playback failed after the resource was loaded (a damaged stream, or a machine without a sound device). This is the fatal decoding error of
     * HTML5 (W3C Recommendation 2014-10-28) 4.7.10.5, "If the media data is corrupted", which the WHATWG HTML standard keeps unchanged: the error
     * becomes <code>MEDIA_ERR_DECODE</code> with the reason as its message, the network state becomes <code>NETWORK_IDLE</code>, the element stops
     * delaying the load event and an <code>error</code> event is queued. Nothing else changes: <code>paused</code> stays as it is (no
     * <code>pause</code> event), the ready state stays, and no <code>play()</code> promise is rejected (the ones whose <code>playing</code> task
     * was queued are fulfilled by it). The resource is dead: the player is not asked to play again until a new load (the overall resource
     * selection was aborted); it stays only to answer the position and to seek.
     * <p>
     * 리소스를 로드한 뒤 재생이 실패했다(손상된 스트림이나 소리 장치가 없는 컴퓨터). 이것은 HTML5(W3C 권고안 2014-10-28) 4.7.10.5 의 "If the media data is
     * corrupted" 치명적 디코딩 오류이며 WHATWG HTML 표준도 같다: 오류는 이유를 메시지로 하는 <code>MEDIA_ERR_DECODE</code> 가 되고, 네트워크 상태는
     * <code>NETWORK_IDLE</code> 이 되며, 요소는 load 이벤트를 그만 늦추고 <code>error</code> 이벤트를 올린다. 그 밖에는 바뀌지 않는다:
     * <code>paused</code> 는 그대로(<code>pause</code> 이벤트 없음), 준비 상태도 그대로이고 <code>play()</code> 프라미스를 거부하지 않는다(<code>playing</code>
     * 작업이 이미 올라간 것은 그 작업이 이행한다). 리소스는 죽었다: 새 로드 전까지 재생기에 다시 재생을 시키지 않으며(전체 리소스 선택이 중단되었다)
     * 재생기는 위치를 답하고 탐색하는 데만 남는다.
     *
     * @param m the element state / 요소 상태
     * @param e the reason / 이유
     */
    private void playbackFailed(Media m, MediaException e) {
        if (m.error != null) {
            // 이미 치명적 오류로 끝난 리소스이다.
            return;
        }
        m.error = new MediaErrorValue(MEDIA_ERR_DECODE, e.getMessage());
        if (m.networkState != NETWORK_EMPTY) {
            m.networkState = NETWORK_IDLE;
        }
        setDelaying(m, false);
        m.started = false;
        // 더 이상 소리를 내지 않으므로 참조 없는 요소가 수거될 수 있게 한다(paused 값은 규격대로 그대로 둔다).
        removePlaying(m);
        if (m.player != null) {
            m.player.pause();
        }
        queue(m, "error");
    }

    /**
     * Closes the player and the link of an element.
     * <p>
     * 요소의 재생기와 연결을 닫는다.
     *
     * @param m the element state / 요소 상태
     */
    private void releasePlayer(Media m) {
        MediaPlayer p = m.player;
        m.player = null;
        m.link = null;
        m.info = null;
        if (p != null) {
            p.close();
            live.remove(p);
        }
    }

    /**
     * Keeps a playing element reachable.
     * <p>
     * 재생 중인 요소를 도달 가능하게 유지한다.
     *
     * @param m the element state / 요소 상태
     */
    private void addPlaying(Media m) {
        Element e = m.element();
        if (e != null) {
            playing.add(e);
        }
    }

    /**
     * Lets an element that does not play any more be collected.
     * <p>
     * 더 이상 재생하지 않는 요소를 수거될 수 있게 한다.
     *
     * @param m the element state / 요소 상태
     */
    private void removePlaying(Media m) {
        Element e = m.element();
        if (e != null) {
            playing.remove(e);
        }
    }

    /**
     * Reacts to a change of the document: a changed <code>src</code> starts the load algorithm, a <code>source</code> inserted into a media
     * element may start or continue the resource selection, a media element inserted without a load starts one, a removed media element is
     * paused, and the <code>loop</code> attribute is passed on to the player.
     * <p>
     * 문서의 변경에 반응한다: 바뀐 <code>src</code> 는 로드 알고리즘을 시작하고, 미디어 요소에 삽입된 <code>source</code> 는 리소스 선택을 시작하거나
     * 이어 가며, 로드 없이 삽입된 미디어 요소는 로드를 시작하고, 제거된 미디어 요소는 일시정지되며, <code>loop</code> 속성은 재생기에 전달된다.
     *
     * @param record    the change / 변경
     * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
     */
    void mutated(MutationRecord record, boolean connected) {
        if (disposed) {
            return;
        }
        if (record.getType() == MutationRecord.ATTRIBUTES) {
            attributeChanged(record);
        } else if (record.getType() == MutationRecord.CHILD_LIST) {
            childrenChanged(record, connected);
        }
    }

    /**
     * Handles a change of an attribute of a media element.
     * <p>
     * 미디어 요소의 속성 변경을 처리한다.
     *
     * @param record the change / 변경
     */
    private void attributeChanged(MutationRecord record) {
        if (!(record.getTarget() instanceof Element) || record.getAttributeName() == null) {
            return;
        }
        Element e = (Element) record.getTarget();
        // track 요소의 src 가 바뀌면 텍스트 트랙의 cue 를 비우고 다시 가져온다.
        if (TextTracks.isTrack(e)) {
            if ("src".equalsIgnoreCase(record.getAttributeName())) {
                textTracks.trackSourceChanged(e);
            }
            return;
        }
        if (!isMedia(e)) {
            return;
        }
        String name = record.getAttributeName().toLowerCase();
        // src 가 설정되거나 바뀌면 로드 알고리즘을 시작한다(src 를 지우는 것은 시작하지 않는다).
        if ("src".equals(name) && e.hasAttribute("src")) {
            load(state(e));
        } else if ("loop".equals(name)) {
            Media m = states.get(e);
            if (m != null && m.player != null) {
                m.player.setLoop(e.hasAttribute("loop"));
            }
        }
    }

    /**
     * Handles inserted and removed children.
     * <p>
     * 삽입되고 제거된 자식을 처리한다.
     *
     * @param record    the change / 변경
     * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
     */
    private void childrenChanged(MutationRecord record, boolean connected) {
        List<Node> added = record.getAddedNodes();
        Node target = record.getTarget();
        // 삽입된 하위 트리마다 source 와 미디어 요소를 살펴본다.
        for (int i = 0; i < added.size(); i++) {
            Node root = added.get(i);
            if (isMedia(target instanceof Element ? (Element) target : null) && isSource(root)) {
                sourceInserted(state((Element) target));
            }
            // 미디어 요소의 자식이 된 track 요소의 텍스트 트랙은 목록에 들어간다.
            if (isMedia(target instanceof Element ? (Element) target : null) && TextTracks.isTrack(root)) {
                textTracks.trackInserted(state((Element) target), (Element) root);
            }
            if (connected) {
                // 삽입된 하위 트리를 훑으며 아직 로드하지 않은 미디어 요소에 로드를 시작한다.
                for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
                    if (n instanceof Element && isMedia((Element) n)) {
                        insertedMedia((Element) n);
                    }
                }
            }
        }
        List<Node> removed = record.getRemovedNodes();
        // 제거된 하위 트리의 재생 중인 미디어 요소는 안정 상태에 문서에 없으면 일시정지한다.
        for (int i = 0; i < removed.size(); i++) {
            Node root = removed.get(i);
            // 미디어 요소에서 빠진 track 요소의 텍스트 트랙은 목록을 떠난다.
            if (isMedia(target instanceof Element ? (Element) target : null) && TextTracks.isTrack(root)) {
                textTracks.trackRemoved(state((Element) target), (Element) root);
            }
            // 제거된 하위 트리를 훑으며 재생 중인 미디어 요소를 찾는다.
            for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
                if (n instanceof Element && isMedia((Element) n)) {
                    Media m = states.get((Element) n);
                    if (m != null && !m.paused) {
                        post(new Task(m, m.epoch, K_PAUSE_CHECK));
                    }
                }
            }
        }
    }

    /**
     * A <code>source</code> element was inserted into a media element: when the element has no <code>src</code> and no resource selection ran
     * (<code>NETWORK_EMPTY</code>), the selection starts; when the selection waits for a source, it goes on.
     * <p>
     * 미디어 요소에 <code>source</code> 요소가 삽입되었다: 요소에 <code>src</code> 가 없고 리소스 선택이 실행되지 않았으면(<code>NETWORK_EMPTY</code>)
     * 선택을 시작하고, 선택이 소스를 기다리는 중이면 이어 간다.
     *
     * @param m the element state / 요소 상태
     */
    private void sourceInserted(Media m) {
        Element e = m.element();
        if (e == null) {
            return;
        }
        if (m.networkState == NETWORK_EMPTY && !e.hasAttribute("src")) {
            startSelection(m);
        } else if (m.waitingForSource) {
            m.waitingForSource = false;
            m.networkState = NETWORK_LOADING;
            setDelaying(m, true);
            post(new Task(m, m.epoch, K_ADVANCE));
        }
    }

    /**
     * A media element was inserted into the document: one that has a resource and did not start loading yet (a parser-created element whose
     * attributes were set before anybody listened) starts now.
     * <p>
     * 미디어 요소가 문서에 삽입되었다: 리소스가 있는데 아직 로드를 시작하지 않은 요소(누가 듣기 전에 속성이 정해진 파서가 만든 요소)는 지금 시작한다.
     *
     * @param e the element / 요소
     */
    void insertedMedia(Element e) {
        Media m = state(e);
        if (!m.everLoaded && m.networkState == NETWORK_EMPTY && (e.hasAttribute("src") || firstSourceChild(e) != null)) {
            load(m);
        }
    }

    /**
     * Returns the first <code>source</code> child of an element.
     * <p>
     * 요소의 첫 <code>source</code> 자식을 반환한다.
     *
     * @param e the element / 요소
     * @return the child or null / 자식. 없으면 null
     */
    private static Element firstSourceChild(Element e) {
        // 자식을 순서대로 훑어 첫 source 요소를 찾는다.
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (isSource(n)) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Returns the current playback position in seconds.
     * <p>
     * 현재 재생 위치(초)를 반환한다.
     *
     * @param m the element state / 요소 상태
     * @return the position / 위치
     */
    double position(Media m) {
        if (m.readyState == HAVE_NOTHING) {
            return m.defaultStart;
        }
        return m.player == null ? 0 : m.player.getPosition();
    }

    /**
     * Sets <code>currentTime</code>: before the metadata it is the default playback start position, afterwards it seeks.
     * <p>
     * <code>currentTime</code> 을 정한다: 메타데이터 전에는 기본 재생 시작 위치이고, 그 뒤에는 탐색한다.
     *
     * @param m     the element state / 요소 상태
     * @param value the new position / 새 위치
     */
    void setPosition(Media m, double value) {
        if (m.readyState == HAVE_NOTHING) {
            m.defaultStart = value;
            return;
        }
        seek(m, value);
    }

    /**
     * The seeking algorithm (HTML5 4.7.10.9): the position is limited to the seekable range, <code>seeking</code> becomes true, the
     * <code>seeking</code> event is queued, and the player moves; its report finishes the seek with <code>timeupdate</code> and
     * <code>seeked</code>.
     * <p>
     * 탐색 알고리즘(HTML5 4.7.10.9): 위치를 탐색 가능한 범위로 제한하고, <code>seeking</code> 이 true 가 되며, <code>seeking</code> 이벤트를 올리고,
     * 재생기가 움직인다. 재생기의 보고가 <code>timeupdate</code> 와 <code>seeked</code> 로 탐색을 끝낸다.
     *
     * @param m     the element state / 요소 상태
     * @param value the new position / 새 위치
     */
    private void seek(Media m, double value) {
        if (m.player == null || m.info == null || m.player.getSeekable().size() == 0) {
            return;
        }
        double v = value < 0 ? 0 : value;
        if (!Double.isInfinite(m.duration) && !Double.isNaN(m.duration) && v > m.duration) {
            v = m.duration;
        }
        m.seeking = true;
        queue(m, "seeking");
        textTracks.positionJumped(m);
        m.player.seek(v);
    }

    /**
     * Implements <code>play()</code>: starts the resource selection if none ran, restarts an ended element, sets <code>paused</code> to false
     * with the events <code>play</code> and <code>waiting</code> or <code>playing</code>, and returns a promise that is fulfilled when the
     * <code>playing</code> event fires and rejected when the element cannot play. A <code>video</code> element always rejects with a
     * <code>NotSupportedError</code> (plan item P18-09).
     * <p>
     * <code>play()</code> 를 구현한다: 선택이 실행되지 않았으면 리소스 선택을 시작하고, 끝난 요소는 다시 시작하며, <code>paused</code> 를 false 로 하고
     * <code>play</code> 와 <code>waiting</code> 또는 <code>playing</code> 이벤트를 올리고, <code>playing</code> 이벤트가 발생하면 이행되고 요소가 재생할
     * 수 없으면 거부되는 프라미스를 반환한다. <code>video</code> 요소는 언제나 <code>NotSupportedError</code> 로 거부한다(계획 P18-09).
     *
     * @param m the element state / 요소 상태
     * @return the promise / 프라미스
     */
    JsObject play(Media m) {
        Deferred d = newDeferred();
        Element e = m.element();
        if (e == null) {
            return d.promise;
        }
        if (m.video) {
            m.everLoaded = true;
            videoUnsupported(m, "video playback is not supported");
            d.reject(DomExceptionObjects.create(page.getBridge(), "NotSupportedError", "video playback is not supported"));
            return d.promise;
        }
        requestPlayback(m, e, d);
        return d.promise;
    }

    /**
     * Runs the shared internal play steps for a script or the user agent controls.
     * <p>
     * 스크립트 또는 사용자 에이전트 컨트롤을 위한 공통 내부 재생 단계를 실행한다.
     * @param m the media state / 미디어 상태
     * @param e the live media element / 살아 있는 미디어 요소
     * @param request the script promise to settle, or null for controls / 확정할 스크립트 프라미스. 컨트롤이면 null
     */
    private void requestPlayback(Media m, Element e, Deferred request) {
        if (m.error != null && m.error.getCode() == MEDIA_ERR_SRC_NOT_SUPPORTED) {
            // 스크립트가 재생을 요청했으면 프라미스를 거부한다. 컨트롤의 오류 상태는 이미 표시되어 있다.
            if (request != null) {
                request.reject(DomExceptionObjects.create(page.getBridge(), "NotSupportedError", "The element has no supported sources."));
            }
            return;
        }
        if (m.networkState == NETWORK_EMPTY) {
            startSelection(m);
        }
        // 끝난 미디어는 처음부터 다시 재생한다.
        if (m.readyState >= HAVE_METADATA && m.player != null && m.player.isEnded() && !e.hasAttribute("loop")) {
            seek(m, 0);
        }
        boolean wasPaused = m.paused;
        if (request != null) {
            m.playPromises.add(request);
        }
        if (wasPaused) {
            m.paused = false;
            addPlaying(m);
            queue(m, "play");
            // 데이터가 모자라면 waiting, 충분하면 playing 과 함께 프라미스를 이행한다(HTML internal play steps).
            if (m.readyState <= HAVE_CURRENT_DATA) {
                queue(m, "waiting");
            } else {
                notifyPlaying(m);
            }
        } else if (request != null && m.readyState >= HAVE_FUTURE_DATA) {
            // 이미 재생 중이면 대기 중인 프라미스를 가져와 다음 작업에서 이행한다.
            Task t = new Task(m, m.epoch, K_RESOLVE);
            t.promises = takePlayPromises(m);
            post(t);
        }
        m.autoplaying = false;
        // preload=none 으로 미룬 가져오기는 이제 시작한다.
        if (m.deferredUrl != null) {
            String url = m.deferredUrl;
            m.deferredUrl = null;
            setDelaying(m, true);
            beginFetch(m, url);
        }
        promoteMetadataOnly(m);
        startPlayback(m);
        textTracks.playbackChanged(m);
    }

    /**
     * A play request ends the wait of an element that stopped at its metadata because of <code>preload="metadata"</code>: the state rises
     * to <code>HAVE_ENOUGH_DATA</code> (all the data is in memory) with the events <code>loadeddata</code>, <code>canplay</code>,
     * <code>playing</code> and <code>canplaythrough</code>.
     * <p>
     * 재생 요청은 <code>preload="metadata"</code> 때문에 메타데이터에서 멈춘 요소의 기다림을 끝낸다: 상태가 <code>HAVE_ENOUGH_DATA</code> 로
     * 올라가며(모든 데이터가 메모리에 있다) <code>loadeddata</code>, <code>canplay</code>, <code>playing</code>, <code>canplaythrough</code> 이벤트가
     * 따른다.
     *
     * @param m the element state / 요소 상태
     */
    private void promoteMetadataOnly(Media m) {
        if (!m.metadataOnly || m.paused) {
            return;
        }
        m.metadataOnly = false;
        m.readyState = HAVE_ENOUGH_DATA;
        queue(m, "loadeddata");
        queue(m, "canplay");
        notifyPlaying(m);
        queue(m, "canplaythrough");
    }

    /**
     * Implements <code>pause()</code>.
     * <p>
     * <code>pause()</code> 를 구현한다.
     *
     * @param m the element state / 요소 상태
     */
    void pause(Media m) {
        if (m.networkState == NETWORK_EMPTY && !m.video && m.element() != null && (m.element().hasAttribute("src") || firstSourceChild(m.element()) != null)) {
            startSelection(m);
        }
        pauseAlgorithm(m);
    }

    /**
     * The pause algorithm of HTML5 4.7.10.8: the autoplaying flag is cleared; a playing element becomes paused with the events
     * <code>timeupdate</code> and <code>pause</code>, and its pending <code>play()</code> promises are rejected with an
     * <code>AbortError</code>.
     * <p>
     * HTML5 4.7.10.8 의 일시정지 알고리즘: autoplaying 플래그를 지운다. 재생 중인 요소는 <code>timeupdate</code>, <code>pause</code> 이벤트와 함께
     * 일시정지가 되고, 기다리던 <code>play()</code> 프라미스는 <code>AbortError</code> 로 거부된다.
     *
     * @param m the element state / 요소 상태
     */
    private void pauseAlgorithm(Media m) {
        m.autoplaying = false;
        if (!m.paused) {
            m.paused = true;
            m.started = false;
            removePlaying(m);
            queue(m, "timeupdate");
            queue(m, "pause");
            settlePlayPromises(m, "AbortError", "The play() request was interrupted by a call to pause().");
        }
        if (m.player != null) {
            m.player.pause();
        }
        textTracks.playbackChanged(m);
    }

    /**
     * Sets the volume; the <code>volumechange</code> event is queued when it changed.
     * <p>
     * 음량을 정한다. 바뀌었으면 <code>volumechange</code> 이벤트를 올린다.
     *
     * @param m     the element state / 요소 상태
     * @param value the volume, 0 to 1 / 음량(0에서 1)
     */
    void setVolume(Media m, double value) {
        if (m.volume != value) {
            m.volume = value;
            if (m.player != null) {
                m.player.setVolume(value);
            }
            queue(m, "volumechange");
        }
    }

    /**
     * Sets <code>muted</code>; the <code>volumechange</code> event is queued when it changed.
     * <p>
     * <code>muted</code> 를 정한다. 바뀌었으면 <code>volumechange</code> 이벤트를 올린다.
     *
     * @param m     the element state / 요소 상태
     * @param value the new value / 새 값
     */
    void setMuted(Media m, boolean value) {
        if (m.muted != value) {
            m.muted = value;
            if (m.player != null) {
                m.player.setMuted(value);
            }
            queue(m, "volumechange");
        }
    }

    /**
     * Sets <code>playbackRate</code> (HTML5 4.7.10.8); the <code>ratechange</code> event is queued when it changed. The caller has checked
     * that the rate is supported ({@link MediaPlayer#isRateSupported(double)}).
     * <p>
     * <code>playbackRate</code> 를 정한다(HTML5 4.7.10.8). 바뀌었으면 <code>ratechange</code> 이벤트를 올린다. 지원하는 속도인지는 호출자가
     * 확인했다({@link MediaPlayer#isRateSupported(double)}).
     *
     * @param m     the element state / 요소 상태
     * @param value the rate / 속도
     */
    void setPlaybackRate(Media m, double value) {
        if (m.playbackRate != value) {
            m.playbackRate = value;
            if (m.player != null) {
                m.player.setRate(value);
            }
            queue(m, "ratechange");
            textTracks.playbackChanged(m);
        }
    }

    /**
     * Sets <code>defaultPlaybackRate</code> (HTML5 4.7.10.8); the <code>ratechange</code> event is queued when it changed. The rate that
     * plays now does not change (a load starts with the default).
     * <p>
     * <code>defaultPlaybackRate</code> 를 정한다(HTML5 4.7.10.8). 바뀌었으면 <code>ratechange</code> 이벤트를 올린다. 지금 재생되는 속도는 바뀌지
     * 않는다(새 로드가 기본값으로 시작한다).
     *
     * @param m     the element state / 요소 상태
     * @param value the rate / 속도
     */
    void setDefaultPlaybackRate(Media m, double value) {
        if (m.defaultRate != value) {
            m.defaultRate = value;
            queue(m, "ratechange");
        }
    }

    /**
     * Sets <code>preservesPitch</code>: whether a speed other than 1.0 keeps the pitch.
     * <p>
     * <code>preservesPitch</code> 를 정한다: 1.0 이 아닌 속도에서 음높이를 유지할지 여부.
     *
     * @param m     the element state / 요소 상태
     * @param value true to keep the pitch / 음높이를 유지하려면 true
     */
    void setPreservesPitch(Media m, boolean value) {
        m.preservesPitch = value;
        if (m.player != null) {
            m.player.setPreservesPitch(value);
        }
    }

    /**
     * Returns <code>ended</code>: the playback is at the end of the media and the element does not loop.
     * <p>
     * <code>ended</code> 를 반환한다: 재생이 미디어의 끝에 있고 요소가 반복하지 않는다.
     *
     * @param m the element state / 요소 상태
     * @return true if ended / 끝났으면 true
     */
    boolean isEnded(Media m) {
        Element e = m.element();
        return e != null && m.readyState >= HAVE_METADATA && m.player != null && m.player.isEnded() && !e.hasAttribute("loop");
    }

    /**
     * Returns <code>buffered</code>: the whole media once it is loaded (the resource is fetched as a whole).
     * <p>
     * <code>buffered</code> 를 반환한다: 로드가 끝나면 미디어 전체(리소스는 통째로 가져온다).
     *
     * @param m the element state / 요소 상태
     * @return the ranges / 범위
     */
    MediaRanges buffered(Media m) {
        if (m.player == null || m.info == null) {
            return MediaRanges.EMPTY;
        }
        double d = m.duration;
        return MediaRanges.of(0, Double.isInfinite(d) ? m.player.getPosition() : d);
    }

    /**
     * Returns <code>seekable</code>.
     * <p>
     * <code>seekable</code> 을 반환한다.
     *
     * @param m the element state / 요소 상태
     * @return the ranges / 범위
     */
    MediaRanges seekable(Media m) {
        return m.player == null || m.info == null ? MediaRanges.EMPTY : m.player.getSeekable();
    }

    /**
     * Returns <code>played</code>.
     * <p>
     * <code>played</code> 를 반환한다.
     *
     * @param m the element state / 요소 상태
     * @return the ranges / 범위
     */
    MediaRanges played(Media m) {
        return m.player == null || m.info == null ? MediaRanges.EMPTY : m.player.getPlayed();
    }

    /**
     * Implements <code>canPlayType</code>: <code>video</code> elements answer the empty string for every type (plan item P18-09), audio
     * elements ask the decoders of the component.
     * <p>
     * <code>canPlayType</code> 을 구현한다: <code>video</code> 요소는 모든 타입에 빈 문자열로 답하고(계획 P18-09), 오디오 요소는 컴포넌트의 디코더에
     * 묻는다.
     *
     * @param e    the element / 요소
     * @param type the type / 타입
     * @return <code>""</code>, <code>"maybe"</code> or <code>"probably"</code> / <code>""</code>, <code>"maybe"</code>, <code>"probably"</code>
     */
    String canPlayType(Element e, String type) {
        if ("video".equals(e.getLocalName()) || type == null) {
            return "";
        }
        return AudioDecoders.canPlayType(type);
    }

    /**
     * Creates a pending promise for <code>play()</code>.
     * <p>
     * <code>play()</code> 를 위한 대기 중인 프라미스를 만든다.
     *
     * @return the promise and its functions / 프라미스와 그 함수
     */
    private Deferred newDeferred() {
        JsRealm realm = page.getRealm();
        final JsValue[] fns = new JsValue[2];
        NativeFunction executor = realm.newFunction("", 2, new NativeFunction.Call() {
            /**
             * Keeps the resolving functions.
             * <p>
             * 확정 함수를 보관한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                fns[0] = Js.arg(args, 0);
                fns[1] = Js.arg(args, 1);
                return Js.UNDEFINED;
            }
        });
        JsObject p = Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] { executor });
        return new Deferred(p, fns[0], fns[1]);
    }

    /**
     * Silences the page at once: every player is paused (callable from any thread without the page lock, and does not wait).
     * <p>
     * 페이지의 소리를 즉시 끈다: 모든 재생기를 일시정지한다(페이지 잠금 없이 어느 스레드에서나 부를 수 있으며 기다리지 않는다).
     */
    void stopSoundNow() {
        // 살아 있는 재생기를 모두 일시정지한다.
        for (MediaPlayer p : live) {
            p.pause();
        }
    }

    /**
     * Releases everything: fetches are cancelled and every player is closed (the thread, the decoder and the output). Results that arrive
     * later are ignored.
     * <p>
     * 모든 것을 해제한다: 가져오기를 취소하고 모든 재생기(스레드, 디코더, 출력)를 닫는다. 나중에 도착하는 결과는 무시한다.
     */
    void dispose() {
        disposed = true;
        textTracks.dispose();
        List<Media> all = new ArrayList<Media>(states.values());
        // 가져오기를 취소하고 재생기를 먼저 모두 일시정지하여 소리부터 끈다.
        for (Media m : all) {
            textTracks.disposeMedia(m);
            cancelFetch(m);
            if (m.player != null) {
                m.player.pause();
            }
        }
        // 재생기를 하나씩 닫는다.
        for (Media m : all) {
            releasePlayer(m);
            m.playPromises.clear();
            m.promisesInFlight.clear();
        }
        stopSoundNow();
        // 아직 남은 재생기도 닫는다.
        for (MediaPlayer p : live) {
            p.close();
        }
        live.clear();
        states.clear();
        playing.clear();
        delaying = 0;
    }
}
