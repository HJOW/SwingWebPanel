package org.duckdns.hjow.swingwebpanel;

/**
 * The state of the search in the page of a {@link SwingWebPanel} (extension, API contract 5.29): what is searched for, how many places
 * of the page match, and which of them is the current one. The component announces a new result as the
 * {@link SwingWebPanel#PROPERTY_FIND_RESULT} property on the event dispatch thread; {@link SwingWebPanel#getFindResult()} returns the
 * last one. Objects are immutable.
 * <p>
 * {@link SwingWebPanel} 의 페이지 안 찾기 상태(확장, API 계약 5.29): 무엇을 찾는지, 페이지에서 몇 곳이 일치하는지, 그 가운데 어느 것이 현재
 * 일치인지. 컴포넌트는 새 결과를 {@link SwingWebPanel#PROPERTY_FIND_RESULT} 속성으로 이벤트 디스패치 스레드에서 알리고,
 * {@link SwingWebPanel#getFindResult()} 가 마지막 결과를 반환한다. 객체는 불변이다.
 * <p>
 * 사용 예 / Example:
 * <pre>
 * panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_FIND_RESULT, new PropertyChangeListener() {
 *     public void propertyChange(PropertyChangeEvent e) {
 *         FindResult r = (FindResult) e.getNewValue();
 *         label.setText(r.getIndex() + " / " + r.getCount());
 *     }
 * });
 * panel.find("swing", false, true);
 * </pre>
 */
public final class FindResult {

    /** The result while nothing is searched for. / 아무것도 찾지 않는 동안의 결과. */
    public static final FindResult NONE = new FindResult("", false, 0, 0);

    /** The search text. / 검색어. */
    private final String text;

    /** True if upper and lower case are told apart. / 대소문자를 구분하면 true. */
    private final boolean matchCase;

    /** The number of matches. / 일치 개수. */
    private final int count;

    /** The number of the current match, starting at 1; 0 if there is none. / 현재 일치의 번호(1 부터). 없으면 0. */
    private final int index;

    /**
     * Creates a result; the component does this.
     * <p>
     * 결과를 만든다. 컴포넌트가 만든다.
     *
     * @param text      the search text / 검색어
     * @param matchCase true if upper and lower case are told apart / 대소문자를 구분하면 true
     * @param count     the number of matches / 일치 개수
     * @param index     the number of the current match starting at 1, or 0 / 현재 일치의 번호(1 부터) 또는 0
     */
    public FindResult(String text, boolean matchCase, int count, int index) {
        this.text = text == null ? "" : text;
        this.matchCase = matchCase;
        this.count = Math.max(0, count);
        this.index = Math.max(0, Math.min(index, this.count));
    }

    /**
     * Returns the text that is searched for.
     * <p>
     * 찾는 글을 반환한다.
     *
     * @return the search text; empty while nothing is searched for / 검색어. 아무것도 찾지 않는 동안에는 빈 문자열
     */
    public String getText() {
        return text;
    }

    /**
     * Tells whether the search tells upper and lower case apart.
     * <p>
     * 찾기가 대소문자를 구분하는지 알려 준다.
     *
     * @return true if case matters / 대소문자를 구분하면 true
     */
    public boolean isMatchCase() {
        return matchCase;
    }

    /**
     * Returns the number of places of the page that match.
     * <p>
     * 페이지에서 일치하는 곳의 수를 반환한다.
     *
     * @return the number of matches / 일치 개수
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the number of the current match, counted from 1 in the order of the page.
     * <p>
     * 현재 일치의 번호를 반환한다. 페이지의 순서로 1 부터 센다.
     *
     * @return the number, or 0 if nothing matches / 번호. 일치가 없으면 0
     */
    public int getIndex() {
        return index;
    }

    /**
     * Tells whether a search is active (a text is searched for, whether or not it was found).
     * <p>
     * 찾기가 활성인지 알려 준다(찾는 글이 있다. 찾았는지와는 상관없다).
     *
     * @return true while a text is searched for / 찾는 글이 있는 동안 true
     */
    public boolean isActive() {
        return text.length() > 0;
    }

    /**
     * Compares all four values.
     * <p>
     * 네 값을 모두 비교한다.
     *
     * @param other another object / 다른 객체
     * @return true for an equal result / 같은 결과이면 true
     */
    public boolean equals(Object other) {
        if (!(other instanceof FindResult)) {
            return false;
        }
        FindResult o = (FindResult) other;
        return text.equals(o.text) && matchCase == o.matchCase && count == o.count && index == o.index;
    }

    /**
     * Returns a hash code that fits {@link #equals(Object)}.
     * <p>
     * {@link #equals(Object)} 에 맞는 해시 코드를 반환한다.
     *
     * @return the hash code / 해시 코드
     */
    public int hashCode() {
        return text.hashCode() * 31 + count * 7 + index + (matchCase ? 1 : 0);
    }

    /**
     * Describes the result, for logs and tests.
     * <p>
     * 결과를 설명한다. 로그와 시험에 쓴다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return "FindResult[" + index + "/" + count + " of \"" + text + "\"" + (matchCase ? ", match case" : "") + "]";
    }
}
