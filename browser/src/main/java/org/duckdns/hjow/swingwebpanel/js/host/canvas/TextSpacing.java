package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.js.NumberText;

/**
 * A CSS <code>&lt;length&gt;</code> as the canvas keeps it for <code>letterSpacing</code> and <code>wordSpacing</code>: the number and the unit
 * as they were parsed, so the getter can return the serialized form (<code>"5px"</code>) and a relative unit (<code>em</code>,
 * <code>ex</code>, <code>ch</code>, <code>rem</code>) is resolved against the font that is current when the text is drawn. The accepted units
 * are <code>px</code>, <code>pt</code>, <code>pc</code>, <code>in</code>, <code>cm</code>, <code>mm</code>, <code>Q</code>, <code>em</code>,
 * <code>ex</code>, <code>ch</code> and <code>rem</code> (<code>ex</code> and <code>ch</code> are half an em, <code>rem</code> is 16px); a plain 0
 * is 0px. Anything else (a percentage, a viewport unit, <code>calc()</code>, a number without a unit that is not 0) is not a length that this
 * parser knows, so the setter ignores it. Instances never change.
 * <p>
 * 캔버스가 <code>letterSpacing</code> 과 <code>wordSpacing</code> 에 보관하는 CSS <code>&lt;length&gt;</code>: 해석한 숫자와 단위를 그대로 가지고 있어서 getter 가 직렬화된
 * 형태(<code>"5px"</code>)를 돌려주고 상대 단위(<code>em</code>, <code>ex</code>, <code>ch</code>, <code>rem</code>)는 글자를 그릴 때의 글꼴을 기준으로 환산한다. 받는 단위는
 * <code>px</code>, <code>pt</code>, <code>pc</code>, <code>in</code>, <code>cm</code>, <code>mm</code>, <code>Q</code>, <code>em</code>, <code>ex</code>, <code>ch</code>,
 * <code>rem</code> 이다(<code>ex</code> 와 <code>ch</code> 는 em 의 절반, <code>rem</code> 은 16px). 단위 없는 0 은 0px 이다. 그 밖의 것(백분율, 뷰포트 단위,
 * <code>calc()</code>, 0 이 아닌 단위 없는 숫자)은 이 해석기가 아는 길이가 아니므로 setter 가 무시한다. 인스턴스는 바뀌지 않는다.
 */
final class TextSpacing {

    /**
     * The initial value of both attributes, <code>0px</code>.
     * <p>
     * 두 속성의 초기값 <code>0px</code>.
     */
    static final TextSpacing ZERO = new TextSpacing(0, "px");

    /**
     * The number as parsed.
     * <p>
     * 해석한 숫자.
     */
    private final double value;

    /**
     * The lower case unit (<code>q</code> for <code>Q</code>).
     * <p>
     * 소문자 단위(<code>Q</code> 는 <code>q</code>).
     */
    private final String unit;

    /**
     * Creates a length.
     * <p>
     * 길이를 만든다.
     *
     * @param value the number / 숫자
     * @param unit  the lower case unit / 소문자 단위
     */
    private TextSpacing(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    /**
     * Parses a CSS length.
     * <p>
     * CSS 길이를 해석한다.
     *
     * @param text the text, white space around it is allowed / 문자열(앞뒤 공백 허용)
     * @return the length, or null if the text is not a length that is understood / 길이. 이해하는 길이가 아니면 null
     */
    static TextSpacing parse(String text) {
        String s = text.trim();
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digits = 0;
        // 정수 부분의 숫자를 읽는다.
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
            digits++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            // 소수 부분의 숫자를 읽는다.
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                digits++;
            }
        }
        if (digits == 0) {
            return null;
        }
        // 지수가 있으면 읽는다(e 뒤에 숫자가 없으면 단위의 시작으로 본다).
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            int expDigits = 0;
            // 지수의 숫자를 읽는다.
            while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                j++;
                expDigits++;
            }
            if (expDigits > 0) {
                i = j;
            }
        }
        double number;
        try {
            number = Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return null;
        }
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return null;
        }
        String unit = s.substring(i).toLowerCase(Locale.ENGLISH);
        if (unit.length() == 0) {
            return number == 0 ? new TextSpacing(0, "px") : null;
        }
        return unitFactor(unit, 16) == 0 ? null : new TextSpacing(number, unit);
    }

    /**
     * Returns the pixels of one unit.
     * <p>
     * 단위 하나의 화소 수를 반환한다.
     *
     * @param unit     the lower case unit / 소문자 단위
     * @param fontSize the font size in pixels that the relative units refer to / 상대 단위가 기준으로 삼는 글꼴 크기(화소)
     * @return the pixels, or 0 for an unknown unit / 화소 수. 모르는 단위이면 0
     */
    private static double unitFactor(String unit, double fontSize) {
        if ("px".equals(unit)) {
            return 1;
        } else if ("pt".equals(unit)) {
            return 96 / 72.0;
        } else if ("pc".equals(unit)) {
            return 16;
        } else if ("in".equals(unit)) {
            return 96;
        } else if ("cm".equals(unit)) {
            return 96 / 2.54;
        } else if ("mm".equals(unit)) {
            return 96 / 25.4;
        } else if ("q".equals(unit)) {
            return 96 / 101.6;
        } else if ("em".equals(unit)) {
            return fontSize;
        } else if ("ex".equals(unit) || "ch".equals(unit)) {
            return fontSize / 2;
        } else if ("rem".equals(unit)) {
            return 16;
        }
        return 0;
    }

    /**
     * Returns the length in pixels.
     * <p>
     * 길이를 화소로 반환한다.
     *
     * @param fontSize the size in pixels of the font that <code>em</code>, <code>ex</code> and <code>ch</code> refer to / <code>em</code>, <code>ex</code>,
     *                 <code>ch</code> 가 기준으로 삼는 글꼴의 크기(화소)
     * @return the pixels / 화소
     */
    double pixels(double fontSize) {
        return value * unitFactor(unit, fontSize);
    }

    /**
     * Returns the serialized form, the number and the unit (<code>"5px"</code>, <code>"-0.5em"</code>, <code>"2Q"</code>).
     * <p>
     * 직렬화된 형태, 곧 숫자와 단위를 반환한다(<code>"5px"</code>, <code>"-0.5em"</code>, <code>"2Q"</code>).
     *
     * @return the text / 문자열
     */
    String serialize() {
        return NumberText.toString(value) + ("q".equals(unit) ? "Q" : unit);
    }
}
