package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the 2D transforms (plan item P10-06, CSS Transforms Level 1): the matrices of the functions, the transform origin, the order
 * of the function list, painting (pixels), the inverse transform of the input (the hit test finds a moved element where it is painted
 * and not where it was laid out), the bounding rectangles of the DOM, the effect on the scrollable area, the stacking context and the
 * containing block of fixed and absolute descendants, nested transforms, clipping and opacity together with a transform, and the 3D
 * exception (functions that are not exactly 2D make the declaration invalid).
 * <p>
 * 2D 변환 시험(계획 P10-06, CSS Transforms Level 1): 함수의 행렬, 변환 원점, 함수 목록의 순서, 그리기(픽셀), 입력의 역변환(히트 테스트는 이동한 요소를 배치된 곳이 아니라 그려진
 * 곳에서 찾는다), DOM 의 경계 사각형, 스크롤 영역에 미치는 영향, 쌓임 맥락과 fixed/절대 배치 자손의 포함 블록, 중첩된 변환, 변환과 함께 쓴 자르기와 불투명도, 3D 예외(정확히
 * 2D 가 아닌 함수는 선언을 무효로 만든다)를 확인한다.
 */
public class TransformPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TransformPaintTest() {
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
     * Maps a point with the transform of a box.
     * <p>
     * 상자의 변환으로 점을 옮긴다.
     *
     * @param b the box / 상자
     * @param x x / x
     * @param y y / y
     * @return the mapped point / 옮겨진 점
     */
    private static Point2D map(LayoutBox b, double x, double y) {
        return b.getTransform().transform(new Point2D.Double(x, y), null);
    }

    /**
     * <code>translate</code> moves the painted box without changing the layout: the siblings stay, the box is painted and hit at the new
     * place and no longer at the old one; percentages refer to the border box.
     * <p>
     * <code>translate</code> 는 배치를 바꾸지 않고 그려진 상자를 옮긴다: 형제는 그대로이고 상자는 새 자리에서 그려지고 히트되며 옛 자리에서는 아니다. 백분율은 border box 를 참조한다.
     */
    public void testTranslateMovesPaintAndHitButNotLayout() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:40px;height:20px;background:#f00;transform:translate(100px,30px)'></div>"
                + "<div id=b style='width:40px;height:20px;background:#00f'></div>", 300, 200, null);
        LayoutBox a = RenderTestSupport.box(p, "a");
        LayoutBox b = RenderTestSupport.box(p, "b");
        Check.near(0, a.getY(), 0.01, "the layout position of a is not changed");
        Check.near(20, b.getY(), 0.01, "the following sibling is not moved by the transform");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 120, 40), "painted at the translated place");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 10), "nothing is painted at the old place");
        Check.equal(0x0000FF, RenderTestSupport.rgb(img, 20, 30), "the sibling is painted where it was laid out");
        Check.equal("a", idOf(p.hitTestDocument(120, 40)), "the click finds the moved element where it is painted");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(20, 10))), "the old place no longer hits the element");
        Check.equal("b", idOf(p.hitTestDocument(20, 30)), "the sibling is hit at its own place");
        RenderedPage q = RenderTestSupport.page("<div id=a style='width:40px;height:20px;background:#f00;transform:translate(50%,100%)'></div>", 300, 200, null);
        Point2D o = map(RenderTestSupport.box(q, "a"), 0, 0);
        Check.near(20, o.getX(), 0.01, "50% of the width");
        Check.near(20, o.getY(), 0.01, "100% of the height");
    }

    /**
     * <code>rotate</code> turns the box around its center: the center stays, the corners move, a point of the painted diamond that is
     * outside of the laid-out square is painted and hit, and the matrix is the expected rotation.
     * <p>
     * <code>rotate</code> 는 상자를 중심 둘레로 돌린다: 중심은 그대로이고 모서리는 움직이며, 그려진 마름모의 점 중 배치된 정사각형 밖의 점은 그려지고 히트된다. 행렬은 기대한 회전이다.
     */
    public void testRotateAroundTheCenter() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='margin:30px;width:40px;height:40px;background:#f00;transform:rotate(45deg)'></div>", 200, 200, null);
        LayoutBox a = RenderTestSupport.box(p, "a");
        Point2D c = map(a, 50, 50);
        Check.near(50, c.getX(), 0.001, "the center x stays");
        Check.near(50, c.getY(), 0.001, "the center y stays");
        Point2D corner = map(a, 30, 30);
        Check.near(50, corner.getX(), 0.001, "the top left corner went to the top of the diamond (x)");
        Check.near(50 - 20 * Math.sqrt(2), corner.getY(), 0.001, "the top left corner went to the top of the diamond (y)");
        BufferedImage img = RenderTestSupport.paint(p, 200, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 50), "the center is painted");
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 50, 25), "the tip of the diamond is outside of the laid-out square");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 32, 32), "the corner of the laid-out square is empty now");
        Check.equal("a", idOf(p.hitTestDocument(50, 24)), "hit at the tip of the diamond");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(31, 31))), "no hit at the old corner");
    }

    /**
     * The functions of a list are multiplied from left to right (the last one is applied to the points first), and
     * <code>transform-origin</code> changes the center of the rotation, scale and skew.
     * <p>
     * 목록의 함수는 왼쪽에서 오른쪽으로 곱해지고(마지막 함수가 점에 가장 먼저 적용된다) <code>transform-origin</code> 은 회전, 배율, 기울임의 중심을 바꾼다.
     */
    public void testOrderAndOrigin() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:100px;height:50px;transform-origin:0 0;transform:translate(10px,0) scale(2)'></div>"
                + "<div id=b style='width:100px;height:50px;transform-origin:0 0;transform:scale(2) translate(10px,0)'></div>"
                + "<div id=c style='width:100px;height:50px;transform-origin:right bottom;transform:scale(0.5)'></div>"
                + "<div id=d style='width:100px;height:50px;transform-origin:top;transform:skewX(45deg)'></div>"
                + "<div id=e style='width:100px;height:50px;transform-origin:20px 10px;transform:rotate(90deg)'></div>", 300, 400, null);
        Point2D pa = map(RenderTestSupport.box(p, "a"), 1, 0);
        Check.near(12, pa.getX(), 0.001, "translate then scale: the scale is applied to the points first");
        Point2D pb = map(RenderTestSupport.box(p, "b"), 1, 50);
        Check.near(22, pb.getX(), 0.001, "scale then translate: the translation is scaled");
        LayoutBox c = RenderTestSupport.box(p, "c");
        Point2D pc = map(c, 0, 100);
        Check.near(50, pc.getX(), 0.001, "origin right bottom: x of the left edge after scale(.5)");
        Check.near(150 - 25, pc.getY(), 0.001, "origin right bottom: y of the top edge");
        LayoutBox d = RenderTestSupport.box(p, "d");
        Check.near(50, map(d, 50, 150).getX(), 0.001, "the top center is the fixed line of the skew");
        Check.near(50 + 50, map(d, 50, 200).getX(), 0.001, "the bottom center moved by tan(45) * 50");
        LayoutBox e = RenderTestSupport.box(p, "e");
        Point2D pe = map(e, 20, 210);
        Check.near(20, pe.getX(), 0.001, "the origin is fixed (x)");
        Check.near(210, pe.getY(), 0.001, "the origin is fixed (y)");
    }

    /**
     * The bounding rectangles of the DOM are those of the transformed box (the bounds of the rotated rectangle).
     * <p>
     * DOM 의 경계 사각형은 변환된 상자의 것이다(회전한 사각형의 경계 상자).
     */
    public void testClientRectsAreTransformed() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:100px;height:50px;transform:rotate(90deg)'></div>", 300, 200, null);
        Element e = RenderTestSupport.lastDocument.getElementById("a");
        Rectangle2D r = p.getHitTester().getBoundingBox(e);
        Check.near(25, r.getX(), 0.01, "x of the bounds of the rotated box");
        Check.near(-25, r.getY(), 0.01, "y of the bounds of the rotated box");
        Check.near(50, r.getWidth(), 0.01, "width");
        Check.near(100, r.getHeight(), 0.01, "height");
    }

    /**
     * A transform does not change the layout, but its box adds to the scrollable area; a transformed box is a stacking context and the
     * containing block of its absolutely positioned and fixed descendants (a fixed box inside it scrolls with the page).
     * <p>
     * 변환은 배치를 바꾸지 않지만 변환된 상자는 스크롤 영역에 더해진다. 변환된 상자는 쌓임 맥락이며 절대 배치와 fixed 자손의 포함 블록이다(그 안의 fixed 상자는 페이지와 함께
     * 스크롤된다).
     */
    public void testScrollableAreaStackingAndContainingBlocks() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:40px;height:40px;transform:translate(0,500px)'></div>", 300, 200, null);
        Check.isTrue(p.getLayout().getContentHeight() >= 540 - 0.01, "the translated box makes the page taller: " + p.getLayout().getContentHeight());
        RenderedPage q = RenderTestSupport.page("<div id=t style='margin:20px;width:100px;height:100px;transform:translate(10px,10px)'>"
                + "<div id=abs style='position:absolute;left:5px;top:6px;width:10px;height:10px'></div>"
                + "<div id=fix style='position:fixed;left:7px;top:8px;width:10px;height:10px'></div></div>"
                + "<div id=free style='position:fixed;left:7px;top:8px;width:10px;height:10px'></div>", 300, 200, null);
        LayoutBox t = RenderTestSupport.box(q, "t");
        Check.isTrue(t.isStackingContext(), "a transformed box is a stacking context");
        LayoutBox abs = RenderTestSupport.box(q, "abs");
        LayoutBox fix = RenderTestSupport.box(q, "fix");
        Check.near(25, abs.getX(), 0.01, "absolute child: left of the padding box of the transformed box + 5");
        Check.near(26, abs.getY(), 0.01, "absolute child: top + 6");
        Check.near(27, fix.getX(), 0.01, "the fixed child is placed relative to the transformed box");
        Check.near(28, fix.getY(), 0.01, "the fixed child is placed relative to the transformed box (y)");
        Check.isFalse(fix.isFixedPosition(), "a fixed box inside a transformed box is not fixed to the viewport");
        Check.isTrue(RenderTestSupport.box(q, "free").isFixedPosition(), "a fixed box without a transformed ancestor is fixed to the viewport");
    }

    /**
     * Transforms of nested elements are combined (the child is moved with the parent, then by its own transform); an element that
     * clips with <code>overflow:hidden</code> clips a transformed child at the place where the clip is painted; an opacity group works
     * together with a transform.
     * <p>
     * 중첩된 요소의 변환은 합쳐진다(자식은 부모와 함께 움직인 뒤 자신의 변환으로 움직인다). <code>overflow:hidden</code> 으로 자르는 요소는 변환된 자식을 자르기가 그려지는 자리에서
     * 자른다. 불투명도 그룹은 변환과 함께 동작한다.
     */
    public void testNestedClipAndOpacity() {
        RenderedPage p = RenderTestSupport.page("<div id=p style='width:100px;height:100px;transform:translate(100px,0)'>"
                + "<div id=c style='width:20px;height:20px;background:#f00;transform:translate(0,50px)'></div></div>", 300, 200, null);
        Point2D o = RenderTestSupport.box(p, "c").getFullTransform().transform(new Point2D.Double(0, 0), null);
        Check.near(100, o.getX(), 0.01, "child x: moved with the parent");
        Check.near(50, o.getY(), 0.01, "child y: moved by its own transform");
        BufferedImage img = RenderTestSupport.paint(p, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 110, 60), "the child is painted at the combined place");
        Check.equal("c", idOf(p.hitTestDocument(110, 60)), "the child is hit at the combined place");
        RenderedPage q = RenderTestSupport.page("<div style='width:50px;height:50px;overflow:hidden'>"
                + "<div style='width:100px;height:30px;background:#f00;transform:translate(10px,10px)'></div></div>", 300, 200, null);
        BufferedImage imgq = RenderTestSupport.paint(q, 300, 200);
        Check.equal(0xFF0000, RenderTestSupport.rgb(imgq, 30, 20), "inside the clip");
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(imgq, 70, 20), "outside the clip of the parent");
        RenderedPage r = RenderTestSupport.page("<div style='width:40px;height:40px;background:#000;opacity:0.5;transform:translate(60px,0)'></div>", 300, 200, null);
        BufferedImage imgr = RenderTestSupport.paint(r, 300, 200);
        int mid = RenderTestSupport.rgb(imgr, 80, 20);
        // 흰 배경 위의 반투명 검정은 회색이다(세 채널이 모두 0x70~0x90).
        Check.isTrue(((mid >> 16) & 0xFF) > 0x70 && ((mid >> 16) & 0xFF) < 0x90 && ((mid >> 8) & 0xFF) > 0x70 && (mid & 0xFF) < 0x90,
                "half transparent black on white is gray: " + Integer.toHexString(mid));
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(imgr, 20, 20), "nothing at the old place");
    }

    /**
     * A transform that cannot be inverted (a scale of 0) hides the box and it cannot be hit; <code>none</code> and the invalid values
     * leave the box alone; 3D functions that are exactly 2D are projected and the others make the declaration invalid (EXC-03).
     * <p>
     * 역변환할 수 없는 변환(배율 0)은 상자를 숨기고 히트되지 않는다. <code>none</code> 과 무효한 값은 상자를 그대로 둔다. 정확히 2D 인 3D 함수는 투영하고 그 밖의 것은 선언을
     * 무효로 만든다(EXC-03).
     */
    public void testDegenerateInvalidAnd3d() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:40px;height:40px;background:#f00;transform:scale(0)'></div>", 100, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 100, 100);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(img, 20, 20), "scale(0) hides the box");
        Check.isFalse("a".equals(idOf(p.hitTestDocument(20, 20))), "scale(0): no hit");
        RenderedPage n = RenderTestSupport.page("<div id=a style='width:40px;height:40px;transform:none'></div>"
                + "<div id=b style='width:40px;height:40px;transform:bogus(1)'></div>"
                + "<div id=c style='width:40px;height:40px;transform:rotateX(45deg)'></div>"
                + "<div id=d style='width:40px;height:40px;transform:translate3d(10px,0,5px)'></div>"
                + "<div id=e style='width:40px;height:40px;transform:perspective(100px) translate(5px)'></div>"
                + "<div id=f style='width:40px;height:40px;transform:translate3d(10px,20px,0)'></div>"
                + "<div id=g style='width:40px;height:40px;transform:rotateZ(90deg) scale3d(2,3,1) translateZ(0)'></div>"
                + "<div id=h style='width:40px;height:40px;transform:matrix3d(1,0,0,0,0,1,0,0,0,0,1,0,7,8,0,1)'></div>"
                + "<div id=i style='width:40px;height:40px;transform:matrix3d(1,0,0,0,0,1,0,0,0,0,1,0,7,8,9,1)'></div>", 100, 500, null);
        String[] none = { "a", "b", "c", "d", "e", "i" };
        // 변환이 없어야 하는 상자: none, 모르는 함수, 실제 3D 함수, z 이동이 있는 translate3d, perspective, 3D 항목이 있는 matrix3d.
        for (int i = 0; i < none.length; i++) {
            Check.isNull(RenderTestSupport.box(n, none[i]).getTransform(), "no transform on #" + none[i]);
        }
        Point2D f = map(RenderTestSupport.box(n, "f"), 0, 0);
        Check.near(10, f.getX(), 0.001, "translate3d(10px,20px,0) is the 2D translation (x)");
        Check.near(20, f.getY(), 0.001, "translate3d(10px,20px,0) is the 2D translation (y)");
        LayoutBox g = RenderTestSupport.box(n, "g");
        Check.notNull(g.getTransform(), "rotateZ, scale3d with z 1 and translateZ(0) are projected");
        Check.near(Math.PI / 2, Math.atan2(g.getTransform().getShearY(), g.getTransform().getScaleX()), 0.001, "rotation by 90 degrees");
        Point2D h = map(RenderTestSupport.box(n, "h"), 0, 0);
        Check.near(7, h.getX(), 0.001, "matrix3d with identity 3D entries: e");
        Check.near(8, h.getY(), 0.001, "matrix3d with identity 3D entries: f");
    }

    /**
     * The individual properties <code>translate</code>, <code>rotate</code> and <code>scale</code> (CSS Transforms 2): the order is
     * translate, rotate, scale and then <code>transform</code>; a percentage scale; <code>rotate: z</code> is the 2D rotation and another
     * axis is invalid; the box is painted and hit at the transformed place; the box is a stacking context.
     * <p>
     * 개별 속성 <code>translate</code>, <code>rotate</code>, <code>scale</code>(CSS Transforms 2): 순서는 translate, rotate, scale, 그다음 <code>transform</code>. 백분율 배율,
     * <code>rotate: z</code> 는 2D 회전이고 다른 축은 무효이다. 상자는 변환된 자리에서 그려지고 히트되며 쌓임 맥락이다.
     */
    public void testIndividualTransformProperties() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:40px;height:40px;transform-origin:0 0;translate:10px;scale:2;transform:translate(5px)'></div>"
                + "<div id=b style='width:40px;height:40px;scale:150%'></div>"
                + "<div id=c style='width:40px;height:40px;rotate:z 90deg;translate:1px 2px'></div>"
                + "<div id=d style='width:40px;height:40px;rotate:x 45deg'></div>"
                + "<div id=e style='width:40px;height:40px;background:#f00;translate:100px 30px'></div>", 300, 300, null);
        Point2D a = map(RenderTestSupport.box(p, "a"), 0, 0);
        Check.near(20, a.getX(), 0.001, "translate(10) * scale(2) * transform translate(5): 5 * 2 + 10");
        Point2D b = map(RenderTestSupport.box(p, "b"), 20, 60);
        Check.near(20, b.getX(), 0.001, "scale 150% around the center: the center x stays");
        Check.near(60, b.getY(), 0.001, "the center y stays");
        Point2D bc = map(RenderTestSupport.box(p, "b"), 0, 40);
        Check.near(-10, bc.getX(), 0.001, "the left edge moved out by half of the growth: 20 - 1.5 * 20");
        LayoutBox c = RenderTestSupport.box(p, "c");
        Check.near(Math.PI / 2, Math.atan2(c.getTransform().getShearY(), c.getTransform().getScaleX()), 0.001, "rotate: z 90deg is a 2D rotation");
        Check.isNull(RenderTestSupport.box(p, "d").getTransform(), "a rotation around the x axis is invalid (EXC-03)");
        BufferedImage img = RenderTestSupport.paint(p, 300, 300);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 120, 210), "painted at the translated place");
        Check.equal("e", idOf(p.hitTestDocument(120, 210)), "hit at the translated place");
        Check.isTrue(RenderTestSupport.box(p, "e").isStackingContext(), "the property makes a stacking context");
    }

    /**
     * The property <code>perspective</code>, <code>transform-style</code> and <code>backface-visibility</code> have no effect on the
     * painted result (EXC-03).
     * <p>
     * <code>perspective</code>, <code>transform-style</code>, <code>backface-visibility</code> 속성은 그려진 결과에 영향이 없다(EXC-03).
     */
    public void testThreeDPropertiesAreIgnored() {
        RenderedPage p = RenderTestSupport.page("<div id=a style='width:40px;height:40px;background:#f00;perspective:50px;transform-style:preserve-3d;"
                + "backface-visibility:hidden'></div>", 100, 100, null);
        BufferedImage img = RenderTestSupport.paint(p, 100, 100);
        Check.equal(0xFF0000, RenderTestSupport.rgb(img, 20, 20), "painted as usual");
        Check.isNull(RenderTestSupport.box(p, "a").getTransform(), "no transform");
        List<LayoutBox> l = p.getLayout().getBoxes(RenderTestSupport.lastDocument.getElementById("a"));
        Check.equal(1, l.size(), "one box");
    }
}
