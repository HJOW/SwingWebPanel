package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the delay node (Web Audio API 1.18, plan item P25-04): the exact delay of a whole number of frames, the linear interpolation of
 * a fractional delay and of a delay time that changes (a ramp input is interpolated exactly, so the expected values are plain
 * formulas), the range of <code>maxDelayTime</code>, the tail time that keeps the delay running after the source has ended, the mixing
 * of the delay line when the number of channels of the input changes, and the clearing of the line after the tail time.
 * <p>
 * 지연 노드 시험(Web Audio API 1.18, 계획 P25-04): 정수 프레임 지연의 정확성, 소수 지연과 바뀌는 지연 시간의 선형 보간(램프 입력은 정확히 보간되므로 기대값은
 * 단순한 식이다), <code>maxDelayTime</code> 의 범위, 원천이 끝난 뒤에도 지연을 계속 돌리는 꼬리 시간, 입력 채널 수가 바뀔 때 지연선의 믹스, 꼬리 시간 뒤의
 * 지연선 지우기.
 */
public class DelayNodeTest {

    /**
     * The sample rate, a power of two so that delays of whole frames are exact floats of seconds.
     * <p>
     * 샘플링 주파수. 2의 거듭제곱이라 정수 프레임 지연이 초 단위 float 로 정확히 표현된다.
     */
    private static final float RATE = 8192f;

    /**
     * The tolerance for the ramp values (floats against doubles).
     * <p>
     * 램프 값의 허용 오차(float 와 double).
     */
    private static final double TOL = 1e-6;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public DelayNodeTest() {
    }

    /**
     * Builds source (ramp buffer) to delay to destination in a context and returns the delay.
     * <p>
     * 원천(램프 버퍼)에서 지연을 지나 목적지로 가는 그래프를 컨텍스트에 만들고 지연을 반환한다.
     *
     * @param ctx    the context / 컨텍스트
     * @param length the length of the ramp buffer / 램프 버퍼 길이
     * @param max    the maximum delay in seconds / 가장 긴 지연(초)
     * @return the delay node / 지연 노드
     */
    private static DelayNode rampThroughDelay(OfflineAudioContext ctx, int length, double max) {
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(1, length, RATE));
        DelayNode delay = ctx.createDelay(max);
        src.connect(delay);
        delay.connect(ctx.getDestination());
        src.start(0);
        return delay;
    }

    /**
     * A delay of 100 frames outputs the input 100 frames later and silence before.
     * <p>
     * 100프레임 지연은 입력을 100프레임 뒤에 출력하고 그 전에는 무음이다.
     */
    public void testWholeFrameDelay() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 1024, RATE);
        DelayNode d = rampThroughDelay(ctx, 700, 1.0);
        d.getDelayTime().setValue(100f / RATE);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSilent("before the delay", out, 0, 100);
        WebAudioTestKit.assertSignal("delayed ramp", out, 100, 800, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return (i - 100) / 1000.0;
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the input", out, 800, 1024);
    }

    /**
     * A delay of 0 passes the signal through, and a delay of one quantum or more than a quantum works across quanta.
     * <p>
     * 지연 0 은 신호를 그대로 통과시키고, 한 quantum 이상의 지연은 quantum 을 가로질러 동작한다.
     */
    public void testZeroAndLongDelay() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 512, RATE);
        rampThroughDelay(ctx, 300, 1.0);
        float[] out = WebAudioTestKit.render(ctx);
        WebAudioTestKit.assertSignal("no delay", out, 0, 300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return i / 1000.0;
            }
        }, TOL);
        OfflineAudioContext c2 = new OfflineAudioContext(1, 2048, RATE);
        DelayNode d = rampThroughDelay(c2, 300, 1.0);
        d.getDelayTime().setValue(1000f / RATE);
        float[] late = WebAudioTestKit.render(c2);
        WebAudioTestKit.assertSilent("before a delay of 1000 frames", late, 0, 1000);
        WebAudioTestKit.assertSignal("after the source has ended", late, 1000, 1300, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return (i - 1000) / 1000.0;
            }
        }, TOL);
        WebAudioTestKit.assertSilent("after the delayed signal", late, 1300, 2048);
    }

    /**
     * A fractional delay interpolates linearly between the two neighbouring samples: 100.5 frames.
     * <p>
     * 소수 지연은 이웃한 두 샘플 사이를 선형 보간한다: 100.5프레임.
     */
    public void testFractionalDelay() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 1024, RATE);
        DelayNode d = rampThroughDelay(ctx, 700, 1.0);
        d.getDelayTime().setValue(100.5f / RATE);
        float[] out = WebAudioTestKit.render(ctx);
        // 램프는 선형 보간으로 정확히 보간되므로 i - 100.5 위치의 값과 같다(입력이 시작하는 경계 프레임 제외).
        WebAudioTestKit.assertSignal("interpolated ramp", out, 102, 800, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return (i - 100.5) / 1000.0;
            }
        }, TOL);
    }

    /**
     * The delay time is a-rate: a ramp of the delay time from 0 to 64 frames over 512 frames gives the delay <code>i / 8</code> at frame
     * <code>i</code>.
     * <p>
     * 지연 시간은 a-rate 이다: 512프레임에 걸쳐 0 에서 64프레임까지 오르는 지연 시간은 프레임 <code>i</code> 에서 지연 <code>i / 8</code> 을 준다.
     */
    public void testDelayTimeAutomation() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 640, RATE);
        DelayNode d = rampThroughDelay(ctx, 640, 1.0);
        d.getDelayTime().setValueAtTime(0f, 0);
        d.getDelayTime().linearRampToValueAtTime(64f / RATE, 512 / (double) RATE);
        float[] out = WebAudioTestKit.render(ctx);
        // 프레임 i 의 지연은 i/8 프레임이므로 출력은 램프의 i - i/8 위치 값이다.
        WebAudioTestKit.assertSignal("moving delay", out, 0, 512, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return (i - i / 8.0) / 1000.0;
            }
        }, TOL);
    }

    /**
     * The maximum delay must be greater than 0 and less than 3 minutes; <code>delayTime</code> is limited to the maximum.
     * <p>
     * 가장 긴 지연은 0 보다 크고 3분 미만이어야 한다. <code>delayTime</code> 은 최댓값으로 제한된다.
     */
    public void testMaxDelayTime() {
        final OfflineAudioContext ctx = WebAudioTestKit.mono(128);
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createDelay(0);
            }
        }, "max 0");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createDelay(-1);
            }
        }, "max negative");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createDelay(180);
            }
        }, "max 3 minutes");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                ctx.createDelay(Double.NaN);
            }
        }, "max NaN");
        DelayNode d = ctx.createDelay(179.5);
        Check.near(179.5, d.getMaxDelayTime(), 0, "max");
        Check.near(179.5, d.getDelayTime().getMaxValue(), 1e-4, "parameter maximum");
        Check.near(0.0, d.getDelayTime().getMinValue(), 0, "parameter minimum");
        Check.near(0.0, d.getDelayTime().getDefaultValue(), 0, "default delay");
        Check.near(1.0, ctx.createDelay().getMaxDelayTime(), 0, "default maximum");
        OfflineAudioContext c2 = new OfflineAudioContext(1, 1024, RATE);
        DelayNode small = rampThroughDelay(c2, 700, 100.0 / RATE);
        small.getDelayTime().setValue(5f);
        float[] out = WebAudioTestKit.render(c2);
        // 지연 시간 5 초는 가장 긴 지연 100 프레임으로 제한된다.
        WebAudioTestKit.assertSignal("limited to the maximum", out, 100, 700, new WebAudioTestKit.Signal() {
            public double at(int i) {
                return (i - 100) / 1000.0;
            }
        }, TOL);
    }

    /**
     * When the number of channels of the input changes, the samples that are in the delay line are mixed to the new layout: a mono
     * impulse that is in the line is output on both channels after a stereo signal has joined the input.
     * <p>
     * 입력의 채널 수가 바뀌면 지연선에 있는 샘플은 새 배치로 섞인다: 지연선에 있는 모노 임펄스는 스테레오 신호가 입력에 더해진 뒤 두 채널로 출력된다.
     */
    public void testChannelCountChange() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 1024, RATE);
        AudioBuffer impulse = new AudioBuffer(1, 1, RATE);
        impulse.getChannelData(0)[0] = 1f;
        AudioBufferSourceNode a = ctx.createBufferSource();
        a.setBuffer(impulse);
        DelayNode d = ctx.createDelay(1.0);
        d.getDelayTime().setValue(300f / RATE);
        a.connect(d);
        d.connect(ctx.getDestination());
        a.start(0);
        ctx.renderNextQuantum();
        AudioBuffer stereo = new AudioBuffer(2, 700, RATE);
        java.util.Arrays.fill(stereo.getChannelData(0), 0.5f);
        java.util.Arrays.fill(stereo.getChannelData(1), -0.25f);
        AudioBufferSourceNode b = ctx.createBufferSource();
        b.setBuffer(stereo);
        b.connect(d);
        b.start(0);
        // 렌더링이 끝날 때까지 quantum 을 하나씩 렌더링한다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        AudioBuffer out = ctx.finishRendering();
        Check.near(1.0, out.getChannelData(0)[300], 1e-6, "the mono impulse reaches the left channel");
        Check.near(1.0, out.getChannelData(1)[300], 1e-6, "and the right channel");
        Check.near(0.5, out.getChannelData(0)[500], 1e-6, "the stereo signal left, delayed by 300 frames after it starts at 128");
        Check.near(-0.25, out.getChannelData(1)[500], 1e-6, "the stereo signal right");
        Check.near(0.0, out.getChannelData(0)[400], 1e-6, "silence between the impulse and the stereo signal");
    }

    /**
     * After the tail time the delay line is cleared, so a source that starts much later is delayed once and the old content does not
     * come back.
     * <p>
     * 꼬리 시간이 지나면 지연선이 지워지므로 한참 뒤에 시작하는 원천은 한 번만 지연되고 이전 내용이 되돌아오지 않는다.
     */
    public void testLineIsClearedAfterTheTail() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 4096, RATE);
        AudioBuffer impulse = new AudioBuffer(1, 1, RATE);
        impulse.getChannelData(0)[0] = 1f;
        DelayNode d = ctx.createDelay(100.0 / RATE);
        d.getDelayTime().setValue(50f / RATE);
        d.connect(ctx.getDestination());
        AudioBufferSourceNode first = ctx.createBufferSource();
        first.setBuffer(impulse);
        first.connect(d);
        first.start(0);
        AudioBufferSourceNode second = ctx.createBufferSource();
        second.setBuffer(impulse);
        second.connect(d);
        second.start(3000 / (double) RATE);
        float[] out = WebAudioTestKit.render(ctx);
        // 임펄스 하나마다 50 프레임 뒤에 정확히 하나의 에코만 있다.
        for (int i = 0; i < 4096; i++) {
            double expected = (i == 50 || i == 3050) ? 1.0 : 0.0;
            Check.near(expected, out[i], 1e-6, "frame " + i);
        }
        Check.equal(0, ctx.getPinnedNodeCount(), "nothing is pinned any more");
    }

    /**
     * The node has one input and one output, its channel attributes have the defaults of the specification, and its output follows
     * the channels of the input.
     * <p>
     * 노드는 입력과 출력이 하나이고 채널 속성은 규격의 기본 값을 가지며 출력은 입력의 채널을 따른다.
     */
    public void testStructureAndStereo() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 512, RATE);
        DelayNode d = ctx.createDelay(1.0);
        Check.equal(1, d.getNumberOfInputs(), "inputs");
        Check.equal(1, d.getNumberOfOutputs(), "outputs");
        Check.equal(2, d.getChannelCount(), "channelCount");
        Check.equal(ChannelCountMode.MAX, d.getChannelCountMode(), "mode");
        Check.equal(ChannelInterpretation.SPEAKERS, d.getChannelInterpretation(), "interpretation");
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(WebAudioTestKit.ramp(2, 200, RATE));
        src.connect(d);
        d.getDelayTime().setValue(100f / RATE);
        d.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(0.05, out.getChannelData(0)[150], TOL, "left");
        Check.near(-0.05, out.getChannelData(1)[150], TOL, "right");
    }
}
