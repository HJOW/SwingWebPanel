package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * An <code>audio</code> element played through a Web Audio graph (plan item P25-04, P25-07; FEATURE_MATRIX WAU-06c, WAU-12): <code>createMediaElementSource(element)</code> routes the sound of the element
 * into the graph of a real-time context, where a gain multiplies it and the output of the context plays it. The element is a real element of a page with a real player (its decoder reads a real WAV
 * file that a fake media service delivers), the output of the context is a recording fake that a simulated device plays at the pace of the wall clock, and the element has no output of its own
 * ({@link FakeOutputs} of the elements is never opened). Shown: the samples of the file arrive in the output exactly (16 bit sample divided by 32768, times the gain) and in order, with the graph as the
 * clock of the element; pause, seek, loop, volume and muting work through the graph; an element that plays to the speakers is switched to the graph in the middle of the playing without a gap; the errors of
 * <code>createMediaElementSource</code> (second connection, video, anything else, offline contexts, closed contexts); media of another origin is silent in the graph while the element goes on;
 * a closed context releases the element, which plays on silently; and nothing is left behind after many contexts or the page.
 * <p>
 * Web Audio 그래프를 거치는 <code>audio</code> 요소(계획 P25-04, P25-07; FEATURE_MATRIX WAU-06c, WAU-12): <code>createMediaElementSource(element)</code> 는 요소의 소리를 실시간 컨텍스트의 그래프로 돌리고,
 * 거기서 게인이 곱해져 컨텍스트의 출력이 재생한다. 요소는 진짜 재생기를 가진 페이지의 진짜 요소이고(디코더는 가짜 미디어 서비스가 전해 준 진짜 WAV 파일을 읽는다), 컨텍스트의 출력은 모의 장치가
 * 벽시계의 속도로 재생하는 기록용 가짜이며, 요소 자신의 출력은 없다(요소의 {@link FakeOutputs} 는 열리지 않는다). 보이는 것: 파일의 표본은 출력에 정확히(16비트 표본을 32768 로 나눈 값에 게인을 곱한
 * 값) 순서대로 도착하며 그래프가 요소의 시계이다. 일시정지, 탐색, 반복, 음량, 음소거가 그래프를 거쳐 동작한다. 스피커로 재생하던 요소는 재생 도중에 틈 없이 그래프로 바뀐다.
 * <code>createMediaElementSource</code> 의 오류(두 번째 연결, 비디오, 그 밖의 것, 오프라인 컨텍스트, 닫힌 컨텍스트). 다른 오리진의 미디어는 그래프에서 무음이지만 요소는 계속 진행한다. 닫힌
 * 컨텍스트는 요소를 놓으며 요소는 소리 없이 계속 재생된다. 많은 컨텍스트나 페이지 뒤에 아무것도 남지 않는다.
 */
public class MediaElementSourceScriptTest {

    /**
     * The sample rate of the media and of the context.
     * <p>
     * 미디어와 컨텍스트의 샘플링 주파수.
     */
    private static final int RATE = 48000;

    /**
     * A fake media service: it records the requests and completes them when the test says so.
     * <p>
     * 가짜 미디어 서비스: 요청을 기록하고 시험이 지시할 때 완료시킨다.
     */
    private static final class FakeMedia implements MediaRequestSource {

        /**
         * The callbacks of the requests, in order.
         * <p>
         * 요청의 콜백(순서대로).
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
                 * Does nothing: the tests do not cancel.
                 * <p>
                 * 아무것도 하지 않는다: 시험은 취소하지 않는다.
                 */
                public void cancel() {
                }
            };
        }

        /**
         * Completes a request successfully.
         * <p>
         * 요청을 성공으로 완료시킨다.
         *
         * @param index the number of the request / 요청 번호
         * @param data  the bytes / 바이트
         */
        void ok(int index, byte[] data) {
            callbacks.get(index).done(data, "audio/wav", null);
        }
    }

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private PageFixture f;

    /**
     * The media service.
     * <p>
     * 미디어 서비스.
     */
    private FakeMedia media;

    /**
     * The sound outputs of the elements (they must stay closed: the elements are connected to a graph).
     * <p>
     * 요소의 소리 출력(닫힌 채로 있어야 한다: 요소는 그래프에 연결된다).
     */
    private FakeOutputs speakers;

    /**
     * The recording outputs of the contexts.
     * <p>
     * 컨텍스트의 기록 출력.
     */
    private ManualOutputFactory outputs;

    /**
     * The simulated device.
     * <p>
     * 모의 장치.
     */
    private PacedDevice device;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaElementSourceScriptTest() {
    }

    /**
     * Starts a page with a fake media service, fake outputs for the elements, and recording outputs for the contexts played by a simulated device.
     * <p>
     * 가짜 미디어 서비스, 요소용 가짜 출력, 모의 장치가 재생하는 컨텍스트용 기록 출력으로 페이지를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        media = new FakeMedia();
        speakers = new FakeOutputs();
        outputs = new ManualOutputFactory();
        device = new PacedDevice(outputs);
        f = new PageFixture();
        f.env.media = media;
        f.env.outputs = speakers;
        f.env.webAudioOutputs = outputs;
        f.start("http://example.test/dir/page.html", "<!doctype html><body><audio id=tag></audio><video id=movie></video><div id=box></div></body>");
    }

    /**
     * Disposes the page and stops the device.
     * <p>
     * 페이지를 폐기하고 장치를 멈춘다.
     */
    public void tearDown() {
        f.scripts.dispose();
        device.close();
    }

    /**
     * Returns the sample of the test ramp (see {@code MediaElementTapTest}): a deterministic 16 bit value, zero at frame 10000 only.
     * <p>
     * 시험 램프의 표본을 반환한다({@code MediaElementTapTest} 참고): 결정적인 16비트 값. 프레임 10000 에서만 0 이다.
     *
     * @param frame the frame / 프레임
     * @return the sample / 표본
     */
    private static short ramp(long frame) {
        return (short) (((frame * 7) % 20000) - 10000);
    }

    /**
     * Makes the mono WAV file of the ramp.
     * <p>
     * 램프의 모노 WAV 파일을 만든다.
     *
     * @param rate   the sample rate / 샘플링 주파수
     * @param frames the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] rampWav(int rate, int frames) {
        byte[] data = new byte[frames * 2];
        // 프레임마다 램프의 표본을 리틀 엔디언으로 쓴다.
        for (int i = 0; i < frames; i++) {
            short s = ramp(i);
            data[i * 2] = (byte) s;
            data[i * 2 + 1] = (byte) (s >> 8);
        }
        return AudioKit.wav(1, 1, rate, 16, data, -1);
    }

    /**
     * Evaluates an expression in the page.
     * <p>
     * 페이지에서 식을 평가한다.
     *
     * @param expr the expression / 식
     * @return the result as text / 문자열 결과
     */
    private String js(String expr) {
        String r = f.scripts.evaluate(expr, "t");
        Check.notNull(r, "the script threw: " + expr);
        return r;
    }

    /**
     * Returns the name of the error that a statement throws, or <code>no error</code>.
     * <p>
     * 문장이 던지는 오류의 이름을 반환한다. 던지지 않으면 <code>no error</code>.
     *
     * @param statement the statement / 문장
     * @return the error name / 오류 이름
     */
    private String errorOf(String statement) {
        return js("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name; } })()");
    }

    /**
     * Runs the tasks of the page until an expression is true.
     * <p>
     * 식이 참이 될 때까지 페이지의 작업을 실행한다.
     *
     * @param expr    the expression / 식
     * @param message the message of a failure / 실패 메시지
     * @throws Exception on failure / 실패 시
     */
    private void until(String expr, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 식이 참이 될 때까지 작업을 실행하며 짧게 쉰다.
        while (System.currentTimeMillis() < end) {
            f.advance(0);
            if ("true".equals(f.scripts.evaluate("String(!!(" + expr + "))", "t"))) {
                return;
            }
            Thread.sleep(2);
        }
        Check.fail("timed out: " + message + " (waiting for: " + expr + ")");
    }

    /**
     * Runs the tasks of the page for a time.
     * <p>
     * 페이지의 작업을 일정 시간 동안 실행한다.
     *
     * @param millis the time / 시간
     * @throws Exception on failure / 실패 시
     */
    private void settle(long millis) throws Exception {
        long end = System.currentTimeMillis() + millis;
        // 정해진 시간 동안 작업을 실행하며 짧게 쉰다.
        while (System.currentTimeMillis() < end) {
            f.advance(0);
            Thread.sleep(2);
        }
        f.advance(0);
    }

    /**
     * Creates an element with a media file that is loaded and ready, connected to a context through a gain to its destination.
     * <p>
     * 로드되어 준비된 미디어 파일이 있는 요소를 만들고, 게인을 거쳐 컨텍스트의 목적지에 연결한다.
     *
     * @param file the file / 파일
     * @param gain the gain / 게인
     * @param url  the URL of the file / 파일의 URL
     * @throws Exception on failure / 실패 시
     */
    private void connected(byte[] file, double gain, String url) throws Exception {
        int requests = media.callbacks.size();
        js("var a = new Audio('" + url + "'); var rt = new AudioContext(); var src = rt.createMediaElementSource(a); var g = rt.createGain(); g.gain.value = " + gain
                + "; src.connect(g); g.connect(rt.destination);");
        deliver(requests, file);
        until("a.readyState === 4", "the element is loaded");
    }

    /**
     * Waits until the page has requested a media resource (the number of requests is above a number) and delivers a file as its response.
     * <p>
     * 페이지가 미디어 리소스를 요청할 때까지(요청 수가 어떤 수를 넘을 때까지) 기다렸다가 응답으로 파일을 전달한다.
     *
     * @param requestsBefore the number of requests before the one that is awaited / 기다리는 요청 전의 요청 수
     * @param file           the file / 파일
     * @throws Exception on failure / 실패 시
     */
    private void deliver(int requestsBefore, byte[] file) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        // 요청이 올 때까지 페이지의 작업을 실행하며 쉰다.
        while (media.callbacks.size() <= requestsBefore) {
            f.advance(0);
            Thread.sleep(2);
            if (System.currentTimeMillis() > end) {
                Check.fail("the page did not request the media resource");
            }
        }
        media.ok(requestsBefore, file);
    }

    /**
     * Waits until what is audible in an output stopped growing (for 200 ms), the page running its tasks meanwhile, and returns it. The end of an element is reported when the graph consumed the
     * last frame, which is a few milliseconds before the rendering thread has written that quantum to the output; this removes the race.
     * <p>
     * 출력에서 들리는 것이 더는 늘지 않을 때까지(200 ms 동안) 기다리며 그동안 페이지의 작업을 실행하고, 그것을 반환한다. 요소의 끝은 그래프가 마지막 프레임을 소비했을 때 알려지는데, 그것은
     * 렌더링 스레드가 그 quantum 을 출력에 쓰기 몇 ms 전이다. 이것이 그 경쟁을 없앤다.
     *
     * @param out the output / 출력
     * @return the audible samples / 들리는 표본
     * @throws Exception on failure / 실패 시
     */
    private float[] stable(ManualOutput out) throws Exception {
        long end = System.currentTimeMillis() + 15000;
        int last = -1;
        long since = System.currentTimeMillis();
        // 들리는 표본의 수가 200 ms 동안 그대로일 때까지 기다린다.
        while (System.currentTimeMillis() < end) {
            settle(20);
            int now = audible(out).length;
            if (now != last) {
                last = now;
                since = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - since >= 200) {
                break;
            }
        }
        return audible(out);
    }

    /**
     * Returns the audible samples that an element of the volume 0.5 delivers for the frames of the ramp from a frame to another (the player multiplies by 0.5 in 16 bit fixed point,
     * rounding down, so a sample of 1 becomes 0 and is not audible).
     * <p>
     * 음량 0.5 의 요소가 램프의 한 프레임부터 다른 프레임까지에 대해 전달하는 들리는 표본을 반환한다(재생기는 16비트 고정 소수점으로 0.5 를 곱하며 내림하므로 1 인 표본은 0 이 되어 들리지 않는다).
     *
     * @param from the first frame / 첫 프레임
     * @param to   the frame after the last / 마지막 다음 프레임
     * @return the samples / 표본
     */
    private static float[] halved(int from, int to) {
        float[] kept = new float[to - from];
        int n = 0;
        // 프레임마다 16비트 고정 소수점으로 반을 곱하고 0 이 아닌 표본만 모은다.
        for (int i = from; i < to; i++) {
            int scaled = (ramp(i) * 16384) >> 15;
            if (scaled != 0) {
                kept[n++] = scaled / 32768f;
            }
        }
        return java.util.Arrays.copyOf(kept, n);
    }

    /**
     * Returns the samples that are not zero of the first channel of an output, in order (a gap of silence that the stream may hold, for example before the start or when the player was late,
     * drops out).
     * <p>
     * 출력의 첫 채널에서 0 이 아닌 표본을 순서대로 반환한다(스트림에 있을 수 있는 무음 구간, 예를 들어 시작 전이나 재생기가 늦었을 때는 빠진다).
     *
     * @param out the output / 출력
     * @return the samples / 표본
     */
    private static float[] audible(ManualOutput out) {
        float[] all = out.copyChannel(0);
        float[] kept = new float[all.length];
        int n = 0;
        // 0 이 아닌 표본만 모은다.
        for (int i = 0; i < all.length; i++) {
            if (all[i] != 0f) {
                kept[n++] = all[i];
            }
        }
        return java.util.Arrays.copyOf(kept, n);
    }

    /**
     * The sound of an element that is connected through a gain arrives at the output exactly: every sample of the file, divided by 32768 and multiplied by the gain, in order and once. The element
     * has no sound output of its own, it reports its events as ever, and its end comes after the graph consumed the last frame (the element takes about as long as the media is long: the graph is
     * its clock). The node is a <code>MediaElementAudioSourceNode</code> that holds the element.
     * <p>
     * 게인을 거쳐 연결한 요소의 소리는 출력에 정확히 도착한다: 파일의 모든 표본이 32768 로 나뉘고 게인이 곱해져 순서대로 한 번씩. 요소는 자기 소리 출력이 없고 이벤트는 전처럼 알리며, 끝은
     * 그래프가 마지막 프레임을 소비한 뒤에 온다(요소는 미디어의 길이만큼 걸린다: 그래프가 요소의 시계이다). 노드는 요소를 붙잡는 <code>MediaElementAudioSourceNode</code> 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testElementSoundArrivesThroughTheGraphExactly() throws Exception {
        connected(rampWav(RATE, 9000), 0.5, "http://example.test/dir/s.wav");
        Check.equal("true:true:[object MediaElementAudioSourceNode]", js("[src instanceof MediaElementAudioSourceNode, src.mediaElement === a, Object.prototype.toString.call(src)].join(':')"),
                "the node holds the element");
        js("var t0 = rt.currentTime; var ended = false; a.onended = function () { ended = true; }; a.play();");
        until("ended", "the element reached its end");
        double took = Double.parseDouble(js("String(rt.currentTime - t0)"));
        Check.isTrue(took >= 9000.0 / RATE - 0.02 && took < 9000.0 / RATE + 0.5, "the graph is the clock: the element needed " + took + " s of context time for 0.1875 s of audio");
        ManualOutput out = outputs.waitFor(1, 10000);
        float[] heard = stable(out);
        Check.equal(9000, heard.length, "every frame of the file reached the output once");
        // 모든 표본이 (16비트 표본 / 32768) * 게인 과 정확히 같아야 한다.
        for (int i = 0; i < heard.length; i++) {
            float want = ramp(i) / 32768f * 0.5f;
            if (heard[i] != want) {
                Check.fail("sample " + i + ": " + heard[i] + " instead of " + want);
            }
        }
        Check.equal(0, speakers.openCalls, "the element has no output of its own");
        Check.equal("true,true,0.1875", js("[a.paused, a.ended, a.currentTime].join(',')"), "the state at the end");
    }

    /**
     * The controls of the element work through the graph: pausing makes the graph quiet and stops the position, playing goes on without a loss or a repetition; a seek discards what was
     * queued and goes on from the new position; the volume of the element is applied to what enters the graph (a browser does the same) and muting makes the graph silent while the element goes on.
     * <p>
     * 요소의 조작은 그래프를 거쳐 동작한다: 일시정지하면 그래프가 조용해지고 위치가 멈추며, 재생하면 잃거나 반복하는 것 없이 이어 간다. 탐색은 큐에 있던 것을 버리고 새 위치에서 이어 간다.
     * 요소의 음량은 그래프로 들어가는 것에 적용되며(브라우저도 그렇다) 음소거는 요소가 계속 진행하는 동안 그래프를 무음으로 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseSeekAndVolumeWorkThroughTheGraph() throws Exception {
        connected(rampWav(RATE, 9000), 1.0, "http://example.test/dir/s.wav");
        js("a.volume = 0.5; a.play();");
        ManualOutput out = outputs.waitFor(1, 10000);
        until("a.currentTime > 0.03", "the element plays");
        js("a.pause();");
        settle(80);
        double position = Double.parseDouble(js("String(a.currentTime)"));
        int heardAtPause = audible(out).length;
        settle(200);
        Check.equal(heardAtPause, audible(out).length, "nothing is audible while the element is paused");
        Check.equal(position, Double.parseDouble(js("String(a.currentTime)")), "the position stands still");
        js("a.play();");
        until("a.currentTime > " + (position + 0.05), "the element plays on");
        js("a.currentTime = 0.15; ");
        until("a.ended", "the element reached its end after the seek to 0.15 s (7200 frames)");
        float[] heard = stable(out);
        // 음량 0.5 는 16비트 고정 소수점 이득이며 작은 표본은 0 이 되어 들리지 않으므로 기대하는 열에서도 뺀다.
        float[] afterSeek = halved(7200, 9000);
        int tail = afterSeek.length;
        Check.isTrue(heard.length > tail, "the stream holds the part before the seek and the part after it: " + heard.length);
        // 끝부분은 탐색한 위치(프레임 7200)부터 파일의 끝까지여야 한다.
        for (int i = 0; i < tail; i++) {
            if (heard[heard.length - tail + i] != afterSeek[i]) {
                Check.fail("after the seek sample " + i + ": " + heard[heard.length - tail + i] + " instead of " + afterSeek[i]);
            }
        }
        int head = heard.length - tail;
        float[] beforeSeek = halved(0, 7200);
        // 앞부분은 파일의 처음부터 끊김 없이 이어진 부분이어야 한다.
        for (int i = 0; i < head; i++) {
            if (heard[i] != beforeSeek[i]) {
                Check.fail("before the seek sample " + i + ": " + heard[i] + " instead of " + beforeSeek[i]);
            }
        }
        Check.isTrue(head > 0 && head < beforeSeek.length, "the element was somewhere before the seek position: " + head);
        // 음소거: 요소가 계속 진행하지만 그래프에는 무음이 전달된다.
        js("a.muted = true; a.currentTime = 0; a.play();");
        int before = audible(out).length;
        until("a.ended", "the muted element plays to its end");
        Check.equal(before, audible(out).length, "a muted element adds nothing audible");
    }

    /**
     * A suspended context stalls the elements that are connected to it: nobody takes their sound, so their position stands still (the element is not paused: <code>paused</code> stays false), and
     * <code>resume()</code> lets them go on without a lost or a repeated frame.
     * <p>
     * 일시정지된 컨텍스트는 거기에 연결된 요소를 멈춘다: 아무도 소리를 가져가지 않으므로 위치가 멈춘다(요소는 일시정지가 아니다: <code>paused</code> 는 false 로 남는다). <code>resume()</code>
     * 은 잃거나 반복하는 프레임 없이 이어 가게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSuspendedContextStallsTheElementAndResumeContinues() throws Exception {
        connected(rampWav(RATE, 9000), 1.0, "http://example.test/dir/s.wav");
        js("a.play();");
        until("a.currentTime > 0.03", "the element plays");
        js("var susp = false; rt.suspend().then(function () { susp = true; });");
        until("susp", "the context is suspended");
        settle(100);
        double position = Double.parseDouble(js("String(a.currentTime)"));
        settle(300);
        Check.equal(position, Double.parseDouble(js("String(a.currentTime)")), "the position of the element stands still while the context is suspended");
        Check.equal("false", js("String(a.paused)"), "the element is not paused");
        js("var ended = false; a.onended = function () { ended = true; }; rt.resume();");
        until("ended", "the element goes on after the context was resumed");
        float[] heard = stable(outputs.waitFor(1, 10000));
        Check.isTrue(outputs.count() >= 2, "the resumed context opened a new output");
        // 일시정지 전과 뒤의 모든 출력을 이어 붙이면 파일 전체가 한 번씩 순서대로여야 한다.
        float[] all = new float[0];
        // 컨텍스트의 모든 구간(출력)의 들리는 표본을 이어 붙인다.
        for (int i = 0; i < outputs.count(); i++) {
            float[] part = stable(outputs.get(i));
            float[] joined = java.util.Arrays.copyOf(all, all.length + part.length);
            System.arraycopy(part, 0, joined, all.length, part.length);
            all = joined;
        }
        // 일시정지는 렌더링 스레드가 요소에서 가져왔지만 아직 장치에 넘기지 못한 오디오(많아야 청크 하나, interactive 에서 3 ms 안팎)를 잃는다. 그 밖의 프레임은 한 번씩 순서대로여야 한다.
        int lost = 9000 - all.length;
        Check.isTrue(lost >= 0 && lost <= 256, "the stream holds the file less the audio that was in flight at the suspension (the first output held " + heard.length + "): " + lost + " frames missing");
        int joint = 0;
        // 파일의 표본과 처음 달라지는 자리(일시정지 때 잃은 부분)를 찾는다.
        while (joint < all.length && all[joint] == ramp(joint) / 32768f) {
            joint++;
        }
        Check.isTrue(lost == 0 || (joint > 0 && joint < all.length), "the loss is at the join of the two outputs: " + joint);
        // 그 뒤로는 잃은 만큼 건너뛴 파일의 표본과 같아야 한다.
        for (int i = joint; i < all.length; i++) {
            if (all[i] != ramp(i + lost) / 32768f) {
                Check.fail("sample " + i + " of the whole stream after the loss of " + lost + " frames: " + all[i] + " instead of " + ramp(i + lost) / 32768f);
            }
        }
    }

    /**
     * An element with <code>loop</code> goes on through the graph at the end of the file without a gap: the audible samples are the file over and over, in order.
     * <p>
     * <code>loop</code> 가 있는 요소는 파일의 끝에서 틈 없이 그래프를 거쳐 계속 간다: 들리는 표본은 파일이 되풀이되는 것이며 순서대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoopGoesOnThroughTheGraph() throws Exception {
        connected(rampWav(RATE, 3000), 1.0, "http://example.test/dir/s.wav");
        js("a.loop = true; a.play();");
        ManualOutput out = outputs.waitFor(1, 10000);
        long end = System.currentTimeMillis() + 20000;
        // 파일이 두 번 반 이상 재생될 때까지 기다린다.
        while (audible(out).length < 7500 && System.currentTimeMillis() < end) {
            settle(20);
        }
        js("a.pause();");
        float[] heard = audible(out);
        Check.isTrue(heard.length >= 7500, "the file played more than twice: " + heard.length);
        // 들리는 표본이 파일의 되풀이여야 한다.
        for (int i = 0; i < 7500; i++) {
            float want = ramp(i % 3000) / 32768f;
            if (heard[i] != want) {
                Check.fail("sample " + i + " of the looped stream: " + heard[i] + " instead of " + want);
            }
        }
    }

    /**
     * A media of another sample rate than the context (44100 Hz in a context of 48000 Hz) is converted by the resampler: the length is right (the element needs the same time) and the signal is
     * the same sine inside the stream.
     * <p>
     * 컨텍스트와 다른 샘플링 주파수의 미디어(48000 Hz 컨텍스트의 44100 Hz)는 리샘플러가 바꾼다: 길이가 맞고(요소가 같은 시간이 걸린다) 신호는 스트림 안쪽에서 같은 사인이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnotherSampleRateIsConverted() throws Exception {
        int frames = 22050;
        byte[] data = new byte[frames * 2];
        // 440 Hz 사인(진폭 0.5)의 16비트 표본을 쓴다.
        for (int i = 0; i < frames; i++) {
            short s = (short) Math.round(16384 * Math.sin(2 * Math.PI * 440 * i / 44100.0));
            data[i * 2] = (byte) s;
            data[i * 2 + 1] = (byte) (s >> 8);
        }
        connected(AudioKit.wav(1, 1, 44100, 16, data, -1), 1.0, "http://example.test/dir/s.wav");
        js("var ended = false; a.onended = function () { ended = true; }; a.play();");
        until("ended", "the element reached its end");
        ManualOutput out = outputs.waitFor(1, 10000);
        long until = System.currentTimeMillis() + 5000;
        float[] raw = out.copyChannel(0);
        int first;
        int last;
        // 요소가 끝난 뒤 변환기의 꼬리까지 그래프가 전달하고 그 뒤에 무음이 이어질 때까지 잠깐 기다린다.
        do {
            settle(10);
            raw = out.copyChannel(0);
            first = 0;
            // 첫 번째 0 이 아닌 표본을 찾는다. 사인의 첫 표본(프레임 0)은 정확히 0 이므로 그 바로 앞이 신호의 시작이다.
            while (first < raw.length && raw[first] == 0f) {
                first++;
            }
            last = raw.length - 1;
            // 마지막 0 이 아닌 표본을 찾는다.
            while (last > 0 && raw[last] == 0f) {
                last--;
            }
        } while (raw.length - 1 - last < 512 && System.currentTimeMillis() < until);
        int start = first - 1;
        int audibleFrames = audible(out).length;
        // 24000 프레임 중 사인의 영점에서 정확히 0 이 되는 표본(약 열 개)은 들리는 표본으로 세지 않는다. 마지막 1.5 ms 는 변환기의 꼬리라 몇 ms 늦게 온다.
        Check.isTrue(audibleFrames >= 23980 && audibleFrames <= 24000, "half a second of 44100 Hz audio is 24000 frames at 48000 Hz (less the exact zeros of the sine): " + audibleFrames);
        // 기대하는 것은 디코딩 경로의 리샘플러가 파일의 표본에 주는 결과 그 자체이다(MediaElementTapTest 가 탭이 그것과 비트 단위로 같음을 보인다).
        float[] input = new float[frames];
        // 입력은 파일의 16비트 표본을 32768 로 나눈 값이다.
        for (int i = 0; i < frames; i++) {
            input[i] = (short) Math.round(16384 * Math.sin(2 * Math.PI * 440 * i / 44100.0)) / 32768f;
        }
        float[] converted = org.duckdns.hjow.swingwebpanel.media.webaudio.decode.Resampler.resample(input, 44100, 48000);
        int firstWanted = 0;
        // 기준 결과에서 0 이 아닌 첫 프레임을 찾아 출력의 첫 0 이 아닌 표본과 맞춘다.
        while (converted[firstWanted] == 0f) {
            firstWanted++;
        }
        int offset = first - firstWanted;
        Check.equal(24000, converted.length, "the conversion of half a second is 24000 frames");
        // 스트림이 끝나기 전의 모든 프레임이 기준 결과와 비트 단위로 같아야 한다(마지막 몇 ms 는 변환기의 꼬리라 늦게 온다).
        for (int i = 0; i < 23800; i++) {
            if (raw[offset + i] != converted[i]) {
                Check.fail("frame " + i + " of the converted stream is " + raw[offset + i] + " but the resampler gives " + converted[i]);
            }
        }
        double worst = 0;
        // 신호의 안쪽에서 새 주파수의 같은 사인이어야 한다(첫 프레임은 리샘플러가 신호의 시작을 0 으로 연장하는 영향이 있으므로 사인의 정확한 시간에서 1 프레임 이내로 맞춘다).
        for (int i = 500; i < 23500; i++) {
            double want = 0.5 * Math.sin(2 * Math.PI * 440 * i / 48000.0);
            worst = Math.max(worst, Math.abs(raw[offset + i] - want));
        }
        Check.isTrue(worst < 2e-3, "the converted signal is the same sine: the largest error is " + worst);
        Check.equal(0, speakers.openCalls, "no output of its own");
    }

    /**
     * An element that plays to the speakers is switched to the graph in the middle of the playing: the sound that the speakers played, followed by the sound that the graph delivers, is the file
     * from its first frame to its last without a skipped or a repeated frame; the position of the element goes on, and the output of the element is closed.
     * <p>
     * 스피커로 재생하던 요소는 재생 도중에 그래프로 바뀐다: 스피커가 재생한 소리에 그래프가 전달하는 소리가 이어지면 건너뛰거나 반복한 프레임 없이 파일의 첫 프레임부터 마지막 프레임까지이다.
     * 요소의 위치는 계속 가고 요소의 출력은 닫힌다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testElementThatPlaysToTheSpeakersIsSwitchedToTheGraph() throws Exception {
        speakers.capacity = 2000;
        js("var a = new Audio('http://example.test/dir/s.wav'); var rt = new AudioContext();");
        deliver(0, rampWav(RATE, 9000));
        until("a.readyState === 4", "loaded");
        js("a.play();");
        FakeOutputs.Out old = speakers.await(0);
        long played = 0;
        long end = System.currentTimeMillis() + 10000;
        // 스피커가 1500 프레임을 재생하는 척하고, 큐가 다시 찰 때까지 기다린다.
        while (played < 1500 && System.currentTimeMillis() < end) {
            played += old.consume(1500 - played);
            Thread.sleep(2);
        }
        // 재생기가 스피커 출력의 큐를 다시 채울 때까지 기다린다.
        while (old.getQueuedFrames() < 1500 && System.currentTimeMillis() < end) {
            Thread.sleep(2);
        }
        js("var src = rt.createMediaElementSource(a); src.connect(rt.destination); var ended = false; a.onended = function () { ended = true; };");
        until("ended", "the element reached its end through the graph");
        float[] heard = stable(outputs.waitFor(1, 10000));
        // 노드는 만들어진 순간부터 렌더링 스레드가 quantum 마다 요소에서 가져가므로, 스크립트가 destination 에 연결하기 전에 가져간 quantum 은 어디에도 가지 않고 사라진다(브라우저도 같다).
        // 그 사이는 스크립트 문장 둘이라 많아야 한두 quantum 이다. 나머지는 스피커가 재생한 곳부터 파일의 끝까지 틈 없이 이어져야 한다.
        int lost = 9000 - (int) played - heard.length;
        Check.isTrue(lost >= 0 && lost <= 512 && lost % 128 == 0, "the graph delivered the rest of the file (the speakers played " + played + " frames), less the quanta that the node took before the script "
                + "connected it: " + lost + " frames were lost");
        // 사라진 quantum 만큼 건너뛰고 파일의 끝까지가 이어져야 한다.
        for (int i = 0; i < heard.length; i++) {
            float want = ramp(played + lost + i) / 32768f;
            if (heard[i] != want) {
                Check.fail("the sample after the " + played + " that the speakers played (and the " + lost + " that were taken before the connection), number " + i + ": " + heard[i] + " instead of " + want);
            }
        }
        Check.isTrue(old.closeCalls() >= 1, "the output of the speakers was closed");
        Check.equal(1, speakers.outputs.size(), "and no second output of the speakers was opened");
    }

    /**
     * The errors of <code>createMediaElementSource</code> and of the constructor: an element can be connected once (also to another context), a <code>video</code> element is not supported (and is left
     * alone), something that is no media element is a <code>TypeError</code>, an offline context has no such factory and its constructor use is a <code>TypeError</code>, a closed context refuses, and the
     * constructor form works like the factory.
     * <p>
     * <code>createMediaElementSource</code> 와 생성자의 오류: 요소는 한 번만 연결할 수 있고(다른 컨텍스트에도 마찬가지), <code>video</code> 요소는 지원하지 않으며(요소는 건드리지 않는다),
     * 미디어 요소가 아닌 것은 <code>TypeError</code> 이고, 오프라인 컨텍스트에는 그런 팩터리가 없으며 생성자로 쓰면 <code>TypeError</code> 이고, 닫힌 컨텍스트는 거부하며, 생성자 형식은 팩터리처럼
     * 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorsOfConnectingAnElement() throws Exception {
        js("var rt = new AudioContext(), rt2 = new AudioContext(); var a = document.getElementById('tag'); var first = rt.createMediaElementSource(a);");
        Check.equal("InvalidStateError", errorOf("rt.createMediaElementSource(a)"), "a second node for the same element");
        Check.equal("InvalidStateError", errorOf("rt2.createMediaElementSource(a)"), "the element is bound to one node of one context for good");
        Check.equal("InvalidStateError", errorOf("new MediaElementAudioSourceNode(rt2, {mediaElement: a})"), "also through the constructor");
        Check.equal("NotSupportedError", errorOf("rt.createMediaElementSource(document.getElementById('movie'))"), "a video element is not supported");
        Check.equal("NotSupportedError", errorOf("new MediaElementAudioSourceNode(rt, {mediaElement: document.getElementById('movie')})"), "also through the constructor");
        Check.equal("0", js("String(document.getElementById('movie').networkState)"), "and the video element is left alone");
        Check.equal("TypeError", errorOf("rt.createMediaElementSource(document.getElementById('box'))"), "a div is no media element");
        Check.equal("TypeError", errorOf("rt.createMediaElementSource({})"), "an object is none either");
        Check.equal("TypeError", errorOf("rt.createMediaElementSource(null)"), "null");
        Check.equal("TypeError", errorOf("rt.createMediaElementSource()"), "no argument");
        Check.equal("TypeError", errorOf("new MediaElementAudioSourceNode(rt)"), "the options are required");
        Check.equal("TypeError", errorOf("new MediaElementAudioSourceNode(rt, {})"), "and so is their member");
        Check.equal("TypeError", errorOf("new MediaElementAudioSourceNode({}, {mediaElement: new Audio()})"), "the context must be one");
        Check.equal("TypeError", errorOf("MediaElementAudioSourceNode(rt, {mediaElement: new Audio()})"), "new is required");
        js("var oc = new OfflineAudioContext(1, 128, 8000);");
        Check.equal("undefined", js("typeof oc.createMediaElementSource"), "an offline context has no factory");
        Check.equal("TypeError", errorOf("new MediaElementAudioSourceNode(oc, {mediaElement: new Audio()})"), "and cannot be used as the context of the node");
        Check.equal("true:true", js("var b = new Audio(); var viaCtor = new MediaElementAudioSourceNode(rt, {mediaElement: b}); [viaCtor.mediaElement === b, viaCtor instanceof AudioNode].join(':')"),
                "the constructor form");
        Check.equal("true", js("String(typeof viaCtor.connect === 'function' && viaCtor.numberOfInputs === 0 && viaCtor.numberOfOutputs === 1)"), "a source: no input, one output");
        js("var done = false; rt2.close().then(function () { done = true; });");
        until("done", "closed");
        Check.equal("InvalidStateError", errorOf("rt2.createMediaElementSource(new Audio())"), "a closed context refuses");
        Check.equal("0", String.valueOf(speakers.openCalls), "no sound output was opened by any of this");
    }

    /**
     * Media of another origin that the page may not read is silent in the graph (a browser does not let a page measure it) while the element is not stalled: its position follows the graph and its
     * end comes; media of the origin of the page is audible in the same graph.
     * <p>
     * 페이지가 읽어서는 안 되는 다른 오리진의 미디어는 그래프에서 무음이다(브라우저는 페이지가 그것을 잴 수 없게 한다). 요소는 멈추지 않는다: 위치는 그래프를 따르고 끝이 온다. 페이지와 같은
     * 오리진의 미디어는 같은 그래프에서 들린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMediaOfAnotherOriginIsSilentInTheGraph() throws Exception {
        connected(rampWav(RATE, 6000), 1.0, "http://other.example/dir/s.wav");
        js("var ended = false; a.onended = function () { ended = true; }; a.play();");
        until("ended", "the element is not stalled by the silence");
        ManualOutput out = outputs.waitFor(1, 10000);
        Check.equal(0, audible(out).length, "nothing of the other origin is audible");
        Check.isTrue(out.getWrittenFrames() >= 6000, "the graph ran for the length of the media: " + out.getWrittenFrames());
        Check.equal("0.125", js("String(a.currentTime)"), "the element reached its end");
        int requests = media.callbacks.size();
        js("var b = new Audio('http://example.test/dir/own.wav'); var srcB = rt.createMediaElementSource(b); srcB.connect(rt.destination);");
        deliver(requests, rampWav(RATE, 6000));
        until("b.readyState === 4", "the second element is loaded");
        js("var endedB = false; b.onended = function () { endedB = true; }; b.play();");
        until("endedB", "the media of the origin of the page plays");
        Check.equal(6000, stable(out).length, "and is audible in the same graph");
    }

    /**
     * Closing the context releases the element for good: it stays bound to its node and goes on playing silently, at the normal speed, to its end (a browser cannot restore the output of the
     * element either), and it never opens an output of its own.
     * <p>
     * 컨텍스트를 닫으면 요소가 영구히 풀린다: 요소는 자기 노드에 묶인 채 보통 속도로 소리 없이 끝까지 계속 재생된다(브라우저도 요소의 출력을 되돌릴 수 없다). 자기 출력은 열지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClosedContextLetsTheElementPlayOnSilently() throws Exception {
        connected(rampWav(RATE, 24000), 1.0, "http://example.test/dir/s.wav");
        js("var ctxClosed = false; rt.close().then(function () { ctxClosed = true; });");
        until("ctxClosed", "the context is closed");
        long t0 = System.nanoTime();
        js("var ended = false; a.onended = function () { ended = true; }; a.play();");
        until("ended", "the element reached its end although the context is closed");
        double seconds = (System.nanoTime() - t0) / 1e9;
        Check.isTrue(seconds >= 0.3 && seconds < 5, "at about the normal speed (0.5 s of audio): " + seconds + " s");
        Check.equal(0, speakers.openCalls, "it never opened an output of its own");
        ManualOutput out = outputs.get(0);
        Check.isTrue(out == null || audible(out).length == 0, "and nothing was heard");
    }

    /**
     * Many contexts that are created and closed, each with an element connected, leave no thread, no output and no player behind, and a page that is replaced ends the contexts that it
     * left open.
     * <p>
     * 만들고 닫는 많은 컨텍스트는, 각각 요소를 연결해도, 스레드도 출력도 재생기도 남기지 않으며, 교체되는 페이지는 열어 둔 컨텍스트를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNothingIsLeftBehind() throws Exception {
        int base = playerThreads();
        // 컨텍스트를 만들고 요소를 연결해 잠깐 재생한 뒤 닫는 것을 되풀이한다.
        for (int i = 0; i < 30; i++) {
            js("var rt = new AudioContext(); var a = new Audio('http://example.test/dir/s" + i + ".wav'); var s = rt.createMediaElementSource(a); s.connect(rt.destination);");
            deliver(i, rampWav(RATE, 2000));
            until("a.readyState === 4", "loaded " + i);
            js("a.play(); var ctxClosed = false; rt.close().then(function () { ctxClosed = true; });");
            until("ctxClosed", "closed " + i);
        }
        js("var a = null, rt = null, s = null;");
        long end = System.currentTimeMillis() + 10000;
        // 렌더링 스레드와 재생 스레드가 모두 끝날 때까지 기다린다(재생기는 요소가 수거되거나 끝나면 닫힌다).
        while (threadCount("swingwebpanel-audio-render-") > 0 && System.currentTimeMillis() < end) {
            settle(20);
        }
        Check.equal(0, threadCount("swingwebpanel-audio-render-"), "no rendering thread is left");
        int count = outputs.count();
        // 열린 출력이 모두 닫혀야 한다.
        for (int i = 0; i < count; i++) {
            ManualOutput o = outputs.get(i);
            Check.isTrue(o.isClosed(), "the output number " + i + " was closed");
        }
        int requests = media.callbacks.size();
        js("var open1 = new AudioContext(); var e1 = new Audio('http://example.test/dir/last.wav'); open1.createMediaElementSource(e1).connect(open1.destination); e1.loop = true;");
        deliver(requests, rampWav(RATE, 2000));
        until("e1.readyState === 4", "the last element is loaded");
        js("e1.play();");
        settle(100);
        Check.equal(1, threadCount("swingwebpanel-audio-render-"), "one context is open");
        f.scripts.dispose();
        end = System.currentTimeMillis() + 10000;
        // 페이지를 폐기하면 열어 둔 컨텍스트와 모든 재생기가 끝나야 한다.
        while ((threadCount("swingwebpanel-audio-render-") > 0 || playerThreads() > base) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(0, threadCount("swingwebpanel-audio-render-"), "disposing the page ended the open context");
        Check.isTrue(playerThreads() <= base, "and closed every player: " + playerThreads() + " player threads against " + base + " before");
        Check.isTrue(outputs.last().isClosed(), "and the output of the context");
    }

    /**
     * Counts the live playback threads of the media elements (their name is exactly <code>swingwebpanel-audio</code>).
     * <p>
     * 미디어 요소의 살아 있는 재생 스레드를 센다(이름이 정확히 <code>swingwebpanel-audio</code> 이다).
     *
     * @return the number of threads / 스레드 수
     */
    private static int playerThreads() {
        Thread[] all = new Thread[Thread.activeCount() + 32];
        int n = Thread.enumerate(all);
        int count = 0;
        // 살아 있는 스레드 중 미디어 재생기의 스레드(이름이 정확히 swingwebpanel-audio)를 센다.
        for (int i = 0; i < n; i++) {
            if (all[i] != null && all[i].isAlive() && "swingwebpanel-audio".equals(all[i].getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the live threads whose names start with a prefix.
     * <p>
     * 이름이 접두사로 시작하는 살아 있는 스레드를 센다.
     *
     * @param prefix the prefix / 접두사
     * @return the number / 수
     */
    private static int threadCount(String prefix) {
        Thread[] all = new Thread[Thread.activeCount() + 32];
        int n = Thread.enumerate(all);
        int count = 0;
        // 살아 있는 스레드 중 이름이 접두사로 시작하는 것을 센다.
        for (int i = 0; i < n; i++) {
            if (all[i] != null && all[i].isAlive() && all[i].getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }
}
