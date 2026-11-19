package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * An <code>HttpURLConnection</code> for <b>http</b> URLs that is implemented over {@link SocketHttpClient}: it sends every request header
 * that the caller sets (also <code>Origin</code> and <code>Access-Control-Request-*</code>, which the <code>HttpURLConnection</code> of the
 * JRE drops) and accepts any method token. {@link HttpFetcher} uses it only for requests that must carry such a header and that are not
 * sent through a proxy; everything else about a request (cookies, redirects, content decoding, limits, cancellation by
 * {@link #disconnect()}) stays in {@link HttpFetcher}. Not supported and ignored: caching, proxies, redirect following, authentication and
 * keep-alive. The https variant is {@link SocketHttpsURLConnection}.
 * <p>
 * {@link SocketHttpClient} 위에 구현한 <b>http</b> URL 용 <code>HttpURLConnection</code>: 호출자가 정한 모든 요청 헤더(JRE 의
 * <code>HttpURLConnection</code> 이 버리는 <code>Origin</code> 과 <code>Access-Control-Request-*</code> 포함)를 보내고 어떤 메서드 토큰이든
 * 받아들인다. {@link HttpFetcher} 는 그런 헤더를 실어야 하고 프록시를 지나지 않는 요청에만 이것을 쓰며, 요청의 나머지(쿠키, 리디렉션, 내용 디코딩,
 * 제한, {@link #disconnect()} 로 하는 취소)는 {@link HttpFetcher} 에 남는다. 지원하지 않고 무시하는 것: 캐시, 프록시, 리디렉션 따라가기, 인증,
 * keep-alive. https 용은 {@link SocketHttpsURLConnection} 이다.
 */
final class SocketHttpURLConnection extends HttpURLConnection {

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
     * @param url the http URL / http URL
     */
    SocketHttpURLConnection(URL url) {
        super(url);
        client = new SocketHttpClient(url, false);
    }

    /**
     * Applies the timeouts that are known so far and returns the client.
     * <p>
     * 지금까지 정해진 제한 시간을 적용하고 클라이언트를 반환한다.
     *
     * @return the client / 클라이언트
     */
    private SocketHttpClient ready() {
        if (!client.isConnected()) {
            client.setTimeouts(getConnectTimeout(), getReadTimeout());
        }
        return client;
    }

    /**
     * Opens the connection.
     * <p>
     * 연결을 연다.
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
            return java.util.Collections.emptyMap();
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
}
