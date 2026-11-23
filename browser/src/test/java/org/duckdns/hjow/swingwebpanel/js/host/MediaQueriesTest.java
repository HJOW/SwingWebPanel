package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>window.matchMedia</code> and the viewport events of the page (plan items P20-01, P20-12): the live <code>matches</code>, the
 * <code>change</code> events of the lists (in the order of "update the rendering": <code>resize</code>, <code>scroll</code>, then the media
 * query changes), the legacy listener methods and the lists that a script dropped.
 * <p>
 * <code>window.matchMedia</code> 와 페이지의 뷰포트 이벤트의 시험(계획 P20-01, P20-12): 실시간 <code>matches</code>, 목록의 <code>change</code>
 * 이벤트("렌더링 갱신" 순서: <code>resize</code>, <code>scroll</code>, 그 뒤 미디어 질의 변화), 옛 리스너 메서드, 스크립트가 버린 목록.
 */
public class MediaQueriesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaQueriesTest() {
    }

    /**
     * P20-01: <code>matches</code> follows the viewport and <code>media</code> is the query.
     * <p>
     * P20-01: <code>matches</code> 는 뷰포트를 따르고 <code>media</code> 는 질의이다.
     */
    public void testMatchesAndMedia() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("true,false,true,false,(min-width: 600px)", f.scripts.evaluate("var a = matchMedia('(min-width: 600px)'); "
                + "[a.matches, matchMedia('(min-width: 900px)').matches, matchMedia('screen').matches, matchMedia('print').matches, a.media].join(',')", "t"),
                "queries against the default 800x600 viewport");
        Check.equal("false", f.scripts.evaluate("var b = matchMedia('(max-width: 700px)'); String(b.matches)", "t"), "wider than 700px");
        f.env.viewportWidth = 600;
        Check.equal("true", f.scripts.evaluate("String(b.matches)", "t"), "matches is live: no event is needed to read it");
        Check.equal("TypeError", f.scripts.evaluate("try { matchMedia() } catch (e) { e.name }", "t"), "the query is required");
        Check.equal("false", f.scripts.evaluate("String(matchMedia('not a valid query ((').matches)", "t"), "an invalid query does not match");
        Check.equal("[object MediaQueryList]", f.scripts.evaluate("Object.prototype.toString.call(b)", "t"), "tag");
        Check.equal("true", f.scripts.evaluate("String(b instanceof EventTarget)", "t"), "a list is an event target");
    }

    /**
     * P20-01: a viewport change fires <code>resize</code>, <code>scroll</code> and then the <code>change</code> events of the lists whose result
     * changed; a list whose result did not change gets no event.
     * <p>
     * P20-01: 뷰포트 변화는 <code>resize</code>, <code>scroll</code>, 그 뒤 결과가 바뀐 목록의 <code>change</code> 이벤트를 발생시킨다. 결과가 바뀌지
     * 않은 목록에는 이벤트가 없다.
     */
    public void testChangeEvents() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        f.scripts.evaluate("window.log = []; window.narrow = matchMedia('(max-width: 700px)'); window.wide = matchMedia('(min-width: 300px)');"
                + "narrow.onchange = function (e) { log.push('onchange:' + e.matches + ':' + e.media + ':' + (e.target === narrow)); };"
                + "narrow.addListener(function (e) { log.push('listener:' + e.matches); });"
                + "wide.addEventListener('change', function (e) { log.push('wide:' + e.matches); });"
                + "window.addEventListener('resize', function () { log.push('resize'); });"
                + "document.addEventListener('scroll', function (e) { log.push('scroll:' + e.bubbles); });"
                + "window.addEventListener('scroll', function () { log.push('window-scroll'); });", "t");
        f.env.viewportWidth = 600;
        f.scripts.viewportChanged(true, true);
        Check.equal("resize|scroll:true|window-scroll|onchange:true:(max-width: 700px):true|listener:true", f.scripts.evaluate("log.join('|')", "t"),
                "the order of the events and the list that changed");
        f.scripts.evaluate("log.length = 0;", "t");
        f.env.viewportWidth = 650;
        f.scripts.viewportChanged(true, false);
        Check.equal("resize", f.scripts.evaluate("log.join('|')", "t"), "the result did not change: only resize");
        f.scripts.evaluate("log.length = 0;", "t");
        f.env.viewportWidth = 200;
        f.scripts.viewportChanged(true, false);
        Check.equal("resize|wide:false", f.scripts.evaluate("log.join('|')", "t"), "the other list changed");
        f.scripts.evaluate("log.length = 0; narrow.removeListener(narrow.onchange); window.narrow = null;", "t");
        f.scripts.viewportChanged(false, true);
        Check.equal("scroll:true|window-scroll", f.scripts.evaluate("log.join('|')", "t"), "a scroll alone changes no list");
    }

    /**
     * P20-12: nothing is fired after the page is disposed.
     * <p>
     * P20-12: 페이지가 폐기된 뒤에는 아무것도 발생하지 않는다.
     */
    public void testNothingAfterDispose() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        f.scripts.evaluate("window.count = 0; window.addEventListener('resize', function () { count++; });", "t");
        f.scripts.viewportChanged(true, false);
        Check.equal("1", f.scripts.evaluate("String(count)", "t"), "fired while alive");
        f.scripts.dispose();
        f.scripts.viewportChanged(true, true);
        Check.equal(true, f.scripts.isDisposed(), "disposed");
    }
}
