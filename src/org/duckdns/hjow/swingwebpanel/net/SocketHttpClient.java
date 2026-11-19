package org.duckdns.hjow.swingwebpanel.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A small HTTP/1.1 client over a plain socket or a TLS socket: it sends one request with <b>any</b> request header and reads one
 * response (no keep-alive, no proxy, no redirect following, no content decoding, no caching). It exists because the
 * <code>HttpURLConnection</code> of old JREs silently drops restricted request headers such as <code>Origin</code> and
 * <code>Access-Control-Request-Method</code>, which cross-origin requests and the CORS preflight need; {@link SocketHttpURLConnection} and
 * {@link SocketHttpsURLConnection} wrap this class in the <code>HttpURLConnection</code> API so that {@link HttpFetcher} keeps its
 * single code path (cookies, redirects, compression, the SSL hook). Header names and values containing CR or LF are refused. Only the
 * response head is parsed with a size limit; the body is a stream (content-length, chunked or until the connection closes). A TLS
 * connection verifies the host name of the certificate itself ({@link HostnameMatcher}) unless a non-default verifier accepts it.
 * Thread rules: one thread uses the request; {@link #disconnect()} may be called from any thread to end a blocked connect or read.
 * <p>
 * 일반 소켓이나 TLS 소켓 위의 작은 HTTP/1.1 클라이언트: <b>어떤</b> 요청 헤더로든 요청 하나를 보내고 응답 하나를 읽는다(keep-alive, 프록시,
 * 리디렉션 따라가기, 내용 디코딩, 캐시 없음). 오래된 JRE 의 <code>HttpURLConnection</code> 이 <code>Origin</code>,
 * <code>Access-Control-Request-Method</code> 같은 제한 요청 헤더를 조용히 버리는데 교차 오리진 요청과 CORS 사전 요청에 그것이 필요하므로 만들었다.
 * {@link SocketHttpURLConnection} 과 {@link SocketHttpsURLConnection} 이 이 클래스를 <code>HttpURLConnection</code> API 로 감싸서
 * {@link HttpFetcher} 가 하나의 코드 경로(쿠키, 리디렉션, 압축, SSL 훅)를 유지한다. CR 이나 LF 가 든 헤더 이름과 값은 거부한다. 응답 머리만 크기
 * 제한을 두고 해석하며 본문은 스트림이다(content-length, chunked, 연결이 닫힐 때까지). TLS 연결은 인증서의 호스트 이름을 스스로 확인한다
 * ({@link HostnameMatcher}). 기본이 아닌 검증기가 받아들이면 통과한다. 스레드 규칙: 요청은 한 스레드가 쓰며, {@link #disconnect()} 는 막힌 연결이나
 * 읽기를 끝내려고 어느 스레드에서나 부를 수 있다.
 */
final class SocketHttpClient {

    /**
     * The longest line of a response head that is accepted.
     * <p>
     * 받아들이는 응답 머리 한 줄의 최대 길이.
     */
    static final int MAX_LINE = 16 * 1024;

    /**
     * The largest number of header lines of a response.
     * <p>
     * 응답의 최대 헤더 줄 수.
     */
    static final int MAX_HEADERS = 256;

    /**
     * The URL of the request.
     * <p>
     * 요청의 URL.
     */
    private final URL url;

    /**
     * True for https.
     * <p>
     * https 이면 true.
     */
    private final boolean secure;

    /**
     * The request method.
     * <p>
     * 요청 메서드.
     */
    private String method = "GET";

    /**
     * The request headers in the order they were set: name and value.
     * <p>
     * 요청 헤더를 설정한 순서대로: 이름과 값.
     */
    private final List<String[]> requestHeaders = new ArrayList<String[]>();

    /**
     * The connect timeout in milliseconds, 0 for none.
     * <p>
     * 연결 제한 시간(밀리초). 0 이면 없음.
     */
    private int connectTimeout;

    /**
     * The read timeout in milliseconds, 0 for none.
     * <p>
     * 읽기 제한 시간(밀리초). 0 이면 없음.
     */
    private int readTimeout;

    /**
     * The socket factory for TLS (null for the default of the JRE).
     * <p>
     * TLS 용 소켓 팩터리(JRE 기본이면 null).
     */
    private SSLSocketFactory factory;

    /**
     * The host name verifier that may accept a certificate whose names do not match (null or the default verifier means none).
     * <p>
     * 이름이 일치하지 않는 인증서를 받아들일 수 있는 호스트 이름 검증기(null 이나 기본 검증기이면 없음).
     */
    private HostnameVerifier verifier;

    /**
     * The socket, published at once so that {@link #disconnect()} can close it while the connection is being made.
     * <p>
     * 소켓. 연결을 만드는 도중에도 {@link #disconnect()} 가 닫을 수 있도록 곧바로 게시한다.
     */
    private volatile Socket socket;

    /**
     * True after {@link #disconnect()}.
     * <p>
     * {@link #disconnect()} 뒤에는 true.
     */
    private volatile boolean disconnected;

    /**
     * The TLS session after the handshake.
     * <p>
     * 핸드셰이크 뒤의 TLS 세션.
     */
    private SSLSession session;

    /**
     * The buffered input of the connection.
     * <p>
     * 연결의 버퍼링된 입력.
     */
    private InputStream in;

    /**
     * The body of the request, collected until the request is sent.
     * <p>
     * 요청 본문. 요청을 보낼 때까지 모은다.
     */
    private ByteArrayOutputStream body;

    /**
     * True once the response head was read.
     * <p>
     * 응답 머리를 읽었으면 true.
     */
    private boolean headRead;

    /**
     * The status code of the response.
     * <p>
     * 응답의 상태 코드.
     */
    private int status = -1;

    /**
     * The reason phrase of the response.
     * <p>
     * 응답의 이유 문구.
     */
    private String message;

    /**
     * The status line (index 0 of the header fields) and the header lines of the response: name (null for the status line) and value.
     * <p>
     * 응답의 상태 줄(헤더 필드의 0 번)과 헤더 줄: 이름(상태 줄이면 null)과 값.
     */
    private final List<String[]> responseHeaders = new ArrayList<String[]>();

    /**
     * The stream of the response body.
     * <p>
     * 응답 본문의 스트림.
     */
    private InputStream bodyStream;

    /**
     * Creates the client of one request.
     * <p>
     * 요청 하나의 클라이언트를 만든다.
     *
     * @param url    the URL, http or https / URL(http 또는 https)
     * @param secure true for https / https 이면 true
     */
    SocketHttpClient(URL url, boolean secure) {
        this.url = url;
        this.secure = secure;
    }

    /**
     * Sets the request method; any token is accepted (also <code>PATCH</code>, which the JRE refuses).
     * <p>
     * 요청 메서드를 정한다. 어떤 토큰이든 받아들인다(JRE 가 거부하는 <code>PATCH</code> 도).
     *
     * @param m the method / 메서드
     * @throws ProtocolException if it is no token / 토큰이 아니면
     */
    void setMethod(String m) throws ProtocolException {
        if (m == null || m.length() == 0) {
            throw new ProtocolException("Invalid HTTP method: " + m);
        }
        // 메서드 이름은 공백이나 제어 문자가 없는 토큰이어야 한다.
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            if (c <= ' ' || c >= 0x7f || c == ':' || c == '/') {
                throw new ProtocolException("Invalid HTTP method: " + m);
            }
        }
        method = m;
    }

    /**
     * Sets a request header, replacing the ones with the same name (case-insensitively).
     * <p>
     * 요청 헤더를 정하고 같은 이름(대소문자 무시)의 기존 헤더를 대신한다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     */
    void setHeader(String name, String value) {
        check(name, value);
        // 같은 이름의 기존 헤더를 지운다.
        for (int i = requestHeaders.size() - 1; i >= 0; i--) {
            if (requestHeaders.get(i)[0].equalsIgnoreCase(name)) {
                requestHeaders.remove(i);
            }
        }
        requestHeaders.add(new String[] { name, value });
    }

    /**
     * Removes the request headers with a name (case-insensitively).
     * <p>
     * 이름이 같은(대소문자 무시) 요청 헤더를 지운다.
     *
     * @param name the name / 이름
     */
    void removeHeader(String name) {
        // 같은 이름의 헤더를 뒤에서부터 지운다.
        for (int i = requestHeaders.size() - 1; i >= 0; i--) {
            if (requestHeaders.get(i)[0].equalsIgnoreCase(name)) {
                requestHeaders.remove(i);
            }
        }
    }

    /**
     * Adds a request header without replacing others.
     * <p>
     * 다른 헤더를 대신하지 않고 요청 헤더를 더한다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     */
    void addHeader(String name, String value) {
        check(name, value);
        requestHeaders.add(new String[] { name, value });
    }

    /**
     * Refuses header names and values that could split the request (CR, LF) or that are empty names.
     * <p>
     * 요청을 쪼갤 수 있는(CR, LF) 헤더 이름과 값, 빈 이름을 거부한다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     */
    private static void check(String name, String value) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("header name must not be empty");
        }
        // 이름에는 토큰 문자만, 값에는 CR, LF, NUL 이 없어야 한다.
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c <= ' ' || c >= 0x7f || c == ':') {
                throw new IllegalArgumentException("illegal character in header name: " + name);
            }
        }
        if (value != null) {
            // 값의 문자를 하나씩 보며 요청을 쪼갤 수 있는 제어 문자가 있는지 확인한다.
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\r' || c == '\n' || c == 0) {
                    throw new IllegalArgumentException("illegal character in header value of " + name);
                }
            }
        }
    }

    /**
     * Returns the first request header with a name.
     * <p>
     * 이름이 같은 첫 요청 헤더를 반환한다.
     *
     * @param name the name / 이름
     * @return the value or null / 값. 없으면 null
     */
    String getHeader(String name) {
        // 이름이 같은 첫 헤더를 찾는다.
        for (String[] h : requestHeaders) {
            if (h[0].equalsIgnoreCase(name)) {
                return h[1];
            }
        }
        return null;
    }

    /**
     * Returns the request headers by name.
     * <p>
     * 요청 헤더를 이름별로 반환한다.
     *
     * @return an unmodifiable map / 수정할 수 없는 맵
     */
    Map<String, List<String>> getHeaders() {
        Map<String, List<String>> m = new LinkedHashMap<String, List<String>>();
        // 같은 이름의 값을 모은다.
        for (String[] h : requestHeaders) {
            List<String> list = m.get(h[0]);
            if (list == null) {
                list = new ArrayList<String>();
                m.put(h[0], list);
            }
            list.add(h[1]);
        }
        return Collections.unmodifiableMap(m);
    }

    /**
     * Sets the timeouts.
     * <p>
     * 제한 시간을 정한다.
     *
     * @param connectMs connect timeout / 연결 제한 시간
     * @param readMs    read timeout / 읽기 제한 시간
     */
    void setTimeouts(int connectMs, int readMs) {
        connectTimeout = Math.max(0, connectMs);
        readTimeout = Math.max(0, readMs);
    }

    /**
     * Sets the TLS socket factory and the host name verifier of an https request.
     * <p>
     * https 요청의 TLS 소켓 팩터리와 호스트 이름 검증기를 정한다.
     *
     * @param f the factory or null / 팩터리 또는 null
     * @param v the verifier that may accept a mismatching certificate, or null / 이름이 맞지 않는 인증서를 받아들일 수 있는 검증기. 없으면 null
     */
    void setTls(SSLSocketFactory f, HostnameVerifier v) {
        factory = f;
        verifier = v;
    }

    /**
     * Returns the stream that collects the request body.
     * <p>
     * 요청 본문을 모으는 스트림을 반환한다.
     *
     * @return the stream / 스트림
     * @throws IOException if the request was already sent / 요청을 이미 보냈으면
     */
    OutputStream outputStream() throws IOException {
        if (headRead) {
            throw new ProtocolException("Cannot write output after reading input.");
        }
        if (body == null) {
            body = new ByteArrayOutputStream();
        }
        return body;
    }

    /**
     * Opens the connection (and, for https, finishes the TLS handshake and checks the host name). Does nothing if it is open.
     * <p>
     * 연결을 연다(https 이면 TLS 핸드셰이크를 마치고 호스트 이름을 확인한다). 이미 열려 있으면 아무것도 하지 않는다.
     *
     * @throws IOException if the connection cannot be made / 연결을 만들 수 없으면
     */
    void connect() throws IOException {
        if (socket != null) {
            return;
        }
        if (disconnected) {
            throw new IOException("connection closed");
        }
        String host = url.getHost();
        int port = url.getPort() >= 0 ? url.getPort() : (secure ? 443 : 80);
        Socket plain = new Socket();
        socket = plain;
        try {
            plain.setTcpNoDelay(true);
            plain.setSoTimeout(readTimeout);
            plain.connect(new InetSocketAddress(host, port), connectTimeout);
            if (disconnected) {
                throw new IOException("connection closed");
            }
            Socket use = plain;
            if (secure) {
                SSLSocketFactory f = factory != null ? factory : (SSLSocketFactory) SSLSocketFactory.getDefault();
                // 일반 소켓 위에 TLS 를 씌우면 호스트 이름이 팩터리에 전달되어 새 JRE 에서는 SNI 도 보낸다.
                SSLSocket ssl = (SSLSocket) f.createSocket(plain, host, port, true);
                socket = ssl;
                ssl.setSoTimeout(readTimeout);
                ssl.startHandshake();
                session = ssl.getSession();
                verifyHost(host);
                use = ssl;
            }
            in = new BufferedInputStream(use.getInputStream(), 8192);
        } catch (IOException e) {
            closeQuietly();
            throw e;
        } catch (RuntimeException e) {
            closeQuietly();
            throw e;
        }
    }

    /**
     * Checks that the certificate names the host; a non-default verifier may accept it anyway.
     * <p>
     * 인증서가 호스트를 이름으로 가지는지 확인한다. 기본이 아닌 검증기는 그래도 받아들일 수 있다.
     *
     * @param host the host name / 호스트 이름
     * @throws IOException if the certificate does not fit the host / 인증서가 호스트에 맞지 않으면
     */
    private void verifyHost(String host) throws IOException {
        Certificate[] chain = session.getPeerCertificates();
        if (chain != null && chain.length > 0 && chain[0] instanceof X509Certificate && HostnameMatcher.matches(host, (X509Certificate) chain[0])) {
            return;
        }
        if (verifier != null && verifier != javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier() && verifier.verify(host, session)) {
            return;
        }
        throw new SSLPeerUnverifiedException("certificate does not match the host name " + host);
    }

    /**
     * Sends the request and reads the response head (the status line and the headers; interim 1xx responses are skipped). Does nothing
     * after the first call.
     * <p>
     * 요청을 보내고 응답 머리(상태 줄과 헤더. 중간 1xx 응답은 건너뛴다)를 읽는다. 처음 부른 뒤에는 아무것도 하지 않는다.
     *
     * @throws IOException on failure / 실패 시
     */
    void readHead() throws IOException {
        if (headRead) {
            return;
        }
        connect();
        try {
            send();
            parseHead();
            headRead = true;
        } catch (IOException e) {
            closeQuietly();
            throw e;
        } catch (RuntimeException e) {
            closeQuietly();
            throw e;
        }
    }

    /**
     * Writes the request line, the headers and the body.
     * <p>
     * 요청 줄, 헤더, 본문을 쓴다.
     *
     * @throws IOException on failure / 실패 시
     */
    private void send() throws IOException {
        byte[] payload = body == null ? new byte[0] : body.toByteArray();
        boolean needsLength = payload.length > 0 || "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        String path = url.getFile();
        if (path == null || path.length() == 0) {
            path = "/";
        } else if (path.charAt(0) == '?') {
            path = "/" + path;
        }
        int port = url.getPort();
        boolean defaultPort = port < 0 || port == (secure ? 443 : 80);
        StringBuilder head = new StringBuilder(512);
        head.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
        head.append("Host: ").append(url.getHost()).append(defaultPort ? "" : ":" + port).append("\r\n");
        // 호출자가 정한 헤더를 순서대로 쓰되 이 클라이언트가 직접 관리하는 헤더는 건너뛴다.
        for (String[] h : requestHeaders) {
            String n = h[0].toLowerCase(Locale.ENGLISH);
            if ("host".equals(n) || "content-length".equals(n) || "connection".equals(n) || "transfer-encoding".equals(n)) {
                continue;
            }
            head.append(h[0]).append(": ").append(h[1] == null ? "" : h[1]).append("\r\n");
        }
        if (needsLength) {
            head.append("Content-Length: ").append(payload.length).append("\r\n");
        }
        head.append("Connection: close\r\n\r\n");
        OutputStream out = socket.getOutputStream();
        out.write(head.toString().getBytes("ISO-8859-1"));
        if (payload.length > 0) {
            out.write(payload);
        }
        out.flush();
    }

    /**
     * Reads the response head and prepares the body stream.
     * <p>
     * 응답 머리를 읽고 본문 스트림을 준비한다.
     *
     * @throws IOException on failure / 실패 시
     */
    private void parseHead() throws IOException {
        // 중간 응답(1xx)을 건너뛰며 최종 응답의 상태 줄을 찾을 때까지 읽는다.
        for (;;) {
            responseHeaders.clear();
            String line = readLine();
            if (line == null) {
                throw new IOException("Unexpected end of file from server");
            }
            int sp = line.indexOf(' ');
            if (sp < 0 || !line.startsWith("HTTP/")) {
                throw new IOException("Invalid HTTP response: " + printable(line));
            }
            int code;
            String reason;
            try {
                String rest = line.substring(sp + 1).trim();
                int sp2 = rest.indexOf(' ');
                code = Integer.parseInt(sp2 < 0 ? rest : rest.substring(0, sp2));
                reason = sp2 < 0 ? "" : rest.substring(sp2 + 1).trim();
            } catch (NumberFormatException e) {
                throw new IOException("Invalid HTTP status line: " + printable(line));
            }
            responseHeaders.add(new String[] { null, line });
            readHeaderLines();
            // 중간 응답(100 Continue 등)은 건너뛰고 다음 응답을 읽는다.
            if (code >= 100 && code < 200 && code != 101) {
                continue;
            }
            status = code;
            message = reason;
            break;
        }
        bodyStream = openBody();
    }

    /**
     * Reads the header lines up to the empty line.
     * <p>
     * 빈 줄까지 헤더 줄을 읽는다.
     *
     * @throws IOException on failure / 실패 시
     */
    private void readHeaderLines() throws IOException {
        int count = 0;
        // 빈 줄이 나올 때까지 헤더 줄을 하나씩 읽는다.
        for (;;) {
            String line = readLine();
            if (line == null) {
                throw new IOException("Unexpected end of file from server");
            }
            if (line.length() == 0) {
                return;
            }
            if (++count > MAX_HEADERS) {
                throw new IOException("too many response headers");
            }
            // 공백으로 시작하는 줄은 앞 헤더의 이어지는 줄이다.
            if ((line.charAt(0) == ' ' || line.charAt(0) == '\t') && responseHeaders.size() > 1) {
                String[] last = responseHeaders.get(responseHeaders.size() - 1);
                last[1] = last[1] + " " + line.trim();
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            responseHeaders.add(new String[] { line.substring(0, colon).trim(), line.substring(colon + 1).trim() });
        }
    }

    /**
     * Reads one line of the response head (CR LF or LF ended), at most {@link #MAX_LINE} characters.
     * <p>
     * 응답 머리의 한 줄(CR LF 나 LF 로 끝남)을 최대 {@link #MAX_LINE} 문자까지 읽는다.
     *
     * @return the line without the line end, or null at the end of the stream before any character / 줄 끝을 뺀 줄. 문자 전에 스트림이 끝났으면 null
     * @throws IOException on failure / 실패 시
     */
    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        // 줄 끝을 만날 때까지 한 문자씩 읽는다.
        while ((c = in.read()) >= 0) {
            if (c == '\n') {
                int n = sb.length();
                if (n > 0 && sb.charAt(n - 1) == '\r') {
                    sb.setLength(n - 1);
                }
                return sb.toString();
            }
            if (sb.length() >= MAX_LINE) {
                throw new IOException("response header line too long");
            }
            sb.append((char) c);
        }
        if (sb.length() == 0) {
            return null;
        }
        throw new IOException("Unexpected end of file from server");
    }

    /**
     * Makes a text printable for messages (control characters removed, shortened).
     * <p>
     * 메시지에 쓸 수 있도록 문자열을 다듬는다(제어 문자 제거, 줄임).
     *
     * @param s the text / 문자열
     * @return the text / 문자열
     */
    private static String printable(String s) {
        StringBuilder sb = new StringBuilder();
        // 제어 문자를 건너뛰며 64자까지만 옮긴다.
        for (int i = 0; i < s.length() && sb.length() < 64; i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < 0x7f) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Chooses how the body is framed and returns its stream.
     * <p>
     * 본문의 경계를 정하는 방법을 고르고 그 스트림을 반환한다.
     *
     * @return the body stream / 본문 스트림
     * @throws IOException if the framing is invalid / 경계 정보가 잘못되었으면
     */
    private InputStream openBody() throws IOException {
        if ("HEAD".equals(method) || status == 204 || status == 304 || status == 101) {
            return new BoundedStream(in, 0);
        }
        String te = headerValue("transfer-encoding");
        if (te != null && te.toLowerCase(Locale.ENGLISH).indexOf("chunked") >= 0) {
            return new ChunkedStream(in);
        }
        String cl = headerValue("content-length");
        if (cl != null) {
            try {
                long n = Long.parseLong(cl.trim());
                if (n < 0) {
                    throw new IOException("negative Content-Length");
                }
                return new BoundedStream(in, n);
            } catch (NumberFormatException e) {
                throw new IOException("invalid Content-Length: " + printable(cl));
            }
        }
        return in;
    }

    /**
     * Returns the last response header with a name.
     * <p>
     * 이름이 같은 마지막 응답 헤더를 반환한다.
     *
     * @param name the name / 이름
     * @return the value or null / 값. 없으면 null
     */
    String headerValue(String name) {
        // 뒤에서부터 찾아 같은 이름의 마지막 값을 고른다.
        for (int i = responseHeaders.size() - 1; i >= 1; i--) {
            String[] h = responseHeaders.get(i);
            if (h[0] != null && h[0].equalsIgnoreCase(name)) {
                return h[1];
            }
        }
        return null;
    }

    /**
     * Returns the number of header fields including the status line.
     * <p>
     * 상태 줄을 포함한 헤더 필드 수를 반환한다.
     *
     * @return the count / 개수
     */
    int headerCount() {
        return responseHeaders.size();
    }

    /**
     * Returns the name of the header field with an index (null for the status line).
     * <p>
     * 번호의 헤더 필드 이름을 반환한다(상태 줄이면 null).
     *
     * @param n the index / 번호
     * @return the name or null / 이름 또는 null
     */
    String headerKey(int n) {
        return n >= 0 && n < responseHeaders.size() ? responseHeaders.get(n)[0] : null;
    }

    /**
     * Returns the value of the header field with an index (the status line for 0).
     * <p>
     * 번호의 헤더 필드 값을 반환한다(0 이면 상태 줄).
     *
     * @param n the index / 번호
     * @return the value or null / 값. 없으면 null
     */
    String headerAt(int n) {
        return n >= 0 && n < responseHeaders.size() ? responseHeaders.get(n)[1] : null;
    }

    /**
     * Returns the response headers by name; the status line has the key null.
     * <p>
     * 응답 헤더를 이름별로 반환한다. 상태 줄의 키는 null 이다.
     *
     * @return an unmodifiable map / 수정할 수 없는 맵
     */
    Map<String, List<String>> headerFields() {
        Map<String, List<String>> m = new LinkedHashMap<String, List<String>>();
        // 헤더 줄을 순서대로 이름별 목록에 모은다.
        for (String[] h : responseHeaders) {
            List<String> list = m.get(h[0]);
            if (list == null) {
                list = new ArrayList<String>();
                m.put(h[0], list);
            }
            list.add(h[1]);
        }
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        // 값 목록도 수정할 수 없게 한다.
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Returns the status code.
     * <p>
     * 상태 코드를 반환한다.
     *
     * @return the code / 코드
     */
    int status() {
        return status;
    }

    /**
     * Returns the reason phrase.
     * <p>
     * 이유 문구를 반환한다.
     *
     * @return the phrase / 문구
     */
    String message() {
        return message;
    }

    /**
     * Returns the body stream of a successful response (the contract of <code>HttpURLConnection.getInputStream</code>: an error status
     * throws).
     * <p>
     * 성공한 응답의 본문 스트림을 반환한다(<code>HttpURLConnection.getInputStream</code> 의 계약: 오류 상태는 예외).
     *
     * @return the stream / 스트림
     * @throws IOException on failure or an error status / 실패이거나 오류 상태이면
     */
    InputStream inputStream() throws IOException {
        readHead();
        if (status >= 400) {
            if (status == 404 || status == 410) {
                throw new FileNotFoundException(url.toString());
            }
            throw new IOException("Server returned HTTP response code: " + status + " for URL: " + url);
        }
        return bodyStream;
    }

    /**
     * Returns the body stream of an error response, or null for a successful one.
     * <p>
     * 오류 응답의 본문 스트림을 반환한다. 성공한 응답이면 null.
     *
     * @return the stream or null / 스트림 또는 null
     */
    InputStream errorStream() {
        try {
            readHead();
        } catch (IOException e) {
            return null;
        }
        return status >= 400 ? bodyStream : null;
    }

    /**
     * Returns the cipher suite of the TLS session.
     * <p>
     * TLS 세션의 암호 스위트를 반환한다.
     *
     * @return the name / 이름
     */
    String cipherSuite() {
        return session == null ? null : session.getCipherSuite();
    }

    /**
     * Returns the certificates of the server.
     * <p>
     * 서버의 인증서를 반환한다.
     *
     * @return the chain / 인증서 사슬
     * @throws SSLPeerUnverifiedException if the peer is not verified / 상대가 확인되지 않았으면
     */
    Certificate[] serverCertificates() throws SSLPeerUnverifiedException {
        if (session == null) {
            throw new IllegalStateException("not connected");
        }
        return session.getPeerCertificates();
    }

    /**
     * Returns the certificates that the client sent.
     * <p>
     * 클라이언트가 보낸 인증서를 반환한다.
     *
     * @return the chain or null / 인증서 사슬 또는 null
     */
    Certificate[] localCertificates() {
        return session == null ? null : session.getLocalCertificates();
    }

    /**
     * Closes the connection; callable from any thread, also while another thread is blocked in the connection or in a read.
     * <p>
     * 연결을 닫는다. 어느 스레드에서나 부를 수 있고 다른 스레드가 연결이나 읽기에서 막혀 있어도 된다.
     */
    void disconnect() {
        disconnected = true;
        closeQuietly();
    }

    /**
     * Closes the socket and ignores errors.
     * <p>
     * 소켓을 닫고 오류는 무시한다.
     */
    private void closeQuietly() {
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                return;
            }
        }
    }

    /**
     * The body of a response with a known length: ends after that many bytes and fails when the connection ends earlier.
     * <p>
     * 길이를 아는 응답의 본문: 그만큼의 바이트 뒤에 끝나며 연결이 더 일찍 끝나면 실패한다.
     */
    private static final class BoundedStream extends InputStream {
        /** The underlying stream. / 아래의 스트림. */
        private final InputStream source;
        /** The bytes that are still expected. / 아직 기대하는 바이트. */
        private long left;

        /**
         * Creates the stream.
         * <p>
         * 스트림을 만든다.
         *
         * @param source the underlying stream / 아래의 스트림
         * @param length the length of the body / 본문 길이
         */
        BoundedStream(InputStream source, long length) {
            this.source = source;
            this.left = length;
        }

        /**
         * Reads one byte.
         * <p>
         * 한 바이트를 읽는다.
         *
         * @return the byte or -1 / 바이트. 끝이면 -1
         * @throws IOException on failure / 실패 시
         */
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xff;
        }

        /**
         * Reads bytes.
         * <p>
         * 바이트를 읽는다.
         *
         * @param b   the buffer / 버퍼
         * @param off the offset / 위치
         * @param len the length / 길이
         * @return the number of bytes or -1 / 읽은 수. 끝이면 -1
         * @throws IOException on failure or a premature end / 실패이거나 일찍 끝나면
         */
        public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) {
                return -1;
            }
            int n = source.read(b, off, (int) Math.min(len, left));
            if (n < 0) {
                throw new EOFException("Premature EOF");
            }
            left -= n;
            return n;
        }

        /**
         * Closes nothing (the connection is closed by the owner).
         * <p>
         * 아무것도 닫지 않는다(연결은 소유자가 닫는다).
         */
        public void close() {
            left = 0;
        }
    }

    /**
     * The body of a chunked response.
     * <p>
     * chunked 응답의 본문.
     */
    private static final class ChunkedStream extends InputStream {
        /** The underlying stream. / 아래의 스트림. */
        private final InputStream source;
        /** The bytes left in the current chunk. / 현재 조각에 남은 바이트. */
        private long left;
        /** True after the last chunk and the trailers. / 마지막 조각과 트레일러 뒤에는 true. */
        private boolean done;

        /**
         * Creates the stream.
         * <p>
         * 스트림을 만든다.
         *
         * @param source the underlying stream / 아래의 스트림
         */
        ChunkedStream(InputStream source) {
            this.source = source;
        }

        /**
         * Reads one byte.
         * <p>
         * 한 바이트를 읽는다.
         *
         * @return the byte or -1 / 바이트. 끝이면 -1
         * @throws IOException on failure / 실패 시
         */
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xff;
        }

        /**
         * Reads bytes.
         * <p>
         * 바이트를 읽는다.
         *
         * @param b   the buffer / 버퍼
         * @param off the offset / 위치
         * @param len the length / 길이
         * @return the number of bytes or -1 / 읽은 수. 끝이면 -1
         * @throws IOException on failure, bad framing or a premature end / 실패, 잘못된 경계, 일찍 끝나는 경우
         */
        public int read(byte[] b, int off, int len) throws IOException {
            if (done) {
                return -1;
            }
            if (left == 0) {
                next();
                if (done) {
                    return -1;
                }
            }
            int n = source.read(b, off, (int) Math.min(len, left));
            if (n < 0) {
                throw new EOFException("Premature EOF");
            }
            left -= n;
            if (left == 0) {
                line();
            }
            return n;
        }

        /**
         * Reads the size line of the next chunk, or the trailers after the last chunk.
         * <p>
         * 다음 조각의 크기 줄을 읽거나 마지막 조각 뒤의 트레일러를 읽는다.
         *
         * @throws IOException on bad framing / 경계가 잘못되었으면
         */
        private void next() throws IOException {
            String size = line();
            int semi = size.indexOf(';');
            String hex = (semi < 0 ? size : size.substring(0, semi)).trim();
            try {
                left = Long.parseLong(hex, 16);
            } catch (NumberFormatException e) {
                throw new IOException("invalid chunk size: " + printable(size));
            }
            if (left < 0) {
                throw new IOException("negative chunk size");
            }
            if (left == 0) {
                // 마지막 조각 뒤의 트레일러는 빈 줄까지 읽어 버린다.
                while (line().length() > 0) {
                    continue;
                }
                done = true;
            }
        }

        /**
         * Reads one line of the chunk framing.
         * <p>
         * 조각 경계의 한 줄을 읽는다.
         *
         * @return the line without the line end / 줄 끝을 뺀 줄
         * @throws IOException on failure or a premature end / 실패이거나 일찍 끝나면
         */
        private String line() throws IOException {
            StringBuilder sb = new StringBuilder();
            int c;
            // 줄 끝을 만날 때까지 읽는다.
            while ((c = source.read()) >= 0) {
                if (c == '\n') {
                    int n = sb.length();
                    if (n > 0 && sb.charAt(n - 1) == '\r') {
                        sb.setLength(n - 1);
                    }
                    return sb.toString();
                }
                if (sb.length() >= MAX_LINE) {
                    throw new IOException("chunk line too long");
                }
                sb.append((char) c);
            }
            throw new EOFException("Premature EOF");
        }

        /**
         * Closes nothing (the connection is closed by the owner).
         * <p>
         * 아무것도 닫지 않는다(연결은 소유자가 닫는다).
         */
        public void close() {
            done = true;
        }
    }

    /**
     * Tells whether the connection was opened (so that request headers may no longer change).
     * <p>
     * 연결을 열었는지(그러면 요청 헤더를 더 바꿀 수 없다) 알려 준다.
     *
     * @return true if connected / 연결했으면 true
     */
    boolean isConnected() {
        return socket != null;
    }

    /**
     * Tells whether a request to a URL would go through a proxy of the JVM (the system properties <code>http.proxyHost</code> and so on, or
     * a default proxy selector). This client has no proxy support, so such a request uses the <code>HttpURLConnection</code> of the JRE.
     * <p>
     * URL 로의 요청이 JVM 의 프록시(<code>http.proxyHost</code> 등의 시스템 속성이나 기본 프록시 선택기)를 지나는지 알려 준다. 이 클라이언트는 프록시를
     * 지원하지 않으므로 그런 요청은 JRE 의 <code>HttpURLConnection</code> 을 쓴다.
     *
     * @param url the URL / URL
     * @return true if a proxy would be used or if that cannot be decided / 프록시를 쓰거나 판단할 수 없으면 true
     */
    static boolean viaProxy(URL url) {
        try {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector == null) {
                return false;
            }
            // 선택기가 돌려준 프록시 중 직접 연결이 아닌 것이 하나라도 있으면 프록시를 쓰는 것으로 본다.
            for (Proxy p : selector.select(url.toURI())) {
                if (p.type() != Proxy.Type.DIRECT) {
                    return true;
                }
            }
            return false;
        } catch (URISyntaxException e) {
            return true;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
