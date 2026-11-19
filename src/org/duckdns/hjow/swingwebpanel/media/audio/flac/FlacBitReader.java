package org.duckdns.hjow.swingwebpanel.media.audio.flac;

/**
 * Most-significant-bit-first reader of a byte range, as used by the FLAC format. Reading beyond the end of the range throws
 * {@link FlacFrameDecoder.Truncated}: the frame is cut off.
 * <p>
 * FLAC 형식이 쓰는, 바이트 범위를 최상위 비트부터 읽는 읽기 객체. 범위 끝을 넘어 읽으면 {@link FlacFrameDecoder.Truncated} 를 던진다: 프레임이 잘린 것이다.
 */
final class FlacBitReader {

    /**
     * The data.
     * <p>
     * 데이터.
     */
    private final byte[] d;

    /**
     * End offset (exclusive) in bytes.
     * <p>
     * 바이트 단위 끝 위치(미포함).
     */
    private final int end;

    /**
     * Current position in bits from the start of the array.
     * <p>
     * 배열 시작부터의 현재 위치(비트).
     */
    private long bit;

    /**
     * Creates a reader.
     * <p>
     * 읽기 객체를 만든다.
     *
     * @param d     data / 데이터
     * @param start first byte / 첫 바이트
     * @param end   end of the range (exclusive) / 범위의 끝(미포함)
     */
    FlacBitReader(byte[] d, int start, int end) {
        this.d = d;
        this.end = end;
        this.bit = (long) start * 8;
    }

    /**
     * Reads up to 32 bits as an unsigned number.
     * <p>
     * 최대 32비트를 부호 없는 수로 읽는다.
     *
     * @param n number of bits, 0..32 / 비트 수(0~32)
     * @return the value / 값
     */
    long read(int n) {
        long r = 0;
        int left = n;
        // 바이트 경계를 따라 필요한 비트를 모은다.
        while (left > 0) {
            int idx = (int) (bit >>> 3);
            if (idx >= end) {
                throw new FlacFrameDecoder.Truncated();
            }
            int avail = 8 - (int) (bit & 7);
            int take = Math.min(avail, left);
            int b = d[idx] & 0xFF;
            r = (r << take) | ((b >> (avail - take)) & ((1 << take) - 1));
            bit += take;
            left -= take;
        }
        return r;
    }

    /**
     * Reads a two's complement number.
     * <p>
     * 2의 보수 수를 읽는다.
     *
     * @param n number of bits, 0..32 / 비트 수(0~32)
     * @return the signed value / 부호 있는 값
     */
    long readSigned(int n) {
        if (n == 0) {
            return 0;
        }
        long v = read(n);
        return (v & (1L << (n - 1))) != 0 ? v - (1L << n) : v;
    }

    /**
     * Counts zero bits up to the next one bit and consumes that bit.
     * <p>
     * 다음 1 비트까지의 0 비트 수를 세고 그 1 비트를 소비한다.
     *
     * @return number of zeros / 0 의 개수
     */
    long readUnary() {
        long zeros = 0;
        // 1 비트가 나올 때까지 비트를 하나씩 읽는다.
        while (read(1) == 0) {
            zeros++;
        }
        return zeros;
    }

    /**
     * Skips to the next byte boundary.
     * <p>
     * 다음 바이트 경계로 건너뛴다.
     */
    void align() {
        bit = (bit + 7) & ~7L;
    }

    /**
     * Returns the current byte position (the reader must be aligned).
     * <p>
     * 현재 바이트 위치를 반환한다(정렬된 상태여야 한다).
     *
     * @return offset / 위치
     */
    int bytePosition() {
        return (int) (bit >>> 3);
    }
}
