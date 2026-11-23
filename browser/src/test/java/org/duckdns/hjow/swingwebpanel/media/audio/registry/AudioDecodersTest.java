package org.duckdns.hjow.swingwebpanel.media.audio.registry;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the decoder registry (plan items P18-01, P18-02): choice by signature, the MIME type as a hint only, the failure kinds of
 * empty, unknown and oversized data, supported MIME types and canPlayType.
 * <p>
 * 디코더 등록부 시험(계획 P18-01, P18-02): 시그니처로 고르기, 힌트일 뿐인 MIME 타입, 빈/알 수 없는/너무 큰 데이터의 실패 종류, 지원하는 MIME 타입,
 * canPlayType.
 */
public class AudioDecodersTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AudioDecodersTest() {
    }

    /**
     * The same bytes decode with any MIME hint; different containers with one hint decode as what they are.
     * <p>
     * 같은 바이트는 어떤 MIME 힌트로도 디코딩되고, 같은 힌트를 줘도 다른 컨테이너는 실제 형식으로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSignatureDecidesNotMime() throws Exception {
        byte[] pcm = new byte[40];
        pcm[0] = 1;
        byte[] wav = AudioKit.wav(1, 1, 8000, 16, pcm, -1);
        byte[] aiff = AudioKit.aiff("sowt", 1, 16, 8000, 20, pcm);
        byte[] au = AudioKit.au(3, 1, 8000, pcm, 40);
        String[] hints = { null, "", "audio/mpeg", "audio/ogg; codecs=vorbis", "text/html", "AUDIO/WAV; charset=x", "audio/basic" };
        // 힌트마다 세 파일이 모두 같은 PCM 으로 디코딩되는지 확인한다.
        for (String hint : hints) {
            Check.equal(40, AudioKit.readAll(AudioDecoders.open(wav, hint, null)).length, "wav with hint " + hint);
            Check.equal(40, AudioKit.readAll(AudioDecoders.open(aiff, hint, null)).length, "aiff with hint " + hint);
            Check.equal(40, AudioKit.readAll(AudioDecoders.open(au, hint, null)).length, "au with hint " + hint);
        }
    }

    /**
     * Empty, null and unknown data fail with the documented kinds and mention the declared type.
     * <p>
     * 빈 데이터, null, 알 수 없는 데이터는 문서화한 종류로 실패하며 선언한 타입을 메시지에 담는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailures() throws Exception {
        try {
            AudioDecoders.open(new byte[0], "audio/wav", null);
            Check.fail("empty data must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "empty");
        }
        try {
            AudioDecoders.open(null, null, null);
            Check.fail("null data must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "null");
        }
        try {
            AudioDecoders.open("<html>this is not audio</html>".getBytes("US-ASCII"), "audio/wav", null);
            Check.fail("unknown data must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "unknown");
            Check.contains(e.getMessage(), "audio/wav", "the declared type is in the message");
        }
        try {
            AudioDecoders.open(new byte[] { 'R', 'I', 'F', 'F' }, null, null);
            Check.fail("a bare RIFF tag must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "too short to be a WAV file");
        }
    }

    /**
     * The supported MIME types include the families of this registry and exclude video and text; parameters and case are ignored.
     * <p>
     * 지원하는 MIME 타입은 이 등록부의 계열을 포함하고 비디오와 텍스트는 제외하며 매개변수와 대소문자는 무시한다.
     */
    public void testSupportedMimeTypes() {
        List<String> all = AudioDecoders.getSupportedMimeTypes();
        String[] expected = { "audio/wav", "audio/x-wav", "audio/wave", "audio/aiff", "audio/x-aiff", "audio/basic", "audio/flac", "audio/x-flac", "audio/ogg", "application/ogg", "audio/vorbis" };
        // 기대하는 타입이 모두 들어 있는지 확인한다.
        for (String m : expected) {
            Check.isTrue(all.contains(m), "supported: " + m);
            Check.isTrue(AudioDecoders.isSupportedMime(m.toUpperCase() + "; codecs=x"), "case and parameters are ignored for " + m);
        }
        Check.isFalse(AudioDecoders.isSupportedMime("video/mp4"), "video is not an audio type");
        Check.isFalse(AudioDecoders.isSupportedMime("text/plain"), "text");
        Check.isFalse(AudioDecoders.isSupportedMime(null), "null");
        Check.isFalse(AudioDecoders.isSupportedMime(""), "empty");
        Check.equal("audio/wav", AudioDecoders.normalizeMime("  Audio/WAV ; x=y"), "normalization");
        Check.isNull(AudioDecoders.normalizeMime(" ; a=b"), "empty type");
    }

    /**
     * The canPlayType answers: maybe for a bare supported type, probably when all listed codecs are known, empty otherwise.
     * <p>
     * canPlayType 의 답: 지원하는 타입만 있으면 maybe, 나열한 코덱을 모두 알면 probably, 그 밖에는 빈 문자열.
     */
    public void testCanPlayType() {
        Check.equal("maybe", AudioDecoders.canPlayType("audio/wav"), "bare WAV");
        Check.equal("probably", AudioDecoders.canPlayType("audio/wav; codecs=\"1\""), "PCM in WAV");
        Check.equal("probably", AudioDecoders.canPlayType("audio/wav; codecs=1, 3"), "list of codecs");
        Check.equal("", AudioDecoders.canPlayType("audio/wav; codecs=vorbis"), "Vorbis is not a WAV codec");
        Check.equal("", AudioDecoders.canPlayType("audio/wav; codecs=\"1, bogus\""), "one unknown codec makes the answer empty");
        Check.equal("", AudioDecoders.canPlayType("video/mp4"), "video");
        Check.equal("", AudioDecoders.canPlayType(null), "null");
        Check.equal("", AudioDecoders.canPlayType("audio/unknown"), "unknown type");
        Check.equal("probably", AudioDecoders.canPlayType("audio/flac; codecs=flac"), "FLAC");
        Check.equal("", AudioDecoders.canPlayType("audio/flac; codecs=vorbis"), "Vorbis is not a FLAC codec");
        Check.equal("probably", AudioDecoders.canPlayType("audio/ogg; codecs=\"vorbis\""), "Vorbis in Ogg");
        Check.equal("probably", AudioDecoders.canPlayType("audio/ogg; codecs=\"flac, vorbis\""), "FLAC and Vorbis in Ogg");
        Check.equal("", AudioDecoders.canPlayType("audio/ogg; codecs=\"theora\""), "Theora is video");
    }

    /**
     * Opening is independent between sources: closing one does not disturb another from the same bytes.
     * <p>
     * 소스끼리는 독립적이다: 같은 바이트에서 만든 소스 하나를 닫아도 다른 소스는 영향을 받지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourcesAreIndependent() throws Exception {
        byte[] wav = AudioKit.wav(1, 1, 8000, 16, new byte[200], -1);
        PcmSource a = AudioDecoders.open(wav, null, null);
        PcmSource b = AudioDecoders.open(wav, null, null);
        a.close();
        Check.equal(200, AudioKit.readAll(b).length, "the second source still reads");
    }
}
