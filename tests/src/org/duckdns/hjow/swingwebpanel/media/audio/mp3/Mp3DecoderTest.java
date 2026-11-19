package org.duckdns.hjow.swingwebpanel.media.audio.mp3;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the MP3 decoder (plan item P18-07, FEATURE_MATRIX AUD-04). The streams come from {@link Mp3TestEncoder}, a Layer III encoder
 * written from the standard's analysis side without using decoder code: tones are encoded, decoded and compared after finding the delay.
 * No real-world MP3 files are available to the tests (see docs/progress/media-aud-b.md).
 * <p>
 * MP3 디코더 시험(계획 P18-07, FEATURE_MATRIX AUD-04). 스트림은 디코더 코드를 쓰지 않고 표준의 분석 쪽에서 작성한 {@link Mp3TestEncoder} 가
 * 만든다: 음을 부호화해 디코딩한 뒤 지연을 찾아 비교한다. 실제 MP3 파일은 시험에 쓰지 못했다(docs/progress/media-aud-b.md 참조).
 */
public class Mp3DecoderTest {

    /**
     * Creates the suite (required by the runner). / 스위트를 만든다(실행기가 필요로 함).
     */
    public Mp3DecoderTest() {
    }

    /**
     * Makes a signal: a sum of sines per channel.
     * <p>
     * 신호를 만든다: 채널마다 사인파의 합.
     *
     * @param n     samples per channel / 채널당 샘플 수
     * @param rate  sampling frequency / 샘플링 주파수
     * @param freqs frequencies per channel / 채널별 주파수
     * @param amps  amplitudes per channel / 채널별 진폭
     * @return signal [channel][n] / 신호
     */
    static double[][] tones(int n, int rate, double[][] freqs, double[][] amps) {
        double[][] x = new double[freqs.length][n];
        for (int c = 0; c < freqs.length; c++) { // 채널마다 사인파를 더한다
            for (int t = 0; t < freqs[c].length; t++) {
                for (int i = 0; i < n; i++) {
                    x[c][i] += amps[c][t] * Math.sin(2 * Math.PI * freqs[c][t] * i / rate) * envelope(i, n);
                }
            }
        }
        return x;
    }

    /**
     * Smooth fade in and fade out so that the test signals have no hard edges that would need many bits.
     * <p>
     * 시험 신호에 많은 비트가 필요한 급격한 가장자리가 없도록 하는 부드러운 페이드 인/아웃.
     *
     * @param i position / 위치
     * @param n length / 길이
     * @return gain 0..1 / 이득
     */
    static double envelope(int i, int n) {
        int ramp = 3000;
        double a = Math.min(1.0, Math.min(i, n - 1 - i) / (double) ramp);
        return Math.sin(a * Math.PI / 2) * Math.sin(a * Math.PI / 2);
    }

    /**
     * Decodes a whole file to interleaved 16-bit samples.
     * <p>
     * 파일 전체를 인터리브된 16비트 샘플로 해독한다.
     *
     * @param file MP3 data / MP3 데이터
     * @return samples / 샘플
     * @throws MediaException on a decoding error / 해독 오류
     */
    static short[] decode(byte[] file) throws MediaException {
        PcmSource s = new Mp3Decoder().open(file, DecodeLimits.DEFAULT);
        try {
            return AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
    }

    /**
     * Finds the delay of the decoder output relative to the input and returns the SNR in dB of one channel.
     * <p>
     * 입력에 대한 디코더 출력의 지연을 찾고 한 채널의 SNR(dB)을 반환한다.
     *
     * @param orig     input / 입력
     * @param dec      decoded interleaved samples / 디코딩된 인터리브 샘플
     * @param channels number of channels / 채널 수
     * @param ch       channel / 채널
     * @return SNR in dB / SNR(dB)
     */
    static double snr(double[] orig, short[] dec, int channels, int ch) {
        int n = orig.length;
        int usable = dec.length / channels;
        return snr(orig, dec, channels, ch, 3000, Math.min(n, usable - 3000) - 1);
    }

    /**
     * Same as {@link #snr(double[], short[], int, int)} for an explicit range of the input.
     * <p>
     * {@link #snr(double[], short[], int, int)} 와 같으며 입력의 범위를 직접 지정한다.
     *
     * @param orig     input / 입력
     * @param dec      decoded interleaved samples / 디코딩된 인터리브 샘플
     * @param channels number of channels / 채널 수
     * @param ch       channel / 채널
     * @param from     first input sample / 첫 입력 샘플
     * @param to       end of the range (exclusive, including the delay margin) / 범위의 끝(미포함, 지연 여유 포함)
     * @return SNR in dB / SNR(dB)
     */
    static double snr(double[] orig, short[] dec, int channels, int ch, int from, int to) {
        int best = 0;
        double bestCorr = -1e300;
        for (int d = 1050; d <= 1064; d++) { // 분석+합성 필터의 지연(1057)과 MDCT 겹침 근처의 후보마다 상관을 구한다
            double c = 0;
            for (int i = from; i < from + 6000 && i < to; i++) {
                c += orig[i] * (dec[(i + d) * channels + ch] / 32768.0);
            }
            if (c > bestCorr) {
                bestCorr = c;
                best = d;
            }
        }
        Check.equal(1057, best, "the delay of the analysis and synthesis filter banks plus the MDCT overlap");
        double sig = 0;
        double err = 0;
        for (int i = from; i < to - 1100; i++) { // 최적 지연으로 오차 에너지를 구한다
            double o = orig[i];
            double v = dec[(i + best) * channels + ch] / 32768.0;
            sig += o * o;
            err += (o - v) * (o - v);
        }
        return 10 * Math.log10(sig / Math.max(err, 1e-30));
    }

    /**
     * MPEG-1 stereo, long blocks: two different tones per channel are reconstructed with a high SNR.
     * <p>
     * MPEG-1 스테레오 긴 블록: 채널마다 다른 음이 높은 SNR 로 복원된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStereoLongBlocks() throws Exception {
        int n = 30000;
        double[][] x = tones(n, 44100, new double[][] { { 440, 3000 }, { 1000 } }, new double[][] { { 0.4, 0.2 }, { 0.5 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 2, 320);
        byte[] file = enc.encode(x);
        short[] pcm = decode(file);
        Check.isTrue(pcm.length >= 2 * n, "decoded length " + pcm.length);
        Check.isTrue(snr(x[0], pcm, 2, 0) > 35, "left SNR " + snr(x[0], pcm, 2, 0));
        Check.isTrue(snr(x[1], pcm, 2, 1) > 35, "right SNR " + snr(x[1], pcm, 2, 1));
    }

    /**
     * MS stereo.
     * <p>
     * MS 스테레오.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMidSide() throws Exception {
        int n = 30000;
        double[][] x = tones(n, 48000, new double[][] { { 700, 5000 }, { 700, 2500 } }, new double[][] { { 0.3, 0.1 }, { 0.3, 0.2 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 48000, 2, 256);
        enc.ms = true;
        short[] pcm = decode(enc.encode(x));
        Check.isTrue(snr(x[0], pcm, 2, 0) > 35, "left SNR " + snr(x[0], pcm, 2, 0));
        Check.isTrue(snr(x[1], pcm, 2, 1) > 35, "right SNR " + snr(x[1], pcm, 2, 1));
    }

    /**
     * Mono at 32 kHz.
     * <p>
     * 32 kHz 모노.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMono() throws Exception {
        int n = 25000;
        double[][] x = tones(n, 32000, new double[][] { { 1234, 8000 } }, new double[][] { { 0.5, 0.1 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 32000, 1, 128);
        PcmSource s = new Mp3Decoder().open(enc.encode(x), DecodeLimits.DEFAULT);
        Check.equal(1, s.getChannels(), "channels");
        Check.equal(32000, s.getSampleRate(), "rate");
        short[] pcm = AudioKit.toShorts(AudioKit.readAll(s));
        Check.isTrue(snr(x[0], pcm, 1, 0) > 35, "mono SNR " + snr(x[0], pcm, 1, 0));
    }

    /**
     * Block switching: long, start, short, stop and long blocks keep the reconstruction (this exercises the short block scale factor bands,
     * the reordering and the three short windows).
     * <p>
     * 블록 전환: 긴 블록, 시작, 짧은 블록, 정지, 긴 블록이 복원을 유지한다(짧은 블록 스케일팩터 대역, 재정렬, 짧은 창 3개를 확인한다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockSwitching() throws Exception {
        int n = 40000;
        double[][] x = tones(n, 44100, new double[][] { { 500, 2200 }, { 900 } }, new double[][] { { 0.3, 0.2 }, { 0.4 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 2, 320);
        enc.blockTypes = new int[] { 0, 0, 0, 0, 1, 2, 2, 2, 3, 0, 0, 1, 2, 3, 0, 0, 0, 1, 2, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        short[] pcm = decode(enc.encode(x));
        Check.isTrue(snr(x[0], pcm, 2, 0) > 30, "left SNR " + snr(x[0], pcm, 2, 0));
        Check.isTrue(snr(x[1], pcm, 2, 1) > 30, "right SNR " + snr(x[1], pcm, 2, 1));
    }

    /**
     * Scale factors (MPEG-1 partitions and scalefac_scale).
     * <p>
     * 스케일팩터(MPEG-1 분할과 scalefac_scale).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScalefactors() throws Exception {
        int n = 25000;
        double[][] x = tones(n, 44100, new double[][] { { 300, 1700, 6000 } }, new double[][] { { 0.2, 0.2, 0.2 } });
        for (int scale = 0; scale < 2; scale++) { // scalefac_scale 0 과 1 을 모두 시험한다
            Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 1, 192);
            enc.scalefactors = true;
            enc.sfScale = scale == 1;
            short[] pcm = decode(enc.encode(x));
            Check.isTrue(snr(x[0], pcm, 1, 0) > 33, "scale " + scale + " SNR " + snr(x[0], pcm, 1, 0));
        }
    }

    /**
     * MPEG-2 (22.05 kHz) and MPEG-2.5 (8 kHz) with the low sampling frequency scale factor syntax.
     * <p>
     * 낮은 샘플링 주파수 스케일팩터 구문을 쓰는 MPEG-2(22.05 kHz)와 MPEG-2.5(8 kHz).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMpeg2AndMpeg25() throws Exception {
        int[] versions = { 2, 2, 25 };
        int[] rates = { 22050, 24000, 8000 };
        for (int k = 0; k < versions.length; k++) { // 버전과 샘플링 주파수마다 시험한다
            int n = 20000;
            double[][] x = tones(n, rates[k], new double[][] { { 440, 1500 } }, new double[][] { { 0.3, 0.2 } });
            Mp3TestEncoder enc = new Mp3TestEncoder(versions[k], rates[k], 1, 128);
            enc.scalefactors = k != 1;
            PcmSource s = new Mp3Decoder().open(enc.encode(x), DecodeLimits.DEFAULT);
            Check.equal(rates[k], s.getSampleRate(), "rate " + k);
            short[] pcm = AudioKit.toShorts(AudioKit.readAll(s));
            Check.isTrue(snr(x[0], pcm, 1, 0) > 30, "version " + versions[k] + " rate " + rates[k] + " SNR " + snr(x[0], pcm, 1, 0));
        }
    }

    /**
     * The bit reservoir: a dense burst after silence needs bits borrowed from earlier frames; without the reservoir the encoder cannot fit it.
     * <p>
     * 비트 저장소: 무음 뒤의 조밀한 구간은 앞 프레임에서 빌린 비트가 필요하며 저장소 없이는 인코더가 담지 못한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBitReservoir() throws Exception {
        int n = 60000;
        double[][] x = new double[1][n];
        Random r = new Random(7);
        double[] freqs = new double[24];
        for (int i = 0; i < freqs.length; i++) { // 조밀한 구간용 사인파 주파수를 정한다
            freqs[i] = 200 + r.nextInt(9000);
        }
        for (int i = 30000; i < 31200; i++) { // 1200 샘플 길이의 조밀한 구간
            double v = 0;
            for (int t = 0; t < freqs.length; t++) {
                v += Math.sin(2 * Math.PI * freqs[t] * i / 44100.0) * 0.03;
            }
            double hann = Math.sin(Math.PI * (i - 30000) / 1200.0);
            x[0][i] = v * hann * hann;
        }
        final Mp3TestEncoder plain = new Mp3TestEncoder(1, 44100, 1, 48);
        plain.maxQuant = 40;
        final double[][] fx = x;
        Check.throwsException(IllegalStateException.class, new Block() {
            /**
             * Encodes without the reservoir. / 저장소 없이 부호화한다.
             */
            public void run() {
                plain.encode(fx);
            }
        }, "the dense burst does not fit a frame");
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 1, 48);
        enc.maxQuant = 40;
        enc.reservoir = true;
        byte[] file = enc.encode(x);
        int max = 0;
        for (int i = 0; i < enc.mainDataBegins.size(); i++) { // 가장 큰 main_data_begin 을 찾는다
            max = Math.max(max, enc.mainDataBegins.get(i).intValue());
        }
        Check.isTrue(max > 0, "some frame uses the reservoir");
        short[] pcm = decode(file);
        double burstSnr = snr(x[0], pcm, 1, 0, 29500, 32500);
        Check.isTrue(burstSnr > 15, "burst SNR " + burstSnr);
    }

    /**
     * An ID3v2 tag before the audio, garbage between frames and an ID3v1 tag after it do not change the decoded audio.
     * <p>
     * 오디오 앞의 ID3v2 태그, 프레임 사이의 쓰레기, 뒤의 ID3v1 태그는 해독된 오디오를 바꾸지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTagsAndResync() throws Exception {
        double[][] x = tones(20000, 44100, new double[][] { { 600 } }, new double[][] { { 0.4 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 1, 128);
        byte[] plain = enc.encode(x);
        short[] ref = decode(plain);
        byte[] tagged = new byte[plain.length + 10 + 300 + 128];
        tagged[0] = 'I';
        tagged[1] = 'D';
        tagged[2] = '3';
        tagged[3] = 3;
        tagged[8] = 2;
        tagged[9] = 0x2C;
        for (int i = 0; i < 300; i++) { // 태그 본문을 헤더처럼 보이는 바이트로 채워도 건너뛴다
            tagged[10 + i] = (byte) (i % 3 == 0 ? 0xFF : 0xE0);
        }
        System.arraycopy(plain, 0, tagged, 310, plain.length);
        tagged[310 + plain.length] = 'T';
        tagged[310 + plain.length + 1] = 'A';
        tagged[310 + plain.length + 2] = 'G';
        short[] withTags = decode(tagged);
        Check.equal(ref.length, withTags.length, "length with tags");
        Check.isTrue(java.util.Arrays.equals(ref, withTags), "samples with tags");
        byte[] noisy = new byte[plain.length + 37];
        int cut = 0;
        for (int i = 0; i < 5; i++) { // 다섯 번째 프레임의 시작 위치를 구한다
            cut += Mp3Header.parse(plain, cut).frameBytes;
        }
        System.arraycopy(plain, 0, noisy, 0, cut);
        for (int i = 0; i < 37; i++) { // 프레임 사이에 쓰레기를 끼운다
            noisy[cut + i] = (byte) (0x55 + i);
        }
        System.arraycopy(plain, cut, noisy, cut + 37, plain.length - cut);
        short[] resynced = decode(noisy);
        Check.equal(ref.length, resynced.length, "length after resync");
        Check.isTrue(java.util.Arrays.equals(ref, resynced), "samples after resync");
    }

    /**
     * The Info frame with a LAME tag removes the encoder delay and padding and gives the exact length.
     * <p>
     * LAME 태그가 있는 Info 프레임은 인코더 지연과 패딩을 없애고 정확한 길이를 알려 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInfoFrameAndLameTag() throws Exception {
        double[][] x = tones(20000, 44100, new double[][] { { 600 } }, new double[][] { { 0.4 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 1, 128);
        byte[] plain = enc.encode(x);
        int frameBytes = 417;
        int frames = 0;
        for (int pos = 0; pos < plain.length; pos += Mp3Header.parse(plain, pos).frameBytes) { // 프레임 수를 센다
            frames++;
        }
        byte[] info = new byte[frameBytes];
        System.arraycopy(plain, 0, info, 0, 4);
        int xo = 4 + 17;
        info[xo] = 'I';
        info[xo + 1] = 'n';
        info[xo + 2] = 'f';
        info[xo + 3] = 'o';
        info[xo + 7] = 1;
        byte[] lame = "LAME3.99r".getBytes("US-ASCII");
        System.arraycopy(lame, 0, info, xo + 8 + 4, lame.length);
        int d = 576;
        int p = 1000;
        int q = xo + 8 + 4 + 21;
        info[q] = (byte) (d >> 4);
        info[q + 1] = (byte) (((d & 15) << 4) | (p >> 8));
        info[q + 2] = (byte) p;
        byte[] all = new byte[info.length + plain.length];
        System.arraycopy(info, 0, all, 0, info.length);
        System.arraycopy(plain, 0, all, info.length, plain.length);
        PcmSource s = new Mp3Decoder().open(all, DecodeLimits.DEFAULT);
        long expected = (long) frames * 1152 - (d + 529) - (p - 529);
        Check.equal(expected, s.getFrameCount(), "exact length from the LAME tag");
        short[] pcm = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(expected, (long) pcm.length, "decoded length");
        double[][] x2 = x;
        Check.isTrue(snr(x2[0], decodeShift(pcm), 1, 0) > 30, "SNR with the delay removed");
    }

    /**
     * Puts the removed 1105 sample delay back as leading zeros so that {@link #snr} can search the delay normally.
     * <p>
     * 지연 탐색이 평소처럼 되도록 제거된 지연 구간 길이만큼 앞에 0 을 붙인다.
     *
     * @param pcm samples / 샘플
     * @return shifted samples / 밀린 샘플
     */
    private static short[] decodeShift(short[] pcm) {
        short[] r = new short[pcm.length + 1105];
        System.arraycopy(pcm, 0, r, 1105, pcm.length);
        return r;
    }

    /**
     * Seeking decodes frames before the target to restore the state, so the samples after a seek equal the continuous decode.
     * <p>
     * 탐색은 상태를 복원하려고 목표 앞의 프레임을 해독하므로 탐색 뒤의 샘플은 연속 해독과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeek() throws Exception {
        double[][] x = tones(40000, 44100, new double[][] { { 440, 1900 }, { 880 } }, new double[][] { { 0.3, 0.2 }, { 0.4 } });
        Mp3TestEncoder enc = new Mp3TestEncoder(1, 44100, 2, 256);
        enc.reservoir = true;
        byte[] file = enc.encode(x);
        short[] all = decode(file);
        PcmSource s = new Mp3Decoder().open(file, DecodeLimits.DEFAULT);
        long[] targets = { 0, 1000, 12345, 20000, 33000 };
        for (int t = 0; t < targets.length; t++) { // 목표마다 탐색한 뒤 이어서 읽은 값을 비교한다
            s.seekToFrame(targets[t]);
            byte[] buf = new byte[4096 * 4];
            int got = 0;
            while (got < buf.length) { // 반복해서 읽는다
                int r = s.read(buf, got, buf.length - got);
                if (r < 0) {
                    break;
                }
                got += r;
            }
            short[] part = AudioKit.toShorts(java.util.Arrays.copyOf(buf, got));
            boolean same = true;
            for (int i = 0; i < part.length && same; i++) { // 샘플을 비교한다
                same = part[i] == all[(int) targets[t] * 2 + i];
            }
            Check.isTrue(same && part.length > 0, "samples after seeking to " + targets[t]);
        }
        s.close();
    }

    /**
     * Damaged and unsupported input.
     * <p>
     * 손상되었거나 지원하지 않는 입력.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidInput() throws Exception {
        final byte[] noise = new byte[5000];
        new Random(3).nextBytes(noise);
        Check.isFalse(new Mp3Decoder().accepts(noise), "random bytes are not MP3");
        Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens random bytes. / 무작위 바이트를 연다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                new Mp3Decoder().open(noise, DecodeLimits.DEFAULT);
            }
        }, "open random bytes");
        // Layer II 프레임 연속(MPEG-1, 128 kbit/s, 44.1 kHz: 417 바이트)은 인식하지만 지원하지 않는다.
        final byte[] layer2 = new byte[417 * 6];
        for (int i = 0; i < 6; i++) {
            layer2[i * 417] = (byte) 0xFF;
            layer2[i * 417 + 1] = (byte) 0xFD;
            layer2[i * 417 + 2] = (byte) 0x80;
            layer2[i * 417 + 3] = 0x44;
        }
        MediaException e = (MediaException) Check.throwsException(MediaException.class, new Block() {
            /**
             * Opens Layer II data. / Layer II 데이터를 연다.
             *
             * @throws Exception always / 항상
             */
            public void run() throws Exception {
                new Mp3Decoder().open(layer2, DecodeLimits.DEFAULT);
            }
        }, "Layer II");
        Check.equal(MediaException.Kind.UNSUPPORTED_FORMAT, e.getKind(), "Layer II is unsupported, not corrupt");
        double[][] x = tones(15000, 44100, new double[][] { { 600 } }, new double[][] { { 0.4 } });
        byte[] file = new Mp3TestEncoder(1, 44100, 1, 128).encode(x);
        byte[] cut = java.util.Arrays.copyOf(file, file.length - 200);
        short[] pcm = decode(cut);
        Check.isTrue(pcm.length > 10000, "a truncated file decodes up to the cut");
        byte[] broken = file.clone();
        Random rr = new Random(9);
        for (int i = 0; i < 200; i++) { // 데이터 곳곳을 망가뜨려도 멈추지 않는다
            broken[100 + rr.nextInt(broken.length - 100)] = (byte) rr.nextInt(256);
        }
        short[] pcm2 = decode(broken);
        Check.isTrue(pcm2.length > 0, "a damaged file still decodes");
    }

    /**
     * An interrupted thread cancels decoding with a CANCELLED failure.
     * <p>
     * 인터럽트된 스레드는 CANCELLED 실패로 해독을 취소한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInterrupt() throws Exception {
        double[][] x = tones(10000, 44100, new double[][] { { 600 } }, new double[][] { { 0.4 } });
        PcmSource s = new Mp3Decoder().open(new Mp3TestEncoder(1, 44100, 1, 128).encode(x), DecodeLimits.DEFAULT);
        Thread.currentThread().interrupt();
        try {
            s.read(new byte[4096], 0, 4096);
            Check.fail("read must fail when interrupted");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CANCELLED, e.getKind(), "kind");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag stays set");
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
        Mp3Decoder d = new Mp3Decoder();
        Check.equal("mp3", d.getFormatName(), "format name");
        Check.isTrue(java.util.Arrays.asList(d.getMimeTypes()).contains("audio/mpeg"), "audio/mpeg");
    }
}
