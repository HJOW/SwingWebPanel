package tlsvectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Reports the TLS version and HTTP status of an HTTPS HEAD request with the optional TLS module.
 * / 선택 TLS 모듈로 HTTPS HEAD 요청을 보내 TLS 버전과 HTTP 상태를 출력한다.
 */
public final class LiveProbe {

    /** No instances. / 인스턴스를 만들지 않는다. */
    private LiveProbe() {
    }

    /**
     * Checks each HTTPS address and prints success or the exception cause chain.
     * / HTTPS 주소마다 성공 또는 예외 원인 사슬을 출력한다.
     *
     * @param args HTTPS addresses / HTTPS 주소들
     * @throws Exception if the optional factory cannot be loaded / 선택 팩터리를 불러올 수 없을 때
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: LiveProbe https://host[:port][/path] ...");
            System.exit(1);
        }
        Class<?> type = Class.forName("org.duckdns.hjow.swingwebpanel.tls13.Tls13SocketFactory");
        SSLSocketFactory factory = (SSLSocketFactory) type.getMethod("getInstance").invoke(null);
        System.out.println("JRE " + System.getProperty("java.version"));
        // URL마다 검사하여 한 사이트의 실패가 다른 사이트의 검사를 막지 않게 한다.
        for (String address : args) {
            try {
                System.out.println(address + " " + probe(new URL(address), factory));
            } catch (IOException e) {
                StringBuilder causes = new StringBuilder();
                // 상위 TLS 오류와 인증서 오류의 세부 이유를 함께 출력한다.
                for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                    if (causes.length() != 0) {
                        causes.append(" <- ");
                    }
                    causes.append(cause);
                }
                System.out.println(address + " FAIL " + causes);
            }
        }
    }

    /**
     * Performs the handshake and reads one HTTP status line. / 핸드셰이크를 하고 HTTP 상태 줄 하나를 읽는다.
     *
     * @param url HTTPS destination / HTTPS 목적지
     * @param factory the socket factory under test / 검사 대상 소켓 팩터리
     * @return the protocol, suite and status / TLS 버전, 스위트, 상태
     * @throws IOException on failure / 실패 시
     */
    private static String probe(URL url, SSLSocketFactory factory) throws IOException {
        if (!"https".equals(url.getProtocol()) || url.getUserInfo() != null || url.getFile().indexOf('\r') >= 0 || url.getFile().indexOf('\n') >= 0) {
            throw new IOException("a plain HTTPS address without credentials or line breaks is required");
        }
        Socket raw = new Socket();
        int port = url.getPort() < 0 ? 443 : url.getPort();
        try {
            raw.connect(new InetSocketAddress(url.getHost(), port), 8000);
            raw.setSoTimeout(10000);
            SSLSocket socket = (SSLSocket) factory.createSocket(raw, url.getHost(), port, true);
            try {
                socket.startHandshake();
                String request = "HEAD " + (url.getFile().length() == 0 ? "/" : url.getFile()) + " HTTP/1.1\r\nHost: " + url.getAuthority()
                        + "\r\nUser-Agent: swingwebpanel-tls-probe/1.0\r\nConnection: close\r\n\r\n";
                socket.getOutputStream().write(request.getBytes("US-ASCII"));
                String status = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII")).readLine();
                if (status == null || !status.startsWith("HTTP/1.")) {
                    throw new IOException("no HTTP/1.x status");
                }
                return socket.getSession().getProtocol() + " " + socket.getSession().getCipherSuite() + " " + status;
            } finally {
                socket.close();
            }
        } finally {
            raw.close();
        }
    }
}
