package org.duckdns.hjow.swingwebpanel.tls13.x509;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Vectors;

/**
 * The trust store of the TLS module (plan items P32-04-a and P32-04-c): the bundled Mozilla root list (made by
 * <code>tools/cabundle</code> from the CCADB report), stores made from a <code>KeyStore</code>, and the validation of certificate chains
 * that real HTTPS servers presented (recorded by <code>tools/tlsvectors</code> into <code>real-chains.txt</code>, validated at the time
 * of the recording because certificates expire).
 * <p>
 * TLS 모듈의 신뢰 저장소(계획 P32-04-a, P32-04-c): 내장된 Mozilla 루트 목록(<code>tools/cabundle</code> 이 CCADB 보고서로 만든 것),
 * <code>KeyStore</code> 로 만든 저장소, 실제 HTTPS 서버가 내놓은 인증서 사슬의 검증(<code>tools/tlsvectors</code> 가 <code>real-chains.txt</code> 에
 * 기록한 것. 인증서는 만료되므로 기록한 시각으로 검증한다).
 */
public class TrustStoreTest {

    /** The recorded chains. / 기록한 사슬. */
    private static final Vectors CHAINS = Vectors.load("/real-chains.txt");

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TrustStoreTest() {
    }

    /**
     * The bundled list loads, names its source, and holds only usable roots; the restrictions of the root program are present.
     * <p>
     * 내장 목록을 읽을 수 있고, 출처를 밝히며, 쓸 수 있는 루트만 담고 있다. 루트 프로그램의 제한이 들어 있다.
     */
    public void testBundledMozillaRoots() {
        TrustStore store = TrustStore.mozilla();
        Check.same(store, TrustStore.mozilla(), "the bundled store is loaded once");
        Check.isTrue(store.size() >= 100 && store.size() <= 300, "a plausible number of roots (" + store.size() + ")");
        Check.contains(store.source(), "ccadb", "the source names the CCADB");
        Check.contains(store.source(), "retrieved 20", "the source names the day of retrieval");
        int distrusted = 0;
        int constrained = 0;
        int ec = 0;
        // 루트마다 키와 CA 표시를 확인하고 제한을 센다.
        for (int i = 0; i < store.size(); i++) {
            TrustRoot r = store.roots().get(i);
            SubjectKey k = r.certificate().key();
            Check.isTrue(k.isRsa() || k.isEc(), "root " + i + " has a supported key");
            Check.isTrue(!k.isRsa() || k.bits() >= 2048, "root " + i + " has an RSA key of at least 2048 bits");
            Check.isTrue(r.certificate().isCa(), "root " + i + " is a CA");
            Check.isTrue(r.certificate().isSelfIssued(), "root " + i + " is self-issued");
            ec += k.isEc() ? 1 : 0;
            distrusted += r.distrustAfter() != Long.MAX_VALUE ? 1 : 0;
            constrained += r.appliedConstraints() != null ? 1 : 0;
            Check.isFalse(store.findBySubject(r.certificate().subject()).isEmpty(), "root " + i + " is found by its subject");
        }
        Check.isTrue(ec > 10, "the list has EC roots (" + ec + ")");
        Check.isTrue(distrusted >= 1, "the list has roots with a distrust date (" + distrusted + ")");
        Check.isTrue(constrained >= 1, "the list has a root with applied constraints (" + constrained + ")");
        Check.isTrue(store.findBySubject(new byte[] { 0x30, 0x00 }).isEmpty(), "an unknown subject finds nothing");
    }

    /**
     * Splits a recorded chain line into its certificates.
     * <p>
     * 기록한 사슬 줄을 인증서들로 나눈다.
     *
     * @param words the words of the line / 줄의 낱말들
     * @return the DER certificates / DER 인증서들
     * @throws X509Exception if the Base64 is damaged / Base64 가 손상되었으면
     */
    private static List<byte[]> certificates(String[] words) throws X509Exception {
        List<byte[]> out = new ArrayList<byte[]>();
        // 넷째 낱말부터가 인증서이다.
        for (int i = 4; i < words.length; i++) {
            out.add(TrustStore.base64(words[i]));
        }
        return out;
    }

    /**
     * The chains of real servers validate against the bundled roots at the time of the recording, for RSA and EC certificates alike;
     * another host name, a much later time and an empty trust store are rejected with the right reasons.
     * <p>
     * 실제 서버의 사슬이 기록한 시각에 내장 루트로 검증된다(RSA 와 EC 인증서 모두). 다른 호스트 이름, 한참 뒤의 시각, 빈 신뢰 저장소는 올바른 이유로
     * 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRealServerChains() throws Exception {
        List<String[]> lines = CHAINS.all("chain");
        Check.isTrue(lines.size() >= 10, "enough recorded chains (" + lines.size() + ")");
        final TrustStore store = TrustStore.mozilla();
        final TrustStore other = TrustStore.ofCertificates(Arrays.asList(new CertBuilder("Other Root", TestKey.rsa("other")).selfSigned().ca(-1).build()));
        int rsa = 0;
        int ec = 0;
        // 사슬마다 검증한다.
        for (int i = 0; i < lines.size(); i++) {
            final String host = lines.get(i)[1];
            final long when = Long.parseLong(lines.get(i)[2]);
            final List<byte[]> chain = certificates(lines.get(i));
            List<X509Cert> path;
            try {
                path = PathValidator.validate(chain, store, host, when);
            } catch (X509Exception e) {
                throw new org.duckdns.hjow.swingwebpanel.testing.AssertionFailure("the chain of " + host + " was rejected: " + e.getMessage());
            }
            Check.isTrue(path.size() >= 1 && path.size() <= chain.size(), host + ": path length " + path.size());
            if (path.get(0).key().isRsa()) {
                rsa++;
            } else {
                ec++;
            }
            X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    PathValidator.validate(chain, store, "wrong.host.example", when);
                }
            }, host + " with another host name");
            Check.equal(X509Exception.Reason.NAME_MISMATCH, e.reason(), host + ": name mismatch");
            e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    PathValidator.validate(chain, store, host, when + 3L * 366 * CertBuilder.DAY);
                }
            }, host + " three years later");
            Check.equal(X509Exception.Reason.EXPIRED, e.reason(), host + ": expired");
            e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    PathValidator.validate(chain, other, host, when);
                }
            }, host + " with another trust store");
            Check.equal(X509Exception.Reason.UNTRUSTED, e.reason(), host + ": untrusted");
            // 서버 인증서의 서명 한 비트를 바꾸면 서명 오류가 된다.
            final List<byte[]> forged = new ArrayList<byte[]>(chain);
            byte[] leaf = forged.get(0).clone();
            leaf[leaf.length - 1] ^= 1;
            forged.set(0, leaf);
            e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    PathValidator.validate(forged, store, host, when);
                }
            }, host + " with a changed signature");
            Check.equal(X509Exception.Reason.BAD_SIGNATURE, e.reason(), host + ": bad signature");
        }
        Check.isTrue(rsa > 0 && ec > 0, "both RSA (" + rsa + ") and EC (" + ec + ") server certificates were validated");
    }

    /**
     * A store made from a <code>KeyStore</code> replaces the bundled list: its certificate entries are the only roots.
     * <p>
     * <code>KeyStore</code> 로 만든 저장소는 내장 목록을 대신한다: 그 인증서 항목만이 루트이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testKeyStoreOfTheHost() throws Exception {
        TestKey rootKey = TestKey.rsa("root");
        TestKey leafKey = TestKey.rsa("leaf");
        byte[] root = new CertBuilder("Host Root", rootKey).selfSigned().ca(-1).build();
        byte[] leaf = new CertBuilder("server", leafKey).issuer("Host Root", rootKey).dns("server.example.test").build();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setCertificateEntry("root", CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(root)));
        final TrustStore store = TrustStore.of(ks);
        Check.equal(1, store.size(), "one root from the key store");
        Check.contains(store.source(), "key store", "the source names the key store");
        Check.equal(1, PathValidator.validate(Arrays.asList(leaf), store, "server.example.test", CertBuilder.NOW).size(), "chain to the root of the key store");
        // 내장 목록의 루트로 이어지는 실제 사슬은 이 저장소에서는 신뢰하지 않는다.
        final String[] real = CHAINS.all("chain").get(0);
        final List<byte[]> chain = certificates(real);
        X509Exception e = (X509Exception) Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                PathValidator.validate(chain, store, real[1], Long.parseLong(real[2]));
            }
        }, "a public chain with the key store of the host");
        Check.equal(X509Exception.Reason.UNTRUSTED, e.reason(), "public roots are not trusted by the store of the host");
        Check.equal(0, TrustStore.of(emptyKeyStore()).size(), "an empty key store gives an empty store");
    }

    /**
     * Creates an empty key store.
     * <p>
     * 빈 키 저장소를 만든다.
     *
     * @return the key store / 키 저장소
     * @throws Exception on a provider failure / 공급자 오류 시
     */
    private static KeyStore emptyKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    /**
     * The reader of the root list rejects damaged files and reads dates and constraints.
     * <p>
     * 루트 목록 해석기가 손상된 파일을 거부하고 날짜와 제약을 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRootListFormat() throws Exception {
        byte[] root = new CertBuilder("List Root", TestKey.rsa("root")).selfSigned().ca(-1).build();
        StringBuilder b64 = new StringBuilder();
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        // 시험용 Base64 부호화(세 바이트씩).
        for (int i = 0; i < root.length; i += 3) {
            int b0 = root[i] & 0xff;
            int b1 = i + 1 < root.length ? root[i + 1] & 0xff : 0;
            int b2 = i + 2 < root.length ? root[i + 2] & 0xff : 0;
            b64.append(alphabet.charAt(b0 >> 2)).append(alphabet.charAt(((b0 & 3) << 4) | (b1 >> 4)));
            b64.append(i + 1 < root.length ? alphabet.charAt(((b1 & 15) << 2) | (b2 >> 6)) : '=');
            b64.append(i + 2 < root.length ? alphabet.charAt(b2 & 63) : '=');
        }
        Check.equal(Hex.encode(root), Hex.encode(TrustStore.base64(b64.toString())), "Base64 round trip");
        String text = "# source: https://example.test/report\n# retrieved: 2026-11-28\n\nroot\t2026-01-31\t*.tr,example.test\tList Root\t" + b64 + "\n";
        TrustStore store = TrustStore.read(new ByteArrayInputStream(text.getBytes("UTF-8")));
        Check.equal(1, store.size(), "one root");
        Check.contains(store.source(), "https://example.test/report", "source URL");
        Check.contains(store.source(), "2026-11-28", "retrieval date");
        TrustRoot r = store.roots().get(0);
        Check.equal(1769903999000L, r.distrustAfter(), "distrust date: the end of 2026-01-31 in UTC");
        Check.notNull(r.appliedConstraints(), "applied constraints");
        String[] bad = { "", "# only comments\n", "root\t-\t-\tname\n", "root\t-\t-\tname\t!!!!\n", "root\tnot-a-date\t-\tname\t" + b64 + "\n",
            "other\t-\t-\tname\t" + b64 + "\n", "root\t-\t-\tname\tAAAA\n" };
        // 손상된 목록을 하나씩 본다.
        for (int i = 0; i < bad.length; i++) {
            final byte[] data = bad[i].getBytes("UTF-8");
            Check.throwsException(X509Exception.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    TrustStore.read(new ByteArrayInputStream(data));
                }
            }, "damaged root list #" + i);
        }
    }
}
