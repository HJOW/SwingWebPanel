package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;
import javax.net.ssl.SSLException;

/**
 * A NIST prime curve <code>y^2 = x^3 - 3x + b</code> (P-256, P-384 or P-521; FIPS 186-4, parameters as printed in RFC 5903 section 3)
 * with the point arithmetic that ECDH and ECDSA verification need. Affine points are arrays <code>{x, y}</code>; <code>null</code> is the
 * point at infinity. The arithmetic uses <code>java.math.BigInteger</code>, whose running time depends on the values, so it is
 * <b>not constant time</b>; {@link #multiply(BigInteger, BigInteger[])} at least performs the same sequence of point operations for every
 * scalar (Montgomery ladder). Example:
 * <pre>
 * BigInteger[] q = EcCurve.P256.decodePoint(peerKeyShare);        // rejects points that are not on the curve
 * BigInteger[] s = EcCurve.P256.multiply(privateScalar, q);
 * </pre>
 * <p>
 * NIST 소수 곡선 <code>y^2 = x^3 - 3x + b</code>(P-256, P-384, P-521; FIPS 186-4, 매개변수는 RFC 5903 3절에 실린 값)과 ECDH·ECDSA 검증에 필요한 점
 * 연산. 아핀 점은 배열 <code>{x, y}</code> 이고 <code>null</code> 은 무한원점이다. 연산은 <code>java.math.BigInteger</code> 를 쓰는데 그 실행 시간이
 * 값에 따라 달라지므로 <b>상수 시간이 아니다</b>. {@link #multiply(BigInteger, BigInteger[])} 는 적어도 모든 스칼라에 대해 같은 순서의 점 연산을
 * 한다(몽고메리 사다리).
 */
public final class EcCurve {

    /** The curve P-256 (secp256r1, TLS group 23). / 곡선 P-256(secp256r1, TLS 그룹 23). */
    public static final EcCurve P256 = new EcCurve("secp256r1", 23, 32,
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff",
            "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b",
            "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296",
            "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",
            "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551");

    /** The curve P-384 (secp384r1, TLS group 24). / 곡선 P-384(secp384r1, TLS 그룹 24). */
    public static final EcCurve P384 = new EcCurve("secp384r1", 24, 48,
            "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff",
            "b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef",
            "aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760ab7",
            "3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5f",
            "ffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973");

    /** The curve P-521 (secp521r1, TLS group 25); used only to verify certificate signatures. / 곡선 P-521(secp521r1, TLS 그룹 25). 인증서 서명 검증에만 쓴다. */
    public static final EcCurve P521 = new EcCurve("secp521r1", 25, 66,
            "01ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            "0051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00",
            "00c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66",
            "011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650",
            "01fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409");

    /** The number three. / 숫자 3. */
    private static final BigInteger THREE = BigInteger.valueOf(3);

    /** The SEC name of the curve. / 곡선의 SEC 이름. */
    private final String name;

    /** The TLS NamedGroup number. / TLS NamedGroup 번호. */
    private final int group;

    /** The length of a field element in bytes. / 체 원소의 길이(바이트). */
    private final int fieldBytes;

    /** The field prime. / 체의 소수. */
    private final BigInteger p;

    /** The curve coefficient b. / 곡선 계수 b. */
    private final BigInteger b;

    /** The base point G. / 기준점 G. */
    private final BigInteger[] g;

    /** The order of the base point. / 기준점의 위수. */
    private final BigInteger n;

    /**
     * Creates a curve from hexadecimal parameters.
     * <p>
     * 16진수 매개변수로 곡선을 만든다.
     *
     * @param name       SEC name / SEC 이름
     * @param group      TLS group number / TLS 그룹 번호
     * @param fieldBytes length of a field element / 체 원소의 길이
     * @param p          field prime / 체의 소수
     * @param b          coefficient b / 계수 b
     * @param gx         x of the base point / 기준점의 x
     * @param gy         y of the base point / 기준점의 y
     * @param n          order / 위수
     */
    private EcCurve(String name, int group, int fieldBytes, String p, String b, String gx, String gy, String n) {
        this.name = name;
        this.group = group;
        this.fieldBytes = fieldBytes;
        this.p = new BigInteger(p, 16);
        this.b = new BigInteger(b, 16);
        this.g = new BigInteger[] { new BigInteger(gx, 16), new BigInteger(gy, 16) };
        this.n = new BigInteger(n, 16);
    }

    /**
     * Returns the SEC name, for example <code>secp256r1</code>.
     * <p>
     * SEC 이름을 반환한다. 예: <code>secp256r1</code>.
     *
     * @return the name / 이름
     */
    public String name() {
        return name;
    }

    /**
     * Returns the TLS NamedGroup number.
     * <p>
     * TLS NamedGroup 번호를 반환한다.
     *
     * @return the group number / 그룹 번호
     */
    public int group() {
        return group;
    }

    /**
     * Returns the length of a coordinate in bytes.
     * <p>
     * 좌표의 길이를 바이트로 반환한다.
     *
     * @return the length / 길이
     */
    public int fieldBytes() {
        return fieldBytes;
    }

    /**
     * Returns the order of the base point.
     * <p>
     * 기준점의 위수를 반환한다.
     *
     * @return the order / 위수
     */
    public BigInteger order() {
        return n;
    }

    /**
     * Returns the base point.
     * <p>
     * 기준점을 반환한다.
     *
     * @return the point {x, y} / 점 {x, y}
     */
    public BigInteger[] base() {
        return g.clone();
    }

    /**
     * Tells if an affine point with coordinates in the field lies on the curve.
     * <p>
     * 좌표가 체 안에 있는 아핀 점이 곡선 위에 있는지 알려 준다.
     *
     * @param x the x coordinate / x 좌표
     * @param y the y coordinate / y 좌표
     * @return true if 0 &lt;= x, y &lt; p and the curve equation holds / 0 &lt;= x, y &lt; p 이고 곡선 방정식이 성립하면 true
     */
    public boolean isOnCurve(BigInteger x, BigInteger y) {
        if (x.signum() < 0 || y.signum() < 0 || x.compareTo(p) >= 0 || y.compareTo(p) >= 0) {
            return false;
        }
        BigInteger left = y.multiply(y).mod(p);
        BigInteger right = x.multiply(x).subtract(THREE).multiply(x).add(b).mod(p);
        return left.equals(right);
    }

    /**
     * Decodes an uncompressed point (<code>04 | x | y</code>) and validates it. The curves have cofactor one, so a point on the curve is
     * in the right group.
     * <p>
     * 비압축 점(<code>04 | x | y</code>)을 읽고 검증한다. 이 곡선들은 여인수가 1 이므로 곡선 위의 점은 올바른 군에 속한다.
     *
     * @param encoded the encoded point / 부호화된 점
     * @return the point {x, y} / 점 {x, y}
     * @throws SSLException if the encoding is wrong or the point is not on the curve / 부호화가 틀리거나 점이 곡선 위에 없으면
     */
    public BigInteger[] decodePoint(byte[] encoded) throws SSLException {
        if (encoded.length != 1 + 2 * fieldBytes || encoded[0] != 4) {
            throw new SSLException("not an uncompressed point of " + name);
        }
        BigInteger x = new BigInteger(1, Bytes.copy(encoded, 1, fieldBytes));
        BigInteger y = new BigInteger(1, Bytes.copy(encoded, 1 + fieldBytes, fieldBytes));
        if (!isOnCurve(x, y)) {
            throw new SSLException("point is not on the curve " + name);
        }
        return new BigInteger[] { x, y };
    }

    /**
     * Encodes a point in uncompressed form.
     * <p>
     * 점을 비압축 형식으로 부호화한다.
     *
     * @param point the point {x, y} / 점 {x, y}
     * @return <code>04 | x | y</code>
     */
    public byte[] encodePoint(BigInteger[] point) {
        byte[] out = new byte[1 + 2 * fieldBytes];
        out[0] = 4;
        System.arraycopy(toFixed(point[0], fieldBytes), 0, out, 1, fieldBytes);
        System.arraycopy(toFixed(point[1], fieldBytes), 0, out, 1 + fieldBytes, fieldBytes);
        return out;
    }

    /**
     * Writes a non-negative integer as big endian bytes of a fixed length.
     * <p>
     * 음이 아닌 정수를 고정 길이의 빅 엔디언 바이트로 쓴다.
     *
     * @param v   the value, below 256^len / 값. 256^len 미만
     * @param len the length / 길이
     * @return the bytes / 바이트
     */
    public static byte[] toFixed(BigInteger v, int len) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[len];
        int n = Math.min(raw.length, len);
        System.arraycopy(raw, raw.length - n, out, len - n, n);
        return out;
    }

    /**
     * Doubles a point in Jacobian coordinates {X, Y, Z} (Z = 0 is infinity), using a = -3.
     * <p>
     * 야코비 좌표 {X, Y, Z}(Z = 0 이 무한원점)의 점을 두 배 한다. a = -3 을 이용한다.
     *
     * @param pt the point / 점
     * @return the doubled point / 두 배 한 점
     */
    private BigInteger[] dbl(BigInteger[] pt) {
        BigInteger x = pt[0], y = pt[1], z = pt[2];
        if (z.signum() == 0 || y.signum() == 0) {
            return new BigInteger[] { BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO };
        }
        BigInteger delta = z.multiply(z).mod(p);
        BigInteger gamma = y.multiply(y).mod(p);
        BigInteger beta = x.multiply(gamma).mod(p);
        BigInteger alpha = THREE.multiply(x.subtract(delta)).multiply(x.add(delta)).mod(p);
        BigInteger x3 = alpha.multiply(alpha).subtract(beta.shiftLeft(3)).mod(p);
        BigInteger yz = y.add(z);
        BigInteger z3 = yz.multiply(yz).subtract(gamma).subtract(delta).mod(p);
        BigInteger y3 = alpha.multiply(beta.shiftLeft(2).subtract(x3)).subtract(gamma.multiply(gamma).shiftLeft(3)).mod(p);
        return new BigInteger[] { x3, y3, z3 };
    }

    /**
     * Adds two points in Jacobian coordinates; handles infinity, equal points and opposite points.
     * <p>
     * 야코비 좌표의 두 점을 더한다. 무한원점, 같은 점, 반대 점을 처리한다.
     *
     * @param a first point / 첫 점
     * @param c second point / 둘째 점
     * @return the sum / 합
     */
    private BigInteger[] add(BigInteger[] a, BigInteger[] c) {
        if (a[2].signum() == 0) {
            return c;
        }
        if (c[2].signum() == 0) {
            return a;
        }
        BigInteger z1z1 = a[2].multiply(a[2]).mod(p);
        BigInteger z2z2 = c[2].multiply(c[2]).mod(p);
        BigInteger u1 = a[0].multiply(z2z2).mod(p);
        BigInteger u2 = c[0].multiply(z1z1).mod(p);
        BigInteger s1 = a[1].multiply(c[2]).multiply(z2z2).mod(p);
        BigInteger s2 = c[1].multiply(a[2]).multiply(z1z1).mod(p);
        BigInteger h = u2.subtract(u1).mod(p);
        BigInteger r = s2.subtract(s1).mod(p);
        if (h.signum() == 0) {
            // x 가 같다: 같은 점이면 두 배, 반대 점이면 무한원점.
            if (r.signum() == 0) {
                return dbl(a);
            }
            return new BigInteger[] { BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO };
        }
        BigInteger hh = h.multiply(h).mod(p);
        BigInteger hhh = h.multiply(hh).mod(p);
        BigInteger v = u1.multiply(hh).mod(p);
        BigInteger x3 = r.multiply(r).subtract(hhh).subtract(v.shiftLeft(1)).mod(p);
        BigInteger y3 = r.multiply(v.subtract(x3)).subtract(s1.multiply(hhh)).mod(p);
        BigInteger z3 = a[2].multiply(c[2]).multiply(h).mod(p);
        return new BigInteger[] { x3, y3, z3 };
    }

    /**
     * Converts a Jacobian point to affine coordinates.
     * <p>
     * 야코비 점을 아핀 좌표로 바꾼다.
     *
     * @param pt the point / 점
     * @return {x, y}, or null for infinity / {x, y}. 무한원점이면 null
     */
    private BigInteger[] toAffine(BigInteger[] pt) {
        if (pt[2].signum() == 0) {
            return null;
        }
        BigInteger zi = pt[2].modInverse(p);
        BigInteger zi2 = zi.multiply(zi).mod(p);
        return new BigInteger[] { pt[0].multiply(zi2).mod(p), pt[1].multiply(zi2).multiply(zi).mod(p) };
    }

    /**
     * Multiplies a point by a scalar with a Montgomery ladder: one addition and one doubling for every bit of the order, whatever the
     * scalar is. Used with secret scalars (ECDH).
     * <p>
     * 몽고메리 사다리로 점에 스칼라를 곱한다: 스칼라가 무엇이든 위수의 비트마다 덧셈 한 번과 두 배 한 번을 한다. 비밀 스칼라(ECDH)에 쓴다.
     *
     * @param k     the scalar, 0 &lt;= k &lt; n / 스칼라. 0 &lt;= k &lt; n
     * @param point the affine point / 아핀 점
     * @return the product, or null for infinity / 곱. 무한원점이면 null
     */
    public BigInteger[] multiply(BigInteger k, BigInteger[] point) {
        BigInteger[] r0 = { BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO };
        BigInteger[] r1 = { point[0], point[1], BigInteger.ONE };
        // 위수의 비트 수만큼 위에서부터 사다리를 밟는다.
        for (int i = n.bitLength() - 1; i >= 0; i--) {
            if (k.testBit(i)) {
                r0 = add(r0, r1);
                r1 = dbl(r1);
            } else {
                r1 = add(r0, r1);
                r0 = dbl(r0);
            }
        }
        return toAffine(r0);
    }

    /**
     * Computes <code>u1*G + u2*Q</code> with interleaved doubling (Shamir's trick). Only for public values (signature verification).
     * <p>
     * <code>u1*G + u2*Q</code> 를 두 배 연산을 공유해 계산한다(샤미르의 방법). 공개 값(서명 검증)에만 쓴다.
     *
     * @param u1 the scalar of the base point / 기준점의 스칼라
     * @param u2 the scalar of the other point / 다른 점의 스칼라
     * @param q  the other affine point / 다른 아핀 점
     * @return the sum, or null for infinity / 합. 무한원점이면 null
     */
    public BigInteger[] multiplyTwo(BigInteger u1, BigInteger u2, BigInteger[] q) {
        BigInteger[] jg = { g[0], g[1], BigInteger.ONE };
        BigInteger[] jq = { q[0], q[1], BigInteger.ONE };
        BigInteger[] both = add(jg, jq);
        BigInteger[] r = { BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO };
        // 두 스칼라의 비트를 위에서부터 함께 본다.
        for (int i = Math.max(u1.bitLength(), u2.bitLength()) - 1; i >= 0; i--) {
            r = dbl(r);
            boolean b1 = u1.testBit(i);
            boolean b2 = u2.testBit(i);
            if (b1 && b2) {
                r = add(r, both);
            } else if (b1) {
                r = add(r, jg);
            } else if (b2) {
                r = add(r, jq);
            }
        }
        return toAffine(r);
    }
}
