package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of CSS Motion Path Level 1 (plan item P10-08, https://www.w3.org/TR/motion-1/, W3C Working Draft 5 November 2024): the offset transform of a box that has an
 * <code>offset-path</code> (a <code>path()</code>, the basic shapes, a <code>ray()</code>), <code>offset-distance</code> with its used value (modulo for a closed
 * path, clamped for an open one, free for a ray), <code>offset-position</code>, <code>offset-anchor</code>, <code>offset-rotate</code>, the order against the
 * <code>transform</code> properties, the <code>offset</code> shorthand, and what a transformed box is (stacking context, containing block).
 * <p>
 * CSS Motion Path Level 1 시험(계획 P10-08): <code>offset-path</code>(<code>path()</code>, 기본 도형, <code>ray()</code>)가 있는 상자의 오프셋 변환, 실제 값을 가진
 * <code>offset-distance</code>(닫힌 경로는 나머지, 열린 경로는 한정, ray 는 자유), <code>offset-position</code>, <code>offset-anchor</code>, <code>offset-rotate</code>,
 * <code>transform</code> 속성들과의 순서, <code>offset</code> 단축 속성, 변환된 상자의 성질(쌓임 맥락, 포함 블록).
 */
public class MotionPathLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MotionPathLayoutTest() {
    }

    /**
     * Lays out a containing block that starts at (30, 20), is 400 by 300 and holds an absolutely positioned 20 by 10 box <code>m</code> at its top left corner with the
     * style.
     * <p>
     * (30, 20) 에서 시작하는 400x300 포함 블록과 그 왼쪽 위에 있는 절대 배치된 20x10 상자 <code>m</code>(스타일 적용)을 배치한다.
     *
     * @param style the style of the box <code>m</code> / 상자 <code>m</code> 의 스타일
     * @return the layout / 레이아웃
     */
    private static LayoutResult layout(String style) {
        return LayoutTestSupport.layout("<div id=cb style='position:relative;margin:20px 0 0 30px;width:400px;height:300px'>"
                + "<div id=m style='position:absolute;left:0;top:0;width:20px;height:10px;" + style + "'></div></div>", 600);
    }

    /**
     * Maps a point of the document through the transform of the box <code>m</code>.
     * <p>
     * 문서의 점을 상자 <code>m</code> 의 변환으로 옮긴다.
     *
     * @param r the layout / 레이아웃
     * @param x x / x
     * @param y y / y
     * @return the moved point / 옮겨진 점
     */
    private static Point2D move(LayoutResult r, double x, double y) {
        AffineTransform t = LayoutTestSupport.box(r, "m").getTransform();
        Check.notNull(t, "the box has a transform");
        return t.transform(new Point2D.Double(x, y), null);
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
        Check.near(ex, p.getX(), 0.05, what + " (x)");
        Check.near(ey, p.getY(), 0.05, what + " (y)");
    }

    /**
     * A <code>path()</code> is in the coordinates of the containing block: the anchor point of the box goes to the point at the distance; a percentage is a fraction of
     * the length of the path; <code>offset-rotate: auto</code> turns the box along the direction of the path.
     * <p>
     * <code>path()</code> 는 포함 블록의 좌표이다: 상자의 앵커 점이 그 거리의 점으로 간다. 백분율은 경로 길이의 비율이고 <code>offset-rotate: auto</code> 는 상자를 경로의 방향으로 돌린다.
     */
    public void testPathPlacementAndRotation() {
        String base = "offset-anchor:left top;offset-path:path(\"M 0 0 L 100 0\");";
        LayoutResult r = layout(base + "offset-distance:50%");
        point(80, 20, move(r, 30, 20), "the top left corner is 50 px along the line, from the corner of the containing block (30, 20)");
        point(100, 20, move(r, 50, 20), "the box keeps its shape: the right edge is 20 px further");
        LayoutResult len = layout(base + "offset-distance:30px");
        point(60, 20, move(len, 30, 20), "a length");
        LayoutResult v = layout("offset-anchor:left top;offset-path:path(\"M 0 0 L 0 100\");offset-distance:40px");
        point(30, 60, move(v, 30, 20), "a vertical line moves the corner down");
        point(30, 80, move(v, 50, 20), "and, with auto rotation, turns the box a quarter turn clockwise (the point 20 px to the right is 20 px lower)");
        point(20, 60, move(v, 30, 30), "and the point 10 px below goes to the left");
        LayoutResult fixed = layout(base + "offset-distance:50%;offset-rotate:90deg");
        point(80, 40, move(fixed, 50, 20), "a fixed angle turns the box about the placed anchor");
        LayoutResult rev = layout(base + "offset-distance:50%;offset-rotate:reverse");
        point(60, 20, move(rev, 50, 20), "reverse turns it by 180 degrees: the right edge is to the left of the anchor");
        LayoutResult sum = layout("offset-anchor:left top;offset-path:path(\"M 0 0 L 0 100\");offset-distance:0;offset-rotate:auto 90deg");
        point(30, 20, move(sum, 30, 20), "auto plus 90 degrees on a vertical line is a half turn: the anchor stays");
        point(10, 20, move(sum, 50, 20), "and the right edge points to the left");
    }

    /**
     * The anchor: <code>auto</code> is the <code>transform-origin</code> (the center by default), a position puts another point of the box on the path.
     * <p>
     * 앵커: <code>auto</code> 는 <code>transform-origin</code>(기본은 가운데)이고 위치는 상자의 다른 점을 경로에 놓는다.
     */
    public void testAnchor() {
        String base = "offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px;offset-rotate:0deg;";
        LayoutResult c = layout(base);
        point(80, 20, move(c, 40, 25), "auto is the center of the box (10, 5): the center goes to 50 px on the line");
        LayoutResult tl = layout(base + "offset-anchor:left top");
        point(80, 20, move(tl, 30, 20), "left top");
        LayoutResult br = layout(base + "offset-anchor:right bottom");
        point(80, 20, move(br, 50, 30), "right bottom");
        LayoutResult origin = layout(base + "transform-origin:0 0");
        point(80, 20, move(origin, 30, 20), "auto follows transform-origin");
        LayoutResult pct = layout(base + "offset-anchor:25% 50%");
        point(80, 20, move(pct, 35, 25), "percentages of the box");
    }

    /**
     * The used distance: modulo the length for a closed path, clamped for an open one.
     * <p>
     * 실제 거리: 닫힌 경로는 길이로 나눈 나머지, 열린 경로는 한정한 값.
     */
    public void testUsedDistance() {
        String open = "offset-anchor:left top;offset-rotate:0deg;offset-path:path(\"M 0 0 H 200 V 150\");";
        point(30, 20, move(layout(open + "offset-distance:-50px"), 30, 20), "an open path clamps below zero");
        point(230, 170, move(layout(open + "offset-distance:900px"), 30, 20), "and above the length");
        point(130, 20, move(layout(open + "offset-distance:100px"), 30, 20), "inside the path");
        point(230, 70, move(layout(open + "offset-distance:250px"), 30, 20), "around the corner");
        String closed = "offset-anchor:left top;offset-rotate:0deg;offset-path:path(\"M 0 0 H 200 V 150 Z\");";
        point(30, 20, move(layout(closed + "offset-distance:0px"), 30, 20), "the start of a closed path");
        point(130, 20, move(layout(closed + "offset-distance:100px"), 30, 20), "one side");
        point(230, 70, move(layout(closed + "offset-distance:250px"), 30, 20), "the next side");
        // 닫힌 경로의 전체 길이는 200 + 150 + 250 = 600 이므로 650 은 50 과 같다.
        point(80, 20, move(layout(closed + "offset-distance:650px"), 30, 20), "a closed path goes round: 650 px is 50 px");
        point(230, 70, move(layout(closed + "offset-distance:-350px"), 30, 20), "and a negative distance goes round the other way: -350 px is 250 px");
        point(80, 20, move(layout(closed + "offset-distance:108.333%"), 30, 20), "a percentage above 100 goes round too");
    }

    /**
     * The basic shapes are the equivalent paths of section 3: a circle starts at its rightmost point and goes clockwise, a rectangle starts at the left end of the top
     * edge, a polygon follows its points; the percentages are those of the containing block.
     * <p>
     * 기본 도형은 3 절의 동등한 경로이다: 원은 가장 오른쪽 점에서 시계 방향으로, 직사각형은 윗변의 왼쪽 끝에서, 다각형은 점을 따른다. 백분율은 포함 블록의 것이다.
     */
    public void testBasicShapes() {
        String base = "offset-anchor:left top;offset-rotate:0deg;";
        LayoutResult circle = layout(base + "offset-path:circle(50px at 100px 100px);offset-distance:0%");
        point(180, 120, move(circle, 30, 20), "a circle starts at its rightmost point (150, 100) of the containing block");
        LayoutResult quarter = layout(base + "offset-path:circle(50px at 100px 100px);offset-distance:25%");
        point(130, 170, move(quarter, 30, 20), "a quarter of the way clockwise is the bottom point (100, 150)");
        LayoutResult half = layout(base + "offset-path:circle(50px at 100px 100px);offset-distance:50%");
        point(80, 120, move(half, 30, 20), "half way is the leftmost point (50, 100)");
        LayoutResult ellipse = layout(base + "offset-path:ellipse(80px 40px at 200px 150px);offset-distance:0%");
        point(310, 170, move(ellipse, 30, 20), "an ellipse starts at its rightmost point (280, 150)");
        LayoutResult inset = layout(base + "offset-path:inset(10px 20px 30px 40px);offset-distance:0px");
        point(70, 30, move(inset, 30, 20), "an inset rectangle starts at the left end of its top edge (40, 10)");
        LayoutResult right = layout(base + "offset-path:inset(10px 20px 30px 40px);offset-distance:100px");
        point(170, 30, move(right, 30, 20), "and goes to the right, clockwise");
        LayoutResult poly = layout(base + "offset-path:polygon(0 0, 100px 0, 100px 100px);offset-distance:150px");
        point(130, 70, move(poly, 30, 20), "a polygon follows its points: 150 px is 50 px down the second edge");
        LayoutResult around = layout(base + "offset-path:circle(50px at 100px 100px);offset-distance:100%");
        point(180, 120, move(around, 30, 20), "a circle is closed: 100% is the start again");
        LayoutResult rotated = layout("offset-anchor:left top;offset-path:circle(50px at 100px 100px);offset-distance:25%");
        point(130, 170, move(rotated, 30, 20), "the anchor is at the bottom point");
        Point2D turned = move(rotated, 30, 30);
        Check.near(130, turned.getX(), 1.0, "and the box is turned along the direction there, to the left (a half turn): the point below the anchor goes above it (x)");
        Check.near(160, turned.getY(), 1.0, "and (y)");
    }

    /**
     * <code>ray()</code>: the starting point, the bearing angle (0 degrees is up, clockwise), the length of the size keyword, and the distance that is not limited
     * without <code>contain</code>.
     * <p>
     * <code>ray()</code>: 시작점, 방위각(0도는 위, 시계 방향), 크기 키워드의 길이, <code>contain</code> 이 없으면 제한이 없는 거리.
     */
    public void testRay() {
        String base = "offset-anchor:left top;offset-rotate:0deg;";
        LayoutResult r = layout(base + "offset-path:ray(90deg farthest-side at 10px 10px);offset-distance:50%");
        point(235, 30, move(r, 30, 20), "the farthest side is 390 px away, 50% is 195 px to the right of (10, 10)");
        LayoutResult up = layout(base + "offset-path:ray(0deg closest-side at 100px 100px);offset-distance:30px");
        point(130, 90, move(up, 30, 20), "0 degrees points up");
        LayoutResult free = layout(base + "offset-path:ray(180deg closest-side at 100px 100px);offset-distance:500px");
        point(130, 620, move(free, 30, 20), "a ray has no end without contain: 500 px down");
        LayoutResult sides = layout(base + "offset-path:ray(90deg sides at 100px 100px);offset-distance:100%");
        point(430, 120, move(sides, 30, 20), "sides: the ray ends where it leaves the box (300 px to the right edge)");
        LayoutResult corner = layout(base + "offset-path:ray(90deg closest-corner at 100px 100px);offset-distance:100%");
        point(30 + 100 + Math.hypot(100, 100), 120, move(corner, 30, 20), "closest-corner: the distance to the nearest corner");
        LayoutResult center = layout(base + "offset-path:ray(90deg closest-side);offset-distance:0");
        point(30 + 200, 20 + 150, move(center, 30, 20), "without a position the ray starts at the center of the containing block");
        LayoutResult auto = layout(base + "offset-position:100px 100px;offset-path:ray(90deg closest-side);offset-distance:10px");
        point(30 + 110, 20 + 100, move(auto, 30, 20), "offset-position is the starting point of the ray");
        LayoutResult rot = layout("offset-anchor:left top;offset-path:ray(90deg closest-side at 100px 100px);offset-distance:0");
        point(130, 120, move(rot, 30, 20), "auto rotation: the ray to the right keeps the box upright");
        point(110, 120, move(layout("offset-anchor:left top;offset-path:ray(180deg closest-side at 100px 100px);offset-distance:0"), 30, 40),
                "a ray pointing down turns the box a quarter turn clockwise: the point 20 px below the anchor goes 20 px to its left");
    }

    /**
     * <code>offset-position</code>: <code>auto</code> is the top left corner of the box itself (a <code>path()</code> then starts there), a position is a point of
     * the containing block, and <code>normal</code> is none.
     * <p>
     * <code>offset-position</code>: <code>auto</code> 는 상자 자신의 왼쪽 위 모서리(그러면 <code>path()</code> 가 거기서 시작한다), 위치는 포함 블록의 한 점, <code>normal</code> 은 없음.
     */
    public void testOffsetPosition() {
        LayoutResult own = LayoutTestSupport.layout("<div id=cb style='position:relative;margin:20px 0 0 30px;width:400px;height:300px'>"
                + "<div id=m style='position:absolute;left:60px;top:70px;width:20px;height:10px;offset-anchor:left top;offset-rotate:0deg;offset-position:auto;"
                + "offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px'></div></div>", 600);
        point(30 + 60 + 50, 20 + 70, move(own, 90, 90), "auto: the path starts at the position of the box itself");
        LayoutResult pos = layout("offset-anchor:left top;offset-rotate:0deg;offset-position:40px 50px;offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px");
        point(30 + 40 + 50, 20 + 50, move(pos, 30, 20), "a position: the path starts there");
        LayoutResult normal = layout("offset-anchor:left top;offset-rotate:0deg;offset-position:normal;offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px");
        point(30 + 50, 20, move(normal, 30, 20), "normal: the path starts at the corner of the containing block");
        LayoutResult circle = layout("offset-anchor:left top;offset-rotate:0deg;offset-position:200px 150px;offset-path:circle(50px);offset-distance:0px");
        point(30 + 250, 20 + 150, move(circle, 30, 20), "a circle without at is centered on the offset position");
    }

    /**
     * The offset transform is a transform of the box: <code>transform</code> acts inside of it, the individual properties outside, a box that has an offset path is a
     * stacking context and the containing block of its fixed and absolute descendants, and a box without a usable offset path has no transform.
     * <p>
     * 오프셋 변환은 상자의 변환이다: <code>transform</code> 은 그 안쪽에서, 개별 속성은 바깥쪽에서 작용하며, 오프셋 경로가 있는 상자는 쌓임 맥락이고 fixed/absolute 자손의
     * 포함 블록이다. 쓸 수 있는 오프셋 경로가 없는 상자는 변환이 없다.
     */
    public void testComposition() {
        String base = "offset-anchor:left top;offset-rotate:0deg;offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px;";
        LayoutResult scaled = layout(base + "transform-origin:0 0;transform:scale(2)");
        point(80, 20, move(scaled, 30, 20), "transform acts first: the anchor is the origin of the scale, so it is on the path");
        point(120, 40, move(scaled, 50, 30), "and the box is twice as large around it");
        LayoutResult moved = layout(base + "transform-origin:0 0;translate:10px 5px");
        point(90, 25, move(moved, 30, 20), "the individual translate acts after the offset");
        Check.isNull(LayoutTestSupport.box(layout("offset-path:none"), "m").getTransform(), "none: no transform");
        Check.notNull(LayoutTestSupport.box(layout("offset-path:url(#p)"), "m").getTransform(), "an url() that is not a shape element is path(\"m 0 0\"): it has a transform (Motion Path 1 2.1, P10-08-h)");
        Check.isNull(LayoutTestSupport.box(layout("offset-distance:50%"), "m").getTransform(), "a distance without a path does nothing");
        LayoutResult sc = LayoutTestSupport.layout("<div id=cb style='position:relative;margin:20px 0 0 30px;width:400px;height:300px'>"
                + "<div id=m style='position:absolute;left:0;top:0;width:100px;height:100px;offset-path:path(\"M 0 0 L 100 0\");offset-distance:50px'>"
                + "<div id=f style='position:fixed;left:7px;top:9px;width:5px;height:5px'></div></div></div>", 600);
        Check.isTrue(LayoutTestSupport.box(sc, "m").isStackingContext(), "a box with an offset path is a stacking context");
        Check.near(30 + 7, LayoutTestSupport.box(sc, "f").getX(), 0.01, "and the containing block of fixed boxes (x)");
        Check.near(20 + 9, LayoutTestSupport.box(sc, "f").getY(), 0.01, "and (y)");
        Check.isNull(LayoutTestSupport.box(layout("offset-path:inset(10px)"), "cb").getTransform(), "the containing block itself is not moved");
    }

    /**
     * The properties and the shorthand: the grammar of the longhands and the forms of <code>offset</code>.
     * <p>
     * 속성과 단축 속성: 개별 속성의 문법과 <code>offset</code> 의 형태.
     */
    public void testProperties() {
        PropertyRegistry reg = PropertyRegistry.getInstance();
        Check.equal("auto", parse(reg, "offset-rotate", "auto", "offset-rotate"), "offset-rotate auto");
        Check.equal("reverse 30deg", parse(reg, "offset-rotate", "reverse 30deg", "offset-rotate"), "reverse and an angle");
        Check.equal("INVALID", parse(reg, "offset-rotate", "auto reverse", "offset-rotate"), "two keywords");
        Check.equal("50%", parse(reg, "offset-distance", "50%", "offset-distance"), "a percentage");
        Check.equal("INVALID", parse(reg, "offset-distance", "auto", "offset-distance"), "auto is not a distance");
        Check.equal("auto", parse(reg, "offset-position", "auto", "offset-position"), "offset-position auto");
        Check.equal("INVALID", parse(reg, "offset-anchor", "normal", "offset-anchor"), "normal is not an anchor");
        Check.equal("path(\"M 0 0 L 10 10\")", parse(reg, "offset", "path(\"M 0 0 L 10 10\")", "offset-path"), "the shorthand with a path");
        Check.equal("initial", parse(reg, "offset", "path(\"M 0 0 L 10 10\")", "offset-distance"), "resets the distance");
        Check.equal("25%", parse(reg, "offset", "path(\"M 0 0 L 10 10\") 25% auto 30deg", "offset-distance"), "path, distance, rotate");
        Check.equal("auto 30deg", parse(reg, "offset", "path(\"M 0 0 L 10 10\") 25% auto 30deg", "offset-rotate"), "the rotation after the distance");
        Check.equal("30deg", parse(reg, "offset", "path(\"M 0 0 L 10 10\") 30deg 25%", "offset-rotate"), "distance and rotation in any order");
        Check.equal("10px 20px", parse(reg, "offset", "10px 20px path(\"M 0 0 L 10 10\")", "offset-position"), "the position comes first");
        Check.equal("left top", parse(reg, "offset", "path(\"M 0 0 L 10 10\") / left top", "offset-anchor"), "the anchor after a slash");
        Check.equal("none", parse(reg, "offset", "none", "offset-path"), "none");
        Check.equal("25%", parse(reg, "offset", "25%", "offset-position"), "a lone percentage is a position");
        Check.equal("INVALID", parse(reg, "offset", "foo bar", "offset-path"), "words that are no path are invalid");
        Check.equal("INVALID", parse(reg, "offset", "", "offset-path"), "empty");
    }

    /**
     * Parses a declaration and returns the text of one longhand or the name of the result code.
     * <p>
     * 선언을 파싱해 개별 속성 하나의 문자열 또는 결과 코드 이름을 반환한다.
     *
     * @param reg      the registry / 등록부
     * @param name     the property / 속성
     * @param value    the value / 값
     * @param longhand the longhand to read / 읽을 개별 속성
     * @return the text, or UNKNOWN, UNSUPPORTED, INVALID, MISSING / 문자열 또는 결과 코드 이름
     */
    private static String parse(PropertyRegistry reg, String name, String value, String longhand) {
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int r = reg.parse(name, value, out);
        if (r == PropertyRegistry.UNKNOWN) {
            return "UNKNOWN";
        }
        if (r == PropertyRegistry.UNSUPPORTED) {
            return "UNSUPPORTED";
        }
        if (r == PropertyRegistry.INVALID) {
            return "INVALID";
        }
        CssValue x = out.get(Integer.valueOf(reg.byName(longhand).getId()));
        return x == null ? "MISSING" : x.toCssText();
    }
}
