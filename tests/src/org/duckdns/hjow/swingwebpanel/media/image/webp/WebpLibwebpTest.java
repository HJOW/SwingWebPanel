package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link WebpDecoder} with files made by libwebp 1.6.0 through Pillow (plan item P18-03; FEATURE_MATRIX IMG-05) and compared with the
 * pixels that libwebp itself decodes from them. The fixture directory <code>testdata/webp/libwebp</code> holds the files and
 * <code>manifest.txt</code>; <code>generate_fixtures.py</code> documents how they were made. The set covers the lossy encoder at several
 * qualities and efforts, odd and tiny sizes, the alpha plane in its compressed and filtered forms, the lossless encoder at every effort level
 * (predictors, colour transforms, palettes of every size, colour cache, several prefix code groups), the container variants and animations
 * (libwebp's own encoder and hand-assembled files with every combination of blending, disposal and offsets). Every frame must equal libwebp's
 * canvas byte for byte (hash of the RGBA bytes).
 * <p>
 * libwebp 1.6.0 이 Pillow 로 만든 파일로 하는 {@link WebpDecoder} 시험(계획 P18-03; FEATURE_MATRIX IMG-05). libwebp 가 그 파일에서 직접
 * 디코딩한 픽셀과 비교한다. 자료 디렉터리 <code>testdata/webp/libwebp</code> 에 파일과 <code>manifest.txt</code> 가 있고
 * <code>generate_fixtures.py</code> 가 만든 방법을 기록한다. 손실 인코더의 여러 품질/노력, 홀수와 아주 작은 크기, 압축되고 필터링된 알파 평면,
 * 모든 노력 수준의 무손실 인코더(예측, 색 변환, 모든 크기의 팔레트, 색 캐시, 여러 접두 부호 집합), 컨테이너 변형, 애니메이션(libwebp 자체
 * 인코더와, 합성/처리 방법/오프셋의 모든 조합을 손으로 조립한 파일)을 다룬다. 모든 프레임이 libwebp 의 캔버스와 바이트 단위로 같아야 한다(RGBA
 * 바이트의 해시).
 */
public class WebpLibwebpTest {

    /**
     * Directory of the fixtures in the class path.
     * <p>
     * 클래스패스에서 자료 디렉터리.
     */
    private static final String DIR = "/org/duckdns/hjow/swingwebpanel/testdata/webp/libwebp/";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WebpLibwebpTest() {
    }

    /**
     * Returns the FNV-1a hash of the RGBA bytes of a frame.
     * <p>
     * 프레임의 RGBA 바이트의 FNV-1a 해시를 반환한다.
     *
     * @param frame the frame / 프레임
     * @return the hash / 해시
     */
    private static long frameHash(DecodedFrame frame) {
        int h = 0x811c9dc5;
        int[] argb = frame.getArgb();
        // 픽셀마다 R, G, B, A 순서로 바이트를 섞는다.
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            h = (h ^ ((p >> 16) & 0xff)) * 16777619;
            h = (h ^ ((p >> 8) & 0xff)) * 16777619;
            h = (h ^ (p & 0xff)) * 16777619;
            h = (h ^ (p >>> 24)) * 16777619;
        }
        return h & 0xffffffffL;
    }

    /**
     * One line of the manifest.
     * <p>
     * 목록의 한 줄.
     */
    private static final class Entry {

        /**
         * File name.
         * <p>
         * 파일 이름.
         */
        String file;

        /**
         * Width of the image.
         * <p>
         * 이미지 가로.
         */
        int width;

        /**
         * Height of the image.
         * <p>
         * 이미지 세로.
         */
        int height;

        /**
         * Loop count.
         * <p>
         * 반복 횟수.
         */
        int loop;

        /**
         * Expected delays.
         * <p>
         * 기대 지연.
         */
        List<Integer> delays = new ArrayList<Integer>();

        /**
         * Expected frame hashes.
         * <p>
         * 기대 프레임 해시.
         */
        List<Long> hashes = new ArrayList<Long>();
    }

    /**
     * Reads the manifest.
     * <p>
     * 목록을 읽는다.
     *
     * @return the entries / 항목들
     * @throws IOException if it cannot be read / 읽을 수 없을 때
     */
    private static List<Entry> manifest() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(WebpLibwebpTest.class.getResourceAsStream(DIR + "manifest.txt"), "US-ASCII"));
        List<Entry> out = new ArrayList<Entry>();
        try {
            String line;
            // 줄마다 항목 하나를 만든다.
            while ((line = r.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] p = line.split("\\|");
                Entry e = new Entry();
                e.file = p[0];
                e.width = Integer.parseInt(p[1]);
                e.height = Integer.parseInt(p[2]);
                e.loop = Integer.parseInt(p[3]);
                String[] frames = p[4].split(",");
                // 프레임마다 "지연:해시" 를 읽는다.
                for (int i = 0; i < frames.length; i++) {
                    String[] f = frames[i].split(":");
                    e.delays.add(Integer.valueOf(f[0]));
                    e.hashes.add(Long.valueOf(Long.parseLong(f[1], 16)));
                }
                out.add(e);
            }
        } finally {
            r.close();
        }
        return out;
    }

    /**
     * Decodes every fixture whose name starts with a prefix and compares size, frames, delays, loop count and pixels.
     * <p>
     * 이름이 접두사로 시작하는 모든 자료를 디코딩해 크기, 프레임, 지연, 반복 횟수, 픽셀을 비교한다.
     *
     * @param prefix name prefix / 이름 접두사
     * @return the number of fixtures compared / 비교한 자료 수
     * @throws Exception on failure / 실패 시
     */
    private static int compare(String prefix) throws Exception {
        List<Entry> all = manifest();
        int count = 0;
        StringBuilder failures = new StringBuilder();
        // 접두사가 맞는 파일마다 디코딩해 기준과 비교한다.
        for (int i = 0; i < all.size(); i++) {
            Entry e = all.get(i);
            if (!e.file.startsWith(prefix)) {
                continue;
            }
            count++;
            DecodedImage img;
            try {
                img = new WebpDecoder().decode(WebpFixtures.readResource(DIR + e.file), DecodeLimits.DEFAULT);
            } catch (MediaException ex) {
                failures.append(e.file).append(": ").append(ex.getKind()).append(' ').append(ex.getMessage()).append('\n');
                continue;
            }
            String problem = null;
            if (img.getWidth() != e.width || img.getHeight() != e.height) {
                problem = "size " + img.getWidth() + "x" + img.getHeight() + " instead of " + e.width + "x" + e.height;
            } else if (img.getFrames().size() != e.hashes.size()) {
                problem = img.getFrames().size() + " frames instead of " + e.hashes.size();
            } else if (e.hashes.size() > 1 && img.getLoopCount() != e.loop) {
                problem = "loop count " + img.getLoopCount() + " instead of " + e.loop;
            } else {
                // 프레임마다 지연과 픽셀 해시를 비교한다.
                for (int f = 0; f < e.hashes.size() && problem == null; f++) {
                    DecodedFrame fr = img.getFrames().get(f);
                    if (e.hashes.size() > 1 && fr.getDelayMillis() != e.delays.get(f).intValue()) {
                        problem = "frame " + f + " delay " + fr.getDelayMillis() + " instead of " + e.delays.get(f);
                    } else if (frameHash(fr) != e.hashes.get(f).longValue()) {
                        problem = "frame " + f + " pixels differ";
                    }
                }
            }
            if (problem != null) {
                failures.append(e.file).append(": ").append(problem).append('\n');
            }
        }
        Check.isTrue(count > 0, "fixtures with prefix " + prefix);
        if (failures.length() > 0) {
            Check.fail(failures.toString());
        }
        return count;
    }

    /**
     * Lossy images without alpha equal libwebp's output: every effort level, qualities from 5 to 100, odd and tiny sizes, flat and noisy content.
     * <p>
     * 알파가 없는 손실 영상이 libwebp 의 출력과 같다: 모든 노력 수준, 품질 5~100, 홀수와 아주 작은 크기, 단색과 잡음 내용.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLossyMatchesLibwebp() throws Exception {
        compare("lossy_photo");
        compare("lossy_flat");
        compare("lossy_ffmpeg");
        compare("lossy_stripes");
        compare("lossy_noise");
        compare("lossy_pattern");
    }

    /**
     * Lossy images with an alpha plane (uncompressed, compressed, each prediction filter, reduced alpha quality) equal libwebp's output.
     * <p>
     * 알파 평면이 있는 손실 영상(무압축, 압축, 각 예측 필터, 낮춘 알파 품질)이 libwebp 의 출력과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLossyAlphaMatchesLibwebp() throws Exception {
        compare("lossy_alpha");
    }

    /**
     * Lossless images (predictors, colour transforms, palettes with and without pixel bundling, colour cache, prefix code groups) equal libwebp's
     * output.
     * <p>
     * 무손실 영상(예측, 색 변환, 픽셀 묶음이 있거나 없는 팔레트, 색 캐시, 접두 부호 집합)이 libwebp 의 출력과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLosslessMatchesLibwebp() throws Exception {
        compare("lossless_");
    }

    /**
     * Container variants (simple files, extended header with profile, EXIF, XMP and unknown chunks) equal libwebp's output.
     * <p>
     * 컨테이너 변형(단순 파일, 프로파일/EXIF/XMP/알 수 없는 청크가 있는 확장 헤더)이 libwebp 의 출력과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testContainersMatchLibwebp() throws Exception {
        compare("container_");
    }

    /**
     * Animations equal the canvases of libwebp's animation decoder, including delays and loop counts.
     * <p>
     * 애니메이션이 libwebp 애니메이션 디코더의 캔버스와 같다(지연과 반복 횟수 포함).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnimationsMatchLibwebp() throws Exception {
        compare("anim_");
    }
}
