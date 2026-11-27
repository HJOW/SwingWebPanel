package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Independent JSSE server run by the tests in a newer JVM. It uses only JRE APIs.
 * / 시험이 더 새 JVM에서 실행하는 독립 JSSE 서버. JRE API만 사용한다.
 */
public final class JsseServer {

    /** Number of bytes advertised by the bulk route. / 대용량 경로가 예고하는 바이트 수. */
    static final int BULK_SIZE = 4 * 1024 * 1024;

    /** Fixed title served to the browser tests. / 브라우저 시험에 반환하는 고정 제목. */
    public static final String TITLE = "Independent JSSE test page";

    /** Utility class. / 인스턴스를 만들지 않는다. */
    private JsseServer() {
    }

    /**
     * Starts the loopback server and announces its ephemeral port on standard output.
     * / 루프백 서버를 시작하고 할당된 포트를 표준 출력에 알린다.
     *
     * @param args key file, algorithm, protocol, suite and mode / 키 파일, 알고리즘, 프로토콜, 스위트, 모드
     * @throws Exception on startup or an unexpected server failure / 시작 또는 예상 밖의 서버 실패 시
     */
    public static void main(String[] args) throws Exception {
        // JSSE는 TLS 1.3 서버 소켓의 두 번째 핸드셰이크를 KeyUpdate로 처리한다.
        SSLContext context = SSLContext.getInstance(args[2]);
        KeyStore keys = KeyStore.getInstance("JKS");
        keys.load(null, null);
        char[] password = "test-only".toCharArray();
        DataInputStream in = new DataInputStream(new FileInputStream(args[0]));
        try {
            PKCS8EncodedKeySpec key = new PKCS8EncodedKeySpec(readBytes(in));
            CertificateFactory parser = CertificateFactory.getInstance("X.509");
            Certificate[] chain = new Certificate[2];
            // 서버 인증서와 시험용 루트를 독립 구현의 X.509 해석기로 읽는다.
            for (int i = 0; i < chain.length; i++) {
                chain[i] = parser.generateCertificate(new java.io.ByteArrayInputStream(readBytes(in)));
            }
            keys.setKeyEntry("server", KeyFactory.getInstance(args[1]).generatePrivate(key), password, chain);
        } finally {
            in.close();
        }
        KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keys, password);
        context.init(managers.getKeyManagers(), null, null);
        ServerSocket server = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        try {
            System.out.println("port " + server.getLocalPort());
            // 연결마다 같은 제약으로 새 TLS 핸드셰이크를 실행한다.
            while (true) {
                Socket raw = server.accept();
                raw.setSoTimeout(10000);
                try {
                    int first = raw.getInputStream().read();
                    // 평문 HTTP에도 응답해 네트워크 라이브러리의 재전송과 브라우저의 재시도를 구별한다.
                    if (first == 'G' || first == 'O') {
                        System.out.println("http");
                        BufferedReader plain = new BufferedReader(new InputStreamReader(raw.getInputStream(), "US-ASCII"));
                        String header;
                        // 상태를 보내기 전에 요청 헤더를 소비한다.
                        while ((header = plain.readLine()) != null && header.length() != 0) {
                            // 헤더 값은 시험에 필요하지 않다.
                        }
                        raw.getOutputStream().write("HTTP/1.1 404 Not Found\r\nConnection: close\r\nContent-Length: 0\r\n\r\n".getBytes("US-ASCII"));
                    } else if (first >= 0) {
                        System.out.println("https");
                        // 새 JSSE의 서버 계층화 API에 미리 읽은 한 바이트를 넘긴다(Java 6 소스 문법 유지).
                        SSLSocket socket = (SSLSocket) SSLSocketFactory.class
                                .getMethod("createSocket", Socket.class, InputStream.class, Boolean.TYPE)
                                .invoke(context.getSocketFactory(), raw, new ByteArrayInputStream(new byte[] { (byte) first }), Boolean.FALSE);
                        serve(socket, raw, args[2], args[3], args[4]);
                    }
                } catch (java.io.IOException expected) {
                    // 오류 응답 뒤에 연결을 닫는 클라이언트도 있으므로 개별 연결의 입출력 실패는 여기서 끝낸다.
                } finally {
                    raw.close();
                }
            }
        } finally {
            server.close();
        }
    }

    /**
     * Reads one length-prefixed key or certificate from the local test fixture.
     * / 로컬 시험 자료에서 길이가 앞에 붙은 키 또는 인증서 하나를 읽는다.
     *
     * @param in the fixture input / 시험 자료 입력
     * @return the encoded value / 부호화된 값
     * @throws java.io.IOException if the fixture cannot be read / 시험 자료를 읽을 수 없을 때
     */
    private static byte[] readBytes(DataInputStream in) throws java.io.IOException {
        byte[] value = new byte[in.readInt()];
        in.readFully(value);
        return value;
    }

    /**
     * Serves one HTTP request after the JSSE handshake.
     * / JSSE 핸드셰이크 뒤 HTTP 요청 하나에 응답한다.
     *
     * @param socket layered server socket / 계층화된 서버 소켓
     * @param raw loopback socket / 루프백 소켓
     * @param protocol enabled protocol / 활성 프로토콜
     * @param suite enabled suite / 활성 스위트
     * @param mode post-handshake behavior / 핸드셰이크 뒤의 동작
     * @throws Exception on failure / 실패 시
     */
    private static void serve(SSLSocket socket, Socket raw, String protocol, String suite, String mode) throws Exception {
        socket.setUseClientMode(false);
        socket.setEnabledProtocols(new String[] { protocol });
        socket.setEnabledCipherSuites(new String[] { suite });
        socket.setWantClientAuth("want-auth".equals(mode));
        socket.setSoTimeout(10000);
        try {
            socket.startHandshake();
            // 앞의 핸드셰이크가 끝난 뒤 서버 쪽 쓰기 키를 갱신한다.
            if ("key-update".equals(mode)) {
                socket.startHandshake();
            }
            BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
            String line = request.readLine();
            if (line == null) {
                return;
            }
            String origin = null;
            String requestHeaders = null;
            String requestMethod = null;
            String field;
            // 요청 헤더를 끝까지 읽고 CORS 사전 요청에 필요한 값만 보관한다.
            while ((field = request.readLine()) != null && field.length() != 0) {
                String lower = field.toLowerCase(java.util.Locale.ENGLISH);
                if (lower.startsWith("origin:")) {
                    origin = field.substring(7).trim();
                } else if (lower.startsWith("access-control-request-headers:")) {
                    requestHeaders = field.substring(31).trim();
                } else if (lower.startsWith("access-control-request-method:")) {
                    requestMethod = field.substring(30).trim();
                }
            }
            boolean bulk = line.startsWith("GET /bulk ");
            boolean preflight = line.startsWith("OPTIONS ");
            byte[] page = html(line).getBytes("US-ASCII");
            String headers = (preflight ? "HTTP/1.1 204 No Content" : "HTTP/1.1 200 OK") + "\r\nConnection: close\r\nContent-Type: "
                    + (bulk ? "application/octet-stream" : "text/html") + "\r\nContent-Length: " + (bulk ? BULK_SIZE : preflight ? 0 : page.length);
            if (origin != null) {
                headers += "\r\nAccess-Control-Allow-Origin: " + origin;
            }
            if (preflight) {
                headers += "\r\nAccess-Control-Allow-Methods: " + requestMethod + "\r\nAccess-Control-Allow-Headers: " + requestHeaders;
            }
            OutputStream out = socket.getOutputStream();
            out.write((headers + "\r\n\r\n").getBytes("US-ASCII"));
            if (preflight) {
                return;
            }
            if (!bulk) {
                out.write(page);
                return;
            }
            byte[] block = new byte[16384];
            // 내용을 검증할 수 있도록 매 레코드에 같은 바이트 순서를 채운다.
            for (int i = 0; i < block.length; i++) {
                block[i] = (byte) i;
            }
            int length = "abrupt".equals(mode) ? block.length : BULK_SIZE;
            // 잘린 연결 모드에서는 예고한 길이보다 작은 본문 하나를 보내고 TCP를 닫는다.
            for (int i = 0; i < length; i += block.length) {
                out.write(block);
            }
            out.flush();
        } finally {
            if ("abrupt".equals(mode)) {
                raw.close();
            } else {
                socket.close();
            }
        }
    }

    /**
     * Builds the test page or the cross-origin request page given a second loopback port.
     * / 시험 페이지를 만든다. 별도 루프백 포트를 넘기면 교차 오리진 요청 페이지를 만든다.
     *
     * @param request HTTP request line / HTTP 요청 줄
     * @return test HTML / 시험 HTML
     */
    private static String html(String request) {
        if (request.startsWith("GET /cors-page?")) {
            int port = Integer.parseInt(request.substring(15, request.indexOf(' ', 15)));
            return "<title>waiting</title><script>fetch('https://127.0.0.1:" + port
                    + "/cors', {headers:{'X-Probe':'yes'}}).then(function(r){return r.text();}).then(function(t){"
                    + "document.title=t.indexOf('Independent JSSE')>=0?'cors-ok':'cors-body';},"
                    + "function(e){document.title='cors-error:'+e;});</script>";
        }
        return "<html><title>" + TITLE + "</title><body>" + TITLE + "</body></html>";
    }
}
