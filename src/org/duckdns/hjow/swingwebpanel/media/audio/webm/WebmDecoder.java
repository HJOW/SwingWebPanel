package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusInOgg;
import org.duckdns.hjow.swingwebpanel.media.audio.vorbis.VorbisDecoder;

/**
 * The WebM / Matroska audio decoder (plan item P18-07, FEATURE_MATRIX AUD-09): it demultiplexes the container and decodes Vorbis tracks
 * (through the Vorbis decoder of the registry package) and Opus tracks (through the Opus decoder of this project). Matroska files with
 * other audio codecs are recognized and reported as unsupported. Registered by the registry through reflection (public no-argument
 * constructor).
 * <p>
 * WebM / Matroska 오디오 디코더(계획 P18-07, FEATURE_MATRIX AUD-09): 컨테이너를 역다중화하고 Vorbis 트랙(레지스트리 패키지의 Vorbis 디코더 사용)과 Opus
 * 트랙(이 프로젝트의 Opus 디코더 사용)을 해독한다. 다른 오디오 코덱의 Matroska 파일은 인식하되 미지원으로 보고한다. 레지스트리가 리플렉션으로 등록하며
 * 인수 없는 공개 생성자가 있다.
 */
public final class WebmDecoder implements AudioDecoder {

    /**
     * Creates the decoder. / 디코더를 만든다.
     */
    public WebmDecoder() {
    }

    /** Returns the format name webm. / 형식 이름 webm 을 반환한다. */
    public String getFormatName() {
        return "webm";
    }

    /** Returns the MIME types of WebM audio. / WebM 오디오의 MIME 타입을 반환한다. */
    public String[] getMimeTypes() {
        return new String[] { "audio/webm", "audio/x-matroska", "audio/matroska", "video/webm" };
    }

    /** Tells whether the data is an EBML file with an audio track. / 데이터가 오디오 트랙이 있는 EBML 파일인지 알려 준다. */
    public boolean accepts(byte[] data) {
        if (!WebmFile.looksLikeEbml(data)) {
            return false;
        }
        try {
            return WebmFile.parse(data).firstAudioTrack() != null;
        } catch (MediaException e) {
            return false;
        }
    }

    /** Opens the file and the decoder of its first audio track. / 파일과 첫 오디오 트랙의 디코더를 연다. */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        if (data.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "audio data is too large");
        }
        WebmFile f = WebmFile.parse(data);
        WebmFile.Track t = f.firstAudioTrack();
        if (t == null) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the file has no audio track");
        }
        if ("A_VORBIS".equals(t.getCodecId())) {
            return new VorbisDecoder().open(VorbisInOgg.build(t, data), limits);
        }
        if ("A_OPUS".equals(t.getCodecId())) {
            return new OpusDecoder().open(OpusInOgg.build(t, data), limits);
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the WebM audio codec " + t.getCodecId() + " is not supported");
    }
}
