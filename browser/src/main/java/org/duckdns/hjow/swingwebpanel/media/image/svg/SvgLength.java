package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSS/SVG length with its unit (plan item P18-04): a number followed by none, <code>px</code>, <code>pt</code>, <code>pc</code>,
 * <code>mm</code>, <code>cm</code>, <code>in</code>, <code>q</code>, <code>em</code>, <code>ex</code>, <code>rem</code>, <code>ch</code>,
 * <code>vw</code>, <code>vh</code>, <code>vmin</code>, <code>vmax</code> or <code>%</code>. The pixel value is computed late because
 * percentages and font-relative units depend on the place of use.
 * <p>
 * 단위가 있는 CSS/SVG 길이(계획 P18-04): 수 뒤에 없음, <code>px</code>, <code>pt</code>, <code>pc</code>, <code>mm</code>, <code>cm</code>,
 * <code>in</code>, <code>q</code>, <code>em</code>, <code>ex</code>, <code>rem</code>, <code>ch</code>, <code>vw</code>, <code>vh</code>,
 * <code>vmin</code>, <code>vmax</code>, <code>%</code>. 백분율과 글꼴 상대 단위는 쓰는 곳에 따라 달라지므로 픽셀 값은 나중에 계산한다.
 */
final class SvgLength {

    /**
     * The number part.
     * <p>
     * 수 부분.
     */
    final double value;

    /**
     * The lower-case unit text (empty for a plain number).
     * <p>
     * 소문자 단위 텍스트(단순한 수이면 빈 문자열).
     */
    final String unit;

    /**
     * Creates a length.
     * <p>
     * 길이를 만든다.
     *
     * @param value number / 수
     * @param unit  unit text / 단위 텍스트
     */
    SvgLength(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    /**
     * Tells whether the unit is a percentage.
     * <p>
     * 단위가 백분율인지 알려 준다.
     *
     * @return true for percent / 백분율이면 true
     */
    boolean isPercent() {
        return "%".equals(unit);
    }

    /**
     * Parses a length from a whole text.
     * <p>
     * 텍스트 전체를 길이로 해석한다.
     *
     * @param text the text / 텍스트
     * @return the length or null if the text is not a length / 길이. 길이가 아니면 null
     */
    static SvgLength parse(String text) {
        if (text == null) {
            return null;
        }
        SvgScanner sc = new SvgScanner(text.trim());
        SvgLength l = read(sc);
        if (l == null) {
            return null;
        }
        sc.skipWs();
        return sc.atEnd() ? l : null;
    }

    /**
     * Reads a length at the scanner position.
     * <p>
     * 스캐너 위치에서 길이를 읽는다.
     *
     * @param sc the scanner / 스캐너
     * @return the length or null (the position is unchanged then) / 길이. 없으면 null(이때 위치는 그대로)
     */
    static SvgLength read(SvgScanner sc) {
        int save = sc.p;
        if (!sc.number()) {
            return null;
        }
        double v = sc.value;
        String u = sc.letters().toLowerCase(java.util.Locale.ENGLISH);
        if (!isUnit(u)) {
            sc.p = save;
            return null;
        }
        return new SvgLength(v, u);
    }

    /**
     * Tells whether a text is a known length unit.
     * <p>
     * 텍스트가 알려진 길이 단위인지 알려 준다.
     *
     * @param u the unit text in lower case / 소문자 단위 텍스트
     * @return true if known / 알려진 단위이면 true
     */
    private static boolean isUnit(String u) {
        return u.length() == 0 || "px".equals(u) || "pt".equals(u) || "pc".equals(u) || "mm".equals(u) || "cm".equals(u) || "in".equals(u)
                || "q".equals(u) || "em".equals(u) || "ex".equals(u) || "rem".equals(u) || "ch".equals(u) || "vw".equals(u) || "vh".equals(u)
                || "vmin".equals(u) || "vmax".equals(u) || "%".equals(u);
    }

    /**
     * Parses a list of lengths separated by white space and commas.
     * <p>
     * 공백과 쉼표로 구분된 길이 목록을 해석한다.
     *
     * @param text the text / 텍스트
     * @return the lengths, or null if any item is invalid / 길이 배열. 하나라도 올바르지 않으면 null
     */
    static SvgLength[] parseList(String text) {
        if (text == null) {
            return null;
        }
        SvgScanner sc = new SvgScanner(text);
        List<SvgLength> list = new ArrayList<SvgLength>();
        sc.skipWsAndCommas();
        // 길이를 끝까지 하나씩 읽는다.
        while (!sc.atEnd()) {
            SvgLength l = read(sc);
            if (l == null) {
                return null;
            }
            list.add(l);
            sc.skipWsAndCommas();
        }
        return list.toArray(new SvgLength[list.size()]);
    }

    /**
     * Converts the length to CSS pixels.
     * <p>
     * 길이를 CSS 픽셀로 바꾼다.
     *
     * @param fontSize    the font size in pixels for <code>em</code> and <code>ex</code> / <code>em</code>, <code>ex</code> 에 쓰는 글꼴 크기(픽셀)
     * @param percentBase the length that 100% stands for / 100% 가 뜻하는 길이
     * @param viewportW   the viewport width for <code>vw</code> / <code>vw</code> 에 쓰는 뷰포트 너비
     * @param viewportH   the viewport height for <code>vh</code> / <code>vh</code> 에 쓰는 뷰포트 높이
     * @return pixels / 픽셀
     */
    double toPx(double fontSize, double percentBase, double viewportW, double viewportH) {
        if (unit.length() == 0 || "px".equals(unit)) {
            return value;
        }
        if ("%".equals(unit)) {
            return value * percentBase / 100;
        }
        if ("pt".equals(unit)) {
            return value * 4 / 3;
        }
        if ("pc".equals(unit)) {
            return value * 16;
        }
        if ("mm".equals(unit)) {
            return value * 96 / 25.4;
        }
        if ("cm".equals(unit)) {
            return value * 96 / 2.54;
        }
        if ("in".equals(unit)) {
            return value * 96;
        }
        if ("q".equals(unit)) {
            return value * 96 / 101.6;
        }
        if ("em".equals(unit) || "rem".equals(unit)) {
            return value * ("rem".equals(unit) ? 16 : fontSize);
        }
        if ("ex".equals(unit) || "ch".equals(unit)) {
            return value * fontSize / 2;
        }
        if ("vw".equals(unit)) {
            return value * viewportW / 100;
        }
        if ("vh".equals(unit)) {
            return value * viewportH / 100;
        }
        if ("vmin".equals(unit)) {
            return value * Math.min(viewportW, viewportH) / 100;
        }
        return value * Math.max(viewportW, viewportH) / 100;
    }
}
