package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the text attributes of the 2D context that came after the first canvas text API (plan item P20-05-b): <code>letterSpacing</code>,
 * <code>wordSpacing</code>, <code>fontKerning</code>, <code>fontStretch</code>, <code>fontVariantCaps</code>, <code>textRendering</code> and
 * <code>lang</code>. Their defaults, the serialized form of the lengths, the values that are ignored, <code>save()</code>,
 * <code>restore()</code> and <code>reset()</code> are checked on the state; the spacings also change the width that <code>measureText()</code>
 * reports and the place where the ink appears, which is checked on exact numbers and on the columns of the pixels.
 * <p>
 * 처음의 캔버스 텍스트 API 뒤에 생긴 2D 컨텍스트의 텍스트 속성 시험(계획 P20-05-b): <code>letterSpacing</code>, <code>wordSpacing</code>,
 * <code>fontKerning</code>, <code>fontStretch</code>, <code>fontVariantCaps</code>, <code>textRendering</code>, <code>lang</code>. 기본값, 길이의 직렬화된 형태,
 * 무시되는 값, <code>save()</code>, <code>restore()</code>, <code>reset()</code> 은 상태로 확인하고, 간격은 <code>measureText()</code> 가 보고하는 너비와 잉크가
 * 나타나는 위치도 바꾸므로 정확한 숫자와 화소의 열로 확인한다.
 */
public class CanvasTextStyleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CanvasTextStyleTest() {
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
     * The defaults, the accepted and the ignored values, and <code>save()</code>/<code>restore()</code>/<code>reset()</code> of the seven
     * attributes.
     * <p>
     * 일곱 속성의 기본값, 받는 값과 무시하는 값, <code>save()</code>/<code>restore()</code>/<code>reset()</code>.
     */
    public void testState() {
        PageFixture f = CanvasBasicsTest.page("width=20 height=20");
        Check.equal("0px,0px,auto,normal,normal,auto,inherit", js(f, "[ctx.letterSpacing, ctx.wordSpacing, ctx.fontKerning, ctx.fontStretch, ctx.fontVariantCaps,"
                + " ctx.textRendering, ctx.lang].join()"), "the defaults");
        Check.equal("5px,1.5px,-2px,0.1em,2Q,10pt", js(f, "var r = []; ctx.letterSpacing = '5px'; r.push(ctx.letterSpacing); ctx.letterSpacing = ' 1.5PX '; ctx.wordSpacing = ctx.letterSpacing;"
                + " r.push(ctx.wordSpacing); ctx.letterSpacing = '-2px'; r.push(ctx.letterSpacing); ctx.letterSpacing = '0.1em'; r.push(ctx.letterSpacing);"
                + " ctx.letterSpacing = '2q'; r.push(ctx.letterSpacing); ctx.letterSpacing = '10pt'; r.push(ctx.letterSpacing); r.join()"),
                "lengths are serialized with the unit in lower case (Q is the exception) and surrounding white space is allowed");
        Check.equal("0px", js(f, "ctx.letterSpacing = '0'; ctx.letterSpacing"), "a plain 0 is a length");
        Check.equal("3px,3px,3px,3px,3px,3px,3px,3px,3px", js(f, "ctx.letterSpacing = '3px'; var r = []; var bad = ['', 'abc', '5', '5%', 'calc(1px + 2px)', '1e', 'px', '5vw']; for (var i = 0; i < bad.length; i++) {"
                + " ctx.letterSpacing = bad[i]; r.push(ctx.letterSpacing); } ctx.letterSpacing = 'normal'; r.push(ctx.letterSpacing); r.join()"),
                "values that are not a CSS length are ignored");
        Check.equal("1e-7px,1e+21px", js(f, "var r = []; ctx.letterSpacing = '1e-7px'; r.push(ctx.letterSpacing); ctx.letterSpacing = '1e21px'; r.push(ctx.letterSpacing); r.join()"), "exponents");
        Check.equal("normal,none,small-caps,geometricPrecision,ko", js(f, "ctx.fontKerning = 'normal'; var r = [ctx.fontKerning]; ctx.fontKerning = 'none'; r.push(ctx.fontKerning);"
                + " ctx.fontVariantCaps = 'small-caps'; r.push(ctx.fontVariantCaps); ctx.textRendering = 'geometricPrecision'; r.push(ctx.textRendering);"
                + " ctx.lang = 'ko'; r.push(ctx.lang); r.join()"), "keywords");
        Check.equal("none,small-caps,geometricPrecision", js(f, "ctx.fontKerning = 'bogus'; ctx.fontKerning = 'NORMAL'; ctx.fontKerning = ''; ctx.fontVariantCaps = 'BIG'; ctx.textRendering = 'optimizespeed';"
                + " [ctx.fontKerning, ctx.fontVariantCaps, ctx.textRendering].join()"), "unknown keywords are ignored (the keywords are case sensitive)");
        String[] stretch = { "ultra-condensed", "extra-condensed", "condensed", "semi-condensed", "normal", "semi-expanded", "expanded", "extra-expanded", "ultra-expanded" };
        StringBuilder expected = new StringBuilder();
        StringBuilder script = new StringBuilder("var r = [];");
        // 아홉 개의 fontStretch 키워드를 하나씩 정하고 읽는다.
        for (int i = 0; i < stretch.length; i++) {
            expected.append(i > 0 ? "," : "").append(stretch[i]);
            script.append("ctx.fontStretch = '").append(stretch[i]).append("'; r.push(ctx.fontStretch);");
        }
        Check.equal(expected.toString(), js(f, script.append("r.join()").toString()), "the nine fontStretch keywords");
        Check.equal("ultra-expanded", js(f, "ctx.fontStretch = 'wide'; ctx.fontStretch"), "an unknown fontStretch keyword is ignored");
        Check.equal("unicase,all-petite-caps", js(f, "ctx.fontVariantCaps = 'unicase'; var r = [ctx.fontVariantCaps]; ctx.fontVariantCaps = 'all-petite-caps'; r.push(ctx.fontVariantCaps); r.join()"),
                "other fontVariantCaps keywords");
        Check.equal("optimizeLegibility,optimizeSpeed,auto", js(f, "var r = []; ctx.textRendering = 'optimizeLegibility'; r.push(ctx.textRendering); ctx.textRendering = 'optimizeSpeed'; r.push(ctx.textRendering);"
                + " ctx.textRendering = 'auto'; r.push(ctx.textRendering); r.join()"), "all textRendering keywords");
        Check.equal(",inherit,zz-Latn", js(f, "var r = []; ctx.lang = ''; r.push(ctx.lang); ctx.lang = 'inherit'; r.push(ctx.lang); ctx.lang = 'zz-Latn'; r.push(ctx.lang); r.join()"), "any string is a language");
        // save 와 restore 는 일곱 속성을 모두 되돌린다.
        js(f, "ctx.letterSpacing = '4px'; ctx.wordSpacing = '6px'; ctx.fontKerning = 'normal'; ctx.fontStretch = 'condensed'; ctx.fontVariantCaps = 'small-caps';"
                + " ctx.textRendering = 'optimizeSpeed'; ctx.lang = 'en'; ctx.save();"
                + " ctx.letterSpacing = '9px'; ctx.wordSpacing = '9px'; ctx.fontKerning = 'none'; ctx.fontStretch = 'expanded'; ctx.fontVariantCaps = 'unicase';"
                + " ctx.textRendering = 'auto'; ctx.lang = 'fr'; ctx.restore();");
        Check.equal("4px,6px,normal,condensed,small-caps,optimizeSpeed,en", js(f, "[ctx.letterSpacing, ctx.wordSpacing, ctx.fontKerning, ctx.fontStretch, ctx.fontVariantCaps,"
                + " ctx.textRendering, ctx.lang].join()"), "restore brings all seven back");
        js(f, "ctx.reset();");
        Check.equal("0px,0px,auto,normal,normal,auto,inherit", js(f, "[ctx.letterSpacing, ctx.wordSpacing, ctx.fontKerning, ctx.fontStretch, ctx.fontVariantCaps,"
                + " ctx.textRendering, ctx.lang].join()"), "reset returns the defaults");
        Check.equal("none", js(f, "var d = Object.getOwnPropertyDescriptor(CanvasRenderingContext2D.prototype, 'letterSpacing'); (typeof d.get === 'function' && typeof d.set === 'function') ? 'none' : 'missing'"),
                "the attributes are accessors on the prototype");
    }

    /**
     * The spacings change what <code>measureText()</code> reports: the letter spacing is added after every character (the last one included), the
     * word spacing after every space and no-break space; negative values narrow the text; relative units follow the font that is current when the
     * text is measured; right-to-left text is spread as well.
     * <p>
     * 간격은 <code>measureText()</code> 가 보고하는 값을 바꾼다: 글자 간격은 마지막을 포함한 모든 글자 뒤에, 낱말 간격은 모든 스페이스와 줄바꿈 없는 스페이스 뒤에
     * 더해지고, 음수는 텍스트를 좁히며, 상대 단위는 텍스트를 잴 때의 글꼴을 따르고, 오른쪽에서 왼쪽 텍스트도 벌어진다.
     */
    public void testMeasureWithSpacing() {
        PageFixture f = CanvasBasicsTest.page("width=20 height=20");
        js(f, "ctx.font = '20px sans-serif'; function w(t) { return ctx.measureText(t).width; } function near(a, b) { return Math.abs(a - b) < 1e-6; }"
                + " var base4 = w('abcd'), baseSpaced = w('a b c'), baseNbsp = w('a\\u00a0b'), baseHebrew = w('\\u05e9\\u05dc\\u05d5\\u05dd');");
        Check.equal("true", js(f, "ctx.letterSpacing = '5px'; String(near(w('abcd') - base4, 20))"), "four characters of 5px add 20");
        Check.equal("true", js(f, "ctx.letterSpacing = '-2px'; String(near(w('abcd') - base4, -8))"), "negative spacing narrows the text");
        Check.equal("true", js(f, "ctx.letterSpacing = '0px'; String(near(w('abcd'), base4))"), "0px is the default width");
        Check.equal("true", js(f, "ctx.letterSpacing = '0px'; ctx.wordSpacing = '7px'; String(near(w('a b c') - baseSpaced, 14))"), "two spaces of 7px add 14");
        Check.equal("true", js(f, "String(near(w('abcd'), base4))"), "text without spaces ignores the word spacing");
        Check.equal("true", js(f, "String(near(w('a\\u00a0b') - baseNbsp, 7))"), "a no-break space is a word separator too");
        Check.equal("true", js(f, "ctx.letterSpacing = '1px'; String(near(w('a b c') - baseSpaced, 14 + 5))"), "both together: five characters of 1px and two spaces of 7px");
        Check.equal("true", js(f, "ctx.wordSpacing = '0px'; ctx.letterSpacing = '0.5em'; String(near(w('abcd') - base4, 40))"), "0.5em at 20px is 10px per character");
        Check.equal("true", js(f, "ctx.letterSpacing = '0px'; ctx.font = '40px sans-serif'; var plain40 = w('abcd'); ctx.letterSpacing = '0.5em'; String(near(w('abcd') - plain40, 80))"),
                "em is resolved against the font that is current when the text is measured: 0.5em at 40px is 20px");
        Check.equal("true", js(f, "ctx.font = '20px sans-serif'; ctx.letterSpacing = '0px'; var no = w('ab'); ctx.letterSpacing = '1in'; String(near(w('ab') - no, 192))"),
                "absolute units: 1in is 96px per character");
        Check.equal("true", js(f, "ctx.letterSpacing = '5px'; ctx.wordSpacing = '5px'; String(w('') === 0)"), "empty text has no width and no spacing");
        Check.equal("true", js(f, "ctx.letterSpacing = '5px'; ctx.wordSpacing = '0px'; var m = ctx.measureText('a'); ctx.letterSpacing = '0px'; var m0 = ctx.measureText('a');"
                + " String(near(m.width - m0.width, 5) && near(m.actualBoundingBoxRight, m0.actualBoundingBoxRight))"),
                "the ink box of a single character does not move: the spacing comes after the glyph");
        Check.equal("true", js(f, "ctx.letterSpacing = '5px'; var mm = ctx.measureText('aa'); ctx.letterSpacing = '0px'; var mn = ctx.measureText('aa');"
                + " String(near(mm.actualBoundingBoxRight - mn.actualBoundingBoxRight, 5))"), "the ink of the second character moved by one spacing");
        Check.equal("true", js(f, "ctx.letterSpacing = '3px'; ctx.textAlign = 'center'; var mc = ctx.measureText('abc'); ctx.textAlign = 'left'; var ml = ctx.measureText('abc');"
                + " String(near(mc.actualBoundingBoxLeft, ml.width / 2 + ml.actualBoundingBoxLeft))"), "alignment uses the width with the spacing");
        Check.equal("true", js(f, "ctx.letterSpacing = '5px'; ctx.direction = 'rtl'; var hebrew = w('\\u05e9\\u05dc\\u05d5\\u05dd'); ctx.direction = 'ltr'; ctx.letterSpacing = '0px'; ctx.direction = 'rtl';"
                + " var hebrew0 = w('\\u05e9\\u05dc\\u05d5\\u05dd'); String(near(hebrew - hebrew0, 20) && near(hebrew0, baseHebrew))"),
                "right-to-left text with four glyphs gets four spacings");
    }

    /**
     * The spacings move the ink: with a vertical bar as the glyph the columns of the ink show where each glyph is. The alignment uses the spaced
     * width and the maximum width of the text condenses the spaced text as a whole.
     * <p>
     * 간격은 잉크를 옮긴다: 글리프가 세로 막대이면 잉크의 열이 각 글리프의 위치를 보여 준다. 정렬은 간격이 벌어진 너비를 쓰고 텍스트의 최대 너비는 간격이 벌어진 텍스트
     * 전체를 줄인다.
     */
    public void testInkMoves() {
        PageFixture f = CanvasBasicsTest.page("width=120 height=30");
        js(f, "ctx.font = '20px sans-serif'; ctx.textBaseline = 'alphabetic'; ctx.fillStyle = 'black';"
                + " function cols() { var d = ctx.getImageData(0, 0, 120, 30).data; var out = []; for (var x = 0; x < 120; x++) { for (var y = 0; y < 30; y++) {"
                + " if (d[(y * 120 + x) * 4 + 3] > 0) { out.push(x); break; } } } return out; }"
                + " function draw(t, x, align, max) { ctx.clearRect(0, 0, 120, 30); ctx.textAlign = align || 'left'; if (max) ctx.fillText(t, x, 25, max); else ctx.fillText(t, x, 25); return cols(); }"
                + " function first(c) { return c[0]; } function last(c) { return c[c.length - 1]; }");
        Check.equal("true", js(f, "var a = draw('II', 10); String(a.length > 0 && first(a) >= 9)"), "plain text appears at the anchor point");
        Check.equal("true", js(f, "ctx.letterSpacing = '10px'; var b = draw('II', 10); String(first(b) === first(a) && last(b) === last(a) + 10)"),
                "the first I stays and the second I moves right by exactly 10 pixels");
        Check.equal("true", js(f, "ctx.letterSpacing = '0px'; ctx.wordSpacing = '0px'; var c = draw('I I', 10); ctx.wordSpacing = '12px'; var d = draw('I I', 10);"
                + " String(first(d) === first(c) && last(d) === last(c) + 12)"), "the word spacing moves only what comes after the space");
        Check.equal("true", js(f, "ctx.wordSpacing = '0px'; var r0 = draw('II', 100, 'right'); ctx.letterSpacing = '10px'; var r10 = draw('II', 100, 'right');"
                + " String(first(r10) === first(r0) - 20 && last(r10) === last(r0) - 10)"),
                "right aligned: the width grew by 20 (the spacing after the last glyph counts), so the start moves left by 20 and the end by 10");
        Check.equal("true", js(f, "ctx.letterSpacing = '0px'; var m0 = draw('II', 60, 'center'); ctx.letterSpacing = '10px'; var m10 = draw('II', 60, 'center');"
                + " String(first(m10) === first(m0) - 10 && last(m10) === last(m0))"), "centered: the start moves left by half of 20, the end stays");
        Check.equal("true", js(f, "ctx.letterSpacing = '10px'; var s = draw('IIII', 10, 'left', 40); String(last(s) <= 10 + 40 + 1 && first(s) >= 9 && last(s) > 30)"),
                "maxWidth condenses the whole spaced text into 40 pixels");
    }

    /**
     * <code>fontKerning = "normal"</code> asks for kerning: the width of a text can only get narrower or stay the same. The effect depends on the
     * kerning pairs that the layout engine of Java finds in the font: on Windows the family Tahoma has them (verified there: the text gets about
     * 8% narrower), Arial and Times New Roman do not give any through Java2D, so the strict check runs only when the family Tahoma is present.
     * <code>auto</code> and <code>none</code> change nothing.
     * <p>
     * <code>fontKerning = "normal"</code> 은 커닝을 요청한다: 텍스트의 너비는 좁아지거나 그대로일 수 있다. 효과는 Java 의 배치 엔진이 글꼴에서 찾는 커닝 쌍에 달려 있다:
     * Windows 의 Tahoma 패밀리에는 있어서(거기서 확인함: 텍스트가 약 8% 좁아진다) Arial 과 Times New Roman 은 Java2D 를 거치면 얻지 못하므로, 엄격한 검사는 Tahoma 가
     * 있을 때만 한다. <code>auto</code> 와 <code>none</code> 은 아무것도 바꾸지 않는다.
     */
    public void testKerning() {
        PageFixture f = CanvasBasicsTest.page("width=20 height=20");
        js(f, "ctx.font = '60px sans-serif'; var text = 'AVAVAVToTo'; ctx.fontKerning = 'none'; var wNone = ctx.measureText(text).width; ctx.fontKerning = 'auto'; var wAuto = ctx.measureText(text).width;"
                + " ctx.fontKerning = 'normal'; var wNormal = ctx.measureText(text).width;");
        Check.equal("true", js(f, "String(wNone === wAuto)"), "auto and none give the same width");
        Check.equal("true", js(f, "String(wNormal <= wNone + 1e-9 && wNormal > wNone * 0.5)"), "normal never widens the text (none " + js(f, "wNone") + ", normal " + js(f, "wNormal") + ")");
        js(f, "function tw(family, kerning) { ctx.font = '60px ' + family; ctx.fontKerning = kerning; return ctx.measureText(text).width; }"
                + " var tahoma = tw('Tahoma', 'none') !== tw('NoSuchFamilyForSure', 'none');");
        // Tahoma 가 설치된 환경(Windows)에서는 커닝 쌍이 있어 실제로 좁아지는지 엄격하게 확인한다.
        if ("true".equals(js(f, "String(tahoma)"))) {
            if (CanvasText.KERNING_SUPPORTED) {
                Check.equal("true", js(f, "String(tw('Tahoma', 'normal') < tw('Tahoma', 'none') - 5 && tw('Tahoma', 'auto') === tw('Tahoma', 'none'))"),
                        "with a font that has kerning pairs normal kerning narrows the text and auto does not (Tahoma: none " + js(f, "tw('Tahoma', 'none')") + ", normal "
                        + js(f, "tw('Tahoma', 'normal')") + ")");
            } else {
                // JRE 6 은 커닝 요청을 무시한다(배치 엔진이 JVM 을 죽이거나 엉뚱한 너비를 돌려주기 때문). 요청해도 너비가 같고 아무 일도 없어야 한다.
                Check.equal("true", js(f, "String(tw('Tahoma', 'normal') === tw('Tahoma', 'none'))"),
                        "on JRE 6 the kerning request is ignored: the layout engine of JRE 6u45 is not reliable with it (JVM crash, wrong widths)");
            }
        }
        Check.equal("true", js(f, "ctx.fontKerning = 'normal'; ctx.font = '20px sans-serif'; String(ctx.measureText('').width === 0 && ctx.measureText('A').width > 0)"), "empty text and a single character");
        Check.equal("true", js(f, "ctx.fontKerning = 'normal'; ctx.letterSpacing = '4px'; var ws = ctx.measureText('AV').width; ctx.letterSpacing = '0px'; var wo = ctx.measureText('AV').width;"
                + " String(Math.abs(ws - wo - 8) < 1e-6)"), "kerning and letter spacing work together");
        Check.equal("true", js(f, "ctx.fontKerning = 'normal'; ctx.font = '20px sans-serif'; ctx.fillText('AVAVTo', 2, 18); String(ctx.measureText('\\u05e9\\u05dc\\u05d5\\u05dd').width > 0)"),
                "drawing and measuring with kerning on do not break (Hebrew text is never given to the layout engine)");
    }
}
