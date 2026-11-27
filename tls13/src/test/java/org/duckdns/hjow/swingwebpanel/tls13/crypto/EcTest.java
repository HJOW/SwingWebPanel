package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import javax.net.ssl.SSLException;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.Vectors;

/**
 * The NIST curves of the TLS module (plan item P32-03-e): consistency of the parameters, scalar multiplication and ECDSA verification
 * with the key pairs and signatures of RFC 6979 appendix A.2.5 to A.2.7, ECDH with the examples of RFC 5903 section 8, and the rejection
 * of invalid points and of signature values out of range.
 * <p>
 * TLS 모듈의 NIST 곡선(계획 P32-03-e): 매개변수의 일관성, RFC 6979 부록 A.2.5~A.2.7 의 키 쌍과 서명으로 본 스칼라 곱과 ECDSA 검증,
 * RFC 5903 8절의 예로 본 ECDH, 잘못된 점과 범위 밖 서명 값의 거부.
 */
public class EcTest {

    /** The vectors extracted from the RFCs. / RFC 에서 뽑은 벡터. */
    private static final Vectors VECTORS = Vectors.load("/ec-vectors.txt");

    /** The curves and the names used in the vector file. / 곡선과 벡터 파일에서 쓰는 이름. */
    private static final Object[][] CURVES = { { EcCurve.P256, "P-256" }, { EcCurve.P384, "P-384" }, { EcCurve.P521, "P-521" } };

    /** The hash functions and the names used in the vector file. / 해시 함수와 벡터 파일에서 쓰는 이름. */
    private static final Object[][] HASHES = { { HashAlg.SHA256, "SHA-256" }, { HashAlg.SHA384, "SHA-384" }, { HashAlg.SHA512, "SHA-512" } };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public EcTest() {
    }

    /**
     * Reads a hexadecimal vector as a number.
     * <p>
     * 16진수 벡터를 수로 읽는다.
     *
     * @param curve the curve name in the file / 파일의 곡선 이름
     * @param name  the value name / 값 이름
     * @return the number / 수
     */
    private static BigInteger num(String curve, String name) {
        return new BigInteger(VECTORS.value(curve, name), 16);
    }

    /**
     * The parameters are consistent: the base point is on the curve, n times the base point is infinity and (n - 1) times is its negative.
     * <p>
     * 매개변수가 일관된다: 기준점이 곡선 위에 있고, 기준점의 n 배는 무한원점이며 (n - 1) 배는 그 음수이다.
     */
    public void testCurveParameters() {
        // 곡선마다 확인한다.
        for (int i = 0; i < CURVES.length; i++) {
            EcCurve c = (EcCurve) CURVES[i][0];
            BigInteger[] g = c.base();
            Check.isTrue(c.isOnCurve(g[0], g[1]), c.name() + ": the base point is on the curve");
            Check.isNull(c.multiply(c.order(), g), c.name() + ": n*G is the point at infinity");
            BigInteger[] minus = c.multiply(c.order().subtract(BigInteger.ONE), g);
            Check.equal(g[0], minus[0], c.name() + ": (n-1)*G has the x of G");
            Check.isFalse(g[1].equals(minus[1]), c.name() + ": (n-1)*G has the other y");
            Check.isTrue(c.isOnCurve(minus[0], minus[1]), c.name() + ": (n-1)*G is on the curve");
            BigInteger[] two = c.multiply(BigInteger.valueOf(2), g);
            BigInteger[] three = c.multiplyTwo(BigInteger.ONE, BigInteger.valueOf(2), g);
            Check.equal(c.multiply(BigInteger.valueOf(3), g)[0], three[0], c.name() + ": G + 2G = 3G (the two points are equal, so the addition doubles)");
            Check.isNull(c.multiplyTwo(BigInteger.ONE, BigInteger.ONE, minus), c.name() + ": G + (-G) is infinity");
            Check.isTrue(c.isOnCurve(two[0], two[1]), c.name() + ": 2G is on the curve");
            Check.equal(1 + 2 * c.fieldBytes(), c.encodePoint(g).length, c.name() + ": encoded length");
        }
        Check.equal(23, EcCurve.P256.group(), "group number of secp256r1");
        Check.equal(24, EcCurve.P384.group(), "group number of secp384r1");
    }

    /**
     * The public keys of RFC 6979 are the private keys times the base point, by both multiplication routines.
     * <p>
     * RFC 6979 의 공개 키가 개인 키와 기준점의 곱이다(두 곱셈 경로 모두).
     */
    public void testScalarMultiplicationRfc6979() {
        // 곡선마다 확인한다.
        for (int i = 0; i < CURVES.length; i++) {
            EcCurve c = (EcCurve) CURVES[i][0];
            String name = (String) CURVES[i][1];
            BigInteger x = num(name, "x");
            BigInteger[] u = c.multiply(x, c.base());
            Check.equal(num(name, "Ux"), u[0], name + ": Ux by the ladder");
            Check.equal(num(name, "Uy"), u[1], name + ": Uy by the ladder");
            BigInteger[] v = c.multiplyTwo(x, BigInteger.ZERO, c.base());
            Check.equal(u[0], v[0], name + ": Ux by the interleaved multiplication");
            Check.equal(u[1], v[1], name + ": Uy by the interleaved multiplication");
        }
    }

    /**
     * The signatures of RFC 6979 over the message "sample" verify for every curve and hash; any change makes them invalid.
     * <p>
     * RFC 6979 의 "sample" 메시지 서명이 모든 곡선과 해시에서 검증되고, 무엇이든 바꾸면 무효가 된다.
     */
    public void testEcdsaRfc6979() {
        byte[] message = { 's', 'a', 'm', 'p', 'l', 'e' };
        // 곡선마다, 해시마다 확인한다.
        for (int i = 0; i < CURVES.length; i++) {
            EcCurve c = (EcCurve) CURVES[i][0];
            String name = (String) CURVES[i][1];
            BigInteger[] q = { num(name, "Ux"), num(name, "Uy") };
            // 해시마다 서명을 검증한다.
            for (int h = 0; h < HASHES.length; h++) {
                HashAlg hash = (HashAlg) HASHES[h][0];
                String hn = (String) HASHES[h][1];
                BigInteger r = num(name, hn + "-r");
                BigInteger s = num(name, hn + "-s");
                byte[] digest = hash.digest(message);
                String what = name + " with " + hn;
                Check.isTrue(Ecdsa.verify(c, q, digest, r, s), what + ": the signature is valid");
                Check.isFalse(Ecdsa.verify(c, q, hash.digest(new byte[] { 't', 'e', 's', 't' }), r, s), what + ": another message");
                Check.isFalse(Ecdsa.verify(c, q, digest, r.add(BigInteger.ONE), s), what + ": r + 1");
                Check.isFalse(Ecdsa.verify(c, q, digest, r, s.add(BigInteger.ONE)), what + ": s + 1");
                Check.isFalse(Ecdsa.verify(c, q, digest, s, r), what + ": r and s exchanged");
                Check.isFalse(Ecdsa.verify(c, c.base(), digest, r, s), what + ": another public key");
                Check.isFalse(Ecdsa.verify(c, q, digest, BigInteger.ZERO, s), what + ": r = 0");
                Check.isFalse(Ecdsa.verify(c, q, digest, r, BigInteger.ZERO), what + ": s = 0");
                Check.isFalse(Ecdsa.verify(c, q, digest, r.add(c.order()), s), what + ": r + n (out of range)");
                Check.isFalse(Ecdsa.verify(c, q, digest, r, c.order()), what + ": s = n");
                Check.isFalse(Ecdsa.verify(c, q, digest, r.negate(), s), what + ": negative r");
                Check.isFalse(Ecdsa.verify(c, new BigInteger[] { q[0], q[1].add(BigInteger.ONE) }, digest, r, s), what + ": key not on the curve");
            }
        }
    }

    /**
     * The ECDH examples of RFC 5903: public keys and the shared x coordinate, computed by both sides.
     * <p>
     * RFC 5903 의 ECDH 예: 공개 키와 공유 x 좌표를 양쪽에서 계산한다.
     *
     * @throws SSLException never for these inputs / 이 입력에서는 발생하지 않는다
     */
    public void testEcdhRfc5903() throws SSLException {
        // 곡선마다 확인한다.
        for (int i = 0; i < CURVES.length; i++) {
            EcCurve c = (EcCurve) CURVES[i][0];
            String name = (String) CURVES[i][1];
            Ecdh initiator = new Ecdh(c, num(name, "ecdh-i"));
            Ecdh responder = new Ecdh(c, num(name, "ecdh-r"));
            byte[] gi = c.encodePoint(new BigInteger[] { num(name, "ecdh-gix"), num(name, "ecdh-giy") });
            byte[] gr = c.encodePoint(new BigInteger[] { num(name, "ecdh-grx"), num(name, "ecdh-gry") });
            Check.equal(Hex.encode(gi), Hex.encode(initiator.publicKey()), name + ": public key of the initiator");
            Check.equal(Hex.encode(gr), Hex.encode(responder.publicKey()), name + ": public key of the responder");
            String shared = Hex.encode(EcCurve.toFixed(num(name, "ecdh-girx"), c.fieldBytes()));
            Check.equal(shared, Hex.encode(initiator.sharedSecret(gr)), name + ": shared secret of the initiator");
            Check.equal(shared, Hex.encode(responder.sharedSecret(gi)), name + ": shared secret of the responder");
            Check.equal(c.fieldBytes(), initiator.sharedSecret(gr).length, name + ": the shared secret has the field length");
        }
    }

    /**
     * Two freshly generated key pairs agree on the shared secret; a destroyed key pair cannot be used.
     * <p>
     * 새로 만든 키 쌍 둘이 같은 공유 비밀에 이른다. 없앤 키 쌍은 쓸 수 없다.
     *
     * @throws SSLException never for these inputs / 이 입력에서는 발생하지 않는다
     */
    public void testGeneratedKeyPairs() throws SSLException {
        SecureRandom rnd = new SecureRandom();
        EcCurve[] curves = { EcCurve.P256, EcCurve.P384 };
        // 키 교환에 쓰는 두 곡선을 확인한다.
        for (int i = 0; i < curves.length; i++) {
            final Ecdh a = new Ecdh(curves[i], rnd);
            final Ecdh b = new Ecdh(curves[i], rnd);
            Check.isFalse(Hex.encode(a.publicKey()).equals(Hex.encode(b.publicKey())), "two key pairs differ");
            Check.equal(Hex.encode(a.sharedSecret(b.publicKey())), Hex.encode(b.sharedSecret(a.publicKey())), curves[i].name() + ": both sides agree");
            a.destroy();
            Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    a.sharedSecret(b.publicKey());
                }
            }, "use after destroy");
        }
    }

    /**
     * Peer points that are malformed or not on the curve are rejected before any multiplication.
     * <p>
     * 형식이 틀리거나 곡선 위에 없는 상대 점은 곱셈 전에 거부한다.
     */
    public void testInvalidPointsAreRejected() {
        final EcCurve c = EcCurve.P256;
        final Ecdh dh = new Ecdh(c, num("P-256", "ecdh-i"));
        byte[] good = c.encodePoint(c.base());
        byte[] offCurve = good.clone();
        offCurve[offCurve.length - 1] ^= 1;
        byte[] compressed = Bytes.copy(good, 0, 33);
        compressed[0] = 2;
        byte[] wrongPrefix = good.clone();
        wrongPrefix[0] = 6;
        byte[] zero = new byte[65];
        zero[0] = 4;
        byte[] xIsP = good.clone();
        System.arraycopy(Hex.decode("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff"), 0, xIsP, 1, 32);
        byte[] p384Point = EcCurve.P384.encodePoint(EcCurve.P384.base());
        final byte[][] bad = { offCurve, compressed, wrongPrefix, zero, xIsP, p384Point, new byte[] { 0 }, new byte[0], Bytes.copy(good, 0, 64) };
        // 잘못된 점을 하나씩 넣어 본다.
        for (int i = 0; i < bad.length; i++) {
            final byte[] point = bad[i];
            Check.throwsException(SSLException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    dh.sharedSecret(point);
                }
            }, "invalid point #" + i);
        }
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new Ecdh(c, BigInteger.ZERO);
            }
        }, "private scalar 0");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new Ecdh(c, c.order());
            }
        }, "private scalar n");
    }
}
