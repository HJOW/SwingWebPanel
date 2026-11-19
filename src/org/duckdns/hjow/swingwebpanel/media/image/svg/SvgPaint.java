package org.duckdns.hjow.swingwebpanel.media.image.svg;

/**
 * A value of the <code>fill</code> or <code>stroke</code> property (plan item P18-04): none, a colour, a reference to a paint server
 * (<code>url(#id)</code>, optionally with a fallback colour or <code>none</code>), <code>currentColor</code>, or the context paint keywords
 * of markers.
 * <p>
 * <code>fill</code>, <code>stroke</code> 속성의 값(계획 P18-04): 없음, 색, 페인트 서버 참조(<code>url(#id)</code>, 대체 색이나 <code>none</code> 선택),
 * <code>currentColor</code>, 마커의 컨텍스트 페인트 키워드.
 */
final class SvgPaint {

    /**
     * Kind: no paint.
     * <p>
     * 종류: 칠하지 않음.
     */
    static final int NONE = 0;

    /**
     * Kind: a plain colour (with alpha).
     * <p>
     * 종류: 단색(알파 포함).
     */
    static final int COLOR = 1;

    /**
     * Kind: a paint server reference.
     * <p>
     * 종류: 페인트 서버 참조.
     */
    static final int URL = 2;

    /**
     * Kind: the current colour of the element.
     * <p>
     * 종류: 요소의 현재 색.
     */
    static final int CURRENT = 3;

    /**
     * Kind: the fill of the element that the marker is applied to.
     * <p>
     * 종류: 마커가 적용된 요소의 채우기.
     */
    static final int CONTEXT_FILL = 4;

    /**
     * Kind: the stroke of the element that the marker is applied to.
     * <p>
     * 종류: 마커가 적용된 요소의 선.
     */
    static final int CONTEXT_STROKE = 5;

    /**
     * The shared "none" paint.
     * <p>
     * 공유하는 "none" 페인트.
     */
    static final SvgPaint NONE_PAINT = new SvgPaint(NONE, 0, null, null);

    /**
     * The kind of the paint.
     * <p>
     * 페인트의 종류.
     */
    final int kind;

    /**
     * The colour as <code>0xAARRGGBB</code> for {@link #COLOR}.
     * <p>
     * {@link #COLOR} 일 때의 색(<code>0xAARRGGBB</code>).
     */
    final int argb;

    /**
     * The id of the referenced paint server for {@link #URL}.
     * <p>
     * {@link #URL} 일 때 참조하는 페인트 서버의 id.
     */
    final String ref;

    /**
     * The fallback used if the reference cannot be resolved, or null.
     * <p>
     * 참조를 해석할 수 없을 때 쓰는 대체 페인트. 없으면 null.
     */
    final SvgPaint fallback;

    /**
     * Creates a paint.
     * <p>
     * 페인트를 만든다.
     *
     * @param kind     kind / 종류
     * @param argb     colour / 색
     * @param ref      paint server id / 페인트 서버 id
     * @param fallback fallback paint / 대체 페인트
     */
    SvgPaint(int kind, int argb, String ref, SvgPaint fallback) {
        this.kind = kind;
        this.argb = argb;
        this.ref = ref;
        this.fallback = fallback;
    }

    /**
     * Creates a colour paint.
     * <p>
     * 색 페인트를 만든다.
     *
     * @param argb the colour / 색
     * @return the paint / 페인트
     */
    static SvgPaint color(int argb) {
        return new SvgPaint(COLOR, argb, null, null);
    }

    /**
     * Parses a paint value.
     * <p>
     * 페인트 값을 해석한다.
     *
     * @param text the value text / 값 텍스트
     * @return the paint or null if the text is not a valid paint / 페인트. 올바른 페인트가 아니면 null
     */
    static SvgPaint parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        String lower = t.toLowerCase(java.util.Locale.ENGLISH);
        if ("none".equals(lower)) {
            return NONE_PAINT;
        }
        if ("currentcolor".equals(lower)) {
            return new SvgPaint(CURRENT, 0, null, null);
        }
        if ("context-fill".equals(lower)) {
            return new SvgPaint(CONTEXT_FILL, 0, null, null);
        }
        if ("context-stroke".equals(lower)) {
            return new SvgPaint(CONTEXT_STROKE, 0, null, null);
        }
        if (lower.startsWith("url(")) {
            int close = findUrlEnd(t);
            if (close < 0) {
                return null;
            }
            String id = SvgStyle.urlId(t.substring(0, close + 1));
            if (id == null) {
                return null;
            }
            String rest = t.substring(close + 1).trim();
            SvgPaint fb = null;
            if (rest.length() > 0) {
                fb = parse(rest);
                if (fb == null || fb.kind == URL) {
                    return null;
                }
            }
            return new SvgPaint(URL, 0, id, fb);
        }
        Integer c = SvgColors.parse(t);
        if (c == null) {
            return null;
        }
        return color(c.intValue());
    }

    /**
     * Finds the closing parenthesis of a leading <code>url(...)</code>, skipping quotes.
     * <p>
     * 앞에 있는 <code>url(...)</code> 의 닫는 괄호를 찾는다(따옴표 안은 건너뜀).
     *
     * @param t the text starting with <code>url(</code> / <code>url(</code> 로 시작하는 텍스트
     * @return the index of the closing parenthesis or -1 / 닫는 괄호의 위치. 없으면 -1
     */
    static int findUrlEnd(String t) {
        int i = 4;
        // 따옴표가 있으면 따옴표 끝을 건너뛴 뒤 ')' 를 찾는다.
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '"' || c == '\'') {
                int e = t.indexOf(c, i + 1);
                if (e < 0) {
                    return -1;
                }
                i = e + 1;
            } else if (c == ')') {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }
}
