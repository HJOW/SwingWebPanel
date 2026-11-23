package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.BlockPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;

/**
 * Base of the FLAC sources: owns the frame decoder and converts a decoded frame to 16-bit output (samples are rounded to 16 bits without
 * dither; more than two channels are mixed down with the FLAC speaker layout, which equals the default layout of {@link ChannelMixer}).
 * <p>
 * FLAC 소스의 기반 클래스: 프레임 디코더를 가지고 있으며 디코딩된 프레임을 16비트 출력으로 바꾼다(샘플은 디더 없이 16비트로 반올림하고, 채널이
 * 둘보다 많으면 {@link ChannelMixer} 의 기본 배치와 같은 FLAC 스피커 배치로 다운믹스한다).
 */
abstract class FlacPcmSource extends BlockPcmSource {

    /**
     * The stream parameters.
     * <p>
     * 스트림 파라미터.
     */
    protected final FlacStreamInfo info;

    /**
     * The frame decoder.
     * <p>
     * 프레임 디코더.
     */
    protected final FlacFrameDecoder decoder;

    /**
     * Mixer for more than two channels, else null.
     * <p>
     * 채널이 둘보다 많을 때의 믹서. 아니면 null.
     */
    private final ChannelMixer mixer;

    /**
     * Scratch for one input frame.
     * <p>
     * 입력 프레임 하나의 작업 공간.
     */
    private final float[] frameBuf;

    /**
     * Scratch for the mixed frame.
     * <p>
     * 섞은 프레임의 작업 공간.
     */
    private final float[] mixed = new float[2];

    /**
     * Creates the source.
     * <p>
     * 소스를 만든다.
     *
     * @param info       stream parameters / 스트림 파라미터
     * @param frameCount total frames or -1 / 전체 프레임 수 또는 -1
     */
    FlacPcmSource(FlacStreamInfo info, long frameCount) {
        super(info.sampleRate, Math.min(info.channels, 2), frameCount, info.maxBlock >= 16 ? info.maxBlock : 65535);
        this.info = info;
        this.decoder = new FlacFrameDecoder(info);
        this.mixer = info.channels > 2 ? ChannelMixer.forChannels(info.channels) : null;
        this.frameBuf = new float[info.channels];
    }

    /**
     * Decodes the next audio frame of the stream into {@link #decoder} (its <code>samples</code> hold the decoded channels), or returns null at the
     * end. Both the 16 bit path ({@link #fill(byte[])} converts what this returns) and the wide path ({@link FlacFloatSource}) read frames through
     * this method.
     * <p>
     * 스트림의 다음 오디오 프레임을 {@link #decoder} 로 디코딩하고(<code>samples</code> 에 디코딩된 채널이 있다) 끝이면 null 을 반환한다. 16비트 경로
     * ({@link #fill(byte[])} 가 이 메서드가 반환한 것을 변환한다)와 넓은 경로({@link FlacFloatSource})가 모두 이 메서드로 프레임을 읽는다.
     *
     * @return the header of the decoded frame, or null at the end of the stream / 디코딩한 프레임의 헤더. 스트림 끝이면 null
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    abstract FlacFrameDecoder.Header nextFrame() throws MediaException;

    /**
     * Decodes the next frame and writes it as 16 bit PCM.
     * <p>
     * 다음 프레임을 디코딩해 16비트 PCM 으로 쓴다.
     *
     * @param out output buffer / 출력 버퍼
     * @return bytes written or -1 at the end / 쓴 바이트 수. 끝이면 -1
     * @throws MediaException if the data is damaged / 데이터가 손상되었으면
     */
    protected final int fill(byte[] out) throws MediaException {
        FlacFrameDecoder.Header h = nextFrame();
        return h == null ? -1 : emit(h, out);
    }

    /**
     * Converts the frame that was just decoded to output PCM.
     * <p>
     * 방금 디코딩한 프레임을 출력 PCM 으로 바꾼다.
     *
     * @param h   the frame header / 프레임 헤더
     * @param out output buffer / 출력 버퍼
     * @return bytes written / 쓴 바이트 수
     * @throws MediaException CORRUPT if the frame is larger than the buffer prepared from STREAMINFO / 프레임이 STREAMINFO 로 준비한 버퍼보다 크면 CORRUPT
     */
    protected final int emit(FlacFrameDecoder.Header h, byte[] out) throws MediaException {
        int outCh = getChannels();
        if ((long) h.blockSize * outCh * 2 > out.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "FLAC frame is larger than the maximum block size");
        }
        int o = 0;
        int bps = h.bps;
        // 프레임의 샘플을 하나씩 16비트로 바꿔 쓴다.
        for (int i = 0; i < h.blockSize; i++) {
            if (mixer == null) {
                // 채널마다 샘플을 16비트로 바꿔 쓴다.
                for (int c = 0; c < outCh; c++) {
                    short s = PcmConverter.narrow(decoder.samples[c][i], bps);
                    out[o++] = (byte) s;
                    out[o++] = (byte) (s >> 8);
                }
            } else {
                double scale = 1.0 / (double) (1L << (bps - 1));
                // 채널마다 샘플을 -1..1 범위의 float 로 바꾼다.
                for (int c = 0; c < info.channels; c++) {
                    frameBuf[c] = (float) (decoder.samples[c][i] * scale);
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
        return o;
    }
}
