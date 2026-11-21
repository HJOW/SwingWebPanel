package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The Web Audio API through the real component (plan items P25-05 and P25-07, P21): a <code>SwingWebPanel</code> that loads pages from a real HTTP server, runs their scripts on the document
 * thread of the component and reports through the session storage. Shown: a page that fetches an audio file, decodes it with <code>decodeAudioData</code> on a decoding thread and renders it
 * through an <code>OfflineAudioContext</code>, with the rendered samples matching the closed-form expectation; the events of the audio objects reaching the page and the console listener of the
 * component on the event thread; a real-time context that runs until the page is replaced, <code>clean()</code>, <code>dispose()</code> or the scripts are switched off, when its thread ends; and an event thread
 * that keeps answering while a heavy offline rendering runs. This is the check of the whole path of the page interface, the scheduler of the component and the engine threads.
 * <p>
 * 실제 컴포넌트를 통한 Web Audio API(계획 P25-05, P25-07, P21): 실제 HTTP 서버에서 페이지를 읽고, 스크립트를 컴포넌트의 문서 스레드에서 실행하고, 세션 저장소로 보고하는
 * <code>SwingWebPanel</code>. 보이는 것: 오디오 파일을 가져와 디코딩 스레드에서 <code>decodeAudioData</code> 로 디코딩하고 <code>OfflineAudioContext</code> 로 렌더링하는 페이지. 렌더링한
 * 표본은 닫힌 식의 기대값과 같다. 오디오 객체의 이벤트가 페이지와 컴포넌트의 콘솔 리스너에 이벤트 스레드로 닿는다. 실시간 컨텍스트는 페이지가 교체되거나 <code>clean()</code>,
 * <code>dispose()</code> 하거나 스크립트를 끌 때까지 돌다가 스레드가 끝난다. 무거운 오프라인 렌더링이 도는 동안에도 이벤트 스레드가 계속 응답한다. 페이지 인터페이스, 컴포넌트의 스케줄러, 엔진
 * 스레드의 전체 경로를 확인한다.
 */
public class WebAudioComponentTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebAudioComponentTest() {
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(400, 300);
            }
        });
        return result[0];
    }

    /**
     * Counts the live threads whose names start with a prefix.
     * <p>
     * 이름이 접두사로 시작하는 살아 있는 스레드를 센다.
     *
     * @param prefix the prefix / 접두사
     * @return the number of threads / 스레드 수
     */
    private static int threads(String prefix) {
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

    /**
     * Waits until the number of threads with a prefix is exactly a number (or the time is over) and returns the last count.
     * <p>
     * 접두사를 가진 스레드 수가 어떤 수가 될 때까지(또는 시간이 다할 때까지) 기다리고 마지막 수를 반환한다.
     *
     * @param prefix   the prefix / 접두사
     * @param expected the expected number / 기대하는 수
     * @param ms       the longest time to wait / 기다리는 최대 시간
     * @return the last count / 마지막 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static int awaitThreads(String prefix, int expected, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        int n = threads(prefix);
        // 스레드 수가 기대한 값이 될 때까지 짧게 쉬며 확인한다.
        while (n != expected && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            n = threads(prefix);
        }
        return n;
    }

    /**
     * Reads a value that the page stored in the session storage of the component.
     * <p>
     * 페이지가 컴포넌트의 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String stored(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the page stored a value.
     * <p>
     * 페이지가 값을 저장할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static String awaitStored(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (stored(panel, key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(stored(panel, key) != null, "the page stored " + key + " (failure: " + stored(panel, "failure") + ")");
        return stored(panel, key);
    }

    /**
     * Makes a PCM WAV file of a sine: 16 bit, mono, 8000 Hz, 1000 samples of a 400 Hz sine with the amplitude 0.5.
     * <p>
     * 사인파의 PCM WAV 파일을 만든다: 16비트, 모노, 8000 Hz, 진폭 0.5 인 400 Hz 사인파 1000 표본.
     *
     * @return the file bytes / 파일 바이트
     */
    private static byte[] toneWav() {
        int n = 1000;
        byte[] b = new byte[44 + n * 2];
        put(b, 0, "RIFF");
        le32(b, 4, 36 + n * 2);
        put(b, 8, "WAVEfmt ");
        le32(b, 16, 16);
        le16(b, 20, 1);
        le16(b, 22, 1);
        le32(b, 24, 8000);
        le32(b, 28, 16000);
        le16(b, 32, 2);
        le16(b, 34, 16);
        put(b, 36, "data");
        le32(b, 40, n * 2);
        // 400 Hz 사인파를 16비트 표본으로 쓴다.
        for (int i = 0; i < n; i++) {
            le16(b, 44 + i * 2, (int) Math.round(16384 * Math.sin(2 * Math.PI * 400 * i / 8000.0)));
        }
        return b;
    }

    /**
     * Writes ASCII text into bytes.
     * <p>
     * ASCII 문자열을 바이트에 쓴다.
     *
     * @param b the bytes / 바이트
     * @param o the offset / 위치
     * @param s the text / 문자열
     */
    private static void put(byte[] b, int o, String s) {
        // 글자마다 한 바이트로 쓴다.
        for (int i = 0; i < s.length(); i++) {
            b[o + i] = (byte) s.charAt(i);
        }
    }

    /**
     * Writes a little endian 16 bit integer.
     * <p>
     * 리틀 엔디언 16비트 정수를 쓴다.
     *
     * @param b the bytes / 바이트
     * @param o the offset / 위치
     * @param v the value / 값
     */
    private static void le16(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
    }

    /**
     * Writes a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 쓴다.
     *
     * @param b the bytes / 바이트
     * @param o the offset / 위치
     * @param v the value / 값
     */
    private static void le32(byte[] b, int o, int v) {
        le16(b, o, v);
        le16(b, o + 2, v >> 16);
    }

    /**
     * Serves a page and the file, and loads the page in the component.
     * <p>
     * 페이지와 파일을 제공하고 컴포넌트에 페이지를 로드한다.
     *
     * @param panel  the component / 컴포넌트
     * @param server the server, not started yet / 서버(아직 시작 전)
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(SwingWebPanel panel, TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.routeFixed("/tone.wav", new TestHttpServer.Response(200).bytes("audio/wav", toneWav()));
        server.start();
        panel.setPage(server.url("/page"));
    }

    /**
     * A page fetches an audio file from the server, decodes it (on a decoding thread of the engine), renders it with a gain through an offline context and reports what it got: the
     * shape of the decoded buffer and the rendered samples, which equal the samples of the file times the gain.
     * <p>
     * 페이지가 서버에서 오디오 파일을 가져와 (엔진의 디코딩 스레드에서) 디코딩하고, 오프라인 컨텍스트로 게인을 거쳐 렌더링하고, 얻은 것을 보고한다: 디코딩한 버퍼의 모양과 렌더링한
     * 표본. 표본은 파일의 표본에 게인을 곱한 것과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDecodeAndRenderThroughTheComponent() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><body><script>"
                    + "fetch('/tone.wav').then(function (r) { return r.arrayBuffer(); }).then(function (ab) { return new OfflineAudioContext(1, 128, 8000).decodeAudioData(ab); })"
                    + ".then(function (buf) { sessionStorage.setItem('decoded', buf.length + ':' + buf.sampleRate + ':' + buf.numberOfChannels);"
                    + " var oc = new OfflineAudioContext(1, buf.length, buf.sampleRate); var s = oc.createBufferSource(); s.buffer = buf; var g = oc.createGain(); g.gain.value = 0.5;"
                    + " s.connect(g); g.connect(oc.destination); s.start(); return oc.startRendering(); })"
                    + ".then(function (r) { var d = r.getChannelData(0), m = 0, e = 0; for (var i = 0; i < d.length; i++) { m = Math.max(m, Math.abs(d[i]));"
                    + " e = Math.max(e, Math.abs(d[i] - 0.25 * Math.sin(2 * Math.PI * 400 * i / 8000))); } sessionStorage.setItem('rendered', m.toFixed(4) + ':' + (e < 0.0001)); })"
                    + ".catch(function (e) { sessionStorage.setItem('failure', String(e && e.name) + ' ' + String(e && e.message)); });</script></body>");
            Check.equal("1000:8000:1", awaitStored(panel, "decoded"), "the decoded buffer: 1000 frames, 8000 Hz, one channel");
            Check.equal("0.2500:true", awaitStored(panel, "rendered"), "the rendered samples are the samples of the file times the gain (peak 0.25)");
            Check.equal(null, stored(panel, "failure"), "no failure");
        } finally {
            server.stop();
            panel.dispose();
        }
    }

    /**
     * The events of the audio objects reach the page as tasks of its document thread, and what the page prints from their handlers reaches the console listener of the component on the event
     * thread, in order.
     * <p>
     * 오디오 객체의 이벤트는 페이지의 문서 스레드의 작업으로 페이지에 닿고, 페이지가 핸들러에서 출력한 것은 컴포넌트의 콘솔 리스너에 이벤트 스레드로 순서대로 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEventsAndConsole() throws Exception {
        final List<String> lines = Collections.synchronizedList(new ArrayList<String>());
        final List<Boolean> onEdt = Collections.synchronizedList(new ArrayList<Boolean>());
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            panel.addConsoleListener(new ConsoleListener() {
                /**
                 * Records the message and the thread.
                 * <p>
                 * 메시지와 스레드를 기록한다.
                 *
                 * @param event the event / 이벤트
                 */
                public void consoleMessage(ConsoleEvent event) {
                    lines.add(event.getMessage());
                    onEdt.add(Boolean.valueOf(SwingUtilities.isEventDispatchThread()));
                }
            });
            serve(panel, server, "<!doctype html><body><script>var rt = new AudioContext(); var o = rt.createOscillator(); o.connect(rt.destination);"
                    + " rt.onstatechange = function () { console.log('state:' + rt.state); };"
                    + " o.onended = function () { console.log('ended'); sessionStorage.setItem('ended', 'yes'); rt.close(); }; o.start(); o.stop(0.05);</script></body>");
            Check.equal("yes", awaitStored(panel, "ended"), "the ended event reached the page");
            long end = System.currentTimeMillis() + 10000;
            // 콘솔 이벤트가 EDT 로 모두 도착할 때까지(close 후의 statechange 까지) 기다린다.
            while (lines.size() < 3 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal("[state:running, ended, state:closed]", lines.toString(), "the console events in order");
            Check.isFalse(onEdt.contains(Boolean.FALSE), "every console event was delivered on the event thread: " + onEdt);
        } finally {
            server.stop();
            panel.dispose();
        }
    }

    /**
     * A real-time context that a page created runs on its thread (the time advances) until the page is replaced by another page; the thread of the old page ends and the new page has its own.
     * <p>
     * 페이지가 만든 실시간 컨텍스트는 페이지가 다른 페이지로 교체될 때까지 자기 스레드에서 돈다(시간이 흐른다). 이전 페이지의 스레드는 끝나고 새 페이지는 자기 스레드를 가진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationEndsTheContexts() throws Exception {
        int base = threads("swingwebpanel-audio-render-");
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            server.routeFixed("/second", TestHttpServer.Response.html("<!doctype html><body><script>var rt2 = new AudioContext(); sessionStorage.setItem('second', 'loaded');</script></body>"));
            serve(panel, server, "<!doctype html><body><script>var rt = new AudioContext(); setTimeout(function () { sessionStorage.setItem('first', rt.state + ':' + (rt.currentTime > 0.05)); }, 400);</script></body>");
            Check.equal("running:true", awaitStored(panel, "first"), "the context of the first page runs and its time advances");
            Check.equal(base + 1, threads("swingwebpanel-audio-render-"), "with a thread of its own");
            panel.setPage(server.url("/second"));
            Check.equal("loaded", awaitStored(panel, "second"), "the second page loaded");
            Check.equal(base + 1, awaitThreads("swingwebpanel-audio-render-", base + 1, 5000), "the thread of the first page ended, the second page has its own");
            panel.clean();
            Check.equal(base, awaitThreads("swingwebpanel-audio-render-", base, 5000), "clean() ends the thread of the page that was shown");
        } finally {
            server.stop();
            panel.dispose();
        }
        Check.equal(base, awaitThreads("swingwebpanel-audio-render-", base, 5000), "nothing is left after dispose()");
    }

    /**
     * Switching the scripts off ends the contexts of the page, and <code>dispose()</code> ends the contexts of a page that is shown (also ones created by a script that is still running timers).
     * <p>
     * 스크립트를 끄면 페이지의 컨텍스트가 끝나고, <code>dispose()</code> 는 보이는 페이지의 컨텍스트(타이머를 돌리는 스크립트가 만든 것 포함)를 끝낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptsOffAndDispose() throws Exception {
        int base = threads("swingwebpanel-audio-render-");
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><body><script>var a = new AudioContext(), b = new AudioContext(); sessionStorage.setItem('made', 'two'); setInterval(function () { a.currentTime; }, 20);</script></body>");
            Check.equal("two", awaitStored(panel, "made"), "two contexts were made");
            Check.equal(base + 2, threads("swingwebpanel-audio-render-"), "two threads");
            panel.setJavascriptEnabled(false);
            Check.equal(base, awaitThreads("swingwebpanel-audio-render-", base, 5000), "switching the scripts off ended both");
        } finally {
            server.stop();
            panel.dispose();
        }
    }

    /**
     * While a heavy offline rendering runs on the engine thread, the event thread keeps answering: a task that is put on the event thread runs within a fraction of a second every time, and the
     * rendering is cancelled when the component is disposed.
     * <p>
     * 무거운 오프라인 렌더링이 엔진 스레드에서 도는 동안 이벤트 스레드는 계속 응답한다: 이벤트 스레드에 올린 작업은 매번 1 초의 일부 안에 실행되고, 컴포넌트를 폐기하면 렌더링은 취소된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEventThreadStaysResponsive() throws Exception {
        int base = threads("swingwebpanel-audio-offline-");
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><body><script>var ctx = new OfflineAudioContext(2, 48000 * 120, 48000); for (var i = 0; i < 100; i++) { var o = ctx.createOscillator(); o.frequency.value = 100 + i;"
                    + " var f = ctx.createBiquadFilter(); o.connect(f); f.connect(ctx.destination); o.start(); } ctx.startRendering().then(function () { sessionStorage.setItem('finished', 'yes'); });"
                    + " sessionStorage.setItem('started', 'yes');</script></body>");
            Check.equal("yes", awaitStored(panel, "started"), "the rendering started");
            Check.equal(base + 1, threads("swingwebpanel-audio-offline-"), "on a thread of its own");
            long worst = 0;
            // 렌더링이 도는 동안 이벤트 스레드에 작업을 올려 응답 시간을 잰다.
            for (int i = 0; i < 20; i++) {
                long t0 = System.nanoTime();
                SwingUtilities.invokeAndWait(new Runnable() {
                    /**
                     * Does nothing: only the time that the event thread needs to get here is measured.
                     * <p>
                     * 아무것도 하지 않는다. 이벤트 스레드가 여기에 오는 데 걸리는 시간만 잰다.
                     */
                    public void run() {
                    }
                });
                worst = Math.max(worst, (System.nanoTime() - t0) / 1000000L);
                Thread.sleep(50);
            }
            Check.isTrue(worst < 1000, "the event thread answered within " + worst + " ms while the rendering ran");
            Check.equal(null, stored(panel, "finished"), "the rendering is still far from its end");
        } finally {
            server.stop();
            panel.dispose();
        }
        Check.equal(base, awaitThreads("swingwebpanel-audio-offline-", base, 5000), "the rendering was cancelled by dispose()");
    }
}
