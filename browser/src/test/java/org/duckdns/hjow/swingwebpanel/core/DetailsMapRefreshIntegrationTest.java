package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests through the real component of three behaviors of old and new HTML that did not work before (plan items P20-01 and P20-11):
 * <code>details</code> and <code>summary</code> (closed content is hidden, a click on the summary opens it, the <code>toggle</code> event),
 * client-side image maps (a click on an <code>area</code> follows its link) and <code>&lt;meta http-equiv=refresh&gt;</code> (the page moves by
 * itself).
 * <p>
 * 이전에는 동작하지 않던 옛 HTML 과 새 HTML 의 세 동작을 실제 컴포넌트로 시험한다(계획 P20-01, P20-11): <code>details</code> 와 <code>summary</code>(닫힌 내용 숨김, summary 를
 * 누르면 열림, <code>toggle</code> 이벤트), 클라이언트 측 이미지 맵(<code>area</code> 를 누르면 그 링크로 이동), <code>&lt;meta http-equiv=refresh&gt;</code>(페이지가 스스로
 * 이동하며 JavaScript 를 꺼도 동작).
 */
public class DetailsMapRefreshIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The component of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private SwingWebPanel panel;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DetailsMapRefreshIntegrationTest() {
    }

    /**
     * Starts the server and the component.
     * <p>
     * 서버와 컴포넌트를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/target1", TestHttpServer.Response.html("<p>target one</p>"));
        server.routeFixed("/target2", TestHttpServer.Response.html("<p>target two</p>"));
        server.routeFixed("/pic.png", new TestHttpServer.Response(200).bytes("image/png", png(100, 100, 0xFFFF00)));
        server.start();
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component on the EDT.
             * <p>
             * EDT 에서 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(300, 200);
            }
        });
        panel = holder[0];
    }

    /**
     * Disposes the component and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        panel.dispose();
        server.stop();
    }

    /**
     * Creates a PNG of one color.
     * <p>
     * 한 가지 색의 PNG 를 만든다.
     *
     * @param w   the width / 너비
     * @param h   the height / 높이
     * @param rgb the color / 색
     * @return the file / 파일
     * @throws Exception on failure / 실패 시
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
     * Returns a value of the session storage by key, or null.
     * <p>
     * 세션 저장소에서 키로 값을 찾는다. 없으면 null.
     *
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private String session(String key) {
        Collection<StorageEntry> entries = panel.getSessionStorage();
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the component confirmed an address and finished loading.
     * <p>
     * 컴포넌트가 주소를 확정하고 로딩을 끝낼 때까지 기다린다.
     *
     * @param url the address / 주소
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void await(String url) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Waits until a value of the session storage has a text.
     * <p>
     * 세션 저장소의 값이 어떤 문자열이 될 때까지 기다린다.
     *
     * @param key   the key / 키
     * @param value the wanted value / 원하는 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitSession(String key, String value) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (!value.equals(session(key)) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(value, session(key), "session storage " + key);
    }

    /**
     * Paints the component on the EDT and returns the color of a pixel.
     * <p>
     * 컴포넌트를 EDT 에서 그리고 한 화소의 색을 반환한다.
     *
     * @param x x / x
     * @param y y / y
     * @return the RGB color / RGB 색
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private int pixel(final int x, final int y) throws Exception {
        final int[] v = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an image without a native window.
             * <p>
             * 네이티브 창 없이 이미지에 그린다.
             */
            public void run() {
                BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                v[0] = image.getRGB(x, y) & 0xFFFFFF;
            }
        });
        return v[0];
    }

    /**
     * Paints the component and tells whether a rectangle of the picture contains a pixel of a color.
     * <p>
     * 컴포넌트를 그리고 그림의 사각형 안에 어떤 색의 화소가 있는지 알려 준다.
     *
     * @param x1  left / 왼쪽
     * @param y1  top / 위쪽
     * @param x2  right (exclusive) / 오른쪽(미포함)
     * @param y2  bottom (exclusive) / 아래쪽(미포함)
     * @param rgb the color / 색
     * @return true if the color is found / 색이 있으면 true
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private boolean regionHas(int x1, int y1, int x2, int y2, int rgb) throws Exception {
        final BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an image without a native window.
             * <p>
             * 네이티브 창 없이 이미지에 그린다.
             */
            public void run() {
                image[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image[0].createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
        // 사각형의 화소를 하나씩 확인한다.
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((image[0].getRGB(x, y) & 0xFFFFFF) == rgb) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Waits until a pixel has a color.
     * <p>
     * 화소가 어떤 색이 될 때까지 기다린다.
     *
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the expected color / 기대한 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitPixel(int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int actual = pixel(x, y);
        // 문서 작업과 EDT 게시가 끝날 때까지 짧게 쉬며 그려진 화소를 확인한다.
        while (actual != rgb && System.currentTimeMillis() < end) {
            Thread.sleep(15);
            actual = pixel(x, y);
        }
        Check.equal(Integer.toHexString(rgb), Integer.toHexString(actual), message);
    }

    /**
     * Clicks a point of the component through its event dispatch, as the AWT does.
     * <p>
     * AWT 가 하는 것처럼 컴포넌트의 이벤트 전달로 한 지점을 클릭한다.
     *
     * @param x x / x
     * @param y y / y
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private void click(final int x, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches press, release and click.
             * <p>
             * 누름, 뗌, 클릭을 전달한다.
             */
            public void run() {
                long t = System.currentTimeMillis();
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, t, MouseEvent.BUTTON1_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, t, MouseEvent.BUTTON1_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, t, MouseEvent.BUTTON1_MASK, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * A closed <code>details</code> shows only its first summary; a click on the summary opens it and fires <code>toggle</code>; a script that
     * closes it fires <code>toggle</code> again; text content of the closed element is hidden too.
     * <p>
     * 닫힌 <code>details</code> 는 첫 summary 만 보인다. summary 를 누르면 열리고 <code>toggle</code> 이 발생하며, 스크립트가 닫아도 <code>toggle</code> 이 다시 발생한다.
     * 닫힌 요소의 텍스트 내용도 보이지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDetailsAndSummary() throws Exception {
        server.routeFixed("/details", TestHttpServer.Response.html("<body style=\"margin:0\"><details id=\"d\" style=\"position:absolute;left:0;top:0;width:100px\">"
                + "<summary id=\"s\" style=\"height:20px;background:#00ff00\">sum</summary><div id=\"c\" style=\"height:40px;background:#ff0000\">content</div></details>"
                + "<details id=\"t\" style=\"position:absolute;left:150px;top:0;width:120px;color:#ff0000;font:20px monospace\">"
                + "<summary style=\"height:10px;overflow:hidden;color:#ffffff\">x</summary>HIDDEN TEXT</details>"
                + "<script>var n = 0; var d = document.getElementById('d'); d.addEventListener('toggle', function (e) { n++; sessionStorage.setItem('toggle', n + ':' + d.open + ':' + e.bubbles); });"
                + "sessionStorage.setItem('ready', '1');</script></body>"));
        panel.setPage(server.url("/details"));
        await(server.url("/details"));
        awaitSession("ready", "1");
        awaitPixel(10, 10, 0x00FF00, "the summary of the closed details is shown");
        awaitPixel(10, 40, 0xFFFFFF, "the content of the closed details is hidden");
        Check.isNull(session("toggle"), "no toggle event before any change");
        click(10, 10);
        awaitSession("toggle", "1:true:false");
        awaitPixel(10, 40, 0xFF0000, "the content of the opened details is shown");
        click(10, 10);
        awaitSession("toggle", "2:false:false");
        awaitPixel(10, 40, 0xFFFFFF, "a second click closes it again");
        panel.getContent();
        // 스크립트가 open 을 바꿔도 toggle 이벤트가 발생한다(여는 것과 닫는 것).
        final BrowserSession state = session(panel);
        Check.equal("true", evaluate(state, "d.open = true; String(d.hasAttribute('open'))"), "script opens it");
        awaitSession("toggle", "3:true:false");
        Check.equal("true", evaluate(state, "d.setAttribute('open', 'again'); String(d.open)"), "changing the value of the attribute fires nothing");
        Thread.sleep(300);
        Check.equal("3:true:false", session("toggle"), "no toggle for a change of the value of open");
        Check.isFalse(regionHas(150, 11, 270, 60, 0xFF0000), "the text of a closed details is hidden");
    }

    /**
     * Evaluates a script on the document queue of the page.
     * <p>
     * 페이지의 문서 큐에서 스크립트를 평가한다.
     *
     * @param s    the session / 세션
     * @param code the script / 스크립트
     * @return the result / 결과
     * @throws Exception on failure / 실패 시
     */
    private static String evaluate(final BrowserSession s, final String code) throws Exception {
        final String[] result = new String[1];
        final Throwable[] failure = new Throwable[1];
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        s.submitDocumentTask(new Runnable() {
            /**
             * Runs the script in the realm of the confirmed page.
             * <p>
             * 확정된 페이지의 realm 에서 스크립트를 실행한다.
             */
            public void run() {
                try {
                    java.lang.reflect.Field field = HtmlPageDocument.class.getDeclaredField("runtime");
                    field.setAccessible(true);
                    PageRuntime runtime = (PageRuntime) field.get(s.getSnapshot().getDocument());
                    result[0] = runtime.scripts().evaluate(code, "details-test");
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    done.countDown();
                }
            }
        });
        Check.isTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS), "the query completed");
        Check.isNull(failure[0], "the query failed: " + failure[0]);
        return result[0];
    }

    /**
     * Finds the session of a component by reflection.
     * <p>
     * 컴포넌트의 세션을 리플렉션으로 찾는다.
     *
     * @param p the component / 컴포넌트
     * @return the session / 세션
     * @throws Exception on failure / 실패 시
     */
    private static BrowserSession session(SwingWebPanel p) throws Exception {
        java.lang.reflect.Field field = SwingWebPanel.class.getDeclaredField("session");
        field.setAccessible(true);
        return (BrowserSession) field.get(p);
    }

    /**
     * A click on an <code>area</code> of an image map follows the link of the area: a rectangle, a circle, a polygon, the first matching
     * area wins, and a point outside all areas is a click on the image itself.
     * <p>
     * 이미지 맵의 <code>area</code> 를 누르면 그 링크로 이동한다: 사각형, 원, 다각형, 먼저 일치하는 area 가 우선이며 어느 area 도 아닌 점은 이미지 자체를 누른 것이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImageMap() throws Exception {
        server.routeFixed("/map", TestHttpServer.Response.html("<body style=\"margin:0\"><img id=\"im\" src=\"/pic.png\" usemap=\"#m\" width=\"100\" height=\"100\" "
                + "style=\"position:absolute;left:10px;top:10px\"><map name=\"m\"><area shape=\"rect\" coords=\"0,0,40,100\" href=\"/target1\">"
                + "<area shape=\"circle\" coords=\"70,50,15\" href=\"/target2\"><area shape=\"poly\" coords=\"50,0,100,0,100,20\" href=\"/target3\"></map>"
                + "<script>if (sessionStorage.getItem('clicks') === null) { sessionStorage.setItem('clicks', ''); } document.getElementById('im').addEventListener('click', function () { "
                + "sessionStorage.setItem('clicks', sessionStorage.getItem('clicks') + 'img;'); });"
                + "document.getElementsByTagName('map')[0].addEventListener('click', function (e) { sessionStorage.setItem('clicks', sessionStorage.getItem('clicks') + e.target.tagName + ';'); });</script></body>"));
        server.routeFixed("/target3", TestHttpServer.Response.html("<p>target three</p>"));
        panel.setPage(server.url("/map"));
        await(server.url("/map"));
        awaitPixel(15, 15, 0xFFFF00, "the image is painted");
        click(10 + 20, 10 + 50);
        await(server.url("/target1"));
        Check.equal(3, panel.getHistories().size(), "an area link adds a history entry: " + panel.getHistories());
        panel.back();
        await(server.url("/map"));
        click(10 + 70, 10 + 50);
        await(server.url("/target2"));
        panel.back();
        await(server.url("/map"));
        click(10 + 90, 10 + 5);
        await(server.url("/target3"));
        panel.back();
        await(server.url("/map"));
        // 어느 area 도 아닌 점(원의 바깥)은 이동하지 않는다.
        click(10 + 55, 10 + 90);
        Thread.sleep(400);
        Check.equal(server.url("/map"), panel.getPage(), "a click outside the areas does not move");
        Check.equal(0, server.count("/target1") - 1 + server.count("/target2") - 1 + server.count("/target3") - 1, "each target was requested once");
        awaitSession("clicks", "AREA;AREA;AREA;img;");
    }

    /**
     * <code>&lt;meta http-equiv=refresh content="0;url=..."&gt;</code> replaces the page at once (no history entry for the first page),
     * a time moves after the time, the first meta element wins, an invalid content is ignored.
     * <p>
     * <code>&lt;meta http-equiv=refresh content="0;url=..."&gt;</code> 는 페이지를 곧바로 교체하고(첫 페이지의 이력 항목이 없다), 시간이 있으면 그 뒤에 이동하며, 첫
     * meta 요소가 이기고, 올바르지 않은 content 는 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMetaRefresh() throws Exception {
        server.routeFixed("/r0", TestHttpServer.Response.html("<meta http-equiv=\"refresh\" content=\"0; url=/target1\"><p>refresh now</p>"));
        server.routeFixed("/r1", TestHttpServer.Response.html("<meta http-equiv=\"Refresh\" content=\"1,url='/target2'\"><meta http-equiv=refresh content=\"0;url=/target1\"><p>wait</p>"));
        server.routeFixed("/bad", TestHttpServer.Response.html("<meta http-equiv=\"refresh\" content=\"soon;url=/target1\"><meta http-equiv=\"refresh\" content=\";url=/target1\"><p>stay</p>"));
        server.routeFixed("/blocked", TestHttpServer.Response.html("<meta http-equiv=\"refresh\" content=\"0;url=file:///etc/passwd\"><p>blocked</p>"));
        panel.setPage(server.url("/r0"));
        await(server.url("/target1"));
        Check.equal(2, panel.getHistories().size(), "a refresh of time 0 replaces the entry: " + panel.getHistories());
        panel.setPage(server.url("/r1"));
        await(server.url("/target2"));
        Check.equal(0, server.count("/target1") - 1, "the second meta element was not used");
        panel.setPage(server.url("/bad"));
        await(server.url("/bad"));
        Thread.sleep(1600);
        Check.equal(server.url("/bad"), panel.getPage(), "an invalid content is ignored");
        panel.setPage(server.url("/blocked"));
        await(server.url("/blocked"));
        Thread.sleep(600);
        Check.equal(server.url("/blocked"), panel.getPage(), "a refresh to a blocked scheme does not move the page");
    }
}
