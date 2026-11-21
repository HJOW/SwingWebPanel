package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The arguments of <code>process()</code> and the channels of an audio worklet node (Web Audio API 1.32.3.3.2 and 1.32.4.3.1): several inputs and outputs (a processor with two inputs and two
 * outputs that sums and subtracts), the number of channels that the processor sees in an input (the mixed channels of <code>computedNumberOfChannels</code>, one channel when
 * <code>channelCount</code> is <code>explicit</code> and 1), an input that nothing is connected to (an empty array), the number of channels of an output (<code>outputChannelCount</code>, or that
 * of the input for a node with one input and one output, or one), the arrays that are frozen (<code>FrozenArray</code>) and whose <code>Float32Array</code> elements are writable, the arrays that are
 * the same objects in successive calls and are made again after the script transferred one of them, and the <code>sampleRate</code> and <code>currentTime</code> of the global scope.
 * <p>
 * <code>process()</code> 의 인수와 오디오 worklet 노드의 채널(Web Audio API 1.32.3.3.2, 1.32.4.3.1): 여러 입력과 출력(합과 차를 구하는 입력 둘 출력 둘의 프로세서), 프로세서가 입력에서 보는 채널 수
 * (<code>computedNumberOfChannels</code> 의 섞인 채널. <code>channelCount</code> 가 <code>explicit</code> 이고 1 이면 채널 하나), 아무것도 연결되지 않은 입력(빈 배열), 출력의 채널 수
 * (<code>outputChannelCount</code>, 입력 하나 출력 하나인 노드는 입력의 채널 수, 아니면 1), 얼려 있고(<code>FrozenArray</code>) <code>Float32Array</code> 요소는 쓸 수 있는 배열, 연속 호출에서 같은 객체이고
 * 스크립트가 하나를 전달한 뒤에는 다시 만드는 배열, 전역 범위의 <code>sampleRate</code> 와 <code>currentTime</code>.
 */
public class AudioWorkletChannelsTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletChannelsTest() {
    }

    /**
     * Loads a module, creates the graph that the statements describe in an offline context with the given channels and renders it; the page has <code>ctx</code>, <code>out</code> and <code>err</code>.
     * <p>
     * 모듈을 불러오고, 문장이 기술하는 그래프를 주어진 채널의 오프라인 컨텍스트에 만들어 렌더링한다. 페이지에는 <code>ctx</code>, <code>out</code>, <code>err</code> 가 있다.
     *
     * @param p        the page / 페이지
     * @param module   the text of the module / 모듈의 텍스트
     * @param channels the channels of the context / 컨텍스트의 채널
     * @param frames   the length of the rendering / 렌더링 길이
     * @param graph    the statements that build the graph (the last returns nothing; the rendering follows) / 그래프를 만드는 문장(렌더링은 그 뒤에 한다)
     */
    private static void render(WorkletPage p, String module, int channels, int frames, String graph) {
        p.module("m.js", module);
        p.run("var out = null; var err = null; var ctx = new OfflineAudioContext(" + channels + ", " + frames + ", 8000);"
                + "ctx.audioWorklet.addModule('m.js').then(function () { " + graph + " return ctx.startRendering(); }).then(function (b) { out = b; }, function (e) { err = e.name + ': ' + e.message; });");
        p.waitFor("out !== null || err !== null", 30000, "the rendering");
        Check.equal("null", p.run("String(err)"), "no error " + p.console());
    }

    /**
     * A processor with two inputs and two outputs: the first output is the sum of the inputs, the second their difference; two constant sources (0.75 and 0.25) feed the inputs, and the destination
     * with two channels shows the sum 1 and the difference 0.5 at every frame.
     * <p>
     * 입력 둘 출력 둘의 프로세서: 첫 출력은 입력의 합, 둘째는 차이다. 상수 원천 둘(0.75 와 0.25)이 입력을 채우고, 채널이 둘인 목적지가 모든 프레임에서 합 1 과 차 0.5 를 보여 준다.
     */
    public void testTwoInputsAndTwoOutputs() {
        WorkletPage p = new WorkletPage();
        render(p, "registerProcessor('mix', class extends AudioWorkletProcessor {\n  process(inputs, outputs) {\n    const a = inputs[0][0], b = inputs[1][0], s = outputs[0][0], d = outputs[1][0];\n"
                + "    if (!a || !b) return true;\n    for (let i = 0; i < s.length; i++) { s[i] = a[i] + b[i]; d[i] = a[i] - b[i]; }\n    return false;\n  }\n});\n", 2, 512,
                "var a = new ConstantSourceNode(ctx, {offset: 0.75}), b = new ConstantSourceNode(ctx, {offset: 0.25}); var n = new AudioWorkletNode(ctx, 'mix', {numberOfInputs: 2, numberOfOutputs: 2});"
                        + "a.connect(n, 0, 0); b.connect(n, 0, 1); a.start(); b.start(); var m = ctx.createChannelMerger(2); n.connect(m, 0, 0); n.connect(m, 1, 1); m.connect(ctx.destination);");
        // 모든 프레임에서 합 1, 차 0.5.
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0)).every(function (v) { return Math.abs(v - 1) < 1e-7; }))"), "the sum");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(1)).every(function (v) { return Math.abs(v - 0.5) < 1e-7; }))"), "the difference");
    }

    /**
     * The channels that the processor sees and writes: a mono source gives an input of one channel, a stereo buffer an input of two; an input whose <code>channelCount</code> is 1 in the mode
     * <code>explicit</code> shows one channel although the source has two; an unconnected input is an empty array; a node with one input and one output follows the channels of the input, and
     * <code>outputChannelCount</code> fixes them; the two outputs of a node with two inputs have one channel each. The processor reports the numbers to the console of the page.
     * <p>
     * 프로세서가 보고 쓰는 채널: 모노 원천은 채널이 하나인 입력을, 스테레오 버퍼는 둘인 입력을 준다. <code>channelCount</code> 가 1 이고 모드가 <code>explicit</code> 인 입력은 원천의 채널이 둘이어도 하나를
     * 보여 준다. 연결되지 않은 입력은 빈 배열이다. 입력 하나 출력 하나인 노드는 입력의 채널 수를 따르고 <code>outputChannelCount</code> 가 그것을 정한다. 입력이 둘인 노드의 두 출력은 각각 채널 하나이다.
     * 프로세서는 그 수를 페이지의 콘솔에 보고한다.
     */
    public void testTheChannelsOfInputsAndOutputs() {
        WorkletPage p = new WorkletPage();
        render(p, "let seen = {};\nregisterProcessor('probe', class extends AudioWorkletProcessor {\n  constructor(o) { super(o); this.tag = o.processorOptions.tag; this.done = false; }\n"
                + "  process(inputs, outputs) {\n    if (!this.done) { this.done = true; console.log(this.tag + ':' + inputs.map(function (x) { return x.length; }).join('/') + ':' + outputs.map(function (x) { return x.length; }).join('/')); }\n"
                + "    return true;\n  }\n});\n", 1, 128,
                "var stereo = ctx.createBuffer(2, 128, 8000); var mono = ctx.createBuffer(1, 128, 8000);"
                        + "function src(b) { var s = ctx.createBufferSource(); s.buffer = b; s.start(); return s; }"
                        + "var a = new AudioWorkletNode(ctx, 'probe', {processorOptions: {tag: 'mono-in'}}); src(mono).connect(a);"
                        + "var b = new AudioWorkletNode(ctx, 'probe', {processorOptions: {tag: 'stereo-in'}}); src(stereo).connect(b);"
                        + "var c = new AudioWorkletNode(ctx, 'probe', {channelCount: 1, channelCountMode: 'explicit', processorOptions: {tag: 'explicit-1'}}); src(stereo).connect(c);"
                        + "var d = new AudioWorkletNode(ctx, 'probe', {processorOptions: {tag: 'unconnected'}});"
                        + "var e = new AudioWorkletNode(ctx, 'probe', {outputChannelCount: [3], processorOptions: {tag: 'fixed-3'}}); src(mono).connect(e);"
                        + "var f = new AudioWorkletNode(ctx, 'probe', {numberOfInputs: 2, numberOfOutputs: 2, processorOptions: {tag: 'two-two'}}); src(stereo).connect(f, 0, 1);"
                        + "var g = new AudioWorkletNode(ctx, 'probe', {numberOfInputs: 0, numberOfOutputs: 3, outputChannelCount: [1, 2, 4], processorOptions: {tag: 'three-outs'}});"
                        + "var h = new AudioWorkletNode(ctx, 'probe', {numberOfOutputs: 0, processorOptions: {tag: 'no-outs'}}); src(stereo).connect(h);");
        java.util.List<String> lines = p.console();
        Check.equal(true, lines.contains("log: mono-in:1:1"), "a mono source: an input of 1 channel and an output that follows it: " + lines);
        Check.equal(true, lines.contains("log: stereo-in:2:2"), "a stereo source: 2 channels, and the output follows: " + lines);
        Check.equal(true, lines.contains("log: explicit-1:1:1"), "channelCount 1 explicit: one channel: " + lines);
        Check.equal(true, lines.contains("log: unconnected:0:1"), "an unconnected input is an empty array, the output has one channel: " + lines);
        Check.equal(true, lines.contains("log: fixed-3:1:3"), "outputChannelCount fixes the channels: " + lines);
        Check.equal(true, lines.contains("log: two-two:0/2:1/1"), "two inputs, two outputs of one channel each: the first input is unconnected: " + lines);
        Check.equal(true, lines.contains("log: three-outs::1/2/4"), "no inputs and three outputs with their own channels: " + lines);
        Check.equal(true, lines.contains("log: no-outs:2:"), "no outputs: " + lines);
    }

    /**
     * Every <code>Float32Array</code> of an output is filled with zero before every call (1.32.4.3.1): a processor that writes only in its first call leaves 0.5 in the first quantum and silence in the
     * later ones, not the values of the first call again.
     * <p>
     * 출력의 모든 <code>Float32Array</code> 는 호출마다 0 으로 채워진 채 주어진다(1.32.4.3.1): 첫 호출에서만 쓰는 프로세서는 첫 quantum 에 0.5 를 남기고 이후에는 무음이다. 첫 호출의 값이 다시 나오지 않는다.
     */
    public void testOutputsAreZeroFilledForEveryCall() {
        WorkletPage p = new WorkletPage();
        render(p, "registerProcessor('once', class extends AudioWorkletProcessor {\n  constructor() { super(); this.n = 0; }\n  process(inputs, outputs) { if (this.n++ === 0) outputs[0][0].fill(0.5); return true; }\n});\n", 1, 512,
                "var n = new AudioWorkletNode(ctx, 'once', {numberOfInputs: 0}); n.connect(ctx.destination);");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0), 0, 128).every(function (v) { return v === 0.5; }))"), "the first quantum");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0), 128).every(function (v) { return v === 0; }))"), "the later quanta are silent");
    }

    /**
     * The arrays: <code>inputs</code>, <code>outputs</code>, their elements and <code>parameters</code> are frozen, the <code>Float32Array</code> elements are not (a processor writes its outputs), the
     * arrays are the same objects in successive calls, <code>sampleRate</code> and <code>currentTime</code> of the global scope are those of the context, and a processor that transfers a channel
     * array with <code>postMessage</code> gets new arrays afterwards (the old one is detached, the processor goes on and its audio is complete).
     * <p>
     * 배열: <code>inputs</code>, <code>outputs</code>, 그 요소, <code>parameters</code> 는 얼려 있고 <code>Float32Array</code> 요소는 그렇지 않으며(프로세서가 출력에 쓴다), 배열은 연속 호출에서 같은 객체이고,
     * 전역 범위의 <code>sampleRate</code> 와 <code>currentTime</code> 은 컨텍스트의 것이며, 채널 배열을 <code>postMessage</code> 로 전달한 프로세서는 그 뒤에 새 배열을 받는다(옛 것은 분리되고, 프로세서는
     * 계속하며 오디오는 온전하다).
     */
    public void testTheFrozenArraysAndTheirReuse() {
        WorkletPage p = new WorkletPage();
        render(p, "let last = null, same = 0, calls = 0, report = null;\nregisterProcessor('arrays', class extends AudioWorkletProcessor {\n  static get parameterDescriptors() { return [{name: 'x', defaultValue: 1}]; }\n"
                + "  process(inputs, outputs, parameters) {\n    calls++;\n    const o = outputs[0][0];\n    if (calls === 1) {\n      let pushFailed = false; try { inputs[0].push(1); } catch (e) { pushFailed = e.name; }\n"
                + "      report = [Object.isFrozen(inputs), Object.isFrozen(inputs[0]), Object.isFrozen(outputs), Object.isFrozen(outputs[0]), Object.isFrozen(parameters), Object.isFrozen(o), pushFailed, sampleRate, currentTime].join();\n"
                + "    }\n    const prev = last; last = outputs[0][0]; if (prev === last) same++;\n    o.fill(0.5);\n    if (calls === 3) { const buf = o.buffer; this.port.postMessage(buf, [buf]); }\n"
                + "    if (calls === 4) { report += ';' + [o.length, o.buffer.byteLength, prev === o, same].join(); console.log(report); }\n"
                + "    return true;\n  }\n});\n", 1, 768, "var n = new AudioWorkletNode(ctx, 'arrays', {numberOfInputs: 0}); n.port.onmessage = function () {}; n.connect(ctx.destination);");
        Check.equal("[log: true,true,true,true,true,false,TypeError,8000,0;128,512,false,2]", p.console().toString(),
                "frozen arrays, the writable elements and the reuse: " + p.console());
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0)).every(function (v) { return v === 0.5; }))"), "the audio is complete although an array was transferred");
    }
}
