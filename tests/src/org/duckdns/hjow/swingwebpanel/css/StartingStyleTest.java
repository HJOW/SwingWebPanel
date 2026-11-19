package org.duckdns.hjow.swingwebpanel.css;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.anim.AnimationClock;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.CssAnimationEvent;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>@starting-style</code> (CSS Transitions Level 2, section 3.3, W3C Working Draft 4 February 2026): the rules inside it make the
 * <em>starting style</em> from which a transition starts on an element that had no before-change style (a new element, or one that was not rendered),
 * they do not apply to the after-change style, they cascade like other grouped rules (no new ordering), and they can be nested in a style rule. The
 * time is set by hand, as in {@link AnimationTest}.
 * <p>
 * <code>@starting-style</code> 시험(CSS Transitions Level 2 3.3 절, W3C 작업 초안 2026-02-04): 안의 규칙은 변경 전 스타일이 없던 요소(새 요소나 렌더링되지 않던
 * 요소)에서 전환이 출발하는 <em>시작 스타일</em>을 만들고, 변경 후 스타일에는 적용되지 않으며, 다른 그룹 규칙처럼 캐스케이드하고(새 순서 없음), 스타일 규칙 안에
 * 중첩할 수 있다. 시각은 {@link AnimationTest} 처럼 손으로 정한다.
 */
public class StartingStyleTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StartingStyleTest() {
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
        /** The events as "type:name". 이벤트("종류:이름"). */
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
                    events.add(e.getType() + ":" + e.getPropertyName());
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
         * Returns the opacity of an element by id.
         * <p>
         * id 로 요소의 불투명도를 반환한다.
         *
         * @param id the id / id
         * @return the opacity / 불투명도
         */
        double opacity(String id) {
            ComputedStyle s = css.getComputedStyle(doc.getElementById(id));
            return s.number(Prop.OPACITY);
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
     * A new element transitions from the starting style to its style: the opacity starts at 0 though the rule of the element says 1, and the
     * transition events come in the order run, start, end.
     * <p>
     * 새 요소는 시작 스타일에서 자신의 스타일로 전환한다: 요소의 규칙은 1 이지만 불투명도는 0 에서 시작하며 전환 이벤트는 run, start, end 순서로 온다.
     */
    public void testNewElementTransitionsFromTheStartingStyle() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear} @starting-style{#a{opacity:0}}", "<div id=a></div>");
        Check.near(0, p.opacity("a"), 0.0001, "the first style is the starting style");
        Check.isTrue(p.engine.isActive(), "a transition runs");
        p.at(500);
        Check.near(0.5, p.opacity("a"), 0.01, "half way");
        p.at(1000);
        Check.near(1, p.opacity("a"), 0.0001, "the end value");
        Check.isFalse(p.engine.isActive(), "nothing runs afterwards");
        Check.equal("[transitionrun:opacity, transitionstart:opacity, transitionend:opacity]", p.events.toString(), "the events");
    }

    /**
     * Without a rule inside <code>@starting-style</code>, a new element has no transition at all, and without a transition the starting style does not
     * change the style of the element (the rules do not apply to the after-change style).
     * <p>
     * <code>@starting-style</code> 안의 규칙이 없으면 새 요소는 전환이 없고, 전환이 없으면 시작 스타일이 요소의 스타일을 바꾸지 않는다(규칙은 변경 후 스타일에는
     * 적용되지 않는다).
     */
    public void testAfterChangeStyleIsNotAffected() {
        Page plain = new Page("#a{opacity:1;transition:opacity 1s linear}", "<div id=a></div>");
        Check.near(1, plain.opacity("a"), 0.0001, "no starting style: no transition");
        Check.isFalse(plain.engine.isActive(), "nothing runs");
        Page noTransition = new Page("#a{opacity:1} @starting-style{#a{opacity:0}}", "<div id=a></div>");
        Check.near(1, noTransition.opacity("a"), 0.0001, "the starting rule does not apply to the element itself");
        Check.isFalse(noTransition.engine.isActive(), "no transition property: nothing runs");
        Check.isTrue(noTransition.css.getResolver().hasStartingStyles(), "the resolver knows the starting rules");
        Check.isFalse(plain.css.getResolver().hasStartingStyles(), "and a document without them");
    }

    /**
     * A <code>@starting-style</code> nested in a style rule takes the selector of the parent.
     * <p>
     * 스타일 규칙 안에 중첩한 <code>@starting-style</code> 은 부모의 선택자를 쓴다.
     */
    public void testNestedStartingStyle() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear; @starting-style{opacity:0.2}}", "<div id=a></div>");
        Check.near(0.2, p.opacity("a"), 0.0001, "starts from the nested starting style");
        p.at(500);
        Check.near(0.6, p.opacity("a"), 0.01, "half way between 0.2 and 1");
        p.at(1000);
        Check.near(1, p.opacity("a"), 0.0001, "the end value");
    }

    /**
     * The rules inside <code>@starting-style</code> cascade like other rules: a rule of higher specificity outside of it wins, so there is nothing
     * to transition from.
     * <p>
     * <code>@starting-style</code> 안의 규칙은 다른 규칙처럼 캐스케이드한다: 밖에 있는 구체성이 더 높은 규칙이 이기므로 전환할 것이 없다.
     */
    public void testStartingRulesDoNotWinByOrder() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear} @starting-style{div{opacity:0}}", "<div id=a></div>");
        Check.near(1, p.opacity("a"), 0.0001, "the id selector outside wins over the type selector inside");
        Check.isFalse(p.engine.isActive(), "so nothing transitions");
        Page q = new Page("div{opacity:1;transition:opacity 1s linear} @starting-style{#a{opacity:0}}", "<div id=a></div><div id=b></div>");
        Check.near(0, q.opacity("a"), 0.0001, "a more specific starting rule gives the starting value");
        Check.near(1, q.opacity("b"), 0.0001, "an element it does not match has none");
    }

    /**
     * An element that changes from <code>display: none</code> to rendered has no before-change style, so it transitions from the starting style; an
     * element that stays <code>display: none</code> does not.
     * <p>
     * <code>display: none</code> 에서 렌더링되는 요소는 변경 전 스타일이 없으므로 시작 스타일에서 전환하고, 계속 <code>display: none</code> 인 요소는 전환하지 않는다.
     */
    public void testDisplayNoneToRendered() {
        Page p = new Page("#a{opacity:1;transition:opacity 1s linear} #a.h{display:none} @starting-style{#a{opacity:0}}", "<div id=a class=h></div>");
        Check.near(1, p.opacity("a"), 0.0001, "not rendered: no transition");
        Check.isFalse(p.engine.isActive(), "nothing runs");
        p.at(0);
        p.el("a").setAttribute("class", "");
        Check.near(0, p.opacity("a"), 0.0001, "rendered now: from the starting style");
        p.at(500);
        Check.near(0.5, p.opacity("a"), 0.01, "half way");
        p.at(1000);
        Check.near(1, p.opacity("a"), 0.0001, "the end value");
        p.el("a").setAttribute("class", "h");
        p.at(1000);
        Check.near(1, p.opacity("a"), 0.0001, "hiding again starts no transition (display does not transition)");
    }

    /**
     * An element inserted later also starts from the starting style, and the style is not computed again as a starting style once the element has a
     * before-change style.
     * <p>
     * 나중에 삽입한 요소도 시작 스타일에서 출발하며, 요소가 변경 전 스타일을 가진 뒤에는 시작 스타일로 다시 계산하지 않는다.
     */
    public void testInsertedElement() {
        Page p = new Page("div{opacity:1;transition:opacity 1s linear} .x{opacity:0.5} @starting-style{div{opacity:0}}", "");
        p.at(0);
        Element d = p.doc.createElement("div");
        d.setAttribute("id", "n");
        p.doc.getElementsByTagName("body").item(0).appendChild(d);
        Check.near(0, p.opacity("n"), 0.0001, "the inserted element starts at the starting value");
        p.at(1000);
        Check.near(1, p.opacity("n"), 0.0001, "and reaches its value");
        d.setAttribute("class", "x");
        Check.near(1, p.opacity("n"), 0.0001, "a change after that transitions from the before-change style, not from the starting style");
        p.at(1500);
        Check.near(0.75, p.opacity("n"), 0.01, "half way from 1 to 0.5");
    }

    /**
     * The rule is a grouping rule of the type <code>@starting-style</code> and serializes as such.
     * <p>
     * 규칙은 <code>@starting-style</code> 종류의 그룹 규칙이며 그대로 직렬화된다.
     */
    public void testRuleModel() {
        Page p = new Page("@starting-style{#a{opacity:0}}", "<div id=a></div>");
        CssRule r = p.css.getStyleSheets().get(0).getRules().get(0);
        Check.isTrue(r instanceof GroupRule, "a group rule");
        Check.equal(CssRule.STARTING_STYLE_RULE, r.getType(), "the type");
        Check.equal(0, r.getCssomType(), "no legacy number");
        Check.isTrue(r.getCssText().startsWith("@starting-style {"), "the text starts with the at-rule name");
        Check.equal(1, ((GroupRule) r).getRules().size(), "the contained rule");
    }
}
