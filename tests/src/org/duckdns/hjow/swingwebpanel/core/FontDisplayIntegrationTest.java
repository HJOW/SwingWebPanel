package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The component-level checks of <code>font-display</code> with real HTTP (plan item P10-08-d, CSS Fonts Level 4 section 4.8,
 * https://www.w3.org/TR/css-fonts-4/#font-display-desc): a font that arrives in time is used, <code>swap</code> uses a font that arrives late too, and
 * <code>optional</code> never uses a font that arrived after its short block period (the fallback font stays in the page). The page measures the text and reports
 * the width through the sessionStorage; the times are those of the request layer, not of the layout.
 * <p>
 * 실제 HTTP 로 <code>font-display</code> 를 컴포넌트 수준에서 확인한다(계획 P10-08-d, CSS Fonts Level 4 4.8 절): 제때 도착한 글꼴은 쓰이고, <code>swap</code> 은 늦게 도착한
 * 글꼴도 쓰며, <code>optional</code> 은 짧은 block 기간이 지나 도착한 글꼴을 쓰지 않는다(대체 글꼴이 페이지에 그대로 남는다). 페이지가 텍스트를 재어 너비를 sessionStorage 로
 * 알린다. 시간은 레이아웃이 아니라 요청 계층의 것이다.
 */
public class FontDisplayIntegrationTest {

    /** The text that is measured. 재는 텍스트. */
    private static final String TEXT = "Hamburgefonstiv";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontDisplayIntegrationTest() {
    }

    /**
     * Reads the TrueType font that every JRE has, or null.
     * <p>
     * 모든 JRE 에 있는 TrueType 글꼴을 읽는다. 없으면 null.
     *
     * @return the bytes or null / 바이트. 없으면 null
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없으면
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
            /** Creates the component. 컴포넌트를 만든다. */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(600, 200);
            }
        });
        return result[0];
    }

    /**
     * Returns the width that the script of the page stored, or -1.
     * <p>
     * 페이지의 스크립트가 저장한 너비를 반환한다. 없으면 -1.
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
     * Builds a page with a web font of a <code>font-display</code> and the measuring script.
     * <p>
     * <code>font-display</code> 가 있는 웹 글꼴과 측정 스크립트가 있는 페이지를 만든다.
     *
     * @param display the value of <code>font-display</code> / <code>font-display</code> 값
     * @return the HTML / HTML
     */
    private static String page(String display) {
        return "<!doctype html><html><head><style>@font-face{font-family:Web;font-display:" + display + ";src:url(font.ttf) format('truetype')}"
                + "body{margin:0} #t{display:inline-block;white-space:nowrap;font:20px Web,monospace}</style></head><body>"
                + "<span id=t>" + TEXT + "</span><script>setInterval(function(){sessionStorage.setItem('w',"
                + "String(document.getElementById('t').getBoundingClientRect().width));},20);</script></body></html>";
    }

    /**
     * Loads a page whose font answers after a delay and checks which font the text ends in.
     * <p>
     * 글꼴이 지연된 뒤 응답하는 페이지를 읽고 텍스트가 어떤 글꼴로 끝나는지 확인한다.
     *
     * @param display   the value of <code>font-display</code> / <code>font-display</code> 값
     * @param delay     the delay of the font in ms / 글꼴의 지연(ms)
     * @param expectWeb true if the text is expected to end in the web font / 텍스트가 웹 글꼴로 끝나야 하면 true
     * @throws Exception on failure / 실패 시
     */
    private static void run(String display, long delay, boolean expectWeb) throws Exception {
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
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/font.ttf", new TestHttpServer.Response(200).bytes("font/ttf", ttf).delay(delay));
            server.routeFixed("/page", TestHttpServer.Response.html(page(display)));
            server.start();
            panel.setPage(server.url("/page"));
            long end = System.currentTimeMillis() + 30000;
            // 글꼴 요청이 서버에 닿을 때까지 기다린다.
            while (server.count("/font.ttf") == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Thread.sleep(delay + 500);
            if (expectWeb) {
                awaitWidth(panel, webWidth, "font-display: " + display + ": the font arrived in time and the text uses it");
            } else {
                awaitWidth(panel, monoWidth, "font-display: " + display + ": the text starts with the fallback");
                Thread.sleep(500);
                Check.near(monoWidth, width(panel), 1.0, "font-display: " + display + ": the late font is never used");
            }
            Check.equal(1, server.count("/font.ttf"), "one request");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>optional</code>: a font that arrives after the short block period is not used.
     * <p>
     * <code>optional</code>: 짧은 block 기간이 지난 뒤 도착한 글꼴은 쓰지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOptionalFontThatArrivesLateIsNeverUsed() throws Exception {
        run("optional", 700, false);
    }

    /**
     * <code>optional</code>: a font that arrives at once is used.
     * <p>
     * <code>optional</code>: 곧바로 도착한 글꼴은 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOptionalFontThatArrivesAtOnceIsUsed() throws Exception {
        run("optional", 0, true);
    }

    /**
     * <code>swap</code> and the default (<code>auto</code>): a font that arrives late is used.
     * <p>
     * <code>swap</code> 과 기본값(<code>auto</code>): 늦게 도착한 글꼴도 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSwapAndAutoUseALateFont() throws Exception {
        run("swap", 700, true);
        run("auto", 700, true);
    }

    /**
     * <code>fallback</code>: a font that arrives within the short swap period (3 s) is used.
     * <p>
     * <code>fallback</code>: 짧은 swap 기간(3초) 안에 도착한 글꼴은 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFallbackUsesAFontWithinTheSwapPeriod() throws Exception {
        run("fallback", 700, true);
    }
}
