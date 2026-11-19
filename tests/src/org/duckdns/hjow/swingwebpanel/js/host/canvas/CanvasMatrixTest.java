package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the places where the 2D canvas uses <code>DOMMatrix</code> (plan item P20-05-b): <code>getTransform()</code> returns a copy as a
 * real <code>DOMMatrix</code>, <code>setTransform()</code> takes numbers, a matrix or a <code>DOMMatrix2DInit</code> dictionary,
 * <code>Path2D.addPath()</code> takes a matrix and <code>CanvasPattern.setTransform()</code> moves, scales and rotates the tiles of a pattern.
 * Pixels are checked on axis-aligned integer rectangles, where antialiasing does not change any value.
 * <p>
 * 2D 캔버스가 <code>DOMMatrix</code> 를 쓰는 곳의 시험(계획 P20-05-b): <code>getTransform()</code> 은 진짜 <code>DOMMatrix</code> 복사본을 반환하고,
 * <code>setTransform()</code> 은 숫자, 행렬, <code>DOMMatrix2DInit</code> 사전을 받으며, <code>Path2D.addPath()</code> 는 행렬을 받고,
 * <code>CanvasPattern.setTransform()</code> 은 패턴의 타일을 옮기고 키우고 돌린다. 화소는 안티앨리어싱이 어떤 값도 바꾸지 않는 축에 나란한 정수 사각형에서
 * 확인한다.
 */
public class CanvasMatrixTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasMatrixTest() {
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
     * <code>getTransform()</code> returns a <code>DOMMatrix</code> that is a copy: later changes of the context or of the matrix do not affect
     * each other; <code>save()</code> and <code>restore()</code> bring the transformation back.
     * <p>
     * <code>getTransform()</code> 은 복사본인 <code>DOMMatrix</code> 를 반환한다: 컨텍스트나 행렬의 이후 변경은 서로 영향을 주지 않으며 <code>save()</code> 와
     * <code>restore()</code> 는 변환을 되돌린다.
     */
    public void testGetTransform() {
        PageFixture f = CanvasBasicsTest.page("width=40 height=40");
        Check.equal("true,matrix(1, 0, 0, 1, 0, 0),true,true", js(f, "var m = ctx.getTransform(); [m instanceof DOMMatrix, String(m), m.is2D, m.isIdentity].join()"),
                "the initial transformation is the identity");
        Check.equal("matrix(2, 0, 0, 3, 10, 20)", js(f, "ctx.translate(10, 20); ctx.scale(2, 3); String(ctx.getTransform())"), "translate then scale");
        Check.equal("matrix(2, 0, 0, 3, 10, 20)|matrix(1, 0, 0, 1, 0, 0)", js(f, "var m = ctx.getTransform(); m.a = 99; m.e = 5; var s1 = String(ctx.getTransform());"
                + " ctx.resetTransform(); [s1, String(ctx.getTransform())].join('|')"), "changing the returned matrix does not change the context");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)|matrix(1, 0, 0, 1, 7, 0)|matrix(1, 0, 0, 1, 0, 0)", js(f, "var before = ctx.getTransform(); ctx.save(); ctx.translate(7, 0); var inside = ctx.getTransform(); ctx.restore();"
                + " [String(before), String(inside), String(ctx.getTransform())].join('|')"), "the copy keeps its value after later changes; restore returns the transformation");
        Check.equal("false", js(f, "String(ctx.getTransform() === ctx.getTransform())"), "every call returns a new object");
        Check.equal("true", js(f, "ctx.rotate(Math.PI / 2); var m = ctx.getTransform(); Math.abs(m.a) < 1e-12 && Math.abs(m.b - 1) < 1e-12 && Math.abs(m.c + 1) < 1e-12 && Math.abs(m.d) < 1e-12"),
                "a rotation is reported as a, b, c, d");
    }

    /**
     * <code>setTransform()</code> with six numbers, with a <code>DOMMatrix</code>, with a dictionary (the short and the long names) and with
     * nothing; conflicting members are a TypeError, a non-finite value is ignored.
     * <p>
     * <code>setTransform()</code> 의 숫자 여섯 개, <code>DOMMatrix</code>, 사전(짧은 이름과 긴 이름), 인수 없음 형태. 충돌하는 멤버는 TypeError 이고 유한하지 않은 값은
     * 무시한다.
     */
    public void testSetTransform() {
        PageFixture f = CanvasBasicsTest.page("width=40 height=40");
        Check.equal("matrix(2, 0, 0, 2, 4, 6)", js(f, "ctx.setTransform(2, 0, 0, 2, 4, 6); String(ctx.getTransform())"), "six numbers");
        Check.equal("matrix(1, 2, 3, 4, 5, 6)", js(f, "ctx.setTransform(new DOMMatrix([1, 2, 3, 4, 5, 6])); String(ctx.getTransform())"), "a DOMMatrix");
        Check.equal("matrix(3, 0, 0, 1, 5, 0)", js(f, "ctx.setTransform({a: 3, e: 5}); String(ctx.getTransform())"), "a dictionary with the short names, missing members are the identity");
        Check.equal("matrix(1, 0, 0, 4, 0, 9)", js(f, "ctx.setTransform({m22: 4, m42: 9}); String(ctx.getTransform())"), "a dictionary with the long names");
        Check.equal("matrix(5, 0, 0, 1, 0, 0)", js(f, "ctx.setTransform({a: 5, m11: 5}); String(ctx.getTransform())"), "both names with the same value");
        Check.equal("TypeError|matrix(5, 0, 0, 1, 0, 0)", js(f, "var e; try { ctx.setTransform({a: 5, m11: 6}); e = 'none'; } catch (x) { e = x.name; } [e, String(ctx.getTransform())].join('|')"),
                "conflicting names are a TypeError and the transformation stays");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "ctx.setTransform(); String(ctx.getTransform())"), "no arguments reset the transformation");
        Check.equal("matrix(1, 0, 0, 1, 0, 0)", js(f, "ctx.setTransform({}); String(ctx.getTransform())"), "an empty dictionary too");
        Check.equal("matrix(2, 0, 0, 2, 0, 0)", js(f, "ctx.setTransform(2, 0, 0, 2, 0, 0); ctx.setTransform(NaN, 0, 0, 1, 0, 0); ctx.setTransform({a: Infinity}); String(ctx.getTransform())"),
                "non-finite values are ignored");
        Check.equal("TypeError|TypeError", js(f, "var r = []; try { ctx.setTransform(5); } catch (x) { r.push(x.name); } try { ctx.setTransform(1, 2, 3); } catch (x) { r.push(x.name); } r.join('|')"),
                "a single number and a short list are a TypeError");
        js(f, "ctx.setTransform(new DOMMatrix().translate(10, 10).scale(2)); ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 5, 5);");
        Check.equal("255,0,0,255|255,0,0,255|0,0,0,0|0,0,0,0", js(f, "[px(10, 10), px(19, 19), px(20, 20), px(9, 9)].join('|')"), "a DOMMatrix transformation draws at 10..20");
        js(f, "ctx.resetTransform(); ctx.clearRect(0, 0, 40, 40); ctx.setTransform(new DOMMatrix().scale(3, 1)); ctx.fillStyle = 'blue'; ctx.fillRect(1, 0, 2, 4);");
        Check.equal("0,0,255,255|0,0,255,255|0,0,0,0|0,0,0,0", js(f, "[px(3, 0), px(8, 3), px(2, 0), px(9, 0)].join('|')"), "a non-uniform scale: x 1..3 becomes 3..9");
    }

    /**
     * <code>Path2D.addPath()</code> transforms the added path with the matrix (a <code>DOMMatrix</code> or a dictionary); a matrix with
     * non-finite members adds nothing.
     * <p>
     * <code>Path2D.addPath()</code> 는 더한 경로를 행렬(<code>DOMMatrix</code> 나 사전)로 변환한다. 유한하지 않은 멤버가 있는 행렬은 아무것도 더하지 않는다.
     */
    public void testAddPathWithMatrix() {
        PageFixture f = CanvasBasicsTest.page("width=40 height=40");
        js(f, "var unit = new Path2D(); unit.rect(0, 0, 5, 5); var all = new Path2D(); all.addPath(unit, new DOMMatrix().translate(20, 20).scale(2));"
                + " all.addPath(unit, {e: 0, f: 30}); ctx.fillStyle = 'lime'; ctx.fill(all);");
        Check.equal("0,255,0,255|0,255,0,255|0,0,0,0|0,0,0,0", js(f, "[px(20, 20), px(29, 29), px(30, 30), px(19, 19)].join('|')"), "the scaled copy covers 20..30");
        Check.equal("0,255,0,255|0,255,0,255|0,0,0,0", js(f, "[px(0, 30), px(4, 34), px(5, 35)].join('|')"), "the dictionary translates without scaling");
        Check.equal("0,0,0,0", js(f, "px(2, 2)"), "the original place stays empty");
        Check.equal("TypeError", js(f, "var e; try { new Path2D().addPath(unit, {a: 1, m11: 2}); e = 'none'; } catch (x) { e = x.name; } e"), "conflicting members");
        js(f, "ctx.clearRect(0, 0, 40, 40); var none = new Path2D(); none.addPath(unit, {a: NaN}); ctx.fill(none);");
        Check.equal("0,0,0,0", js(f, "px(2, 2)"), "a non-finite matrix adds nothing");
        js(f, "ctx.clearRect(0, 0, 40, 40); var twice = new Path2D(unit); twice.addPath(twice, new DOMMatrix().translate(10, 0)); ctx.fill(twice);");
        Check.equal("0,255,0,255|0,255,0,255|0,0,0,0", js(f, "[px(2, 2), px(12, 2), px(7, 2)].join('|')"), "a path can add a copy of itself");
    }

    /**
     * <code>CanvasPattern.setTransform()</code> changes where the tiles of a pattern are: the pattern transformation is applied before the
     * transformation of the context, an identity is the default, a missing argument resets it, and a non-invertible matrix paints nothing.
     * <p>
     * <code>CanvasPattern.setTransform()</code> 은 패턴의 타일이 놓이는 위치를 바꾼다: 패턴 변환은 컨텍스트의 변환보다 먼저 적용되고, 기본값은 단위 행렬이며, 인수가 없으면
     * 되돌리고, 뒤집을 수 없는 행렬로는 아무것도 칠하지 않는다.
     */
    public void testPatternTransform() {
        PageFixture f = CanvasBasicsTest.page("width=40 height=20");
        js(f, "var tile = document.createElement('canvas'); tile.width = 2; tile.height = 1; var t = tile.getContext('2d');"
                + " t.fillStyle = 'red'; t.fillRect(0, 0, 1, 1); t.fillStyle = 'blue'; t.fillRect(1, 0, 1, 1);"
                + " var pat = ctx.createPattern(tile, 'repeat'); ctx.imageSmoothingEnabled = false; ctx.fillStyle = pat; ctx.fillRect(0, 0, 40, 20);");
        Check.equal("255,0,0,255|0,0,255,255|255,0,0,255|0,0,255,255", js(f, "[px(0, 0), px(1, 0), px(2, 0), px(3, 0)].join('|')"), "without a transformation the tile is one pixel per pixel");
        Check.equal("undefined", js(f, "typeof pat.setTransform === 'function' ? String(pat.setTransform()) : 'missing'"), "setTransform exists and returns undefined");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform(new DOMMatrix().scale(10)); ctx.fillRect(0, 0, 40, 20);");
        Check.equal("255,0,0,255|255,0,0,255|0,0,255,255|0,0,255,255|255,0,0,255", js(f, "[px(0, 0), px(9, 0), px(10, 0), px(19, 0), px(20, 0)].join('|')"), "a scale of 10 makes the tiles 10 pixels wide");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform({a: 10, e: 5}); ctx.fillRect(0, 0, 40, 20);");
        Check.equal("0,0,255,255|255,0,0,255|255,0,0,255|0,0,255,255", js(f, "[px(2, 0), px(5, 0), px(14, 0), px(15, 0)].join('|')"), "the tiles move by 5 pixels");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform(new DOMMatrix().scale(10, 20)); ctx.save(); ctx.translate(5, 0); ctx.fillRect(-5, 0, 40, 20); ctx.restore();");
        Check.equal("0,0,255,255|0,0,255,255|255,0,0,255|255,0,0,255|0,0,255,255", js(f, "[px(0, 19), px(4, 0), px(5, 0), px(14, 0), px(15, 0)].join('|')"),
                "the pattern transformation is applied in the space of the context: the translation of the context moves the pattern, too");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform(); ctx.fillRect(0, 0, 40, 20);");
        Check.equal("255,0,0,255|0,0,255,255", js(f, "[px(0, 0), px(1, 0)].join('|')"), "setTransform() with no argument is the identity");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform(new DOMMatrix([0, 0, 0, 0, 0, 0])); ctx.fillRect(0, 0, 40, 20);");
        Check.equal("0,0,0,0", js(f, "px(3, 3)"), "a matrix that cannot be inverted paints nothing");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform({a: NaN}); ctx.fillRect(0, 0, 40, 20);");
        Check.equal("0,0,0,0", js(f, "px(3, 3)"), "a matrix with NaN paints nothing");
        Check.equal("TypeError|TypeError", js(f, "var r = []; try { pat.setTransform({a: 1, m11: 2}); } catch (x) { r.push(x.name); }"
                + " try { CanvasPattern.prototype.setTransform.call({}); } catch (x) { r.push(x.name); } r.join('|')"), "conflicting members and a wrong receiver");
        js(f, "ctx.clearRect(0, 0, 40, 20); pat.setTransform(new DOMMatrix().rotate(90).scale(4)); ctx.fillRect(0, 0, 40, 20);");
        // 4배 확대 후 90도 돌린 타일: 가로로 1 화소였던 폭이 세로 4 화소가 되어 세로 줄무늬가 가로 줄무늬가 된다.
        Check.equal("255,0,0,255|255,0,0,255|0,0,255,255|255,0,0,255", js(f, "[px(30, 1), px(5, 2), px(30, 5), px(30, 9)].join('|')"), "a rotated pattern gives horizontal stripes of 4 pixels");
    }
}
