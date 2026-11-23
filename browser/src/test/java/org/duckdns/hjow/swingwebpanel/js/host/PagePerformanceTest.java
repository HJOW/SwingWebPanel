package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests <code>window.performance</code> (User Timing Level 3, W3C Working Draft, and Navigation Timing Level 2, both fetched 2026-11-17; see
 * <code>docs/progress/webapi2.md</code>): the interfaces, <code>mark()</code>, <code>measure()</code> with all its forms, the timeline queries and the clearing
 * of entries, the navigation entry with the times of the loading stages that the page host really passed, and the legacy <code>performance.timing</code> and
 * <code>performance.navigation</code>. The clock is the manual scheduler of the fixture. The kind of navigation that the real component reports is tested by
 * <code>org.duckdns.hjow.swingwebpanel.core.PerformanceIntegrationTest</code>.
 * <p>
 * <code>window.performance</code> 를 시험한다(User Timing Level 3 W3C Working Draft 와 Navigation Timing Level 2, 모두 2026-11-17 조회, <code>docs/progress/webapi2.md</code>
 * 참조): 인터페이스, <code>mark()</code>, 모든 형태의 <code>measure()</code>, 타임라인 조회와 항목 지우기, 페이지 호스트가 실제로 지난 로딩 단계의 시각을 가진 탐색 항목,
 * 옛 <code>performance.timing</code> 과 <code>performance.navigation</code>. 시계는 시험 틀의 수동 스케줄러이다. 실제 컴포넌트가 알리는 이동의 종류는
 * <code>org.duckdns.hjow.swingwebpanel.core.PerformanceIntegrationTest</code> 가 시험한다.
 */
public class PagePerformanceTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PagePerformanceTest() {
    }

    /**
     * Loads a page on a manual clock that does not start at 0 (a stage that was reached at the time 0 would look like one that was never reached).
     * <p>
     * 0 에서 시작하지 않는 수동 시계로 페이지를 읽는다(시각 0 에 이른 단계는 이르지 못한 단계처럼 보이기 때문이다).
     *
     * @return the fixture / 시험 틀
     */
    private static PageFixture load() {
        PageFixture f = new PageFixture();
        f.env.scheduler.advance(10000);
        f.load("<div id=a></div>");
        f.env.scheduler.advance(10);
        return f;
    }

    /**
     * Returns the value of an expression of the page.
     * <p>
     * 페이지의 식의 값을 반환한다.
     *
     * @param f    the page / 페이지
     * @param code the expression / 식
     * @return the value as text / 문자열 값
     */
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * The interfaces exist with the right names, the entry interfaces cannot be constructed except <code>PerformanceMark</code>, and <code>performance</code> is a
     * <code>Performance</code>.
     * <p>
     * 인터페이스가 올바른 이름으로 있고 <code>PerformanceMark</code> 외의 항목 인터페이스는 만들 수 없으며 <code>performance</code> 는 <code>Performance</code> 이다.
     */
    public void testInterfaces() {
        PageFixture f = load();
        Check.equal("function|function|function|function|function|function|function", js(f, "[typeof Performance, typeof PerformanceEntry, typeof PerformanceMark, "
                + "typeof PerformanceMeasure, typeof PerformanceNavigationTiming, typeof PerformanceTiming, typeof PerformanceNavigation].join('|')"), "the interfaces exist");
        Check.equal("true|true|[object Performance]|[object PerformanceMark]", js(f, "var m = performance.mark('a'); [performance instanceof Performance, m instanceof PerformanceEntry, "
                + "Object.prototype.toString.call(performance), Object.prototype.toString.call(m)].join('|')"), "performance is a Performance; a mark is a PerformanceEntry");
        Check.equal("TypeError|TypeError|TypeError|TypeError", js(f, "[function () { new PerformanceEntry(); }, function () { new PerformanceMeasure(); }, "
                + "function () { new PerformanceNavigationTiming(); }, function () { new Performance(); }].map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')"),
                "only PerformanceMark can be constructed by the page");
        Check.equal("true|0|1|2", js(f, "[typeof performance.timing === 'object' && performance.timing instanceof PerformanceTiming, PerformanceNavigation.TYPE_NAVIGATE, "
                + "PerformanceNavigation.TYPE_RELOAD, PerformanceNavigation.TYPE_BACK_FORWARD].join('|')"), "the legacy objects");
    }

    /**
     * <code>performance.now()</code> counts milliseconds from the time origin, which is the start of the page in milliseconds since the epoch.
     * <p>
     * <code>performance.now()</code> 는 시간 원점부터의 밀리초를 센다. 시간 원점은 에포크 이후 밀리초로 나타낸 페이지의 시작이다.
     */
    public void testNowAndTimeOrigin() {
        PageFixture f = load();
        Check.equal("true|true", js(f, "[performance.timeOrigin >= 10000, performance.timeOrigin === performance.timing.navigationStart].join('|')"),
                "the time origin is the start of the page on the scheduler clock, in milliseconds since the epoch");
        double before = Double.parseDouble(js(f, "String(performance.now())"));
        f.env.scheduler.advance(250);
        double after = Double.parseDouble(js(f, "String(performance.now())"));
        Check.equal(250, Math.round(after - before), "now() follows the clock");
    }

    /**
     * <code>mark()</code>: a mark at the current time or at <code>startTime</code>, the cloned <code>detail</code>, the errors, and a constructed mark that does not
     * enter the timeline.
     * <p>
     * <code>mark()</code>: 현재 시각이나 <code>startTime</code> 의 마크, 복사된 <code>detail</code>, 오류, 타임라인에 들어가지 않는 생성한 마크.
     */
    public void testMark() {
        PageFixture f = load();
        f.env.scheduler.advance(100);
        Check.equal("a|mark|110|0|null", js(f, "var m = performance.mark('a'); [m.name, m.entryType, m.startTime, m.duration, String(m.detail)].join('|')"),
                "a mark at the current time with no detail");
        Check.equal("b|5|1|false", js(f, "var d = { x: 1 }; var m2 = performance.mark('b', { startTime: 5, detail: d }); "
                + "[m2.name, m2.startTime, m2.detail.x, m2.detail === d].join('|')"), "startTime and a detail that was cloned");
        Check.equal("TypeError|SyntaxError|TypeError|TypeError", js(f, "[function () { performance.mark('c', { startTime: -1 }); }, function () { performance.mark('navigationStart'); }, "
                + "function () { performance.mark(); }, function () { performance.mark('d', { startTime: NaN }); }]"
                + ".map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')"), "a negative or non-finite start, a timing attribute name and a missing name are errors");
        Check.equal("m|7|true|2", js(f, "var c = new PerformanceMark('m', { startTime: 7 }); [c.name, c.startTime, c instanceof PerformanceMark, performance.getEntriesByType('mark').length].join('|')"),
                "a mark that was constructed does not enter the timeline");
        Check.equal("{\"name\":\"a\",\"entryType\":\"mark\",\"startTime\":110,\"duration\":0,\"detail\":null}", js(f, "JSON.stringify(performance.getEntriesByName('a')[0])"),
                "toJSON");
    }

    /**
     * <code>measure()</code> in all its forms: to now, between marks, with start/end/duration options, with the names of timing attributes, and the errors.
     * <p>
     * <code>measure()</code> 의 모든 형태: 지금까지, 마크 사이, start/end/duration 옵션, 타이밍 속성 이름, 오류.
     */
    public void testMeasure() {
        PageFixture f = load();
        js(f, "performance.mark('s', { startTime: 10 }); performance.mark('e', { startTime: 40 }); 0");
        f.env.scheduler.advance(100);
        Check.equal("m1|measure|0|110", js(f, "var m = performance.measure('m1'); [m.name, m.entryType, m.startTime, m.duration].join('|')"), "measure(name): from the time origin to now");
        Check.equal("10|30", js(f, "var m2 = performance.measure('m2', 's', 'e'); [m2.startTime, m2.duration].join('|')"), "between two marks");
        Check.equal("10|100", js(f, "var m3 = performance.measure('m3', 's'); [m3.startTime, m3.duration].join('|')"), "from a mark to now");
        Check.equal("10|30", js(f, "var m4 = performance.measure('m4', { start: 's', end: 'e' }); [m4.startTime, m4.duration].join('|')"), "options with marks");
        Check.equal("10|5", js(f, "var m5 = performance.measure('m5', { start: 10, duration: 5 }); [m5.startTime, m5.duration].join('|')"), "start and duration");
        Check.equal("15|5", js(f, "var m6 = performance.measure('m6', { end: 20, duration: 5 }); [m6.startTime, m6.duration].join('|')"), "end and duration");
        Check.equal("1|detail", js(f, "var m7 = performance.measure('m7', { detail: { v: 'detail' }, start: 1, end: 2 }); [m7.duration, m7.detail.v].join('|')"), "detail of a measure");
        Check.equal("true", js(f, "String(performance.measure('m8', 'navigationStart').startTime === 0)"), "the name of a timing attribute is a point on the timeline");
        Check.equal("SyntaxError|TypeError|TypeError|TypeError", js(f, "[function () { performance.measure('x', 'nothing'); }, function () { performance.measure('x', { start: 1 }, 's'); }, "
                + "function () { performance.measure('x', { start: 1, end: 2, duration: 3 }); }, function () { performance.measure(); }]"
                + ".map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')"), "a mark that does not exist, options with an end mark, all three, no name");
    }

    /**
     * The timeline: the navigation entry first, then the entries by start time (stable), filtered by type and name; <code>clearMarks</code> and
     * <code>clearMeasures</code> by name or all.
     * <p>
     * 타임라인: 탐색 항목이 먼저, 그 다음 시작 시각 순서(안정)의 항목. 종류와 이름으로 거른다. <code>clearMarks</code>, <code>clearMeasures</code> 는 이름별 또는 전부.
     */
    public void testTimelineAndClearing() {
        PageFixture f = load();
        js(f, "performance.mark('late', { startTime: 50 }); performance.mark('early', { startTime: 5 }); performance.mark('early', { startTime: 5 }); "
                + "performance.measure('span', 'early', 'late'); 0");
        Check.equal("navigation:0|mark:5|mark:5|measure:5|mark:50", js(f, "performance.getEntries().map(function (e) { return e.entryType + ':' + e.startTime; }).join('|')"),
                "the navigation entry first, then by start time; equal start times keep the order of creation (marks before the measure)");
        Check.equal("2|1|3|0", js(f, "[performance.getEntriesByName('early').length, performance.getEntriesByType('measure').length, performance.getEntriesByType('mark').length, "
                + "performance.getEntriesByType('resource').length].join('|')"), "queries by name and by type");
        Check.equal("1|0", js(f, "[performance.getEntriesByName('early', 'mark').length - 1, performance.getEntriesByName('early', 'measure').length].join('|')"), "name and type");
        Check.equal("1|0", js(f, "performance.clearMarks('early'); [performance.getEntriesByType('mark').length - 0, performance.getEntriesByName('early').length].join('|')"),
                "clearMarks(name) removes the marks with the name");
        js(f, "performance.clearMarks(); 0");
        Check.equal("0|1", js(f, "[performance.getEntriesByType('mark').length, performance.getEntriesByType('measure').length].join('|')"), "clearMarks() removes all marks, not the measures");
        js(f, "performance.clearMeasures('nothing'); 0");
        Check.equal("1", js(f, "String(performance.getEntriesByType('measure').length)"), "clearMeasures(name) with another name keeps it");
        js(f, "performance.clearMeasures(); 0");
        Check.equal("0", js(f, "String(performance.getEntriesByType('measure').length)"), "clearMeasures() removes all measures");
        Check.equal("1", js(f, "String(performance.getEntries().length)"), "the navigation entry stays");
    }

    /**
     * The navigation entry and the legacy timing report the stages that the page host passed, in order, on the clock, and the times that the page cannot know are 0:
     * nothing is invented.
     * <p>
     * 탐색 항목과 옛 timing 은 페이지 호스트가 지난 단계를 시계에 맞춰 순서대로 알리고, 페이지가 알 수 없는 시각은 0 이다. 지어낸 것은 없다.
     */
    public void testNavigationTimingStages() {
        PageFixture f = load();
        Check.equal("navigation|0|navigate|navigation|0", js(f, "var n = performance.getEntriesByType('navigation')[0]; [n.entryType, n.startTime, n.type, n.initiatorType, n.redirectCount].join('|')"),
                "the navigation entry: type navigate, started at the time origin, no redirects counted");
        Check.equal("true", js(f, "String(n instanceof PerformanceNavigationTiming && n === performance.getEntriesByType('navigation')[0] && n.name === document.URL)"),
                "an instance of the interface, the same object every time, named by the address");
        Check.equal("true", js(f, "String(n.domInteractive <= n.domContentLoadedEventStart && n.domContentLoadedEventStart <= n.domContentLoadedEventEnd && "
                + "n.domContentLoadedEventEnd <= n.domComplete && n.domComplete <= n.loadEventStart && n.loadEventStart <= n.loadEventEnd)"), "the stages are in order");
        Check.equal("0|0|0|0|0|0|0|0", js(f, "[n.fetchStart, n.requestStart, n.responseStart, n.responseEnd, n.connectStart, n.domainLookupStart, n.unloadEventStart, n.transferSize].join('|')"),
                "the times of the network and of the previous document are not known to the page: 0");
        Check.equal("true", js(f, "String(n.duration === n.loadEventEnd)"), "the entry lasts until the end of the load event");
        // 단계마다 시각이 다르도록 시계를 움직인 문서를 만든다: 해석이 끝난 뒤 시계를 올리고 load 가 끝나게 한다.
        PageFixture g = new PageFixture();
        g.env.scheduler.advance(10000);
        g.load("<div id=a></div>");
        Check.equal("0", js(g, "String(performance.timing.domInteractive - performance.timing.navigationStart)"),
                "the legacy domInteractive is on the same clock as the time origin (the manual clock did not move while the page loaded)");
        g.env.scheduler.advance(50);
        Check.equal("true|true|true", js(g, "var t = performance.timing; [t.navigationStart === performance.timeOrigin, t.domLoading === t.navigationStart, "
                + "t.loadEventEnd >= t.domComplete && t.domComplete >= t.domInteractive && t.domInteractive >= t.domLoading].join('|')"), "the legacy times are epoch milliseconds in order");
        Check.equal("0|0|0|0|0", js(g, "[t.fetchStart, t.requestStart, t.responseStart, t.responseEnd, t.unloadEventStart].join('|')"), "the network times of the legacy object are 0");
        Check.equal("0|0|true", js(g, "var nv = performance.navigation; [nv.type, nv.redirectCount, nv instanceof PerformanceNavigation].join('|')"), "performance.navigation");
    }
}
