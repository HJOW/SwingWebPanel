package org.duckdns.hjow.swingwebpanel.media.audio.aac;

/**
 * A big-endian bit reader over a byte range of an AAC raw data block. Reading past the end returns zero bits and moves the position beyond
 * the limit; the decoder checks {@link #overrun()} after each element.
 * <p>
 * AAC 원시 데이터 블록의 바이트 범위 위의 빅 엔디언 비트 읽기. 끝을 넘어 읽으면 0 비트를 돌려주고 위치가 한계를 넘어가며, 디코더는 요소마다
 * {@link #overrun()} 으로 확인한다.
 */
final class AacBits {

    /** Data bytes. / 데이터 바이트. */
    private final byte[] data;
    /** Position of the first bit (inclusive). / 첫 비트 위치(포함). */
    private final int start;
    /** End bit position (exclusive). / 끝 비트 위치(미포함). */
    private final int limit;
    /** Current absolute bit position. / 현재 절대 비트 위치. */
    private int pos;

    /**
     * Creates a reader over a byte range.
     * <p>
     * 바이트 범위를 읽는 객체를 만든다.
     *
     * @param data   data / 데이터
     * @param offset first byte / 첫 바이트
     * @param length number of bytes / 바이트 수
     */
    AacBits(byte[] data, int offset, int length) {
        this.data = data;
        this.start = offset * 8;
        this.limit = (offset + length) * 8;
        this.pos = start;
    }

    /**
     * Reads one bit. / 비트 하나를 읽는다.
     *
     * @return 0 or 1 / 0 또는 1
     */
    int bit() {
        int p = pos++;
        if (p >= limit) {
            return 0;
        }
        return (data[p >> 3] >> (7 - (p & 7))) & 1;
    }

    /**
     * Reads one bit as a flag. / 비트 하나를 불리언으로 읽는다.
     *
     * @return true for 1 / 1 이면 true
     */
    boolean flag() {
        return bit() != 0;
    }

    /**
     * Reads up to 25 bits. / 최대 25비트를 읽는다.
     *
     * @param n number of bits / 비트 수
     * @return value / 값
     */
    int bits(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) { // 비트를 하나씩 모아 값을 만든다
            v = (v << 1) | bit();
        }
        return v;
    }

    /**
     * Skips bits. / 비트를 건너뛴다.
     *
     * @param n number of bits / 비트 수
     */
    void skip(int n) {
        pos += n;
    }

    /**
     * Moves to the next byte boundary. / 다음 바이트 경계로 간다.
     */
    void align() {
        int rel = (pos - start) & 7;
        if (rel != 0) {
            pos += 8 - rel;
        }
    }

    /**
     * Returns the number of bits read so far. / 지금까지 읽은 비트 수를 반환한다.
     *
     * @return bits / 비트 수
     */
    int position() {
        return pos - start;
    }

    /**
     * Returns the number of bits left. / 남은 비트 수를 반환한다.
     *
     * @return bits / 비트 수
     */
    int left() {
        return limit - pos;
    }

    /**
     * Tells whether a read went past the end. / 끝을 넘어 읽었는지 알려 준다.
     *
     * @return true if past the end / 넘었으면 true
     */
    boolean overrun() {
        return pos > limit;
    }
}
