package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacket;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacketStream;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggReader;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Decoder of Ogg Vorbis (plan item P18-07, FEATURE_MATRIX AUD-06): Vorbis I with its floors 0 and 1, residues 0, 1 and 2, channel
 * coupling, both block sizes and any number of channels (more than two are mixed down to stereo). The first Vorbis stream of the Ogg
 * file is played: other streams of a multiplexed file are ignored, and in a chained file only the first link plays (the sample rate
 * and the channels may change from link to link, which one PCM stream cannot express).
 * <p>
 * Ogg Vorbis 디코더(계획 P18-07, FEATURE_MATRIX AUD-06): Vorbis I 의 플로어 0/1, 잔차 0/1/2, 채널 결합, 두 블록 크기, 임의의 채널 수(둘보다 많으면
 * 스테레오로 다운믹스). Ogg 파일의 첫 Vorbis 스트림을 재생한다. 다중화 파일의 다른 스트림은 무시하고, 체인 파일은 첫 고리만 재생한다(고리마다 샘플링
 * 주파수와 채널 수가 바뀔 수 있는데 하나의 PCM 스트림으로는 표현할 수 없다).
 */
public final class VorbisDecoder implements AudioDecoder {

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public VorbisDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "vorbis"
     */
    public String getFormatName() {
        return "vorbis";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/ogg", "application/ogg", "audio/vorbis" };
    }

    /**
     * Recognizes an Ogg file that starts a Vorbis stream.
     * <p>
     * Vorbis 스트림으로 시작하는 Ogg 파일을 알아본다.
     *
     * @param data the data / 데이터
     * @return true if the data is Ogg Vorbis / Ogg Vorbis 이면 true
     */
    public boolean accepts(byte[] data) {
        return OggReader.startsWithOgg(data) && OggReader.sniffCodecs(data).contains("vorbis");
    }

    /**
     * Opens the data for decoding.
     * <p>
     * 디코딩을 위해 데이터를 연다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @return PCM source / PCM 소스
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        OggReader reader = OggReader.scan(data);
        OggStream stream = reader.findFirst("vorbis");
        if (stream == null) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "no Vorbis stream in the Ogg file");
        }
        OggPacketStream packets = stream.openPackets(0);
        OggPacket id = packets.next();
        OggPacket comment = packets.next();
        OggPacket setupPacket = packets.next();
        if (id == null || comment == null || setupPacket == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the Vorbis stream has fewer than three header packets");
        }
        byte[] c = comment.getData();
        if (c.length < 7 || c[0] != 3) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis comment header");
        }
        VorbisSetup setup = new VorbisSetup(id.getData());
        setup.parseSetup(setupPacket.getData());
        return new VorbisSource(stream, setup, setupPacket.getPageIndex() + 1);
    }
}
