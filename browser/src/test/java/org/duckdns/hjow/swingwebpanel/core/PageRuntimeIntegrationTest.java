package org.duckdns.hjow.swingwebpanel.core;

import java.lang.reflect.Field;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Standard SwingWebPanel pipeline tests for parsing-time scripts, resource policy, storage and cancellation (P15-08-b-2).
 * DOM observations use the document queue; reflection avoids adding a public testing API to the component.
 * <p>표준 SwingWebPanel 파이프라인의 파싱 중 스크립트·리소스 정책·저장소·취소 시험. DOM은 문서 큐에서 관측하며 시험용 공개 API 추가를 피하고자 리플렉션을 쓴다.
 */
public class PageRuntimeIntegrationTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public PageRuntimeIntegrationTest() {
    }

    /** Finds the existing component state for queue-based observations. / 큐 기반 관측용 기존 컴포넌트 상태를 찾는다.
     * @param panel actual component / 실제 컴포넌트
     * @return owning session / 소유 세션
     * @throws Exception on reflection failure / 리플렉션 실패 시 */
    private static BrowserSession session(SwingWebPanel panel) throws Exception {
        Field field = SwingWebPanel.class.getDeclaredField("session");
        field.setAccessible(true);
        return (BrowserSession) field.get(panel);
    }

    /** Evaluates a host diagnostic only on the page's document queue. / 페이지 문서 큐에서만 호스트 진단을 평가한다.
     * @param session owning session / 소유 세션
     * @param code script expression / 스크립트 식
     * @return expression result / 식의 결과
     * @throws Exception on failure / 실패 시 */
    private static String evaluate(final BrowserSession session, final String code) throws Exception {
        final String[] result = new String[1];
        final Throwable[] failure = new Throwable[1];
        final CountDownLatch done = new CountDownLatch(1);
        session.submitDocumentTask(new Runnable() {
            /** Enters the existing realm without exposing a testing API in the product. / 제품에 시험 API를 노출하지 않고 기존 영역에 진입한다. */
            public void run() {
                try {
                    Field field = HtmlPageDocument.class.getDeclaredField("runtime");
                    field.setAccessible(true);
                    PageRuntime runtime = (PageRuntime) field.get(session.getSnapshot().getDocument());
                    result[0] = runtime.scripts().evaluate(code, "presentation-test");
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    done.countDown();
                }
            }
        });
        Check.isTrue(done.await(5, TimeUnit.SECONDS), "host query completed");
        Check.isNull(failure[0], "host query failure");
        return result[0];
    }

    /** Creates a sized component on the EDT for actual input and painting. / 실제 입력·그리기용 크기가 있는 컴포넌트를 EDT에서 만든다.
     * @return standard component / 표준 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시 */
    private static SwingWebPanel sizedPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the real pointer event source. / 실제 포인터 이벤트 출처를 만든다. */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /** Paints and samples the button background on the EDT. / EDT에서 버튼 배경을 그리고 표본을 읽는다.
     * @param panel actual component / 실제 컴포넌트
     * @return RGB pixel / RGB 픽셀
     * @throws Exception on EDT failure / EDT 실패 시 */
    private static int pixel(final SwingWebPanel panel) throws Exception {
        final int[] color = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Uses the component's actual paint path. / 컴포넌트의 실제 그리기 경로를 쓴다. */
            public void run() {
                BufferedImage image = new BufferedImage(300, 180, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    panel.paint(graphics);
                    color[0] = image.getRGB(50, 30) & 0xffffff;
                } finally {
                    graphics.dispose();
                }
            }
        });
        return color[0];
    }

    /** Waits for a visible asynchronous update. / 보이는 비동기 갱신을 기다린다.
     * @param panel actual component / 실제 컴포넌트
     * @param expected RGB color / 기대 RGB 색
     * @throws Exception on failure / 실패 시 */
    private static void awaitPixel(SwingWebPanel panel, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        int actual;
        // 화면 게시가 끝날 때까지 EDT를 계속 실행하며 확인한다.
        do {
            actual = pixel(panel);
            if (actual == expected) {
                return;
            }
            Thread.sleep(10);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(expected, actual, "actual component background");
    }

    /** Dispatches a real primary AWT click on the EDT. / EDT에서 실제 주 버튼 AWT 클릭을 전달한다.
     * @param panel pointer source / 포인터 출처
     * @throws Exception on EDT failure / EDT 실패 시 */
    private static void click(final SwingWebPanel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Feeds the installed mouse listener without invoking DOM manually. / DOM을 수동 호출하지 않고 설치된 마우스 리스너에 전달한다. */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, 50, 30, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /** Reads one DOM attribute on its owning queue. / 소유 큐에서 DOM 속성 하나를 읽는다.
     * @param session component state / 컴포넌트 상태
     * @param id element ID / 요소 ID
     * @param attribute attribute name, null for text / 속성 이름이며 텍스트는 null
     * @return value, or null for a missing element / 값이며 요소가 없으면 null
     * @throws Exception on queue timeout / 큐 시간 초과 시 */
    private static String read(final BrowserSession session, final String id, final String attribute) throws Exception {
        final String[] result = new String[1];
        final CountDownLatch done = new CountDownLatch(1);
        session.submitDocumentTask(new Runnable() {
            /** Reads DOM only after navigation ownership handoff. / 이동 소유권 이전 뒤에만 DOM을 읽는다. */
            public void run() {
                try {
                    PageDocument holder = session.getSnapshot().getDocument();
                    Document document = holder instanceof HtmlPageDocument ? ((HtmlPageDocument) holder).getDocument() : null;
                    if (document != null && document.getElementById(id) != null) {
                        result[0] = attribute == null ? document.getElementById(id).getTextContent()
                                : document.getElementById(id).getAttribute(attribute);
                    }
                } finally {
                    done.countDown();
                }
            }
        });
        Check.isTrue(done.await(5, TimeUnit.SECONDS), "DOM observation completed");
        return result[0];
    }

    /** Waits for a timer-driven DOM value without reading a live tree from the test thread. / 시험 스레드가 살아 있는 트리를 읽지 않고 타이머의 DOM 값을 기다린다.
     * @param session owning session / 소유 세션
     * @param id element ID / 요소 ID
     * @param expected expected text / 기대 텍스트
     * @throws Exception on failure / 실패 시 */
    private static void awaitText(BrowserSession session, String id, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        String actual;
        // 대기 타이머가 문서 큐에서 실행될 기회를 주며 확인한다.
        do {
            actual = read(session, id, null);
            if (expected.equals(actual)) {
                return;
            }
            Thread.sleep(10);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(expected, actual, "timer-driven DOM text");
    }

    /** Waits for a script's storage signal even while it owns the document queue. / 스크립트가 문서 큐를 소유해도 저장소 신호를 기다린다.
     * @param session owning state / 소유 상태
     * @param key storage key / 저장소 키
     * @throws Exception on timeout / 시간 초과 시 */
    private static void awaitStorage(BrowserSession session, String key) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        // 저장소 조회는 스레드 안전한 복사본이므로 실행 중인 스크립트와 DOM을 공유하지 않는다.
        while (System.currentTimeMillis() < deadline) {
            for (org.duckdns.hjow.swingwebpanel.storage.StorageEntry entry : session.getSessionStorage()) {
                if (key.equals(entry.getKey())) {
                    return;
                }
            }
            Thread.sleep(5);
        }
        Check.fail("script did not publish storage signal: " + key);
    }

    /**
     * Inline ES2015 sees the parser insertion point, writes markup, drains Promise jobs and later runs a document-queue timer.
     * <p>인라인 ES2015가 파서 삽입 지점을 보고 마크업을 쓰며 Promise 작업과 이후 문서 큐 타이머를 실행한다.
     * @throws Exception on failure / 실패 시
     */
    public void testParserPromiseWriteAndTimerPreserveSource() throws Exception {
        SwingWebPanel panel = new SwingWebPanel();
        BrowserSession state = session(panel);
        String source = "<!doctype html><base href='http://runtime.test/'><p id=result>initial</p><script>"
                + "const p=document.getElementById('result'); const write=(s)=>document.write(s);"
                + "p.setAttribute('data-partial',String(document.getElementById('later')===null));"
                + "p.setAttribute('data-current',document.currentScript.tagName);"
                + "write('<b id=written>written</b>');p.textContent='script';"
                + "Promise.resolve().then(()=>{p.textContent+='-promise';});"
                + "setTimeout(()=>{p.textContent+='-timer';p.style.color='red';},0);"
                + "</script><div id=later>later</div>";
        try {
            panel.setContent(source, "text/html");
            TestSupport.settle(state);
            awaitText(state, "result", "script-promise-timer");
            Check.equal("true", read(state, "result", "data-partial"), "script ran before later markup");
            Check.equal("SCRIPT", read(state, "result", "data-current"), "currentScript installed");
            Check.equal("written", read(state, "written", null), "document.write insertion");
            Check.contains(read(state, "result", "style"), "red", "timer style mutation");
            Check.equal(source, panel.getContent(), "received source unchanged");
            Check.equal("about:text", panel.getPage(), "public URL unchanged by DOM");
        } finally {
            panel.dispose();
        }
    }

    /**
     * External scripts share UA/headers/cookies/cache and local/session storage, while force refresh bypasses the cache.
     * <p>외부 스크립트가 UA·헤더·쿠키·캐시·저장소를 공유하며 강제 새로고침은 캐시를 우회한다.
     * @throws Exception on failure / 실패 시
     */
    public void testExternalScriptsRequestPolicyAndStorage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = new SwingWebPanel();
        BrowserSession state = session(panel);
        try {
            String source = "<!doctype html><p id=result></p><script src='code.js'></script>";
            server.routeFixed("/page", TestHttpServer.Response.html(source).header("Cache-Control", "max-age=60")
                    .header("Set-Cookie", "main=yes; Path=/"));
            server.routeFixed("/code.js", TestHttpServer.Response.ok("application/javascript; charset=UTF-8",
                    "document.getElementById('result').textContent=navigator.userAgent+'|'+document.cookie;"
                    + "sessionStorage.setItem('session-key','session-value');localStorage.setItem('local-key','local-value');")
                    .header("Cache-Control", "max-age=60"));
            server.start();
            panel.setUserAgent("script-test/1.0");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Script-Test", "yes");
            panel.addHeaders(headers);
            panel.setPage(server.url("/page"));
            TestSupport.settle(state);
            Check.equal("script-test/1.0|main=yes", read(state, "result", null), "external script executed with common cookie jar");
            Check.equal(1, panel.getSessionStorage().size(), "component session storage");
            Check.isTrue(panel.getLocalStorage().size() > 0, "process local storage");
            panel.refresh();
            TestSupport.settle(state);
            Check.equal(1, server.count("/code.js"), "normal refresh uses script cache");
            panel.refresh(true);
            TestSupport.settle(state);
            Check.equal(2, server.count("/code.js"), "force refresh fetches script again");
            // 서버가 실제 수신한 스크립트 요청의 설정을 검증한다.
            for (TestHttpServer.Request request : server.requests()) {
                if ("/code.js".equals(request.path)) {
                    Check.equal("script-test/1.0", request.header("user-agent"), "script UA");
                    Check.equal("yes", request.header("x-script-test"), "script extra header");
                    Check.contains(request.header("cookie"), "main=yes", "script cookies");
                    Check.equal(server.url("/page"), request.header("referer"), "script referrer");
                }
            }
            Check.equal(source, panel.getContent(), "HTTP source preserved");
        } finally {
            panel.clean();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Disabling aborts an infinite parsing-time script immediately and re-enabling does not restart the old page.
     * <p>비활성화는 파싱 중 무한 반복을 즉시 중단하고 다시 켜도 이전 페이지를 재실행하지 않는다.
     * @throws Exception on failure / 실패 시
     */
    public void testDisableDuringParsingAndExplicitRefresh() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = new SwingWebPanel();
        BrowserSession state = session(panel);
        String source = "<!doctype html><base href='http://runtime.test/'><p id=result>initial</p><script>"
                + "if(sessionStorage.getItem('entered')===null){sessionStorage.setItem('entered','yes');while(true){}}"
                + "document.getElementById('result').textContent='executed';</script>"
                + "<script>document.getElementById('result').setAttribute('data-later','yes');</script>";
        try {
            server.routeFixed("/loop", TestHttpServer.Response.html(source));
            server.start();
            panel.setPage(server.url("/loop"));
            awaitStorage(state, "entered");
            long start = System.currentTimeMillis();
            panel.setJavascriptEnabled(false);
            Check.isTrue(System.currentTimeMillis() - start < 1000, "disable does not wait for script lock");
            TestSupport.settle(state);
            Check.equal("initial", read(state, "result", null), "running script aborted");
            Check.isNull(read(state, "result", "data-later"), "later script suppressed");
            panel.setJavascriptEnabled(true);
            TestSupport.settle(state);
            Check.equal("initial", read(state, "result", null), "old page stays disabled");
            panel.refresh();
            TestSupport.settle(state);
            Check.equal("executed", read(state, "result", null), "new realm executes on explicit refresh");
            Check.equal("yes", read(state, "result", "data-later"), "later scripts on new page execute");
            Check.equal(source, panel.getContent(), "cancel and refresh preserve input source");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * New navigation interrupts a running timer and commits independently of the old task; dispose can run during a timer.
     * <p>새 이동이 실행 중 타이머를 중단하고 이전 작업과 독립적으로 확정되며 타이머 실행 중에도 폐기할 수 있다.
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationAndDisposeCancelRunningTimer() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = new SwingWebPanel();
        BrowserSession state = session(panel);
        String source = "<!doctype html><base href='http://runtime.test/'><p id=result>old</p><script>"
                + "setTimeout(()=>{sessionStorage.setItem('timer-running','yes');while(true){}},0);</script>";
        try {
            server.routeFixed("/timer", TestHttpServer.Response.html(source));
            server.start();
            panel.setPage(server.url("/timer"));
            awaitStorage(state, "timer-running");
            panel.setContent("<p id=result>replacement</p>", "text/html");
            TestSupport.settle(state);
            Check.equal("replacement", read(state, "result", null), "new page commits after cancelling old timer");
            state.getSessionStorageStore().clear();
            panel.setPage(server.url("/timer"));
            awaitStorage(state, "timer-running");
            long start = System.currentTimeMillis();
            panel.dispose();
            Check.isTrue(System.currentTimeMillis() - start < 1000, "dispose does not wait for script lock");
            Check.equal("about:blank", panel.getPage(), "disposed page blank");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /** A stalled external script cannot delay the next document or install a late result. / 멈춘 외부 스크립트가 다음 문서를 지연하거나 늦은 결과를 설치할 수 없다.
     * @throws Exception on failure / 실패 시 */
    public void testStalledScriptFetchCancelledByNavigation() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = new SwingWebPanel();
        BrowserSession state = session(panel);
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        try {
            server.routeFixed("/slow-page", TestHttpServer.Response.html("<p id=result>old</p><script src='slow.js'></script>"));
            server.route("/slow.js", new TestHttpServer.Handler() {
                /** Holds the script response until navigation cancellation has completed. / 이동 취소가 끝날 때까지 스크립트 응답을 보류한다.
                 * @param request recorded request / 기록한 요청
                 * @return late script / 늦은 스크립트
                 * @throws Exception on timeout / 시간 초과 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("application/javascript", "document.getElementById('result').textContent='late';");
                }
            });
            server.start();
            panel.setPage(server.url("/slow-page"));
            Check.isTrue(entered.await(5, TimeUnit.SECONDS), "external request started");
            panel.setContent("<p id=result>new</p>", "text/html");
            TestSupport.settle(state);
            Check.equal("new", read(state, "result", null), "replacement independent of stalled script");
            release.countDown();
            TestSupport.settle(state);
            Check.equal("new", read(state, "result", null), "late script suppressed");
            Check.equal(2, panel.getHistories().size(), "abandoned parsing page excluded from history");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Actual pointer input is trusted and cancellable, drains microtasks/observers, then updates text/class/style and paint.
     * <p>실제 포인터 입력은 신뢰·취소 가능하며 마이크로태스크·관찰자 뒤 텍스트·class·style·그림을 갱신한다.
     * @throws Exception on failure / 실패 시
     */
    public void testTrustedCounterCheckpointAndPainting() throws Exception {
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        String source = "<!doctype html><style>html,body{margin:0;padding:0}#counter{display:block;width:120px;height:60px;background:#112233}</style>"
                + "<a id=counter href='http://never.test/'>0</a><script>"
                + "const button=document.getElementById('counter');let count=0;let order=[];"
                + "new MutationObserver(()=>{order.push('observer');}).observe(button,{childList:true});"
                + "button.addEventListener('click',(e)=>{e.preventDefault();order.push('click');"
                + "button.textContent=String(++count);button.className='active';button.style.backgroundColor='#225588';"
                + "button.setAttribute('data-trusted',String(e.isTrusted));"
                + "Promise.resolve().then(()=>{order.push('promise');});"
                + "setTimeout(()=>{order.push('timer');button.setAttribute('data-order',order.join(','));},0);});</script>";
        try {
            panel.setContent(source, "text/html");
            TestSupport.settle(state);
            awaitPixel(panel, 0x112233);
            click(panel);
            awaitText(state, "counter", "1");
            awaitPixel(panel, 0x225588);
            TestSupport.settle(state);
            Check.equal("true", read(state, "counter", "data-trusted"), "host click trusted");
            Check.equal("active", read(state, "counter", "class"), "counter class changed");
            Check.equal("click,promise,observer,timer", read(state, "counter", "data-order"), "task checkpoint before timer");
            Check.equal("about:text", panel.getPage(), "preventDefault suppressed link navigation");
            Check.equal(2, panel.getHistories().size(), "cancelled link did not add history");
            Check.equal(source, panel.getContent(), "counter changes do not rewrite received HTML");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Disabled JS listeners and inline handlers never restart, while Java DOM listeners and default hyperlinks still work.
     * <p>비활성화한 JS 리스너·인라인 핸들러는 재개하지 않고 Java DOM 리스너와 기본 하이퍼링크는 계속 동작한다.
     * @throws Exception on failure / 실패 시
     */
    public void testDisabledHandlersKeepJavaEventsAndDefaultLink() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = sizedPanel();
        final BrowserSession state = session(panel);
        final CountDownLatch javaClick = new CountDownLatch(1);
        try {
            server.routeFixed("/click", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0;padding:0}"
                    + "a{display:block;width:120px;height:60px;background:#112233}</style>"
                    + "<a id=go href='/next' onclick=\"sessionStorage.setItem('inline-ran','bad');return false\">go</a>"
                    + "<script>document.getElementById('go').addEventListener('click',()=>{sessionStorage.setItem('listener-ran','bad');});</script>"));
            server.routeFixed("/next", TestHttpServer.Response.html("<p id=result>next</p>"));
            server.start();
            panel.setPage(server.url("/click"));
            TestSupport.settle(state);
            awaitPixel(panel, 0x112233);
            state.submitDocumentTask(new Runnable() {
                /** Registers a Java listener, independently of the script bridge. / 스크립트 연결과 독립적인 Java 리스너를 등록한다. */
                public void run() {
                    ((HtmlPageDocument) state.getSnapshot().getDocument()).getDocument().getElementById("go")
                            .addEventListener("click", new EventListener() {
                                /** Records delivery while JavaScript is disabled. / JavaScript 비활성 상태의 전달을 기록한다.
                                 * @param event real input / 실제 입력 */
                                public void handleEvent(Event event) {
                                    Check.isTrue(event.isTrusted(), "Java listener sees trusted input");
                                    javaClick.countDown();
                                }
                            }, false);
                }
            });
            TestSupport.settle(state);
            panel.setJavascriptEnabled(false);
            panel.setJavascriptEnabled(true);
            click(panel);
            Check.isTrue(javaClick.await(5, TimeUnit.SECONDS), "Java DOM listener still notified");
            TestSupport.waitForPage(state, server.url("/next"));
            TestSupport.settle(state);
            Check.equal("next", read(state, "result", null), "default link remains enabled");
            Check.equal(0, panel.getSessionStorage().size(), "neither old listener nor inline handler restarted");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A zero-delay timer reads actual style/layout before the confirmation's EDT notification, then flushes its own mutations.
     * <p>지연 0 타이머가 확정의 EDT 통지 전 실제 스타일·배치를 읽고 자신의 변경도 즉시 반영한다.
     * @throws Exception on failure / 실패 시
     */
    public void testConfirmedQueriesBeforeEdtAndImmediateMutation() throws Exception {
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        final CountDownLatch held = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        TestSupport.flushEdt();
        SwingUtilities.invokeLater(new Runnable() {
            /** Holds only Swing delivery while the document queue remains free. / 문서 큐는 비워 두고 Swing 전달만 보류한다. */
            public void run() {
                held.countDown();
                try {
                    release.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        try {
            Check.isTrue(held.await(5, TimeUnit.SECONDS), "EDT held before navigation");
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}body{padding:20px 30px}"
                    + "#probe{width:100px;height:40px;padding:2px;border:3px solid black}</style>"
                    + "<div id=probe></div><output id=result>pending</output><script>setTimeout(()=>{"
                    + "const e=document.getElementById('probe');const first=e.getBoundingClientRect();"
                    + "const before=getComputedStyle(e).width;e.style.width='160px';"
                    + "const after=e.getBoundingClientRect();const changed=getComputedStyle(e).width;"
                    + "e.style.display='none';const hidden=e.getBoundingClientRect();"
                    + "document.getElementById('result').textContent=[before,first.x,first.y,first.width,first.height,"
                    + "changed,after.width,hidden.width,hidden.height,innerWidth,innerHeight].join('|');},0);</script>", "text/html");
            awaitText(state, "result", "100px|30|20|110|50|160px|170|0|0|300|180");
        } finally {
            release.countDown();
            panel.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * <code>document.elementFromPoint</code> reads the layout of the page: the topmost element at a viewport point, null outside the viewport,
     * and a TypeError for a non-finite coordinate (P20-01).
     * <p>
     * <code>document.elementFromPoint</code> 는 페이지의 배치를 읽는다: 뷰포트 점의 최상위 요소, 뷰포트 밖은 null, 유한하지 않은 좌표는 TypeError(P20-01).
     * @throws Exception on failure / 실패 시
     */
    public void testElementFromPoint() throws Exception {
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        try {
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}#a{position:absolute;left:10px;top:10px;width:50px;height:40px;"
                    + "background:#ff0000}#b{position:absolute;left:30px;top:20px;width:50px;height:40px;background:#0000ff}</style>"
                    + "<div id=a></div><div id=b></div>", "text/html");
            TestSupport.settle(state);
            String hits = evaluate(state, "var f=function(x,y){var h=document.elementFromPoint(x,y);return h?(h.id||h.tagName):'null';};"
                    + "[f(15,15),f(40,30),f(70,50),f(-1,5),f(5000,5),f(5,-1)].join('|')");
            Check.equal("a|b|b|null|null|null", hits, "the topmost element at points inside and outside the viewport");
            String error = evaluate(state, "try { document.elementFromPoint(NaN, 1); 'no error'; } catch (e) { e.name; }");
            Check.equal("TypeError", error, "a coordinate that is not finite");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Scripts scroll the real viewport (<code>scrollTo</code>, <code>scrollBy</code>, <code>scrollIntoView</code>, the scrolling element),
     * <code>scrollX</code>/<code>scrollY</code> read the position, the viewport change fires <code>scroll</code> at the document, and a size
     * change fires <code>resize</code> and updates <code>matchMedia</code> (P20-01, P20-12).
     * <p>
     * 스크립트가 실제 뷰포트를 스크롤하고(<code>scrollTo</code>, <code>scrollBy</code>, <code>scrollIntoView</code>, 스크롤 요소),
     * <code>scrollX</code>/<code>scrollY</code> 가 위치를 읽으며, 뷰포트 변화가 문서에 <code>scroll</code> 을 발생시키고, 크기 변화가 <code>resize</code> 를
     * 발생시키고 <code>matchMedia</code> 를 갱신한다(P20-01, P20-12).
     * @throws Exception on failure / 실패 시
     */
    public void testScriptScrollingAndViewportEvents() throws Exception {
        final SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        final JScrollPane[] panes = new JScrollPane[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Puts the component into a real scroll pane. / 컴포넌트를 실제 스크롤 창에 넣는다. */
                public void run() {
                    panes[0] = new JScrollPane(panel);
                    panes[0].setSize(250, 180);
                    panes[0].doLayout();
                }
            });
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}#top{height:100px}#target{height:40px}#tail{height:900px}</style>"
                    + "<div id=top></div><div id=target></div><div id=tail></div><output id=log></output><script>"
                    + "window.log=[];document.addEventListener('scroll',function(){log.push('scroll:'+scrollY)});"
                    + "window.addEventListener('resize',function(){log.push('resize:'+innerWidth)});"
                    + "window.narrow=matchMedia('(max-width: 200px)');"
                    + "narrow.onchange=function(e){log.push('narrow:'+e.matches)};"
                    + "</script>", "text/html");
            TestSupport.settle(state);
            // 문서가 끝까지 로드되고 크기가 정해지도록 레이아웃을 기다린 뒤 스크롤을 요청한다.
            String start = evaluate(state, "[scrollX, scrollY, document.scrollingElement === document.documentElement].join('|')");
            Check.equal("0|0|true", start, "the initial position and the scrolling element");
            evaluate(state, "window.scrollTo(0, 120); 'asked'");
            Check.equal("0|120", awaitScroll(panes[0], 120), "the viewport follows scrollTo");
            Check.equal("120", evaluate(state, "String(scrollY)"), "scrollY reads the position (right away and after the viewport moved)");
            evaluate(state, "window.scrollBy(0, 50); document.documentElement.scrollTop; 'asked'");
            Check.equal("0|170", awaitScroll(panes[0], 170), "scrollBy is relative");
            evaluate(state, "document.getElementById('top').scrollIntoView(); 'asked'");
            Check.equal("0|0", awaitScroll(panes[0], 0), "scrollIntoView puts the element at the top (the position is clamped)");
            evaluate(state, "document.getElementById('target').scrollIntoView({block: 'start'}); 'asked'");
            Check.equal("0|100", awaitScroll(panes[0], 100), "block start");
            String log = evaluate(state, "log.join('|')");
            Check.contains(log, "scroll:120", "the scroll event of scrollTo");
            // 뷰포트 크기를 줄이면 resize 와 matchMedia 변화가 알려진다.
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Narrows the real viewport. / 실제 뷰포트를 좁힌다. */
                public void run() {
                    panes[0].setSize(180, 180);
                    panes[0].doLayout();
                }
            });
            long deadline = System.currentTimeMillis() + 5000;
            String after;
            // 크기 변화 이벤트가 문서 큐에서 실행될 때까지 기다린다.
            do {
                after = evaluate(state, "log.join('|')");
                Thread.sleep(20);
            } while (after.indexOf("narrow:true") < 0 && System.currentTimeMillis() < deadline);
            Check.contains(after, "resize:", "the resize event");
            Check.contains(after, "narrow:true", "the media query list changed");
            Check.isTrue(after.indexOf("resize:") < after.indexOf("narrow:true"), "resize comes before the media query change");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Waits until the viewport of a scroll pane is at a vertical position and returns its position as <code>x|y</code>.
     * <p>
     * 스크롤 창의 뷰포트가 세로 위치에 닿을 때까지 기다리고 위치를 <code>x|y</code> 로 반환한다.
     *
     * @param pane the scroll pane / 스크롤 창
     * @param y    the expected vertical position / 기대하는 세로 위치
     * @return the position / 위치
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static String awaitScroll(final JScrollPane pane, int y) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        final java.awt.Point[] at = new java.awt.Point[1];
        // 스크롤 요청은 EDT 에서 이루어지므로 위치가 맞을 때까지 확인한다.
        do {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Reads the viewport position. / 뷰포트 위치를 읽는다. */
                public void run() {
                    at[0] = pane.getViewport().getViewPosition();
                }
            });
            if (at[0].y == y) {
                break;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        return at[0].x + "|" + at[0].y;
    }

    /**
     * JScrollPane position/extent changes update script rectangles without Swing reads on the document queue; disposal detaches the observer.
     * <p>JScrollPane 위치·크기 변경이 문서 큐에서 Swing을 읽지 않고 스크립트 사각형에 반영되며 폐기는 관찰자를 해제한다.
     * @throws Exception on failure / 실패 시
     */
    public void testViewportRectanglesAndListenerRelease() throws Exception {
        final SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        final JScrollPane[] panes = new JScrollPane[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Attaches the real scroll viewport. / 실제 스크롤 뷰포트를 부착한다. */
                public void run() {
                    panes[0] = new JScrollPane(panel);
                    panes[0].setSize(250, 180);
                    panes[0].doLayout();
                }
            });
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}"
                    + "body{padding:100px 60px;width:600px;height:1000px}#probe{width:100px;height:40px}</style><div id=probe></div>", "text/html");
            TestSupport.settle(state);
            final JViewport viewport = panes[0].getViewport();
            final Dimension[] extent = new Dimension[1];
            final int[] listeners = new int[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Sets both scroll axes and records the actual extent. / 양쪽 스크롤 축을 설정하고 실제 범위를 기록한다. */
                public void run() {
                    panes[0].doLayout();
                    viewport.setViewSize(new Dimension(720, 1200));
                    viewport.setViewPosition(new Point(40, 80));
                    extent[0] = viewport.getExtentSize();
                    listeners[0] = viewport.getChangeListeners().length;
                }
            });
            String rectangle = evaluate(state, "var r=document.getElementById('probe').getBoundingClientRect();"
                    + "[r.x,r.y,r.width,r.height,innerWidth,innerHeight].join('|')");
            Check.equal("20|20|100|40|" + extent[0].width + "|" + extent[0].height, rectangle, "viewport border rectangle");
            panel.dispose();
            TestSupport.flushEdt();
            Check.equal(listeners[0] - 1, viewport.getChangeListeners().length, "component viewport listener detached");
        } finally {
            panel.dispose();
        }
    }

    /** Parser-time queries and confirmed painting use one presentation, with later markup invalidating the same engine. / 파싱 중 조회와 확정된 그림이 표현 상태 하나를 쓰며 이후 마크업이 같은 엔진을 무효화한다.
     * @throws Exception on failure / 실패 시 */
    public void testParserQueriesAndPresentationHandoff() throws Exception {
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        String source = "<!doctype html><style>html,body{margin:0;padding:0}body{padding:20px 30px}"
                + "#probe{width:100px;height:40px;padding:2px;border:3px solid black;background:#112233}</style>"
                + "<div id=probe></div><output id=result></output><output id=after></output><script>"
                + "const e=document.getElementById('probe');const before=getComputedStyle(e).width;"
                + "const first=e.getBoundingClientRect();e.style.width='160px';e.style.backgroundColor='#225588';"
                + "const changed=getComputedStyle(e).width;const second=e.getBoundingClientRect();"
                + "document.getElementById('result').textContent=[before,first.x,first.y,first.width,first.height,changed,second.width,"
                + "document.getElementById('later')===null].join('|');"
                + "setTimeout(()=>{document.getElementById('after').textContent=getComputedStyle(e).height+'|'+e.getBoundingClientRect().height;},0);"
                + "</script><div id=later>later</div><style>#probe{height:60px}</style>";
        try {
            panel.setContent(source, "text/html");
            TestSupport.settle(state);
            Check.equal("100px|30|20|110|50|160px|170|true", read(state, "result", null), "parser-time actual styles and rectangles");
            awaitText(state, "after", "60px|70");
            awaitPixel(panel, 0x225588);
            Check.equal(source, panel.getContent(), "handoff leaves received source unchanged");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Reads rendered inner text while parsing and after a style change. / 파싱 중과 스타일 변경 뒤 렌더링된 innerText를 읽는다.
     * @throws Exception on failure / 실패 시
     */
    public void testInnerTextReadsCurrentLayout() throws Exception {
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        try {
            panel.setContent("<!doctype html><style>.skip{display:none}.caps{text-transform:uppercase;user-select:none}"
                    + "#wrap{width:38px;overflow-wrap:anywhere}</style>"
                    + "<div id=probe>plain <span class=caps>mixed   case</span><span class=skip>X<br>Y</span>"
                    + "<p>first<br>second</p><div>last</div><span style='display:contents'>tail</span>"
                    + "<span style='visibility:hidden'>omit<span style='visibility:visible'>!</span></span></div>"
                    + "<table id=table><thead><tr><td>A</td><td>B</td></tr></thead><tbody><tr><td>C</td><td>D</td></tr></tbody></table>"
                    + "<pre id=pre>line  x\n\nend\n</pre><div id=wrap>extraordinary word</div>"
                    + "<div id=soft>inter&shy;national</div><textarea id=area>first\nsecond</textarea>"
                    + "<select id=choices><option>One</option><optgroup label='group'><option>Two</option>"
                    + "<option class=skip>Hidden</option></optgroup></select>"
                    + "<script>var el = document.getElementById('probe');var before = el.innerText;</script>", "text/html");
            TestSupport.settle(state);
            Check.equal("plain MIXED CASE\n\nfirst\nsecond\n\nlast\ntail!", evaluate(state, "before"), "visible text with paragraph and br boundaries");
            Check.equal("A\tB\nC\tD", evaluate(state, "document.getElementById('table').innerText"), "table cells and row groups");
            Check.equal("line  x\n\nend\n", evaluate(state, "document.getElementById('pre').innerText"), "preserved white space and line feeds");
            Check.equal("extraordinary word", evaluate(state, "document.getElementById('wrap').innerText"), "soft wrapping without inserted word breaks");
            Check.equal("inter" + (char) 0x00ad + "national", evaluate(state, "document.getElementById('soft').innerText"), "preserved soft hyphen");
            Check.equal("", evaluate(state, "document.getElementById('area').innerText"), "textarea text is not a rendered CSS child");
            Check.equal("One\nTwo", evaluate(state, "document.getElementById('choices').innerText"), "visible options are included");
            Check.equal("true|true|shown", evaluate(state, "var detached=document.createElement('div');detached.textContent='  raw ';"
                    + "el.style.display='none';var hidden=el.innerText===el.textContent;el.style.display='block';"
                    + "el.innerHTML='<span class=skip>hidden</span>shown';[detached.innerText==='  raw ', hidden, el.innerText].join('|')"),
                    "raw text fallback and immediate layout invalidation");
        } finally {
            panel.dispose();
        }
    }

    /** Disabling scripts keeps pending CSS and viewport rendering alive. / 스크립트를 꺼도 대기 CSS와 뷰포트 렌더링을 유지한다.
     * @throws Exception on failure / 실패 시 */
    public void testDisabledScriptsKeepPendingStylesAndRendering() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = sizedPanel();
        BrowserSession state = session(panel);
        try {
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0;padding:0;background:#112233;height:200px}</style>"
                    + "<script>window.initialized=true;</script><link rel=stylesheet href='slow.css'><div>content</div>"));
            server.route("/slow.css", new TestHttpServer.Handler() {
                /** Holds only the stylesheet response. / 스타일시트 응답만 보류한다.
                 * @param request incoming request / 수신 요청
                 * @return delayed CSS / 지연 CSS
                 * @throws Exception on interruption / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("text/css", "html,body{background:#225588}");
                }
            });
            server.start();
            panel.setPage(server.url("/page"));
            TestSupport.settle(state);
            Check.isTrue(entered.await(5, TimeUnit.SECONDS), "late CSS pending after document confirmation");
            panel.setJavascriptEnabled(false);
            release.countDown();
            awaitPixel(panel, 0x225588);
            Check.equal(server.url("/page"), panel.getPage(), "disabled page still displayed");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }
}
