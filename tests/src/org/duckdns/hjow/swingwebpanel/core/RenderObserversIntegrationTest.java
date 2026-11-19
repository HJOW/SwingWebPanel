package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests <code>IntersectionObserver</code> and <code>ResizeObserver</code> in a real {@link SwingWebPanel} inside a real <code>JScrollPane</code>, with the real
 * layout of the page (Intersection Observer, W3C Working Draft 18 October 2023, and Resize Observer Module Level 1, both fetched 2026-11-17; see
 * <code>docs/progress/webapi2.md</code>): the sizes that a <code>ResizeObserver</code> reports follow the layout of the page when the viewport is resized, the
 * entries of an <code>IntersectionObserver</code> follow the scrolling of the viewport, and the observers of a page that was left report nothing. The page
 * reports to <code>window.log</code>, which the test reads on the document queue.
 * <p>
 * 실제 <code>JScrollPane</code> 안의 실제 {@link SwingWebPanel} 에서 페이지의 실제 레이아웃으로 <code>IntersectionObserver</code> 와 <code>ResizeObserver</code> 를
 * 시험한다(Intersection Observer W3C Working Draft 2023-10-18 과 Resize Observer Module Level 1, 모두 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조):
 * 뷰포트 크기가 바뀌면 <code>ResizeObserver</code> 가 보고하는 크기가 페이지의 레이아웃을 따르고, <code>IntersectionObserver</code> 의 항목이 뷰포트의 스크롤을
 * 따르며, 떠난 페이지의 관찰자는 아무것도 보고하지 않는다. 페이지는 <code>window.log</code> 에 보고하고 시험은 문서 큐에서 읽는다.
 */
public class RenderObserversIntegrationTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public RenderObserversIntegrationTest() {
    }

    /**
     * Returns the session of a panel (the component keeps it in a private field).
     * <p>
     * 패널의 세션을 반환한다(컴포넌트가 비공개 필드에 가지고 있다).
     *
     * @param panel the panel / 패널
     * @return the session / 세션
     * @throws Exception on reflection failure / 리플렉션 실패 시
     */
    private static BrowserSession session(SwingWebPanel panel) throws Exception {
        Field field = SwingWebPanel.class.getDeclaredField("session");
        field.setAccessible(true);
        return (BrowserSession) field.get(panel);
    }

    /**
     * Evaluates a script on the document queue of the shown page.
     * <p>
     * 보이는 페이지의 문서 큐에서 스크립트를 평가한다.
     *
     * @param session the session / 세션
     * @param code    the script / 스크립트
     * @return the result as text / 문자열 결과
     * @throws Exception on failure / 실패 시
     */
    private static String evaluate(final BrowserSession session, final String code) throws Exception {
        final String[] result = new String[1];
        final Throwable[] failure = new Throwable[1];
        final CountDownLatch done = new CountDownLatch(1);
        session.submitDocumentTask(new Runnable() {
            /**
             * Enters the script host of the shown page.
             * <p>
             * 보이는 페이지의 스크립트 호스트에 들어간다.
             */
            public void run() {
                try {
                    Field field = HtmlPageDocument.class.getDeclaredField("runtime");
                    field.setAccessible(true);
                    PageRuntime runtime = (PageRuntime) field.get(session.getSnapshot().getDocument());
                    result[0] = runtime.scripts().evaluate(code, "observers-test");
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    done.countDown();
                }
            }
        });
        Check.isTrue(done.await(10, TimeUnit.SECONDS), "the document queue ran the script");
        Check.isNull(failure[0], "the script failed: " + failure[0]);
        return result[0];
    }

    /**
     * Creates a panel in a scroll pane of a given size on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 주어진 크기의 스크롤 창 안에 패널을 만든다.
     *
     * @param panes the array that receives the scroll pane / 스크롤 창을 받는 배열
     * @param width the width of the scroll pane / 스크롤 창의 폭
     * @param height the height of the scroll pane / 스크롤 창의 높이
     * @return the panel / 패널
     * @throws Exception on failure / 실패 시
     */
    private static SwingWebPanel create(final JScrollPane[] panes, final int width, final int height) throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Builds the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                panes[0] = new JScrollPane(result[0]);
                panes[0].setSize(width, height);
                panes[0].doLayout();
            }
        });
        return result[0];
    }

    /**
     * Waits until the page log has the given number of entries, and returns the log.
     * <p>
     * 페이지 기록이 주어진 개수의 항목을 가질 때까지 기다리고 기록을 반환한다.
     *
     * @param session the session / 세션
     * @param count   the number of entries to wait for / 기다릴 항목 수
     * @return the log joined with vertical bars / 세로줄로 이은 기록
     * @throws Exception on failure / 실패 시
     */
    private static String awaitLog(BrowserSession session, int count) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        String log;
        // 마감까지 기록의 항목 수가 충분해지는지 계속 확인한다.
        do {
            log = evaluate(session, "log.join('|')");
            if (log.length() > 0 && log.split("\\|").length >= count) {
                return log;
            }
            Thread.sleep(30);
        } while (System.currentTimeMillis() < deadline);
        Check.fail("the log did not reach " + count + " entries: " + log);
        return log;
    }

    /**
     * A resize observer reports the sizes of the real layout: the first report after the page is laid out, and again when the viewport is resized and the
     * percentage width of the element follows it.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testResizeObserverFollowsTheLayout() throws Exception {
        JScrollPane[] panes = new JScrollPane[1];
        SwingWebPanel panel = create(panes, 320, 200);
        BrowserSession session = session(panel);
        try {
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}#box{width:50%;height:40px;padding:4px 6px;border:2px solid black}</style>"
                    + "<div id=box></div><script>window.log = []; function n(v) { return Math.round(v); } new ResizeObserver(function (entries) { entries.forEach(function (e) { "
                    + "log.push(n(e.contentBoxSize[0].inlineSize) + 'x' + n(e.contentBoxSize[0].blockSize) + ',' + n(e.borderBoxSize[0].inlineSize) + 'x' + "
                    + "n(e.borderBoxSize[0].blockSize) + ',' + n(e.contentRect.x) + ',' + n(e.contentRect.y)); }); }).observe(document.getElementById('box'));</" + "script>",
                    "text/html");
            TestSupport.settle(session);
            String first = awaitLog(session, 1);
            double width = Double.parseDouble(evaluate(session, "String(innerWidth)"));
            // 폭 50% 는 content 폭이므로 border box 는 padding 12 와 border 4 가 더 크다. 높이 40 에도 padding 8 과 border 4 가 더해진다.
            Check.equal(Math.round(width / 2) + "x40," + Math.round(width / 2 + 16) + "x52,6,4", first, "the first report has the sizes of the layout");
            // 뷰포트를 좁히면 백분율 폭이 따라가고 관찰자가 새 크기를 보고한다.
            SwingUtilities.invokeAndWait(narrow(panes[0], 220));
            String second = awaitLog(session, 2);
            double narrow = Double.parseDouble(evaluate(session, "String(innerWidth)"));
            Check.isTrue(narrow < width, "the viewport is narrower now");
            Check.equal(Math.round(narrow / 2) + "x40," + Math.round(narrow / 2 + 16) + "x52,6,4", second.split("\\|")[1], "the second report has the new width: " + second);
        } finally {
            panel.dispose();
        }
    }

    /**
     * Returns a task that resizes a scroll pane.
     * <p>
     * 스크롤 창의 크기를 바꾸는 작업을 반환한다.
     *
     * @param pane  the scroll pane / 스크롤 창
     * @param width the new width / 새 폭
     * @return the task / 작업
     */
    private static Runnable narrow(final JScrollPane pane, final int width) {
        return new Runnable() {
            /**
             * Resizes the pane and lays it out.
             * <p>
             * 창의 크기를 바꾸고 배치한다.
             */
            public void run() {
                pane.setSize(width, pane.getHeight());
                pane.doLayout();
            }
        };
    }

    /**
     * An intersection observer reports the target as not intersecting when the page is laid out, and the entries follow when the viewport scrolls the target in
     * and out of view.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testIntersectionObserverFollowsScrolling() throws Exception {
        JScrollPane[] panes = new JScrollPane[1];
        SwingWebPanel panel = create(panes, 300, 200);
        BrowserSession session = session(panel);
        try {
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}#top{height:600px}#target{height:100px;background:#abc}#tail{height:900px}</style>"
                    + "<div id=top></div><div id=target></div><div id=tail></div><script>window.log = []; new IntersectionObserver(function (entries) { "
                    + "entries.forEach(function (e) { log.push(e.isIntersecting + ':' + Math.round(e.intersectionRatio * 100) / 100 + ':' + Math.round(e.boundingClientRect.y)); }); }, "
                    + "{ threshold: [0, 0.5, 1] }).observe(document.getElementById('target'));</" + "script>", "text/html");
            TestSupport.settle(session);
            Check.equal("false:0:600", awaitLog(session, 1), "below the fold: not intersecting, at y 600");
            double height = Double.parseDouble(evaluate(session, "String(innerHeight)"));
            JViewport viewport = panes[0].getViewport();
            // 뷰포트 아래 가장자리가 대상 중간에 오도록 스크롤한다: 보이는 부분은 절반 이상이 아니다.
            scrollTo(viewport, (int) (680 - height));
            String log = awaitLog(session, 2);
            Check.isTrue(log.split("\\|")[1].startsWith("true:0.8"), "80 of 100 pixels visible: " + log);
            // 대상이 완전히 보이게 스크롤한다.
            scrollTo(viewport, 600);
            log = awaitLog(session, 3);
            Check.isTrue(log.split("\\|")[2].startsWith("true:1:0"), "fully visible at the top of the viewport: " + log);
            // 대상을 지나쳐 스크롤하면 교차하지 않는다.
            scrollTo(viewport, 800);
            log = awaitLog(session, 4);
            Check.isTrue(log.split("\\|")[3].startsWith("false:0:"), "scrolled past: not intersecting: " + log);
        } finally {
            panel.dispose();
        }
    }

    /**
     * Scrolls a viewport on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 뷰포트를 스크롤한다.
     *
     * @param viewport the viewport / 뷰포트
     * @param y        the vertical position / 세로 위치
     * @throws Exception on failure / 실패 시
     */
    private static void scrollTo(final JViewport viewport, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Moves the view.
             * <p>
             * 뷰를 옮긴다.
             */
            public void run() {
                viewport.setViewPosition(new Point(0, y));
            }
        });
    }

    /**
     * The observers of a page that was left (the content was replaced) report nothing any more, and neither do those of a disposed component.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testObserversStopWhenThePageIsLeft() throws Exception {
        JScrollPane[] panes = new JScrollPane[1];
        SwingWebPanel panel = create(panes, 300, 200);
        TestHttpServer server = new TestHttpServer();
        // 첫 페이지의 관찰자는 보고할 때마다 sessionStorage 의 카운터를 올린다.
        server.routeFixed("/one", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}#box{width:50%;height:40px}</style><div id=box></div><script>"
                + "function count() { sessionStorage.setItem('n', String(Number(sessionStorage.getItem('n') || 0) + 1)); } "
                + "new ResizeObserver(function () { count(); }).observe(document.getElementById('box')); "
                + "new IntersectionObserver(function () { count(); }).observe(document.getElementById('box'));</" + "script>").header("Cache-Control", "no-store"));
        server.routeFixed("/two", TestHttpServer.Response.html("<!doctype html><p id=other>other page</p>"));
        server.start();
        try {
            panel.setPage(server.url("/one"));
            long deadline = System.currentTimeMillis() + 10000;
            // 두 관찰자가 한 번씩 보고할 때까지 기다린다.
            while (!"2".equals(counter(panel)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(30);
            }
            Check.equal("2", counter(panel), "both observers reported once");
            panel.setPage(server.url("/two"));
            deadline = System.currentTimeMillis() + 10000;
            // 두 번째 페이지가 확정될 때까지 기다린다.
            while (!server.url("/two").equals(panel.getPage()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(30);
            }
            // 이전 페이지의 요소를 바꿀 방법이 없으므로 뷰포트 크기 변화로 모든 단계를 일으켜 본다.
            SwingUtilities.invokeAndWait(narrow(panes[0], 180));
            Thread.sleep(300);
            Check.equal("2", counter(panel), "the observers of the page that was left reported nothing more");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Reads the counter that the observers of the page keep in <code>sessionStorage</code>.
     * <p>
     * 페이지의 관찰자가 <code>sessionStorage</code> 에 두는 카운터를 읽는다.
     *
     * @param panel the panel / 패널
     * @return the count as text, or <code>0</code> / 문자열 횟수. 없으면 <code>0</code>
     */
    private static String counter(SwingWebPanel panel) {
        // 복사한 저장소 항목에서 카운터 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if ("n".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "0";
    }
}
