package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;
import org.duckdns.hjow.swingwebpanel.css.values.CssUrl;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.media.image.svg.SvgReference;

/**
 * The parsed value of the <code>filter</code> property (CSS Filter Effects Module Level 1, https://www.w3.org/TR/filter-effects-1/, Working
 * Draft 2018-12-18, retrieved 2026-11-17, section 6 "Filter functions" and section 13.1 "Filter functions in SVG terms"): the list of the filter functions
 * <code>blur()</code>, <code>brightness()</code>, <code>contrast()</code>, <code>drop-shadow()</code>, <code>grayscale()</code>,
 * <code>hue-rotate()</code>, <code>invert()</code>, <code>opacity()</code>, <code>saturate()</code> and <code>sepia()</code> with their
 * amounts, in the order of the declaration (the first function takes the element as input, the next ones take the output of the previous
 * one). The amounts are already clamped the way the specification requires (<code>grayscale()</code>, <code>invert()</code>,
 * <code>opacity()</code> and <code>sepia()</code> to at most 1). A <code>url(#id)</code> reference to an SVG <code>filter</code> element of the same
 * document is a function of the chain too (see {@link #resolve}); a reference to another document or to an element that does not exist
 * drops the whole chain (section 5.1: no filter is applied). The class belongs to the layout package because a box with a filter is a stacking context and the containing block of its
 * positioned descendants, and because the painted extent of a filter is larger than the box; the pixel work is in
 * <code>org.duckdns.hjow.swingwebpanel.render</code>.
 * <p>
 * <code>filter</code> 속성의 파싱된 값(CSS Filter Effects Module Level 1, https://www.w3.org/TR/filter-effects-1/, 워킹 드래프트 2018-12-18, 조회 2026-11-17, 6 절
 * "Filter functions", 13.1 절 "Filter functions in SVG terms"): 필터 함수 <code>blur()</code>, <code>brightness()</code>, <code>contrast()</code>,
 * <code>drop-shadow()</code>, <code>grayscale()</code>, <code>hue-rotate()</code>, <code>invert()</code>, <code>opacity()</code>, <code>saturate()</code>,
 * <code>sepia()</code> 의 목록과 그 양. 선언 순서대로이며 첫 함수는 요소를 입력으로 받고 다음 함수는 앞 함수의 출력을 입력으로 받는다. 양은 규격이 요구하는 대로 이미
 * 한정되어 있다(<code>grayscale()</code>, <code>invert()</code>, <code>opacity()</code>, <code>sepia()</code> 는 최대 1). 같은 문서의 SVG <code>filter</code> 요소를 가리키는
 * <code>url(#id)</code> 참조도 사슬의 함수이다({@link #resolve} 참조). 다른 문서나 존재하지 않는 요소를 가리키는 참조가 있으면 사슬 전체를 버린다(5.1 절: 필터를 적용하지
 * 않음). 이 클래스가 layout 패키지에 있는 이유는 필터가 있는
 * 상자가 쌓임 맥락이고 위치 지정 자손의 포함 블록이며 필터가 그리는 범위가 상자보다 크기 때문이다. 픽셀 처리는 <code>org.duckdns.hjow.swingwebpanel.render</code> 에 있다.
 */
public final class FilterSpec {

    /** Kind constant of the <code>blur()</code> filter function. <code>blur()</code> 필터 함수의 종류 상수. */
    public static final int BLUR = 0;
    /** Kind constant of the <code>brightness()</code> filter function. <code>brightness()</code> 필터 함수의 종류 상수. */
    public static final int BRIGHTNESS = 1;
    /** Kind constant of the <code>contrast()</code> filter function. <code>contrast()</code> 필터 함수의 종류 상수. */
    public static final int CONTRAST = 2;
    /** Kind constant of the <code>drop-shadow()</code> filter function. <code>drop-shadow()</code> 필터 함수의 종류 상수. */
    public static final int DROP_SHADOW = 3;
    /** Kind constant of the <code>grayscale()</code> filter function. <code>grayscale()</code> 필터 함수의 종류 상수. */
    public static final int GRAYSCALE = 4;
    /** Kind constant of the <code>hue-rotate()</code> filter function. <code>hue-rotate()</code> 필터 함수의 종류 상수. */
    public static final int HUE_ROTATE = 5;
    /** Kind constant of the <code>invert()</code> filter function. <code>invert()</code> 필터 함수의 종류 상수. */
    public static final int INVERT = 6;
    /** Kind constant of the <code>opacity()</code> filter function. <code>opacity()</code> 필터 함수의 종류 상수. */
    public static final int OPACITY = 7;
    /** Kind constant of the <code>saturate()</code> filter function. <code>saturate()</code> 필터 함수의 종류 상수. */
    public static final int SATURATE = 8;
    /** Kind constant of the <code>sepia()</code> filter function. <code>sepia()</code> 필터 함수의 종류 상수. */
    public static final int SEPIA = 9;
    /** Kind constant of a reference <code>url(#id)</code> to an SVG <code>filter</code> element. SVG <code>filter</code> 요소를 가리키는 <code>url(#id)</code> 참조의 종류 상수. */
    public static final int REFERENCE = 10;

    /** The names of the kinds by constant. 종류 상수별 이름. */
    private static final String[] NAMES = { "blur", "brightness", "contrast", "drop-shadow", "grayscale", "hue-rotate", "invert", "opacity", "saturate",
        "sepia", "url" };

    /**
     * Finds the SVG definitions that a reference to a filter element names (the renderer implements it with the document of the box).
     * <p>
     * 필터 요소를 가리키는 참조가 뜻하는 SVG 정의를 찾는다(렌더러가 상자의 문서로 구현한다).
     */
    public interface Resolver {
        /**
         * Returns the definitions that contain a <code>filter</code> element with an id.
         * <p>
         * id 가 있는 <code>filter</code> 요소가 든 정의를 반환한다.
         *
         * @param id the id of the element (without the <code>#</code>) / 요소의 id(<code>#</code> 제외)
         * @return the definitions, or null if the document has no such element / 정의. 문서에 그런 요소가 없으면 null
         */
        SvgReference filter(String id);
    }

    /**
     * One filter function with its arguments.
     * <p>
     * 인수를 가진 필터 함수 하나.
     */
    public static final class Op {
        /** The kind, one of the constants of {@link FilterSpec}. 종류. {@link FilterSpec} 의 상수 중 하나. */
        public final int kind;
        /**
         * The amount: the standard deviation in px for <code>blur()</code>, the angle in degrees for <code>hue-rotate()</code>, the factor
         * for the others.
         * <p>
         * 양: <code>blur()</code> 는 px 단위의 표준 편차, <code>hue-rotate()</code> 는 도 단위 각도, 나머지는 배율.
         */
        public final double amount;
        /** The horizontal offset of a drop shadow in px. drop shadow 의 가로 오프셋(px). */
        public final double dx;
        /** The vertical offset of a drop shadow in px. drop shadow 의 세로 오프셋(px). */
        public final double dy;
        /** The standard deviation of the blur of a drop shadow in px. drop shadow 의 흐림 표준 편차(px). */
        public final double deviation;
        /** The color of a drop shadow as ARGB. drop shadow 의 색(ARGB). */
        public final int color;
        /** The id of the referenced <code>filter</code> element of a {@link #REFERENCE}, or null. {@link #REFERENCE} 가 가리키는 <code>filter</code> 요소의 id. 없으면 null. */
        public final String ref;
        /** The SVG definitions that the reference was resolved to, or null while it is not resolved. 참조를 푼 SVG 정의. 풀리지 않았으면 null. */
        public final SvgReference svg;
        /** How far the region of the referenced filter reaches beyond the border box {left, top, right, bottom}, or null. 참조한 필터의 영역이 border box 밖으로 뻗는 거리 {왼쪽, 위쪽, 오른쪽, 아래쪽}. 없으면 null. */
        public final double[] reach;

        /**
         * Creates a function with an amount.
         * <p>
         * 양을 가진 함수를 만든다.
         *
         * @param kind   the kind / 종류
         * @param amount the amount / 양
         */
        Op(int kind, double amount) {
            this(kind, amount, 0, 0, 0, 0);
        }

        /**
         * Creates a function with all arguments.
         * <p>
         * 모든 인수를 가진 함수를 만든다.
         *
         * @param kind      the kind / 종류
         * @param amount    the amount / 양
         * @param dx        shadow x offset / 그림자 x 오프셋
         * @param dy        shadow y offset / 그림자 y 오프셋
         * @param deviation shadow blur deviation / 그림자 흐림 편차
         * @param color     shadow color / 그림자 색
         */
        Op(int kind, double amount, double dx, double dy, double deviation, int color) {
            this(kind, amount, dx, dy, deviation, color, null, null, null);
        }

        /**
         * Creates a function with all arguments and a reference.
         * <p>
         * 모든 인수와 참조를 가진 함수를 만든다.
         *
         * @param kind      the kind / 종류
         * @param amount    the amount / 양
         * @param dx        shadow x offset / 그림자 x 오프셋
         * @param dy        shadow y offset / 그림자 y 오프셋
         * @param deviation shadow blur deviation / 그림자 흐림 편차
         * @param color     shadow color / 그림자 색
         * @param ref       the id of the referenced filter or null / 참조한 필터의 id. 없으면 null
         * @param svg       the resolved definitions or null / 푼 정의. 없으면 null
         * @param reach     the reach of the filter region or null / 필터 영역의 뻗음. 없으면 null
         */
        Op(int kind, double amount, double dx, double dy, double deviation, int color, String ref, SvgReference svg, double[] reach) {
            this.ref = ref;
            this.svg = svg;
            this.reach = reach;
            this.kind = kind;
            this.amount = amount;
            this.dx = dx;
            this.dy = dy;
            this.deviation = deviation;
            this.color = color;
        }

        /**
         * Returns a short description for diagnostics.
         * <p>
         * 진단용 짧은 설명을 반환한다.
         *
         * @return the description / 설명
         */
        public String toString() {
            return NAMES[kind] + "(" + amount + (kind == DROP_SHADOW ? " " + dx + " " + dy + " " + deviation : "") + ")";
        }
    }

    /** The functions in order. 순서대로 놓인 함수. */
    private final List<Op> ops;

    /**
     * Creates the specification.
     * <p>
     * 명세를 만든다.
     *
     * @param ops the functions / 함수
     */
    private FilterSpec(List<Op> ops) {
        this.ops = Collections.unmodifiableList(ops);
    }

    /**
     * Returns the functions in order.
     * <p>
     * 순서대로 놓인 함수를 반환한다.
     *
     * @return an unmodifiable list / 수정할 수 없는 목록
     */
    public List<Op> getOps() {
        return ops;
    }

    /**
     * Tells whether the list has a reference to an SVG <code>filter</code> element.
     * <p>
     * 목록에 SVG <code>filter</code> 요소 참조가 있는지 알려 준다.
     *
     * @return true if there is a {@link #REFERENCE} / {@link #REFERENCE} 가 있으면 true
     */
    public boolean hasReferences() {
        // 함수 중 참조가 하나라도 있는지 본다.
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i).kind == REFERENCE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the references to SVG <code>filter</code> elements for a box of a size: the whole chain is dropped (null) if one of them names no filter
     * element of the document, as the specification demands for a reference to an element that does not exist (Filter Effects 1, section 5).
     * <p>
     * SVG <code>filter</code> 요소 참조를 주어진 크기의 상자에 대해 푼다: 하나라도 문서에 없는 필터 요소를 가리키면 사슬 전체를 버린다(null). 존재하지 않는 요소의
     * 참조에 대해 규격이 요구하는 대로이다(Filter Effects 1, 5 절).
     *
     * @param resolver the resolver of the document of the box / 상자의 문서의 해석기
     * @param w        the width of the border box / border box 너비
     * @param h        the height of the border box / border box 높이
     * @return the specification (this one if it has no reference), or null if a reference cannot be resolved / 명세(참조가 없으면 자신). 참조를 풀 수 없으면 null
     */
    public FilterSpec resolve(Resolver resolver, double w, double h) {
        if (!hasReferences()) {
            return this;
        }
        List<Op> out = new ArrayList<Op>(ops.size());
        // 참조가 아닌 함수는 그대로 두고 참조는 문서의 정의와 필터 영역의 뻗음을 담아 다시 만든다.
        for (int i = 0; i < ops.size(); i++) {
            Op o = ops.get(i);
            if (o.kind != REFERENCE) {
                out.add(o);
                continue;
            }
            SvgReference svg = resolver == null ? null : resolver.filter(o.ref);
            if (svg == null) {
                return null;
            }
            out.add(new Op(REFERENCE, 0, 0, 0, 0, 0, o.ref, svg, svg.filterReach(o.ref, w, h)));
        }
        return new FilterSpec(out);
    }

    /**
     * Parses the <code>filter</code> value of a style.
     * <p>
     * 스타일의 <code>filter</code> 값을 파싱한다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the specification, or null for <code>none</code>, an empty list or a value that is not supported (a <code>url()</code>) / 명세. <code>none</code>, 빈 목록,
     *         지원하지 않는 값(<code>url()</code>)이면 null
     */
    public static FilterSpec of(ComputedStyle s) {
        return of(s, Prop.FILTER);
    }

    /**
     * Parses the <code>backdrop-filter</code> value of a style (Filter Effects Level 2): the same function list as <code>filter</code>, applied to what is
     * painted behind the box.
     * <p>
     * 스타일의 <code>backdrop-filter</code> 값을 파싱한다(Filter Effects Level 2): <code>filter</code> 와 같은 함수 목록이며 상자 뒤에 이미 그려진 내용에 적용한다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the specification, or null for <code>none</code>, an empty list or a value that is not supported / 명세. <code>none</code>, 빈 목록, 지원하지 않는 값이면 null
     */
    public static FilterSpec ofBackdrop(ComputedStyle s) {
        FilterSpec f = of(s, Prop.BACKDROP_FILTER);
        // 배경 필터는 SVG filter 요소 참조를 쓰지 않는다(참조가 든 값은 쓸 수 없는 값으로 본다).
        return f != null && f.hasReferences() ? null : f;
    }

    /**
     * Tells whether a style has a <code>backdrop-filter</code> that is used.
     * <p>
     * 스타일에 쓰이는 <code>backdrop-filter</code> 가 있는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box filters its backdrop / 상자가 자신의 배경을 필터링하면 true
     */
    public static boolean hasBackdrop(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        CssValue v = s.get(Prop.BACKDROP_FILTER);
        return v != null && !v.isKeyword("none") && ofBackdrop(s) != null;
    }

    /**
     * Parses a filter function list stored in a property.
     * <p>
     * 속성에 저장된 필터 함수 목록을 파싱한다.
     *
     * @param s    the style / 스타일
     * @param prop the property id (<code>filter</code> or <code>backdrop-filter</code>) / 속성 id
     * @return the specification or null / 명세. 없으면 null
     */
    private static FilterSpec of(ComputedStyle s, int prop) {
        if (s == null) {
            return null;
        }
        return of(s.get(prop), s.color(Prop.COLOR), null);
    }

    /**
     * Parses a filter function list from a parsed value (the 2D canvas uses this for <code>context.filter</code>, whose value is not the
     * computed value of an element).
     * <p>
     * 파싱된 값에서 필터 함수 목록을 파싱한다(2D 캔버스가 <code>context.filter</code> 에 쓴다. 그 값은 요소의 계산 값이 아니다).
     *
     * @param v     the value of <code>filter</code> (a list of functions, one function or <code>none</code>), may be null / <code>filter</code> 의 값(함수 목록, 함수 하나,
     *              <code>none</code>). null 가능
     * @param color the color that <code>currentcolor</code> means / <code>currentcolor</code> 가 뜻하는 색
     * @param ctx   the reference sizes for relative lengths, or null if the lengths are already absolute (computed values) / 상대 길이의 기준 크기.
     *              길이가 이미 절대 값(계산 값)이면 null
     * @return the specification, or null for <code>none</code>, an empty list or a value that is not supported (a <code>url()</code>) / 명세. <code>none</code>, 빈 목록,
     *         지원하지 않는 값(<code>url()</code>)이면 null
     */
    public static FilterSpec of(CssValue v, int color, LengthContext ctx) {
        if (v == null || v.isKeyword("none")) {
            return null;
        }
        List<CssValue> items = new ArrayList<CssValue>();
        if (v instanceof CssList) {
            items.addAll(((CssList) v).getItems());
        } else {
            items.add(v);
        }
        List<Op> ops = new ArrayList<Op>();
        // 값의 항목을 차례로 읽는다. 함수가 아닌 항목(url)이 있으면 사슬 전체를 무시한다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            // url(#id) 는 같은 문서의 SVG filter 요소 참조이다. 다른 주소(다른 문서)를 가리키는 url() 은 쓰지 않는다.
            if (it instanceof CssUrl) {
                String u = ((CssUrl) it).getUrl();
                if (u == null || !u.startsWith("#") || u.length() < 2) {
                    return null;
                }
                ops.add(new Op(REFERENCE, 0, 0, 0, 0, 0, u.substring(1), null, null));
                continue;
            }
            if (!(it instanceof CssFunctionValue)) {
                return null;
            }
            Op op = parse((CssFunctionValue) it, color, ctx);
            if (op == null) {
                return null;
            }
            ops.add(op);
        }
        return ops.isEmpty() ? null : new FilterSpec(ops);
    }

    /**
     * Tells whether a style has a filter that is used.
     * <p>
     * 스타일에 쓰이는 필터가 있는지 알려 준다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return true if the box has a filter / 상자에 필터가 있으면 true
     */
    public static boolean has(ComputedStyle s) {
        if (s == null) {
            return false;
        }
        CssValue v = s.get(Prop.FILTER);
        return v != null && !v.isKeyword("none") && of(s) != null;
    }

    /**
     * Parses one filter function.
     * <p>
     * 필터 함수 하나를 파싱한다.
     *
     * @param f     the function / 함수
     * @param color the computed <code>color</code> for <code>currentcolor</code> / <code>currentcolor</code> 용 계산된 <code>color</code>
     * @return the function, or null if it is not a filter function / 함수. 필터 함수가 아니면 null
     */
    private static Op parse(CssFunctionValue f, int color, LengthContext ctx) {
        String n = f.getName();
        List<CssValue> a = new ArrayList<CssValue>();
        // 인수를 평평한 목록으로 편다(공백으로 나뉜 인수는 목록 하나이다).
        for (CssValue x : f.getArguments()) {
            if (x instanceof CssList) {
                a.addAll(((CssList) x).getItems());
            } else {
                a.add(x);
            }
        }
        // 이름이 같은 필터 함수의 종류를 찾아 인수를 읽는다.
        for (int k = 0; k < REFERENCE; k++) {
            if (!NAMES[k].equals(n)) {
                continue;
            }
            if (k == BLUR) {
                double r = a.isEmpty() ? 0 : px(a.get(0), ctx);
                return Double.isNaN(r) || r < 0 ? null : new Op(BLUR, r);
            }
            if (k == HUE_ROTATE) {
                if (a.isEmpty()) {
                    return new Op(HUE_ROTATE, 0);
                }
                CssValue x = a.get(0);
                if (x instanceof CssDimension && ((CssDimension) x).getKind() == CssUnits.ANGLE) {
                    return new Op(HUE_ROTATE, ((CssDimension) x).canonical());
                }
                return x instanceof CssNumber && ((CssNumber) x).getValue() == 0 ? new Op(HUE_ROTATE, 0) : null;
            }
            if (k == DROP_SHADOW) {
                return dropShadow(a, color, ctx);
            }
            // 나머지 함수는 숫자나 백분율 하나를 받는다. 생략하면 1 이다.
            double amount = 1;
            if (!a.isEmpty()) {
                CssValue x = a.get(0);
                if (x instanceof CssNumber) {
                    amount = ((CssNumber) x).getValue();
                } else if (x instanceof CssPercentage) {
                    amount = ((CssPercentage) x).getValue() / 100.0;
                } else {
                    return null;
                }
            }
            if (amount < 0) {
                return null;
            }
            // grayscale, invert, opacity, sepia 의 양은 1 로 한정한다(6.1 절).
            if (k == GRAYSCALE || k == INVERT || k == OPACITY || k == SEPIA) {
                amount = Math.min(1, amount);
            }
            return new Op(k, amount);
        }
        return null;
    }

    /**
     * Parses the arguments of <code>drop-shadow()</code>: a color and two or three lengths in any order of color and lengths.
     * <p>
     * <code>drop-shadow()</code> 의 인수를 파싱한다: 색 하나와 길이 둘이나 셋(색과 길이의 순서는 상관없다).
     *
     * @param a     the flat arguments / 평평한 인수
     * @param color the computed <code>color</code> / 계산된 <code>color</code>
     * @return the function or null / 함수 또는 null
     */
    private static Op dropShadow(List<CssValue> a, int color, LengthContext ctx) {
        int c = color;
        double[] l = new double[3];
        int n = 0;
        // 인수를 하나씩 보며 색과 길이를 나눈다.
        for (int i = 0; i < a.size(); i++) {
            CssValue x = a.get(i);
            if (x instanceof CssColor) {
                c = ((CssColor) x).getArgb();
            } else if (x.isKeyword("currentcolor")) {
                c = color;
            } else {
                double p = px(x, ctx);
                if (Double.isNaN(p) || n >= 3) {
                    return null;
                }
                l[n++] = p;
            }
        }
        if (n < 2) {
            return null;
        }
        if (n == 3 && l[2] < 0) {
            return null;
        }
        return new Op(DROP_SHADOW, 0, l[0], l[1], n == 3 ? l[2] : 0, c);
    }

    /**
     * Returns a length in pixels.
     * <p>
     * 길이를 픽셀로 반환한다.
     *
     * @param v the value / 값
     * @return pixels, NaN if it is not a length / 픽셀. 길이가 아니면 NaN
     */
    private static double px(CssValue v, LengthContext ctx) {
        if (v instanceof CssLength) {
            double p = ctx == null ? ((CssLength) v).absolutePx() : ((CssLength) v).toPx(ctx);
            return p;
        }
        return v instanceof CssNumber && ((CssNumber) v).getValue() == 0 ? 0 : Double.NaN;
    }

    /**
     * Returns the extent by which the filters make the painted area larger than the content: the distance in px beyond the left, top,
     * right and bottom edges that blurs and shadows may reach.
     * <p>
     * 필터가 그려지는 영역을 내용보다 넓히는 정도를 반환한다: 흐림과 그림자가 닿을 수 있는 왼쪽, 위쪽, 오른쪽, 아래쪽 가장자리 밖 거리(px).
     *
     * @return {left, top, right, bottom} / {왼쪽, 위쪽, 오른쪽, 아래쪽}
     */
    public double[] extent() {
        double[] e = new double[4];
        // 함수마다 영역이 넓어지는 만큼 더한다(앞 함수의 번짐이 뒤 함수의 입력이 되므로 더해진다).
        for (int i = 0; i < ops.size(); i++) {
            Op o = ops.get(i);
            if (o.kind == BLUR) {
                double r = reach(o.amount);
                // 네 방향 모두 같은 거리만큼 넓힌다.
                for (int k = 0; k < 4; k++) {
                    e[k] += r;
                }
            } else if (o.kind == DROP_SHADOW) {
                double r = reach(o.deviation);
                e[0] += Math.max(0, r - o.dx);
                e[1] += Math.max(0, r - o.dy);
                e[2] += Math.max(0, r + o.dx);
                e[3] += Math.max(0, r + o.dy);
            } else if (o.kind == REFERENCE && o.reach != null) {
                // 참조한 필터의 영역이 border box 밖으로 뻗는 만큼 넓힌다.
                for (int k = 0; k < 4; k++) {
                    e[k] += o.reach[k];
                }
            }
        }
        return e;
    }

    /**
     * Returns the distance that a Gaussian blur of a standard deviation reaches (three deviations, rounded up, plus a margin for the box
     * approximation).
     * <p>
     * 표준 편차가 주어진 가우스 흐림이 닿는 거리를 반환한다(편차의 3 배를 올림하고 상자 근사를 위한 여유를 더함).
     *
     * @param deviation the standard deviation in px / 표준 편차(px)
     * @return the reach in px / 닿는 거리(px)
     */
    public static double reach(double deviation) {
        return deviation <= 0 ? 0 : Math.ceil(deviation * 3) + 2;
    }
}
