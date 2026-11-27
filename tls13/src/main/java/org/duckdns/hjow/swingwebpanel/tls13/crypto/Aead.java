package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * Authenticated encryption with associated data, as the TLS record layer uses it: a 12 byte nonce, a 16 byte tag appended to the
 * ciphertext. Implementations: {@link AesGcm} and {@link ChaCha20Poly1305}. Example:
 * <pre>
 * Aead aead = new AesGcm(key);
 * aead.seal(nonce, aad, plain, 0, plain.length, out, 0);            // writes plain.length + 16 bytes
 * boolean ok = aead.open(nonce, aad, out, 0, out.length, back, 0);  // false: forged, nothing usable was written
 * </pre>
 * <p>
 * TLS 레코드 계층이 쓰는 연관 데이터 인증 암호화: 12바이트 nonce, 암호문 뒤에 붙는 16바이트 태그. 구현은 {@link AesGcm} 과
 * {@link ChaCha20Poly1305} 이다.
 */
public interface Aead {

    /** The length of the authentication tag in bytes. / 인증 태그의 길이(바이트). */
    int TAG_LENGTH = 16;

    /** The length of the nonce in bytes. / nonce 의 길이(바이트). */
    int NONCE_LENGTH = 12;

    /**
     * Encrypts and authenticates. Writes <code>len + 16</code> bytes: the ciphertext followed by the tag. Input and output may be the
     * same array at the same position.
     * <p>
     * 암호화하고 인증한다. <code>len + 16</code> 바이트(암호문과 그 뒤의 태그)를 쓴다. 입력과 출력이 같은 배열의 같은 위치여도 된다.
     *
     * @param nonce  the 12 byte nonce, never reused with the same key / 12바이트 nonce. 같은 키로 다시 쓰면 안 된다
     * @param aad    the associated data (authenticated, not encrypted) / 연관 데이터(인증만 하고 암호화하지 않는다)
     * @param in     the plaintext / 평문
     * @param inOff  the start of the plaintext / 평문의 시작 위치
     * @param len    the length of the plaintext / 평문의 길이
     * @param out    the output / 출력
     * @param outOff the start in the output / 출력의 시작 위치
     */
    void seal(byte[] nonce, byte[] aad, byte[] in, int inOff, int len, byte[] out, int outOff);

    /**
     * Verifies the tag and then decrypts. The tag is checked before any plaintext is produced; on failure nothing is written.
     * Input and output may be the same array at the same position.
     * <p>
     * 태그를 검증한 뒤 복호화한다. 평문을 내기 전에 태그를 먼저 확인하며, 실패하면 아무것도 쓰지 않는다. 입력과 출력이 같은 배열의 같은 위치여도 된다.
     *
     * @param nonce  the 12 byte nonce / 12바이트 nonce
     * @param aad    the associated data / 연관 데이터
     * @param in     the ciphertext followed by the tag / 암호문과 그 뒤의 태그
     * @param inOff  the start of the ciphertext / 암호문의 시작 위치
     * @param len    the length of ciphertext and tag together, at least 16 / 암호문과 태그를 합한 길이(16 이상)
     * @param out    receives <code>len - 16</code> bytes of plaintext / 평문 <code>len - 16</code> 바이트를 받는다
     * @param outOff the start in the output / 출력의 시작 위치
     * @return true if the tag was correct / 태그가 맞으면 true
     */
    boolean open(byte[] nonce, byte[] aad, byte[] in, int inOff, int len, byte[] out, int outOff);

    /**
     * Wipes the key material; the object cannot be used afterwards.
     * <p>
     * 키 자료를 지운다. 그 뒤에는 객체를 쓸 수 없다.
     */
    void destroy();
}
