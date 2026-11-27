package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.math.BigInteger;
import java.util.List;
import javax.net.ssl.SSLException;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;

/**
 * The public key of a certificate (SubjectPublicKeyInfo, RFC 5280 section 4.1.2.7): an RSA key (modulus and exponent) or an EC key on
 * P-256, P-384 or P-521 (RFC 5480). Keys of other kinds are kept as "unsupported", so that a chain with such a certificate can still be
 * parsed; using the key fails. Example: <code>SubjectKey key = cert.key(); if (key.isRsa()) { key.modulus(); }</code>.
 * <p>
 * 인증서의 공개 키(SubjectPublicKeyInfo, RFC 5280 4.1.2.7절): RSA 키(법과 지수) 또는 P-256, P-384, P-521 위의 EC 키(RFC 5480). 다른 종류의 키는
 * "지원하지 않음"으로 보관하므로 그런 인증서가 든 사슬도 해석할 수 있고, 그 키를 쓰려고 하면 실패한다.
 */
public final class SubjectKey {

    /** The object identifier of rsaEncryption. / rsaEncryption 의 객체 식별자. */
    private static final String OID_RSA = "1.2.840.113549.1.1.1";

    /** The object identifier of id-ecPublicKey. / id-ecPublicKey 의 객체 식별자. */
    private static final String OID_EC = "1.2.840.10045.2.1";

    /** The whole SubjectPublicKeyInfo element. / SubjectPublicKeyInfo 원소 전체. */
    private final byte[] encoded;

    /** The OID of the key algorithm. / 키 알고리즘의 OID. */
    private final String algorithm;

    /** The RSA modulus, or null. / RSA 법. 없으면 null. */
    private BigInteger modulus;

    /** The RSA public exponent, or null. / RSA 공개 지수. 없으면 null. */
    private BigInteger exponent;

    /** The curve of an EC key, or null. / EC 키의 곡선. 없으면 null. */
    private EcCurve curve;

    /** The point of an EC key, or null. / EC 키의 점. 없으면 null. */
    private BigInteger[] point;

    /**
     * Parses a SubjectPublicKeyInfo element.
     * <p>
     * SubjectPublicKeyInfo 원소를 해석한다.
     *
     * @param spki the element / 원소
     * @throws X509Exception if the structure is malformed or an RSA or EC key is invalid / 구조가 잘못되었거나 RSA·EC 키가 유효하지 않으면
     */
    SubjectKey(Der spki) throws X509Exception {
        encoded = spki.encoded();
        List<Der> parts = spki.expect(Der.SEQUENCE).children(2);
        if (parts.size() != 2) {
            throw Der.malformed("SubjectPublicKeyInfo must have two elements");
        }
        List<Der> alg = parts.get(0).expect(Der.SEQUENCE).children(2);
        if (alg.isEmpty()) {
            throw Der.malformed("empty AlgorithmIdentifier");
        }
        algorithm = alg.get(0).oid();
        byte[] bits = parts.get(1).bitStringBytes();
        if (OID_RSA.equals(algorithm)) {
            // RSAPublicKey ::= SEQUENCE { modulus INTEGER, publicExponent INTEGER }
            List<Der> key = Der.readExactly(bits, 0, bits.length).expect(Der.SEQUENCE).children(2);
            if (key.size() != 2) {
                throw Der.malformed("RSAPublicKey must have two integers");
            }
            modulus = key.get(0).integer();
            exponent = key.get(1).integer();
            if (modulus.signum() <= 0 || exponent.signum() <= 0) {
                throw Der.malformed("negative RSA key value");
            }
        } else if (OID_EC.equals(algorithm) && alg.size() == 2 && alg.get(1).tag() == Der.OID) {
            String named = alg.get(1).oid();
            EcCurve c = "1.2.840.10045.3.1.7".equals(named) ? EcCurve.P256 : "1.3.132.0.34".equals(named) ? EcCurve.P384
                    : "1.3.132.0.35".equals(named) ? EcCurve.P521 : null;
            if (c != null) {
                try {
                    point = c.decodePoint(bits);
                    curve = c;
                } catch (SSLException e) {
                    throw Der.malformed("EC public key: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Tells if this is a usable RSA key.
     * <p>
     * 쓸 수 있는 RSA 키인지 알려 준다.
     *
     * @return true for RSA / RSA 이면 true
     */
    public boolean isRsa() {
        return modulus != null;
    }

    /**
     * Tells if this is a usable EC key.
     * <p>
     * 쓸 수 있는 EC 키인지 알려 준다.
     *
     * @return true for EC / EC 이면 true
     */
    public boolean isEc() {
        return curve != null;
    }

    /**
     * Returns the RSA modulus.
     * <p>
     * RSA 법을 반환한다.
     *
     * @return the modulus, or null for other keys / 법. 다른 키이면 null
     */
    public BigInteger modulus() {
        return modulus;
    }

    /**
     * Returns the RSA public exponent.
     * <p>
     * RSA 공개 지수를 반환한다.
     *
     * @return the exponent, or null for other keys / 지수. 다른 키이면 null
     */
    public BigInteger exponent() {
        return exponent;
    }

    /**
     * Returns the curve of an EC key.
     * <p>
     * EC 키의 곡선을 반환한다.
     *
     * @return the curve, or null for other keys / 곡선. 다른 키이면 null
     */
    public EcCurve curve() {
        return curve;
    }

    /**
     * Returns the point of an EC key.
     * <p>
     * EC 키의 점을 반환한다.
     *
     * @return {x, y}, or null for other keys / {x, y}. 다른 키이면 null
     */
    public BigInteger[] point() {
        return point == null ? null : point.clone();
    }

    /**
     * Returns the OID of the key algorithm.
     * <p>
     * 키 알고리즘의 OID 를 반환한다.
     *
     * @return the OID / OID
     */
    public String algorithm() {
        return algorithm;
    }

    /**
     * Returns the JCA style name of the key algorithm for {@link java.security.PublicKey#getAlgorithm()}.
     * <p>
     * {@link java.security.PublicKey#getAlgorithm()} 용으로 JCA 방식의 키 알고리즘 이름을 반환한다.
     *
     * @return <code>RSA</code>, <code>EC</code> or the OID / <code>RSA</code>, <code>EC</code> 또는 OID
     */
    public String algorithmName() {
        return isRsa() ? "RSA" : isEc() ? "EC" : algorithm;
    }

    /**
     * Returns the strength relevant size: the modulus length of an RSA key or the field size of an EC key; 0 for unsupported keys.
     * <p>
     * 강도와 관련된 크기를 반환한다: RSA 키는 법의 길이, EC 키는 체의 크기. 지원하지 않는 키는 0.
     *
     * @return the size in bits / 비트 단위 크기
     */
    public int bits() {
        return isRsa() ? modulus.bitLength() : isEc() ? curve.order().bitLength() : 0;
    }

    /**
     * Returns a copy of the encoded SubjectPublicKeyInfo.
     * <p>
     * 부호화된 SubjectPublicKeyInfo 의 사본을 반환한다.
     *
     * @return the encoding / 부호화
     */
    public byte[] encoded() {
        return encoded.clone();
    }

    /**
     * Tells if two keys have the same encoding.
     * <p>
     * 두 키의 부호화가 같은지 알려 준다.
     *
     * @param other the other key / 다른 키
     * @return true if equal / 같으면 true
     */
    public boolean sameAs(SubjectKey other) {
        return java.util.Arrays.equals(encoded, other.encoded);
    }
}
