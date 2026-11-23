package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.dom.DomMutationListener;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests <code>IntersectionObserver</code> and <code>ResizeObserver</code> (Intersection Observer, W3C Working Draft 18 October 2023, and Resize Observer
 * Module Level 1, both fetched 2026-11-17; see <code>docs/progress/webapi2.md</code>) on a page whose layout hook is the test: the sizes of the elements are
 * registered by id in {@link PageFixture.Env#bounds} (the fixture has no layout), the rendering step is driven by the manual scheduler of the fixture. The real
 * component with real layout is tested by <code>org.duckdns.hjow.swingwebpanel.core.RenderObserversIntegrationTest</code>.
 * <p>
 * <code>IntersectionObserver</code> 와 <code>ResizeObserver</code> 를 시험한다(Intersection Observer W3C Working Draft 2023-10-18 과 Resize Observer Module
 * Level 1, 모두 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조). 레이아웃 훅이 시험인 페이지에서 한다: 요소의 크기는 {@link PageFixture.Env#bounds} 에
 * id 별로 등록하고(시험 틀에는 레이아웃이 없다) 렌더링 단계는 시험 틀의 수동 스케줄러가 구동한다. 실제 레이아웃이 있는 실제 컴포넌트는
 * <code>org.duckdns.hjow.swingwebpanel.core.RenderObserversIntegrationTest</code> 가 시험한다.
 */
public class RenderObserversTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public RenderObserversTest() {
    }

    /**
     * Loads a page and gives it a style engine, so that computed styles of the inline styles exist.
     * <p>
     * 페이지를 읽고 스타일 엔진을 주어 인라인 스타일의 계산 스타일이 있게 한다.
     *
     * @param html the body of the page / 페이지의 본문
     * @return the fixture / 시험 틀
     */
    private static PageFixture load(String html) {
        PageFixture f = new PageFixture().load("<div id=a></div>" + html + "<script>window.log = [];</" + "script>");
        CssEngine engine = new CssEngine(f.document, new StyleSheetLoader() {
            /**
             * No external style sheets in this test.
             * <p>
             * 이 시험에는 외부 스타일시트가 없다.
             *
             * @param url the URL / URL
             * @return never / 반환하지 않음
             * @throws java.io.IOException always / 항상
             */
            public LoadedStyleSheet load(String url) throws java.io.IOException {
                throw new java.io.IOException("none");
            }
        }, new MediaEnvironment());
        engine.loadStyleSheets();
        f.env.resolver = engine.getResolver();
        f.env.scheduler.advance(10);
        return f;
    }

    /**
     * Runs the rendering step: tells the page that something changed and lets the manual scheduler run the step that is due.
     * <p>
     * 렌더링 단계를 실행한다: 페이지에 무언가 바뀌었음을 알리고 수동 스케줄러가 기한이 된 단계를 실행하게 한다.
     *
     * @param f the page / 페이지
     */
    private static void step(PageFixture f) {
        f.scripts.layoutPublished();
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
    }

    /**
     * Returns the log of the page.
     * <p>
     * 페이지의 기록을 반환한다.
     *
     * @param f the page / 페이지
     * @return the log joined with vertical bars / 세로줄로 이은 기록
     */
    private static String log(PageFixture f) {
        return f.scripts.evaluate("log.join('|')", "t");
    }

    /**
     * Returns the name of the error that a script throws.
     * <p>
     * 스크립트가 던지는 오류의 이름을 반환한다.
     *
     * @param f    the page / 페이지
     * @param code the code that throws / 던지는 코드
     * @return the error name, or <code>none</code> / 오류 이름. 없으면 <code>none</code>
     */
    private static String error(PageFixture f, String code) {
        return f.scripts.evaluate("try { " + code + "; 'none' } catch (e) { e.name }", "t");
    }

    /**
     * The interfaces exist with the right names, cannot be called as functions, and the entry interfaces of the resize observer cannot be constructed.
     * <p>
     * 인터페이스가 올바른 이름으로 있고 함수로 부를 수 없으며 크기 관찰자의 항목 인터페이스는 만들 수 없다.
     */
    public void testInterfaces() {
        PageFixture f = load("");
        Check.equal("function|function|function|function|function", f.scripts.evaluate("[typeof IntersectionObserver, typeof IntersectionObserverEntry, "
                + "typeof ResizeObserver, typeof ResizeObserverEntry, typeof ResizeObserverSize].join('|')", "t"), "the interfaces exist");
        Check.equal("[object IntersectionObserver]|[object ResizeObserver]|[object IntersectionObserverEntry]", f.scripts.evaluate(
                "[Object.prototype.toString.call(new IntersectionObserver(function () {})), Object.prototype.toString.call(new ResizeObserver(function () {})), "
                + "Object.prototype.toString.call(new IntersectionObserverEntry({ time: 1, rootBounds: null, boundingClientRect: {}, intersectionRect: {}, "
                + "isIntersecting: false, intersectionRatio: 0, target: document.body }))].join('|')", "t"), "the string tags");
        Check.equal("TypeError|TypeError|TypeError|TypeError", f.scripts.evaluate("[IntersectionObserver, ResizeObserver, ResizeObserverEntry, ResizeObserverSize]"
                + ".map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"), "none can be called without new");
        Check.equal("TypeError|TypeError|TypeError", f.scripts.evaluate("[function () { new IntersectionObserver(); }, function () { new ResizeObserver(1); }, "
                + "function () { new ResizeObserverEntry(); }].map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"),
                "the callback is required and the entry cannot be constructed");
        Check.equal("true|true|true", f.scripts.evaluate("var io = new IntersectionObserver(function () {}), ro = new ResizeObserver(function () {}); "
                + "[io instanceof IntersectionObserver, ro instanceof ResizeObserver, io.observe === IntersectionObserver.prototype.observe].join('|')", "t"),
                "instances and prototypes");
    }

    /**
     * The options of the intersection observer: the defaults, the serialization of the margins, sorted and frozen thresholds, and the errors of bad options.
     * <p>
     * 교차 관찰자의 옵션: 기본값, 여백의 직렬화, 정렬되고 얼린 임계값, 잘못된 옵션의 오류.
     */
    public void testIntersectionOptions() {
        PageFixture f = load("");
        Check.equal("|0px 0px 0px 0px|0px 0px 0px 0px|[0]|true", f.scripts.evaluate("var o = new IntersectionObserver(function () {}); "
                + "[o.root, o.rootMargin, o.scrollMargin, JSON.stringify(o.thresholds), Object.isFrozen(o.thresholds)].join('|')", "t"), "the defaults");
        Check.equal("10px 20% 10px 20%|1px 2px 3px 2px|1px 2px 3px 4px|0px 0px 0px 0px|96px 0px 0px 0px", f.scripts.evaluate("[new IntersectionObserver(function () {}, "
                + "{ rootMargin: '10px 20%' }).rootMargin, new IntersectionObserver(function () {}, { rootMargin: '1px 2px 3px' }).rootMargin, "
                + "new IntersectionObserver(function () {}, { rootMargin: ' 1px  2px 3px   4px ' }).rootMargin, "
                + "new IntersectionObserver(function () {}, { rootMargin: '' }).rootMargin, new IntersectionObserver(function () {}, { rootMargin: '1in 0px 0px 0px' }).rootMargin"
                + "].join('|')", "t"), "the margins are serialized in pixels and percentages, the CSS way");
        Check.equal("[0,0.25,0.5,1]|[0.5]|[0]", f.scripts.evaluate("[JSON.stringify(new IntersectionObserver(function () {}, { threshold: [1, 0.5, 0, 0.25] }).thresholds), "
                + "JSON.stringify(new IntersectionObserver(function () {}, { threshold: 0.5 }).thresholds), "
                + "JSON.stringify(new IntersectionObserver(function () {}, { threshold: [] }).thresholds)].join('|')", "t"), "the thresholds are sorted, one value is a list, empty means 0");
        Check.equal("SyntaxError|SyntaxError|SyntaxError|SyntaxError|SyntaxError", f.scripts.evaluate("['5', '1px 2px 3px 4px 5px', '1em', 'px', '10px abc']"
                + ".map(function (m) { try { new IntersectionObserver(function () {}, { rootMargin: m }); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"),
                "a margin without a unit, with too many values or with another unit is a SyntaxError");
        Check.equal("SyntaxError", error(f, "new IntersectionObserver(function () {}, { scrollMargin: 'x' })"), "the same for the scroll margin");
        Check.equal("RangeError|RangeError|TypeError|TypeError", f.scripts.evaluate("[{ threshold: -0.1 }, { threshold: [0, 1.5] }, { threshold: NaN }, { root: 5 }]"
                + ".map(function (o) { try { new IntersectionObserver(function () {}, o); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"),
                "thresholds outside 0..1 are a RangeError, bad values and roots a TypeError");
        Check.equal("true|true", f.scripts.evaluate("[new IntersectionObserver(function () {}, { root: document.body }).root === document.body, "
                + "new IntersectionObserver(function () {}, { root: document }).root === document].join('|')", "t"), "a root can be an element or a document");
        Check.equal("TypeError|TypeError", f.scripts.evaluate("var o = new IntersectionObserver(function () {}); [function () { o.observe({}); }, "
                + "function () { o.unobserve(null); }].map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"), "targets are elements");
    }

    /**
     * An observed element reports first when the step runs: a visible one as intersecting with all its rectangles, an element without a box as not intersecting
     * with zero rectangles; a callback gets the entries and the observer, with the observer as <code>this</code>.
     * <p>
     * 관찰하는 요소는 단계가 실행될 때 처음 보고한다: 보이는 요소는 모든 사각형과 함께 교차로, 상자가 없는 요소는 사각형 0 으로 교차하지 않음으로. 콜백은 항목과 관찰자를 받고
     * <code>this</code> 는 관찰자이다.
     */
    public void testInitialEntries() {
        PageFixture f = load("<div id=b></div>");
        f.env.bounds.put("a", new double[] { 10, 20, 100, 50 });
        f.scripts.evaluate("var seen = []; var io = new IntersectionObserver(function (entries, observer) { "
                + "seen.push(this === io && observer === io && Array.isArray(entries)); entries.forEach(function (e) { "
                + "var r = e.boundingClientRect, i = e.intersectionRect, b = e.rootBounds; log.push(e.target.id + ':' + e.isIntersecting + ':' + e.intersectionRatio + ':' "
                + "+ [r.x, r.y, r.width, r.height].join(',') + ':' + [i.x, i.y, i.width, i.height].join(',') + ':' + (b ? [b.x, b.y, b.width, b.height].join(',') : null) "
                + "+ ':' + (e instanceof IntersectionObserverEntry) + ':' + (typeof e.time)); }); }); io.observe(document.getElementById('a')); "
                + "io.observe(document.getElementById('b')); io.observe(document.getElementById('a'));", "t");
        Check.equal("", log(f), "nothing before the rendering step");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("a:true:1:10,20,100,50:10,20,100,50:0,0,800,600:true:number|b:false:0:0,0,0,0:0,0,0,0:0,0,800,600:true:number", log(f),
                "the entries are in the order of observe(), a target that is observed twice reports once, and a box-less target is not intersecting");
        Check.equal("true", f.scripts.evaluate("seen.join('|')", "t"), "this and the second argument are the observer");
        step(f);
        Check.equal("2", f.scripts.evaluate("String(log.length)", "t"), "no new entry for an unchanged page");
    }

    /**
     * Thresholds, partial visibility and the entries that follow a change; a target at the edge of the viewport touches it (intersecting with ratio 0), one beyond
     * the edge does not.
     * <p>
     * 임계값, 부분 가시성과 변화에 이어지는 항목. 뷰포트 가장자리의 대상은 닿는다(교차, 비율 0). 가장자리 너머의 대상은 닿지 않는다.
     */
    public void testThresholdsAndEdges() {
        PageFixture f = load("");
        f.env.bounds.put("a", new double[] { 0, -50, 100, 100 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { "
                + "log.push(e.isIntersecting + ':' + e.intersectionRatio); }); }, { threshold: [0, 0.5, 1] }); io.observe(document.getElementById('a'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("true:0.5", log(f), "half visible: 0.5 reaches the threshold 0.5");
        f.env.bounds.put("a", new double[] { 0, -100, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0", log(f), "the bottom edge touches the top of the viewport: intersecting without area");
        f.env.bounds.put("a", new double[] { 0, -101, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0|false:0", log(f), "one pixel further it does not intersect any more");
        f.env.bounds.put("a", new double[] { 0, -150, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0|false:0", log(f), "no entry while the state stays the same");
        f.env.bounds.put("a", new double[] { 0, 100, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0|false:0|true:1", log(f), "fully visible: ratio 1");
        f.env.bounds.put("a", new double[] { 0, 550, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0|false:0|true:1|true:0.5", log(f), "half way out at the bottom");
        f.env.bounds.put("a", new double[] { 0, 560, 100, 100 });
        step(f);
        Check.equal("true:0.5|true:0|false:0|true:1|true:0.5|true:0.4", log(f), "a ratio of 0.4 is below the threshold 0.5 now, so an entry follows");
    }

    /**
     * A zero-area target reports a ratio of 1 while it intersects (and 0 when it does not).
     * <p>
     * 면적이 0 인 대상은 교차하는 동안 비율 1 을 보고한다(교차하지 않으면 0).
     */
    public void testZeroAreaTarget() {
        PageFixture f = load("<div id=z></div>");
        f.env.bounds.put("a", new double[] { 5, 5, 0, 10 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { log.push(e.isIntersecting + ':' + e.intersectionRatio); }); }); "
                + "io.observe(document.getElementById('a'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("true:1", log(f), "a target with no width inside the viewport: ratio 1");
        f.env.bounds.put("a", new double[] { 5, 700, 0, 10 });
        step(f);
        Check.equal("true:1|false:0", log(f), "outside: ratio 0");
    }

    /**
     * <code>rootMargin</code> grows or shrinks the root rectangle (percentages refer to its width) and <code>rootBounds</code> reports the result.
     * <p>
     * <code>rootMargin</code> 은 root 사각형을 키우거나 줄이고(백분율은 폭에 대한 비율) <code>rootBounds</code> 가 그 결과를 보고한다.
     */
    public void testRootMargin() {
        PageFixture f = load("");
        f.env.bounds.put("a", new double[] { 10, 650, 100, 50 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { var b = e.rootBounds; "
                + "log.push(e.isIntersecting + ':' + [b.x, b.y, b.width, b.height].join(',')); }); }, { rootMargin: '100px 10%' }); "
                + "io.observe(document.getElementById('a'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("true:-80,-100,960,800", log(f), "100px at top and bottom, 10% of 800 = 80px at left and right");
        f.scripts.evaluate("var shrink = new IntersectionObserver(function (entries) { entries.forEach(function (e) { log.push('s:' + e.isIntersecting); }); }, "
                + "{ rootMargin: '-100px' }); f2 = document.getElementById('a'); shrink.observe(f2);", "t");
        f.env.bounds.put("a", new double[] { 10, 550, 100, 50 });
        step(f);
        Check.equal("true:-80,-100,960,800|s:false", log(f), "a negative margin shrinks the root: the target at y 550 is no longer inside (the first observer is unchanged)");
    }

    /**
     * A target that was removed from observation or whose observer was disconnected reports nothing; <code>takeRecords()</code> returns the queued entries and
     * empties the queue (the entries that it takes are not delivered).
     * <p>
     * 관찰에서 빠진 대상이나 연결이 끊긴 관찰자는 아무것도 보고하지 않는다. <code>takeRecords()</code> 는 대기 중인 항목을 반환하고 대기열을 비운다(가져간 항목은 전달되지 않는다).
     */
    public void testUnobserveAndDisconnect() {
        PageFixture f = load("<div id=b></div>");
        f.env.bounds.put("a", new double[] { 0, 0, 10, 10 });
        f.env.bounds.put("b", new double[] { 0, 20, 10, 10 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { log.push(e.target.id + ':' + e.isIntersecting); }); }); "
                + "io.observe(document.getElementById('a')); io.observe(document.getElementById('b')); io.unobserve(document.getElementById('b'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("a:true", log(f), "the target that was unobserved before the step reports nothing");
        f.env.bounds.put("a", new double[] { 0, 700, 10, 10 });
        f.scripts.evaluate("io.disconnect();", "t");
        step(f);
        Check.equal("a:true", log(f), "after disconnect() nothing is reported");
        f.scripts.evaluate("io.observe(document.getElementById('a'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("a:true|a:false", log(f), "an observer can observe again: the first entry is the initial one");
        Check.equal("[]", f.scripts.evaluate("JSON.stringify(io.takeRecords())", "t"), "nothing queued");
    }

    /**
     * An element that clips its content (<code>overflow</code> other than <code>visible</code>) clips the visible part of its descendants: the ratio follows the
     * clipped rectangle, an absolutely positioned target skips the clip of an ancestor that is not its containing block, a fixed one skips all.
     * <p>
     * 내용을 자르는 요소(<code>overflow</code> 가 <code>visible</code> 이 아님)는 후손의 보이는 부분을 자른다: 비율은 잘린 사각형을 따르고, 절대 위치 대상은 포함 블록이
     * 아닌 조상의 자르기를 건너뛰며 fixed 대상은 모두 건너뛴다.
     */
    public void testClippingAncestors() {
        PageFixture f = load("<div id=c style='overflow:hidden;border:5px solid'><div id=t></div><div id=p style='position:absolute'></div>"
                + "<div id=x style='position:fixed'></div></div>");
        // c 는 border box 100x100 이므로 padding box 는 (5,5,90,90) 이다.
        f.env.bounds.put("c", new double[] { 0, 0, 100, 100 });
        f.env.bounds.put("t", new double[] { 5, 5, 90, 180 });
        f.env.bounds.put("p", new double[] { 5, 5, 90, 180 });
        f.env.bounds.put("x", new double[] { 5, 5, 90, 180 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { log.push(e.target.id + ':' + e.intersectionRatio + ':' "
                + "+ e.intersectionRect.height); }); }); ['t', 'p', 'x'].forEach(function (id) { io.observe(document.getElementById(id)); });", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        // t 는 c 의 padding box(높이 90)로 잘려 절반이 보인다. p 의 포함 블록은 문서 루트이므로 c 가 자르지 않는다(높이 180 전체). x 는 fixed 이다.
        Check.equal("t:0.5:90|p:1:180|x:1:180", log(f), "the in-flow target is clipped by the overflow:hidden ancestor, the others are not");
    }

    /**
     * An explicit root: a target outside the root does not intersect, one inside is measured against the padding box of the root when it clips, and the root
     * of the document is the viewport.
     * <p>
     * 명시적 root: root 밖의 대상은 교차하지 않고, root 안의 대상은 root 가 자르면 root 의 padding box 로 잰다. 문서를 root 로 주면 뷰포트이다.
     */
    public void testExplicitRoot() {
        PageFixture f = load("<div id=r style='overflow:auto;border:10px solid'><div id=in></div></div><div id=out></div>");
        f.env.bounds.put("r", new double[] { 100, 100, 220, 220 });
        f.env.bounds.put("in", new double[] { 110, 110, 100, 100 });
        f.env.bounds.put("out", new double[] { 110, 110, 100, 100 });
        f.scripts.evaluate("var io = new IntersectionObserver(function (entries) { entries.forEach(function (e) { var b = e.rootBounds; "
                + "log.push(e.target.id + ':' + e.isIntersecting + ':' + e.intersectionRatio + ':' + [b.x, b.y, b.width, b.height].join(',')); }); }, "
                + "{ root: document.getElementById('r'), threshold: [0, 0.5, 1] }); io.observe(document.getElementById('in')); io.observe(document.getElementById('out')); "
                + "var dio = new IntersectionObserver(function (entries) { entries.forEach(function (e) { log.push('d:' + e.isIntersecting); }); }, { root: document }); "
                + "dio.observe(document.getElementById('out'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("in:true:1:110,110,200,200|out:false:0:110,110,200,200|d:true", log(f),
                "the root rectangle is the padding box of the clipping root; a target that is not inside the root does not intersect");
        f.env.bounds.put("in", new double[] { 110, 110, 100, 300 });
        step(f);
        Check.contains(log(f), "in:true:0.", "a taller target is clipped by the root: a ratio below 1");
    }

    /**
     * The entry interface can be constructed with an init dictionary and reads it back; a missing member is a TypeError.
     * <p>
     * 항목 인터페이스는 초기화 사전으로 만들 수 있고 그것을 되읽는다. 빠진 멤버는 TypeError 이다.
     */
    public void testEntryConstructor() {
        PageFixture f = load("");
        Check.equal("5|true|0.5|4,3,2,1|body||false", f.scripts.evaluate("var e = new IntersectionObserverEntry({ time: 5, rootBounds: null, "
                + "boundingClientRect: { x: 4, y: 3, width: 2, height: 1 }, intersectionRect: { x: 0, y: 0, width: 0, height: 0 }, isIntersecting: true, "
                + "intersectionRatio: 0.5, target: document.body }); var r = e.boundingClientRect; [e.time, e.isIntersecting, e.intersectionRatio, "
                + "[r.x, r.y, r.width, r.height].join(','), e.target.localName, e.rootBounds, Object.keys(e).length > 0].join('|')", "t"),
                "the attributes read what was given, and they are getters of the prototype, not own properties");
        Check.equal("TypeError|TypeError", f.scripts.evaluate("[function () { new IntersectionObserverEntry({}); }, function () { new IntersectionObserverEntry(); }]"
                + ".map(function (c) { try { c(); return 'none'; } catch (e) { return e.name; } }).join('|')", "t"), "the dictionary is required");
    }

    /**
     * A page of blocks with padding and borders reports all three boxes of the first observation; the content rectangle is placed by the padding.
     * <p>
     * 패딩과 테두리가 있는 블록은 첫 관찰에서 세 상자를 모두 보고하고 content 사각형은 패딩이 위치를 정한다.
     */
    public void testResizeInitialEntry() {
        PageFixture f = load("<div id=b style='padding:5px 10px;border:2px solid'></div>");
        f.env.bounds.put("b", new double[] { 30, 40, 100, 50 });
        f.scripts.evaluate("var seen = []; var ro = new ResizeObserver(function (entries, observer) { seen.push(this === ro && observer === ro); entries.forEach(function (e) { "
                + "var c = e.contentRect, bb = e.borderBoxSize, cb = e.contentBoxSize, dp = e.devicePixelContentBoxSize; "
                + "log.push(e.target.id + ':' + [c.x, c.y, c.width, c.height].join(',') + ':' + bb.length + ',' + bb[0].inlineSize + 'x' + bb[0].blockSize + ':' "
                + "+ cb[0].inlineSize + 'x' + cb[0].blockSize + ':' + dp[0].inlineSize + 'x' + dp[0].blockSize + ':' + (e instanceof ResizeObserverEntry) + ':' "
                + "+ (bb[0] instanceof ResizeObserverSize) + ':' + Object.isFrozen(bb)); }); }); ro.observe(document.getElementById('b'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("b:10,5,76,36:1,100x50:76x36:76x36:true:true:true", log(f),
                "content = border box minus 2 x 10 + 2 x 2 wide and 2 x 5 + 2 x 2 high, placed at the padding; one size per sequence");
        Check.equal("true", f.scripts.evaluate("seen.join('|')", "t"), "this and the second argument are the observer");
        step(f);
        Check.equal("1", f.scripts.evaluate("String(log.length)", "t"), "an unchanged element reports once");
    }

    /**
     * An element that is not rendered, or is a non-replaced inline element, never reports (its size stays 0 x 0); the observation reports when it gets a size.
     * <p>
     * 렌더링되지 않거나 치환되지 않은 인라인 요소는 보고하지 않는다(크기가 0 x 0 으로 남는다). 크기가 생기면 보고한다.
     */
    public void testResizeEmptyBoxes() {
        PageFixture f = load("<span id=s></span><img id=i alt=''><div id=n style='display:none'></div>");
        f.env.bounds.put("s", new double[] { 0, 0, 50, 20 });
        f.env.bounds.put("i", new double[] { 0, 0, 30, 30 });
        f.scripts.evaluate("var ro = new ResizeObserver(function (entries) { entries.forEach(function (e) { log.push(e.target.id + ':' + e.contentBoxSize[0].inlineSize); }); }); "
                + "['a', 's', 'i', 'n'].forEach(function (id) { ro.observe(document.getElementById(id)); });", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("i:30", log(f), "only the replaced element reports: the others are not rendered, or are inline and not replaced");
        f.env.bounds.put("a", new double[] { 0, 0, 10, 10 });
        step(f);
        Check.equal("i:30|a:10", log(f), "an element that gets a size reports it");
    }

    /**
     * A change of the observed box reports once; a change of another box does not: the content box ignores a change of the border only when the content stays,
     * the border box ignores a change of the padding; the box of <code>observe</code> can be replaced by observing again.
     * <p>
     * 관찰하는 상자가 바뀌면 한 번 보고하고 다른 상자의 변화는 보고하지 않는다. border 상자를 관찰하면 padding 변화는 무시한다. <code>observe</code> 로 다시 관찰하면 상자를 바꿀 수 있다.
     */
    public void testResizeObservedBoxes() {
        PageFixture f = load("<div id=b style='padding:5px'></div>");
        f.env.bounds.put("b", new double[] { 0, 0, 100, 50 });
        f.scripts.evaluate("var ro = new ResizeObserver(function (entries) { entries.forEach(function (e) { log.push(e.contentBoxSize[0].inlineSize + 'x' + e.borderBoxSize[0].inlineSize); }); }); "
                + "ro.observe(document.getElementById('b'), { box: 'border-box' });", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("90x100", log(f), "the first observation");
        // 테두리 상자가 그대로인 채 padding 만 바뀌면 border-box 관찰은 보고하지 않는다.
        f.scripts.evaluate("document.getElementById('b').style.padding = '10px';", "t");
        step(f);
        Check.equal("90x100", log(f), "the border box did not change: no report");
        f.env.bounds.put("b", new double[] { 0, 0, 120, 50 });
        step(f);
        Check.equal("90x100|100x120", log(f), "the border box changed (the content too, with the new padding)");
        // 같은 대상을 다시 관찰하면 새 관찰이 되어 현재 크기를 처음처럼 보고한다.
        f.scripts.evaluate("ro.observe(document.getElementById('b'), { box: 'content-box' });", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("90x100|100x120|100x120", log(f), "observing again starts a new observation that reports the size once more");
        Check.equal("TypeError", error(f, "ro.observe(document.getElementById('b'), { box: 'nonsense' })"), "an unknown box is a TypeError");
        Check.equal("TypeError", error(f, "ro.observe({})"), "the target is an element");
    }

    /**
     * <code>unobserve</code> and <code>disconnect</code> stop the reports, also those that are due.
     * <p>
     * <code>unobserve</code> 와 <code>disconnect</code> 는 보고를 멈춘다. 예정된 보고도 마찬가지이다.
     */
    public void testResizeUnobserveAndDisconnect() {
        PageFixture f = load("<div id=b></div>");
        f.env.bounds.put("a", new double[] { 0, 0, 10, 10 });
        f.env.bounds.put("b", new double[] { 0, 0, 20, 20 });
        f.scripts.evaluate("var ro = new ResizeObserver(function (entries) { entries.forEach(function (e) { log.push(e.target.id + ':' + e.contentBoxSize[0].inlineSize); }); }); "
                + "ro.observe(document.getElementById('a')); ro.observe(document.getElementById('b')); ro.unobserve(document.getElementById('b'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("a:10", log(f), "the target that was unobserved reports nothing");
        f.env.bounds.put("a", new double[] { 0, 0, 30, 10 });
        f.scripts.evaluate("ro.disconnect();", "t");
        step(f);
        Check.equal("a:10", log(f), "after disconnect() nothing is reported");
    }

    /**
     * The vertical writing modes swap the inline size and the block size.
     * <p>
     * 세로쓰기는 인라인 크기와 블록 크기를 바꾼다.
     */
    public void testResizeWritingMode() {
        PageFixture f = load("<div id=v style='writing-mode:vertical-rl'></div>");
        f.env.bounds.put("v", new double[] { 0, 0, 40, 100 });
        f.scripts.evaluate("var ro = new ResizeObserver(function (entries) { entries.forEach(function (e) { var s = e.borderBoxSize[0]; log.push(s.inlineSize + 'x' + s.blockSize + ':' "
                + "+ e.contentRect.width + 'x' + e.contentRect.height); }); }); ro.observe(document.getElementById('v'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("100x40:40x100", log(f), "the inline size is the height; the content rectangle stays physical");
    }

    /**
     * The loop of the resize observers: targets that change during a delivery are delivered again if they are deeper than the shallowest delivered one, those that
     * are not wait and an <code>error</code> event reports the undelivered notifications.
     * <p>
     * 크기 관찰자의 루프: 전달하는 동안 바뀐 대상은 전달된 가장 얕은 것보다 깊으면 다시 전달하고, 아니면 기다리며 <code>error</code> 이벤트가 전달되지 못한 통지를 보고한다.
     */
    public void testResizeLoop() {
        final PageFixture f = load("<div id=p><div id=c></div></div>");
        // 레이아웃을 흉내 낸다: data-w 속성이 바뀌면 그 요소의 폭이 그 값이 된다.
        f.document.addMutationListener(new DomMutationListener() {
            /**
             * Emulates the layout: <code>data-w</code> is the width of the element.
             * <p>
             * 레이아웃을 흉내 낸다: <code>data-w</code> 가 요소의 폭이다.
             *
             * @param record    the change / 변경
             * @param connected whether the target is in the document / 대상이 문서에 있는지 여부
             */
            public void mutated(MutationRecord record, boolean connected) {
                if (record.getType() == MutationRecord.ATTRIBUTES && "data-w".equals(record.getAttributeName())) {
                    org.duckdns.hjow.swingwebpanel.dom.Element e = (org.duckdns.hjow.swingwebpanel.dom.Element) record.getTarget();
                    f.env.bounds.put(e.getAttribute("id"), new double[] { 0, 0, Double.parseDouble(e.getAttribute("data-w")), 10 });
                }
            }
        });
        f.env.bounds.put("p", new double[] { 0, 0, 100, 10 });
        f.env.bounds.put("c", new double[] { 0, 0, 50, 10 });
        // 콜백이 자식의 폭만 바꾸면 자식이 더 깊으므로 같은 단계 안에서 다시 전달된다.
        f.scripts.evaluate("var n = 0; var ro = new ResizeObserver(function (entries) { n++; log.push('n' + n + ':' + entries.map(function (e) { return e.target.id + '=' + "
                + "e.borderBoxSize[0].inlineSize; }).join(',')); if (n === 1) { document.getElementById('c').setAttribute('data-w', '60'); } }); "
                + "ro.observe(document.getElementById('p')); ro.observe(document.getElementById('c'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("n1:p=100,c=50|n2:c=60", log(f), "the child changed in the callback and is delivered again in the same step");
        // 콜백이 부모의 폭을 바꾸면 부모는 더 얕지 않으므로 건너뛰고 오류 이벤트로 보고한다.
        f.scripts.evaluate("var errors = []; window.addEventListener('error', function (e) { errors.push(e.message); }); "
                + "var ro2 = new ResizeObserver(function () { document.getElementById('p').setAttribute('data-w', String(Math.random() * 100 + 200)); }); "
                + "ro2.observe(document.getElementById('c')); ro2.observe(document.getElementById('p'));", "t");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.contains(f.scripts.evaluate("errors.join('|')", "t"), "ResizeObserver loop completed with undelivered notifications.",
                "a change of a shallower target during a delivery is reported as an error event");
    }

    /**
     * Both kinds of observers run in one step, the resize observers first; a change of the document, of the viewport and a new layout each queue a step, and
     * the step is queued once for a burst of changes.
     * <p>
     * 두 종류의 관찰자는 한 단계에서 실행되며 크기 관찰자가 먼저이다. 문서 변경, 뷰포트 변경, 새 레이아웃이 각각 단계를 예약하고 변경이 몰려도 단계는 한 번 예약된다.
     */
    public void testStepsAndTriggers() {
        PageFixture f = load("");
        int base = f.env.scheduler.pending();
        f.env.bounds.put("a", new double[] { 0, 0, 100, 100 });
        f.scripts.evaluate("var io = new IntersectionObserver(function () { log.push('io'); }); var ro = new ResizeObserver(function () { log.push('ro'); }); "
                + "io.observe(document.getElementById('a')); ro.observe(document.getElementById('a'));", "t");
        Check.equal(base + 1, f.env.scheduler.pending(), "observing queued one step, however many targets were added");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("ro|io", log(f), "the resize observers run before the intersection observers");
        Check.equal(base, f.env.scheduler.pending(), "nothing is queued while nothing changes");
        // 문서 변경이 단계를 예약한다.
        f.env.bounds.put("a", new double[] { 0, 0, 100, 200 });
        f.scripts.evaluate("document.getElementById('a').setAttribute('title', 'x'); document.getElementById('a').setAttribute('title', 'y');", "t");
        Check.equal(base + 1, f.env.scheduler.pending(), "a burst of changes queues one step");
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("ro|io|ro", log(f), "the resize observer saw the new height (the intersection observer saw no change of state)");
        // 뷰포트가 바뀌면 단계가 예약된다.
        f.env.bounds.put("a", new double[] { 0, 700, 100, 100 });
        f.env.viewportHeight = 800;
        f.scripts.viewportChanged(true, false);
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal("ro|io|ro|ro", log(f), "a viewport change queues a step: the target at y 700 is inside the taller viewport (state unchanged); its size changed");
        f.env.scheduler.advance(100);
        Check.equal(base, f.env.scheduler.pending(), "idle again");
    }

    /**
     * A disposed page runs no step, and a step that was queued is dropped.
     * <p>
     * 폐기된 페이지는 단계를 실행하지 않고, 예약된 단계는 버려진다.
     */
    public void testDispose() {
        PageFixture f = load("");
        int base = f.env.scheduler.pending();
        f.env.bounds.put("a", new double[] { 0, 0, 100, 100 });
        f.scripts.evaluate("var ro = new ResizeObserver(function () { log.push('ro'); }); ro.observe(document.getElementById('a'));", "t");
        Check.equal(base + 1, f.env.scheduler.pending(), "a step is queued");
        f.scripts.dispose();
        f.env.scheduler.advance(RenderObservers.UPDATE_DELAY_MS + 1);
        Check.equal(base, f.env.scheduler.pending(), "the queued step was cancelled");
    }
}
