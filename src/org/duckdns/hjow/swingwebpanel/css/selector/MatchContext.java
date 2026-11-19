package org.duckdns.hjow.swingwebpanel.css.selector;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The state of one matching operation: the scoping element (for <code>:scope</code>), the dynamic element state provider, the pseudo-element
 * being styled, and a work budget that protects against selectors with pathological backtracking. A context is cheap and is not thread-safe;
 * create one per thread or per operation.
 * <p>
 * 매칭 작업 하나의 상태: <code>:scope</code> 용 범위 요소, 동적 요소 상태 제공자, 스타일을 계산하는 의사 요소, 비정상적으로 역추적하는 선택자를 막는
 * 작업량 한도. 컨텍스트는 가볍고 스레드 안전하지 않으므로 스레드나 작업마다 만든다.
 */
public final class MatchContext {

    /** Default work budget of one top-level match. / 최상위 매칭 하나의 기본 작업량 한도. */
    public static final int DEFAULT_BUDGET = 200000;

    /** Scoping element or null. / 범위 요소 또는 null. */
    private Element scope;

    /** Dynamic state provider or null. / 동적 상태 제공자 또는 null. */
    private ElementState state;

    /** Pseudo-element being matched (lower case) or null for the element itself. / 매칭하는 의사 요소(소문자). 요소 자체이면 null. */
    private String pseudoElement;

    /** Steps left. / 남은 단계. */
    private int remaining = DEFAULT_BUDGET;

    /** Depth of the nesting inside functional pseudo-class arguments. / 함수형 의사 클래스 인수 안의 중첩 깊이. */
    private int nesting;

    /** True once the budget ran out. / 한도를 넘었으면 true. */
    private boolean exhausted;

    /** True once a selector consulted the dynamic state of an element. / 선택자가 요소의 동적 상태를 조회했으면 true. */
    private boolean dynamicUsed;

    /**
     * Creates a context without scope and state.
     * <p>
     * 범위와 상태가 없는 컨텍스트를 만든다.
     */
    public MatchContext() {
    }

    /**
     * Creates a context.
     * <p>
     * 컨텍스트를 만든다.
     *
     * @param scope scoping element or null / 범위 요소 또는 null
     * @param state state provider or null / 상태 제공자 또는 null
     */
    public MatchContext(Element scope, ElementState state) {
        this.scope = scope;
        this.state = state;
    }

    /**
     * Returns the scoping element.
     * <p>
     * 범위 요소를 반환한다.
     *
     * @return element or null / 요소 또는 null
     */
    public Element getScope() {
        return scope;
    }

    /**
     * Sets the scoping element.
     * <p>
     * 범위 요소를 설정한다.
     *
     * @param scope element or null / 요소 또는 null
     */
    public void setScope(Element scope) {
        this.scope = scope;
    }

    /**
     * Decides which elements <code>:scope</code> matches when a rule of an <code>@scope</code> block is matched: every element that is a scoping
     * root, as <code>@scope</code> can have several roots.
     * <p>
     * <code>@scope</code> 블록의 규칙을 매칭할 때 <code>:scope</code> 가 어떤 요소와 일치하는지 정한다: <code>@scope</code> 는 범위 루트가 여럿일 수 있으므로 범위 루트인 모든 요소.
     */
    public interface ScopeRoot {
        /**
         * Tells whether an element is a scoping root.
         * <p>
         * 요소가 범위 루트인지 알려 준다.
         *
         * @param e   the element / 요소
         * @param ctx the matching context / 매칭 컨텍스트
         * @return true for a scoping root / 범위 루트이면 true
         */
        boolean isScope(Element e, MatchContext ctx);
    }

    /** The scope test of an <code>@scope</code> rule, or null. / <code>@scope</code> 규칙의 범위 시험. 없으면 null. */
    private ScopeRoot scopeTest;

    /**
     * Returns the scope test.
     * <p>
     * 범위 시험을 반환한다.
     *
     * @return the test or null / 시험 또는 null
     */
    public ScopeRoot getScopeTest() {
        return scopeTest;
    }

    /**
     * Sets the scope test (null for the ordinary meaning of <code>:scope</code>).
     * <p>
     * 범위 시험을 설정한다(<code>:scope</code> 의 보통 의미이면 null).
     *
     * @param t the test or null / 시험 또는 null
     */
    public void setScopeTest(ScopeRoot t) {
        this.scopeTest = t;
    }

    /**
     * Returns the state provider.
     * <p>
     * 상태 제공자를 반환한다.
     *
     * @return provider or null / 제공자 또는 null
     */
    public ElementState getState() {
        return state;
    }

    /**
     * Sets the state provider.
     * <p>
     * 상태 제공자를 설정한다.
     *
     * @param state provider or null / 제공자 또는 null
     */
    public void setState(ElementState state) {
        this.state = state;
    }

    /**
     * Returns the pseudo-element being matched.
     * <p>
     * 매칭하는 의사 요소를 반환한다.
     *
     * @return name or null / 이름 또는 null
     */
    public String getPseudoElement() {
        return pseudoElement;
    }

    /**
     * Sets the pseudo-element being matched; null selects selectors that target elements themselves.
     * <p>
     * 매칭할 의사 요소를 설정한다. null 이면 요소 자체를 가리키는 선택자를 고른다.
     *
     * @param pseudoElement name (lower case) or null / 이름(소문자) 또는 null
     */
    public void setPseudoElement(String pseudoElement) {
        this.pseudoElement = pseudoElement;
    }

    /**
     * Tells whether matching consulted the dynamic state (hover, focus, form state ...) of any element since the last {@link #resetDynamic()}. A
     * style whose computation consulted it must be recomputed when that state changes.
     * <p>
     * 마지막 {@link #resetDynamic()} 이후 매칭이 어떤 요소의 동적 상태(hover, focus, 폼 상태 등)를 조회했는지 알려 준다. 계산에서 그것을 조회한 스타일은 그 상태가 바뀌면
     * 다시 계산해야 한다.
     *
     * @return true if dynamic state was consulted / 동적 상태를 조회했으면 true
     */
    public boolean isDynamicUsed() {
        return dynamicUsed;
    }

    /**
     * Clears the dynamic state flag.
     * <p>
     * 동적 상태 플래그를 지운다.
     */
    public void resetDynamic() {
        dynamicUsed = false;
    }

    /**
     * Records that the dynamic state of an element was consulted.
     * <p>
     * 요소의 동적 상태를 조회했음을 기록한다.
     */
    void markDynamic() {
        dynamicUsed = true;
    }

    /**
     * Restores the full work budget (call before each top-level match).
     * <p>
     * 작업량 한도를 가득 채운다(최상위 매칭 전에 호출).
     */
    public void resetBudget() {
        remaining = DEFAULT_BUDGET;
        exhausted = false;
    }

    /**
     * Enters the argument list of a functional pseudo-class (such selectors are not subject to the quirks of the standard that apply to
     * whole selectors).
     * <p>
     * 함수형 의사 클래스의 인수 목록에 들어간다(그런 선택자에는 선택자 전체에 적용되는 표준의 quirk 가 적용되지 않는다).
     */
    void enterNested() {
        nesting++;
    }

    /**
     * Leaves the argument list of a functional pseudo-class.
     * <p>
     * 함수형 의사 클래스의 인수 목록에서 나온다.
     */
    void leaveNested() {
        nesting--;
    }

    /**
     * Tells whether the match is inside the argument list of a functional pseudo-class.
     * <p>
     * 매칭이 함수형 의사 클래스의 인수 목록 안인지 알려 준다.
     *
     * @return true if nested / 중첩되어 있으면 true
     */
    boolean isNested() {
        return nesting > 0;
    }

    /**
     * Counts one step of work.
     * <p>
     * 작업 한 단계를 센다.
     *
     * @return false if the budget is exhausted and the match must give up / 한도를 넘어 매칭을 포기해야 하면 false
     */
    boolean tick() {
        if (exhausted) {
            return false;
        }
        if (--remaining < 0) {
            exhausted = true;
            return false;
        }
        return true;
    }

    /**
     * Tells whether the last matches gave up because the work budget ran out.
     * <p>
     * 작업량 한도가 다 되어 매칭을 포기했는지 알려 준다.
     *
     * @return true if the budget was exhausted / 한도를 넘었으면 true
     */
    public boolean isExhausted() {
        return exhausted;
    }
}
