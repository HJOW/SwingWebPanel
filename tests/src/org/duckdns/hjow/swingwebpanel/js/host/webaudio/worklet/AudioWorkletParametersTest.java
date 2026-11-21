package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The parameters of an audio worklet node (Web Audio API 1.32.3 and 1.32.4.3.1): the <code>AudioParamMap</code> that <code>node.parameters</code> returns (a read-only map in the order of the
 * descriptors, with the iteration methods), the <code>AudioParam</code> objects that carry the descriptor values (<code>defaultValue</code>, <code>minValue</code>, <code>maxValue</code>,
 * <code>automationRate</code>) and clamp to the nominal range, <code>parameterData</code> as the initial values, and what the processor receives in <code>parameters</code>: an a-rate parameter
 * under a ramp has a value for every frame and equals the closed form, a constant block has one value, a k-rate parameter has one value that is the value at the start of the quantum, and
 * the arrays are the same objects in successive calls until the number of values changes. An output of another node connected to a parameter drives it.
 * <p>
 * 오디오 worklet 노드의 파라미터(Web Audio API 1.32.3, 1.32.4.3.1): <code>node.parameters</code> 가 반환하는 <code>AudioParamMap</code>(설명의 순서를 따르는 읽기 전용 맵과 반복 메서드), 설명의 값을 가진
 * <code>AudioParam</code>(<code>defaultValue</code>, <code>minValue</code>, <code>maxValue</code>, <code>automationRate</code>)과 명목 범위로의 제한, 초기 값인 <code>parameterData</code>,
 * 프로세서가 <code>parameters</code> 로 받는 것: 램프 아래의 a-rate 파라미터는 프레임마다 값이 있고 닫힌 식과 같으며, 일정한 블록은 값이 하나이고, k-rate 파라미터는 quantum 시작의 값인 값 하나이며,
 * 값의 수가 바뀔 때까지 배열은 연속 호출에서 같은 객체이다. 다른 노드의 출력을 파라미터에 연결하면 그 파라미터를 구동한다.
 */
public class AudioWorkletParametersTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletParametersTest() {
    }

    /**
     * A module with a processor whose descriptors are given and whose process method writes what the test wants to see.
     * <p>
     * 설명이 주어지고 process 메서드가 시험이 보고 싶은 것을 쓰는 프로세서가 든 모듈.
     */
    private static final String PROBE = "class Probe extends AudioWorkletProcessor {\n" + "  static get parameterDescriptors() {\n" + "    return [\n"
            + "      {name: 'a', defaultValue: 0, minValue: -10, maxValue: 10, automationRate: 'a-rate'},\n"
            + "      {name: 'k', defaultValue: 0.5, minValue: -10, maxValue: 10, automationRate: 'k-rate'}\n" + "    ];\n" + "  }\n"
            + "  constructor() { super(); this.last = null; this.same = 0; this.calls = 0; }\n"
            + "  process(inputs, outputs, parameters) {\n" + "    const o = outputs[0];\n" + "    const a = parameters.a, k = parameters.k;\n"
            + "    this.calls++;\n" + "    if (this.last === a) this.same++;\n" + "    this.last = a;\n"
            + "    for (let i = 0; i < o[0].length; i++) {\n" + "      o[0][i] = a.length > 1 ? a[i] : a[0];\n" + "      o[1][i] = k[0];\n"
            + "      o[2][i] = a.length;\n" + "      o[3][i] = this.same;\n" + "    }\n" + "    return true;\n" + "  }\n" + "}\n" + "registerProcessor('probe', Probe);\n";

    /**
     * Renders 8 quanta of the probe through a destination of four channels; the setup statements (<code>node</code> is the probe node) run before the rendering.
     * <p>
     * 채널이 넷인 목적지로 프로브를 8 quantum 렌더링한다. 설정 문장(<code>node</code> 는 프로브 노드)은 렌더링 전에 실행된다.
     *
     * @param p     the page / 페이지
     * @param setup the statements / 문장
     */
    private static void renderProbe(WorkletPage p, String setup) {
        renderProbe(p, "", setup);
    }

    /**
     * Renders the probe with extra members in the options of the node.
     * <p>
     * 노드의 옵션에 멤버를 더해 프로브를 렌더링한다.
     *
     * @param p       the page / 페이지
     * @param options extra members of the options, each followed by a comma / 옵션에 더할 멤버(각각 쉼표로 끝남)
     * @param setup   the statements / 문장
     */
    private static void renderProbe(WorkletPage p, String options, String setup) {
        p.module("probe.js", PROBE);
        p.run("var out = null; var err = null; var ctx = new OfflineAudioContext(4, 1024, 8000);"
                + "ctx.audioWorklet.addModule('probe.js').then(function () { var node = new AudioWorkletNode(ctx, 'probe', {" + options + "numberOfInputs: 0, outputChannelCount: [4]}); window.node = node;"
                + setup + " node.connect(ctx.destination); return ctx.startRendering(); }).then(function (b) { out = b; }, function (e) { err = e.name + ': ' + e.message; });");
        p.waitFor("out !== null || err !== null", 30000, "the rendering");
        Check.equal("null", p.run("String(err)"), "no error " + p.console());
    }

    /**
     * Reads a channel of the rendered buffer.
     * <p>
     * 렌더링한 버퍼의 채널을 읽는다.
     *
     * @param p       the page / 페이지
     * @param channel the channel / 채널
     * @return the samples / 표본
     */
    private static double[] channel(WorkletPage p, int channel) {
        String json = p.run("JSON.stringify(Array.prototype.slice.call(out.getChannelData(" + channel + ")))");
        String[] parts = json.substring(1, json.length() - 1).split(",");
        double[] v = new double[parts.length];
        // 항목마다 숫자로 읽는다.
        for (int i = 0; i < v.length; i++) {
            v[i] = Double.parseDouble(parts[i]);
        }
        return v;
    }

    /**
     * A linear ramp on the a-rate parameter and on the k-rate parameter: a has a value per frame, equal to the closed form (0 to 4 over the first 512 frames); k is one value per quantum,
     * the ramp value (0.5 to 4.5) at the first frame of the quantum. After the ramp the block is constant and a has one value; the array is then the same object in successive calls.
     * <p>
     * a-rate 파라미터와 k-rate 파라미터의 선형 램프: a 는 프레임마다 값이 있고 닫힌 식과 같다(처음 512 프레임에 0 에서 4). k 는 quantum 마다 값 하나이며 quantum 첫 프레임의 램프 값(0.5 에서 4.5)이다.
     * 램프가 끝난 뒤의 블록은 일정하고 a 는 값 하나이다. 그때 배열은 연속 호출에서 같은 객체이다.
     */
    public void testRampsOfRateAAndRateK() {
        WorkletPage p = new WorkletPage();
        renderProbe(p, "node.parameters.get('a').setValueAtTime(0, 0); node.parameters.get('a').linearRampToValueAtTime(4, 512 / 8000);"
                + "node.parameters.get('k').setValueAtTime(0.5, 0); node.parameters.get('k').linearRampToValueAtTime(4.5, 512 / 8000);");
        double[] a = channel(p, 0);
        double[] k = channel(p, 1);
        double[] lengths = channel(p, 2);
        double[] same = channel(p, 3);
        // 램프 동안은 프레임마다 닫힌 식과 같아야 하고, 그 뒤에는 4 로 유지되어야 한다.
        for (int i = 0; i < 1024; i++) {
            Check.near(i < 512 ? 4.0 * i / 512.0 : 4.0, a[i], 1e-5, "a at frame " + i);
        }
        // k-rate 는 quantum 마다 첫 프레임의 값이다.
        for (int q = 0; q < 8; q++) {
            double expected = q < 4 ? 0.5 + 4.0 * (q * 128) / 512.0 : 4.5;
            // quantum 안의 모든 프레임이 같은 값이어야 한다.
            for (int i = 0; i < 128; i++) {
                Check.near(expected, k[q * 128 + i], 1e-5, "k in quantum " + q);
            }
        }
        // 램프 동안은 배열 길이 128, 램프가 끝난 뒤는 1 이어야 한다.
        for (int q = 0; q < 8; q++) {
            Check.near(q < 4 ? 128 : 1, lengths[q * 128], 0, "number of values of a in quantum " + q);
        }
        // 값의 수가 바뀌기 전까지(0~3 quantum, 4~7 quantum)는 같은 배열 객체이다: 1~3 과 5~7 quantum 에서 같은 객체가 3 + 3 번 보인다.
        Check.near(6, same[1023], 0, "the array of a is the same object while its length does not change (3 + 3 calls)");
    }

    /**
     * The defaults and the initial values: a parameter has its descriptor value without any automation, <code>parameterData</code> sets <code>value</code>, a name that is no parameter is ignored.
     * <p>
     * 기본값과 초기 값: 파라미터는 자동화 없이도 설명의 값을 가지고, <code>parameterData</code> 가 <code>value</code> 를 정하며, 파라미터가 아닌 이름은 무시한다.
     */
    public void testDefaultsAndParameterData() {
        WorkletPage p = new WorkletPage();
        renderProbe(p, "");
        double[] a = channel(p, 0);
        double[] k = channel(p, 1);
        Check.near(0.0, a[500], 0, "the default of a");
        Check.near(0.5, k[500], 1e-7, "the default of k");

        // parameterData 는 초기 값을 정하고, 파라미터가 아닌 이름(unknown)은 무시한다.
        WorkletPage q = new WorkletPage();
        renderProbe(q, "parameterData: {a: 2.5, k: -3, unknown: 9}, ", "");
        Check.near(2.5, channel(q, 0)[500], 1e-7, "parameterData sets a");
        Check.near(-3.0, channel(q, 1)[500], 1e-7, "parameterData sets k");
        Check.equal("2.5", q.run("String(node.parameters.get('a').value)"), "the value attribute");
    }

    /**
     * What the script sees of <code>node.parameters</code>: the interface, the order, the iteration methods, the descriptor values on the parameter objects and the clamping to the nominal range.
     * <p>
     * 스크립트가 <code>node.parameters</code> 에서 보는 것: 인터페이스, 순서, 반복 메서드, 파라미터 객체의 설명 값, 명목 범위로의 제한.
     */
    public void testParameterMapInterface() {
        WorkletPage p = new WorkletPage();
        renderProbe(p, "");
        Check.equal("AudioParamMap", p.run("Object.prototype.toString.call(node.parameters).slice(8, -1)"), "the interface name");
        Check.equal("true", p.run("String(node.parameters === node.parameters)"), "the same object every time");
        Check.equal("2", p.run("String(node.parameters.size)"), "size");
        Check.equal("true", p.run("String(node.parameters.has('a') && node.parameters.has('k') && !node.parameters.has('x'))"), "has");
        Check.equal("undefined", p.run("String(node.parameters.get('x'))"), "get of an unknown name");
        Check.equal("true", p.run("String(node.parameters.get('a') instanceof AudioParam && node.parameters.get('a') === node.parameters.get('a'))"), "get returns the AudioParam");
        Check.equal("a,k", p.run("Array.from(node.parameters.keys()).join(',')"), "keys in the order of the descriptors");
        Check.equal("a,k", p.run("Array.from(node.parameters.values()).map(function (v) { return v === node.parameters.get('a') ? 'a' : 'k'; }).join(',')"), "values");
        Check.equal("a:true,k:true", p.run("Array.from(node.parameters.entries()).map(function (e) { return e[0] + ':' + (e[1] === node.parameters.get(e[0])); }).join(',')"), "entries");
        Check.equal("a,k", p.run("var seen = []; for (var e of node.parameters) { seen.push(e[0]); } seen.join(',')"), "the map is iterable");
        Check.equal("a,k,true", p.run("var each = []; var map = node.parameters; map.forEach(function (v, key, m) { each.push(key); each.last = (m === map); }); each.join(',') + ',' + each.last"), "forEach");
        Check.equal("-10", p.run("String(node.parameters.get('a').minValue)"), "minValue of the descriptor");
        Check.equal("10", p.run("String(node.parameters.get('a').maxValue)"), "maxValue of the descriptor");
        Check.equal("0.5", p.run("String(node.parameters.get('k').defaultValue)"), "defaultValue of the descriptor");
        Check.equal("k-rate", p.run("node.parameters.get('k').automationRate"), "automationRate of the descriptor");
        Check.equal("a-rate", p.run("node.parameters.get('a').automationRate"), "the default automation rate is a-rate");
        Check.equal("TypeError", p.errorOf("node.parameters.forEach(5)"), "forEach needs a function");
        Check.equal("TypeError", p.errorOf("AudioParamMap.prototype.get.call({}, 'a')"), "illegal invocation");
        // 명목 범위(-10..10)를 넘는 값은 파라미터가 계산할 때 제한한다: value 속성은 대입한 값을 그대로 돌려준다.
        p.run("node.parameters.get('a').value = 25;");
        Check.equal("25", p.run("String(node.parameters.get('a').value)"), "value keeps the assigned number");
    }

    /**
     * The nominal range clamps what the processor sees: a parameter set to 25 (range -10..10) is 10 in the processor.
     * <p>
     * 명목 범위는 프로세서가 보는 값을 제한한다: 25 로 정한 파라미터(범위 -10..10)는 프로세서에서 10 이다.
     */
    public void testNominalRangeClampsWhatTheProcessorSees() {
        WorkletPage p = new WorkletPage();
        renderProbe(p, "node.parameters.get('a').value = 25; node.parameters.get('k').value = -50;");
        Check.near(10.0, channel(p, 0)[300], 1e-7, "a is clamped to its maximum");
        Check.near(-10.0, channel(p, 1)[300], 1e-7, "k is clamped to its minimum");
    }

    /**
     * An output of another node connected to a parameter drives it: a constant source of 2 added to the value 1 of an a-rate parameter gives 3, sample by sample, and the processor sees an array of
     * a value per frame.
     * <p>
     * 다른 노드의 출력을 파라미터에 연결하면 그 파라미터를 구동한다: a-rate 파라미터의 값 1 에 상수 원천 2 를 더하면 3 이고, 프로세서는 프레임마다 값이 있는 배열을 본다.
     */
    public void testAnOutputDrivesAParameter() {
        WorkletPage p = new WorkletPage();
        renderProbe(p, "node.parameters.get('a').value = 1; var cs = new ConstantSourceNode(ctx, {offset: 2}); cs.connect(node.parameters.get('a')); cs.start();");
        double[] a = channel(p, 0);
        double[] lengths = channel(p, 2);
        // 모든 프레임이 1 + 2 이고 배열은 프레임마다 값을 가진다.
        for (int i = 0; i < 1024; i += 37) {
            Check.near(3.0, a[i], 1e-6, "a at frame " + i);
        }
        Check.near(128, lengths[600], 0, "the value of an a-rate parameter that is driven by an input differs per frame");
    }
}
