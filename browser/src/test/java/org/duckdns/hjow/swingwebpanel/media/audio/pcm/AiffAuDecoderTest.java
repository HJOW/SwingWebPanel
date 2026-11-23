package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the AIFF/AIFF-C and AU decoders (plan items P18-02, P18-07; FEATURE_MATRIX AUD-02, AUD-03).
 * <p>
 * AIFF/AIFF-C 와 AU 디코더 시험(계획 P18-02, P18-07, FEATURE_MATRIX AUD-02, AUD-03).
 */
public class AiffAuDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AiffAuDecoderTest() {
    }

    /**
     * Makes deterministic random bytes.
     * <p>
     * 결정적인 난수 바이트를 만든다.
     *
     * @param n    count / 개수
     * @param seed seed / 시드
     * @return bytes / 바이트
     */
    private static byte[] random(int n, long seed) {
        byte[] b = new byte[n];
        new Random(seed).nextBytes(b);
        return b;
    }

    /**
     * Swaps the byte order of 16-bit samples.
     * <p>
     * 16비트 샘플의 바이트 순서를 뒤집는다.
     *
     * @param b bytes / 바이트
     * @return swapped copy / 뒤집은 복사본
     */
    private static byte[] swap16(byte[] b) {
        byte[] r = new byte[b.length];
        // 두 바이트씩 순서를 바꾼다.
        for (int i = 0; i + 1 < b.length; i += 2) {
            r[i] = b[i + 1];
            r[i + 1] = b[i];
        }
        return r;
    }

    /**
     * AIFF 16-bit big-endian PCM is converted exactly; the 80-bit sample rate is read for several rates.
     * <p>
     * AIFF 16비트 빅 엔디언 PCM 은 정확히 변환되고 80비트 샘플링 주파수는 여러 값에서 올바르게 읽힌다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAiffPcm16AndRates() throws Exception {
        byte[] be = random(4 * 500, 11);
        int[] rates = { 8000, 11025, 22050, 44100, 48000, 96000, 192000, 1, 3 };
        // 샘플링 주파수마다 파일을 만들어 읽은 값이 같은지 확인한다.
        for (int rate : rates) {
            PcmSource s = AudioDecoders.open(AudioKit.aiff(null, 2, 16, rate, 500, be), "audio/wav", null);
            Check.equal(rate, s.getSampleRate(), "sample rate " + rate);
            Check.equal(500, s.getFrameCount(), "frames");
            Check.equal(true, java.util.Arrays.equals(swap16(be), AudioKit.readAll(s)), "exact PCM at " + rate);
        }
    }

    /**
     * AIFF with 8, 24 and 32 bits and an odd sample size such as 12 bits (stored in two bytes, left-justified).
     * <p>
     * 8, 24, 32비트 AIFF 와 12비트처럼 홀수 크기(2바이트에 왼쪽 정렬로 저장)의 AIFF.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAiffOtherSizes() throws Exception {
        byte[] b8 = random(200, 12);
        short[] g8 = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff(null, 1, 8, 8000, 200, b8), null, null)));
        // 8비트 AIFF 는 부호 있는 값이다.
        for (int i = 0; i < 200; i++) {
            Check.equal(b8[i] << 8, g8[i], "8-bit sample " + i);
        }
        byte[] b24 = random(3 * 100, 13);
        short[] g24 = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff(null, 1, 24, 8000, 100, b24), null, null)));
        // 24비트 빅 엔디언 샘플을 256 으로 나눠 반올림한 값과 비교한다.
        for (int i = 0; i < 100; i++) {
            int s = ((b24[3 * i] << 16) | ((b24[3 * i + 1] & 0xFF) << 8) | (b24[3 * i + 2] & 0xFF));
            Check.equal(Math.min(32767, (s + 128) >> 8), g24[i], "24-bit sample " + i);
        }
        byte[] b12 = random(2 * 100, 14);
        short[] g12 = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff(null, 1, 12, 8000, 100, b12), null, null)));
        // 12비트는 두 바이트 컨테이너로 읽는다.
        for (int i = 0; i < 100; i++) {
            Check.equal((short) (((b12[2 * i] & 0xFF) << 8) | (b12[2 * i + 1] & 0xFF)), g12[i], "12-bit sample " + i);
        }
    }

    /**
     * AIFF-C compression types: sowt, twos, raw, fl32, fl64, ulaw, alaw, in24 and an unsupported one.
     * <p>
     * AIFF-C 압축 유형: sowt, twos, raw, fl32, fl64, ulaw, alaw, in24, 그리고 지원하지 않는 유형.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAifcCompressions() throws Exception {
        byte[] le = random(2 * 100, 15);
        Check.equal(true, java.util.Arrays.equals(le, AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("sowt", 1, 16, 8000, 100, le), null, null))),
                "sowt is little-endian PCM");
        Check.equal(true, java.util.Arrays.equals(swap16(le), AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("twos", 1, 16, 8000, 100, le), null, null))),
                "twos is big-endian PCM");
        byte[] raw = { (byte) 0x80, (byte) 0xFF, 0x00, 0x40 };
        short[] g = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("raw ", 1, 8, 8000, 4, raw), null, null)));
        Check.equal(0, g[0], "raw is unsigned: 0x80 is silence");
        Check.equal(127 << 8, g[1], "0xFF");
        Check.equal(-128 << 8, g[2], "0x00");
        Check.equal(-64 << 8, g[3], "0x40");
        Out f32 = new Out();
        f32.be32(Float.floatToIntBits(0.5f) & 0xFFFFFFFFL).be32(Float.floatToIntBits(-1.0f) & 0xFFFFFFFFL);
        short[] gf = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("fl32", 1, 32, 8000, 2, f32.toByteArray()), null, null)));
        Check.equal(16384, gf[0], "fl32 0.5");
        Check.equal(-32768, gf[1], "fl32 -1.0");
        Out f64 = new Out();
        long l = Double.doubleToLongBits(0.25);
        f64.be32(l >>> 32).be32(l & 0xFFFFFFFFL);
        Check.equal(8192, AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("fl64", 1, 64, 8000, 1, f64.toByteArray()), null, null)))[0],
                "fl64 0.25");
        byte[] companded = new byte[256];
        // 모든 바이트 값을 압신 데이터로 쓴다.
        for (int i = 0; i < 256; i++) {
            companded[i] = (byte) i;
        }
        short[] gu = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("ulaw", 1, 16, 8000, 256, companded), null, null)));
        short[] ga = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("alaw", 1, 16, 8000, 256, companded), null, null)));
        // 바이트마다 G.711 표와 같은지 확인한다.
        for (int i = 0; i < 256; i++) {
            Check.equal(PcmConverter.ulaw(i), gu[i], "ulaw " + i);
            Check.equal(PcmConverter.alaw(i), ga[i], "alaw " + i);
        }
        byte[] in24 = random(300, 16);
        Check.equal(100, AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("in24", 1, 24, 8000, 100, in24), null, null))).length,
                "in24 frames");
        // ima4(QuickTime IMA ADPCM)는 64프레임마다 34바이트 패킷이다(실제 인코더 파일과의 비교는 ReferenceDecodeTest). 모르는 압축은 거부한다.
        Check.equal(4 * 64, AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.aiff("ima4", 1, 16, 8000, 100, new byte[34 * 4]), null, null))).length,
                "ima4 frames");
        try {
            AudioDecoders.open(AudioKit.aiff("GSM ", 1, 16, 8000, 100, new byte[68 * 2]), null, null);
            Check.fail("an unknown compression must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "unsupported compression");
        }
    }

    /**
     * An AIFF whose common chunk declares more frames than the file holds (truncated) ends early; one that declares fewer plays only those.
     * <p>
     * COMM 청크가 파일에 있는 것보다 많은 프레임을 선언한 AIFF(잘린 파일)는 일찍 끝나고, 더 적게 선언했으면 그만큼만 재생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAiffFrameCounts() throws Exception {
        byte[] pcm = random(2 * 50, 17);
        PcmSource more = AudioDecoders.open(AudioKit.aiff(null, 1, 16, 8000, 1000, pcm), null, null);
        Check.equal(50, more.getFrameCount(), "only the frames that exist");
        PcmSource fewer = AudioDecoders.open(AudioKit.aiff(null, 1, 16, 8000, 20, pcm), null, null);
        Check.equal(20, fewer.getFrameCount(), "only the declared frames");
        Check.equal(40, AudioKit.readAll(fewer).length, "bytes of the declared frames");
        PcmSource streaming = AudioDecoders.open(AudioKit.aiff(null, 1, 16, 8000, 0, pcm), null, null);
        Check.equal(50, streaming.getFrameCount(), "zero frames declared means all of the data");
    }

    /**
     * Invalid AIFF files fail with CORRUPT or UNSUPPORTED_FORMAT and never with a runtime exception, for every truncation length.
     * <p>
     * 올바르지 않은 AIFF 파일은 모든 잘린 길이에서 CORRUPT 또는 UNSUPPORTED_FORMAT 으로 실패하며 런타임 예외로 실패하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAiffInvalid() throws Exception {
        byte[] good = AudioKit.aiff("sowt", 2, 16, 44100, 10, new byte[40]);
        // 모든 길이로 잘라 열어 본다.
        for (int cut = 0; cut < good.length; cut++) {
            byte[] c = new byte[cut];
            System.arraycopy(good, 0, c, 0, cut);
            try {
                AudioKit.readAll(AudioDecoders.open(c, null, null));
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "media exception at " + cut);
            }
        }
        byte[] zeroRate = AudioKit.aiff(null, 1, 16, 1, 1, new byte[2]);
        zeroRate[12 + 8 + 8] = 0;
        zeroRate[12 + 8 + 9] = 0;
        try {
            AudioDecoders.open(zeroRate, null, null);
            Check.fail("an invalid sample rate must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "invalid extended number");
        }
    }

    /**
     * AU encodings: mu-law, A-law, PCM 8/16/24/32, float and double.
     * <p>
     * AU 인코딩: mu-law, A-law, PCM 8/16/24/32, float, double.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAuEncodings() throws Exception {
        byte[] be = random(4 * 100, 18);
        PcmSource s16 = AudioDecoders.open(AudioKit.au(3, 2, 8000, be, 0xFFFFFFFFL), null, null);
        Check.equal(100, s16.getFrameCount(), "frames with an unknown size");
        Check.equal(true, java.util.Arrays.equals(swap16(be), AudioKit.readAll(s16)), "PCM16 big-endian");
        byte[] b8 = random(100, 19);
        short[] g8 = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(2, 1, 8000, b8, 100), null, null)));
        // 8비트 AU 는 부호 있는 값이다.
        for (int i = 0; i < 100; i++) {
            Check.equal(b8[i] << 8, g8[i], "pcm8 " + i);
        }
        byte[] b32 = random(4 * 50, 20);
        short[] g32 = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(5, 1, 8000, b32, 200), null, null)));
        // 32비트 샘플은 상위 16비트를 반올림한 값과 1 이내로 같다.
        for (int i = 0; i < 50; i++) {
            int s = ((b32[4 * i] << 24) | ((b32[4 * i + 1] & 0xFF) << 16) | ((b32[4 * i + 2] & 0xFF) << 8) | (b32[4 * i + 3] & 0xFF));
            long expected = Math.min(32767, ((long) s + 32768) >> 16);
            Check.isTrue(Math.abs(expected - g32[i]) <= 1, "pcm32 " + i);
        }
        byte[] all = new byte[256];
        // 모든 바이트를 압신 데이터로 쓴다.
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        short[] gu = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(1, 1, 8000, all, 256), null, null)));
        short[] ga = AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(27, 1, 8000, all, 256), null, null)));
        // 바이트마다 G.711 표와 같은지 확인한다.
        for (int i = 0; i < 256; i++) {
            Check.equal(PcmConverter.ulaw(i), gu[i], "au ulaw " + i);
            Check.equal(PcmConverter.alaw(i), ga[i], "au alaw " + i);
        }
        Out fl = new Out();
        fl.be32(Float.floatToIntBits(0.5f) & 0xFFFFFFFFL);
        Check.equal(16384, AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(6, 1, 8000, fl.toByteArray(), 4), null, null)))[0], "float");
        Out db = new Out();
        long l = Double.doubleToLongBits(-0.5);
        db.be32(l >>> 32).be32(l & 0xFFFFFFFFL);
        Check.equal(-16384, AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(AudioKit.au(7, 1, 8000, db.toByteArray(), 8), null, null)))[0], "double");
    }

    /**
     * AU headers: an offset beyond the file, tiny headers, unsupported encodings and invalid values.
     * <p>
     * AU 헤더: 파일을 넘는 오프셋, 작은 헤더, 지원하지 않는 인코딩, 올바르지 않은 값.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAuInvalid() throws Exception {
        byte[] ok = AudioKit.au(3, 1, 8000, new byte[10], 10);
        // 모든 길이로 잘라 열어 본다.
        for (int cut = 0; cut < ok.length; cut++) {
            byte[] c = new byte[cut];
            System.arraycopy(ok, 0, c, 0, cut);
            try {
                AudioKit.readAll(AudioDecoders.open(c, null, null));
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "media exception at " + cut);
            }
        }
        byte[] adpcm = AudioKit.au(23, 1, 8000, new byte[10], 10);
        try {
            AudioDecoders.open(adpcm, null, null);
            Check.fail("G.721 must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "ADPCM encoding");
        }
        byte[] badOffset = AudioKit.au(3, 1, 8000, new byte[10], 10);
        badOffset[4] = 0x7F;
        try {
            AudioDecoders.open(badOffset, null, null);
            Check.fail("an offset beyond the file must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "offset beyond the file");
        }
    }
}
