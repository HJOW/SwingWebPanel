package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The channel up-mixing and down-mixing of Web Audio API section 4. With the interpretation <code>speakers</code> the layouts mono (1),
 * stereo (2), quad (4: L R SL SR) and 5.1 (6: L R C LFE SL SR) are mixed with the equations of the specification; every other pair of
 * channel counts, and the interpretation <code>discrete</code>, fill the channels one by one and drop or zero the rest. The matrices are
 * the equations of sections 4.4 and 4.5 written as weights.
 * <p>
 * Web Audio API 4절의 채널 업믹스/다운믹스. 해석이 <code>speakers</code> 이면 모노(1), 스테레오(2), 쿼드(4: L R SL SR), 5.1(6: L R C LFE SL SR)
 * 배치를 규격의 식으로 섞고, 그 밖의 채널 수 쌍과 해석 <code>discrete</code> 는 채널을 하나씩 채우고 남는 것은 버리거나 0 으로 둔다. 행렬은 4.4절과
 * 4.5절의 식을 가중치로 적은 것이다.
 */
public final class ChannelMixing {

    /**
     * The weight <code>sqrt(0.5)</code> that the specification uses for the centre and the surround channels.
     * <p>
     * 규격이 중앙과 서라운드 채널에 쓰는 가중치 <code>sqrt(0.5)</code>.
     */
    private static final float S = (float) Math.sqrt(0.5);

    /**
     * Mono to stereo: both sides get the input.
     * <p>
     * 모노에서 스테레오: 양쪽이 입력을 받는다.
     */
    private static final float[][] M_1_2 = {{1f}, {1f}};

    /**
     * Mono to quad: L and R get the input, the surrounds are zero.
     * <p>
     * 모노에서 쿼드: L 과 R 이 입력을 받고 서라운드는 0.
     */
    private static final float[][] M_1_4 = {{1f}, {1f}, {0f}, {0f}};

    /**
     * Mono to 5.1: the input goes to the centre channel.
     * <p>
     * 모노에서 5.1: 입력은 중앙 채널로 간다.
     */
    private static final float[][] M_1_6 = {{0f}, {0f}, {1f}, {0f}, {0f}, {0f}};

    /**
     * Stereo to quad.
     * <p>
     * 스테레오에서 쿼드.
     */
    private static final float[][] M_2_4 = {{1f, 0f}, {0f, 1f}, {0f, 0f}, {0f, 0f}};

    /**
     * Stereo to 5.1.
     * <p>
     * 스테레오에서 5.1.
     */
    private static final float[][] M_2_6 = {{1f, 0f}, {0f, 1f}, {0f, 0f}, {0f, 0f}, {0f, 0f}, {0f, 0f}};

    /**
     * Quad to 5.1: L, R, then the surrounds move to the places 4 and 5.
     * <p>
     * 쿼드에서 5.1: L, R 은 그대로이고 서라운드는 4, 5번 자리로 간다.
     */
    private static final float[][] M_4_6 = {{1f, 0f, 0f, 0f}, {0f, 1f, 0f, 0f}, {0f, 0f, 0f, 0f}, {0f, 0f, 0f, 0f}, {0f, 0f, 1f, 0f}, {0f, 0f, 0f, 1f}};

    /**
     * Stereo to mono: the mean.
     * <p>
     * 스테레오에서 모노: 평균.
     */
    private static final float[][] M_2_1 = {{0.5f, 0.5f}};

    /**
     * Quad to mono: a quarter of the sum.
     * <p>
     * 쿼드에서 모노: 합의 4분의 1.
     */
    private static final float[][] M_4_1 = {{0.25f, 0.25f, 0.25f, 0.25f}};

    /**
     * 5.1 to mono: <code>sqrt(0.5) (L + R) + C + 0.5 (SL + SR)</code>.
     * <p>
     * 5.1 에서 모노: <code>sqrt(0.5) (L + R) + C + 0.5 (SL + SR)</code>.
     */
    private static final float[][] M_6_1 = {{S, S, 1f, 0f, 0.5f, 0.5f}};

    /**
     * Quad to stereo: <code>L = 0.5 (L + SL)</code>, <code>R = 0.5 (R + SR)</code>.
     * <p>
     * 쿼드에서 스테레오: <code>L = 0.5 (L + SL)</code>, <code>R = 0.5 (R + SR)</code>.
     */
    private static final float[][] M_4_2 = {{0.5f, 0f, 0.5f, 0f}, {0f, 0.5f, 0f, 0.5f}};

    /**
     * 5.1 to stereo: <code>L = L + sqrt(0.5) (C + SL)</code>, <code>R = R + sqrt(0.5) (C + SR)</code>.
     * <p>
     * 5.1 에서 스테레오: <code>L = L + sqrt(0.5) (C + SL)</code>, <code>R = R + sqrt(0.5) (C + SR)</code>.
     */
    private static final float[][] M_6_2 = {{1f, 0f, S, 0f, S, 0f}, {0f, 1f, S, 0f, 0f, S}};

    /**
     * 5.1 to quad: the centre is added to L and R, the surrounds stay.
     * <p>
     * 5.1 에서 쿼드: 중앙은 L 과 R 에 더해지고 서라운드는 그대로이다.
     */
    private static final float[][] M_6_4 = {{1f, 0f, S, 0f, 0f, 0f}, {0f, 1f, S, 0f, 0f, 0f}, {0f, 0f, 0f, 0f, 1f, 0f}, {0f, 0f, 0f, 0f, 0f, 1f}};

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private ChannelMixing() {
    }

    /**
     * Returns the weight matrix of the speaker layouts for a pair of channel counts: <code>m[outputChannel][inputChannel]</code>.
     * <p>
     * 채널 수 쌍에 대한 스피커 배치 가중치 행렬을 반환한다: <code>m[출력 채널][입력 채널]</code>.
     *
     * @param in  the number of input channels / 입력 채널 수
     * @param out the number of output channels / 출력 채널 수
     * @return the matrix, or null if the pair is not a speaker layout pair (mixed as discrete) / 행렬. 스피커 배치 쌍이 아니면 null(discrete 로 섞는다)
     */
    public static float[][] speakersMatrix(int in, int out) {
        if (in == 1 && out == 2) {
            return M_1_2;
        }
        if (in == 1 && out == 4) {
            return M_1_4;
        }
        if (in == 1 && out == 6) {
            return M_1_6;
        }
        if (in == 2 && out == 4) {
            return M_2_4;
        }
        if (in == 2 && out == 6) {
            return M_2_6;
        }
        if (in == 4 && out == 6) {
            return M_4_6;
        }
        if (in == 2 && out == 1) {
            return M_2_1;
        }
        if (in == 4 && out == 1) {
            return M_4_1;
        }
        if (in == 6 && out == 1) {
            return M_6_1;
        }
        if (in == 4 && out == 2) {
            return M_4_2;
        }
        if (in == 6 && out == 2) {
            return M_6_2;
        }
        if (in == 6 && out == 4) {
            return M_6_4;
        }
        return null;
    }

    /**
     * Mixes the source bus into the destination bus, converting the channel count with the interpretation. The two buses have the same
     * number of frames (the shorter one decides if they differ).
     * <p>
     * 원본 버스를 대상 버스로 섞으며 해석에 따라 채널 수를 변환한다. 두 버스는 프레임 수가 같다(다르면 짧은 쪽을 따른다).
     *
     * @param src            the source / 원본
     * @param dst            the destination; its number of channels decides the result / 대상. 채널 수가 결과를 정한다
     * @param interpretation how to convert the channels / 채널 변환 방식
     * @param accumulate     true to add to the destination, false to overwrite it / 대상에 더하면 true, 덮어쓰면 false
     */
    public static void mix(AudioBus src, AudioBus dst, ChannelInterpretation interpretation, boolean accumulate) {
        int frames = Math.min(src.getFrames(), dst.getFrames());
        int sc = src.getNumberOfChannels();
        int dc = dst.getNumberOfChannels();
        if (!accumulate) {
            dst.zero();
        }
        float[][] m = (interpretation == ChannelInterpretation.SPEAKERS && sc != dc) ? speakersMatrix(sc, dc) : null;
        if (m == null) {
            int n = Math.min(sc, dc);
            // 채널 수가 같거나 discrete 이면 채널을 하나씩 더한다. 남는 입력 채널은 버리고 남는 출력 채널은 그대로 둔다.
            for (int c = 0; c < n; c++) {
                add(src.channel(c), dst.channel(c), frames, 1f);
            }
            return;
        }
        // 스피커 행렬: 출력 채널마다 입력 채널의 가중 합을 더한다.
        for (int o = 0; o < dc; o++) {
            for (int i = 0; i < sc; i++) {
                float w = m[o][i];
                if (w != 0f) {
                    add(src.channel(i), dst.channel(o), frames, w);
                }
            }
        }
    }

    /**
     * Adds a scaled copy of one array to another.
     * <p>
     * 배열 하나에 다른 배열의 가중 복사본을 더한다.
     *
     * @param from   the source samples / 원본 샘플
     * @param to     the samples that receive the sum / 합을 받는 샘플
     * @param frames the number of frames / 프레임 수
     * @param weight the weight / 가중치
     */
    private static void add(float[] from, float[] to, int frames, float weight) {
        if (weight == 1f) {
            // 가중치가 1 이면 곱셈 없이 더한다.
            for (int i = 0; i < frames; i++) {
                to[i] += from[i];
            }
        } else {
            // 가중치를 곱해 더한다.
            for (int i = 0; i < frames; i++) {
                to[i] += from[i] * weight;
            }
        }
    }
}
