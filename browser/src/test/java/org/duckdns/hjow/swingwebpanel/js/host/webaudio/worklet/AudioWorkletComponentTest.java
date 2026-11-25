package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The audio worklet through the real component (plan item P25-06, P21): a <code>SwingWebPanel</code> that loads pages and worklet modules from real HTTP servers, runs the scripts of the page on its
 * document thread and the processors on the audio threads, and reports through the session storage. Shown: the module is fetched through the request layer of the component (the user agent of the
 * component, the extra headers, the cookies of the page, the redirect with the relative imports of the module resolved against the final address, the MIME type of a module, CORS for another
 * origin) and the audio that the processor produced equals the closed form; what the worklet writes to the console reaches the console listener of the component on the event thread; and the
 * worklet thread and the rendering thread end when the page is replaced, when <code>clean()</code> or <code>dispose()</code> is called and when scripts are switched off.
 * <p>
 * 실제 컴포넌트를 통한 오디오 worklet(계획 P25-06, P21): 실제 HTTP 서버에서 페이지와 worklet 모듈을 읽고, 페이지의 스크립트는 문서 스레드에서, 프로세서는 오디오 스레드에서 실행하고, 세션 저장소로
 * 보고하는 <code>SwingWebPanel</code>. 보이는 것: 모듈은 컴포넌트의 요청 계층으로 가져오며(컴포넌트의 User-Agent, 추가 헤더, 페이지의 쿠키, 리디렉션과 최종 주소 기준으로 해석되는 모듈의 상대 import,
 * 모듈의 MIME 타입, 다른 오리진에 대한 CORS) 프로세서가 만든 오디오는 닫힌 식과 같다. worklet 이 콘솔에 쓴 것은 컴포넌트의 콘솔 리스너에 이벤트 스레드로 닿는다. 페이지가 교체되거나
 * <code>clean()</code>, <code>dispose()</code> 를 부르거나 스크립트를 끌 때 worklet 스레드와 렌더링 스레드가 끝난다.
 */
public class AudioWorkletComponentTest {

    /**
     * The module of a gain processor with a parameter and a message.
     * <p>
     * 파라미터와 메시지가 있는 게인 프로세서의 모듈.
     */
    private static final String GAIN = "import {factor} from './dep.js';\nconsole.log('hello from the worklet');\n" + "class Gain extends AudioWorkletProcessor {\n"
            + "  static get parameterDescriptors() { return [{name: 'gain', defaultValue: 1, minValue: 0, maxValue: 4}]; }\n" + "  process(inputs, outputs, parameters) {\n"
            + "    const i = inputs[0], o = outputs[0], g = parameters.gain;\n" + "    for (let c = 0; c < i.length; c++) for (let k = 0; k < i[c].length; k++) o[c][k] = i[c][k] * g[0] * factor;\n"
            + "    return false;\n" + "  }\n" + "}\n" + "registerProcessor('gain', Gain);\n";

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletComponentTest() {
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
     * Waits until the number of threads with a prefix is a number (or the time is over) and returns the last count.
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
     * Creates a module response.
     * <p>
     * 모듈 응답을 만든다.
     *
     * @param text the text / 텍스트
     * @return the response / 응답
     */
    private static TestHttpServer.Response script(String text) {
        return TestHttpServer.Response.ok("application/javascript", text);
    }

    /**
     * Serves the page and the modules of a test: the module under <code>/mods/real.js</code> (reachable also through the redirect <code>/short.js</code>) imports <code>./dep.js</code>.
     * <p>
     * 시험의 페이지와 모듈을 제공한다: <code>/mods/real.js</code>(리디렉션 <code>/short.js</code> 로도 닿는다)는 <code>./dep.js</code> 를 import 한다.
     *
     * @param server the server, not started yet / 서버(아직 시작 전)
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.routeFixed("/mods/real.js", script(GAIN));
        server.routeFixed("/mods/dep.js", script("export const factor = 2;\n"));
        server.routeFixed("/short.js", TestHttpServer.Response.redirect(302, "/mods/real.js"));
        server.routeFixed("/plain.js", TestHttpServer.Response.ok("text/plain", GAIN));
        server.start();
    }

    /**
     * A page loads a module through a redirect, builds a graph with a processor and renders it offline: the module is fetched with the user agent and the extra header of the component and with the cookie
     * of the page; the import of the module is resolved against the final address of the redirect; the rendered samples equal the closed form (0.125 through the gain 0.5 and the factor 2 of the
     * imported module is 0.125); a module with the MIME type <code>text/plain</code> is refused (<code>AbortError</code>); what the module wrote to the console reached the console listener on the
     * event thread.
     * <p>
     * 페이지가 리디렉션을 거쳐 모듈을 불러오고, 프로세서가 든 그래프를 만들어 오프라인으로 렌더링한다: 모듈은 컴포넌트의 User-Agent, 추가 헤더, 페이지의 쿠키와 함께 가져온다. 모듈의 import 는 리디렉션의
     * 최종 주소를 기준으로 해석된다. 렌더링한 표본은 닫힌 식과 같다(0.125 에 이득 0.5 와 import 한 모듈의 배율 2 를 곱하면 0.125). MIME 타입이 <code>text/plain</code> 인 모듈은 거부된다
     * (<code>AbortError</code>). 모듈이 콘솔에 쓴 것은 콘솔 리스너에 이벤트 스레드로 닿았다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAModuleThroughTheRequestLayer() throws Exception {
        final List<String> lines = Collections.synchronizedList(new ArrayList<String>());
        final List<Object[]> events = Collections.synchronizedList(new ArrayList<Object[]>());
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            panel.addConsoleListener(new ConsoleListener() {
                /**
                 * Records the message, its origin and the thread.
                 * <p>
                 * 메시지와 출처와 스레드를 기록한다.
                 *
                 * @param event the event / 이벤트
                 */
                public void consoleMessage(ConsoleEvent event) {
                    lines.add(event.getMessage());
                    events.add(new Object[] {event.getOrigin(), Boolean.valueOf(SwingUtilities.isEventDispatchThread())});
                }
            });
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Probe", "worklet");
            panel.addHeaders(headers);
            serve(server, "<!doctype html><body><script>document.cookie = 'sid=abc';"
                    + "var ctx = new OfflineAudioContext(1, 256, 8000);"
                    + "ctx.audioWorklet.addModule('/plain.js').then(function () { sessionStorage.setItem('plain', 'loaded'); }, function (e) { sessionStorage.setItem('plain', e.name); })"
                    + ".then(function () { return ctx.audioWorklet.addModule('/short.js'); })"
                    + ".then(function () { var src = new ConstantSourceNode(ctx, {offset: 0.125}); var n = new AudioWorkletNode(ctx, 'gain', {parameterData: {gain: 0.5}});"
                    + " src.connect(n); n.connect(ctx.destination); src.start(); return ctx.startRendering(); })"
                    + ".then(function (r) { var d = r.getChannelData(0), e = 0; for (var i = 0; i < d.length; i++) { e = Math.max(e, Math.abs(d[i] - 0.125)); } sessionStorage.setItem('rendered', d.length + ':' + (e < 1e-7)); })"
                    + ".catch(function (e) { sessionStorage.setItem('failure', String(e && e.name) + ' ' + String(e && e.message)); });</script></body>");
            panel.setPage(server.url("/page"));
            Check.equal("AbortError", awaitStored(panel, "plain"), "a module with the MIME type text/plain is refused");
            Check.equal("256:true", awaitStored(panel, "rendered"), "the audio of the processor: the constant 0.125 times the gain 0.5 times the factor 2 of the imported module");
            Check.equal(null, stored(panel, "failure"), "no failure");
            TestHttpServer.Request module = server.last("/short.js");
            Check.notNull(module, "the module was requested through the redirect");
            Check.equal("swingwebpanel/1.0", module.header("User-Agent"), "the user agent of the component");
            Check.equal("worklet", module.header("X-Probe"), "the extra header of the component");
            Check.contains(module.header("Cookie"), "sid=abc", "the cookie of the page");
            Check.equal(1, server.count("/mods/dep.js"), "the import was resolved against the final address of the redirect and fetched once");
            long end = System.currentTimeMillis() + 10000;
            // 콘솔 이벤트가 EDT 로 도착할 때까지 기다린다.
            while (!lines.contains("hello from the worklet") && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.isTrue(lines.contains("hello from the worklet"), "the console of the worklet reached the listener: " + lines);
            Object[] e = events.get(lines.indexOf("hello from the worklet"));
            Check.equal(ConsoleEvent.Origin.WORKER, e[0], "the origin of a worklet is reported as a worker");
            Check.equal(Boolean.TRUE, e[1], "on the event thread");
        } finally {
            server.stop();
            panel.dispose();
        }
    }

    /**
     * Adds a processor through a static JSON dependency and keeps the JavaScript entry cache separate.
     * <p>
     * 정적 JSON 의존성으로 프로세서를 추가하고 JavaScript 진입 모듈의 캐시는 별도로 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJsonModuleInWorklet() throws Exception {
        final List<String> lines = Collections.synchronizedList(new ArrayList<String>());
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            panel.addConsoleListener(new ConsoleListener() {
                public void consoleMessage(ConsoleEvent event) {
                    lines.add(event.getMessage());
                }
            });
            server.routeFixed("/mods/settings.json", TestHttpServer.Response.ok("application/json", "{\"gain\":0.25}")
                    .header("Cache-Control", "no-store"));
            server.routeFixed("/mods/main.js", script("import settings from './settings.json' with {type:'json'};"
                    + "import('./settings.json',{with:{type:'json'}}).then(()=>console.log('dynamic:loaded'),"
                    + "e=>console.log('dynamic:'+e.name));"
                    + "class Test extends AudioWorkletProcessor {"
                    + " static get parameterDescriptors(){return [{name:'gain',defaultValue:settings.gain}];}"
                    + " process(){return false;} } registerProcessor('json',Test);"));
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><script>"
                    + "const ctx=new OfflineAudioContext(1,128,8000);"
                    + "ctx.audioWorklet.addModule('/mods/main.js',{credentials:'omit'}).then(()=>{"
                    + "const node=new AudioWorkletNode(ctx,'json');"
                    + "sessionStorage.setItem('gain',node.parameters.get('gain').value);"
                    + "return ctx.audioWorklet.addModule('/mods/settings.json');"
                    + "}).catch(e=>sessionStorage.setItem('moduleError',e.name));</script>")
                    .header("Set-Cookie", "worklet=42; Path=/"));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("0.25", awaitStored(panel, "gain"), "processor uses the static JSON value");
            Check.equal("AbortError", awaitStored(panel, "moduleError"), "a cached JSON dependency cannot be a JavaScript entry");
            Check.isNull(server.last("/mods/main.js").header("Cookie"), "omit mode does not send the page cookie");
            Check.equal(2, server.count("/mods/settings.json"), "JSON and JavaScript paths fetch separately");
            long end = System.currentTimeMillis() + 10000;
            // worklet의 동적 가져오기 거부가 콘솔 이벤트로 도착하기를 기다린다.
            while (!lines.contains("dynamic:TypeError") && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.isTrue(lines.contains("dynamic:TypeError"), "dynamic import is rejected in the worklet");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A module of another origin needs CORS (the request mode of module scripts): without <code>Access-Control-Allow-Origin</code> <code>addModule()</code> rejects with <code>AbortError</code>, with it the
     * module loads.
     * <p>
     * 다른 오리진의 모듈에는 CORS 가 필요하다(모듈 스크립트의 요청 모드): <code>Access-Control-Allow-Origin</code> 이 없으면 <code>addModule()</code> 은 <code>AbortError</code> 로 거부하고, 있으면 모듈이
     * 로드된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAModuleOfAnotherOriginNeedsCors() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        try {
            other.routeFixed("/closed.js", script("registerProcessor('closed', class extends AudioWorkletProcessor { process() { return false; } });\n"));
            other.routeFixed("/open.js", script("registerProcessor('open', class extends AudioWorkletProcessor { process() { return false; } });\n").header("Access-Control-Allow-Origin", "*"));
            other.start();
            serve(server, "<!doctype html><body><script>var ctx = new OfflineAudioContext(1, 128, 8000);"
                    + "ctx.audioWorklet.addModule('" + other.url("/closed.js") + "').then(function () { sessionStorage.setItem('closed', 'loaded'); }, function (e) { sessionStorage.setItem('closed', e.name); })"
                    + ".then(function () { return ctx.audioWorklet.addModule('" + other.url("/open.js") + "'); })"
                    + ".then(function () { new AudioWorkletNode(ctx, 'open'); sessionStorage.setItem('open', 'loaded'); }, function (e) { sessionStorage.setItem('open', e.name); });</script></body>");
            panel.setPage(server.url("/page"));
            Check.equal("AbortError", awaitStored(panel, "closed"), "a module that the server does not share");
            Check.equal("loaded", awaitStored(panel, "open"), "a module that the server shares");
        } finally {
            server.stop();
            other.stop();
            panel.dispose();
        }
    }

    /**
     * The threads of the worklet end with the page: a real-time context with a processor has a worklet thread and a rendering thread; replacing the page, <code>clean()</code>, switching scripts off and
     * <code>dispose()</code> end both, and the processor that was playing does not run any more (nothing runs without a page).
     * <p>
     * worklet 의 스레드는 페이지와 함께 끝난다: 프로세서가 있는 실시간 컨텍스트에는 worklet 스레드와 렌더링 스레드가 있다. 페이지 교체, <code>clean()</code>, 스크립트 끄기, <code>dispose()</code> 는 둘 다 끝내며,
     * 재생하던 프로세서는 더 이상 실행되지 않는다(페이지 없이는 아무것도 실행되지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTheThreadsEndWithThePage() throws Exception {
        int baseWorklets = threads("swingwebpanel-audio-worklet-");
        int baseRender = threads("swingwebpanel-audio-render-");
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            String live = "<!doctype html><body><script>var rt = new AudioContext(); rt.audioWorklet.addModule('/mods/real.js').then(function () {"
                    + "var src = new ConstantSourceNode(rt, {offset: 0.5}); var n = new AudioWorkletNode(rt, 'gain'); src.connect(n); n.connect(rt.destination); src.start();"
                    + "setTimeout(function () { sessionStorage.setItem('%KEY%', rt.state + ':' + (rt.currentTime > 0)); }, 400); });</script></body>";
            serve(server, live.replace("%KEY%", "one"));
            server.routeFixed("/second", TestHttpServer.Response.html(live.replace("%KEY%", "two")));
            server.routeFixed("/empty", TestHttpServer.Response.html("<!doctype html><body>empty</body>"));
            panel.setPage(server.url("/page"));
            Check.equal("running:true", awaitStored(panel, "one"), "the real-time context with the worklet runs");
            Check.equal(baseWorklets + 1, threads("swingwebpanel-audio-worklet-"), "one worklet thread");
            Check.equal(baseRender + 1, threads("swingwebpanel-audio-render-"), "one rendering thread");
            // 페이지를 교체하면 이전 페이지의 두 스레드가 끝나고 새 페이지는 자기 스레드를 가진다.
            panel.setPage(server.url("/second"));
            Check.equal("running:true", awaitStored(panel, "two"), "the second page runs its own");
            Check.equal(baseWorklets + 1, awaitThreads("swingwebpanel-audio-worklet-", baseWorklets + 1, 5000), "the worklet thread of the first page ended, the second page has its own");
            Check.equal(baseRender + 1, awaitThreads("swingwebpanel-audio-render-", baseRender + 1, 5000), "and the same for the rendering threads");
            panel.clean();
            Check.equal(baseWorklets, awaitThreads("swingwebpanel-audio-worklet-", baseWorklets, 5000), "clean() ends the worklet thread");
            Check.equal(baseRender, awaitThreads("swingwebpanel-audio-render-", baseRender, 5000), "and the rendering thread");
            // 스크립트를 끄면 이동해도 스크립트가 실행되지 않으므로 스레드가 없다. 다시 켜고 페이지를 불러 dispose 로 끝낸다.
            panel.setJavascriptEnabled(false);
            panel.setPage(server.url("/page"));
            Thread.sleep(300);
            Check.equal(baseWorklets, threads("swingwebpanel-audio-worklet-"), "a page whose scripts are off has no worklet thread");
            panel.setJavascriptEnabled(true);
            panel.setPage(server.url("/page"));
            Check.equal("running:true", awaitStored(panel, "one"), "with scripts on again the page runs");
            Check.equal(baseWorklets + 1, threads("swingwebpanel-audio-worklet-"), "one worklet thread again");
            panel.dispose();
            Check.equal(baseWorklets, awaitThreads("swingwebpanel-audio-worklet-", baseWorklets, 5000), "dispose() ends the worklet thread");
            Check.equal(baseRender, awaitThreads("swingwebpanel-audio-render-", baseRender, 5000), "and the rendering thread");
        } finally {
            server.stop();
            panel.dispose();
        }
    }
}
