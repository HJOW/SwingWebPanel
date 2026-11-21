package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;

/**
 * The wide path of the Opus decoder (plan item P25-05): the decoded floats of every packet in <em>every</em> channel of the stream at 48 kHz, as the
 * decoder produces them (the start trimmed by the pre-skip and the end by the last granule position as in the 16 bit path), without the rounding to 16 bits,
 * without the clipping and without the stereo mix. The packets are decoded by the wrapped source ({@link OpusSource#nextBlock()}), the same code that the 16 bit path
 * uses. <b>Channel order:</b> the channel mapping family 1 (1 to 8 channels) uses the order of Vorbis (RFC 7845 section 5.1.1.2), which differs from the speaker mask
 * for more than two channels; it is put in the canonical order (front left, right, centre, LFE, back left, right, side left, right) of FFmpeg and of the Web Audio
 * speaker layouts. The other families (0 with one or two channels, 2 and 255 which are not speaker layouts) keep the order of the stream.
 * <p>
 * Opus 디코더의 넓은 경로(계획 P25-05): 모든 패킷의 디코딩된 float 를 48 kHz 에서 스트림의 <em>모든</em> 채널로, 디코더가 만든 그대로 준다(16비트 경로처럼 시작은 사전 건너뜀으로,
 * 끝은 마지막 그래뉼 위치로 자른다). 16비트 반올림도, 클리핑도, 스테레오 믹스도 없다. 패킷은 감싼 소스({@link OpusSource#nextBlock()})가 디코딩하며 16비트 경로가 쓰는 것과 같은
 * 코드이다. <b>채널 순서:</b> 채널 매핑 군 1(1 ~ 8채널)은 Vorbis 의 순서(RFC 7845 5.1.1.2절)를 쓰며 이는 채널이 둘보다 많으면 스피커 마스크와 다르다. FFmpeg 와 Web Audio 스피커 배치의
 * 표준 순서(앞 왼쪽, 오른쪽, 중앙, LFE, 뒤 왼쪽, 오른쪽, 옆 왼쪽, 오른쪽)로 바꾼다. 다른 군(채널 한두 개인 0, 스피커 배치가 아닌 2 와 255)은 스트림의 순서를 유지한다.
 */
final class OpusFloatSource extends BlockFloatSource {

    /**
     * The wrapped source, used for its packet decoder.
     * <p>
     * 감싼 소스. 패킷 디코더로 쓴다.
     */
    private final OpusSource inner;

    /**
     * For every output channel the index of the channel of the stream.
     * <p>
     * 출력 채널마다 스트림의 채널 인덱스.
     */
    private final int[] order;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param inner    the 16 bit source whose packet decoder is used / 패킷 디코더를 쓸 16비트 소스
     * @param channels the channels of every link (see {@link OpusSource#uniformChannels()}) / 모든 고리의 채널 수({@link OpusSource#uniformChannels()} 참조)
     */
    OpusFloatSource(OpusSource inner, int channels) {
        super(OpusFrameDecoder.FS, channels, inner.getFrameCount(), OpusPacket.MAX_PACKET_SAMPLES);
        this.inner = inner;
        int[] o = new int[channels];
        // 처음에는 스트림의 순서 그대로이다.
        for (int i = 0; i < channels; i++) {
            o[i] = i;
        }
        // 매핑 군 1 은 Vorbis 순서이므로 표준 순서로 바꾼다.
        if (channels > 2 && inner.family() == 1) {
            o = ChannelMixer.forVorbisOrder(channels).canonicalOrder();
        }
        this.order = o;
    }

    /**
     * Decodes packets until one gives output and copies it in the canonical channel order.
     * <p>
     * 출력이 나오는 패킷까지 디코딩하고 표준 채널 순서로 복사한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException CORRUPT for a stream that cannot be decoded / 디코딩할 수 없는 스트림이면 CORRUPT
     */
    protected int fill(float[][] out) throws MediaException {
        int n = inner.nextBlock();
        if (n < 0) {
            return -1;
        }
        float[][] src = inner.blockChannels();
        int from = inner.blockFrom();
        // 출력 채널마다 스트림의 해당 채널을 복사한다.
        for (int c = 0; c < order.length; c++) {
            System.arraycopy(src[order[c]], from, out[c], 0, n);
        }
        return n;
    }

    /**
     * Closes the wrapped source.
     * <p>
     * 감싼 소스를 닫는다.
     */
    protected void release() {
        inner.close();
    }
}
