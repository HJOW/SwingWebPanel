package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.Rfc8448;

/**
 * Key derivation of the TLS module (plan item P32-03-a): HMAC, HKDF with the vectors of RFC 5869 appendix A, the TLS 1.3 key schedule
 * with the intermediate values of the simple 1-RTT trace of RFC 8448 section 3, and the TLS 1.2 PRF.
 * <p>
 * TLS 모듈의 키 유도(계획 P32-03-a): HMAC, RFC 5869 부록 A 벡터로 본 HKDF, RFC 8448 3절의 단순 1-RTT 추적 중간값으로 본 TLS 1.3 키 스케줄,
 * TLS 1.2 PRF.
 */
public class KdfTest {

    /** The section of the simple 1-RTT handshake in RFC 8448. / RFC 8448 의 단순 1-RTT 핸드셰이크 절. */
    private static final String SIMPLE = "3.";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public KdfTest() {
    }

    /**
     * Converts ASCII text to bytes.
     * <p>
     * ASCII 문자열을 바이트로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] ascii(String s) {
        byte[] out = new byte[s.length()];
        // 문자 값을 그대로 옮긴다.
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) s.charAt(i);
        }
        return out;
    }

    /**
     * HMAC-SHA-256/384/512 give the values of RFC 4231 test cases 1, 2 and 6 (key longer than the block).
     * <p>
     * HMAC-SHA-256/384/512 가 RFC 4231 시험 사례 1, 2, 6(블록보다 긴 키)의 값을 낸다.
     */
    public void testHmacVectors() {
        byte[] key1 = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] data1 = ascii("Hi There");
        Check.equal("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7", Hex.encode(HashAlg.SHA256.hmac(key1, data1)), "case 1 SHA-256");
        Check.equal("afd03944d84895626b0825f4ab46907f15f9dadbe4101ec682aa034c7cebc59cfaea9ea9076ede7f4af152e8b2fa9cb6",
                Hex.encode(HashAlg.SHA384.hmac(key1, data1)), "case 1 SHA-384");
        Check.equal("87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cdedaa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854",
                Hex.encode(HashAlg.SHA512.hmac(key1, data1)), "case 1 SHA-512");
        Check.equal("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                Hex.encode(HashAlg.SHA256.hmac(ascii("Jefe"), ascii("what do ya want "), ascii("for nothing?"))), "case 2 SHA-256 in two parts");
        byte[] key6 = new byte[131];
        java.util.Arrays.fill(key6, (byte) 0xaa);
        Check.equal("60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
                Hex.encode(HashAlg.SHA256.hmac(key6, ascii("Test Using Larger Than Block-Size Key - Hash Key First"))), "case 6 SHA-256");
        Check.equal(32, HashAlg.SHA256.hmac(new byte[0], data1).length, "an empty key is allowed");
    }

    /**
     * HKDF extract and expand give the values of RFC 5869 test cases 1 to 3.
     * <p>
     * HKDF extract 와 expand 가 RFC 5869 시험 사례 1~3 의 값을 낸다.
     */
    public void testHkdfRfc5869() {
        byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
        byte[] prk = Hkdf.extract(HashAlg.SHA256, Hex.decode("000102030405060708090a0b0c"), ikm);
        Check.equal("077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5", Hex.encode(prk), "case 1 PRK");
        Check.equal("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
                Hex.encode(Hkdf.expand(HashAlg.SHA256, prk, Hex.decode("f0f1f2f3f4f5f6f7f8f9"), 42)), "case 1 OKM");

        byte[] ikm2 = new byte[80];
        byte[] salt2 = new byte[80];
        byte[] info2 = new byte[80];
        // 사례 2 의 입력은 연속한 바이트 값이다.
        for (int i = 0; i < 80; i++) {
            ikm2[i] = (byte) i;
            salt2[i] = (byte) (0x60 + i);
            info2[i] = (byte) (0xb0 + i);
        }
        byte[] prk2 = Hkdf.extract(HashAlg.SHA256, salt2, ikm2);
        Check.equal("06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244", Hex.encode(prk2), "case 2 PRK");
        Check.equal("b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271cb41c65e590e09"
                + "da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f1d87",
                Hex.encode(Hkdf.expand(HashAlg.SHA256, prk2, info2, 82)), "case 2 OKM");

        byte[] prk3 = Hkdf.extract(HashAlg.SHA256, new byte[0], ikm);
        Check.equal("19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04", Hex.encode(prk3), "case 3 PRK (empty salt)");
        Check.equal(Hex.encode(prk3), Hex.encode(Hkdf.extract(HashAlg.SHA256, null, ikm)), "a null salt equals an empty salt");
        Check.equal("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8",
                Hex.encode(Hkdf.expand(HashAlg.SHA256, prk3, new byte[0], 42)), "case 3 OKM");
        Check.equal(0, Hkdf.expand(HashAlg.SHA256, prk3, new byte[0], 0).length, "zero length output");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                Hkdf.expand(HashAlg.SHA256, new byte[32], new byte[0], 255 * 32 + 1);
            }
        }, "more than 255 blocks");
    }

    /**
     * The label functions build the <code>HkdfLabel</code> structure of the trace: the expansion of the PRK with the info octets of the
     * trace equals <code>Derive-Secret</code> with the label and the hash.
     * <p>
     * 레이블 함수는 추적의 <code>HkdfLabel</code> 구조를 만든다: 추적의 info 옥텟으로 PRK 를 확장한 값이 레이블과 해시로 부른
     * <code>Derive-Secret</code> 과 같다.
     */
    public void testExpandLabelMatchesTheTraceInfo() {
        Rfc8448.Step s = Rfc8448.load().step(SIMPLE, "{server}  derive secret \"tls13 c hs traffic\"", 0);
        byte[] viaInfo = Hkdf.expand(HashAlg.SHA256, s.get("PRK"), s.get("info"), 32);
        Check.equal(Hex.encode(s.get("expanded")), Hex.encode(viaInfo), "the info octets of the trace give the expanded value");
        Check.equal(Hex.encode(s.get("expanded")), Hex.encode(Hkdf.deriveSecret(HashAlg.SHA256, s.get("PRK"), "c hs traffic", s.get("hash"))),
                "Derive-Secret builds the same info");
    }

    /**
     * The key schedule reproduces the secrets, keys and the server Finished of the simple 1-RTT trace.
     * <p>
     * 키 스케줄이 단순 1-RTT 추적의 비밀, 키, 서버 Finished 를 재현한다.
     */
    public void testKeyScheduleRfc8448() {
        Rfc8448 rfc = Rfc8448.load();
        HashAlg h = HashAlg.SHA256;
        Check.equal(Hex.encode(rfc.step(SIMPLE, "{server}  extract secret \"early\"", 0).get("secret")),
                Hex.encode(Hkdf.extract(h, null, new byte[32])), "early secret");

        KeySchedule13 ks = new KeySchedule13(h);
        ks.addSharedSecret(rfc.step(SIMPLE, "{server}  extract secret \"handshake\"", 0).get("IKM"));
        Rfc8448.Step chs = rfc.step(SIMPLE, "{server}  derive secret \"tls13 c hs traffic\"", 0);
        Rfc8448.Step shs = rfc.step(SIMPLE, "{server}  derive secret \"tls13 s hs traffic\"", 0);
        byte[] clientHs = ks.clientHandshakeTraffic(chs.get("hash"));
        byte[] serverHs = ks.serverHandshakeTraffic(shs.get("hash"));
        Check.equal(Hex.encode(chs.get("expanded")), Hex.encode(clientHs), "client handshake traffic secret");
        Check.equal(Hex.encode(shs.get("expanded")), Hex.encode(serverHs), "server handshake traffic secret");

        Rfc8448.Step keys = rfc.step(SIMPLE, "{server}  derive write traffic keys for handshake data", 0);
        Check.equal(Hex.encode(keys.get("key expanded")), Hex.encode(KeySchedule13.trafficKey(h, serverHs, 16)), "server handshake key");
        Check.equal(Hex.encode(keys.get("iv expanded")), Hex.encode(KeySchedule13.trafficIv(h, serverHs)), "server handshake iv");

        // 서버 Finished 의 전사 해시는 ClientHello 부터 CertificateVerify 까지이다.
        byte[] transcript = h.digest(rfc.step(SIMPLE, "{client}  construct a ClientHello handshake message", 0).get("ClientHello"),
                rfc.step(SIMPLE, "{server}  construct a ServerHello handshake message", 0).get("ServerHello"),
                rfc.step(SIMPLE, "{server}  construct an EncryptedExtensions handshake message", 0).get("EncryptedExtensions"),
                rfc.step(SIMPLE, "{server}  construct a Certificate handshake message", 0).get("Certificate"),
                rfc.step(SIMPLE, "{server}  construct a CertificateVerify handshake message", 0).get("CertificateVerify"));
        Rfc8448.Step fin = rfc.step(SIMPLE, "{server}  calculate finished \"tls13 finished\"", 0);
        Check.equal(Hex.encode(fin.get("finished")), Hex.encode(KeySchedule13.finishedMac(h, serverHs, transcript)), "server Finished verify data");

        ks.toMaster();
        Rfc8448.Step cap = rfc.step(SIMPLE, "{server}  derive secret \"tls13 c ap traffic\"", 0);
        Rfc8448.Step sap = rfc.step(SIMPLE, "{server}  derive secret \"tls13 s ap traffic\"", 0);
        byte[] clientAp = ks.clientApplicationTraffic(cap.get("hash"));
        Check.equal(Hex.encode(cap.get("expanded")), Hex.encode(clientAp), "client application traffic secret");
        Check.equal(Hex.encode(sap.get("expanded")), Hex.encode(ks.serverApplicationTraffic(sap.get("hash"))), "server application traffic secret");
        Rfc8448.Step apKeys = rfc.step(SIMPLE, "{client}  derive write traffic keys for application data", 0);
        Check.equal(Hex.encode(apKeys.get("PRK")), Hex.encode(clientAp), "the client application keys come from the client secret");
        Check.equal(Hex.encode(apKeys.get("key expanded")), Hex.encode(KeySchedule13.trafficKey(h, clientAp, 16)), "client application key");
        Check.equal(Hex.encode(apKeys.get("iv expanded")), Hex.encode(KeySchedule13.trafficIv(h, clientAp)), "client application iv");
    }

    /**
     * The stages of the key schedule must be used in order.
     * <p>
     * 키 스케줄의 단계는 순서대로 써야 한다.
     */
    public void testKeyScheduleStages() {
        final KeySchedule13 ks = new KeySchedule13(HashAlg.SHA384);
        Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                ks.clientHandshakeTraffic(new byte[48]);
            }
        }, "handshake traffic before the shared secret");
        ks.addSharedSecret(new byte[32]);
        Check.equal(48, ks.serverHandshakeTraffic(new byte[48]).length, "SHA-384 secrets have 48 bytes");
        Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                ks.serverApplicationTraffic(new byte[48]);
            }
        }, "application traffic before the master secret");
        ks.toMaster();
        Check.equal(48, KeySchedule13.nextTrafficSecret(HashAlg.SHA384, ks.clientApplicationTraffic(new byte[48])).length, "key update secret");
        ks.destroy();
        Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                ks.clientApplicationTraffic(new byte[48]);
            }
        }, "use after destroy");
    }

    /**
     * The TLS 1.2 PRF with SHA-256 gives the widely used public test vector (posted to the IETF TLS list as "TLS 1.2 PRF test vectors"),
     * and the output is a prefix-consistent stream: a shorter request is the beginning of a longer one.
     * <p>
     * SHA-256 의 TLS 1.2 PRF 가 널리 쓰이는 공개 시험 벡터(IETF TLS 목록에 "TLS 1.2 PRF test vectors" 로 올라온 것)를 내고, 출력은 접두사가
     * 일치하는 스트림이다: 짧게 요청한 것은 길게 요청한 것의 앞부분이다.
     */
    public void testPrf12() {
        byte[] secret = Hex.decode("9bbe436ba940f017b17652849a71db35");
        byte[] seed = Hex.decode("a0ba9f936cda311827a6f796ffd5198c");
        byte[] out = Prf12.prf(HashAlg.SHA256, secret, "test label", seed, 100);
        Check.equal("e3f229ba727be17b8d122620557cd453c2aab21d07c3d495329b52d4e61edb5a6b301791e90d35c9c9a46b4e14baf9af0fa0"
                + "22f7077def17abfd3797c0564bab4fbc91666e9def9b97fce34f796789baa48082d122ee42c5a72e5a5110fff70187347b66",
                Hex.encode(out), "PRF SHA-256 vector");
        byte[] shorter = Prf12.prf(HashAlg.SHA256, secret, "test label", seed, 33);
        Check.equal(Hex.encode(Bytes.copy(out, 0, 33)), Hex.encode(shorter), "a shorter output is a prefix");
        Check.equal(148, Prf12.prf(HashAlg.SHA384, secret, "test label", seed, 148).length, "SHA-384 output length");
    }
}
