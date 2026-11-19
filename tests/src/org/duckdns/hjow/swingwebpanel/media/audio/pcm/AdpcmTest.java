package org.duckdns.hjow.swingwebpanel.media.audio.pcm;

import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit.Out;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the IMA and Microsoft ADPCM decoders: hand-computed vectors, an independent reference decoder, an encoder round trip,
 * channel layouts, partial last blocks and exact seeking.
 * <p>
 * IMA 와 Microsoft ADPCM 디코더 시험: 손으로 계산한 값, 독립적인 기준 디코더, 인코더 왕복, 채널 배치, 불완전한 마지막 블록, 정확한 탐색.
 */
public class AdpcmTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AdpcmTest() {
    }

    /**
     * Builds the format chunk body of an IMA ADPCM file.
     * <p>
     * IMA ADPCM 파일의 fmt 청크 본문을 만든다.
     *
     * @param channels   channels / 채널 수
     * @param blockAlign bytes per block / 블록의 바이트 수
     * @param spb        samples per block / 블록의 샘플 수
     * @return chunk body / 청크 본문
     */
    private static byte[] imaFmt(int channels, int blockAlign, int spb) {
        Out o = new Out();
        o.le16(0x11).le16(channels).le32(8000).le32(8000 * blockAlign / spb).le16(blockAlign).le16(4).le16(2).le16(spb);
        return o.toByteArray();
    }

    /**
     * Builds the format chunk body of a Microsoft ADPCM file with the standard coefficients.
     * <p>
     * 표준 계수를 가진 Microsoft ADPCM 파일의 fmt 청크 본문을 만든다.
     *
     * @param channels   channels / 채널 수
     * @param blockAlign bytes per block / 블록의 바이트 수
     * @param spb        samples per block / 블록의 샘플 수
     * @return chunk body / 청크 본문
     */
    private static byte[] msFmt(int channels, int blockAlign, int spb) {
        Out o = new Out();
        o.le16(2).le16(channels).le32(8000).le32(8000 * blockAlign / spb).le16(blockAlign).le16(4).le16(32).le16(spb).le16(7);
        int[] c = { 256, 0, 512, -256, 0, 0, 192, 64, 240, 0, 460, -208, 392, -232 };
        // 일곱 쌍의 표준 계수를 쓴다.
        for (int v : c) {
            o.le16(v);
        }
        return o.toByteArray();
    }

    /**
     * Hand-computed IMA ADPCM block: header predictor 0 and index 0, nibbles 7, 0, F give 11, 13, -12.
     * <p>
     * 손으로 계산한 IMA ADPCM 블록: 헤더 예측값 0, 인덱스 0, 니블 7, 0, F 는 11, 13, -12 가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImaHandComputed() throws Exception {
        // 7: diff = 0 + 1 + 3 + 7 = 11, index 0 + 8 = 8 (step 16); 0: diff = 16 >> 3 = 2 -> 13, index 7 (step 14);
        // F: diff = 1 + 3 + 7 + 14 = 25, negative -> -12, index 15 (step 31); then zeros: -9 (31>>3=3), -6 (28), -3 (25), -1 (23>>3=2), 1 (21>>3=2).
        Out block = new Out();
        block.le16(0).u8(0).u8(0).bytes(new byte[] { 0x07, 0x0F, 0, 0 });
        PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(imaFmt(1, 8, 9), block.toByteArray(), 8), null, null);
        Check.equal(9, s.getFrameCount(), "one complete block of nine frames");
        short[] got = AudioKit.toShorts(AudioKit.readAll(s));
        short[] expected = { 0, 11, 13, -12, -9, -6, -3, -1, 1 };
        // 손으로 계산한 아홉 프레임과 하나씩 비교한다.
        for (int i = 0; i < expected.length; i++) {
            Check.equal(expected[i], got[i], "frame " + i);
        }
    }

    /**
     * Hand-computed Microsoft ADPCM block: samples 50, 100 then nibbles 2 and 14 (-2) with delta 16.
     * <p>
     * 손으로 계산한 Microsoft ADPCM 블록: 샘플 50, 100 과 델타 16 의 니블 2, 14(-2).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMsHandComputed() throws Exception {
        Out block = new Out();
        block.u8(0).le16(16).le16(100).le16(50).u8(0x2E);
        // 예측: (100 * 256 + 50 * 0) / 256 = 100, +2*16 = 132; 델타는 max(16, 230 * 16 >> 8 = 14) = 16;
        // 다음 예측: (132 * 256) / 256 = 132, -2*16 = 100.
        PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(msFmt(1, 8, 4), block.toByteArray(), 8), null, null);
        short[] got = AudioKit.toShorts(AudioKit.readAll(s));
        Check.equal(4, got.length, "frames");
        Check.equal(50, got[0], "sample2 comes first");
        Check.equal(100, got[1], "then sample1");
        Check.equal(132, got[2], "first nibble");
        Check.equal(100, got[3], "second nibble");
    }

    /**
     * Encodes IMA ADPCM blocks with a simple reference encoder written for the test.
     * <p>
     * 시험용으로 작성한 단순 기준 인코더로 IMA ADPCM 블록을 인코딩한다.
     *
     * @param pcm       interleaved samples of all channels / 모든 채널의 인터리브 샘플
     * @param channels  channels / 채널 수
     * @param spb       samples per block (1 + a multiple of 8) / 블록당 샘플 수(1 + 8의 배수)
     * @param outDecode receives the samples that a correct decoder must produce, same layout as the input / 올바른 디코더가 만들어야 하는 샘플을 받는다. 입력과 같은 배치
     * @return the data chunk body / data 청크 본문
     */
    private static byte[] imaEncode(short[] pcm, int channels, int spb, short[] outDecode) {
        int[] steps = stepTable();
        int[] adj = { -1, -1, -1, -1, 2, 4, 6, 8 };
        int frames = pcm.length / channels;
        Out o = new Out();
        // 블록 단위로 인코딩한다.
        for (int base = 0; base + spb <= frames; base += spb) {
            int[] pred = new int[channels];
            int[] idx = new int[channels];
            // 블록 헤더: 채널마다 첫 샘플과 스텝 인덱스(0).
            for (int c = 0; c < channels; c++) {
                pred[c] = pcm[base * channels + c];
                o.le16(pred[c]).u8(0).u8(0);
                outDecode[base * channels + c] = (short) pred[c];
            }
            // 8프레임 묶음마다 채널별 4바이트씩 쓴다.
            for (int g = 0; g < (spb - 1) / 8; g++) {
                for (int c = 0; c < channels; c++) {
                    int[] nibs = new int[8];
                    // 묶음의 샘플 8개를 니블로 인코딩한다.
                    for (int k = 0; k < 8; k++) {
                        int t = base + 1 + g * 8 + k;
                        int diff = pcm[t * channels + c] - pred[c];
                        int nib = 0;
                        if (diff < 0) {
                            nib = 8;
                            diff = -diff;
                        }
                        int step = steps[idx[c]];
                        int tmp = step;
                        if (diff >= tmp) {
                            nib |= 4;
                            diff -= tmp;
                        }
                        tmp >>= 1;
                        if (diff >= tmp) {
                            nib |= 2;
                            diff -= tmp;
                        }
                        tmp >>= 1;
                        if (diff >= tmp) {
                            nib |= 1;
                        }
                        int d = step >> 3;
                        if ((nib & 1) != 0) {
                            d += step >> 2;
                        }
                        if ((nib & 2) != 0) {
                            d += step >> 1;
                        }
                        if ((nib & 4) != 0) {
                            d += step;
                        }
                        pred[c] += (nib & 8) != 0 ? -d : d;
                        pred[c] = Math.max(-32768, Math.min(32767, pred[c]));
                        idx[c] = Math.max(0, Math.min(88, idx[c] + adj[nib & 7]));
                        nibs[k] = nib;
                        outDecode[t * channels + c] = (short) pred[c];
                    }
                    // 니블 두 개를 한 바이트(낮은 니블이 먼저)로 합친다.
                    for (int k = 0; k < 4; k++) {
                        o.u8(nibs[2 * k] | (nibs[2 * k + 1] << 4));
                    }
                }
            }
        }
        return o.toByteArray();
    }

    /**
     * Returns the 89-entry IMA step table, checked for its defining properties.
     * <p>
     * 89개 IMA 스텝 표를 반환하며 정의하는 성질을 확인한다.
     *
     * @return step table / 스텝 표
     */
    private static int[] stepTable() {
        int[] t = AdpcmSource.STEPS.clone();
        Check.equal(89, t.length, "89 step sizes");
        Check.equal(7, t[0], "first step");
        Check.equal(32767, t[88], "last step");
        // 단계가 항상 늘어나고 한 단계의 비율이 약 10%인지 확인한다.
        for (int i = 1; i < t.length; i++) {
            Check.isTrue(t[i] > t[i - 1] && t[i] <= t[i - 1] * 1.2 + 1, "step " + i + " grows by about 10%");
        }
        return t;
    }

    /**
     * The decoder reproduces exactly what the reference encoder's own decoder state says, for mono and stereo.
     * <p>
     * 디코더는 모노와 스테레오 모두에서 기준 인코더 자신의 디코더 상태가 말하는 값을 그대로 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImaAgainstReferenceEncoder() throws Exception {
        Random r = new Random(21);
        // 모노와 스테레오를 각각 시험한다.
        for (int channels = 1; channels <= 2; channels++) {
            int spb = 1 + 8 * 31;
            int blocks = 6;
            int frames = spb * blocks;
            short[] pcm = new short[frames * channels];
            // 완만하게 변하는 신호에 잡음을 더해 만든다.
            for (int i = 0; i < frames; i++) {
                for (int c = 0; c < channels; c++) {
                    pcm[i * channels + c] = (short) (12000 * Math.sin(i * 0.05 * (c + 1)) + r.nextInt(400) - 200);
                }
            }
            short[] expected = new short[pcm.length];
            byte[] data = imaEncode(pcm, channels, spb, expected);
            int blockAlign = 4 * channels + (spb - 1) / 2 * channels;
            PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(imaFmt(channels, blockAlign, spb), data, data.length), null, null);
            Check.equal(frames, s.getFrameCount(), "frame count, channels " + channels);
            short[] got = AudioKit.toShorts(AudioKit.readAll(s));
            Check.equal(expected.length, got.length, "sample count, channels " + channels);
            // 샘플마다 기준 상태와 같은지 확인하고 원본에 가까운지도 본다.
            long err = 0;
            for (int i = 0; i < got.length; i++) {
                Check.equal(expected[i], got[i], "sample " + i + " channels " + channels);
                err += Math.abs(got[i] - pcm[i]);
            }
            Check.isTrue(err / got.length < 600, "ADPCM stays close to the input: mean error " + err / got.length);
            s.seekToFrame(spb * 2 + 100);
            short[] tail = AudioKit.toShorts(AudioKit.readAll(s));
            int from = (spb * 2 + 100) * channels;
            Check.equal(got.length - from, tail.length, "tail length after seek");
            // 탐색한 뒤의 샘플이 처음부터 읽은 값과 같은지 확인한다.
            for (int i = 0; i < tail.length; i++) {
                Check.equal(got[from + i], tail[i], "after seek " + i);
            }
        }
    }

    /**
     * A last block that is shorter than the others is decoded as far as it is complete.
     * <p>
     * 다른 블록보다 짧은 마지막 블록은 완전한 부분까지 디코딩한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPartialLastBlock() throws Exception {
        int spb = 17;
        short[] pcm = new short[spb * 2];
        // 간단한 톱니파로 두 블록 분량을 만든다.
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) ((i % 17) * 300);
        }
        byte[] data = imaEncode(pcm, 1, spb, new short[pcm.length]);
        int blockAlign = 4 + 8;
        byte[] cut = new byte[blockAlign + 4 + 4];
        System.arraycopy(data, 0, cut, 0, cut.length);
        PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(imaFmt(1, blockAlign, spb), cut, cut.length), null, null);
        Check.equal(17 + 1 + 8, s.getFrameCount(), "a full block, the header sample and one group of the partial block");
        Check.equal((17 + 9) * 2, AudioKit.readAll(s).length, "bytes delivered");
    }

    /**
     * A Microsoft ADPCM stream of random nibbles decodes to the same values as a straightforward reference decoder, for mono and stereo.
     * <p>
     * 임의의 니블로 된 Microsoft ADPCM 스트림이 모노와 스테레오 모두에서 단순한 기준 디코더와 같은 값으로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMsAgainstReferenceDecoder() throws Exception {
        Random r = new Random(22);
        int[] coefs = { 256, 0, 512, -256, 0, 0, 192, 64, 240, 0, 460, -208, 392, -232 };
        int[] adapt = { 230, 230, 230, 230, 307, 409, 512, 614, 768, 614, 512, 409, 307, 230, 230, 230 };
        // 모노와 스테레오를 각각 시험한다.
        for (int channels = 1; channels <= 2; channels++) {
            int blockAlign = 7 * channels + 60 * channels;
            int spb = 2 + 60 * 2;
            int blocks = 5;
            Out data = new Out();
            short[] expected = new short[blocks * spb * channels];
            // 블록마다 임의의 헤더와 니블을 만들고 기준 디코더로 기대값을 계산한다.
            for (int b = 0; b < blocks; b++) {
                int[] pi = new int[channels];
                int[] delta = new int[channels];
                int[] s1 = new int[channels];
                int[] s2 = new int[channels];
                // 채널마다 헤더 필드를 만든다.
                for (int c = 0; c < channels; c++) {
                    pi[c] = r.nextInt(7);
                    data.u8(pi[c]);
                }
                // 채널마다 예측기 번호를 쓴다.
                for (int c = 0; c < channels; c++) {
                    delta[c] = 16 + r.nextInt(500);
                    data.le16(delta[c]);
                }
                // 채널마다 델타를 쓴다.
                for (int c = 0; c < channels; c++) {
                    s1[c] = r.nextInt(20000) - 10000;
                    data.le16(s1[c]);
                }
                // 채널마다 첫 샘플 값을 쓴다.
                for (int c = 0; c < channels; c++) {
                    s2[c] = r.nextInt(20000) - 10000;
                    data.le16(s2[c]);
                }
                // 채널마다 직전 두 샘플을 쓴다.
                for (int c = 0; c < channels; c++) {
                    expected[(b * spb) * channels + c] = (short) s2[c];
                    expected[(b * spb + 1) * channels + c] = (short) s1[c];
                }
                int nibbles = (spb - 2) * channels;
                int[] nib = new int[nibbles + 1];
                // 니블을 만들어 바이트 쌍으로 쓰고 기대 샘플을 복원한다.
                for (int i = 0; i < nibbles; i++) {
                    nib[i] = r.nextInt(16);
                }
                // 니블을 두 개씩 한 바이트로 쓴다.
                for (int i = 0; i < nibbles; i += 2) {
                    data.u8((nib[i] << 4) | nib[i + 1]);
                }
                // 니블마다 기대 샘플을 복원한다.
                for (int i = 0; i < nibbles; i++) {
                    int c = i % channels;
                    int sn = nib[i] >= 8 ? nib[i] - 16 : nib[i];
                    int p = (s1[c] * coefs[2 * pi[c]] + s2[c] * coefs[2 * pi[c] + 1]) / 256 + sn * delta[c];
                    p = Math.max(-32768, Math.min(32767, p));
                    expected[(b * spb + 2 + i / channels) * channels + c] = (short) p;
                    s2[c] = s1[c];
                    s1[c] = p;
                    delta[c] = Math.max(16, delta[c] * adapt[nib[i]] / 256);
                }
            }
            byte[] body = data.toByteArray();
            PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(msFmt(channels, blockAlign, spb), body, body.length), null, null);
            short[] got = AudioKit.toShorts(AudioKit.readAll(s));
            Check.equal(expected.length, got.length, "sample count, channels " + channels);
            // 모든 샘플이 기준 디코더와 같은지 확인한다.
            for (int i = 0; i < got.length; i++) {
                Check.equal(expected[i], got[i], "sample " + i + " channels " + channels);
            }
            s.seekToFrame(spb + 5);
            short[] tail = AudioKit.toShorts(AudioKit.readAll(s));
            Check.equal(got.length - (spb + 5) * channels, tail.length, "tail length after seek");
            Check.equal(got[(spb + 5) * channels], tail[0], "first sample after seek");
        }
    }

    /**
     * Invalid ADPCM headers (step index, predictor index, tiny blocks) fail with CORRUPT.
     * <p>
     * 올바르지 않은 ADPCM 헤더(스텝 인덱스, 예측기 번호, 너무 작은 블록)는 CORRUPT 로 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidAdpcm() throws Exception {
        Out bad = new Out();
        bad.le16(0).u8(200).u8(0).bytes(new byte[] { 0, 0, 0, 0 });
        PcmSource s = AudioDecoders.open(AudioKit.wavFromFmt(imaFmt(1, 8, 9), bad.toByteArray(), 8), null, null);
        try {
            AudioKit.readAll(s);
            Check.fail("a step index above 88 must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "invalid step index");
        }
        Out badMs = new Out();
        badMs.u8(9).le16(16).le16(0).le16(0).u8(0);
        PcmSource m = AudioDecoders.open(AudioKit.wavFromFmt(msFmt(1, 8, 4), badMs.toByteArray(), 8), null, null);
        try {
            AudioKit.readAll(m);
            Check.fail("a predictor index beyond the table must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "invalid predictor index");
        }
        try {
            AudioDecoders.open(AudioKit.wavFromFmt(imaFmt(1, 3, 1), new byte[3], 3), null, null);
            Check.fail("a block smaller than its header must be rejected");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "tiny block");
        }
    }
}
