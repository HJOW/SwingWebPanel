package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>decodeAudioData</code> through the script interface (plan item P25-05; Web Audio API 1.1.2): a file that a script builds as an <code>ArrayBuffer</code> (a PCM
 * WAV file with known samples) is decoded by the decoders of the project on a decoding thread and the resulting buffer is compared with the samples that went in; the
 * promise form and the callback form, the order of the callback and the promise, the <code>ArrayBuffer</code> that is detached, a detached buffer (<code>DataCloneError</code>), data that cannot be
 * decoded (<code>EncodingError</code> through the promise and the error callback), the resampling to the sample rate of the context, a closed context, wrong arguments, and several decodings that are
 * pending at the same time. The decoded buffer is also played by an offline context, which closes the loop from the file to the rendered samples.
 * <p>
 * 스크립트 인터페이스를 통한 <code>decodeAudioData</code> 시험(계획 P25-05. Web Audio API 1.1.2): 스크립트가 <code>ArrayBuffer</code> 로 만든 파일(표본을 아는 PCM WAV 파일)을 프로젝트의
 * 디코더가 디코딩 스레드에서 디코딩하고, 결과 버퍼를 넣은 표본과 비교한다. 프라미스 형식과 콜백 형식, 콜백과 프라미스의 순서, 분리되는 <code>ArrayBuffer</code>, 분리된 버퍼
 * (<code>DataCloneError</code>), 디코딩할 수 없는 데이터(프라미스와 오류 콜백으로 <code>EncodingError</code>), 컨텍스트 샘플링 주파수로의 리샘플링, 닫힌 컨텍스트, 잘못된 인수, 동시에 대기 중인 여러 디코딩.
 * 디코딩한 버퍼를 오프라인 컨텍스트로 재생해 파일에서 렌더링한 표본까지 순환을 닫는다.
 */
public class DecodeAudioDataTest {

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
    public DecodeAudioDataTest() {
    }

    /**
     * Loads a page with the helpers that build a WAV file.
     * <p>
     * WAV 파일을 만드는 도우미가 있는 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        page.run("var log = [];"
                + " function wav(rate, samples) { var n = samples.length, buf = new ArrayBuffer(44 + n * 2), v = new DataView(buf);"
                + " function s(o, t) { for (var i = 0; i < t.length; i++) { v.setUint8(o + i, t.charCodeAt(i)); } }"
                + " s(0, 'RIFF'); v.setUint32(4, 36 + n * 2, true); s(8, 'WAVE'); s(12, 'fmt '); v.setUint32(16, 16, true); v.setUint16(20, 1, true); v.setUint16(22, 1, true);"
                + " v.setUint32(24, rate, true); v.setUint32(28, rate * 2, true); v.setUint16(32, 2, true); v.setUint16(34, 16, true); s(36, 'data'); v.setUint32(40, n * 2, true);"
                + " for (var i = 0; i < n; i++) { v.setInt16(44 + i * 2, samples[i], true); } return buf; }"
                + " function ramp(n) { var a = []; for (var i = 0; i < n; i++) { a.push(i * 100 - 5000); } return a; }");
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
     * The promise form: the buffer has the shape and the samples of the file, the <code>ArrayBuffer</code> that was passed is detached at once, and the buffer is delivered later (not
     * while the call runs).
     * <p>
     * 프라미스 형식: 버퍼가 파일의 모양과 표본을 가지고, 전달한 <code>ArrayBuffer</code> 는 즉시 분리되며, 버퍼는 나중에(호출이 실행되는 동안이 아니라) 전달된다.
     */
    public void testPromiseForm() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var ab = wav(8000, ramp(100)); var before = ab.byteLength; var got = null;"
                + " var p = ctx.decodeAudioData(ab); p.then(function (b) { got = b; }); log.push('after:' + ab.byteLength + ':' + before + ':' + (p instanceof Promise));");
        Check.equal("after:0:244:true", page.run("log.join('|')"), "the ArrayBuffer is detached by the call; the promise exists");
        Check.equal("true", page.run("String(got === null)"), "nothing is delivered while the call runs");
        page.waitFor("got !== null", 10000, "the decoding ends");
        Check.equal("1:100:8000:0.0125", page.run("[got.numberOfChannels, got.length, got.sampleRate, got.duration].join(':')"), "the shape of the decoded buffer");
        double err = page.number("var d = got.getChannelData(0), m = 0, r = ramp(100); for (var i = 0; i < 100; i++) { m = Math.max(m, Math.abs(d[i] - r[i] / 32768)); } m");
        Check.isTrue(err < 1e-9, "the decoded samples are the samples of the file divided by 32768: " + err);
        Check.equal("true", page.run("String(got instanceof AudioBuffer)"), "an AudioBuffer");
    }

    /**
     * The callback form and the order: the success callback is called inside the task that settles the promise, before the reactions of the promise run (a promise reaction is a
     * microtask after the task); both get the same buffer; an exception of a callback is reported and does not disturb the promise.
     * <p>
     * 콜백 형식과 순서: 성공 콜백은 프라미스를 확정하는 작업 안에서 프라미스의 반응이 실행되기 전에 불린다(프라미스 반응은 작업 뒤의 마이크로태스크). 둘은 같은 버퍼를 받는다.
     * 콜백의 예외는 보고되고 프라미스를 방해하지 않는다.
     */
    public void testCallbackFormAndOrder() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var viaCallback = null, viaPromise = null;"
                + " var p = ctx.decodeAudioData(wav(8000, ramp(10)), function (b) { viaCallback = b; log.push('success'); throw new Error('callback failure'); }, function () { log.push('error'); });"
                + " p.then(function (b) { viaPromise = b; log.push('then'); });");
        page.waitFor("viaPromise !== null", 10000, "the decoding ends");
        Check.equal("success|then", page.run("log.join('|')"), "the callback first, then the reaction of the promise");
        Check.equal("true", page.run("String(viaCallback === viaPromise)"), "the same buffer");
        boolean reported = false;
        // 콜백이 던진 예외는 콘솔과 window.onerror 로 보고된다.
        for (int i = 0; i < page.fixture.console.size(); i++) {
            reported |= page.fixture.console.get(i).indexOf("callback failure") >= 0;
        }
        Check.isTrue(reported, "the exception of the callback is reported: " + page.fixture.console);
    }

    /**
     * Failures: a buffer that is detached already is a <code>DataCloneError</code>; data that is no audio is an <code>EncodingError</code> (through the promise and through the error
     * callback, the error callback getting the same exception as the promise); a missing or wrong argument rejects the promise; wrong callbacks reject the promise.
     * <p>
     * 실패: 이미 분리된 버퍼는 <code>DataCloneError</code>, 오디오가 아닌 데이터는 <code>EncodingError</code>(프라미스와 오류 콜백으로. 오류 콜백은 프라미스와 같은 예외를 받는다),
     * 빠졌거나 잘못된 인수는 프라미스를 reject 하고, 잘못된 콜백도 프라미스를 reject 한다.
     */
    public void testFailures() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var errs = {}; var objs = {}; var detached = new ArrayBuffer(8); structuredClone(detached, {transfer: [detached]});"
                + " ctx.decodeAudioData(detached, function () { log.push('no'); }, function (e) { errs.detachedCb = e.name; }).then(null, function (e) { errs.detached = e.name; });"
                + " var junk = new Uint8Array(200); for (var i = 0; i < 200; i++) { junk[i] = (i * 37) & 255; }"
                + " ctx.decodeAudioData(junk.buffer, function () { log.push('no'); }, function (e) { errs.junkCb = e.name; objs.cb = e; }).then(null, function (e) { errs.junk = e.name; objs.p = e; });"
                + " ctx.decodeAudioData(new ArrayBuffer(0)).then(null, function (e) { errs.empty = e.name; });"
                + " ctx.decodeAudioData().then(null, function (e) { errs.none = e.name; });"
                + " ctx.decodeAudioData({}).then(null, function (e) { errs.wrong = e.name; });"
                + " ctx.decodeAudioData(new Uint8Array(4)).then(null, function (e) { errs.view = e.name; });"
                + " ctx.decodeAudioData(wav(8000, ramp(4)), 5).then(null, function (e) { errs.badCallback = e.name; });"
                + " AudioContext.prototype.decodeAudioData.call({}, new ArrayBuffer(4)).then(null, function (e) { errs.receiver = e.name; });");
        page.waitFor("errs.detached && errs.detachedCb && errs.junk && errs.junkCb && errs.empty && errs.none && errs.wrong && errs.view && errs.badCallback && errs.receiver", 10000, "all rejections");
        Check.equal("DataCloneError:DataCloneError:EncodingError:EncodingError:EncodingError:TypeError:TypeError:TypeError:TypeError:TypeError",
                page.run("[errs.detached, errs.detachedCb, errs.junk, errs.junkCb, errs.empty, errs.none, errs.wrong, errs.view, errs.badCallback, errs.receiver].join(':')"), "the errors");
        Check.equal("true:true", page.run("String(objs.p === objs.cb) + ':' + String(objs.p instanceof DOMException)"), "the promise and the error callback get the same DOMException");
        Check.equal("", page.run("log.join('|')"), "no success callback");
    }

    /**
     * The promise rejects invalid shared and resizable decode inputs before touching their data.
     * <p>
     * 공유 버퍼와 가변 버퍼를 디코딩 입력으로 받으면 데이터를 다루기 전에 Promise를 거부한다.
     */
    public void testSharedMemoryDecodeBoundary() {
        page.run("var ctx=new OfflineAudioContext(1,128,8000), s=new SharedArrayBuffer(4), r=new ArrayBuffer(4,{maxByteLength:8}), errs=[];"
                + "ctx.decodeAudioData(s).catch(e=>errs.push(e.name));ctx.decodeAudioData(r).catch(e=>errs.push(e.name));");
        page.waitFor("errs.length===2", 10000, "both IDL buffer conversions reject");
        Check.equal("TypeError|TypeError|4|4|false", page.run("[errs[0],errs[1],s.byteLength,r.byteLength,r.detached].join('|')"),
                "neither failed conversion detached the caller's buffer");
    }

    /**
     * The decoded buffer is converted to the sample rate of the context: a file of 8000 Hz decoded in a context of 16000 Hz has twice the frames, and the content is the same
     * signal (a slow ramp stays a ramp, to the precision of the resampler).
     * <p>
     * 디코딩한 버퍼는 컨텍스트의 샘플링 주파수로 바뀐다: 8000 Hz 파일을 16000 Hz 컨텍스트에서 디코딩하면 프레임이 두 배이고 내용은 같은 신호이다(느린 램프는 리샘플러의 정밀도로 램프로 남는다).
     */
    public void testResamplingToTheContext() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 16000); var got = null; var data = []; for (var i = 0; i < 400; i++) { data.push(Math.round(8000 * Math.sin(2 * Math.PI * 200 * i / 8000))); }"
                + " ctx.decodeAudioData(wav(8000, data)).then(function (b) { got = b; });");
        page.waitFor("got !== null", 10000, "the decoding ends");
        Check.equal("1:800:16000", page.run("[got.numberOfChannels, got.length, got.sampleRate].join(':')"), "twice the frames at twice the rate");
        // 200 Hz 사인파는 16000 Hz 에서도 200 Hz 사인파이다. 가장자리(리샘플러의 과도 구간)를 뺀 가운데에서 비교한다.
        double err = page.number("var d = got.getChannelData(0), m = 0; for (var i = 100; i < 700; i++) { m = Math.max(m, Math.abs(d[i] - 8000 * Math.sin(2 * Math.PI * 200 * i / 16000) / 32768)); } m");
        Check.isTrue(err < 5e-4, "the resampled signal is the same sine, error " + err);
    }

    /**
     * The whole path from a file to samples: a decoded buffer is assigned to a source of an offline context and the rendered samples equal the samples of the file (a source that
     * plays at the rate of the context copies the buffer).
     * <p>
     * 파일에서 표본까지의 전체 경로: 디코딩한 버퍼를 오프라인 컨텍스트의 원천에 대입하면 렌더링한 표본이 파일의 표본과 같다(컨텍스트의 속도로 재생하는 원천은 버퍼를 복사한다).
     */
    public void testDecodedBufferIsPlayed() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var rendered = null;"
                + " ctx.decodeAudioData(wav(8000, ramp(100))).then(function (b) { var ctx2 = new OfflineAudioContext(1, 128, 8000); var s = ctx2.createBufferSource(); s.buffer = b;"
                + " s.connect(ctx2.destination); s.start(); return ctx2.startRendering(); }).then(function (r) { rendered = r; });");
        page.waitFor("rendered !== null", 10000, "the decoding and the rendering end");
        double err = page.number("var o = rendered.getChannelData(0), m = 0, r = ramp(100); for (var i = 0; i < 100; i++) { m = Math.max(m, Math.abs(o[i] - r[i] / 32768)); } m + Math.abs(o[100])");
        Check.isTrue(err < 1e-6, "the rendered samples are the samples of the file: " + err);
    }

    /**
     * Several decodings at the same time all end with their own results; a decoding on a closed context is rejected with <code>InvalidStateError</code> and does not detach the data.
     * <p>
     * 동시에 여러 디코딩이 각자의 결과로 모두 끝난다. 닫힌 컨텍스트의 디코딩은 <code>InvalidStateError</code> 로 reject 되며 데이터를 분리하지 않는다.
     */
    public void testManyAndClosed() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 8000); var lens = []; var count = 0;"
                + " for (var k = 1; k <= 12; k++) { (function (n) { ctx.decodeAudioData(wav(8000, ramp(n * 10))).then(function (b) { lens[n] = b.length; count++; }); })(k); }");
        page.waitFor("count === 12", 20000, "twelve decodings end");
        Check.equal("10,20,30,40,50,60,70,80,90,100,110,120", page.run("lens.slice(1).join(',')"), "each decoding has its own result");
        page.run("var rt = new AudioContext(); var ab = wav(8000, ramp(10)); var closedErr = null; rt.close().then(function () { return rt.decodeAudioData(ab); }).then(null, function (e) { closedErr = e.name + ':' + ab.byteLength; });");
        page.waitFor("closedErr !== null", 10000, "the decoding on the closed context is rejected");
        Check.equal("InvalidStateError:64", page.run("closedErr"), "a closed context rejects and keeps the data");
    }
}
