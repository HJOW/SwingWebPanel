package org.duckdns.hjow.swingwebpanel.css;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.CssAnimationEvent;
import org.duckdns.hjow.swingwebpanel.css.anim.Easing;
import org.duckdns.hjow.swingwebpanel.css.anim.Transforms;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the CSS transitions and animations (plan item P10-07, CSS Transitions Level 1, CSS Animations Level 1, CSS Easing Level 1)
 * with a clock that the test sets by hand: the value at a moment (delay, duration, easing, direction, iterations, fill mode, pausing,
 * keyframes with implicit ends and several offsets, inheritance by the children), the events and their order (run, start, end, cancel,
 * iteration), the interruption of a transition, the properties that do not transition, the interpolation of colors and transforms, and
 * the end of everything with {@link AnimationEngine#dispose()}.
 * <p>
 * 손으로 정하는 시계를 쓴 CSS 전환과 애니메이션 시험(계획 P10-07, CSS Transitions Level 1, CSS Animations Level 1, CSS Easing Level 1): 어느 순간의 값(지연, 지속 시간,
 * 완화, 방향, 반복, 채움, 정지, 암시적 끝과 여러 오프셋이 있는 키프레임, 자식의 상속), 이벤트와 순서(run, start, end, cancel, iteration), 전환의 중단, 전환하지 않는
 * 속성, 색과 변환의 보간, {@link AnimationEngine#dispose()} 로 모두 끝내는 것을 확인한다.
 */
public class AnimationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AnimationTest() {
    }

    /**
     * A page with a manual clock, an animation engine and a recorder of events.
     * <p>
     * 손으로 정하는 시계, 애니메이션 엔진, 이벤트 기록기를 가진 페이지.
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
        /** The events in the order of their dispatch, as "type:name:elapsed". 발생 순서의 이벤트("종류:이름:경과"). */
        final List<String> events = new ArrayList<String>();

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
            EventListener l = new EventListener() {
                /**
                 * Records the event.
                 * <p>
                 * 이벤트를 기록한다.
                 *
                 * @param event the event / 이벤트
                 */
                public void handleEvent(Event event) {
                    CssAnimationEvent e = (CssAnimationEvent) event;
                    events.add(e.getType() + ":" + (e.getPropertyName().length() > 0 ? e.getPropertyName() : e.getAnimationName()) + ":"
                            + Math.round(e.getElapsedTime() * 1000));
                }
            };
            String[] types = { "transitionrun", "transitionstart", "transitionend", "transitioncancel", "animationstart", "animationiteration",
                    "animationend", "animationcancel" };
            // 모든 CSS 이벤트 종류에 기록기를 달아 둔다.
            for (String t : types) {
                doc.getDocumentElement().addEventListener(t, l, false);
            }
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
     * A transition of the width: the value follows the linear easing, the transition ends with the end value, the events come in the
     * order run, start, end, and the engine is idle afterwards.
     * <p>
     * 너비의 전환: 값은 선형 완화를 따르고 전환은 끝 값으로 끝나며 이벤트는 run, start, end 순서로 오고 그 뒤 엔진은 쉰다.
     */
    public void testWidthTransitionAndEventOrder() {
        Page p = new Page("#a{width:100px;transition:width 1s linear} #a.big{width:200px}", "<div id=a></div>");
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "initial width");
        p.at(0);
        p.el("a").setAttribute("class", "big");
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "just after the change the transition starts from the old value");
        Check.isTrue(p.engine.isActive(), "a transition runs");
        p.at(0);
        p.at(500);
        Check.near(150, p.style("a").px(Prop.WIDTH), 0.01, "half way");
        p.at(750);
        Check.near(175, p.style("a").px(Prop.WIDTH), 0.01, "three quarters");
        p.at(1000);
        Check.near(200, p.style("a").px(Prop.WIDTH), 0.001, "the end value");
        Check.isFalse(p.engine.isActive(), "nothing runs afterwards");
        Check.equal("[transitionrun:width:0, transitionstart:width:0, transitionend:width:1000]", p.events.toString(), "the events");
        p.at(5000);
        Check.near(200, p.style("a").px(Prop.WIDTH), 0.001, "the value stays");
        Check.equal(3, p.events.size(), "no more events");
    }

    /**
     * The delay holds the start value and moves the start event; the easing function shapes the progress (ease at one half is about
     * 0.8).
     * <p>
     * 지연은 시작 값을 유지하고 시작 이벤트를 늦춘다. 완화 함수는 진행도를 만든다(ease 의 절반은 약 0.8).
     */
    public void testDelayAndEasing() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s 500ms ease} #a.x{opacity:0}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.style("a");
        p.at(250);
        Check.near(1, p.style("a").number(Prop.OPACITY), 0.0001, "during the delay the start value is kept");
        Check.equal("[transitionrun:opacity:0]", p.events.toString(), "run only, the delay is not over");
        p.at(1000);
        Check.near(1 - 0.8024, p.style("a").number(Prop.OPACITY), 0.01, "ease at one half");
        Check.equal("[transitionrun:opacity:0, transitionstart:opacity:0]", p.events.toString(), "start after the delay");
        p.at(1500);
        Check.near(0, p.style("a").number(Prop.OPACITY), 0.0001, "the end");
        Check.equal(3, p.events.size(), "run, start, end");
        Check.equal("transitionend:opacity:1000", p.events.get(2), "elapsed time of the end excludes the delay");
        Easing e = Easing.of(new CssFunctionValue("steps", java.util.Arrays.<CssValue>asList(new CssNumber(4, true))));
        Check.near(0.25, e.at(0.3), 1e-9, "steps(4): the second quarter");
        Check.near(1, e.at(1), 1e-9, "steps(4): the end");
        Easing bez = Easing.of(new CssFunctionValue("cubic-bezier", java.util.Arrays.<CssValue>asList(new CssNumber(0.5, false), new CssNumber(2, true),
                new CssNumber(0.5, false), new CssNumber(2, true))));
        Check.near(1.625, bez.at(0.5), 0.001, "a cubic-bezier may overshoot (the symmetric curve at one half)");
    }

    /**
     * Colors are interpolated, the children inherit the animated value, and a transition that is interrupted by a new change is
     * canceled and a new one starts from the current value.
     * <p>
     * 색은 보간되고 자식은 애니메이션 값을 상속하며, 새 변경에 중단된 전환은 취소되고 새 전환이 현재 값에서 시작한다.
     */
    public void testColorInheritanceAndInterruption() {
        Page p = new Page("#a{color:#ff0000;transition:color 1s linear} #a.b{color:#0000ff} #a.c{color:#00ff00}",
                "<div id=a><span id=s>text</span></div>");
        p.style("s");
        p.at(0);
        p.el("a").setAttribute("class", "b");
        p.style("s");
        p.at(500);
        int mid = p.style("a").color(Prop.COLOR);
        Check.near(128, (mid >> 16) & 0xFF, 1, "half red");
        Check.near(128, mid & 0xFF, 1, "half blue");
        Check.equal(mid, p.style("s").color(Prop.COLOR), "the child inherits the animated color");
        p.el("a").setAttribute("class", "c");
        p.style("a");
        p.at(500);
        Check.equal(mid, p.style("a").color(Prop.COLOR), "the new transition starts at the current value");
        Check.isTrue(p.events.contains("transitioncancel:color:500"), "the interrupted transition is canceled: " + p.events);
        p.at(1500);
        Check.equal(0xFF00FF00, p.style("a").color(Prop.COLOR), "ends with the new value");
    }

    /**
     * Properties that cannot be interpolated (<code>display</code>, <code>auto</code> to a length) do not start a transition, and a
     * duration of 0 means no transition.
     * <p>
     * 보간할 수 없는 속성(<code>display</code>, <code>auto</code> 에서 길이로)은 전환을 시작하지 않고 지속 시간 0 은 전환이 없다는 뜻이다.
     */
    public void testNothingToTransition() {
        Page p = new Page("#a{width:auto;transition:all 1s} #a.x{width:100px;display:inline-block}#b{width:10px} #b.x{width:20px}",
                "<div id=a></div><div id=b></div>");
        p.style("a");
        p.style("b");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.el("b").setAttribute("class", "x");
        p.style("a");
        p.style("b");
        p.at(10);
        Check.near(100, p.style("a").px(Prop.WIDTH), 0.001, "auto to a length does not interpolate");
        Check.near(20, p.style("b").px(Prop.WIDTH), 0.001, "no transition without a duration");
        Check.equal(0, p.events.size(), "no events: " + p.events);
        Check.isFalse(p.engine.isActive(), "nothing runs");
    }

    /**
     * A transform transition interpolates the functions: the translation is half way at one half.
     * <p>
     * 변환 전환은 함수를 보간한다: 중간 시점에 이동은 절반이다.
     */
    public void testTransformTransition() {
        Page p = new Page("#a{transform:translate(0px,0px) rotate(0deg);transition:transform 1s linear} #a.x{transform:translate(100px,40px) rotate(90deg)}",
                "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "x");
        p.style("a");
        p.at(500);
        AffineTransform m = Transforms.matrix(p.style("a").get(Prop.TRANSFORM), 0, 0);
        Check.near(100 / 2.0, m.getTranslateX(), 0.01, "half of the translation x");
        Check.near(40 / 2.0, m.getTranslateY(), 0.01, "half of the translation y");
        Check.near(Math.toRadians(45), Math.atan2(m.getShearY(), m.getScaleX()), 0.001, "half of the rotation");
    }

    /**
     * A keyframes animation: the value at the moments, alternate direction with two iterations, the forwards fill, and the events
     * start, iteration, end.
     * <p>
     * 키프레임 애니메이션: 여러 시각의 값, 두 번 반복하는 alternate 방향, forwards 채움, start, iteration, end 이벤트.
     */
    public void testKeyframesIterationsDirectionAndFill() {
        Page p = new Page("@keyframes move{from{left:0px} to{left:100px}} #a{position:relative;animation:move 2s linear 2 alternate forwards}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.style("a");
        Check.near(0, p.style("a").px(Prop.LEFT), 0.001, "at the start");
        p.at(1000);
        Check.near(50, p.style("a").px(Prop.LEFT), 0.01, "first iteration, half way");
        p.at(2500);
        Check.near(75, p.style("a").px(Prop.LEFT), 0.01, "second iteration runs backwards: a quarter in is 75");
        p.at(4000);
        Check.near(0, p.style("a").px(Prop.LEFT), 0.001, "the forwards fill keeps the last value (the end of the backwards iteration)");
        p.at(9000);
        Check.near(0, p.style("a").px(Prop.LEFT), 0.001, "it stays");
        Check.isFalse(p.engine.isActive(), "an ended animation does not need ticks");
        Check.equal("[animationstart:move:0, animationiteration:move:2000, animationend:move:4000]", p.events.toString(), "the events");
    }

    /**
     * Keyframes with a missing <code>from</code> use the underlying value, several selectors share a declaration, the timing function
     * inside a keyframe shapes its interval, and the fill mode backwards shows the first keyframe during the delay (not without it).
     * <p>
     * <code>from</code> 이 없는 키프레임은 기저 값을 쓰고, 여러 선택자는 선언을 공유하며, 키프레임 안의 타이밍 함수는 그 구간을 만들고, backwards 채움은 지연 동안 첫
     * 키프레임을 보인다(없으면 보이지 않는다).
     */
    public void testImplicitKeyframesSelectorsAndBackwardsFill() {
        Page p = new Page("@keyframes k{50%{opacity:0.2}} @keyframes w{0%,100%{width:10px} 50%{width:30px}}"
                + " #a{opacity:1;animation:k 1s linear} #b{width:50px;animation:w 2s linear 1s backwards} #c{width:50px;animation:w 2s linear 1s}",
                "<div id=a></div><div id=b></div><div id=c></div>");
        p.style("a");
        p.style("b");
        p.style("c");
        p.at(0);
        p.style("a");
        p.at(250);
        Check.near(0.6, p.style("a").number(Prop.OPACITY), 0.001, "a quarter: half way from the underlying 1 to 0.2");
        p.at(500);
        Check.near(0.2, p.style("a").number(Prop.OPACITY), 0.001, "the middle keyframe");
        p.at(750);
        Check.near(0.6, p.style("a").number(Prop.OPACITY), 0.001, "three quarters");
        p.at(1000);
        Check.near(1, p.style("a").number(Prop.OPACITY), 0.001, "after the end without fill: the underlying value");
        p.at(500);
        Check.near(10, p.style("b").px(Prop.WIDTH), 0.001, "backwards fill: the first keyframe during the delay");
        Check.near(50, p.style("c").px(Prop.WIDTH), 0.001, "no fill: the underlying value during the delay");
        p.at(1500);
        Check.near(20, p.style("b").px(Prop.WIDTH), 0.01, "at one quarter of the active time (delay 1s, 2s long): between 10 and 30");
        p.at(2000);
        Check.near(30, p.style("b").px(Prop.WIDTH), 0.01, "the middle keyframe of the shared declaration");
        p.at(2990);
        Check.near(10, p.style("b").px(Prop.WIDTH), 0.5, "just before the end the width is that of the 100% keyframe, which shares the declaration of 0%");
        p.at(3000);
        Check.near(50, p.style("b").px(Prop.WIDTH), 0.001, "the fill mode backwards does not hold the end: the underlying value");
    }

    /**
     * Pausing keeps the progress: the animation does not move while it is paused and goes on from there afterwards; removing the name
     * cancels the animation.
     * <p>
     * 정지하면 진행도가 유지된다: 정지된 동안 움직이지 않고 그 뒤 거기서 이어 간다. 이름을 없애면 애니메이션이 취소된다.
     */
    public void testPauseAndCancel() {
        Page p = new Page("@keyframes m{from{left:0px} to{left:100px}} #a{position:relative;animation:m 10s linear}"
                + " #a.p{animation-play-state:paused} #a.n{animation:none}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.style("a");
        p.at(2000);
        p.el("a").setAttribute("class", "p");
        p.style("a");
        p.at(8000);
        Check.near(20, p.style("a").px(Prop.LEFT), 0.01, "paused at 2s: the progress does not move");
        p.el("a").setAttribute("class", "");
        p.style("a");
        p.at(10000);
        Check.near(40, p.style("a").px(Prop.LEFT), 0.01, "resumed at 8s: two more seconds");
        p.el("a").setAttribute("class", "n");
        p.style("a");
        p.at(10100);
        Check.isTrue(p.events.contains("animationcancel:m:4000"), "canceled animation: " + p.events);
        Check.isTrue(Double.isNaN(p.style("a").px(Prop.LEFT)) || p.style("a").px(Prop.LEFT) == 0, "the animated value is gone");
    }

    /**
     * After {@link AnimationEngine#dispose()} nothing runs and no event comes.
     * <p>
     * {@link AnimationEngine#dispose()} 뒤에는 아무것도 실행되지 않고 이벤트도 오지 않는다.
     */
    public void testDispose() {
        Page p = new Page("#a{width:100px;transition:width 1s linear} #a.big{width:200px}", "<div id=a></div>");
        p.style("a");
        p.at(0);
        p.el("a").setAttribute("class", "big");
        p.style("a");
        p.engine.dispose();
        p.at(2000);
        Check.isFalse(p.engine.isActive(), "inactive");
        p.css.getResolver().invalidate();
        Check.near(200, p.style("a").px(Prop.WIDTH), 0.001, "the plain style");
        Check.equal(0, p.events.size(), "no event after dispose");
    }
}
