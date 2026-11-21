package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * What happens when a processor fails (Web Audio API 1.32.3.2 and the rendering algorithm of 2.4): an exception of the constructor or of <code>process()</code>, a <code>process</code> that is not a
 * function, and a script that exceeds the limit of a call (an endless loop) fire <code>processorerror</code> at the node with an <code>ErrorEvent</code> (message, file, line, column) once, the node is
 * silent for good (the quantum in which the processor threw is silent although it had written to its outputs) and the processor is not called again; the rendering goes on, the other nodes are not
 * affected, and the console of the page tells what happened. Also the return value: a processor that returns a falsy value stops being processed when no input is active, one that returns a truthy value
 * goes on, and the microtasks of a call run before the next quantum.
 * <p>
 * 프로세서가 실패하면 어떻게 되는지(Web Audio API 1.32.3.2 와 2.4 의 렌더링 알고리즘): 생성자나 <code>process()</code> 의 예외, 함수가 아닌 <code>process</code>, 호출의 한도를 넘는 스크립트(끝없는 반복)는 노드에
 * <code>ErrorEvent</code>(메시지, 파일, 줄, 열)가 담긴 <code>processorerror</code> 를 한 번 발생시키고, 노드는 영구히 무음이며(프로세서가 던진 quantum 은 출력에 썼더라도 무음이다) 프로세서는 다시 불리지
 * 않는다. 렌더링은 계속되고 다른 노드는 영향을 받지 않으며 페이지의 콘솔이 일어난 일을 알려 준다. 반환 값도 확인한다: falsy 를 반환하는 프로세서는 활성 입력이 없으면 처리되지 않고, truthy 를 반환하면
 * 계속되며, 호출의 마이크로태스크는 다음 quantum 전에 실행된다.
 */
public class AudioWorkletFailureTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletFailureTest() {
    }

    /**
     * Loads a module, creates two nodes without inputs (the one that fails and a healthy one that writes 0.25) in a context with two channels, renders and waits.
     * <p>
     * 모듈을 불러오고, 입력이 없는 노드 둘(실패하는 것과 0.25 를 쓰는 멀쩡한 것)을 채널이 둘인 컨텍스트에 만들고, 렌더링하고 기다린다.
     *
     * @param p      the page / 페이지
     * @param module the text of the module that registers <code>bad</code> / <code>bad</code> 를 등록하는 모듈의 텍스트
     * @param after  statements after the nodes were created (<code>bad</code> is the failing node) / 노드를 만든 뒤의 문장(<code>bad</code> 는 실패하는 노드)
     */
    private static void renderWithAHealthyNeighbour(WorkletPage p, String module, String after) {
        p.module("fail.js", module + "registerProcessor('good', class extends AudioWorkletProcessor { process(i, o) { o[0][0].fill(0.25); return true; } });\n");
        p.run("var out = null; var err = null; var events = []; var ctx = new OfflineAudioContext(2, 1024, 8000);"
                + "ctx.audioWorklet.addModule('fail.js').then(function () { var good = new AudioWorkletNode(ctx, 'good', {numberOfInputs: 0, outputChannelCount: [1]}); var merger = ctx.createChannelMerger(2); good.connect(merger, 0, 0); merger.connect(ctx.destination);"
                + "window.bad = new AudioWorkletNode(ctx, 'bad', {numberOfInputs: 0, outputChannelCount: [1]}); bad.connect(merger, 0, 1);" + after
                + "return ctx.startRendering(); }).then(function (b) { out = b; }, function (e) { err = e.name + ': ' + e.message; });");
        p.waitFor("out !== null || err !== null", 30000, "the rendering");
        Check.equal("null", p.run("String(err)"), "the rendering finished " + p.console());
    }

    /**
     * An exception in <code>process()</code> in the third quantum: one <code>processorerror</code> with the message, the file and the line of the script; the two quanta before sound, the failing
     * quantum and everything after it is silent although the processor wrote 0.9 in it; the processor is called three times only; the neighbour is not affected; the console has the error.
     * <p>
     * 세 번째 quantum 의 <code>process()</code> 에서 예외: 스크립트의 메시지, 파일, 줄이 담긴 <code>processorerror</code> 하나. 앞의 두 quantum 은 소리가 나고, 프로세서가 0.9 를 썼더라도 실패한 quantum 과 그
     * 뒤는 모두 무음이며, 프로세서는 세 번만 불린다. 이웃은 영향을 받지 않고 콘솔에 오류가 있다.
     */
    public void testProcessThrows() {
        WorkletPage p = new WorkletPage();
        renderWithAHealthyNeighbour(p, "let calls = 0;\nclass Bad extends AudioWorkletProcessor {\n  process(inputs, outputs) {\n    calls++;\n    console.log('call ' + calls);\n"
                + "    outputs[0][0].fill(0.9);\n    if (calls === 3) throw new Error('boom');\n    return true;\n  }\n}\nregisterProcessor('bad', Bad);\n",
                "bad.onprocessorerror = function (e) { events.push(['on', e instanceof ErrorEvent, e.type, e.message, e.filename, e.lineno > 0, e.colno >= 0, e.bubbles, e.cancelable, e.target === bad].join()); };"
                        + "bad.addEventListener('processorerror', function (e) { events.push('listener'); });");
        Check.equal("2", p.run("String(events.length)"), "the handler and the listener were called once each");
        Check.equal("on,true,processorerror,Error: boom,http://example.test/dir/fail.js,true,true,false,false,true", p.run("events[0]"), "the event");
        Check.equal("listener", p.run("events[1]"), "the second listener");
        // 처음 두 quantum 은 0.9, 세 번째부터는 무음이어야 한다. 이웃은 내내 0.25 이다.
        for (int i = 0; i < 256; i++) {
            Check.near(0.9, Double.parseDouble(p.run("String(out.getChannelData(1)[" + i + "])")), 1e-6, "before the failure, frame " + i);
            i += 40;
        }
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(1), 256).every(function (v) { return v === 0; }))"), "the failing quantum and everything after it is silent");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0)).every(function (v) { return Math.abs(v - 0.25) < 1e-7; }))"), "the neighbour is not affected");
        Check.equal("3", String.valueOf(count(p, "log: call ")), "the processor is not called again after it threw");
        Check.equal("true", String.valueOf(contains(p, "error: Uncaught Error: boom")), "the console has the error: " + p.console());
    }

    /**
     * An exception in the constructor: <code>processorerror</code>, and the node is silent from the first quantum, while the rendering goes on.
     * <p>
     * 생성자의 예외: <code>processorerror</code> 가 발생하고, 노드는 첫 quantum 부터 무음이며 렌더링은 계속된다.
     */
    public void testConstructorThrows() {
        WorkletPage p = new WorkletPage();
        renderWithAHealthyNeighbour(p, "class Bad extends AudioWorkletProcessor {\n  constructor() { super(); throw new TypeError('no way'); }\n  process() { console.log('processed'); return true; }\n}\n"
                + "registerProcessor('bad', Bad);\n", "bad.onprocessorerror = function (e) { events.push(e.message); };");
        Check.equal("TypeError: no way", p.run("events.join()"), "the event of the constructor");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(1)).every(function (v) { return v === 0; }))"), "silence");
        Check.equal("0", String.valueOf(count(p, "log: processed")), "process() never ran");
    }

    /**
     * A processor class without <code>process</code> fails with a <code>TypeError</code> at the first quantum (the specification looks <code>process</code> up for every quantum and throws a
     * <code>TypeError</code> if it is not callable); a <code>process</code> that is removed later fails at the quantum in which it is gone.
     * <p>
     * <code>process</code> 가 없는 프로세서 클래스는 첫 quantum 에서 <code>TypeError</code> 로 실패한다(규격은 quantum 마다 <code>process</code> 를 찾고 호출할 수 없으면 <code>TypeError</code> 를 던진다).
     * 나중에 지운 <code>process</code> 는 그것이 없어진 quantum 에서 실패한다.
     */
    public void testProcessIsNotAFunction() {
        WorkletPage p = new WorkletPage();
        renderWithAHealthyNeighbour(p, "class Bad extends AudioWorkletProcessor {}\nregisterProcessor('bad', Bad);\n", "bad.onprocessorerror = function (e) { events.push(e.message); };");
        Check.equal("true", String.valueOf(p.run("events.join()").indexOf("process") >= 0), "a TypeError about process: " + p.run("events.join()"));
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(1)).every(function (v) { return v === 0; }))"), "silence");

        WorkletPage q = new WorkletPage();
        renderWithAHealthyNeighbour(q, "let n = 0;\nclass Bad extends AudioWorkletProcessor {\n  process(i, o) { o[0][0].fill(0.5); if (++n === 2) delete Bad.prototype.process; return true; }\n}\n"
                + "registerProcessor('bad', Bad);\n", "bad.onprocessorerror = function (e) { events.push(e.message); };");
        Check.equal("1", q.run("String(events.length)"), "one event");
        Check.near(0.5, Double.parseDouble(q.run("String(out.getChannelData(1)[255])")), 1e-6, "the second quantum still sounds");
        Check.equal("0", q.run("String(out.getChannelData(1)[256])"), "the third quantum is silent: process is gone");
    }

    /**
     * A processor that never returns from <code>process()</code> is stopped by the limit of a call (the step limit): <code>processorerror</code> says that the script was aborted, the rendering
     * finishes (it does not take more than a few seconds), and the next processors run. A closed loop on the page thread does not exist: the test thread answers meanwhile.
     * <p>
     * <code>process()</code> 에서 돌아오지 않는 프로세서는 호출의 한도(단계 수 한도)로 멈춘다: <code>processorerror</code> 가 스크립트가 중단되었다고 알리고, 렌더링은 끝나며(몇 초를 넘지 않는다), 다음 프로세서가
     * 실행된다. 그동안 페이지 스레드가 막히지 않는다: 시험 스레드가 계속 응답한다.
     */
    public void testAnEndlessLoopIsStopped() {
        WorkletPage p = new WorkletPage();
        long start = System.currentTimeMillis();
        renderWithAHealthyNeighbour(p, "class Bad extends AudioWorkletProcessor {\n  process() { let x = 0; for (;;) { x++; } }\n}\nregisterProcessor('bad', Bad);\n",
                "bad.onprocessorerror = function (e) { events.push(e.message); };");
        long took = System.currentTimeMillis() - start;
        Check.equal("1", p.run("String(events.length)"), "one event");
        Check.contains(p.run("events[0]"), "Script aborted", "the event says that the script was aborted: " + p.run("events[0]"));
        Check.isTrue(took < 30000, "the rendering ended after " + took + " ms");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(1)).every(function (v) { return v === 0; }))"), "the endless processor is silent");
        Check.equal("true", p.run("String(Array.prototype.slice.call(out.getChannelData(0)).every(function (v) { return Math.abs(v - 0.25) < 1e-7; }))"), "the neighbour is not affected");
    }

    /**
     * The return value is the active source flag (1.32.4.3): a source that returns <code>1</code> (truthy) goes on, one that returns nothing plays its first quantum only, as the specification says
     * for the value <code>undefined</code>; and the microtasks that a call queues run before the next quantum: <code>process</code> and a promise reaction alternate in the console.
     * <p>
     * 반환 값은 active source flag 이다(1.32.4.3): <code>1</code>(truthy)을 반환하는 원천은 계속되고, 아무것도 반환하지 않는 원천은 규격이 <code>undefined</code> 에 대해 말하듯이 첫 quantum 만 재생한다. 호출이
     * 쌓은 마이크로태스크는 다음 quantum 전에 실행된다: <code>process</code> 와 promise 반응이 콘솔에서 번갈아 나타난다.
     */
    public void testTheReturnValueAndTheMicrotasks() {
        WorkletPage p = new WorkletPage();
        p.module("flag.js", "class Truthy extends AudioWorkletProcessor { process(i, o) { o[0][0].fill(0.5); return 1; } }\n"
                + "class Nothing extends AudioWorkletProcessor { process(i, o) { o[0][0].fill(0.5); } }\n"
                + "let k = 0;\nclass Micro extends AudioWorkletProcessor { process(i, o) { const n = ++k; console.log('process ' + n); Promise.resolve().then(() => console.log('micro ' + n)); return k < 3; } }\n"
                + "registerProcessor('truthy', Truthy); registerProcessor('nothing', Nothing); registerProcessor('micro', Micro);\n");
        p.run("var out = null; var ctx = new OfflineAudioContext(3, 512, 8000); var merger = ctx.createChannelMerger(3); ctx.audioWorklet.addModule('flag.js').then(function () {"
                + "['truthy', 'nothing', 'micro'].forEach(function (name, i) { var n = new AudioWorkletNode(ctx, name, {numberOfInputs: 0, outputChannelCount: [1]}); n.connect(merger, 0, i); }); merger.connect(ctx.destination);"
                + "return ctx.startRendering(); }).then(function (b) { out = b; });");
        p.waitFor("out !== null", 30000, "the rendering");
        Check.equal("0.5", p.run("String(out.getChannelData(0)[500])"), "a truthy value keeps the source going");
        Check.equal("0.5", p.run("String(out.getChannelData(1)[100])"), "the first quantum of the source that returned nothing");
        Check.equal("0", p.run("String(out.getChannelData(1)[130])"), "silence after that");
        Check.equal("[log: process 1, log: micro 1, log: process 2, log: micro 2, log: process 3, log: micro 3]", p.console().toString(), "the microtasks of a call run before the next call");
    }

    /**
     * An exception of a message handler on the port of the processor is reported to the console and affects nothing else: the processor goes on and there is no <code>processorerror</code>.
     * <p>
     * 프로세서 포트의 메시지 핸들러가 던진 예외는 콘솔에 보고될 뿐 다른 것에는 영향이 없다: 프로세서는 계속되고 <code>processorerror</code> 는 없다.
     */
    public void testAFailingMessageHandlerDoesNotKillTheProcessor() {
        WorkletPage p = new WorkletPage();
        renderWithAHealthyNeighbour(p, "class Bad extends AudioWorkletProcessor {\n  constructor() { super(); this.port.onmessage = () => { throw new Error('handler'); }; }\n"
                + "  process(i, o) { o[0][0].fill(0.75); return true; }\n}\nregisterProcessor('bad', Bad);\n", "bad.onprocessorerror = function (e) { events.push(e.message); }; bad.port.postMessage('x');");
        Check.equal("0", p.run("String(events.length)"), "no processorerror");
        Check.equal("0.75", p.run("String(out.getChannelData(1)[1000])"), "the processor goes on");
        Check.equal("true", String.valueOf(contains(p, "Uncaught Error: handler")), "the console has the error of the handler: " + p.console());
    }

    /**
     * Counts the lines of the console that start with a prefix.
     * <p>
     * 콘솔의 줄 중 접두사로 시작하는 것을 센다.
     *
     * @param p      the page / 페이지
     * @param prefix the prefix / 접두사
     * @return the number of lines / 줄 수
     */
    private static int count(WorkletPage p, String prefix) {
        int n = 0;
        // 줄마다 접두사로 시작하는지 확인한다.
        for (String line : p.console()) {
            if (line.startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Tells whether a line of the console contains a text.
     * <p>
     * 콘솔의 어떤 줄에 문자열이 있는지 알려 준다.
     *
     * @param p    the page / 페이지
     * @param text the text / 문자열
     * @return true if a line has it / 줄에 있으면 true
     */
    private static boolean contains(WorkletPage p, String text) {
        // 줄마다 문자열이 있는지 확인한다.
        for (String line : p.console()) {
            if (line.indexOf(text) >= 0) {
                return true;
            }
        }
        return false;
    }
}
