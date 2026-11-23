package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

/**
 * Bit reader of a Vorbis packet: bits are taken from the least significant end of every byte, and a value that spans bytes has its first
 * bits in the low positions. Reading past the end of the packet is not an exception: it returns zeros and sets the end-of-packet flag,
 * which the decoding algorithms treat as the end of data as the Vorbis specification prescribes.
 * <p>
 * Vorbis 패킷의 비트 읽기 객체: 비트는 각 바이트의 최하위 쪽부터 가져오고, 바이트에 걸친 값은 먼저 읽은 비트가 낮은 자리에 온다. 패킷 끝을 넘어
 * 읽어도 예외가 아니라 0 을 반환하고 패킷 끝 플래그를 켜며, 디코딩 알고리즘은 Vorbis 규격이 정한 대로 이를 데이터의 끝으로 처리한다.
 */
final class VorbisBitReader {

    /**
     * The packet.
     * <p>
     * 패킷.
     */
    private final byte[] d;

    /**
     * Total number of bits.
     * <p>
     * 전체 비트 수.
     */
    private final long total;

    /**
     * Position in bits.
     * <p>
     * 비트 단위 위치.
     */
    private long bit;

    /**
     * True once a read went past the end.
     * <p>
     * 읽기가 끝을 넘은 뒤에는 true.
     */
    private boolean eop;

    /**
     * Creates a reader over a packet.
     * <p>
     * 패킷에 대한 읽기 객체를 만든다.
     *
     * @param d packet / 패킷
     */
    VorbisBitReader(byte[] d) {
        this.d = d;
        this.total = (long) d.length * 8;
    }

    /**
     * Reads <code>n</code> bits (0 to 32) as the bit pattern of an int; the value of 32 bits may be negative.
     * <p>
     * <code>n</code> 비트(0~32)를 int 의 비트 패턴으로 읽는다. 32비트 값은 음수일 수 있다.
     *
     * @param n number of bits / 비트 수
     * @return the value, or 0 and the end-of-packet flag if the packet is too short / 값. 패킷이 짧으면 0 이고 패킷 끝 플래그가 켜진다
     */
    int read(int n) {
        if (n == 0) {
            return 0;
        }
        if (bit + n > total) {
            eop = true;
            bit = total;
            return 0;
        }
        long result = 0;
        int got = 0;
        // 바이트 경계를 따라 비트를 모아 낮은 자리부터 채운다.
        while (got < n) {
            int idx = (int) (bit >>> 3);
            int off = (int) (bit & 7);
            int take = Math.min(8 - off, n - got);
            result |= (long) (((d[idx] & 0xFF) >> off) & ((1 << take) - 1)) << got;
            bit += take;
            got += take;
        }
        return (int) result;
    }

    /**
     * Tells whether a read went past the end of the packet.
     * <p>
     * 읽기가 패킷의 끝을 넘었는지 알려 준다.
     *
     * @return true at the end of the packet / 패킷 끝이면 true
     */
    boolean isEndOfPacket() {
        return eop;
    }

    /**
     * Returns the number of bits that are left.
     * <p>
     * 남은 비트 수를 반환한다.
     *
     * @return bits left / 남은 비트 수
     */
    long bitsLeft() {
        return total - bit;
    }

    /**
     * The number of bits needed to represent a number: 0 for 0, 1 for 1, 2 for 2 and 3, 3 for 4 to 7 and so on; 0 for negative numbers.
     * <p>
     * 수를 표현하는 데 필요한 비트 수: 0 이면 0, 1 이면 1, 2 와 3 이면 2, 4~7 이면 3 등. 음수는 0.
     *
     * @param x the number / 수
     * @return the bit count / 비트 수
     */
    static int ilog(int x) {
        return x <= 0 ? 0 : 32 - Integer.numberOfLeadingZeros(x);
    }
}
