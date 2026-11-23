package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.layout.Woff;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Web fonts (<code>@font-face</code>) through the real component and real HTTP (plan item P10-05): the text uses the next family while
 * the font file is on its way and the page is laid out again with the web font when it arrived (a TrueType file and a WOFF file); the
 * request goes through the common request layer (one request); a font of another origin is fetched in CORS mode (used only if the response allows the origin) and a
 * file that is not a font is ignored. A script measures the text and stores the width in the session storage; the expected width is
 * computed from the same file with Java2D. The font files are the TrueType fonts of the JRE (<code>lib/fonts</code>); a test is skipped
 * if the JRE has none.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 웹 글꼴(<code>@font-face</code>)(계획 P10-05): 글꼴 파일이 오는 동안 텍스트는 다음 패밀리를 쓰고 도착하면 페이지를 웹 글꼴로 다시
 * 배치한다(TrueType 파일과 WOFF 파일). 요청은 공통 요청 계층을 거친다(요청 하나). 다른 오리진의 글꼴은 CORS 모드로 가져오고(응답이 오리진을 허용할 때만 쓴다) 글꼴이 아닌 파일은 무시한다.
 * 스크립트가 텍스트를 재서 너비를 sessionStorage 에 저장하며, 기대 너비는 같은 파일을 Java2D 로 계산한다. 글꼴 파일은 JRE 의 TrueType 글꼴(<code>lib/fonts</code>)이며
 * JRE 에 없으면 시험을 건너뛴다.
 */
public class WebFontIntegrationTest {

    /** The text of the test pages. 시험 페이지의 텍스트. */
    private static final String TEXT = "Hamburgefonstiv";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebFontIntegrationTest() {
    }

    /**
     * Reads a TrueType font file that the JRE ships, or returns null.
     * <p>
     * JRE 가 담은 TrueType 글꼴 파일을 읽는다. 없으면 null.
     *
     * @return the bytes or null / 바이트 또는 null
     * @throws IOException if reading fails / 읽기가 실패하면
     */
    private static byte[] jreFont() throws IOException {
        File f = new File(new File(System.getProperty("java.home"), "lib/fonts"), "LucidaSansRegular.ttf");
        if (!f.isFile()) {
            return null;
        }
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
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
                result[0].setSize(600, 200);
            }
        });
        return result[0];
    }

    /**
     * Returns the width that the script stored, or -1.
     * <p>
     * 스크립트가 저장한 너비를 반환한다. 없으면 -1.
     *
     * @param panel the component / 컴포넌트
     * @return the width / 너비
     */
    private static double width(SwingWebPanel panel) {
        // 복사한 저장소 항목에서 너비를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if ("w".equals(entry.getKey())) {
                return Double.parseDouble(entry.getValue());
            }
        }
        return -1;
    }

    /**
     * Waits until the stored width is near an expected one.
     * <p>
     * 저장된 너비가 기대한 값에 가까워질 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param expected the width / 너비
     * @param message  the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitWidth(SwingWebPanel panel, double expected, String message) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 기대한 너비를 저장할 때까지 짧게 쉬며 확인한다.
        while (Math.abs(width(panel) - expected) > 1.0 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.near(expected, width(panel), 1.0, message);
    }

    /**
     * Builds a page with the text in the web font family (and monospace as the next family) and the measuring script.
     * <p>
     * 웹 글꼴 패밀리(다음 패밀리는 monospace)로 쓴 텍스트와 측정 스크립트가 있는 페이지를 만든다.
     *
     * @param src the <code>src</code> descriptor / <code>src</code> 서술자
     * @return the HTML / HTML
     */
    private static String page(String src) {
        return "<!doctype html><html><head><style>@font-face{font-family:Web;src:" + src + "}"
                + "body{margin:0} #t{display:inline-block;white-space:nowrap;font:20px Web,monospace}</style></head><body>"
                + "<span id=t>" + TEXT + "</span><script>setInterval(function(){sessionStorage.setItem('w',"
                + "String(document.getElementById('t').getBoundingClientRect().width));},20);</script></body></html>";
    }

    /**
     * A TrueType and a WOFF file: the monospace fallback is measured while the file is on its way; after it arrived the width is that of
     * the web font; one request each.
     * <p>
     * TrueType 파일과 WOFF 파일: 파일이 오는 동안은 monospace 대체 글꼴로 재고, 도착한 뒤의 너비는 웹 글꼴의 것이다. 요청은 각각 하나이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFontArrivesAndTheTextIsLaidOutAgain() throws Exception {
        byte[] ttf = jreFont();
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        Font web = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf)).deriveFont(20f);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        double webWidth = web.getStringBounds(TEXT, frc).getWidth();
        double monoWidth = new Font(Font.MONOSPACED, Font.PLAIN, 20).getStringBounds(TEXT, frc).getWidth();
        Check.isTrue(Math.abs(webWidth - monoWidth) > 5, "the two fonts have different widths: " + webWidth + " and " + monoWidth);
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] woff = Woff.fromSfnt(ttf);
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.route("/font.woff", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the font / 글꼴
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(30, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("font/woff", woff);
                }
            });
            String html = page("url(font.woff) format('woff')");
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitWidth(panel, monoWidth, "while the font is on its way the text uses the next family");
            Thread.sleep(200);
            Check.near(monoWidth, width(panel), 1.0, "still the fallback");
            release.countDown();
            awaitWidth(panel, webWidth, "after the font arrived the text is laid out with it");
            Check.equal(1, server.count("/font.woff"), "one request through the common request layer");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A WOFF2 file (Brotli, transformed glyph table; <code>font/woff2</code>) through the real component and real HTTP: while it is on its way the text uses the next
     * family; after it arrived the page is laid out with the font that the decoded file holds (one request). The file is made at test time from a TrueType
     * font of the JRE that runs the test with the test encoder (no file made from those fonts is kept in the repository, see
     * <code>org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts</code>); the test is skipped if the JRE has none.
     * <p>
     * 실제 컴포넌트와 실제 HTTP 로 확인하는 WOFF2 파일(Brotli, 변환된 글리프 테이블, <code>font/woff2</code>): 파일이 오는 동안 텍스트는 다음 패밀리를 쓰고 도착하면 풀린 파일의 글꼴로
     * 다시 배치한다(요청 하나). 파일은 시험 때 시험을 실행하는 JRE 의 TrueType 글꼴을 시험용 인코더로 바꿔 만든다(그 글꼴로 만든 파일은 저장소에 두지 않는다.
     * <code>org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts</code> 참고). JRE 에 그런 글꼴이 없으면 시험을 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWoff2FontIsUsedByThePage() throws Exception {
        byte[] ttf = org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts.jreFont("LucidaBrightDemiBold.ttf");
        // 그 글꼴이 없으면 JRE 의 다른 글꼴을 쓴다.
        if (ttf == null) {
            ttf = jreFont();
        }
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaBrightDemiBold.ttf or LucidaSansRegular.ttf to convert to WOFF2");
            return;
        }
        final byte[] woff2 = org.duckdns.hjow.swingwebpanel.media.font.Woff2Fonts.woff2(ttf, true);
        Font web = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(org.duckdns.hjow.swingwebpanel.media.font.Woff2.toSfnt(woff2))).deriveFont(20f);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        double webWidth = web.getStringBounds(TEXT, frc).getWidth();
        double monoWidth = new Font(Font.MONOSPACED, Font.PLAIN, 20).getStringBounds(TEXT, frc).getWidth();
        Check.isTrue(Math.abs(webWidth - monoWidth) > 5, "the two fonts have different widths: " + webWidth + " and " + monoWidth);
        final CountDownLatch release = new CountDownLatch(1);
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.route("/font.woff2", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the font / 글꼴
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(30, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("font/woff2", woff2);
                }
            });
            String html = page("url(font.woff2) format('woff2'), url(font.woff) format('woff')");
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitWidth(panel, monoWidth, "while the woff2 file is on its way the text uses the next family");
            release.countDown();
            awaitWidth(panel, webWidth, "after the woff2 file arrived the text is laid out with its font");
            Check.equal(1, server.count("/font.woff2"), "one request for the woff2 file");
            Check.equal(0, server.count("/font.woff"), "the later source is not requested");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A font of another origin without a CORS permission is requested but not used (CORS mode) and a file that is no font is
     * ignored: the text keeps the next family in both cases.
     * <p>
     * CORS 허용이 없는 다른 오리진의 글꼴은 요청하지만 쓰지 않고(CORS 모드) 글꼴이 아닌 파일은 무시한다: 두 경우 모두 텍스트는 다음 패밀리를 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginAndBrokenFontsAreIgnored() throws Exception {
        byte[] ttf = jreFont();
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        FontRenderContext frc = new FontRenderContext(null, true, true);
        double monoWidth = new Font(Font.MONOSPACED, Font.PLAIN, 20).getStringBounds(TEXT, frc).getWidth();
        TestHttpServer pageServer = new TestHttpServer();
        TestHttpServer fontServer = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            fontServer.routeFixed("/font.ttf", new TestHttpServer.Response(200).bytes("font/ttf", ttf));
            fontServer.start();
            pageServer.routeFixed("/broken.ttf", new TestHttpServer.Response(200).bytes("font/ttf", "this is no font".getBytes("US-ASCII")));
            pageServer.routeFixed("/page", TestHttpServer.Response.html(page("url(" + fontServer.url("/font.ttf") + "), url(broken.ttf)")));
            pageServer.start();
            panel.setPage(pageServer.url("/page"));
            awaitWidth(panel, monoWidth, "the text uses the next family");
            Thread.sleep(800);
            Check.near(monoWidth, width(panel), 1.0, "and keeps it: nothing usable arrived");
            Check.equal(1, fontServer.count("/font.ttf"), "the font of the other origin was requested in CORS mode (once)");
            Check.isTrue(fontServer.last("/font.ttf").header("Origin") != null && fontServer.last("/font.ttf").header("Origin").startsWith("http://"),
                    "with the Origin header of the page");
            Check.equal(1, pageServer.count("/broken.ttf"), "the broken file was requested once and its failure is remembered");
        } finally {
            panel.dispose();
            pageServer.stop();
            fontServer.stop();
        }
    }

    /**
     * A font of another origin that answers <code>Access-Control-Allow-Origin</code> is used (CSS Fonts Level 4 section 4.3: web fonts are fetched in
     * CORS mode); the request carries the <code>Origin</code> of the page and no cookies.
     * <p>
     * <code>Access-Control-Allow-Origin</code> 으로 응답하는 다른 오리진의 글꼴은 쓴다(CSS Fonts Level 4 4.3 절: 웹 글꼴은 CORS 모드로 가져온다). 요청은 페이지의
     * <code>Origin</code> 을 싣고 쿠키는 싣지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginFontWithCorsPermissionIsUsed() throws Exception {
        byte[] ttf = jreFont();
        if (ttf == null) {
            Check.skip("the JRE has no lib/fonts/LucidaSansRegular.ttf");
            return;
        }
        Font web = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(ttf)).deriveFont(20f);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        double webWidth = web.getStringBounds(TEXT, frc).getWidth();
        TestHttpServer pageServer = new TestHttpServer();
        TestHttpServer fontServer = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            fontServer.routeFixed("/font.ttf", new TestHttpServer.Response(200).bytes("font/ttf", ttf).header("Access-Control-Allow-Origin", "*"));
            fontServer.start();
            pageServer.routeFixed("/page", TestHttpServer.Response.html(page("url(" + fontServer.url("/font.ttf") + ")")));
            pageServer.start();
            panel.setPage(pageServer.url("/page"));
            awaitWidth(panel, webWidth, "the cross-origin font with a CORS permission is used");
            Check.equal(1, fontServer.count("/font.ttf"), "one request");
            Check.isTrue(fontServer.last("/font.ttf").header("Origin") != null, "the request carried an Origin header");
            Check.isTrue(fontServer.last("/font.ttf").header("Cookie") == null, "and no cookie");
        } finally {
            panel.dispose();
            pageServer.stop();
            fontServer.stop();
        }
    }
}
