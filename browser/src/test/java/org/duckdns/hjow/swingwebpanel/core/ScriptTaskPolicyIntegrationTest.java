package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Verifies host-selected script time limits on actual top-level navigations.
 * <p>
 * 실제 최상위 문서 이동에서 호스트 지정 스크립트 시간 한도를 검증한다.
 */
public final class ScriptTaskPolicyIntegrationTest {
    /** Creates the test suite. / 시험 묶음을 만든다. */
    public ScriptTaskPolicyIntegrationTest() {
    }

    /**
     * Applies a short limit to a redirected page, keeping the default for another top page and its frame.
     * <p>
     * 리디렉션한 페이지에 짧은 한도를 적용하고 다른 최상위 페이지와 그 프레임에는 기본값을 적용한다.
     *
     * @throws Exception if the HTTP fixtures or page scripts fail / HTTP 시험 자료나 페이지 스크립트가 실패하면
     */
    public void testPolicyUsesFinalTopDocumentUrl() throws Exception {
        TestHttpServer site = new TestHttpServer();
        site.start();
        final String limited = site.url("/limited");
        final List<String> selected = Collections.synchronizedList(new ArrayList<String>());
        SwingWebPanel panel = new SwingWebPanel() {
            /** Sets a short limit for just one address. / 한 주소에만 짧은 한도를 정한다. */
            @Override
            protected long getScriptTaskTimeLimitMillis(String url) {
                selected.add(url);
                return limited.equals(url) ? 25 : 0;
            }
        };
        Inbox inbox = new Inbox();
        final CountDownLatch aborted = new CountDownLatch(1);
        try {
            String work = "const end=Date.now()+180;while(Date.now()<end){};";
            site.routeFixed("/redirect", TestHttpServer.Response.redirect(302, limited));
            site.routeFixed("/limited", TestHttpServer.Response.html("<script>" + work + "opener.postMessage('late','*');</script>"
                    + "<script>opener.postMessage('after abort','*');</script>"));
            site.routeFixed("/normal", TestHttpServer.Response.html("<script>onmessage=e=>opener.postMessage(e.data,'*');"
                    + work + "opener.postMessage('default time','*');</script><iframe src='/frame'></iframe>"));
            site.routeFixed("/frame", TestHttpServer.Response.html("<script>" + work + "parent.postMessage('frame time','*');</script>"));
            panel.addWebMessageListener(inbox);
            panel.addConsoleListener(new ConsoleListener() {
                /** Observes the real engine timeout. / 실제 엔진 시간 중단을 확인한다. */
                public void consoleMessage(ConsoleEvent event) {
                    if (event.getKind() == ConsoleEvent.Kind.SCRIPT_ABORTED && event.getMessage().contains("25 ms")) {
                        aborted.countDown();
                    }
                }
            });
            panel.setPage(site.url("/redirect"));
            Check.equal("after abort", inbox.next().getData(), "another script runs after the limited script was aborted");
            Check.isTrue(aborted.await(5, TimeUnit.SECONDS), "the limit was reported by the console");
            panel.setPage(site.url("/normal"));
            Check.equal("default time", inbox.next().getData(), "a nonpositive host selection keeps the default");
            Check.equal("frame time", inbox.next().getData(), "the embedded document also runs with the default");
            Check.equal(java.util.Arrays.asList(limited, site.url("/normal")), selected, "the host selects only the final URL of top documents");
        } finally {
            panel.dispose();
            site.stop();
        }
    }
}
