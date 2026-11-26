package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;
import org.duckdns.hjow.swingwebpanel.testing.TestSsl;

/**
 * The asynchronous web request APIs through the real component and real HTTP (plan item P20-08): <code>XMLHttpRequest</code> and
 * <code>fetch()</code> use the component's User-Agent, additional headers and cookie jar, obey the credentials modes (also across a
 * redirect), the same-origin policy with CORS (an allowing server, a refusing server, the exposed headers, the unsupported preflight),
 * the scheme limits, the forbidden request headers, the timeout and the cancellation of requests when the page is replaced. Two local
 * servers on different ports are two origins. The sessionStorage of the component is the channel through which the page reports.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 비동기 웹 요청 API(계획 P20-08): <code>XMLHttpRequest</code> 와 <code>fetch()</code> 는 컴포넌트의 User-Agent, 추가 헤더,
 * 쿠키 저장소를 쓰고, 자격 증명 모드(리디렉션 너머도), CORS 를 포함한 동일 출처 정책(허용하는 서버, 거부하는 서버, 노출한 헤더, 지원하지 않는 사전 요청),
 * 스킴 제한, 금지된 요청 헤더, 제한 시간, 페이지가 교체될 때의 요청 취소를 따른다. 포트가 다른 두 로컬 서버가 두 오리진이다. 페이지는 컴포넌트의
 * sessionStorage 로 보고한다.
 */
public class WebRequestApiTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebRequestApiTest() {
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
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads a value that a script stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 값을 읽는다.
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
     * Waits until a key has a value and returns it.
     * <p>
     * 키에 값이 생길 때까지 기다리고 그 값을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static String await(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (value(panel, key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.notNull(value(panel, key), "a value for " + key);
        return value(panel, key);
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
     * Returns the origin text of a server.
     * <p>
     * 서버의 오리진 문자열을 반환한다.
     *
     * @param server the server / 서버
     * @return <code>http://localhost:port</code> / <code>http://localhost:port</code>
     */
    private static String origin(TestHttpServer server) {
        String u = server.url("/x");
        return u.substring(0, u.length() - 2);
    }

    /**
     * Wraps a script in a page.
     * <p>
     * 스크립트를 페이지로 감싼다.
     *
     * @param script the script text / 스크립트 텍스트
     * @return the page markup / 페이지 마크업
     */
    private static String page(String script) {
        return "<!doctype html><meta charset=utf-8><script>function put(k, v) { sessionStorage.setItem(k, v); }" + script + "</" + "script>";
    }

    /**
     * XMLHttpRequest and fetch to the page's own origin use the User-Agent, the additional headers and the cookies of the component; the
     * forbidden headers of the page do not get through; the body and the Content-Type reach the server.
     * <p>
     * 페이지 자신의 오리진으로의 XMLHttpRequest 와 fetch 는 컴포넌트의 User-Agent, 추가 헤더, 쿠키를 쓴다. 페이지의 금지된 헤더는 통과하지 못한다. 본문과
     * Content-Type 이 서버에 도달한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSameOriginRequestsUseTheComponentRequestLayer() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/data", TestHttpServer.Response.ok("text/plain", "hello").header("X-Server", "s1").header("Set-Cookie", "sc=1; Path=/"));
            server.route("/echo", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return TestHttpServer.Response.ok("application/json", "{\"method\":\"" + request.method + "\",\"type\":\"" + request.header("Content-Type")
                            + "\",\"len\":" + request.body.length + "}");
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var x = new XMLHttpRequest(); x.open('GET', '/data?x=1'); x.setRequestHeader('X-Page', 'p'); x.setRequestHeader('Cookie', 'evil=1');"
                            + "x.setRequestHeader('User-Agent', 'evil'); x.setRequestHeader('Referer', 'http://evil/'); x.setRequestHeader('Host', 'evil.test');"
                            + "x.onload = function () { put('xhr', x.status + ':' + x.responseText + ':' + x.getResponseHeader('X-Server') + ':' + "
                            + "x.getResponseHeader('Set-Cookie') + ':' + (x.getAllResponseHeaders().toLowerCase().indexOf('set-cookie') < 0)); }; x.send();"
                            + "fetch('/echo', {method: 'POST', headers: {'Content-Type': 'application/json', 'User-Agent': 'evil2', 'Cookie': 'evil=2'}, "
                            + "body: JSON.stringify({a: 1})}).then(function (r) { return r.json(); }).then(function (j) { put('fetch', "
                            + "j.method + ':' + j.type + ':' + j.len); });")).header("Set-Cookie", "sid=1; Path=/"));
            server.start();
            panel.setUserAgent("UA-webapi/1");
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("X-Extra", "e1");
            panel.addHeaders(extra);
            panel.setPage(server.url("/page"));
            Check.equal("200:hello:s1:null:true", await(panel, "xhr"), "XHR response, Set-Cookie is hidden from the page");
            Check.equal("POST:application/json:7", await(panel, "fetch"), "fetch with a JSON body");
            TestHttpServer.Request r = server.last("/data");
            Check.equal("UA-webapi/1", r.header("User-Agent"), "the User-Agent of the component, not the page's");
            Check.equal("e1", r.header("X-Extra"), "the additional header of the component");
            Check.equal("p", r.header("X-Page"), "the header of the page");
            Check.equal("sid=1", r.header("Cookie"), "the cookie jar of the component, not the page's cookie");
            Check.equal(server.url("/page"), r.header("Referer"), "the Referer is the page");
            Check.isTrue(!"evil.test".equals(r.header("Host")), "the Host header is not the page's");
            Check.equal("UA-webapi/1", server.last("/echo").header("User-Agent"), "fetch uses the User-Agent of the component");
            Check.isTrue(server.last("/echo").header("Cookie").indexOf("sid=1") >= 0 && server.last("/echo").header("Cookie").indexOf("evil") < 0,
                    "fetch uses the cookie jar, not the page's cookie: " + server.last("/echo").header("Cookie"));
            Check.isTrue(panel.getCookies().indexOf("sc=1") >= 0, "the cookie of the XHR response was stored: " + panel.getCookies());
            Check.isTrue(!panel.getHeaders().containsKey("User-Agent"), "the page did not change the component headers");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Cross-origin requests need the server's permission: an open server, a refusing server, a server that allows credentials, the
     * headers that a cross-origin response exposes, <code>no-cors</code> and <code>same-origin</code> modes.
     * <p>
     * 교차 오리진 요청은 서버의 허용이 필요하다: 열린 서버, 거부하는 서버, 자격 증명을 허용하는 서버, 교차 오리진 응답이 노출하는 헤더, <code>no-cors</code> 와
     * <code>same-origin</code> 모드.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginRequestsFollowCors() throws Exception {
        TestHttpServer a = new TestHttpServer();
        TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            b.start();
            a.start();
            String ao = origin(a);
            String bo = origin(b);
            b.routeFixed("/open", TestHttpServer.Response.ok("text/plain", "open").header("Access-Control-Allow-Origin", "*"));
            b.routeFixed("/deny", TestHttpServer.Response.ok("text/plain", "deny"));
            b.routeFixed("/cred", TestHttpServer.Response.ok("text/plain", "cred").header("Access-Control-Allow-Origin", ao)
                    .header("Access-Control-Allow-Credentials", "true"));
            b.routeFixed("/credwild", TestHttpServer.Response.ok("text/plain", "credwild").header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Credentials", "true"));
            b.routeFixed("/hdr", TestHttpServer.Response.ok("text/plain", "hdr").header("Access-Control-Allow-Origin", "*").header("X-Secret", "s")
                    .header("X-Exposed", "e").header("Access-Control-Expose-Headers", "X-Exposed").header("Set-Cookie", "other=1; Path=/"));
            a.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var B = '" + bo + "';"
                            + "function done(k, p) { p.then(function (v) { put(k, v); }, function (e) { put(k, 'ERR:' + e.name); }); }"
                            + "done('open', fetch(B + '/open').then(function (r) { return r.type + ':' + r.status; }));"
                            + "done('deny', fetch(B + '/deny').then(function (r) { return 'resolved'; }));"
                            + "done('cred', fetch(B + '/cred', {credentials: 'include'}).then(function (r) { return r.text(); }));"
                            + "done('credwild', fetch(B + '/credwild', {credentials: 'include'}).then(function (r) { return 'resolved'; }));"
                            + "done('nocors', fetch(B + '/deny', {mode: 'no-cors'}).then(function (r) { return r.type + ':' + r.status + ':' + r.headers.has('content-type'); }));"
                            + "done('sameorigin', fetch(B + '/open', {mode: 'same-origin'}).then(function (r) { return 'resolved'; }));"
                            + "var x = new XMLHttpRequest(); x.open('GET', B + '/hdr'); x.onload = function () { put('hdr', [String(x.getResponseHeader('X-Secret')), "
                            + "x.getResponseHeader('X-Exposed'), String(x.getResponseHeader('Set-Cookie')), x.getResponseHeader('Content-Type'), x.status].join('|')); };"
                            + "x.onerror = function () { put('hdr', 'error'); }; x.send();"
                            + "var y = new XMLHttpRequest(); y.open('GET', B + '/deny'); y.onload = function () { put('xdeny', 'loaded'); };"
                            + "y.onerror = function () { put('xdeny', 'error:' + y.status + ':' + y.readyState); }; y.send();")).header("Set-Cookie", "sid=1; Path=/"));
            panel.setPage(a.url("/page"));
            Check.equal("cors:200", await(panel, "open"), "an open server allows the page");
            Check.equal("ERR:TypeError", await(panel, "deny"), "a server without Access-Control-Allow-Origin is refused");
            Check.equal("cred", await(panel, "cred"), "credentials need the exact origin and Allow-Credentials");
            Check.equal("ERR:TypeError", await(panel, "credwild"), "a wildcard origin does not allow credentials");
            Check.equal("opaque:0:false", await(panel, "nocors"), "no-cors gives an opaque response");
            Check.equal("ERR:TypeError", await(panel, "sameorigin"), "same-origin mode refuses another origin");
            Check.equal("null|e|null|text/plain|200", await(panel, "hdr"), "only the exposed and the safelisted headers of a CORS response are visible");
            Check.equal("error:0:4", await(panel, "xdeny"), "XHR fires an error event");
            Check.isNull(b.last("/open").header("Cookie"), "no cookie to another origin by default");
            Check.equal("sid=1", b.last("/cred").header("Cookie"), "the cookie goes along with credentials: include");
            Check.equal(ao, b.last("/open").header("Origin"), "the Origin header of a cross-origin request is the page's");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }

    /**
     * CORS preflight: a cross-origin request with a method or a header that is not safelisted is announced with an OPTIONS request
     * (with the origin of the page, the wanted method and the page's header names, and no cookies) and goes out only if the server grants
     * it; a server that does not grant it ends the request as a network error (<code>TypeError</code>) without receiving it.
     * <p>
     * CORS 사전 요청: 안전 목록에 없는 메서드나 헤더를 가진 교차 오리진 요청은 OPTIONS 요청(페이지의 오리진, 원하는 메서드, 페이지가 쓴 헤더 이름을 싣고
     * 쿠키는 싣지 않는다)으로 미리 알리고 서버가 허락할 때만 나간다. 허락하지 않는 서버는 요청을 받지 못한 채 네트워크 오류(<code>TypeError</code>)로
     * 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPreflightGrantsAndDenials() throws Exception {
        TestHttpServer a = new TestHttpServer();
        TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            b.start();
            a.start();
            final String ao = origin(a);
            String bo = origin(b);
            b.route("/api", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    if ("OPTIONS".equals(request.method)) {
                        return new TestHttpServer.Response(204).header("Access-Control-Allow-Origin", ao)
                                .header("Access-Control-Allow-Methods", "POST, PUT, DELETE").header("Access-Control-Allow-Headers", "content-type, x-custom");
                    }
                    return TestHttpServer.Response.ok("text/plain", "ok:" + request.method + ":" + request.header("Content-Type") + ":" + request.header("X-Custom"))
                            .header("Access-Control-Allow-Origin", ao);
                }
            });
            b.route("/closed", new TestHttpServer.Handler() {
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    if ("OPTIONS".equals(request.method)) {
                        return new TestHttpServer.Response(204);
                    }
                    return TestHttpServer.Response.ok("text/plain", "closed").header("Access-Control-Allow-Origin", ao);
                }
            });
            a.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var B = '" + bo + "';"
                            + "function done(k, p) { p.then(function (v) { put(k, v); }, function (e) { put(k, 'ERR:' + e.name); }); }"
                            + "done('json', fetch(B + '/api', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'}).then(function (r) { return r.text(); }));"
                            + "done('custom', fetch(B + '/api', {method: 'PUT', headers: {'X-Custom': '1'}, body: 'x'}).then(function (r) { return r.text(); }));"
                            + "done('delete', fetch(B + '/api', {method: 'DELETE'}).then(function (r) { return r.text(); }));"
                            + "var x = new XMLHttpRequest(); x.open('PUT', B + '/api'); x.onerror = function () { put('xput', 'error'); };"
                            + "x.onload = function () { put('xput', x.responseText); }; x.send('x');"
                            + "done('refused', fetch(B + '/closed', {method: 'DELETE'}).then(function () { return 'sent'; }));"
                            + "done('refusedjson', fetch(B + '/closed', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'}).then(function () { return 'sent'; }));"
                            + "done('simple', fetch(B + '/closed', {method: 'POST', body: 'plain'}).then(function (r) { return r.text(); }));")));
            panel.setPage(a.url("/page"));
            Check.equal("ok:POST:application/json:null", await(panel, "json"), "a JSON POST goes out after the preflight");
            Check.equal("ok:PUT:text/plain;charset=UTF-8:1", await(panel, "custom"), "a custom header and PUT go out after the preflight");
            Check.equal("ok:DELETE:null:null", await(panel, "delete"), "DELETE goes out after the preflight");
            Check.equal("ok:PUT:text/plain;charset=UTF-8:null", await(panel, "xput"), "XHR PUT goes out after the preflight");
            Check.equal("ERR:TypeError", await(panel, "refused"), "a server that does not grant DELETE ends it");
            Check.equal("ERR:TypeError", await(panel, "refusedjson"), "a server that does not grant a JSON POST ends it");
            Check.equal("closed", await(panel, "simple"), "a simple POST needs no preflight");
            int announced = 0;
            // 서버가 받은 사전 요청마다 오리진과 쿠키 없음, 원하는 메서드가 있는지 확인한다.
            for (TestHttpServer.Request r : b.requests()) {
                if ("OPTIONS".equals(r.method)) {
                    announced++;
                    Check.equal(ao, r.header("Origin"), "the preflight carries the origin of the page");
                    Check.isNull(r.header("Cookie"), "a preflight has no cookies");
                    Check.notNull(r.header("Access-Control-Request-Method"), "the wanted method is announced");
                } else if ("/api".equals(r.path) || "/closed".equals(r.path)) {
                    Check.equal(ao, r.header("Origin"), "the actual request carries the origin of the page: " + r.method + " " + r.path);
                }
            }
            Check.isTrue(announced >= 2 && announced <= 7, "the requests were announced: " + announced);
            Check.equal(1, b.count("/closed") - countOptions(b, "/closed"), "only the simple POST reached /closed");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }

    /**
     * Counts the preflight requests that a server received for a path.
     * <p>
     * 서버가 경로에 대해 받은 사전 요청을 센다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @return the number of OPTIONS requests / OPTIONS 요청 수
     */
    private static int countOptions(TestHttpServer server, String path) {
        int n = 0;
        // 서버 기록에서 경로가 같은 OPTIONS 요청을 센다.
        for (TestHttpServer.Request r : server.requests()) {
            if ("OPTIONS".equals(r.method) && path.equals(r.path)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The credentials modes: omit sends no cookies, same-origin sends them to the page's origin only and does not hand them to the
     * other origin after a redirect, include sends them everywhere (when the server allows it).
     * <p>
     * 자격 증명 모드: omit 은 쿠키를 보내지 않고, same-origin 은 페이지 자신의 오리진에만 보내며 리디렉션 뒤의 다른 오리진에는 넘기지 않고, include 는 어디에나
     * (서버가 허용하면) 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCredentialsModesIncludingRedirects() throws Exception {
        TestHttpServer a = new TestHttpServer();
        TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            b.start();
            a.start();
            String ao = origin(a);
            String bo = origin(b);
            a.routeFixed("/plain", TestHttpServer.Response.ok("text/plain", "plain"));
            a.routeFixed("/to-b", TestHttpServer.Response.redirect(302, bo + "/landing"));
            b.routeFixed("/landing", TestHttpServer.Response.ok("text/plain", "landed").header("Access-Control-Allow-Origin", ao)
                    .header("Access-Control-Allow-Credentials", "true"));
            a.routeFixed("/page", TestHttpServer.Response.html(page(
                    "function done(k, p) { p.then(function (v) { put(k, v); }, function (e) { put(k, 'ERR:' + e.name); }); }"
                            + "var chain = fetch('/plain', {credentials: 'omit'}).then(function () { return fetch('/plain'); })"
                            + ".then(function () { return fetch('/plain', {credentials: 'include'}); })"
                            + ".then(function () { return fetch('/to-b'); }).then(function (r) { return r.text(); }).then(function (t) { put('same', t); }, "
                            + "function (e) { put('same', 'ERR:' + e.name); })"
                            + ".then(function () { return fetch('/to-b', {credentials: 'include'}); }).then(function (r) { return r.text().then(function (t) { return t + ':' + r.redirected; }); })"
                            + ".then(function (t) { put('include', t); }, function (e) { put('include', 'ERR:' + e.name); });")).header("Set-Cookie", "sid=1; Path=/"));
            panel.setPage(a.url("/page"));
            await(panel, "same");
            await(panel, "include");
            java.util.List<TestHttpServer.Request> plain = new java.util.ArrayList<TestHttpServer.Request>();
            // /plain 으로 온 세 요청(omit, 기본, include)을 순서대로 모은다.
            for (TestHttpServer.Request r : a.requests()) {
                if ("/plain".equals(r.path)) {
                    plain.add(r);
                }
            }
            Check.equal(3, plain.size(), "three requests to /plain");
            Check.isNull(plain.get(0).header("Cookie"), "omit sends no cookie");
            Check.equal("sid=1", plain.get(1).header("Cookie"), "the default sends the cookie to the page's origin");
            Check.equal("sid=1", plain.get(2).header("Cookie"), "include sends it too");
            Check.equal("landed", value(panel, "same"), "same-origin credentials mode: the redirect target allows the page's origin, without cookies");
            Check.equal("landed:true", value(panel, "include"), "include follows the redirect with the cookie and the permission");
            // b 로 온 두 요청 중 첫째(same-origin 모드)는 쿠키가 없어야 하고 둘째(include)는 있어야 한다.
            java.util.List<TestHttpServer.Request> landings = b.requests();
            Check.equal(2, landings.size(), "two requests reached the other origin");
            Check.isNull(landings.get(0).header("Cookie"), "a redirect to another origin does not get the same-origin cookie");
            Check.equal("sid=1", landings.get(1).header("Cookie"), "include sends the cookie to the other origin");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }

    /**
     * Only http and https may be requested: ftp, file, data, javascript, about, blob and the class path (from a web page) fail without
     * contacting anything.
     * <p>
     * http 와 https 만 요청할 수 있다. ftp, file, data, javascript, about, blob, (웹 페이지에서의) 클래스패스는 아무 곳에도 접속하지 않고 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOtherSchemesAreRefused() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.start();
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "var urls = ['ftp://localhost:1/x', 'file:///etc/hosts', 'data:text/plain,hi', 'javascript:1', 'about:blank', 'blob:abc', "
                            + "'classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html', 'ws://localhost/x', 'mailto:a@b.c'];"
                            + "var left = urls.length * 2, errors = 0;"
                            + "function fin(ok) { if (!ok) { errors++; } if (--left === 0) { put('schemes', errors + '/' + urls.length * 2); } }"
                            + "urls.forEach(function (u) { fetch(u).then(function () { fin(false); }, function (e) { fin(e instanceof TypeError); });"
                            + "var x = new XMLHttpRequest(); try { x.open('GET', u); } catch (e) { fin(false); return; }"
                            + "x.onload = function () { fin(false); }; x.onerror = function () { fin(true); }; x.send(); });")));
            panel.setPage(server.url("/page"));
            Check.equal("0/18", await(panel, "schemes"), "no request with another scheme succeeded (0 unexpected outcomes of 18)");
            Check.equal(0, server.totalRequests() - 1, "nothing else was requested from the server");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page that is replaced cancels its running requests: a late response never runs script of the old page, and the timers of XHR
     * (the timeout) stop.
     * <p>
     * 교체된 페이지는 실행 중인 요청을 취소한다: 늦은 응답은 옛 페이지의 스크립트를 실행하지 않고 XHR 의 타이머(제한 시간)도 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplacedPageCancelsPendingRequests() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(1500));
            server.routeFixed("/old", TestHttpServer.Response.html(page(
                    "fetch('/slow').then(function () { put('late-fetch', '1'); }, function () { put('late-fetch', 'rejected'); });"
                            + "var x = new XMLHttpRequest(); x.open('GET', '/slow'); x.onload = function () { put('late-xhr', '1'); };"
                            + "x.onerror = function () { put('late-xhr', 'error'); }; x.send(); put('started', '1');")));
            server.routeFixed("/new", TestHttpServer.Response.html(page("put('second', '1');")));
            server.start();
            panel.setPage(server.url("/old"));
            await(panel, "started");
            long end = System.currentTimeMillis() + 30000;
            // 두 요청이 모두 서버에 도착할 때까지 기다린다.
            while (server.count("/slow") < 2 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(2, server.count("/slow"), "both requests reached the server");
            panel.setPage(server.url("/new"));
            await(panel, "second");
            Thread.sleep(2500);
            Check.isNull(value(panel, "late-fetch"), "the late fetch of the old page never ran script");
            Check.isNull(value(panel, "late-xhr"), "the late XHR of the old page never ran script");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Statuses, redirects and timeouts through real HTTP: a 404 is a normal response, a redirect is followed (or refused by
     * <code>redirect: "error"</code>), <code>responseURL</code> is the final URL, HEAD and gzip work, and the timeout fires.
     * <p>
     * 실제 HTTP 를 통한 상태, 리디렉션, 제한 시간: 404 는 일반 응답이고, 리디렉션은 따라가며(<code>redirect: "error"</code> 는 거부), <code>responseURL</code> 은
     * 최종 URL 이고, HEAD 와 gzip 이 동작하며, 제한 시간이 작동한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStatusRedirectHeadGzipAndTimeout() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/missing", new TestHttpServer.Response(404).text("text/plain", "not here"));
            server.routeFixed("/hop", TestHttpServer.Response.redirect(302, "/final"));
            server.routeFixed("/final", TestHttpServer.Response.ok("text/plain", "final").header("X-Final", "1"));
            server.routeFixed("/zip", TestHttpServer.Response.ok("text/plain; charset=utf-8", "zipped 한글").gzip());
            server.routeFixed("/latin", TestHttpServer.Response.ok("text/plain; charset=utf-8", "café"));
            server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "slow").delay(3000));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "function done(k, p) { p.then(function (v) { put(k, v); }, function (e) { put(k, 'ERR:' + e.name); }); }"
                            + "done('missing', fetch('/missing').then(function (r) { return r.status + ':' + r.ok + ':' + r.statusText.length; }));"
                            + "done('hop', fetch('/hop').then(function (r) { return r.redirected + ':' + r.url.replace(/^.*\\//, '') + ':' + r.headers.get('x-final'); }));"
                            + "done('noredirect', fetch('/hop', {redirect: 'error'}).then(function () { return 'resolved'; }));"
                            + "done('zip', fetch('/zip').then(function (r) { return r.text(); }));"
                            + "var h = new XMLHttpRequest(); h.open('HEAD', '/final'); h.onload = function () { put('head', h.status + ':' + h.responseText.length + ':' "
                            + "+ h.getResponseHeader('X-Final')); }; h.send();"
                            + "var u = new XMLHttpRequest(); u.open('GET', '/hop'); u.onload = function () { put('xurl', u.responseURL.replace(/^.*\\//, '')); }; u.send();"
                            + "var t = new XMLHttpRequest(); t.open('GET', '/slow'); t.timeout = 300; var t0 = Date.now();"
                            + "t.ontimeout = function () { put('timeout', (Date.now() - t0 < 2500) + ':' + t.readyState + ':' + t.status); }; t.onload = function () { put('timeout', 'loaded'); };"
                            + "t.send();"
                            + "var w = new XMLHttpRequest(); w.open('GET', '/latin'); w.onload = function () { put('utf8', w.responseText.length + ':' + w.responseText); }; w.send();")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("404:false:", await(panel, "missing").substring(0, 10), "a 404 is a normal response");
            Check.equal("true:final:1", await(panel, "hop"), "the redirect was followed");
            Check.equal("ERR:TypeError", await(panel, "noredirect"), "redirect: error refuses the redirect");
            Check.equal("zipped 한글", await(panel, "zip"), "gzip and UTF-8 text");
            Check.equal("200:0:1", await(panel, "head"), "HEAD has no body but has headers");
            Check.equal("final", await(panel, "xurl"), "responseURL is the final URL");
            Check.equal("true:4:0", await(panel, "timeout"), "the timeout fires long before the slow answer");
            Check.equal("4:café", await(panel, "utf8"), "UTF-8 decoding");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Methods and bodies that the request layer cannot send fail visibly instead of changing the request silently (PATCH goes over the socket connection).
     * <p>
     * 요청 계층이 보낼 수 없는 메서드와 본문은 요청을 조용히 바꾸지 않고 눈에 보이게 실패한다(PATCH 는 소켓 연결로 보낸다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsendableMethodsFailVisibly() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/any", TestHttpServer.Response.ok("text/plain", "ok"));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "function done(k, p) { p.then(function (v) { put(k, v); }, function (e) { put(k, 'ERR:' + e.name); }); }"
                            + "done('patch', fetch('/any', {method: 'PATCH', body: 'x'}).then(function (r) { return r.status + ''; }));"
                            + "done('delbody', fetch('/any', {method: 'DELETE', body: 'x'}).then(function (r) { return 'sent'; }));"
                            + "done('put', fetch('/any', {method: 'PUT', body: 'x'}).then(function (r) { return r.status + ''; }));"
                            + "done('delete', fetch('/any', {method: 'DELETE'}).then(function (r) { return r.status + ''; }));"
                            + "done('options', fetch('/any', {method: 'OPTIONS'}).then(function (r) { return r.status + ''; }));")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("200", await(panel, "patch"), "PATCH with a body is sent");
            Check.equal("ERR:TypeError", await(panel, "delbody"), "a DELETE body cannot be sent");
            Check.equal("200", await(panel, "put"), "PUT with a body");
            Check.equal("200", await(panel, "delete"), "DELETE");
            Check.equal("200", await(panel, "options"), "OPTIONS");
            Check.equal(0, server.count("/any") - 4, "exactly the four sendable requests reached the server");
            boolean sawPatch = false;
            // 서버가 받은 요청 중 메서드가 PATCH 인 것이 있는지 확인한다.
            for (TestHttpServer.Request r : server.requests()) {
                sawPatch = sawPatch || "PATCH".equals(r.method);
            }
            Check.isTrue(sawPatch, "the server saw the method PATCH");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page from the class path requests class path resources through the same API; requests from it to a web server go through CORS
     * (and fail here because the page's origin is not a web origin).
     * <p>
     * 클래스패스의 페이지는 같은 API로 클래스패스 리소스를 요청한다. 거기서 웹 서버로의 요청은 CORS 를 거친다(페이지의 오리진이 웹 오리진이 아니므로 여기서는
     * 실패한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClassPathPageRequestsClassPathResources() throws Exception {
        SwingWebPanel panel = newPanel();
        try {
            panel.setPage("classpath://org/duckdns/hjow/swingwebpanel/webapi/page.html");
            Check.equal("200:from classpath", await(panel, "cpx"), "XHR from the class path");
            Check.equal("from classpath", await(panel, "cpf"), "fetch from the class path");
            Check.equal("from classpath:true:true", await(panel, "cps"), "a class path response can be consumed as byte chunks");
            Check.equal("ERR", await(panel, "cpm"), "a missing class path resource is a network error, there is no HTTP status");
            Check.equal("ERR:TypeError", await(panel, "cph"), "a web request from a class path page needs CORS and is refused here");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Receives headers and the first byte chunk while the test server is still waiting to send the rest.
     * <p>
     * 시험 서버가 나머지를 보내기 전에 응답 머리와 첫 바이트 청크를 받는다.
     *
     * @throws Exception if the server or the page fails / 서버나 페이지가 실패하면
     */
    public void testFetchStreamBeforeResponseEnd() throws Exception {
        final CountDownLatch sendFirst = new CountDownLatch(1);
        final CountDownLatch sendRest = new CountDownLatch(1);
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/body", new TestHttpServer.Response(200).header("Content-Type", "text/plain").streamedBy(new TestHttpServer.Streamer() {
                /** Sends each chunk only after the test observes the previous result. / 시험이 앞 결과를 관찰한 뒤에 청크를 보낸다. */
                public void produce(TestHttpServer.Sink sink) throws Exception {
                    if (!sendFirst.await(15, TimeUnit.SECONDS)) {
                        throw new IOException("the page did not observe the response head");
                    }
                    sink.write("first");
                    if (!sendRest.await(15, TimeUnit.SECONDS)) {
                        throw new IOException("the page did not observe the first chunk");
                    }
                    sink.write("-last");
                }
            }));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "(async()=>{const r=await fetch('/body');put('head',r.status+':'+r.bodyUsed);"
                            + "const copy=r.clone();const copyText=copy.text().then(t=>put('copy',t));"
                            + "const reader=r.body.getReader();const decode=new TextDecoder();"
                            + "const first=await reader.read();put('first',decode.decode(first.value));first.value[0]=88;"
                            + "const last=await reader.read();put('rest',decode.decode(last.value)+':'+(await reader.read()).done);"
                            + "await copyText;reader.releaseLock();})().catch(e=>put('streamFailure',e.name+':'+e.message));")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("200:false", await(panel, "head"), "the Fetch response arrives before its body");
            sendFirst.countDown();
            Check.equal("first", await(panel, "first"), "the first chunk arrives while the connection is still open");
            sendRest.countDown();
            Check.equal("-last:true", await(panel, "rest"), "second chunk then end");
            Check.equal("first-last", await(panel, "copy"), "the clone reads its own bytes without the mutation");
            Check.isNull(value(panel, "streamFailure"), "no streaming error");
        } finally {
            sendFirst.countDown();
            sendRest.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Checks read cancellation, signal abort after the headers and an incomplete HTTP body.
     * <p>
     * 읽기 취소, 헤더 도착 뒤 신호 중단 및 불완전한 HTTP 본문을 검사한다.
     *
     * @throws Exception if the server or the page fails / 서버나 페이지가 실패하면
     */
    public void testFetchStreamAbortAndTransportFailure() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch serverWaitExpired = new CountDownLatch(1);
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/open", new TestHttpServer.Response(200).streamedBy(new TestHttpServer.Streamer() {
                /** Leaves the response open after the first chunk. / 첫 청크를 보낸 뒤 응답을 열어 둔다. */
                public void produce(TestHttpServer.Sink sink) throws Exception {
                    sink.write("a");
                    if (!release.await(15, TimeUnit.SECONDS)) {
                        serverWaitExpired.countDown();
                    }
                }
            }));
            server.routeFixed("/truncated", TestHttpServer.Response.ok("text/plain", "too short").truncateAt(2));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "(async()=>{const c=new AbortController(), r=await fetch('/open',{signal:c.signal});"
                            + "const reader=r.body.getReader();await reader.read();const pending=reader.read();c.abort('stopped');"
                            + "try{await pending;}catch(e){put('abort',e);} })();"
                            + "(async()=>{const r=await fetch('/open'),reader=r.body.getReader();await reader.read();"
                            + "const pending=reader.read();await reader.cancel('stop');put('cancel',(await pending).done+':'+r.bodyUsed);})();"
                            + "fetch('/truncated').then(r=>{put('truncated-head',r.status);return r.text()})"
                            + ".catch(e=>put('truncated',e.name));")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("stopped", await(panel, "abort"), "signal reason after the initial response");
            Check.equal("true:true", await(panel, "cancel"), "reader cancellation closes the pending read before the server finishes");
            Check.equal(Long.valueOf(1L), Long.valueOf(serverWaitExpired.getCount()), "cancellation does not wait for the connection's next chunk");
            Check.equal("200", await(panel, "truncated-head"), "a body error follows the successful headers");
            Check.equal("TypeError", await(panel, "truncated"), "a truncated body rejects full consumption");
        } finally {
            release.countDown();
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Pages created with setContent (an opaque origin) cannot request the class path or other origins.
     * <p>
     * setContent 로 만든 페이지(불투명 오리진)는 클래스패스나 다른 오리진을 요청할 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSetContentPageCannotReachTheClassPath() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/resolved", TestHttpServer.Response.ok("text/plain", "x"));
            server.routeFixed("/rejected", TestHttpServer.Response.ok("text/plain", "x"));
            server.start();
            // 불투명 오리진의 페이지는 sessionStorage 를 쓸 수 없으므로 결과를 서버로 보고한다(no-cors 요청은 응답을 읽지 못해도 서버에 도달한다).
            panel.setContent("<!doctype html><script>fetch('classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html').then(function () { return "
                    + "fetch('" + server.url("/resolved") + "', {mode: 'no-cors'}); }, function () { return fetch('" + server.url("/rejected")
                    + "', {mode: 'no-cors'}); });</" + "script>", "text/html");
            long end = System.currentTimeMillis() + 30000;
            // 보고 요청이 서버에 도착할 때까지 기다린다.
            while (server.totalRequests() < 1 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal(1, server.count("/rejected"), "a page without an origin cannot read the class path");
            Check.equal(0, server.count("/resolved"), "and it did not succeed");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page that is disposed while requests run does not leak threads of work into the dead page: nothing is reported afterwards.
     * <p>
     * 요청이 실행 중일 때 폐기된 페이지는 죽은 페이지로 작업을 흘리지 않는다: 그 뒤에는 아무것도 보고되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposeWhileRequestsRun() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/slow", TestHttpServer.Response.ok("text/plain", "late").delay(1000));
            server.routeFixed("/page", TestHttpServer.Response.html(page(
                    "fetch('/slow').then(function () { put('after-dispose', '1'); }); put('started', '1');")));
            server.start();
            panel.setPage(server.url("/page"));
            await(panel, "started");
            long end = System.currentTimeMillis() + 30000;
            // 요청이 서버에 도착할 때까지 기다린 뒤 컴포넌트를 폐기한다.
            while (server.count("/slow") < 1 && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            panel.dispose();
            Thread.sleep(1800);
            Check.isNull(value(panel, "after-dispose"), "a disposed component runs no script");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The Referer follows the referrer and the referrer policy of the request; the default policy sends the whole URL to the same
     * origin and only the origin to another one.
     * <p>
     * Referer 는 요청의 referrer 와 referrerPolicy 를 따른다. 기본 정책은 같은 오리진에는 전체 URL을, 다른 오리진에는 오리진만 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReferrerPolicy() throws Exception {
        TestHttpServer a = new TestHttpServer();
        TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            b.start();
            a.start();
            String bo = origin(b);
            b.routeFixed("/r5", TestHttpServer.Response.ok("text/plain", "x").header("Access-Control-Allow-Origin", "*"));
            a.routeFixed("/dir/page", TestHttpServer.Response.html(page(
                    "function s(p) { return p.then(function () {}, function () {}); }"
                            + "Promise.all([s(fetch('/r1')), s(fetch('/r2', {referrer: ''})), s(fetch('/r3', {referrerPolicy: 'no-referrer'})), "
                            + "s(fetch('/r4', {referrerPolicy: 'origin'})), s(fetch('" + bo + "/r5')), s(fetch('/r6', {referrer: '/dir/x.html'})), "
                            + "s(fetch('/r7', {referrerPolicy: 'same-origin'}))]).then(function () { put('refs', 'done'); });")));
            panel.setPage(a.url("/dir/page"));
            await(panel, "refs");
            Check.equal(a.url("/dir/page"), a.last("/r1").header("Referer"), "default policy, same origin: the whole URL");
            Check.isNull(a.last("/r2").header("Referer"), "referrer '' sends none");
            Check.isNull(a.last("/r3").header("Referer"), "no-referrer sends none");
            Check.equal(origin(a) + "/", a.last("/r4").header("Referer"), "origin policy");
            Check.equal(origin(a) + "/", b.last("/r5").header("Referer"), "default policy, cross origin: the origin only");
            Check.equal(origin(a) + "/dir/x.html", a.last("/r6").header("Referer"), "an explicit same-origin referrer");
            Check.equal(a.url("/dir/page"), a.last("/r7").header("Referer"), "same-origin policy to the same origin");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }

    /**
     * The cache modes of fetch go through the HTTP cache of the component: a fresh answer is reused, <code>reload</code> asks the server
     * again, <code>force-cache</code> prefers the stored answer.
     * <p>
     * fetch 의 캐시 모드는 컴포넌트의 HTTP 캐시를 거친다: 신선한 응답은 재사용하고, <code>reload</code> 는 서버에 다시 묻고, <code>force-cache</code> 는 저장된
     * 응답을 우선한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCacheModesUseTheHttpCache() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/c", TestHttpServer.Response.ok("text/plain", "cached").header("Cache-Control", "max-age=600"));
            // 한 페이지가 한 단계만 실행하고 끝나면 보고하므로 서버 쪽 횟수를 경쟁 없이 확인할 수 있다. 컴포넌트의 HTTP 캐시는 페이지 사이에 유지된다.
            server.routeFixed("/p1", TestHttpServer.Response.html(page("fetch('/c').then(function () { return fetch('/c'); }).then(function () { put('d1', '1'); });")));
            server.routeFixed("/p2", TestHttpServer.Response.html(page("fetch('/c', {cache: 'reload'}).then(function () { put('d2', '1'); });")));
            server.routeFixed("/p3", TestHttpServer.Response.html(page("fetch('/c', {cache: 'force-cache'}).then(function () { put('d3', '1'); });")));
            server.routeFixed("/p4", TestHttpServer.Response.html(page("fetch('/c', {cache: 'no-store'}).then(function () { put('d4', '1'); });")));
            server.start();
            panel.setPage(server.url("/p1"));
            await(panel, "d1");
            Check.equal(1, server.count("/c"), "the second fetch was answered by the cache");
            panel.setPage(server.url("/p2"));
            await(panel, "d2");
            Check.equal(2, server.count("/c"), "reload asks the server again");
            panel.setPage(server.url("/p3"));
            await(panel, "d3");
            Check.equal(2, server.count("/c"), "force-cache uses the stored answer");
            panel.setPage(server.url("/p4"));
            await(panel, "d4");
            Check.equal(3, server.count("/c"), "no-store goes to the server");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The protected SSL hook of the component is called for the https requests of scripts as well, so a host that installs trust there
     * (or its own socket factory) governs XMLHttpRequest and fetch like every other request.
     * <p>
     * 컴포넌트의 protected SSL 훅은 스크립트의 https 요청에도 호출되므로 거기에 신뢰(또는 자체 소켓 팩터리)를 설치하는 호스트가 XMLHttpRequest 와 fetch 도 다른
     * 모든 요청처럼 다스린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSslHookIsCalledForScriptRequests() throws Exception {
        TestHttpServer s = new TestHttpServer(TestSsl.serverContext());
        final int[] calls = new int[1];
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        try {
            s.routeFixed("/data", TestHttpServer.Response.ok("text/plain", "secure"));
            s.routeFixed("/page", TestHttpServer.Response.html(page(
                    "put('page-calls', 'loaded');"
                            + "var x = new XMLHttpRequest(); x.open('GET', '/data?x'); x.onload = function () { put('xs', x.responseText); }; x.send();"
                            + "fetch('/data?f').then(function (r) { return r.text(); }).then(function (t) { put('fs', t); });")));
            s.start();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    holder[0] = new SwingWebPanel() {
                        private static final long serialVersionUID = 1L;

                        protected void configureSsl(HttpsURLConnection c) throws IOException {
                            synchronized (calls) {
                                calls[0]++;
                            }
                            try {
                                c.setSSLSocketFactory(TestSsl.clientContext().getSocketFactory());
                            } catch (Exception e) {
                                throw new IOException(e.toString());
                            }
                        }
                    };
                    holder[0].setSize(300, 180);
                }
            });
            holder[0].setPage("https://localhost:" + s.getPort() + "/page");
            Check.equal("secure", await(holder[0], "xs"), "XHR over https through the hook");
            Check.equal("secure", await(holder[0], "fs"), "fetch over https through the hook");
            synchronized (calls) {
                Check.isTrue(calls[0] >= 3, "the hook was called for the page, the XHR and the fetch: " + calls[0]);
            }
        } finally {
            if (holder[0] != null) {
                holder[0].dispose();
            }
            s.stop();
        }
    }

    /**
     * The additional headers of the component are not written by the page, so they do not make a cross-origin request non-simple (no
     * preflight is needed for them) and they are still sent.
     * <p>
     * 컴포넌트의 추가 헤더는 페이지가 쓴 것이 아니므로 교차 오리진 요청을 단순하지 않게 만들지 않고(그것 때문에 사전 요청이 필요하지 않다) 그대로 전송된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testComponentHeadersDoNotMakeRequestsNonSimple() throws Exception {
        TestHttpServer a = new TestHttpServer();
        TestHttpServer b = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            b.start();
            a.start();
            b.routeFixed("/open", TestHttpServer.Response.ok("text/plain", "open").header("Access-Control-Allow-Origin", "*"));
            a.routeFixed("/page", TestHttpServer.Response.html(page(
                    "fetch('" + origin(b) + "/open').then(function (r) { return r.text(); }).then(function (t) { put('res', t); }, "
                            + "function (e) { put('res', 'ERR:' + e.name); });")));
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("X-Component", "c");
            panel.addHeaders(extra);
            panel.setPage(a.url("/page"));
            Check.equal("open", await(panel, "res"), "a simple request with a component header is not refused");
            Check.equal("c", b.last("/open").header("X-Component"), "the component header was sent");
        } finally {
            panel.dispose();
            a.stop();
            b.stop();
        }
    }
}
