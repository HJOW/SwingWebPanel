package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the stereo panner (Web Audio API 1.30 and 6.3.3, plan item P25-04): the gains of mono and stereo input against the equations
 * of the specification written out in the test for several pan values, the clamping of the pan, the per-frame automation, and the
 * channels (more than two input channels are down-mixed to stereo, the output always has two).
 * <p>
 * 스테레오 패너 시험(Web Audio API 1.30과 6.3.3, 계획 P25-04): 모노와 스테레오 입력의 이득을 여러 pan 값에서 규격의 식(시험에 직접 적었다)과 비교하고,
 * pan 제한, 프레임별 자동화, 채널(둘을 넘는 입력 채널은 스테레오로 다운믹스되고 출력은 항상 둘)을 확인한다.
 */
public class StereoPannerNodeTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public StereoPannerNodeTest() {
    }

    /**
     * Renders a constant mono signal of 0.5 through a panner of a pan value into a stereo context.
     * <p>
     * 0.5 의 일정한 모노 신호를 pan 값을 가진 패너를 거쳐 스테레오 컨텍스트로 렌더링한다.
     *
     * @param pan the pan / pan
     * @return the rendered stereo audio / 렌더링한 스테레오 오디오
     */
    private static AudioBuffer mono(float pan) {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 256, 8000f);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(0.5f);
        StereoPannerNode p = ctx.createStereoPanner();
        p.getPan().setValue(pan);
        c.connect(p);
        p.connect(ctx.getDestination());
        c.start(0);
        return ctx.startRendering();
    }

    /**
     * A mono input is panned across the image: <code>x = (pan + 1) / 2</code>, left <code>cos(x pi / 2)</code>, right
     * <code>sin(x pi / 2)</code>.
     * <p>
     * 모노 입력은 이미지 전체에 패닝된다: <code>x = (pan + 1) / 2</code>, 왼쪽 <code>cos(x pi / 2)</code>, 오른쪽 <code>sin(x pi / 2)</code>.
     */
    public void testMonoInput() {
        float[] pans = {-1f, -0.5f, 0f, 0.25f, 1f};
        // pan 값마다 규격의 식과 비교한다.
        for (int i = 0; i < pans.length; i++) {
            AudioBuffer out = mono(pans[i]);
            double x = (pans[i] + 1) / 2.0;
            Check.near(0.5 * Math.cos(x * Math.PI / 2), out.getChannelData(0)[100], 1e-6, "left at pan " + pans[i]);
            Check.near(0.5 * Math.sin(x * Math.PI / 2), out.getChannelData(1)[100], 1e-6, "right at pan " + pans[i]);
        }
        AudioBuffer centre = mono(0f);
        Check.near(0.5 * Math.sqrt(0.5), centre.getChannelData(0)[0], 1e-6, "centre keeps the power: each side is 1/sqrt(2)");
    }

    /**
     * A stereo input is balanced: with <code>pan <= 0</code> the right input is mixed into the left, otherwise the left input into
     * the right.
     * <p>
     * 스테레오 입력은 균형이 맞춰진다: <code>pan <= 0</code> 이면 오른쪽 입력이 왼쪽에 섞이고, 아니면 왼쪽 입력이 오른쪽에 섞인다.
     */
    public void testStereoInput() {
        float[] pans = {-1f, -0.5f, 0f, 0.5f, 1f};
        // pan 값마다 스테레오 입력(왼쪽 0.5, 오른쪽 -0.25)을 규격의 식과 비교한다.
        for (int i = 0; i < pans.length; i++) {
            OfflineAudioContext ctx = new OfflineAudioContext(2, 256, 8000f);
            AudioBuffer in = new AudioBuffer(2, 256, 8000f);
            java.util.Arrays.fill(in.getChannelData(0), 0.5f);
            java.util.Arrays.fill(in.getChannelData(1), -0.25f);
            AudioBufferSourceNode src = ctx.createBufferSource();
            src.setBuffer(in);
            StereoPannerNode p = ctx.createStereoPanner();
            p.getPan().setValue(pans[i]);
            src.connect(p);
            p.connect(ctx.getDestination());
            src.start(0);
            AudioBuffer out = ctx.startRendering();
            double pan = pans[i];
            double x = pan <= 0 ? pan + 1 : pan;
            double gl = Math.cos(x * Math.PI / 2);
            double gr = Math.sin(x * Math.PI / 2);
            double l = 0.5;
            double r = -0.25;
            double el = pan <= 0 ? l + r * gl : l * gl;
            double er = pan <= 0 ? r * gr : r + l * gr;
            Check.near(el, out.getChannelData(0)[10], 1e-6, "left at pan " + pan);
            Check.near(er, out.getChannelData(1)[10], 1e-6, "right at pan " + pan);
        }
    }

    /**
     * The pan is limited to the range from -1 to 1.
     * <p>
     * pan 은 -1 에서 1 까지로 제한된다.
     */
    public void testPanIsClamped() {
        AudioBuffer far = mono(5f);
        Check.near(0.0, far.getChannelData(0)[5], 1e-6, "pan 5 is full right: no left");
        Check.near(0.5, far.getChannelData(1)[5], 1e-6, "pan 5 is full right: all right");
        AudioBuffer low = mono(-7f);
        Check.near(0.5, low.getChannelData(0)[5], 1e-6, "pan -7 is full left");
        Check.near(0.0, low.getChannelData(1)[5], 1e-6, "pan -7 is full left: no right");
    }

    /**
     * <code>pan</code> is a-rate: a ramp from -1 to 1 over the rendering changes the gains for every frame.
     * <p>
     * <code>pan</code> 은 a-rate 이다: 렌더링 동안 -1 에서 1 로 오르는 램프는 프레임마다 이득을 바꾼다.
     */
    public void testPanAutomation() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 400, 8000f);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(1f);
        StereoPannerNode p = ctx.createStereoPanner();
        p.getPan().setValueAtTime(-1f, 0);
        p.getPan().linearRampToValueAtTime(1f, 400 / 8000.0);
        c.connect(p);
        p.connect(ctx.getDestination());
        c.start(0);
        AudioBuffer out = ctx.startRendering();
        // 프레임 i 의 pan 은 -1 + 2 i / 400 이다.
        for (int i = 0; i < 400; i += 13) {
            double pan = -1 + 2.0 * i / 400;
            double x = (pan + 1) / 2;
            Check.near(Math.cos(x * Math.PI / 2), out.getChannelData(0)[i], 2e-6, "left at frame " + i);
            Check.near(Math.sin(x * Math.PI / 2), out.getChannelData(1)[i], 2e-6, "right at frame " + i);
        }
    }

    /**
     * The panner has two output channels whatever the input; an input with more channels is down-mixed to stereo first.
     * <p>
     * 패너는 입력과 상관없이 출력 채널이 둘이다. 채널이 더 많은 입력은 먼저 스테레오로 다운믹스된다.
     */
    public void testChannels() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 256, 8000f);
        AudioBuffer quad = new AudioBuffer(4, 256, 8000f);
        float[] v = {0.1f, 0.2f, 0.3f, 0.4f};
        // 쿼드 입력: L R SL SR.
        for (int c = 0; c < 4; c++) {
            java.util.Arrays.fill(quad.getChannelData(c), v[c]);
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(quad);
        StereoPannerNode p = ctx.createStereoPanner();
        p.getPan().setValue(1f);
        src.connect(p);
        p.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        // 쿼드는 스테레오로 L' = 0.5 (L + SL), R' = 0.5 (R + SR) 로 다운믹스되고, pan 1 이면 왼쪽 입력이 오른쪽으로 모두 넘어간다.
        double l = 0.5 * (0.1 + 0.3);
        double r = 0.5 * (0.2 + 0.4);
        Check.near(0.0, out.getChannelData(0)[3], 1e-6, "left output at full right pan");
        Check.near(r + l, out.getChannelData(1)[3], 1e-6, "right output is the right input plus the left input");
        Check.equal(2, p.outputs[0].bus.getNumberOfChannels(), "two output channels");
        Check.equal(1, p.getNumberOfInputs(), "one input");
        Check.equal(1, p.getNumberOfOutputs(), "one output");
        Check.equal(ChannelCountMode.CLAMPED_MAX, p.getChannelCountMode(), "mode");
    }
}
