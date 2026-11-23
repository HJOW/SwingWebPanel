package org.duckdns.hjow.swingwebpanel.css;

import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>transition-behavior: allow-discrete</code> (CSS Transitions Level 2 section 2.3 and 3.1, https://www.w3.org/TR/css-transitions-2/#transition-behavior-property):
 * a property whose values cannot be interpolated also transitions, flipping at the half of the transition; <code>display</code> (and
 * <code>content-visibility</code>) keeps the value that is not <code>none</code> during the whole transition, so an element can fade out before it
 * disappears and is rendered at once when it appears. Without <code>allow-discrete</code> such a property changes at once. The time is set by hand, as in
 * {@link StartingStyleTest}.
 * <p>
 * <code>transition-behavior: allow-discrete</code> 시험(CSS Transitions Level 2 2.3, 3.1 절): 값을 보간할 수 없는 속성도 전환되며 전환의 절반에서 바뀐다.
 * <code>display</code>(와 <code>content-visibility</code>)는 전환 내내 <code>none</code> 이 아닌 값을 유지하므로 요소는 사라지기 전에 서서히 사라질 수 있고 나타날 때는 곧바로
 * 렌더링된다. <code>allow-discrete</code> 가 없으면 그런 속성은 곧바로 바뀐다. 시각은 {@link StartingStyleTest} 처럼 손으로 정한다.
 */
public class DiscreteTransitionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DiscreteTransitionTest() {
    }

    /**
     * A page with a manual clock and an animation engine.
     * <p>
     * 손으로 정하는 시계와 애니메이션 엔진을 가진 페이지.
     */
    private static final class Page {
        /** The time in milliseconds. 시각(밀리초). */
        final long[] time = new long[1];
        /** The document. 문서. */
        final Document doc;
        /** The CSS engine. CSS 엔진. */
        final CssEngine css;
        /** The animation engine. 애니메이션 엔진. */
        final AnimationEngine engine;

        /**
         * Builds a page.
         * <p>
         * 페이지를 만든다.
         *
         * @param style the style rules / 스타일 규칙
         * @param body  the body markup / body 마크업
         */
        Page(String style, String body) {
            doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>" + style + "</style></head><body>" + body + "</body></html>");
            css = new CssEngine(doc, null, new MediaEnvironment(400, 300));
            css.loadStyleSheets();
            engine = new AnimationEngine(css, new AnimationClock() {
                /**
                 * Returns the time that the test set.
                 * <p>
                 * 시험이 정한 시각을 반환한다.
                 *
                 * @return milliseconds / 밀리초
                 */
                public long nowMillis() {
                    return time[0];
                }
            });
        }

        /**
         * Sets the time and advances the engine.
         * <p>
         * 시각을 정하고 엔진을 진행한다.
         *
         * @param t milliseconds / 밀리초
         */
        void at(long t) {
            time[0] = t;
            engine.tick();
        }

        /**
         * Returns the style of an element by id.
         * <p>
         * id 로 요소의 스타일을 반환한다.
         *
         * @param id the id / id
         * @return the style / 스타일
         */
        ComputedStyle style(String id) {
            return css.getComputedStyle(doc.getElementById(id));
        }

        /**
         * Returns the element by id.
         * <p>
         * id 로 요소를 반환한다.
         *
         * @param id the id / id
         * @return the element / 요소
         */
        Element el(String id) {
            return doc.getElementById(id);
        }
    }

    /**
     * An element that is hidden with <code>display: none</code> stays rendered while its opacity fades out, and is removed when the transition ends.
     * <p>
     * <code>display: none</code> 으로 숨겨지는 요소는 불투명도가 줄어드는 동안 렌더링된 채 있다가 전환이 끝나면 제거된다.
     */
    public void testExitAnimationKeepsTheElementRendered() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear,display 1s allow-discrete} #a.h{opacity:0;display:none}", "<div id=a></div>");
        p.at(0);
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "rendered at the start");
        p.el("a").setAttribute("class", "h");
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "right after the change: still rendered");
        p.at(500);
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "half way: still rendered");
        Check.near(0.5, p.style("a").number(Prop.OPACITY), 0.01, "while it fades out");
        p.at(999);
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "just before the end: still rendered");
        p.at(1000);
        Check.equal("none", p.style("a").keyword(Prop.DISPLAY), "at the end the element is removed");
        Check.isFalse(p.engine.isActive(), "nothing runs afterwards");
    }

    /**
     * An element that appears is rendered at once, and with <code>@starting-style</code> it fades in.
     * <p>
     * 나타나는 요소는 곧바로 렌더링되며 <code>@starting-style</code> 이 있으면 서서히 나타난다.
     */
    public void testEnterAnimationRendersAtOnce() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear,display 1s allow-discrete} #a.h{opacity:0;display:none} @starting-style{#a{opacity:0}}",
                "<div id=a class=h></div>");
        p.at(0);
        Check.equal("none", p.style("a").keyword(Prop.DISPLAY), "hidden at the start");
        p.el("a").setAttribute("class", "");
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "rendered at once");
        Check.near(0, p.style("a").number(Prop.OPACITY), 0.01, "from the starting style");
        p.at(500);
        Check.near(0.5, p.style("a").number(Prop.OPACITY), 0.01, "half way");
        Check.equal("block", p.style("a").keyword(Prop.DISPLAY), "rendered");
        p.at(1000);
        Check.near(1, p.style("a").number(Prop.OPACITY), 0.01, "the end value");
    }

    /**
     * Without <code>allow-discrete</code> <code>display</code> changes at once and nothing waits for the opacity.
     * <p>
     * <code>allow-discrete</code> 가 없으면 <code>display</code> 는 곧바로 바뀌고 불투명도를 기다리지 않는다.
     */
    public void testNormalBehaviorChangesAtOnce() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear,display 1s} #a.h{opacity:0;display:none}", "<div id=a></div>");
        p.at(0);
        p.el("a").setAttribute("class", "h");
        Check.equal("none", p.style("a").keyword(Prop.DISPLAY), "removed at once");
    }

    /**
     * Another discrete value flips at the half of the transition.
     * <p>
     * 다른 이산 값은 전환의 절반에서 바뀐다.
     */
    public void testDiscreteValueFlipsAtHalf() {
        Page p = new Page("#a{text-align:left;transition:text-align 1s linear allow-discrete} #a.x{text-align:right}", "<div id=a></div>");
        p.at(0);
        Check.equal("left", p.style("a").keyword(Prop.TEXT_ALIGN), "the first style is computed before the change");
        p.el("a").setAttribute("class", "x");
        Check.equal("left", p.style("a").keyword(Prop.TEXT_ALIGN), "right after the change the transition starts from the old value");
        p.at(400);
        Check.equal("left", p.style("a").keyword(Prop.TEXT_ALIGN), "before the half");
        p.at(600);
        Check.equal("right", p.style("a").keyword(Prop.TEXT_ALIGN), "after the half");
        Page q = new Page("#a{text-align:left;transition:text-align 1s linear} #a.x{text-align:right}", "<div id=a></div>");
        q.at(0);
        Check.equal("left", q.style("a").keyword(Prop.TEXT_ALIGN), "the first style is computed before the change");
        q.el("a").setAttribute("class", "x");
        Check.equal("right", q.style("a").keyword(Prop.TEXT_ALIGN), "without allow-discrete: at once");
    }

    /**
     * The shorthand takes <code>allow-discrete</code> in any place, and the longhand takes a list that is matched with the properties like the other
     * longhands.
     * <p>
     * 단축 속성은 <code>allow-discrete</code> 를 어느 자리에서나 받고 개별 속성은 다른 개별 속성처럼 속성에 대응시키는 목록을 받는다.
     */
    public void testParsing() {
        Page p = new Page("#a{transition:opacity 2s allow-discrete} #b{transition:display 1s ease-in 1s allow-discrete,opacity 3s} #c{transition-behavior:normal,allow-discrete}",
                "<div id=a></div><div id=b></div><div id=c></div>");
        Check.equal("allow-discrete", p.style("a").get(Prop.TRANSITION_BEHAVIOR).toCssText(), "the shorthand value");
        Check.equal("allow-discrete, normal", p.style("b").get(Prop.TRANSITION_BEHAVIOR).toCssText(), "two layers");
        Check.equal("normal, allow-discrete", p.style("c").get(Prop.TRANSITION_BEHAVIOR).toCssText(), "the longhand list");
    }
}
