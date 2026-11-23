package org.duckdns.hjow.swingwebpanel.media.font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Brotli decoder (RFC 7932, plan item P10-08-e) against streams that the reference Brotli encoder made: the streams in
 * <code>tests/resources/org/duckdns/hjow/swingwebpanel/media/font/brotli</code> were produced once by <code>generation/make-brotli-vectors.js</code> with the encoder that ships with
 * Node.js (<code>zlib.brotliCompressSync</code>, Node.js v26.13.0, 2026-11-18) at several qualities, window sizes and modes (generic, text, font), so they
 * cover empty, tiny, text (static dictionary words with the case transformations), random (uncompressed meta-blocks), highly repetitive, 16 bit signed samples,
 * UTF-8 (context modes), long input (several meta-blocks) and a long binary input made of records (no real font: the fonts of the JRE may be used with Java only and
 * are not kept in the repository). Every decoded stream must equal its input byte for byte. The decoder must
 * also reject damaged streams with a {@link BrotliException} (never with another exception) and respect the limit of the output.
 * <p>
 * Brotli 디코더(RFC 7932, 계획 P10-08-e)를 참조 Brotli 인코더가 만든 스트림으로 확인하는 시험: <code>tests/resources/org/duckdns/hjow/swingwebpanel/media/font/brotli</code> 의 스트림은
 * <code>generation/make-brotli-vectors.js</code> 가 Node.js 에 들어 있는 인코더(<code>zlib.brotliCompressSync</code>, Node.js v26.13.0, 2026-11-18)로 여러 품질, 윈도 크기, 모드(일반,
 * 텍스트, 글꼴)에서 한 번 만든 것이라 빈 입력, 아주 짧은 입력, 텍스트(대소문자 변환이 있는 정적 사전 단어), 무작위(압축하지 않은 메타 블록), 반복이 많은 입력, 16 비트 부호 있는
 * 표본, UTF-8(문맥 모드), 긴 입력(여러 메타 블록), 기록으로 이루어진 긴 이진 입력을 덮는다(실제 글꼴은 없다: JRE 의 글꼴은 Java 에서만 쓰도록 허락된 것이어서 저장소에 두지 않는다). 풀린 결과는 입력과 바이트 단위로 같아야 한다. 손상된 스트림은 다른 예외가 아니라
 * {@link BrotliException} 으로 거부해야 하고 출력 한도를 지켜야 한다.
 */
public class BrotliTest {

    /** The directory of the vectors on the class path. 클래스패스의 시험 자료 디렉터리. */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/media/font/brotli/";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrotliTest() {
    }

    /**
     * Reads a resource of the vectors.
     * <p>
     * 시험 자료의 리소스를 읽는다.
     *
     * @param name the file name / 파일 이름
     * @return the bytes / 바이트
     * @throws IOException if it cannot be read / 읽을 수 없으면
     */
    static byte[] resource(String name) throws IOException {
        InputStream in = BrotliTest.class.getResourceAsStream(DIR + name);
        Check.notNull(in, "the resource " + name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 리소스를 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Decodes every stream of the index and compares the result with the input.
     * <p>
     * 색인의 모든 스트림을 풀어 결과를 입력과 비교한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryReferenceStreamDecodesToItsInput() throws Exception {
        String[] lines = new String(resource("index.txt"), "UTF-8").trim().split("\n");
        Check.isTrue(lines.length >= 20, "the index lists the vectors: " + lines.length);
        // 색인의 줄마다 입력과 압축 스트림을 읽어 풀고 비교한다.
        for (int i = 0; i < lines.length; i++) {
            String[] f = lines[i].trim().split(" ");
            byte[] raw = resource(f[1]);
            byte[] br = resource(f[2]);
            byte[] decoded = Brotli.decode(br, raw.length + 16);
            Check.isTrue(Arrays.equals(raw, decoded), f[2] + " (mode " + f[3] + ", quality " + f[4] + ", window " + f[5] + ") decodes to its input: " + decoded.length
                    + " bytes instead of " + raw.length);
        }
    }

    /**
     * The streams cover the format: together they use the static dictionary with many different transformations, uncompressed and compressed meta-blocks, several
     * block types and literal trees, and the context modes that the reference encoder chooses (UTF8 for text and Signed for sample data).
     * <p>
     * 스트림이 형식을 덮는다: 모두 합쳐 여러 가지 변환을 쓴 정적 사전, 압축하지 않은 메타 블록과 압축 메타 블록, 여러 블록 종류와 리터럴 트리, 참조 인코더가 고르는 문맥 모드(텍스트는
     * UTF8, 표본 자료는 Signed)를 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTheStreamsCoverTheFormat() throws Exception {
        String[] lines = new String(resource("index.txt"), "UTF-8").trim().split("\n");
        int[] total = new int[8];
        // 모든 스트림의 통계를 합친다(마스크와 최대값은 합치지 않고 합집합과 최대로 모은다).
        for (int i = 0; i < lines.length; i++) {
            String[] f = lines[i].trim().split(" ");
            byte[] br = resource(f[2]);
            int[] stats = new int[8];
            Brotli.decode(br, 0, br.length, 10 * 1024 * 1024, stats);
            total[0] += stats[0];
            total[1] += stats[1];
            total[2] += stats[2];
            total[3] |= stats[3];
            total[4] = Math.max(total[4], stats[4]);
            total[5] = Math.max(total[5], stats[5]);
            total[6] += stats[6];
            total[7] = Math.max(total[7], stats[7]);
        }
        System.err.println("# Brotli coverage: dictionary references " + total[0] + ", compressed blocks " + total[1] + ", uncompressed blocks " + total[2] + ", context mode mask "
                + total[3] + ", most block types " + total[4] + ", most literal trees " + total[5] + ", blocks with NPOSTFIX/NDIRECT " + total[6] + ", most transformations in one stream "
                + total[7]);
        Check.isTrue(total[0] > 200, "many references to the static dictionary: " + total[0]);
        Check.isTrue(total[7] >= 8, "different word transformations in one stream: " + total[7]);
        Check.isTrue(total[1] >= 20 && total[2] >= 1, "compressed and uncompressed meta-blocks");
        Check.isTrue((total[3] & 4) != 0, "the UTF8 context mode");
        Check.isTrue((total[3] & 8) != 0, "the Signed context mode");
        Check.isTrue(total[4] >= 2, "block switching: " + total[4]);
        Check.isTrue(total[5] >= 2, "several literal prefix trees (context maps): " + total[5]);
    }

    /**
     * A long binary input (150 000 bytes of records and words, compressed in the font mode at two qualities) decodes to the same bytes. No real font is used: the fonts of
     * the JRE (Lucida of Bigelow &amp; Holmes) may be used with Java only, so no Brotli stream made from them is kept in the repository, and a test cannot compress the font of the
     * JRE that runs it (Node.js, which made the streams, is not a dependency of the tests); the real fonts go through the WOFF2 decoder at test time with uncompressed Brotli blocks
     * instead (<code>Woff2Test</code>), and the compressed table data of a WOFF2 file is tested with a synthetic font (<code>Woff2Test</code>).
     * <p>
     * 긴 이진 입력(기록과 낱말 150,000 바이트를 글꼴 모드로 두 품질에서 압축한 것)이 같은 바이트로 풀린다. 실제 글꼴은 쓰지 않는다: JRE 의 글꼴(Bigelow &amp; Holmes 의 Lucida)은 Java 에서만
     * 쓰도록 허락된 것이므로 그것으로 만든 Brotli 스트림을 저장소에 두지 않고, 시험은 실행하는 JRE 의 글꼴을 압축할 수 없다(스트림을 만든 Node.js 는 시험의 의존성이 아니다). 실제 글꼴은 시험 때
     * 압축하지 않은 Brotli 블록으로 WOFF2 디코더를 거치고(<code>Woff2Test</code>), WOFF2 파일의 압축된 테이블 데이터는 합성 글꼴로 시험한다(<code>Woff2Test</code>).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLargeBinaryInput() throws Exception {
        byte[] raw = resource("structured.raw");
        Check.equal(150000, raw.length, "the length of the input");
        String[] files = { "structured-2-q11-w22.br", "structured-2-q5-w16.br" };
        // 두 품질의 스트림을 각각 풀어 입력과 비교한다.
        for (int i = 0; i < files.length; i++) {
            byte[] br = resource(files[i]);
            int[] stats = new int[8];
            byte[] decoded = Brotli.decode(br, 0, br.length, raw.length, stats);
            Check.isTrue(Arrays.equals(raw, decoded), files[i] + " decodes to its input");
            Check.isTrue(br.length < raw.length / 2, files[i] + " is compressed: " + br.length);
            Check.isTrue(stats[1] >= 1, files[i] + " has compressed meta-blocks: " + stats[1]);
        }
    }

    /**
     * A stream is cut at every length that is shorter than it: the decoder rejects it with a BrotliException (never reads outside, never loops).
     * <p>
     * 스트림을 자기보다 짧은 모든 길이에서 자르면 디코더는 BrotliException 으로 거부한다(밖을 읽지 않고 되풀이하지도 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedStreamsAreRejected() throws Exception {
        byte[] br = resource("english-1-q11-w22.br");
        byte[] raw = resource("english.raw");
        // 스트림을 마지막 바이트 직전까지 한 바이트씩 잘라 본다(마지막 바이트에는 닫는 비트가 있으므로 잘라도 앞 부분은 풀리지 않는다).
        for (int cut = 0; cut < br.length; cut++) {
            try {
                byte[] r = Brotli.decode(br, 0, cut, raw.length + 16);
                Check.fail("a stream cut to " + cut + " bytes was accepted (" + r.length + " bytes)");
            } catch (BrotliException e) {
                // 기대한 거부이다.
            }
        }
    }

    /**
     * Single bits of a stream are flipped one after the other: the decoder either decodes (a bit may be harmless or give other valid data) or rejects with a
     * BrotliException; any other exception would be a bug. The output limit holds.
     * <p>
     * 스트림의 비트를 하나씩 뒤집는다: 디코더는 풀거나(비트가 무해하거나 다른 올바른 데이터를 줄 수 있다) BrotliException 으로 거부한다. 다른 예외는 버그이다. 출력 한도는 지켜진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFlippedBitsNeverCrashTheDecoder() throws Exception {
        String[] names = { "english-1-q11-w22.br", "samples-2-q9-w18.br", "utf8-1-q7-w16.br", "random-0-q1-w16.br", "english-0-q1-w14.br" };
        int limit = 20000;
        // 스트림마다 모든 비트를 한 번씩 뒤집어 풀어 본다.
        for (int s = 0; s < names.length; s++) {
            byte[] br = resource(names[s]);
            int bitsToTry = Math.min(br.length * 8, 6000);
            // 앞쪽 비트부터 뒤집는다(머리와 코드 설명이 앞쪽에 있다).
            for (int bit = 0; bit < bitsToTry; bit++) {
                byte[] bad = br.clone();
                bad[bit >> 3] ^= 1 << (bit & 7);
                try {
                    byte[] r = Brotli.decode(bad, limit);
                    Check.isTrue(r.length <= limit, "the output limit is kept");
                } catch (BrotliException e) {
                    // 기대한 거부이다.
                }
            }
        }
    }

    /**
     * The limit of the output is kept: a stream whose result is larger is rejected.
     * <p>
     * 출력 한도를 지킨다: 결과가 더 큰 스트림은 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOutputLimit() throws Exception {
        byte[] br = resource("repeat-0-q11-w22.br");
        Check.equal(30000, Brotli.decode(br, 30000).length, "exactly at the limit");
        try {
            Brotli.decode(br, 29999);
            Check.fail("a result larger than the limit was accepted");
        } catch (BrotliException e) {
            Check.contains(e.getMessage(), "larger", "the reason");
        }
    }

    /**
     * Empty and garbage input.
     * <p>
     * 빈 입력과 쓰레기 입력.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEmptyAndGarbageInput() throws Exception {
        try {
            Brotli.decode(new byte[0], 100);
            Check.fail("an empty stream has no header");
        } catch (BrotliException e) {
            // 기대한 거부이다.
        }
        byte[] garbage = new byte[64];
        // 쓰레기 바이트로 채운다(시험마다 같은 값).
        for (int i = 0; i < garbage.length; i++) {
            garbage[i] = (byte) (i * 37 + 11);
        }
        try {
            byte[] r = Brotli.decode(garbage, 100000);
            Check.isTrue(r.length <= 100000, "the output limit is kept for garbage");
        } catch (BrotliException e) {
            // 기대한 거부이다.
        }
    }
}
