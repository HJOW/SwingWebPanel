package org.duckdns.hjow.swingwebpanel.css;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.testing.Check;

import java.util.List;

/**
 * Tests of calc() and the math functions (plan item P08-05): precedence, whitespace rules, type checking, unit conversion, percentages and folding.
 * <p>
 * calc() 와 수학 함수 시험(계획 P08-05): 우선순위, 공백 규칙, 형식 검사, 단위 변환, 백분율, 접기.
 */
public class CalcTest {

    /** Reference sizes: 20px font, 10px root, 1000x500 viewport. / 기준 크기: 글꼴 20px, 루트 10px, 뷰포트 1000x500. */
    private static final LengthContext LC = new LengthContext(20, 10, 24, 1000, 500);

    /**
     * Creates the suite. 스위트를 만든다.
     */
    public CalcTest() {
    }

    /**
     * Parses a math function from text. 텍스트에서 수학 함수를 파싱한다.
     *
     * @param css function text / 함수 문자열
     * @return the parsed value or null / 파싱한 값 또는 null
     */
    private static CssCalc parse(String css) {
        List<ComponentValue> v = CssParser.trim(CssParser.parseComponentValues(css));
        if (v.size() != 1 || !(v.get(0) instanceof CssFunction)) {
            return null;
        }
        return CssCalc.parse((CssFunction) v.get(0));
    }

    /**
     * Simplifies an expression and returns the result's text. 식을 단순화해 결과 문자열을 반환한다.
     *
     * @param css function text / 함수 문자열
     * @return serialized simplified value or INVALID / 단순화한 값의 직렬화 또는 INVALID
     */
    private static String simple(String css) {
        CssCalc c = parse(css);
        return c == null ? "INVALID" : c.simplify(LC).toCssText();
    }

    /**
     * Basic arithmetic and precedence. 기본 산술과 우선순위.
     */
    public void testArithmetic() {
        Check.equal("5", simple("calc(2 + 3)"), "addition");
        Check.equal("-1", simple("calc(2 - 3)"), "subtraction");
        Check.equal("14", simple("calc(2 + 3 * 4)"), "precedence");
        Check.equal("20", simple("calc((2 + 3) * 4)"), "parentheses");
        Check.equal("2.5", simple("calc(5 / 2)"), "division");
        Check.equal("10", simple("calc(2 * 5)"), "product");
        Check.equal("8px", simple("calc(10px - 2px)"), "length subtraction");
        Check.equal("30px", simple("calc(10px * 3)"), "length times number");
        Check.equal("30px", simple("calc(3 * 10px)"), "number times length");
        Check.equal("5px", simple("calc(10px / 2)"), "length divided by number");
        Check.equal("INVALID", simple("calc(10px / 10px)"), "a length divided by a length is not a valid type");
        Check.equal("-5px", simple("calc(-5px)"), "negative operand");
        Check.equal("15px", simple("calc(10px - -5px)"), "subtracting a negative");
        Check.equal("6", simple("calc(1 + 2 + 3)"), "chained sum");
        Check.equal("0", simple("calc(10 - 4 - 6)"), "left associativity");
        Check.equal("4", simple("calc(8 / 4 * 2)"), "left associative product");
        Check.equal("3.14159", simple("calc(pi)").substring(0, 7), "pi constant");
    }

    /**
     * Unit conversion with the reference sizes. 기준 크기에 따른 단위 변환.
     */
    public void testUnits() {
        Check.equal("96px", simple("calc(1in)"), "inch");
        Check.equal("40px", simple("calc(2em)"), "em uses the font size");
        Check.equal("30px", simple("calc(3rem)"), "rem uses the root font size");
        Check.equal("100px", simple("calc(10vw)"), "viewport width");
        Check.equal("50px", simple("calc(10vh)"), "viewport height");
        Check.equal("60px", simple("calc(1em + 40px)"), "mixed units");
        Check.equal("96px", simple("calc(72pt + 0px)"), "points");
        Check.equal("90deg", simple("calc(0.25turn)"), "angles");
        Check.equal("1.5s", simple("calc(1s + 500ms)"), "times");
    }

    /**
     * Whitespace rules for plus and minus. 더하기와 빼기의 공백 규칙.
     */
    public void testWhitespace() {
        Check.equal("INVALID", simple("calc(1px+2px)"), "a plus without whitespace");
        Check.equal("INVALID", simple("calc(1px -2px)"), "a sign is not an operator");
        Check.equal("INVALID", simple("calc(1px- 2px)"), "a minus needs whitespace before it");
        Check.equal("INVALID", simple("calc(1px + )"), "a missing operand");
        Check.equal("INVALID", simple("calc()"), "empty");
        Check.equal("INVALID", simple("calc(1px 2px)"), "two operands");
        Check.equal("6px", simple("calc(2px*3)"), "multiplication needs no whitespace");
        Check.equal("2px", simple("calc(6px/3)"), "division needs no whitespace");
    }

    /**
     * Type checking. 형식 검사.
     */
    public void testTypes() {
        Check.equal("INVALID", simple("calc(1px + 1)"), "length plus number");
        Check.equal("INVALID", simple("calc(1px * 1px)"), "length times length");
        Check.equal("INVALID", simple("calc(1 / 1px)"), "number divided by length");
        Check.equal("INVALID", simple("calc(1px + 1deg)"), "length plus angle");
        Check.equal("INVALID", simple("calc(1foo)"), "unknown unit");
        Check.equal("INVALID", simple("calc(1px + auto)"), "keyword");
        Check.isTrue(parse("calc(1px + 10%)").isLengthPercentage(), "length plus percentage");
        Check.isTrue(parse("calc(1px + 10px)").isLength(), "length");
        Check.isTrue(parse("calc(10% + 10%)").isPercentage(), "percentage");
        Check.isTrue(parse("calc(1 + 2)").isNumber(), "number");
        Check.isTrue(parse("calc(1deg * 2)").isAngle(), "angle");
        Check.isTrue(parse("calc(1s * 2)").isTime(), "time");
        Check.isTrue(parse("calc(100% - 20px)").hasPercentage(), "percentage dependence");
        Check.isFalse(parse("calc(100px - 20px)").hasPercentage(), "no percentage");
    }

    /**
     * Percentages stay symbolic and resolve against a base. 백분율은 기호로 남고 기준에 대해 계산된다.
     */
    public void testPercentages() {
        CssValue v = parse("calc(100% - 20px)").simplify(LC);
        Check.isTrue(v instanceof CssCalc, "a percentage keeps the calc");
        Check.equal("calc(100% - 20px)", v.toCssText(), "serialization");
        Check.near(180, ((CssCalc) v).resolve(LC, 200), 1e-9, "resolved against 200px");
        CssValue w = parse("calc(1em + 2em + 50%)").simplify(LC);
        Check.equal("calc(60px + 50%)", w.toCssText(), "the percentage-free part is folded");
        Check.near(160, ((CssCalc) w).resolve(LC, 200), 1e-9, "resolved");
        CssValue p = parse("calc(10% + 20%)").simplify(LC);
        Check.isTrue(p instanceof CssPercentage, "a pure percentage expression becomes a percentage");
        Check.near(30, ((CssPercentage) p).getValue(), 1e-9, "its value");
        CssValue m = parse("min(50%, 100px)").simplify(LC);
        Check.near(100, ((CssCalc) m).resolve(LC, 400), 1e-9, "min picks the smaller");
        Check.near(50, ((CssCalc) m).resolve(LC, 100), 1e-9, "min with a smaller percentage");
    }

    /**
     * Math functions. 수학 함수.
     */
    public void testMathFunctions() {
        Check.equal("5px", simple("min(10px, 5px)"), "min");
        Check.equal("10px", simple("max(10px, 5px, 1px)"), "max");
        Check.equal("15px", simple("clamp(10px, 20px, 15px)"), "clamp upper");
        Check.equal("10px", simple("clamp(10px, 5px, 15px)"), "clamp lower");
        Check.equal("12px", simple("clamp(10px, 12px, 15px)"), "clamp middle");
        Check.equal("3", simple("abs(-3)"), "abs");
        Check.equal("-1", simple("sign(-3px)"), "sign");
        Check.equal("5", simple("sqrt(25)"), "sqrt");
        Check.equal("8", simple("pow(2, 3)"), "pow");
        Check.equal("5px", simple("hypot(3px, 4px)"), "hypot");
        Check.equal("2", simple("log(100, 10)"), "log with base");
        Check.equal("1", simple("exp(0)"), "exp");
        Check.equal("1", simple("round(1.4)"), "round without step");
        Check.equal("2", simple("round(1.5)"), "round half up");
        Check.equal("10px", simple("round(12px, 5px)"), "round with step");
        Check.equal("15px", simple("round(up, 11px, 5px)"), "round up");
        Check.equal("10px", simple("round(down, 14px, 5px)"), "round down");
        Check.equal("1px", simple("mod(7px, 3px)"), "mod");
        Check.equal("2px", simple("mod(-7px, 3px)"), "mod with a negative dividend takes the sign of the divisor");
        Check.equal("-1px", simple("rem(-7px, 3px)"), "rem keeps the sign of the dividend");
        Check.equal("0", simple("sin(0)"), "sin of zero");
        Check.equal("1", simple("sin(90deg)"), "sin of an angle");
        Check.equal("INVALID", simple("sin(1px)"), "sin of a length");
        Check.equal("45deg", simple("atan(1)"), "atan gives an angle");
        Check.isTrue(simple("atan2(1px, 1px)").endsWith("deg"), "atan2 gives an angle");
        Check.equal("INVALID", simple("min()"), "min without arguments");
        Check.equal("INVALID", simple("clamp(1px, 2px)"), "clamp needs three arguments");
        Check.equal("INVALID", simple("min(1px, 2)"), "incompatible arguments");
        Check.equal("INVALID", simple("sqrt(1px)"), "sqrt of a length");
        Check.equal("INVALID", simple("round(sideways, 1, 1)"), "unknown rounding strategy");
    }

    /**
     * Nested functions and serialization. 중첩된 함수와 직렬화.
     */
    public void testNestingAndSerialization() {
        Check.equal("15px", simple("calc(min(10px, 20px) + max(1px, 5px))"), "nested functions");
        Check.equal("20px", simple("calc(calc(1px + 1px) * 10)"), "nested calc");
        Check.equal("calc(1px + 2px)", parse("calc(1px + 2px)").toCssText(), "specified serialization keeps the tree");
        Check.equal("calc((1px + 2px) * 3)", parse("calc((1px + 2px) * 3)").toCssText(), "parentheses are re-inserted");
        Check.equal("calc(1px - (2px + 3px))", parse("calc(1px - (2px + 3px))").toCssText(), "right operand of a minus");
        Check.equal("min(1px, 2px)", parse("min(1px, 2px)").toCssText(), "math functions are not wrapped");
        Check.equal("round(up, 1px, 2px)", parse("round(up, 1px, 2px)").toCssText(), "round strategy");
        Check.isTrue(CssCalc.isMathFunction("CLAMP"), "function names are case-insensitive");
        Check.isFalse(CssCalc.isMathFunction("translate"), "other functions");
        CssValue v = parse("calc(10px + 5px)").simplify(LC);
        Check.isTrue(v instanceof CssLength && ((CssLength) v).getValue() == 15, "a length results");
        Check.isTrue(parse("calc(1 + 2)").simplify(LC) instanceof CssNumber, "a number results");
        Check.isTrue(parse("calc(1turn)").simplify(LC) instanceof CssDimension, "an angle results");
    }
}
