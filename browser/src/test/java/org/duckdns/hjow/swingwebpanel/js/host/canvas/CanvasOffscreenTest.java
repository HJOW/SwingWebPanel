package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>OffscreenCanvas</code>, <code>ImageBitmap</code> and <code>createImageBitmap()</code> in a page (plan item P20-05-b): the
 * constructor and its argument checks, the 2D context of an offscreen canvas (the same engine as the canvas element, pixel by pixel), the size
 * attributes, <code>transferToImageBitmap()</code>, <code>convertToBlob()</code>, drawing offscreen canvases and bitmaps on a canvas, the
 * placeholder canvas of <code>transferControlToOffscreen()</code>, and every source type and every option of <code>createImageBitmap()</code> with
 * the pixels of the result. The pictures are checked on axis-aligned integer rectangles, where antialiasing does not change any value.
 * <p>
 * 페이지에서 <code>OffscreenCanvas</code>, <code>ImageBitmap</code>, <code>createImageBitmap()</code> 의 시험(계획 P20-05-b): 생성자와 인수 검사, offscreen
 * 캔버스의 2D 컨텍스트(canvas 요소와 같은 엔진, 화소별), 크기 속성, <code>transferToImageBitmap()</code>, <code>convertToBlob()</code>, offscreen 캔버스와
 * 비트맵을 캔버스에 그리기, <code>transferControlToOffscreen()</code> 의 자리표시 캔버스, <code>createImageBitmap()</code> 의 모든 원본 형식과 옵션을 결과
 * 화소와 함께 확인한다. 그림은 안티앨리어싱이 어떤 값도 바꾸지 않는 축에 나란한 정수 사각형에서 확인한다.
 */
public class CanvasOffscreenTest {

    /**
     * The helper script: <code>opx(c, x, y)</code> reads a pixel of a 2D context as <code>r,g,b,a</code>.
     * <p>
     * 도우미 스크립트: <code>opx(c, x, y)</code> 는 2D 컨텍스트의 화소를 <code>r,g,b,a</code> 로 읽는다.
     */
    private static final String HELPERS = "function opx(c, x, y) { var d = c.getImageData(x, y, 1, 1).data; return d[0] + ',' + d[1] + ',' + d[2] + ',' + d[3]; }"
            + "function bpx(b, x, y) { var oc = new OffscreenCanvas(b.width, b.height); var c = oc.getContext('2d'); c.drawImage(b, 0, 0); return opx(c, x, y); }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasOffscreenTest() {
    }

    /**
     * Loads a page with a canvas and the helpers.
     * <p>
     * 캔버스와 도우미가 있는 페이지를 로드한다.
     *
     * @param canvasAttributes the attributes of the canvas / 캔버스의 속성
     * @return the fixture / 시험 도구
     */
    private static PageFixture page(String canvasAttributes) {
        PageFixture f = CanvasBasicsTest.page(canvasAttributes);
        f.scripts.evaluate(HELPERS, "offscreen-helpers");
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
    private static String js(PageFixture f, String code) {
        return f.scripts.evaluate(code, "t");
    }

    /**
     * The constructor takes two numbers by the rules of WebIDL <code>[EnforceRange]</code>; the object is an <code>EventTarget</code> and an interface
     * object of its own, and the sizes read back.
     * <p>
     * 생성자는 WebIDL <code>[EnforceRange]</code> 규칙대로 숫자 둘을 받는다. 객체는 <code>EventTarget</code> 이고 자기 인터페이스 객체가 있으며 크기가 읽힌다.
     */
    public void testConstructor() {
        PageFixture f = page("");
        Check.equal("function,function,function", js(f, "[typeof OffscreenCanvas, typeof ImageBitmap, typeof createImageBitmap].join()"), "the interfaces exist");
        Check.equal("20,10", js(f, "var oc = new OffscreenCanvas(20, 10); [oc.width, oc.height].join()"), "the size reads back");
        Check.equal("true,true,true", js(f, "[oc instanceof OffscreenCanvas, oc instanceof EventTarget, Object.prototype.toString.call(oc) === '[object OffscreenCanvas]']"
                + ".join()"), "an OffscreenCanvas is an EventTarget with its own class name");
        Check.equal("3,2", js(f, "var o2 = new OffscreenCanvas(3.9, '2'); [o2.width, o2.height].join()"), "a fraction is cut off and a string is converted");
        Check.equal("TypeError", js(f, "try { new OffscreenCanvas(1) } catch (e) { e.name }"), "two arguments are required");
        Check.equal("TypeError", js(f, "try { new OffscreenCanvas(-1, 5) } catch (e) { e.name }"), "a negative size is out of range");
        Check.equal("TypeError", js(f, "try { new OffscreenCanvas(NaN, 5) } catch (e) { e.name }"), "NaN is not a number of the range");
        Check.equal("TypeError", js(f, "try { new OffscreenCanvas(Infinity, 5) } catch (e) { e.name }"), "neither is infinity");
        Check.equal("TypeError", js(f, "try { OffscreenCanvas(1, 1) } catch (e) { e.name }"), "new is required");
        Check.equal("0,0", js(f, "var z = new OffscreenCanvas(0, 0); [z.width, z.height].join()"), "a size of zero is allowed");
        Check.equal("true", js(f, "var got = 0; oc.addEventListener('contextlost', function () { got++; }); String(typeof oc.oncontextlost === 'object' && typeof oc.oncontextrestored"
                + " === 'object')"), "the event handler attributes exist");
    }

    /**
     * The 2D context of an offscreen canvas has all members of the 2D context of an element and draws the same: pixels, state, transformation,
     * <code>canvas</code> returns the offscreen canvas, the other context types are null and an unknown one is a <code>TypeError</code>.
     * <p>
     * offscreen 캔버스의 2D 컨텍스트는 요소의 2D 컨텍스트와 같은 멤버를 가지며 똑같이 그린다: 화소, 상태, 변환. <code>canvas</code> 는 offscreen 캔버스를 반환하고,
     * 다른 컨텍스트 형식은 null 이며 모르는 형식은 <code>TypeError</code> 이다.
     */
    public void testContext() {
        PageFixture f = page("");
        js(f, "var oc = new OffscreenCanvas(40, 30); var c = oc.getContext('2d');");
        Check.equal("[object OffscreenCanvasRenderingContext2D]", js(f, "Object.prototype.toString.call(c)"), "the interface name of the context");
        Check.equal("true,true", js(f, "[oc.getContext('2d') === c, c.canvas === oc].join()"), "the same context every time, and it knows its canvas");
        Check.equal("true", js(f, "String(c instanceof OffscreenCanvasRenderingContext2D && !(c instanceof CanvasRenderingContext2D))"),
                "it is not a CanvasRenderingContext2D");
        Check.equal("true,true,true,true", js(f, "[oc.getContext('bitmaprenderer'), oc.getContext('webgl'), oc.getContext('webgl2'), oc.getContext('webgpu')]"
                + ".map(function (x) { return x === null; }).join()"), "WebGL, WebGPU and bitmaprenderer are not available");
        Check.equal("TypeError", js(f, "try { oc.getContext('bogus') } catch (e) { e.name }"), "an unknown type is not in the enumeration");
        Check.equal("TypeError", js(f, "try { oc.getContext() } catch (e) { e.name }"), "the type is required");
        js(f, "c.fillStyle = '#ff0000'; c.fillRect(5, 5, 10, 10); c.fillStyle = 'rgba(0, 0, 255, 0.5)'; c.fillRect(20, 5, 10, 10);");
        Check.equal("255,0,0,255", js(f, "opx(c, 5, 5)"), "the first pixel of the rectangle");
        Check.equal("255,0,0,255", js(f, "opx(c, 14, 14)"), "the last pixel of the rectangle");
        Check.equal("0,0,0,0", js(f, "opx(c, 15, 15)"), "just outside");
        Check.equal("0,0,255,128", js(f, "opx(c, 25, 10)"), "translucent blue");
        js(f, "c.save(); c.translate(0, 15); c.fillStyle = '#00ff00'; c.fillRect(0, 0, 5, 5); c.restore();");
        Check.equal("0,255,0,255", js(f, "opx(c, 2, 17)"), "a transformed rectangle");
        Check.equal("#ff0000", js(f, "c.fillStyle = '#f00'; c.fillStyle"), "styles read back");
        Check.equal("10px sans-serif", js(f, "c.font"), "the default font of an offscreen canvas is 10px sans-serif");
        js(f, "c.font = '2em serif'");
        Check.equal("20px serif", js(f, "c.font"), "an em size refers to the default 10px font of the offscreen canvas");
        Check.equal("true", js(f, "var m = c.measureText('abc'); String(m.width > 0)"), "text can be measured");
        Check.equal("true,false,false", js(f, "var a = oc.getContext('2d', {}).getContextAttributes(); [a.alpha, a.willReadFrequently, a.desynchronized].join()"),
                "the context attributes of a default context");
    }

    /**
     * The context settings of the first <code>getContext</code> call: <code>alpha: false</code> starts opaque black, the two hints are reported
     * back, an unknown <code>colorSpace</code> is a <code>TypeError</code> that does not create the context, and later calls ignore the settings.
     * <p>
     * 첫 <code>getContext</code> 호출의 컨텍스트 설정: <code>alpha: false</code> 는 불투명한 검정으로 시작하고, 두 힌트는 그대로 보고되며, 모르는
     * <code>colorSpace</code> 는 컨텍스트를 만들지 않는 <code>TypeError</code> 이고, 이후 호출은 설정을 무시한다.
     */
    public void testContextSettings() {
        PageFixture f = page("");
        js(f, "var a = new OffscreenCanvas(4, 4); var ca = a.getContext('2d', { alpha: false, willReadFrequently: true, desynchronized: true });");
        Check.equal("0,0,0,255", js(f, "opx(ca, 1, 1)"), "alpha: false starts opaque black");
        Check.equal("false,true,true,srgb", js(f, "var at = ca.getContextAttributes(); [at.alpha, at.willReadFrequently, at.desynchronized, at.colorSpace].join()"),
                "the settings are reported back");
        js(f, "a.getContext('2d', { alpha: true });");
        Check.equal("false", js(f, "String(ca.getContextAttributes().alpha)"), "the second call does not change the settings");
        js(f, "var b = new OffscreenCanvas(4, 4);");
        Check.equal("TypeError", js(f, "try { b.getContext('2d', { colorSpace: 'rec2020' }) } catch (e) { e.name }"), "an unknown color space");
        Check.equal("true", js(f, "var cb = b.getContext('2d', { colorSpace: 'display-p3' }); String(cb !== null)"), "display-p3 is accepted (the bitmap stays sRGB)");
        PageFixture g = page("");
        Check.equal("TypeError", js(g, "try { document.createElement('canvas').getContext('2d', { colorSpace: 'x' }) } catch (e) { e.name }"),
                "the canvas element checks the settings as well");
        Check.equal("true", js(g, "var c2 = document.createElement('canvas'); var x2 = c2.getContext('2d', { willReadFrequently: true });"
                + " String(x2.getContextAttributes().willReadFrequently)"), "the canvas element reports the hints");
    }

    /**
     * Setting <code>width</code> or <code>height</code> replaces the bitmap by a transparent one, also with the same value, and resets the state of
     * the context, which stays the same object.
     * <p>
     * <code>width</code> 나 <code>height</code> 를 지정하면(같은 값이라도) 비트맵이 투명한 것으로 바뀌고 컨텍스트의 상태가 초기화된다. 컨텍스트는 같은 객체이다.
     */
    public void testResize() {
        PageFixture f = page("");
        js(f, "var oc = new OffscreenCanvas(10, 10); var c = oc.getContext('2d'); c.fillStyle = '#ff0000'; c.lineWidth = 5; c.fillRect(0, 0, 10, 10);");
        Check.equal("255,0,0,255", js(f, "opx(c, 5, 5)"), "drawn");
        js(f, "oc.width = 10;");
        Check.equal("0,0,0,0", js(f, "opx(c, 5, 5)"), "setting the width to the same value clears the bitmap");
        Check.equal("#000000,1", js(f, "[c.fillStyle, c.lineWidth].join()"), "and resets the state of the context");
        Check.equal("true", js(f, "String(oc.getContext('2d') === c)"), "the context is the same object");
        js(f, "oc.height = 20; oc.width = 15;");
        Check.equal("15,20", js(f, "[oc.width, oc.height].join()"), "the new size");
        Check.equal("15,20", js(f, "var d = c.getImageData(0, 0, 15, 20); [d.width, d.height].join()"), "the bitmap has the new size");
        Check.equal("TypeError", js(f, "try { oc.width = -3 } catch (e) { e.name }"), "a negative width is out of range");
        Check.equal("15", js(f, "oc.width"), "and nothing changed");
        js(f, "oc.width = 20000; oc.height = 20000; c.fillRect(0, 0, 10, 10); c.drawImage(oc, 0, 0);");
        Check.equal("20000,20000", js(f, "[oc.width, oc.height].join()"), "a huge size reads back");
        Check.equal("InvalidStateError", js(f, "try { c.getImageData(0, 0, 1, 1) } catch (e) { e.name }"), "and its bitmap is unusable but harmless");
    }

    /**
     * An offscreen canvas and a bitmap can be drawn on a canvas and on another offscreen canvas, also with the 9-argument form, and a pattern can be
     * made of them; a canvas without area and a detached image source are an <code>InvalidStateError</code>.
     * <p>
     * offscreen 캔버스와 비트맵은 캔버스와 다른 offscreen 캔버스에 (9 인수 형식으로도) 그릴 수 있고 패턴도 만들 수 있다. 면적이 없는 캔버스와 떼어 낸 이미지 원본은
     * <code>InvalidStateError</code> 이다.
     */
    public void testDrawOffscreen() {
        PageFixture f = page("width=40 height=40");
        js(f, "var oc = new OffscreenCanvas(10, 10); var c = oc.getContext('2d'); c.fillStyle = '#ff0000'; c.fillRect(0, 0, 5, 10); c.fillStyle = '#0000ff';"
                + " c.fillRect(5, 0, 5, 10);");
        js(f, "ctx.drawImage(oc, 20, 20);");
        Check.equal("255,0,0,255", js(f, "px(20, 20)"), "the left half is red on the canvas");
        Check.equal("0,0,255,255", js(f, "px(29, 29)"), "the right half is blue on the canvas");
        Check.equal("0,0,0,0", js(f, "px(19, 19)"), "nothing outside");
        js(f, "ctx.imageSmoothingEnabled = false; ctx.drawImage(oc, 0, 0, 5, 10, 0, 0, 10, 20);");
        Check.equal("255,0,0,255", js(f, "px(9, 19)"), "the 9-argument form scales the left half up");
        Check.equal("0,0,0,0", js(f, "px(10, 5)"), "and nothing else is drawn");
        js(f, "var oc2 = new OffscreenCanvas(10, 10); var c2 = oc2.getContext('2d'); c2.drawImage(oc, 0, 0);");
        Check.equal("255,0,0,255,0,0,255,255", js(f, "[opx(c2, 1, 1), opx(c2, 8, 1)].join()"), "an offscreen canvas can be drawn on another one");
        js(f, "c2.clearRect(0, 0, 10, 10); c2.drawImage(oc2, 0, 0);");
        Check.equal("0,0,0,0", js(f, "opx(c2, 1, 1)"), "drawing a canvas on itself uses a copy");
        js(f, "var pat = ctx.createPattern(oc, 'repeat'); ctx.fillStyle = pat; ctx.fillRect(0, 0, 4, 4);");
        Check.equal("255,0,0,255", js(f, "px(1, 1)"), "a pattern of an offscreen canvas");
        Check.equal("InvalidStateError", js(f, "try { ctx.drawImage(new OffscreenCanvas(0, 5), 0, 0) } catch (e) { e.name }"), "a source without area");
    }

    /**
     * <code>transferToImageBitmap()</code> gives the picture as an <code>ImageBitmap</code> and leaves a blank bitmap while the context keeps its state;
     * it needs a context, a closed bitmap has the size 0 and cannot be drawn.
     * <p>
     * <code>transferToImageBitmap()</code> 은 그림을 <code>ImageBitmap</code> 으로 주고 컨텍스트의 상태를 유지한 채 빈 비트맵을 남긴다. 컨텍스트가 필요하며 닫힌
     * 비트맵은 크기가 0 이고 그릴 수 없다.
     */
    public void testTransferToImageBitmap() {
        PageFixture f = page("width=30 height=30");
        js(f, "var oc = new OffscreenCanvas(8, 6);");
        Check.equal("InvalidStateError", js(f, "try { oc.transferToImageBitmap() } catch (e) { e.name }"), "without a context there is nothing to transfer");
        js(f, "var c = oc.getContext('2d'); c.fillStyle = '#00ff00'; c.fillRect(0, 0, 8, 6); var bm = oc.transferToImageBitmap();");
        Check.equal("true,8,6", js(f, "[bm instanceof ImageBitmap, bm.width, bm.height].join()"), "an ImageBitmap of the same size");
        Check.equal("0,0,0,0", js(f, "opx(c, 3, 3)"), "the offscreen canvas is blank afterwards");
        Check.equal("#00ff00", js(f, "c.fillStyle"), "and the state of the context is kept");
        Check.equal("0,255,0,255", js(f, "bpx(bm, 3, 3)"), "the picture is in the bitmap");
        js(f, "c.fillRect(0, 0, 2, 2); var bm2 = oc.transferToImageBitmap();");
        Check.equal("0,255,0,255,0,0,0,0", js(f, "[bpx(bm2, 1, 1), bpx(bm2, 5, 5)].join()"), "a second transfer gives the new picture only");
        js(f, "ctx.drawImage(bm, 10, 10);");
        Check.equal("0,255,0,255", js(f, "px(10, 10)"), "a bitmap is drawn on a canvas");
        js(f, "bm.close();");
        Check.equal("0,0", js(f, "[bm.width, bm.height].join()"), "a closed bitmap has no size");
        Check.equal("InvalidStateError", js(f, "try { ctx.drawImage(bm, 0, 0) } catch (e) { e.name }"), "a closed bitmap cannot be drawn");
        Check.equal("undefined", js(f, "String(bm.close())"), "closing twice does nothing");
        Check.equal("InvalidStateError", js(f, "var z = new OffscreenCanvas(0, 4); z.getContext('2d'); try { z.transferToImageBitmap() } catch (e) { e.name }"),
                "a canvas without pixels cannot give a bitmap");
        Check.equal("opaque", js(f, "var op = new OffscreenCanvas(2, 2); var oc3 = op.getContext('2d', { alpha: false }); op.transferToImageBitmap();"
                + " opx(oc3, 0, 0) === '0,0,0,255' ? 'opaque' : 'not'"), "an opaque canvas is opaque black again after a transfer");
    }

    /**
     * <code>convertToBlob()</code> returns a promise of a PNG or JPEG <code>Blob</code> and rejects for a canvas without pixels; the promise is
     * settled in the call.
     * <p>
     * <code>convertToBlob()</code> 은 PNG 나 JPEG <code>Blob</code> 의 프라미스를 반환하고 화소가 없는 캔버스는 거부한다. 프라미스는 호출 안에서 확정된다.
     */
    public void testConvertToBlob() {
        PageFixture f = page("");
        js(f, "var oc = new OffscreenCanvas(8, 6); var c = oc.getContext('2d'); c.fillStyle = '#ff0000'; c.fillRect(0, 0, 8, 6); var log = [];"
                + " oc.convertToBlob().then(function (b) { log.push(b.type + ':' + (b.size > 20)); }, function (e) { log.push('rej:' + e.name); });");
        f.env.scheduler.advance(0);
        Check.equal("image/png:true", js(f, "log.join()"), "a PNG blob by default");
        js(f, "oc.convertToBlob({ type: 'image/jpeg', quality: 0.5 }).then(function (b) { log.push(b.type); });");
        f.env.scheduler.advance(0);
        Check.equal("image/png:true,image/jpeg", js(f, "log.join()"), "a JPEG blob on request");
        js(f, "oc.convertToBlob({ type: 'image/gif' }).then(function (b) { log.push(b.type); });");
        f.env.scheduler.advance(0);
        Check.equal("image/png:true,image/jpeg,image/png", js(f, "log.join()"), "an unknown type gives PNG");
        js(f, "var zero = new OffscreenCanvas(0, 5); zero.convertToBlob().then(null, function (e) { log.push(e.name); });");
        f.env.scheduler.advance(0);
        Check.equal("image/png:true,image/jpeg,image/png,IndexSizeError", js(f, "log.join()"), "no pixels, no file");
        js(f, "var bad = []; oc.convertToBlob(5).then(null, function (e) { bad.push(e.name); });");
        f.env.scheduler.advance(0);
        Check.equal("TypeError", js(f, "bad.join()"), "options must be a dictionary");
        js(f, "oc.convertToBlob().then(function (b) { return createImageBitmap(b); }).then(function (bm) { log.push('decoded:' + bm.width + 'x' + bm.height"
                + " + ':' + bpx(bm, 2, 2)); });");
        f.env.scheduler.advance(0);
        Check.equal("decoded:8x6:255,0,0,255", js(f, "log[log.length - 1]"), "the blob is a PNG that createImageBitmap decodes back to the same pixels");
    }

    /**
     * A canvas element that gave its control to an offscreen canvas has no context any more and shows what the offscreen canvas draws; the control
     * can be given once and only before a context exists.
     * <p>
     * 제어를 offscreen 캔버스에 넘긴 canvas 요소는 더 이상 컨텍스트를 가질 수 없고 offscreen 캔버스가 그리는 것을 보여 준다. 제어는 한 번만, 컨텍스트가 생기기 전에만
     * 넘길 수 있다.
     */
    public void testTransferControlToOffscreen() {
        PageFixture f = page("width=12 height=8");
        js(f, "var pc = document.createElement('canvas'); pc.width = 12; pc.height = 8; var off = pc.transferControlToOffscreen();");
        Check.equal("true,12,8", js(f, "[off instanceof OffscreenCanvas, off.width, off.height].join()"), "an offscreen canvas of the size of the attributes");
        Check.equal("InvalidStateError", js(f, "try { pc.getContext('2d') } catch (e) { e.name }"), "the placeholder has no context of its own");
        Check.equal("InvalidStateError", js(f, "try { pc.getContext('webgl') } catch (e) { e.name }"), "not even a type that is unsupported");
        Check.equal("InvalidStateError", js(f, "try { pc.transferControlToOffscreen() } catch (e) { e.name }"), "the control is given only once");
        Check.equal("InvalidStateError", js(f, "try { cv.getContext('2d'); cv.transferControlToOffscreen() } catch (e) { e.name }"),
                "a canvas that has a context cannot give its control");
        int changes = f.env.canvasChanges;
        js(f, "var oc = off.getContext('2d'); oc.fillStyle = '#ff0000'; oc.fillRect(0, 0, 6, 8);");
        Check.equal(true, f.env.canvasChanges > changes, "drawing on the offscreen canvas tells the page that the placeholder changed");
        Check.equal("true,true", js(f, "[oc.canvas === off, off.getContext('2d') === oc].join()"), "the context of the offscreen canvas");
        js(f, "ctx.drawImage(pc, 0, 0);");
        Check.equal("255,0,0,255,0,0,0,0", js(f, "[px(2, 2), px(9, 2)].join()"), "the placeholder element shows the picture (read back through drawImage)");
        Check.equal(true, js(f, "pc.toDataURL()").startsWith("data:image/png;base64,"), "and can be exported");
        js(f, "pc.width = 40; pc.height = 40;");
        Check.equal("255,0,0,255", js(f, "opx(oc, 2, 2)"), "the size attributes of a placeholder do not resize the bitmap");
        Check.equal("12,8", js(f, "[off.width, off.height].join()"), "the offscreen canvas keeps its size");
    }

    /**
     * <code>createImageBitmap()</code> from a canvas: the whole picture, a crop (also with pixels outside the source, which are transparent), a
     * negative crop rectangle, a size, a size by one dimension, <code>flipY</code> and the quality <code>pixelated</code>.
     * <p>
     * 캔버스를 원본으로 하는 <code>createImageBitmap()</code>: 전체 그림, 자르기(원본 밖의 화소는 투명), 음수 자르기 사각형, 크기, 한 쪽만 정한 크기,
     * <code>flipY</code>, 품질 <code>pixelated</code>.
     */
    public void testCreateFromCanvas() {
        PageFixture f = page("width=8 height=4");
        // 캔버스의 위쪽 절반은 빨강, 아래쪽 절반은 파랑이고 왼쪽 위 화소는 초록이다.
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 8, 2); ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 2, 8, 2); ctx.fillStyle = '#00ff00'; ctx.fillRect(0, 0, 1, 1);"
                + " var log = [];");
        js(f, "createImageBitmap(cv).then(function (b) { log.push([b.width, b.height, bpx(b, 0, 0), bpx(b, 4, 0), bpx(b, 4, 3)].join(' ')); });");
        Check.equal("8 4 0,255,0,255 255,0,0,255 0,0,255,255", js(f, "log[0]"), "the whole picture");
        js(f, "createImageBitmap(cv, 2, 1, 4, 2).then(function (b) { log.push([b.width, b.height, bpx(b, 0, 0), bpx(b, 0, 1)].join(' ')); });");
        Check.equal("4 2 255,0,0,255 0,0,255,255", js(f, "log[1]"), "a crop");
        js(f, "createImageBitmap(cv, -2, 0, 4, 1).then(function (b) { log.push([b.width, b.height, bpx(b, 1, 0), bpx(b, 2, 0)].join(' ')); });");
        Check.equal("4 1 0,0,0,0 0,255,0,255", js(f, "log[2]"), "pixels outside the source are transparent black");
        js(f, "createImageBitmap(cv, 4, 2, -4, -2).then(function (b) { log.push([b.width, b.height, bpx(b, 0, 0), bpx(b, 3, 1)].join(' ')); });");
        Check.equal("4 2 0,255,0,255 255,0,0,255", js(f, "log[3]"), "a negative width and height put the rectangle left of and above the point");
        js(f, "createImageBitmap(cv, { resizeWidth: 16, resizeHeight: 8, resizeQuality: 'pixelated' }).then(function (b) { log.push([b.width, b.height,"
                + " bpx(b, 1, 1), bpx(b, 2, 1), bpx(b, 15, 7)].join(' ')); });");
        Check.equal("16 8 0,255,0,255 255,0,0,255 0,0,255,255", js(f, "log[4]"), "resized with the nearest pixel");
        js(f, "createImageBitmap(cv, { resizeWidth: 4, resizeQuality: 'pixelated' }).then(function (b) { log.push(b.width + 'x' + b.height); });");
        Check.equal("4x2", js(f, "log[5]"), "one resize dimension gives the other by the aspect ratio");
        js(f, "createImageBitmap(cv, { resizeHeight: 3, resizeQuality: 'pixelated' }).then(function (b) { log.push(b.width + 'x' + b.height); });");
        Check.equal("6x3", js(f, "log[6]"), "the derived dimension is rounded up");
        js(f, "createImageBitmap(cv, { imageOrientation: 'flipY' }).then(function (b) { log.push([bpx(b, 0, 3), bpx(b, 0, 0)].join(' ')); });");
        Check.equal("0,255,0,255 0,0,255,255", js(f, "log[7]"), "flipY turns the picture upside down");
        js(f, "createImageBitmap(cv, 0, 0, 4, 4, { imageOrientation: 'flipY', resizeWidth: 2, resizeHeight: 2, resizeQuality: 'pixelated' }).then(function (b) {"
                + " log.push([b.width, b.height, bpx(b, 0, 0), bpx(b, 0, 1)].join(' ')); });");
        Check.equal("2 2 0,0,255,255 255,0,0,255", js(f, "log[8]"), "crop, resize and flip together");
    }

    /**
     * <code>createImageBitmap()</code> takes a copy: later changes of the source do not change the bitmap; sources of the other types are an
     * <code>ImageData</code>, an <code>ImageBitmap</code> and a <code>Blob</code> (PNG file made by the canvas).
     * <p>
     * <code>createImageBitmap()</code> 은 복사본을 만든다: 원본이 나중에 바뀌어도 비트맵은 바뀌지 않는다. 다른 원본 형식은 <code>ImageData</code>,
     * <code>ImageBitmap</code>, <code>Blob</code>(캔버스가 만든 PNG 파일)이다.
     */
    public void testCreateFromOtherSources() {
        PageFixture f = page("width=4 height=4");
        js(f, "ctx.fillStyle = '#ff0000'; ctx.fillRect(0, 0, 4, 4); var log = []; var kept; createImageBitmap(cv).then(function (b) { kept = b; });");
        js(f, "ctx.fillStyle = '#0000ff'; ctx.fillRect(0, 0, 4, 4);");
        Check.equal("255,0,0,255", js(f, "bpx(kept, 2, 2)"), "the bitmap is a copy of the picture at the time of the call");
        js(f, "var id = new ImageData(new Uint8ClampedArray([10, 20, 30, 255, 40, 50, 60, 128]), 2, 1);"
                + " createImageBitmap(id).then(function (b) { log.push([b.width, b.height, bpx(b, 0, 0), bpx(b, 1, 0)].join(' ')); });");
        Check.equal("2 1 10,20,30,255 40,50,60,128", js(f, "log[0]"), "ImageData");
        js(f, "id.data[0] = 99; createImageBitmap(kept, 1, 1, 2, 2).then(function (b) { log.push([b.width, b.height, bpx(b, 0, 0)].join(' ')); });");
        Check.equal("2 2 255,0,0,255", js(f, "log[1]"), "an ImageBitmap, with a crop");
        js(f, "var png; cv.toBlob(function (b) { png = b; });");
        f.env.scheduler.advance(0);
        js(f, "createImageBitmap(png).then(function (b) { log.push([b.width, b.height, bpx(b, 3, 3)].join(' ')); });");
        Check.equal("4 4 0,0,255,255", js(f, "log[2]"), "a Blob that holds a PNG");
        js(f, "createImageBitmap(new Blob(['not an image'], { type: 'image/png' })).then(null, function (e) { log.push(e.name); });");
        Check.equal("InvalidStateError", js(f, "log[3]"), "a Blob that is no image");
        js(f, "createImageBitmap(new Blob([])).then(null, function (e) { log.push(e.name); });");
        Check.equal("InvalidStateError", js(f, "log[4]"), "an empty Blob");
        js(f, "var oc = new OffscreenCanvas(3, 2); var c = oc.getContext('2d'); c.fillStyle = '#00ffff'; c.fillRect(0, 0, 3, 2);"
                + " createImageBitmap(oc).then(function (b) { log.push([b.width, b.height, bpx(b, 2, 1)].join(' ')); });");
        Check.equal("3 2 0,255,255,255", js(f, "log[5]"), "an OffscreenCanvas");
    }

    /**
     * Every wrong call of <code>createImageBitmap()</code> is a rejected promise and never an exception: the arity, the crop size 0, the resize size
     * 0, wrong enumeration values, a value that is no image source, a canvas without area, a closed bitmap, a video and an image that is not loaded.
     * <p>
     * <code>createImageBitmap()</code> 의 잘못된 호출은 모두 예외가 아니라 reject 된 프라미스이다: 인수 개수, 자르기 크기 0, 리사이즈 크기 0, 잘못된 열거 값,
     * 이미지 원본이 아닌 값, 면적이 없는 캔버스, 닫힌 비트맵, 비디오, 아직 읽지 않은 이미지.
     */
    public void testCreateRejections() {
        PageFixture f = page("width=4 height=4");
        js(f, "var log = []; function rej(p) { p.then(function () { log.push('ok'); }, function (e) { log.push(e.name); }); }"
                + " rej(createImageBitmap()); rej(createImageBitmap(cv, 1, 2)); rej(createImageBitmap(cv, 1, 2, 3)); rej(createImageBitmap(cv, 0, 0, 0, 4));"
                + " rej(createImageBitmap(cv, 0, 0, 4, 0)); rej(createImageBitmap(cv, { resizeWidth: 0 })); rej(createImageBitmap(cv, { resizeHeight: 0 }));"
                + " rej(createImageBitmap(cv, { imageOrientation: 'up' })); rej(createImageBitmap(cv, { resizeQuality: 'best' }));"
                + " rej(createImageBitmap(cv, { premultiplyAlpha: 'sure' })); rej(createImageBitmap(cv, { colorSpaceConversion: 'srgb' }));"
                + " rej(createImageBitmap(cv, 5)); rej(createImageBitmap({})); rej(createImageBitmap(42)); rej(createImageBitmap(null));");
        Check.equal("TypeError,TypeError,TypeError,RangeError,RangeError,InvalidStateError,InvalidStateError,TypeError,TypeError,TypeError,TypeError,TypeError,TypeError,"
                + "TypeError,TypeError", js(f, "log.join()"), "argument errors");
        js(f, "log.length = 0; var zero = document.createElement('canvas'); zero.width = 0; rej(createImageBitmap(zero));"
                + " rej(createImageBitmap(new OffscreenCanvas(0, 1))); var closed; createImageBitmap(cv).then(function (b) { closed = b; b.close(); rej(createImageBitmap(b)); });"
                + " var img = document.createElement('img'); rej(createImageBitmap(img)); var vid = document.createElement('video'); rej(createImageBitmap(vid));");
        Check.equal("InvalidStateError,InvalidStateError,InvalidStateError,InvalidStateError,InvalidStateError", js(f, "log.join()"),
                "unusable sources: no area, closed, an image without source, a video");
        js(f, "log.length = 0; rej(createImageBitmap(cv, 0, 0, 100, 100)); rej(createImageBitmap(cv, { resizeWidth: 100000 }));");
        Check.equal("ok,InvalidStateError", js(f, "log.join()"), "a crop larger than the source is fine, a result beyond the limit is rejected");
        Check.equal("true", js(f, "String(createImageBitmap(cv) instanceof Promise)"), "the result is a Promise");
    }

    /**
     * A fake image service that completes at once with the images the test registered.
     * <p>
     * 시험이 등록한 이미지로 곧바로 완료하는 가짜 이미지 서비스.
     */
    private static final class Immediate implements org.duckdns.hjow.swingwebpanel.js.host.ImageRequestSource {
        /**
         * The registered images by URL.
         * <p>
         * URL 별로 등록된 이미지.
         */
        private final java.util.Map<String, org.duckdns.hjow.swingwebpanel.render.ImageResource> images;

        /**
         * Creates the service.
         * <p>
         * 서비스를 만든다.
         *
         * @param images the registered images / 등록된 이미지
         */
        Immediate(java.util.Map<String, org.duckdns.hjow.swingwebpanel.render.ImageResource> images) {
            this.images = images;
        }

        /**
         * Completes the request.
         * <p>
         * 요청을 완료시킨다.
         *
         * @param absoluteUrl the URL / URL
         * @param callback    the receiver / 받는 쪽
         */
        public void request(String absoluteUrl, Callback callback) {
            org.duckdns.hjow.swingwebpanel.render.ImageResource r = images.get(absoluteUrl);
            if (r == null || r.getImage() == null) {
                callback.done(0, 0, "404");
            } else {
                callback.done(r.getImage().getWidth(), r.getImage().getHeight(), null);
            }
        }
    }

    /**
     * Makes a 4 by 4 image of one color.
     * <p>
     * 한 가지 색의 4 x 4 이미지를 만든다.
     *
     * @param argb the color / 색
     * @return the image / 이미지
     */
    private static org.duckdns.hjow.swingwebpanel.media.image.DecodedImage solid(int argb) {
        int[] px = new int[16];
        java.util.Arrays.fill(px, argb);
        return new org.duckdns.hjow.swingwebpanel.media.image.DecodedImage(4, 4, px);
    }

    /**
     * The origin-clean flag passes from an image to its bitmap and from the bitmap to the canvas that draws it: a bitmap of a same-origin image or
     * of a cross-origin image with permission is clean, a bitmap of a cross-origin image without permission taints the canvas, and so does a canvas
     * that drew such a bitmap. A canvas, an <code>OffscreenCanvas</code> and a bitmap that are tainted give tainted bitmaps.
     * <p>
     * origin-clean 표시는 이미지에서 그 비트맵으로, 비트맵에서 그것을 그리는 캔버스로 전달된다: 같은 오리진 이미지나 허락받은 교차 오리진 이미지의 비트맵은 깨끗하고,
     * 허락 없는 교차 오리진 이미지의 비트맵은 그것을 그리는 캔버스를 오염시키며, 그런 비트맵을 그린 캔버스도 오염된다. 오염된 캔버스, <code>OffscreenCanvas</code>,
     * 비트맵의 비트맵도 오염된다.
     */
    public void testTaintPassesThroughBitmaps() {
        PageFixture f = new PageFixture();
        f.env.images = new Immediate(f.env.loadedImages);
        f.env.loadedImages.put("http://example.test/dir/same.png", org.duckdns.hjow.swingwebpanel.render.ImageResource.loaded(solid(0xFFFF0000), "http://example.test/dir/same.png",
                null, null));
        f.env.loadedImages.put("http://other.test/foreign.png", org.duckdns.hjow.swingwebpanel.render.ImageResource.loaded(solid(0xFF00FF00), "http://other.test/foreign.png", null,
                null));
        f.env.loadedImages.put("http://other.test/open.png", org.duckdns.hjow.swingwebpanel.render.ImageResource.loaded(solid(0xFF0000FF), "http://other.test/open.png", "*", null));
        f.load("<canvas id=c width=4 height=4></canvas><img id=same src='same.png'><img id=foreign src='http://other.test/foreign.png'>"
                + "<img id=open crossorigin src='http://other.test/open.png'>");
        f.scripts.evaluate(CanvasBasicsTest.HELPERS + HELPERS, "helpers");
        f.env.scheduler.advance(0);
        js(f, "var log = []; function reads(c) { try { c.getContext('2d').getImageData(0, 0, 1, 1); return 'clean'; } catch (e) { return e.name; } }"
                + " function viaBitmap(src) { var d = document.createElement('canvas'); d.width = 4; d.height = 4; createImageBitmap(src).then(function (b) {"
                + " d.getContext('2d').drawImage(b, 0, 0); log.push(reads(d)); }); }");
        js(f, "viaBitmap(document.getElementById('same')); viaBitmap(document.getElementById('open')); viaBitmap(document.getElementById('foreign'));");
        Check.equal("clean,clean,SecurityError", js(f, "log.join()"), "same origin and permitted images are clean, a foreign image taints");
        js(f, "log.length = 0; var t = document.createElement('canvas'); t.width = 4; t.height = 4; t.getContext('2d').drawImage(document.getElementById('foreign'), 0, 0);"
                + " viaBitmap(t);");
        Check.equal("SecurityError", js(f, "log.join()"), "a bitmap of a tainted canvas is tainted");
        js(f, "log.length = 0; var oc = new OffscreenCanvas(4, 4); oc.getContext('2d').drawImage(document.getElementById('foreign'), 0, 0); viaBitmap(oc);"
                + " oc.convertToBlob().then(null, function (e) { log.push('blob:' + e.name); });");
        Check.equal("SecurityError,blob:SecurityError", js(f, "log.join()"), "so is the bitmap of a tainted OffscreenCanvas, which cannot be exported either");
        js(f, "log.length = 0; createImageBitmap(document.getElementById('foreign')).then(function (b) { return createImageBitmap(b); }).then(function (b2) {"
                + " var d = document.createElement('canvas'); d.getContext('2d').drawImage(b2, 0, 0); log.push(reads(d)); });");
        Check.equal("SecurityError", js(f, "log.join()"), "a bitmap of a tainted bitmap is tainted");
    }
}
