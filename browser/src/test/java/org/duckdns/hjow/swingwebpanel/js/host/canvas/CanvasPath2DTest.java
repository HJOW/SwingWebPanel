package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>Path2D</code> (plan item P20-05): building paths without a context, using them with <code>fill</code>, <code>stroke</code>,
 * <code>clip</code>, <code>isPointInPath</code> and <code>isPointInStroke</code> under the matrix of the time of use, copying and
 * <code>addPath</code> with a matrix, and the SVG path data of the constructor (all commands, relative forms, implicit repetition, compact
 * numbers, arcs, and the stop at the first error).
 * <p>
 * <code>Path2D</code> 시험(계획 P20-05): 컨텍스트 없이 경로를 만들고, <code>fill</code>, <code>stroke</code>, <code>clip</code>, <code>isPointInPath</code>,
 * <code>isPointInStroke</code> 와 함께 쓰는 시점의 행렬로 사용하며, 복사와 행렬이 있는 <code>addPath</code>, 생성자의 SVG 경로 데이터(모든 명령, 상대 형식,
 * 암묵적 반복, 붙여 쓴 숫자, 호, 첫 오류에서 멈춤)를 확인한다.
 */
public class CanvasPath2DTest {

    /**
     * The helper script of the page.
     * <p>
     * 페이지의 도우미 스크립트.
     */
    private static final String HELPERS = CanvasBasicsTest.HELPERS + "function alpha(x, y) { return ctx.getImageData(x, y, 1, 1).data[3]; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasPath2DTest() {
    }

    /**
     * Creates a page with a canvas of a size.
     * <p>
     * 크기가 정해진 캔버스가 있는 페이지를 만든다.
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
     * A <code>Path2D</code> is built without a context and filled, stroked and used for clipping with the matrix of that time; the current
     * path of the context is not touched.
     * <p>
     * <code>Path2D</code> 는 컨텍스트 없이 만들고 그때의 행렬로 채우고, 긋고, 클립에 쓴다. 컨텍스트의 현재 경로는 건드리지 않는다.
     */
    public void testBuildAndUse() {
        PageFixture f = page(40, 40);
        Check.equal("[object Path2D]", js(f, "var p = new Path2D(); p.rect(2, 2, 6, 6); Object.prototype.toString.call(p)"), "interface name");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fill(p);");
        Check.equal("255,0,0,255", js(f, "px(2, 2)"), "fill(path) fills the rectangle");
        Check.equal("0,0,0,0", js(f, "px(8, 8)"), "and no more");
        js(f, "ctx.fill();");
        Check.equal("0,0,0,0", js(f, "px(20, 20)"), "the current path of the context was not touched (it is empty)");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.save(); ctx.translate(20, 20); ctx.fill(p); ctx.restore();");
        Check.equal("255,0,0,255", js(f, "px(22, 22)"), "the matrix of the time of use applies");
        Check.equal("0,0,0,0", js(f, "px(2, 2)"), "also to where the path is");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.strokeStyle = '#0000ff'; ctx.lineWidth = 2; ctx.stroke(p);");
        Check.equal("0,0,255,255", js(f, "px(5, 1)"), "stroke(path) strokes the outline");
        Check.equal("0,0,0,0", js(f, "px(5, 5)"), "and not the inside");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.save(); ctx.clip(p); ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 40, 40); ctx.restore();");
        Check.equal("0,255,0,255", js(f, "px(5, 5)"), "clip(path) clips to the rectangle");
        Check.equal("0,0,0,0", js(f, "px(20, 20)"), "and not beyond");
        Check.equal("true,false,true,false", js(f, "ctx.beginPath(); [ctx.isPointInPath(p, 5, 5), ctx.isPointInPath(p, 20, 20), ctx.isPointInStroke(p, 2, 5), ctx.isPointInStroke(p, 5, 5)].join()"),
                "the point tests take a path");
        Check.equal("true", js(f, "ctx.save(); ctx.translate(100, 0); var inside = ctx.isPointInPath(p, 105, 5); ctx.restore(); String(inside)"), "with the matrix on the path, not on the point");
    }

    /**
     * Copying, <code>addPath</code> with and without a matrix, adding a path to itself, and the fill rule argument after the path.
     * <p>
     * 복사, 행렬이 있거나 없는 <code>addPath</code>, 자기 자신을 더하기, 경로 뒤의 채움 규칙 인수.
     */
    public void testCopyAndAddPath() {
        PageFixture f = page(60, 40);
        js(f, "var a = new Path2D(); a.rect(0, 0, 10, 10); var b = new Path2D(a); b.rect(20, 0, 10, 10); ctx.fillStyle = '#ff0000'; ctx.fill(a);");
        Check.equal("0,0", js(f, "[alpha(25, 5), alpha(15, 5)].join()"), "the copy is independent of the original");
        js(f, "ctx.clearRect(0, 0, 60, 40); ctx.fill(b);");
        Check.equal("255,255", js(f, "[alpha(5, 5), alpha(25, 5)].join()"), "the copy has both rectangles");
        js(f, "ctx.clearRect(0, 0, 60, 40); var c = new Path2D(); c.addPath(a, {a: 1, b: 0, c: 0, d: 1, e: 40, f: 20}); ctx.fill(c);");
        Check.equal("255,0", js(f, "[alpha(45, 25), alpha(5, 5)].join()"), "addPath with a matrix moves the points");
        js(f, "ctx.clearRect(0, 0, 60, 40); var d = new Path2D(); d.addPath(a); d.addPath(a, {e: 20}); d.addPath(d, {f: 20}); ctx.fill(d);");
        Check.equal("255,255,255,255", js(f, "[alpha(5, 5), alpha(25, 5), alpha(5, 25), alpha(25, 25)].join()"), "addPath without a matrix, with a partial matrix, and of itself");
        Check.equal("TypeError", js(f, "try { d.addPath({}) } catch (e) { e.name }"), "not a path");
        Check.equal("TypeError", js(f, "try { d.addPath() } catch (e) { e.name }"), "the path is required");
        js(f, "var e2 = new Path2D(); e2.rect(0, 0, 30, 30); e2.rect(10, 10, 10, 10); ctx.clearRect(0, 0, 60, 40); ctx.fill(e2, 'evenodd');");
        Check.equal("0,255", js(f, "[alpha(15, 15), alpha(5, 5)].join()"), "the fill rule follows the path");
        Check.equal("true,false", js(f, "[ctx.isPointInPath(e2, 5, 5, 'evenodd'), ctx.isPointInPath(e2, 15, 15, 'evenodd')].join()"), "isPointInPath takes the rule last");
    }

    /**
     * Path methods follow the rules of the context: non-finite arguments are ignored, a negative radius is an error, arcs and curves work.
     * <p>
     * 경로 메서드는 컨텍스트의 규칙을 따른다: 유한하지 않은 인수는 무시하고, 음수 반지름은 오류이며, 호와 곡선이 동작한다.
     */
    public void testMethods() {
        PageFixture f = page(100, 100);
        js(f, "var p = new Path2D(); p.moveTo(NaN, 5); p.lineTo(Infinity, 1); p.arc(50, 50, 20, 0, 2 * Math.PI); ctx.fillStyle = '#ff0000'; ctx.fill(p);");
        Check.equal("255,0", js(f, "[alpha(50, 50), alpha(80, 50)].join()"), "non-finite arguments are ignored; the arc is a circle");
        Check.equal("IndexSizeError", js(f, "try { p.arc(1, 1, -1, 0, 1) } catch (e) { e.name }"), "negative radius");
        Check.equal("IndexSizeError", js(f, "try { p.arcTo(1, 1, 2, 2, -1) } catch (e) { e.name }"), "negative arcTo radius");
        Check.equal("IndexSizeError", js(f, "try { p.ellipse(1, 1, 5, -5, 0, 0, 1) } catch (e) { e.name }"), "negative ellipse radius");
        Check.equal("TypeError", js(f, "try { p.lineTo(1) } catch (e) { e.name }"), "missing arguments");
        js(f, "ctx.clearRect(0, 0, 100, 100); var q = new Path2D(); q.moveTo(10, 10); q.arcTo(60, 10, 60, 60, 20); q.lineTo(60, 60); q.closePath(); ctx.lineWidth = 2;");
        Check.equal("true,false", js(f, "[ctx.isPointInStroke(q, 25, 10), ctx.isPointInStroke(q, 60, 10)].join()"), "arcTo cuts the corner");
        js(f, "var r = new Path2D(); r.moveTo(0, 0); r.quadraticCurveTo(50, 0, 50, 50); var s = new Path2D(); s.moveTo(0, 0); s.bezierCurveTo(0, 40, 40, 40, 40, 0);");
        Check.equal("true,true", js(f, "[ctx.isPointInStroke(r, 37.5, 12.5), ctx.isPointInStroke(s, 20, 30)].join()"), "quadratic and cubic curves");
        js(f, "var t = new Path2D(); t.roundRect(10, 10, 40, 40, 10); ctx.clearRect(0, 0, 100, 100); ctx.fill(t);");
        Check.equal("255,0", js(f, "[alpha(30, 10), alpha(10, 10)].join()"), "roundRect on a path");
        js(f, "var u = new Path2D(); u.ellipse(50, 50, 30, 10, Math.PI / 2, 0, 2 * Math.PI); ctx.clearRect(0, 0, 100, 100); ctx.fill(u);");
        Check.equal("255,0", js(f, "[alpha(50, 75), alpha(75, 50)].join()"), "ellipse on a path");
        Check.equal("TypeError", js(f, "try { Path2D() } catch (e) { e.name }"), "the constructor needs new");
        Check.equal("TypeError", js(f, "try { ctx.fill({}) } catch (e) { e.name }"), "an object that is no Path2D is a TypeError");
        Check.equal("TypeError", js(f, "try { ctx.stroke({}) } catch (e) { e.name }"), "also for stroke");
        Check.equal("TypeError", js(f, "try { ctx.isPointInPath(p, 1) } catch (e) { e.name }"), "isPointInPath(path, x) lacks y");
    }

    /**
     * SVG path data: absolute and relative commands, implicit repetition, compact numbers, curves, arcs and the stop at the first error.
     * <p>
     * SVG 경로 데이터: 절대/상대 명령, 암묵적 반복, 붙여 쓴 숫자, 곡선, 호, 첫 오류에서 멈춤.
     */
    public void testSvgPathData() {
        PageFixture f = page(100, 100);
        js(f, "ctx.fillStyle = '#ff0000'; function fillData(d) { ctx.clearRect(0, 0, 100, 100); ctx.fill(new Path2D(d)); }");
        js(f, "fillData('M10 10 H 30 V 30 H 10 Z');");
        Check.equal("255,0", js(f, "[alpha(20, 20), alpha(40, 20)].join()"), "absolute M H V Z");
        js(f, "fillData('m 10 10 h 20 v 20 h -20 z');");
        Check.equal("255,0", js(f, "[alpha(20, 20), alpha(40, 20)].join()"), "relative m h v z");
        js(f, "fillData('M 10 10 40 10 40 40 z');");
        Check.equal("255,0", js(f, "[alpha(35, 15), alpha(15, 35)].join()"), "pairs after M are implicit L commands");
        js(f, "fillData('M10-5L20.5.5 30 30z');");
        Check.equal("true", js(f, "String(ctx.isPointInPath(new Path2D('M10-5L20.5.5 30 30z'), 23, 15))"), "compact numbers: M10-5 is (10, -5), L20.5.5 is (20.5, 0.5)");
        js(f, "fillData('M10,10L50,10,50,50Z');");
        Check.equal("255", js(f, "String(alpha(40, 20))"), "commas separate numbers");
        js(f, "fillData('M 10 50 C 10 10, 50 10, 50 50 S 90 90 90 50 Z');");
        Check.equal("255", js(f, "String(alpha(30, 30))"), "cubic and smooth cubic curves");
        js(f, "fillData('M 10 90 Q 50 10 90 90 T 170 90 Z');");
        Check.equal("255", js(f, "String(alpha(50, 70))"), "quadratic and smooth quadratic curves");
        js(f, "fillData('M10 50 A 20 20 0 0 1 50 50 Z');");
        Check.equal("255,0", js(f, "[alpha(30, 40), alpha(30, 60)].join()"), "an arc with the sweep flag 1 goes over the top (clockwise on the screen)");
        js(f, "fillData('M10 50 A 20 20 0 0 0 50 50 Z');");
        Check.equal("0,255", js(f, "[alpha(30, 40), alpha(30, 60)].join()"), "with the sweep flag 0 it goes below");
        js(f, "fillData('M10 50a20 20 0 1 1 40 0z');");
        Check.equal("255", js(f, "String(alpha(30, 40))"), "compact arc flags and relative arcs");
        js(f, "fillData('M10 50 A 5 5 0 0 1 50 50 Z');");
        Check.equal("255", js(f, "String(alpha(30, 45))"), "radii that are too small are enlarged to reach the end point");
        js(f, "fillData('M10 10 L 30 10 L 30 30 X 60 60 L 10 90');");
        Check.equal("255,0", js(f, "[alpha(25, 15), alpha(20, 70)].join()"), "the first error stops the parsing and keeps the path before it");
        js(f, "fillData('L 10 10 L 30 10 L 30 30');");
        Check.equal("0", js(f, "String(alpha(25, 15))"), "path data must start with a move");
        js(f, "fillData('');");
        Check.equal("0", js(f, "String(alpha(25, 15))"), "empty data is an empty path");
        js(f, "fillData('M10 10 L 30 10 L 30 30 Z L 10 30 L 30 30');");
        Check.equal("255", js(f, "String(alpha(15, 20))"), "a command after Z starts a new subpath at the start point");
        js(f, "fillData('M 1e1 1E1 l 2e1 0 0 +2e1 z');");
        Check.equal("255", js(f, "String(alpha(25, 15))"), "exponents and signs");
        Check.equal("true", js(f, "String(ctx.isPointInPath(new Path2D('M0 0 L 50 0 L 50 50 L 0 50 Z'), 25, 25))"), "isPointInPath works with data paths");
    }
}
