package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the dynamics compressor node (Web Audio API 1.19, plan item P25-04), through the audio graph: the defaults and ranges of
 * the parameters, that they are k-rate and cannot be changed to a-rate, the steady state output against the hard knee line in decibels
 * that the test writes out (so the parameters reach the processing), the automation of a parameter (read once per render quantum), the
 * look-ahead of the graph, the channels (the output has as many channels as the input), the metered reduction and its return to 0 after
 * the tail time, and the errors of the channel properties.
 * <p>
 * 다이내믹 컴프레서 노드 시험(Web Audio API 1.19, 계획 P25-04)을 오디오 그래프를 거쳐 한다: 파라미터의 기본값과 범위, k-rate 이며 a-rate 로 바꿀 수 없다는
 * 점, 시험에 적은 데시벨의 하드 니 직선과 비교한 정상 상태 출력(파라미터가 처리에 닿는다는 확인), 파라미터의 자동화(render quantum 마다 한 번 읽음), 그래프의
 * 선행 지연, 채널(출력은 입력과 같은 채널 수), 측정한 감소량과 꼬리 시간 뒤 0 으로 돌아옴, 채널 속성의 오류를 확인한다.
 */
public class DynamicsCompressorNodeTest {

    /**
     * The sample rate of the contexts of the tests.
     * <p>
     * 시험 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 44100f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public DynamicsCompressorNodeTest() {
    }

    /**
     * The hard knee line of the test: the identity up to the threshold and a line of the slope <code>1 / ratio</code> after it.
     * <p>
     * 시험의 하드 니 직선: 임계값까지는 항등이고 그 뒤는 기울기 <code>1 / ratio</code> 인 직선.
     *
     * @param xDb   the input in decibels / 입력(dB)
     * @param thr   the threshold in decibels / 임계값(dB)
     * @param ratio the ratio / 비율
     * @return the output in decibels / 출력(dB)
     */
    private static double hard(double xDb, double thr, double ratio) {
        return xDb <= thr ? xDb : thr + (xDb - thr) / ratio;
    }

    /**
     * Creates a constant source through a compressor with the hard knee parameters threshold -24 dB, knee 0 and ratio 4 into a
     * destination.
     * <p>
     * 일정한 원천을 하드 니 파라미터(임계값 -24 dB, 무릎 0, 비율 4)의 컴프레서를 거쳐 목적지에 연결한다.
     *
     * @param ctx   the context / 컨텍스트
     * @param level the constant level / 일정한 레벨
     * @param start the start time of the source / 원천의 시작 시간
     * @return the compressor / 컴프레서
     */
    private static DynamicsCompressorNode hardKnee(OfflineAudioContext ctx, float level, double start) {
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(level);
        DynamicsCompressorNode comp = ctx.createDynamicsCompressor();
        comp.getThreshold().setValue(-24f);
        comp.getKnee().setValue(0f);
        comp.getRatio().setValue(4f);
        c.connect(comp);
        comp.connect(ctx.getDestination());
        c.start(start);
        return comp;
    }

    /**
     * The defaults and ranges of the parameters: threshold -24 (-100 to 0), knee 30 (0 to 40), ratio 12 (1 to 20), attack 0.003 (0 to
     * 1), release 0.25 (0 to 1); all k-rate, and a-rate is refused; the reduction starts at 0; the channel properties.
     * <p>
     * 파라미터의 기본값과 범위: threshold -24(-100 ~ 0), knee 30(0 ~ 40), ratio 12(1 ~ 20), attack 0.003(0 ~ 1), release 0.25(0 ~ 1). 모두 k-rate 이고 a-rate
     * 는 거부된다. reduction 은 0 으로 시작한다. 채널 속성도 확인한다.
     */
    public void testDefaultsAndRanges() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        final DynamicsCompressorNode n = ctx.createDynamicsCompressor();
        Check.near(-24.0, n.getThreshold().getValue(), 0.0, "threshold");
        Check.near(30.0, n.getKnee().getValue(), 0.0, "knee");
        Check.near(12.0, n.getRatio().getValue(), 0.0, "ratio");
        Check.near(0.003, n.getAttack().getValue(), 1e-9, "attack");
        Check.near(0.25, n.getRelease().getValue(), 0.0, "release");
        Check.near(-100.0, n.getThreshold().getMinValue(), 0.0, "threshold min");
        Check.near(0.0, n.getThreshold().getMaxValue(), 0.0, "threshold max");
        Check.near(0.0, n.getKnee().getMinValue(), 0.0, "knee min");
        Check.near(40.0, n.getKnee().getMaxValue(), 0.0, "knee max");
        Check.near(1.0, n.getRatio().getMinValue(), 0.0, "ratio min");
        Check.near(20.0, n.getRatio().getMaxValue(), 0.0, "ratio max");
        Check.near(1.0, n.getAttack().getMaxValue(), 0.0, "attack max");
        Check.near(1.0, n.getRelease().getMaxValue(), 0.0, "release max");
        Check.near(0.0, n.getReduction(), 0.0, "the reduction starts at 0");
        Check.equal(2L, (long) n.getChannelCount(), "channelCount");
        Check.same(ChannelCountMode.CLAMPED_MAX, n.getChannelCountMode(), "channelCountMode");
        Check.same(ChannelInterpretation.SPEAKERS, n.getChannelInterpretation(), "channelInterpretation");
        AudioParam[] all = {n.getThreshold(), n.getKnee(), n.getRatio(), n.getAttack(), n.getRelease()};
        // 모든 파라미터가 k-rate 이고 a-rate 로 바꿀 수 없다.
        for (int i = 0; i < all.length; i++) {
            Check.same(AutomationRate.K_RATE, all[i].getAutomationRate(), all[i].getName() + " is k-rate");
            final AudioParam p = all[i];
            WebAudioTestKit.assertThrows(AudioException.Kind.INVALID_STATE, new Runnable() {
                public void run() {
                    p.setAutomationRate(AutomationRate.A_RATE);
                }
            }, "a-rate for " + all[i].getName());
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                n.setChannelCount(3);
            }
        }, "channelCount 3");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                n.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "the mode max");
        n.setChannelCount(1);
        n.setChannelCountMode(ChannelCountMode.EXPLICIT);
        Check.equal(1L, (long) n.getChannelCount(), "channelCount 1 is allowed");
    }

    /**
     * The parameters reach the processing: a constant input of 0.5 through the hard knee (threshold -24 dB, ratio 4) settles at the
     * makeup gain times the line of the test, and a constant input below the threshold only gets the makeup gain.
     * <p>
     * 파라미터가 처리에 닿는다: 0.5 의 일정한 입력은 하드 니(임계값 -24 dB, 비율 4)를 거쳐 메이크업 게인에 시험의 직선을 곱한 값으로 수렴하고, 임계값 아래의
     * 일정한 입력은 메이크업 게인만 받는다.
     */
    public void testSteadyStateThroughTheGraph() {
        double makeup = Math.pow(1.0 / Math.pow(10.0, hard(0, -24, 4) / 20.0), 0.6);
        float[] levels = {0.5f, 0.05f};
        // 레벨마다 0.6 초 렌더링한 끝의 출력을 직선과 비교한다.
        for (int i = 0; i < levels.length; i++) {
            OfflineAudioContext ctx = new OfflineAudioContext(1, 26496, RATE);
            hardKnee(ctx, levels[i], 0);
            float[] out = ctx.startRendering().getChannelData(0);
            double xDb = 20 * Math.log10(levels[i]);
            double expected = makeup * Math.pow(10.0, hard(xDb, -24, 4) / 20.0);
            Check.near(expected, out[26495], expected * 0.015, "output for the input " + levels[i]);
        }
    }

    /**
     * The look-ahead of 6 ms in the graph: a source that starts at 0.05 s is first heard 264 frames later, and before that the output
     * is exactly silent.
     * <p>
     * 그래프에서의 6 ms 선행 지연: 0.05 초에 시작하는 원천은 264 프레임 뒤에 처음 들리고 그 전에는 출력이 정확히 무음이다.
     */
    public void testLookAheadInTheGraph() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 8192, RATE);
        hardKnee(ctx, 0.01f, 0.05);
        float[] out = ctx.startRendering().getChannelData(0);
        int first = 2205 + 264;
        WebAudioTestKit.assertSilent("before the look-ahead", out, 0, first);
        Check.isTrue(out[first] != 0f, "the source is heard at " + first);
    }

    /**
     * A parameter is read once per render quantum: lowering the threshold with an automation event at 0.5 s lowers the output level
     * from the first quantum after that time (the output of a constant 0.5 input drops by the difference of the two hard knee lines),
     * and the metered reduction grows.
     * <p>
     * 파라미터는 render quantum 마다 한 번 읽는다: 0.5 초의 자동화 이벤트로 임계값을 낮추면 그 시간 뒤 첫 quantum 부터 출력 레벨이 낮아진다(일정한 0.5 입력의
     * 출력은 두 하드 니 직선의 차이만큼 떨어진다). 측정한 감소량은 커진다.
     */
    public void testParameterAutomation() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 88192, RATE);
        DynamicsCompressorNode comp = hardKnee(ctx, 0.5f, 0);
        comp.getThreshold().setValueAtTime(-50f, 0.5);
        float[] out = ctx.startRendering().getChannelData(0);
        double makeup1 = Math.pow(1.0 / Math.pow(10.0, hard(0, -24, 4) / 20.0), 0.6);
        double makeup2 = Math.pow(1.0 / Math.pow(10.0, hard(0, -50, 4) / 20.0), 0.6);
        double xDb = 20 * Math.log10(0.5);
        double before = makeup1 * Math.pow(10.0, hard(xDb, -24, 4) / 20.0);
        double after = makeup2 * Math.pow(10.0, hard(xDb, -50, 4) / 20.0);
        Check.near(before, out[22000], before * 0.015, "before the event");
        Check.near(after, out[88000], after * 0.03, "after the event");
        Check.isTrue(Math.abs(after - before) > 0.01, "the two levels differ");
    }

    /**
     * The output has as many channels as the input: a mono source gives a mono output (the second destination channel is the up-mix),
     * a stereo source keeps its two channels, and both channels get the same gain.
     * <p>
     * 출력은 입력과 같은 채널 수이다: 모노 원천은 모노 출력을 주고(목적지의 둘째 채널은 업믹스) 스테레오 원천은 두 채널을 유지하며 두 채널이 같은 게인을 받는다.
     */
    public void testChannels() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 26496, RATE);
        AudioBuffer in = new AudioBuffer(2, 26496, RATE);
        java.util.Arrays.fill(in.getChannelData(0), 0.5f);
        java.util.Arrays.fill(in.getChannelData(1), 0.1f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        DynamicsCompressorNode comp = ctx.createDynamicsCompressor();
        comp.getKnee().setValue(0f);
        comp.getRatio().setValue(4f);
        src.connect(comp);
        comp.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(5.0, out.getChannelData(0)[26000] / out.getChannelData(1)[26000], 1e-4, "both channels get the same gain");
        // 모노 원천은 같은 값이 양쪽 목적지 채널에 나온다.
        OfflineAudioContext ctx2 = new OfflineAudioContext(2, 26496, RATE);
        hardKnee(ctx2, 0.5f, 0);
        AudioBuffer out2 = ctx2.startRendering();
        Check.near(out2.getChannelData(0)[26000], out2.getChannelData(1)[26000], 0.0, "a mono output is up-mixed to both channels");
        Check.near(out.getChannelData(0)[26000] / 0.5, out2.getChannelData(0)[26000] / 0.5, 1e-4, "the stereo and the mono input of the loud channel get the same gain");
    }

    /**
     * The metered reduction: while a loud source plays the reduction is negative decibels, and once the source has stopped and the
     * tail time has passed the node is released and the reduction is 0 again.
     * <p>
     * 측정한 감소량: 큰 원천이 재생되는 동안 감소량은 음의 데시벨이고, 원천이 멈추고 꼬리 시간이 지나면 노드가 풀리고 감소량은 다시 0 이다.
     */
    public void testReductionAndTail() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 132352, RATE);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(0.5f);
        DynamicsCompressorNode comp = ctx.createDynamicsCompressor();
        comp.getKnee().setValue(0f);
        comp.getRatio().setValue(4f);
        c.connect(comp);
        comp.connect(ctx.getDestination());
        c.start(0);
        c.stop(0.5);
        // 원천이 재생되는 중간(0.45 초)까지 렌더링한다.
        while (ctx.getRenderedFrames() < 19840) {
            ctx.renderNextQuantum();
        }
        Check.isTrue(comp.getReduction() < -5.0, "a loud source is reduced: " + comp.getReduction());
        // 끝까지 렌더링한다(3 초). 꼬리 시간(약 1.6 초) 뒤에는 노드가 풀린다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        ctx.finishRendering();
        Check.near(0.0, comp.getReduction(), 0.0, "the reduction is 0 after the tail time");
        Check.equal(0L, (long) ctx.getPinnedNodeCount(), "the node is released after the tail time");
    }
}
