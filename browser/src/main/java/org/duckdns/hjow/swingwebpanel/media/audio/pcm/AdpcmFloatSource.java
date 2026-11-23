package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;

/**
 * The wide path of {@link AdpcmSource}: the decoded IMA, Microsoft and QuickTime ADPCM samples of a WAV or AIFF-C file as float samples
 * (<code>sample / 32768</code>, exact) in <em>every</em> channel of the file instead of the stereo mix of the 16 bit path (plan item P25-05). The
 * decoding is the one of the wrapped source, which is used only through its block decoder. The channels keep the order of the file.
 * <p>
 * {@link AdpcmSource} 의 넓은 경로: WAV 나 AIFF-C 파일의 IMA, Microsoft, QuickTime ADPCM 디코딩 표본을 16비트 경로의 스테레오 믹스 대신 파일의 <em>모든</em> 채널의
 * float 표본(<code>표본 / 32768</code>, 정확)으로 만든다(계획 P25-05). 디코딩은 감싼 소스의 것이며 그 소스는 블록 디코더로만 쓴다. 채널은 파일의 순서를 유지한다.
 */
final class AdpcmFloatSource extends BlockFloatSource {

    /**
     * The wrapped source, used for its block decoder.
     * <p>
     * 감싼 소스. 블록 디코더로 쓴다.
     */
    private final AdpcmSource inner;

    /**
     * The number of channels of the file.
     * <p>
     * 파일의 채널 수.
     */
    private final int channels;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param inner the ADPCM source whose block decoder is used / 블록 디코더를 쓸 ADPCM 소스
     */
    AdpcmFloatSource(AdpcmSource inner) {
        super(inner.getSampleRate(), inner.sourceChannels(), inner.getFrameCount(), inner.maxBlockFrames());
        this.inner = inner;
        this.channels = inner.sourceChannels();
    }

    /**
     * Decodes the next block with samples.
     * <p>
     * 샘플이 있는 다음 블록을 디코딩한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException if a block is damaged or the thread was interrupted / 블록이 손상되었거나 스레드가 인터럽트되었으면
     */
    protected int fill(float[][] out) throws MediaException {
        // 샘플이 없는 블록은 건너뛰며 다음 블록을 디코딩한다.
        for (;;) {
            int n = inner.decodeNextBlock();
            if (n < 0) {
                return -1;
            }
            if (n == 0) {
                continue;
            }
            short[] block = inner.blockSamples();
            // 프레임마다 채널의 16비트 값을 float 로 바꾼다.
            for (int f = 0; f < n; f++) {
                for (int c = 0; c < channels; c++) {
                    out[c][f] = block[f * channels + c] / 32768f;
                }
            }
            return n;
        }
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
