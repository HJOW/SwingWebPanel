package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * The default SSL behaviour: keep the JRE default trust store and host name verification untouched, and only enable the
 * newest TLS protocol versions (TLSv1.3, TLSv1.2, TLSv1.1, TLSv1 - in that preference) that the running JRE supports, because
 * older JREs do not enable the newer ones by default. SSLv3 and SSLv2Hello are never enabled by this class. The change is made
 * per connection through a delegating socket factory; no JVM-wide state is modified.
 * <p>
 * JRE 6 (6u45) supports only TLSv1 (no TLS 1.2, no client SNI), so modern https sites may fail there; the fetch layer
 * then retries once over http as required by the project.
 * <p>
 * 기본 SSL 동작: JRE 기본 신뢰 저장소와 호스트 이름 검증은 건드리지 않고, 실행 중인 JRE 가 지원하는 최신 TLS 프로토콜 버전
 * (TLSv1.3, TLSv1.2, TLSv1.1, TLSv1 순의 선호)만 켠다. 오래된 JRE 는 새 버전을 기본으로 켜지 않기 때문이다. 이 클래스는
 * SSLv3 와 SSLv2Hello 를 켜지 않는다. 변경은 위임하는 소켓 팩토리로 연결마다 적용하며 JVM 전역 상태는 바꾸지 않는다.
 * <p>
 * JRE 6(6u45)은 TLSv1 만 지원하므로(TLS 1.2, 클라이언트 SNI 없음) 요즘 https 사이트는 실패할 수 있고, 그 경우 가져오기
 * 계층이 프로젝트 요구대로 http 로 한 번 재시도한다.
 */
public final class DefaultSslConfigurer implements SslConfigurer {

    /**
     * Shared stateless instance.
     * <p>
     * 상태가 없는 공유 인스턴스.
     */
    public static final DefaultSslConfigurer INSTANCE = new DefaultSslConfigurer();

    /**
     * Protocol versions in order of preference.
     * <p>
     * 선호 순서의 프로토콜 버전.
     */
    private static final String[] PREFERRED = { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" };

    /**
     * Creates the configurer.
     * <p>
     * 설정기를 만든다.
     */
    public DefaultSslConfigurer() {
    }

    /**
     * Installs a socket factory that enables the supported modern protocol versions on this connection only.
     * <p>
     * 이 연결에만 지원되는 최신 프로토콜 버전을 켜는 소켓 팩토리를 설치한다.
     *
     * @param connection https connection / https 연결
     * @throws IOException never thrown by this implementation / 이 구현은 던지지 않는다
     */
    public void configureSsl(HttpsURLConnection connection) throws IOException {
        connection.setSSLSocketFactory(new ProtocolEnablingFactory(connection.getSSLSocketFactory()));
    }

    /**
     * Returns the preferred protocols that a socket supports, newest first.
     * <p>
     * 소켓이 지원하는 선호 프로토콜을 최신 순으로 돌려준다.
     *
     * @param supported protocols supported by the socket / 소켓이 지원하는 프로토콜
     * @return protocols to enable, or null when none of the preferred ones is supported / 켤 프로토콜. 선호 프로토콜이 하나도 없으면 null
     */
    static String[] select(String[] supported) {
        java.util.List<String> chosen = new java.util.ArrayList<String>();
        // 선호 목록을 순서대로 보며 지원되는 것만 고른다.
        for (int i = 0; i < PREFERRED.length; i++) {
            for (int j = 0; j < supported.length; j++) {
                if (PREFERRED[i].equals(supported[j])) {
                    chosen.add(PREFERRED[i]);
                    break;
                }
            }
        }
        return chosen.isEmpty() ? null : chosen.toArray(new String[chosen.size()]);
    }

    /**
     * Socket factory that delegates to another one and then restricts the enabled protocol versions.
     * <p>
     * 다른 팩토리에 위임한 뒤 켜진 프로토콜 버전을 제한하는 소켓 팩토리.
     */
    private static final class ProtocolEnablingFactory extends SSLSocketFactory {
        /**
         * The factory that really creates sockets.
         * <p>
         * 실제로 소켓을 만드는 팩토리.
         */
        private final SSLSocketFactory delegate;

        /**
         * Creates the wrapper.
         * <p>
         * 감싸는 팩토리를 만든다.
         *
         * @param delegate factory to wrap / 감쌀 팩토리
         */
        ProtocolEnablingFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        /**
         * Applies the protocol selection to a freshly created socket.
         * <p>
         * 새로 만든 소켓에 프로토콜 선택을 적용한다.
         *
         * @param s socket from the delegate / 위임 대상이 만든 소켓
         * @return the same socket / 같은 소켓
         */
        private Socket prepare(Socket s) {
            if (s instanceof SSLSocket) {
                SSLSocket ssl = (SSLSocket) s;
                String[] enabled = select(ssl.getSupportedProtocols());
                if (enabled != null) {
                    ssl.setEnabledProtocols(enabled);
                }
            }
            return s;
        }

        /**
         * Returns the default cipher suites of the delegate.
         * <p>
         * 위임 대상의 기본 암호 스위트를 돌려준다.
         *
         * @return cipher suites / 암호 스위트
         */
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        /**
         * Returns the supported cipher suites of the delegate.
         * <p>
         * 위임 대상이 지원하는 암호 스위트를 돌려준다.
         *
         * @return cipher suites / 암호 스위트
         */
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        /**
         * Creates an unconnected socket.
         * <p>
         * 연결되지 않은 소켓을 만든다.
         *
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket() throws IOException {
            return prepare(delegate.createSocket());
        }

        /**
         * Layers TLS over an existing socket.
         * <p>
         * 기존 소켓 위에 TLS 를 얹는다.
         *
         * @param s         existing socket / 기존 소켓
         * @param host      host name / 호스트 이름
         * @param port      port / 포트
         * @param autoClose close the underlying socket / 아래 소켓을 같이 닫을지
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return prepare(delegate.createSocket(s, host, port, autoClose));
        }

        /**
         * Creates a connected socket.
         * <p>
         * 연결된 소켓을 만든다.
         *
         * @param host host name / 호스트 이름
         * @param port port / 포트
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket(String host, int port) throws IOException {
            return prepare(delegate.createSocket(host, port));
        }

        /**
         * Creates a connected socket bound to a local address.
         * <p>
         * 지역 주소에 묶인 연결된 소켓을 만든다.
         *
         * @param host      host name / 호스트 이름
         * @param port      port / 포트
         * @param localHost local address / 지역 주소
         * @param localPort local port / 지역 포트
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return prepare(delegate.createSocket(host, port, localHost, localPort));
        }

        /**
         * Creates a connected socket for an address.
         * <p>
         * 주소로 연결된 소켓을 만든다.
         *
         * @param host address / 주소
         * @param port port / 포트
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return prepare(delegate.createSocket(host, port));
        }

        /**
         * Creates a connected socket for an address bound to a local address.
         * <p>
         * 지역 주소에 묶여 주소로 연결된 소켓을 만든다.
         *
         * @param address   address / 주소
         * @param port      port / 포트
         * @param localAddr local address / 지역 주소
         * @param localPort local port / 지역 포트
         * @return socket / 소켓
         * @throws IOException on failure / 실패 시
         */
        public Socket createSocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
            return prepare(delegate.createSocket(address, port, localAddr, localPort));
        }
    }
}
