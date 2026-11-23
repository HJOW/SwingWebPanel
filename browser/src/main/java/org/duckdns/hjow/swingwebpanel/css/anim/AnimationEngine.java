package org.duckdns.hjow.swingwebpanel.css.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleAnimator;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.CssRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.GroupRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.KeyframesRule;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The CSS transitions and animations of one document (plan item P10-07, CSS Transitions Level 1 https://www.w3.org/TR/css-transitions-1/
 * and CSS Animations Level 1 https://www.w3.org/TR/css-animations-1/, with the easing of CSS Easing Level 1). The engine is the
 * {@link StyleAnimator} of the {@link StyleResolver}: when the style of an element is computed it compares it with the previous one
 * and starts a transition for every property that changed and for which the new style has a transition with a duration or a delay,
 * it starts the animations that <code>animation-name</code> lists (finding the <code>@keyframes</code> by name, the last one wins), and it
 * returns the style with the values of the running transitions and animations at the moment of the {@link AnimationClock} in the composite order of
 * Web Animations (the transitions at the bottom, the CSS animations above them, the script animations of {@link #getWebAnimations()} on top).
 * The descendants inherit the animated values, because the style of the element is what they inherit from. {@link #tick()} advances
 * the engine: it finishes the transitions and animations that ran out, drops the cached styles of the elements whose value is moving
 * (so that the next style computation gives the new value), and then fires the events in the order in which they happened:
 * <code>transitionrun</code>, <code>transitionstart</code>, <code>transitionend</code>, <code>transitioncancel</code>,
 * <code>animationstart</code>, <code>animationiteration</code>, <code>animationend</code>, <code>animationcancel</code> (see
 * {@link CssAnimationEvent}). The host calls {@link #tick()} before it renders and asks {@link #isActive()} whether to render again soon
 * (about 60 times a second) or to stop; nothing in the engine starts a thread, so everything runs on the document thread, and the
 * animations of a page that is replaced or disposed end with {@link #dispose()} without any event. JavaScript being disabled does not
 * change anything here: the engine does not depend on the script engine.
 * <p>
 * Supported: every property whose two computed values can be interpolated ({@link Interpolator}: numbers, lengths, percentages, colors,
 * <code>transform</code>, lists of those, <code>visibility</code>); <code>transition-property</code> with <code>all</code>, names and
 * shorthand names; <code>transition-duration/delay/timing-function</code> lists repeated to the length of the property list;
 * <code>animation-*</code> lists matched by name index; keyframes with several selectors (<code>0%, 50%</code>), missing
 * <code>from</code>/<code>to</code> (the underlying value), the <code>animation-timing-function</code> inside a keyframe (for the interval
 * that starts there), <code>animation-play-state</code> (pausing keeps the progress), the directions, fill modes and iteration counts
 * (also fractional and <code>infinite</code>). Documented limits: no transition starts for the style that is computed first (no
 * <code>@starting-style</code>); a transition that is interrupted starts from the current value over the full duration (no reversing
 * shortening factor); percentages inside the interpolation of two <code>transform</code> lists with different functions are resolved
 * against a reference box of size 0; pseudo-elements run transitions only (no keyframe animations); the properties of a keyframe that are parsed but not
 * interpolable change halfway (discrete); changes of <code>@keyframes</code> after an animation started do not restart it.
 * <p>
 * 한 문서의 CSS 전환과 애니메이션(계획 P10-07, CSS Transitions Level 1 https://www.w3.org/TR/css-transitions-1/, CSS Animations Level 1
 * https://www.w3.org/TR/css-animations-1/, 완화는 CSS Easing Level 1). 엔진은 {@link StyleResolver} 의 {@link StyleAnimator} 이다: 요소의 스타일을 계산할 때 이전 것과
 * 비교해 바뀐 속성 중 새 스타일이 지속 시간이나 지연을 가진 전환을 정한 것마다 전환을 시작하고, <code>animation-name</code> 이 나열한 애니메이션을 시작하며(<code>@keyframes</code>
 * 를 이름으로 찾고 마지막 것이 이긴다), {@link AnimationClock} 의 지금 시각의 실행 중인 전환과 애니메이션의 값을 Web Animations 의 합성 순서(가장 아래에 전환, 그 위에 CSS
 * 애니메이션, 맨 위에 {@link #getWebAnimations()} 의 스크립트 애니메이션)로 적용한 스타일을 돌려준다. 자손은 요소의 스타일을 상속하므로
 * 애니메이션 값을 상속한다. {@link #tick()} 은 엔진을 진행한다: 끝난 전환과 애니메이션을 마치고, 값이 움직이는 요소의 캐시한 스타일을 버리며(다음 스타일 계산이 새 값을 주도록),
 * 일어난 순서대로 이벤트를 발생시킨다: <code>transitionrun</code>, <code>transitionstart</code>, <code>transitionend</code>, <code>transitioncancel</code>,
 * <code>animationstart</code>, <code>animationiteration</code>, <code>animationend</code>, <code>animationcancel</code>({@link CssAnimationEvent} 참조). 호스트는
 * 렌더링 전에 {@link #tick()} 을 부르고 {@link #isActive()} 로 곧(초당 약 60번) 다시 렌더링할지 멈출지 묻는다. 엔진은 스레드를 만들지 않으므로 모두 문서 스레드에서
 * 동작하며, 교체되거나 폐기되는 페이지의 애니메이션은 {@link #dispose()} 로 이벤트 없이 끝난다. JavaScript 를 끄는 것은 여기에 영향이 없다: 엔진은 스크립트 엔진에 의존하지 않는다.
 * <p>
 * 지원: 두 계산 값을 보간할 수 있는 모든 속성({@link Interpolator}: 숫자, 길이, 백분율, 색, <code>transform</code>, 그들의 목록, <code>visibility</code>), <code>all</code>, 이름,
 * 단축 속성 이름을 쓴 <code>transition-property</code>, 속성 목록 길이로 되풀이되는 <code>transition-duration/delay/timing-function</code> 목록, 이름 위치로 짝짓는
 * <code>animation-*</code> 목록, 선택자가 여럿인 키프레임(<code>0%, 50%</code>), 없는 <code>from</code>/<code>to</code>(기저 값), 키프레임 안의
 * <code>animation-timing-function</code>(그 키프레임에서 시작하는 구간), <code>animation-play-state</code>(멈추면 진행도 유지), 방향, 채움, 반복 횟수(소수와
 * <code>infinite</code> 포함). 문서화된 한계: 처음 계산한 스타일에는 전환이 시작되지 않는다(<code>@starting-style</code> 없음), 중단된 전환은 현재 값에서 전체 지속 시간으로 다시
 * 시작한다(되돌림 단축 계수 없음), 함수가 다른 두 <code>transform</code> 목록을 보간할 때 백분율은 크기 0 인 기준 상자로 푼다, 의사 요소는 전환만 하고 키프레임 애니메이션은 하지 않는다, 보간할 수
 * 없는 키프레임 속성은 중간에서 바뀐다(이산), 애니메이션이 시작된 뒤 <code>@keyframes</code> 가 바뀌어도 다시 시작하지 않는다.
 */
public final class AnimationEngine implements StyleAnimator {

    /** The CSS engine of the document. 문서의 CSS 엔진. */
    private final CssEngine css;
    /** The style resolver. 스타일 리졸버. */
    private final StyleResolver resolver;
    /** The time source. 시간 원천. */
    private final AnimationClock clock;
    /** The state of the elements that were seen (weak: the state does not keep an element alive). 본 요소의 상태(약한 참조: 상태는 요소를 살려 두지 않는다). */
    private final Map<Element, State> states = new WeakHashMap<Element, State>();

    /**
     * The states of the pseudo-elements by originating element and pseudo-element name (<code>before</code>, <code>after</code>...): only transitions run on
     * them.
     * <p>
     * 원본 요소와 의사 요소 이름(<code>before</code>, <code>after</code> ...)별 의사 요소의 상태: 전환만 실행한다.
     */
    private final Map<Element, Map<String, State>> pseudoStates = new WeakHashMap<Element, Map<String, State>>();
    /** The elements with something running (strong only while they run). 실행 중인 것이 있는 요소(실행되는 동안만 강한 참조). */
    private final Set<Element> active = Collections.newSetFromMap(new IdentityHashMap<Element, Boolean>());
    /** The events waiting for the next {@link #tick()}. 다음 {@link #tick()} 을 기다리는 이벤트. */
    private final List<Object[]> pending = new ArrayList<Object[]>();
    /** Set by {@link #dispose()}. {@link #dispose()} 가 정한다. */
    private boolean disposed;

    /**
     * The animations that scripts created with Web Animations (<code>element.animate()</code>, P10-08-h): they are at the top of the effect stack.
     * <p>
     * 스크립트가 Web Animations 로 만든 애니메이션(<code>element.animate()</code>, P10-08-h): 효과 스택의 맨 위에 있다.
     */
    private final WebAnimationSet webAnimations;

    /**
     * True when a script animation dropped a cached style since the last {@link #tick()}: the next tick reports a change, so the page is laid out again.
     * <p>
     * 마지막 {@link #tick()} 이후 스크립트 애니메이션이 캐시한 스타일을 버렸으면 true: 다음 틱이 변경을 알려 페이지가 다시 배치된다.
     */
    private boolean scriptStylesChanged;

    /**
     * Records that a script animation changed the style of an element (called by {@link WebAnimationSet}).
     * <p>
     * 스크립트 애니메이션이 요소의 스타일을 바꿨음을 기록한다({@link WebAnimationSet} 이 부른다).
     */
    void scriptStyleChanged() {
        scriptStylesChanged = true;
    }

    /**
     * The ids that stand for registered custom properties start here: a custom property has no property id, so the engine numbers the names that it meets and
     * lets them go through the code of the transitions and the animations as if they were properties of the table.
     * <p>
     * 등록된 사용자 정의 속성을 나타내는 id 가 시작하는 값: 사용자 정의 속성에는 속성 id 가 없으므로 엔진이 만나는 이름에 번호를 붙여 속성 표의 속성처럼 전환과 애니메이션 코드를
     * 지나가게 한다.
     */
    private static final int CUSTOM_BASE = 100000;

    /** The names of the registered custom properties that the engine has numbered; the id is {@link #CUSTOM_BASE} plus the index. 엔진이 번호를 붙인 등록된 사용자 정의 속성의 이름. id 는 {@link #CUSTOM_BASE} 에 위치를 더한 것이다. */
    private final List<String> customNames = new ArrayList<String>();

    /**
     * Returns the id that stands for a registered custom property.
     * <p>
     * 등록된 사용자 정의 속성을 나타내는 id 를 반환한다.
     *
     * @param name the name of the property / 속성 이름
     * @return the id / id
     */
    private int customId(String name) {
        int i = customNames.indexOf(name);
        if (i < 0) {
            customNames.add(name);
            i = customNames.size() - 1;
        }
        return CUSTOM_BASE + i;
    }

    /**
     * Returns the name of the registered custom property that an id stands for.
     * <p>
     * id 가 나타내는 등록된 사용자 정의 속성의 이름을 반환한다.
     *
     * @param id the id, at least {@link #CUSTOM_BASE} / id. {@link #CUSTOM_BASE} 이상
     * @return the name / 이름
     */
    private String customName(int id) {
        return customNames.get(id - CUSTOM_BASE);
    }

    /**
     * Returns the value of a property in a style for the comparison of a transition: the computed value, or the typed value of a registered custom property.
     * <p>
     * 전환의 비교를 위해 스타일에 있는 속성의 값을 반환한다: 계산 값 또는 등록된 사용자 정의 속성의 형 있는 값.
     *
     * @param s  the style / 스타일
     * @param id the property id or the id of a custom property / 속성 id 또는 사용자 정의 속성의 id
     * @return the value, or null if the custom property has none / 값. 사용자 정의 속성에 값이 없으면 null
     */
    private CssValue valueIn(ComputedStyle s, int id) {
        return id >= CUSTOM_BASE ? resolver.typedCustomValue(s, customName(id)) : s.get(id);
    }

    /**
     * Creates the engine and installs it into the style resolver of the CSS engine.
     * <p>
     * 엔진을 만들고 CSS 엔진의 스타일 리졸버에 설치한다.
     *
     * @param css   the CSS engine of the document / 문서의 CSS 엔진
     * @param clock the time source / 시간 원천
     */
    public AnimationEngine(CssEngine css, AnimationClock clock) {
        if (css == null || clock == null) {
            throw new IllegalArgumentException("css engine and clock must not be null");
        }
        this.css = css;
        this.resolver = css.getResolver();
        this.clock = clock;
        this.webAnimations = new WebAnimationSet(resolver, this);
        resolver.setAnimator(this);
    }

    /**
     * Returns the Web Animations state of the document (the script host connects to it to implement <code>element.animate()</code>).
     * <p>
     * 문서의 Web Animations 상태를 반환한다(스크립트 호스트가 <code>element.animate()</code> 를 구현하려고 연결한다).
     *
     * @return the state / 상태
     */
    public WebAnimationSet getWebAnimations() {
        return webAnimations;
    }

    /**
     * Returns the style of an element below its script animations (the cascade with the CSS transitions and animations), as it was last computed; the
     * base of <code>commitStyles()</code>.
     * <p>
     * 요소의 스크립트 애니메이션 아래의 스타일(CSS 전환과 애니메이션이 적용된 캐스케이드)을 마지막으로 계산한 대로 반환한다. <code>commitStyles()</code> 의 기준이다.
     *
     * @param e the element / 요소
     * @return the style, or null if it was never computed / 스타일. 계산한 적이 없으면 null
     */
    ComputedStyle belowScripts(Element e) {
        State st = states.get(e);
        return st == null ? null : (st.belowScripts != null ? st.belowScripts : st.lastBase);
    }

    /**
     * The state of one element: the style that the cascade computed last time and what is running.
     * <p>
     * 요소 하나의 상태: 캐스케이드가 마지막으로 계산한 스타일과 실행 중인 것.
     */
    private static final class State {
        /** The pseudo-element that this state is of (<code>::before</code>), null for the element itself. 이 상태가 속한 의사 요소(<code>::before</code>). 요소 자신이면 null. */
        String pseudo;
        /** The last style of the cascade (without animations). 캐스케이드의 마지막 스타일(애니메이션 없이). */
        ComputedStyle lastBase;
        /** The last style with the CSS transitions and animations but without the script animations, or null. 마지막으로 CSS 전환과 애니메이션은 적용했지만 스크립트 애니메이션은 적용하지 않은 스타일. 없으면 null. */
        ComputedStyle belowScripts;
        /** The running transitions by property id. 속성 id 별 실행 중인 전환. */
        final Map<Integer, Transition> transitions = new LinkedHashMap<Integer, Transition>();
        /** The animations in the order of <code>animation-name</code>. <code>animation-name</code> 순서의 애니메이션. */
        final List<Anim> anims = new ArrayList<Anim>();
    }

    /**
     * One running transition.
     * <p>
     * 실행 중인 전환 하나.
     */
    private static final class Transition {
        /** The property. 속성. */
        int prop;
        /** The start value. 시작 값. */
        CssValue from;
        /** The end value. 끝 값. */
        CssValue to;
        /** When it was created (the delay starts here). 만들어진 시각(지연이 여기서 시작한다). */
        long created;
        /** The delay in milliseconds. 지연(밀리초). */
        double delay;
        /** The duration in milliseconds. 지속 시간(밀리초). */
        double duration;
        /** The easing. 완화. */
        Easing easing;
        /** True when <code>transitionstart</code> was queued. <code>transitionstart</code> 를 큐에 넣었으면 true. */
        boolean startFired;
        /** True for a transition of a value that cannot be interpolated (<code>transition-behavior: allow-discrete</code>). 보간할 수 없는 값의 전환(<code>transition-behavior: allow-discrete</code>)이면 true. */
        boolean discrete;
    }

    /**
     * One running or finished animation.
     * <p>
     * 실행 중이거나 끝난 애니메이션 하나.
     */
    private static final class Anim {
        /** The name. 이름. */
        String name;
        /** The keyframes rule, may be null if there is none of that name. 키프레임 규칙. 그 이름이 없으면 null. */
        KeyframesRule rule;
        /** The computed keyframes, made when first needed. 처음 필요할 때 만든 계산된 키프레임. */
        Frames frames;
        /** When it started. 시작한 시각. */
        long started;
        /** The time that passed before the last pause, in milliseconds. 마지막 정지 전까지 지난 시간(밀리초). */
        double elapsedBeforePause;
        /** The time of the last pause start, or -1 while running. 마지막 정지 시작 시각. 실행 중이면 -1. */
        long pausedAt = -1;
        /** The delay in milliseconds. 지연(밀리초). */
        double delay;
        /** The duration in milliseconds. 지속 시간(밀리초). */
        double duration;
        /** The iteration count, infinite is {@link Double#POSITIVE_INFINITY}. 반복 횟수. infinite 는 {@link Double#POSITIVE_INFINITY}. */
        double iterations = 1;
        /** The direction keyword. 방향 키워드. */
        String direction = "normal";
        /** The fill mode keyword. 채움 키워드. */
        String fill = "none";
        /** The easing of the animation (between keyframes). 애니메이션의 완화(키프레임 사이). */
        Easing easing = Easing.EASE;
        /** True after <code>animationstart</code> was queued. <code>animationstart</code> 를 큐에 넣은 뒤 true. */
        boolean startFired;
        /** The iteration that the last <code>animationiteration</code> belongs to. 마지막 <code>animationiteration</code> 의 반복 번호. */
        int lastIteration;
        /** True after the active interval ended. 활성 구간이 끝난 뒤 true. */
        boolean ended;
    }

    /**
     * The keyframes of an animation as computed values: for every property the list of the offsets where it is declared.
     * <p>
     * 애니메이션의 키프레임을 계산 값으로 나타낸 것: 속성마다 그것이 선언된 오프셋의 목록.
     */
    private static final class Frames {
        /** Offsets by property id, ascending. 속성 id 별 오프셋(오름차순). */
        final Map<Integer, List<Double>> offsets = new LinkedHashMap<Integer, List<Double>>();
        /** Values by property id, parallel to {@link #offsets}. 속성 id 별 값({@link #offsets} 와 나란히). */
        final Map<Integer, List<CssValue>> values = new LinkedHashMap<Integer, List<CssValue>>();
        /** Easings by property id: the easing of the interval that starts at the keyframe, null for the animation's. 속성 id 별 완화: 키프레임에서 시작하는 구간의 완화. 애니메이션의 것이면 null. */
        final Map<Integer, List<Easing>> easings = new LinkedHashMap<Integer, List<Easing>>();
    }

    // ------------------------------------------------------------------------------------------------ public API

    /**
     * Tells whether something is running that makes the next frame different: a transition or an animation that has not ended and is not
     * paused. The host renders again soon (about 16 ms) while this is true.
     * <p>
     * 다음 프레임을 다르게 만드는 것이 실행 중인지 알려 준다: 끝나지 않고 멈추지 않은 전환이나 애니메이션. 이것이 true 인 동안 호스트는 곧(약 16 ms) 다시 렌더링한다.
     *
     * @return true if the engine needs ticks / 틱이 필요하면 true
     */
    public boolean isActive() {
        if (disposed) {
            return false;
        }
        // 실행 중인 항목이 있는 요소를 확인한다(끝나서 채움만 유지하는 애니메이션과 멈춘 애니메이션은 시간이 흘러도 값이 같다).
        for (Element e : active) {
            State st = states.get(e);
            if ((st != null && needsTicks(st)) || pseudoNeedsTicks(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a pseudo-element of an element has a transition that runs.
     * <p>
     * 요소의 의사 요소 중 실행 중인 전환이 있는 것이 있는지 알려 준다.
     *
     * @param e the originating element / 원본 요소
     * @return true if a pseudo-element needs ticks / 의사 요소에 틱이 필요하면 true
     */
    private boolean pseudoNeedsTicks(Element e) {
        Map<String, State> m = pseudoStates.get(e);
        if (m == null) {
            return false;
        }
        // 의사 요소마다 시간에 따라 바뀌는 것이 있는지 본다.
        for (State s : m.values()) {
            if (needsTicks(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the state has something that changes with the time.
     * <p>
     * 상태에 시간에 따라 바뀌는 것이 있는지 알려 준다.
     *
     * @param st the state / 상태
     * @return true if it changes / 바뀌면 true
     */
    private static boolean needsTicks(State st) {
        if (!st.transitions.isEmpty()) {
            return true;
        }
        // 끝나지 않고 멈추지 않은 애니메이션이 있는지 본다.
        for (Anim a : st.anims) {
            if (!a.ended && a.pausedAt < 0 && a.rule != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Advances the engine to the time of the clock: ends what ran out, drops the cached styles of the elements whose animated value
     * changes (so that the next computation gives the value of this moment) and fires the queued events. Call it on the document thread
     * before a frame is rendered.
     * <p>
     * 시계의 시각까지 엔진을 진행한다: 끝난 것을 마치고, 애니메이션 값이 바뀌는 요소의 캐시한 스타일을 버리며(다음 계산이 이 순간의 값을 주도록) 큐의 이벤트를 발생시킨다.
     * 프레임을 렌더링하기 전에 문서 스레드에서 부른다.
     *
     * @return true if the style of an element changed (the page has to be laid out again) / 요소의 스타일이 바뀌었으면(페이지를 다시 배치해야 하면) true
     */
    public boolean tick() {
        if (disposed) {
            return false;
        }
        long now = clock.nowMillis();
        List<Element> changed = new ArrayList<Element>();
        List<Element> idle = new ArrayList<Element>();
        // 스크립트가 멈췄으면 스크립트 애니메이션을 놓고 그 대상을 다시 배치하게 한다.
        boolean released = webAnimations.flushStopped() || scriptStylesChanged;
        scriptStylesChanged = false;
        // 실행 중인 요소마다 시간을 진행해 이벤트를 큐에 넣고 끝난 것을 정리한다.
        for (Element e : new ArrayList<Element>(active)) {
            State st = states.get(e);
            if (st == null) {
                idle.add(e);
                continue;
            }
            boolean moved = advance(st, e, now);
            Map<String, State> ps = pseudoStates.get(e);
            // 의사 요소의 전환도 같은 시각까지 진행한다.
            if (ps != null) {
                for (State p : new ArrayList<State>(ps.values())) {
                    moved |= advance(p, e, now);
                }
            }
            if (moved) {
                changed.add(e);
            }
            if (!needsTicks(st) && !pseudoNeedsTicks(e)) {
                idle.add(e);
            }
        }
        // 더 이상 시간에 따라 바뀌지 않는 요소는 실행 목록에서 뺀다.
        for (Element e : idle) {
            active.remove(e);
        }
        // 값이 바뀐 요소의 캐시한 스타일을 버린다.
        for (Element e : changed) {
            resolver.invalidateElement(e);
        }
        flushEvents();
        return !changed.isEmpty() || released;
    }

    /**
     * Ends everything without firing an event and detaches from the style resolver (the page is replaced or disposed).
     * <p>
     * 이벤트를 발생시키지 않고 모든 것을 끝내며 스타일 리졸버에서 떨어진다(페이지가 교체되거나 폐기된다).
     */
    public void dispose() {
        disposed = true;
        webAnimations.dispose();
        active.clear();
        states.clear();
        pseudoStates.clear();
        pending.clear();
        resolver.setAnimator(null);
    }

    // ------------------------------------------------------------------------------------------------ style hook

    /**
     * Returns the style of a pseudo-element of an element at the moment (CSS Transitions Level 1: the pseudo-elements <code>::before</code>,
     * <code>::after</code>, <code>::marker</code> ... are transitioned like elements): starts the transitions that the style of the pseudo-element asks for
     * when its style changed and applies the values of the running ones. The first style of a pseudo-element starts no transition, and animations
     * (<code>@keyframes</code>) do not run on pseudo-elements.
     * <p>
     * 요소의 의사 요소의 지금 스타일을 반환한다(CSS Transitions Level 1: <code>::before</code>, <code>::after</code>, <code>::marker</code> 등 의사 요소도 요소처럼 전환한다):
     * 의사 요소의 스타일이 바뀌었을 때 그 스타일이 요구하는 전환을 시작하고 실행 중인 것의 값을 적용한다. 의사 요소의 처음 스타일은 전환을 시작하지 않으며
     * 애니메이션(<code>@keyframes</code>)은 의사 요소에서 실행하지 않는다.
     *
     * @param element  the originating element / 원본 요소
     * @param pseudo   the pseudo-element name without colons / 콜론 없는 의사 요소 이름
     * @param computed the style of the cascade / 캐스케이드의 스타일
     * @return the animated style / 애니메이션이 적용된 스타일
     */
    public ComputedStyle animatePseudo(Element element, String pseudo, ComputedStyle computed) {
        if (disposed || computed == null) {
            return computed;
        }
        Map<String, State> m = pseudoStates.get(element);
        if (m == null) {
            m = new java.util.HashMap<String, State>();
            pseudoStates.put(element, m);
        }
        State st = m.get(pseudo);
        if (st == null) {
            st = new State();
            st.pseudo = "::" + pseudo;
            m.put(pseudo, st);
        }
        ComputedStyle old = st.lastBase;
        st.lastBase = computed;
        long now = clock.nowMillis();
        if (old != null) {
            startTransitions(st, element, old, computed, now);
        }
        boolean scripted = webAnimations.targets(element);
        if (st.transitions.isEmpty() && !scripted) {
            return computed;
        }
        ComputedStyle result = computed;
        if (!st.transitions.isEmpty()) {
            active.add(element);
        }
        // 실행 중인 전환의 값을 적용한다.
        for (Transition t : st.transitions.values()) {
            CssValue v = t.prop < CUSTOM_BASE ? transitionValue(t, now) : null;
            if (v != null) {
                result = result.with(t.prop, v);
            }
        }
        // 의사 요소를 대상으로 하는 스크립트 애니메이션(Web Animations)이 맨 위에 온다.
        return scripted ? webAnimations.apply(element, "::" + pseudo, result) : result;
    }

    /**
     * Returns the style of an element at the moment: starts the transitions and animations that the new style asks for and applies the
     * values of the running ones.
     * <p>
     * 요소의 지금 스타일을 반환한다: 새 스타일이 요구하는 전환과 애니메이션을 시작하고 실행 중인 것의 값을 적용한다.
     *
     * @param element  the element / 요소
     * @param computed the style of the cascade / 캐스케이드의 스타일
     * @return the animated style / 애니메이션이 적용된 스타일
     */
    public ComputedStyle animate(Element element, ComputedStyle computed) {
        if (disposed) {
            return computed;
        }
        State st = states.get(element);
        if (st == null) {
            st = new State();
            states.put(element, st);
        }
        ComputedStyle old = st.lastBase;
        st.lastBase = computed;
        long now = clock.nowMillis();
        syncAnimations(st, element, computed, now);
        // 변경 전 스타일이 없는 요소(처음 본 요소나 렌더링되지 않던 요소)는 @starting-style 규칙이 만든 시작 스타일에서 전환을 시작한다.
        if ((old == null || old.get(Prop.DISPLAY).isKeyword("none")) && !computed.get(Prop.DISPLAY).isKeyword("none")) {
            ComputedStyle starting = resolver.getStartingStyle(element);
            if (starting != null) {
                old = starting;
            }
        }
        if (old != null) {
            startTransitions(st, element, old, computed, now);
        }
        boolean scripted = webAnimations.targets(element);
        if (st.transitions.isEmpty() && st.anims.isEmpty() && !scripted) {
            st.belowScripts = null;
            return computed;
        }
        if (!st.transitions.isEmpty() || !st.anims.isEmpty()) {
            active.add(element);
        }
        // 등록된 사용자 정의 속성의 값을 먼저 모은다(합성 순서대로 전환, 그 위에 애니메이션을 목록 순서로). 그 값을 var() 로 쓰는 속성이 새 값을 보도록 스타일을 다시 계산한다.
        Map<String, String> custom = new LinkedHashMap<String, String>();
        // 실행 중인 전환 중 사용자 정의 속성의 값을 모은다.
        for (Transition t : st.transitions.values()) {
            CssValue v = t.prop >= CUSTOM_BASE ? transitionValue(t, now) : null;
            if (v != null) {
                custom.put(customName(t.prop), v.toCssText());
            }
        }
        // CSS 애니메이션의 사용자 정의 속성 값이 전환 위에 온다.
        for (Anim a : st.anims) {
            collectCustom(custom, a, element, computed, now);
        }
        // 스크립트 애니메이션이 주는 사용자 정의 속성 값이 그 위에 온다.
        if (scripted) {
            webAnimations.collectCustom(element, computed, custom);
        }
        ComputedStyle under = custom.isEmpty() ? computed : resolver.restyleWithCustom(element, custom);
        ComputedStyle result = under;
        // 효과 스택의 합성 순서(Web Animations 5.4.2, CSS Transitions 2): CSS 전환이 가장 아래, 그 위에 CSS 애니메이션(목록 순서, 뒤의 것이 이김),
        // 맨 위에 스크립트 애니메이션이 온다.
        for (Transition t : st.transitions.values()) {
            CssValue v = t.prop < CUSTOM_BASE ? transitionValue(t, now) : null;
            if (v != null) {
                result = result.with(t.prop, v);
            }
        }
        // CSS 애니메이션의 값을 적용한다.
        for (Anim a : st.anims) {
            result = applyAnimation(result, a, element, under, now);
        }
        st.belowScripts = result;
        return scripted ? webAnimations.apply(element, null, result) : result;
    }

    /**
     * Collects the values that the keyframes of an animation give to the registered custom properties at a time (CSS Properties and Values API Level 1: the
     * computed values of registered properties of a typed syntax are animated like the properties of the table).
     * <p>
     * 어느 시각에 애니메이션의 키프레임이 등록된 사용자 정의 속성에 주는 값을 모은다(CSS Properties and Values API Level 1: 형 있는 문법으로 등록된 속성의 계산 값은 속성 표의
     * 속성처럼 애니메이션된다).
     *
     * @param out  receives the values (CSS text) by name / 이름별 값(CSS 문자열)을 받는다
     * @param a    the animation / 애니메이션
     * @param e    the element / 요소
     * @param base the style of the cascade / 캐스케이드의 스타일
     * @param time the time / 시각
     */
    private void collectCustom(Map<String, String> out, Anim a, Element e, ComputedStyle base, long time) {
        double progress = progressOf(a, time);
        if (Double.isNaN(progress)) {
            return;
        }
        if (a.frames == null) {
            a.frames = buildFrames(a, e, base);
        }
        // 키프레임에 선언된 사용자 정의 속성마다 진행도에 해당하는 값을 구한다.
        for (Integer key : a.frames.offsets.keySet()) {
            if (key.intValue() >= CUSTOM_BASE) {
                CssValue v = valueAt(a, key.intValue(), progress);
                if (v != null) {
                    out.put(customName(key.intValue()), v.toCssText());
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ transitions

    /**
     * Starts the transitions for the properties whose value changed between two styles.
     * <p>
     * 두 스타일 사이에서 값이 바뀐 속성의 전환을 시작한다.
     *
     * @param st   the state / 상태
     * @param e    the element / 요소
     * @param old  the previous style / 이전 스타일
     * @param now  the new style / 새 스타일
     * @param time the time / 시각
     */
    private void startTransitions(State st, Element e, ComputedStyle old, ComputedStyle now, long time) {
        List<CssValue> names = items(now.get(Prop.TRANSITION_PROPERTY));
        List<CssValue> durations = items(now.get(Prop.TRANSITION_DURATION));
        List<CssValue> delays = items(now.get(Prop.TRANSITION_DELAY));
        List<CssValue> timings = items(now.get(Prop.TRANSITION_TIMING_FUNCTION));
        PropertyRegistry reg = PropertyRegistry.getInstance();
        List<String> registered = resolver.registeredPropertyNames();
        // 모든 개별 속성과 등록된 사용자 정의 속성에 대해 값이 바뀌었는지 확인한다.
        List<CssValue> behaviors = items(now.get(Prop.TRANSITION_BEHAVIOR));
        for (int k = 1; k <= Prop.COUNT + registered.size(); k++) {
            int id = k <= Prop.COUNT ? k : customId(registered.get(k - Prop.COUNT - 1));
            int index = matchIndex(names, id, reg);
            // transition-behavior: allow-discrete 이면 보간할 수 없는 속성(display, position, float 같은 이산 값)도 전환한다(CSS Transitions 2).
            boolean allowDiscrete = index >= 0 && at(behaviors, index) != null && at(behaviors, index).isKeyword("allow-discrete");
            if (!transitionable(id) && !(allowDiscrete && discreteTransitionable(id))) {
                continue;
            }
            CssValue a = valueIn(old, id);
            CssValue b = valueIn(now, id);
            if (a == null || b == null) {
                continue;
            }
            double dur = index < 0 ? 0 : seconds(at(durations, index)) * 1000;
            double del = index < 0 ? 0 : seconds(at(delays, index)) * 1000;
            Transition running = st.transitions.get(Integer.valueOf(id));
            // 전환이 없는 속성(또는 지속 시간과 지연이 모두 0)이면 실행 중인 전환을 취소한다.
            if (index < 0 || dur + del <= 0) {
                if (running != null && !running.to.equals(b)) {
                    cancelTransition(st, running, e, time);
                }
                continue;
            }
            if (a.equals(b)) {
                continue;
            }
            if (running != null && running.to.equals(b)) {
                continue;
            }
            CssValue from = running != null ? transitionValue(running, time) : a;
            if (from == null) {
                from = a;
            }
            boolean discrete = Interpolator.interpolate(id, from, b, 0.5) == null;
            // 보간할 수 없는 값은 allow-discrete 일 때만 이산 전환으로 바뀐다.
            if (discrete && !allowDiscrete) {
                if (running != null) {
                    cancelTransition(st, running, e, time);
                }
                continue;
            }
            if (running != null) {
                cancelTransition(st, running, e, time);
            }
            Transition t = new Transition();
            t.discrete = discrete;
            t.prop = id;
            t.from = from;
            t.to = b;
            t.created = time;
            t.delay = del;
            t.duration = Math.max(0, dur);
            t.easing = Easing.of(at(timings, index));
            st.transitions.put(Integer.valueOf(id), t);
            queueTransition(st, "transitionrun", e, propertyName(id), 0);
        }
    }

    /**
     * Cancels a running transition: it disappears and <code>transitioncancel</code> is queued.
     * <p>
     * 실행 중인 전환을 취소한다: 사라지고 <code>transitioncancel</code> 가 큐에 들어간다.
     *
     * @param st the state / 상태
     * @param t  the transition / 전환
     * @param e  the element / 요소
     * @param time the time / 시각
     */
    private void cancelTransition(State st, Transition t, Element e, long time) {
        st.transitions.remove(Integer.valueOf(t.prop));
        double elapsed = Math.max(0, time - t.created - t.delay);
        queueTransition(st, "transitioncancel", e, propertyName(t.prop), Math.min(elapsed, t.duration) / 1000.0);
    }

    /**
     * Returns the value of a transition at a time: the start value before the delay ended, the end value afterwards.
     * <p>
     * 시각에 대한 전환의 값을 반환한다: 지연이 끝나기 전에는 시작 값, 끝난 뒤에는 끝 값.
     *
     * @param t    the transition / 전환
     * @param time the time / 시각
     * @return the value / 값
     */
    private static CssValue transitionValue(Transition t, long time) {
        double elapsed = time - t.created - t.delay;
        if (elapsed <= 0) {
            return t.from;
        }
        if (elapsed >= t.duration) {
            return t.to;
        }
        double progress = t.easing.at(elapsed / t.duration);
        // 이산 전환: display 와 content-visibility 는 none 이 아닌 쪽 값이 전환 내내 보이게 하고(퇴장 애니메이션이 끝나기 전에 사라지지 않게), 그 밖의 값은 절반에서 바뀐다.
        if (t.discrete) {
            if (t.prop == Prop.DISPLAY || t.prop == Prop.CONTENT_VISIBILITY) {
                boolean fromNone = t.from.isKeyword("none") || t.from.isKeyword("hidden");
                boolean toNone = t.to.isKeyword("none") || t.to.isKeyword("hidden");
                if (fromNone && !toNone) {
                    return t.to;
                }
                if (toNone && !fromNone) {
                    return t.from;
                }
            }
            return progress < 0.5 ? t.from : t.to;
        }
        CssValue v = Interpolator.interpolate(t.prop, t.from, t.to, progress);
        return v == null ? t.to : v;
    }

    /**
     * Tells whether a property whose values cannot be interpolated may transition with <code>transition-behavior: allow-discrete</code>: every property
     * except the properties of transitions and animations themselves.
     * <p>
     * 값을 보간할 수 없는 속성이 <code>transition-behavior: allow-discrete</code> 로 전환할 수 있는지 알려 준다: 전환과 애니메이션 자체의 속성을 뺀 모든 속성.
     *
     * @param id the property id / 속성 id
     * @return true if it may transition discretely / 이산 전환할 수 있으면 true
     */
    private static boolean discreteTransitionable(int id) {
        return !(id >= Prop.TRANSITION_PROPERTY && id <= Prop.ANIMATION_PLAY_STATE) && id != Prop.TRANSITION_BEHAVIOR;
    }

    /**
     * Tells whether a property can be transitioned at all (everything that is not an animation or transition property itself and that
     * is not a keyword-only layout switch).
     * <p>
     * 속성이 전환될 수 있는지 알려 준다(전환과 애니메이션 속성 자체가 아니고 키워드뿐인 배치 전환이 아닌 것).
     *
     * @param id the property id / 속성 id
     * @return true if it may transition / 전환할 수 있으면 true
     */
    private static boolean transitionable(int id) {
        return !(id >= Prop.TRANSITION_PROPERTY && id <= Prop.ANIMATION_PLAY_STATE) && id != Prop.TRANSITION_BEHAVIOR && id != Prop.DISPLAY && id != Prop.POSITION && id != Prop.FLOAT;
    }

    /**
     * Finds the entry of <code>transition-property</code> that applies to a property: the last entry that is <code>all</code> or names
     * the property or a shorthand of it; <code>none</code> stops the search with no transition.
     * <p>
     * 속성에 적용되는 <code>transition-property</code> 항목을 찾는다: <code>all</code> 이거나 그 속성이나 그 단축 속성의 이름인 마지막 항목. <code>none</code> 이면
     * 전환 없음으로 찾기를 멈춘다.
     *
     * @param names the list / 목록
     * @param id    the property id / 속성 id
     * @param reg   the property registry / 속성 레지스트리
     * @return the index or -1 / 위치 또는 -1
     */
    private int matchIndex(List<CssValue> names, int id, PropertyRegistry reg) {
        int found = -1;
        String customName = id >= CUSTOM_BASE ? customName(id) : null;
        // 목록의 항목마다 속성과 맞는지 보고 마지막으로 맞는 것을 고른다.
        for (int i = 0; i < names.size(); i++) {
            CssValue n = names.get(i);
            if (!(n instanceof CssKeyword)) {
                continue;
            }
            String name = ((CssKeyword) n).getName();
            if ("none".equals(name)) {
                return -1;
            }
            if ("all".equals(name)) {
                found = i;
                continue;
            }
            // 사용자 정의 속성은 이름이 같은 항목에만 맞는다(all 은 위에서 처리했다).
            if (customName != null) {
                if (name.equals(customName)) {
                    found = i;
                }
                continue;
            }
            int[] ids = reg.longhandsOf(name);
            // 이름이 가리키는 개별 속성에 이 속성이 있는지 확인한다.
            for (int k = 0; ids != null && k < ids.length; k++) {
                if (ids[k] == id) {
                    found = i;
                }
            }
        }
        return found;
    }

    // ------------------------------------------------------------------------------------------------ animations

    /**
     * Starts the animations that the style lists, updates the parameters of the running ones and cancels those that are no longer listed.
     * <p>
     * 스타일이 나열한 애니메이션을 시작하고, 실행 중인 것의 매개변수를 갱신하고, 더 이상 나열되지 않은 것을 취소한다.
     *
     * @param st   the state / 상태
     * @param e    the element / 요소
     * @param s    the style of the cascade / 캐스케이드의 스타일
     * @param time the time / 시각
     */
    private void syncAnimations(State st, Element e, ComputedStyle s, long time) {
        List<CssValue> names = items(s.get(Prop.ANIMATION_NAME));
        List<Anim> next = new ArrayList<Anim>();
        // 나열된 이름마다 실행 중인 애니메이션을 찾거나 새로 시작한다.
        for (int i = 0; i < names.size(); i++) {
            String name = nameOf(names.get(i));
            if (name == null || "none".equals(name)) {
                continue;
            }
            Anim a = find(st.anims, name);
            if (a == null) {
                a = new Anim();
                a.name = name;
                a.rule = findKeyframes(name);
                a.started = time;
                if (a.rule == null) {
                    continue;
                }
            }
            a.duration = Math.max(0, seconds(at(items(s.get(Prop.ANIMATION_DURATION)), i)) * 1000);
            a.delay = seconds(at(items(s.get(Prop.ANIMATION_DELAY)), i)) * 1000;
            a.easing = Easing.of(at(items(s.get(Prop.ANIMATION_TIMING_FUNCTION)), i));
            a.iterations = iterations(at(items(s.get(Prop.ANIMATION_ITERATION_COUNT)), i));
            a.direction = keyword(at(items(s.get(Prop.ANIMATION_DIRECTION)), i), "normal");
            a.fill = keyword(at(items(s.get(Prop.ANIMATION_FILL_MODE)), i), "none");
            boolean paused = "paused".equals(keyword(at(items(s.get(Prop.ANIMATION_PLAY_STATE)), i), "running"));
            if (paused && a.pausedAt < 0) {
                a.elapsedBeforePause += time - a.started;
                a.pausedAt = time;
            } else if (!paused && a.pausedAt >= 0) {
                a.started = time;
                a.pausedAt = -1;
            }
            next.add(a);
        }
        // 더 이상 나열되지 않은 애니메이션은 시작했고 끝나지 않았다면 animationcancel 과 함께 사라진다.
        for (Anim a : st.anims) {
            if (!next.contains(a) && a.startFired && !a.ended) {
                queue("animationcancel", e, null, a.name, activeElapsed(a, time) / 1000.0);
            }
        }
        st.anims.clear();
        st.anims.addAll(next);
    }

    /**
     * Finds the animation of a name in a list.
     * <p>
     * 목록에서 이름이 같은 애니메이션을 찾는다.
     *
     * @param list the animations / 애니메이션
     * @param name the name / 이름
     * @return the animation or null / 애니메이션 또는 null
     */
    private static Anim find(List<Anim> list, String name) {
        // 목록에서 이름이 같은 것을 찾는다.
        for (Anim a : list) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Finds the <code>@keyframes</code> rule of a name: the last rule of that name in the style sheets of the document.
     * <p>
     * 이름의 <code>@keyframes</code> 규칙을 찾는다: 문서의 스타일 시트에서 그 이름의 마지막 규칙.
     *
     * @param name the name / 이름
     * @return the rule or null / 규칙 또는 null
     */
    private KeyframesRule findKeyframes(String name) {
        KeyframesRule found = null;
        // 스타일 시트의 규칙을 차례로 훑어 이름이 같은 마지막 규칙을 고른다.
        for (StyleSheet sheet : css.getStyleSheets()) {
            found = scan(sheet.getRules(), name, found);
        }
        return found;
    }

    /**
     * Scans a list of rules (and the groups in it) for a keyframes rule of a name.
     * <p>
     * 규칙 목록(과 그 안의 그룹)에서 이름의 키프레임 규칙을 찾는다.
     *
     * @param rules the rules / 규칙
     * @param name  the name / 이름
     * @param found the rule found so far / 지금까지 찾은 규칙
     * @return the last matching rule / 맞는 마지막 규칙
     */
    private static KeyframesRule scan(List<CssRule> rules, String name, KeyframesRule found) {
        KeyframesRule result = found;
        // 규칙마다 키프레임이면 이름을 비교하고 그룹이면 안을 훑는다.
        for (CssRule r : rules) {
            if (r instanceof KeyframesRule && name.equals(((KeyframesRule) r).getName())) {
                result = (KeyframesRule) r;
            } else if (r instanceof GroupRule) {
                result = scan(((GroupRule) r).getRules(), name, result);
            }
        }
        return result;
    }

    /**
     * Returns the time into the active interval of an animation in milliseconds (negative during the delay).
     * <p>
     * 애니메이션의 활성 구간 안에서의 시간을 밀리초로 반환한다(지연 동안은 음수).
     *
     * @param a    the animation / 애니메이션
     * @param time the time / 시각
     * @return milliseconds / 밀리초
     */
    private static double activeElapsed(Anim a, long time) {
        double run = a.elapsedBeforePause + (a.pausedAt >= 0 ? 0 : time - a.started);
        return run - a.delay;
    }

    /**
     * Applies the value of an animation at a time on a style.
     * <p>
     * 어느 시각의 애니메이션 값을 스타일에 적용한다.
     *
     * @param s    the style so far / 지금까지의 스타일
     * @param a    the animation / 애니메이션
     * @param e    the element / 요소
     * @param base the style of the cascade (the underlying values) / 캐스케이드의 스타일(기저 값)
     * @param time the time / 시각
     * @return the style / 스타일
     */
    private ComputedStyle applyAnimation(ComputedStyle s, Anim a, Element e, ComputedStyle base, long time) {
        double progress = progressOf(a, time);
        if (Double.isNaN(progress)) {
            return s;
        }
        if (a.frames == null) {
            a.frames = buildFrames(a, e, base);
        }
        ComputedStyle result = s;
        // 키프레임에 선언된 속성마다 진행도에 해당하는 값을 구해 적용한다(등록된 사용자 정의 속성은 collectCustom 이 이미 반영했다).
        for (Map.Entry<Integer, List<Double>> pe : a.frames.offsets.entrySet()) {
            int id = pe.getKey().intValue();
            if (id >= CUSTOM_BASE) {
                continue;
            }
            CssValue v = valueAt(a, id, progress);
            if (v != null) {
                result = result.with(id, v);
            }
        }
        return result;
    }

    /**
     * Returns the progress along the keyframes of an animation at a time: the position in the active duration with the iteration, the direction and the fill mode
     * applied; NaN if the animation has no effect then (before its start or after its end without the fill that would keep the value).
     * <p>
     * 어느 시각에 애니메이션의 키프레임을 따른 진행도를 반환한다: 활성 지속 시간 안의 위치에 반복, 방향, 채움을 적용한 것. 그 시각에 효과가 없으면(시작 전이거나, 값을 유지하는
     * 채움이 없는 끝난 뒤) NaN.
     *
     * @param a    the animation / 애니메이션
     * @param time the time / 시각
     * @return the progress, or NaN / 진행도. 없으면 NaN
     */
    private static double progressOf(Anim a, long time) {
        double active = activeElapsed(a, time);
        double total = a.duration * a.iterations;
        boolean before = active < 0;
        boolean after = !before && (a.duration <= 0 || active >= total);
        if (before) {
            if (!"backwards".equals(a.fill) && !"both".equals(a.fill)) {
                return Double.NaN;
            }
            return directed(a, 0, 0);
        }
        if (after) {
            if (!"forwards".equals(a.fill) && !"both".equals(a.fill)) {
                return Double.NaN;
            }
            double frac = a.iterations - Math.floor(a.iterations);
            int idx = frac == 0 ? Math.max(0, (int) a.iterations - 1) : (int) Math.floor(a.iterations);
            return directed(a, idx, frac == 0 ? 1 : frac);
        }
        double position = active / a.duration;
        int idx = (int) Math.floor(position);
        return directed(a, idx, position - idx);
    }

    /**
     * Applies the direction to the progress of one iteration.
     * <p>
     * 반복 하나의 진행도에 방향을 적용한다.
     *
     * @param a        the animation / 애니메이션
     * @param iteration the iteration number from 0 / 0 부터의 반복 번호
     * @param p         the progress inside the iteration / 반복 안의 진행도
     * @return the progress along the keyframes / 키프레임을 따른 진행도
     */
    private static double directed(Anim a, int iteration, double p) {
        boolean odd = (iteration & 1) == 1;
        boolean reverse = "reverse".equals(a.direction) || ("alternate".equals(a.direction) && odd)
                || ("alternate-reverse".equals(a.direction) && !odd);
        return reverse ? 1 - p : p;
    }

    /**
     * Returns the value of a property at a progress along the keyframes.
     * <p>
     * 키프레임을 따른 진행도에서 속성의 값을 반환한다.
     *
     * @param a  the animation / 애니메이션
     * @param id the property id / 속성 id
     * @param p  the progress / 진행도
     * @return the value / 값
     */
    private static CssValue valueAt(Anim a, int id, double p) {
        Integer key = Integer.valueOf(id);
        List<Double> off = a.frames.offsets.get(key);
        List<CssValue> val = a.frames.values.get(key);
        List<Easing> eas = a.frames.easings.get(key);
        // 진행도가 놓인 구간의 양 끝 키프레임을 찾는다.
        for (int i = 0; i + 1 < off.size(); i++) {
            double lo = off.get(i).doubleValue();
            double hi = off.get(i + 1).doubleValue();
            if (p <= hi || i + 2 == off.size()) {
                double local = hi > lo ? (p - lo) / (hi - lo) : 1;
                Easing e = eas.get(i) != null ? eas.get(i) : a.easing;
                double eased = e.at(Math.max(0, Math.min(1, local)));
                CssValue v = Interpolator.interpolate(id, val.get(i), val.get(i + 1), eased);
                if (v == null) {
                    // 보간할 수 없는 값(이산)은 구간의 중간에서 바뀐다.
                    return local < 0.5 ? val.get(i) : val.get(i + 1);
                }
                return v;
            }
        }
        return val.get(val.size() - 1);
    }

    /**
     * Makes the computed keyframes of an animation: the declared values are computed against the element, the offsets of the keyframes
     * with several selectors are expanded, a property that has no value at 0 or at 1 gets the underlying value there.
     * <p>
     * 애니메이션의 계산된 키프레임을 만든다: 선언한 값을 요소를 기준으로 계산하고, 선택자가 여럿인 키프레임의 오프셋을 펼치며, 0 이나 1 에 값이 없는 속성은 거기서
     * 기저 값을 가진다.
     *
     * @param a    the animation / 애니메이션
     * @param e    the element / 요소
     * @param base the style of the cascade / 캐스케이드의 스타일
     * @return the keyframes / 키프레임
     */
    private Frames buildFrames(Anim a, Element e, ComputedStyle base) {
        Frames f = new Frames();
        // 선언한 키프레임 항목을 (오프셋, 선언) 쌍으로 펼친다.
        List<Object[]> list = new ArrayList<Object[]>();
        // 키프레임의 선택자(오프셋)마다 한 항목을 만든다.
        for (KeyframesRule.Keyframe k : a.rule.getKeyframes()) {
            // 선택자가 여럿이면 같은 선언을 오프셋마다 되풀이한다.
            for (Double off : k.getOffsets()) {
                list.add(new Object[] { off, k });
            }
        }
        // 오프셋 오름차순(같으면 선언 순서)으로 정렬한다.
        for (int i = 1; i < list.size(); i++) {
            Object[] x = list.get(i);
            int j = i - 1;
            // 삽입 정렬: 더 큰 오프셋을 한 칸씩 뒤로 민다(같은 오프셋은 선언 순서를 유지한다).
            while (j >= 0 && ((Double) list.get(j)[0]).doubleValue() > ((Double) x[0]).doubleValue()) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, x);
        }
        // 키프레임마다 선언을 계산해 속성별 목록에 더한다.
        for (Object[] item : list) {
            double off = ((Double) item[0]).doubleValue();
            KeyframesRule.Keyframe k = (KeyframesRule.Keyframe) item[1];
            Map<Integer, CssValue> declared = new LinkedHashMap<Integer, CssValue>();
            // 애니메이션하지 않는 속성(전환과 애니메이션 속성)은 뺀다.
            for (Map.Entry<Integer, CssValue> d : k.getStyle().normal().entrySet()) {
                int id = d.getKey().intValue();
                if (!(id >= Prop.TRANSITION_PROPERTY && id <= Prop.ANIMATION_PLAY_STATE)) {
                    declared.put(d.getKey(), d.getValue());
                }
            }
            CssValue timing = k.getStyle().normal().get(Integer.valueOf(Prop.ANIMATION_TIMING_FUNCTION));
            ComputedStyle computed = declared.isEmpty() ? null : resolver.computeDeclared(e, declared, base);
            // 키프레임이 선언한 등록된 사용자 정의 속성은 등록한 문법에 맞춰 계산한다(문법에 맞지 않는 선언은 건너뛴다).
            Map<Integer, CssValue> customDeclared = new LinkedHashMap<Integer, CssValue>();
            for (Map.Entry<String, List<ComponentValue>> cd : k.getStyle().customNormal().entrySet()) {
                CssValue typed = resolver.computeRegisteredValue(cd.getKey(), cd.getValue(), base);
                if (typed != null) {
                    customDeclared.put(Integer.valueOf(customId(cd.getKey())), typed);
                }
            }
            List<Integer> keys = new ArrayList<Integer>(declared.keySet());
            keys.addAll(customDeclared.keySet());
            // 선언한 속성마다 값을 목록에 더한다(같은 오프셋의 앞선 값은 뒤의 값으로 바꾼다).
            for (Integer id : keys) {
                List<Double> offs = f.offsets.get(id);
                if (offs == null) {
                    offs = new ArrayList<Double>();
                    f.offsets.put(id, offs);
                    f.values.put(id, new ArrayList<CssValue>());
                    f.easings.put(id, new ArrayList<Easing>());
                }
                CssValue v = id.intValue() >= CUSTOM_BASE ? customDeclared.get(id) : computed.get(id.intValue());
                Easing en = timing == null ? null : Easing.of(timing);
                int last = offs.size() - 1;
                if (last >= 0 && offs.get(last).doubleValue() == off) {
                    f.values.get(id).set(last, v);
                    f.easings.get(id).set(last, en);
                } else {
                    offs.add(Double.valueOf(off));
                    f.values.get(id).add(v);
                    f.easings.get(id).add(en);
                }
            }
        }
        // 0 이나 1 에 값이 없는 속성은 거기서 기저 값을 갖는다.
        for (Integer id : f.offsets.keySet()) {
            List<Double> offs = f.offsets.get(id);
            if (offs.get(0).doubleValue() > 0) {
                offs.add(0, Double.valueOf(0));
                f.values.get(id).add(0, valueIn(base, id.intValue()));
                f.easings.get(id).add(0, null);
            }
            if (offs.get(offs.size() - 1).doubleValue() < 1) {
                offs.add(Double.valueOf(1));
                f.values.get(id).add(valueIn(base, id.intValue()));
                f.easings.get(id).add(null);
            }
            // 구간이 하나도 없으면(오프셋이 하나뿐) 같은 값을 한 번 더 두어 구간을 만든다.
            if (offs.size() == 1) {
                offs.add(Double.valueOf(1));
                f.values.get(id).add(f.values.get(id).get(0));
                f.easings.get(id).add(null);
            }
        }
        return f;
    }

    // ------------------------------------------------------------------------------------------------ time

    /**
     * Advances the state of an element to a time: queues the events and removes what ended.
     * <p>
     * 요소의 상태를 시각까지 진행한다: 이벤트를 큐에 넣고 끝난 것을 제거한다.
     *
     * @param st   the state / 상태
     * @param e    the element / 요소
     * @param time the time / 시각
     * @return true if the animated value of the element can differ from the last style / 요소의 애니메이션 값이 마지막 스타일과 다를 수 있으면 true
     */
    private boolean advance(State st, Element e, long time) {
        boolean moved = false;
        List<Transition> finished = new ArrayList<Transition>();
        // 전환마다 시작과 끝 이벤트를 큐에 넣는다.
        for (Transition t : st.transitions.values()) {
            double elapsed = time - t.created - t.delay;
            moved = true;
            if (elapsed >= 0 && !t.startFired) {
                t.startFired = true;
                queueTransition(st, "transitionstart", e, propertyName(t.prop), 0);
            }
            if (elapsed >= t.duration) {
                finished.add(t);
            }
        }
        // 끝난 전환을 제거하고 transitionend 를 큐에 넣는다.
        for (Transition t : finished) {
            if (!t.startFired) {
                t.startFired = true;
                queueTransition(st, "transitionstart", e, propertyName(t.prop), 0);
            }
            st.transitions.remove(Integer.valueOf(t.prop));
            queueTransition(st, "transitionend", e, propertyName(t.prop), t.duration / 1000.0);
        }
        // 애니메이션마다 시작, 반복, 끝 이벤트를 큐에 넣는다.
        for (Anim a : st.anims) {
            if (a.rule == null || a.pausedAt >= 0) {
                continue;
            }
            double active = activeElapsed(a, time);
            if (!a.ended) {
                moved = true;
            }
            if (active >= 0 && !a.startFired) {
                a.startFired = true;
                queue("animationstart", e, null, a.name, 0);
            }
            if (active >= 0 && a.duration > 0 && !a.ended) {
                int idx = (int) Math.floor(active / a.duration);
                // 마지막 반복 전에 반복 경계를 넘을 때마다 animationiteration 을 큐에 넣는다.
                while (a.lastIteration < idx && a.lastIteration + 1 < a.iterations) {
                    a.lastIteration++;
                    queue("animationiteration", e, null, a.name, a.lastIteration * a.duration / 1000.0);
                }
            }
            if (!a.ended && active >= 0 && (a.duration <= 0 || active >= a.duration * a.iterations)) {
                if (!a.startFired) {
                    a.startFired = true;
                    queue("animationstart", e, null, a.name, 0);
                }
                a.ended = true;
                queue("animationend", e, null, a.name, a.duration * a.iterations / 1000.0);
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------------------------------------ events

    /**
     * Queues an event of an element (it is fired by the next {@link #tick()}, never inside a style computation).
     * <p>
     * 요소의 이벤트를 큐에 넣는다(스타일 계산 안에서가 아니라 다음 {@link #tick()} 이 발생시킨다).
     *
     * @param type    the event type / 이벤트 종류
     * @param e       the target element / 대상 요소
     * @param prop    the property of a transition or null / 전환의 속성 또는 null
     * @param anim    the animation name or null / 애니메이션 이름 또는 null
     * @param elapsed elapsed seconds / 경과 시간(초)
     */
    private void queue(String type, Element e, String prop, String anim, double elapsed) {
        pending.add(new Object[] { type, e, prop, anim, Double.valueOf(elapsed), null });
    }

    /**
     * Queues a transition event of an element or of one of its pseudo-elements (the state knows which): the event is fired at the originating element and
     * names the pseudo-element in {@link CssTransitionEvent#getPseudoElement()}.
     * <p>
     * 요소나 그 의사 요소의 전환 이벤트를 큐에 넣는다(상태가 어느 쪽인지 안다): 이벤트는 원본 요소에 발생하며 {@link CssTransitionEvent#getPseudoElement()} 에 의사 요소의 이름이 든다.
     *
     * @param st      the state of the element or the pseudo-element / 요소나 의사 요소의 상태
     * @param type    the event type / 이벤트 종류
     * @param e       the originating element / 원본 요소
     * @param prop    the property name / 속성 이름
     * @param elapsed the elapsed seconds / 경과 시간(초)
     */
    private void queueTransition(State st, String type, Element e, String prop, double elapsed) {
        pending.add(new Object[] { type, e, prop, null, Double.valueOf(elapsed), st.pseudo });
    }

    /**
     * Fires the queued events in order. A listener that starts another animation only queues events for the next tick.
     * <p>
     * 큐의 이벤트를 순서대로 발생시킨다. 리스너가 다른 애니메이션을 시작해도 이벤트는 다음 틱을 위해 큐에만 들어간다.
     */
    private void flushEvents() {
        List<Object[]> batch = new ArrayList<Object[]>(pending);
        pending.clear();
        // 큐의 이벤트를 일어난 순서대로 요소에 발생시킨다.
        for (Object[] ev : batch) {
            String type = (String) ev[0];
            double elapsed = ((Double) ev[4]).doubleValue();
            // 전환 이벤트와 애니메이션 이벤트는 스크립트 인터페이스가 다르므로 타입으로 나누어 만든다.
            ((Element) ev[1]).dispatchEvent(type.startsWith("transition") ? new CssTransitionEvent(type, (String) ev[2], elapsed, (String) ev[5])
                    : new CssKeyframesEvent(type, (String) ev[3], elapsed));
        }
    }

    // ------------------------------------------------------------------------------------------------ value helpers

    /**
     * Returns the name of a property for the events.
     * <p>
     * 이벤트용 속성 이름을 반환한다.
     *
     * @param id the property id / 속성 id
     * @return the name / 이름
     */
    private String propertyName(int id) {
        // 사용자 정의 속성의 이벤트는 --이름 으로 알린다.
        if (id >= CUSTOM_BASE) {
            return customName(id);
        }
        Property p = PropertyRegistry.getInstance().byId(id);
        return p == null ? "" : p.getName();
    }

    /**
     * Returns the items of a comma list (a single value is a list of one).
     * <p>
     * 쉼표 목록의 항목을 반환한다(값 하나는 항목이 하나인 목록).
     *
     * @param v the value / 값
     * @return the items, empty for null / 항목. null 이면 빈 목록
     */
    private static List<CssValue> items(CssValue v) {
        List<CssValue> out = new ArrayList<CssValue>();
        if (v instanceof CssList && ((CssList) v).isComma()) {
            out.addAll(((CssList) v).getItems());
        } else if (v != null) {
            out.add(v);
        }
        return out;
    }

    /**
     * Returns the item of a list repeated to any length.
     * <p>
     * 어떤 길이로든 되풀이되는 목록의 항목을 반환한다.
     *
     * @param list  the items / 항목
     * @param index the position / 위치
     * @return the item, null for an empty list / 항목. 빈 목록이면 null
     */
    private static CssValue at(List<CssValue> list, int index) {
        return list.isEmpty() ? null : list.get(index % list.size());
    }

    /**
     * Reads a time value in seconds.
     * <p>
     * 시간 값을 초로 읽는다.
     *
     * @param v the value / 값
     * @return seconds, 0 if it cannot be read / 초. 읽을 수 없으면 0
     */
    private static double seconds(CssValue v) {
        if (v instanceof CssDimension) {
            return ((CssDimension) v).canonical();
        }
        return v instanceof CssNumber ? ((CssNumber) v).getValue() : 0;
    }

    /**
     * Reads a keyword with a default.
     * <p>
     * 기본값이 있는 키워드를 읽는다.
     *
     * @param v   the value / 값
     * @param def the default / 기본값
     * @return the keyword / 키워드
     */
    private static String keyword(CssValue v, String def) {
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : def;
    }

    /**
     * Reads an iteration count.
     * <p>
     * 반복 횟수를 읽는다.
     *
     * @param v the value / 값
     * @return the count, infinity for <code>infinite</code> / 횟수. <code>infinite</code> 는 무한대
     */
    private static double iterations(CssValue v) {
        if (v instanceof CssKeyword) {
            return "infinite".equals(((CssKeyword) v).getName()) ? Double.POSITIVE_INFINITY : 1;
        }
        return v instanceof CssNumber ? Math.max(0, ((CssNumber) v).getValue()) : 1;
    }

    /**
     * Reads the name of an animation (an identifier or a string).
     * <p>
     * 애니메이션 이름(식별자나 문자열)을 읽는다.
     *
     * @param v the value / 값
     * @return the name or null / 이름 또는 null
     */
    private static String nameOf(CssValue v) {
        if (v instanceof CssKeyword) {
            return ((CssKeyword) v).getName();
        }
        return v instanceof CssString ? ((CssString) v).getValue() : null;
    }
}
