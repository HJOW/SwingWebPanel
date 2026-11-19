package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the canvas element and the basic drawing of the 2D context (plan item P20-05): the default size and the size attributes, which
 * context types exist (<code>2d</code> only; WebGL, WebGL2 and WebGPU answer null and their interfaces are not defined), colors and styles,
 * the state stack, transformations, rectangles and global alpha. The pictures are checked pixel by pixel with <code>getImageData</code> on
 * axis-aligned integer rectangles, where antialiasing does not change any value.
 * <p>
 * canvas 요소와 2D 컨텍스트의 기본 그리기 시험(계획 P20-05): 기본 크기와 크기 속성, 존재하는 컨텍스트 형식(<code>2d</code> 뿐이며 WebGL, WebGL2, WebGPU 는
 * null 로 답하고 인터페이스도 정의되지 않음), 색과 스타일, 상태 스택, 변환, 사각형, 전역 알파. 그림은 안티앨리어싱이 어떤 값도 바꾸지 않는 축에 나란한 정수
 * 사각형에서 <code>getImageData</code> 로 화소마다 확인한다.
 */
public class CanvasBasicsTest {

    /**
     * The helper script: <code>px(x, y)</code> returns the pixel as <code>r,g,b,a</code>.
     * <p>
     * 도우미 스크립트: <code>px(x, y)</code> 는 화소를 <code>r,g,b,a</code> 로 반환한다.
     */
    static final String HELPERS = "var cv = document.getElementById('c'); var ctx = cv.getContext('2d');"
            + "function px(x, y) { var d = ctx.getImageData(x, y, 1, 1).data; return d[0] + ',' + d[1] + ',' + d[2] + ',' + d[3]; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasBasicsTest() {
    }

    /**
     * Loads a page with a canvas and the helpers.
     * <p>
     * 캔버스와 도우미가 있는 페이지를 로드한다.
     *
     * @param canvasAttributes the attributes of the canvas / 캔버스의 속성
     * @return the fixture / 시험 도구
     */
    static PageFixture page(String canvasAttributes) {
        PageFixture f = new PageFixture().load("<canvas id=c " + canvasAttributes + "></canvas>");
        f.scripts.evaluate(HELPERS, "helpers");
        return f;
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
    static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * The canvas has the default size 300 by 150 and each attribute falls back independently when it is absent or invalid.
     * <p>
     * 캔버스의 기본 크기는 300 x 150 이며 속성이 없거나 올바르지 않으면 각각 따로 기본값으로 돌아간다.
     */
    public void testSizeAttributes() {
        PageFixture f = page("");
        Check.equal("300,150", js(f, "[cv.width, cv.height].join()"), "default size");
        f = page("width=abc height='20px'");
        Check.equal("300,20", js(f, "[cv.width, cv.height].join()"), "invalid width falls back, trailing text after the digits is ignored");
        f = page("width='-5' height=' +7'");
        Check.equal("300,7", js(f, "[cv.width, cv.height].join()"), "a minus sign is invalid, white space and a plus sign are allowed");
        Check.equal("50", js(f, "cv.width = 50; cv.getAttribute('width')"), "the width property writes the attribute");
        Check.equal("200,150", js(f, "cv.height = 150; cv.width = 200; [cv.width, cv.height].join()"), "both properties read the attributes");
        Check.equal("300", js(f, "cv.width = 4294967295; cv.getAttribute('width')"), "a value above 2147483647 sets the default");
    }

    /**
     * Only the 2D context exists: WebGL, WebGL2, WebGPU and every unknown type answer null, and neither their interfaces nor
     * <code>navigator.gpu</code> are defined.
     * <p>
     * 2D 컨텍스트만 있다: WebGL, WebGL2, WebGPU 와 모든 모르는 형식은 null 로 답하고, 그 인터페이스도 <code>navigator.gpu</code> 도 정의되지 않는다.
     */
    public void testOnlyTwoDimensionalContext() {
        PageFixture f = page("");
        Check.equal("true", js(f, "String(cv.getContext('2d') === ctx && ctx.canvas === cv)"), "the same context every time, and it knows its canvas");
        Check.equal("[object CanvasRenderingContext2D]", js(f, "Object.prototype.toString.call(ctx)"), "interface name");
        Check.equal("true,true,true,true,true,true", js(f, "[cv.getContext('webgl'), cv.getContext('webgl2'), cv.getContext('experimental-webgl'),"
                + " cv.getContext('webgpu'), cv.getContext('bitmaprenderer'), cv.getContext('3d')].map(function (c) { return c === null; }).join()"),
                "every other context type is null (also after a 2D context exists)");
        PageFixture g = new PageFixture().load("<canvas id=c></canvas>");
        Check.equal("true,true,true", js(g, "var c2 = document.getElementById('c'); [c2.getContext('webgl') === null, c2.getContext('webgl2') === null,"
                + " c2.getContext('webgpu') === null].join()"), "null also before any 2D context exists");
        Check.equal("undefined,undefined,undefined,undefined,undefined", js(g, "[typeof WebGLRenderingContext, typeof WebGL2RenderingContext,"
                + " typeof GPU, typeof navigator.gpu, typeof GPUCanvasContext].join()"), "WebGL and WebGPU objects do not exist");
        Check.equal("TypeError", js(g, "try { document.getElementById('c').getContext() } catch (e) { e.name }"), "the context type is required");
        Check.equal("function,function", js(g, "[typeof CanvasRenderingContext2D, typeof CanvasGradient].join()"), "interface objects");
        Check.equal("TypeError", js(g, "try { new CanvasRenderingContext2D() } catch (e) { e.name }"), "the interface cannot be constructed");
    }

    /**
     * Colors: a color that cannot be parsed is ignored, opaque colors read back as <code>#rrggbb</code>, translucent ones as
     * <code>rgba()</code>, and <code>currentcolor</code> is the color of the element.
     * <p>
     * 색: 해석할 수 없는 색은 무시하고, 불투명한 색은 <code>#rrggbb</code>, 반투명한 색은 <code>rgba()</code> 로 읽히며, <code>currentcolor</code> 는 요소의
     * 색이다.
     */
    public void testStyleColors() {
        PageFixture f = page("");
        Check.equal("#000000,#000000", js(f, "[ctx.fillStyle, ctx.strokeStyle].join()"), "defaults");
        Check.equal("#ff0000", js(f, "ctx.fillStyle = 'red'; ctx.fillStyle"), "named color");
        Check.equal("#00ff00", js(f, "ctx.fillStyle = '#0F0'; ctx.fillStyle"), "short hex, lower case");
        Check.equal("#336699", js(f, "ctx.fillStyle = 'rgb(51, 102, 153)'; ctx.fillStyle"), "rgb()");
        Check.equal("rgba(255, 0, 0, 0.5)", js(f, "ctx.fillStyle = 'rgba(255, 0, 0, 0.5)'; ctx.fillStyle"), "translucent");
        Check.equal("rgba(0, 0, 0, 0)", js(f, "ctx.fillStyle = 'transparent'; ctx.fillStyle"), "transparent");
        Check.equal("#0000ff", js(f, "ctx.fillStyle = 'blue'; ctx.fillStyle = 'not a color'; ctx.fillStyle"), "invalid values are ignored");
        Check.equal("#ff0000", js(f, "ctx.strokeStyle = 'hsl(0, 100%, 50%)'; ctx.strokeStyle"), "hsl()");
        Check.equal("#000000", js(f, "ctx.fillStyle = 'currentcolor'; ctx.fillStyle"), "currentcolor without a style resolver is black");
        Check.equal("#123456", js(f, "ctx.fillStyle = '#123456'; ctx.fillStyle = {}; ctx.fillStyle"), "an object that is no color is ignored");
        Check.equal("rgba(0, 0, 0, 0.5)", js(f, "ctx.shadowColor = 'rgba(0,0,0,.5)'; ctx.shadowColor"), "shadowColor reads back like a style");
    }

    /**
     * Filling an integer rectangle covers exactly its pixels, and the pixel read back is the color that was set.
     * <p>
     * 정수 사각형을 채우면 정확히 그 화소가 칠해지고, 읽어 온 화소는 지정한 색이다.
     */
    public void testFillRect() {
        PageFixture f = page("width=40 height=40");
        Check.equal("0,0,0,0", js(f, "px(10, 10)"), "a new canvas is transparent black");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(10, 10, 20, 20);");
        Check.equal("255,0,0,255", js(f, "px(10, 10)"), "top left pixel of the rectangle");
        Check.equal("255,0,0,255", js(f, "px(29, 29)"), "bottom right pixel of the rectangle");
        Check.equal("0,0,0,0", js(f, "px(30, 30)"), "just outside on the right and below");
        Check.equal("0,0,0,0", js(f, "px(9, 9)"), "just outside on the left and above");
        js(f, "ctx.fillStyle = '#0000ff'; ctx.fillRect(25, 25, -10, -10);");
        Check.equal("0,0,255,255", js(f, "px(15, 15)"), "a negative size fills the rectangle on the other side of the point");
        Check.equal("0,0,255,255", js(f, "px(24, 24)"), "to the corner of the negative rectangle");
        Check.equal("255,0,0,255", js(f, "px(25, 25)"), "outside the negative rectangle the red remains");
        js(f, "ctx.clearRect(12, 12, 4, 4);");
        Check.equal("0,0,0,0", js(f, "px(13, 13)"), "clearRect makes pixels transparent black");
        Check.equal("0,0,255,255", js(f, "px(16, 16)"), "next to the cleared area");
        js(f, "ctx.fillRect(0, 0, 0, 5); ctx.fillRect(0, 0, 5, 0);");
        Check.equal("0,0,0,0", js(f, "px(0, 0)"), "a rectangle without area paints nothing");
        js(f, "ctx.fillStyle = 'rgba(0, 255, 0, 0.5)'; ctx.fillRect(0, 30, 5, 5);");
        Check.equal("0,255,0,128", js(f, "px(2, 32)"), "a translucent style on transparent pixels keeps its alpha (0.5 of 255)");
        js(f, "ctx.fillRect(0, 30, 5, 5);");
        Check.equal("0,255,0,192", js(f, "px(2, 32)"), "painting twice composites the second layer over the first (1 - (1 - 128/255)^2 of 255)");
    }

    /**
     * Arguments that are NaN or infinite make drawing methods do nothing; too few arguments are a TypeError.
     * <p>
     * NaN 이나 무한대 인수는 그리기 메서드가 아무것도 하지 않게 하고, 인수가 모자라면 TypeError 이다.
     */
    public void testInvalidArguments() {
        PageFixture f = page("width=20 height=20");
        js(f, "ctx.fillRect(NaN, 0, 5, 5); ctx.fillRect(0, 0, Infinity, 5); ctx.fillRect(0, 0, 5, -Infinity); ctx.fillRect(undefined, 1, 2, 3);");
        Check.equal("0,0,0,0", js(f, "px(2, 2)"), "nothing was drawn");
        Check.equal("TypeError", js(f, "try { ctx.fillRect(1, 2, 3) } catch (e) { e.name }"), "missing arguments");
        Check.equal("IndexSizeError", js(f, "try { ctx.arc(5, 5, -1, 0, 1) } catch (e) { e.name }"), "negative radius");
        Check.equal("undefined", js(f, "String(ctx.arc(5, 5, Infinity, 0, 1))"), "a non-finite radius is ignored before the sign is checked");
        Check.equal("1,1", js(f, "ctx.lineWidth = 0; ctx.lineWidth = -3; ctx.lineWidth = NaN; ctx.lineWidth = Infinity; [ctx.lineWidth, 1].join()"),
                "line width ignores zero, negative and non-finite values");
        Check.equal("source-over", js(f, "ctx.globalCompositeOperation = 'bogus'; ctx.globalCompositeOperation"), "unknown operator ignored");
        Check.equal("1", js(f, "ctx.globalAlpha = 2; ctx.globalAlpha = -1; ctx.globalAlpha = NaN; String(ctx.globalAlpha)"), "alpha outside 0..1 is ignored");
        Check.equal("0.25", js(f, "ctx.globalAlpha = 0.25; String(ctx.globalAlpha)"), "alpha inside 0..1 is stored");
        Check.equal("butt,miter,10", js(f, "ctx.lineCap = 'bogus'; ctx.lineJoin = 'bogus'; ctx.miterLimit = -1; [ctx.lineCap, ctx.lineJoin, ctx.miterLimit].join()"),
                "unknown cap, join and miter limit are ignored");
        Check.equal("round,bevel,3", js(f, "ctx.lineCap = 'round'; ctx.lineJoin = 'bevel'; ctx.miterLimit = 3; [ctx.lineCap, ctx.lineJoin, ctx.miterLimit].join()"),
                "valid values are stored");
        Check.equal("start,alphabetic,inherit", js(f, "ctx.textAlign = 'bogus'; ctx.textBaseline = 'bogus'; ctx.direction = 'bogus';"
                + " [ctx.textAlign, ctx.textBaseline, ctx.direction].join()"), "unknown text attributes are ignored");
        Check.equal("TypeError", js(f, "try { ctx.createLinearGradient(NaN, 0, 1, 1) } catch (e) { e.name }"), "a gradient needs finite numbers");
        Check.equal("IndexSizeError", js(f, "try { ctx.createRadialGradient(0, 0, -1, 0, 0, 1) } catch (e) { e.name }"), "negative gradient radius");
    }

    /**
     * <code>save()</code> and <code>restore()</code> keep the styles, the line and text attributes, the matrix and the clip, but not the path;
     * <code>restore()</code> on an empty stack does nothing.
     * <p>
     * <code>save()</code> 와 <code>restore()</code> 는 스타일, 선과 텍스트 속성, 행렬, 클립을 보관하지만 경로는 보관하지 않는다. 빈 스택에서
     * <code>restore()</code> 는 아무것도 하지 않는다.
     */
    public void testSaveRestore() {
        PageFixture f = page("width=30 height=30");
        Check.equal("#ff0000,3,round,20px serif", js(f, "ctx.fillStyle = 'red'; ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.font = '20px serif'; ctx.save();"
                + " ctx.fillStyle = 'blue'; ctx.lineWidth = 9; ctx.lineCap = 'square'; ctx.font = '8px monospace'; ctx.restore();"
                + " [ctx.fillStyle, ctx.lineWidth, ctx.lineCap, ctx.font].join()"), "attributes return to the saved values");
        js(f, "ctx.restore(); ctx.restore(); ctx.fillStyle = '#00ff00'; ctx.save(); ctx.translate(10, 0); ctx.restore(); ctx.fillRect(0, 0, 4, 4);");
        Check.equal("0,255,0,255", js(f, "px(1, 1)"), "restore on an empty stack does nothing, and the matrix was restored");
        js(f, "ctx.save(); ctx.beginPath(); ctx.rect(20, 20, 5, 5); ctx.restore(); ctx.fillStyle = '#ff0000'; ctx.fill();");
        Check.equal("255,0,0,255", js(f, "px(22, 22)"), "the path is not part of the saved state");
        js(f, "ctx.clearRect(0, 0, 30, 30); ctx.save(); ctx.beginPath(); ctx.rect(0, 0, 5, 5); ctx.clip(); ctx.restore(); ctx.fillRect(10, 10, 5, 5);");
        Check.equal("255,0,0,255", js(f, "px(11, 11)"), "restore removes the clip");
    }

    /**
     * Transformations apply to what is drawn after them, in the order they are called, and <code>getTransform()</code> shows the matrix.
     * <p>
     * 변환은 호출한 순서대로 그 뒤에 그리는 것에 적용되며 <code>getTransform()</code> 이 행렬을 보여 준다.
     */
    public void testTransform() {
        PageFixture f = page("width=60 height=60");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.translate(10, 20); ctx.scale(2, 3); ctx.fillRect(0, 0, 5, 5);");
        Check.equal("255,0,0,255", js(f, "px(10, 20)"), "translate then scale: the origin moved");
        Check.equal("255,0,0,255", js(f, "px(19, 34)"), "the rectangle is 10 wide and 15 tall");
        Check.equal("0,0,0,0", js(f, "px(20, 20)"), "right of the scaled rectangle");
        Check.equal("0,0,0,0", js(f, "px(10, 35)"), "below the scaled rectangle");
        Check.equal("2,0,0,3,10,20", js(f, "var m = ctx.getTransform(); [m.a, m.b, m.c, m.d, m.e, m.f].join()"), "getTransform");
        js(f, "ctx.resetTransform(); ctx.clearRect(0, 0, 60, 60); ctx.setTransform(1, 0, 0, 1, 30, 30); ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 4, 4);");
        Check.equal("0,0,255,255", js(f, "px(31, 31)"), "setTransform replaces the matrix");
        js(f, "ctx.setTransform(); ctx.clearRect(0, 0, 60, 60); ctx.translate(30, 30); ctx.rotate(Math.PI / 2); ctx.fillRect(0, 0, 10, 4);");
        Check.equal("0,0,255,255", js(f, "px(28, 35)"), "a quarter turn clockwise maps the x axis to the y axis");
        Check.equal("0,0,0,0", js(f, "px(35, 31)"), "and nothing is left on the original axis");
        js(f, "ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.clearRect(0, 0, 60, 60); ctx.transform(1, 0, 0, 1, 5, 5); ctx.transform(2, 0, 0, 2, 0, 0); ctx.fillRect(0, 0, 3, 3);");
        Check.equal("0,0,255,255", js(f, "px(10, 10)"), "transform multiplies the current matrix: translate by 5, then scale by 2");
        Check.equal("0,0,0,0", js(f, "px(11, 11)"), "the 3 by 3 rectangle covers 6 by 6 pixels from (5, 5)");
        js(f, "ctx.setTransform(NaN, 0, 0, 1, 0, 0); ctx.scale(Infinity, 1);");
        Check.equal("2,0,0,2,5,5", js(f, "var m2 = ctx.getTransform(); [m2.a, m2.b, m2.c, m2.d, m2.e, m2.f].join()"), "non-finite arguments are ignored");
        js(f, "ctx.setTransform({a: 1, b: 0, c: 0, d: 1, e: 7, f: 8});");
        Check.equal("7,8", js(f, "var m3 = ctx.getTransform(); [m3.e, m3.f].join()"), "setTransform accepts a matrix object");
    }

    /**
     * A translucent <code>globalAlpha</code> scales the alpha of everything that is drawn.
     * <p>
     * 반투명한 <code>globalAlpha</code> 는 그려지는 모든 것의 알파에 배율을 곱한다.
     */
    public void testGlobalAlpha() {
        PageFixture f = page("width=10 height=10");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.globalAlpha = 0.5; ctx.fillRect(0, 0, 10, 10);");
        Check.equal("255,0,0,128", js(f, "px(5, 5)"), "half alpha");
        js(f, "ctx.globalAlpha = 0; ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 10, 10);");
        Check.equal("255,0,0,128", js(f, "px(5, 5)"), "alpha zero paints nothing");
        js(f, "ctx.globalAlpha = 1; ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 5, 10);");
        Check.equal("0,0,255,255", js(f, "px(2, 5)"), "alpha one is opaque");
    }

    /**
     * Drawing is reported to the page once for every change that follows a snapshot, however many drawing calls it takes.
     * <p>
     * 그리기는 스냅샷 뒤의 변경마다 그리기 호출이 몇 번이든 한 번만 페이지에 알린다.
     */
    public void testChangeNotification() {
        PageFixture f = page("width=10 height=10");
        int before = f.env.canvasChanges;
        js(f, "ctx.fillRect(0, 0, 1, 1); ctx.fillRect(2, 2, 1, 1); ctx.fillRect(4, 4, 1, 1);");
        Check.equal(before + 1, f.env.canvasChanges, "three drawing calls make one notification until the renderer reads the picture");
        Check.equal(true, f.scripts.canvasSource() != null, "the renderer reads through the canvas source");
        Check.equal(true, f.scripts.canvasSource().getCanvasImage(f.document.getElementById("c")) != null, "snapshot");
        js(f, "ctx.fillRect(6, 6, 1, 1);");
        Check.equal(before + 2, f.env.canvasChanges, "after a snapshot the next change is notified again");
        Check.equal(true, f.scripts.canvasSource().getCanvasImage(f.document.getElementById("c"))
                == f.scripts.canvasSource().getCanvasImage(f.document.getElementById("c")), "an unchanged canvas gives the same snapshot");
    }
}
