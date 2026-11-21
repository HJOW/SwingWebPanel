package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.WideAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * Decoder of RIFF/WAVE files, including the 64-bit forms RF64 and BW64 (plan item P18-07, FEATURE_MATRIX AUD-01): PCM of 8 (unsigned), 16, 24 and
 * 32 bits, IEEE float of 32 and 64 bits, A-law, mu-law, IMA ADPCM and Microsoft ADPCM, also inside WAVE_FORMAT_EXTENSIBLE. Chunks are walked with the padding rule of
 * RIFF; unknown chunks are skipped. A <code>data</code> chunk whose size is 0, 0xFFFFFFFF or larger than the file (streaming or truncated
 * files) extends to the end of the file; an incomplete last frame is ignored. More than two channels are mixed down to stereo (the
 * speaker mask of an extensible header selects the layout). Compressed tags (MP3, AAC, ...) are reported as unsupported.
 * <p>
 * RIFF/WAVE 파일과 64비트 형태인 RF64, BW64 의 디코더(계획 P18-07, FEATURE_MATRIX AUD-01): 8(부호 없음)/16/24/32비트 PCM, 32/64비트 IEEE 부동소수점, A-law,
 * mu-law, IMA ADPCM, Microsoft ADPCM 이며 WAVE_FORMAT_EXTENSIBLE 안에 든 것도 처리한다. 청크는 RIFF 의 패딩 규칙대로 훑고 모르는 청크는 건너뛴다. 크기가 0,
 * 0xFFFFFFFF 이거나 파일보다 큰 <code>data</code> 청크(스트리밍 또는 잘린 파일)는 파일 끝까지로 본다. 불완전한 마지막 프레임은 무시한다. 채널이 둘보다
 * 많으면 스테레오로 다운믹스한다(확장 헤더의 스피커 마스크가 배치를 정한다). 압축 태그(MP3, AAC 등)는 지원하지 않는 형식으로 보고한다.
 */
public final class WavDecoder implements WideAudioDecoder {

    /**
     * Largest supported number of channels.
     * <p>
     * 지원하는 최대 채널 수.
     */
    private static final int MAX_CHANNELS = 32;

    /**
     * Largest supported sample rate.
     * <p>
     * 지원하는 최대 샘플링 주파수.
     */
    private static final int MAX_RATE = 1000000;

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public WavDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "wav"
     */
    public String getFormatName() {
        return "wav";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" };
    }

    /**
     * Checks the RIFF/WAVE signature (RF64 and BW64, the 64-bit forms of EBU Tech 3306 and ITU-R BS.2088, start with RF64 or BW64 instead of RIFF).
     * <p>
     * RIFF/WAVE 시그니처를 확인한다(EBU Tech 3306 과 ITU-R BS.2088 의 64비트 형태인 RF64, BW64 는 RIFF 대신 RF64 나 BW64 로 시작한다).
     *
     * @param data the data / 데이터
     * @return true for RIFF....WAVE, RF64....WAVE or BW64....WAVE / RIFF....WAVE, RF64....WAVE, BW64....WAVE 면 true
     */
    public boolean accepts(byte[] data) {
        return (Bytes.matches(data, 0, "RIFF") || Bytes.matches(data, 0, "RF64") || Bytes.matches(data, 0, "BW64")) && Bytes.matches(data, 8, "WAVE");
    }

    /**
     * The fields of the format chunk.
     * <p>
     * fmt 청크의 필드.
     */
    private static final class Fmt {
        /**
         * Format tag after resolving WAVE_FORMAT_EXTENSIBLE.
         * <p>
         * WAVE_FORMAT_EXTENSIBLE 를 해석한 뒤의 형식 태그.
         */
        int tag;

        /**
         * Number of channels.
         * <p>
         * 채널 수.
         */
        int channels;

        /**
         * Sample rate.
         * <p>
         * 샘플링 주파수.
         */
        int rate;

        /**
         * Bytes of one frame (or of one ADPCM block).
         * <p>
         * 프레임 하나(또는 ADPCM 블록 하나)의 바이트 수.
         */
        int blockAlign;

        /**
         * Bits per sample.
         * <p>
         * 샘플당 비트 수.
         */
        int bits;

        /**
         * Speaker mask of an extensible header, else 0.
         * <p>
         * 확장 헤더의 스피커 마스크. 아니면 0.
         */
        long mask;

        /**
         * Offset of the extra bytes after cbSize.
         * <p>
         * cbSize 뒤 추가 바이트의 위치.
         */
        int extra;

        /**
         * Length of the extra bytes available in the chunk.
         * <p>
         * 청크에 들어 있는 추가 바이트의 길이.
         */
        int extraLen;
    }

    /**
     * Opens the file for decoding.
     * <p>
     * 디코딩을 위해 파일을 연다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @return PCM source / PCM 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        return (PcmSource) create(data, false);
    }

    /**
     * Opens the data with every channel of the file and the precision of its samples (see {@link WideAudioDecoder}).
     * <p>
     * 파일의 모든 채널과 표본의 정밀도로 데이터를 연다({@link WideAudioDecoder} 참조).
     *
     * @param data   the whole encoded data / 인코딩된 전체 데이터
     * @param limits limits to respect / 지켜야 할 제한
     * @return a float source positioned at the start / 처음에 위치한 float 소스
     * @throws MediaException the failures of {@link #open} / {@link #open} 의 실패
     */
    public FloatPcmSource openWide(byte[] data, DecodeLimits limits) throws MediaException {
        return (FloatPcmSource) create(data, true);
    }

    /**
     * Parses the headers and creates the source of the form that was asked for (a {@link PcmSource} or a {@link FloatPcmSource}).
     * <p>
     * 헤더를 해석하고 요청한 형태의 소스({@link PcmSource} 또는 {@link FloatPcmSource})를 만든다.
     *
     * @param data the whole file / 파일 전체
     * @param wide true for the wide path / 넓은 경로이면 true
     * @return the source / 소스
     * @throws MediaException on a damaged or unsupported file / 손상되었거나 지원하지 않는 파일이면
     */
    private Object create(byte[] data, boolean wide) throws MediaException {
        int pos = 12;
        Fmt fmt = null;
        int dataStart = -1;
        long dataLen = 0;
        long ds64Data = -1;
        // 청크를 차례로 훑으며 fmt 와 첫 data 청크를 찾는다.
        while (Bytes.has(data, pos, 8)) {
            String id = Bytes.ascii(data, pos, 4);
            long size = Bytes.le32(data, pos + 4);
            int body = pos + 8;
            long remaining = data.length - body;
            if ("fmt ".equals(id) && fmt == null) {
                if (size < 16 || remaining < 16) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the WAV format chunk is too short");
                }
                fmt = parseFmt(data, body, (int) Math.min(size, remaining));
            } else if ("ds64".equals(id) && ds64Data < 0 && size >= 28 && remaining >= 28) {
                // RF64/BW64 의 ds64 청크: RIFF 크기(8바이트), data 크기(8바이트), 샘플 수(8바이트). 4GB 를 넘는 크기는 여기에만 있다.
                ds64Data = Bytes.le64(data, body + 8);
            } else if ("data".equals(id) && dataStart < 0) {
                dataStart = body;
                // RF64 의 data 청크 크기 0xFFFFFFFF 는 ds64 청크의 data 크기를 뜻한다.
                if (size == 0xFFFFFFFFL && ds64Data >= 0) {
                    size = ds64Data;
                }
                dataLen = (size == 0 || size > remaining) ? remaining : size;
            }
            if (size > remaining) {
                break;
            }
            pos = (int) (body + size + (size & 1));
        }
        if (fmt == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the WAV file has no format chunk");
        }
        if (dataStart < 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the WAV file has no data chunk");
        }
        if (fmt.channels < 1 || fmt.channels > MAX_CHANNELS || fmt.rate < 1 || fmt.rate > MAX_RATE || fmt.blockAlign < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid WAV format values");
        }
        ChannelMixer mixer = fmt.channels > 2 ? (fmt.mask != 0 ? ChannelMixer.forWavMask(fmt.channels, fmt.mask) : ChannelMixer.forChannels(fmt.channels))
                : null;
        int end = (int) (dataStart + dataLen);
        switch (fmt.tag) {
        case 1:
            return PcmSources.raw(wide, data, dataStart, end, integerFormat(fmt), fmt.channels, fmt.rate, mixer);
        case 3:
            if (fmt.bits == 32) {
                return PcmSources.raw(wide, data, dataStart, end, SampleFormat.F32LE, fmt.channels, fmt.rate, mixer);
            } else if (fmt.bits == 64) {
                return PcmSources.raw(wide, data, dataStart, end, SampleFormat.F64LE, fmt.channels, fmt.rate, mixer);
            }
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported WAV float width " + fmt.bits);
        case 6:
            return PcmSources.raw(wide, data, dataStart, end, SampleFormat.ALAW, fmt.channels, fmt.rate, mixer);
        case 7:
            return PcmSources.raw(wide, data, dataStart, end, SampleFormat.ULAW, fmt.channels, fmt.rate, mixer);
        case 0x11:
            return openIma(wide, data, dataStart, end, fmt, mixer);
        case 2:
            return openMs(wide, data, dataStart, end, fmt, mixer);
        default:
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported WAV format tag 0x" + Integer.toHexString(fmt.tag));
        }
    }

    /**
     * Parses the format chunk.
     * <p>
     * fmt 청크를 해석한다.
     *
     * @param d    data / 데이터
     * @param p    offset of the chunk body / 청크 본문의 위치
     * @param size usable bytes of the chunk, at least 16 / 쓸 수 있는 청크 바이트 수(16 이상)
     * @return the fields / 필드
     */
    private static Fmt parseFmt(byte[] d, int p, int size) {
        Fmt f = new Fmt();
        f.tag = Bytes.le16(d, p);
        f.channels = Bytes.le16(d, p + 2);
        f.rate = (int) Math.min(Bytes.le32(d, p + 4), Integer.MAX_VALUE);
        f.blockAlign = Bytes.le16(d, p + 12);
        f.bits = Bytes.le16(d, p + 14);
        int cb = size >= 18 ? Bytes.le16(d, p + 16) : 0;
        f.extra = p + 18;
        f.extraLen = Math.max(0, Math.min(cb, size - 18));
        if (f.tag == 0xFFFE && f.extraLen >= 22) {
            f.mask = Bytes.le32(d, p + 20);
            f.tag = Bytes.le16(d, p + 24);
        }
        return f;
    }

    /**
     * Chooses the integer PCM encoding from the container size.
     * <p>
     * 컨테이너 크기로 정수 PCM 인코딩을 고른다.
     *
     * @param f format / 형식
     * @return the encoding / 인코딩
     * @throws MediaException for sizes that cannot be played / 재생할 수 없는 크기이면
     */
    private static SampleFormat integerFormat(Fmt f) throws MediaException {
        int bytes = (f.bits + 7) / 8;
        if (f.blockAlign % f.channels == 0 && f.blockAlign / f.channels >= bytes && f.blockAlign / f.channels <= 4) {
            bytes = f.blockAlign / f.channels;
        }
        switch (bytes) {
        case 1:
            return SampleFormat.U8;
        case 2:
            return SampleFormat.S16LE;
        case 3:
            return SampleFormat.S24LE;
        case 4:
            return SampleFormat.S32LE;
        default:
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported WAV sample size of " + f.bits + " bits");
        }
    }

    /**
     * Creates the source of an IMA ADPCM file.
     * <p>
     * IMA ADPCM 파일의 소스를 만든다.
     *
     * @param d     data / 데이터
     * @param start start of the data chunk / data 청크의 시작
     * @param end   end of the data chunk / data 청크의 끝
     * @param f     format / 형식
     * @param mixer mixer or null / 믹서 또는 null
     * @return source / 소스
     * @throws MediaException if the format values are invalid / 형식 값이 올바르지 않으면
     */
    private static Object openIma(boolean wide, byte[] d, int start, int end, Fmt f, ChannelMixer mixer) throws MediaException {
        if (f.channels > 8 || f.blockAlign < 4 * f.channels + 4 * f.channels) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid IMA ADPCM block size");
        }
        int capacity = 1 + (f.blockAlign - 4 * f.channels) / (4 * f.channels) * 8;
        int spb = f.extraLen >= 2 ? Bytes.le16(d, f.extra) : capacity;
        spb = Math.min(spb <= 0 ? capacity : spb, capacity);
        return PcmSources.adpcm(wide, d, start, end, AdpcmSource.IMA, f.channels, f.rate, f.blockAlign, spb, null, mixer);
    }

    /**
     * Creates the source of a Microsoft ADPCM file.
     * <p>
     * Microsoft ADPCM 파일의 소스를 만든다.
     *
     * @param d     data / 데이터
     * @param start start of the data chunk / data 청크의 시작
     * @param end   end of the data chunk / data 청크의 끝
     * @param f     format / 형식
     * @param mixer mixer or null / 믹서 또는 null
     * @return source / 소스
     * @throws MediaException if the format values are invalid / 형식 값이 올바르지 않으면
     */
    private static Object openMs(boolean wide, byte[] d, int start, int end, Fmt f, ChannelMixer mixer) throws MediaException {
        if (f.channels > 8 || f.blockAlign < 7 * f.channels + 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid MS ADPCM block size");
        }
        int capacity = 2 + (f.blockAlign - 7 * f.channels) * 2 / f.channels;
        int spb = capacity;
        int[] coefs = AdpcmSource.MS_DEFAULT_COEFS;
        if (f.extraLen >= 4) {
            int declared = Bytes.le16(d, f.extra);
            int n = Bytes.le16(d, f.extra + 2);
            if (n < 1 || n > 256 || f.extraLen < 4 + n * 4) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid MS ADPCM coefficient table");
            }
            coefs = new int[n * 2];
            // 계수 쌍을 읽는다.
            for (int i = 0; i < n * 2; i++) {
                coefs[i] = (short) Bytes.le16(d, f.extra + 4 + 2 * i);
            }
            spb = declared <= 1 ? capacity : Math.min(declared, capacity);
        }
        return PcmSources.adpcm(wide, d, start, end, AdpcmSource.MS, f.channels, f.rate, f.blockAlign, spb, coefs, mixer);
    }
}
