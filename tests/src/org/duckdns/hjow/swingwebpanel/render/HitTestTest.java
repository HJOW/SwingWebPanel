package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.Rectangle2D;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of hit testing and coordinates (plan item P09-06): nested blocks, links with absolute URLs, text offsets, scroll and scale, bounding
 * boxes and client rectangles.
 * <p>
 * 히트 테스트와 좌표 시험(계획 P09-06): 중첩된 블록, 절대 URL 이 있는 링크, 텍스트 오프셋, 스크롤과 배율, 경계 상자와 클라이언트 사각형.
 */
public class HitTestTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HitTestTest() {
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
     * The deepest element at a point is found, also inside nested blocks.
     * <p>
     * 중첩된 블록 안에서도 점 위의 가장 깊은 요소를 찾는다.
     */
    public void testNestedBlocks() {
        RenderedPage p = RenderTestSupport.page("<div id='o' style='padding:10px;width:120px'><div id='i' style='width:100px;height:30px'></div>"
                + "<div id='j' style='width:100px;height:30px'></div></div>", 300, 200, null);
        Check.equal("i", idOf(p.hitTestDocument(50, 20)), "inside the first inner block");
        Check.equal("j", idOf(p.hitTestDocument(50, 55)), "inside the second inner block");
        Check.equal("o", idOf(p.hitTestDocument(5, 5)), "in the padding of the outer block");
        Check.isNull(p.hitTestDocument(50, 20).getLink(), "no link");
        Check.isNull(p.hitTestDocument(50, 20).getHref(), "no URL");
        Check.equal(-1, p.hitTestDocument(50, 20).getTextOffset(), "no text offset over a block");
    }

    /**
     * A link is found from its text and from nested inline elements, with the URL resolved against the base URL.
     * <p>
     * 링크는 그 텍스트와 중첩된 인라인 요소에서 찾으며 URL 은 기준 URL 로 해석한다.
     */
    public void testLinks() {
        RenderedPage p = RenderTestSupport.page("<div style='padding:10px'><a id='l' href='/x/y?q=1#f'>link <b id='b'>bold</b></a> plain</div>"
                + "<div style='padding:10px'><a id='r' href='rel.html'>relative</a></div>", 300, 200, null);
        Element l = RenderTestSupport.lastDocument.getElementById("l");
        List<Rectangle2D> rects = p.getHitTester().getClientRects(l);
        Check.isFalse(rects.isEmpty(), "the link has a rectangle");
        Rectangle2D r = rects.get(0);
        HitResult h = p.hitTestDocument(r.getX() + 3, r.getY() + r.getHeight() / 2);
        Check.same(l, h.getLink(), "the link element");
        Check.equal("http://t.test/x/y?q=1#f", h.getHref(), "an absolute path is resolved against the origin of the base URL");
        Element b = RenderTestSupport.lastDocument.getElementById("b");
        Rectangle2D rb = p.getHitTester().getBoundingBox(b);
        HitResult hb = p.hitTestDocument(rb.getX() + 2, rb.getY() + rb.getHeight() / 2);
        Check.equal("b", idOf(hb), "the nested inline element is hit");
        Check.same(l, hb.getLink(), "and its enclosing link is found");
        Element rel = RenderTestSupport.lastDocument.getElementById("r");
        Rectangle2D rr = p.getHitTester().getBoundingBox(rel);
        Check.equal("http://t.test/dir/rel.html", p.hitTestDocument(rr.getX() + 2, rr.getY() + 2).getHref(), "a relative URL is resolved against the document URL");
        Check.isNull(p.hitTestDocument(280, 150).getLink(), "no link in empty space");
    }

    /**
     * The character offset is the caret position nearest to the point.
     * <p>
     * 문자 오프셋은 점에 가장 가까운 캐럿 위치이다.
     */
    public void testTextOffsets() {
        RenderedPage p = RenderTestSupport.page("<div id='t' style='padding:10px 0 0 10px;font-size:16px'>abcdefghij</div>", 300, 100, null);
        Node text = RenderTestSupport.lastDocument.getElementById("t").getFirstChild();
        LayoutBox b = p.getLayout().getBoxes(text).get(0);
        double adv = b.getWidth() / 10.0;
        double y = b.getY() + b.getHeight() / 2;
        HitResult h2 = p.hitTestDocument(b.getX() + 2.4 * adv, y);
        Check.same(text, h2.getTextNode(), "the text node is reported");
        Check.equal(2, h2.getTextOffset(), "2.4 characters from the left: caret before the third character");
        Check.equal(3, p.hitTestDocument(b.getX() + 2.6 * adv, y).getTextOffset(), "2.6 characters: caret after the third character");
        Check.equal(0, p.hitTestDocument(b.getX() + 0.2, y).getTextOffset(), "at the left edge");
        Check.equal(10, p.hitTestDocument(b.getX() + b.getWidth() - 0.2, y).getTextOffset(), "at the right edge");
    }

    /**
     * Scroll and scale are the inverse of the transform that the painter applies.
     * <p>
     * 스크롤과 배율은 페인터가 적용하는 변환의 역이다.
     */
    public void testScrollAndScaleConversions() {
        RenderedPage p = RenderTestSupport.page("<div style='height:100px'></div><div id='t' style='width:60px;height:40px'></div>"
                + "<div style='height:400px'></div>", 200, 100, null);
        // 문서 (30, 120) 은 스크롤 (0, 50), 배율 2 에서 화면 (60, 140) 이다.
        Check.equal(60.0, HitTester.toScreenX(30, 0, 2.0), "document x to screen x");
        Check.equal(140.0, HitTester.toScreenY(120, 50, 2.0), "document y to screen y");
        Check.equal(30.0, HitTester.toDocumentX(60, 0, 2.0), "screen x to document x");
        Check.equal(120.0, HitTester.toDocumentY(140, 50, 2.0), "screen y to document y");
        Check.equal("t", idOf(p.hitTest(60, 140, 0, 50, 2.0)), "the hit test applies scroll and scale");
        Check.isFalse("t".equals(idOf(p.hitTest(60, 140, 0, 0, 2.0))), "without the scroll the same screen point hits something else");
        Check.isFalse("t".equals(idOf(p.hitTest(60, 140, 0, 50, 1.0))), "and with another scale as well");
    }

    /**
     * Bounding boxes and client rectangles: an inline element that wraps over several lines gives one rectangle per line.
     * <p>
     * 경계 상자와 클라이언트 사각형: 여러 줄에 걸친 인라인 요소는 줄마다 사각형 하나를 준다.
     */
    public void testBoundingBoxAndClientRects() {
        RenderedPage p = RenderTestSupport.page("<div id='d' style='width:40px;font-size:14px'><span id='s'>aaaa bbbb cccc</span></div>"
                + "<div id='e' style='display:none'>x</div>", 200, 200, null);
        Element s = RenderTestSupport.lastDocument.getElementById("s");
        List<Rectangle2D> rects = p.getHitTester().getClientRects(s);
        Check.isTrue(rects.size() >= 2, "the wrapped span has one rectangle per line: " + rects.size());
        Rectangle2D bounds = p.getHitTester().getBoundingBox(s);
        Check.isTrue(bounds.getHeight() > rects.get(0).getHeight() * 1.5, "the bounding box covers all lines");
        Element d = RenderTestSupport.lastDocument.getElementById("d");
        Rectangle2D db = p.getHitTester().getBoundingBox(d);
        Check.equal(0.0, db.getX(), "block x");
        Check.equal(40.0, db.getWidth(), "block width");
        Check.isNull(p.getHitTester().getBoundingBox(RenderTestSupport.lastDocument.getElementById("e")), "display:none has no box");
        Check.isTrue(p.getHitTester().getClientRects(RenderTestSupport.lastDocument.getElementById("e")).isEmpty(), "and no client rectangles");
    }

    /**
     * A point outside every box gives a result without a link or text.
     * <p>
     * 어느 상자에도 속하지 않는 점은 링크도 텍스트도 없는 결과를 준다.
     */
    public void testOutsideTheDocument() {
        RenderedPage p = RenderTestSupport.page("<div style='height:10px'>x</div>", 100, 50, null);
        HitResult h = p.hitTestDocument(5000, 5000);
        Check.isNull(h.getLink(), "no link outside the document");
        Check.isNull(h.getTextNode(), "no text outside the document");
        Check.equal(-1, h.getTextOffset(), "no offset");
        HitResult n = p.hitTestDocument(-10, -10);
        Check.isNull(n.getHref(), "no URL at negative coordinates");
    }
}
