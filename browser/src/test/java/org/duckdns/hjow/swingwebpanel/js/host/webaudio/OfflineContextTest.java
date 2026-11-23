package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the life of an <code>OfflineAudioContext</code> through the script interface (plan item P25-05; Web Audio API 1.3): the states and the order of the events and of the
 * promise (<code>statechange</code>, the settled promise, <code>complete</code>), the errors of <code>startRendering</code>, <code>suspend</code> and <code>resume</code> with the names of the
 * specification, the suspensions that let a script change the graph while the rendering stands still (the new value is heard from the first frame of the next quantum and
 * not earlier), the constructor forms, the page limits, and that the rendering runs on a thread of its own while the page keeps working.
 * <p>
 * 스크립트 인터페이스를 통한 <code>OfflineAudioContext</code> 의 수명 시험(계획 P25-05. Web Audio API 1.3): 상태와 이벤트 및 프라미스의 순서(<code>statechange</code>, 확정된 프라미스,
 * <code>complete</code>), 규격의 이름을 가진 <code>startRendering</code>, <code>suspend</code>, <code>resume</code> 의 오류, 렌더링이 서 있는 동안 스크립트가 그래프를 바꾸게 하는 일시 중단(새 값은 다음
 * quantum 의 첫 프레임부터 들리고 그보다 일찍 들리지 않는다), 생성자 형식, 페이지 한도, 렌더링이 자기 스레드에서 도는 동안 페이지가 계속 동작함.
 */
public class OfflineContextTest {

    /**
     * The page of the case.
     * <p>
     * 사례의 페이지.
     */
    private WebAudioPage page;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OfflineContextTest() {
    }

    /**
     * Loads a page with a log array.
     * <p>
     * 로그 배열이 있는 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        page.run("var log = [];");
    }

    /**
     * Closes what the case opened.
     * <p>
     * 사례가 연 것을 닫는다.
     */
    public void tearDown() {
        page.fixture.scripts.dispose();
    }

    /**
     * The states and the order of the notifications of one rendering: the state is <code>suspended</code> before and <code>running</code> as soon as the rendering was asked
     * for (with a <code>statechange</code> event in a task of its own), then <code>closed</code> with an event, the promise is settled, and <code>complete</code> is the last event,
     * with the buffer that the promise gave.
     * <p>
     * 렌더링 하나의 알림의 상태와 순서: 상태는 이전에는 <code>suspended</code>, 렌더링을 요청하자마자 <code>running</code>(자기 작업의 <code>statechange</code> 이벤트와 함께), 그 다음
     * 이벤트와 함께 <code>closed</code>, 프라미스가 확정되고, <code>complete</code> 가 마지막 이벤트이며 프라미스가 준 버퍼를 싣는다.
     */
    public void testStatesAndOrder() {
        page.run("var ctx = new OfflineAudioContext(2, 1024, 8000); ctx.onstatechange = function () { log.push('state:' + ctx.state); };"
                + " ctx.addEventListener('complete', function (e) { log.push('complete:' + (e.renderedBuffer === got) + ':' + e.isTrusted + ':' + (e instanceof OfflineAudioCompletionEvent)); });"
                + " ctx.oncomplete = function (e) { log.push('oncomplete'); }; var got = null; log.push('before:' + ctx.state + ':' + ctx.length + ':' + ctx.currentTime);"
                + " var p = ctx.startRendering(); log.push('after:' + ctx.state); p.then(function (b) { got = b; log.push('then:' + ctx.state); });");
        Check.equal("before:suspended:1024:0|after:running", page.run("log.join('|')"), "the state right after the request");
        page.waitFor("got !== null && log.length >= 5", 10000, "the rendering ends and complete is fired");
        Check.equal("before:suspended:1024:0|after:running|state:running|state:closed|then:closed|complete:true:true:true|oncomplete", page.run("log.join('|')"),
                "the order of the events and of the promise");
        Check.equal("2:1024:8000:closed", page.run("[got.numberOfChannels, got.length, got.sampleRate, ctx.state].join(':')"), "the result");
        Check.near(1024.0 / 8000.0, page.number("ctx.currentTime"), 1e-9, "the time of the context after the rendering");
    }

    /**
     * Errors: a second <code>startRendering()</code>, <code>resume()</code> before the start and after the end, <code>suspend()</code> at an invalid time, and a promise that is rejected
     * (not an exception thrown) for every failure.
     * <p>
     * 오류: 두 번째 <code>startRendering()</code>, 시작 전과 끝난 뒤의 <code>resume()</code>, 올바르지 않은 시간의 <code>suspend()</code>, 모든 실패에 대해 예외가 던져지지 않고 reject 된 프라미스.
     */
    public void testErrors() {
        page.run("var ctx = new OfflineAudioContext(1, 2048, 8000); var errs = {};"
                + " function note(key) { return function (e) { errs[key] = e.name; }; }"
                + " ctx.resume().then(null, note('resumeBefore'));"
                + " ctx.suspend(0).then(null, note('suspendAtZero'));"
                + " ctx.suspend(-1).then(null, note('suspendNegative'));"
                + " ctx.suspend(10).then(null, note('suspendBeyond'));"
                + " ctx.suspend(0.1).then(function () { return ctx.resume(); }, note('first'));"
                + " ctx.suspend(0.1).then(null, note('duplicate'));"
                + " ctx.suspend(0.1001).then(null, note('sameQuantum'));"
                + " ctx.suspend().then(null, note('noArgument'));"
                + " ctx.suspend(NaN).then(null, note('nan'));"
                + " var done = false; ctx.startRendering().then(function () { done = true; });"
                + " ctx.startRendering().then(null, note('twice'));");
        page.waitFor("errs.twice !== undefined && errs.noArgument !== undefined && errs.nan !== undefined", 10000, "the rejections");
        Check.equal("InvalidStateError:InvalidStateError:InvalidStateError:InvalidStateError:InvalidStateError:InvalidStateError::TypeError:TypeError",
                page.run("[errs.resumeBefore, errs.suspendAtZero, errs.suspendNegative, errs.suspendBeyond, errs.duplicate, errs.sameQuantum, errs.first, errs.noArgument, errs.nan].join(':')"),
                "the errors of the suspensions");
        Check.equal("InvalidStateError", page.run("errs.twice"), "a second startRendering");
        page.waitFor("done", 10000, "the rendering ends");
        Check.equal("closed", page.run("ctx.state"), "closed after the rendering");
        page.run("var late = {}; ctx.resume().then(null, function (e) { late.resume = e.name; }); ctx.suspend(0.2).then(null, function (e) { late.suspend = e.name; });");
        page.waitFor("late.resume !== undefined && late.suspend !== undefined", 5000, "late rejections");
        Check.equal("InvalidStateError:InvalidStateError", page.run("late.resume + ':' + late.suspend"), "resume and suspend of a context whose rendering ended");
    }

    /**
     * A suspension lets a script change the graph while the rendering stands still: the gain that the script sets in the callback of the suspension is heard from the first frame of
     * the quantum at which the rendering stopped (the time is rounded up to the quantum), and the frames before that are untouched; the context time at the suspension is the frame
     * of the stop, and two suspensions in a row work.
     * <p>
     * 일시 중단은 렌더링이 서 있는 동안 스크립트가 그래프를 바꾸게 한다: 스크립트가 일시 중단의 콜백에서 정한 이득은 렌더링이 멈춘 quantum 의 첫 프레임부터 들리고(시간은 quantum 으로
     * 올림한다) 그 전의 프레임은 건드리지 않는다. 일시 중단 시점의 컨텍스트 시간은 멈춘 프레임이며 일시 중단이 연달아 있어도 동작한다.
     */
    public void testSuspendAndChangeTheGraph() {
        // 8000 Hz, quantum 128: 0.1 s = 800 frames -> 896 (7 quanta), 0.2 s = 1600 frames -> 1664 (13 quanta).
        page.run("var ctx = new OfflineAudioContext(1, 3000, 8000); var c = ctx.createConstantSource(); var g = ctx.createGain(); g.gain.value = 1; c.connect(g); g.connect(ctx.destination); c.start();"
                + " ctx.suspend(0.1).then(function () { log.push('s1:' + Math.round(ctx.currentTime * 8000) + ':' + ctx.state); g.gain.value = 0.5; return ctx.resume(); });"
                + " ctx.suspend(0.2).then(function () { log.push('s2:' + Math.round(ctx.currentTime * 8000) + ':' + ctx.state); g.gain.value = 0.25; return ctx.resume(); });"
                + " var out = null; ctx.startRendering().then(function (b) { out = b; });");
        page.waitFor("out !== null", 10000, "the rendering ends");
        Check.equal("s1:896:suspended|s2:1664:suspended", page.run("log.join('|')"), "the callbacks ran at the quanta that follow the times");
        Check.equal("1:1:0.5:0.5:0.25:0.25", page.run("var o = out.getChannelData(0); [o[0], o[895], o[896], o[1663], o[1664], o[2999]].join(':')"), "the gain changes at the first frame of the quantum");
    }

    /**
     * The constructor forms: the dictionary form with its defaults and required members, the three-argument form, and the arguments that are out of range.
     * <p>
     * 생성자 형식: 기본값과 필수 멤버가 있는 사전 형식, 인수 셋 형식, 범위 밖의 인수.
     */
    public void testConstructors() {
        Check.equal("1:4096:8000:1", page.run("var a = new OfflineAudioContext({length: 4096, sampleRate: 8000}); [a.destination.channelCount, a.length, a.sampleRate, a.destination.maxChannelCount > 0 ? 1 : 0].join(':')"),
                "the dictionary form: one channel by default");
        Check.equal("6:100:22050", page.run("var b = new OfflineAudioContext(6, 100, 22050); [b.destination.channelCount, b.length, b.sampleRate].join(':')"), "the three-argument form");
        Check.equal("TypeError", page.errorOf("new OfflineAudioContext({sampleRate: 8000})"), "length is required");
        Check.equal("TypeError", page.errorOf("new OfflineAudioContext({length: 100})"), "sampleRate is required");
        Check.equal("TypeError", page.errorOf("new OfflineAudioContext(2, 100)"), "three arguments are required");
        Check.equal("TypeError", page.errorOf("new OfflineAudioContext()"), "no argument");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(0, 100, 8000)"), "0 channels");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(33, 100, 8000)"), "33 channels");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 0, 8000)"), "length 0");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 100, 100)"), "a sample rate out of range");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 4294967295, 8000)"), "a huge length");
        Check.equal("TypeError", page.errorOf("OfflineAudioContext(1, 100, 8000)"), "the constructor needs new");
        Check.equal("TypeError", page.errorOf("new OfflineAudioContext(1, 100, NaN)"), "a non-finite sample rate");
        Check.equal("true", page.run("class MyContext extends OfflineAudioContext { hello() { return 'hi'; } } var m = new MyContext(1, 128, 8000); String(m instanceof MyContext && m instanceof OfflineAudioContext && m.hello() === 'hi')"),
                "a subclass works");
    }

    /**
     * The memory of a page is bounded: offline contexts that exceed the budget or the number of contexts are refused with <code>NotSupportedError</code>, and a context gives its
     * share back when its rendering has ended.
     * <p>
     * 페이지의 메모리는 제한된다: 예산이나 컨텍스트 수를 넘는 오프라인 컨텍스트는 <code>NotSupportedError</code> 로 거부되고, 컨텍스트는 렌더링이 끝나면 자기 몫을 돌려준다.
     */
    public void testPageLimits() {
        // 컨텍스트 수 제한: 렌더링을 끝내지 않은 16 개까지. 렌더링이 끝난 컨텍스트는 자리를 돌려준다.
        page.run("var cs = []; var err = null; try { for (var i = 0; i < 40; i++) { cs.push(new OfflineAudioContext(1, 128, 8000)); } } catch (e) { err = e.name; }");
        Check.equal("16:NotSupportedError", page.run("cs.length + ':' + err"), "at most 16 contexts that did not finish");
        page.run("var fin = false; cs[0].startRendering().then(function () { fin = true; });");
        page.waitFor("fin", 10000, "a context finishes");
        Check.equal("no error", page.errorOf("cs.push(new OfflineAudioContext(1, 128, 8000))"), "the place of the finished context is free again");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 128, 8000)"), "and the limit applies again");
        page.fixture.scripts.dispose();
        page = new WebAudioPage();
        // 메모리 예산은 2^25 표본이다: 채널 4 개, 길이 2^23 프레임인 컨텍스트가 정확히 예산이다(134 MB 를 쓰므로 힙이 모자라면 건너뛴다).
        if (Runtime.getRuntime().maxMemory() < 700L * 1024 * 1024) {
            Check.skip("the heap is too small for the 134 MB context of the budget case");
        }
        page.run("var big = new OfflineAudioContext(4, 1 << 23, 8000);");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 128, 8000)"), "the budget is used up");
        page.run("big = null;");
        Check.equal("NotSupportedError", page.errorOf("new OfflineAudioContext(1, 128, 8000)"), "a dropped context keeps its memory until its rendering ends or the page ends");
    }

    /**
     * While a long rendering runs on its thread the page stays responsive: a task of the page runs, a timer fires, and the promise is settled later by a task of the page.
     * <p>
     * 긴 렌더링이 자기 스레드에서 도는 동안 페이지는 반응한다: 페이지의 작업이 실행되고, 타이머가 발생하고, 프라미스는 나중에 페이지의 작업으로 확정된다.
     */
    public void testPageStaysResponsive() {
        page.run("var ctx = new OfflineAudioContext(2, 48000 * 60, 48000); var osc = ctx.createOscillator(); var bq = ctx.createBiquadFilter(); osc.connect(bq); bq.connect(ctx.destination); osc.start();"
                + " var fin = false; ctx.startRendering().then(function () { fin = true; }); var ticks = 0;");
        // 렌더링 도중 페이지의 스크립트를 여러 번 실행해 본다(스크립트 스레드는 막히지 않는다).
        for (int i = 0; i < 5; i++) {
            Check.equal(Integer.toString(i + 1), page.run("String(++ticks)"), "the page runs scripts while the rendering is in progress");
            page.pump();
        }
        page.waitFor("fin", 30000, "the long rendering ends");
        Check.equal("closed", page.run("ctx.state"), "closed");
    }

}
