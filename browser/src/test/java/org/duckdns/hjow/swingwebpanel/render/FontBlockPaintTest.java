package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.FontFaceRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.WebFontSet;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the block period of <code>font-display</code> (plan item P10-08-g, CSS Fonts 4 section 4.8): while a web font is loading and its face is in the block
 * period, the text that asks for it is laid out with the fallback font (it keeps the space of that font) and is not painted; when the period is over, or for
 * <code>swap</code>, or when an earlier family of the list is available, the text is painted. The web font set reports the earliest end of a block period so that
 * the page can be painted again then. The request times come from a fake {@link WebFontSet.TimedLoader} whose start time the test sets.
 * <p>
 * <code>font-display</code> 의 block 기간 시험(계획 P10-08-g, CSS Fonts 4 4.8 절): 웹 글꼴을 읽는 동안 그 면이 block 기간이면 그것을 요청한 텍스트는 대체 글꼴로 배치하고(그 글꼴의 자리를
 * 차지한다) 그리지 않는다. 기간이 지났거나 <code>swap</code> 이거나 목록의 앞 패밀리를 쓸 수 있으면 텍스트를 그린다. 웹 글꼴 집합은 가장 이른 block 기간의 끝을 알려 페이지가 그때 다시
 * 그려질 수 있게 한다. 요청 시각은 시험이 시작 시각을 정하는 가짜 {@link WebFontSet.TimedLoader} 가 준다.
 */
public class FontBlockPaintTest {

    /** The text of the measured paragraphs. 재는 문단의 텍스트. */
    private static final String TEXT = "HHHHHHHH";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontBlockPaintTest() {
    }

    /**
     * A loader whose fonts never arrive (or arrive when a file is put) and whose requests started at a time that the test sets.
     * <p>
     * 글꼴이 오지 않고(파일을 넣으면 온다) 요청이 시험이 정한 시각에 시작된 로더.
     */
    private static final class TimedFakeLoader implements WebFontSet.TimedLoader {
        /** The files by URL. URL 별 파일. */
        final Map<String, byte[]> files = new HashMap<String, byte[]>();
        /** The start time of every request in milliseconds of the system clock. 모든 요청의 시작 시각(시스템 시계 ms). */
        long start;

        /**
         * Creates the loader.
         * <p>
         * 로더를 만든다.
         *
         * @param start the start time of the requests / 요청의 시작 시각
         */
        TimedFakeLoader(long start) {
            this.start = start;
        }

        /**
         * Returns a file, null while it is not in the map.
         * <p>
         * 파일을 반환한다. 맵에 없는 동안은 null.
         *
         * @param absoluteUrl the URL / URL
         * @return the bytes or null / 바이트 또는 null
         * @throws IOException never / 던지지 않는다
         */
        public byte[] load(String absoluteUrl) throws IOException {
            return files.get(absoluteUrl);
        }

        /**
         * Returns the start time and the arrival (0 while pending).
         * <p>
         * 시작 시각과 도착 시각(대기 중이면 0)을 반환한다.
         *
         * @param absoluteUrl the URL / URL
         * @return {start, arrival} / {시작, 도착}
         */
        public long[] times(String absoluteUrl) {
            return new long[] { start, files.containsKey(absoluteUrl) ? start + 1 : 0 };
        }
    }

    /**
     * A loader without times (the block period cannot be known, so nothing is blocked).
     * <p>
     * 시각이 없는 로더(block 기간을 알 수 없으므로 아무것도 막지 않는다).
     */
    private static final class PlainLoader implements WebFontSet.Loader {
        /**
         * Returns null: the font never arrives.
         * <p>
         * null 을 반환한다: 글꼴이 오지 않는다.
         *
         * @param absoluteUrl the URL / URL
         * @return null / null
         */
        public byte[] load(String absoluteUrl) {
            return null;
        }
    }

    /**
     * The page, its style engine and its web font set.
     * <p>
     * 페이지, 그 스타일 엔진과 웹 글꼴 집합.
     */
    private static final class Page {
        /** The document. 문서. */
        Document doc;
        /** The style engine. 스타일 엔진. */
        CssEngine css;
        /** The web fonts. 웹 글꼴. */
        WebFontSet fonts;
        /** The rendered page. 렌더링한 페이지. */
        RenderedPage rendered;
    }

    /**
     * Lays out and renders a page with two paragraphs of the same text: <code>#a</code> asks for the web family <code>Web</code> (whose font never arrives)
     * before <code>monospace</code>, <code>#b</code> asks for <code>monospace</code> only.
     * <p>
     * 같은 텍스트의 두 문단이 있는 페이지를 배치하고 렌더링한다: <code>#a</code> 는 <code>monospace</code> 앞에 웹 패밀리 <code>Web</code>(글꼴이 오지 않음)을, <code>#b</code> 는
     * <code>monospace</code> 만 요청한다.
     *
     * @param display  the value of <code>font-display</code> / <code>font-display</code> 값
     * @param familyA  the <code>font-family</code> of <code>#a</code> / <code>#a</code> 의 <code>font-family</code>
     * @param loader   the loader / 로더
     * @return the page / 페이지
     */
    private static Page render(String display, String familyA, WebFontSet.Loader loader) {
        String css = "@font-face{font-family:Web;font-display:" + display + ";src:url(w.ttf)} p{margin:0;font-size:20px;color:#000;white-space:nowrap}"
                + " #a{font-family:" + familyA + "} #b{font-family:monospace}";
        Page p = new Page();
        p.doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + RenderTestSupport.RESET + "<style>" + css + "</style></head><body><p id=a>" + TEXT
                + "</p><p id=b>" + TEXT + "</p></body></html>", "http://t.test/dir/page.html", null, true);
        p.css = new CssEngine(p.doc, null, new MediaEnvironment(400, 100));
        p.css.loadStyleSheets();
        List<FontFaceRule> faces = new ArrayList<FontFaceRule>();
        // 문서의 스타일 시트에서 글꼴 규칙을 모은다.
        for (StyleSheet s : p.css.getStyleSheets()) {
            for (CssRule r : s.getRules()) {
                if (r instanceof FontFaceRule) {
                    faces.add((FontFaceRule) r);
                }
            }
        }
        p.fonts = new WebFontSet(loader);
        p.fonts.setRules(faces);
        p.rendered = PageRenderer.render(p.doc, p.css, null, 400, 100, p.fonts);
        return p;
    }

    /**
     * Returns the first text fragment of an element.
     * <p>
     * 요소의 첫 텍스트 조각을 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id of the element / 요소의 id
     * @return the text box / 텍스트 상자
     */
    private static LayoutBox text(Page p, String id) {
        List<LayoutBox> boxes = p.rendered.getLayout().getBoxes(p.doc.getElementById(id));
        Check.isTrue(!boxes.isEmpty(), "the element " + id + " has boxes");
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport.collect(boxes.get(0), LayoutBox.Type.TEXT, out);
        Check.isTrue(!out.isEmpty(), "the element " + id + " has a text fragment");
        return out.get(0);
    }

    /**
     * Counts the dark pixels inside the border box of an element.
     * <p>
     * 요소의 border box 안에 있는 어두운 픽셀의 수를 센다.
     *
     * @param p  the page / 페이지
     * @param id the id of the element / 요소의 id
     * @return the number of pixels / 픽셀 수
     */
    private static int ink(Page p, String id) {
        BufferedImage img = RenderTestSupport.paint(p.rendered, 400, 100);
        LayoutBox b = p.rendered.getLayout().getBoxes(p.doc.getElementById(id)).get(0);
        return RenderTestSupport.ink(img, (int) b.getX(), (int) b.getY(), (int) Math.ceil(b.getX() + b.getWidth()), (int) Math.ceil(b.getY() + b.getHeight()));
    }

    /**
     * The block periods of the values (CSS Fonts 4 section 4.8): long for <code>block</code> and <code>auto</code>, short for <code>fallback</code> and
     * <code>optional</code>, none for <code>swap</code>; an unknown value is treated as <code>auto</code>.
     * <p>
     * 값별 block 기간(CSS Fonts 4 4.8 절): <code>block</code> 과 <code>auto</code> 는 길고 <code>fallback</code> 과 <code>optional</code> 은 짧으며 <code>swap</code> 은 없다.
     * 모르는 값은 <code>auto</code> 로 다룬다.
     */
    public void testBlockPeriods() {
        Check.equal(3000L, WebFontSet.LONG_BLOCK_MS, "the long block period");
        Check.equal(WebFontSet.LONG_BLOCK_MS, blockPeriodOf("block"), "block");
        Check.equal(WebFontSet.LONG_BLOCK_MS, blockPeriodOf("auto"), "auto");
        Check.equal(WebFontSet.LONG_BLOCK_MS, blockPeriodOf("bogus"), "an unknown value");
        Check.equal(WebFontSet.SHORT_BLOCK_MS, blockPeriodOf("fallback"), "fallback");
        Check.equal(WebFontSet.SHORT_BLOCK_MS, blockPeriodOf("optional"), "optional");
        Check.equal(0L, blockPeriodOf("swap"), "swap");
    }

    /**
     * Measures the block period of a value through the public answer of the set: the end of the period minus the start of the request.
     * <p>
     * 집합의 공개된 답으로 값의 block 기간을 잰다: 기간의 끝에서 요청 시작을 뺀 값.
     *
     * @param display the value / 값
     * @return the period in milliseconds / 기간(ms)
     */
    private static long blockPeriodOf(String display) {
        long start = System.currentTimeMillis();
        Page p = render(display, "Web,monospace", new TimedFakeLoader(start));
        long end = p.fonts.takeBlockDeadline();
        return end == 0 ? 0 : end - start;
    }

    /**
     * Within the block period of <code>font-display: block</code> the text that waits for the web font keeps the width of the fallback font but no pixel of it is
     * painted, while the same text in the fallback family is painted; the set reports the end of the period once.
     * <p>
     * <code>font-display: block</code> 의 block 기간 안에서 웹 글꼴을 기다리는 텍스트는 대체 글꼴의 너비를 유지하되 픽셀이 하나도 그려지지 않고, 대체 패밀리의 같은 텍스트는 그려진다.
     * 집합은 기간의 끝을 한 번 알린다.
     */
    public void testBlockedTextKeepsItsSpaceButIsNotPainted() {
        long start = System.currentTimeMillis();
        Page p = render("block", "Web,monospace", new TimedFakeLoader(start));
        LayoutBox a = text(p, "a");
        LayoutBox b = text(p, "b");
        Check.isTrue(a.isFontBlocked(), "the text that waits for the web font is blocked");
        Check.isFalse(b.isFontBlocked(), "the text in the fallback family is not");
        Check.near(b.getWidth(), a.getWidth(), 0.01, "the blocked text keeps the width of the fallback font");
        Check.equal(b.getFont().getFontName(), a.getFont().getFontName(), "it is laid out with the fallback font");
        Check.equal(0, ink(p, "a"), "no pixel of the blocked text is painted");
        Check.isTrue(ink(p, "b") > 20, "the fallback text is painted");
        long end = p.fonts.takeBlockDeadline();
        Check.equal(start + WebFontSet.LONG_BLOCK_MS, end, "the end of the block period is reported");
        Check.equal(0L, p.fonts.takeBlockDeadline(), "and forgotten after it was taken");
    }

    /**
     * After the block period the text is painted in the fallback font (the swap period), and nothing is reported.
     * <p>
     * block 기간이 지나면 텍스트는 대체 글꼴로 그려지고(swap 기간) 아무것도 알리지 않는다.
     */
    public void testTextIsPaintedAfterTheBlockPeriod() {
        Page p = render("block", "Web,monospace", new TimedFakeLoader(System.currentTimeMillis() - WebFontSet.LONG_BLOCK_MS - 50));
        Check.isFalse(text(p, "a").isFontBlocked(), "the block period is over");
        Check.isTrue(ink(p, "a") > 20, "the text is painted in the fallback font");
        Check.equal(0L, p.fonts.takeBlockDeadline(), "no end is reported");
        Page f = render("fallback", "Web,monospace", new TimedFakeLoader(System.currentTimeMillis() - WebFontSet.SHORT_BLOCK_MS - 50));
        Check.isFalse(text(f, "a").isFontBlocked(), "the short block period of fallback is over");
        Page q = render("fallback", "Web,monospace", new TimedFakeLoader(System.currentTimeMillis()));
        Check.isTrue(text(q, "a").isFontBlocked(), "but a fallback font that has just been requested blocks the text");
    }

    /**
     * <code>swap</code> has no block period, a loader without times cannot tell the period, and an available family before the web family is used visibly: in all
     * three cases the text is painted at once.
     * <p>
     * <code>swap</code> 에는 block 기간이 없고, 시각이 없는 로더는 기간을 알 수 없으며, 웹 패밀리보다 앞의 쓸 수 있는 패밀리는 보이게 쓰인다: 세 경우 모두 텍스트를 곧바로 그린다.
     */
    public void testSwapPlainLoaderAndEarlierFamilyAreVisible() {
        Page swap = render("swap", "Web,monospace", new TimedFakeLoader(System.currentTimeMillis()));
        Check.isFalse(text(swap, "a").isFontBlocked(), "swap");
        Check.isTrue(ink(swap, "a") > 20, "swap: painted");
        Page plain = render("block", "Web,monospace", new PlainLoader());
        Check.isFalse(text(plain, "a").isFontBlocked(), "a loader without times");
        Check.equal(0L, plain.fonts.takeBlockDeadline(), "a loader without times reports nothing");
        Page earlier = render("block", "monospace,Web", new TimedFakeLoader(System.currentTimeMillis()));
        Check.isFalse(text(earlier, "a").isFontBlocked(), "an available family before the web family");
        Check.isTrue(ink(earlier, "a") > 20, "the earlier family is painted");
    }

    /**
     * A web family that the page does not declare, or whose only source cannot be used, does not block the text.
     * <p>
     * 페이지가 선언하지 않은 웹 패밀리나 하나뿐인 소스를 쓸 수 없는 패밀리는 텍스트를 막지 않는다.
     */
    public void testUndeclaredOrFailedFamilyDoesNotBlock() {
        Page other = render("block", "Other,monospace", new TimedFakeLoader(System.currentTimeMillis()));
        Check.isFalse(text(other, "a").isFontBlocked(), "a family without @font-face");
        TimedFakeLoader broken = new TimedFakeLoader(System.currentTimeMillis());
        broken.files.put("http://t.test/dir/w.ttf", new byte[] { 1, 2, 3, 4 });
        Page failed = render("block", "Web,monospace", broken);
        Check.isFalse(text(failed, "a").isFontBlocked(), "a face whose file failed to load");
        Check.isTrue(ink(failed, "a") > 20, "the text is painted in the fallback font");
    }
}
