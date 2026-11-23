package org.duckdns.hjow.swingwebpanel.css;

import org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the conditions of <code>@supports</code> (plan item P10-08, CSS Conditional Level 3 https://www.w3.org/TR/css-conditional-3/, Level 4 and
 * Level 5): declarations (also of the properties that the engine only parses and of the new properties of the Containment, Scroll and Text modules),
 * <code>selector()</code>, <code>font-format()</code>, <code>font-tech()</code>, <code>at-rule()</code> and the operators.
 * <p>
 * <code>@supports</code> 조건 시험(계획 P10-08, CSS Conditional Level 3 https://www.w3.org/TR/css-conditional-3/, Level 4, Level 5): 선언(엔진이 파싱만 하는 속성과 Containment,
 * Scroll, Text 모듈의 새 속성도), <code>selector()</code>, <code>font-format()</code>, <code>font-tech()</code>, <code>at-rule()</code>, 연산자.
 */
public class SupportsConditionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SupportsConditionTest() {
    }

    /**
     * Declarations are true if the engine parses them, false if the property or the value is not known or not supported.
     * <p>
     * 선언은 엔진이 파싱하면 참이고, 속성이나 값을 모르거나 지원하지 않으면 거짓이다.
     */
    public void testDeclarations() {
        Check.isTrue(SupportsCondition.evaluate("(display: grid)"), "display: grid");
        Check.isTrue(SupportsCondition.evaluate("(container-type: inline-size)"), "container-type");
        Check.isTrue(SupportsCondition.evaluate("(container: card / inline-size)"), "the container shorthand");
        Check.isTrue(SupportsCondition.evaluate("(scroll-snap-type: x mandatory)"), "scroll-snap-type");
        Check.isTrue(SupportsCondition.evaluate("(text-wrap: balance)"), "text-wrap");
        Check.isTrue(SupportsCondition.evaluate("(color-scheme: light dark)"), "color-scheme");
        Check.isTrue(SupportsCondition.evaluate("(--any: value)"), "a custom property");
        Check.isFalse(SupportsCondition.evaluate("(display: bogus)"), "an invalid value");
        Check.isFalse(SupportsCondition.evaluate("(bogus-property: 1)"), "an unknown property");
        Check.isFalse(SupportsCondition.evaluate("(perspective: 100px)"), "an unsupported property");
        Check.isTrue(SupportsCondition.supports("container-type", "size"), "CSS.supports(property, value)");
        Check.isFalse(SupportsCondition.supports("container-type", "block"), "CSS.supports with an invalid value");
    }

    /**
     * Operators and nested conditions.
     * <p>
     * 연산자와 중첩 조건.
     */
    public void testOperators() {
        Check.isTrue(SupportsCondition.evaluate("(display: grid) and (container-type: size)"), "and");
        Check.isFalse(SupportsCondition.evaluate("(display: grid) and (bogus: 1)"), "and fails");
        Check.isTrue(SupportsCondition.evaluate("(bogus: 1) or (display: grid)"), "or");
        Check.isTrue(SupportsCondition.evaluate("not (bogus: 1)"), "not");
        Check.isFalse(SupportsCondition.evaluate("not (display: grid)"), "not fails");
        Check.isTrue(SupportsCondition.evaluate("((display: grid) or (bogus: 1)) and (not (foo: 1))"), "nested");
        Check.isFalse(SupportsCondition.evaluate("(display: grid) and (display: flex) or (display: block)"), "and and or cannot be mixed");
    }

    /**
     * Functions: <code>selector()</code>, <code>font-format()</code>, <code>font-tech()</code> and <code>at-rule()</code>.
     * <p>
     * 함수: <code>selector()</code>, <code>font-format()</code>, <code>font-tech()</code>, <code>at-rule()</code>.
     */
    public void testFunctions() {
        Check.isTrue(SupportsCondition.evaluate("selector(:has(a))"), "selector(:has())");
        Check.isTrue(SupportsCondition.evaluate("selector(a > b)"), "selector(a > b)");
        Check.isFalse(SupportsCondition.evaluate("selector(:bogus)"), "an unknown pseudo-class");
        Check.isTrue(SupportsCondition.evaluate("font-format(woff)"), "woff");
        Check.isTrue(SupportsCondition.evaluate("font-format(truetype)"), "truetype");
        Check.isTrue(SupportsCondition.evaluate("font-format(woff2)"), "woff2 is supported (Woff2 decoder)");
        Check.isFalse(SupportsCondition.evaluate("font-format(embedded-opentype)"), "eot is not supported");
        Check.isFalse(SupportsCondition.evaluate("font-tech(variations)"), "no font technology is supported");
        Check.isTrue(SupportsCondition.evaluate("at-rule(@container)"), "at-rule(@container)");
        Check.isTrue(SupportsCondition.evaluate("at-rule(@property) and at-rule(@scope)"), "at-rule(@property) and @scope");
        Check.isFalse(SupportsCondition.evaluate("at-rule(@bogus)"), "an unknown at-rule");
        Check.isTrue(SupportsCondition.evaluate("not font-format(embedded-opentype)"), "not with a function");
    }
}
