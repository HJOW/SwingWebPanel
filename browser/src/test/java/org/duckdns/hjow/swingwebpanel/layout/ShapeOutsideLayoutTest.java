package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>shape-outside</code>, <code>shape-margin</code> and the wrapping of text around a float shape (plan item P10-08, CSS
 * Shapes Module Level 1, https://www.w3.org/TR/css-shapes-1/): the lines beside a float with a circle, an ellipse, a polygon, an inset or a
 * reference box follow the shape (and not the margin box), the shape is grown by <code>shape-margin</code>, a right float shortens the
 * lines on its right, a value that is not usable wraps around the margin box, and boxes that do not take part in the lines keep using
 * the margin box.
 * <p>
 * <code>shape-outside</code>, <code>shape-margin</code> 과 float 모양을 따라 텍스트를 감싸는 시험(계획 P10-08, CSS Shapes Module Level 1,
 * https://www.w3.org/TR/css-shapes-1/): 원, 타원, 다각형, inset, 기준 상자를 가진 float 옆의 줄은 margin box 가 아니라 모양을 따르고, 모양은
 * <code>shape-margin</code> 만큼 커지며, 오른쪽 float 는 그 오른쪽의 줄을 짧게 하고, 쓸 수 없는 값은 margin box 를 감싸며, 줄에 참여하지 않는 상자는 계속 margin box 를 쓴다.
 */
public class ShapeOutsideLayoutTest {

    /** Tolerance for coordinates (curves are approximated). 좌표 허용 오차(곡선은 근사한다). */
    private static final double T = 1.0;

    /** A line height of 10px. 줄 높이 10px. */
    private static final String LH = "<style>body{line-height:10px}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ShapeOutsideLayoutTest() {
    }

    /**
     * Returns the lines of a paragraph of many short words beside a float.
     * <p>
     * float 옆에 있는, 짧은 단어가 많은 문단의 줄을 반환한다.
     *
     * @param floatStyle the style of the 100 by 100 float / 100x100 float 의 스타일
     * @param side       left or right / left 나 right
     * @return the lines / 줄
     */
    private static List<LayoutBox> lines(String floatStyle, String side) {
        StringBuilder words = new StringBuilder();
        // 줄마다 여러 단어가 들어가도록 짧은 단어를 많이 만든다.
        for (int i = 0; i < 400; i++) {
            words.append("ab ");
        }
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:" + side + ";width:100px;height:100px;" + floatStyle + "'></div><p id=p style='margin:0'>" + words + "</p>", 400);
        return LayoutTestSupport.lines(LayoutTestSupport.box(r, "p"));
    }

    /**
     * Returns the first line whose top is at or below a height.
     * <p>
     * 위쪽이 주어진 높이 이상인 첫 줄을 반환한다.
     *
     * @param lines the lines / 줄
     * @param y     the height / 높이
     * @return the line / 줄
     */
    private static LayoutBox lineAt(List<LayoutBox> lines, double y) {
        // 위쪽이 y 이상인 첫 줄을 찾는다.
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getY() >= y - 0.001) {
                return lines.get(i);
            }
        }
        Check.fail("no line at " + y);
        return null;
    }

    /**
     * Without a shape the lines are 100px from the left next to a 100 by 100 float; with a circle they follow the circle: a line that
     * is at the top is shortened less than a line at the middle, and below the float the lines are not shortened.
     * <p>
     * 모양이 없으면 100x100 float 옆의 줄은 왼쪽에서 100px 떨어지지만, 원이 있으면 원을 따른다: 위쪽의 줄은 가운데 줄보다 덜 짧아지고 float 아래에서는 줄이 짧아지지 않는다.
     */
    public void testCircle() {
        List<LayoutBox> plain = lines("", "left");
        Check.near(100, plain.get(0).getX(), T, "without a shape the first line starts at the margin box edge");
        List<LayoutBox> c = lines("shape-outside:circle(50%)", "left");
        // 첫 줄은 [0, 10] 띠: 원은 y=10 에서 x = 50 + sqrt(50*50 - 40*40) = 80 까지 온다.
        Check.near(80, c.get(0).getX(), T, "the first line starts at the right edge of the circle in its band (80)");
        // 가운데 줄은 [40, 50] 띠: 원은 y=50 에서 x = 100 까지 온다.
        Check.near(100, lineAt(c, 40).getX(), T, "the line at the middle starts at 100");
        // 아래쪽 [90, 100] 띠: y=90 에서 x = 80.
        Check.near(80, lineAt(c, 90).getX(), T, "the last line beside the float starts at 80 again");
        Check.near(0, lineAt(c, 100).getX(), T, "below the float the lines start at the left edge");
        Check.isTrue(c.get(0).getWidth() > plain.get(0).getWidth(), "the line is wider than the line next to the margin box");
    }

    /**
     * An ellipse, a polygon, an inset and a reference box.
     * <p>
     * 타원, 다각형, inset, 기준 상자.
     */
    public void testOtherShapes() {
        List<LayoutBox> e = lines("shape-outside:ellipse(30px 50px at 30px 50px)", "left");
        // 타원의 가장 오른쪽은 x = 60: 가운데 띠에서 60, 위쪽 띠에서는 더 작다.
        Check.near(60, lineAt(e, 40).getX(), T, "the middle of an ellipse of 30 by 50 at (30, 50) reaches 60");
        Check.near(48, e.get(0).getX(), T, "the first band [0, 10] reaches 30 + 30 * sqrt(1 - (40/50)^2) = 48");
        List<LayoutBox> p = lines("shape-outside:polygon(0 0, 100px 0, 0 100px)", "left");
        // 직각삼각형: 위에서는 x=100 까지, 아래로 갈수록 좁아진다. 띠 [y, y+10] 의 가장 오른쪽은 y 에서의 x = 100 - y.
        Check.near(100, p.get(0).getX(), T, "a triangle is as wide as the box at the top");
        Check.near(60, lineAt(p, 40).getX(), T, "at y=40 the triangle reaches 60");
        Check.near(10, lineAt(p, 90).getX(), T, "at y=90 the triangle reaches 10");
        List<LayoutBox> i = lines("shape-outside:inset(0 50px 0 0)", "left");
        Check.near(50, i.get(0).getX(), T, "an inset of 50px on the right leaves the lines 50px from the left");
        List<LayoutBox> rb = lines("padding:20px;width:60px;height:60px;shape-outside:content-box", "left");
        Check.near(0, rb.get(0).getX(), T, "content-box: above the content box (the padding) the lines are not shortened");
        Check.near(80, lineAt(rb, 30).getX(), T, "content-box: beside it the lines start at its right edge (20 + 60)");
        List<LayoutBox> mb = lines("shape-outside:margin-box;margin:10px", "left");
        Check.near(120, mb.get(0).getX(), T, "margin-box is the default box of the layout");
    }

    /**
     * <code>shape-margin</code> grows the shape outwards.
     * <p>
     * <code>shape-margin</code> 은 모양을 바깥으로 넓힌다.
     */
    public void testShapeMargin() {
        List<LayoutBox> c = lines("shape-outside:circle(50%);shape-margin:10px", "left");
        Check.near(100, lineAt(c, 50).getX(), T, "the margin box clips the grown shape at the middle (110 is cut to 100)");
        // 위쪽 띠 [0, 10]: 반지름 60 의 원(중심 (50, 50))이 y=10 에서 x = 50 + sqrt(60*60 - 40*40) = 50 + 44.7 = 94.7 까지 온다.
        Check.near(94.7, c.get(0).getX(), T, "the first line is pushed further by the shape margin (94.7)");
        List<LayoutBox> pct = lines("shape-outside:circle(50%);shape-margin:10%", "left");
        Check.near(100, pct.get(0).getX(), T, "10% of the 400px containing block is 40px: the grown circle is cut off at the margin box (100)");
    }

    /**
     * A right float shortens the lines on its right.
     * <p>
     * 오른쪽 float 는 오른쪽의 줄을 짧게 한다.
     */
    public void testRightFloat() {
        List<LayoutBox> r = lines("shape-outside:circle(50%)", "right");
        // 오른쪽 float 의 원은 x = 300 + 50 - 50 = ... 컨테이너 400 의 오른쪽에서 100 너비이므로 float 는 300..400, 원의 가장 왼쪽은 y=10 에서 400 - 80 = 320.
        Check.near(0, r.get(0).getX(), T, "the lines start at the left edge");
        Check.near(320, r.get(0).getWidth(), T, "the first line ends where the circle begins in its band (320)");
        Check.near(300, lineAt(r, 40).getWidth(), T, "the middle line ends at 300");
        Check.near(400, lineAt(r, 100).getWidth(), T, "below the float the line is as wide as the container");
    }

    /**
     * A value that cannot be used (an image, a shape that is wrong, <code>none</code>) wraps around the margin box; the box of the float
     * does not change.
     * <p>
     * 쓸 수 없는 값(이미지, 틀린 도형, <code>none</code>)은 margin box 를 감싼다. float 상자는 바뀌지 않는다.
     */
    public void testUnusableValues() {
        Check.near(100, lines("shape-outside:none", "left").get(0).getX(), T, "none");
        Check.near(100, lines("shape-outside:url(a.png)", "left").get(0).getX(), T, "an image shape is not supported");
        Check.near(100, lines("shape-outside:linear-gradient(red, blue)", "left").get(0).getX(), T, "a gradient shape is not supported");
        Check.near(100, lines("shape-outside:circle(bogus)", "left").get(0).getX(), T, "a wrong circle() wraps around the margin box");
        LayoutResult r = LayoutTestSupport.layout(LH + "<div id=f style='float:left;width:100px;height:100px;shape-outside:circle(50%)'></div><p>x</p>", 400);
        Check.near(100, LayoutTestSupport.box(r, "f").getWidth(), T, "the float keeps its size");
        Check.equal("circle(50%)", LayoutTestSupport.box(r, "f").getStyle().get(Prop.SHAPE_OUTSIDE).toCssText(), "the value is kept");
    }

    /**
     * A block that establishes a block formatting context beside a float with a shape uses the margin box, not the shape.
     * <p>
     * 모양이 있는 float 옆에서 블록 서식 문맥을 만드는 블록은 모양이 아니라 margin box 를 쓴다.
     */
    public void testBfcBoxUsesMarginBox() {
        LayoutResult r = LayoutTestSupport.layout(LH + "<div style='float:left;width:100px;height:100px;shape-outside:circle(50%)'></div>"
                + "<div id=b style='overflow:hidden;height:20px'></div>", 400);
        Check.near(100, LayoutTestSupport.box(r, "b").getX(), T, "a BFC box starts at the margin box edge");
    }
}
