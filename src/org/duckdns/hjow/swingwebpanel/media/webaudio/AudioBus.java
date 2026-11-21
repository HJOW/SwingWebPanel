package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * A block of audio: a number of channels of one render quantum of float samples. The engine allocates the buses of a node once and
 * reuses them for every quantum; only a change of the number of channels reallocates, and only when the new number exceeds the capacity
 * that was allocated before. The channels are live arrays: the node that owns the bus writes them, the nodes that read the bus must not
 * change them.
 * <p>
 * 오디오 블록: render quantum 하나 분량의 float 샘플로 된 여러 채널. 엔진은 노드의 버스를 한 번 할당해 quantum 마다 다시 쓰며, 채널 수가 바뀌고
 * 새 수가 이전에 할당한 용량을 넘을 때만 다시 할당한다. 채널은 살아 있는 배열이다: 버스를 가진 노드가 쓰고 버스를 읽는 노드는 바꾸면 안 된다.
 */
public final class AudioBus {

    /**
     * The channel arrays; the length is the allocated capacity, only the first {@link #channels} are in use.
     * <p>
     * 채널 배열. 길이는 할당한 용량이며 앞의 {@link #channels} 개만 쓰인다.
     */
    private float[][] data;

    /**
     * The number of channels in use.
     * <p>
     * 쓰이는 채널 수.
     */
    private int channels;

    /**
     * The number of sample frames of every channel.
     * <p>
     * 모든 채널의 샘플 프레임 수.
     */
    private final int frames;

    /**
     * Creates a silent bus.
     * <p>
     * 무음 버스를 만든다.
     *
     * @param channels the number of channels, at least 1 / 채널 수(1 이상)
     * @param frames   the number of sample frames, at least 1 / 샘플 프레임 수(1 이상)
     */
    public AudioBus(int channels, int frames) {
        if (channels < 1 || channels > WebAudioLimits.MAX_CHANNELS || frames < 1) {
            throw new IllegalArgumentException("channels=" + channels + " frames=" + frames);
        }
        this.frames = frames;
        this.channels = channels;
        this.data = new float[channels][frames];
    }

    /**
     * Returns the number of channels in use.
     * <p>
     * 쓰이는 채널 수를 반환한다.
     *
     * @return the number of channels / 채널 수
     */
    public int getNumberOfChannels() {
        return channels;
    }

    /**
     * Returns the number of sample frames of every channel.
     * <p>
     * 모든 채널의 샘플 프레임 수를 반환한다.
     *
     * @return the number of frames / 프레임 수
     */
    public int getFrames() {
        return frames;
    }

    /**
     * Returns the live array of a channel.
     * <p>
     * 채널의 살아 있는 배열을 반환한다.
     *
     * @param c the channel, from 0 to {@link #getNumberOfChannels()} - 1 / 채널(0 ~ {@link #getNumberOfChannels()} - 1)
     * @return the samples / 샘플
     */
    public float[] channel(int c) {
        return data[c];
    }

    /**
     * Sets the number of channels in use. Channels that become visible keep whatever they held, so the caller zeroes the bus when it needs
     * silence.
     * <p>
     * 쓰이는 채널 수를 정한다. 새로 보이게 된 채널은 가지고 있던 값을 그대로 가지므로, 무음이 필요하면 호출자가 버스를 0 으로 채운다.
     *
     * @param n the new number of channels, at least 1 / 새 채널 수(1 이상)
     */
    public void setNumberOfChannels(int n) {
        if (n < 1 || n > WebAudioLimits.MAX_CHANNELS) {
            throw new IllegalArgumentException("channels=" + n);
        }
        if (n > data.length) {
            float[][] bigger = new float[n][];
            System.arraycopy(data, 0, bigger, 0, data.length);
            // 용량을 넘는 채널만 새로 할당한다.
            for (int c = data.length; c < n; c++) {
                bigger[c] = new float[frames];
            }
            data = bigger;
        }
        channels = n;
    }

    /**
     * Fills every channel in use with zero.
     * <p>
     * 쓰이는 모든 채널을 0 으로 채운다.
     */
    public void zero() {
        // 채널마다 0 으로 채운다.
        for (int c = 0; c < channels; c++) {
            java.util.Arrays.fill(data[c], 0f);
        }
    }

    /**
     * Makes this bus a single silent channel.
     * <p>
     * 이 버스를 무음 채널 하나로 만든다.
     */
    public void makeSilentMono() {
        channels = 1;
        java.util.Arrays.fill(data[0], 0f);
    }

    /**
     * Copies the channels of another bus of the same frame count (the number of channels follows the source).
     * <p>
     * 프레임 수가 같은 다른 버스의 채널을 복사한다(채널 수는 원본을 따른다).
     *
     * @param src the source / 원본
     */
    public void copyFrom(AudioBus src) {
        setNumberOfChannels(src.channels);
        // 채널마다 복사한다.
        for (int c = 0; c < channels; c++) {
            System.arraycopy(src.data[c], 0, data[c], 0, Math.min(frames, src.frames));
        }
    }
}
