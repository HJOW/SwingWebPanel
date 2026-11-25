package org.duckdns.hjow.swingwebpanel.core;

import java.util.Collections;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
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
import org.duckdns.hjow.swingwebpanel.net.HttpCache;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Cross-origin response policy tests through the component's HTTP, parsing and script loading pipeline.
 * <p>
 * 컴포넌트의 HTTP, 파싱, 스크립트 로드 경로를 통한 교차 오리진 응답 정책 시험.
 */
public class CrossOriginIsolationTest {
    /**
     * Enforces the policy for cached scripts, allowed CORS scripts and child frame navigations.
     * <p>
     * 캐시된 스크립트, CORS로 허용된 스크립트, 하위 프레임 이동에 정책을 적용한다.
     *
     * @throws Exception on setup or request failure / 환경 구성이나 요청 실패 시
     */
    public void testRequireCorpDocumentsAndResources() throws Exception {
        TestHttpServer site = new TestHttpServer();
        TestHttpServer external = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            external.routeFixed("/cached.js", TestHttpServer.Response.ok("text/javascript", "window.fromCache='ran';")
                    .header("Cache-Control", "public, max-age=600"));
            external.routeFixed("/corp.js", TestHttpServer.Response.ok("text/javascript", "window.withCorp='allowed';")
                    .header("Cross-Origin-Resource-Policy", "cross-origin"));
            external.routeFixed("/cors.js", TestHttpServer.Response.ok("text/javascript", "window.withCors='allowed';")
                    .header("Access-Control-Allow-Origin", "*"));
            external.routeFixed("/embedded", TestHttpServer.Response.html("<script>parent.postMessage('embedded','*');</script>")
                    .header("Cross-Origin-Embedder-Policy", "require-corp").header("Cross-Origin-Resource-Policy", "cross-origin"));
            external.start();
            site.routeFixed("/plain", TestHttpServer.Response.html("<script src='" + external.url("/cached.js") + "'></script>"
                    + "<script>sessionStorage.setItem('unrestricted',String(window.fromCache));</script>"));
            site.routeFixed("/bare-frame", TestHttpServer.Response.html("<script>window.frameValue='unexpected';</script>"));
            site.routeFixed("/isolated-frame", TestHttpServer.Response.html("<script>window.frameValue='allowed';</script>")
                    .header("Cross-Origin-Embedder-Policy", "require-corp"));
            site.routeFixed("/isolated", TestHttpServer.Response.html(
                    "<script>onmessage=e=>sessionStorage.setItem('from-frame',e.data);</script>"
                    + "<script src='" + external.url("/cached.js") + "'></script>"
                    + "<script src='" + external.url("/corp.js") + "'></script>"
                    + "<script type=module src='" + external.url("/cors.js") + "'></script>"
                    + "<iframe src='/bare-frame'></iframe><iframe src='/isolated-frame'></iframe>"
                    + "<iframe src='" + external.url("/embedded") + "'></iframe>"
                    + "<script>window.onload=()=>sessionStorage.setItem('policy-result',[typeof fromCache,withCorp,withCors,"
                    + "typeof frames[0].frameValue,frames[1].frameValue].join());</script>")
                    .header("Cross-Origin-Opener-Policy", "same-origin")
                    .header("Cross-Origin-Embedder-Policy", "require-corp"));
            site.start();
            panel.setPage(site.url("/plain"));
            FrameTestSupport.awaitStored(panel, "unrestricted", "ran");
            panel.setPage(site.url("/isolated"));
            FrameTestSupport.awaitStored(panel, "policy-result", "undefined,allowed,allowed,undefined,allowed");
            FrameTestSupport.awaitStored(panel, "from-frame", "embedded");
        } finally {
            panel.dispose();
            external.stop();
            site.stop();
        }
    }

    /**
     * Separates credentialless cache variants and omits credentials at every cross-origin hop.
     * <p>
     * credentialless 캐시 변형을 분리하고 교차 오리진 구간마다 자격 증명을 생략한다.
     *
     * @throws Exception on setup or request failure / 환경 구성이나 요청 실패 시
     */
    public void testCredentiallessRedirectAndCache() throws Exception {
        TestHttpServer site = new TestHttpServer();
        TestHttpServer external = new TestHttpServer();
        ResourceFetcher fetcher = new ResourceFetcher(DefaultSslConfigurer.INSTANCE, FetchLimits.DEFAULT, getClass().getClassLoader());
        fetcher.setCache(new HttpCache());
        CookieStore cookies = new CookieStore();
        try {
            external.routeFixed("/asset", TestHttpServer.Response.ok("text/javascript", "/* asset */")
                    .header("Cache-Control", "public, max-age=600").header("Set-Cookie", "new=from-response; Path=/"));
            external.start();
            site.routeFixed("/hop", new TestHttpServer.Response(302).header("Location", external.url("/asset")));
            site.start();
            cookies.storeFromResponse(external.url("/asset"), Collections.singletonList("session=private; Path=/"));
            RequestHeaders extra = new RequestHeaders();
            extra.addAll(Collections.singletonMap("Authorization", "Basic dGVzdDp0ZXN0"));
            ResourceRequest request = new ResourceRequest(external.url("/asset")).setExtraHeaders(extra).setCookies(cookies)
                    .setInitiator(Origin.of(site.url("/doc"))).setPurpose(RequestPurpose.SCRIPT);
            Check.isFalse(fetcher.fetch(request).isError(), "the unrestricted resource is cached");
            Check.isTrue(external.requests().get(0).header("Cookie").indexOf("session=private") >= 0, "ordinary request sends cookies");
            cookies.clear();
            cookies.storeFromResponse(external.url("/asset"), Collections.singletonList("session=private; Path=/"));
            request.setEmbedderPolicy(CrossOriginPolicy.Embedder.CREDENTIALLESS);
            Check.isFalse(fetcher.fetch(request).isError(), "credentialless no-CORS resource loads without a CORP header");
            Check.equal(2, external.requests().size(), "credentialless does not use the credentialed cache entry");
            Check.isNull(external.requests().get(1).header("Cookie"), "credentialless omits outgoing cookies");
            Check.isNull(external.requests().get(1).header("Authorization"), "credentialless omits explicit authorization");
            Check.equal("session=private", cookies.getCookieHeader(external.url("/asset")), "credentialless ignores Set-Cookie");
            ResourceResponse hit = fetcher.fetch(request);
            Check.isTrue(hit.isFromCache(), "the credentialless partition has its own reusable response");
            ResourceRequest redirect = new ResourceRequest(site.url("/hop")).setCookies(cookies).setInitiator(Origin.of(site.url("/doc")))
                    .setPurpose(RequestPurpose.SCRIPT).setEmbedderPolicy(CrossOriginPolicy.Embedder.CREDENTIALLESS);
            Check.isFalse(fetcher.fetch(redirect).isError(), "a cross-origin redirect also loads without credentials");
            Check.isNull(external.requests().get(2).header("Cookie"), "the redirect's cross-origin hop omits cookies");
            Check.isTrue(site.requests().get(0).header("Cookie").indexOf("session=private") >= 0, "the same-origin hop retains cookies");
        } finally {
            external.stop();
            site.stop();
        }
    }

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
