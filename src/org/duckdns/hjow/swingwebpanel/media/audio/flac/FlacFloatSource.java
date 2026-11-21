package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;

/**
 * The wide path of the FLAC decoder (plan item P25-05): every decoded frame as float samples in <em>every</em> channel of the file with the full bit
 * depth of the stream. A sample of an N bit stream is exactly <code>sample / 2^(N-1)</code> (the integer of the decoder divided by a power of two in
 * double arithmetic and rounded to a float, which is the nearest float of the quotient: exact for 24 bits and less). The frames come from the
 * block decoder of the wrapped source ({@link FlacPcmSource#nextFrame()}), the same code that the 16 bit path uses, so both paths accept the same
 * streams. The channels keep the order of the file: FLAC defines it for 1 to 8 channels as the order of the speaker mask (front left,
 * right, centre, LFE, back left, right, then side left, right where there are eight).
 * <p>
 * FLAC 디코더의 넓은 경로(계획 P25-05): 디코딩한 모든 프레임을 스트림의 전체 비트 깊이로 파일의 <em>모든</em> 채널을 가진 float 표본으로 만든다. N비트 스트림의
 * 표본은 정확히 <code>표본 / 2^(N-1)</code> 이다(디코더의 정수를 double 연산으로 2 의 거듭제곱으로 나누어 float 로 반올림한 값으로, 몫에 가장 가까운 float 이며 24비트
 * 이하에서는 정확하다). 프레임은 감싼 소스의 블록 디코더({@link FlacPcmSource#nextFrame()})에서 오며 16비트 경로가 쓰는 것과 같은 코드이므로 두 경로가 같은 스트림을
 * 받아들인다. 채널은 파일의 순서를 유지한다: FLAC 은 1 ~ 8 채널에서 이를 스피커 마스크의 순서(앞 왼쪽, 오른쪽, 중앙, LFE, 뒤 왼쪽, 오른쪽, 여덟 채널이면 이어서 옆 왼쪽,
 * 오른쪽)로 정의한다.
 */
final class FlacFloatSource extends BlockFloatSource {

    /**
     * The wrapped source, used for its frame decoder.
     * <p>
     * 감싼 소스. 프레임 디코더로 쓴다.
     */
    private final FlacPcmSource inner;

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param inner the 16 bit source whose frame decoder is used / 프레임 디코더를 쓸 16비트 소스
     */
    FlacFloatSource(FlacPcmSource inner) {
        super(inner.info.sampleRate, inner.info.channels, inner.getFrameCount(), inner.info.maxBlock >= 16 ? inner.info.maxBlock : 65535);
        this.inner = inner;
        this.channels = inner.info.channels;
    }

    /**
     * Decodes the next frame and converts it.
     * <p>
     * 다음 프레임을 디코딩해 변환한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException CORRUPT if the frame is damaged or larger than STREAMINFO allows / 프레임이 손상되었거나 STREAMINFO 가 허용하는 것보다 크면 CORRUPT
     */
    protected int fill(float[][] out) throws MediaException {
        FlacFrameDecoder.Header h = inner.nextFrame();
        if (h == null) {
            return -1;
        }
        if (h.blockSize > out[0].length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "FLAC frame is larger than the maximum block size");
        }
        double scale = 1.0 / (double) (1L << (h.bps - 1));
        // 채널마다 디코딩한 정수를 2 의 거듭제곱으로 나누어 float 로 만든다.
        for (int c = 0; c < channels; c++) {
            long[] s = inner.decoder.samples[c];
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < h.blockSize; i++) {
                out[c][i] = (float) (s[i] * scale);
            }
        }
        return h.blockSize;
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
