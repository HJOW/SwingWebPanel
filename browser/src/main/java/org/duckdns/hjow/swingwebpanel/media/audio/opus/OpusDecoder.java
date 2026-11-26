package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmFloatAdapter;
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
public final class OpusDecoder extends ByteAudioDecoder {

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
    public boolean acceptsSource(ByteSource data) {
        return OggReader.sniffSourceCodecs(data).contains("opus");
    }

    /**
     * Opens the data as 16-bit PCM or in float with every channel of the stream. A chained stream whose
     * links differ in the channel layout cannot be given with a fixed number of channels; it is given through the 16 bit path (stereo).
     * <p>
     * 16비트 PCM 또는 스트림의 모든 채널을 담은 float으로 데이터를 연다. 고리마다 채널 배치가 다른 연결된 스트림은 고정된 채널 수로 줄 수
     * 없으므로 16비트 경로(스테레오)로 준다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @param wide true for float output / float 출력이면 true
     * @return PCM source / PCM 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    protected Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        OpusSource source = createSource(data);
        if (!wide) {
            return source;
        }
        int channels = source.uniformChannels();
        // 고리마다 채널 배치가 다르면 16비트 경로로 준다.
        if (channels < 0) {
            return new PcmFloatAdapter(source);
        }
        return new OpusFloatSource(source, channels);
    }

    /**
     * Opens the Opus logical streams of the data and indexes their packets.
     * <p>
     * 데이터의 Opus 논리 스트림을 열고 패킷을 색인한다.
     *
     * @param data the data / 데이터
     * @return the source that decodes packet after packet / 패킷을 차례로 디코딩하는 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    private static OpusSource createSource(ByteSource data) throws MediaException {
        OggReader reader = OggReader.scanSource(data);
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
        return OpusSource.open(data, streams);
    }
}
