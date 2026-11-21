package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>AudioBuffer</code> data type (plan item P25-05, Web Audio API 1.4): the construction rules and their limits, the
 * attributes, <code>getChannelData</code>, the exact element counts of <code>copyFromChannel</code>/<code>copyToChannel</code> given by the
 * specification (1.4.3) and the <code>wrap</code> factory that adopts decoded arrays. Expected values are the specification formulas
 * written out by the test, not taken from the implementation.
 * <p>
 * <code>AudioBuffer</code> 자료형의 시험(계획 P25-05, Web Audio API 1.4): 생성 규칙과 한계, 속성, <code>getChannelData</code>, 규격(1.4.3)이 정한
 * <code>copyFromChannel</code>/<code>copyToChannel</code> 의 정확한 원소 수, 디코딩한 배열을 가져가는 <code>wrap</code> 팩터리. 기대값은 구현에서
 * 가져오지 않고 시험이 규격의 식을 직접 풀어 쓴 것이다.
 */
public class AudioBufferTest {

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public AudioBufferTest() {
    }

    /**
     * Runs a constructor call and expects a <code>NotSupportedError</code>.
     * <p>
     * 생성자 호출을 실행하고 <code>NotSupportedError</code> 를 기대한다.
     *
     * @param channels the number of channels / 채널 수
     * @param length   the length / 길이
     * @param rate     the sample rate / 샘플링 주파수
     * @param what     the description / 설명
     */
    private static void rejected(final int channels, final int length, final float rate, String what) {
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                new AudioBuffer(channels, length, rate);
            }
        }, what);
    }

    /**
     * The boundary values of the construction: the smallest and the largest accepted values work, the next values outside are rejected
     * with <code>NotSupportedError</code> (1.4.1), and a value that does not fit a WebIDL unsigned long (a negative Java value) is out of range.
     * <p>
     * 생성의 경계값: 받아들이는 가장 작은 값과 가장 큰 값은 되고 바로 바깥 값은 <code>NotSupportedError</code> 로 거부한다(1.4.1). WebIDL
     * unsigned long 에 들어가지 않는 값(음수인 Java 값)은 범위 밖이다.
     */
    public void testConstructionBoundaries() {
        Check.equal(1, new AudioBuffer(1, 1, 3000f).getNumberOfChannels(), "1 channel");
        Check.equal(32, new AudioBuffer(32, 1, 768000f).getNumberOfChannels(), "32 channels, the highest rate");
        Check.equal(1, new AudioBuffer(1, 1, 3000f).getLength(), "length 1");
        rejected(0, 10, 8000f, "0 channels");
        rejected(33, 10, 8000f, "33 channels");
        rejected(-1, 10, 8000f, "channels as a large unsigned value");
        rejected(1, 0, 8000f, "length 0");
        rejected(1, -1, 8000f, "length as a large unsigned value");
        rejected(1, 10, 2999.99f, "a rate just below the range");
        rejected(1, 10, 768000.06f, "a rate just above the range");
        rejected(1, 10, 0f, "rate 0");
        rejected(1, 10, -8000f, "a negative rate");
        rejected(1, 10, Float.NaN, "rate NaN");
        rejected(1, 10, Float.POSITIVE_INFINITY, "an infinite rate");
        rejected(2, WebAudioLimits.MAX_BUFFER_SAMPLES / 2 + 1, 8000f, "one sample more than the limit");
        rejected(1, WebAudioLimits.MAX_BUFFER_SAMPLES + 1, 8000f, "one frame more than the limit");
    }

    /**
     * The attributes: channels, length, rate, duration (<code>length / sampleRate</code>) and the zero initialisation of every channel.
     * <p>
     * 속성: 채널 수, 길이, 샘플링 주파수, 재생 시간(<code>length / sampleRate</code>), 모든 채널의 0 초기화.
     */
    public void testAttributesAndZeroInitialisation() {
        AudioBuffer b = new AudioBuffer(3, 1234, 44100f);
        Check.equal(3, b.getNumberOfChannels(), "channels");
        Check.equal(1234, b.getLength(), "length");
        Check.near(44100.0, b.getSampleRate(), 0, "sample rate");
        Check.near(1234 / 44100.0, b.getDuration(), 1e-15, "duration");
        // 모든 채널의 모든 샘플이 0 이어야 한다.
        for (int c = 0; c < 3; c++) {
            float[] d = b.getChannelData(c);
            Check.equal(1234, d.length, "channel length " + c);
            for (int i = 0; i < d.length; i++) {
                // 샘플마다 0 인지 확인한다.
                if (d[i] != 0f) {
                    Check.fail("sample " + i + " of channel " + c + " is not zero");
                }
            }
        }
    }

    /**
     * <code>getChannelData</code> returns the live array of the channel and throws <code>IndexSizeError</code> for a channel that does not
     * exist (the index is an unsigned long, so a negative Java value does not exist either).
     * <p>
     * <code>getChannelData</code> 는 채널의 살아 있는 배열을 반환하고 없는 채널이면 <code>IndexSizeError</code> 를 던진다(인덱스는 unsigned long 이므로
     * 음수인 Java 값도 없는 채널이다).
     */
    public void testChannelDataIsLive() {
        final AudioBuffer b = new AudioBuffer(2, 8, 8000f);
        Check.same(b.getChannelData(1), b.getChannelData(1), "the same array every time");
        Check.isTrue(b.getChannelData(0) != b.getChannelData(1), "the channels are distinct arrays");
        b.getChannelData(1)[3] = 0.5f;
        float[] out = new float[8];
        b.copyFromChannel(out, 1, 0);
        Check.near(0.5, out[3], 0, "a change of the live array is seen by a copy");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.getChannelData(2);
            }
        }, "channel 2 of 2");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.getChannelData(-1);
            }
        }, "a negative channel index");
    }

    /**
     * The element count of <code>copyFromChannel</code> is <code>max(0, min(N_b - k, N_f))</code> for a buffer of 10 frames: every
     * combination of destination length and offset is checked against that formula, and the elements that are not copied keep their old
     * values.
     * <p>
     * <code>copyFromChannel</code> 의 원소 수는 10 프레임 버퍼에서 <code>max(0, min(N_b - k, N_f))</code> 이다. 목적지 길이와 시작의 모든 조합을 그 식과
     * 비교하고, 복사하지 않는 원소는 이전 값을 유지하는지 확인한다.
     */
    public void testCopyFromChannelCounts() {
        AudioBuffer b = new AudioBuffer(2, 10, 8000f);
        // 채널마다 서로 다른 값을 채워 채널이 바뀌어 복사되면 드러나게 한다.
        for (int i = 0; i < 10; i++) {
            b.getChannelData(0)[i] = 100 + i;
            b.getChannelData(1)[i] = 200 + i;
        }
        // 목적지 길이와 시작의 모든 조합을 본다.
        for (int nf = 0; nf <= 14; nf++) {
            for (int k = 0; k <= 13; k++) {
                float[] dst = new float[nf];
                java.util.Arrays.fill(dst, -1f);
                b.copyFromChannel(dst, 1, k);
                int expected = Math.max(0, Math.min(10 - k, nf));
                // 복사한 원소는 채널 1 의 시작부터의 값이고 나머지는 -1 그대로여야 한다.
                for (int i = 0; i < nf; i++) {
                    double want = i < expected ? 200 + k + i : -1;
                    Check.near(want, dst[i], 0, "destination " + nf + " offset " + k + " element " + i);
                }
            }
        }
    }

    /**
     * The element count of <code>copyToChannel</code> is <code>max(0, min(N_b - k, N_f))</code>: the elements of the source that do not fit
     * are ignored and the samples of the buffer that are not written keep their values.
     * <p>
     * <code>copyToChannel</code> 의 원소 수는 <code>max(0, min(N_b - k, N_f))</code> 이다. 들어가지 않는 원본 원소는 무시하고 쓰지 않는 버퍼 샘플은
     * 값을 유지한다.
     */
    public void testCopyToChannelCounts() {
        // 원본 길이와 시작의 모든 조합을 본다.
        for (int nf = 0; nf <= 14; nf++) {
            for (int k = 0; k <= 13; k++) {
                AudioBuffer b = new AudioBuffer(2, 10, 8000f);
                java.util.Arrays.fill(b.getChannelData(0), -1f);
                java.util.Arrays.fill(b.getChannelData(1), -1f);
                float[] src = new float[nf];
                for (int i = 0; i < nf; i++) {
                    // 원본에는 1, 2, 3, ... 을 넣는다.
                    src[i] = i + 1;
                }
                b.copyToChannel(src, 1, k);
                int expected = Math.max(0, Math.min(10 - k, nf));
                // 채널 1 은 시작부터 expected 개만 바뀌고 채널 0 은 전혀 바뀌지 않아야 한다.
                for (int i = 0; i < 10; i++) {
                    boolean written = i >= k && i < k + expected;
                    Check.near(written ? i - k + 1 : -1, b.getChannelData(1)[i], 0, "source " + nf + " offset " + k + " frame " + i);
                    Check.near(-1, b.getChannelData(0)[i], 0, "channel 0 is not touched, source " + nf + " offset " + k + " frame " + i);
                }
            }
        }
    }

    /**
     * An offset at, after or far after the end (also a value that only fits an unsigned 32 bit number) copies nothing and is not an error; a
     * channel that does not exist is the only error of the copy methods.
     * <p>
     * 끝과 같거나 뒤이거나 한참 뒤인 시작(부호 없는 32비트에만 들어가는 값 포함)은 아무것도 복사하지 않으며 오류가 아니다. 없는 채널이 복사 메서드의 유일한
     * 오류이다.
     */
    public void testOffsetsBeyondTheEndAndTheOnlyError() {
        final AudioBuffer b = new AudioBuffer(1, 4, 8000f);
        float[] data = b.getChannelData(0);
        data[0] = 1f;
        data[3] = 4f;
        int[] offsets = {4, 5, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE, -1};
        // 끝 이후를 가리키는 모든 시작에서 아무것도 복사되지 않아야 한다.
        for (int i = 0; i < offsets.length; i++) {
            float[] dst = {9f, 9f};
            b.copyFromChannel(dst, 0, offsets[i]);
            Check.near(9, dst[0], 0, "nothing is read at the offset " + offsets[i]);
            Check.near(9, dst[1], 0, "nothing is read at the offset " + offsets[i] + " (second)");
            b.copyToChannel(new float[] {5f, 5f}, 0, offsets[i]);
            Check.near(1, data[0], 0, "nothing is written at the offset " + offsets[i]);
            Check.near(4, data[3], 0, "nothing is written at the offset " + offsets[i] + " (last)");
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.copyFromChannel(new float[1], 1, 0);
            }
        }, "copyFromChannel of a missing channel");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.copyToChannel(new float[1], 1, 0);
            }
        }, "copyToChannel of a missing channel");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                b.copyFromChannel(new float[1], -1, 0);
            }
        }, "a negative channel number is a large unsigned number");
    }

    /**
     * <code>wrap</code> adopts the arrays without a copy, takes the length from them and applies every rule of the constructor, and the
     * arrays have to be of the same length.
     * <p>
     * <code>wrap</code> 은 배열을 복사 없이 가져가고 길이를 배열에서 얻으며 생성자의 모든 규칙을 적용한다. 배열의 길이는 같아야 한다.
     */
    public void testWrap() {
        float[][] ch = {new float[5], new float[5], new float[5]};
        ch[2][4] = 0.25f;
        AudioBuffer b = AudioBuffer.wrap(ch, 22050f);
        Check.equal(3, b.getNumberOfChannels(), "channels");
        Check.equal(5, b.getLength(), "length");
        Check.near(22050.0, b.getSampleRate(), 0, "rate");
        Check.same(ch[2], b.getChannelData(2), "the array is adopted, not copied");
        Check.near(0.25, b.getChannelData(2)[4], 0, "the content is kept");
        final float[][] uneven = {new float[5], new float[4]};
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(uneven, 8000f);
            }
        }, "arrays of different lengths");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(new float[0][], 8000f);
            }
        }, "no channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(null, 8000f);
            }
        }, "null channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(new float[][] {new float[0]}, 8000f);
            }
        }, "empty channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(new float[][] {new float[4], null}, 8000f);
            }
        }, "a null channel");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(new float[33][1], 8000f);
            }
        }, "33 channels");
        WebAudioTestKit.assertThrows(AudioException.Kind.NOT_SUPPORTED, new Runnable() {
            public void run() {
                AudioBuffer.wrap(new float[][] {new float[4]}, 2999f);
            }
        }, "a rate below the range");
    }
}
