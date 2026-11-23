package org.duckdns.hjow.swingwebpanel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Navigations that are still loading when something newer happens, through the real component and real HTTP (plan item P19-09): a response
 * that arrives late, or breaks off, after a newer page was confirmed never brings the old page back, never changes the history, the source,
 * the status or the progress, and a <code>clean()</code> during a load leaves the blank page.
 * <p>
 * 더 새로운 일이 생길 때 아직 로딩 중인 이동을 실제 컴포넌트와 실제 HTTP 로 확인하는 시험(계획 P19-09): 더 새로운 페이지가 확정된 뒤 늦게 도착하거나
 * 중간에 끊긴 응답은 이전 페이지를 되살리지 않고 이력, 원문, 상태, 진행률을 바꾸지 않으며, 로딩 중의 <code>clean()</code> 은 빈 페이지를 남긴다.
 */
public class PendingNavigationIntegrationTest {

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Servers created by the case.
     * <p>
     * 사례가 만든 서버.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Gates that the case holds closed; they are opened in tearDown so no server thread waits for ever.
     * <p>
     * 사례가 닫아 둔 문. 서버 스레드가 영원히 기다리지 않도록 tearDown 에서 연다.
     */
    private final List<CountDownLatch> gates = new ArrayList<CountDownLatch>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PendingNavigationIntegrationTest() {
    }

    /**
     * Opens every gate and releases everything the case created.
     * <p>
     * 모든 문을 열고 사례가 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 닫아 둔 문을 모두 열어 서버 스레드를 풀어 준다.
        for (CountDownLatch g : gates) {
            g.countDown();
        }
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        // 시작한 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
    }

    /**
     * Starts a server that answers <code>/fast</code> at once and <code>/slow</code> after the returned gate is opened. The arrival of the
     * slow request is counted down on <code>arrived</code>.
     * <p>
     * <code>/fast</code> 에는 곧바로, <code>/slow</code> 에는 돌려주는 문이 열린 뒤에 답하는 서버를 시작한다. 느린 요청의 도착은 <code>arrived</code> 에서
     * 센다.
     *
     * @param arrived    counted down when the slow request arrived / 느린 요청이 도착하면 센다
     * @param slowAnswer the answer of the slow path / 느린 경로의 응답
     * @return the gate of the slow path / 느린 경로의 문
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private CountDownLatch start(final CountDownLatch arrived, final TestHttpServer.Response slowAnswer) throws Exception {
        TestHttpServer server = new TestHttpServer();
        final CountDownLatch gate = new CountDownLatch(1);
        gates.add(gate);
        server.routeFixed("/fast", TestHttpServer.Response.html("<p>fast</p>"));
        server.routeFixed("/fast2", TestHttpServer.Response.html("<p>fast2</p>"));
        server.route("/slow", new TestHttpServer.Handler() {
            /**
             * Answers when the gate is opened.
             * <p>
             * 문이 열리면 응답한다.
             *
             * @param request the request / 요청
             * @return the answer / 응답
             * @throws Exception if interrupted / 인터럽트되면
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                arrived.countDown();
                gate.await(30, TimeUnit.SECONDS);
                return slowAnswer;
            }
        });
        server.start();
        servers.add(server);
        return gate;
    }

    /**
     * Waits until the component shows a page, finished loading with the given status.
     * <p>
     * 컴포넌트가 주어진 상태로 로딩을 마치고 페이지를 보여 줄 때까지 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param url    the address / 주소
     * @param status the status text / 상태 메시지
     */
    private static void await(SwingWebPanel p, String url, String status) {
        long end = System.currentTimeMillis() + 30000;
        // 기대한 페이지가 확정되고 진행이 100 이 될 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || !status.equals(p.getStatusText())) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.equal(url, p.getPage(), "page");
        Check.equal(status, p.getStatusText(), "status");
        Check.equal(100, p.getProgress(), "progress");
    }

    /**
     * Watches the component for a while: the page, source, status and progress must not change although the old request ends meanwhile.
     * <p>
     * 컴포넌트를 한동안 지켜본다: 그 사이 이전 요청이 끝나도 페이지, 원문, 상태, 진행률이 바뀌면 안 된다.
     *
     * @param p       the component / 컴포넌트
     * @param url     the address that must stay / 유지되어야 하는 주소
     * @param content the source that must stay / 유지되어야 하는 원문
     * @param history the history that must stay / 유지되어야 하는 이력
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void watchStays(SwingWebPanel p, String url, String content, String history) throws Exception {
        long end = System.currentTimeMillis() + 800;
        // 이전 요청이 끝날 시간을 주며 매번 상태가 그대로인지 확인한다.
        while (System.currentTimeMillis() < end) {
            Check.equal(url, p.getPage(), "the page does not change");
            Check.equal(content, p.getContent(), "the source does not change");
            Check.equal(100, p.getProgress(), "the progress stays 100");
            Check.equal(history, join(p), "the history does not change");
            Thread.sleep(20);
        }
    }

    /**
     * Joins the history of the component with commas.
     * <p>
     * 컴포넌트의 이력을 쉼표로 이어 붙인다.
     *
     * @param p the component / 컴포넌트
     * @return the URLs / URL
     */
    private static String join(SwingWebPanel p) {
        StringBuilder sb = new StringBuilder();
        // 이력의 URL 을 순서대로 이어 붙인다.
        for (String u : p.getHistories()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(u);
        }
        return sb.toString();
    }

    /**
     * A slow page that was replaced by a faster one does not come back when its answer arrives, nor does its late answer touch the history
     * or the progress.
     * <p>
     * 더 빠른 페이지로 교체된 느린 페이지는 응답이 도착해도 돌아오지 않고, 늦은 응답이 이력이나 진행률을 건드리지도 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLateResponseNeverBringsTheOldPageBack() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch gate = start(arrived, TestHttpServer.Response.html("<p>slow</p>"));
        String base = "http://127.0.0.1:" + servers.get(0).getPort();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(base + "/slow");
        Check.isTrue(arrived.await(30, TimeUnit.SECONDS), "the slow request reached the server");
        Check.isTrue(p.getStatusText().startsWith("Loading") || p.getProgress() != 100, "the slow page is loading");
        Check.equal("about:blank", p.getPage(), "the old page stays while the new one loads");
        p.setPage(base + "/fast");
        await(p, base + "/fast", "Done");
        gate.countDown();
        watchStays(p, base + "/fast", "<p>fast</p>", "about:blank," + base + "/fast");
        Check.equal("Done", p.getStatusText(), "the status stays");
    }

    /**
     * A refresh or a new navigation during a load wins over the load; a series of quick navigations ends on the last address, and the
     * history holds only pages that were confirmed.
     * <p>
     * 로딩 중의 새로고침이나 새 이동은 그 로딩을 이긴다. 빠르게 이어지는 이동은 마지막 주소에서 끝나고 이력에는 확정된 페이지만 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshAndQuickNavigationsWinOverALoad() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch gate = start(arrived, TestHttpServer.Response.html("<p>slow</p>"));
        String base = "http://127.0.0.1:" + servers.get(0).getPort();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(base + "/fast");
        await(p, base + "/fast", "Done");
        // 느린 페이지를 읽는 도중 새로고침하면 새로고침이 이기고 이력에는 느린 페이지가 없다.
        p.setPage(base + "/slow");
        Check.isTrue(arrived.await(30, TimeUnit.SECONDS), "the slow request reached the server");
        p.refresh(true);
        await(p, base + "/fast", "Done");
        gate.countDown();
        watchStays(p, base + "/fast", "<p>fast</p>", "about:blank," + base + "/fast");
        // 연달아 이동하면 마지막 주소만 남고 도중에 취소된 것은 이력에 없다.
        p.setPage(base + "/fast2");
        p.setPage(base + "/fast");
        p.setPage(base + "/fast2");
        await(p, base + "/fast2", "Done");
        Check.equal("<p>fast2</p>", p.getContent(), "the source of the last address");
        Check.equal(base + "/fast2", p.getHistories().lastElement(), "the last address is the last entry");
        Check.isTrue(p.getHistories().size() <= 5, "at most the confirmed pages are entries: " + join(p));
        watchStays(p, base + "/fast2", "<p>fast2</p>", join(p));
    }

    /**
     * A <code>clean()</code> during a load leaves the blank page, and the answer that comes later does not restore it.
     * <p>
     * 로딩 중의 <code>clean()</code> 은 빈 페이지를 남기고, 나중에 오는 응답이 그것을 되살리지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCleanDuringALoadLeavesTheBlankPage() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch gate = start(arrived, TestHttpServer.Response.html("<p>slow</p>"));
        String base = "http://127.0.0.1:" + servers.get(0).getPort();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(base + "/fast");
        await(p, base + "/fast", "Done");
        p.setPage(base + "/slow");
        Check.isTrue(arrived.await(30, TimeUnit.SECONDS), "the slow request reached the server");
        p.clean();
        // clean() 이 끝난 상태는 초기 상태와 같다: 빈 페이지, 빈 상태 문구, 진행률 100(API 계약 5.11).
        await(p, "about:blank", "");
        gate.countDown();
        watchStays(p, "about:blank", "", "about:blank");
    }

    /**
     * An answer that breaks off after a newer page was confirmed does not put an error page on the screen.
     * <p>
     * 더 새로운 페이지가 확정된 뒤 중간에 끊기는 응답은 오류 페이지를 화면에 올리지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenOffAnswerOfAReplacedLoadIsIgnored() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch gate = start(arrived, new TestHttpServer.Response(200).text("text/html", "<p>0123456789</p>").truncateAt(5));
        String base = "http://127.0.0.1:" + servers.get(0).getPort();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(base + "/slow");
        Check.isTrue(arrived.await(30, TimeUnit.SECONDS), "the slow request reached the server");
        p.setPage(base + "/fast");
        await(p, base + "/fast", "Done");
        gate.countDown();
        watchStays(p, base + "/fast", "<p>fast</p>", "about:blank," + base + "/fast");
        Check.equal("Done", p.getStatusText(), "no error status from the broken answer");
    }

    /**
     * When nothing newer happens, a truncated answer of the current load is an error page of that load (and a normal one afterwards works).
     * <p>
     * 더 새로운 일이 없으면 현재 로딩의 잘린 응답은 그 로딩의 오류 페이지가 된다(그 뒤의 정상 이동은 동작한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenOffAnswerOfTheCurrentLoadIsAnErrorPage() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch gate = start(arrived, new TestHttpServer.Response(200).text("text/html", "<p>0123456789</p>").truncateAt(5));
        String base = "http://127.0.0.1:" + servers.get(0).getPort();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(base + "/slow");
        Check.isTrue(arrived.await(30, TimeUnit.SECONDS), "the slow request reached the server");
        gate.countDown();
        long end = System.currentTimeMillis() + 30000;
        // 오류 페이지가 확정될 때까지 짧게 쉬며 기다린다.
        while (!(base + "/slow").equals(p.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        TestSupport.flushEdt();
        Check.equal(base + "/slow", p.getPage(), "the failed address is the page");
        Check.isTrue(p.getStatusText().startsWith("Error"), "an error page was confirmed: " + p.getStatusText());
        Check.equal(100, p.getProgress(), "the load of the error page ended");
        p.setPage(base + "/fast");
        await(p, base + "/fast", "Done");
        Check.equal("<p>fast</p>", p.getContent(), "a normal navigation works afterwards");
    }
}
