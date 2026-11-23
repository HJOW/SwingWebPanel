package org.duckdns.hjow.swingwebpanel.core;

import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Pages whose scripts fail or never stop, through the real component (plan item P15-12): a thrown error reaches
 * <code>window.onerror</code> and the page goes on, a script that loops forever is aborted by the execution limit while the user
 * interface stays responsive, and the next script and the received source are not affected.
 * <p>
 * 스크립트가 실패하거나 끝나지 않는 페이지를 실제 컴포넌트로 확인한다(계획 P15-12): 던진 오류는 <code>window.onerror</code> 에 전달되고 페이지는
 * 계속되며, 영원히 도는 스크립트는 실행 한도로 중단되고 그동안 사용자 인터페이스가 반응하며, 다음 스크립트와 수신 원문은 영향을 받지 않는다.
 */
public class ScriptRobustnessTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptRobustnessTest() {
    }

    /**
     * Reads a value that a script stored in the sessionStorage.
     * <p>
     * 스크립트가 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String value(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the confirmed page has the address.
     * <p>
     * 확정된 페이지가 주소가 될 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param url     the address / 주소
     * @param seconds the longest wait / 최대 대기 시간(초)
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
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
     * P15-12: a script that throws reaches <code>window.onerror</code>, the scripts after it run, and the page loads with its source.
     * <p>
     * P15-12: 예외를 던진 스크립트는 <code>window.onerror</code> 에 전달되고, 뒤의 스크립트는 실행되며, 페이지는 원문과 함께 로드된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThrownErrorsReachOnerrorAndThePageGoesOn() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String html = "<!doctype html><script>window.onerror = function (m, src, line) { sessionStorage.setItem('error', String(m)); return true; };</script>"
                    + "<script>var broken = null; broken.property = 1;</script>"
                    + "<script>sessionStorage.setItem('next', 'ran');</script><p>text</p>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"), 60);
            Check.equal("ran", value(panel, "next"), "the script after the failing one ran");
            Check.notNull(value(panel, "error"), "onerror was told about the failure");
            Check.equal(html, panel.getContent(), "the received source is kept");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-12: a script that never ends is aborted by the execution limit, the user interface keeps answering while it runs, and the page
     * is shown with the scripts that follow.
     * <p>
     * P15-12: 끝나지 않는 스크립트는 실행 한도로 중단되고, 실행되는 동안 사용자 인터페이스가 계속 응답하며, 뒤따르는 스크립트와 함께 페이지가 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRunawayScriptIsAbortedWhileTheInterfaceStaysResponsive() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String html = "<!doctype html><script>sessionStorage.setItem('before', 'ran'); for (;;) { }</script>"
                    + "<script>sessionStorage.setItem('after', 'ran');</script><p>text</p>";
            server.routeFixed("/loop", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/loop"));
            long worst = 0;
            long end = System.currentTimeMillis() + 90000;
            // 스크립트가 도는 동안 EDT 가 얼마나 빨리 응답하는지 재면서 페이지가 확정되기를 기다린다.
            while (!server.url("/loop").equals(panel.getPage()) && System.currentTimeMillis() < end) {
                long t0 = System.currentTimeMillis();
                SwingUtilities.invokeAndWait(new Runnable() {
                    /**
                     * Does nothing: the call only measures how long the EDT needs to answer.
                     * <p>
                     * 아무것도 하지 않는다. EDT 가 응답하는 데 걸리는 시간만 잰다.
                     */
                    public void run() {
                    }
                });
                worst = Math.max(worst, System.currentTimeMillis() - t0);
                Thread.sleep(50);
            }
            Check.equal(server.url("/loop"), panel.getPage(), "the page was shown after the runaway script was aborted");
            Check.isTrue(worst < 2000, "the EDT stayed responsive while the script ran: worst answer " + worst + " ms");
            Check.equal("ran", value(panel, "before"), "the script ran until it was stopped");
            Check.equal("ran", value(panel, "after"), "the script after the aborted one ran");
            Check.equal(html, panel.getContent(), "the received source is kept");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-10: turning JavaScript off stops the timers of the page that is shown and keeps its HTML and links; turning it on again does
     * not restart what was stopped, and the next load runs scripts normally.
     * <p>
     * P15-10: JavaScript 를 끄면 보이는 페이지의 타이머가 멈추고 HTML 과 링크는 유지된다. 다시 켜도 멈춘 것을 재시작하지 않으며 다음 로드부터
     * 스크립트가 정상 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptingSwitchStopsTimersAndAppliesFromTheNextLoad() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            String html = "<!doctype html><script>var n = 0; setInterval(function () { n++; sessionStorage.setItem('ticks', String(n)); }, 20);"
                    + "sessionStorage.setItem('loads', String(Number(sessionStorage.getItem('loads') || 0) + 1));</script><p>kept</p>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"), 60);
            long end = System.currentTimeMillis() + 30000;
            // 타이머가 몇 번 돌 때까지 기다린다.
            while ((value(panel, "ticks") == null || Integer.parseInt(value(panel, "ticks")) < 3) && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            panel.setJavascriptEnabled(false);
            Thread.sleep(150);
            String stopped = value(panel, "ticks");
            Thread.sleep(300);
            Check.equal(stopped, value(panel, "ticks"), "the timer stopped when scripts were turned off");
            Check.equal(html, panel.getContent(), "the page and its source are kept");
            panel.setJavascriptEnabled(true);
            Thread.sleep(300);
            Check.equal(stopped, value(panel, "ticks"), "turning scripts on again does not restart the stopped page");
            panel.refresh();
            end = System.currentTimeMillis() + 30000;
            // 새로고침한 페이지가 스크립트를 다시 실행해 타이머가 다시 돌 때까지 기다린다.
            while ((value(panel, "loads") == null || !"2".equals(value(panel, "loads"))) && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal("2", value(panel, "loads"), "the next load runs the scripts again");
            panel.setJavascriptEnabled(false);
            panel.refresh();
            Thread.sleep(800);
            Check.equal("2", value(panel, "loads"), "a load with scripts off does not run them");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
