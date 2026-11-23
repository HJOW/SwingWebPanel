package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parses CSS colour values for SVG (plan item P18-04): the named colours, <code>#rgb</code>, <code>#rgba</code>, <code>#rrggbb</code>,
 * <code>#rrggbbaa</code>, <code>rgb()</code>/<code>rgba()</code> (comma or space separated, percentages, slash alpha),
 * <code>hsl()</code>/<code>hsla()</code>, <code>hwb()</code>, <code>transparent</code> and the system colour keywords mapped to fixed values.
 * <code>currentColor</code> is handled by the caller. The result is a non-premultiplied <code>0xAARRGGBB</code> integer.
 * <p>
 * SVG 를 위한 CSS 색상 값 해석(계획 P18-04): 이름 있는 색, <code>#rgb</code>, <code>#rgba</code>, <code>#rrggbb</code>, <code>#rrggbbaa</code>,
 * <code>rgb()</code>/<code>rgba()</code>(쉼표 또는 공백 구분, 백분율, 슬래시 알파), <code>hsl()</code>/<code>hsla()</code>, <code>hwb()</code>,
 * <code>transparent</code>, 고정 값으로 대응시킨 시스템 색 키워드. <code>currentColor</code> 는 호출자가 처리한다. 결과는 알파를 미리 곱하지 않은
 * <code>0xAARRGGBB</code> 정수이다.
 */
final class SvgColors {

    /**
     * The named colours (lower case name to opaque RGB).
     * <p>
     * 이름 있는 색(소문자 이름 → 불투명 RGB).
     */
    private static final Map<String, Integer> NAMED = new HashMap<String, Integer>();

    static {
        String[] t = { "aliceblue", "f0f8ff", "antiquewhite", "faebd7", "aqua", "00ffff", "aquamarine", "7fffd4", "azure", "f0ffff", "beige",
                "f5f5dc", "bisque", "ffe4c4", "black", "000000", "blanchedalmond", "ffebcd", "blue", "0000ff", "blueviolet", "8a2be2", "brown",
                "a52a2a", "burlywood", "deb887", "cadetblue", "5f9ea0", "chartreuse", "7fff00", "chocolate", "d2691e", "coral", "ff7f50",
                "cornflowerblue", "6495ed", "cornsilk", "fff8dc", "crimson", "dc143c", "cyan", "00ffff", "darkblue", "00008b", "darkcyan",
                "008b8b", "darkgoldenrod", "b8860b", "darkgray", "a9a9a9", "darkgreen", "006400", "darkgrey", "a9a9a9", "darkkhaki", "bdb76b",
                "darkmagenta", "8b008b", "darkolivegreen", "556b2f", "darkorange", "ff8c00", "darkorchid", "9932cc", "darkred", "8b0000",
                "darksalmon", "e9967a", "darkseagreen", "8fbc8f", "darkslateblue", "483d8b", "darkslategray", "2f4f4f", "darkslategrey",
                "2f4f4f", "darkturquoise", "00ced1", "darkviolet", "9400d3", "deeppink", "ff1493", "deepskyblue", "00bfff", "dimgray", "696969",
                "dimgrey", "696969", "dodgerblue", "1e90ff", "firebrick", "b22222", "floralwhite", "fffaf0", "forestgreen", "228b22", "fuchsia",
                "ff00ff", "gainsboro", "dcdcdc", "ghostwhite", "f8f8ff", "gold", "ffd700", "goldenrod", "daa520", "gray", "808080", "green",
                "008000", "greenyellow", "adff2f", "grey", "808080", "honeydew", "f0fff0", "hotpink", "ff69b4", "indianred", "cd5c5c", "indigo",
                "4b0082", "ivory", "fffff0", "khaki", "f0e68c", "lavender", "e6e6fa", "lavenderblush", "fff0f5", "lawngreen", "7cfc00",
                "lemonchiffon", "fffacd", "lightblue", "add8e6", "lightcoral", "f08080", "lightcyan", "e0ffff", "lightgoldenrodyellow",
                "fafad2", "lightgray", "d3d3d3", "lightgreen", "90ee90", "lightgrey", "d3d3d3", "lightpink", "ffb6c1", "lightsalmon", "ffa07a",
                "lightseagreen", "20b2aa", "lightskyblue", "87cefa", "lightslategray", "778899", "lightslategrey", "778899", "lightsteelblue",
                "b0c4de", "lightyellow", "ffffe0", "lime", "00ff00", "limegreen", "32cd32", "linen", "faf0e6", "magenta", "ff00ff", "maroon",
                "800000", "mediumaquamarine", "66cdaa", "mediumblue", "0000cd", "mediumorchid", "ba55d3", "mediumpurple", "9370db",
                "mediumseagreen", "3cb371", "mediumslateblue", "7b68ee", "mediumspringgreen", "00fa9a", "mediumturquoise", "48d1cc",
                "mediumvioletred", "c71585", "midnightblue", "191970", "mintcream", "f5fffa", "mistyrose", "ffe4e1", "moccasin", "ffe4b5",
                "navajowhite", "ffdead", "navy", "000080", "oldlace", "fdf5e6", "olive", "808000", "olivedrab", "6b8e23", "orange", "ffa500",
                "orangered", "ff4500", "orchid", "da70d6", "palegoldenrod", "eee8aa", "palegreen", "98fb98", "paleturquoise", "afeeee",
                "palevioletred", "db7093", "papayawhip", "ffefd5", "peachpuff", "ffdab9", "peru", "cd853f", "pink", "ffc0cb", "plum", "dda0dd",
                "powderblue", "b0e0e6", "purple", "800080", "rebeccapurple", "663399", "red", "ff0000", "rosybrown", "bc8f8f", "royalblue",
                "4169e1", "saddlebrown", "8b4513", "salmon", "fa8072", "sandybrown", "f4a460", "seagreen", "2e8b57", "seashell", "fff5ee",
                "sienna", "a0522d", "silver", "c0c0c0", "skyblue", "87ceeb", "slateblue", "6a5acd", "slategray", "708090", "slategrey",
                "708090", "snow", "fffafa", "springgreen", "00ff7f", "steelblue", "4682b4", "tan", "d2b48c", "teal", "008080", "thistle",
                "d8bfd8", "tomato", "ff6347", "turquoise", "40e0d0", "violet", "ee82ee", "wheat", "f5deb3", "white", "ffffff", "whitesmoke",
                "f5f5f5", "yellow", "ffff00", "yellowgreen", "9acd32" };
        // 이름과 16진수 값이 번갈아 든 표를 사전으로 옮긴다.
        for (int i = 0; i < t.length; i += 2) {
            NAMED.put(t[i], Integer.valueOf(0xFF000000 | Integer.parseInt(t[i + 1], 16)));
        }
        // 시스템 색 키워드는 밝은 테마의 고정 값으로 대응시킨다.
        String[] sys = { "canvas", "ffffff", "canvastext", "000000", "linktext", "0000ee", "visitedtext", "551a8b", "activetext", "ff0000",
                "buttonface", "efefef", "buttontext", "000000", "buttonborder", "767676", "field", "ffffff", "fieldtext", "000000", "highlight",
                "3390ff", "highlighttext", "ffffff", "graytext", "808080", "mark", "ffff00", "marktext", "000000", "accentcolor", "3390ff",
                "accentcolortext", "ffffff", "window", "ffffff", "windowtext", "000000", "windowframe", "000000", "menu", "ffffff", "menutext",
                "000000", "background", "ffffff", "buttonhighlight", "ffffff", "buttonshadow", "767676", "threeddarkshadow", "000000",
                "threedface", "efefef", "threedhighlight", "ffffff", "threedlightshadow", "efefef", "threedshadow", "767676", "infobackground",
                "ffffe1", "infotext", "000000", "inactiveborder", "ffffff", "inactivecaption", "ffffff", "inactivecaptiontext", "000000",
                "scrollbar", "ffffff", "appworkspace", "ababab", "activeborder", "ffffff", "activecaption", "ccccff", "captiontext", "000000" };
        // 같은 방식으로 시스템 색을 등록한다.
        for (int i = 0; i < sys.length; i += 2) {
            NAMED.put(sys[i], Integer.valueOf(0xFF000000 | Integer.parseInt(sys[i + 1], 16)));
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgColors() {
    }

    /**
     * Parses a colour value. <code>currentColor</code> is not accepted here.
     * <p>
     * 색상 값을 해석한다. <code>currentColor</code> 는 여기서 받지 않는다.
     *
     * @param value the CSS colour text / CSS 색상 텍스트
     * @return the colour as <code>0xAARRGGBB</code>, or null if the text is not a colour / <code>0xAARRGGBB</code> 색. 색이 아니면 null
     */
    static Integer parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase(Locale.ENGLISH);
        if (v.length() == 0) {
            return null;
        }
        if (v.charAt(0) == '#') {
            return parseHex(v);
        }
        if ("transparent".equals(v)) {
            return Integer.valueOf(0);
        }
        int paren = v.indexOf('(');
        if (paren > 0 && v.endsWith(")")) {
            return parseFunction(v.substring(0, paren).trim(), v.substring(paren + 1, v.length() - 1));
        }
        return NAMED.get(v);
    }

    /**
     * Parses <code>#rgb</code>, <code>#rgba</code>, <code>#rrggbb</code> or <code>#rrggbbaa</code>.
     * <p>
     * <code>#rgb</code>, <code>#rgba</code>, <code>#rrggbb</code>, <code>#rrggbbaa</code> 를 해석한다.
     *
     * @param v lower-case text starting with # / #으로 시작하는 소문자 텍스트
     * @return colour or null / 색 또는 null
     */
    private static Integer parseHex(String v) {
        int len = v.length() - 1;
        if (len != 3 && len != 4 && len != 6 && len != 8) {
            return null;
        }
        long x = 0;
        // 16진수 숫자를 모두 읽는다.
        for (int i = 1; i < v.length(); i++) {
            int d = Character.digit(v.charAt(i), 16);
            if (d < 0) {
                return null;
            }
            x = (x << 4) | d;
        }
        int a = 255;
        int r;
        int g;
        int b;
        if (len == 3 || len == 4) {
            r = (int) ((x >> (len == 3 ? 8 : 12)) & 0xF) * 17;
            g = (int) ((x >> (len == 3 ? 4 : 8)) & 0xF) * 17;
            b = (int) ((x >> (len == 3 ? 0 : 4)) & 0xF) * 17;
            if (len == 4) {
                a = (int) (x & 0xF) * 17;
            }
        } else {
            int sh = len == 6 ? 0 : 8;
            r = (int) ((x >> (16 + sh)) & 0xFF);
            g = (int) ((x >> (8 + sh)) & 0xFF);
            b = (int) ((x >> sh) & 0xFF);
            if (len == 8) {
                a = (int) (x & 0xFF);
            }
        }
        return Integer.valueOf((a << 24) | (r << 16) | (g << 8) | b);
    }

    /**
     * Parses the arguments of a colour function.
     * <p>
     * 색상 함수의 인수를 해석한다.
     *
     * @param fn   function name in lower case / 소문자 함수 이름
     * @param args the text between the parentheses / 괄호 사이의 텍스트
     * @return colour or null / 색 또는 null
     */
    private static Integer parseFunction(String fn, String args) {
        boolean rgb = "rgb".equals(fn) || "rgba".equals(fn);
        boolean hsl = "hsl".equals(fn) || "hsla".equals(fn);
        boolean hwb = "hwb".equals(fn);
        if (!rgb && !hsl && !hwb) {
            return null;
        }
        // 쉼표, 공백, 슬래시를 모두 구분자로 보고 값을 나눈다.
        String[] parts = args.trim().split("[\\s,/]+");
        if (parts.length != 3 && parts.length != 4) {
            return null;
        }
        double[] v = new double[4];
        boolean[] pct = new boolean[4];
        v[3] = 1;
        // 각 값을 숫자(퍼센트 여부 포함)로 읽는다.
        for (int i = 0; i < parts.length; i++) {
            String t = parts[i];
            if ("none".equals(t)) {
                v[i] = 0;
                continue;
            }
            if (t.endsWith("%")) {
                pct[i] = true;
                t = t.substring(0, t.length() - 1);
            } else if (i == 0 && (hsl || hwb)) {
                t = stripAngleUnit(t);
                if (t == null) {
                    return null;
                }
            }
            try {
                v[i] = Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return null;
            }
            if (Double.isNaN(v[i]) || Double.isInfinite(v[i])) {
                return null;
            }
        }
        double alpha = parts.length == 4 ? clamp01(pct[3] ? v[3] / 100 : v[3]) : 1;
        int r;
        int g;
        int b;
        if (rgb) {
            r = channel(v[0], pct[0]);
            g = channel(v[1], pct[1]);
            b = channel(v[2], pct[2]);
        } else {
            double h = ((v[0] % 360) + 360) % 360;
            double s = clamp01(v[1] / 100);
            double l = clamp01(v[2] / 100);
            double[] c = new double[3];
            if (hsl) {
                hslToRgb(h, s, l, c);
            } else {
                double w = s;
                double bl = l;
                if (w + bl >= 1) {
                    double gr = w / (w + bl);
                    c[0] = gr;
                    c[1] = gr;
                    c[2] = gr;
                } else {
                    hslToRgb(h, 1, 0.5, c);
                    // 순색에 흰색과 검은색 비율을 섞는다.
                    for (int i = 0; i < 3; i++) {
                        c[i] = c[i] * (1 - w - bl) + w;
                    }
                }
            }
            r = (int) Math.round(clamp01(c[0]) * 255);
            g = (int) Math.round(clamp01(c[1]) * 255);
            b = (int) Math.round(clamp01(c[2]) * 255);
        }
        int a = (int) Math.round(alpha * 255);
        return Integer.valueOf((a << 24) | (r << 16) | (g << 8) | b);
    }

    /**
     * Removes an angle unit from a hue text and converts it to degrees.
     * <p>
     * 색상환 각도 텍스트에서 단위를 제거하고 도(degree)로 바꾼다.
     *
     * @param t the text such as <code>120</code>, <code>0.5turn</code> / <code>120</code>, <code>0.5turn</code> 같은 텍스트
     * @return the number of degrees as text, or null if the unit is unknown / 도 단위 숫자 텍스트. 알 수 없는 단위면 null
     */
    private static String stripAngleUnit(String t) {
        double mul = 1;
        String num = t;
        if (t.endsWith("deg")) {
            num = t.substring(0, t.length() - 3);
        } else if (t.endsWith("grad")) {
            num = t.substring(0, t.length() - 4);
            mul = 0.9;
        } else if (t.endsWith("rad")) {
            num = t.substring(0, t.length() - 3);
            mul = 180 / Math.PI;
        } else if (t.endsWith("turn")) {
            num = t.substring(0, t.length() - 4);
            mul = 360;
        }
        try {
            return Double.toString(Double.parseDouble(num) * mul);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts an RGB channel number to 0..255.
     * <p>
     * RGB 채널 값을 0..255 로 바꾼다.
     *
     * @param v   the number / 값
     * @param pct true if the number is a percentage / 값이 백분율이면 true
     * @return channel value / 채널 값
     */
    private static int channel(double v, boolean pct) {
        double x = pct ? v * 255 / 100 : v;
        if (x < 0) {
            x = 0;
        }
        if (x > 255) {
            x = 255;
        }
        return (int) Math.round(x);
    }

    /**
     * Clamps a number to 0..1.
     * <p>
     * 수를 0..1 로 제한한다.
     *
     * @param x the number / 수
     * @return clamped / 제한된 값
     */
    private static double clamp01(double x) {
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }

    /**
     * Converts HSL to RGB (all results in 0..1).
     * <p>
     * HSL 을 RGB(모두 0..1)로 바꾼다.
     *
     * @param h   hue in degrees / 색상(도)
     * @param s   saturation 0..1 / 채도 0..1
     * @param l   lightness 0..1 / 명도 0..1
     * @param out receives red, green, blue / 빨강, 초록, 파랑을 받는다
     */
    private static void hslToRgb(double h, double s, double l, double[] out) {
        double a = s * Math.min(l, 1 - l);
        int[] offsets = { 0, 8, 4 };
        // 세 채널을 CSS 규격의 공식으로 계산한다.
        for (int i = 0; i < 3; i++) {
            double k = (offsets[i] + h / 30) % 12;
            out[i] = l - a * Math.max(-1, Math.min(Math.min(k - 3, 9 - k), 1));
        }
    }
}
