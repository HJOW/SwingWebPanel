package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * GHASH, the universal hash of GCM (NIST SP 800-38D): the running value is multiplied by the hash key H in GF(2^128) after every
 * 16 byte block. The multiplication uses no table and no data dependent branch: a carry-less 64 bit product is built from ordinary
 * integer products of operands with "holes" (three zero bits between data bits, so that carries cannot reach the next data bit).
 * Example:
 * <pre>
 * Ghash g = new Ghash(h);
 * g.update(aad, 0, aad.length);         // every update is padded with zeros to a block boundary
 * g.update(ciphertext, 0, n);
 * byte[] y = g.finish(aad.length, n);   // adds the length block
 * </pre>
 * <p>
 * GCM 의 범용 해시 GHASH(NIST SP 800-38D): 16바이트 블록마다 누적 값에 GF(2^128) 에서 해시 키 H 를 곱한다. 곱셈에는 표와 데이터에 따른 분기가
 * 없다: 캐리 없는 64비트 곱을 "구멍"이 있는 피연산자(데이터 비트 사이에 0 비트 셋을 두어 캐리가 다음 데이터 비트에 닿지 못하게 함)의 보통 정수 곱으로
 * 만든다.
 */
final class Ghash {

    /** The high half of H (bytes 0 to 7, big endian). / H 의 상위 절반(0~7 바이트, 빅 엔디언). */
    private long h1;

    /** The low half of H (bytes 8 to 15). / H 의 하위 절반(8~15 바이트). */
    private long h0;

    /** The high half of the running value. / 누적 값의 상위 절반. */
    private long y1;

    /** The low half of the running value. / 누적 값의 하위 절반. */
    private long y0;

    /**
     * Creates the hash for a key.
     * <p>
     * 키에 대한 해시를 만든다.
     *
     * @param h the 16 byte hash key (the encryption of the zero block) / 16바이트 해시 키(0 블록을 암호화한 값)
     */
    Ghash(byte[] h) {
        h1 = Bytes.be64(h, 0);
        h0 = Bytes.be64(h, 8);
    }

    /**
     * The low 64 bits of the carry-less product of two 64 bit values. Each operand is split into four values that keep every fourth bit;
     * a product of two such values has carries only inside the three hole bits, so masking the result leaves the XOR sum.
     * <p>
     * 두 64비트 값의 캐리 없는 곱의 하위 64비트. 피연산자를 네 번째 비트마다만 남긴 값 넷으로 나눈다. 그런 값 둘의 곱은 캐리가 구멍 비트 셋 안에서만
     * 생기므로 결과를 마스크하면 XOR 합이 남는다.
     *
     * @param x first operand / 첫 피연산자
     * @param y second operand / 둘째 피연산자
     * @return the low half of the carry-less product / 캐리 없는 곱의 하위 절반
     */
    private static long bmul(long x, long y) {
        long x0 = x & 0x1111111111111111L;
        long x1 = x & 0x2222222222222222L;
        long x2 = x & 0x4444444444444444L;
        long x3 = x & 0x8888888888888888L;
        long y0 = y & 0x1111111111111111L;
        long y1 = y & 0x2222222222222222L;
        long y2 = y & 0x4444444444444444L;
        long y3 = y & 0x8888888888888888L;
        long z0 = (x0 * y0) ^ (x1 * y3) ^ (x2 * y2) ^ (x3 * y1);
        long z1 = (x0 * y1) ^ (x1 * y0) ^ (x2 * y3) ^ (x3 * y2);
        long z2 = (x0 * y2) ^ (x1 * y1) ^ (x2 * y0) ^ (x3 * y3);
        long z3 = (x0 * y3) ^ (x1 * y2) ^ (x2 * y1) ^ (x3 * y0);
        return (z0 & 0x1111111111111111L) | (z1 & 0x2222222222222222L) | (z2 & 0x4444444444444444L) | (z3 & 0x8888888888888888L);
    }

    /**
     * The high 64 bits of the carry-less product: the product of the bit reversed operands, reversed again and shifted.
     * <p>
     * 캐리 없는 곱의 상위 64비트: 비트를 뒤집은 피연산자의 곱을 다시 뒤집어 한 칸 옮긴 것.
     *
     * @param x first operand / 첫 피연산자
     * @param y second operand / 둘째 피연산자
     * @return the high half of the carry-less product / 캐리 없는 곱의 상위 절반
     */
    private static long bmulHigh(long x, long y) {
        return Long.reverse(bmul(Long.reverse(x), Long.reverse(y))) >>> 1;
    }

    /**
     * Multiplies the running value by H. GCM numbers the bits of a block from the most significant bit, so the integer product of the two
     * blocks is the bit reversed polynomial product; after a shift by one bit the upper 128 bits hold the terms below x^128 and the lower
     * 128 bits are folded in with x^128 = x^7 + x^2 + x + 1.
     * <p>
     * 누적 값에 H 를 곱한다. GCM 은 블록의 비트를 최상위 비트부터 세므로, 두 블록의 정수 곱은 다항식 곱을 비트 단위로 뒤집은 것이다. 한 비트 옮기면
     * 상위 128비트가 x^128 미만의 항이 되고, 하위 128비트는 x^128 = x^7 + x^2 + x + 1 로 접어 넣는다.
     */
    private void multiply() {
        // 카라추바: 64비트 캐리 없는 곱 세 번.
        long a = y0 ^ y1;
        long b = h0 ^ h1;
        long lo0 = bmul(y0, h0);
        long lo1 = bmulHigh(y0, h0);
        long hi0 = bmul(y1, h1);
        long hi1 = bmulHigh(y1, h1);
        long mid0 = bmul(a, b) ^ lo0 ^ hi0;
        long mid1 = bmulHigh(a, b) ^ lo1 ^ hi1;
        long w0 = lo0;
        long w1 = lo1 ^ mid0;
        long w2 = hi0 ^ mid1;
        long w3 = hi1;
        // 256비트 값을 왼쪽으로 한 비트 옮긴다.
        w3 = (w3 << 1) | (w2 >>> 63);
        w2 = (w2 << 1) | (w1 >>> 63);
        w1 = (w1 << 1) | (w0 >>> 63);
        w0 <<= 1;
        // 하위 두 워드를 차례로 접는다.
        w2 ^= w0 ^ (w0 >>> 1) ^ (w0 >>> 2) ^ (w0 >>> 7);
        w1 ^= (w0 << 63) ^ (w0 << 62) ^ (w0 << 57);
        w3 ^= w1 ^ (w1 >>> 1) ^ (w1 >>> 2) ^ (w1 >>> 7);
        w2 ^= (w1 << 63) ^ (w1 << 62) ^ (w1 << 57);
        y1 = w3;
        y0 = w2;
    }

    /**
     * Absorbs data; a last partial block is padded with zeros.
     * <p>
     * 데이터를 흡수한다. 마지막의 덜 찬 블록은 0 으로 채운다.
     *
     * @param data the data / 데이터
     * @param off  the start / 시작 위치
     * @param len  the length / 길이
     */
    void update(byte[] data, int off, int len) {
        int at = off;
        int end = off + len;
        // 온전한 블록을 차례로 흡수한다.
        while (end - at >= 16) {
            y1 ^= Bytes.be64(data, at);
            y0 ^= Bytes.be64(data, at + 8);
            multiply();
            at += 16;
        }
        if (at < end) {
            byte[] last = new byte[16];
            System.arraycopy(data, at, last, 0, end - at);
            y1 ^= Bytes.be64(last, 0);
            y0 ^= Bytes.be64(last, 8);
            multiply();
        }
    }

    /**
     * Absorbs the length block and returns the hash value. The object is reset for the next message with the same key.
     * <p>
     * 길이 블록을 흡수하고 해시 값을 반환한다. 객체는 같은 키의 다음 메시지를 위해 초기화된다.
     *
     * @param aadLen  the length of the associated data in bytes / 연관 데이터의 길이(바이트)
     * @param textLen the length of the ciphertext in bytes / 암호문의 길이(바이트)
     * @return the 16 byte hash / 16바이트 해시
     */
    byte[] finish(long aadLen, long textLen) {
        y1 ^= aadLen * 8;
        y0 ^= textLen * 8;
        multiply();
        byte[] out = new byte[16];
        Bytes.putBe64(y1, out, 0);
        Bytes.putBe64(y0, out, 8);
        y1 = 0;
        y0 = 0;
        return out;
    }

    /**
     * Wipes the key and the state.
     * <p>
     * 키와 상태를 지운다.
     */
    void destroy() {
        h1 = 0;
        h0 = 0;
        y1 = 0;
        y0 = 0;
    }
}
