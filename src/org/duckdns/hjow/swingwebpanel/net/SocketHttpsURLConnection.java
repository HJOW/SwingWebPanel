package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * An <code>HttpsURLConnection</code> for <b>https</b> URLs implemented over {@link SocketHttpClient}; see {@link SocketHttpURLConnection}
 * for what it does and does not do. The TLS settings are those of the connection when it is opened: the socket factory
 * (<code>setSSLSocketFactory</code>) and the host name verifier (<code>setHostnameVerifier</code>) that the SSL extension point
 * ({@link SslConfigurer#configureSsl(HttpsURLConnection)}) may set, so a subclass of the component that overrides that method works the
 * same as for the connections of the JRE. The certificate chain is checked by the TLS implementation of the JRE with its default trust
 * store, and the host name by {@link HostnameMatcher}; the verifier of the connection can accept a certificate whose names do not fit, but
 * the default verifier of the JVM is ignored.
 * <p>
 * {@link SocketHttpClient} 위에 구현한 <b>https</b> URL 용 <code>HttpsURLConnection</code>. 하는 일과 하지 않는 일은
 * {@link SocketHttpURLConnection} 을 보라. TLS 설정은 연결을 열 때의 것이다: SSL 확장 지점({@link SslConfigurer#configureSsl(HttpsURLConnection)})이
 * 정할 수 있는 소켓 팩터리(<code>setSSLSocketFactory</code>)와 호스트 이름 검증기(<code>setHostnameVerifier</code>)이므로, 그 메서드를 재정의한
 * 컴포넌트 하위 클래스는 JRE 의 연결에서와 똑같이 동작한다. 인증서 사슬은 JRE 의 TLS 구현이 기본 신뢰 저장소로, 호스트 이름은
 * {@link HostnameMatcher} 가 확인한다. 연결의 검증기는 이름이 맞지 않는 인증서를 받아들일 수 있지만 JVM 의 기본 검증기는 무시한다.
 */
final class SocketHttpsURLConnection extends HttpsURLConnection {

    /**
     * The client that does the work.
     * <p>
     * 일을 하는 클라이언트.
     */
    private final SocketHttpClient client;

    /**
     * Creates the connection of a URL (nothing is connected yet).
     * <p>
     * URL 의 연결을 만든다(아직 아무것도 연결하지 않는다).
     *
     * @param url the https URL / https URL
     */
    SocketHttpsURLConnection(URL url) {
        super(url);
        client = new SocketHttpClient(url, true);
    }

    /**
     * Applies the timeouts and the TLS settings that are known so far and returns the client.
     * <p>
     * 지금까지 정해진 제한 시간과 TLS 설정을 적용하고 클라이언트를 반환한다.
     *
     * @return the client / 클라이언트
     */
    private SocketHttpClient ready() {
        if (!client.isConnected()) {
            client.setTimeouts(getConnectTimeout(), getReadTimeout());
            client.setTls(getSSLSocketFactory(), getHostnameVerifier());
        }
        return client;
    }

    /**
     * Opens the connection and finishes the TLS handshake.
     * <p>
     * 연결을 열고 TLS 핸드셰이크를 마친다.
     *
     * @throws IOException if it cannot be made / 만들 수 없으면
     */
    public void connect() throws IOException {
        ready().connect();
        connected = true;
    }

    /**
     * Sets the request method; any token is accepted.
     * <p>
     * 요청 메서드를 정한다. 어떤 토큰이든 받아들인다.
     *
     * @param m the method / 메서드
     * @throws ProtocolException if it is no token or the connection is open / 토큰이 아니거나 연결이 열려 있으면
     */
    public void setRequestMethod(String m) throws ProtocolException {
        if (client.isConnected()) {
            throw new ProtocolException("Can't reset method: already connected");
        }
        client.setMethod(m);
        method = m;
    }

    /**
     * Sets a request header (a null value removes it).
     * <p>
     * 요청 헤더를 정한다(null 값은 지운다).
     *
     * @param key   the name / 이름
     * @param value the value / 값
     */
    public void setRequestProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        if (client.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
        if (value == null) {
            client.removeHeader(key);
        } else {
            client.setHeader(key, value);
        }
    }

    /**
     * Adds a request header.
     * <p>
     * 요청 헤더를 더한다.
     *
     * @param key   the name / 이름
     * @param value the value / 값
     */
    public void addRequestProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        if (client.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
        if (value != null) {
            client.addHeader(key, value);
        }
    }

    /**
     * Returns the first request header with a name.
     * <p>
     * 이름이 같은 첫 요청 헤더를 반환한다.
     *
     * @param key the name / 이름
     * @return the value or null / 값. 없으면 null
     */
    public String getRequestProperty(String key) {
        return key == null ? null : client.getHeader(key);
    }

    /**
     * Returns the request headers.
     * <p>
     * 요청 헤더를 반환한다.
     *
     * @return the headers / 헤더
     */
    public Map<String, List<String>> getRequestProperties() {
        if (client.isConnected()) {
            throw new IllegalStateException("Already connected");
        }
        return client.getHeaders();
    }

    /**
     * Returns the stream that collects the request body (the body is sent with the request).
     * <p>
     * 요청 본문을 모으는 스트림을 반환한다(본문은 요청과 함께 보낸다).
     *
     * @return the stream / 스트림
     * @throws IOException if output is not enabled or the request was sent / 출력이 켜지지 않았거나 요청을 이미 보냈으면
     */
    public OutputStream getOutputStream() throws IOException {
        if (!getDoOutput()) {
            throw new ProtocolException("cannot write to a URLConnection if doOutput=false - call setDoOutput(true)");
        }
        return ready().outputStream();
    }

    /**
     * Sends the request if that did not happen and returns the body of a successful response.
     * <p>
     * 요청을 아직 보내지 않았다면 보내고 성공한 응답의 본문을 반환한다.
     *
     * @return the stream / 스트림
     * @throws IOException on failure or for an error status / 실패이거나 오류 상태이면
     */
    public InputStream getInputStream() throws IOException {
        InputStream in = ready().inputStream();
        connected = true;
        return in;
    }

    /**
     * Returns the body of an error response, or null.
     * <p>
     * 오류 응답의 본문을 반환한다. 없으면 null.
     *
     * @return the stream or null / 스트림 또는 null
     */
    public InputStream getErrorStream() {
        return ready().errorStream();
    }

    /**
     * Sends the request if that did not happen and returns the status code.
     * <p>
     * 요청을 아직 보내지 않았다면 보내고 상태 코드를 반환한다.
     *
     * @return the code / 코드
     * @throws IOException on failure / 실패 시
     */
    public int getResponseCode() throws IOException {
        ready().readHead();
        connected = true;
        return client.status();
    }

    /**
     * Sends the request if that did not happen and returns the reason phrase.
     * <p>
     * 요청을 아직 보내지 않았다면 보내고 이유 문구를 반환한다.
     *
     * @return the phrase / 문구
     * @throws IOException on failure / 실패 시
     */
    public String getResponseMessage() throws IOException {
        ready().readHead();
        connected = true;
        return client.message();
    }

    /**
     * Returns the last response header with a name, or null (also when the request fails).
     * <p>
     * 이름이 같은 마지막 응답 헤더를 반환한다. 없거나 요청이 실패하면 null.
     *
     * @param name the name / 이름
     * @return the value or null / 값 또는 null
     */
    public String getHeaderField(String name) {
        try {
            ready().readHead();
        } catch (IOException e) {
            return null;
        }
        return name == null ? client.headerAt(0) : client.headerValue(name);
    }

    /**
     * Returns the response header with an index (0 is the status line).
     * <p>
     * 번호의 응답 헤더를 반환한다(0 은 상태 줄).
     *
     * @param n the index / 번호
     * @return the value or null / 값 또는 null
     */
    public String getHeaderField(int n) {
        try {
            ready().readHead();
        } catch (IOException e) {
            return null;
        }
        return client.headerAt(n);
    }

    /**
     * Returns the name of the response header with an index (null for the status line).
     * <p>
     * 번호의 응답 헤더 이름을 반환한다(상태 줄이면 null).
     *
     * @param n the index / 번호
     * @return the name or null / 이름 또는 null
     */
    public String getHeaderFieldKey(int n) {
        try {
            ready().readHead();
        } catch (IOException e) {
            return null;
        }
        return client.headerKey(n);
    }

    /**
     * Returns the response headers by name; the status line has the key null.
     * <p>
     * 응답 헤더를 이름별로 반환한다. 상태 줄의 키는 null 이다.
     *
     * @return the headers (empty if the request fails) / 헤더(요청이 실패하면 빈 맵)
     */
    public Map<String, List<String>> getHeaderFields() {
        try {
            ready().readHead();
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        return client.headerFields();
    }

    /**
     * Closes the connection; callable from any thread, also while another thread is blocked in the connection.
     * <p>
     * 연결을 닫는다. 어느 스레드에서나 부를 수 있고 다른 스레드가 연결에서 막혀 있어도 된다.
     */
    public void disconnect() {
        client.disconnect();
    }

    /**
     * Tells that no proxy is used.
     * <p>
     * 프록시를 쓰지 않는다고 알려 준다.
     *
     * @return false / false
     */
    public boolean usingProxy() {
        return false;
    }

    /**
     * Returns the cipher suite of the TLS session.
     * <p>
     * TLS 세션의 암호 스위트를 반환한다.
     *
     * @return the name / 이름
     */
    public String getCipherSuite() {
        if (!client.isConnected()) {
            throw new IllegalStateException("connection not yet open");
        }
        return client.cipherSuite();
    }

    /**
     * Returns the certificates that the client sent, or null.
     * <p>
     * 클라이언트가 보낸 인증서를 반환한다. 없으면 null.
     *
     * @return the chain or null / 인증서 사슬 또는 null
     */
    public Certificate[] getLocalCertificates() {
        if (!client.isConnected()) {
            throw new IllegalStateException("connection not yet open");
        }
        return client.localCertificates();
    }

    /**
     * Returns the certificate chain of the server.
     * <p>
     * 서버의 인증서 사슬을 반환한다.
     *
     * @return the chain / 인증서 사슬
     * @throws SSLPeerUnverifiedException if the server is not verified / 서버가 확인되지 않았으면
     */
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        if (!client.isConnected()) {
            throw new IllegalStateException("connection not yet open");
        }
        return client.serverCertificates();
    }
}
