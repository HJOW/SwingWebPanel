package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.reference.ReferenceFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The wide path against the 16 bit path on every file of the fixture set (plan item P25-05): both come from the same decoders, so for a source
 * with at most two channels the floats of the wide path, reduced to 16 bit by the conversion of the 16 bit path, must be the very samples that the
 * 16 bit path delivers, bit for bit (a lossless file gives exactly the 16 bit samples that ffmpeg's decoder gives, which
 * <code>ReferenceDecodeTest</code> checks for the 16 bit path; a lossy codec gives the same decoded float before the rounding). The wide path
 * must also deliver the same number of frames, the channels of the file where the format has a wide path (the families in
 * {@link #WIDE_GROUPS}) and the frame count that the 16 bit path reports.
 * <p>
 * 시험 자료 모든 파일에서 넓은 경로를 16비트 경로와 비교한다(계획 P25-05). 둘은 같은 디코더에서 나오므로 채널이 둘 이하인 소스에서는 넓은 경로의 float 를 16비트
 * 경로의 변환으로 16비트로 줄인 것이 16비트 경로가 주는 표본과 비트 단위로 같아야 한다(무손실 파일은 ffmpeg 의 디코더가 주는 16비트 표본과 정확히 같으며 16비트 경로에
 * 대해서는 <code>ReferenceDecodeTest</code> 가 이를 확인한다. 손실 코덱은 반올림 전의 같은 디코딩 float 를 준다). 넓은 경로는 프레임 수도 같아야 하고,
 * 넓은 경로가 있는 형식({@link #WIDE_GROUPS} 의 계열)에서는 파일의 채널을 주어야 하며 16비트 경로가 알리는 프레임 수와 같아야 한다.
 */
public class WideEqualsNarrowTest {

    /**
     * The families whose decoders have a wide path of their own (the others are used through the 16 bit path, which gives at most two channels).
     * <p>
     * 자기 넓은 경로를 가진 디코더의 계열(나머지는 최대 채널 둘을 주는 16비트 경로로 쓴다).
     */
    private static final Set<String> WIDE_GROUPS = new HashSet<String>(Arrays.asList("wav", "aiff", "au"));

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public WideEqualsNarrowTest() {
    }

    /**
     * Every file with at most two channels: the wide path reduced to 16 bit is the 16 bit path, bit for bit, with the same number of frames.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testWideReducedTo16BitIsTheNarrowPath() throws Exception {
        List<String> problems = new ArrayList<String>();
        int checked = 0;
        // 시험 자료마다 두 경로로 디코딩해 비교한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused() || f.channels > 2) {
                continue;
            }
            checked++;
            byte[] data = f.readFile();
            short[] narrow = WideKit.narrow(data, f.mime);
            float[][] wide = WideKit.wide(data, f.mime);
            if (wide.length != f.channels) {
                problems.add(f.name + ": " + wide.length + " channels instead of " + f.channels);
                continue;
            }
            if (wide[0].length != narrow.length / f.channels) {
                problems.add(f.name + ": " + wide[0].length + " frames instead of " + narrow.length / f.channels);
                continue;
            }
            if (!Arrays.equals(WideKit.toShorts(wide), narrow)) {
                problems.add(f.name + ": the samples differ from the 16 bit path");
            }
        }
        Check.isTrue(checked > 90, "the fixtures were found: " + checked);
        Check.isTrue(problems.isEmpty(), problems.size() + " of " + checked + " fixtures differ:\n" + join(problems));
    }

    /**
     * The families with a wide path give every channel of the file (the 16 bit path mixes more than two channels down) and report the frame
     * count of the 16 bit path; the number of frames delivered is the same as in the 16 bit path.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testWideGivesTheChannelsOfTheFile() throws Exception {
        List<String> problems = new ArrayList<String>();
        int seen = 0;
        // 넓은 경로가 있는 계열의 시험 자료마다 채널 수와 프레임 수를 확인한다.
        for (ReferenceFixture f : ReferenceFixture.loadAll()) {
            if (f.isRefused() || !WIDE_GROUPS.contains(f.group)) {
                continue;
            }
            byte[] data = f.readFile();
            FloatPcmSource s = AudioDecoders.openWide(data, f.mime, DecodeLimits.DEFAULT);
            try {
                if (s.getChannels() != f.channels) {
                    problems.add(f.name + ": " + s.getChannels() + " channels instead of " + f.channels);
                    continue;
                }
                if (f.channels > 2) {
                    seen++;
                }
                float[][] all = WideKit.readAll(s);
                if (all[0].length != f.expected) {
                    problems.add(f.name + ": " + all[0].length + " frames instead of " + f.expected);
                }
                if (s.getFrameCount() >= 0 && s.getFrameCount() != all[0].length) {
                    problems.add(f.name + ": getFrameCount() is " + s.getFrameCount() + " but " + all[0].length + " frames were delivered");
                }
            } finally {
                s.close();
            }
        }
        System.out.println("multichannel fixtures of the wide families: " + seen);
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
