package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.duckdns.hjow.swingwebpanel.css.anim.Interpolator;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the rest of CSS Motion Path 1 (plan item P10-08-h, https://www.w3.org/TR/motion-1/ WD 5 November 2024, sections 2.1 and 3): the reference box of a
 * <code>&lt;coord-box&gt;</code> (content, padding, border and margin box of the containing block), a <code>&lt;coord-box&gt;</code> alone as
 * <code>inset(0 round X)</code> with the <code>border-radius</code> of the containing block (starting after the top left corner), <code>url(#id)</code> to the
 * SVG shape elements of the document (and to something else: <code>path("m 0 0")</code>), and the interpolation of <code>offset-path</code>.
 * <p>
 * CSS Motion Path 1 의 나머지 시험(계획 P10-08-h, 2.1, 3 절): <code>&lt;coord-box&gt;</code> 의 기준 상자(포함 블록의 content, padding, border, margin box), 포함 블록의
 * <code>border-radius</code> 로 <code>inset(0 round X)</code> 가 되는 <code>&lt;coord-box&gt;</code> 만의 경로(왼쪽 위 모서리 뒤에서 시작), 문서의 SVG 도형 요소를 가리키는
 * <code>url(#id)</code>(그 밖이면 <code>path("m 0 0")</code>), <code>offset-path</code> 의 보간.
 */
public class MotionPathMoreLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MotionPathMoreLayoutTest() {
    }

    /**
     * Lays out a containing block at (30, 20) (its margin box), 400 by 300 content with a padding of 10px, a border of 5px and a margin of 20px top and 30px
     * left, and an absolutely positioned 20 by 10 box <code>m</code> with the style (anchor at its top left, no rotation).
     * <p>
     * (30, 20)(margin box)에 있고 content 400x300, padding 10px, border 5px, 위 margin 20px, 왼쪽 margin 30px 인 포함 블록과 스타일을 가진 절대 배치 20x10 상자
     * <code>m</code>(앵커는 왼쪽 위, 회전 없음)을 배치한다.
     *
     * @param cbStyle more style of the containing block / 포함 블록의 추가 스타일
     * @param style   the style of the box / 상자의 스타일
     * @param more    more markup / 추가 마크업
     * @return the layout / 레이아웃
     */
    private static LayoutResult layout(String cbStyle, String style, String more) {
        return LayoutTestSupport.layout(more + "<div id=cb style='position:relative;margin:20px 0 0 30px;width:400px;height:300px;padding:10px;border:5px solid;" + cbStyle
                + "'><div id=m style='position:absolute;left:0;top:0;width:20px;height:10px;offset-anchor:0 0;offset-rotate:0deg;" + style + "'></div></div>", 600);
    }

    /**
     * Returns where the top left corner of the box <code>m</code> goes.
     * <p>
     * 상자 <code>m</code> 의 왼쪽 위 모서리가 가는 곳을 반환한다.
     *
     * @param r the layout / 레이아웃
     * @return the point / 점
     */
    private static Point2D corner(LayoutResult r) {
        LayoutBox m = LayoutTestSupport.box(r, "m");
        AffineTransform t = m.getTransform();
        Check.notNull(t, "the box has a transform");
        return t.transform(new Point2D.Double(m.getX(), m.getY()), null);
    }

    /**
     * Checks a point.
     * <p>
     * 점을 확인한다.
     *
     * @param ex   the expected x / 기대 x
     * @param ey   the expected y / 기대 y
     * @param p    the point / 점
     * @param what the message / 메시지
     */
    private static void point(double ex, double ey, Point2D p, String what) {
        Check.near(ex, p.getX(), 0.6, what + " (x) " + p);
        Check.near(ey, p.getY(), 0.6, what + " (y) " + p);
    }

    /**
     * The <code>&lt;coord-box&gt;</code> chooses the box of the containing block: a box alone is a path around that box that starts at its top left corner, and a
     * <code>ray()</code> is measured in it.
     * <p>
     * <code>&lt;coord-box&gt;</code> 는 포함 블록의 상자를 고른다: 상자만 있으면 그 상자를 도는 경로이며 왼쪽 위 모서리에서 시작하고, <code>ray()</code> 는 그 안에서 잰다.
     */
    public void testCoordBox() {
        // border box 의 왼쪽 위는 (30, 20), padding box 는 (35, 25), content box 는 (45, 35), margin box 는 (0, 0).
        point(30, 20, corner(layout("", "offset-path:border-box", "")), "border-box");
        point(35, 25, corner(layout("", "offset-path:padding-box", "")), "padding-box");
        point(45, 35, corner(layout("", "offset-path:content-box", "")), "content-box");
        point(0, 0, corner(layout("", "offset-path:margin-box", "")), "margin-box");
        point(45, 35, corner(layout("", "offset-path:fill-box", "")), "fill-box is the content box");
        point(30, 20, corner(layout("", "offset-path:view-box", "")), "view-box is the border box");
        // 경로를 따라 윗변의 너비(content box 400)만큼 가면 오른쪽 위 모서리이다.
        point(445, 35, corner(layout("", "offset-path:content-box;offset-distance:400px", "")), "along the top edge of the content box");
        point(445, 135, corner(layout("", "offset-path:content-box;offset-distance:500px", "")), "then down the right edge (clockwise)");
        // ray(): 가운데 (245, 185) 에서 오른쪽으로 가장 가까운 변까지. content box 는 위아래 변이 150, border box 는 165 떨어져 있다.
        point(395, 185, corner(layout("", "offset-path:ray(90deg closest-side) content-box;offset-distance:100%", "")), "a ray in the content box: the closest side is 150 away");
        point(410, 185, corner(layout("", "offset-path:ray(90deg closest-side) border-box;offset-distance:100%", "")), "a ray in the border box: 165 away");
    }

    /**
     * A box alone is <code>inset(0 round X)</code> with the <code>border-radius</code> of the containing block: the path starts at the left end of the top
     * straight edge, after the rounded corner, and the radius shrinks for the inner boxes.
     * <p>
     * 상자만 있으면 포함 블록의 <code>border-radius</code> 로 <code>inset(0 round X)</code> 이다: 경로는 둥근 모서리 뒤 윗변의 직선 왼쪽 끝에서 시작하고, 안쪽 상자는 반지름이 준다.
     */
    public void testRoundedBox() {
        point(70, 20, corner(layout("border-radius:40px", "offset-path:border-box", "")), "after the corner of radius 40");
        point(70, 25, corner(layout("border-radius:40px", "offset-path:padding-box", "")), "the padding box has a radius of 35 (40 - 5)");
        point(70, 35, corner(layout("border-radius:40px", "offset-path:content-box", "")), "the content box has a radius of 25 (40 - 15)");
        point(50, 20, corner(layout("border-radius:20px 0 0 0", "offset-path:border-box", "")), "only the top left radius matters for the start");
        point(30, 20, corner(layout("border-radius:0 40px 0 0", "offset-path:border-box", "")), "a square top left corner starts at the corner");
    }

    /**
     * <code>url(#id)</code> follows the equivalent path of an SVG shape element in the coordinates of the <code>&lt;coord-box&gt;</code>; something that is not a shape
     * is <code>path("m 0 0")</code>, which puts the box at the origin of the box.
     * <p>
     * <code>url(#id)</code> 은 SVG 도형 요소의 동등한 경로를 <code>&lt;coord-box&gt;</code> 좌표로 따른다. 도형이 아닌 것은 <code>path("m 0 0")</code> 이며 상자를 그 상자의
     * 원점에 놓는다.
     */
    public void testUrlReference() {
        String svg = "<svg width=0 height=0 style='position:absolute'><path id=p d='M 10 20 L 110 20'/><circle id=c cx=50 cy=50 r=40 /><rect id=r x=10 y=10 width=100 height=50 rx=10 />"
                + "<polygon id=g points='0,0 100,0 100,100' /><line id=l x1=0 y1=0 x2=0 y2=100 /><ellipse id=e cx=100 cy=50 rx=50 ry=25 /></svg><span id=x></span>";
        point(30 + 60, 20 + 20, corner(layout("", "offset-path:url(#p);offset-distance:50%", svg)), "half of the path, from the border box origin");
        point(30 + 90, 20 + 50, corner(layout("", "offset-path:url(#c)", svg)), "a circle starts at its rightmost point");
        point(30 + 50, 20 + 90, corner(layout("", "offset-path:url(#c);offset-distance:25%", svg)), "and goes clockwise: a quarter is the bottom");
        point(30 + 20, 20 + 10, corner(layout("", "offset-path:url(#r)", svg)), "a rounded rect starts after its top left corner");
        point(30 + 100, 20 + 100, corner(layout("", "offset-path:url(#g);offset-distance:200px", svg)), "a polygon");
        point(30, 20 + 50, corner(layout("", "offset-path:url(#l);offset-distance:50%", svg)), "a line");
        point(30 + 150, 20 + 50, corner(layout("", "offset-path:url(#e)", svg)), "an ellipse");
        point(45 + 60, 35 + 20, corner(layout("", "offset-path:url(#p) content-box;offset-distance:50%", svg)), "the coord-box is the origin of the user space");
        point(30, 20, corner(layout("", "offset-path:url(#x)", svg)), "not a shape: path(\"m 0 0\") at the origin of the box");
        point(30, 20, corner(layout("", "offset-path:url(#missing)", svg)), "no element: the same");
    }

    /**
     * <code>offset-path</code> interpolates two shapes of the same kind, two <code>path()</code> with the same commands and two <code>ray()</code>; other pairs are
     * discrete.
     * <p>
     * <code>offset-path</code> 는 같은 종류의 두 도형, 명령이 같은 두 <code>path()</code>, 두 <code>ray()</code> 를 보간한다. 다른 쌍은 이산이다.
     */
    public void testInterpolation() {
        Check.equal("circle(20px at 10px 20px)", mix("circle(10px at 0px 0px)", "circle(30px at 20px 40px)"), "circles");
        Check.equal("path(\"M 0 0 L 150 50\")", mix("path('M 0 0 L 100 0')", "path('M 0 0 L 200 100')"), "paths with the same commands");
        Check.equal("ray(45deg closest-side)", mix("ray(0deg closest-side)", "ray(90deg closest-side)"), "rays");
        Check.equal("inset(15px round 5px) padding-box", mix("inset(10px round 0px) padding-box", "inset(20px round 10px) padding-box"), "insets with the same box");
        Check.equal("null", mix("path('M 0 0 L 100 0')", "path('M 0 0 H 100')"), "different commands are discrete");
        Check.equal("null", mix("circle(10px)", "inset(10px)"), "different shapes are discrete");
        Check.equal("null", mix("circle(10px) content-box", "circle(20px) border-box"), "different boxes are discrete");
        Check.equal("null", mix("circle(10px)", "circle(50%)"), "a length and a percentage are not mixed");
    }

    /**
     * Interpolates two values of <code>offset-path</code> half way.
     * <p>
     * <code>offset-path</code> 의 두 값을 절반만큼 보간한다.
     *
     * @param a the start / 시작
     * @param b the end / 끝
     * @return the text of the result, or "null" / 결과 텍스트. 없으면 "null"
     */
    private static String mix(String a, String b) {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style=\"offset-path:" + a + "\"></div><div id=b style=\"offset-path:" + b + "\"></div>", 600);
        CssValue x = LayoutTestSupport.box(r, "a").getStyle().get(Prop.OFFSET_PATH);
        CssValue y = LayoutTestSupport.box(r, "b").getStyle().get(Prop.OFFSET_PATH);
        Check.isFalse(x.isKeyword("none") || y.isKeyword("none"), "both values are valid: " + a + " / " + b);
        CssValue v = Interpolator.interpolate(Prop.OFFSET_PATH, x, y, 0.5);
        return v == null ? "null" : v.toCssText();
    }
}
