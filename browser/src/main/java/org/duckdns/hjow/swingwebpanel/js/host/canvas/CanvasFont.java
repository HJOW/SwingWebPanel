package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A parsed value of the <code>font</code> attribute of the 2D context (the CSS <code>font</code> shorthand without line height): optional style
 * (<code>italic</code>, <code>oblique</code>), variant (<code>small-caps</code>, parsed and ignored), weight (<code>bold</code>,
 * <code>bolder</code>, <code>lighter</code>, 100 to 900), stretch keywords (parsed and ignored), a size and a family list. The size units
 * <code>px pt pc in cm mm q em rem ex %</code> and the size keywords <code>xx-small</code> to <code>xxx-large</code>, <code>smaller</code> and
 * <code>larger</code> are understood; <code>em</code> and percentages are relative to the font size of the canvas element. System fonts and
 * <code>calc()</code> are not supported (the assignment is then ignored, as for any invalid value). The families map to Java fonts as the layout
 * engine maps them (<code>serif</code>, <code>sans-serif</code>, <code>monospace</code> and the installed families; the first family that can
 * display the text is used).
 * <p>
 * 2D 컨텍스트의 <code>font</code> 속성 값을 해석한 결과(줄 높이가 없는 CSS <code>font</code> 단축 속성): 선택적인 스타일(<code>italic</code>,
 * <code>oblique</code>), 변형(<code>small-caps</code>, 해석만 하고 무시), 굵기(<code>bold</code>, <code>bolder</code>, <code>lighter</code>, 100~900),
 * 늘임 키워드(해석만 하고 무시), 크기, 글꼴 패밀리 목록. 크기 단위 <code>px pt pc in cm mm q em rem ex %</code> 와 크기 키워드
 * <code>xx-small</code> ~ <code>xxx-large</code>, <code>smaller</code>, <code>larger</code> 를 이해하며 <code>em</code> 과 백분율은 캔버스 요소의 글꼴
 * 크기에 상대적이다. 시스템 글꼴과 <code>calc()</code> 는 지원하지 않는다(그러면 다른 잘못된 값처럼 대입이 무시된다). 패밀리는 레이아웃 엔진과 같게
 * Java 글꼴에 대응한다(<code>serif</code>, <code>sans-serif</code>, <code>monospace</code>, 설치된 패밀리. 텍스트를 표시할 수 있는 첫 패밀리를 쓴다).
 */
public final class CanvasFont {

    /**
     * The default font, <code>10px sans-serif</code>.
     * <p>
     * 기본 글꼴 <code>10px sans-serif</code>.
     */
    public static final String DEFAULT = "10px sans-serif";

    /**
     * The size in pixels of a font size keyword, from <code>xx-small</code> to <code>xxx-large</code>.
     * <p>
     * 크기 키워드(<code>xx-small</code> 부터 <code>xxx-large</code> 까지)의 화소 크기.
     */
    private static final double[] KEYWORD_SIZES = { 9, 10, 13, 16, 18, 24, 32, 48 };

    /**
     * The size keywords, in the order of {@link #KEYWORD_SIZES}.
     * <p>
     * {@link #KEYWORD_SIZES} 와 같은 순서의 크기 키워드.
     */
    private static final String[] KEYWORDS = { "xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large", "xxx-large" };

    /**
     * The families installed on the machine in lower case, read once.
     * <p>
     * 이 컴퓨터에 설치된 패밀리(소문자). 한 번만 읽는다.
     */
    private static Set<String> installed;

    /**
     * True if the style is italic or oblique.
     * <p>
     * 스타일이 italic 또는 oblique 이면 true.
     */
    private final boolean italic;

    /**
     * The weight 100..900.
     * <p>
     * 굵기 100..900.
     */
    private final int weight;

    /**
     * The size in CSS pixels.
     * <p>
     * 크기(CSS 화소).
     */
    private final double size;

    /**
     * The families as written (quotes removed).
     * <p>
     * 쓰인 대로의 패밀리(따옴표 제거).
     */
    private final List<String> families;

    /**
     * Creates a font description.
     * <p>
     * 글꼴 기술을 만든다.
     *
     * @param italic   italic or oblique / italic 또는 oblique
     * @param weight   the weight / 굵기
     * @param size     the size in px / 크기(px)
     * @param families the families / 패밀리
     */
    private CanvasFont(boolean italic, int weight, double size, List<String> families) {
        this.italic = italic;
        this.weight = weight;
        this.size = size;
        this.families = families;
    }

    /**
     * Returns the size in pixels.
     * <p>
     * 크기를 화소로 반환한다.
     *
     * @return the size / 크기
     */
    public double getSize() {
        return size;
    }

    /**
     * Returns the serialized form that the <code>font</code> attribute returns: the style and weight when they are not normal, the size in
     * pixels and the families.
     * <p>
     * <code>font</code> 속성이 돌려주는 직렬화 형식을 반환한다: 보통이 아닌 스타일과 굵기, 화소 크기, 패밀리.
     *
     * @return the text / 문자열
     */
    public String serialize() {
        StringBuilder b = new StringBuilder();
        if (italic) {
            b.append("italic ");
        }
        if (weight == 700) {
            b.append("bold ");
        } else if (weight != 400) {
            b.append(weight).append(' ');
        }
        String s = Double.toString(size);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        b.append(s).append("px ");
        // 패밀리를 쉼표로 이어 붙인다. 공백이 있는 이름은 따옴표로 감싼다.
        for (int i = 0; i < families.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            String f = families.get(i);
            b.append(f.indexOf(' ') >= 0 ? "\"" + f + "\"" : f);
        }
        return b.toString();
    }

    /**
     * Parses a font value.
     * <p>
     * 글꼴 값을 해석한다.
     *
     * @param text          the text / 문자열
     * @param elementSizePx the font size of the canvas element that <code>em</code> and percentages refer to / <code>em</code> 과 백분율이 기준으로 삼는 캔버스 요소의 글꼴 크기
     * @return the font, or null if the text is not a valid font shorthand / 글꼴. 올바른 font 단축 값이 아니면 null
     */
    public static CanvasFont parse(String text, double elementSizePx) {
        if (text == null) {
            return null;
        }
        List<String> tokens = tokenize(text.trim());
        boolean italic = false;
        int weight = 400;
        int i = 0;
        // 크기 앞의 style, variant, weight, stretch 를 순서 없이 읽는다.
        while (i < tokens.size()) {
            String t = tokens.get(i).toLowerCase(Locale.ENGLISH);
            if ("normal".equals(t) || "small-caps".equals(t) || isStretch(t)) {
                i++;
            } else if ("italic".equals(t) || "oblique".equals(t)) {
                italic = true;
                i++;
            } else if ("bold".equals(t) || "bolder".equals(t)) {
                weight = 700;
                i++;
            } else if ("lighter".equals(t)) {
                weight = 300;
                i++;
            } else if (t.length() == 3 && t.charAt(0) >= '1' && t.charAt(0) <= '9' && t.endsWith("00")) {
                weight = (t.charAt(0) - '0') * 100;
                i++;
            } else {
                break;
            }
        }
        if (i >= tokens.size()) {
            return null;
        }
        String sizeToken = tokens.get(i);
        int slash = sizeToken.indexOf('/');
        // 크기 뒤의 /줄높이는 해석하되 버린다.
        if (slash >= 0) {
            sizeToken = sizeToken.substring(0, slash);
        }
        double size = parseSize(sizeToken.toLowerCase(Locale.ENGLISH), elementSizePx);
        if (Double.isNaN(size) || size < 0) {
            return null;
        }
        i++;
        // 크기와 /줄높이가 떨어져 있는 형식(12px / 1.5)도 허용한다.
        if (i < tokens.size() && tokens.get(i).startsWith("/")) {
            if (tokens.get(i).length() == 1) {
                i++;
            }
            i++;
        }
        List<String> families = parseFamilies(tokens, i);
        if (families == null || families.isEmpty()) {
            return null;
        }
        return new CanvasFont(italic, weight, size, families);
    }

    /**
     * Tells whether a token is a font-stretch keyword.
     * <p>
     * 토큰이 font-stretch 키워드인지 알려 준다.
     *
     * @param t the lower-case token / 소문자 토큰
     * @return true for a stretch keyword / 늘임 키워드이면 true
     */
    private static boolean isStretch(String t) {
        return t.endsWith("condensed") || t.endsWith("expanded");
    }

    /**
     * Splits a font value at white space, keeping quoted family names whole.
     * <p>
     * 글꼴 값을 공백으로 나누되 따옴표로 묶인 패밀리 이름은 하나로 둔다.
     *
     * @param text the text / 문자열
     * @return the tokens / 토큰
     */
    private static List<String> tokenize(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        // 글자를 하나씩 읽으며 따옴표 밖의 공백에서 토큰을 끊는다.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                cur.append(c);
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
                cur.append(c);
            } else if (Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    /**
     * Parses a size token.
     * <p>
     * 크기 토큰을 해석한다.
     *
     * @param t       the lower-case token / 소문자 토큰
     * @param parentPx the size that em and percentages refer to / em 과 백분율이 기준으로 삼는 크기
     * @return the size in px, or NaN if the token is not a size / 크기(px). 크기가 아니면 NaN
     */
    private static double parseSize(String t, double parentPx) {
        // 크기 키워드를 먼저 확인한다.
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (KEYWORDS[i].equals(t)) {
                return KEYWORD_SIZES[i];
            }
        }
        if ("larger".equals(t)) {
            return parentPx * 1.2;
        }
        if ("smaller".equals(t)) {
            return parentPx / 1.2;
        }
        int n = 0;
        // 숫자 부분(부호, 숫자, 소수점)의 길이를 센다.
        while (n < t.length() && (Character.isDigit(t.charAt(n)) || t.charAt(n) == '.' || (n == 0 && (t.charAt(n) == '+' || t.charAt(n) == '-')))) {
            n++;
        }
        if (n == 0) {
            return Double.NaN;
        }
        double v;
        try {
            v = Double.parseDouble(t.substring(0, n));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
        String unit = t.substring(n);
        if ("px".equals(unit)) {
            return v;
        } else if ("pt".equals(unit)) {
            return v * 4 / 3;
        } else if ("pc".equals(unit)) {
            return v * 16;
        } else if ("in".equals(unit)) {
            return v * 96;
        } else if ("cm".equals(unit)) {
            return v * 96 / 2.54;
        } else if ("mm".equals(unit)) {
            return v * 96 / 25.4;
        } else if ("q".equals(unit)) {
            return v * 96 / 101.6;
        } else if ("em".equals(unit)) {
            return v * parentPx;
        } else if ("rem".equals(unit)) {
            return v * 16;
        } else if ("ex".equals(unit)) {
            return v * parentPx / 2;
        } else if ("%".equals(unit)) {
            return v * parentPx / 100;
        } else if (unit.length() == 0 && v == 0) {
            return 0;
        }
        return Double.NaN;
    }

    /**
     * Parses the family list of the remaining tokens.
     * <p>
     * 남은 토큰의 패밀리 목록을 해석한다.
     *
     * @param tokens the tokens / 토큰
     * @param from   the first family token / 첫 패밀리 토큰
     * @return the families, or null if the list is malformed / 패밀리. 목록이 잘못되었으면 null
     */
    private static List<String> parseFamilies(List<String> tokens, int from) {
        StringBuilder joined = new StringBuilder();
        // 남은 토큰을 공백으로 이어 붙인다.
        for (int i = from; i < tokens.size(); i++) {
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(tokens.get(i));
        }
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        // 따옴표 밖의 쉼표에서 패밀리를 끊는다.
        for (int i = 0; i < joined.length(); i++) {
            char c = joined.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    cur.append(c);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ',') {
                String f = cur.toString().trim();
                if (f.length() == 0) {
                    return null;
                }
                out.add(f);
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (quote != 0) {
            return null;
        }
        String last = cur.toString().trim();
        if (last.length() == 0) {
            return null;
        }
        out.add(last);
        return out;
    }

    /**
     * Returns the families installed on the machine.
     * <p>
     * 이 컴퓨터에 설치된 패밀리를 반환한다.
     *
     * @return the lower-case names / 소문자 이름
     */
    private static synchronized Set<String> installedFamilies() {
        if (installed == null) {
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
            installed = s;
        }
        return installed;
    }

    /**
     * Maps a CSS family name to a Java font name.
     * <p>
     * CSS 패밀리 이름을 Java 글꼴 이름으로 대응시킨다.
     *
     * @param css the CSS name / CSS 이름
     * @return the Java name, or null if the family is not available / Java 이름. 쓸 수 없는 패밀리이면 null
     */
    private static String javaName(String css) {
        String l = css.toLowerCase(Locale.ENGLISH);
        if ("serif".equals(l) || "cursive".equals(l) || "fantasy".equals(l)) {
            return Font.SERIF;
        }
        if ("sans-serif".equals(l)) {
            return Font.SANS_SERIF;
        }
        if ("monospace".equals(l)) {
            return Font.MONOSPACED;
        }
        if ("system-ui".equals(l)) {
            return Font.DIALOG;
        }
        return installedFamilies().contains(l) ? css : null;
    }

    /**
     * Chooses the Java font for a text: the first family that is available and can display the whole text, else the dialog font.
     * <p>
     * 텍스트에 쓸 Java 글꼴을 고른다: 쓸 수 있고 텍스트를 모두 표시할 수 있는 첫 패밀리, 없으면 대화상자 글꼴.
     *
     * @param text the text to draw or measure (may be empty) / 그리거나 측정할 텍스트(비어 있을 수 있음)
     * @return the font of the size of this description / 이 기술의 크기를 가진 글꼴
     */
    public Font resolve(String text) {
        int style = (weight >= 600 ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);
        float px = (float) Math.max(0.01, size);
        // 패밀리를 차례로 보며 텍스트를 모두 표시할 수 있는 첫 글꼴을 고른다.
        for (int i = 0; i < families.size(); i++) {
            String name = javaName(families.get(i));
            if (name == null) {
                continue;
            }
            Font f = new Font(name, style, 1).deriveFont(px);
            if (text == null || text.length() == 0 || f.canDisplayUpTo(text) < 0) {
                return f;
            }
        }
        return new Font(Font.DIALOG, style, 1).deriveFont(px);
    }
}
