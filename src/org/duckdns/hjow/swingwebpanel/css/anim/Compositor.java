package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The composite operations <code>add</code> and <code>accumulate</code> of Web Animations Level 1 (section 5.4.4) for the computed values
 * that this engine can combine: numbers (an integer stays an integer when both are integers), lengths in pixels, percentages, angles, colors
 * (component-wise sum in the premultiplied space, clamped), <code>transform</code> lists, and space or comma lists of such values of the same
 * length. For <code>transform</code>, <code>add</code> appends the effect list to the underlying list; <code>accumulate</code> adds the arguments
 * of matching functions (scale factors as <code>a + b - 1</code>) and appends when the functions differ (the matrix accumulation of CSS
 * Transforms 2 is not implemented). A value that cannot be combined (a keyword, a length and a percentage) gives null, and the caller uses the
 * effect value as for <code>replace</code>.
 * <p>
 * 이 엔진이 결합할 수 있는 계산 값에 대한 Web Animations Level 1(5.4.4 절)의 합성 연산 <code>add</code>, <code>accumulate</code>: 숫자(둘 다 정수이면 정수 유지),
 * 픽셀 길이, 백분율, 각도, 색(전승산 공간의 성분별 합, 제한), <code>transform</code> 목록, 같은 길이의 그런 값의 공백·쉼표 목록. <code>transform</code> 에서
 * <code>add</code> 는 효과 목록을 기저 목록 뒤에 붙이고 <code>accumulate</code> 는 같은 함수의 인수를 더하며(배율은 <code>a + b - 1</code>) 함수가 다르면 붙인다
 * (CSS Transforms 2 의 행렬 누적은 구현하지 않음). 결합할 수 없는 값(키워드, 길이와 백분율)은 null 을 주고 호출자는 <code>replace</code> 처럼 효과 값을 쓴다.
 */
public final class Compositor {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private Compositor() {
    }

    /**
     * Combines an underlying value and an effect value.
     * <p>
     * 기저 값과 효과 값을 결합한다.
     *
     * @param id         the property id / 속성 id
     * @param underlying the underlying value (Va) / 기저 값(Va)
     * @param value      the effect value (Vb) / 효과 값(Vb)
     * @param operation  <code>add</code> or <code>accumulate</code> / <code>add</code> 또는 <code>accumulate</code>
     * @return the result, or null if the values cannot be combined / 결과. 결합할 수 없으면 null
     */
    public static CssValue combine(int id, CssValue underlying, CssValue value, String operation) {
        if (underlying == null || value == null) {
            return null;
        }
        boolean accumulate = "accumulate".equals(operation);
        if (id == Prop.TRANSFORM) {
            return transform(underlying, value, accumulate);
        }
        return sum(underlying, value);
    }

    /**
     * Adds two values of the same kind.
     * <p>
     * 같은 종류의 두 값을 더한다.
     *
     * @param a the first value / 첫 값
     * @param b the second value / 둘째 값
     * @return the sum or null / 합 또는 null
     */
    private static CssValue sum(CssValue a, CssValue b) {
        if (a instanceof CssNumber && b instanceof CssNumber) {
            CssNumber x = (CssNumber) a;
            CssNumber y = (CssNumber) b;
            return new CssNumber(x.getValue() + y.getValue(), x.isInteger() && y.isInteger());
        }
        if (a instanceof CssLength && b instanceof CssLength) {
            double x = px((CssLength) a);
            double y = px((CssLength) b);
            return Double.isNaN(x) || Double.isNaN(y) ? null : CssLength.px(x + y);
        }
        if (a instanceof CssPercentage && b instanceof CssPercentage) {
            return new CssPercentage(((CssPercentage) a).getValue() + ((CssPercentage) b).getValue());
        }
        if (a instanceof CssDimension && b instanceof CssDimension && ((CssDimension) a).getKind() == CssUnits.ANGLE
                && ((CssDimension) b).getKind() == CssUnits.ANGLE) {
            return new CssDimension(((CssDimension) a).canonical() + ((CssDimension) b).canonical(), "deg");
        }
        if (a instanceof CssColor && b instanceof CssColor) {
            return color((CssColor) a, (CssColor) b);
        }
        if (a instanceof CssList && b instanceof CssList) {
            CssList x = (CssList) a;
            CssList y = (CssList) b;
            if (x.size() != y.size() || x.isComma() != y.isComma()) {
                return null;
            }
            List<CssValue> out = new ArrayList<CssValue>(x.size());
            // 항목 쌍마다 더한다. 하나라도 더할 수 없으면 목록 전체를 결합할 수 없다.
            for (int i = 0; i < x.size(); i++) {
                CssValue s = sum(x.get(i), y.get(i));
                if (s == null) {
                    return null;
                }
                out.add(s);
            }
            return new CssList(out, x.isComma());
        }
        return null;
    }

    /**
     * Combines two <code>transform</code> values.
     * <p>
     * 두 <code>transform</code> 값을 결합한다.
     *
     * @param a          the underlying list / 기저 목록
     * @param b          the effect list / 효과 목록
     * @param accumulate true for <code>accumulate</code> / <code>accumulate</code> 이면 true
     * @return the result or null / 결과 또는 null
     */
    private static CssValue transform(CssValue a, CssValue b, boolean accumulate) {
        List<CssValue> x = functions(a);
        List<CssValue> y = functions(b);
        if (x == null || y == null) {
            return null;
        }
        if (x.isEmpty()) {
            return b;
        }
        if (y.isEmpty()) {
            return a;
        }
        if (accumulate && x.size() == y.size()) {
            List<CssValue> out = new ArrayList<CssValue>(x.size());
            boolean ok = true;
            // 같은 함수끼리 인수를 더한다(배율은 1 을 기준으로 더한다).
            for (int i = 0; i < x.size() && ok; i++) {
                CssValue s = accumulateFunction(x.get(i), y.get(i));
                ok = s != null;
                out.add(s);
            }
            if (ok) {
                return new CssList(out, false);
            }
        }
        List<CssValue> out = new ArrayList<CssValue>(x);
        out.addAll(y);
        return new CssList(out, false);
    }

    /**
     * Returns the functions of a <code>transform</code> value (empty for <code>none</code>).
     * <p>
     * <code>transform</code> 값의 함수를 반환한다(<code>none</code> 이면 빈 목록).
     *
     * @param v the value / 값
     * @return the functions or null if the value is not a transform list / 함수. transform 목록이 아니면 null
     */
    private static List<CssValue> functions(CssValue v) {
        if (v instanceof CssKeyword) {
            return v.isKeyword("none") ? new ArrayList<CssValue>() : null;
        }
        if (v instanceof CssFunctionValue) {
            List<CssValue> l = new ArrayList<CssValue>();
            l.add(v);
            return l;
        }
        if (v instanceof CssList && !((CssList) v).isComma()) {
            List<CssValue> l = new ArrayList<CssValue>(((CssList) v).getItems());
            // 목록의 항목이 모두 함수여야 transform 목록이다.
            for (CssValue i : l) {
                if (!(i instanceof CssFunctionValue)) {
                    return null;
                }
            }
            return l;
        }
        return null;
    }

    /**
     * Accumulates two transform functions of the same name.
     * <p>
     * 이름이 같은 두 transform 함수를 누적한다.
     *
     * @param a the underlying function / 기저 함수
     * @param b the effect function / 효과 함수
     * @return the result, or null if the names or the argument counts differ / 결과. 이름이나 인수 수가 다르면 null
     */
    private static CssValue accumulateFunction(CssValue a, CssValue b) {
        CssFunctionValue f = (CssFunctionValue) a;
        CssFunctionValue g = (CssFunctionValue) b;
        if (!f.getName().equalsIgnoreCase(g.getName()) || f.getArguments().size() != g.getArguments().size()) {
            return null;
        }
        boolean scale = f.getName().toLowerCase(java.util.Locale.ENGLISH).startsWith("scale");
        List<CssValue> out = new ArrayList<CssValue>();
        // 인수 쌍마다 더한다(배율 함수의 숫자는 a + b - 1).
        for (int i = 0; i < f.getArguments().size(); i++) {
            CssValue x = f.getArguments().get(i);
            CssValue y = g.getArguments().get(i);
            if (scale && x instanceof CssNumber && y instanceof CssNumber) {
                out.add(new CssNumber(((CssNumber) x).getValue() + ((CssNumber) y).getValue() - 1, false));
                continue;
            }
            CssValue s = sum(x, y);
            if (s == null) {
                return null;
            }
            out.add(s);
        }
        return new CssFunctionValue(f.getName(), out);
    }

    /**
     * Returns a length in pixels.
     * <p>
     * 길이를 픽셀로 반환한다.
     *
     * @param l the length / 길이
     * @return pixels, NaN if relative / 픽셀. 상대 단위이면 NaN
     */
    private static double px(CssLength l) {
        return l.isPx() ? l.getValue() : l.absolutePx();
    }

    /**
     * Adds two colors component-wise in the premultiplied space and clamps the result.
     * <p>
     * 두 색을 전승산 공간에서 성분별로 더하고 결과를 제한한다.
     *
     * @param a the first color / 첫 색
     * @param b the second color / 둘째 색
     * @return the sum / 합
     */
    private static CssValue color(CssColor a, CssColor b) {
        double aa = a.getAlpha() / 255.0;
        double ba = b.getAlpha() / 255.0;
        double al = Math.min(1, aa + ba);
        if (al <= 0) {
            return CssColor.TRANSPARENT;
        }
        double r = Math.min(255, (a.getRed() * aa + b.getRed() * ba) / al);
        double g = Math.min(255, (a.getGreen() * aa + b.getGreen() * ba) / al);
        double bl = Math.min(255, (a.getBlue() * aa + b.getBlue() * ba) / al);
        return CssColor.rgba((int) Math.round(r), (int) Math.round(g), (int) Math.round(bl), (int) Math.round(al * 255));
    }
}
