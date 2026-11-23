package org.duckdns.hjow.swingwebpanel.media.audio.aac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.mp4.Mp4TestBuilder;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the AAC decoder (plan item P18-07, FEATURE_MATRIX AUD-05). The streams come from {@link AacTestEncoder}, an AAC-LC encoder written
 * from the standard that shares only the Huffman tables with the decoder, and from {@link Mp4TestBuilder}. No real-world AAC files are
 * available to the tests (see docs/progress/media-aud-b.md).
 * <p>
 * AAC 디코더 시험(계획 P18-07, FEATURE_MATRIX AUD-05). 스트림은 표준에서 작성해 허프만 표만 디코더와 공유하는 AAC-LC 인코더 {@link AacTestEncoder}
 * 와 {@link Mp4TestBuilder} 가 만든다. 실제 AAC 파일은 시험에 쓰지 못했다(docs/progress/media-aud-b.md 참조).
 */
public class AacDecoderTest {

    /**
     * Creates the suite (required by the runner). / 스위트를 만든다(실행기가 필요로 함).
     */
    public AacDecoderTest() {
    }

    /**
     * Makes a smooth test signal: sines with a fade in and out.
     * <p>
     * 부드러운 시험 신호를 만든다: 페이드 인/아웃이 있는 사인파.
     *
     * @param n     samples / 샘플 수
     * @param rate  sampling frequency / 샘플링 주파수
     * @param freqs frequencies by channel / 채널별 주파수
     * @return signal / 신호
     */
    static double[][] tones(int n, int rate, double[][] freqs) {
        double[][] x = new double[freqs.length][n];
        for (int c = 0; c < freqs.length; c++) { // 채널마다 사인파를 더한다
            for (int t = 0; t < freqs[c].length; t++) {
                for (int i = 0; i < n; i++) {
                    double a = Math.min(1.0, Math.min(i, n - 1 - i) / 3000.0);
                    double env = Math.sin(a * Math.PI / 2) * Math.sin(a * Math.PI / 2);
                    x[c][i] += 0.25 * Math.sin(2 * Math.PI * freqs[c][t] * i / rate) * env;
                }
            }
        }
        return x;
    }

    /**
     * Decodes a whole file.
     * <p>
     * 파일 전체를 해독한다.
     *
     * @param file data / 데이터
     * @return interleaved samples / 인터리브된 샘플
     * @throws MediaException on a decoding error / 해독 오류
     */
    static short[] decode(byte[] file) throws MediaException {
        PcmSource s = new AacDecoder().open(file, DecodeLimits.DEFAULT);
        try {
            return AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
    }

    /**
     * The SNR in dB of a channel over a range of the input; decoded sample j corresponds to input sample j + shift.
     * <p>
     * 입력 범위에 대한 한 채널의 SNR(dB). 해독한 샘플 j 는 입력 샘플 j + shift 에 해당한다.
     *
     * @param x        input / 입력
     * @param pcm      decoded interleaved / 해독한 인터리브 샘플
     * @param channels channels / 채널 수
     * @param ch       channel / 채널
     * @param shift    offset of the decoded samples in the input / 입력에서 해독한 샘플의 오프셋
     * @param from     first input sample / 첫 입력 샘플
     * @param to       end of the range / 범위의 끝
     * @return SNR / SNR
     */
    static double snr(double[] x, short[] pcm, int channels, int ch, int shift, int from, int to) {
        double sig = 0;
        double err = 0;
        for (int i = from; i < to; i++) { // 범위의 샘플마다 오차 에너지를 더한다
            int j = i - shift;
            double v = (j >= 0 && (j + 1) * channels <= pcm.length) ? pcm[j * channels + ch] / 32768.0 : 0.0;
            sig += x[i] * x[i];
            err += (x[i] - v) * (x[i] - v);
        }
        return 10 * Math.log10(sig / Math.max(err, 1e-30));
    }

    /**
     * Encodes with the given encoder and checks the SNR of every channel.
     * <p>
     * 인코더로 부호화해 모든 채널의 SNR 을 확인한다.
     *
     * @param enc    encoder / 인코더
     * @param x      signal / 신호
     * @param limit  minimum SNR / 최소 SNR
     * @param what   description / 설명
     * @throws Exception on failure / 실패 시
     */
    private static void roundTrip(AacTestEncoder enc, double[][] x, double limit, String what) throws Exception {
        byte[] adts = enc.encodeAdts(x);
        short[] pcm = decode(adts);
        for (int ch = 0; ch < x.length; ch++) { // 채널마다 SNR 을 확인한다
            double s = snr(x[ch], pcm, x.length, ch, 0, 2048, x[0].length - 2048);
            Check.isTrue(s > limit, what + " channel " + ch + " SNR " + s);
        }
    }

    /**
     * Stereo long blocks at 44.1 kHz.
     * <p>
     * 44.1 kHz 스테레오 긴 블록.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStereoLongBlocks() throws Exception {
        double[][] x = tones(30000, 44100, new double[][] { { 440, 3000 }, { 1000, 5200 } });
        roundTrip(new AacTestEncoder(4, 2), x, 35, "stereo");
    }

    /**
     * Mono at every sampling frequency index of the tables (scale factor band layouts).
     * <p>
     * 표의 모든 샘플링 주파수 인덱스(스케일팩터 대역 배치)에서의 모노.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSamplingFrequencies() throws Exception {
        int[] idx = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        for (int k = 0; k < idx.length; k++) { // 샘플링 주파수 인덱스마다 시험한다
            int rate = AacBands.SAMPLE_RATES[idx[k]];
            double[][] x = tones(16000, rate, new double[][] { { rate * 0.01, rate * 0.11 } });
            roundTrip(new AacTestEncoder(idx[k], 1), x, 33, "rate " + rate);
        }
    }

    /**
     * Sine and Kaiser-Bessel window shapes and the switch between them.
     * <p>
     * 사인과 카이저-베셀 윈도 모양, 그리고 둘 사이의 전환.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWindowShapes() throws Exception {
        double[][] x = tones(30000, 44100, new double[][] { { 700, 2500 }, { 1300 } });
        AacTestEncoder enc = new AacTestEncoder(4, 2);
        enc.shapes = new int[] { 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0 };
        roundTrip(enc, x, 35, "window shapes");
    }

    /**
     * Window switching with eight short windows (start, short, stop) in several groupings and with both window shapes.
     * <p>
     * 짧은 윈도 8개를 쓰는 윈도 전환(시작, 짧은 블록, 정지). 여러 그룹과 두 윈도 모양을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShortWindows() throws Exception {
        double[][] x = tones(40000, 44100, new double[][] { { 500, 2200 }, { 900 } });
        int[] groupings = { 0, 0x7F, 0x55, 0x33, 0x0F };
        for (int g = 0; g < groupings.length; g++) { // 윈도 그룹 방식마다 시험한다
            AacTestEncoder enc = new AacTestEncoder(4, 2);
            enc.sequences = new int[] { 0, 0, 0, 1, 2, 2, 3, 0, 0, 1, 2, 3, 0, 0, 0, 1, 2, 2, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            enc.shapes = g % 2 == 0 ? new int[0] : new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
            enc.grouping = groupings[g];
            roundTrip(enc, x, 30, "short windows grouping " + groupings[g]);
        }
    }

    /**
     * M/S stereo (all bands and per band).
     * <p>
     * M/S 스테레오(모든 대역, 대역별).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMidSide() throws Exception {
        double[][] x = tones(30000, 48000, new double[][] { { 700, 5000 }, { 700, 2500 } });
        for (int mode = 1; mode <= 2; mode++) { // MS 마스크 방식마다
            AacTestEncoder enc = new AacTestEncoder(3, 2);
            enc.msMode = mode;
            roundTrip(enc, x, 35, "ms mode " + mode);
        }
    }

    /**
     * Scale factors that differ between the bands, TNS filtering and pulse data.
     * <p>
     * 대역마다 다른 스케일팩터, TNS 필터링, 펄스 데이터.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScaleFactorsTnsAndPulses() throws Exception {
        double[][] x = tones(30000, 44100, new double[][] { { 440, 3000, 7000 } });
        AacTestEncoder a = new AacTestEncoder(4, 1);
        a.varySf = true;
        roundTrip(a, x, 33, "varying scale factors");
        AacTestEncoder b = new AacTestEncoder(4, 1);
        b.tnsFilters = 1;
        roundTrip(b, x, 30, "TNS");
        AacTestEncoder c = new AacTestEncoder(4, 1);
        c.pulse = true;
        roundTrip(c, x, 33, "pulse data");
        AacTestEncoder d = new AacTestEncoder(4, 1);
        d.tnsFilters = 1;
        d.sequences = new int[] { 0, 0, 0, 1, 2, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        roundTrip(d, x, 28, "TNS with short windows");
    }

    /**
     * Builds the AudioSpecificConfig of AAC-LC.
     * <p>
     * AAC-LC 의 AudioSpecificConfig 를 만든다.
     *
     * @param rateIndex sampling frequency index / 샘플링 주파수 인덱스
     * @param channels  channel configuration / 채널 구성
     * @return two bytes / 2바이트
     */
    static byte[] asc(int rateIndex, int channels) {
        int v = (2 << 11) | (rateIndex << 7) | (channels << 3);
        return new byte[] { (byte) (v >> 8), (byte) v };
    }

    /**
     * Wraps encoded blocks in an MP4 file.
     * <p>
     * 부호화된 블록을 MP4 파일에 담는다.
     *
     * @param enc      encoder that holds the blocks / 블록을 가진 인코더
     * @param builder  MP4 builder / MP4 생성기
     * @return file / 파일
     */
    private static byte[] mp4(AacTestEncoder enc, Mp4TestBuilder builder) {
        List<byte[]> samples = new ArrayList<byte[]>(enc.blocks);
        return builder.build(samples);
    }

    /**
     * An M4A file gives the same samples as the ADTS stream; the edit list removes the priming samples and fixes the length.
     * <p>
     * M4A 파일은 ADTS 스트림과 같은 샘플을 주며 편집 목록이 프라이밍 샘플을 없애고 길이를 정한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMp4Container() throws Exception {
        double[][] x = tones(30000, 44100, new double[][] { { 440, 3000 }, { 1000, 5200 } });
        AacTestEncoder enc = new AacTestEncoder(4, 2);
        byte[] adts = enc.encodeAdts(x);
        short[] ref = decode(adts);
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.config = asc(4, 2);
        short[] plain = decode(mp4(enc, b));
        Check.isTrue(Arrays.equals(ref, plain), "an MP4 file decodes like the ADTS stream");
        b = new Mp4TestBuilder();
        b.config = asc(4, 2);
        b.editStart = 1024;
        b.editDuration = 500;
        PcmSource s = new AacDecoder().open(mp4(enc, b), DecodeLimits.DEFAULT);
        Check.equal(2, s.getChannels(), "channels");
        Check.equal(44100, s.getSampleRate(), "rate");
        Check.equal(44100L * 500 / 1000, s.getFrameCount(), "the edit duration gives the length");
        short[] cut = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(22050L * 2, (long) cut.length, "decoded length");
        boolean same = true;
        for (int i = 0; i < cut.length && same; i++) { // 편집 시작 1024 샘플을 건너뛴 값과 같아야 한다
            same = cut[i] == ref[2 * 1024 + i];
        }
        Check.isTrue(same, "the edit list skips the priming samples");
        double s2 = snr(x[0], cut, 2, 0, 1024, 3072, 20000);
        Check.isTrue(s2 > 35, "SNR through the MP4 path " + s2);
    }

    /**
     * A fragmented MP4 file (moof/trun) decodes like the plain one.
     * <p>
     * 조각화된 MP4 파일(moof/trun)은 일반 파일과 같게 해독된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmentedMp4() throws Exception {
        double[][] x = tones(30000, 44100, new double[][] { { 440 }, { 1000 } });
        AacTestEncoder enc = new AacTestEncoder(4, 2);
        enc.encodeAdts(x);
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.config = asc(4, 2);
        short[] plain = decode(mp4(enc, b));
        b = new Mp4TestBuilder();
        b.config = asc(4, 2);
        b.fragmented = true;
        short[] frag = decode(mp4(enc, b));
        Check.isTrue(plain.length > 0 && Arrays.equals(plain, frag), "fragmented and plain files give the same samples");
    }

    /**
     * Seeking decodes one block before the target so that the samples equal the continuous decode.
     * <p>
     * 탐색은 목표 한 블록 앞부터 해독하므로 샘플이 연속 해독과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeek() throws Exception {
        double[][] x = tones(40000, 44100, new double[][] { { 440, 1900 }, { 880 } });
        AacTestEncoder enc = new AacTestEncoder(4, 2);
        enc.sequences = new int[] { 0, 0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] file = enc.encodeAdts(x);
        short[] all = decode(file);
        PcmSource s = new AacDecoder().open(file, DecodeLimits.DEFAULT);
        Check.isTrue(s.isSeekable(), "seekable");
        long[] targets = { 0, 1000, 2049, 12345, 30000 };
        for (int t = 0; t < targets.length; t++) { // 목표마다 탐색한 뒤 이어서 읽은 값을 비교한다
            s.seekToFrame(targets[t]);
            byte[] buf = new byte[4096 * 4];
            int got = 0;
            while (got < buf.length) { // 버퍼가 찰 때까지 읽는다
                int r = s.read(buf, got, buf.length - got);
                if (r < 0) {
                    break;
                }
                got += r;
            }
            short[] part = AudioKit.toShorts(Arrays.copyOf(buf, got));
            boolean same = part.length > 0;
            for (int i = 0; i < part.length && same; i++) { // 샘플을 하나씩 비교한다
                same = part[i] == all[(int) targets[t] * 2 + i];
            }
            Check.isTrue(same, "samples after seeking to " + targets[t]);
        }
        s.close();
    }

    /**
     * HE-AAC signalling: the AAC-LC core is decoded at the core sampling frequency; other object types are unsupported.
     * <p>
     * HE-AAC 표시: AAC-LC 핵심을 핵심 샘플링 주파수로 해독하며 다른 객체 종류는 지원하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeAacAndUnsupportedConfigurations() throws Exception {
        double[][] x = tones(20000, 24000, new double[][] { { 440 } });
        AacTestEncoder enc = new AacTestEncoder(6, 1);
        enc.encodeAdts(x);
        // 명시적 SBR 표시: 객체 5, 핵심 24 kHz(6), 모노(1), 확장 48 kHz(3), 객체 2, 프레임 길이 플래그 등.
        long bits = 0;
        bits = (bits << 5) | 5;
        bits = (bits << 4) | 6;
        bits = (bits << 4) | 1;
        bits = (bits << 4) | 3;
        bits = (bits << 5) | 2;
        bits = (bits << 3) | 0;
        int total = 25;
        byte[] sbr = new byte[4];
        bits <<= (32 - total);
        for (int i = 0; i < 4; i++) { // 비트열을 바이트로 나눈다
            sbr[i] = (byte) (bits >> (24 - 8 * i));
        }
        Mp4TestBuilder b = new Mp4TestBuilder();
        b.config = sbr;
        b.sampleRate = 48000;
        b.timescale = 48000;
        b.sampleDuration = 2048;
        b.channels = 1;
        PcmSource s = new AacDecoder().open(mp4(enc, b), DecodeLimits.DEFAULT);
        Check.equal(24000, s.getSampleRate(), "the core sampling frequency");
        short[] pcm = AudioKit.toShorts(AudioKit.readAll(s));
        Check.isTrue(pcm.length > 10000, "the core decodes");
        final Mp4TestBuilder ltp = new Mp4TestBuilder();
        ltp.config = new byte[] { (byte) ((4 << 3) | 1), (byte) (0x20 | (1 << 3)) };
        final AacTestEncoder fenc = enc;
        MediaException e = (MediaException) Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens a stream signalling LTP. / LTP 를 표시한 스트림을 연다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                new AacDecoder().open(mp4(fenc, ltp), DecodeLimits.DEFAULT);
            }
        }, "LTP");
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "LTP is unsupported");
    }

    /**
     * Damaged and truncated input does not hang and ends cleanly.
     * <p>
     * 손상되었거나 잘린 입력은 멈추지 않고 정상적으로 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidInput() throws Exception {
        final byte[] noise = new byte[5000];
        new Random(5).nextBytes(noise);
        Check.isFalse(new AacDecoder().accepts(noise), "random bytes are not AAC");
        Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens random bytes. / 무작위 바이트를 연다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                new AacDecoder().open(noise, DecodeLimits.DEFAULT);
            }
        }, "open random bytes");
        double[][] x = tones(20000, 44100, new double[][] { { 600 }, { 900 } });
        byte[] file = new AacTestEncoder(4, 2).encodeAdts(x);
        short[] cut = decode(Arrays.copyOf(file, file.length - 150));
        Check.isTrue(cut.length > 20000, "a truncated stream decodes up to the cut");
        byte[] broken = file.clone();
        Random r = new Random(11);
        for (int i = 0; i < 300; i++) { // 데이터 곳곳을 망가뜨려도 멈추지 않는다
            broken[100 + r.nextInt(broken.length - 100)] = (byte) r.nextInt(256);
        }
        Check.isTrue(decode(broken).length > 0, "a damaged stream still decodes");
        Check.isTrue(new AacDecoder().accepts(file), "ADTS is accepted");
        Check.isFalse(new AacDecoder().accepts(new byte[100]), "zeros are not AAC");
    }

    /**
     * An interrupted thread cancels decoding.
     * <p>
     * 인터럽트된 스레드는 해독을 취소한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInterrupt() throws Exception {
        double[][] x = tones(10000, 44100, new double[][] { { 600 } });
        PcmSource s = new AacDecoder().open(new AacTestEncoder(4, 1).encodeAdts(x), DecodeLimits.DEFAULT);
        Thread.currentThread().interrupt();
        try {
            s.read(new byte[4096], 0, 4096);
            Check.fail("read must fail when interrupted");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CANCELLED, e.getKind(), "kind");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Format name and MIME types.
     * <p>
     * 형식 이름과 MIME 타입.
     */
    public void testIdentity() {
        AacDecoder d = new AacDecoder();
        Check.equal("aac", d.getFormatName(), "format name");
        Check.isTrue(Arrays.asList(d.getMimeTypes()).contains("audio/aac"), "audio/aac");
        Check.isTrue(Arrays.asList(d.getMimeTypes()).contains("audio/mp4"), "audio/mp4");
    }
}
