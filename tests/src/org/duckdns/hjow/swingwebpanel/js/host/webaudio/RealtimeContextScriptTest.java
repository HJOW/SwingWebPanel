package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>AudioContext</code> through the script interface (plan item P25-05 and P25-07, script side; Web Audio API 1.2): the constructor and its options, the state
 * (<code>suspended</code> at first, <code>running</code> after the first task, with <code>statechange</code>), a time that advances with the wall clock and stops while suspended, the promises of
 * <code>resume</code>, <code>suspend</code> and <code>close</code> and the order of the event and the promise, the errors of a closed context, the events of the sources (<code>ended</code> is delivered by
 * a task of the page and never while the rendering thread runs), the analyser that sees a signal in real time, the limit of the open contexts, the output device (<code>sinkId</code>,
 * <code>setSinkId</code>, <code>sinkchange</code>) and the output timestamp. A sound device is not needed: the context of this stage renders in real time and discards the output.
 * <p>
 * 스크립트 인터페이스를 통한 <code>AudioContext</code> 시험(계획 P25-05, P25-07 의 스크립트 쪽. Web Audio API 1.2): 생성자와 옵션, 상태(처음 <code>suspended</code>, 첫 작업 뒤
 * <code>running</code>, <code>statechange</code> 와 함께), 벽시계와 함께 흐르고 일시 중단되면 멈추는 시간, <code>resume</code>, <code>suspend</code>, <code>close</code> 의 프라미스와 이벤트·프라미스의
 * 순서, 닫힌 컨텍스트의 오류, 원천의 이벤트(<code>ended</code> 는 페이지의 작업으로 전달되며 렌더링 스레드가 도는 동안 전달되지 않는다), 실시간으로 신호를 보는 분석기, 열린 컨텍스트의 한도,
 * 출력 장치(<code>sinkId</code>, <code>setSinkId</code>, <code>sinkchange</code>), 출력 타임스탬프. 소리 장치는 필요 없다: 이 단계의 컨텍스트는 실시간으로 렌더링하고 출력은 버린다.
 */
public class RealtimeContextScriptTest {

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
    public RealtimeContextScriptTest() {
    }

    /**
     * Loads a page with a log.
     * <p>
     * 로그가 있는 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        page.run("var log = [];");
    }

    /**
     * Disposes the page, which closes the contexts that the case left open.
     * <p>
     * 페이지를 폐기하며, 사례가 열어 둔 컨텍스트가 닫힌다.
     */
    public void tearDown() {
        page.fixture.scripts.dispose();
    }

    /**
     * A new context is <code>suspended</code> until its first task, <code>running</code> after it with exactly one <code>statechange</code>; the defaults and the options.
     * <p>
     * 새 컨텍스트는 첫 작업까지 <code>suspended</code>, 그 뒤에는 <code>running</code> 이며 <code>statechange</code> 는 정확히 한 번이다. 기본값과 옵션.
     */
    public void testStateAndDefaults() {
        page.run("var rt = new AudioContext(); rt.onstatechange = function () { log.push(rt.state); }; log.push('init:' + rt.state);");
        Check.equal("init:suspended", page.run("log.join('|')"), "suspended at first");
        page.waitFor("rt.state === 'running'", 5000, "the context runs");
        page.pumpFor(50);
        Check.equal("init:suspended|running", page.run("log.join('|')"), "one event for the change");
        // 소리 장치가 없을 때도(무음 출력) 출력 지연은 출력에 쌓아 둔 오디오의 시간(대략 50 ms)이다.
        Check.equal("48000:2:true:true", page.run("[rt.sampleRate, rt.destination.channelCount, rt.baseLatency > 0, rt.outputLatency >= 0 && rt.outputLatency < 0.2].join(':')"), "the defaults without a device");
        Check.equal("22050", page.run("var r2 = new AudioContext({sampleRate: 22050}); String(r2.sampleRate)"), "the sample rate option");
        Check.equal("true:true:true", page.run("var rI = new AudioContext({latencyHint: 'interactive'}), rB = new AudioContext({latencyHint: 'balanced'}), rP = new AudioContext({latencyHint: 'playback'});"
                + " var rN = new AudioContext({latencyHint: 0.05}); [rI.baseLatency < rB.baseLatency, rB.baseLatency < rP.baseLatency, rN.baseLatency >= 0.05].join(':')"), "the latency hints order the base latency");
        page.run("rI.close(); rB.close(); rP.close(); rN.close(); r2.close(); 1");
        Check.equal("TypeError", page.errorOf("new AudioContext({latencyHint: 'fastest'})"), "an invalid latency category");
        Check.equal("TypeError", page.errorOf("new AudioContext({latencyHint: NaN})"), "a non-finite latency");
        Check.equal("NotSupportedError", page.errorOf("new AudioContext({sampleRate: 100})"), "a sample rate out of range");
        Check.equal("TypeError", page.errorOf("new AudioContext(5)"), "options must be a dictionary");
        Check.equal("TypeError", page.errorOf("AudioContext()"), "new is required");
        Check.equal("true", page.run("class MyAudioContext extends AudioContext { hello() { return 'hi'; } } var m = new MyAudioContext(); String(m instanceof MyAudioContext && m.hello() === 'hi')"), "a subclass");
    }

    /**
     * The time follows the wall clock, stops while the context is suspended and continues where it stopped.
     * <p>
     * 시간은 벽시계를 따라 흐르고, 컨텍스트가 일시 중단된 동안 멈추며, 멈춘 곳에서 이어진다.
     */
    public void testTimeAdvances() {
        page.run("var rt = new AudioContext(); var t0 = rt.currentTime;");
        page.waitFor("rt.currentTime > 0.15", 5000, "the time advances");
        page.run("var susp = false; rt.suspend().then(function () { susp = true; });");
        page.waitFor("susp", 5000, "the context is suspended");
        double t1 = page.number("rt.currentTime");
        page.pumpFor(150);
        Check.equal(t1, page.number("rt.currentTime"), "the time does not advance while suspended");
        page.run("var res = false; rt.resume().then(function () { res = true; });");
        page.waitFor("res && rt.currentTime > " + (t1 + 0.1), 5000, "the time continues");
        Check.isTrue(page.number("rt.currentTime") >= t1, "from where it stopped");
    }

    /**
     * The promises and the events of <code>suspend</code>, <code>resume</code> and <code>close</code>: the state changes and the event fires before the promise is settled, a context that
     * is closed refuses everything with <code>InvalidStateError</code>, and the promise of a call that changes nothing is settled without an event.
     * <p>
     * <code>suspend</code>, <code>resume</code>, <code>close</code> 의 프라미스와 이벤트: 상태가 바뀌고 이벤트가 프라미스를 확정하기 전에 발생하며, 닫힌 컨텍스트는 모든 것을
     * <code>InvalidStateError</code> 로 거부하고, 아무것도 바꾸지 않는 호출의 프라미스는 이벤트 없이 확정된다.
     */
    public void testSuspendResumeClose() {
        page.run("var rt = new AudioContext(); rt.onstatechange = function () { log.push('event:' + rt.state); };");
        page.waitFor("rt.state === 'running'", 5000, "running");
        page.run("log.length = 0; rt.suspend().then(function () { log.push('suspend-promise:' + rt.state); }); rt.suspend().then(function () { log.push('suspend-again'); });");
        page.waitFor("log.length >= 3", 5000, "suspended");
        Check.equal("event:suspended|suspend-promise:suspended|suspend-again", page.run("log.join('|')"), "the event comes first and a second suspend changes nothing");
        page.run("log.length = 0; rt.resume().then(function () { log.push('resume-promise:' + rt.state); });");
        page.waitFor("log.length >= 2", 5000, "resumed");
        Check.equal("event:running|resume-promise:running", page.run("log.join('|')"), "resume");
        page.run("log.length = 0; rt.resume().then(function () { log.push('resume-noop'); });");
        page.waitFor("log.length >= 1", 5000, "resume while running");
        page.pumpFor(30);
        Check.equal("resume-noop", page.run("log.join('|')"), "resume of a running context fires no event");
        page.run("log.length = 0; rt.close().then(function () { log.push('close-promise:' + rt.state); });");
        page.waitFor("log.length >= 2", 5000, "closed");
        Check.equal("event:closed|close-promise:closed", page.run("log.join('|')"), "close");
        page.run("var errs = []; ['close', 'resume', 'suspend'].forEach(function (m) { rt[m]().then(null, function (e) { errs.push(m + ':' + e.name); }); });");
        page.waitFor("errs.length === 3", 5000, "rejections");
        Check.equal("close:InvalidStateError|resume:InvalidStateError|suspend:InvalidStateError", page.run("errs.join('|')"), "a closed context refuses");
        Check.equal("closed", page.run("rt.state"), "it stays closed");
        Check.equal("true", page.run("String(rt.createGain() instanceof GainNode && rt.createBuffer(1, 1, 8000).length === 1)"), "nodes and buffers can still be created");
    }

    /**
     * The <code>ended</code> event of a source is delivered by a task of the page: it does not fire while the page does nothing (the rendering thread tells the page, it does not call the
     * script), and it fires once with a trusted <code>Event</code> at the handler property and at a listener when the page runs its tasks.
     * <p>
     * 원천의 <code>ended</code> 이벤트는 페이지의 작업으로 전달된다: 페이지가 아무것도 하지 않는 동안에는 발생하지 않고(렌더링 스레드가 페이지에 알릴 뿐 스크립트를 부르지 않는다),
     * 페이지가 작업을 실행할 때 핸들러 프로퍼티와 리스너에 신뢰되는 <code>Event</code> 로 한 번 발생한다.
     */
    public void testEndedIsDeliveredByATask() {
        page.run("var rt = new AudioContext(); var osc = rt.createOscillator(); var hits = 0; osc.onended = function (e) { hits++; log.push('on:' + e.type + ':' + e.isTrusted + ':' + (e.target === osc)); };"
                + " osc.addEventListener('ended', function () { log.push('listener'); }); osc.connect(rt.destination); osc.start(0); osc.stop(0.03);");
        // 페이지의 작업을 실행하지 않고 렌더링이 0.03 초를 지나도록 기다린다. 이벤트는 오지 않아야 한다.
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Check.fail("interrupted");
        }
        Check.equal("0", page.run("String(hits)"), "nothing is delivered while the page does not run its tasks");
        page.pump();
        Check.equal("on:ended:true:true|listener", page.run("log.join('|')"), "delivered by the tasks of the page, handler property first (registration order)");
        page.pumpFor(100);
        Check.equal("1", page.run("String(hits)"), "once");
        Check.equal("InvalidStateError", page.errorOf("osc.start()"), "an ended source cannot start again");
    }

    /**
     * An analyser that is connected to a source sees the signal of the real-time graph.
     * <p>
     * 소스에 연결된 분석기는 실시간 그래프의 신호를 본다.
     */
    public void testAnalyserSeesTheSignal() {
        page.run("var rt = new AudioContext(); var c = rt.createConstantSource(); c.offset.value = 0.5; var an = rt.createAnalyser(); c.connect(an); c.start();");
        page.waitFor("rt.currentTime > 0.2", 5000, "the context runs");
        Check.near(0.5, page.number("var f = new Float32Array(an.fftSize); an.getFloatTimeDomainData(f); f[f.length - 1]"), 1e-6, "the analyser sees the constant signal");
    }

    /**
     * At most six contexts are open at the same time; a closed context gives its place back.
     * <p>
     * 동시에 열려 있는 컨텍스트는 최대 여섯이며 닫힌 컨텍스트는 자리를 돌려준다.
     */
    public void testLimitOfOpenContexts() {
        page.run("var cs = []; var err = null; try { for (var i = 0; i < 20; i++) { cs.push(new AudioContext()); } } catch (e) { err = e.name; }");
        Check.equal("6:NotSupportedError", page.run("cs.length + ':' + err"), "six contexts");
        page.run("var closed = false; cs[0].close().then(function () { closed = true; });");
        page.waitFor("closed", 5000, "one is closed");
        Check.equal("no error", page.errorOf("cs.push(new AudioContext())"), "the place is free again");
        Check.equal("NotSupportedError", page.errorOf("new AudioContext()"), "and the limit applies again");
        Check.equal("no error", page.errorOf("new OfflineAudioContext(1, 128, 8000)"), "offline contexts have a limit of their own");
    }

    /**
     * The output device: the default device is the empty string; <code>setSinkId</code> accepts it and <code>{type: "none"}</code> (with a <code>sinkchange</code> event when the sink changes) and
     * refuses any other device with <code>NotSupportedError</code>.
     * <p>
     * 출력 장치: 기본 장치는 빈 문자열이다. <code>setSinkId</code> 는 그것과 <code>{type: "none"}</code> 을 받으며(싱크가 바뀌면 <code>sinkchange</code> 이벤트) 다른 장치는
     * <code>NotSupportedError</code> 로 거부한다.
     */
    public void testSink() {
        page.run("var rt = new AudioContext(); rt.onsinkchange = function () { log.push('sinkchange:' + (typeof rt.sinkId === 'object' ? rt.sinkId.type : '[' + rt.sinkId + ']')); }; var res = {};");
        Check.equal("", page.run("rt.sinkId"), "the default device");
        page.run("rt.setSinkId('speakers-1').then(null, function (e) { res.device = e.name; }); rt.setSinkId('').then(function () { res.same = 'ok'; }); rt.setSinkId({type: 'none'}).then(function () { res.none = 'ok'; });");
        page.waitFor("res.device && res.same && res.none", 5000, "the promises");
        Check.equal("NotSupportedError:ok:ok", page.run("[res.device, res.same, res.none].join(':')"), "the results");
        page.pumpFor(30);
        Check.equal("sinkchange:none", page.run("log.join('|')"), "one event for the change to none, none for the call that changed nothing");
        Check.equal("none", page.run("rt.sinkId.type"), "the sink is none");
        page.run("res.back = null; rt.setSinkId('').then(function () { res.back = 'ok'; });");
        page.waitFor("res.back", 5000, "back to the default");
        page.pumpFor(30);
        Check.equal("sinkchange:none|sinkchange:[]", page.run("log.join('|')"), "the event for the change back");
        page.run("var e1 = null; rt.setSinkId({type: 'bogus'}).then(null, function (e) { e1 = e.name; });");
        page.waitFor("e1", 5000, "the rejection");
        Check.equal("TypeError", page.run("e1"), "a bad dictionary rejects with TypeError");
    }

    /**
     * <code>getOutputTimestamp()</code> is zero before the first quantum was processed and behind <code>currentTime</code> afterwards.
     * <p>
     * <code>getOutputTimestamp()</code> 는 첫 quantum 을 처리하기 전에는 0 이고 그 뒤에는 <code>currentTime</code> 보다 뒤이다.
     */
    public void testOutputTimestamp() {
        page.run("var rt = new AudioContext(); var ts0 = rt.getOutputTimestamp();");
        Check.equal("true", page.run("String(typeof ts0.contextTime === 'number' && typeof ts0.performanceTime === 'number' && ts0.contextTime <= rt.currentTime)"), "the shape");
        page.waitFor("rt.currentTime > 0.2", 5000, "the context runs");
        Check.equal("true", page.run("var ts = rt.getOutputTimestamp(); String(ts.contextTime > 0 && ts.contextTime <= rt.currentTime && ts.contextTime > rt.currentTime - 0.5 && ts.performanceTime >= 0)"),
                "not after the current time and not far behind it");
    }
}
