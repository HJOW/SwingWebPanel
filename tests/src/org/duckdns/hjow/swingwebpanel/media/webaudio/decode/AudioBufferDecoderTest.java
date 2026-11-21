package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.reference.ReferenceFixture;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the decoding pipeline of <code>decodeAudioData</code> (plan item P25-05). The files are the ones that real encoders wrote for the audio
 * decoder tests (<code>tests/resources/media/audio</code>: WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM, with the PCM that ffmpeg's own decoders
 * produced as the reference) and synthetic WAV files of sine waves of known frequency. The expectations are the reference PCM of ffmpeg
 * (bit for bit for the lossless formats, through the same fixture index as the decoder tests), the exact conversion <code>sample / 32768</code>,
 * the closed form of a sine at the output times and the length formula <code>ceil(frames * rate / fileRate)</code>.
 * <p>
 * <code>decodeAudioData</code> 의 디코딩 파이프라인 시험(계획 P25-05). 파일은 오디오 디코더 시험이 쓰는, 실제 인코더가 만든 것
 * (<code>tests/resources/media/audio</code>: WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM. ffmpeg 자신의 디코더가 만든 PCM 이 참조)과 주파수를 아는
 * 사인파의 합성 WAV 파일이다. 기대값은 ffmpeg 의 참조 PCM(무손실 형식은 같은 시험 자료 색인을 통해 비트 단위), 정확한 변환 <code>표본 / 32768</code>,
 * 출력 시각에서의 사인의 닫힌 식, 길이 식 <code>ceil(프레임 수 * 표본율 / 파일 표본율)</code> 이다.
 */
public class AudioBufferDecoderTest {

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public AudioBufferDecoderTest() {
    }

    /**
     * Decodes and expects an <code>EncodingError</code> with a cause.
     * <p>
     * 디코딩하고 원인이 붙은 <code>EncodingError</code> 를 기대한다.
     *
     * @param data the data / 데이터
     * @param what the description / 설명
     * @return the error / 오류
     */
    private static AudioException expectEncodingError(byte[] data, String what) {
        try {
            AudioBufferDecoder.decode(data, null, 44100f, null);
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), what + ": the kind of " + e);
            Check.notNull(e.getCause(), what + ": the cause is kept");
            return e;
        }
        Check.fail(what + ": no error");
        return null;
    }

    /**
     * Computes the MD5 of 16 bit samples as lower case hex.
     * <p>
     * 16비트 표본의 MD5 를 소문자 16진수로 구한다.
     *
     * @param s the samples / 표본
     * @return the hex digest / 16진수 요약
     * @throws Exception if MD5 is not available / MD5 를 쓸 수 없으면
     */
    private static String md5(short[] s) throws Exception {
        byte[] h = MessageDigest.getInstance("MD5").digest(AudioKit.toBytes(s));
        StringBuilder b = new StringBuilder();
        // 바이트마다 두 자리 16진수로 적는다.
        for (int i = 0; i < h.length; i++) {
            b.append(Character.forDigit((h[i] >> 4) & 15, 16)).append(Character.forDigit(h[i] & 15, 16));
        }
        return b.toString();
    }

    /**
     * Converts a buffer back to interleaved 16 bit samples (<code>round(x * 32768)</code>), which is exact for what the pipeline makes of
     * 16 bit samples.
     * <p>
     * 버퍼를 인터리브된 16비트 표본(<code>round(x * 32768)</code>)으로 되돌린다. 파이프라인이 16비트 표본으로 만든 값에는 정확하다.
     *
     * @param b the buffer / 버퍼
     * @return the samples / 표본
     */
    private static short[] toShorts(AudioBuffer b) {
        int ch = b.getNumberOfChannels();
        short[] r = new short[b.getLength() * ch];
        // 채널마다 프레임 순서로 인터리브한다.
        for (int c = 0; c < ch; c++) {
            float[] d = b.getChannelData(c);
            // 채널의 모든 표본을 16비트 정수로 되돌린다.
            for (int f = 0; f < d.length; f++) {
                r[f * ch + c] = (short) Math.round(d[f] * 32768f);
            }
        }
        return r;
    }

    /**
     * Every file of the fixture set decodes at its own sample rate to exactly the samples that the media decoder delivers (each sample divided by
     * 32768, nothing else is done to it), with the channels of the decoder and the length announced by the fixture index. For the lossless
     * fixtures that have a reference this is ffmpeg's PCM bit for bit: through the MD5 of the index, or the reference FLAC for the files
     * with more than two channels' mix that have none. All nine families (WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM) take the same path.
     * <p>
     * 시험 자료의 모든 파일은 자기 표본율에서 미디어 디코더가 주는 표본 그대로(표본을 32768 로 나눌 뿐 다른 처리는 없다) 디코딩되고, 채널은 디코더의 채널이며
     * 길이는 시험 자료 색인이 알리는 길이이다. 참조가 있는 무손실 자료는 ffmpeg 의 PCM 과 비트 단위로 같다. 색인의 MD5 로, 또는 MD5 가 없으면 참조 FLAC 으로
     * 확인한다. 아홉 계열(WAV, AIFF, AU, FLAC, MP3, AAC, Vorbis, Opus, WebM) 모두 같은 경로를 쓴다.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testEveryFixtureDecodesToTheSamplesOfTheMediaDecoder() throws Exception {
        List<String> problems = new ArrayList<String>();
        int checked = 0;
        int bitExact = 0;
        // 시험 자료마다 디코딩해 미디어 디코더의 16비트 표본과 비교한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                continue;
            }
            checked++;
            AudioBuffer b = AudioBufferDecoder.decode(f.readFile(), f.mime, f.rate, null);
            int outCh = Math.min(f.channels, 2);
            if (b.getNumberOfChannels() != outCh || b.getSampleRate() != f.rate || b.getLength() != f.expected) {
                problems.add(f.name + ": " + b.getNumberOfChannels() + " channels, " + b.getSampleRate() + " Hz, " + b.getLength() + " frames instead of " + outCh + ", "
                        + f.rate + " Hz, " + f.expected);
                continue;
            }
            short[] ours = toShorts(b);
            if (!Arrays.equals(ours, f.decode(f.mime))) {
                problems.add(f.name + ": the samples differ from the media decoder");
                continue;
            }
            // 무손실 자료는 ffmpeg 의 PCM 과 비트 단위로 같아야 한다.
            if (f.kind.equals("exact") && f.channels <= 2) {
                boolean same = f.isMd5() ? md5(ours).equals(f.md5()) : Arrays.equals(ours, f.readReferencePcm());
                if (!same) {
                    problems.add(f.name + ": differs from the PCM of ffmpeg");
                } else {
                    bitExact++;
                }
            }
        }
        Check.isTrue(checked > 100, "the fixtures were found: " + checked);
        Check.isTrue(bitExact > 30, "the lossless fixtures compared bit for bit: " + bitExact);
        Check.isTrue(problems.isEmpty(), problems.size() + " of " + checked + " fixtures differ:\n" + join(problems));
    }

    /**
     * Joins lines.
     * <p>
     * 줄을 잇는다.
     *
     * @param lines the lines / 줄
     * @return the text / 텍스트
     */
    private static String join(List<String> lines) {
        StringBuilder b = new StringBuilder();
        // 줄마다 줄바꿈과 함께 덧붙인다.
        for (String l : lines) {
            b.append(l).append('\n');
        }
        return b.toString();
    }

    /**
     * The sources of more than two channels are mixed down to stereo by the media decoders and 24 bit samples are rounded to 16 bits inside
     * them, so a 5.1 file gives a stereo buffer: a documented limit of this pipeline (<code>docs/progress/webaudio-b.md</code>), checked here so
     * that the day a decoder interface delivers the original channels this test is changed on purpose.
     *
     * <p>
     * 채널이 둘보다 많은 소스는 미디어 디코더가 스테레오로 다운믹스하고 24비트 표본은 그 안에서 16비트로 반올림하므로 5.1 파일은 스테레오 버퍼가 된다. 이 파이프라인의 문서화한 한계(<code>docs/progress/webaudio-b.md</code>)이며, 디코더 인터페이스가 원래 채널을 주는 날 이 시험을 의도적으로 바꾸도록 여기서 확인한다.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testMultichannelFilesAreMixedDownByTheMediaDecoders() throws Exception {
        int seen = 0;
        // 채널이 셋 이상인 모든 파일이 스테레오 버퍼가 되는지 확인한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused() || f.channels <= 2) {
                continue;
            }
            seen++;
            Check.equal(2, AudioBufferDecoder.decode(f.readFile(), f.mime, f.rate, null).getNumberOfChannels(), f.name + " (" + f.channels + " channels)");
        }
        Check.isTrue(seen >= 8, "the multichannel fixtures were found: " + seen);
    }

    /**
     * The MIME type is only a hint: the same data decodes to the same samples with the right type, with none and with a wrong one (the
     * specification determines the type by sniffing the data).
     *
     * <p>
     * MIME 타입은 힌트일 뿐이다. 같은 데이터가 올바른 타입, 타입 없음, 틀린 타입에서 같은 표본으로 디코딩된다(규격도 데이터를 스니핑하여 타입을 정한다).
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testTheMimeTypeIsOnlyAHint() throws Exception {
        List<String> problems = new ArrayList<String>();
        // 시험 자료마다 올바른 힌트, 힌트 없음, 틀린 힌트로 디코딩해 결과가 같은지 본다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                continue;
            }
            byte[] data = f.readFile();
            short[] right = toShorts(AudioBufferDecoder.decode(data, f.mime, f.rate, null));
            short[] none = toShorts(AudioBufferDecoder.decode(data, null, f.rate, null));
            short[] wrong = toShorts(AudioBufferDecoder.decode(data, f.mime.equals("audio/wav") ? "audio/mpeg" : "audio/wav", f.rate, null));
            if (!Arrays.equals(right, none) || !Arrays.equals(right, wrong)) {
                problems.add(f.name);
            }
        }
        Check.isTrue(problems.isEmpty(), "the result depends on the MIME type for " + problems);
    }

    /**
     * Decoding at another sample rate gives the file's sine at the output times: stereo files with a different tone in each channel, files at
     * 8000 to 96000 Hz converted to 44100, 48000 and 22050 Hz and a few others; the length is <code>ceil(frames * rate / fileRate)</code>,
     * the left channel holds only the left tone and the right channel only the right tone (the error of the 16 bit samples is
     * at most 1.5e-5 and the filter adds at most 5e-6, so 6e-5 is allowed).
     * <p>
     * 다른 표본율로 디코딩하면 출력 시각에서의 파일의 사인파가 나온다. 채널마다 다른 톤을 가진 스테레오 파일을 8000 ~ 96000 Hz 에서 44100, 48000, 22050 Hz 등으로 변환한다. 길이는 <code>ceil(프레임 수 * 표본율 / 파일 표본율)</code> 이고 왼쪽 채널에는 왼쪽 톤만, 오른쪽 채널에는 오른쪽 톤만 있다(16비트 표본의 오차는 최대 1.5e-5, 필터가 더하는 오차는 최대 5e-6 이므로 6e-5 까지 허용한다).
     */
    public void testResamplingGivesTheSineAtTheOutputTimes() {
        int[] fileRates = {8000, 22050, 44100, 48000, 96000};
        int[] targets = {44100, 48000, 22050, 8000, 96000, 11025};
        // 파일 표본율과 목표 표본율의 모든 쌍을 확인한다.
        for (int fi = 0; fi < fileRates.length; fi++) {
            for (int ti = 0; ti < targets.length; ti++) {
                int fr = fileRates[fi];
                int target = targets[ti];
                double low = Math.min(fr, target) / 2.0;
                double[] hz = {low * 0.1, low * 0.6};
                int frames = fr * 3 / 4 + 17;
                AudioBuffer b = AudioBufferDecoder.decode(AudioFiles.sineWav(fr, frames, hz, 0.5), "audio/wav", target, null);
                long wantLength = ((long) frames * target + fr - 1) / fr;
                Check.equal(2, b.getNumberOfChannels(), "channels, " + fr + " -> " + target);
                Check.equal(wantLength, b.getLength(), "length, " + fr + " -> " + target);
                Check.near(target, b.getSampleRate(), 0, "the sample rate of the buffer");
                int m = (int) Math.ceil(new Resampler(fr, target).getTaps() * (double) target / fr) + 20;
                // 양 끝을 뺀 모든 출력 표본을 채널의 사인과 비교한다.
                for (int c = 0; c < 2; c++) {
                    float[] d = b.getChannelData(c);
                    double max = 0;
                    // 가장자리를 뺀 모든 표본의 오차 최댓값을 구한다.
                    for (int i = m; i < d.length - m; i++) {
                        max = Math.max(max, Math.abs(d[i] - 0.5 * Math.sin(2 * Math.PI * hz[c] * i / target)));
                    }
                    Check.isTrue(max < 6e-5, fr + " -> " + target + " channel " + c + ": error " + max);
                }
            }
        }
    }

    /**
     * A sample rate of the context that is not a whole number (the specification allows any float in the range) uses the interpolated
     * table: the length follows <code>ceil(frames * rate / fileRate)</code> and the sine is reproduced as for the whole rates.
     * <p>
     * 컨텍스트의 표본율이 정수가 아니어도(규격은 범위 안의 어떤 float 도 허용한다) 보간하는 표를 써서 처리한다. 길이는
     * <code>ceil(프레임 수 * 표본율 / 파일 표본율)</code> 을 따르고 사인파는 정수 표본율에서와 같이 재현된다.
     */
    public void testAFractionalSampleRateOfTheContext() {
        double hz = 1000;
        int frames = 20000;
        AudioBuffer b = AudioBufferDecoder.decode(AudioFiles.sineWav(44100, frames, new double[] {hz}, 0.5), null, 44100.5f, null);
        Check.equal((long) Math.ceil(frames * 44100.5 / 44100.0), b.getLength(), "length");
        Check.near(44100.5, b.getSampleRate(), 0, "the rate of the buffer");
        float[] d = b.getChannelData(0);
        double max = 0;
        // 양 끝 400 표본을 뺀 모든 표본을 사인과 비교한다.
        for (int i = 400; i < d.length - 400; i++) {
            max = Math.max(max, Math.abs(d[i] - 0.5 * Math.sin(2 * Math.PI * hz * i / 44100.5)));
        }
        Check.isTrue(max < 6e-5, "error " + max);
    }

    /**
     * The frame counts around the block size of the pipeline (1, 4095, 4096, 4097, 8192) at the rate of the file and converted: the length is
     * exact, and at the same rate every sample is the file's sample.
     * <p>
     * 파이프라인의 블록 크기 근처의 프레임 수(1, 4095, 4096, 4097, 8192)를 파일의 표본율과 변환한 표본율에서 확인한다. 길이가 정확하고 같은 표본율에서는 모든 표본이 파일의 표본 그대로이다.
     */
    public void testBlockBoundaries() {
        int[] lengths = {1, 2, 4095, 4096, 4097, 8191, 8192, 8193};
        // 블록 경계 근처의 길이마다 같은 표본율과 변환의 결과를 확인한다.
        for (int i = 0; i < lengths.length; i++) {
            int n = lengths[i];
            short[][] ch = new short[2][n];
            // 채널마다 구별되는 값을 넣어 어긋남이 드러나게 한다.
            for (int f = 0; f < n; f++) {
                ch[0][f] = (short) (f * 7 + 1);
                ch[1][f] = (short) (-f * 5 - 1);
            }
            byte[] wav = AudioFiles.wav(44100, ch);
            AudioBuffer same = AudioBufferDecoder.decode(wav, null, 44100f, null);
            short[] inter = new short[n * 2];
            // 두 채널을 프레임 순서로 인터리브한다.
            for (int f = 0; f < n; f++) {
                inter[2 * f] = ch[0][f];
                inter[2 * f + 1] = ch[1][f];
            }
            AudioFiles.assertExactly("same rate, " + n + " frames", same, inter);
            AudioBuffer up = AudioBufferDecoder.decode(wav, null, 48000f, null);
            Check.equal(((long) n * 48000 + 44099) / 44100, up.getLength(), "converted length for " + n + " frames");
            AudioBuffer down = AudioBufferDecoder.decode(wav, null, 22050f, null);
            Check.equal(((long) n * 22050 + 44099) / 44100, down.getLength(), "length at half the rate for " + n + " frames");
        }
    }

    /**
     * A file of one frame gives the response of the filter: one frame at the rate of the file, <code>ceil(48000 / 8000) = 6</code> frames at
     * six times the rate.
     * <p>
     * 프레임 하나짜리 파일은 필터의 응답을 준다. 파일의 표본율에서는 프레임 하나이고 여섯 배의 표본율에서는 <code>ceil(48000 / 8000) = 6</code> 프레임이다.
     */
    public void testASingleFrame() {
        byte[] one = AudioFiles.wav(8000, new short[][] {{16384}});
        Check.equal(1, AudioBufferDecoder.decode(one, null, 8000f, null).getLength(), "one frame at the same rate");
        Check.near(0.5, AudioBufferDecoder.decode(one, null, 8000f, null).getChannelData(0)[0], 0, "its value");
        AudioBuffer six = AudioBufferDecoder.decode(one, null, 48000f, null);
        Check.equal(6, six.getLength(), "one frame at six times the rate");
        Check.near(0.5 * 0.95, six.getChannelData(0)[0], 1e-3, "the first frame is the centre of the kernel");
    }

    /**
     * A file without a single sample is a decoding failure (an <code>AudioBuffer</code> needs a length of at least 1).
     * <p>
     * 표본이 하나도 없는 파일은 디코딩 실패이다(<code>AudioBuffer</code> 는 길이가 1 이상이어야 한다).
     */
    public void testAFileWithoutSamplesIsAnEncodingError() {
        expectEncodingError(AudioKit.wav(1, 2, 44100, 16, new byte[0], -1), "an empty data chunk");
    }

    /**
     * The failures of the data, all of them <code>EncodingError</code>s with the cause attached: the formats that the decoders refuse (the four
     * fixtures of the kind "refuse": Yamaha ADPCM, Windows Media Audio, MPEG Layer II, Apple Lossless), empty and tiny data, text, random bytes, an HTML page.
     *
     * <p>
     * 데이터의 실패는 모두 원인이 붙은 <code>EncodingError</code> 이다. 디코더가 거부하는 형식(종류가 "refuse" 인 시험 자료 넷: Yamaha ADPCM, Windows Media Audio, MPEG Layer II, Apple Lossless), 빈 데이터와 아주 작은 데이터, 텍스트, 무작위 바이트, HTML 페이지.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testUnsupportedAndNonAudioDataGiveEncodingErrors() throws Exception {
        int refused = 0;
        // 거부해야 하는 시험 자료는 모두 EncodingError 이고 원인은 미디어 계층의 예외이다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                refused++;
                AudioException e = expectEncodingError(f.readFile(), f.name);
                Check.isTrue(e.getCause() instanceof MediaException, f.name + ": the cause is the exception of the media layer");
            }
        }
        Check.equal(4, refused, "the refused fixtures");
        expectEncodingError(new byte[0], "empty data");
        expectEncodingError(new byte[] {1}, "one byte");
        expectEncodingError("this is not audio, it is text".getBytes("UTF-8"), "text");
        expectEncodingError("<html><body>an error page</body></html>".getBytes("UTF-8"), "html");
        Random rnd = new Random(20261122L);
        int[] sizes = {2, 12, 100, 1000, 65536};
        // 크기마다 무작위 바이트를 여러 번 시험한다.
        for (int s = 0; s < sizes.length; s++) {
            for (int t = 0; t < 10; t++) {
                byte[] noise = new byte[sizes[s]];
                rnd.nextBytes(noise);
                expectEncodingError(noise, "random bytes " + sizes[s] + "/" + t);
            }
        }
    }

    /**
     * Damaged copies of every file (cut off, bytes changed in the middle, a block of zeros, bytes changed in the headers and anywhere) either
     * decode (to a valid buffer: at least one frame, the rate asked for, finite samples of at most 4 in size: a full scale noise that a damaged file decodes to overshoots 1 when it is converted) or fail with an
     * <code>EncodingError</code>: no other exception, no error such as an array index, escapes.
     *
     * <p>
     * 모든 파일의 손상된 사본(잘림, 가운데 바이트 변경, 0 블록, 헤더와 임의 위치의 바이트 변경)은 올바른 버퍼(프레임 1 개 이상, 요청한 표본율, 크기 4 이하의 유한한 표본)로 디코딩되거나 <code>EncodingError</code> 로 실패한다. 그 밖의 예외나 배열 인덱스 같은 오류는 새어 나가지 않는다.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testDamagedFilesDecodeOrGiveAnEncodingErrorAndNothingElse() throws Exception {
        Random rnd = new Random(20261122L);
        List<String> problems = new ArrayList<String>();
        int decoded = 0;
        int failed = 0;
        // 시험 자료마다 손상 종류별 사본을 디코딩한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            byte[] orig = f.readFile();
            // 손상 종류마다 사본을 만들어 디코딩한다.
            for (int kind = 0; kind <= 8; kind++) {
                byte[] d = orig.clone();
                String what;
                if (kind == 0) {
                    d = Arrays.copyOf(orig, Math.max(1, orig.length * 3 / 5));
                    what = "cut off";
                } else if (kind == 1) {
                    // 가운데 20~80% 구간에서 바이트 16개를 무작위로 바꾼다.
                    for (int i = 0; i < 16; i++) {
                        d[orig.length / 5 + rnd.nextInt(Math.max(1, orig.length * 3 / 5))] = (byte) rnd.nextInt(256);
                    }
                    what = "flipped bytes";
                } else if (kind == 2) {
                    int from = orig.length / 2;
                    Arrays.fill(d, from, Math.min(d.length, from + 64), (byte) 0);
                    what = "zeroed block";
                } else if (kind <= 5) {
                    // 머리와 설정 헤더가 있는 앞쪽 300바이트에서 1, 3, 8 바이트를 바꾼다.
                    int n = kind == 3 ? 1 : (kind == 4 ? 3 : 8);
                    for (int i = 0; i < n; i++) {
                        d[rnd.nextInt(Math.min(300, d.length))] = (byte) rnd.nextInt(256);
                    }
                    what = n + " bytes changed in the first 300";
                } else {
                    // 파일 어디서든 1, 4, 32 바이트를 바꾼다.
                    int n = kind == 6 ? 1 : (kind == 7 ? 4 : 32);
                    for (int i = 0; i < n; i++) {
                        d[rnd.nextInt(d.length)] = (byte) rnd.nextInt(256);
                    }
                    what = n + " bytes changed anywhere";
                }
                try {
                    AudioBuffer b = AudioBufferDecoder.decode(d, f.mime, 48000f, null);
                    decoded++;
                    String bad = validate(b, 48000f);
                    if (bad != null) {
                        problems.add(f.name + " (" + what + "): " + bad);
                    }
                } catch (AudioException e) {
                    failed++;
                    if (e.getKind() != AudioException.Kind.ENCODING || e.getCause() == null) {
                        problems.add(f.name + " (" + what + "): " + e + ", cause " + e.getCause());
                    }
                } catch (Throwable t) {
                    problems.add(f.name + " (" + what + "): " + t);
                }
            }
        }
        System.out.println("damaged copies: " + decoded + " decoded, " + failed + " gave an EncodingError");
        Check.isTrue(decoded > 0 && failed > 0, "both outcomes occur: " + decoded + " / " + failed);
        Check.isTrue(problems.isEmpty(), problems.size() + " damaged copies ended in something else:\n" + join(problems));
    }

    /**
     * Checks the invariants of a decoded buffer.
     * <p>
     * 디코딩한 버퍼의 불변 조건을 검사한다.
     *
     * @param b    the buffer / 버퍼
     * @param rate the rate that was asked for / 요청한 표본율
     * @return a description of the first violation, or null / 처음 위반의 설명. 없으면 null
     */
    private static String validate(AudioBuffer b, float rate) {
        if (b.getLength() < 1 || b.getNumberOfChannels() < 1 || b.getNumberOfChannels() > WebAudioLimits.MAX_CHANNELS) {
            return "invalid shape " + b.getNumberOfChannels() + " x " + b.getLength();
        }
        if (b.getSampleRate() != rate) {
            return "rate " + b.getSampleRate();
        }
        // 모든 표본이 유한하고 유한하고 크기가 4 이하인지 본다(손상된 데이터가 만든 풀 스케일 잡음을 변환하면 필터의 오버슈트로 1 을 넘을 수 있으며 그 한계는 커널의 가중치 절댓값 합 2~3 이다).
        for (int c = 0; c < b.getNumberOfChannels(); c++) {
            float[] d = b.getChannelData(c);
            // 채널의 모든 표본을 확인한다.
            for (int i = 0; i < d.length; i++) {
                if (!(Math.abs(d[i]) <= 4f)) {
                    return "sample " + i + " of channel " + c + " is " + d[i];
                }
            }
        }
        return null;
    }

    /**
     * The limits: a file larger than the limit of the media layer, a decoded length over the limit of the buffer (known in advance, with and
     * without a conversion) and a stream whose length is not known in advance that grows past the limit.
     *
     * <p>
     * 한도: 미디어 계층의 한도보다 큰 파일, 버퍼 한도를 넘는 디코딩 길이(미리 알 수 있는 경우, 변환이 있을 때와 없을 때), 한도를 넘어 자라는 길이를 미리 알 수 없는 스트림.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testLimits() throws Exception {
        byte[] wav = AudioFiles.sineWav(44100, 44100, new double[] {440, 880}, 0.5);
        // 입력 바이트 한도: 100 바이트 한도에서 파일이 너무 크다.
        try {
            AudioBufferDecoder.decode(wav, null, 44100f, null, new DecodeLimits(1000, 100, 10, 1000), WebAudioLimits.MAX_BUFFER_SAMPLES);
            Check.fail("a file over the byte limit");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "byte limit");
            Check.isTrue(e.getCause() instanceof MediaException && ((MediaException) e.getCause()).getKind() == MediaException.Kind.TOO_LARGE, "the cause is TOO_LARGE");
        }
        // 출력 한도: 44100 프레임 x 2 채널 = 88200 표본이 한도 88199 를 넘는다. 같은 표본율과 변환 모두.
        try {
            AudioBufferDecoder.decode(wav, null, 44100f, null, null, 88199);
            Check.fail("a buffer over the output limit");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "output limit");
        }
        AudioBuffer exactly = AudioBufferDecoder.decode(wav, null, 44100f, null, null, 88200);
        Check.equal(44100, exactly.getLength(), "a buffer exactly at the limit");
        try {
            AudioBufferDecoder.decode(wav, null, 48000f, null, null, 88200);
            Check.fail("a converted buffer over the output limit (48000 frames x 2)");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "converted output limit");
        }
        Check.equal(48000, AudioBufferDecoder.decode(wav, null, 48000f, null, null, 96000).getLength(), "a converted buffer exactly at the limit");
        // 길이를 미리 알 수 없는 파일이 있으면 자라는 도중에 한도를 넘는 경우도 확인한다.
        boolean unknownChecked = false;
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                continue;
            }
            PcmSource s = AudioDecoders.open(f.readFile(), f.mime, DecodeLimits.DEFAULT);
            long known = s.getFrameCount();
            s.close();
            // 길이를 알려 주지 않는 첫 파일로 한도 아래와 위를 확인한다.
            if (known < 0) {
                try {
                    AudioBufferDecoder.decode(f.readFile(), f.mime, f.rate, null, null, 100);
                    Check.fail(f.name + ": a stream of unknown length over the limit");
                } catch (AudioException e) {
                    Check.equal(AudioException.Kind.ENCODING, e.getKind(), f.name + ": the limit while growing");
                }
                unknownChecked = true;
                break;
            }
        }
        System.out.println("fixture of unknown length found: " + unknownChecked);
    }

    /**
     * The growing array of the pipeline: a size known in advance is never exceeded and the result has exactly the collected length, an
     * unknown size doubles, and the limit is an <code>EncodingError</code>.
     * <p>
     * 파이프라인의 늘어나는 배열: 미리 아는 크기를 넘지 않고 결과가 모은 길이와 정확히 같으며, 모르는 크기는 두 배씩 늘고, 한도는 <code>EncodingError</code> 이다.
     */
    public void testChannelSink() {
        ChannelSink exact = new ChannelSink(10, 100);
        exact.append(new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 10);
        Check.equal(10, exact.length(), "length");
        float[] a = exact.finish();
        Check.equal(10, a.length, "a full array is returned as it is");
        ChannelSink grow = new ChannelSink(2, 1000);
        float[] chunk = new float[300];
        Arrays.fill(chunk, 0.25f);
        grow.append(chunk, 300);
        grow.append(chunk, 100);
        Check.equal(400, grow.length(), "grown length");
        float[] g = grow.finish();
        Check.equal(400, g.length, "trimmed to the length");
        Check.near(0.25, g[399], 0, "content");
        final ChannelSink limited = new ChannelSink(4, 8);
        limited.append(new float[8], 8);
        try {
            limited.append(new float[1], 1);
            Check.fail("past the limit");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "limit");
        }
        limited.append(new float[1], 0);
    }

    /**
     * A sample rate of the file that cannot be converted (a ratio above 1024 to the wanted rate) is an <code>EncodingError</code>, and the largest
     * accepted ratio works.
     * <p>
     * 변환할 수 없는 파일 표본율(원하는 표본율과의 비율이 1024 를 넘음)은 <code>EncodingError</code> 이고, 받아들이는 가장 큰 비율은 동작한다.
     */
    public void testRatiosOfTheSampleRates() {
        byte[] tiny = AudioFiles.sineWav(100, 200, new double[] {5}, 0.5);
        try {
            AudioBufferDecoder.decode(tiny, null, 768000f, null);
            Check.fail("a file at 100 Hz converted to 768000 Hz");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "ratio above 1024");
        }
        byte[] slow = AudioFiles.sineWav(3000, 3000, new double[] {100}, 0.5);
        AudioBuffer big = AudioBufferDecoder.decode(slow, null, 768000f, null);
        Check.equal(768000, big.getLength(), "3000 Hz to 768000 Hz");
    }

    /**
     * Cancellation: a flag that is set before the start, a flag that is set after a few blocks (the decoding stops within a few blocks, which
     * the number of polls shows), an interrupted thread; none of them gives a result or an <code>EncodingError</code>.
     * <p>
     * 취소: 시작 전에 선 플래그, 몇 블록 뒤에 서는 플래그(질의 횟수로 확인하는 대로 디코딩이 몇 블록 안에 멈춘다), 인터럽트된 스레드. 어느 것도 결과나 <code>EncodingError</code> 를 주지 않는다.
     */
    public void testCancellation() {
        final byte[] long5s = AudioFiles.sineWav(44100, 44100 * 5, new double[] {300, 400}, 0.4);
        Check.throwsException(CancellationException.class, new Block() {
            public void run() throws Exception {
                AudioBufferDecoder.decode(long5s, null, 48000f, new DecodeCancellation() {
                    public boolean isCancelled() {
                        return true;
                    }
                });
            }
        }, "a flag that is set from the start");
        final int[] polls = {0};
        Check.throwsException(CancellationException.class, new Block() {
            public void run() throws Exception {
                AudioBufferDecoder.decode(long5s, null, 48000f, new DecodeCancellation() {
                    public boolean isCancelled() {
                        return ++polls[0] > 5;
                    }
                });
            }
        }, "a flag that is set after five polls");
        // 5초(약 54 블록)를 다 읽었다면 질의가 50 번을 넘었을 것이다. 취소 후 곧바로 멈추어야 한다.
        Check.isTrue(polls[0] >= 6 && polls[0] <= 8, "the decoding stopped within a block of the flag: " + polls[0] + " polls");
        Thread.currentThread().interrupt();
        try {
            Check.throwsException(CancellationException.class, new Block() {
                public void run() throws Exception {
                    AudioBufferDecoder.decode(long5s, null, 48000f, null);
                }
            }, "an interrupted thread");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * The arguments that the caller controls: null data, a sample rate outside 3000 to 768000 Hz (or not a number) and a limit that is not positive.
     * <p>
     * 호출한 쪽이 정하는 인수: null 데이터, 3000 ~ 768000 Hz 밖(또는 숫자가 아닌) 표본율, 양수가 아닌 한도.
     */
    public void testArguments() {
        final byte[] wav = AudioFiles.sineWav(8000, 100, new double[] {100}, 0.5);
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                AudioBufferDecoder.decode(null, null, 44100f, null);
            }
        }, "null data");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() throws Exception {
                AudioBufferDecoder.decode(wav, null, 44100f, null, null, 0);
            }
        }, "a limit of 0");
        float[] bad = {2999f, 768001f, Float.NaN, 0f, -44100f, Float.POSITIVE_INFINITY};
        // 범위 밖의 표본율은 모두 NotSupportedError 이다.
        for (int i = 0; i < bad.length; i++) {
            final float rate = bad[i];
            try {
                AudioBufferDecoder.decode(wav, null, rate, null);
                Check.fail("rate " + rate);
            } catch (AudioException e) {
                Check.equal(AudioException.Kind.NOT_SUPPORTED, e.getKind(), "rate " + rate);
            }
        }
        Check.equal(38, AudioBufferDecoder.decode(wav, null, 3000f, null).getLength(), "the lowest rate is accepted: ceil(100 * 3000 / 8000) frames");
        Check.equal(800, AudioBufferDecoder.decode(wav, null, 64000f, null).getLength(), "and a higher one: 100 * 64000 / 8000 frames");
    }

    /**
     * A source of 16 bit PCM for the tests of the pipeline: a different constant or sine in every channel, delivered in reads of odd sizes,
     * with a reported length that can be unknown or wrong and with failures at a chosen frame.
     * <p>
     * 파이프라인 시험용 16비트 PCM 소스: 채널마다 다른 상수나 사인을 홀수 크기의 읽기로 준다. 알리는 길이는 모르거나 틀릴 수 있고 정한 프레임에서 실패할 수 있다.
     */
    private static final class FakeSource implements PcmSource {

        /** The rate. / 표본율. */
        final int rate;

        /** The number of channels. / 채널 수. */
        final int channels;

        /** The number of frames that it delivers. / 주는 프레임 수. */
        final long frames;

        /** The length that it reports. / 알리는 길이. */
        final long reported;

        /** The frame at which a read fails, or -1. / 읽기가 실패하는 프레임. 없으면 -1. */
        long failAt = -1;

        /** The kind of the failure: 0 a MediaException, 1 an ArrayIndexOutOfBoundsException, 2 a cancelled MediaException. / 실패의 종류: 0 은 MediaException, 1 은 ArrayIndexOutOfBoundsException, 2 는 취소된 MediaException. */
        int failKind;

        /** True if every read delivers nothing. / 모든 읽기가 아무것도 주지 않으면 true. */
        boolean stalled;

        /** The next frame. / 다음 프레임. */
        long next;

        /** True after close. / 닫은 뒤에는 true. */
        boolean closed;

        /**
         * Creates a source.
         * <p>
         * 소스를 만든다.
         *
         * @param rate     the rate / 표본율
         * @param channels the channels / 채널 수
         * @param frames   the frames delivered / 주는 프레임 수
         * @param reported the length reported (-1 for unknown) / 알리는 길이(-1 이면 모름)
         */
        FakeSource(int rate, int channels, long frames, long reported) {
            this.rate = rate;
            this.channels = channels;
            this.frames = frames;
            this.reported = reported;
        }

        /**
         * The sample of a channel at a frame: a sine of the frequency 100 times the channel number plus 100, quarter scale.
         * <p>
         * 채널의 프레임에서의 표본: 주파수가 100 x (채널 번호 + 1) 인 사인. 풀 스케일의 4분의 1.
         *
         * @param c the channel / 채널
         * @param f the frame / 프레임
         * @return the sample / 표본
         */
        short sample(int c, long f) {
            return (short) Math.round(8192.0 * Math.sin(2 * Math.PI * (100.0 * (c + 1)) * f / rate));
        }

        /** @return the rate / 표본율 */
        public int getSampleRate() {
            return rate;
        }

        /** @return the channels / 채널 수 */
        public int getChannels() {
            return channels;
        }

        /** @return the reported length / 알리는 길이 */
        public long getFrameCount() {
            return reported;
        }

        /**
         * Delivers the next frames in reads of an odd number of frames.
         * <p>
         * 다음 프레임을 홀수 프레임 단위로 준다.
         *
         * @param buffer the buffer / 버퍼
         * @param offset the offset / 위치
         * @param length the length / 길이
         * @return the bytes delivered or -1 / 준 바이트 수 또는 -1
         * @throws MediaException the failure / 실패
         */
        public int read(byte[] buffer, int offset, int length) throws MediaException {
            if (stalled) {
                return 0;
            }
            if (failAt >= 0 && next >= failAt) {
                if (failKind == 1) {
                    return buffer[-1];
                }
                throw new MediaException(failKind == 2 ? MediaException.Kind.CANCELLED : MediaException.Kind.CORRUPT, "the fake source fails");
            }
            if (next >= frames) {
                return -1;
            }
            long n = Math.min(Math.min(frames - next, 1000 + (next % 7)), length / (channels * 2));
            if (failAt >= 0) {
                n = Math.min(n, failAt - next);
            }
            int o = offset;
            // 프레임마다 채널의 표본을 리틀 엔디언으로 쓴다.
            for (long f = 0; f < n; f++) {
                for (int c = 0; c < channels; c++) {
                    short s = sample(c, next + f);
                    buffer[o++] = (byte) s;
                    buffer[o++] = (byte) (s >> 8);
                }
            }
            next += n;
            return (int) n * channels * 2;
        }

        /**
         * Tells that the source cannot seek.
         * <p>
         * 소스가 탐색할 수 없음을 알려 준다.
         *
         * @return false / false
         */
        public boolean isSeekable() {
            return false;
        }

        /**
         * Not supported.
         * <p>
         * 지원하지 않는다.
         *
         * @param frame the frame / 프레임
         * @throws MediaException always / 항상
         */
        public void seekToFrame(long frame) throws MediaException {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "not seekable");
        }

        /**
         * Closes the source.
         * <p>
         * 소스를 닫는다.
         */
        public void close() {
            closed = true;
        }
    }

    /**
     * A source whose length is not known in advance (the decoders of the media layer all report it, but the interface allows -1) grows its
     * result as it is read: the same samples as a source of known length, at the rate of the file and converted; a length that the source
     * reports wrongly changes nothing (the result has the frames that were really delivered); the source is closed.
     * <p>
     * 길이를 미리 모르는 소스(미디어 계층의 디코더는 모두 길이를 알리지만 인터페이스는 -1 을 허용한다)는 읽는 대로 결과를 키운다. 길이를 아는 소스와 같은 표본이 파일의 표본율과 변환한 표본율에서 나온다. 소스가 틀리게 알린 길이는 아무것도 바꾸지 않는다(결과는 실제로 준 프레임을 가진다). 소스는 닫힌다.
     */
    public void testSourcesOfUnknownOrWrongLength() {
        FakeSource unknown = new FakeSource(44100, 2, 150000, -1);
        AudioBuffer same = AudioBufferDecoder.decodeSource(unknown, 44100f, null, WebAudioLimits.MAX_BUFFER_SAMPLES);
        Check.isTrue(unknown.closed, "the source is closed");
        Check.equal(150000, same.getLength(), "unknown length, same rate");
        // 모든 프레임이 원본 표본 그대로인지 확인한다.
        for (int c = 0; c < 2; c++) {
            for (int f = 0; f < 150000; f += 7) {
                if (same.getChannelData(c)[f] != unknown.sample(c, f) / 32768f) {
                    Check.fail("channel " + c + " frame " + f);
                }
            }
        }
        FakeSource unknown2 = new FakeSource(44100, 1, 150000, -1);
        AudioBuffer up = AudioBufferDecoder.decodeSource(unknown2, 48000f, null, WebAudioLimits.MAX_BUFFER_SAMPLES);
        Check.equal((150000L * 48000 + 44099) / 44100, up.getLength(), "unknown length, converted");
        FakeSource tooShort = new FakeSource(44100, 1, 100, 50000);
        Check.equal(100, AudioBufferDecoder.decodeSource(tooShort, 44100f, null, WebAudioLimits.MAX_BUFFER_SAMPLES).getLength(), "a source that reports more than it delivers");
        FakeSource tooLong = new FakeSource(44100, 1, 5000, 1000);
        Check.equal(5000, AudioBufferDecoder.decodeSource(tooLong, 44100f, null, WebAudioLimits.MAX_BUFFER_SAMPLES).getLength(), "a source that reports less than it delivers");
        FakeSource endless = new FakeSource(44100, 1, 1000000, -1);
        try {
            AudioBufferDecoder.decodeSource(endless, 44100f, null, 100000);
            Check.fail("a source of unknown length over the limit");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "the limit while growing");
        }
        Check.isTrue(endless.closed, "the source is closed after the failure");
        Check.isTrue(endless.next < 200000, "the reading stopped soon after the limit: " + endless.next);
    }

    /**
     * The channels that a source delivers are kept in their order and count, up to 32 (the pipeline applies no mix: a decoder that delivers the
     * original channels of a 5.1 file would give a six channel buffer); a source of 33 channels or with an impossible format is an
     * <code>EncodingError</code>.
     * <p>
     * 소스가 주는 채널은 32 개까지 순서와 개수 그대로 유지된다(파이프라인은 믹스를 하지 않는다. 5.1 파일의 원래 채널을 주는 디코더는 채널 여섯 개의 버퍼를 줄 것이다). 33 채널이나 불가능한 형식의 소스는 <code>EncodingError</code> 이다.
     */
    public void testChannelsOfASourceAreKept() {
        int[] counts = {1, 2, 3, 4, 6, 8, 32};
        // 채널 수마다 채널의 순서와 값이 그대로인지 확인한다.
        for (int k = 0; k < counts.length; k++) {
            FakeSource s = new FakeSource(22050, counts[k], 5000, 5000);
            AudioBuffer b = AudioBufferDecoder.decodeSource(s, 22050f, null, WebAudioLimits.MAX_BUFFER_SAMPLES);
            Check.equal(counts[k], b.getNumberOfChannels(), "channels");
            // 채널마다 값이 그대로인지 확인한다.
            for (int c = 0; c < counts[k]; c++) {
                // 채널의 프레임을 띄엄띄엄 확인한다.
                for (int f = 0; f < 5000; f += 13) {
                    if (b.getChannelData(c)[f] != s.sample(c, f) / 32768f) {
                        Check.fail(counts[k] + " channels: channel " + c + " frame " + f);
                    }
                }
            }
        }
        FakeSource tooMany = new FakeSource(22050, 33, 100, 100);
        try {
            AudioBufferDecoder.decodeSource(tooMany, 22050f, null, WebAudioLimits.MAX_BUFFER_SAMPLES);
            Check.fail("33 channels");
        } catch (AudioException e) {
            Check.equal(AudioException.Kind.ENCODING, e.getKind(), "33 channels");
        }
        Check.isTrue(tooMany.closed, "the source is closed");
    }

    /**
     * The failures of a source in the middle of the stream: a <code>MediaException</code>, an unexpected runtime exception such as an array
     * index (damaged data that a decoder does not check), a decoder that delivers nothing for ever and one that reports a cancellation. The
     * first three are <code>EncodingError</code>s with the cause, the last one a cancellation, and the source is always closed.
     * <p>
     * 스트림 중간의 소스 실패: <code>MediaException</code>, 배열 인덱스 같은 예상 밖의 런타임 예외(디코더가 검사하지 않은 손상된 데이터), 영원히 아무것도 주지 않는 디코더, 취소를 보고하는 디코더. 앞의 셋은 원인이 붙은 <code>EncodingError</code>, 마지막은 취소이며 소스는 언제나 닫힌다.
     */
    public void testFailuresInTheMiddleOfTheStream() {
        // 실패 종류마다 소스를 만들어 결과를 확인한다.
        for (int kind = 0; kind <= 3; kind++) {
            FakeSource s = new FakeSource(44100, 2, 100000, 100000);
            if (kind == 3) {
                s.stalled = true;
            } else {
                s.failAt = 30000;
                s.failKind = kind;
            }
            try {
                AudioBufferDecoder.decodeSource(s, 48000f, null, WebAudioLimits.MAX_BUFFER_SAMPLES);
                Check.fail("failure kind " + kind);
            } catch (CancellationException e) {
                Check.equal(2, kind, "only the cancelled decoder gives a cancellation");
            } catch (AudioException e) {
                Check.isTrue(kind != 2, "the cancelled decoder is not an error");
                Check.equal(AudioException.Kind.ENCODING, e.getKind(), "kind " + kind);
                Check.isTrue(kind == 3 || e.getCause() != null, "the cause is kept for kind " + kind);
            }
            Check.isTrue(s.closed, "the source is closed after kind " + kind);
        }
    }

    /**
     * The class has no state: several threads decode the same file at once and all get the same samples.
     *
     * <p>
     * 클래스에 상태가 없다. 여러 스레드가 같은 파일을 동시에 디코딩해도 모두 같은 표본을 얻는다.
     *
     * @throws Exception if a thread fails / 스레드가 실패하면
     */
    public void testThreadsDecodeAtTheSameTime() throws Exception {
        final byte[] wav = AudioFiles.sineWav(44100, 30000, new double[] {440, 1000}, 0.5);
        final AudioBuffer want = AudioBufferDecoder.decode(wav, null, 48000f, null);
        final boolean[] ok = new boolean[4];
        Thread[] threads = new Thread[4];
        // 스레드마다 같은 파일을 여러 번 디코딩해 결과가 같은지 확인한다.
        for (int t = 0; t < threads.length; t++) {
            final int id = t;
            threads[t] = new Thread(new Runnable() {
                public void run() {
                    boolean good = true;
                    // 같은 파일을 되풀이해 디코딩하며 결과가 같은지 모은다.
                    for (int i = 0; i < 10; i++) {
                        AudioBuffer b = AudioBufferDecoder.decode(wav, null, 48000f, null);
                        good &= Arrays.equals(b.getChannelData(0), want.getChannelData(0)) && Arrays.equals(b.getChannelData(1), want.getChannelData(1));
                    }
                    ok[id] = good;
                }
            });
            threads[t].start();
        }
        // 모든 스레드가 끝나기를 기다린다.
        for (int t = 0; t < threads.length; t++) {
            threads[t].join();
            Check.isTrue(ok[t], "thread " + t + " got the same samples");
        }
    }
}
