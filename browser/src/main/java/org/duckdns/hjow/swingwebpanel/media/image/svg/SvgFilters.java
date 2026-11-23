package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;

/**
 * Filter effects (plan item P18-04): the <code>filter</code> property with <code>url(#id)</code> references to <code>filter</code> elements
 * and with the CSS filter functions (<code>blur</code>, <code>brightness</code>, <code>contrast</code>, <code>grayscale</code>,
 * <code>hue-rotate</code>, <code>invert</code>, <code>opacity</code>, <code>saturate</code>, <code>sepia</code>, <code>drop-shadow</code>),
 * and the blend modes of <code>mix-blend-mode</code>. The filter primitives <code>feBlend</code>, <code>feColorMatrix</code>,
 * <code>feComponentTransfer</code>, <code>feComposite</code>, <code>feConvolveMatrix</code>, <code>feDisplacementMap</code>,
 * <code>feDropShadow</code>, <code>feFlood</code>, <code>feGaussianBlur</code>, <code>feImage</code> (data URIs and element references),
 * <code>feMerge</code>, <code>feMorphology</code>, <code>feOffset</code>, <code>feTile</code> and <code>feTurbulence</code> work on pixel
 * arrays in device resolution, in linearRGB or sRGB as <code>color-interpolation-filters</code> says, with filter regions, primitive
 * subregions, named results and <code>objectBoundingBox</code> primitive units. Not supported: the lighting primitives
 * (<code>feDiffuseLighting</code>, <code>feSpecularLighting</code>), which are skipped, and the non-separable blend modes (treated as
 * normal). A filter on a rotated or skewed element is computed with the scale of its transform only.
 * <p>
 * 필터 효과(계획 P18-04): <code>filter</code> 요소를 가리키는 <code>url(#id)</code> 와 CSS 필터 함수(<code>blur</code>, <code>brightness</code>,
 * <code>contrast</code>, <code>grayscale</code>, <code>hue-rotate</code>, <code>invert</code>, <code>opacity</code>, <code>saturate</code>,
 * <code>sepia</code>, <code>drop-shadow</code>)를 쓰는 <code>filter</code> 속성과 <code>mix-blend-mode</code> 의 혼합 모드. 필터 기본 요소
 * <code>feBlend</code>, <code>feColorMatrix</code>, <code>feComponentTransfer</code>, <code>feComposite</code>, <code>feConvolveMatrix</code>,
 * <code>feDisplacementMap</code>, <code>feDropShadow</code>, <code>feFlood</code>, <code>feGaussianBlur</code>, <code>feImage</code>(data URI 와
 * 요소 참조), <code>feMerge</code>, <code>feMorphology</code>, <code>feOffset</code>, <code>feTile</code>, <code>feTurbulence</code> 는 장치
 * 해상도의 픽셀 배열에서 <code>color-interpolation-filters</code> 에 따라 linearRGB 또는 sRGB 로 동작하며 필터 영역, 기본 요소 하위 영역, 이름 붙은
 * 결과, <code>objectBoundingBox</code> 기본 요소 단위를 지원한다. 지원하지 않는 것: 조명 기본 요소(<code>feDiffuseLighting</code>,
 * <code>feSpecularLighting</code>, 건너뜀)와 비분리형 혼합 모드(보통으로 처리). 회전하거나 기울인 요소의 필터는 변환의 배율만으로 계산한다.
 */
final class SvgFilters {

    /**
     * The sRGB to linearRGB table for 8 bit values.
     * <p>
     * 8비트 값의 sRGB → linearRGB 표.
     */
    private static final int[] TO_LINEAR = new int[256];

    /**
     * The linearRGB to sRGB table for 8 bit values.
     * <p>
     * 8비트 값의 linearRGB → sRGB 표.
     */
    private static final int[] TO_SRGB = new int[256];

    static {
        // 두 변환 표를 규격의 식으로 미리 계산한다.
        for (int i = 0; i < 256; i++) {
            double c = i / 255.0;
            double lin = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
            TO_LINEAR[i] = (int) Math.round(lin * 255);
            double s = c <= 0.0031308 ? c * 12.92 : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
            TO_SRGB[i] = (int) Math.round(Math.max(0, Math.min(1, s)) * 255);
        }
    }

    /**
     * A CSS filter function such as <code>blur(2px)</code> in the filter list.
     * <p>
     * 필터 목록의 <code>blur(2px)</code> 같은 CSS 필터 함수.
     */
    static final class Fn {

        /**
         * The lower-case function name.
         * <p>
         * 소문자 함수 이름.
         */
        String name;

        /**
         * The numeric arguments (lengths in pixels, angles in degrees, amounts as numbers).
         * <p>
         * 수 인수(길이는 픽셀, 각도는 도, 양은 수).
         */
        double[] args = new double[0];

        /**
         * The colour argument of <code>drop-shadow</code> (<code>0xAARRGGBB</code>).
         * <p>
         * <code>drop-shadow</code> 의 색 인수(<code>0xAARRGGBB</code>).
         */
        int color = 0xFF000000;
    }

    /**
     * A pixel buffer of the filter pipeline: premultiplied ARGB with its colour space.
     * <p>
     * 필터 파이프라인의 픽셀 버퍼: 색 공간을 가진 알파 미리 곱한 ARGB.
     */
    private static final class Img {

        /**
         * The pixels (premultiplied ARGB).
         * <p>
         * 픽셀(알파 미리 곱한 ARGB).
         */
        final int[] px;

        /**
         * The width.
         * <p>
         * 너비.
         */
        final int w;

        /**
         * The height.
         * <p>
         * 높이.
         */
        final int h;

        /**
         * True if the colour values are in linearRGB.
         * <p>
         * 색 값이 linearRGB 이면 true.
         */
        boolean linear;

        /**
         * The subregion that holds the content (anything outside is transparent), in buffer coordinates.
         * <p>
         * 내용이 있는 하위 영역(바깥은 투명), 버퍼 좌표.
         */
        Rectangle sub;

        /**
         * Creates an empty buffer.
         * <p>
         * 빈 버퍼를 만든다.
         *
         * @param w width / 너비
         * @param h height / 높이
         */
        Img(int w, int h) {
            this.w = w;
            this.h = h;
            this.px = new int[w * h];
            this.sub = new Rectangle(0, 0, w, h);
        }

        /**
         * Creates a copy.
         * <p>
         * 복사본을 만든다.
         *
         * @return the copy / 복사본
         */
        Img copy() {
            Img c = new Img(w, h);
            System.arraycopy(px, 0, c.px, 0, px.length);
            c.linear = linear;
            c.sub = new Rectangle(sub);
            return c;
        }
    }

    /**
     * The context of one filter element evaluation.
     * <p>
     * 필터 요소 하나를 평가하는 문맥.
     */
    private static final class Ctx {

        /**
         * The renderer.
         * <p>
         * 렌더러.
         */
        SvgRenderer r;

        /**
         * The region in device pixels (origin of the buffers).
         * <p>
         * 장치 픽셀 단위의 영역(버퍼의 원점).
         */
        Rectangle region;

        /**
         * The transform from user space to device pixels.
         * <p>
         * 사용자 공간에서 장치 픽셀로의 변환.
         */
        AffineTransform ctm;

        /**
         * The device scale along x.
         * <p>
         * x 방향 장치 배율.
         */
        double sx;

        /**
         * The device scale along y.
         * <p>
         * y 방향 장치 배율.
         */
        double sy;

        /**
         * The bounding box of the filtered element or null.
         * <p>
         * 필터가 걸리는 요소의 경계 상자. 없으면 null.
         */
        Rectangle2D bbox;

        /**
         * True if primitive units are object bounding box.
         * <p>
         * 기본 요소 단위가 객체 경계 상자이면 true.
         */
        boolean primObb;

        /**
         * The filter region in user space.
         * <p>
         * 사용자 공간의 필터 영역.
         */
        Rectangle2D userRegion;

        /**
         * The source graphic (sRGB).
         * <p>
         * 원본 그래픽(sRGB).
         */
        Img source;

        /**
         * The named results.
         * <p>
         * 이름 붙은 결과.
         */
        Map<String, Img> named = new HashMap<String, Img>();

        /**
         * The target element (for feImage references).
         * <p>
         * 대상 요소(feImage 참조용).
         */
        SvgElement target;

        /**
         * The style of the target.
         * <p>
         * 대상의 스타일.
         */
        SvgStyle style;
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgFilters() {
    }

    /**
     * Parses a filter property value into a list of {@link SvgElement} (filter elements) and {@link Fn} (CSS functions). A reference to
     * something that is not a filter element is skipped, so that the element is drawn without that filter.
     * <p>
     * 필터 속성 값을 {@link SvgElement}(필터 요소)와 {@link Fn}(CSS 함수)의 목록으로 해석한다. 필터 요소가 아닌 것에 대한 참조는 건너뛰어 그 필터 없이
     * 요소를 그린다.
     *
     * @param r     the renderer / 렌더러
     * @param value the property text / 속성 텍스트
     * @param st    the style (for lengths) / 스타일(길이용)
     * @return the list, possibly empty / 목록. 비어 있을 수 있음
     */
    static List<Object> parseFilterList(SvgRenderer r, String value, SvgStyle st) {
        List<Object> out = new ArrayList<Object>();
        String v = value.trim();
        int i = 0;
        // 공백으로 구분된 url(...) 과 함수(...) 항목을 차례로 읽는다.
        while (i < v.length()) {
            while (i < v.length() && SvgScanner.isWs(v.charAt(i))) {
                i++;
            }
            if (i >= v.length()) {
                break;
            }
            int open = v.indexOf('(', i);
            if (open < 0) {
                return new ArrayList<Object>();
            }
            int close = matchParen(v, open);
            if (close < 0) {
                return new ArrayList<Object>();
            }
            String name = v.substring(i, open).trim().toLowerCase(Locale.ENGLISH);
            String args = v.substring(open + 1, close).trim();
            i = close + 1;
            if ("url".equals(name)) {
                String id = SvgStyle.urlId("url(" + args + ")");
                SvgElement f = id == null ? null : r.doc.ids.get(id);
                if (f != null && f.is("filter")) {
                    out.add(f);
                }
            } else {
                Fn fn = parseFunction(name, args, st);
                if (fn == null) {
                    return new ArrayList<Object>();
                }
                out.add(fn);
            }
        }
        return out;
    }

    /**
     * Finds the closing parenthesis that matches an opening one.
     * <p>
     * 여는 괄호에 짝이 되는 닫는 괄호를 찾는다.
     *
     * @param s    the text / 텍스트
     * @param open the index of the opening parenthesis / 여는 괄호의 위치
     * @return the index of the closing one or -1 / 닫는 괄호의 위치. 없으면 -1
     */
    private static int matchParen(String s, int open) {
        int depth = 0;
        // 중첩된 괄호 깊이를 세며 짝을 찾는다.
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Parses one CSS filter function.
     * <p>
     * CSS 필터 함수 하나를 해석한다.
     *
     * @param name the lower-case function name / 소문자 함수 이름
     * @param args the argument text / 인수 텍스트
     * @param st   the style / 스타일
     * @return the function or null if invalid / 함수. 올바르지 않으면 null
     */
    private static Fn parseFunction(String name, String args, SvgStyle st) {
        Fn f = new Fn();
        f.name = name;
        if ("blur".equals(name)) {
            SvgLength l = args.length() == 0 ? new SvgLength(0, "px") : SvgLength.parse(args);
            if (l == null || l.isPercent() || l.value < 0) {
                return null;
            }
            f.args = new double[] { st.px(l, 2) };
            return f;
        }
        if ("hue-rotate".equals(name)) {
            SvgScanner sc = new SvgScanner(args);
            double deg = 0;
            if (sc.number()) {
                deg = SvgTransforms.angleToDegrees(sc.value, sc.letters().toLowerCase(Locale.ENGLISH));
            }
            if (Double.isNaN(deg)) {
                return null;
            }
            f.args = new double[] { deg };
            return f;
        }
        if ("drop-shadow".equals(name)) {
            return parseDropShadow(f, args, st);
        }
        if ("brightness".equals(name) || "contrast".equals(name) || "grayscale".equals(name) || "invert".equals(name)
                || "opacity".equals(name) || "saturate".equals(name) || "sepia".equals(name)) {
            double def = ("grayscale".equals(name) || "invert".equals(name) || "sepia".equals(name)) ? 1 : 1;
            double amount = def;
            if (args.length() > 0) {
                SvgLength l = SvgLength.parse(args);
                if (l == null || (l.unit.length() > 0 && !l.isPercent()) || l.value < 0) {
                    return null;
                }
                amount = l.isPercent() ? l.value / 100 : l.value;
            }
            if ("grayscale".equals(name) || "invert".equals(name) || "sepia".equals(name) || "opacity".equals(name)) {
                amount = Math.min(1, amount);
            }
            f.args = new double[] { amount };
            return f;
        }
        return null;
    }

    /**
     * Parses the arguments of <code>drop-shadow</code>: two or three lengths and an optional colour in any order.
     * <p>
     * <code>drop-shadow</code> 의 인수를 해석한다: 길이 두세 개와 선택적인 색(순서 무관).
     *
     * @param f    the function to fill / 채울 함수
     * @param args the argument text / 인수 텍스트
     * @param st   the style / 스타일
     * @return the function or null / 함수. 올바르지 않으면 null
     */
    private static Fn parseDropShadow(Fn f, String args, SvgStyle st) {
        // 색 함수(rgb(...))를 쪼개지 않도록 괄호 밖의 공백으로 토큰을 나눈다.
        List<String> tokens = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        // 괄호 밖의 공백에서 인수를 토큰으로 나눈다.
        for (int i = 0; i <= args.length(); i++) {
            char c = i < args.length() ? args.charAt(i) : ' ';
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ' ' && depth == 0) {
                if (i > start) {
                    tokens.add(args.substring(start, i));
                }
                start = i + 1;
            }
        }
        List<Double> nums = new ArrayList<Double>();
        int color = st.color();
        // 토큰이 길이이면 수로, 아니면 색으로 읽는다.
        for (int i = 0; i < tokens.size(); i++) {
            SvgLength l = SvgLength.parse(tokens.get(i));
            if (l != null) {
                nums.add(Double.valueOf(st.px(l, 2)));
            } else {
                Integer c = "currentcolor".equalsIgnoreCase(tokens.get(i)) ? Integer.valueOf(st.color()) : SvgColors.parse(tokens.get(i));
                if (c == null) {
                    return null;
                }
                color = c.intValue();
            }
        }
        if (nums.size() < 2 || nums.size() > 3) {
            return null;
        }
        f.args = new double[] { nums.get(0).doubleValue(), nums.get(1).doubleValue(), nums.size() > 2 ? nums.get(2).doubleValue() : 0 };
        f.color = color;
        return f;
    }

    /**
     * Applies filters to a layer.
     * <p>
     * 레이어에 필터를 적용한다.
     *
     * @param r       the renderer / 렌더러
     * @param layer   the layer image (premultiplied) / 레이어 이미지(알파 미리 곱함)
     * @param filters the filters from {@link #parseFilterList} / {@link #parseFilterList} 가 만든 필터
     * @param target  the filtered element / 필터가 걸리는 요소
     * @param st      its style / 스타일
     */
    static void apply(SvgRenderer r, BufferedImage layer, List<Object> filters, SvgElement target, SvgStyle st) {
        int[] data = ((DataBufferInt) layer.getRaster().getDataBuffer()).getData();
        // 필터 목록을 순서대로 적용한다(앞 필터의 결과가 다음 필터의 입력이다).
        for (int i = 0; i < filters.size(); i++) {
            r.tick();
            Object f = filters.get(i);
            Ctx c = new Ctx();
            c.r = r;
            c.ctm = r.g.getTransform();
            c.sx = Math.sqrt(c.ctm.getScaleX() * c.ctm.getScaleX() + c.ctm.getShearY() * c.ctm.getShearY());
            c.sy = Math.sqrt(c.ctm.getShearX() * c.ctm.getShearX() + c.ctm.getScaleY() * c.ctm.getScaleY());
            c.target = target;
            c.style = st;
            Img result;
            Rectangle full = new Rectangle(0, 0, layer.getWidth(), layer.getHeight());
            if (f instanceof Fn) {
                c.region = full;
                c.source = extract(data, layer.getWidth(), full);
                result = applyFunction(c, (Fn) f);
            } else {
                result = applyFilterElement(c, (SvgElement) f, layer, data);
            }
            // 결과를 레이어에 되쓴다. 필터 영역 밖은 지운다.
            java.util.Arrays.fill(data, 0);
            if (result != null) {
                store(result, c.region, data, layer.getWidth(), layer.getHeight());
            }
        }
    }

    /**
     * Copies a rectangle of the layer into a new buffer (sRGB).
     * <p>
     * 레이어의 사각형을 새 버퍼(sRGB)로 복사한다.
     *
     * @param data   the layer pixels / 레이어 픽셀
     * @param stride the layer width / 레이어 너비
     * @param rect   the rectangle / 사각형
     * @return the buffer / 버퍼
     */
    private static Img extract(int[] data, int stride, Rectangle rect) {
        Img img = new Img(rect.width, rect.height);
        // 행 단위로 복사한다.
        for (int y = 0; y < rect.height; y++) {
            System.arraycopy(data, (rect.y + y) * stride + rect.x, img.px, y * rect.width, rect.width);
        }
        return img;
    }

    /**
     * Writes a buffer back into the layer in sRGB.
     * <p>
     * 버퍼를 sRGB 로 레이어에 되쓴다.
     *
     * @param img    the buffer / 버퍼
     * @param rect   the region that receives the result / 결과를 받는 영역
     * @param data   the layer pixels / 레이어 픽셀
     * @param stride the layer width / 레이어 너비
     * @param lh     the layer height / 레이어 높이
     */
    private static void store(Img img, Rectangle rect, int[] data, int stride, int lh) {
        Img s = toSpace(img, false);
        Rectangle clip = rect.intersection(s.sub).intersection(new Rectangle(0, 0, stride, lh));
        // 영역과 하위 영역이 겹치는 부분만 레이어에 쓴다.
        for (int y = clip.y; y < clip.y + clip.height; y++) {
            for (int x = clip.x; x < clip.x + clip.width; x++) {
                data[y * stride + x] = s.px[y * s.w + x];
            }
        }
    }

    /**
     * Converts a buffer to a colour space (a copy is returned if a conversion is needed, else the same buffer).
     * <p>
     * 버퍼를 색 공간으로 바꾼다(변환이 필요하면 복사본을, 아니면 같은 버퍼를 반환한다).
     *
     * @param in     the buffer / 버퍼
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the buffer in that space / 그 공간의 버퍼
     */
    private static Img toSpace(Img in, boolean linear) {
        if (in.linear == linear) {
            return in;
        }
        Img out = new Img(in.w, in.h);
        out.sub = new Rectangle(in.sub);
        out.linear = linear;
        int[] table = linear ? TO_LINEAR : TO_SRGB;
        // 픽셀마다 알파를 나누어 색 값에 표를 적용한 뒤 다시 알파를 곱한다.
        for (int i = 0; i < in.px.length; i++) {
            int p = in.px[i];
            int a = p >>> 24;
            if (a == 0) {
                continue;
            }
            int r = a == 255 ? (p >> 16) & 0xFF : Math.min(255, (((p >> 16) & 0xFF) * 255 + a / 2) / a);
            int g = a == 255 ? (p >> 8) & 0xFF : Math.min(255, (((p >> 8) & 0xFF) * 255 + a / 2) / a);
            int b = a == 255 ? p & 0xFF : Math.min(255, ((p & 0xFF) * 255 + a / 2) / a);
            r = table[r];
            g = table[g];
            b = table[b];
            if (a != 255) {
                r = (r * a + 127) / 255;
                g = (g * a + 127) / 255;
                b = (b * a + 127) / 255;
            }
            out.px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return out;
    }

    /**
     * Applies one CSS filter function to the source graphic of the context.
     * <p>
     * CSS 필터 함수 하나를 문맥의 원본 그래픽에 적용한다.
     *
     * @param c  the context / 문맥
     * @param fn the function / 함수
     * @return the result / 결과
     */
    private static Img applyFunction(Ctx c, Fn fn) {
        Img src = toSpace(c.source, false);
        String n = fn.name;
        if ("blur".equals(n)) {
            return gaussian(src, fn.args[0] * c.sx, fn.args[0] * c.sy);
        }
        if ("drop-shadow".equals(n)) {
            double dx = fn.args[0] * c.sx;
            double dy = fn.args[1] * c.sy;
            double blur = fn.args[2] / 2;
            Img shadow = flood(src.w, src.h, fn.color, true);
            shadow = composite(shadow, src, 1);
            shadow = offset(shadow, (int) Math.round(dx), (int) Math.round(dy));
            if (blur > 0) {
                shadow = gaussian(shadow, blur * c.sx, blur * c.sy);
            }
            return over(src, shadow);
        }
        double a = fn.args[0];
        double[] m;
        if ("brightness".equals(n)) {
            m = new double[] { a, 0, 0, 0, 0, 0, a, 0, 0, 0, 0, 0, a, 0, 0, 0, 0, 0, 1, 0 };
        } else if ("contrast".equals(n)) {
            double t = 0.5 - a / 2;
            m = new double[] { a, 0, 0, 0, t, 0, a, 0, 0, t, 0, 0, a, 0, t, 0, 0, 0, 1, 0 };
        } else if ("grayscale".equals(n)) {
            m = saturateMatrix(1 - a);
        } else if ("hue-rotate".equals(n)) {
            m = hueMatrix(fn.args[0]);
        } else if ("invert".equals(n)) {
            m = new double[] { 1 - 2 * a, 0, 0, 0, a, 0, 1 - 2 * a, 0, 0, a, 0, 0, 1 - 2 * a, 0, a, 0, 0, 0, 1, 0 };
        } else if ("opacity".equals(n)) {
            m = new double[] { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, a, 0 };
        } else if ("saturate".equals(n)) {
            m = saturateMatrix(a);
        } else {
            m = new double[] { 0.393 + 0.607 * (1 - a), 0.769 - 0.769 * (1 - a), 0.189 - 0.189 * (1 - a), 0, 0,
                    0.349 - 0.349 * (1 - a), 0.686 + 0.314 * (1 - a), 0.168 - 0.168 * (1 - a), 0, 0,
                    0.272 - 0.272 * (1 - a), 0.534 - 0.534 * (1 - a), 0.131 + 0.869 * (1 - a), 0, 0, 0, 0, 0, 1, 0 };
        }
        return colorMatrix(src, m);
    }

    /**
     * Builds the matrix of <code>feColorMatrix type="saturate"</code>.
     * <p>
     * <code>feColorMatrix type="saturate"</code> 의 행렬을 만든다.
     *
     * @param s the saturation / 채도
     * @return the 4x5 matrix / 4x5 행렬
     */
    private static double[] saturateMatrix(double s) {
        return new double[] { 0.213 + 0.787 * s, 0.715 - 0.715 * s, 0.072 - 0.072 * s, 0, 0, 0.213 - 0.213 * s, 0.715 + 0.285 * s,
                0.072 - 0.072 * s, 0, 0, 0.213 - 0.213 * s, 0.715 - 0.715 * s, 0.072 + 0.928 * s, 0, 0, 0, 0, 0, 1, 0 };
    }

    /**
     * Builds the matrix of <code>feColorMatrix type="hueRotate"</code>.
     * <p>
     * <code>feColorMatrix type="hueRotate"</code> 의 행렬을 만든다.
     *
     * @param deg the angle in degrees / 각도(도)
     * @return the 4x5 matrix / 4x5 행렬
     */
    private static double[] hueMatrix(double deg) {
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new double[] { 0.213 + cos * 0.787 - sin * 0.213, 0.715 - cos * 0.715 - sin * 0.715, 0.072 - cos * 0.072 + sin * 0.928, 0, 0,
                0.213 - cos * 0.213 + sin * 0.143, 0.715 + cos * 0.285 + sin * 0.140, 0.072 - cos * 0.072 - sin * 0.283, 0, 0,
                0.213 - cos * 0.213 - sin * 0.787, 0.715 - cos * 0.715 + sin * 0.715, 0.072 + cos * 0.928 + sin * 0.072, 0, 0, 0, 0, 0, 1, 0 };
    }

    /**
     * Evaluates a <code>filter</code> element on the layer.
     * <p>
     * <code>filter</code> 요소를 레이어에 평가한다.
     *
     * @param c      the context / 문맥
     * @param filter the filter element / filter 요소
     * @param layer  the layer image / 레이어 이미지
     * @param data   the layer pixels / 레이어 픽셀
     * @return the result, or null if the element is not rendered / 결과. 요소를 그리지 않으면 null
     */
    private static Img applyFilterElement(Ctx c, SvgElement filter, BufferedImage layer, int[] data) {
        SvgRenderer r = c.r;
        if (r.active.contains(filter)) {
            return null;
        }
        boolean userUnits = "userSpaceOnUse".equals(SvgPaintServers.serverAttr(r, filter, "filterUnits", null));
        c.primObb = "objectBoundingBox".equals(SvgPaintServers.serverAttr(r, filter, "primitiveUnits", null));
        Rectangle2D bb = null;
        if (!userUnits || c.primObb) {
            bb = SvgBounds.bbox(r, c.target, c.style);
            if (bb == null || bb.getWidth() <= 0 || bb.getHeight() <= 0) {
                c.region = new Rectangle(0, 0, 0, 0);
                return null;
            }
        }
        c.bbox = bb;
        SvgStyle fs = r.styleFor(filter);
        c.userRegion = userRegion(r, filter, fs, userUnits, bb);
        if (c.userRegion == null) {
            c.region = new Rectangle(0, 0, 0, 0);
            return null;
        }
        Rectangle2D dev = c.ctm.createTransformedShape(c.userRegion).getBounds2D();
        int x0 = (int) Math.max(0, Math.floor(dev.getMinX() + 1e-6));
        int y0 = (int) Math.max(0, Math.floor(dev.getMinY() + 1e-6));
        int x1 = (int) Math.min(layer.getWidth(), Math.ceil(dev.getMaxX() - 1e-6));
        int y1 = (int) Math.min(layer.getHeight(), Math.ceil(dev.getMaxY() - 1e-6));
        if (x1 <= x0 || y1 <= y0) {
            c.region = new Rectangle(0, 0, 0, 0);
            return null;
        }
        c.region = new Rectangle(x0, y0, x1 - x0, y1 - y0);
        // 원본 그래픽은 영역 밖의 내용도 포함한 캔버스 전체이다(결과만 영역으로 자른다).
        c.source = extract(data, layer.getWidth(), new Rectangle(0, 0, layer.getWidth(), layer.getHeight()));
        SvgElement src = filterChildren(r, filter);
        if (src == null) {
            return null;
        }
        String cif = SvgPaintServers.serverAttr(r, filter, "color-interpolation-filters", null);
        String own = fs.keyword("color-interpolation-filters");
        boolean linearDefault = !("srgb".equals(cif == null ? own : cif.toLowerCase(Locale.ENGLISH)));
        r.active.add(filter);
        try {
            Img last = null;
            SvgStyle parent = r.styleFor(src);
            // 기본 요소를 순서대로 평가한다.
            for (int i = 0; i < src.children.size(); i++) {
                SvgElement p = src.children.get(i);
                if (!p.isSvg() || !p.name.startsWith("fe")) {
                    continue;
                }
                r.tick();
                SvgStyle ps = new SvgStyle(parent, p, r.vpW, r.vpH);
                String pc = ps.own("color-interpolation-filters");
                boolean linear = pc == null ? (own != null || cif != null ? linearDefault : true) : !"srgb".equalsIgnoreCase(pc);
                if (pc == null && ps.get("color-interpolation-filters") != null) {
                    linear = !"srgb".equalsIgnoreCase(ps.get("color-interpolation-filters"));
                }
                Img out = primitive(c, p, ps, last, linear);
                if (out == null) {
                    continue;
                }
                last = out;
                String name = p.attrs.get("result");
                if (name != null) {
                    c.named.put(name, out);
                }
            }
            return last;
        } finally {
            r.active.remove(filter);
        }
    }

    /**
     * Returns the filter region of a <code>filter</code> element in the user space of the filtered element: the attributes <code>x</code>,
     * <code>y</code>, <code>width</code> and <code>height</code> in <code>filterUnits</code> (default -10%, -10%, 120%, 120% of the bounding box).
     * <p>
     * 필터 요소의 필터 영역을 필터가 걸리는 요소의 사용자 공간으로 반환한다: <code>filterUnits</code> 로 읽은 <code>x</code>, <code>y</code>,
     * <code>width</code>, <code>height</code> 속성(기본은 경계 상자의 -10%, -10%, 120%, 120%).
     *
     * @param r         the renderer / 렌더러
     * @param filter    the filter element / 필터 요소
     * @param fs        the style of the filter element / 필터 요소의 스타일
     * @param userUnits true for <code>userSpaceOnUse</code> / <code>userSpaceOnUse</code> 이면 true
     * @param bb        the bounding box of the element (needed unless <code>userUnits</code>) / 요소의 경계 상자(<code>userUnits</code> 가 아니면 필요)
     * @return the region, or null if it is empty / 영역. 비어 있으면 null
     */
    static Rectangle2D userRegion(SvgRenderer r, SvgElement filter, SvgStyle fs, boolean userUnits, Rectangle2D bb) {
        double x;
        double y;
        double w;
        double h;
        // 좌표 단위에 따라 길이나 경계 상자 비율로 영역을 읽는다.
        if (userUnits) {
            x = lengthFrom(r, filter, "x", fs, -0.1 * r.vpW, 0);
            y = lengthFrom(r, filter, "y", fs, -0.1 * r.vpH, 1);
            w = lengthFrom(r, filter, "width", fs, 1.2 * r.vpW, 0);
            h = lengthFrom(r, filter, "height", fs, 1.2 * r.vpH, 1);
        } else {
            x = bb.getX() + SvgMasks.fraction(SvgPaintServers.serverAttr(r, filter, "x", null), -0.1) * bb.getWidth();
            y = bb.getY() + SvgMasks.fraction(SvgPaintServers.serverAttr(r, filter, "y", null), -0.1) * bb.getHeight();
            w = SvgMasks.fraction(SvgPaintServers.serverAttr(r, filter, "width", null), 1.2) * bb.getWidth();
            h = SvgMasks.fraction(SvgPaintServers.serverAttr(r, filter, "height", null), 1.2) * bb.getHeight();
        }
        return !(w > 0) || !(h > 0) ? null : new Rectangle2D.Double(x, y, w, h);
    }

    /**
     * Finds the filter element along the href chain that has primitives.
     * <p>
     * href 연쇄에서 기본 요소를 가진 필터 요소를 찾는다.
     *
     * @param r      the renderer / 렌더러
     * @param filter the first filter / 첫 필터
     * @return the element holding the primitives, or null / 기본 요소를 가진 요소. 없으면 null
     */
    private static SvgElement filterChildren(SvgRenderer r, SvgElement filter) {
        SvgElement cur = filter;
        // 자식에 fe 요소가 있는 첫 필터를 href 를 따라 찾는다(최대 16단계).
        for (int depth = 0; cur != null && depth < 16; depth++) {
            for (int i = 0; i < cur.children.size(); i++) {
                SvgElement ch = cur.children.get(i);
                if (ch.isSvg() && ch.name.startsWith("fe")) {
                    return cur;
                }
            }
            SvgElement next = r.lookup(cur.href());
            if (next == null || !next.is("filter")) {
                return null;
            }
            cur = next;
        }
        return null;
    }

    /**
     * Reads a length attribute of an element, following the href chain of a filter.
     * <p>
     * 요소의 길이 속성을 읽는다(필터의 href 연쇄를 따라간다).
     *
     * @param r    the renderer / 렌더러
     * @param e    the filter element / 필터 요소
     * @param name the attribute name / 속성 이름
     * @param st   the style / 스타일
     * @param def  the default / 기본값
     * @param axis the axis / 축
     * @return pixels / 픽셀
     */
    private static double lengthFrom(SvgRenderer r, SvgElement e, String name, SvgStyle st, double def, int axis) {
        String v = SvgPaintServers.serverAttr(r, e, name, null);
        SvgLength l = v == null ? null : SvgLength.parse(v);
        return l == null ? def : st.px(l, axis);
    }

    /**
     * Resolves an input reference of a primitive.
     * <p>
     * 기본 요소의 입력 참조를 해석한다.
     *
     * @param c    the context / 문맥
     * @param p    the primitive / 기본 요소
     * @param attr <code>in</code> or <code>in2</code> / <code>in</code> 또는 <code>in2</code>
     * @param last the result of the previous primitive or null / 이전 기본 요소의 결과. 없으면 null
     * @return the input (may be shared, do not modify) / 입력(공유될 수 있으므로 바꾸지 않는다)
     */
    private static Img input(Ctx c, SvgElement p, String attr, Img last) {
        String in = p.attrs.get(attr);
        if (in != null) {
            in = in.trim();
            if ("SourceGraphic".equals(in)) {
                return c.source;
            }
            if ("SourceAlpha".equals(in)) {
                return alphaOnly(c.source);
            }
            if ("BackgroundImage".equals(in) || "BackgroundAlpha".equals(in) || "FillPaint".equals(in) || "StrokePaint".equals(in)) {
                return new Img(c.source.w, c.source.h);
            }
            Img named = c.named.get(in);
            if (named != null) {
                return named;
            }
        }
        return last != null ? last : c.source;
    }

    /**
     * Returns an image with the alpha channel of the source and black colour.
     * <p>
     * 원본의 알파 채널과 검정 색을 가진 이미지를 반환한다.
     *
     * @param s the source / 원본
     * @return the alpha image / 알파 이미지
     */
    private static Img alphaOnly(Img s) {
        Img o = new Img(s.w, s.h);
        o.sub = new Rectangle(s.sub);
        o.linear = s.linear;
        // 알파만 남기고 색은 검정으로 둔다.
        for (int i = 0; i < s.px.length; i++) {
            o.px[i] = s.px[i] & 0xFF000000;
        }
        return o;
    }

    /**
     * Evaluates one filter primitive.
     * <p>
     * 필터 기본 요소 하나를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive element / 기본 요소
     * @param ps     its style / 스타일
     * @param last   the previous result or null / 이전 결과. 없으면 null
     * @param linear true to compute in linearRGB / linearRGB 로 계산하면 true
     * @return the result, or null if the primitive is skipped / 결과. 건너뛰면 null
     */
    private static Img primitive(Ctx c, SvgElement p, SvgStyle ps, Img last, boolean linear) {
        String n = p.name;
        Img out;
        if ("feGaussianBlur".equals(n)) {
            double[] sd = SvgScanner.numberList(p.attrs.get("stdDeviation"));
            double sx = 0;
            double sy = 0;
            if (sd != null && sd.length >= 1) {
                sx = sd[0];
                sy = sd.length > 1 ? sd[1] : sd[0];
            }
            if (sx < 0 || sy < 0) {
                return null;
            }
            double[] s = scalePrim(c, sx, sy);
            out = gaussian(toSpace(input(c, p, "in", last), linear), s[0] * c.sx, s[1] * c.sy);
        } else if ("feOffset".equals(n)) {
            double[] d = scalePrim(c, num(p, "dx", 0), num(p, "dy", 0));
            out = offset(toSpace(input(c, p, "in", last), linear), (int) Math.round(d[0] * c.sx), (int) Math.round(d[1] * c.sy));
        } else if ("feFlood".equals(n)) {
            out = flood(c.source.w, c.source.h, floodColor(ps, "flood-color", "flood-opacity"), false);
            out.linear = false;
            out = toSpace(out, linear);
        } else if ("feMerge".equals(n)) {
            out = new Img(c.source.w, c.source.h);
            out.linear = linear;
            // feMergeNode 를 순서대로 겹쳐 쌓는다.
            for (int i = 0; i < p.children.size(); i++) {
                SvgElement node = p.children.get(i);
                if (node.is("feMergeNode")) {
                    out = over(toSpace(input(c, node, "in", last), linear), out);
                }
            }
        } else if ("feBlend".equals(n)) {
            String mode = p.attrs.get("mode") == null ? "normal" : p.attrs.get("mode").trim().toLowerCase(Locale.ENGLISH);
            Img a = toSpace(input(c, p, "in", last), linear);
            Img b = toSpace(input(c, p, "in2", last), linear);
            out = blendImgs(b, a, mode);
        } else if ("feComposite".equals(n)) {
            out = feComposite(c, p, last, linear);
        } else if ("feColorMatrix".equals(n)) {
            out = feColorMatrix(c, p, last, linear);
        } else if ("feComponentTransfer".equals(n)) {
            out = feComponentTransfer(c, p, last, linear);
        } else if ("feMorphology".equals(n)) {
            out = feMorphology(c, p, last, linear);
        } else if ("feConvolveMatrix".equals(n)) {
            out = feConvolve(c, p, last, linear);
        } else if ("feDisplacementMap".equals(n)) {
            out = feDisplacement(c, p, last, linear);
        } else if ("feTile".equals(n)) {
            out = feTile(c, p, last, linear);
        } else if ("feImage".equals(n)) {
            out = feImage(c, p, ps);
            if (out == null) {
                return null;
            }
            out = toSpace(out, linear);
        } else if ("feDropShadow".equals(n)) {
            double[] sd = SvgScanner.numberList(p.attrs.get("stdDeviation"));
            double sx = sd != null && sd.length >= 1 ? sd[0] : 2;
            double sy = sd != null && sd.length >= 2 ? sd[1] : sx;
            double[] d = scalePrim(c, num(p, "dx", 2), num(p, "dy", 2));
            double[] s = scalePrim(c, sx, sy);
            Img src = toSpace(input(c, p, "in", last), linear);
            Img fl = flood(src.w, src.h, floodColor(ps, "flood-color", "flood-opacity"), false);
            fl.linear = false;
            fl = toSpace(fl, linear);
            Img shadow = composite(fl, src, 1);
            shadow = offset(shadow, (int) Math.round(d[0] * c.sx), (int) Math.round(d[1] * c.sy));
            if (s[0] > 0 || s[1] > 0) {
                shadow = gaussian(shadow, s[0] * c.sx, s[1] * c.sy);
            }
            out = over(src, shadow);
        } else if ("feTurbulence".equals(n)) {
            out = new Img(c.source.w, c.source.h);
            int[] noise = SvgTurbulence.generate(out.w, out.h, p, new Rectangle(0, 0, out.w, out.h), c.ctm, c.userRegion);
            System.arraycopy(noise, 0, out.px, 0, noise.length);
            // 난류 값은 필터의 색 공간(linearRGB 또는 sRGB)의 값으로 해석한다.
            out.linear = linear;
        } else {
            return null;
        }
        out = restrictToSubregion(c, p, out);
        return out;
    }

    /**
     * Reads a number attribute with a default.
     * <p>
     * 기본값이 있는 수 속성을 읽는다.
     *
     * @param p    the element / 요소
     * @param name the attribute name / 속성 이름
     * @param def  the default / 기본값
     * @return the number / 수
     */
    private static double num(SvgElement p, String name, double def) {
        String v = p.attrs.get(name);
        if (v == null) {
            return def;
        }
        double[] l = SvgScanner.numberList(v);
        return l == null || l.length == 0 ? def : l[0];
    }

    /**
     * Scales primitive unit values to user space (for <code>primitiveUnits="objectBoundingBox"</code>).
     * <p>
     * 기본 요소 단위의 값을 사용자 공간으로 바꾼다(<code>primitiveUnits="objectBoundingBox"</code> 용).
     *
     * @param c the context / 문맥
     * @param a x value / x 값
     * @param b y value / y 값
     * @return the scaled pair / 바꾼 쌍
     */
    private static double[] scalePrim(Ctx c, double a, double b) {
        if (c.primObb && c.bbox != null) {
            return new double[] { a * c.bbox.getWidth(), b * c.bbox.getHeight() };
        }
        return new double[] { a, b };
    }

    /**
     * Reads the flood colour with its opacity as <code>0xAARRGGBB</code>.
     * <p>
     * 채움 색을 투명도와 함께 <code>0xAARRGGBB</code> 로 읽는다.
     *
     * @param ps    the style / 스타일
     * @param color the colour property / 색 속성
     * @param op    the opacity property / 투명도 속성
     * @return the colour / 색
     */
    private static int floodColor(SvgStyle ps, String color, String op) {
        String cv = ps.own(color);
        int rgb = 0xFF000000;
        if (cv != null) {
            if ("currentcolor".equalsIgnoreCase(cv)) {
                rgb = ps.color();
            } else {
                Integer pc = SvgColors.parse(cv);
                if (pc != null) {
                    rgb = pc.intValue();
                }
            }
        }
        double opacity = 1;
        String ov = ps.own(op);
        if (ov != null) {
            SvgLength l = SvgLength.parse(ov);
            if (l != null) {
                opacity = Math.max(0, Math.min(1, l.isPercent() ? l.value / 100 : l.value));
            }
        }
        int a = (int) Math.round((rgb >>> 24) * opacity);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * Restricts a primitive result to the primitive subregion (the filter region unless x, y, width and height say otherwise).
     * <p>
     * 기본 요소 결과를 하위 영역(x, y, width, height 가 따로 정하지 않으면 필터 영역)으로 제한한다.
     *
     * @param c   the context / 문맥
     * @param p   the primitive / 기본 요소
     * @param out the result / 결과
     * @return the restricted result / 제한된 결과
     */
    private static Img restrictToSubregion(Ctx c, SvgElement p, Img out) {
        Rectangle2D u = c.userRegion;
        double x = u.getX();
        double y = u.getY();
        double w = u.getWidth();
        double h = u.getHeight();
        SvgLength lx = SvgLength.parse(p.attrs.get("x"));
        SvgLength ly = SvgLength.parse(p.attrs.get("y"));
        SvgLength lw = SvgLength.parse(p.attrs.get("width"));
        SvgLength lh = SvgLength.parse(p.attrs.get("height"));
        // 단위가 객체 경계 상자이면 비율로 읽고 아니면 사용자 공간 길이로 읽는다.
        if (c.primObb && c.bbox != null) {
            Rectangle2D b = c.bbox;
            if (lx != null) {
                x = b.getX() + (lx.isPercent() ? lx.value / 100 : lx.value) * b.getWidth();
            }
            if (ly != null) {
                y = b.getY() + (ly.isPercent() ? ly.value / 100 : ly.value) * b.getHeight();
            }
            if (lw != null) {
                w = (lw.isPercent() ? lw.value / 100 : lw.value) * b.getWidth();
            }
            if (lh != null) {
                h = (lh.isPercent() ? lh.value / 100 : lh.value) * b.getHeight();
            }
        } else {
            SvgStyle st = c.style;
            if (lx != null) {
                x = st.px(lx, 0);
            }
            if (ly != null) {
                y = st.px(ly, 1);
            }
            if (lw != null) {
                w = st.px(lw, 0);
            }
            if (lh != null) {
                h = st.px(lh, 1);
            }
        }
        Rectangle2D dev = c.ctm.createTransformedShape(new Rectangle2D.Double(x, y, w, h)).getBounds2D();
        int x0 = (int) Math.floor(dev.getMinX() + 1e-6);
        int y0 = (int) Math.floor(dev.getMinY() + 1e-6);
        int x1 = (int) Math.ceil(dev.getMaxX() - 1e-6);
        int y1 = (int) Math.ceil(dev.getMaxY() - 1e-6);
        Rectangle r = new Rectangle(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)).intersection(new Rectangle(0, 0, out.w, out.h));
        if (r.isEmpty()) {
            r = new Rectangle(0, 0, 0, 0);
        }
        Img res = out.copy();
        // 하위 영역 밖의 픽셀을 지운다.
        for (int yy = 0; yy < res.h; yy++) {
            for (int xx = 0; xx < res.w; xx++) {
                if (xx < r.x || yy < r.y || xx >= r.x + r.width || yy >= r.y + r.height) {
                    res.px[yy * res.w + xx] = 0;
                }
            }
        }
        res.sub = r;
        return res;
    }

    /**
     * Creates an image filled with one colour (or with the colour only where another image has alpha, see {@link #composite}).
     * <p>
     * 한 색으로 채운 이미지를 만든다.
     *
     * @param w      width / 너비
     * @param h      height / 높이
     * @param argb   the colour (non-premultiplied) / 색(알파 미리 곱하지 않음)
     * @param linear unused marker (the colour is in sRGB) / 쓰지 않는 표시(색은 sRGB)
     * @return the image in sRGB / sRGB 이미지
     */
    private static Img flood(int w, int h, int argb, boolean linear) {
        Img o = new Img(w, h);
        int a = argb >>> 24;
        int r = (((argb >> 16) & 0xFF) * a + 127) / 255;
        int g = (((argb >> 8) & 0xFF) * a + 127) / 255;
        int b = ((argb & 0xFF) * a + 127) / 255;
        int p = (a << 24) | (r << 16) | (g << 8) | b;
        java.util.Arrays.fill(o.px, p);
        return o;
    }

    /**
     * Keeps the colour of <code>colour</code> weighted by the alpha of <code>mask</code> (the "in" operator).
     * <p>
     * <code>colour</code> 의 색을 <code>mask</code> 의 알파로 가중해 남긴다("in" 연산).
     *
     * @param colour the colour image / 색 이미지
     * @param mask   the mask image / 마스크 이미지
     * @param scale  extra factor / 추가 계수
     * @return the result / 결과
     */
    private static Img composite(Img colour, Img mask, double scale) {
        Img o = new Img(colour.w, colour.h);
        o.linear = colour.linear;
        // 마스크의 알파를 색 이미지의 모든 채널에 곱한다.
        for (int i = 0; i < o.px.length; i++) {
            int p = colour.px[i];
            int m = (int) Math.round((mask.px[i] >>> 24) * scale);
            if (m <= 0 || p == 0) {
                continue;
            }
            o.px[i] = mul(p, m);
        }
        return o;
    }

    /**
     * Multiplies all channels of a premultiplied pixel by a factor of 0..255.
     * <p>
     * 알파를 미리 곱한 픽셀의 모든 채널에 0..255 계수를 곱한다.
     *
     * @param p the pixel / 픽셀
     * @param m the factor / 계수
     * @return the product / 결과
     */
    private static int mul(int p, int m) {
        if (m >= 255) {
            return p;
        }
        int a = (((p >>> 24) * m) + 127) / 255;
        int r = ((((p >> 16) & 0xFF) * m) + 127) / 255;
        int g = ((((p >> 8) & 0xFF) * m) + 127) / 255;
        int b = (((p & 0xFF) * m) + 127) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Draws <code>top</code> over <code>bottom</code> (the "over" operator on premultiplied pixels).
     * <p>
     * <code>top</code> 을 <code>bottom</code> 위에 그린다(알파를 미리 곱한 픽셀의 "over" 연산).
     *
     * @param top    the upper image / 위 이미지
     * @param bottom the lower image / 아래 이미지
     * @return the result / 결과
     */
    private static Img over(Img top, Img bottom) {
        Img o = new Img(top.w, top.h);
        o.linear = top.linear;
        // 픽셀마다 위 이미지에 (1 - 위 알파) 만큼의 아래 이미지를 더한다.
        for (int i = 0; i < o.px.length; i++) {
            int t = top.px[i];
            int b = bottom.px[i];
            int ia = 255 - (t >>> 24);
            if (ia == 0) {
                o.px[i] = t;
                continue;
            }
            int a = Math.min(255, (t >>> 24) + (((b >>> 24) * ia + 127) / 255));
            int r = Math.min(255, ((t >> 16) & 0xFF) + ((((b >> 16) & 0xFF) * ia + 127) / 255));
            int g = Math.min(255, ((t >> 8) & 0xFF) + ((((b >> 8) & 0xFF) * ia + 127) / 255));
            int bl = Math.min(255, (t & 0xFF) + (((b & 0xFF) * ia + 127) / 255));
            o.px[i] = (a << 24) | (r << 16) | (g << 8) | bl;
        }
        return o;
    }

    /**
     * Shifts an image by whole pixels; the area that moves in is transparent.
     * <p>
     * 이미지를 정수 픽셀만큼 이동한다. 새로 들어오는 영역은 투명하다.
     *
     * @param in the image / 이미지
     * @param dx shift along x / x 이동
     * @param dy shift along y / y 이동
     * @return the shifted image / 이동한 이미지
     */
    private static Img offset(Img in, int dx, int dy) {
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        // 원본 행을 이동한 위치에 복사한다.
        for (int y = 0; y < in.h; y++) {
            int ty = y + dy;
            if (ty < 0 || ty >= in.h) {
                continue;
            }
            // 한 행의 픽셀을 차례로 처리한다.
            for (int x = 0; x < in.w; x++) {
                int tx = x + dx;
                if (tx >= 0 && tx < in.w) {
                    o.px[ty * in.w + tx] = in.px[y * in.w + x];
                }
            }
        }
        return o;
    }

    /**
     * Blurs an image with a Gaussian of the given standard deviations (box blurs for large values, as the specification allows).
     * <p>
     * 이미지를 주어진 표준 편차의 가우시안으로 흐린다(큰 값에는 규격이 허용하는 상자 흐림을 쓴다).
     *
     * @param in the image / 이미지
     * @param sx standard deviation along x in pixels / x 방향 표준 편차(픽셀)
     * @param sy standard deviation along y in pixels / y 방향 표준 편차(픽셀)
     * @return the blurred image / 흐린 이미지
     */
    private static Img gaussian(Img in, double sx, double sy) {
        // 여러 번의 상자 흐림이 가장자리 밖으로 퍼지는 것을 잃지 않도록 3σ 만큼 투명한 여백을 붙여 흐린 뒤 잘라 낸다.
        int px = sx > 0 ? (int) Math.min(1000, Math.ceil(sx * 3) + 2) : 0;
        int py = sy > 0 ? (int) Math.min(1000, Math.ceil(sy * 3) + 2) : 0;
        if ((long) (in.w + 2 * px) * (in.h + 2 * py) > 64L * 1000 * 1000) {
            px = 0;
            py = 0;
        }
        Img cur = new Img(in.w + 2 * px, in.h + 2 * py);
        cur.linear = in.linear;
        // 원본 행을 여백이 붙은 버퍼의 가운데에 복사한다.
        for (int y = 0; y < in.h; y++) {
            System.arraycopy(in.px, y * in.w, cur.px, (y + py) * cur.w + px, in.w);
        }
        if (sx > 0) {
            cur = blurAxis(cur, sx, true);
        }
        if (sy > 0) {
            cur = blurAxis(cur, sy, false);
        }
        Img out = new Img(in.w, in.h);
        out.linear = in.linear;
        // 여백을 잘라 낸 가운데를 결과로 쓴다.
        for (int y = 0; y < in.h; y++) {
            System.arraycopy(cur.px, (y + py) * cur.w + px, out.px, y * in.w, in.w);
        }
        return out;
    }

    /**
     * Blurs along one axis.
     * <p>
     * 한 축을 따라 흐린다.
     *
     * @param in    the image / 이미지
     * @param sigma the standard deviation / 표준 편차
     * @param horiz true for the x axis / x 축이면 true
     * @return the blurred image / 흐린 이미지
     */
    private static Img blurAxis(Img in, double sigma, boolean horiz) {
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        int len = horiz ? in.w : in.h;
        int lines = horiz ? in.h : in.w;
        int[] line = new int[len];
        int[] res = new int[len];
        int[] tmp = new int[len];
        // 줄마다 네 채널을 따로 흐린다.
        for (int l = 0; l < lines; l++) {
            for (int ch = 0; ch < 4; ch++) {
                int shift = ch * 8;
                // 줄의 픽셀 값을 읽거나 되쓴다.
                for (int i = 0; i < len; i++) {
                    int idx = horiz ? l * in.w + i : i * in.w + l;
                    line[i] = (in.px[idx] >>> shift) & 0xFF;
                }
                blurLine(line, res, tmp, sigma);
                // 줄의 픽셀 값을 읽거나 되쓴다.
                for (int i = 0; i < len; i++) {
                    int idx = horiz ? l * in.w + i : i * in.w + l;
                    o.px[idx] |= (res[i] & 0xFF) << shift;
                }
            }
        }
        return o;
    }

    /**
     * Blurs one line of 8 bit values.
     * <p>
     * 8비트 값 한 줄을 흐린다.
     *
     * @param src   the input / 입력
     * @param dst   the output (same length) / 출력(같은 길이)
     * @param tmp   scratch space of the same length / 같은 길이의 작업 공간
     * @param sigma the standard deviation / 표준 편차
     */
    private static void blurLine(int[] src, int[] dst, int[] tmp, double sigma) {
        int n = src.length;
        if (sigma < 2.0) {
            int rad = (int) Math.ceil(sigma * 3);
            double[] k = new double[rad * 2 + 1];
            double sum = 0;
            // 가우시안 커널을 만들고 합이 1 이 되게 정규화한다.
            for (int i = -rad; i <= rad; i++) {
                k[i + rad] = Math.exp(-(i * i) / (2 * sigma * sigma));
                sum += k[i + rad];
            }
            // 모든 항목을 차례로 처리한다.
            for (int i = 0; i < n; i++) {
                double acc = 0;
                // 커널의 모든 칸에 대해 가중합을 더한다.
                for (int j = -rad; j <= rad; j++) {
                    int p = i + j;
                    if (p >= 0 && p < n) {
                        acc += src[p] * k[j + rad];
                    }
                }
                dst[i] = (int) Math.round(acc / sum);
            }
            return;
        }
        int d = (int) Math.floor(sigma * 3 * Math.sqrt(2 * Math.PI) / 4 + 0.5);
        if (d < 1) {
            d = 1;
        }
        if (d % 2 == 1) {
            // 홀수 d: 가운데 정렬 상자 흐림 세 번.
            box(src, dst, d, d / 2);
            box(dst, tmp, d, d / 2);
            box(tmp, dst, d, d / 2);
        } else {
            // 짝수 d: 왼쪽/오른쪽으로 어긋난 상자 두 번과 크기 d+1 의 가운데 상자 한 번.
            box(src, dst, d, d / 2);
            box(dst, tmp, d, d / 2 - 1);
            box(tmp, dst, d + 1, d / 2);
        }
    }

    /**
     * Applies a box blur of a size with the given number of pixels to the left of the centre.
     * <p>
     * 지정한 크기의 상자 흐림을 적용한다. 중심 왼쪽 픽셀 수를 지정한다.
     *
     * @param src  the input / 입력
     * @param dst  the output / 출력
     * @param size the box size / 상자 크기
     * @param left the number of pixels left of the output pixel that the box covers / 상자가 출력 픽셀의 왼쪽으로 덮는 픽셀 수
     */
    private static void box(int[] src, int[] dst, int size, int left) {
        int n = src.length;
        long sum = 0;
        // 첫 출력 픽셀의 창 합을 구한다.
        for (int j = -left; j < size - left; j++) {
            if (j >= 0 && j < n) {
                sum += src[j];
            }
        }
        // 모든 항목을 차례로 처리한다.
        for (int i = 0; i < n; i++) {
            dst[i] = (int) ((sum + size / 2) / size);
            int out = i - left;
            int in = i + size - left;
            if (out >= 0 && out < n) {
                sum -= src[out];
            }
            if (in >= 0 && in < n) {
                sum += src[in];
            }
        }
    }

    /**
     * Applies a 4x5 colour matrix to the unpremultiplied colours of an image (in its own colour space).
     * <p>
     * 이미지의 알파를 곱하지 않은 색에 4x5 색 행렬을 적용한다(이미지 자신의 색 공간에서).
     *
     * @param in the image / 이미지
     * @param m  the 20 matrix values / 20개의 행렬 값
     * @return the result / 결과
     */
    private static Img colorMatrix(Img in, double[] m) {
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        o.sub = new Rectangle(0, 0, in.w, in.h);
        // 픽셀마다 알파를 나눈 색에 행렬을 곱하고 다시 알파를 곱한다.
        for (int i = 0; i < in.px.length; i++) {
            int p = in.px[i];
            int a = p >>> 24;
            double r = 0;
            double g = 0;
            double b = 0;
            if (a > 0) {
                r = Math.min(1, ((p >> 16) & 0xFF) / (double) a);
                g = Math.min(1, ((p >> 8) & 0xFF) / (double) a);
                b = Math.min(1, (p & 0xFF) / (double) a);
            }
            double af = a / 255.0;
            double nr = m[0] * r + m[1] * g + m[2] * b + m[3] * af + m[4];
            double ng = m[5] * r + m[6] * g + m[7] * b + m[8] * af + m[9];
            double nb = m[10] * r + m[11] * g + m[12] * b + m[13] * af + m[14];
            double na = m[15] * r + m[16] * g + m[17] * b + m[18] * af + m[19];
            o.px[i] = pack(nr, ng, nb, na);
        }
        return o;
    }

    /**
     * Packs unpremultiplied colour components of 0..1 into a premultiplied pixel (values are clamped).
     * <p>
     * 알파를 곱하지 않은 0..1 색 성분을 알파를 미리 곱한 픽셀로 묶는다(값은 제한된다).
     *
     * @param r red / 빨강
     * @param g green / 초록
     * @param b blue / 파랑
     * @param a alpha / 알파
     * @return the pixel / 픽셀
     */
    private static int pack(double r, double g, double b, double a) {
        double aa = Math.max(0, Math.min(1, a));
        int ai = (int) Math.round(aa * 255);
        int ri = (int) Math.round(Math.max(0, Math.min(1, r)) * aa * 255);
        int gi = (int) Math.round(Math.max(0, Math.min(1, g)) * aa * 255);
        int bi = (int) Math.round(Math.max(0, Math.min(1, b)) * aa * 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    /**
     * Evaluates <code>feColorMatrix</code>.
     * <p>
     * <code>feColorMatrix</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feColorMatrix(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        String type = p.attrs.get("type");
        double[] vals = SvgScanner.numberList(p.attrs.get("values"));
        double[] m = { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0 };
        if (type == null || "matrix".equals(type.trim())) {
            if (vals != null && vals.length == 20) {
                m = vals;
            }
        } else if ("saturate".equals(type.trim())) {
            m = saturateMatrix(vals != null && vals.length == 1 ? vals[0] : 1);
        } else if ("hueRotate".equals(type.trim())) {
            m = hueMatrix(vals != null && vals.length == 1 ? vals[0] : 0);
        } else if ("luminanceToAlpha".equals(type.trim())) {
            m = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.2125, 0.7154, 0.0721, 0, 0 };
        }
        return colorMatrix(in, m);
    }

    /**
     * Evaluates <code>feComponentTransfer</code>.
     * <p>
     * <code>feComponentTransfer</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feComponentTransfer(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        SvgElement[] f = new SvgElement[4];
        // feFuncR/G/B/A 를 찾는다(같은 채널이 여럿이면 마지막이 이긴다).
        for (int i = 0; i < p.children.size(); i++) {
            SvgElement ch = p.children.get(i);
            if (ch.is("feFuncR")) {
                f[0] = ch;
            } else if (ch.is("feFuncG")) {
                f[1] = ch;
            } else if (ch.is("feFuncB")) {
                f[2] = ch;
            } else if (ch.is("feFuncA")) {
                f[3] = ch;
            }
        }
        double[][] lut = new double[4][];
        // 채널마다 256 단계의 변환 표를 만든다.
        for (int k = 0; k < 4; k++) {
            lut[k] = transferTable(f[k]);
        }
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        // 모든 픽셀에 대해 같은 연산을 적용한다.
        for (int i = 0; i < in.px.length; i++) {
            int px = in.px[i];
            int a = px >>> 24;
            double r = 0;
            double g = 0;
            double b = 0;
            if (a > 0) {
                r = Math.min(255, (((px >> 16) & 0xFF) * 255 + a / 2) / a);
                g = Math.min(255, (((px >> 8) & 0xFF) * 255 + a / 2) / a);
                b = Math.min(255, ((px & 0xFF) * 255 + a / 2) / a);
            }
            double nr = lut[0] == null ? r / 255 : lut[0][(int) r];
            double ng = lut[1] == null ? g / 255 : lut[1][(int) g];
            double nb = lut[2] == null ? b / 255 : lut[2][(int) b];
            double na = lut[3] == null ? a / 255.0 : lut[3][a];
            o.px[i] = pack(nr, ng, nb, na);
        }
        return o;
    }

    /**
     * Builds the table of one transfer function (<code>identity</code> returns null).
     * <p>
     * 전달 함수 하나의 표를 만든다(<code>identity</code> 이면 null).
     *
     * @param f the feFunc element or null / feFunc 요소. 없으면 null
     * @return 256 values of 0..1, or null / 0..1 의 값 256개. 없으면 null
     */
    private static double[] transferTable(SvgElement f) {
        if (f == null) {
            return null;
        }
        String type = f.attrs.get("type");
        if (type == null) {
            return null;
        }
        double[] t = new double[256];
        String ty = type.trim();
        if ("table".equals(ty) || "discrete".equals(ty)) {
            double[] v = SvgScanner.numberList(f.attrs.get("tableValues"));
            if (v == null || v.length == 0) {
                return null;
            }
            int n = v.length;
            // 입력 값마다 표에서 값을 고르거나 선형 보간한다.
            for (int i = 0; i < 256; i++) {
                double c = i / 255.0;
                if ("discrete".equals(ty)) {
                    int k = (int) Math.min(n - 1, Math.floor(c * n));
                    t[i] = v[k];
                } else if (n == 1) {
                    t[i] = v[0];
                } else {
                    int k = (int) Math.min(n - 2, Math.floor(c * (n - 1)));
                    t[i] = v[k] + (c - k / (double) (n - 1)) * (n - 1) * (v[k + 1] - v[k]);
                }
            }
            return t;
        }
        if ("linear".equals(ty)) {
            double slope = num(f, "slope", 1);
            double icpt = num(f, "intercept", 0);
            // 0부터 255까지 입력 값마다 표를 채운다.
            for (int i = 0; i < 256; i++) {
                t[i] = slope * (i / 255.0) + icpt;
            }
            return t;
        }
        if ("gamma".equals(ty)) {
            double amp = num(f, "amplitude", 1);
            double exp = num(f, "exponent", 1);
            double off = num(f, "offset", 0);
            // 0부터 255까지 입력 값마다 표를 채운다.
            for (int i = 0; i < 256; i++) {
                t[i] = amp * Math.pow(i / 255.0, exp) + off;
            }
            return t;
        }
        return null;
    }

    /**
     * Evaluates <code>feComposite</code>.
     * <p>
     * <code>feComposite</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feComposite(Ctx c, SvgElement p, Img last, boolean linear) {
        Img a = toSpace(input(c, p, "in", last), linear);
        Img b = toSpace(input(c, p, "in2", last), linear);
        String op = p.attrs.get("operator") == null ? "over" : p.attrs.get("operator").trim();
        Img o = new Img(a.w, a.h);
        o.linear = linear;
        double k1 = num(p, "k1", 0);
        double k2 = num(p, "k2", 0);
        double k3 = num(p, "k3", 0);
        double k4 = num(p, "k4", 0);
        // 픽셀마다 선택한 연산자의 식을 알파를 미리 곱한 값에 적용한다.
        for (int i = 0; i < o.px.length; i++) {
            int pa = a.px[i];
            int pb = b.px[i];
            int aa = pa >>> 24;
            int ba = pb >>> 24;
            if ("arithmetic".equals(op)) {
                int res = 0;
                double ra = Math.max(0, Math.min(1, k1 * (aa / 255.0) * (ba / 255.0) + k2 * (aa / 255.0) + k3 * (ba / 255.0) + k4));
                int shift = 16;
                int rch = 0;
                int[] chan = new int[3];
                // 세 색 채널을 차례로 계산한다.
                for (int ch = 0; ch < 3; ch++) {
                    double av = ((pa >> shift) & 0xFF) / 255.0;
                    double bv = ((pb >> shift) & 0xFF) / 255.0;
                    double v = k1 * av * bv + k2 * av + k3 * bv + k4;
                    v = Math.max(0, Math.min(ra, v));
                    chan[ch] = (int) Math.round(v * 255);
                    shift -= 8;
                }
                res = ((int) Math.round(ra * 255) << 24) | (chan[0] << 16) | (chan[1] << 8) | chan[2];
                o.px[i] = res + rch;
                continue;
            }
            int fa;
            int fb;
            if ("in".equals(op)) {
                fa = ba;
                fb = 0;
            } else if ("out".equals(op)) {
                fa = 255 - ba;
                fb = 0;
            } else if ("atop".equals(op)) {
                fa = ba;
                fb = 255 - aa;
            } else if ("xor".equals(op)) {
                fa = 255 - ba;
                fb = 255 - aa;
            } else {
                fa = 255;
                fb = 255 - aa;
            }
            int ra = Math.min(255, ((aa * fa + 127) / 255) + ((ba * fb + 127) / 255));
            int rr = Math.min(255, ((((pa >> 16) & 0xFF) * fa + 127) / 255) + ((((pb >> 16) & 0xFF) * fb + 127) / 255));
            int rg = Math.min(255, ((((pa >> 8) & 0xFF) * fa + 127) / 255) + ((((pb >> 8) & 0xFF) * fb + 127) / 255));
            int rb = Math.min(255, (((pa & 0xFF) * fa + 127) / 255) + (((pb & 0xFF) * fb + 127) / 255));
            o.px[i] = (ra << 24) | (rr << 16) | (rg << 8) | rb;
        }
        return o;
    }

    /**
     * Blends the source image over a backdrop image with a blend mode (both premultiplied).
     * <p>
     * 원본 이미지를 배경 이미지 위에 혼합 모드로 혼합한다(둘 다 알파를 미리 곱함).
     *
     * @param backdrop the backdrop / 배경
     * @param source   the source / 원본
     * @param mode     the lower-case blend mode / 소문자 혼합 모드
     * @return the result / 결과
     */
    private static Img blendImgs(Img backdrop, Img source, String mode) {
        Img o = new Img(source.w, source.h);
        o.linear = source.linear;
        // 픽셀마다 혼합 식을 적용한다.
        for (int i = 0; i < o.px.length; i++) {
            o.px[i] = blendPixel(backdrop.px[i], source.px[i], mode, 1);
        }
        return o;
    }

    /**
     * Blends one source pixel onto a backdrop pixel (premultiplied) following the compositing specification.
     * <p>
     * 합성 규격에 따라 원본 픽셀 하나를 배경 픽셀(알파 미리 곱함)에 혼합한다.
     *
     * @param b     the backdrop pixel / 배경 픽셀
     * @param s     the source pixel / 원본 픽셀
     * @param mode  the blend mode / 혼합 모드
     * @param alpha the opacity of the source / 원본의 투명도
     * @return the result pixel / 결과 픽셀
     */
    private static int blendPixel(int b, int s, String mode, double alpha) {
        int sa8 = (int) Math.round((s >>> 24) * alpha);
        int ba8 = b >>> 24;
        if (sa8 == 0) {
            return b;
        }
        double sa = sa8 / 255.0;
        double ba = ba8 / 255.0;
        double ao = sa + ba - sa * ba;
        int out = 0;
        int shift = 16;
        // 세 색 채널마다 분리형 혼합 식을 적용한다.
        for (int ch = 0; ch < 3; ch++) {
            double cs = (s >>> 24) == 0 ? 0 : Math.min(1, ((s >> shift) & 0xFF) / (double) (s >>> 24));
            double cb = ba8 == 0 ? 0 : Math.min(1, ((b >> shift) & 0xFF) / (double) ba8);
            double mixed = blendFunction(mode, cb, cs);
            double co = sa * (1 - ba) * cs + sa * ba * mixed + (1 - sa) * ba * cb;
            int v = (int) Math.round(Math.max(0, Math.min(1, co)) * 255);
            out |= v << shift;
            shift -= 8;
        }
        return ((int) Math.round(Math.min(1, ao) * 255) << 24) | out;
    }

    /**
     * The separable blend functions of the compositing specification for one channel.
     * <p>
     * 합성 규격의 분리형 혼합 함수(채널 하나).
     *
     * @param mode the mode / 모드
     * @param cb   the backdrop component / 배경 성분
     * @param cs   the source component / 원본 성분
     * @return the blended component / 혼합된 성분
     */
    private static double blendFunction(String mode, double cb, double cs) {
        if ("multiply".equals(mode)) {
            return cb * cs;
        }
        if ("screen".equals(mode)) {
            return cb + cs - cb * cs;
        }
        if ("overlay".equals(mode)) {
            return blendFunction("hard-light", cs, cb);
        }
        if ("darken".equals(mode)) {
            return Math.min(cb, cs);
        }
        if ("lighten".equals(mode)) {
            return Math.max(cb, cs);
        }
        if ("color-dodge".equals(mode)) {
            if (cb == 0) {
                return 0;
            }
            return cs >= 1 ? 1 : Math.min(1, cb / (1 - cs));
        }
        if ("color-burn".equals(mode)) {
            if (cb >= 1) {
                return 1;
            }
            return cs <= 0 ? 0 : 1 - Math.min(1, (1 - cb) / cs);
        }
        if ("hard-light".equals(mode)) {
            return cs <= 0.5 ? cb * 2 * cs : cb + (2 * cs - 1) - cb * (2 * cs - 1);
        }
        if ("soft-light".equals(mode)) {
            if (cs <= 0.5) {
                return cb - (1 - 2 * cs) * cb * (1 - cb);
            }
            double d = cb <= 0.25 ? ((16 * cb - 12) * cb + 4) * cb : Math.sqrt(cb);
            return cb + (2 * cs - 1) * (d - cb);
        }
        if ("difference".equals(mode)) {
            return Math.abs(cb - cs);
        }
        if ("exclusion".equals(mode)) {
            return cb + cs - 2 * cb * cs;
        }
        return cs;
    }

    /**
     * Blends a layer onto an image with a blend mode (used by <code>mix-blend-mode</code>); the result is written into the image.
     * <p>
     * 레이어를 이미지에 혼합 모드로 혼합한다(<code>mix-blend-mode</code> 용). 결과는 이미지에 쓴다.
     *
     * @param dst   the image that receives the result (premultiplied) / 결과를 받는 이미지(알파 미리 곱함)
     * @param src   the layer (premultiplied) / 레이어(알파 미리 곱함)
     * @param mode  the lower-case blend mode / 소문자 혼합 모드
     * @param alpha the opacity of the layer / 레이어의 투명도
     */
    static void blendLayer(BufferedImage dst, BufferedImage src, String mode, double alpha) {
        int[] d = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        int[] s = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int n = Math.min(d.length, s.length);
        // 비어 있지 않은 레이어 픽셀마다 배경과 혼합한다.
        for (int i = 0; i < n; i++) {
            if (s[i] != 0) {
                d[i] = blendPixel(d[i], s[i], mode, alpha);
            }
        }
    }

    /**
     * Evaluates <code>feMorphology</code>.
     * <p>
     * <code>feMorphology</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feMorphology(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        double[] rad = SvgScanner.numberList(p.attrs.get("radius"));
        double rx = rad != null && rad.length >= 1 ? rad[0] : 0;
        double ry = rad != null && rad.length >= 2 ? rad[1] : rx;
        double[] sc = scalePrim(c, rx, ry);
        int ix = (int) Math.min(100, Math.round(sc[0] * c.sx));
        int iy = (int) Math.min(100, Math.round(sc[1] * c.sy));
        boolean dilate = "dilate".equals(p.attrs.get("operator"));
        if (rx < 0 || ry < 0 || (rx == 0 && ry == 0)) {
            return new Img(in.w, in.h);
        }
        Img cur = in;
        if (ix > 0) {
            cur = morphAxis(cur, ix, dilate, true);
        }
        if (iy > 0) {
            cur = morphAxis(cur, iy, dilate, false);
        }
        return cur;
    }

    /**
     * Erodes or dilates along one axis (per channel minimum or maximum over the window).
     * <p>
     * 한 축을 따라 침식 또는 팽창한다(창 안에서 채널별 최소 또는 최대).
     *
     * @param in     the image / 이미지
     * @param r      the radius / 반지름
     * @param dilate true for dilate / 팽창이면 true
     * @param horiz  true for the x axis / x 축이면 true
     * @return the result / 결과
     */
    private static Img morphAxis(Img in, int r, boolean dilate, boolean horiz) {
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        // 픽셀마다 창 안의 채널별 최소 또는 최대를 구한다(창 밖은 투명으로 본다).
        for (int y = 0; y < in.h; y++) {
            for (int x = 0; x < in.w; x++) {
                int res = dilate ? 0 : 0xFFFFFFFF;
                // 창 안의 모든 위치를 살펴 최소 또는 최대를 구한다.
                for (int k = -r; k <= r; k++) {
                    int xx = horiz ? x + k : x;
                    int yy = horiz ? y : y + k;
                    int px = (xx < 0 || yy < 0 || xx >= in.w || yy >= in.h) ? 0 : in.px[yy * in.w + xx];
                    res = dilate ? maxChannels(res, px) : minChannels(res, px);
                }
                o.px[y * in.w + x] = res;
            }
        }
        return o;
    }

    /**
     * Per channel maximum of two pixels.
     * <p>
     * 두 픽셀의 채널별 최대.
     *
     * @param a first / 첫째
     * @param b second / 둘째
     * @return maximum / 최대
     */
    private static int maxChannels(int a, int b) {
        int r = 0;
        // 네 채널을 따로 비교한다.
        for (int s = 0; s < 32; s += 8) {
            r |= Math.max((a >>> s) & 0xFF, (b >>> s) & 0xFF) << s;
        }
        return r;
    }

    /**
     * Per channel minimum of two pixels.
     * <p>
     * 두 픽셀의 채널별 최소.
     *
     * @param a first / 첫째
     * @param b second / 둘째
     * @return minimum / 최소
     */
    private static int minChannels(int a, int b) {
        int r = 0;
        // 네 채널을 따로 비교한다.
        for (int s = 0; s < 32; s += 8) {
            r |= Math.min((a >>> s) & 0xFF, (b >>> s) & 0xFF) << s;
        }
        return r;
    }

    /**
     * Evaluates <code>feConvolveMatrix</code>.
     * <p>
     * <code>feConvolveMatrix</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feConvolve(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        double[] ord = SvgScanner.numberList(p.attrs.get("order"));
        int ox = ord != null && ord.length >= 1 ? (int) ord[0] : 3;
        int oy = ord != null && ord.length >= 2 ? (int) ord[1] : ox;
        double[] kernel = SvgScanner.numberList(p.attrs.get("kernelMatrix"));
        if (ox < 1 || oy < 1 || ox * oy > 400 || kernel == null || kernel.length != ox * oy) {
            return new Img(in.w, in.h);
        }
        double sum = 0;
        // 커널 합으로 기본 divisor 를 구한다.
        for (int i = 0; i < kernel.length; i++) {
            sum += kernel[i];
        }
        double div = num(p, "divisor", 0);
        if (div == 0) {
            div = sum == 0 ? 1 : sum;
        }
        double bias = num(p, "bias", 0);
        int tx = (int) num(p, "targetX", ox / 2);
        int ty = (int) num(p, "targetY", oy / 2);
        if (tx < 0 || tx >= ox || ty < 0 || ty >= oy) {
            return new Img(in.w, in.h);
        }
        String edge = p.attrs.get("edgeMode") == null ? "duplicate" : p.attrs.get("edgeMode").trim();
        boolean preserveAlpha = "true".equals(p.attrs.get("preserveAlpha"));
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        // 모든 행을 차례로 처리한다.
        for (int y = 0; y < in.h; y++) {
            for (int x = 0; x < in.w; x++) {
                double[] acc = new double[4];
                // 커널 칸마다 원본 픽셀(가장자리는 edgeMode 에 따름)에 곱해 더한다.
                for (int j = 0; j < oy; j++) {
                    for (int i = 0; i < ox; i++) {
                        int sx = x - tx + i;
                        int sy = y - ty + j;
                        double kv = kernel[(oy - 1 - j) * ox + (ox - 1 - i)];
                        int px = fetch(in, sx, sy, edge);
                        acc[0] += kv * ((px >> 16) & 0xFF);
                        acc[1] += kv * ((px >> 8) & 0xFF);
                        acc[2] += kv * (px & 0xFF);
                        acc[3] += kv * (px >>> 24);
                    }
                }
                int orig = in.px[y * in.w + x];
                double a = preserveAlpha ? (orig >>> 24) / 255.0 : (acc[3] / div / 255.0 + bias);
                double r = acc[0] / div / 255.0 + bias * a;
                double g = acc[1] / div / 255.0 + bias * a;
                double b = acc[2] / div / 255.0 + bias * a;
                a = Math.max(0, Math.min(1, a));
                r = Math.max(0, Math.min(a, r));
                g = Math.max(0, Math.min(a, g));
                b = Math.max(0, Math.min(a, b));
                o.px[y * in.w + x] = ((int) Math.round(a * 255) << 24) | ((int) Math.round(r * 255) << 16) | ((int) Math.round(g * 255) << 8)
                        | (int) Math.round(b * 255);
            }
        }
        return o;
    }

    /**
     * Reads a pixel with an edge mode for positions outside the image.
     * <p>
     * 이미지 밖의 위치에 대한 가장자리 모드로 픽셀을 읽는다.
     *
     * @param in   the image / 이미지
     * @param x    x / x
     * @param y    y / y
     * @param edge <code>duplicate</code>, <code>wrap</code> or <code>none</code> / <code>duplicate</code>, <code>wrap</code>, <code>none</code>
     * @return the pixel / 픽셀
     */
    private static int fetch(Img in, int x, int y, String edge) {
        if (x >= 0 && y >= 0 && x < in.w && y < in.h) {
            return in.px[y * in.w + x];
        }
        if ("wrap".equals(edge)) {
            int xx = ((x % in.w) + in.w) % in.w;
            int yy = ((y % in.h) + in.h) % in.h;
            return in.px[yy * in.w + xx];
        }
        if ("none".equals(edge)) {
            return 0;
        }
        int xx = Math.max(0, Math.min(in.w - 1, x));
        int yy = Math.max(0, Math.min(in.h - 1, y));
        return in.px[yy * in.w + xx];
    }

    /**
     * Evaluates <code>feDisplacementMap</code>.
     * <p>
     * <code>feDisplacementMap</code> 를 평가한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feDisplacement(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        Img map = toSpace(input(c, p, "in2", last), linear);
        double[] sc = scalePrim(c, num(p, "scale", 0), num(p, "scale", 0));
        int xs = channelIndex(p.attrs.get("xChannelSelector"));
        int ys = channelIndex(p.attrs.get("yChannelSelector"));
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        // 모든 행을 차례로 처리한다.
        for (int y = 0; y < in.h; y++) {
            // 한 행의 픽셀을 차례로 처리한다.
            for (int x = 0; x < in.w; x++) {
                int m = map.px[y * in.w + x];
                int a = m >>> 24;
                double vx = 0.5;
                double vy = 0.5;
                // 변위 지도는 알파를 나눈 색 채널을 쓴다.
                if (a > 0) {
                    vx = Math.min(1, channelValue(m, xs) / (double) a);
                    vy = Math.min(1, channelValue(m, ys) / (double) a);
                } else if (xs == 3 || ys == 3) {
                    vx = xs == 3 ? 0 : 0.5;
                    vy = ys == 3 ? 0 : 0.5;
                }
                int sx = (int) Math.round(x + sc[0] * c.sx * (vx - 0.5));
                int sy = (int) Math.round(y + sc[1] * c.sy * (vy - 0.5));
                if (sx >= 0 && sy >= 0 && sx < in.w && sy < in.h) {
                    o.px[y * in.w + x] = in.px[sy * in.w + sx];
                }
            }
        }
        return o;
    }

    /**
     * Maps a channel selector letter to an index (0 red, 1 green, 2 blue, 3 alpha; default alpha).
     * <p>
     * 채널 선택 문자를 인덱스로 바꾼다(0 빨강, 1 초록, 2 파랑, 3 알파, 기본 알파).
     *
     * @param s the selector / 선택자
     * @return the index / 인덱스
     */
    private static int channelIndex(String s) {
        if ("R".equals(s)) {
            return 0;
        }
        if ("G".equals(s)) {
            return 1;
        }
        if ("B".equals(s)) {
            return 2;
        }
        return 3;
    }

    /**
     * Reads a premultiplied channel by index.
     * <p>
     * 인덱스로 알파를 미리 곱한 채널을 읽는다.
     *
     * @param px  the pixel / 픽셀
     * @param idx 0 red, 1 green, 2 blue, 3 alpha / 0 빨강, 1 초록, 2 파랑, 3 알파
     * @return the value / 값
     */
    private static int channelValue(int px, int idx) {
        return idx == 3 ? px >>> 24 : (px >> (16 - 8 * idx)) & 0xFF;
    }

    /**
     * Evaluates <code>feTile</code>: the subregion of the input is repeated over the whole region.
     * <p>
     * <code>feTile</code> 을 평가한다: 입력의 하위 영역을 영역 전체에 반복한다.
     *
     * @param c      the context / 문맥
     * @param p      the primitive / 기본 요소
     * @param last   the previous result / 이전 결과
     * @param linear true for linearRGB / linearRGB 이면 true
     * @return the result / 결과
     */
    private static Img feTile(Ctx c, SvgElement p, Img last, boolean linear) {
        Img in = toSpace(input(c, p, "in", last), linear);
        Rectangle t = in.sub;
        Img o = new Img(in.w, in.h);
        o.linear = in.linear;
        if (t.width <= 0 || t.height <= 0) {
            return o;
        }
        // 입력의 하위 영역을 기준으로 모든 픽셀의 원본 위치를 구한다.
        for (int y = 0; y < o.h; y++) {
            for (int x = 0; x < o.w; x++) {
                int sx = t.x + (((x - t.x) % t.width) + t.width) % t.width;
                int sy = t.y + (((y - t.y) % t.height) + t.height) % t.height;
                o.px[y * o.w + x] = in.px[sy * in.w + sx];
            }
        }
        return o;
    }

    /**
     * Evaluates <code>feImage</code> for data URIs and references to elements of the document; external addresses are not loaded.
     * <p>
     * data URI 와 문서 요소 참조에 대해 <code>feImage</code> 를 평가한다. 외부 주소는 읽지 않는다.
     *
     * @param c  the context / 문맥
     * @param p  the primitive / 기본 요소
     * @param ps its style / 스타일
     * @return the image in sRGB, or null / sRGB 이미지. 없으면 null
     */
    private static Img feImage(Ctx c, SvgElement p, SvgStyle ps) {
        String href = p.href();
        if (href == null) {
            return null;
        }
        String h = href.trim();
        Img out = new Img(c.source.w, c.source.h);
        Rectangle2D area = c.userRegion;
        SvgLength lx = SvgLength.parse(p.attrs.get("x"));
        SvgLength ly = SvgLength.parse(p.attrs.get("y"));
        SvgLength lw = SvgLength.parse(p.attrs.get("width"));
        SvgLength lh = SvgLength.parse(p.attrs.get("height"));
        double x = lx != null ? c.style.px(lx, 0) : area.getX();
        double y = ly != null ? c.style.px(ly, 1) : area.getY();
        double w = lw != null ? c.style.px(lw, 0) : area.getWidth();
        double hh = lh != null ? c.style.px(lh, 1) : area.getHeight();
        if (h.startsWith("#")) {
            SvgElement ref = c.r.lookup(h);
            if (ref == null || !SvgRenderer.isGraphicalName(ref.name) || c.r.active.contains(ref)) {
                return null;
            }
            BufferedImage tmp = new BufferedImage(c.r.canvasW, c.r.canvasH, BufferedImage.TYPE_INT_ARGB_PRE);
            SvgRenderer.Layer l = c.r.redirect(tmp, c.ctm);
            c.r.active.add(ref);
            try {
                c.r.g.setClip(null);
                c.r.g.translate(x, y);
                c.r.renderElement(ref, c.r.styleFor(ref.parent));
            } finally {
                c.r.active.remove(ref);
                c.r.endLayer(l);
            }
            int[] data = ((DataBufferInt) tmp.getRaster().getDataBuffer()).getData();
            return extract(data, tmp.getWidth(), new Rectangle(0, 0, tmp.getWidth(), tmp.getHeight()));
        }
        byte[] data = SvgRenderer.dataUri(h);
        if (data == null) {
            return null;
        }
        DecodedImage di;
        try {
            di = ImageDecoders.decode(data, null, c.r.limits());
        } catch (MediaException e) {
            return null;
        }
        BufferedImage bi = new BufferedImage(di.getWidth(), di.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] bp = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        System.arraycopy(di.getFrames().get(0).getArgb(), 0, bp, 0, bp.length);
        BufferedImage tmp = new BufferedImage(c.r.canvasW, c.r.canvasH, BufferedImage.TYPE_INT_ARGB_PRE);
        SvgRenderer.Layer l = c.r.redirect(tmp, c.ctm);
        try {
            c.r.g.setClip(null);
            AffineTransform fit = SvgRenderer.viewBoxTransform(0, 0, di.getWidth(), di.getHeight(), p.attrs.get("preserveAspectRatio"), w, hh);
            c.r.g.translate(x, y);
            c.r.g.transform(fit);
            c.r.g.drawImage(bi, 0, 0, null);
        } finally {
            c.r.endLayer(l);
        }
        int[] data2 = ((DataBufferInt) tmp.getRaster().getDataBuffer()).getData();
        out = extract(data2, tmp.getWidth(), new Rectangle(0, 0, tmp.getWidth(), tmp.getHeight()));
        return out;
    }
}
