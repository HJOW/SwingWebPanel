package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.duckdns.hjow.swingwebpanel.tls13.proto.CipherSuite;
import org.duckdns.hjow.swingwebpanel.tls13.proto.Connection;
import org.duckdns.hjow.swingwebpanel.tls13.proto.Parameters;

/**
 * The <code>SSLSocket</code> of this module: a TLS 1.3 or TLS 1.2 client connection layered over an already connected socket. It is
 * created by {@link Tls13SocketFactory} and used through the <code>SSLSocket</code> and <code>Socket</code> API; the socket options
 * (timeouts, addresses and so on) are those of the underlying socket. The handshake starts with {@link #startHandshake()} or with the
 * first read or write. Client mode is the only mode. Example:
 * <pre>
 * SSLSocket s = (SSLSocket) factory.createSocket(plainSocket, "www.example.com", 443, true);
 * s.startHandshake();
 * s.getOutputStream().write(request);
 * </pre>
 * <p>
 * 이 모듈의 <code>SSLSocket</code>: 이미 연결된 소켓 위에 얹은 TLS 1.3 또는 TLS 1.2 클라이언트 연결. {@link Tls13SocketFactory} 가 만들고
 * <code>SSLSocket</code>·<code>Socket</code> API 로 쓴다. 소켓 옵션(제한 시간, 주소 등)은 아래 소켓의 것이다. 핸드셰이크는
 * {@link #startHandshake()} 또는 첫 읽기·쓰기에서 시작한다. 클라이언트 모드만 있다.
 */
final class TlsSocket extends SSLSocket {

    /** The connected socket below. / 아래의 연결된 소켓. */
    private final Socket base;

    /** The host that the application named, used for the server name and the certificate check. / 응용 프로그램이 지정한 호스트. 서버 이름과 인증서 검사에 쓴다. */
    private final String host;

    /** The port of the peer. / 상대의 포트. */
    private final int port;

    /** True if closing this socket closes the socket below. / 이 소켓을 닫을 때 아래 소켓도 닫으면 true. */
    private final boolean autoClose;

    /** The settings; they can be changed until the handshake starts. / 설정. 핸드셰이크가 시작되기 전까지 바꿀 수 있다. */
    private final Parameters parameters;

    /** The protocol connection, created when the handshake starts. / 프로토콜 연결. 핸드셰이크가 시작될 때 만든다. */
    private Connection connection;

    /** The session, created after the handshake. / 세션. 핸드셰이크 뒤에 만든다. */
    private TlsSession session;

    /** The listeners to tell about the end of the handshake. / 핸드셰이크의 끝을 알릴 리스너들. */
    private final List<HandshakeCompletedListener> listeners = new ArrayList<HandshakeCompletedListener>();

    /** True once the listeners were told. / 리스너에게 알렸으면 true. */
    private boolean notified;

    /** True once this socket was closed. / 이 소켓을 닫았으면 true. */
    private volatile boolean closed;

    /** The stream of decrypted application data. / 복호화한 응용 데이터의 스트림. */
    private final InputStream input = new InputStream() {
        /**
         * Reads one byte of application data.
         * <p>
         * 응용 데이터 한 바이트를 읽는다.
         *
         * @return the byte or -1 / 바이트 또는 -1
         * @throws IOException on a failure / 실패 시
         */
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xff;
        }

        /**
         * Reads application data.
         * <p>
         * 응용 데이터를 읽는다.
         *
         * @param b   the destination / 대상
         * @param off the start / 시작
         * @param len the largest number of bytes / 최대 바이트 수
         * @return the number of bytes or -1 / 바이트 수 또는 -1
         * @throws IOException on a failure / 실패 시
         */
        public int read(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            return connection().read(b, off, len);
        }

        /**
         * Returns the number of decrypted bytes that are ready.
         * <p>
         * 준비된 복호화 바이트 수를 반환한다.
         *
         * @return the count / 개수
         */
        public int available() {
            Connection c = current();
            return c == null ? 0 : c.available();
        }

        /**
         * Closes the socket.
         * <p>
         * 소켓을 닫는다.
         *
         * @throws IOException on a failure / 실패 시
         */
        public void close() throws IOException {
            TlsSocket.this.close();
        }
    };

    /** The stream that encrypts application data. / 응용 데이터를 암호화하는 스트림. */
    private final OutputStream output = new OutputStream() {
        /**
         * Writes one byte of application data.
         * <p>
         * 응용 데이터 한 바이트를 쓴다.
         *
         * @param b the byte / 바이트
         * @throws IOException on a failure / 실패 시
         */
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        /**
         * Writes application data.
         * <p>
         * 응용 데이터를 쓴다.
         *
         * @param b   the data / 데이터
         * @param off the start / 시작
         * @param len the length / 길이
         * @throws IOException on a failure / 실패 시
         */
        public void write(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            connection().write(b, off, len);
        }

        /**
         * Closes the socket.
         * <p>
         * 소켓을 닫는다.
         *
         * @throws IOException on a failure / 실패 시
         */
        public void close() throws IOException {
            TlsSocket.this.close();
        }
    };

    /**
     * Layers TLS over a connected socket.
     * <p>
     * 연결된 소켓 위에 TLS 를 얹는다.
     *
     * @param base       the connected socket / 연결된 소켓
     * @param host       the host that the application named / 응용 프로그램이 지정한 호스트
     * @param port       the port of the peer / 상대의 포트
     * @param autoClose  true to close the socket below with this one / 이 소켓과 함께 아래 소켓을 닫으려면 true
     * @param parameters the settings of this connection / 이 연결의 설정
     * @throws IOException if the socket is not connected / 소켓이 연결되지 않았으면
     */
    TlsSocket(Socket base, String host, int port, boolean autoClose, Parameters parameters) throws IOException {
        super();
        if (base == null || !base.isConnected()) {
            throw new SocketException("the underlying socket is not connected");
        }
        this.base = base;
        this.host = host;
        this.port = port;
        this.autoClose = autoClose;
        this.parameters = parameters;
    }

    /**
     * Returns the connection if it exists.
     * <p>
     * 연결이 있으면 반환한다.
     *
     * @return the connection or null / 연결 또는 null
     */
    private synchronized Connection current() {
        return connection;
    }

    /**
     * Returns the connection, creating it on first use; from then on the settings are fixed.
     * <p>
     * 연결을 반환한다. 처음 쓸 때 만들며, 그 뒤로 설정은 고정된다.
     *
     * @return the connection / 연결
     * @throws IOException if the socket is closed or the streams cannot be opened / 소켓이 닫혔거나 스트림을 열 수 없으면
     */
    private synchronized Connection connection() throws IOException {
        if (closed) {
            throw new SocketException("Socket is closed");
        }
        if (connection == null) {
            connection = new Connection(base.getInputStream(), base.getOutputStream(), parameters, host);
        }
        return connection;
    }

    /**
     * Runs the handshake (once) and tells the listeners.
     * <p>
     * 핸드셰이크를 (한 번) 수행하고 리스너에게 알린다.
     *
     * @throws IOException    on a network failure / 네트워크 실패 시
     * @throws Tls13Exception if the handshake fails / 핸드셰이크가 실패하면
     */
    public void startHandshake() throws IOException {
        Connection c = connection();
        c.handshake();
        List<HandshakeCompletedListener> toTell;
        SSLSession s;
        synchronized (this) {
            if (notified) {
                return;
            }
            notified = true;
            s = sessionOf(c);
            toTell = new ArrayList<HandshakeCompletedListener>(listeners);
        }
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(this, s);
        // 등록된 리스너에게 차례로 알린다.
        for (int i = 0; i < toTell.size(); i++) {
            toTell.get(i).handshakeCompleted(event);
        }
    }

    /**
     * Returns the session of an established connection, creating it on first use.
     * <p>
     * 성립한 연결의 세션을 반환한다. 처음 쓸 때 만든다.
     *
     * @param c the established connection / 성립한 연결
     * @return the session / 세션
     */
    private synchronized TlsSession sessionOf(Connection c) {
        if (session == null) {
            session = new TlsSession(c.sessionId(), c.protocol(), c.suite().name(), c.peerChain(), host, port);
        }
        return session;
    }

    /**
     * Returns the session; the handshake is run first if needed. If the handshake fails, an invalid session is returned, as the
     * <code>SSLSocket</code> contract says.
     * <p>
     * 세션을 반환한다. 필요하면 먼저 핸드셰이크를 수행한다. 핸드셰이크가 실패하면 <code>SSLSocket</code> 계약대로 유효하지 않은 세션을 반환한다.
     *
     * @return the session / 세션
     */
    public SSLSession getSession() {
        try {
            startHandshake();
            return sessionOf(connection());
        } catch (IOException e) {
            return TlsSession.none(host, port);
        }
    }

    /**
     * Returns the stream of decrypted application data.
     * <p>
     * 복호화한 응용 데이터의 스트림을 반환한다.
     *
     * @return the stream / 스트림
     * @throws IOException if the socket is closed / 소켓이 닫혔으면
     */
    public InputStream getInputStream() throws IOException {
        if (closed) {
            throw new SocketException("Socket is closed");
        }
        return input;
    }

    /**
     * Returns the stream that encrypts application data.
     * <p>
     * 응용 데이터를 암호화하는 스트림을 반환한다.
     *
     * @return the stream / 스트림
     * @throws IOException if the socket is closed / 소켓이 닫혔으면
     */
    public OutputStream getOutputStream() throws IOException {
        if (closed) {
            throw new SocketException("Socket is closed");
        }
        return output;
    }

    /**
     * Closes the connection: sends close_notify if that is possible without waiting, then closes the socket below if this socket owns
     * it. A thread that is blocked in a read or write of this socket is released by the closing of the socket below.
     * <p>
     * 연결을 닫는다: 기다리지 않고 가능하면 close_notify 를 보낸 뒤, 이 소켓이 아래 소켓을 소유하면 그것을 닫는다. 이 소켓의 읽기나 쓰기에 막혀 있던
     * 스레드는 아래 소켓이 닫히면서 풀려난다.
     *
     * @throws IOException if the socket below cannot be closed / 아래 소켓을 닫을 수 없으면
     */
    public void close() throws IOException {
        Connection c;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            c = connection;
        }
        try {
            if (c != null) {
                c.close();
                c.destroy();
            }
        } finally {
            if (autoClose) {
                base.close();
            }
        }
    }

    /**
     * Returns the names of the cipher suites of this module.
     * <p>
     * 이 모듈의 암호 스위트 이름들을 반환한다.
     *
     * @return the names / 이름들
     */
    public String[] getSupportedCipherSuites() {
        return Tls13Config.defaults().cipherSuites();
    }

    /**
     * Returns the names of the cipher suites that this socket offers.
     * <p>
     * 이 소켓이 제안하는 암호 스위트 이름들을 반환한다.
     *
     * @return the names / 이름들
     */
    public synchronized String[] getEnabledCipherSuites() {
        List<String> out = new ArrayList<String>();
        // 켜 둔 버전의 스위트만 넣는다.
        for (int i = 0; i < parameters.suites.length; i++) {
            if (parameters.offers(parameters.suites[i])) {
                out.add(parameters.suites[i].name());
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * Sets the cipher suites to offer; has no effect once the handshake started.
     * <p>
     * 제안할 암호 스위트를 정한다. 핸드셰이크가 시작된 뒤에는 효과가 없다.
     *
     * @param suites names of suites of this module / 이 모듈의 스위트 이름들
     * @throws IllegalArgumentException for a suite that the module does not have / 모듈에 없는 스위트이면
     */
    public synchronized void setEnabledCipherSuites(String[] suites) {
        if (suites == null) {
            throw new IllegalArgumentException("cipher suites must not be null");
        }
        CipherSuite[] all = CipherSuite.all();
        List<CipherSuite> chosen = new ArrayList<CipherSuite>();
        // 이름마다 모듈의 스위트를 찾는다.
        for (int i = 0; i < suites.length; i++) {
            CipherSuite found = null;
            // 모듈의 스위트 목록에서 이름이 같은 것을 찾는다.
            for (int k = 0; k < all.length; k++) {
                if (all[k].name().equals(suites[i])) {
                    found = all[k];
                }
            }
            if (found == null) {
                throw new IllegalArgumentException("unsupported cipher suite " + suites[i]);
            }
            chosen.add(found);
        }
        if (connection == null) {
            parameters.suites = chosen.toArray(new CipherSuite[chosen.size()]);
        }
    }

    /**
     * Returns the protocol versions of this module.
     * <p>
     * 이 모듈의 프로토콜 버전들을 반환한다.
     *
     * @return <code>TLSv1.3</code> and <code>TLSv1.2</code>
     */
    public String[] getSupportedProtocols() {
        return new String[] { "TLSv1.3", "TLSv1.2" };
    }

    /**
     * Returns the protocol versions that this socket offers.
     * <p>
     * 이 소켓이 제안하는 프로토콜 버전들을 반환한다.
     *
     * @return the names / 이름들
     */
    public synchronized String[] getEnabledProtocols() {
        return parameters.tls13 && parameters.tls12 ? new String[] { "TLSv1.3", "TLSv1.2" }
                : parameters.tls13 ? new String[] { "TLSv1.3" } : new String[] { "TLSv1.2" };
    }

    /**
     * Sets the protocol versions to offer; has no effect once the handshake started.
     * <p>
     * 제안할 프로토콜 버전을 정한다. 핸드셰이크가 시작된 뒤에는 효과가 없다.
     *
     * @param protocols <code>TLSv1.3</code> and/or <code>TLSv1.2</code> / <code>TLSv1.3</code>, <code>TLSv1.2</code>
     * @throws IllegalArgumentException for another name or an empty list / 다른 이름이거나 목록이 비었으면
     */
    public synchronized void setEnabledProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("protocols must not be null");
        }
        boolean v13 = false;
        boolean v12 = false;
        // 이름마다 버전을 켠다.
        for (int i = 0; i < protocols.length; i++) {
            if ("TLSv1.3".equals(protocols[i])) {
                v13 = true;
            } else if ("TLSv1.2".equals(protocols[i])) {
                v12 = true;
            } else {
                throw new IllegalArgumentException("unsupported protocol " + protocols[i]);
            }
        }
        if (!v13 && !v12) {
            throw new IllegalArgumentException("no protocol");
        }
        if (connection == null) {
            parameters.tls13 = v13;
            parameters.tls12 = v12;
        }
    }

    /**
     * Registers a listener for the end of the handshake.
     * <p>
     * 핸드셰이크의 끝을 듣는 리스너를 등록한다.
     *
     * @param listener the listener / 리스너
     */
    public synchronized void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     * <p>
     * 리스너를 제거한다.
     *
     * @param listener the listener / 리스너
     */
    public synchronized void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (listener == null || !listeners.remove(listener)) {
            throw new IllegalArgumentException("listener not registered");
        }
    }

    /**
     * Accepts only client mode.
     * <p>
     * 클라이언트 모드만 받아들인다.
     *
     * @param mode must be true / true 여야 한다
     */
    public void setUseClientMode(boolean mode) {
        if (!mode) {
            throw new IllegalArgumentException("this module is a TLS client only");
        }
    }

    /**
     * Returns true: the socket is always in client mode.
     * <p>
     * true 를 반환한다: 소켓은 항상 클라이언트 모드이다.
     *
     * @return true
     */
    public boolean getUseClientMode() {
        return true;
    }

    /**
     * Ignored: client authentication is a server side setting.
     * <p>
     * 무시한다: 클라이언트 인증은 서버 쪽 설정이다.
     *
     * @param need ignored / 무시한다
     */
    public void setNeedClientAuth(boolean need) {
        // 클라이언트 전용이므로 할 일이 없다.
    }

    /**
     * Returns false.
     * <p>
     * false 를 반환한다.
     *
     * @return false
     */
    public boolean getNeedClientAuth() {
        return false;
    }

    /**
     * Ignored: client authentication is a server side setting.
     * <p>
     * 무시한다: 클라이언트 인증은 서버 쪽 설정이다.
     *
     * @param want ignored / 무시한다
     */
    public void setWantClientAuth(boolean want) {
        // 클라이언트 전용이므로 할 일이 없다.
    }

    /**
     * Returns false.
     * <p>
     * false 를 반환한다.
     *
     * @return false
     */
    public boolean getWantClientAuth() {
        return false;
    }

    /**
     * Ignored: every connection has its own new session.
     * <p>
     * 무시한다: 연결마다 새 세션을 가진다.
     *
     * @param flag ignored / 무시한다
     */
    public void setEnableSessionCreation(boolean flag) {
        // 세션 재개가 없으므로 항상 새 세션이다.
    }

    /**
     * Returns true: every connection creates its session.
     * <p>
     * true 를 반환한다: 연결마다 세션을 만든다.
     *
     * @return true
     */
    public boolean getEnableSessionCreation() {
        return true;
    }

    /**
     * Refused: the socket is layered over a connected socket.
     * <p>
     * 거절한다: 이 소켓은 연결된 소켓 위에 얹혀 있다.
     *
     * @param endpoint ignored / 무시한다
     * @param timeout  ignored / 무시한다
     * @throws IOException always / 항상
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        throw new SocketException("already connected");
    }

    /**
     * Refused: the socket is layered over a connected socket.
     * <p>
     * 거절한다: 이 소켓은 연결된 소켓 위에 얹혀 있다.
     *
     * @param bindpoint ignored / 무시한다
     * @throws IOException always / 항상
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new SocketException("already bound");
    }

    /**
     * Returns the remote address of the socket below.
     * <p>
     * 아래 소켓의 원격 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public InetAddress getInetAddress() {
        return base.getInetAddress();
    }

    /**
     * Returns the local address of the socket below.
     * <p>
     * 아래 소켓의 지역 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public InetAddress getLocalAddress() {
        return base.getLocalAddress();
    }

    /**
     * Returns the remote port of the socket below.
     * <p>
     * 아래 소켓의 원격 포트를 반환한다.
     *
     * @return the port / 포트
     */
    public int getPort() {
        return base.getPort();
    }

    /**
     * Returns the local port of the socket below.
     * <p>
     * 아래 소켓의 지역 포트를 반환한다.
     *
     * @return the port / 포트
     */
    public int getLocalPort() {
        return base.getLocalPort();
    }

    /**
     * Returns the remote socket address of the socket below.
     * <p>
     * 아래 소켓의 원격 소켓 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public SocketAddress getRemoteSocketAddress() {
        return base.getRemoteSocketAddress();
    }

    /**
     * Returns the local socket address of the socket below.
     * <p>
     * 아래 소켓의 지역 소켓 주소를 반환한다.
     *
     * @return the address / 주소
     */
    public SocketAddress getLocalSocketAddress() {
        return base.getLocalSocketAddress();
    }

    /**
     * Sets TCP_NODELAY of the socket below.
     * <p>
     * 아래 소켓의 TCP_NODELAY 를 정한다.
     *
     * @param on the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setTcpNoDelay(boolean on) throws SocketException {
        base.setTcpNoDelay(on);
    }

    /**
     * Returns TCP_NODELAY of the socket below.
     * <p>
     * 아래 소켓의 TCP_NODELAY 를 반환한다.
     *
     * @return the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public boolean getTcpNoDelay() throws SocketException {
        return base.getTcpNoDelay();
    }

    /**
     * Sets SO_LINGER of the socket below.
     * <p>
     * 아래 소켓의 SO_LINGER 를 정한다.
     *
     * @param on     the option switch / 옵션 켬
     * @param linger the linger time / 지연 시간
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setSoLinger(boolean on, int linger) throws SocketException {
        base.setSoLinger(on, linger);
    }

    /**
     * Returns SO_LINGER of the socket below.
     * <p>
     * 아래 소켓의 SO_LINGER 를 반환한다.
     *
     * @return the linger time or -1 / 지연 시간 또는 -1
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public int getSoLinger() throws SocketException {
        return base.getSoLinger();
    }

    /**
     * Refused: urgent data cannot be sent through TLS.
     * <p>
     * 거절한다: 긴급 데이터는 TLS 로 보낼 수 없다.
     *
     * @param data ignored / 무시한다
     * @throws IOException always / 항상
     */
    public void sendUrgentData(int data) throws IOException {
        throw new SocketException("urgent data is not supported by TLS sockets");
    }

    /**
     * Refused: urgent data cannot be received through TLS.
     * <p>
     * 거절한다: 긴급 데이터는 TLS 로 받을 수 없다.
     *
     * @param on ignored / 무시한다
     * @throws SocketException always / 항상
     */
    public void setOOBInline(boolean on) throws SocketException {
        throw new SocketException("urgent data is not supported by TLS sockets");
    }

    /**
     * Returns false: urgent data is not supported.
     * <p>
     * false 를 반환한다: 긴급 데이터를 지원하지 않는다.
     *
     * @return false
     */
    public boolean getOOBInline() {
        return false;
    }

    /**
     * Sets the read timeout of the socket below; it applies to the handshake and to reads of application data.
     * <p>
     * 아래 소켓의 읽기 제한 시간을 정한다. 핸드셰이크와 응용 데이터 읽기에 적용된다.
     *
     * @param timeout the timeout in milliseconds, 0 for none / 밀리초 단위 제한 시간. 0 이면 없음
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setSoTimeout(int timeout) throws SocketException {
        base.setSoTimeout(timeout);
    }

    /**
     * Returns the read timeout of the socket below.
     * <p>
     * 아래 소켓의 읽기 제한 시간을 반환한다.
     *
     * @return the timeout in milliseconds / 밀리초 단위 제한 시간
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public int getSoTimeout() throws SocketException {
        return base.getSoTimeout();
    }

    /**
     * Sets the send buffer size of the socket below.
     * <p>
     * 아래 소켓의 송신 버퍼 크기를 정한다.
     *
     * @param size the size / 크기
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setSendBufferSize(int size) throws SocketException {
        base.setSendBufferSize(size);
    }

    /**
     * Returns the send buffer size of the socket below.
     * <p>
     * 아래 소켓의 송신 버퍼 크기를 반환한다.
     *
     * @return the size / 크기
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public int getSendBufferSize() throws SocketException {
        return base.getSendBufferSize();
    }

    /**
     * Sets the receive buffer size of the socket below.
     * <p>
     * 아래 소켓의 수신 버퍼 크기를 정한다.
     *
     * @param size the size / 크기
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        base.setReceiveBufferSize(size);
    }

    /**
     * Returns the receive buffer size of the socket below.
     * <p>
     * 아래 소켓의 수신 버퍼 크기를 반환한다.
     *
     * @return the size / 크기
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public int getReceiveBufferSize() throws SocketException {
        return base.getReceiveBufferSize();
    }

    /**
     * Sets SO_KEEPALIVE of the socket below.
     * <p>
     * 아래 소켓의 SO_KEEPALIVE 를 정한다.
     *
     * @param on the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setKeepAlive(boolean on) throws SocketException {
        base.setKeepAlive(on);
    }

    /**
     * Returns SO_KEEPALIVE of the socket below.
     * <p>
     * 아래 소켓의 SO_KEEPALIVE 를 반환한다.
     *
     * @return the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public boolean getKeepAlive() throws SocketException {
        return base.getKeepAlive();
    }

    /**
     * Sets the traffic class of the socket below.
     * <p>
     * 아래 소켓의 트래픽 클래스를 정한다.
     *
     * @param tc the traffic class / 트래픽 클래스
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setTrafficClass(int tc) throws SocketException {
        base.setTrafficClass(tc);
    }

    /**
     * Returns the traffic class of the socket below.
     * <p>
     * 아래 소켓의 트래픽 클래스를 반환한다.
     *
     * @return the traffic class / 트래픽 클래스
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public int getTrafficClass() throws SocketException {
        return base.getTrafficClass();
    }

    /**
     * Sets SO_REUSEADDR of the socket below.
     * <p>
     * 아래 소켓의 SO_REUSEADDR 를 정한다.
     *
     * @param on the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public void setReuseAddress(boolean on) throws SocketException {
        base.setReuseAddress(on);
    }

    /**
     * Returns SO_REUSEADDR of the socket below.
     * <p>
     * 아래 소켓의 SO_REUSEADDR 를 반환한다.
     *
     * @return the option value / 옵션 값
     * @throws SocketException on a socket error / 소켓 오류 시
     */
    public boolean getReuseAddress() throws SocketException {
        return base.getReuseAddress();
    }

    /**
     * Refused: a TLS connection cannot be half closed this way.
     * <p>
     * 거절한다: TLS 연결은 이런 식으로 반만 닫을 수 없다.
     *
     * @throws IOException always / 항상
     */
    public void shutdownInput() throws IOException {
        throw new UnsupportedOperationException("shutdownInput is not supported by TLS sockets");
    }

    /**
     * Refused: a TLS connection cannot be half closed this way.
     * <p>
     * 거절한다: TLS 연결은 이런 식으로 반만 닫을 수 없다.
     *
     * @throws IOException always / 항상
     */
    public void shutdownOutput() throws IOException {
        throw new UnsupportedOperationException("shutdownOutput is not supported by TLS sockets");
    }

    /**
     * Tells if the socket below is connected.
     * <p>
     * 아래 소켓이 연결되었는지 알려 준다.
     *
     * @return true if connected / 연결되었으면 true
     */
    public boolean isConnected() {
        return base.isConnected();
    }

    /**
     * Tells if the socket below is bound.
     * <p>
     * 아래 소켓이 묶였는지 알려 준다.
     *
     * @return true if bound / 묶였으면 true
     */
    public boolean isBound() {
        return base.isBound();
    }

    /**
     * Tells if this socket or the socket below was closed.
     * <p>
     * 이 소켓이나 아래 소켓이 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        return closed || base.isClosed();
    }

    /**
     * Tells if the input of the socket below was shut down.
     * <p>
     * 아래 소켓의 입력이 닫혔는지 알려 준다.
     *
     * @return true if shut down / 닫혔으면 true
     */
    public boolean isInputShutdown() {
        return base.isInputShutdown();
    }

    /**
     * Tells if the output of the socket below was shut down.
     * <p>
     * 아래 소켓의 출력이 닫혔는지 알려 준다.
     *
     * @return true if shut down / 닫혔으면 true
     */
    public boolean isOutputShutdown() {
        return base.isOutputShutdown();
    }

    /**
     * Describes the socket.
     * <p>
     * 소켓을 설명한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return "TlsSocket[" + host + ":" + port + " over " + base + "]";
    }
}
