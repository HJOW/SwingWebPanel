package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;

/**
 * RSA signature verification (RFC 8017): RSASSA-PKCS1-v1_5 and RSASSA-PSS with SHA-256, SHA-384 or SHA-512. Only public values are
 * involved. The PKCS #1 v1.5 check builds the one valid encoding and compares all bytes, so loosely parsed forgeries with extra data are
 * rejected. Example:
 * <code>boolean ok = RsaVerify.pss(modulus, exponent, HashAlg.SHA256, message, signature, 32);</code>.
 * <p>
 * RSA 서명 검증(RFC 8017): SHA-256, SHA-384, SHA-512 의 RSASSA-PKCS1-v1_5 와 RSASSA-PSS. 공개 값만 다룬다. PKCS #1 v1.5 검사는 유효한 부호화
 * 하나를 만들어 모든 바이트를 비교하므로, 느슨한 해석을 노린 여분 데이터가 든 위조를 거부한다.
 */
public final class RsaVerify {

    /** The DER prefix of DigestInfo for SHA-256. / SHA-256 의 DigestInfo DER 접두사. */
    private static final byte[] PREFIX_SHA256 = { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05,
        0x00, 0x04, 0x20 };

    /** The DER prefix of DigestInfo for SHA-384. / SHA-384 의 DigestInfo DER 접두사. */
    private static final byte[] PREFIX_SHA384 = { 0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x05,
        0x00, 0x04, 0x30 };

    /** The DER prefix of DigestInfo for SHA-512. / SHA-512 의 DigestInfo DER 접두사. */
    private static final byte[] PREFIX_SHA512 = { 0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05,
        0x00, 0x04, 0x40 };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RsaVerify() {
    }

    /**
     * The RSA verification primitive: checks the signature length and range and returns <code>s^e mod n</code> as bytes of the modulus
     * length.
     * <p>
     * RSA 검증 기본 연산: 서명의 길이와 범위를 확인하고 <code>s^e mod n</code> 을 법의 길이만큼의 바이트로 반환한다.
     *
     * @param n         the modulus / 법
     * @param e         the public exponent / 공개 지수
     * @param signature the signature / 서명
     * @return the encoded message, or null if the key or the signature is unusable / 부호화된 메시지. 키나 서명을 쓸 수 없으면 null
     */
    private static byte[] recover(BigInteger n, BigInteger e, byte[] signature) {
        int k = (n.bitLength() + 7) / 8;
        if (n.signum() <= 0 || !n.testBit(0) || e.compareTo(BigInteger.valueOf(3)) < 0 || !e.testBit(0) || e.bitLength() > 64) {
            return null;
        }
        if (signature.length != k) {
            return null;
        }
        BigInteger s = new BigInteger(1, signature);
        if (s.compareTo(n) >= 0) {
            return null;
        }
        return EcCurve.toFixed(s.modPow(e, n), k);
    }

    /**
     * Verifies an RSASSA-PKCS1-v1_5 signature (RFC 8017 section 8.2.2).
     * <p>
     * RSASSA-PKCS1-v1_5 서명을 검증한다(RFC 8017 8.2.2절).
     *
     * @param n         the modulus / 법
     * @param e         the public exponent / 공개 지수
     * @param hash      SHA-256, SHA-384 or SHA-512 / SHA-256, SHA-384, SHA-512
     * @param message   the signed message / 서명한 메시지
     * @param signature the signature / 서명
     * @return true if the signature is valid / 서명이 유효하면 true
     */
    public static boolean pkcs1(BigInteger n, BigInteger e, HashAlg hash, byte[] message, byte[] signature) {
        byte[] em = recover(n, e, signature);
        if (em == null) {
            return false;
        }
        byte[] prefix = hash == HashAlg.SHA256 ? PREFIX_SHA256 : hash == HashAlg.SHA384 ? PREFIX_SHA384 : hash == HashAlg.SHA512 ? PREFIX_SHA512 : null;
        if (prefix == null) {
            return false;
        }
        byte[] digest = hash.digest(message);
        int tLen = prefix.length + digest.length;
        if (em.length < tLen + 11) {
            return false;
        }
        // 기대하는 부호화 00 01 FF..FF 00 DigestInfo 를 만들어 통째로 비교한다.
        byte[] expected = new byte[em.length];
        expected[1] = 1;
        java.util.Arrays.fill(expected, 2, em.length - tLen - 1, (byte) 0xff);
        System.arraycopy(prefix, 0, expected, em.length - tLen, prefix.length);
        System.arraycopy(digest, 0, expected, em.length - digest.length, digest.length);
        return Bytes.constantTimeEquals(em, expected);
    }

    /**
     * Verifies an RSASSA-PSS signature (RFC 8017 sections 8.1.2 and 9.1.2) with MGF1 over the same hash.
     * <p>
     * 같은 해시의 MGF1 을 쓰는 RSASSA-PSS 서명을 검증한다(RFC 8017 8.1.2절, 9.1.2절).
     *
     * @param n         the modulus / 법
     * @param e         the public exponent / 공개 지수
     * @param hash      the hash of the message and of MGF1 / 메시지와 MGF1 의 해시
     * @param message   the signed message / 서명한 메시지
     * @param signature the signature / 서명
     * @param saltLen   the salt length in bytes (the hash length in TLS) / salt 길이(바이트). TLS 에서는 해시 길이
     * @return true if the signature is valid / 서명이 유효하면 true
     */
    public static boolean pss(BigInteger n, BigInteger e, HashAlg hash, byte[] message, byte[] signature, int saltLen) {
        byte[] full = recover(n, e, signature);
        if (full == null || saltLen < 0) {
            return false;
        }
        int emBits = n.bitLength() - 1;
        int emLen = (emBits + 7) / 8;
        int hLen = hash.length();
        // 법의 비트 수가 8의 배수 + 1 이면 부호화된 메시지가 법보다 한 바이트 짧고, 맨 앞 바이트는 0 이어야 한다.
        if (emLen < full.length && full[0] != 0) {
            return false;
        }
        byte[] em = Bytes.copy(full, full.length - emLen, emLen);
        if (emLen < hLen + saltLen + 2 || em[emLen - 1] != (byte) 0xbc) {
            return false;
        }
        int dbLen = emLen - hLen - 1;
        int topBits = 8 * emLen - emBits;
        if ((em[0] & 0xff) >>> (8 - topBits) != 0) {
            return false;
        }
        byte[] h = Bytes.copy(em, dbLen, hLen);
        byte[] db = new byte[dbLen];
        int at = 0;
        // MGF1: Hash(H | counter) 를 이어 붙여 마스크를 만들고 DB 를 푼다.
        for (int counter = 0; at < dbLen; counter++) {
            byte[] c = new byte[4];
            Bytes.putBe32(counter, c, 0);
            byte[] block = hash.digest(h, c);
            // 마스크 블록을 가린 DB 에 더한다.
            for (int i = 0; i < block.length && at < dbLen; i++, at++) {
                db[at] = (byte) (em[at] ^ block[i]);
            }
        }
        db[0] &= (byte) (0xff >>> topBits);
        int zeros = dbLen - saltLen - 1;
        int bad = db[zeros] ^ 1;
        // 패딩은 0 들과 그 뒤의 01 이어야 한다.
        for (int i = 0; i < zeros; i++) {
            bad |= db[i];
        }
        if (bad != 0) {
            return false;
        }
        byte[] salt = Bytes.copy(db, dbLen - saltLen, saltLen);
        byte[] expected = hash.digest(new byte[8], hash.digest(message), salt);
        return Bytes.constantTimeEquals(expected, h);
    }
}
