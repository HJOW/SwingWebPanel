package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the convolver node (Web Audio API 1.17, plan item P25-04): the output for an impulse is the impulse response, the six
 * channel configurations of the specification (input of one or two channels, response of one, two or four channels, including the
 * "true stereo" matrix) against the direct convolution sums that the test computes itself, the normalization against the algorithm of
 * the specification written out in the test (the sample rate factor, the minimum power, the halving for four channels, and that
 * <code>normalize</code> is read when the buffer is set), the acquisition of the buffer content, the errors, the tail time, and the
 * silence without a buffer.
 * <p>
 * 컨볼버 노드 시험(Web Audio API 1.17, 계획 P25-04): 임펄스의 출력이 임펄스 응답인 것, 규격의 여섯 가지 채널 구성(입력 채널 1~2, 응답 채널 1, 2, 4개이며
 * "진짜 스테레오" 행렬 포함)을 시험이 직접 계산한 합성곱 합과 비교, 정규화를 규격의 알고리즘(시험에 직접 적었다: 샘플링 주파수 인자, 최소 파워, 채널 4개일 때의
 * 절반, <code>normalize</code> 를 버퍼 설정 때 읽는다는 점)과 비교, 버퍼 내용을 가져감, 오류, 꼬리 시간, 버퍼가 없을 때의 무음을 확인한다.
 */
public class ConvolverNodeTest {

    /**
     * The sample rate of the contexts of the tests.
     * <p>
     * 시험 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ConvolverNodeTest() {
    }

    /**
     * Creates a buffer with a sparse response: the channel <code>c</code> has the taps <code>pos[c][i]</code> with the weights
     * <code>w[c][i]</code>.
     * <p>
     * 성긴 응답의 버퍼를 만든다: 채널 <code>c</code> 는 탭 <code>pos[c][i]</code> 를 가중치 <code>w[c][i]</code> 로 가진다.
     *
     * @param length the length in frames / 길이(프레임)
     * @param pos    the positions / 위치
     * @param w      the weights / 가중치
     * @return the buffer / 버퍼
     */
    private static AudioBuffer sparse(int length, int[][] pos, float[][] w) {
        AudioBuffer b = new AudioBuffer(pos.length, length, RATE);
        // 채널마다 탭에 가중치를 넣는다.
        for (int c = 0; c < pos.length; c++) {
            for (int i = 0; i < pos[c].length; i++) {
                b.getChannelData(c)[pos[c][i]] = w[c][i];
            }
        }
        return b;
    }

    /**
     * Returns the direct convolution of a channel of an input with a channel of a response.
     * <p>
     * 입력 채널과 응답 채널의 직접 합성곱을 반환한다.
     *
     * @param x the input / 입력
     * @param h the response / 응답
     * @param n the number of output frames / 출력 프레임 수
     * @return the output / 출력
     */
    private static double[] conv(float[] x, float[] h, int n) {
        double[] y = new double[n];
        // 출력 프레임마다 직접 합을 계산한다.
        for (int t = 0; t < n; t++) {
            double s = 0;
            // 응답의 탭마다 지연된 입력과 곱한다.
            for (int k = 0; k < h.length && k <= t; k++) {
                if (t - k < x.length) {
                    s += (double) h[k] * x[t - k];
                }
            }
            y[t] = s;
        }
        return y;
    }

    /**
     * Renders a buffer through a convolver into a context of a number of channels.
     * <p>
     * 버퍼를 컨볼버에 통과시켜 채널 수가 정해진 컨텍스트로 렌더링한다.
     *
     * @param input     the input buffer / 입력 버퍼
     * @param response  the response, or null / 응답. 없으면 null
     * @param normalize the <code>normalize</code> flag / <code>normalize</code> 값
     * @param frames    the length of the rendering / 렌더링 길이
     * @param outputs   the number of channels of the context / 컨텍스트의 채널 수
     * @return the rendered audio / 렌더링한 오디오
     */
    private static AudioBuffer render(AudioBuffer input, AudioBuffer response, boolean normalize, int frames, int outputs) {
        OfflineAudioContext ctx = new OfflineAudioContext(outputs, frames, RATE);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(input);
        ConvolverNode cv = ctx.createConvolver();
        cv.setNormalize(normalize);
        if (response != null) {
            cv.setBuffer(response);
        }
        src.connect(cv);
        cv.connect(ctx.getDestination());
        src.start(0);
        return ctx.startRendering();
    }

    /**
     * Creates a mono buffer with an impulse of the value 1 at the first frame.
     * <p>
     * 첫 프레임에 값 1 의 임펄스가 있는 모노 버퍼를 만든다.
     *
     * @param length the length / 길이
     * @return the buffer / 버퍼
     */
    private static AudioBuffer impulse(int length) {
        AudioBuffer b = new AudioBuffer(1, length, RATE);
        b.getChannelData(0)[0] = 1f;
        return b;
    }

    /**
     * An impulse gives the impulse response itself (no latency, no normalization with <code>normalize</code> false), for a response that
     * reaches into every level.
     * <p>
     * 임펄스는 임펄스 응답 그대로를 준다(지연 없음, <code>normalize</code> 가 false 이면 정규화 없음). 모든 레벨에 걸친 응답으로 확인한다.
     */
    public void testImpulseGivesTheResponse() {
        int n = 5000;
        AudioBuffer h = new AudioBuffer(1, n, RATE);
        // 지수로 줄어드는 사인으로 응답을 만든다.
        for (int i = 0; i < n; i++) {
            h.getChannelData(0)[i] = (float) (Math.exp(-i / 1200.0) * Math.sin(2 * Math.PI * 300.0 * i / RATE));
        }
        AudioBuffer out = render(impulse(256), h, false, 6144, 1);
        // 응답의 모든 탭이 첫 프레임부터 그대로 나온다.
        for (int i = 0; i < n; i++) {
            Check.near(h.getChannelData(0)[i], out.getChannelData(0)[i], 1e-6, "tap " + i);
        }
        // 응답 뒤는 무음이다.
        for (int i = n; i < 6144; i++) {
            Check.near(0.0, out.getChannelData(0)[i], 1e-7, "after the response " + i);
        }
    }

    /**
     * The six channel configurations. The response channels have one tap each (different positions and weights), so the expectation is a
     * sum of delayed and scaled input channels that the test writes out from the matrix of the specification.
     * <p>
     * 여섯 가지 채널 구성. 응답 채널은 탭이 하나씩이므로(위치와 가중치가 다르다) 기대값은 지연되고 크기가 바뀐 입력 채널의 합이며, 시험이 규격의 행렬대로 직접
     * 적는다.
     */
    public void testChannelConfigurations() {
        int frames = 1024;
        // 입력: 왼쪽은 프레임 0 의 임펄스 1, 오른쪽은 프레임 3 의 임펄스 -2 와 프레임 40 의 0.5.
        AudioBuffer stereo = new AudioBuffer(2, 256, RATE);
        stereo.getChannelData(0)[0] = 1f;
        stereo.getChannelData(1)[3] = -2f;
        stereo.getChannelData(1)[40] = 0.5f;
        AudioBuffer mono = new AudioBuffer(1, 256, RATE);
        mono.getChannelData(0)[0] = 1f;
        mono.getChannelData(0)[17] = 0.25f;
        int[][] pos = {{5}, {700}, {130}, {2}};
        float[][] w = {{0.5f}, {-0.25f}, {2f}, {0.75f}};
        AudioBuffer resp1 = sparse(1000, new int[][] {pos[0]}, new float[][] {w[0]});
        AudioBuffer resp2 = sparse(1000, new int[][] {pos[0], pos[1]}, new float[][] {w[0], w[1]});
        AudioBuffer resp4 = sparse(1000, pos, w);
        float[] h0 = resp4.getChannelData(0);
        float[] h1 = resp4.getChannelData(1);
        float[] h2 = resp4.getChannelData(2);
        float[] h3 = resp4.getChannelData(3);
        float[] l = stereo.getChannelData(0);
        float[] r = stereo.getChannelData(1);
        float[] m = mono.getChannelData(0);
        // 사례 1: 입력 1 채널, 응답 1 채널, 출력 1 채널.
        AudioBuffer o = render(mono, resp1, false, frames, 1);
        compare("1 -> 1 -> 1", conv(m, h0, frames), o.getChannelData(0));
        // 사례 2: 입력 1 채널, 응답 2 채널, 출력 2 채널.
        o = render(mono, resp2, false, frames, 2);
        compare("1 -> 2 -> 2 left", conv(m, h0, frames), o.getChannelData(0));
        compare("1 -> 2 -> 2 right", conv(m, h1, frames), o.getChannelData(1));
        // 사례 3: 입력 1 채널, 응답 4 채널: 모노가 L 과 R 로 쓰인다.
        o = render(mono, resp4, false, frames, 2);
        compare("1 -> 4 -> 2 left", sum(conv(m, h0, frames), conv(m, h2, frames)), o.getChannelData(0));
        compare("1 -> 4 -> 2 right", sum(conv(m, h1, frames), conv(m, h3, frames)), o.getChannelData(1));
        // 사례 4: 입력 2 채널, 응답 1 채널: 채널마다 같은 응답.
        o = render(stereo, resp1, false, frames, 2);
        compare("2 -> 1 -> 2 left", conv(l, h0, frames), o.getChannelData(0));
        compare("2 -> 1 -> 2 right", conv(r, h0, frames), o.getChannelData(1));
        // 사례 5: 입력 2 채널, 응답 2 채널: 채널마다 자기 응답.
        o = render(stereo, resp2, false, frames, 2);
        compare("2 -> 2 -> 2 left", conv(l, h0, frames), o.getChannelData(0));
        compare("2 -> 2 -> 2 right", conv(r, h1, frames), o.getChannelData(1));
        // 사례 6: 입력 2 채널, 응답 4 채널: 진짜 스테레오.
        o = render(stereo, resp4, false, frames, 2);
        compare("true stereo left", sum(conv(l, h0, frames), conv(r, h2, frames)), o.getChannelData(0));
        compare("true stereo right", sum(conv(l, h1, frames), conv(r, h3, frames)), o.getChannelData(1));
    }

    /**
     * Adds two arrays.
     * <p>
     * 두 배열을 더한다.
     *
     * @param a the first / 첫째
     * @param b the second / 둘째
     * @return the sum / 합
     */
    private static double[] sum(double[] a, double[] b) {
        double[] s = new double[a.length];
        // 요소마다 더한다.
        for (int i = 0; i < a.length; i++) {
            s[i] = a[i] + b[i];
        }
        return s;
    }

    /**
     * Compares a rendered channel with an expectation.
     * <p>
     * 렌더링한 채널을 기대값과 비교한다.
     *
     * @param what     the name of the check / 검사 이름
     * @param expected the expectation / 기대값
     * @param actual   the rendered samples / 렌더링한 샘플
     */
    private static void compare(String what, double[] expected, float[] actual) {
        // 프레임마다 기대값과 비교한다.
        for (int i = 0; i < expected.length; i++) {
            Check.near(expected[i], actual[i], 1e-6, what + " frame " + i);
        }
    }

    /**
     * The normalization of the specification: <code>scale = 0.00125 / max(rms, 0.000125) * 44100 / sampleRate</code>, halved for four
     * channels; the scale applies to the convolution, and a change of <code>normalize</code> after the buffer was set has no effect.
     * <p>
     * 규격의 정규화: <code>scale = 0.00125 / max(rms, 0.000125) * 44100 / sampleRate</code>, 채널이 넷이면 절반이다. 배율은 합성곱에 적용되고 버퍼를 설정한
     * 뒤의 <code>normalize</code> 변경은 효과가 없다.
     */
    public void testNormalization() {
        // 상수 0.1 의 응답(길이 300): rms = 0.1 이다.
        AudioBuffer h = new AudioBuffer(1, 300, RATE);
        java.util.Arrays.fill(h.getChannelData(0), 0.1f);
        double scale = 0.00125 / 0.1 * 44100.0 / RATE;
        AudioBuffer out = render(impulse(128), h, true, 512, 1);
        Check.near(0.1 * scale, out.getChannelData(0)[0], 1e-6, "normalized tap 0");
        Check.near(0.1 * scale, out.getChannelData(0)[299], 1e-6, "normalized tap 299");
        Check.near(scale, ConvolverNode.normalizationScale(h), scale * 1e-6, "normalizationScale");
        // 너무 작은 응답은 최소 파워 0.000125 로 막힌다.
        AudioBuffer tiny = new AudioBuffer(1, 200, RATE);
        java.util.Arrays.fill(tiny.getChannelData(0), 1e-6f);
        double tinyScale = 0.00125 / 0.000125 * 44100.0 / RATE;
        Check.near(1e-6 * tinyScale, render(impulse(128), tiny, true, 512, 1).getChannelData(0)[10], 1e-9, "minimum power");
        // 채널 4개이면 절반이다(상수 0.2, 모든 채널의 rms 가 0.2).
        AudioBuffer four = new AudioBuffer(4, 300, RATE);
        // 네 채널을 같은 상수로 채운다.
        for (int c = 0; c < 4; c++) {
            java.util.Arrays.fill(four.getChannelData(c), 0.2f);
        }
        double fourScale = 0.00125 / 0.2 * 44100.0 / RATE * 0.5;
        AudioBuffer fo = render(impulse(128), four, true, 512, 2);
        // 모노 입력은 L 과 R 로 쓰이므로 출력은 두 경로의 합이다: 2 * 0.2 * scale.
        Check.near(2 * 0.2 * fourScale, fo.getChannelData(0)[5], 1e-6, "true stereo scale");
        // normalize 를 false 로 하면 응답 그대로이고, 버퍼를 설정한 뒤 바꾼 값은 효과가 없다.
        Check.near(0.1, render(impulse(128), h, false, 512, 1).getChannelData(0)[0], 1e-6, "no normalization");
        OfflineAudioContext ctx = new OfflineAudioContext(1, 512, RATE);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse(128));
        ConvolverNode cv = ctx.createConvolver();
        cv.setBuffer(h);
        cv.setNormalize(false);
        Check.isFalse(cv.isNormalize(), "normalize can be changed");
        src.connect(cv);
        cv.connect(ctx.getDestination());
        src.start(0);
        Check.near(0.1 * scale, ctx.startRendering().getChannelData(0)[0], 1e-6, "normalize is read when the buffer is set");
    }

    /**
     * The content of the buffer is acquired when it is set: changes of the array afterwards do not change the convolution.
     * <p>
     * 버퍼의 내용은 설정할 때 가져간다. 이후 배열을 바꿔도 합성곱은 바뀌지 않는다.
     */
    public void testBufferContentIsAcquired() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 512, RATE);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse(128));
        ConvolverNode cv = ctx.createConvolver();
        cv.setNormalize(false);
        AudioBuffer h = new AudioBuffer(1, 100, RATE);
        h.getChannelData(0)[7] = 0.5f;
        cv.setBuffer(h);
        h.getChannelData(0)[7] = 9f;
        h.getChannelData(0)[8] = 9f;
        Check.same(h, cv.getBuffer(), "getBuffer returns the buffer that was set");
        src.connect(cv);
        cv.connect(ctx.getDestination());
        src.start(0);
        float[] o = ctx.startRendering().getChannelData(0);
        Check.near(0.5, o[7], 1e-6, "the acquired tap");
        Check.near(0.0, o[8], 1e-7, "a later change is not heard");
    }

    /**
     * The errors: a response with three channels, a response with another sample rate, a <code>channelCount</code> above 2 and the
     * mode <code>max</code> are <code>NotSupportedError</code>; null is allowed and the node is then silent.
     * <p>
     * 오류: 채널이 셋인 응답, 샘플링 주파수가 다른 응답, 2 를 넘는 <code>channelCount</code>, <code>max</code> 모드는 <code>NotSupportedError</code> 이다.
     * null 은 허용되며 그때 노드는 무음이다.
     */
    public void testErrorsAndNullBuffer() {
        final OfflineAudioContext ctx = new OfflineAudioContext(1, 512, RATE);
        final ConvolverNode cv = ctx.createConvolver();
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                cv.setBuffer(new AudioBuffer(3, 100, RATE));
            }
        }, "a response of three channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                cv.setBuffer(new AudioBuffer(1, 100, 44100f));
            }
        }, "a response with another sample rate");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                cv.setChannelCount(3);
            }
        }, "channelCount 3");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                cv.setChannelCountMode(ChannelCountMode.MAX);
            }
        }, "the mode max");
        Check.equal(2L, (long) cv.getChannelCount(), "the default channelCount is 2");
        Check.same(ChannelCountMode.CLAMPED_MAX, cv.getChannelCountMode(), "the default mode is clamped-max");
        Check.isNull(cv.getBuffer(), "no buffer at first");
        Check.isTrue(cv.isNormalize(), "normalize is true by default");
        AudioBuffer h = new AudioBuffer(1, 100, RATE);
        h.getChannelData(0)[0] = 1f;
        cv.setBuffer(h);
        cv.setBuffer(null);
        Check.isNull(cv.getBuffer(), "null clears the buffer");
        AudioBuffer out = render(impulse(128), null, true, 512, 1);
        // 응답이 없으면 출력은 무음이다.
        for (int i = 0; i < 512; i++) {
            Check.near(0.0, out.getChannelData(0)[i], 0.0, "silence " + i);
        }
    }

    /**
     * The tail time is the length of the response: the output of an impulse reaches the last tap, is silent after it, and the context
     * lets go of the node afterwards.
     * <p>
     * 꼬리 시간은 응답의 길이이다. 임펄스의 출력은 마지막 탭까지 닿고 그 뒤에는 무음이며 그 뒤에 컨텍스트가 노드를 놓아 준다.
     */
    public void testTailTime() {
        AudioBuffer h = new AudioBuffer(1, 3000, RATE);
        h.getChannelData(0)[0] = 1f;
        h.getChannelData(0)[2999] = 0.5f;
        OfflineAudioContext ctx = new OfflineAudioContext(1, 8192, RATE);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(impulse(128));
        ConvolverNode cv = ctx.createConvolver();
        cv.setNormalize(false);
        cv.setBuffer(h);
        src.connect(cv);
        cv.connect(ctx.getDestination());
        src.start(0);
        float[] o = ctx.startRendering().getChannelData(0);
        Check.near(1.0, o[0], 1e-6, "first tap");
        Check.near(0.5, o[2999], 1e-6, "last tap");
        // 꼬리 뒤에는 FFT 반올림 잡음(1e-7 미만)뿐이다.
        for (int i = 3000; i < 8192; i++) {
            Check.near(0.0, o[i], 1e-7, "after the tail " + i);
        }
        Check.equal(0L, (long) ctx.getPinnedNodeCount(), "nothing is pinned after the tail");
    }

    /**
     * A response that is replaced while the context is rendering starts with an empty state (the specification warns of a glitch):
     * after a constant input has played through a response that passes it, the new response that delays by 200 frames outputs silence
     * for 200 frames, which it would not if the old input were still in its memory.
     * <p>
     * 컨텍스트가 렌더링하는 중에 응답을 바꾸면 빈 상태로 시작한다(규격은 글리치를 경고한다). 상수 입력이 그대로 지나가는 응답을 거쳐 재생된 뒤에, 200 프레임
     * 지연하는 새 응답은 200 프레임 동안 무음을 출력한다. 예전 입력이 기억에 남아 있다면 그렇지 않을 것이다.
     */
    public void testReplacingTheResponse() {
        OfflineAudioContext ctx = new OfflineAudioContext(1, 1024, RATE);
        ConstantSourceNode c = ctx.createConstantSource();
        c.getOffset().setValue(1f);
        ConvolverNode cv = ctx.createConvolver();
        cv.setNormalize(false);
        cv.setBuffer(sparse(300, new int[][] {{0}}, new float[][] {{1f}}));
        c.connect(cv);
        cv.connect(ctx.getDestination());
        c.start(0);
        // 4 quantum(512 프레임)을 렌더링한 뒤 200 프레임 지연하는 응답으로 바꾼다.
        for (int q = 0; q < 4; q++) {
            ctx.renderNextQuantum();
        }
        cv.setBuffer(sparse(300, new int[][] {{200}}, new float[][] {{1f}}));
        // 나머지 quantum 을 끝까지 렌더링한다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        float[] all = ctx.finishRendering().getChannelData(0);
        Check.near(1.0, all[511], 1e-6, "before the change the input passes");
        // 새 응답이 빈 상태로 시작하므로 지연 동안 무음이다.
        for (int i = 512; i < 712; i++) {
            Check.near(0.0, all[i], 1e-7, "the new response starts empty " + i);
        }
        Check.near(1.0, all[712], 1e-6, "after the delay of the new response");
    }

    /**
     * A constant input through a constant response gives a ramp that rises by one tap per frame and stays at the sum of the taps once
     * the whole response was filled.
     * <p>
     * 상수 입력을 상수 응답에 통과시키면 프레임마다 탭 하나씩 올라가는 경사가 되고 응답이 모두 채워지면 탭의 합에 머문다.
     */
    public void testConstantInputGivesTheSumOfTheTaps() {
        AudioBuffer a = new AudioBuffer(1, 400, RATE);
        java.util.Arrays.fill(a.getChannelData(0), 0.01f);
        OfflineAudioContext ctx2 = new OfflineAudioContext(1, 1024, RATE);
        ConstantSourceNode c2 = ctx2.createConstantSource();
        ConvolverNode cv2 = ctx2.createConvolver();
        cv2.setNormalize(false);
        cv2.setBuffer(a);
        c2.connect(cv2);
        cv2.connect(ctx2.getDestination());
        c2.start(0);
        float[] o = ctx2.startRendering().getChannelData(0);
        Check.near(0.01 * 1, o[0], 1e-6, "the first frame sees one tap");
        Check.near(0.01 * 200, o[199], 1e-5, "frame 199 sees 200 taps");
        Check.near(4.0, o[600], 1e-4, "the steady state is the sum of the taps");
    }
}
