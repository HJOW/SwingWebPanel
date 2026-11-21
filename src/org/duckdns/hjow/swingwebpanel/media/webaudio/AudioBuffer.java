package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * An in-memory audio asset: linear PCM floats in a number of channels with a sample rate (Web Audio API 1.4). A source node plays it and
 * an offline context renders into it. The channel arrays are live: {@link #getChannelData(int)} returns the array itself, so the script
 * layer can wrap it without a copy. A playing source node shares the arrays (the specification says it acquires the content at
 * <code>start()</code>; this engine does not copy, so a change of the arrays while the buffer plays is heard).
 * <p>
 * 메모리 안의 오디오 자료: 샘플링 주파수를 가진 여러 채널의 선형 PCM float(Web Audio API 1.4). 원천 노드가 재생하고 오프라인 컨텍스트가 여기에
 * 렌더링한다. 채널 배열은 살아 있다: {@link #getChannelData(int)} 는 배열 자체를 반환하므로 스크립트 계층이 복사 없이 감쌀 수 있다. 재생 중인 원천
 * 노드는 배열을 공유한다(규격은 <code>start()</code> 때 내용을 획득한다고 하지만 이 엔진은 복사하지 않으므로, 재생 중에 배열을 바꾸면 그 소리가 들린다).
 */
public final class AudioBuffer {

    /**
     * The channel arrays.
     * <p>
     * 채널 배열.
     */
    private final float[][] data;

    /**
     * The sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    private final float sampleRate;

    /**
     * The number of sample frames of every channel.
     * <p>
     * 모든 채널의 샘플 프레임 수.
     */
    private final int length;

    /**
     * Creates a silent buffer. Every argument must be inside its range, or a <code>NotSupportedError</code> is thrown
     * (<code>createBuffer</code>, Web Audio API 1.1.2).
     * <p>
     * 무음 버퍼를 만든다. 모든 인수가 범위 안이어야 하며 아니면 <code>NotSupportedError</code> 를 던진다(<code>createBuffer</code>,
     * Web Audio API 1.1.2).
     *
     * @param numberOfChannels the number of channels, from 1 to {@link WebAudioLimits#MAX_CHANNELS} / 채널 수(1 ~ {@link WebAudioLimits#MAX_CHANNELS})
     * @param length           the number of sample frames, at least 1 / 샘플 프레임 수(1 이상)
     * @param sampleRate       the sample rate, from 3000 to 768000 Hz / 샘플링 주파수(3000 ~ 768000 Hz)
     */
    public AudioBuffer(int numberOfChannels, int length, float sampleRate) {
        check(numberOfChannels, length, sampleRate);
        this.length = length;
        this.sampleRate = sampleRate;
        this.data = new float[numberOfChannels][length];
    }

    /**
     * Creates a buffer that adopts the given channel arrays (no copy). Used by code that has already produced the samples, such as the
     * audio file decoder, to avoid allocating and copying a second set of arrays. The arrays must not be modified by the caller through
     * a second reference unless it wants the buffer to change.
     * <p>
     * 주어진 채널 배열을 그대로 가져가는(복사하지 않는) 버퍼를 만든다. 오디오 파일 디코더처럼 샘플을 이미 만든 코드가 두 번째 배열 묶음을 할당하고
     * 복사하는 일을 피하려고 쓴다. 버퍼가 바뀌기를 원하는 경우가 아니라면 호출한 쪽은 두 번째 참조로 배열을 수정하지 않아야 한다.
     *
     * @param channels   the channel arrays, 1 to {@link WebAudioLimits#MAX_CHANNELS} of them, all of the same length of at least 1 / 채널 배열(1 ~ {@link WebAudioLimits#MAX_CHANNELS} 개, 길이는 모두 같고 1 이상)
     * @param sampleRate the sample rate, from 3000 to 768000 Hz / 샘플링 주파수(3000 ~ 768000 Hz)
     * @return the buffer, which shares the arrays / 배열을 공유하는 버퍼
     * @throws AudioException <code>NotSupportedError</code> if a count, the length (also when the arrays differ in length or the total is above
     *                        {@link WebAudioLimits#MAX_BUFFER_SAMPLES}) or the sample rate is out of range / 개수, 길이(배열의 길이가 서로 다르거나 총량이
     *                        {@link WebAudioLimits#MAX_BUFFER_SAMPLES} 를 넘는 경우 포함), 샘플링 주파수가 범위 밖이면 <code>NotSupportedError</code>
     */
    public static AudioBuffer wrap(float[][] channels, float sampleRate) {
        if (channels == null || channels.length == 0 || channels[0] == null) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "numberOfChannels 0 is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        int len = channels[0].length;
        check(channels.length, len, sampleRate);
        // 모든 채널 배열의 길이가 같아야 한다.
        for (int c = 1; c < channels.length; c++) {
            if (channels[c] == null || channels[c].length != len) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "channel " + c + " does not have the length " + len + " of channel 0");
            }
        }
        return new AudioBuffer(channels, len, sampleRate);
    }

    /**
     * Adopts checked arrays.
     * <p>
     * 검사를 마친 배열을 가져간다.
     *
     * @param data       the channel arrays / 채널 배열
     * @param length     the length of every channel / 모든 채널의 길이
     * @param sampleRate the sample rate / 샘플링 주파수
     */
    private AudioBuffer(float[][] data, int length, float sampleRate) {
        this.data = data;
        this.length = length;
        this.sampleRate = sampleRate;
    }

    /**
     * Checks the three construction values against their nominal ranges (<code>NotSupportedError</code>). Negative Java values stand for
     * WebIDL <code>unsigned long</code> values of 2<sup>31</sup> or more, which are out of range as well.
     * <p>
     * 생성에 쓰는 세 값이 공칭 범위 안인지 검사한다(<code>NotSupportedError</code>). 음수인 Java 값은 2<sup>31</sup> 이상인 WebIDL
     * <code>unsigned long</code> 값을 뜻하며 이것도 범위 밖이다.
     *
     * @param numberOfChannels the number of channels / 채널 수
     * @param length           the number of sample frames / 샘플 프레임 수
     * @param sampleRate       the sample rate / 샘플링 주파수
     */
    private static void check(int numberOfChannels, int length, float sampleRate) {
        if (numberOfChannels < 1 || numberOfChannels > WebAudioLimits.MAX_CHANNELS) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "numberOfChannels " + numberOfChannels + " is not in 1.." + WebAudioLimits.MAX_CHANNELS);
        }
        if (length < 1) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "length " + length + " is less than 1");
        }
        checkSampleRate(sampleRate);
        if ((long) length * numberOfChannels > WebAudioLimits.MAX_BUFFER_SAMPLES) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the buffer needs more than " + WebAudioLimits.MAX_BUFFER_SAMPLES + " samples");
        }
    }

    /**
     * Throws a <code>NotSupportedError</code> if the sample rate is not finite or outside the supported range.
     * <p>
     * 샘플링 주파수가 유한하지 않거나 지원 범위 밖이면 <code>NotSupportedError</code> 를 던진다.
     *
     * @param sampleRate the sample rate / 샘플링 주파수
     */
    static void checkSampleRate(float sampleRate) {
        if (!(sampleRate >= WebAudioLimits.MIN_SAMPLE_RATE && sampleRate <= WebAudioLimits.MAX_SAMPLE_RATE)) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "sampleRate " + sampleRate + " is not in " + WebAudioLimits.MIN_SAMPLE_RATE + ".." + WebAudioLimits.MAX_SAMPLE_RATE);
        }
    }

    /**
     * Returns the number of channels.
     * <p>
     * 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    public int getNumberOfChannels() {
        return data.length;
    }

    /**
     * Returns the number of sample frames of every channel.
     * <p>
     * 모든 채널의 샘플 프레임 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz)를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the duration in seconds: the length divided by the sample rate.
     * <p>
     * 길이를 샘플링 주파수로 나눈 재생 시간(초)을 반환한다.
     *
     * @return the duration / 재생 시간
     */
    public double getDuration() {
        return length / (double) sampleRate;
    }

    /**
     * Returns the live array of a channel.
     * <p>
     * 채널의 살아 있는 배열을 반환한다.
     *
     * @param channel the channel index / 채널 인덱스
     * @return the samples (the array itself, not a copy) / 샘플(복사본이 아니라 배열 자체)
     * @throws AudioException <code>IndexSizeError</code> if the channel does not exist / 채널이 없으면 <code>IndexSizeError</code>
     */
    public float[] getChannelData(int channel) {
        if (channel < 0 || channel >= data.length) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "channel " + channel + " is not in 0.." + (data.length - 1));
        }
        return data[channel];
    }

    /**
     * Copies the samples of a channel into an array, starting at a frame of the channel (<code>copyFromChannel</code>, Web Audio API
     * 1.4.3). With <i>N<sub>b</sub></i> frames in the buffer, <i>N<sub>f</sub></i> elements in the destination and the offset <i>k</i>, exactly
     * <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code> frames are copied to the start of the destination and the rest of the
     * destination is not modified. An offset at or after the end is therefore not an error: nothing is copied. The two integer arguments
     * stand for WebIDL <code>unsigned long</code> values (a negative Java value is a value of 2<sup>31</sup> or more).
     * <p>
     * 채널의 샘플을 배열로 복사한다. 채널의 어느 프레임에서 시작할지 정할 수 있다(<code>copyFromChannel</code>, Web Audio API 1.4.3). 버퍼의
     * 프레임이 <i>N<sub>b</sub></i>, 목적지 배열의 원소가 <i>N<sub>f</sub></i>, 시작이 <i>k</i> 이면 정확히
     * <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code> 프레임을 목적지의 앞에서부터 복사하고 목적지의 나머지는 바꾸지 않는다. 그래서 시작이
     * 끝과 같거나 그 뒤여도 오류가 아니며 아무것도 복사하지 않는다. 두 정수 인수는 WebIDL <code>unsigned long</code> 값을 뜻한다(음수인 Java 값은
     * 2<sup>31</sup> 이상인 값이다).
     *
     * @param destination   the array that receives the samples / 샘플을 받는 배열
     * @param channelNumber the channel index / 채널 인덱스
     * @param bufferOffset  the first frame of the channel to copy / 복사할 채널의 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel does not exist / 채널이 없으면 <code>IndexSizeError</code>
     */
    public void copyFromChannel(float[] destination, int channelNumber, int bufferOffset) {
        float[] src = getChannelData(channelNumber);
        long count = copyCount(destination.length, bufferOffset);
        if (count > 0) {
            System.arraycopy(src, bufferOffset, destination, 0, (int) count);
        }
    }

    /**
     * Copies the samples of an array into a channel, starting at a frame of the channel (<code>copyToChannel</code>, Web Audio API 1.4.3).
     * With <i>N<sub>b</sub></i> frames in the buffer, <i>N<sub>f</sub></i> elements in the source and the offset <i>k</i>, exactly
     * <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code> frames are copied from the start of the source and the rest of the
     * buffer is not modified; elements of the source that do not fit are ignored and an offset at or after the end copies nothing. The two
     * integer arguments stand for WebIDL <code>unsigned long</code> values (a negative Java value is a value of 2<sup>31</sup> or more).
     * <p>
     * 배열의 샘플을 채널로 복사한다. 채널의 어느 프레임에서 시작할지 정할 수 있다(<code>copyToChannel</code>, Web Audio API 1.4.3). 버퍼의 프레임이
     * <i>N<sub>b</sub></i>, 원본 배열의 원소가 <i>N<sub>f</sub></i>, 시작이 <i>k</i> 이면 정확히 <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code>
     * 프레임을 원본의 앞에서부터 복사하고 버퍼의 나머지는 바꾸지 않는다. 들어가지 않는 원본 원소는 무시하고 시작이 끝과 같거나 그 뒤이면 아무것도 복사하지
     * 않는다. 두 정수 인수는 WebIDL <code>unsigned long</code> 값을 뜻한다(음수인 Java 값은 2<sup>31</sup> 이상인 값이다).
     *
     * @param source        the array with the samples / 샘플이 든 배열
     * @param channelNumber the channel index / 채널 인덱스
     * @param bufferOffset  the first frame of the channel to overwrite / 덮어쓸 채널의 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel does not exist / 채널이 없으면 <code>IndexSizeError</code>
     */
    public void copyToChannel(float[] source, int channelNumber, int bufferOffset) {
        float[] dst = getChannelData(channelNumber);
        long count = copyCount(source.length, bufferOffset);
        if (count > 0) {
            System.arraycopy(source, 0, dst, bufferOffset, (int) count);
        }
    }

    /**
     * The number of frames a copy moves: <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code> with the offset taken as an unsigned
     * 32 bit value.
     * <p>
     * 복사가 옮기는 프레임 수: 시작을 부호 없는 32비트 값으로 보고 <code>max(0, min(N<sub>b</sub> - k, N<sub>f</sub>))</code>.
     *
     * @param arrayLength  the number of elements of the other array / 다른 배열의 원소 수
     * @param bufferOffset the offset argument / 시작 인수
     * @return the number of frames to copy / 복사할 프레임 수
     */
    private long copyCount(int arrayLength, int bufferOffset) {
        long k = bufferOffset & 0xFFFFFFFFL;
        return Math.max(0L, Math.min(length - k, (long) arrayLength));
    }
}
