package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.TextEmphasis;

/**
 * Paints the emphasis marks (<code>text-emphasis</code>, CSS Text Decoration Level 4) of a text fragment: one mark per typographic character unit, centered on the character, over
 * or under the text; separators, punctuation and control characters get none ({@link TextEmphasis#marked}). The positions come from the advances of the font of the text, the same
 * ones that the layout used, including the letter and word spacing; the fragments of right-to-left text mirror the positions.
 * <p>
 * 텍스트 조각의 강조점(<code>text-emphasis</code>, CSS Text Decoration Level 4)을 그린다: 글자 단위마다 점 하나를 글자 가운데에 텍스트 위나 아래에 그리며 구분자, 문장 부호,
 * 제어 문자에는 그리지 않는다({@link TextEmphasis#marked}). 위치는 배치가 쓴 것과 같은, 텍스트 글꼴의 advance(자간과 단어 간격 포함)에서 나오며 오른쪽에서 왼쪽 텍스트의
 * 조각은 위치를 좌우로 뒤집는다.
 */
final class EmphasisMarks extends DisplayCommand {

    /** The text of the fragment in logical order. 조각의 텍스트(논리 순서). */
    private final String text;

    /** The font of the text. 텍스트의 글꼴. */
    private final Font font;

    /** The marks. 강조점. */
    private final TextEmphasis emphasis;

    /** Left of the fragment. 조각의 왼쪽. */
    private final double x;

    /** The baseline of the text. 텍스트의 기준선. */
    private final double baseline;

    /** The width of the fragment. 조각의 너비. */
    private final double width;

    /** The ascent of the text font itself (without the room of the marks). 텍스트 글꼴 자체의 ascent(점의 공간 제외). */
    private final double ascent;

    /** The descent of the text font itself. 텍스트 글꼴 자체의 descent. */
    private final double descent;

    /** Letter spacing in px. 글자 간격(px). */
    private final double letterSpacing;

    /** Word spacing in px. 단어 간격(px). */
    private final double wordSpacing;

    /** The bidi level of the fragment. 조각의 양방향 수준. */
    private final int bidiLevel;

    /**
     * Creates the command.
     * <p>
     * 명령을 만든다.
     *
     * @param text          the text / 텍스트
     * @param font          the font of the text / 텍스트의 글꼴
     * @param emphasis      the marks / 강조점
     * @param bounds        the area of the fragment {x, y, width, height} / 조각의 영역
     * @param baseline      the baseline of the text / 텍스트의 기준선
     * @param ascent        the ascent of the font itself / 글꼴 자체의 ascent
     * @param descent       the descent of the font itself / 글꼴 자체의 descent
     * @param letterSpacing letter spacing / 글자 간격
     * @param wordSpacing   word spacing / 단어 간격
     * @param bidiLevel     the bidi level / 양방향 수준
     * @param node          the DOM node or null / DOM 노드 또는 null
     */
    EmphasisMarks(String text, Font font, TextEmphasis emphasis, double[] bounds, double baseline, double ascent, double descent, double letterSpacing,
            double wordSpacing, int bidiLevel, Node node) {
        super(Type.EMPHASIS, bounds[0], bounds[1], bounds[2], bounds[3], node);
        this.text = text;
        this.font = font;
        this.emphasis = emphasis;
        this.x = bounds[0];
        this.width = bounds[2];
        this.baseline = baseline;
        this.ascent = ascent;
        this.descent = descent;
        this.letterSpacing = letterSpacing;
        this.wordSpacing = wordSpacing;
        this.bidiLevel = bidiLevel;
    }

    /**
     * Returns the positions of the marks: the centers of the characters that get a mark, relative to the left of the fragment.
     * <p>
     * 점의 위치를 반환한다: 점이 붙는 글자의 가운데(조각 왼쪽 기준).
     *
     * @return the x centers / 가운데 x 좌표
     */
    double[] centers() {
        double[] tmp = new double[text.length()];
        int n = 0;
        double prefix = 0;
        // 코드 포인트마다 advance 를 누적하며 점이 붙는 글자의 가운데를 기록한다.
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            double adv = TextRenderer.advance(font, ch) + letterSpacing + (cp == ' ' ? wordSpacing : 0);
            if (TextEmphasis.marked(cp)) {
                tmp[n++] = prefix + (adv - letterSpacing) / 2;
            }
            prefix += adv;
            i += Character.charCount(cp);
        }
        double[] out = new double[n];
        boolean rtl = (bidiLevel & 1) != 0;
        // 오른쪽에서 왼쪽 조각은 조각의 너비 안에서 좌우를 뒤집는다.
        for (int i = 0; i < n; i++) {
            out[i] = rtl ? Math.max(width, prefix) - tmp[n - 1 - i] : tmp[i];
        }
        return out;
    }

    /**
     * Paints the marks.
     * <p>
     * 점을 그린다.
     *
     * @param ctx the context / 컨텍스트
     */
    void paint(PaintContext ctx) {
        Graphics2D g = ctx.g;
        g.setColor(new Color(emphasis.getColor(), true));
        double mark = TextRenderer.advance(emphasis.getFont(), emphasis.getMark());
        double mb = emphasis.markBaseline(baseline, ascent, descent);
        double[] c = centers();
        // 점마다 글자 가운데에 맞춰 그린다.
        for (int i = 0; i < c.length; i++) {
            TextRenderer.draw(g, emphasis.getMark(), emphasis.getFont(), x + c[i] - mark / 2, mb, 0, 0, 0);
        }
    }
}
