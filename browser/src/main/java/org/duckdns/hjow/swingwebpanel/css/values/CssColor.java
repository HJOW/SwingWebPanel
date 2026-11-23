package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * An sRGB color with an alpha channel, stored as four 8-bit channels. The parser accepts every notation of CSS Color Level 3 and Level 4 that does
 * not need a context: named colors, system colors (light scheme), <code>#rgb</code> <code>#rgba</code> <code>#rrggbb</code> <code>#rrggbbaa</code>,
 * <code>rgb()</code>, <code>rgba()</code>, <code>hsl()</code>, <code>hsla()</code>, <code>hwb()</code>, <code>lab()</code>, <code>lch()</code>,
 * <code>oklab()</code>, <code>oklch()</code>, <code>color()</code> (srgb, srgb-linear, display-p3, a98-rgb, prophoto-rgb, rec2020, xyz, xyz-d50, xyz-d65),
 * <code>color-mix()</code> in sRGB and <code>light-dark()</code> (the light value). Colors outside the sRGB gamut are clipped. Relative color syntax
 * (<code>from</code>) is not supported and makes the value invalid. <code>currentcolor</code> is not a CssColor; it is the keyword
 * {@link CssKeyword#CURRENTCOLOR}.
 * <p>
 * 알파 채널이 있는 sRGB 색. 8비트 채널 넷으로 저장한다. 파서는 컨텍스트 없이 쓸 수 있는 CSS Color Level 3 와 4 의 모든 표기를 받아들인다: 이름 있는 색,
 * 시스템 색(밝은 구성표), <code>#rgb</code> <code>#rgba</code> <code>#rrggbb</code> <code>#rrggbbaa</code>, <code>rgb()</code>, <code>rgba()</code>, <code>hsl()</code>,
 * <code>hsla()</code>, <code>hwb()</code>, <code>lab()</code>, <code>lch()</code>, <code>oklab()</code>, <code>oklch()</code>, <code>color()</code>(srgb, srgb-linear, display-p3,
 * a98-rgb, prophoto-rgb, rec2020, xyz, xyz-d50, xyz-d65), sRGB 의 <code>color-mix()</code>, <code>light-dark()</code>(밝은 값). sRGB 범위 밖의 색은 잘라 낸다.
 * 상대 색 문법(<code>from</code>)은 지원하지 않으며 값을 무효로 한다. <code>currentcolor</code> 는 CssColor 가 아니라 키워드 {@link CssKeyword#CURRENTCOLOR} 이다.
 */
public final class CssColor extends CssValue {

    /** Fully transparent black. / 완전히 투명한 검정. */
    public static final CssColor TRANSPARENT = new CssColor(0, "transparent");

    /** Opaque black. / 불투명한 검정. */
    public static final CssColor BLACK = new CssColor(0xFF000000, "black");

    /** Opaque white. / 불투명한 흰색. */
    public static final CssColor WHITE = new CssColor(0xFFFFFFFF, "white");

    /** The color as 0xAARRGGBB. / 0xAARRGGBB 로 나타낸 색. */
    private final int argb;

    /** The keyword the color was written as (named color), or null. / 색이 쓰인 키워드(이름 있는 색). 아니면 null. */
    private final String keyword;

    /** The color of the dark color scheme as 0xAARRGGBB, meaningful only if {@link #dual} (the color of the light scheme is {@link #argb}). 어두운 색 구성표의 색(0xAARRGGBB). {@link #dual} 일 때만 의미가 있다(밝은 구성표의 색은 {@link #argb}). */
    private final int darkArgb;

    /** True for a color that depends on the color scheme: a system color or <code>light-dark()</code>. 색 구성표에 따라 달라지는 색(시스템 색이나 <code>light-dark()</code>)이면 true. */
    private final boolean dual;

    /**
     * Creates a color.
     * <p>
     * 색을 만든다.
     *
     * @param argb    0xAARRGGBB / 0xAARRGGBB
     * @param keyword the keyword it was written as, or null / 쓰인 키워드 또는 null
     */
    private CssColor(int argb, String keyword) {
        this(argb, keyword, argb, false);
    }

    /**
     * Creates a color that may depend on the color scheme.
     * <p>
     * 색 구성표에 따라 달라질 수 있는 색을 만든다.
     *
     * @param argb     the color of the light scheme / 밝은 구성표의 색
     * @param keyword  the keyword it was written as, or null / 쓰인 키워드 또는 null
     * @param darkArgb the color of the dark scheme / 어두운 구성표의 색
     * @param dual     true if the color depends on the scheme / 구성표에 따라 달라지면 true
     */
    private CssColor(int argb, String keyword, int darkArgb, boolean dual) {
        this.argb = argb;
        this.keyword = keyword;
        this.darkArgb = darkArgb;
        this.dual = dual;
    }

    /**
     * Creates the color <code>light-dark(light, dark)</code> (CSS Color Level 5): the light color in a light color scheme and the dark color in a dark
     * one; the scheme of an element is known when its style is computed ({@link #resolve(boolean)}).
     * <p>
     * <code>light-dark(light, dark)</code> 색을 만든다(CSS Color Level 5): 밝은 색 구성표에서는 밝은 색, 어두운 구성표에서는 어두운 색. 요소의 구성표는 스타일을 계산할 때
     * 알려진다({@link #resolve(boolean)}).
     *
     * @param light the color of the light scheme / 밝은 구성표의 색
     * @param dark  the color of the dark scheme / 어두운 구성표의 색
     * @return the color / 색
     */
    public static CssColor lightDark(CssColor light, CssColor dark) {
        return new CssColor(light.argb, null, dark.argb, light.argb != dark.argb);
    }

    /**
     * Returns the color for a color scheme: the dark color of a color that depends on the scheme in a dark scheme, the plain color otherwise.
     * <p>
     * 색 구성표에 맞는 색을 반환한다: 구성표에 따라 달라지는 색은 어두운 구성표에서 어두운 색, 그 밖에는 일반 색.
     *
     * @param darkScheme true for the dark color scheme / 어두운 색 구성표이면 true
     * @return the color without keyword spelling / 키워드 표기가 없는 색
     */
    public CssColor resolve(boolean darkScheme) {
        return dual && darkScheme ? new CssColor(darkArgb, null) : plain();
    }

    /**
     * Creates a color from channels.
     * <p>
     * 채널로 색을 만든다.
     *
     * @param argb 0xAARRGGBB / 0xAARRGGBB
     * @return the color / 색
     */
    public static CssColor of(int argb) {
        return new CssColor(argb, null);
    }

    /**
     * Creates a color from 8-bit channels (each clamped to 0..255).
     * <p>
     * 8비트 채널로 색을 만든다(각각 0..255 로 제한).
     *
     * @param r red / 빨강
     * @param g green / 초록
     * @param b blue / 파랑
     * @param a alpha / 알파
     * @return the color / 색
     */
    public static CssColor rgba(int r, int g, int b, int a) {
        return new CssColor((clamp8(a) << 24) | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b), null);
    }

    /**
     * Clamps a channel to 0..255.
     * <p>
     * 채널을 0..255 로 제한한다.
     *
     * @param v channel / 채널
     * @return clamped / 제한한 값
     */
    private static int clamp8(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /**
     * Returns the color as 0xAARRGGBB.
     * <p>
     * 색을 0xAARRGGBB 로 반환한다.
     *
     * @return packed color / 묶은 색
     */
    public int getArgb() {
        return argb;
    }

    /**
     * Returns the red channel.
     * <p>
     * 빨강 채널을 반환한다.
     *
     * @return 0..255 / 0..255
     */
    public int getRed() {
        return (argb >> 16) & 255;
    }

    /**
     * Returns the green channel.
     * <p>
     * 초록 채널을 반환한다.
     *
     * @return 0..255 / 0..255
     */
    public int getGreen() {
        return (argb >> 8) & 255;
    }

    /**
     * Returns the blue channel.
     * <p>
     * 파랑 채널을 반환한다.
     *
     * @return 0..255 / 0..255
     */
    public int getBlue() {
        return argb & 255;
    }

    /**
     * Returns the alpha channel.
     * <p>
     * 알파 채널을 반환한다.
     *
     * @return 0..255 / 0..255
     */
    public int getAlpha() {
        return (argb >>> 24) & 255;
    }

    /**
     * Returns a copy without the keyword spelling (used for computed values, which always serialize as rgb()).
     * <p>
     * 키워드 표기를 없앤 복사본을 반환한다(계산 값은 항상 rgb() 로 직렬화하므로 쓴다).
     *
     * @return the plain color / 일반 색
     */
    public CssColor plain() {
        return keyword == null && !dual ? this : new CssColor(argb, null);
    }

    /**
     * Serializes as written for named colors, else as <code>rgb()</code> or <code>rgba()</code>.
     * <p>
     * 이름 있는 색은 쓰인 그대로, 나머지는 <code>rgb()</code> 나 <code>rgba()</code> 로 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return keyword != null ? keyword : toRgbText();
    }

    /**
     * Serializes as <code>rgb()</code> or <code>rgba()</code> (the form of computed and resolved values).
     * <p>
     * <code>rgb()</code> 나 <code>rgba()</code> 로 직렬화한다(계산 값과 확정 값의 형태).
     *
     * @return CSS text / CSS 문자열
     */
    public String toRgbText() {
        int a = getAlpha();
        if (a == 255) {
            return "rgb(" + getRed() + ", " + getGreen() + ", " + getBlue() + ")";
        }
        double alpha = Math.round(a / 255.0 * 100.0) / 100.0;
        String as = alpha == Math.rint(alpha) ? Integer.toString((int) alpha) : Double.toString(alpha);
        return "rgba(" + getRed() + ", " + getGreen() + ", " + getBlue() + ", " + as + ")";
    }

    /**
     * Compares the channels.
     * <p>
     * 채널을 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssColor && ((CssColor) o).argb == argb && ((CssColor) o).dual == dual && (!dual || ((CssColor) o).darkArgb == darkArgb);
    }

    /**
     * Hash of the channels.
     * <p>
     * 채널의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return argb;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 파싱
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses a color from text.
     * <p>
     * 문자열에서 색을 파싱한다.
     *
     * @param text color text / 색 문자열
     * @return a CssColor or {@link CssKeyword#CURRENTCOLOR}, or null if the text is not a single valid color / CssColor 또는 CURRENTCOLOR. 올바른 색 하나가 아니면 null
     */
    public static CssValue parse(String text) {
        List<ComponentValue> v = CssParser.trim(CssParser.parseComponentValues(text));
        return v.size() == 1 ? parseValue(v.get(0)) : null;
    }

    /**
     * Parses one component value as a color.
     * <p>
     * 컴포넌트 값 하나를 색으로 파싱한다.
     *
     * @param v a token or function / 토큰 또는 함수
     * @return a CssColor or {@link CssKeyword#CURRENTCOLOR}, or null if it is not a color / CssColor 또는 CURRENTCOLOR. 색이 아니면 null
     */
    public static CssValue parseValue(ComponentValue v) {
        if (v instanceof CssToken) {
            CssToken t = (CssToken) v;
            if (t.is(CssToken.HASH)) {
                return parseHex(t.getValue());
            }
            if (t.is(CssToken.IDENT)) {
                return parseKeyword(t.getValue());
            }
            return null;
        }
        if (v instanceof CssFunction) {
            return ColorFunctions.parse((CssFunction) v);
        }
        return null;
    }

    /**
     * Parses a color keyword.
     * <p>
     * 색 키워드를 파싱한다.
     *
     * @param name keyword / 키워드
     * @return the color, or null / 색 또는 null
     */
    private static CssValue parseKeyword(String name) {
        String l = CssKeyword.lower(name);
        if (l.equals("currentcolor")) {
            return CssKeyword.CURRENTCOLOR;
        }
        if (l.equals("transparent")) {
            return TRANSPARENT;
        }
        int rgb = NamedColors.lookup(l);
        if (rgb >= 0) {
            return new CssColor(0xFF000000 | rgb, l);
        }
        rgb = NamedColors.lookupSystem(l);
        if (rgb >= 0) {
            // 시스템 색은 요소의 색 구성표에 따라 밝은 값이나 어두운 값이 된다.
            int dark = NamedColors.lookupSystemDark(l);
            return new CssColor(0xFF000000 | rgb, l, 0xFF000000 | dark, dark != rgb);
        }
        return null;
    }

    /**
     * Parses the digits of a hash color.
     * <p>
     * 해시 색의 숫자를 파싱한다.
     *
     * @param hex digits without the number sign / # 없는 숫자
     * @return the color, or null if the length or digits are wrong / 색. 길이나 숫자가 틀리면 null
     */
    private static CssColor parseHex(String hex) {
        int n = hex.length();
        if (n != 3 && n != 4 && n != 6 && n != 8) {
            return null;
        }
        int[] d = new int[n];
        // 모든 문자가 16진수 숫자인지 확인하며 값을 모은다.
        for (int i = 0; i < n; i++) {
            d[i] = Character.digit(hex.charAt(i), 16);
            if (d[i] < 0 || hex.charAt(i) > 127) {
                return null;
            }
        }
        int r;
        int g;
        int b;
        int a = 255;
        if (n <= 4) {
            r = d[0] * 17;
            g = d[1] * 17;
            b = d[2] * 17;
            if (n == 4) {
                a = d[3] * 17;
            }
        } else {
            r = d[0] * 16 + d[1];
            g = d[2] * 16 + d[3];
            b = d[4] * 16 + d[5];
            if (n == 8) {
                a = d[6] * 16 + d[7];
            }
        }
        return rgba(r, g, b, a);
    }

    /**
     * Splits a color function argument list into items, dropping whitespace.
     * <p>
     * 색 함수의 인수 목록을 공백을 빼고 항목으로 나눈다.
     *
     * @param args function arguments / 함수 인수
     * @return the non-whitespace component values / 공백이 아닌 컴포넌트 값
     */
    static List<ComponentValue> items(List<ComponentValue> args) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 공백 토큰을 뺀 나머지를 모은다.
        for (int i = 0; i < args.size(); i++) {
            if (!CssParser.isWhitespace(args.get(i))) {
                out.add(args.get(i));
            }
        }
        return out;
    }
}
