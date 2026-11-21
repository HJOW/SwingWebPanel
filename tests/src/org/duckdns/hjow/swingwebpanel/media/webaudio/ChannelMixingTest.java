package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the channel up-mixing and down-mixing (Web Audio API section 4, plan item P25-02): every pair of speaker layouts (mono, stereo,
 * quad, 5.1) is checked against the equations of sections 4.4 and 4.5 with distinct input values, written out in the test; the layouts
 * that are not speaker layouts and the interpretation <code>discrete</code> fill channels one by one; the mix can overwrite or add.
 * <p>
 * 채널 업믹스/다운믹스 시험(Web Audio API 4절, 계획 P25-02): 스피커 배치(모노, 스테레오, 쿼드, 5.1)의 모든 쌍을 서로 다른 입력 값으로 4.4절과 4.5절의 식과
 * 비교한다(식은 시험에 직접 적었다). 스피커 배치가 아닌 경우와 <code>discrete</code> 해석은 채널을 하나씩 채우며, 섞기는 덮어쓰거나 더할 수 있다.
 */
public class ChannelMixingTest {

    /**
     * The weight of the specification for the centre and the surround channels.
     * <p>
     * 규격이 중앙과 서라운드 채널에 쓰는 가중치.
     */
    private static final double S = Math.sqrt(0.5);

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ChannelMixingTest() {
    }

    /**
     * Mixes one frame with distinct values.
     * <p>
     * 서로 다른 값의 프레임 하나를 섞는다.
     *
     * @param in             the values of the input channels / 입력 채널의 값
     * @param outChannels    the number of output channels / 출력 채널 수
     * @param interpretation the interpretation / 해석
     * @return the values of the output channels / 출력 채널의 값
     */
    private static double[] mix(double[] in, int outChannels, ChannelInterpretation interpretation) {
        AudioBus src = new AudioBus(in.length, 1);
        // 입력 값을 채널마다 채운다.
        for (int c = 0; c < in.length; c++) {
            src.channel(c)[0] = (float) in[c];
        }
        AudioBus dst = new AudioBus(outChannels, 1);
        ChannelMixing.mix(src, dst, interpretation, false);
        double[] out = new double[outChannels];
        // 출력 채널의 값을 읽는다.
        for (int c = 0; c < outChannels; c++) {
            out[c] = dst.channel(c)[0];
        }
        return out;
    }

    /**
     * Checks the values of an output.
     * <p>
     * 출력의 값을 검사한다.
     *
     * @param what     the name / 이름
     * @param actual   the values / 값
     * @param expected the expected values / 기대 값
     */
    private static void same(String what, double[] actual, double[] expected) {
        Check.equal(expected.length, actual.length, what + " channels");
        // 채널마다 비교한다.
        for (int i = 0; i < expected.length; i++) {
            Check.near(expected[i], actual[i], 1e-6, what + " channel " + i);
        }
    }

    /**
     * Mono is up-mixed to stereo (both sides), to quad (L R, surrounds 0) and to 5.1 (centre only).
     * <p>
     * 모노는 스테레오(양쪽), 쿼드(L R, 서라운드 0), 5.1(중앙만)로 업믹스된다.
     */
    public void testMonoUpMix() {
        double[] m = {0.5};
        same("1->2", mix(m, 2, ChannelInterpretation.SPEAKERS), new double[] {0.5, 0.5});
        same("1->4", mix(m, 4, ChannelInterpretation.SPEAKERS), new double[] {0.5, 0.5, 0, 0});
        same("1->6", mix(m, 6, ChannelInterpretation.SPEAKERS), new double[] {0, 0, 0.5, 0, 0, 0});
    }

    /**
     * Stereo is up-mixed to quad and 5.1 with L and R kept and the other channels 0; quad to 5.1 keeps L R SL SR in their places.
     * <p>
     * 스테레오는 쿼드와 5.1 로 L 과 R 을 유지하고 나머지는 0 으로 업믹스된다. 쿼드는 5.1 로 L R SL SR 이 제 자리를 지킨다.
     */
    public void testStereoAndQuadUpMix() {
        double[] st = {0.25, -0.5};
        same("2->4", mix(st, 4, ChannelInterpretation.SPEAKERS), new double[] {0.25, -0.5, 0, 0});
        same("2->6", mix(st, 6, ChannelInterpretation.SPEAKERS), new double[] {0.25, -0.5, 0, 0, 0, 0});
        double[] quad = {0.1, 0.2, 0.3, 0.4};
        // 쿼드의 채널 순서는 L R SL SR 이고 5.1 은 L R C LFE SL SR 이다.
        same("4->6", mix(quad, 6, ChannelInterpretation.SPEAKERS), new double[] {0.1, 0.2, 0, 0, 0.3, 0.4});
    }

    /**
     * The down-mixes to mono: stereo mean, quad quarter of the sum, 5.1 with the weights of the specification.
     * <p>
     * 모노로의 다운믹스: 스테레오는 평균, 쿼드는 합의 4분의 1, 5.1 은 규격의 가중치.
     */
    public void testDownMixToMono() {
        same("2->1", mix(new double[] {0.5, 0.25}, 1, ChannelInterpretation.SPEAKERS), new double[] {0.375});
        same("4->1", mix(new double[] {0.1, 0.2, 0.3, 0.4}, 1, ChannelInterpretation.SPEAKERS), new double[] {0.25 * (0.1 + 0.2 + 0.3 + 0.4)});
        // 5.1: sqrt(0.5) (L + R) + C + 0.5 (SL + SR), LFE 는 버린다.
        same("6->1", mix(new double[] {0.1, 0.2, 0.3, 0.9, 0.5, 0.6}, 1, ChannelInterpretation.SPEAKERS),
                new double[] {S * (0.1 + 0.2) + 0.3 + 0.5 * (0.5 + 0.6)});
    }

    /**
     * The down-mixes to stereo and to quad.
     * <p>
     * 스테레오와 쿼드로의 다운믹스.
     */
    public void testDownMixToStereoAndQuad() {
        same("4->2", mix(new double[] {0.1, 0.2, 0.3, 0.4}, 2, ChannelInterpretation.SPEAKERS), new double[] {0.5 * (0.1 + 0.3), 0.5 * (0.2 + 0.4)});
        double[] f = {0.1, 0.2, 0.3, 0.9, 0.5, 0.6};
        // 5.1 -> 2: L + sqrt(0.5) (C + SL), R + sqrt(0.5) (C + SR)
        same("6->2", mix(f, 2, ChannelInterpretation.SPEAKERS), new double[] {0.1 + S * (0.3 + 0.5), 0.2 + S * (0.3 + 0.6)});
        // 5.1 -> 4: L + sqrt(0.5) C, R + sqrt(0.5) C, SL, SR
        same("6->4", mix(f, 4, ChannelInterpretation.SPEAKERS), new double[] {0.1 + S * 0.3, 0.2 + S * 0.3, 0.5, 0.6});
    }

    /**
     * The same number of channels is copied in either interpretation.
     * <p>
     * 채널 수가 같으면 어느 해석에서도 그대로 복사된다.
     */
    public void testSameCountCopies() {
        double[] in = {0.1, 0.2, 0.3};
        same("3->3 speakers", mix(in, 3, ChannelInterpretation.SPEAKERS), in);
        same("3->3 discrete", mix(in, 3, ChannelInterpretation.DISCRETE), in);
    }

    /**
     * Layouts that are not speaker layouts, and the interpretation <code>discrete</code>, fill the channels one by one: missing channels
     * are 0 and extra channels are dropped.
     * <p>
     * 스피커 배치가 아닌 경우와 <code>discrete</code> 해석은 채널을 하나씩 채운다: 없는 채널은 0 이고 남는 채널은 버린다.
     */
    public void testDiscreteMixing() {
        same("1->3 speakers", mix(new double[] {0.5}, 3, ChannelInterpretation.SPEAKERS), new double[] {0.5, 0, 0});
        same("3->2 speakers", mix(new double[] {0.1, 0.2, 0.3}, 2, ChannelInterpretation.SPEAKERS), new double[] {0.1, 0.2});
        same("1->2 discrete", mix(new double[] {0.5}, 2, ChannelInterpretation.DISCRETE), new double[] {0.5, 0});
        same("2->1 discrete", mix(new double[] {0.5, 0.25}, 1, ChannelInterpretation.DISCRETE), new double[] {0.5});
        same("6->2 discrete", mix(new double[] {1, 2, 3, 4, 5, 6}, 2, ChannelInterpretation.DISCRETE), new double[] {1, 2});
        same("2->6 discrete", mix(new double[] {1, 2}, 6, ChannelInterpretation.DISCRETE), new double[] {1, 2, 0, 0, 0, 0});
        same("8->2 speakers", mix(new double[] {1, 2, 3, 4, 5, 6, 7, 8}, 2, ChannelInterpretation.SPEAKERS), new double[] {1, 2});
    }

    /**
     * Accumulating adds to the destination, overwriting replaces it.
     * <p>
     * 누적하면 대상에 더하고, 덮어쓰면 대상을 바꾼다.
     */
    public void testAccumulate() {
        AudioBus src = new AudioBus(1, 4);
        java.util.Arrays.fill(src.channel(0), 0.5f);
        AudioBus dst = new AudioBus(2, 4);
        java.util.Arrays.fill(dst.channel(0), 1f);
        java.util.Arrays.fill(dst.channel(1), 2f);
        ChannelMixing.mix(src, dst, ChannelInterpretation.SPEAKERS, true);
        Check.near(1.5, dst.channel(0)[3], 1e-9, "accumulated left");
        Check.near(2.5, dst.channel(1)[3], 1e-9, "accumulated right");
        ChannelMixing.mix(src, dst, ChannelInterpretation.SPEAKERS, false);
        Check.near(0.5, dst.channel(0)[3], 1e-9, "overwritten left");
        Check.near(0.5, dst.channel(1)[3], 1e-9, "overwritten right");
    }

    /**
     * The matrices exist exactly for the twelve pairs of speaker layouts.
     * <p>
     * 행렬은 스피커 배치의 열두 쌍에만 있다.
     */
    public void testMatrixTable() {
        int[] layouts = {1, 2, 4, 6};
        int found = 0;
        // 모든 채널 수 쌍에서 행렬이 있는 쌍을 센다.
        for (int i = 0; i < layouts.length; i++) {
            for (int o = 0; o < layouts.length; o++) {
                if (i != o) {
                    Check.notNull(ChannelMixing.speakersMatrix(layouts[i], layouts[o]), "matrix " + layouts[i] + "->" + layouts[o]);
                    found++;
                }
            }
        }
        Check.equal(12, found, "number of pairs");
        Check.isNull(ChannelMixing.speakersMatrix(3, 2), "no matrix for 3 channels");
        Check.isNull(ChannelMixing.speakersMatrix(2, 3), "no matrix for 2->3");
    }
}
