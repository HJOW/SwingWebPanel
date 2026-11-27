package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * AES in Galois/Counter Mode (NIST SP 800-38D) with a 96 bit nonce and a 128 bit tag, the AEAD of the AES cipher suites of TLS.
 * An object is not thread safe; the record layer uses one object per direction. Example: see {@link Aead}.
 * <p>
 * 96비트 nonce 와 128비트 태그의 AES 갈루아/카운터 모드(NIST SP 800-38D). TLS 의 AES 암호 스위트가 쓰는 AEAD 이다. 객체는 스레드에 안전하지 않으며
 * 레코드 계층은 방향마다 객체 하나를 쓴다. 사용 예는 {@link Aead} 참고.
 */
public final class AesGcm implements Aead {

    /** The block cipher. / 블록 암호. */
    private final Aes aes;

    /** The universal hash with the key H. / 키 H 의 범용 해시. */
    private final Ghash ghash;

    /** Four counter blocks. / 카운터 블록 네 개. */
    private final byte[] counters = new byte[64];

    /** The key stream of four blocks. / 네 블록의 키 스트림. */
    private final byte[] stream = new byte[64];

    /**
     * Creates the AEAD for a key.
     * <p>
     * 키에 대한 AEAD 를 만든다.
     *
     * @param key the AES key of 16 or 32 bytes / 16 또는 32바이트 AES 키
     */
    public AesGcm(byte[] key) {
        if (key.length != 16 && key.length != 32) {
            throw new IllegalArgumentException("AES-GCM key length " + key.length);
        }
        aes = new Aes(key);
        byte[] h = new byte[16];
        aes.encryptBlocks(h, 0, h, 0, 1);
        ghash = new Ghash(h);
        Bytes.wipe(h);
    }

    /**
     * XORs the counter mode key stream into data, starting with the given counter value.
     * <p>
     * 주어진 카운터 값부터 카운터 모드 키 스트림을 데이터에 XOR 한다.
     *
     * @param nonce   the nonce / nonce
     * @param counter the first counter value (2 for the first data block) / 첫 카운터 값(첫 데이터 블록은 2)
     * @param in      the input / 입력
     * @param inOff   the start of the input / 입력의 시작 위치
     * @param len     the length / 길이
     * @param out     the output / 출력
     * @param outOff  the start in the output / 출력의 시작 위치
     */
    private void ctr(byte[] nonce, int counter, byte[] in, int inOff, int len, byte[] out, int outOff) {
        int done = 0;
        int ctr = counter;
        // 네 블록(64바이트)씩 키 스트림을 만들어 XOR 한다.
        while (done < len) {
            int n = Math.min(64, len - done);
            int blocks = (n + 15) / 16;
            // 필요한 카운터 블록을 채운다.
            for (int b = 0; b < blocks; b++) {
                System.arraycopy(nonce, 0, counters, 16 * b, 12);
                Bytes.putBe32(ctr++, counters, 16 * b + 12);
            }
            aes.encryptBlocks(counters, 0, stream, 0, blocks);
            // 키 스트림을 데이터에 더한다.
            for (int i = 0; i < n; i++) {
                out[outOff + done + i] = (byte) (in[inOff + done + i] ^ stream[i]);
            }
            done += n;
        }
    }

    /**
     * Computes the tag over the associated data and the ciphertext.
     * <p>
     * 연관 데이터와 암호문에 대한 태그를 계산한다.
     *
     * @param nonce the nonce / nonce
     * @param aad   the associated data / 연관 데이터
     * @param text  the array holding the ciphertext / 암호문이 든 배열
     * @param off   the start of the ciphertext / 암호문의 시작 위치
     * @param len   the length of the ciphertext / 암호문의 길이
     * @return the 16 byte tag / 16바이트 태그
     */
    private byte[] tag(byte[] nonce, byte[] aad, byte[] text, int off, int len) {
        ghash.update(aad, 0, aad.length);
        ghash.update(text, off, len);
        byte[] s = ghash.finish(aad.length, len);
        // 첫 카운터 블록(카운터 1)의 암호화 결과로 해시를 가린다.
        System.arraycopy(nonce, 0, counters, 0, 12);
        Bytes.putBe32(1, counters, 12);
        aes.encryptBlocks(counters, 0, stream, 0, 1);
        // 해시와 키 스트림을 더한다.
        for (int i = 0; i < 16; i++) {
            s[i] ^= stream[i];
        }
        return s;
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
     * Encrypts with counter mode and appends the tag.
     * <p>
     * 카운터 모드로 암호화하고 태그를 붙인다.
     */
    public void seal(byte[] nonce, byte[] aad, byte[] in, int inOff, int len, byte[] out, int outOff) {
        checkNonce(nonce);
        ctr(nonce, 2, in, inOff, len, out, outOff);
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
        ctr(nonce, 2, in, inOff, n, out, outOff);
        return true;
    }

    /**
     * Wipes the round keys, the hash key and the buffers.
     * <p>
     * 라운드 키, 해시 키, 버퍼를 지운다.
     */
    public void destroy() {
        aes.destroy();
        ghash.destroy();
        Bytes.wipe(counters);
        Bytes.wipe(stream);
    }
}
