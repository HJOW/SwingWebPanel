package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.StickyConstraint;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>position:sticky</code> (CSS Positioned Layout Module Level 3, section 3.4): the painting and the hit test shift the box
 * while the page is scrolled so that its border edge stays at the inset from the edge of the viewport, never leaving the content box of its
 * containing block. The shift is checked with real pixels of an off-screen image at several scroll positions (for a host that applies the
 * scroll to the graphics and for a host such as a <code>JScrollPane</code> that scrolled the graphics already), for the vertical and horizontal
 * axes, for descendants that follow the box, for the hit test and for the cases that are not shifted.
 * <p>
 * <code>position:sticky</code> 시험(CSS Positioned Layout Module Level 3 3.4 절): 페이지가 스크롤되는 동안 그리기와 히트 테스트가 상자를 옮겨, border 가장자리는
 * 뷰포트 가장자리에서 오프셋만큼 떨어진 곳에 머물고 포함 블록의 콘텐츠 상자를 벗어나지 않는다. 옮김은 여러 스크롤 위치에서 화면 밖 이미지의 실제 픽셀로 확인한다(그래픽스에
 * 스크롤을 적용하는 호스트와 <code>JScrollPane</code> 처럼 이미 스크롤한 그래픽스를 주는 호스트, 세로와 가로 축, 상자를 따라가는 자손, 히트 테스트, 옮기지 않는 경우).
 */
public class StickyPaintTest {

    /** The red of the sticky boxes. sticky 상자의 빨강. */
    private static final int RED = 0xFF0000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StickyPaintTest() {
    }

    /**
     * Returns the id of the element of a hit.
     * <p>
     * 히트의 요소 id 를 반환한다.
     *
     * @param h the hit / 히트
     * @return the id or "" / id 또는 ""
     */
    private static String idOf(HitResult h) {
        return h.getElement() == null || h.getElement().getAttribute("id") == null ? "" : h.getElement().getAttribute("id");
    }

    /**
     * Paints a page whose scroll is applied to the graphics by the painter.
     * <p>
     * 페이지를 그리기 단계가 그래픽스에 스크롤을 적용하는 방식으로 그린다.
     *
     * @param p       the page / 페이지
     * @param w       image width / 이미지 너비
     * @param h       image height / 이미지 높이
     * @param scrollX horizontal scroll / 가로 스크롤
     * @param scrollY vertical scroll / 세로 스크롤
     * @return the image / 이미지
     */
    private static BufferedImage paintAt(RenderedPage p, int w, int h, int scrollX, int scrollY) {
        BufferedImage img = RenderTestSupport.blank(w, h);
        Graphics2D g = img.createGraphics();
        try {
            p.paint(g, new Rectangle(0, 0, w, h), scrollX, scrollY);
        } finally {
            g.dispose();
        }
        return img;
    }

    /** The markup of a section with a sticky header: 50px of spacer, then a 600px section whose first child is the 20px header. 50px 간격 뒤에 첫 자식이 20px 머리글인 600px 구역이 있는 마크업. */
    private static final String SECTION = "<div style='height:50px'></div><div id=sec style='height:600px'>"
            + "<div id=hd style='position:sticky;top:10px;height:20px;width:50px;background:#f00'></div><div style='height:580px;background:#eee'></div></div>"
            + "<div style='height:400px;background:#ddd'></div>";

    /**
     * The header stays 10px below the top edge of the viewport while it scrolls, and rests at its place before that.
     * <p>
     * 머리글은 스크롤하는 동안 뷰포트 위쪽 가장자리에서 10px 아래에 머물고, 그 전에는 제자리에 있다.
     */
    public void testHeaderSticksToTheTopOfTheViewport() {
        RenderedPage p = RenderTestSupport.page(SECTION, 200, 100, null);
        BufferedImage top = paintAt(p, 200, 100, 0, 0);
        Check.equal(RED, RenderTestSupport.rgb(top, 10, 60), "scroll 0: the header is at its place (y 50..70)");
        Check.isFalse(RED == RenderTestSupport.rgb(top, 10, 20), "scroll 0: not moved up");
        BufferedImage scrolled = paintAt(p, 200, 100, 0, 100);
        Check.equal(RED, RenderTestSupport.rgb(scrolled, 10, 15), "scroll 100: the header is 10px below the viewport top");
        Check.equal(RED, RenderTestSupport.rgb(scrolled, 10, 28), "scroll 100: the header is 20px high");
        Check.isFalse(RED == RenderTestSupport.rgb(scrolled, 10, 5), "scroll 100: nothing above the inset");
        Check.isFalse(RED == RenderTestSupport.rgb(scrolled, 10, 35), "scroll 100: nothing below the header");
        Check.isFalse(RED == RenderTestSupport.rgb(scrolled, 80, 15), "the header is 50px wide");
    }

    /**
     * The header stops at the end of its containing block (the section) and scrolls away with it.
     * <p>
     * 머리글은 포함 블록(구역)의 끝에서 멈추고 구역과 함께 스크롤되어 사라진다.
     */
    public void testHeaderStopsAtTheEndOfTheContainingBlock() {
        RenderedPage p = RenderTestSupport.page(SECTION, 200, 100, null);
        // 구역의 내용 상자는 문서 y 50..650, 머리글의 마진 상자는 50..70 이므로 이동량의 한계는 580(머리글이 630..650 에 놓일 때)이다.
        BufferedImage atLimit = paintAt(p, 200, 100, 0, 620);
        Check.equal(RED, RenderTestSupport.rgb(atLimit, 10, 15), "scroll 620: still 10px below the viewport top");
        BufferedImage past = paintAt(p, 200, 100, 0, 640);
        Check.equal(RED, RenderTestSupport.rgb(past, 10, 5), "scroll 640: held at the end of the section (document y 630..650)");
        Check.equal(RED, RenderTestSupport.rgb(past, 10, 8), "scroll 640: the header ends at the section end");
        Check.isFalse(RED == RenderTestSupport.rgb(past, 10, 15), "scroll 640: not below the end of the section");
        RenderedPage q = p;
        double[] shift = RenderTestSupport.box(q, "hd").getSticky().shift(new Rectangle2D.Double(0, 640, 200, 100));
        Check.equal(580.0, shift[1], "the shift is limited by the containing block");
        Check.equal(0.0, shift[0], "no horizontal shift without a left or right inset");
    }

    /**
     * A box with a bottom inset sticks to the bottom of the viewport while its place is below the visible part, then rests.
     * <p>
     * 아래쪽 오프셋이 있는 상자는 제자리가 보이는 부분 아래에 있는 동안 뷰포트 아래쪽에 붙고 그 뒤에는 제자리에 머문다.
     */
    public void testFooterSticksToTheBottom() {
        RenderedPage p = RenderTestSupport.page("<div style='height:500px;background:#eee'></div>"
                + "<div id=ft style='position:sticky;bottom:0;height:20px;width:50px;background:#f00'></div><div style='height:300px;background:#ddd'></div>", 200, 100, null);
        BufferedImage top = paintAt(p, 200, 100, 0, 0);
        Check.equal(RED, RenderTestSupport.rgb(top, 10, 90), "scroll 0: held at the viewport bottom (y 80..100)");
        Check.isFalse(RED == RenderTestSupport.rgb(top, 10, 70), "scroll 0: not above the bottom edge");
        BufferedImage near = paintAt(p, 200, 100, 0, 440);
        Check.equal(RED, RenderTestSupport.rgb(near, 10, 70), "scroll 440: the footer is at its place (y 500..520 is device 60..80)");
        Check.equal(RED, RenderTestSupport.rgb(near, 10, 78), "scroll 440: the footer ends at 80");
        Check.isFalse(RED == RenderTestSupport.rgb(near, 10, 90), "scroll 440: it does not follow the bottom edge any more");
    }

    /**
     * The horizontal axis works the same way with <code>left</code>.
     * <p>
     * 가로 축도 <code>left</code> 로 같게 동작한다.
     */
    public void testHorizontalSticky() {
        RenderedPage p = RenderTestSupport.page("<div id=row style='width:400px;height:40px;background:#eee'>"
                + "<div id=hd style='position:sticky;left:5px;margin-left:100px;width:30px;height:20px;background:#f00'></div></div>", 150, 60, null);
        BufferedImage top = paintAt(p, 150, 60, 0, 0);
        Check.equal(RED, RenderTestSupport.rgb(top, 110, 10), "scroll 0: at its place (x 100..130)");
        BufferedImage scrolled = paintAt(p, 150, 60, 200, 0);
        Check.equal(RED, RenderTestSupport.rgb(scrolled, 10, 10), "scroll 200: 5px from the viewport left (document x 205..235)");
        Check.equal(RED, RenderTestSupport.rgb(scrolled, 33, 10), "scroll 200: 30px wide");
        Check.isFalse(RED == RenderTestSupport.rgb(scrolled, 2, 10), "scroll 200: nothing left of the inset");
    }

    /**
     * Descendants follow the sticky box, also the absolutely positioned ones that the box contains; the box with an auto offset on all
     * sides and the box that is static are not shifted.
     * <p>
     * 자손은 sticky 상자를 따라간다. 상자가 포함하는 절대 배치 자손도 마찬가지이다. 모든 오프셋이 auto 인 상자와 static 상자는 옮기지 않는다.
     */
    public void testDescendantsFollowAndPlainBoxesStay() {
        RenderedPage p = RenderTestSupport.page("<div style='height:50px'></div><div id=sec style='height:600px'>"
                + "<div id=hd style='position:sticky;top:0;height:20px;width:50px;background:#f00'>"
                + "<div style='position:absolute;left:0;top:0;width:10px;height:10px;background:#00f'></div></div>"
                + "<div id=auto style='position:sticky;height:20px;width:50px;background:#0f0'></div>"
                + "<div style='height:560px;background:#eee'></div></div>", 200, 100, null);
        BufferedImage scrolled = paintAt(p, 200, 100, 0, 100);
        Check.equal(0x0000FF, RenderTestSupport.rgb(scrolled, 5, 5), "the absolutely positioned child is at the corner of the moved box");
        Check.equal(RED, RenderTestSupport.rgb(scrolled, 25, 5), "the sticky box is at the viewport top");
        Check.isNull(RenderTestSupport.box(p, "auto").getSticky(), "all insets auto: no constraint");
        Check.notNull(RenderTestSupport.box(p, "hd").getSticky(), "a box with an inset has one");
    }

    /**
     * A host that scrolled the graphics already (a <code>JScrollPane</code> moves the component) gives the position of the viewport
     * separately, and the box sticks to it.
     * <p>
     * 이미 그래픽스를 스크롤한 호스트(<code>JScrollPane</code> 은 컴포넌트를 옮긴다)는 뷰포트 위치를 따로 주며 상자는 그것에 붙는다.
     */
    public void testHostThatScrolledTheGraphics() {
        RenderedPage p = RenderTestSupport.page(SECTION, 200, 100, null);
        BufferedImage img = RenderTestSupport.blank(200, 100);
        Graphics2D g = img.createGraphics();
        try {
            // 호스트가 컴포넌트를 위로 100px 옮긴 것처럼 그래픽스를 옮기고, 보이는 영역은 문서 y 100..200 이다.
            g.translate(0, -100);
            p.paint(g, new Rectangle(0, 100, 200, 100), 0, 0, 1.0, 0L, 0, 100);
        } finally {
            g.dispose();
        }
        Check.equal(RED, RenderTestSupport.rgb(img, 10, 15), "the header is 10px below the viewport top although the graphics were scrolled by the host");
        Check.isFalse(RED == RenderTestSupport.rgb(img, 10, 60), "not at its static place");
    }

    /**
     * A fixed box also stays at the viewport when the host scrolled the graphics already.
     * <p>
     * 호스트가 이미 그래픽스를 스크롤했어도 fixed 상자는 뷰포트에 머문다.
     */
    public void testFixedBoxWithHostScroll() {
        RenderedPage p = RenderTestSupport.page("<div style='height:1000px;background:#eee'></div>"
                + "<div id=fx style='position:fixed;left:20px;top:30px;width:40px;height:40px;background:#f00'></div>", 200, 100, null);
        BufferedImage img = RenderTestSupport.blank(200, 100);
        Graphics2D g = img.createGraphics();
        try {
            g.translate(0, -500);
            p.paint(g, new Rectangle(0, 500, 200, 100), 0, 0, 1.0, 0L, 0, 500);
        } finally {
            g.dispose();
        }
        Check.equal(RED, RenderTestSupport.rgb(img, 40, 50), "the fixed box is at the same place of the viewport");
        Check.equal(0xEEEEEE, RenderTestSupport.rgb(img, 150, 50), "the page behind it");
    }

    /**
     * The hit test follows the shifted box: the click finds the header where it is painted and not where it would have been.
     * <p>
     * 히트 테스트는 옮겨진 상자를 따라간다: 클릭은 머리글이 있었을 자리가 아니라 그려진 자리에서 머리글을 찾는다.
     */
    public void testHitTestFollowsTheShift() {
        RenderedPage p = RenderTestSupport.page(SECTION, 200, 100, null);
        Check.equal("hd", idOf(p.hitTestDocument(10, 60)), "scroll 0: the header at its place");
        // 스크롤 100 에서 머리글은 화면 y 10..30 에 그려진다(문서 y 110..130).
        Check.equal("hd", idOf(p.hitTest(10, 20, 0, 100, 1.0)), "scroll 100: the header at the viewport position");
        Check.isFalse("hd".equals(idOf(p.hitTest(10, 55, 0, 100, 1.0))), "scroll 100: not at the place it would have been");
        Check.isFalse("hd".equals(idOf(p.hitTest(80, 20, 0, 100, 1.0))), "scroll 100: outside the width");
    }

    /**
     * A box inside an element with <code>overflow</code> other than visible is not shifted (the engine does not scroll such an element),
     * nor is an inline box; the root and the body do not count as such an element.
     * <p>
     * <code>overflow</code> 가 visible 이 아닌 요소 안의 상자는 옮기지 않고(엔진은 그런 요소를 스크롤하지 않는다) 인라인 상자도 옮기지 않는다. 루트와 body 는 그런
     * 요소로 치지 않는다.
     */
    public void testBoxesThatAreNotShifted() {
        RenderedPage p = RenderTestSupport.page("<div style='overflow:auto;height:200px'><div id=in style='position:sticky;top:0;height:20px;background:#f00'></div></div>"
                + "<span id=sp style='position:sticky;top:0'>inline</span>", 200, 100, null);
        Check.isNull(RenderTestSupport.box(p, "in").getSticky(), "inside an overflow:auto box");
        LayoutBox span = RenderTestSupport.box(p, "sp");
        Check.isNull(span.getSticky(), "an inline box");
        RenderedPage q = RenderTestSupport.page("<style>body{overflow-x:hidden}</style><div style='height:300px'></div><div id=b style='position:sticky;top:0;height:20px'></div>", 200, 100, null);
        Check.notNull(RenderTestSupport.box(q, "b").getSticky(), "the overflow of the body belongs to the viewport");
    }

    /**
     * The shift along one axis: the insets, the limit of the containing block and which edge wins when the box is too large for both.
     * <p>
     * 한 축의 이동량: 오프셋, 포함 블록의 한계, 상자가 둘을 모두 만족시키기에 너무 클 때 이기는 가장자리.
     */
    public void testAxisAlgorithm() {
        // 마진 상자 100..150, border 상자 100..150, 포함 블록 0..1000, 뷰포트 0..80 + 스크롤.
        Check.equal(0.0, StickyConstraintAccess.axis(100, 150, 100, 150, 0, 1000, 0, 80, 10, Double.NaN, true), "box below the inset: no shift");
        Check.equal(30.0, StickyConstraintAccess.axis(100, 150, 100, 150, 0, 1000, 120, 200, 10, Double.NaN, true), "box above the inset: moved to 130");
        Check.equal(850.0, StickyConstraintAccess.axis(100, 150, 100, 150, 0, 1000, 1000, 1100, 0, Double.NaN, true), "limited by the end of the containing block (850 = 1000 - 150)");
        Check.equal(-70.0, StickyConstraintAccess.axis(100, 150, 100, 150, 0, 1000, 0, 80, Double.NaN, 0, true), "bottom inset: the end edge held at the viewport end");
        // 상자(높이 200)가 뷰포트(100)보다 커서 두 오프셋을 모두 만족시킬 수 없으면 시작 가장자리가 이긴다.
        double both = StickyConstraintAccess.axis(0, 200, 0, 200, 0, 1000, 50, 150, 0, 0, true);
        Check.equal(50.0, both, "start wins: the top edge is at the viewport top");
        double endWins = StickyConstraintAccess.axis(0, 200, 0, 200, -100, 1000, 50, 150, 0, 0, false);
        Check.equal(-50.0, endWins, "end wins (right-to-left): the end edge is at the viewport end");
    }

    /**
     * Gives the test access to the shift of one axis through the public API (a constraint of a made-up geometry).
     * <p>
     * 공개 API 로 한 축의 이동량을 시험할 수 있게 하는 도우미(만든 기하의 제약).
     */
    private static final class StickyConstraintAccess {
        /**
         * Computes the shift of one axis through a constraint of that axis.
         * <p>
         * 한 축의 제약으로 그 축의 이동량을 계산한다.
         *
         * @param ps        margin start / 마진 시작
         * @param pe        margin end / 마진 끝
         * @param bs        border start / border 시작
         * @param be        border end / border 끝
         * @param cs        container start / 컨테이너 시작
         * @param ce        container end / 컨테이너 끝
         * @param vs        viewport start / 뷰포트 시작
         * @param ve        viewport end / 뷰포트 끝
         * @param insetS    start inset / 시작 오프셋
         * @param insetE    end inset / 끝 오프셋
         * @param startWins true if the start wins / 시작이 이기면 true
         * @return the shift / 이동량
         */
        static double axis(double ps, double pe, double bs, double be, double cs, double ce, double vs, double ve, double insetS, double insetE, boolean startWins) {
            return StickyConstraint.axisShift(ps, pe, bs, be, cs, ce, vs, ve, insetS, insetE, startWins);
        }
    }
}
