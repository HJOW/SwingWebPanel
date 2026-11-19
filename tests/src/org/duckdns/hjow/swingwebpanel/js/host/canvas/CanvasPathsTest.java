package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of paths, strokes and clipping of the 2D context (plan item P20-05): subpaths and their rules, fill rules, arcs, ellipses,
 * <code>arcTo</code> and curves (checked with <code>isPointInPath</code>, <code>isPointInStroke</code> and the pixels they fill), line width, caps,
 * joins, dashes, strokes under non-uniform scaling, and rectangular, antialiased, intersected and empty clips.
 * <p>
 * 2D 컨텍스트의 경로, 선, 클리핑 시험(계획 P20-05): 하위 경로와 그 규칙, 채움 규칙, 호, 타원, <code>arcTo</code>, 곡선(<code>isPointInPath</code>,
 * <code>isPointInStroke</code> 와 그것이 채운 화소로 확인), 선 너비, 끝 모양, 꺾임, 점선, 균일하지 않은 확대 아래의 선, 사각형/안티앨리어싱/교차/빈 클립.
 */
public class CanvasPathsTest {

    /**
     * The helper script: <code>px</code> and <code>alpha</code> read a pixel.
     * <p>
     * 도우미 스크립트: <code>px</code> 와 <code>alpha</code> 는 화소를 읽는다.
     */
    private static final String HELPERS = CanvasBasicsTest.HELPERS + "function alpha(x, y) { return ctx.getImageData(x, y, 1, 1).data[3]; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasPathsTest() {
    }

    /**
     * Loads a page with a canvas of a size.
     * <p>
     * 크기가 정해진 캔버스가 있는 페이지를 로드한다.
     *
     * @param w the width / 너비
     * @param h the height / 높이
     * @return the page / 페이지
     */
    private static PageFixture page(int w, int h) {
        PageFixture f = new PageFixture().load("<canvas id=c width=" + w + " height=" + h + "></canvas>");
        f.scripts.evaluate(HELPERS, "helpers");
        return f;
    }

    /**
     * Evaluates a script.
     * <p>
     * 스크립트를 평가한다.
     *
     * @param f    the page / 페이지
     * @param code the script / 스크립트
     * @return the result / 결과
     */
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * Rectangles and polygons: fill covers the inside, <code>beginPath</code> forgets the path, a line without a subpath starts one.
     * <p>
     * 사각형과 다각형: 채우기는 안쪽을 칠하고, <code>beginPath</code> 는 경로를 잊고, 하위 경로가 없을 때의 선은 하위 경로를 시작한다.
     */
    public void testSubpathsAndFill() {
        PageFixture f = page(30, 30);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.beginPath(); ctx.rect(2, 2, 6, 6); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(2, 2)"), "first pixel of the rectangle");
        Check.equal("255,0,0,255", js(f, "px(7, 7)"), "last pixel of the rectangle");
        Check.equal("0,0,0,0", js(f, "px(8, 8)"), "outside");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(20, 0); ctx.lineTo(0, 20); ctx.closePath(); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(5, 5)"), "inside the triangle");
        Check.equal("0,0,0,0", js(f, "px(15, 15)"), "outside the triangle (beyond the hypotenuse)");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.beginPath(); ctx.lineTo(5, 5); ctx.lineTo(25, 5); ctx.lineTo(25, 25); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(20, 10)"), "a first lineTo starts the subpath; the open path is closed for filling");
        Check.equal("0,0,0,0", js(f, "px(10, 20)"), "outside the triangle");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.beginPath(); ctx.rect(0, 0, 10, 10); ctx.beginPath(); ctx.fill();");
        Check.equal("0,0,0,0", js(f, "px(5, 5)"), "beginPath removed the rectangle");
        js(f, "ctx.beginPath(); ctx.rect(0, 0, 10, 10); ctx.fillRect(20, 20, 5, 5); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(5, 5)"), "fillRect does not touch the current path");
    }

    /**
     * The non-zero and even-odd rules decide whether a nested rectangle is a hole.
     * <p>
     * 0이 아닌 규칙과 짝홀 규칙이 안에 놓인 사각형이 구멍인지 정한다.
     */
    public void testFillRules() {
        PageFixture f = page(40, 40);
        js(f, "ctx.fillStyle = '#00ff00'; ctx.beginPath(); ctx.rect(0, 0, 30, 30); ctx.rect(10, 10, 10, 10); ctx.fill();");
        Check.equal("0,255,0,255", js(f, "px(15, 15)"), "same direction, non-zero: no hole");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.fill('evenodd');");
        Check.equal("0,0,0,0", js(f, "px(15, 15)"), "even-odd: the inner rectangle is a hole");
        Check.equal("0,255,0,255", js(f, "px(5, 5)"), "even-odd: the ring is filled");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.beginPath(); ctx.rect(0, 0, 30, 30); ctx.moveTo(10, 10); ctx.lineTo(10, 20); ctx.lineTo(20, 20);"
                + " ctx.lineTo(20, 10); ctx.closePath(); ctx.fill();");
        Check.equal("0,0,0,0", js(f, "px(15, 15)"), "opposite direction, non-zero: a hole");
        Check.equal("true,false", js(f, "ctx.beginPath(); ctx.rect(0, 0, 30, 30); ctx.rect(10, 10, 10, 10);"
                + " [ctx.isPointInPath(15, 15), ctx.isPointInPath(15, 15, 'evenodd')].join()"), "isPointInPath takes the rule");
        Check.equal("TypeError", js(f, "try { ctx.fill('bogus') } catch (e) { e.name }"), "an unknown fill rule is a TypeError");
        Check.equal("TypeError", js(f, "try { ctx.fill({}) } catch (e) { e.name }"), "a Path2D is not supported");
    }

    /**
     * Circles, partial arcs in both directions and ellipses.
     * <p>
     * 원, 양방향의 부분 호, 타원.
     */
    public void testArcsAndEllipses() {
        PageFixture f = page(100, 100);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.beginPath(); ctx.arc(50, 50, 20, 0, 2 * Math.PI); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(50, 50)"), "center of the circle");
        Check.equal("255,0,0,255", js(f, "px(60, 50)"), "inside on the x axis");
        Check.equal("255,0,0,255", js(f, "px(50, 35)"), "inside on the y axis");
        Check.equal("0,0,0,0", js(f, "px(75, 50)"), "outside");
        Check.equal("0,0,0,0", js(f, "px(50, 25)"), "outside above");
        js(f, "ctx.clearRect(0, 0, 100, 100); ctx.beginPath(); ctx.moveTo(50, 50); ctx.arc(50, 50, 30, 0, Math.PI / 2, false); ctx.closePath(); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(60, 60)"), "clockwise quarter: lower right");
        Check.equal("0,0,0,0", js(f, "px(40, 60)"), "and not the lower left quarter");
        Check.equal("0,0,0,0", js(f, "px(60, 40)"), "and not the upper right quarter");
        js(f, "ctx.clearRect(0, 0, 100, 100); ctx.beginPath(); ctx.moveTo(50, 50); ctx.arc(50, 50, 30, 0, Math.PI / 2, true); ctx.closePath(); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(40, 40)"), "counter-clockwise from 0 to a quarter turn is the long way round: upper left is inside");
        Check.equal("0,0,0,0", js(f, "px(60, 60)"), "and the lower right quarter is not");
        Check.equal("true", js(f, "ctx.beginPath(); ctx.arc(50, 50, 20, 0, 10); String(ctx.isPointInPath(30.5, 50))"), "more than a full turn is a full circle");
        js(f, "ctx.clearRect(0, 0, 100, 100); ctx.beginPath(); ctx.ellipse(50, 50, 30, 10, 0, 0, 2 * Math.PI); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(75, 50)"), "ellipse: inside along the long radius");
        Check.equal("0,0,0,0", js(f, "px(50, 65)"), "ellipse: outside along the short axis");
        js(f, "ctx.clearRect(0, 0, 100, 100); ctx.beginPath(); ctx.ellipse(50, 50, 30, 10, Math.PI / 2, 0, 2 * Math.PI); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(50, 75)"), "a quarter turn of the ellipse: the long radius points down");
        Check.equal("0,0,0,0", js(f, "px(75, 50)"), "and not right");
        Check.equal("IndexSizeError", js(f, "try { ctx.ellipse(1, 1, 5, -5, 0, 0, 1) } catch (e) { e.name }"), "negative y radius");
        Check.equal("true", js(f, "ctx.beginPath(); ctx.moveTo(10, 10); ctx.arc(50, 50, 0, 0, 1); String(ctx.isPointInPath(10, 10) || true)"), "radius zero is allowed");
        js(f, "ctx.clearRect(0, 0, 100, 100); ctx.save(); ctx.translate(50, 50); ctx.scale(2, 1); ctx.beginPath(); ctx.arc(0, 0, 10, 0, 2 * Math.PI); ctx.restore(); ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(65, 50)"), "an arc under a scaling matrix is an ellipse, and the path keeps its shape after restore");
        Check.equal("0,0,0,0", js(f, "px(50, 65)"), "(twice as wide as tall)");
    }

    /**
     * <code>arcTo</code> rounds a corner and degenerates to a line when it must.
     * <p>
     * <code>arcTo</code> 는 모서리를 둥글게 하고 그래야 할 때는 직선이 된다.
     */
    public void testArcTo() {
        PageFixture f = page(80, 80);
        js(f, "ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(10, 10); ctx.arcTo(60, 10, 60, 60, 20); ctx.lineTo(60, 60);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(25, 10))"), "the straight part before the arc");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(54.14, 15.86))"), "the middle of the arc (center (40, 30), radius 20)");
        Check.equal("false", js(f, "String(ctx.isPointInStroke(60, 10))"), "the corner itself is cut off");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(60, 45))"), "the straight part after the arc");
        js(f, "ctx.beginPath(); ctx.moveTo(10, 10); ctx.arcTo(60, 10, 60, 60, 0);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(59, 10))"), "radius zero: a line to the first point");
        js(f, "ctx.beginPath(); ctx.moveTo(10, 10); ctx.arcTo(30, 10, 60, 10, 5);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(29, 10))"), "collinear points: a line to the first point");
        Check.equal("IndexSizeError", js(f, "try { ctx.arcTo(1, 1, 2, 2, -1) } catch (e) { e.name }"), "negative radius");
        js(f, "ctx.beginPath(); ctx.arcTo(40, 40, 60, 60, 5); ctx.lineTo(40, 70);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(40, 55))"), "without a subpath the first point starts one: the next line starts there");
    }

    /**
     * Quadratic and cubic curves pass through their mathematically known points.
     * <p>
     * 2차와 3차 곡선은 수학적으로 알려진 점을 지난다.
     */
    public void testCurves() {
        PageFixture f = page(60, 60);
        js(f, "ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(0, 0); ctx.quadraticCurveTo(50, 0, 50, 50);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(37.5, 12.5))"), "the quadratic curve at t = 0.5");
        Check.equal("false", js(f, "String(ctx.isPointInStroke(25, 25))"), "a point far from the curve");
        js(f, "ctx.beginPath(); ctx.moveTo(0, 0); ctx.bezierCurveTo(0, 40, 40, 40, 40, 0);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(20, 30))"), "the cubic curve at t = 0.5");
        Check.equal("false", js(f, "String(ctx.isPointInStroke(20, 10))"), "a point inside the arch");
        js(f, "ctx.beginPath(); ctx.quadraticCurveTo(30, 30, 50, 10);");
        Check.equal("true", js(f, "String(ctx.isPointInStroke(35, 25))"), "a curve without a subpath starts at its control point: the middle of (30,30)-(30,30)-(50,10)");
    }

    /**
     * The shape of a stroke: width, caps, joins, closing segment and strokeRect.
     * <p>
     * 선의 모양: 너비, 끝 모양, 꺾임, 닫는 선분, strokeRect.
     */
    public void testStrokeShapes() {
        PageFixture f = page(60, 60);
        js(f, "ctx.strokeStyle = '#0000ff'; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(10, 20); ctx.lineTo(50, 20); ctx.stroke();");
        Check.equal("0,0,255,255", js(f, "px(30, 18)") , "the top row of a line of width 4 centered on y = 20");
        Check.equal("0,0,255,255", js(f, "px(30, 21)"), "the bottom row");
        Check.equal("0,0", js(f, "[alpha(30, 17), alpha(30, 22)].join()"), "and nothing beyond");
        Check.equal("0,0,255,255", js(f, "px(10, 20)"), "butt cap: the first pixel");
        Check.equal("0", js(f, "String(alpha(9, 20))"), "butt cap: nothing before the start");
        Check.equal("0", js(f, "String(alpha(50, 20))"), "butt cap: nothing after the end");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.lineCap = 'square'; ctx.stroke();");
        Check.equal("255", js(f, "String(alpha(8, 20))"), "square cap extends by half the width");
        Check.equal("255", js(f, "String(alpha(51, 20))"), "on both ends");
        Check.equal("0", js(f, "String(alpha(7, 20))"), "but no further");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.lineCap = 'round'; ctx.stroke();");
        Check.equal(true, Integer.parseInt(js(f, "String(alpha(9, 20))")) > 0, "round cap covers pixels before the start");
        Check.equal("0", js(f, "String(alpha(6, 20))"), "but not beyond the radius");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.lineCap = 'butt'; ctx.lineWidth = 10; ctx.lineJoin = 'miter'; ctx.beginPath(); ctx.moveTo(10, 10);"
                + " ctx.lineTo(40, 10); ctx.lineTo(40, 40); ctx.stroke();");
        Check.equal("255", js(f, "String(alpha(44, 5))"), "miter join fills the outer corner");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.lineJoin = 'bevel'; ctx.stroke();");
        Check.equal("0", js(f, "String(alpha(44, 5))"), "bevel join cuts the outer corner");
        Check.equal("255", js(f, "String(alpha(40, 8))"), "the bevel still covers the middle of the join");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.lineWidth = 2; ctx.lineJoin = 'miter'; ctx.beginPath(); ctx.moveTo(10, 10); ctx.lineTo(40, 10);"
                + " ctx.lineTo(40, 40); ctx.closePath(); ctx.stroke();");
        Check.equal("true", js(f, "String(alpha(25, 25) > 0)"), "closePath adds the closing segment of the stroke (the hypotenuse)");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.strokeRect(10, 10, 20, 20);");
        Check.equal("255", js(f, "String(alpha(20, 9))"), "the top edge of strokeRect (rows 9 and 10)");
        Check.equal("0", js(f, "String(alpha(20, 11))"), "inside");
        Check.equal("0", js(f, "String(alpha(20, 20))"), "the middle is empty");
        Check.equal("255", js(f, "String(alpha(9, 20))"), "the left edge");
        Check.equal("255", js(f, "String(alpha(30, 20))"), "the right edge (columns 29 and 30)");
    }

    /**
     * Dashes follow <code>setLineDash</code> and <code>lineDashOffset</code>; invalid patterns are ignored; an odd list is repeated.
     * <p>
     * 점선은 <code>setLineDash</code> 와 <code>lineDashOffset</code> 을 따른다. 올바르지 않은 패턴은 무시하고 홀수 목록은 한 번 더 반복한다.
     */
    public void testLineDash() {
        PageFixture f = page(60, 20);
        js(f, "ctx.lineWidth = 2; ctx.strokeStyle = '#000000'; ctx.setLineDash([4, 4]); ctx.beginPath(); ctx.moveTo(0, 10); ctx.lineTo(40, 10); ctx.stroke();");
        Check.equal("255,0,255,0,255", js(f, "[alpha(2, 9), alpha(5, 9), alpha(9, 9), alpha(13, 9), alpha(17, 9)].join()"), "dashes of 4 and gaps of 4");
        js(f, "ctx.clearRect(0, 0, 60, 20); ctx.lineDashOffset = 4; ctx.stroke();");
        Check.equal("0,255,0,255", js(f, "[alpha(2, 9), alpha(5, 9), alpha(9, 9), alpha(13, 9)].join()"), "the offset shifts the pattern");
        Check.equal("4,4", js(f, "ctx.getLineDash().join()"), "getLineDash");
        Check.equal("3,3", js(f, "ctx.setLineDash([3]); ctx.getLineDash().join()"), "a pattern of one entry is repeated");
        Check.equal("1,2,3,1,2,3", js(f, "ctx.setLineDash([1, 2, 3]); ctx.getLineDash().join()"), "an odd list is repeated");
        Check.equal("1,2,3,1,2,3", js(f, "ctx.setLineDash([-1, 5]); ctx.setLineDash([NaN]); ctx.setLineDash([Infinity, 1]); ctx.getLineDash().join()"),
                "negative and non-finite entries leave the pattern unchanged");
        Check.equal("", js(f, "ctx.setLineDash([]); ctx.getLineDash().join()"), "an empty list makes a solid line");
        Check.equal("4", js(f, "ctx.lineDashOffset = 4; ctx.lineDashOffset = NaN; String(ctx.lineDashOffset)"), "the offset ignores NaN");
        js(f, "var d = ctx.getLineDash(); d.push(9); ");
        Check.equal("", js(f, "ctx.getLineDash().join()"), "getLineDash returns a copy");
    }

    /**
     * The stroke is computed in user space, so a non-uniform scale makes it thicker in one direction.
     * <p>
     * 선은 사용자 좌표계에서 계산되므로 균일하지 않게 확대하면 한 방향으로 더 두꺼워진다.
     */
    public void testStrokeUnderScaling() {
        PageFixture f = page(60, 60);
        js(f, "ctx.save(); ctx.scale(1, 4); ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(0, 5); ctx.lineTo(40, 5); ctx.stroke(); ctx.restore();");
        Check.equal("255,255", js(f, "[alpha(20, 16), alpha(20, 23)].join()"), "eight pixels tall: rows 16 to 23");
        Check.equal("0,0", js(f, "[alpha(20, 15), alpha(20, 24)].join()"), "and not beyond");
        Check.equal("1", js(f, "String(ctx.lineWidth)"), "restore brought back the line width; the matrix never changes the attribute");
    }

    /**
     * Clipping: rectangles, antialiased diagonal edges, intersections, the empty path, save and restore, and the effect on clearRect.
     * <p>
     * 클리핑: 사각형, 안티앨리어싱된 대각선 가장자리, 교차, 빈 경로, 저장과 복원, clearRect 에 미치는 영향.
     */
    public void testClip() {
        PageFixture f = page(40, 40);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.beginPath(); ctx.rect(10, 10, 20, 20); ctx.clip(); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,0,0,0", js(f, "px(9, 9)"), "outside the clip");
        Check.equal("255,0,0,255", js(f, "px(10, 10)"), "inside, first pixel");
        Check.equal("255,0,0,255", js(f, "px(29, 29)"), "inside, last pixel");
        Check.equal("0,0,0,0", js(f, "px(30, 30)"), "outside");
        PageFixture g = page(40, 40);
        js(g, "ctx.fillStyle = '#ff0000'; ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(40, 0); ctx.lineTo(0, 40); ctx.closePath(); ctx.clip(); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("255", js(g, "String(alpha(5, 5))"), "inside the triangle clip");
        Check.equal("0", js(g, "String(alpha(35, 35))"), "outside the triangle clip");
        int edge = Integer.parseInt(js(g, "String(alpha(19, 20))"));
        Check.equal(true, edge > 90 && edge < 170, "the diagonal edge of a clip is antialiased (half covered pixel, alpha " + edge + ")");
        PageFixture h = page(40, 40);
        js(h, "ctx.fillStyle = '#00ff00'; ctx.beginPath(); ctx.rect(0, 0, 20, 40); ctx.clip(); ctx.beginPath(); ctx.rect(10, 0, 30, 40); ctx.clip();"
                + " ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,255,0,255", js(h, "px(15, 5)"), "inside both clips");
        Check.equal("0,0,0,0", js(h, "px(5, 5)"), "only in the first");
        Check.equal("0,0,0,0", js(h, "px(25, 5)"), "only in the second");
        PageFixture k = page(40, 40);
        js(k, "ctx.beginPath(); ctx.clip(); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,0,0,0", js(k, "px(5, 5)"), "clipping with an empty path clips everything away");
        PageFixture m = page(40, 40);
        js(m, "ctx.fillRect(0, 0, 40, 40); ctx.save(); ctx.beginPath(); ctx.rect(0, 0, 10, 10); ctx.clip(); ctx.clearRect(0, 0, 40, 40); ctx.restore();");
        Check.equal("0,0,0,0", js(m, "px(5, 5)"), "clearRect inside the clip clears");
        Check.equal("0,0,0,255", js(m, "px(20, 20)"), "clearRect outside the clip does not");
        PageFixture n = page(40, 40);
        js(n, "ctx.fillRect(0, 0, 40, 40); ctx.save(); ctx.beginPath(); ctx.arc(20, 20, 10, 0, 7); ctx.clip(); ctx.clearRect(0, 0, 40, 40); ctx.restore();");
        Check.equal("0,0,0,0", js(n, "px(20, 20)"), "clearRect inside a curved clip clears");
        Check.equal("0,0,0,255", js(n, "px(2, 2)"), "and leaves the rest");
    }

    /**
     * <code>isPointInPath</code> and <code>isPointInStroke</code> use canvas coordinates, while the path keeps the matrix of the time its
     * points were added.
     * <p>
     * <code>isPointInPath</code> 와 <code>isPointInStroke</code> 는 캔버스 좌표를 쓰고, 경로는 점을 더한 때의 행렬을 유지한다.
     */
    public void testPointTests() {
        PageFixture f = page(200, 100);
        Check.equal("true,false", js(f, "ctx.beginPath(); ctx.rect(10, 10, 10, 10); [ctx.isPointInPath(15, 15), ctx.isPointInPath(5, 5)].join()"), "inside and outside");
        Check.equal("true,false", js(f, "ctx.save(); ctx.translate(100, 0); ctx.beginPath(); ctx.rect(0, 0, 10, 10); ctx.restore();"
                + " [ctx.isPointInPath(105, 5), ctx.isPointInPath(5, 5)].join()"), "the points of the path were transformed when they were added");
        Check.equal("true,false", js(f, "ctx.setTransform(2, 0, 0, 2, 0, 0); [ctx.isPointInPath(105, 5), ctx.isPointInPath(52, 2)].join()"),
                "the arguments are canvas coordinates, not transformed by the current matrix");
        Check.equal("false,false", js(f, "ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.beginPath(); [ctx.isPointInPath(NaN, 1), ctx.isPointInPath(1, 1)].join()"),
                "non-finite arguments and an empty path give false");
        Check.equal("true,false", js(f, "ctx.lineWidth = 10; ctx.beginPath(); ctx.moveTo(0, 50); ctx.lineTo(100, 50); [ctx.isPointInStroke(50, 54), ctx.isPointInStroke(50, 56)].join()"),
                "points near the stroke");
        Check.equal("false", js(f, "ctx.beginPath(); String(ctx.isPointInStroke(1, 1))"), "an empty path has no stroke");
    }

    /**
     * Rounded rectangles: the corners are cut, and the radii are reduced when they do not fit.
     * <p>
     * 둥근 사각형: 모서리가 잘리고, 반지름이 맞지 않으면 줄어든다.
     */
    public void testRoundRect() {
        PageFixture f = page(60, 60);
        js(f, "ctx.fillStyle = '#ff0000'; ctx.beginPath(); ctx.roundRect(10, 10, 40, 40, 10); ctx.fill();");
        Check.equal("255", js(f, "String(alpha(30, 10))"), "the straight top edge");
        Check.equal("0", js(f, "String(alpha(10, 10))"), "the corner pixel is cut away");
        Check.equal("255", js(f, "String(alpha(12, 18))"), "a pixel inside the rounded left edge");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.beginPath(); ctx.roundRect(10, 10, 40, 20, [0, 15]); ctx.fill();");
        Check.equal("255", js(f, "String(alpha(10, 10))"), "two radii: the top left corner stays square");
        Check.equal("0", js(f, "String(alpha(49, 10))"), "and the top right corner is round");
        js(f, "ctx.clearRect(0, 0, 60, 60); ctx.beginPath(); ctx.roundRect(10, 10, 40, 20, 100); ctx.fill();");
        Check.equal("255", js(f, "String(alpha(30, 20))"), "radii larger than the sides are scaled down: a pill shape");
        Check.equal("RangeError", js(f, "try { ctx.roundRect(0, 0, 5, 5, -1) } catch (e) { e.name }"), "a negative radius is a RangeError");
        Check.equal("RangeError", js(f, "try { ctx.roundRect(0, 0, 5, 5, [1, 2, 3, 4, 5]) } catch (e) { e.name }"), "at most four radii");
    }
}
