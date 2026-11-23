package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of offline rendering through the script interface on a real page (plan item P25-05, JavaScript binding): graphs that scripts build with the constructors, the
 * factories and the parameters are rendered by <code>OfflineAudioContext.startRendering()</code> on the thread of the engine, and the resulting buffer is read back by the script
 * and compared with <b>closed-form expectations</b> (a sine of a known frequency and amplitude, a linear ramp, an impulse delayed by a known number of frames, the
 * response of a filter that the specification gives in closed form, a panning gain, the sum of a convolution that is small enough to write down), so that the binding is
 * shown to do what the engine does when it is driven from a script and not only to have the right names. The expectations are computed in the test and never copied from the engine.
 * <p>
 * 실제 페이지에서 스크립트 인터페이스를 통한 오프라인 렌더링 시험(계획 P25-05, JavaScript 연결): 스크립트가 생성자, 팩터리, 파라미터로 만든 그래프를
 * <code>OfflineAudioContext.startRendering()</code> 이 엔진의 스레드에서 렌더링하고, 결과 버퍼를 스크립트가 읽어 <b>닫힌 식의 기대값</b>(주파수와 진폭을 아는 사인파, 선형 램프,
 * 정해진 프레임만큼 지연된 임펄스, 규격이 닫힌 식으로 주는 필터 응답, 패닝 이득, 적을 수 있을 만큼 작은 합성곱의 합)과 비교한다. 그래서 연결이 이름만 맞는 것이 아니라 엔진을 스크립트가
 * 구동할 때 엔진이 하는 일을 한다는 것이 보인다. 기대값은 시험에서 계산하며 엔진에서 복사하지 않는다.
 */
public class OfflineRenderingTest {

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
    public OfflineRenderingTest() {
    }

    /**
     * Loads a page with the helper functions of the script side.
     * <p>
     * 스크립트 쪽 도우미 함수가 있는 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
        page.run("var result = {done: false};"
                + " function go(ctx) { ctx.startRendering().then(function (b) { result.buf = b; result.done = true; }, function (e) { result.err = e.name; result.done = true; }); }"
                + " function maxErr(arr, f, from, to) { var m = 0; for (var i = from; i < to; i++) { m = Math.max(m, Math.abs(arr[i] - f(i))); } return m; }");
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
     * Starts the rendering of the context <code>ctx</code> of the page and waits for the buffer (kept in <code>result.buf</code>).
     * <p>
     * 페이지의 컨텍스트 <code>ctx</code> 의 렌더링을 시작하고 버퍼(<code>result.buf</code>)를 기다린다.
     */
    private void render() {
        page.run("go(ctx); 1");
        page.waitFor("result.done", 20000, "the rendering ends");
        Check.equal("undefined", page.run("String(result.err)"), "no error");
    }

    /**
     * An oscillator through a gain node gives a sine with the amplitude and the frequency that the script set, to the precision of the wave table of the engine.
     * <p>
     * 게인 노드를 거친 발진기는 스크립트가 정한 진폭과 주파수의 사인파를 엔진의 파형 표의 정밀도로 준다.
     */
    public void testOscillatorThroughGain() {
        page.run("var ctx = new OfflineAudioContext(1, 8820, 44100); var osc = ctx.createOscillator(); osc.frequency.value = 441; var g = new GainNode(ctx, {gain: 0.5});"
                + " osc.connect(g); g.connect(ctx.destination); osc.start();");
        render();
        Check.equal("8820:1:44100", page.run("[result.buf.length, result.buf.numberOfChannels, result.buf.sampleRate].join(':')"), "the shape of the result");
        double err = page.number("maxErr(result.buf.getChannelData(0), function (n) { return 0.5 * Math.sin(2 * Math.PI * 441 * n / 44100); }, 0, 8820)");
        Check.isTrue(err < 1e-5, "a sine with amplitude 0.5, error " + err);
        Check.near(0.0, page.number("result.buf.getChannelData(0)[0]"), 1e-6, "the sine starts at 0");
    }

    /**
     * A parameter that a script automates: a linear ramp, a target approach and a value curve give the values of the specification's formulas.
     * <p>
     * 스크립트가 자동화한 파라미터: 선형 램프, 목표 접근, 값 곡선이 규격의 식대로의 값을 준다.
     */
    public void testAutomation() {
        page.run("var ctx = new OfflineAudioContext(1, 4410, 44100); var src = ctx.createConstantSource(); var g = ctx.createGain(); g.gain.value = 0;"
                + " g.gain.setValueAtTime(0, 0); g.gain.linearRampToValueAtTime(1, 0.05); g.gain.setTargetAtTime(0.25, 0.05, 0.01); src.connect(g); g.connect(ctx.destination); src.start();");
        render();
        // 램프: n / 2205 (0 <= n <= 2205), 그 뒤: 0.25 + 0.75 * exp(-(n - 2205) / 44100 / 0.01)
        double ramp = page.number("maxErr(result.buf.getChannelData(0), function (n) { return n / 2205; }, 0, 2205)");
        Check.isTrue(ramp < 1e-6, "the linear ramp, error " + ramp);
        double target = page.number("maxErr(result.buf.getChannelData(0), function (n) { return 0.25 + 0.75 * Math.exp(-(n - 2205) / 44100 / 0.01); }, 2206, 4410)");
        Check.isTrue(target < 2e-6, "the target approach, error " + target);
        page.run("var ctx = new OfflineAudioContext(1, 4800, 8000); var s = ctx.createConstantSource(); s.offset.setValueCurveAtTime(new Float32Array([0, 1, 0]), 0.1, 0.4);"
                + " s.connect(ctx.destination); s.start(); result.done = false; delete result.buf;");
        render();
        // 값 곡선 [0, 1, 0]: 0.1 s 에서 0.5 s 까지(8000 Hz). 한가운데(0.3 s, 프레임 2400)는 1 이다.
        Check.near(1.0, page.number("result.buf.getChannelData(0)[2400]"), 1e-6, "the middle of the value curve");
        Check.near(0.5, page.number("result.buf.getChannelData(0)[1600]"), 1e-3, "a quarter of the value curve");
    }

    /**
     * A delay node delays an impulse by exactly the number of frames of the delay time; a buffer source plays a buffer that a script filled, with its playback rate.
     * <p>
     * 지연 노드는 임펄스를 지연 시간의 프레임 수만큼 정확히 지연시킨다. 버퍼 원천은 스크립트가 채운 버퍼를 재생 속도에 맞춰 재생한다.
     */
    public void testDelayAndBufferSource() {
        page.run("var ctx = new OfflineAudioContext(1, 4000, 8000); var b = ctx.createBuffer(1, 10, 8000); b.getChannelData(0)[0] = 1;"
                + " var src = ctx.createBufferSource(); src.buffer = b; var d = ctx.createDelay(); d.delayTime.value = 0.25; src.connect(d); d.connect(ctx.destination); src.start();");
        render();
        Check.equal("1:0:0", page.run("var o = result.buf.getChannelData(0); [o[2000], o[1999], o[2001]].join(':')"), "the impulse is delayed by 2000 frames");
        page.run("var ctx = new OfflineAudioContext(1, 64, 8000); var b = ctx.createBuffer(1, 16, 8000); var a = b.getChannelData(0); for (var i = 0; i < 16; i++) { a[i] = i; }"
                + " var src = new AudioBufferSourceNode(ctx, {buffer: b, playbackRate: 2}); src.connect(ctx.destination); src.start(); result.done = false; delete result.buf;");
        render();
        // 재생 속도 2: 출력 n 은 버퍼의 2n 번째 표본이다. 끝(16 프레임 = 8 출력 프레임) 뒤는 무음이다.
        double err = page.number("maxErr(result.buf.getChannelData(0), function (n) { return n < 8 ? 2 * n : 0; }, 0, 64)");
        Check.isTrue(err < 1e-6, "a buffer played at twice the speed, error " + err);
    }

    /**
     * Channel routing: a splitter and a merger place a mono signal in the channel that the script chose, a stereo panner gives the gains of the equal power law, and a
     * panner node at the right of the listener sends the sound to the right channel.
     * <p>
     * 채널 라우팅: 스플리터와 머저가 모노 신호를 스크립트가 고른 채널에 놓고, 스테레오 패너가 등전력 법칙의 이득을 주며, 청취자의 오른쪽에 있는 패너 노드는 소리를 오른쪽 채널로 보낸다.
     */
    public void testRoutingAndPanning() {
        page.run("var ctx = new OfflineAudioContext(2, 256, 8000); var c = ctx.createConstantSource(); var m = ctx.createChannelMerger(2); c.connect(m, 0, 1); m.connect(ctx.destination); c.start();");
        render();
        Check.equal("0:1", page.run("[result.buf.getChannelData(0)[10], result.buf.getChannelData(1)[10]].join(':')"), "the signal is in the right channel only");
        // 스테레오 패너: pan = 0.5 이면 x = (0.5 + 1) / 2 = 0.75 이므로 gainL = cos(0.75 * pi / 2), gainR = sin(0.75 * pi / 2).
        page.run("var ctx = new OfflineAudioContext(2, 256, 8000); var c = ctx.createConstantSource(); var p = new StereoPannerNode(ctx, {pan: 0.5}); c.connect(p); p.connect(ctx.destination);"
                + " c.start(); result.done = false; delete result.buf;");
        render();
        Check.near(Math.cos(0.75 * Math.PI / 2), page.number("result.buf.getChannelData(0)[20]"), 1e-6, "the left gain of the equal power law");
        Check.near(Math.sin(0.75 * Math.PI / 2), page.number("result.buf.getChannelData(1)[20]"), 1e-6, "the right gain of the equal power law");
        page.run("var ctx = new OfflineAudioContext(2, 256, 8000); var c = ctx.createConstantSource(); var pn = new PannerNode(ctx, {positionX: 5, refDistance: 5, panningModel: 'equalpower'});"
                + " c.connect(pn); pn.connect(ctx.destination); c.start(); result.done = false; delete result.buf;");
        render();
        Check.isTrue(page.number("Math.abs(result.buf.getChannelData(0)[20])") < 1e-6, "a source at the right is silent in the left channel");
        Check.isTrue(page.number("result.buf.getChannelData(1)[20]") > 0.5, "and sounds in the right channel");
    }

    /**
     * A biquad filter that a script created has the frequency response that the specification gives in closed form (a low-pass filter at the cutoff frequency has the gain
     * <code>10^(Q/20)</code>, a band-pass filter has gain 1), and a sine that is rendered through the filter comes out with that amplitude.
     * <p>
     * 스크립트가 만든 쌍이차 필터는 규격이 닫힌 식으로 주는 주파수 응답을 가진다(차단 주파수에서 저역 통과 필터의 이득은 <code>10^(Q/20)</code>, 대역 통과 필터의 이득은 1). 필터를
     * 거쳐 렌더링한 사인파는 그 진폭으로 나온다.
     */
    public void testBiquadFrequencyResponse() {
        page.run("var ctx = new OfflineAudioContext(1, 22050, 44100); var f = ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 1000; f.Q.value = 6;"
                + " var fr = new Float32Array([1000, 100, 20000]), mag = new Float32Array(3), ph = new Float32Array(3); f.getFrequencyResponse(fr, mag, ph);");
        Check.near(Math.pow(10, 6 / 20.0), page.number("mag[0]"), 1e-4, "the gain of a low-pass filter at the cutoff frequency, 10^(Q/20)");
        Check.near(1.0, page.number("mag[1]"), 1e-2, "and in the pass band");
        Check.isTrue(page.number("mag[2]") < 0.01, "and in the stop band");
        page.run("f.type = 'bandpass'; f.Q.value = 5; f.getFrequencyResponse(fr, mag, ph);");
        Check.near(1.0, page.number("mag[0]"), 1e-4, "a band-pass filter has gain 1 at its center");
        page.run("f.type = 'lowpass'; f.Q.value = 6; var osc = ctx.createOscillator(); osc.frequency.value = 1000; osc.connect(f); f.connect(ctx.destination); osc.start();");
        render();
        // 정상 상태에서 사인파 진폭은 필터 응답의 크기이다(앞의 과도 구간 5000 프레임은 건너뛴다).
        double peak = page.number("var o = result.buf.getChannelData(0), p = 0; for (var i = 12000; i < 22050; i++) { p = Math.max(p, Math.abs(o[i])); } p");
        Check.near(Math.pow(10, 6 / 20.0), peak, 0.02, "a sine at the cutoff frequency has the amplitude of the response");
    }

    /**
     * The convolver convolves with an impulse response that a script wrote (and acquired when it was set: a change afterwards has no effect); the wave shaper applies the
     * curve that it copied; the sum of the engine matches the sum written down by hand.
     * <p>
     * 컨볼버는 스크립트가 쓴 임펄스 응답으로 합성곱한다(정할 때 가져가므로 이후의 변경은 효과가 없다). 웨이브셰이퍼는 복사한 곡선을 적용한다. 엔진의 합은 손으로 쓴 합과 같다.
     */
    public void testConvolverAndWaveShaper() {
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var ir = ctx.createBuffer(1, 3, 8000); var a = ir.getChannelData(0); a[0] = 0.5; a[1] = 0.25; a[2] = -0.125;"
                + " var imp = ctx.createBuffer(1, 4, 8000); imp.getChannelData(0)[0] = 1; var s = ctx.createBufferSource(); s.buffer = imp;"
                + " var cv = new ConvolverNode(ctx, {disableNormalization: true, buffer: ir}); a[0] = 99; s.connect(cv); cv.connect(ctx.destination); s.start();");
        render();
        Check.near(0.5, page.number("result.buf.getChannelData(0)[0]"), 1e-6, "the response of the convolver, acquired when it was set: frame 0");
        Check.near(0.25, page.number("result.buf.getChannelData(0)[1]"), 1e-6, "frame 1");
        Check.near(-0.125, page.number("result.buf.getChannelData(0)[2]"), 1e-6, "frame 2");
        Check.near(0.0, page.number("result.buf.getChannelData(0)[3]"), 1e-6, "and silence after the response");
        Check.equal("true", page.run("String(cv.buffer === ir && cv.normalize === false)"), "the buffer attribute returns the object that was set");
        // 웨이브셰이퍼 곡선 [-0.5, 0.5] 는 입력 1 을 0.5 로, 입력 -1 을 -0.5 로 바꾼다. 곡선을 바꿔도 노드는 복사본을 쓴다.
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var c = ctx.createConstantSource(); var ws = ctx.createWaveShaper(); var curve = new Float32Array([-0.5, 0.5]);"
                + " ws.curve = curve; curve[1] = 9; c.connect(ws); ws.connect(ctx.destination); c.start(); result.done = false; delete result.buf;");
        render();
        Check.near(0.5, page.number("result.buf.getChannelData(0)[10]"), 1e-6, "the curve was copied when it was set");
        Check.equal("true:2", page.run("String(ws.curve === curve) + ':' + ws.curve.length"), "the curve attribute returns the array that was set");
    }

    /**
     * The analyser reads the signal while a rendering is suspended (the only moment when a script can look at an offline graph), and its data methods write into typed arrays.
     * <p>
     * 분석기는 렌더링이 일시 중단된 동안(스크립트가 오프라인 그래프를 볼 수 있는 유일한 순간) 신호를 읽으며, 데이터 메서드는 형식화 배열에 쓴다.
     */
    public void testAnalyserDuringSuspension() {
        page.run("var ctx = new OfflineAudioContext(1, 4096, 8000); var c = ctx.createConstantSource(); c.offset.value = 0.5; var an = ctx.createAnalyser(); an.fftSize = 256;"
                + " c.connect(an); an.connect(ctx.destination); c.start(); var seen = null;"
                + " ctx.suspend(0.1).then(function () { var f = new Float32Array(256), u = new Uint8Array(256), fr = new Float32Array(128), b = new Uint8Array(128);"
                + " an.getFloatTimeDomainData(f); an.getByteTimeDomainData(u); an.getFloatFrequencyData(fr); an.getByteFrequencyData(b);"
                + " seen = {time: f[255], byteTime: u[255], bins: an.frequencyBinCount, dc: fr[0], state: ctx.state}; return ctx.resume(); });");
        render();
        Check.equal("true", page.run("String(seen !== null)"), "the suspension callback ran");
        Check.near(0.5, page.number("seen.time"), 1e-6, "the time domain data holds the signal");
        Check.equal("192", page.run("String(seen.byteTime)"), "the byte time domain data of 0.5 is 128 * (1 + 0.5)");
        Check.equal("128", page.run("String(seen.bins)"), "frequencyBinCount");
        Check.isTrue(page.number("seen.dc") > -30, "the DC bin of a constant signal is strong: " + page.run("String(seen.dc)"));
        Check.equal("suspended", page.run("seen.state"), "the state is suspended while the rendering stands still");
    }

    /**
     * A node connected to an <code>AudioParam</code> modulates it (an LFO): the parameter value is the sum of its own value and the signal; <code>disconnect</code> in its forms removes
     * exactly the connections that the form names (all, one output, one destination, one input), and a parameter can be disconnected from a node.
     * <p>
     * <code>AudioParam</code> 에 연결한 노드는 파라미터를 변조한다(LFO): 파라미터 값은 자기 값과 신호의 합이다. <code>disconnect</code> 의 형식은 그 형식이 가리키는 연결만 정확히 제거하며
     * (모두, 한 출력, 한 대상, 한 입력), 파라미터를 노드에서 끊을 수 있다.
     */
    public void testConnectToParamAndDisconnect() {
        page.run("var ctx = new OfflineAudioContext(1, 4410, 44100); var c = ctx.createConstantSource(); var g = ctx.createGain(); g.gain.value = 0.5;"
                + " var lfo = ctx.createOscillator(); lfo.frequency.value = 100; var depth = ctx.createGain(); depth.gain.value = 0.25; lfo.connect(depth); var ret = depth.connect(g.gain);"
                + " c.connect(g); g.connect(ctx.destination); c.start(); lfo.start();");
        Check.equal("undefined", page.run("String(ret)"), "connect to a parameter returns undefined");
        render();
        double err = page.number("maxErr(result.buf.getChannelData(0), function (n) { return 0.5 + 0.25 * Math.sin(2 * Math.PI * 100 * n / 44100); }, 0, 4410)");
        Check.isTrue(err < 1e-5, "the parameter follows its value plus the modulation, error " + err);
        // 같은 그래프에서 모듈레이션 연결을 끊으면 이득은 상수 0.5 이다.
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var c = ctx.createConstantSource(); var g = ctx.createGain(); g.gain.value = 0.5; var lfo = ctx.createConstantSource(); lfo.offset.value = 0.25;"
                + " lfo.connect(g.gain); lfo.disconnect(g.gain); c.connect(g); g.connect(ctx.destination); c.start(); lfo.start(); result.done = false; delete result.buf;");
        render();
        Check.near(0.5, page.number("result.buf.getChannelData(0)[100]"), 1e-6, "disconnect(param) removes the modulation");
        // 분리 형식: 세 개의 출처를 하나의 합산 노드에 연결하고 형식마다 하나씩 끊는다.
        page.run("var ctx = new OfflineAudioContext(1, 256, 8000); var s = ctx.createGain(); var m = ctx.createChannelMerger(2); var a = ctx.createConstantSource(), b = ctx.createConstantSource(); a.offset.value = 1; b.offset.value = 2;"
                + " a.connect(m, 0, 0); b.connect(m, 0, 1); var splitter = ctx.createChannelSplitter(2); m.connect(splitter); splitter.connect(s, 0); splitter.connect(s, 1); s.connect(ctx.destination);"
                + " var sp = splitter; sp.disconnect(s, 1); a.start(); b.start(); result.done = false; delete result.buf;");
        render();
        // 스플리터의 출력 0 만 남는다(값 1, 채널 하나). 출력 1(값 2)의 연결은 disconnect(s, 1) 이 끊었다.
        Check.near(1.0, page.number("result.buf.getChannelData(0)[100]"), 1e-6, "disconnect(destination, output) removes that output only");
        Check.equal("InvalidAccessError", page.errorOf("sp.disconnect(s, 1)"), "disconnecting what is not connected");
        Check.equal("IndexSizeError", page.errorOf("sp.disconnect(7)"), "an output out of range");
        Check.equal("no error", page.errorOf("sp.disconnect(); sp.disconnect()"), "disconnect() of everything is allowed when nothing is left");
        Check.equal("TypeError", page.errorOf("sp.disconnect({})"), "a plain object");
    }

    /**
     * The listener and the spatial nodes: the parameters of the listener and of a panner can be read and written, the operations <code>setPosition</code> and <code>setOrientation</code> change
     * them, enumerations are strings, and the distance model, the cone and the panning model have their defaults.
     * <p>
     * 청취자와 공간 노드: 청취자와 패너의 파라미터를 읽고 쓸 수 있고, <code>setPosition</code>, <code>setOrientation</code> 연산이 그것을 바꾸며, 열거형은 문자열이고, 거리 모델, 원뿔, 패닝 모델에
     * 기본값이 있다.
     */
    public void testListenerAndPanner() {
        page.run("var ctx = new OfflineAudioContext(2, 128, 44100); var L = ctx.listener; var pn = ctx.createPanner();");
        Check.equal("0:0:0:0:0:-1:0:1:0", page.run("[L.positionX.value, L.positionY.value, L.positionZ.value, L.forwardX.value, L.forwardY.value, L.forwardZ.value, L.upX.value, L.upY.value, L.upZ.value].join(':')"),
                "the default listener");
        Check.equal("1:2:3:0:0:1:0:-1:0", page.run("L.setPosition(1, 2, 3); L.setOrientation(0, 0, 1, 0, -1, 0); [L.positionX.value, L.positionY.value, L.positionZ.value, L.forwardX.value, L.forwardY.value,"
                + " L.forwardZ.value, L.upX.value, L.upY.value, L.upZ.value].join(':')"), "setPosition and setOrientation of the listener");
        Check.equal("equalpower:inverse:1:10000:1:360:360:0", page.run("[pn.panningModel, pn.distanceModel, pn.refDistance, pn.maxDistance, pn.rolloffFactor, pn.coneInnerAngle, pn.coneOuterAngle, pn.coneOuterGain].join(':')"),
                "the defaults of a panner");
        Check.equal("4:5:6:0:0:1", page.run("pn.setPosition(4, 5, 6); pn.setOrientation(0, 0, 1); [pn.positionX.value, pn.positionY.value, pn.positionZ.value, pn.orientationX.value, pn.orientationY.value, pn.orientationZ.value].join(':')"),
                "setPosition and setOrientation of a panner");
        Check.equal("HRTF:linear:2:50:90", page.run("pn.panningModel = 'HRTF'; pn.distanceModel = 'linear'; pn.refDistance = 2; pn.coneOuterAngle = 50; pn.coneInnerAngle = 90; [pn.panningModel, pn.distanceModel, pn.refDistance, pn.maxDistance === 10000 ? 50 : 0, pn.coneInnerAngle].join(':')"),
                "attributes of a panner");
        Check.equal("RangeError", page.errorOf("pn.refDistance = -1"), "a negative reference distance");
        Check.equal("RangeError", page.errorOf("pn.maxDistance = 0"), "a maximum distance that is not positive");
        Check.equal("TypeError", page.errorOf("L.setPosition(1, 2)"), "a missing argument");
        Check.equal("TypeError", page.errorOf("L.setPosition(1, 2, NaN)"), "a non-finite argument");
    }

    /**
     * A render that has a large number of nodes and runs for a while still finishes and is correct (the rendering thread is not the thread of the page).
     * <p>
     * 노드가 많고 한동안 도는 렌더링도 끝나고 올바르다(렌더링 스레드는 페이지의 스레드가 아니다).
     */
    public void testManyNodes() {
        page.run("var ctx = new OfflineAudioContext(1, 44100, 44100); var sum = ctx.createGain(); sum.gain.value = 0.01; sum.connect(ctx.destination);"
                + " for (var i = 0; i < 50; i++) { var o = ctx.createOscillator(); o.frequency.value = 100 + 10 * i; var g = ctx.createGain(); g.gain.value = 1; o.connect(g); g.connect(sum); o.start(); }");
        render();
        double peak = page.number("var o2 = result.buf.getChannelData(0), p = 0; for (var i = 0; i < 44100; i++) { p = Math.max(p, Math.abs(o2[i])); } p");
        Check.isTrue(peak > 0.05 && peak <= 0.5 + 1e-6, "fifty sines add up: " + peak);
    }
}
