package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;

/**
 * ChaCha20-Poly1305 of the TLS module (plan item P32-03-c) with the vectors of RFC 8439: the block function (section 2.3.2), Poly1305
 * (section 2.5.2 and the edge cases 5 to 11 of appendix A.3), the AEAD (section 2.8.2), the decryption example of appendix A.5, and the
 * rejection of forgeries.
 * <p>
 * TLS 모듈의 ChaCha20-Poly1305(계획 P32-03-c)를 RFC 8439 벡터로 시험한다: 블록 함수(2.3.2절), Poly1305(2.5.2절과 부록 A.3 의 경계 사례 5~11),
 * AEAD(2.8.2절), 부록 A.5 의 복호화 예, 위조 거부.
 */
public class ChaChaPolyTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ChaChaPolyTest() {
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
     * Computes a Poly1305 tag, feeding the data in pieces of the given size.
     * <p>
     * 데이터를 주어진 크기의 조각으로 넣어 Poly1305 태그를 계산한다.
     *
     * @param r     the key part r in hexadecimal / 16진수 키 부분 r
     * @param s     the key part s in hexadecimal / 16진수 키 부분 s
     * @param data  the message / 메시지
     * @param piece the size of the pieces / 조각 크기
     * @return the tag in hexadecimal / 16진수 태그
     */
    private static String poly(String r, String s, byte[] data, int piece) {
        Poly1305 mac = new Poly1305(Hex.decode(r + s), 0);
        // 조각 단위로 넣는다.
        for (int at = 0; at < data.length; at += piece) {
            mac.update(data, at, Math.min(piece, data.length - at));
        }
        return Hex.encode(mac.finish());
    }

    /**
     * The block function gives the serialized block of RFC 8439 section 2.3.2.
     * <p>
     * 블록 함수가 RFC 8439 2.3.2절의 직렬화된 블록을 낸다.
     */
    public void testBlockFunction() {
        ChaCha20Poly1305 c = new ChaCha20Poly1305(Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"));
        Check.equal("10f1e7e4d13b5915500fdd1fa32071c4c7d1f4c733c068030422aa9ac3d46c4e"
                + "d2826446079faa0914c2d705d98b02a2b5129cd1de164eb9cbd083e8a2503c4e",
                Hex.encode(c.keyStreamBlock(Hex.decode("000000090000004a00000000"), 1)), "serialized block");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new ChaCha20Poly1305(new byte[16]);
            }
        }, "a key of 16 bytes");
    }

    /**
     * Poly1305 gives the tag of section 2.5.2 and of the edge cases of appendix A.3, whatever the size of the pieces.
     * <p>
     * Poly1305 가 2.5.2절과 부록 A.3 경계 사례의 태그를 낸다(조각 크기와 무관).
     */
    public void testPoly1305Vectors() {
        String zero = "00000000000000000000000000000000";
        String ones = "ffffffffffffffffffffffffffffffff";
        int[] pieces = { 1, 7, 16, 33, 1000 };
        // 조각 크기를 바꿔 가며 같은 결과가 나오는지 본다.
        for (int i = 0; i < pieces.length; i++) {
            int p = pieces[i];
            Check.equal("a8061dc1305136c6c22b8baf0c0127a9", poly("85d6be7857556d337f4452fe42d506a8", "0103808afb0db2fd4abff6af4149f51b",
                    ascii("Cryptographic Forum Research Group"), p), "section 2.5.2, pieces of " + p);
            Check.equal("03000000000000000000000000000000", poly("02000000000000000000000000000000", zero, Hex.decode(ones), p), "A.3 #5");
            Check.equal("03000000000000000000000000000000", poly("02000000000000000000000000000000", ones,
                    Hex.decode("02000000000000000000000000000000"), p), "A.3 #6");
            Check.equal("05000000000000000000000000000000", poly("01000000000000000000000000000000", zero,
                    Hex.decode(ones + "f0ffffffffffffffffffffffffffffff" + "11000000000000000000000000000000"), p), "A.3 #7");
            Check.equal(zero, poly("01000000000000000000000000000000", zero,
                    Hex.decode(ones + "fbfefefefefefefefefefefefefefefe" + "01010101010101010101010101010101"), p), "A.3 #8");
            Check.equal("faffffffffffffffffffffffffffffff", poly("02000000000000000000000000000000", zero,
                    Hex.decode("fdffffffffffffffffffffffffffffff"), p), "A.3 #9");
            String data10 = "e33594d7505e43b90000000000000000" + "3394d7505e4379cd0100000000000000" + zero;
            Check.equal("14000000000000005500000000000000", poly("01000000000000000400000000000000", zero,
                    Hex.decode(data10 + "01000000000000000000000000000000"), p), "A.3 #10");
            Check.equal("13000000000000000000000000000000", poly("01000000000000000400000000000000", zero, Hex.decode(data10), p), "A.3 #11");
        }
    }

    /**
     * The AEAD gives the ciphertext and the tag of RFC 8439 section 2.8.2 and opens it again.
     * <p>
     * AEAD 가 RFC 8439 2.8.2절의 암호문과 태그를 내고 다시 연다.
     */
    public void testAeadVector() {
        Aead aead = new ChaCha20Poly1305(Hex.decode("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"));
        byte[] nonce = Hex.decode("070000004041424344454647");
        byte[] aad = Hex.decode("50515253c0c1c2c3c4c5c6c7");
        byte[] plain = ascii("Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.");
        byte[] out = new byte[plain.length + 16];
        aead.seal(nonce, aad, plain, 0, plain.length, out, 0);
        Check.equal("d31a8d34648e60db7b86afbc53ef7ec2a4aded51296e08fea9e2b5a736ee62d63dbea45e8ca9671282fafb69da92728b"
                + "1a71de0a9e060b2905d6a5b67ecd3b3692ddbd7f2d778b8c9803aee328091b58fab324e4fad675945585808b4831d7bc"
                + "3ff4def08e4b7a9de576d26586cec64b6116" + "1ae10b594f09e26a7e902ecbd0600691", Hex.encode(out), "ciphertext and tag");
        byte[] back = new byte[plain.length];
        Check.isTrue(aead.open(nonce, aad, out, 0, out.length, back, 0), "the sealed data opens");
        Check.equal(Hex.encode(plain), Hex.encode(back), "opening gives the plaintext");
        // 제자리 암호화와 복호화도 같은 결과여야 한다.
        byte[] inPlace = new byte[plain.length + 16];
        System.arraycopy(plain, 0, inPlace, 0, plain.length);
        aead.seal(nonce, aad, inPlace, 0, plain.length, inPlace, 0);
        Check.equal(Hex.encode(out), Hex.encode(inPlace), "sealing in place");
        Check.isTrue(aead.open(nonce, aad, inPlace, 0, inPlace.length, inPlace, 0), "opening in place");
        Check.equal(Hex.encode(plain), Hex.encode(Bytes.copy(inPlace, 0, plain.length)), "plaintext in place");
    }

    /**
     * The decryption example of appendix A.5: the received tag is accepted and the plaintext is the expected text.
     * <p>
     * 부록 A.5 의 복호화 예: 받은 태그를 받아들이고 평문이 기대한 글이다.
     */
    public void testDecryptionExample() {
        Aead aead = new ChaCha20Poly1305(Hex.decode("1c9240a5eb55d38af333888604f6b5f0473917c1402b80099dca5cbc207075c0"));
        byte[] sealed = Hex.decode("64a0861575861af460f062c79be643bd5e805cfd345cf389f108670ac76c8cb2"
                + "4c6cfc18755d43eea09ee94e382d26b0bdb7b73c321b0100d4f03b7f355894cf"
                + "332f830e710b97ce98c8a84abd0b948114ad176e008d33bd60f982b1ff37c855"
                + "9797a06ef4f0ef61c186324e2b3506383606907b6a7c02b0f9f6157b53c867e4"
                + "b9166c767b804d46a59b5216cde7a4e99040c5a40433225ee282a1b0a06c523e"
                + "af4534d7f83fa1155b0047718cbc546a0d072b04b3564eea1b422273f548271a"
                + "0bb2316053fa76991955ebd63159434ecebb4e466dae5a1073a6727627097a10"
                + "49e617d91d361094fa68f0ff77987130305beaba2eda04df997b714d6c6f2c29"
                + "a6ad5cb4022b02709b" + "eead9d67890cbb22392336fea1851f38");
        byte[] nonce = Hex.decode("000000000102030405060708");
        byte[] aad = Hex.decode("f33388860000000000004e91");
        byte[] plain = new byte[sealed.length - 16];
        Check.isTrue(aead.open(nonce, aad, sealed, 0, sealed.length, plain, 0), "the received tag is accepted");
        StringBuilder text = new StringBuilder();
        // 평문은 ASCII 글이다(따옴표 문자만 Latin-1 이 아니라 UTF-8 로 들어 있으므로 앞부분만 본다).
        for (int i = 0; i < 68; i++) {
            text.append((char) (plain[i] & 0xff));
        }
        Check.equal("Internet-Drafts are draft documents valid for a maximum of six month", text.toString(), "the beginning of the plaintext");
        Check.equal(265, plain.length, "plaintext length");
    }

    /**
     * A change of any single bit of the ciphertext, the tag or the associated data is rejected, and nothing is written to the output.
     * <p>
     * 암호문, 태그, 연관 데이터의 어느 한 비트를 바꿔도 거부하며 출력에 아무것도 쓰지 않는다.
     */
    public void testForgeriesAreRejected() {
        Aead aead = new ChaCha20Poly1305(Hex.decode("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f"));
        byte[] nonce = Hex.decode("070000004041424344454647");
        byte[] aad = Hex.decode("50515253c0c1c2c3c4c5c6c7");
        byte[] plain = ascii("Ladies and Gentlemen of the class of '99");
        byte[] sealed = new byte[plain.length + 16];
        aead.seal(nonce, aad, plain, 0, plain.length, sealed, 0);
        // 암호문과 태그의 모든 비트를 하나씩 뒤집어 본다.
        for (int bit = 0; bit < sealed.length * 8; bit++) {
            byte[] forged = sealed.clone();
            forged[bit / 8] ^= (byte) (1 << (bit % 8));
            byte[] out = new byte[plain.length];
            Check.isFalse(aead.open(nonce, aad, forged, 0, forged.length, out, 0), "flipped bit " + bit + " of ciphertext or tag");
            Check.equal(Hex.encode(new byte[plain.length]), Hex.encode(out), "no plaintext is written for a forgery");
        }
        // 연관 데이터의 모든 비트를 하나씩 뒤집어 본다.
        for (int bit = 0; bit < aad.length * 8; bit++) {
            byte[] other = aad.clone();
            other[bit / 8] ^= (byte) (1 << (bit % 8));
            Check.isFalse(aead.open(nonce, other, sealed, 0, sealed.length, new byte[plain.length], 0), "flipped bit " + bit + " of the associated data");
        }
        Check.isFalse(aead.open(nonce, aad, sealed, 0, sealed.length - 1, new byte[plain.length], 0), "truncated by one byte");
        Check.isFalse(aead.open(nonce, aad, sealed, 0, 15, new byte[plain.length], 0), "shorter than a tag");
        Check.isTrue(aead.open(nonce, aad, sealed, 0, sealed.length, new byte[plain.length], 0), "the original still opens");
        aead.destroy();
    }
}
