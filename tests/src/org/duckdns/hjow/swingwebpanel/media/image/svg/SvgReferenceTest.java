package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Compares the SVG renderer with Chrome (plan item P18-04; FEATURE_MATRIX IMG-07). Every document under <code>/media/svg/cases</code> was drawn
 * by Chrome 156 as an image (the SVG loaded in an <code>img</code> element and drawn onto a canvas) and the pixels are stored as PNG under
 * <code>/media/svg/refs</code>. The test decodes the same document with {@link SvgDecoder} and compares the result pixel by pixel after
 * compositing both over white and over black: the mean of the largest channel difference and the share of pixels that differ by more than 40
 * must stay under the limits of <code>/media/svg/index.txt</code> (derived from the measured values with headroom, because the anti-aliasing
 * of Java2D and Skia differs at the edges). The size of the image must be identical. Text cases need the Windows fonts that Chrome used
 * (Arial, Times New Roman, Courier New) and are skipped, with a reason, if they are not installed.
 * <p>
 * SVG 렌더러를 Chrome 과 비교한다(계획 P18-04; FEATURE_MATRIX IMG-07). <code>/media/svg/cases</code> 의 모든 문서는 Chrome 156 이 이미지로 그린 것
 * (<code>img</code> 요소에 SVG 를 읽어 캔버스에 그림)이며 픽셀을 <code>/media/svg/refs</code> 에 PNG 로 저장했다. 이 시험은 같은 문서를
 * {@link SvgDecoder} 로 디코딩해 흰색 위와 검은색 위에 합성한 뒤 픽셀 단위로 비교한다: 가장 큰 채널 차이의 평균과 40 넘게 다른 픽셀의 비율이
 * <code>/media/svg/index.txt</code> 의 제한 아래여야 한다(제한은 측정값에 여유를 둔 값이다. Java2D 와 Skia 의 안티앨리어싱이 가장자리에서 다르기
 * 때문이다). 이미지 크기는 같아야 한다. 문자 사례는 Chrome 이 쓴 Windows 글꼴(Arial, Times New Roman, Courier New)이 필요하며 없으면 이유와 함께
 * 건너뛴다.
 */
public class SvgReferenceTest {

    /**
     * One line of the index: a case with its limits.
     * <p>
     * 색인의 한 줄: 제한을 가진 사례 하나.
     */
    private static final class Entry {

        /**
         * The case name.
         * <p>
         * 사례 이름.
         */
        String name;

        /**
         * The limit of the mean error.
         * <p>
         * 평균 오차의 제한.
         */
        double maxMean;

        /**
         * The limit of the share of bad pixels in percent.
         * <p>
         * 틀린 픽셀 비율의 제한(퍼센트).
         */
        double maxBad;
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgReferenceTest() {
    }

    /**
     * Reads the index of the cases.
     * <p>
     * 사례 색인을 읽는다.
     *
     * @return the entries / 항목
     * @throws IOException if the index cannot be read / 색인을 읽을 수 없을 때
     */
    private static List<Entry> index() throws IOException {
        byte[] data = SvgTestKit.resource("/media/svg/index.txt");
        Check.notNull(data, "the index of the reference cases is missing");
        BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), "UTF-8"));
        List<Entry> list = new ArrayList<Entry>();
        String line;
        // 주석이 아닌 줄을 이름과 제한 두 개로 나눈다.
        while ((line = r.readLine()) != null) {
            if (line.startsWith("#") || line.trim().length() == 0) {
                continue;
            }
            String[] t = line.trim().split("\\s+");
            Entry e = new Entry();
            e.name = t[0];
            e.maxMean = Double.parseDouble(t[1]);
            e.maxBad = Double.parseDouble(t[2]);
            list.add(e);
        }
        return list;
    }

    /**
     * Tells whether the fonts that the Chrome renderings of the text cases used are installed.
     * <p>
     * 문자 사례의 Chrome 렌더링이 쓴 글꼴이 설치되어 있는지 알려 준다.
     *
     * @return true if Arial, Times New Roman and Courier New exist / Arial, Times New Roman, Courier New 가 있으면 true
     */
    private static boolean fontsInstalled() {
        Set<String> have = new HashSet<String>();
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH);
        // 설치된 계열 이름을 소문자로 모은다.
        for (int i = 0; i < names.length; i++) {
            have.add(names[i].toLowerCase(Locale.ENGLISH));
        }
        return have.containsAll(Arrays.asList("arial", "times new roman", "courier new"));
    }

    /**
     * Compares all cases whose name starts with one of the prefixes with their Chrome renderings.
     * <p>
     * 이름이 접두사 중 하나로 시작하는 모든 사례를 Chrome 렌더링과 비교한다.
     *
     * @param prefixes the name prefixes / 이름 접두사
     * @throws Exception on a failure / 실패 시
     */
    private static void compare(String... prefixes) throws Exception {
        List<Entry> all = index();
        int count = 0;
        StringBuilder failures = new StringBuilder();
        // 접두사가 맞는 사례마다 디코딩해 기준 이미지와 비교한다.
        for (int i = 0; i < all.size(); i++) {
            Entry e = all.get(i);
            boolean match = false;
            // 접두사 목록 중 하나라도 이름이 시작하면 해당 사례로 본다.
            for (int k = 0; k < prefixes.length; k++) {
                if (e.name.startsWith(prefixes[k])) {
                    match = true;
                }
            }
            if (!match) {
                continue;
            }
            count++;
            String msg = check(e);
            if (msg != null) {
                failures.append(e.name).append(": ").append(msg).append('\n');
            }
        }
        Check.isTrue(count > 0, "no reference case starts with " + Arrays.asList(prefixes));
        if (failures.length() > 0) {
            Check.fail(failures.toString());
        }
    }

    /**
     * Compares one case with its reference.
     * <p>
     * 사례 하나를 기준과 비교한다.
     *
     * @param e the case / 사례
     * @return null if it matches, else a description of the difference / 맞으면 null, 아니면 차이의 설명
     * @throws Exception on a failure to load or decode / 읽기나 디코딩 실패 시
     */
    private static String check(Entry e) throws Exception {
        byte[] svg = SvgTestKit.resource("/media/svg/cases/" + e.name + ".svg");
        byte[] ref = SvgTestKit.resource("/media/svg/refs/" + e.name + ".png");
        if (svg == null || ref == null) {
            return "the case or its reference image is missing";
        }
        BufferedImage want = ImageIO.read(new ByteArrayInputStream(ref));
        DecodedImage got;
        try {
            got = new SvgDecoder().decode(svg, DecodeLimits.DEFAULT);
        } catch (MediaException ex) {
            return "decoding failed: " + ex.getKind() + " " + ex.getMessage();
        }
        if (got.getWidth() != want.getWidth() || got.getHeight() != want.getHeight()) {
            return "size " + got.getWidth() + "x" + got.getHeight() + " but Chrome drew " + want.getWidth() + "x" + want.getHeight();
        }
        int w = got.getWidth();
        int h = got.getHeight();
        int[] a = want.getRGB(0, 0, w, h, null, 0, w);
        int[] b = got.getFrames().get(0).getArgb();
        double total = 0;
        int bad = 0;
        // 픽셀마다 흰색 위와 검은색 위에 합성한 값의 가장 큰 채널 차이를 구한다.
        for (int i = 0; i < a.length; i++) {
            int d = Math.abs((a[i] >>> 24) - (b[i] >>> 24));
            // 배경 두 가지(흰색 0xFF, 검은색 0)에 대해 차이를 구한다.
            for (int bg = 0; bg <= 255; bg += 255) {
                // 빨강, 초록, 파랑 채널을 차례로 비교한다.
                for (int shift = 16; shift >= 0; shift -= 8) {
                    double c1 = (((a[i] >> shift) & 0xFF) * (a[i] >>> 24) + bg * (255 - (a[i] >>> 24))) / 255.0;
                    double c2 = (((b[i] >> shift) & 0xFF) * (b[i] >>> 24) + bg * (255 - (b[i] >>> 24))) / 255.0;
                    d = Math.max(d, (int) Math.round(Math.abs(c1 - c2)));
                }
            }
            total += d;
            if (d > 40) {
                bad++;
            }
        }
        double mean = total / a.length;
        double badPercent = 100.0 * bad / a.length;
        if (mean > e.maxMean || badPercent > e.maxBad) {
            return String.format("mean error %.3f (limit %.2f), bad pixels %.3f%% (limit %.2f%%)", mean, e.maxMean, badPercent, e.maxBad);
        }
        return null;
    }

    /**
     * Basic shapes and paths.
     * <p>
     * 기본 도형과 경로.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testShapesAndPaths() throws Exception {
        compare("shape-", "path-", "points-", "huge-");
    }

    /**
     * Strokes (caps, joins, dashes, opacity, non-scaling stroke) and paint order.
     * <p>
     * 선(끝 모양, 이음, 점선, 투명도, 배율 무시 선)과 칠하기 순서.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testStrokes() throws Exception {
        compare("stroke-", "paint-order");
    }

    /**
     * Transforms, view boxes, preserveAspectRatio and the size rules of the root element.
     * <p>
     * 변환, viewBox, preserveAspectRatio, 루트 요소의 크기 규칙.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testTransformsViewBoxAndSize() throws Exception {
        compare("transform-", "viewbox-", "size-");
    }

    /**
     * Gradients.
     * <p>
     * 그라디언트.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testGradients() throws Exception {
        compare("grad-");
    }

    /**
     * Patterns.
     * <p>
     * 패턴.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testPatterns() throws Exception {
        compare("pattern-");
    }

    /**
     * Clip paths and masks.
     * <p>
     * 클립 경로와 마스크.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testClipsAndMasks() throws Exception {
        compare("clip-", "mask-");
    }

    /**
     * use, symbol, nested svg, switch, a, display and visibility.
     * <p>
     * use, symbol, 중첩 svg, switch, a, display, visibility.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testStructure() throws Exception {
        compare("use-", "symbol-", "nested-", "switch-", "a-element", "display-");
    }

    /**
     * Style sheets, selectors, the cascade, colours and units.
     * <p>
     * 스타일시트, 선택자, 연쇄, 색, 단위.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testCss() throws Exception {
        compare("css-", "colors-", "fill-keywords", "root-");
    }

    /**
     * Group opacity, embedded images, markers, group effects and entities.
     * <p>
     * 그룹 투명도, 포함된 이미지, 마커, 그룹 효과, 엔티티.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testOpacityImagesMarkers() throws Exception {
        compare("opacity-", "image-", "marker-", "group-effects", "xml-");
    }

    /**
     * Filter effects and blend modes.
     * <p>
     * 필터 효과와 혼합 모드.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFilters() throws Exception {
        compare("filter-", "blend-");
    }

    /**
     * Text; needs the fonts of the reference renderings.
     * <p>
     * 문자. 기준 렌더링의 글꼴이 필요하다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testText() throws Exception {
        if (!fontsInstalled()) {
            Check.skip("the fonts Arial, Times New Roman and Courier New of the Chrome reference renderings are not installed");
        }
        compare("text-");
    }

    /**
     * The case files and the references are in step: every case has a reference and a limit, nothing is left over.
     * <p>
     * 사례 파일과 기준이 서로 맞는다: 모든 사례에 기준과 제한이 있고 남는 것이 없다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testIndexIsComplete() throws Exception {
        List<Entry> all = index();
        Check.isTrue(all.size() >= 140, "expected at least 140 reference cases but found " + all.size());
        Set<String> seen = new HashSet<String>();
        // 항목마다 사례와 기준 이미지가 모두 있는지 보고 이름이 겹치지 않는지 본다.
        for (int i = 0; i < all.size(); i++) {
            Entry e = all.get(i);
            Check.isTrue(seen.add(e.name), "duplicate case " + e.name);
            Check.notNull(SvgTestKit.resource("/media/svg/cases/" + e.name + ".svg"), "missing case file " + e.name);
            Check.notNull(SvgTestKit.resource("/media/svg/refs/" + e.name + ".png"), "missing reference " + e.name);
        }
    }
}
