package org.duckdns.hjow.swingwebpanel.css;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.CssTransitionEvent;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the transitions and the animations of registered custom properties (plan item P10-08, CSS Properties and Values API Level 1,
 * https://www.w3.org/TR/css-properties-values-api-1/, the animation of registered properties): a property registered with <code>@property</code> and a typed syntax
 * (<code>&lt;length&gt;</code>, <code>&lt;number&gt;</code>, <code>&lt;color&gt;</code>, <code>&lt;percentage&gt;</code>, <code>&lt;integer&gt;</code>) is interpolated
 * between its computed values by <code>transition</code> and by <code>@keyframes</code>; the declarations that use it with <code>var()</code> and the descendants that
 * inherit it see the values of every moment; a property with the universal syntax or without registration changes at once (discretely).
 * <p>
 * 등록된 사용자 정의 속성의 전환과 애니메이션 시험(계획 P10-08, CSS Properties and Values API Level 1): <code>@property</code> 로 형 있는 문법(<code>&lt;length&gt;</code>,
 * <code>&lt;number&gt;</code>, <code>&lt;color&gt;</code>, <code>&lt;percentage&gt;</code>, <code>&lt;integer&gt;</code>)으로 등록한 속성은 <code>transition</code> 과
 * <code>@keyframes</code> 로 계산 값 사이를 보간한다. <code>var()</code> 로 그것을 쓰는 선언과 그것을 상속하는 자손은 매 순간의 값을 보며, 보편 문법이나 등록되지 않은 속성은
 * 한 번에(이산적으로) 바뀐다.
 */
public class RegisteredPropertyAnimationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RegisteredPropertyAnimationTest() {
    }

    /**
     * A page with the animation engine and a clock that the test moves.
     * <p>
     * 애니메이션 엔진과 시험이 움직이는 시계를 가진 페이지.
     */
    private static final class Page {
        /** The clock. 시계. */
        final long[] time = new long[1];
        /** The document. 문서. */
        final Document doc;
        /** The CSS engine. CSS 엔진. */
        final CssEngine css;
        /** The animation engine. 애니메이션 엔진. */
        final AnimationEngine engine;
        /** The transition events. 전환 이벤트. */
        final List<String> events = new ArrayList<String>();

        /**
         * Creates the page.
         * <p>
         * 페이지를 만든다.
         *
         * @param style the style sheet / 스타일시트
         * @param body  the body markup / 본문 마크업
         */
        Page(String style, String body) {
            doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + style + "</style></head><body>" + body + "</body></html>");
            css = new CssEngine(doc, null, new MediaEnvironment(400, 300));
            css.loadStyleSheets();
            engine = new AnimationEngine(css, new AnimationClock() {
                /**
                 * Returns the time of the test.
                 * <p>
                 * 시험의 시각을 반환한다.
                 *
                 * @return the time / 시각
                 */
                public long nowMillis() {
                    return time[0];
                }
            });
            EventListener l = new EventListener() {
                /**
                 * Records a transition event.
                 * <p>
                 * 전환 이벤트를 기록한다.
                 *
                 * @param event the event / 이벤트
                 */
                public void handleEvent(Event event) {
                    if (event instanceof CssTransitionEvent) {
                        CssTransitionEvent e = (CssTransitionEvent) event;
                        events.add(e.getType() + ":" + e.getPropertyName() + ":" + Math.round(e.getElapsedTime() * 1000));
                    }
                }
            };
            String[] types = {"transitionrun", "transitionstart", "transitionend", "transitioncancel"};
            // 전환 이벤트 종류마다 기록기를 단다.
            for (int i = 0; i < types.length; i++) {
                doc.getDocumentElement().addEventListener(types[i], l, false);
            }
        }

        /**
         * Moves the clock and ticks the engine.
         * <p>
         * 시계를 옮기고 엔진을 진행한다.
         *
         * @param t the time / 시각
         */
        void at(long t) {
            time[0] = t;
            engine.tick();
        }

        /**
         * Returns the style of an element.
         * <p>
         * 요소의 스타일을 반환한다.
         *
         * @param id the id / id
         * @return the style / 스타일
         */
        ComputedStyle style(String id) {
            return css.getComputedStyle(doc.getElementById(id));
        }

        /**
         * Returns an element.
         * <p>
         * 요소를 반환한다.
         *
         * @param id the id / id
         * @return the element / 요소
         */
        Element el(String id) {
            return doc.getElementById(id);
        }
    }

    /** The registration of a length and a number and a color that the tests use. 시험이 쓰는 길이, 숫자, 색의 등록. */
    private static final String REG = "@property --w { syntax: '<length>'; inherits: true; initial-value: 100px } "
            + "@property --n { syntax: '<number>'; inherits: false; initial-value: 0 } "
            + "@property --c { syntax: '<color>'; inherits: true; initial-value: #000000 } "
            + "@property --p { syntax: '<percentage>'; inherits: false; initial-value: 0% } "
            + "@property --i { syntax: '<integer>'; inherits: false; initial-value: 0 } ";

    /**
     * Returns the value of a custom property in the style of an element.
     * <p>
     * 요소의 스타일에서 사용자 정의 속성의 값을 반환한다.
     *
     * @param p    the page / 페이지
     * @param id   the element id / 요소 id
     * @param name the property / 속성
     * @return the text / 문자열
     */
    private static String custom(Page p, String id, String name) {
        return p.style(id).getPropertyValue(name);
    }

    /**
     * A transition of a registered length runs between the two computed values, a declaration that uses it with <code>var()</code> follows it, and the events are those
     * of the property.
     * <p>
     * 등록된 길이의 전환은 두 계산 값 사이를 달리고, <code>var()</code> 로 그것을 쓰는 선언이 따라가며, 이벤트는 그 속성의 것이다.
     */
    public void testLengthTransitionReachesVar() {
        Page p = new Page(REG + "#a{--w:100px;width:var(--w);transition:--w 1s linear} #a.big{--w:200px}", "<div id=a></div>");
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "initial width");
        p.at(0);
        p.el("a").setAttribute("class", "big");
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "just after the change the transition starts from the old value");
        Check.isTrue(p.engine.isActive(), "a transition runs");
        p.at(0);
        p.at(500);
        Check.equal("150px", custom(p, "a", "--w"), "half way the property is 150px");
        Check.near(150, p.style("a").px(Prop.WIDTH), 0.01, "and the width that uses it follows");
        p.at(750);
        Check.near(175, p.style("a").px(Prop.WIDTH), 0.01, "three quarters");
        p.at(1000);
        Check.equal("200px", custom(p, "a", "--w"), "the end value");
        Check.near(200, p.style("a").px(Prop.WIDTH), 0.001, "the width at the end");
        Check.isFalse(p.engine.isActive(), "nothing runs afterwards");
        Check.equal("[transitionrun:--w:0, transitionstart:--w:0, transitionend:--w:1000]", p.events.toString(), "the events carry the name of the property");
    }

    /**
     * A property that inherits is seen by the descendants with the value of the moment, and a transition of <code>all</code> covers the registered properties too.
     * <p>
     * 상속하는 속성은 자손이 그 순간의 값으로 보며, <code>all</code> 전환은 등록된 속성도 덮는다.
     */
    public void testInheritedAndAll() {
        Page p = new Page(REG + "#a{--w:0px;transition:all 1s linear} #a.big{--w:100px} #b{width:var(--w)}", "<div id=a><div id=b></div></div>");
        Check.near(0, p.style("b").px(Prop.WIDTH), 0.001, "the child starts with the inherited value");
        p.at(0);
        p.el("a").setAttribute("class", "big");
        p.style("b");
        p.at(0);
        p.at(250);
        Check.near(25, p.style("b").px(Prop.WIDTH), 0.01, "the child sees the value of the moment");
        Check.equal("25px", custom(p, "b", "--w"), "the inherited property");
        p.at(1000);
        Check.near(100, p.style("b").px(Prop.WIDTH), 0.001, "the end");
    }

    /**
     * The types: numbers, colors, percentages and integers are interpolated (an integer is rounded).
     * <p>
     * 형: 숫자, 색, 백분율, 정수를 보간한다(정수는 반올림한다).
     */
    public void testOtherTypes() {
        Page p = new Page(REG + "#a{--n:0;--c:#000000;--p:0%;--i:0;transition:--n 1s linear, --c 1s linear, --p 1s linear, --i 1s linear;"
                + "opacity:var(--n);color:var(--c);width:var(--p)} #a.x{--n:1;--c:#ff0000;--p:100%;--i:5}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.style("a");
        p.at(0);
        p.at(500);
        Check.equal("0.5", custom(p, "a", "--n"), "a number");
        Check.near(0.5, p.style("a").number(Prop.OPACITY), 0.001, "opacity follows the number");
        Check.equal("rgb(128, 0, 0)", custom(p, "a", "--c"), "a color");
        Check.equal("50%", custom(p, "a", "--p"), "a percentage");
        Check.equal("3", custom(p, "a", "--i"), "an integer is rounded (2.5 becomes 3)");
        p.at(1000);
        Check.equal("1", custom(p, "a", "--n"), "the end of the number");
        Check.equal("5", custom(p, "a", "--i"), "the end of the integer");
    }

    /**
     * Only a registered property with a typed syntax is interpolated; an unregistered one and one with the universal syntax change at once (they have no transition),
     * and a property whose values cannot be mixed (different types) changes at the end of the half.
     * <p>
     * 형 있는 문법으로 등록된 속성만 보간한다. 등록되지 않은 속성과 보편 문법의 속성은 한 번에 바뀌고(전환 없음), 값을 섞을 수 없는 속성(다른 형)은 절반에서 바뀐다.
     */
    public void testNotInterpolated() {
        Page p = new Page("@property --u { syntax: '*'; inherits: true; initial-value: 0px } "
                + "#a{--u:0px;--v:0px;transition:--u 1s linear, --v 1s linear} #a.x{--u:100px;--v:100px}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.style("a");
        p.at(500);
        Check.equal("100px", custom(p, "a", "--u"), "a property with the universal syntax changes at once");
        Check.equal("100px", custom(p, "a", "--v"), "an unregistered property changes at once");
        Check.isFalse(p.engine.isActive(), "and nothing runs");
        Check.equal("[]", p.events.toString(), "no transition events");
        Page q = new Page("@property --m { syntax: '<length> | <percentage>'; inherits: true; initial-value: 0px } "
                + "#a{--m:0px;transition:--m 1s linear} #a.x{--m:100%}", "<div id=a></div>");
        q.style("a");
        q.at(0);
        q.el("a").setAttribute("class", "x");
        q.style("a");
        q.at(500);
        Check.equal("100%", custom(q, "a", "--m"), "a length and a percentage cannot be mixed, so the value changes at once");
    }

    /**
     * <code>@keyframes</code> animate registered properties between the declared values, the declarations that use them with <code>var()</code> see the values, the
     * missing <code>from</code> is the underlying value, and the iterations and the fill mode apply as for any property.
     * <p>
     * <code>@keyframes</code> 는 등록된 속성을 선언한 값 사이에서 애니메이션하고, <code>var()</code> 로 그것을 쓰는 선언이 값을 보며, 없는 <code>from</code> 은 기저 값이고,
     * 반복과 채움은 다른 속성과 같이 적용된다.
     */
    public void testKeyframes() {
        Page p = new Page(REG + "@keyframes grow { to { --w: 300px } } @keyframes multi { from { --w: 0px; --n: 0 } 50% { --w: 100px; --n: 1 } to { --w: 400px; --n: 0 } } "
                + "#a{--w:100px;width:var(--w);animation:grow 1s linear forwards} #b{--w:0px;width:var(--w);opacity:var(--n);animation:multi 2s linear}",
                "<div id=a></div><div id=b></div>");
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "before the animation starts");
        p.at(0);
        p.style("a");
        p.style("b");
        p.at(0);
        p.at(500);
        Check.equal("200px", custom(p, "a", "--w"), "the missing from keyframe is the underlying value, half way is 200px");
        Check.near(200, p.style("a").px(Prop.WIDTH), 0.01, "the width follows");
        Check.near(50, p.style("b").px(Prop.WIDTH), 0.01, "the first interval of three keyframes");
        Check.near(0.5, p.style("b").number(Prop.OPACITY), 0.001, "a number animates with it");
        p.at(1000);
        Check.near(300, p.style("a").px(Prop.WIDTH), 0.001, "the end with the forwards fill");
        Check.near(100, p.style("b").px(Prop.WIDTH), 0.01, "the middle keyframe");
        p.at(1500);
        Check.near(250, p.style("b").px(Prop.WIDTH), 0.01, "the second interval");
        p.at(3000);
        Check.near(300, p.style("a").px(Prop.WIDTH), 0.001, "the fill keeps the value");
        Check.near(0, p.style("b").px(Prop.WIDTH), 0.001, "an animation without the fill goes back to the underlying value");
    }

    /**
     * An animation of a registered property that cannot be computed for the syntax (a wrong value in a keyframe) is skipped, and a keyframe of an unregistered
     * property changes nothing.
     * <p>
     * 문법에 맞지 않는 값(키프레임 안의 틀린 값)의 등록된 속성 애니메이션은 건너뛰고, 등록되지 않은 속성의 키프레임은 아무것도 바꾸지 않는다.
     */
    public void testInvalidKeyframeValues() {
        Page p = new Page(REG + "@keyframes bad { to { --w: red; --z: 5px } } #a{--w:100px;width:var(--w);animation:bad 1s linear forwards}", "<div id=a></div>");
        p.at(0);
        p.style("a");
        p.at(0);
        p.at(500);
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "a wrong value in a keyframe does not animate");
        Check.equal("100px", custom(p, "a", "--w"), "the property keeps its value");
        Check.equal("", custom(p, "a", "--z"), "an unregistered property of a keyframe is not set");
    }

    /**
     * A transition that is interrupted starts from the current value, and the end of the transition lets the style go back to the cascade; disposing the engine
     * leaves the cascade values.
     * <p>
     * 중단된 전환은 현재 값에서 다시 시작하고, 전환이 끝나면 스타일은 캐스케이드로 돌아가며, 엔진을 폐기하면 캐스케이드 값이 남는다.
     */
    public void testInterruptionAndDispose() {
        Page p = new Page(REG + "#a{--w:0px;width:var(--w);transition:--w 1s linear} #a.x{--w:100px}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.style("a");
        p.at(0);
        p.at(500);
        Check.near(50, p.style("a").px(Prop.WIDTH), 0.01, "half way");
        p.el("a").removeAttribute("class");
        p.style("a");
        p.at(500);
        Check.near(50, p.style("a").px(Prop.WIDTH), 0.01, "reversed: it starts from the current value");
        p.at(1000);
        Check.near(25, p.style("a").px(Prop.WIDTH), 0.01, "and runs back");
        p.at(1500);
        Check.near(0, p.style("a").px(Prop.WIDTH), 0.001, "to the end value");
        Check.isTrue(p.events.toString().indexOf("transitioncancel:--w") > 0, "the interrupted transition is cancelled: " + p.events);
        p.engine.dispose();
        Check.near(0, p.css.getComputedStyle(p.el("a")).px(Prop.WIDTH), 0.001, "no engine, no animation");
    }
}
