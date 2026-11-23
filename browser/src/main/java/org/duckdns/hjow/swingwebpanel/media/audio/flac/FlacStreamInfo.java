package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;

/**
 * The STREAMINFO metadata block of a FLAC stream: the stream parameters that frame headers may refer to.
 * <p>
 * FLAC 스트림의 STREAMINFO 메타데이터 블록: 프레임 헤더가 참조할 수 있는 스트림 파라미터.
 */
final class FlacStreamInfo {

    /**
     * Size of the STREAMINFO block body in bytes.
     * <p>
     * STREAMINFO 블록 본문의 바이트 수.
     */
    static final int SIZE = 34;

    /**
     * Smallest block size in samples.
     * <p>
     * 가장 작은 블록 크기(샘플 수).
     */
    final int minBlock;

    /**
     * Largest block size in samples.
     * <p>
     * 가장 큰 블록 크기(샘플 수).
     */
    final int maxBlock;

    /**
     * Sample rate in Hz.
     * <p>
     * 샘플링 주파수(Hz).
     */
    final int sampleRate;

    /**
     * Number of channels, 1 to 8.
     * <p>
     * 채널 수(1~8).
     */
    final int channels;

    /**
     * Bits per sample, 4 to 32.
     * <p>
     * 샘플당 비트 수(4~32).
     */
    final int bitsPerSample;

    /**
     * Total number of samples per channel, 0 if unknown.
     * <p>
     * 채널당 전체 샘플 수. 모르면 0.
     */
    final long totalSamples;

    /**
     * Parses a STREAMINFO block body.
     * <p>
     * STREAMINFO 블록 본문을 해석한다.
     *
     * @param d   data / 데이터
     * @param off offset of the 34 bytes / 34바이트의 위치
     * @throws MediaException CORRUPT if the values are impossible / 값이 불가능하면 CORRUPT
     */
    FlacStreamInfo(byte[] d, int off) throws MediaException {
        minBlock = Bytes.be16(d, off);
        maxBlock = Bytes.be16(d, off + 2);
        long packed = ((long) Bytes.be32(d, off + 10) << 32) | Bytes.be32(d, off + 14);
        // 20비트 주파수, 3비트 채널, 5비트 샘플 크기, 36비트 전체 샘플 수가 이어진 64비트 값.
        sampleRate = (int) (packed >>> 44);
        channels = (int) ((packed >>> 41) & 7) + 1;
        bitsPerSample = (int) ((packed >>> 36) & 31) + 1;
        totalSamples = packed & 0xFFFFFFFFFL;
        if (sampleRate < 1 || bitsPerSample < 4) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC STREAMINFO values");
        }
    }
}
