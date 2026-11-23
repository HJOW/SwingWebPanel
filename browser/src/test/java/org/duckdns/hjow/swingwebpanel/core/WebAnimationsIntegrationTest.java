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
 * Web Animations through the real component and real HTTP (plan item P10-08-h): an animation that a script starts with
 * <code>element.animate()</code> changes the painted page by itself, its <code>finished</code> promise and <code>finish</code> event reach the
 * script, <code>fill: 'forwards'</code> keeps the last value on the screen, <code>cancel()</code> from a timer gives the cascade value back, and
 * neither a navigation nor turning JavaScript off leaves an animation running on the old page.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 Web Animations(계획 P10-08-h): 스크립트가 <code>element.animate()</code> 로 시작한 애니메이션은 스스로 그려진 페이지를 바꾸고,
 * <code>finished</code> 프라미스와 <code>finish</code> 이벤트가 스크립트에 도달하며, <code>fill: 'forwards'</code> 는 마지막 값을 화면에 유지하고, 타이머의
 * <code>cancel()</code> 은 캐스케이드 값을 돌려주며, 이동이나 JavaScript 끄기 뒤에 이전 페이지의 애니메이션이 계속 실행되지 않는다.
 */
public class WebAnimationsIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebAnimationsIntegrationTest() {
    }

    /** A red 40 by 40 square at the top left. 왼쪽 위의 빨간 40x40 사각형. */
    private static final String HEAD = "<!doctype html><html><head><style>body{margin:0}#s{width:40px;height:40px;background:#f00}</style></head><body>"
            + "<div id=s></div><script>var s = document.getElementById('s');";

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
     * Waits until the painted pixel has a color.
     * <p>
     * 그려진 픽셀이 어떤 색이 될 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param rgb   the color / 색
     * @param ms    the longest wait / 최대 대기
     * @return the last color / 마지막 색
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int awaitColor(SwingWebPanel panel, int rgb, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        int last = pixel(panel);
        // 색이 맞을 때까지 다시 그리며 확인한다.
        while (last != rgb && System.currentTimeMillis() < end) {
            Thread.sleep(10);
            last = pixel(panel);
        }
        return last;
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
     * Waits until the session storage has a value for a key.
     * <p>
     * sessionStorage 에 키의 값이 생길 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static String awaitStored(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        // 값이 생길 때까지 짧게 쉬며 확인한다.
        while (stored(panel, key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return stored(panel, key);
    }

    /**
     * A fade with <code>fill: 'forwards'</code> runs by itself: the square is painted red, then pink, then it stays white (opacity 0 on white) after
     * the end; the finished promise sees the final computed opacity and the <code>finish</code> event its current time.
     * <p>
     * <code>fill: 'forwards'</code> 인 사라지기가 스스로 실행된다: 사각형은 빨강, 분홍을 거쳐 끝난 뒤 흰색(흰 바탕 위의 불투명도 0)으로 남는다. finished 프라미스는 마지막
     * 계산 불투명도를, <code>finish</code> 이벤트는 현재 시간을 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptAnimationPaintsAndFinishes() throws Exception {
        String page = HEAD + "var a = s.animate([{opacity: 1}, {opacity: 0}], {duration: 1500, fill: 'forwards'});"
                + "a.finished.then(function () { sessionStorage.setItem('finished', a.playState + ':' + getComputedStyle(s).opacity); });"
                + "a.onfinish = function (e) { sessionStorage.setItem('event', e.type + ':' + e.currentTime + ':' + (e instanceof AnimationPlaybackEvent)); };"
                + "</script></body></html>";
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(page));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            long end = System.currentTimeMillis() + 30000;
            int last = pixel(panel);
            boolean red = false;
            boolean pink = false;
            // 사각형이 흰색이 될 때까지 다시 그리며 빨강과 중간 색(분홍)을 거쳤는지 본다.
            while ((last != 0xFFFFFF || !red) && System.currentTimeMillis() < end) {
                int g = (last >> 8) & 0xFF;
                if (last == 0xFF0000 || ((last >> 16) == 0xFF && g < 0x30)) {
                    red = true;
                }
                if ((last >> 16) == 0xFF && g > 0x30 && g < 0xD0) {
                    pink = true;
                }
                Thread.sleep(15);
                last = pixel(panel);
            }
            Check.isTrue(red, "the square was red at first");
            Check.isTrue(pink, "a color between red and white was painted on the way");
            Check.equal(0xFFFFFF, last, "the square faded out by itself");
            Check.equal("finished:0", awaitStored(panel, "finished"), "the finished promise saw the final value");
            Check.equal("finish:1500:true", awaitStored(panel, "event"), "the finish event");
            Thread.sleep(300);
            Check.equal(0xFFFFFF, pixel(panel), "fill forwards keeps the last value on the screen");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A long blue animation paints the square blue; a timer cancels it and the square is red again; an animation that a hashchange handler finishes with
     * <code>fill: 'forwards'</code> stays at its last keyframe (green).
     * <p>
     * 긴 파란 애니메이션은 사각형을 파랗게 그린다. 타이머가 취소하면 사각형이 다시 빨갛다. hashchange 핸들러가 <code>fill: 'forwards'</code> 인 애니메이션을 끝내면 마지막 키프레임(초록)에 머문다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelAndFinishChangeThePicture() throws Exception {
        String page = HEAD + "var a = s.animate({backgroundColor: ['#0000ff', '#0000ff']}, 100000);"
                + "setTimeout(function () { a.cancel(); sessionStorage.setItem('canceled', a.playState); }, 2000);"
                + "window.onhashchange = function () { var b = s.animate({backgroundColor: ['#0000ff', '#00ff00']}, {duration: 100000, fill: 'forwards'});"
                + " b.finish(); sessionStorage.setItem('finished', b.playState); };"
                + "</script></body></html>";
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(page));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            Check.equal(0x0000FF, awaitColor(panel, 0x0000FF, 20000), "the animation paints the square blue");
            Check.equal("idle", awaitStored(panel, "canceled"), "the timer canceled it");
            Check.equal(0xFF0000, awaitColor(panel, 0xFF0000, 20000), "red again after cancel");
            // 같은 문서 안의 프래그먼트 이동으로 hashchange 를 일으켜 두 번째 애니메이션을 시작하고 끝내게 한다.
            panel.setPage(server.url("/page") + "#go");
            Check.equal("finished", awaitStored(panel, "finished"), "the hashchange handler finished the second one");
            Check.equal(0x00FF00, awaitColor(panel, 0x00FF00, 20000), "fill forwards shows the last keyframe");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A navigation ends the endless animation of the old page without events: the new page is painted with its own colors and the old page's
     * <code>cancel</code> and <code>finish</code> handlers never run; turning JavaScript off ends a script animation and the square shows its
     * cascade color again.
     * <p>
     * 이동은 이전 페이지의 끝없는 애니메이션을 이벤트 없이 끝낸다: 새 페이지는 자기 색으로 그려지고 이전 페이지의 <code>cancel</code>, <code>finish</code> 핸들러는
     * 실행되지 않는다. JavaScript 를 끄면 스크립트 애니메이션이 끝나고 사각형이 다시 캐스케이드 색을 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationAndDisabledScriptsEndAnimations() throws Exception {
        String first = HEAD + "var a = s.animate({backgroundColor: ['#0000ff', '#0000ff']}, {duration: 1000, iterations: Infinity});"
                + "a.oncancel = function () { sessionStorage.setItem('old', 'cancel'); };"
                + "a.onfinish = function () { sessionStorage.setItem('old', 'finish'); };"
                + "</script></body></html>";
        String second = "<!doctype html><html><head><style>body{margin:0}#s{width:40px;height:40px;background:#0f0}</style></head><body><div id=s></div></body></html>";
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/first", TestHttpServer.Response.html(first));
            server.routeFixed("/second", TestHttpServer.Response.html(second));
            server.start();
            panel.setPage(server.url("/first"));
            awaitPage(panel, server.url("/first"));
            Check.equal(0x0000FF, awaitColor(panel, 0x0000FF, 20000), "the old page animates");
            panel.setPage(server.url("/second"));
            awaitPage(panel, server.url("/second"));
            Check.equal(0x00FF00, awaitColor(panel, 0x00FF00, 20000), "the new page has its own color");
            Thread.sleep(1500);
            Check.equal(0x00FF00, pixel(panel), "nothing of the old animation reaches the new page");
            Check.isNull(stored(panel, "old"), "no event of the old page ran");
            panel.setPage(server.url("/first"));
            awaitPage(panel, server.url("/first"));
            Check.equal(0x0000FF, awaitColor(panel, 0x0000FF, 20000), "animating again");
            panel.setJavascriptEnabled(false);
            Check.equal(0xFF0000, awaitColor(panel, 0xFF0000, 20000), "turning JavaScript off ends the script animation");
            Thread.sleep(500);
            Check.equal(0xFF0000, pixel(panel), "and it stays ended");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
