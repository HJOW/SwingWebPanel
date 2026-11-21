package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;

/**
 * The wide path of the MP3 decoder (plan item P25-05): the decoded floats of every frame, as the synthesis filter produces them, without the rounding to 16
 * bits and without the clipping. MPEG audio has one or two channels, so nothing is mixed in either path; the frames are decoded by the wrapped source
 * ({@link Mp3Source#decodeBlock()}), the same code that the 16 bit path uses, with the same start delay and end trim (the LAME gapless information), so
 * both paths give the same samples before the conversion.
 * <p>
 * MP3 디코더의 넓은 경로(계획 P25-05): 모든 프레임의 디코딩된 float 를 합성 필터가 만든 그대로 준다. 16비트 반올림도 클리핑도 없다. MPEG 오디오는 채널이 한두 개이므로 어느
 * 경로에서도 섞는 것이 없다. 프레임은 감싼 소스({@link Mp3Source#decodeBlock()})가 해독하며 16비트 경로가 쓰는 것과 같은 코드이고 시작 지연과 끝 자르기(LAME 의 갭리스
 * 정보)도 같으므로 두 경로는 변환 전에는 같은 표본을 준다.
 */
final class Mp3FloatSource extends BlockFloatSource {

    /**
     * Frames of the largest MPEG audio frame (Layer III, MPEG-1).
     * <p>
     * 가장 큰 MPEG 오디오 프레임(Layer III, MPEG-1)의 프레임 수.
     */
    private static final int MAX_FRAME_SAMPLES = 1152;

    /**
     * The wrapped source, used for its frame decoder.
     * <p>
     * 감싼 소스. 프레임 디코더로 쓴다.
     */
    private final Mp3Source inner;

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
    Mp3FloatSource(Mp3Source inner) {
        super(inner.getSampleRate(), inner.getChannels(), inner.getFrameCount(), MAX_FRAME_SAMPLES);
        this.inner = inner;
        this.channels = inner.getChannels();
    }

    /**
     * Decodes frames until one has output frames and copies them.
     * <p>
     * 출력 프레임이 있는 프레임이 나올 때까지 해독하고 복사한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException if the thread was interrupted / 스레드가 인터럽트되었으면
     */
    protected int fill(float[][] out) throws MediaException {
        // 시작 지연으로 모두 버려지는 프레임은 건너뛰며 다음 프레임을 해독한다.
        for (;;) {
            if (!inner.decodeBlock()) {
                return -1;
            }
            int usable = inner.blockUsable();
            if (usable == 0) {
                continue;
            }
            int from = inner.blockFrom();
            // 출력 프레임마다 채널의 표본을 복사한다.
            for (int i = 0; i < usable; i++) {
                for (int c = 0; c < channels; c++) {
                    out[c][i] = inner.sampleOf(c, from + i);
                }
            }
            return usable;
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
