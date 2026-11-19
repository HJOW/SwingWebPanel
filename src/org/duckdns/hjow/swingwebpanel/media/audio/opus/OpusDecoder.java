package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggReader;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Decoder of Ogg Opus (plan item P18-07, FEATURE_MATRIX AUD-08), a pure Java implementation of RFC 6716 (the SILK, CELT and hybrid
 * modes with all frame sizes from 2.5 to 60 ms, the four frame packing codes, mono and stereo, redundancy frames, packet loss
 * concealment) and of the Ogg encapsulation of RFC 7845 (header packets, pre-skip, output gain, channel mapping families 0 and 1
 * with up to eight channels, chained streams, trimming of the last page). The data is recognized by the <code>OpusHead</code>
 * signature of the first packet of a logical stream and not by the file name or the MIME type. The output is 16-bit PCM at 48 kHz;
 * more than two channels are mixed down to stereo. Opus in WebM/Matroska is decoded through {@link org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmDecoder}.
 * <p>
 * Ogg Opus 디코더(계획 P18-07, FEATURE_MATRIX AUD-08). RFC 6716(2.5~60 ms 모든 프레임 크기의 SILK, CELT, 하이브리드 모드, 프레임 묶음 코드 넷,
 * 모노와 스테레오, 중복 프레임, 패킷 손실 은닉)과 RFC 7845 의 Ogg 캡슐화(헤더 패킷, 사전 건너뛰기, 출력 이득, 최대 8채널의 채널 매핑 군 0 과 1,
 * 체인 스트림, 마지막 페이지 자르기)를 순수 Java 로 구현했다. 파일 이름이나 MIME 타입이 아니라 논리 스트림 첫 패킷의 <code>OpusHead</code>
 * 시그니처로 알아본다. 출력은 48 kHz 16비트 PCM 이며 채널이 둘보다 많으면 스테레오로 다운믹스한다. WebM/Matroska 안의 Opus 는
 * {@link org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmDecoder} 를 통해 디코딩한다.
 */
public final class OpusDecoder implements AudioDecoder {

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public OpusDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "opus"
     */
    public String getFormatName() {
        return "opus";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/ogg", "audio/opus", "application/ogg" };
    }

    /**
     * Recognizes an Ogg file that contains an Opus stream.
     * <p>
     * Opus 스트림이 있는 Ogg 파일을 알아본다.
     *
     * @param data the data / 데이터
     * @return true if the data is Ogg Opus / Ogg Opus 이면 true
     */
    public boolean accepts(byte[] data) {
        return OggReader.startsWithOgg(data) && OggReader.sniffCodecs(data).contains("opus");
    }

    /**
     * Opens the data for decoding.
     * <p>
     * 디코딩을 위해 데이터를 연다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @return PCM source / PCM 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        OggReader reader = OggReader.scan(data);
        List<OggStream> streams = new ArrayList<OggStream>();
        // Opus 논리 스트림만 모은다(다른 코덱의 스트림은 무시한다).
        for (OggStream s : reader.getStreams()) {
            if ("opus".equals(s.getCodec())) {
                streams.add(s);
            }
        }
        if (streams.isEmpty()) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "no Opus stream in the Ogg file");
        }
        return OpusSource.open(streams);
    }
}
