package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the remaining members of the 2D context (plan item P20-05-b): <code>createConicGradient()</code>, <code>drawFocusIfNeeded()</code>, the
 * members about a lost context (<code>isContextLost()</code>, <code>getContextAttributes()</code>, <code>oncontextlost</code>) and the members that are
 * deliberately not defined (hit regions, <code>scrollPathIntoView()</code>). The text attributes are in <code>CanvasTextStyleTest</code>, the
 * <code>filter</code> attribute in <code>CanvasFilterTest</code>, the matrix members in <code>CanvasMatrixTest</code>. The pictures are checked on
 * axis-aligned integer rectangles or on the center of large regions, where antialiasing does not change any value.
 * <p>
 * 2D 컨텍스트의 남은 멤버 시험(계획 P20-05-b): <code>createConicGradient()</code>, <code>drawFocusIfNeeded()</code>, 잃어버린 컨텍스트에 관한 멤버
 * (<code>isContextLost()</code>, <code>getContextAttributes()</code>, <code>oncontextlost</code>), 일부러 정의하지 않은 멤버(히트 영역,
 * <code>scrollPathIntoView()</code>). 텍스트 속성은 <code>CanvasTextStyleTest</code>, <code>filter</code> 속성은 <code>CanvasFilterTest</code>, 행렬 멤버는
 * <code>CanvasMatrixTest</code> 에 있다. 그림은 안티앨리어싱이 어떤 값도 바꾸지 않는 축에 나란한 정수 사각형이나 큰 영역의 가운데에서 확인한다.
 */
public class CanvasExtrasTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasExtrasTest() {
    }

    /**
     * Evaluates a script on a page and returns the string of the last expression.
     * <p>
     * 페이지에서 스크립트를 평가하여 마지막 식의 문자열을 반환한다.
     *
     * @param f    the page / 페이지
     * @param code the script / 스크립트
     * @return the result / 결과
     */
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * The conic gradient paints clockwise around its center starting at the start angle: with four hard-edged quarters each quadrant of the
     * canvas has one color, the start angle turns the quarters, a transformation moves the center, stops outside the turn are clamped, and the
     * arguments are checked like those of the other gradient constructors.
     * <p>
     * 원뿔형 그라디언트는 중심 둘레를 시작 각도에서 시계 방향으로 칠한다: 날카로운 네 구간이면 캔버스의 각 사분면이 한 가지 색이고, 시작 각도는 구간을 돌리고,
     * 변환은 중심을 옮기며, 한 바퀴 밖의 정지점은 한정되고, 인수는 다른 그라디언트 생성자처럼 검사한다.
     */
    public void testConicGradient() {
        PageFixture f = CanvasBasicsTest.page("width=40 height=40");
        js(f, "function quarters(start) { var g = ctx.createConicGradient(start, 20, 20); g.addColorStop(0, 'red'); g.addColorStop(0.25, 'red');"
                + " g.addColorStop(0.25, 'blue'); g.addColorStop(0.5, 'blue'); g.addColorStop(0.5, 'lime'); g.addColorStop(0.75, 'lime');"
                + " g.addColorStop(0.75, 'yellow'); g.addColorStop(1, 'yellow'); return g; }"
                + " function quad() { return [px(30, 30), px(10, 30), px(10, 10), px(30, 10)].join(' '); }");
        Check.equal("true", js(f, "var g0 = quarters(0); String(g0 instanceof CanvasGradient)"), "a CanvasGradient");
        js(f, "ctx.fillStyle = g0; ctx.fillRect(0, 0, 40, 40);");
        Check.equal("255,0,0,255 0,0,255,255 0,255,0,255 255,255,0,255", js(f, "quad()"),
                "clockwise from the positive x axis: below right is red, below left blue, above left lime, above right yellow");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.fillStyle = quarters(Math.PI / 2); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("255,255,0,255 255,0,0,255 0,0,255,255 0,255,0,255", js(f, "quad()"), "a start angle of a quarter turn rotates the colors clockwise");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.fillStyle = quarters(-Math.PI / 2); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,0,255,255 0,255,0,255 255,255,0,255 255,0,0,255", js(f, "quad()"), "a negative start angle rotates them back");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.fillStyle = quarters(0); ctx.save(); ctx.translate(10, 10); ctx.fillRect(-10, -10, 40, 40); ctx.restore();");
        // 중심이 (20,20)에서 (30,30)으로 옮겨지면 (25,25)는 중심의 왼쪽 위(라임)이고, 변환이 없었다면 오른쪽 아래(빨강)이다.
        Check.equal("0,255,0,255 255,0,0,255", js(f, "[px(25, 25), px(35, 35)].join(' ')"),
                "the gradient lives in the coordinate space of the painting (the transformation moves the center to 30,30)");
        js(f, "ctx.clearRect(0, 0, 40, 40); var g = ctx.createConicGradient(0, 20, 20); g.addColorStop(0.5, 'blue'); ctx.fillStyle = g; ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,0,255,255 0,0,255,255", js(f, "[px(30, 30), px(10, 10)].join(' ')"), "one stop colors everything");
        js(f, "ctx.clearRect(0, 0, 40, 40); var h = ctx.createConicGradient(0, 20, 20); h.addColorStop(0, 'black'); h.addColorStop(1, 'white');"
                + " ctx.fillStyle = h; ctx.fillRect(0, 0, 40, 40);");
        String left = js(f, "px(10, 20)");
        String below = js(f, "px(20, 30)");
        // 서쪽은 한 바퀴의 절반(회색 중간), 남쪽은 4분의 1(어두운 회색)이다.
        int leftValue = Integer.parseInt(left.split(",")[0]);
        int belowValue = Integer.parseInt(below.split(",")[0]);
        Check.equal(true, leftValue > 120 && leftValue < 136, "west is half a turn from the start: mid gray (" + left + ")");
        Check.equal(true, belowValue > 56 && belowValue < 72, "south is a quarter turn: dark gray (" + below + ")");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.strokeStyle = quarters(0); ctx.lineWidth = 8; ctx.strokeRect(4, 4, 32, 32);");
        Check.equal("255,0,0,255", js(f, "px(32, 32)"), "it can stroke as well");
        Check.equal("TypeError", js(f, "try { ctx.createConicGradient(0, 1) } catch (e) { e.name }"), "three arguments are required");
        Check.equal("TypeError", js(f, "try { ctx.createConicGradient(NaN, 1, 1) } catch (e) { e.name }"), "the arguments must be finite");
        Check.equal("TypeError", js(f, "try { ctx.createConicGradient(0, Infinity, 1) } catch (e) { e.name }"), "also the center");
        Check.equal("IndexSizeError", js(f, "try { ctx.createConicGradient(0, 1, 1).addColorStop(2, 'red') } catch (e) { e.name }"), "addColorStop checks the offset");
        Check.equal("SyntaxError", js(f, "try { ctx.createConicGradient(0, 1, 1).addColorStop(1, 'nonsense') } catch (e) { e.name }"), "and the color");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.fillStyle = ctx.createConicGradient(0, 20, 20); ctx.fillRect(0, 0, 40, 40);");
        Check.equal("0,0,0,0", js(f, "px(30, 30)"), "a gradient without stops paints transparent black");
    }

    /**
     * <code>drawFocusIfNeeded()</code> draws a ring only when the element has the focus and lives inside the canvas (its fallback content); the
     * ring follows the transformation and the clip, is not changed by the shadow, the global alpha, the compositing operator, the filter or the
     * styles, works with a <code>Path2D</code> too, and rejects arguments that are no elements. The context of an <code>OffscreenCanvas</code>
     * has no such method.
     * <p>
     * <code>drawFocusIfNeeded()</code> 는 요소가 포커스를 가졌고 캔버스 안(대체 콘텐츠)에 있을 때만 링을 그린다. 링은 변환과 클립을 따르고, 그림자, 전역 알파, 합성 연산자,
     * 필터, 스타일에 영향받지 않으며, <code>Path2D</code> 와도 동작하고, 요소가 아닌 인수는 거부한다. <code>OffscreenCanvas</code> 의 컨텍스트에는 이 메서드가 없다.
     */
    public void testDrawFocusIfNeeded() {
        PageFixture f = new PageFixture().load("<canvas id=c width=40 height=40><button id=b>b</button><input id=i></canvas><button id=out>o</button>");
        f.scripts.evaluate(CanvasBasicsTest.HELPERS + " var b = document.getElementById('b'), out = document.getElementById('out'); ctx.rect(10.5, 10.5, 20, 20);", "helpers");
        js(f, "ctx.drawFocusIfNeeded(b);");
        Check.equal("0,0,0,0", js(f, "px(10, 20)"), "nothing is drawn for an element that is not focused");
        js(f, "b.focus(); ctx.drawFocusIfNeeded(b);");
        Check.equal("16,16,16,255|255,255,255,255|255,255,255,255|0,0,0,0|0,0,0,0", js(f, "[px(10, 20), px(9, 20), px(11, 20), px(15, 20), px(5, 5)].join('|')"),
                "a dark ring of one pixel with a white halo on both sides around the current path, nothing inside or outside");
        js(f, "ctx.clearRect(0, 0, 40, 40); ctx.globalAlpha = 0.2; ctx.shadowColor = 'red'; ctx.shadowOffsetX = 5; ctx.shadowBlur = 2; ctx.globalCompositeOperation = 'destination-out';"
                + " ctx.filter = 'invert(1)'; ctx.strokeStyle = 'lime'; ctx.fillStyle = 'blue'; ctx.lineWidth = 6; ctx.drawFocusIfNeeded(b);");
        Check.equal("16,16,16,255|255,255,255,255|0,0,0,0", js(f, "[px(10, 20), px(9, 20), px(15, 20)].join('|')"),
                "the ring is not subject to the alpha, the shadow, the operator, the filter and the styles");
        js(f, "ctx.reset(); var p = new Path2D(); p.rect(10.5, 10.5, 20, 20); ctx.translate(0, 8); ctx.drawFocusIfNeeded(p, b);");
        Check.equal("16,16,16,255|16,16,16,255|0,0,0,0", js(f, "[px(10, 28), px(20, 18), px(20, 10)].join('|')"), "a Path2D with the transformation of the context: the ring moved down by 8");
        js(f, "ctx.reset(); ctx.beginPath(); ctx.rect(0, 0, 20, 40); ctx.clip(); ctx.beginPath(); ctx.rect(10.5, 10.5, 20, 20); ctx.drawFocusIfNeeded(b);");
        Check.equal("16,16,16,255|0,0,0,0", js(f, "[px(15, 10), px(25, 10)].join('|')"), "the clip applies to the ring");
        js(f, "ctx.reset(); ctx.beginPath(); ctx.rect(10.5, 10.5, 20, 20); out.focus(); ctx.drawFocusIfNeeded(out); ctx.drawFocusIfNeeded(cv);");
        Check.equal("0,0,0,0", js(f, "px(10, 20)"), "a focused element outside the canvas and the canvas element itself get no ring");
        js(f, "document.getElementById('i').focus(); ctx.drawFocusIfNeeded(b);");
        Check.equal("0,0,0,0", js(f, "px(10, 20)"), "an element of the canvas that is not the focused one gets no ring");
        js(f, "b.focus(); ctx.reset(); ctx.drawFocusIfNeeded(b);");
        Check.equal("0,0,0,0", js(f, "px(10, 20)"), "an empty path has no ring");
        Check.equal("TypeError|TypeError|TypeError|TypeError", js(f, "var r = []; function t(fn) { try { fn(); r.push('none'); } catch (e) { r.push(e.name); } } t(function() { ctx.drawFocusIfNeeded(); });"
                + " t(function() { ctx.drawFocusIfNeeded({}); }); t(function() { ctx.drawFocusIfNeeded(new Path2D()); }); t(function() { ctx.drawFocusIfNeeded({}, b); }); r.join('|')"),
                "missing and wrong arguments are a TypeError");
        Check.equal("function|undefined", js(f, "[typeof ctx.drawFocusIfNeeded, typeof new OffscreenCanvas(5, 5).getContext('2d').drawFocusIfNeeded].join('|')"),
                "the offscreen context does not have the user interface mixin");
    }

    /**
     * The members about a lost context: this software canvas never loses its context, so <code>isContextLost()</code> is false, the settings
     * are those of the specification, and the handler attributes <code>oncontextlost</code> and <code>oncontextrestored</code> exist on the
     * element (an event listener can be added, it just never fires). <code>scrollPathIntoView()</code> and the hit region methods were removed
     * from the specification and are not defined.
     * <p>
     * 잃어버린 컨텍스트에 관한 멤버: 이 소프트웨어 캔버스는 컨텍스트를 잃지 않으므로 <code>isContextLost()</code> 는 false 이고, 설정은 규격의 것이며, 핸들러 속성
     * <code>oncontextlost</code> 와 <code>oncontextrestored</code> 가 요소에 있다(이벤트 리스너를 달 수 있지만 발생하지 않는다). <code>scrollPathIntoView()</code> 와
     * 히트 영역 메서드는 규격에서 삭제되었으므로 정의하지 않는다.
     */
    public void testContextLossAndRemovedMembers() {
        PageFixture f = CanvasBasicsTest.page("width=20 height=20");
        Check.equal("false", js(f, "String(ctx.isContextLost())"), "the context is not lost");
        Check.equal("true|true", js(f, "var d = ctx.getContextAttributes(); [d.alpha === true && d.desynchronized === false && d.willReadFrequently === false && d.colorSpace === 'srgb',"
                + " JSON.stringify(Object.keys(d).sort()) === JSON.stringify(['alpha', 'colorSpace', 'desynchronized', 'willReadFrequently'])].join('|')"), "the settings of the context");
        Check.equal("object,object", js(f, "[typeof cv.oncontextlost, typeof cv.oncontextrestored].join()"), "the handler attributes exist (null at first)");
        Check.equal("0", js(f, "var got = 0; cv.addEventListener('contextlost', function () { got++; }); cv.oncontextlost = function () { got++; }; ctx.fillRect(0, 0, 5, 5); cv.width = 30; String(got)"),
                "nothing fires: the canvas never loses its context");
        Check.equal("undefined,undefined,undefined,undefined", js(f, "[typeof ctx.scrollPathIntoView, typeof ctx.addHitRegion, typeof ctx.removeHitRegion, typeof ctx.clearHitRegions].join()"),
                "members that were removed from the specification are not defined");
    }
}
