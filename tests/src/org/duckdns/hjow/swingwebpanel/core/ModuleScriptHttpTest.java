package org.duckdns.hjow.swingwebpanel.core;

import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The browser rules of module scripts through the real component and real HTTP (plan item P15-04-c): JavaScript MIME type, CORS across
 * origins, cookies that follow the credentials mode, relative imports that resolve against the URL after a redirect, and modules that are
 * requested and evaluated once. A second local server on another port is the other origin.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 모듈 스크립트의 브라우저 규칙을 확인하는 시험(계획 P15-04-c): JavaScript MIME 타입, 오리진 사이의 CORS,
 * 자격 증명 모드를 따르는 쿠키, 리디렉션 뒤의 URL을 기준으로 해석되는 상대 import, 한 번만 요청되고 평가되는 모듈. 다른 포트의 두 번째 로컬 서버가
 * 다른 오리진이다.
 */
public class ModuleScriptHttpTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ModuleScriptHttpTest() {
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
     * Waits until a stored value equals the expected one.
     * <p>
     * 저장된 값이 기대한 값이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param key      the key / 키
     * @param expected the value / 기대 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitValue(SwingWebPanel panel, String key, String expected) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (!expected.equals(value(panel, key)) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(expected, value(panel, key), "stored value " + key);
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
     * Builds a script that appends a name to the stored log.
     * <p>
     * 이름을 저장된 기록에 덧붙이는 스크립트를 만든다.
     *
     * @param name the name / 이름
     * @return the script text / 스크립트 텍스트
     */
    private static String mark(String name) {
        return "sessionStorage.setItem('log',(sessionStorage.getItem('log')||'')+'" + name + ",');";
    }

    /**
     * Returns the markup of a module script element that records its load and error events.
     * <p>
     * load 와 error 이벤트를 기록하는 모듈 script 요소의 마크업을 반환한다.
     *
     * @param src   the module URL / 모듈 URL
     * @param name  the name that identifies it in the log / 기록에서 식별하는 이름
     * @param extra more attributes or "" / 더 붙일 속성 또는 ""
     * @return the markup / 마크업
     */
    private static String moduleTag(String src, String name, String extra) {
        return "<script type=module src='" + src + "' " + extra + " onload=\"" + mark("load-" + name) + "\" onerror=\"" + mark("error-" + name)
                + "\"></script>";
    }

    /**
     * P15-04-c: a module must be served with a JavaScript MIME type; a page that answers with another type is an error and does not run.
     * <p>
     * P15-04-c: 모듈은 JavaScript MIME 타입으로 제공되어야 한다. 다른 타입으로 응답한 페이지는 오류이며 실행되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testModuleMimeTypeIsEnforced() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/ok.js", TestHttpServer.Response.ok("text/javascript; charset=utf-8", mark("ok")));
            server.routeFixed("/plain.js", TestHttpServer.Response.ok("text/plain", mark("plain")));
            server.routeFixed("/html.js", TestHttpServer.Response.ok("text/html", mark("html")));
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html>" + moduleTag("ok.js", "ok", "") + moduleTag("plain.js", "plain", "")
                    + moduleTag("html.js", "html", "")));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            String log = "," + value(panel, "log");
            Check.isTrue(log.indexOf(",ok,") >= 0 && log.indexOf(",load-ok,") >= 0, "the JavaScript module ran and fired load: " + log);
            Check.isTrue(log.indexOf(",error-plain,") >= 0 && log.indexOf(",plain,") < 0, "text/plain is refused: " + log);
            Check.isTrue(log.indexOf(",error-html,") >= 0 && log.indexOf(",html,") < 0, "text/html is refused: " + log);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * P15-04-c: a module from another origin needs the server's CORS permission; without it nothing runs and error is fired.
     * <p>
     * P15-04-c: 다른 오리진의 모듈은 서버의 CORS 허용이 필요하다. 허용이 없으면 아무것도 실행되지 않고 error 가 발생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginModuleNeedsCors() throws Exception {
        TestHttpServer page = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            other.routeFixed("/denied.js", TestHttpServer.Response.ok("text/javascript", mark("denied")));
            other.routeFixed("/open.js", TestHttpServer.Response.ok("text/javascript", mark("open")).header("Access-Control-Allow-Origin", "*"));
            other.start();
            page.routeFixed("/page", TestHttpServer.Response.html("<!doctype html>" + moduleTag(other.url("/denied.js"), "denied", "")
                    + moduleTag(other.url("/open.js"), "open", "")));
            page.start();
            panel.setPage(page.url("/page"));
            awaitPage(panel, page.url("/page"));
            String log = "," + value(panel, "log");
            Check.isTrue(log.indexOf(",error-denied,") >= 0 && log.indexOf(",denied,") < 0, "a cross-origin module without CORS is blocked: " + log);
            Check.isTrue(log.indexOf(",open,") >= 0 && log.indexOf(",load-open,") >= 0, "a module that allows every origin runs: " + log);
            Check.isTrue(log.indexOf(",error-open,") < 0, "no error for the allowed module: " + log);
        } finally {
            panel.dispose();
            page.stop();
            other.stop();
        }
    }

    /**
     * P15-04-c: cookies go to the page's own origin, but only to another origin when the script asks for credentials, and then the server
     * has to allow credentials explicitly.
     * <p>
     * P15-04-c: 쿠키는 페이지 자신의 오리진에는 가고, 다른 오리진에는 스크립트가 자격 증명을 요청할 때만 가며, 그때는 서버가 자격 증명을 명시적으로
     * 허용해야 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCookiesFollowTheCredentialsMode() throws Exception {
        TestHttpServer page = new TestHttpServer();
        TestHttpServer other = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            other.start();
            page.start();
            String origin = page.url("/page").substring(0, page.url("/page").length() - "/page".length());
            page.routeFixed("/own.js", TestHttpServer.Response.ok("text/javascript", mark("own")));
            other.routeFixed("/anon.js", TestHttpServer.Response.ok("text/javascript", mark("anon")).header("Access-Control-Allow-Origin", "*"));
            other.routeFixed("/cred.js", TestHttpServer.Response.ok("text/javascript", mark("cred")).header("Access-Control-Allow-Origin", origin)
                    .header("Access-Control-Allow-Credentials", "true"));
            other.routeFixed("/cred-wild.js", TestHttpServer.Response.ok("text/javascript", mark("credwild")).header("Access-Control-Allow-Origin", "*"));
            page.routeFixed("/page", TestHttpServer.Response.html("<!doctype html>" + moduleTag("own.js", "own", "")
                    + moduleTag(other.url("/anon.js"), "anon", "") + moduleTag(other.url("/cred.js"), "cred", "crossorigin=use-credentials")
                    + moduleTag(other.url("/cred-wild.js"), "credwild", "crossorigin=use-credentials")).header("Set-Cookie", "sid=42; Path=/"));
            panel.setPage(page.url("/page"));
            awaitPage(panel, page.url("/page"));
            String log = "," + value(panel, "log");
            Check.equal("sid=42", page.last("/own.js").header("Cookie"), "the page's own origin gets the cookie");
            Check.isNull(other.last("/anon.js").header("Cookie"), "another origin gets no cookie by default");
            Check.equal("sid=42", other.last("/cred.js").header("Cookie"), "credentials were requested, so the cookie is sent");
            Check.isTrue(log.indexOf(",cred,") >= 0 && log.indexOf(",anon,") >= 0, "both allowed modules ran: " + log);
            Check.isTrue(log.indexOf(",error-credwild,") >= 0 && log.indexOf(",credwild,") < 0,
                    "a wildcard origin does not allow credentials: " + log);
        } finally {
            panel.dispose();
            page.stop();
            other.stop();
        }
    }

    /**
     * P15-04-c: the relative imports of a redirected module resolve against the URL that served it, and shared and cyclic modules are
     * requested and evaluated once.
     * <p>
     * P15-04-c: 리디렉션된 모듈의 상대 import 는 그 모듈을 제공한 URL을 기준으로 해석되고, 공유 모듈과 순환 모듈은 한 번만 요청되어 평가된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectedModuleResolvesImportsAndGraphsRunOnce() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = newPanel();
        try {
            server.routeFixed("/r/entry.js", TestHttpServer.Response.redirect(302, "/real/entry.js"));
            server.routeFixed("/real/entry.js", TestHttpServer.Response.ok("text/javascript", "import './dep.js'; import '/shared.js';" + mark("entry")));
            server.routeFixed("/real/dep.js", TestHttpServer.Response.ok("text/javascript", "import '/shared.js';" + mark("dep")));
            server.routeFixed("/r/dep.js", TestHttpServer.Response.ok("text/javascript", mark("WRONG")));
            server.routeFixed("/shared.js", TestHttpServer.Response.ok("text/javascript", mark("shared")));
            server.routeFixed("/cyc/a.js", TestHttpServer.Response.ok("text/javascript", "import './b.js';" + mark("a")));
            server.routeFixed("/cyc/b.js", TestHttpServer.Response.ok("text/javascript", "import './a.js';" + mark("b")));
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html>" + moduleTag("r/entry.js", "entry", "")
                    + moduleTag("r/entry.js", "entry2", "") + moduleTag("cyc/a.js", "a", "")));
            server.start();
            panel.setPage(server.url("/page"));
            awaitPage(panel, server.url("/page"));
            String log = value(panel, "log");
            Check.isTrue(log.startsWith("shared,dep,entry,"), "dependencies ran before the importing module: " + log);
            Check.isTrue(log.indexOf("WRONG") < 0, "the import was not resolved against the requested URL: " + log);
            Check.equal(1, server.count("/real/dep.js"), "the dependency was requested once");
            Check.equal(1, server.count("/shared.js"), "the shared module was requested once");
            Check.equal(1, server.count("/r/entry.js"), "the entry module was requested once for two script elements");
            Check.isTrue(log.indexOf("b,a,") > 0, "the cycle ran b before a: " + log);
            Check.isTrue(log.indexOf("load-entry,") >= 0 && log.indexOf("load-entry2,") >= 0, "both script elements fired load: " + log);
            Check.equal(1, countOf(log, "entry,"), "the entry module ran once");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Counts the whole-word occurrences of an item in a comma separated log (an item ends with a comma).
     * <p>
     * 쉼표로 이은 기록에서 항목이 나타난 횟수를 센다(항목은 쉼표로 끝난다).
     *
     * @param log  the log / 기록
     * @param item the item with its trailing comma / 쉼표가 붙은 항목
     * @return the count / 횟수
     */
    private static int countOf(String log, String item) {
        int n = 0;
        // 기록을 쉼표로 나눈 항목과 비교하여 센다.
        String[] parts = log.split(",");
        for (int i = 0; i < parts.length; i++) {
            if ((parts[i] + ",").equals(item)) {
                n++;
            }
        }
        return n;
    }
}
