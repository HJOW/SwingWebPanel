package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The interpolation of computed property values (plan item P10-07, CSS Transitions Level 1 section 4 and CSS Values 3 section 9,
 * https://www.w3.org/TR/css-transitions-1/): numbers and lengths linearly, percentages linearly, colors with premultiplied alpha
 * (CSS Color 4 section 12), <code>transform</code> as lists of functions (see {@link Transforms#interpolate}), lists of the same length
 * element by element (for example the two components of <code>transform-origin</code> or <code>background-position</code>), and
 * <code>visibility</code> as in CSS Transitions (visible during the interpolation if one end is visible). Every other pair has no
 * interpolation: {@link #interpolate} returns null, which means that a transition does not start and an animation switches halfway
 * (discrete). A length and a percentage, or lengths that are not absolute, are not mixed (the <code>calc()</code> of the mix is not
 * built). The class is stateless.
 * <p>
 * 계산된 속성 값의 보간(계획 P10-07, CSS Transitions Level 1 4 절, CSS Values 3 9 절, https://www.w3.org/TR/css-transitions-1/): 숫자와 길이는 선형, 백분율은 선형,
 * 색은 알파를 미리 곱해서(CSS Color 4 12 절), <code>transform</code> 은 함수 목록으로({@link Transforms#interpolate} 참조), 같은 길이의 목록은 요소별로(예:
 * <code>transform-origin</code>, <code>background-position</code> 의 두 성분), <code>visibility</code> 는 CSS Transitions 처럼(한쪽 끝이 visible 이면 보간 중 visible)
 * 보간한다. 그 밖의 쌍은 보간이 없다: {@link #interpolate} 는 null 을 반환하며 전환은 시작하지 않고 애니메이션은 중간에서 바뀐다(이산). 길이와 백분율, 절대가 아닌 길이는
 * 섞지 않는다(섞은 <code>calc()</code> 를 만들지 않는다). 클래스는 상태가 없다.
 */
public final class Interpolator {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Interpolator() {
    }

    /**
     * Interpolates two computed values of a property.
     * <p>
     * 속성의 계산 값 둘을 보간한다.
     *
     * @param id   the property id (a constant of {@link Prop}) / 속성 id({@link Prop} 의 상수)
     * @param from the start value / 시작 값
     * @param to   the end value / 끝 값
     * @param p    the progress, outside 0..1 for an overshooting easing / 진행도. 넘치는 완화에서는 0..1 밖
     * @return the value, or null if the two values cannot be interpolated / 값. 두 값을 보간할 수 없으면 null
     */
    public static CssValue interpolate(int id, CssValue from, CssValue to, double p) {
        if (from == null || to == null || from.equals(to)) {
            return null;
        }
        if (id == Prop.TRANSFORM) {
            return Transforms.interpolate(from, to, p, 0, 0);
        }
        // offset-path 는 같은 종류의 도형끼리, 명령이 같은 path() 끼리 보간한다(CSS Motion Path 1 2.1 절).
        if (id == Prop.OFFSET_PATH) {
            return org.duckdns.hjow.swingwebpanel.css.values.ShapeInterpolation.interpolate(from, to, p);
        }
        if (id == Prop.VISIBILITY) {
            boolean a = from.isKeyword("visible");
            boolean b = to.isKeyword("visible");
            if (!a && !b) {
                return null;
            }
            // 한쪽 끝이 visible 이면 보간 중(0 과 1 사이)은 visible 이고 끝점은 각자의 값이다.
            return p <= 0 ? from : (p >= 1 ? to : (a ? from : to));
        }
        return blend(id, from, to, p);
    }

    /**
     * Interpolates two values of the same kind.
     * <p>
     * 같은 종류의 값 둘을 보간한다.
     *
     * @param id   the property id / 속성 id
     * @param from the start / 시작
     * @param to   the end / 끝
     * @param p    the progress / 진행도
     * @return the value or null / 값 또는 null
     */
    private static CssValue blend(int id, CssValue from, CssValue to, double p) {
        if (from instanceof CssNumber && to instanceof CssNumber) {
            double v = mix(((CssNumber) from).getValue(), ((CssNumber) to).getValue(), p);
            boolean integer = ((CssNumber) from).isInteger() && ((CssNumber) to).isInteger();
            return integer ? new CssNumber(Math.round(v), true) : new CssNumber(v, false);
        }
        if (from instanceof CssLength && to instanceof CssLength) {
            double a = px((CssLength) from);
            double b = px((CssLength) to);
            return Double.isNaN(a) || Double.isNaN(b) ? null : CssLength.px(mix(a, b, p));
        }
        if (from instanceof CssPercentage && to instanceof CssPercentage) {
            return new CssPercentage(mix(((CssPercentage) from).getValue(), ((CssPercentage) to).getValue(), p));
        }
        if (from instanceof CssDimension && to instanceof CssDimension && ((CssDimension) from).getKind() == CssUnits.ANGLE
                && ((CssDimension) to).getKind() == CssUnits.ANGLE) {
            // 각도(예: rotate)는 도로 바꿔 선형으로 섞는다.
            return new CssDimension(mix(((CssDimension) from).canonical(), ((CssDimension) to).canonical(), p), "deg");
        }
        if (from instanceof CssColor && to instanceof CssColor) {
            return color((CssColor) from, (CssColor) to, p);
        }
        if (from instanceof CssList && to instanceof CssList) {
            List<CssValue> a = ((CssList) from).getItems();
            List<CssValue> b = ((CssList) to).getItems();
            if (a.size() != b.size() || ((CssList) from).isComma() != ((CssList) to).isComma()) {
                return null;
            }
            List<CssValue> out = new ArrayList<CssValue>(a.size());
            // 요소 쌍마다 보간하고 같은 요소는 그대로 둔다. 하나라도 보간할 수 없으면 목록 전체가 이산이다.
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).equals(b.get(i))) {
                    out.add(a.get(i));
                    continue;
                }
                CssValue v = blend(id, a.get(i), b.get(i), p);
                if (v == null) {
                    return null;
                }
                out.add(v);
            }
            return new CssList(out, ((CssList) from).isComma());
        }
        return null;
    }

    /**
     * Returns the pixels of an absolute length (a computed length is in pixels).
     * <p>
     * 절대 길이의 픽셀 값을 반환한다(계산된 길이는 픽셀이다).
     *
     * @param l the length / 길이
     * @return pixels or NaN / 픽셀 또는 NaN
     */
    private static double px(CssLength l) {
        return l.isPx() ? l.getValue() : l.absolutePx();
    }

    /**
     * Interpolates two colors with premultiplied alpha.
     * <p>
     * 색 둘을 알파를 미리 곱해서 보간한다.
     *
     * @param a the start color / 시작 색
     * @param b the end color / 끝 색
     * @param p the progress / 진행도
     * @return the color / 색
     */
    private static CssValue color(CssColor a, CssColor b, double p) {
        double a1 = a.getAlpha() / 255.0;
        double a2 = b.getAlpha() / 255.0;
        double al = clamp(mix(a1, a2, p), 0, 1);
        if (al <= 0) {
            return CssColor.TRANSPARENT;
        }
        double r = mix(a.getRed() * a1, b.getRed() * a2, p) / al;
        double g = mix(a.getGreen() * a1, b.getGreen() * a2, p) / al;
        double bl = mix(a.getBlue() * a1, b.getBlue() * a2, p) / al;
        return CssColor.rgba((int) Math.round(clamp(r, 0, 255)), (int) Math.round(clamp(g, 0, 255)), (int) Math.round(clamp(bl, 0, 255)),
                (int) Math.round(al * 255));
    }

    /**
     * Limits a number to a range.
     * <p>
     * 숫자를 범위로 제한한다.
     *
     * @param v  the number / 숫자
     * @param lo the lower limit / 하한
     * @param hi the upper limit / 상한
     * @return the limited number / 제한한 숫자
     */
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Linear interpolation.
     * <p>
     * 선형 보간.
     *
     * @param a the start / 시작
     * @param b the end / 끝
     * @param p the progress / 진행도
     * @return <code>a + (b - a) * p</code> / <code>a + (b - a) * p</code>
     */
    private static double mix(double a, double b, double p) {
        return a + (b - a) * p;
    }
}
