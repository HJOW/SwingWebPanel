package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * Parses and serializes the colors of the canvas API with the CSS color parser of the project: named colors, <code>#rgb</code>,
 * <code>#rrggbb</code>, <code>#rgba</code>, <code>#rrggbbaa</code>, <code>rgb()</code>, <code>rgba()</code>, <code>hsl()</code>,
 * <code>hsla()</code> and the other forms that {@link CssColor} knows. <code>currentcolor</code> is the color of the canvas element, which the
 * caller passes in. The serialization follows the rule of the canvas specification: an opaque color is <code>#rrggbb</code> in lower case,
 * any other color is <code>rgba(r, g, b, a)</code>.
 * <p>
 * 프로젝트의 CSS 색 파서로 캔버스 API 의 색을 해석하고 직렬화한다: 이름 있는 색, <code>#rgb</code>, <code>#rrggbb</code>, <code>#rgba</code>,
 * <code>#rrggbbaa</code>, <code>rgb()</code>, <code>rgba()</code>, <code>hsl()</code>, <code>hsla()</code> 와 {@link CssColor} 가 아는 그 밖의 형식.
 * <code>currentcolor</code> 는 캔버스 요소의 색이며 호출자가 넘긴다. 직렬화는 캔버스 규격의 규칙을 따른다: 불투명한 색은 소문자 <code>#rrggbb</code>,
 * 그 밖의 색은 <code>rgba(r, g, b, a)</code> 이다.
 */
public final class CanvasColors {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CanvasColors() {
    }

    /**
     * Parses a color.
     * <p>
     * 색을 해석한다.
     *
     * @param text         the color text / 색 문자열
     * @param currentColor the ARGB value that <code>currentcolor</code> means / <code>currentcolor</code> 가 뜻하는 ARGB 값
     * @return the ARGB value (<code>0xAARRGGBB</code>), or null if the text is not a color / ARGB 값(<code>0xAARRGGBB</code>). 색이 아니면 null
     */
    public static Integer parse(String text, int currentColor) {
        if (text == null) {
            return null;
        }
        CssValue v = CssColor.parse(text.trim());
        if (v instanceof CssColor) {
            return Integer.valueOf(((CssColor) v).getArgb());
        }
        // currentcolor 이면 호출자가 알려 준 색을 쓴다.
        if (v == CssKeyword.CURRENTCOLOR) {
            return Integer.valueOf(currentColor);
        }
        return null;
    }

    /**
     * Serializes a color as the canvas attributes <code>fillStyle</code>, <code>strokeStyle</code> and <code>shadowColor</code> return it.
     * <p>
     * 캔버스 속성 <code>fillStyle</code>, <code>strokeStyle</code>, <code>shadowColor</code> 가 돌려주는 형식으로 색을 직렬화한다.
     *
     * @param argb the color / 색
     * @return <code>#rrggbb</code> for an opaque color, else <code>rgba(r, g, b, a)</code> / 불투명하면 <code>#rrggbb</code>, 아니면 <code>rgba(r, g, b, a)</code>
     */
    public static String serialize(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 255) {
            String hex = Integer.toHexString(0x1000000 | (argb & 0xFFFFFF)).substring(1);
            return "#" + hex;
        }
        return "rgba(" + r + ", " + g + ", " + b + ", " + alpha(a) + ")";
    }

    /**
     * Formats an 8-bit alpha as the shortest decimal with two or three digits that maps back to the same 8-bit value.
     * <p>
     * 8비트 알파를, 같은 8비트 값으로 되돌아오는 두세 자리의 가장 짧은 소수로 만든다.
     *
     * @param a alpha 0..255 / 알파 0..255
     * @return the text, for example <code>0.5</code> / 문자열. 예를 들어 <code>0.5</code>
     */
    static String alpha(int a) {
        // 소수 둘째 자리까지로 같은 값이 되는지 확인하고, 아니면 셋째 자리까지 쓴다.
        for (int digits = 2; digits <= 3; digits++) {
            double scale = digits == 2 ? 100.0 : 1000.0;
            double rounded = Math.round(a / 255.0 * scale) / scale;
            if (Math.round(rounded * 255.0) == a) {
                return trim(rounded);
            }
        }
        return trim(Math.round(a / 255.0 * 1000.0) / 1000.0);
    }

    /**
     * Prints a number without a trailing <code>.0</code> and without trailing zeros.
     * <p>
     * 숫자를 끝의 <code>.0</code> 과 불필요한 0 없이 출력한다.
     *
     * @param d the number / 숫자
     * @return the text / 문자열
     */
    private static String trim(double d) {
        String s = Double.toString(d);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    /**
     * Returns the premultiplied-free ARGB value with the alpha scaled by a factor (used for <code>globalAlpha</code> on shadow colors).
     * <p>
     * 알파에 배율을 곱한 ARGB 값을 반환한다.
     *
     * @param argb   the color / 색
     * @param factor the factor 0..1 / 배율 0..1
     * @return the color / 색
     */
    public static int scaleAlpha(int argb, double factor) {
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
