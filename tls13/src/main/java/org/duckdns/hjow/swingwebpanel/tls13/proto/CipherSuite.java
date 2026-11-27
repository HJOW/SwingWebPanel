package org.duckdns.hjow.swingwebpanel.tls13.proto;

import org.duckdns.hjow.swingwebpanel.tls13.crypto.Aead;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.AesGcm;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.ChaCha20Poly1305;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;

/**
 * The cipher suites of this module: the three AEAD suites of TLS 1.3 (RFC 8446 appendix B.4) and six ECDHE suites with AEAD ciphers of
 * TLS 1.2 (RFC 5289, RFC 7905). Suites with CBC, RC4, 3DES, static RSA or DHE key exchange are deliberately absent (RFC 9325).
 * Example: <code>CipherSuite s = CipherSuite.byId(0x1301); Aead aead = s.newAead(key);</code>.
 * <p>
 * 이 모듈의 암호 스위트: TLS 1.3 의 AEAD 스위트 셋(RFC 8446 부록 B.4)과 TLS 1.2 의 AEAD 암호를 쓰는 ECDHE 스위트 여섯(RFC 5289, RFC 7905).
 * CBC, RC4, 3DES, 정적 RSA, DHE 키 교환 스위트는 일부러 넣지 않았다(RFC 9325).
 */
public final class CipherSuite {

    /** Authentication of TLS 1.2 suites: the server signs with an RSA key. / TLS 1.2 스위트의 인증: 서버가 RSA 키로 서명한다. */
    public static final int AUTH_RSA = 1;

    /** Authentication of TLS 1.2 suites: the server signs with an EC key. / TLS 1.2 스위트의 인증: 서버가 EC 키로 서명한다. */
    public static final int AUTH_ECDSA = 2;

    /** All suites in the order of preference of the ClientHello. / ClientHello 의 선호 순서대로의 모든 스위트. */
    private static final CipherSuite[] ALL = {
        new CipherSuite(0x1301, "TLS_AES_128_GCM_SHA256", true, HashAlg.SHA256, 16, false, 0),
        new CipherSuite(0x1302, "TLS_AES_256_GCM_SHA384", true, HashAlg.SHA384, 32, false, 0),
        new CipherSuite(0x1303, "TLS_CHACHA20_POLY1305_SHA256", true, HashAlg.SHA256, 32, true, 0),
        new CipherSuite(0xc02b, "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", false, HashAlg.SHA256, 16, false, AUTH_ECDSA),
        new CipherSuite(0xc02f, "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", false, HashAlg.SHA256, 16, false, AUTH_RSA),
        new CipherSuite(0xc02c, "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", false, HashAlg.SHA384, 32, false, AUTH_ECDSA),
        new CipherSuite(0xc030, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", false, HashAlg.SHA384, 32, false, AUTH_RSA),
        new CipherSuite(0xcca9, "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", false, HashAlg.SHA256, 32, true, AUTH_ECDSA),
        new CipherSuite(0xcca8, "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", false, HashAlg.SHA256, 32, true, AUTH_RSA) };

    /** The code point. / 코드 값. */
    private final int id;

    /** The standard name. / 표준 이름. */
    private final String name;

    /** True for a TLS 1.3 suite. / TLS 1.3 스위트이면 true. */
    private final boolean tls13;

    /** The hash of the key schedule or of the PRF. / 키 스케줄 또는 PRF 의 해시. */
    private final HashAlg hash;

    /** The key length of the AEAD in bytes. / AEAD 의 키 길이(바이트). */
    private final int keyLength;

    /** True for ChaCha20-Poly1305, false for AES-GCM. / ChaCha20-Poly1305 이면 true, AES-GCM 이면 false. */
    private final boolean chacha;

    /** The authentication of a TLS 1.2 suite, 0 for TLS 1.3. / TLS 1.2 스위트의 인증. TLS 1.3 이면 0. */
    private final int auth;

    /**
     * Creates a suite.
     * <p>
     * 스위트를 만든다.
     *
     * @param id        code point / 코드 값
     * @param name      standard name / 표준 이름
     * @param tls13     true for TLS 1.3 / TLS 1.3 이면 true
     * @param hash      hash / 해시
     * @param keyLength AEAD key length / AEAD 키 길이
     * @param chacha    true for ChaCha20-Poly1305 / ChaCha20-Poly1305 이면 true
     * @param auth      authentication of a TLS 1.2 suite / TLS 1.2 스위트의 인증
     */
    private CipherSuite(int id, String name, boolean tls13, HashAlg hash, int keyLength, boolean chacha, int auth) {
        this.id = id;
        this.name = name;
        this.tls13 = tls13;
        this.hash = hash;
        this.keyLength = keyLength;
        this.chacha = chacha;
        this.auth = auth;
    }

    /**
     * Returns all suites of the module in the order of preference.
     * <p>
     * 모듈의 모든 스위트를 선호 순서대로 반환한다.
     *
     * @return the suites / 스위트 목록
     */
    public static CipherSuite[] all() {
        return ALL.clone();
    }

    /**
     * Returns the suite of a code point.
     * <p>
     * 코드 값의 스위트를 반환한다.
     *
     * @param id the code point / 코드 값
     * @return the suite, or null if the module does not have it / 스위트. 모듈에 없으면 null
     */
    public static CipherSuite byId(int id) {
        // 스위트가 몇 개뿐이므로 차례로 찾는다.
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id == id) {
                return ALL[i];
            }
        }
        return null;
    }

    /**
     * Returns the code point.
     * <p>
     * 코드 값을 반환한다.
     *
     * @return the code point / 코드 값
     */
    public int id() {
        return id;
    }

    /**
     * Returns the standard name, for example <code>TLS_AES_128_GCM_SHA256</code>.
     * <p>
     * 표준 이름을 반환한다. 예: <code>TLS_AES_128_GCM_SHA256</code>.
     *
     * @return the name / 이름
     */
    public String name() {
        return name;
    }

    /**
     * Tells if this is a TLS 1.3 suite.
     * <p>
     * TLS 1.3 스위트인지 알려 준다.
     *
     * @return true for TLS 1.3 / TLS 1.3 이면 true
     */
    public boolean isTls13() {
        return tls13;
    }

    /**
     * Returns the hash of the key schedule (TLS 1.3) or of the PRF (TLS 1.2).
     * <p>
     * 키 스케줄(TLS 1.3) 또는 PRF(TLS 1.2)의 해시를 반환한다.
     *
     * @return the hash / 해시
     */
    public HashAlg hash() {
        return hash;
    }

    /**
     * Returns the key length of the AEAD in bytes.
     * <p>
     * AEAD 의 키 길이를 바이트로 반환한다.
     *
     * @return the length / 길이
     */
    public int keyLength() {
        return keyLength;
    }

    /**
     * Returns the length of the IV that the key derivation gives: 12 bytes, except 4 bytes (the salt of the nonce) for AES-GCM in TLS 1.2.
     * <p>
     * 키 유도가 내는 IV 의 길이를 반환한다: 12바이트. 단 TLS 1.2 의 AES-GCM 은 4바이트(nonce 의 salt)이다.
     *
     * @return the length / 길이
     */
    public int ivLength() {
        return tls13 || chacha ? 12 : 4;
    }

    /**
     * Tells if records carry an explicit eight byte nonce (AES-GCM in TLS 1.2, RFC 5288).
     * <p>
     * 레코드가 명시적 8바이트 nonce 를 싣는지 알려 준다(TLS 1.2 의 AES-GCM, RFC 5288).
     *
     * @return true if explicit / 명시적이면 true
     */
    public boolean explicitNonce() {
        return !tls13 && !chacha;
    }

    /**
     * Returns the authentication of a TLS 1.2 suite.
     * <p>
     * TLS 1.2 스위트의 인증을 반환한다.
     *
     * @return {@link #AUTH_RSA}, {@link #AUTH_ECDSA}, or 0 for TLS 1.3 / {@link #AUTH_RSA}, {@link #AUTH_ECDSA}. TLS 1.3 이면 0
     */
    public int auth() {
        return auth;
    }

    /**
     * Creates the AEAD of the suite for a key.
     * <p>
     * 키에 대한 스위트의 AEAD 를 만든다.
     *
     * @param key the key of {@link #keyLength()} bytes / {@link #keyLength()} 바이트의 키
     * @return the AEAD / AEAD
     */
    public Aead newAead(byte[] key) {
        return chacha ? (Aead) new ChaCha20Poly1305(key) : (Aead) new AesGcm(key);
    }
}
