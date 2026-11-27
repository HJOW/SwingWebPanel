package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.TestKey;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;

/**
 * Complete handshakes of the client against the scripted server (plan items P32-05-a and P32-05-c): every cipher suite and group of
 * TLS 1.3 and TLS 1.2, RSA and ECDSA server certificates with the signature schemes the client accepts, HelloRetryRequest, the
 * extended master secret on and off, certificate requests, and the content of the ClientHello as the server sees it. After each
 * handshake application data is echoed and the connection is closed in both directions.
 * <p>
 * 시험 서버를 상대로 한 클라이언트의 완전한 핸드셰이크(계획 P32-05-a, P32-05-c): TLS 1.3 과 TLS 1.2 의 모든 암호 스위트와 그룹, 클라이언트가 받아들이는
 * 서명 방식의 RSA·ECDSA 서버 인증서, HelloRetryRequest, 확장 마스터 비밀의 사용과 미사용, 인증서 요청, 서버가 본 ClientHello 의 내용. 핸드셰이크마다
 * 응용 데이터를 되돌려 받고 양방향으로 연결을 닫는다.
 */
public class HandshakeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HandshakeTest() {
    }

    /**
     * Runs one connection: handshake, echo of data of several sizes, orderly close; returns the server for further checks.
     * <p>
     * 연결 하나를 수행한다: 핸드셰이크, 여러 크기의 데이터 되돌리기, 정상 종료. 추가 확인을 위해 서버를 반환한다.
     *
     * @param what       the description of the case / 사례 설명
     * @param server     the server (not yet started) / 서버(아직 시작하지 않은 것)
     * @param parameters the settings of the client / 클라이언트의 설정
     * @param protocol   the expected protocol name / 기대하는 프로토콜 이름
     * @param suite      the expected cipher suite name / 기대하는 암호 스위트 이름
     * @return the server after it finished / 끝난 뒤의 서버
     * @throws Exception on failure / 실패 시
     */
    static ScriptedServer roundTrip(String what, ScriptedServer server, Parameters parameters, String protocol, String suite) throws Exception {
        server.start();
        TlsFixture.Client client = TlsFixture.connect(server, parameters);
        try {
            client.connection.handshake();
            Check.isTrue(client.connection.isEstablished(), what + ": established");
            Check.equal(protocol, client.connection.protocol(), what + ": protocol");
            Check.equal(suite, client.connection.suite().name(), what + ": cipher suite");
            Check.equal(1, client.connection.peerChain().size(), what + ": certificates of the server");
            int[] sizes = { 1, 100, 16384, 40000 };
            // 레코드 하나보다 작은 것부터 여러 레코드에 걸치는 것까지 되돌려 받는다.
            for (int i = 0; i < sizes.length; i++) {
                byte[] data = new byte[sizes[i]];
                // 위치마다 다른 값을 채운다.
                for (int k = 0; k < data.length; k++) {
                    data[k] = (byte) (k * 7 + i);
                }
                client.write(data);
                Check.isTrue(Arrays.equals(data, client.read(data.length)), what + ": echo of " + sizes[i] + " bytes");
            }
            client.connection.close();
            Check.equal(-1, client.connection.read(new byte[1], 0, 1), what + ": end of the stream after close");
        } finally {
            server.finish();
            client.socket.close();
        }
        Check.isTrue(server.established, what + ": the server completed the handshake");
        Check.equal(0, server.clientAlert, what + ": the server received close_notify");
        Check.equal(TlsFixture.HOST, server.sni, what + ": server name");
        return server;
    }

    /**
     * TLS 1.3 with each cipher suite, and what the ClientHello offers: versions 1.3 and 1.2, the three groups, the nine suites, the
     * legacy version 1.2 and the extended master secret for a TLS 1.2 answer.
     * <p>
     * 암호 스위트별 TLS 1.3, 그리고 ClientHello 가 제안하는 것: 버전 1.3 과 1.2, 그룹 셋, 스위트 아홉, 레거시 버전 1.2, TLS 1.2 응답을 위한 확장 마스터
     * 비밀.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13CipherSuites() throws Exception {
        int[] suites = { 0x1301, 0x1302, 0x1303 };
        String[] names = { "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256" };
        ScriptedServer last = null;
        // 스위트마다 연결 하나를 수행한다.
        for (int i = 0; i < suites.length; i++) {
            ScriptedServer server = TlsFixture.server(TlsFixture.rsaKey());
            server.suite = suites[i];
            last = roundTrip(names[i], server, TlsFixture.parameters(), "TLSv1.3", names[i]);
            Check.isFalse(last.retried, names[i] + ": no HelloRetryRequest with x25519");
        }
        Check.equal("[772, 771]", last.offeredVersions.toString(), "supported_versions: TLS 1.3 then TLS 1.2");
        Check.equal("[29, 23, 24]", last.offeredGroups.toString(), "supported_groups: x25519, secp256r1, secp384r1");
        Check.equal("[4865, 4866, 4867, 49195, 49199, 49196, 49200, 52393, 52392]", last.offeredSuites.toString(), "cipher suites in order of preference");
        Check.equal(0x0303, last.legacyVersion, "legacy_version");
        Check.isTrue(last.offeredEms, "extended_master_secret is offered");
        Check.equal("[0, 10, 13, 50, 16, 11, 23, 65281, 43, 51]", last.offeredExtensions.toString(), "extensions of the ClientHello");
    }

    /**
     * TLS 1.3 with the NIST groups (which need a HelloRetryRequest, because the first key share is x25519), with ECDSA certificates on
     * P-256 and P-384, without the compatibility change_cipher_spec, and with a client whose first group is P-256.
     * <p>
     * NIST 그룹의 TLS 1.3(첫 키 공유가 x25519 이므로 HelloRetryRequest 가 필요하다), P-256·P-384 의 ECDSA 인증서, 호환용 change_cipher_spec
     * 없음, 첫 그룹이 P-256 인 클라이언트.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls13GroupsAndCertificates() throws Exception {
        ScriptedServer p256 = TlsFixture.server(TlsFixture.p256Key());
        p256.group = ClientHello.GROUP_SECP256R1;
        Check.isTrue(roundTrip("P-256 group, ECDSA P-256", p256, TlsFixture.parameters(), "TLSv1.3", "TLS_AES_128_GCM_SHA256").retried, "HelloRetryRequest for P-256");

        ScriptedServer p384 = TlsFixture.server(TlsFixture.p384Key());
        p384.group = ClientHello.GROUP_SECP384R1;
        p384.suite = 0x1302;
        p384.compatCcs = false;
        Check.isTrue(roundTrip("P-384 group, ECDSA P-384", p384, TlsFixture.parameters(), "TLSv1.3", "TLS_AES_256_GCM_SHA384").retried, "HelloRetryRequest for P-384");

        ScriptedServer direct = TlsFixture.server(TlsFixture.rsaKey());
        direct.group = ClientHello.GROUP_SECP256R1;
        direct.scheme = 0x0806;
        Parameters first = TlsFixture.parameters();
        first.groups = new int[] { ClientHello.GROUP_SECP256R1, ClientHello.GROUP_X25519 };
        ScriptedServer done = roundTrip("P-256 as first group, rsa_pss_rsae_sha512", direct, first, "TLSv1.3", "TLS_AES_128_GCM_SHA256");
        Check.isFalse(done.retried, "no HelloRetryRequest when the key share is already P-256");
        Check.equal("[23, 29]", done.offeredGroups.toString(), "groups in the configured order");

        ScriptedServer pss384 = TlsFixture.server(TlsFixture.rsaKey());
        pss384.scheme = 0x0805;
        pss384.alpn = null;
        roundTrip("rsa_pss_rsae_sha384, no ALPN answer", pss384, TlsFixture.parameters(), "TLSv1.3", "TLS_AES_128_GCM_SHA256");
    }

    /**
     * A certificate request is answered with an empty certificate list in both versions, and the handshake goes on.
     * <p>
     * 두 버전 모두에서 인증서 요청에 빈 인증서 목록으로 답하고 핸드셰이크를 계속한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCertificateRequest() throws Exception {
        ScriptedServer v13 = TlsFixture.server(TlsFixture.rsaKey());
        v13.requestCertificate = true;
        Check.equal(0, roundTrip("TLS 1.3 certificate request", v13, TlsFixture.parameters(), "TLSv1.3", "TLS_AES_128_GCM_SHA256").clientCertificateListLength,
                "empty certificate list in TLS 1.3");
        ScriptedServer v12 = TlsFixture.server(TlsFixture.rsaKey());
        v12.tls13 = false;
        v12.suite = 0xc02f;
        v12.requestCertificate = true;
        Check.equal(0, roundTrip("TLS 1.2 certificate request", v12, TlsFixture.parameters(), "TLSv1.2", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
                .clientCertificateListLength, "empty certificate list in TLS 1.2");
    }

    /**
     * TLS 1.2 with each of the six cipher suites, the three groups, RSA (PKCS #1 and PSS) and ECDSA signatures, with and without the
     * extended master secret.
     * <p>
     * 여섯 암호 스위트, 세 그룹, RSA(PKCS #1, PSS)·ECDSA 서명, 확장 마스터 비밀 사용·미사용의 TLS 1.2.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTls12CipherSuites() throws Exception {
        int[] suites = { 0xc02f, 0xc030, 0xcca8, 0xc02b, 0xc02c, 0xcca9 };
        String[] names = { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256" };
        int[] groups = { ClientHello.GROUP_X25519, ClientHello.GROUP_SECP256R1, ClientHello.GROUP_SECP384R1 };
        int[] rsaSchemes = { 0x0401, 0x0804, 0x0601 };
        // 스위트마다 그룹, 서명 방식, 확장 마스터 비밀 사용을 바꿔 가며 연결한다.
        for (int i = 0; i < suites.length; i++) {
            boolean rsa = i < 3;
            TestKey key = rsa ? TlsFixture.rsaKey() : i == 4 ? TlsFixture.p384Key() : TlsFixture.p256Key();
            ScriptedServer server = TlsFixture.server(key);
            server.tls13 = false;
            server.suite = suites[i];
            server.group = groups[i % 3];
            server.ems = i % 2 == 0;
            if (rsa) {
                server.scheme = rsaSchemes[i];
            }
            String what = names[i] + ", group " + server.group + ", scheme " + Integer.toHexString(server.scheme) + (server.ems ? ", EMS" : ", no EMS");
            ScriptedServer done = roundTrip(what, server, TlsFixture.parameters(), "TLSv1.2", names[i]);
            Check.isFalse(done.retried, what + ": no HelloRetryRequest in TLS 1.2");
        }
    }

    /**
     * A client restricted to one version: TLS 1.2 only sends no TLS 1.3 extensions and suites; TLS 1.3 only sends no TLS 1.2 suites
     * and fails with a negotiation failure against a TLS 1.2 server, as TLS 1.2 only does against a TLS 1.3 server.
     * <p>
     * 한 버전으로 제한한 클라이언트: TLS 1.2 전용은 TLS 1.3 확장과 스위트를 보내지 않는다. TLS 1.3 전용은 TLS 1.2 스위트를 보내지 않고 TLS 1.2
     * 서버에 대해 협상 실패로 끝나며, TLS 1.2 전용도 TLS 1.3 서버에 대해 그렇다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSingleVersionClients() throws Exception {
        Parameters only12 = TlsFixture.parameters();
        only12.tls13 = false;
        ScriptedServer v12 = TlsFixture.server(TlsFixture.p256Key());
        v12.tls13 = false;
        v12.suite = 0xc02b;
        ScriptedServer done = roundTrip("TLS 1.2 only client", v12, only12, "TLSv1.2", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
        Check.equal("[]", done.offeredVersions.toString(), "no supported_versions");
        Check.equal("[49195, 49199, 49196, 49200, 52393, 52392]", done.offeredSuites.toString(), "only TLS 1.2 suites");
        Check.isFalse(done.offeredExtensions.contains(Integer.valueOf(ClientHello.EXT_KEY_SHARE)), "no key_share");

        Parameters only13 = TlsFixture.parameters();
        only13.tls12 = false;
        ScriptedServer v13 = TlsFixture.server(TlsFixture.rsaKey());
        done = roundTrip("TLS 1.3 only client", v13, only13, "TLSv1.3", "TLS_AES_128_GCM_SHA256");
        Check.equal("[772]", done.offeredVersions.toString(), "only TLS 1.3 in supported_versions");
        Check.equal("[4865, 4866, 4867]", done.offeredSuites.toString(), "only TLS 1.3 suites");
        Check.isFalse(done.offeredEms, "no extended_master_secret");

        ScriptedServer old = TlsFixture.server(TlsFixture.rsaKey());
        old.tls13 = false;
        old.suite = 0xc02f;
        only13 = TlsFixture.parameters();
        only13.tls12 = false;
        Tls13Exception e = TlsFixture.failure("TLS 1.3 only client, TLS 1.2 server", old, only13);
        Check.equal(Tls13Exception.Kind.NEGOTIATION, e.kind(), "negotiation failure: " + e.getMessage());
        Check.equal(Tls13Exception.PROTOCOL_VERSION, old.clientAlert, "protocol_version alert");

        ScriptedServer modern = TlsFixture.server(TlsFixture.rsaKey());
        only12 = TlsFixture.parameters();
        only12.tls13 = false;
        e = TlsFixture.failure("TLS 1.2 only client, TLS 1.3 server", modern, only12);
        Check.equal(Tls13Exception.Kind.PROTOCOL, e.kind(), "a ServerHello that selects a version that was not offered is a protocol error: " + e.getMessage());
    }

    /**
     * The session identifier of the client is 32 random bytes that differ between connections, as do the random values.
     * <p>
     * 클라이언트의 세션 식별자는 연결마다 다른 난수 32바이트이며, 난수 값도 연결마다 다르다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFreshRandomValues() throws Exception {
        Parameters p = TlsFixture.parameters();
        ClientHello a = new ClientHello(p, TlsFixture.HOST);
        ClientHello b = new ClientHello(p, TlsFixture.HOST);
        Check.equal(32, a.sessionId().length, "session identifier length");
        Check.isFalse(Hex.encode(a.sessionId()).equals(Hex.encode(b.sessionId())), "session identifiers differ");
        Check.isFalse(Hex.encode(a.random()).equals(Hex.encode(b.random())), "random values differ");
        Check.isFalse(Hex.encode(a.keyShare(ClientHello.GROUP_X25519)).equals(Hex.encode(b.keyShare(ClientHello.GROUP_X25519))), "x25519 shares differ");
        Check.isFalse(Hex.encode(a.keyShare(ClientHello.GROUP_SECP256R1)).equals(Hex.encode(b.keyShare(ClientHello.GROUP_SECP256R1))), "P-256 shares differ");
        byte[] hello = a.build(0, null);
        Check.equal(Hex.encode(hello), Hex.encode(a.build(0, null)), "building twice gives the same message");
        ClientHello ip = new ClientHello(p, "127.0.0.1");
        byte[] body = ip.build(0, null);
        ByteReader r = new ByteReader(body);
        r.bytes(34);
        r.vector8();
        r.vector16();
        r.vector8();
        ByteReader ext = r.sub16();
        Check.equal(ClientHello.EXT_SUPPORTED_GROUPS, ext.u16(), "no server_name extension for an IP address");
    }
}
