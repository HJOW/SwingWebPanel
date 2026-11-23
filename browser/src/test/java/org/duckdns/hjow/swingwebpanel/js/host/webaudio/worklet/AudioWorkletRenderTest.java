package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The audio that JavaScript processors produce, through the whole script path (plan item P25-06): a module is loaded with <code>audioWorklet.addModule()</code>, a processor class
 * registered with <code>registerProcessor()</code> becomes an <code>AudioWorkletNode</code>, and an <code>OfflineAudioContext</code> renders the graph, which is deterministic. The expected
 * samples are closed forms that the test computes itself: a gain on a constant, a sine generator that reads <code>currentFrame</code> and <code>sampleRate</code> of the worklet global scope,
 * the input copied to the output (the bypass processor of the specification), a processor with a-rate and k-rate parameters under a linear ramp, several inputs and outputs, outputs with a number
 * of channels of their own, and what a processor that returns <code>false</code> does.
 * <p>
 * JavaScript 프로세서가 만드는 오디오를 전체 스크립트 경로로 시험한다(계획 P25-06): <code>audioWorklet.addModule()</code> 로 모듈을 불러오고, <code>registerProcessor()</code> 로 등록한 프로세서 클래스가
 * <code>AudioWorkletNode</code> 가 되며, <code>OfflineAudioContext</code> 가 그래프를 렌더링한다(결정적이다). 기대하는 표본은 시험이 직접 계산한 닫힌 식이다: 상수에 대한 이득, worklet 전역 범위의
 * <code>currentFrame</code> 과 <code>sampleRate</code> 를 읽는 사인 생성기, 입력을 출력으로 복사(규격의 bypass 프로세서), 선형 램프를 따르는 a-rate 와 k-rate 파라미터가 있는 프로세서, 여러 입력과
 * 출력, 자기 채널 수를 가진 출력, <code>false</code> 를 반환하는 프로세서의 동작.
 */
public class AudioWorkletRenderTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletRenderTest() {
    }

    /**
     * Starts a script that runs the steps in a worklet and stores the rendered buffer in <code>out</code> (or the error in <code>err</code>), and waits for it.
     * <p>
     * worklet 에서 단계를 실행하고 렌더링한 버퍼를 <code>out</code> 에(또는 오류를 <code>err</code> 에) 저장하는 스크립트를 시작하고 기다린다.
     *
     * @param p      the page / 페이지
     * @param setup  the statements before the module is added (the context is <code>ctx</code>) / 모듈을 추가하기 전의 문장(컨텍스트는 <code>ctx</code>)
     * @param module the file name of the module / 모듈의 파일 이름
     * @param steps  the statements that run when the module is loaded; the last one returns the promise of the rendering / 모듈을 불러오면 실행하는 문장. 마지막은 렌더링의 프라미스를 반환한다
     */
    private static void render(WorkletPage p, String setup, String module, String steps) {
        p.run("var out = null; var err = null; " + setup + " ctx.audioWorklet.addModule('" + module + "').then(function () { " + steps + " }).then(function (b) { out = b; }, "
                + "function (e) { err = e.name + ': ' + e.message; });");
        p.waitFor("out !== null || err !== null", 30000, "the rendering");
        Check.equal("null", p.run("String(err)"), "no error " + p.console());
    }

    /**
     * Reads a channel of the rendered buffer as numbers.
     * <p>
     * 렌더링한 버퍼의 채널을 수로 읽는다.
     *
     * @param p       the page / 페이지
     * @param channel the channel / 채널
     * @return the samples / 표본
     */
    private static double[] samples(WorkletPage p, int channel) {
        String json = p.run("JSON.stringify(Array.prototype.slice.call(out.getChannelData(" + channel + ")))");
        String body = json.substring(1, json.length() - 1);
        if (body.length() == 0) {
            return new double[0];
        }
        String[] parts = body.split(",");
        double[] v = new double[parts.length];
        // 항목마다 숫자로 읽는다.
        for (int i = 0; i < v.length; i++) {
            v[i] = Double.parseDouble(parts[i]);
        }
        return v;
    }

    /**
     * A gain on the input (an a-rate parameter with a default and a range): a constant of 0.5 through the gain 0.25 is 0.125 in every frame.
     * <p>
     * 입력에 대한 이득(기본값과 범위가 있는 a-rate 파라미터): 상수 0.5 가 이득 0.25 를 지나면 모든 프레임에서 0.125 이다.
     */
    public void testGainProcessor() {
        WorkletPage p = new WorkletPage();
        p.module("gain.js", "class Gain extends AudioWorkletProcessor {\n" + "  static get parameterDescriptors() { return [{name: 'gain', defaultValue: 0.5, minValue: 0, maxValue: 1}]; }\n"
                + "  process(inputs, outputs, parameters) {\n" + "    const input = inputs[0], output = outputs[0], g = parameters.gain;\n"
                + "    for (let c = 0; c < output.length; c++) for (let i = 0; i < output[c].length; i++) output[c][i] = input[c][i] * (g.length > 1 ? g[i] : g[0]);\n"
                + "    return true;\n" + "  }\n" + "}\n" + "registerProcessor('gain', Gain);\n");
        render(p, "var ctx = new OfflineAudioContext(1, 512, 8000);", "gain.js", "var src = new ConstantSourceNode(ctx, {offset: 0.5});"
                + "var node = new AudioWorkletNode(ctx, 'gain', {parameterData: {gain: 0.25}}); src.connect(node); node.connect(ctx.destination); src.start();"
                + "return ctx.startRendering();");
        double[] a = samples(p, 0);
        Check.equal(512, a.length, "the length of the rendering");
        // 모든 프레임이 0.5 * 0.25 이어야 한다.
        for (int i = 0; i < a.length; i++) {
            Check.near(0.125, a[i], 1e-7, "frame " + i);
        }
    }

    /**
     * A sine generator without inputs that reads <code>currentFrame</code> and <code>sampleRate</code> of the global scope: every sample is the closed form, from frame 0.
     * <p>
     * 입력이 없고 전역 범위의 <code>currentFrame</code> 과 <code>sampleRate</code> 를 읽는 사인 생성기: 모든 표본이 첫 프레임부터 닫힌 식이다.
     */
    public void testSineGenerator() {
        WorkletPage p = new WorkletPage();
        p.module("sine.js", "class Sine extends AudioWorkletProcessor {\n" + "  process(inputs, outputs) {\n" + "    const o = outputs[0][0];\n"
                + "    for (let i = 0; i < o.length; i++) o[i] = Math.sin(2 * Math.PI * 250 * (currentFrame + i) / sampleRate);\n" + "    return true;\n" + "  }\n" + "}\n"
                + "registerProcessor('sine', Sine);\n");
        render(p, "var ctx = new OfflineAudioContext(1, 1024, 8000);", "sine.js", "var n = new AudioWorkletNode(ctx, 'sine', {numberOfInputs: 0}); n.connect(ctx.destination);"
                + "return ctx.startRendering();");
        double[] a = samples(p, 0);
        Check.equal(1024, a.length, "the length");
        // 프레임마다 닫힌 식과 비교한다. float 저장의 오차는 1e-6 미만이다.
        for (int i = 0; i < a.length; i++) {
            Check.near(Math.sin(2 * Math.PI * 250.0 * i / 8000.0), a[i], 1e-6, "frame " + i);
        }
    }

    /**
     * The bypass processor of the specification (1.32.1): it copies the first input to the first output and returns <code>false</code>, so it runs only while its input is active.
     * A source that stops at frame 256 leaves silence after that, and the processor is not called any more (its counter, kept in the global scope, shows it).
     * <p>
     * 규격의 bypass 프로세서(1.32.1): 첫 입력을 첫 출력에 복사하고 <code>false</code> 를 반환하므로 입력이 활성인 동안만 실행된다. 프레임 256 에서 끝나는 원천 뒤에는 무음이며 프로세서는 더 이상
     * 호출되지 않는다(전역 범위에 둔 계수기가 보여 준다).
     */
    public void testBypassProcessorRunsOnlyWhileItsInputIsActive() {
        WorkletPage p = new WorkletPage();
        p.module("bypass.js", "var calls = 0;\n" + "class Bypass extends AudioWorkletProcessor {\n" + "  process(inputs, outputs) {\n" + "    calls++;\n"
                + "    const input = inputs[0], output = outputs[0];\n" + "    for (let c = 0; c < output.length; c++) output[c].set(input[c]);\n" + "    return false;\n" + "  }\n" + "}\n"
                + "registerProcessor('bypass', Bypass);\n" + "registerProcessor('calls', class extends AudioWorkletProcessor { process() { return false; } });\n");
        render(p, "var ctx = new OfflineAudioContext(1, 1024, 8000);", "bypass.js", "var src = new ConstantSourceNode(ctx, {offset: 0.75}); var node = new AudioWorkletNode(ctx, 'bypass');"
                + "src.connect(node); node.connect(ctx.destination); src.start(0); src.stop(256 / 8000); return ctx.startRendering();");
        double[] a = samples(p, 0);
        // 원천이 도는 동안(프레임 0..255)은 0.75, 그 뒤에는 무음이어야 한다.
        for (int i = 0; i < 256; i++) {
            Check.near(0.75, a[i], 1e-7, "frame " + i);
        }
        // 원천이 끝난 뒤에는 정확히 0 이어야 한다.
        for (int i = 256; i < 1024; i++) {
            Check.near(0.0, a[i], 0, "frame " + i);
        }
    }
}
