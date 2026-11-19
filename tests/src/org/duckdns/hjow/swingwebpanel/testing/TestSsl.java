package org.duckdns.hjow.swingwebpanel.testing;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Test-only TLS material. It loads <code>/test-keystore.jks</code> (a self-signed 2048-bit RSA certificate for CN=localhost,
 * password <code>changeit</code>) from the test class path and builds {@link SSLContext} objects for the HTTPS test server
 * and for test clients. The trust applies only to contexts created here; nothing in the JVM-wide trust, default
 * <code>SSLContext</code>, <code>HttpsURLConnection</code> defaults or host name verifiers is changed.
 * Clients must connect to the host name <code>localhost</code> so that the certificate matches.
 * <p>
 * 시험 전용 TLS 자료. 시험 클래스 경로의 <code>/test-keystore.jks</code>(CN=localhost, 비밀번호 <code>changeit</code> 인
 * 자체 서명 2048비트 RSA 인증서)를 읽어 HTTPS 시험 서버와 시험 클라이언트가 쓸 {@link SSLContext} 를 만든다.
 * 신뢰는 여기서 만든 컨텍스트에만 적용되며 JVM 전역 신뢰 설정, 기본 <code>SSLContext</code>, <code>HttpsURLConnection</code>
 * 기본값, 호스트 이름 검증기는 바꾸지 않는다. 인증서가 맞도록 클라이언트는 호스트 이름 <code>localhost</code> 로 접속해야 한다.
 */
public final class TestSsl {

    /**
     * Class path location of the test key store.
     * <p>
     * 시험 키 저장소의 클래스 경로 위치.
     */
    public static final String KEYSTORE_RESOURCE = "/test-keystore.jks";

    /**
     * Password of the test key store and its key (public, test only).
     * <p>
     * 시험 키 저장소와 키의 비밀번호(공개, 시험 전용).
     */
    public static final String PASSWORD = "changeit";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TestSsl() {
    }

    /**
     * Loads the test key store from the class path.
     * <p>
     * 클래스 경로에서 시험 키 저장소를 읽는다.
     *
     * @return the key store / 키 저장소
     * @throws Exception when the resource is missing or unreadable / 자원이 없거나 읽을 수 없을 때
     */
    public static KeyStore loadKeyStore() throws Exception {
        InputStream in = TestSsl.class.getResourceAsStream(KEYSTORE_RESOURCE);
        if (in == null) {
            throw new IllegalStateException("missing test resource " + KEYSTORE_RESOURCE);
        }
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, PASSWORD.toCharArray());
            return ks;
        } finally {
            in.close();
        }
    }

    /**
     * Creates the server side context holding the test private key.
     * <p>
     * 시험 개인 키를 가진 서버 쪽 컨텍스트를 만든다.
     *
     * @return server context / 서버 컨텍스트
     * @throws Exception on failure / 실패 시
     */
    public static SSLContext serverContext() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), PASSWORD.toCharArray());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * Creates a client context that trusts only the test certificate.
     * <p>
     * 시험 인증서만 신뢰하는 클라이언트 컨텍스트를 만든다.
     *
     * @return client context / 클라이언트 컨텍스트
     * @throws Exception on failure / 실패 시
     */
    public static SSLContext clientContext() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(loadKeyStore());
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }
}
