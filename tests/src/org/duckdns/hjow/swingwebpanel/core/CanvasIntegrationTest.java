package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The canvas through the real component (plan items P20-05, P00-08): what a script draws on a <code>canvas</code> shows up in the painted
 * component (pixels read from an offscreen paint of the component), scaled by CSS, over the background of the canvas, without the fallback
 * content, redrawn by animation frames and after a resize, receiving mouse events; WebGL and WebGPU are not available to the page; the
 * origin-clean rule works with real HTTP responses (a foreign image taints, <code>crossorigin</code> with permission does not); and the
 * pictures are gone with the page, <code>clean()</code> and <code>dispose()</code>.
 * <p>
 * 실제 컴포넌트를 통한 캔버스(계획 P20-05, P00-08): 스크립트가 <code>canvas</code> 에 그린 것이 그려진 컴포넌트(컴포넌트를 화면 밖에 그려 읽은 화소)에 보이고,
 * CSS 로 늘어나고, 캔버스의 배경 위에 놓이고, 대체 콘텐츠 없이, 애니메이션 프레임과 크기 변경 뒤에 다시 그려지고, 마우스 이벤트를 받는다. 페이지는 WebGL 과
 * WebGPU 를 쓸 수 없다. origin-clean 규칙이 실제 HTTP 응답으로 동작한다(외부 이미지는 오염시키고, 허락이 있는 <code>crossorigin</code> 은 그렇지 않다).
 * 그림은 페이지, <code>clean()</code>, <code>dispose()</code> 와 함께 사라진다.
 */
public class CanvasIntegrationTest {

    /**
     * The style that removes the margins of the page so that the pixels of the tests have known positions.
     * <p>
     * 시험 화소의 위치를 알 수 있도록 페이지의 여백을 없애는 스타일.
     */
    private static final String RESET = "<style>html,body{margin:0;padding:0}canvas{display:block}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasIntegrationTest() {
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
     * Paints the component on the EDT into an image and returns the image.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그려 그 이미지를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the picture / 그림
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static BufferedImage snapshot(final SwingWebPanel panel) throws Exception {
        final BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an ARGB image without a native window.
             * <p>
             * 네이티브 창 없이 ARGB 이미지에 그린다.
             */
            public void run() {
                BufferedImage image = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                result[0] = image;
            }
        });
        return result[0];
    }

    /**
     * Waits until a pixel of the painted component has a color.
     * <p>
     * 그려진 컴포넌트의 화소가 어떤 색이 될 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the expected color / 기대한 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPixel(SwingWebPanel panel, int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int actual = 0;
        // 문서 작업과 EDT 게시가 끝날 때까지 짧게 쉬며 그려진 화소를 확인한다.
        do {
            actual = snapshot(panel).getRGB(x, y) & 0xFFFFFF;
            if (actual == rgb) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() < end);
        Check.equal(Integer.toHexString(rgb), Integer.toHexString(actual), message);
    }

    /**
     * Reads the color of a pixel of the painted component now.
     * <p>
     * 그려진 컴포넌트의 화소 색을 지금 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param x     x / x
     * @param y     y / y
     * @return the RGB color / RGB 색
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int pixel(SwingWebPanel panel, int x, int y) throws Exception {
        return snapshot(panel).getRGB(x, y) & 0xFFFFFF;
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
        Check.equal(true, stored(panel, key) != null, "the page stored " + key);
        return stored(panel, key);
    }

    /**
     * Serves a page over HTTP and loads it (pages made with <code>setContent</code> have an opaque origin and no storage, so a page that must
     * report through the session storage needs a real origin).
     * <p>
     * 페이지를 HTTP 로 제공하고 로드한다(<code>setContent</code> 로 만든 페이지는 오리진이 불투명하여 저장소가 없으므로 세션 저장소로 보고해야 하는 페이지에는 실제
     * 오리진이 필요하다).
     *
     * @param panel  the component / 컴포넌트
     * @param server the server (not started yet) / 서버(아직 시작 전)
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(SwingWebPanel panel, TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.start();
        panel.setPage(server.url("/page"));
    }

    /**
     * Makes the bytes of a PNG image of one color.
     * <p>
     * 한 가지 색의 PNG 이미지 바이트를 만든다.
     *
     * @param w   the width / 너비
     * @param h   the height / 높이
     * @param rgb the color / 색
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png(int w, int h, int rgb) throws Exception {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // 모든 화소를 같은 색으로 채운다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * What a script draws is painted by the component at the position of the canvas; the source that <code>getContent()</code> returns is the
     * received markup, not a serialization of the DOM.
     * <p>
     * 스크립트가 그린 것은 컴포넌트가 캔버스의 위치에 그리며, <code>getContent()</code> 가 돌려주는 원문은 DOM 의 직렬화가 아니라 받은 마크업이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDrawingIsPainted() throws Exception {
        SwingWebPanel panel = newPanel();
        try {
            String html = "<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=50></canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 50, 50); ctx.fillStyle = '#0000ff'; ctx.fillRect(50, 0, 50, 50);</script></body></html>";
            panel.setContent(html, "text/html");
            awaitPixel(panel, 10, 10, 0xFF0000, "the left half is red");
            Check.equal(0x0000FF, pixel(panel, 60, 10), "the right half is blue");
            Check.equal(0xFFFFFF, pixel(panel, 120, 10), "right of the canvas the page is white");
            Check.equal(0xFFFFFF, pixel(panel, 10, 60), "below the canvas the page is white");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
        }
    }

    /**
     * The bitmap is scaled to the CSS size of the box, the background of the canvas shows through transparent pixels, and the fallback
     * content between the tags is not shown.
     * <p>
     * 비트맵은 상자의 CSS 크기로 늘어나고, 캔버스의 배경은 투명한 화소로 비쳐 보이며, 태그 사이의 대체 콘텐츠는 보이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScalingBackgroundAndFallback() throws Exception {
        SwingWebPanel panel = newPanel();
        try {
            panel.setContent("<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=50 style='width:200px;height:100px;background:#ffff00'>"
                    + "FALLBACK TEXT FALLBACK TEXT</canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 50, 50);</script></body></html>", "text/html");
            awaitPixel(panel, 20, 20, 0xFF0000, "the left half of the bitmap covers the left 100 pixels of the 200 pixel box");
            Check.equal(0xFF0000, pixel(panel, 90, 80), "down to the bottom of the box");
            Check.equal(0xFFFF00, pixel(panel, 150, 20), "the background of the canvas shows where it is transparent");
            Check.equal(0xFFFFFF, pixel(panel, 210, 20), "the box is 200 pixels wide");
            Check.equal(0xFFFFFF, pixel(panel, 20, 110), "and 100 pixels tall");
            BufferedImage image = snapshot(panel);
            boolean dark = false;
            // 상자 안에서 글자처럼 어두운 화소가 있는지 찾는다. 배경이 노랑이므로 어두운 화소는 대체 텍스트이다.
            for (int y = 0; y < 100 && !dark; y++) {
                for (int x = 100; x < 200 && !dark; x++) {
                    int rgb = image.getRGB(x, y) & 0xFFFFFF;
                    dark = (rgb >> 16 & 0xFF) < 100 && (rgb >> 8 & 0xFF) < 100;
                }
            }
            Check.equal(false, dark, "the fallback text is not painted");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Animation frames redraw the canvas: the last frame is painted, and the picture is repainted without the page being laid out again for
     * every frame.
     * <p>
     * 애니메이션 프레임이 캔버스를 다시 그린다: 마지막 프레임이 그려지며, 프레임마다 페이지를 다시 배치하지 않고 그림만 다시 그린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationFramesRedraw() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><html><body>" + RESET + "<canvas id=c width=60 height=60></canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "var n = 0; function frame() { n++; ctx.fillStyle = (n % 2) ? '#ff0000' : '#0000ff'; ctx.fillRect(0, 0, 60, 60);"
                    + " if (n < 40) { requestAnimationFrame(frame); } else { sessionStorage.setItem('frames', String(n)); } } requestAnimationFrame(frame);</script></body></html>");
            Check.equal("40", awaitStored(panel, "frames"), "forty frames ran");
            awaitPixel(panel, 30, 30, 0x0000FF, "the last (even) frame is blue");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Setting the size of a canvas lays the page out again with the new size and shows an empty bitmap.
     * <p>
     * 캔버스의 크기를 지정하면 새 크기로 페이지를 다시 배치하고 빈 비트맵을 보여 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testResizeRelayout() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=40 style='background:#ffff00'></canvas><script>"
                    + "var cv = document.getElementById('c'); var ctx = cv.getContext('2d'); ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 100, 40);"
                    + "setTimeout(function () { cv.width = 200; sessionStorage.setItem('done', '1'); }, 50);</script></body></html>");
            awaitStored(panel, "done");
            awaitPixel(panel, 150, 20, 0xFFFF00, "the canvas is 200 pixels wide now, and its new bitmap is empty (the yellow background shows)");
            Check.equal(0xFFFF00, pixel(panel, 20, 20), "the old drawing is gone");
            Check.equal(0xFFFFFF, pixel(panel, 250, 20), "beyond the new width");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A click on the canvas reaches its listeners with the position relative to the page.
     * <p>
     * 캔버스를 누르면 리스너가 페이지에 상대적인 위치와 함께 이벤트를 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMouseEvents() throws Exception {
        final SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><html><body>" + RESET + "<div style='height:20px'></div><canvas id=c width=100 height=50></canvas><script>"
                    + "var cv = document.getElementById('c'); var ctx = cv.getContext('2d'); ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 100, 50);"
                    + "cv.addEventListener('click', function (e) { sessionStorage.setItem('click', e.target.id + ':' + e.clientX + ',' + e.clientY + ':' + e.offsetX + ',' + e.offsetY); });"
                    + "</script></body></html>");
            awaitPixel(panel, 10, 30, 0x00FF00, "the canvas is painted 20 pixels below the top");
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Delivers a click on the canvas to the component.
                 * <p>
                 * 캔버스를 누르는 이벤트를 컴포넌트에 전달한다.
                 */
                public void run() {
                    panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 40, 45, 1, false, MouseEvent.BUTTON1));
                }
            });
            Check.equal("c:40,45:40,25", awaitStored(panel, "click"), "the click reached the canvas element: the page position, and the position inside the canvas (offsetX/offsetY), which scripts use as canvas coordinates");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The page cannot get a 3D context: WebGL, WebGL 2, WebGPU and unknown types answer null, the interface objects and
     * <code>navigator.gpu</code> do not exist, and the 2D context works next to them.
     * <p>
     * 페이지는 3D 컨텍스트를 얻을 수 없다: WebGL, WebGL 2, WebGPU, 모르는 형식은 null 로 답하고, 인터페이스 객체와 <code>navigator.gpu</code> 는 없으며, 2D
     * 컨텍스트는 그 옆에서 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoThreeDimensionalContext() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><html><body><canvas id=c width=10 height=10></canvas><script>var c = document.getElementById('c');"
                    + "var r = [c.getContext('webgl') === null, c.getContext('experimental-webgl') === null, c.getContext('webgl2') === null,"
                    + " c.getContext('webgpu') === null, typeof WebGLRenderingContext, typeof WebGL2RenderingContext, typeof navigator.gpu,"
                    + " typeof c.getContext('2d').fillRect];"
                    + "sessionStorage.setItem('gl', r.join());</script></body></html>");
            Check.equal("true,true,true,true,undefined,undefined,undefined,function", awaitStored(panel, "gl"), "no WebGL, no WebGPU, but a 2D context");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The origin-clean rule with real HTTP responses: an image of another origin taints the canvas (reading the pixels fails with
     * <code>SecurityError</code>), the same image with <code>crossorigin</code> and an <code>Access-Control-Allow-Origin</code> header does not,
     * and an image of the same origin does not.
     * <p>
     * 실제 HTTP 응답으로 보는 origin-clean 규칙: 다른 오리진의 이미지는 캔버스를 오염시키고(화소를 읽으면 <code>SecurityError</code> 로 실패), 같은 이미지라도
     * <code>crossorigin</code> 과 <code>Access-Control-Allow-Origin</code> 헤더가 있으면 그렇지 않으며, 같은 오리진의 이미지도 그렇지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTaintWithRealResponses() throws Exception {
        TestHttpServer home = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            byte[] image = png(8, 8, 0x336699);
            home.route("/same.png", fixedImage(image, false));
            other.route("/foreign.png", fixedImage(image, false));
            other.route("/open.png", fixedImage(image, true));
            other.start();
            String script = "<script>function test(id, key) { var i = document.getElementById(id); function done() { var c = document.createElement('canvas'); c.width = 8; c.height = 8;"
                    + " var x = c.getContext('2d'); x.drawImage(i, 0, 0); var r; try { x.getImageData(0, 0, 1, 1); r = 'clean'; } catch (e) { r = e.name; }"
                    + " try { c.toDataURL(); r += '+url'; } catch (e) { r += '+' + e.name; } sessionStorage.setItem(key, r); }"
                    + " if (i.complete) { if (i.naturalWidth === 0) { sessionStorage.setItem(key, 'error'); } else { done(); } } else { i.addEventListener('load', done); i.addEventListener('error', function () { sessionStorage.setItem(key, 'error'); }); } }"
                    + "window.addEventListener('load', function () { test('a', 'same'); test('b', 'foreign'); test('c', 'open'); test('d', 'closed'); });</script>";
            String page = "<!doctype html><html><body><img id=a src='/same.png'><img id=b src='" + other.url("/foreign.png") + "'>"
                    + "<img id=c crossorigin src='" + other.url("/open.png") + "'><img id=d crossorigin src='" + other.url("/foreign.png") + "'>" + script + "</body></html>";
            home.routeFixed("/page", TestHttpServer.Response.html(page));
            home.start();
            panel.setPage(home.url("/page"));
            Check.equal("clean+url", awaitStored(panel, "same"), "an image of the same origin does not taint");
            Check.equal("SecurityError+SecurityError", awaitStored(panel, "foreign"), "an image of another origin taints: no pixels, no data URL");
            Check.equal("clean+url", awaitStored(panel, "open"), "crossorigin with Access-Control-Allow-Origin keeps the canvas clean");
            Check.equal("error", awaitStored(panel, "closed"), "crossorigin without permission: the image does not load (its error event fires), as in a browser");
        } finally {
            panel.dispose();
            home.stop();
            other.stop();
        }
    }

    /**
     * Makes a handler that serves a PNG, optionally with a CORS header.
     * <p>
     * PNG 를 제공하는 핸들러를 만든다. CORS 헤더를 붙일 수 있다.
     *
     * @param data the file bytes / 파일 바이트
     * @param cors true to answer <code>Access-Control-Allow-Origin: *</code> / <code>Access-Control-Allow-Origin: *</code> 로 답하려면 true
     * @return the handler / 핸들러
     */
    private static TestHttpServer.Handler fixedImage(final byte[] data, final boolean cors) {
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
                if (cors) {
                    r.header("Access-Control-Allow-Origin", "*");
                }
                return r;
            }
        };
    }

    /**
     * The picture belongs to the page: the next page, <code>clean()</code> and <code>dispose()</code> remove it, and an old script cannot paint
     * into the next page.
     * <p>
     * 그림은 페이지의 것이다: 다음 페이지, <code>clean()</code>, <code>dispose()</code> 가 그것을 없애고, 옛 스크립트는 다음 페이지에 그릴 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPictureIsGoneWithThePage() throws Exception {
        SwingWebPanel panel = newPanel();
        try {
            panel.setContent("<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=50></canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 100, 50); setTimeout(function () { ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 100, 50); }, 400);</script></body></html>",
                    "text/html");
            awaitPixel(panel, 10, 10, 0xFF0000, "the first page shows its drawing");
            panel.setContent("<!doctype html><html><body>" + RESET + "<div style='height:50px;background:#808080'></div></body></html>", "text/html");
            awaitPixel(panel, 10, 10, 0x808080, "the next page replaces it");
            Thread.sleep(700);
            Check.equal(0x808080, pixel(panel, 10, 10), "the timer of the old page did not paint into the new one");
            panel.setContent("<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=50></canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 100, 50);</script></body></html>", "text/html");
            awaitPixel(panel, 10, 10, 0x0000FF, "a new canvas of the new page");
            panel.clean();
            awaitPixel(panel, 10, 10, 0xFFFFFF, "clean() removes the picture");
            panel.setContent("<!doctype html><html><body>" + RESET + "<canvas id=c width=100 height=50></canvas><script>var ctx = document.getElementById('c').getContext('2d');"
                    + "ctx.fillStyle = '#ff00ff'; ctx.fillRect(0, 0, 100, 50);</script></body></html>", "text/html");
            awaitPixel(panel, 10, 10, 0xFF00FF, "the component works after clean()");
            panel.dispose();
            TestSupport.flushEdt();
            Check.equal(0xFFFFFF, pixel(panel, 10, 10), "dispose() removes the picture");
        } finally {
            panel.dispose();
        }
    }

    /**
     * A canvas bigger than the limits is harmless: the page works, the box has the size of the attributes, nothing is drawn.
     * <p>
     * 한도보다 큰 캔버스는 해롭지 않다: 페이지는 동작하고, 상자는 속성의 크기를 가지며, 아무것도 그려지지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHugeCanvasIsHarmless() throws Exception {
        SwingWebPanel panel = newPanel();
        TestHttpServer server = new TestHttpServer();
        try {
            serve(panel, server, "<!doctype html><html><body>" + RESET + "<canvas id=c width=30000 height=100 style='width:100px;background:#ffff00'></canvas><script>"
                    + "var c = document.getElementById('c'); var x = c.getContext('2d'); x.fillStyle = '#ff0000'; x.fillRect(0, 0, 50, 50); var r;"
                    + "try { x.getImageData(0, 0, 1, 1); r = 'read'; } catch (e) { r = e.name; } sessionStorage.setItem('huge', r + ':' + c.width + ':' + c.toDataURL());</script></body></html>");
            Check.equal("InvalidStateError:30000:data:,", awaitStored(panel, "huge"), "no pixels can be read and nothing is exported");
            awaitPixel(panel, 10, 10, 0xFFFF00, "the box is there and nothing was drawn on it");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
