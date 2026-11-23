package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>change</code> event of a media query list (<code>MediaQueryListEvent</code>, CSSOM View Module 4.2.1): it carries the text of
 * the query (<code>media</code>) and its new result (<code>matches</code>). It does not bubble and cannot be cancelled.
 * <p>
 * 미디어 질의 목록의 <code>change</code> 이벤트(<code>MediaQueryListEvent</code>, CSSOM View Module 4.2.1): 질의 문자열(<code>media</code>)과 새
 * 결과(<code>matches</code>)를 가진다. 버블링하지 않고 취소할 수 없다.
 */
public class MediaQueryListEvent extends Event {

    /**
     * The query text.
     * <p>
     * 질의 문자열.
     */
    private final String media;

    /**
     * The new result of the query.
     * <p>
     * 질의의 새 결과.
     */
    private final boolean matches;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type    the event type, usually <code>change</code> / 이벤트 종류. 보통 <code>change</code>
     * @param media   the query text / 질의 문자열
     * @param matches the new result / 새 결과
     */
    public MediaQueryListEvent(String type, String media, boolean matches) {
        super(type, false, false);
        this.media = media;
        this.matches = matches;
    }

    /**
     * Returns the query text (<code>MediaQueryListEvent.media</code>).
     * <p>
     * 질의 문자열을 반환한다(<code>MediaQueryListEvent.media</code>).
     *
     * @return the query / 질의
     */
    public String getMedia() {
        return media;
    }

    /**
     * Returns the new result (<code>MediaQueryListEvent.matches</code>).
     * <p>
     * 새 결과를 반환한다(<code>MediaQueryListEvent.matches</code>).
     *
     * @return true if the query matches / 질의가 일치하면 true
     */
    public boolean getMatches() {
        return matches;
    }
}
