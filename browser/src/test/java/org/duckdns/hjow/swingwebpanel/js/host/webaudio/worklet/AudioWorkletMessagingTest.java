package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The message channel between an <code>AudioWorkletNode</code> and its processor (<code>node.port</code> and <code>this.port</code>, Web Audio API 1.32.3.2 and 1.32.4.2) and the options that reach the
 * constructor of the processor (1.32.2.3): messages are copied with the structured clone algorithm in both directions (objects, arrays, <code>Map</code>, <code>Date</code>, a transferred
 * <code>ArrayBuffer</code>), arrive in the order in which they were sent, and a message that is posted before an offline rendering starts is delivered before the first quantum (the result of the
 * rendering does not depend on timing); the processor can send a message from <code>process()</code>; a closed port delivers nothing; the options that the script gave arrive deserialized in the
 * realm of the worklet, with the defaults that the specification fills in. A processor is constructed when its context renders (a control message of the first quantum), so the conversations run in a
 * real-time context, which renders all the time; the determinism of an offline context is checked with the messages that are posted before the rendering.
 * <p>
 * <code>AudioWorkletNode</code> 와 프로세서 사이의 메시지 채널(<code>node.port</code> 와 <code>this.port</code>. Web Audio API 1.32.3.2, 1.32.4.2)과 프로세서의 생성자에 닿는 옵션(1.32.2.3): 메시지는 양방향으로
 * 구조화 복제 알고리즘으로 복사되고(객체, 배열, <code>Map</code>, <code>Date</code>, 전달한 <code>ArrayBuffer</code>), 보낸 순서대로 도착하며, 오프라인 렌더링이 시작되기 전에 보낸 메시지는 첫 quantum 전에
 * 전달된다(렌더링 결과가 타이밍에 의존하지 않는다). 프로세서는 <code>process()</code> 에서 메시지를 보낼 수 있고, 닫힌 포트는 아무것도 전달하지 않으며, 스크립트가 준 옵션은 규격이 채우는 기본값과 함께
 * worklet 의 realm 에 역직렬화되어 도착한다. 프로세서는 컨텍스트가 렌더링할 때(첫 quantum 의 제어 메시지) 만들어지므로, 대화는 늘 렌더링하는 실시간 컨텍스트에서 시험하고, 오프라인 컨텍스트의 결정성은
 * 렌더링 전에 보낸 메시지로 확인한다.
 */
public class AudioWorkletMessagingTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletMessagingTest() {
    }

    /**
     * A processor that answers messages and reports its options: <code>gain</code> sets the constant that it writes, <code>options</code> describes what its constructor received, <code>bytes</code>
     * sums a transferred buffer, <code>count</code> answers the number of messages so far, <code>report</code> makes it post the frame of every call of <code>process()</code>.
     * <p>
     * 메시지에 답하고 자기 옵션을 보고하는 프로세서: <code>gain</code> 은 쓰는 상수를 정하고, <code>options</code> 는 생성자가 받은 것을 기술하고, <code>bytes</code> 는 전달받은 버퍼의 합을 구하고,
     * <code>count</code> 는 지금까지의 메시지 수로 답하고, <code>report</code> 는 <code>process()</code> 를 부를 때마다 프레임을 보내게 한다.
     */
    private static final String ECHO = "class Echo extends AudioWorkletProcessor {\n" + "  constructor(options) {\n" + "    super(options);\n"
            + "    this.opts = options; this.gain = 1; this.n = 0; this.report = false;\n" + "    this.port.onmessage = (e) => {\n" + "      this.n++;\n" + "      const d = e.data;\n"
            + "      if (d.type === 'gain') { this.gain = d.value; this.port.postMessage({type: 'ack', value: d.value}); }\n" + "      else if (d.type === 'report') { this.report = d.on; }\n"
            + "      else if (d.type === 'options') {\n" + "        const o = this.opts, p = o.processorOptions;\n"
            + "        this.port.postMessage({type: 'options', text: [o.numberOfInputs, o.numberOfOutputs, o.outputChannelCount ? o.outputChannelCount.join('|') : '-', JSON.stringify(p.a), p.m.get(1), p.d.getTime(),\n"
            + "          typeof o.parameterData, Object.keys(o).sort().join('|')].join(';')});\n" + "      }\n"
            + "      else if (d.type === 'bytes') { let s = 0; const v = new Uint8Array(d.buf); for (let i = 0; i < v.length; i++) s += v[i]; this.port.postMessage({type: 'bytes', sum: s, length: d.buf.byteLength}); }\n"
            + "      else if (d.type === 'count') { this.port.postMessage({type: 'count', n: this.n, same: e instanceof MessageEvent, origin: e.origin, source: String(e.source)}); }\n"
            + "      else if (d.type === 'echo') { this.port.postMessage({type: 'echo', n: d.n}); }\n" + "    };\n" + "  }\n" + "  process(inputs, outputs) {\n"
            + "    if (this.report) this.port.postMessage({type: 'frame', frame: currentFrame});\n" + "    const o = outputs[0][0];\n" + "    for (let i = 0; i < o.length; i++) o[i] = this.gain;\n"
            + "    return true;\n" + "  }\n" + "}\n" + "registerProcessor('echo', Echo);\n";

    /**
     * Creates a page with the echo processor loaded; <code>node</code> is a node without inputs and <code>msgs</code> collects the messages that the page receives.
     * <p>
     * 에코 프로세서를 불러온 페이지를 만든다. <code>node</code> 는 입력이 없는 노드이고 <code>msgs</code> 는 페이지가 받는 메시지를 모은다.
     *
     * @param context     the construction of the context, the text of an expression / 컨텍스트의 생성(식 문자열)
     * @param nodeOptions the options of the node, the text of an object literal / 노드의 옵션(객체 리터럴 문자열)
     * @return the page / 페이지
     */
    private static WorkletPage echoPage(String context, String nodeOptions) {
        WorkletPage p = new WorkletPage();
        p.module("echo.js", ECHO);
        p.run("var ctx = " + context + "; var msgs = []; var ready = false; var node = null;"
                + "ctx.audioWorklet.addModule('echo.js').then(function () { node = new AudioWorkletNode(ctx, 'echo', " + nodeOptions + "); node.port.onmessage = function (e) { msgs.push(e.data); };"
                + "node.connect(ctx.destination); ready = true; });");
        p.waitFor("ready", 20000, "the module and the node");
        return p;
    }

    /**
     * Creates the page for a conversation: a real-time context, which renders all the time.
     * <p>
     * 대화를 위한 페이지를 만든다: 늘 렌더링하는 실시간 컨텍스트.
     *
     * @param nodeOptions the options of the node / 노드의 옵션
     * @return the page / 페이지
     */
    private static WorkletPage live(String nodeOptions) {
        return echoPage("new AudioContext()", nodeOptions);
    }

    /**
     * Renders the context of an offline page and waits; the buffer is in <code>out</code>.
     * <p>
     * 오프라인 페이지의 컨텍스트를 렌더링하고 기다린다. 버퍼는 <code>out</code> 에 있다.
     *
     * @param p the page / 페이지
     */
    private static void render(WorkletPage p) {
        p.run("var out = null; ctx.startRendering().then(function (b) { out = b; });");
        p.waitFor("out !== null", 30000, "the rendering");
    }

    /**
     * Messages go both ways: a message is answered by the processor, with the value that it got, and the answer reaches the page as an object that the page can use.
     * <p>
     * 메시지는 양방향으로 간다: 프로세서가 메시지에 답하고, 받은 값과 함께 온 답이 페이지가 쓸 수 있는 객체로 페이지에 닿는다.
     */
    public void testAMessageIsAnswered() {
        WorkletPage p = live("{numberOfInputs: 0}");
        p.run("node.port.postMessage({type: 'gain', value: 0.25});");
        p.waitFor("msgs.some(function (m) { return m.type === 'ack'; })", 20000, "the answer of the processor");
        Check.equal("0.25", p.run("String(msgs.filter(function (m) { return m.type === 'ack'; })[0].value)"), "the value in the answer");
        p.run("ctx.close();");
    }

    /**
     * A message that is posted before the rendering starts is delivered before the first quantum: the processor writes the gain that the message set, from the first frame, on every run. That is what makes
     * an offline rendering deterministic. The messages that the processor posted from <code>process()</code> reach the page after the rendering, one per quantum, with the frame counter of each quantum.
     * <p>
     * 렌더링이 시작되기 전에 보낸 메시지는 첫 quantum 전에 전달된다: 프로세서는 메시지가 정한 이득을 첫 프레임부터 쓰며 실행할 때마다 그렇다. 이것이 오프라인 렌더링을 결정적으로 만든다. 프로세서가
     * <code>process()</code> 에서 보낸 것은 렌더링 뒤에 페이지에 닿으며 quantum 마다 하나이고 각 quantum 의 프레임 계수와 함께이다.
     */
    public void testMessagesBeforeTheRenderingAreDeliveredBeforeTheFirstQuantum() {
        WorkletPage p = echoPage("new OfflineAudioContext(1, 1024, 8000)", "{numberOfInputs: 0}");
        p.run("node.port.postMessage({type: 'gain', value: 0.5}); node.port.postMessage({type: 'report', on: true});");
        render(p);
        Check.equal("0.5", p.run("String(out.getChannelData(0)[0])"), "the gain applies from the first frame");
        Check.equal("0.5", p.run("String(out.getChannelData(0)[1023])"), "and to the last");
        p.waitFor("msgs.filter(function (m) { return m.type === 'frame'; }).length === 8", 20000, "the messages that process() posted");
        Check.equal("0,128,256,384,512,640,768,896", p.run("msgs.filter(function (m) { return m.type === 'frame'; }).map(function (m) { return m.frame; }).join()"),
                "one message per quantum with the frame counter of the global scope");
    }

    /**
     * The options reach the constructor in the realm of the worklet: the defaults that the dictionary has (<code>numberOfInputs</code>, <code>numberOfOutputs</code>), <code>outputChannelCount</code>
     * and <code>processorOptions</code> with objects, arrays, a <code>Map</code> and a <code>Date</code>; a member that the script did not give is not there.
     * <p>
     * 옵션은 worklet 의 realm 에서 생성자에 닿는다: 사전의 기본값(<code>numberOfInputs</code>, <code>numberOfOutputs</code>), <code>outputChannelCount</code>, 객체, 배열, <code>Map</code>,
     * <code>Date</code> 가 든 <code>processorOptions</code>. 스크립트가 주지 않은 멤버는 없다.
     */
    public void testTheOptionsReachTheConstructor() {
        WorkletPage p = live("{numberOfInputs: 0, numberOfOutputs: 1, outputChannelCount: [1], processorOptions: {a: [1, 2, {b: 'x'}], m: new Map([[1, 'one']]), d: new Date(86400000)}}");
        p.run("node.port.postMessage({type: 'options'});");
        p.waitFor("msgs.some(function (m) { return m.type === 'options'; })", 20000, "the description of the options");
        Check.equal("0;1;1;[1,2,{\"b\":\"x\"}];one;86400000;undefined;numberOfInputs|numberOfOutputs|outputChannelCount|processorOptions",
                p.run("msgs.filter(function (m) { return m.type === 'options'; })[0].text"), "the options that the constructor received");
        p.run("ctx.close();");
    }

    /**
     * The messages arrive in the order in which they were sent, none is lost: 200 messages that the page posts are answered 200 times, in order.
     * <p>
     * 메시지는 보낸 순서대로 도착하며 하나도 잃지 않는다: 페이지가 보낸 메시지 200 개에 200 번, 순서대로 답한다.
     */
    public void testTheOrderOfMessages() {
        WorkletPage p = live("{numberOfInputs: 0}");
        p.run("for (var i = 0; i < 200; i++) { node.port.postMessage({type: 'echo', n: i}); }");
        p.waitFor("msgs.filter(function (m) { return m.type === 'echo'; }).length === 200", 30000, "200 answers");
        StringBuilder expected = new StringBuilder();
        // 0 부터 199 까지의 순서를 만든다.
        for (int i = 0; i < 200; i++) {
            expected.append(i == 0 ? "" : ",").append(i);
        }
        Check.equal(expected.toString(), p.run("msgs.filter(function (m) { return m.type === 'echo'; }).map(function (m) { return m.n; }).join()"), "the answers are in order");
        p.run("ctx.close();");
    }

    /**
     * A transferred <code>ArrayBuffer</code> arrives with its bytes and is detached on the page (the transfer list of <code>postMessage</code>); a <code>MessageEvent</code> in the worklet carries an empty
     * origin and no source, as for a channel.
     * <p>
     * 전달한 <code>ArrayBuffer</code> 는 바이트와 함께 도착하고 페이지에서는 분리된다(<code>postMessage</code> 의 전달 목록). worklet 의 <code>MessageEvent</code> 는 채널에서처럼 오리진이 비고 소스가 없다.
     */
    public void testTransferAndTheMessageEvent() {
        WorkletPage p = live("{numberOfInputs: 0}");
        p.run("var ab = new ArrayBuffer(8); var u = new Uint8Array(ab); for (var i = 0; i < 8; i++) u[i] = i + 1; node.port.postMessage({type: 'bytes', buf: ab}, [ab]);"
                + "node.port.postMessage({type: 'count'});");
        p.waitFor("msgs.some(function (m) { return m.type === 'bytes'; }) && msgs.some(function (m) { return m.type === 'count'; })", 20000, "the answers");
        Check.equal("36,8", p.run("var m = msgs.filter(function (m) { return m.type === 'bytes'; })[0]; m.sum + ',' + m.length"), "the bytes that arrived");
        Check.equal("0", p.run("String(ab.byteLength)"), "the buffer is detached on the page");
        Check.equal("2,true,,null", p.run("var c = msgs.filter(function (m) { return m.type === 'count'; })[0]; [c.n, c.same, c.origin, c.source].join()"), "the message event in the worklet");
        p.run("ctx.close();");
    }

    /**
     * A closed port delivers nothing: after <code>node.port.close()</code> the page hears nothing of the processor, and the processor goes on (the audio is complete).
     * <p>
     * 닫힌 포트는 아무것도 전달하지 않는다: <code>node.port.close()</code> 뒤에 페이지는 프로세서의 소식을 듣지 못하고, 프로세서는 계속 동작한다(오디오는 온전하다).
     */
    public void testAClosedPortDeliversNothing() {
        WorkletPage p = echoPage("new OfflineAudioContext(1, 1024, 8000)", "{numberOfInputs: 0}");
        p.run("node.port.postMessage({type: 'report', on: true}); node.port.close();");
        render(p);
        p.pumpFor(300);
        Check.equal("0", p.run("String(msgs.length)"), "nothing arrived");
        Check.equal("1", p.run("String(out.getChannelData(0)[1000])"), "the processor produced the audio all the same");
    }

    /**
     * <code>addEventListener</code> needs <code>start()</code> on a port, <code>onmessage</code> starts it (the rules of a <code>MessagePort</code>): a listener that is added alone hears nothing until
     * the port is started.
     * <p>
     * 포트에서 <code>addEventListener</code> 는 <code>start()</code> 가 필요하고 <code>onmessage</code> 는 포트를 시작한다(<code>MessagePort</code> 의 규칙): 혼자 추가한 리스너는 포트를 시작할 때까지 아무것도
     * 듣지 못한다.
     */
    public void testStartingThePort() {
        WorkletPage p = new WorkletPage();
        p.module("echo.js", ECHO);
        p.run("var ctx = new AudioContext(); var heard = []; var n2 = null;"
                + "ctx.audioWorklet.addModule('echo.js').then(function () { n2 = new AudioWorkletNode(ctx, 'echo', {numberOfInputs: 0}); n2.port.addEventListener('message', function (e) { heard.push(e.data.type); });"
                + "n2.port.postMessage({type: 'gain', value: 2}); });");
        p.waitFor("n2 !== null", 20000, "the node");
        p.pumpFor(300);
        Check.equal("0", p.run("String(heard.length)"), "a port that was not started delivers nothing");
        p.run("n2.port.start();");
        p.waitFor("heard.length >= 1", 20000, "the answer after start()");
        Check.equal("ack", p.run("heard[0]"), "the answer");
        p.run("ctx.close();");
    }
}
