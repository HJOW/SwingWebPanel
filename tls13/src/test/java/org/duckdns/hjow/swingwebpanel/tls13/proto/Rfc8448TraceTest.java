package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.Rfc8448;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Ecdh;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.KeySchedule13;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Cert;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Exception;

/**
 * The TLS 1.3 handshake against the recorded traces of RFC 8448 (plan item P32-05-a). The client of the module is given the private
 * keys and the ClientHello of a trace and is fed the records that the server of the trace sent; because everything else in TLS 1.3 is
 * deterministic, every record that the client writes must equal the record of the trace byte for byte: the Finished record, the
 * application data and the closing alert. The traces are the simple 1-RTT handshake (section 3), the HelloRetryRequest (section 5), the
 * handshake with a certificate request (section 6) and the middlebox compatibility mode (section 7). The certificate of the traces has a
 * 1024 bit RSA key that no trust store accepts, so the certificate check is replaced by one that only parses the certificate; the
 * signature of CertificateVerify is still verified with its key.
 * <p>
 * RFC 8448 의 기록된 추적으로 본 TLS 1.3 핸드셰이크(계획 P32-05-a). 모듈의 클라이언트에 추적의 개인 키와 ClientHello 를 주고 추적의 서버가 보낸
 * 레코드를 먹인다. TLS 1.3 의 나머지는 모두 결정적이므로 클라이언트가 쓰는 모든 레코드(Finished 레코드, 응용 데이터, 닫는 경보)가 추적의 레코드와
 * 바이트 단위로 같아야 한다. 추적은 단순 1-RTT 핸드셰이크(3절), HelloRetryRequest(5절), 인증서 요청이 있는 핸드셰이크(6절), 미들박스 호환
 * 모드(7절)이다. 추적의 인증서는 어떤 신뢰 저장소도 받지 않는 1024비트 RSA 키를 쓰므로 인증서 검사를 인증서 해석만 하는 것으로 바꾼다.
 * CertificateVerify 의 서명은 그 키로 그대로 검증한다.
 */
public class Rfc8448TraceTest {

    /**
     * A ClientHello state with the keys of a trace.
     * <p>
     * 추적의 키를 가진 ClientHello 상태.
     */
    private static final class TraceHello extends ClientHello {

        /** The P-256 private key of the trace, or null. / 추적의 P-256 개인 키 또는 null. */
        private final byte[] p256Private;

        /**
         * Creates the state from the first ClientHello of the trace.
         * <p>
         * 추적의 첫 ClientHello 로 상태를 만든다.
         *
         * @param parameters  the settings / 설정
         * @param clientHello the first ClientHello message of the trace / 추적의 첫 ClientHello 메시지
         * @param x25519      the X25519 private key / X25519 개인 키
         * @param p256        the P-256 private key or null / P-256 개인 키 또는 null
         */
        TraceHello(Parameters parameters, byte[] clientHello, byte[] x25519, byte[] p256) {
            super(parameters, "server");
            this.random = new byte[32];
            System.arraycopy(clientHello, 6, random, 0, 32);
            this.sessionId = new byte[clientHello[38] & 0xff];
            System.arraycopy(clientHello, 39, sessionId, 0, sessionId.length);
            this.x25519Private = x25519;
            this.p256Private = p256;
        }

        /**
         * Returns the key pair of the trace.
         * <p>
         * 추적의 키 쌍을 반환한다.
         *
         * @param curve the curve / 곡선
         * @return the key pair / 키 쌍
         */
        protected Ecdh newEcdh(EcCurve curve) {
            return new Ecdh(curve, new BigInteger(1, p256Private));
        }
    }

    /**
     * A handshake that sends the ClientHello messages of a trace.
     * <p>
     * 추적의 ClientHello 메시지를 보내는 핸드셰이크.
     */
    private static final class TraceHandshake extends ClientHandshake {

        /** The ClientHello state. / ClientHello 상태. */
        private final ClientHello hello;

        /** The ClientHello messages of the trace in order. / 추적의 ClientHello 메시지(순서대로). */
        private final List<byte[]> hellos;

        /** The index of the next ClientHello. / 다음 ClientHello 의 번호. */
        private int next;

        /**
         * Creates the handshake.
         * <p>
         * 핸드셰이크를 만든다.
         *
         * @param records    the record layer / 레코드 계층
         * @param io         the handshake reader and writer / 핸드셰이크 읽기·쓰기 도구
         * @param parameters the settings / 설정
         * @param hello      the ClientHello state / ClientHello 상태
         * @param hellos     the ClientHello messages of the trace / 추적의 ClientHello 메시지
         */
        TraceHandshake(RecordLayer records, HandshakeIo io, Parameters parameters, ClientHello hello, List<byte[]> hellos) {
            super(records, io, parameters, "server", hello);
            this.hello = hello;
            this.hellos = hellos;
        }

        /**
         * Returns the recorded ClientHello; the own one is built only to select the key share group.
         * <p>
         * 기록된 ClientHello 를 반환한다. 자체 메시지는 키 공유 그룹을 정하려고만 만든다.
         *
         * @param retryGroup the group of the HelloRetryRequest or 0 / HelloRetryRequest 의 그룹 또는 0
         * @param cookie     the cookie or null / 쿠키 또는 null
         * @return the message of the trace / 추적의 메시지
         */
        protected byte[] clientHelloMessage(int retryGroup, byte[] cookie) {
            hello.build(retryGroup, cookie);
            return hellos.get(next++);
        }

        /**
         * Also accepts record_size_limit, which the ClientHello of the traces offers.
         * <p>
         * 추적의 ClientHello 가 제안하는 record_size_limit 도 받아들인다.
         *
         * @param type the extension type / 확장 형식
         * @return true if acceptable / 받아들일 수 있으면 true
         */
        protected boolean offeredInEncryptedExtensions(int type) {
            return type == 28 || super.offeredInEncryptedExtensions(type);
        }
    }

    /**
     * One replay: the client of the module connected to the recorded records of the server.
     * <p>
     * 재현 하나: 서버의 기록된 레코드에 연결한 모듈의 클라이언트.
     */
    private static final class Replay {

        /** The section of the trace. / 추적의 절. */
        final String section;

        /** Everything that the client wrote. / 클라이언트가 쓴 모든 것. */
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        /** The handshake. / 핸드셰이크. */
        final TraceHandshake handshake;

        /** The connection. / 연결. */
        final Connection connection;

        /**
         * Prepares the replay of a section.
         * <p>
         * 절의 재현을 준비한다.
         *
         * @param section the section number with the dot / 점을 포함한 절 번호
         */
        Replay(String section) {
            this.section = section;
            Rfc8448 rfc = Rfc8448.load();
            List<byte[]> hellos = new ArrayList<byte[]>();
            ByteArrayOutputStream server = new ByteArrayOutputStream();
            byte[] p256 = null;
            List<Rfc8448.Step> steps = rfc.steps(section);
            // 절의 단계에서 ClientHello 메시지, P-256 키, 서버가 보낸 레코드를 모은다.
            for (int i = 0; i < steps.size(); i++) {
                String title = steps.get(i).title().replaceAll("\\s+", " ");
                if (title.equals("{client} construct a ClientHello handshake message")) {
                    hellos.add(steps.get(i).get("ClientHello"));
                } else if (title.equals("{client} create an ephemeral P-256 key pair")) {
                    p256 = steps.get(i).get("private key");
                } else if (title.startsWith("{server} send ")) {
                    byte[] record = steps.get(i).get("complete record");
                    server.write(record, 0, record.length);
                }
            }
            byte[] x25519 = rfc.step(section, "{client} create an ephemeral x25519 key pair", 0).get("private key");
            Parameters parameters = new Parameters(null);
            parameters.checker = new Parameters.CertificateChecker() {
                public List<X509Cert> check(List<byte[]> chain, String host, long now) throws X509Exception {
                    List<X509Cert> path = new ArrayList<X509Cert>();
                    path.add(X509Cert.parse(chain.get(0)));
                    return path;
                }
            };
            RecordLayer records = new RecordLayer(new ByteArrayInputStream(server.toByteArray()), out);
            HandshakeIo io = new HandshakeIo(records);
            handshake = new TraceHandshake(records, io, parameters, new TraceHello(parameters, hellos.get(0), x25519, p256), hellos);
            connection = new Connection(records, io, handshake);
        }

        /**
         * Returns the records that the client of the trace sent, joined.
         * <p>
         * 추적의 클라이언트가 보낸 레코드를 이어서 반환한다.
         *
         * @return the bytes / 바이트
         */
        byte[] expectedOutput() {
            ByteArrayOutputStream client = new ByteArrayOutputStream();
            List<Rfc8448.Step> steps = Rfc8448.load().steps(section);
            // 클라이언트가 보낸 레코드를 순서대로 잇는다.
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).title().replaceAll("\\s+", " ").startsWith("{client} send ")) {
                    byte[] record = steps.get(i).get("complete record");
                    client.write(record, 0, record.length);
                }
            }
            return client.toByteArray();
        }

        /**
         * Returns an item of a step of the section.
         * <p>
         * 절의 단계에 있는 항목을 반환한다.
         *
         * @param title the step title / 단계 제목
         * @param nth   the occurrence of the step / 단계의 순번
         * @param name  the item name / 항목 이름
         * @return the octets / 옥텟
         */
        byte[] item(String title, int nth, String name) {
            return Rfc8448.load().step(section, title, nth).get(name);
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public Rfc8448TraceTest() {
    }

    /**
     * Section 3, the simple 1-RTT handshake: the handshake completes with the secrets of the trace, a NewSessionTicket is ignored,
     * application data goes both ways, the close_notify of the server is answered, and all records of the client equal the trace.
     * <p>
     * 3절, 단순 1-RTT 핸드셰이크: 추적의 비밀로 핸드셰이크가 끝나고, NewSessionTicket 을 무시하며, 응용 데이터가 양방향으로 오가고, 서버의
     * close_notify 에 답하며, 클라이언트의 모든 레코드가 추적과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSimpleHandshake() throws Exception {
        Replay r = new Replay("3.");
        r.connection.handshake();
        Check.isTrue(r.connection.isEstablished(), "established");
        Check.equal("TLSv1.3", r.connection.protocol(), "protocol");
        Check.equal("TLS_AES_128_GCM_SHA256", r.connection.suite().name(), "cipher suite");
        Check.equal(1, r.connection.peerChain().size(), "one certificate");
        Check.equal(Hex.encode(r.item("{server} derive secret \"tls13 c ap traffic\"", 0, "expanded")), Hex.encode(r.handshake.clientApplicationSecret()),
                "client application traffic secret");
        Check.equal(Hex.encode(r.item("{server} derive secret \"tls13 s ap traffic\"", 0, "expanded")), Hex.encode(r.handshake.serverApplicationSecret()),
                "server application traffic secret");

        byte[] data = r.item("{client} send application_data record", 0, "payload");
        r.connection.write(data, 0, data.length);
        byte[] got = new byte[100];
        int n = r.connection.read(got, 0, got.length);
        Check.equal(Hex.encode(r.item("{server} send application_data record", 0, "payload")), Hex.encode(java.util.Arrays.copyOf(got, n)),
                "application data of the server (after the ignored NewSessionTicket)");
        Check.equal(-1, r.connection.read(got, 0, got.length), "close_notify of the server ends the stream");
        Check.equal(-1, r.connection.read(got, 0, got.length), "and it stays ended");
        Check.equal(Hex.encode(r.expectedOutput()), Hex.encode(r.out.toByteArray()), "ClientHello, Finished, application data and alert records of the client");
    }

    /**
     * Section 5, HelloRetryRequest: the client answers with a P-256 key share and the cookie, the transcript starts with the synthetic
     * message hash, and the records of the client (second ClientHello with record version 0x0303, Finished, alert) equal the trace.
     * <p>
     * 5절, HelloRetryRequest: 클라이언트가 P-256 키 공유와 쿠키로 답하고, 전사가 합성 message_hash 로 시작하며, 클라이언트의 레코드(레코드 버전
     * 0x0303 의 둘째 ClientHello, Finished, 경보)가 추적과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHelloRetryRequest() throws Exception {
        Replay r = new Replay("5.");
        r.connection.handshake();
        Check.equal("TLS_AES_128_GCM_SHA256", r.connection.suite().name(), "cipher suite");
        Check.equal(Hex.encode(r.item("{server} derive secret \"tls13 c ap traffic\"", 0, "expanded")), Hex.encode(r.handshake.clientApplicationSecret()),
                "client application traffic secret");
        Check.equal(-1, r.connection.read(new byte[10], 0, 10), "close_notify of the server");
        Check.equal(Hex.encode(r.expectedOutput()), Hex.encode(r.out.toByteArray()), "both ClientHello records, Finished and alert of the client");
    }

    /**
     * Section 7, middlebox compatibility mode: the change_cipher_spec of the server is dropped, the client sends its own before the
     * Finished, and {@link Connection#close()} sends the close_notify of the trace.
     * <p>
     * 7절, 미들박스 호환 모드: 서버의 change_cipher_spec 을 버리고, 클라이언트는 Finished 앞에 자기 것을 보내며, {@link Connection#close()} 가
     * 추적의 close_notify 를 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompatibilityMode() throws Exception {
        Replay r = new Replay("7.");
        r.connection.handshake();
        Check.equal(32, r.connection.sessionId().length, "the trace uses a session identifier");
        r.connection.close();
        r.connection.close();
        Check.equal(Hex.encode(r.expectedOutput()), Hex.encode(r.out.toByteArray()), "ClientHello, change_cipher_spec, Finished and one alert of the client");
        Check.equal(-1, r.connection.read(new byte[10], 0, 10), "a closed connection reads the end of the stream");
        final Replay closed = r;
        Check.throwsException(java.io.IOException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                closed.connection.write(new byte[1], 0, 1);
            }
        }, "writing after close");
    }

    /**
     * Section 6, certificate request: the module has no client certificate, so it answers with an empty Certificate message and a
     * Finished over it. The trace cannot be equal here; instead the flight of the client is decrypted with the client handshake keys of
     * the trace and compared with the expected messages, and the alert of the server (which does not depend on the flight of the
     * client) still decrypts.
     * <p>
     * 6절, 인증서 요청: 모듈에는 클라이언트 인증서가 없으므로 빈 Certificate 메시지와 그 위의 Finished 로 답한다. 여기서는 추적과 같을 수 없다. 대신
     * 클라이언트의 비행을 추적의 클라이언트 핸드셰이크 키로 복호화해 기대하는 메시지와 비교하고, 서버의 경보(클라이언트의 비행에 좌우되지 않는다)가
     * 그대로 복호화되는지 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCertificateRequestIsAnsweredWithAnEmptyCertificate() throws Exception {
        Replay r = new Replay("6.");
        r.connection.handshake();
        Check.equal(-1, r.connection.read(new byte[10], 0, 10), "close_notify of the server");

        CipherSuite suite = r.connection.suite();
        RecordLayer reader = new RecordLayer(new ByteArrayInputStream(r.out.toByteArray()), new ByteArrayOutputStream());
        RecordLayer.Record hello = reader.read();
        Check.equal(RecordLayer.HANDSHAKE, hello.type, "first record");
        byte[] clientHello = r.item("{client} construct a ClientHello handshake message", 0, "ClientHello");
        Check.equal(Hex.encode(clientHello), Hex.encode(hello.data), "ClientHello of the trace");
        reader.setReadProtection(suite, true, r.item("{server} derive read traffic keys for handshake data", 0, "key expanded"),
                r.item("{server} derive read traffic keys for handshake data", 0, "iv expanded"));
        RecordLayer.Record certificate = reader.read();
        Check.equal(RecordLayer.HANDSHAKE, certificate.type, "second record");
        Check.equal("0b00000400000000", Hex.encode(certificate.data), "Certificate with an empty context and an empty list");
        RecordLayer.Record finished = reader.read();
        HashAlg hash = suite.hash();
        byte[] transcript = org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes.concat(clientHello,
                r.item("{server} construct a ServerHello handshake message", 0, "ServerHello"), r.item("{server} send handshake record", 1, "payload"),
                certificate.data);
        byte[] mac = KeySchedule13.finishedMac(hash, r.item("{server} derive secret \"tls13 c hs traffic\"", 0, "expanded"), hash.digest(transcript));
        Check.equal("14000020" + Hex.encode(mac), Hex.encode(finished.data), "Finished over the transcript with the empty Certificate");
    }
}
