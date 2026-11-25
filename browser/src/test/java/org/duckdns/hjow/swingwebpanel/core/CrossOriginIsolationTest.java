package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptText;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer;
import org.duckdns.hjow.swingwebpanel.net.FetchLimits;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Cross-origin response policy tests through the component's HTTP, parsing and script loading pipeline.
 * <p>
 * 컴포넌트의 HTTP, 파싱, 스크립트 로드 경로를 통한 교차 오리진 응답 정책 시험.
 */
public class CrossOriginIsolationTest {
    /**
     * Uses the final response's policy for documents and classic and module script responses.
     * <p>
     * 문서와 일반 및 모듈 스크립트 응답에 최종 응답의 정책을 사용한다.
     *
     * @throws Exception on setup or request failure / 환경 구성이나 요청 실패 시
     */
    public void testFinalResponseSettings() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CookieStore cookies = new CookieStore();
        ResourceFetcher fetcher = new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader());
        ParsingPageLoader loader = new ParsingPageLoader(new NetworkPageLoader(fetcher, cookies));
        BrowserSession session = new BrowserSession(loader, null, cookies);
        PageView view = new PageView(session, new ViewMetrics(), new PageView.Host() {
            /** The test inspects the document directly. / 시험이 문서를 직접 확인한다. */
            public void viewChanged() {
            }
        }, fetcher, cookies);
        loader.bindScripts(session, fetcher, cookies, view);
        try {
            server.routeFixed("/redirect", new TestHttpServer.Response(302).header("Location", "/doc")
                    .header("Cross-Origin-Opener-Policy", "same-origin-allow-popups")
                    .header("Cross-Origin-Embedder-Policy", "credentialless"));
            server.routeFixed("/doc", TestHttpServer.Response.html("<title>settings</title><script>var trust=isSecureContext;</script>")
                    .header("Cross-Origin-Opener-Policy", "same-origin").header("Cross-Origin-Embedder-Policy", "require-corp"));
            server.routeFixed("/script-redirect", new TestHttpServer.Response(302).header("Location", "/entry.js")
                    .header("Cross-Origin-Embedder-Policy", "require-corp"));
            server.routeFixed("/entry.js", TestHttpServer.Response.ok("text/javascript", "var x = 42;")
                    .header("Cross-Origin-Embedder-Policy", "credentialless"));
            server.start();

            session.setPage(server.url("/redirect"));
            TestSupport.settle(session);
            PageSnapshot snapshot = session.getSnapshot();
            Check.equal("settings", snapshot.getTitle(), "the final page was parsed");
            Check.equal("same-origin", snapshot.getCrossOriginPolicy().getOpener(), "the final COOP token");
            Check.equal(CrossOriginPolicy.Embedder.REQUIRE_CORP, snapshot.getCrossOriginPolicy().getEmbedder(), "the final COEP token");
            Check.same(snapshot.getCrossOriginPolicy(), snapshot.withoutDocument().getCrossOriginPolicy(), "history retains the policy");
            PageScripts scripts = ((HtmlPageDocument) snapshot.getDocument()).scripts();
            Check.equal("true", Ops.toString(scripts.getRealm().getGlobalObject().get("trust")), "loopback HTTP is a secure context");

            ScriptSource source = scripts.getEnvironment().getScriptSource();
            String redirected = server.url("/script-redirect");
            ScriptText classic = ((ScriptSource.WithResponse) source).fetchScript(redirected);
            ScriptText module = ((ModuleScriptSource.WithAttributes) source)
                    .fetchModule(new ModuleRequest(redirected), ModuleScriptSource.Credentials.SAME_ORIGIN, true);
            Check.equal(server.url("/entry.js"), classic.getUrl(), "the classic script records its final URL");
            Check.equal(classic.getUrl(), module.getUrl(), "the module loader also records its final URL");
            Check.equal(CrossOriginPolicy.Embedder.CREDENTIALLESS, classic.getCrossOriginPolicy().getEmbedder(), "the final classic policy");
            Check.equal(classic.getCrossOriginPolicy().getEmbedder(), module.getCrossOriginPolicy().getEmbedder(), "the final module policy");
        } finally {
            view.dispose();
            session.dispose();
            server.stop();
        }
    }
}
