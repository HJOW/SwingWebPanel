package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

/**
 * The <code>SSLSession</code> of a connection of this module, as <code>HttpsURLConnection</code> and applications read it: protocol,
 * cipher suite and the certificates of the server. There is one session per socket; sessions are never cached or resumed, so the
 * session context is null. A session of a connection whose handshake failed is invalid, names the cipher suite
 * <code>SSL_NULL_WITH_NULL_NULL</code> and has no peer certificates, like the sessions of the JRE. Example:
 * <code>String suite = socket.getSession().getCipherSuite();</code>.
 * <p>
 * 이 모듈 연결의 <code>SSLSession</code>. <code>HttpsURLConnection</code> 과 응용 프로그램이 프로토콜, 암호 스위트, 서버의 인증서를 읽는다. 세션은
 * 소켓마다 하나이고, 캐시하거나 재개하지 않으므로 세션 컨텍스트는 null 이다. 핸드셰이크가 실패한 연결의 세션은 JRE 의 세션처럼 유효하지 않고, 암호
 * 스위트 이름이 <code>SSL_NULL_WITH_NULL_NULL</code> 이며, 상대 인증서가 없다.
 */
final class TlsSession implements SSLSession {

    /** The session identifier. / 세션 식별자. */
    private final byte[] id;

    /** The protocol name. / 프로토콜 이름. */
    private final String protocol;

    /** The cipher suite name. / 암호 스위트 이름. */
    private final String cipherSuite;

    /** The DER certificates of the server, or null for a failed handshake. / 서버의 DER 인증서들. 핸드셰이크가 실패했으면 null. */
    private final List<byte[]> chain;

    /** The host of the peer. / 상대의 호스트. */
    private final String host;

    /** The port of the peer. / 상대의 포트. */
    private final int port;

    /** The creation time. / 생성 시각. */
    private final long created = System.currentTimeMillis();

    /** The application values bound to the session. / 세션에 묶인 응용 값. */
    private final Map<String, Object> values = new HashMap<String, Object>();

    /** The certificates as JCA objects, created on first use. / JCA 객체로 만든 인증서들. 처음 쓸 때 만든다. */
    private Certificate[] certificates;

    /** False after {@link #invalidate()} or for a failed handshake. / {@link #invalidate()} 뒤나 실패한 핸드셰이크이면 false. */
    private boolean valid;

    /**
     * Creates a session.
     * <p>
     * 세션을 만든다.
     *
     * @param id          the session identifier / 세션 식별자
     * @param protocol    the protocol name / 프로토콜 이름
     * @param cipherSuite the cipher suite name / 암호 스위트 이름
     * @param chain       the DER certificates of the server, or null for a failed handshake / 서버의 DER 인증서들. 실패한 핸드셰이크이면 null
     * @param host        the peer host / 상대 호스트
     * @param port        the peer port / 상대 포트
     */
    TlsSession(byte[] id, String protocol, String cipherSuite, List<byte[]> chain, String host, int port) {
        this.id = id;
        this.protocol = protocol;
        this.cipherSuite = cipherSuite;
        this.chain = chain;
        this.host = host;
        this.port = port;
        this.valid = chain != null;
    }

    /**
     * Creates the session of a connection without a successful handshake.
     * <p>
     * 성공한 핸드셰이크가 없는 연결의 세션을 만든다.
     *
     * @param host the peer host / 상대 호스트
     * @param port the peer port / 상대 포트
     * @return the invalid session / 유효하지 않은 세션
     */
    static TlsSession none(String host, int port) {
        return new TlsSession(new byte[0], "NONE", "SSL_NULL_WITH_NULL_NULL", null, host, port);
    }

    /**
     * Returns the session identifier (the random value sent in the ClientHello; it cannot be used to resume).
     * <p>
     * 세션 식별자를 반환한다(ClientHello 에 보낸 난수 값. 재개에는 쓸 수 없다).
     *
     * @return the identifier / 식별자
     */
    public byte[] getId() {
        return id.clone();
    }

    /**
     * Returns null: sessions of this module are not cached.
     * <p>
     * null 을 반환한다: 이 모듈의 세션은 캐시하지 않는다.
     *
     * @return null
     */
    public SSLSessionContext getSessionContext() {
        return null;
    }

    /**
     * Returns the creation time.
     * <p>
     * 생성 시각을 반환한다.
     *
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     */
    public long getCreationTime() {
        return created;
    }

    /**
     * Returns the creation time: a session belongs to one connection.
     * <p>
     * 생성 시각을 반환한다: 세션은 연결 하나에 속한다.
     *
     * @return milliseconds since 1970 / 1970년부터의 밀리초
     */
    public long getLastAccessedTime() {
        return created;
    }

    /**
     * Marks the session invalid. The connection is not affected.
     * <p>
     * 세션을 유효하지 않게 표시한다. 연결에는 영향이 없다.
     */
    public synchronized void invalidate() {
        valid = false;
    }

    /**
     * Tells if the session is valid.
     * <p>
     * 세션이 유효한지 알려 준다.
     *
     * @return true if valid / 유효하면 true
     */
    public synchronized boolean isValid() {
        return valid;
    }

    /**
     * Binds an application value to the session.
     * <p>
     * 응용 값을 세션에 묶는다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     */
    public synchronized void putValue(String name, Object value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value must not be null");
        }
        values.put(name, value);
    }

    /**
     * Returns an application value.
     * <p>
     * 응용 값을 반환한다.
     *
     * @param name the name / 이름
     * @return the value or null / 값 또는 null
     */
    public synchronized Object getValue(String name) {
        return values.get(name);
    }

    /**
     * Removes an application value.
     * <p>
     * 응용 값을 지운다.
     *
     * @param name the name / 이름
     */
    public synchronized void removeValue(String name) {
        values.remove(name);
    }

    /**
     * Returns the names of the application values.
     * <p>
     * 응용 값의 이름들을 반환한다.
     *
     * @return the names / 이름들
     */
    public synchronized String[] getValueNames() {
        return values.keySet().toArray(new String[values.size()]);
    }

    /**
     * Returns the certificates of the server as it sent them, the server certificate first.
     * <p>
     * 서버가 보낸 그대로의 인증서들을 반환한다. 서버 인증서가 먼저이다.
     *
     * @return the certificates / 인증서들
     * @throws SSLPeerUnverifiedException if the handshake failed or the JRE cannot represent a certificate / 핸드셰이크가 실패했거나 JRE 가 인증서를 표현할 수 없으면
     */
    public synchronized Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        if (chain == null) {
            throw new SSLPeerUnverifiedException("peer not authenticated");
        }
        if (certificates == null) {
            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                Certificate[] out = new Certificate[chain.size()];
                // 인증서마다 JRE 의 객체로 만든다.
                for (int i = 0; i < out.length; i++) {
                    out[i] = factory.generateCertificate(new ByteArrayInputStream(chain.get(i)));
                }
                certificates = out;
            } catch (java.security.cert.CertificateException e) {
                throw new SSLPeerUnverifiedException("the JRE cannot represent the certificate of the server: " + e.getMessage());
            }
        }
        return certificates.clone();
    }

    /**
     * Returns null: this module never sends a client certificate.
     * <p>
     * null 을 반환한다: 이 모듈은 클라이언트 인증서를 보내지 않는다.
     *
     * @return null
     */
    public Certificate[] getLocalCertificates() {
        return null;
    }

    /**
     * Returns the certificates of the server in the old <code>javax.security.cert</code> form.
     * <p>
     * 서버의 인증서들을 옛 <code>javax.security.cert</code> 형식으로 반환한다.
     *
     * @return the certificates / 인증서들
     * @throws SSLPeerUnverifiedException if the handshake failed or the JRE cannot represent a certificate / 핸드셰이크가 실패했거나 JRE 가 인증서를 표현할 수 없으면
     */
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        if (chain == null) {
            throw new SSLPeerUnverifiedException("peer not authenticated");
        }
        try {
            javax.security.cert.X509Certificate[] out = new javax.security.cert.X509Certificate[chain.size()];
            // 인증서마다 옛 형식의 객체로 만든다.
            for (int i = 0; i < out.length; i++) {
                out[i] = javax.security.cert.X509Certificate.getInstance(chain.get(i));
            }
            return out;
        } catch (javax.security.cert.CertificateException e) {
            throw new SSLPeerUnverifiedException("the JRE cannot represent the certificate of the server: " + e.getMessage());
        }
    }

    /**
     * Returns the subject of the server certificate.
     * <p>
     * 서버 인증서의 주체를 반환한다.
     *
     * @return the principal / 주체
     * @throws SSLPeerUnverifiedException if the handshake failed / 핸드셰이크가 실패했으면
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return ((X509Certificate) getPeerCertificates()[0]).getSubjectX500Principal();
    }

    /**
     * Returns null: this module never sends a client certificate.
     * <p>
     * null 을 반환한다: 이 모듈은 클라이언트 인증서를 보내지 않는다.
     *
     * @return null
     */
    public Principal getLocalPrincipal() {
        return null;
    }

    /**
     * Returns the name of the cipher suite, for example <code>TLS_AES_128_GCM_SHA256</code>.
     * <p>
     * 암호 스위트의 이름을 반환한다. 예: <code>TLS_AES_128_GCM_SHA256</code>.
     *
     * @return the name / 이름
     */
    public String getCipherSuite() {
        return cipherSuite;
    }

    /**
     * Returns the protocol name: <code>TLSv1.3</code> or <code>TLSv1.2</code>.
     * <p>
     * 프로토콜 이름을 반환한다: <code>TLSv1.3</code> 또는 <code>TLSv1.2</code>.
     *
     * @return the name / 이름
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns the host of the peer as the application named it.
     * <p>
     * 응용 프로그램이 지정한 상대 호스트를 반환한다.
     *
     * @return the host / 호스트
     */
    public String getPeerHost() {
        return host;
    }

    /**
     * Returns the port of the peer.
     * <p>
     * 상대의 포트를 반환한다.
     *
     * @return the port / 포트
     */
    public int getPeerPort() {
        return port;
    }

    /**
     * Returns the largest size of a TLS record.
     * <p>
     * TLS 레코드의 최대 크기를 반환한다.
     *
     * @return the size in bytes / 바이트 단위 크기
     */
    public int getPacketBufferSize() {
        return 16384 + 2048 + 5;
    }

    /**
     * Returns the largest size of the application data of one record.
     * <p>
     * 레코드 하나에 담기는 응용 데이터의 최대 크기를 반환한다.
     *
     * @return the size in bytes / 바이트 단위 크기
     */
    public int getApplicationBufferSize() {
        return 16384;
    }
}
