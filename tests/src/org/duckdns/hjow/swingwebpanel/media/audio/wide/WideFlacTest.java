package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.util.Arrays;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.FlacWriter;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the wide path of the FLAC decoder (plan item P25-05) against ground truth that the test makes itself: integer samples of 4 to 32 bits
 * (with the extreme values), encoded by the FLAC writer of the test kit (several block sizes, prediction strategies and stereo modes), must come out as
 * exactly <code>(float) (sample / 2^(bits-1))</code> in all channels (1 to 8), in a native FLAC stream and in an Ogg FLAC stream, whatever the size of
 * the reads.
 * <p>
 * FLAC 디코더의 넓은 경로 시험(계획 P25-05). 정답은 시험이 직접 만든다: 4 ~ 32비트 정수 표본(극단값 포함)을 시험 도구의 FLAC 기록기로 인코딩하면(여러 블록
 * 크기, 예측 전략, 스테레오 모드) 읽기 크기와 무관하게 네이티브 FLAC 스트림과 Ogg FLAC 스트림에서 모든 채널(1 ~ 8)에서 정확히
 * <code>(float) (표본 / 2^(비트-1))</code> 로 나와야 한다.
 */
public class WideFlacTest {

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public WideFlacTest() {
    }

    /**
     * Makes a deterministic signal inside the range of a bit depth: two sines of different frequency per channel plus noise.
     * <p>
     * 비트 깊이의 범위 안에서 결정적인 신호를 만든다: 채널마다 주파수가 다른 사인파 두 개와 잡음.
     *
     * @param ch   channels / 채널 수
     * @param n    samples per channel / 채널당 표본 수
     * @param bits bit depth / 비트 깊이
     * @param seed seed / 시드
     * @return samples <code>[channel][index]</code> / 표본 <code>[채널][위치]</code>
     */
    private static long[][] signal(int ch, int n, int bits, long seed) {
        Random r = new Random(seed);
        long[][] x = new long[ch][n];
        double amp = Math.pow(2, bits - 1) * 0.55;
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
     * The expected floats: <code>(float) (v / 2^(bits-1))</code> in double arithmetic.
     * <p>
     * 기대 float: double 연산으로 <code>(float) (v / 2^(bits-1))</code>.
     *
     * @param x    samples / 표본
     * @param bits bit depth / 비트 깊이
     * @return the expected floats / 기대 float
     */
    private static float[][] expected(long[][] x, int bits) {
        float[][] e = new float[x.length][x[0].length];
        double scale = 1.0 / (double) (1L << (bits - 1));
        // 모든 표본을 double 로 나눈 값을 float 로 줄인다.
        for (int c = 0; c < x.length; c++) {
            for (int i = 0; i < x[c].length; i++) {
                e[c][i] = (float) (x[c][i] * scale);
            }
        }
        return e;
    }

    /**
     * Wraps a FLAC stream in Ogg pages (the identification packet, one comment packet, one packet per frame; the granule position of a packet is
     * the number of samples up to the end of its frame).
     * <p>
     * FLAC 스트림을 Ogg 페이지에 담는다(식별 패킷, 주석 패킷 하나, 프레임마다 패킷 하나. 패킷의 그래뉼 위치는 그 프레임의 끝까지의 샘플 수).
     *
     * @param r the encoded stream / 인코딩한 스트림
     * @return the Ogg file / Ogg 파일
     */
    private static byte[] toOgg(FlacWriter.Result r) {
        byte[] comment = new byte[] {(byte) 0x84, 0, 0, 8, 1, 2, 3, 4, 5, 6, 7, 8};
        byte[][] packets = FlacWriter.oggPackets(r, new byte[][] {comment});
        long[] granules = new long[packets.length];
        long total = 0;
        // 프레임 패킷마다 그때까지의 샘플 수를 그래뉼 위치로 쓴다.
        for (int i = 2; i < packets.length; i++) {
            total += r.frameSize[i - 2];
            granules[i] = total;
        }
        byte[] head = OggMux.mux(77, 0, new byte[][] {packets[0]}, new long[] {0}, 255, true, false);
        byte[] meta = OggMux.mux(77, 1, new byte[][] {packets[1]}, new long[] {0}, 255, false, false);
        byte[][] audio = Arrays.copyOfRange(packets, 2, packets.length);
        byte[] data = OggMux.mux(77, 2, audio, Arrays.copyOfRange(granules, 2, granules.length), 255, false, true);
        return OggMux.concat(head, meta, data);
    }

    /**
     * Every bit depth in every channel count, native and in Ogg: the wide path gives exactly the integers divided by a power of two.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryDepthAndChannelCount() throws Exception {
        int[] depths = {4, 8, 12, 16, 20, 24, 32};
        int[] channels = {1, 2, 3, 6, 8};
        // 비트 깊이와 채널 수의 모든 조합을 시험한다.
        for (int d = 0; d < depths.length; d++) {
            for (int c = 0; c < channels.length; c++) {
                int bits = depths[d];
                int ch = channels[c];
                long[][] x = signal(ch, 3000, bits, 50 + d * 10 + c);
                FlacWriter.Options o = new FlacWriter.Options();
                o.bps = bits;
                o.rate = 8000;
                o.blockSize = 1000;
                o.stereo = ch == 2 ? 3 : 0;
                o.strategy = "lpc6";
                o.riceMethod1 = bits > 16;
                FlacWriter.Result r = FlacWriter.encode(x, o);
                float[][] want = expected(x, bits);
                WideKit.assertSame(bits + " bit, " + ch + " channels, native", want, WideKit.wide(r.file, "audio/flac"));
                WideKit.assertSame(bits + " bit, " + ch + " channels, in Ogg", want, WideKit.wide(toOgg(r), "audio/ogg"));
            }
        }
    }

    /**
     * The extreme values of every depth (the smallest, the largest, zero and minus one) survive: a 32 bit sample of 2147483647 is the float
     * nearest to <code>2147483647 / 2^31</code> (1.0f), the smallest is exactly -1.0f.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExtremeValues() throws Exception {
        int[] depths = {8, 16, 24, 32};
        // 깊이마다 극단값만으로 된 채널 둘의 스트림을 시험한다.
        for (int d = 0; d < depths.length; d++) {
            int bits = depths[d];
            long min = -(1L << (bits - 1));
            long max = (1L << (bits - 1)) - 1;
            long[][] x = {{max, min, 0, -1, max, min, 1, min + 1, max - 1}, {min, max, -1, 0, min, max, -2, max - 1, min + 1}};
            FlacWriter.Options o = new FlacWriter.Options();
            o.bps = bits;
            o.rate = 8000;
            o.strategy = "verbatim";
            WideKit.assertSame(bits + " bit extremes", expected(x, bits), WideKit.wide(FlacWriter.encode(x, o).file, null));
        }
    }

    /**
     * Block sizes from 16 to 4608 and strategies (constant, fixed, LPC) with the stereo modes give the same floats: the conversion does not depend on how
     * the stream was coded.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockSizesAndStrategies() throws Exception {
        long[][] x = signal(2, 5000, 24, 9);
        float[][] want = expected(x, 24);
        int[] blocks = {16, 192, 1152, 4096, 4608};
        String[] strategies = {"fixed2", "lpc6", "verbatim"};
        // 블록 크기, 예측 전략, 스테레오 모드의 조합을 시험한다.
        for (int b = 0; b < blocks.length; b++) {
            for (int s = 0; s < strategies.length; s++) {
                // 항목마다 반복해서 처리한다.
                for (int mode = 0; mode <= 3; mode++) {
                    FlacWriter.Options o = new FlacWriter.Options();
                    o.bps = 24;
                    o.rate = 8000;
                    o.blockSize = blocks[b];
                    o.strategy = strategies[s];
                    o.stereo = mode;
                    WideKit.assertSame("block " + blocks[b] + " " + strategies[s] + " stereo " + mode, want, WideKit.wide(FlacWriter.encode(x, o).file, null));
                }
            }
        }
    }
}
