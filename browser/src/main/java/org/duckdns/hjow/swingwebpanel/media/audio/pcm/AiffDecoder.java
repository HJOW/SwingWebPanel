package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.WideAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * Decoder of AIFF and AIFF-C files (plan item P18-07, FEATURE_MATRIX AUD-02): big-endian PCM of 1 to 32 bits, and the AIFF-C
 * compression types <code>NONE</code>/<code>twos</code>, <code>sowt</code> (little-endian), <code>raw </code> (8-bit unsigned),
 * <code>in24</code>, <code>in32</code>, <code>fl32</code>, <code>fl64</code>, <code>ulaw</code>, <code>alaw</code> and <code>ima4</code> (QuickTime IMA ADPCM). The
 * sample rate is read from the 80-bit extended number. Other compression types are reported as unsupported. A sound data chunk that is
 * shorter than the declared number of frames (truncated file) ends the stream early.
 * <p>
 * AIFF 와 AIFF-C 파일의 디코더(계획 P18-07, FEATURE_MATRIX AUD-02): 1~32비트 빅 엔디언 PCM 과 AIFF-C 압축 유형 <code>NONE</code>/<code>twos</code>,
 * <code>sowt</code>(리틀 엔디언), <code>raw </code>(8비트 부호 없음), <code>in24</code>, <code>in32</code>, <code>fl32</code>, <code>fl64</code>,
 * <code>ulaw</code>, <code>alaw</code>, <code>ima4</code>(QuickTime IMA ADPCM). 샘플링 주파수는 80비트 확장 수에서 읽는다. 다른 압축 유형은 지원하지 않는
 * 형식으로 보고한다. 소리 데이터 청크가 선언한 프레임 수보다 짧으면(잘린 파일) 스트림이 일찍 끝난다.
 */
public final class AiffDecoder implements WideAudioDecoder {

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public AiffDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "aiff"
     */
    public String getFormatName() {
        return "aiff";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/aiff", "audio/x-aiff", "audio/aifc", "audio/x-aifc" };
    }

    /**
     * Checks the FORM/AIFF or FORM/AIFC signature.
     * <p>
     * FORM/AIFF 또는 FORM/AIFC 시그니처를 확인한다.
     *
     * @param data the data / 데이터
     * @return true if it matches / 맞으면 true
     */
    public boolean accepts(byte[] data) {
        return Bytes.matches(data, 0, "FORM") && (Bytes.matches(data, 8, "AIFF") || Bytes.matches(data, 8, "AIFC"));
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
        boolean aifc = Bytes.matches(data, 8, "AIFC");
        int pos = 12;
        int comm = -1;
        int commSize = 0;
        int sound = -1;
        long soundSize = 0;
        // 청크를 훑으며 COMM 과 SSND 를 찾는다.
        while (Bytes.has(data, pos, 8)) {
            String id = Bytes.ascii(data, pos, 4);
            long size = Bytes.be32(data, pos + 4);
            int body = pos + 8;
            long remaining = data.length - body;
            if ("COMM".equals(id) && comm < 0) {
                comm = body;
                commSize = (int) Math.min(size, remaining);
            } else if ("SSND".equals(id) && sound < 0) {
                sound = body;
                soundSize = Math.min(size, remaining);
            }
            if (size > remaining) {
                break;
            }
            pos = (int) (body + size + (size & 1));
        }
        if (comm < 0 || commSize < 18) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the AIFF file has no common chunk");
        }
        int channels = Bytes.be16(data, comm);
        long declaredFrames = Bytes.be32(data, comm + 2);
        int bits = Bytes.be16(data, comm + 6);
        double rate = extended(data, comm + 8);
        if (channels < 1 || channels > 64 || bits < 1 || bits > 64 || !(rate >= 1 && rate <= 1000000)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid AIFF format values");
        }
        String compression = "NONE";
        if (aifc) {
            if (commSize < 22) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the AIFF-C common chunk is too short");
            }
            compression = Bytes.ascii(data, comm + 18, 4);
        }
        if ("ima4".equals(compression)) {
            return openIma4(wide, data, sound, soundSize, channels, rate);
        }
        SampleFormat format = chooseFormat(compression, bits);
        if (sound < 0 || soundSize < 8) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the AIFF file has no sound data chunk");
        }
        long offset = Bytes.be32(data, sound);
        long start = sound + 8 + offset;
        long end = sound + soundSize;
        if (start > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid AIFF sound data offset");
        }
        long frameBytes = (long) format.getWidth() * channels;
        // 선언한 프레임 수가 실제 데이터보다 적으면 그만큼만 쓴다(0 이면 데이터 전체).
        if (declaredFrames > 0 && declaredFrames * frameBytes < end - start) {
            end = start + declaredFrames * frameBytes;
        }
        return PcmSources.raw(wide, data, (int) start, (int) end, format, channels, (int) Math.round(rate), null);
    }

    /**
     * Opens AIFF-C <code>ima4</code> data (QuickTime IMA ADPCM: 34 bytes per channel for every 64 frames).
     * <p>
     * AIFF-C <code>ima4</code> 데이터(QuickTime IMA ADPCM: 64프레임마다 채널별 34바이트)를 연다.
     *
     * @param data      the file / 파일
     * @param sound     offset of the sound data chunk body, or -1 / SSND 청크 본문의 위치. 없으면 -1
     * @param soundSize size of the sound data chunk / SSND 청크의 크기
     * @param channels  channels / 채널 수
     * @param rate      sample rate / 샘플링 주파수
     * @return PCM source / PCM 소스
     * @throws MediaException for a missing or invalid sound chunk / SSND 청크가 없거나 올바르지 않으면
     */
    private static Object openIma4(boolean wide, byte[] data, int sound, long soundSize, int channels, double rate) throws MediaException {
        if (sound < 0 || soundSize < 8) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the AIFF file has no sound data chunk");
        }
        long offset = Bytes.be32(data, sound);
        long start = sound + 8 + offset;
        long end = Math.min(sound + soundSize, data.length);
        if (start > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid AIFF sound data offset");
        }
        return PcmSources.adpcm(wide, data, (int) start, (int) end, AdpcmSource.IMA_QT, channels, (int) Math.round(rate), 34 * channels, 64, null, null);
    }

    /**
     * Chooses the sample encoding from the compression type and the sample size.
     * <p>
     * 압축 유형과 샘플 크기로 샘플 인코딩을 고른다.
     *
     * @param compression four-character compression type ("NONE" for plain AIFF) / 네 글자 압축 유형(일반 AIFF 는 "NONE")
     * @param bits        bits per sample / 샘플당 비트 수
     * @return the encoding / 인코딩
     * @throws MediaException for compressions that are not supported / 지원하지 않는 압축이면
     */
    private static SampleFormat chooseFormat(String compression, int bits) throws MediaException {
        int bytes = (bits + 7) / 8;
        if ("NONE".equals(compression) || "twos".equals(compression) || "in24".equals(compression) || "in32".equals(compression)) {
            if ("in24".equals(compression)) {
                bytes = 3;
            } else if ("in32".equals(compression)) {
                bytes = 4;
            }
            switch (bytes) {
            case 1:
                return SampleFormat.S8;
            case 2:
                return SampleFormat.S16BE;
            case 3:
                return SampleFormat.S24BE;
            case 4:
                return SampleFormat.S32BE;
            default:
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported AIFF sample size of " + bits + " bits");
            }
        }
        if ("sowt".equals(compression)) {
            switch (bytes) {
            case 1:
                return SampleFormat.S8;
            case 2:
                return SampleFormat.S16LE;
            case 3:
                return SampleFormat.S24LE;
            case 4:
                return SampleFormat.S32LE;
            default:
                throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported AIFF-C sample size of " + bits + " bits");
            }
        }
        if ("raw ".equals(compression)) {
            return SampleFormat.U8;
        }
        if ("fl32".equals(compression) || "FL32".equals(compression)) {
            return SampleFormat.F32BE;
        }
        if ("fl64".equals(compression) || "FL64".equals(compression)) {
            return SampleFormat.F64BE;
        }
        if ("ulaw".equals(compression) || "ULAW".equals(compression)) {
            return SampleFormat.ULAW;
        }
        if ("alaw".equals(compression) || "ALAW".equals(compression)) {
            return SampleFormat.ALAW;
        }
        throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported AIFF-C compression '" + compression + "'");
    }

    /**
     * Reads an IEEE 754 80-bit extended number (sign, 15-bit exponent, 64-bit mantissa with explicit integer bit).
     * <p>
     * IEEE 754 80비트 확장 수(부호, 15비트 지수, 정수 비트가 명시된 64비트 가수)를 읽는다.
     *
     * @param d data / 데이터
     * @param p offset of the 10 bytes / 10바이트의 위치
     * @return the value, or NaN for an invalid number / 값. 올바르지 않으면 NaN
     */
    static double extended(byte[] d, int p) {
        int se = Bytes.be16(d, p);
        int exp = se & 0x7FFF;
        double mant = Bytes.be32(d, p + 2) * 4294967296.0 + Bytes.be32(d, p + 6);
        if (exp == 0x7FFF || (se & 0x8000) != 0) {
            return Double.NaN;
        }
        if (mant == 0) {
            return 0;
        }
        return Math.scalb(mant, exp - 16383 - 63);
    }
}
