package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.math.BigInteger;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Ecdsa;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.RsaVerify;

/**
 * A signature algorithm with its parameters, and the verification with a certificate key. It comes either from the AlgorithmIdentifier
 * of a certificate (RFC 5280, RFC 4055, RFC 5758) or from a SignatureScheme of TLS (RFC 8446 section 4.2.3). Supported: RSA PKCS #1
 * v1.5, RSA-PSS and ECDSA, each with SHA-256, SHA-384 or SHA-512. SHA-1 and MD5 are rejected as weak. Example:
 * <pre>
 * SignatureAlgorithm alg = SignatureAlgorithm.forTlsScheme(0x0804);        // rsa_pss_rsae_sha256
 * boolean ok = alg.verify(cert.key(), signedContent, signature);
 * </pre>
 * <p>
 * 매개변수를 가진 서명 알고리즘과 인증서 키로 하는 검증. 인증서의 AlgorithmIdentifier(RFC 5280, RFC 4055, RFC 5758) 또는 TLS 의
 * SignatureScheme(RFC 8446 4.2.3절)에서 만든다. 지원: SHA-256, SHA-384, SHA-512 의 RSA PKCS #1 v1.5, RSA-PSS, ECDSA. SHA-1 과 MD5 는 약한 것으로 거부한다.
 */
public final class SignatureAlgorithm {

    /** Kind: RSASSA-PKCS1-v1_5. / 종류: RSASSA-PKCS1-v1_5. */
    public static final int RSA_PKCS1 = 1;

    /** Kind: RSASSA-PSS. / 종류: RSASSA-PSS. */
    public static final int RSA_PSS = 2;

    /** Kind: ECDSA. / 종류: ECDSA. */
    public static final int ECDSA = 3;

    /** The kind. / 종류. */
    private final int kind;

    /** The hash. / 해시. */
    private final HashAlg hash;

    /** The salt length of RSA-PSS. / RSA-PSS 의 salt 길이. */
    private final int saltLength;

    /**
     * Creates an algorithm.
     * <p>
     * 알고리즘을 만든다.
     *
     * @param kind       the kind / 종류
     * @param hash       the hash / 해시
     * @param saltLength the PSS salt length / PSS salt 길이
     */
    private SignatureAlgorithm(int kind, HashAlg hash, int saltLength) {
        this.kind = kind;
        this.hash = hash;
        this.saltLength = saltLength;
    }

    /**
     * Returns the kind: {@link #RSA_PKCS1}, {@link #RSA_PSS} or {@link #ECDSA}.
     * <p>
     * 종류를 반환한다: {@link #RSA_PKCS1}, {@link #RSA_PSS}, {@link #ECDSA}.
     *
     * @return the kind / 종류
     */
    public int kind() {
        return kind;
    }

    /**
     * Returns the hash.
     * <p>
     * 해시를 반환한다.
     *
     * @return the hash / 해시
     */
    public HashAlg hash() {
        return hash;
    }

    /**
     * Returns the hash of a digest algorithm OID, or null.
     * <p>
     * 다이제스트 알고리즘 OID 의 해시를 반환한다. 없으면 null.
     *
     * @param oid the OID / OID
     * @return the hash or null / 해시 또는 null
     */
    private static HashAlg hashOf(String oid) {
        return "2.16.840.1.101.3.4.2.1".equals(oid) ? HashAlg.SHA256 : "2.16.840.1.101.3.4.2.2".equals(oid) ? HashAlg.SHA384
                : "2.16.840.1.101.3.4.2.3".equals(oid) ? HashAlg.SHA512 : null;
    }

    /**
     * Parses the AlgorithmIdentifier of a certificate signature.
     * <p>
     * 인증서 서명의 AlgorithmIdentifier 를 해석한다.
     *
     * @param algId the element / 원소
     * @return the algorithm / 알고리즘
     * @throws X509Exception WEAK_SIGNATURE for SHA-1 and MD5, UNSUPPORTED_ALGORITHM for others, MALFORMED for a bad structure /
     *                       SHA-1·MD5 는 WEAK_SIGNATURE, 그 밖은 UNSUPPORTED_ALGORITHM, 구조가 틀리면 MALFORMED
     */
    static SignatureAlgorithm parse(Der algId) throws X509Exception {
        List<Der> parts = algId.expect(Der.SEQUENCE).children(2);
        if (parts.isEmpty()) {
            throw Der.malformed("empty AlgorithmIdentifier");
        }
        String oid = parts.get(0).oid();
        if ("1.2.840.113549.1.1.11".equals(oid)) {
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA256, 0);
        }
        if ("1.2.840.113549.1.1.12".equals(oid)) {
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA384, 0);
        }
        if ("1.2.840.113549.1.1.13".equals(oid)) {
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA512, 0);
        }
        if ("1.2.840.10045.4.3.2".equals(oid)) {
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA256, 0);
        }
        if ("1.2.840.10045.4.3.3".equals(oid)) {
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA384, 0);
        }
        if ("1.2.840.10045.4.3.4".equals(oid)) {
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA512, 0);
        }
        if ("1.2.840.113549.1.1.10".equals(oid)) {
            return parsePss(parts);
        }
        if ("1.2.840.113549.1.1.5".equals(oid) || "1.2.840.113549.1.1.4".equals(oid) || "1.2.840.113549.1.1.2".equals(oid)
                || "1.2.840.10045.4.1".equals(oid) || "1.3.14.3.2.29".equals(oid)) {
            throw new X509Exception(X509Exception.Reason.WEAK_SIGNATURE, "signature with SHA-1 or MD5 (" + oid + ")");
        }
        throw new X509Exception(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "signature algorithm " + oid);
    }

    /**
     * Parses RSASSA-PSS-params (RFC 4055 section 3.1). The mask generation function must be MGF1 with the hash of the message.
     * <p>
     * RSASSA-PSS-params 를 해석한다(RFC 4055 3.1절). 마스크 생성 함수는 메시지 해시와 같은 해시의 MGF1 이어야 한다.
     *
     * @param parts the elements of the AlgorithmIdentifier / AlgorithmIdentifier 의 원소들
     * @return the algorithm / 알고리즘
     * @throws X509Exception if the parameters are missing, weak or unsupported / 매개변수가 없거나 약하거나 지원하지 않으면
     */
    private static SignatureAlgorithm parsePss(List<Der> parts) throws X509Exception {
        if (parts.size() != 2) {
            throw new X509Exception(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "RSASSA-PSS without parameters");
        }
        HashAlg hash = null;
        HashAlg mgfHash = null;
        int salt = 20;
        List<Der> fields = parts.get(1).expect(Der.SEQUENCE).children(4);
        // [0] hashAlgorithm, [1] maskGenAlgorithm, [2] saltLength, [3] trailerField 를 차례로 본다.
        for (int i = 0; i < fields.size(); i++) {
            Der f = fields.get(i);
            if (f.tag() == 0xa0) {
                hash = hashOf(f.inner().expect(Der.SEQUENCE).children(2).get(0).oid());
                if (hash == null) {
                    throw new X509Exception(X509Exception.Reason.WEAK_SIGNATURE, "RSASSA-PSS with an unsupported or weak hash");
                }
            } else if (f.tag() == 0xa1) {
                List<Der> mgf = f.inner().expect(Der.SEQUENCE).children(2);
                if (mgf.size() != 2 || !"1.2.840.113549.1.1.8".equals(mgf.get(0).oid())) {
                    throw new X509Exception(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "RSASSA-PSS mask generation function");
                }
                mgfHash = hashOf(mgf.get(1).expect(Der.SEQUENCE).children(2).get(0).oid());
            } else if (f.tag() == 0xa2) {
                BigInteger v = f.inner().integer();
                if (v.signum() < 0 || v.bitLength() > 16) {
                    throw Der.malformed("RSASSA-PSS salt length");
                }
                salt = v.intValue();
            } else if (f.tag() == 0xa3) {
                if (!BigInteger.ONE.equals(f.inner().integer())) {
                    throw new X509Exception(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "RSASSA-PSS trailer field");
                }
            } else {
                throw Der.malformed("unexpected RSASSA-PSS parameter");
            }
        }
        // 해시를 생략하면 기본값이 SHA-1 이므로 거부한다.
        if (hash == null || mgfHash != hash) {
            throw new X509Exception(X509Exception.Reason.WEAK_SIGNATURE, "RSASSA-PSS with SHA-1 or mismatching hashes");
        }
        return new SignatureAlgorithm(RSA_PSS, hash, salt);
    }

    /**
     * Returns the algorithm of a TLS SignatureScheme code point that this module accepts.
     * <p>
     * 이 모듈이 받아들이는 TLS SignatureScheme 코드의 알고리즘을 반환한다.
     *
     * @param scheme the code point, for example 0x0804 for rsa_pss_rsae_sha256 / 코드. 예: rsa_pss_rsae_sha256 은 0x0804
     * @return the algorithm, or null for an unsupported scheme / 알고리즘. 지원하지 않는 방식이면 null
     */
    public static SignatureAlgorithm forTlsScheme(int scheme) {
        switch (scheme) {
        case 0x0401:
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA256, 0);
        case 0x0501:
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA384, 0);
        case 0x0601:
            return new SignatureAlgorithm(RSA_PKCS1, HashAlg.SHA512, 0);
        case 0x0403:
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA256, 0);
        case 0x0503:
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA384, 0);
        case 0x0603:
            return new SignatureAlgorithm(ECDSA, HashAlg.SHA512, 0);
        case 0x0804:
            return new SignatureAlgorithm(RSA_PSS, HashAlg.SHA256, 32);
        case 0x0805:
            return new SignatureAlgorithm(RSA_PSS, HashAlg.SHA384, 48);
        case 0x0806:
            return new SignatureAlgorithm(RSA_PSS, HashAlg.SHA512, 64);
        default:
            return null;
        }
    }

    /**
     * Verifies a signature with a certificate key. The key must be of the kind that the algorithm needs.
     * <p>
     * 인증서 키로 서명을 검증한다. 키는 알고리즘이 필요로 하는 종류여야 한다.
     *
     * @param key       the public key / 공개 키
     * @param message   the signed data / 서명한 데이터
     * @param signature the signature (ECDSA: the DER structure of r and s) / 서명(ECDSA 는 r 과 s 의 DER 구조)
     * @return true if the signature is valid / 서명이 유효하면 true
     */
    public boolean verify(SubjectKey key, byte[] message, byte[] signature) {
        if (kind == ECDSA) {
            if (!key.isEc()) {
                return false;
            }
            try {
                // ECDSA-Sig-Value ::= SEQUENCE { r INTEGER, s INTEGER }
                List<Der> rs = Der.readExactly(signature, 0, signature.length).expect(Der.SEQUENCE).children(2);
                if (rs.size() != 2) {
                    return false;
                }
                return Ecdsa.verify(key.curve(), key.point(), hash.digest(message), rs.get(0).integer(), rs.get(1).integer());
            } catch (X509Exception e) {
                return false;
            }
        }
        if (!key.isRsa()) {
            return false;
        }
        if (kind == RSA_PSS) {
            return RsaVerify.pss(key.modulus(), key.exponent(), hash, message, signature, saltLength);
        }
        return RsaVerify.pkcs1(key.modulus(), key.exponent(), hash, message, signature);
    }

    /**
     * Returns the JCA style name, for example <code>SHA256withRSA</code>.
     * <p>
     * JCA 방식의 이름을 반환한다. 예: <code>SHA256withRSA</code>.
     *
     * @return the name / 이름
     */
    public String name() {
        String h = hash.name().replace("-", "");
        return kind == ECDSA ? h + "withECDSA" : kind == RSA_PSS ? "RSASSA-PSS" : h + "withRSA";
    }
}
