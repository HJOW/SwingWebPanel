package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * <code>audioWorklet.addModule()</code>, <code>registerProcessor()</code> and the construction of <code>AudioWorkletNode</code> (Web Audio API 1.32 and the HTML Worklets model): the promise
 * resolves after the names that the module registered are known to the page; the failures reject as the HTML Standard and Chromium do (<code>AbortError</code> for a module that cannot be
 * fetched, the <code>SyntaxError</code> itself, an error object and no <code>DOMException</code>, for a module with a syntax error or an import that cannot be linked, a <code>SyntaxError</code>
 * <code>DOMException</code> for an address that cannot be parsed, <code>TypeError</code> for wrong arguments, <code>InvalidStateError</code> for a closed context) while an exception that a module throws
 * while it runs, an error of <code>registerProcessor()</code> included, does not reject: the promise resolves and the console of the page reports it;
 * a module is evaluated once however often it is added, also when two modules import it; relative imports resolve against the importing module, also in a directory below, and cycles work;
 * <code>registerProcessor()</code> throws the errors of the specification in its order and checks the parameter descriptors; the constructor of <code>AudioWorkletNode</code> refuses what the
 * specification lists and converts the options as WebIDL says. The modules report what they saw through <code>console.log</code>, which reaches the console of the page.
 * <p>
 * <code>audioWorklet.addModule()</code>, <code>registerProcessor()</code>, <code>AudioWorkletNode</code> 의 생성(Web Audio API 1.32 와 HTML Worklets 모델): 프라미스는 모듈이 등록한 이름을 페이지가 알게 된
 * 뒤에 resolve 된다. 실패는 HTML Standard 와 Chromium 처럼 거부된다(가져올 수 없는 모듈은 <code>AbortError</code>, 구문 오류가 있거나 링크할 수 없는 import 가 있는 모듈은 <code>DOMException</code> 이 아닌
 * 오류 객체인 <code>SyntaxError</code> 자체, 파싱할 수 없는 주소는 <code>SyntaxError</code> <code>DOMException</code>, 잘못된 인수는 <code>TypeError</code>, 닫힌 컨텍스트는
 * <code>InvalidStateError</code>). 모듈이 실행 중 던지는 예외(<code>registerProcessor()</code> 의 오류 포함)는 거부하지 않는다: 프라미스는 resolve 되고 페이지의 콘솔이 보고한다. 모듈은 몇 번을 추가해도, 두 모듈이 import 해도 한 번만 평가된다. 상대 import 는 가져오는 모듈을 기준으로 해석되며 아래
 * 디렉터리에서도 그렇고, 순환도 동작한다. <code>registerProcessor()</code> 는 규격의 오류를 규격의 순서로 던지고 파라미터 설명을 검사한다. <code>AudioWorkletNode</code> 의 생성자는 규격이 나열한 것을
 * 거부하고 옵션을 WebIDL 대로 변환한다. 모듈은 <code>console.log</code> 로 본 것을 알리며 이는 페이지의 콘솔에 닿는다.
 */
public class AudioWorkletModulesTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AudioWorkletModulesTest() {
    }

    /**
     * Checks the globalThis descriptor and module top-level this in an AudioWorklet.
     * <p>
     * AudioWorklet의 globalThis 기술자와 모듈 최상위 this를 확인한다.
     */
    public void testGlobalThis() {
        WorkletPage p = new WorkletPage();
        p.module("global.js", "const d=Object.getOwnPropertyDescriptor(globalThis,'globalThis');"
                + "console.log(d.value===globalThis && d.writable && !d.enumerable && d.configurable && this===undefined);");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000);");
        Check.equal("ok", add(p, "ctx", "'global.js'"), "the module loads");
        Check.equal("log: true", p.console().get(0), "worklet module global descriptor");
    }

    /**
     * Checks that an idle AudioWorklet rejects a synchronous wait and still runs an async timeout job.
     * <p>
     * 유휴 AudioWorklet에서 동기 대기를 거부하고 비동기 타임아웃 작업을 실행하는지 확인한다.
     */
    public void testAtomicsWaitPolicyAndTimeout() {
        WorkletPage p = new WorkletPage();
        try {
            p.module("atomics.js", "const a=new Int32Array(new SharedArrayBuffer(4));"
                    + "try{Atomics.wait(a,0,0,0);console.log('missing');}catch(e){console.log(e.name);}"
                    + "Atomics.waitAsync(a,0,0,20).value.then(v=>console.log(v));");
            p.run("var ctx=new OfflineAudioContext(1,128,8000);");
            Check.equal("ok", add(p, "ctx", "'atomics.js'"), "the worklet module loads");
            long deadline = System.currentTimeMillis() + 10000;
            // 새 worklet 스크립트나 렌더링 없이 큐의 시간 초과 작업이 콘솔에 도달할 때까지 기다린다.
            while (!p.console().contains("log: timed-out") && System.currentTimeMillis() < deadline) {
                p.pumpFor(5);
            }
            Check.isTrue(p.console().contains("log: TypeError"), "audio worklet agent cannot suspend");
            Check.isTrue(p.console().contains("log: timed-out"), "idle audio worklet processed the external timeout job");
        } finally {
            p.fixture.scripts.dispose();
        }
    }

    /**
     * Adds a module and waits for the promise to settle; the result is <code>ok</code> or the name of the error in the variable <code>result</code>.
     * <p>
     * 모듈을 추가하고 프라미스가 확정될 때까지 기다린다. 결과는 변수 <code>result</code> 에 <code>ok</code> 또는 오류 이름으로 둔다.
     *
     * @param p       the page / 페이지
     * @param context the expression of the context / 컨텍스트 식
     * @param args    the arguments of <code>addModule</code> as script / <code>addModule</code> 의 인수(스크립트)
     * @return <code>ok</code> or the name of the error / <code>ok</code> 또는 오류 이름
     */
    private static String add(WorkletPage p, String context, String args) {
        p.run("var result = null; " + context + ".audioWorklet.addModule(" + args + ").then(function () { result = 'ok'; }, function (e) { result = e.name; reason = e; });");
        p.waitFor("result !== null", 20000, "addModule(" + args + ")");
        return p.run("result");
    }

    /**
     * Tells whether the reason of the last rejection that {@link #add} saw is a <code>DOMException</code> (an <code>instanceof</code> check in the page).
     * <p>
     * {@link #add} 가 마지막으로 본 거부 이유가 <code>DOMException</code> 인지 알려 준다(페이지에서 <code>instanceof</code> 로 확인).
     *
     * @param p the page / 페이지
     * @return "true" or "false" / "true" 또는 "false"
     */
    private static String reasonIsDomException(WorkletPage p) {
        return p.run("String(reason instanceof DOMException)");
    }

    /**
     * A module that is added resolves the promise after the names that it registered are known: a node can be created in the first <code>then</code>.
     * <p>
     * 추가한 모듈은 등록한 이름이 알려진 뒤에 프라미스를 resolve 한다: 첫 <code>then</code> 에서 노드를 만들 수 있다.
     */
    public void testRegisteredNamesAreKnownWhenThePromiseResolves() {
        WorkletPage p = new WorkletPage();
        p.module("one.js", "class One extends AudioWorkletProcessor { process() { return false; } }\nregisterProcessor('one', One);\nregisterProcessor('uno', One);\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000); var made = null; Promise.resolve(ctx.audioWorklet.addModule('one.js')).then(function () {"
                + " made = [new AudioWorkletNode(ctx, 'one') instanceof AudioWorkletNode, new AudioWorkletNode(ctx, 'uno') instanceof AudioNode].join(); }, function (e) { made = e.name; });");
        p.waitFor("made !== null", 20000, "the first then");
        Check.equal("true,true", p.run("made"), "two names of one module");
        Check.equal("true", p.run("String(ctx.audioWorklet === ctx.audioWorklet)"), "the same audioWorklet every time");
        Check.equal("true", p.run("String(ctx.audioWorklet instanceof AudioWorklet && ctx.audioWorklet instanceof Worklet)"), "the interfaces");
        Check.equal("AudioWorklet", p.run("Object.prototype.toString.call(ctx.audioWorklet).slice(8, -1)"), "the class name");
        Check.equal("true", p.run("String(ctx.audioWorklet.addModule('one.js') instanceof Promise)"), "addModule returns a promise");
    }

    /**
     * A node cannot be created for a name that no module registered (the specification: <code>InvalidStateError</code>), before any module was added, and the arguments are checked.
     * <p>
     * 어떤 모듈도 등록하지 않은 이름으로는 노드를 만들 수 없다(규격: <code>InvalidStateError</code>). 모듈을 추가하기 전도 마찬가지이며 인수를 검사한다.
     */
    public void testConstructorErrors() {
        WorkletPage p = new WorkletPage();
        p.run("var ctx = new OfflineAudioContext(2, 128, 8000);");
        Check.equal("InvalidStateError", p.errorOf("new AudioWorkletNode(ctx, 'nothing')"), "no module was added");
        p.module("m.js", "registerProcessor('m', class extends AudioWorkletProcessor { process() { return true; } });\n");
        Check.equal("ok", add(p, "ctx", "'m.js'"), "adding the module");
        Check.equal("InvalidStateError", p.errorOf("new AudioWorkletNode(ctx, 'other')"), "a name that is not registered");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode(ctx)"), "the name is required");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode()"), "the context is required");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode({}, 'm')"), "the context must be a BaseAudioContext");
        Check.equal("TypeError", p.errorOf("AudioWorkletNode(ctx, 'm')"), "the constructor needs new");
        Check.equal("NotSupportedError", p.errorOf("new AudioWorkletNode(ctx, 'm', {numberOfInputs: 0, numberOfOutputs: 0})"), "no inputs and no outputs");
        Check.equal("IndexSizeError", p.errorOf("new AudioWorkletNode(ctx, 'm', {numberOfOutputs: 2, outputChannelCount: [1]})"), "outputChannelCount of another length");
        Check.equal("NotSupportedError", p.errorOf("new AudioWorkletNode(ctx, 'm', {outputChannelCount: [0]})"), "zero channels");
        Check.equal("NotSupportedError", p.errorOf("new AudioWorkletNode(ctx, 'm', {outputChannelCount: [33]})"), "more channels than supported");
        Check.equal("NotSupportedError", p.errorOf("new AudioWorkletNode(ctx, 'm', {numberOfInputs: 33})"), "more inputs than supported");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode(ctx, 'm', {outputChannelCount: 3})"), "outputChannelCount is a sequence");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode(ctx, 'm', {parameterData: {x: 'many'}})"), "a value of parameterData is a number");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode(ctx, 'm', {processorOptions: 5})"), "processorOptions is an object");
        Check.equal("DataCloneError", p.errorOf("new AudioWorkletNode(ctx, 'm', {processorOptions: {f: function () {}}})"), "processorOptions that cannot be cloned");
        Check.equal("TypeError", p.errorOf("new AudioWorkletNode(ctx, 'm', {channelCountMode: 'wide'})"), "a bad enum value");
        Check.equal("NotSupportedError", p.errorOf("new AudioWorkletNode(ctx, 'm', {channelCount: 0})"), "channelCount 0");
        // 속성: 기본값(1 입력, 1 출력, channelCount 2, max, speakers)과 옵션으로 정한 채널 속성.
        Check.equal("1,1,2,max,speakers", p.run("var n = new AudioWorkletNode(ctx, 'm'); [n.numberOfInputs, n.numberOfOutputs, n.channelCount, n.channelCountMode, n.channelInterpretation].join()"),
                "the properties of a new node");
        Check.equal("2,3,1,explicit,discrete", p.run("var n2 = new AudioWorkletNode(ctx, 'm', {numberOfInputs: 2, numberOfOutputs: 3, channelCount: 1, channelCountMode: 'explicit', "
                + "channelInterpretation: 'discrete'}); [n2.numberOfInputs, n2.numberOfOutputs, n2.channelCount, n2.channelCountMode, n2.channelInterpretation].join()"), "the options");
        Check.equal("true", p.run("String(n instanceof AudioWorkletNode && n instanceof AudioNode && n instanceof EventTarget)"), "the prototype chain");
        Check.equal("AudioWorkletNode", p.run("Object.prototype.toString.call(n).slice(8, -1)"), "the class name");
        Check.equal("true", p.run("String(n.context === ctx && n.parameters instanceof AudioParamMap && n.port instanceof MessagePort && n.port === n.port)"), "context, parameters and port");
        Check.equal("null", p.run("String(n.onprocessorerror)"), "onprocessorerror is null at first");
    }

    /**
     * <code>registerProcessor()</code>: the errors of 1.32.2.2 in the order of the steps, and the descriptors (a missing name, a name twice, a default outside the range, an automation rate that does not
     * exist, a value that is not a sequence, an item that is no dictionary, a value that is not finite) while a set of descriptors (an iterable that is not an array) is accepted.
     * <p>
     * <code>registerProcessor()</code>: 1.32.2.2 의 오류를 단계의 순서대로, 그리고 설명(없는 이름, 두 번 쓴 이름, 범위 밖의 기본값, 없는 자동화 속도, 시퀀스가 아닌 값, 사전이 아닌 항목, 유한하지 않은 값).
     * 이터러블(배열이 아닌)인 설명 모음은 받아들인다.
     */
    public void testRegisterProcessorErrors() {
        WorkletPage p = new WorkletPage();
        p.module("reg.js", "const log = [];\n" + "function t(f) { try { f(); log.push('ok'); } catch (e) { log.push(e.name); } }\n"
                + "class P extends AudioWorkletProcessor { process() { return false; } }\n" + "function desc(d) { return class extends AudioWorkletProcessor { static get parameterDescriptors() { return d; } process() { return false; } }; }\n"
                + "t(function () { registerProcessor('', P); });\n" + "t(function () { registerProcessor('a', P); });\n" + "t(function () { registerProcessor('a', P); });\n"
                + "t(function () { registerProcessor('c', () => {}); });\n" + "t(function () { registerProcessor('d', 5); });\n" + "t(function () { registerProcessor('e'); });\n"
                + "t(function () { registerProcessor('f', desc([{}])); });\n" + "t(function () { registerProcessor('g', desc([{name: 'x'}, {name: 'x'}])); });\n"
                + "t(function () { registerProcessor('h', desc([{name: 'x', defaultValue: 5, maxValue: 1}])); });\n"
                + "t(function () { registerProcessor('i', desc([{name: 'x', automationRate: 'fast'}])); });\n" + "t(function () { registerProcessor('j', desc(7)); });\n"
                + "t(function () { registerProcessor('k', desc(['text'])); });\n" + "t(function () { registerProcessor('l', desc([{name: 'x', defaultValue: Infinity}])); });\n"
                + "t(function () { registerProcessor('m', desc(new Set([{name: 'x'}, {name: 'y', defaultValue: 2, minValue: 1, maxValue: 3}]))); });\n"
                + "t(function () { registerProcessor('n', desc(undefined)); });\n" + "console.log(log.join(','));\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000);");
        Check.equal("ok", add(p, "ctx", "'reg.js'"), "the module itself loads (every error was caught inside)");
        Check.equal("log: NotSupportedError,ok,NotSupportedError,TypeError,TypeError,TypeError,TypeError,NotSupportedError,InvalidStateError,TypeError,TypeError,TypeError,TypeError,ok,ok",
                p.console().get(0), "the errors in the order of the calls: " + p.console());
        Check.equal("true", p.run("String(new AudioWorkletNode(ctx, 'm').parameters.size === 2)"), "a set of descriptors was accepted");
        Check.equal("true", p.run("var y = new AudioWorkletNode(ctx, 'm').parameters.get('y'); String(y.defaultValue === 2 && y.minValue === 1 && y.maxValue === 3)"), "the descriptor values");
        Check.equal("InvalidStateError", p.errorOf("new AudioWorkletNode(ctx, 'c')"), "a failed registration leaves no name");
        Check.equal("0", p.run("String(new AudioWorkletNode(ctx, 'n').parameters.size)"), "undefined descriptors mean no parameters");
    }

    /**
     * <code>AudioWorkletProcessor</code> cannot be constructed by the script: <code>new AudioWorkletProcessor()</code> in the global scope throws a <code>TypeError</code> (1.32.4.1), and
     * the global scope has what 1.32.2 lists (<code>registerProcessor</code>, <code>currentFrame</code>, <code>currentTime</code>, <code>sampleRate</code>) and no window, document or timer.
     * <p>
     * 스크립트가 <code>AudioWorkletProcessor</code> 를 만들 수 없다: 전역 범위의 <code>new AudioWorkletProcessor()</code> 는 <code>TypeError</code> 를 던진다(1.32.4.1). 전역 범위에는 1.32.2 가
     * 나열한 것(<code>registerProcessor</code>, <code>currentFrame</code>, <code>currentTime</code>, <code>sampleRate</code>)이 있고 window, document, 타이머는 없다.
     */
    public void testTheGlobalScope() {
        WorkletPage p = new WorkletPage();
        p.module("scope.js", "const r = [];\n" + "try { new AudioWorkletProcessor(); r.push('built'); } catch (e) { r.push(e.name); }\n"
                + "r.push(typeof registerProcessor, typeof currentFrame, typeof currentTime, typeof sampleRate, sampleRate);\n"
                + "r.push(typeof window, typeof document, typeof setTimeout, typeof fetch, typeof MessageChannel, typeof MessagePort, typeof Promise, typeof Float32Array, typeof AudioContext);\n"
                + "r.push(currentFrame, currentTime);\n" + "console.log(r.join(','));\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000);");
        Check.equal("ok", add(p, "ctx", "'scope.js'"), "the module loads");
        Check.equal("log: TypeError,function,number,number,number,8000,undefined,undefined,undefined,undefined,undefined,function,function,function,undefined,0,0", p.console().get(0),
                "the global scope: " + p.console());
    }

    /**
     * Failures of <code>addModule()</code> reject as the HTML Standard and Chromium do: <code>AbortError</code> (a <code>DOMException</code>) for a module that cannot be fetched (the address is
     * unknown, a module that it imports is unknown, a scheme that is not allowed); the <code>SyntaxError</code> itself, an error object that is no <code>DOMException</code>, for a module with a syntax
     * error and for an import of a name that the imported module does not export (the module cannot be linked); a <code>SyntaxError</code> <code>DOMException</code> for an address that does not parse;
     * <code>TypeError</code> for a missing argument and a bad option; the console of the page tells why. A context that was closed rejects with <code>InvalidStateError</code>. A syntax error is not
     * remembered: adding the module again tries again. A module that throws while it runs does not fail (see the next test).
     * <p>
     * <code>addModule()</code> 의 실패는 HTML Standard 와 Chromium 처럼 거부된다: 가져올 수 없는 모듈(알 수 없는 주소, 그것이 import 하는 알 수 없는 모듈, 허용되지 않는 스킴)은 <code>AbortError</code>
     * (<code>DOMException</code>), 구문 오류가 있는 모듈과 import 한 모듈이 export 하지 않는 이름의 import(모듈을 링크할 수 없다)는 <code>DOMException</code> 이 아닌 오류 객체인 <code>SyntaxError</code> 자체,
     * 파싱되지 않는 주소는 <code>SyntaxError</code> <code>DOMException</code>, 인수가 없거나 옵션이 잘못되면 <code>TypeError</code>. 페이지의 콘솔이 이유를 알려 준다. 닫힌 컨텍스트는
     * <code>InvalidStateError</code> 로 거부한다. 구문 오류는 기억하지 않는다: 같은 모듈을 다시 추가하면 다시 시도한다. 실행 중 던지는 모듈은 실패하지 않는다(다음 시험).
     */
    public void testFailuresOfAddModule() {
        WorkletPage p = new WorkletPage();
        p.module("syntax.js", "class {{{ nope");
        p.module("throws.js", "throw new RangeError('top level');\n");
        p.module("imports-missing.js", "import {x} from './missing.js';\nregisterProcessor('x', class extends AudioWorkletProcessor { process() { return false; } });\n");
        p.module("bad-export.js", "import {nothing} from './export1.js';\n");
        p.module("export1.js", "export const something = 1;\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000); var rt = new AudioContext();");
        Check.equal("AbortError", add(p, "ctx", "'unknown.js'"), "an unknown address");
        Check.equal("true", reasonIsDomException(p), "AbortError is a DOMException");
        Check.equal("SyntaxError", add(p, "ctx", "'syntax.js'"), "a syntax error rejects with the SyntaxError");
        Check.equal("false:true", reasonIsDomException(p) + ":" + p.run("String(reason instanceof SyntaxError)"), "an error object, no DOMException");
        Check.equal("SyntaxError", add(p, "ctx", "'syntax.js'"), "a syntax error is not remembered: it fails again");
        Check.equal("AbortError", add(p, "ctx", "'imports-missing.js'"), "an import that cannot be fetched");
        Check.equal("SyntaxError", add(p, "ctx", "'bad-export.js'"), "an import of a name that the module does not export: the module cannot be linked");
        Check.equal("false:true", reasonIsDomException(p) + ":" + p.run("String(reason instanceof SyntaxError)"), "an error object, no DOMException");
        Check.equal("AbortError", add(p, "ctx", "'javascript:alert(1)'"), "a scheme that is not allowed");
        Check.equal("AbortError", add(p, "ctx", "'data:text/javascript,1'"), "a data address");
        Check.equal("TypeError", add(p, "ctx", ""), "no argument");
        Check.equal("TypeError", add(p, "ctx", "'syntax.js', {credentials: 'sure'}"), "a bad option");
        Check.equal("AbortError", add(p, "ctx", "'unknown.js', {credentials: 'include'}"), "a valid option");
        Check.equal("SyntaxError", add(p, "ctx", "'http://['"), "an address that does not parse");
        Check.equal("SyntaxError", add(p, "ctx", "'http://host:99999999/m.js'"), "an address with a port that does not exist");
        p.run("var r2 = null; AudioWorklet.prototype.addModule.call({}, 'x.js').then(null, function (e) { r2 = e.name; });");
        p.waitFor("r2 !== null", 20000, "the rejection of an illegal receiver");
        Check.equal("TypeError", p.run("r2"), "an illegal receiver is a rejected promise");
        // 콘솔에는 왜 실패했는지가 있다: 구문 오류, 링크 오류, 가져올 수 없는 주소.
        String console = p.console().toString();
        Check.contains(console, "Uncaught SyntaxError", "the syntax error: " + console);
        Check.contains(console, "does not provide an export named 'nothing'", "the link error: " + console);
        Check.contains(console, "unknown.js", "the module that could not be fetched: " + console);
        p.run("rt.close();");
        p.pumpFor(100);
        Check.equal("InvalidStateError", add(p, "rt", "'syntax.js'"), "a closed context");
        Check.equal("ok", p.run("'ok'"), "the page goes on");
    }

    /**
     * An exception that a module throws while it runs does not reject the promise (HTML Standard "Worklets": "run a module script" ends the step, Chromium resolves): the promise resolves, the console
     * of the page reports the uncaught exception, the names that the module registered before the exception stay registered, a duplicate name and a constructor that is no constructor in
     * <code>registerProcessor()</code> are exceptions of the module too, and the module counts as evaluated (it does not run again when it is added again).
     * <p>
     * 모듈이 실행 중 던지는 예외는 프라미스를 reject 하지 않는다(HTML Standard "Worklets": "run a module script" 가 단계를 끝낸다. Chromium 은 resolve 한다): 프라미스는 resolve 되고, 페이지의 콘솔이 처리되지
     * 않은 예외를 보고하며, 예외 전에 모듈이 등록한 이름은 등록된 채로 남고, <code>registerProcessor()</code> 의 중복 이름과 생성자가 아닌 인수도 모듈의 예외이며, 모듈은 평가한 것으로 친다(다시 추가해도 다시
     * 실행하지 않는다).
     */
    public void testAnExceptionWhileTheModuleRunsResolves() {
        WorkletPage p = new WorkletPage();
        p.module("throws.js", "console.log('running');\nthrow new RangeError('top level');\n");
        p.module("partial.js", "const P = class extends AudioWorkletProcessor { process() { return false; } };\nregisterProcessor('first', P);\nnull.x;\nregisterProcessor('second', P);\n");
        p.module("dup.js", "const P = class extends AudioWorkletProcessor { process() { return false; } };\nregisterProcessor('d', P);\nregisterProcessor('d', P);\n");
        p.module("notctor.js", "registerProcessor('n', {});\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000);");
        Check.equal("ok", add(p, "ctx", "'throws.js'"), "a module that throws resolves");
        Check.equal("ok", add(p, "ctx", "'throws.js'"), "and counts as evaluated");
        int runs = 0;
        int uncaught = 0;
        // 콘솔에서 모듈이 실행된 횟수와 처리되지 않은 예외의 보고를 센다.
        for (String line : p.console()) {
            runs += line.equals("log: running") ? 1 : 0;
            uncaught += line.indexOf("Uncaught RangeError: top level") >= 0 ? 1 : 0;
        }
        Check.equal(1, runs, "the module ran once: " + p.console());
        Check.equal(1, uncaught, "the console reports the uncaught exception once: " + p.console());
        Check.equal("ok", add(p, "ctx", "'partial.js'"), "an exception in the middle of a module");
        Check.equal("true", p.run("String(new AudioWorkletNode(ctx, 'first') instanceof AudioWorkletNode)"), "the name registered before the exception stays");
        Check.equal("InvalidStateError", p.errorOf("new AudioWorkletNode(ctx, 'second')"), "the code after the exception did not run");
        Check.equal("ok", add(p, "ctx", "'dup.js'"), "a duplicate name in registerProcessor is an exception of the module");
        Check.equal("true", p.run("String(new AudioWorkletNode(ctx, 'd') instanceof AudioWorkletNode)"), "the first registration stands");
        Check.contains(p.console().toString(), "Uncaught NotSupportedError: A processor with name 'd'", "the console reports the duplicate: " + p.console());
        Check.equal("ok", add(p, "ctx", "'notctor.js'"), "registerProcessor with something that is no constructor");
        Check.equal("InvalidStateError", p.errorOf("new AudioWorkletNode(ctx, 'n')"), "nothing was registered");
        Check.contains(p.console().toString(), "Uncaught TypeError: Failed to execute 'registerProcessor'", "the console reports the TypeError: " + p.console());
    }

    /**
     * A module is evaluated once: two <code>addModule()</code> calls of the same address (also at the same time) run it once, a module that two entry modules import is evaluated once for the
     * two of them, the second call of a module that failed to load is tried again, and a module that was added is not fetched again (the module source counts the fetches).
     * <p>
     * 모듈은 한 번만 평가한다: 같은 주소의 <code>addModule()</code> 호출 둘(동시에도)은 한 번만 실행하고, 진입 모듈 둘이 import 하는 모듈은 둘을 합쳐 한 번만 평가하며, 불러오기에 실패한 모듈의 두 번째
     * 호출은 다시 시도하고, 추가한 모듈은 다시 가져오지 않는다(모듈 공급원이 가져온 횟수를 센다).
     */
    public void testAModuleIsEvaluatedOnce() {
        WorkletPage p = new WorkletPage();
        p.module("shared.js", "console.log('shared');\nexport const factor = 3;\n");
        p.module("a.js", "import {factor} from './shared.js';\nconsole.log('a ' + factor);\n");
        p.module("b.js", "import {factor} from './shared.js';\nconsole.log('b ' + factor);\n");
        p.run("var ctx = new OfflineAudioContext(1, 128, 8000);");
        // 같은 모듈을 연달아, 그리고 한꺼번에 두 번 추가해도 한 번만 평가한다.
        p.run("var done = 0; ctx.audioWorklet.addModule('a.js').then(function () { done++; }); ctx.audioWorklet.addModule('a.js').then(function () { done++; });");
        p.waitFor("done === 2", 20000, "two calls");
        Check.equal("ok", add(p, "ctx", "'a.js'"), "a third call");
        Check.equal("ok", add(p, "ctx", "'b.js'"), "another entry module that imports the shared module");
        String console = p.console().toString();
        Check.equal("[log: shared, log: a 3, log: b 3]", console, "each module is evaluated once, the shared module first: " + console);
    }

    /**
     * Relative imports resolve against the importing module: a module in a directory below imports <code>../up.js</code> and <code>./sub.js</code>, an absolute address works, a cycle between two modules
     * is fine, and what a module exports reaches the processor (the factor is used in the audio).
     * <p>
     * 상대 import 는 가져오는 모듈을 기준으로 해석된다: 아래 디렉터리의 모듈이 <code>../up.js</code> 와 <code>./sub.js</code> 를 import 하고, 절대 주소도 동작하며, 두 모듈 사이의 순환도 괜찮고, 모듈이
     * export 한 것이 프로세서에 닿는다(배율이 오디오에 쓰인다).
     */
    public void testImportsAndCycles() {
        WorkletPage p = new WorkletPage();
        p.module("up.js", "export const up = 2;\nexport function twice(x) { return x * up; }\n");
        p.module("dir/sub.js", "import {twice} from '../up.js';\nimport {back} from './cycle.js';\nexport const sub = twice(5);\nexport const viaCycle = back;\n");
        p.module("dir/cycle.js", "import {sub} from './sub.js';\nexport const back = 'cycle';\nexport function peek() { return typeof sub; }\n");
        p.module("dir/main.js", "import {sub, viaCycle} from './sub.js';\nimport {twice} from 'http://example.test/dir/up.js';\n"
                + "class Mul extends AudioWorkletProcessor { process(i, o) { for (let c = 0; c < i[0].length; c++) for (let k = 0; k < i[0][c].length; k++) o[0][c][k] = i[0][c][k] * twice(sub); return false; } }\n"
                + "console.log(sub + ' ' + viaCycle);\nregisterProcessor('mul', Mul);\n");
        p.run("var out = null; var ctx = new OfflineAudioContext(1, 256, 8000); ctx.audioWorklet.addModule('dir/main.js').then(function () { var s = new ConstantSourceNode(ctx, {offset: 0.125});"
                + "var n = new AudioWorkletNode(ctx, 'mul'); s.connect(n); n.connect(ctx.destination); s.start(); return ctx.startRendering(); }).then(function (b) { out = b; }, function (e) { out = e.name; });");
        p.waitFor("out !== null", 30000, "the rendering");
        Check.equal("[log: 10 cycle]", p.console().toString(), "the modules ran: " + p.console());
        // 0.125 * twice(10) = 0.125 * 20 = 2.5
        Check.equal("2.5", p.run("String(out.getChannelData(0)[100])"), "the factor that two modules computed reaches the audio");
    }
}
