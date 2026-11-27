package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * HKDF (RFC 5869) and the TLS 1.3 label functions built on it (RFC 8446 section 7.1): <code>HKDF-Expand-Label</code> and
 * <code>Derive-Secret</code>. Example: <code>byte[] key = Hkdf.expandLabel(HashAlg.SHA256, secret, "key", new byte[0], 16);</code>.
 * <p>
 * HKDF(RFC 5869)와 그 위의 TLS 1.3 레이블 함수(RFC 8446 7.1절): <code>HKDF-Expand-Label</code>, <code>Derive-Secret</code>.
 * 사용 예: <code>byte[] key = Hkdf.expandLabel(HashAlg.SHA256, secret, "key", new byte[0], 16);</code>.
 */
public final class Hkdf {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Hkdf() {
    }

    /**
     * HKDF-Extract: <code>PRK = HMAC(salt, IKM)</code>. A null or empty salt means a string of zero bytes of the hash length.
     * <p>
     * HKDF-Extract: <code>PRK = HMAC(salt, IKM)</code>. salt 가 null 이거나 비어 있으면 해시 길이만큼의 0 바이트를 뜻한다.
     *
     * @param hash the hash / 해시
     * @param salt the salt or null / salt 또는 null
     * @param ikm  the input keying material / 입력 키 자료
     * @return the pseudo random key / 의사 난수 키
     */
    public static byte[] extract(HashAlg hash, byte[] salt, byte[] ikm) {
        byte[] s = salt == null || salt.length == 0 ? new byte[hash.length()] : salt;
        return hash.hmac(s, ikm);
    }

    /**
     * HKDF-Expand: output keying material of the requested length.
     * <p>
     * HKDF-Expand: 요청한 길이의 출력 키 자료.
     *
     * @param hash the hash / 해시
     * @param prk  the pseudo random key / 의사 난수 키
     * @param info the context / 문맥 정보
     * @param len  the output length, at most 255 times the hash length / 출력 길이. 해시 길이의 255배 이하
     * @return the output / 출력
     */
    public static byte[] expand(HashAlg hash, byte[] prk, byte[] info, int len) {
        if (len < 0 || len > 255 * hash.length()) {
            throw new IllegalArgumentException("HKDF output length " + len);
        }
        byte[] out = new byte[len];
        byte[] t = new byte[0];
        int at = 0;
        // T(i) = HMAC(PRK, T(i-1) | info | i) 를 필요한 만큼 이어 붙인다.
        for (int i = 1; at < len; i++) {
            t = hash.hmac(prk, t, info, new byte[] { (byte) i });
            int n = Math.min(t.length, len - at);
            System.arraycopy(t, 0, out, at, n);
            at += n;
        }
        Bytes.wipe(t);
        return out;
    }

    /**
     * HKDF-Expand-Label of TLS 1.3: expands with the structure <code>HkdfLabel</code> (length, <code>"tls13 " + label</code>, context).
     * <p>
     * TLS 1.3 의 HKDF-Expand-Label: <code>HkdfLabel</code> 구조(길이, <code>"tls13 " + label</code>, context)로 확장한다.
     *
     * @param hash    the hash / 해시
     * @param secret  the secret / 비밀
     * @param label   the label without the prefix (ASCII) / 접두사를 뺀 레이블(ASCII)
     * @param context the context (may be empty) / 문맥(비어 있어도 된다)
     * @param len     the output length / 출력 길이
     * @return the output / 출력
     */
    public static byte[] expandLabel(HashAlg hash, byte[] secret, String label, byte[] context, int len) {
        String full = "tls13 " + label;
        if (full.length() > 255 || context.length > 255) {
            throw new IllegalArgumentException("label or context too long");
        }
        byte[] info = new byte[2 + 1 + full.length() + 1 + context.length];
        info[0] = (byte) (len >>> 8);
        info[1] = (byte) len;
        info[2] = (byte) full.length();
        // 레이블은 ASCII 이므로 문자 값을 그대로 쓴다.
        for (int i = 0; i < full.length(); i++) {
            info[3 + i] = (byte) full.charAt(i);
        }
        info[3 + full.length()] = (byte) context.length;
        System.arraycopy(context, 0, info, 4 + full.length(), context.length);
        return expand(hash, secret, info, len);
    }

    /**
     * Derive-Secret of TLS 1.3: <code>HKDF-Expand-Label(secret, label, transcriptHash, Hash.length)</code>.
     * <p>
     * TLS 1.3 의 Derive-Secret: <code>HKDF-Expand-Label(secret, label, transcriptHash, Hash.length)</code>.
     *
     * @param hash           the hash / 해시
     * @param secret         the secret / 비밀
     * @param label          the label / 레이블
     * @param transcriptHash the hash of the handshake messages so far / 지금까지의 핸드셰이크 메시지 해시
     * @return the derived secret / 유도한 비밀
     */
    public static byte[] deriveSecret(HashAlg hash, byte[] secret, String label, byte[] transcriptHash) {
        return expandLabel(hash, secret, label, transcriptHash, hash.length());
    }
}
