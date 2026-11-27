package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.HashAlg;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.KeySchedule13;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Prf12;
import org.duckdns.hjow.swingwebpanel.tls13.x509.SignatureAlgorithm;
import org.duckdns.hjow.swingwebpanel.tls13.x509.SubjectKey;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Cert;
import org.duckdns.hjow.swingwebpanel.tls13.x509.X509Exception;

/**
 * The client side of the TLS handshake: one ClientHello that offers TLS 1.3 and TLS 1.2, then the full handshake of the version that
 * the server chooses (RFC 8446 section 4; RFC 5246 section 7.4 with ECDHE key exchange, RFC 8422). There is no session resumption, no
 * renegotiation and no client certificate; a certificate request is answered with an empty certificate list. After {@link #run()} the
 * record layer protects application data and the results are available from the getters. Example:
 * <pre>
 * ClientHandshake hs = new ClientHandshake(records, io, parameters, "www.example.com");
 * hs.run();
 * CipherSuite suite = hs.suite();
 * </pre>
 * <p>
 * TLS 핸드셰이크의 클라이언트 쪽: TLS 1.3 과 TLS 1.2 를 제안하는 ClientHello 하나를 보내고, 서버가 고른 버전의 전체 핸드셰이크를 한다(RFC 8446 4절;
 * ECDHE 키 교환의 RFC 5246 7.4절, RFC 8422). 세션 재개, 재협상, 클라이언트 인증서는 없다. 인증서 요청에는 빈 인증서 목록으로 답한다.
 * {@link #run()} 뒤에는 레코드 계층이 응용 데이터를 보호하고, 결과는 조회 메서드로 얻는다.
 */
public class ClientHandshake {

    /** The last bytes of the server random when a TLS 1.3 server negotiates TLS 1.2: "DOWNGRD" and 01. / TLS 1.3 서버가 TLS 1.2 를 협상할 때 서버 난수의 끝: "DOWNGRD" 와 01. */
    private static final int[] DOWNGRADE = { 0x44, 0x4f, 0x57, 0x4e, 0x47, 0x52, 0x44 };

    /** The record layer. / 레코드 계층. */
    private final RecordLayer records;

    /** The handshake message reader and writer. / 핸드셰이크 메시지 읽기·쓰기 도구. */
    private final HandshakeIo io;

    /** The settings. / 설정. */
    private final Parameters parameters;

    /** The host that the client connects to. / 클라이언트가 연결하는 호스트. */
    private final String host;

    /** The ClientHello state. / ClientHello 상태. */
    private final ClientHello hello;

    /** The negotiated cipher suite. / 협상한 암호 스위트. */
    private CipherSuite suite;

    /** True if TLS 1.3 was negotiated. / TLS 1.3 을 협상했으면 true. */
    private boolean tls13;

    /** The certificates as the server sent them. / 서버가 보낸 그대로의 인증서들. */
    private List<byte[]> peerChain = new ArrayList<byte[]>();

    /** The validated certification path. / 검증한 인증 경로. */
    private List<X509Cert> path;

    /** The application traffic secret of the client (TLS 1.3). / 클라이언트의 응용 트래픽 비밀(TLS 1.3). */
    private byte[] clientApplicationSecret;

    /** The application traffic secret of the server (TLS 1.3). / 서버의 응용 트래픽 비밀(TLS 1.3). */
    private byte[] serverApplicationSecret;

    /** True once the compatibility change_cipher_spec was sent. / 호환용 change_cipher_spec 을 보냈으면 true. */
    private boolean ccsSent;

    /**
     * Creates a handshake with a fresh ClientHello.
     * <p>
     * 새 ClientHello 로 핸드셰이크를 만든다.
     *
     * @param records    the record layer / 레코드 계층
     * @param io         the handshake message reader and writer / 핸드셰이크 메시지 읽기·쓰기 도구
     * @param parameters the settings / 설정
     * @param host       the host that the client connects to / 클라이언트가 연결하는 호스트
     */
    public ClientHandshake(RecordLayer records, HandshakeIo io, Parameters parameters, String host) {
        this(records, io, parameters, host, new ClientHello(parameters, host));
    }

    /**
     * Creates a handshake with a given ClientHello state (tests that replay recorded traces).
     * <p>
     * 주어진 ClientHello 상태로 핸드셰이크를 만든다(기록된 추적을 재현하는 시험).
     *
     * @param records    the record layer / 레코드 계층
     * @param io         the handshake message reader and writer / 핸드셰이크 메시지 읽기·쓰기 도구
     * @param parameters the settings / 설정
     * @param host       the host / 호스트
     * @param hello      the ClientHello state / ClientHello 상태
     */
    public ClientHandshake(RecordLayer records, HandshakeIo io, Parameters parameters, String host, ClientHello hello) {
        this.records = records;
        this.io = io;
        this.parameters = parameters;
        this.host = host;
        this.hello = hello;
    }

    /**
     * Returns the negotiated cipher suite.
     * <p>
     * 협상한 암호 스위트를 반환한다.
     *
     * @return the suite / 스위트
     */
    public CipherSuite suite() {
        return suite;
    }

    /**
     * Tells if TLS 1.3 was negotiated (otherwise TLS 1.2).
     * <p>
     * TLS 1.3 을 협상했는지 알려 준다(아니면 TLS 1.2).
     *
     * @return true for TLS 1.3 / TLS 1.3 이면 true
     */
    public boolean isTls13() {
        return tls13;
    }

    /**
     * Returns the certificates as the server sent them, the server certificate first.
     * <p>
     * 서버가 보낸 그대로의 인증서들을 반환한다. 서버 인증서가 먼저이다.
     *
     * @return the DER certificates / DER 인증서들
     */
    public List<byte[]> peerChain() {
        return peerChain;
    }

    /**
     * Returns the session identifier that the client sent.
     * <p>
     * 클라이언트가 보낸 세션 식별자를 반환한다.
     *
     * @return the identifier / 식별자
     */
    public byte[] sessionId() {
        return hello.sessionId();
    }

    /**
     * Returns the current application traffic secret of the client (TLS 1.3, for key updates).
     * <p>
     * 클라이언트의 현재 응용 트래픽 비밀을 반환한다(TLS 1.3, 키 갱신용).
     *
     * @return the secret / 비밀
     */
    byte[] clientApplicationSecret() {
        return clientApplicationSecret;
    }

    /**
     * Returns the current application traffic secret of the server (TLS 1.3, for key updates).
     * <p>
     * 서버의 현재 응용 트래픽 비밀을 반환한다(TLS 1.3, 키 갱신용).
     *
     * @return the secret / 비밀
     */
    byte[] serverApplicationSecret() {
        return serverApplicationSecret;
    }

    /**
     * Replaces an application traffic secret after a key update.
     * <p>
     * 키 갱신 뒤에 응용 트래픽 비밀을 바꾼다.
     *
     * @param client true for the secret of the client / 클라이언트의 비밀이면 true
     * @param secret the new secret / 새 비밀
     */
    void setApplicationSecret(boolean client, byte[] secret) {
        if (client) {
            Bytes.wipe(clientApplicationSecret);
            clientApplicationSecret = secret;
        } else {
            Bytes.wipe(serverApplicationSecret);
            serverApplicationSecret = secret;
        }
    }

    /**
     * Builds the encoded ClientHello message. Tests override this to replay a recorded ClientHello.
     * <p>
     * 부호화한 ClientHello 메시지를 만든다. 시험은 기록된 ClientHello 를 재현하려고 이것을 재정의한다.
     *
     * @param retryGroup the group of a HelloRetryRequest, or 0 / HelloRetryRequest 의 그룹 또는 0
     * @param cookie     the cookie of a HelloRetryRequest, or null / HelloRetryRequest 의 쿠키 또는 null
     * @return the message with its header / 헤더가 붙은 메시지
     */
    protected byte[] clientHelloMessage(int retryGroup, byte[] cookie) {
        return HandshakeIo.encode(HandshakeIo.CLIENT_HELLO, hello.build(retryGroup, cookie));
    }

    /**
     * Tells if the ClientHello offered an extension that a server may answer in EncryptedExtensions (besides ALPN, which is checked
     * separately). Tests that replay a recorded ClientHello with other extensions override this.
     * <p>
     * ClientHello 가 제안한 확장 가운데 서버가 EncryptedExtensions 로 답할 수 있는 것인지 알려 준다(따로 검사하는 ALPN 은 제외). 다른 확장을 가진
     * 기록된 ClientHello 를 재현하는 시험은 이것을 재정의한다.
     *
     * @param type the extension type / 확장 형식
     * @return true if the server may send it / 서버가 보낼 수 있으면 true
     */
    protected boolean offeredInEncryptedExtensions(int type) {
        return type == ClientHello.EXT_SERVER_NAME || type == ClientHello.EXT_SUPPORTED_GROUPS;
    }

    /**
     * Reads the next handshake message and checks its type.
     * <p>
     * 다음 핸드셰이크 메시지를 읽고 형식을 확인한다.
     *
     * @param type the expected handshake type / 기대하는 핸드셰이크 형식
     * @return the message / 메시지
     * @throws IOException on a read failure or an unexpected message / 읽기 실패 또는 예상 밖의 메시지
     */
    private HandshakeIo.Message expect(int type) throws IOException {
        return check(io.receive(), type);
    }

    /**
     * Checks the type of a message.
     * <p>
     * 메시지의 형식을 확인한다.
     *
     * @param m    the message / 메시지
     * @param type the expected handshake type / 기대하는 핸드셰이크 형식
     * @return the message / 메시지
     * @throws Tls13Exception unexpected_message if the type differs / 형식이 다르면 unexpected_message
     */
    private static HandshakeIo.Message check(HandshakeIo.Message m, int type) throws Tls13Exception {
        if (m.type != type) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "handshake message " + m.type + " instead of " + type);
        }
        return m;
    }

    /**
     * Sends the change_cipher_spec record of the middlebox compatibility mode once (RFC 8446 appendix D.4), if the ClientHello carried a
     * session identifier.
     * <p>
     * 미들박스 호환 모드의 change_cipher_spec 레코드를 한 번 보낸다(RFC 8446 부록 D.4). ClientHello 가 세션 식별자를 실었을 때만 보낸다.
     *
     * @throws IOException on a write failure / 쓰기 실패 시
     */
    private void sendCompatibilityCcs() throws IOException {
        if (!ccsSent && hello.sessionId().length > 0) {
            records.write(RecordLayer.CHANGE_CIPHER_SPEC, new byte[] { 1 }, 0, 1);
        }
        ccsSent = true;
    }

    /**
     * Runs the handshake to its end.
     * <p>
     * 핸드셰이크를 끝까지 수행한다.
     *
     * @throws IOException    on a read or write failure / 읽기·쓰기 실패 시
     * @throws Tls13Exception if the negotiation fails, the certificate is rejected or the server violates the protocol /
     *                        협상이 실패하거나 인증서를 거부하거나 서버가 프로토콜을 어기면
     */
    public void run() throws IOException {
        io.sendEncoded(clientHelloMessage(0, null), true);
        records.useRecordVersion12();
        HandshakeIo.Message m = expect(HandshakeIo.SERVER_HELLO);
        ServerHello sh = new ServerHello(m.body);
        if (sh.retry) {
            // HelloRetryRequest: 다른 그룹의 키 공유로 ClientHello 를 다시 보낸다(한 번만).
            CipherSuite s = CipherSuite.byId(sh.suite);
            if (!parameters.tls13 || sh.selectedVersion != 0x0304 || s == null || !s.isTls13() || !parameters.offers(s)) {
                throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "unacceptable HelloRetryRequest");
            }
            sh.allowOnly(ClientHello.EXT_SUPPORTED_VERSIONS, ClientHello.EXT_KEY_SHARE, ClientHello.EXT_COOKIE);
            if (sh.keyShareGroup == 0 || !parameters.offersGroup(sh.keyShareGroup) || sh.keyShareGroup == hello.sentGroup()) {
                throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "HelloRetryRequest selects group " + sh.keyShareGroup);
            }
            suite = s;
            io.removeLast(m);
            io.replaceByMessageHash(s.hash());
            io.appendToTranscript(HandshakeIo.encode(HandshakeIo.SERVER_HELLO, m.body));
            records.setDropPlainChangeCipherSpec(true);
            sendCompatibilityCcs();
            io.sendEncoded(clientHelloMessage(sh.keyShareGroup, sh.cookie), true);
            m = expect(HandshakeIo.SERVER_HELLO);
            ServerHello second = new ServerHello(m.body);
            if (second.retry) {
                throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "second HelloRetryRequest");
            }
            if (second.suite != s.id() || second.selectedVersion != 0x0304) {
                throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "ServerHello differs from the HelloRetryRequest");
            }
            sh = second;
        }
        if (sh.selectedVersion != 0 && sh.selectedVersion != 0x0304) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "supported_versions selects " + Integer.toHexString(sh.selectedVersion));
        }
        int version = sh.selectedVersion != 0 ? sh.selectedVersion : sh.legacyVersion;
        if (version == 0x0304 && parameters.tls13) {
            handshake13(sh);
        } else if (version == 0x0303 && parameters.tls12) {
            if (parameters.tls13) {
                // TLS 1.3 을 제안했는데 1.2 로 내려왔다면 다운그레이드 표식이 없어야 한다(RFC 8446 4.1.3절).
                boolean marked = true;
                // 서버 난수의 끝 8바이트가 "DOWNGRD" + 00 또는 01 인지 본다.
                for (int i = 0; i < 7; i++) {
                    marked &= (sh.random[24 + i] & 0xff) == DOWNGRADE[i];
                }
                if (marked && (sh.random[31] == 0 || sh.random[31] == 1)) {
                    throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "downgrade to TLS 1.2 detected");
                }
            }
            handshake12(sh);
        } else {
            throw new Tls13Exception(Tls13Exception.Kind.NEGOTIATION, Tls13Exception.PROTOCOL_VERSION, false,
                    "the server chose the protocol version " + Integer.toHexString(version) + " that this module does not offer");
        }
        hello.destroy();
    }

    /**
     * Validates the certificates of the server and returns the key of the server certificate.
     * <p>
     * 서버의 인증서들을 검증하고 서버 인증서의 키를 반환한다.
     *
     * @return the public key of the server / 서버의 공개 키
     * @throws Tls13Exception of the kind CERTIFICATE if the certificates are rejected / 인증서를 거부하면 종류가 CERTIFICATE 인 예외
     */
    private SubjectKey verifyCertificates() throws Tls13Exception {
        try {
            path = parameters.checker.check(peerChain, host, parameters.now());
            return path.get(0).key();
        } catch (X509Exception e) {
            int alert;
            switch (e.reason()) {
            case EXPIRED:
            case NOT_YET_VALID:
                alert = Tls13Exception.CERTIFICATE_EXPIRED;
                break;
            case UNTRUSTED:
            case DISTRUSTED:
                alert = Tls13Exception.UNKNOWN_CA;
                break;
            case MALFORMED:
            case BAD_SIGNATURE:
                alert = Tls13Exception.BAD_CERTIFICATE;
                break;
            case UNSUPPORTED_ALGORITHM:
            case WEAK_KEY:
            case WEAK_SIGNATURE:
                alert = Tls13Exception.UNSUPPORTED_CERTIFICATE;
                break;
            default:
                alert = Tls13Exception.CERTIFICATE_UNKNOWN;
                break;
            }
            Tls13Exception t = new Tls13Exception(Tls13Exception.Kind.CERTIFICATE, alert, false, "the certificate of the server was rejected: " + e.getMessage());
            t.initCause(e);
            throw t;
        }
    }

    /**
     * Reads a list of certificates of the form <code>opaque cert&lt;1..2^24-1&gt;</code>, each followed by extensions in TLS 1.3.
     * <p>
     * <code>opaque cert&lt;1..2^24-1&gt;</code> 형식의 인증서 목록을 읽는다. TLS 1.3 에서는 인증서마다 확장이 뒤따른다.
     *
     * @param list           the reader over the list / 목록을 읽는 해석기
     * @param withExtensions true for TLS 1.3 / TLS 1.3 이면 true
     * @throws Tls13Exception if the list is malformed or empty / 목록이 잘못되었거나 비었으면
     */
    private void readCertificates(ByteReader list, boolean withExtensions) throws Tls13Exception {
        // 인증서를 차례로 읽는다.
        while (list.remaining() > 0) {
            byte[] cert = list.vector24();
            if (cert.length == 0 || peerChain.size() >= 32) {
                throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "bad certificate list");
            }
            peerChain.add(cert);
            if (withExtensions) {
                list.vector16();
            }
        }
        if (peerChain.isEmpty()) {
            throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "the server sent no certificate");
        }
    }

    /**
     * Checks that a signature scheme was offered and fits the key of the server.
     * <p>
     * 서명 방식이 제안한 것이고 서버의 키에 맞는지 확인한다.
     *
     * @param scheme the SignatureScheme code point / SignatureScheme 코드
     * @param key    the key of the server / 서버의 키
     * @param v13    true for TLS 1.3 rules: no PKCS #1 v1.5, and the curve is bound to the scheme / TLS 1.3 규칙이면 true: PKCS #1 v1.5 금지, 곡선이 방식에 묶인다
     * @return the algorithm / 알고리즘
     * @throws Tls13Exception illegal_parameter if the scheme is not acceptable / 받아들일 수 없는 방식이면 illegal_parameter
     */
    private static SignatureAlgorithm scheme(int scheme, SubjectKey key, boolean v13) throws Tls13Exception {
        boolean offered = false;
        // 제안한 방식인지 본다.
        for (int i = 0; i < ClientHello.SIGNATURE_SCHEMES.length; i++) {
            offered |= ClientHello.SIGNATURE_SCHEMES[i] == scheme;
        }
        SignatureAlgorithm alg = offered ? SignatureAlgorithm.forTlsScheme(scheme) : null;
        boolean ok = alg != null;
        if (ok && alg.kind() == SignatureAlgorithm.ECDSA) {
            ok = key.isEc();
            if (ok && v13) {
                // TLS 1.3 에서는 ecdsa_secp256r1_sha256 은 P-256 키, ecdsa_secp384r1_sha384 는 P-384 키여야 한다.
                ok = key.curve().fieldBytes() == (scheme == 0x0403 ? 32 : 48);
            }
        } else if (ok) {
            ok = key.isRsa() && !(v13 && alg.kind() == SignatureAlgorithm.RSA_PKCS1);
        }
        if (!ok) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "signature scheme " + Integer.toHexString(scheme) + " is not acceptable");
        }
        return alg;
    }

    /**
     * The TLS 1.3 handshake after the ServerHello.
     * <p>
     * ServerHello 뒤의 TLS 1.3 핸드셰이크.
     *
     * @param sh the ServerHello / ServerHello
     * @throws IOException on a failure / 실패 시
     */
    private void handshake13(ServerHello sh) throws IOException {
        tls13 = true;
        CipherSuite s = CipherSuite.byId(sh.suite);
        if (s == null || !s.isTls13() || !parameters.offers(s) || (suite != null && suite != s)) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server chose the cipher suite " + Integer.toHexString(sh.suite));
        }
        suite = s;
        if (!Bytes.constantTimeEquals(sh.sessionId, hello.sessionId())) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server did not echo the session identifier");
        }
        sh.allowOnly(ClientHello.EXT_SUPPORTED_VERSIONS, ClientHello.EXT_KEY_SHARE);
        if (sh.keyShare == null || sh.keyShareGroup != hello.sentGroup()) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server key share is missing or of another group");
        }
        io.negotiated();
        records.setDropPlainChangeCipherSpec(true);
        HashAlg hash = s.hash();
        byte[] shared = hello.sharedSecret(sh.keyShareGroup, sh.keyShare);
        KeySchedule13 ks = new KeySchedule13(hash);
        ks.addSharedSecret(shared);
        Bytes.wipe(shared);
        byte[] helloHash = io.transcriptHash(hash);
        byte[] clientHs = ks.clientHandshakeTraffic(helloHash);
        byte[] serverHs = ks.serverHandshakeTraffic(helloHash);
        io.expectBoundary();
        records.setReadProtection(s, true, KeySchedule13.trafficKey(hash, serverHs, s.keyLength()), KeySchedule13.trafficIv(hash, serverHs));

        // EncryptedExtensions: 제안한 확장만 올 수 있다.
        ByteReader ee = new ByteReader(expect(HandshakeIo.ENCRYPTED_EXTENSIONS).body).sub16();
        // 확장을 차례로 확인한다.
        while (ee.remaining() > 0) {
            int type = ee.u16();
            ByteReader data = new ByteReader(ee.vector16());
            if (type == ClientHello.EXT_ALPN) {
                ServerHello.checkAlpn(data);
            } else if (!offeredInEncryptedExtensions(type)) {
                throw Tls13Exception.protocol(Tls13Exception.UNSUPPORTED_EXTENSION, "the server sent extension " + type + " that was not offered");
            }
        }

        HandshakeIo.Message m = io.receive();
        boolean certificateRequested = false;
        if (m.type == HandshakeIo.CERTIFICATE_REQUEST) {
            ByteReader cr = new ByteReader(m.body);
            if (cr.vector8().length != 0) {
                throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "certificate request context in the handshake");
            }
            cr.vector16();
            cr.expectEnd();
            certificateRequested = true;
            m = io.receive();
        }
        ByteReader cert = new ByteReader(check(m, HandshakeIo.CERTIFICATE).body);
        if (cert.vector8().length != 0) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "certificate request context of the server certificate");
        }
        readCertificates(cert.sub24(), true);
        cert.expectEnd();
        byte[] certHash = io.transcriptHash(hash);
        SubjectKey key = verifyCertificates();

        ByteReader cv = new ByteReader(expect(HandshakeIo.CERTIFICATE_VERIFY).body);
        SignatureAlgorithm alg = scheme(cv.u16(), key, true);
        byte[] signature = cv.vector16();
        cv.expectEnd();
        // 서명 대상: 공백 64개, 문맥 문자열, 0 바이트, 전사 해시(RFC 8446 4.4.3절).
        byte[] context = ascii("TLS 1.3, server CertificateVerify");
        byte[] content = new byte[64 + context.length + 1 + certHash.length];
        java.util.Arrays.fill(content, 0, 64, (byte) 0x20);
        System.arraycopy(context, 0, content, 64, context.length);
        System.arraycopy(certHash, 0, content, 65 + context.length, certHash.length);
        if (!alg.verify(key, content, signature)) {
            throw Tls13Exception.protocol(Tls13Exception.DECRYPT_ERROR, "the signature of CertificateVerify is wrong");
        }

        byte[] verifyHash = io.transcriptHash(hash);
        byte[] finished = expect(HandshakeIo.FINISHED).body;
        if (!Bytes.constantTimeEquals(finished, KeySchedule13.finishedMac(hash, serverHs, verifyHash))) {
            throw Tls13Exception.protocol(Tls13Exception.DECRYPT_ERROR, "the Finished value of the server is wrong");
        }
        byte[] finishedHash = io.transcriptHash(hash);
        ks.toMaster();
        clientApplicationSecret = ks.clientApplicationTraffic(finishedHash);
        serverApplicationSecret = ks.serverApplicationTraffic(finishedHash);
        ks.destroy();
        io.expectBoundary();
        records.setReadProtection(s, true, KeySchedule13.trafficKey(hash, serverApplicationSecret, s.keyLength()),
                KeySchedule13.trafficIv(hash, serverApplicationSecret));

        // 클라이언트의 둘째 비행: (호환용 CCS), [빈 Certificate], Finished.
        sendCompatibilityCcs();
        records.setWriteProtection(s, true, KeySchedule13.trafficKey(hash, clientHs, s.keyLength()), KeySchedule13.trafficIv(hash, clientHs));
        if (certificateRequested) {
            io.send(HandshakeIo.CERTIFICATE, new byte[] { 0, 0, 0, 0 });
        }
        io.send(HandshakeIo.FINISHED, KeySchedule13.finishedMac(hash, clientHs, io.transcriptHash(hash)));
        records.setWriteProtection(s, true, KeySchedule13.trafficKey(hash, clientApplicationSecret, s.keyLength()),
                KeySchedule13.trafficIv(hash, clientApplicationSecret));
        Bytes.wipe(clientHs);
        Bytes.wipe(serverHs);
    }

    /**
     * Converts ASCII text to bytes.
     * <p>
     * ASCII 문자열을 바이트로 바꾼다.
     *
     * @param s the text / 문자열
     * @return the bytes / 바이트
     */
    private static byte[] ascii(String s) {
        byte[] out = new byte[s.length()];
        // 문자 값을 그대로 옮긴다.
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) s.charAt(i);
        }
        return out;
    }

    /**
     * The TLS 1.2 handshake after the ServerHello: Certificate, ServerKeyExchange, [CertificateRequest], ServerHelloDone, then
     * [Certificate], ClientKeyExchange, ChangeCipherSpec, Finished, and the ChangeCipherSpec and Finished of the server.
     * <p>
     * ServerHello 뒤의 TLS 1.2 핸드셰이크: Certificate, ServerKeyExchange, [CertificateRequest], ServerHelloDone, 이어서 [Certificate],
     * ClientKeyExchange, ChangeCipherSpec, Finished, 그리고 서버의 ChangeCipherSpec 과 Finished.
     *
     * @param sh the ServerHello / ServerHello
     * @throws IOException on a failure / 실패 시
     */
    private void handshake12(ServerHello sh) throws IOException {
        tls13 = false;
        CipherSuite s = CipherSuite.byId(sh.suite);
        if (suite != null || s == null || s.isTls13() || !parameters.offers(s)) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server chose the cipher suite " + Integer.toHexString(sh.suite));
        }
        suite = s;
        if (sh.sessionId.length > 0 && Bytes.constantTimeEquals(sh.sessionId, hello.sessionId())) {
            // 세션 재개를 하지 않으므로, 서버가 우리의 임의 세션 식별자를 되돌려 보내 재개를 주장하면 거절한다.
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server tried to resume a session");
        }
        sh.allowOnly(ClientHello.EXT_RENEGOTIATION_INFO, ClientHello.EXT_EXTENDED_MASTER_SECRET, ClientHello.EXT_EC_POINT_FORMATS, ClientHello.EXT_ALPN,
                ClientHello.EXT_SERVER_NAME);
        io.negotiated();
        records.setDropPlainChangeCipherSpec(false);
        HashAlg hash = s.hash();
        byte[] clientRandom = hello.random();

        ByteReader cert = new ByteReader(expect(HandshakeIo.CERTIFICATE).body);
        readCertificates(cert.sub24(), false);
        cert.expectEnd();
        SubjectKey key = verifyCertificates();
        if (s.auth() == CipherSuite.AUTH_RSA ? !key.isRsa() : !key.isEc()) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the certificate key does not fit the cipher suite");
        }

        // ServerKeyExchange: ECParameters(named_curve), 공개 점, 그리고 그 위의 서명.
        byte[] ske = expect(HandshakeIo.SERVER_KEY_EXCHANGE).body;
        ByteReader r = new ByteReader(ske);
        if (r.u8() != 3) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server key exchange does not use a named curve");
        }
        int group = r.u16();
        byte[] serverPoint = r.vector8();
        int paramsLength = ske.length - r.remaining();
        if (!parameters.offersGroup(group)) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server chose the group " + group + " that was not offered");
        }
        SignatureAlgorithm alg = scheme(r.u16(), key, false);
        byte[] signature = r.vector16();
        r.expectEnd();
        byte[] signed = Bytes.concat(clientRandom, sh.random, Bytes.copy(ske, 0, paramsLength));
        if (!alg.verify(key, signed, signature)) {
            throw Tls13Exception.protocol(Tls13Exception.DECRYPT_ERROR, "the signature of the server key exchange is wrong");
        }

        HandshakeIo.Message m = io.receive();
        boolean certificateRequested = false;
        if (m.type == HandshakeIo.CERTIFICATE_REQUEST) {
            certificateRequested = true;
            m = io.receive();
        }
        if (check(m, HandshakeIo.SERVER_HELLO_DONE).body.length != 0) {
            throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "ServerHelloDone is not empty");
        }

        if (certificateRequested) {
            io.send(HandshakeIo.CERTIFICATE, new byte[] { 0, 0, 0 });
        }
        io.send(HandshakeIo.CLIENT_KEY_EXCHANGE, new ByteWriter().vector8(hello.keyShare(group)).toByteArray());
        byte[] premaster = hello.sharedSecret(group, serverPoint);
        byte[] master = sh.extendedMasterSecret ? Prf12.prf(hash, premaster, "extended master secret", io.transcriptHash(hash), 48)
                : Prf12.prf(hash, premaster, "master secret", Bytes.concat(clientRandom, sh.random), 48);
        Bytes.wipe(premaster);
        int kl = s.keyLength();
        int il = s.ivLength();
        byte[] block = Prf12.prf(hash, master, "key expansion", Bytes.concat(sh.random, clientRandom), 2 * kl + 2 * il);
        io.expectBoundary();
        records.write(RecordLayer.CHANGE_CIPHER_SPEC, new byte[] { 1 }, 0, 1);
        records.setWriteProtection(s, false, Bytes.copy(block, 0, kl), Bytes.copy(block, 2 * kl, il));
        io.send(HandshakeIo.FINISHED, Prf12.prf(hash, master, "client finished", io.transcriptHash(hash), 12));

        // 서버의 ChangeCipherSpec 은 핸드셰이크 메시지가 아닌 별도 레코드이다.
        RecordLayer.Record ccs = records.read();
        if (ccs == null) {
            throw new EOFException("the server closed the connection during the handshake");
        }
        if (ccs.type == RecordLayer.ALERT) {
            throw HandshakeIo.alert(ccs.data, false);
        }
        if (ccs.type != RecordLayer.CHANGE_CIPHER_SPEC || ccs.data.length != 1 || ccs.data[0] != 1) {
            throw Tls13Exception.protocol(Tls13Exception.UNEXPECTED_MESSAGE, "the server did not send change_cipher_spec");
        }
        records.setReadProtection(s, false, Bytes.copy(block, kl, kl), Bytes.copy(block, 2 * kl + il, il));
        Bytes.wipe(block);
        byte[] expected = Prf12.prf(hash, master, "server finished", io.transcriptHash(hash), 12);
        if (!Bytes.constantTimeEquals(expect(HandshakeIo.FINISHED).body, expected)) {
            throw Tls13Exception.protocol(Tls13Exception.DECRYPT_ERROR, "the Finished value of the server is wrong");
        }
        io.expectBoundary();
        Bytes.wipe(master);
    }
}
