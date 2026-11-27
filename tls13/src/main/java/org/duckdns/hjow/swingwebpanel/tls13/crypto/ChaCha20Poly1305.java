package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * The AEAD of RFC 8439: the ChaCha20 stream cipher with the Poly1305 authenticator, used by the CHACHA20_POLY1305 cipher suites of TLS.
 * The first key stream block (counter 0) gives the one-time Poly1305 key, the data is encrypted from counter 1. ChaCha20 uses only
 * additions, rotations and XOR, so nothing depends on secret data. An object is not thread safe. Example: see {@link Aead}.
 * <p>
 * RFC 8439 의 AEAD: ChaCha20 스트림 암호와 Poly1305 인증자. TLS 의 CHACHA20_POLY1305 암호 스위트가 쓴다. 첫 키 스트림 블록(카운터 0)이
 * 일회용 Poly1305 키가 되고 데이터는 카운터 1부터 암호화한다. ChaCha20 은 덧셈, 회전, XOR 만 쓰므로 비밀 데이터에 따라 달라지는 동작이 없다.
 * 객체는 스레드에 안전하지 않다. 사용 예는 {@link Aead} 참고.
 */
public final class ChaCha20Poly1305 implements Aead {

    /** The key as eight little endian words. / 리틀 엔디언 워드 여덟 개로 둔 키. */
    private final int[] key = new int[8];

    /** The working state of the block function. / 블록 함수의 작업 상태. */
    private final int[] x = new int[16];

    /** One key stream block. / 키 스트림 블록 하나. */
    private final byte[] stream = new byte[64];

    /**
     * Creates the AEAD for a key.
     * <p>
     * 키에 대한 AEAD 를 만든다.
     *
     * @param key the 32 byte key / 32바이트 키
     */
    public ChaCha20Poly1305(byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("ChaCha20 key length " + key.length);
        }
        // 키를 리틀 엔디언 워드로 읽는다.
        for (int i = 0; i < 8; i++) {
            this.key[i] = Bytes.le32(key, 4 * i);
        }
    }

    /**
     * The ChaCha20 block function (RFC 8439 section 2.3): writes the 64 byte key stream block of a counter value into {@link #stream}.
     * <p>
     * ChaCha20 블록 함수(RFC 8439 2.3절): 카운터 값의 64바이트 키 스트림 블록을 {@link #stream} 에 쓴다.
     *
     * @param nonce   the 12 byte nonce / 12바이트 nonce
     * @param counter the block counter / 블록 카운터
     */
    private void block(byte[] nonce, int counter) {
        int[] s = x;
        s[0] = 0x61707865;
        s[1] = 0x3320646e;
        s[2] = 0x79622d32;
        s[3] = 0x6b206574;
        System.arraycopy(key, 0, s, 4, 8);
        s[12] = counter;
        s[13] = Bytes.le32(nonce, 0);
        s[14] = Bytes.le32(nonce, 4);
        s[15] = Bytes.le32(nonce, 8);
        int x0 = s[0], x1 = s[1], x2 = s[2], x3 = s[3], x4 = s[4], x5 = s[5], x6 = s[6], x7 = s[7];
        int x8 = s[8], x9 = s[9], x10 = s[10], x11 = s[11], x12 = s[12], x13 = s[13], x14 = s[14], x15 = s[15];
        // 20 라운드: 열 라운드와 대각선 라운드를 열 번 되풀이한다.
        for (int i = 0; i < 10; i++) {
            x0 += x4; x12 = Integer.rotateLeft(x12 ^ x0, 16); x8 += x12; x4 = Integer.rotateLeft(x4 ^ x8, 12);
            x0 += x4; x12 = Integer.rotateLeft(x12 ^ x0, 8); x8 += x12; x4 = Integer.rotateLeft(x4 ^ x8, 7);
            x1 += x5; x13 = Integer.rotateLeft(x13 ^ x1, 16); x9 += x13; x5 = Integer.rotateLeft(x5 ^ x9, 12);
            x1 += x5; x13 = Integer.rotateLeft(x13 ^ x1, 8); x9 += x13; x5 = Integer.rotateLeft(x5 ^ x9, 7);
            x2 += x6; x14 = Integer.rotateLeft(x14 ^ x2, 16); x10 += x14; x6 = Integer.rotateLeft(x6 ^ x10, 12);
            x2 += x6; x14 = Integer.rotateLeft(x14 ^ x2, 8); x10 += x14; x6 = Integer.rotateLeft(x6 ^ x10, 7);
            x3 += x7; x15 = Integer.rotateLeft(x15 ^ x3, 16); x11 += x15; x7 = Integer.rotateLeft(x7 ^ x11, 12);
            x3 += x7; x15 = Integer.rotateLeft(x15 ^ x3, 8); x11 += x15; x7 = Integer.rotateLeft(x7 ^ x11, 7);
            x0 += x5; x15 = Integer.rotateLeft(x15 ^ x0, 16); x10 += x15; x5 = Integer.rotateLeft(x5 ^ x10, 12);
            x0 += x5; x15 = Integer.rotateLeft(x15 ^ x0, 8); x10 += x15; x5 = Integer.rotateLeft(x5 ^ x10, 7);
            x1 += x6; x12 = Integer.rotateLeft(x12 ^ x1, 16); x11 += x12; x6 = Integer.rotateLeft(x6 ^ x11, 12);
            x1 += x6; x12 = Integer.rotateLeft(x12 ^ x1, 8); x11 += x12; x6 = Integer.rotateLeft(x6 ^ x11, 7);
            x2 += x7; x13 = Integer.rotateLeft(x13 ^ x2, 16); x8 += x13; x7 = Integer.rotateLeft(x7 ^ x8, 12);
            x2 += x7; x13 = Integer.rotateLeft(x13 ^ x2, 8); x8 += x13; x7 = Integer.rotateLeft(x7 ^ x8, 7);
            x3 += x4; x14 = Integer.rotateLeft(x14 ^ x3, 16); x9 += x14; x4 = Integer.rotateLeft(x4 ^ x9, 12);
            x3 += x4; x14 = Integer.rotateLeft(x14 ^ x3, 8); x9 += x14; x4 = Integer.rotateLeft(x4 ^ x9, 7);
        }
        Bytes.putLe32(x0 + s[0], stream, 0);
        Bytes.putLe32(x1 + s[1], stream, 4);
        Bytes.putLe32(x2 + s[2], stream, 8);
        Bytes.putLe32(x3 + s[3], stream, 12);
        Bytes.putLe32(x4 + s[4], stream, 16);
        Bytes.putLe32(x5 + s[5], stream, 20);
        Bytes.putLe32(x6 + s[6], stream, 24);
        Bytes.putLe32(x7 + s[7], stream, 28);
        Bytes.putLe32(x8 + s[8], stream, 32);
        Bytes.putLe32(x9 + s[9], stream, 36);
        Bytes.putLe32(x10 + s[10], stream, 40);
        Bytes.putLe32(x11 + s[11], stream, 44);
        Bytes.putLe32(x12 + s[12], stream, 48);
        Bytes.putLe32(x13 + s[13], stream, 52);
        Bytes.putLe32(x14 + s[14], stream, 56);
        Bytes.putLe32(x15 + s[15], stream, 60);
    }

    /**
     * Returns one key stream block (for tests of the block function).
     * <p>
     * 키 스트림 블록 하나를 반환한다(블록 함수 시험용).
     *
     * @param nonce   the 12 byte nonce / 12바이트 nonce
     * @param counter the block counter / 블록 카운터
     * @return the 64 byte block / 64바이트 블록
     */
    byte[] keyStreamBlock(byte[] nonce, int counter) {
        block(nonce, counter);
        return stream.clone();
    }

    /**
     * XORs the key stream into data, starting at block counter 1.
     * <p>
     * 블록 카운터 1부터 키 스트림을 데이터에 XOR 한다.
     *
     * @param nonce  the nonce / nonce
     * @param in     the input / 입력
     * @param inOff  the start of the input / 입력의 시작 위치
     * @param len    the length / 길이
     * @param out    the output / 출력
     * @param outOff the start in the output / 출력의 시작 위치
     */
    private void crypt(byte[] nonce, byte[] in, int inOff, int len, byte[] out, int outOff) {
        int counter = 1;
        // 64바이트 블록마다 키 스트림을 만들어 더한다.
        for (int done = 0; done < len; done += 64) {
            block(nonce, counter++);
            int n = Math.min(64, len - done);
            // 키 스트림을 데이터에 더한다.
            for (int i = 0; i < n; i++) {
                out[outOff + done + i] = (byte) (in[inOff + done + i] ^ stream[i]);
            }
        }
    }

    /**
     * Computes the tag: Poly1305 with the one-time key over the padded associated data, the padded ciphertext and the two lengths.
     * <p>
     * 태그를 계산한다: 일회용 키의 Poly1305 를 0 으로 채운 연관 데이터, 0 으로 채운 암호문, 두 길이에 대해 구한다.
     *
     * @param nonce the nonce / nonce
     * @param aad   the associated data / 연관 데이터
     * @param text  the array holding the ciphertext / 암호문이 든 배열
     * @param off   the start of the ciphertext / 암호문의 시작 위치
     * @param len   the length of the ciphertext / 암호문의 길이
     * @return the 16 byte tag / 16바이트 태그
     */
    private byte[] tag(byte[] nonce, byte[] aad, byte[] text, int off, int len) {
        block(nonce, 0);
        Poly1305 mac = new Poly1305(stream, 0);
        mac.update(aad, 0, aad.length);
        mac.padToBlock();
        mac.update(text, off, len);
        mac.padToBlock();
        byte[] lengths = new byte[16];
        Bytes.putLe64(aad.length, lengths, 0);
        Bytes.putLe64(len, lengths, 8);
        mac.update(lengths, 0, 16);
        return mac.finish();
    }

    /**
     * Checks the nonce length.
     * <p>
     * nonce 길이를 확인한다.
     *
     * @param nonce the nonce / nonce
     */
    private static void checkNonce(byte[] nonce) {
        if (nonce.length != NONCE_LENGTH) {
            throw new IllegalArgumentException("nonce length " + nonce.length);
        }
    }

    /**
     * Encrypts with ChaCha20 and appends the Poly1305 tag.
     * <p>
     * ChaCha20 으로 암호화하고 Poly1305 태그를 붙인다.
     */
    public void seal(byte[] nonce, byte[] aad, byte[] in, int inOff, int len, byte[] out, int outOff) {
        checkNonce(nonce);
        crypt(nonce, in, inOff, len, out, outOff);
        byte[] t = tag(nonce, aad, out, outOff, len);
        System.arraycopy(t, 0, out, outOff + len, TAG_LENGTH);
    }

    /**
     * Verifies the tag first and decrypts only if it is correct.
     * <p>
     * 태그를 먼저 검증하고 맞을 때만 복호화한다.
     */
    public boolean open(byte[] nonce, byte[] aad, byte[] in, int inOff, int len, byte[] out, int outOff) {
        checkNonce(nonce);
        if (len < TAG_LENGTH) {
            return false;
        }
        int n = len - TAG_LENGTH;
        byte[] t = tag(nonce, aad, in, inOff, n);
        if (!Bytes.constantTimeEquals(t, 0, in, inOff + n, TAG_LENGTH)) {
            return false;
        }
        crypt(nonce, in, inOff, n, out, outOff);
        return true;
    }

    /**
     * Wipes the key and the buffers.
     * <p>
     * 키와 버퍼를 지운다.
     */
    public void destroy() {
        java.util.Arrays.fill(key, 0);
        java.util.Arrays.fill(x, 0);
        Bytes.wipe(stream);
    }
}
