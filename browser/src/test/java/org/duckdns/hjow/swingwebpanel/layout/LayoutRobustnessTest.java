package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Robustness tests of the layout engine (plan items P09-09, P22-09): empty and unusual documents, very deep nesting, many elements, a
 * huge text and repeated layouts at changing widths.
 * <p>
 * 레이아웃 엔진의 견고성 시험(계획 P09-09, P22-09): 빈 문서와 특이한 문서, 아주 깊은 중첩, 많은 요소, 거대한 텍스트, 너비를 바꿔 가며 반복한 배치.
 */
public class LayoutRobustnessTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LayoutRobustnessTest() {
    }

    /**
     * Lays out markup without the reset sheet.
     * <p>
     * 초기화 시트 없이 마크업을 배치한다.
     *
     * @param html  the complete markup / 전체 마크업
     * @param width viewport width / 뷰포트 너비
     * @return the result / 결과
     */
    private static LayoutResult raw(String html, double width) {
        Document doc = HtmlParser.parseDocument(html);
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(width, 600));
        css.loadStyleSheets();
        return LayoutEngine.layout(doc, new CssStyleSource(css), null, width, 600);
    }

    /**
     * Empty documents, documents with only text, a root with <code>display:none</code> and a zero-width viewport do not fail.
     * <p>
     * 빈 문서, 텍스트만 있는 문서, <code>display:none</code> 루트, 너비 0 뷰포트는 실패하지 않는다.
     */
    public void testUnusualDocuments() {
        Check.notNull(raw("", 300).getRoot(), "empty source");
        LayoutResult t = raw("just text", 300);
        Check.isTrue(t.getContentHeight() > 0, "text without markup has height");
        Check.near(0, raw("<html style='display:none'><body>x</body></html>", 300).getContentHeight(), 0.001, "invisible root");
        LayoutResult z = raw("<p>some words that cannot fit anywhere</p>", 0);
        Check.isTrue(z.getContentHeight() > 0, "zero width viewport still lays out");
        LayoutResult n = raw("<p>x</p>", Double.NaN);
        Check.notNull(n.getRoot(), "NaN width is treated as zero");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Calls the engine without a document.
             * <p>
             * 문서 없이 엔진을 호출한다.
             */
            public void run() {
                LayoutEngine.layout(null, null, null, 10, 10);
            }
        }, "null arguments");
    }

    /**
     * Very deep nesting is cut at the documented depth and does not overflow the stack.
     * <p>
     * 아주 깊은 중첩은 문서화된 깊이에서 잘리며 스택을 넘치게 하지 않는다.
     */
    public void testDeepNesting() {
        StringBuilder sb = new StringBuilder("<div style='margin:1px'>");
        int depth = 5000;
        // 깊게 중첩된 div 를 만든다.
        for (int i = 0; i < depth; i++) {
            sb.append("<div style='padding:1px'>");
        }
        sb.append("deep text");
        // 중첩된 div 를 닫는다.
        for (int i = 0; i < depth; i++) {
            sb.append("</div>");
        }
        sb.append("</div>");
        LayoutResult r = raw(sb.toString(), 400);
        Check.notNull(r.getRoot(), "laid out");
        Check.isTrue(r.getBoxCount() <= BoxBuilder.MAX_DEPTH * 2 + 50, "depth is limited: " + r.getBoxCount());
        Check.isTrue(r.getContentHeight() > BoxBuilder.MAX_DEPTH, "the padding of the visible levels adds up");
    }

    /**
     * Ten thousand elements lay out in reasonable time and give a consistent document height.
     * <p>
     * 요소 만 개도 합리적인 시간에 배치되고 일관된 문서 높이를 준다.
     */
    public void testManyElements() {
        StringBuilder sb = new StringBuilder("<style>div{height:3px}</style>");
        // 높이가 같은 형제 div 를 만 개 만든다.
        for (int i = 0; i < 10000; i++) {
            sb.append("<div>x</div>");
        }
        long t0 = System.currentTimeMillis();
        LayoutResult r = raw(sb.toString(), 400);
        long ms = System.currentTimeMillis() - t0;
        Check.isTrue(r.getBoxCount() >= 10000, "boxes: " + r.getBoxCount());
        Check.isTrue(r.getContentHeight() >= 30000, "height: " + r.getContentHeight());
        Check.isTrue(ms < 120000, "ten thousand elements took " + ms + " ms");
    }

    /**
     * A long text lays out line by line, and laying out again at other widths gives consistent results (no loop, same text).
     * <p>
     * 긴 텍스트는 줄 단위로 배치되고 다른 너비로 다시 배치해도 결과가 일관된다(반복 없음, 같은 텍스트).
     */
    public void testLongTextAndRelayout() {
        StringBuilder words = new StringBuilder();
        // 같은 낱말을 많이 이어 긴 문단을 만든다.
        for (int i = 0; i < 3000; i++) {
            words.append("word").append(i % 10).append(' ');
        }
        String html = "<style>body{font:10px monospace}</style><p>" + words + "</p>";
        LayoutResult wide = raw(html, 800);
        LayoutResult narrow = raw(html, 200);
        Check.isTrue(narrow.getContentHeight() > wide.getContentHeight() * 2, "a narrow viewport makes the text taller");
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        // 두 배치의 모든 줄 텍스트를 이어 같은 낱말 순서인지 비교한다.
        for (LayoutBox l : LayoutTestSupport.lines(wide.getRoot())) {
            a.append(LayoutTestSupport.lineText(l)).append(' ');
        }
        // 좁은 배치의 모든 줄 텍스트를 이어 붙인다.
        for (LayoutBox l : LayoutTestSupport.lines(narrow.getRoot())) {
            b.append(LayoutTestSupport.lineText(l)).append(' ');
        }
        Check.equal(a.toString().trim().replaceAll("\\s+", " "), b.toString().trim().replaceAll("\\s+", " "), "same words in the same order");
    }

    /**
     * Results are independent of each other: a later layout does not change an earlier one.
     * <p>
     * 결과는 서로 독립적이다: 나중 배치가 앞선 배치를 바꾸지 않는다.
     */
    public void testResultsAreSnapshots() {
        Document doc = HtmlParser.parseDocument("<html><body><p id=a>some text that wraps when the viewport is narrow enough</p></body></html>");
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(500, 600));
        css.loadStyleSheets();
        CssStyleSource styles = new CssStyleSource(css);
        LayoutResult first = LayoutEngine.layout(doc, styles, null, 500, 600);
        double h = first.getContentHeight();
        int n = first.getBoxCount();
        LayoutEngine.layout(doc, styles, null, 60, 600);
        Check.near(h, first.getContentHeight(), 0.0001, "the earlier result keeps its height");
        Check.equal(n, first.getBoxCount(), "and its box count");
    }
}
