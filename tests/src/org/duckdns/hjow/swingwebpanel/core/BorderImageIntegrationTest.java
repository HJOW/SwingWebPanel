package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The component-level check of <code>border-image</code> with real HTTP (plan item P10-08): the source image arrives late, so the border styles are painted first and the
 * border image replaces them after the image arrived (the layout and the display list are rebuilt once the image is decoded); the request is shared and the received
 * source is unchanged; a source that fails to load leaves the border styles in force.
 * <p>
 * 실제 HTTP 로 <code>border-image</code> 를 컴포넌트 수준에서 확인한다(계획 P10-08): 원본 이미지가 늦게 도착하므로 border 스타일이 먼저 그려지고 이미지가 도착한 뒤 border 이미지가
 * 그것을 대신한다(이미지를 디코딩하면 레이아웃과 디스플레이 리스트를 다시 만든다). 요청은 공유되고 받은 원문은 바뀌지 않으며, 가져오기에 실패한 원본은 border 스타일을 그대로 둔다.
 */
public class BorderImageIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BorderImageIntegrationTest() {
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @param width  the width / 너비
     * @param height the height / 높이
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel(final int width, final int height) throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(width, height);
            }
        });
        return result[0];
    }

    /**
     * Paints the component into an image on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그린다.
     *
     * @param panel the component / 컴포넌트
     * @return the image / 이미지
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static BufferedImage snapshot(final SwingWebPanel panel) throws Exception {
        final BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
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
                    g.fillRect(0, 0, img.getWidth(), img.getHeight());
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
        return img;
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
     * Makes the bytes of a 3 by 3 PNG image: red corners and a green rest.
     * <p>
     * 3x3 PNG 이미지의 바이트를 만든다: 모서리는 빨강, 나머지는 초록.
     *
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] frame() throws Exception {
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        // 모서리 네 칸은 빨강, 나머지는 초록으로 채운다.
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                boolean corner = (x == 0 || x == 2) && (y == 0 || y == 2);
                image.setRGB(x, y, corner ? 0xFF0000 : 0x00FF00);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Tells whether a pixel of the image has a color.
     * <p>
     * 이미지의 화소가 어떤 색인지 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @param rgb the color / 색
     * @return true if it has the color / 그 색이면 true
     */
    private static boolean is(BufferedImage img, int x, int y, int rgb) {
        return (img.getRGB(x, y) & 0xFFFFFF) == rgb;
    }

    /**
     * A border image that arrives late: before the arrival the black border style is painted, after it the corner is red and the top edge green; there is one request and
     * the received source is unchanged.
     * <p>
     * 늦게 도착하는 border 이미지: 도착 전에는 검은 border 스타일이 그려지고 도착 뒤에는 모서리가 빨갛고 위쪽 가장자리가 초록이다. 요청은 하나이며 받은 원문은 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBorderImageAppearsAfterItArrived() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] data = frame();
        SwingWebPanel panel = newPanel(300, 180);
        try {
            server.route("/f.png", new TestHttpServer.Handler() {
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
            String html = "<!doctype html><html><body style='margin:0'><div style=\"width:60px;height:40px;border:10px solid #000;border-image:url('f.png') 1 fill / 10px\"></div></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            Thread.sleep(200);
            BufferedImage before = snapshot(panel);
            Check.isTrue(is(before, 5, 5, 0x000000), "before the image arrived the border style is painted");
            Check.isTrue(is(before, 40, 5, 0x000000), "also at the top edge");
            release.countDown();
            long end = System.currentTimeMillis() + 30000;
            BufferedImage after = before;
            // 이미지가 도착해 border 이미지가 그려질 때까지 짧게 쉬며 다시 그려 본다.
            while (!is(after, 5, 5, 0xFF0000) && System.currentTimeMillis() < end) {
                Thread.sleep(20);
                after = snapshot(panel);
            }
            Check.isTrue(is(after, 5, 5, 0xFF0000), "after the image arrived the corner is red");
            Check.isTrue(is(after, 40, 5, 0x00FF00), "and the top edge is green");
            Check.isTrue(is(after, 40, 30, 0x00FF00), "and so is the middle (fill)");
            Check.isTrue(is(after, 80, 70, 0xFFFFFF), "outside the box nothing else is painted");
            Check.equal(1, server.count("/f.png"), "the layout and the display list share one request");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A border image that cannot be fetched (404) leaves the border styles in force.
     * <p>
     * 가져올 수 없는(404) border 이미지는 border 스타일을 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailedImageKeepsTheBorderStyles() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel(300, 180);
        try {
            String html = "<!doctype html><html><body style='margin:0'><div style=\"width:60px;height:40px;border:10px solid #00f;border-image:url('gone.png') 1\"></div></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            long end = System.currentTimeMillis() + 10000;
            // 요청이 실패로 끝날 때까지 기다린다(서버가 404 를 한 번 센다).
            while (server.count("/gone.png") == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Thread.sleep(300);
            BufferedImage img = snapshot(panel);
            Check.isTrue(is(img, 5, 5, 0x0000FF), "the blue border style is still painted");
            Check.isTrue(is(img, 40, 5, 0x0000FF), "also at the top edge");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
