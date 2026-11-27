package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A hash function of the TLS module (SHA-256, SHA-384 or SHA-512) with HMAC on top of it (RFC 2104). The digests come from the
 * <code>MessageDigest</code> of the JRE; HMAC is computed here so that an empty key works and no <code>javax.crypto</code> class is
 * needed. Example: <code>byte[] mac = HashAlg.SHA256.hmac(key, data);</code>.
 * <p>
 * TLS 모듈의 해시 함수(SHA-256, SHA-384, SHA-512)와 그 위의 HMAC(RFC 2104). 다이제스트는 JRE 의 <code>MessageDigest</code> 를 쓰고, HMAC 은
 * 빈 키도 쓸 수 있고 <code>javax.crypto</code> 클래스가 필요 없도록 여기서 계산한다. 사용 예: <code>byte[] mac = HashAlg.SHA256.hmac(key, data);</code>.
 */
public final class HashAlg {

    /** SHA-256 (32 byte output). / SHA-256(출력 32바이트). */
    public static final HashAlg SHA256 = new HashAlg("SHA-256", 32, 64);

    /** SHA-384 (48 byte output). / SHA-384(출력 48바이트). */
    public static final HashAlg SHA384 = new HashAlg("SHA-384", 48, 128);

    /** SHA-512 (64 byte output). / SHA-512(출력 64바이트). */
    public static final HashAlg SHA512 = new HashAlg("SHA-512", 64, 128);

    /** The JCA name of the digest. / 다이제스트의 JCA 이름. */
    private final String name;

    /** The output length in bytes. / 출력 길이(바이트). */
    private final int length;

    /** The block length in bytes (for HMAC). / 블록 길이(바이트, HMAC 용). */
    private final int block;

    /**
     * Creates a hash description.
     * <p>
     * 해시 설명을 만든다.
     *
     * @param name   JCA name / JCA 이름
     * @param length output length / 출력 길이
     * @param block  block length / 블록 길이
     */
    private HashAlg(String name, int length, int block) {
        this.name = name;
        this.length = length;
        this.block = block;
    }

    /**
     * Returns the JCA name, for example <code>SHA-256</code>.
     * <p>
     * JCA 이름을 반환한다. 예: <code>SHA-256</code>.
     *
     * @return the name / 이름
     */
    public String name() {
        return name;
    }

    /**
     * Returns the output length in bytes.
     * <p>
     * 출력 길이를 바이트로 반환한다.
     *
     * @return the length / 길이
     */
    public int length() {
        return length;
    }

    /**
     * Creates a new digest object of the JRE.
     * <p>
     * JRE 의 새 다이제스트 객체를 만든다.
     *
     * @return the digest / 다이제스트
     */
    public MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("the JRE has no " + name);
        }
    }

    /**
     * Hashes the concatenation of the parts.
     * <p>
     * 조각들을 이어 붙인 것의 해시를 구한다.
     *
     * @param parts the data / 데이터
     * @return the digest / 다이제스트
     */
    public byte[] digest(byte[]... parts) {
        MessageDigest md = newDigest();
        // 조각을 차례로 넣는다.
        for (int i = 0; i < parts.length; i++) {
            md.update(parts[i]);
        }
        return md.digest();
    }

    /**
     * Computes HMAC over the concatenation of the parts.
     * <p>
     * 조각들을 이어 붙인 것의 HMAC 을 계산한다.
     *
     * @param key   the key (may be empty) / 키(비어 있어도 된다)
     * @param parts the data / 데이터
     * @return the MAC / MAC
     */
    public byte[] hmac(byte[] key, byte[]... parts) {
        MessageDigest md = newDigest();
        byte[] k = key.length > block ? md.digest(key) : key;
        byte[] pad = new byte[block];
        // 안쪽 패딩: 키 XOR 0x36.
        for (int i = 0; i < block; i++) {
            pad[i] = (byte) ((i < k.length ? k[i] : 0) ^ 0x36);
        }
        md.update(pad);
        // 조각을 차례로 넣는다.
        for (int i = 0; i < parts.length; i++) {
            md.update(parts[i]);
        }
        byte[] inner = md.digest();
        // 바깥 패딩: 키 XOR 0x5c (0x36 ^ 0x6a = 0x5c).
        for (int i = 0; i < block; i++) {
            pad[i] ^= 0x6a;
        }
        md.update(pad);
        md.update(inner);
        Bytes.wipe(pad);
        return md.digest();
    }
}
