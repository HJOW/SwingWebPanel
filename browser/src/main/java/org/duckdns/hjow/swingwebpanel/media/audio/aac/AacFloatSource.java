package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;

/**
 * The wide path of the AAC decoder (plan item P25-05): the decoded floats of every raw data block in <em>every</em> channel of the stream (1 to 8), as the
 * decoder produces them, without the rounding to 16 bits, without the clipping and without the stereo mix of the 16 bit path. The blocks are decoded by the
 * same frame decoder as in the 16 bit path ({@link AacFrameDecoder#decodeWide}), with the same start delay, end trim and silence for a block that cannot be
 * decoded, so both paths give the same samples before the conversion. <b>Channel order:</b> AAC codes the centre first (channel configuration 3: centre, left,
 * right; 5.1: centre, left, right, back left, back right, LFE); the channels are put in the canonical order (front left, right, centre, LFE, back left, right, side
 * left, right) of FFmpeg and of the Web Audio speaker layouts, using the speaker roles of the channel configuration.
 * <p>
 * AAC 디코더의 넓은 경로(계획 P25-05): 모든 원시 데이터 블록의 디코딩된 float 를 스트림의 <em>모든</em> 채널(1 ~ 8)로, 디코더가 만든 그대로 준다. 16비트 반올림도, 클리핑도, 16비트
 * 경로의 스테레오 믹스도 없다. 블록은 16비트 경로와 같은 프레임 디코더({@link AacFrameDecoder#decodeWide})가 디코딩하며 시작 지연, 끝 자르기, 디코딩할 수 없는 블록의 무음도 같으므로
 * 두 경로는 변환 전에는 같은 표본을 준다. <b>채널 순서:</b> AAC 는 중앙을 먼저 부호화한다(채널 구성 3: 중앙, 왼쪽, 오른쪽. 5.1: 중앙, 왼쪽, 오른쪽, 뒤 왼쪽, 뒤 오른쪽, LFE).
 * 채널 구성의 스피커 역할로 채널을 FFmpeg 와 Web Audio 스피커 배치의 표준 순서(앞 왼쪽, 오른쪽, 중앙, LFE, 뒤 왼쪽, 오른쪽, 옆 왼쪽, 오른쪽)로 바꾼다.
 */
final class AacFloatSource extends BlockFloatSource {

    /**
     * Indexed encoded blocks.
     * <p>
     * 색인된 인코딩 블록.
     */
    private final AacBlockInput input;

    /**
     * The frame decoder.
     * <p>
     * 프레임 디코더.
     */
    private final AacFrameDecoder decoder;

    /**
     * Total frames or -1.
     * <p>
     * 전체 프레임 수 또는 -1.
     */
    private final long total;

    /**
     * Decoded channels of one block.
     * <p>
     * 블록 하나의 디코딩된 채널.
     */
    private final float[][] block;

    /**
     * Next block to decode.
     * <p>
     * 다음에 디코딩할 블록.
     */
    private int next;

    /**
     * Frames still to drop before output starts (encoder delay).
     * <p>
     * 출력이 시작되기 전에 더 버릴 프레임(인코더 지연).
     */
    private long drop;

    /**
     * Frames delivered so far.
     * <p>
     * 지금까지 내보낸 프레임.
     */
    private long produced;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param input     indexed encoded blocks / 색인된 인코딩 블록
     * @param cfg       the audio configuration / 오디오 구성
     * @param channels  channels of the stream / 스트림의 채널 수
     * @param startSkip frames to drop at the start / 처음에 버릴 프레임
     * @param total     total frames or -1 / 전체 프레임 수 또는 -1
     */
    AacFloatSource(AacBlockInput input, AacConfig cfg, int channels, int startSkip, long total) {
        super(cfg.sampleRate, channels, total, AacFrameDecoder.FRAME);
        this.input = input;
        this.total = total;
        this.decoder = new AacFrameDecoder(cfg.rateIndex, cfg.channelConfig);
        this.block = new float[channels][AacFrameDecoder.FRAME];
        this.drop = startSkip;
    }

    /**
     * Decodes blocks until one has output frames and copies them.
     * <p>
     * 출력 프레임이 있는 블록이 나올 때까지 디코딩하고 복사한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException if interrupted or the input cannot be read / 인터럽트되었거나 입력을 읽을 수 없으면
     */
    protected int fill(float[][] out) throws MediaException {
        // 건너뛸 프레임을 지나 출력이 나올 때까지 블록을 해독한다.
        for (;;) {
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "decoding was interrupted");
            }
            if (next >= input.size() || (total >= 0 && produced >= total)) {
                return -1;
            }
            AacBits bits = input.read(next);
            next++;
            try {
                decoder.decodeWide(bits, block);
            } catch (AacException e) {
                // 해독하지 못한 블록은 무음으로 처리한다.
                silence();
            } catch (RuntimeException e) {
                // 손상된 비트 스트림이 배열 범위를 벗어나는 값을 만들면 그 블록도 무음으로 처리한다.
                silence();
            }
            int from = 0;
            if (drop > 0) {
                from = (int) Math.min(drop, AacFrameDecoder.FRAME);
                drop -= from;
            }
            int count = AacFrameDecoder.FRAME - from;
            if (total >= 0) {
                count = (int) Math.min(count, total - produced);
            }
            if (count <= 0) {
                continue;
            }
            // 채널마다 출력할 프레임을 복사한다.
            for (int c = 0; c < block.length; c++) {
                System.arraycopy(block[c], from, out[c], 0, count);
            }
            produced += count;
            return count;
        }
    }

    /**
     * Fills the block with silence.
     * <p>
     * 블록을 무음으로 채운다.
     */
    private void silence() {
        // 채널마다 0 으로 채운다.
        for (int c = 0; c < block.length; c++) {
            java.util.Arrays.fill(block[c], 0f);
        }
    }

    /** Closes the encoded input. / 인코딩 입력을 닫는다. */
    protected void release() {
        input.close();
    }
}
