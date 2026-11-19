package org.duckdns.hjow.swingwebpanel.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * Draws text with Java2D in a way that cannot crash the JVM (plan items P09-04, P09-09). Finding on JRE 6u45 (Windows): for Hebrew,
 * <code>TextLayout</code> terminates the whole JVM and <code>drawString</code> of the whole string paints nothing; <code>GlyphVector</code>
 * drawing works for Hebrew, Arabic, Thai and Devanagari. Therefore text in the ranges that {@link #isComplex(String)} reports is never given to
 * <code>TextLayout</code>: it is drawn with <code>Font.createGlyphVector</code> + <code>drawGlyphVector</code> (no shaping: Arabic letters
 * keep their isolated forms, Indic conjuncts are not formed), right-to-left runs in reversed (visual) order. All other text uses
 * <code>drawString</code> (Korean, Latin, combining marks and surrogate pairs work) or, with letter/word spacing, one cluster at a time.
 * <p>
 * 텍스트를 JVM 을 죽이지 않는 방식으로 Java2D 로 그린다(계획 P09-04, P09-09). JRE 6u45(Windows)에서의 관찰: 히브리어에서 <code>TextLayout</code> 은 JVM
 * 전체를 종료시키고 문자열 전체의 <code>drawString</code> 은 아무것도 그리지 않으며, <code>GlyphVector</code> 로 그리면 히브리어, 아랍어, 태국어,
 * 데바나가리가 모두 그려진다. 그래서 {@link #isComplex(String)} 가 알리는 범위의 텍스트는 <code>TextLayout</code> 에 주지 않고
 * <code>Font.createGlyphVector</code> + <code>drawGlyphVector</code> 로 그린다(모양 변환 없음: 아랍 문자는 독립형 그대로, 인도계 결합은 만들지 않음).
 * 오른쪽에서 왼쪽 조각은 시각 순서(뒤집음)로 그린다. 그 밖의 텍스트는 <code>drawString</code>(한글, 라틴, 결합 문자, 서로게이트 쌍이 동작) 또는
 * 글자/단어 간격이 있으면 클러스터 하나씩 그린다.
 */
final class TextRenderer {

    /**
     * The render context that is used for glyph vectors and measuring (fractional metrics, as in the layout).
     * <p>
     * 글리프 벡터와 측정에 쓰는 렌더 컨텍스트(레이아웃과 같은 소수 메트릭).
     */
    static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * Decoration style: solid.
     * <p>
     * 장식 스타일: 실선.
     */
    static final int STYLE_SOLID = 0;

    /**
     * Decoration style: double.
     * <p>
     * 장식 스타일: 이중선.
     */
    static final int STYLE_DOUBLE = 1;

    /**
     * Decoration style: dotted.
     * <p>
     * 장식 스타일: 점선(점).
     */
    static final int STYLE_DOTTED = 2;

    /**
     * Decoration style: dashed.
     * <p>
     * 장식 스타일: 파선.
     */
    static final int STYLE_DASHED = 3;

    /**
     * Decoration style: wavy.
     * <p>
     * 장식 스타일: 물결선.
     */
    static final int STYLE_WAVY = 4;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextRenderer() {
    }

    /**
     * Tells whether a string contains characters of scripts that must not go through <code>TextLayout</code> on this JRE (Hebrew to Lao
     * blocks, Myanmar, Khmer, bidi controls, Arabic presentation forms).
     * <p>
     * 이 JRE 에서 <code>TextLayout</code> 에 주면 안 되는 문자 체계의 문자가 문자열에 있는지 알려 준다(히브리어~라오어 블록, 미얀마, 크메르,
     * 양방향 제어 문자, 아랍 표시 형태).
     *
     * @param s the string / 문자열
     * @return true if the glyph vector path must be used / 글리프 벡터 경로를 써야 하면 true
     */
    static boolean isComplex(String s) {
        // 문자를 하나씩 보며 위험한 범위에 드는 것이 있는지 찾는다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x0590) {
                continue;
            }
            if ((c >= 0x0590 && c <= 0x0EFF) || (c >= 0x1000 && c <= 0x109F) || (c >= 0x1780 && c <= 0x17FF) || (c >= 0x200C && c <= 0x200F)
                    || (c >= 0x202A && c <= 0x202E) || (c >= 0x2066 && c <= 0x2069) || (c >= 0xFB1D && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws a text fragment with its baseline origin at (x, baseline).
     * <p>
     * 기준선 원점이 (x, baseline) 인 텍스트 조각을 그린다.
     *
     * @param g             the graphics (color and hints are set by the caller) / 그래픽스(색과 힌트는 호출자가 정함)
     * @param text          the text in logical order / 논리 순서의 텍스트
     * @param font          the font / 글꼴
     * @param x             left edge / 왼쪽 가장자리
     * @param baseline      baseline y / 기준선 y
     * @param letterSpacing extra space after every character (px) / 모든 글자 뒤에 더하는 간격(px)
     * @param wordSpacing   extra space after every space (px) / 모든 공백 뒤에 더하는 간격(px)
     * @param bidiLevel     the bidi level of the fragment, odd for right-to-left / 조각의 양방향 수준. 홀수면 오른쪽에서 왼쪽
     */
    static void draw(Graphics2D g, String text, Font font, double x, double baseline, double letterSpacing, double wordSpacing, int bidiLevel) {
        if (text == null || text.length() == 0) {
            return;
        }
        g.setFont(font);
        if (isComplex(text)) {
            String visual = (bidiLevel & 1) == 1 ? reverse(text) : text;
            GlyphVector gv = font.createGlyphVector(FRC, visual);
            g.drawGlyphVector(gv, (float) x, (float) baseline);
            return;
        }
        if (letterSpacing == 0 && wordSpacing == 0) {
            g.drawString(text, (float) x, (float) baseline);
            return;
        }
        double cx = x;
        int i = 0;
        // 클러스터(기본 문자 + 결합 문자, 서로게이트 쌍)를 하나씩 그리며 간격을 더한다.
        while (i < text.length()) {
            int end = clusterEnd(text, i);
            String cluster = text.substring(i, end);
            g.drawString(cluster, (float) cx, (float) baseline);
            cx += advance(font, cluster) + letterSpacing + (cluster.equals(" ") ? wordSpacing : 0);
            i = end;
        }
    }

    /**
     * Returns the end of the cluster that starts at an index: a base character followed by its combining marks, or a surrogate pair.
     * <p>
     * 위치에서 시작하는 클러스터의 끝을 반환한다: 기본 문자와 뒤따르는 결합 문자, 또는 서로게이트 쌍.
     *
     * @param s     the string / 문자열
     * @param start start index / 시작 위치
     * @return end index (exclusive) / 끝 위치(미포함)
     */
    static int clusterEnd(String s, int start) {
        int i = start + 1;
        if (Character.isHighSurrogate(s.charAt(start)) && i < s.length() && Character.isLowSurrogate(s.charAt(i))) {
            i++;
        }
        // 뒤따르는 결합 문자를 같은 클러스터에 넣는다.
        while (i < s.length()) {
            int type = Character.getType(s.charAt(i));
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK) {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    /**
     * Reverses a string by code points (visual order of a right-to-left fragment).
     * <p>
     * 문자열을 코드 포인트 단위로 뒤집는다(오른쪽에서 왼쪽 조각의 시각 순서).
     *
     * @param s the string / 문자열
     * @return the reversed string / 뒤집은 문자열
     */
    static String reverse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = s.length();
        // 뒤에서부터 코드 포인트를 하나씩 붙인다(서로게이트 쌍은 순서를 유지한다).
        while (i > 0) {
            int cp = s.codePointBefore(i);
            sb.appendCodePoint(cp);
            i -= Character.charCount(cp);
        }
        return sb.toString();
    }

    /**
     * Measures the advance of a string that is not complex (the glyph vector bounds of complex text are not used for measuring here).
     * <p>
     * 복합 문자가 아닌 문자열의 advance 를 잰다(복합 텍스트는 여기서 측정에 쓰지 않는다).
     *
     * @param font the font / 글꼴
     * @param s    the string / 문자열
     * @return the advance in px / px 단위 advance
     */
    static double advance(Font font, String s) {
        if (s.length() == 0) {
            return 0;
        }
        return font.getStringBounds(s, FRC).getWidth();
    }

    /**
     * Draws the decoration lines of a fragment (underline, overline, line-through).
     * <p>
     * 조각의 장식선(밑줄, 윗줄, 취소선)을 그린다.
     *
     * @param g         the graphics / 그래픽스
     * @param bits      {@link org.duckdns.hjow.swingwebpanel.layout.LayoutBox#DECORATION_UNDERLINE} etc. / 장식 비트
     * @param style     one of the STYLE_ constants / STYLE_ 상수 중 하나
     * @param color     the line color (ARGB) / 선 색(ARGB)
     * @param thickness the line thickness in px, NaN or less than 1 for automatic / 선 두께(px). NaN 이거나 1 미만이면 자동
     * @param x         left edge / 왼쪽 가장자리
     * @param baseline  baseline y / 기준선 y
     * @param width     width of the fragment / 조각의 너비
     * @param ascent    ascent of the font / 글꼴의 ascent
     * @param fontSize  size of the font in px / 글꼴 크기(px)
     */
    static void drawDecorations(Graphics2D g, int bits, int style, int color, double thickness, double x, double baseline, double width,
            double ascent, double fontSize) {
        drawDecorations(g, bits, style, color, thickness, x, baseline, width, ascent, fontSize, null);
    }

    /**
     * Draws the decoration lines of a fragment with the details of <code>text-underline-offset</code>, <code>text-underline-position</code> and
     * <code>text-decoration-skip-ink</code> (CSS Text Decoration Level 4, https://www.w3.org/TR/css-text-decoration-4/#text-underline-offset-property and
     * https://www.w3.org/TR/css-text-decoration-4/#text-decoration-skip-ink-property): the underline is moved to a given distance below the baseline, or
     * below the descent of the font for <code>under</code>, and an underline or overline is interrupted where it would touch the glyphs (with a gap of
     * about the thickness of the line), unless <code>skip-ink: none</code>. The line-through is never interrupted.
     * <p>
     * <code>text-underline-offset</code>, <code>text-underline-position</code>, <code>text-decoration-skip-ink</code> 의 세부를 적용해 조각의 장식선을 그린다: 밑줄은
     * 기준선 아래 정해진 거리로(<code>under</code> 이면 글꼴의 descent 아래로) 옮기고, 밑줄과 윗줄은 글리프에 닿는 곳에서 선의 두께 정도의 틈을 두고 끊는다(<code>skip-ink: none</code> 이
     * 아니면). 취소선은 끊지 않는다.
     *
     * @param g         the graphics / 그래픽스
     * @param bits      decoration bits / 장식 비트
     * @param style     one of the STYLE_ constants / STYLE_ 상수 중 하나
     * @param color     the line color (ARGB) / 선 색(ARGB)
     * @param thickness the line thickness in px, NaN or less than 1 for automatic / 선 두께(px). NaN 이거나 1 미만이면 자동
     * @param x         left edge / 왼쪽 가장자리
     * @param baseline  baseline y / 기준선 y
     * @param width     width of the fragment / 조각의 너비
     * @param ascent    ascent of the font / 글꼴의 ascent
     * @param fontSize  size of the font in px / 글꼴 크기(px)
     * @param extras    the glyphs and the options of the underline, or null for plain lines / 글리프와 밑줄의 옵션. 보통 선이면 null
     */
    static void drawDecorations(Graphics2D g, int bits, int style, int color, double thickness, double x, double baseline, double width,
            double ascent, double fontSize, DecorationExtras extras) {
        double t = Double.isNaN(thickness) || thickness < 1 ? Math.max(1.0, Math.round(fontSize / 14.0)) : thickness;
        g.setColor(new Color(color, true));
        // 밑줄이나 윗줄이 글리프를 건너뛰어야 하면 글리프의 윤곽을 한 번만 만든다(건너뛰는 것은 밑줄과 윗줄뿐이다).
        java.awt.Shape ink = null;
        if (extras != null && extras.text != null && (bits & 3) != 0 && extras.text.length() <= MAX_SKIP_INK_CHARS) {
            ink = outline(extras.text, extras.font, x, baseline, extras.letterSpacing, extras.wordSpacing);
        }
        if ((bits & 1) != 0) {
            double y = baseline + Math.max(1.0, Math.round(fontSize * 0.1));
            // text-underline-offset 이 길이이면 기준선에서 그 거리에, under 이면 글꼴의 descent 아래에 놓는다.
            if (extras != null && !Double.isNaN(extras.underlineOffset)) {
                y = baseline + extras.underlineOffset;
            } else if (extras != null && extras.under) {
                y = baseline + extras.descent + Math.max(1.0, Math.round(fontSize * 0.05));
            }
            skipInk(g, ink, style, x, y, width, t);
        }
        if ((bits & 2) != 0) {
            skipInk(g, ink, style, x, baseline - ascent, width, t);
        }
        if ((bits & 4) != 0) {
            line(g, style, x, baseline - Math.round(ascent * 0.33) - t / 2.0, width, t);
        }
    }

    /**
     * The longest text for which an underline skips the ink (a longer fragment keeps a plain line, as the geometry is computed for every paint).
     * <p>
     * 밑줄이 잉크를 건너뛰는 텍스트의 최대 길이(더 긴 조각은 보통 선을 유지한다. 기하를 그릴 때마다 계산하기 때문이다).
     */
    private static final int MAX_SKIP_INK_CHARS = 400;

    /**
     * Draws a decoration line that leaves out the places where it would touch the ink of the glyphs: the line is drawn with a clip of the
     * line band without the glyph outline widened by the thickness of the line (the clearance on both sides of a glyph). Without ink, or if
     * no glyph reaches the band, the line is drawn as it is.
     * <p>
     * 글리프의 잉크에 닿는 곳을 비우고 장식선을 그린다: 선이 놓이는 띠에서 선 두께만큼 넓힌 글리프 윤곽(글리프 양옆의 여유)을 뺀 클립으로 선을 그린다. 잉크가 없거나 어떤 글리프도
     * 띠에 닿지 않으면 선을 그대로 그린다.
     *
     * @param g     the graphics / 그래픽스
     * @param ink   the outline of the glyphs or null / 글리프의 윤곽. 없으면 null
     * @param style the line style / 선 스타일
     * @param x     left edge / 왼쪽 가장자리
     * @param y     top edge of the line / 선의 위쪽 가장자리
     * @param w     length / 길이
     * @param t     thickness / 두께
     */
    private static void skipInk(Graphics2D g, java.awt.Shape ink, int style, double x, double y, double w, double t) {
        double amp = Math.max(1.0, t);
        double bandH = (style == STYLE_DOUBLE ? 3 * t : t) + 2 * amp;
        Rectangle2D band = new Rectangle2D.Double(x - amp, y - amp, w + 2 * amp, bandH);
        double gap = Math.max(1.0, t);
        Rectangle2D reach = new Rectangle2D.Double(band.getX() - gap, band.getY() - gap, band.getWidth() + 2 * gap, band.getHeight() + 2 * gap);
        if (ink == null || !ink.intersects(reach)) {
            line(g, style, x, y, w, t);
            return;
        }
        java.awt.geom.Area hole = new java.awt.geom.Area(ink);
        hole.intersect(new java.awt.geom.Area(reach));
        // 윤곽을 선 두께만큼 넓혀(획을 입혀) 글리프 둘레에 여유를 둔다.
        hole.add(new java.awt.geom.Area(new BasicStroke((float) (2 * gap), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(hole)));
        java.awt.geom.Area visible = new java.awt.geom.Area(band);
        visible.subtract(hole);
        java.awt.Shape old = g.getClip();
        try {
            g.clip(visible);
            line(g, style, x, y, w, t);
        } finally {
            g.setClip(old);
        }
    }

    /**
     * The glyphs of a text fragment and the options of its underline and overline: <code>text-underline-offset</code> (NaN for <code>auto</code>),
     * <code>text-underline-position: under</code>, the descent of the font and, if the lines skip the ink of the glyphs, the text with its font and
     * spacing from which the outline is made.
     * <p>
     * 텍스트 조각의 글리프와 밑줄, 윗줄의 옵션: <code>text-underline-offset</code>(<code>auto</code> 이면 NaN), <code>text-underline-position: under</code>, 글꼴의 descent,
     * 선이 글리프의 잉크를 건너뛰면 윤곽을 만드는 데 쓰는 텍스트와 글꼴, 간격.
     */
    static final class DecorationExtras {
        /** The offset of the underline from the baseline, NaN for auto. 기준선에서 밑줄까지의 거리. auto 이면 NaN. */
        final double underlineOffset;
        /** True for <code>text-underline-position: under</code>. <code>text-underline-position: under</code> 이면 true. */
        final boolean under;
        /** The descent of the font. 글꼴의 descent. */
        final double descent;
        /** The text whose glyphs interrupt the lines, or null if the lines are not interrupted. 선을 끊는 글리프의 텍스트. 끊지 않으면 null. */
        final String text;
        /** The font. 글꼴. */
        final Font font;
        /** Letter spacing. 글자 간격. */
        final double letterSpacing;
        /** Word spacing. 단어 간격. */
        final double wordSpacing;
        /** The bidi level of the fragment. 조각의 양방향 수준. */
        final int bidiLevel;

        /**
         * Creates the options.
         * <p>
         * 옵션을 만든다.
         *
         * @param underlineOffset offset of the underline or NaN / 밑줄의 거리 또는 NaN
         * @param under           true for position under / 위치가 under 이면 true
         * @param descent         descent of the font / 글꼴의 descent
         * @param text            the text for skip-ink or null / skip-ink 용 텍스트 또는 null
         * @param font            the font / 글꼴
         * @param letterSpacing   letter spacing / 글자 간격
         * @param wordSpacing     word spacing / 단어 간격
         * @param bidiLevel       bidi level / 양방향 수준
         */
        DecorationExtras(double underlineOffset, boolean under, double descent, String text, Font font, double letterSpacing, double wordSpacing,
                int bidiLevel) {
            this.underlineOffset = underlineOffset;
            this.under = under;
            this.descent = descent;
            this.text = text;
            this.font = font;
            this.letterSpacing = letterSpacing;
            this.wordSpacing = wordSpacing;
            this.bidiLevel = bidiLevel;
        }
    }

    /**
     * Returns the outline of a text fragment as it is drawn by {@link #draw} (the same positions of the glyphs, the clusters with their spacing), or null if the
     * text is of a script that must not go through the glyph outline code of this JRE.
     * <p>
     * {@link #draw} 가 그리는 대로 텍스트 조각의 윤곽을 반환한다(글리프 위치가 같고 클러스터마다 간격을 둔다). 이 JRE 의 글리프 윤곽 코드를 거치면 안 되는 문자 체계의 텍스트이면 null.
     *
     * @param text          the text / 텍스트
     * @param font          the font / 글꼴
     * @param x             left edge / 왼쪽 가장자리
     * @param baseline      baseline y / 기준선 y
     * @param letterSpacing extra space after every character / 모든 글자 뒤의 간격
     * @param wordSpacing   extra space after every space / 모든 공백 뒤의 간격
     * @return the outline or null / 윤곽. 없으면 null
     */
    static java.awt.Shape outline(String text, Font font, double x, double baseline, double letterSpacing, double wordSpacing) {
        if (text == null || text.length() == 0 || isComplex(text)) {
            return null;
        }
        if (letterSpacing == 0 && wordSpacing == 0) {
            return font.createGlyphVector(FRC, text).getOutline((float) x, (float) baseline);
        }
        java.awt.geom.Path2D.Double all = new java.awt.geom.Path2D.Double();
        double cx = x;
        int i = 0;
        // 클러스터마다 윤곽을 만들어 이어 붙이며 간격을 더한다.
        while (i < text.length()) {
            int end = clusterEnd(text, i);
            String cluster = text.substring(i, end);
            all.append(font.createGlyphVector(FRC, cluster).getOutline((float) cx, (float) baseline), false);
            cx += advance(font, cluster) + letterSpacing + (cluster.equals(" ") ? wordSpacing : 0);
            i = end;
        }
        return all;
    }

    /**
     * Draws one decoration line whose top edge is at y.
     * <p>
     * 위쪽 가장자리가 y 인 장식선 하나를 그린다.
     *
     * @param g     the graphics / 그래픽스
     * @param style the line style / 선 스타일
     * @param x     left edge / 왼쪽 가장자리
     * @param y     top edge / 위쪽 가장자리
     * @param w     length / 길이
     * @param t     thickness / 두께
     */
    private static void line(Graphics2D g, int style, double x, double y, double w, double t) {
        if (style == STYLE_SOLID) {
            g.fill(new Rectangle2D.Double(x, y, w, t));
        } else if (style == STYLE_DOUBLE) {
            g.fill(new Rectangle2D.Double(x, y, w, t));
            g.fill(new Rectangle2D.Double(x, y + 2 * t, w, t));
        } else if (style == STYLE_DOTTED || style == STYLE_DASHED) {
            double dash = style == STYLE_DOTTED ? t : 3 * t;
            // 선분을 일정한 길이의 조각으로 나누어 하나씩 채운다.
            for (double px = x; px < x + w; px += dash + t) {
                g.fill(new Rectangle2D.Double(px, y, Math.min(dash, x + w - px), t));
            }
        } else {
            Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath p = new GeneralPath();
            double amp = Math.max(1.0, t);
            double step = Math.max(2.0, 2 * t);
            p.moveTo(x, y + t / 2.0);
            boolean up = true;
            // 일정한 간격마다 위아래로 꺾이는 지그재그 선을 만든다.
            for (double px = x + step; px < x + w + step; px += step) {
                p.lineTo(Math.min(px, x + w), y + t / 2.0 + (up ? -amp : amp));
                up = !up;
            }
            g.setStroke(new BasicStroke((float) t));
            g.draw(p);
            if (aa != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        }
    }
}
