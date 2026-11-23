package org.duckdns.hjow.swingwebpanel.js.host.anim;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script tests of the Web Animations interface (Web Animations Level 1, P10-08-h) on a page with a manual clock and a real style engine:
 * <code>element.animate()</code> and its return value, the computed style during the animation, both forms of the keyframes argument and the
 * normalized result of <code>getKeyframes()</code>, the timing dictionaries, the TypeErrors and DOMExceptions of invalid arguments and states, the
 * order of promise reactions and playback events, the constructors and timelines, <code>getAnimations()</code>, the removal of replaced animations,
 * <code>commitStyles()</code>, the shared frame with <code>requestAnimationFrame</code> and the end of the animations when the scripts stop.
 * <p>
 * 수동 시계와 실제 스타일 엔진을 가진 페이지에서 Web Animations 인터페이스(Web Animations Level 1, P10-08-h)를 스크립트로 시험한다: <code>element.animate()</code> 와
 * 반환 값, 애니메이션 중의 계산 스타일, 키프레임 인수의 두 형식과 <code>getKeyframes()</code> 의 정규화 결과, 시간 사전, 잘못된 인수와 상태의 TypeError 와
 * DOMException, 프라미스 반응과 재생 이벤트의 순서, 생성자와 타임라인, <code>getAnimations()</code>, 교체된 애니메이션 제거, <code>commitStyles()</code>,
 * <code>requestAnimationFrame</code> 과 공유하는 프레임, 스크립트가 멈출 때 애니메이션의 끝.
 */
public class WebAnimationsApiTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public WebAnimationsApiTest() {
    }

    /** The animation engine of the last page. 마지막 페이지의 애니메이션 엔진. */
    private AnimationEngine engine;
    /** The style engine of the last page. 마지막 페이지의 스타일 엔진. */
    private CssEngine css;

    /**
     * Loads a page and gives it a style engine with an animation engine on the clock of the fixture.
     * <p>
     * 페이지를 로드하고 시험 도구의 시계를 쓰는 애니메이션 엔진을 가진 스타일 엔진을 준다.
     *
     * @param html the page / 페이지
     * @return the fixture / 시험 도구
     */
    private PageFixture page(String html) {
        final PageFixture f = new PageFixture().load(html);
        css = new CssEngine(f.document, null, new MediaEnvironment(800, 600));
        css.loadStyleSheets();
        engine = new AnimationEngine(css, new AnimationClock() {
            /**
             * Returns the clock of the fixture.
             * <p>
             * 시험 도구의 시계를 반환한다.
             *
             * @return milliseconds / 밀리초
             */
            public long nowMillis() {
                return f.env.scheduler.now();
            }
        });
        f.env.cssEngine = css;
        return f;
    }

    /**
     * Evaluates a script.
     * <p>
     * 스크립트를 평가한다.
     *
     * @param f      the fixture / 시험 도구
     * @param script the script / 스크립트
     * @return the completion value / 완료 값
     */
    private static String js(PageFixture f, String script) {
        return f.scripts.evaluate(script, "t");
    }

    /**
     * <code>element.animate()</code> returns a pending running animation of a <code>KeyframeEffect</code> on the element and the document timeline;
     * the next frame resolves <code>ready</code> with the animation and resolves the start time.
     * <p>
     * <code>element.animate()</code> 는 요소에 대한 <code>KeyframeEffect</code> 와 문서 타임라인을 가진 대기 중인 실행 애니메이션을 반환한다. 다음 프레임이
     * <code>ready</code> 를 애니메이션으로 resolve 하고 시작 시간을 정한다.
     */
    public void testAnimateReturnsARunningAnimation() {
        PageFixture f = page("<div id=d></div>");
        Check.equal("true,running,true,true,true,true,true,function,x,", js(f, "var d = document.getElementById('d');"
                + "var a = d.animate([{opacity: 0}, {opacity: 1}], {duration: 1000, id: 'x'});"
                + "a.ready.then(function (v) { console.log('ready ' + (v === a) + ' ' + a.pending); });"
                + "[a instanceof Animation, a.playState, a.pending, a.effect instanceof KeyframeEffect, a.effect instanceof AnimationEffect,"
                + " a.effect.target === d, a.timeline === document.timeline, typeof a.ready.then, a.id, a.startTime].join()"), "the new animation");
        Check.equal("0", js(f, "String(a.currentTime)"), "the current time while pending");
        f.advance(16);
        Check.equal("ready true false", f.log(), "ready resolved at the frame with the animation");
        Check.equal("false|true|0", js(f, "[a.pending, typeof a.startTime === 'number', a.currentTime].join('|')"), "started");
        f.advance(16);
        Check.equal("16", js(f, "String(a.currentTime)"), "the time of the next frame");
        Check.equal("true", js(f, "String(a.startTime + a.currentTime === document.timeline.currentTime)"), "the timeline time");
    }

    /**
     * The computed style follows the animation (<code>getComputedStyle</code> sees the animated value), seeking changes it at once, and
     * <code>cancel()</code> takes it away.
     * <p>
     * 계산 스타일이 애니메이션을 따르고(<code>getComputedStyle</code> 이 애니메이션 값을 본다), 이동하면 즉시 바뀌며 <code>cancel()</code> 은 그것을 없앤다.
     */
    public void testComputedStyleFollowsTheAnimation() {
        PageFixture f = page("<div id=d style='opacity:0.9;margin-left:10px'></div>");
        js(f, "var d = document.getElementById('d'); var a = d.animate({opacity: [0, 1], marginLeft: ['0px', '100px']}, 1000);");
        f.advance(16);
        js(f, "a.currentTime = 500;");
        Check.equal("0.5|50px", js(f, "var cs = getComputedStyle(d); [cs.opacity, cs.marginLeft].join('|')"), "half way");
        Check.near(0.5, css.getComputedStyle(f.document.getElementById("d")).number(Prop.OPACITY), 1e-6, "the style engine sees the same value");
        js(f, "a.cancel();");
        Check.equal("0.9|10px", js(f, "var cs = getComputedStyle(d); [cs.opacity, cs.marginLeft].join('|')"), "the cascade value after cancel");
        Check.equal("idle|null", js(f, "a.playState + '|' + String(a.currentTime)"), "idle");
    }

    /**
     * Both forms of the keyframes argument and the normalized result of <code>getKeyframes()</code>: computed offsets, easings, composite
     * operations, IDL names, serialized values, dropped invalid values and ignored keys that are not animatable IDL names.
     * <p>
     * 키프레임 인수의 두 형식과 <code>getKeyframes()</code> 의 정규화 결과: 계산 오프셋, 완화, 합성 연산, IDL 이름, 직렬화 값, 버려지는 잘못된 값, 애니메이션할 수 있는 IDL
     * 이름이 아닌 키의 무시.
     */
    public void testKeyframeFormsAndGetKeyframes() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d'); function kf(k) { return JSON.stringify(new KeyframeEffect(d, k).getKeyframes()); }");
        Check.equal("[{\"offset\":null,\"computedOffset\":0,\"easing\":\"ease-in\",\"composite\":\"auto\",\"marginLeft\":\"0px\",\"opacity\":\"0\"},"
                + "{\"offset\":null,\"computedOffset\":1,\"easing\":\"ease-in\",\"composite\":\"auto\",\"marginLeft\":\"10px\",\"opacity\":\"1\"}]",
                js(f, "kf({opacity: [0, 1], marginLeft: ['0px', '10px'], easing: 'ease-in'})"), "the property-indexed form");
        Check.equal("0,0.5,0.65,0.8,1", js(f, "new KeyframeEffect(d, [{opacity: 0}, {opacity: 0.1, offset: 0.5}, {opacity: 0.2}, {opacity: 0.3, offset: 0.8},"
                + " {opacity: 1}]).getKeyframes().map(function (k) { return k.computedOffset; }).join()"), "the computed offsets of the array form");
        Check.equal("null,0.5,null,0.8,null", js(f, "new KeyframeEffect(d, {opacity: [0, 0.1, 0.2, 0.3, 1], offset: [null, 0.5, null, 0.8]}).getKeyframes()"
                + ".map(function (k) { return String(k.offset); }).join()"), "offsets of the property-indexed form");
        Check.equal("[{\"offset\":null,\"computedOffset\":1,\"easing\":\"linear\",\"composite\":\"auto\",\"color\":\"red\"}]", js(f, "kf({color: 'red'})"),
                "a single value is the last keyframe");
        Check.equal("[{\"offset\":null,\"computedOffset\":1,\"easing\":\"linear\",\"composite\":\"add\",\"left\":\"5px\"}]",
                js(f, "kf([{opacity: 'abc', left: '5px', composite: 'add'}])"), "an invalid value is dropped");
        Check.equal("--x,backgroundColor,cssFloat", js(f, "Object.keys(new KeyframeEffect(d, [{'background-color': 'red', animationName: 'x', backgroundColor: 'red',"
                + " cssFloat: 'left', float: 'left', '--x': '1px', bogusProperty: '1'}]).getKeyframes()[0]).slice(4).join()"),
                "only animatable IDL names (and custom properties) are kept");
        Check.equal("easing:steps(2, jump-start)|composite:auto", js(f, "var k = new KeyframeEffect(d, [{opacity: 0, easing: 'steps(2, jump-start)'}, {opacity: 1}])"
                + ".getKeyframes()[0]; 'easing:' + k.easing + '|composite:' + k.composite"), "keyframe easing");
        Check.equal("0", js(f, "String(new KeyframeEffect(d, null).getKeyframes().length)"), "null keyframes");
        Check.equal("2", js(f, "var it = {}; it[Symbol.iterator] = function* () { yield {opacity: 0}; yield {opacity: 1}; };"
                + "String(new KeyframeEffect(d, it).getKeyframes().length)"), "any iterable is the array form");
    }

    /**
     * The TypeErrors and DOMExceptions of invalid arguments and states.
     * <p>
     * 잘못된 인수와 상태의 TypeError 와 DOMException.
     */
    public void testInvalidArgumentsThrow() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d'); function err(fn) { try { fn(); return 'none'; } catch (e) { return e.name; } }");
        Check.equal("TypeError", js(f, "err(function () { d.animate([{opacity: 0, offset: 0.6}, {opacity: 1, offset: 0.2}]); })"), "unsorted offsets");
        Check.equal("TypeError", js(f, "err(function () { d.animate([{opacity: 0, offset: 2}]); })"), "offset out of range");
        Check.equal("TypeError", js(f, "err(function () { d.animate([{opacity: 0, easing: 'bogus'}]); })"), "keyframe easing");
        Check.equal("TypeError", js(f, "err(function () { d.animate({opacity: [0, 1], easing: ['linear', 'linear', 'bogus']}); })"), "an unused easing");
        Check.equal("TypeError", js(f, "err(function () { d.animate('x'); })"), "not an object");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, -1); })"), "negative duration");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {iterations: NaN}); })"), "NaN iterations");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {iterationStart: -1}); })"), "negative iteration start");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {fill: 'bogus'}); })"), "fill");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {duration: 'long'}); })"), "duration string");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {easing: 'nope'}); })"), "timing easing");
        Check.equal("TypeError", js(f, "err(function () { d.animate({}, {composite: 'bogus'}); })"), "composite");
        Check.equal("SyntaxError", js(f, "err(function () { d.animate({}, {pseudoElement: 'bogus'}); })"), "pseudo-element");
        Check.equal("none", js(f, "err(function () { d.animate({}, {pseudoElement: ':before', duration: 'auto', iterations: Infinity}); })"), "valid");
        Check.equal("TypeError", js(f, "var a = d.animate({opacity: [0, 1]}, 1000); err(function () { a.currentTime = null; })"), "null current time");
        Check.equal("TypeError", js(f, "err(function () { a.playbackRate = Infinity; })"), "non-finite rate");
        Check.equal("InvalidStateError", js(f, "var b = d.animate({}, {duration: 10, iterations: Infinity}); err(function () { b.finish(); })"), "finish endless");
        Check.equal("true", js(f, "var x; try { b.finish(); } catch (e) { x = e; } String(x instanceof DOMException)"), "a DOMException");
        Check.equal("InvalidStateError", js(f, "var c = new Animation(new KeyframeEffect(d, {}, 10), null); err(function () { c.reverse(); })"),
                "reverse without a timeline");
        Check.equal("TypeError", js(f, "err(function () { Animation(); })"), "constructor without new");
        Check.equal("TypeError", js(f, "err(function () { new Animation({}); })"), "not an effect");
        Check.equal("TypeError", js(f, "err(function () { Animation.prototype.play.call({}); })"), "illegal invocation");
        Check.equal("ease-in", js(f, "var e = new KeyframeEffect(d, {}, {easing: 'ease-in'}); err(function () { e.updateTiming({easing: 'bad'}); });"
                + "e.getTiming().easing"), "a failed update changes nothing");
        Check.equal("::before|SyntaxError|::before", js(f, "var p = new KeyframeEffect(d, {}); p.pseudoElement = ':before';"
                + "[p.pseudoElement, err(function () { p.pseudoElement = 'x'; }), p.pseudoElement].join('|')"), "pseudoElement setter");
    }

    /**
     * The timing members of an effect: the specified timing of <code>getTiming()</code>, the calculated values of <code>getComputedTiming()</code> and
     * the partial update of <code>updateTiming()</code>.
     * <p>
     * 효과의 시간 멤버: <code>getTiming()</code> 의 지정 시간, <code>getComputedTiming()</code> 의 계산 값, <code>updateTiming()</code> 의 부분 갱신.
     */
    public void testTimingApi() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d');");
        Check.equal("{\"delay\":0,\"direction\":\"normal\",\"duration\":\"auto\",\"easing\":\"linear\",\"endDelay\":0,\"fill\":\"auto\","
                + "\"iterationStart\":0,\"iterations\":1}", js(f, "JSON.stringify(new KeyframeEffect(d, null).getTiming())"), "the default timing");
        Check.equal("1500", js(f, "String(new KeyframeEffect(d, null, 1500).getTiming().duration)"), "a number is the duration");
        js(f, "var a = d.animate({opacity: [0, 1]}, {duration: 1000, iterations: 2, delay: 0}); a.pause(); a.currentTime = 1250;");
        Check.equal("{\"delay\":0,\"direction\":\"normal\",\"duration\":1000,\"easing\":\"linear\",\"endDelay\":0,\"fill\":\"none\",\"iterationStart\":0,"
                + "\"iterations\":2,\"activeDuration\":2000,\"currentIteration\":1,\"endTime\":2000,\"localTime\":1250,\"progress\":0.25}",
                js(f, "JSON.stringify(a.effect.getComputedTiming())"), "the computed timing");
        Check.equal("3|ease-out|alternate|1000", js(f, "a.effect.updateTiming({iterations: 3, easing: 'ease-out', direction: 'alternate'});"
                + "var t = a.effect.getTiming(); [t.iterations, t.easing, t.direction, t.duration].join('|')"), "updateTiming keeps the other members");
        Check.equal("null|null", js(f, "var c = new KeyframeEffect(d, null, 100).getComputedTiming(); String(c.localTime) + '|' + String(c.progress)"),
                "no animation: no local time");
        Check.equal("Infinity|Infinity", js(f, "var g = d.animate(null, {duration: 0, iterations: Infinity, fill: 'forwards'}); g.finish();"
                + "var ct = g.effect.getComputedTiming(); [ct.currentIteration, ct.iterations].join('|')"), "zero duration endless");
    }

    /**
     * The order of a frame: the finished promise reactions run before the <code>finish</code> event; the event is an
     * <code>AnimationPlaybackEvent</code> with the times; <code>cancel()</code> rejects <code>ready</code> with an <code>AbortError</code> at once and
     * the <code>cancel</code> event comes with the next frame.
     * <p>
     * 프레임의 순서: finished 프라미스 반응이 <code>finish</code> 이벤트보다 먼저 실행되고, 이벤트는 시간을 가진 <code>AnimationPlaybackEvent</code> 이다.
     * <code>cancel()</code> 은 <code>ready</code> 를 즉시 <code>AbortError</code> 로 reject 하고 <code>cancel</code> 이벤트는 다음 프레임에 온다.
     */
    public void testPromisesAndEventsOrder() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d'); var a = d.animate({opacity: [0, 1]}, 100);"
                + "a.finished.then(function (x) { console.log('finished promise ' + (x === a)); });"
                + "a.onfinish = function (e) { console.log('onfinish ' + e.currentTime + ' ' + (e instanceof AnimationPlaybackEvent) + ' '"
                + " + (typeof e.timelineTime) + ' ' + e.bubbles + ' ' + (e.target === a)); };"
                + "a.addEventListener('finish', function () { console.log('listener'); });");
        f.advance(200);
        Check.equal("finished promise true,onfinish 100 true number false true,listener", f.log(), "promise first, then the events");
        f.console.clear();
        js(f, "var b = d.animate({opacity: [0, 1]}, 100);"
                + "b.ready.catch(function (e) { console.log('ready ' + e.name); });"
                + "b.finished.catch(function (e) { console.log('finished ' + e.name); });"
                + "b.oncancel = function (e) { console.log('cancel ' + e.currentTime + ' ' + e.type); };"
                + "b.cancel(); console.log('sync ' + b.playState);");
        Check.equal("sync idle,ready AbortError,finished AbortError", f.log(), "the rejections at the end of the task");
        f.advance(16);
        Check.equal("sync idle,ready AbortError,finished AbortError,cancel null cancel", f.log(), "the cancel event at the next frame");
        Check.equal("true|0|5", js(f, "var e = new AnimationPlaybackEvent('finish', {currentTime: 5});"
                + "[e instanceof Event, e.timelineTime === null ? 0 : 1, e.currentTime].join('|')"), "the event constructor");
    }

    /**
     * The constructors and the timelines: <code>new Animation()</code>, a null timeline, <code>new DocumentTimeline({originTime})</code>, the copy
     * constructor of <code>KeyframeEffect</code>, and the shared frame with <code>requestAnimationFrame</code> (same time).
     * <p>
     * 생성자와 타임라인: <code>new Animation()</code>, null 타임라인, <code>new DocumentTimeline({originTime})</code>, <code>KeyframeEffect</code> 복사 생성자,
     * <code>requestAnimationFrame</code> 과 공유하는 프레임(같은 시각).
     */
    public void testConstructorsAndTimelines() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d');");
        Check.equal("idle|true|true||", js(f, "var a = new Animation(); [a.playState, a.effect === null, a.timeline === document.timeline,"
                + " a.currentTime, a.id].join('|')"), "a default animation");
        Check.equal("true", js(f, "String(new Animation(null, null).timeline === null)"), "a null timeline");
        Check.equal("true|true|true", js(f, "var t = new DocumentTimeline({originTime: 100});"
                + "[t instanceof AnimationTimeline, typeof document.timeline.currentTime === 'number',"
                + " t.currentTime === document.timeline.currentTime - 100].join('|')"), "a timeline with an origin time");
        Check.equal("true|true|500|add|true", js(f, "var e = new KeyframeEffect(d, {opacity: [0, 1]}, {duration: 500, composite: 'add'});"
                + "var c = new KeyframeEffect(e); [c !== e, c.target === d, c.getTiming().duration, c.composite,"
                + " JSON.stringify(c.getKeyframes()) === JSON.stringify(e.getKeyframes())].join('|')"), "the copy constructor");
        Check.equal("true", js(f, "String(document.timeline === document.timeline)"), "the same timeline object");
        js(f, "var b = d.animate({opacity: [0, 1]}, 1000); requestAnimationFrame(function (t) { console.log('raf ' + (t === document.timeline.currentTime)); });");
        f.advance(16);
        Check.equal("raf true", f.log(), "the animation frame and the timeline share the time");
        Check.equal("1|0", js(f, "var other = new DOMParser().parseFromString('<p>x</p>', 'text/html');"
                + "[other.timeline === other.timeline ? 1 : 0, other.timeline.currentTime === null ? 0 : 1].join('|')"),
                "another document has an inactive timeline");
    }

    /**
     * <code>getAnimations()</code> on elements (with and without the subtree) and on the document: only relevant animations in composite order; a
     * finished animation without fill and a canceled one disappear.
     * <p>
     * 요소(서브트리 포함 여부)와 문서의 <code>getAnimations()</code>: 합성 순서의 관련 애니메이션만. 채움 없이 끝난 애니메이션과 취소한 것은 사라진다.
     */
    public void testGetAnimations() {
        PageFixture f = page("<div id=d><span id=c></span></div>");
        js(f, "var d = document.getElementById('d'); var c = document.getElementById('c');"
                + "var a = d.animate({opacity: [0, 1]}, 100); var b = c.animate({opacity: [0, 1]}, 1000); var x = d.animate({color: ['red', 'blue']}, 1000);");
        Check.equal("2|3|3|true|true", js(f, "[d.getAnimations().length, d.getAnimations({subtree: true}).length, document.getAnimations().length,"
                + " document.getAnimations()[0] === a, document.getAnimations()[2] === x].join('|')"), "relevant animations in composite order");
        f.advance(200);
        Check.equal("1|2", js(f, "[d.getAnimations().length, document.getAnimations().length].join('|')"), "the finished one without fill is gone");
        js(f, "b.cancel();");
        Check.equal("1", js(f, "String(document.getAnimations().length)"), "the canceled one is gone");
        Check.equal("true", js(f, "String(c.getAnimations().length === 0)"), "nothing on c");
    }

    /**
     * Replaced animations are removed with a <code>remove</code> event, <code>persist()</code> keeps one, and <code>commitStyles()</code> writes the
     * animated value into the <code>style</code> attribute.
     * <p>
     * 교체된 애니메이션은 <code>remove</code> 이벤트와 함께 제거되고, <code>persist()</code> 는 남기며, <code>commitStyles()</code> 는 애니메이션 값을
     * <code>style</code> 속성에 쓴다.
     */
    public void testReplaceAndCommitStyles() {
        PageFixture f = page("<div id=d></div>");
        js(f, "var d = document.getElementById('d');"
                + "var a = d.animate({opacity: 0.2}, {duration: 50, fill: 'forwards'});"
                + "var p = d.animate({opacity: 0.4}, {duration: 50, fill: 'forwards'}); p.persist();"
                + "var b = d.animate({opacity: 0.6}, {duration: 50, fill: 'forwards'});"
                + "a.onremove = function (e) { console.log('remove ' + e.type + ' ' + a.replaceState); };");
        f.advance(100);
        Check.equal("remove remove removed", f.log(), "the replaced animation");
        Check.equal("removed|persisted|active|0.6", js(f, "[a.replaceState, p.replaceState, b.replaceState, getComputedStyle(d).opacity].join('|')"),
                "the states and the top value");
        js(f, "a.commitStyles(); b.cancel(); p.cancel();");
        Check.contains(js(f, "d.getAttribute('style')"), "opacity: 0.2", "the removed animation committed its own value");
        Check.equal("0.2", js(f, "getComputedStyle(d).opacity"), "the committed value stays");
    }

    /**
     * When the scripts of the page stop, the script animations end: the timeline becomes inactive and, after the next rendering tick, the style is the
     * cascade value again.
     * <p>
     * 페이지의 스크립트가 멈추면 스크립트 애니메이션이 끝난다: 타임라인은 비활성이 되고 다음 렌더링 틱 뒤에는 스타일이 다시 캐스케이드 값이다.
     */
    public void testStoppedScriptsEndTheAnimations() {
        PageFixture f = page("<div id=d style='opacity:0.9'></div>");
        js(f, "var d = document.getElementById('d'); var a = d.animate({opacity: [0.1, 0.1]}, {duration: 100000});");
        f.advance(16);
        Check.near(0.1, css.getComputedStyle(f.document.getElementById("d")).number(Prop.OPACITY), 1e-6, "animated");
        f.scripts.dispose();
        Check.isTrue(engine.tick(), "the next rendering tick reports the change");
        Check.near(0.9, css.getComputedStyle(f.document.getElementById("d")).number(Prop.OPACITY), 1e-6, "the cascade value again");
        Check.isTrue(engine.getWebAnimations().getAnimations(f.document, true).isEmpty(), "no animation is left");
        f.advance(100);
        Check.near(0.9, css.getComputedStyle(f.document.getElementById("d")).number(Prop.OPACITY), 1e-6, "no later frame changes it");
    }
}
