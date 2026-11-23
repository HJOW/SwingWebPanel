package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The audio worklet in a real-time context, where the script runs on the rendering thread against the clock (plan item P25-06): the audio of a processor reaches the graph (an analyser that listens
 * to a sine processor sees the sine, every call of <code>process()</code> sees the next 128 frames and the processor keeps up with the clock), a processor that needs longer than the limit of a call
 * fails like one that throws and the context goes on, a worklet that is busy with something else (a long message handler) does not hold the rendering thread: the processor is skipped for the quanta in
 * which the script realm is busy, silence in them, and plays again afterwards without an error, while an offline context waits for the script and renders the same audio as always. The cost of a
 * processor is measured with an offline rendering of 2 seconds (the speed in times real time and the time of one quantum are printed).
 * <p>
 * The tests do not assert the speed of the machine: what depends on the wall clock is printed, and the checks are loose lower bounds that catch a stuck or an order of magnitude slower realm but not a
 * busy computer (see the constants <code>MINIMUM_TIMES_REAL_TIME</code>, <code>MAXIMUM_SKIPPED_SHARE</code> and the notes in <code>docs/progress/webaudio-d1.md</code>).
 * <p>
 * 실시간 컨텍스트의 오디오 worklet. 스크립트는 렌더링 스레드에서 시계를 상대로 실행된다(계획 P25-06): 프로세서의 오디오가 그래프에 닿고(사인 프로세서를 듣는 분석기가 사인을 보고, 모든
 * <code>process()</code> 호출이 다음 128 프레임을 보고, 프로세서가 시계를 따라간다), 호출의 한도보다 오래 걸리는 프로세서는 던지는 프로세서처럼 실패하고 컨텍스트는 계속되며, 다른 일(긴 메시지 핸들러)로
 * 바쁜 worklet 이 렌더링 스레드를 붙잡지 않는다: 스크립트 realm 이 바쁜 quantum 에서는 프로세서를 건너뛰어 무음이고, 그 뒤에는 오류 없이 다시 재생한다. 오프라인 컨텍스트는 스크립트를 기다려 언제나 같은
 * 오디오를 렌더링한다. 프로세서의 비용은 2 초의 오프라인 렌더링으로 측정한다(실시간의 몇 배 속도인지와 quantum 하나의 시간을 출력한다).
 * <p>
 * 시험은 컴퓨터의 속도를 단언하지 않는다: 벽시계에 달린 값은 출력만 하고, 검사는 멈췄거나 한 자릿수 이상 느린 realm 은 잡되 바쁜 컴퓨터는 잡지 않는 느슨한 하한이다(상수
 * <code>MINIMUM_TIMES_REAL_TIME</code>, <code>MAXIMUM_SKIPPED_SHARE</code> 와 <code>docs/progress/webaudio-d1.md</code> 의 설명 참조).
 */
public class AudioWorkletRealtimeTest {

    /**
     * The lowest speed, in times real time, that a single simple processor must reach in the cost measurement: 0.1, that is, ten seconds of audio in at most one hundred seconds. It catches a hang or a
     * regression of one order of magnitude or more, not the load of the computer (a heavily loaded shared machine measured 0.8 for a gain processor where the quiet one measures about 6 to 8).
     * <p>
     * 비용 측정에서 단순한 프로세서 하나가 도달해야 하는 가장 낮은 속도(실시간의 배수): 0.1, 곧 10 초 분량을 많아야 100 초에 렌더링한다. 멈춤이나 한 자릿수 이상의 퇴보는 잡지만 컴퓨터의 부하는 잡지 않는다
     * (여럿이 쓰는 몹시 바쁜 컴퓨터에서 게인 프로세서가 0.8 로 측정되었고 한가한 컴퓨터에서는 약 6 에서 8 이 측정된다).
     */
    private static final double MINIMUM_TIMES_REAL_TIME = 0.1;

    /**
     * The most that the cost measurement tries for one single processor: the fastest of up to this many renderings counts, and it stops as soon as one reaches real time.
     * <p>
     * 비용 측정이 단일 프로세서 하나에 시도하는 최대 횟수: 이 횟수까지 렌더링한 것 중 가장 빠른 것을 쓰며, 하나가 실시간에 이르면 바로 멈춘다.
     */
    private static final int MAXIMUM_ATTEMPTS = 3;

    /**
     * The time, in milliseconds, after the start of the cost measurement of the single processors after which a slow rendering is not repeated: the test runner stops a case after 60 seconds, and on a computer
     * that is so busy that one rendering takes many seconds the repetitions would only end in that timeout.
     * <p>
     * 단일 프로세서의 비용 측정이 시작된 뒤 느린 렌더링을 되풀이하지 않게 되는 시간(밀리초): 시험 실행기는 사례를 60 초 뒤에 멈추는데, 렌더링 하나가 몇 초씩 걸릴 만큼 바쁜 컴퓨터에서는 되풀이가 그 제한
     * 시간 초과로 끝날 뿐이다.
     */
    private static final long RETRY_BUDGET_MILLIS = 20000L;

    /**
     * The speed, in times real time, that one gain processor must reach before the measurement of several processors is made: a computer that renders one processor more slowly is too busy, and the
     * renderings of four and eight processors (about 5 and 11 seconds on a quiet computer) would run into the limit of the time of a test. The test is skipped with this reason then, not failed.
     * <p>
     * 여러 프로세서의 측정을 하기 전에 게인 프로세서 하나가 도달해야 하는 속도(실시간의 배수): 하나를 이보다 느리게 렌더링하는 컴퓨터는 너무 바쁘고, 넷과 여덟 프로세서의 렌더링(한가한 컴퓨터에서 약 5 초와
     * 11 초)은 시험의 제한 시간에 걸린다. 그때는 이 이유로 시험을 건너뛰며 실패시키지 않는다.
     */
    private static final double MINIMUM_TIMES_FOR_SEVERAL = 2.0;

    /**
     * The speed, in times real time, that the empty processor must reach before the other processors are measured: it measures the fixed cost of a call, 90 times real time and more on a quiet computer
     * and 1.7 times with eight programs that fill the four processors (where one gain processor needs 16 seconds for 10 seconds of audio). Below it the test is skipped with that reason, because the
     * renderings that follow would not end within the limit of the time of a test.
     * <p>
     * 다른 프로세서를 재기 전에 빈 프로세서가 도달해야 하는 속도(실시간의 배수): 호출의 고정 비용을 재며 한가한 컴퓨터에서는 실시간의 90 배 이상이고, 프로세서 넷을 채우는 프로그램 여덟 개가 도는 중에는 1.7
     * 배였다(그때 게인 프로세서 하나는 10 초 분량에 16 초가 걸렸다). 이보다 느리면 이어지는 렌더링이 시험의 제한 시간 안에 끝나지 않으므로 그 이유를 밝히고 시험을 건너뛴다.
     */
    private static final double MINIMUM_EMPTY_TIMES_REAL_TIME = 5.0;

    /**
     * The share of the calls of a real-time processor that may skip quanta (the frame counter jumps by more than 128) before the test says that quanta are skipped too often: with a loaded computer the
     * worklet thread can be descheduled while it holds the lock of the realm, so that the rendering thread gives up after its short wait.
     * <p>
     * 실시간 프로세서의 호출 중 quantum 을 건너뛰어도(프레임 계수가 128 보다 크게 뛰어도) 되는 비율. 이를 넘으면 quantum 을 너무 자주 건너뛴다고 판정한다: 바쁜 컴퓨터에서는 worklet 스레드가 realm 의 잠금을
     * 쥔 채 스케줄에서 밀려 렌더링 스레드가 짧은 기다림 뒤 포기할 수 있다.
     */
    private static final double MAXIMUM_SKIPPED_SHARE = 0.25;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletRealtimeTest() {
    }

    /**
     * A sine of 1000 Hz with its own phase; it posts the frame of every call when asked.
     * <p>
     * 자기 위상을 가진 1000 Hz 사인. 요청하면 호출마다 프레임을 보낸다.
     */
    private static final String SINE = "class Sine extends AudioWorkletProcessor {\n" + "  constructor() { super(); this.phase = 0; this.report = false; this.port.onmessage = (e) => {\n"
            + "    if (e.data.type === 'report') this.report = e.data.on;\n" + "    else if (e.data.type === 'busy') { const end = Date.now() + e.data.ms; while (Date.now() < end) {} this.port.postMessage({type: 'busy-done'}); }\n"
            + "  }; }\n" + "  process(inputs, outputs) {\n" + "    if (this.report) this.port.postMessage({type: 'frame', frame: currentFrame});\n" + "    const o = outputs[0][0];\n"
            + "    const step = 2 * Math.PI * 1000 / sampleRate;\n" + "    for (let i = 0; i < o.length; i++) { o[i] = Math.sin(this.phase); this.phase += step; }\n"
            + "    this.phase %= 2 * Math.PI;\n" + "    return true;\n" + "  }\n" + "}\n" + "registerProcessor('sine', Sine);\n";

    /**
     * Creates a page with a real-time context, a sine processor connected to an analyser, and <code>frames</code> that collects the frames that the processor reports.
     * <p>
     * 실시간 컨텍스트, 분석기에 연결한 사인 프로세서, 프로세서가 보고하는 프레임을 모으는 <code>frames</code> 가 있는 페이지를 만든다.
     *
     * @return the page / 페이지
     */
    private static WorkletPage live() {
        WorkletPage p = new WorkletPage();
        p.module("sine.js", SINE);
        p.run("var ctx = new AudioContext(); var frames = []; var events = []; var ready = false; var node = null; var an = null; var busyDone = false;"
                + "ctx.audioWorklet.addModule('sine.js').then(function () { node = new AudioWorkletNode(ctx, 'sine', {numberOfInputs: 0});"
                + "node.port.onmessage = function (e) { if (e.data.type === 'frame') frames.push(e.data.frame); else if (e.data.type === 'busy-done') busyDone = true; };"
                + "node.onprocessorerror = function (e) { events.push(e.message); };"
                + "an = ctx.createAnalyser(); an.fftSize = 2048; node.connect(an); an.connect(ctx.destination); ready = true; });");
        p.waitFor("ready", 20000, "the module and the node");
        return p;
    }

    /**
     * An analyser that listens to a sine processor in a real-time context sees the sine (peak about 1, mean square about 0.5), every call of <code>process()</code> reports the next quantum (the
     * frame counter grows by exactly 128 when no quantum is skipped, and by a positive multiple of 128 otherwise), and the counter keeps up with the clock: after a time <code>t</code> the context
     * rendered about <code>t</code> seconds. The checks that depend on the load of the computer are loose (at most a quarter of the calls may follow a skipped quantum; the context may be as slow as a
     * tenth of real time, but not much faster than real time).
     * <p>
     * 실시간 컨텍스트에서 사인 프로세서를 듣는 분석기는 사인을 본다(최댓값 약 1, 제곱 평균 약 0.5). 모든 <code>process()</code> 호출은 다음 quantum 을 보고한다(건너뛴 quantum 이 없으면 프레임 계수는
     * 정확히 128 씩, 있으면 128 의 양의 배수만큼 늘어난다). 계수는 시계를 따라간다: 시간 <code>t</code> 뒤에 컨텍스트는 약 <code>t</code> 초를 렌더링한다. 컴퓨터의 부하에 달린 검사는 느슨하다
     * (호출의 4 분의 1 까지는 건너뛴 quantum 뒤여도 되고, 컨텍스트는 실시간의 10 분의 1 만큼 느려도 되지만 실시간보다 훨씬 빨라서는 안 된다).
     */
    public void testTheAudioOfAProcessorInARealTimeContext() {
        WorkletPage p = live();
        p.run("node.port.postMessage({type: 'report', on: true});");
        long start = System.currentTimeMillis();
        p.waitFor("frames.length > 200", 30000, "200 quanta (0.5 seconds of audio)");
        long took = System.currentTimeMillis() - start;
        // 분석기가 사인을 본다.
        p.run("var buf = new Float32Array(2048); an.getFloatTimeDomainData(buf); var peak = 0, sq = 0; for (var i = 0; i < buf.length; i++) { peak = Math.max(peak, Math.abs(buf[i])); sq += buf[i] * buf[i]; }"
                + "var ms = sq / buf.length;");
        Check.near(1.0, p.number("peak"), 0.01, "the peak of the sine that the analyser sees");
        Check.near(0.5, p.number("ms"), 0.02, "the mean square of the sine");
        // 프레임 계수는 언제나 128 의 양의 배수로 늘고(되돌아가거나 쪼개지지 않는다), 거의 언제나 정확히 128 씩 늘며(건너뛴 quantum 은 드물다), 시계를 따라간다(0.5 초는 24000 프레임 안팎).
        // 부하가 큰 컴퓨터에서는 worklet 스레드가 잠금을 쥔 채 밀려 렌더링 스레드가 quantum 을 건너뛸 수 있으므로 건너뜀의 비율만 제한한다.
        double odd = p.number("var odd = 0; for (var i = 1; i < frames.length; i++) { var d = frames[i] - frames[i - 1]; if (d <= 0 || d % 128 !== 0) odd++; } odd");
        Check.near(0, odd, 0, "the frame counter always grows by a positive multiple of 128");
        double gaps = p.number("var bad = 0; for (var i = 1; i < frames.length; i++) { if (frames[i] - frames[i - 1] !== 128) bad++; } bad");
        double calls = p.number("frames.length - 1");
        System.out.println("[worklet realtime] " + (long) gaps + " of " + (long) calls + " calls followed a skipped quantum, the context rendered " + p.number("frames[frames.length - 1] / 48000") + " s in "
                + took + " ms");
        Check.isTrue(gaps <= calls * MAXIMUM_SKIPPED_SHARE, "quanta are skipped rarely: " + gaps + " of " + calls + " calls followed a skip");
        // 시계: 아래쪽은 아주 느슨하게(대기 시간에는 폴링의 지연도 들어 있다), 위쪽은 실시간보다 빨리 렌더링할 수 없다는 사실로 확인한다.
        double seconds = p.number("frames[frames.length - 1] / 48000");
        Check.isTrue(seconds > 0.1 * took / 1000.0 && seconds < 2.0 * took / 1000.0 + 0.5, "the context rendered " + seconds + " s in " + took + " ms");
        Check.equal("0", p.run("String(events.length)"), "no processorerror");
        p.run("ctx.close();");
    }

    /**
     * A call that exceeds the real-time or step budget fails like a processor that throws: <code>processorerror</code> reports the script limit
     * and the context goes on (its time advances; the page answers).
     * <p>
     * 실시간 또는 실행 단계 한도를 넘은 호출은 예외를 던지는 프로세서처럼 실패한다. <code>processorerror</code>가 스크립트 한도를 알리고
     * 컨텍스트는 계속된다(시간이 흐르고 페이지는 응답한다).
     */
    public void testASlowProcessorIsStopped() {
        WorkletPage p = new WorkletPage();
        p.module("slow.js", "let n = 0;\nregisterProcessor('slow', class extends AudioWorkletProcessor {\n  process(i, o) {\n    if (++n === 3) { const end = Date.now() + 1000; while (Date.now() < end) {} }\n"
                + "    o[0][0].fill(0.5); return true;\n  }\n});\n");
        p.run("var ctx = new AudioContext(); var events = []; ctx.audioWorklet.addModule('slow.js').then(function () { var n = new AudioWorkletNode(ctx, 'slow', {numberOfInputs: 0});"
                + "n.onprocessorerror = function (e) { events.push(e.message); }; n.connect(ctx.destination); });");
        p.waitFor("events.length === 1", 30000, "the processorerror of the slow processor");
        // 실행 속도에 따라 시간 한도와 단계 한도 중 먼저 도달한 것이 보고된다.
        String message = p.run("events[0]");
        Check.isTrue(message.contains("time limit") || message.contains("step limit"), "the event names an execution limit: " + message);
        // 컨텍스트가 계속되는지는 고정된 시간 창이 아니라 시간이 앞서는 것을 기다려 확인한다(부하가 큰 컴퓨터에서도 흔들리지 않는다).
        double t0 = p.number("ctx.currentTime");
        p.waitFor("ctx.currentTime > " + (t0 + 0.1), 30000, "the context goes on: its time advances by 0.1 s after the failure");
        Check.equal("1", p.run("String(events.length)"), "one event only");
        p.run("ctx.close();");
    }

    /**
     * A worklet that is busy does not hold the rendering thread: while a message handler of the processor runs for 400 ms the script realm is not free, the processor is skipped in those quanta (the frame
     * counter jumps, the clock does not wait), it plays again afterwards without a <code>processorerror</code>, and the page thread is not blocked.
     * <p>
     * 바쁜 worklet 은 렌더링 스레드를 붙잡지 않는다: 프로세서의 메시지 핸들러가 400 ms 동안 실행되는 동안 스크립트 realm 이 비어 있지 않아 그 quantum 에서 프로세서를 건너뛰고(프레임 계수가 건너뛰고 시계는
     * 기다리지 않는다), 그 뒤에는 <code>processorerror</code> 없이 다시 재생하며, 페이지 스레드는 막히지 않는다.
     */
    public void testABusyWorkletDoesNotHoldTheRenderingThread() {
        WorkletPage p = live();
        p.run("node.port.postMessage({type: 'report', on: true});");
        p.waitFor("frames.length > 50", 30000, "the processor runs");
        p.run("node.port.postMessage({type: 'busy', ms: 400});");
        p.waitFor("busyDone", 30000, "the long handler ends");
        int before = (int) p.number("frames.length");
        p.waitFor("frames.length > " + (before + 80), 30000, "the processor plays again");
        // 프레임 계수의 가장 큰 도약: 건너뛴 quantum 이 있어야 한다. 400 ms 는 48 kHz 에서 약 150 quantum 이다.
        double biggest = p.number("var big = 0; for (var i = 1; i < frames.length; i++) { big = Math.max(big, frames[i] - frames[i - 1]); } big / 128");
        Check.isTrue(biggest >= 40, "the processor was skipped for " + biggest + " quanta while the handler ran (about 150 expected)");
        // 위쪽 한도는 느슨하다(약 5 초): 핸들러가 끝난 뒤 다시 재생하는 것을 확인하되, 바쁜 컴퓨터에서 재개가 늦어지는 것은 허용한다.
        Check.isTrue(biggest <= 2000, "but it came back after the handler (a stuck realm would never): " + biggest + " quanta");
        Check.equal("0", p.run("String(events.length)"), "no processorerror: a busy realm is not a failure");
        // 콘솔에는 worklet 이 시계를 따라가지 못했다는 경고가 있다(건너뛴 quantum 수와 함께).
        boolean warned = false;
        // 콘솔의 줄마다 경고가 있는지 확인한다.
        for (String line : p.console()) {
            if (line.startsWith("warn: AudioWorklet: the audio worklet could not keep up with the clock")) {
                warned = true;
            }
        }
        Check.isTrue(warned, "the console of the page was warned: " + p.console());
        p.run("ctx.close();");
    }

    /**
     * An offline context waits for a busy worklet instead of skipping: the same busy handler before the rendering changes nothing about the audio, the rendering is complete and the samples are the closed
     * form from the first frame to the last.
     * <p>
     * 오프라인 컨텍스트는 바쁜 worklet 을 건너뛰지 않고 기다린다: 렌더링 전의 같은 바쁜 핸들러는 오디오를 전혀 바꾸지 않는다. 렌더링은 온전하고 표본은 첫 프레임부터 마지막까지 닫힌 식이다.
     */
    public void testAnOfflineContextWaitsForABusyWorklet() {
        WorkletPage p = new WorkletPage();
        p.module("sine.js", SINE);
        p.run("var ctx = new OfflineAudioContext(1, 4096, 8000); var out = null; ctx.audioWorklet.addModule('sine.js').then(function () { var n = new AudioWorkletNode(ctx, 'sine', {numberOfInputs: 0});"
                + "n.connect(ctx.destination); n.port.postMessage({type: 'busy', ms: 300}); return ctx.startRendering(); }).then(function (b) { out = b; });");
        p.waitFor("out !== null", 30000, "the rendering");
        // sin(2 pi 1000 n / 8000): 위상은 quantum 마다 이어지므로 모든 표본이 닫힌 식이어야 한다.
        double worst = p.number("var w = 0; var d = out.getChannelData(0); for (var i = 0; i < d.length; i++) { w = Math.max(w, Math.abs(d[i] - Math.sin(2 * Math.PI * 1000 * i / 8000))); } w");
        Check.isTrue(worst < 1e-4, "the worst error of 4096 samples is " + worst);
    }


    /**
     * An empty processor: the fixed cost of one call (the arrays, the conversion, the call).
     * <p>
     * 빈 프로세서: 호출 하나의 고정 비용(배열, 변환, 호출).
     */
    private static final String COST_EMPTY = "class P extends AudioWorkletProcessor { process() { return true; } }";

    /**
     * A gain of 0.5 that reads the constant 1 of its input.
     * <p>
     * 입력의 상수 1 을 읽는 0.5 의 게인.
     */
    private static final String COST_GAIN = "class P extends AudioWorkletProcessor { process(inputs, outputs) { const i = inputs[0][0], o = outputs[0][0]; if (!i) return true; for (let k = 0; k < o.length; k++) o[k] = i[k] * 0.5; return false; } }";

    /**
     * A sine generator with a phase.
     * <p>
     * 위상이 있는 사인 생성기.
     */
    private static final String COST_SINE = "class P extends AudioWorkletProcessor { constructor() { super(); this.ph = 0; } process(inputs, outputs) { const o = outputs[0][0]; for (let k = 0; k < o.length; k++) { o[k] = Math.sin(this.ph); this.ph += 0.131; if (this.ph > 6.283185307179586) this.ph -= 6.283185307179586; } return true; } }";

    /**
     * A white noise generator.
     * <p>
     * 백색 잡음 생성기.
     */
    private static final String COST_NOISE = "class P extends AudioWorkletProcessor { process(inputs, outputs) { const o = outputs[0][0]; for (let k = 0; k < o.length; k++) o[k] = Math.random() * 2 - 1; return true; } }";

    /**
     * Renders 2 seconds at 48 kHz (750 quanta) with a number of nodes of one processor kind and returns the time in milliseconds.
     * <p>
     * 한 종류의 프로세서 노드 여럿으로 48 kHz 2 초(750 quanta)를 렌더링하고 시간(밀리초)을 반환한다.
     *
     * @param body         the text of the class of the processor (registered as <code>p</code>) / 프로세서 클래스의 텍스트(<code>p</code> 로 등록)
     * @param inputs       1 if the nodes read a constant of 1, 0 for a generator / 노드가 상수 1 을 읽으면 1, 생성기이면 0
     * @param nodes        the number of nodes / 노드 수
     * @param expectedLast the expected last sample of the rendering (to be sure that the measured rendering did the work), or NaN not to check it / 렌더링의 마지막 표본의 기대값(잰 렌더링이 일을 했는지
     *                     확인한다). 확인하지 않으려면 NaN
     * @return the time of the rendering in milliseconds / 렌더링 시간(밀리초)
     */
    private static double render10Seconds(String body, int inputs, int nodes, double expectedLast) {
        WorkletPage p = new WorkletPage();
        p.module("p.js", body + "\nregisterProcessor('p', P);\n");
        p.run("var ctx = new OfflineAudioContext(1, 96000, 48000); var out = null; ctx.audioWorklet.addModule('p.js').then(function () {"
                + (inputs == 1 ? "var s = new ConstantSourceNode(ctx, {offset: 1}); s.start();" : "") + "for (var i = 0; i < " + nodes + "; i++) { var n = new AudioWorkletNode(ctx, 'p', {numberOfInputs: " + inputs
                + "}); " + (inputs == 1 ? "s.connect(n); " : "") + "n.connect(ctx.destination); } start = Date.now(); return ctx.startRendering(); }).then(function (b) { out = b; took = Date.now() - start; });");
        p.waitFor("out !== null", 180000, "the rendering of 2 seconds with " + nodes + " processors");
        if (!Double.isNaN(expectedLast)) {
            Check.near(expectedLast, p.number("out.getChannelData(0)[95999]"), 1e-6, "the last sample of the rendering that was measured");
        }
        return p.number("took");
    }

    /**
     * Measures the rendering of 2 seconds with a number of nodes of one processor kind and prints the speed (times real time) and the time of one quantum per node. With <code>attempts</code> above
     * one it renders again while the speed is below real time, the time budget is left and the attempts are not used up, and the fastest rendering counts (the wall clock depends on the load of
     * the computer, and a rendering that was slowed down by another program says nothing about the processor).
     * <p>
     * 한 종류의 프로세서 노드 여럿으로 2 초의 렌더링을 재고 속도(실시간의 배수)와 노드마다 quantum 하나의 시간을 출력한다. <code>attempts</code> 가 1 보다 크면 속도가 실시간에 못 미치고 시간 예산이
     * 남고 시도 횟수가 남은 동안 다시 렌더링하며 가장 빠른 렌더링을 쓴다(벽시계는 컴퓨터의 부하에 달려 있고, 다른 프로그램에 느려진 렌더링은 프로세서에 대해 아무것도 말해 주지 않는다).
     *
     * @param name         the label of the kind / 종류의 이름
     * @param body         the text of the class of the processor / 프로세서 클래스의 텍스트
     * @param inputs       1 if the nodes read a constant of 1, 0 for a generator / 노드가 상수 1 을 읽으면 1, 생성기이면 0
     * @param nodes        the number of nodes / 노드 수
     * @param expectedLast the expected last sample, or NaN / 마지막 표본의 기대값. 없으면 NaN
     * @param attempts     the most renderings / 렌더링 최대 횟수
     * @param budgetEnd    the time (in milliseconds since 1970) after which no rendering is repeated / 그 뒤로는 렌더링을 되풀이하지 않는 시각(1970 년부터의 밀리초)
     * @return the speed of the fastest rendering in times real time / 가장 빠른 렌더링의 속도(실시간의 배수)
     */
    private static double measure(String name, String body, int inputs, int nodes, double expectedLast, int attempts, long budgetEnd) {
        double ms = Double.MAX_VALUE;
        int tried = 0;
        // 실시간에 못 미치고 예산과 시도 횟수가 남은 동안 되풀이해 재고 가장 짧은 시간을 쓴다.
        while (tried < attempts) {
            ms = Math.min(ms, render10Seconds(body, inputs, nodes, expectedLast));
            tried++;
            if (2000.0 / ms >= 1.0 || System.currentTimeMillis() > budgetEnd) {
                break;
            }
        }
        double times = 2000.0 / ms;
        System.out.println("[worklet cost] " + nodes + " x " + name + ", 2 s at 48 kHz: " + (long) ms + " ms = " + Math.round(times * 10) / 10.0 + " x real time, "
                + Math.round(ms * 1000.0 / 750 / nodes) + " us per quantum per node" + (tried > 1 ? " (the fastest of " + tried + " renderings)" : ""));
        return times;
    }

    /**
     * The cost of simple processors, measured (JDK 6, offline rendering of 2 seconds at 48 kHz = 750 quanta (the first measurements used 10 seconds; the test was shortened so that a busy computer does not run into the limit of a test case), shared machine): an empty processor (the fixed cost of one call: the arrays, the conversion,
     * the call), a gain, a sine generator with a phase and a white noise generator, one node each. The speed in times real time and the time of one quantum per node are printed (a quantum lasts
     * 2667 us at 48 kHz, so a processor must stay well below that to play in real time). The speed depends on the wall clock and so on the load of the computer: a processor is rendered up to
     * <code>MAXIMUM_ATTEMPTS</code> times (it stops as soon as one rendering reaches real time or the time budget is used up) and the fastest counts, and only a very loose lower bound is checked
     * (<code>MINIMUM_TIMES_REAL_TIME</code>, 0.1 x real time), which a hang or a slowdown of an order of magnitude breaks but a busy computer does not. Every rendering also has to produce the audio that the
     * processor stands for (the last sample), so that a rendering that did nothing is not measured as fast. (A busy computer measured 0.8 x for the gain processor with the former bound of 1.0 x, which
     * failed the regression of the coordinator; the thread CPU time of <code>ThreadMXBean</code> was not used because the rendering runs on threads of the engine that the test does not own and the support
     * of the CPU time of threads is optional.)
     * <p>
     * 단순한 프로세서의 비용 측정(JDK 6, 48 kHz 2 초 = 750 quantum(처음 측정은 10 초였고, 바쁜 컴퓨터가 시험 사례의 제한에 걸리지 않도록 줄였다) 의 오프라인 렌더링, 여럿이 쓰는 컴퓨터): 빈 프로세서(호출 하나의 고정 비용: 배열, 변환, 호출), 게인, 위상이 있는 사인 생성기, 백색
     * 잡음 생성기를 각각 노드 하나로. 실시간의 몇 배 속도인지와 노드마다 quantum 하나의 시간을 출력한다(48 kHz 에서 quantum 하나는 2667 us 이므로 실시간으로 재생하려면 그보다 한참 작아야 한다). 속도는
     * 벽시계에 달려 있어 컴퓨터의 부하에 따라 흔들린다: 프로세서를 최대 <code>MAXIMUM_ATTEMPTS</code> 번 렌더링해(한 번이라도 실시간에 이르거나 시간 예산이 다하면 멈춘다) 가장 빠른 것을 쓰고, 아주
     * 느슨한 하한(<code>MINIMUM_TIMES_REAL_TIME</code>, 실시간의 0.1 배)만 확인한다. 멈춤이나 한 자릿수의 느려짐은 이 하한을 깨지만 바쁜 컴퓨터는 깨지 않는다. 빈 프로세서가 <code>MINIMUM_EMPTY_TIMES_REAL_TIME</code> 배보다 느릴
     * 만큼 컴퓨터가 몹시 바쁘면 나머지 렌더링이 시험의 제한 시간 안에 끝나지 않으므로 이유를 밝히고 시험을 건너뛴다. 렌더링마다 프로세서가 뜻하는 오디오(마지막
     * 표본)도 만들어야 하므로 아무것도 하지 않은 렌더링이 빠르다고 측정되지 않는다. (바쁜 컴퓨터에서 게인 프로세서가 이전 하한 1.0 배에 못 미치는 0.8 배로 측정되어 조정자의 회귀를 실패시켰다.
     * <code>ThreadMXBean</code> 의 스레드 CPU 시간은 쓰지 않았다: 렌더링은 시험이 소유하지 않은 엔진의 스레드에서 돌고 스레드 CPU 시간의 지원은 선택 사항이다.)
     */
    public void testTheCostOfAProcessor() {
        String[] names = {"empty", "gain", "sine", "noise"};
        String[] bodies = {COST_EMPTY, COST_GAIN, COST_SINE, COST_NOISE};
        int[] inputs = {0, 1, 0, 0};
        double[] last = {0, 0.5, Double.NaN, Double.NaN};
        long budgetEnd = System.currentTimeMillis() + RETRY_BUDGET_MILLIS;
        // 종류마다 2 초 분량을 렌더링해 속도를 잰다.
        for (int k = 0; k < names.length; k++) {
            double times = measure(names[k], bodies[k], inputs[k], 1, last[k], MAXIMUM_ATTEMPTS, budgetEnd);
            // 단언은 부하에 둔감한 느슨한 하한뿐이다: 한 자릿수 이상의 퇴보나 멈춤만 잡는다.
            Check.isTrue(times >= MINIMUM_TIMES_REAL_TIME, "one simple processor (" + names[k] + ") renders at 0.1 x real time or faster even on a busy computer: " + times + " x");
            // 빈 프로세서(고정 비용뿐)가 이렇게 느리면 컴퓨터가 몹시 바쁜 것이다. 나머지 렌더링이 시험의 제한 시간 안에 끝나지 않으므로 이유를 밝히고 건너뛴다.
            if (k == 0 && times < MINIMUM_EMPTY_TIMES_REAL_TIME) {
                Check.skip("the computer is too busy to measure the other processors: the empty processor renders at " + Math.round(times * 10) / 10.0 + " x real time (below " + MINIMUM_EMPTY_TIMES_REAL_TIME
                        + " x, 90 x and more when quiet), the other renderings would not end within the limit of a test");
            }
        }
    }

    /**
     * Several gain processors at once (four and eight nodes): the speed and the time of one quantum per node are printed, to see how the cost grows with the number of processors (the cost is the sum of the
     * calls, so about seven simple processors are the limit of real time on the measured machine). Nothing is asserted about the speed; the audio of every rendering is checked (the sum of the nodes). On a computer that is too busy (the empty processor
     * below <code>MINIMUM_EMPTY_TIMES_REAL_TIME</code> x real time, or one gain processor below <code>MINIMUM_TIMES_FOR_SEVERAL</code> x) the test is skipped with that reason, because the
     * renderings would not end within the limit of a test.
     * <p>
     * 게인 프로세서 여럿(노드 넷과 여덟): 프로세서 수에 따라 비용이 어떻게 느는지 보려고 속도와 노드마다 quantum 하나의 시간을 출력한다(비용은 호출의 합이므로 잰 컴퓨터에서는 단순한 프로세서 약 일곱이
     * 실시간의 한계이다). 속도는 단언하지 않는다. 렌더링마다 오디오(노드의 합)는 확인한다. 너무 바쁜 컴퓨터(빈 프로세서가 실시간의
     * <code>MINIMUM_EMPTY_TIMES_REAL_TIME</code> 배보다, 또는 게인 프로세서 하나가 <code>MINIMUM_TIMES_FOR_SEVERAL</code> 배보다 느림)에서는 렌더링이 시험의 제한 시간 안에 끝나지 않으므로 그 이유를
     * 밝히고 시험을 건너뛴다.
     */
    public void testTheCostOfSeveralProcessors() {
        // 컴퓨터가 한가한지 먼저 본다. 바쁜 컴퓨터에서는 렌더링이 시험 실행기의 제한 시간(60 초)을 넘기므로 이유를 밝히고 건너뛴다. 빈 프로세서가 가장 싸므로 그것으로 먼저 보고, 그다음 게인 하나로 본다.
        double idle = measure("empty", COST_EMPTY, 0, 1, 0, 1, 0L);
        if (idle < MINIMUM_EMPTY_TIMES_REAL_TIME) {
            Check.skip("the computer is too busy to measure several processors: the empty processor renders at " + Math.round(idle * 10) / 10.0 + " x real time (below " + MINIMUM_EMPTY_TIMES_REAL_TIME
                    + " x), the renderings of four and eight gain processors would not end within the limit of a test");
        }
        double single = measure("gain", COST_GAIN, 1, 1, 0.5, 1, 0L);
        if (single < MINIMUM_TIMES_FOR_SEVERAL) {
            Check.skip("the computer is too busy to measure several processors: one gain processor renders at " + Math.round(single * 10) / 10.0 + " x real time (below " + MINIMUM_TIMES_FOR_SEVERAL
                    + " x), the renderings of four and eight would not end within the limit of a test");
        }
        int[] nodes = {4, 8};
        // 노드 수마다 한 번 재고 출력한다(속도는 단언하지 않는다).
        for (int k = 0; k < nodes.length; k++) {
            measure("gain", COST_GAIN, 1, nodes[k], 0.5 * nodes[k], 1, 0L);
        }
    }
}
