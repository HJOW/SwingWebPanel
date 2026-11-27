package org.duckdns.hjow.swingwebpanel.tls13.proto;

import javax.net.ssl.SSLException;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.EcCurve;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Ecdh;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.X25519;
import org.duckdns.hjow.swingwebpanel.tls13.x509.HostNames;

/**
 * The ClientHello of a connection and the secrets that belong to it: the random value, the legacy session identifier and the private
 * keys of the key shares. One ClientHello serves TLS 1.3 and TLS 1.2 (RFC 8446 section 4.1.2 and appendix D): it lists
 * <code>supported_versions</code> 1.3 and 1.2, carries a key share for TLS 1.3, and the extensions that TLS 1.2 needs
 * (<code>ec_point_formats</code>, <code>extended_master_secret</code>, <code>renegotiation_info</code>). Example:
 * <pre>
 * ClientHello hello = new ClientHello(parameters, "www.example.com");
 * byte[] body = hello.build(0, null);                 // first ClientHello
 * byte[] secret = hello.sharedSecret(group, serverShare);
 * </pre>
 * <p>
 * 연결의 ClientHello 와 거기에 딸린 비밀: 난수 값, 레거시 세션 식별자, 키 공유의 개인 키. ClientHello 하나가 TLS 1.3 과 TLS 1.2 에 함께 쓰인다
 * (RFC 8446 4.1.2절, 부록 D): <code>supported_versions</code> 로 1.3 과 1.2 를 나열하고, TLS 1.3 용 키 공유와 TLS 1.2 에 필요한 확장
 * (<code>ec_point_formats</code>, <code>extended_master_secret</code>, <code>renegotiation_info</code>)을 싣는다.
 */
public class ClientHello {

    /** Named group x25519. / 이름 붙은 그룹 x25519. */
    public static final int GROUP_X25519 = 0x001d;

    /** Named group secp256r1. / 이름 붙은 그룹 secp256r1. */
    public static final int GROUP_SECP256R1 = 0x0017;

    /** Named group secp384r1. / 이름 붙은 그룹 secp384r1. */
    public static final int GROUP_SECP384R1 = 0x0018;

    /** Extension server_name. / 확장 server_name. */
    public static final int EXT_SERVER_NAME = 0;

    /** Extension supported_groups. / 확장 supported_groups. */
    public static final int EXT_SUPPORTED_GROUPS = 10;

    /** Extension ec_point_formats. / 확장 ec_point_formats. */
    public static final int EXT_EC_POINT_FORMATS = 11;

    /** Extension signature_algorithms. / 확장 signature_algorithms. */
    public static final int EXT_SIGNATURE_ALGORITHMS = 13;

    /** Extension application_layer_protocol_negotiation. / 확장 application_layer_protocol_negotiation. */
    public static final int EXT_ALPN = 16;

    /** Extension extended_master_secret. / 확장 extended_master_secret. */
    public static final int EXT_EXTENDED_MASTER_SECRET = 23;

    /** Extension supported_versions. / 확장 supported_versions. */
    public static final int EXT_SUPPORTED_VERSIONS = 43;

    /** Extension cookie. / 확장 cookie. */
    public static final int EXT_COOKIE = 44;

    /** Extension signature_algorithms_cert. / 확장 signature_algorithms_cert. */
    public static final int EXT_SIGNATURE_ALGORITHMS_CERT = 50;

    /** Extension key_share. / 확장 key_share. */
    public static final int EXT_KEY_SHARE = 51;

    /** Extension renegotiation_info. / 확장 renegotiation_info. */
    public static final int EXT_RENEGOTIATION_INFO = 0xff01;

    /** The signature schemes accepted in handshake signatures, in order of preference. / 핸드셰이크 서명에 받아들이는 서명 방식(선호 순서). */
    static final int[] SIGNATURE_SCHEMES = { 0x0403, 0x0503, 0x0804, 0x0805, 0x0806, 0x0401, 0x0501, 0x0601 };

    /** The signature schemes accepted in certificates: the handshake schemes and ECDSA on P-521. / 인증서에 받아들이는 서명 방식: 핸드셰이크 방식과 P-521 의 ECDSA. */
    static final int[] CERTIFICATE_SCHEMES = { 0x0403, 0x0503, 0x0603, 0x0804, 0x0805, 0x0806, 0x0401, 0x0501, 0x0601 };

    /** The settings of the connection. / 연결의 설정. */
    protected final Parameters parameters;

    /** The host name for the server_name extension, or null for an IP address. / server_name 확장의 호스트 이름. IP 주소이면 null. */
    private final String serverName;

    /** The client random. / 클라이언트 난수. */
    protected byte[] random;

    /** The legacy session identifier (32 random bytes, for middlebox compatibility). / 레거시 세션 식별자(난수 32바이트. 미들박스 호환용). */
    protected byte[] sessionId;

    /** The X25519 private key. / X25519 개인 키. */
    protected byte[] x25519Private;

    /** The ECDH key pairs by group, created on demand. / 그룹별 ECDH 키 쌍. 필요할 때 만든다. */
    private Ecdh p256, p384;

    /** The group of the key share in the hello that was built last. / 마지막으로 만든 hello 의 키 공유 그룹. */
    private int sentGroup;

    /**
     * Creates the hello state with fresh random values.
     * <p>
     * 새 난수 값으로 hello 상태를 만든다.
     *
     * @param parameters the settings / 설정
     * @param host       the host that the client connects to, or null / 클라이언트가 연결하는 호스트 또는 null
     */
    public ClientHello(Parameters parameters, String host) {
        this.parameters = parameters;
        String h = HostNames.normalize(host);
        this.serverName = h == null || HostNames.ipLiteral(h) != null ? null : h;
        this.random = new byte[32];
        this.sessionId = new byte[32];
        this.x25519Private = new byte[32];
        parameters.random.nextBytes(random);
        parameters.random.nextBytes(sessionId);
        parameters.random.nextBytes(x25519Private);
    }

    /**
     * Returns the client random.
     * <p>
     * 클라이언트 난수를 반환한다.
     *
     * @return 32 bytes / 32바이트
     */
    public byte[] random() {
        return random.clone();
    }

    /**
     * Returns the legacy session identifier that was sent.
     * <p>
     * 보낸 레거시 세션 식별자를 반환한다.
     *
     * @return the identifier / 식별자
     */
    public byte[] sessionId() {
        return sessionId.clone();
    }

    /**
     * Returns the group of the key share that was sent last.
     * <p>
     * 마지막으로 보낸 키 공유의 그룹을 반환한다.
     *
     * @return the group number / 그룹 번호
     */
    public int sentGroup() {
        return sentGroup;
    }

    /**
     * Returns the ECDH key pair of a NIST group, creating it on first use.
     * <p>
     * NIST 그룹의 ECDH 키 쌍을 반환한다. 처음 쓸 때 만든다.
     *
     * @param group the group number / 그룹 번호
     * @return the key pair / 키 쌍
     */
    private Ecdh ecdh(int group) {
        if (group == GROUP_SECP256R1) {
            if (p256 == null) {
                p256 = newEcdh(EcCurve.P256);
            }
            return p256;
        }
        if (p384 == null) {
            p384 = newEcdh(EcCurve.P384);
        }
        return p384;
    }

    /**
     * Generates the ECDH key pair of a curve. Tests override this to replay a recorded key pair.
     * <p>
     * 곡선의 ECDH 키 쌍을 만든다. 시험은 기록된 키 쌍을 재현하려고 이것을 재정의한다.
     *
     * @param curve the curve / 곡선
     * @return the key pair / 키 쌍
     */
    protected Ecdh newEcdh(EcCurve curve) {
        return new Ecdh(curve, parameters.random);
    }

    /**
     * Returns the public key share of a group.
     * <p>
     * 그룹의 공개 키 공유를 반환한다.
     *
     * @param group the group number / 그룹 번호
     * @return the key exchange value / 키 교환 값
     */
    public byte[] keyShare(int group) {
        return group == GROUP_X25519 ? X25519.publicKey(x25519Private) : ecdh(group).publicKey();
    }

    /**
     * Computes the shared secret with the key share of the server.
     * <p>
     * 서버의 키 공유로 공유 비밀을 계산한다.
     *
     * @param group     the group number / 그룹 번호
     * @param peerShare the key exchange value of the server / 서버의 키 교환 값
     * @return the shared secret / 공유 비밀
     * @throws Tls13Exception illegal_parameter if the value is invalid / 값이 유효하지 않으면 illegal_parameter
     */
    public byte[] sharedSecret(int group, byte[] peerShare) throws Tls13Exception {
        try {
            return group == GROUP_X25519 ? X25519.sharedSecret(x25519Private, peerShare) : ecdh(group).sharedSecret(peerShare);
        } catch (SSLException e) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "invalid key share of the server: " + e.getMessage());
        }
    }

    /**
     * Encodes a list of two byte values.
     * <p>
     * 2바이트 값들의 목록을 부호화한다.
     *
     * @param values the values / 값들
     * @return the bytes / 바이트
     */
    private static byte[] list16(int[] values) {
        ByteWriter w = new ByteWriter();
        // 값을 차례로 쓴다.
        for (int i = 0; i < values.length; i++) {
            w.u16(values[i]);
        }
        return w.toByteArray();
    }

    /**
     * Writes one extension.
     * <p>
     * 확장 하나를 쓴다.
     *
     * @param w    the writer / 작성기
     * @param type the extension type / 확장 형식
     * @param data the extension data / 확장 데이터
     */
    private static void extension(ByteWriter w, int type, byte[] data) {
        w.u16(type).vector16(data);
    }

    /**
     * Builds the body of the ClientHello message.
     * <p>
     * ClientHello 메시지의 본문을 만든다.
     *
     * @param retryGroup the group that a HelloRetryRequest selected, or 0 for the first ClientHello / HelloRetryRequest 가 고른 그룹. 첫 ClientHello 이면 0
     * @param cookie     the cookie of the HelloRetryRequest, or null / HelloRetryRequest 의 쿠키 또는 null
     * @return the message body / 메시지 본문
     */
    public byte[] build(int retryGroup, byte[] cookie) {
        ByteWriter w = new ByteWriter();
        w.u16(0x0303).bytes(random).vector8(sessionId);
        ByteWriter suites = new ByteWriter();
        // 켜 둔 버전의 스위트만 선호 순서대로 넣는다.
        for (int i = 0; i < parameters.suites.length; i++) {
            if (parameters.offers(parameters.suites[i])) {
                suites.u16(parameters.suites[i].id());
            }
        }
        w.vector16(suites.toByteArray());
        w.vector8(new byte[] { 0 });

        ByteWriter ext = new ByteWriter();
        if (serverName != null) {
            // ServerNameList: 형식 host_name(0) 인 이름 하나.
            byte[] name = new byte[serverName.length()];
            // 호스트 이름은 ASCII(A-레이블)이다.
            for (int i = 0; i < name.length; i++) {
                name[i] = (byte) serverName.charAt(i);
            }
            byte[] entry = new ByteWriter().u8(0).vector16(name).toByteArray();
            extension(ext, EXT_SERVER_NAME, new ByteWriter().vector16(entry).toByteArray());
        }
        extension(ext, EXT_SUPPORTED_GROUPS, new ByteWriter().vector16(list16(parameters.groups)).toByteArray());
        extension(ext, EXT_SIGNATURE_ALGORITHMS, new ByteWriter().vector16(list16(SIGNATURE_SCHEMES)).toByteArray());
        extension(ext, EXT_SIGNATURE_ALGORITHMS_CERT, new ByteWriter().vector16(list16(CERTIFICATE_SCHEMES)).toByteArray());
        byte[] http11 = { 'h', 't', 't', 'p', '/', '1', '.', '1' };
        extension(ext, EXT_ALPN, new ByteWriter().vector16(new ByteWriter().vector8(http11).toByteArray()).toByteArray());
        if (parameters.tls12) {
            extension(ext, EXT_EC_POINT_FORMATS, new byte[] { 1, 0 });
            extension(ext, EXT_EXTENDED_MASTER_SECRET, new byte[0]);
            extension(ext, EXT_RENEGOTIATION_INFO, new byte[] { 0 });
        }
        if (parameters.tls13) {
            ByteWriter versions = new ByteWriter().u16(0x0304);
            if (parameters.tls12) {
                versions.u16(0x0303);
            }
            extension(ext, EXT_SUPPORTED_VERSIONS, new ByteWriter().vector8(versions.toByteArray()).toByteArray());
            sentGroup = retryGroup != 0 ? retryGroup : parameters.groups[0];
            byte[] share = new ByteWriter().u16(sentGroup).vector16(keyShare(sentGroup)).toByteArray();
            extension(ext, EXT_KEY_SHARE, new ByteWriter().vector16(share).toByteArray());
            if (cookie != null) {
                extension(ext, EXT_COOKIE, new ByteWriter().vector16(cookie).toByteArray());
            }
        }
        w.vector16(ext.toByteArray());
        return w.toByteArray();
    }

    /**
     * Wipes the private keys.
     * <p>
     * 개인 키를 지운다.
     */
    public void destroy() {
        Bytes.wipe(x25519Private);
        if (p256 != null) {
            p256.destroy();
        }
        if (p384 != null) {
            p384.destroy();
        }
    }
}
