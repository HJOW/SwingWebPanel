package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;

/**
 * A {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource} over interleaved samples that are stored in the encoded data itself (WAV, AIFF, AU): the
 * samples are converted to 16 bits when they are read, so nothing is decoded in advance. The data range may end in the middle of a frame
 * (a truncated file): the incomplete last frame is ignored and the stream simply ends. More than two channels are mixed down by a
 * {@link ChannelMixer}.
 * <p>
 * 인코딩된 데이터 안에 그대로 들어 있는 인터리브 샘플(WAV, AIFF, AU)에 대한 {@link org.duckdns.hjow.swingwebpanel.media.audio.PcmSource}: 샘플은 읽을 때 16비트로
 * 변환하므로 미리 디코딩하는 것이 없다. 데이터 범위가 프레임 중간에서 끝나도(잘린 파일) 불완전한 마지막 프레임은 무시하고 스트림이 그냥 끝난다.
 * 채널이 둘보다 많으면 {@link ChannelMixer} 로 다운믹스한다.
 */
public final class RawPcmSource extends BlockPcmSource {

    /**
     * Frames converted per block.
     * <p>
     * 블록마다 변환하는 프레임 수.
     */
    private static final int BLOCK_FRAMES = 2048;

    /**
     * The encoded data.
     * <p>
     * 인코딩된 데이터.
     */
    private final byte[] data;

    /**
     * Offset of the first sample byte.
     * <p>
     * 첫 샘플 바이트의 위치.
     */
    private final int start;

    /**
     * Sample encoding.
     * <p>
     * 샘플 인코딩.
     */
    private final SampleFormat format;

    /**
     * Channels in the data.
     * <p>
     * 데이터의 채널 수.
     */
    private final int inChannels;

    /**
     * Bytes of one input frame.
     * <p>
     * 입력 프레임 하나의 바이트 수.
     */
    private final int inFrameBytes;

    /**
     * Number of whole frames in the data range.
     * <p>
     * 데이터 범위 안의 완전한 프레임 수.
     */
    private final long frames;

    /**
     * Mixer for more than two channels, else null.
     * <p>
     * 채널이 둘보다 많을 때의 믹서. 아니면 null.
     */
    private final ChannelMixer mixer;

    /**
     * Index of the next frame to convert.
     * <p>
     * 다음에 변환할 프레임 번호.
     */
    private long next;

    /**
     * Scratch for one input frame (only with a mixer).
     * <p>
     * 입력 프레임 하나의 작업 공간(믹서가 있을 때만).
     */
    private final float[] frameBuf;

    /**
     * Scratch for the mixed frame (only with a mixer).
     * <p>
     * 섞은 프레임의 작업 공간(믹서가 있을 때만).
     */
    private final float[] mixed = new float[2];

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param data       encoded data / 인코딩된 데이터
     * @param start      offset of the first sample byte / 첫 샘플 바이트의 위치
     * @param end        end offset (exclusive) of the sample bytes / 샘플 바이트의 끝 위치(미포함)
     * @param format     sample encoding / 샘플 인코딩
     * @param channels   channels in the data (at least 1) / 데이터의 채널 수(1 이상)
     * @param sampleRate sample rate / 샘플링 주파수
     * @param mixer      mixer for more than two channels, may be null (then the default layout is used) / 채널이 둘보다 많을 때의 믹서. null 이면 기본 배치를 쓴다
     */
    public RawPcmSource(byte[] data, int start, int end, SampleFormat format, int channels, int sampleRate, ChannelMixer mixer) {
        super(sampleRate, Math.min(channels, 2), (long) (end - start) / ((long) format.getWidth() * channels), BLOCK_FRAMES);
        this.data = data;
        this.start = start;
        this.format = format;
        this.inChannels = channels;
        this.inFrameBytes = format.getWidth() * channels;
        this.frames = (long) (end - start) / inFrameBytes;
        this.mixer = channels > 2 ? (mixer != null ? mixer : ChannelMixer.forChannels(channels)) : null;
        this.frameBuf = new float[channels];
    }

    /**
     * Converts the next block of frames.
     * <p>
     * 다음 프레임 블록을 변환한다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException if the thread was interrupted / 스레드가 인터럽트되었으면
     */
    protected int fill(byte[] out) throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
        }
        int outCh = getChannels();
        int maxFrames = out.length / (outCh * 2);
        long count = Math.min(maxFrames, frames - next);
        if (count <= 0) {
            return -1;
        }
        int o = 0;
        // 프레임마다 샘플을 읽어 16비트로 바꿔 쓴다.
        for (long f = 0; f < count; f++) {
            int pos = start + (int) ((next + f) * inFrameBytes);
            if (inChannels <= 2) {
                // 채널마다 16비트 샘플로 바꿔 쓴다.
                for (int c = 0; c < inChannels; c++) {
                    short s = sample16(pos + c * format.getWidth());
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            } else {
                // 채널마다 float 샘플을 읽어 믹서에 넣을 준비를 한다.
                for (int c = 0; c < inChannels; c++) {
                    frameBuf[c] = sampleFloat(pos + c * format.getWidth());
                }
                mixer.mix(frameBuf, mixed);
                // 믹스한 두 채널을 16비트로 쓴다.
                for (int c = 0; c < 2; c++) {
                    short s = PcmConverter.clip16(mixed[c]);
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            }
        }
        next += count;
        return o;
    }

    /**
     * Returns whether this source can seek.
     * <p>
     * 이 소스가 탐색할 수 있는지 반환한다.
     *
     * @return always true / 항상 true
     */
    protected boolean canSeek() {
        return true;
    }

    /**
     * Jumps to a frame (exact).
     * <p>
     * 프레임으로 이동한다(정확하다).
     *
     * @param frame frame index / 프레임 번호
     * @return the same frame / 같은 프레임
     */
    protected long seekTo(long frame) {
        next = Math.min(frame, frames);
        return next;
    }

    /**
     * Converts the sample at an offset to a 16-bit value.
     * <p>
     * 위치의 샘플을 16비트 값으로 바꾼다.
     *
     * @param p offset of the sample / 샘플의 위치
     * @return 16-bit sample / 16비트 샘플
     */
    private short sample16(int p) {
        byte[] d = data;
        switch (format) {
        case U8:
            return (short) (((d[p] & 0xFF) - 128) << 8);
        case S8:
            return (short) (d[p] << 8);
        case S16LE:
            return (short) ((d[p] & 0xFF) | (d[p + 1] << 8));
        case S16BE:
            return (short) ((d[p] << 8) | (d[p + 1] & 0xFF));
        case ULAW:
            return PcmConverter.ulaw(d[p]);
        case ALAW:
            return PcmConverter.alaw(d[p]);
        default:
            return PcmConverter.clip16(sampleFloat(p));
        }
    }

    /**
     * Converts the sample at an offset to a full-scale float.
     * <p>
     * 위치의 샘플을 풀 스케일 float 로 바꾼다.
     *
     * @param p offset of the sample / 샘플의 위치
     * @return sample, 1.0 is full scale / 샘플. 1.0 이 풀 스케일
     */
    private float sampleFloat(int p) {
        byte[] d = data;
        switch (format) {
        case U8:
            return ((d[p] & 0xFF) - 128) / 128f;
        case S8:
            return d[p] / 128f;
        case S16LE:
            return ((short) ((d[p] & 0xFF) | (d[p + 1] << 8))) / 32768f;
        case S16BE:
            return ((short) ((d[p] << 8) | (d[p + 1] & 0xFF))) / 32768f;
        case S24LE:
            return ((d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8) | (d[p + 2] << 16)) / 8388608f;
        case S24BE:
            return ((d[p + 2] & 0xFF) | ((d[p + 1] & 0xFF) << 8) | (d[p] << 16)) / 8388608f;
        case S32LE:
            return ((d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8) | ((d[p + 2] & 0xFF) << 16) | (d[p + 3] << 24)) / 2147483648f;
        case S32BE:
            return ((d[p + 3] & 0xFF) | ((d[p + 2] & 0xFF) << 8) | ((d[p + 1] & 0xFF) << 16) | (d[p] << 24)) / 2147483648f;
        case F32LE:
            return Float.intBitsToFloat((d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8) | ((d[p + 2] & 0xFF) << 16) | (d[p + 3] << 24));
        case F32BE:
            return Float.intBitsToFloat((d[p + 3] & 0xFF) | ((d[p + 2] & 0xFF) << 8) | ((d[p + 1] & 0xFF) << 16) | (d[p] << 24));
        case F64LE:
            return (float) Double.longBitsToDouble(le64(d, p));
        case F64BE:
            return (float) Double.longBitsToDouble(be64(d, p));
        case ULAW:
            return PcmConverter.ulaw(d[p]) / 32768f;
        case ALAW:
            return PcmConverter.alaw(d[p]) / 32768f;
        default:
            return 0f;
        }
    }

    /**
     * Reads a little-endian long.
     * <p>
     * 리틀 엔디언 long 을 읽는다.
     *
     * @param d array / 배열
     * @param p offset / 위치
     * @return value / 값
     */
    private static long le64(byte[] d, int p) {
        long v = 0;
        // 높은 바이트부터 채워 8바이트를 조립한다.
        for (int i = 7; i >= 0; i--) {
            v = (v << 8) | (d[p + i] & 0xFFL);
        }
        return v;
    }

    /**
     * Reads a big-endian long.
     * <p>
     * 빅 엔디언 long 을 읽는다.
     *
     * @param d array / 배열
     * @param p offset / 위치
     * @return value / 값
     */
    private static long be64(byte[] d, int p) {
        long v = 0;
        // 앞 바이트부터 채워 8바이트를 조립한다.
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (d[p + i] & 0xFFL);
        }
        return v;
    }
}
