package org.duckdns.hjow.swingwebpanel.media.playback;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the speed change of the audio player (plan item P18-08, <code>playbackRate</code>; FEATURE_MATRIX AUD-10): the length of the
 * result, the pitch with and without <code>preservePitch</code>, the level, that the chunking of the input does not change the result, a
 * very short input, the stereo channels, and the supported range. Everything is computed on arrays, without a sound device.
 * <p>
 * 오디오 재생기의 속도 변경 시험(계획 P18-08, <code>playbackRate</code>; FEATURE_MATRIX AUD-10): 결과의 길이, <code>preservePitch</code> 에 따른
 * 음높이, 크기, 입력을 나누는 방식이 결과를 바꾸지 않음, 아주 짧은 입력, 스테레오 채널, 지원 범위. 모든 계산은 배열에서 하며 소리 장치를 쓰지 않는다.
 */
public class RateConverterTest {

    /**
     * The sample rate of the test sounds.
     * <p>
     * 시험 소리의 샘플링 주파수.
     */
    private static final int SR = 22050;

    /**
     * Makes a sine wave as little-endian 16-bit interleaved PCM.
     * <p>
     * 사인파를 리틀 엔디언 16비트 인터리브 PCM 으로 만든다.
     *
     * @param frames   the number of frames / 프레임 수
     * @param channels the number of channels / 채널 수
     * @param hz       the frequency of the first channel (the second channel has 1.5 times) / 첫 채널의 주파수(둘째 채널은 1.5배)
     * @return the bytes / 바이트
     */
    private static byte[] sine(int frames, int channels, double hz) {
        byte[] b = new byte[frames * channels * 2];
        int p = 0;
        // 프레임마다 채널별 사인 값을 쓴다.
        for (int i = 0; i < frames; i++) {
            for (int c = 0; c < channels; c++) {
                double f = c == 0 ? hz : hz * 1.5;
                int v = (int) Math.round(12000 * Math.sin(2 * Math.PI * f * i / SR));
                b[p++] = (byte) v;
                b[p++] = (byte) (v >> 8);
            }
        }
        return b;
    }

    /**
     * Converts the whole sound with a converter, giving it chunks of the given size.
     * <p>
     * 소리 전체를 주어진 크기의 덩어리로 변환기에 주며 변환한다.
     *
     * @param src      the sound / 소리
     * @param channels the channels / 채널 수
     * @param rate     the speed / 속도
     * @param keep     true to keep the pitch / 음높이를 유지하려면 true
     * @param chunk    the frames per chunk / 덩어리당 프레임 수
     * @return the converted sound / 변환된 소리
     */
    private static byte[] convert(byte[] src, int channels, double rate, boolean keep, int chunk) {
        RateConverter rc = new RateConverter(SR, channels);
        rc.setRate(rate, keep);
        byte[] all = new byte[src.length * 8];
        int n = 0;
        int frameBytes = channels * 2;
        byte[] tmp = new byte[8192 * frameBytes];
        // 입력을 덩어리로 나누어 주고 준비된 출력을 그때그때 가져온다.
        for (int at = 0; at < src.length; at += chunk * frameBytes) {
            int len = Math.min(chunk * frameBytes, src.length - at);
            rc.push(src, at, len);
            int got;
            // 준비된 출력이 없을 때까지 계속 가져온다.
            while ((got = rc.read(tmp, 0, tmp.length)) > 0) {
                System.arraycopy(tmp, 0, all, n, got);
                n += got;
            }
        }
        rc.finish();
        int got;
        // 끝낸 뒤 남은 출력을 모두 가져온다.
        while ((got = rc.read(tmp, 0, tmp.length)) > 0) {
            System.arraycopy(tmp, 0, all, n, got);
            n += got;
        }
        Check.isTrue(rc.isDrained(), "the converter is drained after the end");
        return Arrays.copyOf(all, n);
    }

    /**
     * Counts the rising zero crossings of one channel and returns the frequency that they give.
     * <p>
     * 한 채널의 상승 영점 통과를 세어 그것이 주는 주파수를 반환한다.
     *
     * @param pcm      the sound / 소리
     * @param channels the channels / 채널 수
     * @param channel  the channel / 채널
     * @param from     the first frame to look at / 살펴볼 첫 프레임
     * @param to       the end frame (exclusive) / 끝 프레임(미포함)
     * @return the frequency in Hz / 주파수(Hz)
     */
    private static double frequency(byte[] pcm, int channels, int channel, int from, int to) {
        int crossings = 0;
        int prev = sample(pcm, channels, channel, from);
        // 부호가 음에서 양으로 바뀌는 곳을 센다.
        for (int i = from + 1; i < to; i++) {
            int v = sample(pcm, channels, channel, i);
            if (prev < 0 && v >= 0) {
                crossings++;
            }
            prev = v;
        }
        return crossings / ((to - from) / (double) SR);
    }

    /**
     * Reads one sample.
     * <p>
     * 샘플 하나를 읽는다.
     *
     * @param pcm      the sound / 소리
     * @param channels the channels / 채널 수
     * @param channel  the channel / 채널
     * @param frame    the frame / 프레임
     * @return the sample / 샘플
     */
    private static int sample(byte[] pcm, int channels, int channel, int frame) {
        int p = (frame * channels + channel) * 2;
        return (short) ((pcm[p] & 0xff) | (pcm[p + 1] << 8));
    }

    /**
     * Returns the root mean square of the first channel over a range of frames.
     * <p>
     * 첫 채널의 한 범위의 제곱평균제곱근을 반환한다.
     *
     * @param pcm      the sound / 소리
     * @param channels the channels / 채널 수
     * @param from     the first frame / 첫 프레임
     * @param to       the end frame (exclusive) / 끝 프레임(미포함)
     * @return the value / 값
     */
    private static double rms(byte[] pcm, int channels, int from, int to) {
        double sum = 0;
        // 범위의 샘플 제곱을 더한다.
        for (int i = from; i < to; i++) {
            double v = sample(pcm, channels, 0, i);
            sum += v * v;
        }
        return Math.sqrt(sum / (to - from));
    }

    /**
     * Doubling the speed keeps the pitch: half the length, the same frequency, the same level.
     * <p>
     * 속도를 두 배로 하면 음높이는 유지된다: 길이는 절반, 주파수와 크기는 그대로.
     */
    public void testFasterKeepsPitch() {
        byte[] src = sine(SR * 2, 1, 440);
        byte[] out = convert(src, 1, 2.0, true, 2048);
        int frames = out.length / 2;
        Check.isTrue(Math.abs(frames - SR) <= 2, "half the length: " + frames);
        Check.near(440, frequency(out, 1, 0, 2000, frames - 2000), 15, "the pitch is kept");
        Check.near(12000 / Math.sqrt(2), rms(out, 1, 2000, frames - 2000), 900, "the level is kept");
    }

    /**
     * Halving the speed keeps the pitch: twice the length, the same frequency.
     * <p>
     * 속도를 절반으로 하면 음높이는 유지된다: 길이는 두 배, 주파수는 그대로.
     */
    public void testSlowerKeepsPitch() {
        byte[] src = sine(SR, 1, 440);
        byte[] out = convert(src, 1, 0.5, true, 1500);
        int frames = out.length / 2;
        Check.isTrue(Math.abs(frames - SR * 2) <= 2, "twice the length: " + frames);
        Check.near(440, frequency(out, 1, 0, 2000, frames - 2000), 15, "the pitch is kept");
        Check.near(12000 / Math.sqrt(2), rms(out, 1, 2000, frames - 2000), 900, "the level is kept");
    }

    /**
     * Four times the speed still produces sound of the right length with the pitch kept roughly.
     * <p>
     * 속도 4배도 올바른 길이의 소리를 만들고 음높이는 대체로 유지된다.
     */
    public void testFourTimes() {
        byte[] src = sine(SR * 4, 1, 300);
        byte[] out = convert(src, 1, 4.0, true, 2048);
        int frames = out.length / 2;
        Check.isTrue(Math.abs(frames - SR) <= 2, "a quarter of the length: " + frames);
        Check.near(300, frequency(out, 1, 0, 2000, frames - 2000), 40, "the pitch is roughly kept");
    }

    /**
     * Without preserving the pitch the sound is resampled: the frequency follows the speed.
     * <p>
     * 음높이를 유지하지 않으면 소리는 리샘플링된다: 주파수가 속도를 따른다.
     */
    public void testResamplingChangesPitch() {
        byte[] src = sine(SR * 2, 1, 440);
        byte[] fast = convert(src, 1, 2.0, false, 2048);
        int fastFrames = fast.length / 2;
        Check.isTrue(Math.abs(fastFrames - SR) <= 2, "half the length: " + fastFrames);
        Check.near(880, frequency(fast, 1, 0, 100, fastFrames - 100), 15, "twice the frequency");
        byte[] slow = convert(src, 1, 0.5, false, 3000);
        int slowFrames = slow.length / 2;
        Check.isTrue(Math.abs(slowFrames - SR * 4) <= 2, "twice the length: " + slowFrames);
        Check.near(220, frequency(slow, 1, 0, 100, slowFrames - 100), 8, "half the frequency");
    }

    /**
     * The way the input is cut into chunks does not change the result.
     * <p>
     * 입력을 덩어리로 나누는 방식은 결과를 바꾸지 않는다.
     */
    public void testChunkingDoesNotMatter() {
        byte[] src = sine(SR, 2, 330);
        // 음높이를 유지하는 방법과 리샘플링 방법을 차례로 확인한다.
        for (int k = 0; k < 2; k++) {
            boolean keep = k == 0;
            byte[] a = convert(src, 2, 1.5, keep, 997);
            byte[] b = convert(src, 2, 1.5, keep, 4096);
            byte[] c = convert(src, 2, 1.5, keep, SR);
            Check.isTrue(Arrays.equals(a, b), "chunks of 997 and 4096 give the same sound (keep=" + keep + ")");
            Check.isTrue(Arrays.equals(b, c), "chunks of 4096 and the whole give the same sound (keep=" + keep + ")");
        }
    }

    /**
     * Both channels of a stereo sound keep their own frequency.
     * <p>
     * 스테레오 소리의 두 채널은 각자의 주파수를 유지한다.
     */
    public void testStereoChannelsKeepTheirPitch() {
        byte[] src = sine(SR * 2, 2, 400);
        byte[] out = convert(src, 2, 2.0, true, 2048);
        int frames = out.length / 4;
        Check.near(400, frequency(out, 2, 0, 2000, frames - 2000), 15, "the left channel");
        Check.near(600, frequency(out, 2, 1, 2000, frames - 2000), 20, "the right channel");
    }

    /**
     * An input shorter than a window still produces output of about the right length.
     * <p>
     * 창보다 짧은 입력도 대략 올바른 길이의 출력을 만든다.
     */
    public void testVeryShortInput() {
        byte[] src = sine(300, 1, 440);
        byte[] out = convert(src, 1, 2.0, true, 300);
        Check.isTrue(out.length / 2 >= 100 && out.length / 2 <= 200, "about half of 300 frames: " + out.length / 2);
        byte[] none = convert(new byte[0], 1, 2.0, true, 300);
        Check.equal(0, none.length, "silence in, nothing out");
    }

    /**
     * Resetting forgets the state: converting again after a reset gives the same sound as a new converter.
     * <p>
     * 초기화하면 상태를 잊는다: 초기화 뒤에 다시 변환하면 새 변환기와 같은 소리가 나온다.
     */
    public void testResetStartsOver() {
        byte[] src = sine(SR / 2, 1, 500);
        RateConverter rc = new RateConverter(SR, 1);
        rc.setRate(1.25, true);
        rc.push(src, 0, src.length);
        rc.reset();
        rc.push(src, 0, src.length);
        rc.finish();
        byte[] tmp = new byte[src.length * 2];
        int got = rc.read(tmp, 0, tmp.length);
        byte[] again = Arrays.copyOf(tmp, got);
        byte[] fresh = convert(src, 1, 1.25, true, src.length / 2);
        Check.isTrue(Arrays.equals(fresh, again), "the converter after reset behaves like a new one");
    }

    /**
     * The supported range is a quarter to four times.
     * <p>
     * 지원 범위는 4분의 1배에서 4배이다.
     */
    public void testSupportedRange() {
        Check.isTrue(RateConverter.isSupported(0.25), "a quarter");
        Check.isTrue(RateConverter.isSupported(1.0), "normal");
        Check.isTrue(RateConverter.isSupported(4.0), "four times");
        Check.isFalse(RateConverter.isSupported(0.2), "too slow");
        Check.isFalse(RateConverter.isSupported(4.5), "too fast");
        Check.isFalse(RateConverter.isSupported(0), "zero");
        Check.isFalse(RateConverter.isSupported(-1), "backwards");
        Check.isFalse(RateConverter.isSupported(Double.NaN), "not a number");
    }
}
