package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the wide path of the uncompressed families (WAV, AIFF, AU; plan item P25-05) against ground truth that the test makes itself: the
 * integer samples are generated here (including the extreme values), packed into the container with the file writers of the test kit and
 * the expected float of every sample is computed as <code>(float) (sample / 2^(bits-1))</code> in double arithmetic, independent of the decoders. The
 * decoded floats must be identical bit for bit, for 8, 16, 24 and 32 bit integers, 32 and 64 bit floats, in 1, 2, 3, 6 and 8 channels, little and
 * big endian, with the channels in the order of the file.
 * <p>
 * 비압축 계열(WAV, AIFF, AU)의 넓은 경로 시험(계획 P25-05). 정답은 시험이 직접 만든다: 정수 표본을 여기서(극단값 포함) 만들고 시험 도구의 파일 기록기로 컨테이너에
 * 담으며, 모든 표본의 기대 float 는 디코더와 무관하게 double 연산으로 <code>(float) (표본 / 2^(비트-1))</code> 로 계산한다. 디코딩한 float 는 8, 16, 24, 32비트 정수,
 * 32, 64비트 float, 채널 1, 2, 3, 6, 8개, 리틀/빅 엔디언에서 비트 단위로 같아야 하고 채널은 파일의 순서를 따라야 한다.
 */
public class WidePcmTest {

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public WidePcmTest() {
    }

    /**
     * Makes integer samples of a width, with the extreme values at the beginning.
     * <p>
     * 비트 폭의 정수 표본을 만든다. 처음에 극단값을 둔다.
     *
     * @param channels the channels / 채널 수
     * @param frames   the frames / 프레임 수
     * @param bits     the width in bits / 비트 폭
     * @param seed     the random seed / 난수 시드
     * @return the samples as <code>[channel][frame]</code> / <code>[채널][프레임]</code> 형태의 표본
     */
    private static long[][] ints(int channels, int frames, int bits, long seed) {
        Random rnd = new Random(seed);
        long min = -(1L << (bits - 1));
        long max = (1L << (bits - 1)) - 1;
        long[][] x = new long[channels][frames];
        // 채널마다 극단값 몇 개와 무작위 값을 넣는다.
        for (int c = 0; c < channels; c++) {
            for (int f = 0; f < frames; f++) {
                long v;
                if (f == 0) {
                    v = min;
                } else if (f == 1) {
                    v = max;
                } else if (f == 2) {
                    v = 0;
                } else if (f == 3) {
                    v = -1;
                } else {
                    v = min + (long) (rnd.nextDouble() * (double) (max - min));
                }
                x[c][f] = v;
            }
        }
        return x;
    }

    /**
     * Packs samples as interleaved bytes.
     * <p>
     * 표본을 인터리브된 바이트로 만든다.
     *
     * @param x         the samples / 표본
     * @param bytes     the bytes per sample / 표본당 바이트 수
     * @param bigEndian true for big endian / 빅 엔디언이면 true
     * @param offset    added to every sample (128 packs signed 8 bit samples as unsigned) / 모든 표본에 더하는 값(128 이면 부호 있는 8비트를 부호 없음으로 만든다)
     * @return the bytes / 바이트
     */
    private static byte[] pack(long[][] x, int bytes, boolean bigEndian, int offset) {
        int ch = x.length;
        byte[] b = new byte[x[0].length * ch * bytes];
        int p = 0;
        // 프레임과 채널 순서로 표본을 바이트로 푼다.
        for (int f = 0; f < x[0].length; f++) {
            for (int c = 0; c < ch; c++) {
                long v = x[c][f] + offset;
                // 바이트마다 처리한다.
                for (int k = 0; k < bytes; k++) {
                    int shift = bigEndian ? 8 * (bytes - 1 - k) : 8 * k;
                    b[p++] = (byte) (v >> shift);
                }
            }
        }
        return b;
    }

    /**
     * The expected floats of integer samples: <code>(float) (v / 2^(bits-1))</code> in double arithmetic.
     * <p>
     * 정수 표본의 기대 float: double 연산으로 <code>(float) (v / 2^(bits-1))</code>.
     *
     * @param x    the samples / 표본
     * @param bits the width in bits / 비트 폭
     * @return the expected floats / 기대 float
     */
    private static float[][] expected(long[][] x, int bits) {
        float[][] e = new float[x.length][x[0].length];
        double scale = 1.0 / (double) (1L << (bits - 1));
        // 모든 표본을 double 로 나눈 값을 float 로 줄인다.
        for (int c = 0; c < x.length; c++) {
            for (int f = 0; f < x[c].length; f++) {
                e[c][f] = (float) (x[c][f] * scale);
            }
        }
        return e;
    }

    /**
     * Integer WAV files of every width and channel count are decoded to exactly <code>v / 2^(bits-1)</code>; the channels are in the file order,
     * also with a speaker mask (<code>WAVE_FORMAT_EXTENSIBLE</code>), and the 8 bit files are unsigned.
     * <p>
     * 모든 폭과 채널 수의 정수 WAV 파일은 정확히 <code>v / 2^(bits-1)</code> 로 디코딩된다. 채널은 스피커 마스크(<code>WAVE_FORMAT_EXTENSIBLE</code>)가 있어도 파일의
     * 순서이고 8비트 파일은 부호 없음이다.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testIntegerWav() throws MediaException {
        int[] widths = {8, 16, 24, 32};
        int[] channels = {1, 2, 3, 6, 8};
        // 폭과 채널 수의 모든 조합을 시험한다.
        for (int w = 0; w < widths.length; w++) {
            for (int c = 0; c < channels.length; c++) {
                int bits = widths[w];
                long[][] x = ints(channels[c], 3000, bits, 100 + w * 10 + c);
                byte[] data = pack(x, bits / 8, false, bits == 8 ? 128 : 0);
                float[][] want = expected(x, bits);
                String what = bits + " bit " + channels[c] + " channels";
                WideKit.assertSame("plain WAV " + what, want, WideKit.wide(AudioKit.wav(1, channels[c], 8000, bits, data, -1), "audio/wav"));
                // 마스크가 있는 파일도 같다(마스크의 켜진 비트가 채널 수와 같게 만든다).
                long mask = (1L << channels[c]) - 1;
                WideKit.assertSame("extensible WAV " + what, want, WideKit.wide(AudioKit.wav(1, channels[c], 8000, bits, data, mask), "audio/wav"));
            }
        }
    }

    /**
     * Float WAV files (32 and 64 bit) pass the samples through: the 32 bit value itself, the 64 bit value rounded to a float; values outside
     * -1 to 1, tiny values and infinities are kept.
     * <p>
     * float WAV 파일(32, 64비트)은 표본을 그대로 통과시킨다. 32비트는 값 그대로, 64비트는 float 로 반올림한 값이다. -1 ~ 1 밖의 값, 아주 작은 값, 무한대도 유지된다.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testFloatWav() throws MediaException {
        double[] values = {0.0, 1.0, -1.0, 0.5, -0.25, 1.5, -3.25, 1e-30, 123456.789, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.1, 1.0 / 3.0};
        int[] channels = {1, 2, 6};
        // 채널 수마다 값을 번갈아 채널에 넣는다.
        for (int ci = 0; ci < channels.length; ci++) {
            int ch = channels[ci];
            int frames = values.length * 2;
            float[][] want = new float[ch][frames];
            byte[] d32 = new byte[frames * ch * 4];
            byte[] d64 = new byte[frames * ch * 8];
            int p32 = 0;
            int p64 = 0;
            // 프레임마다 채널의 값을 정하고 두 폭으로 기록한다.
            for (int f = 0; f < frames; f++) {
                for (int c = 0; c < ch; c++) {
                    double v = values[(f + c * 3) % values.length] * ((f / values.length) == 0 ? 1 : -1);
                    float f32 = (float) v;
                    want[c][f] = f32;
                    int bits = Float.floatToIntBits(f32);
                    // 바이트마다 처리한다.
                    for (int k = 0; k < 4; k++) {
                        d32[p32++] = (byte) (bits >> (8 * k));
                    }
                    long lbits = Double.doubleToLongBits(v);
                    // 바이트마다 처리한다.
                    for (int k = 0; k < 8; k++) {
                        d64[p64++] = (byte) (lbits >> (8 * k));
                    }
                }
            }
            WideKit.assertSame("32 bit float WAV, " + ch + " channels", want, WideKit.wide(AudioKit.wav(3, ch, 8000, 32, d32, -1), "audio/wav"));
            WideKit.assertSame("64 bit float WAV, " + ch + " channels", want, WideKit.wide(AudioKit.wav(3, ch, 8000, 64, d64, -1), "audio/wav"));
        }
    }

    /**
     * AIFF and AIFF-C files: big endian integers of 8, 16, 24 and 32 bits, the little endian <code>sowt</code> form, 32 and 64 bit floats in
     * AIFF-C (<code>fl32</code>, <code>fl64</code>), in 1, 2 and 6 channels. The AIFF 8 bit samples are signed.
     * <p>
     * AIFF 와 AIFF-C 파일: 빅 엔디언 8, 16, 24, 32비트 정수, 리틀 엔디언 <code>sowt</code> 형식, AIFF-C 의 32, 64비트 float(<code>fl32</code>, <code>fl64</code>), 채널 1, 2, 6개.
     * AIFF 의 8비트 표본은 부호 있음이다.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testAiff() throws MediaException {
        int[] widths = {8, 16, 24, 32};
        int[] channels = {1, 2, 6};
        // 폭과 채널 수마다 AIFF(빅 엔디언)와 AIFF-C sowt(리틀 엔디언)를 시험한다.
        for (int w = 0; w < widths.length; w++) {
            for (int c = 0; c < channels.length; c++) {
                int bits = widths[w];
                int ch = channels[c];
                long[][] x = ints(ch, 2500, bits, 200 + w * 10 + c);
                float[][] want = expected(x, bits);
                String what = bits + " bit " + ch + " channels";
                WideKit.assertSame("AIFF " + what, want, WideKit.wide(AudioKit.aiff(null, ch, bits, 8000, 2500, pack(x, bits / 8, true, 0)), "audio/aiff"));
                WideKit.assertSame("AIFF-C sowt " + what, want, WideKit.wide(AudioKit.aiff("sowt", ch, bits, 8000, 2500, pack(x, bits / 8, false, 0)), "audio/aifc"));
            }
        }
        double[] values = {0.0, 1.0, -1.0, 0.75, 2.5, -1e-20, 0.1};
        // AIFF-C 의 float 두 폭을 채널 수마다 시험한다.
        for (int c = 0; c < channels.length; c++) {
            int ch = channels[c];
            int frames = 14;
            float[][] want = new float[ch][frames];
            byte[] d32 = new byte[frames * ch * 4];
            byte[] d64 = new byte[frames * ch * 8];
            int p32 = 0;
            int p64 = 0;
            // 프레임마다 처리한다.
            for (int f = 0; f < frames; f++) {
                // 채널마다 처리한다.
                for (int k = 0; k < ch; k++) {
                    double v = values[(f + k) % values.length];
                    want[k][f] = (float) v;
                    int bits = Float.floatToIntBits((float) v);
                    // 바이트마다 처리한다.
                    for (int b = 3; b >= 0; b--) {
                        d32[p32++] = (byte) (bits >> (8 * b));
                    }
                    long lbits = Double.doubleToLongBits(v);
                    // 바이트마다 처리한다.
                    for (int b = 7; b >= 0; b--) {
                        d64[p64++] = (byte) (lbits >> (8 * b));
                    }
                }
            }
            WideKit.assertSame("AIFF-C fl32 " + ch + " channels", want, WideKit.wide(AudioKit.aiff("fl32", ch, 32, 8000, frames, d32), "audio/aifc"));
            WideKit.assertSame("AIFF-C fl64 " + ch + " channels", want, WideKit.wide(AudioKit.aiff("fl64", ch, 64, 8000, frames, d64), "audio/aifc"));
        }
    }

    /**
     * AU files: the signed big endian integers of 8, 16, 24 and 32 bits (encodings 2 to 5), the floats of 32 and 64 bits (encodings 6 and 7), in
     * 1, 2 and 6 channels.
     * <p>
     * AU 파일: 부호 있는 빅 엔디언 8, 16, 24, 32비트 정수(인코딩 2 ~ 5), 32, 64비트 float(인코딩 6, 7), 채널 1, 2, 6개.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testAu() throws MediaException {
        int[] widths = {8, 16, 24, 32};
        int[] encodings = {2, 3, 4, 5};
        int[] channels = {1, 2, 6};
        // 폭과 채널 수의 모든 조합을 시험한다.
        for (int w = 0; w < widths.length; w++) {
            for (int c = 0; c < channels.length; c++) {
                int bits = widths[w];
                int ch = channels[c];
                long[][] x = ints(ch, 2000, bits, 300 + w * 10 + c);
                byte[] au = AudioKit.au(encodings[w], ch, 8000, pack(x, bits / 8, true, 0), 0xFFFFFFFFL);
                WideKit.assertSame("AU " + bits + " bit " + ch + " channels", expected(x, bits), WideKit.wide(au, "audio/basic"));
            }
        }
        float[][] want = new float[2][3];
        byte[] d32 = new byte[3 * 2 * 4];
        byte[] d64 = new byte[3 * 2 * 8];
        double[] v = {0.5, -0.5, 1.25, -2.0, 0.0, 1e-10};
        int p32 = 0;
        int p64 = 0;
        // 프레임 세 개의 float 값을 두 폭으로 기록한다.
        for (int f = 0; f < 3; f++) {
            for (int c = 0; c < 2; c++) {
                double d = v[f * 2 + c];
                want[c][f] = (float) d;
                int bits = Float.floatToIntBits((float) d);
                // 바이트마다 처리한다.
                for (int b = 3; b >= 0; b--) {
                    d32[p32++] = (byte) (bits >> (8 * b));
                }
                long lbits = Double.doubleToLongBits(d);
                // 바이트마다 처리한다.
                for (int b = 7; b >= 0; b--) {
                    d64[p64++] = (byte) (lbits >> (8 * b));
                }
            }
        }
        WideKit.assertSame("AU float32", want, WideKit.wide(AudioKit.au(6, 2, 8000, d32, 0xFFFFFFFFL), null));
        WideKit.assertSame("AU float64", want, WideKit.wide(AudioKit.au(7, 2, 8000, d64, 0xFFFFFFFFL), null));
    }

    /**
     * G.711: the well known end points of mu-law (0xFF is 0, 0x00 is -32124, 0x80 is 32124) and A-law (0xD5 is 8, 0x55 is -8, 0xAA is 32256,
     * 0x2A is -32256), divided by 32768, in a file of 2 channels.
     * <p>
     * G.711: mu-law 의 잘 알려진 끝점(0xFF 는 0, 0x00 은 -32124, 0x80 은 32124)과 A-law(0xD5 는 8, 0x55 는 -8, 0xAA 는 32256, 0x2A 는 -32256)를 32768 로 나눈 값. 채널
     * 둘인 파일에서 확인한다.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testCompandedSamples() throws MediaException {
        byte[] mu = {(byte) 0xFF, (byte) 0x00, (byte) 0x80, (byte) 0xFF};
        float[][] wantMu = {{0f, 32124 / 32768f}, {-32124 / 32768f, 0f}};
        WideKit.assertSame("mu-law AU", wantMu, WideKit.wide(AudioKit.au(1, 2, 8000, mu, 0xFFFFFFFFL), null));
        byte[] a = {(byte) 0xD5, (byte) 0x55, (byte) 0xAA, (byte) 0x2A};
        float[][] wantA = {{8 / 32768f, 32256 / 32768f}, {-8 / 32768f, -32256 / 32768f}};
        WideKit.assertSame("A-law AU", wantA, WideKit.wide(AudioKit.au(27, 2, 8000, a, 0xFFFFFFFFL), null));
    }

    /**
     * Reading the wide source of a file in reads of every size gives the same samples (block boundaries are invisible), and the frame count is
     * the number of frames that are delivered.
     *
     * @throws MediaException if decoding fails / 디코딩이 실패하면
     */
    public void testReadSizesDoNotMatter() throws MediaException {
        long[][] x = ints(3, 5000, 24, 77);
        byte[] wav = AudioKit.wav(1, 3, 8000, 24, pack(x, 3, false, 0), -1);
        float[][] want = expected(x, 24);
        int[] sizes = {1, 2, 3, 100, 2047, 2048, 2049, 4096, 5000, 9999};
        // 읽기 크기마다 처음부터 끝까지 읽어 같은 표본이 나오는지 확인한다.
        for (int s = 0; s < sizes.length; s++) {
            org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource src = org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders.openWide(wav, null, null);
            Check.equal(3, src.getChannels(), "channels");
            Check.equal(5000L, src.getFrameCount(), "frame count");
            float[][] buf = new float[3][sizes[s]];
            float[][] got = new float[3][5000];
            int pos = 0;
            // 끝이 나올 때까지 읽고 이어 붙인다.
            for (;;) {
                int n = src.read(buf, 0, sizes[s]);
                if (n < 0) {
                    break;
                }
                // 채널마다 처리한다.
                for (int c = 0; c < 3; c++) {
                    System.arraycopy(buf[c], 0, got[c], pos, n);
                }
                pos += n;
            }
            src.close();
            Check.equal(5000, pos, "frames for the read size " + sizes[s]);
            WideKit.assertSame("read size " + sizes[s], want, got);
        }
    }
}
