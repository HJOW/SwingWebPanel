package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import javax.net.ssl.SSLException;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;
import org.duckdns.hjow.swingwebpanel.tls13.Rfc8448;

/**
 * X25519 of the TLS module (plan item P32-03-d) with the vectors of RFC 7748: the two single results and the iterated results of
 * section 5.2 (1 and 1,000 iterations), the Diffie-Hellman example of section 6.1, the key pairs of the RFC 8448 trace, and the rejection
 * of peer keys of small order (all-zero shared secret).
 * <p>
 * TLS 모듈의 X25519(계획 P32-03-d)를 RFC 7748 벡터로 시험한다: 5.2절의 단일 결과 둘과 반복 결과(1회, 1,000회), 6.1절의 디피-헬먼 예,
 * RFC 8448 추적의 키 쌍, 작은 차수의 상대 키 거부(모두 0 인 공유 비밀).
 */
public class X25519Test {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public X25519Test() {
    }

    /**
     * The two single test vectors of RFC 7748 section 5.2.
     * <p>
     * RFC 7748 5.2절의 단일 시험 벡터 둘.
     *
     * @throws SSLException never for these inputs / 이 입력에서는 발생하지 않는다
     */
    public void testSingleVectors() throws SSLException {
        Check.equal("c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552",
                Hex.encode(X25519.sharedSecret(Hex.decode("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4"),
                        Hex.decode("e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c"))), "first vector");
        Check.equal("95cbde9476e8907d7aade45cb4b873f88b595a68799fa152e6f8f7647aac7957",
                Hex.encode(X25519.sharedSecret(Hex.decode("4b66e9d4d1b4673c5ad22691957d6af5c11b6421e0ea01d42ca4169e7918ba0d"),
                        Hex.decode("e5210f12786811d3f4b7959d0538ae2c31dbe7106fc03c3efc4cd549c715a493"))), "second vector (top bit of u set)");
    }

    /**
     * The iterated test of RFC 7748 section 5.2: k and u start as the base point, each round sets k to X25519(k, u) and u to the old k.
     * <p>
     * RFC 7748 5.2절의 반복 시험: k 와 u 는 기준점에서 시작하고, 매 회 k 는 X25519(k, u), u 는 이전 k 가 된다.
     *
     * @throws SSLException never for these inputs / 이 입력에서는 발생하지 않는다
     */
    public void testIteratedVectors() throws SSLException {
        byte[] k = Hex.decode("0900000000000000000000000000000000000000000000000000000000000000");
        byte[] u = k.clone();
        // 1,000회 되풀이하며 1회와 1,000회의 값을 확인한다.
        for (int i = 1; i <= 1000; i++) {
            byte[] next = X25519.sharedSecret(k, u);
            u = k;
            k = next;
            if (i == 1) {
                Check.equal("422c8e7a6227d7bca1350b3e2bb7279f7897b87bb6854b783c60e80311ae3079", Hex.encode(k), "after one iteration");
            }
        }
        Check.equal("684cf59ba83309552800ef566f2f4d3c1c3887c49360e3875f2eb94d99532c51", Hex.encode(k), "after 1,000 iterations");
    }

    /**
     * The Diffie-Hellman example of RFC 7748 section 6.1 and the key pairs of the simple trace of RFC 8448.
     * <p>
     * RFC 7748 6.1절의 디피-헬먼 예와 RFC 8448 단순 추적의 키 쌍.
     *
     * @throws SSLException never for these inputs / 이 입력에서는 발생하지 않는다
     */
    public void testDiffieHellman() throws SSLException {
        byte[] alice = Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a");
        byte[] bob = Hex.decode("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb");
        byte[] alicePub = X25519.publicKey(alice);
        byte[] bobPub = X25519.publicKey(bob);
        Check.equal("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a", Hex.encode(alicePub), "public key of Alice");
        Check.equal("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f", Hex.encode(bobPub), "public key of Bob");
        String shared = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742";
        Check.equal(shared, Hex.encode(X25519.sharedSecret(alice, bobPub)), "shared secret computed by Alice");
        Check.equal(shared, Hex.encode(X25519.sharedSecret(bob, alicePub)), "shared secret computed by Bob");

        Rfc8448 rfc = Rfc8448.load();
        Rfc8448.Step client = rfc.step("3.", "{client}  create an ephemeral x25519 key pair", 0);
        Rfc8448.Step server = rfc.step("3.", "{server}  create an ephemeral x25519 key pair", 0);
        Check.equal(Hex.encode(client.get("public key")), Hex.encode(X25519.publicKey(client.get("private key"))), "client key share of the trace");
        Check.equal(Hex.encode(server.get("public key")), Hex.encode(X25519.publicKey(server.get("private key"))), "server key share of the trace");
        Check.equal(Hex.encode(rfc.step("3.", "{server}  extract secret \"handshake\"", 0).get("IKM")),
                Hex.encode(X25519.sharedSecret(client.get("private key"), server.get("public key"))), "shared secret of the trace");
    }

    /**
     * Peer keys of small order give an all-zero shared secret and are rejected; so are keys of the wrong length.
     * <p>
     * 작은 차수의 상대 키는 모두 0 인 공유 비밀을 내므로 거부한다. 길이가 틀린 키도 거부한다.
     */
    public void testSmallOrderPointsAreRejected() {
        final byte[] priv = Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a");
        String[] small = { "0000000000000000000000000000000000000000000000000000000000000000",
            "0100000000000000000000000000000000000000000000000000000000000000",
            "e0eb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b800",
            "5f9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f1157",
            "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
            "edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
            "eeffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f" };
        // 작은 차수의 점을 하나씩 넣어 본다.
        for (int i = 0; i < small.length; i++) {
            final byte[] peer = Hex.decode(small[i]);
            Check.throwsException(SSLException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() throws Exception {
                    X25519.sharedSecret(priv, peer);
                }
            }, "small order point " + small[i]);
        }
        Check.throwsException(SSLException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                X25519.sharedSecret(priv, new byte[31]);
            }
        }, "a peer key of 31 bytes");
    }
}
