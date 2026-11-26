package org.duckdns.hjow.swingwebpanel.media.audio;

import java.io.IOException;
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

    /** Former buffered response limit retained for array-only registered codecs. / 배열만 받는 등록 코덱에 유지하는 기존 응답 버퍼 한도. */
    private static final int LEGACY_ARRAY_MAX_BYTES = 16 * 1024 * 1024;

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
        return openWith(choose(data, mimeHint, lim), data, lim);
    }

    /**
     * Opens playback PCM from a fetched memory or file resource. The caller still owns the resource and may open it again.
     * <p>
     * 메모리나 파일로 가져온 리소스를 재생 PCM으로 연다. 리소스는 호출자가 계속 소유하며 다시 열 수 있다.
     * @param data encoded media resource / 인코딩된 미디어 리소스
     * @param mimeHint MIME type hint or null / MIME 타입 힌트 또는 null
     * @param limits resource limits, null for defaults / 리소스 제한. null이면 기본값
     * @return PCM positioned at the start / 시작 위치의 PCM 소스
     * @throws MediaException for invalid, unsupported or unreadable data / 손상·미지원·읽기 실패 시
     */
    public static PcmSource openMedia(MediaData data, String mimeHint, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        checkSize(data == null ? 0 : data.length(), lim);
        byte[] memory = data.inMemoryBytes();
        if (memory != null) {
            return open(memory, mimeHint, lim);
        }
        String hint = normalizeMime(mimeHint);
        ByteSource input = null;
        try {
            input = data.open();
            byte[] legacy = null;
            // 힌트 우선순위를 유지하면서 파일을 바로 읽을 수 있는 디코더부터 각각 그 진입점으로 연다.
            for (AudioDecoder decoder : preferredDecoders(hint)) {
                if (decoder instanceof ByteAudioDecoder) {
                    ByteAudioDecoder seekable = (ByteAudioDecoder) decoder;
                    if (accepts(seekable, input)) {
                        PcmSource pcm;
                        try {
                            pcm = seekable.openSource(input, lim);
                        } catch (RuntimeException e) {
                            throw damaged(decoder, e);
                        }
                        input = null;
                        return pcm;
                    }
                } else if (input.length() <= LEGACY_ARRAY_MAX_BYTES) {
                    // 배열 API만 등록한 코덱에는 필요한 경우에만 기존 전송 한도 안에서 복사한 입력을 준다.
                    if (legacy == null) {
                        legacy = Bytes.copy(input, 0, (int) input.length());
                    }
                    if (accepts(decoder, legacy)) {
                        return openWith(decoder, legacy, lim);
                    }
                }
            }
            throw unsupported(hint);
        } catch (ByteSourceException e) {
            throw e.toMediaException();
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.ACCESS_FAILED, "could not open encoded audio", e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Opens encoded audio for decoding with the channels and the precision of the file (plan item P25-05): the decoder is chosen exactly as
     * {@link #open} chooses it (by the signature of the data, the MIME type is only a hint); a decoder that implements {@link WideAudioDecoder} delivers
     * 32 bit float samples in every channel of the file, any other decoder is used through its 16 bit path ({@link PcmFloatAdapter}, at most two
     * channels). The result is read from the start to the end.
     * <p>
     * 파일의 채널과 정밀도로 디코딩하려고 인코딩된 오디오를 연다(계획 P25-05). 디코더는 {@link #open} 이 고르는 것과 똑같이 고른다(데이터의 시그니처로.
     * MIME 타입은 힌트일 뿐이다). {@link WideAudioDecoder} 를 구현한 디코더는 파일의 모든 채널을 32비트 float 표본으로 주고, 다른 디코더는 16비트 경로로
     * 쓴다({@link PcmFloatAdapter}, 채널은 최대 둘). 결과는 처음부터 끝까지 읽는다.
     *
     * @param data     the whole encoded data / 인코딩된 전체 데이터
     * @param mimeHint MIME type of the response, may be null; only a hint / 응답의 MIME 타입. null 가능하며 힌트일 뿐이다
     * @param limits   limits, null for the defaults / 제한. null 이면 기본값
     * @return a float source positioned at the start / 처음에 위치한 float 소스
     * @throws MediaException the same failures as {@link #open} / {@link #open} 과 같은 실패
     */
    public static FloatPcmSource openWide(byte[] data, String mimeHint, DecodeLimits limits) throws MediaException {
        DecodeLimits lim = limits == null ? DecodeLimits.DEFAULT : limits;
        AudioDecoder d = choose(data, mimeHint, lim);
        // 넓은 경로가 있는 디코더는 그것을 쓰고 없으면 16비트 경로를 float 로 감싼다.
        if (d instanceof WideAudioDecoder) {
            try {
                return ((WideAudioDecoder) d).openWide(data, lim);
            } catch (RuntimeException e) {
                throw damaged(d, e);
            }
        }
        return new PcmFloatAdapter(openWith(d, data, lim));
    }

    /**
     * Chooses the decoder of the data: validates the size, asks the decoder of the hinted MIME type first, then every other one, and takes
     * the first whose signature matches.
     * <p>
     * 데이터의 디코더를 고른다. 크기를 검사하고 힌트의 MIME 타입을 선언한 디코더에게 먼저 묻고 그다음 나머지 모두에게 물어 시그니처가 맞는 첫 디코더를 쓴다.
     *
     * @param data     the whole encoded data / 인코딩된 전체 데이터
     * @param mimeHint MIME type hint or null / MIME 타입 힌트 또는 null
     * @param lim      limits / 제한
     * @return the decoder / 디코더
     * @throws MediaException CORRUPT for empty data, TOO_LARGE over the limits, UNSUPPORTED_FORMAT if no decoder recognizes the data / 빈 데이터는 CORRUPT, 제한 초과는 TOO_LARGE, 어떤 디코더도 알아보지 못하면 UNSUPPORTED_FORMAT
     */
    private static AudioDecoder choose(byte[] data, String mimeHint, DecodeLimits lim) throws MediaException {
        checkSize(data == null ? 0 : data.length, lim);
        String hint = normalizeMime(mimeHint);
        // 시그니처가 맞는 첫 디코더로 연다.
        for (AudioDecoder decoder : preferredDecoders(hint)) {
            if (accepts(decoder, data)) {
                return decoder;
            }
        }
        throw unsupported(hint);
    }

    /**
     * Rejects empty or excess-size input. / 비었거나 너무 큰 입력을 거부한다.
     * @param size encoded length / 인코딩 길이
     * @param limits decoding limits / 해독 제한
     * @throws MediaException if size is invalid / 크기가 올바르지 않으면
     */
    private static void checkSize(long size, DecodeLimits limits) throws MediaException {
        if (size == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the audio data is empty");
        }
        if (size > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the audio data is larger than the limit");
        }
    }

    /**
     * Lists hinted codecs before the remaining registered codecs. / 힌트와 일치하는 코덱 뒤에 나머지 등록 코덱을 나열한다.
     * @param hint normalized MIME hint or null / 정규화한 MIME 힌트 또는 null
     * @return codecs in probing order / 판별 순서의 코덱
     */
    private static List<AudioDecoder> preferredDecoders(String hint) {
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
        return order;
    }

    /**
     * Probes an array signature, treating malformed probes as unrelated data. / 배열 시그니처를 조사하며 손상 입력은 일치하지 않은 것으로 본다.
     * @param decoder the codec / 코덱
     * @param data encoded array / 인코딩 배열
     * @return true when the signature matches / 시그니처가 맞으면 true
     */
    private static boolean accepts(AudioDecoder decoder, byte[] data) {
        try {
            return decoder.accepts(data);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Probes a seekable signature while preserving storage errors. / 탐색 가능한 원천의 시그니처를 조사하고 저장소 오류를 보존한다.
     * @param decoder the codec / 코덱
     * @param data encoded source / 인코딩 원천
     * @return true when the signature matches / 시그니처가 맞으면 true
     */
    private static boolean accepts(ByteAudioDecoder decoder, ByteSource data) {
        try {
            return decoder.acceptsSource(data);
        } catch (ByteSourceException e) {
            throw e;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Creates the failure when no registered codec recognizes an input. / 등록된 코덱이 입력을 알아보지 못했을 때의 오류를 만든다.
     * @param hint normalized MIME hint or null / 정규화한 MIME 힌트 또는 null
     * @return unsupported format error / 미지원 형식 오류
     */
    private static MediaException unsupported(String hint) {
        return new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unrecognized audio format"
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
            throw damaged(d, e);
        }
    }

    /**
     * Converts an unexpected decoder exception into a codec-data failure. / 디코더의 예상하지 못한 예외를 코덱 데이터 오류로 변환한다.
     * @param decoder the chosen codec / 선택된 코덱
     * @param cause unexpected failure / 예상하지 못한 오류
     * @return corrupt data error / 데이터 손상 오류
     */
    private static MediaException damaged(AudioDecoder decoder, RuntimeException cause) {
        return new MediaException(MediaException.Kind.CORRUPT, "damaged " + decoder.getFormatName() + " data: " + cause, cause);
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
