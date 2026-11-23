package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The lenient handling of cut-off and damaged data by <code>decodeAudioData</code> (plan item P25-05): a file that is cut off or damaged after frames
 * were decoded is not an <code>EncodingError</code>; the frames before the damage are the result, as in a browser, and the result says that it is only a part.
 * The files are 2 seconds of a synthetic stereo signal in Vorbis, Opus, FLAC (native and in Ogg), MP3 and AAC
 * (<code>tests/resources/media/audio/trunc</code>, <code>generation/make_trunc_fixtures.sh</code>), long enough to have many pages and frames; the test cuts them
 * at 70, 85, 95 and 99 percent and zeroes a block in the middle. Expectations: it decodes; the frames that it delivers are exactly the first frames of the
 * decoding of the whole file (the decoders are deterministic, so a partial result is a true prefix, apart from the last block, whose overlap with the next
 * one is missing); a longer cut never gives fewer frames; the formats whose header announces the length (FLAC) mark the result as truncated; a file that is
 * cut inside its headers is still an <code>EncodingError</code>. The table of what every format gives is printed.
 * <p>
 * <code>decodeAudioData</code> 가 잘리거나 손상된 데이터를 관대하게 다루는 것의 시험(계획 P25-05). 프레임을 디코딩한 뒤에 잘리거나 손상된 파일은 <code>EncodingError</code> 가
 * 아니다. 브라우저처럼 손상 앞의 프레임이 결과이고 결과가 일부뿐임을 알린다. 파일은 합성 스테레오 신호 2초를 Vorbis, Opus, FLAC(네이티브와 Ogg 안), MP3, AAC 로 만든 것으로
 * (<code>tests/resources/media/audio/trunc</code>, <code>generation/make_trunc_fixtures.sh</code>) 페이지와 프레임이 많을 만큼 길다. 시험이 70, 85, 95, 99 퍼센트에서 자르고 가운데 한 블록을 0 으로
 * 만든다. 기대: 디코딩된다. 내는 프레임은 파일 전체를 디코딩한 것의 첫 프레임과 정확히 같다(디코더가 결정적이므로 일부 결과는 진짜 앞부분이다. 다음 블록과의 겹침이 없는 마지막 블록은
 * 예외). 더 길게 자르면 프레임이 줄지 않는다. 머리에 길이를 알리는 형식(FLAC)은 결과를 잘린 것으로 표시한다. 머리 안에서 잘린 파일은 여전히 <code>EncodingError</code> 이다. 형식마다 무엇을
 * 주는지 표를 출력한다.
 */
public class TruncatedDataTest {

    /**
     * The files: name, sample rate, whether the header announces the length (so that a cut-off copy is recognised as truncated).
     * <p>
     * 파일: 이름, 표본율, 머리가 길이를 알리는지(그래서 잘린 사본을 잘린 것으로 알아보는지).
     */
    private static final Object[][] FILES = {{"long_vorbis_44100_st.ogg", 44100, Boolean.FALSE}, {"long_opus_48000_st.opus", 48000, Boolean.FALSE},
            {"long_flac_8000_st.flac", 8000, Boolean.TRUE}, {"long_oggflac_8000_st.oga", 8000, Boolean.FALSE}, {"long_mp3_44100_st.mp3", 44100, Boolean.FALSE},
            {"long_aac_44100_st.aac", 44100, Boolean.FALSE}};

    /**
     * The cut points in percent.
     * <p>
     * 자르는 위치(퍼센트).
     */
    private static final int[] CUTS = {70, 85, 95, 99};

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public TruncatedDataTest() {
    }

    /**
     * Reads a resource completely.
     * <p>
     * 자원을 통째로 읽는다.
     *
     * @param name the file name below <code>trunc</code> / <code>trunc</code> 아래의 파일 이름
     * @return its bytes / 바이트
     * @throws IOException if it is missing / 없으면
     */
    static byte[] resource(String name) throws IOException {
        InputStream in = TruncatedDataTest.class.getResourceAsStream("/media/audio/trunc/" + name);
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
     * Checks that the first frames of a partial result are the frames of the whole decoding, except the last <code>margin</code> frames.
     * <p>
     * 일부 결과의 첫 프레임이 전체 디코딩의 프레임과 같은지 검사한다. 마지막 <code>margin</code> 프레임은 제외한다.
     *
     * @param what    the description / 설명
     * @param partial the partial result / 일부 결과
     * @param whole   the whole decoding / 전체 디코딩
     * @param margin  the number of frames at the end that may differ / 끝에서 달라도 되는 프레임 수
     */
    private static void assertPrefix(String what, AudioBuffer partial, AudioBuffer whole, int margin) {
        Check.equal(whole.getNumberOfChannels(), partial.getNumberOfChannels(), what + ": channels");
        int n = Math.max(0, partial.getLength() - margin);
        Check.isTrue(partial.getLength() <= whole.getLength(), what + ": longer than the whole decoding: " + partial.getLength() + " > " + whole.getLength());
        // 채널마다 앞부분이 전체 디코딩과 정확히 같은지 확인한다.
        for (int c = 0; c < partial.getNumberOfChannels(); c++) {
            float[] a = partial.getChannelData(c);
            float[] b = whole.getChannelData(c);
            // 항목마다 반복해서 처리한다.
            for (int i = 0; i < n; i++) {
                if (a[i] != b[i]) {
                    Check.fail(what + ": channel " + c + " frame " + i + " is " + a[i] + " but the whole decoding has " + b[i]);
                }
            }
        }
    }

    /**
     * Every file cut at four points: it decodes, the frames are a true prefix of the whole decoding (apart from the last block), a longer cut never gives
     * fewer frames, and a format whose header announces the length gives a result that is marked truncated and carries a warning. The whole file is not marked.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testCutOffFilesGiveTheirFirstFrames() throws Exception {
        List<String> problems = new ArrayList<String>();
        StringBuilder table = new StringBuilder();
        // 파일마다 전체와 여러 위치에서 자른 사본을 디코딩한다.
        for (int i = 0; i < FILES.length; i++) {
            String name = (String) FILES[i][0];
            float rate = ((Integer) FILES[i][1]).floatValue();
            boolean announces = ((Boolean) FILES[i][2]).booleanValue();
            byte[] data = resource(name);
            DecodeResult whole = AudioBufferDecoder.decodeResult(data, null, rate, null);
            Check.isFalse(whole.isTruncated(), name + ": the whole file is not marked");
            Check.isNull(whole.getWarning(), name + ": the whole file has no warning");
            Check.equal(2, whole.getBuffer().getNumberOfChannels(), name + ": channels");
            table.append(String.format("%-26s whole %6d frames", name, whole.getBuffer().getLength()));
            int previous = 0;
            // 자르는 위치마다 결과를 확인한다.
            for (int k = 0; k < CUTS.length; k++) {
                byte[] cut = Arrays.copyOf(data, data.length * CUTS[k] / 100);
                String what = name + " cut at " + CUTS[k] + " %";
                try {
                    DecodeResult r = AudioBufferDecoder.decodeResult(cut, null, rate, null);
                    int len = r.getBuffer().getLength();
                    table.append(String.format("  %d%%: %6d%s", CUTS[k], len, r.isTruncated() ? " T" : "  "));
                    assertPrefix(what, r.getBuffer(), whole.getBuffer(), 4608);
                    if (len < previous) {
                        problems.add(what + ": a longer cut gave fewer frames (" + len + " < " + previous + ")");
                    }
                    previous = len;
                    if (announces && !r.isTruncated()) {
                        problems.add(what + ": the header announces the length but the result is not marked truncated");
                    }
                    if (r.isTruncated() && (r.getWarning() == null || r.getWarning().length() == 0)) {
                        problems.add(what + ": truncated without a warning");
                    }
                    if (CUTS[k] >= 95 && len < whole.getBuffer().getLength() / 2) {
                        problems.add(what + ": only " + len + " of " + whole.getBuffer().getLength() + " frames");
                    }
                } catch (AudioException e) {
                    table.append(String.format("  %d%%: error", CUTS[k]));
                    problems.add(what + ": " + e);
                } catch (RuntimeException e) {
                    problems.add(what + ": " + e);
                }
            }
            table.append('\n');
        }
        System.out.print(table);
        Check.isTrue(problems.isEmpty(), problems.size() + " problems:\n" + join(problems));
    }

    /**
     * A block of 300 zero bytes in the middle of every file: the decoding does not fail (a decoder either conceals the damage and delivers everything, or
     * ends there and delivers the frames before it with the result marked truncated); whatever is delivered up to the damage is the true beginning of the file.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testADamagedBlockInTheMiddle() throws Exception {
        List<String> problems = new ArrayList<String>();
        StringBuilder table = new StringBuilder();
        // 파일마다 가운데 300 바이트를 0 으로 만든 사본을 디코딩한다.
        for (int i = 0; i < FILES.length; i++) {
            String name = (String) FILES[i][0];
            float rate = ((Integer) FILES[i][1]).floatValue();
            byte[] data = resource(name);
            DecodeResult whole = AudioBufferDecoder.decodeResult(data, null, rate, null);
            byte[] bad = data.clone();
            Arrays.fill(bad, data.length / 2, data.length / 2 + 300, (byte) 0);
            try {
                DecodeResult r = AudioBufferDecoder.decodeResult(bad, null, rate, null);
                // 손상 앞까지는 원래 파일의 앞부분과 같아야 한다. 처음 달라지는 프레임을 찾아 전체 길이의 5% 이후인지 확인한다(손상은 파일의 가운데이지만 머리가 큰 파일은 오디오의
                // 앞쪽에 손상이 있다).
                int firstDiff = Math.min(r.getBuffer().getLength(), whole.getBuffer().getLength());
                // 채널마다 처음 달라지는 프레임을 찾는다.
                for (int c = 0; c < 2; c++) {
                    float[] a = r.getBuffer().getChannelData(c);
                    float[] b = whole.getBuffer().getChannelData(c);
                    // 바이트마다 처리한다.
                    for (int k = 0; k < firstDiff; k++) {
                        if (a[k] != b[k]) {
                            firstDiff = k;
                            break;
                        }
                    }
                }
                table.append(String.format("%-26s damaged block: %6d of %6d frames%s, the first difference at frame %d%n", name, r.getBuffer().getLength(), whole.getBuffer().getLength(),
                        r.isTruncated() ? ", truncated" : "", firstDiff));
                if (firstDiff < whole.getBuffer().getLength() / 20) {
                    problems.add(name + ": the beginning is already different at frame " + firstDiff);
                }
            } catch (AudioException e) {
                problems.add(name + ": " + e);
            }
        }
        System.out.print(table);
        Check.isTrue(problems.isEmpty(), problems.size() + " problems:\n" + join(problems));
    }

    /**
     * A file that is cut off inside its headers has no frame to deliver and stays an <code>EncodingError</code> with the cause, in every format.
     *
     * @throws Exception if a fixture cannot be read / 시험 자료를 읽을 수 없으면
     */
    public void testCutInsideTheHeadersIsStillAnError() throws Exception {
        // 파일마다 머리 안(앞 40 바이트)에서 자른 사본을 확인한다.
        for (int i = 0; i < FILES.length; i++) {
            String name = (String) FILES[i][0];
            byte[] data = resource(name);
            try {
                AudioBufferDecoder.decode(Arrays.copyOf(data, 40), null, 44100f, null);
                Check.fail(name + ": cut inside the headers gave a result");
            } catch (AudioException e) {
                Check.equal(AudioException.Kind.ENCODING, e.getKind(), name + ": the kind");
                Check.notNull(e.getCause(), name + ": the cause");
            }
        }
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
