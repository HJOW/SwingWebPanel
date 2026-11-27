package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Aead;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Ecdh;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.KeySchedule13;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Prf12;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.X25519;

/**
 * A TLS 1.3 and TLS 1.2 server for the tests of the client: it listens on a loopback port, serves the connections on its own thread
 * and can be told to misbehave ({@link Fault}) or to follow a {@link Script} after the handshake. It is a test tool and not a real
 * server: it trusts the ClientHello, has one certificate and no error handling beyond recording what went wrong. It writes its records
 * with its own code (so that it can pad, fragment and damage them, and use random explicit nonces in TLS 1.2) and reads the records of
 * the client with the record layer of the module. Usage:
 * <pre>
 * ScriptedServer server = new ScriptedServer(key, chain);
 * server.tls13 = false;
 * server.suite = 0xc02f;
 * server.start();
 * ... connect a client to server.port() ...
 * server.finish();          // joins the thread and rethrows what the server recorded as an error
 * </pre>
 * <p>
 * 클라이언트 시험용 TLS 1.3·TLS 1.2 서버: 루프백 포트에서 듣고, 자체 스레드에서 연결을 처리하며, 잘못 동작하게 하거나({@link Fault}) 핸드셰이크 뒤에
 * {@link Script} 를 따르게 할 수 있다. 시험 도구이지 실제 서버가 아니다: ClientHello 를 믿고, 인증서가 하나이며, 잘못된 것을 기록하는 것 말고는
 * 오류 처리가 없다. 레코드는 자체 코드로 쓰고(패딩·분할·손상, TLS 1.2 의 임의 명시적 nonce 를 위해) 클라이언트의 레코드는 모듈의 레코드 계층으로
 * 읽는다.
 */
final class ScriptedServer {

    /**
     * The ways in which the server can misbehave during the handshake.
     * <p>
     * 서버가 핸드셰이크 중에 잘못 동작하는 방식.
     */
    enum Fault {
        /** Answer the ClientHello with a fatal handshake_failure alert. / ClientHello 에 치명적 handshake_failure 경보로 답한다. */
        ALERT_HANDSHAKE_FAILURE,
        /** Answer the ClientHello with a fatal protocol_version alert. / ClientHello 에 치명적 protocol_version 경보로 답한다. */
        ALERT_PROTOCOL_VERSION,
        /** Answer the ClientHello with a fatal internal_error alert. / ClientHello 에 치명적 internal_error 경보로 답한다. */
        ALERT_INTERNAL_ERROR,
        /** Answer with a TLS 1.1 ServerHello. / TLS 1.1 ServerHello 로 답한다. */
        TLS11_HELLO,
        /** Answer with a TLS 1.0 ServerHello that carries an extension the client did not offer. / 클라이언트가 제안하지 않은 확장을 실은 TLS 1.0 ServerHello 로 답한다. */
        TLS10_HELLO_WITH_UNKNOWN_EXTENSION,
        /** TLS 1.2: end the server random with the downgrade marker. / TLS 1.2: 서버 난수를 다운그레이드 표식으로 끝낸다. */
        DOWNGRADE_MARKER,
        /** Select the cipher suite in {@link ScriptedServer#suite} even if the client did not offer it, then wait. / 클라이언트가 제안하지 않았어도 {@link ScriptedServer#suite} 의 스위트를 고르고 기다린다. */
        FOREIGN_SUITE,
        /** TLS 1.3: echo a different session identifier. / TLS 1.3: 다른 세션 식별자를 되돌린다. */
        WRONG_SESSION_ID,
        /** Add an extension that was not offered to the ServerHello. / ServerHello 에 제안받지 않은 확장을 넣는다. */
        HELLO_EXTRA_EXTENSION,
        /** TLS 1.3: add an extension that was not offered to EncryptedExtensions. / TLS 1.3: EncryptedExtensions 에 제안받지 않은 확장을 넣는다. */
        ENCRYPTED_EXTRA_EXTENSION,
        /** TLS 1.3: leave the key share out of the ServerHello. / TLS 1.3: ServerHello 에서 키 공유를 뺀다. */
        NO_KEY_SHARE,
        /** Send a key exchange value that is not a valid public key. / 유효한 공개 키가 아닌 키 교환 값을 보낸다. */
        BAD_KEY_SHARE,
        /** TLS 1.3: leave EncryptedExtensions out. / TLS 1.3: EncryptedExtensions 를 뺀다. */
        SKIP_ENCRYPTED_EXTENSIONS,
        /** TLS 1.3: leave CertificateVerify out. / TLS 1.3: CertificateVerify 를 뺀다. */
        SKIP_CERTIFICATE_VERIFY,
        /** Damage the signature of CertificateVerify or ServerKeyExchange. / CertificateVerify 나 ServerKeyExchange 의 서명을 손상한다. */
        BAD_SIGNATURE,
        /** Sign the right content but for another connection (other server random or transcript). / 맞는 형식이지만 다른 연결의 내용(다른 서버 난수나 전사)에 서명한다. */
        SIGNATURE_OVER_OTHER_CONTENT,
        /** Send a Finished value that is wrong. / 틀린 Finished 값을 보낸다. */
        BAD_FINISHED,
        /** TLS 1.3: send a second HelloRetryRequest. / TLS 1.3: HelloRetryRequest 를 두 번 보낸다. */
        RETRY_TWICE,
        /** TLS 1.3: send a HelloRetryRequest for the group that the client already sent. / TLS 1.3: 클라이언트가 이미 보낸 그룹으로 HelloRetryRequest 를 보낸다. */
        RETRY_SAME_GROUP,
        /** Send a fatal alert after the ServerHello. / ServerHello 뒤에 치명적 경보를 보낸다. */
        ALERT_AFTER_HELLO,
        /** Close the connection after the ServerHello. / ServerHello 뒤에 연결을 닫는다. */
        CLOSE_AFTER_HELLO,
        /** TLS 1.2: echo the session identifier of the client, as if a session were resumed. / TLS 1.2: 세션을 재개하는 것처럼 클라이언트의 세션 식별자를 되돌린다. */
        RESUME,
        /** TLS 1.2: send the Finished without change_cipher_spec. / TLS 1.2: change_cipher_spec 없이 Finished 를 보낸다. */
        NO_CHANGE_CIPHER_SPEC,
        /** TLS 1.2: send a ServerHelloDone that is not empty. / TLS 1.2: 비어 있지 않은 ServerHelloDone 을 보낸다. */
        HELLO_DONE_NOT_EMPTY,
        /** Split a handshake message across the key change: the ServerHello record also carries the first byte of the next message. / 핸드셰이크 메시지를 키 변경에 걸치게 한다. */
        MESSAGE_ACROSS_KEY_CHANGE
    }

    /**
     * What the server does after a successful handshake.
     * <p>
     * 핸드셰이크가 성공한 뒤에 서버가 하는 일.
     */
    interface Script {

        /**
         * Runs on the server thread after the handshake.
         * <p>
         * 핸드셰이크 뒤에 서버 스레드에서 실행된다.
         *
         * @param server the server, for its record methods / 서버(레코드 메서드를 쓴다)
         * @throws Exception to record a failure / 실패를 기록하려면
         */
        void run(ScriptedServer server) throws Exception;
    }

    /** The random value that marks a HelloRetryRequest. / HelloRetryRequest 를 나타내는 난수 값. */
    private static final int[] RETRY_RANDOM = { 0xcf, 0x21, 0xad, 0x74, 0xe5, 0x9a, 0x61, 0x11, 0xbe, 0x1d, 0x8c, 0x02, 0x1e, 0x65, 0xb8, 0x91, 0xc2, 0xa2, 0x11,
            0x16, 0x7a, 0xbb, 0x8c, 0x5e, 0x07, 0x9e, 0x09, 0xe2, 0xc8, 0xa8, 0x33, 0x9c };

    /** True to negotiate TLS 1.3, false for TLS 1.2. / TLS 1.3 을 협상하려면 true, TLS 1.2 이면 false. */
    boolean tls13 = true;

    /** The cipher suite to select. / 고를 암호 스위트. */
    int suite = 0x1301;

    /** The key exchange group to use. / 쓸 키 교환 그룹. */
    int group = ClientHello.GROUP_X25519;

    /** The signature scheme of CertificateVerify or ServerKeyExchange. / CertificateVerify 나 ServerKeyExchange 의 서명 방식. */
    int scheme = 0x0804;

    /** TLS 1.2: agree to the extended master secret if offered. / TLS 1.2: 제안받으면 확장 마스터 비밀에 동의한다. */
    boolean ems = true;

    /** Ask for a client certificate. / 클라이언트 인증서를 요청한다. */
    boolean requestCertificate;

    /** TLS 1.3: send the compatibility change_cipher_spec. / TLS 1.3: 호환용 change_cipher_spec 을 보낸다. */
    boolean compatCcs = true;

    /** The largest number of handshake bytes per record. / 레코드 하나에 넣는 핸드셰이크 바이트의 최대 수. */
    int fragment = RecordLayer.MAX_PLAINTEXT;

    /** TLS 1.3: the number of zero bytes of padding in each protected record. / TLS 1.3: 보호된 레코드마다 넣는 0 패딩 바이트 수. */
    int padding;

    /** After the handshake: milliseconds to pause in the middle of each record, or 0. / 핸드셰이크 뒤: 레코드마다 도중에 멈추는 밀리초. 0 이면 멈추지 않는다. */
    volatile int splitDelay;

    /** The protocol to select with ALPN, or null to leave the extension out. / ALPN 으로 고를 프로토콜. null 이면 확장을 뺀다. */
    String alpn = "http/1.1";

    /** The faults to inject. / 주입할 결함. */
    final Set<Fault> faults = EnumSet.noneOf(Fault.class);

    /** What to do after the handshake; null echoes application data until the client closes. / 핸드셰이크 뒤에 할 일. null 이면 클라이언트가 닫을 때까지 응용 데이터를 되돌린다. */
    Script script;

    /** The number of connections to serve. / 처리할 연결 수. */
    int connections = 1;

    /** The first failure of the server thread, or null. / 서버 스레드의 첫 실패. 없으면 null. */
    volatile Throwable error;

    /** The description of the last alert that the client sent, or -1. / 클라이언트가 마지막으로 보낸 경보의 설명. 없으면 -1. */
    volatile int clientAlert = -1;

    /** The level of the last alert that the client sent, or -1. / 클라이언트가 마지막으로 보낸 경보의 수준. 없으면 -1. */
    volatile int clientAlertLevel = -1;

    /** The host name of the server_name extension, or null. / server_name 확장의 호스트 이름. 없으면 null. */
    volatile String sni;

    /** True if a HelloRetryRequest was sent. / HelloRetryRequest 를 보냈으면 true. */
    volatile boolean retried;

    /** True if the handshake completed on the server side. / 서버 쪽에서 핸드셰이크가 끝났으면 true. */
    volatile boolean established;

    /** The length of the certificate list that the client sent, or -1 if it sent no Certificate message. / 클라이언트가 보낸 인증서 목록의 길이. Certificate 메시지를 보내지 않았으면 -1. */
    volatile int clientCertificateListLength = -1;

    /** True if the client offered the extended master secret. / 클라이언트가 확장 마스터 비밀을 제안했으면 true. */
    volatile boolean offeredEms;

    /** The version field of the record that carried the first ClientHello. / 첫 ClientHello 를 실은 레코드의 버전 필드. */
    volatile int legacyVersion;

    /** The cipher suites that the client offered. / 클라이언트가 제안한 암호 스위트. */
    final List<Integer> offeredSuites = new ArrayList<Integer>();

    /** The groups that the client offered. / 클라이언트가 제안한 그룹. */
    final List<Integer> offeredGroups = new ArrayList<Integer>();

    /** The versions of supported_versions. / supported_versions 의 버전들. */
    final List<Integer> offeredVersions = new ArrayList<Integer>();

    /** The extension types of the ClientHello in order. / ClientHello 의 확장 형식(순서대로). */
    final List<Integer> offeredExtensions = new ArrayList<Integer>();

    /** The key of the server certificate. / 서버 인증서의 키. */
    private final TestKey key;

    /** The certificates to send, the server certificate first. / 보낼 인증서들. 서버 인증서가 먼저. */
    private final List<byte[]> chain;

    /** The random source. / 난수원. */
    private final SecureRandom random = new SecureRandom();

    /** The listening socket. / 듣는 소켓. */
    private ServerSocket listener;

    /** The server thread. / 서버 스레드. */
    private Thread thread;

    /** The connection being served. / 처리 중인 연결. */
    private Socket socket;

    /** The stream to the client. / 클라이언트로 가는 스트림. */
    private OutputStream out;

    /** The record layer that reads the records of the client. / 클라이언트의 레코드를 읽는 레코드 계층. */
    private RecordLayer in;

    /** The handshake reader and the transcript. / 핸드셰이크 읽기 도구와 전사. */
    private HandshakeIo io;

    /** Handshake bytes that wait to be sent. / 보내기를 기다리는 핸드셰이크 바이트. */
    private ByteArrayOutputStream flight = new ByteArrayOutputStream();

    /** The negotiated suite. / 협상한 스위트. */
    private CipherSuite negotiated;

    /** The AEAD for writing, or null. / 쓰기용 AEAD. 없으면 null. */
    private Aead writeAead;

    /** The IV for writing. / 쓰기용 IV. */
    private byte[] writeIv;

    /** The sequence number of the next written record. / 다음에 쓸 레코드의 순서 번호. */
    private long writeSequence;

    /** True to damage the next protected record. / 다음 보호된 레코드를 손상하려면 true. */
    private boolean damageNext;

    /** The client random. / 클라이언트 난수. */
    private byte[] clientRandom;

    /** The session identifier of the client. / 클라이언트의 세션 식별자. */
    private byte[] clientSessionId;

    /** The key shares of the client by group. / 그룹별 클라이언트 키 공유. */
    private final Map<Integer, byte[]> clientShares = new HashMap<Integer, byte[]>();

    /** The current application traffic secret of the server (TLS 1.3). / 서버의 현재 응용 트래픽 비밀(TLS 1.3). */
    private byte[] serverSecret;

    /** The current application traffic secret of the client (TLS 1.3). / 클라이언트의 현재 응용 트래픽 비밀(TLS 1.3). */
    private byte[] clientSecret;

    /**
     * Creates a server with a certificate.
     * <p>
     * 인증서를 가진 서버를 만든다.
     *
     * @param key   the key of the server certificate / 서버 인증서의 키
     * @param chain the certificates to send, the server certificate first / 보낼 인증서들. 서버 인증서가 먼저
     */
    ScriptedServer(TestKey key, List<byte[]> chain) {
        this.key = key;
        this.chain = chain;
        this.scheme = key.isRsa() ? 0x0804 : key.curve() == EcCurve.P256 ? 0x0403 : 0x0503;
    }

    /**
     * Starts listening and serving on a new thread.
     * <p>
     * 새 스레드에서 듣고 처리하기 시작한다.
     *
     * @return this server / 이 서버
     * @throws IOException if the port cannot be opened / 포트를 열 수 없으면
     */
    ScriptedServer start() throws IOException {
        listener = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
        listener.setSoTimeout(20000);
        thread = new Thread(new Runnable() {
            public void run() {
                // 정한 수만큼 연결을 차례로 처리한다.
                for (int i = 0; i < connections; i++) {
                    try {
                        socket = listener.accept();
                        socket.setSoTimeout(20000);
                        serve();
                    } catch (Throwable t) {
                        if (error == null) {
                            error = t;
                        }
                    } finally {
                        closeSocket();
                    }
                }
            }
        }, "scripted-tls-server");
        thread.setDaemon(true);
        thread.start();
        return this;
    }

    /**
     * Returns the port that the server listens on.
     * <p>
     * 서버가 듣는 포트를 반환한다.
     *
     * @return the port / 포트
     */
    int port() {
        return listener.getLocalPort();
    }

    /**
     * Waits for the server thread and closes the port.
     * <p>
     * 서버 스레드를 기다리고 포트를 닫는다.
     *
     * @throws Exception if the thread does not end / 스레드가 끝나지 않으면
     */
    void join() throws Exception {
        thread.join(30000);
        boolean alive = thread.isAlive();
        listener.close();
        closeSocket();
        if (alive) {
            throw new IllegalStateException("the scripted server did not finish");
        }
    }

    /**
     * Waits for the server thread and fails if the server recorded an error.
     * <p>
     * 서버 스레드를 기다리고, 서버가 오류를 기록했으면 실패한다.
     *
     * @throws Exception the recorded error / 기록된 오류
     */
    void finish() throws Exception {
        join();
        if (error != null) {
            IllegalStateException e = new IllegalStateException("the scripted server failed: " + error);
            e.initCause(error);
            throw e;
        }
    }

    /**
     * Closes the connection without a closing alert.
     * <p>
     * 닫는 경보 없이 연결을 닫는다.
     */
    void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // 닫기 실패는 시험 결과에 영향이 없다.
            socket = null;
        }
    }

    /**
     * Serves one connection.
     * <p>
     * 연결 하나를 처리한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    private void serve() throws Exception {
        out = socket.getOutputStream();
        in = new RecordLayer(socket.getInputStream(), new ByteArrayOutputStream());
        io = new HandshakeIo(in);
        flight = new ByteArrayOutputStream();
        writeAead = null;
        writeSequence = 0;
        established = false;
        try {
            HandshakeIo.Message hello = io.receive();
            if (hello.type != HandshakeIo.CLIENT_HELLO) {
                throw new IllegalStateException("no ClientHello");
            }
            parseClientHello(hello.body);
            if (faults.contains(Fault.ALERT_HANDSHAKE_FAILURE) || faults.contains(Fault.ALERT_PROTOCOL_VERSION) || faults.contains(Fault.ALERT_INTERNAL_ERROR)) {
                int description = faults.contains(Fault.ALERT_HANDSHAKE_FAILURE) ? 40 : faults.contains(Fault.ALERT_PROTOCOL_VERSION) ? 70 : 80;
                writeRecord(RecordLayer.ALERT, new byte[] { 2, (byte) description });
                return;
            }
            if (faults.contains(Fault.TLS11_HELLO) || faults.contains(Fault.TLS10_HELLO_WITH_UNKNOWN_EXTENSION)) {
                ByteWriter w = new ByteWriter().u16(faults.contains(Fault.TLS11_HELLO) ? 0x0302 : 0x0301).bytes(randomBytes(32)).vector8(new byte[0]).u16(0xc013).u8(0);
                if (faults.contains(Fault.TLS10_HELLO_WITH_UNKNOWN_EXTENSION)) {
                    w.vector16(new ByteWriter().u16(0x3374).vector16(new byte[0]).toByteArray());
                }
                handshake(HandshakeIo.SERVER_HELLO, w.toByteArray());
                flush();
                awaitAlert();
                return;
            }
            boolean done = tls13 ? handshake13() : handshake12();
            if (!done) {
                return;
            }
            established = true;
            if (script != null) {
                script.run(this);
            } else {
                echo();
            }
        } catch (Tls13Exception e) {
            if (e.received()) {
                // 클라이언트가 경보를 보냈다. 시험이 확인할 수 있게 기록한다.
                clientAlert = e.alert();
                clientAlertLevel = 2;
            } else {
                throw e;
            }
        }
    }

    /**
     * Returns random bytes.
     * <p>
     * 난수 바이트를 반환한다.
     *
     * @param n the count / 개수
     * @return the bytes / 바이트
     */
    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return b;
    }

    /**
     * Reads the fields of the ClientHello that the server needs.
     * <p>
     * 서버에 필요한 ClientHello 의 필드를 읽는다.
     *
     * @param body the message body / 메시지 본문
     * @throws Exception if the message is malformed / 메시지가 잘못되었으면
     */
    private void parseClientHello(byte[] body) throws Exception {
        ByteReader r = new ByteReader(body);
        legacyVersion = r.u16();
        clientRandom = r.bytes(32);
        clientSessionId = r.vector8();
        ByteReader suites = new ByteReader(r.vector16());
        offeredSuites.clear();
        // 제안한 스위트를 모은다.
        while (suites.remaining() > 0) {
            offeredSuites.add(Integer.valueOf(suites.u16()));
        }
        r.vector8();
        ByteReader ext = r.sub16();
        r.expectEnd();
        clientShares.clear();
        offeredGroups.clear();
        offeredVersions.clear();
        offeredExtensions.clear();
        offeredEms = false;
        // 확장을 차례로 읽는다.
        while (ext.remaining() > 0) {
            int type = ext.u16();
            ByteReader data = new ByteReader(ext.vector16());
            offeredExtensions.add(Integer.valueOf(type));
            if (type == ClientHello.EXT_SERVER_NAME) {
                ByteReader list = data.sub16();
                list.u8();
                byte[] name = list.vector16();
                sni = new String(name, "US-ASCII");
            } else if (type == ClientHello.EXT_SUPPORTED_GROUPS) {
                ByteReader list = data.sub16();
                // 제안한 그룹을 모은다.
                while (list.remaining() > 0) {
                    offeredGroups.add(Integer.valueOf(list.u16()));
                }
            } else if (type == ClientHello.EXT_SUPPORTED_VERSIONS) {
                ByteReader list = new ByteReader(data.vector8());
                // 제안한 버전을 모은다.
                while (list.remaining() > 0) {
                    offeredVersions.add(Integer.valueOf(list.u16()));
                }
            } else if (type == ClientHello.EXT_KEY_SHARE) {
                ByteReader list = data.sub16();
                // 키 공유를 그룹별로 모은다.
                while (list.remaining() > 0) {
                    int g = list.u16();
                    clientShares.put(Integer.valueOf(g), list.vector16());
                }
            } else if (type == ClientHello.EXT_EXTENDED_MASTER_SECRET) {
                offeredEms = true;
            }
        }
    }

    /**
     * Queues a handshake message and adds it to the transcript.
     * <p>
     * 핸드셰이크 메시지를 대기열에 넣고 전사에 더한다.
     *
     * @param type the handshake type / 핸드셰이크 형식
     * @param body the body / 본문
     */
    private void handshake(int type, byte[] body) {
        byte[] message = HandshakeIo.encode(type, body);
        io.appendToTranscript(message);
        flight.write(message, 0, message.length);
    }

    /**
     * Sends the queued handshake bytes in records of at most {@link #fragment} bytes.
     * <p>
     * 대기 중인 핸드셰이크 바이트를 최대 {@link #fragment} 바이트의 레코드로 보낸다.
     *
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    private void flush() throws IOException {
        byte[] all = flight.toByteArray();
        flight = new ByteArrayOutputStream();
        // 정한 크기로 나눠 보낸다.
        for (int at = 0; at < all.length; at += fragment) {
            writeRecord(RecordLayer.HANDSHAKE, Bytes.copy(all, at, Math.min(fragment, all.length - at)));
        }
    }

    /**
     * Installs the keys for writing.
     * <p>
     * 쓰기 키를 설치한다.
     *
     * @param key the key / 키
     * @param iv  the IV or salt / IV 또는 salt
     */
    private void setWriteKeys(byte[] key, byte[] iv) {
        writeAead = negotiated.newAead(key);
        writeIv = iv;
        writeSequence = 0;
    }

    /**
     * Makes the next protected record fail its authentication.
     * <p>
     * 다음 보호된 레코드가 인증에 실패하게 한다.
     */
    void damageNextRecord() {
        damageNext = true;
    }

    /**
     * Writes one record with the current protection.
     * <p>
     * 현재 보호 상태로 레코드 하나를 쓴다.
     *
     * @param type the content type / 내용 형식
     * @param data the plaintext / 평문
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    void writeRecord(int type, byte[] data) throws IOException {
        byte[] record;
        if (writeAead == null) {
            record = new byte[5 + data.length];
            record[0] = (byte) type;
            System.arraycopy(data, 0, record, 5, data.length);
        } else if (tls13) {
            // TLSInnerPlaintext: 내용, 실제 형식, 0 패딩. 패딩은 전체가 2^14 + 1 바이트를 넘지 않는 만큼만 넣는다.
            byte[] inner = new byte[data.length + 1 + Math.max(0, Math.min(padding, RecordLayer.MAX_PLAINTEXT - data.length))];
            System.arraycopy(data, 0, inner, 0, data.length);
            inner[data.length] = (byte) type;
            record = new byte[5 + inner.length + Aead.TAG_LENGTH];
            record[0] = (byte) RecordLayer.APPLICATION_DATA;
            record[3] = (byte) ((record.length - 5) >> 8);
            record[4] = (byte) (record.length - 5);
            record[1] = 3;
            record[2] = 3;
            writeAead.seal(sequenceNonce(), Bytes.copy(record, 0, 5), inner, 0, inner.length, record, 5);
        } else {
            boolean explicit = negotiated.explicitNonce();
            int skip = explicit ? 8 : 0;
            record = new byte[5 + skip + data.length + Aead.TAG_LENGTH];
            record[0] = (byte) type;
            byte[] nonce;
            if (explicit) {
                // 명시적 nonce 는 순서 번호와 무관한 임의 값으로 한다: 클라이언트가 레코드의 값을 써야 한다.
                byte[] chosen = randomBytes(8);
                System.arraycopy(chosen, 0, record, 5, 8);
                nonce = Bytes.concat(writeIv, chosen);
            } else {
                nonce = sequenceNonce();
            }
            byte[] aad = new byte[13];
            Bytes.putBe64(writeSequence, aad, 0);
            aad[8] = (byte) type;
            aad[9] = 3;
            aad[10] = 3;
            aad[11] = (byte) (data.length >> 8);
            aad[12] = (byte) data.length;
            writeAead.seal(nonce, aad, data, 0, data.length, record, 5 + skip);
        }
        record[1] = 3;
        record[2] = 3;
        record[3] = (byte) ((record.length - 5) >> 8);
        record[4] = (byte) (record.length - 5);
        if (writeAead != null) {
            writeSequence++;
            if (damageNext) {
                record[record.length - 1] ^= 1;
                damageNext = false;
            }
        }
        if (splitDelay > 0 && established && record.length > 9) {
            // 레코드 도중에 멈췄다가 나머지를 보낸다(클라이언트의 읽기 제한 시간 시험).
            out.write(record, 0, 9);
            out.flush();
            try {
                Thread.sleep(splitDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            out.write(record, 9, record.length - 9);
        } else {
            out.write(record);
        }
        out.flush();
    }

    /**
     * Builds the nonce from the IV and the sequence number.
     * <p>
     * IV 와 순서 번호로 nonce 를 만든다.
     *
     * @return the 12 byte nonce / 12바이트 nonce
     */
    private byte[] sequenceNonce() {
        byte[] n = writeIv.clone();
        // 순서 번호를 뒤쪽 8바이트에 XOR 한다.
        for (int i = 0; i < 8; i++) {
            n[11 - i] ^= (byte) (writeSequence >>> (8 * i));
        }
        return n;
    }

    /**
     * Writes raw bytes to the client (malformed records).
     * <p>
     * 클라이언트에 날 바이트를 쓴다(잘못된 레코드).
     *
     * @param bytes the bytes / 바이트
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    void writeRaw(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
    }

    /**
     * Reads records until the client sends an alert or closes, and records the alert.
     * <p>
     * 클라이언트가 경보를 보내거나 닫을 때까지 레코드를 읽고, 경보를 기록한다.
     *
     * @throws IOException on a read failure / 읽기 실패 시
     */
    void awaitAlert() throws IOException {
        RecordLayer.Record r;
        // 경보나 스트림 끝까지 읽는다.
        while ((r = in.read()) != null) {
            if (r.type == RecordLayer.ALERT && r.data.length == 2) {
                clientAlertLevel = r.data[0];
                clientAlert = r.data[1] & 0xff;
                return;
            }
        }
    }

    /**
     * Reads the next record of the client; an alert is recorded.
     * <p>
     * 클라이언트의 다음 레코드를 읽는다. 경보는 기록한다.
     *
     * @return the record, or null at the end of the stream / 레코드. 스트림 끝이면 null
     * @throws IOException on a read failure / 읽기 실패 시
     */
    RecordLayer.Record readRecord() throws IOException {
        RecordLayer.Record r = in.read();
        if (r != null && r.type == RecordLayer.ALERT && r.data.length == 2) {
            clientAlertLevel = r.data[0];
            clientAlert = r.data[1] & 0xff;
        }
        return r;
    }

    /**
     * Reads application data of the client until a number of bytes arrived.
     * <p>
     * 정한 바이트 수가 올 때까지 클라이언트의 응용 데이터를 읽는다.
     *
     * @param n the number of bytes / 바이트 수
     * @return the data, shorter if the client closed / 데이터. 클라이언트가 닫았으면 더 짧다
     * @throws IOException on a read failure / 읽기 실패 시
     */
    byte[] readData(int n) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        // 응용 데이터 레코드를 모은다.
        while (data.size() < n) {
            RecordLayer.Record r = readRecord();
            if (r == null || r.type == RecordLayer.ALERT) {
                break;
            }
            if (r.type == RecordLayer.APPLICATION_DATA) {
                data.write(r.data, 0, r.data.length);
            }
        }
        return data.toByteArray();
    }

    /**
     * Sends application data.
     * <p>
     * 응용 데이터를 보낸다.
     *
     * @param data the data / 데이터
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    void sendData(byte[] data) throws IOException {
        int at = 0;
        // 레코드의 최대 크기로 나눠 보낸다. 빈 데이터는 빈 레코드 하나로 보낸다.
        do {
            int n = Math.min(RecordLayer.MAX_PLAINTEXT, data.length - at);
            writeRecord(RecordLayer.APPLICATION_DATA, Bytes.copy(data, at, n));
            at += n;
        } while (at < data.length);
    }

    /**
     * Sends a handshake message after the handshake (NewSessionTicket, KeyUpdate, HelloRequest).
     * <p>
     * 핸드셰이크 뒤에 핸드셰이크 메시지를 보낸다(NewSessionTicket, KeyUpdate, HelloRequest).
     *
     * @param type the handshake type / 핸드셰이크 형식
     * @param body the body / 본문
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    void sendPostHandshake(int type, byte[] body) throws IOException {
        writeRecord(RecordLayer.HANDSHAKE, HandshakeIo.encode(type, body));
    }

    /**
     * Sends an alert.
     * <p>
     * 경보를 보낸다.
     *
     * @param level       1 or 2 / 1 또는 2
     * @param description the description / 설명
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    void sendAlert(int level, int description) throws IOException {
        writeRecord(RecordLayer.ALERT, new byte[] { (byte) level, (byte) description });
    }

    /**
     * TLS 1.3: moves the write keys of the server to the next generation (after it sent a KeyUpdate).
     * <p>
     * TLS 1.3: 서버의 쓰기 키를 다음 세대로 넘긴다(KeyUpdate 를 보낸 뒤).
     */
    void updateWriteKeys() {
        HashAlg hash = negotiated.hash();
        serverSecret = KeySchedule13.nextTrafficSecret(hash, serverSecret);
        setWriteKeys(KeySchedule13.trafficKey(hash, serverSecret, negotiated.keyLength()), KeySchedule13.trafficIv(hash, serverSecret));
    }

    /**
     * TLS 1.3: moves the read keys of the server to the next generation (after the client sent a KeyUpdate).
     * <p>
     * TLS 1.3: 서버의 읽기 키를 다음 세대로 넘긴다(클라이언트가 KeyUpdate 를 보낸 뒤).
     */
    void updateReadKeys() {
        HashAlg hash = negotiated.hash();
        clientSecret = KeySchedule13.nextTrafficSecret(hash, clientSecret);
        in.setReadProtection(negotiated, true, KeySchedule13.trafficKey(hash, clientSecret, negotiated.keyLength()), KeySchedule13.trafficIv(hash, clientSecret));
    }

    /**
     * The default script: echoes application data until the client closes, and answers its close_notify.
     * <p>
     * 기본 스크립트: 클라이언트가 닫을 때까지 응용 데이터를 되돌리고, close_notify 에 답한다.
     *
     * @throws IOException on a failure / 실패 시
     */
    void echo() throws IOException {
        RecordLayer.Record r;
        // 클라이언트가 닫을 때까지 받은 것을 되돌린다.
        while ((r = readRecord()) != null) {
            if (r.type == RecordLayer.APPLICATION_DATA) {
                writeRecord(RecordLayer.APPLICATION_DATA, r.data);
            } else if (r.type == RecordLayer.ALERT) {
                if (clientAlert == 0) {
                    sendAlert(1, 0);
                }
                return;
            }
        }
    }

    /**
     * Creates the key exchange of the server for the configured group and returns the shared secret.
     * <p>
     * 설정한 그룹으로 서버의 키 교환을 만들고 공유 비밀을 반환한다.
     *
     * @param peer   the key exchange value of the client / 클라이언트의 키 교환 값
     * @param shares receives the key exchange value of the server at index 0 / 0번에 서버의 키 교환 값을 받는다
     * @return the shared secret / 공유 비밀
     * @throws Exception on a failure / 실패 시
     */
    private byte[] exchange(byte[] peer, byte[][] shares) throws Exception {
        if (group == ClientHello.GROUP_X25519) {
            byte[] priv = randomBytes(32);
            shares[0] = X25519.publicKey(priv);
            return peer == null ? null : X25519.sharedSecret(priv, peer);
        }
        Ecdh ecdh = new Ecdh(group == ClientHello.GROUP_SECP256R1 ? EcCurve.P256 : group == ClientHello.GROUP_SECP384R1 ? EcCurve.P384 : EcCurve.P521, random);
        shares[0] = ecdh.publicKey();
        return peer == null ? null : ecdh.sharedSecret(peer);
    }

    /**
     * Signs with the key of the server in the configured scheme.
     * <p>
     * 설정한 방식으로 서버의 키로 서명한다.
     *
     * @param content the content to sign / 서명할 내용
     * @return the signature / 서명
     */
    private byte[] sign(byte[] content) {
        HashAlg hash = (scheme & 0xff00) == 0x0400 || scheme == 0x0804 ? HashAlg.SHA256 : (scheme & 0xff00) == 0x0500 || scheme == 0x0805 ? HashAlg.SHA384
                : HashAlg.SHA512;
        int kind = (scheme & 0xff00) == 0x0800 ? TestKey.PSS : (scheme & 0xff) == 0x03 ? TestKey.ECDSA : TestKey.PKCS1;
        byte[] signature = key.sign(kind, hash, content);
        if (faults.contains(Fault.BAD_SIGNATURE)) {
            signature[signature.length / 2] ^= 0x10;
        }
        return signature;
    }

    /**
     * Encodes the ALPN extension data for the configured protocol.
     * <p>
     * 설정한 프로토콜의 ALPN 확장 데이터를 부호화한다.
     *
     * @return the extension data / 확장 데이터
     * @throws IOException never / 발생하지 않는다
     */
    private byte[] alpnData() throws IOException {
        return new ByteWriter().vector16(new ByteWriter().vector8(alpn.getBytes("US-ASCII")).toByteArray()).toByteArray();
    }

    /**
     * Builds a HelloRetryRequest for a group.
     * <p>
     * 그룹에 대한 HelloRetryRequest 를 만든다.
     *
     * @param retryGroup the group / 그룹
     * @return the message body / 메시지 본문
     */
    private byte[] retryRequest(int retryGroup) {
        byte[] marker = new byte[32];
        // HelloRetryRequest 의 고정 난수를 넣는다.
        for (int i = 0; i < 32; i++) {
            marker[i] = (byte) RETRY_RANDOM[i];
        }
        ByteWriter ext = new ByteWriter();
        ext.u16(ClientHello.EXT_SUPPORTED_VERSIONS).vector16(new byte[] { 3, 4 });
        ext.u16(ClientHello.EXT_KEY_SHARE).vector16(new ByteWriter().u16(retryGroup).toByteArray());
        ext.u16(ClientHello.EXT_COOKIE).vector16(new ByteWriter().vector16(new byte[] { 'c', 'o', 'o', 'k', 'i', 'e' }).toByteArray());
        return new ByteWriter().u16(0x0303).bytes(marker).vector8(clientSessionId).u16(suite).u8(0).vector16(ext.toByteArray()).toByteArray();
    }

    /**
     * The TLS 1.3 handshake of the server.
     * <p>
     * 서버의 TLS 1.3 핸드셰이크.
     *
     * @return true if the handshake completed / 핸드셰이크가 끝났으면 true
     * @throws Exception on a failure / 실패 시
     */
    private boolean handshake13() throws Exception {
        negotiated = CipherSuite.byId(suite);
        HashAlg hash = negotiated != null ? negotiated.hash() : HashAlg.SHA256;
        byte[] share = clientShares.get(Integer.valueOf(group));
        if (share == null || faults.contains(Fault.RETRY_SAME_GROUP)) {
            // HelloRetryRequest: 전사의 첫 ClientHello 를 message_hash 로 바꾼 뒤에 넣는다.
            int retryGroup = faults.contains(Fault.RETRY_SAME_GROUP) ? clientShares.keySet().iterator().next().intValue() : group;
            io.replaceByMessageHash(hash);
            handshake(HandshakeIo.SERVER_HELLO, retryRequest(retryGroup));
            flush();
            retried = true;
            if (compatCcs) {
                writeRecord(RecordLayer.CHANGE_CIPHER_SPEC, new byte[] { 1 });
            }
            in.setDropPlainChangeCipherSpec(true);
            if (faults.contains(Fault.RETRY_SAME_GROUP)) {
                awaitAlert();
                return false;
            }
            HandshakeIo.Message second = io.receive();
            parseClientHello(second.body);
            share = clientShares.get(Integer.valueOf(group));
            if (share == null) {
                throw new IllegalStateException("the second ClientHello has no key share of group " + group);
            }
            if (faults.contains(Fault.RETRY_TWICE)) {
                handshake(HandshakeIo.SERVER_HELLO, retryRequest(group));
                flush();
                awaitAlert();
                return false;
            }
        }
        byte[][] own = new byte[1][];
        byte[] shared = exchange(share, own);
        if (faults.contains(Fault.BAD_KEY_SHARE)) {
            own[0] = new byte[own[0].length];
        }
        ByteWriter ext = new ByteWriter();
        ext.u16(ClientHello.EXT_SUPPORTED_VERSIONS).vector16(new byte[] { 3, 4 });
        if (!faults.contains(Fault.NO_KEY_SHARE)) {
            ext.u16(ClientHello.EXT_KEY_SHARE).vector16(new ByteWriter().u16(group).vector16(own[0]).toByteArray());
        }
        if (faults.contains(Fault.HELLO_EXTRA_EXTENSION)) {
            ext.u16(0x3374).vector16(new byte[0]);
        }
        byte[] sessionId = faults.contains(Fault.WRONG_SESSION_ID) ? randomBytes(32) : clientSessionId;
        handshake(HandshakeIo.SERVER_HELLO,
                new ByteWriter().u16(0x0303).bytes(randomBytes(32)).vector8(sessionId).u16(suite).u8(0).vector16(ext.toByteArray()).toByteArray());
        if (faults.contains(Fault.MESSAGE_ACROSS_KEY_CHANGE)) {
            // 다음 메시지(EncryptedExtensions)의 첫 바이트를 보호되지 않은 ServerHello 레코드에 붙인다.
            flight.write(HandshakeIo.ENCRYPTED_EXTENSIONS);
        }
        flush();
        if (compatCcs) {
            writeRecord(RecordLayer.CHANGE_CIPHER_SPEC, new byte[] { 1 });
        }
        in.setDropPlainChangeCipherSpec(true);
        if (negotiated == null || faults.contains(Fault.FOREIGN_SUITE) || faults.contains(Fault.NO_KEY_SHARE) || faults.contains(Fault.BAD_KEY_SHARE)
                || faults.contains(Fault.WRONG_SESSION_ID) || faults.contains(Fault.HELLO_EXTRA_EXTENSION) || faults.contains(Fault.MESSAGE_ACROSS_KEY_CHANGE)) {
            awaitAlert();
            return false;
        }
        if (faults.contains(Fault.CLOSE_AFTER_HELLO)) {
            return false;
        }

        KeySchedule13 ks = new KeySchedule13(hash);
        ks.addSharedSecret(shared);
        byte[] helloHash = io.transcriptHash(hash);
        byte[] clientHs = ks.clientHandshakeTraffic(helloHash);
        byte[] serverHs = ks.serverHandshakeTraffic(helloHash);
        setWriteKeys(KeySchedule13.trafficKey(hash, serverHs, negotiated.keyLength()), KeySchedule13.trafficIv(hash, serverHs));
        if (faults.contains(Fault.ALERT_AFTER_HELLO)) {
            sendAlert(2, Tls13Exception.INTERNAL_ERROR);
            return false;
        }

        boolean flightFault = false;
        if (!faults.contains(Fault.SKIP_ENCRYPTED_EXTENSIONS)) {
            ByteWriter ee = new ByteWriter();
            if (alpn != null) {
                ee.u16(ClientHello.EXT_ALPN).vector16(alpnData());
            }
            if (sni != null) {
                ee.u16(ClientHello.EXT_SERVER_NAME).vector16(new byte[0]);
            }
            if (faults.contains(Fault.ENCRYPTED_EXTRA_EXTENSION)) {
                ee.u16(0x3374).vector16(new byte[0]);
                flightFault = true;
            }
            handshake(HandshakeIo.ENCRYPTED_EXTENSIONS, new ByteWriter().vector16(ee.toByteArray()).toByteArray());
        } else {
            flightFault = true;
        }
        if (requestCertificate) {
            byte[] algorithms = new ByteWriter().vector16(new byte[] { 4, 3, 8, 4 }).toByteArray();
            handshake(HandshakeIo.CERTIFICATE_REQUEST,
                    new ByteWriter().vector8(new byte[0]).vector16(new ByteWriter().u16(ClientHello.EXT_SIGNATURE_ALGORITHMS).vector16(algorithms).toByteArray())
                            .toByteArray());
        }
        ByteWriter list = new ByteWriter();
        // 인증서마다 빈 확장 목록을 붙인다.
        for (int i = 0; i < chain.size(); i++) {
            list.vector24(chain.get(i)).vector16(new byte[0]);
        }
        handshake(HandshakeIo.CERTIFICATE, new ByteWriter().vector8(new byte[0]).vector24(list.toByteArray()).toByteArray());
        if (!faults.contains(Fault.SKIP_CERTIFICATE_VERIFY)) {
            byte[] context = "TLS 1.3, server CertificateVerify".getBytes("US-ASCII");
            byte[] transcriptHash = io.transcriptHash(hash);
            if (faults.contains(Fault.SIGNATURE_OVER_OTHER_CONTENT)) {
                transcriptHash = hash.digest(transcriptHash);
                flightFault = true;
            }
            byte[] spaces = new byte[64];
            java.util.Arrays.fill(spaces, (byte) 0x20);
            byte[] signature = sign(Bytes.concat(spaces, context, new byte[1], transcriptHash));
            handshake(HandshakeIo.CERTIFICATE_VERIFY, new ByteWriter().u16(scheme).vector16(signature).toByteArray());
        } else {
            flightFault = true;
        }
        byte[] mac = KeySchedule13.finishedMac(hash, serverHs, io.transcriptHash(hash));
        if (faults.contains(Fault.BAD_FINISHED)) {
            mac[0] ^= 1;
            flightFault = true;
        }
        handshake(HandshakeIo.FINISHED, mac);
        flush();
        if (flightFault || faults.contains(Fault.BAD_SIGNATURE)) {
            awaitAlert();
            return false;
        }
        byte[] finishedHash = io.transcriptHash(hash);
        ks.toMaster();
        clientSecret = ks.clientApplicationTraffic(finishedHash);
        serverSecret = ks.serverApplicationTraffic(finishedHash);
        setWriteKeys(KeySchedule13.trafficKey(hash, serverSecret, negotiated.keyLength()), KeySchedule13.trafficIv(hash, serverSecret));

        in.setReadProtection(negotiated, true, KeySchedule13.trafficKey(hash, clientHs, negotiated.keyLength()), KeySchedule13.trafficIv(hash, clientHs));
        if (requestCertificate) {
            HandshakeIo.Message certificate = io.receive();
            if (certificate.type != HandshakeIo.CERTIFICATE) {
                throw new IllegalStateException("the client did not answer the certificate request");
            }
            ByteReader r = new ByteReader(certificate.body);
            r.vector8();
            clientCertificateListLength = r.vector24().length;
        }
        byte[] expected = KeySchedule13.finishedMac(hash, clientHs, io.transcriptHash(hash));
        HandshakeIo.Message finished = io.receive();
        if (finished.type != HandshakeIo.FINISHED || !Bytes.constantTimeEquals(expected, finished.body)) {
            throw new IllegalStateException("the Finished of the client is wrong");
        }
        io.expectBoundary();
        in.setReadProtection(negotiated, true, KeySchedule13.trafficKey(hash, clientSecret, negotiated.keyLength()), KeySchedule13.trafficIv(hash, clientSecret));
        in.setDropPlainChangeCipherSpec(false);
        return true;
    }

    /**
     * The TLS 1.2 handshake of the server (ECDHE key exchange).
     * <p>
     * 서버의 TLS 1.2 핸드셰이크(ECDHE 키 교환).
     *
     * @return true if the handshake completed / 핸드셰이크가 끝났으면 true
     * @throws Exception on a failure / 실패 시
     */
    private boolean handshake12() throws Exception {
        negotiated = CipherSuite.byId(suite);
        byte[] serverRandom = randomBytes(32);
        if (faults.contains(Fault.DOWNGRADE_MARKER)) {
            System.arraycopy(new byte[] { 0x44, 0x4f, 0x57, 0x4e, 0x47, 0x52, 0x44, 0x01 }, 0, serverRandom, 24, 8);
        }
        boolean useEms = ems && offeredEms;
        ByteWriter ext = new ByteWriter();
        ext.u16(ClientHello.EXT_RENEGOTIATION_INFO).vector16(new byte[] { 0 });
        if (useEms) {
            ext.u16(ClientHello.EXT_EXTENDED_MASTER_SECRET).vector16(new byte[0]);
        }
        ext.u16(ClientHello.EXT_EC_POINT_FORMATS).vector16(new byte[] { 1, 0 });
        if (alpn != null) {
            ext.u16(ClientHello.EXT_ALPN).vector16(alpnData());
        }
        if (faults.contains(Fault.HELLO_EXTRA_EXTENSION)) {
            ext.u16(0x3374).vector16(new byte[0]);
        }
        byte[] sessionId = faults.contains(Fault.RESUME) ? clientSessionId : randomBytes(32);
        handshake(HandshakeIo.SERVER_HELLO,
                new ByteWriter().u16(0x0303).bytes(serverRandom).vector8(sessionId).u16(suite).u8(0).vector16(ext.toByteArray()).toByteArray());
        if (negotiated == null || negotiated.isTls13() || faults.contains(Fault.FOREIGN_SUITE) || faults.contains(Fault.DOWNGRADE_MARKER)
                || faults.contains(Fault.RESUME) || faults.contains(Fault.HELLO_EXTRA_EXTENSION)) {
            flush();
            awaitAlert();
            return false;
        }
        if (faults.contains(Fault.ALERT_AFTER_HELLO)) {
            flush();
            sendAlert(2, Tls13Exception.INTERNAL_ERROR);
            return false;
        }
        if (faults.contains(Fault.CLOSE_AFTER_HELLO)) {
            flush();
            return false;
        }
        HashAlg hash = negotiated.hash();

        ByteWriter list = new ByteWriter();
        // 인증서를 차례로 넣는다.
        for (int i = 0; i < chain.size(); i++) {
            list.vector24(chain.get(i));
        }
        handshake(HandshakeIo.CERTIFICATE, new ByteWriter().vector24(list.toByteArray()).toByteArray());
        byte[][] own = new byte[1][];
        Ecdh ecdh = null;
        byte[] x25519Private = null;
        if (group == ClientHello.GROUP_X25519) {
            x25519Private = randomBytes(32);
            own[0] = X25519.publicKey(x25519Private);
        } else {
            ecdh = new Ecdh(group == ClientHello.GROUP_SECP256R1 ? EcCurve.P256 : group == ClientHello.GROUP_SECP384R1 ? EcCurve.P384 : EcCurve.P521, random);
            own[0] = ecdh.publicKey();
        }
        if (faults.contains(Fault.BAD_KEY_SHARE)) {
            own[0] = new byte[own[0].length];
        }
        byte[] params = new ByteWriter().u8(3).u16(group).vector8(own[0]).toByteArray();
        byte[] signedRandom = faults.contains(Fault.SIGNATURE_OVER_OTHER_CONTENT) ? randomBytes(32) : serverRandom;
        byte[] signature = sign(Bytes.concat(clientRandom, signedRandom, params));
        handshake(HandshakeIo.SERVER_KEY_EXCHANGE, new ByteWriter().bytes(params).u16(scheme).vector16(signature).toByteArray());
        if (requestCertificate) {
            handshake(HandshakeIo.CERTIFICATE_REQUEST,
                    new ByteWriter().vector8(new byte[] { 1, 64 }).vector16(new byte[] { 4, 3, 8, 4, 4, 1 }).vector16(new byte[0]).toByteArray());
        }
        handshake(HandshakeIo.SERVER_HELLO_DONE, faults.contains(Fault.HELLO_DONE_NOT_EMPTY) ? new byte[1] : new byte[0]);
        flush();
        if (faults.contains(Fault.BAD_SIGNATURE) || faults.contains(Fault.SIGNATURE_OVER_OTHER_CONTENT) || faults.contains(Fault.BAD_KEY_SHARE)
                || faults.contains(Fault.HELLO_DONE_NOT_EMPTY) || !clientAccepts12()) {
            awaitAlert();
            return false;
        }

        HandshakeIo.Message m = io.receive();
        if (requestCertificate) {
            if (m.type != HandshakeIo.CERTIFICATE) {
                throw new IllegalStateException("the client did not answer the certificate request");
            }
            clientCertificateListLength = new ByteReader(m.body).vector24().length;
            m = io.receive();
        }
        if (m.type != HandshakeIo.CLIENT_KEY_EXCHANGE) {
            throw new IllegalStateException("no ClientKeyExchange but message " + m.type);
        }
        byte[] peer = new ByteReader(m.body).vector8();
        byte[] premaster = x25519Private != null ? X25519.sharedSecret(x25519Private, peer) : ecdh.sharedSecret(peer);
        byte[] master = useEms ? Prf12.prf(hash, premaster, "extended master secret", io.transcriptHash(hash), 48)
                : Prf12.prf(hash, premaster, "master secret", Bytes.concat(clientRandom, serverRandom), 48);
        int kl = negotiated.keyLength();
        int il = negotiated.ivLength();
        byte[] block = Prf12.prf(hash, master, "key expansion", Bytes.concat(serverRandom, clientRandom), 2 * kl + 2 * il);
        RecordLayer.Record ccs = in.read();
        if (ccs == null || ccs.type != RecordLayer.CHANGE_CIPHER_SPEC) {
            throw new IllegalStateException("no change_cipher_spec of the client");
        }
        in.setReadProtection(negotiated, false, Bytes.copy(block, 0, kl), Bytes.copy(block, 2 * kl, il));
        byte[] expected = Prf12.prf(hash, master, "client finished", io.transcriptHash(hash), 12);
        HandshakeIo.Message finished = io.receive();
        if (finished.type != HandshakeIo.FINISHED || !Bytes.constantTimeEquals(expected, finished.body)) {
            throw new IllegalStateException("the Finished of the client is wrong");
        }
        if (!faults.contains(Fault.NO_CHANGE_CIPHER_SPEC)) {
            writeRecord(RecordLayer.CHANGE_CIPHER_SPEC, new byte[] { 1 });
        }
        byte[] mac = Prf12.prf(hash, master, "server finished", io.transcriptHash(hash), 12);
        if (faults.contains(Fault.BAD_FINISHED)) {
            mac[0] ^= 1;
        }
        if (faults.contains(Fault.NO_CHANGE_CIPHER_SPEC)) {
            handshake(HandshakeIo.FINISHED, mac);
            flush();
            awaitAlert();
            return false;
        }
        setWriteKeys(Bytes.copy(block, kl, kl), Bytes.copy(block, 2 * kl + il, il));
        handshake(HandshakeIo.FINISHED, mac);
        flush();
        if (faults.contains(Fault.BAD_FINISHED)) {
            awaitAlert();
            return false;
        }
        return true;
    }

    /**
     * Tells if the client can be expected to accept the TLS 1.2 flight: the group was offered and the key fits the suite.
     * <p>
     * 클라이언트가 TLS 1.2 비행을 받아들일 것으로 볼 수 있는지 알려 준다: 그룹을 제안했고 키가 스위트에 맞다.
     *
     * @return true if acceptable / 받아들일 수 있으면 true
     */
    private boolean clientAccepts12() {
        boolean rsaSuite = negotiated.auth() == CipherSuite.AUTH_RSA;
        boolean pkcs1 = (scheme & 0xff) == 0x01;
        boolean schemeFits = key.isRsa() ? (scheme & 0xff00) == 0x0800 || pkcs1 : (scheme & 0xff) == 0x03;
        return offeredGroups.contains(Integer.valueOf(group)) && rsaSuite == key.isRsa() && schemeFits && offeredSuites.contains(Integer.valueOf(suite));
    }
}
