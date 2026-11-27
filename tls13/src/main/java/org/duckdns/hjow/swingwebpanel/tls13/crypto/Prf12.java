package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * The pseudo random function of TLS 1.2 (RFC 5246 section 5): <code>PRF(secret, label, seed) = P_hash(secret, label + seed)</code> with the
 * hash of the cipher suite (SHA-256 or SHA-384). Example:
 * <code>byte[] master = Prf12.prf(HashAlg.SHA256, premaster, "master secret", seed, 48);</code>.
 * <p>
 * TLS 1.2 의 의사 난수 함수(RFC 5246 5절): 암호 스위트의 해시(SHA-256 또는 SHA-384)로 <code>PRF(secret, label, seed) = P_hash(secret, label + seed)</code>.
 * 사용 예: <code>byte[] master = Prf12.prf(HashAlg.SHA256, premaster, "master secret", seed, 48);</code>.
 */
public final class Prf12 {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Prf12() {
    }

    /**
     * Computes the PRF output.
     * <p>
     * PRF 출력을 계산한다.
     *
     * @param hash   the hash of the cipher suite / 암호 스위트의 해시
     * @param secret the secret / 비밀
     * @param label  the ASCII label / ASCII 레이블
     * @param seed   the seed / 시드
     * @param len    the output length / 출력 길이
     * @return the output / 출력
     */
    public static byte[] prf(HashAlg hash, byte[] secret, String label, byte[] seed, int len) {
        byte[] ls = new byte[label.length() + seed.length];
        // 레이블은 ASCII 이므로 문자 값을 그대로 쓴다.
        for (int i = 0; i < label.length(); i++) {
            ls[i] = (byte) label.charAt(i);
        }
        System.arraycopy(seed, 0, ls, label.length(), seed.length);
        byte[] out = new byte[len];
        byte[] a = ls;
        int at = 0;
        // A(i) = HMAC(secret, A(i-1)), 출력 조각 = HMAC(secret, A(i) | label | seed).
        while (at < len) {
            a = hash.hmac(secret, a);
            byte[] block = hash.hmac(secret, a, ls);
            int n = Math.min(block.length, len - at);
            System.arraycopy(block, 0, out, at, n);
            at += n;
            Bytes.wipe(block);
        }
        return out;
    }
}
