package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The text layout of the 2D context: it prepares the text (ASCII white space becomes a space), picks the Java font, lays the glyphs out as a
 * <code>GlyphVector</code> and returns their outline placed relative to the anchor point according to <code>textAlign</code> and
 * <code>textBaseline</code>, together with the numbers of <code>TextMetrics</code>. The outline is filled or stroked with the same code as any
 * other shape, so gradients, patterns, transformations, shadows and compositing work for text as they do for paths. Text direction
 * <code>inherit</code> is treated as left to right. <code>letterSpacing</code> and <code>wordSpacing</code> move the glyphs apart (the
 * spacing after the last glyph is part of the width), <code>fontKerning = "normal"</code> (not on JRE 6, see {@link #KERNING_SUPPORTED}) lays the text out with the kerning attribute of the
 * font, and Hebrew text never reaches <code>Font.layoutGlyphVector</code> because that crashes the JVM of JRE 6u45 on Windows.
 * <p>
 * 2D 컨텍스트의 텍스트 배치: 텍스트를 준비하고(ASCII 공백은 스페이스가 된다), Java 글꼴을 고르고, 글리프를 <code>GlyphVector</code> 로 배치하여
 * <code>textAlign</code> 과 <code>textBaseline</code> 에 따라 기준점에 상대적으로 놓인 윤곽을 <code>TextMetrics</code> 의 숫자와 함께 돌려준다. 윤곽은 다른
 * 도형과 같은 코드로 채우거나 그리므로 그라디언트, 패턴, 변환, 그림자, 합성이 경로와 똑같이 텍스트에도 동작한다. 텍스트 방향 <code>inherit</code> 는 왼쪽에서
 * 오른쪽으로 본다. <code>letterSpacing</code> 과 <code>wordSpacing</code> 은 글리프를 벌리고(마지막 글리프 뒤의 간격도 너비에 포함된다),
 * <code>fontKerning = "normal"</code> 은(JRE 6 제외, {@link #KERNING_SUPPORTED} 참고) 글꼴의 커닝 속성을 켜고 텍스트를 배치하며, 히브리어는 JRE 6u45(Windows)에서 JVM 을 죽이는
 * <code>Font.layoutGlyphVector</code> 에 절대 주지 않는다.
 */
final class CanvasText {

    /**
     * The font render context of the canvas: antialiased text with fractional metrics.
     * <p>
     * 캔버스의 글꼴 렌더 컨텍스트: 안티앨리어싱된 텍스트와 소수 자리 측정.
     */
    static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /**
     * Tells whether <code>fontKerning = "normal"</code> is carried out. It is not on JRE 6: the layout engine of JRE 6u45 for Windows
     * (<code>fontmanager.dll</code>) is not reliable when the kerning attribute is used. Reproduced with a plain Java program (no code of this
     * project): <code>Font.layoutGlyphVector</code> of the family Tahoma, called after a layout with a logical font such as SansSerif or Dialog,
     * ends the whole JVM with an access violation in about one run out of twenty, and the widths it returns for Tahoma with kerning on are
     * sometimes far too wide (about 30% instead of about 8% narrower). A crash of the host's JVM is worse than a text without kerning, so on
     * JRE 6 the request is ignored (the same width as <code>auto</code>); newer Java versions kern.
     * <p>
     * <code>fontKerning = "normal"</code> 을 실제로 수행하는지 알려 준다. JRE 6 에서는 수행하지 않는다: Windows 의 JRE 6u45 배치 엔진
     * (<code>fontmanager.dll</code>)은 커닝 속성을 쓸 때 믿을 수 없다. 이 프로젝트의 코드가 없는 Java 프로그램으로 재현했다: SansSerif, Dialog 같은
     * 논리 글꼴로 배치한 뒤 부른 Tahoma 패밀리의 <code>Font.layoutGlyphVector</code> 는 스무 번에 한 번쯤 글꼴 관리자의 접근 위반으로 JVM 전체를 끝내고,
     * 커닝을 켠 Tahoma 의 너비를 가끔 너무 넓게(약 8% 좁아지는 대신 약 30% 넓게) 돌려준다. 호스트의 JVM 이 죽는 것이 커닝 없는 텍스트보다 나쁘므로
     * JRE 6 에서는 요청을 무시하고(<code>auto</code> 와 같은 너비), 더 새로운 Java 에서는 커닝한다.
     */
    static final boolean KERNING_SUPPORTED = !"1.6".equals(System.getProperty("java.specification.version"));

    /**
     * The laid out text.
     * <p>
     * 배치된 텍스트.
     */
    static final class Laid {
        /**
         * The outline relative to the anchor point (alignment and baseline applied), or null for empty text.
         * <p>
         * 기준점에 상대적인 윤곽(정렬과 기준선 적용). 빈 텍스트이면 null.
         */
        Shape outline;

        /**
         * The twelve numbers of <code>TextMetrics</code> (see {@link TextMetricsValue}).
         * <p>
         * <code>TextMetrics</code> 의 열두 숫자({@link TextMetricsValue} 참고).
         */
        double[] metrics;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CanvasText() {
    }

    /**
     * Replaces ASCII white space by spaces as the canvas text preparation requires.
     * <p>
     * 캔버스 텍스트 준비가 요구하는 대로 ASCII 공백을 스페이스로 바꾼다.
     *
     * @param text the text / 텍스트
     * @return the prepared text / 준비된 텍스트
     */
    static String prepare(String text) {
        StringBuilder b = new StringBuilder(text.length());
        // 탭, 줄바꿈, 폼 피드, 캐리지 리턴을 스페이스로 바꾼다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            b.append(c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r' ? ' ' : c);
        }
        return b.toString();
    }

    /**
     * Tells whether a text needs the full layout engine (right-to-left or complex scripts).
     * <p>
     * 텍스트가 전체 배치 엔진(오른쪽에서 왼쪽 또는 복합 문자)을 필요로 하는지 알려 준다.
     *
     * @param s the text / 텍스트
     * @return true if complex / 복합이면 true
     */
    private static boolean isComplex(String s) {
        // 복합 문자 체계의 범위에 드는 문자가 있는지 찾는다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x0590 && c <= 0x0EFF) || (c >= 0x1000 && c <= 0x109F) || (c >= 0x1780 && c <= 0x17FF) || (c >= 0x200C && c <= 0x200F)
                    || (c >= 0x202A && c <= 0x202E) || (c >= 0xFB1D && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a text has Hebrew letters (the Hebrew block and its presentation forms). Finding on JRE 6u45 for Windows: for Hebrew,
     * <code>Font.layoutGlyphVector</code> (like <code>TextLayout</code>) terminates the whole JVM with an access violation in the font manager,
     * so such text is never given to the layout engine.
     * <p>
     * 텍스트에 히브리 문자(히브리어 블록과 그 표시 형태)가 있는지 알려 준다. JRE 6u45(Windows)에서의 관찰: 히브리어에서 <code>Font.layoutGlyphVector</code> 는
     * (<code>TextLayout</code> 처럼) 글꼴 관리자의 접근 위반으로 JVM 전체를 종료시키므로 그런 텍스트는 배치 엔진에 주지 않는다.
     *
     * @param s the text / 텍스트
     * @return true if there is a Hebrew character / 히브리 문자가 있으면 true
     */
    private static boolean hasHebrew(String s) {
        // 히브리어 범위의 문자를 찾는다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x0590 && c <= 0x05FF) || (c >= 0xFB1D && c <= 0xFB4F)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lays a text out.
     * <p>
     * 텍스트를 배치한다.
     *
     * @param raw      the text as given by the script / 스크립트가 준 텍스트
     * @param state    the drawing state (font, alignment, baseline, direction) / 그리기 상태(글꼴, 정렬, 기준선, 방향)
     * @param maxWidth the maximum width, or NaN for none / 최대 너비. 없으면 NaN
     * @return the result / 결과
     */
    static Laid layout(String raw, DrawState state, double maxWidth) {
        String text = prepare(raw);
        Font font = state.font.resolve(text);
        boolean rtl = "rtl".equals(state.direction);
        LineMetrics lm = font.getLineMetrics(text.length() == 0 ? "x" : text, FRC);
        double ascent = lm.getAscent();
        double descent = lm.getDescent();
        double size = font.getSize2D();
        double emAscent = ascent + descent > 0 ? size * ascent / (ascent + descent) : size * 0.8;
        double emDescent = size - emAscent;
        double hanging = emAscent * 0.8;
        double up = baselineUp(state.textBaseline, emAscent, emDescent, hanging);
        Laid out = new Laid();
        double advance = 0;
        GlyphVector gv = null;
        // 빈 텍스트가 아니면 글리프를 배치한다.
        if (text.length() > 0) {
            // JRE 6 의 배치 엔진은 커닝에서 믿을 수 없으므로(KERNING_SUPPORTED 참고) 거기서는 커닝 요청을 무시한다.
            boolean kern = KERNING_SUPPORTED && "normal".equals(state.fontKerning);
            String laid = text;
            // 히브리어는 JRE 6u45(Windows)의 배치 엔진이 JVM 을 종료시키므로 배치 엔진에 주지 않고 글리프를 일대일로 만든다(오른쪽에서 왼쪽이면 시각 순서로 뒤집는다).
            if (hasHebrew(text)) {
                laid = rtl ? new StringBuilder(text).reverse().toString() : text;
                gv = font.createGlyphVector(FRC, laid);
            } else {
                // 커닝을 요청했으면 커닝 속성을 켠 글꼴로 전체 배치를 한다.
                if (kern) {
                    Map<TextAttribute, Object> attrs = new HashMap<TextAttribute, Object>();
                    attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
                    font = font.deriveFont(attrs);
                }
                gv = isComplex(text) || kern ? font.layoutGlyphVector(FRC, text.toCharArray(), 0, text.length(), rtl ? Font.LAYOUT_RIGHT_TO_LEFT
                        : Font.LAYOUT_LEFT_TO_RIGHT) : font.createGlyphVector(FRC, text);
            }
            applySpacing(gv, laid, state.letterSpacing.pixels(size), state.wordSpacing.pixels(size));
            advance = gv.getGlyphPosition(gv.getNumGlyphs()).getX();
        }
        double scaleX = 1;
        // 최대 너비보다 넓으면 가로로 줄인다.
        if (maxWidth == maxWidth && advance > maxWidth && advance > 0) {
            scaleX = maxWidth / advance;
        }
        double drawn = advance * scaleX;
        double xOff = alignOffset(state.textAlign, rtl, drawn);
        double yOff = up;
        double left = 0;
        double right = 0;
        double inkAscent = 0;
        double inkDescent = 0;
        if (gv != null) {
            AffineTransform at = new AffineTransform();
            at.translate(xOff, yOff);
            at.scale(scaleX, 1);
            out.outline = at.createTransformedShape(gv.getOutline(0f, 0f));
            Rectangle2D vb = gv.getVisualBounds();
            // 잉크 상자가 비어 있지 않으면 정렬 기준점과 기준선에서의 거리를 계산한다.
            if (vb.getWidth() > 0 || vb.getHeight() > 0) {
                left = -(xOff + vb.getX() * scaleX);
                right = xOff + (vb.getX() + vb.getWidth()) * scaleX;
                inkAscent = -(yOff + vb.getY());
                inkDescent = yOff + vb.getY() + vb.getHeight();
            }
        }
        out.metrics = new double[] { drawn, left, right, ascent - up, descent + up, inkAscent, inkDescent, emAscent - up, emDescent + up,
            up - hanging, up, up + emDescent };
        return out;
    }

    /**
     * Moves the glyphs apart by the letter spacing (added after every glyph, the last one included, so it is part of the width) and the word
     * spacing (added after every space and no-break space), the way a hypothetical CSS line box with <code>letter-spacing</code> and
     * <code>word-spacing</code> does. The glyph order is the visual order, so right-to-left text is spread the same way.
     * <p>
     * 글리프를 글자 간격(마지막 글리프를 포함해 모든 글리프 뒤에 더하므로 너비에 포함된다)과 낱말 간격(모든 스페이스와 줄바꿈 없는 스페이스 뒤에 더함)만큼
     * 벌린다. <code>letter-spacing</code>, <code>word-spacing</code> 이 있는 가상의 CSS 줄 상자와 같다. 글리프 순서는 눈에 보이는 순서이므로 오른쪽에서 왼쪽 텍스트도
     * 똑같이 벌어진다.
     *
     * @param gv     the glyphs, changed in place / 글리프(제자리에서 바뀜)
     * @param text   the text the glyphs were made from / 글리프를 만든 텍스트
     * @param letter the letter spacing in pixels / 글자 간격(화소)
     * @param word   the word spacing in pixels / 낱말 간격(화소)
     */
    private static void applySpacing(GlyphVector gv, String text, double letter, double word) {
        if (letter == 0 && word == 0) {
            return;
        }
        double shift = 0;
        int n = gv.getNumGlyphs();
        // 글리프를 앞에서부터 보며 지금까지의 간격만큼 밀고 자기 뒤의 간격을 더한다.
        for (int i = 0; i < n; i++) {
            Point2D p = gv.getGlyphPosition(i);
            gv.setGlyphPosition(i, new Point2D.Double(p.getX() + shift, p.getY()));
            shift += letter;
            int index = gv.getGlyphCharIndex(i);
            char ch = index >= 0 && index < text.length() ? text.charAt(index) : 0;
            // 스페이스와 줄바꿈 없는 스페이스는 낱말 구분 문자이다.
            if (ch == ' ' || ch == (char) 0x00A0) {
                shift += word;
            }
        }
        Point2D end = gv.getGlyphPosition(n);
        gv.setGlyphPosition(n, new Point2D.Double(end.getX() + shift, end.getY()));
    }

    /**
     * Returns how far the chosen baseline lies above the alphabetic baseline.
     * <p>
     * 고른 기준선이 alphabetic 기준선보다 얼마나 위에 있는지 반환한다.
     *
     * @param baseline  the value of <code>textBaseline</code> / <code>textBaseline</code> 값
     * @param emAscent  the ascent of the em square / em 사각형의 ascent
     * @param emDescent the descent of the em square / em 사각형의 descent
     * @param hanging   the height of the hanging baseline / hanging 기준선의 높이
     * @return the distance, upwards positive / 거리(위쪽이 양수)
     */
    private static double baselineUp(String baseline, double emAscent, double emDescent, double hanging) {
        if ("top".equals(baseline)) {
            return emAscent;
        } else if ("hanging".equals(baseline)) {
            return hanging;
        } else if ("middle".equals(baseline)) {
            return (emAscent - emDescent) / 2;
        } else if ("ideographic".equals(baseline) || "bottom".equals(baseline)) {
            return -emDescent;
        }
        return 0;
    }

    /**
     * Returns the horizontal offset of the text from the anchor point for an alignment.
     * <p>
     * 정렬에 따른 텍스트의 기준점으로부터의 가로 오프셋을 반환한다.
     *
     * @param align the value of <code>textAlign</code> / <code>textAlign</code> 값
     * @param rtl   true for right-to-left text / 오른쪽에서 왼쪽 텍스트이면 true
     * @param width the advance width / 진행 너비
     * @return the offset / 오프셋
     */
    private static double alignOffset(String align, boolean rtl, double width) {
        String a = align;
        // start 와 end 는 방향에 따라 left 와 right 로 바꾼다.
        if ("start".equals(a)) {
            a = rtl ? "right" : "left";
        } else if ("end".equals(a)) {
            a = rtl ? "left" : "right";
        }
        if ("right".equals(a)) {
            return -width;
        }
        return "center".equals(a) ? -width / 2 : 0;
    }
}
