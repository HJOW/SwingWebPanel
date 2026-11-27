package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;

/**
 * ECDSA signature verification (FIPS 186-4 section 6.4) on the NIST curves. Only public values are involved. The caller hashes the
 * message and decodes the DER structure of the signature into r and s. Example:
 * <code>boolean ok = Ecdsa.verify(EcCurve.P256, publicPoint, HashAlg.SHA256.digest(message), r, s);</code>.
 * <p>
 * NIST 곡선의 ECDSA 서명 검증(FIPS 186-4 6.4절). 공개 값만 다룬다. 호출자가 메시지의 해시를 구하고 서명의 DER 구조를 r 과 s 로 풀어 준다.
 */
public final class Ecdsa {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Ecdsa() {
    }

    /**
     * Verifies a signature.
     * <p>
     * 서명을 검증한다.
     *
     * @param curve the curve of the key / 키의 곡선
     * @param q     the public point {x, y}, already validated to be on the curve / 공개 점 {x, y}. 곡선 위에 있음을 이미 검증한 것
     * @param hash  the hash of the message / 메시지의 해시
     * @param r     the first signature value / 서명의 첫 값
     * @param s     the second signature value / 서명의 둘째 값
     * @return true if the signature is valid / 서명이 유효하면 true
     */
    public static boolean verify(EcCurve curve, BigInteger[] q, byte[] hash, BigInteger r, BigInteger s) {
        BigInteger n = curve.order();
        if (r.signum() <= 0 || s.signum() <= 0 || r.compareTo(n) >= 0 || s.compareTo(n) >= 0) {
            return false;
        }
        if (q == null || !curve.isOnCurve(q[0], q[1])) {
            return false;
        }
        BigInteger e = new BigInteger(1, hash);
        int extra = hash.length * 8 - n.bitLength();
        if (extra > 0) {
            // 해시가 위수보다 길면 왼쪽 비트만 쓴다.
            e = e.shiftRight(extra);
        }
        BigInteger w = s.modInverse(n);
        BigInteger u1 = e.multiply(w).mod(n);
        BigInteger u2 = r.multiply(w).mod(n);
        BigInteger[] point = curve.multiplyTwo(u1, u2, q);
        return point != null && point[0].mod(n).equals(r);
    }
}
