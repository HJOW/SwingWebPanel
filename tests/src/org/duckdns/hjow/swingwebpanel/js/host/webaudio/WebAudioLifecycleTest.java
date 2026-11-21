package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the life and the cleanup of everything that the Web Audio interface of a page owns (plan items P25-07 and P21, script side): the threads of the real-time contexts,
 * of the offline renderings and of the decoding services end when the page is disposed (and no result is delivered afterwards), many pages that are created and disposed do not leave
 * threads behind, a page whose scripts do not run creates nothing, what the engine produces reaches the script only as tasks of the page, and the console events of the page (P24)
 * show what scripts print and the errors that they throw from the handlers of the audio events.
 * <p>
 * 페이지의 Web Audio 인터페이스가 소유한 모든 것의 수명과 정리 시험(계획 P25-07, P21 의 스크립트 쪽): 실시간 컨텍스트, 오프라인 렌더링, 디코딩 서비스의 스레드가 페이지를 폐기하면 끝나고
 * (그 뒤에는 어떤 결과도 전달되지 않는다), 만들고 폐기하는 많은 페이지가 스레드를 남기지 않으며, 스크립트가 실행되지 않는 페이지는 아무것도 만들지 않고, 엔진이 만든 것은 페이지의 작업으로만
 * 스크립트에 닿으며, 페이지의 콘솔 이벤트(P24)가 스크립트가 출력한 것과 오디오 이벤트 핸들러가 던진 오류를 보여 준다.
 */
public class WebAudioLifecycleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebAudioLifecycleTest() {
    }

    /**
     * Counts the live threads of the Web Audio engine whose names start with a prefix (<code>swingwebpanel-audio-render-</code>, <code>-offline-</code>, <code>-decode-</code>).
     * <p>
     * 이름이 접두사로 시작하는 Web Audio 엔진의 살아 있는 스레드를 센다(<code>swingwebpanel-audio-render-</code>, <code>-offline-</code>, <code>-decode-</code>).
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
     * Waits until the number of threads with a prefix is at most a number.
     * <p>
     * 접두사를 가진 스레드 수가 어떤 수 이하가 될 때까지 기다린다.
     *
     * @param prefix the prefix / 접두사
     * @param max    the largest number accepted / 허용하는 최대 수
     * @param ms     the longest time to wait / 기다리는 최대 시간
     * @return the number of threads at the end / 끝에서의 스레드 수
     */
    private static int awaitThreads(String prefix, int max, long ms) {
        long end = System.currentTimeMillis() + ms;
        int n = threads(prefix);
        // 스레드가 끝날 때까지(최대 ms) 기다린다.
        while (n > max && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Check.fail("interrupted");
            }
            n = threads(prefix);
        }
        return n;
    }

    /**
     * Disposing a page closes its real-time contexts and ends their threads; the contexts of other pages are not touched.
     * <p>
     * 페이지를 폐기하면 그 실시간 컨텍스트가 닫히고 스레드가 끝난다. 다른 페이지의 컨텍스트는 건드리지 않는다.
     */
    public void testDisposeEndsTheRenderingThreads() {
        int base = threads("swingwebpanel-audio-render-");
        WebAudioPage a = new WebAudioPage();
        WebAudioPage b = new WebAudioPage();
        a.run("var c1 = new AudioContext(), c2 = new AudioContext(), c3 = new AudioContext(); 1");
        b.run("var d1 = new AudioContext(); 1");
        Check.equal(base + 4, threads("swingwebpanel-audio-render-"), "four contexts, four rendering threads");
        a.fixture.scripts.dispose();
        Check.equal(base + 1, awaitThreads("swingwebpanel-audio-render-", base + 1, 3000), "the threads of the disposed page ended, the other page keeps its thread");
        Check.equal("running", pumpAndRead(b, "d1.state"), "the other page's context runs");
        b.fixture.scripts.dispose();
        Check.equal(base, awaitThreads("swingwebpanel-audio-render-", base, 3000), "all threads ended");
    }

    /**
     * Runs the tasks of a page and reads a script value.
     * <p>
     * 페이지의 작업을 실행하고 스크립트 값을 읽는다.
     *
     * @param p      the page / 페이지
     * @param source the script / 스크립트
     * @return the value / 값
     */
    private static String pumpAndRead(WebAudioPage p, String source) {
        p.pumpFor(100);
        return p.run(source);
    }

    /**
     * Many pages that create contexts and are disposed leave no thread behind (the threads are daemon threads with the product's names, so they are counted by name).
     * <p>
     * 컨텍스트를 만들고 폐기하는 많은 페이지가 스레드를 남기지 않는다(스레드는 제품 이름의 데몬 스레드이므로 이름으로 센다).
     */
    public void testManyPagesLeaveNoThreads() {
        int render = threads("swingwebpanel-audio-render-");
        int offline = threads("swingwebpanel-audio-offline-");
        int decode = threads("swingwebpanel-audio-decode-");
        // 페이지를 스무 번 만들고 컨텍스트, 렌더링, 디코딩을 시작한 뒤 폐기한다.
        for (int i = 0; i < 20; i++) {
            WebAudioPage p = new WebAudioPage();
            p.run("var rt = new AudioContext(); var osc = rt.createOscillator(); osc.connect(rt.destination); osc.start(); var o = new OfflineAudioContext(1, 48000 * 4, 48000);"
                    + " var s = o.createOscillator(); s.connect(o.destination); s.start(); o.startRendering(); o.decodeAudioData(new ArrayBuffer(64)); 1");
            p.pumpFor(5);
            p.fixture.scripts.dispose();
        }
        Check.isTrue(awaitThreads("swingwebpanel-audio-render-", render, 5000) <= render, "no rendering thread is left");
        Check.isTrue(awaitThreads("swingwebpanel-audio-offline-", offline, 5000) <= offline, "no offline rendering thread is left");
        Check.isTrue(awaitThreads("swingwebpanel-audio-decode-", decode, 8000) <= decode, "no decoding thread is left");
    }

    /**
     * An offline rendering that is in progress when the page is disposed stops, and neither the promise nor an event is delivered afterwards.
     * <p>
     * 페이지를 폐기할 때 진행 중인 오프라인 렌더링은 멈추며, 그 뒤에는 프라미스도 이벤트도 전달되지 않는다.
     */
    public void testDisposeCancelsAnOfflineRendering() {
        int offline = threads("swingwebpanel-audio-offline-");
        WebAudioPage p = new WebAudioPage();
        p.run("var delivered = 0; var ctx = new OfflineAudioContext(2, 48000 * 120, 48000); for (var i = 0; i < 100; i++) { var o = ctx.createOscillator(); o.frequency.value = 100 + i;"
                + " var f = ctx.createBiquadFilter(); o.connect(f); f.connect(ctx.destination); o.start(); } ctx.oncomplete = function () { delivered++; }; ctx.startRendering().then(function () { delivered++; }); 1");
        Check.equal(offline + 1, threads("swingwebpanel-audio-offline-"), "the rendering thread is running");
        p.fixture.scripts.dispose();
        Check.equal(offline, awaitThreads("swingwebpanel-audio-offline-", offline, 5000), "the rendering thread ended after the disposal, long before the rendering would have");
        p.pumpFor(200);
        Check.isNull(p.fixture.scripts.evaluate("delivered", "after"), "no script runs after the disposal");
    }

    /**
     * A page whose scripts are switched off runs no script, so it creates no context, no thread and no object of the Web Audio API; the same page with scripting on does.
     * <p>
     * 스크립트가 꺼진 페이지는 스크립트를 실행하지 않으므로 컨텍스트도 스레드도 Web Audio API 객체도 만들지 않는다. 스크립트를 켠 같은 페이지는 만든다.
     */
    public void testScriptingOffCreatesNothing() {
        int render = threads("swingwebpanel-audio-render-");
        String html = "<!doctype html><body><p id=\"mark\"></p><script>var made = typeof AudioContext === 'function' ? new AudioContext() : null; document.getElementById('mark').setAttribute('data-made', made ? 'created' : 'none');</script></body>";
        org.duckdns.hjow.swingwebpanel.js.host.PageFixture off = new org.duckdns.hjow.swingwebpanel.js.host.PageFixture();
        off.env.scripting = false;
        off.load("http://example.test/a.html", html);
        Check.isNull(off.document.getElementById("mark").getAttribute("data-made"), "the script did not run");
        Check.equal(render, threads("swingwebpanel-audio-render-"), "no rendering thread");
        off.scripts.dispose();
        org.duckdns.hjow.swingwebpanel.js.host.PageFixture on = new org.duckdns.hjow.swingwebpanel.js.host.PageFixture();
        on.load("http://example.test/a.html", html);
        Check.equal("created", on.document.getElementById("mark").getAttribute("data-made"), "with scripting on the context is created");
        Check.equal(render + 1, threads("swingwebpanel-audio-render-"), "with its thread");
        on.scripts.dispose();
        Check.equal(render, awaitThreads("swingwebpanel-audio-render-", render, 3000), "ended with the page");
    }

    /**
     * The decoding services of a page end with it: decodings that are pending when the page is disposed are dropped (no callback, no promise) and their threads end.
     * <p>
     * 페이지의 디코딩 서비스는 페이지와 함께 끝난다: 페이지를 폐기할 때 대기 중인 디코딩은 버려지고(콜백도 프라미스도 없다) 스레드가 끝난다.
     */
    public void testDisposeDropsPendingDecodings() {
        int decode = threads("swingwebpanel-audio-decode-");
        WebAudioPage p = new WebAudioPage();
        p.run("var done = 0; var ctx = new OfflineAudioContext(1, 128, 48000); function wavBig(n) { var buf = new ArrayBuffer(44 + n * 2), v = new DataView(buf);"
                + " function s(o, t) { for (var i = 0; i < t.length; i++) { v.setUint8(o + i, t.charCodeAt(i)); } } s(0, 'RIFF'); v.setUint32(4, 36 + n * 2, true); s(8, 'WAVE'); s(12, 'fmt ');"
                + " v.setUint32(16, 16, true); v.setUint16(20, 1, true); v.setUint16(22, 1, true); v.setUint32(24, 8000, true); v.setUint32(28, 16000, true); v.setUint16(32, 2, true);"
                + " v.setUint16(34, 16, true); s(36, 'data'); v.setUint32(40, n * 2, true); return buf; }"
                + " for (var k = 0; k < 20; k++) { ctx.decodeAudioData(wavBig(2000000), function () { done++; }); } 1");
        p.fixture.scripts.dispose();
        p.pumpFor(100);
        Check.isNull(p.fixture.scripts.evaluate("done", "after"), "no script runs after the disposal");
        Check.isTrue(awaitThreads("swingwebpanel-audio-decode-", decode, 10000) <= decode, "the decoding threads ended");
    }

    /**
     * What the engine produces reaches the script only as tasks of the page: a rendering that has finished does not touch the script until the page runs its tasks.
     * <p>
     * 엔진이 만든 것은 페이지의 작업으로만 스크립트에 닿는다: 끝난 렌더링은 페이지가 작업을 실행할 때까지 스크립트를 건드리지 않는다.
     */
    public void testResultsAreTasksOfThePage() {
        WebAudioPage p = new WebAudioPage();
        try {
            p.run("var settled = false; var ctx = new OfflineAudioContext(1, 1024, 8000); ctx.startRendering().then(function () { settled = true; }); 1");
            // 렌더링은 짧아서 곧 끝나지만 페이지의 작업을 실행하지 않는 동안에는 프라미스가 확정되지 않는다.
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Check.fail("interrupted");
            }
            Check.equal("false", p.run("String(settled)"), "not settled while the page runs no task");
            p.pump();
            Check.equal("true", p.run("String(settled)"), "settled by a task of the page");
        } finally {
            p.fixture.scripts.dispose();
        }
    }

    /**
     * The console of the page shows what scripts print from the handlers of the audio events, and an error that a handler throws is reported without stopping the others (the
     * handlers and the promises of the page keep working).
     * <p>
     * 페이지의 콘솔은 스크립트가 오디오 이벤트 핸들러에서 출력한 것을 보여 주고, 핸들러가 던진 오류는 다른 핸들러를 멈추지 않고 보고된다(페이지의 핸들러와 프라미스는 계속 동작한다).
     */
    public void testConsoleAndErrorsFromHandlers() {
        WebAudioPage p = new WebAudioPage();
        try {
            p.run("var ctx = new OfflineAudioContext(1, 256, 8000); var ok = false; ctx.addEventListener('statechange', function () { console.log('state=' + ctx.state); });"
                    + " ctx.addEventListener('statechange', function () { throw new Error('handler failure'); }); ctx.addEventListener('statechange', function () { ok = true; });"
                    + " ctx.startRendering().then(function (b) { console.log('rendered ' + b.length); }); 1");
            p.waitFor("ok", 5000, "the third listener runs although the second throws");
            p.pumpFor(200);
            String console = p.fixture.console.toString();
            Check.contains(console, "state=running", "the console shows what the handler printed: " + console);
            Check.contains(console, "rendered 256", "and what the promise reaction printed: " + console);
            Check.contains(console, "handler failure", "an error of a handler is reported: " + console);
            List<String> lines = new ArrayList<String>(p.fixture.console);
            Check.isTrue(lines.size() >= 3, "the console has the lines");
            Check.equal("true", p.run("String(ok)"), "the other handlers ran");
        } finally {
            p.fixture.scripts.dispose();
        }
    }
}
