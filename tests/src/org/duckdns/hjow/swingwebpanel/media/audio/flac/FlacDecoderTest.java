package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import java.util.Arrays;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.FlacWriter;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the FLAC decoder (plan item P18-07, FEATURE_MATRIX AUD-07) against an independent test encoder: all subframe types, stereo
 * modes, bit depths, channel counts, block sizes and header codes, wasted bits, Rice variants, seeking, Ogg FLAC, damaged and truncated data.
 * <p>
 * 독립적인 시험용 인코더를 상대로 한 FLAC 디코더 시험(계획 P18-07, FEATURE_MATRIX AUD-07): 모든 서브프레임 유형, 스테레오 방식, 비트 깊이, 채널 수,
 * 블록 크기와 헤더 코드, 버려진 비트, Rice 변형, 탐색, Ogg FLAC, 손상/잘린 데이터.
 */
public class FlacDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FlacDecoderTest() {
    }

    /**
     * Makes a deterministic test signal: two sines and noise within the range of the bit depth.
     * <p>
     * 결정적인 시험 신호를 만든다: 사인파 두 개와 잡음. 비트 깊이의 범위 안에 있다.
     *
     * @param ch   channels / 채널 수
     * @param n    samples per channel / 채널당 샘플 수
     * @param bps  bit depth / 비트 깊이
     * @param seed seed / 시드
     * @return samples [channel][index] / 샘플 [채널][위치]
     */
    static long[][] signal(int ch, int n, int bps, long seed) {
        Random r = new Random(seed);
        long[][] x = new long[ch][n];
        double amp = Math.pow(2, bps - 1) * 0.55;
        // 채널마다 주파수가 다른 사인파에 잡음을 더한다.
        for (int c = 0; c < ch; c++) {
            for (int i = 0; i < n; i++) {
                double v = amp * (0.6 * Math.sin(i * 0.031 * (c + 1)) + 0.3 * Math.sin(i * 0.173 + c) + 0.1 * (r.nextDouble() * 2 - 1));
                x[c][i] = Math.round(v);
            }
        }
        return x;
    }

    /**
     * The expected 16-bit interleaved output for one or two channels, computed independently of the decoder.
     * <p>
     * 디코더와 별개로 계산한 1~2채널의 기대 16비트 인터리브 출력.
     *
     * @param x   samples / 샘플
     * @param bps bit depth / 비트 깊이
     * @return expected PCM bytes / 기대하는 PCM 바이트
     */
    static byte[] expected(long[][] x, int bps) {
        int ch = x.length;
        int n = x[0].length;
        short[] s = new short[ch * n];
        // 샘플마다 16비트로 올리거나(작은 깊이) 반올림해 줄인다(큰 깊이).
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < ch; c++) {
                long v = x[c][i];
                long r;
                if (bps <= 16) {
                    r = v << (16 - bps);
                } else {
                    int shift = bps - 16;
                    r = (v + (1L << (shift - 1))) >> shift;
                }
                s[i * ch + c] = (short) Math.max(-32768, Math.min(32767, r));
            }
        }
        return AudioKit.toBytes(s);
    }

    /**
     * Decodes a file through the registry with a misleading MIME hint.
     * <p>
     * 잘못된 MIME 힌트를 주고 등록부로 파일을 디코딩한다.
     *
     * @param file file bytes / 파일 바이트
     * @return the PCM / PCM
     * @throws MediaException on failure / 실패 시
     */
    private static byte[] decode(byte[] file) throws MediaException {
        return AudioKit.readAll(AudioDecoders.open(file, "audio/wav", null));
    }

    /**
     * Every subframe strategy round-trips exactly (16-bit stereo, independent channels).
     * <p>
     * 모든 서브프레임 전략이 정확히 왕복한다(16비트 스테레오, 독립 채널).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSubframeTypes() throws Exception {
        long[][] x = signal(2, 5000, 16, 1);
        String[] strategies = { "verbatim", "fixed0", "fixed1", "fixed2", "fixed3", "fixed4", "lpc1", "lpc2", "lpc8", "lpc12", "lpc32", "auto" };
        // 전략마다 인코딩하고 디코딩한 결과가 입력과 같은지 확인한다.
        for (String s : strategies) {
            FlacWriter.Options o = new FlacWriter.Options();
            o.strategy = s;
            FlacWriter.Result r = FlacWriter.encode(x, o);
            Check.equal(true, Arrays.equals(expected(x, 16), decode(r.file)), "strategy " + s);
        }
        long[][] silence = new long[2][3000];
        FlacWriter.Options o = new FlacWriter.Options();
        o.strategy = "constant";
        Check.equal(true, Arrays.equals(expected(silence, 16), decode(FlacWriter.encode(silence, o).file)), "constant subframes");
        long[][] flat = { new long[1000], new long[1000] };
        Arrays.fill(flat[0], 1234);
        Arrays.fill(flat[1], -4321);
        Check.equal(true, Arrays.equals(expected(flat, 16), decode(FlacWriter.encode(flat, o).file)), "constant non-zero subframes");
    }

    /**
     * All stereo modes round-trip, including the 17-bit side channel of extreme signals.
     * <p>
     * 모든 스테레오 방식이 정확히 왕복하며 극단적인 신호의 17비트 side 채널도 포함한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStereoModes() throws Exception {
        long[][] x = signal(2, 4000, 16, 2);
        long[][] extreme = new long[2][600];
        // 한쪽 채널이 최대, 다른 쪽이 최소가 되는 신호를 만든다.
        for (int i = 0; i < 600; i++) {
            extreme[0][i] = (i % 2 == 0) ? 32767 : -32768;
            extreme[1][i] = (i % 2 == 0) ? -32768 : 32767;
        }
        // 모든 스테레오 방식과 두 신호를 시험한다.
        for (int mode = 0; mode < 4; mode++) {
            for (int sig = 0; sig < 2; sig++) {
                long[][] s = sig == 0 ? x : extreme;
                FlacWriter.Options o = new FlacWriter.Options();
                o.stereo = mode;
                o.strategy = "lpc4";
                o.blockSize = 777;
                Check.equal(true, Arrays.equals(expected(s, 16), decode(FlacWriter.encode(s, o).file)), "stereo mode " + mode + " signal " + sig);
            }
        }
    }

    /**
     * Bit depths from 4 to 32 are converted to 16 bits (shifted up or rounded) and decode without overflow.
     * <p>
     * 4~32비트 깊이가 16비트로 변환되며(올림 또는 반올림) 넘침 없이 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBitDepths() throws Exception {
        int[] depths = { 4, 8, 9, 12, 16, 17, 20, 24, 32 };
        // 비트 깊이마다 모노와 스테레오(mid/side 포함)를 시험한다.
        for (int bps : depths) {
            for (int ch = 1; ch <= 2; ch++) {
                long[][] x = signal(ch, 1500, bps, 3 + bps);
                FlacWriter.Options o = new FlacWriter.Options();
                o.bps = bps;
                o.stereo = ch == 2 ? 3 : 0;
                o.strategy = "lpc6";
                o.riceMethod1 = bps > 16;
                Check.equal(true, Arrays.equals(expected(x, bps), decode(FlacWriter.encode(x, o).file)), bps + " bits, " + ch + " channels");
            }
        }
        long[][] full = { { 2147483647L, -2147483648L, 0, 2147483647L, -2147483648L, 5, 2147483647L, -2147483648L } };
        FlacWriter.Options o = new FlacWriter.Options();
        o.bps = 32;
        o.strategy = "verbatim";
        short[] got = AudioKit.toShorts(decode(FlacWriter.encode(full, o).file));
        Check.equal(32767, got[0], "32-bit maximum clips to 16-bit maximum");
        Check.equal(-32768, got[1], "32-bit minimum");
    }

    /**
     * Channel counts 1 to 8 decode; more than two channels are mixed down like the mixer of the default layout.
     * <p>
     * 채널 수 1~8 이 디코딩되며 채널이 둘보다 많으면 기본 배치의 믹서와 같게 다운믹스된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChannelCounts() throws Exception {
        // 채널 수마다 인코딩해 디코딩한다.
        for (int ch = 1; ch <= 8; ch++) {
            long[][] x = signal(ch, 700, 16, 20 + ch);
            FlacWriter.Options o = new FlacWriter.Options();
            o.strategy = "fixed2";
            PcmSource s = AudioDecoders.open(FlacWriter.encode(x, o).file, null, null);
            Check.equal(Math.min(ch, 2), s.getChannels(), "output channels for " + ch);
            byte[] got = AudioKit.readAll(s);
            if (ch <= 2) {
                Check.equal(true, Arrays.equals(expected(x, 16), got), "exact for " + ch + " channels");
            } else {
                ChannelMixer m = ChannelMixer.forChannels(ch);
                short[] g = AudioKit.toShorts(got);
                float[] frame = new float[ch];
                float[] out = new float[2];
                // 프레임마다 같은 믹서로 계산한 기대값과 비교한다.
                for (int i = 0; i < 700; i++) {
                    for (int c = 0; c < ch; c++) {
                        frame[c] = (float) (x[c][i] * (1.0 / 32768.0));
                    }
                    m.mix(frame, out);
                    Check.equal(PcmConverter.clip16(out[0]), g[2 * i], "left of frame " + i + " with " + ch + " channels");
                    Check.equal(PcmConverter.clip16(out[1]), g[2 * i + 1], "right of frame " + i + " with " + ch + " channels");
                }
            }
        }
    }

    /**
     * Block size codes (standard, 8-bit, 16-bit), variable block sizes and sample rate / sample size header codes.
     * <p>
     * 블록 크기 코드(표준, 8비트, 16비트), 가변 블록 크기, 샘플링 주파수/샘플 크기 헤더 코드.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockSizesAndHeaderCodes() throws Exception {
        int[] sizes = { 192, 576, 1152, 2304, 4608, 256, 512, 1024, 2048, 4096, 8192, 16384, 100, 255, 256, 257, 5000, 65535 };
        // 블록 크기마다 고정 크기 스트림을 시험한다.
        for (int bs : sizes) {
            long[][] x = signal(2, Math.min(70000, bs * 2 + 37), 16, 40);
            FlacWriter.Options o = new FlacWriter.Options();
            o.blockSize = bs;
            o.strategy = bs > 5000 ? "fixed2" : "lpc3";
            Check.equal(true, Arrays.equals(expected(x, 16), decode(FlacWriter.encode(x, o).file)), "block size " + bs);
        }
        long[][] x = signal(1, 9000, 16, 41);
        FlacWriter.Options v = new FlacWriter.Options();
        v.sizes = new int[] { 4096, 1000, 333, 4608, 16, 2048, 700 };
        v.strategy = "fixed1";
        Check.equal(true, Arrays.equals(expected(x, 16), decode(FlacWriter.encode(x, v).file)), "variable block sizes");
        int[] rates = { 8000, 44100, 48000, 96000, 192000, 22050, 12000, 37800 };
        // 샘플링 주파수마다 헤더 코드를 달리 쓴다(STREAMINFO 에서 가져오는 경우 포함).
        for (int rate : rates) {
            for (int fromInfo = 0; fromInfo < 2; fromInfo++) {
                FlacWriter.Options o = new FlacWriter.Options();
                o.rate = rate;
                o.fromStreamInfo = fromInfo == 1;
                PcmSource s = AudioDecoders.open(FlacWriter.encode(x, o).file, null, null);
                Check.equal(rate, s.getSampleRate(), "rate " + rate);
                Check.equal(true, Arrays.equals(expected(x, 16), AudioKit.readAll(s)), "samples at " + rate);
            }
        }
    }

    /**
     * Wasted bits and the Rice variants: partition orders, the 5-bit parameter and escape codes.
     * <p>
     * 버려진 비트와 Rice 변형: 파티션 차수, 5비트 매개변수, 이스케이프 코드.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWastedBitsAndRice() throws Exception {
        long[][] w = signal(2, 3000, 16, 50);
        // 모든 샘플의 하위 3비트를 0 으로 만들어 버려진 비트가 생기게 한다.
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < w[c].length; i++) {
                w[c][i] &= ~7L;
            }
        }
        FlacWriter.Options o = new FlacWriter.Options();
        o.strategy = "lpc4";
        Check.equal(true, Arrays.equals(expected(w, 16), decode(FlacWriter.encode(w, o).file)), "wasted bits");
        long[][] x = signal(1, 4096, 24, 51);
        // 파티션 차수와 Rice 방식 조합을 모두 시험한다.
        for (int po = 0; po <= 8; po++) {
            for (int m = 0; m < 2; m++) {
                FlacWriter.Options r = new FlacWriter.Options();
                r.bps = 24;
                r.partitionOrder = po;
                r.riceMethod1 = m == 1;
                r.strategy = "fixed3";
                r.escape = po % 2 == 0;
                Check.equal(true, Arrays.equals(expected(x, 24), decode(FlacWriter.encode(x, r).file)), "partition order " + po + " method " + m);
            }
        }
        long[][] noise = new long[1][2000];
        Random rnd = new Random(52);
        // 잔차가 매우 커서 이스케이프 코드가 더 싼 신호를 만든다.
        for (int i = 0; i < 2000; i++) {
            noise[0][i] = i % 7 == 0 ? rnd.nextInt(60000) - 30000 : rnd.nextInt(3) - 1;
        }
        FlacWriter.Options e = new FlacWriter.Options();
        e.escape = true;
        e.strategy = "fixed1";
        e.partitionOrder = 4;
        Check.equal(true, Arrays.equals(expected(noise, 16), decode(FlacWriter.encode(noise, e).file)), "escape codes");
    }

    /**
     * Seeking is exact with a seek table, without one (index built while decoding) and for variable block sizes.
     * <p>
     * 탐색은 탐색 표가 있을 때, 없을 때(디코딩하며 색인을 만듦), 가변 블록 크기일 때 모두 정확하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeeking() throws Exception {
        long[][] x = signal(2, 30000, 16, 60);
        byte[] all = expected(x, 16);
        int[] tables = { 0, 3 };
        // 탐색 표 유무와 가변 블록 크기 조합을 시험한다.
        for (int table : tables) {
            for (int variable = 0; variable < 2; variable++) {
                FlacWriter.Options o = new FlacWriter.Options();
                o.blockSize = 1000;
                o.seekEvery = table;
                o.strategy = "lpc3";
                if (variable == 1) {
                    o.sizes = new int[] { 1000, 700, 1300, 512 };
                }
                PcmSource s = AudioDecoders.open(FlacWriter.encode(x, o).file, null, null);
                long[] targets = { 0, 1, 999, 1000, 1001, 12345, 29999, 30000, 40000, 5, 20000, 100 };
                // 앞뒤로 마구 이동해도 항상 정확한 위치에서 읽는지 확인한다.
                for (long t : targets) {
                    s.seekToFrame(t);
                    byte[] got = AudioKit.readAll(s);
                    int from = (int) Math.min(t, 30000) * 4;
                    byte[] exp = Arrays.copyOfRange(all, from, all.length);
                    Check.equal(true, Arrays.equals(exp, got), "table " + table + " variable " + variable + " seek to " + t);
                }
            }
        }
    }

    /**
     * An ID3v2 tag in front, an ID3v1 tag behind and unknown total samples are tolerated.
     * <p>
     * 앞의 ID3v2 태그, 뒤의 ID3v1 태그, 알 수 없는 전체 샘플 수를 받아들인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTagsAndUnknownLength() throws Exception {
        long[][] x = signal(2, 5000, 16, 70);
        FlacWriter.Options o = new FlacWriter.Options();
        o.claimedTotal = 0;
        byte[] flac = FlacWriter.encode(x, o).file;
        byte[] id3v2 = new Out().ascii("ID3").u8(3).u8(0).u8(0).u8(0).u8(0).u8(0).u8(20).bytes(new byte[20]).toByteArray();
        byte[] tag = new byte[128];
        tag[0] = 'T';
        tag[1] = 'A';
        tag[2] = 'G';
        byte[] file = OggMux.concat(id3v2, flac, tag);
        PcmSource s = AudioDecoders.open(file, null, null);
        Check.equal(-1, s.getFrameCount(), "unknown length");
        Check.equal(true, Arrays.equals(expected(x, 16), AudioKit.readAll(s)), "tags do not disturb the decoding");
        s.seekToFrame(4000);
        Check.equal((5000 - 4000) * 4, AudioKit.readAll(s).length, "seeking without a known length");
    }

    /**
     * A bad CRC, damaged subframes and lost sync fail with CORRUPT after the PCM before the damage; a cut file ends cleanly.
     * <p>
     * 틀린 CRC, 손상된 서브프레임, 잃어버린 동기는 손상 앞의 PCM 을 전달한 뒤 CORRUPT 로 실패하고, 잘린 파일은 깔끔하게 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedData() throws Exception {
        long[][] x = signal(2, 8000, 16, 80);
        FlacWriter.Options o = new FlacWriter.Options();
        o.blockSize = 1000;
        o.strategy = "lpc2";
        FlacWriter.Result r = FlacWriter.encode(x, o);
        byte[] all = expected(x, 16);
        int third = r.firstFrame + r.frames[0].length + r.frames[1].length;
        byte[] bad = r.file.clone();
        bad[third + r.frames[2].length / 2] ^= 0x10;
        PcmSource s = AudioDecoders.open(bad, null, null);
        byte[] buf = new byte[1 << 20];
        int got = 0;
        // 손상된 프레임 앞까지 읽고 그 다음 읽기에서 오류가 나는지 확인한다.
        try {
            for (;;) {
                int n = s.read(buf, got, buf.length - got);
                if (n < 0) {
                    break;
                }
                got += n;
            }
            Check.fail("the damaged frame must be reported");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "damaged frame");
        }
        Check.equal(2000 * 4, got, "the two intact frames are delivered first");
        Check.equal(true, Arrays.equals(Arrays.copyOf(all, got), Arrays.copyOf(buf, got)), "and they are exact");
        byte[] noHeader = r.file.clone();
        noHeader[third + 3] ^= 0x40;
        try {
            AudioKit.readAll(AudioDecoders.open(noHeader, null, null));
            Check.fail("a damaged header must lose sync");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "lost sync");
        }
        byte[] cut = Arrays.copyOf(r.file, r.file.length - r.frames[7].length / 2);
        byte[] part = AudioKit.readAll(AudioDecoders.open(cut, null, null));
        Check.equal(7000 * 4, part.length, "a cut last frame ends the stream cleanly");
        Check.equal(true, Arrays.equals(Arrays.copyOf(all, part.length), part), "with exact samples");
    }

    /**
     * Invalid metadata fails cleanly with CORRUPT and no cut of a valid file throws a runtime exception.
     * <p>
     * 올바르지 않은 메타데이터는 CORRUPT 로 깔끔하게 실패하고, 올바른 파일을 어디서 잘라도 런타임 예외가 나지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidFiles() throws Exception {
        long[][] x = signal(2, 3000, 16, 90);
        FlacWriter.Options o = new FlacWriter.Options();
        o.blockSize = 1000;
        o.seekEvery = 1;
        byte[] good = FlacWriter.encode(x, o).file;
        // 모든 길이로 잘라 열어 읽는다(간격을 두고).
        for (int cut = 0; cut < good.length; cut += (cut < 200 ? 1 : 97)) {
            try {
                AudioKit.readAll(AudioDecoders.open(Arrays.copyOf(good, cut), null, null));
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "a media exception at " + cut);
            }
        }
        byte[] notInfo = good.clone();
        notInfo[4] = (byte) (notInfo[4] & 0x80);
        notInfo[4] |= 1;
        expectFailure(notInfo, MediaException.Kind.CORRUPT, "first block is not STREAMINFO");
        byte[] zeroRate = good.clone();
        zeroRate[18] = 0;
        zeroRate[19] = 0;
        zeroRate[20] &= 0x0F;
        expectFailure(zeroRate, MediaException.Kind.CORRUPT, "sample rate 0");
        Random rnd = new Random(91);
        // 임의의 바이트를 바꾼 파일도 MediaException 이나 정상 종료만 있어야 한다.
        for (int k = 0; k < 150; k++) {
            byte[] fuzz = good.clone();
            // 바이트 네 개를 임의의 값으로 바꾼다.
            for (int j = 0; j < 4; j++) {
                fuzz[rnd.nextInt(fuzz.length)] = (byte) rnd.nextInt(256);
            }
            try {
                AudioKit.readAll(AudioDecoders.open(fuzz, null, null));
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "fuzz " + k);
            }
        }
    }

    /**
     * Opens data that must fail and checks the kind.
     * <p>
     * 실패해야 하는 데이터를 열어 종류를 확인한다.
     *
     * @param file file bytes / 파일 바이트
     * @param kind expected kind / 기대하는 종류
     * @param what description / 설명
     */
    private static void expectFailure(byte[] file, MediaException.Kind kind, String what) {
        try {
            AudioKit.readAll(AudioDecoders.open(file, null, null));
            Check.fail(what + " must fail");
        } catch (MediaException e) {
            Check.equal(kind, e.getKind(), what);
        }
    }

    /**
     * FLAC in Ogg: audio packets over many pages, exact seeking and recognition by signature.
     * <p>
     * Ogg 안의 FLAC: 여러 페이지에 걸친 오디오 패킷, 정확한 탐색, 시그니처로의 인식.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOggFlac() throws Exception {
        long[][] x = signal(2, 20000, 16, 100);
        byte[] all = expected(x, 16);
        // 페이지당 세그먼트 수를 바꿔 가며 같은 결과가 나오는지 확인한다.
        for (int maxSegs : new int[] { 255, 9, 2 }) {
            FlacWriter.Options o = new FlacWriter.Options();
            o.blockSize = 900;
            o.strategy = "lpc6";
            FlacWriter.Result r = FlacWriter.encode(x, o);
            byte[] comment = new byte[] { (byte) 0x84, 0, 0, 8, 1, 2, 3, 4, 5, 6, 7, 8 };
            byte[][] packets = FlacWriter.oggPackets(r, new byte[][] { comment });
            long[] granules = new long[packets.length];
            long total = 0;
            // 프레임 패킷마다 그때까지의 샘플 수를 그래뉼 위치로 쓴다.
            for (int i = 2; i < packets.length; i++) {
                total += r.frameSize[i - 2];
                granules[i] = total;
            }
            byte[] head = OggMux.mux(77, 0, new byte[][] { packets[0] }, new long[] { 0 }, 255, true, false);
            byte[] meta = OggMux.mux(77, 1, new byte[][] { packets[1] }, new long[] { 0 }, 255, false, false);
            byte[][] audio = Arrays.copyOfRange(packets, 2, packets.length);
            byte[] data = OggMux.mux(77, 2, audio, Arrays.copyOfRange(granules, 2, granules.length), maxSegs, false, true);
            byte[] file = OggMux.concat(head, meta, data);
            PcmSource s = AudioDecoders.open(file, "audio/ogg", null);
            Check.equal(20000, s.getFrameCount(), "frame count from STREAMINFO");
            Check.equal(true, Arrays.equals(all, AudioKit.readAll(s)), "all samples, " + maxSegs + " segments per page");
            long[] targets = { 0, 899, 900, 901, 10000, 19999, 20000, 123, 15000 };
            // 앞뒤로 이동해도 정확한 위치에서 읽는지 확인한다.
            for (long t : targets) {
                s.seekToFrame(t);
                byte[] exp = Arrays.copyOfRange(all, (int) Math.min(t, 20000) * 4, all.length);
                Check.equal(true, Arrays.equals(exp, AudioKit.readAll(s)), "Ogg FLAC seek to " + t + ", " + maxSegs + " segments per page");
            }
        }
    }
}
