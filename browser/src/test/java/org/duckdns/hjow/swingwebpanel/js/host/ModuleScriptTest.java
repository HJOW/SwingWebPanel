package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the browser rules for module scripts (plan item P15-04-c) without a network: every inline module is a module of its own,
 * <code>nomodule</code> scripts are skipped, a module that appears in several places runs once, cyclic graphs run each module once in the
 * order of the dependencies, relative imports resolve against the URL that finally served the importing module, and a graph that cannot
 * be fetched or parsed fires <code>error</code> at the script element while an error thrown during evaluation still fires
 * <code>load</code>. A fake source that knows the module rules stands in for the component's request layer.
 * <p>
 * 네트워크 없이 모듈 스크립트의 브라우저 규칙을 시험한다(계획 P15-04-c): 인라인 모듈은 각각 별개의 모듈이고, <code>nomodule</code> 스크립트는
 * 건너뛰며, 여러 곳에 나타난 모듈은 한 번만 실행되고, 순환 그래프는 의존 순서로 모듈마다 한 번 실행되며, 상대 import 는 가져오는 모듈을 최종적으로
 * 제공한 URL을 기준으로 해석되고, 가져오거나 파싱할 수 없는 그래프는 script 요소에 <code>error</code> 를 보내며 평가 중 던진 오류는 여전히
 * <code>load</code> 를 보낸다. 모듈 규칙을 아는 가짜 공급원이 컴포넌트의 요청 계층을 대신한다.
 */
public class ModuleScriptTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ModuleScriptTest() {
    }

    /**
     * A source of module texts with redirects: a request URL may be served from another URL, and every request is counted.
     * <p>
     * 리디렉션이 있는 모듈 텍스트 공급원: 요청 URL을 다른 URL이 제공할 수 있고 모든 요청을 센다.
     */
    private static final class FakeModules implements ModuleScriptSource {
        /**
         * The texts by serving URL.
         * <p>
         * 제공 URL별 텍스트.
         */
        final Map<String, String> texts = new HashMap<String, String>();

        /**
         * The redirects: requested URL to serving URL.
         * <p>
         * 리디렉션: 요청 URL에서 제공 URL로.
         */
        final Map<String, String> redirects = new HashMap<String, String>();

        /**
         * The number of module requests by requested URL.
         * <p>
         * 요청 URL별 모듈 요청 수.
         */
        final Map<String, Integer> requests = new HashMap<String, Integer>();

        /**
         * Counts a request and returns the text, or fails with 404.
         * <p>
         * 요청을 세고 텍스트를 반환하거나 404 로 실패한다.
         *
         * @param url the requested URL / 요청 URL
         * @return the text and the serving URL / 텍스트와 제공 URL
         * @throws IOException if unknown / 모르는 URL이면
         */
        synchronized ScriptText serve(String url) throws IOException {
            Integer n = requests.get(url);
            requests.put(url, n == null ? 1 : n + 1);
            String serving = redirects.containsKey(url) ? redirects.get(url) : url;
            String text = texts.get(serving);
            if (text == null) {
                throw new IOException("404 " + serving);
            }
            return new ScriptText(text, serving);
        }

        /**
         * Fetches a classic script.
         * <p>
         * 일반 스크립트를 가져온다.
         *
         * @param url the URL / URL
         * @return the text / 텍스트
         * @throws IOException if unknown / 모르는 URL이면
         */
        public String fetch(String url) throws IOException {
            return serve(url).getText();
        }

        /**
         * Fetches a module.
         * <p>
         * 모듈을 가져온다.
         *
         * @param url            the URL / URL
         * @param useCredentials ignored / 무시
         * @return the text and the final URL / 텍스트와 최종 URL
         * @throws IOException if unknown / 모르는 URL이면
         */
        public ScriptText fetchModule(String url, boolean useCredentials) throws IOException {
            return serve(url);
        }

        /**
         * Returns the number of requests of a URL.
         * <p>
         * URL의 요청 수를 반환한다.
         *
         * @param url the requested URL / 요청 URL
         * @return the count / 개수
         */
        synchronized int count(String url) {
            Integer n = requests.get(url);
            return n == null ? 0 : n;
        }
    }

    /**
     * Loads a page on the calling thread with the fake modules (no scheduler: the page waits on the calling thread).
     * <p>
     * 호출한 스레드에서 가짜 모듈로 페이지를 로드한다(스케줄러 없음: 페이지가 호출한 스레드에서 기다린다).
     *
     * @param modules the module source / 모듈 공급원
     * @param html    the page / 페이지
     * @param log     the receiver of the console lines / 콘솔 줄을 받는 곳
     * @return the scripts of the page / 페이지의 스크립트
     */
    private static PageScripts load(final FakeModules modules, String html, final StringBuilder log) {
        Document document = Document.createHtmlDocument();
        document.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(document);
        document.setUrl("http://example.test/dir/page.html");
        HostEnvironment env = new HostEnvironment() {
            /**
             * Returns the module-aware source.
             * <p>
             * 모듈을 아는 공급원을 반환한다.
             *
             * @return the source / 공급원
             */
            public ScriptSource getScriptSource() {
                return modules;
            }
        };
        PageScripts scripts = new PageScripts(document, env);
        scripts.getRealm().setConsole(new ConsoleHook() {
            /**
             * Records the lines of level log and error.
             * <p>
             * log 와 error 수준의 줄을 기록한다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                if (log.length() > 0) {
                    log.append(',');
                }
                log.append("error".equals(level) ? "!" : "").append(text);
            }
        });
        HtmlParser parser = new HtmlParser(document, html);
        parser.setScripting(true);
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
        return scripts;
    }

    /**
     * P15-04-c: two inline modules are two modules; a classic script with <code>nomodule</code> is skipped; imports of an inline module
     * resolve against the document base URL, and a module that both import is fetched and evaluated once.
     * <p>
     * P15-04-c: 인라인 모듈 둘은 모듈 둘이고, <code>nomodule</code> 일반 스크립트는 건너뛰며, 인라인 모듈의 import 는 문서 기준 URL을 기준으로
     * 해석되고, 둘 다 import 한 모듈은 한 번만 가져와 평가된다.
     */
    public void testInlineModulesNomoduleAndSharedDependency() {
        FakeModules m = new FakeModules();
        m.texts.put("http://example.test/base/shared.js", "console.log('shared');");
        StringBuilder log = new StringBuilder();
        load(m, "<base href='http://example.test/base/'><script type=module>import './shared.js'; console.log('first');</script>"
                + "<script type=module>import './shared.js'; console.log('second');</script>"
                + "<script nomodule>console.log('nomodule ran')</script><script>console.log('classic')</script>", log);
        Check.equal("classic,shared,first,second", log.toString(), "both inline modules ran after the classic script, shared ran once");
        Check.equal(1, m.count("http://example.test/base/shared.js"), "the shared module was fetched once");
    }

    /**
     * P15-04-c: the same module requested by two scripts runs once but both script elements get load; a cycle runs every module once in
     * dependency order.
     * <p>
     * P15-04-c: 두 스크립트가 요청한 같은 모듈은 한 번 실행되지만 두 script 요소 모두 load 를 받고, 순환은 모든 모듈을 의존 순서로 한 번씩 실행한다.
     */
    public void testDuplicateModulesAndCycles() {
        FakeModules m = new FakeModules();
        m.texts.put("http://example.test/dir/once.js", "console.log('once');");
        m.texts.put("http://example.test/dir/a.js", "import './b.js'; console.log('a');");
        m.texts.put("http://example.test/dir/b.js", "import './a.js'; console.log('b');");
        StringBuilder log = new StringBuilder();
        load(m, "<script type=module src=once.js onload=\"console.log('load1')\"></script>"
                + "<script type=module src=once.js onload=\"console.log('load2')\"></script>"
                + "<script type=module src=a.js></script>", log);
        Check.equal("once,load1,load2,b,a", log.toString(), "once ran once, the cycle ran b before a");
        Check.equal(1, m.count("http://example.test/dir/once.js"), "the module map made the second script element share the fetch");
    }

    /**
     * P15-04-c: relative imports resolve against the final URL after a redirect, not against the requested URL.
     * <p>
     * P15-04-c: 상대 import 는 요청한 URL이 아니라 리디렉션 뒤의 최종 URL을 기준으로 해석된다.
     */
    public void testImportsResolveAgainstTheFinalUrl() {
        FakeModules m = new FakeModules();
        m.redirects.put("http://example.test/dir/entry.js", "http://example.test/other/real.js");
        m.texts.put("http://example.test/other/real.js", "import './dep.js'; console.log('entry');");
        m.texts.put("http://example.test/other/dep.js", "console.log('dep from other');");
        m.texts.put("http://example.test/dir/dep.js", "console.log('dep from dir');");
        StringBuilder log = new StringBuilder();
        load(m, "<script type=module src=entry.js></script>", log);
        Check.equal("dep from other,entry", log.toString(), "the dependency came from the redirected location");
    }

    /**
     * P15-04-c: a graph that cannot be fetched or parsed fires error and runs nothing; a module that throws while it runs still fires load.
     * <p>
     * P15-04-c: 가져오거나 파싱할 수 없는 그래프는 error 를 보내고 아무것도 실행하지 않으며, 실행 중 예외를 던진 모듈은 그래도 load 를 보낸다.
     */
    public void testGraphFailureFiresErrorAndEvaluationErrorFiresLoad() {
        FakeModules m = new FakeModules();
        m.texts.put("http://example.test/dir/missing-dep.js", "import './nope.js'; console.log('must not run');");
        m.texts.put("http://example.test/dir/syntax.js", "export var = ;");
        m.texts.put("http://example.test/dir/throws.js", "throw new Error('boom');");
        StringBuilder log = new StringBuilder();
        load(m, "<script type=module src=missing-dep.js onload=\"console.log('L1')\" onerror=\"console.log('E1')\"></script>"
                + "<script type=module src=syntax.js onload=\"console.log('L2')\" onerror=\"console.log('E2')\"></script>"
                + "<script type=module src=throws.js onload=\"console.log('L3')\" onerror=\"console.log('E3')\"></script>"
                + "<script type=module src=gone.js onload=\"console.log('L4')\" onerror=\"console.log('E4')\"></script>", log);
        String text = "," + log + ",";
        Check.isTrue(text.indexOf(",E1,") >= 0 && text.indexOf(",L1,") < 0, "missing dependency fires error: " + log);
        Check.isTrue(text.indexOf(",E2,") >= 0 && text.indexOf(",L2,") < 0, "syntax error fires error: " + log);
        Check.isTrue(text.indexOf(",L3,") >= 0 && text.indexOf(",E3,") < 0, "evaluation error still fires load: " + log);
        Check.isTrue(text.indexOf(",E4,") >= 0 && text.indexOf(",L4,") < 0, "missing module fires error: " + log);
        Check.isTrue(text.indexOf("must not run") < 0, "nothing of a failed graph ran: " + log);
    }

    /**
     * P15-04-c: a dynamically inserted module script is fetched and evaluated like a parser-inserted one.
     * <p>
     * P15-04-c: 동적으로 삽입한 모듈 스크립트도 파서가 삽입한 것과 같이 가져와 평가된다.
     */
    public void testDynamicModuleScript() {
        FakeModules m = new FakeModules();
        m.texts.put("http://example.test/dir/dyn.js", "console.log('dynamic module');");
        StringBuilder log = new StringBuilder();
        load(m, "<body><script>var s = document.createElement('script'); s.type = 'module'; s.src = 'dyn.js';"
                + "document.body.appendChild(s); console.log('inserted');</script></body>", log);
        Check.equal("inserted,dynamic module", log.toString(), "the module ran after the inserting script");
    }

    /**
     * P28-03-e: dynamic import and a module with top-level await resume on the page's microtask queue.
     * <p>
     * P28-03-e: 동적 import 와 top-level await 가 페이지 마이크로태스크 큐에서 재개된다.
     */
    public void testDynamicImportAndTopLevelAwait() {
        FakeModules m = new FakeModules();
        m.texts.put("http://example.test/dir/dyn.js",
                "await Promise.resolve(); console.log('module ' + import.meta.url); export default 7;");
        StringBuilder log = new StringBuilder();
        load(m, "<script>console.log('start'); import('./dyn.js').then(function(m){ console.log('value ' + m.default); });</script>"
                + "<script type=module>await 0; console.log('inline done');</script>", log);
        Check.equal("start,module http://example.test/dir/dyn.js,value 7,inline done", log.toString(),
                "dynamic and TLA modules resumed through the page event loop");
        Check.equal(1, m.count("http://example.test/dir/dyn.js"), "dynamic module fetched once");
    }
}
