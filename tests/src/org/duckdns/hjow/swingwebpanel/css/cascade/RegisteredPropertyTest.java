package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>@property</code>, the registered custom properties (plan item P10-08, CSS Properties and Values API Level 1,
 * https://www.w3.org/TR/css-properties-values-api-1/): the initial value of an undeclared property, the inheritance flag, the syntax check that turns an
 * invalid value into <code>unset</code>, the canonical computed value (lengths become pixels at the element, and the descendants inherit that), the use of
 * the typed values through <code>var()</code>, alternative syntaxes, the universal syntax and the invalid registrations.
 * <p>
 * 등록된 사용자 정의 속성 <code>@property</code> 시험(계획 P10-08, CSS Properties and Values API Level 1, https://www.w3.org/TR/css-properties-values-api-1/): 선언되지
 * 않은 속성의 초기값, 상속 플래그, 맞지 않는 값을 <code>unset</code> 으로 만드는 문법 검사, 정규 계산 값(길이는 요소에서 픽셀이 되고 자손은 그것을 상속한다),
 * <code>var()</code> 를 통한 형 있는 값의 사용, 선택 문법, 보편 문법, 올바르지 않은 등록.
 */
public class RegisteredPropertyTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RegisteredPropertyTest() {
    }

    /**
     * Parses a page and returns the computed style of an element.
     * <p>
     * 페이지를 해석하고 요소의 계산 스타일을 반환한다.
     *
     * @param css  the style sheet / 스타일시트
     * @param body the body markup / 본문 마크업
     * @param id   the id of the element whose style is returned / 스타일을 반환할 요소의 id
     * @return the computed style / 계산 스타일
     */
    private static ComputedStyle style(String css, String body, String id) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + body + "</body></html>",
                "http://t.test/page.html", null, true);
        CssEngine e = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        e.loadStyleSheets();
        return e.getComputedStyle(doc.getElementById(id));
    }

    /**
     * Returns the computed value of a custom property.
     * <p>
     * 사용자 정의 속성의 계산 값을 반환한다.
     *
     * @param css  the style sheet / 스타일시트
     * @param body the body markup / 본문 마크업
     * @param id   the element id / 요소 id
     * @param name the property / 속성
     * @return the text, empty if undefined / 문자열. 정의되지 않았으면 빈 문자열
     */
    private static String value(String css, String body, String id, String name) {
        return style(css, body, id).getPropertyValue(name);
    }

    /**
     * An undeclared registered property has its initial value, and a registered property that does not inherit does not take the value of the
     * parent, which an unregistered custom property does.
     * <p>
     * 선언되지 않은 등록된 속성은 초기값을 가지며, 상속되지 않는 등록된 속성은 부모의 값을 받지 않는다(등록되지 않은 사용자 정의 속성은 받는다).
     */
    public void testInitialValueAndInheritance() {
        String reg = "@property --w { syntax: '<length>'; inherits: false; initial-value: 5px }";
        String body = "<div id=a style='--w: 20px'><div id=b></div></div>";
        Check.equal("5px", value(reg, "<div id=a></div>", "a", "--w"), "an undeclared property has its initial value");
        Check.equal("20px", value(reg, body, "a", "--w"), "a declared value");
        Check.equal("5px", value(reg, body, "b", "--w"), "a property that does not inherit gets the initial value in the child");
        Check.equal("20px", value("", body, "b", "--w"), "an unregistered property is inherited");
        String inh = "@property --w { syntax: '<length>'; inherits: true; initial-value: 5px }";
        Check.equal("20px", value(inh, body, "b", "--w"), "a property that inherits");
        Check.equal("5px", value(inh, "<div id=a><div id=b></div></div>", "b", "--w"), "or the initial value at the top");
    }

    /**
     * A value that does not match the syntax is invalid at computed-value time and becomes <code>unset</code>.
     * <p>
     * 문법과 맞지 않는 값은 계산 시점에 무효가 되어 <code>unset</code> 이 된다.
     */
    public void testSyntaxCheck() {
        String reg = "@property --w { syntax: '<length>'; inherits: false; initial-value: 5px }";
        Check.equal("5px", value(reg, "<div id=a style='--w: red'></div>", "a", "--w"), "a color is no length");
        Check.equal("5px", value(reg, "<div id=a style='--w: 10'></div>", "a", "--w"), "a number is no length");
        Check.equal("7px", value(reg, "<div id=a style='--w: 7px'></div>", "a", "--w"), "a length");
        Check.equal("5px", value(reg, "<div id=a style='--w: initial'></div>", "a", "--w"), "initial");
        String alt = "@property --w { syntax: '<length> | auto'; inherits: false; initial-value: auto }";
        Check.equal("auto", value(alt, "<div id=a></div>", "a", "--w"), "an alternative with a keyword");
        Check.equal("auto", value(alt, "<div id=a style='--w: auto'></div>", "a", "--w"), "the keyword");
        Check.equal("3px", value(alt, "<div id=a style='--w: 3px'></div>", "a", "--w"), "the length");
        Check.equal("auto", value(alt, "<div id=a style='--w: none'></div>", "a", "--w"), "another keyword is invalid");
        String list = "@property --l { syntax: '<number>+'; inherits: false; initial-value: 1 2 }";
        Check.equal("1 2", value(list, "<div id=a></div>", "a", "--l"), "a list of numbers");
        Check.equal("3 4 5", value(list, "<div id=a style='--l: 3 4 5'></div>", "a", "--l"), "a longer list");
        Check.equal("1 2", value(list, "<div id=a style='--l: 3 x'></div>", "a", "--l"), "a list with a wrong item");
    }

    /**
     * The computed value is the canonical form of the type: a relative length is resolved at the element and the descendants inherit the resolved
     * length (the unregistered property is inherited as written and resolved again in every descendant).
     * <p>
     * 계산 값은 자료형의 정규 형태이다: 상대 길이는 요소에서 풀리고 자손은 풀린 길이를 상속한다(등록되지 않은 속성은 쓰인 그대로 상속되어 자손마다 다시 풀린다).
     */
    public void testComputedValue() {
        String reg = "@property --w { syntax: '<length>'; inherits: true; initial-value: 0px }";
        String body = "<div id=a style='font-size: 20px; --w: 2em'><div id=b style='font-size: 10px'><div id=c style='width: var(--w)'></div></div></div>";
        Check.equal("40px", value(reg, body, "a", "--w"), "2em at 20px is 40px");
        Check.equal("40px", value(reg, body, "b", "--w"), "the child inherits the computed length");
        Check.equal("2em", value("", body, "b", "--w"), "an unregistered property keeps the tokens");
        Check.near(40.0, style(reg, body, "c").px(Prop.WIDTH), 0.01, "width: var(--w) uses the computed 40px");
        Check.near(20.0, style("", body, "c").px(Prop.WIDTH), 0.01, "while the unregistered property is 2em of the child, 20px");
        Check.equal("100%", value("@property --p { syntax: '<percentage>'; inherits: false; initial-value: 0% }", "<div id=a style='--p: 100%'></div>", "a", "--p"),
                "a percentage");
        Check.equal("1.5", value("@property --n { syntax: '<number>'; inherits: false; initial-value: 0 }", "<div id=a style='--n: calc(1 + 0.5)'></div>", "a", "--n"),
                "calc is simplified");
        Check.equal("0", value("@property --n { syntax: '<number>'; inherits: false; initial-value: 0 }", "<div id=a></div>", "a", "--n"), "the number initial value");
    }

    /**
     * A registered color property can be used as a color; the universal syntax accepts anything and needs no initial value.
     * <p>
     * 등록된 색 속성은 색으로 쓸 수 있다. 보편 문법은 무엇이든 받아들이고 초기값이 필요 없다.
     */
    public void testColorsAndUniversalSyntax() {
        String reg = "@property --c { syntax: '<color>'; inherits: true; initial-value: blue }";
        String two = "<p id=a style='color: var(--c)'>x</p><p id=b style='--c: red; color: var(--c)'>y</p>";
        Check.equal(0x0000FF, style(reg, two, "a").color(Prop.COLOR) & 0xFFFFFF, "the initial value of the color");
        Check.equal(0xFF0000, style(reg, two, "b").color(Prop.COLOR) & 0xFFFFFF, "a declared color");
        String any = "@property --x { syntax: '*'; inherits: false }";
        Check.equal("", value(any, "<div id=a></div>", "a", "--x"), "the universal syntax without an initial value is not defined");
        Check.equal("1 + (2)", value(any, "<div id=a style='--x: 1 + (2)'></div>", "a", "--x"), "and takes any value");
        Check.equal("", value(any, "<div id=a style='--x: 1'><div id=b></div></div>", "b", "--x"), "but does not inherit if inherits is false");
    }

    /**
     * Invalid registrations are ignored: a missing descriptor, a missing initial value, an initial value that depends on its context, a bad syntax,
     * a name that is not a custom property name.
     * <p>
     * 올바르지 않은 등록은 무시된다: 서술자 누락, 초기값 누락, 문맥에 의존하는 초기값, 틀린 문법, 사용자 정의 속성 이름이 아닌 이름.
     */
    public void testInvalidRegistrations() {
        String body = "<div id=a style='--w: 2em; font-size: 10px'></div>";
        Check.equal("2em", value("@property --w { syntax: '<length>'; initial-value: 0px }", body, "a", "--w"), "inherits is required");
        Check.equal("2em", value("@property --w { inherits: false; initial-value: 0px }", body, "a", "--w"), "syntax is required");
        Check.equal("2em", value("@property --w { syntax: '<length>'; inherits: false }", body, "a", "--w"), "the initial value is required");
        Check.equal("2em", value("@property --w { syntax: '<length>'; inherits: false; initial-value: 1em }", body, "a", "--w"),
                "an initial value in em depends on the context");
        Check.equal("2em", value("@property --w { syntax: '<length>'; inherits: false; initial-value: red }", body, "a", "--w"), "an initial value of another type");
        Check.equal("2em", value("@property --w { syntax: '<bogus>'; inherits: false; initial-value: 0px }", body, "a", "--w"), "an unknown type in the syntax");
        Check.equal("2em", value("@property --w { syntax: '<length>'; inherits: maybe; initial-value: 0px }", body, "a", "--w"), "inherits must be true or false");
        Check.equal("2em", value("@property w { syntax: '<length>'; inherits: false; initial-value: 0px }", body, "a", "--w"), "the name must start with two hyphens");
    }

    /**
     * Rules inside conditional groups register only when the group applies, and a later registration replaces an earlier one.
     * <p>
     * 조건 그룹 안의 규칙은 그룹이 적용될 때만 등록하며, 나중 등록이 앞의 것을 대체한다.
     */
    public void testGroupsAndOrder() {
        String r1 = "@property --w { syntax: '<length>'; inherits: false; initial-value: 5px }";
        String r2 = "@property --w { syntax: '<length>'; inherits: false; initial-value: 9px }";
        Check.equal("9px", value(r1 + r2, "<div id=a></div>", "a", "--w"), "the later registration wins");
        Check.equal("5px", value("@media (min-width: 100px) { " + r1 + " }", "<div id=a></div>", "a", "--w"), "inside a matching @media");
        Check.equal("", value("@media (min-width: 1000px) { " + r1 + " }", "<div id=a></div>", "a", "--w"), "inside an @media that does not match");
    }
}
