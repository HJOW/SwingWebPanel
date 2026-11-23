package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockFloatSource;

/**
 * The wide path of {@link RawPcmSource}: the PCM, float and companded samples of a WAV, AIFF or AU file as float samples in every channel of the
 * file (plan item P25-05). An integer sample is exactly <code>sample / 2^(bits-1)</code> (8, 16, 24, 32 bits; the conversion of 24 bit
 * integers is exact in a float and a 32 bit one is rounded to the 24 bit mantissa of the float, which is the nearest float of the exact
 * quotient), a float sample is the sample itself (a 64 bit one rounded to a float), mu-law and A-law are the 16 bit values of G.711 divided
 * by 32768. The channels keep the order of the file, which for WAV is the order of the speaker mask and so is the canonical order.
 * <p>
 * {@link RawPcmSource} 의 넓은 경로: WAV, AIFF, AU 파일의 PCM, float, 압신 표본을 파일의 모든 채널을 가진 float 표본으로 만든다(계획 P25-05). 정수 표본은
 * 정확히 <code>표본 / 2^(비트-1)</code> 이다(8, 16, 24, 32비트. 24비트 정수의 변환은 float 에서 정확하고 32비트는 float 의 24비트 가수로 반올림되며 이는 정확한 몫에
 * 가장 가까운 float 이다). float 표본은 표본 그대로이다(64비트는 float 로 반올림). mu-law 와 A-law 는 G.711 의 16비트 값을 32768 로 나눈 값이다. 채널은 파일의 순서를
 * 유지하며 WAV 에서는 이것이 스피커 마스크의 순서이므로 표준 순서이다.
 */
final class RawFloatSource extends BlockFloatSource {

    /**
     * Frames per block.
     * <p>
     * 블록당 프레임 수.
     */
    private static final int BLOCK_FRAMES = 2048;

    /**
     * The file data.
     * <p>
     * 파일 데이터.
     */
    private final byte[] data;

    /**
     * Offset of the first sample.
     * <p>
     * 첫 샘플의 위치.
     */
    private final int start;

    /**
     * The sample encoding.
     * <p>
     * 샘플 인코딩.
     */
    private final SampleFormat format;

    /**
     * The number of channels.
     * <p>
     * 채널 수.
     */
    private final int channels;

    /**
     * Bytes of one frame.
     * <p>
     * 프레임 하나의 바이트 수.
     */
    private final int frameBytes;

    /**
     * Total frames.
     * <p>
     * 전체 프레임 수.
     */
    private final long frames;

    /**
     * Next frame.
     * <p>
     * 다음 프레임.
     */
    private long next;

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data       file data / 파일 데이터
     * @param start      offset of the first sample / 첫 샘플의 위치
     * @param end        end offset of the samples / 샘플의 끝 위치
     * @param format     sample encoding / 샘플 인코딩
     * @param channels   channels / 채널 수
     * @param sampleRate frames per second / 초당 프레임 수
     */
    RawFloatSource(byte[] data, int start, int end, SampleFormat format, int channels, int sampleRate) {
        super(sampleRate, channels, (long) (end - start) / ((long) format.getWidth() * channels), BLOCK_FRAMES);
        this.data = data;
        this.start = start;
        this.format = format;
        this.channels = channels;
        this.frameBytes = format.getWidth() * channels;
        this.frames = (long) (end - start) / frameBytes;
    }

    /**
     * Converts the next block.
     * <p>
     * 다음 블록을 변환한다.
     *
     * @param out one array per channel / 채널마다 배열 하나
     * @return frames written or -1 at the end / 쓴 프레임 수. 끝이면 -1
     * @throws MediaException CANCELLED if the thread was interrupted / 스레드가 인터럽트되었으면 CANCELLED
     */
    protected int fill(float[][] out) throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
        }
        int count = (int) Math.min(out[0].length, frames - next);
        if (count <= 0) {
            return -1;
        }
        int width = format.getWidth();
        // 프레임마다 채널의 샘플을 float 로 바꾼다.
        for (int f = 0; f < count; f++) {
            int pos = start + (int) ((next + f) * frameBytes);
            // 채널마다 처리한다.
            for (int c = 0; c < channels; c++) {
                out[c][f] = RawPcmSource.toFloat(format, data, pos + c * width);
            }
        }
        next += count;
        return count;
    }
}
