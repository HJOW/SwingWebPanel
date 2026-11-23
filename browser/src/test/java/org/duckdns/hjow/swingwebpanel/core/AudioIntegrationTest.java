package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.playback.ScriptedDecoder;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The <code>audio</code> element through the real component and real HTTP (plan items P18-05-c, P18-08, P18-09, P18-10): the resource is
 * fetched with the common request layer (user agent, extra headers, cookies), decoded and played block by block, the events reach the page in
 * the order of the Recommendation, and a navigation, <code>clean()</code> and <code>dispose()</code> close the sound output. The component
 * is a subclass that overrides {@link SwingWebPanel#createAudioOutputFactory()} with a fake output ({@link FakeOutputs}), so the PCM that
 * would be played is recorded exactly. This is an automated check with a fake output: nothing is heard, and no real sound device is used.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 를 통한 <code>audio</code> 요소(계획 P18-05-c, P18-08, P18-09, P18-10): 리소스를 공통 요청 계층(User-Agent, 추가 헤더,
 * 쿠키)으로 가져와 블록 단위로 디코딩하고 재생하며, 이벤트는 권고안의 순서로 페이지에 도달하고, 이동과 <code>clean()</code>, <code>dispose()</code> 는
 * 소리 출력을 닫는다. 컴포넌트는 {@link SwingWebPanel#createAudioOutputFactory()} 를 가짜 출력({@link FakeOutputs})으로 재정의한 하위 클래스이므로
 * 재생되었을 PCM 이 정확히 기록된다. 가짜 출력을 쓰는 자동 검증이며 아무 소리도 들리지 않고 실제 소리 장치는 쓰지 않는다.
 */
public class AudioIntegrationTest {

    /**
     * The scripts of the test pages: <code>mark(name)</code> appends a name to the order that is stored in the sessionStorage of the component;
     * every media event of the element <code>a</code> is marked.
     * <p>
     * 시험 페이지의 스크립트: <code>mark(name)</code> 은 컴포넌트의 sessionStorage 에 저장된 순서에 이름을 덧붙인다. 요소 <code>a</code> 의 모든 미디어
     * 이벤트를 기록한다.
     */
    private static final String SCRIPT = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}"
            + "['loadstart','durationchange','loadedmetadata','loadeddata','canplay','playing','canplaythrough','play','pause','ended','error','suspend','progress']"
            + ".forEach(function(n){document.addEventListener(n,function(e){mark(e.target.id+'.'+n);},true);});"
            + "window.addEventListener('load',function(){mark('win');});</script>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AudioIntegrationTest() {
    }

    /**
     * Creates the component on the EDT with a fake sound output.
     * <p>
     * EDT 에서 가짜 소리 출력을 쓰는 컴포넌트를 만든다.
     *
     * @param outs the outputs, or null for a component without sound / 출력. 소리 없는 컴포넌트이면 null
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
     * Waits until an output was closed.
     * <p>
     * 출력이 닫힐 때까지 기다린다.
     *
     * @param o the output / 출력
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

    /**
     * An <code>audio</code> element with <code>autoplay</code>: one HTTP request that carries the user agent, the extra headers and the
     * cookies of the component, the events in the order of the Recommendation, the whole PCM at the output, the end, and the received source
     * of the page unchanged.
     * <p>
     * <code>autoplay</code> 가 있는 <code>audio</code> 요소: 컴포넌트의 User-Agent, 추가 헤더, 쿠키를 실은 HTTP 요청 하나, 권고안 순서의 이벤트, 출력에
     * 도달한 전체 PCM, 끝, 변경되지 않은 페이지의 수신 원문.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFetchedWithCommonRequestLayerAndPlayed() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            panel.setUserAgent("audio-test/1.0");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Audio-Test", "yes");
            panel.addHeaders(headers);
            String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=a src='/clip.wav' autoplay></audio></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html).header("Set-Cookie", "sid=abc123; Path=/"));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(6000)));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "a.ended,", "the audio played to the end");
            awaitOrder(panel, "win,", "the window load event");
            Check.equal(1, server.count("/clip.wav"), "one request for the audio");
            TestHttpServer.Request request = server.last("/clip.wav");
            Check.equal("audio-test/1.0", request.header("User-Agent"), "the user agent of the component");
            Check.equal("yes", request.header("X-Audio-Test"), "the extra header");
            Check.contains(request.header("Cookie"), "sid=abc123", "the cookie of the component");
            String order = order(panel);
            int loadstart = order.indexOf("a.loadstart,");
            int metadata = order.indexOf("a.loadedmetadata,");
            int canplay = order.indexOf("a.canplay,");
            int play = order.indexOf("a.play,");
            int playing = order.indexOf("a.playing,");
            int through = order.indexOf("a.canplaythrough,");
            int pause = order.indexOf("a.pause,");
            int ended = order.indexOf("a.ended,");
            Check.isTrue(0 <= loadstart && loadstart < metadata && metadata < canplay && canplay < play && play < playing && playing < through
                    && through < pause && pause < ended, "the order of the events: " + order);
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 6000), outs.await(0).written()), "the whole PCM reached the output");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Resources that cannot be played (a missing file and bytes that are no audio) fire <code>error</code> with
     * <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> and do not hold the window <code>load</code> event back for ever; a <code>video</code> element
     * never requests its resource and reports the unsupported result.
     * <p>
     * 재생할 수 없는 리소스(없는 파일과 오디오가 아닌 바이트)는 <code>MEDIA_ERR_SRC_NOT_SUPPORTED</code> 와 함께 <code>error</code> 를 발생시키고 window
     * <code>load</code> 이벤트를 영원히 붙잡지 않는다. <code>video</code> 요소는 리소스를 요청하지 않고 미지원 결과를 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailuresAndVideo() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        SwingWebPanel panel = newPanel(outs);
        try {
            server.routeFixed("/missing.wav", new TestHttpServer.Response(404).text("text/plain", "no"));
            server.routeFixed("/junk.wav", new TestHttpServer.Response(200).bytes("audio/wav", "this is not audio, whatever the type says".getBytes("US-ASCII")));
            server.routeFixed("/movie.mp4", new TestHttpServer.Response(200).bytes("video/mp4", new byte[100]));
            String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=a src='/missing.wav'></audio><audio id=b src='/junk.wav'></audio>"
                    + "<video id=v src='/movie.mp4'></video>"
                    + "<script>window.addEventListener('load',function(){mark('code:'+document.getElementById('a').error.code+document.getElementById('b').error.code"
                    + "+document.getElementById('v').error.code);});</script></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "code:444,", "the load event came after the failures and every error code is 4");
            String order = order(panel);
            Check.isTrue(order.indexOf("a.error,") >= 0 && order.indexOf("b.error,") >= 0 && order.indexOf("v.error,") >= 0, "an error event for each: " + order);
            Check.equal(0, server.count("/movie.mp4"), "a video resource is never requested");
            Check.equal(0, outs.outputs.size(), "no sound output was opened");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A machine without a sound device (the output factory fails): <code>autoplay</code> cannot play, so the element reports an
     * <code>error</code> (<code>MEDIA_ERR_DECODE</code>), pauses again, and the page keeps working.
     * <p>
     * 소리 장치가 없는 컴퓨터(출력 팩터리가 실패함): <code>autoplay</code> 는 재생할 수 없으므로 요소는 <code>error</code>(<code>MEDIA_ERR_DECODE</code>)를
     * 알리고 다시 일시정지가 되며, 페이지는 계속 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoSoundDevice() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs();
        outs.failure = new MediaException(MediaException.Kind.ACCESS_FAILED, "no sound device here");
        SwingWebPanel panel = newPanel(outs);
        try {
            String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=a src='/clip.wav' autoplay></audio>"
                    + "<script>document.getElementById('a').addEventListener('error',function(){var a=document.getElementById('a');"
                    + "mark('code:'+a.error.code+':'+a.paused+':'+(a.error.message.indexOf('no sound device') >= 0));});</script></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(3000)));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            // HTML5 4.7.10.5 의 치명적 디코딩 오류: 오류 코드 3, 요소는 일시정지되지 않는다(paused 는 autoplay 의 false 그대로).
            awaitOrder(panel, "code:3:false:true,", "the failure is reported to the page");
            Check.equal(1, outs.openCalls, "the device was tried once");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A navigation, <code>clean()</code> and <code>dispose()</code> stop the sound: the output of the playing element is closed (also
     * when the audio was still waiting for the device buffer) and nothing of the old page is reported afterwards.
     * <p>
     * 이동, <code>clean()</code>, <code>dispose()</code> 는 소리를 멈춘다: 재생 중인 요소의 출력이 닫히고(오디오가 아직 장치 버퍼를 기다리는 중이어도)
     * 그 뒤에는 이전 페이지의 것이 보고되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationCleanAndDisposeCloseTheOutput() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        String html = "<!doctype html><html><body>" + SCRIPT + "<audio id=a src='/long.wav' autoplay></audio></body></html>";
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.routeFixed("/page2", TestHttpServer.Response.html("<!doctype html><html><body>second</body></html>"));
        server.routeFixed("/long.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(80000)));
        server.start();
        try {
            // 이동, clean, dispose 세 가지 방법으로 재생 중인 페이지를 떠난다.
            for (int way = 0; way < 3; way++) {
                FakeOutputs outs = new FakeOutputs(400);
                SwingWebPanel panel = newPanel(outs);
                try {
                    panel.setPage(server.url("/page"));
                    awaitPage(panel, server.url("/page"));
                    awaitOrder(panel, "a.playing,", "playing (way " + way + ")");
                    FakeOutputs.Out o = outs.await(0);
                    o.awaitQueued(400);
                    if (way == 0) {
                        panel.setPage(server.url("/page2"));
                        awaitPage(panel, server.url("/page2"));
                    } else if (way == 1) {
                        panel.clean();
                    } else {
                        panel.dispose();
                    }
                    awaitClosed(o, "the output was closed (way " + way + ")");
                    int written = o.written().length;
                    Thread.sleep(80);
                    Check.equal(written, o.written().length, "nothing is written after the page was left (way " + way + ")");
                    Check.equal(1, o.closeCalls(), "the output was closed once (way " + way + ")");
                } finally {
                    panel.dispose();
                }
            }
        } finally {
            server.stop();
        }
    }

    /**
     * The media requests of the page resources: the bytes and the MIME type of a resource arrive once, a failing status is reported as a
     * failure, a cancelled request reports nothing, and the states that the media elements pass on (playable, failed) are what the renderer
     * reads with {@link PageResources#getMedia(String)} (pending while nothing is known); a closed page keeps its states out.
     * <p>
     * 페이지 리소스의 미디어 요청: 리소스의 바이트와 MIME 타입은 한 번 도착하고, 실패 상태는 실패로 알리며, 취소한 요청은 아무것도 알리지 않는다.
     * 미디어 요소가 전달하는 상태(재생 가능, 실패)는 렌더러가 {@link PageResources#getMedia(String)} 로 읽는 것이며(아무것도 모르면 진행 중), 닫힌 페이지는
     * 상태를 받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageResourcesMediaRequestsAndStates() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        final AtomicInteger changes = new AtomicInteger();
        PageResources resources = null;
        try {
            server.routeFixed("/ok.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(100)));
            server.routeFixed("/gone.wav", new TestHttpServer.Response(404).text("text/plain", "gone"));
            final CountDownLatch release = new CountDownLatch(1);
            server.route("/slow.wav", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the audio / 오디오
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(30, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("audio/wav", wav(10));
                }
            });
            server.start();
            resources = new PageResources(new ResourceFetcher(null, FetchLimits.DEFAULT, null), new CookieStore(),
                    PageSnapshot.loaded(server.url("/page"), "", "text/html", server.url("/page"), 200), new PageRequestContext(null), workers,
                    new PageResources.Listener() {
                        /**
                         * Counts the completions.
                         * <p>
                         * 완료를 센다.
                         *
                         * @param stylesheet stylesheet flag / 시트 표시
                         */
                        public void resourceChanged(boolean stylesheet) {
                            changes.incrementAndGet();
                        }
                    });
            final byte[][] got = new byte[3][];
            final String[] mimes = new String[3];
            final String[] errors = new String[3];
            final CountDownLatch done = new CountDownLatch(2);
            resources.requestMedia(server.url("/ok.wav"), new MediaRequestSource.Callback() {
                /**
                 * Records the result.
                 * <p>
                 * 결과를 기록한다.
                 *
                 * @param data     the bytes / 바이트
                 * @param mimeType the MIME type / MIME 타입
                 * @param error    the failure / 실패 원인
                 */
                public void done(byte[] data, String mimeType, String error) {
                    got[0] = data;
                    mimes[0] = mimeType;
                    errors[0] = error;
                    done.countDown();
                }
            });
            resources.requestMedia(server.url("/gone.wav"), new MediaRequestSource.Callback() {
                /**
                 * Records the result.
                 * <p>
                 * 결과를 기록한다.
                 *
                 * @param data     the bytes / 바이트
                 * @param mimeType the MIME type / MIME 타입
                 * @param error    the failure / 실패 원인
                 */
                public void done(byte[] data, String mimeType, String error) {
                    got[1] = data;
                    errors[1] = error;
                    done.countDown();
                }
            });
            MediaRequestSource.Request slow = resources.requestMedia(server.url("/slow.wav"), new MediaRequestSource.Callback() {
                /**
                 * Records a result that must not come.
                 * <p>
                 * 오면 안 되는 결과를 기록한다.
                 *
                 * @param data     the bytes / 바이트
                 * @param mimeType the MIME type / MIME 타입
                 * @param error    the failure / 실패 원인
                 */
                public void done(byte[] data, String mimeType, String error) {
                    errors[2] = "called";
                }
            });
            Check.isTrue(done.await(20, TimeUnit.SECONDS), "both requests ended");
            Check.isTrue(Arrays.equals(wav(100), got[0]), "the bytes of the resource");
            Check.equal("audio/wav", mimes[0], "the MIME type of the resource");
            Check.isNull(errors[0], "no error");
            Check.isNull(got[1], "no bytes of a failed request");
            Check.contains(errors[1], "404", "the status is in the failure");
            slow.cancel();
            release.countDown();
            Thread.sleep(150);
            Check.isNull(errors[2], "a cancelled request reports nothing");
            Check.equal(ImageResource.State.PENDING, resources.getMedia("http://example.test/unknown.wav").getState(), "unknown is pending");
            int before = changes.get();
            resources.mediaState("http://example.test/a.wav", null);
            resources.mediaState("http://example.test/b.wav", "unsupported format");
            Check.equal(ImageResource.State.LOADED, resources.getMedia("http://example.test/a.wav").getState(), "playable");
            Check.equal(ImageResource.State.FAILED, resources.getMedia("http://example.test/b.wav").getState(), "failed");
            Check.equal("unsupported format", resources.getMedia("http://example.test/b.wav").getMessage(), "the reason");
            Check.equal(before + 2, changes.get(), "the page is drawn again for each state");
            resources.close();
            resources.mediaState("http://example.test/c.wav", null);
            Check.equal(ImageResource.State.PENDING, resources.getMedia("http://example.test/c.wav").getState(), "a closed page keeps nothing");
        } finally {
            if (resources != null) {
                resources.close();
            }
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
            server.stop();
        }
    }

    /**
     * Paints the component into an image on the EDT and returns a signature of the pixels of the top left area.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그리고 왼쪽 위 영역 픽셀의 서명을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the signature / 서명
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static long paintSignature(final SwingWebPanel panel) throws Exception {
        final long[] sig = new long[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints and hashes the pixels.
             * <p>
             * 그리고 픽셀을 해시한다.
             */
            public void run() {
                BufferedImage img = new BufferedImage(300, 90, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, 300, 90);
                panel.paint(g);
                g.dispose();
                long h = 1125899906842597L;
                // 픽셀을 하나씩 섞어 서명을 만든다.
                for (int y = 0; y < 90; y++) {
                    for (int x = 0; x < 300; x++) {
                        h = 31 * h + img.getRGB(x, y);
                    }
                }
                sig[0] = h;
            }
        });
        return sig[0];
    }

    /**
     * The state that the media element found out reaches the renderer: a playable audio resource and one that cannot be fetched are drawn
     * differently (the control bar, or the unsupported URL with the alternative text), also when the failure comes after the first drawing.
     * The check compares painted pixels of two real components, so it shows the flow from the element to the screen, not the exact look.
     * <p>
     * 미디어 요소가 알아낸 상태가 렌더러에 도달한다: 재생할 수 있는 오디오 리소스와 가져올 수 없는 리소스는 다르게 그려진다(컨트롤 막대, 또는 미지원
     * URL 과 대체 텍스트). 첫 그림 뒤에 실패가 와도 그렇다. 이 시험은 실제 컴포넌트 둘의 그려진 픽셀을 비교하므로 요소에서 화면까지의 흐름을 보여 줄 뿐
     * 정확한 모양을 확인하지는 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRendererSeesThePlayableAndTheFailedAudio() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch release = new CountDownLatch(1);
        SwingWebPanel good = newPanel(new FakeOutputs());
        SwingWebPanel bad = newPanel(new FakeOutputs());
        SwingWebPanel waiting = newPanel(new FakeOutputs());
        try {
            server.routeFixed("/ok.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav(1000)));
            server.routeFixed("/gone.wav", new TestHttpServer.Response(404).text("text/plain", "gone"));
            server.route("/slow.wav", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the audio / 오디오
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(60, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(404).text("text/plain", "late");
                }
            });
            String head = "<!doctype html><html><body style='margin:0'>" + SCRIPT;
            server.routeFixed("/good", TestHttpServer.Response.html(head + "<audio id=a controls alt='sound' src='/ok.wav'></audio></body></html>"));
            server.routeFixed("/bad", TestHttpServer.Response.html(head + "<audio id=a controls alt='sound' src='/gone.wav'></audio></body></html>"));
            server.routeFixed("/waiting", TestHttpServer.Response.html(head + "<audio id=a controls alt='sound' src='/slow.wav'></audio></body></html>"));
            server.start();
            good.setPage(server.url("/good"));
            bad.setPage(server.url("/bad"));
            waiting.setPage(server.url("/waiting"));
            awaitPage(good, server.url("/good"));
            awaitPage(bad, server.url("/bad"));
            awaitPage(waiting, server.url("/waiting"));
            awaitOrder(good, "a.canplaythrough,", "the playable audio loaded");
            awaitOrder(bad, "a.error,", "the failing audio reported its error");
            awaitOrder(waiting, "a.loadstart,", "the waiting audio started to load");
            long end = System.currentTimeMillis() + 30000;
            long signatureGood = paintSignature(good);
            long signatureBad = paintSignature(bad);
            long signatureWaiting = paintSignature(waiting);
            // 실패한 오디오의 대체 그림이 그려지고 재생할 수 있는 오디오가 기다리는 오디오와 같은 컨트롤 막대가 될 때까지 다시 그려 본다.
            while ((signatureGood == signatureBad || signatureGood == signatureWaiting || signatureWaiting == signatureBad)
                    && System.currentTimeMillis() < end) {
                Thread.sleep(50);
                signatureGood = paintSignature(good);
                signatureBad = paintSignature(bad);
                signatureWaiting = paintSignature(waiting);
            }
            Check.isTrue(signatureGood != signatureBad, "the failed audio is drawn differently from the playable audio");
            Check.isTrue(signatureWaiting != signatureBad, "an audio that is still loading shows its control bar, not the failure");
            Check.isTrue(signatureGood != signatureWaiting, "the control bar of a loaded audio shows the handle of the seek bar that a loading one lacks");
        } finally {
            release.countDown();
            good.dispose();
            bad.dispose();
            waiting.dispose();
            server.stop();
        }
    }
}
