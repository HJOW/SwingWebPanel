package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The decoder of an Opus stream as an Ogg or WebM file carries it (RFC 7845 section 5.1.1): for the channel mapping family 0 one
 * mono or stereo Opus stream, for family 1 several mono and stereo streams whose packets are concatenated into one packet (all
 * but the last in the self-delimited framing of RFC 6716 appendix B) and whose decoded channels are assigned to the output
 * channels by a mapping table. The decoded samples are returned per output channel at 48 kHz, scaled by the output gain of the
 * header. Not thread safe.
 * <p>
 * Ogg 나 WebM 파일이 담는 Opus 스트림의 디코더(RFC 7845 5.1.1): 채널 매핑 군 0 에서는 모노 또는 스테레오 Opus 스트림 하나, 군 1 에서는 모노와
 * 스테레오 스트림 여러 개의 패킷을 하나의 패킷으로 이어 붙이고(마지막을 제외하고는 RFC 6716 부록 B 의 자체 구분 프레이밍) 디코딩한 채널을 매핑
 * 표로 출력 채널에 배정한다. 디코딩한 샘플은 헤더의 출력 이득을 곱해 출력 채널별 48 kHz 로 돌려준다. 스레드에 안전하지 않다.
 */
final class OpusMultistream {

    /**
     * The identification header.
     * <p>
     * 식별 헤더.
     */
    private final OpusHead head;

    /**
     * The decoders of the streams (the coupled ones first).
     * <p>
     * 스트림의 디코더(결합 스트림이 먼저).
     */
    private final OpusFrameDecoder[] decoders;

    /**
     * Decoded samples of each stream, interleaved.
     * <p>
     * 스트림별 디코딩한 샘플(인터리브).
     */
    private final float[][] streamPcm;

    /**
     * Linear factor of the output gain.
     * <p>
     * 출력 이득의 선형 배율.
     */
    private final float gain;

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param head the identification header / 식별 헤더
     */
    OpusMultistream(OpusHead head) {
        this.head = head;
        this.decoders = new OpusFrameDecoder[head.streamCount];
        this.streamPcm = new float[head.streamCount][OpusPacket.MAX_PACKET_SAMPLES * 2];
        // 결합 스트림은 스테레오 디코더, 나머지는 모노 디코더를 쓴다.
        for (int s = 0; s < head.streamCount; s++) {
            decoders[s] = new OpusFrameDecoder(s < head.coupledCount ? 2 : 1);
        }
        this.gain = head.gainFactor();
    }

    /**
     * Resets all decoders to the state of new decoders.
     * <p>
     * 모든 디코더를 새 디코더의 상태로 되돌린다.
     */
    void reset() {
        // 스트림마다 리셋한다.
        for (int s = 0; s < decoders.length; s++) {
            decoders[s].reset();
        }
    }

    /**
     * Returns the number of output channels.
     * <p>
     * 출력 채널 수를 반환한다.
     *
     * @return the channel count / 채널 수
     */
    int getChannels() {
        return head.channels;
    }

    /**
     * Decodes a packet into the output channels, or conceals a lost packet.
     * <p>
     * 패킷을 출력 채널로 디코딩하거나 손실된 패킷을 은닉한다.
     *
     * @param data    the buffer, or null for a lost packet / 버퍼. 손실된 패킷이면 null
     * @param off     start of the packet / 패킷의 시작
     * @param len     size of the packet / 패킷의 크기
     * @param lostLen number of samples per channel to conceal when the packet is lost (at least 120) / 패킷이 손실되었을 때 은닉할 채널당 샘플 수(120 이상)
     * @param out     receives the samples, one array per output channel (each of at least 5760 samples) / 출력 채널마다 샘플을 받는 배열(각각 5760 샘플 이상)
     * @return the number of samples per channel, or a negative error code / 채널당 샘플 수. 오류면 음수 코드
     */
    int decode(byte[] data, int off, int len, int lostLen, float[][] out) {
        int n = -1;
        int pos = off;
        int end = off + len;
        // 스트림마다 자기 패킷을 디코딩한다(마지막을 제외하고는 자체 구분 프레이밍).
        for (int s = 0; s < decoders.length; s++) {
            int r;
            if (data == null || len == 0) {
                r = decoders[s].decodeLost(streamPcm[s], 0, lostLen);
            } else {
                OpusPacket p = s < decoders.length - 1 ? OpusPacket.parseSelfDelimited(data, pos, end - pos) : OpusPacket.parse(data, pos, end - pos);
                if (p == null) {
                    return OpusFrameDecoder.ERR_INVALID_PACKET;
                }
                r = decoders[s].decodeParsed(data, p, streamPcm[s], 0, OpusPacket.MAX_PACKET_SAMPLES);
                pos += p.length;
            }
            if (r < 0) {
                return r;
            }
            if (n >= 0 && r != n) {
                return OpusFrameDecoder.ERR_INVALID_PACKET;
            }
            n = r;
        }
        // 출력 채널마다 매핑 표에 따라 디코딩한 채널을 골라 이득을 곱해 복사한다.
        for (int c = 0; c < head.channels; c++) {
            int idx = head.mapping[c];
            float[] dst = out[c];
            if (idx == 255) {
                java.util.Arrays.fill(dst, 0, n, 0.f);
                continue;
            }
            int stream;
            int chan;
            int width;
            if (idx < 2 * head.coupledCount) {
                stream = idx / 2;
                chan = idx % 2;
                width = 2;
            } else {
                stream = head.coupledCount + (idx - 2 * head.coupledCount);
                chan = 0;
                width = 1;
            }
            float[] src = streamPcm[stream];
            // 샘플마다 채널을 꺼내 이득을 곱한다.
            for (int i = 0; i < n; i++) {
                dst[i] = src[i * width + chan] * gain;
            }
        }
        return n;
    }
}
