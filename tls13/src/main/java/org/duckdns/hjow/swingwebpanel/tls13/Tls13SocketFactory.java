package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ssl.SSLSocketFactory;

/**
 * The entry point of the module: an <code>SSLSocketFactory</code> whose sockets speak TLS 1.3 and TLS 1.2 with the module's own
 * implementation, on any JRE from Java 6 on. It replaces the TLS of the JRE for the connections that use it and changes no JVM wide
 * setting. The certificate chain and the host name of the server are always checked against the trust store of the
 * {@link Tls13Config}. Usage with an <code>HttpsURLConnection</code>:
 * <pre>
 * HttpsURLConnection c = (HttpsURLConnection) new URL("https://www.example.com/").openConnection();
 * c.setSSLSocketFactory(Tls13SocketFactory.getInstance());
 * InputStream in = c.getInputStream();
 * </pre>
 * and with an own socket:
 * <pre>
 * Socket plain = new Socket("www.example.com", 443);
 * SSLSocket tls = (SSLSocket) Tls13SocketFactory.getInstance().createSocket(plain, "www.example.com", 443, true);
 * tls.startHandshake();
 * </pre>
 * The host name must be the name that the user asked for (it is sent as server name and compared with the certificate), so the
 * variants that take a host name are the ones to use. {@link #createSocket()} without arguments is refused on purpose: the JRE's
 * <code>HttpsURLConnection</code> then connects a plain socket itself and calls
 * {@link #createSocket(Socket, String, int, boolean)} with the host name.
 * <p>
 * 모듈의 진입점: 소켓이 모듈 자체 구현으로 TLS 1.3 과 TLS 1.2 를 말하는 <code>SSLSocketFactory</code>. Java 6 이상의 어느 JRE 에서나 동작한다. 이
 * 팩터리를 쓰는 연결에서만 JRE 의 TLS 를 대신하며 JVM 전역 설정을 바꾸지 않는다. 서버의 인증서 사슬과 호스트 이름은 항상 {@link Tls13Config} 의 신뢰
 * 저장소로 검사한다. 호스트 이름은 사용자가 요청한 이름이어야 하므로(서버 이름으로 보내고 인증서와 비교한다) 호스트 이름을 받는 메서드를 써야 한다.
 * 인수 없는 {@link #createSocket()} 은 일부러 거절한다: 그러면 JRE 의 <code>HttpsURLConnection</code> 이 일반 소켓을 직접 연결한 뒤 호스트 이름과
 * 함께 {@link #createSocket(Socket, String, int, boolean)} 를 부른다.
 */
public final class Tls13SocketFactory extends SSLSocketFactory {

    /** The shared factory with the default configuration, created on first use. / 기본 설정의 공유 팩터리. 처음 쓸 때 만든다. */
    private static Tls13SocketFactory shared;

    /** The settings of the sockets of this factory. / 이 팩터리가 만드는 소켓의 설정. */
    private final Tls13Config config;

    /**
     * Creates a factory with the default configuration (bundled Mozilla root list, TLS 1.3 and TLS 1.2).
     * <p>
     * 기본 설정(내장 Mozilla 루트 목록, TLS 1.3 과 TLS 1.2)의 팩터리를 만든다.
     */
    public Tls13SocketFactory() {
        this(Tls13Config.defaults());
    }

    /**
     * Creates a factory with the given configuration.
     * <p>
     * 주어진 설정의 팩터리를 만든다.
     *
     * @param config the settings / 설정
     */
    public Tls13SocketFactory(Tls13Config config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /**
     * Returns the shared factory with the default configuration. Unlike <code>SSLSocketFactory.getDefault()</code> this is not a JVM
     * wide default: only callers that install this factory use it.
     * <p>
     * 기본 설정의 공유 팩터리를 반환한다. <code>SSLSocketFactory.getDefault()</code> 와 달리 JVM 전역 기본값이 아니다: 이 팩터리를 설치한 호출자만 쓴다.
     *
     * @return the factory / 팩터리
     */
    public static synchronized Tls13SocketFactory getInstance() {
        if (shared == null) {
            shared = new Tls13SocketFactory();
        }
        return shared;
    }

    /**
     * Returns the settings of this factory.
     * <p>
     * 이 팩터리의 설정을 반환한다.
     *
     * @return the settings / 설정
     */
    public Tls13Config config() {
        return config;
    }

    /**
     * Returns the names of the cipher suites that sockets of this factory offer.
     * <p>
     * 이 팩터리의 소켓이 제안하는 암호 스위트 이름들을 반환한다.
     *
     * @return the names / 이름들
     */
    public String[] getDefaultCipherSuites() {
        return config.cipherSuites();
    }

    /**
     * Returns the names of all cipher suites of the module.
     * <p>
     * 모듈의 모든 암호 스위트 이름을 반환한다.
     *
     * @return the names / 이름들
     */
    public String[] getSupportedCipherSuites() {
        return Tls13Config.defaults().cipherSuites();
    }

    /**
     * Refused: a socket of this module needs the host name before it connects. The exception has an
     * <code>UnsupportedOperationException</code> as cause, which is how a socket factory tells the JRE that it has no unconnected
     * sockets; <code>HttpsURLConnection</code> then uses {@link #createSocket(Socket, String, int, boolean)}.
     * <p>
     * 거절한다: 이 모듈의 소켓은 연결하기 전에 호스트 이름이 필요하다. 예외의 원인은 <code>UnsupportedOperationException</code> 이며, 소켓 팩터리가
     * 연결되지 않은 소켓을 만들 수 없음을 JRE 에 알리는 방법이다. 그러면 <code>HttpsURLConnection</code> 은
     * {@link #createSocket(Socket, String, int, boolean)} 를 쓴다.
     *
     * @return never / 반환하지 않는다
     * @throws IOException always / 항상
     */
    public Socket createSocket() throws IOException {
        SocketException e = new SocketException("Unconnected sockets not implemented");
        e.initCause(new UnsupportedOperationException("the TLS module needs the host name before it connects"));
        throw e;
    }

    /**
     * Layers TLS over a connected socket. The handshake starts with <code>startHandshake()</code> or the first read or write.
     * <p>
     * 연결된 소켓 위에 TLS 를 얹는다. 핸드셰이크는 <code>startHandshake()</code> 또는 첫 읽기·쓰기에서 시작한다.
     *
     * @param s         the connected socket / 연결된 소켓
     * @param host      the host name that the user asked for; sent as server name and compared with the certificate / 사용자가 요청한 호스트 이름. 서버 이름으로 보내고 인증서와 비교한다
     * @param port      the port of the server / 서버의 포트
     * @param autoClose true to close <code>s</code> when the returned socket is closed / 반환한 소켓을 닫을 때 <code>s</code> 도 닫으려면 true
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the socket is not connected or the host is missing / 소켓이 연결되지 않았거나 호스트가 없으면
     */
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        if (host == null || host.length() == 0) {
            throw new SocketException("the TLS module needs the host name of the server");
        }
        return new TlsSocket(s, host, port, autoClose, config.toParameters());
    }

    /**
     * Connects to a server and layers TLS over the connection.
     * <p>
     * 서버에 연결하고 그 위에 TLS 를 얹는다.
     *
     * @param host the host name / 호스트 이름
     * @param port the port / 포트
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    public Socket createSocket(String host, int port) throws IOException {
        return connect(host, new InetSocketAddress(host, port), null, 0);
    }

    /**
     * Connects to a server from a local address and layers TLS over the connection.
     * <p>
     * 지역 주소에서 서버에 연결하고 그 위에 TLS 를 얹는다.
     *
     * @param host      the host name / 호스트 이름
     * @param port      the port / 포트
     * @param localHost the local address / 지역 주소
     * @param localPort the local port / 지역 포트
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return connect(host, new InetSocketAddress(host, port), localHost, localPort);
    }

    /**
     * Connects to an address and layers TLS over the connection. Only the address is known here, so the certificate of the server
     * must name that IP address; no name is looked up, because a reverse lookup could be forged. Use a variant with a host name for
     * ordinary servers.
     * <p>
     * 주소에 연결하고 그 위에 TLS 를 얹는다. 여기서는 주소만 알 수 있으므로 서버의 인증서에 그 IP 주소가 있어야 한다. 역방향 조회는 위조될 수 있어 이름을
     * 조회하지 않는다. 보통의 서버에는 호스트 이름을 받는 메서드를 쓴다.
     *
     * @param host the address / 주소
     * @param port the port / 포트
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return connect(host.getHostAddress(), new InetSocketAddress(host, port), null, 0);
    }

    /**
     * Connects to an address from a local address and layers TLS over the connection; see {@link #createSocket(InetAddress, int)}.
     * <p>
     * 지역 주소에서 주소에 연결하고 그 위에 TLS 를 얹는다. {@link #createSocket(InetAddress, int)} 참고.
     *
     * @param address      the address / 주소
     * @param port         the port / 포트
     * @param localAddress the local address / 지역 주소
     * @param localPort    the local port / 지역 포트
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return connect(address.getHostAddress(), new InetSocketAddress(address, port), localAddress, localPort);
    }

    /**
     * Connects a plain socket and wraps it; the plain socket is closed if wrapping fails.
     * <p>
     * 일반 소켓을 연결하고 감싼다. 감싸는 데 실패하면 일반 소켓을 닫는다.
     *
     * @param host      the host for the server name and the certificate check / 서버 이름과 인증서 검사에 쓸 호스트
     * @param remote    the address to connect to / 연결할 주소
     * @param localHost the local address or null / 지역 주소 또는 null
     * @param localPort the local port / 지역 포트
     * @return the TLS socket / TLS 소켓
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    private Socket connect(String host, InetSocketAddress remote, InetAddress localHost, int localPort) throws IOException {
        Socket plain = new Socket();
        boolean ok = false;
        try {
            if (localHost != null) {
                plain.bind(new InetSocketAddress(localHost, localPort));
            }
            plain.connect(remote);
            Socket tls = createSocket(plain, host, remote.getPort(), true);
            ok = true;
            return tls;
        } finally {
            // 실패했으면 연결한 소켓을 남기지 않는다.
            if (!ok) {
                plain.close();
            }
        }
    }
}
