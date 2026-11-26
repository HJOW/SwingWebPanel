package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;

/**
 * Decoder of Sun/NeXT AU (SND) files (plan item P18-07, FEATURE_MATRIX AUD-03): mu-law, A-law, linear PCM of 8, 16, 24 and 32 bits (big
 * endian) and IEEE float of 32 and 64 bits. A data size of 0xFFFFFFFF (unknown) or larger than the file extends to the end of the file.
 * The ADPCM and DSP encodings of the AU specification are reported as unsupported.
 * <p>
 * Sun/NeXT AU(SND) 파일의 디코더(계획 P18-07, FEATURE_MATRIX AUD-03): mu-law, A-law, 8/16/24/32비트 선형 PCM(빅 엔디언), 32/64비트 IEEE 부동소수점.
 * 데이터 크기가 0xFFFFFFFF(알 수 없음)이거나 파일보다 크면 파일 끝까지로 본다. AU 규격의 ADPCM 과 DSP 인코딩은 지원하지 않는 형식으로 보고한다.
 */
public final class AuDecoder extends ByteAudioDecoder {

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public AuDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "au"
     */
    public String getFormatName() {
        return "au";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/basic", "audio/x-au", "audio/au", "audio/x-pn-au" };
    }

    /**
     * Checks the <code>.snd</code> signature.
     * <p>
     * <code>.snd</code> 시그니처를 확인한다.
     *
     * @param data the data / 데이터
     * @return true if it matches / 맞으면 true
     */
    public boolean acceptsSource(ByteSource data) {
        return Bytes.matches(data, 0, ".snd");
    }

    /**
     * Parses the header and creates the source of the form that was asked for (a {@link PcmSource} or a {@link FloatPcmSource}).
     * <p>
     * 헤더를 해석하고 요청한 형태의 소스({@link PcmSource} 또는 {@link FloatPcmSource})를 만든다.
     *
     * @param data the whole file / 파일 전체
     * @param limits resource limits / 리소스 제한
     * @param wide true for the wide path / 넓은 경로이면 true
     * @return the source / 소스
     * @throws MediaException on a damaged or unsupported file / 손상되었거나 지원하지 않는 파일이면
     */
    protected Object create(ByteSource data, DecodeLimits limits, boolean wide) throws MediaException {
        if (!Bytes.has(data, 0, 24)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the AU header is incomplete");
        }
        long offset = Bytes.be32(data, 4);
        long size = Bytes.be32(data, 8);
        long encoding = Bytes.be32(data, 12);
        long rate = Bytes.be32(data, 16);
        long channels = Bytes.be32(data, 20);
        if (offset < 24 || offset > data.length()) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid AU data offset");
        }
        if (channels < 1 || channels > 64 || rate < 1 || rate > 1000000) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid AU format values");
        }
        SampleFormat format = chooseFormat((int) encoding);
        long remaining = data.length() - offset;
        long length = (size == 0xFFFFFFFFL || size > remaining) ? remaining : size;
        return PcmSources.raw(wide, data, (int) offset, (int) (offset + length), format, (int) channels, (int) rate, null);
    }

    /**
     * Chooses the sample encoding from the AU encoding number.
     * <p>
     * AU 인코딩 번호로 샘플 인코딩을 고른다.
     *
     * @param encoding encoding number of the header / 헤더의 인코딩 번호
     * @return the encoding / 인코딩
     * @throws MediaException for encodings that are not supported / 지원하지 않는 인코딩이면
     */
    private static SampleFormat chooseFormat(int encoding) throws MediaException {
        switch (encoding) {
        case 1:
            return SampleFormat.ULAW;
        case 2:
            return SampleFormat.S8;
        case 3:
            return SampleFormat.S16BE;
        case 4:
            return SampleFormat.S24BE;
        case 5:
            return SampleFormat.S32BE;
        case 6:
            return SampleFormat.F32BE;
        case 7:
            return SampleFormat.F64BE;
        case 27:
            return SampleFormat.ALAW;
        default:
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported AU encoding " + encoding);
        }
    }
}
