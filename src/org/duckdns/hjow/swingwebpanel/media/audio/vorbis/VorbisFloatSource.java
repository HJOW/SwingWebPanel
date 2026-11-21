package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;

/**
 * The wide path of the Vorbis decoder (plan item P25-05): the decoded floats of every packet in <em>every</em> channel of the stream, as the decoder
 * produces them, without the rounding to 16 bits, without the clipping and without the stereo mix of the 16 bit path. The packets are decoded by the
 * wrapped source ({@link VorbisSource#nextBlock()}), the same code that the 16 bit path uses, so both paths give the same samples before the
 * conversion. <b>Channel order:</b> Vorbis I codes more than two channels in an order that differs from the speaker mask (section 4.3.9 of the
 * specification: 3 channels left, centre, right; 5.1 left, centre, right, back left, back right, LFE; 7.1 left, centre, right, side left, side right, back
 * left, back right, LFE). The channels are put in the canonical order (front left, right, centre, LFE, back left, right, side left, right), which is the order
 * of FFmpeg and of the Web Audio speaker layouts; where the layout is not defined (more than 8 channels) the order of the stream is kept.
 * <p>
 * Vorbis 디코더의 넓은 경로(계획 P25-05): 모든 패킷의 디코딩된 float 를 스트림의 <em>모든</em> 채널로, 디코더가 만든 그대로 준다. 16비트 반올림도, 클리핑도,
 * 16비트 경로의 스테레오 믹스도 없다. 패킷은 감싼 소스({@link VorbisSource#nextBlock()})가 디코딩하며 16비트 경로가 쓰는 것과 같은 코드이므로 두 경로는 변환 전에는
 * 같은 표본을 준다. <b>채널 순서:</b> Vorbis I 은 채널이 둘보다 많을 때 스피커 마스크와 다른 순서로 부호화한다(규격 4.3.9절: 3채널은 왼쪽, 중앙, 오른쪽. 5.1 은 왼쪽,
 * 중앙, 오른쪽, 뒤 왼쪽, 뒤 오른쪽, LFE. 7.1 은 왼쪽, 중앙, 오른쪽, 옆 왼쪽, 옆 오른쪽, 뒤 왼쪽, 뒤 오른쪽, LFE). 채널은 표준 순서(앞 왼쪽, 오른쪽, 중앙, LFE, 뒤 왼쪽, 오른쪽, 옆
 * 왼쪽, 오른쪽)로 바꾼다. 이것이 FFmpeg 와 Web Audio 스피커 배치의 순서이다. 배치가 정해지지 않은 곳(채널이 8개보다 많음)에서는 스트림의 순서를 유지한다.
 */
final class VorbisFloatSource extends BlockFloatSource {

    /**
     * The wrapped source, used for its packet decoder.
     * <p>
     * 감싼 소스. 패킷 디코더로 쓴다.
     */
    private final VorbisSource inner;

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
     * @param inner the 16 bit source whose packet decoder is used / 패킷 디코더를 쓸 16비트 소스
     */
    VorbisFloatSource(VorbisSource inner) {
        super(inner.getSampleRate(), inner.sourceChannels(), inner.getFrameCount(), inner.blockOutput()[0].length);
        this.inner = inner;
        int ch = inner.sourceChannels();
        this.order = ch > 2 ? ChannelMixer.forVorbisOrder(ch).canonicalOrder() : identity(ch);
    }

    /**
     * Returns the identity order.
     * <p>
     * 항등 순서를 반환한다.
     *
     * @param n the number of channels / 채널 수
     * @return 0, 1, ... n-1 / 0, 1, ... n-1
     */
    private static int[] identity(int n) {
        int[] o = new int[n];
        // 채널 번호를 그대로 넣는다.
        for (int i = 0; i < n; i++) {
            o[i] = i;
        }
        return o;
    }

    /**
     * Decodes packets until one gives output and copies it in the canonical channel order.
     * <p>
     * 출력이 나오는 패킷까지 디코딩하고 표준 채널 순서로 복사한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException CORRUPT for invalid packets / 패킷이 올바르지 않으면 CORRUPT
     */
    protected int fill(float[][] out) throws MediaException {
        int n = inner.nextBlock();
        if (n < 0) {
            return -1;
        }
        float[][] src = inner.blockOutput();
        // 출력 채널마다 스트림의 해당 채널을 복사한다.
        for (int c = 0; c < order.length; c++) {
            System.arraycopy(src[order[c]], 0, out[c], 0, n);
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
