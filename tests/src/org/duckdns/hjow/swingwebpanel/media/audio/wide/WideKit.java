package org.duckdns.hjow.swingwebpanel.media.audio.wide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.AudioDecoders;
import org.duckdns.hjow.swingwebpanel.media.audio.FloatPcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmConverter;
import org.duckdns.hjow.swingwebpanel.media.audio.PcmSource;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the tests of the wide decoding path (plan item P25-05): reading a whole {@link FloatPcmSource} in reads of odd sizes (so that the
 * block boundaries of the decoders are crossed at arbitrary places), reducing floats to 16 bit exactly as the 16 bit path does, and the 16 bit
 * path itself. The class is not a suite (its name does not end with <code>Test</code>).
 * <p>
 * 넓은 디코딩 경로 시험의 도우미(계획 P25-05): 홀수 크기의 읽기로 {@link FloatPcmSource} 전체를 읽기(디코더의 블록 경계를 임의의 자리에서 넘도록), float 를 16비트
 * 경로와 똑같이 16비트로 줄이기, 16비트 경로 자체. 이 클래스는 스위트가 아니다(이름이 <code>Test</code> 로 끝나지 않는다).
 */
final class WideKit {

    /**
     * The families (the <code>group</code> column of the fixture index) whose decoders have a wide path of their own; the others are used through
     * the 16 bit path, which gives at most two channels.
     * <p>
     * 자기 넓은 경로를 가진 디코더의 계열(시험 자료 색인의 <code>group</code> 열). 나머지는 최대 채널 둘을 주는 16비트 경로로 쓴다.
     */
    static final java.util.Set<String> WIDE_GROUPS = new java.util.HashSet<String>(Arrays.asList("wav", "aiff", "au", "flac", "vorbis", "opus", "webm"));

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private WideKit() {
    }

    /**
     * Reads a source to its end in reads of 777 frames.
     * <p>
     * 소스를 777 프레임씩 끝까지 읽는다.
     *
     * @param s the source (not closed) / 소스(닫지 않는다)
     * @return one array per channel with all samples / 모든 표본을 담은 채널별 배열
     * @throws MediaException if a read fails / 읽기가 실패하면
     */
    static float[][] readAll(FloatPcmSource s) throws MediaException {
        int ch = s.getChannels();
        List<float[][]> parts = new ArrayList<float[][]>();
        int total = 0;
        float[][] buf = new float[ch][777];
        // 끝(-1)이 나올 때까지 읽는다.
        for (;;) {
            int n = s.read(buf, 0, 777);
            if (n < 0) {
                break;
            }
            float[][] part = new float[ch][];
            // 채널마다 처리한다.
            for (int c = 0; c < ch; c++) {
                part[c] = Arrays.copyOf(buf[c], n);
            }
            parts.add(part);
            total += n;
        }
        float[][] all = new float[ch][total];
        int pos = 0;
        // 읽은 조각을 채널별로 이어 붙인다.
        for (float[][] part : parts) {
            for (int c = 0; c < ch; c++) {
                System.arraycopy(part[c], 0, all[c], pos, part[c].length);
            }
            pos += part[0].length;
        }
        return all;
    }

    /**
     * Opens the wide path of data and reads it all.
     * <p>
     * 데이터의 넓은 경로를 열어 모두 읽는다.
     *
     * @param data the file / 파일
     * @param mime the MIME type hint or null / MIME 타입 힌트 또는 null
     * @return the samples per channel / 채널별 표본
     * @throws MediaException if opening or reading fails / 열기나 읽기가 실패하면
     */
    static float[][] wide(byte[] data, String mime) throws MediaException {
        FloatPcmSource s = AudioDecoders.openWide(data, mime, DecodeLimits.DEFAULT);
        try {
            return readAll(s);
        } finally {
            s.close();
        }
    }

    /**
     * Decodes data through the 16 bit path.
     * <p>
     * 데이터를 16비트 경로로 디코딩한다.
     *
     * @param data the file / 파일
     * @param mime the MIME type hint or null / MIME 타입 힌트 또는 null
     * @return the interleaved 16 bit samples / 인터리브된 16비트 표본
     * @throws MediaException if opening or reading fails / 열기나 읽기가 실패하면
     */
    static short[] narrow(byte[] data, String mime) throws MediaException {
        PcmSource s = AudioDecoders.open(data, mime, DecodeLimits.DEFAULT);
        try {
            return AudioKit.toShorts(AudioKit.readAll(s));
        } finally {
            s.close();
        }
    }

    /**
     * Reduces floats to interleaved 16 bit samples with the conversion of the 16 bit path ({@link PcmConverter#clip16(float)}).
     * <p>
     * float 를 16비트 경로의 변환({@link PcmConverter#clip16(float)})으로 인터리브된 16비트 표본으로 줄인다.
     *
     * @param w one array per channel / 채널별 배열
     * @return the interleaved samples / 인터리브된 표본
     */
    static short[] toShorts(float[][] w) {
        int ch = w.length;
        short[] r = new short[w[0].length * ch];
        // 프레임마다 채널의 값을 16비트로 줄여 인터리브한다.
        for (int f = 0; f < w[0].length; f++) {
            for (int c = 0; c < ch; c++) {
                r[f * ch + c] = PcmConverter.clip16(w[c][f]);
            }
        }
        return r;
    }

    /**
     * Checks that the samples are exactly the expected floats.
     * <p>
     * 표본이 기대한 float 와 정확히 같은지 검사한다.
     *
     * @param what     the description / 설명
     * @param expected the expected samples per channel / 기대하는 채널별 표본
     * @param actual   the decoded samples per channel / 디코딩한 채널별 표본
     */
    static void assertSame(String what, float[][] expected, float[][] actual) {
        Check.equal(expected.length, actual.length, what + ": channels");
        Check.equal(expected[0].length, actual[0].length, what + ": frames");
        // 채널의 모든 표본이 비트 단위로 같은지 확인한다.
        for (int c = 0; c < expected.length; c++) {
            for (int f = 0; f < expected[c].length; f++) {
                if (Float.floatToIntBits(expected[c][f]) != Float.floatToIntBits(actual[c][f])) {
                    Check.fail(what + ": channel " + c + " frame " + f + " is " + actual[c][f] + " instead of " + expected[c][f]);
                }
            }
        }
    }
}
