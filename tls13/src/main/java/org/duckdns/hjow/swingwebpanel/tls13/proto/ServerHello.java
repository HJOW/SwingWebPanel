package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;

/**
 * A parsed ServerHello or HelloRetryRequest (RFC 8446 section 4.1.3 and 4.1.4, RFC 5246 section 7.4.1.3). Both have the same
 * structure; a HelloRetryRequest is recognized by its special random value. Only the extensions that this module offers are accepted;
 * any other extension is an <code>unsupported_extension</code> error. Example:
 * <code>ServerHello sh = new ServerHello(message.body); if (sh.retry) { ... }</code>.
 * <p>
 * 해석한 ServerHello 또는 HelloRetryRequest(RFC 8446 4.1.3절·4.1.4절, RFC 5246 7.4.1.3절). 둘은 구조가 같고 HelloRetryRequest 는 특별한 난수
 * 값으로 알아본다. 이 모듈이 제안한 확장만 받아들이며, 그 밖의 확장은 <code>unsupported_extension</code> 오류이다.
 */
final class ServerHello {

    /** The random value that marks a HelloRetryRequest: SHA-256 of "HelloRetryRequest". / HelloRetryRequest 를 나타내는 난수 값: "HelloRetryRequest" 의 SHA-256. */
    private static final int[] RETRY_RANDOM = { 0xcf, 0x21, 0xad, 0x74, 0xe5, 0x9a, 0x61, 0x11, 0xbe, 0x1d, 0x8c, 0x02, 0x1e, 0x65, 0xb8, 0x91, 0xc2, 0xa2, 0x11, 0x16,
        0x7a, 0xbb, 0x8c, 0x5e, 0x07, 0x9e, 0x09, 0xe2, 0xc8, 0xa8, 0x33, 0x9c };

    /** The legacy version field. / 레거시 버전 필드. */
    final int legacyVersion;

    /** The server random. / 서버 난수. */
    final byte[] random;

    /** The echoed or chosen session identifier. / 되돌려 보냈거나 고른 세션 식별자. */
    final byte[] sessionId;

    /** The chosen cipher suite. / 고른 암호 스위트. */
    final int suite;

    /** True for a HelloRetryRequest. / HelloRetryRequest 이면 true. */
    final boolean retry;

    /** The version of the supported_versions extension, or 0 if absent. / supported_versions 확장의 버전. 없으면 0. */
    int selectedVersion;

    /** The group of the key_share extension, or 0 if absent. / key_share 확장의 그룹. 없으면 0. */
    int keyShareGroup;

    /** The key exchange value of the key_share extension (ServerHello only). / key_share 확장의 키 교환 값(ServerHello 에만 있다). */
    byte[] keyShare;

    /** The cookie of a HelloRetryRequest, or null. / HelloRetryRequest 의 쿠키 또는 null. */
    byte[] cookie;

    /** True if the server accepted extended_master_secret (TLS 1.2). / 서버가 extended_master_secret 을 받아들였으면 true(TLS 1.2). */
    boolean extendedMasterSecret;

    /** The extension types that were present. / 있었던 확장 형식들. */
    final List<Integer> extensions = new ArrayList<Integer>();

    /**
     * Parses the message body.
     * <p>
     * 메시지 본문을 해석한다.
     *
     * @param body the body of the handshake message / 핸드셰이크 메시지의 본문
     * @throws Tls13Exception if the message is malformed or carries an extension that was not offered / 메시지가 잘못되었거나 제안하지 않은 확장을 실었으면
     */
    ServerHello(byte[] body) throws Tls13Exception {
        ByteReader r = new ByteReader(body);
        legacyVersion = r.u16();
        random = r.bytes(32);
        sessionId = r.vector8();
        if (sessionId.length > 32) {
            throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "session identifier of " + sessionId.length + " bytes");
        }
        suite = r.u16();
        if (r.u8() != 0) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server chose a compression method");
        }
        boolean isRetry = true;
        // 난수가 HelloRetryRequest 의 값인지 본다.
        for (int i = 0; i < 32; i++) {
            isRetry &= (random[i] & 0xff) == RETRY_RANDOM[i];
        }
        retry = isRetry;
        // TLS 1.2 의 ServerHello 는 확장 블록이 아예 없을 수 있다.
        if (r.remaining() > 0) {
            ByteReader ext = r.sub16();
            // 확장을 차례로 읽는다.
            while (ext.remaining() > 0) {
                int type = ext.u16();
                ByteReader data = new ByteReader(ext.vector16());
                if (extensions.contains(Integer.valueOf(type))) {
                    throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "extension " + type + " appears twice");
                }
                extensions.add(Integer.valueOf(type));
                if (extension(type, data)) {
                    data.expectEnd();
                }
            }
        }
        r.expectEnd();
    }

    /**
     * Reads one extension.
     * <p>
     * 확장 하나를 읽는다.
     *
     * @param type the extension type / 확장 형식
     * @param data the extension data / 확장 데이터
     * @return false for an extension that this module does not know; it is only recorded here and refused by
     *         {@link #allowOnly(int...)} once the protocol version is known, so that a server of an older version is reported as a
     *         negotiation failure and not as a protocol error / 이 모듈이 모르는 확장이면 false. 여기서는 기록만 하고, 프로토콜 버전이 정해진 뒤
     *         {@link #allowOnly(int...)} 가 거절한다. 그래야 옛 버전의 서버가 프로토콜 오류가 아니라 협상 실패로 보고된다
     * @throws Tls13Exception if the extension is malformed / 확장이 잘못되었으면
     */
    private boolean extension(int type, ByteReader data) throws Tls13Exception {
        switch (type) {
        case ClientHello.EXT_SUPPORTED_VERSIONS:
            selectedVersion = data.u16();
            break;
        case ClientHello.EXT_KEY_SHARE:
            keyShareGroup = data.u16();
            if (!retry) {
                keyShare = data.vector16();
            }
            break;
        case ClientHello.EXT_COOKIE:
            cookie = data.vector16();
            if (cookie.length == 0) {
                throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "empty cookie");
            }
            break;
        case ClientHello.EXT_RENEGOTIATION_INFO:
            // 첫 핸드셰이크에서는 renegotiated_connection 이 비어 있어야 한다(RFC 5746 3.4절).
            if (data.vector8().length != 0) {
                throw Tls13Exception.protocol(Tls13Exception.HANDSHAKE_FAILURE, "renegotiation_info is not empty");
            }
            break;
        case ClientHello.EXT_EXTENDED_MASTER_SECRET:
            extendedMasterSecret = true;
            break;
        case ClientHello.EXT_EC_POINT_FORMATS:
            byte[] formats = data.vector8();
            boolean uncompressed = false;
            // 비압축 형식(0)이 들어 있어야 한다.
            for (int i = 0; i < formats.length; i++) {
                uncompressed |= formats[i] == 0;
            }
            if (!uncompressed) {
                throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server does not support uncompressed points");
            }
            break;
        case ClientHello.EXT_ALPN:
            checkAlpn(data);
            break;
        case ClientHello.EXT_SERVER_NAME:
            // 서버는 빈 server_name 으로 답한다. 내용은 보지 않는다.
            return false;
        default:
            return false;
        }
        return true;
    }

    /**
     * Checks the protocol that the server selected with ALPN: it must be the one protocol this module offers.
     * <p>
     * 서버가 ALPN 으로 고른 프로토콜을 확인한다: 이 모듈이 제안한 하나뿐인 프로토콜이어야 한다.
     *
     * @param data the extension data / 확장 데이터
     * @throws Tls13Exception if another protocol was selected / 다른 프로토콜을 골랐으면
     */
    static void checkAlpn(ByteReader data) throws Tls13Exception {
        ByteReader list = new ByteReader(data.vector16());
        byte[] name = list.vector8();
        list.expectEnd();
        String text = "";
        // 프로토콜 이름을 문자열로 바꾼다.
        for (int i = 0; i < name.length; i++) {
            text += (char) (name[i] & 0xff);
        }
        if (!"http/1.1".equals(text)) {
            throw Tls13Exception.protocol(Tls13Exception.ILLEGAL_PARAMETER, "the server selected the protocol " + text);
        }
    }

    /**
     * Checks that only extensions of an allowed set are present.
     * <p>
     * 허용한 집합의 확장만 있는지 확인한다.
     *
     * @param allowed the allowed extension types / 허용하는 확장 형식들
     * @throws Tls13Exception unsupported_extension if another extension is present / 다른 확장이 있으면 unsupported_extension
     */
    void allowOnly(int... allowed) throws Tls13Exception {
        // 있었던 확장마다 허용 목록에 있는지 본다.
        for (int i = 0; i < extensions.size(); i++) {
            boolean ok = false;
            // 허용 목록을 차례로 비교한다.
            for (int k = 0; k < allowed.length; k++) {
                ok |= allowed[k] == extensions.get(i).intValue();
            }
            if (!ok) {
                throw Tls13Exception.protocol(Tls13Exception.UNSUPPORTED_EXTENSION,
                        "the server sent extension " + extensions.get(i) + " that was not offered or is not allowed in this message");
            }
        }
    }
}
