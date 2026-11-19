package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The display commands of the text that stands upright in vertical lines (plan item P10-03-e, CSS Writing Modes Level 3): characters set
 * upright one after the other along the line ({@link UprightText}) and text composed horizontally into the space of one character
 * ({@link CombinedText}, <code>text-combine-upright</code>). Sideways text is painted by {@link DisplayCommands.RotatedText}. They are
 * created by {@link DisplayListBuilder} only.
 * <p>
 * 세로 줄에서 똑바로 서는 텍스트의 디스플레이 명령(계획 P10-03-e, CSS Writing Modes Level 3): 줄을 따라 차례로 똑바로 놓이는 글자({@link UprightText})와
 * 글자 하나의 공간에 가로로 짜 넣은 텍스트({@link CombinedText}, <code>text-combine-upright</code>). 눕힌 텍스트는 {@link DisplayCommands.RotatedText} 가 그린다.
 * {@link DisplayListBuilder} 만 만든다.
 */
final class VerticalTextCommands {

    /**
     * Private constructor: static holder class.
     * <p>
     * 비공개 생성자: 정적 묶음 클래스이다.
     */
    private VerticalTextCommands() {
    }

    /**
     * Creates the command that paints a text fragment of vertical text whose glyphs stand upright or are composed horizontally.
     * <p>
     * 글리프가 똑바로 서거나 가로로 짜인 세로쓰기 텍스트 조각을 그리는 명령을 만든다.
     *
     * @param t          the TEXT box / TEXT 상자
     * @param text       the text / 텍스트
     * @param color      ARGB color / ARGB 색
     * @param decoration decoration bits / 장식 비트
     * @param decoStyle  decoration style / 장식 스타일
     * @param decoColor  decoration color / 장식 색
     * @param thickness  decoration thickness or NaN / 장식 두께 또는 NaN
     * @return the command, or null if the fragment is plain horizontal or sideways text / 명령. 보통의 가로 텍스트나 눕힌 텍스트이면 null
     */
    static DisplayCommand create(org.duckdns.hjow.swingwebpanel.layout.LayoutBox t, String text, int color, int decoration, int decoStyle, int decoColor,
            double thickness) {
        int mode = t.getGlyphMode();
        // 세로 줄(회전 1)에서 똑바로 서거나 조합한 조각만 이 명령으로 그린다.
        if (mode == org.duckdns.hjow.swingwebpanel.layout.LayoutBox.GLYPHS_SIDEWAYS || t.getTextRotation() != 1) {
            return null;
        }
        double[] bounds = { t.getX(), t.getY(), t.getWidth(), t.getHeight() };
        if (mode == org.duckdns.hjow.swingwebpanel.layout.LayoutBox.GLYPHS_UPRIGHT) {
            return new UprightText(text, t.getClusterOffsets(), t.getFont(), bounds, t.getAscent(), t.getDescent(), t.getLetterSpacing(),
                    (t.getBidiLevel() & 1) == 1, color, decoration, decoStyle, decoColor, thickness, t.getNode());
        }
        return new CombinedText(text, t.getFont(), bounds, t.getAscent(), t.getDescent(), color, decoration, decoStyle, decoColor, thickness,
                t.getNode());
    }

    /**
     * Paints the decoration lines of a vertical fragment in the frame that is rotated a quarter turn around the origin (like the lines of
     * sideways text: they run along the line on the descender side).
     * <p>
     * 세로 조각의 장식선을 원점을 중심으로 사분원 돌린 좌표계에서 그린다(눕힌 텍스트의 선처럼 descender 쪽에서 줄을 따라 달린다).
     *
     * @param g          the graphics / 그래픽스
     * @param originX    x of the origin / 원점의 x
     * @param originY    y of the origin / 원점의 y
     * @param clockwise  true for a clockwise turn / 시계 방향이면 true
     * @param advance    length of the fragment along the line / 줄을 따라 조각의 길이
     * @param ascent     font ascent / 글꼴 ascent
     * @param fontSize   font size / 글꼴 크기
     * @param decoration decoration bits / 장식 비트
     * @param decoStyle  decoration style / 장식 스타일
     * @param decoColor  decoration color / 장식 색
     * @param thickness  decoration thickness or NaN / 장식 두께 또는 NaN
     */
    private static void decorate(Graphics2D g, double originX, double originY, boolean clockwise, double advance, double ascent, double fontSize,
            int decoration, int decoStyle, int decoColor, double thickness) {
        if (decoration == 0) {
            return;
        }
        AffineTransform saved = g.getTransform();
        try {
            g.translate(originX, originY);
            g.rotate(clockwise ? Math.PI / 2 : -Math.PI / 2);
            TextRenderer.drawDecorations(g, decoration, decoStyle, decoColor, thickness, 0, 0, advance, ascent, fontSize);
        } finally {
            g.setTransform(saved);
        }
    }

    /**
     * Paints characters that stand upright in a vertical line: the character clusters of the text take one cell of one em each (plus the
     * letter spacing) from the top of the box (in reverse order for a right-to-left run), every glyph is centered across the line and its
     * em box is centered in its cell.
     * <p>
     * 세로 줄에서 똑바로 서는 글자를 그린다: 텍스트의 글자 클러스터가 상자 위쪽부터 각각 1em(과 글자 간격) 한 칸을 차지하고(오른쪽에서 왼쪽 구간은 거꾸로) 글리프는
     * 줄을 가로질러 가운데에, em 상자는 칸의 가운데에 놓인다.
     */
    static final class UprightText extends DisplayCommand {
        /** The text in logical order. 논리 순서의 텍스트. */
        private final String text;
        /** Start offsets of the clusters followed by the text length. 클러스터 시작 위치와 마지막에 텍스트 길이. */
        private final int[] clusters;
        /** The font. 글꼴. */
        private final Font font;
        /** Left of the box that holds the run. 구간을 담은 상자의 왼쪽. */
        private final double boxX;
        /** Top of the box. 상자의 위쪽. */
        private final double boxY;
        /** Width of the box (across the line). 상자의 너비(줄을 가로지르는 방향). */
        private final double boxW;
        /** Font ascent. 글꼴 ascent. */
        private final double ascent;
        /** Font descent. 글꼴 descent. */
        private final double descent;
        /** Letter spacing after every cluster. 클러스터마다 뒤에 오는 글자 간격. */
        private final double letterSpacing;
        /** True to place the clusters in reverse order. 클러스터를 거꾸로 놓으면 true. */
        private final boolean reverse;
        /** The color as ARGB value. ARGB 값으로 나타낸 색상. */
        private final int color;
        /** Decoration bits. 장식 비트. */
        private final int decoration;
        /** Decoration style. 장식 스타일. */
        private final int decoStyle;
        /** Decoration ARGB color. 장식 ARGB 색. */
        private final int decoColor;
        /** Decoration thickness or NaN. 장식 두께. 자동이면 NaN. */
        private final double decoThickness;
        /** X of the origin of the decoration frame. 장식 좌표계 원점의 x. */
        private final double originX;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param text          the text / 텍스트
         * @param clusters      cluster start offsets followed by the text length / 클러스터 시작 위치와 마지막에 텍스트 길이
         * @param font          the font / 글꼴
         * @param bounds        {x, y, width, height} of the box / 상자의 {x, y, 너비, 높이}
         * @param ascent        font ascent / 글꼴 ascent
         * @param descent       font descent / 글꼴 descent
         * @param letterSpacing letter spacing / 글자 간격
         * @param reverse       true for a right-to-left run / 오른쪽에서 왼쪽 구간이면 true
         * @param color         ARGB color / ARGB 색
         * @param decoration    decoration bits / 장식 비트
         * @param decoStyle     decoration style / 장식 스타일
         * @param decoColor     decoration color / 장식 색
         * @param decoThickness decoration thickness or NaN / 장식 두께 또는 NaN
         * @param node          DOM node or null / DOM 노드 또는 null
         */
        UprightText(String text, int[] clusters, Font font, double[] bounds, double ascent, double descent, double letterSpacing, boolean reverse,
                int color, int decoration, int decoStyle, int decoColor, double decoThickness, Node node) {
            super(Type.TEXT, bounds[0] - 3, bounds[1] - 3, bounds[2] + 6, bounds[3] + 6, node);
            this.text = text;
            this.clusters = clusters;
            this.font = font;
            this.boxX = bounds[0];
            this.boxY = bounds[1];
            this.boxW = bounds[2];
            this.ascent = ascent;
            this.descent = descent;
            this.letterSpacing = letterSpacing;
            this.reverse = reverse;
            this.color = color;
            this.decoration = decoration;
            this.decoStyle = decoStyle;
            this.decoColor = decoColor;
            this.decoThickness = decoThickness;
            this.originX = boxX + descent;
        }

        /**
         * Paints the glyphs and the decoration lines.
         * <p>
         * 글리프와 장식선을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            g.setColor(new Color(color, true));
            double em = font.getSize2D();
            double cell = em + letterSpacing;
            double centerX = boxX + boxW / 2;
            int n = clusters.length - 1;
            // 클러스터마다 자기 칸의 가운데에 똑바로 그린다.
            for (int k = 0; k < n; k++) {
                int idx = reverse ? n - 1 - k : k;
                String c = text.substring(clusters[idx], clusters[idx + 1]);
                double gw = TextRenderer.advance(font, c);
                double baseline = boxY + k * cell + em / 2 + (ascent - descent) / 2;
                TextRenderer.draw(g, c, font, centerX - gw / 2, baseline, 0, 0, 0);
            }
            decorate(g, originX, boxY, true, n * cell, ascent, em, decoration, decoStyle, decoColor, decoThickness);
        }
    }

    /**
     * Paints the emphasis marks (<code>text-emphasis</code>) of a text fragment of vertical text (CSS Text Decoration Level 4 section 3.2, marks in a vertical
     * typographic mode stand upright at the right of the glyphs, or at the left for <code>text-emphasis-position: left</code>): one mark per character that gets
     * one, at the center of the character along the line and in the space that the layout reserved at the side. An upright run has the cells of its clusters
     * (a cell is one em), a sideways run the advances of its characters, and combined text one mark in the middle of the box.
     * <p>
     * 세로 텍스트의 텍스트 조각의 강조점(<code>text-emphasis</code>)을 그린다(CSS Text Decoration Level 4 3.2 절. 세로 조판 모드의 점은 글리프의 오른쪽에 똑바로 서며
     * <code>text-emphasis-position: left</code> 이면 왼쪽): 점이 붙는 글자마다 하나씩, 줄을 따른 글자의 가운데에, 배치가 옆에 확보해 둔 공간에 그린다. 똑바로 선 조각은 클러스터의
     * 칸(칸은 1em)을, 눕힌 조각은 글자의 advance 를, 조합한 텍스트는 상자 가운데의 점 하나를 쓴다.
     */
    static final class EmphasisColumn extends DisplayCommand {
        /** The text. 텍스트. */
        private final String text;
        /** The cluster offsets of upright text, or null. 똑바로 선 텍스트의 클러스터 오프셋. 없으면 null. */
        private final int[] clusters;
        /** The glyph mode, one of the <code>GLYPHS_</code> constants of the layout box. 글리프 모드. 레이아웃 상자의 <code>GLYPHS_</code> 상수 중 하나. */
        private final int mode;
        /** The font of the text. 텍스트의 글꼴. */
        private final Font font;
        /** The marks. 강조점. */
        private final org.duckdns.hjow.swingwebpanel.layout.TextEmphasis emphasis;
        /** The left of the text box. 텍스트 상자의 왼쪽. */
        private final double boxX;
        /** The top of the text box. 텍스트 상자의 위쪽. */
        private final double boxY;
        /** The width of the text box. 텍스트 상자의 너비. */
        private final double boxW;
        /** The height of the text box. 텍스트 상자의 높이. */
        private final double boxH;
        /** The letter spacing. 글자 간격. */
        private final double letterSpacing;
        /** The word spacing. 단어 간격. */
        private final double wordSpacing;
        /** True if the clusters are in reverse order (right-to-left text). 클러스터가 거꾸로 놓이면(오른쪽에서 왼쪽 텍스트) true. */
        private final boolean reverse;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param t             the text box / 텍스트 상자
         * @param text          the text / 텍스트
         * @param emphasis      the marks / 강조점
         * @param wordSpacing   the word spacing / 단어 간격
         * @param node          the DOM node / DOM 노드
         */
        EmphasisColumn(org.duckdns.hjow.swingwebpanel.layout.LayoutBox t, String text, org.duckdns.hjow.swingwebpanel.layout.TextEmphasis emphasis, double wordSpacing, Node node) {
            super(Type.EMPHASIS, t.getX() - emphasis.reserve(), t.getY(), t.getWidth() + 2 * emphasis.reserve(), t.getHeight(), node);
            this.text = text;
            this.clusters = t.getClusterOffsets();
            this.mode = t.getGlyphMode();
            this.font = t.getFont();
            this.emphasis = emphasis;
            this.boxX = t.getX();
            this.boxY = t.getY();
            this.boxW = t.getWidth();
            this.boxH = t.getHeight();
            this.letterSpacing = t.getLetterSpacing();
            this.wordSpacing = wordSpacing;
            this.reverse = (t.getBidiLevel() & 1) == 1;
        }

        /**
         * Returns the positions of the marks along the line: the y of the center of every character that gets a mark.
         * <p>
         * 줄을 따른 점의 위치를 반환한다: 점이 붙는 글자마다 가운데의 y.
         *
         * @return the y values / y 값
         */
        double[] centers() {
            double[] tmp = new double[text.length() + 1];
            int n = 0;
            // 조합한 텍스트는 상자 가운데에 점 하나이다(점이 붙는 글자가 하나라도 있으면).
            if (mode == org.duckdns.hjow.swingwebpanel.layout.LayoutBox.GLYPHS_COMBINED) {
                for (int i = 0; i < text.length();) {
                    int cp = text.codePointAt(i);
                    if (org.duckdns.hjow.swingwebpanel.layout.TextEmphasis.marked(cp)) {
                        return new double[] { boxY + boxH / 2 };
                    }
                    i += Character.charCount(cp);
                }
                return new double[0];
            }
            // 똑바로 선 조각은 클러스터마다 1em 칸이다(칸의 가운데).
            if (mode == org.duckdns.hjow.swingwebpanel.layout.LayoutBox.GLYPHS_UPRIGHT && clusters != null) {
                double em = font.getSize2D();
                double cell = em + letterSpacing;
                int count = clusters.length - 1;
                // 칸마다 그 클러스터의 첫 글자에 점이 붙는지 본다.
                for (int k = 0; k < count; k++) {
                    int idx = reverse ? count - 1 - k : k;
                    if (clusters[idx] < clusters[idx + 1] && org.duckdns.hjow.swingwebpanel.layout.TextEmphasis.marked(text.codePointAt(clusters[idx]))) {
                        tmp[n++] = boxY + k * cell + em / 2;
                    }
                }
                double[] out = new double[n];
                System.arraycopy(tmp, 0, out, 0, n);
                return out;
            }
            // 눕힌 조각은 글자의 advance 를 위에서 아래로 누적한다.
            double prefix = 0;
            for (int i = 0; i < text.length();) {
                int cp = text.codePointAt(i);
                String ch = new String(Character.toChars(cp));
                double adv = TextRenderer.advance(font, ch) + letterSpacing + (cp == ' ' ? wordSpacing : 0);
                if (org.duckdns.hjow.swingwebpanel.layout.TextEmphasis.marked(cp)) {
                    tmp[n++] = boxY + prefix + (adv - letterSpacing) / 2;
                }
                prefix += adv;
                i += Character.charCount(cp);
            }
            double[] out = new double[n];
            System.arraycopy(tmp, 0, out, 0, n);
            return out;
        }

        /**
         * Paints the marks upright in the reserved space at the right or the left of the text box.
         * <p>
         * 점을 텍스트 상자 오른쪽이나 왼쪽의 확보된 공간에 똑바로 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            g.setColor(new Color(emphasis.getColor(), true));
            double r = emphasis.reserve();
            double cx = emphasis.isOver() ? boxX + boxW + r / 2 : boxX - r / 2;
            double mark = TextRenderer.advance(emphasis.getFont(), emphasis.getMark());
            double[] c = centers();
            // 점마다 글자의 가운데(세로)와 확보한 공간의 가운데(가로)에 그린다.
            for (int i = 0; i < c.length; i++) {
                TextRenderer.draw(g, emphasis.getMark(), emphasis.getFont(), cx - mark / 2, c[i] + (emphasis.markAscent() - emphasis.markDescent()) / 2, 0, 0, 0);
            }
        }
    }

    /**
     * Paints text that is composed horizontally into the space of one upright character (<code>text-combine-upright</code>): the text is
     * centered in the box and compressed horizontally if it is wider than one em.
     * <p>
     * 똑바로 선 글자 하나의 공간에 가로로 짜 넣은 텍스트(<code>text-combine-upright</code>)를 그린다: 텍스트는 상자 가운데에 놓이고 1em 보다 넓으면 가로로 압축한다.
     */
    static final class CombinedText extends DisplayCommand {
        /** The text. 텍스트. */
        private final String text;
        /** The font. 글꼴. */
        private final Font font;
        /** Left of the box. 상자의 왼쪽. */
        private final double boxX;
        /** Top of the box. 상자의 위쪽. */
        private final double boxY;
        /** Width of the box (across the line). 상자의 너비(줄을 가로지르는 방향). */
        private final double boxW;
        /** Height of the box (one em along the line). 상자의 높이(줄을 따라 1em). */
        private final double boxH;
        /** Font ascent. 글꼴 ascent. */
        private final double ascent;
        /** Font descent. 글꼴 descent. */
        private final double descent;
        /** The color as ARGB value. ARGB 값으로 나타낸 색상. */
        private final int color;
        /** Decoration bits. 장식 비트. */
        private final int decoration;
        /** Decoration style. 장식 스타일. */
        private final int decoStyle;
        /** Decoration ARGB color. 장식 ARGB 색. */
        private final int decoColor;
        /** Decoration thickness or NaN. 장식 두께. 자동이면 NaN. */
        private final double decoThickness;

        /**
         * Creates the command.
         * <p>
         * 명령을 만든다.
         *
         * @param text          the text / 텍스트
         * @param font          the font / 글꼴
         * @param bounds        {x, y, width, height} of the box / 상자의 {x, y, 너비, 높이}
         * @param ascent        font ascent / 글꼴 ascent
         * @param descent       font descent / 글꼴 descent
         * @param color         ARGB color / ARGB 색
         * @param decoration    decoration bits / 장식 비트
         * @param decoStyle     decoration style / 장식 스타일
         * @param decoColor     decoration color / 장식 색
         * @param decoThickness decoration thickness or NaN / 장식 두께 또는 NaN
         * @param node          DOM node or null / DOM 노드 또는 null
         */
        CombinedText(String text, Font font, double[] bounds, double ascent, double descent, int color, int decoration, int decoStyle, int decoColor,
                double decoThickness, Node node) {
            super(Type.TEXT, bounds[0] - 3, bounds[1] - 3, bounds[2] + 6, bounds[3] + 6, node);
            this.text = text;
            this.font = font;
            this.boxX = bounds[0];
            this.boxY = bounds[1];
            this.boxW = bounds[2];
            this.boxH = bounds[3];
            this.ascent = ascent;
            this.descent = descent;
            this.color = color;
            this.decoration = decoration;
            this.decoStyle = decoStyle;
            this.decoColor = decoColor;
            this.decoThickness = decoThickness;
        }

        /**
         * Paints the composed text and its decoration lines.
         * <p>
         * 짜 넣은 텍스트와 장식선을 그린다.
         *
         * @param ctx the context / 컨텍스트
         */
        void paint(PaintContext ctx) {
            Graphics2D g = ctx.g;
            double em = font.getSize2D();
            double tw = TextRenderer.advance(font, text);
            double scale = tw > em ? em / tw : 1;
            AffineTransform saved = g.getTransform();
            try {
                // 상자 가운데에서 가로로만 압축하며 그린다.
                g.translate(boxX + boxW / 2, boxY + boxH / 2 + (ascent - descent) / 2);
                g.scale(scale, 1);
                g.setColor(new Color(color, true));
                TextRenderer.draw(g, text, font, -tw / 2, 0, 0, 0, 0);
            } finally {
                g.setTransform(saved);
            }
            decorate(g, boxX + descent, boxY, true, boxH, ascent, em, decoration, decoStyle, decoColor, decoThickness);
        }
    }
}
