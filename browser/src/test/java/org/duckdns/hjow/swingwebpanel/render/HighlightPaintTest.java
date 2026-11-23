package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Highlight;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.StaticRange;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of custom highlights (plan item P10-08-h, CSS Custom Highlight API Level 1 https://www.w3.org/TR/css-highlight-api-1/ WD 14 December
 * 2021, sections 4.2.3 to 4.2.5, and CSS Pseudo-Elements 4 sections 3.5 and 3.6): the background and the color of <code>::highlight(name)</code> over the
 * parts of the text that the ranges cover, the inheritance of the highlight styles from the parent element (also for <code>background-color</code>), the
 * order of overlapping highlights by priority and registration, <code>currentcolor</code>, decorations, static ranges and the ranges that are not painted.
 * <p>
 * 사용자 정의 하이라이트 그리기 시험(계획 P10-08-h, CSS Custom Highlight API Level 1 4.2.3~4.2.5 절, CSS Pseudo-Elements 4 3.5, 3.6 절): 범위가 덮는 텍스트 부분 위의
 * <code>::highlight(이름)</code> 의 배경과 색, 부모 요소의 하이라이트 스타일 상속(<code>background-color</code> 포함), 겹치는 하이라이트의 우선순위와 등록 순서,
 * <code>currentcolor</code>, 장식, 정적 범위, 그리지 않는 범위.
 */
public class HighlightPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HighlightPaintTest() {
    }

    /**
     * A parsed document with its style engine, painted after the test registered its highlights.
     * <p>
     * 시험이 하이라이트를 등록한 뒤 그리는, 스타일 엔진이 있는 파싱한 문서.
     */
    private static final class Page {
        /** The document. 문서. */
        final Document doc;
        /** The style engine. 스타일 엔진. */
        final CssEngine css;

        /**
         * Parses a page with a style sheet and a body (the text is 10px monospace, the paragraph <code>#p</code> holds the text that is highlighted).
         * <p>
         * 스타일 시트와 body 로 페이지를 파싱한다(텍스트는 10px monospace, 문단 <code>#p</code> 가 하이라이트되는 텍스트를 담는다).
         *
         * @param style the style sheet / 스타일 시트
         * @param body  the body / body
         */
        Page(String style, String body) {
            doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + RenderTestSupport.RESET + "<style>p{margin:0;font:20px monospace;line-height:30px}" + style
                    + "</style></head><body>" + body + "</body></html>", "http://t.test/dir/page.html", null, true);
            css = new CssEngine(doc, null, new MediaEnvironment(300, 100));
            css.loadStyleSheets();
        }

        /**
         * Returns the text node of an element.
         * <p>
         * 요소의 텍스트 노드를 반환한다.
         *
         * @param id the id of the element / 요소의 id
         * @return the first child / 첫 자식
         */
        Node text(String id) {
            return doc.getElementById(id).getFirstChild();
        }

        /**
         * Creates a live range over a part of a text node.
         * <p>
         * 텍스트 노드의 일부에 대한 살아 있는 범위를 만든다.
         *
         * @param n    the text node / 텍스트 노드
         * @param from the start / 시작
         * @param to   the end / 끝
         * @return the range / 범위
         */
        Range range(Node n, int from, int to) {
            Range r = doc.createRange();
            r.setStart(n, from);
            r.setEnd(n, to);
            return r;
        }

        /**
         * Lays the page out and paints it.
         * <p>
         * 페이지를 배치해 그린다.
         *
         * @return the image / 이미지
         */
        BufferedImage paint() {
            RenderedPage p = PageRenderer.render(doc, css, null, 300, 100);
            return RenderTestSupport.paint(p, 300, 100);
        }

        /**
         * Returns the left edge of the text of an element in the painted page, and the advance of one character.
         * <p>
         * 그린 페이지에서 요소의 텍스트 왼쪽 가장자리와 글자 하나의 advance 를 반환한다.
         *
         * @param id the id / id
         * @return {x, y, advance} / {x, y, advance}
         */
        double[] metrics(String id) {
            RenderedPage p = PageRenderer.render(doc, css, null, 300, 100);
            Element e = doc.getElementById(id);
            LayoutBox t = p.getLayout().getBoxes(e.getFirstChild()).get(0);
            return new double[] { t.getX(), t.getY(), t.getWidth() / t.getText().length() };
        }
    }

    /**
     * Tells whether a pixel is close to a color.
     * <p>
     * 화소가 색에 가까운지 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @param rgb the color / 색
     * @return true if close / 가까우면 true
     */
    private static boolean is(BufferedImage img, int x, int y, int rgb) {
        return RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), rgb, 8);
    }

    /**
     * Counts the pixels of a rectangle that are close to a color.
     * <p>
     * 사각형에서 색에 가까운 화소를 센다.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right / 오른쪽
     * @param y1  bottom / 아래쪽
     * @param rgb the color / 색
     * @return the count / 수
     */
    private static int count(BufferedImage img, int x0, int y0, int x1, int y1, int rgb) {
        int n = 0;
        // 사각형의 화소를 훑으며 센다.
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), rgb, 60)) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * A registered highlight paints its background under the covered characters and the text in its color; the rest of the text keeps its own color.
     * <p>
     * 등록된 하이라이트는 덮는 글자 밑에 배경을 그리고 텍스트를 자신의 색으로 그린다. 나머지 텍스트는 자신의 색을 지킨다.
     */
    public void testBackgroundAndColor() {
        Page pg = new Page("::highlight(found){background-color:yellow;color:blue}", "<p id=p>One two three</p>");
        double[] m = pg.metrics("p");
        Highlight h = new Highlight();
        h.add(pg.range(pg.text("p"), 0, 7));
        pg.doc.getHighlights().set("found", h);
        BufferedImage img = pg.paint();
        int y = (int) m[1] + 1;
        Check.isTrue(is(img, (int) (m[0] + 1), y, 0xFFFF00), "the background of the first character is yellow");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 6.5), y, 0xFFFF00), "the background of the seventh character is yellow");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 8.5), y, 0xFFFFFF), "the ninth character is not highlighted");
        Check.isTrue(count(img, (int) m[0], (int) m[1], (int) (m[0] + m[2] * 7), (int) m[1] + 30, 0x0000FF) > 5, "the highlighted text is blue");
        Check.equal(0, count(img, (int) (m[0] + m[2] * 8), (int) m[1], (int) (m[0] + m[2] * 13), (int) m[1] + 30, 0x0000FF), "the other text is not blue");
        Check.isTrue(count(img, (int) (m[0] + m[2] * 8), (int) m[1], (int) (m[0] + m[2] * 13), (int) m[1] + 30, 0x000000) > 5, "it stays black");
    }

    /**
     * Without a registration, or without a rule for the name, nothing changes; a collapsed range and a range of another document are not painted.
     * <p>
     * 등록이 없거나 이름에 대한 규칙이 없으면 아무것도 바뀌지 않는다. 접힌 범위와 다른 문서의 범위는 그리지 않는다.
     */
    public void testWhatIsNotPainted() {
        Page pg = new Page("::highlight(found){background-color:yellow}", "<p id=p>One two three</p>");
        double[] m = pg.metrics("p");
        int y = (int) m[1] + 1;
        Highlight h = new Highlight();
        h.add(pg.range(pg.text("p"), 0, 7));
        Check.isTrue(is(pg.paint(), (int) (m[0] + 1), y, 0xFFFFFF), "a highlight that is not registered is not painted");
        pg.doc.getHighlights().set("other", h);
        Check.isTrue(is(pg.paint(), (int) (m[0] + 1), y, 0xFFFFFF), "a name without a ::highlight() rule paints nothing");
        pg.doc.getHighlights().delete("other");
        Highlight c = new Highlight();
        c.add(pg.range(pg.text("p"), 2, 2));
        Page other = new Page("", "<p id=q>elsewhere</p>");
        c.add(other.range(other.text("q"), 0, 4));
        pg.doc.getHighlights().set("found", c);
        Check.isTrue(is(pg.paint(), (int) (m[0] + m[2] * 2), y, 0xFFFFFF), "a collapsed range and a range of another document are ignored");
    }

    /**
     * The highlight style inherits from the highlight of the parent element, even <code>background-color</code>, and a rule on the element overrides only
     * what it sets (CSS Pseudo-Elements 4 section 3.5).
     * <p>
     * 하이라이트 스타일은 부모 요소의 하이라이트에서 <code>background-color</code> 까지 물려받고, 요소의 규칙은 정한 것만 바꾼다(CSS Pseudo-Elements 4 3.5 절).
     */
    public void testInheritanceFromTheParentHighlight() {
        Page pg = new Page(":root::highlight(x){background-color:yellow;color:blue} #s::highlight(x){color:red}", "<p id=p>ab<span id=s>cd</span>ef</p>");
        double[] m = pg.metrics("p");
        Highlight h = new Highlight();
        Range r = pg.doc.createRange();
        r.selectNodeContents(pg.doc.getElementById("p"));
        h.add(r);
        pg.doc.getHighlights().set("x", h);
        BufferedImage img = pg.paint();
        int y = (int) m[1] + 1;
        Check.isTrue(is(img, (int) (m[0] + 1), y, 0xFFFF00), "the paragraph inherits the highlight of :root");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 2.5), y, 0xFFFF00), "the span inherits background-color from the parent highlight");
        Check.isTrue(count(img, (int) (m[0] + m[2] * 2), (int) m[1], (int) (m[0] + m[2] * 4), (int) m[1] + 30, 0xFF0000) > 5, "and has its own red color");
        Check.isTrue(count(img, (int) m[0], (int) m[1], (int) (m[0] + m[2] * 2), (int) m[1] + 30, 0x0000FF) > 5, "the paragraph text is blue");
    }

    /**
     * Overlapping highlights: the later registration is above at the same priority, a higher priority is above, and a layer whose color is
     * <code>currentcolor</code> keeps the color of the layer below (the example of section 4.2.5).
     * <p>
     * 겹치는 하이라이트: 같은 우선순위에서는 나중에 등록한 것이 위이고, 우선순위가 높은 것이 위이며, 색이 <code>currentcolor</code> 인 층은 아래 층의 색을 쓴다(4.2.5 절의 예).
     */
    public void testOverlappingHighlights() {
        Page pg = new Page(":root::highlight(foo){color:blue;background-color:yellow} :root::highlight(bar){background-color:orange}", "<p id=p>Some text</p>");
        double[] m = pg.metrics("p");
        Highlight h1 = new Highlight();
        h1.add(pg.range(pg.text("p"), 0, 6));
        Highlight h2 = new Highlight();
        h2.add(pg.range(pg.text("p"), 3, 9));
        pg.doc.getHighlights().set("foo", h1);
        pg.doc.getHighlights().set("bar", h2);
        BufferedImage img = pg.paint();
        int y = (int) m[1] + 1;
        Check.isTrue(is(img, (int) (m[0] + m[2] * 1.5), y, 0xFFFF00), "'Som' has the yellow background of foo");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 4.5), y, 0xFFA500), "'e t' has the orange background of bar, registered later");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 7.5), y, 0xFFA500), "'ext' is orange");
        Check.isTrue(count(img, (int) (m[0] + m[2] * 3), (int) m[1], (int) (m[0] + m[2] * 6), (int) m[1] + 30, 0x0000FF) > 3, "'e t' keeps the blue text of foo");
        Check.equal(0, count(img, (int) (m[0] + m[2] * 6.2), (int) m[1], (int) (m[0] + m[2] * 9), (int) m[1] + 30, 0x0000FF), "'ext' is not blue");
        h1.setPriority(1);
        BufferedImage up = pg.paint();
        Check.isTrue(is(up, (int) (m[0] + m[2] * 4.5), y, 0xFFFF00), "with a higher priority foo is above: 'e t' is yellow");
        Check.isTrue(is(up, (int) (m[0] + m[2] * 7.5), y, 0xFFA500), "'ext' stays orange");
    }

    /**
     * Static ranges are painted; overlapping ranges of one highlight are one area; a decoration of the highlight is drawn under its part only.
     * <p>
     * 정적 범위도 그린다. 한 하이라이트의 겹치는 범위는 한 영역이다. 하이라이트의 장식은 그 부분 아래에만 그린다.
     */
    public void testStaticRangesAndDecoration() {
        Page pg = new Page("::highlight(s){background-color:rgba(0,0,255,0.5)} ::highlight(u){text-decoration:underline;text-decoration-color:red;text-decoration-thickness:3px}",
                "<p id=p>Lorem Ipsum.</p>");
        double[] m = pg.metrics("p");
        Highlight s = new Highlight();
        s.add(new StaticRange(pg.text("p"), 1, pg.text("p"), 5));
        s.add(new StaticRange(pg.text("p"), 3, pg.text("p"), 7));
        pg.doc.getHighlights().set("s", s);
        BufferedImage img = pg.paint();
        int y = (int) m[1] + 1;
        int a = RenderTestSupport.rgb(img, (int) (m[0] + m[2] * 2), y);
        int b = RenderTestSupport.rgb(img, (int) (m[0] + m[2] * 4), y);
        Check.isTrue(RenderTestSupport.near(a, 0x8080FF, 6), "a static range is painted half blue");
        Check.equal(a, b, "the overlap of two ranges of one highlight is not darker");
        Check.isTrue(is(img, (int) (m[0] + m[2] * 8), y, 0xFFFFFF), "after the ranges nothing");
        pg.doc.getHighlights().clear();
        Highlight u = new Highlight();
        u.add(pg.range(pg.text("p"), 0, 5));
        pg.doc.getHighlights().set("u", u);
        BufferedImage line = pg.paint();
        Check.isTrue(count(line, (int) m[0], (int) m[1], (int) (m[0] + m[2] * 5), (int) m[1] + 30, 0xFF0000) > 8, "a red underline under 'Lorem'");
        Check.equal(0, count(line, (int) (m[0] + m[2] * 6), (int) m[1], (int) (m[0] + m[2] * 11), (int) m[1] + 30, 0xFF0000), "none under 'Ipsum'");
    }
}
