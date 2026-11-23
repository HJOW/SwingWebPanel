package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;

/**
 * The {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource} of an AAC stream: it decodes one raw data block (1024 samples per channel) at a time from
 * a list of block positions (taken from ADTS headers or from the MP4 sample table). The priming samples at the start (edit list or encoder
 * delay) are dropped and the end is trimmed to the exact length when it is known. Seeking restarts decoding one block before the target to
 * rebuild the overlap, so the samples after a seek are the ones of continuous decoding.
 * <p>
 * AAC 스트림의 {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource}: 블록 위치 목록(ADTS 헤더 또는 MP4 샘플 표)에서 원시 데이터 블록(채널당 1024샘플)을
 * 하나씩 해독한다. 시작의 프라이밍 샘플(편집 목록이나 인코더 지연)은 버리고 길이를 알면 끝을 정확히 자른다. 탐색은 겹침을 복원하려고 목표 한 블록
 * 앞에서 해독을 다시 시작하므로 탐색 뒤의 샘플은 연속 해독과 같다.
 */
final class AacSource extends BlockPcmSource {

    /** Encoded data. / 인코딩된 데이터. */
    private final byte[] data;
    /** Start offset of every raw data block. / 원시 데이터 블록마다 시작 위치. */
    private final int[] offsets;
    /** Length of every raw data block. / 원시 데이터 블록마다 길이. */
    private final int[] lengths;
    /** Stream decoder. / 스트림 디코더. */
    private final AacFrameDecoder decoder;
    /** Output channels (1 or 2). / 출력 채널 수. */
    private final int channels;
    /** Samples dropped at the start. / 시작에서 버리는 샘플. */
    private final int startSkip;
    /** Total number of output frames after trimming, or -1. / 자른 뒤의 출력 프레임 총수. 모르면 -1. */
    private final long total;
    /** Decoded block [channel][1024]. / 해독한 블록. */
    private final float[][] block = new float[2][AacFrameDecoder.FRAME];
    /** Next block to decode. / 다음에 해독할 블록. */
    private int next;
    /** Output frames still to drop at the start or after a seek. / 시작이나 탐색 뒤에 더 버릴 출력 프레임. */
    private long drop;
    /** Output frames produced so far, counted from the logical start. / 논리적 시작부터 지금까지 만든 출력 프레임. */
    private long produced;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data      encoded data / 인코딩된 데이터
     * @param offsets   block offsets / 블록 위치
     * @param lengths   block lengths / 블록 길이
     * @param cfg       stream configuration / 스트림 구성
     * @param channels  output channels / 출력 채널 수
     * @param startSkip samples to drop at the start / 시작에서 버릴 샘플
     * @param total     number of output frames or -1 / 출력 프레임 수 또는 -1
     */
    AacSource(byte[] data, int[] offsets, int[] lengths, AacConfig cfg, int channels, int startSkip, long total) {
        super(cfg.sampleRate, channels, total, 2 * AacFrameDecoder.FRAME);
        this.data = data;
        this.offsets = offsets;
        this.lengths = lengths;
        this.channels = channels;
        this.startSkip = startSkip;
        this.total = total;
        this.decoder = new AacFrameDecoder(cfg.rateIndex, cfg.channelConfig);
        this.drop = startSkip;
    }

    /**
     * Decodes blocks until some output is available.
     * <p>
     * 출력이 생길 때까지 블록을 해독한다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written, or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException if the thread was interrupted / 스레드가 인터럽트되었으면
     */
    protected int fill(byte[] out) throws MediaException {
        while (true) { // 건너뛸 샘플을 지나 출력이 나올 때까지 블록을 해독한다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "decoding was interrupted");
            }
            if (next >= offsets.length || (total >= 0 && produced >= total)) {
                return -1;
            }
            int i = next++;
            try {
                decoder.decode(new AacBits(data, offsets[i], lengths[i]), block, channels);
            } catch (AacException e) {
                // 해독하지 못한 블록은 무음으로 처리한다.
                for (int c = 0; c < 2; c++) {
                    java.util.Arrays.fill(block[c], 0f);
                }
            } catch (RuntimeException e) {
                // 손상된 비트 스트림이 배열 범위를 벗어나는 값(예: 잘못된 대역 번호나 구간 길이)을 만들면 그 블록도 무음으로 처리한다.
                for (int c = 0; c < 2; c++) {
                    java.util.Arrays.fill(block[c], 0f);
                }
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
            int bi = 0;
            for (int n = 0; n < count; n++) { // 샘플을 16비트 PCM 으로 바꾼다
                for (int c = 0; c < channels; c++) {
                    int s = (int) Math.floor(block[c][from + n] * 32768.0f + 0.5f);
                    if (s > 32767) {
                        s = 32767;
                    } else if (s < -32768) {
                        s = -32768;
                    }
                    out[bi++] = (byte) s;
                    out[bi++] = (byte) (s >> 8);
                }
            }
            produced += count;
            return bi;
        }
    }

    /**
     * AAC streams are always seekable through the block list.
     * <p>
     * AAC 스트림은 블록 목록으로 항상 탐색할 수 있다.
     *
     * @return true / true
     */
    protected boolean canSeek() {
        return true;
    }

    /**
     * Restarts decoding one block before the target.
     * <p>
     * 목표 한 블록 앞에서 해독을 다시 시작한다.
     *
     * @param frame target frame / 목표 프레임
     * @return the frame position that the next read delivers / 다음 읽기가 주는 프레임 위치
     */
    protected long seekTo(long frame) {
        long absolute = frame + startSkip;
        int idx = (int) Math.min(offsets.length, absolute / AacFrameDecoder.FRAME);
        int start = Math.max(0, idx - 1);
        decoder.reset();
        next = start;
        drop = absolute - (long) start * AacFrameDecoder.FRAME;
        produced = frame;
        return frame;
    }
}
