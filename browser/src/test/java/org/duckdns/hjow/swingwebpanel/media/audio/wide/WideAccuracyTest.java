package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.reference.ReferenceFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The precision of the wide path of the lossy codecs against 32 bit float references (plan item P25-05): ffmpeg's own decoders (native ones; libopus for
 * Opus) decoded ten of the lossy fixtures to float, without any rounding (<code>wide/*.f32</code>, the first 1500 frames, interleaved, in ffmpeg's channel
 * order; <code>generation/make_wide_refs.sh</code>). The wide path must be at least as close to these floats as the 16 bit path of the same decoder is
 * (rounding to 16 bits only adds error), within a small absolute bound, and in the multichannel files the channels must be the right ones in the right order (a
 * mistake in the order shows as an error as large as the signal). The measured errors are printed.
 * <p>
 * 손실 코덱의 넓은 경로의 정밀도를 32비트 float 참조와 비교한다(계획 P25-05). ffmpeg 자신의 디코더(기본 디코더. Opus 는 libopus)가 손실 시험 자료 열 개를 반올림 없이 float 로
 * 디코딩했다(<code>wide/*.f32</code>, 처음 1500 프레임, 인터리브, ffmpeg 의 채널 순서. <code>generation/make_wide_refs.sh</code>). 넓은 경로는 같은 디코더의 16비트 경로보다 이
 * float 에 더 가깝거나 같아야 하고(16비트로의 반올림은 오차만 더한다) 작은 절대 한도 안이어야 하며, 다채널 파일에서는 채널이 올바른 것이 올바른 순서로 있어야 한다(순서가
 * 틀리면 신호만큼 큰 오차로 드러난다). 측정한 오차를 출력한다.
 */
public class WideAccuracyTest {

    /**
     * The fixtures with a float reference: name, number of channels.
     * <p>
     * float 참조가 있는 시험 자료: 이름, 채널 수.
     */
    private static final Object[][] REFS = {{"mp3_cbr128_44100_js", 2}, {"mp3_v2_32000_mono", 1}, {"aac_adts_44100_st_128k", 2}, {"aac_m4a_48000_mono_64k", 1}, {"aac_adts_16000_6ch", 6},
            {"aac_m4a_16000_8ch", 8}, {"vorbis_q6_48000_webm", 2}, {"opus_audio_128k_48000_st", 2}, {"opus_voip_40k_48000_mono", 1}, {"opus_audio_96k_48000_51", 6}};

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public WideAccuracyTest() {
    }

    /**
     * Reads a resource completely.
     * <p>
     * 자원을 통째로 읽는다.
     *
     * @param name the resource name / 자원 이름
     * @return its bytes / 바이트
     * @throws IOException if it is missing / 없으면
     */
    private static byte[] resource(String name) throws IOException {
        InputStream in = WideAccuracyTest.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("missing test resource " + name);
        }
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[65536];
            int r;
            // 끝까지 읽는다.
            while ((r = in.read(b)) > 0) {
                o.write(b, 0, r);
            }
            return o.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Finds a fixture by name.
     * <p>
     * 이름으로 시험 자료를 찾는다.
     *
     * @param name the name / 이름
     * @return the fixture / 시험 자료
     * @throws IOException if the index cannot be read or has no such fixture / 색인을 읽을 수 없거나 그런 시험 자료가 없으면
     */
    private static ReferenceFixture fixture(String name) throws IOException {
        // 색인에서 이름이 같은 시험 자료를 찾는다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        throw new IOException("no fixture " + name);
    }

    /**
     * Decodes the float reference: little endian 32 bit floats, interleaved.
     * <p>
     * float 참조를 해석한다: 리틀 엔디언 32비트 float, 인터리브.
     *
     * @param b        the bytes / 바이트
     * @param channels the channels / 채널 수
     * @return the samples per channel / 채널별 표본
     */
    private static float[][] floats(byte[] b, int channels) {
        int frames = b.length / (4 * channels);
        float[][] r = new float[channels][frames];
        // 프레임마다 채널의 float 를 읽는다.
        for (int f = 0; f < frames; f++) {
            for (int c = 0; c < channels; c++) {
                int p = (f * channels + c) * 4;
                int bits = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
                r[c][f] = Float.intBitsToFloat(bits);
            }
        }
        return r;
    }

    /**
     * Every fixture with a float reference: the wide path is within a bound of the reference, and for one or two channels it is at least as close to the reference
     * as the 16 bit path (root mean square over all samples); the multichannel files match channel by channel.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testWideIsAtLeastAsCloseToTheFloatReferenceAsThe16BitPath() throws Exception {
        List<String> problems = new ArrayList<String>();
        // 참조가 있는 시험 자료마다 넓은 경로와 16비트 경로의 오차를 잰다.
        for (int i = 0; i < REFS.length; i++) {
            String name = (String) REFS[i][0];
            int ch = ((Integer) REFS[i][1]).intValue();
            ReferenceFixture f = fixture(name);
            float[][] ref = floats(resource("/media/audio/wide/" + name + ".f32"), ch);
            float[][] wide = WideKit.wide(f.readFile(), f.mime);
            if (wide.length != ch) {
                problems.add(name + ": " + wide.length + " channels instead of " + ch);
                continue;
            }
            int n = ref[0].length;
            double wideSq = 0;
            double wideMax = 0;
            // 채널마다 넓은 경로와 참조의 차이를 모은다.
            for (int c = 0; c < ch; c++) {
                for (int k = 0; k < n; k++) {
                    double d = wide[c][k] - ref[c][k];
                    wideSq += d * d;
                    wideMax = Math.max(wideMax, Math.abs(d));
                }
            }
            double wideRms = Math.sqrt(wideSq / (n * ch));
            String line;
            if (ch <= 2) {
                short[] narrow = WideKit.narrow(f.readFile(), f.mime);
                double sq = 0;
                // 16비트 경로의 표본과 참조의 차이를 모은다.
                for (int k = 0; k < n; k++) {
                    for (int c = 0; c < ch; c++) {
                        double d = narrow[k * ch + c] / 32768.0 - ref[c][k];
                        sq += d * d;
                    }
                }
                double narrowRms = Math.sqrt(sq / (n * ch));
                line = String.format("%-28s wide: rms %.3e max %.3e | 16 bit path: rms %.3e (%.2f times)", name, wideRms, wideMax, narrowRms, narrowRms / wideRms);
                if (!(wideRms <= narrowRms)) {
                    problems.add(name + ": the wide path (rms " + wideRms + ") is farther from the float reference than the 16 bit path (rms " + narrowRms + ")");
                }
            } else {
                line = String.format("%-28s wide: rms %.3e max %.3e (%d channels)", name, wideRms, wideMax, ch);
            }
            System.out.println(line);
            // 절대 한도: 16비트 한 단계(1/32768 = 3.05e-5)의 두 배 안이어야 한다.
            if (wideMax > 6.1e-5) {
                problems.add(name + ": the largest difference " + wideMax + " is more than two steps of 16 bits");
            }
        }
        Check.isTrue(problems.isEmpty(), problems.size() + " problems:\n" + join(problems));
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
}
