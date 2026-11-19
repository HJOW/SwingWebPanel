package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.Bytes;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacket;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggPacketStream;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggReader;
import org.duckdns.hjow.swingwebpanel.media.audio.ogg.OggStream;

/**
 * Decoder of FLAC (plan item P18-07, FEATURE_MATRIX AUD-07): native FLAC files (optionally behind an ID3v2 tag) and FLAC in Ogg. All
 * subframe types, all channel assignments, sample sizes of 4 to 32 bits (rounded to 16 bits), fixed and variable block sizes, frame
 * CRC-8 and CRC-16 checks and seeking (SEEKTABLE or a frame index) are supported. Details of the damaged-data policy are in
 * {@link NativeFlacSource}.
 * <p>
 * FLAC 디코더(계획 P18-07, FEATURE_MATRIX AUD-07): 네이티브 FLAC 파일(앞에 ID3v2 태그가 있어도 됨)과 Ogg 안의 FLAC. 모든 서브프레임 유형, 모든 채널
 * 배정, 4~32비트 샘플 크기(16비트로 반올림), 고정/가변 블록 크기, 프레임 CRC-8/CRC-16 확인, 탐색(SEEKTABLE 또는 프레임 색인)을 지원한다. 손상된
 * 데이터를 다루는 정책은 {@link NativeFlacSource} 에 있다.
 */
public final class FlacDecoder implements AudioDecoder {

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     */
    public FlacDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return "flac"
     */
    public String getFormatName() {
        return "flac";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "audio/flac", "audio/x-flac" };
    }

    /**
     * Recognizes native FLAC (after an optional ID3v2 tag) and Ogg FLAC.
     * <p>
     * 네이티브 FLAC(ID3v2 태그가 앞에 있어도 됨)과 Ogg FLAC 을 알아본다.
     *
     * @param data the data / 데이터
     * @return true if it is FLAC / FLAC 이면 true
     */
    public boolean accepts(byte[] data) {
        if (Bytes.matches(data, nativeStart(data), "fLaC")) {
            return true;
        }
        return OggReader.startsWithOgg(data) && OggReader.sniffCodecs(data).contains("flac");
    }

    /**
     * Returns the offset where <code>fLaC</code> is expected: 0, or after an ID3v2 tag.
     * <p>
     * <code>fLaC</code> 가 있어야 할 위치를 반환한다: 0, 또는 ID3v2 태그 뒤.
     *
     * @param d data / 데이터
     * @return offset / 위치
     */
    private static int nativeStart(byte[] d) {
        if (Bytes.matches(d, 0, "ID3") && Bytes.has(d, 0, 10)) {
            int size = ((d[6] & 0x7F) << 21) | ((d[7] & 0x7F) << 14) | ((d[8] & 0x7F) << 7) | (d[9] & 0x7F);
            boolean footer = (d[5] & 0x10) != 0;
            long off = 10L + size + (footer ? 10 : 0);
            return off > d.length ? d.length : (int) off;
        }
        return 0;
    }

    /**
     * Opens the data for decoding.
     * <p>
     * 디코딩을 위해 데이터를 연다.
     *
     * @param data   the data / 데이터
     * @param limits limits / 제한
     * @return PCM source / PCM 소스
     * @throws MediaException on damaged or unsupported data / 손상되었거나 지원하지 않는 데이터일 때
     */
    public PcmSource open(byte[] data, DecodeLimits limits) throws MediaException {
        int start = nativeStart(data);
        if (Bytes.matches(data, start, "fLaC")) {
            return openNative(data, start + 4);
        }
        return openOgg(data);
    }

    /**
     * Opens a native FLAC file whose metadata starts at an offset.
     * <p>
     * 메타데이터가 위치에서 시작하는 네이티브 FLAC 파일을 연다.
     *
     * @param d   data / 데이터
     * @param pos offset of the first metadata block header / 첫 메타데이터 블록 헤더의 위치
     * @return the source / 소스
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    private static PcmSource openNative(byte[] d, int pos) throws MediaException {
        FlacStreamInfo info = null;
        List<long[]> points = new ArrayList<long[]>();
        boolean last = false;
        int p = pos;
        // 마지막 블록 표시가 나올 때까지 메타데이터 블록을 하나씩 읽는다.
        while (!last) {
            if (!Bytes.has(d, p, 4)) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the FLAC metadata is cut off");
            }
            last = (d[p] & 0x80) != 0;
            int type = d[p] & 0x7F;
            int len = Bytes.be24(d, p + 1);
            int body = p + 4;
            if (type == 127 || !Bytes.has(d, body, len)) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC metadata block");
            }
            if (info == null) {
                if (type != 0 || len != FlacStreamInfo.SIZE) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "the first FLAC metadata block is not STREAMINFO");
                }
                info = new FlacStreamInfo(d, body);
            } else if (type == 3) {
                // 탐색 표의 18바이트 항목에서 자리 표시(샘플 번호가 모두 1)가 아닌 것만 모은다.
                for (int i = 0; i + 18 <= len; i += 18) {
                    long sample = (Bytes.be32(d, body + i) << 32) | Bytes.be32(d, body + i + 4);
                    long offset = (Bytes.be32(d, body + i + 8) << 32) | Bytes.be32(d, body + i + 12);
                    if (sample != -1L) {
                        points.add(new long[] { sample, offset });
                    }
                }
            }
            p = body + len;
        }
        long[] samples = new long[points.size()];
        long[] offsets = new long[points.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < samples.length; i++) {
            samples[i] = points.get(i)[0];
            offsets[i] = points.get(i)[1];
        }
        return new NativeFlacSource(d, info, p, samples, offsets);
    }

    /**
     * Opens FLAC in Ogg.
     * <p>
     * Ogg 안의 FLAC 을 연다.
     *
     * @param d data / 데이터
     * @return the source / 소스
     * @throws MediaException on damaged data / 손상된 데이터일 때
     */
    private static PcmSource openOgg(byte[] d) throws MediaException {
        OggReader reader = OggReader.scan(d);
        OggStream stream = reader.findFirst("flac");
        if (stream == null) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "no FLAC stream in the Ogg file");
        }
        byte[] id = stream.getFirstPacket();
        if (id == null || id.length < 13 + 4 + FlacStreamInfo.SIZE || !Bytes.matches(id, 9, "fLaC")) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Ogg FLAC identification header");
        }
        FlacStreamInfo info = new FlacStreamInfo(id, 13 + 4);
        OggPacketStream packets = stream.openPackets(0);
        OggPacket p = packets.next();
        int headerPage = p == null ? 0 : p.getPageIndex();
        OggPacket firstAudio = null;
        // 첫 패킷 뒤에서 오디오 프레임으로 시작하지 않는 패킷(메타데이터)을 지나 첫 오디오 패킷을 찾는다.
        for (;;) {
            p = packets.next();
            if (p == null) {
                break;
            }
            byte[] b = p.getData();
            if (b.length >= 2 && FlacFrameDecoder.isSync(b[0], b[1])) {
                firstAudio = p;
                break;
            }
            headerPage = p.getPageIndex();
        }
        return new OggFlacSource(stream, info, headerPage, packets, firstAudio);
    }
}
