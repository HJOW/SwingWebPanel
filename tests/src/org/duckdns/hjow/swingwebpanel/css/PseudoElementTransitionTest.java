package org.duckdns.hjow.swingwebpanel.css;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.CssAnimationEvent;
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
 * Tests of the transitions of pseudo-elements (CSS Transitions Level 1 section 2: the pseudo-elements <code>::before</code> and <code>::after</code> are
 * transitioned like elements, and the events name the pseudo-element): a change of the style of <code>::after</code> that has a
 * <code>transition</code> runs over time, the events come in the order run, start, end with <code>pseudoElement</code> set to <code>::after</code>, a pseudo-element
 * without a transition changes at once, and the element itself and its pseudo-element transition independently. The time is set by hand.
 * <p>
 * 의사 요소의 전환 시험(CSS Transitions Level 1 2 절: <code>::before</code> 와 <code>::after</code> 는 요소처럼 전환하며 이벤트는 의사 요소의 이름을 가진다):
 * <code>transition</code> 이 있는 <code>::after</code> 의 스타일이 바뀌면 시간에 따라 진행하고, 이벤트는 <code>pseudoElement</code> 가 <code>::after</code> 로
 * 설정되어 run, start, end 순서로 오며, 전환이 없는 의사 요소는 곧바로 바뀌고, 요소 자신과 그 의사 요소의 전환은 서로 독립이다. 시각은 손으로 정한다.
 */
public class PseudoElementTransitionTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PseudoElementTransitionTest() {
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
        /** The events as "type:property:pseudo". 이벤트("종류:속성:의사 요소"). */
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
                    events.add(e.getType() + ":" + e.getPropertyName() + ":" + e.getPseudoElement());
                }
            };
            String[] types = { "transitionrun", "transitionstart", "transitionend", "transitioncancel" };
            // 모든 전환 이벤트 종류에 기록기를 달아 둔다.
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
         * Returns the style of the pseudo-element <code>::after</code> of an element.
         * <p>
         * 요소의 <code>::after</code> 의사 요소의 스타일을 반환한다.
         *
         * @param id the id / id
         * @return the style / 스타일
         */
        ComputedStyle after(String id) {
            return css.getResolver().getPseudoStyle(doc.getElementById(id), "after");
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

    /** The rules of the tests: a bar that grows in the pseudo-element when the class is set. 시험의 규칙: 클래스를 지정하면 의사 요소에서 자라는 막대. */
    private static final String BAR = "#a::after{content:'';display:block;width:0;opacity:1;transition:width 1s linear} #a.x::after{width:100px}";

    /**
     * The width of the pseudo-element grows over the second and the events name the pseudo-element.
     * <p>
     * 의사 요소의 너비가 1 초에 걸쳐 자라며 이벤트는 의사 요소의 이름을 가진다.
     */
    public void testWidthOfAfterTransitions() {
        Page p = new Page(BAR, "<div id=a></div>");
        Check.near(0, p.after("a").px(Prop.WIDTH), 0.01, "the first style starts no transition");
        p.el("a").setAttribute("class", "x");
        Check.near(0, p.after("a").px(Prop.WIDTH), 0.01, "right after the change: still the old value");
        Check.isTrue(p.engine.isActive(), "a transition runs");
        p.at(500);
        Check.near(50, p.after("a").px(Prop.WIDTH), 0.5, "half way");
        p.at(1000);
        Check.near(100, p.after("a").px(Prop.WIDTH), 0.01, "the end value");
        Check.isFalse(p.engine.isActive(), "nothing runs afterwards");
        Check.equal("[transitionrun:width:::after, transitionstart:width:::after, transitionend:width:::after]", p.events.toString(), "the events name the pseudo-element");
    }

    /**
     * A pseudo-element without a transition changes at once, and the element and its pseudo-element transition independently.
     * <p>
     * 전환이 없는 의사 요소는 곧바로 바뀌고, 요소와 그 의사 요소는 독립적으로 전환한다.
     */
    public void testNoTransitionAndIndependence() {
        Page p = new Page("#a::after{content:'';display:block;width:0} #a.x::after{width:100px}", "<div id=a></div>");
        Check.near(0, p.after("a").px(Prop.WIDTH), 0.01, "before");
        p.el("a").setAttribute("class", "x");
        Check.near(100, p.after("a").px(Prop.WIDTH), 0.01, "without transition: at once");
        Check.isFalse(p.engine.isActive(), "nothing runs");
        Page q = new Page("#a{opacity:1;transition:opacity 1s linear} #a.x{opacity:0} #a::after{content:'';display:block;width:0;transition:width 2s linear} #a.x::after{width:100px}",
                "<div id=a></div>");
        Check.near(1, q.css.getComputedStyle(q.el("a")).number(Prop.OPACITY), 0.01, "the element before");
        Check.near(0, q.after("a").px(Prop.WIDTH), 0.01, "the pseudo-element before");
        q.el("a").setAttribute("class", "x");
        q.css.getComputedStyle(q.el("a"));
        q.after("a");
        q.at(1000);
        Check.near(0, q.css.getComputedStyle(q.el("a")).number(Prop.OPACITY), 0.01, "the element transition of one second is over");
        Check.near(50, q.after("a").px(Prop.WIDTH), 0.5, "the pseudo-element transition of two seconds is half way");
        q.at(2000);
        Check.near(100, q.after("a").px(Prop.WIDTH), 0.01, "and ends later");
    }

    /**
     * The transition can be reversed: a change while it runs starts a new one from the current value.
     * <p>
     * 전환은 되돌릴 수 있다: 실행 중에 바꾸면 현재 값에서 새 전환이 시작된다.
     */
    public void testChangeWhileRunning() {
        Page p = new Page(BAR, "<div id=a></div>");
        p.after("a");
        p.el("a").setAttribute("class", "x");
        p.after("a");
        p.at(500);
        Check.near(50, p.after("a").px(Prop.WIDTH), 0.5, "half way");
        p.el("a").setAttribute("class", "");
        Check.near(50, p.after("a").px(Prop.WIDTH), 0.5, "a new transition starts from the current value");
        p.at(1000);
        Check.near(25, p.after("a").px(Prop.WIDTH), 0.5, "back towards 0 over the full second");
        Check.isTrue(p.events.contains("transitioncancel:width:::after"), "the first transition was canceled: " + p.events);
    }
}
