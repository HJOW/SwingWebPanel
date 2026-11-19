package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;

/**
 * The filter functions of the 2D canvas (<code>context.filter</code>, HTML Living Standard 4.12.5.1.20): the public door to the pixel code of
 * {@link FilterRenderer} that the page layout uses for the CSS <code>filter</code> property, with a parser for a string that is a
 * <code>&lt;filter-value-list&gt;</code>. A value is valid when the grammar of the <code>filter</code> property accepts it (CSS-wide keywords
 * such as <code>inherit</code> and <code>var()</code> are refused as the canvas specification demands); only the functions
 * (<code>blur()</code>, <code>brightness()</code>, <code>contrast()</code>, <code>drop-shadow()</code>, <code>grayscale()</code>,
 * <code>hue-rotate()</code>, <code>invert()</code>, <code>opacity()</code>, <code>saturate()</code>, <code>sepia()</code>) have an effect, a
 * <code>url()</code> reference to an SVG filter is valid but filters nothing (the filter of the canvas is applied "with no filtering" as for a
 * document that is not loaded). Relative lengths (<code>em</code>) are resolved against the font size that is given at the time the value is
 * parsed. The lengths are pixels of the bitmap and are not affected by the transformation of the context.
 * <p>
 * 2D 캔버스의 필터 함수(<code>context.filter</code>, HTML Living Standard 4.12.5.1.20): 페이지 레이아웃이 CSS <code>filter</code> 속성에 쓰는
 * {@link FilterRenderer} 의 화소 코드로 가는 공개 문이며 <code>&lt;filter-value-list&gt;</code> 인 문자열의 해석기가 딸려 있다. 값은 <code>filter</code> 속성의 문법이
 * 받아들이면 올바르다(<code>inherit</code> 같은 CSS 전역 키워드와 <code>var()</code> 는 캔버스 규격이 요구하는 대로 거부한다). 함수(<code>blur()</code>,
 * <code>brightness()</code>, <code>contrast()</code>, <code>drop-shadow()</code>, <code>grayscale()</code>, <code>hue-rotate()</code>, <code>invert()</code>,
 * <code>opacity()</code>, <code>saturate()</code>, <code>sepia()</code>)만 효과가 있고 SVG 필터를 가리키는 <code>url()</code> 은 올바르지만 아무것도 거르지 않는다
 * (문서가 로드되지 않은 것처럼 "필터 없이" 그린다). 상대 길이(<code>em</code>)는 값을 해석할 때 주어진 글꼴 크기를 기준으로 환산한다. 길이는 비트맵의 화소이며 컨텍스트의
 * 변환의 영향을 받지 않는다.
 */
public final class CanvasFilters {

    /**
     * The result of parsing a filter string.
     * <p>
     * 필터 문자열을 해석한 결과.
     */
    public static final class Parsed {
        /**
         * The functions to apply, or null if the string is valid but filters nothing (<code>none</code>, a <code>url()</code> reference).
         * <p>
         * 적용할 함수. 문자열이 올바르지만 아무것도 거르지 않으면(<code>none</code>, <code>url()</code> 참조) null.
         */
        private final FilterSpec spec;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param spec the functions or null / 함수 또는 null
         */
        Parsed(FilterSpec spec) {
            this.spec = spec;
        }

        /**
         * Returns the functions.
         * <p>
         * 함수를 반환한다.
         *
         * @return the functions, null if nothing is filtered / 함수. 아무것도 거르지 않으면 null
         */
        public FilterSpec getSpec() {
            return spec;
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CanvasFilters() {
    }

    /**
     * Parses a string as a <code>&lt;filter-value-list&gt;</code>.
     * <p>
     * 문자열을 <code>&lt;filter-value-list&gt;</code> 로 해석한다.
     *
     * @param text         the string / 문자열
     * @param fontSizePx   the font size in pixels that <code>em</code> refers to / <code>em</code> 이 기준으로 삼는 글꼴 크기(화소)
     * @param currentColor the ARGB color that <code>currentcolor</code> and the default color of <code>drop-shadow()</code> mean / <code>currentcolor</code> 와
     *                     <code>drop-shadow()</code> 의 기본 색이 뜻하는 ARGB 색
     * @return the result, or null if the string is not a valid filter list / 결과. 올바른 필터 목록이 아니면 null
     */
    public static Parsed parse(String text, double fontSizePx, int currentColor) {
        if (text == null || text.trim().length() == 0) {
            return null;
        }
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        int code = PropertyRegistry.getInstance().parse("filter", text, out);
        CssValue v = out.get(Integer.valueOf(Prop.FILTER));
        if (code != PropertyRegistry.OK || v == null) {
            return null;
        }
        // 전역 키워드(inherit 등)와 대기 값(var())은 캔버스의 필터로 올바르지 않다.
        if (v instanceof CssKeyword && ((CssKeyword) v).isCssWide()) {
            return null;
        }
        if (v instanceof CssPending) {
            return null;
        }
        LengthContext ctx = new LengthContext(fontSizePx, LengthContext.DEFAULT_FONT_SIZE, fontSizePx * 1.2, 300, 150);
        FilterSpec spec = FilterSpec.of(v, currentColor, ctx);
        // url() 참조는 올바르지만 캔버스에서는 거르지 않는 값이다(캔버스는 문서의 SVG 요소를 읽지 않는다).
        if (spec != null && spec.hasReferences()) {
            return new Parsed(null);
        }
        if (spec != null) {
            return new Parsed(spec);
        }
        // 필터 함수가 아닌 항목이 url() 이면 올바르지만 거르지 않는 값이다. 그 밖의 해석 실패(잘못된 인수)는 올바르지 않은 값이다.
        List<CssValue> items = new ArrayList<CssValue>();
        if (v instanceof CssList) {
            items.addAll(((CssList) v).getItems());
        } else {
            items.add(v);
        }
        // 항목 중에 url() 참조가 있는지 찾는다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof CssUrl) {
                return new Parsed(null);
            }
        }
        return null;
    }

    /**
     * Returns how far the filter functions grow the painted area beyond what was drawn: blurs and shadows reach outwards.
     * <p>
     * 필터 함수가 그린 영역을 얼마나 넓히는지 반환한다: 흐림과 그림자는 바깥으로 번진다.
     *
     * @param spec the functions / 함수
     * @return {left, top, right, bottom} in whole pixels / 정수 화소의 {왼쪽, 위쪽, 오른쪽, 아래쪽}
     */
    public static int[] margin(FilterSpec spec) {
        double[] e = spec.extent();
        int[] m = new int[4];
        // 네 방향의 거리를 올려서 정수로 만든다.
        for (int i = 0; i < 4; i++) {
            m[i] = (int) Math.min(1 << 20, Math.ceil(e[i]));
        }
        return m;
    }

    /**
     * Applies the filter functions to a layer in place: image A of the drawing model becomes image B.
     * <p>
     * 레이어에 필터 함수를 제자리에서 적용한다: 그리기 모델의 이미지 A 가 이미지 B 가 된다.
     *
     * @param argb   the pixels (ARGB, not premultiplied) / 화소(ARGB, 미리 곱하지 않음)
     * @param width  the width of the layer / 레이어 너비
     * @param height the height of the layer / 레이어 높이
     * @param spec   the functions / 함수
     */
    public static void apply(int[] argb, int width, int height, FilterSpec spec) {
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, width, height, argb, 0, width);
        // 필터의 길이는 비트맵 화소이므로 변환 없이(단위 변환으로) 적용한다.
        FilterRenderer.apply(img, spec, new AffineTransform());
        img.getRGB(0, 0, width, height, argb, 0, width);
    }
}
