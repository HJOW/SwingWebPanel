package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * The Poly1305 one-time authenticator (RFC 8439 section 2.5). The 130 bit accumulator and the clamped key part r are kept as five
 * 26 bit limbs in <code>long</code> values, so all products fit without overflow; no branch depends on the key or the data. A key
 * must be used for one message only. Example:
 * <pre>
 * Poly1305 mac = new Poly1305(oneTimeKey, 0);
 * mac.update(data, 0, data.length);
 * byte[] tag = mac.finish();
 * </pre>
 * <p>
 * Poly1305 일회용 인증자(RFC 8439 2.5절). 130비트 누산기와 클램프한 키 부분 r 을 <code>long</code> 값 안의 26비트 림 다섯 개로 두어 모든 곱이
 * 넘침 없이 들어간다. 키나 데이터에 따른 분기가 없다. 키 하나는 메시지 하나에만 써야 한다.
 */
final class Poly1305 {

    /** The mask of a 26 bit limb. / 26비트 림의 마스크. */
    private static final long MASK = 0x3ffffffL;

    /** The limbs of r. / r 의 림. */
    private long r0, r1, r2, r3, r4;

    /** The limbs of r multiplied by five (for the reduction modulo 2^130 - 5). / r 의 림에 5 를 곱한 값(2^130 - 5 로 줄일 때 쓴다). */
    private long s1, s2, s3, s4;

    /** The limbs of the accumulator. / 누산기의 림. */
    private long h0, h1, h2, h3, h4;

    /** The four 32 bit words of the key part s. / 키 부분 s 의 32비트 워드 넷. */
    private long pad0, pad1, pad2, pad3;

    /** A partial block waiting for more data. / 데이터를 더 기다리는 덜 찬 블록. */
    private final byte[] buffer = new byte[16];

    /** The number of bytes in the buffer. / 버퍼에 든 바이트 수. */
    private int buffered;

    /**
     * Creates the authenticator from a 32 byte one-time key (r followed by s).
     * <p>
     * 32바이트 일회용 키(r 과 그 뒤의 s)로 인증자를 만든다.
     *
     * @param key the array holding the key / 키가 든 배열
     * @param off the start of the key / 키의 시작 위치
     */
    Poly1305(byte[] key, int off) {
        // r 을 클램프하면서 26비트 림으로 나눈다.
        r0 = Bytes.le32(key, off) & 0x3ffffffL;
        r1 = (Bytes.le32(key, off + 3) >>> 2) & 0x3ffff03L;
        r2 = (Bytes.le32(key, off + 6) >>> 4) & 0x3ffc0ffL;
        r3 = (Bytes.le32(key, off + 9) >>> 6) & 0x3f03fffL;
        r4 = (Bytes.le32(key, off + 12) >>> 8) & 0x00fffffL;
        s1 = r1 * 5;
        s2 = r2 * 5;
        s3 = r3 * 5;
        s4 = r4 * 5;
        pad0 = Bytes.le32(key, off + 16) & 0xffffffffL;
        pad1 = Bytes.le32(key, off + 20) & 0xffffffffL;
        pad2 = Bytes.le32(key, off + 24) & 0xffffffffL;
        pad3 = Bytes.le32(key, off + 28) & 0xffffffffL;
    }

    /**
     * Adds one 16 byte block to the accumulator and multiplies by r.
     * <p>
     * 16바이트 블록 하나를 누산기에 더하고 r 을 곱한다.
     *
     * @param m     the array holding the block / 블록이 든 배열
     * @param off   the start of the block / 블록의 시작 위치
     * @param hibit 2^24 for a full block (the bit 2^128), 0 for the padded last block / 온전한 블록이면 2^24(2^128 비트), 채운 마지막 블록이면 0
     */
    private void block(byte[] m, int off, long hibit) {
        h0 += Bytes.le32(m, off) & 0x3ffffffL;
        h1 += (Bytes.le32(m, off + 3) >>> 2) & 0x3ffffffL;
        h2 += (Bytes.le32(m, off + 6) >>> 4) & 0x3ffffffL;
        h3 += (Bytes.le32(m, off + 9) >>> 6) & 0x3ffffffL;
        h4 += ((Bytes.le32(m, off + 12) >>> 8) & 0xffffffL) | hibit;
        long d0 = h0 * r0 + h1 * s4 + h2 * s3 + h3 * s2 + h4 * s1;
        long d1 = h0 * r1 + h1 * r0 + h2 * s4 + h3 * s3 + h4 * s2;
        long d2 = h0 * r2 + h1 * r1 + h2 * r0 + h3 * s4 + h4 * s3;
        long d3 = h0 * r3 + h1 * r2 + h2 * r1 + h3 * r0 + h4 * s4;
        long d4 = h0 * r4 + h1 * r3 + h2 * r2 + h3 * r1 + h4 * r0;
        long c = d0 >>> 26;
        h0 = d0 & MASK;
        d1 += c;
        c = d1 >>> 26;
        h1 = d1 & MASK;
        d2 += c;
        c = d2 >>> 26;
        h2 = d2 & MASK;
        d3 += c;
        c = d3 >>> 26;
        h3 = d3 & MASK;
        d4 += c;
        c = d4 >>> 26;
        h4 = d4 & MASK;
        h0 += c * 5;
        c = h0 >>> 26;
        h0 &= MASK;
        h1 += c;
    }

    /**
     * Absorbs data.
     * <p>
     * 데이터를 흡수한다.
     *
     * @param data the data / 데이터
     * @param off  the start / 시작 위치
     * @param len  the length / 길이
     */
    void update(byte[] data, int off, int len) {
        int at = off;
        int left = len;
        if (buffered > 0) {
            int n = Math.min(16 - buffered, left);
            System.arraycopy(data, at, buffer, buffered, n);
            buffered += n;
            at += n;
            left -= n;
            if (buffered < 16) {
                return;
            }
            block(buffer, 0, 1L << 24);
            buffered = 0;
        }
        // 온전한 블록을 차례로 처리한다.
        while (left >= 16) {
            block(data, at, 1L << 24);
            at += 16;
            left -= 16;
        }
        System.arraycopy(data, at, buffer, 0, left);
        buffered = left;
    }

    /**
     * Pads the absorbed data with zeros to a multiple of 16 bytes (the AEAD construction of RFC 8439 section 2.8).
     * <p>
     * 흡수한 데이터를 16바이트의 배수가 되도록 0 으로 채운다(RFC 8439 2.8절의 AEAD 구성).
     */
    void padToBlock() {
        if (buffered > 0) {
            java.util.Arrays.fill(buffer, buffered, 16, (byte) 0);
            block(buffer, 0, 1L << 24);
            buffered = 0;
        }
    }

    /**
     * Finishes the message and returns the 16 byte tag; the key material is wiped.
     * <p>
     * 메시지를 끝내고 16바이트 태그를 반환한다. 키 자료는 지운다.
     *
     * @return the tag / 태그
     */
    byte[] finish() {
        if (buffered > 0) {
            // 마지막 덜 찬 블록에는 1 바이트를 붙이고 0 으로 채운다.
            buffer[buffered] = 1;
            java.util.Arrays.fill(buffer, buffered + 1, 16, (byte) 0);
            block(buffer, 0, 0);
        }
        // 올림을 끝까지 전파한다.
        long c = h1 >>> 26;
        h1 &= MASK;
        h2 += c;
        c = h2 >>> 26;
        h2 &= MASK;
        h3 += c;
        c = h3 >>> 26;
        h3 &= MASK;
        h4 += c;
        c = h4 >>> 26;
        h4 &= MASK;
        h0 += c * 5;
        c = h0 >>> 26;
        h0 &= MASK;
        h1 += c;
        // g = h + 5 - 2^130 을 구해 음수가 아니면(h >= p) g 를 고른다. 분기 없이 마스크로 고른다.
        long g0 = h0 + 5;
        c = g0 >>> 26;
        g0 &= MASK;
        long g1 = h1 + c;
        c = g1 >>> 26;
        g1 &= MASK;
        long g2 = h2 + c;
        c = g2 >>> 26;
        g2 &= MASK;
        long g3 = h3 + c;
        c = g3 >>> 26;
        g3 &= MASK;
        long g4 = h4 + c - (1L << 26);
        long takeG = (g4 >>> 63) - 1;
        long keepH = ~takeG;
        h0 = (h0 & keepH) | (g0 & takeG);
        h1 = (h1 & keepH) | (g1 & takeG);
        h2 = (h2 & keepH) | (g2 & takeG);
        h3 = (h3 & keepH) | (g3 & takeG);
        h4 = (h4 & keepH) | (g4 & takeG);
        // 2^128 을 법으로 32비트 워드 넷으로 모으고 s 를 더한다.
        long w0 = (h0 | (h1 << 26)) & 0xffffffffL;
        long w1 = ((h1 >>> 6) | (h2 << 20)) & 0xffffffffL;
        long w2 = ((h2 >>> 12) | (h3 << 14)) & 0xffffffffL;
        long w3 = ((h3 >>> 18) | (h4 << 8)) & 0xffffffffL;
        long f = w0 + pad0;
        w0 = f & 0xffffffffL;
        f = w1 + pad1 + (f >>> 32);
        w1 = f & 0xffffffffL;
        f = w2 + pad2 + (f >>> 32);
        w2 = f & 0xffffffffL;
        f = w3 + pad3 + (f >>> 32);
        w3 = f & 0xffffffffL;
        byte[] tag = new byte[16];
        Bytes.putLe32((int) w0, tag, 0);
        Bytes.putLe32((int) w1, tag, 4);
        Bytes.putLe32((int) w2, tag, 8);
        Bytes.putLe32((int) w3, tag, 12);
        r0 = r1 = r2 = r3 = r4 = 0;
        s1 = s2 = s3 = s4 = 0;
        h0 = h1 = h2 = h3 = h4 = 0;
        pad0 = pad1 = pad2 = pad3 = 0;
        Bytes.wipe(buffer);
        buffered = 0;
        return tag;
    }
}
