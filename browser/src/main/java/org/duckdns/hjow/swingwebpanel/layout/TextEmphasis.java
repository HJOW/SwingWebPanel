package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The emphasis marks of a style (CSS Text Decoration Module Level 4, <a href="https://www.w3.org/TR/css-text-decor-4/#emphasis-marks">https://www.w3.org/TR/css-text-decor-4/#emphasis-marks</a>,
 * W3C Working Draft 4 May 2022): <code>text-emphasis-style</code> (a filled or open shape: <code>dot</code>, <code>circle</code>, <code>double-circle</code>, <code>triangle</code>,
 * <code>sesame</code>, or a string), <code>text-emphasis-color</code> and <code>text-emphasis-position</code> (<code>over</code> or <code>under</code> the text in horizontal text). A mark
 * is drawn once for each typographic character unit at 50 percent of the font size, centered on the character, except for separators (Z*), punctuation (P*, with a few exceptions
 * such as <code>#</code> and <code>%</code>) and control, format and unassigned characters ({@link #marked}). The room for the marks is added to the line like the room of ruby text:
 * {@link #reserve} is the height that is added above (<code>over</code>) or below (<code>under</code>) the text fragment. The marks of vertical text are not drawn.
 * <p>
 * Usage: <code>TextEmphasis e = TextEmphasis.of(style); if (e != null) { ... e.getMark() ... }</code>.
 * <p>
 * 스타일의 강조점(CSS Text Decoration Module Level 4, W3C 작업 초안 2022-05-04): <code>text-emphasis-style</code>(채운 또는 빈 모양: <code>dot</code>, <code>circle</code>,
 * <code>double-circle</code>, <code>triangle</code>, <code>sesame</code>, 또는 문자열), <code>text-emphasis-color</code>, <code>text-emphasis-position</code>(가로 텍스트에서 텍스트의
 * 위(<code>over</code>) 또는 아래(<code>under</code>)). 점은 글자 단위마다 한 번, 글꼴 크기의 50 % 로 글자 가운데에 그리며 구분자(Z*), 문장 부호(P*. <code>#</code>, <code>%</code> 등
 * 몇 가지 예외 제외), 제어/서식/미할당 문자에는 그리지 않는다({@link #marked}). 점의 공간은 ruby 텍스트의 공간처럼 줄에 더해진다: {@link #reserve} 는 텍스트 조각의 위(<code>over</code>)
 * 또는 아래(<code>under</code>)에 더하는 높이이다. 세로쓰기 텍스트의 점은 그리지 않는다.
 * <p>
 * 사용법: <code>TextEmphasis e = TextEmphasis.of(style); if (e != null) { ... e.getMark() ... }</code>.
 */
public final class TextEmphasis {

    /** The size of the marks relative to the font size. 점의 크기(글꼴 크기 대비). */
    public static final double MARK_SCALE = 0.5;

    /** The shapes: {name, filled mark, open mark}. 모양: {이름, 채운 점, 빈 점}. */
    private static final String[][] SHAPES = {
        {"dot", "•", "◦"},
        {"circle", "●", "○"},
        {"double-circle", "◉", "◎"},
        {"triangle", "▲", "△"},
        {"sesame", "﹅", "﹆"}};

    /** The punctuation characters that get a mark all the same (the list of the specification). 문장 부호이지만 점이 붙는 문자(규격의 목록). */
    private static final String PUNCTUATION_WITH_MARKS = "#%‰‱٪؉؊&⁊@§¶⁋⁓〽";

    /** The mark (a shape character or the first character of a string). 점(모양 문자 또는 문자열의 첫 글자). */
    private final String mark;

    /** The ARGB color of the marks. 점의 ARGB 색. */
    private final int color;

    /** True if the marks are over the text, false if they are under it. 점이 텍스트 위에 있으면 true, 아래이면 false. */
    private final boolean over;

    /** The font of the marks. 점의 글꼴. */
    private final Font font;

    /** The height that the marks need above or below the text. 점이 텍스트 위나 아래에 필요로 하는 높이. */
    private final double reserve;

    /** The distance from the baseline of the marks to the bottom of their box (the descent of the mark font). 점의 기준선에서 점 상자 아래까지의 거리(점 글꼴의 descent). */
    private final double markDescent;

    /** The ascent of the mark font. 점 글꼴의 ascent. */
    private final double markAscent;

    /** True if the text is in a vertical typographic mode (<code>writing-mode: vertical-rl</code> or <code>vertical-lr</code>): the marks are at the right or at the left of the text. 텍스트가 세로 조판 모드(<code>writing-mode: vertical-rl</code> 또는 <code>vertical-lr</code>)이면 true: 점은 텍스트의 오른쪽이나 왼쪽에 있다. */
    private final boolean vertical;

    /**
     * Creates the marks.
     * <p>
     * 강조점을 만든다.
     *
     * @param mark     the mark / 점
     * @param color    the ARGB color / ARGB 색
     * @param over     true for over (in vertical text: at the right) / 위이면 true(세로 텍스트에서는 오른쪽)
     * @param font     the mark font / 점 글꼴
     * @param vertical true for vertical typographic mode / 세로 조판 모드이면 true
     */
    private TextEmphasis(String mark, int color, boolean over, Font font, boolean vertical) {
        this.mark = mark;
        this.color = color;
        this.over = over;
        this.vertical = vertical;
        this.font = font;
        double[] m = FontResolver.metrics(font);
        this.markAscent = m[0];
        this.markDescent = m[1];
        this.reserve = m[0] + m[1];
    }

    /**
     * Reads the emphasis marks of a style.
     * <p>
     * 스타일의 강조점을 읽는다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the marks, or null for <code>none</code> / 강조점. <code>none</code> 이면 null
     */
    public static TextEmphasis of(ComputedStyle s) {
        if (s == null) {
            return null;
        }
        CssValue v = s.get(Prop.TEXT_EMPHASIS_STYLE);
        if (v == null || v.isKeyword("none")) {
            return null;
        }
        String writingMode = s.keyword(Prop.WRITING_MODE);
        boolean vertical = writingMode != null && writingMode.startsWith("vertical");
        String mark = markOf(v, vertical);
        if (mark == null) {
            return null;
        }
        CssValue cv = s.get(Prop.TEXT_EMPHASIS_COLOR);
        int color = cv == null || cv.isKeyword("currentcolor") ? s.color(Prop.COLOR) : s.color(Prop.TEXT_EMPHASIS_COLOR);
        CssValue pv = s.get(Prop.TEXT_EMPHASIS_POSITION);
        String position = pv == null ? "" : " " + pv.toCssText().toLowerCase() + " ";
        // 가로 조판에서는 over/under 가, 세로 조판에서는 right/left 가 점의 쪽을 정한다(Text Decoration 4 text-emphasis-position). 위와 오른쪽이 기본이다.
        boolean over = vertical ? position.indexOf(" left ") < 0 : position.indexOf(" under ") < 0;
        Font base = FontResolver.resolve(s, mark);
        Font f = base.deriveFont((float) (base.getSize2D() * MARK_SCALE));
        return new TextEmphasis(mark, color, over, f, vertical);
    }

    /**
     * Tells whether the marks belong to text in a vertical typographic mode (<code>writing-mode: vertical-rl</code> or <code>vertical-lr</code>): such
     * marks stand upright at the right (<code>over</code>) or at the left (<code>under</code>) of the glyphs, and the default shape is the sesame.
     * <p>
     * 점이 세로 조판 모드(<code>writing-mode: vertical-rl</code> 또는 <code>vertical-lr</code>)의 텍스트에 속하는지 알려 준다: 그런 점은 글리프의 오른쪽(<code>over</code>)이나
     * 왼쪽(<code>under</code>)에 똑바로 서며 기본 모양은 sesame 이다.
     *
     * @return true for vertical typographic mode / 세로 조판 모드이면 true
     */
    public boolean isVertical() {
        return vertical;
    }

    /**
     * Returns the ascent of the mark font.
     * <p>
     * 점 글꼴의 ascent 를 반환한다.
     *
     * @return the ascent in px / ascent(px)
     */
    public double markAscent() {
        return markAscent;
    }

    /**
     * Returns the descent of the mark font.
     * <p>
     * 점 글꼴의 descent 를 반환한다.
     *
     * @return the descent in px / descent(px)
     */
    public double markDescent() {
        return markDescent;
    }

    /**
     * Works out the mark of a <code>text-emphasis-style</code> value: a string gives its first grapheme cluster; keywords give the shape, with <code>filled</code> as the default
     * filling, and <code>filled</code> or <code>open</code> alone computing to the circle.
     * <p>
     * <code>text-emphasis-style</code> 값의 점을 구한다: 문자열은 첫 글자 단위(그래핌 클러스터)이고, 키워드는 모양이며 채움의 기본은 <code>filled</code>, <code>filled</code> 나
     * <code>open</code> 만 있으면 원(circle)이 된다.
     *
     * @param v        the value / 값
     * @param vertical true in a vertical typographic mode, where <code>filled</code> or <code>open</code> alone compute to the sesame / 세로 조판 모드이면 true. 그 모드에서는
     *                 <code>filled</code> 나 <code>open</code> 만 있으면 sesame 이 된다
     * @return the mark, or null if the value gives none / 점. 없으면 null
     */
    private static String markOf(CssValue v, boolean vertical) {
        if (v instanceof CssString) {
            String t = ((CssString) v).getValue();
            if (t.length() == 0) {
                return null;
            }
            return t.substring(0, clusterEnd(t));
        }
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        boolean open = false;
        String shape = null;
        // 키워드를 하나씩 읽어 채움과 모양을 정한다.
        for (int i = 0; i < items.size(); i++) {
            String k = items.get(i).toCssText().toLowerCase();
            if (k.equals("open")) {
                open = true;
            } else if (!k.equals("filled")) {
                shape = k;
            }
        }
        // 모양이 없으면(filled 나 open 만 있으면) 가로 쓰기에서는 원이고 세로 쓰기에서는 sesame 이다.
        if (shape == null) {
            shape = vertical ? "sesame" : "circle";
        }
        // 모양 표에서 이름이 같은 항목의 점을 채움에 따라 고른다.
        for (int i = 0; i < SHAPES.length; i++) {
            if (SHAPES[i][0].equals(shape)) {
                return open ? SHAPES[i][2] : SHAPES[i][1];
            }
        }
        return null;
    }

    /**
     * Returns the end of the first grapheme cluster of a text: the first code point and the combining marks and joiners (with what they join) that follow it.
     * <p>
     * 텍스트의 첫 글자 단위(그래핌 클러스터)의 끝을 반환한다: 첫 코드 포인트와 뒤따르는 결합 문자와 접합자(접합되는 것 포함).
     *
     * @param t the text, not empty / 텍스트. 비어 있지 않음
     * @return the end offset / 끝 위치
     */
    private static int clusterEnd(String t) {
        int i = t.offsetByCodePoints(0, 1);
        // 결합 문자는 앞 글자에 붙이고, 접합자(ZWJ)는 그 다음 글자까지 붙인다.
        while (i < t.length()) {
            int cp = t.codePointAt(i);
            int type = Character.getType(cp);
            boolean joiner = cp == 0x200D;
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK || joiner || cp == 0xFE0F) {
                i += Character.charCount(cp);
                if (joiner && i < t.length()) {
                    i += Character.charCount(t.codePointAt(i));
                }
            } else {
                break;
            }
        }
        return i;
    }

    /**
     * Tells whether a character gets a mark: not separators (Z*), not punctuation (P*) except a few symbols, and not control, format or unassigned characters.
     * <p>
     * 글자에 점이 붙는지 알려 준다: 구분자(Z*), 몇 가지 기호를 뺀 문장 부호(P*), 제어/서식/미할당 문자에는 붙지 않는다.
     *
     * @param cp the code point / 코드 포인트
     * @return true if the character is marked / 점이 붙으면 true
     */
    public static boolean marked(int cp) {
        int t = Character.getType(cp);
        if (t == Character.SPACE_SEPARATOR || t == Character.LINE_SEPARATOR || t == Character.PARAGRAPH_SEPARATOR || t == Character.CONTROL
                || t == Character.FORMAT || t == Character.UNASSIGNED) {
            return false;
        }
        boolean punctuation = t == Character.CONNECTOR_PUNCTUATION || t == Character.DASH_PUNCTUATION || t == Character.START_PUNCTUATION
                || t == Character.END_PUNCTUATION || t == Character.INITIAL_QUOTE_PUNCTUATION || t == Character.FINAL_QUOTE_PUNCTUATION
                || t == Character.OTHER_PUNCTUATION;
        // 문장 부호는 규격이 정한 예외 문자만 점이 붙는다.
        return !punctuation || (cp < 0x10000 && PUNCTUATION_WITH_MARKS.indexOf((char) cp) >= 0);
    }

    /**
     * Returns the mark.
     * <p>
     * 점을 반환한다.
     *
     * @return the mark text / 점 문자열
     */
    public String getMark() {
        return mark;
    }

    /**
     * Returns the color of the marks.
     * <p>
     * 점의 색을 반환한다.
     *
     * @return the ARGB color / ARGB 색
     */
    public int getColor() {
        return color;
    }

    /**
     * Tells where the marks are.
     * <p>
     * 점이 어디에 있는지 알려 준다.
     *
     * @return true for over the text, false for under it / 텍스트 위이면 true, 아래이면 false
     */
    public boolean isOver() {
        return over;
    }

    /**
     * Returns the font of the marks (50 percent of the size of the text font).
     * <p>
     * 점의 글꼴을 반환한다(텍스트 글꼴 크기의 50 %).
     *
     * @return the font / 글꼴
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns the height that the marks add to the line: the height of the mark font, above the text for <code>over</code> and below it for <code>under</code>.
     * <p>
     * 점이 줄에 더하는 높이를 반환한다: 점 글꼴의 높이이며 <code>over</code> 이면 텍스트 위, <code>under</code> 이면 아래에 더한다.
     *
     * @return the height in px / 높이(px)
     */
    public double reserve() {
        return reserve;
    }

    /**
     * Returns the baseline of the marks of a text fragment. The marks stand in the room that {@link #reserve} gave: over the text, just above its ascent (the part that
     * the text font itself needs; the room of the marks is on top of it), and under the text, just below its descent.
     * <p>
     * 텍스트 조각의 점의 기준선을 반환한다. 점은 {@link #reserve} 가 준 공간에 선다: 위이면 텍스트 자신의 ascent(글꼴 자체가 필요로 하는 부분. 점의 공간은 그 위에 더해진다) 바로 위,
     * 아래이면 descent 바로 아래.
     *
     * @param baseline    the baseline of the text / 텍스트의 기준선
     * @param textAscent  the ascent of the text font itself / 텍스트 글꼴 자체의 ascent
     * @param textDescent the descent of the text font itself / 텍스트 글꼴 자체의 descent
     * @return the baseline of the marks / 점의 기준선
     */
    public double markBaseline(double baseline, double textAscent, double textDescent) {
        return over ? baseline - textAscent - markDescent : baseline + textDescent + markAscent;
    }
}
