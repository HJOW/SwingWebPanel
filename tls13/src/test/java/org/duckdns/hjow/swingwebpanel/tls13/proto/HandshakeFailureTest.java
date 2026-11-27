package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.CertBuilder;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Failures;
import org.duckdns.hjow.swingwebpanel.tls13.x509.TrustStore;

/**
 * Handshakes that must fail (plan item P32-05-d): a server that misbehaves in one way at a time, and certificates that the client must
 * reject. Each case checks the kind of the failure, the alert that the client sends, and whether {@link Tls13Failures} reports it as a
 * negotiation failure: only "no version or cipher suite in common" may be retried with another TLS implementation, while a wrong
 * signature, a wrong Finished value, a rejected certificate or a downgrade marker may not.
 * <p>
 * 실패해야 하는 핸드셰이크(계획 P32-05-d): 한 번에 한 가지씩 잘못 동작하는 서버, 그리고 클라이언트가 거부해야 하는 인증서. 사례마다 실패의 종류,
 * 클라이언트가 보내는 경보, {@link Tls13Failures} 가 협상 실패로 보고하는지를 확인한다: "공통 버전이나 암호 스위트 없음"만 다른 TLS 구현으로 다시
 * 시도할 수 있고, 틀린 서명, 틀린 Finished 값, 거부된 인증서, 다운그레이드 표식은 그럴 수 없다.
 */
public class HandshakeFailureTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HandshakeFailureTest() {
    }

    /**
     * Creates a TLS 1.3 server with RSA certificate and one fault.
     * <p>
     * RSA 인증서와 결함 하나를 가진 TLS 1.3 서버를 만든다.
     *
     * @param fault the fault or null / 결함 또는 null
     * @return the server / 서버
     */
    private static ScriptedServer server13(ScriptedServer.Fault fault) {
        ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
        if (fault != null) {
            server.faults.add(fault);
        }
        return server;
    }

    /**
     * Creates a TLS 1.2 server with RSA certificate and one fault.
     * <p>
     * RSA 인증서와 결함 하나를 가진 TLS 1.2 서버를 만든다.
     *
     * @param fault the fault or null / 결함 또는 null
     * @return the server / 서버
     */
    private static ScriptedServer server12(ScriptedServer.Fault fault) {
        ScriptedServer server = server13(fault);
        server.tls13 = false;
        server.suite = 0xc02f;
        return server;
    }

    /**
     * Expects a failure that this side detects: a protocol error with the given alert, which the server receives, and which is not a
     * negotiation failure.
     * <p>
     * 이쪽에서 발견하는 실패를 기대한다: 주어진 경보의 프로토콜 오류이고, 서버가 그 경보를 받으며, 협상 실패가 아니다.
     *
     * @param what   the description / 설명
     * @param server the server / 서버
     * @param alert  the expected alert / 기대하는 경보
     * @throws Exception on failure / 실패 시
     */
    private static void protocolError(String what, ScriptedServer server, int alert) throws Exception {
        Tls13Exception e = TlsFixture.failure(what, server, TlsFixture.parameters());
        Check.equal(Tls13Exception.Kind.PROTOCOL, e.kind(), what + ": kind (" + e.getMessage() + ")");
        Check.equal(Tls13Exception.alertName(alert), Tls13Exception.alertName(e.alert()), what + ": alert (" + e.getMessage() + ")");
        Check.isFalse(e.received(), what + ": raised by the client");
        Check.equal(Tls13Exception.alertName(alert), Tls13Exception.alertName(server.clientAlert), what + ": alert that the server received");
        Check.equal(2, server.clientAlertLevel, what + ": the alert is fatal");
        Check.isFalse(Tls13Failures.isNegotiationFailure(e), what + ": not a negotiation failure");
        Check.isTrue(Tls13Failures.isModuleFailure(e), what + ": a failure of the module");
    }

    /**
     * Only refusals of the ClientHello and servers of an older version are negotiation failures.
     * <p>
     * ClientHello 의 거절과 옛 버전의 서버만 협상 실패이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNegotiationFailures() throws Exception {
        ScriptedServer.Fault[] refusals = { ScriptedServer.Fault.ALERT_HANDSHAKE_FAILURE, ScriptedServer.Fault.ALERT_PROTOCOL_VERSION };
        int[] alerts = { Tls13Exception.HANDSHAKE_FAILURE, Tls13Exception.PROTOCOL_VERSION };
        // 서버가 ClientHello 를 경보로 거절하는 경우.
        for (int i = 0; i < refusals.length; i++) {
            ScriptedServer server = server13(refusals[i]);
            Tls13Exception e = TlsFixture.failure("refusal " + refusals[i], server, TlsFixture.parameters());
            Check.equal(Tls13Exception.Kind.NEGOTIATION, e.kind(), refusals[i] + ": kind");
            Check.equal(alerts[i], e.alert(), refusals[i] + ": alert");
            Check.isTrue(e.received(), refusals[i] + ": received from the server");
            Check.equal(-1, server.clientAlert, refusals[i] + ": the client does not answer an alert with an alert");
            Check.isTrue(Tls13Failures.isNegotiationFailure(e), refusals[i] + ": negotiation failure");
            IOException wrapped = new IOException("wrapped");
            wrapped.initCause(new RuntimeException(e));
            Check.isTrue(Tls13Failures.isNegotiationFailure(wrapped), refusals[i] + ": found in the cause chain");
        }

        ScriptedServer.Fault[] old = { ScriptedServer.Fault.TLS11_HELLO, ScriptedServer.Fault.TLS10_HELLO_WITH_UNKNOWN_EXTENSION };
        // 서버가 TLS 1.1 이하의 ServerHello 로 답하는 경우.
        for (int i = 0; i < old.length; i++) {
            ScriptedServer server = server13(old[i]);
            Tls13Exception e = TlsFixture.failure("old server " + old[i], server, TlsFixture.parameters());
            Check.equal(Tls13Exception.Kind.NEGOTIATION, e.kind(), old[i] + ": kind (" + e.getMessage() + ")");
            Check.isFalse(e.received(), old[i] + ": raised by the client");
            Check.equal(Tls13Exception.PROTOCOL_VERSION, server.clientAlert, old[i] + ": protocol_version alert");
            Check.isTrue(Tls13Failures.isNegotiationFailure(e), old[i] + ": negotiation failure");
        }

        ScriptedServer other = server13(ScriptedServer.Fault.ALERT_INTERNAL_ERROR);
        Tls13Exception e = TlsFixture.failure("internal_error instead of ServerHello", other, TlsFixture.parameters());
        Check.equal(Tls13Exception.Kind.PEER_ALERT, e.kind(), "another alert is not a negotiation failure");
        Check.isFalse(Tls13Failures.isNegotiationFailure(e), "internal_error: no retry");

        Check.isFalse(Tls13Failures.isNegotiationFailure(null), "null");
        Check.isFalse(Tls13Failures.isNegotiationFailure(new java.net.ConnectException("refused")), "a connection failure is not a negotiation failure");
        Check.isFalse(Tls13Failures.isModuleFailure(new javax.net.ssl.SSLHandshakeException("of another implementation")), "exception of another implementation");
    }

    /**
     * A TLS 1.3 server that violates the protocol in its ServerHello or its flight.
     * <p>
     * ServerHello 나 그 뒤의 비행에서 프로토콜을 어기는 TLS 1.3 서버.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13ProtocolViolations() throws Exception {
        protocolError("session id not echoed", server13(ScriptedServer.Fault.WRONG_SESSION_ID), Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("unknown extension in ServerHello", server13(ScriptedServer.Fault.HELLO_EXTRA_EXTENSION), Tls13Exception.UNSUPPORTED_EXTENSION);
        protocolError("unknown extension in EncryptedExtensions", server13(ScriptedServer.Fault.ENCRYPTED_EXTRA_EXTENSION), Tls13Exception.UNSUPPORTED_EXTENSION);
        protocolError("no key share", server13(ScriptedServer.Fault.NO_KEY_SHARE), Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("all-zero x25519 key share", server13(ScriptedServer.Fault.BAD_KEY_SHARE), Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer badPoint = server13(ScriptedServer.Fault.BAD_KEY_SHARE);
        badPoint.group = ClientHello.GROUP_SECP256R1;
        protocolError("P-256 key share that is not a point", badPoint, Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("EncryptedExtensions missing", server13(ScriptedServer.Fault.SKIP_ENCRYPTED_EXTENSIONS), Tls13Exception.UNEXPECTED_MESSAGE);
        protocolError("CertificateVerify missing", server13(ScriptedServer.Fault.SKIP_CERTIFICATE_VERIFY), Tls13Exception.UNEXPECTED_MESSAGE);
        protocolError("damaged CertificateVerify signature", server13(ScriptedServer.Fault.BAD_SIGNATURE), Tls13Exception.DECRYPT_ERROR);
        protocolError("CertificateVerify over another transcript", server13(ScriptedServer.Fault.SIGNATURE_OVER_OTHER_CONTENT), Tls13Exception.DECRYPT_ERROR);
        protocolError("wrong Finished", server13(ScriptedServer.Fault.BAD_FINISHED), Tls13Exception.DECRYPT_ERROR);
        protocolError("handshake message across the key change", server13(ScriptedServer.Fault.MESSAGE_ACROSS_KEY_CHANGE), Tls13Exception.UNEXPECTED_MESSAGE);
        ScriptedServer twice = server13(ScriptedServer.Fault.RETRY_TWICE);
        twice.group = ClientHello.GROUP_SECP256R1;
        protocolError("second HelloRetryRequest", twice, Tls13Exception.UNEXPECTED_MESSAGE);
        protocolError("HelloRetryRequest for the group already sent", server13(ScriptedServer.Fault.RETRY_SAME_GROUP), Tls13Exception.ILLEGAL_PARAMETER);

        ScriptedServer ccm = server13(ScriptedServer.Fault.FOREIGN_SUITE);
        ccm.suite = 0x1304;
        protocolError("cipher suite that the module does not have", ccm, Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer notOffered = server13(ScriptedServer.Fault.FOREIGN_SUITE);
        notOffered.suite = 0x1302;
        Parameters one = TlsFixture.parameters();
        one.suites = new CipherSuite[] { CipherSuite.byId(0x1301), CipherSuite.byId(0xc02f) };
        Tls13Exception e = TlsFixture.failure("cipher suite that was not offered", notOffered, one);
        Check.equal(Tls13Exception.ILLEGAL_PARAMETER, e.alert(), "suite not offered: illegal_parameter");
        Check.equal("[4865, 49199]", notOffered.offeredSuites.toString(), "only the configured suites are offered");
    }

    /**
     * Signature schemes that TLS 1.3 does not allow in CertificateVerify: PKCS #1 v1.5, and an ECDSA scheme whose curve is not the curve
     * of the key. The server continues normally, so the alert is only checked on the client.
     * <p>
     * TLS 1.3 이 CertificateVerify 에 허용하지 않는 서명 방식: PKCS #1 v1.5, 그리고 곡선이 키의 곡선과 다른 ECDSA 방식. 서버는 정상적으로 계속하므로
     * 경보는 클라이언트에서만 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13SignatureSchemeRules() throws Exception {
        ScriptedServer pkcs1 = server13(null);
        pkcs1.scheme = 0x0401;
        Tls13Exception e = TlsFixture.failure("rsa_pkcs1_sha256 in CertificateVerify", pkcs1, TlsFixture.parameters());
        Check.equal(Tls13Exception.ILLEGAL_PARAMETER, e.alert(), "PKCS #1 v1.5 is refused: " + e.getMessage());
        Check.equal(Tls13Exception.Kind.PROTOCOL, e.kind(), "kind");

        ScriptedServer curve = TlsFixture.server(TlsFixture.p256Key());
        curve.scheme = 0x0503;
        e = TlsFixture.failure("ecdsa_secp384r1_sha384 with a P-256 key", curve, TlsFixture.parameters());
        Check.equal(Tls13Exception.ILLEGAL_PARAMETER, e.alert(), "curve bound to the scheme: " + e.getMessage());

        ScriptedServer sha1 = server13(null);
        sha1.scheme = 0x0201;
        e = TlsFixture.failure("a scheme that was not offered", sha1, TlsFixture.parameters());
        Check.equal(Tls13Exception.ILLEGAL_PARAMETER, e.alert(), "scheme not offered: " + e.getMessage());
    }

    /**
     * A TLS 1.2 server that violates the protocol, tries to resume or to downgrade.
     * <p>
     * 프로토콜을 어기거나, 재개를 시도하거나, 다운그레이드하려는 TLS 1.2 서버.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls12ProtocolViolations() throws Exception {
        protocolError("downgrade marker in the server random", server12(ScriptedServer.Fault.DOWNGRADE_MARKER), Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("session resumption", server12(ScriptedServer.Fault.RESUME), Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("unknown extension in ServerHello", server12(ScriptedServer.Fault.HELLO_EXTRA_EXTENSION), Tls13Exception.UNSUPPORTED_EXTENSION);
        protocolError("damaged ServerKeyExchange signature", server12(ScriptedServer.Fault.BAD_SIGNATURE), Tls13Exception.DECRYPT_ERROR);
        protocolError("ServerKeyExchange signed for another server random", server12(ScriptedServer.Fault.SIGNATURE_OVER_OTHER_CONTENT), Tls13Exception.DECRYPT_ERROR);
        protocolError("ServerHelloDone not empty", server12(ScriptedServer.Fault.HELLO_DONE_NOT_EMPTY), Tls13Exception.DECODE_ERROR);
        protocolError("all-zero x25519 public key", server12(ScriptedServer.Fault.BAD_KEY_SHARE), Tls13Exception.ILLEGAL_PARAMETER);
        protocolError("Finished without change_cipher_spec", server12(ScriptedServer.Fault.NO_CHANGE_CIPHER_SPEC), Tls13Exception.UNEXPECTED_MESSAGE);
        protocolError("wrong Finished", server12(ScriptedServer.Fault.BAD_FINISHED), Tls13Exception.DECRYPT_ERROR);

        ScriptedServer cbc = server12(ScriptedServer.Fault.FOREIGN_SUITE);
        cbc.suite = 0xc013;
        protocolError("CBC cipher suite", cbc, Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer tls13Suite = server12(ScriptedServer.Fault.FOREIGN_SUITE);
        tls13Suite.suite = 0x1301;
        protocolError("TLS 1.3 cipher suite in a TLS 1.2 ServerHello", tls13Suite, Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer ecdsaSuite = server12(null);
        ecdsaSuite.suite = 0xc02b;
        protocolError("ECDSA cipher suite with an RSA certificate", ecdsaSuite, Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer p521 = server12(null);
        p521.group = 0x0019;
        protocolError("group secp521r1 that was not offered", p521, Tls13Exception.ILLEGAL_PARAMETER);
        ScriptedServer ecdsaWithRsaKey = server12(null);
        ecdsaWithRsaKey.scheme = 0x0201;
        protocolError("signature scheme that was not offered", ecdsaWithRsaKey, Tls13Exception.ILLEGAL_PARAMETER);
    }

    /**
     * Alerts and an end of the stream after the ServerHello are failures, but not negotiation failures.
     * <p>
     * ServerHello 뒤의 경보와 스트림 끝은 실패이지만 협상 실패는 아니다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAlertOrCloseAfterServerHello() throws Exception {
        ScriptedServer[] alerting = { server13(ScriptedServer.Fault.ALERT_AFTER_HELLO), server12(ScriptedServer.Fault.ALERT_AFTER_HELLO) };
        // 두 버전 모두에서 ServerHello 뒤의 경보를 본다.
        for (int i = 0; i < alerting.length; i++) {
            Tls13Exception e = TlsFixture.failure("alert after ServerHello #" + i, alerting[i], TlsFixture.parameters());
            Check.equal(Tls13Exception.Kind.PEER_ALERT, e.kind(), "kind #" + i);
            Check.equal(Tls13Exception.INTERNAL_ERROR, e.alert(), "alert #" + i);
            Check.isTrue(e.received(), "received #" + i);
            Check.isFalse(Tls13Failures.isNegotiationFailure(e), "no retry #" + i);
        }
        ScriptedServer[] closing = { server13(ScriptedServer.Fault.CLOSE_AFTER_HELLO), server12(ScriptedServer.Fault.CLOSE_AFTER_HELLO) };
        // 두 버전 모두에서 ServerHello 뒤에 닫히는 연결을 본다.
        for (int i = 0; i < closing.length; i++) {
            closing[i].start();
            final TlsFixture.Client client = TlsFixture.connect(closing[i], TlsFixture.parameters());
            Throwable t = Check.throwsException(EOFException.class, new Block() {
                public void run() throws Exception {
                    client.connection.handshake();
                }
            }, "connection closed after ServerHello #" + i);
            Check.isFalse(Tls13Failures.isNegotiationFailure(t), "an end of the stream is not a negotiation failure #" + i);
            closing[i].join();
            client.socket.close();
        }
    }

    /**
     * Certificates that the client must reject, in both versions: unknown root, expired, another host name, no certificate for the
     * name at all. They are failures of the kind CERTIFICATE and never negotiation failures.
     * <p>
     * 클라이언트가 거부해야 하는 인증서(두 버전 모두): 모르는 루트, 만료, 다른 호스트 이름. 종류가 CERTIFICATE 인 실패이며 협상 실패가 아니다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRejectedCertificates() throws Exception {
        // 두 버전에서 같은 사례를 본다.
        for (int version = 0; version < 2; version++) {
            boolean v13 = version == 0;
            String tag = v13 ? "TLS 1.3 " : "TLS 1.2 ";

            ScriptedServer untrusted = v13 ? server13(null) : server12(null);
            Parameters otherRoot = new Parameters(TrustStore.ofCertificates(Arrays.asList(
                    new CertBuilder("Other Root", TestKey.rsa("other-root")).selfSigned().ca(-1).build())));
            otherRoot.fixedTime = CertBuilder.NOW;
            certificateError(tag + "unknown root", untrusted, otherRoot, Tls13Exception.UNKNOWN_CA);

            ScriptedServer expired = v13 ? server13(null) : server12(null);
            Parameters later = TlsFixture.parameters();
            later.fixedTime = System.currentTimeMillis() + 60 * CertBuilder.DAY;
            certificateError(tag + "expired", expired, later, Tls13Exception.CERTIFICATE_EXPIRED);

            ScriptedServer early = v13 ? server13(null) : server12(null);
            Parameters before = TlsFixture.parameters();
            before.fixedTime = System.currentTimeMillis() - 10 * CertBuilder.DAY;
            certificateError(tag + "not yet valid", early, before, Tls13Exception.CERTIFICATE_EXPIRED);

            TestKey key = TlsFixture.rsaKey();
            List<byte[]> wrongName = new ArrayList<byte[]>();
            long now = System.currentTimeMillis();
            wrongName.add(new CertBuilder("other.example.test", key).issuer("TLS Test Root", TestKey.rsa("tls-root")).dns("other.example.test")
                    .validity(now - CertBuilder.DAY, now + CertBuilder.DAY).build());
            ScriptedServer mismatch = new ScriptedServer(key, wrongName);
            if (!v13) {
                mismatch.tls13 = false;
                mismatch.suite = 0xc02f;
            }
            certificateError(tag + "another host name", mismatch, TlsFixture.parameters(), Tls13Exception.CERTIFICATE_UNKNOWN);

            List<byte[]> selfSigned = new ArrayList<byte[]>();
            selfSigned.add(new CertBuilder(TlsFixture.HOST, key).selfSigned().dns(TlsFixture.HOST).validity(now - CertBuilder.DAY, now + CertBuilder.DAY).build());
            ScriptedServer self = new ScriptedServer(key, selfSigned);
            if (!v13) {
                self.tls13 = false;
                self.suite = 0xc02f;
            }
            certificateError(tag + "self-signed", self, TlsFixture.parameters(), Tls13Exception.UNKNOWN_CA);
        }
    }

    /**
     * Expects the rejection of the certificate with an alert.
     * <p>
     * 그 경보로 인증서를 거부함을 기대한다.
     *
     * @param what       the description / 설명
     * @param server     the server / 서버
     * @param parameters the settings of the client / 클라이언트의 설정
     * @param alert      the expected alert / 기대하는 경보
     * @throws Exception on failure / 실패 시
     */
    private static void certificateError(String what, ScriptedServer server, Parameters parameters, int alert) throws Exception {
        Tls13Exception e = TlsFixture.failure(what, server, parameters);
        Check.equal(Tls13Exception.Kind.CERTIFICATE, e.kind(), what + ": kind (" + e.getMessage() + ")");
        Check.equal(Tls13Exception.alertName(alert), Tls13Exception.alertName(e.alert()), what + ": alert (" + e.getMessage() + ")");
        Check.notNull(e.getCause(), what + ": the reason is the cause");
        Check.isFalse(Tls13Failures.isNegotiationFailure(e), what + ": never retried with another implementation");
        Check.isFalse(server.established, what + ": the server did not complete the handshake");
    }
}
