package org.duckdns.hjow.swingwebpanel.css.anim;

import java.awt.geom.AffineTransform;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * The 2D transform of an element (plan item P10-06, CSS Transforms Level 1, https://www.w3.org/TR/css-transforms-1/ section 8 and 13):
 * turns the computed value of <code>transform</code> and <code>transform-origin</code> into an {@link AffineTransform}. The list of
 * functions is multiplied left to right (<code>matrix = F1 * F2 * ... * Fn</code>, so the last function is applied to the points
 * first); <code>translate</code> percentages refer to the width and height of the reference box (the border box of the element, the
 * <code>view-box</code> of an HTML element); the transform is applied around the transform origin (default <code>50% 50%</code>):
 * <code>translate(origin) * T * translate(-origin)</code>. 3D (CSS Transforms Level 2, explicit exception EXC-03 of the feature table):
 * the property grammar accepts only the 3D function names whose arguments make them exactly 2D (<code>translate3d(x, y, 0)</code>,
 * <code>translateZ(0)</code>, <code>scale3d(x, y, 1)</code>, <code>scaleZ(1)</code>, <code>rotateZ(a)</code>,
 * <code>rotate3d(0, 0, 1, a)</code> and a <code>matrix3d()</code> whose 3D entries are those of the identity); they are projected to the
 * 2D function. Any other 3D value (<code>rotateX()</code>, <code>perspective()</code>, a real <code>translateZ()</code> ...) makes the
 * declaration invalid, as for any unknown function, so it is ignored; <code>perspective</code>, <code>transform-style</code> and
 * <code>backface-visibility</code> have no effect. This class is stateless. {@link #interpolate} blends two transform lists for
 * transitions and animations (CSS Transforms 1 section 15: a pair of lists with the same function names is interpolated function by
 * function, otherwise the matrices are interpolated through their decomposition).
 * <p>
 * 요소의 2D 변환(계획 P10-06, CSS Transforms Level 1, https://www.w3.org/TR/css-transforms-1/ 8, 13 절): <code>transform</code> 과 <code>transform-origin</code> 의
 * 계산 값을 {@link AffineTransform} 으로 바꾼다. 함수 목록은 왼쪽에서 오른쪽으로 곱한다(<code>행렬 = F1 * F2 * ... * Fn</code>, 곧 마지막 함수가 점에 가장 먼저 적용됨).
 * <code>translate</code> 의 백분율은 기준 상자(요소의 border box, HTML 요소의 <code>view-box</code>)의 너비와 높이를 참조한다. 변환은 변환 원점 둘레에 적용한다(기본
 * <code>50% 50%</code>): <code>translate(원점) * T * translate(-원점)</code>. 3D(CSS Transforms Level 2, 기능 표의 명시적 예외 EXC-03): 속성 문법은 인수가 정확히 2D 가 되는
 * 3D 함수 이름만 받는다(<code>translate3d(x, y, 0)</code>, <code>translateZ(0)</code>, <code>scale3d(x, y, 1)</code>, <code>scaleZ(1)</code>, <code>rotateZ(a)</code>,
 * <code>rotate3d(0, 0, 1, a)</code>, 3D 항목이 단위 행렬의 것인 <code>matrix3d()</code>). 이들은 2D 함수로 투영한다. 그 밖의 3D 값(<code>rotateX()</code>, <code>perspective()</code>,
 * 0 이 아닌 <code>translateZ()</code> 등)은 모르는 함수처럼 선언을 무효로 만들어 무시된다. <code>perspective</code>, <code>transform-style</code>,
 * <code>backface-visibility</code> 는 효과가 없다. 이 클래스는 상태가 없다. {@link #interpolate} 는 전환과 애니메이션을 위해 두 변환 목록을 섞는다(CSS Transforms 1 15 절:
 * 함수 이름이 같은 목록 쌍은 함수별로 보간하고, 아니면 행렬을 분해해서 보간한다).
 */
public final class Transforms {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Transforms() {
    }

    /**
     * Tells whether a computed <code>transform</code> value is <code>none</code> (or empty).
     * <p>
     * 계산된 <code>transform</code> 값이 <code>none</code>(또는 빈 목록)인지 알려 준다.
     *
     * @param v the value, may be null / 값. null 가능
     * @return true for none / none 이면 true
     */
    public static boolean isNone(CssValue v) {
        return v == null || v.isKeyword("none") || (v instanceof CssList && ((CssList) v).getItems().isEmpty());
    }

    /**
     * Returns the transform of a box in the coordinates of its parent: the transform of the style around the transform origin, or null if
     * the style has no transform.
     * <p>
     * 상자의 변환을 부모 좌표로 반환한다: 스타일의 변환을 변환 원점 둘레에 적용한 것. 스타일에 변환이 없으면 null.
     *
     * @param s the computed style / 계산 스타일
     * @param x left of the border box in document coordinates / 문서 좌표의 border box 왼쪽
     * @param y top of the border box / border box 위쪽
     * @param w width of the border box / border box 너비
     * @param h height of the border box / border box 높이
     * @return the transform or null / 변환 또는 null
     */
    public static AffineTransform boxTransform(ComputedStyle s, double x, double y, double w, double h) {
        if (s == null) {
            return null;
        }
        // 변환 행렬은 translate, rotate, scale 개별 속성을 차례로 곱한 뒤 transform 을 곱한 것이다(CSS Transforms 2 의 순서).
        AffineTransform ind = individual(s, w, h);
        AffineTransform m = isNone(s.get(Prop.TRANSFORM)) ? null : matrix(s.get(Prop.TRANSFORM), w, h);
        if (ind == null && m == null) {
            return null;
        }
        AffineTransform t = ind == null ? m : new AffineTransform(ind);
        if (ind != null && m != null) {
            t.concatenate(m);
        }
        return anchor(t, s.get(Prop.TRANSFORM_ORIGIN), x, y, w, h);
    }

    /**
     * Returns the matrix of the individual properties <code>translate</code>, <code>rotate</code> and <code>scale</code> (CSS Transforms
     * Level 2 section 3, https://www.w3.org/TR/css-transforms-2/): <code>translate * rotate * scale</code> in this order. Only the 2D forms
     * exist in the property grammar (<code>rotate: z 30deg</code> is the same as <code>rotate: 30deg</code>; a rotation around another axis
     * and the three-value forms are invalid).
     * <p>
     * 개별 속성 <code>translate</code>, <code>rotate</code>, <code>scale</code> 의 행렬을 반환한다(CSS Transforms Level 2 3 절, https://www.w3.org/TR/css-transforms-2/):
     * 이 순서의 <code>translate * rotate * scale</code>. 속성 문법에는 2D 형태만 있다(<code>rotate: z 30deg</code> 는 <code>rotate: 30deg</code> 와 같고, 다른 축의 회전과
     * 값 세 개 형태는 무효이다).
     *
     * @param s the computed style / 계산 스타일
     * @param w width of the reference box / 기준 상자 너비
     * @param h height of the reference box / 기준 상자 높이
     * @return the matrix, null if all three are <code>none</code> / 행렬. 셋 다 <code>none</code> 이면 null
     */
    public static AffineTransform individual(ComputedStyle s, double w, double h) {
        AffineTransform t = null;
        CssValue tr = s.get(Prop.TRANSLATE);
        if (!isNone(tr)) {
            List<CssValue> a = flat(tr);
            t = new AffineTransform();
            t.translate(lp(a.get(0), w), a.size() > 1 ? lp(a.get(1), h) : 0);
        }
        CssValue ro = s.get(Prop.ROTATE);
        if (!isNone(ro)) {
            List<CssValue> a = flat(ro);
            if (t == null) {
                t = new AffineTransform();
            }
            t.rotate(Math.toRadians(deg(a.get(a.size() - 1))));
        }
        CssValue sc = s.get(Prop.SCALE);
        if (!isNone(sc)) {
            List<CssValue> a = flat(sc);
            if (t == null) {
                t = new AffineTransform();
            }
            double sx = scaleOf(a.get(0));
            t.scale(sx, a.size() > 1 ? scaleOf(a.get(1)) : sx);
        }
        return t;
    }

    /**
     * Returns the items of a value that may be a space list (a single value is a list of one).
     * <p>
     * 공백 목록일 수 있는 값의 항목을 반환한다(값 하나는 항목이 하나인 목록).
     *
     * @param v the value / 값
     * @return the items / 항목
     */
    private static List<CssValue> flat(CssValue v) {
        if (v instanceof CssList) {
            return ((CssList) v).getItems();
        }
        List<CssValue> one = new java.util.ArrayList<CssValue>(1);
        one.add(v);
        return one;
    }

    /**
     * Reads a scale factor: a number, or a percentage (150% is 1.5).
     * <p>
     * 배율을 읽는다: 숫자나 백분율(150% 는 1.5).
     *
     * @param v the value / 값
     * @return the factor / 배율
     */
    private static double scaleOf(CssValue v) {
        return v instanceof CssPercentage ? ((CssPercentage) v).getValue() / 100.0 : num(v);
    }

    /**
     * Applies a transform around the transform origin of a box: <code>translate(origin) * T * translate(-origin)</code> in document
     * coordinates.
     * <p>
     * 상자의 변환 원점 둘레에 변환을 적용한다: 문서 좌표의 <code>translate(원점) * T * translate(-원점)</code>.
     *
     * @param t      the transform / 변환
     * @param origin the value of <code>transform-origin</code> / <code>transform-origin</code> 값
     * @param x      left of the border box / border box 왼쪽
     * @param y      top of the border box / border box 위쪽
     * @param w      width of the border box / border box 너비
     * @param h      height of the border box / border box 높이
     * @return the transform in document coordinates / 문서 좌표의 변환
     */
    private static AffineTransform anchor(AffineTransform t, CssValue origin, double x, double y, double w, double h) {
        double[] o = origin(origin, w, h);
        AffineTransform full = new AffineTransform();
        full.translate(x + o[0], y + o[1]);
        full.concatenate(t);
        // 원점은 문서 좌표로 (x + ox, y + oy) 이므로 같은 값을 되돌려 뺀다.
        full.translate(-(x + o[0]), -(y + o[1]));
        return full;
    }

    /**
     * Returns the transform of a box for given values of <code>transform</code> and <code>transform-origin</code> (animations compute
     * the values at a moment and call this).
     * <p>
     * 주어진 <code>transform</code> 과 <code>transform-origin</code> 값으로 상자의 변환을 반환한다(애니메이션은 어느 시각의 값을 계산해 이것을 부른다).
     *
     * @param transform the value of <code>transform</code> / <code>transform</code> 값
     * @param origin    the value of <code>transform-origin</code> / <code>transform-origin</code> 값
     * @param x         left of the border box / border box 왼쪽
     * @param y         top of the border box / border box 위쪽
     * @param w         width of the border box / border box 너비
     * @param h         height of the border box / border box 높이
     * @return the transform or null / 변환 또는 null
     */
    public static AffineTransform boxTransform(CssValue transform, CssValue origin, double x, double y, double w, double h) {
        if (isNone(transform)) {
            return null;
        }
        AffineTransform t = matrix(transform, w, h);
        if (t == null) {
            return null;
        }
        return anchor(t, origin, x, y, w, h);
    }

    /**
     * Returns the matrix of a computed <code>transform</code> value: the functions multiplied from the left to the right.
     * <p>
     * 계산된 <code>transform</code> 값의 행렬을 반환한다: 함수를 왼쪽에서 오른쪽으로 곱한 것.
     *
     * @param v the value / 값
     * @param w width of the reference box (base of percentages of the horizontal translation) / 기준 상자 너비(가로 이동 백분율의 기준)
     * @param h height of the reference box / 기준 상자 높이
     * @return the matrix, or null for none or a value that cannot be read / 행렬. none 이거나 읽을 수 없는 값이면 null
     */
    public static AffineTransform matrix(CssValue v, double w, double h) {
        if (isNone(v)) {
            return null;
        }
        AffineTransform at = new AffineTransform();
        if (v instanceof CssFunctionValue) {
            return apply(at, (CssFunctionValue) v, w, h) ? at : null;
        }
        if (!(v instanceof CssList)) {
            return null;
        }
        // 함수를 왼쪽에서 오른쪽으로 곱한다.
        for (CssValue f : ((CssList) v).getItems()) {
            if (!(f instanceof CssFunctionValue) || !apply(at, (CssFunctionValue) f, w, h)) {
                return null;
            }
        }
        return at;
    }

    /**
     * Multiplies one transform function onto a matrix.
     * <p>
     * 변환 함수 하나를 행렬에 곱한다.
     *
     * @param at the matrix that is multiplied from the right / 오른쪽에서 곱할 행렬
     * @param f  the function / 함수
     * @param w  width of the reference box / 기준 상자 너비
     * @param h  height of the reference box / 기준 상자 높이
     * @return true if the function is known and was applied / 아는 함수이고 적용했으면 true
     */
    private static boolean apply(AffineTransform at, CssFunctionValue f, double w, double h) {
        String n = f.getName();
        List<CssValue> a = f.getArguments();
        if ("matrix".equals(n) && a.size() == 6) {
            at.concatenate(new AffineTransform(num(a.get(0)), num(a.get(1)), num(a.get(2)), num(a.get(3)), num(a.get(4)), num(a.get(5))));
        } else if ("matrix3d".equals(n) && a.size() == 16) {
            // 2D 와 같은 matrix3d 는 열 우선 순서에서 a, b, c, d, e, f 의 자리만 읽는다.
            at.concatenate(new AffineTransform(num(a.get(0)), num(a.get(1)), num(a.get(4)), num(a.get(5)), num(a.get(12)), num(a.get(13))));
        } else if (("translate".equals(n) || "translate3d".equals(n)) && a.size() >= 1) {
            at.translate(lp(a.get(0), w), a.size() > 1 ? lp(a.get(1), h) : 0);
        } else if ("translatex".equals(n) && a.size() == 1) {
            at.translate(lp(a.get(0), w), 0);
        } else if ("translatey".equals(n) && a.size() == 1) {
            at.translate(0, lp(a.get(0), h));
        } else if ("translatez".equals(n) || "scalez".equals(n)) {
            // 2D 로 투영하면 항등이다(문법이 z 크기가 0 이거나 1 인 값만 받는다).
            return a.size() == 1;
        } else if (("scale".equals(n) || "scale3d".equals(n)) && a.size() >= 1) {
            double sx = num(a.get(0));
            at.scale(sx, a.size() > 1 ? num(a.get(1)) : sx);
        } else if ("scalex".equals(n) && a.size() == 1) {
            at.scale(num(a.get(0)), 1);
        } else if ("scaley".equals(n) && a.size() == 1) {
            at.scale(1, num(a.get(0)));
        } else if (("rotate".equals(n) || "rotatez".equals(n)) && a.size() == 1) {
            at.rotate(Math.toRadians(deg(a.get(0))));
        } else if ("rotate3d".equals(n) && a.size() == 4) {
            at.rotate(Math.toRadians(deg(a.get(3))));
        } else if ("skew".equals(n) && a.size() >= 1) {
            at.concatenate(new AffineTransform(1, a.size() > 1 ? Math.tan(Math.toRadians(deg(a.get(1)))) : 0, Math.tan(Math.toRadians(deg(a.get(0)))), 1, 0, 0));
        } else if ("skewx".equals(n) && a.size() == 1) {
            at.concatenate(new AffineTransform(1, 0, Math.tan(Math.toRadians(deg(a.get(0)))), 1, 0, 0));
        } else if ("skewy".equals(n) && a.size() == 1) {
            at.concatenate(new AffineTransform(1, Math.tan(Math.toRadians(deg(a.get(0)))), 0, 1, 0, 0));
        } else {
            return false;
        }
        return true;
    }

    /**
     * Reads a number argument (0 if it cannot be read).
     * <p>
     * 숫자 인수를 읽는다(읽을 수 없으면 0).
     *
     * @param v the argument / 인수
     * @return the number / 숫자
     */
    private static double num(CssValue v) {
        if (v instanceof CssNumber) {
            return ((CssNumber) v).getValue();
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), 0);
        }
        return 0;
    }

    /**
     * Reads an angle argument in degrees (a unitless zero is 0).
     * <p>
     * 각도 인수를 도로 읽는다(단위 없는 0 은 0).
     *
     * @param v the argument / 인수
     * @return degrees / 도
     */
    private static double deg(CssValue v) {
        if (v instanceof CssDimension && ((CssDimension) v).getKind() == CssUnits.ANGLE) {
            return ((CssDimension) v).canonical();
        }
        return v instanceof CssNumber ? ((CssNumber) v).getValue() : 0;
    }

    /**
     * Reads a length or percentage argument in pixels.
     * <p>
     * 길이나 백분율 인수를 픽셀로 읽는다.
     *
     * @param v    the argument / 인수
     * @param base the base of a percentage / 백분율의 기준
     * @return pixels / 픽셀
     */
    private static double lp(CssValue v, double base) {
        if (v instanceof CssLength) {
            double px = ((CssLength) v).absolutePx();
            return Double.isNaN(px) ? ((CssLength) v).getValue() : px;
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() / 100.0 * base;
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), base);
        }
        return v instanceof CssNumber ? ((CssNumber) v).getValue() : 0;
    }

    /**
     * Returns the transform origin relative to the border box: a keyword list is read like <code>background-position</code> (a
     * vertical keyword first, or a horizontal keyword second, swaps the two), a single value is the horizontal position, the vertical
     * position is the center; the third (z) value of the grammar is ignored.
     * <p>
     * 변환 원점을 border box 기준으로 반환한다: 키워드 목록은 <code>background-position</code> 처럼 읽고(세로 키워드가 먼저이거나 가로 키워드가 둘째이면 둘을 맞바꾼다),
     * 값 하나는 가로 위치이며 세로 위치는 가운데이다. 문법의 셋째(z) 값은 무시한다.
     *
     * @param v the computed value of <code>transform-origin</code>, may be null / <code>transform-origin</code> 의 계산 값. null 가능
     * @param w width of the border box / border box 너비
     * @param h height of the border box / border box 높이
     * @return {x, y} / {x, y}
     */
    public static double[] origin(CssValue v, double w, double h) {
        List<CssValue> items = null;
        if (v instanceof CssList) {
            items = ((CssList) v).getItems();
        }
        CssValue first = items != null && items.size() > 0 ? items.get(0) : v;
        CssValue second = items != null && items.size() > 1 ? items.get(1) : null;
        if (first == null) {
            return new double[] { w / 2, h / 2 };
        }
        boolean swap = isVertical(first) || (second != null && isHorizontal(second));
        CssValue hx = swap ? second : first;
        CssValue hy = swap ? first : second;
        return new double[] { position(hx, w, true), position(hy, h, false) };
    }

    /**
     * Tells whether an origin component is the keyword <code>top</code> or <code>bottom</code>.
     * <p>
     * 원점 성분이 키워드 <code>top</code> 이나 <code>bottom</code> 인지 알려 준다.
     *
     * @param v the component / 성분
     * @return true for a vertical keyword / 세로 키워드이면 true
     */
    private static boolean isVertical(CssValue v) {
        return v != null && (v.isKeyword("top") || v.isKeyword("bottom"));
    }

    /**
     * Tells whether an origin component is the keyword <code>left</code> or <code>right</code>.
     * <p>
     * 원점 성분이 키워드 <code>left</code> 나 <code>right</code> 인지 알려 준다.
     *
     * @param v the component / 성분
     * @return true for a horizontal keyword / 가로 키워드이면 true
     */
    private static boolean isHorizontal(CssValue v) {
        return v != null && (v.isKeyword("left") || v.isKeyword("right"));
    }

    /**
     * Converts one origin component to pixels (a missing component is the center).
     * <p>
     * 원점 성분 하나를 픽셀로 바꾼다(없는 성분은 가운데).
     *
     * @param v          the component or null / 성분 또는 null
     * @param size       the size of the border box along the axis / 축 방향 border box 크기
     * @param horizontal true for the horizontal axis / 가로 축이면 true
     * @return pixels / 픽셀
     */
    private static double position(CssValue v, double size, boolean horizontal) {
        if (v == null || v.isKeyword("center")) {
            return size / 2;
        }
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            if ("left".equals(k) || "top".equals(k)) {
                return 0;
            }
            if ("right".equals(k) || "bottom".equals(k)) {
                return size;
            }
        }
        return lp(v, size);
    }

    /**
     * Interpolates two computed <code>transform</code> values (CSS Transforms 1 section 15): two lists with the same number of
     * functions with the same names are interpolated function by function (numbers, lengths and angles linearly; a missing length
     * is 0 and a missing scale is 1); other pairs (and <code>none</code> against a list) are interpolated as matrices through the
     * decomposition into translation, rotation, skew and scale. The result of the second kind is a <code>matrix()</code> function.
     * <p>
     * 계산된 <code>transform</code> 값 둘을 보간한다(CSS Transforms 1 15 절): 같은 이름의 함수가 같은 수만큼 있는 목록 쌍은 함수별로 보간하고(숫자, 길이, 각도는 선형. 없는
     * 길이는 0, 없는 배율은 1), 그 밖의 쌍(과 목록 대 <code>none</code>)은 이동, 회전, 기울임, 배율로 분해한 행렬로 보간한다. 둘째 경우의 결과는 <code>matrix()</code> 함수이다.
     *
     * @param from the start value / 시작 값
     * @param to   the end value / 끝 값
     * @param p    the progress, may lie outside 0..1 for cubic-bezier overshoot / 진행도. cubic-bezier 의 넘침으로 0..1 밖일 수 있다
     * @param w    width of the reference box (for percentages in the matrix fallback) / 기준 상자 너비(행렬 대체의 백분율용)
     * @param h    height of the reference box / 기준 상자 높이
     * @return the interpolated value / 보간한 값
     */
    public static CssValue interpolate(CssValue from, CssValue to, double p, double w, double h) {
        List<CssValue> a = functions(from);
        List<CssValue> b = functions(to);
        if (a.size() == b.size() && sameNames(a, b)) {
            java.util.ArrayList<CssValue> out = new java.util.ArrayList<CssValue>(a.size());
            // 함수 쌍마다 인수를 선형으로 섞는다.
            for (int i = 0; i < a.size(); i++) {
                out.add(blend((CssFunctionValue) a.get(i), (CssFunctionValue) b.get(i), p));
            }
            return new CssList(out, false);
        }
        return blendMatrices(matrix(from, w, h), matrix(to, w, h), p);
    }

    /**
     * Returns the functions of a transform value (none is the empty list).
     * <p>
     * 변환 값의 함수를 반환한다(none 은 빈 목록).
     *
     * @param v the value / 값
     * @return the functions / 함수
     */
    private static List<CssValue> functions(CssValue v) {
        if (isNone(v)) {
            return new java.util.ArrayList<CssValue>(0);
        }
        if (v instanceof CssFunctionValue) {
            java.util.ArrayList<CssValue> one = new java.util.ArrayList<CssValue>(1);
            one.add(v);
            return one;
        }
        return v instanceof CssList ? ((CssList) v).getItems() : new java.util.ArrayList<CssValue>(0);
    }

    /**
     * Tells whether two function lists have the same function names in the same order (with none against a list, a list of identity
     * functions of the same names is made of the other, so only equal lists qualify; a pair with one empty list qualifies only when
     * both are empty).
     * <p>
     * 두 함수 목록이 같은 순서로 같은 함수 이름을 갖는지 알려 준다(둘 중 하나가 빈 목록인 쌍은 둘 다 비었을 때만 해당한다).
     *
     * @param a the first list / 첫째 목록
     * @param b the second list / 둘째 목록
     * @return true if the names are equal / 이름이 같으면 true
     */
    private static boolean sameNames(List<CssValue> a, List<CssValue> b) {
        // 함수마다 이름과 인수 개수를 비교한다.
        for (int i = 0; i < a.size(); i++) {
            if (!(a.get(i) instanceof CssFunctionValue) || !(b.get(i) instanceof CssFunctionValue)) {
                return false;
            }
            CssFunctionValue x = (CssFunctionValue) a.get(i);
            CssFunctionValue y = (CssFunctionValue) b.get(i);
            if (!x.getName().equals(y.getName()) || x.getArguments().size() != y.getArguments().size()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Blends the arguments of two functions of the same name linearly.
     * <p>
     * 같은 이름의 함수 둘의 인수를 선형으로 섞는다.
     *
     * @param x the first function / 첫째 함수
     * @param y the second function / 둘째 함수
     * @param p the progress / 진행도
     * @return the function with the blended arguments / 인수를 섞은 함수
     */
    private static CssValue blend(CssFunctionValue x, CssFunctionValue y, double p) {
        java.util.ArrayList<CssValue> args = new java.util.ArrayList<CssValue>(x.getArguments().size());
        // 인수 쌍마다 같은 형식끼리 섞는다(형식이 다르면 둘째 값을 진행도 0.5 에서 바꾼다).
        for (int i = 0; i < x.getArguments().size(); i++) {
            args.add(blendArgument(x.getArguments().get(i), y.getArguments().get(i), p));
        }
        return new CssFunctionValue(x.getName(), args);
    }

    /**
     * Blends two arguments of a transform function.
     * <p>
     * 변환 함수의 인수 둘을 섞는다.
     *
     * @param a the first argument / 첫째 인수
     * @param b the second argument / 둘째 인수
     * @param p the progress / 진행도
     * @return the blended argument / 섞은 인수
     */
    private static CssValue blendArgument(CssValue a, CssValue b, double p) {
        if (a instanceof CssNumber && b instanceof CssNumber) {
            return new CssNumber(mix(((CssNumber) a).getValue(), ((CssNumber) b).getValue(), p), false);
        }
        if (a instanceof CssLength && b instanceof CssLength) {
            return CssLength.px(mix(lp(a, 0), lp(b, 0), p));
        }
        if (a instanceof CssPercentage && b instanceof CssPercentage) {
            return new CssPercentage(mix(((CssPercentage) a).getValue(), ((CssPercentage) b).getValue(), p));
        }
        if (a instanceof CssDimension && b instanceof CssDimension) {
            return new CssDimension(mix(deg(a), deg(b), p), "deg");
        }
        return p < 0.5 ? a : b;
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
    static double mix(double a, double b, double p) {
        return a + (b - a) * p;
    }

    /**
     * Interpolates two matrices through the decomposition into translation, rotation, shear and scale (CSS Transforms 1 section 15.2,
     * the 2D case): the translation and scale are mixed linearly, the rotation along the shorter way, the shear linearly. The result is
     * a <code>matrix()</code> function.
     * <p>
     * 두 행렬을 이동, 회전, 전단, 배율로 분해해서 보간한다(CSS Transforms 1 15.2 절의 2D 경우): 이동과 배율은 선형, 회전은 짧은 쪽으로, 전단은 선형으로 섞는다.
     * 결과는 <code>matrix()</code> 함수이다.
     *
     * @param a the start matrix (null: identity) / 시작 행렬(null: 항등)
     * @param b the end matrix (null: identity) / 끝 행렬(null: 항등)
     * @param p the progress / 진행도
     * @return the matrix function / 행렬 함수
     */
    private static CssValue blendMatrices(AffineTransform a, AffineTransform b, double p) {
        double[] da = decompose(a == null ? new AffineTransform() : a);
        double[] db = decompose(b == null ? new AffineTransform() : b);
        // 회전은 짧은 쪽으로 돌도록 차이를 -180..180 도 안으로 맞춘다.
        double dAngle = db[2] - da[2];
        while (dAngle > Math.PI) {
            dAngle -= 2 * Math.PI;
        }
        // 차이가 -180 도보다 작으면 한 바퀴를 더한다.
        while (dAngle < -Math.PI) {
            dAngle += 2 * Math.PI;
        }
        double tx = mix(da[0], db[0], p);
        double ty = mix(da[1], db[1], p);
        double ang = da[2] + dAngle * p;
        double sh = mix(da[3], db[3], p);
        double sx = mix(da[4], db[4], p);
        double sy = mix(da[5], db[5], p);
        AffineTransform r = new AffineTransform();
        r.translate(tx, ty);
        r.rotate(ang);
        r.concatenate(new AffineTransform(1, 0, sh, 1, 0, 0));
        r.scale(sx, sy);
        double[] m = new double[6];
        r.getMatrix(m);
        java.util.ArrayList<CssValue> args = new java.util.ArrayList<CssValue>(6);
        // 행렬의 여섯 값을 matrix() 인수로 옮긴다.
        for (int i = 0; i < 6; i++) {
            args.add(new CssNumber(m[i], false));
        }
        return new CssFunctionValue("matrix", args);
    }

    /**
     * Decomposes a 2D matrix into {tx, ty, rotation (radians), shear, sx, sy} (the 2D decomposition of CSS Transforms 1 appendix).
     * <p>
     * 2D 행렬을 {tx, ty, 회전(라디안), 전단, sx, sy} 로 분해한다(CSS Transforms 1 부록의 2D 분해).
     *
     * @param m the matrix / 행렬
     * @return the components / 성분
     */
    private static double[] decompose(AffineTransform m) {
        double a = m.getScaleX();
        double b = m.getShearY();
        double c = m.getShearX();
        double d = m.getScaleY();
        double sx = Math.sqrt(a * a + b * b);
        if (sx != 0) {
            a /= sx;
            b /= sx;
        }
        double shear = a * c + b * d;
        c -= a * shear;
        d -= b * shear;
        double sy = Math.sqrt(c * c + d * d);
        if (sy != 0) {
            c /= sy;
            d /= sy;
            shear /= sy;
        }
        // 행렬식이 음수이면 한 축이 뒤집힌 것이다.
        if (a * d - b * c < 0) {
            sx = -sx;
            a = -a;
            b = -b;
            shear = -shear;
        }
        return new double[] { m.getTranslateX(), m.getTranslateY(), Math.atan2(b, a), shear, sx, sy };
    }
}
