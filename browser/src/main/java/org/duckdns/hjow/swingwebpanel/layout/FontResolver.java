package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;

/**
 * Chooses the Java2D font for a computed style (plan item P09-03): the families of <code>font-family</code> are tried in order, generic
 * family names map to the logical fonts of the JRE, and a family is only used if it can display the text (otherwise the next family is
 * tried and finally the composite logical font <code>Dialog</code>, which falls back across scripts such as Hangul). Everything here works
 * without a display (headless). Fonts and metrics are cached.
 * <p>
 * 계산 스타일에 맞는 Java2D 글꼴을 고른다(계획 P09-03): <code>font-family</code> 의 패밀리를 차례로 시도하고, 일반 패밀리 이름은 JRE 의 논리
 * 글꼴로 바꾸며, 패밀리는 텍스트를 표시할 수 있을 때만 쓴다(아니면 다음 패밀리, 마지막으로 한글 같은 문자 체계로 대체 글꼴을 찾는 합성 논리 글꼴
 * <code>Dialog</code>). 모두 디스플레이 없이(headless) 동작한다. 글꼴과 메트릭은 캐시한다.
 */
final class FontResolver {

    /** The render context used for every measurement (anti-aliased, fractional metrics). 모든 측정에 쓰는 렌더 컨텍스트(안티앨리어싱, 소수 메트릭). */
    static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /** Lower-case names of the installed font families, loaded on first use. 설치된 글꼴 패밀리의 소문자 이름(처음 쓸 때 읽음). */
    private static Set<String> families;

    /** Cache of fonts by family, style and size. 패밀리, 스타일, 크기별 글꼴 캐시. */
    private static final Map<String, Font> FONTS = new LinkedHashMap<String, Font>(64, 0.75f, true) {
        /** Serialization version id. 직렬화 버전 식별자. */
        private static final long serialVersionUID = 1L;

        /**
         * Removes the eldest entry when the cache is full.
         * <p>
         * 캐시가 차면 가장 오래된 항목을 제거한다.
         *
         * @param eldest the eldest entry / 가장 오래된 항목
         * @return true to remove it / 제거하려면 true
         */
        protected boolean removeEldestEntry(Map.Entry<String, Font> eldest) {
            return size() > 512;
        }
    };

    /** Cache of font metrics (ascent, descent, leading) by font. 글꼴별 메트릭(ascent, descent, leading) 캐시. */
    private static final Map<Font, double[]> METRICS = new LinkedHashMap<Font, double[]>(64, 0.75f, true) {
        /** Serialization version id. 직렬화 버전 식별자. */
        private static final long serialVersionUID = 1L;

        /**
         * Removes the eldest entry when the cache is full.
         * <p>
         * 캐시가 차면 가장 오래된 항목을 제거한다.
         *
         * @param eldest the eldest entry / 가장 오래된 항목
         * @return true to remove it / 제거하려면 true
         */
        protected boolean removeEldestEntry(Map.Entry<Font, double[]> eldest) {
            return size() > 512;
        }
    };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FontResolver() {
    }

    /** The web fonts of the page that is being laid out on this thread (P10-05), or none. 이 스레드에서 배치하는 페이지의 웹 글꼴(P10-05). 없으면 없음. */
    private static final ThreadLocal<WebFontSource> WEB = new ThreadLocal<WebFontSource>();

    /**
     * Sets the web fonts for the layout that runs on the current thread (null removes them): the layout engine sets them for the time
     * of one layout, so a web font of a page never reaches the layout of another page.
     * <p>
     * 현재 스레드에서 실행되는 레이아웃의 웹 글꼴을 정한다(null 이면 제거): 레이아웃 엔진이 한 번의 레이아웃 동안 정하므로 한 페이지의 웹 글꼴이 다른 페이지의 레이아웃에
     * 닿지 않는다.
     *
     * @param source the web fonts or null / 웹 글꼴 또는 null
     */
    static void setWebFonts(WebFontSource source) {
        if (source == null) {
            WEB.remove();
        } else {
            WEB.set(source);
        }
    }

    /**
     * Returns the names of the installed families in lower case.
     * <p>
     * 설치된 패밀리 이름을 소문자로 반환한다.
     *
     * @return names / 이름
     */
    private static synchronized Set<String> installed() {
        if (families == null) {
            Set<String> s = new HashSet<String>();
            try {
                String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                // 설치된 패밀리 이름을 소문자로 모은다.
                for (int i = 0; i < names.length; i++) {
                    s.add(names[i].toLowerCase(Locale.ENGLISH));
                }
            } catch (RuntimeException e) {
                // 글꼴 목록을 읽을 수 없으면 논리 글꼴만 쓴다.
                s.clear();
            }
            families = s;
        }
        return families;
    }

    /**
     * Returns the base logical or physical font name for a CSS family name, or null if it is not available.
     * <p>
     * CSS 패밀리 이름에 해당하는 논리 또는 물리 글꼴 이름을 반환한다. 쓸 수 없으면 null.
     *
     * @param css a CSS family name / CSS 패밀리 이름
     * @return the Java font name or null / Java 글꼴 이름 또는 null
     */
    private static String javaName(String css) {
        String l = css.toLowerCase(Locale.ENGLISH);
        // 호스트가 일반 이름에 패밀리를 정해 두었고(API 계약 5.27) 그 패밀리가 설치되어 있으면 그것을 쓴다.
        WebFontSource web = WEB.get();
        if (web instanceof WebFontSet) {
            String chosen = ((WebFontSet) web).genericFamily(l);
            if (chosen != null && installed().contains(chosen.toLowerCase(Locale.ENGLISH))) {
                return chosen;
            }
        }
        if ("serif".equals(l) || "cursive".equals(l) || "fantasy".equals(l) || "ui-serif".equals(l)) {
            return Font.SERIF;
        }
        if ("sans-serif".equals(l) || "ui-sans-serif".equals(l)) {
            return Font.SANS_SERIF;
        }
        if ("monospace".equals(l) || "ui-monospace".equals(l)) {
            return Font.MONOSPACED;
        }
        if ("system-ui".equals(l) || "-apple-system".equals(l) || "ui-rounded".equals(l) || "dialog".equals(l)) {
            return Font.DIALOG;
        }
        return installed().contains(l) ? css : null;
    }

    /**
     * Removes characters from a sample text that fonts commonly cannot display but that do not matter for the choice.
     * <p>
     * 글꼴이 흔히 표시하지 못하지만 선택에는 영향이 없는 문자를 표본 텍스트에서 뺀다.
     *
     * @param s the text / 텍스트
     * @return the cleaned sample (at most 256 characters) / 정리한 표본(최대 256자)
     */
    private static String clean(String s) {
        StringBuilder b = new StringBuilder();
        // 앞쪽 일부만 훑어 제어 문자와 폭 없는 문자를 건너뛴다.
        for (int i = 0; i < s.length() && b.length() < 256; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && !(c >= 0x200B && c <= 0x200F) && c != 0xFEFF && c != 0x00AD && !(c >= 0x7F && c < 0xA0)) {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Chooses the font for a style and a sample of the text that it will display.
     * <p>
     * 스타일과 표시할 텍스트의 표본에 맞는 글꼴을 고른다.
     *
     * @param s      computed style / 계산 스타일
     * @param sample the text (null if unknown) / 텍스트(알 수 없으면 null)
     * @return the font, never null / 글꼴(null 이 아님)
     */
    static Font resolve(ComputedStyle s, String sample) {
        return resolve(s, sample, null);
    }

    /**
     * Chooses the font for a style and a sample of the text, and tells whether the text must be invisible: when a family of the list before the chosen one is a
     * web font that is still loading in the block period of its <code>font-display</code>, the text is laid out with the chosen (fallback) font but not painted
     * (CSS Fonts 4 section 4.8, "an invisible fallback font face").
     * <p>
     * 스타일과 텍스트 표본에 맞는 글꼴을 고르고 텍스트가 보이지 않아야 하는지 알려 준다: 고른 글꼴보다 앞의 패밀리가 <code>font-display</code> 의 block 기간에 아직 읽는 중인 웹 글꼴이면
     * 텍스트는 고른(대체) 글꼴로 배치하되 그리지 않는다(CSS Fonts 4 4.8 절, "보이지 않는 대체 글꼴 면").
     *
     * @param s       computed style / 계산 스타일
     * @param sample  the text (null if unknown) / 텍스트(알 수 없으면 null)
     * @param blocked receives in its first element the end of the block period (milliseconds of the system clock) or 0 if the text is visible; may be null /
     *                첫 원소에 block 기간의 끝(시스템 시계 ms)을 받는다. 텍스트가 보이면 0. null 가능
     * @return the font, never null / 글꼴(null 이 아님)
     */
    static synchronized Font resolve(ComputedStyle s, String sample, long[] blocked) {
        if (blocked != null) {
            blocked[0] = 0;
        }
        int style = (s.fontWeight() >= 600 ? Font.BOLD : Font.PLAIN);
        String fs = s.keyword(org.duckdns.hjow.swingwebpanel.css.props.Prop.FONT_STYLE);
        if ("italic".equals(fs) || "oblique".equals(fs)) {
            style |= Font.ITALIC;
        }
        // font-synthesis-weight/style 가 none 이면 글꼴에 진짜 굵은 면이나 기울인 면이 없을 때 JRE 가 합성하지 않게 한다(CSS Fonts 4 2.8).
        boolean synthBold = FontFeatures.mayRealize(s, "weight");
        boolean synthItalic = FontFeatures.mayRealize(s, "style");
        float size = (float) Math.max(0.01, s.fontSizePx());
        String clean = sample == null ? null : clean(sample);
        List<String> list = s.fontFamilies();
        // 패밀리 목록을 차례로 보며 텍스트를 모두 표시할 수 있는 첫 글꼴을 고른다.
        WebFontSource web = WEB.get();
        for (int i = 0; i < list.size(); i++) {
            // 페이지의 @font-face 가 그 패밀리를 선언했고 읽혔으면 웹 글꼴이 먼저다(P10-05).
            if (web != null) {
                Font wf = web.find(list.get(i), s.fontWeight(), (style & Font.ITALIC) != 0, clean, size, synthBold, synthItalic, s);
                if (wf != null) {
                    return wf;
                }
                // 처음 만난 대기 중인 웹 글꼴이 block 기간이면 뒤에서 고르는 대체 글꼴은 보이지 않게 그린다(CSS Fonts 4 4.8).
                if (blocked != null && blocked[0] == 0) {
                    blocked[0] = web.blockedUntil(list.get(i), s.fontWeight(), (style & Font.ITALIC) != 0, clean);
                }
            }
            String name = javaName(list.get(i));
            if (name == null) {
                continue;
            }
            int used = restrict(name, style, synthBold, synthItalic);
            Font f = font(name, used, size);
            if (clean == null || clean.length() == 0 || f.canDisplayUpTo(clean) < 0) {
                return adjusted(f, name, used, size, s.number(org.duckdns.hjow.swingwebpanel.css.props.Prop.FONT_SIZE_ADJUST));
            }
        }
        int used = restrict(Font.DIALOG, style, synthBold, synthItalic);
        return adjusted(font(Font.DIALOG, used, size), Font.DIALOG, used, size, s.number(org.duckdns.hjow.swingwebpanel.css.props.Prop.FONT_SIZE_ADJUST));
    }

    /**
     * Takes the bold and the italic bits away from a style when they would be synthesized by the JRE and the style does not allow it (<code>font-synthesis-weight: none</code>,
     * <code>font-synthesis-style: none</code>): a bit stays if the family has a real face for it. The JRE gives no direct answer, so a real face is recognized by its name:
     * the font of the family in that style has another face name (such as <code>Arial Bold</code>) than the plain font, while a synthesized one reports the name of the plain face.
     * <p>
     * JRE 가 합성하게 될 굵게/기울임 비트를 스타일이 허락하지 않으면(<code>font-synthesis-weight: none</code>, <code>font-synthesis-style: none</code>) 스타일에서 뺀다. 패밀리에 그것의
     * 진짜 면이 있으면 비트를 그대로 둔다. JRE 가 직접 알려 주지 않으므로 면의 이름으로 알아본다: 그 스타일의 글꼴이 보통 글꼴과 다른 면 이름(<code>Arial Bold</code> 같은)을 가지면
     * 진짜 면이고, 합성한 글꼴은 보통 면의 이름을 보고한다.
     *
     * @param name       the Java font name / Java 글꼴 이름
     * @param style      the requested style bits / 요청한 스타일 비트
     * @param synthBold  true if a bold may be synthesized / 굵게를 합성해도 되면 true
     * @param synthItalic true if an italic may be synthesized / 기울임을 합성해도 되면 true
     * @return the style bits to use / 쓸 스타일 비트
     */
    static int restrict(String name, int style, boolean synthBold, boolean synthItalic) {
        int used = style;
        // 합성이 금지된 비트마다 진짜 면이 없으면 뺀다.
        if ((style & Font.BOLD) != 0 && !synthBold && !realFace(name, Font.BOLD)) {
            used &= ~Font.BOLD;
        }
        if ((style & Font.ITALIC) != 0 && !synthItalic && !realFace(name, Font.ITALIC)) {
            used &= ~Font.ITALIC;
        }
        return used;
    }

    /**
     * Tells whether a family has a real face for a style bit (see {@link #restrict}).
     * <p>
     * 패밀리에 스타일 비트의 진짜 면이 있는지 알려 준다({@link #restrict} 참조).
     *
     * @param name the Java font name / Java 글꼴 이름
     * @param bit  {@link Font#BOLD} or {@link Font#ITALIC} / {@link Font#BOLD} 나 {@link Font#ITALIC}
     * @return true if the face is real / 진짜 면이면 true
     */
    private static boolean realFace(String name, int bit) {
        return !font(name, bit, 12).getFontName().equals(font(name, Font.PLAIN, 12).getFontName());
    }

    /**
     * Applies <code>font-size-adjust</code> (CSS Fonts Level 4 section 3.6): the font size is scaled so that the x-height of the font, as a fraction
     * of the size, becomes the given aspect value (<code>size × aspect ÷ the aspect of the font</code>). The x-height is measured on the glyph of
     * <code>x</code>; a font without a measurable one keeps its size.
     * <p>
     * <code>font-size-adjust</code> 를 적용한다(CSS Fonts Level 4 3.6 절): 글꼴의 x 높이가 크기에 대한 비율로 주어진 aspect 값이 되도록 크기를 조정한다(<code>크기 × aspect ÷
     * 글꼴의 aspect</code>). x 높이는 <code>x</code> 글리프로 재며, 잴 수 없는 글꼴은 크기를 유지한다.
     *
     * @param f      the font at the nominal size / 명목 크기의 글꼴
     * @param name   Java font name / Java 글꼴 이름
     * @param style  Font style bits / 글꼴 스타일 비트
     * @param size   the nominal size in pixels / 명목 크기(픽셀)
     * @param adjust the value of <code>font-size-adjust</code>, NaN or not positive for <code>none</code> / <code>font-size-adjust</code> 값. <code>none</code> 이면 NaN 이나 0 이하
     * @return the font at the adjusted size / 조정한 크기의 글꼴
     */
    private static Font adjusted(Font f, String name, int style, float size, double adjust) {
        if (Double.isNaN(adjust) || adjust <= 0) {
            return f;
        }
        double aspect = f.createGlyphVector(FRC, "x").getVisualBounds().getHeight() / Math.max(0.01, f.getSize2D());
        if (aspect < 0.05 || aspect > 2) {
            return f;
        }
        return font(name, style, (float) Math.max(0.01, Math.min(size * 4, size * adjust / aspect)));
    }

    /**
     * Returns a cached font.
     * <p>
     * 캐시된 글꼴을 반환한다.
     *
     * @param name  Java font name / Java 글꼴 이름
     * @param style Font style bits / 글꼴 스타일 비트
     * @param size  size in pixels / 픽셀 크기
     * @return the font / 글꼴
     */
    private static Font font(String name, int style, float size) {
        String key = name + '|' + style + '|' + size;
        Font f = FONTS.get(key);
        if (f == null) {
            f = new Font(name, style, 1).deriveFont(size);
            FONTS.put(key, f);
        }
        return f;
    }

    /**
     * Returns the metrics of a font: ascent, descent and leading in pixels.
     * <p>
     * 글꼴의 메트릭을 반환한다: ascent, descent, leading(픽셀).
     *
     * @param f the font / 글꼴
     * @return array of ascent, descent, leading / ascent, descent, leading 배열
     */
    static synchronized double[] metrics(Font f) {
        double[] m = METRICS.get(f);
        if (m == null) {
            LineMetrics lm = f.getLineMetrics("Ag", FRC);
            m = new double[] { lm.getAscent(), lm.getDescent(), lm.getLeading() };
            METRICS.put(f, m);
        }
        return m;
    }

    /** Graphics object that provides font metrics without a display (created on first use). 디스플레이 없이 글꼴 메트릭을 주는 그래픽스 객체(처음 쓸 때 만듦). */
    private static java.awt.Graphics2D metricsGraphics;

    /**
     * Tells whether a string contains characters of scripts that the JRE lays out with its native complex-text engine (Hebrew, Arabic and
     * the scripts that follow up to Khmer, bidi controls and Arabic presentation forms). On JRE 6 that engine can crash the whole JVM
     * (<code>fontmanager.dll</code>), so such text is measured with per-character advances instead of {@link java.awt.Font#getStringBounds}.
     * <p>
     * 문자열에 JRE 가 네이티브 복합 텍스트 엔진으로 배치하는 문자 체계(히브리어, 아랍어와 크메르어까지 이어지는 문자 체계, 양방향 제어 문자, 아랍어 표시
     * 형태)의 문자가 있는지 알려 준다. JRE 6 에서는 그 엔진이 JVM 전체를 죽일 수 있으므로(<code>fontmanager.dll</code>) 그런 텍스트는
     * {@link java.awt.Font#getStringBounds} 대신 글자별 advance 로 잰다.
     *
     * @param s the string / 문자열
     * @return true if complex / 복합이면 true
     */
    static boolean isComplex(String s) {
        // 문자를 하나씩 보며 복합 문자 체계의 범위에 드는 것이 있는지 찾는다.
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
     * Measures a string as the sum of the advances of its code points (no shaping, no kerning, no native complex-text layout).
     * <p>
     * 문자열을 코드 포인트별 advance 의 합으로 잰다(문자 모양 변환, 커닝, 네이티브 복합 텍스트 배치 없음).
     *
     * @param f the font / 글꼴
     * @param s the string / 문자열
     * @return width in pixels / 픽셀 너비
     */
    static synchronized double simpleAdvance(Font f, String s) {
        if (metricsGraphics == null) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            metricsGraphics = img.createGraphics();
            metricsGraphics.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        java.awt.FontMetrics fm = metricsGraphics.getFontMetrics(f);
        double w = 0;
        int i = 0;
        // 코드 포인트마다 advance 를 더한다.
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            w += fm.charWidth(cp);
            i += Character.charCount(cp);
        }
        return w;
    }
}
