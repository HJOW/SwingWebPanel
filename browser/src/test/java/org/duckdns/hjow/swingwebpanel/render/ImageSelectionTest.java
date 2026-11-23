package org.duckdns.hjow.swingwebpanel.render;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the choice of an image source: <code>src</code>, <code>srcset</code> with density and width descriptors, <code>sizes</code>
 * (media conditions, lengths, <code>calc()</code>), the <code>picture</code> element with <code>type</code> and <code>media</code>, and the
 * effect on the layout and the painting (plan item P18-05).
 * <p>
 * 이미지 소스 선택의 시험: <code>src</code>, 밀도/너비 기술자가 있는 <code>srcset</code>, <code>sizes</code>(미디어 조건, 길이,
 * <code>calc()</code>), <code>type</code> 과 <code>media</code> 가 있는 <code>picture</code> 요소, 그리고 레이아웃과 그리기에 주는 영향(계획 P18-05).
 */
public class ImageSelectionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageSelectionTest() {
    }

    /**
     * Parses a body and returns the element with the id <code>t</code>.
     * <p>
     * 본문을 해석하여 id 가 <code>t</code> 인 요소를 반환한다.
     *
     * @param body the body markup / 본문 마크업
     * @return the element / 요소
     */
    private static Element target(String body) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><body>" + body + "</body></html>", "http://t.test/dir/page.html", null, true);
        return doc.getElementById("t");
    }

    /**
     * Selects with a viewport and a resolution.
     * <p>
     * 뷰포트와 해상도를 정해 선택한다.
     *
     * @param e     the element / 요소
     * @param width the viewport width / 뷰포트 너비
     * @param ratio the device pixel ratio / 장치 픽셀 비율
     * @return the chosen URL or null / 선택된 URL 또는 null
     */
    private static String pick(Element e, double width, double ratio) {
        MediaEnvironment env = new MediaEnvironment(width, 600);
        env.setResolution(ratio);
        ImageSelection.Choice c = ImageSelection.select(e, env);
        return c == null ? null : c.getUrl();
    }

    /**
     * A plain <code>src</code> is the only candidate; an element without a source selects nothing.
     * <p>
     * 일반 <code>src</code> 는 유일한 후보이고, 소스가 없는 요소는 아무것도 고르지 않는다.
     */
    public void testPlainSrc() {
        Check.equal("a.png", pick(target("<img id=t src='  a.png '>"), 800, 1), "src is trimmed");
        Check.isNull(pick(target("<img id=t>"), 800, 1), "no source");
        Check.isNull(pick(target("<img id=t src='  '>"), 800, 1), "a blank src is no source");
        Check.equal(1.0, ImageSelection.select(target("<img id=t src=a.png>"), new MediaEnvironment(800, 600)).getDensity(), "density 1");
    }

    /**
     * Density descriptors: the smallest density that is not below the device pixel ratio, or else the largest one; the src attribute counts as
     * 1x unless a 1x candidate exists.
     * <p>
     * 밀도 기술자: 장치 픽셀 비율 이상인 가장 작은 밀도, 없으면 가장 큰 밀도. src 속성은 1x 후보가 없으면 1x 로 친다.
     */
    public void testDensityDescriptors() {
        Element e = target("<img id=t src=a.png srcset='b.png 2x, c.png 3x'>");
        Check.equal("a.png", pick(e, 800, 1), "src is the 1x candidate");
        Check.equal("b.png", pick(e, 800, 1.5), "the smallest density that is not below 1.5");
        Check.equal("b.png", pick(e, 800, 2), "exactly 2");
        Check.equal("c.png", pick(e, 800, 2.5), "3x is the next one");
        Check.equal("c.png", pick(e, 800, 5), "the largest when nothing is large enough");
        Element f = target("<img id=t src=a.png srcset='b.png 1x, c.png 2x'>");
        Check.equal("b.png", pick(f, 800, 1), "an explicit 1x candidate wins over src");
        Check.equal(2.0, ImageSelection.select(target("<img id=t srcset='big.png 2x'>"), new MediaEnvironment(800, 600)).getDensity(),
                "an only candidate with 2x is used even at ratio 1");
        Element g = target("<img id=t srcset='a.png, b.png 2x'>");
        Check.equal("a.png", pick(g, 800, 1), "a candidate without a descriptor is 1x");
    }

    /**
     * The parsing of a srcset: commas inside URLs, trailing commas, white space, invalid and conflicting descriptors.
     * <p>
     * srcset 해석: URL 안의 쉼표, 끝 쉼표, 공백, 잘못되었거나 충돌하는 기술자.
     */
    public void testSrcsetParsing() {
        Check.equal("a,b.png", pick(target("<img id=t srcset='a,b.png 1x'>"), 800, 1), "a comma inside the URL is kept");
        Check.equal("a.png,b.png", pick(target("<img id=t srcset='a.png,b.png 2x'>"), 800, 1), "a comma without a space does not end the URL");
        Check.equal("a.png", pick(target("<img id=t srcset='a.png, b.png 2x'>"), 800, 1), "a trailing comma ends a candidate");
        Check.equal("b.png", pick(target("<img id=t srcset=' a.png 0x ,\n b.png  1x '>"), 800, 1), "0x is invalid; white space is ignored");
        Check.equal("b.png", pick(target("<img id=t srcset='a.png 1x 2w, b.png 1x'>"), 800, 1), "x with w conflicts: the candidate is dropped");
        Check.equal("b.png", pick(target("<img id=t srcset='a.png 100h, b.png 1x'>"), 800, 1), "h without w is invalid");
        Check.equal("b.png", pick(target("<img id=t srcset='a.png abc, b.png 1x'>"), 800, 1), "an unknown descriptor drops the candidate");
        Check.equal("a.png", pick(target("<img id=t srcset='a.png 1.5x'>"), 800, 1), "fractions are valid");
        Check.equal(3, ImageSelection.parseSrcset("a.png 1x, b.png 2x,c.png 3x").size(), "three candidates");
    }

    /**
     * Width descriptors with <code>sizes</code>: the width of the image slot decides which file is large enough.
     * <p>
     * <code>sizes</code> 가 있는 너비 기술자: 이미지 칸의 너비가 어떤 파일이 충분히 큰지를 정한다.
     */
    public void testWidthDescriptorsAndSizes() {
        Element e = target("<img id=t src=fallback.png srcset='s.png 400w, m.png 800w, l.png 1600w' sizes='(max-width: 600px) 480px, 800px'>");
        Check.equal("m.png", pick(e, 500, 1), "viewport 500: slot 480px, 800w is 1.67x");
        Check.equal("l.png", pick(e, 500, 2), "viewport 500 at ratio 2: 1600w is 3.3x (the smallest not below 2)");
        Check.equal("m.png", pick(e, 1000, 1), "viewport 1000: slot 800px, 800w is 1x");
        Check.equal("l.png", pick(e, 1000, 2), "viewport 1000 at ratio 2: 1600w is 2x");
        Element d = target("<img id=t srcset='s.png 400w, m.png 800w'>");
        Check.equal("m.png", pick(d, 800, 1), "without sizes the slot is 100vw: 800w is 1x");
        Check.equal("s.png", pick(d, 400, 1), "100vw = 400 gives 400w as 1x");
        Element c = target("<img id=t srcset='s.png 300w, m.png 600w' sizes='calc(100vw - 40px)'>");
        Check.equal("s.png", pick(c, 340, 1), "calc(100vw - 40px) at 340 is 300px: 300w is 1x");
        Check.equal("m.png", pick(c, 640, 1), "calc(100vw - 40px) at 640 is 600px: 600w is 1x");
        Element f = target("<img id=t srcset='s.png 300w, m.png 600w' sizes='(min-width: 700px) 50vw, 100vw'>");
        Check.equal("m.png", pick(f, 600, 1), "a condition that does not match falls to 100vw");
        Check.equal("m.png", pick(f, 700, 1), "50vw of 700 is 350px: 300w is 0.86x, 600w is 1.7x; the smallest not below 1 is 600w");
    }

    /**
     * Source sizes: units, calc, min and max, and invalid lengths.
     * <p>
     * 소스 크기: 단위, calc, min, max, 잘못된 길이.
     */
    public void testSourceSizeLengths() {
        MediaEnvironment env = new MediaEnvironment(500, 400);
        Check.equal(300.0, ImageSelection.evaluateLength("300px", env), "px");
        Check.equal(250.0, ImageSelection.evaluateLength("50vw", env), "vw");
        Check.equal(40.0, ImageSelection.evaluateLength("10vh", env), "vh");
        Check.equal(32.0, ImageSelection.evaluateLength("2em", env), "em uses 16px");
        Check.equal(96.0, ImageSelection.evaluateLength("1in", env), "in");
        Check.equal(460.0, ImageSelection.evaluateLength("calc(100vw - 40px)", env), "calc with a difference");
        Check.equal(150.0, ImageSelection.evaluateLength("calc(100vw / 2 - 100px)", env), "calc with a quotient");
        Check.equal(300.0, ImageSelection.evaluateLength("min(300px, 90vw)", env), "min");
        Check.equal(450.0, ImageSelection.evaluateLength("max(300px, 90vw)", env), "max");
        Check.isTrue(Double.isNaN(ImageSelection.evaluateLength("100", env)), "a unitless number other than 0 is invalid");
        Check.equal(0.0, ImageSelection.evaluateLength("0", env), "0 is a length");
        Check.isTrue(Double.isNaN(ImageSelection.evaluateLength("abc", env)), "invalid");
        Check.isTrue(Double.isNaN(ImageSelection.evaluateLength("10px 20px", env)), "two lengths are invalid");
        Check.equal(500.0, ImageSelection.sourceSize(null, env), "no sizes: 100vw");
        Check.equal(500.0, ImageSelection.sourceSize("garbage", env), "invalid sizes: 100vw");
        Check.equal(200.0, ImageSelection.sourceSize("(min-width: 300px) 200px, 100px", env), "the first matching condition");
        Check.equal(100.0, ImageSelection.sourceSize("(min-width: 900px) 200px, 100px", env), "the last entry without a condition");
    }

    /**
     * A picture chooses its first usable source: unsupported types and media queries that do not match are skipped, the img is the fallback.
     * <p>
     * picture 는 쓸 수 있는 첫 source 를 고른다: 지원하지 않는 형식과 맞지 않는 미디어 질의는 건너뛰고 img 가 대체이다.
     */
    public void testPicture() {
        String html = "<picture><source srcset='x.jxl' type='image/jxl'><source srcset='wide.png' media='(min-width: 700px)'>"
                + "<source srcset='narrow.webp 1x, narrow2.webp 2x' type='image/webp'><img id=t src='fallback.png'></picture>";
        Element e = target(html);
        Check.equal("wide.png", pick(e, 800, 1), "a wide viewport takes the media source");
        Check.equal("narrow.webp", pick(e, 500, 1), "a narrow viewport skips it and takes the webp source (decoded here)");
        Check.equal("narrow2.webp", pick(e, 500, 2), "and its 2x candidate at ratio 2");
        Element f = target("<picture><source srcset='x.jxl' type='image/jxl'><img id=t src='fallback.png'></picture>");
        Check.equal("fallback.png", pick(f, 800, 1), "an unsupported type falls back to the img");
        Element g = target("<picture><source type='image/png'><source srcset='ok.png'><img id=t src='fallback.png'></picture>");
        Check.equal("ok.png", pick(g, 800, 1), "a source without srcset is skipped");
        Element h = target("<div><source srcset='no.png'><img id=t src='fallback.png'></div>");
        Check.equal("fallback.png", pick(h, 800, 1), "source siblings count only inside a picture");
        Element i = target("<picture><img id=t src='fallback.png'><source srcset='after.png'></picture>");
        Check.equal("fallback.png", pick(i, 800, 1), "a source after the img is ignored");
    }

    /**
     * Finds the first box of an element.
     * <p>
     * 요소의 첫 상자를 찾는다.
     *
     * @param p the page / 페이지
     * @return the box / 상자
     */
    private static LayoutBox imageBox(RenderedPage p) {
        return RenderTestSupport.firstBox(p, "img");
    }

    /**
     * The layout and the painting use the chosen file, and the density scales the natural size.
     * <p>
     * 레이아웃과 그리기는 선택된 파일을 쓰고 밀도는 자연 크기를 줄인다.
     */
    public void testLayoutAndPaint() {
        RenderTestSupport.FixedImages src = new RenderTestSupport.FixedImages()
                .put("http://t.test/dir/one.png", ImageResource.loaded(RenderTestSupport.solid(4, 4, 0xFFFF0000)))
                .put("http://t.test/dir/two.png", ImageResource.loaded(RenderTestSupport.solid(8, 8, 0xFF0000FF)))
                .put("http://t.test/dir/broken.png", ImageResource.failed("404"));
        RenderedPage same = RenderTestSupport.page("<img src='one.png' srcset='two.png 2x'>", 60, 30, src);
        Check.equal(4.0, imageBox(same).getWidth(), "at ratio 1 the src (1x) is chosen over the 2x candidate");
        RenderedPage p = RenderTestSupport.page("<img srcset='two.png 2x'>", 60, 30, src);
        LayoutBox b = imageBox(p);
        Check.equal(4.0, b.getWidth(), "an 8px image with density 2 is laid out 4px wide");
        java.awt.image.BufferedImage img = RenderTestSupport.paint(p, 60, 30);
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, (int) b.getX() + 1, (int) Math.round(b.getY()) + 1), "the srcset file is painted");
        RenderedPage q = RenderTestSupport.page("<picture><source srcset='two.png' media='(min-width: 50px)'><img src='one.png'></picture>", 60, 30, src);
        LayoutBox qb = imageBox(q);
        Check.equal(8.0, qb.getWidth(), "the picture source gives the natural size");
        RenderedPage r = RenderTestSupport.page("<picture><source srcset='two.png' media='(min-width: 500px)'><img src='one.png'></picture>", 60, 30, src);
        Check.equal(4.0, imageBox(r).getWidth(), "the media query does not match: the fallback img is used");
        RenderedPage s = RenderTestSupport.page("<img srcset='broken.png 1x' alt='fallback text' width='100' height='20'>", 120, 40, src);
        List<DisplayCommand> found = new java.util.ArrayList<DisplayCommand>();
        DisplayList l = s.getDisplayList();
        // 실패한 선택 이미지는 그 URL 로 대체 그림을 만든다.
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getType() == DisplayCommand.Type.PLACEHOLDER) {
                found.add(l.get(i));
            }
        }
        Check.equal(1, found.size(), "one placeholder for the failed chosen file");
        Check.equal(DisplayListBuilder.UNSUPPORTED_URL + "http://t.test/dir/broken.png", ((DisplayCommands.Placeholder) found.get(0)).getLines()[0],
                "it names the chosen URL");
    }
}
