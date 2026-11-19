package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The computed style of one element while the document is rendered (plan item P18-04): the declared properties of the element from the
 * cascade ({@link SvgElement#declared}) combined with the inherited properties of the parent style. Values stay as text and are
 * parsed by typed accessors; a value that does not parse is ignored as if it was not declared (so an inherited property keeps the inherited
 * value). The keywords <code>inherit</code>, <code>initial</code> and <code>unset</code> and <code>var()</code> references to custom
 * properties are resolved here. The font size is computed eagerly because <code>em</code> and percentages of children depend on it. The
 * style of an element reached through <code>use</code> inherits from the <code>use</code> element, not from its original parent.
 * <p>
 * 문서를 그리는 동안 요소 하나의 계산된 스타일(계획 P18-04): 연쇄가 만든 요소의 선언된 속성({@link SvgElement#declared})과 부모 스타일에서 상속되는
 * 속성의 결합. 값은 텍스트로 두고 형 있는 접근자가 해석한다. 해석되지 않는 값은 선언되지 않은 것처럼 무시한다(상속 속성은 상속 값을 유지).
 * 키워드 <code>inherit</code>, <code>initial</code>, <code>unset</code> 과 사용자 정의 속성의 <code>var()</code> 참조를 여기서 해결한다.
 * 자식의 <code>em</code> 과 백분율이 글꼴 크기에 의존하므로 글꼴 크기는 미리 계산한다. <code>use</code> 로 도달한 요소의 스타일은 원래 부모가 아니라
 * <code>use</code> 요소에서 상속한다.
 */
final class SvgStyle {

    /**
     * The properties that inherit by default.
     * <p>
     * 기본적으로 상속되는 속성.
     */
    private static final Set<String> INHERITED = new HashSet<String>(Arrays.asList("clip-rule", "color", "color-interpolation",
            "color-interpolation-filters", "direction", "dominant-baseline", "fill", "fill-opacity", "fill-rule", "font-family",
            "font-size", "font-size-adjust", "font-stretch", "font-style", "font-variant", "font-weight", "image-rendering",
            "letter-spacing", "marker-start", "marker-mid", "marker-end", "paint-order", "pointer-events", "shape-rendering", "stroke",
            "stroke-dasharray", "stroke-dashoffset", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "stroke-opacity",
            "stroke-width", "text-anchor", "text-rendering", "visibility", "white-space", "word-spacing", "writing-mode"));

    /**
     * The parent style or null for the base style.
     * <p>
     * 부모 스타일. 기본 스타일이면 null.
     */
    final SvgStyle parent;

    /**
     * The element that this style belongs to, or null for the base style.
     * <p>
     * 이 스타일이 속한 요소. 기본 스타일이면 null.
     */
    final SvgElement element;

    /**
     * The declared properties of the element after custom property substitution.
     * <p>
     * 사용자 정의 속성을 치환한 뒤의 요소의 선언된 속성.
     */
    private final Map<String, String> own;

    /**
     * The computed font size in pixels.
     * <p>
     * 계산된 글꼴 크기(픽셀).
     */
    final double fontSize;

    /**
     * The width of the nearest viewport (the base of percentages along x).
     * <p>
     * 가장 가까운 뷰포트의 너비(x 방향 백분율의 기준).
     */
    final double vpW;

    /**
     * The height of the nearest viewport (the base of percentages along y).
     * <p>
     * 가장 가까운 뷰포트의 높이(y 방향 백분율의 기준).
     */
    final double vpH;

    /**
     * Creates the base style: everything has its initial value, the font size is 16 pixels.
     * <p>
     * 기본 스타일을 만든다. 모든 속성이 초기값이며 글꼴 크기는 16 픽셀이다.
     *
     * @param vpW viewport width / 뷰포트 너비
     * @param vpH viewport height / 뷰포트 높이
     */
    SvgStyle(double vpW, double vpH) {
        this.parent = null;
        this.element = null;
        this.own = new HashMap<String, String>();
        this.fontSize = 16;
        this.vpW = vpW;
        this.vpH = vpH;
    }

    /**
     * Creates the style of an element.
     * <p>
     * 요소의 스타일을 만든다.
     *
     * @param parent the style that the element inherits from / 요소가 상속받는 스타일
     * @param e      the element / 요소
     * @param vpW    the viewport width in effect / 적용되는 뷰포트 너비
     * @param vpH    the viewport height in effect / 적용되는 뷰포트 높이
     */
    SvgStyle(SvgStyle parent, SvgElement e, double vpW, double vpH) {
        this.parent = parent;
        this.element = e;
        this.vpW = vpW;
        this.vpH = vpH;
        Map<String, String> decl = e.declared;
        if (decl == null || decl.isEmpty()) {
            this.own = new HashMap<String, String>();
        } else {
            this.own = new HashMap<String, String>(decl);
            substituteVars();
        }
        this.fontSize = computeFontSize();
    }

    /**
     * Replaces <code>var()</code> references in the declared values; a value whose reference cannot be resolved is dropped.
     * <p>
     * 선언된 값의 <code>var()</code> 참조를 치환한다. 참조를 해결하지 못한 값은 버린다.
     */
    private void substituteVars() {
        boolean any = false;
        // var( 가 든 값이 있는지 먼저 확인한다.
        for (String v : own.values()) {
            if (v.indexOf("var(") >= 0) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        List<String> names = new ArrayList<String>(own.keySet());
        // 사용자 정의 속성부터 치환해 다른 속성이 최종 값을 읽게 한다.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                boolean custom = name.startsWith("--");
                if (custom != (pass == 0)) {
                    continue;
                }
                String v = own.get(name);
                if (v == null || v.indexOf("var(") < 0) {
                    continue;
                }
                String r = substitute(v, 0, name);
                if (r == null) {
                    own.remove(name);
                } else {
                    own.put(name, r);
                }
            }
        }
    }

    /**
     * Substitutes <code>var(--name, fallback)</code> references in a value.
     * <p>
     * 값 안의 <code>var(--name, fallback)</code> 참조를 치환한다.
     *
     * @param v     the value / 값
     * @param depth the recursion depth / 재귀 깊이
     * @param self  the name of the property being substituted (to detect cycles) / 치환 중인 속성 이름(순환 감지용)
     * @return the value without references, or null if one cannot be resolved / 참조가 없는 값. 해결하지 못하면 null
     */
    private String substitute(String v, int depth, String self) {
        if (depth > 8) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        // var( 를 만날 때마다 이름과 대체 값을 읽어 치환한다.
        while (i < v.length()) {
            int at = v.indexOf("var(", i);
            if (at < 0) {
                sb.append(v, i, v.length());
                break;
            }
            sb.append(v, i, at);
            int depthParen = 1;
            int j = at + 4;
            int comma = -1;
            // 짝이 맞는 닫는 괄호와 첫 쉼표를 찾는다.
            while (j < v.length() && depthParen > 0) {
                char c = v.charAt(j);
                if (c == '(') {
                    depthParen++;
                } else if (c == ')') {
                    depthParen--;
                    if (depthParen == 0) {
                        break;
                    }
                } else if (c == ',' && depthParen == 1 && comma < 0) {
                    comma = j;
                }
                j++;
            }
            if (j >= v.length()) {
                return null;
            }
            String name = v.substring(at + 4, comma < 0 ? j : comma).trim();
            String fallback = comma < 0 ? null : v.substring(comma + 1, j).trim();
            String value = name.equals(self) ? null : customValue(name);
            if (value != null && value.indexOf("var(") >= 0) {
                value = substitute(value, depth + 1, self);
            }
            if (value == null && fallback != null) {
                value = substitute(fallback, depth + 1, self);
            }
            if (value == null) {
                return null;
            }
            sb.append(value);
            i = j + 1;
        }
        return sb.toString();
    }

    /**
     * Looks up a custom property in this style or its ancestors.
     * <p>
     * 이 스타일과 조상에서 사용자 정의 속성을 찾는다.
     *
     * @param name the property name starting with two dashes / 대시 두 개로 시작하는 속성 이름
     * @return the value or null / 값 또는 null
     */
    private String customValue(String name) {
        // 자기 자신부터 조상 쪽으로 올라가며 찾는다.
        for (SvgStyle s = this; s != null; s = s.parent) {
            String v = s.own.get(name);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * Tells whether a property inherits by default.
     * <p>
     * 속성이 기본적으로 상속되는지 알려 준다.
     *
     * @param prop the property name / 속성 이름
     * @return true if inherited / 상속되면 true
     */
    static boolean isInherited(String prop) {
        return INHERITED.contains(prop) || prop.startsWith("--");
    }

    /**
     * Returns the effective value text of a property: the declared value of this element, or the inherited value for inherited properties,
     * or null for the initial value. A value that the validator rejects is skipped.
     * <p>
     * 속성의 유효한 값 텍스트를 반환한다: 이 요소의 선언된 값, 상속 속성이면 상속된 값, 초기값이면 null. 검사기가 거부한 값은 건너뛴다.
     *
     * @param prop the property name / 속성 이름
     * @return the text or null / 텍스트 또는 null
     */
    String get(String prop) {
        String v = own.get(prop);
        boolean inh = isInherited(prop);
        if (v != null) {
            String l = v.trim();
            if ("inherit".equalsIgnoreCase(l)) {
                return parent == null ? null : parent.get(prop);
            }
            if ("initial".equalsIgnoreCase(l)) {
                return null;
            }
            if ("unset".equalsIgnoreCase(l) || "revert".equalsIgnoreCase(l)) {
                return inh && parent != null ? parent.get(prop) : null;
            }
            return l;
        }
        if (inh && parent != null) {
            return parent.get(prop);
        }
        return null;
    }

    /**
     * Returns a value that parses, trying this element and then (for inherited properties) the ancestors.
     * <p>
     * 해석되는 값을 반환한다. 이 요소를 먼저, 상속 속성이면 조상 순으로 시도한다.
     *
     * @param prop the property name / 속성 이름
     * @param kind the parser id: 0 number, 1 opacity, 2 length, 3 paint, 4 color / 해석기 번호: 0 수, 1 투명도, 2 길이, 3 페인트, 4 색
     * @return the parsed object or null / 해석된 객체 또는 null
     */
    private Object parsed(String prop, int kind) {
        boolean inh = isInherited(prop);
        // 이 요소에서 시작해 해석되는 값이 나올 때까지 조상으로 올라간다.
        for (SvgStyle s = this; s != null; s = s.parent) {
            String v = s.own.get(prop);
            if (v == null) {
                if (inh) {
                    continue;
                }
                return null;
            }
            String l = v.trim();
            if ("inherit".equalsIgnoreCase(l)) {
                continue;
            }
            if ("initial".equalsIgnoreCase(l)) {
                return null;
            }
            if ("unset".equalsIgnoreCase(l) || "revert".equalsIgnoreCase(l)) {
                if (inh) {
                    continue;
                }
                return null;
            }
            Object o = parseKind(l, kind, s);
            if (o != null) {
                return o;
            }
            // 해석되지 않는 값은 선언되지 않은 것으로 본다(상속 속성은 조상 값을 쓴다).
            if (!inh) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parses a value text with one of the parsers of {@link #parsed}.
     * <p>
     * {@link #parsed} 의 해석기 중 하나로 값 텍스트를 해석한다.
     *
     * @param l    the trimmed text / 다듬은 텍스트
     * @param kind the parser id / 해석기 번호
     * @param s    the style that declared the value / 값을 선언한 스타일
     * @return the parsed object or null / 해석된 객체 또는 null
     */
    private Object parseKind(String l, int kind, SvgStyle s) {
        if (kind == 0) {
            SvgScanner sc = new SvgScanner(l);
            if (sc.number()) {
                sc.skipWs();
                if (sc.atEnd()) {
                    return Double.valueOf(sc.value);
                }
            }
            return null;
        }
        if (kind == 1) {
            SvgLength len = SvgLength.parse(l);
            if (len == null || (len.unit.length() > 0 && !len.isPercent())) {
                return null;
            }
            double x = len.isPercent() ? len.value / 100 : len.value;
            return Double.valueOf(x < 0 ? 0 : (x > 1 ? 1 : x));
        }
        if (kind == 2) {
            return SvgLength.parse(l);
        }
        if (kind == 3) {
            return SvgPaint.parse(l);
        }
        // 색: currentColor 는 요소의 부모 색을 가리킨다.
        if ("currentcolor".equalsIgnoreCase(l)) {
            return Integer.valueOf(s.parent == null ? 0xFF000000 : s.parent.color());
        }
        return SvgColors.parse(l);
    }

    /**
     * Returns the computed text colour (the <code>color</code> property) as <code>0xAARRGGBB</code>; black by default.
     * <p>
     * 계산된 글자색(<code>color</code> 속성)을 <code>0xAARRGGBB</code> 로 반환한다. 기본은 검정.
     *
     * @return the colour / 색
     */
    int color() {
        Object o = parsed("color", 4);
        return o == null ? 0xFF000000 : ((Integer) o).intValue();
    }

    /**
     * Returns a paint property (<code>fill</code> or <code>stroke</code>); the initial value of <code>fill</code> is black and of
     * <code>stroke</code> none.
     * <p>
     * 페인트 속성(<code>fill</code>, <code>stroke</code>)을 반환한다. <code>fill</code> 의 초기값은 검정, <code>stroke</code> 는 none 이다.
     *
     * @param prop <code>fill</code> or <code>stroke</code> / <code>fill</code> 또는 <code>stroke</code>
     * @return the paint (<code>currentColor</code> already replaced) / 페인트(<code>currentColor</code> 는 이미 치환됨)
     */
    SvgPaint paint(String prop) {
        Object o = parsed(prop, 3);
        SvgPaint p;
        if (o == null) {
            p = "fill".equals(prop) ? SvgPaint.color(0xFF000000) : SvgPaint.NONE_PAINT;
        } else {
            p = (SvgPaint) o;
        }
        if (p.kind == SvgPaint.CURRENT) {
            return SvgPaint.color(color());
        }
        if (p.kind == SvgPaint.URL && p.fallback != null && p.fallback.kind == SvgPaint.CURRENT) {
            return new SvgPaint(SvgPaint.URL, 0, p.ref, SvgPaint.color(color()));
        }
        return p;
    }

    /**
     * Returns a number property (no units allowed), or the default.
     * <p>
     * 수 속성(단위 불가)을 반환하며 없으면 기본값을 반환한다.
     *
     * @param prop the property name / 속성 이름
     * @param def  the default / 기본값
     * @return the number / 수
     */
    double number(String prop, double def) {
        Object o = parsed(prop, 0);
        return o == null ? def : ((Double) o).doubleValue();
    }

    /**
     * Returns an opacity-like property clamped to 0..1 (a number or a percentage), or the default.
     * <p>
     * 0..1 로 제한한 투명도 속성(수 또는 백분율)을 반환하며 없으면 기본값을 반환한다.
     *
     * @param prop the property name / 속성 이름
     * @param def  the default / 기본값
     * @return the opacity / 투명도
     */
    double opacity(String prop, double def) {
        Object o = parsed(prop, 1);
        return o == null ? def : ((Double) o).doubleValue();
    }

    /**
     * Returns a length property or null.
     * <p>
     * 길이 속성을 반환하며 없으면 null 이다.
     *
     * @param prop the property name / 속성 이름
     * @return the length or null / 길이 또는 null
     */
    SvgLength length(String prop) {
        return (SvgLength) parsed(prop, 2);
    }

    /**
     * Converts a length to pixels in the context of this style (font size and viewport).
     * <p>
     * 길이를 이 스타일의 문맥(글꼴 크기와 뷰포트)에서 픽셀로 바꾼다.
     *
     * @param l    the length / 길이
     * @param axis 0 for x (viewport width), 1 for y (viewport height), 2 for the normalized diagonal / 0 이면 x(뷰포트 너비), 1 이면 y(뷰포트 높이), 2 이면 정규화된 대각선
     * @return pixels / 픽셀
     */
    double px(SvgLength l, int axis) {
        double base = axis == 0 ? vpW : (axis == 1 ? vpH : Math.sqrt((vpW * vpW + vpH * vpH) / 2));
        return l.toPx(fontSize, base, vpW, vpH);
    }

    /**
     * Returns a keyword property in lower case, or null if not set.
     * <p>
     * 키워드 속성을 소문자로 반환하며 설정되지 않았으면 null 이다.
     *
     * @param prop the property name / 속성 이름
     * @return the keyword or null / 키워드 또는 null
     */
    String keyword(String prop) {
        String v = get(prop);
        return v == null ? null : v.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Computes the font size from the <code>font-size</code> property.
     * <p>
     * <code>font-size</code> 속성에서 글꼴 크기를 계산한다.
     *
     * @return the size in pixels / 크기(픽셀)
     */
    private double computeFontSize() {
        double base = parent == null ? 16 : parent.fontSize;
        String v = own.get("font-size");
        if (v == null) {
            return base;
        }
        String l = v.trim().toLowerCase(Locale.ENGLISH);
        if ("inherit".equals(l) || "unset".equals(l) || "revert".equals(l)) {
            return base;
        }
        if ("initial".equals(l) || "medium".equals(l)) {
            return 16;
        }
        double kw = keywordSize(l);
        if (kw > 0) {
            return kw;
        }
        if ("larger".equals(l)) {
            return base * 1.2;
        }
        if ("smaller".equals(l)) {
            return base / 1.2;
        }
        SvgLength len = SvgLength.parse(l);
        if (len == null) {
            return base;
        }
        double px = len.toPx(base, base, vpW, vpH);
        return px < 0 ? base : px;
    }

    /**
     * Returns the pixel size of an absolute font size keyword, or 0.
     * <p>
     * 절대 글꼴 크기 키워드의 픽셀 크기를 반환하며 아니면 0 이다.
     *
     * @param k the keyword / 키워드
     * @return pixels or 0 / 픽셀 또는 0
     */
    private static double keywordSize(String k) {
        if ("xx-small".equals(k)) {
            return 9;
        }
        if ("x-small".equals(k)) {
            return 10;
        }
        if ("small".equals(k)) {
            return 13;
        }
        if ("large".equals(k)) {
            return 18;
        }
        if ("x-large".equals(k)) {
            return 24;
        }
        if ("xx-large".equals(k)) {
            return 32;
        }
        if ("xxx-large".equals(k)) {
            return 48;
        }
        return 0;
    }

    /**
     * Returns the computed font weight (100 to 900).
     * <p>
     * 계산된 글꼴 굵기(100~900)를 반환한다.
     *
     * @return the weight / 굵기
     */
    int fontWeight() {
        String v = keyword("font-weight");
        if (v == null || "normal".equals(v)) {
            return 400;
        }
        if ("bold".equals(v)) {
            return 700;
        }
        if ("bolder".equals(v)) {
            int base = parent == null ? 400 : parent.fontWeight();
            return base < 400 ? 400 : (base < 600 ? 700 : 900);
        }
        if ("lighter".equals(v)) {
            int base = parent == null ? 400 : parent.fontWeight();
            return base > 700 ? 700 : (base > 500 ? 400 : 100);
        }
        try {
            int w = (int) Math.round(Double.parseDouble(v));
            return w < 1 ? 400 : (w > 1000 ? 1000 : w);
        } catch (NumberFormatException e) {
            return 400;
        }
    }

    /**
     * Tells whether the font style is italic or oblique.
     * <p>
     * 글꼴 스타일이 이탤릭이나 오블리크인지 알려 준다.
     *
     * @return true for italic/oblique / 이탤릭/오블리크이면 true
     */
    boolean italic() {
        String v = keyword("font-style");
        return v != null && (v.startsWith("italic") || v.startsWith("oblique"));
    }

    /**
     * Returns the dash array of the stroke in pixels, or null for a solid line (also for invalid arrays).
     * <p>
     * 선의 점선 배열(픽셀)을 반환한다. 실선이거나 올바르지 않은 배열이면 null.
     *
     * @return the dash array with an even length, or null / 짝수 길이의 점선 배열 또는 null
     */
    float[] dashArray() {
        String v = get("stroke-dasharray");
        if (v == null || "none".equalsIgnoreCase(v)) {
            return null;
        }
        SvgLength[] ls = SvgLength.parseList(v);
        if (ls == null || ls.length == 0) {
            return null;
        }
        double sum = 0;
        double[] px = new double[ls.length];
        // 모든 항목을 픽셀로 바꾸며 음수가 있으면 배열 전체를 무시한다.
        for (int i = 0; i < ls.length; i++) {
            px[i] = px(ls[i], 2);
            if (px[i] < 0 || Double.isNaN(px[i])) {
                return null;
            }
            sum += px[i];
        }
        if (sum <= 0) {
            return null;
        }
        int n = ls.length % 2 == 0 ? ls.length : ls.length * 2;
        float[] r = new float[n];
        // 홀수 개이면 배열을 두 번 이어 짝수로 만든다.
        for (int i = 0; i < n; i++) {
            r[i] = (float) px[i % ls.length];
        }
        return r;
    }

    /**
     * Extracts the id of a <code>url(#id)</code> value.
     * <p>
     * <code>url(#id)</code> 값에서 id 를 꺼낸다.
     *
     * @param t the text starting with <code>url(</code> / <code>url(</code> 로 시작하는 텍스트
     * @return the id, or null if the url is not a fragment reference / id. 조각 참조가 아니면 null
     */
    static String urlId(String t) {
        String s = t.trim();
        if (!s.toLowerCase(Locale.ENGLISH).startsWith("url(") || !s.endsWith(")")) {
            return null;
        }
        String inner = s.substring(4, s.length() - 1).trim();
        // 따옴표로 감싼 URL 이면 따옴표를 벗긴다.
        if (inner.length() >= 2 && (inner.charAt(0) == '"' || inner.charAt(0) == '\'') && inner.charAt(inner.length() - 1) == inner.charAt(0)) {
            inner = inner.substring(1, inner.length() - 1).trim();
        }
        if (inner.length() < 2 || inner.charAt(0) != '#') {
            return null;
        }
        return inner.substring(1);
    }

    /**
     * Returns the id referenced by a <code>url(#id)</code> property such as <code>clip-path</code> or <code>mask</code>.
     * <p>
     * <code>clip-path</code>, <code>mask</code> 같은 <code>url(#id)</code> 속성이 가리키는 id 를 반환한다.
     *
     * @param prop the property name / 속성 이름
     * @return the id or null / id 또는 null
     */
    String urlProperty(String prop) {
        String v = own.get(prop);
        if (v == null) {
            return null;
        }
        String l = v.trim();
        if (l.toLowerCase(Locale.ENGLISH).startsWith("url(")) {
            return urlId(l.substring(0, Math.max(4, SvgPaint.findUrlEnd(l) + 1)));
        }
        return null;
    }

    /**
     * Returns the declared text of a property of this element only (no inheritance), or null.
     * <p>
     * 이 요소 자신이 선언한 속성 텍스트를 반환한다(상속 없음). 없으면 null.
     *
     * @param prop the property name / 속성 이름
     * @return the text or null / 텍스트 또는 null
     */
    String own(String prop) {
        String v = own.get(prop);
        return v == null ? null : v.trim();
    }

    /**
     * Tells whether the element is displayed (the <code>display</code> property is not <code>none</code>).
     * <p>
     * 요소가 표시되는지(<code>display</code> 속성이 <code>none</code> 이 아닌지) 알려 준다.
     *
     * @return true if displayed / 표시되면 true
     */
    boolean displayed() {
        String v = own("display");
        return v == null || !"none".equalsIgnoreCase(v);
    }

    /**
     * Tells whether the element is visible (the <code>visibility</code> property is <code>visible</code>).
     * <p>
     * 요소가 보이는지(<code>visibility</code> 속성이 <code>visible</code> 인지) 알려 준다.
     *
     * @return true if visible / 보이면 true
     */
    boolean visible() {
        String v = keyword("visibility");
        return v == null || "visible".equals(v);
    }
}
