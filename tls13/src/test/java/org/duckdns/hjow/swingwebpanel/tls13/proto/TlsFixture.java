package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.x509.TrustStore;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Exception;

/**
 * Shared material of the connection tests: a test root, server certificates with RSA and EC keys for the names
 * <code>www.example.test</code>, <code>localhost</code> and <code>127.0.0.1</code>, the settings of a client that trusts the root, and
 * helpers that connect a client of the module to a {@link ScriptedServer}. The certificates are valid around the current time, so that
 * tests which use the system clock (the socket factory) accept them too.
 * <p>
 * 연결 시험의 공유 자료: 시험용 루트, 이름 <code>www.example.test</code>, <code>localhost</code>, <code>127.0.0.1</code> 에 대한 RSA·EC 키의 서버
 * 인증서, 그 루트를 신뢰하는 클라이언트 설정, 모듈의 클라이언트를 {@link ScriptedServer} 에 연결하는 도우미. 인증서는 현재 시각 주변에서 유효하므로
 * 시스템 시계를 쓰는 시험(소켓 팩터리)도 받아들인다.
 */
final class TlsFixture {

    /** The host name that the clients ask for. / 클라이언트가 요청하는 호스트 이름. */
    static final String HOST = "www.example.test";

    /** The key of the test root. / 시험용 루트의 키. */
    private static final TestKey ROOT_KEY = TestKey.rsa("tls-root");

    /** The certificate of the test root, created on first use. / 시험용 루트의 인증서. 처음 쓸 때 만든다. */
    private static byte[] rootCertificate;

    /**
     * A connected client.
     * <p>
     * 연결된 클라이언트.
     */
    static final class Client {

        /** The socket to the server. / 서버로 가는 소켓. */
        final Socket socket;

        /** The connection of the module. / 모듈의 연결. */
        final Connection connection;

        /**
         * Creates the client.
         * <p>
         * 클라이언트를 만든다.
         *
         * @param socket     the socket / 소켓
         * @param connection the connection / 연결
         */
        Client(Socket socket, Connection connection) {
            this.socket = socket;
            this.connection = connection;
        }

        /**
         * Writes application data.
         * <p>
         * 응용 데이터를 쓴다.
         *
         * @param data the data / 데이터
         * @throws IOException on a failure / 실패 시
         */
        void write(byte[] data) throws IOException {
            connection.write(data, 0, data.length);
        }

        /**
         * Reads application data until a number of bytes arrived or the stream ended.
         * <p>
         * 정한 바이트 수가 오거나 스트림이 끝날 때까지 응용 데이터를 읽는다.
         *
         * @param n the number of bytes / 바이트 수
         * @return the data, shorter at the end of the stream / 데이터. 스트림이 끝났으면 더 짧다
         * @throws IOException on a failure / 실패 시
         */
        byte[] read(int n) throws IOException {
            byte[] buf = new byte[n];
            int have = 0;
            // 원하는 만큼 찰 때까지 읽는다.
            while (have < n) {
                int r = connection.read(buf, have, n - have);
                if (r < 0) {
                    break;
                }
                have += r;
            }
            return Arrays.copyOf(buf, have);
        }

        /**
         * Sends close_notify and closes the socket.
         * <p>
         * close_notify 를 보내고 소켓을 닫는다.
         *
         * @throws IOException on a failure / 실패 시
         */
        void close() throws IOException {
            connection.close();
            socket.close();
        }
    }

    /**
     * Not instantiated.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private TlsFixture() {
    }

    /**
     * Returns the certificate of the test root.
     * <p>
     * 시험용 루트의 인증서를 반환한다.
     *
     * @return the DER certificate / DER 인증서
     */
    static synchronized byte[] root() {
        if (rootCertificate == null) {
            long now = System.currentTimeMillis();
            rootCertificate = new CertBuilder("TLS Test Root", ROOT_KEY).selfSigned().ca(-1).validity(now - CertBuilder.DAY, now + 3650 * CertBuilder.DAY).build();
        }
        return rootCertificate;
    }

    /**
     * Returns the RSA key of the server.
     * <p>
     * 서버의 RSA 키를 반환한다.
     *
     * @return the key / 키
     */
    static TestKey rsaKey() {
        return TestKey.rsa("tls-server");
    }

    /**
     * Returns the P-256 key of the server.
     * <p>
     * 서버의 P-256 키를 반환한다.
     *
     * @return the key / 키
     */
    static TestKey p256Key() {
        return TestKey.ec("tls-server", EcCurve.P256);
    }

    /**
     * Returns the P-384 key of the server.
     * <p>
     * 서버의 P-384 키를 반환한다.
     *
     * @return the key / 키
     */
    static TestKey p384Key() {
        return TestKey.ec("tls-server", EcCurve.P384);
    }

    /**
     * Builds the certificate of a server key, issued by the test root, for the test names.
     * <p>
     * 서버 키의 인증서를 만든다. 시험용 루트가 발급하고 시험용 이름들을 담는다.
     *
     * @param key the server key / 서버 키
     * @return the builder, to change it before building / 빌더. 만들기 전에 바꿀 수 있다
     */
    static CertBuilder certificate(TestKey key) {
        long now = System.currentTimeMillis();
        return new CertBuilder(HOST, key).issuer("TLS Test Root", ROOT_KEY).dns(HOST, "localhost").ip(new byte[] { 127, 0, 0, 1 })
                .validity(now - CertBuilder.DAY, now + 30 * CertBuilder.DAY);
    }

    /**
     * Returns the certificate list of a server key.
     * <p>
     * 서버 키의 인증서 목록을 반환한다.
     *
     * @param key the server key / 서버 키
     * @return the list with the server certificate / 서버 인증서를 담은 목록
     */
    static List<byte[]> chain(TestKey key) {
        List<byte[]> chain = new ArrayList<byte[]>();
        chain.add(certificate(key).build());
        return chain;
    }

    /**
     * Returns the trust store with the test root.
     * <p>
     * 시험용 루트를 담은 신뢰 저장소를 반환한다.
     *
     * @return the store / 저장소
     */
    static TrustStore trust() {
        try {
            return TrustStore.ofCertificates(Arrays.asList(root()));
        } catch (X509Exception e) {
            throw new IllegalStateException("the test root is not usable: " + e);
        }
    }

    /**
     * Returns the settings of a client that trusts the test root.
     * <p>
     * 시험용 루트를 신뢰하는 클라이언트의 설정을 반환한다.
     *
     * @return the settings / 설정
     */
    static Parameters parameters() {
        return new Parameters(trust());
    }

    /**
     * Creates a server with the certificate of a key (not yet started).
     * <p>
     * 키의 인증서를 가진 서버를 만든다(아직 시작하지 않는다).
     *
     * @param key the server key / 서버 키
     * @return the server / 서버
     */
    static ScriptedServer server(TestKey key) {
        return new ScriptedServer(key, chain(key));
    }

    /**
     * Connects a client to a started server; the handshake is not run yet.
     * <p>
     * 시작한 서버에 클라이언트를 연결한다. 핸드셰이크는 아직 하지 않는다.
     *
     * @param server     the server / 서버
     * @param parameters the settings of the client / 클라이언트의 설정
     * @return the client / 클라이언트
     * @throws IOException if the connection fails / 연결이 실패하면
     */
    static Client connect(ScriptedServer server, Parameters parameters) throws IOException {
        Socket socket = new Socket("127.0.0.1", server.port());
        socket.setSoTimeout(20000);
        return new Client(socket, new Connection(socket.getInputStream(), socket.getOutputStream(), parameters, HOST));
    }

    /**
     * Starts the server, connects a client and expects its handshake to fail with an exception of the module.
     * <p>
     * 서버를 시작하고 클라이언트를 연결한 뒤, 핸드셰이크가 모듈의 예외로 실패함을 기대한다.
     *
     * @param what       the description of the case / 사례 설명
     * @param server     the server (not yet started) / 서버(아직 시작하지 않은 것)
     * @param parameters the settings of the client / 클라이언트의 설정
     * @return the failure / 실패
     * @throws Exception on an unexpected failure / 예상 밖의 실패 시
     */
    static Tls13Exception failure(String what, ScriptedServer server, Parameters parameters) throws Exception {
        server.start();
        final Client client = connect(server, parameters);
        try {
            final Tls13Exception e = (Tls13Exception) Check.throwsException(Tls13Exception.class, new Block() {
                public void run() throws Exception {
                    client.connection.handshake();
                }
            }, what);
            Check.isFalse(client.connection.isEstablished(), what + ": not established");
            Throwable again = Check.throwsException(Tls13Exception.class, new Block() {
                public void run() throws Exception {
                    client.connection.read(new byte[1], 0, 1);
                }
            }, what + ": reading after the failure");
            Check.same(e, again, what + ": the same failure is reported again");
            return e;
        } finally {
            server.join();
            client.socket.close();
        }
    }
}
