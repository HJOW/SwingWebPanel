package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>context.filter</code> (plan item P20-05-b, HTML Living Standard 4.12.5.1.20 and CSS Filter Effects Module Level 1): the state
 * (default, accepted and ignored values, <code>save()</code>/<code>restore()</code>/<code>reset()</code>), the colors that the filter functions
 * produce on flat regions, the order of a function list, blur and <code>drop-shadow()</code> that reach beyond what was drawn, that the global
 * alpha is applied before the filter, that the transformation of the context does not scale the filter, that <code>clearRect()</code> is not
 * filtered and that <code>url()</code> references are valid but filter nothing. Pixels are read at the middle of large flat regions; color
 * functions are compared with a tolerance of 2 because the matrices work on 8 bit channels.
 * <p>
 * <code>context.filter</code> 시험(계획 P20-05-b, HTML Living Standard 4.12.5.1.20 과 CSS Filter Effects Module Level 1): 상태(기본값, 받는 값과 무시하는 값,
 * <code>save()</code>/<code>restore()</code>/<code>reset()</code>), 필터 함수가 평평한 영역에 만드는 색, 함수 목록의 순서, 그린 것 밖으로 번지는 흐림과
 * <code>drop-shadow()</code>, 전역 알파를 필터 앞에 적용하는 것, 컨텍스트의 변환이 필터를 확대하지 않는 것, <code>clearRect()</code> 가 필터를 거치지 않는 것,
 * <code>url()</code> 참조가 올바르지만 아무것도 거르지 않는 것. 화소는 큰 평평한 영역의 가운데에서 읽고, 색 함수는 행렬이 8비트 채널에서 일하므로 오차 2 로 비교한다.
 */
public class CanvasFilterTest {

    /**
     * The helper script: <code>near(x, y, [r, g, b, a])</code> compares a pixel with a tolerance of 2.
     * <p>
     * 도우미 스크립트: <code>near(x, y, [r, g, b, a])</code> 는 화소를 오차 2 로 비교한다.
     */
    private static final String NEAR = "function near(x, y, e) { var d = ctx.getImageData(x, y, 1, 1).data; for (var i = 0; i < 4; i++) { if (Math.abs(d[i] - e[i]) > 2) return false; }"
            + " return true; }";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasFilterTest() {
    }

    /**
     * Loads a page with a canvas and the helpers.
     * <p>
     * 캔버스와 도우미가 있는 페이지를 로드한다.
     *
     * @param attributes the attributes of the canvas / 캔버스의 속성
     * @return the fixture / 시험 도구
     */
    private static PageFixture page(String attributes) {
        PageFixture f = CanvasBasicsTest.page(attributes);
        f.scripts.evaluate(NEAR, "near");
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
     * The default is <code>none</code>; the getter returns the string as it was set; values that are not a filter list are ignored and the old
     * value stays; <code>save()</code>, <code>restore()</code> and <code>reset()</code> handle the filter like any other state.
     * <p>
     * 기본값은 <code>none</code> 이다. getter 는 설정한 그대로의 문자열을 반환한다. 필터 목록이 아닌 값은 무시하고 옛 값이 남는다. <code>save()</code>,
     * <code>restore()</code>, <code>reset()</code> 은 필터를 다른 상태처럼 다룬다.
     */
    public void testState() {
        PageFixture f = page("width=20 height=20");
        Check.equal("none", js(f, "ctx.filter"), "the default");
        Check.equal("blur(2px)|Invert( 1 ) SEPIA(.5)|none", js(f, "var r = []; ctx.filter = 'blur(2px)'; r.push(ctx.filter); ctx.filter = 'Invert( 1 ) SEPIA(.5)'; r.push(ctx.filter);"
                + " ctx.filter = 'none'; r.push(ctx.filter); r.join('|')"), "the string is kept as written, none switches the filter off");
        String[] bad = { "''", "'bogus'", "'blur(-1px)'", "'blur(1px) bogus'", "'inherit'", "'initial'", "'unset'", "'var(--x)'", "'blur(5)'", "'brightness(-1)'",
            "'drop-shadow(1px)'", "'hue-rotate(10)'", "null", "undefined", "'blur()  ,'", "42" };
        StringBuilder script = new StringBuilder("ctx.filter = 'grayscale(1)'; var r = [];");
        // 올바르지 않은 값을 하나씩 대입하고 필터가 그대로인지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            script.append("ctx.filter = ").append(bad[i]).append("; r.push(ctx.filter);");
        }
        String result = js(f, script.append("r.join('|')").toString());
        StringBuilder expected = new StringBuilder();
        // 모든 항목이 처음 값 그대로여야 한다.
        for (int i = 0; i < bad.length; i++) {
            expected.append(i > 0 ? "|" : "").append("grayscale(1)");
        }
        Check.equal(expected.toString(), result, "values that are not a filter list are ignored");
        Check.equal("blur(1px) drop-shadow(1px 2px 3px red) url(#x)", js(f, "ctx.filter = 'blur(1px) drop-shadow(1px 2px 3px red) url(#x)'; ctx.filter"), "a list with a url reference is valid");
        Check.equal("saturate(150%)|hue-rotate(0.5turn)|blur(0)", js(f, "var r = []; ctx.filter = 'saturate(150%)'; r.push(ctx.filter); ctx.filter = 'hue-rotate(0.5turn)'; r.push(ctx.filter);"
                + " ctx.filter = 'blur(0)'; r.push(ctx.filter); r.join('|')"), "percentages, angle units and a plain 0");
        Check.equal("sepia(1)|none|sepia(1)", js(f, "ctx.filter = 'sepia(1)'; var r = [ctx.filter]; ctx.save(); ctx.filter = 'none'; r.push(ctx.filter); ctx.restore(); r.push(ctx.filter); r.join('|')"),
                "save and restore");
        Check.equal("none", js(f, "ctx.filter = 'blur(3px)'; ctx.reset(); ctx.filter"), "reset");
        Check.equal("true", js(f, "var d = Object.getOwnPropertyDescriptor(CanvasRenderingContext2D.prototype, 'filter'); String(typeof d.get === 'function' && typeof d.set === 'function')"),
                "an accessor on the prototype");
        Check.equal("none|true", js(f, "var oc = new OffscreenCanvas(10, 10).getContext('2d'); var r = [oc.filter]; oc.filter = 'invert(1)'; r.push(oc.filter === 'invert(1)'); r.join('|')"),
                "OffscreenCanvasRenderingContext2D has the attribute as well");
    }

    /**
     * The color functions on flat regions: <code>invert</code>, <code>grayscale</code>, <code>brightness</code>, <code>contrast</code>,
     * <code>saturate</code>, <code>sepia</code>, <code>hue-rotate</code> and <code>opacity</code>, with numbers and percentages; the filter
     * applies to fills, strokes, images and text, and a list is applied from left to right.
     * <p>
     * 평평한 영역에서의 색 함수: <code>invert</code>, <code>grayscale</code>, <code>brightness</code>, <code>contrast</code>, <code>saturate</code>,
     * <code>sepia</code>, <code>hue-rotate</code>, <code>opacity</code>(숫자와 백분율). 필터는 채우기, 선, 이미지, 텍스트에 적용되고 목록은 왼쪽에서 오른쪽으로 적용된다.
     */
    public void testColorFunctions() {
        PageFixture f = page("width=30 height=30");
        js(f, "function paint(filter, color) { ctx.reset(); ctx.filter = filter; ctx.fillStyle = color; ctx.fillRect(0, 0, 30, 30); }");
        Check.equal("true", js(f, "paint('invert(1)', 'red'); String(near(15, 15, [0, 255, 255, 255]))"), "invert(1) turns red into cyan");
        Check.equal("true", js(f, "paint('invert(100%)', 'white'); String(near(15, 15, [0, 0, 0, 255]))"), "100% is 1");
        Check.equal("true", js(f, "paint('invert(0.5)', 'red'); String(near(15, 15, [128, 128, 128, 255]))"), "invert(0.5) of red is the middle gray");
        Check.equal("true", js(f, "paint('invert()', 'black'); String(near(15, 15, [255, 255, 255, 255]))"), "no argument is 1");
        Check.equal("true", js(f, "paint('grayscale(1)', 'red'); String(near(15, 15, [54, 54, 54, 255]))"), "grayscale(1) of red has the luminance 0.2126");
        Check.equal("true", js(f, "paint('grayscale(0)', 'red'); String(near(15, 15, [255, 0, 0, 255]))"), "grayscale(0) changes nothing");
        Check.equal("true", js(f, "paint('brightness(0.5)', 'white'); String(near(15, 15, [128, 128, 128, 255]))"), "brightness(0.5) of white");
        Check.equal("true", js(f, "paint('brightness(2)', 'rgb(100, 50, 20)'); String(near(15, 15, [200, 100, 40, 255]))"), "brightness(2) doubles the channels");
        Check.equal("true", js(f, "paint('brightness(0)', 'lime'); String(near(15, 15, [0, 0, 0, 255]))"), "brightness(0) is black");
        Check.equal("true", js(f, "paint('contrast(0)', 'red'); String(near(15, 15, [128, 128, 128, 255]))"), "contrast(0) is the middle gray");
        Check.equal("true", js(f, "paint('contrast(2)', 'rgb(192, 128, 64)'); String(near(15, 15, [255, 128, 1, 255]))"), "contrast(2) doubles the distance from the middle");
        Check.equal("true", js(f, "paint('saturate(0)', 'red'); String(near(15, 15, [54, 54, 54, 255]))"), "saturate(0) is gray");
        Check.equal("true", js(f, "paint('saturate(1)', 'rgb(10, 200, 90)'); String(near(15, 15, [10, 200, 90, 255]))"), "saturate(1) changes nothing");
        Check.equal("true", js(f, "paint('sepia(1)', 'white'); String(near(15, 15, [255, 255, 239, 255]))"), "sepia(1) of white");
        Check.equal("true", js(f, "paint('hue-rotate(180deg)', 'red'); String(near(15, 15, [0, 109, 109, 255]))"), "hue-rotate(180deg) of red");
        Check.equal("true", js(f, "paint('hue-rotate(0.5turn)', 'red'); String(near(15, 15, [0, 109, 109, 255]))"), "the same angle in turns");
        Check.equal("true", js(f, "paint('hue-rotate(360deg)', 'rgb(10, 200, 90)'); String(near(15, 15, [10, 200, 90, 255]))"), "a full turn changes nothing");
        Check.equal("true", js(f, "paint('opacity(0.5)', 'red'); String(near(15, 15, [255, 0, 0, 128]))"), "opacity(0.5) halves the alpha");
        Check.equal("true", js(f, "paint('opacity(25%)', 'red'); String(near(15, 15, [255, 0, 0, 64]))"), "opacity(25%)");
        Check.equal("true", js(f, "paint('invert(1) brightness(0.5)', 'white'); String(near(15, 15, [0, 0, 0, 255]))"), "invert first: white, black, black");
        Check.equal("true", js(f, "paint('brightness(0.5) invert(1)', 'white'); String(near(15, 15, [128, 128, 128, 255]))"), "brightness first: white, gray, gray");
        Check.equal("true", js(f, "paint('invert(1)', 'rgba(255, 0, 0, 0.5)'); String(near(15, 15, [0, 255, 255, 128]))"), "the alpha of a translucent fill is kept");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'invert(1)'; ctx.strokeStyle = 'white'; ctx.lineWidth = 8; ctx.strokeRect(10, 10, 20, 20); String(near(10, 20, [0, 0, 0, 255]) && near(20, 20, [0, 0, 0, 0]))"),
                "strokes are filtered (the middle of the square stays empty)");
        Check.equal("true", js(f, "ctx.reset(); var src = document.createElement('canvas'); src.width = 10; src.height = 10; var sc = src.getContext('2d'); sc.fillStyle = 'red'; sc.fillRect(0, 0, 10, 10);"
                + " ctx.filter = 'invert(1)'; ctx.drawImage(src, 5, 5, 20, 20); String(near(15, 15, [0, 255, 255, 255]) && near(2, 2, [0, 0, 0, 0]))"), "drawImage is filtered, the area outside stays empty");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'invert(1)'; ctx.fillStyle = 'white'; ctx.font = '24px sans-serif'; ctx.fillText('I', 10, 25); var d = ctx.getImageData(0, 0, 30, 30).data; var dark = 0;"
                + " for (var i = 0; i < d.length; i += 4) { if (d[i + 3] > 200 && d[i] < 50) dark++; } String(dark > 5)"), "text is filtered");
        Check.equal("true", js(f, "ctx.reset(); ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 30, 30); ctx.filter = 'invert(1)'; ctx.clearRect(10, 10, 10, 10); ctx.fillStyle = 'blue'; ctx.fillRect(0, 0, 4, 4);"
                + " String(near(15, 15, [0, 0, 0, 0]) && near(25, 25, [255, 0, 0, 255]) && near(2, 2, [255, 255, 0, 255]))"),
                "clearRect is not filtered (the hole is transparent, not cyan), the later fill is");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'invert(1)'; var id = ctx.createImageData(2, 2); for (var i = 0; i < 16; i += 4) { id.data[i] = 255; id.data[i + 3] = 255; }"
                + " ctx.putImageData(id, 0, 0); String(near(0, 0, [255, 0, 0, 255]))"), "putImageData is not filtered");
        Check.equal("true", js(f, "paint('invert(1)', 'red'); ctx.filter = 'none'; ctx.fillStyle = 'lime'; ctx.fillRect(10, 10, 10, 10);"
                + " String(near(15, 15, [0, 255, 0, 255]) && near(2, 2, [0, 255, 255, 255]))"), "none switches the filter off for later drawing, earlier pixels stay");
    }

    /**
     * The global alpha is applied to the image before the filter and a shadow is made from the filtered image; the compositing operator works
     * on the filtered image.
     * <p>
     * 전역 알파는 필터 앞에서 이미지에 적용되고 그림자는 필터를 거친 이미지로 만든다. 합성 연산자는 필터를 거친 이미지에 작용한다.
     */
    public void testAlphaShadowAndComposite() {
        PageFixture f = page("width=40 height=40");
        Check.equal("true", js(f, "ctx.globalAlpha = 0.5; ctx.filter = 'invert(1)'; ctx.fillStyle = 'white'; ctx.fillRect(0, 0, 40, 40); String(near(20, 20, [0, 0, 0, 128]))"),
                "globalAlpha 0.5 and invert: translucent black");
        Check.equal("true", js(f, "ctx.reset(); ctx.globalAlpha = 0.5; ctx.filter = 'opacity(0.5)'; ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 40, 40); String(near(20, 20, [255, 0, 0, 64]))"),
                "alpha and opacity multiply (the alpha is counted once)");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'invert(1)'; ctx.shadowColor = 'blue'; ctx.shadowOffsetX = 12; ctx.shadowOffsetY = 0; ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 16, 40);"
                + " String(near(8, 20, [0, 255, 255, 255]) && near(20, 20, [0, 0, 255, 255]) && near(30, 20, [0, 0, 0, 0]))"), "the shadow of a filtered fill: cyan body, shadow shifted by 12 and blue");
        Check.equal("true", js(f, "ctx.reset(); ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 40, 40); ctx.globalCompositeOperation = 'destination-over'; ctx.filter = 'invert(1)'; ctx.fillStyle = 'white'; ctx.fillRect(0, 0, 40, 40);"
                + " String(near(20, 20, [255, 0, 0, 255]))"), "destination-over keeps the red body above the filtered new image");
        Check.equal("true", js(f, "ctx.reset(); ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 40, 40); ctx.globalCompositeOperation = 'source-in'; ctx.filter = 'invert(1)'; ctx.fillStyle = 'red'; ctx.fillRect(10, 10, 20, 20);"
                + " String(near(20, 20, [0, 255, 255, 255]) && near(5, 5, [0, 0, 0, 0]))"), "source-in with a filtered source: cyan inside, nothing outside");
        Check.equal("true", js(f, "ctx.reset(); ctx.fillStyle = 'red'; ctx.beginPath(); ctx.rect(0, 0, 20, 40); ctx.clip(); ctx.filter = 'invert(1)'; ctx.fillStyle = 'white'; ctx.fillRect(0, 0, 40, 40);"
                + " String(near(10, 20, [0, 0, 0, 255]) && near(30, 20, [0, 0, 0, 0]))"), "the clip applies to the filtered image");
    }

    /**
     * <code>blur()</code> and <code>drop-shadow()</code> reach beyond what was drawn; the lengths are pixels of the bitmap, not scaled by the
     * transformation of the context, and <code>em</code> follows the font size of the canvas element.
     * <p>
     * <code>blur()</code> 와 <code>drop-shadow()</code> 는 그린 것 밖으로 번진다. 길이는 비트맵의 화소이며 컨텍스트의 변환으로 확대되지 않고 <code>em</code> 은 캔버스
     * 요소의 글꼴 크기를 따른다.
     */
    public void testBlurAndDropShadow() {
        PageFixture f = page("width=80 height=80");
        js(f, "function alpha(x, y) { return ctx.getImageData(x, y, 1, 1).data[3]; }");
        js(f, "ctx.filter = 'blur(4px)'; ctx.fillStyle = 'black'; ctx.fillRect(30, 30, 20, 20);");
        Check.equal("true", js(f, "String(alpha(40, 40) > 235 && alpha(40, 40) < 256)"), "the middle of a 20 x 20 square blurred by 4px stays almost opaque (" + js(f, "alpha(40, 40)") + ")");
        Check.equal("true", js(f, "String(alpha(30, 40) > 90 && alpha(30, 40) < 160)"), "the edge is about half (" + js(f, "alpha(30, 40)") + ")");
        Check.equal("true", js(f, "String(alpha(26, 40) > 5 && alpha(26, 40) < alpha(30, 40) && alpha(26, 40) > alpha(21, 40))"), "the blur reaches beyond the square and falls off ("
                + js(f, "[alpha(21, 40), alpha(26, 40), alpha(30, 40)].join()") + ")");
        Check.equal("true", js(f, "String(alpha(5, 5) === 0 && alpha(70, 70) === 0)"), "far away nothing");
        Check.equal("true", js(f, "var sum = 0; var d = ctx.getImageData(0, 0, 80, 80).data; for (var i = 3; i < d.length; i += 4) sum += d[i]; String(Math.abs(sum / 255 - 400) < 8)"),
                "the blur keeps the total coverage (about 400 pixels)");
        js(f, "ctx.reset(); ctx.filter = 'drop-shadow(12px 0px 0px red)'; ctx.fillStyle = 'blue'; ctx.fillRect(10, 30, 16, 16);");
        Check.equal("true", js(f, "String(near(15, 38, [0, 0, 255, 255]) && near(32, 38, [255, 0, 0, 255]) && near(40, 38, [0, 0, 0, 0]))"),
                "drop-shadow 12px 0 0 red: blue body, red copy shifted right by 12, body in front");
        js(f, "ctx.reset(); ctx.filter = 'drop-shadow(0px 0px 4px black)'; ctx.fillStyle = 'black'; ctx.fillRect(30, 30, 20, 20);");
        Check.equal("true", js(f, "String(alpha(40, 40) === 255 && alpha(27, 40) > 5 && alpha(5, 5) === 0)"), "a blurred drop-shadow shows around the body (" + js(f, "alpha(27, 40)") + ")");
        js(f, "ctx.reset(); ctx.filter = 'drop-shadow(10px 0px 0px)'; ctx.fillStyle = 'blue'; ctx.fillRect(10, 30, 16, 16);");
        Check.equal("true", js(f, "String(near(32, 38, [0, 0, 0, 255]))"), "without a color the shadow is black (the color of the canvas element)");
        js(f, "ctx.reset(); var a = ctx.getImageData(0, 0, 80, 80).data; ctx.filter = 'blur(4px)'; ctx.scale(2, 2); ctx.fillStyle = 'black'; ctx.fillRect(15, 15, 10, 10);"
                + " var scaled = ctx.getImageData(0, 0, 80, 80).data; ctx.reset(); ctx.clearRect(0, 0, 80, 80); ctx.filter = 'blur(4px)'; ctx.fillStyle = 'black'; ctx.fillRect(30, 30, 20, 20);"
                + " var plain = ctx.getImageData(0, 0, 80, 80).data; var same = true; for (var i = 0; i < scaled.length; i++) { if (scaled[i] !== plain[i]) { same = false; break; } }");
        Check.equal("true", js(f, "String(same)"), "the transformation moves the input only: scale(2) with a 10 square equals no scale with a 20 square (the blur is the same)");
        js(f, "ctx.reset(); ctx.filter = 'blur(1em)'; ctx.fillStyle = 'black'; ctx.fillRect(30, 30, 20, 20); var em = ctx.getImageData(0, 0, 80, 80).data;"
                + " ctx.reset(); ctx.filter = 'blur(16px)'; ctx.fillStyle = 'black'; ctx.fillRect(30, 30, 20, 20); var px = ctx.getImageData(0, 0, 80, 80).data; var same2 = true;"
                + " for (var j = 0; j < em.length; j++) { if (em[j] !== px[j]) { same2 = false; break; } }");
        Check.equal("true", js(f, "String(same2)"), "1em is the font size of the canvas element, 16px");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'blur(3px)'; ctx.fillStyle = 'black'; ctx.fillRect(0, 0, 80, 80);"
                + " String(alpha(0, 40) > 80 && alpha(0, 40) < 160 && alpha(0, 0) > 25 && alpha(0, 0) < 100 && alpha(0, 0) < alpha(0, 40) && alpha(40, 40) >= 253)"),
                "a fill that covers the canvas: the edge fades to about 0.4 and the corner to about 0.17 because the outside counts as transparent, the middle stays ("
                + js(f, "[alpha(0, 40), alpha(0, 0), alpha(40, 40)].join()") + ")");
    }

    /**
     * A <code>url()</code> reference to an SVG filter is a valid value that filters nothing; a list that mixes functions and a reference is
     * ignored by the functions' effect only as far as the reference makes the whole list unusable (drawing proceeds unfiltered).
     * <p>
     * SVG 필터를 가리키는 <code>url()</code> 참조는 아무것도 거르지 않는 올바른 값이다. 함수와 참조가 섞인 목록은 참조가 목록 전체를 쓸 수 없게 만드는 만큼 함수의 효과도
     * 없다(필터 없이 그린다).
     */
    public void testUrlReference() {
        PageFixture f = page("width=20 height=20");
        Check.equal("true", js(f, "ctx.filter = 'url(#nothing)'; ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 20, 20); String(ctx.filter === 'url(#nothing)' && near(10, 10, [255, 0, 0, 255]))"),
                "the value is kept and the drawing is unfiltered");
        Check.equal("true", js(f, "ctx.reset(); ctx.filter = 'invert(1) url(#nothing)'; ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 20, 20); String(near(10, 10, [255, 0, 0, 255]))"),
                "a list with a reference draws unfiltered, as if the reference could not be loaded");
    }
}
