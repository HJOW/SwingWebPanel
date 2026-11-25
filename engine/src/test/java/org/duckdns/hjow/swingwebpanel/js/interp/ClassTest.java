package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of classes (plan item P13-08).
 * <p>
 * 클래스 시험(계획 항목 P13-08).
 */
public class ClassTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ClassTest() {
    }

    /**
     * Definition, inheritance, super, accessors, static members and constructor semantics.
     * <p>
     * 정의, 상속, super, 접근자, 정적 멤버, 생성자 의미.
     */
    public void testClasses() {
        Scripts.checkResource("classes.js", 120);
    }

    /**
     * Initial error conditions of class syntax are reported before any code runs.
     * <p>
     * 클래스 구문의 초기 오류 조건은 코드를 실행하기 전에 보고된다.
     */
    public void testClassEarlyErrors() {
        String[] bad = {
            "class A { constructor() { } constructor() { } }",
            "class A { get constructor() { } }",
            "class A { *constructor() { } }",
            "class A { static prototype() { } }",
            "class A extends B { constructor() { } m() { super(); } }",
            "class A { m() { super(); } }",
            "function f() { super.x; }",
            "class { }",
            "class A { m() { var a; let a; } }",
            "class let { }",
            "class A { static static() {} get get() {} } class A { }",
        };
        // 잘못된 클래스 구문마다 실행 전에 SyntaxError 가 나는지 확인한다.
        for (int i = 0; i < bad.length; i++) {
            Check.contains(Scripts.thrown(bad[i]), "SyntaxError", "early error for: " + bad[i]);
        }
    }
}
