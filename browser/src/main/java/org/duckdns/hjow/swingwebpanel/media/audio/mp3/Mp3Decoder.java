package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;

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
public final class Mp3Decoder extends ByteAudioDecoder {

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

    /** Looks for consecutive Layer III frames after any ID3v2 tag. / ID3v2 태그가 있다면 그 뒤에서 연속한 Layer III 프레임을 찾는다. */
    public boolean acceptsSource(ByteSource data) {
        return Mp3Source.looksLikeMp3(data);
    }

    /**
     * Builds a frame index and returns an on-demand PCM decoder. / 프레임 색인을 만들고 요청할 때 PCM을 해독하는 소스를 반환한다.
     * @param data encoded input / 인코딩 입력
     * @param limits limits / 제한
     * @param wide true for float PCM / float PCM이면 true
     * @return 16-bit or float PCM source / 16비트 또는 float PCM 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    protected Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        Mp3Source source = new Mp3Source(data, limits);
        return wide ? new Mp3FloatSource(source) : source;
    }
}
