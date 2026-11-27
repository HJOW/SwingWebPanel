package org.duckdns.hjow.swingwebpanel.tls13.crypto;

import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Hex;

/**
 * AES and AES-GCM of the TLS module (plan item P32-03-b): the S-box circuit against the definition (inverse in GF(2^8) and affine map),
 * the block cipher with the vectors of FIPS 197 appendices B and C, GCM with test cases 1 to 4 and 13 to 16 of the GCM specification
 * (McGrew and Viega), GHASH against a bit by bit reference multiplication, and the rejection of forged ciphertext, tag and associated data.
 * <p>
 * TLS 모듈의 AES 와 AES-GCM(계획 P32-03-b): 정의(GF(2^8) 역원과 아핀 변환)와 비교한 S-box 회로, FIPS 197 부록 B·C 벡터의 블록 암호,
 * GCM 규격(McGrew, Viega)의 시험 사례 1~4, 13~16, 비트 단위 기준 곱셈과 비교한 GHASH, 위조한 암호문·태그·연관 데이터의 거부.
 */
public class AesGcmTest {

    /** The 64 byte plaintext of GCM test cases 3 and 15. / GCM 시험 사례 3, 15 의 64바이트 평문. */
    private static final String PLAIN = "d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72"
            + "1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AesGcmTest() {
    }

    /**
     * Multiplies in GF(2^8) modulo x^8 + x^4 + x^3 + x + 1 (reference for the S-box).
     * <p>
     * GF(2^8) 에서 x^8 + x^4 + x^3 + x + 1 을 법으로 곱한다(S-box 기준 구현용).
     *
     * @param a first factor / 첫 인수
     * @param b second factor / 둘째 인수
     * @return the product / 곱
     */
    private static int gmul(int a, int b) {
        int p = 0;
        // 러시아 농부 곱셈.
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            a = ((a << 1) ^ ((a & 0x80) != 0 ? 0x11b : 0)) & 0xff;
            b >>= 1;
        }
        return p;
    }

    /**
     * The S-box by its definition in FIPS 197 section 5.1.1.
     * <p>
     * FIPS 197 5.1.1 절의 정의대로 구한 S-box.
     *
     * @param x the input byte / 입력 바이트
     * @return the output byte / 출력 바이트
     */
    private static int referenceSbox(int x) {
        int inv = 0;
        // 역원을 전수 탐색으로 찾는다(0 의 역원은 0 으로 둔다).
        for (int c = 1; c < 256 && x != 0; c++) {
            if (gmul(x, c) == 1) {
                inv = c;
                break;
            }
        }
        int s = 0;
        // 아핀 변환: b_i ^ b_{i+4} ^ b_{i+5} ^ b_{i+6} ^ b_{i+7} ^ c_i, c = 0x63.
        for (int i = 0; i < 8; i++) {
            int bit = ((inv >> i) ^ (inv >> ((i + 4) % 8)) ^ (inv >> ((i + 5) % 8)) ^ (inv >> ((i + 6) % 8)) ^ (inv >> ((i + 7) % 8)) ^ (0x63 >> i)) & 1;
            s |= bit << i;
        }
        return s;
    }

    /**
     * The S-box circuit equals the definition for all 256 inputs, at every bit position of the planes.
     * <p>
     * S-box 회로가 256가지 입력 모두에서, 평면의 모든 비트 위치에서 정의와 같다.
     */
    public void testSboxCircuitMatchesTheDefinition() {
        Check.equal(0x63, referenceSbox(0), "S(00)");
        Check.equal(0xed, referenceSbox(0x53), "S(53), the example of FIPS 197");
        // 평면의 64개 위치에 서로 다른 값을 넣어 네 번에 256가지를 모두 본다.
        for (int base = 0; base < 256; base += 64) {
            long[] q = new long[8];
            // 위치 p 의 값은 base + p 이다.
            for (int p = 0; p < 64; p++) {
                // 값의 비트를 평면에 나눠 넣는다.
                for (int b = 0; b < 8; b++) {
                    q[b] |= (long) (((base + p) >> b) & 1) << p;
                }
            }
            Aes.sbox(q);
            // 위치마다 결과를 모아 정의와 비교한다.
            for (int p = 0; p < 64; p++) {
                int v = 0;
                // 평면에서 비트를 모은다.
                for (int b = 0; b < 8; b++) {
                    v |= (int) ((q[b] >>> p) & 1L) << b;
                }
                Check.equal(referenceSbox(base + p), v, "S-box of " + Integer.toHexString(base + p));
            }
        }
    }

    /**
     * Encrypts one block and returns the hexadecimal result.
     * <p>
     * 블록 하나를 암호화해 16진수 결과를 반환한다.
     *
     * @param key   the key in hexadecimal / 16진수 키
     * @param plain the block in hexadecimal / 16진수 블록
     * @return the ciphertext block / 암호문 블록
     */
    private static String encrypt(String key, String plain) {
        Aes aes = new Aes(Hex.decode(key));
        byte[] out = new byte[16];
        aes.encryptBlocks(Hex.decode(plain), 0, out, 0, 1);
        return Hex.encode(out);
    }

    /**
     * The block cipher gives the ciphertexts of FIPS 197 appendix B and appendix C.1 to C.3.
     * <p>
     * 블록 암호가 FIPS 197 부록 B 와 부록 C.1~C.3 의 암호문을 낸다.
     */
    public void testAesFips197() {
        Check.equal("3925841d02dc09fbdc118597196a0b32", encrypt("2b7e151628aed2a6abf7158809cf4f3c", "3243f6a8885a308d313198a2e0370734"), "appendix B");
        String plain = "00112233445566778899aabbccddeeff";
        Check.equal("69c4e0d86a7b0430d8cdb78070b4c55a", encrypt("000102030405060708090a0b0c0d0e0f", plain), "C.1 AES-128");
        Check.equal("dda97ca4864cdfe06eaf70a0ec0d7191", encrypt("000102030405060708090a0b0c0d0e0f1011121314151617", plain), "C.2 AES-192");
        Check.equal("8ea2b7ca516745bfeafc49904b496089", encrypt("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", plain), "C.3 AES-256");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() {
                new Aes(new byte[15]);
            }
        }, "a key of 15 bytes");
    }

    /**
     * Several blocks in one call give the same result as single blocks, for every count from 1 to 9 and in place.
     * <p>
     * 한 번에 여러 블록을 암호화한 결과가 블록 하나씩 한 결과와 같다(1~9 블록, 제자리 암호화 포함).
     */
    public void testBlocksInParallel() {
        Aes aes = new Aes(Hex.decode("2b7e151628aed2a6abf7158809cf4f3c"));
        byte[] in = new byte[9 * 16 + 3];
        // 입력을 서로 다른 값으로 채운다.
        for (int i = 0; i < in.length; i++) {
            in[i] = (byte) (i * 7 + 1);
        }
        byte[] single = new byte[in.length];
        // 블록 하나씩 암호화한 기준 결과.
        for (int b = 0; b < 9; b++) {
            aes.encryptBlocks(in, 3 + 16 * b, single, 3 + 16 * b, 1);
        }
        // 블록 수마다 한 번에 암호화한 결과와 비교한다.
        for (int n = 1; n <= 9; n++) {
            byte[] out = new byte[in.length];
            aes.encryptBlocks(in, 3, out, 3, n);
            Check.equal(Hex.encode(Bytes.copy(single, 3, 16 * n)), Hex.encode(Bytes.copy(out, 3, 16 * n)), n + " blocks at once");
        }
        byte[] inPlace = in.clone();
        aes.encryptBlocks(inPlace, 3, inPlace, 3, 9);
        Check.equal(Hex.encode(Bytes.copy(single, 3, 144)), Hex.encode(Bytes.copy(inPlace, 3, 144)), "in place");
    }

    /**
     * Seals and returns ciphertext and tag in hexadecimal, and checks that opening gives the plaintext back.
     * <p>
     * 암호화해 암호문과 태그를 16진수로 반환하고, 복호화하면 평문이 돌아오는지 확인한다.
     *
     * @param key   the key / 키
     * @param nonce the nonce / nonce
     * @param aad   the associated data / 연관 데이터
     * @param plain the plaintext / 평문
     * @return ciphertext and tag / 암호문과 태그
     */
    private static String seal(String key, String nonce, String aad, String plain) {
        Aead gcm = new AesGcm(Hex.decode(key));
        byte[] p = Hex.decode(plain);
        byte[] out = new byte[p.length + 16];
        gcm.seal(Hex.decode(nonce), Hex.decode(aad), p, 0, p.length, out, 0);
        byte[] back = new byte[p.length];
        Check.isTrue(gcm.open(Hex.decode(nonce), Hex.decode(aad), out, 0, out.length, back, 0), "the sealed data opens");
        Check.equal(Hex.encode(p), Hex.encode(back), "opening gives the plaintext");
        return Hex.encode(out);
    }

    /**
     * GCM gives the ciphertexts and tags of test cases 1 to 4 (AES-128) and 13 to 16 (AES-256) of the GCM specification.
     * <p>
     * GCM 이 GCM 규격의 시험 사례 1~4(AES-128)와 13~16(AES-256)의 암호문과 태그를 낸다.
     */
    public void testGcmSpecificationVectors() {
        String zeroKey = "00000000000000000000000000000000";
        String zeroNonce = "000000000000000000000000";
        String key = "feffe9928665731c6d6a8f9467308308";
        String nonce = "cafebabefacedbaddecaf888";
        String aad = "feedfacedeadbeeffeedfacedeadbeefabaddad2";
        String cipher = "42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091473f5985";
        Check.equal("58e2fccefa7e3061367f1d57a4e7455a", seal(zeroKey, zeroNonce, "", ""), "test case 1");
        Check.equal("0388dace60b6a392f328c2b971b2fe78" + "ab6e47d42cec13bdf53a67b21257bddf", seal(zeroKey, zeroNonce, "", zeroKey), "test case 2");
        Check.equal(cipher + "4d5c2af327cd64a62cf35abd2ba6fab4", seal(key, nonce, "", PLAIN), "test case 3");
        Check.equal(cipher.substring(0, 120) + "5bc94fbc3221a5db94fae95ae7121a47", seal(key, nonce, aad, PLAIN.substring(0, 120)), "test case 4");

        String cipher256 = "522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662898015ad";
        Check.equal("530f8afbc74536b9a963b4f1c4cb738b", seal(zeroKey + zeroKey, zeroNonce, "", ""), "test case 13");
        Check.equal("cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919", seal(zeroKey + zeroKey, zeroNonce, "", zeroKey), "test case 14");
        Check.equal(cipher256 + "b094dac5d93471bdec1a502270e3cc6c", seal(key + key, nonce, "", PLAIN), "test case 15");
        Check.equal(cipher256.substring(0, 120) + "76fc6ece0f4e1768cddf8853bb2d551b", seal(key + key, nonce, aad, PLAIN.substring(0, 120)), "test case 16");
    }

    /**
     * Multiplies two blocks in GF(2^128) with the bit by bit algorithm of NIST SP 800-38D section 6.3 (reference for GHASH).
     * <p>
     * NIST SP 800-38D 6.3 절의 비트 단위 알고리즘으로 GF(2^128) 에서 두 블록을 곱한다(GHASH 기준 구현용).
     *
     * @param x first block / 첫 블록
     * @param h second block / 둘째 블록
     * @return the product / 곱
     */
    private static byte[] referenceMultiply(byte[] x, byte[] h) {
        long z1 = 0, z0 = 0;
        long v1 = Bytes.be64(h, 0), v0 = Bytes.be64(h, 8);
        // X 의 비트를 최상위부터 보며 Z ^= V, V 는 오른쪽으로 옮기고 넘치면 R 을 더한다.
        for (int i = 0; i < 128; i++) {
            if (((x[i / 8] >> (7 - (i % 8))) & 1) != 0) {
                z1 ^= v1;
                z0 ^= v0;
            }
            boolean carry = (v0 & 1L) != 0;
            v0 = (v0 >>> 1) | (v1 << 63);
            v1 >>>= 1;
            if (carry) {
                v1 ^= 0xe100000000000000L;
            }
        }
        byte[] out = new byte[16];
        Bytes.putBe64(z1, out, 0);
        Bytes.putBe64(z0, out, 8);
        return out;
    }

    /**
     * The multiplication of GHASH equals the bit by bit algorithm of NIST SP 800-38D section 6.3 for pseudo random operands and edge
     * values: a hash of one block X with a zero length block is (X*H)*H.
     * <p>
     * GHASH 의 곱셈이 의사 난수 피연산자와 경계 값에 대해 NIST SP 800-38D 6.3 절의 비트 단위 알고리즘과 같다: 블록 X 하나와 0 인 길이 블록의 해시는
     * (X*H)*H 이다.
     */
    public void testGhashMultiplicationAgainstReference() {
        java.util.Random rnd = new java.util.Random(20261128L);
        // 무작위 X, H 쌍 200개와 경계 값을 본다.
        for (int n = 0; n < 204; n++) {
            byte[] h = new byte[16];
            byte[] x = new byte[16];
            rnd.nextBytes(h);
            rnd.nextBytes(x);
            if (n == 200) {
                java.util.Arrays.fill(h, (byte) 0xff);
                java.util.Arrays.fill(x, (byte) 0xff);
            } else if (n == 201) {
                java.util.Arrays.fill(h, (byte) 0);
            } else if (n == 202) {
                java.util.Arrays.fill(x, (byte) 0);
                x[15] = 1;
            } else if (n == 203) {
                java.util.Arrays.fill(h, (byte) 0);
                h[0] = (byte) 0x80;
            }
            Ghash g = new Ghash(h);
            g.update(x, 0, 16);
            byte[] expected = referenceMultiply(referenceMultiply(x, h), h);
            Check.equal(Hex.encode(expected), Hex.encode(g.finish(0, 0)), "(X*H)*H against the reference, case " + n);
            // 덜 찬 블록은 0 으로 채운 블록과 같고, finish 뒤에는 상태가 초기화된다.
            g.update(x, 0, 5);
            byte[] padded = new byte[16];
            System.arraycopy(x, 0, padded, 0, 5);
            Check.equal(Hex.encode(referenceMultiply(referenceMultiply(padded, h), h)), Hex.encode(g.finish(0, 0)), "partial block, case " + n);
        }
    }
    /**
     * A change of any single bit of the ciphertext, the tag or the associated data is rejected, and nothing is written to the output.
     * <p>
     * 암호문, 태그, 연관 데이터의 어느 한 비트를 바꿔도 거부하며 출력에 아무것도 쓰지 않는다.
     */
    public void testForgeriesAreRejected() {
        byte[] key = Hex.decode("feffe9928665731c6d6a8f9467308308");
        byte[] nonce = Hex.decode("cafebabefacedbaddecaf888");
        byte[] aad = Hex.decode("feedfacedeadbeeffeedfacedeadbeefabaddad2");
        byte[] plain = Hex.decode(PLAIN.substring(0, 120));
        Aead gcm = new AesGcm(key);
        byte[] sealed = new byte[plain.length + 16];
        gcm.seal(nonce, aad, plain, 0, plain.length, sealed, 0);
        // 암호문과 태그의 모든 비트를 하나씩 뒤집어 본다.
        for (int bit = 0; bit < sealed.length * 8; bit++) {
            byte[] forged = sealed.clone();
            forged[bit / 8] ^= (byte) (1 << (bit % 8));
            byte[] out = new byte[plain.length];
            Check.isFalse(gcm.open(nonce, aad, forged, 0, forged.length, out, 0), "flipped bit " + bit + " of ciphertext or tag");
            Check.equal(Hex.encode(new byte[plain.length]), Hex.encode(out), "no plaintext is written for a forgery");
        }
        // 연관 데이터의 모든 비트를 하나씩 뒤집어 본다.
        for (int bit = 0; bit < aad.length * 8; bit++) {
            byte[] other = aad.clone();
            other[bit / 8] ^= (byte) (1 << (bit % 8));
            Check.isFalse(gcm.open(nonce, other, sealed, 0, sealed.length, new byte[plain.length], 0), "flipped bit " + bit + " of the associated data");
        }
        byte[] otherNonce = nonce.clone();
        otherNonce[11] ^= 1;
        Check.isFalse(gcm.open(otherNonce, aad, sealed, 0, sealed.length, new byte[plain.length], 0), "another nonce");
        Check.isFalse(gcm.open(nonce, aad, sealed, 0, sealed.length - 1, new byte[plain.length], 0), "truncated by one byte");
        Check.isFalse(gcm.open(nonce, aad, sealed, 0, 15, new byte[plain.length], 0), "shorter than a tag");
        Check.isTrue(gcm.open(nonce, aad, sealed, 0, sealed.length, new byte[plain.length], 0), "the original still opens");
        gcm.destroy();
    }
}
