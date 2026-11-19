package org.duckdns.hjow.swingwebpanel.media.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.flac.FlacDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.pcm.AiffDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.pcm.AuDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.pcm.WavDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.vorbis.VorbisDecoder;

/**
 * The registry of audio decoders and the single entry point of the audio pipeline (plan items P18-01, P18-02, P18-07): it chooses the
 * decoder from the <em>signature of the data</em> and uses the MIME type of the response only as a hint (a decoder that names the hinted
 * MIME type is asked first), so the same extension or MIME type with a different real codec is decoded correctly and a wrong MIME type
 * does not make a valid file fail. Decoders of the other audio families (MP3, AAC, Opus, WebM) are found by name when their classes
 * exist; a missing class is skipped silently. The registry is thread-safe.
 * <p>
 * 오디오 디코더 등록부이자 오디오 파이프라인의 유일한 진입점(계획 P18-01, P18-02, P18-07): 디코더를 <em>데이터의 시그니처</em>로 고르고 응답의 MIME 타입은
 * 힌트로만 쓴다(힌트의 MIME 타입을 선언한 디코더에게 먼저 묻는다). 그래서 확장자나 MIME 타입이 같아도 실제 코덱이 다른 경우를 올바르게 디코딩하고
 * 잘못된 MIME 타입 때문에 올바른 파일이 실패하지 않는다. 다른 계열(MP3, AAC, Opus, WebM)의 디코더는 클래스가 있으면 이름으로 찾으며 없으면 조용히
 * 건너뛴다. 등록부는 스레드 안전하다.
 */
public final class AudioDecoders {

    /**
     * Class names of optional decoders of the other audio families, loaded by reflection when present.
     * <p>
     * 다른 오디오 계열의 선택적 디코더 클래스 이름. 있으면 리플렉션으로 불러온다.
     */
    private static final String[] OPTIONAL_DECODERS = { "org.duckdns.hjow.swingwebpanel.media.audio.mp3.Mp3Decoder", "org.duckdns.hjow.swingwebpanel.media.audio.aac.AacDecoder",
            "org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusDecoder", "org.duckdns.hjow.swingwebpanel.media.audio.webm.WebmDecoder" };

    /**
     * The registered decoders in the order in which they are asked.
     * <p>
     * 등록된 디코더. 묻는 순서대로이다.
     */
    private static final List<AudioDecoder> DECODERS = new CopyOnWriteArrayList<AudioDecoder>();

    /**
     * Codec identifiers (as in the <code>codecs</code> parameter of a MIME type) that may be played inside a container MIME type,
     * each with the format name of the decoder that plays it.
     * <p>
     * 컨테이너 MIME 타입 안에서 재생할 수 있는 코덱 식별자(MIME 타입의 <code>codecs</code> 매개변수에서 쓰는 값)와 그것을 재생하는 디코더의
     * 형식 이름.
     */
    private static final Map<String, Map<String, String>> CODECS = new HashMap<String, Map<String, String>>();

    static {
        DECODERS.add(new WavDecoder());
        DECODERS.add(new AiffDecoder());
        DECODERS.add(new AuDecoder());
        DECODERS.add(new FlacDecoder());
        DECODERS.add(new VorbisDecoder());
        // 선택적 디코더는 클래스가 있을 때만 이름으로 만들어 등록한다.
        for (String name : OPTIONAL_DECODERS) {
            try {
                Object o = Class.forName(name).newInstance();
                if (o instanceof AudioDecoder) {
                    DECODERS.add((AudioDecoder) o);
                }
            } catch (ClassNotFoundException e) {
                continue;
            } catch (LinkageError e) {
                continue;
            } catch (Exception e) {
                continue;
            }
        }
        codec("wav", "audio/wav", "1", "3", "6", "7", "2", "17");
        codec("wav", "audio/x-wav", "1", "3", "6", "7", "2", "17");
        codec("wav", "audio/wave", "1", "3", "6", "7", "2", "17");
        codec("aiff", "audio/aiff", "pcm");
        codec("aiff", "audio/x-aiff", "pcm");
        codec("au", "audio/basic", "ulaw", "pcm");
        codec("flac", "audio/flac", "flac");
        codec("flac", "audio/x-flac", "flac");
        codec("flac", "audio/ogg", "flac");
        codec("vorbis", "audio/ogg", "vorbis");
        codec("opus", "audio/ogg", "opus");
        codec("opus", "audio/opus", "opus");
        codec("mp3", "audio/mpeg", "mp3");
        codec("mp3", "audio/mp3", "mp3");
        codec("aac", "audio/aac", "mp4a.40.2", "mp4a.40.5", "mp4a.40.29");
        codec("aac", "audio/mp4", "mp4a.40.2", "mp4a.40.5", "mp4a.40.29");
        codec("aac", "audio/x-m4a", "mp4a.40.2", "mp4a.40.5", "mp4a.40.29");
        codec("mp3", "audio/mp4", "mp3", "mp4a.6b", "mp4a.69");
        codec("webm", "audio/webm", "vorbis", "opus");
    }

    /**
     * Private constructor: static facade class.
     * <p>
     * 비공개 생성자: 정적 파사드 클래스이다.
     */
    private AudioDecoders() {
    }

    /**
     * Records codec identifiers of a MIME type together with the format name of the decoder that plays them.
     * <p>
     * MIME 타입의 코덱 식별자를 그것을 재생하는 디코더의 형식 이름과 함께 기록한다.
     *
     * @param format name of the decoder format as returned by {@link AudioDecoder#getFormatName()} / {@link AudioDecoder#getFormatName()} 이 반환하는 디코더 형식 이름
     * @param mime   MIME type / MIME 타입
     * @param codecs codec identifiers / 코덱 식별자
     */
    private static void codec(String format, String mime, String... codecs) {
        Map<String, String> m = CODECS.get(mime);
        if (m == null) {
            m = new HashMap<String, String>();
            CODECS.put(mime, m);
        }
        // 코덱마다 형식 이름을 기록한다.
        for (String c : codecs) {
            m.put(c, format);
        }
    }

    /**
     * Registers an additional decoder; it is asked after the ones that are already registered.
     * <p>
     * 디코더를 추가로 등록한다. 이미 등록된 것들 다음에 묻는다.
     *
     * @param decoder the decoder / 디코더
     */
    public static void register(AudioDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("decoder must not be null");
        }
        DECODERS.add(decoder);
    }

    /**
     * Opens encoded audio for decoding.
     * <p>
     * 인코딩된 오디오를 디코딩하려고 연다.
     *
     * @param data     the whole encoded data / 인코딩된 전체 데이터
     * @param mimeHint MIME type of the response, may be null; only a hint / 응답의 MIME 타입. null 가능하며 힌트일 뿐이다
     * @param limits   limits, null for the defaults / 제한. null 이면 기본값
     * @return a PCM source positioned at the start / 처음에 위치한 PCM 소스
     * @throws MediaException CORRUPT for empty or damaged data, TOO_LARGE over the limits, UNSUPPORTED_FORMAT if no decoder recognizes the data / 비었거나 손상된 데이터는 CORRUPT, 제한 초과는 TOO_LARGE, 어떤 디코더도 알아보지 못하면 UNSUPPORTED_FORMAT
     */
    public static PcmSource open(byte[] data, String mimeHint, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        if (data == null || data.length == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the audio data is empty");
        }
        if (data.length > lim.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the audio data is larger than the limit");
        }
        String hint = normalizeMime(mimeHint);
        List<AudioDecoder> order = new ArrayList<AudioDecoder>(DECODERS.size());
        // 힌트의 MIME 타입을 선언한 디코더를 앞에 둔다.
        for (AudioDecoder d : DECODERS) {
            if (hint != null && Arrays.asList(d.getMimeTypes()).contains(hint)) {
                order.add(d);
            }
        }
        // 등록된 디코더를 하나씩 확인한다.
        for (AudioDecoder d : DECODERS) {
            if (!order.contains(d)) {
                order.add(d);
            }
        }
        // 시그니처가 맞는 첫 디코더로 연다.
        for (AudioDecoder d : order) {
            boolean accepted;
            try {
                accepted = d.accepts(data);
            } catch (RuntimeException e) {
                accepted = false;
            }
            if (accepted) {
                return openWith(d, data, lim);
            }
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unrecognized audio format"
                + (hint == null ? "" : " (declared as " + hint + ")"));
    }

    /**
     * Opens with a chosen decoder and turns unexpected runtime failures caused by damaged data into CORRUPT.
     * <p>
     * 고른 디코더로 열며 손상된 데이터 때문에 생긴 예상 밖의 런타임 오류를 CORRUPT 로 바꾼다.
     *
     * @param d    decoder / 디코더
     * @param data data / 데이터
     * @param lim  limits / 제한
     * @return source / 소스
     * @throws MediaException on failure / 실패 시
     */
    private static PcmSource openWith(AudioDecoder d, byte[] data, DecodeLimits lim) throws MediaException {
        try {
            return d.open(data, lim);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged " + d.getFormatName() + " data: " + e, e);
        }
    }

    /**
     * Normalizes a MIME type: parameters removed, lower case, trimmed; null for null or empty.
     * <p>
     * MIME 타입을 정규화한다: 매개변수를 떼고 소문자로 바꾸고 공백을 없앤다. null 이나 빈 값이면 null.
     *
     * @param mime MIME type or null / MIME 타입 또는 null
     * @return normalized type or null / 정규화한 타입 또는 null
     */
    public static String normalizeMime(String mime) {
        if (mime == null) {
            return null;
        }
        int semi = mime.indexOf(';');
        String t = (semi >= 0 ? mime.substring(0, semi) : mime).trim().toLowerCase(Locale.ENGLISH);
        return t.length() == 0 ? null : t;
    }

    /**
     * Returns every MIME type that a registered decoder handles, sorted.
     * <p>
     * 등록된 디코더가 처리하는 모든 MIME 타입을 정렬해 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public static List<String> getSupportedMimeTypes() {
        Set<String> all = new TreeSet<String>();
        // 디코더마다 선언한 MIME 타입을 모은다.
        for (AudioDecoder d : DECODERS) {
            all.addAll(Arrays.asList(d.getMimeTypes()));
        }
        return Collections.unmodifiableList(new ArrayList<String>(all));
    }

    /**
     * Tells whether a registered decoder handles a MIME type (parameters are ignored).
     * <p>
     * 등록된 디코더가 MIME 타입을 처리하는지 알려 준다(매개변수는 무시한다).
     *
     * @param mime MIME type, may contain parameters / MIME 타입. 매개변수가 있어도 된다
     * @return true if supported / 지원하면 true
     */
    public static boolean isSupportedMime(String mime) {
        String t = normalizeMime(mime);
        return t != null && getSupportedMimeTypes().contains(t);
    }

    /**
     * Tells whether a decoder of a format name is registered.
     * <p>
     * 형식 이름의 디코더가 등록되어 있는지 알려 준다.
     *
     * @param format format name / 형식 이름
     * @return true if registered / 등록되어 있으면 true
     */
    private static boolean hasFormat(String format) {
        // 등록된 디코더 중 형식 이름이 같은 것이 있는지 찾는다.
        for (AudioDecoder d : DECODERS) {
            if (format.equals(d.getFormatName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The answer of HTMLMediaElement.canPlayType for a MIME type: the empty string when the type or one of the listed codecs is not
     * supported, <code>"probably"</code> when the type and all listed codecs are supported, and <code>"maybe"</code> for a supported
     * type without a codecs list.
     * <p>
     * MIME 타입에 대한 HTMLMediaElement.canPlayType 의 답: 타입이나 나열한 코덱 중 하나라도 지원하지 않으면 빈 문자열, 타입과 나열한 모든 코덱을
     * 지원하면 <code>"probably"</code>, 코덱 목록이 없는 지원하는 타입이면 <code>"maybe"</code>.
     *
     * @param type MIME type, optionally with <code>codecs="a, b"</code> / MIME 타입. <code>codecs="a, b"</code> 가 있어도 된다
     * @return "", "maybe" or "probably" / "", "maybe" 또는 "probably"
     */
    public static String canPlayType(String type) {
        String base = normalizeMime(type);
        if (base == null || !isSupportedMime(base)) {
            return "";
        }
        String codecs = codecsParameter(type);
        if (codecs == null) {
            return "maybe";
        }
        Map<String, String> known = CODECS.get(base);
        // 나열된 코덱이 모두 이 컨테이너에서 알려진 코덱인지 확인한다.
        for (String c : codecs.split(",")) {
            String name = c.trim().toLowerCase(Locale.ENGLISH);
            if (name.length() == 0) {
                continue;
            }
            if (known == null || !known.containsKey(name) || !hasFormat(known.get(name))) {
                return "";
            }
        }
        return "probably";
    }

    /**
     * Extracts the value of the codecs parameter of a MIME type.
     * <p>
     * MIME 타입의 codecs 매개변수 값을 꺼낸다.
     *
     * @param type MIME type with parameters / 매개변수가 있는 MIME 타입
     * @return the codecs list without quotes, or null / 따옴표를 뗀 코덱 목록. 없으면 null
     */
    private static String codecsParameter(String type) {
        int semi = type.indexOf(';');
        if (semi < 0) {
            return null;
        }
        String rest = type.substring(semi + 1);
        // 매개변수를 세미콜론으로 나누어 codecs 를 찾는다.
        for (String p : rest.split(";")) {
            int eq = p.indexOf('=');
            if (eq > 0 && p.substring(0, eq).trim().equalsIgnoreCase("codecs")) {
                String v = p.substring(eq + 1).trim();
                if (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
                    v = v.substring(1, v.length() - 1);
                }
                return v;
            }
        }
        return null;
    }
}
