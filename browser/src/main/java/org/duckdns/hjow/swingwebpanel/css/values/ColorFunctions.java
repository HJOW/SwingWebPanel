package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The color functions of CSS Color Level 3 and 4 and the color space conversions behind them. Every function is converted to sRGB; values outside
 * the sRGB gamut are clipped channel by channel, which is what simple implementations do and good enough for a screen without wide gamut support.
 * <p>
 * CSS Color Level 3 와 4 의 색 함수와 그 뒤의 색 공간 변환. 모든 함수를 sRGB 로 바꾸며 sRGB 범위를 벗어난 값은 채널별로 잘라 낸다. 단순한 구현이 하는
 * 방식이며 넓은 색 영역을 지원하지 않는 화면에는 충분하다.
 */
final class ColorFunctions {

    /**
     * One numeric component of a color function.
     * <p>
     * 색 함수의 숫자 구성 요소 하나.
     */
    private static final class Comp {
        /** The number (degrees for angles). / 숫자(각도는 도). */
        final double v;
        /** True for percentages. / 백분율이면 true. */
        final boolean pct;
        /** True for the keyword none. / 키워드 none 이면 true. */
        final boolean none;
        /** True for angle dimensions. / 각도 차원이면 true. */
        final boolean angle;

        /**
         * Creates a component.
         * <p>
         * 구성 요소를 만든다.
         *
         * @param v     number / 숫자
         * @param pct   percentage flag / 백분율 플래그
         * @param none  none flag / none 플래그
         * @param angle angle flag / 각도 플래그
         */
        Comp(double v, boolean pct, boolean none, boolean angle) {
            this.v = v;
            this.pct = pct;
            this.none = none;
            this.angle = angle;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ColorFunctions() {
    }

    /**
     * Converts one component value to a component.
     * <p>
     * 컴포넌트 값 하나를 구성 요소로 바꾼다.
     *
     * @param c component value / 컴포넌트 값
     * @return the component or null if it is not numeric / 구성 요소. 숫자가 아니면 null
     */
    private static Comp comp(ComponentValue c) {
        if (!(c instanceof CssToken)) {
            return null;
        }
        CssToken t = (CssToken) c;
        if (t.is(CssToken.NUMBER)) {
            return new Comp(t.getNumber(), false, false, false);
        }
        if (t.is(CssToken.PERCENTAGE)) {
            return new Comp(t.getNumber(), true, false, false);
        }
        if (t.is(CssToken.DIMENSION) && CssUnits.kindOf(t.getUnit()) == CssUnits.ANGLE) {
            return new Comp(CssUnits.angleToDegrees(t.getNumber(), CssKeyword.lower(t.getUnit())), false, false, true);
        }
        if (t.isIdent("none")) {
            return new Comp(0, false, true, false);
        }
        return null;
    }

    /**
     * Reads the components of a function: three values and an optional alpha, in the legacy comma form or the modern space form.
     * <p>
     * 함수의 구성 요소를 읽는다: 값 셋과 선택적 알파. 이전의 쉼표 형태나 새 공백 형태.
     *
     * @param args   function arguments / 함수 인수
     * @param legacy receives true if the comma form was used (array of one) / 쉼표 형태를 썼으면 true 를 받는다(원소 하나짜리 배열)
     * @return four components (the alpha may be null), or null if invalid / 구성 요소 넷(알파는 null 일 수 있음). 틀리면 null
     */
    private static Comp[] read(List<ComponentValue> args, boolean[] legacy) {
        List<ComponentValue> items = CssColor.items(args);
        boolean comma = false;
        // 쉼표가 있으면 이전 형태이다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof CssToken && ((CssToken) items.get(i)).is(CssToken.COMMA)) {
                comma = true;
            }
        }
        legacy[0] = comma;
        Comp[] out = new Comp[4];
        if (comma) {
            List<List<ComponentValue>> parts = CssParser.splitCommas(items);
            if (parts.size() != 3 && parts.size() != 4) {
                return null;
            }
            // 쉼표로 나눈 조각이 각각 숫자 하나여야 한다.
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).size() != 1 || (out[i] = comp(parts.get(i).get(0))) == null || out[i].none) {
                    return null;
                }
            }
            return out;
        }
        int n = items.size();
        if (n != 3 && n != 5 && n != 4) {
            return null;
        }
        // 값 셋을 읽는다.
        for (int i = 0; i < 3; i++) {
            out[i] = comp(items.get(i));
            if (out[i] == null) {
                return null;
            }
        }
        if (n == 5) {
            if (!(items.get(3) instanceof CssToken) || !((CssToken) items.get(3)).isDelim('/')) {
                return null;
            }
            out[3] = comp(items.get(4));
            return out[3] == null ? null : out;
        }
        return n == 3 ? out : null;
    }

    /**
     * Converts the alpha component to 0..255.
     * <p>
     * 알파 구성 요소를 0..255 로 바꾼다.
     *
     * @param a alpha component or null / 알파 구성 요소 또는 null
     * @return 0..255 / 0..255
     */
    private static int alpha(Comp a) {
        if (a == null) {
            return 255;
        }
        double v = a.none ? 0 : a.pct ? a.v / 100.0 : a.v;
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 255.0);
    }

    /**
     * Parses a color function.
     * <p>
     * 색 함수를 파싱한다.
     *
     * @param f the function / 함수
     * @return the color, or null if the function is not a valid color / 색. 올바른 색이 아니면 null
     */
    static CssValue parse(CssFunction f) {
        String n = CssKeyword.lower(f.getName());
        List<ComponentValue> a = f.getArguments();
        if (n.equals("rgb") || n.equals("rgba")) {
            return rgb(a);
        }
        if (n.equals("hsl") || n.equals("hsla")) {
            return hsl(a);
        }
        if (n.equals("hwb")) {
            return hwb(a);
        }
        if (n.equals("lab") || n.equals("lch") || n.equals("oklab") || n.equals("oklch")) {
            return labLike(n, a);
        }
        if (n.equals("color")) {
            return color(a);
        }
        if (n.equals("light-dark")) {
            return lightDark(a);
        }
        if (n.equals("color-mix")) {
            return mix(a);
        }
        return null;
    }

    /**
     * Parses <code>rgb()</code> and <code>rgba()</code>.
     * <p>
     * <code>rgb()</code> 와 <code>rgba()</code> 를 파싱한다.
     *
     * @param args arguments / 인수
     * @return the color or null / 색 또는 null
     */
    private static CssColor rgb(List<ComponentValue> args) {
        boolean[] legacy = new boolean[1];
        Comp[] c = read(args, legacy);
        if (c == null) {
            return null;
        }
        // 이전 형태는 세 값이 모두 숫자이거나 모두 백분율이어야 한다.
        if (legacy[0] && (c[0].pct != c[1].pct || c[1].pct != c[2].pct)) {
            return null;
        }
        int[] ch = new int[3];
        // 세 채널을 0..255 정수로 바꾸며 각도 값은 거부한다.
        for (int i = 0; i < 3; i++) {
            if (c[i].angle) {
                return null;
            }
            double v = c[i].none ? 0 : c[i].pct ? c[i].v * 2.55 : c[i].v;
            ch[i] = (int) Math.round(Math.max(0, Math.min(255, v)));
        }
        return CssColor.rgba(ch[0], ch[1], ch[2], alpha(c[3]));
    }

    /**
     * Normalizes a hue component to degrees in [0, 360).
     * <p>
     * 색상 구성 요소를 [0, 360) 도로 정규화한다.
     *
     * @param c component / 구성 요소
     * @return degrees or NaN if the component is not a valid hue / 도. 올바른 색상이 아니면 NaN
     */
    private static double hue(Comp c) {
        if (c.pct) {
            return Double.NaN;
        }
        double h = c.none ? 0 : c.v;
        h = h % 360.0;
        return h < 0 ? h + 360.0 : h;
    }

    /**
     * Returns a 0..1 fraction of a percentage-or-number component.
     * <p>
     * 백분율 또는 숫자 구성 요소의 0..1 비율을 반환한다.
     *
     * @param c        component / 구성 요소
     * @param legacy   true if only percentages are allowed / 백분율만 허용하면 true
     * @return the fraction or NaN if invalid / 비율. 틀리면 NaN
     */
    private static double fraction(Comp c, boolean legacy) {
        if (c.angle || (legacy && !c.pct && !c.none)) {
            return Double.NaN;
        }
        return c.none ? 0 : c.v / 100.0;
    }

    /**
     * Parses <code>hsl()</code> and <code>hsla()</code>.
     * <p>
     * <code>hsl()</code> 와 <code>hsla()</code> 를 파싱한다.
     *
     * @param args arguments / 인수
     * @return the color or null / 색 또는 null
     */
    private static CssColor hsl(List<ComponentValue> args) {
        boolean[] legacy = new boolean[1];
        Comp[] c = read(args, legacy);
        if (c == null) {
            return null;
        }
        double h = hue(c[0]);
        double s = fraction(c[1], legacy[0]);
        double l = fraction(c[2], legacy[0]);
        if (Double.isNaN(h) || Double.isNaN(s) || Double.isNaN(l)) {
            return null;
        }
        s = Math.max(0, Math.min(1, s));
        l = Math.max(0, Math.min(1, l));
        double[] rgb = hslToRgb(h, s, l);
        return CssColor.rgba(to8(rgb[0]), to8(rgb[1]), to8(rgb[2]), alpha(c[3]));
    }

    /**
     * Converts HSL to sRGB (all channels 0..1).
     * <p>
     * HSL 을 sRGB 로 바꾼다(모든 채널 0..1).
     *
     * @param h hue in degrees / 색상(도)
     * @param s saturation 0..1 / 채도 0..1
     * @param l lightness 0..1 / 명도 0..1
     * @return red, green, blue / 빨강, 초록, 파랑
     */
    static double[] hslToRgb(double h, double s, double l) {
        double a = s * Math.min(l, 1 - l);
        double[] out = new double[3];
        // 세 채널을 각각 색상 오프셋 0, 8, 4 로 계산한다.
        for (int i = 0; i < 3; i++) {
            int[] off = {0, 8, 4};
            double k = (off[i] + h / 30.0) % 12.0;
            out[i] = l - a * Math.max(-1, Math.min(Math.min(k - 3, 9 - k), 1));
        }
        return out;
    }

    /**
     * Rounds a 0..1 channel to 0..255.
     * <p>
     * 0..1 채널을 0..255 로 반올림한다.
     *
     * @param v channel / 채널
     * @return 0..255 / 0..255
     */
    private static int to8(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 255.0);
    }

    /**
     * Parses <code>hwb()</code>.
     * <p>
     * <code>hwb()</code> 를 파싱한다.
     *
     * @param args arguments / 인수
     * @return the color or null / 색 또는 null
     */
    private static CssColor hwb(List<ComponentValue> args) {
        boolean[] legacy = new boolean[1];
        Comp[] c = read(args, legacy);
        if (c == null || legacy[0]) {
            return null;
        }
        double h = hue(c[0]);
        double w = fraction(c[1], false);
        double b = fraction(c[2], false);
        if (Double.isNaN(h) || Double.isNaN(w) || Double.isNaN(b)) {
            return null;
        }
        w = Math.max(0, w);
        b = Math.max(0, b);
        if (w + b >= 1) {
            double g = w / (w + b);
            return CssColor.rgba(to8(g), to8(g), to8(g), alpha(c[3]));
        }
        double[] rgb = hslToRgb(h, 1, 0.5);
        // 순수한 색에 흰색과 검은색 비율을 섞는다.
        for (int i = 0; i < 3; i++) {
            rgb[i] = rgb[i] * (1 - w - b) + w;
        }
        return CssColor.rgba(to8(rgb[0]), to8(rgb[1]), to8(rgb[2]), alpha(c[3]));
    }

    /**
     * Applies the sRGB transfer function (linear to encoded).
     * <p>
     * sRGB 전달 함수를 적용한다(선형에서 부호화로).
     *
     * @param v linear value / 선형 값
     * @return encoded value / 부호화한 값
     */
    private static double encode(double v) {
        double a = Math.abs(v);
        double r = a <= 0.0031308 ? 12.92 * a : 1.055 * Math.pow(a, 1 / 2.4) - 0.055;
        return v < 0 ? -r : r;
    }

    /**
     * Applies the inverse sRGB transfer function (encoded to linear).
     * <p>
     * sRGB 역전달 함수를 적용한다(부호화에서 선형으로).
     *
     * @param v encoded value / 부호화한 값
     * @return linear value / 선형 값
     */
    private static double decode(double v) {
        double a = Math.abs(v);
        double r = a <= 0.04045 ? a / 12.92 : Math.pow((a + 0.055) / 1.055, 2.4);
        return v < 0 ? -r : r;
    }

    /**
     * Converts XYZ (D65) to linear sRGB.
     * <p>
     * XYZ(D65)를 선형 sRGB 로 바꾼다.
     *
     * @param x X / X
     * @param y Y / Y
     * @param z Z / Z
     * @return linear red, green, blue / 선형 빨강, 초록, 파랑
     */
    private static double[] xyzD65ToLinear(double x, double y, double z) {
        return new double[] {3.2404542 * x - 1.5371385 * y - 0.4985314 * z, -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z};
    }

    /**
     * Converts XYZ (D50) to linear sRGB through the Bradford adaptation.
     * <p>
     * XYZ(D50)를 브래드포드 적응으로 선형 sRGB 로 바꾼다.
     *
     * @param x X / X
     * @param y Y / Y
     * @param z Z / Z
     * @return linear red, green, blue / 선형 빨강, 초록, 파랑
     */
    private static double[] xyzD50ToLinear(double x, double y, double z) {
        double x65 = 0.9554734527042182 * x - 0.023098536874261423 * y + 0.0632593086610217 * z;
        double y65 = -0.028369706963208136 * x + 1.0099954580058226 * y + 0.021041398966943008 * z;
        double z65 = 0.012314001688319899 * x - 0.020507696433477912 * y + 1.3303659366080753 * z;
        return xyzD65ToLinear(x65, y65, z65);
    }

    /**
     * Builds a color from linear sRGB channels.
     * <p>
     * 선형 sRGB 채널로 색을 만든다.
     *
     * @param lin   linear channels / 선형 채널
     * @param alpha alpha 0..255 / 알파 0..255
     * @return the color / 색
     */
    private static CssColor fromLinear(double[] lin, int alpha) {
        return CssColor.rgba(to8(encode(lin[0])), to8(encode(lin[1])), to8(encode(lin[2])), alpha);
    }

    /**
     * Converts CIE Lab (D50) to XYZ (D50).
     * <p>
     * CIE Lab(D50)을 XYZ(D50)으로 바꾼다.
     *
     * @param l lightness / 명도
     * @param a a axis / a 축
     * @param b b axis / b 축
     * @return x, y, z / x, y, z
     */
    private static double[] labToXyzD50(double l, double a, double b) {
        double eps = 216.0 / 24389.0;
        double kappa = 24389.0 / 27.0;
        double fy = (l + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - b / 200.0;
        double xr = fx * fx * fx > eps ? fx * fx * fx : (116.0 * fx - 16.0) / kappa;
        double yr = l > kappa * eps ? Math.pow((l + 16.0) / 116.0, 3) : l / kappa;
        double zr = fz * fz * fz > eps ? fz * fz * fz : (116.0 * fz - 16.0) / kappa;
        return new double[] {xr * 0.3457 / 0.3585, yr, zr * (1.0 - 0.3457 - 0.3585) / 0.3585};
    }

    /**
     * Converts Oklab to linear sRGB.
     * <p>
     * Oklab 을 선형 sRGB 로 바꾼다.
     *
     * @param l lightness / 명도
     * @param a a axis / a 축
     * @param b b axis / b 축
     * @return linear red, green, blue / 선형 빨강, 초록, 파랑
     */
    private static double[] oklabToLinear(double l, double a, double b) {
        double l1 = l + 0.3963377774 * a + 0.2158037573 * b;
        double m1 = l - 0.1055613458 * a - 0.0638541728 * b;
        double s1 = l - 0.0894841775 * a - 1.2914855480 * b;
        double l3 = l1 * l1 * l1;
        double m3 = m1 * m1 * m1;
        double s3 = s1 * s1 * s1;
        return new double[] {4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
            -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3, -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3};
    }

    /**
     * Converts linear sRGB to Oklab.
     * <p>
     * 선형 sRGB 를 Oklab 으로 바꾼다.
     *
     * @param r linear red / 선형 빨강
     * @param g linear green / 선형 초록
     * @param b linear blue / 선형 파랑
     * @return L, a, b / L, a, b
     */
    private static double[] linearToOklab(double r, double g, double b) {
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[] {0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s, 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s};
    }

    /**
     * Parses <code>lab()</code>, <code>lch()</code>, <code>oklab()</code> and <code>oklch()</code>.
     * <p>
     * <code>lab()</code>, <code>lch()</code>, <code>oklab()</code>, <code>oklch()</code> 를 파싱한다.
     *
     * @param name function name in lower case / 소문자 함수 이름
     * @param args arguments / 인수
     * @return the color or null / 색 또는 null
     */
    private static CssColor labLike(String name, List<ComponentValue> args) {
        boolean[] legacy = new boolean[1];
        Comp[] c = read(args, legacy);
        if (c == null || legacy[0]) {
            return null;
        }
        boolean ok = name.startsWith("ok");
        boolean polar = name.endsWith("ch");
        if (c[0].angle || c[1].angle || (polar ? c[2].pct : c[2].angle)) {
            return null;
        }
        double l = c[0].none ? 0 : c[0].pct ? c[0].v / 100.0 * (ok ? 1.0 : 100.0) : c[0].v;
        double second = c[1].none ? 0 : c[1].pct ? c[1].v / 100.0 * (polar ? (ok ? 0.4 : 150.0) : (ok ? 0.4 : 125.0)) : c[1].v;
        double third;
        if (polar) {
            double h = hue(c[2]) * Math.PI / 180.0;
            third = Math.sin(h) * second;
            second = Math.cos(h) * second;
        } else {
            third = c[2].none ? 0 : c[2].pct ? c[2].v / 100.0 * (ok ? 0.4 : 125.0) : c[2].v;
        }
        double[] lin;
        if (ok) {
            lin = oklabToLinear(Math.max(0, Math.min(1, l)), second, third);
        } else {
            double[] xyz = labToXyzD50(Math.max(0, Math.min(100, l)), second, third);
            lin = xyzD50ToLinear(xyz[0], xyz[1], xyz[2]);
        }
        return fromLinear(lin, alpha(c[3]));
    }

    /**
     * Parses <code>color()</code> with a predefined color space.
     * <p>
     * 미리 정의된 색 공간을 쓰는 <code>color()</code> 를 파싱한다.
     *
     * @param args arguments / 인수
     * @return the color or null / 색 또는 null
     */
    private static CssColor color(List<ComponentValue> args) {
        List<ComponentValue> items = CssColor.items(args);
        if (items.size() < 4 || !(items.get(0) instanceof CssToken) || !((CssToken) items.get(0)).is(CssToken.IDENT)) {
            return null;
        }
        String space = CssKeyword.lower(((CssToken) items.get(0)).getValue());
        List<ComponentValue> rest = items.subList(1, items.size());
        Comp[] c = new Comp[4];
        int n = rest.size();
        if (n != 3 && n != 5) {
            return null;
        }
        // 값 셋을 읽는다(숫자 또는 백분율, 100% 가 1).
        for (int i = 0; i < 3; i++) {
            c[i] = comp(rest.get(i));
            if (c[i] == null || c[i].angle) {
                return null;
            }
        }
        if (n == 5) {
            if (!(rest.get(3) instanceof CssToken) || !((CssToken) rest.get(3)).isDelim('/') || (c[3] = comp(rest.get(4))) == null) {
                return null;
            }
        }
        double[] v = new double[3];
        // 백분율은 100% 를 1 로 환산한다.
        for (int i = 0; i < 3; i++) {
            v[i] = c[i].none ? 0 : c[i].pct ? c[i].v / 100.0 : c[i].v;
        }
        int a = alpha(c[3]);
        if (space.equals("srgb")) {
            return CssColor.rgba(to8(v[0]), to8(v[1]), to8(v[2]), a);
        }
        if (space.equals("srgb-linear")) {
            return fromLinear(v, a);
        }
        if (space.equals("display-p3")) {
            double[] x = mul(P3_TO_XYZ, decodeAll(v));
            return fromLinear(xyzD65ToLinear(x[0], x[1], x[2]), a);
        }
        if (space.equals("a98-rgb")) {
            double[] lin = new double[3];
            // a98-rgb 는 감마 563/256 을 쓴다.
            for (int i = 0; i < 3; i++) {
                lin[i] = Math.signum(v[i]) * Math.pow(Math.abs(v[i]), 563.0 / 256.0);
            }
            double[] x = mul(A98_TO_XYZ, lin);
            return fromLinear(xyzD65ToLinear(x[0], x[1], x[2]), a);
        }
        if (space.equals("prophoto-rgb")) {
            double[] lin = new double[3];
            // prophoto-rgb 는 감마 1.8 에 선형 구간이 있다.
            for (int i = 0; i < 3; i++) {
                double m = Math.abs(v[i]);
                lin[i] = Math.signum(v[i]) * (m <= 16.0 / 512.0 ? m / 16.0 : Math.pow(m, 1.8));
            }
            double[] x = mul(PROPHOTO_TO_XYZ, lin);
            return fromLinear(xyzD50ToLinear(x[0], x[1], x[2]), a);
        }
        if (space.equals("rec2020")) {
            double[] lin = new double[3];
            double alpha = 1.09929682680944;
            double beta = 0.018053968510807;
            // rec2020 의 전달 함수를 역으로 적용한다.
            for (int i = 0; i < 3; i++) {
                double m = Math.abs(v[i]);
                lin[i] = Math.signum(v[i]) * (m < beta * 4.5 ? m / 4.5 : Math.pow((m + alpha - 1) / alpha, 1 / 0.45));
            }
            double[] x = mul(REC2020_TO_XYZ, lin);
            return fromLinear(xyzD65ToLinear(x[0], x[1], x[2]), a);
        }
        if (space.equals("xyz") || space.equals("xyz-d65")) {
            return fromLinear(xyzD65ToLinear(v[0], v[1], v[2]), a);
        }
        if (space.equals("xyz-d50")) {
            return fromLinear(xyzD50ToLinear(v[0], v[1], v[2]), a);
        }
        return null;
    }

    /** Display P3 linear RGB to XYZ D65. / Display P3 선형 RGB 에서 XYZ D65 로. */
    private static final double[][] P3_TO_XYZ = {{0.4865709486482162, 0.26566769316909306, 0.1982172852343625},
        {0.2289745640697488, 0.6917385218365064, 0.079286914093745}, {0.0, 0.04511338185890264, 1.043944368900976}};

    /** A98 RGB linear to XYZ D65. / A98 RGB 선형에서 XYZ D65 로. */
    private static final double[][] A98_TO_XYZ = {{0.5766690429101305, 0.1855582379065463, 0.1882286462349947},
        {0.29734497525053605, 0.6273635662554661, 0.0752914584939979}, {0.027031361386412366, 0.07068885253582723, 0.9913375368376388}};

    /** ProPhoto RGB linear to XYZ D50. / ProPhoto RGB 선형에서 XYZ D50 으로. */
    private static final double[][] PROPHOTO_TO_XYZ = {{0.7977604896723027, 0.13518583717574031, 0.0313493495815248},
        {0.2880711282292934, 0.7118432178101014, 0.00008565396060525902}, {0.0, 0.0, 0.8251046025104601}};

    /** Rec. 2020 linear to XYZ D65. / Rec. 2020 선형에서 XYZ D65 로. */
    private static final double[][] REC2020_TO_XYZ = {{0.6369580483012914, 0.14461690358620832, 0.1688809751641721},
        {0.2627002120112671, 0.6779980715188708, 0.05930171646986196}, {0.0, 0.028072693049087428, 1.0609850577107909}};

    /**
     * Decodes the sRGB transfer function of three channels.
     * <p>
     * 세 채널에 sRGB 전달 함수를 역으로 적용한다.
     *
     * @param v encoded channels / 부호화한 채널
     * @return linear channels / 선형 채널
     */
    private static double[] decodeAll(double[] v) {
        return new double[] {decode(v[0]), decode(v[1]), decode(v[2])};
    }

    /**
     * Multiplies a 3x3 matrix with a vector.
     * <p>
     * 3x3 행렬과 벡터를 곱한다.
     *
     * @param m matrix / 행렬
     * @param v vector / 벡터
     * @return the product / 곱
     */
    private static double[] mul(double[][] m, double[] v) {
        double[] r = new double[3];
        // 행마다 내적을 구한다.
        for (int i = 0; i < 3; i++) {
            r[i] = m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2];
        }
        return r;
    }

    /**
     * Parses <code>light-dark()</code>: the color that depends on the color scheme of the element (see {@link CssColor#lightDark}); with <code>currentcolor</code> the light value is used.
     * <p>
     * <code>light-dark()</code> 를 파싱한다: 요소의 색 구성표에 따라 고르는 색({@link CssColor#lightDark} 참조). <code>currentcolor</code> 가 끼면 밝은 값을 쓴다.
     *
     * @param args arguments / 인수
     * @return the light color or null / 밝은 색 또는 null
     */
    private static CssValue lightDark(List<ComponentValue> args) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(args);
        if (parts.size() != 2 || parts.get(0).size() != 1 || parts.get(1).size() != 1) {
            return null;
        }
        CssValue light = CssColor.parseValue(parts.get(0).get(0));
        CssValue dark = CssColor.parseValue(parts.get(1).get(0));
        if (light == null || dark == null) {
            return null;
        }
        // 두 색이 모두 색이면 색 구성표에 따라 고르는 색으로 둔다(currentcolor 가 끼면 밝은 값을 쓴다).
        return light instanceof CssColor && dark instanceof CssColor ? CssColor.lightDark((CssColor) light, (CssColor) dark) : light;
    }

    /**
     * Parses <code>color-mix()</code> in the sRGB, linear sRGB and Oklab spaces with premultiplied alpha.
     * <p>
     * sRGB, 선형 sRGB, Oklab 공간의 <code>color-mix()</code> 를 알파 사전 곱셈으로 파싱한다.
     *
     * @param args arguments / 인수
     * @return the mixed color or null / 섞은 색 또는 null
     */
    private static CssColor mix(List<ComponentValue> args) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(args);
        if (parts.size() != 3) {
            return null;
        }
        List<ComponentValue> head = CssColor.items(parts.get(0));
        if (head.size() != 2 || !(head.get(0) instanceof CssToken) || !((CssToken) head.get(0)).isIdent("in")
                || !(head.get(1) instanceof CssToken) || !((CssToken) head.get(1)).is(CssToken.IDENT)) {
            return null;
        }
        String space = CssKeyword.lower(((CssToken) head.get(1)).getValue());
        if (!space.equals("srgb") && !space.equals("srgb-linear") && !space.equals("oklab")) {
            return null;
        }
        CssColor[] col = new CssColor[2];
        double[] pct = {Double.NaN, Double.NaN};
        // 두 색과 선택적인 백분율을 읽는다.
        for (int k = 0; k < 2; k++) {
            List<ComponentValue> it = CssColor.items(parts.get(k + 1));
            List<ComponentValue> rest = new ArrayList<ComponentValue>();
            // 백분율 토큰은 따로 떼고 나머지가 색이다.
            for (int i = 0; i < it.size(); i++) {
                if (it.get(i) instanceof CssToken && ((CssToken) it.get(i)).is(CssToken.PERCENTAGE)) {
                    if (!Double.isNaN(pct[k])) {
                        return null;
                    }
                    pct[k] = ((CssToken) it.get(i)).getNumber();
                } else {
                    rest.add(it.get(i));
                }
            }
            if (rest.size() != 1) {
                return null;
            }
            CssValue cv = CssColor.parseValue(rest.get(0));
            if (!(cv instanceof CssColor) || pct[k] < 0) {
                return null;
            }
            col[k] = (CssColor) cv;
        }
        double p1 = pct[0];
        double p2 = pct[1];
        if (Double.isNaN(p1) && Double.isNaN(p2)) {
            p1 = 50;
            p2 = 50;
        } else if (Double.isNaN(p1)) {
            p1 = 100 - p2;
        } else if (Double.isNaN(p2)) {
            p2 = 100 - p1;
        }
        double sum = p1 + p2;
        if (sum <= 0) {
            return null;
        }
        double w1 = p1 / sum;
        double w2 = p2 / sum;
        double alphaMul = Math.min(1.0, sum / 100.0);
        double a1 = col[0].getAlpha() / 255.0;
        double a2 = col[1].getAlpha() / 255.0;
        double[] c1 = toSpace(col[0], space);
        double[] c2 = toSpace(col[1], space);
        double alpha = a1 * w1 + a2 * w2;
        double[] m = new double[3];
        // 알파를 미리 곱한 값으로 보간한 뒤 알파로 나눈다.
        for (int i = 0; i < 3; i++) {
            m[i] = alpha == 0 ? 0 : (c1[i] * a1 * w1 + c2[i] * a2 * w2) / alpha;
        }
        int outAlpha = (int) Math.round(alpha * alphaMul * 255.0);
        if (space.equals("srgb")) {
            return CssColor.rgba(to8(m[0]), to8(m[1]), to8(m[2]), outAlpha);
        }
        if (space.equals("srgb-linear")) {
            return fromLinear(m, outAlpha);
        }
        return fromLinear(oklabToLinear(m[0], m[1], m[2]), outAlpha);
    }

    /**
     * Converts a color to the channels of a mixing space.
     * <p>
     * 색을 섞는 공간의 채널로 바꾼다.
     *
     * @param c     the color / 색
     * @param space srgb, srgb-linear or oklab / srgb, srgb-linear 또는 oklab
     * @return the three channels / 세 채널
     */
    private static double[] toSpace(CssColor c, String space) {
        double[] s = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        if (space.equals("srgb")) {
            return s;
        }
        double[] lin = decodeAll(s);
        return space.equals("srgb-linear") ? lin : linearToOklab(lin[0], lin[1], lin[2]);
    }
}
