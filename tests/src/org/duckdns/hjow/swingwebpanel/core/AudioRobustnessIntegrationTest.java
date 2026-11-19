package org.duckdns.hjow.swingwebpanel.core;

import java.net.ServerSocket;
import java.util.Arrays;
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
 * Audio in situations that go wrong or overlap (plan item P18-10; FEATURE_MATRIX AUD-10), through the real component and real HTTP with a
 * fake sound output: a resource over the size limit, a connection that is refused, a response that is cut off, a WAV header that lies
 * about the size of its data, an audio file that is a page full of text, a page that goes away while a resource is still on its way, and
 * several components and several elements that play at the same time. The checks show that every failure ends as an <code>error</code>
 * event with a code, that the page still loads (the window <code>load</code> event comes), that nothing keeps running afterwards, and
 * that playing components do not disturb each other. Nothing is heard: the output is a fake one (the real device is checked in
 * <code>JavaSoundDeviceTest</code>, with silence).
 * <p>
 * 잘못되거나 겹치는 상황의 오디오(계획 P18-10; FEATURE_MATRIX AUD-10)를 실제 컴포넌트와 실제 HTTP, 가짜 소리 출력으로 확인한다: 크기 제한을 넘는 리소스,
 * 거절된 연결, 중간에 끊긴 응답, 데이터 크기를 속이는 WAV 헤더, 글만 가득한 오디오 파일, 리소스가 오는 도중에 사라지는 페이지, 동시에 재생하는 여러
 * 컴포넌트와 여러 요소. 모든 실패는 코드가 있는 <code>error</code> 이벤트로 끝나고, 페이지는 여전히 로드되며(window <code>load</code> 이벤트가 온다),
 * 그 뒤에 계속 실행되는 것이 없고, 재생 중인 컴포넌트들이 서로 방해하지 않는다는 것을 보여 준다. 아무 소리도 들리지 않는다. 출력은 가짜이다(실제 장치는
 * <code>JavaSoundDeviceTest</code> 가 무음으로 확인한다).
 */
public class AudioRobustnessIntegrationTest {

    /**
     * The script of the test pages: marks the events of every audio element (named by its id) in the sessionStorage of the component.
     * <p>
     * 시험 페이지의 스크립트: 모든 오디오 요소의 이벤트를 (id 로 이름 붙여) 컴포넌트의 sessionStorage 에 기록한다.
     */
    private static final String SCRIPT = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}"
            + "['canplaythrough','play','playing','pause','ended','error','loadstart','emptied','abort'].forEach(function(n){"
            + "document.addEventListener(n,function(e){mark(e.target.id+'.'+n);},true);});"
            + "window.addEventListener('load',function(){var r=['win'];var l=document.getElementsByTagName('audio');"
            + "for(var i=0;i<l.length;i++){r.push(l[i].id+'='+(l[i].error?l[i].error.code:'ok'));}mark(r.join('/'));});</script>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AudioRobustnessIntegrationTest() {
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
     * @return the order / 순서
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
     * Waits until the stored order contains a text.
     * <p>
     * 저장된 순서에 어떤 문자열이 들어갈 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param part    the text / 문자열
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitOrder(SwingWebPanel panel, String part, String message) throws Exception {
        long end = System.currentTimeMillis() + 40000;
        // 스크립트가 기록할 때까지 짧게 쉬며 확인한다.
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
        long end = System.currentTimeMillis() + 40000;
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
     * Finds a TCP port that nobody listens on.
     * <p>
     * 아무도 듣지 않는 TCP 포트를 찾는다.
     *
     * @return the port / 포트
     * @throws Exception if no socket can be opened / 소켓을 열 수 없으면
     */
    private static int closedPort() throws Exception {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

    /**
     * Resources that cannot be fetched or used end as an <code>error</code> with the code of an unsupported source and never hold the
     * window <code>load</code> event back: one over the size limit of a fetch (17 MiB), one that nobody serves (connection refused), one that
     * is cut off in the middle of the body, a text page named as audio, and a file of zero length. The component stays usable: a good
     * audio on the same page loads, and the received source of the page is unchanged.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenResourcesAllEndAsErrors() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            int refused = closedPort();
            byte[] huge = new byte[17 * 1024 * 1024];
            Arrays.fill(huge, (byte) 0x20);
            server.routeFixed("/huge.wav", new TestHttpServer.Response(200).bytes("audio/wav", huge));
            server.routeFixed("/cut.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(20000)).truncateAt(3000));
            server.routeFixed("/text.wav", new TestHttpServer.Response(200).bytes("audio/wav", "<html><body>this page is not audio</body></html>".getBytes("US-ASCII")));
            server.routeFixed("/empty.wav", new TestHttpServer.Response(200).bytes("audio/wav", new byte[0]));
            server.routeFixed("/good.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(2000)));
            String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=huge src='/huge.wav'></audio><audio id=gone src='http://127.0.0.1:" + refused
                    + "/x.wav'></audio><audio id=cut src='/cut.wav'></audio><audio id=text src='/text.wav'></audio><audio id=empty src='/empty.wav'></audio>"
                    + "<audio id=good src='/good.wav'></audio></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "win/", "the window load event came although resources failed");
            String o = order(panel);
            Check.contains(o, "huge=4", "a resource over the size limit is an unsupported source: " + o);
            Check.contains(o, "gone=4", "a refused connection is an unsupported source: " + o);
            Check.contains(o, "cut=4", "a cut-off response is an unsupported source: " + o);
            Check.contains(o, "text=4", "text that is no audio is an unsupported source: " + o);
            Check.contains(o, "empty=4", "an empty file is an unsupported source: " + o);
            Check.contains(o, "good=ok", "the good audio of the same page loaded: " + o);
            long end = System.currentTimeMillis() + 15000;
            // 진행률이 확정 단계를 마치고 100 이 될 때까지 짧게 쉬며 확인한다.
            while (panel.getProgress() != 100 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.equal(100, panel.getProgress(), "the page finished loading");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
            Check.equal(0, outs.outputs.size(), "nothing was played, so no output was opened");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A WAV header that declares a data chunk of 2 GiB while the file has 400 frames: the player plays what exists, to the end, without
     * trying to allocate what the header promised. The sound that reaches the output is the real data.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeaderThatLiesAboutTheSize() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            byte[] file = wav(400);
            // 표준 44바이트 헤더에서 data 청크 크기(40~43바이트)를 2 GiB 에 가까운 값으로 바꾼다.
            file[40] = (byte) 0xF0;
            file[41] = (byte) 0xFF;
            file[42] = (byte) 0xFF;
            file[43] = (byte) 0x7F;
            server.routeFixed("/lie.wav", new TestHttpServer.Response(200).bytes("audio/wav", file));
            String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=a src='/lie.wav' autoplay></audio></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "a.ended,", "the audio with the lying header played to its real end");
            Check.equal(800, outs.await(0).written().length, "exactly the 400 frames that exist reached the output");
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 400), outs.await(0).written()), "the sound is the real data");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page that is replaced while a resource is still on its way: the late response does not play, the old element does not open an
     * output, and the new page works. The old request is cancelled, so the server sees its connection end.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageReplacedWhileTheResourceIsOnItsWay() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            server.routeFixed("/slow.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(3000)).delay(1500));
            server.routeFixed("/old", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT + "<audio id=old src='/slow.wav' autoplay></audio></body></html>"));
            server.routeFixed("/new", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT + "<p>new page</p></body></html>"));
            server.start();
            panel.setPage(server.url("/old"));
            awaitPage(panel, server.url("/old"));
            awaitOrder(panel, "old.loadstart,", "the old audio started to load");
            panel.setPage(server.url("/new"));
            awaitPage(panel, server.url("/new"));
            // 늦은 응답이 도착할 시간을 충분히 준다.
            Thread.sleep(2500);
            Check.equal(0, outs.outputs.size(), "the late response of the old page did not open an output");
            Check.equal(-1, order(panel).indexOf("old.play,"), "the old element did not play: " + order(panel));
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Two components play at the same time, each with its own sound output: both outputs receive exactly their own sound, pausing one does
     * not stop the other, and disposing one closes only its output while the other plays to its end.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoComponentsPlayAtTheSameTime() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outsA = new FakeOutputs(1000);
        FakeOutputs outsB = new FakeOutputs(1000);
        SwingWebPanel a = newPanel(outsA);
        SwingWebPanel b = newPanel(outsB);
        try {
            byte[] pcmA = ScriptedDecoder.pcm(0, 6000);
            byte[] pcmB = ScriptedDecoder.pcm(500, 6000);
            server.routeFixed("/a.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, pcmA, -1)));
            server.routeFixed("/b.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, pcmB, -1)));
            server.routeFixed("/pa", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT + "<audio id=x src='/a.wav' autoplay></audio></body></html>"));
            server.routeFixed("/pb", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT + "<audio id=x src='/b.wav' autoplay></audio></body></html>"));
            server.start();
            a.setPage(server.url("/pa"));
            b.setPage(server.url("/pb"));
            awaitOrder(a, "x.playing,", "the first component plays");
            awaitOrder(b, "x.playing,", "the second component plays");
            FakeOutputs.Out oa = outsA.await(0);
            FakeOutputs.Out ob = outsB.await(0);
            Check.isTrue(oa != ob, "the components have their own outputs");
            // 두 출력을 번갈아 재생시키며 한쪽을 일시정지하고 다른 쪽이 계속 가는지 본다.
            long end = System.currentTimeMillis() + 30000;
            while ((oa.written().length < 3000 * 2 || ob.written().length < 3000 * 2) && System.currentTimeMillis() < end) {
                oa.consume(300);
                ob.consume(300);
                Thread.sleep(2);
            }
            Check.isTrue(oa.written().length >= 3000 * 2 && ob.written().length >= 3000 * 2, "both components kept playing");
            a.dispose();
            awaitClosed(oa, "the disposed component closed its output");
            Check.equal(0, ob.closeCalls(), "the other output is still open");
            end = System.currentTimeMillis() + 30000;
            // 남은 컴포넌트가 끝까지 재생하도록 출력을 계속 재생시킨다.
            while (order(b).indexOf("x.ended,") < 0 && System.currentTimeMillis() < end) {
                ob.consume(500);
                Thread.sleep(2);
            }
            Check.isTrue(order(b).indexOf("x.ended,") >= 0, "the remaining component played to its end: " + order(b));
            Check.isTrue(Arrays.equals(pcmB, ob.written()), "the second component got exactly its own sound");
            byte[] gotA = oa.written();
            Check.isTrue(Arrays.equals(Arrays.copyOf(pcmA, gotA.length), gotA), "the first component got only its own sound, from the start");
        } finally {
            a.dispose();
            b.dispose();
            server.stop();
        }
    }

    /**
     * Two audio elements of one page play at once: two outputs are open, both play to their ends, each with its own sound, and both
     * report their events.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoElementsOfOnePagePlayTogether() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            byte[] pcmOne = ScriptedDecoder.pcm(0, 3000);
            byte[] pcmTwo = ScriptedDecoder.pcm(900, 5000);
            server.routeFixed("/one.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, pcmOne, -1)));
            server.routeFixed("/two.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, pcmTwo, -1)));
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT
                    + "<audio id=one src='/one.wav' autoplay></audio><audio id=two src='/two.wav' autoplay></audio></body></html>"));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "one.ended,", "the first element played to its end");
            awaitOrder(panel, "two.ended,", "the second element played to its end");
            Check.equal(2, outs.outputs.size(), "one output per playing element");
            byte[] w0 = outs.await(0).written();
            byte[] w1 = outs.await(1).written();
            boolean straight = Arrays.equals(pcmOne, w0) && Arrays.equals(pcmTwo, w1);
            boolean crossed = Arrays.equals(pcmOne, w1) && Arrays.equals(pcmTwo, w0);
            Check.isTrue(straight || crossed, "each element got exactly its own sound");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Waits until an output was closed.
     * <p>
     * 출력이 닫힐 때까지 기다린다.
     *
     * @param o       the output / 출력
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitClosed(FakeOutputs.Out o, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 출력이 닫힐 때까지 짧게 쉬며 확인한다.
        while (o.closeCalls() == 0 && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(1, o.closeCalls(), message);
    }
}
