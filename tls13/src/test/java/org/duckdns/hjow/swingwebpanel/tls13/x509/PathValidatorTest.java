package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;

/**
 * Path building and validation of the TLS module (plan item P32-04-b) with certificate chains made for the purpose: the valid chains
 * pass, and every defect is rejected with its own reason (expired, not yet valid, name mismatch, self-signed, untrusted root, missing
 * intermediate, non-CA issuer, key usage, extended key usage, path length, name constraints, weak key, SHA-1 signature, wrong
 * signature, unknown critical extension, distrusted root).
 * <p>
 * TLS 모듈의 경로 구성과 검증(계획 P32-04-b)을 목적에 맞게 만든 인증서 사슬로 시험한다: 유효한 사슬은 통과하고, 결함마다 고유한 이유로 거부한다
 * (만료, 아직 유효하지 않음, 이름 불일치, 자체 서명, 미신뢰 루트, 중간 인증서 누락, CA 가 아닌 발급자, 키 용도, 확장 키 용도, 경로 길이, 이름 제약,
 * 약한 키, SHA-1 서명, 틀린 서명, 모르는 critical 확장, 불신 루트).
 */
public class PathValidatorTest {

    /** The host name of the test server. / 시험 서버의 호스트 이름. */
    private static final String HOST = "www.example.test";

    /** The key of the root. / 루트의 키. */
    private static final TestKey ROOT_KEY = TestKey.rsa("root");

    /** The key of the intermediate CA. / 중간 CA 의 키. */
    private static final TestKey CA_KEY = TestKey.rsa("ca");

    /** The key of the server. / 서버의 키. */
    private static final TestKey LEAF_KEY = TestKey.rsa("leaf");

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PathValidatorTest() {
    }

    /**
     * Builds the root certificate.
     * <p>
     * 루트 인증서를 만든다.
     *
     * @return the builder / 빌더
     */
    private static CertBuilder root() {
        return new CertBuilder("Test Root", ROOT_KEY).selfSigned().ca(-1);
    }

    /**
     * Builds the intermediate CA certificate, signed by the root.
     * <p>
     * 루트가 서명한 중간 CA 인증서를 만든다.
     *
     * @return the builder / 빌더
     */
    private static CertBuilder ca() {
        return new CertBuilder("Test CA", CA_KEY).issuer("Test Root", ROOT_KEY).ca(0);
    }

    /**
     * Builds the server certificate, signed by the intermediate CA.
     * <p>
     * 중간 CA 가 서명한 서버 인증서를 만든다.
     *
     * @return the builder / 빌더
     */
    private static CertBuilder leaf() {
        return new CertBuilder(HOST, LEAF_KEY).issuer("Test CA", CA_KEY).dns(HOST).keyUsage(1).eku(X509Cert.EKU_SERVER_AUTH);
    }

    /**
     * Makes a trust store with one root.
     * <p>
     * 루트 하나를 가진 신뢰 저장소를 만든다.
     *
     * @param rootDer the root certificate / 루트 인증서
     * @return the store / 저장소
     * @throws X509Exception never for valid test certificates / 유효한 시험 인증서에서는 발생하지 않는다
     */
    private static TrustStore store(byte[] rootDer) throws X509Exception {
        return TrustStore.ofCertificates(Arrays.asList(rootDer));
    }

    /**
     * Validates at the fixed test time.
     * <p>
     * 고정된 시험 시각으로 검증한다.
     *
     * @param store the trust store / 신뢰 저장소
     * @param host  the host / 호스트
     * @param chain the certificates of the server / 서버의 인증서들
     * @return the path / 경로
     * @throws X509Exception if the chain is rejected / 사슬을 거부하면
     */
    private static List<X509Cert> validate(TrustStore store, String host, byte[]... chain) throws X509Exception {
        return PathValidator.validate(Arrays.asList(chain), store, host, CertBuilder.NOW);
    }

    /**
     * Expects a rejection with a reason.
     * <p>
     * 그 이유로 거부됨을 기대한다.
     *
     * @param reason the expected reason / 기대하는 이유
     * @param what   the description / 설명
     * @param store  the trust store / 신뢰 저장소
     * @param host   the host / 호스트
     * @param chain  the certificates of the server / 서버의 인증서들
     */
    private static void reject(X509Exception.Reason reason, String what, final TrustStore store, final String host, final byte[]... chain) {
        X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                validate(store, host, chain);
            }
        }, what);
        Check.equal(reason, e.reason(), what + " (" + e.getMessage() + ")");
    }

    /**
     * Valid chains pass: with the intermediate, in any order, with the root included, with extra unrelated certificates, and EC chains.
     * <p>
     * 유효한 사슬은 통과한다: 중간 인증서 포함, 임의의 순서, 루트 포함, 관계없는 인증서 추가, EC 사슬.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testValidChains() throws Exception {
        byte[] root = root().build();
        byte[] ca = ca().build();
        byte[] leaf = leaf().build();
        TrustStore store = store(root);
        List<X509Cert> path = validate(store, HOST, leaf, ca);
        Check.equal(2, path.size(), "path of server and intermediate certificate");
        Check.isTrue(path.get(0).key().isRsa(), "the first certificate is the server certificate");
        byte[] other = new CertBuilder("Other CA", TestKey.rsa("other")).selfSigned().ca(-1).build();
        Check.equal(2, validate(store, HOST, leaf, other, root, ca).size(), "extra certificates and any order");
        Check.equal(2, validate(store, "WWW.Example.Test.", leaf, ca).size(), "host name in another case with a trailing dot");

        // 루트가 직접 서명한 서버 인증서.
        byte[] direct = new CertBuilder(HOST, LEAF_KEY).issuer("Test Root", ROOT_KEY).dns(HOST).build();
        Check.equal(1, validate(store, HOST, direct).size(), "certificate signed by the root");

        // EC 사슬: P-384 루트, P-256 중간 인증서, P-256 서버, 그리고 PSS 로 서명한 RSA 서버.
        TestKey ecRoot = TestKey.ec("root", EcCurve.P384);
        TestKey ecCa = TestKey.ec("ca", EcCurve.P256);
        TestKey ecLeaf = TestKey.ec("leaf", EcCurve.P256);
        byte[] r = new CertBuilder("EC Root", ecRoot).selfSigned().ca(-1).signature(TestKey.ECDSA, HashAlg.SHA384).build();
        byte[] c = new CertBuilder("EC CA", ecCa).issuer("EC Root", ecRoot).ca(0).signature(TestKey.ECDSA, HashAlg.SHA384).build();
        byte[] l = new CertBuilder(HOST, ecLeaf).issuer("EC CA", ecCa).dns("*.example.test").keyUsage(1).build();
        Check.isTrue(validate(store(r), HOST, l, c).get(0).key().isEc(), "EC chain with a wildcard name");
        byte[] pssLeaf = new CertBuilder(HOST, LEAF_KEY).issuer("Test CA", CA_KEY).dns(HOST).signature(TestKey.PSS, HashAlg.SHA256).build();
        Check.equal(2, validate(store, HOST, pssLeaf, ca).size(), "certificate signed with RSA-PSS");

        // 서버 인증서 자체를 신뢰 저장소에 넣은 경우(자체 서명 시험 서버).
        byte[] self = new CertBuilder(HOST, LEAF_KEY).selfSigned().dns(HOST).build();
        Check.equal(1, validate(store(self), HOST, self).size(), "a self-signed certificate that the host trusts directly");
        Check.equal(1, validate(store(root), "127.0.0.1", new CertBuilder("ip", LEAF_KEY).issuer("Test Root", ROOT_KEY).ip(new byte[] { 127, 0, 0, 1 }).build()).size(),
                "IP address literal");
    }

    /**
     * Validity dates of the server certificate and of the intermediate certificate are enforced.
     * <p>
     * 서버 인증서와 중간 인증서의 유효 기간을 강제한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testValidityDates() throws Exception {
        byte[] root = root().build();
        TrustStore store = store(root);
        byte[] ca = ca().build();
        long now = CertBuilder.NOW;
        long day = CertBuilder.DAY;
        reject(X509Exception.Reason.EXPIRED, "expired server certificate", store, HOST, leaf().validity(now - 400 * day, now - day).build(), ca);
        reject(X509Exception.Reason.NOT_YET_VALID, "server certificate from the future", store, HOST, leaf().validity(now + day, now + 30 * day).build(), ca);
        reject(X509Exception.Reason.EXPIRED, "expired intermediate certificate", store, HOST, leaf().build(), ca().validity(now - 400 * day, now - day).build());
        reject(X509Exception.Reason.NOT_YET_VALID, "intermediate certificate from the future", store, HOST, leaf().build(),
                ca().validity(now + day, now + 30 * day).build());
        Check.equal(2, validate(store, HOST, leaf().validity(now, now).build(), ca).size(), "valid exactly at both ends");
        // 같은 이름의 중간 인증서가 둘이고 하나만 유효하면(교차 서명, 갱신) 유효한 것으로 경로를 만든다.
        Check.equal(2, validate(store, HOST, leaf().build(), ca().validity(now - 400 * day, now - day).build(), ca).size(), "an expired and a valid intermediate");
    }

    /**
     * The host name must match; chains without a path to a trusted root are untrusted.
     * <p>
     * 호스트 이름이 맞아야 한다. 신뢰하는 루트까지 경로가 없는 사슬은 신뢰하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNameAndTrust() throws Exception {
        byte[] root = root().build();
        byte[] ca = ca().build();
        byte[] leaf = leaf().build();
        TrustStore store = store(root);
        reject(X509Exception.Reason.NAME_MISMATCH, "another host", store, "other.example.test", leaf, ca);
        reject(X509Exception.Reason.NAME_MISMATCH, "an IP address for a certificate with DNS names", store, "127.0.0.1", leaf, ca);
        reject(X509Exception.Reason.NAME_MISMATCH, "common name without subjectAltName", store, HOST,
                new CertBuilder(HOST, LEAF_KEY).issuer("Test CA", CA_KEY).build(), ca);
        reject(X509Exception.Reason.UNTRUSTED, "self-signed server certificate", store, HOST, new CertBuilder(HOST, LEAF_KEY).selfSigned().dns(HOST).build());
        reject(X509Exception.Reason.UNTRUSTED, "missing intermediate certificate", store, HOST, leaf);
        byte[] otherRoot = new CertBuilder("Other Root", TestKey.rsa("other")).selfSigned().ca(-1).build();
        reject(X509Exception.Reason.UNTRUSTED, "root that is not in the trust store", store(otherRoot), HOST, leaf, ca, root);
        // 이름은 같지만 키가 다른 루트: 서명이 맞지 않는다.
        byte[] fakeRoot = new CertBuilder("Test Root", TestKey.rsa("other")).selfSigned().ca(-1).build();
        reject(X509Exception.Reason.BAD_SIGNATURE, "trusted root with the same name but another key", store(fakeRoot), HOST, leaf, ca);
        reject(X509Exception.Reason.BAD_SIGNATURE, "server certificate with a wrong signature", store, HOST, leaf().corruptSignature().build(), ca);
        reject(X509Exception.Reason.BAD_SIGNATURE, "intermediate certificate with a wrong signature", store, HOST, leaf, ca().corruptSignature().build());
        reject(X509Exception.Reason.MALFORMED, "no certificate", store, HOST);
        reject(X509Exception.Reason.MALFORMED, "garbage instead of a certificate", store, HOST, new byte[] { 1, 2, 3 });
        byte[][] many = new byte[PathValidator.MAX_CHAIN + 1][];
        Arrays.fill(many, leaf);
        reject(X509Exception.Reason.TOO_LONG, "more certificates than the limit", store, HOST, many);
    }

    /**
     * The issuer must be a CA with the right key usage; key usage, extended key usage and path length are enforced.
     * <p>
     * 발급자는 올바른 키 용도를 가진 CA 여야 한다. 키 용도, 확장 키 용도, 경로 길이를 강제한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUsageAndPathLength() throws Exception {
        byte[] root = root().build();
        byte[] ca = ca().build();
        TrustStore store = store(root);
        byte[] notCa = new CertBuilder("Test CA", CA_KEY).issuer("Test Root", ROOT_KEY).build();
        reject(X509Exception.Reason.NOT_CA, "intermediate certificate without the CA flag", store, HOST, leaf().build(), notCa);
        byte[] noCertSign = new CertBuilder("Test CA", CA_KEY).issuer("Test Root", ROOT_KEY).ca(0).keyUsage(1).build();
        reject(X509Exception.Reason.KEY_USAGE, "intermediate certificate without keyCertSign", store, HOST, leaf().build(), noCertSign);
        reject(X509Exception.Reason.KEY_USAGE, "server certificate without digitalSignature", store, HOST, leaf().keyUsage(1 << 2).build(), ca);
        reject(X509Exception.Reason.EXTENDED_KEY_USAGE, "server certificate for client authentication only", store, HOST,
                leaf().eku("1.3.6.1.5.5.7.3.2").build(), ca);
        reject(X509Exception.Reason.EXTENDED_KEY_USAGE, "intermediate certificate for email only", store, HOST, leaf().build(),
                ca().eku("1.3.6.1.5.5.7.3.4").build());
        Check.equal(2, validate(store, HOST, leaf().eku(X509Cert.EKU_ANY).build(), ca).size(), "anyExtendedKeyUsage is accepted");

        // 경로 길이: 중간 CA(pathLen 0) 아래에 CA 가 하나 더 있으면 거부한다.
        TestKey subKey = TestKey.rsa("other");
        byte[] sub = new CertBuilder("Sub CA", subKey).issuer("Test CA", CA_KEY).ca(-1).build();
        byte[] deepLeaf = new CertBuilder(HOST, LEAF_KEY).issuer("Sub CA", subKey).dns(HOST).build();
        reject(X509Exception.Reason.PATH_LENGTH, "a CA below a CA with path length 0", store, HOST, deepLeaf, sub, ca);
        Check.equal(3, validate(store, HOST, deepLeaf, sub, ca().ca(1).build()).size(), "path length 1 allows one CA below");
        byte[] strictRoot = new CertBuilder("Test Root", ROOT_KEY).selfSigned().ca(0).build();
        reject(X509Exception.Reason.PATH_LENGTH, "the path length of the root", store(strictRoot), HOST, leaf().build(), ca);
    }

    /**
     * Weak keys, SHA-1 signatures, unsupported algorithms and unknown critical extensions are rejected.
     * <p>
     * 약한 키, SHA-1 서명, 지원하지 않는 알고리즘, 모르는 critical 확장을 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWeakAndUnsupported() throws Exception {
        byte[] root = root().build();
        byte[] ca = ca().build();
        TrustStore store = store(root);
        TestKey weak = TestKey.rsa("weak", 1024);
        reject(X509Exception.Reason.WEAK_KEY, "server certificate with a 1024 bit RSA key", store, HOST,
                new CertBuilder(HOST, weak).issuer("Test CA", CA_KEY).dns(HOST).build(), ca);
        byte[] weakCa = new CertBuilder("Weak CA", weak).issuer("Test Root", ROOT_KEY).ca(0).build();
        reject(X509Exception.Reason.WEAK_KEY, "intermediate certificate with a 1024 bit RSA key", store, HOST,
                new CertBuilder(HOST, LEAF_KEY).issuer("Weak CA", weak).dns(HOST).build(), weakCa);
        reject(X509Exception.Reason.WEAK_SIGNATURE, "server certificate signed with SHA-1", store, HOST, leaf().signatureOid("1.2.840.113549.1.1.5").build(), ca);
        reject(X509Exception.Reason.WEAK_SIGNATURE, "intermediate certificate signed with SHA-1", store, HOST, leaf().build(),
                ca().signatureOid("1.2.840.113549.1.1.5").build());
        reject(X509Exception.Reason.UNSUPPORTED_ALGORITHM, "server certificate signed with an unknown algorithm", store, HOST,
                leaf().signatureOid("1.3.101.112").build(), ca);
        reject(X509Exception.Reason.UNKNOWN_CRITICAL_EXTENSION, "unknown critical extension in the server certificate", store, HOST,
                leaf().extension("1.2.3.4.5", true).build(), ca);
        reject(X509Exception.Reason.UNKNOWN_CRITICAL_EXTENSION, "unknown critical extension in the intermediate certificate", store, HOST, leaf().build(),
                ca().extension("1.2.3.4.5", true).build());
        Check.equal(2, validate(store, HOST, leaf().extension("1.2.3.4.5", false).build(), ca).size(), "an unknown extension that is not critical");
        // SHA-1 로 자체 서명한 루트는 괜찮다(루트의 서명은 검증하지 않는다).
        byte[] sha1Root = new CertBuilder("Test Root", ROOT_KEY).selfSigned().ca(-1).signatureOid("1.2.840.113549.1.1.5").build();
        Check.equal(2, validate(store(sha1Root), HOST, leaf().build(), ca).size(), "a root that is self-signed with SHA-1");
    }

    /**
     * Name constraints of intermediate certificates, of the root and of the root program are enforced.
     * <p>
     * 중간 인증서, 루트, 루트 프로그램의 이름 제약을 강제한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNameConstraints() throws Exception {
        byte[] root = root().build();
        TrustStore store = store(root);
        byte[] leaf = leaf().build();
        Check.equal(2, validate(store, HOST, leaf, ca().permitDns("example.test").build()).size(), "name inside the permitted subtree");
        Check.equal(2, validate(store, HOST, leaf, ca().permitDns(".example.test").build()).size(), "name below a subtree that starts with a dot");
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "name outside the permitted subtree", store, HOST, leaf, ca().permitDns("example.org").build());
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "a longer name that only ends with the subtree text", store, "www.badexample.test",
                new CertBuilder("x", LEAF_KEY).issuer("Test CA", CA_KEY).dns("www.badexample.test").build(), ca().permitDns("example.test").build());
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "name inside an excluded subtree", store, HOST, leaf, ca().excludeDns("example.test").build());
        Check.equal(2, validate(store, HOST, leaf, ca().excludeDns("other.test").build()).size(), "name outside the excluded subtree");
        // 여러 이름 가운데 하나라도 허용 범위 밖이면 거부한다.
        byte[] two = leaf().dns("www.example.org").build();
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "one of two names is outside", store, HOST, two, ca().permitDns("example.test").build());
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "wildcard name outside the permitted subtree", store, "a.example.org",
                new CertBuilder("x", LEAF_KEY).issuer("Test CA", CA_KEY).dns("*.example.org").build(), ca().permitDns("example.test").build());

        byte[] ipLeaf = new CertBuilder("ip", LEAF_KEY).issuer("Test CA", CA_KEY).ip(new byte[] { 10, 1, 2, 3 }).build();
        byte[] tenNet = { 10, 0, 0, 0, (byte) 255, 0, 0, 0 };
        byte[] otherNet = { (byte) 192, (byte) 168, 0, 0, (byte) 255, (byte) 255, 0, 0 };
        Check.equal(2, validate(store, "10.1.2.3", ipLeaf, ca().permitIp(tenNet).build()).size(), "address inside the permitted range");
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "address outside the permitted range", store, "10.1.2.3", ipLeaf, ca().permitIp(otherNet).build());

        byte[] orgLeaf = leaf().subject("Example Org", HOST).build();
        Check.equal(2, validate(store, HOST, orgLeaf, ca().permitOrganization("Example Org").build()).size(), "subject inside the permitted directory subtree");
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "subject outside the permitted directory subtree", store, HOST, orgLeaf,
                ca().permitOrganization("Other Org").build());

        // 루트 자체의 제약과 루트 프로그램이 적용한 제약.
        byte[] constrainedRoot = new CertBuilder("Test Root", ROOT_KEY).selfSigned().ca(-1).permitDns("example.org").build();
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "the name constraints of the root", store(constrainedRoot), HOST, leaf, ca().build());
        TrustStore applied = store.restrict(0, Long.MAX_VALUE, Arrays.asList("*.org"));
        reject(X509Exception.Reason.NAME_CONSTRAINTS, "the constraints applied to the root", applied, HOST, leaf, ca().build());
        Check.equal(2, validate(store.restrict(0, Long.MAX_VALUE, Arrays.asList("*.test")), HOST, leaf, ca().build()).size(), "inside the applied constraints");
    }

    /**
     * A root with a distrust date is trusted only for certificates issued up to that date.
     * <p>
     * 불신 날짜가 있는 루트는 그 날짜까지 발급한 인증서에 대해서만 신뢰한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDistrustAfter() throws Exception {
        byte[] root = root().build();
        byte[] ca = ca().build();
        long issued = CertBuilder.NOW - 30 * CertBuilder.DAY;
        TrustStore before = store(root).restrict(0, issued - CertBuilder.DAY, null);
        reject(X509Exception.Reason.DISTRUSTED, "certificate issued after the distrust date", before, HOST, leaf().build(), ca);
        TrustStore after = store(root).restrict(0, issued + CertBuilder.DAY, null);
        Check.equal(2, validate(after, HOST, leaf().build(), ca).size(), "certificate issued before the distrust date");
    }

    /**
     * A chain with many certificates of the same name cannot make the search run long: the number of signature checks is limited.
     * <p>
     * 같은 이름의 인증서가 많은 사슬이 탐색을 오래 끌 수 없다: 서명 검사 횟수가 제한된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSearchIsBounded() throws Exception {
        TestKey loopKey = TestKey.rsa("other");
        List<byte[]> chain = new ArrayList<byte[]>();
        chain.add(new CertBuilder(HOST, LEAF_KEY).issuer("Loop", loopKey).dns(HOST).build());
        // 자신이 자신을 발급한 것처럼 보이는 같은 이름의 CA 인증서 아홉 개.
        for (int i = 0; i < 9; i++) {
            chain.add(new CertBuilder("Loop", loopKey).selfSigned().ca(-1).build());
        }
        long start = System.currentTimeMillis();
        final List<byte[]> c = chain;
        final TrustStore store = store(root().build());
        X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                PathValidator.validate(c, store, HOST, CertBuilder.NOW);
            }
        }, "a chain of look-alike certificates");
        Check.isTrue(e.reason() == X509Exception.Reason.TOO_LONG || e.reason() == X509Exception.Reason.UNTRUSTED, "reason " + e.reason());
        Check.isTrue(System.currentTimeMillis() - start < 20000, "the search ends quickly");
    }
}
