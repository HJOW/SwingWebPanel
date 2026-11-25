package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script interfaces of the Web Audio API on a real page (plan item P25-05, JavaScript binding): that every interface of the target list exists with the members
 * of the specification and no others, the prototype chains and <code>instanceof</code>, <code>Symbol.toStringTag</code>, the constructors and the factories for all nodes, the WebIDL
 * conversions (a missing required argument, a non-finite number, an invalid enumeration for an attribute and for an argument), the errors that the engine raises as
 * <code>DOMException</code> with the names of the specification, the identity of the objects that scripts get, and the names that are not exposed (the media stream
 * interfaces, the worklet).
 * <p>
 * 실제 페이지에서 Web Audio API 의 스크립트 인터페이스 시험(계획 P25-05, JavaScript 연결): 대상 목록의 모든 인터페이스가 규격의 멤버를 정확히 가지고 있는지, 프로토타입 사슬과
 * <code>instanceof</code>, <code>Symbol.toStringTag</code>, 모든 노드의 생성자와 팩터리, WebIDL 변환(필수 인수 누락, 유한하지 않은 수, 속성과 인수의 잘못된 열거형), 엔진이 규격의
 * 이름을 가진 <code>DOMException</code> 으로 올리는 오류, 스크립트가 얻는 객체의 동일성, 노출하지 않는 이름(미디어 스트림 인터페이스, worklet).
 */
public class WebAudioInterfaceTest {

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
    public WebAudioInterfaceTest() {
    }

    /**
     * Loads a page before each case.
     * <p>
     * 사례마다 페이지를 로드한다.
     */
    public void setUp() {
        page = new WebAudioPage();
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
     * Checks that Web Audio typed-array input and output reject shared or resizable backing.
     * <p>
     * Web Audio의 형식화 배열 입력과 출력이 공유 또는 가변 저장소를 거부하는지 확인한다.
     */
    public void testSharedMemoryAudioArrayBoundary() {
        String script = "function fail(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "var ctx=new OfflineAudioContext(1,128,8000), buf=ctx.createBuffer(1,8,8000), analyser=ctx.createAnalyser();"
                + "var shared=new SharedArrayBuffer(32), floats=new Float32Array(shared), bytes=new Uint8Array(shared);"
                + "var rab=new Float32Array(new ArrayBuffer(32,{maxByteLength:64}));floats[0]=3;"
                + "[fail(()=>buf.copyFromChannel(floats,0)),fail(()=>buf.copyToChannel(floats,0)),"
                + "fail(()=>analyser.getByteTimeDomainData(bytes)),fail(()=>buf.copyToChannel(rab,0)),floats[0],buf.getChannelData(0)[0]].join('|')";
        Check.equal("TypeError|TypeError|TypeError|TypeError|3|0", page.run(script),
                "Web Audio typed array methods fail before changing either storage block");
    }

    /**
     * The own property names of an interface prototype, sorted, as the specification lists them: no member is missing and none is extra.
     * <p>
     * 인터페이스 프로토타입의 자체 프로퍼티 이름(정렬). 규격이 나열한 대로이며 빠진 멤버도 남는 멤버도 없다.
     */
    public void testInterfaceMembers() {
        String[][] table = {
            {"BaseAudioContext", "audioWorklet,constructor,createAnalyser,createBiquadFilter,createBuffer,createBufferSource,createChannelMerger,createChannelSplitter,createConstantSource,"
                + "createConvolver,createDelay,createDynamicsCompressor,createGain,createIIRFilter,createOscillator,createPanner,createPeriodicWave,createScriptProcessor,"
                + "createStereoPanner,createWaveShaper,currentTime,decodeAudioData,destination,listener,onstatechange,renderQuantumSize,sampleRate,state"},
            {"AudioContext", "baseLatency,close,constructor,createMediaElementSource,createMediaStreamDestination,createMediaStreamSource,createMediaStreamTrackSource,"
                + "getOutputTimestamp,onsinkchange,outputLatency,resume,setSinkId,sinkId,suspend"},
            {"OfflineAudioContext", "constructor,length,oncomplete,resume,startRendering,suspend"},
            {"AudioNode", "channelCount,channelCountMode,channelInterpretation,connect,constructor,context,disconnect,numberOfInputs,numberOfOutputs"},
            {"AudioScheduledSourceNode", "constructor,onended,start,stop"},
            {"AudioDestinationNode", "constructor,maxChannelCount"},
            {"AudioBufferSourceNode", "buffer,constructor,detune,loop,loopEnd,loopStart,playbackRate,start"},
            {"OscillatorNode", "constructor,detune,frequency,setPeriodicWave,type"},
            {"Worklet", "addModule,constructor"},
            {"AudioWorklet", "constructor"},
            {"AudioWorkletNode", "constructor,onprocessorerror,parameters,port"},
            {"AudioParamMap", "constructor,entries,forEach,get,has,keys,size,values"},
            {"ConstantSourceNode", "constructor,offset"},
            {"GainNode", "constructor,gain"},
            {"DelayNode", "constructor,delayTime"},
            {"BiquadFilterNode", "Q,constructor,detune,frequency,gain,getFrequencyResponse,type"},
            {"IIRFilterNode", "constructor,getFrequencyResponse"},
            {"StereoPannerNode", "constructor,pan"},
            {"ChannelSplitterNode", "constructor"},
            {"ChannelMergerNode", "constructor"},
            {"WaveShaperNode", "constructor,curve,oversample"},
            {"ConvolverNode", "buffer,constructor,normalize"},
            {"DynamicsCompressorNode", "attack,constructor,knee,ratio,reduction,release,threshold"},
            {"AnalyserNode", "constructor,fftSize,frequencyBinCount,getByteFrequencyData,getByteTimeDomainData,getFloatFrequencyData,getFloatTimeDomainData,maxDecibels,minDecibels,"
                + "smoothingTimeConstant"},
            {"PannerNode", "coneInnerAngle,coneOuterAngle,coneOuterGain,constructor,distanceModel,maxDistance,orientationX,orientationY,orientationZ,panningModel,positionX,"
                + "positionY,positionZ,refDistance,rolloffFactor,setOrientation,setPosition"},
            {"AudioParam", "automationRate,cancelAndHoldAtTime,cancelScheduledValues,constructor,defaultValue,exponentialRampToValueAtTime,linearRampToValueAtTime,maxValue,minValue,"
                + "setTargetAtTime,setValueAtTime,setValueCurveAtTime,value"},
            {"AudioListener", "constructor,forwardX,forwardY,forwardZ,positionX,positionY,positionZ,setOrientation,setPosition,upX,upY,upZ"},
            {"PeriodicWave", "constructor"},
            {"AudioBuffer", "constructor,copyFromChannel,copyToChannel,duration,getChannelData,length,numberOfChannels,sampleRate"},
            {"OfflineAudioCompletionEvent", "constructor,renderedBuffer"},
            {"AudioProcessingEvent", "constructor,inputBuffer,outputBuffer,playbackTime"},
        };
        // 인터페이스마다 프로토타입의 자체 프로퍼티 이름을 규격의 목록과 비교한다.
        for (int i = 0; i < table.length; i++) {
            Check.equal(table[i][1], page.run("Object.getOwnPropertyNames(" + table[i][0] + ".prototype).sort().join(',')"), "the members of " + table[i][0]);
        }
    }

    /**
     * The prototype chains, <code>instanceof</code> and the string tags of the interfaces and of the objects that scripts create.
     * <p>
     * 인터페이스의 프로토타입 사슬, <code>instanceof</code>, 문자열 태그와 스크립트가 만드는 객체.
     */
    public void testPrototypeChainsAndTags() {
        Check.equal("true", page.run("String(Object.getPrototypeOf(AudioNode.prototype) === EventTarget.prototype)"), "AudioNode is an EventTarget");
        Check.equal("true", page.run("String(Object.getPrototypeOf(OscillatorNode.prototype) === AudioScheduledSourceNode.prototype"
                + " && Object.getPrototypeOf(AudioScheduledSourceNode.prototype) === AudioNode.prototype)"), "OscillatorNode chain");
        Check.equal("true", page.run("String(Object.getPrototypeOf(GainNode.prototype) === AudioNode.prototype)"), "GainNode chain");
        Check.equal("true", page.run("String(Object.getPrototypeOf(AudioContext.prototype) === BaseAudioContext.prototype"
                + " && Object.getPrototypeOf(OfflineAudioContext.prototype) === BaseAudioContext.prototype"
                + " && Object.getPrototypeOf(BaseAudioContext.prototype) === EventTarget.prototype)"), "the contexts");
        Check.equal("true", page.run("String(Object.getPrototypeOf(OfflineAudioCompletionEvent.prototype) === Event.prototype)"), "the event");
        page.run("var ctx = new OfflineAudioContext(2, 128, 44100); var rt = new AudioContext();");
        Check.equal("true:true:true:false", page.run("[ctx instanceof OfflineAudioContext, ctx instanceof BaseAudioContext, ctx instanceof EventTarget, ctx instanceof AudioContext].join(':')"),
                "instanceof of an offline context");
        Check.equal("true:true:false", page.run("[rt instanceof AudioContext, rt instanceof BaseAudioContext, rt instanceof OfflineAudioContext].join(':')"), "instanceof of a context");
        Check.equal("[object OfflineAudioContext]:[object AudioContext]", page.run("Object.prototype.toString.call(ctx) + ':' + Object.prototype.toString.call(rt)"), "toStringTag");
        Check.equal("[object GainNode]:[object AudioParam]:[object AudioDestinationNode]:[object AudioListener]:[object AudioBuffer]:[object PeriodicWave]",
                page.run("[ctx.createGain(), ctx.createGain().gain, ctx.destination, ctx.listener, ctx.createBuffer(1, 1, 8000), ctx.createPeriodicWave([0, 1], [0, 0])]"
                        + ".map(function (o) { return Object.prototype.toString.call(o); }).join(':')"), "tags of the objects");
        Check.equal("true:true:true", page.run("var o = ctx.createOscillator(); [o instanceof OscillatorNode, o instanceof AudioScheduledSourceNode, o instanceof AudioNode].join(':')"),
                "instanceof of a node");
        Check.equal("true:true", page.run("[ctx.createBufferSource() instanceof AudioBufferSourceNode, ctx.createConstantSource() instanceof ConstantSourceNode].join(':')"), "sources");
        Check.equal("webkit:true", page.run("(typeof webkitAudioContext === 'function' && webkitAudioContext === AudioContext ? 'webkit' : 'none') + ':' + (webkitOfflineAudioContext === OfflineAudioContext)"),
                "the prefixed names are the same constructors");
        rtClose();
    }

    /**
     * Closes the real-time context of a case.
     * <p>
     * 사례의 실시간 컨텍스트를 닫는다.
     */
    private void rtClose() {
        page.run("rt.close(); 1");
        page.pump();
    }

    /**
     * Every node can be created with its constructor and with the factory of the context, with options and without; the options are applied (parameters, attributes, the
     * <code>AudioNodeOptions</code>), the constructor needs a context and the factory needs a receiver of the right kind.
     * <p>
     * 모든 노드를 생성자와 컨텍스트의 팩터리로, 옵션과 함께 또는 없이 만들 수 있다. 옵션이 적용되고(파라미터, 속성, <code>AudioNodeOptions</code>), 생성자는 컨텍스트가 필요하고
     * 팩터리는 올바른 종류의 수신자가 필요하다.
     */
    public void testConstructorsAndFactories() {
        page.run("var ctx = new OfflineAudioContext(2, 128, 44100);");
        String[][] nodes = {
            {"AnalyserNode", "createAnalyser"}, {"AudioBufferSourceNode", "createBufferSource"}, {"BiquadFilterNode", "createBiquadFilter"},
            {"ChannelMergerNode", "createChannelMerger"}, {"ChannelSplitterNode", "createChannelSplitter"}, {"ConstantSourceNode", "createConstantSource"},
            {"ConvolverNode", "createConvolver"}, {"DelayNode", "createDelay"}, {"DynamicsCompressorNode", "createDynamicsCompressor"}, {"GainNode", "createGain"},
            {"OscillatorNode", "createOscillator"}, {"PannerNode", "createPanner"}, {"StereoPannerNode", "createStereoPanner"}, {"WaveShaperNode", "createWaveShaper"},
        };
        // 노드마다 생성자와 팩터리가 같은 인터페이스의 객체를 만드는지 확인한다.
        for (int i = 0; i < nodes.length; i++) {
            Check.equal("true:true:true", page.run("var a = new " + nodes[i][0] + "(ctx), b = ctx." + nodes[i][1] + "(); [a instanceof " + nodes[i][0] + ", b instanceof "
                    + nodes[i][0] + ", a.context === ctx && b.context === ctx].join(':')"), "construct " + nodes[i][0]);
        }
        Check.equal("true:true", page.run("var iir = new IIRFilterNode(ctx, {feedforward: [1], feedback: [1]}); var iir2 = ctx.createIIRFilter([1, 0.5], [1, -0.2]);"
                + " [iir instanceof IIRFilterNode, iir2 instanceof IIRFilterNode].join(':')"), "the IIR filter");
        // 옵션이 적용된다: 파라미터 값, 속성, AudioNodeOptions.
        Check.equal("0.25:2:explicit:discrete", page.run("var g = new GainNode(ctx, {gain: 0.25, channelCount: 2, channelCountMode: 'explicit', channelInterpretation: 'discrete'});"
                + " [g.gain.value, g.channelCount, g.channelCountMode, g.channelInterpretation].join(':')"), "the options of a gain node");
        Check.equal("square:880:7", page.run("var o = new OscillatorNode(ctx, {type: 'square', frequency: 880, detune: 7}); [o.type, o.frequency.value, o.detune.value].join(':')"),
                "the options of an oscillator");
        Check.equal("highpass:3:100", page.run("var f = new BiquadFilterNode(ctx, {type: 'highpass', Q: 3, frequency: 100}); [f.type, f.Q.value, f.frequency.value].join(':')"), "biquad options");
        Check.equal("2048:-90:-20:0.5", page.run("var a = new AnalyserNode(ctx, {fftSize: 2048, minDecibels: -90, maxDecibels: -20, smoothingTimeConstant: 0.5});"
                + " [a.fftSize, a.minDecibels, a.maxDecibels, a.smoothingTimeConstant].join(':')"), "analyser options");
        Check.equal("2:3", page.run("var d = new DelayNode(ctx, {maxDelayTime: 2, delayTime: 0.1}); var s = new ChannelSplitterNode(ctx, {numberOfOutputs: 3});"
                + " var m = new ChannelMergerNode(ctx, {numberOfInputs: 4}); [d.numberOfOutputs + 1, s.numberOfOutputs].join(':')"), "counts");
        Check.equal("6:6:1:1:0:1", page.run("var s6 = ctx.createChannelSplitter(), m6 = ctx.createChannelMerger(); [s6.numberOfOutputs, m6.numberOfInputs, s6.numberOfInputs, m6.numberOfOutputs,"
                + " ctx.destination.numberOfOutputs, ctx.destination.numberOfInputs].join(':')"), "the default numbers of inputs and outputs");
        Check.equal("TypeError", page.errorOf("new GainNode()"), "a context is required");
        Check.equal("TypeError", page.errorOf("new GainNode({})"), "a plain object is no context");
        Check.equal("TypeError", page.errorOf("GainNode(ctx)"), "the interface cannot be called");
        Check.equal("TypeError", page.errorOf("new BaseAudioContext()"), "BaseAudioContext has no constructor");
        Check.equal("TypeError", page.errorOf("new AudioParam()"), "an AudioParam cannot be constructed");
        Check.equal("TypeError", page.errorOf("new AudioDestinationNode(ctx)"), "an AudioDestinationNode cannot be constructed");
        Check.equal("TypeError", page.errorOf("ctx.createGain.call({})"), "a factory needs a context");
        Check.equal("TypeError", page.errorOf("new IIRFilterNode(ctx, {feedforward: [1]})"), "feedback is required");
        Check.equal("TypeError", page.errorOf("new GainNode(ctx, 5)"), "options must be a dictionary");
        Check.equal("TypeError", page.errorOf("new OscillatorNode(ctx, {type: 'noise'})"), "an invalid enumeration in a dictionary");
        Check.equal("NotSupportedError", page.errorOf("new GainNode(ctx, {channelCount: 0})"), "channelCount 0");
        Check.equal("NotSupportedError", page.errorOf("ctx.createDelay(0)"), "maxDelayTime 0");
        Check.equal("InvalidStateError", page.errorOf("new OscillatorNode(ctx, {type: 'custom'})"), "the type custom without a wave");
        Check.equal("TypeError", page.errorOf("new GainNode(ctx, {gain: NaN})"), "a non-finite parameter option");
        Check.equal("true", page.run("var g2 = ctx.createGain(); String(g2.gain === g2.gain && ctx.destination === ctx.destination && ctx.listener === ctx.listener)"), "the same object each time");
    }

    /**
     * WebIDL conversions of attributes and arguments: a number that is not finite, a missing required argument, an invalid enumeration (ignored when an attribute is assigned,
     * a <code>TypeError</code> for an argument), <code>unsigned long</code> wrapping and the read-only attributes.
     * <p>
     * 속성과 인수의 WebIDL 변환: 유한하지 않은 수, 빠진 필수 인수, 잘못된 열거형(속성에 대입하면 무시되고 인수이면 <code>TypeError</code>), <code>unsigned long</code> 변환, 읽기 전용 속성.
     */
    public void testWebIdlConversions() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 44100); var g = ctx.createGain(); var o = ctx.createOscillator(); var p = g.gain;");
        Check.equal("TypeError", page.errorOf("p.value = NaN"), "NaN value");
        Check.equal("TypeError", page.errorOf("p.value = Infinity"), "Infinity value");
        Check.equal("TypeError", page.errorOf("p.setValueAtTime(1)"), "a missing argument");
        Check.equal("TypeError", page.errorOf("p.setValueAtTime(NaN, 0)"), "a non-finite value");
        Check.equal("TypeError", page.errorOf("p.setValueAtTime(1, Infinity)"), "a non-finite time");
        Check.equal("TypeError", page.errorOf("p.setValueAtTime(1e50, 0)"), "a float overflow");
        Check.equal("0.5", page.run("p.value = '0.5'; String(p.value)"), "a string is converted");
        Check.equal("0.10000000149011612", page.run("p.value = 0.1; String(p.value)"), "float precision");
        Check.equal("sine", page.run("o.type = 'bogus'; o.type"), "an invalid enumeration is ignored for an attribute");
        Check.equal("triangle", page.run("o.type = 'triangle'; o.type"), "a valid enumeration");
        Check.equal("TypeError", page.errorOf("ctx.createPanner().panningModel = 'x'; new PannerNode(ctx, {panningModel: 'x'})"), "an invalid enumeration is an error for a dictionary");
        Check.equal("max", page.run("g.channelCountMode = 'nonsense'; g.channelCountMode"), "channelCountMode");
        Check.equal("a-rate", page.run("p.automationRate"), "automationRate");
        Check.equal("TypeError", page.errorOf("g.connect()"), "connect without an argument");
        Check.equal("TypeError", page.errorOf("g.connect({})"), "connect to a plain object");
        Check.equal("TypeError", page.errorOf("AudioNode.prototype.connect.call({}, g)"), "connect on a plain object");
        Check.equal("TypeError", page.errorOf("Object.getOwnPropertyDescriptor(AudioNode.prototype, 'numberOfInputs').get.call({})"), "Illegal invocation");
        Check.equal("true", page.run("var d = Object.getOwnPropertyDescriptor(AudioNode.prototype, 'numberOfInputs'); String(typeof d.get === 'function' && d.set === undefined && d.enumerable && d.configurable)"),
                "a read-only attribute");
        Check.equal("true", page.run("var d2 = Object.getOwnPropertyDescriptor(AudioParam.prototype, 'value'); String(typeof d2.get === 'function' && typeof d2.set === 'function' && d2.enumerable)"),
                "a read-write attribute");
        Check.equal("2:2:0", page.run("[AudioParam.prototype.setValueAtTime.length, AudioParam.prototype.setTargetAtTime.length - 1, AudioScheduledSourceNode.prototype.start.length].join(':')"),
                "the lengths of the operations");
        Check.equal("0", page.run("p.value = 0.25; p.value = 0; String(p.value)"), "assign 0");
        Check.equal("IndexSizeError", page.errorOf("g.connect(ctx.destination, 5)"), "an output out of range");
        Check.equal("IndexSizeError", page.errorOf("g.connect(ctx.destination, 0, 5)"), "an input out of range");
        Check.equal("IndexSizeError", page.errorOf("g.connect(ctx.destination, 4294967295)"), "an unsigned long that is far out of range");
        Check.equal("InvalidAccessError", page.errorOf("g.connect(new OfflineAudioContext(1, 128, 44100).createGain())"), "nodes of different contexts");
        Check.equal("InvalidAccessError", page.errorOf("g.disconnect(o)"), "disconnect of an unconnected node");
        Check.equal("InvalidAccessError", page.errorOf("g.connect(new OfflineAudioContext(1, 128, 44100).createGain().gain)"), "a parameter of another context");
        Check.equal("RangeError", page.errorOf("o.start(-1)"), "a negative start time");
        Check.equal("InvalidStateError", page.errorOf("o.start(0); o.start(0)"), "start twice");
        Check.equal("InvalidStateError", page.errorOf("ctx.createOscillator().stop(1)"), "stop before start");
    }

    /**
     * The names that must not exist: the media stream interfaces; the factories that are refused say so with <code>NotSupportedError</code>. The script processor
     * and the media element source exist since the output stage (their tests are in <code>WebAudioOutputScriptTest</code> and <code>MediaElementSourceScriptTest</code>)
     * and the worklet since P25-06.
     * <p>
     * 있으면 안 되는 이름: 미디어 스트림 인터페이스. 거부하는 팩터리는 <code>NotSupportedError</code> 로 그렇게 알린다. 스크립트 프로세서와 미디어 요소 원천은
     * 출력 단계부터(시험은 <code>WebAudioOutputScriptTest</code> 와 <code>MediaElementSourceScriptTest</code> 에 있다), worklet 은 P25-06 부터 있다.
     */
    public void testExcludedNames() {
        Check.equal("undefined:undefined:undefined:function:function:function:function", page.run("[typeof MediaStreamAudioSourceNode, typeof MediaStreamAudioDestinationNode, typeof MediaStreamTrackAudioSourceNode,"
                + " typeof AudioWorkletNode, typeof AudioWorklet, typeof ScriptProcessorNode, typeof MediaElementAudioSourceNode].join(':')"), "excluded interfaces");
        page.run("var ctx = new OfflineAudioContext(1, 128, 44100); var rt = new AudioContext();");
        Check.equal("no error", page.errorOf("ctx.createScriptProcessor(4096, 1, 1)"), "the script processor exists");
        Check.equal("NotSupportedError", page.errorOf("rt.createMediaStreamDestination()"), "createMediaStreamDestination");
        Check.equal("NotSupportedError", page.errorOf("rt.createMediaStreamSource({})"), "createMediaStreamSource");
        Check.equal("NotSupportedError", page.errorOf("rt.createMediaStreamTrackSource({})"), "createMediaStreamTrackSource");
        Check.equal("TypeError", page.errorOf("rt.createMediaElementSource(document.body)"), "createMediaElementSource of something that is no media element");
        Check.equal("object", page.run("typeof ctx.audioWorklet"), "audioWorklet exists since P25-06 (D1)");
        rtClose();
    }

    /**
     * The buffer factories and the wave factory: arguments, defaults, errors with the names of the specification.
     * <p>
     * 버퍼와 파형의 팩터리: 인수, 기본값, 규격의 이름을 가진 오류.
     */
    public void testBufferAndWaveFactories() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 44100);");
        Check.equal("2:100:44100:100", page.run("var b = ctx.createBuffer(2, 100, 44100); [b.numberOfChannels, b.length, b.sampleRate, Math.round(b.duration * 44100)].join(':')"), "createBuffer");
        Check.equal("1:5:8000", page.run("var b2 = new AudioBuffer({length: 5, sampleRate: 8000}); [b2.numberOfChannels, b2.length, b2.sampleRate].join(':')"), "the AudioBuffer constructor");
        Check.equal("NotSupportedError", page.errorOf("ctx.createBuffer(0, 100, 44100)"), "0 channels");
        Check.equal("NotSupportedError", page.errorOf("ctx.createBuffer(1, 0, 44100)"), "length 0");
        Check.equal("NotSupportedError", page.errorOf("ctx.createBuffer(33, 1, 44100)"), "33 channels");
        Check.equal("NotSupportedError", page.errorOf("ctx.createBuffer(1, 1, 100)"), "a sample rate out of range");
        Check.equal("TypeError", page.errorOf("ctx.createBuffer(1, 1)"), "a missing argument");
        Check.equal("TypeError", page.errorOf("new AudioBuffer({length: 5})"), "a missing required member");
        Check.equal("TypeError", page.errorOf("new AudioBuffer()"), "a missing options argument");
        Check.equal("IndexSizeError", page.errorOf("b.getChannelData(2)"), "a channel out of range");
        Check.equal("IndexSizeError", page.errorOf("b.copyFromChannel(new Float32Array(1), 5)"), "copyFromChannel channel out of range");
        Check.equal("TypeError", page.errorOf("b.copyToChannel([1, 2], 0)"), "copyToChannel needs a Float32Array");
        Check.equal("true:true", page.run("var w = new PeriodicWave(ctx, {real: [0, 1], imag: [0, 0]}); var w2 = ctx.createPeriodicWave(new Float32Array([0, 0, 1]), new Float32Array([0, 1, 0]), {disableNormalization: true});"
                + " [w instanceof PeriodicWave, w2 instanceof PeriodicWave].join(':')"), "periodic waves");
        Check.equal("IndexSizeError", page.errorOf("ctx.createPeriodicWave([0], [0])"), "a wave of one coefficient");
        Check.equal("IndexSizeError", page.errorOf("ctx.createPeriodicWave([0, 1], [0, 1, 2])"), "arrays of different length");
        Check.equal("TypeError", page.errorOf("ctx.createPeriodicWave([0, 1])"), "imag is required");
        Check.equal("TypeError", page.errorOf("ctx.createPeriodicWave([0, NaN], [0, 1])"), "a non-finite coefficient");
        Check.equal("TypeError", page.errorOf("ctx.createPeriodicWave(5, [0, 1])"), "not a sequence");
        Check.equal("custom", page.run("var osc = ctx.createOscillator(); osc.setPeriodicWave(w); osc.type"), "setPeriodicWave makes the type custom");
        Check.equal("TypeError", page.errorOf("osc.setPeriodicWave({})"), "not a PeriodicWave");
        Check.equal("InvalidStateError", page.errorOf("osc.type = 'custom'; var o3 = ctx.createOscillator(); o3.type = 'custom'"), "type custom by hand");
    }

    /**
     * The completion event can be constructed by scripts, and the processing event too: required members, the init dictionary, the prototypes.
     * <p>
     * 완료 이벤트와 처리 이벤트는 스크립트가 생성할 수 있다: 필수 멤버, 초기화 사전, 프로토타입.
     */
    public void testEventConstructors() {
        page.run("var ctx = new OfflineAudioContext(1, 128, 44100); var b = ctx.createBuffer(1, 4, 44100);");
        Check.equal("complete:true:true:true", page.run("var e = new OfflineAudioCompletionEvent('complete', {renderedBuffer: b});"
                + " [e.type, e.renderedBuffer === b, e instanceof Event, e instanceof OfflineAudioCompletionEvent].join(':')"), "the completion event");
        Check.equal("TypeError", page.errorOf("new OfflineAudioCompletionEvent('complete', {})"), "renderedBuffer is required");
        Check.equal("TypeError", page.errorOf("new OfflineAudioCompletionEvent('complete')"), "the init dictionary is required");
        Check.equal("TypeError", page.errorOf("new OfflineAudioCompletionEvent('complete', {renderedBuffer: {}})"), "renderedBuffer must be a buffer");
        Check.equal("audioprocess:1.5:true:true", page.run("var pe = new AudioProcessingEvent('audioprocess', {playbackTime: 1.5, inputBuffer: b, outputBuffer: b});"
                + " [pe.type, pe.playbackTime, pe.inputBuffer === b, pe.outputBuffer === b].join(':')"), "the processing event");
        Check.equal("TypeError", page.errorOf("new AudioProcessingEvent('audioprocess', {playbackTime: 1})"), "required members");
    }
}
