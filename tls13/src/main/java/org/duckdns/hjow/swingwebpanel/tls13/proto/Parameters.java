package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.security.SecureRandom;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.x509.PathValidator;
import org.duckdns.hjow.swingwebpanel.tls13.x509.TrustStore;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Cert;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Exception;

/**
 * The settings of one connection, as the protocol code sees them. This is an internal class: applications use
 * <code>Tls13Config</code>, which creates these settings and offers no way to weaken the certificate check. The replaceable random
 * source, clock and certificate check exist for the tests of this module (deterministic handshakes, the traces of RFC 8448).
 * Example: <code>Parameters p = new Parameters(TrustStore.mozilla());</code>.
 * <p>
 * 프로토콜 코드가 보는 연결 하나의 설정. 내부 클래스이다: 응용 프로그램은 <code>Tls13Config</code> 를 쓰며, 그것이 이 설정을 만들고 인증서 검사를
 * 약하게 할 방법을 제공하지 않는다. 바꿀 수 있는 난수원, 시계, 인증서 검사는 이 모듈의 시험(결정적 핸드셰이크, RFC 8448 추적)을 위해 있다.
 */
public final class Parameters {

    /**
     * The check of the certificates that a server presents.
     * <p>
     * 서버가 내놓은 인증서의 검사.
     */
    public interface CertificateChecker {

        /**
         * Validates the certificates and returns the path whose first element is the server certificate.
         * <p>
         * 인증서들을 검증하고, 첫 원소가 서버 인증서인 경로를 반환한다.
         *
         * @param chain the DER certificates as sent, the server certificate first / 보낸 그대로의 DER 인증서들. 서버 인증서가 먼저
         * @param host  the host that the client connects to / 클라이언트가 연결하는 호스트
         * @param now   the current time / 현재 시각
         * @return the validated path / 검증한 경로
         * @throws X509Exception if the certificates are rejected / 인증서를 거부하면
         */
        List<X509Cert> check(List<byte[]> chain, String host, long now) throws X509Exception;
    }

    /** The trusted roots. / 신뢰하는 루트. */
    public final TrustStore trustStore;

    /** The certificate check; the default validates the path against {@link #trustStore}. / 인증서 검사. 기본값은 {@link #trustStore} 로 경로를 검증한다. */
    public CertificateChecker checker;

    /** The random source. / 난수원. */
    public SecureRandom random = new SecureRandom();

    /** A fixed time for certificate checks in milliseconds since 1970, or -1 for the system clock. / 인증서 검사용 고정 시각(1970년부터의 밀리초). -1 이면 시스템 시계. */
    public long fixedTime = -1;

    /** True to offer TLS 1.3. / TLS 1.3 을 제안하려면 true. */
    public boolean tls13 = true;

    /** True to offer TLS 1.2. / TLS 1.2 를 제안하려면 true. */
    public boolean tls12 = true;

    /** The cipher suites to offer, in order of preference. / 제안할 암호 스위트(선호 순서). */
    public CipherSuite[] suites = CipherSuite.all();

    /** The key exchange groups to offer, in order of preference; the first one is sent as key share. / 제안할 키 교환 그룹(선호 순서). 첫째 것을 키 공유로 보낸다. */
    public int[] groups = { ClientHello.GROUP_X25519, ClientHello.GROUP_SECP256R1, ClientHello.GROUP_SECP384R1 };

    /**
     * Creates settings with the default certificate check against a trust store.
     * <p>
     * 신뢰 저장소로 검사하는 기본 인증서 검사를 가진 설정을 만든다.
     *
     * @param trustStore the trusted roots / 신뢰하는 루트
     */
    public Parameters(TrustStore trustStore) {
        this.trustStore = trustStore;
        final TrustStore store = trustStore;
        this.checker = new CertificateChecker() {
            /**
             * Validates the path against the trust store and checks the host name.
             * <p>
             * 신뢰 저장소로 경로를 검증하고 호스트 이름을 확인한다.
             *
             * @param chain the certificates / 인증서들
             * @param host  the host / 호스트
             * @param now   the time / 시각
             * @return the path / 경로
             * @throws X509Exception if rejected / 거부하면
             */
            public List<X509Cert> check(List<byte[]> chain, String host, long now) throws X509Exception {
                if (host == null) {
                    throw new X509Exception(X509Exception.Reason.NAME_MISMATCH, "no host name to check the certificate against");
                }
                return PathValidator.validate(chain, store, host, now);
            }
        };
    }

    /**
     * Returns the time for certificate checks.
     * <p>
     * 인증서 검사에 쓸 시각을 반환한다.
     *
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     */
    public long now() {
        return fixedTime >= 0 ? fixedTime : System.currentTimeMillis();
    }

    /**
     * Tells if a cipher suite is offered.
     * <p>
     * 암호 스위트를 제안하는지 알려 준다.
     *
     * @param suite the suite / 스위트
     * @return true if offered / 제안하면 true
     */
    public boolean offers(CipherSuite suite) {
        if (suite == null || (suite.isTls13() ? !tls13 : !tls12)) {
            return false;
        }
        // 설정한 스위트 목록에 있는지 본다.
        for (int i = 0; i < suites.length; i++) {
            if (suites[i] == suite) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells if a key exchange group is offered.
     * <p>
     * 키 교환 그룹을 제안하는지 알려 준다.
     *
     * @param group the group number / 그룹 번호
     * @return true if offered / 제안하면 true
     */
    public boolean offersGroup(int group) {
        // 설정한 그룹 목록에 있는지 본다.
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] == group) {
                return true;
            }
        }
        return false;
    }
}
