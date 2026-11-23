package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * A node of a <code>calc()</code> expression tree. The tree knows its result type (number, length, percentage, ...), can be evaluated to a
 * canonical number (pixels, degrees, seconds, ...) when the relative units and the percentage base are known, and can be folded so that every
 * part that does not depend on a percentage becomes a constant.
 * <p>
 * <code>calc()</code> 식 트리의 노드. 트리는 결과 형식(숫자, 길이, 백분율 등)을 알고, 상대 단위와 백분율 기준을 알면 기준 숫자(픽셀, 도, 초 등)로 계산할 수 있으며,
 * 백분율에 의존하지 않는 부분은 모두 상수로 접을 수 있다.
 */
abstract class CalcNode {

    /** Type: plain number. / 형식: 일반 숫자. */
    static final int NUMBER = 0;
    /** Type: length. / 형식: 길이. */
    static final int LENGTH = 1;
    /** Type: percentage. / 형식: 백분율. */
    static final int PERCENT = 2;
    /** Type: length mixed with percentage. / 형식: 길이와 백분율의 혼합. */
    static final int LENGTH_PERCENT = 3;
    /** Type: angle. / 형식: 각도. */
    static final int ANGLE = 4;
    /** Type: time. / 형식: 시간. */
    static final int TIME = 5;
    /** Type: frequency. / 형식: 주파수. */
    static final int FREQUENCY = 6;
    /** Type: resolution. / 형식: 해상도. */
    static final int RESOLUTION = 7;
    /** Type: invalid combination. / 형식: 올바르지 않은 조합. */
    static final int INVALID = -1;

    /**
     * Returns the result type of the node.
     * <p>
     * 노드의 결과 형식을 반환한다.
     *
     * @return type constant / 형식 상수
     */
    abstract int type();

    /**
     * Evaluates the node to a number in the canonical unit of its type.
     * <p>
     * 노드를 그 형식의 기준 단위 숫자로 계산한다.
     *
     * @param lc      reference sizes for relative lengths / 상대 길이의 기준 크기
     * @param pctBase the length a percentage of 100 stands for, or NaN if unknown / 100% 가 뜻하는 길이. 모르면 NaN
     * @return the value (NaN if it depends on an unknown percentage base) / 값(알 수 없는 백분율 기준에 의존하면 NaN)
     */
    abstract double eval(LengthContext lc, double pctBase);

    /**
     * Tells whether the node contains a percentage.
     * <p>
     * 노드가 백분율을 포함하는지 알려 준다.
     *
     * @return true if a percentage occurs / 백분율이 있으면 true
     */
    abstract boolean hasPercent();

    /**
     * Folds the parts that do not depend on a percentage into constants.
     * <p>
     * 백분율에 의존하지 않는 부분을 상수로 접는다.
     *
     * @param lc reference sizes / 기준 크기
     * @return the folded tree / 접은 트리
     */
    abstract CalcNode fold(LengthContext lc);

    /**
     * Tells whether the node contains an <code>anchor()</code> or <code>anchor-size()</code> function (CSS Anchor Positioning 1), which is only known when
     * the box is positioned.
     * <p>
     * 노드가 <code>anchor()</code> 나 <code>anchor-size()</code> 함수(CSS Anchor Positioning 1)를 포함하는지 알려 준다. 그 값은 상자를 배치할 때에야 알 수 있다.
     *
     * @return true if there is such a function / 그런 함수가 있으면 true
     */
    boolean hasAnchor() {
        return false;
    }

    /**
     * Replaces the anchor functions by the lengths that they resolve to.
     * <p>
     * anchor 함수를 그것이 푸는 길이로 바꾼다.
     *
     * @param r the resolver / 해석기
     * @return the new tree, or null if an anchor function cannot be resolved and has no usable fallback / 새 트리. anchor 함수를 풀 수 없고 쓸 수 있는 대체값도 없으면 null
     */
    CalcNode substitute(CssCalc.AnchorResolver r) {
        return this;
    }

    /**
     * Rewrites the anchor functions, which stay anchor functions (the position fallback tactics of CSS Anchor Positioning 1 section 6.5.2 change their sides).
     * <p>
     * anchor 함수를 고쳐 쓴다. 고쳐 쓴 것도 anchor 함수로 남는다(CSS Anchor Positioning 1 6.5.2 절의 위치 대안 전술이 변을 바꾼다).
     *
     * @param m the mapper / 변환기
     * @return the new tree / 새 트리
     */
    CalcNode mapAnchors(CssCalc.FunctionMapper m) {
        return this;
    }

    /**
     * An <code>anchor()</code> or <code>anchor-size()</code> function inside a math expression (CSS Anchor Positioning 1 sections 3.2 and 5.1: the functions can be
     * used in <code>calc()</code> and the other math functions). It is a leaf of the type length-percentage that has no value until the box is positioned
     * ({@link #eval} gives NaN); it counts as depending on the layout like a percentage does, so that the folding of the computed value leaves it alone.
     * <p>
     * 수식 안의 <code>anchor()</code> 나 <code>anchor-size()</code> 함수(CSS Anchor Positioning 1 3.2, 5.1 절: 함수는 <code>calc()</code> 와 다른 수학 함수 안에서 쓸 수
     * 있다). 상자를 배치하기 전에는 값이 없는(NaN) 길이-백분율 형식의 잎이다. 백분율처럼 레이아웃에 의존하는 것으로 세어 계산 값을 접을 때 그대로 둔다.
     */
    static final class Anchor extends CalcNode {
        /** The function with its arguments. 인수를 가진 함수. */
        final CssFunctionValue fn;

        /**
         * Creates the leaf.
         * <p>
         * 잎을 만든다.
         *
         * @param fn the function / 함수
         */
        Anchor(CssFunctionValue fn) {
            this.fn = fn;
        }

        /** Returns length-percentage. length-percentage 를 반환한다. */
        int type() {
            return LENGTH_PERCENT;
        }

        /** Has no value before the box is positioned. 상자를 배치하기 전에는 값이 없다. */
        double eval(LengthContext lc, double pctBase) {
            return Double.NaN;
        }

        /** Counts as layout dependent. 레이아웃에 의존하는 것으로 센다. */
        boolean hasPercent() {
            return true;
        }

        /** Stays as it is. 그대로 둔다. */
        CalcNode fold(LengthContext lc) {
            return this;
        }

        /** Is an anchor function. anchor 함수이다. */
        boolean hasAnchor() {
            return true;
        }

        /** Replaces the function by what the resolver gives (a length in pixels or a percentage). 해석기가 주는 것(픽셀 길이나 백분율)으로 함수를 바꾼다. */
        CalcNode substitute(CssCalc.AnchorResolver r) {
            CssValue v = r.resolve(fn);
            if (v instanceof CssPercentage) {
                return new Pct(((CssPercentage) v).getValue());
            }
            if (v instanceof CssLength) {
                return new Dim(((CssLength) v).absolutePx(), "px", LENGTH);
            }
            return null;
        }

        /** Rewrites the function. 함수를 고쳐 쓴다. */
        CalcNode mapAnchors(CssCalc.FunctionMapper m) {
            return new Anchor(m.map(fn));
        }

        /** Serializes the function. 함수를 직렬화한다. */
        String text(int prec) {
            return fn.toCssText();
        }
    }

    /**
     * Serializes the node.
     * <p>
     * 노드를 직렬화한다.
     *
     * @param prec precedence of the context: 0 top, 1 inside a sum, 2 inside a product / 문맥의 우선순위: 0 최상위, 1 합 안, 2 곱 안
     * @return text / 문자열
     */
    abstract String text(int prec);

    /**
     * Merges two types for addition, subtraction and the arguments of min, max and clamp.
     * <p>
     * 덧셈, 뺄셈, min/max/clamp 인수를 위해 두 형식을 합친다.
     *
     * @param a first type / 첫 형식
     * @param b second type / 둘째 형식
     * @return the common type or INVALID / 공통 형식 또는 INVALID
     */
    static int merge(int a, int b) {
        if (a == INVALID || b == INVALID) {
            return INVALID;
        }
        if (a == b) {
            return a;
        }
        boolean la = a == LENGTH || a == PERCENT || a == LENGTH_PERCENT;
        boolean lb = b == LENGTH || b == PERCENT || b == LENGTH_PERCENT;
        return la && lb ? LENGTH_PERCENT : INVALID;
    }

    /**
     * A plain number.
     * <p>
     * 일반 숫자.
     */
    static final class Num extends CalcNode {
        /** The numeric value. / 숫자 값. */
        final double v;

        /**
         * Creates a number node.
         * <p>
         * 숫자 노드를 만든다.
         *
         * @param v value / 값
         */
        Num(double v) {
            this.v = v;
        }

        /** Returns the result type. 결과 형식을 반환한다. */
        int type() {
            return NUMBER;
        }

        /** Evaluates the node. 노드를 계산한다. */
        double eval(LengthContext lc, double pctBase) {
            return v;
        }

        /** Tells whether a percentage occurs. 백분율이 있는지 알려 준다. */
        boolean hasPercent() {
            return false;
        }

        /** Folds percentage-free parts. 백분율 없는 부분을 접는다. */
        CalcNode fold(LengthContext lc) {
            return this;
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text(int prec) {
            return Double.isNaN(v) ? "NaN" : Double.isInfinite(v) ? (v > 0 ? "infinity" : "-infinity") : CssEscape.number(v);
        }
    }

    /**
     * A dimension: a length, angle, time, frequency or resolution with its unit.
     * <p>
     * 차원 값: 단위가 붙은 길이, 각도, 시간, 주파수, 해상도.
     */
    static final class Dim extends CalcNode {
        /** The number as written. / 쓰인 숫자. */
        final double v;
        /** Lower-case unit. / 소문자 단위. */
        final String unit;
        /** Type of the unit. / 단위의 형식. */
        final int type;

        /**
         * Creates a dimension node.
         * <p>
         * 차원 노드를 만든다.
         *
         * @param v    number / 숫자
         * @param unit lower-case unit / 소문자 단위
         * @param type type constant / 형식 상수
         */
        Dim(double v, String unit, int type) {
            this.v = v;
            this.unit = unit;
            this.type = type;
        }

        /**
         * Returns the type constant for a unit kind, or INVALID.
         * <p>
         * 단위 종류에 맞는 형식 상수를 반환한다. 없으면 INVALID.
         *
         * @param unit unit name / 단위 이름
         * @return type constant / 형식 상수
         */
        static int typeOfUnit(String unit) {
            int k = CssUnits.kindOf(unit);
            if (k == CssUnits.ABSOLUTE_LENGTH || k == CssUnits.FONT_RELATIVE_LENGTH || k == CssUnits.VIEWPORT_LENGTH || k == CssUnits.CONTAINER_LENGTH) {
                return LENGTH;
            }
            if (k == CssUnits.ANGLE) {
                return ANGLE;
            }
            if (k == CssUnits.TIME) {
                return TIME;
            }
            if (k == CssUnits.FREQUENCY) {
                return FREQUENCY;
            }
            if (k == CssUnits.RESOLUTION) {
                return RESOLUTION;
            }
            return INVALID;
        }

        /** Returns the result type. 결과 형식을 반환한다. */
        int type() {
            return type;
        }

        /** Evaluates the node. 노드를 계산한다. */
        double eval(LengthContext lc, double pctBase) {
            switch (type) {
            case LENGTH:
                return lc.toPx(v, unit);
            case ANGLE:
                return CssUnits.angleToDegrees(v, unit);
            case TIME:
                return CssUnits.timeToSeconds(v, unit);
            case FREQUENCY:
                return unit.equals("khz") ? v * 1000.0 : v;
            default:
                return CssUnits.resolutionToDppx(v, unit);
            }
        }

        /** Tells whether a percentage occurs. 백분율이 있는지 알려 준다. */
        boolean hasPercent() {
            return false;
        }

        /** Folds percentage-free parts. 백분율 없는 부분을 접는다. */
        CalcNode fold(LengthContext lc) {
            double c = eval(lc, Double.NaN);
            String u = type == LENGTH ? "px" : type == ANGLE ? "deg" : type == TIME ? "s" : type == FREQUENCY ? "hz" : "dppx";
            return new Dim(c, u, type);
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text(int prec) {
            return CssEscape.number(v) + unit;
        }
    }

    /**
     * A percentage operand such as <code>50%</code>.
     * <p>
     * <code>50%</code> 같은 백분율 피연산자.
     */
    static final class Pct extends CalcNode {
        /** The percentage number. / 백분율 숫자. */
        final double v;

        /**
         * Creates a percentage node.
         * <p>
         * 백분율 노드를 만든다.
         *
         * @param v number before the percent sign / 퍼센트 기호 앞의 숫자
         */
        Pct(double v) {
            this.v = v;
        }

        /** Returns the result type. 결과 형식을 반환한다. */
        int type() {
            return PERCENT;
        }

        /** Evaluates the node. 노드를 계산한다. */
        double eval(LengthContext lc, double pctBase) {
            return Double.isNaN(pctBase) ? Double.NaN : v * pctBase / 100.0;
        }

        /** Tells whether a percentage occurs. 백분율이 있는지 알려 준다. */
        boolean hasPercent() {
            return true;
        }

        /** Folds percentage-free parts. 백분율 없는 부분을 접는다. */
        CalcNode fold(LengthContext lc) {
            return this;
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text(int prec) {
            return CssEscape.number(v) + "%";
        }
    }

    /**
     * A binary operation: addition, subtraction, multiplication or division.
     * <p>
     * 이항 연산: 덧셈, 뺄셈, 곱셈, 나눗셈.
     */
    static final class Bin extends CalcNode {
        /** The operator character. / 연산자 문자. */
        final char op;
        /** Left operand. / 왼쪽 피연산자. */
        final CalcNode l;
        /** Right operand. / 오른쪽 피연산자. */
        final CalcNode r;

        /**
         * Creates an operation node.
         * <p>
         * 연산 노드를 만든다.
         *
         * @param op operator / 연산자
         * @param l  left / 왼쪽
         * @param r  right / 오른쪽
         */
        Bin(char op, CalcNode l, CalcNode r) {
            this.op = op;
            this.l = l;
            this.r = r;
        }

        /** Returns the result type. 결과 형식을 반환한다. */
        int type() {
            int a = l.type();
            int b = r.type();
            if (op == '+' || op == '-') {
                return merge(a, b);
            }
            if (op == '*') {
                return a == NUMBER ? b : b == NUMBER ? a : INVALID;
            }
            return b == NUMBER ? a : INVALID;
        }

        /** Evaluates the node. 노드를 계산한다. */
        double eval(LengthContext lc, double pctBase) {
            double a = l.eval(lc, pctBase);
            double b = r.eval(lc, pctBase);
            switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            default:
                return a / b;
            }
        }

        /** Tells whether a percentage occurs. 백분율이 있는지 알려 준다. */
        boolean hasPercent() {
            return l.hasPercent() || r.hasPercent();
        }

        /** Folds percentage-free parts. 백분율 없는 부분을 접는다. */
        CalcNode fold(LengthContext lc) {
            CalcNode a = l.fold(lc);
            CalcNode b = r.fold(lc);
            if (!a.hasPercent() && !b.hasPercent()) {
                return constant(new Bin(op, a, b), lc);
            }
            return new Bin(op, a, b);
        }

        /** Tells whether an operand has an anchor function. 피연산자에 anchor 함수가 있는지 알려 준다. */
        boolean hasAnchor() {
            return l.hasAnchor() || r.hasAnchor();
        }

        /** Replaces the anchor functions of the operands. 피연산자의 anchor 함수를 바꾼다. */
        CalcNode substitute(CssCalc.AnchorResolver res) {
            CalcNode a = l.substitute(res);
            CalcNode b = r.substitute(res);
            return a == null || b == null ? null : new Bin(op, a, b);
        }

        /** Rewrites the anchor functions of the operands. 피연산자의 anchor 함수를 고쳐 쓴다. */
        CalcNode mapAnchors(CssCalc.FunctionMapper m) {
            return new Bin(op, l.mapAnchors(m), r.mapAnchors(m));
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text(int prec) {
            int mine = op == '+' || op == '-' ? 1 : 2;
            String s = l.text(mine) + " " + op + " " + r.text(mine + (op == '-' || op == '/' ? 1 : 0));
            return prec > mine ? "(" + s + ")" : s;
        }
    }

    /**
     * A math function: min, max, clamp, abs, sign, round, mod, rem, sin, cos, tan, asin, acos, atan, atan2, pow, sqrt, hypot, log, exp.
     * <p>
     * 수학 함수: min, max, clamp, abs, sign, round, mod, rem, sin, cos, tan, asin, acos, atan, atan2, pow, sqrt, hypot, log, exp.
     */
    static final class Fn extends CalcNode {
        /** Function name in lower case. / 소문자 함수 이름. */
        final String name;
        /** Arguments (for round the strategy is the first argument as a Num holding 0..3). / 인수(round 의 전략은 첫 인수로 0..3 을 담은 Num). */
        final CalcNode[] args;

        /**
         * Creates a function node.
         * <p>
         * 함수 노드를 만든다.
         *
         * @param name function name / 함수 이름
         * @param args arguments / 인수
         */
        Fn(String name, List<CalcNode> args) {
            this.name = name;
            this.args = args.toArray(new CalcNode[args.size()]);
        }

        /**
         * Creates a function node from an array.
         * <p>
         * 배열로 함수 노드를 만든다.
         *
         * @param name function name / 함수 이름
         * @param args arguments / 인수
         */
        Fn(String name, CalcNode[] args) {
            this.name = name;
            this.args = args.clone();
        }

        /** Returns the result type. 결과 형식을 반환한다. */
        int type() {
            int t = args[0].type();
            if (name.equals("min") || name.equals("max") || name.equals("clamp") || name.equals("hypot") || name.equals("mod")
                    || name.equals("rem") || name.equals("round")) {
                int from = name.equals("round") ? 1 : 0;
                t = args[from].type();
                // 모든 인수 형식이 호환되어야 한다.
                for (int i = from + 1; i < args.length; i++) {
                    t = merge(t, args[i].type());
                }
                return t;
            }
            if (name.equals("abs")) {
                return t;
            }
            if (name.equals("sign") || name.equals("pow") || name.equals("sqrt") || name.equals("log") || name.equals("exp")) {
                boolean ok = true;
                // 이 함수들은 숫자 인수만 받는다(sign 은 어떤 형식이든 받는다).
                for (int i = 0; i < args.length; i++) {
                    ok = ok && (name.equals("sign") || args[i].type() == NUMBER);
                }
                return ok ? NUMBER : INVALID;
            }
            if (name.equals("sin") || name.equals("cos") || name.equals("tan")) {
                return t == NUMBER || t == ANGLE ? NUMBER : INVALID;
            }
            if (name.equals("asin") || name.equals("acos") || name.equals("atan")) {
                return t == NUMBER ? ANGLE : INVALID;
            }
            if (name.equals("atan2")) {
                return merge(t, args[1].type()) == INVALID ? INVALID : ANGLE;
            }
            return INVALID;
        }

        /** Evaluates the node. 노드를 계산한다. */
        double eval(LengthContext lc, double pctBase) {
            double[] v = new double[args.length];
            // 인수를 모두 계산한다(round 의 전략 인수는 상수).
            for (int i = 0; i < args.length; i++) {
                v[i] = args[i].eval(lc, pctBase);
            }
            if (name.equals("min")) {
                double m = v[0];
                // 가장 작은 값을 찾는다.
                for (int i = 1; i < v.length; i++) {
                    m = Math.min(m, v[i]);
                }
                return m;
            }
            if (name.equals("max")) {
                double m = v[0];
                // 가장 큰 값을 찾는다.
                for (int i = 1; i < v.length; i++) {
                    m = Math.max(m, v[i]);
                }
                return m;
            }
            if (name.equals("clamp")) {
                return Math.max(v[0], Math.min(v[1], v[2]));
            }
            if (name.equals("abs")) {
                return Math.abs(v[0]);
            }
            if (name.equals("sign")) {
                return Math.signum(v[0]);
            }
            if (name.equals("sin")) {
                return Math.sin(radians(v[0], args[0]));
            }
            if (name.equals("cos")) {
                return Math.cos(radians(v[0], args[0]));
            }
            if (name.equals("tan")) {
                return Math.tan(radians(v[0], args[0]));
            }
            if (name.equals("asin")) {
                return Math.toDegrees(Math.asin(v[0]));
            }
            if (name.equals("acos")) {
                return Math.toDegrees(Math.acos(v[0]));
            }
            if (name.equals("atan")) {
                return Math.toDegrees(Math.atan(v[0]));
            }
            if (name.equals("atan2")) {
                return Math.toDegrees(Math.atan2(v[0], v[1]));
            }
            if (name.equals("pow")) {
                return Math.pow(v[0], v[1]);
            }
            if (name.equals("sqrt")) {
                return Math.sqrt(v[0]);
            }
            if (name.equals("hypot")) {
                double s = 0;
                // 제곱의 합을 구한다.
                for (int i = 0; i < v.length; i++) {
                    s += v[i] * v[i];
                }
                return Math.sqrt(s);
            }
            if (name.equals("log")) {
                return v.length == 2 ? Math.log(v[0]) / Math.log(v[1]) : Math.log(v[0]);
            }
            if (name.equals("exp")) {
                return Math.exp(v[0]);
            }
            if (name.equals("mod")) {
                double r = v[0] % v[1];
                return r != 0 && (r < 0) != (v[1] < 0) ? r + v[1] : r;
            }
            if (name.equals("rem")) {
                return v[0] % v[1];
            }
            return round((int) v[0], v[1], v[2]);
        }

        /**
         * Converts a trigonometric argument to radians (numbers are radians, angles are degrees).
         * <p>
         * 삼각 함수 인수를 라디안으로 바꾼다(숫자는 라디안, 각도는 도).
         *
         * @param v   evaluated value / 계산한 값
         * @param arg the argument node / 인수 노드
         * @return radians / 라디안
         */
        private static double radians(double v, CalcNode arg) {
            return arg.type() == ANGLE ? Math.toRadians(v) : v;
        }

        /**
         * Implements <code>round()</code>.
         * <p>
         * <code>round()</code> 를 구현한다.
         *
         * @param strategy 0 nearest, 1 up, 2 down, 3 to-zero / 0 nearest, 1 up, 2 down, 3 to-zero
         * @param a        the value / 값
         * @param step     the rounding interval / 반올림 간격
         * @return the rounded value / 반올림한 값
         */
        private static double round(int strategy, double a, double step) {
            int s = strategy;
            double q = a / step;
            double r;
            if (s == 1) {
                r = Math.ceil(q);
            } else if (s == 2) {
                r = Math.floor(q);
            } else if (s == 3) {
                r = q < 0 ? Math.ceil(q) : Math.floor(q);
            } else {
                r = Math.floor(q + 0.5);
            }
            return r * step;
        }

        /** Tells whether a percentage occurs. 백분율이 있는지 알려 준다. */
        boolean hasPercent() {
            // 인수 중 하나라도 백분율을 가지면 참이다.
            for (int i = 0; i < args.length; i++) {
                if (args[i].hasPercent()) {
                    return true;
                }
            }
            return false;
        }

        /** Folds percentage-free parts. 백분율 없는 부분을 접는다. */
        CalcNode fold(LengthContext lc) {
            CalcNode[] folded = new CalcNode[args.length];
            boolean pct = false;
            // 인수를 접고 백분율이 남았는지 기록한다.
            for (int i = 0; i < args.length; i++) {
                folded[i] = args[i].fold(lc);
                pct = pct || folded[i].hasPercent();
            }
            Fn f = new Fn(name, folded);
            return pct ? f : constant(f, lc);
        }

        /** Tells whether an argument has an anchor function. 인수에 anchor 함수가 있는지 알려 준다. */
        boolean hasAnchor() {
            // 인수 중 하나라도 anchor 함수를 가지면 참이다.
            for (int i = 0; i < args.length; i++) {
                if (args[i].hasAnchor()) {
                    return true;
                }
            }
            return false;
        }

        /** Replaces the anchor functions of the arguments. 인수의 anchor 함수를 바꾼다. */
        CalcNode substitute(CssCalc.AnchorResolver r) {
            CalcNode[] replaced = new CalcNode[args.length];
            // 인수마다 anchor 함수를 바꾸고 하나라도 풀 수 없으면 전체를 버린다.
            for (int i = 0; i < args.length; i++) {
                replaced[i] = args[i].substitute(r);
                if (replaced[i] == null) {
                    return null;
                }
            }
            return new Fn(name, replaced);
        }

        /** Rewrites the anchor functions of the arguments. 인수의 anchor 함수를 고쳐 쓴다. */
        CalcNode mapAnchors(CssCalc.FunctionMapper m) {
            CalcNode[] mapped = new CalcNode[args.length];
            // 인수마다 anchor 함수를 고쳐 쓴다.
            for (int i = 0; i < args.length; i++) {
                mapped[i] = args[i].mapAnchors(m);
            }
            return new Fn(name, mapped);
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text(int prec) {
            StringBuilder sb = new StringBuilder(name).append('(');
            int from = 0;
            if (name.equals("round")) {
                String[] modes = {"nearest", "up", "down", "to-zero"};
                int mode = (int) ((Num) args[0]).v;
                if (mode != 0) {
                    sb.append(modes[mode]).append(", ");
                }
                from = 1;
            }
            // 인수를 쉼표와 공백으로 이어 쓴다.
            for (int i = from; i < args.length; i++) {
                if (i > from) {
                    sb.append(", ");
                }
                sb.append(args[i].text(0));
            }
            return sb.append(')').toString();
        }
    }

    /**
     * Evaluates a percentage-free node and returns it as a constant node of the same type.
     * <p>
     * 백분율이 없는 노드를 계산해 같은 형식의 상수 노드로 돌려준다.
     *
     * @param n  the node / 노드
     * @param lc reference sizes / 기준 크기
     * @return a Num or Dim node / Num 또는 Dim 노드
     */
    static CalcNode constant(CalcNode n, LengthContext lc) {
        int t = n.type();
        double v = n.eval(lc, Double.NaN);
        if (Double.isNaN(v)) {
            v = 0;
        }
        switch (t) {
        case LENGTH:
            return new Dim(v, "px", LENGTH);
        case ANGLE:
            return new Dim(v, "deg", ANGLE);
        case TIME:
            return new Dim(v, "s", TIME);
        case FREQUENCY:
            return new Dim(v, "hz", FREQUENCY);
        case RESOLUTION:
            return new Dim(v, "dppx", RESOLUTION);
        default:
            return new Num(v);
        }
    }
}
