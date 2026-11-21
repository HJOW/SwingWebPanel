package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the decoding tests (plan item P25-05): synthetic WAV files of sine waves of known frequency and the comparison of a decoded
 * buffer with the 16 bit samples it must hold. The class is not a suite (its name does not end with <code>Test</code>).
 * <p>
 * 디코딩 시험의 도우미(계획 P25-05): 주파수를 아는 사인파의 합성 WAV 파일과, 디코딩한 버퍼를 그것이 담아야 하는 16비트 표본과 비교하는 일. 이 클래스는
 * 스위트가 아니다(이름이 <code>Test</code> 로 끝나지 않는다).
 */
final class AudioFiles {

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private AudioFiles() {
    }

    /**
     * Builds a 16 bit PCM WAV file with sine waves.
     * <p>
     * 사인파로 16비트 PCM WAV 파일을 만든다.
     *
     * @param rate   the sample rate of the file / 파일의 표본율
     * @param frames the number of frames / 프레임 수
     * @param hz     the frequency of the sine of every channel (one entry per channel) / 채널마다의 사인 주파수(채널마다 하나)
     * @param amp    the amplitude, 1 is full scale / 진폭(1 이 풀 스케일)
     * @return the file / 파일
     */
    static byte[] sineWav(int rate, int frames, double[] hz, double amp) {
        int channels = hz.length;
        short[] pcm = new short[frames * channels];
        // 프레임마다 채널의 사인 값을 16비트 정수로 반올림해 인터리브한다.
        for (int f = 0; f < frames; f++) {
            for (int c = 0; c < channels; c++) {
                pcm[f * channels + c] = (short) Math.round(amp * 32768.0 * Math.sin(2 * Math.PI * hz[c] * f / rate));
            }
        }
        return AudioKit.wav(1, channels, rate, 16, AudioKit.toBytes(pcm), -1);
    }

    /**
     * Builds a 16 bit PCM WAV file from the samples of the channels.
     * <p>
     * 채널의 표본으로 16비트 PCM WAV 파일을 만든다.
     *
     * @param rate     the sample rate / 표본율
     * @param channels the samples, one array per channel / 표본. 채널마다 배열 하나
     * @return the file / 파일
     */
    static byte[] wav(int rate, short[][] channels) {
        int n = channels[0].length;
        short[] pcm = new short[n * channels.length];
        // 채널의 표본을 프레임 순서로 인터리브한다.
        for (int f = 0; f < n; f++) {
            for (int c = 0; c < channels.length; c++) {
                pcm[f * channels.length + c] = channels[c][f];
            }
        }
        return AudioKit.wav(1, channels.length, rate, 16, AudioKit.toBytes(pcm), -1);
    }

    /**
     * Checks that a buffer holds exactly the given interleaved 16 bit samples (each as <code>sample / 32768</code>).
     * <p>
     * 버퍼가 주어진 인터리브된 16비트 표본(각각 <code>표본 / 32768</code>)을 정확히 담고 있는지 검사한다.
     *
     * @param what     the description / 설명
     * @param buffer   the buffer / 버퍼
     * @param shorts   the interleaved samples / 인터리브된 표본
     */
    static void assertExactly(String what, AudioBuffer buffer, short[] shorts) {
        int ch = buffer.getNumberOfChannels();
        Check.equal(shorts.length / ch, buffer.getLength(), what + ": frames");
        // 표본마다 float 값이 정수 / 32768 과 정확히 같은지 확인한다.
        for (int c = 0; c < ch; c++) {
            float[] d = buffer.getChannelData(c);
            // 채널의 모든 프레임을 확인한다.
            for (int f = 0; f < d.length; f++) {
                if (d[f] != shorts[f * ch + c] / 32768f) {
                    Check.fail(what + ": channel " + c + " frame " + f + " is " + d[f] + " instead of " + shorts[f * ch + c] + " / 32768");
                }
            }
        }
    }
}
