package org.duckdns.hjow.swingwebpanel.media.audio.opus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Conformance tests of the Opus decoder with the test vectors of RFC 6716 appendix A.4 (opus-codec.org, SHA1 checked against the
 * RFC): every packet of a vector carries the final range coder state of the encoder, which the decoder has to reproduce exactly
 * (it proves that every entropy-coded symbol of SILK and CELT was decoded correctly), and the decoded PCM has to match the
 * output of the reference decoder (<code>.dec</code> file) within the RFC 6716 quality metric of <code>opus_compare</code> and
 * within a stricter bound that we measure ourselves (maximum sample difference, signal to error ratio). The committed resources
 * hold, per vector, a prefix of the packets and a window of the reference output; the complete vectors are checked when the
 * directory with the original files is given in the environment variable <code>SWP_OPUS_VECTORS</code> (or the system property
 * <code>swingwebpanel.opus.vectors</code>). The vectors are decoded as stereo at 48 kHz, as <code>opus_demo -d 48000 2</code> does.
 * <p>
 * RFC 6716 부록 A.4 의 시험 벡터(opus-codec.org, SHA1 은 RFC 와 대조함)로 하는 Opus 디코더 적합성 시험: 벡터의 모든 패킷은 인코더의 최종
 * 범위 디코더 상태를 담고 있으며 디코더가 이를 정확히 재현해야 한다(SILK 와 CELT 의 모든 엔트로피 부호화 기호를 올바르게 디코딩했다는 증거).
 * 디코딩한 PCM 은 참조 디코더의 출력(<code>.dec</code> 파일)과 <code>opus_compare</code> 의 RFC 6716 품질 지표 안에서, 그리고 우리가 직접 재는 더
 * 엄격한 한계(최대 샘플 차이, 신호 대 오차비) 안에서 일치해야 한다. 저장소의 자료는 벡터마다 패킷 앞부분과 참조 출력의 일부 구간을 담고 있으며,
 * 원본 파일이 있는 디렉터리를 환경 변수 <code>SWP_OPUS_VECTORS</code>(또는 시스템 속성 <code>swingwebpanel.opus.vectors</code>)로 주면 전체 벡터를
 * 검사한다. 벡터는 <code>opus_demo -d 48000 2</code> 처럼 48 kHz 스테레오로 디코딩한다.
 */
public class OpusVectorTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OpusVectorTest() {
    }

    /**
     * Reads a classpath resource completely.
     * <p>
     * 클래스패스 자원을 모두 읽는다.
     *
     * @param name resource path / 자원 경로
     * @return the bytes / 바이트
     * @throws IOException if missing / 없으면
     */
    private static byte[] resource(String name) throws IOException {
        InputStream in = OpusVectorTest.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("missing test resource " + name);
        }
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }

    /**
     * Reads a stream completely.
     * <p>
     * 스트림을 모두 읽는다.
     *
     * @param in the stream / 스트림
     * @return the bytes / 바이트
     * @throws IOException on failure / 실패 시
     */
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[65536];
        int r;
        // 끝까지 읽는다.
        while ((r = in.read(b)) > 0) {
            o.write(b, 0, r);
        }
        return o.toByteArray();
    }

    /**
     * Converts little-endian bytes to 16-bit samples.
     * <p>
     * 리틀 엔디언 바이트를 16비트 샘플로 바꾼다.
     *
     * @param b the bytes / 바이트
     * @return the samples / 샘플
     */
    private static short[] samples(byte[] b) {
        short[] s = new short[b.length / 2];
        // 샘플마다 두 바이트를 합친다.
        for (int i = 0; i < s.length; i++) {
            s[i] = (short) ((b[2 * i] & 255) | (b[2 * i + 1] << 8));
        }
        return s;
    }

    /**
     * Converts a decoded float sample to 16 bits as the reference program does (scale, clip, round to even).
     * <p>
     * 참조 프로그램처럼 디코딩된 float 샘플을 16비트로 바꾼다(크기 조정, 클리핑, 짝수로 반올림).
     *
     * @param x the sample / 샘플
     * @return the 16-bit value / 16비트 값
     */
    static short toPcm16(float x) {
        float v = x * 32768.f;
        if (v > 32767.f) {
            v = 32767.f;
        }
        if (v < -32768.f) {
            v = -32768.f;
        }
        return (short) Math.rint(v);
    }

    /**
     * Result of decoding a test vector.
     * <p>
     * 시험 벡터 디코딩 결과.
     */
    private static final class Decoded {

        /**
         * Decoded stereo samples (interleaved).
         * <p>
         * 디코딩한 스테레오 샘플(인터리브).
         */
        short[] pcm;

        /**
         * Number of packets decoded.
         * <p>
         * 디코딩한 패킷 수.
         */
        int packets;

        /**
         * Number of packets whose final range differed from the encoder's.
         * <p>
         * 최종 범위가 인코더와 달랐던 패킷 수.
         */
        int rangeMismatches;

        /**
         * Number of packets that failed to decode.
         * <p>
         * 디코딩에 실패한 패킷 수.
         */
        int errors;
    }

    /**
     * Decodes the packets of an opus_demo style bit stream file as 48 kHz stereo.
     * <p>
     * opus_demo 방식 비트 스트림 파일의 패킷을 48 kHz 스테레오로 디코딩한다.
     *
     * @param d     the file / 파일
     * @param limit maximum number of output samples per channel to keep (the rest is decoded but dropped), or -1 for all / 보관할 채널당 최대 출력 샘플 수(나머지는 디코딩만 하고 버림). 전체면 -1
     * @param legacy true for the hybrid folding of RFC 6716, false for RFC 8251 / 하이브리드 접기가 RFC 6716 이면 true, RFC 8251 이면 false
     * @return the result / 결과
     */
    private static Decoded decodeAll(byte[] d, long limit, boolean legacy) {
        OpusFrameDecoder dec = new OpusFrameDecoder(2);
        dec.setLegacyFolding(legacy);
        Decoded r = new Decoded();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        float[] pcm = new float[OpusPacket.MAX_PACKET_SAMPLES * 2];
        long total = 0;
        int pos = 0;
        // 패킷마다 4 바이트 길이, 4 바이트 최종 범위, 페이로드 순으로 읽는다.
        while (pos + 8 <= d.length) {
            int len = ((d[pos] & 255) << 24) | ((d[pos + 1] & 255) << 16) | ((d[pos + 2] & 255) << 8) | (d[pos + 3] & 255);
            int rng = ((d[pos + 4] & 255) << 24) | ((d[pos + 5] & 255) << 16) | ((d[pos + 6] & 255) << 8) | (d[pos + 7] & 255);
            pos += 8;
            int n = dec.decodePacket(d, pos, len, pcm, 0, OpusPacket.MAX_PACKET_SAMPLES);
            if (n < 0) {
                r.errors++;
                n = 0;
            }
            if (rng != 0 && dec.getFinalRange() != rng) {
                r.rangeMismatches++;
            }
            // 보관 한도 안의 샘플만 16비트로 바꿔 모은다.
            for (int i = 0; i < n * 2 && (limit < 0 || total * 2 + i < limit * 2); i++) {
                short s = toPcm16(pcm[i]);
                out.write(s);
                out.write(s >> 8);
            }
            total += n;
            r.packets++;
            pos += len;
        }
        r.pcm = samples(out.toByteArray());
        return r;
    }

    /**
     * Checks one committed excerpt of a vector.
     * <p>
     * 저장소에 있는 벡터 발췌본 하나를 검사한다.
     *
     * @param n vector number, 1..12 / 벡터 번호(1..12)
     * @throws Exception on failure / 실패 시
     */
    private void checkExcerpt(int n) throws Exception {
        checkExcerpt(n, true, ".dec.gz");
    }

    /**
     * Checks one committed excerpt of a vector against a reference output.
     * <p>
     * 벡터 발췌본 하나를 참조 출력과 대조해 검사한다.
     *
     * @param n      vector number, 1..12 / 벡터 번호(1..12)
     * @param legacy true to decode with the hybrid folding of RFC 6716 (the vectors of RFC 6716), false for RFC 8251 / RFC 6716 의 하이브리드 접기(RFC 6716 의 벡터)면 true, RFC 8251 이면 false
     * @param suffix suffix of the reference file / 참조 파일의 접미사
     * @throws Exception on failure / 실패 시
     */
    private void checkExcerpt(int n, boolean legacy, String suffix) throws Exception {
        String id = String.format("testvector%02d", Integer.valueOf(n));
        Properties p = new Properties();
        p.load(new java.io.ByteArrayInputStream(resource("/media/opus/vectors/vectors.properties")));
        int packets = Integer.parseInt(p.getProperty(id + ".packets"));
        int ws = Integer.parseInt(p.getProperty(id + ".windowStart"));
        int wl = Integer.parseInt(p.getProperty(id + ".windowLength"));
        byte[] bit = resource("/media/opus/vectors/" + id + ".bit");
        GZIPInputStream gz = new GZIPInputStream(new java.io.ByteArrayInputStream(resource("/media/opus/vectors/" + id + suffix)));
        short[] ref = samples(readAll(gz));
        Check.equal(wl * 2, ref.length, id + " reference window size");
        Decoded r = decodeAll(bit, ws + wl, legacy);
        Check.equal(packets, r.packets, id + " packet count");
        Check.equal(0, r.errors, id + " packets that failed to decode");
        Check.equal(0, r.rangeMismatches, id + " packets whose final range differs from the encoder's");
        short[] test = new short[ref.length];
        System.arraycopy(r.pcm, ws * 2, test, 0, test.length);
        compare(id, ref, test);
    }

    /**
     * Compares decoded samples with the reference output.
     * <p>
     * 디코딩한 샘플을 참조 출력과 비교한다.
     *
     * @param id   name of the vector / 벡터 이름
     * @param ref  reference samples / 기준 샘플
     * @param test decoded samples / 디코딩한 샘플
     */
    private static void compare(String id, short[] ref, short[] test) {
        int maxDiff = 0;
        double err = 0;
        double sig = 0;
        // 샘플마다 차이와 에너지를 모은다.
        for (int i = 0; i < ref.length; i++) {
            int df = test[i] - ref[i];
            maxDiff = Math.max(maxDiff, Math.abs(df));
            err += (double) df * df;
            sig += (double) ref[i] * ref[i];
        }
        Check.isTrue(maxDiff <= 2, id + ": largest sample difference to the reference decoder is " + maxDiff + " (allowed 2)");
        double snr = err == 0 ? Double.POSITIVE_INFINITY : 10 * Math.log10(sig / err);
        Check.isTrue(snr >= 80, id + ": signal to error ratio " + snr + " dB (required 80)");
        double q = OpusCompare.quality(ref, test);
        Check.isTrue(q >= 99, id + ": opus_compare quality " + q + " % (the RFC passes at 0, we require 99)");
    }

    /**
     * Vector 1: CELT only, stereo, all frame sizes, transient frames.
     * <p>
     * 벡터 1: CELT 전용, 스테레오, 모든 프레임 크기, 과도 프레임.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector01() throws Exception {
        checkExcerpt(1);
    }

    /**
     * Vector 2: SILK only, mono and stereo.
     * <p>
     * 벡터 2: SILK 전용, 모노와 스테레오.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector02() throws Exception {
        checkExcerpt(2);
    }

    /**
     * Vector 3: SILK only, mono and stereo.
     * <p>
     * 벡터 3: SILK 전용, 모노와 스테레오.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector03() throws Exception {
        checkExcerpt(3);
    }

    /**
     * Vector 4: SILK only, mono and stereo.
     * <p>
     * 벡터 4: SILK 전용, 모노와 스테레오.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector04() throws Exception {
        checkExcerpt(4);
    }

    /**
     * Vector 5: hybrid mode, super-wideband.
     * <p>
     * 벡터 5: 하이브리드 모드, 초광대역.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector05() throws Exception {
        checkExcerpt(5);
    }

    /**
     * Vector 6: hybrid mode, fullband.
     * <p>
     * 벡터 6: 하이브리드 모드, 전대역.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector06() throws Exception {
        checkExcerpt(6);
    }

    /**
     * Vector 7: CELT only, mono and stereo.
     * <p>
     * 벡터 7: CELT 전용, 모노와 스테레오.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector07() throws Exception {
        checkExcerpt(7);
    }

    /**
     * Vector 8: mixed modes with a switch from SILK to CELT (cross-fade with the redundancy frame).
     * <p>
     * 벡터 8: 혼합 모드, SILK 에서 CELT 로 전환(중복 프레임과의 교차 페이드).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector08() throws Exception {
        checkExcerpt(8);
    }

    /**
     * Vector 9: mixed modes with a switch from SILK to CELT.
     * <p>
     * 벡터 9: 혼합 모드, SILK 에서 CELT 로 전환.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector09() throws Exception {
        checkExcerpt(9);
    }

    /**
     * Vector 10: switches between CELT and hybrid frames (the window contains both directions).
     * <p>
     * 벡터 10: CELT 와 하이브리드 프레임 사이의 전환(구간이 양방향 전환을 포함).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector10() throws Exception {
        checkExcerpt(10);
    }

    /**
     * Vector 11: CELT only with large packets and all frame packing codes.
     * <p>
     * 벡터 11: 큰 패킷과 모든 프레임 묶음 코드를 쓰는 CELT 전용.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector11() throws Exception {
        checkExcerpt(11);
    }

    /**
     * Vector 12: switch between SILK and hybrid frames.
     * <p>
     * 벡터 12: SILK 와 하이브리드 프레임 사이의 전환.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVector12() throws Exception {
        checkExcerpt(12);
    }

    /**
     * Vector 5 of RFC 8251 (hybrid folding of the second band when the first one is too narrow).
     * <p>
     * RFC 8251 의 벡터 5(첫 대역이 너무 좁을 때 둘째 대역의 하이브리드 접기).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRfc8251Vector05() throws Exception {
        checkExcerpt(5, false, ".8251.dec.gz");
    }

    /**
     * Vector 6 of RFC 8251 (hybrid folding).
     * <p>
     * RFC 8251 의 벡터 6(하이브리드 접기).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRfc8251Vector06() throws Exception {
        checkExcerpt(6, false, ".8251.dec.gz");
    }

    /**
     * Vector 12 of RFC 8251 (hybrid folding with switches between SILK and hybrid frames).
     * <p>
     * RFC 8251 의 벡터 12(SILK 와 하이브리드 프레임 사이의 전환이 있는 하이브리드 접기).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRfc8251Vector12() throws Exception {
        checkExcerpt(12, false, ".8251.dec.gz");
    }

    /**
     * The decoding with the folding of RFC 6716 differs from the one of RFC 8251 on the hybrid vectors, and each matches its own reference
     * (so the two behaviors are really different and the switch works).
     * <p>
     * RFC 6716 의 접기로 디코딩한 결과는 하이브리드 벡터에서 RFC 8251 의 접기와 다르며 각각 자기 참조와 일치한다(두 동작이 실제로 다르고 전환 장치가 동작한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFoldingSwitchChangesHybridOutput() throws Exception {
        byte[] bit = resource("/media/opus/vectors/testvector05.bit");
        short[] legacy = decodeAll(bit, 36000, true).pcm;
        short[] modern = decodeAll(bit, 36000, false).pcm;
        int different = 0;
        // 두 출력이 다른 샘플을 센다.
        for (int i = 0; i < Math.min(legacy.length, modern.length); i++) {
            if (legacy[i] != modern[i]) {
                different++;
            }
        }
        Check.isTrue(different > 1000, "the two foldings give different output on a hybrid vector (" + different + " samples differ)");
    }

    /**
     * Decodes and checks a range of the complete vectors from the directory that a system property or an environment variable names:
     * every packet's final range, the sample count, the output against the whole reference file and the <code>opus_compare</code>
     * quality. Skips the case (with a reason) when the directory is not given.
     * <p>
     * 시스템 속성이나 환경 변수가 가리키는 디렉터리의 전체 벡터 중 일부 범위를 디코딩해 검사한다: 모든 패킷의 최종 범위, 샘플 수, 참조 파일 전체와의
     * 출력 비교, <code>opus_compare</code> 품질. 디렉터리를 주지 않으면 이유와 함께 건너뛴다.
     *
     * @param property the system property naming the directory / 디렉터리를 가리키는 시스템 속성
     * @param env      the environment variable naming the directory / 디렉터리를 가리키는 환경 변수
     * @param legacy   true for the RFC 6716 folding of the old vectors, false for RFC 8251 / 옛 벡터의 RFC 6716 접기이면 true, RFC 8251 이면 false
     * @param first    first vector number / 첫 벡터 번호
     * @param last     last vector number / 마지막 벡터 번호
     * @throws Exception on failure / 실패 시
     */
    private static void completeVectors(String property, String env, boolean legacy, int first, int last) throws Exception {
        String dir = System.getProperty(property);
        if (dir == null) {
            dir = System.getenv(env);
        }
        if (dir == null || !new File(dir).isDirectory()) {
            Check.skip("complete test vectors not available; set " + env + " to the directory with testvector01..12 .bit and .dec");
        }
        // 지정한 범위의 벡터를 모두 디코딩해 비교한다.
        for (int n = first; n <= last; n++) {
            String id = String.format("testvector%02d", Integer.valueOf(n));
            byte[] bit = readFile(new File(dir, id + ".bit"));
            short[] ref = samples(readFile(new File(dir, id + ".dec")));
            Decoded r = decodeAll(bit, -1, legacy);
            Check.equal(0, r.errors, id + " decode errors");
            Check.equal(0, r.rangeMismatches, id + " final range mismatches");
            Check.equal(ref.length, r.pcm.length, id + " sample count");
            compare(id, ref, r.pcm);
        }
    }

    /**
     * The complete vectors 1 to 2 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 1~2: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors01To02IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 1, 2);
    }

    /**
     * The complete vectors 3 to 4 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 3~4: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors03To04IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 3, 4);
    }

    /**
     * The complete vectors 5 to 6 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 5~6: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors05To06IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 5, 6);
    }

    /**
     * The complete vectors 7 to 8 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 7~8: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors07To08IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 7, 8);
    }

    /**
     * The complete vectors 9 to 10 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 9~10: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors09To10IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 9, 10);
    }

    /**
     * The complete vectors 11 to 12 of RFC 6716, when the original files are available (directory in <code>SWP_OPUS_VECTORS</code>): the decoder
     * with the folding of RFC 6716 has to reproduce the reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS</code> 의 디렉터리) RFC 6716 의 전체 벡터 11~12: 디코더가 RFC 6716 의 접기로 참조 출력을 재현해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteVectors11To12IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors", "SWP_OPUS_VECTORS", true, 11, 12);
    }

    /**
     * The complete vectors 1 to 2 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 1~2: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors01To02IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 1, 2);
    }

    /**
     * The complete vectors 3 to 4 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 3~4: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors03To04IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 3, 4);
    }

    /**
     * The complete vectors 5 to 6 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 5~6: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors05To06IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 5, 6);
    }

    /**
     * The complete vectors 7 to 8 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 7~8: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors07To08IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 7, 8);
    }

    /**
     * The complete vectors 9 to 10 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 9~10: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors09To10IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 9, 10);
    }

    /**
     * The complete vectors 11 to 12 of RFC 8251, when the original files are available (directory in <code>SWP_OPUS_VECTORS_8251</code>): the decoder
     * with the folding of RFC 8251 has to match the new reference output. Split into small groups so that each case stays within the runner's time limit.
     * <p>
     * 원본 파일이 있을 때(<code>SWP_OPUS_VECTORS_8251</code> 의 디렉터리) RFC 8251 의 전체 벡터 11~12: 디코더가 RFC 8251 의 접기로 새 참조 출력과 일치해야 한다. 사례마다 실행기의 제한 시간 안에 끝나도록
     * 작은 그룹으로 나눈다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompleteRfc8251Vectors11To12IfAvailable() throws Exception {
        completeVectors("swingwebpanel.opus.vectors8251", "SWP_OPUS_VECTORS_8251", false, 11, 12);
    }

    /**
     * Reads a file completely.
     * <p>
     * 파일을 모두 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws IOException on failure / 실패 시
     */
    private static byte[] readFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }
}
