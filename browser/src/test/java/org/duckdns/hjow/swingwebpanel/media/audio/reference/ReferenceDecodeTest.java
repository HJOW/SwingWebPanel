package org.duckdns.hjow.swingwebpanel.media.audio.reference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteAudioDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.ByteSourceException;
import org.duckdns.hjow.swingwebpanel.media.audio.FileByteSource;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.aac.AacDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.mp3.Mp3Decoder;
import org.duckdns.hjow.swingwebpanel.media.audio.opus.OpusDecoder;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.OggMux;
import org.duckdns.hjow.swingwebpanel.media.audio.vorbis.VorbisDecoder;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Cross check of the audio decoders against files that real encoders wrote and against the PCM that ffmpeg's decoders produced from them (plan item
 * P18-11). The decoders of the product had been verified only with test encoders written from the specifications; here the inputs come from libmp3lame,
 * the native AAC encoder of ffmpeg, libvorbis, libopus, flac and the PCM/ADPCM encoders (the fixtures and how they were made: see
 * <code>tests/resources/media/audio/generation</code>). Lossless formats (PCM, float, mu-law, A-law, ADPCM, FLAC) have to be decoded bit for bit as
 * ffmpeg does. The lossy codecs (MP3, AAC, Vorbis, Opus) are compared with the reference output of ffmpeg within a tolerance (see {@link #tolerance}),
 * the length has to be the same frame count (gapless: decoder delay and padding removed as the containers say), and there must be no time shift.
 * Channels beyond two are mixed down by the product, so the reference is mixed down by the speaker roles in the test with the documented policy.
 * The same files also check that the decoder is chosen by the signature (not by the MIME hint), that a seek gives the samples of a sequential decoding,
 * that formats which the product does not support are rejected with UNSUPPORTED_FORMAT, and that damaged copies fail only with a MediaException.
 * <p>
 * 실제 인코더가 쓴 파일과 ffmpeg 의 디코더가 그것에서 만든 PCM 에 대한 오디오 디코더의 교차 검증(계획 P18-11). 제품의 디코더는 규격에서 직접 쓴 시험용
 * 인코더로만 검증되어 있었다. 여기서는 libmp3lame, ffmpeg 의 기본 AAC 인코더, libvorbis, libopus, flac, PCM/ADPCM 인코더가 만든 입력을 쓴다(시험 자료와 만든 방법은
 * <code>tests/resources/media/audio/generation</code>). 무손실 형식(PCM, 부동 소수점, mu-law, A-law, ADPCM, FLAC)은 ffmpeg 와 비트 단위로 같아야 한다.
 * 손실 코덱(MP3, AAC, Vorbis, Opus)은 ffmpeg 의 참조 출력과 허용 오차({@link #tolerance}) 안에서 비교하며, 길이(프레임 수)가 같아야 하고(갭리스: 컨테이너가
 * 알려 주는 대로 디코더 지연과 패딩을 제거) 시간 이동이 없어야 한다. 두 개를 넘는 채널은 제품이 섞으므로 시험에서 참조를 스피커 역할로 문서화된 정책대로 섞는다.
 * 같은 파일로 디코더가 MIME 힌트가 아니라 시그니처로 정해지는지, 탐색한 결과가 순차 디코딩과 같은지, 제품이 지원하지 않는 형식이 UNSUPPORTED_FORMAT 으로 거부되는지,
 * 손상된 사본이 MediaException 으로만 실패하는지도 검사한다.
 */
public class ReferenceDecodeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ReferenceDecodeTest() {
    }

    /**
     * The quantization step of 16-bit PCM relative to full scale divided by the square root of twelve: the RMS error of rounding to 16 bits is
     * <code>2^-15 / sqrt(12)</code> of full scale, which is the "full accuracy" bound for an MPEG audio decoder in ISO/IEC 11172-4 (in units of the
     * least significant bit it is 0.2887).
     * <p>
     * 16비트 PCM 의 양자화 간격을 12 의 제곱근으로 나눈 값: 16비트로 반올림하는 오차의 RMS 는 풀 스케일의 <code>2^-15 / sqrt(12)</code> 이며 이것이 ISO/IEC
     * 11172-4 의 MPEG 오디오 디코더 "완전 정확도" 한계이다(최하위 비트 단위로 0.2887).
     */
    static final double ISO_FULL_ACCURACY_RMS = 1.0 / Math.sqrt(12.0);

    /**
     * Error bound of one fixture: the largest absolute difference of a sample and the RMS of the difference, both in units of the 16-bit LSB.
     * <p>
     * 시험 자료 하나의 오차 한계: 샘플 하나의 최대 절대 차이와 차이의 RMS(둘 다 16비트 최하위 비트 단위).
     */
    static final class Tolerance {
        /**
         * Largest allowed absolute difference of a sample.
         * <p>
         * 허용하는 샘플 하나의 최대 절대 차이.
         */
        final double maxAbs;

        /**
         * Largest allowed RMS of the difference.
         * <p>
         * 허용하는 차이의 RMS 최대값.
         */
        final double rms;

        /**
         * Creates a bound.
         * <p>
         * 한계를 만든다.
         *
         * @param maxAbs largest absolute difference / 최대 절대 차이
         * @param rms    largest RMS / 최대 RMS
         */
        Tolerance(double maxAbs, double rms) {
            this.maxAbs = maxAbs;
            this.rms = rms;
        }
    }

    /**
     * The error bound of a fixture that is compared with a tolerance.
     * <p>
     * MP3 (ISO/IEC 11172-4, section 2.6.3, "full accuracy"; quoted at https://www.underbit.com/resources/mpeg/audio/compliance, read 2026-11-18): the RMS of
     * the difference to the reference output is below <code>2^-15 / sqrt(12)</code> of full scale (0.2887 of a 16-bit LSB) and the largest difference is at
     * most <code>2^-14</code> of full scale (2 LSB). The standard measures a -20 dB sine sweep; the same bounds are applied here to the whole test signal,
     * which contains loud, quiet, noisy and transient parts. AAC and Vorbis have no such criterion in the files that were available, so the bounds of MP3
     * are used for them too (the measured errors are far below: 1 LSB at most and an RMS of 0.04). Opus decodes in floating point like libopus, so the
     * differences are only the rounding of different operation orders: at most 1 LSB, and an RMS of 0.32 allows one sample in eleven to differ by it (the
     * measured worst case is 0.25). More than two channels are mixed down by the product from the unrounded channels, while the reference is mixed down
     * from channels that ffmpeg had rounded to 16 bits, which adds the rounding of up to eight channels: at most 2 LSB and an RMS of 0.6.
     * <p>
     * 허용 오차를 두고 비교하는 시험 자료의 오차 한계. MP3(ISO/IEC 11172-4 2.6.3절의 "완전 정확도". 인용: https://www.underbit.com/resources/mpeg/audio/compliance,
     * 2026-11-18 조회): 참조 출력과의 차이의 RMS 가 풀 스케일의 <code>2^-15 / sqrt(12)</code>(16비트 LSB 의 0.2887) 미만이고 최대 차이가 풀 스케일의
     * <code>2^-14</code>(2 LSB) 이하. 규격은 -20 dB 사인 스윕으로 재지만 여기서는 큰 소리, 작은 소리, 잡음, 과도 구간이 든 시험 신호 전체에 같은 한계를 적용한다.
     * AAC 와 Vorbis 는 구할 수 있었던 자료에 그런 기준이 없어 MP3 의 한계를 같이 쓴다(측정한 오차는 훨씬 작다: 최대 1 LSB, RMS 0.04). Opus 는 libopus 처럼
     * 부동 소수점으로 디코딩하므로 차이는 연산 순서가 다른 데서 오는 반올림뿐이다: 최대 1 LSB 이고 RMS 0.32 는 열한 샘플에 하나가 1 달라도 되는 값이다(측정한 최악은
     * 0.25). 채널이 둘을 넘으면 제품은 반올림하지 않은 채널에서 섞고 참조는 ffmpeg 가 16비트로 반올림한 채널에서 섞으므로 최대 여덟 채널의 반올림이 더해진다:
     * 최대 2 LSB, RMS 0.6.
     *
     * @param f the fixture / 시험 자료
     * @return the bound / 한계
     */
    static Tolerance tolerance(ReferenceFixture f) {
        if (f.channels > 2) {
            // 무손실 다채널은 16비트 소스에서는 같아야 하고, 24비트 소스는 참조가 채널마다 반올림되어 있어 1 이 다를 수 있다.
            return f.kind.equals("exact") ? new Tolerance(1, f.name.endsWith("_24") ? 0.45 : 0.05) : new Tolerance(2, 0.6);
        }
        if (f.name.startsWith("opus_")) {
            return new Tolerance(1, 0.32);
        }
        return new Tolerance(2, ISO_FULL_ACCURACY_RMS);
    }

    /**
     * Decodes a fixture and compares it with its reference; the statistics line is appended to the log.
     * <p>
     * 시험 자료를 디코딩해 참조와 비교한다. 통계 줄은 로그에 덧붙인다.
     *
     * @param f   the fixture / 시험 자료
     * @param log the statistics log / 통계 로그
     * @return null if it matches, else the description of the failure / 맞으면 null, 아니면 실패 설명
     * @throws Exception on unexpected errors / 예상 밖의 오류
     */
    private String checkOne(ReferenceFixture f, StringBuilder log) throws Exception {
        short[] ours;
        PcmSource s = AudioDecoders.open(f.readFile(), f.mime, DecodeLimits.DEFAULT);
        try {
            int outCh = f.channels > 2 ? 2 : f.channels;
            if (s.getSampleRate() != f.rate) {
                return f.name + ": sample rate " + s.getSampleRate() + " instead of " + f.rate;
            }
            if (s.getChannels() != outCh) {
                return f.name + ": " + s.getChannels() + " channels instead of " + outCh;
            }
            ours = AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
        int outCh = f.channels > 2 ? 2 : f.channels;
        long oursFrames = ours.length / outCh;
        if (f.isMd5()) {
            String md5 = md5(ours);
            log.append(String.format("%-34s exact  frames=%d%n", f.name, oursFrames));
            if (oursFrames != f.expected) {
                return f.name + ": " + oursFrames + " frames instead of " + f.expected;
            }
            if (!md5.equals(f.md5())) {
                return f.name + ": the PCM differs from ffmpeg's (md5 " + md5 + " instead of " + f.md5() + ")";
            }
            return null;
        }
        short[] ref = f.readReferencePcm();
        if (f.channels > 2) {
            ref = ReferenceCompare.downmix(ref, f.roles);
        }
        ReferenceCompare.Diff d = ReferenceCompare.compare(ours, ref);
        log.append(String.format("%-34s %-6s frames=%d (expected %d, ffmpeg %d) %s%n", f.name, f.kind, oursFrames, f.expected, ref.length / outCh, d));
        // 길이는 컨테이너가 알려 주는 대로(갭리스) 같아야 한다. ffmpeg 의 길이가 다르면 공통 구간만 비교한다.
        if (oursFrames != f.expected) {
            int lag = ReferenceCompare.bestLag(ours, ref, outCh, 4096);
            return f.name + ": " + oursFrames + " frames instead of " + f.expected + " (ffmpeg: " + ref.length / outCh + "; best time shift " + lag + " frames; " + d + ")";
        }
        Tolerance t = tolerance(f);
        if (d.maxAbs > t.maxAbs || d.rms > t.rms) {
            int lag = ReferenceCompare.bestLag(ours, ref, outCh, 4096);
            return f.name + ": error beyond the tolerance (max " + t.maxAbs + ", rms " + t.rms + "): " + d + " at sample " + d.maxAt + ", best time shift "
                    + lag + " frames";
        }
        return null;
    }

    /**
     * Computes the md5 of 16-bit samples as little-endian bytes (the form in which ffmpeg's reference was hashed).
     * <p>
     * 16비트 샘플을 리틀 엔디언 바이트로 보고 md5 를 계산한다(ffmpeg 참조를 해시한 형태).
     *
     * @param s the samples / 샘플
     * @return lower case hexadecimal md5 / 소문자 16진수 md5
     * @throws Exception if md5 is missing / md5 가 없으면
     */
    static String md5(short[] s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] raw = AudioKit.toBytes(s);
        byte[] h = md.digest(raw);
        StringBuilder b = new StringBuilder();
        // 바이트마다 두 자리 16진수로 적는다.
        for (int i = 0; i < h.length; i++) {
            b.append(Character.forDigit((h[i] >> 4) & 15, 16)).append(Character.forDigit(h[i] & 15, 16));
        }
        return b.toString();
    }

    /**
     * Checks all fixtures of a group and fails with the list of every problem found.
     * <p>
     * 그룹의 모든 시험 자료를 검사하고 발견한 모든 문제의 목록과 함께 실패시킨다.
     *
     * @param group the group / 그룹
     * @throws Exception on unexpected errors / 예상 밖의 오류
     */
    private void checkGroup(String group) throws Exception {
        List<ReferenceFixture> all = ReferenceFixture.ofGroup(group);
        Check.isTrue(all.size() > 0, "no fixtures of the group " + group);
        StringBuilder log = new StringBuilder();
        List<String> problems = new ArrayList<String>();
        // 시험 자료마다 비교하고 문제를 모은다(거부해야 하는 파일은 따로 시험한다).
        for (ReferenceFixture f : all) {
            if (f.isRefused()) {
                continue;
            }
            String p;
            try {
                p = checkOne(f, log);
            } catch (MediaException e) {
                p = f.name + ": the product failed: " + e;
            }
            if (p != null) {
                problems.add(p);
            }
        }
        System.out.print(log);
        Check.isTrue(problems.isEmpty(), problems.size() + " of " + all.size() + " fixtures differ from ffmpeg:\n" + join(problems));
    }

    /**
     * Joins lines.
     * <p>
     * 줄들을 이어 붙인다.
     *
     * @param lines the lines / 줄
     * @return the text / 문자열
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
     * WAV and RF64 files: PCM 8/16/24/32 bit, float 32/64, A-law, mu-law, IMA/MS ADPCM, EXTENSIBLE with speaker masks.
     * <p>
     * WAV 와 RF64 파일: PCM 8/16/24/32비트, 부동 소수점 32/64, A-law, mu-law, IMA/MS ADPCM, 스피커 마스크가 있는 EXTENSIBLE.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWavMatchesFfmpeg() throws Exception {
        checkGroup("wav");
    }

    /**
     * AIFF and AIFF-C files.
     * <p>
     * AIFF 와 AIFF-C 파일.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAiffMatchesFfmpeg() throws Exception {
        checkGroup("aiff");
    }

    /**
     * AU files.
     * <p>
     * AU 파일.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAuMatchesFfmpeg() throws Exception {
        checkGroup("au");
    }

    /**
     * FLAC files: compression levels, block sizes, channel assignments, bit depths, Ogg FLAC.
     * <p>
     * FLAC 파일: 압축 수준, 블록 크기, 채널 배정, 비트 깊이, Ogg FLAC.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFlacMatchesFfmpeg() throws Exception {
        checkGroup("flac");
    }

    /**
     * MP3 files of libmp3lame: MPEG-1, MPEG-2 and MPEG-2.5, CBR/VBR/ABR, joint stereo, tags.
     * <p>
     * libmp3lame 이 만든 MP3 파일: MPEG-1, MPEG-2, MPEG-2.5, CBR/VBR/ABR, 조인트 스테레오, 태그.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMp3MatchesFfmpeg() throws Exception {
        checkGroup("mp3");
    }

    /**
     * Checks all real MP3 fixtures through file windows and includes an incomplete final frame.
     * <p>
     * 실제 MP3 시험 자료 전체를 파일 창으로 확인하며 끝 프레임이 잘린 경우도 비교한다.
     * @throws Exception on I/O or decode failure / 입출력 또는 해독에 실패하면
     */
    public void testMp3FilePcmMatchesArrayPcm() throws Exception {
        int count = 0;
        Mp3Decoder decoder = new Mp3Decoder();
        // MPEG 버전, 태그, 비트레이트가 다른 모든 참조 자료를 비교한다.
        for (ReferenceFixture fixture : ReferenceFixture.loadAll()) {
            if (!"mp3".equals(fixture.group) || fixture.isRefused()) {
                continue;
            }
            byte[] data = fixture.readFile();
            compareFileSources(decoder, fixture.name, data, false);
            // 태그가 없는 음원의 끝을 잘라 불완전 프레임을 같은 방식으로 건너뛰는지 확인한다.
            if (fixture.name.equals("mp3_cbr192_44100_notag")) {
                compareFileSources(decoder, fixture.name + " truncated", Arrays.copyOf(data, data.length - 17), false);
            }
            count++;
        }
        Check.equal(16, count, "all reference MP3 files were checked");
    }

    /**
     * Checks file-backed AAC for ADTS, edited MP4, multichannel audio and an ADTS header with CRC bytes.
     * <p>
     * ADTS, 편집한 MP4, 다채널 오디오와 CRC 바이트를 가진 ADTS 헤더의 파일 디코딩을 확인한다.
     * @throws Exception on I/O or decode failure / 입출력 또는 해독에 실패하면
     */
    public void testAacFilePcmMatchesArrayPcm() throws Exception {
        int count = 0;
        AacDecoder decoder = new AacDecoder();
        // 거부 대상으로 기록된 ALAC를 제외한 모든 ADTS·MP4 음원을 비교한다.
        for (ReferenceFixture fixture : ReferenceFixture.ofGroup("aac")) {
            if (fixture.isRefused()) {
                continue;
            }
            byte[] data = fixture.readFile();
            compareFileSources(decoder, fixture.name, data, true);
            if (fixture.name.equals("aac_adts_44100_mono_24k")) {
                int size = ((data[3] & 3) << 11) | ((data[4] & 255) << 3) | ((data[5] & 255) >> 5);
                byte[] protectedFrame = new byte[data.length + 2];
                System.arraycopy(data, 0, protectedFrame, 0, 7);
                System.arraycopy(data, 7, protectedFrame, 9, data.length - 7);
                protectedFrame[1] &= 0xFE;
                protectedFrame[3] = (byte) ((protectedFrame[3] & 0xFC) | ((size + 2) >> 11));
                protectedFrame[4] = (byte) ((size + 2) >> 3);
                protectedFrame[5] = (byte) ((protectedFrame[5] & 0x1F) | (((size + 2) & 7) << 5));
                compareFileSources(decoder, fixture.name + " with CRC field", protectedFrame, true);
                PcmSource ordinary = decoder.open(data, DecodeLimits.DEFAULT);
                PcmSource withCrc = decoder.open(protectedFrame, DecodeLimits.DEFAULT);
                try {
                    Check.isTrue(Arrays.equals(AudioKit.readAll(ordinary), AudioKit.readAll(withCrc)), "ADTS skips the CRC field before decoding");
                } finally {
                    ordinary.close();
                    withCrc.close();
                }
            }
            count++;
        }
        Check.equal(25, count, "all reference AAC files were checked");
    }

    /**
     * Compares file-backed Ogg Vorbis and Opus including an Opus chain that changes channel layout.
     * <p>
     * 파일에서 Ogg Vorbis와 Opus를 해독해 비교한다. 채널 배치가 바뀌는 Opus 체인도 포함한다.
     * @throws Exception on I/O or decode failure / 입출력 또는 해독에 실패하면
     */
    public void testVorbisAndOpusFilePcmMatchesArrayPcm() throws Exception {
        List<ReferenceFixture> vorbis = ReferenceFixture.ofGroup("vorbis");
        Check.equal(9, vorbis.size(), "Vorbis fixture count");
        VorbisDecoder vorbisDecoder = new VorbisDecoder();
        // 모노부터 7.1까지 파일에서 전체 PCM을 비교한다.
        for (ReferenceFixture fixture : vorbis) {
            compareFileSources(vorbisDecoder, fixture.name, fixture.readFile(), true);
        }
        List<ReferenceFixture> opus = ReferenceFixture.ofGroup("opus");
        Check.equal(12, opus.size(), "Opus fixture count");
        OpusDecoder opusDecoder = new OpusDecoder();
        byte[] stereo = null;
        byte[] mono = null;
        // Opus 모드와 채널 배치마다 PCM을 비교하고 연쇄 시험에 쓸 두 고리를 고른다.
        for (ReferenceFixture fixture : opus) {
            byte[] data = fixture.readFile();
            compareFileSources(opusDecoder, fixture.name, data, true);
            if (fixture.name.equals("opus_audio_128k_48000_st")) {
                stereo = data;
            } else if (fixture.name.equals("opus_voip_16k_16000_mono")) {
                mono = data;
            }
        }
        Check.notNull(stereo, "stereo Opus fixture");
        Check.notNull(mono, "mono Opus fixture");
        compareFileSources(opusDecoder, "Opus chain with a channel layout change", OggMux.concat(stereo, mono), true);
    }

    /**
     * Compares complete PCM, backwards seeking and file ownership with the array path.
     * <p>
     * 전체 PCM과 역탐색을 배열 경로와 비교하고 파일 소유권을 확인한다.
     * @param decoder decoder under test / 시험할 디코더
     * @param label name used in assertions / 검증 메시지의 이름
     * @param data encoded bytes / 인코딩 바이트
     * @param wide whether to check all float channels / 모든 float 채널을 확인할지 여부
     * @throws Exception on I/O or decode failure / 입출력 또는 해독에 실패하면
     */
    private void compareFileSources(ByteAudioDecoder decoder, String label, byte[] data, boolean wide) throws Exception {
        File file = File.createTempFile("swp-reference-audio-", ".data");
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            FileByteSource input = new FileByteSource(file, 31);
            try {
                Check.isTrue(decoder.acceptsSource(input), label + " is recognized on disk");
                PcmSource disk = decoder.openSource(input, DecodeLimits.DEFAULT);
                PcmSource mem = decoder.open(data, DecodeLimits.DEFAULT);
                try {
                    Check.equal(mem.getFrameCount(), disk.getFrameCount(), label + " sample count after trimming");
                    Check.isTrue(Arrays.equals(AudioKit.readAll(mem), AudioKit.readAll(disk)), label + " complete PCM");
                    long target = disk.getFrameCount() / 2;
                    disk.seekToFrame(target);
                    mem.seekToFrame(target);
                    Check.isTrue(Arrays.equals(AudioKit.readAll(mem), AudioKit.readAll(disk)), label + " PCM after seeking back");
                } finally {
                    mem.close();
                    disk.close();
                }
                assertFileInputClosed(input, label);
            } finally {
                input.close();
            }
            if (wide) {
                compareWideFilePcm(decoder, label, data, file);
            }
        } finally {
            Check.isTrue(file.delete(), label + " can be deleted");
        }
    }

    /**
     * Reads all float channels from both input representations. / 두 입력 형태에서 모든 float 채널을 읽는다.
     * @param decoder decoder under test / 시험할 디코더
     * @param label name used in assertions / 검증 메시지의 이름
     * @param data array data / 배열 데이터
     * @param file temporary file / 임시 파일
     * @throws Exception on I/O or decode failure / 입출력 또는 해독에 실패하면
     */
    private void compareWideFilePcm(ByteAudioDecoder decoder, String label, byte[] data, File file) throws Exception {
        FileByteSource input = new FileByteSource(file, 29);
        try {
            FloatPcmSource disk = decoder.openWideSource(input, DecodeLimits.DEFAULT);
            FloatPcmSource mem = decoder.openWide(data, DecodeLimits.DEFAULT);
            try {
                Check.equal(mem.getChannels(), disk.getChannels(), label + " float channel count");
                float[][] expected = new float[mem.getChannels()][211];
                float[][] actual = new float[disk.getChannels()][211];
                // 스트림 끝까지 같은 크기의 분할로 읽어 채널별 표본을 비교한다.
                for (;;) {
                    int frames = mem.read(expected, 0, expected[0].length);
                    Check.equal(frames, disk.read(actual, 0, actual[0].length), label + " float frame count");
                    if (frames < 0) {
                        break;
                    }
                    // 같은 읽기 크기와 기록 순서를 사용하므로 마지막 읽기의 미사용 배열 부분도 같다.
                    for (int ch = 0; ch < expected.length; ch++) {
                        Check.isTrue(Arrays.equals(expected[ch], actual[ch]), label + " float PCM channel " + ch);
                    }
                }
            } finally {
                mem.close();
                disk.close();
            }
            assertFileInputClosed(input, label + " float PCM");
        } finally {
            input.close();
        }
    }

    /**
     * Checks that a decoder closed the file that it owned. / 디코더가 소유한 파일을 닫았는지 확인한다.
     * @param input the input given to the decoder / 디코더에 전달한 입력
     * @param label name used in assertions / 검증 메시지의 이름
     */
    private void assertFileInputClosed(FileByteSource input, String label) {
        try {
            input.get(0);
            Check.fail(label + " PCM close must close its input");
        } catch (ByteSourceException expected) {
            Check.equal(MediaException.Kind.ACCESS_FAILED, expected.toMediaException().getKind(), label + " closed source access");
        }
    }

    /**
     * AAC files of ffmpeg's native encoder: ADTS and MP4, sampling frequency indexes, channel configurations.
     * <p>
     * ffmpeg 기본 인코더가 만든 AAC 파일: ADTS 와 MP4, 샘플링 주파수 인덱스, 채널 구성.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAacMatchesFfmpeg() throws Exception {
        checkGroup("aac");
    }

    /**
     * Ogg Vorbis files of libvorbis.
     * <p>
     * libvorbis 가 만든 Ogg Vorbis 파일.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVorbisMatchesFfmpeg() throws Exception {
        checkGroup("vorbis");
    }

    /**
     * Ogg Opus files of libopus.
     * <p>
     * libopus 가 만든 Ogg Opus 파일.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpusMatchesFfmpeg() throws Exception {
        checkGroup("opus");
    }

    /**
     * WebM files (Vorbis, Opus).
     * <p>
     * WebM 파일(Vorbis, Opus).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWebmMatchesFfmpeg() throws Exception {
        checkGroup("webm");
    }

    /**
     * The decoder is chosen from the signature of the data: the same output without a MIME hint and with a wrong one.
     * <p>
     * 디코더는 데이터의 시그니처로 고른다: MIME 힌트가 없거나 틀려도 출력이 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDecoderIsChosenBySignature() throws Exception {
        List<String> problems = new ArrayList<String>();
        // 시험 자료마다 올바른 힌트, 힌트 없음, 틀린 힌트의 출력을 비교한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                continue;
            }
            short[] a = f.decode(f.mime);
            short[] none = f.decode(null);
            short[] wrong = f.decode(f.mime.equals("audio/wav") ? "audio/mpeg" : "audio/wav");
            if (!java.util.Arrays.equals(a, none) || !java.util.Arrays.equals(a, wrong)) {
                problems.add(f.name);
            }
        }
        Check.isTrue(problems.isEmpty(), "the output depends on the MIME hint for " + problems);
    }

    /**
     * A seek gives exactly the samples that sequential decoding gives from that position (what a player relies on for scrubbing), and the declared frame
     * count equals the decoded one.
     * <p>
     * 탐색하면 그 위치부터 순차 디코딩과 똑같은 샘플이 나온다(재생기가 되감기에서 의존하는 성질). 선언된 프레임 수는 디코딩한 수와 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeekMatchesSequentialDecoding() throws Exception {
        List<String> problems = new ArrayList<String>();
        // 시험 자료마다 여러 위치로 탐색해 순차 결과의 뒷부분과 비교한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused()) {
                continue;
            }
            PcmSource s = AudioDecoders.open(f.readFile(), f.mime, DecodeLimits.DEFAULT);
            try {
                int ch = s.getChannels();
                short[] all = AudioKit.toShorts(AudioKit.readAll(s));
                long frames = all.length / ch;
                if (s.getFrameCount() >= 0 && s.getFrameCount() != frames) {
                    problems.add(f.name + ": getFrameCount()=" + s.getFrameCount() + " but " + frames + " frames were decoded");
                }
                if (!s.isSeekable()) {
                    problems.add(f.name + ": not seekable");
                    continue;
                }
                // 3/4 지점은 mp3_v2_44100_st_gap 에서 작은 VBR 프레임 일곱 개 다음의 큰 프레임이다(비트 저장소를 다시 채우려면 앞 8프레임으로는 모자란다).
                long[] positions = { frames / 3, frames * 2 / 3, Math.max(0, frames - 37), 0, frames / 2 + 1, frames * 3 / 4, frames * 3 / 4 + 1100, frames * 4 / 5 };
                // 위치마다 탐색한 뒤 끝까지 읽고 순차 결과의 같은 구간과 비교한다.
                for (long wanted : positions) {
                    // 짧은 시험 자료에서는 위치가 끝을 넘을 수 있으므로 끝으로 줄인다.
                    long p = Math.min(wanted, frames);
                    s.seekToFrame(p);
                    short[] tail = AudioKit.toShorts(AudioKit.readAll(s));
                    short[] expect = new short[all.length - (int) p * ch];
                    System.arraycopy(all, (int) p * ch, expect, 0, expect.length);
                    if (!java.util.Arrays.equals(tail, expect)) {
                        ReferenceCompare.Diff d = ReferenceCompare.compare(tail, expect);
                        // AAC 의 지각 잡음 대체(PNS)는 순차 디코딩에서 이어지는 난수열로 잡음을 만들어, 중간으로 탐색하면 잡음만 다른 수열이 된다(ffmpeg 도 같다).
                        // 처음(0)으로 탐색하면 새 디코더와 같아야 하고, 그 밖에는 잡음 대역의 차이만 허용한다(신호 대 오차비 25 dB 이상).
                        if (f.group.equals("aac") && p != 0 && d.snr() >= 25.0) {
                            continue;
                        }
                        problems.add(f.name + ": after a seek to frame " + p + " the samples differ (" + tail.length / ch + " frames instead of " + expect.length / ch
                                + "; " + d + ")");
                        break;
                    }
                }
            } finally {
                s.close();
            }
        }
        Check.isTrue(problems.isEmpty(), problems.size() + " problems:\n" + join(problems));
    }

    /**
     * Damaged copies of the real files (cut off, with changed bytes in the middle, with a zeroed block) are either decoded or rejected with a
     * {@link MediaException}; no other exception, no endless loop (every case has a time limit). The damage is made with a fixed seed.
     * <p>
     * 실제 파일을 손상시킨 사본(잘림, 가운데 바이트 변경, 0 으로 채운 블록)은 디코딩되거나 {@link MediaException} 으로 거부되어야 한다. 다른 예외나 무한 반복은 안 된다(사례마다
     * 제한 시간이 있다). 손상은 고정된 씨앗으로 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedRealFilesFailCleanly() throws Exception {
        damaged(0, 2);
    }

    /**
     * Damaged copies with bytes changed in the headers (the first 300 bytes) or anywhere in the file.
     * <p>
     * 헤더(앞쪽 300바이트)나 파일 아무 곳의 바이트를 바꾼 손상 사본.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedHeadersFailCleanly() throws Exception {
        damaged(3, 8);
    }

    /**
     * Decodes damaged copies of all fixtures and fails with the list of the copies that fail with something else than a {@link MediaException}.
     * <p>
     * 모든 시험 자료의 손상 사본을 디코딩하고, {@link MediaException} 이 아닌 것으로 실패하는 사본의 목록과 함께 실패시킨다.
     *
     * @param firstKind first kind of damage (0 cut off, 1 changed bytes in the middle, 2 zeroed block, 3 to 5 changed bytes in the first 300 bytes,
     *                  6 to 8 changed bytes anywhere) / 첫 손상 종류(0 잘림, 1 가운데 바이트 변경, 2 0 블록, 3~5 앞쪽 300바이트 변경, 6~8 아무 곳 변경)
     * @param lastKind  last kind of damage / 마지막 손상 종류
     * @throws Exception on failure / 실패 시
     */
    private void damaged(int firstKind, int lastKind) throws Exception {
        java.util.Random rnd = new java.util.Random(20261118L + firstKind);
        List<String> problems = new ArrayList<String>();
        // 시험 자료마다 정해진 종류의 손상 사본을 디코딩해 본다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            byte[] orig = f.readFile();
            // 손상 종류마다 사본을 만들어 디코딩한다.
            for (int kind = firstKind; kind <= lastKind; kind++) {
                byte[] d = orig.clone();
                String what;
                if (kind == 0) {
                    d = java.util.Arrays.copyOf(orig, Math.max(1, orig.length * 3 / 5));
                    what = "cut off";
                } else if (kind == 1) {
                    // 가운데 20~80% 구간에서 바이트 16개를 무작위로 바꾼다.
                    for (int i = 0; i < 16; i++) {
                        d[orig.length / 5 + rnd.nextInt(Math.max(1, orig.length * 3 / 5))] = (byte) rnd.nextInt(256);
                    }
                    what = "flipped bytes";
                } else if (kind == 2) {
                    int from = orig.length / 2;
                    java.util.Arrays.fill(d, from, Math.min(d.length, from + 64), (byte) 0);
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
                    PcmSource s = AudioDecoders.open(d, f.mime, DecodeLimits.DEFAULT);
                    try {
                        AudioKit.readAll(s);
                    } finally {
                        s.close();
                    }
                } catch (MediaException e) {
                    // 손상을 알리는 정상적인 실패이다.
                    continue;
                } catch (Throwable t) {
                    problems.add(f.name + " (" + what + "): " + t);
                }
            }
        }
        Check.isTrue(problems.isEmpty(), problems.size() + " damaged copies failed with something else than a MediaException:\n" + join(problems));
    }

    /**
     * Formats that ffmpeg decodes but that the product refuses with a clear UNSUPPORTED_FORMAT error (a documented gap, not a wrong output).
     * <p>
     * ffmpeg 는 디코딩하지만 제품이 분명한 UNSUPPORTED_FORMAT 오류로 거부하는 형식(잘못된 출력이 아니라 문서화된 빈 곳).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefusedFormatsAreReportedAsUnsupported() throws Exception {
        List<String> problems = new ArrayList<String>();
        // 거부해야 하는 시험 자료가 UNSUPPORTED_FORMAT 으로 끝나는지 확인한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (!f.isRefused()) {
                continue;
            }
            try {
                AudioDecoders.open(f.readFile(), f.mime, DecodeLimits.DEFAULT).close();
                problems.add(f.name + ": was decoded although it is listed as unsupported");
            } catch (MediaException e) {
                if (e.getKind() != MediaException.Kind.UNSUPPORTED_FORMAT) {
                    problems.add(f.name + ": " + e);
                }
            }
        }
        Check.isTrue(problems.isEmpty(), join(problems));
    }

    /**
     * Makes sure that this class sees the fixtures (an empty index would let every test pass vacuously).
     * <p>
     * 이 클래스가 시험 자료를 보는지 확인한다(색인이 비면 모든 시험이 내용 없이 통과한다).
     *
     * @throws IOException if the index is missing / 색인이 없으면
     */
    public void testIndexIsComplete() throws IOException {
        List<ReferenceFixture> all = ReferenceFixture.loadAll();
        Check.isTrue(all.size() >= 100, "only " + all.size() + " fixtures in the index");
        String[] groups = { "mp3", "aac", "vorbis", "flac", "opus", "wav", "aiff", "au", "webm" };
        // 그룹마다 시험 자료가 있는지 확인한다.
        for (String g : groups) {
            Check.isTrue(ReferenceFixture.ofGroup(g).size() > 0, "no fixture of the group " + g);
        }
        int refused = 0;
        // 거부해야 하는 파일이 있는지 센다(없으면 거부 시험이 내용 없이 통과한다).
        for (ReferenceFixture f : all) {
            if (f.isRefused()) {
                refused++;
            }
        }
        Check.isTrue(refused >= 3, "only " + refused + " fixtures that have to be refused");
    }
}
