package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import javax.net.ssl.SSLException;

/**
 * The X25519 key agreement of RFC 7748, the default key exchange group of this module. Field elements modulo 2^255 - 19 are sixteen
 * 16 bit limbs in <code>long</code> values; the scalar multiplication is a Montgomery ladder whose steps are the same for every scalar,
 * with a conditional swap by masks instead of branches. Usage:
 * <pre>
 * byte[] priv = new byte[32];
 * random.nextBytes(priv);
 * byte[] pub = X25519.publicKey(priv);                 // sent in the key share
 * byte[] secret = X25519.sharedSecret(priv, peerPub);  // throws if the result is all zero
 * </pre>
 * <p>
 * RFC 7748 의 X25519 키 합의. 이 모듈의 기본 키 교환 그룹이다. 2^255 - 19 를 법으로 하는 체의 원소는 <code>long</code> 값 안의 16비트 림 열여섯 개이고,
 * 스칼라 곱은 모든 스칼라에 대해 같은 단계를 밟는 몽고메리 사다리이며 조건부 교환은 분기 대신 마스크로 한다.
 */
public final class X25519 {

    /** The length of private keys, public keys and shared secrets in bytes. / 개인 키, 공개 키, 공유 비밀의 길이(바이트). */
    public static final int LENGTH = 32;

    /** The constant a24 = (486662 - 2) / 4 = 121665 of the ladder step, as limbs. / 사다리 단계의 상수 a24 = (486662 - 2) / 4 = 121665 (림). */
    private static final long[] A24 = { 0xDB41, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private X25519() {
    }

    /**
     * Brings the limbs back to 16 bits by propagating carries; the carry out of the last limb is multiplied by 38 (2^256 = 38).
     * <p>
     * 올림을 전파해 림을 16비트로 되돌린다. 마지막 림에서 넘친 올림은 38 을 곱해 더한다(2^256 = 38).
     *
     * @param o the element / 원소
     */
    private static void carry(long[] o) {
        // 림마다 올림을 다음 림으로 넘긴다(위치에 따른 분기는 공개 값이다).
        for (int i = 0; i < 16; i++) {
            o[i] += 1L << 16;
            long c = o[i] >> 16;
            if (i < 15) {
                o[i + 1] += c - 1;
            } else {
                o[0] += 38 * (c - 1);
            }
            o[i] -= c << 16;
        }
    }

    /**
     * Swaps two elements if the bit is one, without a branch.
     * <p>
     * 비트가 1 이면 두 원소를 분기 없이 맞바꾼다.
     *
     * @param p   first element / 첫 원소
     * @param q   second element / 둘째 원소
     * @param bit 0 or 1 / 0 또는 1
     */
    private static void swap(long[] p, long[] q, long bit) {
        long mask = -bit;
        // 림마다 마스크로 맞바꾼다.
        for (int i = 0; i < 16; i++) {
            long t = mask & (p[i] ^ q[i]);
            p[i] ^= t;
            q[i] ^= t;
        }
    }

    /**
     * Adds two elements limb by limb.
     * <p>
     * 두 원소를 림 단위로 더한다.
     *
     * @param o the result / 결과
     * @param a first operand / 첫 피연산자
     * @param b second operand / 둘째 피연산자
     */
    private static void add(long[] o, long[] a, long[] b) {
        // 림마다 더한다.
        for (int i = 0; i < 16; i++) {
            o[i] = a[i] + b[i];
        }
    }

    /**
     * Subtracts two elements limb by limb.
     * <p>
     * 두 원소를 림 단위로 뺀다.
     *
     * @param o the result / 결과
     * @param a first operand / 첫 피연산자
     * @param b second operand / 둘째 피연산자
     */
    private static void sub(long[] o, long[] a, long[] b) {
        // 림마다 뺀다.
        for (int i = 0; i < 16; i++) {
            o[i] = a[i] - b[i];
        }
    }

    /**
     * Multiplies two elements. The result may be one of the operands.
     * <p>
     * 두 원소를 곱한다. 결과가 피연산자 중 하나여도 된다.
     *
     * @param o the result / 결과
     * @param a first operand / 첫 피연산자
     * @param b second operand / 둘째 피연산자
     */
    private static void mul(long[] o, long[] a, long[] b) {
        long[] t = new long[31];
        // 교과서 곱셈으로 31개 림을 만든다.
        for (int i = 0; i < 16; i++) {
            // a 의 림 하나에 b 의 모든 림을 곱해 더한다.
            for (int j = 0; j < 16; j++) {
                t[i + j] += a[i] * b[j];
            }
        }
        // 위쪽 림을 38 배 해 아래로 접는다(2^256 = 38).
        for (int i = 0; i < 15; i++) {
            t[i] += 38 * t[i + 16];
        }
        System.arraycopy(t, 0, o, 0, 16);
        carry(o);
        carry(o);
        java.util.Arrays.fill(t, 0L);
    }

    /**
     * Computes the inverse by the power p - 2 (Fermat). The exponent is a public constant.
     * <p>
     * p - 2 제곱(페르마)으로 역원을 구한다. 지수는 공개 상수이다.
     *
     * @param o the result / 결과
     * @param in the element / 원소
     */
    private static void invert(long[] o, long[] in) {
        long[] c = in.clone();
        // 지수 2^255 - 21 의 비트를 위에서부터 본다: 비트 2 와 4 만 0 이다.
        for (int a = 253; a >= 0; a--) {
            mul(c, c, c);
            if (a != 2 && a != 4) {
                mul(c, c, in);
            }
        }
        System.arraycopy(c, 0, o, 0, 16);
        java.util.Arrays.fill(c, 0L);
    }

    /**
     * Reduces an element completely and writes it as 32 little endian bytes.
     * <p>
     * 원소를 완전히 줄여 리틀 엔디언 32바이트로 쓴다.
     *
     * @param n the element / 원소
     * @return the bytes / 바이트
     */
    private static byte[] pack(long[] n) {
        long[] t = n.clone();
        long[] m = new long[16];
        carry(t);
        carry(t);
        carry(t);
        // p 를 두 번까지 빼 본다. 빌림이 없을 때만 뺀 값을 마스크로 고른다.
        for (int j = 0; j < 2; j++) {
            m[0] = t[0] - 0xffed;
            // 가운데 림에서 0xffff 와 앞 림의 빌림을 뺀다.
            for (int i = 1; i < 15; i++) {
                m[i] = t[i] - 0xffff - ((m[i - 1] >> 16) & 1);
                m[i - 1] &= 0xffff;
            }
            m[15] = t[15] - 0x7fff - ((m[14] >> 16) & 1);
            long b = (m[15] >> 16) & 1;
            m[14] &= 0xffff;
            swap(t, m, 1 - b);
        }
        byte[] out = new byte[32];
        // 림마다 두 바이트를 쓴다.
        for (int i = 0; i < 16; i++) {
            out[2 * i] = (byte) t[i];
            out[2 * i + 1] = (byte) (t[i] >> 8);
        }
        java.util.Arrays.fill(t, 0L);
        java.util.Arrays.fill(m, 0L);
        return out;
    }

    /**
     * The function X25519(k, u) of RFC 7748 section 5.
     * <p>
     * RFC 7748 5절의 함수 X25519(k, u).
     *
     * @param k the 32 byte scalar (clamped here) / 32바이트 스칼라(여기서 클램프한다)
     * @param u the 32 byte u-coordinate; the top bit is ignored / 32바이트 u 좌표. 최상위 비트는 무시한다
     * @return the 32 byte result / 32바이트 결과
     */
    private static byte[] scalarMult(byte[] k, byte[] u) {
        if (k.length != LENGTH || u.length != LENGTH) {
            throw new IllegalArgumentException("X25519 values have 32 bytes");
        }
        byte[] z = k.clone();
        z[31] = (byte) ((z[31] & 127) | 64);
        z[0] &= (byte) 248;
        long[] x = new long[16];
        // u 좌표를 림으로 읽는다(최상위 비트는 버린다).
        for (int i = 0; i < 16; i++) {
            x[i] = (u[2 * i] & 0xffL) | ((u[2 * i + 1] & 0xffL) << 8);
        }
        x[15] &= 0x7fff;
        long[] a = new long[16];
        long[] b = x.clone();
        long[] c = new long[16];
        long[] d = new long[16];
        long[] e = new long[16];
        long[] f = new long[16];
        a[0] = 1;
        d[0] = 1;
        // 몽고메리 사다리: 스칼라의 비트 254부터 0까지 같은 연산을 한다.
        for (int i = 254; i >= 0; i--) {
            long r = (z[i >>> 3] >>> (i & 7)) & 1;
            swap(a, b, r);
            swap(c, d, r);
            add(e, a, c);
            sub(a, a, c);
            add(c, b, d);
            sub(b, b, d);
            mul(d, e, e);
            mul(f, a, a);
            mul(a, c, a);
            mul(c, b, e);
            add(e, a, c);
            sub(a, a, c);
            mul(b, a, a);
            sub(c, d, f);
            mul(a, c, A24);
            add(a, a, d);
            mul(c, c, a);
            mul(a, d, f);
            mul(d, b, x);
            mul(b, e, e);
            swap(a, b, r);
            swap(c, d, r);
        }
        invert(c, c);
        mul(a, a, c);
        byte[] out = pack(a);
        Bytes.wipe(z);
        java.util.Arrays.fill(a, 0L);
        java.util.Arrays.fill(b, 0L);
        java.util.Arrays.fill(c, 0L);
        java.util.Arrays.fill(d, 0L);
        java.util.Arrays.fill(e, 0L);
        java.util.Arrays.fill(f, 0L);
        return out;
    }

    /**
     * Computes the public key of a private key: X25519(k, 9).
     * <p>
     * 개인 키의 공개 키를 계산한다: X25519(k, 9).
     *
     * @param privateKey 32 random bytes / 난수 32바이트
     * @return the 32 byte public key / 32바이트 공개 키
     */
    public static byte[] publicKey(byte[] privateKey) {
        byte[] base = new byte[LENGTH];
        base[0] = 9;
        return scalarMult(privateKey, base);
    }

    /**
     * Computes the shared secret and rejects the all-zero result, which a peer can force with a point of small order (RFC 7748 section 6.1,
     * RFC 8446 section 7.4.2).
     * <p>
     * 공유 비밀을 계산하고 결과가 모두 0 이면 거부한다. 상대가 작은 차수의 점으로 그런 결과를 강제할 수 있다(RFC 7748 6.1절, RFC 8446 7.4.2절).
     *
     * @param privateKey the own private key / 자신의 개인 키
     * @param peerPublic the 32 byte public key of the peer / 상대의 32바이트 공개 키
     * @return the 32 byte shared secret / 32바이트 공유 비밀
     * @throws SSLException if the peer key has the wrong length or the shared secret is all zero / 상대 키의 길이가 틀리거나 공유 비밀이 모두 0 이면
     */
    public static byte[] sharedSecret(byte[] privateKey, byte[] peerPublic) throws SSLException {
        if (peerPublic.length != LENGTH) {
            throw new SSLException("X25519 public key of " + peerPublic.length + " bytes");
        }
        byte[] out = scalarMult(privateKey, peerPublic);
        int all = 0;
        // 모든 바이트를 OR 로 모아 0 인지 본다(중간에 멈추지 않는다).
        for (int i = 0; i < LENGTH; i++) {
            all |= out[i];
        }
        if (all == 0) {
            throw new SSLException("X25519 shared secret is all zero (peer key of small order)");
        }
        return out;
    }
}
