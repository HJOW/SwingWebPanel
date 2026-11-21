package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The audio worklet in a real-time context, where the script runs on the rendering thread against the clock (plan item P25-06): the audio of a processor reaches the graph (an analyser that listens
 * to a sine processor sees the sine, every call of <code>process()</code> sees the next 128 frames and the processor keeps up with the clock), a processor that needs longer than the limit of a call
 * fails like one that throws and the context goes on, a worklet that is busy with something else (a long message handler) does not hold the rendering thread: the processor is skipped for the quanta in
 * which the script realm is busy, silence in them, and plays again afterwards without an error, while an offline context waits for the script and renders the same audio as always. The cost of a
 * processor is measured with an offline rendering of 10 seconds (the speed in times real time and the time of one quantum are printed).
 * <p>
 * 실시간 컨텍스트의 오디오 worklet. 스크립트는 렌더링 스레드에서 시계를 상대로 실행된다(계획 P25-06): 프로세서의 오디오가 그래프에 닿고(사인 프로세서를 듣는 분석기가 사인을 보고, 모든
 * <code>process()</code> 호출이 다음 128 프레임을 보고, 프로세서가 시계를 따라간다), 호출의 한도보다 오래 걸리는 프로세서는 던지는 프로세서처럼 실패하고 컨텍스트는 계속되며, 다른 일(긴 메시지 핸들러)로
 * 바쁜 worklet 이 렌더링 스레드를 붙잡지 않는다: 스크립트 realm 이 바쁜 quantum 에서는 프로세서를 건너뛰어 무음이고, 그 뒤에는 오류 없이 다시 재생한다. 오프라인 컨텍스트는 스크립트를 기다려 언제나 같은
 * 오디오를 렌더링한다. 프로세서의 비용은 10 초의 오프라인 렌더링으로 측정한다(실시간의 몇 배 속도인지와 quantum 하나의 시간을 출력한다).
 */
public class AudioWorkletRealtimeTest {

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
     * frame counter grows by exactly 128 when no quantum is skipped), and the counter keeps up with the clock: after a time <code>t</code> the context rendered about <code>t</code> seconds.
     * <p>
     * 실시간 컨텍스트에서 사인 프로세서를 듣는 분석기는 사인을 본다(최댓값 약 1, 제곱 평균 약 0.5). 모든 <code>process()</code> 호출은 다음 quantum 을 보고한다(건너뛴 quantum 이 없으면 프레임 계수는
     * 정확히 128 씩 늘어난다). 계수는 시계를 따라간다: 시간 <code>t</code> 뒤에 컨텍스트는 약 <code>t</code> 초를 렌더링한다.
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
        // 프레임 계수는 정확히 128 씩 늘고(건너뛴 quantum 이 없다), 시계를 따라간다(0.5 초는 24000 프레임 안팎).
        double gaps = p.number("var bad = 0; for (var i = 1; i < frames.length; i++) { if (frames[i] - frames[i - 1] !== 128) bad++; } bad");
        Check.near(0, gaps, 0, "no quantum was skipped: the frame counter grew by 128 every time");
        double seconds = p.number("frames[frames.length - 1] / 48000");
        Check.isTrue(seconds > 0.4 * took / 1000.0 && seconds < 2.0 * took / 1000.0 + 0.5, "the context rendered " + seconds + " s in " + took + " ms");
        Check.equal("0", p.run("String(events.length)"), "no processorerror");
        p.run("ctx.close();");
    }

    /**
     * A call that takes longer than {@link WorkletLimits#REALTIME_PROCESS_TIME_LIMIT_MILLIS} fails like a processor that throws: <code>processorerror</code> says that the script was aborted, the node is
     * silent from then on, and the context goes on (its time advances; the page answers).
     * <p>
     * {@link WorkletLimits#REALTIME_PROCESS_TIME_LIMIT_MILLIS} 보다 오래 걸리는 호출은 예외를 던지는 프로세서처럼 실패한다: <code>processorerror</code> 가 스크립트가 중단되었다고 알리고, 노드는 그 뒤 무음이며,
     * 컨텍스트는 계속된다(시간이 흐르고 페이지는 응답한다).
     */
    public void testASlowProcessorIsStopped() {
        WorkletPage p = new WorkletPage();
        p.module("slow.js", "let n = 0;\nregisterProcessor('slow', class extends AudioWorkletProcessor {\n  process(i, o) {\n    if (++n === 3) { const end = Date.now() + 1000; while (Date.now() < end) {} }\n"
                + "    o[0][0].fill(0.5); return true;\n  }\n});\n");
        p.run("var ctx = new AudioContext(); var events = []; ctx.audioWorklet.addModule('slow.js').then(function () { var n = new AudioWorkletNode(ctx, 'slow', {numberOfInputs: 0});"
                + "n.onprocessorerror = function (e) { events.push(e.message); }; n.connect(ctx.destination); });");
        p.waitFor("events.length === 1", 30000, "the processorerror of the slow processor");
        Check.contains(p.run("events[0]"), "time limit", "the event names the time limit: " + p.run("events[0]"));
        double t0 = p.number("ctx.currentTime");
        p.pumpFor(300);
        Check.isTrue(p.number("ctx.currentTime") > t0 + 0.1, "the context goes on");
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
        Check.isTrue(biggest <= 400, "but not much longer than the handler: " + biggest + " quanta");
        Check.equal("0", p.run("String(events.length)"), "no processorerror: a busy realm is not a failure");
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
     * The cost of a processor, measured: an offline rendering of 10 seconds (3750 quanta at 48 kHz) of one, four and eight gain processors on a constant, JDK 6. The speed is printed in times real time and
     * as the time of one quantum per node; only a generous bound is checked, because the machine is shared (a simple processor must render at least as fast as real time).
     * <p>
     * 프로세서의 비용 측정: 상수에 대한 게인 프로세서 하나, 넷, 여덟의 10 초(48 kHz 에서 3750 quantum) 오프라인 렌더링(JDK 6). 속도를 실시간의 몇 배인지와 노드마다 quantum 하나의 시간으로 출력한다. 컴퓨터를
     * 여럿이 쓰므로 넉넉한 한도만 확인한다(단순한 프로세서는 적어도 실시간만큼 빨라야 한다).
     */
    public void testTheCostOfAProcessor() {
        int[] counts = {1, 4, 8};
        // 노드 수마다 10 초 분량을 렌더링해 시간을 잰다.
        for (int k = 0; k < counts.length; k++) {
            WorkletPage p = new WorkletPage();
            p.module("g.js", "registerProcessor('g', class extends AudioWorkletProcessor {\n  process(inputs, outputs) {\n    const i = inputs[0][0], o = outputs[0][0];\n"
                    + "    if (!i) return true;\n    for (let k = 0; k < o.length; k++) o[k] = i[k] * 0.5;\n    return false;\n  }\n});\n");
            p.run("var ctx = new OfflineAudioContext(1, 480000, 48000); var out = null; ctx.audioWorklet.addModule('g.js').then(function () { var s = new ConstantSourceNode(ctx, {offset: 1}); s.start();"
                    + "for (var i = 0; i < " + counts[k] + "; i++) { var n = new AudioWorkletNode(ctx, 'g'); s.connect(n); n.connect(ctx.destination); } start = Date.now(); return ctx.startRendering(); })"
                    + ".then(function (b) { out = b; took = Date.now() - start; });");
            p.waitFor("out !== null", 120000, "the rendering of 10 seconds with " + counts[k] + " processors");
            double ms = p.number("took");
            double times = 10000.0 / ms;
            System.out.println("[worklet cost] " + counts[k] + " gain processors, 10 s at 48 kHz: " + (long) ms + " ms = " + Math.round(times * 10) / 10.0 + " x real time, "
                    + Math.round(ms * 1000.0 / 3750 / counts[k]) + " us per quantum per node");
            Check.near(counts[k] * 0.5, p.number("out.getChannelData(0)[1000]"), 1e-5, "the sum of the processors");
            if (counts[k] == 1) {
                Check.isTrue(times >= 1.0, "one simple processor renders at least as fast as real time: " + times + " x");
            }
        }
    }
}
