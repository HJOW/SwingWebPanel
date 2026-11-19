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
 * The component-level checks of plan item P10-04-f with real HTTP: the layer of a background image that arrives late appears after it
 * arrived (the layout and the display list are rebuilt once the image is decoded), the request is shared and the received source is
 * unchanged; and <code>::first-line</code> reaches the screen through the real component: the first line is colored and a narrower
 * component breaks the line earlier, so the colored part follows the width.
 * <p>
 * 실제 HTTP 로 계획 P10-04-f 를 컴포넌트 수준에서 확인한다: 늦게 도착하는 배경 이미지의 레이어는 도착한 뒤 나타나고(이미지를 디코딩하면 레이아웃과 디스플레이 리스트를
 * 다시 만든다), 요청은 공유되며 받은 원문은 바뀌지 않는다. <code>::first-line</code> 은 실제 컴포넌트를 거쳐 화면에 도달한다: 첫 줄은 색이 바뀌고 더 좁은 컴포넌트는
 * 줄을 더 일찍 나누므로 색이 바뀐 부분이 너비를 따른다.
 */
public class LateBackgroundIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LateBackgroundIntegrationTest() {
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
     * Makes the bytes of a solid-colored PNG image.
     * <p>
     * 한 가지 색의 PNG 이미지의 바이트를 만든다.
     *
     * @param size the width and the height / 너비와 높이
     * @param rgb  the color / 색
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png(int size, int rgb) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        // 모든 픽셀을 같은 색으로 채운다.
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Counts the pixels of a color in a rectangle.
     * <p>
     * 사각형 안에서 한 색의 픽셀 수를 센다.
     *
     * @param img the image / 이미지
     * @param rgb the color / 색
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return the count / 개수
     */
    private static int count(BufferedImage img, int rgb, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 색이 같은 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * A background image that arrives late: before the arrival the box is white, after it the layers (a tiled red image) are painted;
     * the layout and the later paint share one request and the source is unchanged.
     * <p>
     * 늦게 도착하는 배경 이미지: 도착 전에는 상자가 흰색이고 도착 뒤에는 레이어(타일로 깔린 빨간 이미지)가 그려진다. 레이아웃과 이후 그리기가 요청 하나를 공유하며 원문은
     * 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBackgroundImageAppearsAfterItArrived() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] data = png(8, 0xFF0000);
        SwingWebPanel panel = newPanel(300, 180);
        try {
            server.route("/bg.png", new TestHttpServer.Handler() {
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
            String html = "<!doctype html><html><body style='margin:0'><div id=a style=\"width:60px;height:40px;background:url('bg.png')\"></div></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            Thread.sleep(200);
            BufferedImage before = snapshot(panel);
            Check.equal(0, count(before, 0xFF0000, 0, 0, 100, 60), "before the image arrived the box is empty");
            release.countDown();
            long end = System.currentTimeMillis() + 30000;
            BufferedImage after = before;
            // 이미지가 도착해 레이어가 그려질 때까지 짧게 쉬며 다시 그려 본다.
            while (count(after, 0xFF0000, 0, 0, 100, 60) == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
                after = snapshot(panel);
            }
            Check.equal(60 * 40, count(after, 0xFF0000, 0, 0, 100, 60), "after the image arrived the whole box is tiled with it");
            Check.equal(0, count(after, 0xFF0000, 60, 0, 100, 60), "nothing is painted outside of the box");
            Check.equal(1, server.count("/bg.png"), "the layout and the display list share one request");
            Check.equal(html, panel.getContent(), "the received source is unchanged");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>::first-line</code> through the component: the red text is on the first line only, and a narrower component breaks the line
     * earlier so that less text is red.
     * <p>
     * 컴포넌트를 통한 <code>::first-line</code>: 빨간 글자는 첫 줄에만 있고, 더 좁은 컴포넌트는 줄을 더 일찍 나누어 빨간 글자가 더 적다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFirstLineThroughTheComponent() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel wide = newPanel(400, 120);
        SwingWebPanel narrow = newPanel(150, 120);
        try {
            String html = "<!doctype html><html><head><style>body{margin:0;font:14px monospace} p::first-line{color:#f00;font-weight:bold}</style></head>"
                    + "<body><p>one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen</p></body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            wide.setPage(server.url("/page"));
            narrow.setPage(server.url("/page"));
            awaitPage(wide, server.url("/page"));
            awaitPage(narrow, server.url("/page"));
            Thread.sleep(300);
            BufferedImage a = snapshot(wide);
            BufferedImage b = snapshot(narrow);
            int redWide = redRows(a);
            int redNarrow = redRows(b);
            Check.isTrue(redWide >= 1, "the first line has red ink in the wide component");
            Check.isTrue(redNarrow >= 1, "the first line has red ink in the narrow component");
            // 빨간 글자는 첫 줄의 높이 안에만 있다(둘째 줄 이후에는 없다): 빨간 행의 수가 한 줄의 높이를 넘지 않는다.
            Check.isTrue(redWide <= 24, "red ink only on one line (wide): " + redWide);
            Check.isTrue(redNarrow <= 24, "red ink only on one line (narrow): " + redNarrow);
            Check.isTrue(redPixels(a) > redPixels(b), "the wide component has more red text: " + redPixels(a) + " > " + redPixels(b));
        } finally {
            wide.dispose();
            narrow.dispose();
            server.stop();
        }
    }

    /**
     * Counts the rows of the image that contain reddish pixels.
     * <p>
     * 이미지에서 붉은 픽셀이 있는 행의 수를 센다.
     *
     * @param img the image / 이미지
     * @return the number of rows / 행 수
     */
    private static int redRows(BufferedImage img) {
        int rows = 0;
        // 행마다 붉은 픽셀이 있는지 확인한다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (isRed(img.getRGB(x, y))) {
                    rows++;
                    break;
                }
            }
        }
        return rows;
    }

    /**
     * Counts the reddish pixels of the image.
     * <p>
     * 이미지의 붉은 픽셀 수를 센다.
     *
     * @param img the image / 이미지
     * @return the number of pixels / 픽셀 수
     */
    private static int redPixels(BufferedImage img) {
        int n = 0;
        // 모든 픽셀 중 붉은 것을 센다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (isRed(img.getRGB(x, y))) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Tells whether a pixel is reddish (red high, green and blue low).
     * <p>
     * 픽셀이 붉은지(빨강은 크고 초록과 파랑은 작은지) 알려 준다.
     *
     * @param c the pixel / 픽셀
     * @return true if reddish / 붉으면 true
     */
    private static boolean isRed(int c) {
        return ((c >> 16) & 0xFF) > 0xC0 && ((c >> 8) & 0xFF) < 0xA0 && (c & 0xFF) < 0xA0;
    }
}
