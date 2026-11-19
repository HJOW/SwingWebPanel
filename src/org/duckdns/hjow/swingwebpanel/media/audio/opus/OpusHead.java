package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The identification header of an Opus stream (RFC 7845 section 5.1, the "OpusHead" packet of Ogg Opus, which is also the
 * CodecPrivate of a WebM/Matroska track): the number of channels, the pre-skip (the decoder output that has to be discarded at
 * the start), the output gain and the channel mapping (family 0: mono or stereo; family 1: up to 8 channels in the Vorbis
 * order that are coded as several mono and stereo streams). Other mapping families are reported as unsupported.
 * <p>
 * Opus 스트림의 식별 헤더(RFC 7845 5.1. Ogg Opus 의 "OpusHead" 패킷이며 WebM/Matroska 트랙의 CodecPrivate 이기도 하다): 채널 수, 사전 건너뛰기
 * (시작에서 버려야 하는 디코더 출력), 출력 이득, 채널 매핑(군 0: 모노 또는 스테레오. 군 1: Vorbis 순서의 최대 8채널로 모노/스테레오 스트림 여러
 * 개로 부호화). 다른 매핑 군은 미지원으로 보고한다.
 */
final class OpusHead {

    /**
     * Number of output channels.
     * <p>
     * 출력 채널 수.
     */
    final int channels;

    /**
     * Number of 48 kHz samples to discard at the start of the decoder output.
     * <p>
     * 디코더 출력의 시작에서 버릴 48 kHz 샘플 수.
     */
    final int preSkip;

    /**
     * Sampling rate of the original input (informational).
     * <p>
     * 원본 입력의 샘플링 주파수(참고용).
     */
    final long inputRate;

    /**
     * Output gain in dB with 8 fractional bits (a signed 16-bit value).
     * <p>
     * 소수 8비트인 dB 단위 출력 이득(부호 있는 16비트 값).
     */
    final int outputGainQ8;

    /**
     * Channel mapping family.
     * <p>
     * 채널 매핑 군.
     */
    final int family;

    /**
     * Number of coded streams.
     * <p>
     * 부호화된 스트림 수.
     */
    final int streamCount;

    /**
     * Number of coded streams that carry two channels (they come first).
     * <p>
     * 채널이 둘인 스트림의 수(앞에 온다).
     */
    final int coupledCount;

    /**
     * For each output channel the index of the decoded channel (255 for silence).
     * <p>
     * 출력 채널마다 디코딩된 채널의 번호(무음이면 255).
     */
    final int[] mapping;

    /**
     * Creates a header.
     * <p>
     * 헤더를 만든다.
     *
     * @param channels     channels / 채널 수
     * @param preSkip      pre-skip / 사전 건너뛰기
     * @param inputRate    input rate / 입력 주파수
     * @param outputGainQ8 output gain / 출력 이득
     * @param family       mapping family / 매핑 군
     * @param streamCount  streams / 스트림 수
     * @param coupledCount coupled streams / 결합 스트림 수
     * @param mapping      channel mapping / 채널 매핑
     */
    private OpusHead(int channels, int preSkip, long inputRate, int outputGainQ8, int family, int streamCount, int coupledCount, int[] mapping) {
        this.channels = channels;
        this.preSkip = preSkip;
        this.inputRate = inputRate;
        this.outputGainQ8 = outputGainQ8;
        this.family = family;
        this.streamCount = streamCount;
        this.coupledCount = coupledCount;
        this.mapping = mapping;
    }

    /**
     * Parses an identification header.
     * <p>
     * 식별 헤더를 파싱한다.
     *
     * @param d   the bytes / 바이트
     * @param off start of the header / 헤더의 시작
     * @param len size of the header / 헤더의 크기
     * @return the header / 헤더
     * @throws MediaException CORRUPT for a malformed header, UNSUPPORTED_FORMAT for a version or mapping family that is not supported / 올바르지 않은 헤더는 CORRUPT, 지원하지 않는 버전이나 매핑 군은 UNSUPPORTED_FORMAT
     */
    static OpusHead parse(byte[] d, int off, int len) throws MediaException {
        if (len < 19 || d[off] != 'O' || d[off + 1] != 'p' || d[off + 2] != 'u' || d[off + 3] != 's' || d[off + 4] != 'H' || d[off + 5] != 'e'
                || d[off + 6] != 'a' || d[off + 7] != 'd') {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid OpusHead identification header");
        }
        int version = d[off + 8] & 0xFF;
        if ((version & 0xF0) != 0) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported Opus header version " + version);
        }
        int channels = d[off + 9] & 0xFF;
        int preSkip = (d[off + 10] & 0xFF) | ((d[off + 11] & 0xFF) << 8);
        long rate = (d[off + 12] & 0xFFL) | ((d[off + 13] & 0xFFL) << 8) | ((d[off + 14] & 0xFFL) << 16) | ((d[off + 15] & 0xFFL) << 24);
        int gain = (short) ((d[off + 16] & 0xFF) | ((d[off + 17] & 0xFF) << 8));
        int family = d[off + 18] & 0xFF;
        if (channels < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the Opus stream has no channels");
        }
        if (family == 0) {
            if (channels > 2) {
                throw new MediaException(MediaException.Kind.CORRUPT, "channel mapping family 0 allows at most two channels");
            }
            return new OpusHead(channels, preSkip, rate, gain, 0, 1, channels - 1, channels == 1 ? new int[] { 0 } : new int[] { 0, 1 });
        }
        if (family != 1) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported Opus channel mapping family " + family);
        }
        if (channels > 8) {
            throw new MediaException(MediaException.Kind.CORRUPT, "channel mapping family 1 allows at most eight channels");
        }
        if (len < 21 + channels) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the channel mapping table of the Opus header is truncated");
        }
        int streams = d[off + 19] & 0xFF;
        int coupled = d[off + 20] & 0xFF;
        if (streams < 1 || coupled > streams || streams > 8) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid or too many streams in the Opus header (at most eight are supported)");
        }
        int[] map = new int[channels];
        // 채널마다 디코딩된 채널 번호가 범위 안인지 확인한다.
        for (int i = 0; i < channels; i++) {
            map[i] = d[off + 21 + i] & 0xFF;
            if (map[i] != 255 && map[i] >= streams + coupled) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the channel mapping of the Opus header refers to a missing channel");
            }
        }
        return new OpusHead(channels, preSkip, rate, gain, 1, streams, coupled, map);
    }

    /**
     * Returns the linear factor of the output gain.
     * <p>
     * 출력 이득의 선형 배율을 반환한다.
     *
     * @return the factor, 1.0 for no gain / 배율. 이득이 없으면 1.0
     */
    float gainFactor() {
        if (outputGainQ8 == 0) {
            return 1.0f;
        }
        return (float) Math.pow(10.0, outputGainQ8 / (20.0 * 256.0));
    }
}
