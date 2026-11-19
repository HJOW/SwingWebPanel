package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The CSS Object Model through the real component and real HTTP (DOM-13): a script that inserts a rule, changes the declarations of a rule, disables a
 * style sheet, changes the media of a rule or adopts a constructed style sheet changes what the component paints, a rule that a script inserted survives
 * the arrival of another style sheet (the sheet object stays the same), and a style sheet from another origin cannot be read by the script of the page.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 CSS 객체 모델(DOM-13): 스크립트가 규칙을 넣거나, 규칙의 선언을 바꾸거나, 스타일 시트를 끄거나, 규칙의 미디어를 바꾸거나, 생성
 * 스타일 시트를 채택하면 컴포넌트가 그리는 것이 바뀌고, 스크립트가 넣은 규칙은 다른 스타일 시트가 도착해도 남으며(시트 객체가 그대로이다), 다른 오리진의 스타일
 * 시트는 페이지의 스크립트가 읽을 수 없다.
 */
public class CssomIntegrationTest {

    /** The page of the paint tests: a red square of 40 by 40 pixels, and a script that does the change that the test asks for after the load. 칠 시험의 페이지: 40x40 빨간 사각형과, 시험이 요청한 변경을 로드 뒤에 하는 스크립트. */
    private static final String HEAD = "<!doctype html><html><head><style>body{margin:0}#a{width:40px;height:40px;background:#f00}"
            + "@media print{#a{background:#0f0}}</style></head><body><div id=a></div><script>"
            + "window.addEventListener('load', function () { setTimeout(function () { try {";

    /** The number of the pages that the paint tests served so far. 칠 시험이 지금까지 내놓은 페이지 수. */
    private static int pages;

    /** The end of the script of the paint tests: the result of the change is stored for the test. 칠 시험의 스크립트의 끝: 변경의 결과를 시험이 읽도록 저장한다. */
    private static final String TAIL = "} catch (e) { sessionStorage.setItem('error', e.name + ': ' + e.message); } "
            + "sessionStorage.setItem('done', getComputedStyle(document.getElementById('a')).backgroundColor); }, 20); });</script></body></html>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssomIntegrationTest() {
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
                result[0].setSize(200, 100);
            }
        });
        return result[0];
    }

    /**
     * Paints the component and returns the color of a pixel.
     * <p>
     * 컴포넌트를 그리고 한 픽셀의 색을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param x     the x of the pixel / 픽셀의 x
     * @param y     the y of the pixel / 픽셀의 y
     * @return 0xRRGGBB / 0xRRGGBB
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int pixel(final SwingWebPanel panel, int x, int y) throws Exception {
        final BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints the component on a white image.
             * <p>
             * 흰 이미지에 컴포넌트를 그린다.
             */
            public void run() {
                Graphics2D g = img.createGraphics();
                try {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, 200, 100);
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
        return img.getRGB(x, y) & 0xFFFFFF;
    }

    /**
     * Waits until a pixel of the painted component has a color.
     * <p>
     * 그려진 컴포넌트의 픽셀이 어떤 색이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param x        the x / x
     * @param y        the y / y
     * @param expected the color / 색
     * @param message  the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPixel(SwingWebPanel panel, int x, int y, int expected, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int actual = pixel(panel, x, y);
        // 기대한 색이 될 때까지 짧게 쉬며 다시 그려 본다.
        while (actual != expected && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            actual = pixel(panel, x, y);
        }
        Check.equal(expected, actual, message);
    }

    /**
     * Reads a value of the session storage of the component.
     * <p>
     * 컴포넌트의 sessionStorage 값을 읽는다.
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
     * Waits until the script stored a value.
     * <p>
     * 스크립트가 값을 저장할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static String awaitStored(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        String v = stored(panel, key);
        // 값이 저장될 때까지 짧게 쉬며 확인한다.
        while (v == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            v = stored(panel, key);
        }
        Check.isNull(stored(panel, "error"), "the script of the page did not fail: " + stored(panel, "error"));
        Check.notNull(v, "the script stored " + key);
        return v;
    }

    /**
     * Shows a page whose script makes one change after the load, and waits until the script reported it.
     * <p>
     * 로드 뒤 스크립트가 변경 하나를 하는 페이지를 보이고 스크립트가 그것을 보고할 때까지 기다린다.
     *
     * @param panel  the component / 컴포넌트
     * @param server the server of the pages / 페이지를 내놓는 서버
     * @param change the script of the change / 변경의 스크립트
     * @return the computed background color of the square after the change / 변경 뒤 사각형의 계산 배경색
     * @throws Exception on failure / 실패 시
     */
    private static String change(SwingWebPanel panel, TestHttpServer server, String change) throws Exception {
        String path = "/p" + (++pages);
        server.routeFixed(path, TestHttpServer.Response.html(HEAD + change + TAIL));
        panel.setPage(server.url(path));
        return awaitStored(panel, "done");
    }

    /**
     * An inserted rule, a changed declaration block, a disabled sheet, the media of a rule and an adopted sheet each change the painted square.
     * <p>
     * 넣은 규칙, 바꾼 선언 블록, 끈 시트, 규칙의 미디어, 채택한 시트는 각각 그려진 사각형을 바꾼다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptChangesRepaintThePage() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.start();
            Check.equal("rgb(0, 0, 255)", change(panel, server, "document.styleSheets[0].insertRule('#a { background: #00f }', 3);"), "inserted rule: the computed color");
            awaitPixel(panel, 20, 20, 0x0000FF, "an inserted rule is painted");
            panel.clean();
            Check.equal("rgb(0, 0, 255)", change(panel, server, "var s = document.styleSheets[0]; s.cssRules[1].style.backgroundColor = '#00f'; s.cssRules[1].style.width = '80px';"),
                    "changed declaration: the computed color");
            awaitPixel(panel, 60, 20, 0x0000FF, "the changed width and color are painted");
            panel.clean();
            Check.equal("rgba(0, 0, 0, 0)", change(panel, server, "document.styleSheets[0].disabled = true;"), "disabled sheet: the computed color");
            awaitPixel(panel, 20, 20, 0xFFFFFF, "a disabled sheet is not painted");
            panel.clean();
            Check.equal("rgb(0, 255, 0)", change(panel, server, "var m = document.styleSheets[0].cssRules[2]; m.media.mediaText = 'screen';"), "media of a rule: the computed color");
            awaitPixel(panel, 20, 20, 0x00FF00, "a media rule whose media now match is painted");
            panel.clean();
            Check.equal("rgb(255, 165, 0)", change(panel, server, "var c = new CSSStyleSheet(); c.replaceSync('#a { background: orange }'); document.adoptedStyleSheets = [c];"),
                    "adopted sheet: the computed color");
            awaitPixel(panel, 20, 20, 0xFFA500, "an adopted sheet is painted");
            panel.clean();
            Check.equal("rgb(128, 0, 128)", change(panel, server, "var c2 = new CSSStyleSheet(); document.adoptedStyleSheets.push(c2); c2.insertRule('#a { background: purple }');"),
                    "a rule inserted into an adopted sheet: the computed color");
            awaitPixel(panel, 20, 20, 0x800080, "the rule of an adopted sheet is painted");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A rule that a script inserted survives the arrival of another style sheet: the sheet objects are reused when the page collects its sheets again.
     * <p>
     * 스크립트가 넣은 규칙은 다른 스타일 시트가 도착해도 남는다: 페이지가 시트를 다시 모을 때 시트 객체를 재사용한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInsertedRuleSurvivesTheArrivalOfAnotherSheet() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String page = "<!doctype html><html><head><style>body{margin:0}#a{width:40px;height:40px;background:#f00}</style></head><body><div id=a></div><script>"
                    + "window.addEventListener('load', function () {"
                    + "var s0 = document.styleSheets[0]; s0.insertRule('#a { background: #00f }', 2);"
                    + "var l = document.createElement('link'); l.rel = 'stylesheet'; l.href = '/late.css';"
                    + "document.head.appendChild(l); sessionStorage.setItem('before', String(document.styleSheets.length));"
                    + "var iv = setInterval(function () { if (document.styleSheets.length === 2) { clearInterval(iv);"
                    + "sessionStorage.setItem('after', (document.styleSheets[0] === s0) + ':' + s0.cssRules.length + ':' + document.styleSheets.length"
                    + " + ':' + document.styleSheets[1].cssRules.length); } }, 20); });</script></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(page));
            server.routeFixed("/late.css", TestHttpServer.Response.ok("text/css", "#a { margin-left: 60px }").delay(300));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPixel(panel, 20, 20, 0xFFFFFF, "the square moved away from the left edge after the late sheet arrived (the test waits for the second state)");
            Check.equal("1", awaitStored(panel, "before"), "one sheet while the link is loading");
            Check.equal("true:3:2:1", awaitStored(panel, "after"), "the first sheet is the same object and keeps its two rules; the linked sheet is the second");
            awaitPixel(panel, 80, 20, 0x0000FF, "the inserted rule still applies after the arrival");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An <code>@import</code> rule that a script inserts starts the request of the imported sheet, and the sheet that arrives later is given to the
     * rule (the sheet object stays the same), so the imported rules apply and the script can read them.
     * <p>
     * 스크립트가 넣은 <code>@import</code> 규칙은 가져올 시트의 요청을 시작하고, 나중에 도착한 시트는 규칙에 주어진다(시트 객체는 그대로이다). 그래서 가져온 규칙이
     * 적용되고 스크립트가 그것을 읽을 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInsertedImportRuleLoadsTheSheetLater() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String page = "<!doctype html><html><head><style>body{margin:0}#a{width:40px;height:40px;background:#f00}</style></head><body><div id=a></div><script>"
                    + "window.addEventListener('load', function () {"
                    + "var s = document.styleSheets[0]; s.insertRule('@import url(/imp.css);', 0);"
                    + "sessionStorage.setItem('before', s.cssRules.length + ':' + String(s.cssRules[0].styleSheet));"
                    + "var iv = setInterval(function () { if (s.cssRules[0].styleSheet) { clearInterval(iv);"
                    + "sessionStorage.setItem('after', s.cssRules.length + ':' + s.cssRules[0].styleSheet.cssRules.length + ':' + (s.cssRules[0].styleSheet.parentStyleSheet === s)); } }, 20); });"
                    + "</script></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(page));
            server.routeFixed("/imp.css", TestHttpServer.Response.ok("text/css", "#a { background: #00f !important }").delay(200));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("3:null", awaitStored(panel, "before"), "the import rule is in the list at once, without a sheet");
            Check.equal("3:1:true", awaitStored(panel, "after"), "the sheet arrives, knows its parent, and the list did not change");
            awaitPixel(panel, 20, 20, 0x0000FF, "the imported rules are painted");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A style sheet from another origin cannot be read by the script of the page (<code>SecurityError</code>), but it still applies.
     * <p>
     * 다른 오리진의 스타일 시트는 페이지의 스크립트가 읽을 수 없지만(<code>SecurityError</code>) 여전히 적용된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginSheetIsNotReadable() throws Exception {
        TestHttpServer main = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            other.routeFixed("/x.css", TestHttpServer.Response.ok("text/css", "#a { background: #0f0 }"));
            other.start();
            String page = "<!doctype html><html><head><style>body{margin:0}#a{width:40px;height:40px;background:#f00}</style><link rel=stylesheet href='" + other.url("/x.css")
                    + "'></head><body><div id=a></div><script>window.addEventListener('load', function () {"
                    + "var r = ''; try { document.styleSheets[1].cssRules; r = 'readable'; } catch (e) { r = e.name; }"
                    + "sessionStorage.setItem('result', r + ':' + document.styleSheets[0].cssRules.length + ':' + document.styleSheets[1].href); });</script></body></html>";
            main.routeFixed("/page", TestHttpServer.Response.html(page));
            main.start();
            panel.setPage(main.url("/page"));
            Check.equal("SecurityError:2:" + other.url("/x.css"), awaitStored(panel, "result"), "the other origin: SecurityError, the own sheet is readable");
            awaitPixel(panel, 20, 20, 0x00FF00, "the foreign sheet still applies to the page");
        } finally {
            panel.dispose();
            main.stop();
            other.stop();
        }
    }
}
