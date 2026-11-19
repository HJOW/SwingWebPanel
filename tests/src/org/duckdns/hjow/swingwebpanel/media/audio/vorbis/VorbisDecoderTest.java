package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import java.util.Arrays;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.ChannelMixer;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisBits;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisBook;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.VorbisWriter;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Vorbis decoder (plan item P18-07, FEATURE_MATRIX AUD-06) against an independent test encoder and its model of the
 * decoding: the inverse MDCT, the codebooks, whole streams of many configurations compared sample by sample with the model, the
 * reconstruction of the input, seeking, the trimmed end, multiplexed and chained files and damaged data.
 * <p>
 * 독립적인 시험용 인코더와 그 디코딩 모델을 상대로 한 Vorbis 디코더 시험(계획 P18-07, FEATURE_MATRIX AUD-06): 역 MDCT, 코드북, 여러 설정의 전체
 * 스트림을 모델과 샘플 단위로 비교, 입력의 복원, 탐색, 잘린 끝, 다중화/체인 파일, 손상된 데이터.
 */
public class VorbisDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public VorbisDecoderTest() {
    }

    /**
     * A deterministic test signal: two sines and a little noise per channel, amplitude 0.2.
     * <p>
     * 결정적인 시험 신호: 채널마다 사인파 두 개와 약간의 잡음. 진폭 0.2.
     *
     * @param ch   channels / 채널 수
     * @param n    frames / 프레임 수
     * @param seed seed / 시드
     * @return the signal / 신호
     */
    static float[][] signal(int ch, int n, long seed) {
        Random r = new Random(seed);
        float[][] x = new float[ch][n];
        // 채널마다 주파수가 다른 사인파에 잡음을 더한다.
        for (int c = 0; c < ch; c++) {
            for (int i = 0; i < n; i++) {
                x[c][i] = (float) (0.2 * (0.5 * Math.sin(i * 0.03 * (c + 1)) + 0.35 * Math.sin(i * 0.41 + c) + 0.15 * (r.nextDouble() * 2 - 1)));
            }
        }
        return x;
    }

    /**
     * Converts a model to the 16-bit interleaved samples that the decoder should deliver (mixing more than two channels).
     * <p>
     * 모델을 디코더가 내야 할 16비트 인터리브 샘플로 바꾼다(채널이 둘보다 많으면 믹스).
     *
     * @param model the model per channel / 채널마다의 모델
     * @return the samples / 샘플
     */
    static short[] expected(float[][] model) {
        int ch = model.length;
        int n = model[0].length;
        int outCh = Math.min(ch, 2);
        short[] s = new short[n * outCh];
        // 채널 순서는 Vorbis I 규격 4.3.9절의 순서(3채널이면 왼쪽, 중앙, 오른쪽)이다.
        ChannelMixer mixer = ch > 2 ? ChannelMixer.forVorbisOrder(ch) : null;
        float[] frame = new float[ch];
        float[] mixed = new float[2];
        // 프레임마다 16비트로 바꾼다.
        for (int i = 0; i < n; i++) {
            if (mixer == null) {
                // 항목마다 반복해서 처리한다.
                for (int c = 0; c < ch; c++) {
                    s[i * outCh + c] = PcmConverter.clip16(model[c][i]);
                }
            } else {
                // 항목마다 반복해서 처리한다.
                for (int c = 0; c < ch; c++) {
                    frame[c] = model[c][i];
                }
                mixer.mix(frame, mixed);
                s[i * 2] = PcmConverter.clip16(mixed[0]);
                s[i * 2 + 1] = PcmConverter.clip16(mixed[1]);
            }
        }
        return s;
    }

    /**
     * Checks that two sample arrays are equal within a tolerance.
     * <p>
     * 두 샘플 배열이 허용 오차 안에서 같은지 확인한다.
     *
     * @param expected expected / 기대값
     * @param got      decoded / 디코딩 값
     * @param tol      tolerance in LSB / 허용 오차(LSB)
     * @param what     description / 설명
     */
    static void assertClose(short[] expected, short[] got, int tol, String what) {
        Check.equal(expected.length, got.length, what + ": number of samples");
        int worst = 0;
        int at = -1;
        // 샘플마다 차이를 보고 가장 큰 차이를 기록한다.
        for (int i = 0; i < expected.length; i++) {
            int d = Math.abs(expected[i] - got[i]);
            if (d > worst) {
                worst = d;
                at = i;
            }
        }
        Check.isTrue(worst <= tol, what + ": largest difference " + worst + " at sample " + at + " (expected " + (at >= 0 ? expected[at] : 0) + ", got "
                + (at >= 0 ? got[at] : 0) + ")");
    }

    /**
     * The inverse MDCT agrees with the defining sum for every block size.
     * <p>
     * 역 MDCT 는 모든 블록 크기에서 정의식의 합과 일치한다.
     */
    public void testImdctMatchesDefinition() {
        Random r = new Random(1);
        // 블록 크기마다 임의의 스펙트럼으로 FFT 구현과 정의식을 비교한다.
        for (int n = 64; n <= 4096; n *= 2) {
            float[] x = new float[n / 2];
            // 항목마다 반복해서 처리한다.
            for (int k = 0; k < x.length; k++) {
                x[k] = (float) (r.nextDouble() * 2 - 1);
            }
            float[] y = new float[n];
            new Imdct(n).inverse(x, y);
            double maxErr = 0;
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < n; i += (n > 512 ? 7 : 1)) {
                double sum = 0;
                // 항목마다 반복해서 처리한다.
                for (int k = 0; k < n / 2; k++) {
                    sum += x[k] * Math.cos(2 * Math.PI / n * (i + 0.5 + n / 4.0) * (k + 0.5));
                }
                maxErr = Math.max(maxErr, Math.abs(sum - y[i]));
            }
            Check.isTrue(maxErr < 1e-3 * Math.sqrt(n / 64.0), "IMDCT of size " + n + ": error " + maxErr);
        }
    }

    /**
     * Codebooks written by the test (plain, ordered, sparse, with the three lookup types) decode every entry: this checks the
     * assignment of codewords, the tree and the vector lookup against the independent implementation of the test.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCodebooks() throws Exception {
        Random r = new Random(2);
        float[] g3 = { -0.5f, 0f, 0.5f };
        VorbisBook[] books = {
                VorbisBook.flat(128, 7, false, 1),
                VorbisBook.flat(9, 4, true, 2),
                VorbisBook.grid(4, g3, new VorbisBook.Weight() {
                    /**
                     * Weighted by sparsity, entries with four non-zero parts unused.
                     * <p>
                     * 0 이 많을수록 가중치가 크며 네 성분이 모두 0 이 아닌 항목은 쓰지 않는다.
                     *
                     * @param v vector / 벡터
                     * @return weight / 가중치
                     */
                    public double of(float[] v) {
                        int nz = 0;
                        // 항목마다 반복해서 처리한다.
                        for (float f : v) {
                            if (f != 0) {
                                nz++;
                            }
                        }
                        return nz == 4 ? 0 : Math.exp(-1.5 * nz);
                    }
                }),
                VorbisBook.grid(1, new float[] { -0.25f, -0.125f, 0f, 0.125f, 0.25f }, null),
                VorbisBook.table(new float[][] { { 0.1f, -0.2f }, { 0.3f, 0.4f }, { -0.1f, 0.2f }, { 0f, 0f }, { 0.5f, -0.5f } }, new VorbisBook.Weight() {
                    /**
                     * Different weights for different entries.
                     * <p>
                     * 항목마다 다른 가중치.
                     *
                     * @param v vector / 벡터
                     * @return weight / 가중치
                     */
                    public double of(float[] v) {
                        return 1 + Math.abs(v[0]) * 10;
                    }
                }, false) };
        // 코드북마다 쓰고 해석해 모든 항목의 부호어와 벡터를 확인한다.
        for (int b = 0; b < books.length; b++) {
            VorbisBook book = books[b];
            VorbisBits w = new VorbisBits();
            book.write(w);
            int n = 400;
            int[] seq = new int[n];
            // 쓰는 항목 중에서 임의로 골라 부호어를 이어 쓴다.
            for (int i = 0; i < n; i++) {
                int e;
                // 조건을 만족할 때까지 반복해서 처리한다.
                do {
                    e = r.nextInt(book.entries);
                } while (book.lengths[e] == 0);
                seq[i] = e;
            }
            VorbisBits data = new VorbisBits();
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < n; i++) {
                book.writeEntry(data, seq[i]);
            }
            VorbisCodebook parsed = new VorbisCodebook(new VorbisBitReader(w.bytes()));
            Check.equal(book.dims, parsed.dimensions, "dimensions of book " + b);
            Check.equal(book.entries, parsed.entries, "entries of book " + b);
            VorbisBitReader br = new VorbisBitReader(data.bytes());
            float[] vec = new float[Math.max(1, book.dims)];
            // 부호어를 하나씩 해석해 항목과 벡터가 같은지 확인한다.
            for (int i = 0; i < n; i++) {
                if (book.vectors != null) {
                    Check.isTrue(parsed.decodeVector(br, vec, 0), "vector " + i + " of book " + b);
                    // 항목마다 반복해서 처리한다.
                    for (int d = 0; d < book.dims; d++) {
                        Check.near(book.value(seq[i], d), vec[d], 1e-6, "component " + d + " of entry " + seq[i] + " of book " + b);
                    }
                } else {
                    Check.equal(seq[i], parsed.decodeScalar(br), "entry " + i + " of book " + b);
                }
            }
        }
        Check.equal(0.5f, VorbisCodebook.unpack(VorbisBits.pack(0.5f)), "float packing of 0.5");
        Check.equal(-3.25f, VorbisCodebook.unpack(VorbisBits.pack(-3.25f)), "float packing of -3.25");
        Check.equal(0f, VorbisCodebook.unpack(0), "float packing of 0");
        Check.near(1.0 / 2048, VorbisCodebook.unpack(VorbisBits.pack(1f / 2048)), 1e-9, "a small float");
    }

    /**
     * Decodes a whole file through the registry.
     * <p>
     * 파일 전체를 등록부로 디코딩한다.
     *
     * @param file the file / 파일
     * @return the PCM samples / PCM 샘플
     * @throws MediaException on failure / 실패 시
     */
    private static short[] decode(byte[] file) throws MediaException {
        return AudioKit.toShorts(AudioKit.readAll(AudioDecoders.open(file, "audio/mpeg", null)));
    }

    /**
     * Whole streams of many configurations decode to the model of the test encoder, sample by sample: both profiles, residue types 0, 1
     * and 2, mono, coupled stereo, three channels with two submaps, and several block patterns.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStreamsMatchTheModel() throws Exception {
        String[] profiles = { "vq", "fine" };
        int[] types = { 0, 1, 2 };
        String[] patterns = { "L", "S", "LS", "LLSSLSLL", "SSSL" };
        int count = 0;
        // 프로필, 잔차 유형, 블록 패턴을 조합해 모노, 결합 스테레오, 하위 맵 둘의 3채널을 시험한다.
        for (String profile : profiles) {
            for (int type : types) {
                // 항목마다 반복해서 처리한다.
                for (String pattern : patterns) {
                    // 항목마다 반복해서 처리한다.
                    for (int layout = 0; layout < 3; layout++) {
                        if ((count++ % 4) != 0 && !pattern.equals("LLSSLSLL")) {
                            continue;
                        }
                        VorbisWriter.Config cfg = new VorbisWriter.Config();
                        cfg.profile = profile;
                        cfg.residueType = type;
                        cfg.pattern = pattern;
                        cfg.blockShort = 128;
                        cfg.blockLong = 1024;
                        cfg.channels = layout == 0 ? 1 : layout == 1 ? 2 : 3;
                        cfg.couple = layout == 1;
                        cfg.mux = layout == 2 ? new int[] { 0, 1, 0 } : null;
                        cfg.sloped = (count & 1) == 0;
                        cfg.segmentsPerPage = layout == 2 ? 5 : 255;
                        VorbisWriter.Result r = VorbisWriter.encode(signal(cfg.channels, 3300 + count, 10 + count), cfg);
                        String what = profile + " type " + type + " pattern " + pattern + " channels " + cfg.channels;
                        PcmSource s = AudioDecoders.open(r.file, null, null);
                        Check.equal(r.length, s.getFrameCount(), "frame count of " + what);
                        Check.equal(Math.min(cfg.channels, 2), s.getChannels(), "channels of " + what);
                        assertClose(expected(r.model), AudioKit.toShorts(AudioKit.readAll(s)), 2, what);
                    }
                }
            }
        }
    }

    /**
     * With the fine profile the decoded signal reproduces the input: this checks the scaling of the inverse transform and the window
     * independently of the decoder, for long blocks only, for short blocks only and for a mixture.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReconstructionOfTheInput() throws Exception {
        String[] patterns = { "L", "S", "LLSLSSL" };
        // 블록 패턴마다 입력과 디코딩한 신호의 오차를 잰다.
        for (String pattern : patterns) {
            VorbisWriter.Config cfg = new VorbisWriter.Config();
            cfg.profile = "fine";
            cfg.pattern = pattern;
            cfg.blockShort = 256;
            cfg.blockLong = 2048;
            cfg.sloped = false;
            float[][] x = signal(1, 9000, 7);
            VorbisWriter.Result r = VorbisWriter.encode(x, cfg);
            short[] got = decode(r.file);
            Check.equal(9000, got.length, "length for pattern " + pattern);
            double err = 0;
            double sig = 0;
            // 오차와 신호의 제곱 평균을 구한다(처음과 끝의 창 가장자리는 제외한다).
            for (int i = 300; i < 8000; i++) {
                double d = got[i] / 32768.0 - x[0][i];
                err += d * d;
                sig += x[0][i] * x[0][i];
            }
            double snr = 10 * Math.log10(sig / err);
            Check.isTrue(snr > 25, "signal to noise ratio " + snr + " dB for pattern " + pattern);
        }
    }

    /**
     * The end of the stream is trimmed to the granule position: the decoder delivers exactly the number of frames of the input, and a
     * stream of a single sample works.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrimmedEnd() throws Exception {
        int[] lengths = { 1, 5, 99, 1000, 1023, 1024, 1025, 5000 };
        // 길이마다 인코딩해 디코딩한 프레임 수가 입력 길이와 같은지 확인한다.
        for (int len : lengths) {
            VorbisWriter.Config cfg = new VorbisWriter.Config();
            cfg.blockShort = 128;
            cfg.blockLong = 512;
            cfg.pattern = "LS";
            VorbisWriter.Result r = VorbisWriter.encode(signal(1, len, len), cfg);
            short[] got = decode(r.file);
            Check.equal(len, got.length, "frames for length " + len);
            assertClose(expected(r.model), got, 2, "samples for length " + len);
        }
    }

    /**
     * Seeking is exact: after a seek the decoder delivers the same samples as a sequential decode from there, for packets that span
     * pages, pages without a granule position and every kind of position including the ends.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeeking() throws Exception {
        int[] pageSizes = { 255, 7, 2 };
        // 페이지당 세그먼트 수를 바꿔 가며 같은 위치를 탐색한다.
        for (int perPage : pageSizes) {
            VorbisWriter.Config cfg = new VorbisWriter.Config();
            cfg.channels = 2;
            cfg.couple = true;
            cfg.blockShort = 128;
            cfg.blockLong = 1024;
            cfg.pattern = "LLSLSSLL";
            cfg.segmentsPerPage = perPage;
            VorbisWriter.Result r = VorbisWriter.encode(signal(2, 12000, 33), cfg);
            PcmSource s = AudioDecoders.open(r.file, null, null);
            byte[] all = AudioKit.readAll(s);
            Check.equal(12000 * 4, all.length, "all frames, " + perPage + " segments per page");
            long[] targets = { 0, 1, 127, 128, 129, 1000, 4096, 4097, 7777, 11999, 12000, 20000, 5, 9000, 100, 11000 };
            // 위치마다 이동해 읽은 결과가 처음부터 읽은 결과의 해당 부분과 같은지 확인한다.
            for (long t : targets) {
                s.seekToFrame(t);
                byte[] got = AudioKit.readAll(s);
                int from = (int) Math.min(t, 12000) * 4;
                byte[] exp = Arrays.copyOfRange(all, from, all.length);
                Check.equal(true, Arrays.equals(exp, got), "seek to " + t + " with " + perPage + " segments per page");
            }
        }
    }

    /**
     * Streams with other logical streams in front or behind: only the first Vorbis stream plays.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMultiplexedAndChained() throws Exception {
        VorbisWriter.Config cfg = new VorbisWriter.Config();
        cfg.blockShort = 128;
        cfg.blockLong = 512;
        cfg.pattern = "LS";
        cfg.serial = 100;
        VorbisWriter.Result a = VorbisWriter.encode(signal(1, 2500, 41), cfg);
        short[] want = decode(a.file);
        VorbisWriter.Config second = new VorbisWriter.Config();
        second.blockShort = 128;
        second.blockLong = 512;
        second.rate = 22050;
        second.serial = 200;
        VorbisWriter.Result b = VorbisWriter.encode(signal(1, 1000, 42), second);
        byte[] chained = OggMux.concat(a.file, b.file);
        short[] got = decode(chained);
        Check.equal(want.length, got.length, "only the first link of a chain plays");
        assertClose(want, got, 0, "chain");
        PcmSource s = AudioDecoders.open(chained, null, null);
        Check.equal(44100, s.getSampleRate(), "the sample rate of the first link");
        byte[] theora = new byte[] { (byte) 0x80, 't', 'h', 'e', 'o', 'r', 'a', 0, 0, 0, 0, 0 };
        byte[] bos = OggMux.mux(55, 0, new byte[][] { theora }, new long[] { 0 }, 255, true, false);
        byte[] eos = OggMux.mux(55, 1, new byte[][] { new byte[20] }, new long[] { 3 }, 255, false, true);
        short[] muxed = decode(OggMux.concat(bos, a.file, eos));
        assertClose(want, muxed, 0, "a video stream in front");
    }

    /**
     * Invalid headers fail with CORRUPT or UNSUPPORTED_FORMAT, and damaged audio never throws a runtime exception.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedData() throws Exception {
        VorbisWriter.Config cfg = new VorbisWriter.Config();
        cfg.channels = 2;
        cfg.couple = true;
        cfg.blockShort = 128;
        cfg.blockLong = 512;
        cfg.pattern = "LLS";
        cfg.segmentsPerPage = 4;
        VorbisWriter.Result r = VorbisWriter.encode(signal(2, 4000, 51), cfg);
        // 세 헤더 패킷을 하나씩 망가뜨리면 열 때 CORRUPT 가 되어야 한다.
        for (int h = 0; h < 3; h++) {
            byte[][] packets = new byte[3][];
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < 3; i++) {
                packets[i] = r.packets[i].clone();
            }
            packets[h][0] ^= 0x7F;
            byte[] p0 = OggMux.mux(1, 0, new byte[][] { packets[0] }, new long[] { 0 }, 255, true, false);
            byte[] p1 = OggMux.mux(1, 1, new byte[][] { packets[1], packets[2] }, new long[] { 0, 0 }, 255, false, true);
            try {
                AudioKit.readAll(AudioDecoders.open(OggMux.concat(p0, p1), null, null));
                Check.fail("a damaged header packet " + h + " must be rejected");
            } catch (MediaException e) {
                Check.isTrue(e.getKind() == MediaException.Kind.CORRUPT || e.getKind() == MediaException.Kind.UNSUPPORTED_FORMAT, "kind " + e.getKind() + " for header " + h);
            }
        }
        Random rnd = new Random(52);
        // 임의의 바이트를 바꾸거나 잘라 낸 파일은 MediaException 이나 정상 종료만 있어야 한다.
        for (int k = 0; k < 120; k++) {
            byte[] fuzz = r.file.clone();
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < 3; j++) {
                fuzz[rnd.nextInt(fuzz.length)] = (byte) rnd.nextInt(256);
            }
            byte[] use = k % 3 == 0 ? Arrays.copyOf(fuzz, fuzz.length - rnd.nextInt(fuzz.length / 2)) : fuzz;
            try {
                AudioKit.readAll(AudioDecoders.open(use, null, null));
            } catch (MediaException expected) {
                Check.isTrue(expected.getKind() != null, "fuzz " + k);
            }
        }
        byte[] damaged = r.file.clone();
        damaged[damaged.length - 200] ^= 0x55;
        short[] part = decode(damaged);
        Check.isTrue(part.length > 0 && part.length <= 4000 * 2, "a damaged page does not end the stream: " + part.length);
    }
}
