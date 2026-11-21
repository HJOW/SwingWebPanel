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
        this.length = length;
        this.sampleRate = sampleRate;
        this.data = new float[numberOfChannels][length];
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
     * Copies the samples of a channel into an array, starting at a frame of the channel.
     * <p>
     * 채널의 샘플을 배열로 복사한다. 채널의 어느 프레임에서 시작할지 정할 수 있다.
     *
     * @param destination      the array that receives the samples / 샘플을 받는 배열
     * @param channelNumber    the channel index / 채널 인덱스
     * @param bufferOffset     the first frame of the channel to copy / 복사할 채널의 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel does not exist or the offset is not smaller than the length /
     *                        채널이 없거나 시작이 길이 이상이면 <code>IndexSizeError</code>
     */
    public void copyFromChannel(float[] destination, int channelNumber, int bufferOffset) {
        float[] src = getChannelData(channelNumber);
        if (bufferOffset < 0 || bufferOffset >= length) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "bufferOffset " + bufferOffset + " is not in 0.." + (length - 1));
        }
        int n = Math.min(destination.length, length - bufferOffset);
        System.arraycopy(src, bufferOffset, destination, 0, n);
    }

    /**
     * Copies the samples of an array into a channel, starting at a frame of the channel.
     * <p>
     * 배열의 샘플을 채널로 복사한다. 채널의 어느 프레임에서 시작할지 정할 수 있다.
     *
     * @param source        the array with the samples / 샘플이 든 배열
     * @param channelNumber the channel index / 채널 인덱스
     * @param bufferOffset  the first frame of the channel to overwrite / 덮어쓸 채널의 첫 프레임
     * @throws AudioException <code>IndexSizeError</code> if the channel does not exist or the offset is not smaller than the length /
     *                        채널이 없거나 시작이 길이 이상이면 <code>IndexSizeError</code>
     */
    public void copyToChannel(float[] source, int channelNumber, int bufferOffset) {
        float[] dst = getChannelData(channelNumber);
        if (bufferOffset < 0 || bufferOffset >= length) {
            throw new AudioException(AudioException.Kind.INDEX_SIZE, "bufferOffset " + bufferOffset + " is not in 0.." + (length - 1));
        }
        int n = Math.min(source.length, length - bufferOffset);
        System.arraycopy(source, 0, dst, bufferOffset, n);
    }
}
