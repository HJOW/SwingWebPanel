package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * A math expression value: <code>calc()</code> and the math functions <code>min() max() clamp() abs() sign() round() mod() rem() sin() cos() tan()
 * asin() acos() atan() atan2() pow() sqrt() hypot() log() exp()</code> of CSS Values and Units Level 4. The parser enforces the type rules (a sum needs
 * compatible types, a product needs a number on one side, a quotient a number on the right) and rejects expressions that break them. At computed-value
 * time {@link #simplify(LengthContext)} folds everything that does not depend on a percentage; what depends on a percentage stays a CssCalc and is
 * finished by the layout with {@link #resolve(LengthContext, double)} once the reference length is known.
 * <p>
 * 수식 값: <code>calc()</code> 와 CSS Values and Units Level 4 의 수학 함수 <code>min() max() clamp() abs() sign() round() mod() rem() sin() cos() tan() asin()
 * acos() atan() atan2() pow() sqrt() hypot() log() exp()</code>. 파서는 형식 규칙(합은 호환되는 형식, 곱은 한쪽이 숫자, 나눗셈은 오른쪽이 숫자)을 검사하고
 * 어기는 식을 거부한다. 계산 값을 구할 때 {@link #simplify(LengthContext)} 가 백분율에 의존하지 않는 부분을 모두 접으며, 백분율에 의존하는 식은 CssCalc 로
 * 남아 기준 길이가 정해진 뒤 레이아웃이 {@link #resolve(LengthContext, double)} 로 마무리한다.
 */
public final class CssCalc extends CssValue {

    /** The expression tree. / 식 트리. */
    private final CalcNode root;

    /** True if serialization wraps the tree in calc(). / 직렬화할 때 트리를 calc() 로 감싸면 true. */
    private final boolean wrapped;

    /**
     * Creates a calc value.
     * <p>
     * calc 값을 만든다.
     *
     * @param root    expression tree / 식 트리
     * @param wrapped true to serialize inside calc() / calc() 안에 직렬화하려면 true
     */
    private CssCalc(CalcNode root, boolean wrapped) {
        this.root = root;
        this.wrapped = wrapped;
    }

    /**
     * Tells whether a function name is calc or a math function handled by this class.
     * <p>
     * 함수 이름이 calc 이거나 이 클래스가 다루는 수학 함수인지 알려 준다.
     *
     * @param name function name / 함수 이름
     * @return true for calc and math functions / calc 와 수학 함수이면 true
     */
    public static boolean isMathFunction(String name) {
        String n = CssKeyword.lower(name);
        String[] all = {"calc", "min", "max", "clamp", "abs", "sign", "round", "mod", "rem", "sin", "cos", "tan", "asin", "acos", "atan",
            "atan2", "pow", "sqrt", "hypot", "log", "exp"};
        // 이름 목록에서 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a math function.
     * <p>
     * 수학 함수를 파싱한다.
     *
     * @param f the function component value / 함수 컴포넌트 값
     * @return the value, or null if the expression is invalid / 값. 식이 올바르지 않으면 null
     */
    public static CssCalc parse(CssFunction f) {
        String name = CssKeyword.lower(f.getName());
        CalcNode n = new Parser(f.getArguments()).function(name, f.getArguments());
        if (n == null || n.type() == CalcNode.INVALID) {
            return null;
        }
        return new CssCalc(n, name.equals("calc") || !(n instanceof CalcNode.Fn));
    }

    /**
     * The expression parser over component values.
     * <p>
     * 컴포넌트 값 위의 식 파서.
     */
    private static final class Parser {
        /** Component values with whitespace. / 공백을 포함한 컴포넌트 값. */
        private final List<ComponentValue> in;
        /** Current index. / 현재 위치. */
        private int pos;

        /**
         * Creates a parser.
         * <p>
         * 파서를 만든다.
         *
         * @param in component values / 컴포넌트 값
         */
        Parser(List<ComponentValue> in) {
            this.in = in;
        }

        /**
         * Skips whitespace.
         * <p>
         * 공백을 건너뛴다.
         *
         * @return true if some whitespace was skipped / 공백을 건너뛰었으면 true
         */
        private boolean ws() {
            boolean any = false;
            // 연속된 공백 토큰을 건너뛴다.
            while (pos < in.size() && CssParser.isWhitespace(in.get(pos))) {
                pos++;
                any = true;
            }
            return any;
        }

        /**
         * Parses a whole argument list as one sum; the parser must consume it all.
         * <p>
         * 인수 목록 전체를 합 하나로 파싱한다. 파서는 전부 소비해야 한다.
         *
         * @param list component values / 컴포넌트 값
         * @return the node or null / 노드 또는 null
         */
        private static CalcNode whole(List<ComponentValue> list) {
            Parser p = new Parser(list);
            CalcNode n = p.sum();
            p.ws();
            return n != null && p.pos == list.size() ? n : null;
        }

        /**
         * Parses the call of a calc or math function with its arguments.
         * <p>
         * calc 나 수학 함수의 호출을 인수와 함께 파싱한다.
         *
         * @param name lower-case function name / 소문자 함수 이름
         * @param args the arguments / 인수
         * @return the node or null / 노드 또는 null
         */
        CalcNode function(String name, List<ComponentValue> args) {
            if (name.equals("calc")) {
                return whole(args);
            }
            List<List<ComponentValue>> parts = CssParser.splitCommas(args);
            List<CalcNode> nodes = new ArrayList<CalcNode>();
            int start = 0;
            if (name.equals("round") && parts.get(0).size() == 1 && parts.get(0).get(0) instanceof CssToken
                    && ((CssToken) parts.get(0).get(0)).is(CssToken.IDENT)) {
                String m = CssKeyword.lower(((CssToken) parts.get(0).get(0)).getValue());
                int mode = m.equals("nearest") ? 0 : m.equals("up") ? 1 : m.equals("down") ? 2 : m.equals("to-zero") ? 3 : -1;
                if (mode < 0) {
                    return null;
                }
                nodes.add(new CalcNode.Num(mode));
                start = 1;
            } else if (name.equals("round")) {
                nodes.add(new CalcNode.Num(0));
            }
            // 각 인수를 합 하나로 파싱한다.
            for (int i = start; i < parts.size(); i++) {
                CalcNode n = whole(parts.get(i));
                if (n == null) {
                    return null;
                }
                nodes.add(n);
            }
            int count = nodes.size() - (name.equals("round") ? 1 : 0);
            if (name.equals("round") && count == 1 && nodes.get(1).type() == CalcNode.NUMBER) {
                nodes.add(new CalcNode.Num(1));
                count = 2;
            }
            return arityOk(name, count) ? new CalcNode.Fn(name, nodes) : null;
        }

        /**
         * Checks the number of arguments of a math function.
         * <p>
         * 수학 함수의 인수 개수를 확인한다.
         *
         * @param name  function name / 함수 이름
         * @param count number of arguments (without the round strategy) / 인수 개수(round 의 전략 제외)
         * @return true if the count is allowed / 허용되는 개수이면 true
         */
        private static boolean arityOk(String name, int count) {
            if (name.equals("min") || name.equals("max") || name.equals("hypot")) {
                return count >= 1;
            }
            if (name.equals("clamp")) {
                return count == 3;
            }
            if (name.equals("atan2") || name.equals("pow") || name.equals("mod") || name.equals("rem") || name.equals("round")) {
                return count == 2;
            }
            if (name.equals("log")) {
                return count == 1 || count == 2;
            }
            return count == 1;
        }

        /**
         * Parses a sum: products joined by plus and minus, which must be surrounded by whitespace.
         * <p>
         * 합을 파싱한다: 앞뒤에 공백이 있어야 하는 더하기와 빼기로 이은 곱들.
         *
         * @return the node or null / 노드 또는 null
         */
        CalcNode sum() {
            CalcNode left = product();
            // 연산자가 더 없을 때까지 합을 이어 간다.
            while (left != null) {
                int save = pos;
                boolean before = ws();
                char op = peekOp('+', '-');
                if (op == 0) {
                    pos = save;
                    break;
                }
                if (!before) {
                    return null;
                }
                pos++;
                if (!ws()) {
                    return null;
                }
                CalcNode right = product();
                if (right == null) {
                    return null;
                }
                left = new CalcNode.Bin(op, left, right);
            }
            return left;
        }

        /**
         * Parses a product: operands joined by multiplication and division.
         * <p>
         * 곱을 파싱한다: 곱셈과 나눗셈으로 이은 피연산자들.
         *
         * @return the node or null / 노드 또는 null
         */
        private CalcNode product() {
            CalcNode left = operand();
            // 연산자가 더 없을 때까지 곱을 이어 간다.
            while (left != null) {
                int save = pos;
                ws();
                char op = peekOp('*', '/');
                if (op == 0) {
                    pos = save;
                    break;
                }
                pos++;
                ws();
                CalcNode right = operand();
                if (right == null) {
                    return null;
                }
                left = new CalcNode.Bin(op, left, right);
            }
            return left;
        }

        /**
         * Returns the operator character at the current position if it is one of two candidates.
         * <p>
         * 현재 위치의 연산자 문자가 두 후보 중 하나이면 반환한다.
         *
         * @param a first candidate / 첫 후보
         * @param b second candidate / 둘째 후보
         * @return the operator or 0 / 연산자 또는 0
         */
        private char peekOp(char a, char b) {
            if (pos < in.size() && in.get(pos) instanceof CssToken) {
                CssToken t = (CssToken) in.get(pos);
                if (t.isDelim(a)) {
                    return a;
                }
                if (t.isDelim(b)) {
                    return b;
                }
            }
            return 0;
        }

        /**
         * Parses an operand: a number, dimension, percentage, constant, parenthesized sum or function.
         * <p>
         * 피연산자를 파싱한다: 숫자, 차원, 백분율, 상수, 괄호로 묶은 합, 함수.
         *
         * @return the node or null / 노드 또는 null
         */
        private CalcNode operand() {
            ws();
            if (pos >= in.size()) {
                return null;
            }
            ComponentValue v = in.get(pos);
            if (v instanceof CssToken) {
                CalcNode n = token((CssToken) v);
                if (n != null) {
                    pos++;
                }
                return n;
            }
            if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_PAREN) {
                pos++;
                return whole(((CssBlock) v).getContents());
            }
            if (v instanceof CssFunction) {
                CssFunction f = (CssFunction) v;
                String name = CssKeyword.lower(f.getName());
                // anchor() 와 anchor-size() 는 수식 안에서 쓸 수 있다(CSS Anchor Positioning 1 3.2, 5.1 절). 값은 상자를 배치할 때 푼다.
                if (name.equals("anchor") || name.equals("anchor-size")) {
                    CssValue fn = Terminals.generic(f, new ParseEnv(null, null));
                    if (!(fn instanceof CssFunctionValue)) {
                        return null;
                    }
                    pos++;
                    return new CalcNode.Anchor((CssFunctionValue) fn);
                }
                if (!isMathFunction(name)) {
                    return null;
                }
                pos++;
                return function(name, f.getArguments());
            }
            return null;
        }

        /**
         * Converts a token to a leaf node.
         * <p>
         * 토큰을 잎 노드로 바꾼다.
         *
         * @param t token / 토큰
         * @return the node or null / 노드 또는 null
         */
        private static CalcNode token(CssToken t) {
            if (t.is(CssToken.NUMBER)) {
                return new CalcNode.Num(t.getNumber());
            }
            if (t.is(CssToken.PERCENTAGE)) {
                return new CalcNode.Pct(t.getNumber());
            }
            if (t.is(CssToken.DIMENSION)) {
                String u = CssKeyword.lower(t.getUnit());
                int type = CalcNode.Dim.typeOfUnit(u);
                return type == CalcNode.INVALID ? null : new CalcNode.Dim(t.getNumber(), u, type);
            }
            if (t.is(CssToken.IDENT)) {
                String n = CssKeyword.lower(t.getValue());
                if (n.equals("pi")) {
                    return new CalcNode.Num(Math.PI);
                }
                if (n.equals("e")) {
                    return new CalcNode.Num(Math.E);
                }
                if (n.equals("infinity")) {
                    return new CalcNode.Num(Double.POSITIVE_INFINITY);
                }
                if (n.equals("-infinity")) {
                    return new CalcNode.Num(Double.NEGATIVE_INFINITY);
                }
                if (n.equals("nan")) {
                    return new CalcNode.Num(Double.NaN);
                }
            }
            return null;
        }
    }

    /**
     * Tells whether the result is a plain number.
     * <p>
     * 결과가 일반 숫자인지 알려 준다.
     *
     * @return true for numbers / 숫자이면 true
     */
    public boolean isNumber() {
        return root.type() == CalcNode.NUMBER;
    }

    /**
     * Tells whether the result is a length (without a percentage).
     * <p>
     * 결과가 길이(백분율 없음)인지 알려 준다.
     *
     * @return true for lengths / 길이이면 true
     */
    public boolean isLength() {
        return root.type() == CalcNode.LENGTH;
    }

    /**
     * Tells whether the result is a percentage.
     * <p>
     * 결과가 백분율인지 알려 준다.
     *
     * @return true for percentages / 백분율이면 true
     */
    public boolean isPercentage() {
        return root.type() == CalcNode.PERCENT;
    }

    /**
     * Tells whether the result may be a length or a percentage.
     * <p>
     * 결과가 길이 또는 백분율일 수 있는지 알려 준다.
     *
     * @return true for length-percentage results / 길이-백분율 결과이면 true
     */
    public boolean isLengthPercentage() {
        int t = root.type();
        return t == CalcNode.LENGTH || t == CalcNode.PERCENT || t == CalcNode.LENGTH_PERCENT;
    }

    /**
     * Tells whether the result is an angle.
     * <p>
     * 결과가 각도인지 알려 준다.
     *
     * @return true for angles / 각도이면 true
     */
    public boolean isAngle() {
        return root.type() == CalcNode.ANGLE;
    }

    /**
     * Tells whether the result is a time.
     * <p>
     * 결과가 시간인지 알려 준다.
     *
     * @return true for times / 시간이면 true
     */
    public boolean isTime() {
        return root.type() == CalcNode.TIME;
    }

    /**
     * Tells whether the value depends on a percentage.
     * <p>
     * 값이 백분율에 의존하는지 알려 준다.
     *
     * @return true if a percentage occurs / 백분율이 있으면 true
     */
    public boolean hasPercentage() {
        return root.hasPercent();
    }

    /**
     * Folds the expression for a computed value: without a percentage it becomes a number, length, angle or time; with a percentage the parts that
     * do not depend on it are folded and a CssCalc remains (a pure percentage expression becomes a CssPercentage).
     * <p>
     * 계산 값을 위해 식을 접는다: 백분율이 없으면 숫자, 길이, 각도, 시간이 되고, 백분율이 있으면 그에 의존하지 않는 부분만 접어 CssCalc 가 남는다(순수한 백분율 식은
     * CssPercentage 가 된다).
     *
     * @param lc reference sizes for relative units / 상대 단위의 기준 크기
     * @return the simplified value / 단순화한 값
     */
    public CssValue simplify(LengthContext lc) {
        if (!root.hasPercent()) {
            return leaf(CalcNode.constant(root, lc));
        }
        CalcNode folded = root.fold(lc);
        if (folded.type() == CalcNode.PERCENT) {
            return new CssPercentage(folded.eval(lc, 100.0));
        }
        return new CssCalc(folded, wrapped);
    }

    /**
     * Converts a constant node to its plain value.
     * <p>
     * 상수 노드를 일반 값으로 바꾼다.
     *
     * @param n a Num or Dim node / Num 또는 Dim 노드
     * @return the value / 값
     */
    private static CssValue leaf(CalcNode n) {
        if (n instanceof CalcNode.Num) {
            double v = ((CalcNode.Num) n).v;
            return new CssNumber(v, v == Math.rint(v) && !Double.isInfinite(v));
        }
        CalcNode.Dim d = (CalcNode.Dim) n;
        if (d.type == CalcNode.LENGTH) {
            return CssLength.px(d.v);
        }
        return new CssDimension(d.v, d.unit);
    }

    /**
     * Resolves one <code>anchor()</code> or <code>anchor-size()</code> function of an expression when the box is positioned.
     * <p>
     * 상자를 배치할 때 식 안의 <code>anchor()</code> 나 <code>anchor-size()</code> 함수 하나를 푼다.
     */
    public interface AnchorResolver {
        /**
         * Returns the value that the function stands for.
         * <p>
         * 함수가 뜻하는 값을 반환한다.
         *
         * @param fn the function / 함수
         * @return a {@link CssLength} in pixels or a {@link CssPercentage} (the fallback), or null if it cannot be resolved and has no usable fallback / 픽셀 {@link CssLength} 나
         *         {@link CssPercentage}(대체값). 풀 수 없고 쓸 수 있는 대체값도 없으면 null
         */
        CssValue resolve(CssFunctionValue fn);
    }

    /**
     * Rewrites one <code>anchor()</code> or <code>anchor-size()</code> function of an expression into another function (the position fallback tactics).
     * <p>
     * 식 안의 <code>anchor()</code> 나 <code>anchor-size()</code> 함수 하나를 다른 함수로 고쳐 쓴다(위치 대안 전술).
     */
    public interface FunctionMapper {
        /**
         * Returns the function that replaces a function.
         * <p>
         * 함수를 대체할 함수를 반환한다.
         *
         * @param fn the function / 함수
         * @return the replacement / 대체할 함수
         */
        CssFunctionValue map(CssFunctionValue fn);
    }

    /**
     * Rewrites the anchor functions of the expression: the result has the same shape, with every anchor function replaced by what the mapper gives.
     * <p>
     * 식의 anchor 함수를 고쳐 쓴다: 결과는 모양이 같고 모든 anchor 함수가 변환기가 주는 것으로 바뀐다.
     *
     * @param m the mapper / 변환기
     * @return the new expression / 새 식
     */
    public CssCalc mapAnchors(FunctionMapper m) {
        return new CssCalc(root.mapAnchors(m), wrapped);
    }

    /**
     * Tells whether the expression has an <code>anchor()</code> or <code>anchor-size()</code> function (CSS Anchor Positioning 1 sections 3.2 and 5.1).
     * <p>
     * 식에 <code>anchor()</code> 나 <code>anchor-size()</code> 함수가 있는지 알려 준다(CSS Anchor Positioning 1 3.2, 5.1 절).
     *
     * @return true if there is one / 있으면 true
     */
    public boolean hasAnchor() {
        return root.hasAnchor();
    }

    /**
     * Replaces the anchor functions of the expression by the values that they resolve to: the result is a plain expression of lengths and percentages.
     * <p>
     * 식의 anchor 함수를 그것이 푸는 값으로 바꾼다. 결과는 길이와 백분율로만 된 일반 식이다.
     *
     * @param r the resolver / 해석기
     * @return the expression, or null if a function cannot be resolved and has no usable fallback (the property then behaves as if it were <code>auto</code>) / 식. 함수를 풀 수
     *         없고 쓸 수 있는 대체값도 없으면 null(그러면 속성은 <code>auto</code> 인 것처럼 동작한다)
     */
    public CssCalc withAnchors(AnchorResolver r) {
        CalcNode n = root.substitute(r);
        return n == null ? null : new CssCalc(n, wrapped);
    }

    /**
     * Evaluates the expression to pixels (or the canonical unit of its type).
     * <p>
     * 식을 픽셀(또는 그 형식의 기준 단위)로 계산한다.
     *
     * @param lc          reference sizes for relative units / 상대 단위의 기준 크기
     * @param percentBase the length that 100% stands for / 100% 가 뜻하는 길이
     * @return the value / 값
     */
    public double resolve(LengthContext lc, double percentBase) {
        double v = root.eval(lc, percentBase);
        return Double.isNaN(v) ? 0 : v;
    }

    /**
     * Serializes the expression.
     * <p>
     * 식을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return wrapped ? "calc(" + root.text(0) + ")" : root.text(0);
    }
}
