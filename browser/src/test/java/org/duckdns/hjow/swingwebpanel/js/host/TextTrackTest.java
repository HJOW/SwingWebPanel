package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.playback.ScriptedDecoder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the text tracks of the media elements (plan item P20-13; FEATURE_MATRIX VID-02, HEI-08): the interfaces and constructors, the
 * <code>VTTCue</code> and <code>VTTRegion</code> objects, <code>addTextTrack()</code> and the list order, the cue list and its order, the fetch of the
 * WebVTT file of a <code>track</code> element through a fake request service (the request mode, success, the failures, the change of
 * <code>src</code>), the automatic selection of a <code>default</code> track, and the time marches on steps with their events: <code>enter</code>,
 * <code>exit</code> and <code>cuechange</code> after seeks and during playback, the pause-on-exit flag, the reset by a new load, the track
 * list events, and the disposal. The sound goes to a fake output ({@link FakeOutputs}); nothing is heard.
 * <p>
 * 미디어 요소의 텍스트 트랙 시험(계획 P20-13, FEATURE_MATRIX VID-02, HEI-08): 인터페이스와 생성자, <code>VTTCue</code> 와 <code>VTTRegion</code> 객체,
 * <code>addTextTrack()</code> 과 목록 순서, cue 목록과 그 순서, 가짜 요청 서비스를 거친 <code>track</code> 요소의 WebVTT 파일 가져오기(요청 모드, 성공,
 * 실패, <code>src</code> 변경), <code>default</code> 트랙의 자동 선택, time marches on 단계와 그 이벤트(탐색과 재생 중의 <code>enter</code>,
 * <code>exit</code>, <code>cuechange</code>), pause-on-exit 표시, 새 로드에 의한 초기화, 트랙 목록 이벤트, 폐기. 소리는 가짜 출력({@link FakeOutputs})으로 가며
 * 아무 소리도 들리지 않는다.
 */
public class TextTrackTest {

    /**
     * Creates the suite (required by the runner) and registers the scripted decoder.
     * <p>
     * 스위트를 만들고(실행기가 필요로 함) 스크립트 디코더를 등록한다.
     */
    public TextTrackTest() {
        ScriptedDecoder.install();
    }

    /**
     * A fake request service for the files of the tracks: it records the requests and completes them when the test says so.
     * <p>
     * 트랙 파일을 위한 가짜 요청 서비스: 요청을 기록하고 시험이 지시할 때 완료시킨다.
     */
    private static final class FakeWeb implements WebRequestSource {
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
        final class Req implements WebRequestSource.Call {
            /**
             * The request.
             * <p>
             * 요청.
             */
            final WebRequest request;

            /**
             * The receiver.
             * <p>
             * 받는 쪽.
             */
            final WebRequestSource.Callback callback;

            /**
             * True after the page cancelled the request.
             * <p>
             * 페이지가 요청을 취소했으면 true.
             */
            volatile boolean cancelled;

            /**
             * Creates the record.
             * <p>
             * 기록을 만든다.
             *
             * @param request  the request / 요청
             * @param callback the receiver / 받는 쪽
             */
            Req(WebRequest request, WebRequestSource.Callback callback) {
                this.request = request;
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
         * @param request  the request / 요청
         * @param callback the receiver / 받는 쪽
         * @return the handle / 핸들
         */
        public WebRequestSource.Call start(WebRequest request, WebRequestSource.Callback callback) {
            Req r = new Req(request, callback);
            requests.add(r);
            return r;
        }

        /**
         * Completes a request with a body.
         * <p>
         * 요청을 본문으로 완료시킨다.
         *
         * @param index  the request number / 요청 번호
         * @param status the status / 상태
         * @param body   the body / 본문
         */
        void respond(int index, int status, String body) {
            WebResponse r = new WebResponse(WebResponse.TYPE_BASIC);
            r.setStatus(status, status == 200 ? "OK" : "Not OK");
            try {
                r.setBody(body.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            requests.get(index).callback.onComplete(r);
        }

        /**
         * Completes a request with a network failure.
         * <p>
         * 요청을 네트워크 실패로 완료시킨다.
         *
         * @param index the request number / 요청 번호
         */
        void networkError(int index) {
            requests.get(index).callback.onComplete(WebResponse.error("network down", false));
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

        /**
         * Returns the URL of a request.
         * <p>
         * 요청의 URL 을 반환한다.
         *
         * @param index the request number / 요청 번호
         * @return the URL / URL
         */
        String url(int index) {
            return requests.get(index).request.getUrl();
        }
    }

    /**
     * A fake media service for the sound resources, completed by hand.
     * <p>
     * 소리 리소스를 위한 가짜 미디어 서비스. 손으로 완료시킨다.
     */
    private static final class FakeMedia implements MediaRequestSource {
        /**
         * The receivers of the requests.
         * <p>
         * 요청의 받는 쪽.
         */
        final List<Callback> callbacks = Collections.synchronizedList(new ArrayList<Callback>());

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
            callbacks.add(callback);
            return new Request() {
                /**
                 * Ignores the cancellation.
                 * <p>
                 * 취소를 무시한다.
                 */
                public void cancel() {
                    // 시험은 취소를 확인하지 않는다.
                }
            };
        }
    }

    /**
     * The script that logs the events of a media element and its tracks, and the states in the page.
     * <p>
     * 미디어 요소와 그 트랙의 이벤트를 기록하는 스크립트와 페이지의 상태.
     */
    private static final String PRELUDE = "<script>var log = []; function L(s) { log.push(s); }"
            + "function watch(c, name) { c.addEventListener('enter', function () { L(name + ':enter'); }); c.addEventListener('exit', function () { L(name + ':exit'); }); }"
            + "function out() { var s = log.join(','); log.length = 0; return s; }</script>";

    /**
     * Makes a mono WAV file of the scripted ramp (8000 frames are one second).
     * <p>
     * 스크립트 램프의 모노 WAV 파일을 만든다(8000 프레임이 1초).
     *
     * @param frames the number of frames at 8 kHz / 8 kHz 의 프레임 수
     * @return the file / 파일
     */
    private static byte[] wav(int frames) {
        return AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, frames), -1);
    }

    /**
     * Creates a fixture with the fake services.
     * <p>
     * 가짜 서비스가 있는 시험 도구를 만든다.
     *
     * @param web     the request service of the page / 페이지의 요청 서비스
     * @param media   the media service / 미디어 서비스
     * @param outputs the sound outputs / 소리 출력
     * @return the fixture / 시험 도구
     */
    private static PageFixture fixture(FakeWeb web, FakeMedia media, FakeOutputs outputs) {
        PageFixture f = new PageFixture();
        f.env.web = web;
        f.env.media = media;
        f.env.outputs = outputs;
        return f;
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
     * Runs the queued tasks until an expression is true.
     * <p>
     * 식이 참이 될 때까지 올라온 작업을 실행한다.
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
        throw new AssertionError(message + " (waiting for: " + expr + "; log: " + f.scripts.evaluate("log.join(',')", "t") + ")");
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
     * Loads a page with a loaded <code>audio</code> element (<code>a</code>, 10 seconds) and a sound output that only plays when the test asks.
     * <p>
     * 로드된 <code>audio</code> 요소(<code>a</code>, 10초)와 시험이 요청할 때만 재생되는 소리 출력이 있는 페이지를 로드한다.
     *
     * @param web     the request service / 요청 서비스
     * @param outs    the outputs / 출력
     * @param content the children of the element / 요소의 자식
     * @param script  the script after the element / 요소 뒤의 스크립트
     * @return the fixture / 시험 도구
     * @throws Exception on failure / 실패 시
     */
    private static PageFixture loadedAudio(FakeWeb web, FakeOutputs outs, String content, String script) throws Exception {
        FakeMedia media = new FakeMedia();
        PageFixture f = fixture(web, media, outs);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a src='s.wav'>" + content + "</audio><script>" + script + "</script>");
        settleBriefly(f);
        media.callbacks.get(0).done(wav(80000), "audio/wav", null);
        until(f, "document.getElementById('a').readyState === 4", "the audio is loaded");
        return f;
    }

    /**
     * Plays the element of a fixture until its position reaches a time: the test lets the fake output render a few frames at a time.
     * <p>
     * 시험 도구의 요소를 위치가 어떤 시각에 닿을 때까지 재생한다: 시험이 가짜 출력이 몇 프레임씩 재생하게 한다.
     *
     * @param f       the fixture / 시험 도구
     * @param outs    the outputs / 출력
     * @param seconds the position to reach / 닿을 위치
     * @throws Exception on failure / 실패 시
     */
    private static void playUntil(PageFixture f, FakeOutputs outs, double seconds) throws Exception {
        FakeOutputs.Out o = outs.await(0);
        long end = System.currentTimeMillis() + 30000;
        // 위치가 목표에 닿거나 요소가 멈출 때까지 출력을 조금씩 재생한다.
        while (System.currentTimeMillis() < end) {
            o.consume(200);
            // 200 프레임은 25 ms 이므로 수동 시계도 25 ms 가게 하여 타이머가 소리의 위치와 함께 움직이게 한다.
            f.advance(25);
            double pos = Double.parseDouble(js(f, "String(a.currentTime)"));
            if (pos >= seconds || "true".equals(js(f, "String(a.paused)"))) {
                return;
            }
            Thread.sleep(1);
        }
        throw new AssertionError("the position did not reach " + seconds + ": " + js(f, "String(a.currentTime)"));
    }

    /**
     * The interface objects, the constructors that exist, the ones that throw, and the event object.
     * <p>
     * 인터페이스 객체, 있는 생성자, 던지는 생성자, 이벤트 객체.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInterfaces() throws Exception {
        PageFixture f = fixture(null, null, null);
        f.load("<audio id=a><track id=t></audio>");
        Check.equal("function,function,function,function,function,function,function", js(f, "[typeof TextTrack, typeof TextTrackList, typeof TextTrackCue, "
                + "typeof TextTrackCueList, typeof VTTCue, typeof VTTRegion, typeof TrackEvent].join(',')"), "the interface objects");
        Check.equal("true,true,[object VTTCue],[object TextTrack],[object TextTrackList],[object TextTrackCueList]", js(f, "var c = new VTTCue(1, 2, 'x'); var a = document.getElementById('a'); "
                + "a.textTracks.length; var tr = a.addTextTrack('metadata'); [c instanceof TextTrackCue, c instanceof EventTarget, Object.prototype.toString.call(c), "
                + "Object.prototype.toString.call(tr), Object.prototype.toString.call(a.textTracks), Object.prototype.toString.call(tr.cues)].join(',')"), "tags and chains");
        Check.equal("TypeError:Illegal constructor,TypeError:Illegal constructor,TypeError:Illegal constructor,TypeError:Illegal constructor", js(f,
                "[TextTrack, TextTrackList, TextTrackCue, TextTrackCueList].map(function (C) { try { new C(); return 'none'; } catch (e) { return e.name + ':' + e.message; } }).join(',')"),
                "interfaces that a script cannot construct");
        Check.equal("true,addtrack,true,true,TypeError", js(f, "var track2 = document.getElementById('a').addTextTrack('captions'); var te = new TrackEvent('addtrack', { track: track2 }); "
                + "var bad; try { new TrackEvent('x', { track: {} }); bad = 'none'; } catch (e) { bad = e.name; } "
                + "[te instanceof Event, te.type, te.track === track2, te.bubbles === false && te.cancelable === false, bad].join(',')"), "TrackEvent");
        Check.equal("0,1,2,3", js(f, "[HTMLTrackElement.NONE, HTMLTrackElement.LOADING, HTMLTrackElement.LOADED, HTMLTrackElement.ERROR].join(',')"), "the constants");
        Check.equal("subtitles,captions,x,metadata", js(f, "var tk = document.getElementById('t'); var k0 = tk.kind; tk.setAttribute('kind', 'CAPTIONS'); var k1 = tk.kind; tk.kind = 'x'; "
                + "[k0, k1, tk.getAttribute('kind'), tk.kind].join(',')"), "HTMLTrackElement.kind reads the keyword of the enumerated attribute and writes the attribute");
        Check.equal("true,true,true", js(f, "var el = document.getElementById('t'); var h = [typeof el.onload, typeof el.oncuechange, 'oncuechange' in el]; "
                + "[h[0] === 'object', h[1] === 'object', h[2]].join(',')"), "event handler attributes of the element");
    }

    /**
     * <code>VTTCue</code>: the constructor and its errors, the defaults, the setters with their errors and the enumerations that ignore a wrong value,
     * <code>region</code>, <code>line</code> and <code>position</code> with <code>auto</code>, and <code>getCueAsHTML()</code>.
     * <p>
     * <code>VTTCue</code>: 생성자와 그 오류, 기본값, 오류가 있는 setter 와 틀린 값을 무시하는 열거형, <code>region</code>, <code>auto</code> 가 있는
     * <code>line</code>, <code>position</code>, <code>getCueAsHTML()</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVttCue() throws Exception {
        PageFixture f = fixture(null, null, null);
        f.load("<p></p>");
        Check.equal(",1,2,false,null,,true,auto,start,auto,auto,100,center,x", js(f, "var c = new VTTCue(1, 2, 'x'); [c.id, c.startTime, c.endTime, c.pauseOnExit, String(c.region), "
                + "c.vertical, c.snapToLines, c.line, c.lineAlign, c.position, c.positionAlign, c.size, c.align, c.text].join(',')"), "the defaults");
        Check.equal("TypeError,TypeError,TypeError,TypeError,Infinity", js(f, "function e(fn) { try { fn(); return 'none'; } catch (x) { return x.name; } } "
                + "[e(function () { new VTTCue(1); }), e(function () { new VTTCue(NaN, 2, ''); }), e(function () { new VTTCue(0, NaN, ''); }), "
                + "e(function () { new VTTCue(0, -Infinity, ''); }), new VTTCue(0, Infinity, '').endTime].join(',')"), "constructor errors and an unbounded cue");
        Check.equal("TypeError", js(f, "e(function () { VTTCue(0, 1, ''); })"), "calling without new");
        js(f, "c.id = 'i1'; c.startTime = 3; c.endTime = 4.5; c.pauseOnExit = true; c.vertical = 'lr'; c.snapToLines = false; c.line = 50; c.lineAlign = 'end'; "
                + "c.position = 10; c.positionAlign = 'line-right'; c.size = 40; c.align = 'left'; c.text = 'y';");
        Check.equal("i1,3,4.5,true,lr,false,50,end,10,line-right,40,left,y", js(f, "[c.id, c.startTime, c.endTime, c.pauseOnExit, c.vertical, c.snapToLines, c.line, c.lineAlign, "
                + "c.position, c.positionAlign, c.size, c.align, c.text].join(',')"), "the setters");
        js(f, "c.vertical = 'bogus'; c.lineAlign = 'x'; c.positionAlign = 'y'; c.align = 'z';");
        Check.equal("lr,end,line-right,left", js(f, "[c.vertical, c.lineAlign, c.positionAlign, c.align].join(',')"), "a wrong keyword is ignored");
        Check.equal("auto,auto", js(f, "c.line = 'auto'; c.position = 'auto'; [c.line, c.position].join(',')"), "auto");
        Check.equal("IndexSizeError,IndexSizeError,IndexSizeError,TypeError,TypeError,TypeError,TypeError", js(f, "[e(function () { c.size = 101; }), e(function () { c.position = -1; }), "
                + "e(function () { c.position = 100.5; }), e(function () { c.line = NaN; }), e(function () { c.startTime = Infinity; }), e(function () { c.endTime = NaN; }), "
                + "e(function () { c.endTime = -Infinity; })].join(',')"), "setter errors");
        Check.equal("100,true", js(f, "c.size = 100; c.endTime = Infinity; [c.size, c.endTime === Infinity].join(',')"), "boundary values");
        Check.equal("true,true,null,TypeError", js(f, "var r = new VTTRegion(); c.region = r; var same = c.region === r; var again = c.region === r; c.region = null; var none = String(c.region); "
                + "var bad = e(function () { c.region = {}; }); [same, again, none, bad].join(',')"), "region");
        Check.equal(",100,3,0,100,0,100,", js(f, "var rg = new VTTRegion(); [rg.id, rg.width, rg.lines, rg.regionAnchorX, rg.regionAnchorY, rg.viewportAnchorX, rg.viewportAnchorY, rg.scroll].join(',')"),
                "the defaults of a region");
        Check.equal("r1,50,7,10,20,30,40,up|IndexSizeError,IndexSizeError,|", js(f, "rg.id = 'r1'; rg.width = 50; rg.lines = 7; rg.regionAnchorX = 10; rg.regionAnchorY = 20; "
                + "rg.viewportAnchorX = 30; rg.viewportAnchorY = 40; rg.scroll = 'up'; var a1 = [rg.id, rg.width, rg.lines, rg.regionAnchorX, rg.regionAnchorY, rg.viewportAnchorX, "
                + "rg.viewportAnchorY, rg.scroll].join(','); var a2 = [e(function () { rg.width = 101; }), e(function () { rg.viewportAnchorY = -1; }), (rg.scroll = 'bogus', rg.scroll === 'up' ? '' : 'x')].join(','); "
                + "rg.scroll = ''; a1 + '|' + a2 + '|' + rg.scroll"), "the setters of a region");
        Check.equal("1:SPAN.red big|3: & |1:B.|7:timestamp=00:00:01.500|3:c|1:SPAN.|1:SPAN.", js(f, "var fr = new VTTCue(0, 1, '<c.red.big>a</c> &amp; <b>b</b><00:01.500>c<v Bob>v</v><lang en>l</lang>').getCueAsHTML(); "
                + "Array.prototype.map.call(fr.childNodes, function (n) { return n.nodeType + ':' + (n.nodeType === 1 ? n.tagName + '.' + n.className : "
                + "n.nodeType === 7 ? n.target + '=' + n.data : n.data); }).join('|')"), "getCueAsHTML");
        Check.equal("Bob,en", js(f, "[fr.childNodes[5].getAttribute('title'), fr.childNodes[6].getAttribute('lang')].join(',')"), "the voice and the language");
    }

    /**
     * <code>addTextTrack()</code> and the text track list: the order of the list (elements first, then the tracks of the script), the live list, the
     * events <code>addtrack</code> and <code>removetrack</code>, <code>getTrackById()</code>, and the properties of a new track.
     * <p>
     * <code>addTextTrack()</code> 과 텍스트 트랙 목록: 목록의 순서(요소가 먼저, 스크립트의 트랙이 뒤), 살아 있는 목록, <code>addtrack</code>,
     * <code>removetrack</code> 이벤트, <code>getTrackById()</code>, 새 트랙의 속성.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAddTextTrackAndTheList() throws Exception {
        PageFixture f = fixture(new FakeWeb(), null, null);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a><track id=t1 kind=captions label=One srclang=en><track id=t2></audio>"
                + "<script>var a = document.getElementById('a'); var list = a.textTracks; list.addEventListener('addtrack', function (e) { L('add:' + (e.track === list[list.length - 1]) + ':' + e.track.kind); });"
                + "list.onremovetrack = function (e) { L('remove:' + e.track.id); };</script>");
        settleBriefly(f);
        Check.equal("2,true,true,true", js(f, "[list.length, list === a.textTracks, list[0] === document.getElementById('t1').track, list[1] === document.getElementById('t2').track].join(',')"),
                "the track elements in tree order");
        Check.equal("captions,One,en,t1,disabled", js(f, "var t1 = list[0]; [t1.kind, t1.label, t1.language, t1.id, t1.mode].join(',')"), "an element's track");
        js(f, "out(); var s = a.addTextTrack('subtitles', 'Own', 'fr'); var d = a.addTextTrack('metadata');");
        Check.equal("4,true,true,subtitles,Own,fr,,hidden,0,0", js(f, "[list.length, list[2] === s, list[3] === d, s.kind, s.label, s.language, s.id, s.mode, s.cues.length, s.activeCues.length].join(',')"),
                "tracks of the script");
        Check.equal("TypeError,TypeError", js(f, "function e(fn) { try { fn(); return 'none'; } catch (x) { return x.name; } } [e(function () { a.addTextTrack('bogus'); }), e(function () { a.addTextTrack(); })].join(',')"),
                "a kind that is not one is an error");
        settleBriefly(f);
        Check.equal("add:false:subtitles,add:true:metadata", js(f, "out()"), "addtrack for each track of the script (the events come after the script)");
        js(f, "var el = document.createElement('track'); el.id = 't3'; a.insertBefore(el, a.firstChild);");
        settleBriefly(f);
        Check.equal("5,true,t3", js(f, "[list.length, list[0] === el.track, list[0].id].join(',')"), "a new track element comes first, before the scripted tracks");
        Check.equal("add:false:subtitles", js(f, "out()"), "addtrack for the element (it is first, not last)");
        Check.equal("true,true,true", js(f, "[list.getTrackById('t1') === t1, list.getTrackById('t3') === el.track, list.getTrackById('missing') === null].join(',')"),
                "getTrackById");
        js(f, "a.removeChild(document.getElementById('t2'));");
        settleBriefly(f);
        Check.equal("4,remove:t2", js(f, "[list.length, out()].join(',')"), "a removed element leaves the list");
        Check.equal("true", js(f, "String(document.getElementById('t2') === null)"), "removed");
        Check.equal("true", js(f, "String(list[0].mode === 'disabled' && s.mode === 'hidden')"), "modes");
    }

    /**
     * The cue list: the text track cue order (start, then the later end first, then the order of insertion), <code>getCueById</code>, adding a cue to
     * another track moves it, <code>removeCue</code> with its error, a disabled track has no cue lists, and a change of a time reorders the cues.
     * <p>
     * cue 목록: 텍스트 트랙 cue 순서(시작, 끝이 늦은 것 먼저, 들어온 순서), <code>getCueById</code>, 다른 트랙에 cue 를 더하면 옮겨짐, 오류가 있는
     * <code>removeCue</code>, 꺼진 트랙에는 cue 목록이 없음, 시각을 바꾸면 순서가 바뀜.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCueList() throws Exception {
        PageFixture f = fixture(null, null, null);
        f.load(PRELUDE + "<audio id=a></audio>");
        js(f, "var a = document.getElementById('a'); var tr = a.addTextTrack('metadata'); var tr2 = a.addTextTrack('metadata'); "
                + "function mk(id, s, e) { var c = new VTTCue(s, e, id); c.id = id; return c; } "
                + "var c1 = mk('a', 5, 6), c2 = mk('b', 1, 2), c3 = mk('c', 1, 4), c4 = mk('d', 1, 2); tr.addCue(c1); tr.addCue(c2); tr.addCue(c3); tr.addCue(c4); "
                + "function ids(t) { return Array.prototype.map.call(t.cues, function (c) { return c.id; }).join(''); } "
                + "function e(fn) { try { fn(); return 'none'; } catch (x) { return x.name; } }");
        Check.equal("cbda", js(f, "ids(tr)"), "start, later end first, order of insertion");
        Check.equal("4,true,true,null,true", js(f, "[tr.cues.length, tr.cues[0] === c3, c1.track === tr, String(tr.cues.getCueById('')), tr.cues.getCueById('b') === c2].join(',')"),
                "the list, the track of a cue and getCueById");
        Check.equal("true", js(f, "String(tr.cues === tr.cues)"), "the same list object every time");
        js(f, "tr.removeCue(c2);");
        Check.equal("cda,null,NotFoundError", js(f, "[ids(tr), String(c2.track), e(function () { tr.removeCue(c2); })].join(',')"), "removeCue");
        Check.equal("TypeError,TypeError", js(f, "[e(function () { tr.addCue({}); }), e(function () { tr.removeCue('x'); })].join(',')"), "not a cue");
        js(f, "tr2.addCue(c1);");
        Check.equal("cd,a,true", js(f, "[ids(tr), ids(tr2), c1.track === tr2].join(',')"), "a cue that is added to another track leaves the first one");
        js(f, "tr2.addCue(c1);");
        Check.equal("a,1", js(f, "[ids(tr2), tr2.cues.length].join(',')"), "adding a cue again leaves one");
        js(f, "c4.startTime = 0;");
        Check.equal("dc", js(f, "ids(tr)"), "a change of the start time reorders the cues");
        js(f, "c3.startTime = 0; c3.endTime = 0.5;");
        Check.equal("dc", js(f, "ids(tr)"), "same start: the later end first");
        js(f, "tr.mode = 'disabled';");
        Check.equal("null,null,1", js(f, "[String(tr.cues), String(tr.activeCues), tr.mode === 'disabled' ? 1 : 0].join(',')"), "a disabled track has no lists");
        js(f, "tr.mode = 'showing';");
        Check.equal("2,true", js(f, "[tr.cues.length, tr.cues === tr.cues].join(',')"), "and has them again, with the cues it had");
        js(f, "var ended = new VTTCue(-5, -1, 'n'); tr.addCue(ended);");
        Check.equal("2", js(f, "String(tr.cues.length)"), "a cue that ends before the earliest position is not listed");
    }

    /**
     * The fetch of the file of a <code>track</code> element: nothing is fetched while the mode is <code>disabled</code>; a mode that asks for it starts the
     * request in the same-origin fallback mode; the file's cues arrive, the state becomes loaded and <code>load</code> fires; a track is not fetched again
     * when its mode changes while the URL stays.
     * <p>
     * <code>track</code> 요소의 파일 가져오기: 모드가 <code>disabled</code> 인 동안은 아무것도 가져오지 않는다. 요구하는 모드가 되면 same-origin 대체 모드로 요청이
     * 시작되고, 파일의 cue 가 들어오며 상태가 loaded 가 되고 <code>load</code> 가 발생한다. URL 이 같은 동안 모드가 바뀌어도 다시 가져오지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrackFetch() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a><track id=t src='sub.vtt' kind=subtitles srclang=en label=English></audio>"
                + "<script>var t = document.getElementById('t'); ['load', 'error', 'cuechange'].forEach(function (n) { t.addEventListener(n, function () { L('t:' + n); }); });</script>");
        settleBriefly(f);
        Check.equal(0, web.count(), "a disabled track is not fetched");
        Check.equal("0,disabled,null", js(f, "[t.readyState, t.track.mode, String(t.track.cues)].join(',')"), "not loaded");
        js(f, "t.track.mode = 'hidden';");
        Check.equal(1, web.count(), "the mode asks for the file");
        Check.equal("http://example.test/dir/sub.vtt", web.url(0), "resolved against the document");
        Check.equal("same-origin", web.requests.get(0).request.getMode(), "the same-origin fallback of a track request");
        Check.equal("1,0", js(f, "[t.readyState, t.track.cues.length].join(',')"), "loading");
        web.respond(0, 200, "WEBVTT\n\n1\n00:01.000 --> 00:02.000\nHello\n\n2\n00:03.000 --> 00:04.000 line:2\n<b>World</b>\n");
        settleBriefly(f);
        Check.equal("2,2,Hello,<b>World</b>,2,true", js(f, "var cs = t.track.cues; [t.readyState, cs.length, cs[0].text, cs[1].text, cs[1].line, cs[0].track === t.track].join(',')"),
                "loaded with the cues of the file");
        Check.equal("t:load", js(f, "out()"), "load fires at the element");
        js(f, "t.track.mode = 'disabled'; t.track.mode = 'showing'; t.track.mode = 'hidden';");
        settleBriefly(f);
        Check.equal(1, web.count(), "no second request for the same URL");
        js(f, "var first = t.track.cues[0];");
        Check.equal("2", js(f, "String(t.readyState)"), "still loaded");
        // 요소의 속성은 트랙에 반영된다.
        js(f, "t.setAttribute('label', 'Eng'); t.setAttribute('srclang', 'fr'); t.id = 'tt';");
        Check.equal("subtitles,Eng,fr,tt", js(f, "[t.track.kind, t.track.label, t.track.language, t.track.id].join(',')"), "the track follows the attributes");
    }

    /**
     * The failures of the fetch: a status that is not a success, a network failure, a file without the signature and a missing address each end with the
     * state failed to load and <code>error</code> at the element, and a cue that arrived before is not added.
     * <p>
     * 가져오기의 실패: 성공이 아닌 상태, 네트워크 실패, 서명이 없는 파일, 없는 주소는 모두 failed to load 상태와 요소의 <code>error</code> 로 끝나며 그 전에
     * 도착한 cue 는 더해지지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrackFetchFailures() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a><track id=t1 src='a.vtt'><track id=t2 src='b.vtt'><track id=t3 src='c.vtt'><track id=t4></audio>"
                + "<script>var a = document.getElementById('a'); ['t1', 't2', 't3', 't4'].forEach(function (id) { var el = document.getElementById(id); "
                + "el.addEventListener('error', function () { L(id + ':error'); }); el.addEventListener('load', function () { L(id + ':load'); }); el.track.mode = 'hidden'; });</script>");
        settleBriefly(f);
        Check.equal(3, web.count(), "three requests (the track without an address needs none)");
        Check.equal("t4:error", js(f, "out()"), "no address: error at once");
        web.respond(0, 404, "WEBVTT\n\n00:01.000 --> 00:02.000\nnot used");
        web.networkError(1);
        web.respond(2, 200, "<html>not a vtt</html>");
        settleBriefly(f);
        Check.equal("t1:error,t2:error,t3:error", js(f, "out()"), "errors in the order of the results");
        Check.equal("3,3,3,3", js(f, "['t1', 't2', 't3', 't4'].map(function (id) { return document.getElementById(id).readyState; }).join(',')"), "failed to load");
        Check.equal("0", js(f, "String(document.getElementById('t1').track.cues.length)"), "no cue from a failed file");
        // 실패한 주소는 다시 가져오지 않지만 주소가 바뀌면 다시 시도한다.
        js(f, "document.getElementById('t1').track.mode = 'showing'; document.getElementById('t1').src = 'a2.vtt';");
        settleBriefly(f);
        Check.equal(4, web.count(), "a new address is fetched again");
        Check.equal("http://example.test/dir/a2.vtt", web.url(3), "the new address");
        web.respond(3, 200, "WEBVTT\n\n00:00.000 --> 00:01.000\nok");
        settleBriefly(f);
        Check.equal("t1:load", js(f, "out()"), "and it loads");
    }

    /**
     * The request mode follows the <code>crossorigin</code> attribute of the media element: without it the same-origin fallback, with it a CORS request
     * (and the credentials of <code>use-credentials</code>).
     * <p>
     * 요청 모드는 미디어 요소의 <code>crossorigin</code> 속성을 따른다: 없으면 same-origin 대체, 있으면 CORS 요청(<code>use-credentials</code> 는 자격 증명 포함).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrackRequestMode() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", "<audio id=a1><track src='http://other.test/a.vtt'></audio><audio id=a2 crossorigin><track src='http://other.test/b.vtt'></audio>"
                + "<audio id=a3 crossorigin=use-credentials><track src='http://other.test/c.vtt'></audio><audio id=a4 crossorigin=bogus><track src='http://other.test/d.vtt'></audio>"
                + "<script>Array.prototype.forEach.call(document.getElementsByTagName('track'), function (t) { t.track.mode = 'hidden'; });</script>");
        settleBriefly(f);
        Check.equal(4, web.count(), "four requests");
        Check.equal("same-origin/same-origin", web.requests.get(0).request.getMode() + "/" + web.requests.get(0).request.getCredentials(), "no crossorigin attribute");
        Check.equal("cors/same-origin", web.requests.get(1).request.getMode() + "/" + web.requests.get(1).request.getCredentials(), "anonymous");
        Check.equal("cors/include", web.requests.get(2).request.getMode() + "/" + web.requests.get(2).request.getCredentials(), "use-credentials");
        Check.equal("cors/same-origin", web.requests.get(3).request.getMode() + "/" + web.requests.get(3).request.getCredentials(), "an invalid value is anonymous");
    }

    /**
     * A change of <code>src</code>: the cues are emptied at once, a fetch that runs is aborted (with <code>error</code>) and the late result of the old
     * request is ignored, and the new file is fetched; a track element that leaves the media element aborts its fetch.
     * <p>
     * <code>src</code> 변경: cue 를 곧바로 비우고, 진행 중인 가져오기는 중단하며(<code>error</code> 와 함께) 이전 요청의 늦은 결과는 무시하고, 새 파일을 가져온다.
     * 미디어 요소를 떠난 track 요소는 가져오기를 중단한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourceChangeAndRemoval() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a><track id=t src='a.vtt' default></audio><audio id=b><track id=u src='u.vtt' default></audio>"
                + "<script>var t = document.getElementById('t'); ['load', 'error'].forEach(function (n) { t.addEventListener(n, function () { L('t:' + n); }); });</script>");
        settleBriefly(f);
        Check.equal(2, web.count(), "the default tracks are fetched");
        js(f, "t.src = 'b.vtt';");
        settleBriefly(f);
        Check.isTrue(web.requests.get(0).cancelled, "the old request was cancelled");
        Check.equal(3, web.count(), "the new file is fetched");
        Check.equal("t:error", js(f, "out()"), "the aborted fetch ends with error");
        web.respond(0, 200, "WEBVTT\n\n00:00.000 --> 00:01.000\nstale");
        settleBriefly(f);
        Check.equal("0,1", js(f, "[t.track.cues.length, t.readyState].join(',')"), "the late result of the old request is ignored");
        web.respond(2, 200, "WEBVTT\n\n00:00.000 --> 00:01.000\nnew");
        settleBriefly(f);
        Check.equal("1,new,2", js(f, "[t.track.cues.length, t.track.cues[0].text, t.readyState].join(',')"), "the new file");
        js(f, "t.src = 'c.vtt';");
        Check.equal("0", js(f, "String(t.track.cues.length)"), "the cues are emptied at once");
        settleBriefly(f);
        Check.equal(4, web.count(), "the new address is fetched");
        // 가져오는 중에 요소가 미디어 요소를 떠나면 요청이 취소된다.
        js(f, "document.getElementById('b').removeChild(document.getElementById('u'));");
        settleBriefly(f);
        Check.isTrue(web.requests.get(1).cancelled, "a removed element aborts the fetch");
        js(f, "var u = document.createElement('track'); u.src = 'v.vtt'; u.track.mode = 'hidden'; document.getElementById('b').appendChild(u);");
        settleBriefly(f);
        Check.equal(5, web.count(), "a track with a mode is fetched when it joins a media element");
        Check.equal("http://example.test/dir/v.vtt", web.url(4), "the address");
    }

    /**
     * The automatic selection: the first <code>default</code> track of subtitles and captions becomes <code>showing</code> and the others stay disabled;
     * a descriptions track with <code>default</code> is selected for its kind; a chapters or metadata track with <code>default</code> becomes
     * <code>hidden</code>; nothing is selected if a track of the kinds is already showing; the <code>change</code> event of the list fires once.
     * <p>
     * 자동 선택: 자막/캡션의 첫 <code>default</code> 트랙은 <code>showing</code> 이 되고 나머지는 꺼진 채로 남는다. <code>default</code> 가 있는 설명 트랙은 그
     * 종류에서 선택된다. <code>default</code> 가 있는 chapters/metadata 트랙은 <code>hidden</code> 이 된다. 해당 종류의 트랙이 이미 showing 이면 아무것도
     * 선택하지 않는다. 목록의 <code>change</code> 이벤트는 한 번 발생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAutomaticSelection() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", PRELUDE + "<audio id=a><track id=t1 src='1.vtt'><track id=t2 kind=captions default src='2.vtt'><track id=t3 kind=subtitles default src='3.vtt'>"
                + "<track id=t4 kind=descriptions default src='4.vtt'><track id=t5 kind=chapters default src='5.vtt'><track id=t6 kind=metadata src='6.vtt'><track id=t7 kind=metadata default src='7.vtt'></audio>"
                + "<script>var a = document.getElementById('a'); a.textTracks.onchange = function () { L('change'); };</script>"
                + "<audio id=b><track id=u1 default src='u1.vtt'></audio><script>var b = document.getElementById('b'); b.textTracks[0].mode = 'hidden'; b.textTracks.onchange = function () { L('b-change'); };</script>");
        settleBriefly(f);
        Check.equal("disabled,showing,disabled,showing,hidden,disabled,hidden", js(f, "['t1', 't2', 't3', 't4', 't5', 't6', 't7'].map(function (id) { return document.getElementById(id).track.mode; }).join(',')"),
                "the first default of subtitles and captions, the default descriptions, chapters and metadata tracks");
        Check.equal(5, web.count(), "the tracks with a mode other than disabled are fetched (t2, t4, t5, t7) and the one that the page changed (u1)");
        Check.equal("change", js(f, "out().split(',').filter(function (s) { return s === 'change'; }).join(',')"), "one change event for the list of a");
        Check.equal("hidden", js(f, "document.getElementById('u1').track.mode"), "a track that the script changed first is not selected again");
    }

    /**
     * The active cues follow the position after seeks: <code>enter</code> and <code>exit</code> in the order of their times, one <code>cuechange</code> for the
     * track, no events for cues that a seek skipped, no events while the track is disabled, and the flags are unset (without events) when the media is loaded
     * again.
     * <p>
     * 활성 cue 는 탐색 뒤의 위치를 따른다: 시각 순서의 <code>enter</code>, <code>exit</code>, 트랙에 대해 한 번의 <code>cuechange</code>, 탐색이 건너뛴 cue 는 이벤트가
     * 없고, 트랙이 꺼진 동안은 이벤트가 없으며, 미디어를 다시 로드하면 표시가 이벤트 없이 지워진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testActiveCuesAfterSeeks() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        PageFixture f = loadedAudio(null, outs, "", "var a = document.getElementById('a'); var tr = a.addTextTrack('subtitles'); "
                + "function mk(id, s, e) { var c = new VTTCue(s, e, id); c.id = id; watch(c, id); tr.addCue(c); return c; } var A = mk('A', 1, 3), B = mk('B', 2, 5), C = mk('C', 6, 7); "
                + "tr.oncuechange = function () { L('cuechange:' + Array.prototype.map.call(tr.activeCues, function (c) { return c.id; }).join('')); };"
                + "function ids() { return Array.prototype.map.call(tr.activeCues, function (c) { return c.id; }).join(''); }");
        settleBriefly(f);
        Check.equal("", js(f, "out() + ids()"), "nothing is active at 0");
        js(f, "a.currentTime = 2.5;");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("A:enter,B:enter,cuechange:AB|AB", js(f, "out() + '|' + ids()"), "a seek into two cues: the enter events by start time, one cuechange");
        js(f, "a.currentTime = 4;");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("A:exit,cuechange:B|B", js(f, "out() + '|' + ids()"), "A ended");
        js(f, "a.currentTime = 9;");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("B:exit,cuechange:|", js(f, "out() + '|' + ids()"), "B exits and C is skipped by the seek: no events for C");
        js(f, "a.currentTime = 6.5;");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("C:enter,cuechange:C|C", js(f, "out() + '|' + ids()"), "C is active");
        js(f, "tr.mode = 'disabled';");
        Check.equal("null,", js(f, "String(tr.activeCues) + ',' + out()"), "disabling clears the active flags at once without events");
        js(f, "a.currentTime = 1.5;");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("", js(f, "out()"), "no events while the track is disabled");
        js(f, "tr.mode = 'hidden';");
        settleBriefly(f);
        Check.equal("A:enter,cuechange:A|A", js(f, "out() + '|' + ids()"), "enabling it works the active cues out again");
        js(f, "a.load();");
        settleBriefly(f);
        Check.equal("0,", js(f, "tr.activeCues.length + ',' + out().replace(/emptied|abort/g, '')"), "a new load unsets the flags without exit events");
    }

    /**
     * Changing a cue: the end time of an active cue that moves before the position makes it exit; a new cue that is added while the position is inside it
     * enters (after the script that added it); removing an active cue gives no <code>exit</code> but the next run works the others out.
     * <p>
     * cue 변경: 활성 cue 의 끝 시각이 위치 앞으로 가면 exit 한다. 위치가 그 안에 있을 때 더한 새 cue 는 (더한 스크립트가 끝난 뒤) enter 한다. 활성 cue 를 빼도
     * <code>exit</code> 는 없지만 다음 실행이 나머지를 다시 계산한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChangingCues() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        PageFixture f = loadedAudio(null, outs, "", "var a = document.getElementById('a'); var tr = a.addTextTrack('metadata'); "
                + "function mk(id, s, e) { var c = new VTTCue(s, e, id); c.id = id; watch(c, id); tr.addCue(c); return c; } var A = mk('A', 1, 5); a.currentTime = 2; "
                + "function ids() { return Array.prototype.map.call(tr.activeCues, function (c) { return c.id; }).join(''); }");
        until(f, "a.seeking === false", "the seek ended");
        settleBriefly(f);
        Check.equal("A:enter|A", js(f, "out() + '|' + ids()"), "A is active");
        js(f, "A.endTime = 1.5;");
        Check.equal("A", js(f, "ids()"), "the active cues change after the script");
        settleBriefly(f);
        Check.equal("A:exit|", js(f, "out() + '|' + ids()"), "the end time moved before the position: exit");
        js(f, "var B = mk('B', 0, 10); var seen = ids();");
        Check.equal("", js(f, "seen"), "a cue that was just added is not active in the same script");
        settleBriefly(f);
        Check.equal("B:enter|B", js(f, "out() + '|' + ids()"), "it enters afterwards");
        js(f, "tr.removeCue(B);");
        settleBriefly(f);
        Check.equal("|", js(f, "out() + '|' + ids()"), "a removed cue gives no exit event");
        js(f, "var D = mk('D', 2, 3); D.startTime = 5;");
        settleBriefly(f);
        Check.equal("|", js(f, "out() + '|' + ids()"), "a cue that starts later is not active");
    }

    /**
     * During playback the events come in the order of their times: <code>enter</code> and <code>exit</code> of cues that overlap, one after the other and
     * not in the order of the cues, and the events of a cue that was shorter than the steps' interval are not lost (a missed cue enters and exits).
     * <p>
     * 재생 중에는 이벤트가 시각 순서로 온다: 겹치는 cue 의 <code>enter</code>, <code>exit</code> 는 cue 순서가 아니라 시각 순서이며, 단계의 간격보다 짧은 cue 의
     * 이벤트도 사라지지 않는다(놓친 cue 는 enter 와 exit).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEventsDuringPlayback() throws Exception {
        FakeOutputs outs = new FakeOutputs(800);
        PageFixture f = loadedAudio(null, outs, "", "var a = document.getElementById('a'); var tr = a.addTextTrack('subtitles'); "
                + "function mk(id, s, e) { var c = new VTTCue(s, e, id); c.id = id; watch(c, id); tr.addCue(c); return c; } mk('D', 0.5, 0.6); mk('E', 0.55, 0.9); mk('F', 1.1, 1.12);");
        settleBriefly(f);
        js(f, "a.play();");
        playUntil(f, outs, 1.4);
        settleBriefly(f);
        String log = js(f, "out().split(',').filter(function (s) { return s.indexOf('cuechange') < 0; }).join(',')");
        Check.equal("D:enter,E:enter,D:exit,E:exit,F:enter,F:exit", log, "events in the order of their times");
        Check.equal("", js(f, "Array.prototype.map.call(tr.activeCues, function (c) { return c.id; }).join('')"), "nothing is active at the end");
        js(f, "a.pause();");
        Check.equal("true", js(f, "String(a.paused)"), "paused again");
    }

    /**
     * The pause-on-exit flag: playback that leaves such a cue pauses the element; a seek past the cue does not.
     * <p>
     * pause-on-exit 표시: 그런 cue 를 재생이 벗어나면 요소가 일시정지된다. cue 를 넘는 탐색은 그렇지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseOnExit() throws Exception {
        FakeOutputs outs = new FakeOutputs(800);
        PageFixture f = loadedAudio(null, outs, "", "var a = document.getElementById('a'); var tr = a.addTextTrack('metadata'); var c = new VTTCue(1, 2, 'x'); c.pauseOnExit = true; tr.addCue(c);");
        settleBriefly(f);
        js(f, "a.currentTime = 3; ");
        until(f, "a.seeking === false", "the seek ended");
        js(f, "a.play();");
        settleBriefly(f);
        Check.equal("false", js(f, "String(a.paused)"), "a seek past the cue does not pause");
        js(f, "a.pause(); a.currentTime = 1.5;");
        until(f, "a.seeking === false", "the seek ended");
        js(f, "a.play();");
        playUntil(f, outs, 3);
        settleBriefly(f);
        Check.equal("true", js(f, "String(a.paused)"), "playback left the cue: the element is paused");
        double pos = Double.parseDouble(js(f, "String(a.currentTime)"));
        Check.isTrue(pos >= 2 && pos < 2.6, "the position is just past the end of the cue: " + pos);
    }

    /**
     * The events of the track list: <code>change</code> after a change of a mode (one event for several changes, before the events of the cues that the
     * change brings), and a track whose element leaves the media element: <code>removetrack</code>, and its cues stop being active without an
     * <code>exit</code> event.
     * <p>
     * 트랙 목록의 이벤트: 모드가 바뀐 뒤의 <code>change</code>(여러 변경에 이벤트 하나이며 변경이 가져오는 cue 이벤트보다 먼저), 요소가 미디어 요소를 떠난
     * 트랙: <code>removetrack</code>, 그 cue 는 <code>exit</code> 없이 활성이 아니게 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testListEventsAndRemoval() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        FakeWeb web = new FakeWeb();
        PageFixture f = loadedAudio(web, outs, "<track id=t src='x.vtt' default>", "var a = document.getElementById('a'); var t = document.getElementById('t'); "
                + "var tr = a.addTextTrack('metadata'); var c = new VTTCue(0, 5, 'x'); watch(c, 'c'); tr.addCue(c); a.currentTime = 1;");
        settleBriefly(f);
        Check.equal(1, web.count(), "the default track is fetched");
        web.respond(0, 200, "WEBVTT\n\nc1\n00:00.000 --> 00:05.000\nfrom the file");
        until(f, "t.readyState === 2 && a.seeking === false", "loaded and positioned");
        settleBriefly(f);
        js(f, "var fc = t.track.cues[0]; watch(fc, 'fc'); out();");
        js(f, "a.textTracks.onchange = function () { L('change'); }; a.textTracks.onremovetrack = function (e) { L('remove:' + e.track.id); }; tr.mode = 'disabled'; tr.mode = 'showing'; t.track.mode = 'hidden';");
        settleBriefly(f);
        Check.equal("change,c:enter", js(f, "out()"), "one change event for three changes, then the cue of tr enters again (the other track did not change its cues)");
        Check.equal("1,c1", js(f, "[t.track.activeCues.length, fc.id].join(',')"), "the cue of the file is active");
        js(f, "a.removeChild(t);");
        settleBriefly(f);
        Check.equal("remove:t", js(f, "out()"), "removetrack and no exit event");
        Check.equal("0,1,true", js(f, "[t.track.activeCues.length, a.textTracks.length, a.textTracks[0] === tr].join(',')"), "the cues of the removed track are not active and the list lost it");
        js(f, "a.appendChild(t);");
        settleBriefly(f);
        Check.equal("true,true", js(f, "[a.textTracks[0] === t.track, a.textTracks[1] === tr].join(',')"), "the track joins the list again at its place in the tree, before the scripted tracks");
    }

    /**
     * A <code>video</code> element has the structure of the text tracks (the list, <code>addTextTrack()</code>, the fetch of a <code>default</code>
     * track and its cues) but no cue becomes active, because a video never loads (EXC-01).
     * <p>
     * <code>video</code> 요소는 텍스트 트랙의 구조(목록, <code>addTextTrack()</code>, <code>default</code> 트랙의 가져오기와 cue)를 가지지만 비디오는 로드되지 않으므로(EXC-01)
     * 활성이 되는 cue 는 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVideoElementHasOnlyTheStructure() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", "<video id=v><track id=t src='v.vtt' default></video>");
        settleBriefly(f);
        Check.equal(1, web.count(), "the default track of the video is fetched");
        web.respond(0, 200, "WEBVTT" + (char) 10 + (char) 10 + "00:00.000 --> 00:10.000" + (char) 10 + "x");
        settleBriefly(f);
        Check.equal("1,2,showing,1,0", js(f, "var v = document.getElementById('v'); var m = v.addTextTrack('metadata'); [v.textTracks.length - 1, document.getElementById('t').readyState, "
                + "document.getElementById('t').track.mode, document.getElementById('t').track.cues.length, document.getElementById('t').track.activeCues.length].join(',')"),
                "the structure works and nothing is active");
        Check.equal("2,hidden", js(f, "[v.textTracks.length, m.mode].join(',')"), "a track of the script");
    }

    /**
     * Disposing the page cancels what runs: a fetch of a track is cancelled and a late result is ignored.
     * <p>
     * 페이지를 폐기하면 실행 중인 것이 취소된다: 트랙의 가져오기가 취소되고 늦은 결과는 무시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDispose() throws Exception {
        FakeWeb web = new FakeWeb();
        PageFixture f = fixture(web, null, null);
        f.start("http://example.test/dir/page.html", "<audio id=a><track id=t src='a.vtt' default></audio>");
        settleBriefly(f);
        Check.equal(1, web.count(), "the fetch started");
        f.scripts.dispose();
        Check.isTrue(web.requests.get(0).cancelled, "the fetch was cancelled by the disposal");
        web.respond(0, 200, "WEBVTT\n\n00:00.000 --> 00:01.000\nlate");
        f.advance(0);
        Check.isTrue(f.scripts.isDisposed(), "the page stays disposed");
    }
}
