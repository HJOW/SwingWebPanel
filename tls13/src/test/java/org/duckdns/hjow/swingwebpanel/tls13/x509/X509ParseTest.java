package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.math.BigInteger;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.DerWriter;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;

/**
 * The DER reader and the certificate parser of the TLS module (plan item P32-04-a): the fields and extensions of RSA and EC
 * certificates, strict DER rules, and damaged input (truncated at every length, every byte changed, wrong lengths, very deep nesting),
 * which must end in an {@link X509Exception} and nothing else.
 * <p>
 * TLS 모듈의 DER 해석기와 인증서 해석기(계획 P32-04-a): RSA·EC 인증서의 필드와 확장, 엄격한 DER 규칙, 손상된 입력(모든 길이에서 자르기, 모든 바이트
 * 바꾸기, 틀린 길이, 매우 깊은 중첩). 손상된 입력은 {@link X509Exception} 으로만 끝나야 한다.
 */
public class X509ParseTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public X509ParseTest() {
    }

    /**
     * Parses and expects a malformed certificate.
     * <p>
     * 해석해서 잘못된 인증서임을 기대한다.
     *
     * @param der  the bytes / 바이트
     * @param what the description for the failure message / 실패 메시지용 설명
     */
    private static void expectMalformed(final byte[] der, String what) {
        X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                X509Cert.parse(der);
            }
        }, what);
        Check.equal(X509Exception.Reason.MALFORMED, e.reason(), what + ": reason");
    }

    /**
     * An RSA certificate with all supported extensions is parsed field by field.
     * <p>
     * 지원하는 확장을 모두 가진 RSA 인증서를 필드별로 해석한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRsaCertificateFields() throws Exception {
        TestKey key = TestKey.rsa("leaf");
        TestKey caKey = TestKey.rsa("ca");
        byte[] der = new CertBuilder("www.example.test", key).issuer("Test CA", caKey).dns("www.example.test", "*.example.test")
                .ip(new byte[] { 127, 0, 0, 1 }).ip(new byte[16]).keyUsage(1 | (1 << 2)).eku(X509Cert.EKU_SERVER_AUTH, "1.3.6.1.5.5.7.3.2")
                .extension("2.5.29.14", false).build();
        X509Cert c = X509Cert.parse(der);
        Check.equal(Hex.encode(der), Hex.encode(c.encoded()), "encoded");
        Check.equal(Hex.encode(DerWriter.name(null, "www.example.test")), Hex.encode(c.subject()), "subject");
        Check.equal(Hex.encode(DerWriter.name(null, "Test CA")), Hex.encode(c.issuer()), "issuer");
        Check.isTrue(c.isIssuedBy(DerWriter.name(null, "Test CA")), "issued by the CA name");
        Check.isFalse(c.isSelfIssued(), "not self-issued");
        Check.equal(CertBuilder.NOW - 30 * CertBuilder.DAY, c.notBefore(), "notBefore");
        Check.equal(CertBuilder.NOW + 335 * CertBuilder.DAY, c.notAfter(), "notAfter");
        Check.isTrue(c.key().isRsa(), "RSA key");
        Check.equal(2048, c.key().bits(), "key size");
        Check.equal(Hex.encode(key.spki()), Hex.encode(c.key().encoded()), "SubjectPublicKeyInfo");
        Check.equal("RSA", c.key().algorithmName(), "algorithm name");
        Check.equal("[www.example.test, *.example.test]", c.dnsNames().toString(), "DNS names");
        Check.equal(2, c.ipAddresses().size(), "IP addresses");
        Check.equal("7f000001", Hex.encode(c.ipAddresses().get(0)), "IPv4 address");
        Check.isFalse(c.isCa(), "not a CA");
        Check.equal(-1, c.pathLength(), "no path length");
        Check.isTrue(c.allowsKeyUsage(X509Cert.KU_DIGITAL_SIGNATURE), "digitalSignature");
        Check.isFalse(c.allowsKeyUsage(X509Cert.KU_KEY_CERT_SIGN), "not keyCertSign");
        Check.isTrue(c.allowsServerAuth(), "serverAuth");
        Check.isNull(c.nameConstraints(), "no name constraints");
        Check.isNull(c.unknownCriticalExtension(), "no unknown critical extension");
        Check.equal(SignatureAlgorithm.RSA_PKCS1, c.signatureAlgorithm().kind(), "signature algorithm");
        Check.equal("SHA256withRSA", c.signatureAlgorithm().name(), "signature algorithm name");
        Check.equal(256, c.signature().length, "signature length");
        Check.isTrue(c.signatureAlgorithm().verify(X509Cert.parse(new CertBuilder("Test CA", caKey).selfSigned().build()).key(), c.tbs(), c.signature()),
                "the signature verifies with the CA key");
    }

    /**
     * EC certificates on the three curves, CA extensions, name constraints and PSS signatures are parsed.
     * <p>
     * 세 곡선의 EC 인증서, CA 확장, 이름 제약, PSS 서명을 해석한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEcAndCaCertificates() throws Exception {
        EcCurve[] curves = { EcCurve.P256, EcCurve.P384, EcCurve.P521 };
        // 곡선마다 자체 서명 CA 를 만들어 해석한다.
        for (int i = 0; i < curves.length; i++) {
            TestKey key = TestKey.ec("ca", curves[i]);
            HashAlg hash = i == 0 ? HashAlg.SHA256 : i == 1 ? HashAlg.SHA384 : HashAlg.SHA512;
            X509Cert c = X509Cert.parse(new CertBuilder("EC Root", key).selfSigned().ca(i).signature(TestKey.ECDSA, hash).build());
            Check.isTrue(c.key().isEc(), curves[i].name() + ": EC key");
            Check.same(curves[i], c.key().curve(), curves[i].name() + ": curve");
            Check.isTrue(c.isCa(), "CA");
            Check.equal(i, c.pathLength(), "path length");
            Check.isTrue(c.allowsKeyUsage(X509Cert.KU_KEY_CERT_SIGN), "keyCertSign");
            Check.isFalse(c.allowsKeyUsage(X509Cert.KU_DIGITAL_SIGNATURE), "not digitalSignature");
            Check.isTrue(c.isSelfIssued(), "self-issued");
            Check.equal(SignatureAlgorithm.ECDSA, c.signatureAlgorithm().kind(), "ECDSA");
            Check.isTrue(c.signatureAlgorithm().verify(c.key(), c.tbs(), c.signature()), curves[i].name() + ": the self-signature verifies");
        }
        TestKey rsa = TestKey.rsa("ca");
        X509Cert pss = X509Cert.parse(new CertBuilder("PSS CA", rsa).selfSigned().ca(-1).signature(TestKey.PSS, HashAlg.SHA384).permitDns("example.test")
                .excludeDns("bad.example.test").permitIp(new byte[] { 10, 0, 0, 0, (byte) 255, 0, 0, 0 }).build());
        Check.equal(SignatureAlgorithm.RSA_PSS, pss.signatureAlgorithm().kind(), "PSS");
        Check.isTrue(pss.signatureAlgorithm().verify(pss.key(), pss.tbs(), pss.signature()), "the PSS self-signature verifies");
        Check.notNull(pss.nameConstraints(), "name constraints");
        X509Cert v1 = X509Cert.parse(new CertBuilder("Old Root", rsa).selfSigned().version1().build());
        Check.isFalse(v1.isCa(), "a version 1 certificate has no basicConstraints");
    }

    /**
     * Weak and unknown signature algorithms are parsed, but asking for the algorithm gives the reason.
     * <p>
     * 약하거나 모르는 서명 알고리즘은 해석은 되지만, 알고리즘을 요구하면 그 이유가 나온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWeakAndUnknownAlgorithms() throws Exception {
        TestKey rsa = TestKey.rsa("ca");
        final X509Cert sha1 = X509Cert.parse(new CertBuilder("SHA1", rsa).selfSigned().signatureOid("1.2.840.113549.1.1.5").build());
        X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                sha1.signatureAlgorithm();
            }
        }, "SHA-1 signature");
        Check.equal(X509Exception.Reason.WEAK_SIGNATURE, e.reason(), "SHA-1 is weak");
        final X509Cert unknown = X509Cert.parse(new CertBuilder("Unknown", rsa).selfSigned().signatureOid("1.3.101.112").build());
        e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                unknown.signatureAlgorithm();
            }
        }, "Ed25519 signature");
        Check.equal(X509Exception.Reason.UNSUPPORTED_ALGORITHM, e.reason(), "Ed25519 is not supported");
        X509Cert critical = X509Cert.parse(new CertBuilder("Critical", rsa).selfSigned().extension("1.2.3.4.5", true).extension("1.2.3.4.6", false).build());
        Check.equal("1.2.3.4.5", critical.unknownCriticalExtension(), "the unknown critical extension is reported");
        X509Cert policies = X509Cert.parse(new CertBuilder("Policies", rsa).selfSigned().extension("2.5.29.32", true).build());
        Check.isNull(policies.unknownCriticalExtension(), "critical certificatePolicies is accepted");
    }

    /**
     * Damaged certificates end in an X509Exception with the reason MALFORMED: truncated at every length, with trailing data, and with
     * every single byte changed (a changed byte may also still give a parsable certificate).
     * <p>
     * 손상된 인증서는 이유가 MALFORMED 인 X509Exception 으로 끝난다: 모든 길이에서 자른 것, 뒤에 데이터가 붙은 것, 바이트 하나씩 바꾼 것(바이트를
     * 바꿔도 해석 가능한 인증서가 될 수는 있다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedInputNeverLeaksOtherExceptions() throws Exception {
        byte[] der = new CertBuilder("www.example.test", TestKey.ec("leaf", EcCurve.P256)).issuer("Test CA", TestKey.rsa("ca")).dns("www.example.test")
                .keyUsage(1).eku(X509Cert.EKU_SERVER_AUTH).permitDns("example.test").build();
        X509Cert.parse(der);
        // 모든 길이에서 잘라 본다.
        for (int len = 0; len < der.length; len++) {
            byte[] cut = new byte[len];
            System.arraycopy(der, 0, cut, 0, len);
            expectMalformed(cut, "truncated to " + len + " bytes");
        }
        byte[] longer = new byte[der.length + 1];
        System.arraycopy(der, 0, longer, 0, der.length);
        expectMalformed(longer, "one trailing byte");
        int stillValid = 0;
        // 바이트를 하나씩 여러 값으로 바꿔 본다. X509Exception 이외의 예외가 나오면 시험이 실패한다.
        for (int i = 0; i < der.length; i++) {
            int[] changes = { 0xff, 0x01, 0x80 };
            // 세 가지 XOR 값을 적용한다.
            for (int k = 0; k < changes.length; k++) {
                byte[] changed = der.clone();
                changed[i] ^= (byte) changes[k];
                try {
                    X509Cert.parse(changed);
                    stillValid++;
                } catch (X509Exception e) {
                    Check.equal(X509Exception.Reason.MALFORMED, e.reason(), "reason for a change at " + i);
                }
            }
        }
        Check.isTrue(stillValid > 0 && stillValid < der.length * 3, "some changes keep the structure, most do not (" + stillValid + ")");
    }

    /**
     * Structural defects are rejected: wrong element counts, differing signature algorithms, bad times, repeated extensions, an
     * extension in a version 1 certificate, a negative version, and DER that is not strict.
     * <p>
     * 구조 결함을 거부한다: 틀린 원소 수, 서로 다른 서명 알고리즘, 잘못된 시간, 되풀이된 확장, 버전 1 인증서의 확장, 음수 버전, 엄격하지 않은 DER.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStructuralDefects() throws Exception {
        TestKey rsa = TestKey.rsa("ca");
        byte[] alg = DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.11"), DerWriter.nul());
        byte[] otherAlg = DerWriter.seq(DerWriter.oid("1.2.840.113549.1.1.12"), DerWriter.nul());
        byte[] name = DerWriter.name(null, "X");
        byte[] validity = DerWriter.seq(DerWriter.time(CertBuilder.NOW), DerWriter.time(CertBuilder.NOW + CertBuilder.DAY));
        byte[] version = DerWriter.tlv(0xa0, DerWriter.integer(2));
        byte[] sig = DerWriter.bits(new byte[256]);
        byte[] tbs = DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, rsa.spki());
        X509Cert.parse(DerWriter.seq(tbs, alg, sig));
        expectMalformed(DerWriter.seq(tbs, alg), "two elements");
        expectMalformed(DerWriter.seq(tbs, alg, sig, sig), "four elements");
        expectMalformed(DerWriter.seq(tbs, otherAlg, sig), "the signature algorithm fields differ");
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name), alg, sig), "no key");
        expectMalformed(DerWriter.seq(DerWriter.seq(DerWriter.tlv(0xa0, DerWriter.integer(3)), DerWriter.integer(1), alg, name, validity, name, rsa.spki()), alg, sig),
                "version 4");
        expectMalformed(DerWriter.seq(DerWriter.seq(DerWriter.tlv(0xa0, DerWriter.integer(-1)), DerWriter.integer(1), alg, name, validity, name, rsa.spki()), alg, sig),
                "negative version");
        byte[] badTime = DerWriter.seq(DerWriter.ascii(0x17, "2611280000Z"), DerWriter.time(CertBuilder.NOW));
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, badTime, name, rsa.spki()), alg, sig), "time without seconds");
        byte[] offsetTime = DerWriter.seq(DerWriter.ascii(0x17, "261128000000+0900"), DerWriter.time(CertBuilder.NOW));
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, offsetTime, name, rsa.spki()), alg, sig), "time with an offset");
        byte[] ext = DerWriter.seq(DerWriter.oid("2.5.29.19"), DerWriter.octets(DerWriter.seq()));
        byte[] twice = DerWriter.tlv(0xa3, DerWriter.seq(ext, ext));
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, rsa.spki(), twice), alg, sig), "an extension twice");
        byte[] once = DerWriter.tlv(0xa3, DerWriter.seq(ext));
        X509Cert.parse(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, rsa.spki(), once), alg, sig));
        expectMalformed(DerWriter.seq(DerWriter.seq(DerWriter.integer(1), alg, name, validity, name, rsa.spki(), once), alg, sig), "extensions in version 1");
        byte[] emptyValue = DerWriter.tlv(0xa3, DerWriter.seq(DerWriter.seq(DerWriter.oid("2.5.29.19"), DerWriter.octets(new byte[0]))));
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, rsa.spki(), emptyValue), alg, sig), "empty extension value");
        byte[] offCurve = DerWriter.seq(DerWriter.seq(DerWriter.oid("1.2.840.10045.2.1"), DerWriter.oid("1.2.840.10045.3.1.7")), DerWriter.bits(new byte[65]));
        expectMalformed(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, offCurve), alg, sig), "EC point not on the curve");
        byte[] unknownKey = DerWriter.seq(DerWriter.seq(DerWriter.oid("1.3.101.112")), DerWriter.bits(new byte[32]));
        X509Cert ed = X509Cert.parse(DerWriter.seq(DerWriter.seq(version, DerWriter.integer(1), alg, name, validity, name, unknownKey), alg, sig));
        Check.isFalse(ed.key().isRsa() || ed.key().isEc(), "an Ed25519 key is kept as unsupported");
        Check.equal(0, ed.key().bits(), "an unsupported key has no size");
        Check.equal("1.3.101.112", ed.key().algorithmName(), "the OID is the name of an unsupported key");
        expectMalformed(new byte[70 * 1024], "a certificate above the size limit");
    }

    /**
     * The DER reader is strict: minimal lengths, no indefinite lengths, no multi-byte tags, minimal integers, and very deep nesting
     * fails cleanly.
     * <p>
     * DER 해석기는 엄격하다: 최소 길이, 부정 길이 금지, 여러 바이트 태그 금지, 최소 정수. 매우 깊은 중첩도 깨끗하게 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStrictDer() throws Exception {
        String[] bad = { "", "30", "3081", "308100", "30810130", "3080", "30820001" + "00", "1f8101" + "00", "3003020100" + "00", "30840000000100",
            "300102" };
        // 엄격하지 않거나 잘린 부호화를 하나씩 본다.
        for (int i = 0; i < bad.length; i++) {
            final byte[] b = Hex.decode(bad[i]);
            Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    Der.readExactly(b, 0, b.length).children(10);
                }
            }, "DER " + bad[i]);
        }
        Check.equal(BigInteger.valueOf(128), Der.readExactly(Hex.decode("02020080"), 0, 4).integer(), "INTEGER 128");
        Check.equal(BigInteger.valueOf(-128), Der.readExactly(Hex.decode("020180"), 0, 3).integer(), "INTEGER -128");
        String[] badInt = { "0200", "02020001", "0202ff80" };
        // 최소가 아닌 정수를 하나씩 본다.
        for (int i = 0; i < badInt.length; i++) {
            final byte[] b = Hex.decode(badInt[i]);
            Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    Der.readExactly(b, 0, b.length).integer();
                }
            }, "INTEGER " + badInt[i]);
        }
        Check.equal("1.2.840.113549.1.1.11", Der.readExactly(DerWriter.oid("1.2.840.113549.1.1.11"), 0, 11).oid(), "OID round trip");
        Check.equal("2.5.29.19", Der.readExactly(Hex.decode("0603551d13"), 0, 5).oid(), "OID 2.5.29.19");
        final byte[] badOid = Hex.decode("06028001");
        Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                Der.readExactly(badOid, 0, badOid.length).oid();
            }
        }, "OID arc with a leading 0x80");
        Check.equal(1 | (1 << 5), Der.readExactly(DerWriter.namedBits(1 | (1 << 5)), 0, 4).namedBits(), "named bits round trip");
        // 10,000 겹의 SEQUENCE: 길이 옥텟 때문에 인증서 크기 한도 안에 다 들어가지 않으므로 잘린 구조가 된다.
        byte[] deep = new byte[20000];
        // 30 80 대신 엄격한 형식 30 82 xx xx 를 겹겹이 넣는다.
        for (int i = 0; i + 4 <= deep.length; i += 4) {
            int rest = deep.length - i - 4;
            deep[i] = 0x30;
            deep[i + 1] = (byte) 0x82;
            deep[i + 2] = (byte) (rest >> 8);
            deep[i + 3] = (byte) rest;
        }
        expectMalformed(deep, "5,000 nested sequences");
    }
}
