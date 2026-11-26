package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.playback.ScriptedDecoder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script side of the media elements (plan items P18-05-c, P18-08, P18-09; FEATURE_MATRIX AUD-10, AUD-11): the events and states of
 * the load algorithm in the order of HTML5 4.7.10, the resource selection with <code>src</code> and <code>source</code> children (the
 * <code>type</code> and <code>media</code> of the candidates, a failing candidate, waiting for a <code>source</code>), <code>play()</code>,
 * <code>pause()</code>, seeking, <code>ended</code>, the volume, <code>canPlayType()</code>, the time ranges, <code>new Audio()</code>,
 * <code>preload="none"</code>, a new load that cancels the old one, removal from the document, disposal, and the explicit unsupported result of
 * <code>video</code>. The resources are served by a fake service that the test completes by hand, and the sound goes to a fake output
 * ({@link FakeOutputs}); nothing is heard (no real sound device is used).
 * <p>
 * 미디어 요소의 스크립트 쪽 시험(계획 P18-05-c, P18-08, P18-09; FEATURE_MATRIX AUD-10, AUD-11): HTML5 4.7.10 순서의 로드 알고리즘 이벤트와 상태,
 * <code>src</code> 와 <code>source</code> 자식의 리소스 선택(후보의 <code>type</code>, <code>media</code>, 실패하는 후보, <code>source</code> 를 기다림),
 * <code>play()</code>, <code>pause()</code>, 탐색, <code>ended</code>, 음량, <code>canPlayType()</code>, 시간 범위, <code>new Audio()</code>,
 * <code>preload="none"</code>, 이전 로드를 취소하는 새 로드, 문서에서 제거, 폐기, <code>video</code> 의 명시적인 미지원 결과. 리소스는 시험이 손으로
 * 완료시키는 가짜 서비스가 제공하고 소리는 가짜 출력({@link FakeOutputs})으로 가므로 아무 소리도 들리지 않는다(실제 소리 장치는 쓰지 않는다).
 */
public class MediaElementsTest {

    /** Pages created by the current case. / 현재 사례에서 만든 페이지. */
    private final List<PageFixture> fixtures = new ArrayList<PageFixture>();

    /**
     * Creates the suite (required by the runner) and registers the scripted decoder.
     * <p>
     * 스위트를 만들고(실행기가 필요로 함) 스크립트 디코더를 등록한다.
     */
    public MediaElementsTest() {
        ScriptedDecoder.install();
    }

    /** Closes the pages and their player threads after each case. / 각 사례 뒤 페이지와 재생 스레드를 닫는다. */
    public void tearDown() {
        // 재생 상태를 확인하던 중 실패해도 모든 시험 페이지를 폐기한다.
        for (PageFixture page : fixtures) {
            page.scripts.dispose();
        }
    }

    /**
     * A fake media service: it records the requests and completes them when the test says so.
     * <p>
     * 가짜 미디어 서비스: 요청을 기록하고 시험이 지시할 때 완료시킨다.
     */
    private static final class FakeMedia implements MediaRequestSource {
        /**
         * The requests in order.
         * <p>
         * 순서대로의 요청.
         */
        final List<Req> requests = Collections.synchronizedList(new ArrayList<Req>());

        /**
         * One request.
         * <p>
         * 요청 하나.
         */
        final class Req implements MediaRequestSource.Request {
            /**
             * The requested URL.
             * <p>
             * 요청된 URL.
             */
            final String url;

            /**
             * The receiver.
             * <p>
             * 받는 쪽.
             */
            final Callback callback;

            /**
             * True after the page cancelled the request.
             * <p>
             * 페이지가 요청을 취소한 뒤에는 true.
             */
            volatile boolean cancelled;

            /**
             * Creates the record.
             * <p>
             * 기록을 만든다.
             *
             * @param url      the URL / URL
             * @param callback the receiver / 받는 쪽
             */
            Req(String url, Callback callback) {
                this.url = url;
                this.callback = callback;
            }

            /**
             * Records the cancellation.
             * <p>
             * 취소를 기록한다.
             */
            public void cancel() {
                cancelled = true;
            }
        }

        /**
         * Records a request.
         * <p>
         * 요청을 기록한다.
         *
         * @param absoluteUrl the URL / URL
         * @param callback    the receiver / 받는 쪽
         * @return the handle / 핸들
         */
        public Request request(String absoluteUrl, Callback callback) {
            Req r = new Req(absoluteUrl, callback);
            requests.add(r);
            return r;
        }

        /**
         * Completes a request successfully.
         * <p>
         * 요청을 성공으로 완료시킨다.
         *
         * @param index the request number / 요청 번호
         * @param data  the bytes / 바이트
         * @param mime  the MIME type / MIME 타입
         */
        void ok(int index, byte[] data, String mime) {
            requests.get(index).callback.done(data, mime, null);
        }

        /**
         * Completes a request as failed.
         * <p>
         * 요청을 실패로 완료시킨다.
         *
         * @param index the request number / 요청 번호
         */
        void fail(int index) {
            requests.get(index).callback.done(null, null, "404 Not Found");
        }

        /**
         * Returns the number of requests.
         * <p>
         * 요청의 수를 반환한다.
         *
         * @return the number / 수
         */
        int count() {
            return requests.size();
        }
    }

    /**
     * The script that logs the events of every element (captured at the document, as they do not bubble) and the states in the page.
     * <p>
     * 모든 요소의 이벤트를 기록하는 스크립트(버블링하지 않으므로 문서에서 캡처한다)와 페이지의 상태.
     */
    private static final String PRELUDE = "<script>var log = []; var names = ['loadstart','progress','suspend','abort','error','emptied','stalled',"
            + "'loadedmetadata','loadeddata','canplay','canplaythrough','playing','waiting','seeking','seeked','ended','durationchange','timeupdate',"
            + "'play','pause','ratechange','volumechange'];"
            + "names.forEach(function (n) { document.addEventListener(n, function (e) { log.push((e.target.id || e.target.tagName) + ':' + n); }, true); });"
            + "function events() { return log.filter(function (x) { return x.indexOf('timeupdate') < 0; }).join(','); }</script>";

    /**
     * Makes a mono WAV file of the scripted ramp.
     * <p>
     * 스크립트 램프의 모노 WAV 파일을 만든다.
     *
     * @param frames the number of frames at 8 kHz / 8 kHz 의 프레임 수
     * @return the file / 파일
     */
    private static byte[] wav(int frames) {
        return AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, frames), -1);
    }

    /**
     * Creates a fixture with a fake media service and a fake sound output.
     * <p>
     * 가짜 미디어 서비스와 가짜 소리 출력이 있는 시험 도구를 만든다.
     *
     * @param media   the service / 서비스
     * @param outputs the outputs / 출력
     * @return the fixture (not loaded yet) / 시험 도구(아직 로드 전)
     */
    private PageFixture fixture(FakeMedia media, FakeOutputs outputs) {
        PageFixture f = new PageFixture();
        fixtures.add(f);
        f.env.media = media;
        f.env.outputs = outputs;
        return f;
    }

    /**
     * Runs the queued tasks of the page until an expression is true: the player reports from its own thread, so the test lets time pass.
     * <p>
     * 식이 참이 될 때까지 페이지에 올라온 작업을 실행한다: 재생기는 자기 스레드에서 보고하므로 시험이 시간을 흘려 보낸다.
     *
     * @param f       the fixture / 시험 도구
     * @param expr    the expression / 식
     * @param message the message of a failure / 실패 메시지
     * @throws Exception on failure / 실패 시
     */
    private static void until(PageFixture f, String expr, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 식이 참이 될 때까지 작업을 실행하며 짧게 쉰다.
        while (System.currentTimeMillis() < end) {
            f.advance(0);
            if ("true".equals(f.scripts.evaluate(expr, "t"))) {
                return;
            }
            Thread.sleep(2);
        }
        throw new AssertionError(message + " (waiting for: " + expr + "; events: " + f.scripts.evaluate("events()", "t") + ")");
    }

    /**
     * Runs the queued tasks for a moment.
     * <p>
     * 올라온 작업을 잠깐 실행한다.
     *
     * @param f the fixture / 시험 도구
     * @throws Exception on failure / 실패 시
     */
    private static void settleBriefly(PageFixture f) throws Exception {
        // 재생기의 보고가 도착할 시간을 주며 작업을 몇 번 실행한다.
        for (int i = 0; i < 10; i++) {
            f.advance(0);
            Thread.sleep(5);
        }
    }

    /**
     * Evaluates an expression in the page.
     * <p>
     * 페이지에서 식을 평가한다.
     *
     * @param f    the fixture / 시험 도구
     * @param expr the expression / 식
     * @return the result as text / 문자열 결과
     */
    private static String js(PageFixture f, String expr) {
        return f.scripts.evaluate(expr, "t");
    }

    /**
     * The load of a resource in the order of the Recommendation: <code>loadstart</code> when the selection runs, then the metadata events and
     * the ready states, and the window <code>load</code> event waits until the first data is ready. The state that the script reads: network
     * and ready state, duration, paused, ended, error, the absolute <code>currentSrc</code>.
     * <p>
     * 권고안 순서의 리소스 로드: 선택이 실행되면 <code>loadstart</code>, 이어 메타데이터 이벤트와 준비 상태가 오고, window <code>load</code> 이벤트는
     * 첫 데이터가 준비될 때까지 기다린다. 스크립트가 읽는 상태: 네트워크/준비 상태, 길이, paused, ended, error, 절대 <code>currentSrc</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoadEventsAndState() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(f);
        Check.equal(1, media.count(), "the resource was requested");
        Check.equal("http://example.test/dir/s.wav", media.requests.get(0).url, "the URL is resolved against the document");
        Check.equal("interactive", f.scripts.getReadyState(), "the element delays the window load event");
        Check.equal("a:loadstart", js(f, "events()"), "loadstart comes first");
        Check.equal("2,0,NaN,true,false,,http://example.test/dir/s.wav",
                js(f, "var a = document.getElementById('a'); [a.networkState, a.readyState, a.duration, a.paused, a.ended, a.error, a.currentSrc].join(',')"),
                "while loading");
        media.ok(0, wav(4000), "audio/wav");
        until(f, "events().indexOf('canplaythrough') >= 0", "the first data is ready");
        Check.equal("a:loadstart,a:durationchange,a:loadedmetadata,a:loadeddata,a:progress,a:suspend,a:canplay,a:canplaythrough", js(f, "events()"),
                "the order of the events");
        Check.equal("1,4,0.5,true,false,", js(f, "[a.networkState, a.readyState, a.duration, a.paused, a.ended, a.error].join(',')"), "loaded");
        Check.equal("[http://example.test/dir/s.wav=ok]", String.valueOf(f.env.mediaStates), "the renderer is told that the resource can be played");
        until(f, "document.readyState === 'complete'", "the window load event follows the first data");
        Check.equal("0,0.5", js(f, "[a.currentTime, a.duration].join(',')"), "position and duration");
    }

    /**
     * An element with <code>autoplay</code> starts playing when enough data is ready: <code>play</code>, <code>playing</code> between
     * <code>canplay</code> and <code>canplaythrough</code>; the sound reaches the output; at the end <code>timeupdate</code>, <code>pause</code>,
     * <code>ended</code>, and the element is paused and ended.
     * <p>
     * <code>autoplay</code> 가 있는 요소는 데이터가 충분하면 재생을 시작한다: <code>canplay</code> 와 <code>canplaythrough</code> 사이에
     * <code>play</code>, <code>playing</code>. 소리는 출력에 도달한다. 끝에서는 <code>timeupdate</code>, <code>pause</code>, <code>ended</code> 이고
     * 요소는 일시정지가 되고 끝난 상태이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAutoplayToTheEnd() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav' autoplay></audio>");
        settleBriefly(f);
        media.ok(0, wav(4000), "audio/wav");
        until(f, "events().indexOf('ended') >= 0", "the end of the media");
        Check.equal("a:loadstart,a:durationchange,a:loadedmetadata,a:loadeddata,a:progress,a:suspend,a:canplay,a:play,a:playing,a:canplaythrough,a:pause,a:ended",
                js(f, "events()"), "the order of the events of an autoplay");
        Check.equal("true,true,0.5,4", js(f, "var a = document.getElementById('a'); [a.paused, a.ended, a.currentTime, a.readyState].join(',')"), "state after the end");
        Check.isTrue(java.util.Arrays.equals(ScriptedDecoder.pcm(0, 4000), outs.outputs.get(0).written()), "all the sound reached the output");
        Check.isTrue(Integer.parseInt(js(f, "log.filter(function (x) { return x.indexOf('timeupdate') >= 0; }).length")) >= 1, "a timeupdate at the end");
    }

    /**
     * <code>play()</code> returns a promise that is fulfilled when the <code>playing</code> event fires; calling it before the data is loaded
     * queues <code>play</code> and <code>waiting</code> first; a second element is not affected.
     * <p>
     * <code>play()</code> 는 <code>playing</code> 이벤트가 발생하면 이행되는 프라미스를 반환한다. 데이터가 로드되기 전에 부르면 <code>play</code> 와
     * <code>waiting</code> 이 먼저 올라가며, 다른 요소는 영향을 받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlayPromiseAndPlayBeforeLoad() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(1000);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio><audio id=b></audio>");
        settleBriefly(f);
        js(f, "var a = document.getElementById('a'); var result = []; a.play().then(function () { result.push('resolved'); }, function (e) { result.push(e.name); });"
                + " result.push(typeof a.play().then)");
        settleBriefly(f);
        Check.equal("0,false", js(f, "[a.readyState, a.paused].join(',')"), "playing was requested before the data came");
        Check.equal("object", js(f, "typeof a.play()"), "play returns an object");
        media.ok(0, wav(16000), "audio/wav");
        until(f, "result.indexOf('resolved') >= 0", "the promise is fulfilled by the playing event");
        String events = js(f, "events()");
        Check.isTrue(events.indexOf("a:play,a:waiting") >= 0, "play and waiting before the data: " + events);
        Check.isTrue(events.indexOf("a:canplay,a:playing") >= 0, "playing after canplay: " + events);
        Check.equal("", js(f, "document.getElementById('b').currentSrc"), "the other element did not start");
        js(f, "a.pause()");
        settleBriefly(f);
        Check.equal("true", js(f, "String(a.paused)"), "paused");
    }

    /**
     * The promise of <code>play()</code> follows the <code>playing</code> event and not the sound device (HTML "notify about playing the media
     * element"): with an output that takes no data for a while (Java Sound over a remote desktop needs seconds), the promise is fulfilled in the
     * task of <code>playing</code> although nothing reached the device; a later <code>pause()</code> does not reject it; a <code>play()</code> on an
     * element that already plays is fulfilled at once, also while the device still waits; a new load settles a promise whose task did not run.
     * <p>
     * <code>play()</code> 의 프라미스는 소리 장치가 아니라 <code>playing</code> 이벤트를 따른다(HTML "notify about playing the media element"): 한동안 데이터를
     * 받지 않는 출력(원격 데스크톱의 Java Sound 는 몇 초 걸린다)에서도 장치에 아무것도 닿지 않았는데 <code>playing</code> 의 작업에서 이행되고, 뒤의
     * <code>pause()</code> 가 거부하지 않으며, 이미 재생 중인 요소의 <code>play()</code> 는 장치가 기다리는 동안에도 곧바로 이행되고, 작업이 실행되지 않은
     * 프라미스는 새 로드가 확정한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlayPromiseFollowsPlayingNotTheDevice() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(1000);
        outs.holdWrites = true;
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(f);
        media.ok(0, wav(16000), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "the data was loaded");
        js(f, "var a = document.getElementById('a'); var result = []; log.length = 0;"
                + " a.addEventListener('playing', function () { result.push('playing:' + result.length); });"
                + " a.play().then(function () { result.push('resolved'); }, function (e) { result.push(e.name); });");
        until(f, "result.indexOf('resolved') >= 0", "the promise is fulfilled although the device took nothing");
        Check.equal("playing:0,resolved", js(f, "result.join(',')"), "the promise follows the playing event");
        Check.equal(0, outs.outputs.isEmpty() ? 0 : outs.outputs.get(0).written().length, "nothing reached the device yet");
        js(f, "a.play().then(function () { result.push('again'); }, function (e) { result.push('again:' + e.name); });");
        until(f, "result.indexOf('again') >= 0", "play() on a playing element is fulfilled while the device waits");
        js(f, "a.pause();");
        settleBriefly(f);
        Check.equal("playing:0,resolved,again", js(f, "result.join(',')"), "pause() rejects nothing that was fulfilled");
        // 작업이 실행되기 전에 새 로드가 오면 그 작업이 이행하려던 프라미스는 곧바로 이행된다.
        js(f, "a.play().then(function () { result.push('beforeLoad'); }, function (e) { result.push('beforeLoad:' + e.name); }); a.load();");
        until(f, "result.length === 4", "the promise of the removed task was settled");
        Check.equal("beforeLoad", js(f, "result[3]"), "a new load fulfils the promise of the playing task it removes");
        outs.holdWrites = false;
    }

    /**
     * <code>pause()</code> and <code>currentTime</code>: pausing queues <code>timeupdate</code> and <code>pause</code>, rejects nothing that
     * already resolved, keeps the position; setting <code>currentTime</code> shows the new value at once, sets <code>seeking</code>, and
     * <code>seeking</code>, <code>timeupdate</code>, <code>seeked</code> follow; a value past the end moves to the end.
     * <p>
     * <code>pause()</code> 와 <code>currentTime</code>: 일시정지는 <code>timeupdate</code>, <code>pause</code> 를 올리고 이미 이행된 것을 거부하지 않으며
     * 위치를 유지한다. <code>currentTime</code> 을 정하면 새 값이 곧바로 보이고 <code>seeking</code> 이 되며 <code>seeking</code>,
     * <code>timeupdate</code>, <code>seeked</code> 가 따른다. 끝을 넘는 값은 끝으로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseAndSeek() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(500);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(f);
        media.ok(0, wav(16000), "audio/wav");
        until(f, "events().indexOf('canplaythrough') >= 0", "loaded");
        js(f, "var a = document.getElementById('a'); log.length = 0; a.play();");
        until(f, "events().indexOf('playing') >= 0", "playing");
        js(f, "a.pause();");
        settleBriefly(f);
        Check.equal("a:play,a:playing,a:pause", js(f, "events()"), "play and pause events");
        Check.equal("true", js(f, "String(a.paused)"), "paused");
        js(f, "log.length = 0; a.currentTime = 1.25;");
        Check.equal("1.25,true", js(f, "[a.currentTime, a.seeking].join(',')"), "the new position is visible and seeking is true");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("a:seeking,a:seeked", js(f, "events()"), "seek events");
        Check.equal("1.25", js(f, "String(a.currentTime)"), "the position after the seek");
        js(f, "a.currentTime = 100;");
        Check.equal("2", js(f, "String(a.currentTime)"), "a position past the end moves to the end");
        until(f, "a.seeking === false", "the seek to the end ended");
        Check.equal("true", js(f, "String(a.ended)"), "ended after seeking to the end");
        js(f, "a.currentTime = -5;");
        Check.equal("0", js(f, "String(a.currentTime)"), "a negative position moves to the start");
        Check.equal("TypeError", js(f, "try { a.currentTime = NaN; 'none'; } catch (e) { e.name; }"), "a non-finite position is a TypeError");
    }

    /**
     * Resource selection with <code>source</code> children: a candidate whose <code>type</code> is not supported or whose <code>media</code>
     * query does not match fails with an <code>error</code> event at the <code>source</code> and the next one is tried; only the usable one is
     * fetched; a candidate whose fetch fails is followed by the next; the element itself gets no <code>error</code> as long as a candidate
     * works.
     * <p>
     * <code>source</code> 자식의 리소스 선택: <code>type</code> 을 지원하지 않거나 <code>media</code> 질의가 맞지 않는 후보는 <code>source</code> 에
     * <code>error</code> 이벤트와 함께 실패하고 다음 후보를 시도한다. 쓸 수 있는 후보만 가져온다. 가져오기가 실패한 후보 다음에는 다음 후보가 온다.
     * 후보가 하나라도 되는 동안 요소 자신은 <code>error</code> 를 받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourceSelection() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/dir/p.html", PRELUDE + "<audio id=a><source id=s1 src='a.ogv' type='video/ogg'>"
                + "<source id=s2 src='b.bin' type='audio/x-unknown'><source id=s3 src='c.wav' media='(min-width: 99999px)'>"
                + "<source id=s4 src='' type='audio/wav'><source id=s5 src='bad.wav' type='audio/wav'><source id=s6 src='good.wav' type='audio/wav; codecs=1'></audio>");
        settleBriefly(f);
        Check.equal(1, media.count(), "only the first usable candidate is fetched");
        Check.equal("http://example.test/dir/bad.wav", media.requests.get(0).url, "the fifth source");
        media.fail(0);
        until(f, "log.indexOf('s5:error') >= 0", "the failed fetch is reported at its source");
        settleBriefly(f);
        Check.equal(2, media.count(), "the next candidate is fetched");
        Check.equal("http://example.test/dir/good.wav", media.requests.get(1).url, "the sixth source");
        media.ok(1, wav(2000), "audio/wav");
        until(f, "events().indexOf('canplaythrough') >= 0", "loaded");
        Check.equal("a:loadstart,s1:error,s2:error,s3:error,s4:error,s5:error,a:durationchange,a:loadedmetadata,a:loadeddata,a:progress,a:suspend,a:canplay,"
                + "a:canplaythrough", js(f, "events()"), "events: errors at the sources, none at the element");
        Check.equal("http://example.test/dir/good.wav,,1,4", js(f, "var a = document.getElementById('a'); [a.currentSrc, a.error, a.networkState, a.readyState].join(',')"),
                "the state");
    }

    /**
     * A candidate without a <code>type</code> whose bytes no decoder accepts fails after it was fetched, and the next candidate is used:
     * the selection looks at what the resource really is, not only at what the page says.
     * <p>
     * <code>type</code> 이 없고 어떤 디코더도 바이트를 받아들이지 않는 후보는 가져온 뒤에 실패하며 다음 후보를 쓴다: 선택은 페이지가 말하는 것만이 아니라
     * 리소스가 실제로 무엇인지도 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUndecodableCandidateFallsBackToTheNextSource() throws Exception {
        FakeMedia media = new FakeMedia();
        PageFixture f = fixture(media, new FakeOutputs());
        f.start("http://example.test/dir/p.html", PRELUDE + "<audio id=a><source id=s1 src='x.dat'><source id=s2 src='y.wav'></audio>");
        settleBriefly(f);
        media.ok(0, "this is not audio data at all, but the server claims it is".getBytes("US-ASCII"), "audio/mpeg");
        until(f, "log.indexOf('s1:error') >= 0", "the first candidate failed after the fetch");
        settleBriefly(f);
        Check.equal(2, media.count(), "the second candidate was requested");
        media.ok(1, wav(800), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "the second candidate loaded");
        Check.equal("http://example.test/dir/y.wav,", js(f, "var a = document.getElementById('a'); [a.currentSrc, a.error ? a.error.code : ''].join(',')"), "state");
        Check.equal("a:loadstart,s1:error,a:durationchange,a:loadedmetadata,a:loadeddata,a:progress,a:suspend,a:canplay,a:canplaythrough",
                js(f, "events()"), "events");
        Check.equal("[http://example.test/dir/x.dat=ok]", String.valueOf(f.env.mediaStates),
                "the failed candidate is not reported as failed while another candidate plays; the state is kept for the address the renderer shows");
    }

    /**
     * When every candidate fails the element waits (<code>NETWORK_NO_SOURCE</code>), stops delaying the load event, and goes on when a new
     * <code>source</code> is inserted.
     * <p>
     * 모든 후보가 실패하면 요소는 기다리고(<code>NETWORK_NO_SOURCE</code>), load 이벤트를 그만 늦추며, 새 <code>source</code> 가 삽입되면 이어 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWaitingForSource() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/dir/p.html", PRELUDE + "<audio id=a><source id=s1 src='a.ogv' type='video/ogg'></audio>");
        settleBriefly(f);
        Check.equal(0, media.count(), "nothing to fetch");
        Check.equal("complete", f.scripts.getReadyState(), "the element stopped delaying the load event");
        Check.equal("3,0", js(f, "var a = document.getElementById('a'); [a.networkState, a.readyState].join(',')"), "no source");
        Check.equal("[http://example.test/dir/a.ogv=no usable source]", String.valueOf(f.env.mediaStates), "the renderer is told that nothing can be played");
        js(f, "var s = document.createElement('source'); s.id = 's2'; s.src = 'ok.wav'; a.appendChild(s);");
        settleBriefly(f);
        Check.equal(1, media.count(), "the new source is tried");
        media.ok(0, wav(800), "audio/wav");
        until(f, "a.readyState === 4", "the element went on");
        Check.equal("http://example.test/dir/ok.wav", js(f, "a.currentSrc"), "currentSrc");
    }

    /**
     * A failing <code>src</code>: an <code>error</code> event, <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code>, <code>NETWORK_NO_SOURCE</code>; the
     * window <code>load</code> event is not blocked for ever; <code>play()</code> then rejects with <code>NotSupportedError</code>. The same
     * for bytes that no decoder accepts.
     * <p>
     * 실패하는 <code>src</code>: <code>error</code> 이벤트, <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code>, <code>NETWORK_NO_SOURCE</code>. window
     * <code>load</code> 이벤트는 영원히 막히지 않는다. 그 뒤 <code>play()</code> 는 <code>NotSupportedError</code> 로 거부한다. 어떤 디코더도 받아들이지
     * 않는 바이트도 마찬가지이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailingSource() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='gone.wav'></audio><audio id=b src='junk.wav'></audio>");
        settleBriefly(f);
        media.fail(0);
        media.ok(1, "this is not an audio file at all".getBytes("US-ASCII"), "audio/wav");
        until(f, "document.readyState === 'complete'", "the failures do not block the load event");
        until(f, "events().indexOf('b:error') >= 0", "the second element failed");
        Check.equal("a:loadstart,b:loadstart,a:error,b:error", js(f, "events()"), "events");
        Check.equal("4,3,0,4", js(f, "var a = document.getElementById('a'); var b = document.getElementById('b'); [a.error.code, a.networkState, a.readyState, b.error.code].join(',')"),
                "error state");
        Check.equal("true", js(f, "a.error instanceof MediaError && MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED === 4"), "MediaError");
        js(f, "var rejected = []; a.play().catch(function (e) { rejected.push(e.name); });");
        until(f, "rejected.length === 1", "play() was rejected");
        Check.equal("NotSupportedError", js(f, "rejected[0]"), "the rejection");
        Check.equal(2, f.env.mediaStates.size(), "the renderer is told about both failures");
        Check.contains(f.env.mediaStates.get(0), "http://example.test/gone.wav=404", "the failed fetch");
        Check.contains(f.env.mediaStates.get(1), "http://example.test/junk.wav=", "the unsupported data");
    }

    /**
     * <code>canPlayType()</code> answers from the decoders of the component: the empty string for what cannot be rendered (also for
     * <code>application/octet-stream</code>, a missing type and all <code>video</code> elements), <code>maybe</code> and
     * <code>probably</code> for the supported types.
     * <p>
     * <code>canPlayType()</code> 은 컴포넌트의 디코더가 답한다: 렌더링할 수 없는 것(<code>application/octet-stream</code>, 없는 타입, 모든
     * <code>video</code> 요소 포함)은 빈 문자열, 지원하는 타입은 <code>maybe</code> 와 <code>probably</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCanPlayType() throws Exception {
        PageFixture f = fixture(new FakeMedia(), new FakeOutputs());
        f.start("http://example.test/p.html", "<audio id=a></audio><video id=v></video>");
        Check.equal("maybe", js(f, "document.getElementById('a').canPlayType('audio/wav')"), "supported type");
        Check.equal("probably", js(f, "document.getElementById('a').canPlayType('audio/wav; codecs=\"1\"')"), "with a known codec");
        Check.equal("", js(f, "document.getElementById('a').canPlayType('audio/wav; codecs=\"vorbis\"')"), "with an unknown codec");
        Check.equal("", js(f, "document.getElementById('a').canPlayType('video/mp4')"), "video type");
        Check.equal("", js(f, "document.getElementById('a').canPlayType('')"), "empty");
        Check.equal("", js(f, "document.getElementById('a').canPlayType('application/octet-stream')"), "octet stream");
        Check.equal("", js(f, "document.getElementById('a').canPlayType()"), "no argument");
        Check.equal("", js(f, "document.getElementById('v').canPlayType('audio/wav')"), "a video element answers nothing for audio");
        Check.equal("", js(f, "document.getElementById('v').canPlayType('video/mp4; codecs=\"avc1.42E01E, mp4a.40.2\"')"), "nor for video");
    }

    /**
     * A <code>video</code> element is an explicit "unsupported": it has the members of the interface, never fetches anything, its resource
     * selection ends with <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> and an <code>error</code> event, <code>play()</code> rejects with a
     * <code>NotSupportedError</code>, and <code>videoWidth</code> and <code>videoHeight</code> are 0.
     * <p>
     * <code>video</code> 요소는 명시적인 "미지원"이다: 인터페이스의 멤버를 가지고, 아무것도 가져오지 않으며, 리소스 선택은
     * <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 와 <code>error</code> 이벤트로 끝나고, <code>play()</code> 는 <code>NotSupportedError</code> 로 거부하며,
     * <code>videoWidth</code> 와 <code>videoHeight</code> 는 0 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVideoIsExplicitlyUnsupported() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<video id=v src='m.mp4' width=320 height=240></video>"
                + "<video id=w><source id=ws src='n.webm' type='video/webm'></video><video id=x></video>");
        settleBriefly(f);
        Check.equal(0, media.count(), "a video is never fetched");
        Check.equal("v:loadstart,v:error,w:loadstart,ws:error,w:error", js(f, "events()"), "events: loadstart then error, per element");
        Check.equal("true,4,3,0,0,0", js(f, "var v = document.getElementById('v'); [v instanceof HTMLVideoElement && v instanceof HTMLMediaElement, v.error.code, v.networkState,"
                + " v.readyState, v.videoWidth, v.videoHeight].join(',')"), "state of a video");
        Check.equal("true,320,240", js(f, "[v.paused, v.width, v.height].join(',')"), "attributes");
        js(f, "var out = []; v.play().catch(function (e) { out.push(e.name); });"
                + " var x = document.getElementById('x'); x.play().catch(function (e) { out.push('x:' + e.name); }); x.load();");
        until(f, "out.length === 2", "both play() calls were rejected");
        Check.equal("NotSupportedError,x:NotSupportedError", js(f, "out.join(',')"), "the rejections");
        Check.equal("4", js(f, "String(x.error.code)"), "a video without any source reports the unsupported result too");
        Check.equal("", js(f, "v.canPlayType('video/mp4')"), "canPlayType");
        Check.equal(0, media.count(), "still nothing fetched");
        Check.equal("true", js(f, "String(v.paused)"), "stays paused");
    }

    /**
     * The volume and the muted flag: defaults, the <code>volumechange</code> event only for a real change, the range check, the WebIDL
     * conversion, <code>defaultMuted</code> reflecting the content attribute, the playback rate that is accepted only at 1.0.
     * <p>
     * 음량과 음소거: 기본값, 실제로 바뀔 때만 <code>volumechange</code> 이벤트, 범위 확인, WebIDL 변환, 콘텐츠 속성을 반영하는
     * <code>defaultMuted</code>, 1.0 에서만 받아들이는 재생 속도.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVolumeMutedAndRate() throws Exception {
        PageFixture f = fixture(new FakeMedia(), new FakeOutputs());
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a muted loop controls preload=auto></audio><audio id=b></audio>");
        Check.equal("1,true,true,true,true", js(f, "var a = document.getElementById('a'); [a.volume, a.muted, a.defaultMuted, a.loop, a.controls].join(',')"),
                "defaults of an element with attributes");
        Check.equal("1,false,false,false,auto", js(f, "var b = document.getElementById('b'); [b.volume, b.muted, b.defaultMuted, b.loop, b.preload || 'auto'].join(',')"),
                "defaults of a bare element");
        js(f, "b.volume = 0.5; b.volume = 0.5; b.muted = true; b.muted = true; b.muted = false;");
        settleBriefly(f);
        Check.equal("b:volumechange,b:volumechange,b:volumechange", js(f, "events()"), "one event per change (0.5, muted, unmuted)");
        Check.equal("0.5,false", js(f, "[b.volume, b.muted].join(',')"), "values");
        Check.equal("IndexSizeError", js(f, "try { b.volume = 1.5; 'none'; } catch (e) { e.name; }"), "range");
        Check.equal("IndexSizeError", js(f, "try { b.volume = -0.1; 'none'; } catch (e) { e.name; }"), "range");
        Check.equal("TypeError", js(f, "try { b.volume = NaN; 'none'; } catch (e) { e.name; }"), "NaN");
        Check.equal("0.5", js(f, "String(b.volume)"), "the volume did not change");
        js(f, "b.defaultMuted = true;");
        Check.equal("true,", js(f, "[b.hasAttribute('muted'), b.muted ? 'x' : ''].join(',')"), "defaultMuted sets the attribute, the current state stays");
        Check.equal("1,1", js(f, "[b.playbackRate, b.defaultPlaybackRate].join(',')"), "rates");
        js(f, "b.playbackRate = 1;");
        Check.equal("NotSupportedError", js(f, "try { b.playbackRate = 8; 'none'; } catch (e) { e.name; }"), "unsupported rate");
        Check.equal("NotSupportedError", js(f, "try { b.defaultPlaybackRate = 0.1; 'none'; } catch (e) { e.name; }"), "unsupported default rate");
        Check.equal("NotSupportedError", js(f, "try { b.playbackRate = 0; 'none'; } catch (e) { e.name; }"), "zero is not supported");
        Check.equal("NotSupportedError", js(f, "try { b.playbackRate = -1; 'none'; } catch (e) { e.name; }"), "backwards is not supported");
        Check.equal("TypeError", js(f, "try { b.playbackRate = NaN; 'none'; } catch (e) { e.name; }"), "NaN is a TypeError");
        Check.equal("1,1", js(f, "[b.playbackRate, b.defaultPlaybackRate].join(',')"), "refused rates change nothing");
        Check.equal("0,1,2,3,0,1,2,3,4", js(f, "[HTMLMediaElement.NETWORK_EMPTY, HTMLMediaElement.NETWORK_IDLE, HTMLMediaElement.NETWORK_LOADING,"
                + " HTMLMediaElement.NETWORK_NO_SOURCE, HTMLMediaElement.HAVE_NOTHING, HTMLMediaElement.HAVE_METADATA, HTMLMediaElement.HAVE_CURRENT_DATA,"
                + " HTMLMediaElement.HAVE_FUTURE_DATA, HTMLMediaElement.HAVE_ENOUGH_DATA].join(',')"), "constants");
    }

    /**
     * The time ranges: a loaded media is buffered and seekable as a whole, nothing played yet, and <code>start()</code> and <code>end()</code>
     * of a missing range are an <code>IndexSizeError</code>. Without a resource all ranges are empty.
     * <p>
     * 시간 범위: 로드된 미디어는 통째로 buffered 이고 seekable 이며 아직 재생된 것은 없고, 없는 범위의 <code>start()</code>, <code>end()</code> 는
     * <code>IndexSizeError</code> 이다. 리소스가 없으면 모든 범위가 비어 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTimeRanges() throws Exception {
        FakeMedia media = new FakeMedia();
        PageFixture f = fixture(media, new FakeOutputs());
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio><audio id=b></audio>");
        settleBriefly(f);
        Check.equal("0,0,0", js(f, "var a = document.getElementById('a'); [a.buffered.length, a.seekable.length, a.played.length].join(',')"), "before loading");
        media.ok(0, wav(4000), "audio/wav");
        until(f, "a.readyState === 4", "loaded");
        Check.equal("1,1,0,0,0.5,0.5", js(f, "[a.buffered.length, a.seekable.length, a.played.length, a.buffered.start(0), a.buffered.end(0), a.seekable.end(0)].join(',')"),
                "ranges after loading");
        Check.equal("IndexSizeError", js(f, "try { a.buffered.start(1); 'none'; } catch (e) { e.name; }"), "index out of range");
        Check.equal("true", js(f, "a.buffered instanceof TimeRanges && document.getElementById('b').buffered.length === 0"), "TimeRanges and an empty element");
    }

    /**
     * <code>new Audio(url)</code> creates an <code>audio</code> element with <code>preload</code> <code>auto</code> that starts to load at
     * once, also when it is not in the document, and plays to the end without any reference to it.
     * <p>
     * <code>new Audio(url)</code> 는 <code>preload</code> 가 <code>auto</code> 인 <code>audio</code> 요소를 만들며, 문서에 없어도 곧바로 로드를 시작하고,
     * 참조가 없어도 끝까지 재생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNewAudio() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/dir/p.html", "<script>var done = false; var a = new Audio('fx.wav');"
                + " a.addEventListener('ended', function () { done = true; }); a.play(); a = null;</script>");
        settleBriefly(f);
        Check.equal(1, media.count(), "an Audio object loads at once");
        Check.equal("http://example.test/dir/fx.wav", media.requests.get(0).url, "the URL");
        Check.equal("auto,true,1", js(f, "var b = new Audio(); [b.preload, b instanceof HTMLAudioElement, Audio.length === 0 ? 1 : 0].join(',')"), "constructor");
        Check.equal("TypeError", js(f, "try { Audio('x'); 'none'; } catch (e) { e.name; }"), "new is required");
        media.ok(0, wav(2000), "audio/wav");
        until(f, "done === true", "the unreferenced element played to the end");
        Check.isTrue(java.util.Arrays.equals(ScriptedDecoder.pcm(0, 2000), outs.outputs.get(0).written()), "the sound");
    }

    /**
     * <code>preload="none"</code> defers the fetch until the element is played: no request, <code>suspend</code>, no delay of the load event;
     * <code>play()</code> then starts the request.
     * <p>
     * <code>preload="none"</code> 은 요소가 재생될 때까지 가져오기를 미룬다: 요청 없음, <code>suspend</code>, load 이벤트 지연 없음. 그 뒤
     * <code>play()</code> 가 요청을 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPreloadNone() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav' preload=none></audio>");
        settleBriefly(f);
        Check.equal(0, media.count(), "nothing is fetched");
        Check.equal("a:loadstart,a:suspend", js(f, "events()"), "loadstart and suspend");
        Check.equal("complete", f.scripts.getReadyState(), "the load event is not delayed");
        Check.equal("1,0", js(f, "var a = document.getElementById('a'); [a.networkState, a.readyState].join(',')"), "idle");
        js(f, "a.play();");
        settleBriefly(f);
        Check.equal(1, media.count(), "play() starts the fetch");
        media.ok(0, wav(1000), "audio/wav");
        until(f, "events().indexOf('ended') >= 0", "played");
    }

    /**
     * A new load removes the old one: the fetch is cancelled, <code>abort</code> and <code>emptied</code> are queued, the state is reset, a
     * late result of the old fetch is ignored and the new fetch decides. Changing <code>src</code> starts the load algorithm, and
     * <code>load()</code> resets a playing element (paused, position 0).
     * <p>
     * 새 로드는 이전 로드를 없앤다: 가져오기를 취소하고, <code>abort</code> 와 <code>emptied</code> 를 올리고, 상태를 초기화하며, 이전 가져오기의 늦은
     * 결과는 무시하고 새 가져오기가 정한다. <code>src</code> 를 바꾸면 로드 알고리즘이 시작되고 <code>load()</code> 는 재생 중인 요소를
     * 초기화한다(일시정지, 위치 0).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNewLoadCancelsTheOldOne() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(1000);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/dir/p.html", PRELUDE + "<audio id=a src='one.wav'></audio>");
        settleBriefly(f);
        js(f, "var a = document.getElementById('a'); log.length = 0; a.src = 'two.wav';");
        settleBriefly(f);
        Check.isTrue(media.requests.get(0).cancelled, "the old fetch was cancelled");
        Check.equal(2, media.count(), "the new fetch started");
        Check.equal("http://example.test/dir/two.wav", media.requests.get(1).url, "the new URL");
        Check.equal("a:abort,a:emptied,a:loadstart", js(f, "events()"), "the events of the new load");
        media.ok(0, wav(100), "audio/wav");
        settleBriefly(f);
        Check.equal("a:abort,a:emptied,a:loadstart", js(f, "events()"), "the late result of the old fetch is ignored");
        media.ok(1, wav(16000), "audio/wav");
        until(f, "a.readyState === 4", "the new resource loaded");
        js(f, "a.play();");
        until(f, "!a.paused && a.currentTime >= 0 && events().indexOf('playing') >= 0", "playing");
        FakeOutputs.Out output = outs.await(0);
        js(f, "log.length = 0; a.load();");
        settleBriefly(f);
        Check.equal("true,0,0,NaN", js(f, "[a.paused, a.currentTime, a.readyState, a.duration].join(',')"), "load() resets the element");
        Check.equal("a:abort,a:emptied,a:loadstart", js(f, "events()"), "events of load() on a loaded element");
        Check.equal(3, media.count(), "a third fetch");
        Check.equal(1, output.closeCalls(), "the output of the old playback was closed");
    }

    /**
     * An element that is removed from the document is paused (a <code>pause</code> event), but an element that was never in the document
     * (<code>new Audio()</code>) is not affected by that rule.
     * <p>
     * 문서에서 제거된 요소는 일시정지된다(<code>pause</code> 이벤트). 문서에 있었던 적이 없는 요소(<code>new Audio()</code>)에는 그 규칙이 적용되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRemovalPauses() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(500);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<div id=d><audio id=a src='s.wav'></audio></div>");
        settleBriefly(f);
        media.ok(0, wav(16000), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "loaded");
        js(f, "var a = document.getElementById('a'); a.play();");
        until(f, "events().indexOf('playing') >= 0", "playing");
        js(f, "log.length = 0; a.addEventListener('pause', function () { log.push('a:pause'); }); var d = document.getElementById('d'); d.removeChild(a);");
        until(f, "events().indexOf('a:pause') >= 0", "the removed element was paused");
        Check.equal("a:pause", js(f, "events()"), "the pause event");
        Check.equal("true", js(f, "String(a.paused)"), "paused");
        js(f, "log.length = 0; a.play(); d.appendChild(a); d.removeChild(a); d.appendChild(a);");
        settleBriefly(f);
        Check.equal("false", js(f, "String(a.paused)"), "an element that is in the document again at the stable state keeps playing");
    }

    /**
     * Disposing the page closes every player (the output is closed, the thread ended), cancels the fetches, and later results change nothing.
     * <p>
     * 페이지를 폐기하면 모든 재생기가 닫히고(출력이 닫히고 스레드가 끝남) 가져오기가 취소되며, 나중의 결과는 아무것도 바꾸지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeClosesPlayers() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(200);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav' autoplay></audio><audio id=b src='t.wav'></audio>");
        settleBriefly(f);
        media.ok(0, wav(16000), "audio/wav");
        until(f, "events().indexOf('a:playing') >= 0", "playing");
        FakeOutputs.Out o = outs.await(0);
        o.awaitQueued(200);
        f.scripts.dispose();
        Check.equal(1, o.closeCalls(), "the output was closed by the disposal");
        Check.isTrue(media.requests.get(1).cancelled, "the pending fetch was cancelled");
        media.ok(1, wav(100), "audio/wav");
        settleBriefly(f);
        Check.equal(1, outs.outputs.size(), "no output was opened for the late result");
    }

    /**
     * A page without media services: a resource fails like an unsupported one. A page with a media service but without a sound output can
     * load and seek; playing fails as the fatal decoding error of HTML5 4.7.10.5: an <code>error</code> event (<code>MEDIA_ERR_DECODE</code>
     * and the reason in the message), <code>NETWORK_IDLE</code>, the element is not paused and no promise is rejected (the promise of
     * <code>play()</code> was fulfilled with the <code>playing</code> event); the dead resource is not tried again by a later <code>play()</code>,
     * and <code>pause()</code> still works.
     * <p>
     * 미디어 서비스가 없는 페이지: 리소스는 지원하지 않는 것처럼 실패한다. 미디어 서비스는 있고 소리 출력이 없는 페이지는 로드하고 탐색할 수 있으며
     * 재생은 HTML5 4.7.10.5 의 치명적 디코딩 오류로 실패한다: <code>error</code> 이벤트(<code>MEDIA_ERR_DECODE</code>, 메시지에 이유),
     * <code>NETWORK_IDLE</code>, 요소는 일시정지되지 않고 프라미스는 거부되지 않는다(<code>play()</code> 의 프라미스는 <code>playing</code> 이벤트와 함께
     * 이행되었다). 죽은 리소스는 뒤의 <code>play()</code> 가 다시 시도하지 않으며 <code>pause()</code> 는 여전히 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPagesWithoutServices() throws Exception {
        PageFixture none = fixture(null, null);
        none.load("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(none);
        Check.equal("a:loadstart,a:error", js(none, "events()"), "no media service");
        Check.equal("4,3", js(none, "var a = document.getElementById('a'); [a.error.code, a.networkState].join(',')"), "unsupported");
        FakeMedia media = new FakeMedia();
        PageFixture silent = fixture(media, null);
        silent.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(silent);
        media.ok(0, wav(4000), "audio/wav");
        until(silent, "document.getElementById('a').readyState === 4", "loaded without a sound output");
        js(silent, "var a = document.getElementById('a'); a.currentTime = 0.25; var out = []; log.length = 0;"
                + " a.play().then(function () { out.push('resolved'); }, function (e) { out.push(e.name); });");
        until(silent, "out.length === 1 && a.error !== null", "the playing task settled the promise and the failure came");
        Check.equal("resolved", js(silent, "out[0]"), "the promise follows the playing event");
        Check.equal("3,false,1", js(silent, "[a.error.code, a.paused, a.networkState].join(',')"), "decode error, not paused, idle");
        Check.contains(js(silent, "a.error.message"), "audio output", "the reason is in the message");
        String ev = js(silent, "events()");
        Check.isTrue(ev.indexOf("a:error") >= 0 && ev.indexOf("a:pause") < 0, "error and no pause event: " + ev);
        js(silent, "a.play().then(function () { out.push('again'); }); a.pause(); a.play();");
        settleBriefly(silent);
        Check.equal("1", js(silent, "String(log.filter(function (x) { return x === 'a:error'; }).length)"), "the dead resource is not tried again");
        Check.contains(js(silent, "events()"), "a:pause,a:play", "pause() and play() still change the state");
    }

    /**
     * The loop attribute: the player starts again at the end without an <code>ended</code> event, and <code>seeking</code>,
     * <code>seeked</code> are queued for each wrap-around; <code>ended</code> stays false.
     * <p>
     * loop 속성: 재생기는 끝에서 <code>ended</code> 이벤트 없이 다시 시작하며 되돌아갈 때마다 <code>seeking</code>, <code>seeked</code> 가 올라간다.
     * <code>ended</code> 는 false 로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoopAttribute() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs(1000);
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav' loop></audio>");
        settleBriefly(f);
        media.ok(0, wav(2500), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "loaded");
        js(f, "var a = document.getElementById('a'); log.length = 0; a.play();");
        FakeOutputs.Out o = outs.await(0);
        long end = System.currentTimeMillis() + 15000;
        // 두 번 되돌아갈 때까지 출력을 재생하며 기다린다.
        while (o.written().length < 2500 * 2 * 3 && System.currentTimeMillis() < end) {
            o.consume(400);
            f.advance(0);
            Thread.sleep(1);
        }
        until(f, "log.filter(function (x) { return x === 'a:seeked'; }).length >= 2", "the loop wrapped around twice");
        Check.equal("false", js(f, "String(a.ended)"), "a looping element does not end");
        Check.equal(0, Integer.parseInt(js(f, "log.filter(function (x) { return x === 'a:ended'; }).length")), "no ended event");
        js(f, "a.loop = false; a.pause();");
        Check.equal("true", js(f, "String(a.paused)"), "paused");
    }

    /**
     * The events of the media element carry the usual event properties: they do not bubble, cannot be cancelled and are fired at the element
     * (a <code>source</code> for its error), and the handler attributes (<code>onended</code> style) work for the media events.
     * <p>
     * 미디어 요소의 이벤트는 보통의 이벤트 속성을 가진다: 버블링하지 않고, 취소할 수 없고, 요소에 발생하며(<code>source</code> 의 오류는 그 요소),
     * 핸들러 속성(<code>onended</code> 등)이 미디어 이벤트에 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEventProperties() throws Exception {
        FakeMedia media = new FakeMedia();
        PageFixture f = fixture(media, new FakeOutputs());
        f.start("http://example.test/p.html", "<script>var seen = [];</script><audio id=a src='s.wav' onloadedmetadata=\"seen.push('attr:' + event.type + ':' + event.bubbles + ':' + event.cancelable + ':' + (event.target === this))\""
                + " onerror=\"seen.push('error')\"></audio><div id=d onloadedmetadata=\"seen.push('bubbled')\"></div>");
        settleBriefly(f);
        media.ok(0, wav(100), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "loaded");
        Check.equal("attr:loadedmetadata:false:false:true", js(f, "seen.join(',')"), "the handler attribute sees a simple event at the element");
    }

    /**
     * The transfer of a resource is visible to the page: <code>progress</code> fires when bytes arrived (a burst of reports is one event, and
     * a report that carries no bytes is none), <code>stalled</code> fires once when no data comes for three seconds and starts again after
     * the next progress, and nothing fires after the resource arrived.
     * <p>
     * 리소스의 전송이 페이지에 보인다: 바이트가 도착하면 <code>progress</code> 가 발생하고(연속 보고는 이벤트 하나, 바이트가 없는 보고는 없음), 3초 동안
     * 데이터가 오지 않으면 <code>stalled</code> 가 한 번 발생하며 다음 진행 뒤에 다시 시작되고, 리소스가 도착한 뒤에는 아무것도 발생하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testProgressAndStalledEvents() throws Exception {
        FakeMedia media = new FakeMedia();
        PageFixture f = fixture(media, new FakeOutputs());
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(f);
        Check.equal(1, media.count(), "the resource was requested");
        js(f, "var a = document.getElementById('a'); log.length = 0;");
        MediaRequestSource.ProgressCallback transfer = (MediaRequestSource.ProgressCallback) media.requests.get(0).callback;
        transfer.progress(0, 5000);
        settleBriefly(f);
        Check.equal("", js(f, "events()"), "a report without bytes is no progress");
        transfer.progress(1000, 5000);
        transfer.progress(2000, 5000);
        settleBriefly(f);
        Check.equal("a:progress", js(f, "events()"), "a burst of reports is one progress event");
        transfer.progress(3000, 5000);
        settleBriefly(f);
        Check.equal("a:progress,a:progress", js(f, "events()"), "the next burst is another event");
        f.advance(2800);
        settleBriefly(f);
        Check.equal("a:progress,a:progress", js(f, "events()"), "no stalled event before three seconds");
        f.advance(400);
        settleBriefly(f);
        Check.equal("a:progress,a:progress,a:stalled", js(f, "events()"), "stalled after three seconds without data");
        f.advance(20000);
        settleBriefly(f);
        Check.equal("a:progress,a:progress,a:stalled", js(f, "events()"), "stalled fires once per stall");
        transfer.progress(4000, 5000);
        settleBriefly(f);
        f.advance(3100);
        settleBriefly(f);
        Check.equal("a:progress,a:progress,a:stalled,a:progress,a:stalled", js(f, "events()"), "progress starts the timer again");
        media.ok(0, wav(500), "audio/wav");
        until(f, "events().indexOf('canplaythrough') >= 0", "the resource loaded");
        js(f, "log.length = 0;");
        f.advance(60000);
        settleBriefly(f);
        Check.equal("", js(f, "events()"), "nothing fires after the resource arrived");
    }

    /**
     * <code>preload="metadata"</code>: the element stops at <code>HAVE_METADATA</code> (the network is idle, <code>suspend</code> fires, there
     * is no <code>loadeddata</code>, <code>canplay</code> or <code>canplaythrough</code>, no sound output is opened, and the window
     * <code>load</code> event does not wait), seeking works there, and <code>play()</code> raises the state to <code>HAVE_ENOUGH_DATA</code> with
     * the events that belong to it before the sound plays to the end. With <code>autoplay</code> the element goes all the way at once.
     * <p>
     * <code>preload="metadata"</code>: 요소는 <code>HAVE_METADATA</code> 에서 멈춘다(네트워크는 놀고, <code>suspend</code> 가 발생하며,
     * <code>loadeddata</code>, <code>canplay</code>, <code>canplaythrough</code> 가 없고, 소리 출력을 열지 않으며, window <code>load</code> 이벤트는
     * 기다리지 않는다). 거기서도 탐색이 되고, <code>play()</code> 는 상태를 <code>HAVE_ENOUGH_DATA</code> 로 올리며 그에 속한 이벤트를 낸 뒤 소리가 끝까지
     * 재생된다. <code>autoplay</code> 가 있으면 요소는 곧바로 끝까지 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPreloadMetadataStopsAtMetadataUntilPlayed() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav' preload=metadata></audio><audio id=b src='t.wav' preload=metadata autoplay></audio>");
        settleBriefly(f);
        media.ok(0, wav(4000), "audio/wav");
        media.ok(1, wav(1000), "audio/wav");
        until(f, "document.getElementById('a').readyState === 1 && document.getElementById('b').readyState === 4", "metadata known, autoplay element loaded");
        until(f, "document.readyState === 'complete'", "the window load event does not wait for the data of a metadata-only element");
        settleBriefly(f);
        js(f, "var a = document.getElementById('a'); var b = document.getElementById('b');");
        Check.equal("1,1,0.5", js(f, "[a.readyState, a.networkState, a.duration].join(',')"), "ready state, network idle, duration");
        Check.equal("a:loadstart,a:durationchange,a:loadedmetadata,a:progress,a:suspend",
                js(f, "log.filter(function (x) { return x.indexOf('a:') === 0; }).filter(function (x) { return x.indexOf('timeupdate') < 0; }).join(',')"),
                "the events of a metadata-only element");
        Check.isTrue(js(f, "log.indexOf('b:canplaythrough') >= 0 && log.indexOf('b:play') >= 0").equals("true"), "autoplay goes all the way");
        until(f, "log.indexOf('b:ended') >= 0", "the autoplay element ended");
        js(f, "log.length = 0; a.currentTime = 0.25;");
        until(f, "a.seeking === false", "seeking works at HAVE_METADATA");
        Check.equal("1", js(f, "String(a.readyState)"), "the ready state stays at the metadata");
        int opened = outs.outputs.size();
        js(f, "log.length = 0; a.play();");
        until(f, "log.indexOf('a:ended') >= 0", "the element played to the end");
        Check.equal("a:play,a:waiting,a:loadeddata,a:canplay,a:playing,a:canplaythrough,a:pause,a:ended", js(f, "events()"),
                "play() raises the state with the events that belong to it");
        Check.equal(opened + 1, outs.outputs.size(), "the output was opened by the play request, not before");
        Check.equal("4,true,true", js(f, "[a.readyState, a.paused, a.ended].join(',')"), "state at the end");
    }

    /**
     * <code>playbackRate</code>, <code>defaultPlaybackRate</code> and <code>preservesPitch</code>: one <code>ratechange</code> per change
     * of a rate (none for an equal value or for the pitch flag), the rate reaches the sound (twice the speed gives about half the frames),
     * and a new load starts with the default rate again.
     * <p>
     * <code>playbackRate</code>, <code>defaultPlaybackRate</code>, <code>preservesPitch</code>: 속도가 바뀔 때마다 <code>ratechange</code> 하나(같은
     * 값이나 음높이 플래그는 없음), 속도가 소리에 도달하고(두 배 속도는 대략 절반의 프레임), 새 로드는 다시 기본 속도로 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlaybackRateEventsAndSound() throws Exception {
        FakeMedia media = new FakeMedia();
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = fixture(media, outs);
        f.start("http://example.test/p.html", PRELUDE + "<audio id=a src='s.wav'></audio>");
        settleBriefly(f);
        media.ok(0, wav(16000), "audio/wav");
        until(f, "document.getElementById('a').readyState === 4", "loaded");
        js(f, "var a = document.getElementById('a'); log.length = 0; a.playbackRate = 2; a.playbackRate = 2; a.defaultPlaybackRate = 1.5;"
                + " a.defaultPlaybackRate = 1.5; a.preservesPitch = false; a.preservesPitch = true;");
        settleBriefly(f);
        Check.equal("a:ratechange,a:ratechange", js(f, "events()"), "one ratechange per change of a rate");
        Check.equal("2,1.5,true", js(f, "[a.playbackRate, a.defaultPlaybackRate, a.preservesPitch].join(',')"), "the values");
        js(f, "a.play();");
        until(f, "a.ended === true", "the end of the media at twice the speed");
        int frames = outs.outputs.get(0).written().length / 2;
        Check.isTrue(Math.abs(frames - 8000) <= 200, "about half of the 16000 frames reached the output: " + frames);
        Check.equal("2", js(f, "String(a.currentTime)"), "the position at the end is the duration");
        js(f, "log.length = 0; a.load();");
        settleBriefly(f);
        Check.equal("1.5", js(f, "String(a.playbackRate)"), "a new load starts with the default rate");
        Check.isTrue(js(f, "events()").indexOf("a:ratechange") >= 0, "the reset to the default is announced");
    }
}
