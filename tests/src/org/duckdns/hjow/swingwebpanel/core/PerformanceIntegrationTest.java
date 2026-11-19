package org.duckdns.hjow.swingwebpanel.core;

import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests <code>window.performance</code> in a real {@link SwingWebPanel} against a real local HTTP server (Navigation Timing Level 2, fetched 2026-11-17; see
 * <code>docs/progress/webapi2.md</code>): the kind of navigation that made the page (<code>navigate</code>, <code>reload</code>, <code>back_forward</code>) in the
 * navigation entry and in <code>performance.navigation</code>, and the times of the loading stages on the real clock (in order, and 0 for the network times that
 * the page cannot know). Each page appends what it saw to <code>sessionStorage</code> when its <code>load</code> event has ended.
 * <p>
 * 실제 {@link SwingWebPanel} 에서 실제 로컬 HTTP 서버를 상대로 <code>window.performance</code> 를 시험한다(Navigation Timing Level 2, 2026-11-17 조회,
 * <code>docs/progress/webapi2.md</code> 참조): 탐색 항목과 <code>performance.navigation</code> 의 페이지를 만든 이동의 종류(<code>navigate</code>, <code>reload</code>,
 * <code>back_forward</code>), 실제 시계로 잰 로딩 단계의 시각(순서대로이고 페이지가 알 수 없는 네트워크 시각은 0). 각 페이지는 <code>load</code> 이벤트가 끝나면
 * 본 것을 <code>sessionStorage</code> 에 덧붙인다.
 */
public class PerformanceIntegrationTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PerformanceIntegrationTest() {
    }

    /**
     * Reads what the pages reported.
     * <p>
     * 페이지가 보고한 것을 읽는다.
     *
     * @param panel the panel / 패널
     * @return the reports joined with slashes / 사선으로 이은 보고
     */
    private static String reports(SwingWebPanel panel) {
        // 복사한 저장소 항목에서 보고 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if ("T".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    /**
     * Waits until the reports have a number of entries.
     * <p>
     * 보고가 주어진 개수가 될 때까지 기다린다.
     *
     * @param panel the panel / 패널
     * @param count the number of reports / 보고의 수
     * @return the reports / 보고
     * @throws Exception on interruption / 중단되면
     */
    private static String await(SwingWebPanel panel, int count) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        String text;
        // 마감까지 보고가 쌓이기를 기다린다.
        do {
            text = reports(panel);
            if (text.length() > 0 && text.split("/").length >= count) {
                return text;
            }
            Thread.sleep(30);
        } while (System.currentTimeMillis() < deadline);
        Check.fail("the reports did not reach " + count + ": " + text);
        return text;
    }

    /**
     * The navigation entry and <code>performance.navigation</code> follow the kind of navigation that made the page, and the stages have real times in order.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNavigationTypesAndRealStages() throws Exception {
        TestHttpServer server = new TestHttpServer();
        String page = "<!doctype html><title>t</title><p>x</p><script>window.addEventListener('load', function () { setTimeout(function () { "
                + "var n = performance.getEntriesByType('navigation')[0], t = performance.timing; "
                + "var ordered = t.navigationStart > 0 && t.domLoading >= t.navigationStart && t.domInteractive >= t.domLoading && "
                + "t.domContentLoadedEventStart >= t.domInteractive && t.domContentLoadedEventEnd >= t.domContentLoadedEventStart && "
                + "t.domComplete >= t.domContentLoadedEventEnd && t.loadEventStart >= t.domComplete && t.loadEventEnd >= t.loadEventStart; "
                + "var nets = [t.fetchStart, t.requestStart, t.responseEnd, n.fetchStart, n.responseEnd, n.redirectCount].join(','); "
                + "var mark = Math.abs(performance.now() - (Date.now() - performance.timeOrigin)) < 50; "
                + "sessionStorage.setItem('T', (sessionStorage.getItem('T') || '') + n.type + ':' + performance.navigation.type + ':' + ordered + ':' + nets + ':' + mark + '/'); }, 0); });"
                + "</" + "script>";
        server.routeFixed("/p", TestHttpServer.Response.html(page).header("Cache-Control", "no-store"));
        server.routeFixed("/q", TestHttpServer.Response.html(page).header("Cache-Control", "no-store"));
        server.start();
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the panel.
             * <p>
             * 패널을 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(300, 180);
            }
        });
        SwingWebPanel panel = holder[0];
        try {
            panel.setPage(server.url("/p"));
            await(panel, 1);
            panel.setPage(server.url("/q"));
            await(panel, 2);
            panel.back();
            await(panel, 3);
            panel.refresh();
            String all = await(panel, 4);
            String[] parts = all.split("/");
            String tail = ":true:0,0,0,0,0,0:true";
            Check.equal("navigate:0" + tail, parts[0], "the first navigation");
            Check.equal("navigate:0" + tail, parts[1], "the second navigation");
            Check.equal("back_forward:2" + tail, parts[2], "going back");
            Check.equal("reload:1" + tail, parts[3], "refreshing");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
