// Web Audio 참조 비교용 시나리오(Chromium 과 이 프로젝트의 엔진이 같은 스크립트를 실행한다). 외부 입력 없이 결정적이다(잡음은 LCG).
// 각 시나리오: add(이름, 채널 수, 프레임 수, 표본율, 그래프를 만드는 함수). 규칙은 generation/README.md.
var SCENARIOS = [];
function add(name, ch, frames, rate, build) { SCENARIOS.push({ name: name, ch: ch, frames: frames, rate: rate, build: build }); }
function rng(seed) { var s = seed >>> 0; return function () { s = (Math.imul(s, 1664525) + 1013904223) >>> 0; return (s / 4294967296) * 2 - 1; }; }
function noise(ctx, frames, channels, seed, amp) {
    var b = ctx.createBuffer(channels, frames, ctx.sampleRate), r = rng(seed);
    for (var c = 0; c < channels; c++) { var d = b.getChannelData(c); for (var i = 0; i < frames; i++) { d[i] = r() * amp; } }
    return b;
}
function play(ctx, buf) { var s = ctx.createBufferSource(); s.buffer = buf; s.start(0); return s; }
function osc(ctx, type, freq) { var o = ctx.createOscillator(); o.type = type; o.frequency.value = freq; o.start(0); return o; }
function gain(ctx, v) { var g = ctx.createGain(); g.gain.value = v; return g; }

// ---- 발진기 ----
add('osc_sine_441', 1, 1024, 44100, function (ctx) { osc(ctx, 'sine', 441).connect(gain(ctx, 0.5)).connect(ctx.destination); });
add('osc_sine_48k', 1, 1024, 48000, function (ctx) { osc(ctx, 'sine', 1234.5).connect(gain(ctx, 0.5)).connect(ctx.destination); });
add('osc_sine_22k', 1, 1024, 22050, function (ctx) { osc(ctx, 'sine', 777).connect(ctx.destination); });
add('osc_sine_nyq', 1, 1024, 44100, function (ctx) { osc(ctx, 'sine', 21900).connect(ctx.destination); });
var wave = [['saw', 'sawtooth', 440], ['saw5k', 'sawtooth', 5000], ['saw15k', 'sawtooth', 15000], ['saw21k', 'sawtooth', 21500], ['sq880', 'square', 880], ['sq9k', 'square', 9000], ['tri3k', 'triangle', 3000], ['tri100', 'triangle', 100]];
wave.forEach(function (w) { add('osc_' + w[0], 1, 1024, 44100, function (ctx) { osc(ctx, w[1], w[2]).connect(ctx.destination); }); });
add('osc_custom', 1, 1024, 44100, function (ctx) {
    var pw = ctx.createPeriodicWave(new Float32Array([0, 0, 0.5, 0.25]), new Float32Array([0, 0.3, 0, 0.1]));
    var o = ctx.createOscillator(); o.setPeriodicWave(pw); o.frequency.value = 600; o.start(0); o.connect(ctx.destination);
});
add('osc_custom_raw', 1, 1024, 44100, function (ctx) {
    var pw = ctx.createPeriodicWave(new Float32Array([0, 0, 0.5, 0.25]), new Float32Array([0, 0.3, 0, 0.1]), { disableNormalization: true });
    var o = ctx.createOscillator(); o.setPeriodicWave(pw); o.frequency.value = 600; o.start(0); o.connect(ctx.destination);
});
add('osc_freq_ramp', 1, 1024, 44100, function (ctx) {
    var o = osc(ctx, 'sine', 200); o.frequency.setValueAtTime(200, 0); o.frequency.linearRampToValueAtTime(2000, 1024 / 44100); o.connect(ctx.destination);
});
add('osc_detune', 1, 1024, 44100, function (ctx) { var o = osc(ctx, 'sine', 440); o.detune.value = 700; o.connect(ctx.destination); });
// 시작/정지 시각이 프레임 경계에 있는 경우(두 엔진이 같아야 한다)와 프레임 사이에 있는 경우(Chromium 은 시각을 프레임으로 반올림하고 이 엔진은 규격 문장대로 프레임 사이의 정확한 시각에서 시작한다: 알려진 차이)
add('osc_start_int', 1, 1024, 44100, function (ctx) { var o = ctx.createOscillator(); o.frequency.value = 1000; o.start(100 / 44100); o.stop(700 / 44100); o.connect(ctx.destination); });
add('osc_start_mid', 1, 1024, 44100, function (ctx) { var o = ctx.createOscillator(); o.frequency.value = 1000; o.start(100.5 / 44100); o.stop(700.25 / 44100); o.connect(ctx.destination); });

// ---- 쌍이차 필터 ----
['lowpass', 'highpass', 'bandpass', 'notch', 'allpass', 'peaking', 'lowshelf', 'highshelf'].forEach(function (t) {
    add('biquad_' + t, 1, 1024, 44100, function (ctx) {
        var f = ctx.createBiquadFilter(); f.type = t; f.frequency.value = 2000; f.Q.value = 3; f.gain.value = 6;
        play(ctx, noise(ctx, 1024, 1, 11, 0.5)).connect(f).connect(ctx.destination);
    });
});
add('biquad_sweep', 1, 1024, 44100, function (ctx) {
    var f = ctx.createBiquadFilter(); f.type = 'peaking'; f.Q.value = 2; f.gain.value = 9;
    f.frequency.setValueAtTime(500, 0); f.frequency.exponentialRampToValueAtTime(8000, 1000 / 44100);
    play(ctx, noise(ctx, 1024, 1, 12, 0.5)).connect(f).connect(ctx.destination);
});
add('biquad_22k', 1, 1024, 22050, function (ctx) {
    var f = ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 3000; f.Q.value = 5;
    play(ctx, noise(ctx, 1024, 1, 13, 0.5)).connect(f).connect(ctx.destination);
});
add('iir', 1, 1024, 44100, function (ctx) {
    var f = ctx.createIIRFilter([0.1, 0.2, 0.1], [1, -0.5, 0.2]);
    play(ctx, noise(ctx, 1024, 1, 14, 0.5)).connect(f).connect(ctx.destination);
});

// ---- 지연 ----
add('delay_frac', 1, 1024, 44100, function (ctx) {
    var d = ctx.createDelay(0.01); d.delayTime.value = 0.000523; play(ctx, noise(ctx, 1024, 1, 15, 0.5)).connect(d).connect(ctx.destination);
});
add('delay_mod', 1, 1024, 44100, function (ctx) {
    var d = ctx.createDelay(0.01); d.delayTime.setValueAtTime(0.0002, 0); d.delayTime.linearRampToValueAtTime(0.002, 1024 / 44100);
    play(ctx, noise(ctx, 1024, 1, 16, 0.5)).connect(d).connect(ctx.destination);
});
add('delay_feedback', 1, 1024, 44100, function (ctx) {
    var b = ctx.createBuffer(1, 1024, 44100), r = rng(17), x = b.getChannelData(0); for (var i = 0; i < 200; i++) { x[i] = r() * 0.5; }
    var d = ctx.createDelay(0.01); d.delayTime.value = 0.001; var g = gain(ctx, 0.5);
    var s = play(ctx, b); s.connect(d); d.connect(g); g.connect(d); d.connect(ctx.destination);
});

// ---- AudioParam 자동화(상수 원천 x 게인 파라미터) ----
function autoCase(name, setup) {
    add('auto_' + name, 1, 1024, 44100, function (ctx) {
        var c = ctx.createConstantSource(); c.offset.value = 1; c.start(0); var g = ctx.createGain(); g.gain.value = 0;
        setup(g.gain); c.connect(g).connect(ctx.destination);
    });
}
autoCase('linear_exp', function (p) { p.setValueAtTime(0.1, 0); p.linearRampToValueAtTime(1, 0.01); p.exponentialRampToValueAtTime(0.01, 0.02); });
autoCase('target', function (p) { p.setValueAtTime(0, 0); p.setTargetAtTime(1, 0.005, 0.003); });
autoCase('curve', function (p) { p.setValueCurveAtTime(new Float32Array([0, 1, 0.25, 0.75, 0]), 0.002, 0.012); });
autoCase('hold', function (p) { p.setValueAtTime(0, 0); p.linearRampToValueAtTime(1, 0.02); p.cancelAndHoldAtTime(0.01); });
autoCase('cancel', function (p) { p.setValueAtTime(0.2, 0); p.linearRampToValueAtTime(1, 0.01); p.setValueAtTime(0.5, 0.015); p.cancelScheduledValues(0.012); });
autoCase('steps', function (p) { p.setValueAtTime(0.25, 0.001); p.setValueAtTime(0.75, 0.004); p.setValueAtTime(0.1, 0.0123); });

// ---- 버퍼 원천 ----
add('bufsrc_rate075', 1, 1024, 44100, function (ctx) { var s = play(ctx, noise(ctx, 1500, 1, 21, 0.5)); s.playbackRate.value = 0.75; s.connect(ctx.destination); });
add('bufsrc_rate13', 1, 1024, 44100, function (ctx) { var s = play(ctx, noise(ctx, 1500, 1, 22, 0.5)); s.playbackRate.value = 1.3; s.connect(ctx.destination); });
add('bufsrc_loop_frac', 1, 1024, 44100, function (ctx) {
    var s = ctx.createBufferSource(); s.buffer = noise(ctx, 1500, 1, 23, 0.5); s.loop = true; s.loopStart = 0.0007; s.loopEnd = 0.0201; s.start(0); s.connect(ctx.destination);
});
add('bufsrc_detune', 1, 1024, 44100, function (ctx) { var s = play(ctx, noise(ctx, 1500, 1, 24, 0.5)); s.detune.value = -500; s.connect(ctx.destination); });
add('bufsrc_offset_int', 1, 1024, 44100, function (ctx) {
    var s = ctx.createBufferSource(); s.buffer = noise(ctx, 1500, 1, 25, 0.5); s.start(100 / 44100, 132 / 44100, 529 / 44100); s.connect(ctx.destination);
});
// offset 0.003 초는 132.3 프레임이다: Chromium 은 정수 프레임(132)에서 시작하고 이 엔진은 규격대로 132.3 프레임 위치를 보간한다(알려진 차이)
add('bufsrc_offset', 1, 1024, 44100, function (ctx) {
    var s = ctx.createBufferSource(); s.buffer = noise(ctx, 1500, 1, 25, 0.5); s.start(100 / 44100, 0.003, 0.012); s.connect(ctx.destination);
});
add('bufsrc_rate_48k', 1, 1024, 48000, function (ctx) {
    var b = noise(ctx, 1500, 1, 26, 0.5); var s = play(ctx, b); s.playbackRate.value = 1.1; s.connect(ctx.destination);
});

// ---- 웨이브셰이퍼 ----
var curve = new Float32Array(21); for (var ci = 0; ci < 21; ci++) { curve[ci] = Math.tanh(3 * (ci / 10 - 1)); }
['none', '2x', '4x'].forEach(function (o) {
    add('shaper_' + o, 1, 1024, 44100, function (ctx) {
        var w = ctx.createWaveShaper(); w.curve = curve; w.oversample = o; osc(ctx, 'sine', 1000).connect(gain(ctx, 0.9)).connect(w).connect(ctx.destination);
    });
});

// ---- 컨볼버 ----
function ir(ctx) { var b = noise(ctx, 300, 1, 31, 0.2), d = b.getChannelData(0); for (var i = 0; i < 300; i++) { d[i] *= Math.exp(-i / 80); } return b; }
function burst(ctx) { var b = ctx.createBuffer(1, 1024, 44100), r = rng(32), d = b.getChannelData(0); for (var i = 0; i < 100; i++) { d[i] = r() * 0.5; } return b; }
add('conv_norm', 1, 1024, 44100, function (ctx) { var c = ctx.createConvolver(); c.buffer = ir(ctx); play(ctx, burst(ctx)).connect(c).connect(ctx.destination); });
add('conv_raw', 1, 1024, 44100, function (ctx) { var c = ctx.createConvolver(); c.normalize = false; c.buffer = ir(ctx); play(ctx, burst(ctx)).connect(c).connect(ctx.destination); });

// ---- 컴프레서 ----
add('compressor', 1, 6144, 44100, function (ctx) {
    var g = ctx.createGain(); g.gain.setValueAtTime(0.1, 0); g.gain.setValueAtTime(0.9, 0.04);
    var c = ctx.createDynamicsCompressor(); osc(ctx, 'sine', 440).connect(g).connect(c).connect(ctx.destination);
});

// ---- 패너 ----
add('stereopan_mono', 2, 1024, 44100, function (ctx) { var p = ctx.createStereoPanner(); p.pan.value = -0.3; osc(ctx, 'sine', 700).connect(p).connect(ctx.destination); });
add('stereopan_stereo', 2, 1024, 44100, function (ctx) { var p = ctx.createStereoPanner(); p.pan.value = 0.8; play(ctx, noise(ctx, 1024, 2, 41, 0.5)).connect(p).connect(ctx.destination); });
add('panner_inverse', 2, 1024, 44100, function (ctx) {
    var p = ctx.createPanner(); p.panningModel = 'equalpower'; p.distanceModel = 'inverse'; p.refDistance = 1; p.rolloffFactor = 1.5;
    p.positionX.value = 3; p.positionY.value = 0; p.positionZ.value = -4; osc(ctx, 'sine', 500).connect(p).connect(ctx.destination);
});
add('panner_linear', 2, 1024, 44100, function (ctx) {
    var p = ctx.createPanner(); p.distanceModel = 'linear'; p.refDistance = 1; p.maxDistance = 20; p.rolloffFactor = 0.8;
    p.positionX.value = -2; p.positionY.value = 1; p.positionZ.value = -5; osc(ctx, 'sine', 500).connect(p).connect(ctx.destination);
});
add('panner_exponential', 2, 1024, 44100, function (ctx) {
    var p = ctx.createPanner(); p.distanceModel = 'exponential'; p.refDistance = 2; p.rolloffFactor = 1.2;
    p.positionX.value = 1; p.positionY.value = 0; p.positionZ.value = 6; osc(ctx, 'sine', 500).connect(p).connect(ctx.destination);
});
add('panner_cone', 2, 1024, 44100, function (ctx) {
    var p = ctx.createPanner(); p.distanceModel = 'inverse'; p.refDistance = 1; p.positionX.value = 0; p.positionY.value = 0; p.positionZ.value = -2;
    p.orientationX.value = 1; p.orientationY.value = 0; p.orientationZ.value = 0; p.coneInnerAngle = 40; p.coneOuterAngle = 120; p.coneOuterGain = 0.3;
    osc(ctx, 'sine', 500).connect(p).connect(ctx.destination);
});
add('panner_listener', 2, 1024, 44100, function (ctx) {
    var l = ctx.listener; l.positionX.value = 1; l.positionY.value = 0; l.positionZ.value = 1; l.forwardX.value = 1; l.forwardY.value = 0; l.forwardZ.value = 0;
    var p = ctx.createPanner(); p.positionX.value = 1; p.positionY.value = 0; p.positionZ.value = -3; osc(ctx, 'sine', 500).connect(p).connect(ctx.destination);
});

// ---- 채널 믹스 ----
add('mix_stereo_to_mono', 1, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 2, 51, 0.5)).connect(ctx.destination); });
add('mix_mono_to_stereo', 2, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 1, 52, 0.5)).connect(ctx.destination); });
add('mix_quad_to_stereo', 2, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 4, 53, 0.4)).connect(ctx.destination); });
add('mix_51_to_stereo', 2, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 6, 54, 0.3)).connect(ctx.destination); });
add('mix_stereo_to_51', 6, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 2, 55, 0.5)).connect(ctx.destination); });
add('mix_51_to_mono', 1, 1024, 44100, function (ctx) { play(ctx, noise(ctx, 1024, 6, 56, 0.3)).connect(ctx.destination); });
add('mix_discrete', 2, 1024, 44100, function (ctx) {
    var g = ctx.createGain(); g.channelInterpretation = 'discrete'; play(ctx, noise(ctx, 1024, 1, 57, 0.5)).connect(g).connect(ctx.destination);
});
add('splitter_merger', 2, 1024, 44100, function (ctx) {
    var sp = ctx.createChannelSplitter(2), mg = ctx.createChannelMerger(2), g = gain(ctx, 0.5);
    play(ctx, noise(ctx, 1024, 2, 58, 0.5)).connect(sp); sp.connect(g, 0); g.connect(mg, 0, 1); sp.connect(mg, 1, 0); mg.connect(ctx.destination);
});

// ---- 파라미터 연결(진폭 변조) ----
add('am', 1, 1024, 44100, function (ctx) {
    var g = ctx.createGain(); g.gain.value = 0.5; osc(ctx, 'sine', 800).connect(g); var m = osc(ctx, 'sine', 50); m.connect(gain(ctx, 0.4)).connect(g.gain); g.connect(ctx.destination);
});
