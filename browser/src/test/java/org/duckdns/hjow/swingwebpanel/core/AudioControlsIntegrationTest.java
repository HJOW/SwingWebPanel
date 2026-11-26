package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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
 * The control bar of an <code>audio</code> element with <code>controls</code> in the real component, driven by real AWT mouse events
 * (plan item P18-08; FEATURE_MATRIX AUD-10): a click on the play button plays and pauses, a click on the seek bar seeks, a click on the
 * mute button mutes and unmutes, a click and a drag on the volume slider set the volume, a click on the time label does nothing, the DOM
 * <code>click</code> still reaches the page, and the painted bar follows the state (pause symbol, position). The sound goes to a fake output
 * that renders only when the test says so, so the state of the element is exact. This is an automated check with synthesized AWT events and
 * a fake output: a person with a mouse and a sound device has not tried it (P22-10).
 * <p>
 * <code>controls</code> 가 있는 <code>audio</code> 요소의 컨트롤 막대를 실제 컴포넌트에서 실제 AWT 마우스 이벤트로 조작한다(계획 P18-08;
 * FEATURE_MATRIX AUD-10): 재생 버튼을 누르면 재생하고 일시정지하며, 탐색 막대를 누르면 탐색하고, 음소거 버튼을 누르면 소리를 껐다 켜며, 음량
 * 슬라이더를 누르고 끌면 음량이 정해지고, 시간 표시를 눌러도 아무 일도 없으며, DOM <code>click</code> 은 여전히 페이지에 도달하고, 그려진 막대는
 * 상태(일시정지 기호, 위치)를 따른다. 소리는 시험이 지시할 때만 재생하는 가짜 출력으로 가므로 요소의 상태가 정확하다. 합성한 AWT 이벤트와 가짜
 * 출력을 쓰는 자동 검증이며 마우스와 소리 장치를 가진 사람이 해 보지는 않았다(P22-10).
 */
public class AudioControlsIntegrationTest {

    /**
     * The scripts of the test page: every event of the audio element is marked in the sessionStorage of the component (the position at a
     * seek and the volume at a volume change are part of the mark).
     * <p>
     * 시험 페이지의 스크립트: 오디오 요소의 모든 이벤트를 컴포넌트의 sessionStorage 에 기록한다(탐색 때의 위치와 음량이 바뀔 때의 음량도 기록에 들어간다).
     */
    private static final String SCRIPT = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}"
            + "['canplaythrough','play','playing','pause','ended','error','seeking','seeked','volumechange','click'].forEach(function(n){"
            + "document.addEventListener(n,function(e){var t=e.target;"
            + "mark(n==='seeked'?'seeked@'+t.currentTime:n==='volumechange'?'vol@'+t.volume.toFixed(2)+(t.muted?'m':''):n);},true);});</script>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AudioControlsIntegrationTest() {
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
     * Counts the marks of a name in the stored order.
     * <p>
     * 저장된 순서에서 어떤 이름의 기록 수를 센다.
     *
     * @param panel the component / 컴포넌트
     * @param mark  the mark / 기록
     * @return the number of marks / 기록 수
     */
    private static int count(SwingWebPanel panel, String mark) {
        String order = "," + order(panel);
        int n = 0;
        int at = 0;
        // 쉼표로 구분된 기록에서 같은 이름이 나오는 횟수를 센다.
        while ((at = order.indexOf("," + mark + ",", at)) >= 0) {
            n++;
            at += 1;
        }
        return n;
    }

    /**
     * Waits until the stored order has at least a number of marks of a name.
     * <p>
     * 저장된 순서에 어떤 이름의 기록이 일정 수 이상이 될 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param mark    the mark / 기록
     * @param times   the number / 수
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel panel, String mark, int times, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 기록할 때까지 짧게 쉬며 확인한다.
        while (count(panel, mark) < times && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.isTrue(count(panel, mark) >= times, message + " (order: " + order(panel) + ")");
    }

    /**
     * Sends an AWT mouse event to the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트에 AWT 마우스 이벤트를 보낸다.
     *
     * @param panel the component / 컴포넌트
     * @param id    the event id / 이벤트 종류
     * @param x     the x / x
     * @param y     the y / y
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static void send(final SwingWebPanel panel, final int id, final int x, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the event as the system would.
             * <p>
             * 시스템이 하듯 이벤트를 보낸다.
             */
            public void run() {
                int modifiers = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED ? InputEvent.BUTTON1_DOWN_MASK : 0;
                panel.dispatchEvent(new MouseEvent(panel, id, System.currentTimeMillis(), modifiers, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Clicks with the main button: press, release, click.
     * <p>
     * 주 버튼으로 누른다: 누름, 뗌, 클릭.
     *
     * @param panel the component / 컴포넌트
     * @param x     the x / x
     * @param y     the y / y
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static void click(SwingWebPanel panel, int x, int y) throws Exception {
        send(panel, MouseEvent.MOUSE_PRESSED, x, y);
        send(panel, MouseEvent.MOUSE_RELEASED, x, y);
        send(panel, MouseEvent.MOUSE_CLICKED, x, y);
    }

    /**
     * Paints the component into an image on the EDT and returns a hash of the pixels of the top left area.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그리고 왼쪽 위 영역 픽셀의 해시를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the hash / 해시
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
                BufferedImage img = new BufferedImage(300, 60, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, 300, 60);
                panel.paint(g);
                g.dispose();
                long h = 1125899906842597L;
                // 픽셀을 하나씩 섞어 해시를 만든다.
                for (int y = 0; y < 60; y++) {
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
     * Waits until the painted bar differs from a hash: the renderer paints the new state after the element said that it changed.
     * <p>
     * 그려진 막대가 어떤 해시와 달라질 때까지 기다린다: 요소가 상태가 바뀌었다고 알린 뒤 렌더러가 새 상태를 그린다.
     *
     * @param panel   the component / 컴포넌트
     * @param old     the hash that must change / 바뀌어야 하는 해시
     * @param message the message of a failure / 실패 메시지
     * @return the new hash / 새 해시
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static long awaitRepaint(SwingWebPanel panel, long old, String message) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        long now = paintSignature(panel);
        // 그림이 달라질 때까지 짧게 쉬며 다시 그려 본다.
        while (now == old && System.currentTimeMillis() < end) {
            Thread.sleep(30);
            now = paintSignature(panel);
        }
        Check.isTrue(now != old, message);
        return now;
    }

    /**
     * Waits until the painted bar stops changing (two equal pictures 150 ms apart).
     * <p>
     * 그려진 막대가 더 이상 바뀌지 않을 때까지(150 ms 간격의 두 그림이 같을 때까지) 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @return the hash / 해시
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static long stable(SwingWebPanel panel) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        long a = paintSignature(panel);
        // 두 번 연속 같은 그림이 나올 때까지 다시 그려 본다.
        while (System.currentTimeMillis() < end) {
            Thread.sleep(150);
            long b = paintSignature(panel);
            if (a == b) {
                return b;
            }
            a = b;
        }
        return a;
    }

    /**
     * Operates the bar with the mouse: play, pause, seek, mute, volume by click and by drag, a click on the time label, and the DOM
     * <code>click</code> events; the painted bar follows (pause symbol, moving position); the received source of the page is unchanged.
     * <p>
     * 마우스로 막대를 조작한다: 재생, 일시정지, 탐색, 음소거, 클릭과 끌기로 하는 음량, 시간 표시를 누르기, DOM <code>click</code> 이벤트. 그려진 막대가
     * 따라온다(일시정지 기호, 움직이는 위치). 페이지의 수신 원문은 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMouseOperatesTheControlBar() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        FakeOutputs outs = new FakeOutputs(2000);
        SwingWebPanel panel = newPanel(outs);
        try {
            String html = "<!doctype html><html><body style='margin:0'>" + SCRIPT
                    + "<audio id=a controls src='/clip.wav' style='display:block;width:300px;height:54px'></audio></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, 40000), -1)));
            server.start();
            panel.setPage(server.url("/page"));
            await(panel, "canplaythrough", 1, "the audio loaded");
            long loaded = stable(panel);
            // 재생 버튼(22, 27)을 누르면 재생이 시작되고 막대는 일시정지 기호로 바뀐다.
            click(panel, 22, 27);
            await(panel, "play", 1, "the play button plays");
            await(panel, "playing", 1, "the sound began");
            await(panel, "click", 1, "the DOM click event still reaches the page");
            long playing = awaitRepaint(panel, loaded, "the bar shows the pause symbol while playing");
            FakeOutputs.Out out = outs.await(0);
            // 소리를 조금 재생시키면 위치가 움직이고 시간 표시가 바뀐다.
            long shown = playing;
            long end = System.currentTimeMillis() + 20000;
            // 그림이 달라질 때까지 소리를 조금씩 재생시키며 확인한다.
            while (shown == playing && System.currentTimeMillis() < end) {
                out.consume(4000);
                Thread.sleep(60);
                shown = paintSignature(panel);
            }
            Check.isTrue(shown != playing, "the bar follows the position while playing");
            // 같은 버튼을 다시 누르면 일시정지한다.
            click(panel, 22, 27);
            await(panel, "pause", 1, "the play button pauses");
            // 탐색 막대의 한가운데(165, 27)를 누르면 5초 길이의 한가운데로 탐색한다.
            click(panel, 165, 27);
            await(panel, "seeked@2.5", 1, "the seek bar seeks to the middle");
            // 음소거 버튼(218, 27)을 누르면 소리를 끄고, 다시 누르면 켠다.
            click(panel, 218, 27);
            await(panel, "vol@1.00m", 1, "the mute button mutes");
            click(panel, 218, 27);
            await(panel, "vol@1.00", 1, "the mute button unmutes");
            // 음량 슬라이더의 한가운데(264, 27)를 누르면 음량이 절반이 된다.
            click(panel, 264, 27);
            await(panel, "vol@0.50", 1, "the volume slider sets the volume");
            // 슬라이더를 누른 채 끌면 음량이 따라 움직인다.
            send(panel, MouseEvent.MOUSE_PRESSED, 250, 27);
            await(panel, "vol@0.25", 1, "pressing the volume slider sets the volume");
            send(panel, MouseEvent.MOUSE_DRAGGED, 270, 40);
            await(panel, "vol@0.61", 1, "dragging moves the volume");
            send(panel, MouseEvent.MOUSE_DRAGGED, 400, 90);
            await(panel, "vol@1.00", 2, "dragging beyond the end gives the full volume");
            send(panel, MouseEvent.MOUSE_RELEASED, 400, 90);
            send(panel, MouseEvent.MOUSE_CLICKED, 400, 90);
            // 놓은 뒤의 끌기는 슬라이더를 움직이지 않는다.
            int volumeEvents = count(panel, "vol@1.00");
            send(panel, MouseEvent.MOUSE_DRAGGED, 240, 27);
            Thread.sleep(150);
            Check.equal(volumeEvents, count(panel, "vol@1.00"), "after the release the slider is not held any more");
            Check.equal(0, count(panel, "vol@0.07"), "no volume change after the release");
            // 시간 표시를 눌러도 재생 상태는 바뀌지 않는다.
            int plays = count(panel, "play");
            int pauses = count(panel, "pause");
            click(panel, 50, 27);
            Thread.sleep(150);
            Check.equal(plays, count(panel, "play"), "the time label does not play");
            Check.equal(pauses, count(panel, "pause"), "the time label does not pause");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Sends a complete AWT key press through the panel listeners on the EDT. / EDT에서 패널 리스너에 AWT 키 누름과 놓음을 보낸다.
     * @param panel the page component / 페이지 컴포넌트
     * @param code the virtual key code / 가상 키 코드
     * @param character the corresponding character or CHAR_UNDEFINED / 해당 문자 또는 CHAR_UNDEFINED
     * @throws Exception if the EDT cannot deliver the event / EDT에서 이벤트를 보낼 수 없으면
     */
    private static void pressKey(final SwingWebPanel panel, final int code, final char character) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches both events to the installed listeners. / 설치된 리스너에 두 이벤트를 전달한다. */
            public void run() {
                long when = System.currentTimeMillis();
                KeyEvent press = new KeyEvent(panel, KeyEvent.KEY_PRESSED, when, 0, code, character);
                KeyEvent release = new KeyEvent(panel, KeyEvent.KEY_RELEASED, when, 0, code, character);
                // 실제 패널에 설치된 키 입력 경로에 누름과 놓음을 차례로 보낸다.
                for (KeyListener listener : panel.getKeyListeners()) {
                    listener.keyPressed(press);
                    listener.keyReleased(release);
                }
            }
        });
    }

    /**
     * Tabs into and out of the controls and operates them through trusted AWT keys. / 신뢰 AWT 키로 컨트롤에 들어가 조작하고 Tab으로 나온다.
     * @throws Exception if the server or the input fails / 서버나 입력이 실패하면
     */
    public void testKeyboardOperatesFocusedAudioControls() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel(new FakeOutputs(2000));
        try {
            String html = "<!doctype html><style>:focus-visible{outline:solid 2px blue}</style>" + SCRIPT + "<audio id=plain preload=none></audio>"
                    + "<audio id=a controls src='/clip.wav'></audio><button id=after>After</button>"
                    + "<script>var audio=document.getElementById('a');document.addEventListener('keyup',function(e){if(e.key==='Tab')"
                    + "mark('focus@'+e.target.id+(getComputedStyle(e.target).getPropertyValue('outline-style')==='solid'?'-visible':''));});"
                    + "audio.addEventListener('keydown',function(e){if(e.key==='ArrowUp')e.preventDefault();});"
                    + "audio.addEventListener('keyup',function(e){if(e.key==='ArrowUp')mark('up@'+audio.volume.toFixed(2));});"
                    + "Promise.prototype.catch=function(){mark('script-catch');throw Error('author catch');};</script>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            byte[] wav = AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, 96000), -1);
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav));
            server.start();
            panel.setPage(server.url("/page"));
            await(panel, "canplaythrough", 1, "the audio source is ready");

            pressKey(panel, KeyEvent.VK_TAB, '\t');
            await(panel, "focus@a-visible", 1, "Tab reaches audio controls and enables the focus outline");
            pressKey(panel, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
            await(panel, "seeked@5", 1, "Right seeks five seconds forward");
            pressKey(panel, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
            await(panel, "seeked@0", 1, "Left returns to the start");
            pressKey(panel, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
            await(panel, "vol@0.90", 1, "Down lowers the volume");
            pressKey(panel, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
            await(panel, "up@0.90", 1, "a cancelled keydown leaves the volume unchanged at keyup");
            pressKey(panel, KeyEvent.VK_M, 'm');
            await(panel, "vol@0.90m", 1, "M mutes the audio");
            pressKey(panel, KeyEvent.VK_SPACE, ' ');
            await(panel, "playing", 1, "Space plays the audio");
            pressKey(panel, KeyEvent.VK_ENTER, '\n');
            await(panel, "pause", 1, "Enter pauses the audio");
            pressKey(panel, KeyEvent.VK_TAB, '\t');
            await(panel, "focus@after-visible", 1, "the next Tab moves off the audio");
            Check.equal(0, count(panel, "script-catch"), "audio controls do not call the author's Promise methods");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Waits for a top-level document and its required loads. / 최상위 문서와 필수 리소스의 로딩을 기다린다.
     * @param panel the component / 컴포넌트
     * @param url the expected page / 기대하는 페이지
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private static void awaitLoaded(SwingWebPanel panel, String url) throws InterruptedException {
        long end = System.currentTimeMillis() + 30000;
        // 상태가 확정되고 필수 로드가 끝날 때까지 상태를 읽는다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "the expected document is current");
        Check.equal(100, panel.getProgress(), "the required page resources finished");
    }

    /**
     * A script-disabled document plays deferred audio on request and releases its output on clean.
     * <p>
     * 스크립트를 끈 문서는 조작할 때 지연된 오디오를 재생하고 clean으로 출력 장치를 해제한다.
     * @throws Exception if setup or playback failed / 준비 또는 재생이 실패하면
     */
    public void testDeferredAudioPlaysWithJavaScriptDisabled() throws Exception {
        TestHttpServer server = new TestHttpServer();
        FakeOutputs outputs = new FakeOutputs(2000);
        SwingWebPanel panel = newPanel(outputs);
        try {
            String html = "<!doctype html><body style='margin:0'><script src='/author.js'></script>"
                    + "<audio controls preload=none src='/clip.wav' style='display:block;width:300px;height:54px'></audio>";
            byte[] pcm = ScriptedDecoder.pcm(0, 24000);
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", AudioKit.wav(1, 1, 8000, 16, pcm, -1)));
            server.start();
            panel.setJavascriptEnabled(false);
            panel.setPage(server.url("/page"));
            awaitLoaded(panel, server.url("/page"));
            Check.equal(0, server.count("/author.js"), "author scripts never load");
            Check.equal(0, server.count("/clip.wav"), "preload none waits for the user");

            click(panel, 22, 27);
            FakeOutputs.Out output = outputs.await(0);
            output.awaitWritten(64);
            Check.equal(1, server.count("/clip.wav"), "the control requests the audio");
            Check.isTrue(Arrays.equals(Arrays.copyOf(pcm, 64), Arrays.copyOf(output.written(), 64)), "the output receives the decoded PCM");
            Check.equal(html, panel.getContent(), "the received source is preserved");

            panel.clean();
            awaitLoaded(panel, "about:blank");
            Check.equal(1, output.closeCalls(), "clean closed the sound output");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Preloading and autoplay run without scripts, and a failed audio request ends the wait for the page.
     * <p>
     * 스크립트 없이 로딩과 자동 재생이 동작하며 오디오 요청 실패로도 페이지의 기다림이 끝난다.
     * @throws Exception if setup or playback failed / 준비 또는 재생이 실패하면
     */
    public void testAutoplayAndFailedAudioWithJavaScriptDisabled() throws Exception {
        TestHttpServer server = new TestHttpServer();
        FakeOutputs outputs = new FakeOutputs(2000);
        SwingWebPanel panel = newPanel(outputs);
        try {
            String html = "<!doctype html><audio controls autoplay src='/clip.wav'></audio>"
                    + "<audio controls src='/missing.wav' alt='Missing audio'></audio>";
            byte[] wav = AudioKit.wav(1, 1, 8000, 16, ScriptedDecoder.pcm(0, 24000), -1);
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.routeFixed("/clip.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav));
            server.routeFixed("/missing.wav", new TestHttpServer.Response(404));
            server.start();
            panel.setJavascriptEnabled(false);
            panel.setPage(server.url("/page"));
            awaitLoaded(panel, server.url("/page"));
            Check.equal(1, server.count("/missing.wav"), "the failing resource was tried once");
            FakeOutputs.Out output = outputs.await(0);
            output.awaitWritten(64);
            Check.isTrue(output.isStarted(), "autoplay starts without a script or click");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
