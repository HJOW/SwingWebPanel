package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

/**
 * A big-endian bit reader over a byte array used for the side information and the main data of Layer III frames. Reading past the end
 * returns zero bits and moves the position beyond the limit so that the caller can detect the overrun with {@link #overrun()}.
 * <p>
 * Layer III 프레임의 부가 정보와 주 데이터를 읽는 바이트 배열 위의 빅 엔디언 비트 읽기. 끝을 넘어 읽으면 0 비트를 돌려주고 위치가 한계를
 * 넘어가므로 호출자는 {@link #overrun()} 으로 초과를 알 수 있다.
 */
final class Mp3Bits {

    /**
     * Data bytes.
     * <p>
     * 데이터.
     */
    private final byte[] data;

    /**
     * Number of valid bits (limit of the reader).
     * <p>
     * 유효한 비트 수(읽기 한계).
     */
    private final int limit;

    /**
     * Current bit position.
     * <p>
     * 현재 비트 위치.
     */
    private int pos;

    /**
     * Creates a reader.
     * <p>
     * 읽기 객체를 만든다.
     *
     * @param data   data / 데이터
     * @param offset first byte / 첫 바이트 위치
     * @param length number of bytes / 바이트 수
     */
    Mp3Bits(byte[] data, int offset, int length) {
        if (offset == 0) {
            this.data = data;
        } else {
            byte[] copy = new byte[length];
            System.arraycopy(data, offset, copy, 0, length);
            this.data = copy;
        }
        this.limit = length * 8;
    }

    /**
     * Returns the current bit position.
     * <p>
     * 현재 비트 위치를 반환한다.
     *
     * @return position / 위치
     */
    int position() {
        return pos;
    }

    /**
     * Sets the bit position.
     * <p>
     * 비트 위치를 설정한다.
     *
     * @param p position / 위치
     */
    void seek(int p) {
        this.pos = p;
    }

    /**
     * Tells whether a read went beyond the end of the data.
     * <p>
     * 데이터 끝을 넘어 읽었는지 알려 준다.
     *
     * @return true if the position is beyond the limit / 위치가 한계를 넘었으면 true
     */
    boolean overrun() {
        return pos > limit;
    }

    /**
     * Returns the number of bits in the data.
     * <p>
     * 데이터의 비트 수를 반환한다.
     *
     * @return bits / 비트 수
     */
    int limit() {
        return limit;
    }

    /**
     * Reads one bit.
     * <p>
     * 비트 하나를 읽는다.
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
     * Reads up to 24 bits.
     * <p>
     * 최대 24비트를 읽는다.
     *
     * @param n number of bits (0 to 24) / 비트 수(0~24)
     * @return the value / 값
     */
    int bits(int n) {
        int v = 0;
        // 비트를 하나씩 모아 값을 만든다(최대 24비트이므로 충분히 빠르다).
        for (int i = 0; i < n; i++) {
            v = (v << 1) | bit();
        }
        return v;
    }
}
