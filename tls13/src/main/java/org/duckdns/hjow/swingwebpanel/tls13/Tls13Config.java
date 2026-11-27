package org.duckdns.hjow.swingwebpanel.tls13;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.proto.CipherSuite;
import org.duckdns.hjow.swingwebpanel.tls13.proto.ClientHello;
import org.duckdns.hjow.swingwebpanel.tls13.proto.Parameters;
import org.duckdns.hjow.swingwebpanel.tls13.x509.TrustStore;

/**
 * The settings of a {@link Tls13SocketFactory}: the trusted roots, the protocol versions, the cipher suites and the key exchange
 * groups. An object is immutable; the <code>with...</code> methods return changed copies. There is deliberately no setting that turns
 * the certificate check or the host name check off; a test server with its own certificate is used by giving its certificate (or its
 * root) as the trust store. Usage:
 * <pre>
 * Tls13Config config = Tls13Config.defaults();                       // bundled Mozilla roots, TLS 1.3 and 1.2
 * Tls13Config own = config.withKeyStore(myTrustedCertificates);      // other roots
 * SSLSocketFactory factory = new Tls13SocketFactory(own);
 * </pre>
 * <p>
 * {@link Tls13SocketFactory} 의 설정: 신뢰하는 루트, 프로토콜 버전, 암호 스위트, 키 교환 그룹. 객체는 변경할 수 없고 <code>with...</code> 메서드가 바뀐
 * 사본을 반환한다. 인증서 검사나 호스트 이름 검사를 끄는 설정은 일부러 두지 않았다. 자체 인증서를 쓰는 시험 서버는 그 인증서(또는 그 루트)를 신뢰
 * 저장소로 지정해서 쓴다.
 */
public final class Tls13Config {

    /** The trusted roots, or null for the bundled Mozilla list (loaded when first needed). / 신뢰하는 루트. null 이면 내장 Mozilla 목록(처음 필요할 때 읽는다). */
    private final TrustStore trustStore;

    /** True to offer TLS 1.3. / TLS 1.3 을 제안하려면 true. */
    private final boolean tls13;

    /** True to offer TLS 1.2. / TLS 1.2 를 제안하려면 true. */
    private final boolean tls12;

    /** The cipher suites in order of preference. / 선호 순서의 암호 스위트. */
    private final CipherSuite[] suites;

    /** The key exchange groups in order of preference. / 선호 순서의 키 교환 그룹. */
    private final int[] groups;

    /**
     * Creates a configuration.
     * <p>
     * 설정을 만든다.
     *
     * @param trustStore the trusted roots or null / 신뢰하는 루트 또는 null
     * @param tls13      offer TLS 1.3 / TLS 1.3 제안
     * @param tls12      offer TLS 1.2 / TLS 1.2 제안
     * @param suites     the cipher suites / 암호 스위트
     * @param groups     the groups / 그룹
     */
    private Tls13Config(TrustStore trustStore, boolean tls13, boolean tls12, CipherSuite[] suites, int[] groups) {
        this.trustStore = trustStore;
        this.tls13 = tls13;
        this.tls12 = tls12;
        this.suites = suites;
        this.groups = groups;
    }

    /**
     * Returns the default configuration: the bundled Mozilla root list, TLS 1.3 and TLS 1.2, all cipher suites of the module, and the
     * groups x25519, secp256r1 and secp384r1.
     * <p>
     * 기본 설정을 반환한다: 내장 Mozilla 루트 목록, TLS 1.3 과 TLS 1.2, 모듈의 모든 암호 스위트, 그룹 x25519, secp256r1, secp384r1.
     *
     * @return the configuration / 설정
     */
    public static Tls13Config defaults() {
        return new Tls13Config(null, true, true, CipherSuite.all(),
                new int[] { ClientHello.GROUP_X25519, ClientHello.GROUP_SECP256R1, ClientHello.GROUP_SECP384R1 });
    }

    /**
     * Returns a copy that trusts the given roots instead of the bundled list.
     * <p>
     * 내장 목록 대신 주어진 루트를 신뢰하는 사본을 반환한다.
     *
     * @param store the trusted roots / 신뢰하는 루트
     * @return the changed copy / 바뀐 사본
     */
    public Tls13Config withTrustStore(TrustStore store) {
        if (store == null) {
            throw new IllegalArgumentException("trust store must not be null");
        }
        return new Tls13Config(store, tls13, tls12, suites, groups);
    }

    /**
     * Returns a copy that trusts the certificate entries of a key store (for example the <code>cacerts</code> of a JRE) instead of the
     * bundled list.
     * <p>
     * 내장 목록 대신 키 저장소(예: JRE 의 <code>cacerts</code>)의 인증서 항목을 신뢰하는 사본을 반환한다.
     *
     * @param keyStore the loaded key store / 읽어 둔 키 저장소
     * @return the changed copy / 바뀐 사본
     * @throws KeyStoreException if the key store is not loaded / 키 저장소를 읽지 않았으면
     */
    public Tls13Config withKeyStore(KeyStore keyStore) throws KeyStoreException {
        return withTrustStore(TrustStore.of(keyStore));
    }

    /**
     * Returns a copy that offers the given protocol versions.
     * <p>
     * 주어진 프로토콜 버전을 제안하는 사본을 반환한다.
     *
     * @param names <code>TLSv1.3</code> and/or <code>TLSv1.2</code> / <code>TLSv1.3</code>, <code>TLSv1.2</code>
     * @return the changed copy / 바뀐 사본
     * @throws IllegalArgumentException for another name or an empty list / 다른 이름이거나 목록이 비었으면
     */
    public Tls13Config withProtocols(String... names) {
        boolean v13 = false;
        boolean v12 = false;
        // 이름마다 버전을 켠다.
        for (int i = 0; i < names.length; i++) {
            if ("TLSv1.3".equals(names[i])) {
                v13 = true;
            } else if ("TLSv1.2".equals(names[i])) {
                v12 = true;
            } else {
                throw new IllegalArgumentException("unsupported protocol " + names[i]);
            }
        }
        if (!v13 && !v12) {
            throw new IllegalArgumentException("no protocol");
        }
        return new Tls13Config(trustStore, v13, v12, suites, groups);
    }

    /**
     * Returns a copy that offers the given cipher suites in the given order.
     * <p>
     * 주어진 암호 스위트를 주어진 순서로 제안하는 사본을 반환한다.
     *
     * @param names standard names such as <code>TLS_AES_128_GCM_SHA256</code> / <code>TLS_AES_128_GCM_SHA256</code> 같은 표준 이름
     * @return the changed copy / 바뀐 사본
     * @throws IllegalArgumentException for a suite that the module does not have, or an empty list / 모듈에 없는 스위트이거나 목록이 비었으면
     */
    public Tls13Config withCipherSuites(String... names) {
        List<CipherSuite> chosen = new ArrayList<CipherSuite>();
        CipherSuite[] all = CipherSuite.all();
        // 이름마다 모듈의 스위트를 찾는다.
        for (int i = 0; i < names.length; i++) {
            CipherSuite found = null;
            // 모듈의 스위트 목록에서 이름이 같은 것을 찾는다.
            for (int k = 0; k < all.length; k++) {
                if (all[k].name().equals(names[i])) {
                    found = all[k];
                }
            }
            if (found == null) {
                throw new IllegalArgumentException("unsupported cipher suite " + names[i]);
            }
            if (!chosen.contains(found)) {
                chosen.add(found);
            }
        }
        if (chosen.isEmpty()) {
            throw new IllegalArgumentException("no cipher suite");
        }
        return new Tls13Config(trustStore, tls13, tls12, chosen.toArray(new CipherSuite[chosen.size()]), groups);
    }

    /**
     * Returns a copy that offers the given key exchange groups in the given order; the first one is sent as key share.
     * <p>
     * 주어진 키 교환 그룹을 주어진 순서로 제안하는 사본을 반환한다. 첫째 것을 키 공유로 보낸다.
     *
     * @param names <code>x25519</code>, <code>secp256r1</code>, <code>secp384r1</code>
     * @return the changed copy / 바뀐 사본
     * @throws IllegalArgumentException for another name or an empty list / 다른 이름이거나 목록이 비었으면
     */
    public Tls13Config withGroups(String... names) {
        int[] g = new int[names.length];
        // 이름마다 그룹 번호로 바꾼다.
        for (int i = 0; i < names.length; i++) {
            if ("x25519".equals(names[i])) {
                g[i] = ClientHello.GROUP_X25519;
            } else if ("secp256r1".equals(names[i])) {
                g[i] = ClientHello.GROUP_SECP256R1;
            } else if ("secp384r1".equals(names[i])) {
                g[i] = ClientHello.GROUP_SECP384R1;
            } else {
                throw new IllegalArgumentException("unsupported group " + names[i]);
            }
        }
        if (g.length == 0) {
            throw new IllegalArgumentException("no group");
        }
        return new Tls13Config(trustStore, tls13, tls12, suites, g);
    }

    /**
     * Returns the trusted roots (the bundled Mozilla list unless another store was set).
     * <p>
     * 신뢰하는 루트를 반환한다(다른 저장소를 정하지 않았으면 내장 Mozilla 목록).
     *
     * @return the trust store / 신뢰 저장소
     */
    public TrustStore trustStore() {
        return trustStore != null ? trustStore : TrustStore.mozilla();
    }

    /**
     * Returns the names of the offered protocol versions, the newest first.
     * <p>
     * 제안하는 프로토콜 버전의 이름을 최신 것부터 반환한다.
     *
     * @return the names / 이름들
     */
    public String[] protocols() {
        return tls13 && tls12 ? new String[] { "TLSv1.3", "TLSv1.2" } : tls13 ? new String[] { "TLSv1.3" } : new String[] { "TLSv1.2" };
    }

    /**
     * Returns the names of the offered cipher suites in order of preference.
     * <p>
     * 제안하는 암호 스위트의 이름을 선호 순서로 반환한다.
     *
     * @return the names / 이름들
     */
    public String[] cipherSuites() {
        List<String> out = new ArrayList<String>();
        // 켜 둔 버전의 스위트만 넣는다.
        for (int i = 0; i < suites.length; i++) {
            if (suites[i].isTls13() ? tls13 : tls12) {
                out.add(suites[i].name());
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * Creates the protocol settings of one connection.
     * <p>
     * 연결 하나의 프로토콜 설정을 만든다.
     *
     * @return the settings / 설정
     */
    Parameters toParameters() {
        Parameters p = new Parameters(trustStore());
        p.tls13 = tls13;
        p.tls12 = tls12;
        p.suites = suites.clone();
        p.groups = groups.clone();
        return p;
    }
}
