package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Parser stylesheet ordering using actual HTTP gates, script-visible computed styles/storage and Swing pixels (P15-08-b-3-b-2-b).
 * <p>실제 HTTP 장벽·스크립트 계산 스타일/저장소·Swing 픽셀을 사용하는 파서 스타일시트 순서 시험.
 */
public class ParserStyleBlockingTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public ParserStyleBlockingTest() {
    }

    /** One real component and server, with a controlled stylesheet response. / 제어 가능한 스타일시트 응답을 가진 실제 컴포넌트와 서버. */
    private static final class Fixture {
        /** Local HTTP server. / 로컬 HTTP 서버. */
        final TestHttpServer server = new TestHttpServer();
        /** Actual component with the standard loading pipeline. / 표준 로딩 파이프라인의 실제 컴포넌트. */
        final SwingWebPanel panel;
        /** Signals that a stylesheet request reached the gate. / 스타일시트 요청의 장벽 도달 신호. */
        final CountDownLatch entered = new CountDownLatch(1);
        /** Releases every gated response. / 보류한 모든 응답의 해제. */
        final CountDownLatch release = new CountDownLatch(1);
        /** Creates and sizes the component on the EDT. / EDT에서 컴포넌트를 만들고 크기를 정한다.
         * @throws Exception on EDT failure / EDT 실패 시 */
        Fixture() throws Exception {
            final SwingWebPanel[] result = new SwingWebPanel[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Creates the real offscreen event/paint surface. / 실제 오프스크린 이벤트·그리기 표면을 만든다. */
                public void run() {
                    result[0] = new SwingWebPanel();
                    result[0].setSize(300, 180);
                }
            });
            panel = result[0];
        }
        /** Registers one stylesheet held until the test releases it. / 시험이 해제할 때까지 보류할 시트를 등록한다.
         * @param path request path / 요청 경로
         * @param css response source / 응답 원문 */
        void gate(String path, final String css) {
            server.route(path, new TestHttpServer.Handler() {
                /** Waits without holding any browser lock. / 브라우저 잠금을 소유하지 않고 기다린다.
                 * @param request received request / 수신 요청
                 * @return controlled CSS response / 제어한 CSS 응답
                 * @throws Exception on interruption / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(15, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("text/css", css);
                }
            });
        }
        /** Starts the gated document. / 장벽이 있는 문서를 시작한다.
         * @param html received HTML / 수신 HTML
         * @throws Exception on server failure / 서버 실패 시 */
        void open(String html) throws Exception {
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
        }
        /** Releases the fixture even if an assertion fails. / 단언이 실패해도 시험 자원을 해제한다. */
        void close() {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /** Reads a script's thread-safe storage signal without touching its live DOM. / 살아 있는 DOM에 접근하지 않고 스레드 안전한 저장소 신호를 읽는다.
     * @param panel actual browser / 실제 브라우저
     * @param key signal key / 신호 키
     * @return signal or null / 신호 또는 null */
    private static String value(SwingWebPanel panel, String key) {
        // 복사된 저장소 항목에서 해당 신호를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Waits for observable script execution. / 관측 가능한 스크립트 실행을 기다린다.
     * @param panel actual browser / 실제 브라우저
     * @param key signal key / 신호 키
     * @param expected expected value / 기대 값
     * @throws Exception on interruption / 인터럽트 시 */
    private static void awaitValue(SwingWebPanel panel, String key, String expected) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        // 스크립트가 실제 실행하여 저장소 신호를 쓸 때까지 확인한다.
        while (!expected.equals(value(panel, key)) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(expected, value(panel, key), "script signal " + key);
    }

    /** Waits for atomic main-document confirmation. / 주 문서의 원자적 확정을 기다린다.
     * @param panel actual browser / 실제 브라우저
     * @param url expected address / 기대 주소
     * @throws Exception on interruption / 인터럽트 시 */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        // 별도 로더가 폐기된 요청과 독립적으로 페이지를 확정하는지 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /** Samples actual CSS rendering on the EDT. / EDT에서 실제 CSS 그리기를 표본 추출한다.
     * @param panel browser surface / 브라우저 표면
     * @return RGB pixel / RGB 픽셀
     * @throws Exception on EDT failure / EDT 실패 시 */
    private static int pixel(final SwingWebPanel panel) throws Exception {
        final int[] rgb = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints using the public component path. / 공개 컴포넌트 경로로 그린다. */
            public void run() {
                BufferedImage image = new BufferedImage(300, 180, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    panel.paint(graphics);
                    rgb[0] = image.getRGB(50, 30) & 0xffffff;
                } finally {
                    graphics.dispose();
                }
            }
        });
        return rgb[0];
    }

    /** A classic external and then inline script wait for nested CSS imports and see their layout. / 일반 외부·후속 인라인 스크립트가 중첩 CSS import를 기다리고 그 배치를 본다.
     * @throws Exception on failure / 실패 시 */
    public void testClassicAndInlineWaitForImport() throws Exception {
        Fixture fixture = new Fixture();
        final CountDownLatch scriptRequested = new CountDownLatch(1);
        String html = "<!doctype html><script>sessionStorage.setItem('early','yes');</script>"
                + "<style>html,body{margin:0;padding:0}#probe{padding:2px;border:3px solid black}</style>"
                + "<link rel=stylesheet href='sheet.css'><div id=probe></div><script src='classic.js'></script>"
                + "<script>sessionStorage.setItem('inline',getComputedStyle(document.getElementById('probe')).width);</script>";
        try {
            fixture.server.routeFixed("/sheet.css", TestHttpServer.Response.ok("text/css", "@import 'nested.css';#probe{height:40px}"));
            fixture.gate("/nested.css", "#probe{width:180px}");
            fixture.server.route("/classic.js", new TestHttpServer.Handler() {
                /** Records script fetching independently of the stylesheet gate. / 스타일시트 장벽과 독립적으로 스크립트 가져오기를 기록한다.
                 * @param request received request / 수신 요청
                 * @return script reading styles and geometry / 스타일·기하 조회 스크립트 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    scriptRequested.countDown();
                    return TestHttpServer.Response.ok("application/javascript", "const e=document.getElementById('probe');"
                            + "const r=e.getBoundingClientRect();sessionStorage.setItem('classic',[getComputedStyle(e).width,r.width,r.height].join('|')); ");
                }
            });
            fixture.open(html);
            awaitValue(fixture.panel, "early", "yes");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "nested CSS request pending");
            Check.isTrue(scriptRequested.await(5, TimeUnit.SECONDS), "classic script fetched while CSS pending");
            Thread.sleep(100);
            Check.isNull(value(fixture.panel, "classic"), "classic execution waits for CSS");
            Check.equal("about:blank", fixture.panel.getPage(), "parsing not yet confirmed");
            fixture.release.countDown();
            awaitValue(fixture.panel, "classic", "180px|190|50");
            awaitValue(fixture.panel, "inline", "180px");
            awaitPage(fixture.panel, fixture.server.url("/page"));
            Check.equal(html, fixture.panel.getContent(), "source preserved through stylesheet wait");
        } finally {
            fixture.close();
        }
    }

    /** Deferred classic scripts wait for styles parsed after their own element. / defer 일반 스크립트가 자기 요소 뒤에 파싱된 스타일을 기다린다.
     * @throws Exception on failure / 실패 시 */
    public void testDeferWaitsForLaterParserStyles() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/later.css", "#probe{width:175px}");
            fixture.server.routeFixed("/deferred.js", TestHttpServer.Response.ok("application/javascript",
                    "sessionStorage.setItem('defer',getComputedStyle(document.getElementById('probe')).width);"));
            fixture.open("<!doctype html><script defer src='deferred.js'></script><link rel=stylesheet href='later.css'><div id=probe></div>");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "later stylesheet pending");
            Thread.sleep(100);
            Check.isNull(value(fixture.panel, "defer"), "defer execution waits for later CSS");
            fixture.release.countDown();
            awaitValue(fixture.panel, "defer", "175px");
        } finally {
            fixture.close();
        }
    }

    /** Async scripts do not acquire the parser stylesheet wait. / async 스크립트는 파서 스타일시트 대기를 얻지 않는다.
     * @throws Exception on failure / 실패 시 */
    public void testAsyncDoesNotWaitForParserStyles() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/slow.css", "#probe{width:175px}");
            fixture.server.routeFixed("/async.js", TestHttpServer.Response.ok("application/javascript", "sessionStorage.setItem('async','ran');"));
            fixture.open("<!doctype html><link rel=stylesheet href='slow.css'><div id=probe></div><script async src='async.js'></script>");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "applicable CSS pending");
            awaitValue(fixture.panel, "async", "ran");
            Check.equal(1, fixture.release.getCount(), "async executed before CSS gate released");
        } finally {
            fixture.close();
        }
    }

    /** Nonmatching media and disabled sheets keep their nonblocking scope through nested imports. / 미일치 미디어·비활성 시트가 중첩 import에도 비차단 범위를 유지한다.
     * @throws Exception on failure / 실패 시 */
    public void testNonmatchingAndDisabledScopesDoNotWait() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.server.routeFixed("/print.css", TestHttpServer.Response.ok("text/css", "@import 'print-child.css';#probe{width:333px}"));
            fixture.server.routeFixed("/disabled.css", TestHttpServer.Response.ok("text/css", "@import 'disabled-child.css';#probe{width:444px}"));
            fixture.gate("/print-child.css", "#probe{width:555px}");
            fixture.gate("/disabled-child.css", "#probe{width:666px}");
            fixture.gate("/inline-print.css", "#probe{width:777px}");
            fixture.gate("/media-import.css", "#probe{width:888px}");
            fixture.gate("/invalid-type.css", "#probe{width:999px}");
            fixture.open("<!doctype html><style>#probe{width:100px;height:20px}</style>"
                    + "<link rel=stylesheet media=print href='print.css'><link disabled rel=stylesheet href='disabled.css'>"
                    + "<style media=print>@import 'inline-print.css';</style><style>@import 'media-import.css' print;</style>"
                    + "<style id=wrong type=text/plain>@import 'invalid-type.css';</style>"
                    + "<script>document.getElementById('wrong').setAttribute('type','text/css');</script>"
                    + "<div id=probe></div><script>const s=getComputedStyle(document.getElementById('probe'));"
                    + "sessionStorage.setItem('nonblocking',s.width+'|'+s.height);</script>");
            awaitValue(fixture.panel, "nonblocking", "100px|20px");
            Check.equal(1, fixture.release.getCount(), "nonapplicable CSS never blocks classic execution");
        } finally {
            fixture.close();
        }
    }

    /** A dynamically inserted link does not become a parser-created blocker for a later parser script. / 동적으로 삽입한 link는 후속 파서 스크립트의 파서 생성 차단자가 되지 않는다.
     * @throws Exception on failure / 실패 시 */
    public void testDynamicStylesDoNotBlockLaterParserScript() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/dynamic.css", "#probe{width:175px}");
            fixture.open("<!doctype html><style>#probe{width:100px}</style><div id=probe></div><script>"
                    + "const l=document.createElement('link');l.setAttribute('rel','stylesheet');l.setAttribute('href','dynamic.css');"
                    + "document.querySelector('head').appendChild(l);</script><script>"
                    + "sessionStorage.setItem('dynamic',getComputedStyle(document.getElementById('probe')).width);</script>");
            awaitValue(fixture.panel, "dynamic", "100px");
            Check.equal(1, fixture.release.getCount(), "dynamic CSS did not block parser execution");
        } finally {
            fixture.close();
        }
    }

    /** Navigating cancels a CSS wait and its network request without waiting for the response. / 이동은 응답을 기다리지 않고 CSS 대기와 네트워크 요청을 취소한다.
     * @throws Exception on failure / 실패 시 */
    public void testNavigationCancelsStylesheetWait() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/slow.css", "body{background:red}");
            fixture.open("<!doctype html><link rel=stylesheet href='slow.css'><script>sessionStorage.setItem('late','bad');</script>");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "CSS wait started");
            fixture.panel.setContent("<p>replacement</p>", "text/html");
            awaitPage(fixture.panel, "about:text");
            Check.equal("<p>replacement</p>", fixture.panel.getContent(), "new document independent of gated CSS");
            Check.isNull(value(fixture.panel, "late"), "cancelled parser script did not execute");
            Check.equal(2, fixture.panel.getHistories().size(), "unconfirmed CSS-blocked page excluded from history");
            Check.equal(1, fixture.release.getCount(), "replacement confirmed before old response released");
        } finally {
            fixture.close();
        }
    }

    /** Disabling JS releases a parser CSS wait but preserves pending CSS and subsequent HTML drawing. / JS 비활성화는 파서 CSS 대기를 풀되 대기 CSS와 후속 HTML 그리기를 유지한다.
     * @throws Exception on failure / 실패 시 */
    public void testDisableReleasesWaitButKeepsStylesheet() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/slow.css", "html,body{margin:0;padding:0;background:#225588;height:200px}");
            fixture.open("<!doctype html><link rel=stylesheet href='slow.css'><script>sessionStorage.setItem('late','bad');</script><div>HTML</div>");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "CSS pending");
            long start = System.currentTimeMillis();
            fixture.panel.setJavascriptEnabled(false);
            Check.isTrue(System.currentTimeMillis() - start < 1000, "disable returns without stylesheet response");
            awaitPage(fixture.panel, fixture.server.url("/page"));
            Check.equal(1, fixture.release.getCount(), "HTML confirmed before pending CSS response");
            Check.isNull(value(fixture.panel, "late"), "disabled parser script suppressed");
            fixture.release.countDown();
            long end = System.currentTimeMillis() + 5000;
            // 스크립트 중단 뒤에도 완료한 CSS가 실제 픽셀에 적용되는지 확인한다.
            while (pixel(fixture.panel) != 0x225588 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(0x225588, pixel(fixture.panel), "pending CSS survives script-only cancellation");
        } finally {
            fixture.close();
        }
    }

    /** Failed sheets/imports unblock scripts without replacing the successful main document. / 실패한 시트·import는 정상 주 문서를 교체하지 않고 스크립트 차단을 푼다.
     * @throws Exception on failure / 실패 시 */
    public void testFailedStylesheetsUnblockScripts() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.server.routeFixed("/missing.css", new TestHttpServer.Response(404));
            fixture.server.routeFixed("/sheet.css", TestHttpServer.Response.ok("text/css", "@import 'missing-import.css';#probe{width:180px}"));
            fixture.server.routeFixed("/missing-import.css", new TestHttpServer.Response(500));
            fixture.open("<!doctype html><link rel=stylesheet href='missing.css'><link rel=stylesheet href='sheet.css'>"
                    + "<div id=probe></div><script>sessionStorage.setItem('error-recovery',getComputedStyle(document.getElementById('probe')).width);</script>");
            awaitValue(fixture.panel, "error-recovery", "180px");
            awaitPage(fixture.panel, fixture.server.url("/page"));
            Check.equal(1, fixture.server.count("/missing.css"), "failed sheet remains a terminal resource");
            Check.equal(1, fixture.server.count("/missing-import.css"), "failed import remains a terminal resource");
        } finally {
            fixture.close();
        }
    }

    /** Disposal interrupts a loader blocked on CSS without waiting for its response. / 폐기는 CSS에 막힌 로더를 응답 대기 없이 중단한다.
     * @throws Exception on failure / 실패 시 */
    public void testDisposeCancelsStylesheetWait() throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.gate("/slow.css", "body{background:red}");
            fixture.open("<!doctype html><link rel=stylesheet href='slow.css'><script>while(true){}</script>");
            Check.isTrue(fixture.entered.await(5, TimeUnit.SECONDS), "CSS wait pending");
            long start = System.currentTimeMillis();
            fixture.panel.dispose();
            Check.isTrue(System.currentTimeMillis() - start < 1000, "dispose returns before response");
            Check.equal("about:blank", fixture.panel.getPage(), "disposed page blank");
            Check.equal(1, fixture.release.getCount(), "response still gated at disposal");
        } finally {
            fixture.close();
        }
    }
}
