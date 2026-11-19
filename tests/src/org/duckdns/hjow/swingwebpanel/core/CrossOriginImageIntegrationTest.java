package org.duckdns.hjow.swingwebpanel.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Images with a <code>crossorigin</code> attribute through the real component and two real HTTP servers (plan item P20-05-b): the request of a
 * cross-origin image carries the <code>Origin</code> header of the page (<code>null</code> for a page without an origin), a server that grants
 * access only to the origin it sees makes the image load and the canvas stay clean, a response without permission makes the image fail
 * (<code>error</code> event, no pixels for a canvas), <code>anonymous</code> sends no cookies across origins and <code>use-credentials</code>
 * does but needs <code>Access-Control-Allow-Credentials</code>, a same-origin image needs nothing, and an image without the attribute keeps
 * the old behavior: it is shown, sends no <code>Origin</code> and taints a canvas.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 서버 둘로 보는 <code>crossorigin</code> 속성이 있는 이미지(계획 P20-05-b): 교차 오리진 이미지의 요청은 페이지의 <code>Origin</code> 헤더를
 * 싣고(오리진이 없는 페이지는 <code>null</code>), 보이는 오리진에게만 접근을 허용하는 서버는 이미지를 로드되게 하고 캔버스를 깨끗하게 두며, 허락 없는 응답은 이미지를
 * 실패시키고(<code>error</code> 이벤트, 캔버스에 화소 없음), <code>anonymous</code> 는 오리진을 넘어 쿠키를 보내지 않고 <code>use-credentials</code> 는 보내되
 * <code>Access-Control-Allow-Credentials</code> 가 필요하며, 같은 오리진 이미지는 아무것도 필요 없고, 속성이 없는 이미지는 예전대로 보이고 <code>Origin</code> 을
 * 보내지 않고 캔버스를 오염시킨다.
 */
public class CrossOriginImageIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CrossOriginImageIntegrationTest() {
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
     * Makes the bytes of a PNG image of one color.
     * <p>
     * 한 가지 색의 PNG 이미지 바이트를 만든다.
     *
     * @param rgb the color / 색
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png(int rgb) throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        // 모든 화소를 같은 색으로 채운다.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Makes a handler that serves a PNG with the CORS headers that the test asks for.
     * <p>
     * 시험이 요구하는 CORS 헤더를 붙여 PNG 를 제공하는 핸들러를 만든다.
     *
     * @param data        the file bytes / 파일 바이트
     * @param allowOrigin <code>"echo"</code> to answer the <code>Origin</code> header of the request (and nothing without one), a fixed value,
     *                    or null for no header / <code>"echo"</code> 면 요청의 <code>Origin</code> 헤더를 그대로 답하고(없으면 아무것도 안 줌), 고정 값이거나 null 이면 헤더 없음
     * @param credentials true to add <code>Access-Control-Allow-Credentials: true</code> / <code>Access-Control-Allow-Credentials: true</code> 를 붙이려면 true
     * @return the handler / 핸들러
     */
    private static TestHttpServer.Handler image(final byte[] data, final String allowOrigin, final boolean credentials) {
        return new TestHttpServer.Handler() {
            /**
             * Answers with the image.
             * <p>
             * 이미지로 답한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                TestHttpServer.Response r = new TestHttpServer.Response(200).bytes("image/png", data);
                String value = "echo".equals(allowOrigin) ? request.header("Origin") : allowOrigin;
                if (value != null) {
                    r.header("Access-Control-Allow-Origin", value);
                }
                if (credentials) {
                    r.header("Access-Control-Allow-Credentials", "true");
                }
                return r;
            }
        };
    }

    /**
     * Reads a value that the page stored in its session storage.
     * <p>
     * 페이지가 세션 저장소에 저장한 값을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값. 없으면 null
     */
    private static String stored(SwingWebPanel panel, String key) {
        // 저장소 항목을 하나씩 보며 키가 같은 것을 찾는다.
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
        Check.equal(true, stored(panel, key) != null, "the page stored " + key);
        return stored(panel, key);
    }

    /**
     * The test script of a page: when the window has loaded, every image reports <code>error</code> if it failed, else what a canvas can read
     * from it: <code>clean:r,g,b</code> or the name of the exception.
     * <p>
     * 페이지의 시험 스크립트: 창이 로드되면 모든 이미지가 실패했으면 <code>error</code> 를, 아니면 캔버스가 그것에서 읽을 수 있는 것(<code>clean:r,g,b</code> 또는 예외
     * 이름)을 보고한다.
     */
    private static final String SCRIPT = "<script>function report(id) { var i = document.getElementById(id); var key = 'r_' + id; if (!i.complete || i.naturalWidth === 0) {"
            + " sessionStorage.setItem(key, 'error'); return; } var c = document.createElement('canvas'); c.width = 8; c.height = 8; var x = c.getContext('2d'); x.drawImage(i, 0, 0);"
            + " var r; try { var d = x.getImageData(0, 0, 1, 1).data; r = 'clean:' + d[0] + ',' + d[1] + ',' + d[2]; } catch (e) { r = e.name; } sessionStorage.setItem(key, r); }"
            + " window.addEventListener('load', function () { var imgs = document.getElementsByTagName('img'); for (var k = 0; k < imgs.length; k++) { report(imgs[k].id); }"
            + " sessionStorage.setItem('done', 'yes'); });</script>";

    /**
     * An image whose server grants access only to the origin it sees: it loads, a canvas reads it back, the request carried the origin of the
     * page, and the same image without the attribute sends no <code>Origin</code> and taints a canvas.
     * <p>
     * 서버가 보이는 오리진에게만 접근을 허용하는 이미지: 로드되고, 캔버스가 다시 읽을 수 있고, 요청이 페이지의 오리진을 실었으며, 속성 없는 같은 이미지는 <code>Origin</code> 을
     * 보내지 않고 캔버스를 오염시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOriginGatedImage() throws Exception {
        TestHttpServer home = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] data = png(0x336699);
            other.route("/gated.png", image(data, "echo", false));
            other.route("/plain.png", image(data, "echo", false));
            other.route("/wild.png", image(data, "*", false));
            other.route("/closed.png", image(data, null, false));
            other.route("/wrong.png", image(data, "http://example.org", false));
            other.start();
            home.route("/same.png", image(data, null, false));
            String page = "<!doctype html><html><body><img id=gated crossorigin src='" + other.url("/gated.png") + "'><img id=plain src='" + other.url("/plain.png") + "'>"
                    + "<img id=wild crossorigin=anonymous src='" + other.url("/wild.png") + "'><img id=closed crossorigin src='" + other.url("/closed.png") + "'>"
                    + "<img id=wrong crossorigin src='" + other.url("/wrong.png") + "'><img id=same crossorigin src='/same.png'><img id=odd crossorigin=foo src='" + other.url("/wild.png") + "?x=1'>"
                    + SCRIPT + "</body></html>";
            home.routeFixed("/page", TestHttpServer.Response.html(page));
            home.start();
            panel.setPage(home.url("/page"));
            Check.equal("yes", awaitStored(panel, "done"), "the window load event fired (the images did not block it for ever)");
            String origin = home.url("/page").substring(0, home.url("/page").length() - "/page".length());
            Check.equal("clean:51,102,153", stored(panel, "r_gated"), "a server that echoes the origin: the image loads and the canvas stays clean");
            Check.equal(origin, other.last("/gated.png").header("Origin"), "the request carried the origin of the page");
            Check.equal("SecurityError", stored(panel, "r_plain"), "without the attribute the image is shown and taints the canvas");
            Check.isNull(other.last("/plain.png").header("Origin"), "and the request has no Origin header");
            Check.equal("clean:51,102,153", stored(panel, "r_wild"), "Access-Control-Allow-Origin: * grants an anonymous request");
            Check.equal("error", stored(panel, "r_closed"), "a response without permission fails the image");
            Check.equal(origin, other.last("/closed.png").header("Origin"), "the denied request carried the origin as well");
            Check.equal("error", stored(panel, "r_wrong"), "permission for another origin fails the image");
            Check.equal("clean:51,102,153", stored(panel, "r_same"), "a same-origin image needs no permission");
            Check.isNull(home.last("/same.png").header("Origin"), "a same-origin GET carries no Origin header");
            Check.equal("clean:51,102,153", stored(panel, "r_odd"), "an unknown attribute value means anonymous");
        } finally {
            panel.dispose();
            home.stop();
            other.stop();
        }
    }

    /**
     * Cookies and credentials: <code>anonymous</code> sends no cookie to another origin, <code>use-credentials</code> sends it and needs an exact
     * <code>Access-Control-Allow-Origin</code> together with <code>Access-Control-Allow-Credentials: true</code> (the wildcard or a missing
     * credentials header fails the image).
     * <p>
     * 쿠키와 자격 증명: <code>anonymous</code> 는 다른 오리진에 쿠키를 보내지 않고 <code>use-credentials</code> 는 보내며 정확한
     * <code>Access-Control-Allow-Origin</code> 과 <code>Access-Control-Allow-Credentials: true</code> 가 함께 있어야 한다(와일드카드나 없는 자격 증명 헤더는 이미지를
     * 실패시킨다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCredentials() throws Exception {
        TestHttpServer home = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] data = png(0x336699);
            other.route("/anon.png", image(data, "echo", true));
            other.route("/cred.png", image(data, "echo", true));
            other.route("/credwild.png", image(data, "*", true));
            other.route("/crednoallow.png", image(data, "echo", false));
            other.routeFixed("/cookie", new TestHttpServer.Response(200).header("Set-Cookie", "sid=abc; Path=/").text("text/html", "<html><body>cookie</body></html>"));
            other.start();
            String page = "<!doctype html><html><body><img id=anon crossorigin=anonymous src='" + other.url("/anon.png") + "'>"
                    + "<img id=cred crossorigin=use-credentials src='" + other.url("/cred.png") + "'><img id=credwild crossorigin=use-credentials src='" + other.url("/credwild.png") + "'>"
                    + "<img id=crednoallow crossorigin=use-credentials src='" + other.url("/crednoallow.png") + "'>" + SCRIPT + "</body></html>";
            home.routeFixed("/page", TestHttpServer.Response.html(page));
            home.start();
            panel.setPage(other.url("/cookie"));
            long end = System.currentTimeMillis() + 15000;
            // 쿠키 응답이 처리되어 컴포넌트에 쿠키가 생길 때까지 기다린다.
            while (panel.getCookies().indexOf("sid=abc") < 0 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.contains(panel.getCookies(), "sid=abc", "the component has the cookie of the other server");
            panel.setPage(home.url("/page"));
            Check.equal("yes", awaitStored(panel, "done"), "the window load event fired");
            Check.isNull(other.last("/anon.png").header("Cookie"), "anonymous sends no cookie across origins");
            Check.equal("sid=abc", other.last("/cred.png").header("Cookie"), "use-credentials sends the cookie");
            Check.equal("clean:51,102,153", stored(panel, "r_anon"), "anonymous with permission loads");
            Check.equal("clean:51,102,153", stored(panel, "r_cred"), "use-credentials with an exact origin and Allow-Credentials loads");
            Check.equal("error", stored(panel, "r_credwild"), "use-credentials is not granted by the wildcard");
            Check.equal("error", stored(panel, "r_crednoallow"), "use-credentials needs Access-Control-Allow-Credentials");
        } finally {
            panel.dispose();
            home.stop();
            other.stop();
        }
    }

    /**
     * Setting the <code>crossorigin</code> attribute of an image that has loaded without it fetches the image again in CORS mode: the second
     * request carries the <code>Origin</code> header and, after the new <code>load</code> event, a canvas can read the pixels.
     * <p>
     * 속성 없이 로드된 이미지에 <code>crossorigin</code> 속성을 지정하면 이미지를 CORS 모드로 다시 가져온다: 두 번째 요청은 <code>Origin</code> 헤더를 싣고 새
     * <code>load</code> 이벤트 뒤에는 캔버스가 화소를 읽을 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAttributeChangeFetchesAgain() throws Exception {
        TestHttpServer home = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] data = png(0x336699);
            other.route("/gated.png", image(data, "echo", false));
            other.start();
            String script = "<script>window.addEventListener('load', function () { var i = document.getElementById('a');"
                    + " sessionStorage.setItem('before', String(i.complete)); i.addEventListener('load', function () { var c = document.createElement('canvas'); c.width = 8; c.height = 8;"
                    + " var x = c.getContext('2d'); x.drawImage(i, 0, 0); var r; try { var d = x.getImageData(0, 0, 1, 1).data; r = 'clean:' + d[0] + ',' + d[1] + ',' + d[2]; } catch (e) { r = e.name; }"
                    + " sessionStorage.setItem('after', r); }); i.setAttribute('crossorigin', 'anonymous'); });</script>";
            String page = "<!doctype html><html><body><img id=a src='" + other.url("/gated.png") + "'>" + script + "</body></html>";
            home.routeFixed("/page", TestHttpServer.Response.html(page));
            home.start();
            panel.setPage(home.url("/page"));
            Check.equal("true", awaitStored(panel, "before"), "the image had loaded before the attribute was set");
            Check.equal("clean:51,102,153", awaitStored(panel, "after"), "after the attribute the image is fetched in CORS mode and the canvas stays clean");
            Check.equal(2, other.count("/gated.png"), "the image was fetched twice: once plain, once with CORS");
            String origin = home.url("/page").substring(0, home.url("/page").length() - "/page".length());
            Check.equal(origin, other.last("/gated.png").header("Origin"), "the second request carried the origin of the page");
        } finally {
            panel.dispose();
            home.stop();
            other.stop();
        }
    }

    /**
     * What the component paints: an image that the server granted is painted from the CORS fetch (one request with <code>Origin</code>, no
     * second plain fetch), an image that it denied shows the broken-image notice instead of the picture, and a plain image of the same address
     * is painted from its own fetch.
     * <p>
     * 컴포넌트가 그리는 것: 서버가 허락한 이미지는 CORS 가져오기로 그려지고(<code>Origin</code> 이 있는 요청 하나, 일반 가져오기가 하나 더 있지 않다), 허락하지 않은 이미지는 그림
     * 대신 깨진 이미지 안내를 보여 주며, 같은 주소의 일반 이미지는 자신의 가져오기로 그려진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPainting() throws Exception {
        TestHttpServer home = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] data = png(0x336699);
            other.route("/gated.png", image(data, "echo", false));
            other.route("/closed.png", image(data, null, false));
            other.route("/closed2.png", image(data, null, false));
            other.start();
            String page = "<!doctype html><html><head><style>html,body{margin:0;padding:0}img{display:block;width:8px;height:8px}</style></head><body>"
                    + "<img id=gated crossorigin src='" + other.url("/gated.png") + "'>" + "<img id=plain src='" + other.url("/closed.png") + "'>"
                    + "<img id=closed crossorigin src='" + other.url("/closed2.png") + "'>" + SCRIPT + "</body></html>";
            home.routeFixed("/page", TestHttpServer.Response.html(page));
            home.start();
            panel.setPage(home.url("/page"));
            Check.equal("yes", awaitStored(panel, "done"), "the window load event fired");
            long end = System.currentTimeMillis() + 20000;
            // 그려진 화소가 이미지 색이 될 때까지 기다린다.
            while ((snapshotColor(panel, 2, 2) != 0x336699 || snapshotColor(panel, 2, 10) != 0x336699) && System.currentTimeMillis() < end) {
                Thread.sleep(15);
            }
            Check.equal(0x336699, snapshotColor(panel, 2, 2), "the granted crossorigin image is painted");
            Check.equal(0x336699, snapshotColor(panel, 2, 10), "the plain image of another address is painted from its own fetch");
            Check.equal(true, snapshotColor(panel, 2, 18) != 0x336699, "the denied crossorigin image is not painted");
            Check.equal(1, other.count("/gated.png"), "the granted image was fetched once (in CORS mode)");
            Check.equal(1, other.count("/closed.png"), "the plain image was fetched once");
            Check.equal(1, other.count("/closed2.png"), "the denied image was fetched once");
        } finally {
            panel.dispose();
            home.stop();
            other.stop();
        }
    }

    /**
     * Reads the color of a pixel of the painted component.
     * <p>
     * 그려진 컴포넌트의 화소 색을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param x     x / x
     * @param y     y / y
     * @return the RGB color / RGB 색
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int snapshotColor(final SwingWebPanel panel, final int x, final int y) throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an image without a native window and reads the pixel.
             * <p>
             * 네이티브 창 없이 이미지에 그려 화소를 읽는다.
             */
            public void run() {
                BufferedImage image = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                result[0] = image.getRGB(x, y) & 0xFFFFFF;
            }
        });
        return result[0];
    }

    /**
     * A page without an origin (made with <code>setContent</code>) sends <code>Origin: null</code> with its cross-origin image requests (the
     * wildcard and the answer <code>null</code> grant it, nothing else does).
     * <p>
     * 오리진이 없는 페이지(<code>setContent</code> 로 만든)는 교차 오리진 이미지 요청에 <code>Origin: null</code> 을 싣는다(와일드카드와 <code>null</code> 이라는 답이
     * 허용하고 그 밖에는 허용하지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageWithoutOrigin() throws Exception {
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] data = png(0x336699);
            other.route("/wild.png", image(data, "*", false));
            other.route("/echo.png", image(data, "echo", false));
            other.start();
            panel.setContent("<!doctype html><html><head><title>t</title></head><body><img id=a crossorigin src='" + other.url("/wild.png") + "'>"
                    + "<img id=b crossorigin src='" + other.url("/echo.png") + "'><script>var n = 0; function mark(e) { n++; document.title = 'events' + n; }"
                    + " var imgs = document.getElementsByTagName('img'); for (var i = 0; i < imgs.length; i++) { imgs[i].addEventListener('load', mark); imgs[i].addEventListener('error', mark); }</script></body></html>",
                    "text/html");
            long end = System.currentTimeMillis() + 20000;
            // 두 이미지의 요청이 서버에 닿을 때까지 기다린다.
            while ((other.count("/wild.png") == 0 || other.count("/echo.png") == 0) && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal("null", other.last("/wild.png").header("Origin"), "a page without an origin sends Origin: null");
            Check.equal("null", other.last("/echo.png").header("Origin"), "also for the other image");
        } finally {
            panel.dispose();
            other.stop();
        }
    }
}
