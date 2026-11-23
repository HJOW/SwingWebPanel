package org.duckdns.hjow.swingwebpanel.media.audio;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Presents a 16 bit {@link PcmSource} as a {@link FloatPcmSource}: every sample becomes <code>sample / 32768</code>, which is exact. It is what
 * {@link AudioDecoders#openWide} uses for a decoder that has no wide path of its own, so such a decoder gives (at most two channels of) 16 bit
 * precision, exactly what it gives for playing. Not thread-safe.
 * <p>
 * 16비트 {@link PcmSource} 를 {@link FloatPcmSource} 로 보여 준다. 모든 표본이 <code>표본 / 32768</code> 이 되며 이는 정확하다. 자기 넓은 경로가 없는 디코더에
 * 대해 {@link AudioDecoders#openWide} 가 쓰므로, 그런 디코더는 (최대 두 채널의) 16비트 정밀도를 준다. 재생용으로 주는 것과 똑같다. 스레드 안전하지 않다.
 */
public final class PcmFloatAdapter implements FloatPcmSource {

    /**
     * The number of frames converted per read of the source.
     * <p>
     * 소스를 한 번 읽을 때 변환하는 프레임 수.
     */
    private static final int BLOCK_FRAMES = 4096;

    /**
     * The wrapped source.
     * <p>
     * 감싼 소스.
     */
    private final PcmSource source;

    /**
     * Bytes of the wrapped source that are read and converted.
     * <p>
     * 감싼 소스에서 읽어 변환할 바이트.
     */
    private final byte[] bytes;

    /**
     * Creates the adapter.
     * <p>
     * 어댑터를 만든다.
     *
     * @param source the 16 bit source; the adapter owns it and closes it with {@link #close()} / 16비트 소스. 어댑터가 소유하며 {@link #close()} 때 닫는다
     */
    public PcmFloatAdapter(PcmSource source) {
        this.source = source;
        this.bytes = new byte[BLOCK_FRAMES * source.getChannels() * 2];
    }

    /**
     * Returns the sample rate of the wrapped source.
     * <p>
     * 감싼 소스의 샘플링 주파수를 반환한다.
     *
     * @return sample rate / 샘플링 주파수
     */
    public int getSampleRate() {
        return source.getSampleRate();
    }

    /**
     * Returns the channels of the wrapped source (1 or 2).
     * <p>
     * 감싼 소스의 채널 수(1 또는 2)를 반환한다.
     *
     * @return channels / 채널 수
     */
    public int getChannels() {
        return source.getChannels();
    }

    /**
     * Returns the frame count of the wrapped source.
     * <p>
     * 감싼 소스의 프레임 수를 반환한다.
     *
     * @return frames or -1 / 프레임 수 또는 -1
     */
    public long getFrameCount() {
        return source.getFrameCount();
    }

    /**
     * Reads frames and converts them (see {@link FloatPcmSource#read}).
     * <p>
     * 프레임을 읽어 변환한다({@link FloatPcmSource#read} 참조).
     *
     * @param planar one array per channel / 채널마다 배열 하나
     * @param offset first sample index in every array / 모든 배열에서 첫 표본 인덱스
     * @param frames maximum number of frames / 최대 프레임 수
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException the failure of the wrapped source / 감싼 소스의 실패
     */
    public int read(float[][] planar, int offset, int frames) throws MediaException {
        int channels = source.getChannels();
        if (planar == null || planar.length < channels || offset < 0 || frames < 1) {
            throw new IllegalArgumentException("invalid buffer range");
        }
        int want = Math.min(frames, BLOCK_FRAMES);
        int n = source.read(bytes, 0, want * channels * 2);
        if (n < 0) {
            return -1;
        }
        int count = n / (channels * 2);
        // 프레임마다 채널의 16비트 표본을 float 로 바꾼다.
        for (int f = 0; f < count; f++) {
            for (int c = 0; c < channels; c++) {
                int p = (f * channels + c) * 2;
                planar[c][offset + f] = ((short) ((bytes[p] & 0xFF) | (bytes[p + 1] << 8))) / 32768f;
            }
        }
        return count;
    }

    /**
     * Closes the wrapped source.
     * <p>
     * 감싼 소스를 닫는다.
     */
    public void close() {
        source.close();
    }
}
