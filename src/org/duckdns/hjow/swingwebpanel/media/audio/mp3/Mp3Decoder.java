package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.WideAudioDecoder;

/**
 * The MP3 decoder (plan item P18-07, FEATURE_MATRIX AUD-04): MPEG-1/2/2.5 Layer III with ID3v2/ID3v1 tags, Xing/Info/VBRI and LAME
 * headers, constant and variable bit rate, mono and stereo (MS and intensity), short and mixed blocks, the bit reservoir and
 * resynchronization after damaged data. Layer I and Layer II streams are recognized but reported as unsupported (browsers decode MP3 only
 * as Layer III; free-format streams are not supported). Registered by the audio decoder registry through reflection; it has a public no-argument
 * constructor.
 * <p>
 * MP3 디코더(계획 P18-07, FEATURE_MATRIX AUD-04): MPEG-1/2/2.5 Layer III, ID3v2/ID3v1 태그, Xing/Info/VBRI 와 LAME 헤더, 고정/가변 비트레이트,
 * 모노와 스테레오(MS, 인텐시티), 짧은/혼합 블록, 비트 저장소, 손상된 데이터 뒤의 재동기화를 지원한다. Layer I, II 스트림은 인식하지만
 * 미지원으로 보고한다(브라우저는 MP3 를 Layer III 로만 해독하며 자유 형식 스트림은 지원하지 않는다). 오디오 디코더 등록부가 리플렉션으로 등록하며
 * 인수 없는 공개 생성자가 있다.
 *
 * Usage / 사용법: <code>PcmSource s = new Mp3Decoder().open(bytes, DecodeLimits.DEFAULT); s.read(buf, 0, buf.length);</code>
 */
public final class Mp3Decoder implements WideAudioDecoder {

    /**
     * Creates the decoder. / 디코더를 만든다.
     */
    public Mp3Decoder() {
    }

    /** Returns the format name mp3. / 형식 이름 mp3 를 반환한다. */
    public String getFormatName() {
        return "mp3";
    }

    /** Returns the MIME types of MP3 audio. / MP3 오디오의 MIME 타입을 반환한다. */
    public String[] getMimeTypes() {
        return new String[] { "audio/mpeg", "audio/mp3", "audio/x-mpeg", "audio/mpeg3", "audio/x-mp3" };
    }

    /** Tells whether the data is an MP3 stream (ID3v2 tag or a chain of Layer III frames). / 데이터가 MP3 스트림인지(ID3v2 태그 또는 이어지는 Layer III 프레임) 알려 준다. */
    public boolean accepts(byte[] data) {
        return Mp3Source.looksLikeMp3(data);
    }

    /** Opens the data and reads the headers; frames are decoded on demand. / 데이터를 열어 헤더를 읽으며 프레임은 요청할 때 해독한다. */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        return new Mp3Source(data, limits);
    }

    /**
     * Opens the data and reads the headers; frames are decoded on demand and delivered as the floats of the decoder (see {@link WideAudioDecoder}).
     * <p>
     * 데이터를 열어 헤더를 읽으며 프레임은 요청할 때 해독하여 디코더의 float 로 준다({@link WideAudioDecoder} 참조).
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @return float source / float 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    public FloatPcmSource openWide(byte[] data, DecodeLimits limits) throws MediaException {
        return new Mp3FloatSource(new Mp3Source(data, limits));
    }
}
