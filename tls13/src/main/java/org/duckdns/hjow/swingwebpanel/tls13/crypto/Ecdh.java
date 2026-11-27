package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.net.ssl.SSLException;

/**
 * An ephemeral ECDH key pair on a NIST curve (key exchange groups secp256r1 and secp384r1 of TLS; RFC 8446 section 4.2.8.2, RFC 8422).
 * The key pair is made for one handshake and destroyed afterwards. The arithmetic is not constant time (see {@link EcCurve}); a key is
 * used for one scalar multiplication only. Usage:
 * <pre>
 * Ecdh dh = new Ecdh(EcCurve.P256, random);
 * byte[] share = dh.publicKey();                 // 04 | x | y
 * byte[] secret = dh.sharedSecret(peerShare);    // the x coordinate, throws for an invalid peer point
 * dh.destroy();
 * </pre>
 * <p>
 * NIST 곡선 위의 임시 ECDH 키 쌍(TLS 의 키 교환 그룹 secp256r1, secp384r1; RFC 8446 4.2.8.2절, RFC 8422). 키 쌍은 핸드셰이크 한 번을 위해 만들고
 * 그 뒤에 없앤다. 연산은 상수 시간이 아니다({@link EcCurve} 참고). 키 하나는 스칼라 곱 한 번에만 쓴다.
 */
public final class Ecdh {

    /** The curve. / 곡선. */
    private final EcCurve curve;

    /** The private scalar, or null after {@link #destroy()}. / 개인 스칼라. {@link #destroy()} 뒤에는 null. */
    private BigInteger priv;

    /** The encoded public point. / 부호화한 공개 점. */
    private final byte[] pub;

    /**
     * Generates a key pair: a uniformly random scalar in [1, n - 1] by rejection sampling.
     * <p>
     * 키 쌍을 만든다: 기각 추출로 [1, n - 1] 에서 균일한 난수 스칼라를 고른다.
     *
     * @param curve  the curve / 곡선
     * @param random the random source / 난수원
     */
    public Ecdh(EcCurve curve, SecureRandom random) {
        this(curve, randomScalar(curve, random));
    }

    /**
     * Creates the key pair of a given private scalar (for test vectors).
     * <p>
     * 주어진 개인 스칼라의 키 쌍을 만든다(시험 벡터용).
     *
     * @param curve the curve / 곡선
     * @param priv  the private scalar in [1, n - 1] / [1, n - 1] 의 개인 스칼라
     */
    Ecdh(EcCurve curve, BigInteger priv) {
        if (priv.signum() <= 0 || priv.compareTo(curve.order()) >= 0) {
            throw new IllegalArgumentException("private scalar out of range");
        }
        this.curve = curve;
        this.priv = priv;
        this.pub = curve.encodePoint(curve.multiply(priv, curve.base()));
    }

    /**
     * Draws a scalar in [1, n - 1].
     * <p>
     * [1, n - 1] 의 스칼라를 뽑는다.
     *
     * @param curve  the curve / 곡선
     * @param random the random source / 난수원
     * @return the scalar / 스칼라
     */
    private static BigInteger randomScalar(EcCurve curve, SecureRandom random) {
        BigInteger n = curve.order();
        // 범위 안의 값이 나올 때까지 다시 뽑는다(P-256, P-384 에서는 거의 항상 첫 번에 끝난다).
        while (true) {
            BigInteger k = new BigInteger(n.bitLength(), random);
            if (k.signum() > 0 && k.compareTo(n) < 0) {
                return k;
            }
        }
    }

    /**
     * Returns the public key as an uncompressed point.
     * <p>
     * 공개 키를 비압축 점으로 반환한다.
     *
     * @return <code>04 | x | y</code>
     */
    public byte[] publicKey() {
        return pub.clone();
    }

    /**
     * Computes the shared secret: the x coordinate of <code>priv * peer</code>, as bytes of the field length.
     * <p>
     * 공유 비밀을 계산한다: <code>priv * peer</code> 의 x 좌표를 체 길이의 바이트로 낸다.
     *
     * @param peer the uncompressed public point of the peer / 상대의 비압축 공개 점
     * @return the shared secret / 공유 비밀
     * @throws SSLException if the peer point is malformed or not on the curve / 상대 점이 잘못되었거나 곡선 위에 없으면
     */
    public byte[] sharedSecret(byte[] peer) throws SSLException {
        if (priv == null) {
            throw new IllegalStateException("the key pair was destroyed");
        }
        BigInteger[] q = curve.decodePoint(peer);
        BigInteger[] s = curve.multiply(priv, q);
        if (s == null) {
            // 유효한 점과 범위 안의 스칼라에서는 일어나지 않는다.
            throw new SSLException("ECDH result is the point at infinity");
        }
        return EcCurve.toFixed(s[0], curve.fieldBytes());
    }

    /**
     * Drops the private scalar. (A <code>BigInteger</code> cannot be overwritten; the reference is released.)
     * <p>
     * 개인 스칼라를 버린다. (<code>BigInteger</code> 는 덮어쓸 수 없으므로 참조만 놓는다.)
     */
    public void destroy() {
        priv = null;
    }
}
