package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the Web Audio tests (plan items P25-02 to P25-04): the construction of an offline context, the rendering to one array, the
 * comparison of a rendered signal with a closed-form expectation that the test computes independently of the engine, and a few
 * measurements (root mean square, the amplitude of a frequency by the Goertzel algorithm). The class is not a suite (its name does not
 * end with <code>Test</code>).
 * <p>
 * Web Audio 시험의 도우미(계획 P25-02~P25-04): 오프라인 컨텍스트 구성, 한 배열로의 렌더링, 렌더링한 신호를 엔진과 별개로 시험이 계산한 닫힌 식의 기대값과
 * 비교하기, 몇 가지 측정(제곱 평균 제곱근, Goertzel 알고리즘으로 구한 주파수의 진폭). 이 클래스는 스위트가 아니다(이름이 <code>Test</code> 로 끝나지 않는다).
 */
public final class WebAudioTestKit {

    /**
     * The sample rate that most tests use.
     * <p>
     * 대부분의 시험이 쓰는 샘플링 주파수.
     */
    public static final float SR = 8000f;

    /**
     * A closed-form expectation: the value of the signal at a frame.
     * <p>
     * 닫힌 식의 기대값: 프레임에서의 신호 값.
     */
    public interface Signal {

        /**
         * Returns the expected value at a frame.
         * <p>
         * 프레임에서의 기대값을 반환한다.
         *
         * @param frame the frame index / 프레임 인덱스
         * @return the value / 값
         */
        double at(int frame);
    }

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private WebAudioTestKit() {
    }

    /**
     * Creates an offline context with one channel at {@link #SR}.
     * <p>
     * {@link #SR} 의 채널 하나인 오프라인 컨텍스트를 만든다.
     *
     * @param frames the length in frames / 길이(프레임)
     * @return the context / 컨텍스트
     */
    public static OfflineAudioContext mono(int frames) {
        return new OfflineAudioContext(1, frames, SR);
    }

    /**
     * Renders the whole context and returns the first channel.
     * <p>
     * 컨텍스트 전체를 렌더링하고 첫 채널을 반환한다.
     *
     * @param ctx the context / 컨텍스트
     * @return the samples of channel 0 / 채널 0 의 샘플
     */
    public static float[] render(OfflineAudioContext ctx) {
        return ctx.startRendering().getChannelData(0);
    }

    /**
     * Checks that a rendered signal equals the expectation at every frame of a range.
     * <p>
     * 렌더링한 신호가 범위의 모든 프레임에서 기대값과 같은지 검사한다.
     *
     * @param what      the name of the check / 검사 이름
     * @param actual    the rendered samples / 렌더링한 샘플
     * @param from      the first frame / 첫 프레임
     * @param to        the end frame, exclusive / 끝 프레임(미포함)
     * @param expected  the expectation / 기대값
     * @param tolerance the largest allowed difference / 허용하는 최대 차이
     */
    public static void assertSignal(String what, float[] actual, int from, int to, Signal expected, double tolerance) {
        // 프레임마다 기대값과의 차이를 확인한다.
        for (int i = from; i < to; i++) {
            double e = expected.at(i);
            if (Math.abs(actual[i] - e) > tolerance) {
                Check.fail(what + ": frame " + i + " expected " + e + " +/- " + tolerance + " but was " + actual[i]);
            }
        }
    }

    /**
     * Checks that a range of samples is silent.
     * <p>
     * 샘플 범위가 무음인지 검사한다.
     *
     * @param what   the name of the check / 검사 이름
     * @param actual the samples / 샘플
     * @param from   the first frame / 첫 프레임
     * @param to     the end frame, exclusive / 끝 프레임(미포함)
     */
    public static void assertSilent(String what, float[] actual, int from, int to) {
        // 프레임마다 0 인지 확인한다.
        for (int i = from; i < to; i++) {
            if (actual[i] != 0f) {
                Check.fail(what + ": frame " + i + " should be silent but was " + actual[i]);
            }
        }
    }

    /**
     * Returns the root mean square of a range.
     * <p>
     * 범위의 제곱 평균 제곱근을 반환한다.
     *
     * @param a    the samples / 샘플
     * @param from the first frame / 첫 프레임
     * @param to   the end frame, exclusive / 끝 프레임(미포함)
     * @return the root mean square / 제곱 평균 제곱근
     */
    public static double rms(float[] a, int from, int to) {
        double sum = 0;
        // 구간의 제곱을 더한다.
        for (int i = from; i < to; i++) {
            sum += (double) a[i] * a[i];
        }
        return Math.sqrt(sum / (to - from));
    }

    /**
     * Returns the largest absolute value of a range.
     * <p>
     * 범위의 절댓값 최댓값을 반환한다.
     *
     * @param a    the samples / 샘플
     * @param from the first frame / 첫 프레임
     * @param to   the end frame, exclusive / 끝 프레임(미포함)
     * @return the peak / 최댓값
     */
    public static double peak(float[] a, int from, int to) {
        double m = 0;
        // 절댓값의 최댓값을 구한다.
        for (int i = from; i < to; i++) {
            m = Math.max(m, Math.abs(a[i]));
        }
        return m;
    }

    /**
     * Returns the amplitude of a sine of a frequency in a range: twice the magnitude of the Fourier sum at that frequency divided by the
     * length (an independent measurement of the amplitude of a tone; exact when the range holds whole periods).
     * <p>
     * 범위 안의 한 주파수 사인의 진폭을 반환한다: 그 주파수의 푸리에 합의 크기에 2 를 곱해 길이로 나눈 값(음의 진폭을 독립적으로 측정한다. 범위가 주기의
     * 정수 배이면 정확하다).
     *
     * @param a     the samples / 샘플
     * @param from  the first frame / 첫 프레임
     * @param to    the end frame, exclusive / 끝 프레임(미포함)
     * @param hz    the frequency in Hz / 주파수(Hz)
     * @param sr    the sample rate / 샘플링 주파수
     * @return the amplitude / 진폭
     */
    public static double amplitudeAt(float[] a, int from, int to, double hz, double sr) {
        double re = 0;
        double im = 0;
        // 주파수의 코사인과 사인을 곱해 더한다.
        for (int i = from; i < to; i++) {
            double w = 2 * Math.PI * hz * i / sr;
            re += a[i] * Math.cos(w);
            im += a[i] * Math.sin(w);
        }
        return 2 * Math.sqrt(re * re + im * im) / (to - from);
    }

    /**
     * Creates a buffer filled with a ramp: frame <code>i</code> has the value <code>i / 1000</code> in channel 0 (and the negated value
     * in channel 1 if there are two channels).
     * <p>
     * 램프로 채운 버퍼를 만든다: 채널 0 의 프레임 <code>i</code> 는 <code>i / 1000</code> 이다(채널이 둘이면 채널 1 은 그 부호를 바꾼 값).
     *
     * @param channels the number of channels, 1 or 2 / 채널 수(1 또는 2)
     * @param length   the length in frames / 길이(프레임)
     * @param rate     the sample rate / 샘플링 주파수
     * @return the buffer / 버퍼
     */
    public static AudioBuffer ramp(int channels, int length, float rate) {
        AudioBuffer b = new AudioBuffer(channels, length, rate);
        // 프레임마다 램프 값을 쓴다.
        for (int i = 0; i < length; i++) {
            b.getChannelData(0)[i] = i / 1000f;
            if (channels > 1) {
                b.getChannelData(1)[i] = -i / 1000f;
            }
        }
        return b;
    }

    /**
     * Returns the kind of the exception thrown by a block, or null if nothing was thrown.
     * <p>
     * 코드 조각이 던진 예외의 종류를 반환한다. 던지지 않으면 null.
     *
     * @param r the block / 코드 조각
     * @return the kind / 종류
     */
    public static AudioException.Kind kindOf(Runnable r) {
        try {
            r.run();
        } catch (AudioException e) {
            return e.getKind();
        }
        return null;
    }

    /**
     * Checks that a block throws a Web Audio error of a kind.
     * <p>
     * 코드 조각이 어떤 종류의 Web Audio 오류를 던지는지 검사한다.
     *
     * @param expected the kind / 종류
     * @param r        the block / 코드 조각
     * @param message  the failure description / 실패 설명
     */
    public static void assertThrows(AudioException.Kind expected, Runnable r, String message) {
        AudioException.Kind k = kindOf(r);
        if (k != expected) {
            Check.fail(message + " (expected " + expected + " but was " + k + ")");
        }
    }
}
