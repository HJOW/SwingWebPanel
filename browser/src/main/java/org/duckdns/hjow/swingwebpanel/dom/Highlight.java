package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom highlight of the CSS Custom Highlight API Level 1 (https://www.w3.org/TR/css-highlight-api-1/, W3C Working Draft of 14 December 2021, section 3.1):
 * a set of ranges (live {@link Range}s or {@link StaticRange}s, compared by identity and kept in insertion order) with a priority (default 0; a higher
 * priority is painted above) and a type (<code>highlight</code>, <code>spelling-error</code> or <code>grammar-error</code>; an unknown type is ignored, as
 * for an IDL enumeration). A highlight has an effect only when it is registered in the {@link HighlightRegistry} of a document under a name that the
 * pseudo-element <code>::highlight(name)</code> styles. Every change is reported to the registries that hold the highlight, so the page is painted again.
 * <p>
 * Use: <code>Highlight h = new Highlight(); h.add(range); document.getHighlights().set("found", h);</code>. Change it on the document thread only.
 * <p>
 * CSS Custom Highlight API Level 1 의 사용자 정의 하이라이트(W3C Working Draft 2021-12-14, 3.1 절): 범위(살아 있는 {@link Range} 나 {@link StaticRange}. 동일성으로
 * 비교하고 넣은 순서로 보관)의 집합이며, 우선순위(기본 0, 높을수록 위에 그린다)와 종류(<code>highlight</code>, <code>spelling-error</code>, <code>grammar-error</code>. IDL 열거형처럼
 * 모르는 종류는 무시)를 가진다. 문서의 {@link HighlightRegistry} 에 의사 요소 <code>::highlight(이름)</code> 이 꾸미는 이름으로 등록되어야 효과가 있다. 모든 변경은 이 하이라이트를
 * 가진 등록부에 알려 페이지를 다시 그리게 한다.
 * <p>
 * 사용법: <code>Highlight h = new Highlight(); h.add(range); document.getHighlights().set("found", h);</code>. 문서 스레드에서만 바꾼다.
 */
public final class Highlight {

    /** The ranges in insertion order. 넣은 순서의 범위. */
    private final List<Object> ranges = new ArrayList<Object>();

    /** The priority (CSS Highlight API 4.2.5). 우선순위(CSS Highlight API 4.2.5 절). */
    private int priority;

    /** The type (CSS Highlight API 4.2.6). 종류(CSS Highlight API 4.2.6 절). */
    private String type = "highlight";

    /** The registries that hold this highlight. 이 하이라이트를 가진 등록부. */
    private final List<HighlightRegistry> owners = new ArrayList<HighlightRegistry>(1);

    /**
     * Creates an empty highlight with the priority 0 and the type <code>highlight</code>.
     * <p>
     * 우선순위 0, 종류 <code>highlight</code> 인 빈 하이라이트를 만든다.
     */
    public Highlight() {
    }

    /**
     * Tells whether an object can be an entry of a highlight (an <code>AbstractRange</code>: a {@link Range} or a {@link StaticRange}).
     * <p>
     * 객체가 하이라이트의 항목(<code>AbstractRange</code>: {@link Range} 나 {@link StaticRange})이 될 수 있는지 알려 준다.
     *
     * @param o the object / 객체
     * @return true for ranges / 범위이면 true
     */
    public static boolean isRange(Object o) {
        return o instanceof Range || o instanceof StaticRange;
    }

    /**
     * Adds a range (setlike <code>add</code>); a range that is already in the set is not added again.
     * <p>
     * 범위를 더한다(setlike <code>add</code>). 이미 있는 범위는 다시 넣지 않는다.
     *
     * @param range a {@link Range} or a {@link StaticRange} / {@link Range} 나 {@link StaticRange}
     * @return this highlight / 이 하이라이트
     * @throws IllegalArgumentException if the object is not a range / 범위가 아니면
     */
    public Highlight add(Object range) {
        if (!isRange(range)) {
            throw new IllegalArgumentException("not an AbstractRange");
        }
        if (indexOf(range) < 0) {
            ranges.add(range);
            changed();
        }
        return this;
    }

    /**
     * Removes a range (setlike <code>delete</code>).
     * <p>
     * 범위를 뺀다(setlike <code>delete</code>).
     *
     * @param range the range / 범위
     * @return true if it was in the set / 집합에 있었으면 true
     */
    public boolean delete(Object range) {
        int i = indexOf(range);
        if (i < 0) {
            return false;
        }
        ranges.remove(i);
        changed();
        return true;
    }

    /**
     * Tells whether a range is in the set (setlike <code>has</code>).
     * <p>
     * 범위가 집합에 있는지 알려 준다(setlike <code>has</code>).
     *
     * @param range the range / 범위
     * @return true if it is / 있으면 true
     */
    public boolean has(Object range) {
        return indexOf(range) >= 0;
    }

    /**
     * Removes every range (setlike <code>clear</code>).
     * <p>
     * 모든 범위를 뺀다(setlike <code>clear</code>).
     */
    public void clear() {
        if (!ranges.isEmpty()) {
            ranges.clear();
            changed();
        }
    }

    /**
     * Returns the number of ranges (setlike <code>size</code>).
     * <p>
     * 범위의 수를 반환한다(setlike <code>size</code>).
     *
     * @return the size / 크기
     */
    public int getSize() {
        return ranges.size();
    }

    /**
     * Returns a snapshot of the ranges in insertion order.
     * <p>
     * 넣은 순서의 범위 스냅샷을 반환한다.
     *
     * @return a new list / 새 목록
     */
    public List<Object> getRanges() {
        return new ArrayList<Object>(ranges);
    }

    /**
     * Returns the priority.
     * <p>
     * 우선순위를 반환한다.
     *
     * @return the priority / 우선순위
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority; the highlights are painted again.
     * <p>
     * 우선순위를 정한다. 하이라이트를 다시 그린다.
     *
     * @param priority the priority / 우선순위
     */
    public void setPriority(int priority) {
        if (this.priority != priority) {
            this.priority = priority;
            changed();
        }
    }

    /**
     * Returns the type.
     * <p>
     * 종류를 반환한다.
     *
     * @return <code>highlight</code>, <code>spelling-error</code> or <code>grammar-error</code> / 종류
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type; a value that is not one of the three types is ignored (an IDL enumeration attribute).
     * <p>
     * 종류를 정한다. 세 종류가 아닌 값은 무시한다(IDL 열거형 속성).
     *
     * @param type the type / 종류
     */
    public void setType(String type) {
        if ("highlight".equals(type) || "spelling-error".equals(type) || "grammar-error".equals(type)) {
            this.type = type;
        }
    }

    /**
     * Returns the index of a range by identity.
     * <p>
     * 동일성으로 범위의 위치를 반환한다.
     *
     * @param range the range / 범위
     * @return the index or -1 / 위치. 없으면 -1
     */
    private int indexOf(Object range) {
        // 범위는 동일성으로 비교한다(Set 의 SameValueZero).
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i) == range) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Records that a registry holds this highlight (once per registration).
     * <p>
     * 등록부가 이 하이라이트를 가진다고 기록한다(등록마다 한 번).
     *
     * @param registry the registry / 등록부
     */
    void attach(HighlightRegistry registry) {
        owners.add(registry);
    }

    /**
     * Forgets one registration of this highlight in a registry.
     * <p>
     * 등록부에서 이 하이라이트의 등록 하나를 잊는다.
     *
     * @param registry the registry / 등록부
     */
    void detach(HighlightRegistry registry) {
        // 같은 등록부의 기록 하나만 지운다.
        for (int i = 0; i < owners.size(); i++) {
            if (owners.get(i) == registry) {
                owners.remove(i);
                return;
            }
        }
    }

    /**
     * Reports a change to the registries that hold this highlight.
     * <p>
     * 이 하이라이트를 가진 등록부에 변경을 알린다.
     */
    private void changed() {
        // 이 하이라이트를 가진 등록부마다 다시 그리기를 요청한다.
        for (int i = 0; i < owners.size(); i++) {
            owners.get(i).changed();
        }
    }
}
