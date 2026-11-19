package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A big-endian bit reader over a byte array for the fixed length descriptors of the AV1 syntax (plan item P18-03): <code>f(n)</code>,
 * <code>leb128()</code>, <code>su(n)</code>, <code>ns(n)</code>, <code>uvlc()</code> and byte alignment. Reading past the end of the data
 * fails with a CORRUPT {@link MediaException}.
 * <p>
 * AV1 구문의 고정 길이 서술자를 위한 바이트 배열 위의 빅 엔디언 비트 읽기(계획 P18-03): <code>f(n)</code>, <code>leb128()</code>,
 * <code>su(n)</code>, <code>ns(n)</code>, <code>uvlc()</code>, 바이트 정렬. 데이터 끝을 넘어 읽으면 CORRUPT {@link MediaException} 으로 실패한다.
 */
final class Av1BitReader {

    /**
     * The data.
     * <p>
     * 데이터.
     */
    private final byte[] data;

    /**
     * The current position in bits.
     * <p>
     * 현재 위치(비트).
     */
    private long pos;

    /**
     * The end of the readable data in bits.
     * <p>
     * 읽을 수 있는 데이터의 끝(비트).
     */
    private final long end;

    /**
     * Creates a reader over a range of a byte array.
     * <p>
     * 바이트 배열의 구간에 대한 읽기를 만든다.
     *
     * @param data   the data / 데이터
     * @param offset the first byte / 첫 바이트
     * @param length the number of bytes / 바이트 수
     */
    Av1BitReader(byte[] data, int offset, int length) {
        this.data = data;
        this.pos = (long) offset * 8;
        this.end = (long) (offset + length) * 8;
    }

    /**
     * Returns the current position in bits.
     * <p>
     * 현재 위치(비트)를 반환한다.
     *
     * @return the position / 위치
     */
    long position() {
        return pos;
    }

    /**
     * Returns the current byte position (rounded up if the reader is not aligned).
     * <p>
     * 현재 바이트 위치를 반환한다(정렬되어 있지 않으면 올림).
     *
     * @return the byte position / 바이트 위치
     */
    int bytePosition() {
        return (int) ((pos + 7) >> 3);
    }

    /**
     * Reads one bit.
     * <p>
     * 비트 하나를 읽는다.
     *
     * @return 0 or 1 / 0 또는 1
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    int bit() throws MediaException {
        if (pos >= end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AV1 header data ends unexpectedly");
        }
        int b = (data[(int) (pos >> 3)] >> (7 - (int) (pos & 7))) & 1;
        pos++;
        return b;
    }

    /**
     * Reads an unsigned value of n bits, <code>f(n)</code>.
     * <p>
     * n 비트의 부호 없는 값 <code>f(n)</code> 을 읽는다.
     *
     * @param n the number of bits (0 to 32) / 비트 수(0~32)
     * @return the value (as a long for 32 bits) / 값(32비트는 long)
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    long bitsLong(int n) throws MediaException {
        long v = 0;
        // 비트를 하나씩 이어 붙여 값을 만든다.
        for (int i = 0; i < n; i++) {
            v = (v << 1) | bit();
        }
        return v;
    }

    /**
     * Reads an unsigned value of up to 31 bits.
     * <p>
     * 최대 31 비트의 부호 없는 값을 읽는다.
     *
     * @param n the number of bits / 비트 수
     * @return the value / 값
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    int bits(int n) throws MediaException {
        return (int) bitsLong(n);
    }

    /**
     * Reads a flag.
     * <p>
     * 플래그를 읽는다.
     *
     * @return true for 1 / 1 이면 true
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    boolean flag() throws MediaException {
        return bit() != 0;
    }

    /**
     * Reads a signed value of n bits, <code>su(n)</code> (the n includes the sign bit).
     * <p>
     * n 비트의 부호 있는 값 <code>su(n)</code> 을 읽는다(n 에 부호 비트가 들어 있다).
     *
     * @param n the number of bits / 비트 수
     * @return the value / 값
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    int su(int n) throws MediaException {
        int v = bits(n);
        int signMask = 1 << (n - 1);
        if ((v & signMask) != 0) {
            v = v - 2 * signMask;
        }
        return v;
    }

    /**
     * Reads a non-symmetric unsigned value less than n, <code>ns(n)</code>.
     * <p>
     * n 보다 작은 비대칭 부호 없는 값 <code>ns(n)</code> 을 읽는다.
     *
     * @param n the number of possible values / 가능한 값의 수
     * @return the value / 값
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    int ns(int n) throws MediaException {
        int w = 0;
        int x = n;
        // n 을 표현하는 데 필요한 비트 수 w 를 구한다.
        while (x != 0) {
            x >>= 1;
            w++;
        }
        int m = (1 << w) - n;
        int v = bits(w - 1);
        if (v < m) {
            return v;
        }
        int extra = bit();
        return (v << 1) - m + extra;
    }

    /**
     * Reads an unsigned little endian base 128 value, <code>leb128()</code>.
     * <p>
     * 부호 없는 리틀 엔디언 base 128 값 <code>leb128()</code> 을 읽는다.
     *
     * @return the value / 값
     * @throws MediaException CORRUPT for a value that is too long or the end of the data / 값이 너무 길거나 데이터 끝이면 CORRUPT
     */
    long leb128() throws MediaException {
        long value = 0;
        // 최대 8 바이트까지 하위 7비트씩 이어 붙인다.
        for (int i = 0; i < 8; i++) {
            int b = bits(8);
            value |= (long) (b & 0x7F) << (i * 7);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return value;
    }

    /**
     * Reads a variable length code, <code>uvlc()</code>.
     * <p>
     * 가변 길이 부호 <code>uvlc()</code> 를 읽는다.
     *
     * @return the value / 값
     * @throws MediaException CORRUPT at the end of the data / 데이터 끝이면 CORRUPT
     */
    long uvlc() throws MediaException {
        int leadingZeros = 0;
        // 앞쪽의 0 비트 수를 센다(1 비트를 만나면 멈춘다).
        while (true) {
            if (bit() != 0) {
                break;
            }
            leadingZeros++;
            if (leadingZeros >= 32) {
                return (1L << 32) - 1;
            }
        }
        return bitsLong(leadingZeros) + (1L << leadingZeros) - 1;
    }

    /**
     * Skips to the next byte boundary.
     * <p>
     * 다음 바이트 경계까지 건너뛴다.
     */
    void byteAlign() {
        pos = (pos + 7) & ~7L;
    }
}
