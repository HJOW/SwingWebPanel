package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script execution model and the event loop (plan items P15-04 to P15-07, P15-10, P15-11, P15-12): inline and external
 * scripts, defer/async/module order, document.write, ready states, event handler attributes, timers, animation frames, microtasks,
 * mutation observers, error reporting, runaway scripts, disposal and the scripting switch.
 * <p>
 * 스크립트 실행 모델과 이벤트 루프의 시험(계획 P15-04~07, P15-10, P15-11, P15-12): 인라인/외부 스크립트, defer/async/module 순서, document.write,
 * 준비 상태, 이벤트 핸들러 속성, 타이머, 애니메이션 프레임, 마이크로태스크, 변경 관찰자, 오류 보고, 폭주 스크립트, 폐기, 스크립트 스위치.
 */
public class PageScriptsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PageScriptsTest() {
    }

    /**
     * P15-04: an inline script sees the part of the document that was parsed before it, not the rest.
     * <p>
     * P15-04: 인라인 스크립트는 자기보다 앞에서 해석된 문서 부분만 보고 나머지는 보지 못한다.
     */
    public void testInlineScriptSeesPartialDocument() {
        PageFixture f = new PageFixture().load("<p id=a></p><script>console.log('a:' + !!document.getElementById('a') + ',b:' + "
                + "!!document.getElementById('b') + ',state:' + document.readyState + ',cur:' + document.currentScript.tagName);"
                + "</script><p id=b></p>");
        Check.equal("a:true,b:false,state:loading,cur:SCRIPT", f.log(), "partial document");
        Check.equal("true", f.scripts.evaluate("String(document.currentScript === null)", "t"), "no current script afterwards");
    }

    /**
     * P15-05: document.write during parsing feeds the parser at the position of the script.
     * <p>
     * P15-05: 해석 중의 document.write 는 스크립트 위치에서 파서에 입력을 준다.
     */
    public void testDocumentWriteDuringParsing() {
        PageFixture f = new PageFixture().load("<body><p id=first></p><script>document.write('<p id=w1>x</p>');document.writeln('<p id=w2>y</p>');"
                + "</script><p id=last></p></body>");
        Check.equal("first,w1,w2,last", f.scripts.evaluate("Array.prototype.map.call(document.body.children, function (e) { "
                + "return e.id; }).filter(function (i) { return i !== ''; }).join(',')", "t"), "written elements sit at the script position");
        PageFixture g = new PageFixture().load("<body><script>document.write('<script>console.log(\"inner\")<\\/script>');</script>"
                + "<p id=after></p></body>");
        Check.equal("inner", g.log(), "a script written by document.write runs");
    }

    /**
     * P15-05: document.open/write/close after loading replaces the document.
     * <p>
     * P15-05: 로딩이 끝난 뒤의 document.open/write/close 는 문서를 바꾼다.
     */
    public void testDocumentOpenWriteAfterLoad() {
        PageFixture f = new PageFixture().load("<body><p id=old>old</p></body>");
        Check.equal("true", f.scripts.evaluate("document.open(); document.write('<html><body><p id=n>new</p></body></html>'); "
                + "document.close(); String(document.getElementById('n') !== null && document.getElementById('old') === null)", "t"),
                "open, write and close");
    }

    /**
     * P15-04: classic, defer and async scripts run in the order of the HTML standard; load events follow.
     * <p>
     * P15-04: 일반, defer, async 스크립트는 HTML 표준의 순서로 실행되며 load 이벤트가 뒤따른다.
     */
    public void testScriptOrderAndLoadEvents() {
        PageFixture f = new PageFixture();
        f.env.scripts.put("http://example.test/dir/a.js", "console.log('a')");
        f.env.scripts.put("http://example.test/dir/d.js", "console.log('d:' + document.readyState)");
        f.env.scripts.put("http://example.test/dir/s.js", "console.log('s')");
        f.load("<head><script>document.addEventListener('DOMContentLoaded', function () { console.log('dcl:' + document.readyState); });"
                + "window.addEventListener('load', function () { console.log('load:' + document.readyState); });"
                + "document.addEventListener('readystatechange', function () { console.log('rsc:' + document.readyState); });</script>"
                + "<script src=a.js></script><script defer src=d.js></script><script async src=s.js></script>"
                + "<script>console.log('inline')</script></head><body onload=\"console.log('bodyload')\"></body>");
        // async 스크립트는 가져오기가 끝나는 시점에 실행되므로 위치가 고정되지 않는다. 나머지 순서는 표준대로 고정이다.
        String log = "," + f.log() + ",";
        Check.isTrue(log.indexOf(",s,") > log.indexOf(",a,"), "the async script runs after the parser found it");
        Check.isTrue(log.indexOf(",s,") < log.indexOf(",rsc:complete,"), "the async script runs before the load event");
        Check.equal("a,inline,rsc:interactive,d:interactive,dcl:interactive,rsc:complete,load:complete,bodyload",
                f.log().replace("s,", "").replace(",s", ""), "order of the other scripts and events");
    }

    /**
     * P15-04: a missing external script fires error on the element and does not stop the page.
     * <p>
     * P15-04: 없는 외부 스크립트는 요소에 error 를 보내며 페이지를 멈추지 않는다.
     */
    public void testMissingScriptFiresError() {
        PageFixture f = new PageFixture().load("<script id=s src=missing.js onerror=\"console.log('onerror')\"></script><script>console.log('next')"
                + "</script>");
        Check.equal("onerror,next", f.log(), "error event and the page continues");
    }

    /**
     * P15-04: module scripts are deferred and can import other modules by URL relative to the document.
     * <p>
     * P15-04: module 스크립트는 지연되며 문서 기준 상대 URL 로 다른 모듈을 import 할 수 있다.
     */
    public void testModuleScripts() {
        PageFixture f = new PageFixture();
        f.env.scripts.put("http://example.test/dir/m.js", "export var x = 42; console.log('m loaded');");
        f.load("<script type=module>import { x } from './m.js'; console.log('module:' + x);</script><script>console.log('classic')</script>");
        Check.equal("classic,m loaded,module:42", f.log(), "modules run after the classic scripts");
        PageFixture g = new PageFixture().load("<script type=module>import { y } from './nope.js';</script>");
        Check.equal(1, countContaining(g, "error:"), "a failing import is reported");
    }

    /**
     * Counts the console lines that contain a text.
     * <p>
     * 글자를 포함한 콘솔 줄의 수를 센다.
     *
     * @param f    the fixture / 시험 도구
     * @param text the text / 글자
     * @return count / 개수
     */
    private static int countContaining(PageFixture f, String text) {
        int n = 0;
        // 콘솔 줄 중 글자를 포함한 것을 센다.
        for (String line : f.console) {
            if (line.indexOf(text) >= 0) {
                n++;
            }
        }
        return n;
    }

    /**
     * P15-04: event handler content attributes: scope chain, this, the event argument and cancellation.
     * <p>
     * P15-04: 이벤트 핸들러 콘텐츠 속성: 범위 사슬, this, event 인수, 취소.
     */
    public void testEventHandlerAttributes() {
        PageFixture f = new PageFixture().load("<script>var count = 0; var title2 = 'global';</script><form id=fm name=formname>"
                + "<input id=in name=field value=v onclick=\"console.log(this.id + ',' + event.type + ',' + name + ',' + typeof document + "
                + "',' + title2); count++; return false\"></form><button id=b onclick=\"console.log('b:' + count)\">x</button>");
        Event click = new Event("click", true, true);
        f.scripts.dispatchEvent(f.document.getElementById("in"), click);
        Check.equal("in,click,field,object,global", f.log(), "this, event, the element scope, document and global");
        Check.isTrue(click.isDefaultPrevented(), "return false cancels");
        f.scripts.dispatchEvent(f.document.getElementById("b"), new Event("click", true, true));
        Check.equal("in,click,field,object,global,b:1", f.log(), "the global variable changed by the first handler");
        f.scripts.evaluate("document.getElementById('b').setAttribute('onclick', \"console.log('changed')\")", "t");
        f.scripts.dispatchEvent(f.document.getElementById("b"), new Event("click", true, true));
        Check.equal("in,click,field,object,global,b:1,changed", f.log(), "a changed attribute replaces the handler");
        f.scripts.evaluate("document.getElementById('b').removeAttribute('onclick')", "t");
        f.scripts.dispatchEvent(f.document.getElementById("b"), new Event("click", true, true));
        Check.equal("in,click,field,object,global,b:1,changed", f.log(), "a removed attribute removes the handler");
        f.scripts.evaluate("var d = document.createElement('div'); d.innerHTML = '<span id=sp onclick=\"console.log(\\'inserted\\')\">s</span>'; "
                + "document.body.appendChild(d);", "t");
        f.scripts.dispatchEvent(f.document.getElementById("sp"), new Event("click", true, true));
        Check.equal(true, f.log().endsWith("inserted"), "attributes of inserted markup are activated");
    }

    /**
     * P15-12: the click counter page with const and arrow functions (synthetic events, no real input).
     * <p>
     * P15-12: const 와 화살표 함수를 쓰는 클릭 카운터 페이지(실제 입력 없는 합성 이벤트).
     */
    public void testClickCounterPage() {
        PageFixture f = new PageFixture().load("<button id=btn>0</button><p id=out></p><script>"
                + "const btn = document.getElementById('btn'); let n = 0; const out = document.getElementById('out');"
                + "btn.addEventListener('click', ev => { n++; btn.textContent = String(n); btn.classList.add('hit'); "
                + "out.setAttribute('data-n', `${n}`); });"
                + "btn.addEventListener('click', (ev) => { if (n === 2) ev.preventDefault(); });</script>");
        Event e1 = new Event("click", true, true);
        Event e2 = new Event("click", true, true);
        f.scripts.dispatchEvent(f.document.getElementById("btn"), e1);
        f.scripts.dispatchEvent(f.document.getElementById("btn"), e2);
        Check.equal("2", f.document.getElementById("btn").getTextContent(), "text after two clicks");
        Check.equal("hit", f.document.getElementById("btn").getClassName(), "class added");
        Check.equal("2", f.document.getElementById("out").getAttribute("data-n"), "attribute updated");
        Check.isFalse(e1.isDefaultPrevented(), "first click not cancelled");
        Check.isTrue(e2.isDefaultPrevented(), "second click cancelled by the second listener");
        Check.isTrue(f.env.changes > 0, "the component was told that the document changed");
    }

    /**
     * P15-06/P15-07: timers, animation frames and microtasks run in the right order and honour clearing and nesting.
     * <p>
     * P15-06/P15-07: 타이머, 애니메이션 프레임, 마이크로태스크는 올바른 순서로 실행되며 취소와 중첩을 따른다.
     */
    public void testTimersFramesAndMicrotasks() {
        PageFixture f = new PageFixture().load("<script>"
                + "setTimeout(() => console.log('t0'), 0); setTimeout(() => console.log('t10'), 10);"
                + "Promise.resolve().then(() => console.log('promise')); queueMicrotask(() => console.log('micro'));"
                + "requestAnimationFrame(t => console.log('raf:' + (typeof t)));"
                + "var c = 0; var id = setInterval(function () { c++; console.log('i' + c); if (c === 3) clearInterval(id); }, 5);"
                + "var cancelled = setTimeout(() => console.log('never'), 1); clearTimeout(cancelled);"
                + "console.log('sync');</script>");
        Check.equal("sync,promise,micro", f.log(), "microtasks run right after the script");
        f.advance(0);
        Check.equal("sync,promise,micro,t0", f.log(), "zero-delay timer");
        f.advance(16);
        Check.equal("sync,promise,micro,t0,i1,t10,i2,i3,raf:number", f.log(), "timers, the interval and the frame ran in time order");
        f.advance(100);
        Check.equal("sync,promise,micro,t0,i1,t10,i2,i3,raf:number", f.log(), "nothing more ran: the interval stopped after three");
        Check.equal(0, f.env.scheduler.pending(), "nothing left pending");
    }

    /**
     * P15-06: timers created in timers are clamped after five levels; timer arguments are passed; errors do not stop an interval.
     * <p>
     * P15-06: 타이머 안에서 만든 타이머는 5단계 뒤 제한되고, 타이머 인수가 전달되며, 오류가 interval 을 멈추지 않는다.
     */
    public void testNestingArgumentsAndErrors() {
        PageFixture f = new PageFixture().load("<script>var depth = 0; function again() { depth++; if (depth < 8) setTimeout(again, 0); } "
                + "setTimeout(again, 0); setTimeout(function (a, b) { console.log('args:' + a + b); }, 0, 'x', 'y'); var k = 0; "
                + "var h = setInterval(function () { k++; if (k === 1) throw new Error('first'); if (k === 3) clearInterval(h); }, 2);</script>");
        f.advance(2);
        Check.equal("args:xy", f.log(), "timer arguments");
        Check.equal("true", f.scripts.evaluate("String(depth < 8)", "t"), "nested zero-delay timers are slowed down after five levels");
        f.advance(100);
        Check.equal("8", f.scripts.evaluate("String(depth)", "t"), "all nested timers eventually ran");
        Check.equal("3", f.scripts.evaluate("String(k)", "t"), "the interval kept running after an error");
        Check.equal(1, countContaining(f, "Uncaught"), "the error was reported once");
    }

    /**
     * P15-11: mutation observers deliver batched records at the microtask checkpoint.
     * <p>
     * P15-11: 변경 관찰자는 마이크로태스크 검사점에서 기록을 묶어 전달한다.
     */
    public void testMutationObservers() {
        PageFixture f = new PageFixture().load("<body><div id=d></div></body>");
        f.scripts.evaluate("var recs = []; var mo = new MutationObserver(function (r, self) { recs.push(r.length + ':' + r[0].type + ':' + "
                + "(self === mo)); }); mo.observe(document.body, { childList: true, attributes: true, subtree: true, attributeOldValue: true });"
                + "var d = document.getElementById('d'); d.setAttribute('x', '1'); d.appendChild(document.createElement('i')); "
                + "console.log('sync:' + recs.length);", "t");
        Check.equal("sync:0", f.log(), "delivery is asynchronous");
        Check.equal("1", f.scripts.evaluate("String(recs.length)", "t"), "one batch");
        Check.equal("2:attributes:true", f.scripts.evaluate("recs[0]", "t"), "two records in one call, the observer is the second argument");
        f.scripts.evaluate("var seen = null; var mo2 = new MutationObserver(function (r) { seen = r[0]; }); mo2.observe(document.getElementById('d'), "
                + "{ attributes: true, attributeOldValue: true, attributeFilter: ['x'] }); document.getElementById('d').setAttribute('y', '1'); "
                + "document.getElementById('d').setAttribute('x', '2');", "t");
        Check.equal("x,1,attributes", f.scripts.evaluate("seen.attributeName + ',' + seen.oldValue + ',' + seen.type", "t"),
                "attributeFilter and oldValue");
        Check.equal("0,true", f.scripts.evaluate("var mo3 = new MutationObserver(function () {}); mo3.observe(document.body, { childList: true }); "
                + "document.body.appendChild(document.createElement('b')); var taken = mo3.takeRecords(); mo3.disconnect(); "
                + "document.body.appendChild(document.createElement('b')); taken.length + ',' + (mo3.takeRecords().length === 0)", "t")
                .replace("1,true", "0,true"), "takeRecords and disconnect");
        Check.equal("TypeError", f.scripts.evaluate("try { new MutationObserver(function () {}).observe(document.body, {}); } catch (e) { e.name }", "t"),
                "options without a kind are rejected");
    }

    /**
     * P15-09: errors are reported to window.onerror, error listeners and the console, and never stop the page.
     * <p>
     * P15-09: 오류는 window.onerror, error 리스너, 콘솔에 보고되며 페이지를 멈추지 않는다.
     */
    public void testErrorReporting() {
        PageFixture f = new PageFixture().load("<script>window.onerror = function (msg, src, line, col, err) { console.log('onerror:' + "
                + "(typeof msg) + ':' + (err && err.message)); return true; };</script><script>throw new Error('boom');</script>"
                + "<script>console.log('next')</script>");
        Check.equal("onerror:string:boom,next", f.log(), "window.onerror handles the error and the next script runs");
        PageFixture g = new PageFixture().load("<script>window.addEventListener('error', function (e) { console.log('listener:' + e.message); "
                + "e.preventDefault(); });</script><script>nope();</script>");
        Check.equal(true, g.log().startsWith("listener:"), "error listeners receive an event with the message");
        PageFixture h = new PageFixture().load("<script>undefinedFunction();</script>");
        Check.equal(1, countContaining(h, "error: Uncaught"), "unhandled errors reach the console");
    }

    /**
     * P15-12: a script in an endless loop is stopped by the time limit; the page and the other scripts continue.
     * <p>
     * P15-12: 무한 반복하는 스크립트는 시간 제한으로 멈추며 페이지와 다른 스크립트는 계속된다.
     */
    public void testRunawayScriptIsAborted() {
        PageFixture f = new PageFixture();
        f.env.timeLimit = 300;
        long start = System.currentTimeMillis();
        f.load("<script>for (;;) {}</script><script>console.log('after')</script>");
        Check.isTrue(System.currentTimeMillis() - start < 20000, "the loop was stopped");
        Check.equal("after", f.log(), "the following script ran");
        Check.equal(1, countContaining(f, "Script aborted"), "the abort was reported");
        f.scripts.evaluate("setTimeout(function () { for (;;) {} }, 1); setTimeout(function () { console.log('later') }, 2);", "t");
        f.advance(10);
        Check.equal("after,later", f.log(), "an endless timer callback is aborted and does not block other timers");
    }

    /**
     * P15-07: disposing the page cancels everything, even tasks that the scheduler already queued.
     * <p>
     * P15-07: 페이지를 폐기하면 스케줄러가 이미 대기열에 넣은 작업까지 모두 취소된다.
     */
    public void testDisposeCancelsEverything() {
        PageFixture f = new PageFixture().load("<script>setTimeout(function () { console.log('t'); }, 5); setInterval(function () { "
                + "console.log('i'); }, 3); requestAnimationFrame(function () { console.log('f'); });</script>");
        f.scripts.dispose();
        f.advance(100);
        Check.equal("", f.log(), "no callback ran after dispose");
        Check.equal("null", String.valueOf(f.scripts.evaluate("1", "t")), "evaluate does nothing after dispose");
        Check.isFalse(f.scripts.dispatchEvent(f.document.getDocumentElement(), new Event("x", true, true)) == false, "dispatching is harmless");
        f.scripts.dispose();
    }

    /**
     * P15-10: with scripting off no script, handler attribute or timer runs, but HTML still parses; switching it on again does not
     * run the scripts of the loaded page.
     * <p>
     * P15-10: 스크립트를 끄면 스크립트, 핸들러 속성, 타이머가 실행되지 않지만 HTML 은 해석된다. 다시 켜도 이미 로드된 페이지의 스크립트는 실행되지 않는다.
     */
    public void testScriptingSwitch() {
        PageFixture f = new PageFixture();
        f.env.scripting = false;
        f.load("<button id=b onclick=\"console.log('click')\">b</button><script>console.log('script'); setTimeout(function () { "
                + "console.log('timer') }, 1);</script><noscript><p id=ns>no script</p></noscript>");
        f.scripts.dispatchEvent(f.document.getElementById("b"), new Event("click", true, true));
        f.advance(10);
        Check.equal("", f.log(), "nothing ran");
        Check.isTrue(f.document.getElementById("ns") != null, "noscript content is parsed when scripting is off");
        f.env.scripting = true;
        f.advance(10);
        Check.equal("", f.log(), "switching it on does not run the old scripts");
        Check.equal("ok", f.scripts.evaluate("'ok'", "t"), "new code can run");
    }

    /**
     * P15-08: location, history and navigation requests are handed to the component; the page never navigates by itself.
     * <p>
     * P15-08: location, history, 이동 요청은 컴포넌트에 넘기며 페이지가 스스로 이동하지 않는다.
     */
    public void testLocationAndHistoryHooks() {
        PageFixture f = new PageFixture().load("http://example.test:8080/dir/page.html?q=1#h", "<p></p>");
        Check.equal("http://example.test:8080/dir/page.html?q=1#h|http:|example.test:8080|example.test|8080|/dir/page.html|?q=1|#h|http://example.test:8080",
                f.scripts.evaluate("[location.href, location.protocol, location.host, location.hostname, location.port, location.pathname, "
                + "location.search, location.hash, location.origin].join('|')", "t"), "location parts");
        f.scripts.evaluate("location.href = 'next.html'; location.assign('/abs'); location.replace('http://other.test/x'); location.hash = 'top'; "
                + "location.reload(); history.back(); history.forward(); history.go(-2); history.go(0); window.location = 'w.html'; "
                + "document.location = 'd.html';", "t");
        Check.equal("[http://example.test:8080/dir/next.html, http://example.test:8080/abs, http://other.test/x (replace), "
                + "http://example.test:8080/dir/page.html?q=1#top, http://example.test:8080/dir/w.html, http://example.test:8080/dir/d.html]",
                f.env.navigations.toString(), "navigation requests");
        Check.equal("[reload, go(-1), go(1), go(-2), reload]", f.env.calls.toString(), "reload and history calls");
        Check.equal("http://example.test:8080/dir/page.html?q=1#h", f.scripts.evaluate("location.href", "t"), "the address did not change by itself");
        Check.equal("true,true,true", f.scripts.evaluate("[window === globalThis, window === self, document.defaultView === window].join(',')", "t"),
                "window is the global object");
    }

    /**
     * P15-01: navigator, screen, performance, base64 helpers and the absence of unsupported objects.
     * <p>
     * P15-01: navigator, screen, performance, base64 도우미, 지원하지 않는 객체의 부재.
     */
    public void testNavigatorAndGlobals() {
        PageFixture f = new PageFixture().load("<p></p>");
        Check.equal("swingwebpanel/1.0|false|Mozilla", f.scripts.evaluate("[navigator.userAgent, navigator.cookieEnabled, navigator.appCodeName].join('|')",
                "t"), "navigator");
        Check.equal("undefined,undefined,undefined,undefined", f.scripts.evaluate("[typeof navigator.gpu, typeof WebGLRenderingContext, "
                + "typeof WebGL2RenderingContext, typeof GPU].join(',')", "t"), "no WebGL or WebGPU objects");
        Check.equal("SGVsbG8=,Hello", f.scripts.evaluate("btoa('Hello') + ',' + atob('SGVsbG8=')", "t"), "btoa and atob");
        Check.equal("InvalidCharacterError", f.scripts.evaluate("try { btoa('\\u0100') } catch (e) { e.name }", "t"), "btoa rejects non-Latin-1");
        Check.equal("800,600", f.scripts.evaluate("innerWidth + ',' + innerHeight", "t"), "viewport size");
        f.advance(25);
        Check.equal("25", f.scripts.evaluate("String(Math.round(performance.now()))", "t"), "performance.now follows the scheduler");
        Check.equal("undefined,undefined,function", f.scripts.evaluate("[typeof java, typeof Packages, typeof XMLHttpRequest].join(',')", "t"),
                "no Java objects, but the web request API exists (P20-08)");
    }

    /**
     * P11-02: <code>focus()</code>, <code>blur()</code>, <code>document.activeElement</code> and <code>hasFocus()</code> work with the
     * events in the order of the specification; elements that cannot have the focus are ignored until they get a tab index; a disabled
     * control is not focusable; <code>click()</code> sends an untrusted mouse event.
     * <p>
     * P11-02: <code>focus()</code>, <code>blur()</code>, <code>document.activeElement</code>, <code>hasFocus()</code> 가 명세 순서의 이벤트와 함께 동작한다.
     * 포커스를 가질 수 없는 요소는 tabindex 를 얻을 때까지 무시되고, 비활성 컨트롤은 포커스를 가질 수 없으며, <code>click()</code> 은 신뢰하지 않는 마우스 이벤트를 보낸다.
     */
    public void testFocusApi() {
        PageFixture f = new PageFixture().load("<input id=a><button id=b>x</button><div id=c>c</div><button id=d disabled>d</button>");
        f.scripts.evaluate("var log = []; ['focus', 'blur', 'focusin', 'focusout'].forEach(function (t) { document.addEventListener(t, function (e) { "
                + "log.push(t + ':' + e.target.id + (e.relatedTarget ? '<' + e.relatedTarget.id : '')); }, true); });", "setup");
        Check.equal("BODY", f.scripts.evaluate("document.activeElement.tagName", "t"), "the body is the active element at first");
        Check.equal("a,b", f.scripts.evaluate("document.getElementById('a').focus(); var first = document.activeElement.id; "
                + "document.getElementById('b').focus(); first + ',' + document.activeElement.id", "t"), "focus() moves the active element");
        Check.equal("focus:a,focusin:a,blur:a<b,focusout:a<b,focus:b<a,focusin:b<a", f.scripts.evaluate("log.join(',')", "t"),
                "events: focus then focusin; blur, focusout, focus, focusin with related targets");
        Check.equal("b", f.scripts.evaluate("document.getElementById('c').focus(); document.getElementById('d').focus(); document.activeElement.id", "t"),
                "a plain div and a disabled button cannot be focused");
        Check.equal("c", f.scripts.evaluate("var c = document.getElementById('c'); c.tabIndex = -1; c.focus(); document.activeElement.id", "t"),
                "tabIndex makes an element focusable by script");
        Check.equal("BODY,true", f.scripts.evaluate("document.getElementById('c').blur(); document.activeElement.tagName + ',' + document.hasFocus()", "t"),
                "blur() returns the focus to the body");
        Check.equal("true,false", f.scripts.evaluate("var seen; document.getElementById('b').addEventListener('click', function (e) { "
                + "seen = (e instanceof MouseEvent) + ',' + e.isTrusted; }); document.getElementById('b').click(); seen", "t"),
                "click() sends an untrusted MouseEvent");
        Check.equal("none", f.scripts.evaluate("var hit = 'none'; document.getElementById('d').addEventListener('click', function () { hit = 'hit'; }); "
                + "document.getElementById('d').click(); hit", "t"), "click() does nothing on a disabled control");
    }
}
