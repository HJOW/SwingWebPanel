package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the identity and target of the top WindowProxy through the browser navigation pipeline.
 * <p>
 * 브라우저 이동 경로에서 최상위 WindowProxy의 정체성과 대상을 시험한다.
 */
public class TopWindowNavigationTest {
    /**
     * Checks a retained WindowProxy after a superseded load and a second committed document.
     * <p>
     * 대체된 로딩과 두 번째 문서 확정 뒤에도 보관한 WindowProxy가 유효한지 확인한다.
     *
     * @throws Exception if setup or navigation fails / 환경 구성이나 이동 실패 시
     */
    public void testTopWindowProxyFollowsNavigation() throws Exception {
        final String staleUrl = "http://stale.test/pending";
        final CountDownLatch entered = new CountDownLatch(1);
        FakeLoader loader = new FakeLoader() {
            /**
             * Signals that the transport of the superseded request started.
             * <p>
             * 대체될 요청의 전송이 시작되었음을 알린다.
             *
             * @param request the navigation / 이동 요청
             * @return its loaded page / 읽은 페이지
             * @throws Exception on load failure / 로딩 실패 시
             */
            public PageSnapshot load(NavigationRequest request) throws Exception {
                if (staleUrl.equals(request.getUrl())) {
                    entered.countDown();
                }
                return super.load(request);
            }
        };
        loader.ignoreCancel = true;
        loader.gate(staleUrl);
        CookieStore cookies = new CookieStore();
        ResourceFetcher fetcher = new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader());
        ParsingPageLoader parser = new ParsingPageLoader(loader);
        BrowserSession session = new BrowserSession(parser, null, cookies);
        PageView view = new PageView(session, new ViewMetrics(), new PageView.Host() {
            /** The test observes document state directly. / 시험에서 문서 상태를 직접 관찰한다. */
            public void viewChanged() {
            }
        }, fetcher, cookies);
        parser.bindScripts(session, fetcher, cookies, view);
        try {
            session.setContent("<title>old</title><script>var oldProperty=1;"
                    + "var parserProbe=top.document===document&&window===top&&self===globalThis?'first':'wrong';</script>", "text/html");
            TestSupport.settle(session);
            HtmlPageDocument old = (HtmlPageDocument) session.getSnapshot().getDocument();
            PageScripts first = old.scripts();
            final JsObject held = first.getRealm().getGlobalThisValue();
            Check.equal("first", Ops.toString(first.getRealm().getGlobalObject().get("parserProbe")),
                    "the parser sees its own top window before confirmation");

            session.setPage(staleUrl);
            Check.isTrue(entered.await(10, TimeUnit.SECONDS), "the stale load starts");
            Check.same(old.getDocument(), session.topContext().getDocument(), "loading leaves the confirmed document visible");
            session.setContent("<title>new</title><script>var newProperty=2;"
                    + "var parserProbe=top.document===document&&window===top&&window.oldProperty===undefined?'second':'wrong';</script>", "text/html");
            loader.release(staleUrl);
            TestSupport.settle(session);
            HtmlPageDocument now = (HtmlPageDocument) session.getSnapshot().getDocument();
            final PageScripts active = now.scripts();
            Check.equal("second", Ops.toString(active.getRealm().getGlobalObject().get("parserProbe")),
                    "the parser resolves the fresh global before confirmation");
            Check.same(held, active.getRealm().getGlobalThisValue(), "one stable top-level WindowProxy");
            Check.isTrue(first.getRealm().getGlobalObject() != active.getRealm().getGlobalObject(), "the two documents own distinct globals");
            Check.same(now.getDocument(), session.topContext().getDocument(), "the stale transport cannot replace the active target");

            final String[] result = new String[1];
            session.submitDocumentTask(new Runnable() {
                /** Reads the retained proxy from the active document. / 현재 문서에서 보관한 프록시를 읽는다. */
                public void run() {
                    active.getRealm().getGlobalObject().set("held", held);
                    result[0] = active.evaluate("[held===window,held.document===document,held.newProperty,!('oldProperty' in held)].join()", "verify");
                }
            });
            TestSupport.settle(session);
            Check.equal("true,true,2,true", result[0], "the retained proxy resolves all properties against the active document");
        } finally {
            loader.release(staleUrl);
            view.dispose();
            session.dispose();
        }
    }
}
