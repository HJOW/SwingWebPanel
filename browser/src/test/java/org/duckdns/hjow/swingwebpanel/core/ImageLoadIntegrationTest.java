package org.duckdns.hjow.swingwebpanel.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The image loading of a page through the real component and real HTTP (plan items P18-05, P15-04-b-2): the <code>load</code> and
 * <code>error</code> events of <code>img</code> elements, the natural size that scripts read, and the delay of the window <code>load</code>
 * event until the images of the document ended, while <code>DOMContentLoaded</code> and the confirmation of the page do not wait for them.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 페이지의 이미지 로딩을 확인하는 시험(계획 P18-05, P15-04-b-2): <code>img</code> 요소의 <code>load</code>,
 * <code>error</code> 이벤트, 스크립트가 읽는 자연 크기, 문서의 이미지가 끝날 때까지의 window <code>load</code> 이벤트 지연. 단
 * <code>DOMContentLoaded</code> 와 페이지 확정은 이미지를 기다리지 않는다.
 */
public class ImageLoadIntegrationTest {

    /**
     * The scripts of the test pages: <code>mark(name)</code> appends a name to the order stored in the sessionStorage of the component.
     * <p>
     * 시험 페이지의 스크립트: <code>mark(name)</code> 는 컴포넌트의 sessionStorage 에 저장된 순서에 이름을 덧붙인다.
     */
    private static final String SCRIPT = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}"
            + "document.addEventListener('DOMContentLoaded',function(){mark('dcl');});"
            + "window.addEventListener('load',function(){mark('win');});</script>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageLoadIntegrationTest() {
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
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads the order that the scripts stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 순서를 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @return the order, empty if nothing was stored / 순서. 저장된 것이 없으면 빈 문자열
     */
    private static String order(SwingWebPanel panel) {
        // 복사한 저장소 항목에서 순서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if ("order".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    /**
     * Waits until the stored order equals the expected one.
     * <p>
     * 저장된 순서가 기대한 값이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param expected the order / 기대한 순서
     * @param message  the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitOrder(SwingWebPanel panel, String expected, String message) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (!expected.equals(order(panel)) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(expected, order(panel), message);
    }

    /**
     * Waits until the component confirmed a page.
     * <p>
     * 컴포넌트가 페이지를 확정할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url   the URL / URL
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Makes the bytes of a PNG image.
     * <p>
     * PNG 이미지의 바이트를 만든다.
     *
     * @param width  the width / 너비
     * @param height the height / 높이
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * A slow image: the page is confirmed and <code>DOMContentLoaded</code> fires without waiting for it, the window <code>load</code> event
     * comes after the <code>load</code> event of the image, scripts read the natural size, and the layout and the load of the event share one
     * request.
     * <p>
     * 느린 이미지: 페이지는 이미지를 기다리지 않고 확정되고 <code>DOMContentLoaded</code> 가 발생하며, window <code>load</code> 이벤트는 이미지의
     * <code>load</code> 이벤트 뒤에 오고, 스크립트가 자연 크기를 읽으며, 레이아웃과 이벤트용 로드가 요청 하나를 공유한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWindowLoadWaitsForSlowImage() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] data = png(7, 5);
        SwingWebPanel panel = newPanel();
        try {
            server.route("/slow.png", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the image / 이미지
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(30, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("image/png", data);
                }
            });
            String html = "<!doctype html><html><body>" + SCRIPT
                    + "<img id=i src='slow.png' onload=\"mark('img:'+this.naturalWidth+'x'+this.naturalHeight+':'+this.complete)\""
                    + " onerror=\"mark('img-error')\"></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            awaitOrder(panel, "dcl,", "DOMContentLoaded does not wait for the image");
            // 이미지가 아직 도착하지 않았으므로 잠시 기다려도 load 는 발생하지 않아야 한다.
            Thread.sleep(300);
            Check.equal("dcl,", order(panel), "the window load event waits for the image");
            release.countDown();
            awaitOrder(panel, "dcl,img:7x5:true,win,", "the image event comes first, then the window load event");
            Check.equal(1, server.count("/slow.png"), "layout and the load event share one request");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Images that cannot be shown (a missing file and bytes that are no image) fire <code>error</code> and do not hold the window
     * <code>load</code> event back for ever.
     * <p>
     * 보여 줄 수 없는 이미지(없는 파일과 이미지가 아닌 바이트)는 <code>error</code> 를 발생시키고 window <code>load</code> 이벤트를 영원히
     * 붙잡지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenImagesFireErrorAndDoNotBlockLoad() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/missing.png", new TestHttpServer.Response(404).text("text/plain", "no"));
            server.routeFixed("/bad.png", new TestHttpServer.Response(200).bytes("image/png", "this is not an image".getBytes("US-ASCII")));
            server.routeFixed("/good.png", new TestHttpServer.Response(200).bytes("image/png", png(3, 2)));
            String html = "<!doctype html><html><body>" + SCRIPT
                    + "<img src='missing.png' onload=\"mark('missing-load')\" onerror=\"mark('missing-error')\">"
                    + "<img src='bad.png' onload=\"mark('bad-load')\" onerror=\"mark('bad-error')\">"
                    + "<img src='good.png' onload=\"mark('good')\" onerror=\"mark('good-error')\">"
                    + "</body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            long end = System.currentTimeMillis() + 60000;
            // window load 가 발생할 때까지(모든 이미지가 끝날 때까지) 짧게 쉬며 확인한다.
            while (order(panel).indexOf("win,") < 0 && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            String order = order(panel);
            Check.isTrue(order.endsWith("win,"), "the window load event comes after every image event: " + order);
            Check.isTrue(order.indexOf("missing-error,") >= 0, "a missing file fires error: " + order);
            Check.isTrue(order.indexOf("bad-error,") >= 0, "bytes that are no image fire error: " + order);
            Check.isTrue(order.indexOf("good,") >= 0, "the good image fires load: " + order);
            Check.isTrue(order.indexOf("-load,") < 0, "no failed image fires load: " + order);
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
