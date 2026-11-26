package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The closing of a window that web content created (case C-ROOTCLOSE-7, plan item P30-03, API contract 5.23). Until P31 adds popups, no
 * public path creates such a window, so the test marks a session itself: scripts may close it whatever the setting for the window of the
 * host says, the close handler hears that web content created the window (so that the program is never ended), and a navigation of the
 * page does not cancel the closing.
 * <p>
 * 웹 콘텐츠가 만든 창의 닫기(사례 C-ROOTCLOSE-7, 계획 P30-03, API 계약 5.23). P31 이 팝업을 추가하기 전에는 그런 창을 만드는 공개 경로가 없으므로
 * 시험이 세션에 직접 표시한다: 호스트 창에 대한 설정과 상관없이 스크립트가 닫을 수 있고, 닫기 처리기는 웹 콘텐츠가 만든 창임을 전달받으며(그래서
 * 프로그램을 끝내지 않는다), 페이지의 이동은 닫기를 취소하지 않는다.
 */
public class WebContentWindowClosingTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebContentWindowClosingTest() {
    }

    /**
     * Waits until the handler was called a number of times.
     * <p>
     * 처리기가 일정 횟수 호출될 때까지 기다린다.
     *
     * @param calls the record of the calls / 호출 기록
     * @param n     the number / 횟수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(List<String> calls, int n) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 호출 횟수에 이를 때까지 짧게 쉬며 기다린다.
        for (;;) {
            synchronized (calls) {
                if (calls.size() >= n) {
                    break;
                }
            }
            if (System.currentTimeMillis() >= end) {
                break;
            }
            Thread.sleep(10);
        }
        synchronized (calls) {
            Check.equal(n, calls.size(), "calls of the close handler: " + calls);
        }
    }

    /**
     * C-ROOTCLOSE-7: a window that web content created is closed by its script although the setting of the host is off; the handler
     * runs once on the event dispatch thread and is told that web content created the window; a navigation that the page starts right after
     * the call does not cancel it, and <code>closed</code> is true at once. The same page in a window of the host is not closed.
     * <p>
     * C-ROOTCLOSE-7: 웹 콘텐츠가 만든 창은 호스트의 설정이 꺼져 있어도 그 스크립트가 닫는다. 처리기는 이벤트 디스패치 스레드에서 한 번 실행되고 웹
     * 콘텐츠가 만든 창임을 전달받는다. 호출 직후에 페이지가 시작한 이동은 닫기를 취소하지 않고 <code>closed</code> 는 곧바로 true 이다. 같은 페이지를
     * 호스트의 창에 띄우면 닫히지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_7_WindowCreatedByWebContent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CookieStore cookies = new CookieStore();
        ResourceFetcher fetcher = new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader());
        ParsingPageLoader loader = new ParsingPageLoader(new NetworkPageLoader(fetcher, cookies));
        final BrowserSession session = new BrowserSession(loader, null, cookies);
        PageView view = new PageView(session, new ViewMetrics(), new PageView.Host() {
            /** The test looks at the session only. / 시험은 세션만 본다. */
            public void viewChanged() {
            }
        }, fetcher, cookies);
        loader.bindScripts(session, fetcher, cookies, view);
        final List<String> calls = new ArrayList<String>();
        session.setWindowCloseHandler(new WindowCloseHandler() {
            /** Records who created the window, the thread and the state. / 창을 만든 주체, 스레드, 상태를 기록한다. */
            public void closeWindow(boolean createdByWebContent) {
                synchronized (calls) {
                    calls.add((createdByWebContent ? "web" : "host") + (SwingUtilities.isEventDispatchThread() ? "@edt" : "@other")
                            + (session.isWindowClosing() ? ",closing" : ",open"));
                }
            }
        });
        try {
            server.routeFixed("/slow", TestHttpServer.Response.html("<title>slow</title>").delay(400));
            // 이동이 시작되면 옛 페이지의 스크립트는 그 자리에서 중단되므로 닫기를 먼저 부른다.
            server.routeFixed("/close", TestHttpServer.Response.html("<title>close</title><script>window.close();\nwindow.close();\n"
                    + "location.href = '/slow';</" + "script>"));
            server.start();
            // 호스트가 만든 창: 설정이 꺼져 있으므로 닫히지 않는다.
            Check.isFalse(session.isCreatedByWebContent(), "a session is a window of the host by default");
            session.setPage(server.url("/close"));
            TestSupport.waitForPage(session, server.url("/slow"));
            TestSupport.settle(session);
            synchronized (calls) {
                Check.equal(0, calls.size(), "the window of the host is not closed: " + calls);
            }
            Check.isFalse(session.isWindowClosing(), "the window of the host is not closing");
            // 웹 콘텐츠가 만든 창: 같은 설정에서도 닫힌다.
            session.setCreatedByWebContent(true);
            Check.isFalse(session.isRootWindowClosingEnabled(), "the setting of the host stays off");
            session.setPage(server.url("/close"));
            await(calls, 1);
            synchronized (calls) {
                Check.equal("[web@edt,closing]", calls.toString(), "one call on the EDT for a window that web content created");
            }
            // 페이지가 시작한 이동이 끝나도 닫는 중 상태는 그대로이다.
            TestSupport.waitForPage(session, server.url("/slow"));
            TestSupport.settle(session);
            Check.isTrue(session.isWindowClosing(), "the navigation of the page does not cancel the closing");
            Check.isTrue(session.topContext().isClosing() && !session.topContext().isClosed(), "the context is closing, not discarded");
            synchronized (calls) {
                Check.equal(1, calls.size(), "still one call: " + calls);
            }
            // 호스트의 설정을 꺼도(이미 꺼져 있다) 웹 콘텐츠가 만든 창의 닫기는 취소되지 않고, 호스트의 이동은 취소한다.
            session.setRootWindowClosingEnabled(false);
            Check.isTrue(session.isWindowClosing(), "the setting of the host does not touch this window");
            session.setPage("about:blank");
            Check.isFalse(session.isWindowClosing(), "a navigation of the host cancels the closing");
        } finally {
            view.dispose();
            session.dispose();
            server.stop();
        }
    }
}
