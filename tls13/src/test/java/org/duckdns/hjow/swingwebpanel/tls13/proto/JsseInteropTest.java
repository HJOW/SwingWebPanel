package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Failures;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Exception;

/**
 * Exercises the legacy JVM client against a separate, independent JSSE server.
 * / 별도 독립 JSSE 서버를 상대로 옛 JVM의 클라이언트를 검증한다.
 */
public final class JsseInteropTest {

    /** First TLS 1.3 suite and performance baseline. / 첫 TLS 1.3 스위트이자 성능 기준. */
    public static final String AES_13 = "TLS_AES_128_GCM_SHA256";

    /** An authenticated AEAD suite for TLS 1.2. / TLS 1.2 인증 AEAD 스위트. */
    public static final String RSA_GCM_12 = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";

    /** A legacy suite that the module does not offer. / 모듈이 제안하지 않는 옛 스위트. */
    public static final String RSA_CBC = "TLS_RSA_WITH_AES_128_CBC_SHA";

    /** Groups accepted by the independent server in separate runs. / 독립 서버가 실행 별로 허용하는 그룹. */
    private static final String[] GROUPS = { "x25519", "secp256r1", "secp384r1" };

    /** Fixed random source for loopback handshake tests only. / 루프백 핸드셰이크 시험 전용 고정 난수원. */
    private static final class FixedRandom extends SecureRandom {
        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /** Generator seeded afresh for each test connection. / 시험 연결마다 같은 시드로 시작하는 생성기. */
        private final Random random = new Random(326);

        /** Creates the test generator. / 시험용 생성기를 만든다. */
        FixedRandom() {
        }

        /**
         * Fills the requested bytes. / 요청된 바이트를 채운다.
         *
         * @param bytes destination / 대상 배열
         */
        public void nextBytes(byte[] bytes) {
            random.nextBytes(bytes);
        }
    }

    /** Creates a regression test instance. / 회귀 시험 인스턴스를 만든다. */
    public JsseInteropTest() {
    }

    /** Measures the first handshake and warm transfers against JSSE. / JSSE와 첫 핸드셰이크 및 예열 뒤 전송을 측정한다. */
    public void testBenchmark() throws Exception {
        JsseFixture server = new JsseFixture("TLSv1.3", AES_13, GROUPS[0], "normal", false);
        try {
            long cold = exchange(server, "TLSv1.3", AES_13, "normal", false)[0];
            long[] warm = new long[5];
            double slowest = Double.MAX_VALUE;
            // 같은 서버에서 새 핸드셰이크와 4 MiB 수신을 다섯 번 측정한다.
            for (int i = 0; i < warm.length; i++) {
                long[] result = exchange(server, "TLSv1.3", AES_13, "normal", false);
                warm[i] = result[0];
                slowest = Math.min(slowest, JsseServer.BULK_SIZE * 1000.0 / result[1]);
            }
            Arrays.sort(warm);
            System.out.printf(java.util.Locale.US, "[tls-bench] %s first %.1f ms, warm median %.1f ms, slowest %.2f MB/s%n",
                    System.getProperty("java.version"), cold / 1e6, warm[2] / 1e6, slowest);
        } finally {
            server.close();
        }
    }

    /** Tests all TLS 1.3 suites, all groups and server behaviors. / 모든 TLS 1.3 스위트·그룹과 서버 동작을 검증한다. */
    public void testTls13() throws Exception {
        run("TLSv1.3", AES_13, GROUPS[0], "want-auth", false);
        // x25519만 첫 ClientHello에 키 공유를 담으므로 다른 두 그룹은 HelloRetryRequest를 거친다.
        run("TLSv1.3", "TLS_AES_256_GCM_SHA384", GROUPS[1], "key-update", false);
        run("TLSv1.3", "TLS_CHACHA20_POLY1305_SHA256", GROUPS[2], "abrupt", true);
    }

    /** Tests all TLS 1.2 suites and groups, with and without EMS. / 모든 TLS 1.2 스위트·그룹과 EMS 유무를 검증한다. */
    public void testTls12() throws Exception {
        // 각 스위트에서 JSSE가 ServerKeyExchange와 Finished를 만든다.
        for (CipherSuite suite : CipherSuite.all()) {
            if (suite.isTls13()) {
                continue;
            }
            boolean ecdsa = suite.auth() == CipherSuite.AUTH_ECDSA;
            String mode = suite.name().equals(RSA_GCM_12) ? "no-ems" : "normal";
            // TLS 1.2의 JSSE는 서버 인증서의 EC 곡선도 활성 그룹에 포함해야 한다.
            String group = ecdsa ? GROUPS[1] : suite.hash() == org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg.SHA384 ? GROUPS[2] : GROUPS[0];
            run("TLSv1.2", suite.name(), group, mode, ecdsa);
        }
    }

    /** Validates both server certificate rejection paths. / 서버 인증서 거부 경로 두 가지를 검증한다. */
    public void testCertificates() throws Exception {
        reject("TLSv1.3", AES_13, "expired", TlsFixture.HOST, X509Exception.Reason.EXPIRED);
        reject("TLSv1.3", AES_13, "normal", "other.example.test", X509Exception.Reason.NAME_MISMATCH);
    }

    /** Distinguishes unsupported negotiation from a bad certificate. / 지원 밖 협상과 잘못된 인증서를 구분한다. */
    public void testUnsupported() throws Exception {
        reject("TLSv1.1", RSA_CBC, "normal", TlsFixture.HOST, null);
        reject("TLSv1.2", RSA_CBC, "normal", TlsFixture.HOST, null);
    }

    /**
     * Runs one forced server configuration. / 한 가지로 제한한 서버 설정을 검증한다.
     *
     * @param protocol server protocol / 서버 프로토콜
     * @param suite server suite / 서버 스위트
     * @param group server group / 서버 그룹
     * @param mode test behavior / 시험 동작
     * @param ec true for EC authentication / EC 인증이면 true
     * @throws Exception on failure / 실패 시
     */
    private static void run(String protocol, String suite, String group, String mode, boolean ec) throws Exception {
        JsseFixture server = new JsseFixture(protocol, suite, group, mode, ec);
        try {
            exchange(server, protocol, suite, mode, true);
        } finally {
            server.close();
        }
    }

    /**
     * Checks handshake identity and the advertised large HTTP response, and returns elapsed times.
     * / 협상 결과와 길이를 예고한 큰 HTTP 응답을 확인하고 소요 시간을 반환한다.
     *
     * @param server running server / 실행 중인 서버
     * @param protocol expected protocol / 기대 프로토콜
     * @param suite expected cipher suite / 기대 스위트
     * @param mode expected server behavior / 기대 서버 동작
     * @param fixed true to seed the test client deterministically / 시험 클라이언트에 고정 시드를 쓸 때 true
     * @return handshake and download nanoseconds / 핸드셰이크와 다운로드 소요 나노초
     * @throws Exception on failure / 실패 시
     */
    private static long[] exchange(JsseFixture server, String protocol, String suite, String mode, boolean fixed) throws Exception {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", server.port()), 5000);
            socket.setSoTimeout(20000);
            long start = System.nanoTime();
            Parameters parameters = TlsFixture.parameters();
            if (fixed) {
                parameters.random = new FixedRandom();
            }
            RecordLayer records = new RecordLayer(socket.getInputStream(), socket.getOutputStream());
            HandshakeIo io = new HandshakeIo(records);
            Connection tls = new Connection(records, io, new ClientHandshake(records, io, parameters, TlsFixture.HOST));
            tls.handshake();
            long handshake = System.nanoTime() - start;
            Check.equal(protocol, tls.protocol(), "negotiated protocol");
            Check.equal(suite, tls.suite().name(), "negotiated cipher suite");
            Field protection = RecordLayer.class.getDeclaredField("read");
            protection.setAccessible(true);
            Object initialKeys = protection.get(records);
            byte[] request = ("GET /bulk HTTP/1.1\r\nHost: " + TlsFixture.HOST + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII");
            start = System.nanoTime();
            tls.write(request, 0, request.length);
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            byte[] block = new byte[16384];
            String headerText = "";
            // 작은 시험 헤더에서 빈 줄을 찾아 그 뒤의 바이트만 본문으로 센다.
            while (!headerText.endsWith("\r\n\r\n") && headers.size() < 1024) {
                Check.equal(Integer.valueOf(1), Integer.valueOf(tls.read(block, 0, 1)), "HTTP header byte");
                headers.write(block[0]);
                headerText = headers.toString("US-ASCII");
            }
            Check.isTrue(headerText.startsWith("HTTP/1.1 200 OK\r\n"), "independent HTTP response");
            Check.isTrue(headerText.indexOf("Content-Length: " + JsseServer.BULK_SIZE) >= 0, "advertised HTTP body length");
            int received = 0;
            int count;
            boolean match = true;
            // 레코드 경계와 상관없이 받은 모든 바이트의 순서를 확인한다.
            while ((count = tls.read(block, 0, block.length)) != -1) {
                for (int i = 0; i < count; i++) {
                    match &= block[i] == (byte) (received + i);
                }
                received += count;
            }
            long download = System.nanoTime() - start;
            Check.isTrue(match, "bulk payload byte sequence");
            Check.equal(Integer.valueOf("abrupt".equals(mode) ? block.length : JsseServer.BULK_SIZE), Integer.valueOf(received), "received byte count");
            if ("key-update".equals(mode)) {
                Check.isTrue(initialKeys != protection.get(records), "JSSE post-handshake KeyUpdate changed the read keys");
            }
            tls.close();
            return new long[] { handshake, Math.max(1, download) };
        } finally {
            socket.close();
        }
    }

    /**
     * Checks that the rejected server yields the expected failure type. / 서버 거부 이유의 종류가 맞는지 확인한다.
     *
     * @param protocol server protocol / 서버 프로토콜
     * @param suite server suite / 서버 스위트
     * @param mode test behavior / 시험 동작
     * @param host name that the client verifies / 클라이언트가 검증할 이름
     * @param reason certificate failure or null for negotiation / 인증서 실패 이유 또는 협상 실패이면 null
     * @throws Exception on unexpected failure / 예상 밖 실패 시
     */
    private static void reject(String protocol, String suite, String mode, String host, X509Exception.Reason reason) throws Exception {
        JsseFixture server = new JsseFixture(protocol, suite, GROUPS[0], mode, false);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", server.port()), 5000);
            socket.setSoTimeout(10000);
            Connection tls = new Connection(socket.getInputStream(), socket.getOutputStream(), TlsFixture.parameters(), host);
            try {
                tls.handshake();
                throw new AssertionError("independent server unexpectedly accepted: " + protocol + " " + suite + " " + mode);
            } catch (Tls13Exception e) {
                if (reason == null) {
                    Check.isTrue(Tls13Failures.isNegotiationFailure(e), "module negotiation failure: " + e);
                } else {
                    Check.equal(Tls13Exception.Kind.CERTIFICATE, e.kind(), "failure class");
                    Check.equal(reason, ((X509Exception) e.getCause()).reason(), "certificate failure reason");
                }
            }
        } finally {
            socket.close();
            server.close();
        }
    }
}
