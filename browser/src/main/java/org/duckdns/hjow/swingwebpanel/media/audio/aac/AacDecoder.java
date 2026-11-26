package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.mp4.Mp4File;
import org.duckdns.hjow.swingwebpanel.media.audio.mp4.Mp4Track;

/**
 * The AAC decoder (plan item P18-07, FEATURE_MATRIX AUD-05): AAC-LC (and Main profile streams without prediction) in ADTS files and in the
 * MP4 / M4A container, mono, stereo and multichannel (mixed down to stereo) with TNS, PNS, M/S and intensity stereo, short window
 * sequences, pulse data and sine/Kaiser-Bessel windows. HE-AAC streams are decoded as their AAC-LC core without the SBR bandwidth extension
 * (they play at the core sampling frequency with reduced bandwidth); LTP, scalable, error resilient, 960-line and ADIF streams are reported as
 * unsupported. Registered by the registry through reflection (public no-argument constructor).
 * <p>
 * AAC 디코더(계획 P18-07, FEATURE_MATRIX AUD-05): ADTS 파일과 MP4 / M4A 컨테이너 안의 AAC-LC(예측 없는 Main 프로파일 포함). 모노, 스테레오, 다채널
 * (스테레오로 섞음)과 TNS, PNS, M/S, 인텐시티 스테레오, 짧은 윈도 순서, 펄스 데이터, 사인/카이저-베셀 윈도를 지원한다. HE-AAC 는 SBR 대역 확장 없이
 * AAC-LC 핵심으로 해독한다(핵심 샘플링 주파수로 대역이 줄어든 채 재생). LTP, 스케일러블, 오류 내성, 960선, ADIF 스트림은 미지원으로 보고한다.
 * 레지스트리가 리플렉션으로 등록하며 인수 없는 공개 생성자가 있다.
 */
public final class AacDecoder extends ByteAudioDecoder {

    /** Maximum buffered bytes for one MP4 AAC access unit. / MP4 AAC 접근 단위 하나의 최대 버퍼 크기. */
    private static final int MAX_AAC_BLOCK = 1024 * 1024;

    /**
     * Creates the decoder. / 디코더를 만든다.
     */
    public AacDecoder() {
    }

    /** Returns the format name aac. / 형식 이름 aac 를 반환한다. */
    public String getFormatName() {
        return "aac";
    }

    /** Returns the MIME types of AAC audio. / AAC 오디오의 MIME 타입을 반환한다. */
    public String[] getMimeTypes() {
        return new String[] { "audio/aac", "audio/aacp", "audio/x-aac", "audio/mp4", "audio/x-m4a", "audio/m4a", "audio/mp4a-latm", "audio/3gpp" };
    }

    /** Tells whether the data is an ADTS stream or an MP4 file with an AAC track. / 데이터가 ADTS 스트림이거나 AAC 트랙이 있는 MP4 파일인지 알려 준다. */
    public boolean acceptsSource(ByteSource data) {
        int start = skipId3(data);
        if (findAdts(data, start) >= 0) {
            return true;
        }
        if (Mp4File.looksLikeMp4Source(data)) {
            try {
                Mp4Track t = Mp4File.parseSource(data).firstAudioTrack();
                return t != null && "mp4a".equals(t.getFormat()) && isAacObject(t.getObjectType());
            } catch (MediaException e) {
                if (e.getCause() instanceof ByteSourceException) {
                    throw (ByteSourceException) e.getCause();
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Finds the ADTS stream or the MP4 file and creates the source of the form that was asked for.
     * <p>
     * ADTS 스트림이나 MP4 파일을 찾아 요청한 형태의 소스를 만든다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @param wide   true for the wide path / 넓은 경로이면 true
     * @return a {@link PcmSource} or a {@link FloatPcmSource} / {@link PcmSource} 또는 {@link FloatPcmSource}
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    protected Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        if (data.length() > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "audio data is too large");
        }
        int start = skipId3(data);
        try {
            int a = findAdts(data, start);
            if (a >= 0) {
                return openAdts(data, a, wide);
            }
            if (Mp4File.looksLikeMp4Source(data)) {
                return openMp4(data, wide);
            }
        } catch (AacException e) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, e.getMessage());
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "not an AAC stream");
    }

    /**
     * Tells whether an MP4 object type indication is AAC (MPEG-4 audio 0x40, MPEG-2 AAC profiles 0x66 to 0x68).
     * <p>
     * MP4 객체 종류 표시가 AAC 인지(MPEG-4 오디오 0x40, MPEG-2 AAC 프로파일 0x66~0x68) 알려 준다.
     *
     * @param ot object type / 객체 종류
     * @return true for AAC / AAC 이면 true
     */
    private static boolean isAacObject(int ot) {
        return ot == 0x40 || (ot >= 0x66 && ot <= 0x68) || ot == 0;
    }

    /**
     * Skips an ID3v2 tag.
     * <p>
     * ID3v2 태그를 건너뛴다.
     *
     * @param d data / 데이터
     * @return position after the tag / 태그 뒤 위치
     */
    private static int skipId3(ByteSource d) {
        if (Bytes.has(d, 0, 10) && Bytes.matches(d, 0, "ID3")) {
            int size = ((d.get(6) & 0x7F) << 21) | ((d.get(7) & 0x7F) << 14) | ((d.get(8) & 0x7F) << 7) | (d.get(9) & 0x7F);
            int total = 10 + size + ((d.get(5) & 0x10) != 0 ? 10 : 0);
            if (total <= d.length()) {
                return total;
            }
        }
        return 0;
    }

    /**
     * Returns the length of the ADTS frame at a position, or -1 if there is none.
     * <p>
     * 위치의 ADTS 프레임 길이를 반환하며 없으면 -1 이다.
     *
     * @param d data / 데이터
     * @param p position / 위치
     * @return frame length or -1 / 프레임 길이 또는 -1
     */
    private static int adtsLength(ByteSource d, int p) {
        if (!Bytes.has(d, p, 7) || d.get(p) != 0xFF || (d.get(p + 1) & 0xF6) != 0xF0) {
            return -1;
        }
        int rate = (d.get(p + 2) >> 2) & 15;
        if (rate > 12) {
            return -1;
        }
        int len = ((d.get(p + 3) & 3) << 11) | (d.get(p + 4) << 3) | (d.get(p + 5) >> 5);
        int header = (d.get(p + 1) & 1) != 0 ? 7 : 9;
        return len >= header ? len : -1;
    }

    /**
     * Finds the first position of a chain of ADTS frames.
     * <p>
     * ADTS 프레임이 이어지는 첫 위치를 찾는다.
     *
     * @param d     data / 데이터
     * @param start start of the search / 검색 시작
     * @return position or -1 / 위치 또는 -1
     */
    private static int findAdts(ByteSource d, int start) {
        int end = (int) Math.min(d.length(), Integer.MAX_VALUE);
        int limit = (int) Math.min(end - 7, (long) start + 65536);
        for (int p = start; p <= limit; p++) { // 한 바이트씩 옮기며 ADTS 헤더가 두 번 연속 맞는 곳을 찾는다
            int len = adtsLength(d, p);
            if (len > 0 && len <= end - p) {
                int next = p + len;
                if (next == end || (next <= end - 7 && adtsLength(d, next) > 0 && sameStream(d, p, next))) {
                    return p;
                }
            }
        }
        return -1;
    }

    /**
     * Tells whether two ADTS headers describe the same stream (profile, sampling frequency).
     * <p>
     * 두 ADTS 헤더가 같은 스트림(프로파일, 샘플링 주파수)을 설명하는지 알려 준다.
     *
     * @param d data / 데이터
     * @param a first header / 첫 헤더
     * @param b second header / 둘째 헤더
     * @return true if equal / 같으면 true
     */
    private static boolean sameStream(ByteSource d, int a, int b) {
        return (d.get(a + 2) & 0xFC) == (d.get(b + 2) & 0xFC);
    }

    /**
     * Opens an ADTS stream.
     * <p>
     * ADTS 스트림을 연다.
     *
     * @param d     data / 데이터
     * @param first first frame / 첫 프레임
     * @param wide true for float PCM / float PCM이면 true
     * @return source / 소스
     * @throws MediaException on damaged data / 손상된 데이터일 때
     * @throws AacException   on an unsupported profile / 지원하지 않는 프로파일일 때
     */
    private Object openAdts(ByteSource d, int first, boolean wide) throws MediaException, AacException {
        int headerConfig = d.get(first + 2);
        int profile = (headerConfig >> 6) + 1;
        int rate = (headerConfig >> 2) & 15;
        int cc = ((headerConfig & 1) << 2) | (d.get(first + 3) >> 6);
        if (profile > 2) {
            throw new AacException("AAC profile " + profile + " is not supported");
        }
        int[] off = new int[1024];
        int[] len = new int[1024];
        int n = 0;
        int p = first;
        int end = (int) d.length();
        while (p <= end - 7) { // 프레임 길이만큼 건너뛰며 블록 위치를 모은다
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "AAC indexing was interrupted");
            }
            int fl = adtsLength(d, p);
            if (fl < 0 || fl > end - p || !sameStream(d, first, p)) {
                int found = -1;
                int limit = (int) Math.min(end - 7, (long) p + 65536);
                for (int q = p + 1; q <= limit; q++) { // 어긋나면 다음 올바른 헤더를 찾는다
                    int l2 = adtsLength(d, q);
                    if (l2 > 0 && l2 <= end - q && sameStream(d, first, q)) {
                        found = q;
                        break;
                    }
                }
                if (found < 0) {
                    break;
                }
                p = found;
                continue;
            }
            int header = (d.get(p + 1) & 1) != 0 ? 7 : 9;
            if (n == off.length) {
                off = java.util.Arrays.copyOf(off, n * 2);
                len = java.util.Arrays.copyOf(len, n * 2);
            }
            off[n] = p + header;
            len[n] = fl - header;
            n++;
            p += fl;
        }
        if (n == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "no AAC frame found");
        }
        AacBlockInput blocks = new AacBlockInput(d, java.util.Arrays.copyOf(off, n), java.util.Arrays.copyOf(len, n));
        AacConfig cfg = new AacConfig(profile, rate, cc, false);
        return createPcm(blocks, cfg, wide, 0, (long) n * AacFrameDecoder.FRAME);
    }

    /**
     * Opens an MP4 / M4A file.
     * <p>
     * MP4 / M4A 파일을 연다.
     *
     * @param d data / 데이터
     * @param wide true for float PCM / float PCM이면 true
     * @return source / 소스
     * @throws MediaException on damaged data / 손상된 데이터일 때
     * @throws AacException   on an unsupported stream / 지원하지 않는 스트림일 때
     */
    private Object openMp4(ByteSource d, boolean wide) throws MediaException, AacException {
        Mp4Track t = Mp4File.parseSource(d).firstAudioTrack();
        if (t == null || !"mp4a".equals(t.getFormat())) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the MP4 file has no AAC audio track");
        }
        if (!isAacObject(t.getObjectType())) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "the audio track is not AAC (object type " + t.getObjectType() + ")");
        }
        AacConfig cfg = AacConfig.fromAudioSpecificConfig(t.getConfig(), 0, t.getConfig().length);
        int n = t.getSampleCount();
        int[] off = new int[n];
        int[] len = new int[n];
        int count = 0;
        long totalTicks = 0;
        long end = d.length();
        for (int i = 0; i < n; i++) { // 파일 안에 완전히 있는 샘플만 쓴다
            long o = t.getOffset(i);
            int s = t.getSize(i);
            if (o < 0 || s < 0 || o > end - s) {
                continue;
            }
            if (s > MAX_AAC_BLOCK) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "AAC access unit exceeds the decoding block limit");
            }
            off[count] = (int) o;
            len[count] = s;
            count++;
            totalTicks += t.getDuration(i);
        }
        if (count == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the AAC track has no samples");
        }
        double ticksToFrames = (double) cfg.sampleRate / Math.max(1, t.getTimescale());
        int skip = (int) Math.round(t.getEditStart() * ticksToFrames);
        long total = Math.round(totalTicks * ticksToFrames) - skip;
        if (t.getEditDurationInMediaUnits() > 0) {
            total = Math.min(total, Math.round(t.getEditDurationInMediaUnits() * ticksToFrames));
        }
        if (total <= 0) {
            total = (long) count * AacFrameDecoder.FRAME - skip;
        }
        AacBlockInput blocks = new AacBlockInput(d, java.util.Arrays.copyOf(off, count), java.util.Arrays.copyOf(len, count));
        return createPcm(blocks, cfg, wide, skip, total);
    }

    /**
     * Creates the PCM source, probing the first block if the header omits the channel configuration.
     * <p>
     * PCM 소스를 만든다. 헤더에 채널 구성이 없으면 첫 블록의 채널 수를 확인한다.
     *
     * @param blocks encoded blocks / 인코딩 블록
     * @param cfg configuration / 구성
     * @param wide true for float PCM / float PCM이면 true
     * @param skip initial frames to discard / 시작에서 버릴 프레임 수
     * @param total total output frames / 전체 출력 프레임 수
     * @return the requested PCM source / 요청한 PCM 소스
     * @throws MediaException if the first block cannot be read / 첫 블록을 읽을 수 없으면
     * @throws AacException if the first block cannot be decoded / 첫 블록을 해독할 수 없으면
     */
    private static Object createPcm(AacBlockInput blocks, AacConfig cfg, boolean wide, int skip, long total) throws MediaException, AacException {
        int ch = cfg.channels();
        if (ch <= 0) {
            AacFrameDecoder probe = new AacFrameDecoder(cfg.rateIndex, 0);
            AacBits bits = blocks.read(0);
            if (wide) {
                ch = probe.decodeWide(bits, new float[8][AacFrameDecoder.FRAME]);
            } else {
                probe.decode(bits, new float[2][AacFrameDecoder.FRAME], 2);
                ch = probe.lastChannelCount();
            }
            ch = Math.max(1, ch);
        }
        return wide ? new AacFloatSource(blocks, cfg, ch, skip, total) : new AacSource(blocks, cfg, Math.min(ch, 2), skip, total);
    }
}
