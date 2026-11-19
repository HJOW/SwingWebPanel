package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * CSS animations through the real component and real HTTP (plan item P10-07): an animation runs without any input (the page schedules
 * its frames and renders again), a transition that a script starts moves the painted result, the <code>animationend</code> event reaches
 * a script listener, the animations go on when JavaScript is disabled (they are CSS, not script), and the frame timer thread ends when the
 * component is disposed.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 CSS 애니메이션(계획 P10-07): 애니메이션은 입력 없이 실행되고(페이지가 프레임을 예약해 다시 렌더링한다), 스크립트가 시작한 전환은
 * 그려진 결과를 움직이며, <code>animationend</code> 이벤트는 스크립트 리스너에 도달하고, JavaScript 를 꺼도 애니메이션은 계속되며(CSS 이지 스크립트가 아니다),
 * 컴포넌트를 폐기하면 프레임 타이머 스레드가 끝난다.
 */
public class AnimationIntegrationTest {

    /** The page: a red 40 by 40 square that fades out in 2 s, a script listener for the end of the animation. 600 ms 에 사라지는 빨간 40x40 사각형과 애니메이션 끝의 스크립트 리스너가 있는 페이지. */
    private static final String PAGE = "<!doctype html><html><head><style>body{margin:0}"
            + "@keyframes fade{from{opacity:1} to{opacity:0}}"
            + "#a{width:40px;height:40px;background:#f00;animation:fade 2s linear forwards}</style></head><body><div id=a></div>"
            + "<script>document.getElementById('a').addEventListener('animationend',function(e){sessionStorage.setItem('ended',e.type + ':' + e.animationName + ':' + (e instanceof AnimationEvent) + ':' + (e instanceof Event) + ':' + e.elapsedTime + ':' + e.bubbles);});</script></body></html>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AnimationIntegrationTest() {
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
     * Paints the component and returns the color of a pixel inside the square.
     * <p>
     * 컴포넌트를 그리고 사각형 안 한 픽셀의 색을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return 0xRRGGBB / 0xRRGGBB
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int pixel(final SwingWebPanel panel) throws Exception {
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
        return img.getRGB(20, 20) & 0xFFFFFF;
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
     * Counts the threads of the animation frame timers that are alive.
     * <p>
     * 살아 있는 애니메이션 프레임 타이머 스레드의 수를 센다.
     *
     * @return the count / 수
     */
    private static int frameThreads() {
        int n = 0;
        // 모든 스레드에서 프레임 타이머 스레드를 센다.
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            if ("swingwebpanel-animation".equals(e.getKey().getName()) && e.getKey().isAlive()) {
                n++;
            }
        }
        return n;
    }

    /**
     * A fade animation runs by itself: the square is red at first, the colors in between are pink and it ends white (opacity 0 on white),
     * a script listener gets <code>animationend</code>, and after the disposal no frame timer thread is left.
     * <p>
     * 사라지는 애니메이션이 스스로 실행된다: 사각형은 처음에 빨갛고 중간은 분홍이며 흰색(흰 바탕 위의 불투명도 0)으로 끝나고, 스크립트 리스너가
     * <code>animationend</code> 를 받으며, 폐기한 뒤에는 프레임 타이머 스레드가 남지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationRunsByItselfAndEndsWithAnEvent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(PAGE));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            long end = System.currentTimeMillis() + 30000;
            boolean pink = false;
            int last = pixel(panel);
            // 페이지가 처음 그려져 사각형이 보일 때까지(흰색이 아닐 때까지) 기다린다.
            while (last == 0xFFFFFF && System.currentTimeMillis() < end) {
                Thread.sleep(5);
                last = pixel(panel);
            }
            Check.isTrue(last != 0xFFFFFF, "the square was painted at first");
            // 사각형이 흰색이 될 때까지 다시 그리며 중간 색(분홍)을 거쳤는지 본다.
            while (last != 0xFFFFFF && System.currentTimeMillis() < end) {
                int g = (last >> 8) & 0xFF;
                if ((last >> 16) == 0xFF && g > 0x20 && g < 0xE0) {
                    pink = true;
                }
                Thread.sleep(15);
                last = pixel(panel);
            }
            Check.equal(0xFFFFFF, last, "the square faded out by itself (no input)");
            Check.isTrue(pink, "a color between red and white was painted on the way");
            long wait = System.currentTimeMillis() + 10000;
            // 스크립트 리스너가 animationend 를 받을 때까지 짧게 쉬며 확인한다.
            while (stored(panel, "ended") == null && System.currentTimeMillis() < wait) {
                Thread.sleep(10);
            }
            Check.equal("animationend:fade:true:true:2:true", stored(panel, "ended"), "the script listener got the end event as an AnimationEvent with its properties");
            Check.equal(PAGE, panel.getContent(), "the received source is unchanged");
        } finally {
            panel.dispose();
            server.stop();
        }
        long gone = System.currentTimeMillis() + 10000;
        // 폐기한 뒤 프레임 타이머 스레드가 끝날 때까지 짧게 쉬며 확인한다.
        while (frameThreads() > 0 && System.currentTimeMillis() < gone) {
            Thread.sleep(20);
        }
        Check.equal(0, frameThreads(), "no frame timer thread is left after the disposal");
    }

    /**
     * The same animation with JavaScript disabled: CSS animations are not scripts, so the square still fades out; the script of the page
     * does not run.
     * <p>
     * JavaScript 를 끈 같은 애니메이션: CSS 애니메이션은 스크립트가 아니므로 사각형은 여전히 사라지고 페이지의 스크립트는 실행되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationContinuesWithJavascriptDisabled() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            panel.setJavascriptEnabled(false);
            server.routeFixed("/page", TestHttpServer.Response.html(PAGE));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            long end = System.currentTimeMillis() + 30000;
            int last = pixel(panel);
            // 페이지가 처음 그려져 사각형이 보일 때까지(흰색이 아닐 때까지) 기다린다.
            while (last == 0xFFFFFF && System.currentTimeMillis() < end) {
                Thread.sleep(5);
                last = pixel(panel);
            }
            Check.isTrue(last != 0xFFFFFF, "the square was painted at first");
            // 사각형이 흰색이 될 때까지 다시 그린다.
            while (last != 0xFFFFFF && System.currentTimeMillis() < end) {
                Thread.sleep(15);
                last = pixel(panel);
            }
            Check.equal(0xFFFFFF, last, "the CSS animation runs with JavaScript disabled");
            Check.isNull(stored(panel, "ended"), "the script did not run");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
