package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.layout.WebFontSet;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The block period of <code>font-display</code> through the real component and real HTTP (plan item P10-08-g, CSS Fonts 4 section 4.8): the server holds the
 * web font back. With <code>font-display: block</code> the text that waits for it is not painted at first, and when the block period (3 s from the request) is
 * over the component paints it in the fallback font by itself (no other event happens on the page); with <code>swap</code> it is painted at once. A red bar
 * shows that the page was painted. Disposing the component while a block period is running is safe.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 <code>font-display</code> 의 block 기간(계획 P10-08-g, CSS Fonts 4 4.8 절): 서버가 웹 글꼴을 붙잡아 둔다. <code>font-display: block</code> 이면
 * 그것을 기다리는 텍스트는 처음에 그려지지 않고, block 기간(요청부터 3초)이 지나면 컴포넌트가 스스로(페이지에 다른 일이 없어도) 대체 글꼴로 그린다. <code>swap</code> 이면 곧바로
 * 그린다. 빨간 막대는 페이지가 그려졌음을 보인다. block 기간 중에 컴포넌트를 폐기해도 안전하다.
 */
public class FontBlockIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontBlockIntegrationTest() {
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
                result[0].setSize(500, 160);
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
     * Counts the pixels of an image region that match a test.
     * <p>
     * 이미지 영역에서 조건에 맞는 픽셀의 수를 센다.
     *
     * @param img  the image / 이미지
     * @param y0   the top / 위쪽
     * @param y1   the bottom (exclusive) / 아래쪽(미포함)
     * @param red  true to count red pixels, false to count black (text) pixels / 빨간 픽셀을 세려면 true, 검은(텍스트) 픽셀을 세려면 false
     * @return the number / 수
     */
    private static int count(BufferedImage img, int y0, int y1, boolean red) {
        int n = 0;
        // 영역의 픽셀을 모두 훑는다.
        for (int y = y0; y < Math.min(y1, img.getHeight()); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (red ? (r > 200 && g < 60 && b < 60) : (r < 100 && g < 100 && b < 100)) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the page: a red bar 20 pixels high, then a line of black text in <code>Web, monospace</code> (from y 20 to 80).
     * <p>
     * 페이지를 반환한다: 높이 20 픽셀의 빨간 막대, 그 아래 <code>Web, monospace</code> 의 검은 텍스트 한 줄(y 20~80).
     *
     * @param display the value of <code>font-display</code> / <code>font-display</code> 값
     * @return the HTML / HTML
     */
    private static String page(String display) {
        return "<!doctype html><html><head><style>@font-face{font-family:Web;font-display:" + display + ";src:url(font.ttf) format('truetype')}"
                + "body{margin:0} #bar{height:20px;background:#f00} #t{margin:0;height:60px;font:40px Web,monospace;color:#000;white-space:nowrap}</style></head>"
                + "<body><div id=bar></div><p id=t>HHHHHHHH</p></body></html>";
    }

    /**
     * Starts a server whose font answer waits for a gate and records when the font was requested.
     * <p>
     * 글꼴 응답이 문을 기다리고 글꼴을 요청한 시각을 기록하는 서버를 시작한다.
     *
     * @param display   the value of <code>font-display</code> / <code>font-display</code> 값
     * @param gate      the gate / 문
     * @param requested receives the time of the font request / 글꼴 요청 시각을 받는다
     * @return the server / 서버
     * @throws Exception if it cannot start / 시작할 수 없으면
     */
    private static TestHttpServer server(String display, final CountDownLatch gate, final long[] requested) throws Exception {
        TestHttpServer server = new TestHttpServer();
        server.route("/font.ttf", new TestHttpServer.Handler() {
            /**
             * Records the time and answers with a broken font when the test opens the gate.
             * <p>
             * 시각을 기록하고 시험이 문을 열면 깨진 글꼴로 응답한다.
             *
             * @param request the request / 요청
             * @return the answer / 응답
             * @throws Exception if interrupted / 인터럽트되면
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                synchronized (requested) {
                    requested[0] = System.currentTimeMillis();
                }
                gate.await(30, TimeUnit.SECONDS);
                return new TestHttpServer.Response(404);
            }
        });
        server.routeFixed("/page", TestHttpServer.Response.html(page(display)));
        server.start();
        return server;
    }

    /**
     * Waits until the page was painted (the red bar is there) and returns that picture.
     * <p>
     * 페이지가 그려질 때까지(빨간 막대가 있을 때까지) 기다리고 그 그림을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the picture / 그림
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static BufferedImage awaitPainted(SwingWebPanel panel) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        BufferedImage img = snapshot(panel);
        // 빨간 막대가 그려질 때까지 짧게 쉬며 다시 찍는다.
        while (count(img, 0, 20, true) < 1000 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
            img = snapshot(panel);
        }
        Check.isTrue(count(img, 0, 20, true) >= 1000, "the page is painted");
        return img;
    }

    /**
     * <code>font-display: block</code>: the text is invisible while the font is held back within its block period and becomes visible in the fallback font after
     * the period, without any other event on the page.
     * <p>
     * <code>font-display: block</code>: 글꼴이 block 기간 안에서 붙잡혀 있는 동안 텍스트는 보이지 않고, 기간이 지나면 페이지에 다른 일이 없어도 대체 글꼴로 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedTextAppearsWhenTheBlockPeriodEnds() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        long[] requested = new long[1];
        TestHttpServer server = server("block", gate, requested);
        SwingWebPanel panel = newPanel();
        try {
            panel.setPage(server.url("/page"));
            BufferedImage first = awaitPainted(panel);
            long at = System.currentTimeMillis();
            long asked;
            synchronized (requested) {
                asked = requested[0];
            }
            Check.isTrue(asked > 0, "the font was requested before the page was painted");
            // 그림을 찍은 때가 block 기간 안이었을 때만 보이지 않음을 판정할 수 있다(아주 바쁜 컴퓨터에서는 기간이 이미 지났을 수 있다).
            if (at - asked < WebFontSet.LONG_BLOCK_MS - 1000) {
                Check.equal(0, count(first, 20, 80, false), "within the block period no pixel of the text is painted");
                long mid = asked + WebFontSet.LONG_BLOCK_MS - 1000;
                // 기간의 중간쯤(요청 2초 뒤)에도 여전히 보이지 않는지 한 번 더 본다.
                while (System.currentTimeMillis() < mid) {
                    Thread.sleep(20);
                }
                BufferedImage middle = snapshot(panel);
                if (System.currentTimeMillis() - asked < WebFontSet.LONG_BLOCK_MS - 400) {
                    Check.equal(0, count(middle, 20, 80, false), "two seconds after the request the text is still invisible");
                }
            } else {
                System.err.println("# the first picture came " + (at - asked) + " ms after the font request; the invisible state was not checked");
            }
            long end = asked + WebFontSet.LONG_BLOCK_MS + 15000;
            BufferedImage later = snapshot(panel);
            // block 기간이 끝나면 컴포넌트가 스스로 다시 그린다: 텍스트 픽셀이 나타날 때까지 기다린다.
            while (count(later, 20, 80, false) < 200 && System.currentTimeMillis() < end) {
                Thread.sleep(50);
                later = snapshot(panel);
            }
            Check.isTrue(count(later, 20, 80, false) >= 200, "after the block period the text is painted in the fallback font");
            Check.equal(1, server.count("/font.ttf"), "one font request");
        } finally {
            gate.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>font-display: swap</code> has no block period: the text is painted in the fallback font while the font is held back.
     * <p>
     * <code>font-display: swap</code> 에는 block 기간이 없다: 글꼴이 붙잡혀 있는 동안에도 텍스트를 대체 글꼴로 그린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSwapTextIsPaintedAtOnce() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        long[] requested = new long[1];
        TestHttpServer server = server("swap", gate, requested);
        SwingWebPanel panel = newPanel();
        try {
            panel.setPage(server.url("/page"));
            BufferedImage img = awaitPainted(panel);
            long end = System.currentTimeMillis() + 2000;
            // 텍스트는 곧바로 보여야 한다(배치가 막대보다 늦게 게시되는 경우에 대비해 잠시 다시 찍는다).
            while (count(img, 20, 80, false) < 200 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
                img = snapshot(panel);
            }
            Check.isTrue(count(img, 20, 80, false) >= 200, "swap: the text is painted while the font is held back");
        } finally {
            gate.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Disposing the component while a block period is running cancels the waiting rendering: no exception, and the component stays empty.
     * <p>
     * block 기간이 진행되는 동안 컴포넌트를 폐기하면 기다리던 렌더링이 취소된다: 예외가 없고 컴포넌트는 빈 상태로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeDuringTheBlockPeriod() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        long[] requested = new long[1];
        TestHttpServer server = server("block", gate, requested);
        SwingWebPanel panel = newPanel();
        try {
            panel.setPage(server.url("/page"));
            awaitPainted(panel);
            panel.dispose();
            Thread.sleep(WebFontSet.LONG_BLOCK_MS + 500);
            Check.equal("about:blank", panel.getPage(), "the disposed component stays empty");
            BufferedImage img = snapshot(panel);
            Check.equal(0, count(img, 0, img.getHeight(), true), "nothing of the old page is painted after the block period");
        } finally {
            gate.countDown();
            panel.dispose();
            server.stop();
        }
    }
}
