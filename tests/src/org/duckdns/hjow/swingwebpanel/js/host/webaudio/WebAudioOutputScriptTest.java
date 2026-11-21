package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The real-time <code>AudioContext</code> of a page played to an output (plan item P25-07, FEATURE_MATRIX WAU-08c, WAU-12): what a script builds with an oscillator and a gain reaches the output
 * as the closed-form signal; <code>suspend()</code> makes the output quiet and releases it, <code>resume()</code> opens a new one that goes on at the same time, <code>close()</code> ends the rendering
 * thread and closes the output; the options <code>sampleRate</code>, <code>latencyHint</code> and <code>sinkId</code> and <code>setSinkId</code> choose the output (the sink <code>none</code> plays to no
 * device at all); a machine without a device (an output that cannot be opened) still runs the context, silently; <code>outputLatency</code> and <code>getOutputTimestamp()</code> report the output. The
 * outputs are recording fakes ({@link ManualOutputFactory}), so everything is exact and no sound device is used. The deprecated <code>ScriptProcessorNode</code> is checked through an
 * <code>OfflineAudioContext</code> (exact delay of two blocks, exact values, the handler of a page) and through a real-time context.
 * <p>
 * 페이지의 실시간 <code>AudioContext</code> 를 출력으로 재생(계획 P25-07, FEATURE_MATRIX WAU-08c, WAU-12): 스크립트가 오실레이터와 게인으로 만든 것은 닫힌 식의 신호로 출력에 닿는다.
 * <code>suspend()</code> 는 출력을 조용하게 하고 놓으며 <code>resume()</code> 은 같은 시간에서 이어 가는 새 출력을 연다. <code>close()</code> 는 렌더링 스레드를 끝내고 출력을 닫는다. 옵션
 * <code>sampleRate</code>, <code>latencyHint</code>, <code>sinkId</code> 와 <code>setSinkId</code> 가 출력을 고른다(싱크 <code>none</code> 은 어떤 장치로도 재생하지 않는다). 장치가 없는
 * 컴퓨터(출력을 열 수 없음)에서도 컨텍스트는 소리 없이 돈다. <code>outputLatency</code> 와 <code>getOutputTimestamp()</code> 는 출력을 보고한다. 출력은 기록하는 가짜
 * ({@link ManualOutputFactory})이므로 모든 것이 정확하며 소리 장치는 쓰지 않는다. 폐기 예정인 <code>ScriptProcessorNode</code> 는 <code>OfflineAudioContext</code>(두 블록의 정확한 지연,
 * 정확한 값, 페이지의 핸들러)와 실시간 컨텍스트로 확인한다.
 */
public class WebAudioOutputScriptTest {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private WebAudioPage page;

    /**
     * The recording factory of the outputs.
     * <p>
     * 출력을 기록하는 팩터리.
     */
    private ManualOutputFactory outputs;

    /**
     * The simulated device that plays the outputs at the pace of the wall clock.
     * <p>
     * 출력을 벽시계의 속도로 재생하는 모의 장치.
     */
    private PacedDevice device;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebAudioOutputScriptTest() {
    }

    /**
     * Creates the page with recording outputs that a simulated device plays at the pace of the wall clock.
     * <p>
     * 모의 장치가 벽시계의 속도로 재생하는 기록 출력으로 페이지를 만든다.
     */
    public void setUp() {
        page = new WebAudioPage();
        outputs = new ManualOutputFactory();
        device = new PacedDevice(outputs);
        page.fixture.env.webAudioOutputs = outputs;
        page.run("var log = [];");
    }

    /**
     * Disposes the page, which ends all the contexts, and stops the device.
     * <p>
     * 페이지를 폐기하고(모든 컨텍스트가 끝난다) 장치를 멈춘다.
     */
    public void tearDown() {
        page.fixture.scripts.dispose();
        device.close();
    }

    /**
     * Counts the live threads whose names start with a prefix.
     * <p>
     * 이름이 접두사로 시작하는 살아 있는 스레드를 센다.
     *
     * @param prefix the prefix / 접두사
     * @return the number of threads / 스레드 수
     */
    private static int threads(String prefix) {
        Thread[] all = new Thread[Thread.activeCount() + 32];
        int n = Thread.enumerate(all);
        int count = 0;
        // 살아 있는 스레드 중 이름이 접두사로 시작하는 것을 센다.
        for (int i = 0; i < n; i++) {
            if (all[i] != null && all[i].isAlive() && all[i].getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Waits (running the tasks of the page) until an output holds a number of frames.
     * <p>
     * 출력이 정해진 수의 프레임을 가질 때까지 기다린다(그동안 페이지의 작업을 실행한다).
     *
     * @param out    the output / 출력
     * @param frames the number of frames / 프레임 수
     */
    private void awaitFrames(ManualOutput out, long frames) {
        long end = System.currentTimeMillis() + 15000;
        // 출력에 충분한 프레임이 쓰일 때까지 페이지의 작업을 실행하며 쉰다.
        while (out.getWrittenFrames() < frames) {
            page.pump();
            if (System.currentTimeMillis() > end) {
                Check.fail("only " + out.getWrittenFrames() + " of " + frames + " frames were played");
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Check.fail("interrupted");
            }
        }
    }

    /**
     * Waits for the first output of the factory.
     * <p>
     * 팩터리의 n 번째 출력을 기다린다.
     *
     * @param n the number of the output, from 1 / 출력의 번호(1 부터)
     * @return the output / 출력
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private ManualOutput open(int n) throws InterruptedException {
        ManualOutput o = outputs.waitFor(n, 10000);
        Check.notNull(o, "the context opened the output number " + n);
        return o;
    }

    /**
     * An oscillator through a gain reaches the output as the closed-form sine: 441 Hz with the amplitude 0.5, started at 0.1 s (frame 4800), both channels equal, silence before the start. The
     * output is a stereo output at the sample rate of the context.
     * <p>
     * 게인을 거친 오실레이터는 닫힌 식의 사인으로 출력에 닿는다: 진폭 0.5 의 441 Hz 를 0.1 초(프레임 4800)에 시작하며 두 채널이 같고 시작 전에는 무음이다. 출력은 컨텍스트의 샘플링
     * 주파수의 스테레오 출력이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOscillatorReachesTheOutput() throws Exception {
        page.run("var rt = new AudioContext(); var o = rt.createOscillator(); o.frequency.value = 441; var g = rt.createGain(); g.gain.value = 0.5; o.connect(g); g.connect(rt.destination);"
                + " o.start(0.1);");
        ManualOutput out = open(1);
        Check.equal(48000, out.getSampleRate(), "the output plays at the sample rate of the context");
        Check.equal(2, out.getChannels(), "stereo");
        awaitFrames(out, 4800 + 9600);
        float[] left = out.copyChannel(0);
        float[] right = out.copyChannel(1);
        // 시작 프레임(4800) 전은 정확히 무음이어야 한다.
        for (int k = 0; k < 4800; k++) {
            if (left[k] != 0f || right[k] != 0f) {
                Check.fail("frame " + k + " is not silent before the start: " + left[k]);
            }
        }
        double worst = 0;
        // 시작 뒤의 프레임은 0.5 sin(2 pi 441 n / 48000) 이어야 하고 두 채널이 같아야 한다.
        for (int k = 4800; k < 4800 + 9000; k++) {
            double want = 0.5 * Math.sin(2 * Math.PI * 441 * (k - 4800) / 48000.0);
            worst = Math.max(worst, Math.abs(left[k] - want));
            if (left[k] != right[k]) {
                Check.fail("the channels differ at frame " + k);
            }
        }
        Check.isTrue(worst < 1e-4, "the output is the closed-form sine: the largest error is " + worst);
    }

    /**
     * <code>suspend()</code> makes the output quiet (nothing more is written) and releases it, <code>resume()</code> opens a new output and the signal goes on at the same time, and
     * <code>close()</code> ends the rendering thread and closes the output; the page sees the states and their events in order.
     * <p>
     * <code>suspend()</code> 는 출력을 조용하게 하고(더는 쓰지 않는다) 놓는다. <code>resume()</code> 은 새 출력을 열며 같은 시간에서 신호가 이어진다. <code>close()</code> 는 렌더링 스레드를
     * 끝내고 출력을 닫는다. 페이지는 상태와 이벤트를 순서대로 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSuspendResumeAndCloseControlTheOutput() throws Exception {
        int base = threads("swingwebpanel-audio-render-");
        page.run("var rt = new AudioContext(); var c = rt.createConstantSource(); c.offset.value = 0.25; c.connect(rt.destination); c.start();"
                + " rt.onstatechange = function () { log.push(rt.state); };");
        Check.equal(base + 1, threads("swingwebpanel-audio-render-"), "the context has a rendering thread");
        ManualOutput first = open(1);
        awaitFrames(first, 3000);
        page.run("var susp = false; rt.suspend().then(function () { susp = true; });");
        page.waitFor("susp", 5000, "suspended");
        long deadline = System.currentTimeMillis() + 5000;
        // 렌더링 스레드가 구간을 끝내고 출력을 놓을 때까지 기다린다.
        while (!first.isClosed() && System.currentTimeMillis() < deadline) {
            Thread.sleep(2);
        }
        Check.isTrue(first.isClosed(), "a suspended context does not hold the output");
        long written = first.getWrittenFrames();
        double stopped = page.number("rt.currentTime");
        page.pumpFor(200);
        Check.equal(written, first.getWrittenFrames(), "nothing is played while the context is suspended");
        Check.equal(stopped, page.number("rt.currentTime"), "the time stands still");
        page.run("var res = false; rt.resume().then(function () { res = true; });");
        page.waitFor("res", 5000, "resumed");
        ManualOutput second = open(2);
        awaitFrames(second, 2000);
        float[] again = second.copyChannel(0);
        Check.equal(0.25f, again[0], "the signal is there from the first frame of the new output");
        Check.isTrue(page.number("rt.currentTime") > stopped, "the time goes on from where it stopped");
        page.run("var closed = false; rt.close().then(function () { closed = true; });");
        page.waitFor("closed", 5000, "closed");
        Check.equal(base, threads("swingwebpanel-audio-render-"), "close() ends the rendering thread");
        deadline = System.currentTimeMillis() + 5000;
        // 출력이 닫힐 때까지 기다린다.
        while (!second.isClosed() && System.currentTimeMillis() < deadline) {
            Thread.sleep(2);
        }
        Check.isTrue(second.isClosed(), "and closes the output");
        page.pump();
        Check.equal("[running, suspended, running, closed]", page.run("'[' + log.join(', ') + ']'"), "the states in order");
    }

    /**
     * The options choose the output: the sample rate (the output plays at that rate), the latency hint (the longer the latency, the larger the buffer of the output and the output latency that the
     * context reports once the buffer is full). An output that nobody consumes shows the buffer: it fills and <code>outputLatency</code> is its length in seconds.
     * <p>
     * 옵션이 출력을 고른다: 샘플링 주파수(출력이 그 주파수로 재생한다), 지연 힌트(지연이 길수록 출력의 버퍼가 크고 버퍼가 찼을 때 컨텍스트가 알리는 출력 지연이 크다). 아무도 소비하지 않는
     * 출력은 버퍼를 보여 준다: 채워지며 <code>outputLatency</code> 는 그 길이(초)이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOptionsChooseTheOutput() throws Exception {
        device.setPlaying(false);
        page.run("var fast = new AudioContext({latencyHint: 'interactive'}); var slow = new AudioContext({sampleRate: 22050, latencyHint: 'playback'});");
        ManualOutput a = open(1);
        ManualOutput b = open(2);
        // 두 출력 중 어느 것이 어느 컨텍스트의 것인지는 표본율로 안다.
        ManualOutput interactive = a.getSampleRate() == 48000 ? a : b;
        ManualOutput playback = a.getSampleRate() == 22050 ? a : b;
        Check.equal(48000, interactive.getSampleRate(), "the default sample rate");
        Check.equal(22050, playback.getSampleRate(), "the sample rate option");
        double interactiveSeconds = interactive.getBufferFrames() / 48000.0;
        double playbackSeconds = playback.getBufferFrames() / 22050.0;
        Check.isTrue(interactiveSeconds >= 0.04 && interactiveSeconds <= 0.07, "the buffer of the interactive output is about 50 ms: " + interactiveSeconds);
        Check.isTrue(playbackSeconds > 0.3, "the buffer of the playback output is long: " + playbackSeconds);
        page.waitFor("fast.outputLatency > 0.03", 10000, "the buffer of the interactive output is full");
        page.waitFor("slow.outputLatency > 0.3", 10000, "the buffer of the playback output is full");
        Check.isTrue(page.number("fast.outputLatency") < page.number("slow.outputLatency"), "the output latency follows the latency hint");
        Check.isTrue(page.number("fast.baseLatency") > 0 && page.number("fast.baseLatency") < page.number("slow.baseLatency"), "and so does the base latency");
    }

    /**
     * <code>getOutputTimestamp()</code> reports the time that the output plays: the context time is not after the time that the context has rendered, and it is not far behind it (the output
     * consumes at once here); the performance time is not after <code>performance.now()</code>.
     * <p>
     * <code>getOutputTimestamp()</code> 는 출력이 재생하는 시간을 보고한다: 컨텍스트 시간은 컨텍스트가 렌더링한 시간보다 뒤이고 그리 멀지 않다(여기서 출력은 곧바로 소비한다). 페이지 시계
     * 시간은 <code>performance.now()</code> 보다 앞서지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputTimestamp() throws Exception {
        page.run("var rt = new AudioContext();");
        page.waitFor("rt.currentTime > 0.3", 10000, "the context runs");
        page.run("var t0 = performance.now(); var ts = rt.getOutputTimestamp(); var now = rt.currentTime;");
        double contextTime = page.number("ts.contextTime");
        double now = page.number("now");
        Check.isTrue(contextTime > 0 && contextTime <= now + 1e-9, "the context time is not after the rendered time: " + contextTime + " vs " + now);
        Check.isTrue(contextTime > now - 0.5, "and not far behind: " + contextTime + " vs " + now);
        Check.isTrue(page.number("ts.performanceTime") <= page.number("performance.now()") + 1e-6, "the performance time is not in the future");
    }

    /**
     * The sink: <code>{type: "none"}</code> in the options plays to no device at all (the factory is never asked) while the time runs, <code>setSinkId({type: "none"})</code> of a context that plays
     * releases its output at once and never asks the factory again, <code>setSinkId("")</code> brings the sound back to a new output of the factory, and <code>sinkchange</code> comes after each
     * switch has taken effect.
     * <p>
     * 싱크: 옵션의 <code>{type: "none"}</code> 은 어떤 장치로도 재생하지 않으며(팩터리에 묻지 않는다) 시간은 흐른다. 재생 중인 컨텍스트의 <code>setSinkId({type: "none"})</code> 은 출력을 곧바로
     * 놓고 팩터리에 다시 묻지 않는다. <code>setSinkId("")</code> 은 소리를 팩터리의 새 출력으로 되돌린다. <code>sinkchange</code> 는 바뀐 것이 적용된 뒤에 온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSinkNoneAndBack() throws Exception {
        page.run("var quiet = new AudioContext({sinkId: {type: 'none'}});");
        page.waitFor("quiet.currentTime > 0.2", 10000, "a context without an output keeps its time");
        Check.equal(0, outputs.count(), "the factory was not asked for a context whose sink is none");
        Check.equal("none", page.run("quiet.sinkId.type"), "the sink is reported");
        page.run("quiet.close(); var rt = new AudioContext(); var o = rt.createConstantSource(); o.offset.value = 0.5; o.connect(rt.destination); o.start();"
                + " rt.onsinkchange = function () { log.push('sinkchange:' + (typeof rt.sinkId === 'object' ? rt.sinkId.type : '[' + rt.sinkId + ']')); };");
        ManualOutput first = open(1);
        awaitFrames(first, 2000);
        double before = page.number("rt.currentTime");
        page.run("var d1 = false; rt.setSinkId({type: 'none'}).then(function () { d1 = true; });");
        page.waitFor("d1", 5000, "the sink is none");
        page.pumpFor(50);
        Check.isTrue(first.isClosed(), "the output that played was released at once");
        Check.equal(1, outputs.count(), "and the factory was not asked for another output");
        Check.equal("[sinkchange:none]", "[" + page.run("log.join('|')") + "]", "sinkchange comes once, after the output was released");
        page.waitFor("rt.currentTime > " + (before + 0.2), 10000, "the time runs without an output");
        page.run("log.length = 0; var d2 = false; rt.setSinkId('').then(function () { d2 = true; });");
        page.waitFor("d2", 5000, "back to the default device");
        page.pumpFor(50);
        ManualOutput second = open(2);
        awaitFrames(second, 1000);
        Check.equal(0.5f, second.copyChannel(0)[0], "the signal plays through the new output");
        Check.equal("sinkchange:[]", page.run("log.join('|')"), "sinkchange for the way back");
        Check.equal(2, outputs.count(), "two outputs in all");
    }

    /**
     * A machine without a sound device (an output that cannot be opened) still runs the context: no exception reaches the script, the state is <code>running</code> and the time advances at the pace of
     * the wall clock.
     * <p>
     * 소리 장치가 없는 컴퓨터(출력을 열 수 없음)에서도 컨텍스트는 돈다: 스크립트에 예외가 닿지 않고, 상태는 <code>running</code> 이며, 시간은 벽시계의 속도로 흐른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoDeviceStillRuns() throws Exception {
        outputs.setFailure("no sound device");
        long t0 = System.nanoTime();
        page.run("var rt = new AudioContext(); var o = rt.createOscillator(); o.connect(rt.destination); o.start();");
        page.waitFor("rt.state === 'running'", 5000, "the context runs");
        page.waitFor("rt.currentTime > 0.4", 10000, "the time advances without a device");
        double wall = (System.nanoTime() - t0) / 1e9;
        // 컨텍스트는 벽시계보다 버퍼 하나(약 50 ms) 정도만 앞설 수 있다.
        Check.isTrue(page.number("rt.currentTime") < wall + 0.15, "and not faster than the wall clock (plus the buffer that is rendered ahead): " + page.number("rt.currentTime") + " vs " + wall);
        Check.equal(0, outputs.count(), "no output was opened");
        Check.equal("no error", page.errorOf("rt.suspend(); rt.resume(); rt.close()"), "the controls work");
    }

    /**
     * <code>createScriptProcessor</code> and the rules of <code>ScriptProcessorNode</code> (deprecated, 1.29): the arguments and their errors, the attributes, the interface that scripts cannot
     * construct, a channel count that cannot be changed, and <code>onaudioprocess</code> as a property and as a listener.
     * <p>
     * <code>createScriptProcessor</code> 와 <code>ScriptProcessorNode</code>(폐기 예정, 1.29)의 규칙: 인수와 오류, 속성, 스크립트가 생성할 수 없는 인터페이스, 바꿀 수 없는 채널 수,
     * 속성과 리스너로서의 <code>onaudioprocess</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptProcessorInterface() throws Exception {
        page.run("var oc = new OfflineAudioContext(1, 1024, 8000);");
        Check.equal("true:2048:2:2", page.run("var d = oc.createScriptProcessor(); [d instanceof ScriptProcessorNode && d instanceof AudioNode, d.bufferSize, d.numberOfInputs + 1, d.numberOfOutputs + 1].join(':')"),
                "the defaults: 2048 frames, one input and one output");
        Check.equal("256:512:16384", page.run("[oc.createScriptProcessor(256).bufferSize, oc.createScriptProcessor(512, 1, 1).bufferSize, oc.createScriptProcessor(16384, 0, 2).bufferSize].join(':')"),
                "the allowed buffer sizes");
        Check.equal("IndexSizeError", page.errorOf("oc.createScriptProcessor(100)"), "too small");
        Check.equal("IndexSizeError", page.errorOf("oc.createScriptProcessor(3000)"), "not a power of two");
        Check.equal("IndexSizeError", page.errorOf("oc.createScriptProcessor(32768)"), "too large");
        Check.equal("IndexSizeError", page.errorOf("oc.createScriptProcessor(256, 0, 0)"), "both numbers of channels zero");
        Check.equal("IndexSizeError", page.errorOf("oc.createScriptProcessor(256, 33, 1)"), "too many input channels");
        Check.equal("no error", page.errorOf("oc.createScriptProcessor(256, 0, 32)"), "32 output channels");
        Check.equal("TypeError", page.errorOf("new ScriptProcessorNode()"), "scripts cannot construct the interface");
        Check.equal("[object ScriptProcessorNode]", page.run("Object.prototype.toString.call(oc.createScriptProcessor())"), "the tag");
        page.run("var sp = oc.createScriptProcessor(256, 2, 2); var hits = 0;");
        Check.equal("InvalidStateError", page.errorOf("sp.channelCount = 1"), "the channel count cannot be changed");
        Check.equal("InvalidStateError", page.errorOf("sp.channelCountMode = 'max'"), "nor the mode");
        Check.equal("no error", page.errorOf("sp.channelInterpretation = 'discrete'"), "the interpretation can");
        Check.equal("2", page.run("String(sp.channelCount)"), "the channel count is the number of input channels");
        Check.equal("null", page.run("String(sp.onaudioprocess)"), "no handler at first");
        page.run("var f = function () {}; sp.onaudioprocess = f;");
        Check.equal("true", page.run("String(sp.onaudioprocess === f)"), "the handler property");
        Check.equal("true", page.run("String('onaudioprocess' in ScriptProcessorNode.prototype && 'bufferSize' in ScriptProcessorNode.prototype)"), "the members are on the prototype");
    }

    /**
     * A script processor through an offline context: the audio is delayed by exactly two blocks (an impulse at frame 100 of a stream comes out at frame 100 + 2 * 256), the values that the handler
     * writes are exact (the processing doubles every sample), the event carries <code>playbackTime</code> as the time at which the output starts to play, the input buffer has the input channels and the
     * output buffer is silent at first, the buffers that a handler keeps are independent of the engine afterwards, and a handler that throws costs one block of silence at most.
     * <p>
     * 오프라인 컨텍스트를 거치는 스크립트 프로세서: 오디오는 정확히 두 블록 지연된다(스트림의 프레임 100 의 임펄스는 프레임 100 + 2 * 256 에서 나온다). 핸들러가 쓰는 값은 정확하다(처리가 모든
     * 표본을 두 배로 한다). 이벤트는 <code>playbackTime</code> 을 출력이 재생되기 시작하는 시간으로 싣고, 입력 버퍼는 입력 채널을 가지며 출력 버퍼는 처음에 무음이다. 핸들러가 간직한 버퍼는
     * 이후 엔진에서 독립이며, 던지는 핸들러는 많아야 한 블록의 무음을 치른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptProcessorThroughAnOfflineContext() throws Exception {
        page.run("var oc = new OfflineAudioContext(1, 2048, 8000); var impulse = oc.createBuffer(1, 2048, 8000); var data = impulse.getChannelData(0); data[100] = 1; data[300] = -0.5; data[1000] = 0.25;"
                + " var src = oc.createBufferSource(); src.buffer = impulse; var sp = oc.createScriptProcessor(256, 1, 1); var events = []; var kept = null; var silentOutputAtFirst = true;"
                + " sp.onaudioprocess = function (e) { var inp = e.inputBuffer.getChannelData(0), out = e.outputBuffer.getChannelData(0);"
                + " for (var i = 0; i < out.length; i++) { if (out[i] !== 0) { silentOutputAtFirst = false; } out[i] = inp[i] * 2; }"
                + " events.push(e.playbackTime.toFixed(6) + ':' + e.inputBuffer.numberOfChannels + ':' + e.outputBuffer.length + ':' + (e.type === 'audioprocess')); if (!kept) { kept = e.outputBuffer; } };"
                + " src.connect(sp); sp.connect(oc.destination); src.start(); var rendered = null; oc.startRendering().then(function (b) { rendered = b; });");
        page.waitFor("rendered !== null", 20000, "the rendering is complete");
        double[] values = new double[2048];
        // 렌더링 결과 표본을 읽는다.
        for (int i = 0; i < 2048; i++) {
            values[i] = page.number("rendered.getChannelData(0)[" + i + "]");
        }
        // 정확히 두 블록(512 프레임) 늦은 위치에서만 임펄스의 두 배 값이 나와야 한다.
        for (int i = 0; i < 2048; i++) {
            double want = i == 100 + 512 ? 2.0 : (i == 300 + 512 ? -1.0 : (i == 1000 + 512 ? 0.5 : 0.0));
            if (values[i] != want) {
                Check.fail("frame " + i + ": " + values[i] + " instead of " + want);
            }
        }
        Check.equal("true", page.run("String(silentOutputAtFirst)"), "the output buffer is silent when the event starts");
        Check.equal("8", page.run("String(events.length)"), "one event per block of 256 frames");
        Check.equal("0.064000:1:256:true", page.run("events[0]"), "playbackTime of the first block is (0 + 2 * 256) / 8000");
        Check.equal("0.096000:1:256:true", page.run("events[1]"), "and of the second block (256 + 2 * 256) / 8000");
        Check.equal("true", page.run("String(kept.length === 256 && kept.numberOfChannels === 1)"), "the output buffer of the first event is kept");
        // 간직한 첫 번째 출력 버퍼는 핸들러가 쓴 값(입력의 두 배, 이 블록에서는 프레임 100 의 임펄스 2)을 그대로 가져야 한다(엔진이 블록을 재활용해도 바뀌지 않는다).
        Check.equal("2", page.run("String(kept.getChannelData(0)[100])"), "a buffer that the handler keeps still shows what it wrote");
        // 던지는 핸들러가 있는 컨텍스트도 렌더링을 끝낸다.
        page.run("var oc2 = new OfflineAudioContext(1, 1024, 8000); var c2 = oc2.createConstantSource(); c2.offset.value = 0.25; var sp2 = oc2.createScriptProcessor(256, 1, 1); var calls = 0;"
                + " sp2.onaudioprocess = function (e) { calls++; throw new Error('boom'); }; c2.connect(sp2); sp2.connect(oc2.destination); c2.start(); var done2 = null; oc2.startRendering().then(function (b) { done2 = b; });");
        page.waitFor("done2 !== null", 20000, "a throwing handler does not stop the rendering");
        Check.equal("0", page.run("String(done2.getChannelData(0)[1023])"), "a handler that throws leaves the output silent");
        Check.equal("4", page.run("String(calls)"), "and it is called for every block");
    }

    /**
     * A script processor without input channels is a generator and without a handler it is silent: a handler that writes a counter into a one channel processor with no input comes out of an
     * offline context as that counter, two blocks late; without a handler (and with a listener that is added later) the node makes no events and no sound; <code>inputBuffer</code> of a node without
     * input channels is null.
     * <p>
     * 입력 채널이 없는 스크립트 프로세서는 생성기이고 핸들러가 없으면 무음이다: 입력이 없는 1 채널 프로세서의 핸들러가 카운터를 쓰면 오프라인 컨텍스트에서 두 블록 늦은 그 카운터가 나온다.
     * 핸들러가 없으면(그리고 나중에 리스너를 더하면) 노드는 이벤트도 소리도 만들지 않는다. 입력 채널이 없는 노드의 <code>inputBuffer</code> 는 null 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptProcessorAsAGeneratorAndWithoutAHandler() throws Exception {
        page.run("var oc = new OfflineAudioContext(1, 1536, 8000); var gen = oc.createScriptProcessor(256, 0, 1); var n = 0; var inputIsNull = true; var calls = 0;"
                + " gen.addEventListener('audioprocess', function (e) { calls++; if (e.inputBuffer !== null) { inputIsNull = false; } var out = e.outputBuffer.getChannelData(0);"
                + " for (var i = 0; i < out.length; i++) { out[i] = ++n; } }); gen.connect(oc.destination); var done = null; oc.startRendering().then(function (b) { done = b; });");
        page.waitFor("done !== null", 20000, "rendered");
        Check.equal("true", page.run("String(inputIsNull)"), "the input buffer of a node without input channels is null");
        // 처음 두 블록(512 프레임)은 무음이고 그 뒤로는 1, 2, 3 ... 이 이어져야 한다.
        for (int i = 0; i < 1536; i++) {
            double want = i < 512 ? 0.0 : i - 511;
            double got = page.number("done.getChannelData(0)[" + i + "]");
            if (got != want) {
                Check.fail("frame " + i + ": " + got + " instead of " + want);
            }
        }
        Check.equal("6", page.run("String(calls)"), "one event per block");
        page.run("var oc2 = new OfflineAudioContext(1, 1024, 8000); var c = oc2.createConstantSource(); c.offset.value = 1; var quiet = oc2.createScriptProcessor(256, 1, 1); c.connect(quiet); quiet.connect(oc2.destination); c.start();"
                + " var done2 = null; oc2.startRendering().then(function (b) { done2 = b; });");
        page.waitFor("done2 !== null", 20000, "rendered without a handler");
        Check.equal("0", page.run("var m = 0, d = done2.getChannelData(0); for (var i = 0; i < d.length; i++) { m = Math.max(m, Math.abs(d[i])); } String(m)"), "without a handler the node is silent");
    }

    /**
     * A script processor of a real-time context: a generator whose handler writes a counter reaches the output as that counter, two blocks late, without a gap (the blocks of 4096 frames give the
     * handler more than 80 ms to answer; the page thread answers at once), and the end of the context stops the events.
     * <p>
     * 실시간 컨텍스트의 스크립트 프로세서: 핸들러가 카운터를 쓰는 생성기는 두 블록 늦게 틈 없이 그 카운터로 출력에 닿는다(4096 프레임의 블록은 핸들러에게 80 ms 이상의 응답 시간을
     * 준다. 페이지 스레드는 곧바로 응답한다). 컨텍스트가 끝나면 이벤트가 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptProcessorInARealTimeContext() throws Exception {
        page.run("var rt = new AudioContext(); var gen = rt.createScriptProcessor(4096, 0, 1); var n = 0; var calls = 0;"
                + " gen.onaudioprocess = function (e) { calls++; var out = e.outputBuffer.getChannelData(0); for (var i = 0; i < out.length; i++) { out[i] = ++n / 1000000; } };"
                + " gen.connect(rt.destination);");
        ManualOutput out = open(1);
        awaitFrames(out, 4096 * 6);
        float[] l = out.copyChannel(0);
        int start = 0;
        // 첫 번째 0 이 아닌 표본(생성기의 첫 값)을 찾는다.
        while (start < l.length && l[start] == 0f) {
            start++;
        }
        Check.isTrue(start >= 4096 * 2 - 128 && start <= 4096 * 2 + 4096, "the first sample comes two blocks late: " + start);
        // 첫 표본부터 끝까지 카운터가 틈 없이 이어져야 한다.
        int checked = 0;
        for (int i = start; i < l.length && i < start + 4096 * 3; i++) {
            float want = (i - start + 1) / 1000000f;
            if (l[i] != want) {
                Check.fail("frame " + i + " of the output (" + (i - start) + " after the first): " + l[i] + " instead of " + want);
            }
            checked++;
        }
        Check.isTrue(checked >= 4096 * 2, "three blocks were checked without a gap: " + checked);
        page.run("rt.close();");
        page.pumpFor(100);
        double calls = page.number("calls");
        page.pumpFor(200);
        Check.equal(calls, page.number("calls"), "a closed context makes no events");
    }
}
