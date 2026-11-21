package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.playback.AudioOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.playback.FakeOutputs;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The output of the Web Audio contexts through the real component (plan items P25-04 and P25-07, P21; FEATURE_MATRIX WAU-06c, WAU-10, WAU-12): a <code>SwingWebPanel</code> that loads pages from a
 * real HTTP server and whose protected method {@code createWebAudioOutputFactory()} (and {@code createAudioOutputFactory()} for the elements) a test overrides, as a host does. Shown: a page with an
 * <code>audio</code> element connected to a context through a gain plays the real file from the server through the output that the host chose (the samples of the file times the gain, the output of
 * the element itself is never opened); the rendering threads, the player threads and the outputs end with the page when it is replaced, with <code>clean()</code>, with <code>dispose()</code> and when the
 * scripts are switched off; two components play at the same time, each to its own output; many components that are created and disposed leave nothing behind; and the default of the component (the
 * outputs of Java Sound) lets a context run on a machine without a sound device, without a script error.
 * <p>
 * 실제 컴포넌트를 통한 Web Audio 컨텍스트의 출력(계획 P25-04, P25-07, P21; FEATURE_MATRIX WAU-06c, WAU-10, WAU-12): 실제 HTTP 서버에서 페이지를 읽고 시험이 호스트처럼 보호 메서드
 * {@code createWebAudioOutputFactory()}(그리고 요소를 위한 {@code createAudioOutputFactory()})를 재정의한 <code>SwingWebPanel</code>. 보이는 것: 게인을 거쳐 컨텍스트에 연결된
 * <code>audio</code> 요소가 있는 페이지는 서버의 진짜 파일을 호스트가 고른 출력으로 재생한다(파일의 표본에 게인을 곱한 값. 요소 자신의 출력은 열리지 않는다). 렌더링 스레드, 재생 스레드, 출력은
 * 페이지가 교체되거나 <code>clean()</code>, <code>dispose()</code>, 스크립트를 끌 때 페이지와 함께 끝난다. 두 컴포넌트가 동시에 각자의 출력으로 재생한다. 만들고 폐기하는 많은 컴포넌트는 아무것도
 * 남기지 않는다. 컴포넌트의 기본값(Java Sound 출력)은 소리 장치가 없는 컴퓨터에서도 스크립트 오류 없이 컨텍스트가 돌게 한다.
 */
public class WebAudioOutputComponentTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebAudioOutputComponentTest() {
    }

    /**
     * Creates a component whose outputs the test chooses, on the EDT.
     * <p>
     * 출력을 시험이 고르는 컴포넌트를 EDT 에서 만든다.
     *
     * @param web    the outputs of the real-time contexts / 실시간 컨텍스트의 출력
     * @param direct the outputs of the audio elements, or null / 오디오 요소의 출력. 없으면 null
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel(final OutputFactory web, final AudioOutputFactory direct) throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel() {
                    private static final long serialVersionUID = 1L;

                    /**
                     * Chooses the outputs of the contexts.
                     * <p>
                     * 컨텍스트의 출력을 고른다.
                     *
                     * @return the factory / 팩터리
                     */
                    @Override
                    protected OutputFactory createWebAudioOutputFactory() {
                        return web;
                    }

                    /**
                     * Chooses the outputs of the audio elements.
                     * <p>
                     * 오디오 요소의 출력을 고른다.
                     *
                     * @return the factory / 팩터리
                     */
                    @Override
                    protected AudioOutputFactory createAudioOutputFactory() {
                        return direct;
                    }
                };
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
     * Counts the live playback threads of the media elements (their name is exactly <code>swingwebpanel-audio</code>).
     * <p>
     * 미디어 요소의 살아 있는 재생 스레드를 센다(이름이 정확히 <code>swingwebpanel-audio</code> 이다).
     *
     * @return the number of threads / 스레드 수
     */
    private static int players() {
        Thread[] all = new Thread[Thread.activeCount() + 32];
        int n = Thread.enumerate(all);
        int count = 0;
        // 살아 있는 스레드 중 재생기의 스레드를 센다.
        for (int i = 0; i < n; i++) {
            if (all[i] != null && all[i].isAlive() && "swingwebpanel-audio".equals(all[i].getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Waits until the number of threads with a prefix is at most a number and returns the last count.
     * <p>
     * 접두사를 가진 스레드 수가 어떤 수 이하가 될 때까지 기다리고 마지막 수를 반환한다.
     *
     * @param prefix the prefix / 접두사
     * @param most   the number that it must not exceed / 넘으면 안 되는 수
     * @return the last count / 마지막 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static int awaitAtMost(String prefix, int most) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        int n = threads(prefix);
        // 스레드 수가 줄어들 때까지 짧게 쉬며 확인한다.
        while (n > most && System.currentTimeMillis() < end) {
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
        Check.isTrue(stored(panel, key) != null, "the page stored " + key);
        return stored(panel, key);
    }

    /**
     * Returns the sample of the test ramp: a deterministic 16 bit value, zero at frame 10000 only.
     * <p>
     * 시험 램프의 표본을 반환한다: 결정적인 16비트 값. 프레임 10000 에서만 0 이다.
     *
     * @param frame the frame / 프레임
     * @return the sample / 표본
     */
    private static short ramp(long frame) {
        return (short) (((frame * 7) % 20000) - 10000);
    }

    /**
     * Makes the mono WAV file of the ramp at 48000 Hz.
     * <p>
     * 48000 Hz 램프의 모노 WAV 파일을 만든다.
     *
     * @param frames the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] rampWav(int frames) {
        byte[] data = new byte[frames * 2];
        // 프레임마다 램프의 표본을 리틀 엔디언으로 쓴다.
        for (int i = 0; i < frames; i++) {
            short s = ramp(i);
            data[i * 2] = (byte) s;
            data[i * 2 + 1] = (byte) (s >> 8);
        }
        return AudioKit.wav(1, 1, 48000, 16, data, -1);
    }

    /**
     * Returns the samples that are not zero of the first channel of an output, in order.
     * <p>
     * 출력의 첫 채널에서 0 이 아닌 표본을 순서대로 반환한다.
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
     * Waits until what is audible in an output stopped growing for 200 ms and returns it.
     * <p>
     * 출력에서 들리는 것이 200 ms 동안 더는 늘지 않을 때까지 기다리고 그것을 반환한다.
     *
     * @param out the output / 출력
     * @return the samples / 표본
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static float[] stable(ManualOutput out) throws Exception {
        long end = System.currentTimeMillis() + 15000;
        int last = -1;
        long since = System.currentTimeMillis();
        // 들리는 표본의 수가 200 ms 동안 그대로일 때까지 기다린다.
        while (System.currentTimeMillis() < end) {
            Thread.sleep(20);
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
     * The page: an <code>audio</code> element connected to a context through a gain (the gain is a parameter of the page), played when it can play through; it reports its end in the
     * session storage. With <code>loop</code> it never ends.
     * <p>
     * 페이지: 게인을 거쳐 컨텍스트에 연결한 <code>audio</code> 요소를 끝까지 재생할 수 있게 되면 재생하고 끝을 sessionStorage 에 보고한다. <code>loop</code> 가 있으면 끝나지 않는다.
     *
     * @param src  the address of the audio file / 오디오 파일의 주소
     * @param gain the gain / 게인
     * @param loop true to loop / 반복하려면 true
     * @return the page / 페이지
     */
    private static String page(String src, double gain, boolean loop) {
        return "<!doctype html><body><audio id=a src='" + src + "'" + (loop ? " loop" : "") + "></audio><script>var a = document.getElementById('a'); var rt = new AudioContext();"
                + " var s = rt.createMediaElementSource(a); var g = rt.createGain(); g.gain.value = " + gain + "; s.connect(g); g.connect(rt.destination);"
                + " a.oncanplaythrough = function () { a.play(); sessionStorage.setItem('playing', 'yes'); }; a.onended = function () { sessionStorage.setItem('ended', 'yes'); };"
                + " a.onerror = function () { sessionStorage.setItem('failure', 'error ' + (a.error && a.error.code)); };</script></body>";
    }

    /**
     * A page with an element that is connected to a context plays the real file from the server through the output that the host chose: the samples of the file times the gain, in order and once;
     * the output of the element is never opened; the context runs at 48000 Hz on a stereo output.
     * <p>
     * 요소를 컨텍스트에 연결한 페이지는 서버의 진짜 파일을 호스트가 고른 출력으로 재생한다: 파일의 표본에 게인을 곱한 값이 순서대로 한 번씩이다. 요소의 출력은 열리지 않는다. 컨텍스트는
     * 스테레오 출력에서 48000 Hz 로 돈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testElementOfARealPagePlaysThroughTheOutputOfTheHost() throws Exception {
        ManualOutputFactory contexts = new ManualOutputFactory();
        PacedDevice device = new PacedDevice(contexts);
        FakeOutputs speakers = new FakeOutputs();
        SwingWebPanel panel = newPanel(contexts, speakers);
        TestHttpServer server = new TestHttpServer();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(page("/tone.wav", 0.5, false)));
            server.routeFixed("/tone.wav", new TestHttpServer.Response(200).bytes("audio/wav", rampWav(6000)));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("yes", awaitStored(panel, "ended"), "the element reached its end (failure: " + stored(panel, "failure") + ")");
            ManualOutput out = contexts.waitFor(1, 10000);
            Check.notNull(out, "the host's factory was asked for the output of the context");
            Check.equal(48000, out.getSampleRate(), "at the sample rate of the context");
            Check.equal(2, out.getChannels(), "on a stereo output");
            float[] heard = stable(out);
            Check.equal(6000, heard.length, "every frame of the file reached the output once");
            // 모든 표본이 (16비트 표본 / 32768) * 게인 과 정확히 같아야 한다.
            for (int i = 0; i < heard.length; i++) {
                float want = ramp(i) / 32768f * 0.5f;
                if (heard[i] != want) {
                    Check.fail("sample " + i + ": " + heard[i] + " instead of " + want);
                }
            }
            Check.equal(0, speakers.openCalls, "the output of the element itself was never opened");
            Check.isTrue(panel.getContent().indexOf("createMediaElementSource") >= 0, "getContent() returns the received text of the page, not what the scripts made of it");
        } finally {
            server.stop();
            panel.dispose();
            device.close();
        }
    }

    /**
     * The rendering threads, the player threads and the outputs end with the page: when the page is replaced by another one, with <code>clean()</code>, with <code>dispose()</code> and when the scripts are
     * switched off, while an element plays through a context (a looping element that never ends by itself).
     * <p>
     * 렌더링 스레드, 재생 스레드, 출력은 페이지와 함께 끝난다: 페이지가 다른 페이지로 교체될 때, <code>clean()</code> 할 때, <code>dispose()</code> 할 때, 스크립트를 끌 때. 요소가
     * 컨텍스트를 거쳐 재생 중이어도 그렇다(스스로 끝나지 않는 반복 요소).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageEndsTheContextsThePlayersAndTheOutputs() throws Exception {
        int baseRender = threads("swingwebpanel-audio-render-");
        int basePlayers = players();
        // 컨텍스트와 재생기를 정리하는 네 가지 경로(이동, clean, 스크립트 끄기, dispose)를 차례로 확인한다.
        for (int way = 0; way < 4; way++) {
            ManualOutputFactory contexts = new ManualOutputFactory();
            PacedDevice device = new PacedDevice(contexts);
            SwingWebPanel panel = newPanel(contexts, new FakeOutputs());
            TestHttpServer server = new TestHttpServer();
            try {
                server.routeFixed("/page", TestHttpServer.Response.html(page("/tone.wav", 1.0, true)));
                server.routeFixed("/other", TestHttpServer.Response.html("<!doctype html><body><script>sessionStorage.setItem('other', 'loaded');</script></body>"));
                server.routeFixed("/tone.wav", new TestHttpServer.Response(200).bytes("audio/wav", rampWav(4800)));
                server.start();
                panel.setPage(server.url("/page"));
                Check.equal("yes", awaitStored(panel, "playing"), "the element plays (way " + way + ")");
                ManualOutput out = contexts.waitFor(1, 10000);
                long end = System.currentTimeMillis() + 10000;
                // 소리가 출력에 닿을 때까지 기다린다.
                while (audible(out).length < 4800 && System.currentTimeMillis() < end) {
                    Thread.sleep(10);
                }
                Check.isTrue(audible(out).length >= 4800, "the file is heard (way " + way + ")");
                Check.equal(baseRender + 1, threads("swingwebpanel-audio-render-"), "the context has its rendering thread");
                Check.isTrue(players() >= basePlayers + 1, "and the element has its player");
                if (way == 0) {
                    panel.setPage(server.url("/other"));
                    Check.equal("loaded", awaitStored(panel, "other"), "the other page loaded");
                } else if (way == 1) {
                    panel.clean();
                } else if (way == 2) {
                    panel.setJavascriptEnabled(false);
                } else {
                    panel.dispose();
                }
                Check.equal(baseRender, awaitAtMost("swingwebpanel-audio-render-", baseRender), "the rendering thread ended (way " + way + ")");
                Check.isTrue(awaitAtMost("swingwebpanel-audio", baseRender + basePlayers) <= baseRender + basePlayers, "no player is left (way " + way + ")");
                long until = System.currentTimeMillis() + 10000;
                // 출력이 닫힐 때까지 기다린다.
                while (!out.isClosed() && System.currentTimeMillis() < until) {
                    Thread.sleep(10);
                }
                Check.isTrue(out.isClosed(), "the output of the context was closed (way " + way + ")");
            } finally {
                server.stop();
                panel.dispose();
                device.close();
            }
        }
    }

    /**
     * Two components play at the same time, each through its own context to its own output (the two outputs of the same device are the business of the system mixer), with different files and
     * gains: each output holds the samples of its own file, and disposing one component does not disturb the other.
     * <p>
     * 두 컴포넌트가 동시에, 각자의 컨텍스트로 각자의 출력에 재생한다(같은 장치의 두 출력은 시스템 믹서의 몫이다). 파일과 게인이 서로 다르며 각 출력은 자기 파일의 표본을 가진다. 한 컴포넌트를
     * 폐기해도 다른 컴포넌트는 방해받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTwoComponentsPlayToTheirOwnOutputs() throws Exception {
        ManualOutputFactory firstOutputs = new ManualOutputFactory();
        ManualOutputFactory secondOutputs = new ManualOutputFactory();
        PacedDevice firstDevice = new PacedDevice(firstOutputs);
        PacedDevice secondDevice = new PacedDevice(secondOutputs);
        SwingWebPanel first = newPanel(firstOutputs, new FakeOutputs());
        SwingWebPanel second = newPanel(secondOutputs, new FakeOutputs());
        TestHttpServer server = new TestHttpServer();
        try {
            server.routeFixed("/one", TestHttpServer.Response.html(page("/one.wav", 0.5, false)));
            server.routeFixed("/two", TestHttpServer.Response.html(page("/two.wav", 0.25, true)));
            server.routeFixed("/one.wav", new TestHttpServer.Response(200).bytes("audio/wav", rampWav(5000)));
            server.routeFixed("/two.wav", new TestHttpServer.Response(200).bytes("audio/wav", rampWav(3000)));
            server.start();
            first.setPage(server.url("/one"));
            second.setPage(server.url("/two"));
            Check.equal("yes", awaitStored(first, "ended"), "the first element reached its end");
            Check.equal("yes", awaitStored(second, "playing"), "the second element plays");
            ManualOutput outOne = firstOutputs.waitFor(1, 10000);
            ManualOutput outTwo = secondOutputs.waitFor(1, 10000);
            float[] one = stable(outOne);
            Check.equal(5000, one.length, "the first output holds the file of the first component");
            // 첫 번째 출력은 첫 번째 파일의 표본에 0.5 를 곱한 값이어야 한다.
            for (int i = 0; i < one.length; i++) {
                if (one[i] != ramp(i) / 32768f * 0.5f) {
                    Check.fail("first output sample " + i + ": " + one[i]);
                }
            }
            second.dispose();
            Check.isTrue(outTwo.getWrittenFrames() > 0, "the second output played");
            float[] two = audible(outTwo);
            Check.isTrue(two.length >= 3000 || two.length > 0, "the second output holds audio of its own file");
            // 두 번째 출력은 두 번째 파일의 표본에 0.25 를 곱한 값의 반복이어야 한다(마지막 표본 몇 개만 다를 수 있으므로 앞의 3000 표본만 본다).
            for (int i = 0; i < Math.min(two.length, 3000); i++) {
                if (two[i] != ramp(i % 3000) / 32768f * 0.25f) {
                    Check.fail("second output sample " + i + ": " + two[i] + " instead of " + ramp(i % 3000) / 32768f * 0.25f);
                }
            }
            Check.isFalse(outOne.isClosed(), "the context of the first component goes on after the disposal of the second component");
        } finally {
            server.stop();
            first.dispose();
            second.dispose();
            firstDevice.close();
            secondDevice.close();
        }
    }

    /**
     * Components that are created and disposed again and again, each with a page that creates a context and connects an element, leave no thread behind: the rendering threads and the
     * player threads return to their number from before.
     * <p>
     * 되풀이해서 만들고 폐기하는 컴포넌트는, 각각 컨텍스트를 만들고 요소를 연결하는 페이지를 가져도, 스레드를 남기지 않는다: 렌더링 스레드와 재생 스레드는 이전의 수로 돌아간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testManyComponentsLeaveNothingBehind() throws Exception {
        int baseRender = threads("swingwebpanel-audio-render-");
        int basePlayers = players();
        TestHttpServer server = new TestHttpServer();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(page("/tone.wav", 1.0, true)));
            server.routeFixed("/tone.wav", new TestHttpServer.Response(200).bytes("audio/wav", rampWav(2400)));
            server.start();
            // 컴포넌트를 만들고 재생이 시작되면 폐기하는 것을 되풀이한다.
            for (int i = 0; i < 12; i++) {
                ManualOutputFactory contexts = new ManualOutputFactory();
                PacedDevice device = new PacedDevice(contexts);
                SwingWebPanel panel = newPanel(contexts, new FakeOutputs());
                panel.setPage(server.url("/page"));
                awaitStored(panel, "playing");
                panel.dispose();
                device.close();
            }
            Check.equal(baseRender, awaitAtMost("swingwebpanel-audio-render-", baseRender), "no rendering thread is left");
            Check.isTrue(awaitAtMost("swingwebpanel-audio", baseRender + basePlayers) <= baseRender + basePlayers, "no player thread is left: " + players() + " against " + basePlayers);
        } finally {
            server.stop();
        }
    }

    /**
     * The default of the component (no override: the outputs of Java Sound) lets a context run on a machine without a sound device, and also on a machine with one, as no source plays (silence is
     * written to the device): the context is running, its time advances and no script error or console error appears.
     * <p>
     * 컴포넌트의 기본값(재정의 없음: Java Sound 출력)은 소리 장치가 없는 컴퓨터에서도, 장치가 있는 컴퓨터에서도(소스가 재생하지 않으므로 장치에는 무음을 쓴다) 컨텍스트를 돌게 한다: 컨텍스트는
     * running 이고 시간이 흐르며 스크립트 오류나 콘솔 오류가 나타나지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultOutputLetsAContextRunWithoutAScriptError() throws Exception {
        final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component with the defaults.
             * <p>
             * 기본값으로 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(400, 300);
            }
        });
        SwingWebPanel panel = holder[0];
        panel.addConsoleListener(new ConsoleListener() {
            /**
             * Records the errors.
             * <p>
             * 오류를 기록한다.
             *
             * @param event the event / 이벤트
             */
            public void consoleMessage(ConsoleEvent event) {
                if (event.getLevel() == ConsoleEvent.Level.ERROR) {
                    errors.add(event.getMessage());
                }
            }
        });
        TestHttpServer server = new TestHttpServer();
        int baseRender = threads("swingwebpanel-audio-render-");
        try {
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><body><script>var rt = new AudioContext(); var g = rt.createGain(); g.connect(rt.destination);"
                    + " setTimeout(function () { sessionStorage.setItem('state', rt.state + ':' + (rt.currentTime > 0.2) + ':' + (rt.outputLatency >= 0) + ':' + (rt.baseLatency > 0));"
                    + " rt.suspend().then(function () { return rt.resume(); }).then(function () { return rt.close(); }).then(function () { sessionStorage.setItem('closed', rt.state); }); }, 500);</script></body>"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("running:true:true:true", awaitStored(panel, "state"), "the context runs and its time advances");
            Check.equal("closed", awaitStored(panel, "closed"), "and it can be suspended, resumed and closed");
            Check.equal("[]", errors.toString(), "no script error or console error");
            Check.equal(baseRender, awaitAtMost("swingwebpanel-audio-render-", baseRender), "the rendering thread ended with the close");
        } finally {
            server.stop();
            panel.dispose();
        }
    }
}
