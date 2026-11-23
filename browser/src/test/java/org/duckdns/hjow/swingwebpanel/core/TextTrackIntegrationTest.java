package org.duckdns.hjow.swingwebpanel.core;

import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.playback.ScriptedDecoder;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The text tracks of an <code>audio</code> element through the real component and real HTTP (plan item P20-13; FEATURE_MATRIX VID-02): the WebVTT file
 * of a <code>track</code> element is fetched with the common request layer (user agent, extra headers, cookies), a <code>default</code> track is
 * selected and fetched without the page doing anything, the cues arrive and their <code>enter</code> and <code>exit</code> events fire while the audio
 * plays, a file of another origin is refused unless the media element has a <code>crossorigin</code> attribute and the server allows it, and the
 * received source of the page stays as it was. The sound goes to a fake output ({@link FakeOutputs}); nothing is heard.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 를 통한 <code>audio</code> 요소의 텍스트 트랙(계획 P20-13, FEATURE_MATRIX VID-02): <code>track</code> 요소의 WebVTT 파일을 공통 요청
 * 계층(User-Agent, 추가 헤더, 쿠키)으로 가져오고, <code>default</code> 트랙은 페이지가 아무것도 하지 않아도 선택되어 가져오며, cue 가 도착하고 오디오가
 * 재생되는 동안 <code>enter</code>, <code>exit</code> 이벤트가 발생하고, 다른 오리진의 파일은 미디어 요소에 <code>crossorigin</code> 속성이 있고 서버가
 * 허용하지 않으면 거부되며, 페이지의 수신 원문은 그대로이다. 소리는 가짜 출력({@link FakeOutputs})으로 가며 아무 소리도 들리지 않는다.
 */
public class TextTrackIntegrationTest {

    /**
     * The WebVTT file of the tests: two cues, the second with settings.
     * <p>
     * 시험의 WebVTT 파일: cue 둘, 둘째는 설정이 있다.
     */
    private static final String VTT = "WEBVTT\n\n1\n00:00.100 --> 00:00.250\nfirst\n\n2\n00:00.400 --> 00:00.500 line:2 align:start\nsecond <i>cue</i>\n";

    /**
     * The functions of the test pages: <code>mark(name)</code> appends a name to the order that is stored in the sessionStorage of the component.
     * <p>
     * 시험 페이지의 함수: <code>mark(name)</code> 은 컴포넌트의 sessionStorage 에 저장된 순서에 이름을 덧붙인다.
     */
    private static final String MARK = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}</script>";

    /**
     * Creates the suite (required by the runner) and registers the scripted decoder.
     * <p>
     * 스위트를 만들고(실행기가 필요로 함) 스크립트 디코더를 등록한다.
     */
    public TextTrackIntegrationTest() {
        ScriptedDecoder.install();
    }

    /**
     * Creates the component on the EDT with a fake sound output.
     * <p>
     * EDT 에서 가짜 소리 출력을 쓰는 컴포넌트를 만든다.
     *
     * @param outs the outputs / 출력
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel(final FakeOutputs outs) throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel() {
                    /**
                     * Gives the test output.
                     * <p>
                     * 시험 출력을 준다.
                     *
                     * @return the fake factory / 가짜 팩터리
                     */
                    protected AudioOutputFactory createAudioOutputFactory() {
                        return outs;
                    }
                };
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads the order that the scripts stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 순서를 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @return the order, empty if nothing was stored / 순서. 저장된 것이 없으면 빈 문자열
     */
    private static String order(SwingWebPanel panel) {
        // 복사한 저장소 항목에서 순서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if ("order".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    /**
     * Counts how many times a part occurs in a text.
     * <p>
     * 텍스트에 어떤 부분이 몇 번 나오는지 센다.
     *
     * @param text the text / 텍스트
     * @param part the part / 부분
     * @return the number of occurrences / 나온 횟수
     */
    private static int countOf(String text, String part) {
        int n = 0;
        int at = text.indexOf(part);
        // 부분이 더 나오지 않을 때까지 위치를 옮기며 센다.
        while (at >= 0) {
            n++;
            at = text.indexOf(part, at + part.length());
        }
        return n;
    }

    /**
     * Waits until the stored order contains a part.
     * <p>
     * 저장된 순서에 어떤 부분이 들어갈 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param part    the part / 부분
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitOrder(SwingWebPanel panel, String part, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (order(panel).indexOf(part) < 0 && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.isTrue(order(panel).indexOf(part) >= 0, message + " (order: " + order(panel) + ")");
    }

    /**
     * Waits until the component confirmed a page.
     * <p>
     * 컴포넌트가 페이지를 확정할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url   the URL / URL
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Makes a mono 8 kHz WAV file of the scripted ramp.
     * <p>
     * 스크립트 램프의 모노 8 kHz WAV 파일을 만든다.
     *
     * @param frames the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] wav(int frames) {
        return AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, frames), -1);
    }

    /**
     * A <code>default</code> subtitle track is fetched by itself with the user agent, the extra headers and the cookies of the component; its cues
     * arrive; the page plays the audio and the cues enter and exit in order; the track list and the element's <code>load</code> event report what
     * happened; the source of the page is unchanged.
     * <p>
     * <code>default</code> 자막 트랙은 컴포넌트의 User-Agent, 추가 헤더, 쿠키와 함께 저절로 가져온다. cue 가 도착하고, 페이지가 오디오를 재생하며, cue 가 순서대로
     * enter, exit 한다. 트랙 목록과 요소의 <code>load</code> 이벤트가 일어난 일을 알린다. 페이지의 원문은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultTrackIsFetchedAndItsCuesFire() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        // 가짜 출력을 수동 모드(용량 1000 프레임)로 둔다: 재생 위치를 시험이 정하므로 재생 속도와 스레드 일정에 결과가 달라지지 않는다.
        FakeOutputs outs = new FakeOutputs(1000);
        SwingWebPanel panel = newPanel(outs);
        try {
            panel.setUserAgent("track-test/1.0");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Track-Test", "yes");
            panel.addHeaders(headers);
            String html = "<!doctype html><html><body>" + MARK + "<audio id=a src='/clip.wav' preload=auto>"
                    + "<track id=t default kind=subtitles srclang=en label='English' src='/subs.vtt'></audio>"
                    + "<script>var a = document.getElementById('a'), t = document.getElementById('t');"
                    + "mark('mode:' + t.track.mode + ':' + a.textTracks.length);"
                    + "t.addEventListener('load', function () { mark('load:' + t.track.cues.length + ':' + t.readyState + ':' + t.track.cues[1].line + ':' + t.track.cues[1].align);"
                    + "for (var i = 0; i < t.track.cues.length; i++) { (function (c) { c.onenter = function () { mark('enter:' + c.id); }; c.onexit = function () { mark('exit:' + c.id); }; })(t.track.cues[i]); }"
                    + "a.play(); });"
                    + "a.addEventListener('timeupdate', function () { mark('tu'); });"
                    + "t.addEventListener('error', function () { mark('error'); });</script></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html).header("Set-Cookie", "sid=xyz; Path=/"));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(6000)));
            server.routeFixed("/subs.vtt", new TestHttpServer.Response(200).text("text/vtt", VTT));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            // 소리를 조금씩(0.05초씩) 재생시키며 cue 가 모두 지나갈 때까지 진행한다. 위치가 바뀔 때마다 페이지가 처리하기를 기다린다.
            FakeOutputs.Out out = outs.await(0);
            long end = System.currentTimeMillis() + 30000;
            // 마지막 cue 가 끝날 때까지 한 걸음씩 진행한다.
            while (order(panel).indexOf("exit:2,") < 0 && System.currentTimeMillis() < end) {
                int before = countOf(order(panel), "tu,");
                out.consume(400);
                long until = System.currentTimeMillis() + 500;
                // 위치 변화가 timeupdate 로 페이지에 닿을 때까지 짧게 쉬며 확인한다(최대 0.5초).
                while (countOf(order(panel), "tu,") == before && System.currentTimeMillis() < until) {
                    Thread.sleep(5);
                }
            }
            awaitOrder(panel, "exit:2,", "the cues played");
            String order = order(panel);
            Check.isTrue(order.startsWith("mode:disabled:1,") || order.startsWith("mode:showing:1,"), "the page saw one track at the start: " + order);
            Check.contains(order, "load:2:2:2:start,", "load fired with the cues, the state loaded and the settings of the second cue");
            int e1 = order.indexOf("enter:1,");
            int x1 = order.indexOf("exit:1,");
            int e2 = order.indexOf("enter:2,");
            int x2 = order.indexOf("exit:2,");
            Check.isTrue(0 < e1 && e1 < x1 && x1 < e2 && e2 < x2, "enter and exit in the order of the times: " + order);
            Check.equal(1, server.count("/subs.vtt"), "one request for the file");
            TestHttpServer.Request request = server.last("/subs.vtt");
            Check.equal("track-test/1.0", request.header("User-Agent"), "the user agent of the component");
            Check.equal("yes", request.header("X-Track-Test"), "the extra header");
            Check.contains(request.header("Cookie"), "sid=xyz", "the cookie of the component");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A file of another origin: without the <code>crossorigin</code> attribute the request is refused and never reaches the server (the same-origin
     * fallback of the track request), and the element gets <code>error</code>; with the attribute and a server that allows the origin the file is
     * fetched and loaded; with the attribute and a server that does not allow it the file fails.
     * <p>
     * 다른 오리진의 파일: <code>crossorigin</code> 속성이 없으면 요청이 거부되어 서버에 닿지 않고(track 요청의 same-origin 대체) 요소는 <code>error</code> 를
     * 받는다. 속성이 있고 서버가 오리진을 허용하면 파일을 가져와 로드하고, 속성이 있어도 서버가 허용하지 않으면 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFileOfAnotherOrigin() throws Exception {
        final TestHttpServer page = new TestHttpServer();
        final TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel(new FakeOutputs());
        try {
            other.routeFixed("/open.vtt", new TestHttpServer.Response(200).text("text/vtt", VTT).header("Access-Control-Allow-Origin", "*"));
            other.routeFixed("/closed.vtt", new TestHttpServer.Response(200).text("text/vtt", VTT));
            other.start();
            String script = "<script>['t1', 't2', 't3'].forEach(function (id) { var el = document.getElementById(id); "
                    + "el.addEventListener('load', function () { mark(id + ':load:' + el.track.cues.length); }); el.addEventListener('error', function () { mark(id + ':error:' + el.readyState); }); "
                    + "el.track.mode = 'hidden'; });</script>";
            String html = "<!doctype html><html><body>" + MARK + "<audio id=a1><track id=t1 src='" + other.url("/open.vtt") + "'></audio>"
                    + "<audio id=a2 crossorigin><track id=t2 src='" + other.url("/open.vtt") + "'></audio>"
                    + "<audio id=a3 crossorigin><track id=t3 src='" + other.url("/closed.vtt") + "'></audio>" + script + "</body></html>";
            page.routeFixed("/page", TestHttpServer.Response.html(html));
            page.start();
            panel.setPage(page.url("/page"));
            awaitPage(panel, page.url("/page"));
            awaitOrder(panel, "t1:error:3,", "a file of another origin without crossorigin fails");
            awaitOrder(panel, "t2:load:2,", "the file that the server allows is loaded");
            awaitOrder(panel, "t3:error:3,", "the file that the server does not allow fails");
            Check.equal(1, other.count("/open.vtt"), "only the track of the audio with crossorigin asked for open.vtt: the refused request never reached the server");
            Check.equal(1, other.count("/closed.vtt"), "closed.vtt was asked for once (the response was refused afterwards)");
            Check.contains(other.last("/open.vtt").header("Origin"), "http://", "the CORS request carries the Origin header");
        } finally {
            panel.dispose();
            page.stop();
            other.stop();
        }
    }
}
