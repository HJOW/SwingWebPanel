package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;

/**
 * RSA signature verification of the TLS module (plan item P32-03-f): signatures made by the running JRE (<code>SHA256withRSA</code> and
 * so on) verify, and forgeries built with the private key but with a wrong encoding are rejected: DigestInfo without the NULL
 * parameters, extra data after the digest (the low exponent forgery pattern), wrong block type, short padding, values out of range and
 * keys too short for the encoding. PSS encodings with a wrong trailer, non-zero padding or non-zero top bits are rejected as well.
 * <p>
 * TLS 모듈의 RSA 서명 검증(계획 P32-03-f): 실행 중인 JRE 가 만든 서명(<code>SHA256withRSA</code> 등)이 검증되고, 개인 키로 만들었지만 부호화가 틀린
 * 위조는 거부한다: NULL 매개변수가 없는 DigestInfo, 다이제스트 뒤의 여분 데이터(낮은 지수 위조 형태), 틀린 블록 형식, 짧은 패딩, 범위 밖의 값,
 * 부호화에 비해 너무 짧은 키. 트레일러가 틀리거나 패딩이 0 이 아니거나 최상위 비트가 0 이 아닌 PSS 부호화도 거부한다.
 */
public class RsaVerifyTest {

    /** A 2048 bit key pair shared by the cases (generation takes a moment). / 사례들이 함께 쓰는 2048비트 키 쌍(생성에 시간이 걸린다). */
    private static KeyPair pair;

    /** The message of the cases. / 사례의 메시지. */
    private static final byte[] MESSAGE = { 'T', 'L', 'S', ' ', 'm', 'o', 'd', 'u', 'l', 'e' };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RsaVerifyTest() {
    }

    /**
     * Returns the shared key pair, generating it on first use.
     * <p>
     * 공유 키 쌍을 반환한다. 처음 쓸 때 만든다.
     *
     * @return the key pair / 키 쌍
     * @throws Exception if the JRE cannot generate RSA keys / JRE 가 RSA 키를 만들 수 없으면
     */
    private static synchronized KeyPair pair() throws Exception {
        if (pair == null) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            pair = gen.generateKeyPair();
        }
        return pair;
    }

    /**
     * Returns the modulus of the shared key.
     * <p>
     * 공유 키의 법을 반환한다.
     *
     * @return the modulus / 법
     * @throws Exception on a key generation failure / 키 생성 실패 시
     */
    private static BigInteger n() throws Exception {
        return ((RSAPublicKey) pair().getPublic()).getModulus();
    }

    /**
     * Returns the public exponent of the shared key.
     * <p>
     * 공유 키의 공개 지수를 반환한다.
     *
     * @return the exponent / 지수
     * @throws Exception on a key generation failure / 키 생성 실패 시
     */
    private static BigInteger e() throws Exception {
        return ((RSAPublicKey) pair().getPublic()).getPublicExponent();
    }

    /**
     * Signs an encoded message with the raw private key operation, so that any encoding can be turned into a signature.
     * <p>
     * 부호화된 메시지에 개인 키 원시 연산으로 서명한다. 어떤 부호화든 서명으로 만들 수 있다.
     *
     * @param em the encoded message of the modulus length / 법 길이의 부호화된 메시지
     * @return the signature / 서명
     * @throws Exception on a key generation failure / 키 생성 실패 시
     */
    private static byte[] rawSign(byte[] em) throws Exception {
        BigInteger d = ((RSAPrivateKey) pair().getPrivate()).getPrivateExponent();
        return EcCurve.toFixed(new BigInteger(1, em).modPow(d, n()), 256);
    }

    /**
     * Signatures of the JRE with SHA-256, SHA-384 and SHA-512 verify; other messages, hashes and keys do not.
     * <p>
     * JRE 의 SHA-256, SHA-384, SHA-512 서명이 검증되고, 다른 메시지·해시·키는 검증되지 않는다.
     *
     * @throws Exception on a provider failure / 공급자 오류 시
     */
    public void testJreSignaturesVerify() throws Exception {
        Object[][] algs = { { "SHA256withRSA", HashAlg.SHA256 }, { "SHA384withRSA", HashAlg.SHA384 }, { "SHA512withRSA", HashAlg.SHA512 } };
        // 해시마다 JRE 로 서명하고 모듈로 검증한다.
        for (int i = 0; i < algs.length; i++) {
            Signature s = Signature.getInstance((String) algs[i][0]);
            s.initSign(pair().getPrivate());
            s.update(MESSAGE);
            byte[] sig = s.sign();
            HashAlg h = (HashAlg) algs[i][1];
            Check.isTrue(RsaVerify.pkcs1(n(), e(), h, MESSAGE, sig), algs[i][0] + ": valid");
            Check.isFalse(RsaVerify.pkcs1(n(), e(), h, new byte[] { 'x' }, sig), algs[i][0] + ": another message");
            Check.isFalse(RsaVerify.pkcs1(n().add(BigInteger.valueOf(2)), e(), h, MESSAGE, sig), algs[i][0] + ": another modulus");
            Check.isFalse(RsaVerify.pkcs1(n(), BigInteger.valueOf(3), h, MESSAGE, sig), algs[i][0] + ": another exponent");
        }
    }

    /**
     * Builds a PKCS #1 v1.5 encoding <code>00 01 FF..FF 00 T</code> of 256 bytes.
     * <p>
     * 256바이트의 PKCS #1 v1.5 부호화 <code>00 01 FF..FF 00 T</code> 를 만든다.
     *
     * @param type the block type byte (1 is the valid one) / 블록 형식 바이트(1 이 올바르다)
     * @param t    the bytes after the zero separator / 0 구분자 뒤의 바이트
     * @param tail extra bytes after t (taken from the padding) / t 뒤의 여분 바이트(패딩에서 덜어 낸다)
     * @return the encoding / 부호화
     */
    private static byte[] encode(int type, byte[] t, byte[] tail) {
        byte[] em = new byte[256];
        em[1] = (byte) type;
        int end = 256 - t.length - tail.length - 1;
        java.util.Arrays.fill(em, 2, end, (byte) 0xff);
        System.arraycopy(t, 0, em, end + 1, t.length);
        System.arraycopy(tail, 0, em, end + 1 + t.length, tail.length);
        return em;
    }

    /**
     * Only the one correct PKCS #1 v1.5 encoding is accepted.
     * <p>
     * 올바른 PKCS #1 v1.5 부호화 하나만 받아들인다.
     *
     * @throws Exception on a key generation failure / 키 생성 실패 시
     */
    public void testPkcs1EncodingIsStrict() throws Exception {
        byte[] digest = HashAlg.SHA256.digest(MESSAGE);
        byte[] prefix = Hex.decode("3031300d060960864801650304020105000420");
        byte[] good = Bytes.concat(prefix, digest);
        Check.isTrue(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(1, good, new byte[0]))), "the correct encoding");

        byte[] noNull = Bytes.concat(Hex.decode("302f300b0609608648016503040201" + "0420"), digest);
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(1, noNull, new byte[0]))), "DigestInfo without NULL parameters");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(1, good, new byte[40]))), "garbage after the digest");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(2, good, new byte[0]))), "block type 2");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(1, digest, new byte[0]))), "a bare digest without DigestInfo");
        byte[] zeroInPadding = encode(1, good, new byte[0]);
        zeroInPadding[10] = 0;
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(zeroInPadding)), "a zero byte inside the padding");
        byte[] firstNotZero = encode(1, good, new byte[0]);
        firstNotZero[0] = 1;
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, EcCurve.toFixed(new BigInteger(1, firstNotZero).mod(n())
                .modPow(((RSAPrivateKey) pair().getPrivate()).getPrivateExponent(), n()), 256)), "first byte not zero");
        byte[] sha384Prefix = Bytes.concat(Hex.decode("3041300d060960864801650304020205000430"), HashAlg.SHA384.digest(MESSAGE));
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(encode(1, sha384Prefix, new byte[0]))), "the DigestInfo of another hash");
        Check.isTrue(RsaVerify.pkcs1(n(), e(), HashAlg.SHA384, MESSAGE, rawSign(encode(1, sha384Prefix, new byte[0]))), "SHA-384 encoding with SHA-384");
    }

    /**
     * Signatures of the wrong length or not below the modulus, and unusable keys, are rejected.
     * <p>
     * 길이가 틀리거나 법보다 작지 않은 서명, 쓸 수 없는 키를 거부한다.
     *
     * @throws Exception on a provider failure / 공급자 오류 시
     */
    public void testRangeChecks() throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(pair().getPrivate());
        s.update(MESSAGE);
        byte[] sig = s.sign();
        Check.isTrue(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, sig), "the signature is valid");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, Bytes.concat(new byte[1], sig)), "one leading zero byte more");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, Bytes.copy(sig, 1, sig.length - 1)), "one byte shorter");
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, new byte[0]), "an empty signature");
        byte[] plusN = EcCurve.toFixed(new BigInteger(1, sig).add(n()), 256);
        if (new BigInteger(1, sig).add(n()).bitLength() <= 2048) {
            Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, plusN), "signature + n (same residue, out of range)");
        }
        byte[] all = new byte[256];
        java.util.Arrays.fill(all, (byte) 0xff);
        Check.isFalse(RsaVerify.pkcs1(n(), e(), HashAlg.SHA256, MESSAGE, all), "a value above the modulus");
        Check.isFalse(RsaVerify.pkcs1(n(), BigInteger.ONE, HashAlg.SHA256, MESSAGE, sig), "exponent 1");
        Check.isFalse(RsaVerify.pkcs1(n(), BigInteger.valueOf(65536), HashAlg.SHA256, MESSAGE, sig), "an even exponent");
        Check.isFalse(RsaVerify.pkcs1(n().shiftLeft(1), e(), HashAlg.SHA256, MESSAGE, Bytes.concat(new byte[1], sig)), "an even modulus");
        Check.isFalse(RsaVerify.pkcs1(n().negate(), e(), HashAlg.SHA256, MESSAGE, sig), "a negative modulus");

        // 부호화에 비해 너무 짧은 키: 512비트 법에는 SHA-512 의 DigestInfo 와 패딩이 들어가지 않는다.
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(512);
        KeyPair small = gen.generateKeyPair();
        RSAPublicKey sp = (RSAPublicKey) small.getPublic();
        byte[] any = new byte[64];
        any[63] = 2;
        Check.isFalse(RsaVerify.pkcs1(sp.getModulus(), sp.getPublicExponent(), HashAlg.SHA512, MESSAGE, any), "512 bit key with SHA-512");
        Check.isFalse(RsaVerify.pss(sp.getModulus(), sp.getPublicExponent(), HashAlg.SHA512, MESSAGE, any, 64), "512 bit key with PSS SHA-512");
    }

    /**
     * Builds an RSA-PSS encoding by the rules of RFC 8017 section 9.1.1 with a fixed salt, with optional defects.
     * <p>
     * RFC 8017 9.1.1절의 규칙대로 고정 salt 의 RSA-PSS 부호화를 만든다. 결함을 넣을 수 있다.
     *
     * @param trailer    the last byte (0xbc is the valid one) / 마지막 바이트(0xbc 가 올바르다)
     * @param padByte    the value of the first padding byte (0 is the valid one) / 첫 패딩 바이트의 값(0 이 올바르다)
     * @param separator  the byte before the salt (1 is the valid one) / salt 앞의 바이트(1 이 올바르다)
     * @param keepTopBit true to leave the top bit of the masked DB set / 가린 DB 의 최상위 비트를 켠 채로 두려면 true
     * @return the encoding of 256 bytes / 256바이트 부호화
     */
    private static byte[] pssEncode(int trailer, int padByte, int separator, boolean keepTopBit) {
        HashAlg hash = HashAlg.SHA256;
        byte[] salt = new byte[32];
        java.util.Arrays.fill(salt, (byte) 0x5a);
        byte[] h = hash.digest(new byte[8], hash.digest(MESSAGE), salt);
        int emLen = 256;
        int dbLen = emLen - 32 - 1;
        byte[] db = new byte[dbLen];
        db[0] = (byte) padByte;
        db[dbLen - 33] = (byte) separator;
        System.arraycopy(salt, 0, db, dbLen - 32, 32);
        int at = 0;
        // MGF1 마스크로 DB 를 가린다.
        for (int counter = 0; at < dbLen; counter++) {
            byte[] block = hash.digest(h, new byte[] { 0, 0, 0, (byte) counter });
            // 마스크 블록을 더한다.
            for (int i = 0; i < block.length && at < dbLen; i++, at++) {
                db[at] ^= block[i];
            }
        }
        if (keepTopBit) {
            db[0] |= (byte) 0x80;
        } else {
            db[0] &= 0x7f;
        }
        byte[] em = new byte[emLen];
        System.arraycopy(db, 0, em, 0, dbLen);
        System.arraycopy(h, 0, em, dbLen, 32);
        em[emLen - 1] = (byte) trailer;
        return em;
    }

    /**
     * Only correct PSS encodings are accepted: trailer, zero padding, separator and top bits are all checked.
     * <p>
     * 올바른 PSS 부호화만 받아들인다: 트레일러, 0 패딩, 구분자, 최상위 비트를 모두 검사한다.
     *
     * @throws Exception on a key generation failure / 키 생성 실패 시
     */
    public void testPssEncodingIsStrict() throws Exception {
        Check.isTrue(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 0, 1, false)), 32), "the correct encoding");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xcc, 0, 1, false)), 32), "wrong trailer");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 1, 1, false)), 32), "padding not zero");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 0, 2, false)), 32), "separator not 01");
        byte[] top = pssEncode(0xbc, 0, 1, true);
        if (new BigInteger(1, top).compareTo(n()) < 0) {
            Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(top), 32), "top bit of the encoding set");
        }
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 0, 1, false)), 31), "salt length 31");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 0, 1, false)), -1), "negative salt length");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, MESSAGE, rawSign(pssEncode(0xbc, 0, 1, false)), 300), "salt longer than the encoding");
        Check.isFalse(RsaVerify.pss(n(), e(), HashAlg.SHA256, new byte[] { 'x' }, rawSign(pssEncode(0xbc, 0, 1, false)), 32), "another message");
    }
}
