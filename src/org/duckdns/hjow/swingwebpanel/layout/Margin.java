package org.duckdns.hjow.swingwebpanel.layout;

/**
 * A set of adjoining vertical margins that collapse (CSS 2.1 section 8.3.1): the largest positive margin and the most negative margin;
 * the collapsed value is their sum.
 * <p>
 * 합쳐지는 인접한 세로 margin 의 집합(CSS 2.1 8.3.1): 가장 큰 양수 margin 과 가장 작은 음수 margin 이며 합쳐진 값은 그 합이다.
 */
final class Margin {

    /** The largest positive margin. 가장 큰 양수 margin. */
    double pos;
    /** The most negative margin. 가장 작은(음수) margin. */
    double neg;

    /**
     * Creates an empty set.
     * <p>
     * 빈 집합을 만든다.
     */
    Margin() {
    }

    /**
     * Creates a set with one margin.
     * <p>
     * margin 하나를 가진 집합을 만든다.
     *
     * @param m the margin / margin
     * @return the set / 집합
     */
    static Margin of(double m) {
        Margin r = new Margin();
        r.add(m);
        return r;
    }

    /**
     * Adds a margin that collapses with the others.
     * <p>
     * 다른 margin 과 합쳐지는 margin 을 더한다.
     *
     * @param m the margin / margin
     */
    void add(double m) {
        if (m >= 0) {
            pos = Math.max(pos, m);
        } else {
            neg = Math.min(neg, m);
        }
    }

    /**
     * Adds all margins of another set.
     * <p>
     * 다른 집합의 모든 margin 을 더한다.
     *
     * @param o the other set / 다른 집합
     */
    void addAll(Margin o) {
        pos = Math.max(pos, o.pos);
        neg = Math.min(neg, o.neg);
    }

    /**
     * Returns the collapsed value.
     * <p>
     * 합쳐진 값을 반환한다.
     *
     * @return pos plus neg / pos 와 neg 의 합
     */
    double value() {
        return pos + neg;
    }
}
