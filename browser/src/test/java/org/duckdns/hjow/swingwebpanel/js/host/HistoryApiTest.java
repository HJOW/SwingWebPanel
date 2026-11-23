package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;

/**
 * Tests of <code>window.history</code> without the component (plan item P20-07, HTML5 2014 REC 5.5.2 and 5.6.10): the structured clone of the
 * state, the checks of the URL argument (<code>SecurityError</code>), the immediate change of <code>document.URL</code> and
 * <code>history.state</code>, the calls that the page makes to its environment, <code>history.length</code>, the traversal events
 * (<code>popstate</code>, <code>hashchange</code>) and the state that a document starts with.
 * <p>
 * 컴포넌트 없이 <code>window.history</code> 를 시험한다(계획 P20-07, HTML5 2014 REC 5.5.2, 5.6.10): 상태의 구조화 복제, URL 인수 검사
 * (<code>SecurityError</code>), <code>document.URL</code> 과 <code>history.state</code> 의 즉시 변경, 페이지가 환경에 하는 호출,
 * <code>history.length</code>, 이동 이벤트(<code>popstate</code>, <code>hashchange</code>), 문서가 가지고 시작하는 상태.
 */
public class HistoryApiTest {

    /**
     * An environment that records what the page asks the component to do with the history.
     * <p>
     * 페이지가 컴포넌트에 이력에 관해 요구하는 것을 기록하는 환경.
     */
    private static final class Recorder extends HostEnvironment {
        /**
         * The manual scheduler.
         * <p>
         * 수동 스케줄러.
         */
        final PageFixture.ManualScheduler scheduler = new PageFixture.ManualScheduler();

        /**
         * The calls as <code>push url</code> or <code>replace url</code>.
         * <p>
         * <code>push url</code> 또는 <code>replace url</code> 형태의 호출 기록.
         */
        final List<String> calls = new ArrayList<String>();

        /**
         * The states of the calls.
         * <p>
         * 호출마다의 상태.
         */
        final List<ClonedValue> states = new ArrayList<ClonedValue>();

        /**
         * The history moves that the page asked for.
         * <p>
         * 페이지가 요구한 이력 이동.
         */
        final List<Integer> moves = new ArrayList<Integer>();

        /**
         * The history length that the environment reports.
         * <p>
         * 환경이 알리는 이력 길이.
         */
        int length = 1;

        /**
         * Returns the scheduler.
         * <p>
         * 스케줄러를 반환한다.
         *
         * @return the scheduler / 스케줄러
         */
        public Scheduler getScheduler() {
            return scheduler;
        }

        /**
         * Records a push or replace.
         * <p>
         * push 나 replace 를 기록한다.
         *
         * @param state   the state / 상태
         * @param url     the URL / URL
         * @param replace true for replace / replace 이면 true
         */
        public void updateHistoryState(ClonedValue state, String url, boolean replace) {
            calls.add((replace ? "replace " : "push ") + url);
            states.add(state);
        }

        /**
         * Returns the configured length.
         * <p>
         * 설정한 길이를 반환한다.
         *
         * @return the length / 길이
         */
        public int getHistoryLength() {
            return length;
        }

        /**
         * Records a history move.
         * <p>
         * 이력 이동을 기록한다.
         *
         * @param delta the steps / 칸 수
         */
        public void historyGo(int delta) {
            moves.add(Integer.valueOf(delta));
        }
    }

    /**
     * The environment of the case.
     * <p>
     * 사례의 환경.
     */
    private Recorder env;

    /**
     * The scripts of the page of the case.
     * <p>
     * 사례 페이지의 스크립트.
     */
    private PageScripts scripts;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HistoryApiTest() {
    }

    /**
     * Loads a page at a URL with the recording environment.
     * <p>
     * 기록하는 환경으로 URL의 페이지를 로드한다.
     *
     * @param url  the document URL / 문서 URL
     * @param html the page / 페이지
     */
    private void load(String url, String html) {
        env = new Recorder();
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setUrl(url);
        scripts = new PageScripts(doc, env);
        HtmlParser parser = new HtmlParser(doc, html);
        parser.setScripting(true);
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
    }

    /**
     * Evaluates a script and returns its completion value as text.
     * <p>
     * 스크립트를 실행하고 완료 값을 문자열로 반환한다.
     *
     * @param source the script / 스크립트
     * @return the text / 문자열
     */
    private String run(String source) {
        return scripts.evaluate(source, "test");
    }

    /**
     * Returns the text of the error that a script statement throws (name, code and message start).
     * <p>
     * 스크립트 문장이 던지는 오류의 내용(이름, 코드, 메시지 시작)을 반환한다.
     *
     * @param statement the statement / 문장
     * @return <code>name:code</code>, or "no error" / <code>이름:코드</code>. 오류가 없으면 "no error"
     */
    private String errorOf(String statement) {
        return run("(function () { try { " + statement + "; return 'no error'; } catch (e) { return e.name + ':' + e.code + ':' + (e instanceof DOMException); } })()");
    }

    /**
     * pushState and replaceState change document.URL and history.state at once, clone the state and report to the environment.
     * <p>
     * pushState 와 replaceState 는 document.URL 과 history.state 를 곧바로 바꾸고, 상태를 복제하며, 환경에 알린다.
     */
    public void testPushAndReplaceChangeUrlAndState() {
        load("http://example.test/dir/page.html", "<!doctype html><p>x</p>");
        Check.equal("null:http://example.test/dir/page.html", run("String(history.state) + ':' + document.URL"), "initial state");
        run("var src = { a: [1, 2], when: new Date(5) }; history.pushState(src, 'title', 'next?x=1#f');");
        Check.equal("http://example.test/dir/next?x=1#f|http://example.test/dir/next?x=1#f|2|true|false",
                run("document.URL + '|' + location.href + '|' + history.state.a[1] + '|' + (history.state.when instanceof Date) + '|' + (history.state === src)"),
                "the URL and the state changed at once");
        Check.equal("true", run("String(history.state === history.state)"), "the same object is returned every time");
        run("src.a.push(3);");
        Check.equal("2", run("String(history.state.a.length)"), "the state is a copy");
        run("history.replaceState({ r: 1 }, '', '/other/path');");
        Check.equal("http://example.test/other/path:1", run("location.href + ':' + history.state.r"), "replaceState with an absolute path");
        Check.equal("[push http://example.test/dir/next?x=1#f, replace http://example.test/other/path]", env.calls.toString(), "calls to the environment");
        scripts.getRealm().getGlobalObject().set("replaced", StructuredClone.deserialize(scripts.getRealm(), env.states.get(1)));
        Check.equal("{\"r\":1}", run("JSON.stringify(replaced)"), "the environment got the serialized state");
    }

    /**
     * Without a URL argument (missing, undefined or null) the document URL stays; only the state changes.
     * <p>
     * URL 인수가 없으면(생략, undefined, null) 문서 URL은 그대로이고 상태만 바뀐다.
     */
    public void testStateOnlyCallsKeepTheUrl() {
        load("http://example.test/a/b.html?q=1#h", "<p>x</p>");
        run("history.pushState(1, '');");
        run("history.pushState(2, '', null);");
        run("history.replaceState(3, '', undefined);");
        Check.equal("[push http://example.test/a/b.html?q=1#h, push http://example.test/a/b.html?q=1#h, replace http://example.test/a/b.html?q=1#h]",
                env.calls.toString(), "the URL is that of the document");
        Check.equal("3:http://example.test/a/b.html?q=1#h", run("history.state + ':' + location.href"), "state changed, URL did not");
    }

    /**
     * A URL of another origin, another scheme, another port or user information, or one that cannot be parsed, is a SecurityError; nothing changes.
     * <p>
     * 다른 오리진, 스킴, 포트, 사용자 정보의 URL이나 해석할 수 없는 URL은 SecurityError 이며 아무것도 바뀌지 않는다.
     */
    public void testUrlOfAnotherOriginIsSecurityError() {
        load("http://example.test/dir/page.html", "<p>x</p>");
        String[] bad = { "http://other.test/dir/page.html", "https://example.test/dir/page.html", "http://example.test:8080/dir/page.html",
            "http://user@example.test/dir/page.html", "ftp://example.test/x", "javascript:void(0)", "http://[bad/x", "file:///c:/x" };
        // 오리진이 달라지거나 해석할 수 없는 URL마다 SecurityError(코드 18)가 나야 한다.
        for (int i = 0; i < bad.length; i++) {
            Check.equal("SecurityError:18:true", errorOf("history.pushState(1, '', '" + bad[i] + "')"), bad[i]);
            Check.equal("SecurityError:18:true", errorOf("history.replaceState(1, '', '" + bad[i] + "')"), "replace " + bad[i]);
        }
        Check.equal("[]:null:http://example.test/dir/page.html", env.calls + ":" + run("String(history.state)") + ":" + run("document.URL"),
                "nothing changed");
        Check.equal("no error", errorOf("history.pushState(1, '', 'http://example.test/other/page2?a=b#c')"), "a same origin absolute URL");
        Check.equal("no error", errorOf("history.pushState(1, '', 'http://EXAMPLE.test:80/x')"), "case and default port do not matter");
    }

    /**
     * A document without an origin of its own (about:blank) cannot push a relative URL.
     * <p>
     * 자기 오리진이 없는 문서(about:blank)는 상대 URL을 푸시할 수 없다.
     */
    public void testBlankDocumentCannotPushRelativeUrl() {
        load("about:blank", "<p>x</p>");
        Check.equal("SecurityError:18:true", errorOf("history.pushState(1, '', '/x')"), "relative URL in about:blank");
        Check.equal("no error", errorOf("history.pushState(1, '')"), "state only is fine");
    }

    /**
     * A state that cannot be cloned is a DataCloneError (code 25) and changes nothing; so is an argument error.
     * <p>
     * 복제할 수 없는 상태는 DataCloneError(코드 25)이며 아무것도 바꾸지 않는다. 인수 오류도 마찬가지로 아무것도 바꾸지 않는다.
     */
    public void testNotCloneableStateAndArgumentErrors() {
        load("http://example.test/p", "<p>x</p>");
        Check.equal("DataCloneError:25:true", errorOf("history.pushState(function () {}, '')"), "function");
        Check.equal("DataCloneError:25:true", errorOf("history.replaceState({ s: Symbol('x') }, '')"), "symbol inside an object");
        Check.equal("DataCloneError:25:true", errorOf("history.pushState(document.body, '')"), "a DOM node");
        Check.equal("TypeError:undefined:false", errorOf("history.pushState({})"), "title is required");
        Check.equal("[]:http://example.test/p", env.calls + ":" + run("location.href"), "nothing changed");
    }

    /**
     * history.length comes from the environment; back, forward and go ask it to move; go(0) and go() reload.
     * <p>
     * history.length 는 환경에서 오며, back, forward, go 는 환경에 이동을 요구하고, go(0)과 go()는 새로고침한다.
     */
    public void testLengthAndMoves() {
        load("http://example.test/p", "<p>x</p>");
        env.length = 4;
        Check.equal("4", run("String(history.length)"), "length");
        run("history.back(); history.forward(); history.go(-2); history.go(3); history.go('1');");
        Check.equal("[-1, 1, -2, 3, 1]", env.moves.toString(), "moves");
        Check.equal("auto", run("history.scrollRestoration"), "default scrollRestoration");
        run("history.scrollRestoration = 'manual';");
        Check.equal("manual", run("history.scrollRestoration"), "set to manual");
        run("history.scrollRestoration = 'bogus';");
        Check.equal("manual", run("history.scrollRestoration"), "an invalid value is ignored");
        Check.equal("[object History]", run("String(history)"), "tag");
    }

    /**
     * A traversal to another entry of the document sets history.state and fires popstate (bubbling, with the state), then hashchange.
     * <p>
     * 문서의 다른 항목으로의 이동은 history.state 를 바꾸고 popstate(버블링, 상태 포함)를 보낸 뒤 hashchange 를 보낸다.
     */
    public void testTraversalFiresPopStateThenHashChange() {
        load("http://example.test/p", "<p>x</p>");
        run("var log = []; window.addEventListener('popstate', function (e) { log.push('pop:' + JSON.stringify(e.state) + ':' + (e instanceof PopStateEvent) + ':'"
                + " + e.bubbles + ':' + e.cancelable + ':' + JSON.stringify(history.state)); });"
                + "window.onhashchange = function (e) { log.push('hash:' + e.oldURL + '>' + e.newURL + ':' + (e instanceof HashChangeEvent)); };");
        ClonedValue c = StructuredClone.serialize(scripts.getRealm(), scripts.getRealm().evaluateScript("({ n: [1, 2] })", "state"));
        scripts.historyTraversed(c, true, "http://example.test/p", "http://example.test/p#a", true);
        scripts.historyTraversed(null, true, "http://example.test/p#a", "http://example.test/q", false);
        scripts.historyTraversed(null, false, "http://example.test/q", "http://example.test/q", false);
        Check.equal("pop:{\"n\":[1,2]}:true:true:false:{\"n\":[1,2]}|hash:http://example.test/p>http://example.test/p#a:true|pop:null:true:true:false:null",
                run("log.join('|')"), "events in order");
        Check.equal("null", run("String(history.state)"), "the last traversal set the state to null");
        scripts.historyTraversed(c, false, "x", "y", false);
        Check.equal("[1,2]", run("JSON.stringify(history.state.n)"), "a traversal without events still sets the state");
        Check.equal("3", run("String(log.length)"), "and fired nothing");
    }

    /**
     * A document loaded again for a history entry starts with the state of the entry.
     * <p>
     * 이력 항목을 위해 다시 읽은 문서는 그 항목의 상태를 가지고 시작한다.
     */
    public void testInitialStateOfAReloadedDocument() {
        env = new Recorder();
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(doc);
        doc.setUrl("http://example.test/p?s=1");
        scripts = new PageScripts(doc, env);
        scripts.setInitialHistoryState(StructuredClone.serialize(scripts.getRealm(), scripts.getRealm().evaluateScript("({ page: 7 })", "state")));
        HtmlParser parser = new HtmlParser(doc, "<script>window.seen = JSON.stringify(history.state);</script>");
        parser.setScripting(true);
        parser.setListener(scripts.createParserListener());
        scripts.parsingStarted();
        parser.run();
        scripts.completeParsing();
        Check.equal("{\"page\":7}", run("window.seen"), "the first script already sees the state");
    }

    /**
     * PopStateEvent and HashChangeEvent can be constructed, with the init dictionaries of the specification.
     * <p>
     * PopStateEvent 와 HashChangeEvent 는 규격의 초기화 사전과 함께 생성할 수 있다.
     */
    public void testEventConstructors() {
        load("http://example.test/p", "<p>x</p>");
        Check.equal("null:true:Event", run("var e = new PopStateEvent('popstate'); e.state + ':' + (e instanceof Event) + ':' + Object.getPrototypeOf(PopStateEvent).name"),
                "default state is null");
        Check.equal("5:true:popstate:false", run("var e2 = new PopStateEvent('popstate', { state: 5, bubbles: true }); e2.state + ':' + e2.bubbles + ':' + e2.type + ':' + e2.cancelable"),
                "state and bubbles");
        Check.equal("a>b:popstate", run("var h = new HashChangeEvent('hashchange', { oldURL: 'a', newURL: 'b' }); h.oldURL + '>' + h.newURL + ':' + e2.type"),
                "hash change dictionary");
        Check.equal("TypeError", run("(function () { try { PopStateEvent('x'); } catch (e) { return e.name; } })()"), "needs new");
    }
}
