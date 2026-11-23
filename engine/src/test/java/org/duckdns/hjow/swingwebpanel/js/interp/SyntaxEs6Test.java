package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * Tests of ES2015 syntax evaluation (plan items P13-06 and P13-07): literals, default and rest parameters, spread,
 * destructuring, template literals, tagged templates and arrow functions.
 * <p>
 * ES2015 구문 평가 시험(계획 항목 P13-06, P13-07): 리터럴, 기본/나머지 매개변수, 전개, 분해, 템플릿 문자열, 태그 템플릿, 화살표 함수.
 */
public class SyntaxEs6Test {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public SyntaxEs6Test() {
    }

    /**
     * Literals, parameters, spread, destructuring and templates.
     * <p>
     * 리터럴, 매개변수, 전개, 분해, 템플릿.
     */
    public void testLiteralsParametersDestructuringTemplates() {
        Scripts.checkResource("es6-syntax.js", 170);
    }

    /**
     * Arrow functions: lexical this, arguments, new.target and super, no prototype and no construction.
     * <p>
     * 화살표 함수: 렉시컬 this, arguments, new.target, super, prototype 없음, 생성 불가.
     */
    public void testArrowFunctions() {
        Scripts.checkResource("arrows.js", 60);
    }
}
