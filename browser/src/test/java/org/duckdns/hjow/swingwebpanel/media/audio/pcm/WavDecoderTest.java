package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the WAV decoder (plan items P18-02, P18-07; FEATURE_MATRIX AUD-01): sample formats, extensible headers, chunk walking,
 * streaming and truncated files, invalid headers, exact seeking and the mix-down of surround layouts.
 * <p>
 * WAV 디코더 시험(계획 P18-02, P18-07, FEATURE_MATRIX AUD-01): 샘플 형식, 확장 헤더, 청크 순회, 스트리밍/잘린 파일, 잘못된 헤더, 정확한 탐색, 서라운드
 * 배치의 다운믹스.
 */
public class WavDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WavDecoderTest() {
    }

    /**
     * Opens through the registry with a deliberately different MIME hint.
     * <p>
     * 일부러 다른 MIME 힌트를 주고 등록부로 연다.
     *
     * @param file file bytes / 파일 바이트
     * @return source / 소스
     * @throws MediaException on failure / 실패 시
     */
    private static PcmSource open(byte[] file) throws MediaException {
        return AudioDecoders.open(file, "audio/mpeg", null);
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
     * 16-bit stereo PCM round trip is exact and the format values are reported.
     * <p>
     * 16비트 스테레오 PCM 왕복은 정확하고 형식 값이 보고된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPcm16Stereo() throws Exception {
        byte[] pcm = random(4 * 1000, 1);
        PcmSource s = open(AudioKit.wav(1, 2, 44100, 16, pcm, -1));
        Check.equal(44100, s.getSampleRate(), "sample rate");
        Check.equal(2, s.getChannels(), "channels");
        Check.equal(1000, s.getFrameCount(), "frame count");
        Check.isTrue(s.isSeekable(), "seekable");
        Check.equal(true, java.util.Arrays.equals(pcm, AudioKit.readAll(s)), "exact PCM");
        Check.equal(-1, s.read(new byte[4], 0, 4), "end of stream");
        s.close();
    }

    /**
     * 8-bit unsigned PCM becomes signed 16-bit, mono stays mono.
     * <p>
     * 8비트 부호 없는 PCM 은 부호 있는 16비트가 되고 모노는 모노로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPcm8Unsigned() throws Exception {
        byte[] pcm = random(500, 2);
        PcmSource s = open(AudioKit.wav(1, 1, 8000, 8, pcm, -1));
        Check.equal(1, s.getChannels(), "mono");
        short[] got = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(500, got.length, "frames");
        // 샘플마다 (값 - 128) << 8 과 같은지 확인한다.
        for (int i = 0; i < 500; i++) {
            Check.equal(((pcm[i] & 0xFF) - 128) << 8, got[i], "sample " + i);
        }
    }

    /**
     * 24-bit and 32-bit PCM are rounded to 16 bits.
     * <p>
     * 24비트와 32비트 PCM 은 16비트로 반올림된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPcm24And32() throws Exception {
        byte[] p24 = random(3 * 400, 3);
        short[] g24 = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(1, 1, 22050, 24, p24, -1))));
        // 24비트 샘플은 256 으로 나눠 반올림한 값과 같아야 한다.
        for (int i = 0; i < 400; i++) {
            int s = (p24[3 * i] & 0xFF) | ((p24[3 * i + 1] & 0xFF) << 8) | (p24[3 * i + 2] << 16);
            Check.equal(Math.min(32767, (s + 128) >> 8), g24[i], "24-bit sample " + i);
        }
        byte[] p32 = random(4 * 400, 4);
        short[] g32 = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(1, 1, 22050, 32, p32, -1))));
        // 32비트 샘플은 65536 으로 나눠 반올림한 값과 1 이내로 같아야 한다.
        for (int i = 0; i < 400; i++) {
            int s = (p32[4 * i] & 0xFF) | ((p32[4 * i + 1] & 0xFF) << 8) | ((p32[4 * i + 2] & 0xFF) << 16) | (p32[4 * i + 3] << 24);
            long expected = Math.min(32767, ((long) s + 32768) >> 16);
            Check.isTrue(Math.abs(expected - g32[i]) <= 1, "32-bit sample " + i + " expected " + expected + " got " + g32[i]);
        }
    }

    /**
     * IEEE float of 32 and 64 bits is scaled, clipped and NaN becomes silence.
     * <p>
     * 32/64비트 IEEE 부동소수점은 스케일되고 잘리며 NaN 은 무음이 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFloat() throws Exception {
        double[] v = { 0.0, 0.5, -0.5, 1.0, -1.0, 2.5, -3.0, Double.NaN, 0.25, -0.0001 };
        short[] expected = { 0, 16384, -16384, 32767, -32768, 32767, -32768, 0, 8192, -3 };
        Out o32 = new Out();
        Out o64 = new Out();
        // 값마다 32비트와 64비트 표현을 쓴다.
        for (double d : v) {
            o32.le32(Float.floatToIntBits((float) d) & 0xFFFFFFFFL);
            long l = Double.doubleToLongBits(d);
            o64.le32(l & 0xFFFFFFFFL).le32(l >>> 32);
        }
        short[] g32 = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(3, 1, 44100, 32, o32.toByteArray(), -1))));
        short[] g64 = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(3, 1, 44100, 64, o64.toByteArray(), -1))));
        // 32비트와 64비트 결과를 각각 기대값과 비교한다.
        for (int i = 0; i < v.length; i++) {
            Check.equal(expected[i], g32[i], "float32 " + v[i]);
            Check.equal(expected[i], g64[i], "float64 " + v[i]);
        }
        Check.equal(16384, PcmConverter.clip16(0.5), "double clip");
    }

    /**
     * Every A-law and mu-law byte decodes to the table value.
     * <p>
     * 모든 A-law 와 mu-law 바이트가 표의 값으로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCompanded() throws Exception {
        byte[] all = new byte[256];
        // 0..255 모든 바이트를 만든다.
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        short[] u = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(7, 1, 8000, 8, all, -1))));
        short[] a = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(6, 1, 8000, 8, all, -1))));
        // 바이트마다 변환기의 값과 같은지 확인한다.
        for (int i = 0; i < 256; i++) {
            Check.equal(PcmConverter.ulaw(i), u[i], "mu-law " + i);
            Check.equal(PcmConverter.alaw(i), a[i], "A-law " + i);
        }
    }

    /**
     * WAVE_FORMAT_EXTENSIBLE with PCM and float subformats and a speaker mask.
     * <p>
     * PCM 과 float 서브포맷, 스피커 마스크가 있는 WAVE_FORMAT_EXTENSIBLE.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExtensible() throws Exception {
        byte[] pcm = random(4 * 50, 5);
        PcmSource s = open(AudioKit.wav(1, 2, 48000, 16, pcm, 3));
        Check.equal(true, java.util.Arrays.equals(pcm, AudioKit.readAll(s)), "extensible PCM");
        Out f = new Out();
        f.le32(Float.floatToIntBits(0.5f) & 0xFFFFFFFFL);
        PcmSource sf = open(AudioKit.wav(3, 1, 48000, 32, f.toByteArray(), 4));
        Check.equal(16384, AudioKit.toShorts(AudioKit.readAll(sf))[0], "extensible float");
    }

    /**
     * Surround layouts are mixed down: a centre-only signal reaches both sides at -3 dB scaled by the common factor.
     * <p>
     * 서라운드 배치는 다운믹스된다: 중앙 채널만 있는 신호는 -3 dB 로 양쪽에 들어가며 공통 계수로 스케일된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSurroundMixdown() throws Exception {
        // 5.1 채널의 한 프레임: FL FR FC LFE BL BR 중 FC 만 10000.
        Out o = new Out();
        o.le16(0).le16(0).le16(10000).le16(0).le16(0).le16(0);
        PcmSource s = open(AudioKit.wav(1, 6, 48000, 16, o.toByteArray(), 0x3F));
        Check.equal(2, s.getChannels(), "mixed to stereo");
        short[] got = AudioKit.toShorts(AudioKit.readAll(s));
        double expected = 10000 * 0.70710678 / (1 + 2 * 0.70710678);
        Check.isTrue(Math.abs(got[0] - expected) <= 1.5 && Math.abs(got[1] - expected) <= 1.5, "centre in both sides: " + got[0] + "/" + got[1]);
        // 앞 왼쪽만 있는 신호는 왼쪽에만 나온다.
        Out l = new Out();
        l.le16(20000).le16(0).le16(0).le16(0).le16(0).le16(0);
        short[] left = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(1, 6, 48000, 16, l.toByteArray(), 0x3F))));
        Check.isTrue(left[0] > 8000 && left[1] == 0, "front left stays left: " + left[0] + "/" + left[1]);
        // 마스크가 없는 6채널은 기본 배치를 쓴다.
        short[] def = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(1, 6, 48000, 16, o.toByteArray(), -1))));
        Check.equal(got[0], def[0], "default layout equals the mask 0x3F");
        // 전체 스케일 입력이 출력에서 풀 스케일을 넘지 않는다.
        Out full = new Out();
        // 여섯 채널 모두 최대값으로 채운다.
        for (int c = 0; c < 6; c++) {
            full.le16(32767);
        }
        short[] fs = AudioKit.toShorts(AudioKit.readAll(open(AudioKit.wav(1, 6, 48000, 16, full.toByteArray(), -1))));
        Check.isTrue(fs[0] > 20000 && fs[0] <= 32767, "no clipping: " + fs[0]);
    }

    /**
     * Unknown chunks, odd-sized chunks with padding and chunks after the data are walked correctly.
     * <p>
     * 모르는 청크, 패딩이 있는 홀수 크기 청크, data 뒤의 청크를 올바르게 훑는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChunkWalking() throws Exception {
        byte[] pcm = random(40, 6);
        Out o = new Out();
        o.ascii("RIFF").le32(0).ascii("WAVE");
        o.ascii("LIST").le32(5).bytes(new byte[] { 1, 2, 3, 4, 5, 0 });
        o.ascii("fmt ").le32(16).le16(1).le16(2).le32(8000).le32(32000).le16(4).le16(16);
        o.ascii("fact").le32(4).le32(10);
        o.ascii("data").le32(40).bytes(pcm);
        o.ascii("junk").le32(3).bytes(new byte[] { 9, 9, 9, 0 });
        PcmSource s = open(o.toByteArray());
        Check.equal(10, s.getFrameCount(), "frames");
        Check.equal(true, java.util.Arrays.equals(pcm, AudioKit.readAll(s)), "data after odd chunks");
    }

    /**
     * Streaming headers (size 0 or 0xFFFFFFFF) and sizes larger than the file extend to the end of the file.
     * <p>
     * 스트리밍 헤더(크기 0 또는 0xFFFFFFFF)와 파일보다 큰 크기는 파일 끝까지로 본다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStreamingAndOversizedDataChunk() throws Exception {
        byte[] pcm = random(400, 7);
        Out fmt = new Out();
        fmt.le16(1).le16(2).le32(8000).le32(32000).le16(4).le16(16);
        long[] sizes = { 0, 0xFFFFFFFFL, 0x7FFFFFFFL, 401 };
        // 크기 값마다 같은 PCM 이 끝까지 나오는지 확인한다.
        for (long size : sizes) {
            PcmSource s = open(AudioKit.wavFromFmt(fmt.toByteArray(), pcm, size));
            Check.equal(100, s.getFrameCount(), "frames for size field " + size);
            Check.equal(true, java.util.Arrays.equals(pcm, AudioKit.readAll(s)), "PCM for size field " + size);
        }
    }

    /**
     * A file cut in the middle of a frame ends cleanly after the last complete frame.
     * <p>
     * 프레임 중간에서 잘린 파일은 마지막 완전한 프레임 뒤에서 깔끔하게 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncatedFile() throws Exception {
        byte[] pcm = random(4 * 100, 8);
        byte[] file = AudioKit.wav(1, 2, 8000, 16, pcm, -1);
        byte[] cut = new byte[file.length - 2 - 4 * 10];
        System.arraycopy(file, 0, cut, 0, cut.length);
        PcmSource s = open(cut);
        byte[] got = AudioKit.readAll(s);
        Check.equal(89 * 4, got.length, "89 complete frames remain");
        Check.equal(89, s.getFrameCount(), "frame count of the truncated file");
        byte[] expected = new byte[89 * 4];
        System.arraycopy(pcm, 0, expected, 0, expected.length);
        Check.equal(true, java.util.Arrays.equals(expected, got), "prefix is exact");
    }

    /**
     * Damaged and unsupported headers fail with the right kind and never with a runtime exception.
     * <p>
     * 손상되었거나 지원하지 않는 헤더는 올바른 종류로 실패하며 런타임 예외로 실패하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidHeaders() throws Exception {
        byte[] good = AudioKit.wav(1, 2, 8000, 16, new byte[8], -1);
        expectFailure(new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E' }, MediaException.Kind.CORRUPT, "no chunks");
        Out noData = new Out();
        noData.ascii("RIFF").le32(28).ascii("WAVE").ascii("fmt ").le32(16).le16(1).le16(2).le32(8000).le32(32000).le16(4).le16(16);
        expectFailure(noData.toByteArray(), MediaException.Kind.CORRUPT, "no data chunk");
        Out noFmt = new Out();
        noFmt.ascii("RIFF").le32(12).ascii("WAVE").ascii("data").le32(4).le32(0);
        expectFailure(noFmt.toByteArray(), MediaException.Kind.CORRUPT, "no format chunk");
        expectFailure(AudioKit.wav(0x55, 2, 8000, 16, new byte[8], -1), MediaException.Kind.UNSUPPORTED_FORMAT, "MP3 in WAV");
        expectFailure(AudioKit.wav(1, 0, 8000, 16, new byte[8], -1), MediaException.Kind.CORRUPT, "zero channels");
        expectFailure(AudioKit.wav(1, 2, 0, 16, new byte[8], -1), MediaException.Kind.CORRUPT, "zero sample rate");
        expectFailure(AudioKit.wav(3, 1, 8000, 16, new byte[8], -1), MediaException.Kind.UNSUPPORTED_FORMAT, "16-bit float");
        expectFailure(AudioKit.wav(1, 1, 8000, 40, new byte[8], -1), MediaException.Kind.UNSUPPORTED_FORMAT, "40-bit PCM");
        // 어떤 길이로 잘라도 런타임 예외가 나지 않아야 한다.
        for (int cut = 0; cut < good.length; cut++) {
            byte[] c = new byte[cut];
            System.arraycopy(good, 0, c, 0, cut);
            try {
                PcmSource s = AudioDecoders.open(c, null, null);
                AudioKit.readAll(s);
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "a media exception for the cut at " + cut);
            }
        }
    }

    /**
     * Opens a file that must fail and checks the kind.
     * <p>
     * 실패해야 하는 파일을 열어 종류를 확인한다.
     *
     * @param file file bytes / 파일 바이트
     * @param kind expected kind / 기대하는 종류
     * @param what description / 설명
     */
    private static void expectFailure(final byte[] file, MediaException.Kind kind, String what) {
        Throwable t = Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens the file.
             * <p>
             * 파일을 연다.
             *
             * @throws Exception on failure / 실패 시
             */
            public void run() throws Exception {
                AudioDecoders.open(file, null, null);
            }
        }, what);
        Check.equal(kind, ((MediaException) t).getKind(), what + ": kind");
    }

    /**
     * Seeking is exact and repeatable for every position including the end.
     * <p>
     * 탐색은 끝을 포함한 모든 위치에서 정확하고 되풀이할 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeek() throws Exception {
        byte[] pcm = random(4 * 3000, 9);
        PcmSource s = open(AudioKit.wav(1, 2, 44100, 16, pcm, -1));
        long[] frames = { 0, 1, 2047, 2048, 2049, 2999, 1500, 7, 3000, 5000 };
        // 위치마다 이동한 뒤 읽은 PCM 이 원본의 해당 부분과 같은지 확인한다.
        for (long f : frames) {
            s.seekToFrame(f);
            byte[] got = AudioKit.readAll(s);
            int from = (int) Math.min(f, 3000) * 4;
            byte[] expected = new byte[pcm.length - from];
            System.arraycopy(pcm, from, expected, 0, expected.length);
            Check.equal(true, java.util.Arrays.equals(expected, got), "after seeking to frame " + f);
        }
        s.seekToFrame(-5);
        Check.equal(true, java.util.Arrays.equals(pcm, AudioKit.readAll(s)), "negative position means the start");
    }

    /**
     * A source rejects reads after close, accepts short reads in whole frames and refuses buffers smaller than a frame.
     * <p>
     * 소스는 닫은 뒤의 읽기를 거부하고 프레임 단위로 짧게 읽는 것을 받아들이며 프레임보다 작은 버퍼를 거부한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReadContracts() throws Exception {
        byte[] pcm = random(4 * 10, 10);
        final PcmSource s = open(AudioKit.wav(1, 2, 8000, 16, pcm, -1));
        byte[] buf = new byte[7];
        int n = s.read(buf, 0, 7);
        Check.equal(4, n, "a 7-byte request returns one whole 4-byte frame");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Reads into a buffer smaller than one frame.
             * <p>
             * 한 프레임보다 작은 버퍼로 읽는다.
             *
             * @throws Exception on failure / 실패 시
             */
            public void run() throws Exception {
                s.read(new byte[3], 0, 3);
            }
        }, "buffer smaller than a frame");
        s.close();
        s.close();
        Throwable t = Check.throwsException(MediaException.class, new Block() {
            /**
             * Reads after close.
             * <p>
             * 닫은 뒤 읽는다.
             *
             * @throws Exception on failure / 실패 시
             */
            public void run() throws Exception {
                s.read(new byte[16], 0, 16);
            }
        }, "read after close");
        Check.equal(MediaException.Kind.ACCESS_FAILED, ((MediaException) t).getKind(), "kind after close");
    }

    /**
     * Huge claims in the header never lead to huge allocations: a 4 GiB data size and 1 million channels fail or are clamped quickly.
     * <p>
     * 헤더의 터무니없는 값은 큰 할당으로 이어지지 않는다: 4 GiB 데이터 크기와 백만 채널은 빠르게 실패하거나 잘린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHugeClaims() throws Exception {
        Out fmt = new Out();
        fmt.le16(1).le16(2).le32(8000).le32(32000).le16(4).le16(16);
        PcmSource s = open(AudioKit.wavFromFmt(fmt.toByteArray(), new byte[8], 0xFFFFFFF0L));
        Check.equal(2, s.getFrameCount(), "clamped to the available data");
        Out big = new Out();
        big.le16(1).le16(0xFFFF).le32(8000).le32(32000).le16(4).le16(16);
        expectFailure(AudioKit.wavFromFmt(big.toByteArray(), new byte[8], 8), MediaException.Kind.CORRUPT, "65535 channels");
        Out ima = new Out();
        ima.le16(0x11).le16(2).le32(8000).le32(8000).le16(0xFFFF).le16(4).le16(2).le16(0xFFFF);
        PcmSource tiny = open(AudioKit.wavFromFmt(ima.toByteArray(), new byte[16], 16));
        Check.equal(9, tiny.getFrameCount(), "a 65535-byte ADPCM block claim over 16 bytes of data decodes only what exists");
        Check.equal(9 * 4, AudioKit.readAll(tiny).length, "and delivers exactly that");
    }

    /**
     * The limit of the encoded size is enforced by the registry.
     * <p>
     * 인코딩 크기 제한은 등록부가 강제한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSizeLimit() throws Exception {
        final byte[] file = AudioKit.wav(1, 2, 8000, 16, new byte[4000], -1);
        Throwable t = Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens with a very small limit.
             * <p>
             * 아주 작은 제한으로 연다.
             *
             * @throws Exception on failure / 실패 시
             */
            public void run() throws Exception {
                AudioDecoders.open(file, null, new DecodeLimits(1000, 100, 10, 1000));
            }
        }, "over the byte limit");
        Check.equal(MediaException.Kind.TOO_LARGE, ((MediaException) t).getKind(), "kind");
    }
}
