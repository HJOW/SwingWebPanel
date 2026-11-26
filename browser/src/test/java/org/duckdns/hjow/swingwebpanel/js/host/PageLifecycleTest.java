package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the life cycle events of a page that the host reports (HTML Standard "Page lifecycle", fetched 2026-11-17; Page Visibility Level 2, fetched 2026-11-17;
 * see <code>docs/progress/webapi2.md</code>): <code>pageshow</code>, the unloading sequence (<code>beforeunload</code>, <code>pagehide</code>, <code>visibilitychange</code>,
 * <code>unload</code>), <code>document.visibilityState</code>, the window <code>focus</code>/<code>blur</code> events and <code>navigator.onLine</code> with the
 * <code>online</code>/<code>offline</code> events. The real component that triggers them is tested by <code>org.duckdns.hjow.swingwebpanel.core.PageLifecycleIntegrationTest</code>.
 * <p>
 * 호스트가 알리는 페이지 수명 이벤트를 시험한다(HTML Standard "Page lifecycle", Page Visibility Level 2, 모두 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조):
 * <code>pageshow</code>, 언로드 순서(<code>beforeunload</code>, <code>pagehide</code>, <code>visibilitychange</code>, <code>unload</code>), <code>document.visibilityState</code>, window 의
 * <code>focus</code>/<code>blur</code> 이벤트, <code>navigator.onLine</code> 과 <code>online</code>/<code>offline</code> 이벤트. 이를 일으키는 실제 컴포넌트는
 * <code>org.duckdns.hjow.swingwebpanel.core.PageLifecycleIntegrationTest</code> 가 시험한다.
 */
public class PageLifecycleTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PageLifecycleTest() {
    }

    /**
     * Loads a page and records the lifecycle events of the window and the document into <code>window.log</code>.
     * <p>
     * 페이지를 불러오고 window 와 문서의 수명 이벤트를 <code>window.log</code> 에 기록한다.
     *
     * @param script extra script that runs first / 먼저 실행할 추가 스크립트
     * @return the fixture / 시험 틀
     */
    private static PageFixture load(String script) {
        PageFixture f = new PageFixture().load("<div id=a></div><script>window.log = [];"
                + "['beforeunload', 'pagehide', 'pageshow', 'unload', 'focus', 'blur', 'online', 'offline'].forEach(function (t) { window.addEventListener(t, function (e) { "
                + "log.push(t + ':' + (e instanceof PageTransitionEvent ? 'pt' + e.persisted : e instanceof BeforeUnloadEvent ? 'bu' : e instanceof FocusEvent ? 'fe' : "
                + "'ev') + (e.bubbles ? '+' : '-') + (e.isTrusted ? 'T' : 'u')); }); });"
                + "document.addEventListener('visibilitychange', function (e) { log.push('visibilitychange:' + document.visibilityState + ':' + document.hidden + (e.bubbles ? '+' : '-')); });"
                + script + "</" + "script>");
        f.env.scheduler.advance(10);
        return f;
    }

    /**
     * <code>pageshow</code> fires after <code>load</code>, a page counts as shown from then on, and the document starts visible.
     * <p>
     * <code>pageshow</code> 는 <code>load</code> 뒤에 발생하고 그때부터 페이지는 보인 것으로 센다. 문서는 보이는 상태로 시작한다.
     */
    public void testPageShowAndInitialState() {
        PageFixture f = load("");
        Check.isTrue(f.scripts.isPageShown(), "the page was shown");
        Check.equal("pageshow:ptfalse-u", f.scripts.evaluate("log.join('|')", "t"), "pageshow with persisted false");
        Check.equal("visible|false|true", f.scripts.evaluate("[document.visibilityState, document.hidden, navigator.onLine].join('|')", "t"), "the initial state");
        Check.equal("function|function", f.scripts.evaluate("[typeof BeforeUnloadEvent, typeof PageTransitionEvent].join('|')", "t"), "the interfaces");
    }

    /**
     * The unloading sequence of a page that is left: the order of the events, their types, the state after <code>pagehide</code>, the answer of
     * <code>beforeunload</code> and the fact that the sequence runs once.
     * <p>
     * 떠나는 페이지의 언로드 순서: 이벤트의 순서와 종류, <code>pagehide</code> 뒤의 상태, <code>beforeunload</code> 의 답, 순서가 한 번만 실행된다는 것.
     */
    public void testUnloadingSequence() {
        PageFixture f = load("window.onbeforeunload = function (e) { log.push('onbeforeunload'); return 'Really?'; };"
                + "window.addEventListener('unload', function () { log.push('state:' + document.visibilityState); });");
        f.scripts.evaluate("log.length = 0", "t");
        Check.isTrue(f.scripts.getLifecycle().hasUnloadListeners(), "the page listens to the unloading events");
        String answer = f.scripts.getLifecycle().unload();
        Check.equal("Really?", answer, "the returned string of onbeforeunload is the confirmation text");
        // 도우미 스크립트의 리스너가 onbeforeunload 핸들러보다 먼저 등록되었으므로 먼저 불린다. pagehide 는 버블링하지 않는다.
        Check.equal("beforeunload:bu-T|onbeforeunload|pagehide:ptfalse-T|visibilitychange:hidden:true+|unload:ev-T|state:hidden", f.scripts.evaluate("log.join('|')", "t"),
                "order and kinds: beforeunload (not bubbling, a BeforeUnloadEvent), pagehide (a PageTransitionEvent), visibilitychange (hidden) and unload");
        f.scripts.evaluate("log.length = 0", "t");
        Check.isNull(f.scripts.getLifecycle().unload(), "the sequence runs once");
        Check.equal("", f.scripts.evaluate("log.join('|')", "t"), "and fires nothing the second time");
        Check.equal("hidden|true", f.scripts.evaluate("[document.visibilityState, document.hidden].join('|')", "t"), "an unloaded document stays hidden");
    }

    /**
     * What counts as a request to confirm: a cancelled event, a <code>returnValue</code>, a handler that returns a string; nothing otherwise.
     * <p>
     * 확인 요청으로 치는 것: 취소된 이벤트, <code>returnValue</code>, 문자열을 반환하는 핸들러. 그 밖에는 없다.
     */
    public void testBeforeUnloadAnswers() {
        PageFixture a = load("window.addEventListener('beforeunload', function (e) { e.preventDefault(); });");
        Check.equal("", a.scripts.getLifecycle().unload(), "preventDefault alone asks, with an empty text");
        PageFixture b = load("window.addEventListener('beforeunload', function (e) { e.returnValue = 'Bye?'; });");
        Check.equal("Bye?", b.scripts.getLifecycle().unload(), "returnValue asks with its text");
        PageFixture c = load("window.onbeforeunload = function () { };");
        Check.isNull(c.scripts.getLifecycle().unload(), "a handler that returns nothing does not ask");
        PageFixture d = load("window.onbeforeunload = function () { return null; };");
        Check.isNull(d.scripts.getLifecycle().unload(), "null does not ask");
        PageFixture e = load("window.onbeforeunload = function () { return 12; };");
        Check.equal("12", e.scripts.getLifecycle().unload(), "any other value asks, as text");
        PageFixture g = load("window.addEventListener('beforeunload', function (e) { e.returnValue = 'first'; });"
                + "window.onbeforeunload = function () { return 'second'; };");
        Check.equal("first", g.scripts.getLifecycle().unload(), "a returnValue that was set already stays");
    }

    /**
     * A page without listeners is closed without the unloading sequence, and a page that was never shown is not unloaded at all.
     * <p>
     * 리스너가 없는 페이지는 언로드 순서 없이 닫히고, 한 번도 보이지 않은 페이지는 언로드하지 않는다.
     */
    public void testNothingToUnload() {
        PageFixture f = new PageFixture().load("<div id=a></div>");
        f.env.scheduler.advance(10);
        Check.isFalse(f.scripts.getLifecycle().hasUnloadListeners(), "no listener");
        Check.isNull(f.scripts.getLifecycle().unload(), "an empty sequence answers null");
        PageFixture g = new PageFixture();
        g.document = null;
        PageFixture parsing = new PageFixture().load("<div id=a></div>");
        parsing.scripts.evaluate("window.log = []; window.addEventListener('unload', function () { log.push('unload'); });", "t");
        Check.equal("unload", firedBy(parsing), "a page with a listener runs the sequence");
    }

    /**
     * Fires the unloading sequence and returns the log of the page.
     * <p>
     * 언로드 순서를 실행하고 페이지의 기록을 반환한다.
     *
     * @param f the page / 페이지
     * @return the log joined with vertical bars / 세로줄로 이은 기록
     */
    private static String firedBy(PageFixture f) {
        f.scripts.getLifecycle().unload();
        return f.scripts.evaluate("log.join('|')", "t");
    }

    /**
     * The host says that the document became hidden and visible: <code>visibilitychange</code> bubbles from the document, the state is read from the host, and a
     * call that does not change the state fires nothing.
     * <p>
     * 호스트가 문서가 숨겨졌다가 보인다고 알린다: <code>visibilitychange</code> 는 문서에서 버블링하고, 상태는 호스트에서 읽으며, 상태가 바뀌지 않는 호출은 아무것도 발생시키지 않는다.
     */
    public void testVisibilityChanges() {
        PageFixture f = load("window.addEventListener('visibilitychange', function () { log.push('window-heard'); });");
        f.scripts.evaluate("log.length = 0", "t");
        f.scripts.getLifecycle().visibilityChanged();
        Check.equal("", f.scripts.evaluate("log.join('|')", "t"), "no change, no event");
        f.env.visible = false;
        f.scripts.getLifecycle().visibilityChanged();
        Check.equal("visibilitychange:hidden:true+|window-heard", f.scripts.evaluate("log.join('|')", "t"), "hidden: the event bubbles to the window");
        f.scripts.getLifecycle().visibilityChanged();
        f.env.visible = true;
        f.scripts.getLifecycle().visibilityChanged();
        Check.equal("visibilitychange:hidden:true+|window-heard|visibilitychange:visible:false+|window-heard", f.scripts.evaluate("log.join('|')", "t"),
                "visible again, and the repeated call did nothing");
    }

    /**
     * <code>focus</code> and <code>blur</code> at the window do not bubble, they are trusted focus events; <code>online</code> and <code>offline</code> follow the state of the
     * host and <code>navigator.onLine</code> reads it.
     * <p>
     * window 의 <code>focus</code> 와 <code>blur</code> 는 버블링하지 않고 신뢰하는 포커스 이벤트이다. <code>online</code> 과 <code>offline</code> 은 호스트의 상태를 따르고
     * <code>navigator.onLine</code> 이 그것을 읽는다.
     */
    public void testWindowFocusAndNetworkState() {
        PageFixture f = load("");
        f.scripts.evaluate("log.length = 0", "t");
        f.scripts.getLifecycle().windowFocus(true);
        f.scripts.getLifecycle().windowFocus(false);
        Check.equal("focus:fe-T|blur:fe-T", f.scripts.evaluate("log.join('|')", "t"), "focus and blur at the window");
        f.scripts.evaluate("log.length = 0", "t");
        f.env.online = false;
        Check.equal("false", f.scripts.evaluate("String(navigator.onLine)", "t"), "navigator.onLine follows the host");
        f.scripts.getLifecycle().onlineChanged();
        f.scripts.getLifecycle().onlineChanged();
        f.env.online = true;
        f.scripts.getLifecycle().onlineChanged();
        Check.equal("offline:ev-T|online:ev-T", f.scripts.evaluate("log.join('|')", "t"), "one event per change");
        Check.equal("true", f.scripts.evaluate("String(navigator.onLine)", "t"), "online again");
    }

    /**
     * A page that starts while the host says hidden (or offline) starts in that state, and the first change of the host is announced (a change during the load is
     * tested with the real component, <code>PageLifecycleIntegrationTest</code>).
     * <p>
     * 호스트가 hidden(또는 offline)이라고 할 때 시작한 페이지는 그 상태로 시작하고, 호스트의 첫 변화가 알려진다(로딩 중의 변화는 실제 컴포넌트로
     * <code>PageLifecycleIntegrationTest</code> 가 시험한다).
     */
    public void testStartStateAndChangesDuringTheLoad() {
        PageFixture hidden = new PageFixture();
        hidden.env.visible = false;
        hidden.env.online = false;
        hidden.load("<script>window.log = [];" + "window.log.push(document.visibilityState + ':' + navigator.onLine);"
                + "document.addEventListener('visibilitychange', function () { log.push('vc:' + document.visibilityState); });"
                + "window.addEventListener('online', function () { log.push('online'); });</" + "script>");
        hidden.env.scheduler.advance(10);
        Check.equal("hidden:false", hidden.scripts.evaluate("log.join('|')", "t"), "the page starts hidden and offline, and announces nothing for that");
        hidden.env.visible = true;
        hidden.env.online = true;
        hidden.scripts.getLifecycle().visibilityChanged();
        hidden.scripts.getLifecycle().onlineChanged();
        Check.equal("hidden:false|vc:visible|online", hidden.scripts.evaluate("log.join('|')", "t"), "the first changes of the host are announced");
    }

    /**
     * A page that listens to <code>visibilitychange</code> only at the document counts as a page that listens to the end of the page.
     * <p>
     * 문서에서만 <code>visibilitychange</code> 를 듣는 페이지도 페이지의 끝을 듣는 페이지로 센다.
     */
    public void testDocumentVisibilityListenerCounts() {
        PageFixture f = new PageFixture().load("<script>window.log = []; document.addEventListener('visibilitychange', function () { log.push(document.visibilityState); });</" + "script>");
        f.env.scheduler.advance(10);
        Check.isTrue(f.scripts.getLifecycle().hasUnloadListeners(), "a document listener is a listener for the unloading sequence");
        f.scripts.getLifecycle().unload();
        Check.equal("hidden", f.scripts.evaluate("log.join('|')", "t"), "and it hears the document become hidden");
    }

    /**
     * The lifecycle sees successful registrations made with an abort signal.
     * <p>
     * 수명 주기는 중단 신호를 지정하여 성공한 등록을 확인한다.
     */
    public void testAbortableUnloadListenersCount() {
        PageFixture f = new PageFixture().load("<script>var log=[]; var c=new AbortController();"
                + "document.addEventListener('visibilitychange',()=>log.push('hidden'),{signal:c.signal});"
                + "addEventListener('unload',()=>log.push('unload'),{signal:c.signal});</script>");
        f.env.scheduler.advance(10);
        Check.isTrue(f.scripts.getWindow().hasUnloadListener(), "the window records an active abortable listener");
        Check.isTrue(f.scripts.getDocument().hasVisibilityListener(), "the document records an active abortable listener");
        f.scripts.getLifecycle().unload();
        Check.equal("hidden,unload", f.scripts.evaluate("log.join(',')", "t"), "the lifecycle delivers both registered events");
    }

    /**
     * Nothing is fired once the scripts are switched off.
     * <p>
     * 스크립트를 끄면 아무것도 발생시키지 않는다.
     */
    public void testNothingAfterScriptsAreOff() {
        PageFixture f = load("");
        f.scripts.evaluate("log.length = 0", "t");
        f.env.scripting = false;
        f.env.visible = false;
        f.scripts.getLifecycle().visibilityChanged();
        f.scripts.getLifecycle().windowFocus(true);
        f.env.scripting = true;
        Check.equal("", f.scripts.evaluate("log.join('|')", "t"), "no event while scripting was off");
    }
}
