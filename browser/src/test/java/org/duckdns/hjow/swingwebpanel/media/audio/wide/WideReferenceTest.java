package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.audio.reference.ReferenceFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The wide path against the PCM that ffmpeg's own decoders produced, channel by channel (plan item P25-05): the files with more than two channels
 * (3, 4, 6 and 8 channels in WAV, AIFF, FLAC, AAC, Vorbis and Opus), whose references (<code>ref/*.N.flac</code>) are one 16 bit file per channel in the
 * native channel order of ffmpeg. The wide path must deliver the same number of channels in the same order (an error in the order shows as an error as
 * large as the signal), and the values must agree: exactly for 16 bit lossless sources, within the rounding of the reference for 24 bit ones, within
 * the tolerance that the 16 bit tests of the same files use for lossy codecs. The comparison runs over the families that have a wide path
 * ({@link WideKit#WIDE_GROUPS}).
 * <p>
 * 넓은 경로를 ffmpeg 자신의 디코더가 만든 PCM 과 채널마다 비교한다(계획 P25-05). 채널이 둘보다 많은 파일(WAV, AIFF, FLAC, AAC, Vorbis, Opus 의 3, 4, 6, 8채널)의
 * 참조(<code>ref/*.N.flac</code>)는 ffmpeg 고유의 채널 순서로 채널마다 하나인 16비트 파일이다. 넓은 경로는 같은 순서로 같은 수의 채널을 주어야 하며(순서가 틀리면
 * 신호만큼 큰 오차로 드러난다) 값이 일치해야 한다. 16비트 무손실 소스는 정확히, 24비트는 참조의 반올림 안에서, 손실 코덱은 같은 파일의 16비트 시험이 쓰는 허용 오차
 * 안에서. 비교는 넓은 경로가 있는 계열({@link WideKit#WIDE_GROUPS})에서 한다.
 */
public class WideReferenceTest {

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public WideReferenceTest() {
    }

    /**
     * The largest difference and the root mean square of the difference between the 16 bit reduction of the wide samples and the reference, for one
     * channel, over the frames that both have.
     * <p>
     * 한 채널에서 넓은 경로 표본을 16비트로 줄인 것과 참조의 차이의 최댓값과 제곱 평균 제곱근. 둘 다 가진 프레임에서 잰다.
     *
     * @param w   the wide samples of the channel / 채널의 넓은 경로 표본
     * @param ref the interleaved reference / 인터리브된 참조
     * @param ch  the number of channels of the reference / 참조의 채널 수
     * @param c   the channel / 채널
     * @return <code>{max, rms}</code> / <code>{최댓값, rms}</code>
     */
    private static double[] diff(float[] w, short[] ref, int ch, int c) {
        int n = Math.min(w.length, ref.length / ch);
        double max = 0;
        double sum = 0;
        // 두 신호가 모두 있는 프레임마다 차이를 구한다.
        for (int i = 0; i < n; i++) {
            double d = Math.abs(org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter.clip16(w[i]) - ref[i * ch + c]);
            max = Math.max(max, d);
            sum += d * d;
        }
        return new double[] {max, Math.sqrt(sum / Math.max(1, n))};
    }

    /**
     * Every multichannel fixture of the wide families, every channel: the number of channels, their order and their values.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testMultichannelFilesMatchTheReferenceChannelByChannel() throws Exception {
        List<String> problems = new ArrayList<String>();
        int checked = 0;
        // 넓은 경로가 있는 계열의 채널이 셋 이상인 시험 자료마다 채널별로 참조와 비교한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused() || f.channels <= 2 || !WideKit.WIDE_GROUPS.contains(f.group) || f.isMd5()) {
                continue;
            }
            checked++;
            float[][] w = WideKit.wide(f.readFile(), f.mime);
            if (w.length != f.channels) {
                problems.add(f.name + ": " + w.length + " channels instead of " + f.channels);
                continue;
            }
            short[] ref = f.readReferencePcm();
            double maxAbs = f.kind.equals("exact") ? (f.name.endsWith("_24") ? 1 : 0) : 2;
            double maxRms = f.kind.equals("exact") ? (f.name.endsWith("_24") ? 0.45 : 0.0) : 0.6;
            StringBuilder line = new StringBuilder();
            // 채널마다 참조와의 차이를 잰다.
            for (int c = 0; c < f.channels; c++) {
                double[] d = diff(w[c], ref, f.channels, c);
                line.append(String.format(" ch%d max=%.0f rms=%.3f", c, d[0], d[1]));
                if (d[0] > maxAbs || d[1] > maxRms) {
                    problems.add(f.name + " channel " + c + " (" + f.roles[c] + "): max " + d[0] + ", rms " + d[1] + " exceed " + maxAbs + ", " + maxRms);
                }
            }
            System.out.println(String.format("%-28s %-6s%s", f.name, f.kind, line));
        }
        Check.isTrue(checked >= 6, "the multichannel fixtures were found: " + checked);
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
