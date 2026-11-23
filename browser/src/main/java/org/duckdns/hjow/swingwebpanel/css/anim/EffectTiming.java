package org.duckdns.hjow.swingwebpanel.css.anim;

import org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The timing properties of an animation effect and the timing calculations of Web Animations Level 1 (W3C Working Draft 5 June 2023,
 * https://www.w3.org/TR/2023/WD-web-animations-1-20230605/, sections 4.5 to 4.11 and 6.5): the start delay, the end delay, the fill mode, the
 * iteration start, the iteration count, the iteration duration (<code>auto</code> is zero in this level), the playback direction and the timing
 * function. {@link #compute(double, boolean, boolean)} turns a local time into the phase, the active time, the overall progress, the simple
 * iteration progress, the current iteration and the transformed progress with the endpoint-exclusive rules of the specification. An unresolved
 * time is <code>NaN</code>. Use: create one, change the members through {@link #setDelay(double)} and the other setters (which do not check:
 * {@link #validate} checks the values that a script passes first), then call {@link #compute}. Not thread-safe; it belongs to the document thread.
 * <p>
 * 애니메이션 효과의 시간 속성과 Web Animations Level 1(W3C Working Draft 2023-06-05, https://www.w3.org/TR/2023/WD-web-animations-1-20230605/,
 * 4.5~4.11 절과 6.5 절)의 시간 계산: 시작 지연, 끝 지연, 채움 모드, 반복 시작, 반복 횟수, 반복 지속 시간(<code>auto</code> 는 이 수준에서 0), 재생 방향,
 * 완화 함수. {@link #compute(double, boolean, boolean)} 은 지역 시간을 규격의 끝점 제외 규칙에 따라 단계, 활성 시간, 전체 진행도, 단순 반복 진행도, 현재 반복,
 * 변환된 진행도로 바꾼다. 결정되지 않은 시간은 <code>NaN</code> 이다. 사용법: 만들고 {@link #setDelay(double)} 등의 설정자로 값을 바꾼 뒤(설정자는 검사하지
 * 않는다. 스크립트가 넘긴 값은 {@link #validate} 로 먼저 검사한다) {@link #compute} 를 부른다. 스레드 안전하지 않으며 문서 스레드에 속한다.
 */
public final class EffectTiming {

    /** The phase of an effect without a local time (the idle phase). 지역 시간이 없는 효과의 단계(유휴 단계). */
    public static final int PHASE_IDLE = 0;
    /** The before phase. before 단계. */
    public static final int PHASE_BEFORE = 1;
    /** The active phase. active 단계. */
    public static final int PHASE_ACTIVE = 2;
    /** The after phase. after 단계. */
    public static final int PHASE_AFTER = 3;

    /** The start delay in milliseconds. 시작 지연(밀리초). */
    private double delay;
    /** The end delay in milliseconds. 끝 지연(밀리초). */
    private double endDelay;
    /** The fill mode as specified: none, forwards, backwards, both or auto. 지정한 채움 모드: none, forwards, backwards, both, auto. */
    private String fill = "auto";
    /** The iteration start. 반복 시작. */
    private double iterationStart;
    /** The iteration count, positive infinity for endless. 반복 횟수. 끝없으면 양의 무한대. */
    private double iterations = 1;
    /** The iteration duration in milliseconds, NaN for <code>auto</code>. 반복 지속 시간(밀리초). <code>auto</code> 이면 NaN. */
    private double duration = Double.NaN;
    /** The playback direction. 재생 방향. */
    private String direction = "normal";
    /** The timing function as serialized text. 직렬화한 완화 함수 문자열. */
    private String easingText = "linear";
    /** The timing function. 완화 함수. */
    private Easing easing = Easing.LINEAR;

    /**
     * Creates the default timing: no delays, fill <code>auto</code>, one iteration, duration <code>auto</code>, direction <code>normal</code>,
     * easing <code>linear</code>.
     * <p>
     * 기본 시간을 만든다: 지연 없음, 채움 <code>auto</code>, 반복 1 회, 지속 시간 <code>auto</code>, 방향 <code>normal</code>, 완화 <code>linear</code>.
     */
    public EffectTiming() {
    }

    /**
     * Returns a copy (the <code>KeyframeEffect(source)</code> constructor copies the specified timing).
     * <p>
     * 복사본을 반환한다(<code>KeyframeEffect(source)</code> 생성자가 지정한 시간을 복사한다).
     *
     * @return the copy / 복사본
     */
    public EffectTiming copy() {
        EffectTiming t = new EffectTiming();
        t.delay = delay;
        t.endDelay = endDelay;
        t.fill = fill;
        t.iterationStart = iterationStart;
        t.iterations = iterations;
        t.duration = duration;
        t.direction = direction;
        t.easingText = easingText;
        t.easing = easing;
        return t;
    }

    /**
     * Parses a timing function (the <code>easing</code> member of the timing dictionaries and of the keyframes): the text must be one
     * <code>&lt;easing-function&gt;</code> of CSS Easing Level 1 (a keyword, <code>cubic-bezier()</code>, <code>steps()</code> or
     * <code>linear()</code>); CSS comments and escapes are allowed.
     * <p>
     * 완화 함수를 해석한다(시간 사전과 키프레임의 <code>easing</code> 멤버): 문자열은 CSS Easing Level 1 의 <code>&lt;easing-function&gt;</code> 하나여야 한다(키워드,
     * <code>cubic-bezier()</code>, <code>steps()</code>, <code>linear()</code>). CSS 주석과 이스케이프를 허용한다.
     *
     * @param text the text / 문자열
     * @return the parsed value, or null if the text is not one easing function / 해석한 값. 완화 함수 하나가 아니면 null
     */
    public static CssValue parseEasing(String text) {
        if (text == null) {
            return null;
        }
        CssStyleDeclaration d = new CssStyleDeclaration();
        // animation-timing-function 의 문법으로 읽고, 쉼표 목록(값이 여럿)과 CSS 전역 키워드는 거부한다.
        if (!d.setProperty("animation-timing-function", text, "")) {
            return null;
        }
        CssValue v = d.get(Prop.ANIMATION_TIMING_FUNCTION);
        // 목록 속성이므로 항목이 하나인 쉼표 목록은 그 항목이다.
        if (v instanceof CssList && ((CssList) v).isComma() && ((CssList) v).size() == 1) {
            v = ((CssList) v).get(0);
        }
        if (v == null || (v instanceof CssList && ((CssList) v).isComma()) || v.isKeyword("initial") || v.isKeyword("inherit")
                || v.isKeyword("unset") || v.isKeyword("revert") || v.isKeyword("revert-layer")) {
            return null;
        }
        return v;
    }

    /**
     * Checks the members of a timing dictionary before they are assigned (Web Animations 6.5.4 "update the timing properties"): the iteration start
     * must not be negative, the iteration count and the duration must not be negative or NaN, the easing must parse.
     * <p>
     * 시간 사전의 멤버를 할당하기 전에 검사한다(Web Animations 6.5.4 "update the timing properties"): 반복 시작은 음수가 아니어야 하고, 반복 횟수와 지속 시간은
     * 음수나 NaN 이 아니어야 하며, 완화는 해석되어야 한다.
     *
     * @param iterationStart the iteration start or NaN when the member is missing / 반복 시작. 멤버가 없으면 NaN
     * @param iterations     the iteration count or null when missing / 반복 횟수. 없으면 null
     * @param duration       the duration in milliseconds, null when missing or <code>auto</code> / 지속 시간(밀리초). 없거나 <code>auto</code> 이면 null
     * @param easing         the easing text or null when missing / 완화 문자열. 없으면 null
     * @return null if everything is valid, otherwise the message of the TypeError / 모두 올바르면 null. 아니면 TypeError 의 메시지
     */
    public static String validate(double iterationStart, Double iterations, Double duration, String easing) {
        if (!Double.isNaN(iterationStart) && iterationStart < 0) {
            return "iterationStart must be non-negative.";
        }
        if (iterations != null && (iterations.isNaN() || iterations.doubleValue() < 0)) {
            return "iterationCount must be non-negative.";
        }
        if (duration != null && (duration.isNaN() || duration.doubleValue() < 0)) {
            return "duration must be non-negative or auto.";
        }
        if (easing != null && parseEasing(easing) == null) {
            return "'" + easing + "' is not a valid value for easing";
        }
        return null;
    }

    /**
     * Sets the start delay.
     * <p>
     * 시작 지연을 정한다.
     *
     * @param ms milliseconds, finite / 밀리초. 유한
     */
    public void setDelay(double ms) {
        delay = ms;
    }

    /**
     * Returns the start delay.
     * <p>
     * 시작 지연을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    public double getDelay() {
        return delay;
    }

    /**
     * Sets the end delay.
     * <p>
     * 끝 지연을 정한다.
     *
     * @param ms milliseconds, finite / 밀리초. 유한
     */
    public void setEndDelay(double ms) {
        endDelay = ms;
    }

    /**
     * Returns the end delay.
     * <p>
     * 끝 지연을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    public double getEndDelay() {
        return endDelay;
    }

    /**
     * Sets the fill mode as specified (<code>none</code>, <code>forwards</code>, <code>backwards</code>, <code>both</code> or <code>auto</code>).
     * <p>
     * 지정한 채움 모드를 정한다(<code>none</code>, <code>forwards</code>, <code>backwards</code>, <code>both</code>, <code>auto</code>).
     *
     * @param fill the keyword / 키워드
     */
    public void setFill(String fill) {
        this.fill = fill;
    }

    /**
     * Returns the fill mode as specified (<code>getTiming().fill</code>).
     * <p>
     * 지정한 채움 모드를 반환한다(<code>getTiming().fill</code>).
     *
     * @return the keyword / 키워드
     */
    public String getFill() {
        return fill;
    }

    /**
     * Returns the fill mode used by the calculations: <code>auto</code> is <code>none</code> for a keyframe effect (6.5.1).
     * <p>
     * 계산에 쓰는 채움 모드를 반환한다: 키프레임 효과에서 <code>auto</code> 는 <code>none</code> 이다(6.5.1).
     *
     * @return the keyword / 키워드
     */
    public String getComputedFill() {
        return "auto".equals(fill) ? "none" : fill;
    }

    /**
     * Sets the iteration start.
     * <p>
     * 반복 시작을 정한다.
     *
     * @param v the value, 0 or more / 값. 0 이상
     */
    public void setIterationStart(double v) {
        iterationStart = v;
    }

    /**
     * Returns the iteration start.
     * <p>
     * 반복 시작을 반환한다.
     *
     * @return the value / 값
     */
    public double getIterationStart() {
        return iterationStart;
    }

    /**
     * Sets the iteration count.
     * <p>
     * 반복 횟수를 정한다.
     *
     * @param v the count, 0 or more, infinity allowed / 횟수. 0 이상이며 무한대 허용
     */
    public void setIterations(double v) {
        iterations = v;
    }

    /**
     * Returns the iteration count.
     * <p>
     * 반복 횟수를 반환한다.
     *
     * @return the count / 횟수
     */
    public double getIterations() {
        return iterations;
    }

    /**
     * Sets the iteration duration; NaN stands for <code>auto</code>.
     * <p>
     * 반복 지속 시간을 정한다. NaN 은 <code>auto</code> 를 뜻한다.
     *
     * @param ms milliseconds, 0 or more, infinity allowed, or NaN / 밀리초. 0 이상, 무한대 허용, 또는 NaN
     */
    public void setDuration(double ms) {
        duration = ms;
    }

    /**
     * Returns the specified iteration duration, NaN for <code>auto</code> (<code>getTiming().duration</code> shows it as the string).
     * <p>
     * 지정한 반복 지속 시간을 반환한다. <code>auto</code> 이면 NaN(<code>getTiming().duration</code> 은 문자열로 보인다).
     *
     * @return milliseconds or NaN / 밀리초 또는 NaN
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Returns the iteration duration used by the calculations: <code>auto</code> is zero in this level.
     * <p>
     * 계산에 쓰는 반복 지속 시간을 반환한다: 이 수준에서 <code>auto</code> 는 0 이다.
     *
     * @return milliseconds / 밀리초
     */
    public double getIterationDuration() {
        return Double.isNaN(duration) ? 0 : duration;
    }

    /**
     * Sets the playback direction (<code>normal</code>, <code>reverse</code>, <code>alternate</code>, <code>alternate-reverse</code>).
     * <p>
     * 재생 방향을 정한다(<code>normal</code>, <code>reverse</code>, <code>alternate</code>, <code>alternate-reverse</code>).
     *
     * @param d the keyword / 키워드
     */
    public void setDirection(String d) {
        direction = d;
    }

    /**
     * Returns the playback direction.
     * <p>
     * 재생 방향을 반환한다.
     *
     * @return the keyword / 키워드
     */
    public String getDirection() {
        return direction;
    }

    /**
     * Sets the timing function from text that {@link #parseEasing(String)} accepts; text that it refuses leaves the timing function unchanged.
     * <p>
     * {@link #parseEasing(String)} 가 받아들이는 문자열로 완화 함수를 정한다. 거부하는 문자열은 완화 함수를 바꾸지 않는다.
     *
     * @param text the text / 문자열
     * @return true if it was set / 정했으면 true
     */
    public boolean setEasing(String text) {
        CssValue v = parseEasing(text);
        if (v == null) {
            return false;
        }
        easingText = v.toCssText();
        easing = Easing.of(v);
        return true;
    }

    /**
     * Returns the timing function as serialized text.
     * <p>
     * 완화 함수를 직렬화한 문자열로 반환한다.
     *
     * @return the text / 문자열
     */
    public String getEasing() {
        return easingText;
    }

    /**
     * Returns the active duration: iteration duration times iteration count, zero if either is zero (4.8.2).
     * <p>
     * 활성 지속 시간을 반환한다: 반복 지속 시간 곱하기 반복 횟수. 어느 쪽이든 0 이면 0(4.8.2).
     *
     * @return milliseconds, may be infinite / 밀리초. 무한대일 수 있다
     */
    public double getActiveDuration() {
        double d = getIterationDuration();
        if (d == 0 || iterations == 0) {
            return 0;
        }
        return d * iterations;
    }

    /**
     * Returns the end time: <code>max(start delay + active duration + end delay, 0)</code> (4.5.3).
     * <p>
     * 끝 시간을 반환한다: <code>max(시작 지연 + 활성 지속 시간 + 끝 지연, 0)</code>(4.5.3).
     *
     * @return milliseconds, may be infinite / 밀리초. 무한대일 수 있다
     */
    public double getEndTime() {
        return Math.max(delay + getActiveDuration() + endDelay, 0);
    }

    /**
     * The result of the timing calculations at one local time.
     * <p>
     * 지역 시간 하나에서의 시간 계산 결과.
     */
    public static final class Computed {
        /** The phase, one of the <code>PHASE_</code> constants. 단계. <code>PHASE_</code> 상수 중 하나. */
        public int phase;
        /** The local time, NaN if unresolved. 지역 시간. 결정되지 않았으면 NaN. */
        public double localTime = Double.NaN;
        /** The active time, NaN if unresolved (the effect is not in effect). 활성 시간. 결정되지 않았으면(효과가 적용되지 않으면) NaN. */
        public double activeTime = Double.NaN;
        /** The overall progress, NaN if unresolved. 전체 진행도. 결정되지 않았으면 NaN. */
        public double overallProgress = Double.NaN;
        /** The simple iteration progress, NaN if unresolved. 단순 반복 진행도. 결정되지 않았으면 NaN. */
        public double simpleProgress = Double.NaN;
        /** The current iteration, NaN if unresolved, may be infinite. 현재 반복. 결정되지 않았으면 NaN, 무한대일 수 있다. */
        public double currentIteration = Double.NaN;
        /** The directed progress, NaN if unresolved. 방향을 적용한 진행도. 결정되지 않았으면 NaN. */
        public double directedProgress = Double.NaN;
        /** The transformed progress (the iteration progress), NaN if unresolved. 변환된 진행도(반복 진행도). 결정되지 않았으면 NaN. */
        public double progress = Double.NaN;
        /** True if the current direction is forwards. 현재 방향이 정방향이면 true. */
        public boolean forwards = true;

        /**
         * Creates an empty result.
         * <p>
         * 빈 결과를 만든다.
         */
        public Computed() {
        }

        /**
         * Tells whether the effect is in effect (its active time is resolved).
         * <p>
         * 효과가 적용 중인지(활성 시간이 결정되었는지) 알려 준다.
         *
         * @return true if in effect / 적용 중이면 true
         */
        public boolean inEffect() {
            return !Double.isNaN(activeTime);
        }
    }

    /**
     * Runs the timing calculations of sections 4.5.5 to 4.10 for a local time.
     * <p>
     * 지역 시간에 대해 4.5.5~4.10 절의 시간 계산을 실행한다.
     *
     * @param localTime the local time (the current time of the animation), NaN if unresolved / 지역 시간(애니메이션의 현재 시간). 결정되지 않았으면 NaN
     * @param backwards true if the animation direction is backwards (the playback rate of the animation is negative) / 애니메이션 방향이 역방향(재생 속도가 음수)이면 true
     * @param keyframe  true for a keyframe effect (fill <code>auto</code> is <code>none</code>) / 키프레임 효과이면 true(채움 <code>auto</code> 는 <code>none</code>)
     * @return the result / 결과
     */
    public Computed compute(double localTime, boolean backwards, boolean keyframe) {
        Computed c = new Computed();
        c.localTime = localTime;
        if (Double.isNaN(localTime)) {
            c.phase = PHASE_IDLE;
            return c;
        }
        double active = getActiveDuration();
        double end = getEndTime();
        double beforeActive = Math.max(Math.min(delay, end), 0);
        double activeAfter = Math.max(Math.min(delay + active, end), 0);
        // 단계를 정한다: 역방향이면 before 경계 시각 자체가 before 이고 정방향이면 after 경계 시각 자체가 after 이다(4.5.5).
        if (localTime < beforeActive || (backwards && localTime == beforeActive)) {
            c.phase = PHASE_BEFORE;
        } else if (localTime > activeAfter || (!backwards && localTime == activeAfter)) {
            c.phase = PHASE_AFTER;
        } else {
            c.phase = PHASE_ACTIVE;
        }
        String f = keyframe ? getComputedFill() : ("auto".equals(fill) ? "both" : fill);
        // 활성 시간: 단계와 채움 모드에 따라 정한다(4.8.3.1).
        if (c.phase == PHASE_BEFORE) {
            c.activeTime = "backwards".equals(f) || "both".equals(f) ? Math.max(localTime - delay, 0) : Double.NaN;
        } else if (c.phase == PHASE_ACTIVE) {
            c.activeTime = localTime - delay;
        } else {
            c.activeTime = "forwards".equals(f) || "both".equals(f) ? Math.max(Math.min(localTime - delay, active), 0) : Double.NaN;
        }
        if (Double.isNaN(c.activeTime)) {
            return c;
        }
        double iterDuration = getIterationDuration();
        // 전체 진행도(4.8.3.2): 반복 지속 시간이 0 이면 before 단계에서 0, 그 밖에는 반복 횟수이다.
        double overall;
        if (iterDuration == 0) {
            overall = c.phase == PHASE_BEFORE ? 0 : iterations;
        } else {
            overall = c.activeTime / iterDuration;
        }
        overall += iterationStart;
        c.overallProgress = overall;
        // 단순 반복 진행도(4.8.3.3): 반복의 끝에서 정확히 끝나는 채움은 다음 반복의 0 이 아니라 마지막 반복의 1 을 유지한다.
        double simple = Double.isInfinite(overall) ? iterationStart % 1.0 : overall % 1.0;
        if (simple == 0 && (c.phase == PHASE_ACTIVE || c.phase == PHASE_AFTER) && c.activeTime == active && iterations != 0) {
            simple = 1.0;
        }
        c.simpleProgress = simple;
        // 현재 반복(4.8.4).
        if (c.phase == PHASE_AFTER && Double.isInfinite(iterations)) {
            c.currentIteration = Double.POSITIVE_INFINITY;
        } else if (simple == 1.0) {
            c.currentIteration = Math.floor(overall) - 1;
        } else {
            c.currentIteration = Math.floor(overall);
        }
        // 방향을 적용한다(4.9.1).
        boolean forwards;
        if ("normal".equals(direction)) {
            forwards = true;
        } else if ("reverse".equals(direction)) {
            forwards = false;
        } else {
            double d = c.currentIteration;
            if ("alternate-reverse".equals(direction)) {
                d += 1;
            }
            forwards = Double.isInfinite(d) || d % 2 == 0;
        }
        c.forwards = forwards;
        c.directedProgress = forwards ? simple : 1.0 - simple;
        // 완화 함수를 적용한다(4.10.1): before 플래그는 계단 함수의 경계에서 쓰인다.
        boolean beforeFlag = (c.phase == PHASE_BEFORE && forwards) || (c.phase == PHASE_AFTER && !forwards);
        c.progress = easing.at(c.directedProgress, beforeFlag);
        return c;
    }
}
