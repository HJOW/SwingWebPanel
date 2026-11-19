package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * An easing (timing) function (plan item P10-07, CSS Easing Functions Level 1, https://www.w3.org/TR/css-easing-1/): a function from
 * the input progress of an animation or a transition to the output progress. The keywords <code>linear</code>, <code>ease</code>,
 * <code>ease-in</code>, <code>ease-out</code>, <code>ease-in-out</code>, <code>step-start</code>, <code>step-end</code> and the functions
 * <code>cubic-bezier()</code> and <code>steps()</code> (with the jump terms <code>jump-start</code>, <code>jump-end</code>,
 * <code>jump-none</code>, <code>jump-both</code>, <code>start</code>, <code>end</code>) are read from the computed value. The
 * <code>linear()</code> function with stops is not interpreted: it behaves as the linear easing. A cubic Bezier output may leave the
 * range 0..1 (overshoot) because only the x values of the control points are limited to 0..1. An instance is immutable.
 * <p>
 * 완화(타이밍) 함수(계획 P10-07, CSS Easing Functions Level 1, https://www.w3.org/TR/css-easing-1/): 애니메이션이나 전환의 입력 진행도를 출력 진행도로 바꾸는 함수.
 * 키워드 <code>linear</code>, <code>ease</code>, <code>ease-in</code>, <code>ease-out</code>, <code>ease-in-out</code>, <code>step-start</code>, <code>step-end</code> 와
 * 함수 <code>cubic-bezier()</code>, <code>steps()</code>(점프 항 <code>jump-start</code>, <code>jump-end</code>, <code>jump-none</code>, <code>jump-both</code>,
 * <code>start</code>, <code>end</code>)를 계산 값에서 읽는다. 정지점이 있는 <code>linear()</code> 함수는 해석하지 않고 선형 완화처럼 동작한다. 3차 베지어 출력은
 * 0..1 을 벗어날 수 있다(제어점의 x 만 0..1 로 제한되기 때문). 인스턴스는 불변이다.
 */
public abstract class Easing {

    /** The linear easing. 선형 완화. */
    public static final Easing LINEAR = new Easing() {
        /**
         * Returns the input.
         * <p>
         * 입력을 그대로 반환한다.
         *
         * @param t the input progress / 입력 진행도
         * @return the same value / 같은 값
         */
        public double at(double t) {
            return t;
        }
    };

    /** The default easing <code>ease</code>: <code>cubic-bezier(0.25, 0.1, 0.25, 1)</code>. 기본 완화 <code>ease</code>. */
    public static final Easing EASE = new Bezier(0.25, 0.1, 0.25, 1.0);

    /**
     * Creates an easing (only this package creates subclasses).
     * <p>
     * 완화를 만든다(이 패키지만 하위 클래스를 만든다).
     */
    Easing() {
    }

    /**
     * Returns the output progress for an input progress.
     * <p>
     * 입력 진행도에 대한 출력 진행도를 반환한다.
     *
     * @param t the input progress, normally between 0 and 1 / 입력 진행도. 보통 0~1
     * @return the output progress / 출력 진행도
     */
    public abstract double at(double t);

    /**
     * Returns the output progress for an input progress with the before flag of CSS Easing Level 1 (used by Web Animations 4.10.1): only the step
     * functions look at the flag, the others ignore it.
     * <p>
     * CSS Easing Level 1 의 before 플래그와 함께 입력 진행도에 대한 출력 진행도를 반환한다(Web Animations 4.10.1 이 쓴다): 계단 함수만 플래그를 보고 나머지는
     * 무시한다.
     *
     * @param t      the input progress / 입력 진행도
     * @param before the before flag / before 플래그
     * @return the output progress / 출력 진행도
     */
    public double at(double t, boolean before) {
        return at(t);
    }

    /**
     * Reads an easing from a computed value: a keyword, a <code>cubic-bezier()</code> or <code>steps()</code> function. An unknown value
     * is the default easing <code>ease</code>.
     * <p>
     * 계산 값에서 완화를 읽는다: 키워드, <code>cubic-bezier()</code>, <code>steps()</code> 함수. 모르는 값은 기본 완화 <code>ease</code> 이다.
     *
     * @param v the value, may be null / 값. null 가능
     * @return the easing / 완화
     */
    public static Easing of(CssValue v) {
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            if ("linear".equals(k)) {
                return LINEAR;
            }
            if ("ease-in".equals(k)) {
                return new Bezier(0.42, 0, 1, 1);
            }
            if ("ease-out".equals(k)) {
                return new Bezier(0, 0, 0.58, 1);
            }
            if ("ease-in-out".equals(k)) {
                return new Bezier(0.42, 0, 0.58, 1);
            }
            if ("step-start".equals(k)) {
                return new Steps(1, "jump-start");
            }
            if ("step-end".equals(k)) {
                return new Steps(1, "jump-end");
            }
            return EASE;
        }
        if (v instanceof CssFunctionValue) {
            CssFunctionValue f = (CssFunctionValue) v;
            List<CssValue> a = f.getArguments();
            if (f.isNamed("cubic-bezier") && a.size() == 4 && all(a, 4)) {
                return new Bezier(num(a.get(0)), num(a.get(1)), num(a.get(2)), num(a.get(3)));
            }
            if (f.isNamed("steps") && a.size() >= 1 && a.get(0) instanceof CssNumber) {
                int n = (int) Math.max(1, Math.round(num(a.get(0))));
                String jump = a.size() > 1 && a.get(1) instanceof CssKeyword ? ((CssKeyword) a.get(1)).getName() : "jump-end";
                return new Steps(n, jump);
            }
            if (f.isNamed("linear")) {
                return LINEAR;
            }
        }
        return EASE;
    }

    /**
     * Tells whether the first <code>n</code> arguments are numbers.
     * <p>
     * 앞의 <code>n</code> 개의 인수가 숫자인지 알려 준다.
     *
     * @param a the arguments / 인수
     * @param n the count / 개수
     * @return true if all are numbers / 모두 숫자이면 true
     */
    private static boolean all(List<CssValue> a, int n) {
        // 인수마다 숫자인지 확인한다.
        for (int i = 0; i < n; i++) {
            if (!(a.get(i) instanceof CssNumber)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a number argument.
     * <p>
     * 숫자 인수를 읽는다.
     *
     * @param v the argument / 인수
     * @return the number / 숫자
     */
    private static double num(CssValue v) {
        return ((CssNumber) v).getValue();
    }

    /**
     * A cubic Bezier easing through (0,0), (x1,y1), (x2,y2), (1,1) (CSS Easing 1 section 2.2): the output for an input is the y value of
     * the point of the curve whose x value is the input, found with Newton's method and bisection as a fallback.
     * <p>
     * (0,0), (x1,y1), (x2,y2), (1,1) 을 지나는 3차 베지어 완화(CSS Easing 1 2.2 절): 입력에 대한 출력은 x 값이 입력인 곡선 위 점의 y 값이며, 뉴턴 방법으로 찾고
     * 실패하면 이분법을 쓴다.
     */
    static final class Bezier extends Easing {
        /** The control points (x limited to 0..1). 제어점(x 는 0..1 로 제한). */
        private final double x1;
        /** y of the first control point. 첫째 제어점의 y. */
        private final double y1;
        /** x of the second control point. 둘째 제어점의 x. */
        private final double x2;
        /** y of the second control point. 둘째 제어점의 y. */
        private final double y2;
        /** Polynomial coefficients of x(s). x(s) 의 다항식 계수. */
        private final double ax;
        /** Coefficient b of x(s). x(s) 의 계수 b. */
        private final double bx;
        /** Coefficient c of x(s). x(s) 의 계수 c. */
        private final double cx;
        /** Coefficient a of y(s). y(s) 의 계수 a. */
        private final double ay;
        /** Coefficient b of y(s). y(s) 의 계수 b. */
        private final double by;
        /** Coefficient c of y(s). y(s) 의 계수 c. */
        private final double cy;

        /**
         * Creates the curve; the x values of the control points are limited to 0..1.
         * <p>
         * 곡선을 만든다. 제어점의 x 값은 0..1 로 제한한다.
         *
         * @param x1 x of the first control point / 첫째 제어점의 x
         * @param y1 y of the first control point / 첫째 제어점의 y
         * @param x2 x of the second control point / 둘째 제어점의 x
         * @param y2 y of the second control point / 둘째 제어점의 y
         */
        Bezier(double x1, double y1, double x2, double y2) {
            double cx1 = Math.max(0, Math.min(1, x1));
            double cx2 = Math.max(0, Math.min(1, x2));
            this.x1 = cx1;
            this.y1 = y1;
            this.x2 = cx2;
            this.y2 = y2;
            this.cx = 3 * cx1;
            this.bx = 3 * (cx2 - cx1) - cx;
            this.ax = 1 - cx - bx;
            this.cy = 3 * y1;
            this.by = 3 * (y2 - y1) - cy;
            this.ay = 1 - cy - by;
        }

        /**
         * Returns x(s).
         * <p>
         * x(s) 를 반환한다.
         *
         * @param s the curve parameter / 곡선 매개변수
         * @return x / x
         */
        private double x(double s) {
            return ((ax * s + bx) * s + cx) * s;
        }

        /**
         * Returns the output for an input progress: before 0 and after 1 the curve is extended by the line to the end point (the
         * slope of the first and last control segments), as the specification does.
         * <p>
         * 입력 진행도에 대한 출력을 반환한다: 0 앞과 1 뒤에서는 규격처럼 끝점으로 가는 직선(처음과 마지막 제어 선분의 기울기)으로 곡선을 연장한다.
         *
         * @param t the input progress / 입력 진행도
         * @return the output progress / 출력 진행도
         */
        public double at(double t) {
            // 0 앞과 1 뒤는 끝점에서 처음/마지막 제어 선분의 기울기로 연장한다.
            if (t <= 0) {
                double slope = x1 > 0 ? y1 / x1 : (x2 > 0 ? y2 / x2 : 0);
                return t * slope;
            }
            if (t >= 1) {
                double slope = x2 < 1 ? (1 - y2) / (1 - x2) : (x1 < 1 ? (1 - y1) / (1 - x1) : 0);
                return 1 + (t - 1) * slope;
            }
            double s = t;
            // 뉴턴 방법으로 x(s) = t 를 푼다.
            for (int i = 0; i < 8; i++) {
                double err = x(s) - t;
                if (Math.abs(err) < 1e-7) {
                    return ((ay * s + by) * s + cy) * s;
                }
                double d = (3 * ax * s + 2 * bx) * s + cx;
                if (Math.abs(d) < 1e-6) {
                    break;
                }
                s -= err / d;
            }
            double lo = 0;
            double hi = 1;
            s = t;
            // 뉴턴 방법이 실패하면 이분법으로 x(s) = t 를 푼다.
            for (int i = 0; i < 40; i++) {
                double v = x(s);
                if (Math.abs(v - t) < 1e-7) {
                    break;
                }
                if (v < t) {
                    lo = s;
                } else {
                    hi = s;
                }
                s = (lo + hi) / 2;
            }
            return ((ay * s + by) * s + cy) * s;
        }

    }

    /**
     * A step easing (CSS Easing 1 section 3): the output stays on a step and jumps; the jump term tells where the jumps are.
     * <p>
     * 계단 완화(CSS Easing 1 3 절): 출력은 한 단계에 머물다 점프한다. 점프 항이 점프의 위치를 정한다.
     */
    static final class Steps extends Easing {
        /** The number of steps (the intervals). 단계(구간)의 수. */
        private final int n;
        /** The jump term. 점프 항. */
        private final String jump;

        /**
         * Creates the easing.
         * <p>
         * 완화를 만든다.
         *
         * @param n    the number of steps, at least 1 / 단계 수. 1 이상
         * @param jump the jump term / 점프 항
         */
        Steps(int n, String jump) {
            this.n = n;
            this.jump = jump;
        }

        /**
         * Returns the output for an input progress.
         * <p>
         * 입력 진행도에 대한 출력을 반환한다.
         *
         * @param t the input progress / 입력 진행도
         * @return the output progress / 출력 진행도
         */
        public double at(double t) {
            boolean start = "jump-start".equals(jump) || "start".equals(jump) || "jump-both".equals(jump);
            int jumps = "jump-both".equals(jump) ? n + 1 : ("jump-none".equals(jump) ? Math.max(1, n - 1) : n);
            double step = Math.floor(t * n);
            if (start) {
                step += 1;
            }
            double r = step / jumps;
            // 입력이 0..1 안이면 출력도 0..1 안으로 제한한다.
            if (t >= 0 && t <= 1) {
                r = Math.max(0, Math.min(1, r));
            }
            return r;
        }

        /**
         * Returns the output for an input progress with the before flag (CSS Easing Level 1 section 3.1, "the step easing function"): at a step
         * boundary the before flag keeps the step before the jump.
         * <p>
         * before 플래그와 함께 입력 진행도에 대한 출력을 반환한다(CSS Easing Level 1 3.1 절, "step easing function"): 계단 경계에서 before 플래그는 점프 전의
         * 단계를 유지한다.
         *
         * @param t      the input progress / 입력 진행도
         * @param before the before flag / before 플래그
         * @return the output progress / 출력 진행도
         */
        public double at(double t, boolean before) {
            boolean start = "jump-start".equals(jump) || "start".equals(jump) || "jump-both".equals(jump);
            int jumps = "jump-both".equals(jump) ? n + 1 : ("jump-none".equals(jump) ? Math.max(1, n - 1) : n);
            double step = Math.floor(t * n);
            if (start) {
                step += 1;
            }
            // before 플래그가 있고 입력이 정확히 계단 경계이면 한 단계 낮춘다.
            if (before && (t * n) % 1 == 0) {
                step -= 1;
            }
            if (t >= 0 && step < 0) {
                step = 0;
            }
            if (t <= 1 && step > jumps) {
                step = jumps;
            }
            return step / jumps;
        }
    }
}
