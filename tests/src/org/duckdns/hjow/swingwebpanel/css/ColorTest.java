package org.duckdns.hjow.swingwebpanel.css;

import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.NamedColors;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of color parsing and conversion (plan item P08-05): hash colors, names, rgb/hsl/hwb/lab/lch/oklab/oklch/color()/color-mix()/light-dark(),
 * clamping, alpha and serialization.
 * <p>
 * 색 파싱과 변환 시험(계획 P08-05): 해시 색, 이름, rgb/hsl/hwb/lab/lch/oklab/oklch/color()/color-mix()/light-dark(), 범위 제한, 알파, 직렬화.
 */
public class ColorTest {

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public ColorTest() {
    }

    /**
     * Parses a color and returns it, failing the case if the text is not a color. 색을 파싱해 반환하며 색이 아니면 사례를 실패시킨다.
     *
     * @param css color text / 색 문자열
     * @return the color / 색
     */
    private static CssColor c(String css) {
        CssValue v = CssColor.parse(css);
        Check.isTrue(v instanceof CssColor, "'" + css + "' must be a color but was " + v);
        return (CssColor) v;
    }

    /**
     * Checks the channels of a color within a tolerance. 색의 채널을 허용 오차 안에서 확인한다.
     *
     * @param css color text / 색 문자열
     * @param r   red / 빨강
     * @param g   green / 초록
     * @param b   blue / 파랑
     * @param a   alpha / 알파
     * @param tol tolerance / 허용 오차
     */
    private static void is(String css, int r, int g, int b, int a, int tol) {
        CssColor x = c(css);
        Check.near(r, x.getRed(), tol, css + " red");
        Check.near(g, x.getGreen(), tol, css + " green");
        Check.near(b, x.getBlue(), tol, css + " blue");
        Check.near(a, x.getAlpha(), tol, css + " alpha");
    }

    /**
     * Hash colors in all four lengths. 네 가지 길이의 해시 색.
     */
    public void testHash() {
        is("#f00", 255, 0, 0, 255, 0);
        is("#F00A", 255, 0, 0, 170, 0);
        is("#336699", 0x33, 0x66, 0x99, 255, 0);
        is("#33669980", 0x33, 0x66, 0x99, 128, 0);
        Check.isNull(CssColor.parse("#12345"), "five digits");
        Check.isNull(CssColor.parse("#ggg"), "not hex");
        Check.isNull(CssColor.parse("#"), "empty hash");
    }

    /**
     * Named, system and special keywords. 이름 있는 색, 시스템 색, 특수 키워드.
     */
    public void testKeywords() {
        is("RED", 255, 0, 0, 255, 0);
        is("RebeccaPurple", 0x66, 0x33, 0x99, 255, 0);
        is("transparent", 0, 0, 0, 0, 0);
        is("Canvas", 255, 255, 255, 255, 0);
        Check.same(CssKeyword.CURRENTCOLOR, CssColor.parse("currentColor"), "currentcolor is the keyword");
        Check.isNull(CssColor.parse("notacolor"), "unknown identifier");
        Check.equal(0xff0000, NamedColors.lookup("red"), "table lookup");
        Check.equal(-1, NamedColors.lookup("nothing"), "unknown name");
        Check.equal("red", c("RED").toCssText(), "named colors serialize as the keyword");
        Check.equal("rgb(255, 0, 0)", c("RED").plain().toCssText(), "plain colors serialize as rgb()");
        Check.equal(0xff0000, NamedColors.lookup("grey") - NamedColors.lookup("gray") + 0xff0000, "grey and gray are the same");
    }

    /**
     * rgb() and rgba() in both syntaxes. 두 문법의 rgb() 와 rgba().
     */
    public void testRgb() {
        is("rgb(255, 0, 0)", 255, 0, 0, 255, 0);
        is("rgb(100% 0% 0% / 50%)", 255, 0, 0, 128, 1);
        is("rgba(0, 0, 0, .5)", 0, 0, 0, 128, 1);
        is("rgb(255 0 0 / .25)", 255, 0, 0, 64, 1);
        is("rgb(300, -5, 0)", 255, 0, 0, 255, 0);
        is("rgb(none 10 20)", 0, 10, 20, 255, 0);
        is("rgb(10% 20 30)", 26, 20, 30, 255, 1);
        Check.isNull(CssColor.parse("rgb(10%, 20, 30)"), "mixed numbers and percentages are invalid in the comma syntax");
        Check.isNull(CssColor.parse("rgb(1, 2)"), "too few");
        Check.isNull(CssColor.parse("rgb()"), "empty");
        Check.isNull(CssColor.parse("rgb(1 2 3 4)"), "alpha needs a slash");
        Check.isNull(CssColor.parse("rgb(1, 2, 3, 4, 5)"), "too many");
        Check.equal("rgba(255, 0, 0, 0.5)", c("rgba(255,0,0,.5)").toCssText(), "alpha serialization");
        Check.equal("rgb(255, 255, 255)", c("#fff").toCssText(), "hash serializes as rgb");
    }

    /**
     * The hsl(), hsla() and hwb() functions. hsl(), hsla(), hwb() 함수.
     */
    public void testHslAndHwb() {
        is("hsl(120, 100%, 50%)", 0, 255, 0, 255, 0);
        is("hsl(120deg 100% 25%)", 0, 128, 0, 255, 1);
        is("hsla(0, 0%, 0%, 0.5)", 0, 0, 0, 128, 1);
        is("hsl(0 100 50)", 255, 0, 0, 255, 0);
        is("hsl(-120 100% 50%)", 0, 0, 255, 255, 0);
        is("hsl(0.5turn 100% 50%)", 0, 255, 255, 255, 0);
        Check.isNull(CssColor.parse("hsl(0, 100, 50)"), "numbers are invalid in the comma syntax");
        is("hwb(0 0% 0%)", 255, 0, 0, 255, 0);
        is("hwb(0 50% 50%)", 128, 128, 128, 255, 1);
        is("hwb(120 0% 50%)", 0, 128, 0, 255, 1);
    }

    /**
     * Lab, LCH, Oklab and Oklch colors. Lab, LCH, Oklab, Oklch 색 표기.
     */
    public void testLabFamily() {
        is("lab(100 0 0)", 255, 255, 255, 255, 1);
        is("lab(0 0 0)", 0, 0, 0, 255, 0);
        is("lab(50 0 0)", 119, 119, 119, 255, 2);
        is("lch(50 0 0)", 119, 119, 119, 255, 2);
        is("lab(54.29 80.8 69.89)", 255, 0, 0, 255, 6);
        is("oklab(1 0 0)", 255, 255, 255, 255, 1);
        is("oklab(0 0 0)", 0, 0, 0, 255, 0);
        is("oklch(0.628 0.2577 29.23)", 255, 0, 0, 255, 4);
        is("oklch(100% 0 0 / 50%)", 255, 255, 255, 128, 1);
        Check.isNull(CssColor.parse("lab(1 2)"), "missing component");
        Check.isNull(CssColor.parse("lab(1, 2, 3)"), "no comma syntax");
        Check.isNull(CssColor.parse("lch(50 10 10%)"), "hue cannot be a percentage");
    }

    /**
     * The color() function with predefined spaces. 미리 정의된 공간을 쓰는 color() 함수.
     */
    public void testColorFunction() {
        is("color(srgb 1 0 0)", 255, 0, 0, 255, 0);
        is("color(srgb 100% 0% 0% / 0.5)", 255, 0, 0, 128, 1);
        is("color(srgb-linear 0.5 0.5 0.5)", 188, 188, 188, 255, 1);
        is("color(display-p3 1 1 1)", 255, 255, 255, 255, 1);
        is("color(display-p3 1 0 0)", 255, 0, 0, 255, 2);
        is("color(xyz-d65 0.9505 1 1.089)", 255, 255, 255, 255, 2);
        is("color(xyz-d50 0.9642 1 0.8249)", 255, 255, 255, 255, 2);
        is("color(a98-rgb 1 1 1)", 255, 255, 255, 255, 2);
        is("color(prophoto-rgb 1 1 1)", 255, 255, 255, 255, 2);
        is("color(rec2020 1 1 1)", 255, 255, 255, 255, 2);
        Check.isNull(CssColor.parse("color(unknown 1 1 1)"), "unknown color space");
        Check.isNull(CssColor.parse("color(srgb 1 1)"), "too few components");
    }

    /**
     * The color-mix() and light-dark() functions. color-mix() 와 light-dark() 함수.
     */
    public void testMixAndLightDark() {
        is("color-mix(in srgb, red, blue)", 128, 0, 128, 255, 1);
        is("color-mix(in srgb, red 25%, blue)", 64, 0, 191, 255, 1);
        is("color-mix(in srgb, red 100%, blue)", 255, 0, 0, 255, 0);
        is("color-mix(in srgb, red 30%, blue 30%)", 128, 0, 128, 153, 1);
        is("color-mix(in srgb, rgb(255 0 0 / 0.5), blue)", 85, 0, 170, 191, 2);
        is("color-mix(in oklab, white, black)", 99, 99, 99, 255, 40);
        Check.isNull(CssColor.parse("color-mix(in hsl, red, blue)"), "unsupported mixing space");
        Check.isNull(CssColor.parse("color-mix(in srgb, red)"), "needs two colors");
        is("light-dark(red, blue)", 255, 0, 0, 255, 0);
        Check.isNull(CssColor.parse("light-dark(red)"), "needs two colors");
    }
}
