package org.duckdns.hjow.swingwebpanel.media.audio.testkit;

import java.io.ByteArrayOutputStream;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;

/**
 * Test-only builders of audio files and helpers to read decoded PCM. The writers are written from the format specifications
 * independently of the decoders, so a round trip through them checks the decoders.
 * <p>
 * 시험 전용의 오디오 파일 생성기와 디코딩된 PCM 을 읽는 도우미. 쓰기 코드는 디코더와 별개로 형식 규격에서 직접 작성하므로 이를 거친 왕복이
 * 디코더를 검증한다.
 */
public final class AudioKit {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private AudioKit() {
    }

    /**
     * A growable byte sink with endian-aware writers.
     * <p>
     * 엔디언을 가리는 쓰기 메서드를 가진 늘어나는 바이트 출력.
     */
    public static final class Out {
        /**
         * The bytes written so far.
         * <p>
         * 지금까지 쓴 바이트.
         */
        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        /**
         * Writes one byte.
         * <p>
         * 바이트 하나를 쓴다.
         *
         * @param v value (low 8 bits) / 값(하위 8비트)
         * @return this / 자신
         */
        public Out u8(int v) {
            bos.write(v);
            return this;
        }

        /**
         * Writes a 16-bit little-endian value.
         * <p>
         * 16비트 리틀 엔디언 값을 쓴다.
         *
         * @param v value / 값
         * @return this / 자신
         */
        public Out le16(int v) {
            bos.write(v);
            bos.write(v >> 8);
            return this;
        }

        /**
         * Writes a 32-bit little-endian value.
         * <p>
         * 32비트 리틀 엔디언 값을 쓴다.
         *
         * @param v value / 값
         * @return this / 자신
         */
        public Out le32(long v) {
            le16((int) v);
            le16((int) (v >> 16));
            return this;
        }

        /**
         * Writes a 16-bit big-endian value.
         * <p>
         * 16비트 빅 엔디언 값을 쓴다.
         *
         * @param v value / 값
         * @return this / 자신
         */
        public Out be16(int v) {
            bos.write(v >> 8);
            bos.write(v);
            return this;
        }

        /**
         * Writes a 32-bit big-endian value.
         * <p>
         * 32비트 빅 엔디언 값을 쓴다.
         *
         * @param v value / 값
         * @return this / 자신
         */
        public Out be32(long v) {
            be16((int) (v >> 16));
            be16((int) v);
            return this;
        }

        /**
         * Writes a 24-bit big-endian value.
         * <p>
         * 24비트 빅 엔디언 값을 쓴다.
         *
         * @param v value / 값
         * @return this / 자신
         */
        public Out be24(int v) {
            bos.write(v >> 16);
            bos.write(v >> 8);
            bos.write(v);
            return this;
        }

        /**
         * Writes ASCII text.
         * <p>
         * ASCII 문자열을 쓴다.
         *
         * @param s text / 문자열
         * @return this / 자신
         */
        public Out ascii(String s) {
            // 글자를 하나씩 바이트로 쓴다.
            for (int i = 0; i < s.length(); i++) {
                bos.write(s.charAt(i));
            }
            return this;
        }

        /**
         * Writes bytes.
         * <p>
         * 바이트들을 쓴다.
         *
         * @param b bytes / 바이트
         * @return this / 자신
         */
        public Out bytes(byte[] b) {
            bos.write(b, 0, b.length);
            return this;
        }

        /**
         * Returns the number of bytes written.
         * <p>
         * 쓴 바이트 수를 반환한다.
         *
         * @return size / 크기
         */
        public int size() {
            return bos.size();
        }

        /**
         * Returns the bytes written.
         * <p>
         * 쓴 바이트를 반환한다.
         *
         * @return bytes / 바이트
         */
        public byte[] toByteArray() {
            return bos.toByteArray();
        }
    }

    /**
     * Reads a whole source into one byte array of 16-bit little-endian PCM.
     * <p>
     * 소스 전체를 16비트 리틀 엔디언 PCM 바이트 배열 하나로 읽는다.
     *
     * @param s source / 소스
     * @return the PCM bytes / PCM 바이트
     * @throws MediaException if a read fails / 읽기가 실패하면
     */
    public static byte[] readAll(PcmSource s) throws MediaException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[3000];
        // 끝(-1)이 나올 때까지 읽는다.
        for (;;) {
            int n = s.read(buf, 0, buf.length);
            if (n < 0) {
                break;
            }
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /**
     * Converts 16-bit little-endian PCM bytes to shorts.
     * <p>
     * 16비트 리틀 엔디언 PCM 바이트를 short 로 바꾼다.
     *
     * @param pcm bytes / 바이트
     * @return samples / 샘플
     */
    public static short[] toShorts(byte[] pcm) {
        short[] r = new short[pcm.length / 2];
        // 두 바이트씩 묶어 short 로 만든다.
        for (int i = 0; i < r.length; i++) {
            r[i] = (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8));
        }
        return r;
    }

    /**
     * Converts shorts to 16-bit little-endian PCM bytes.
     * <p>
     * short 를 16비트 리틀 엔디언 PCM 바이트로 바꾼다.
     *
     * @param s samples / 샘플
     * @return bytes / 바이트
     */
    public static byte[] toBytes(short[] s) {
        byte[] r = new byte[s.length * 2];
        // 샘플마다 두 바이트로 푼다.
        for (int i = 0; i < s.length; i++) {
            r[2 * i] = (byte) s[i];
            r[2 * i + 1] = (byte) (s[i] >> 8);
        }
        return r;
    }

    /**
     * Builds a WAV file.
     * <p>
     * WAV 파일을 만든다.
     *
     * @param tag      format tag (the subformat tag when a mask is given) / 형식 태그(마스크를 주면 서브포맷 태그)
     * @param channels channels / 채널 수
     * @param rate     sample rate / 샘플링 주파수
     * @param bits     bits per sample / 샘플당 비트 수
     * @param data     the data chunk body / data 청크 본문
     * @param mask     speaker mask for WAVE_FORMAT_EXTENSIBLE, or -1 for a plain format chunk / WAVE_FORMAT_EXTENSIBLE 의 스피커 마스크. 일반 fmt 청크면 -1
     * @return file bytes / 파일 바이트
     */
    public static byte[] wav(int tag, int channels, int rate, int bits, byte[] data, long mask) {
        int blockAlign = channels * ((bits + 7) / 8);
        Out fmt = new Out();
        fmt.le16(mask >= 0 ? 0xFFFE : tag).le16(channels).le32(rate).le32((long) rate * blockAlign).le16(blockAlign).le16(bits);
        if (mask >= 0) {
            fmt.le16(22).le16(bits).le32(mask).le16(tag);
            fmt.bytes(new byte[] { 0, 0, 0, 0, 0x10, 0, (byte) 0x80, 0, 0, (byte) 0xAA, 0, 0x38, (byte) 0x9B, 0x71 });
        }
        return wavFromFmt(fmt.toByteArray(), data, data.length);
    }

    /**
     * Builds a WAV file from a prepared format chunk body.
     * <p>
     * 준비된 fmt 청크 본문으로 WAV 파일을 만든다.
     *
     * @param fmtBody   body of the format chunk / fmt 청크 본문
     * @param data      body of the data chunk / data 청크 본문
     * @param sizeField value written as the size of the data chunk / data 청크의 크기로 쓰는 값
     * @return file bytes / 파일 바이트
     */
    public static byte[] wavFromFmt(byte[] fmtBody, byte[] data, long sizeField) {
        Out o = new Out();
        o.ascii("RIFF").le32(4 + 8 + fmtBody.length + (fmtBody.length & 1) + 8 + data.length + (data.length & 1)).ascii("WAVE");
        o.ascii("fmt ").le32(fmtBody.length).bytes(fmtBody);
        if ((fmtBody.length & 1) != 0) {
            o.u8(0);
        }
        o.ascii("data").le32(sizeField).bytes(data);
        if ((data.length & 1) != 0) {
            o.u8(0);
        }
        return o.toByteArray();
    }

    /**
     * Builds an AU file.
     * <p>
     * AU 파일을 만든다.
     *
     * @param encoding  encoding number / 인코딩 번호
     * @param channels  channels / 채널 수
     * @param rate      sample rate / 샘플링 주파수
     * @param data      sample bytes / 샘플 바이트
     * @param sizeField value written as the data size / 데이터 크기로 쓰는 값
     * @return file bytes / 파일 바이트
     */
    public static byte[] au(int encoding, int channels, int rate, byte[] data, long sizeField) {
        Out o = new Out();
        o.ascii(".snd").be32(24).be32(sizeField).be32(encoding).be32(rate).be32(channels).bytes(data);
        return o.toByteArray();
    }

    /**
     * Writes an integer sample rate as an 80-bit IEEE extended number.
     * <p>
     * 정수 샘플링 주파수를 80비트 IEEE 확장 수로 쓴다.
     *
     * @param o    sink / 출력
     * @param rate sample rate / 샘플링 주파수
     */
    public static void extended80(Out o, long rate) {
        int e = 63 - Long.numberOfLeadingZeros(rate);
        long m = rate << (63 - e);
        o.be16(16383 + e).be32(m >>> 32).be32(m & 0xFFFFFFFFL);
    }

    /**
     * Builds an AIFF or AIFF-C file.
     * <p>
     * AIFF 또는 AIFF-C 파일을 만든다.
     *
     * @param comp     AIFF-C compression type, or null for plain AIFF / AIFF-C 압축 유형. 일반 AIFF 면 null
     * @param channels channels / 채널 수
     * @param bits     sample size in bits / 샘플 크기(비트)
     * @param rate     sample rate / 샘플링 주파수
     * @param frames   number of frames written in the common chunk / COMM 청크에 쓰는 프레임 수
     * @param data     sample bytes / 샘플 바이트
     * @return file bytes / 파일 바이트
     */
    public static byte[] aiff(String comp, int channels, int bits, int rate, long frames, byte[] data) {
        Out comm = new Out();
        comm.be16(channels).be32(frames).be16(bits);
        extended80(comm, rate);
        if (comp != null) {
            comm.ascii(comp).u8(0).u8(0);
        }
        Out o = new Out();
        o.ascii("FORM");
        byte[] c = comm.toByteArray();
        o.be32(4 + 8 + c.length + 8 + 8 + data.length + (data.length & 1)).ascii(comp != null ? "AIFC" : "AIFF");
        o.ascii("COMM").be32(c.length).bytes(c);
        o.ascii("SSND").be32(8 + data.length).be32(0).be32(0).bytes(data);
        if ((data.length & 1) != 0) {
            o.u8(0);
        }
        return o.toByteArray();
    }

    /**
     * Encodes a linear sample as G.711 mu-law (reference algorithm).
     * <p>
     * 선형 샘플을 G.711 mu-law 로 인코딩한다(기준 알고리즘).
     *
     * @param pcm 16-bit sample / 16비트 샘플
     * @return mu-law byte / mu-law 바이트
     */
    public static int ulawEncode(int pcm) {
        int mask = 0xFF;
        int v = pcm >> 2;
        if (v < 0) {
            v = -v;
            mask = 0x7F;
        }
        v += 33;
        if (v > 0x1FFF) {
            v = 0x1FFF;
        }
        int seg = 0;
        // 구간(segment)을 찾는다.
        while (seg < 7 && v >= (0x40 << seg)) {
            seg++;
        }
        int uval = (seg << 4) | ((v >> (seg + 1)) & 0xF);
        return uval ^ mask;
    }

    /**
     * Encodes a linear sample as G.711 A-law (reference algorithm).
     * <p>
     * 선형 샘플을 G.711 A-law 로 인코딩한다(기준 알고리즘).
     *
     * @param pcm 16-bit sample / 16비트 샘플
     * @return A-law byte / A-law 바이트
     */
    public static int alawEncode(int pcm) {
        int mask;
        int v = pcm >> 3;
        if (v >= 0) {
            mask = 0xD5;
        } else {
            mask = 0x55;
            v = -v - 1;
        }
        int seg = 0;
        // 구간을 찾는다.
        while (seg < 8 && v >= (0x20 << seg)) {
            seg++;
        }
        if (seg >= 8) {
            return 0x7F ^ mask;
        }
        int aval = seg << 4;
        aval |= seg < 2 ? (v >> 1) & 0xF : (v >> seg) & 0xF;
        return aval ^ mask;
    }
}
