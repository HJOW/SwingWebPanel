package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the small window members (plan items P20-01, P20-12): <code>origin</code>, <code>isSecureContext</code>, <code>status</code>,
 * <code>reportError</code>, <code>requestIdleCallback</code>/<code>cancelIdleCallback</code> and the additions of <code>screen</code>.
 * <p>
 * 작은 window 멤버의 시험(계획 P20-01, P20-12): <code>origin</code>, <code>isSecureContext</code>, <code>status</code>, <code>reportError</code>,
 * <code>requestIdleCallback</code>/<code>cancelIdleCallback</code>, <code>screen</code> 의 추가 멤버.
 */
public class WindowBasicsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WindowBasicsTest() {
    }

    /**
     * <code>origin</code> and <code>isSecureContext</code> follow the URL of the document.
     * <p>
     * <code>origin</code> 과 <code>isSecureContext</code> 는 문서의 URL 을 따른다.
     */
    public void testOriginAndSecureContext() {
        PageFixture https = new PageFixture().load("https://example.test/a/b?x=1", "<p>x</p>");
        Check.equal("https://example.test|true", https.scripts.evaluate("origin + '|' + isSecureContext", "t"), "an https page");
        PageFixture local = new PageFixture().load("http://localhost:8080/x", "<p>x</p>");
        Check.equal("http://localhost:8080|true", local.scripts.evaluate("origin + '|' + isSecureContext", "t"), "a loopback http page is trustworthy");
        PageFixture plain = new PageFixture().load("http://example.test/x", "<p>x</p>");
        Check.equal("http://example.test|false", plain.scripts.evaluate("origin + '|' + isSecureContext", "t"), "plain http");
        PageFixture blank = new PageFixture().load("<p>x</p>");
        Check.equal("false", blank.scripts.evaluate("String(isSecureContext)", "t"), "a page without a network origin");
    }

    /**
     * <code>status</code> and <code>defaultStatus</code> are strings that scripts may write; <code>reportError</code> reaches
     * <code>window.onerror</code> and the <code>error</code> event.
     * <p>
     * <code>status</code> 와 <code>defaultStatus</code> 는 스크립트가 쓸 수 있는 문자열이다. <code>reportError</code> 는
     * <code>window.onerror</code> 와 <code>error</code> 이벤트에 닿는다.
     */
    public void testStatusAndReportError() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("|a|b", f.scripts.evaluate("var r = [status]; status = 'a'; r.push(status); defaultStatus = 'b'; r.push(defaultStatus); r.join('|')", "t"),
                "status is a writable string");
        f.scripts.evaluate("window.seen = []; window.onerror = function (m, src, line, col, err) { seen.push('onerror:' + m + ':' + (err === window.err)); return true; };"
                + "window.addEventListener('error', function (e) { seen.push('event:' + (e.error === window.err)); });"
                + "window.err = new Error('boom'); reportError(err);", "t");
        Check.equal("true", f.scripts.evaluate("String(seen.length >= 1 && seen[0].indexOf('boom') > 0)", "t"), "onerror got the message");
        Check.equal("TypeError", f.scripts.evaluate("try { reportError() } catch (e) { e.name }", "t"), "the argument is required");
    }

    /**
     * <code>requestIdleCallback</code>: a deadline object, the order, cancellation and the argument check.
     * <p>
     * <code>requestIdleCallback</code>: 마감 객체, 순서, 취소, 인수 검사.
     */
    public void testIdleCallbacks() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        f.scripts.evaluate("window.log = []; var a = requestIdleCallback(function (d) { log.push('a:' + d.didTimeout + ':' + (d.timeRemaining() <= 50)); });"
                + "var b = requestIdleCallback(function () { log.push('b'); }); var c = requestIdleCallback(function () { log.push('c'); }); cancelIdleCallback(b);"
                + "window.ids = [typeof a, a !== b].join();", "t");
        Check.equal("0", f.scripts.evaluate("String(log.length)", "t"), "the callbacks run later, as tasks");
        f.advance(5);
        Check.equal("a:false:true|c", f.scripts.evaluate("log.join('|')", "t"), "registration order, cancelled one skipped");
        Check.equal("number,true", f.scripts.evaluate("ids", "t"), "ids are numbers");
        Check.equal("TypeError", f.scripts.evaluate("try { requestIdleCallback(1) } catch (e) { e.name }", "t"), "the callback must be a function");
    }

    /**
     * <code>pageshow</code> is fired at the window after <code>load</code> with <code>persisted</code> false.
     * <p>
     * <code>pageshow</code> 는 <code>load</code> 뒤에 <code>persisted</code> false 로 window 에 발생한다.
     */
    public void testPageShowAfterLoad() {
        PageFixture f = new PageFixture().load("<script>window.order = []; addEventListener('load', function () { order.push('load'); }); "
                + "addEventListener('pageshow', function (e) { order.push([e.type, e.persisted, e.bubbles, e instanceof PageTransitionEvent].join()); });</script><p>x</p>");
        f.settle();
        Check.equal("load|pageshow,false,false,true", f.scripts.evaluate("order.join('|')", "t"), "load first, then pageshow");
    }

    /**
     * The additions of <code>screen</code>.
     * <p>
     * <code>screen</code> 의 추가 멤버.
     */
    public void testScreenAdditions() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("800,600,0,0,landscape-primary,0", f.scripts.evaluate("[screen.availWidth, screen.availHeight, screen.availLeft, screen.availTop, "
                + "screen.orientation.type, screen.orientation.angle].join()", "t"), "available area and orientation");
        f.env.viewportWidth = 400;
        Check.equal("portrait-primary,400", f.scripts.evaluate("[screen.orientation.type, screen.availWidth].join()", "t"), "follows the viewport");
    }
}
