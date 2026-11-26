package org.duckdns.hjow.swingwebpanel.media.audio.webm;

import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaData;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaSpool;
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
public final class WebmDecoder extends ByteAudioDecoder {

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
    public boolean acceptsSource(ByteSource data) {
        if (!WebmFile.looksLikeEbmlSource(data)) {
            return false;
        }
        try {
            return WebmFile.parseSource(data).firstAudioTrack() != null;
        } catch (MediaException e) {
            if (e.getCause() instanceof ByteSourceException) {
                throw (ByteSourceException) e.getCause();
            }
            return false;
        }
    }

    /**
     * Re-muxes the first audio track as an Ogg stream and opens it with the decoder of its codec, in the form that was asked for.
     * <p>
     * 첫 오디오 트랙을 Ogg 스트림으로 다시 묶어 그 코덱의 디코더로 요청한 형태로 연다.
     *
     * @param data   the file / 파일
     * @param limits limits / 제한
     * @param wide   true for the wide path / 넓은 경로이면 true
     * @return a {@link PcmSource} or a {@link FloatPcmSource} / {@link PcmSource} 또는 {@link FloatPcmSource}
     * @throws MediaException on a damaged or unsupported file / 손상되었거나 지원하지 않는 파일이면
     */
    protected Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        if (data.length() > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "audio data is too large");
        }
        WebmFile f = WebmFile.parseSource(data);
        WebmFile.Track t = f.firstAudioTrack();
        if (t == null) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the file has no audio track");
        }
        boolean opus = "A_OPUS".equals(t.getCodecId());
        if (!opus && !"A_VORBIS".equals(t.getCodecId())) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the WebM audio codec " + t.getCodecId() + " is not supported");
        }
        ByteAudioDecoder codec = opus ? new OpusDecoder() : new VorbisDecoder();
        MediaData derived = remux(t, data, opus);
        boolean opened = false;
        try {
            ByteSource packets = derived.openOwned();
            Object pcm = wide ? codec.openWideSource(packets, limits) : codec.openSource(packets, limits);
            data.close();
            opened = true;
            return pcm;
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "could not open derived Ogg data", e);
        } finally {
            if (!opened) {
                derived.release();
            }
        }
    }

    /**
     * Writes Ogg pages to bounded storage. The caller owns the returned resource.
     * <p>
     * 제한된 저장소에 Ogg 페이지를 쓴다. 반환한 리소스는 호출자가 소유한다.
     * @param track parsed audio track / 해석한 오디오 트랙
     * @param data source of the packets / 패킷 원천
     * @param opus true for Opus, false for Vorbis / Opus이면 true, Vorbis이면 false
     * @return the derived Ogg resource / 파생 Ogg 리소스
     * @throws MediaException if remuxing fails / 재포장에 실패하면
     */
    static MediaData remux(WebmFile.Track track, ByteSource data, boolean opus) throws MediaException {
        MediaSpool spool = new MediaSpool();
        try {
            if (opus) {
                OpusInOgg.write(track, data, spool);
            } else {
                VorbisInOgg.write(track, data, spool);
            }
            return spool.finish();
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "could not spool derived Ogg data", e);
        } finally {
            spool.discard();
        }
    }
}
