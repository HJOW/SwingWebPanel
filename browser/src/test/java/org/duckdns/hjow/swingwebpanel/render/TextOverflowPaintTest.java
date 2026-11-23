package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of truncated lines (plan item P10-08, <code>text-overflow</code> and <code>line-clamp</code>): the page that is truncated
 * is painted exactly as a page whose text is the kept text followed by the ellipsis, so that the ellipsis is drawn, nothing is drawn beyond it, and the
 * clamped lines are not painted.
 * <p>
 * 잘린 줄의 그리기 시험(계획 P10-08, <code>text-overflow</code> 와 <code>line-clamp</code>): 잘린 페이지는 남은 텍스트 뒤에 줄임표가 붙은 텍스트를 가진 페이지와 똑같이
 * 그려진다. 즉 줄임표가 그려지고, 그 뒤에는 아무것도 그려지지 않으며, 잘린 줄은 그려지지 않는다.
 */
public class TextOverflowPaintTest {

    /** A text of many letters. 글자가 많은 텍스트. */
    private static final String LONG = "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextOverflowPaintTest() {
    }

    /**
     * Returns the text of the first line of the element <code>d</code> of a rendered page.
     * <p>
     * 렌더링된 페이지의 요소 <code>d</code> 의 첫 줄의 텍스트를 반환한다.
     *
     * @param p the page / 페이지
     * @return the text / 텍스트
     */
    private static String firstLine(RenderedPage p) {
        LayoutResult r = p.getLayout();
        List<LayoutBox> boxes = r.getBoxes(RenderTestSupport.lastDocument.getElementById("d"));
        StringBuilder sb = new StringBuilder();
        LayoutBox line = null;
        // 요소의 상자 아래에서 첫 줄 상자를 찾는다.
        for (LayoutBox c : boxes.get(0).getChildren()) {
            if (c.getType() == LayoutBox.Type.LINE && line == null) {
                line = c;
            }
        }
        // 줄의 텍스트 조각을 이어 붙인다.
        for (LayoutBox c : line.getChildren()) {
            if (c.getType() == LayoutBox.Type.TEXT) {
                sb.append(c.getText());
            }
        }
        return sb.toString();
    }

    /**
     * The truncated line is painted like the line of the kept text and the ellipsis.
     * <p>
     * 잘린 줄은 남은 텍스트와 줄임표로 된 줄처럼 그려진다.
     */
    public void testEllipsisIsPainted() {
        String box = "width:100px;white-space:nowrap;overflow:hidden;";
        RenderedPage cut = RenderTestSupport.page("<div id=d style='" + box + "text-overflow:ellipsis'>" + LONG + "</div>", 200, 30, null);
        String kept = firstLine(cut);
        Check.isTrue(kept.endsWith("…"), "the line ends in an ellipsis: " + kept);
        RenderedPage ref = RenderTestSupport.page("<div id=d style='" + box + "'>" + kept + "</div>", 200, 30, null);
        BufferedImage a = RenderTestSupport.paint(cut, 200, 30);
        BufferedImage b = RenderTestSupport.paint(ref, 200, 30);
        int ink = RenderTestSupport.ink(a, 0, 0, 100, 14);
        Check.isTrue(ink > 20, "the text is drawn, " + ink);
        Check.equal(0, RenderTestSupport.ink(a, 101, 0, 200, 30), "nothing is drawn beyond the box");
        int different = 0;
        // 두 이미지를 상자 영역에서 화소마다 비교한다.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 100; x++) {
                if (RenderTestSupport.rgb(a, x, y) != RenderTestSupport.rgb(b, x, y)) {
                    different++;
                }
            }
        }
        Check.equal(0, different, "the same pixels as the text that holds the ellipsis itself");
    }

    /**
     * The clamped lines are not painted: the page below the clamp has no ink.
     * <p>
     * 잘린 줄은 그려지지 않는다: clamp 아래의 페이지에는 잉크가 없다.
     */
    public void testClampedLinesAreNotPainted() {
        RenderedPage p = RenderTestSupport.page("<p id=d style='margin:0;width:60px;line-clamp:2'>" + LONG + "</p>", 200, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 200, 100);
        LayoutBox box = p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("d")).get(0);
        int bottom = (int) Math.ceil(box.getY() + box.getHeight());
        Check.isTrue(RenderTestSupport.ink(img, 0, 0, 200, bottom) > 20, "the first two lines are drawn");
        Check.equal(0, RenderTestSupport.ink(img, 0, bottom + 1, 200, 100), "and nothing below them");
    }
}
