package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>hashchange</code> event (<code>HashChangeEvent</code>, HTML5 2014 REC 5.6.10.1): fired at the window after a move between two
 * entries of the same document whose URLs differ in the fragment. It carries the old and the new URL.
 * <p>
 * <code>hashchange</code> 이벤트(<code>HashChangeEvent</code>, HTML5 2014 REC 5.6.10.1): URL의 프래그먼트가 다른 같은 문서의 두 항목 사이를 옮겨 간 뒤
 * window 에 발생한다. 이전 URL과 새 URL을 싣는다.
 */
public class HashChangeEvent extends Event {

    /**
     * The URL before the move.
     * <p>
     * 이동 전의 URL.
     */
    private String oldUrl = "";

    /**
     * The URL after the move.
     * <p>
     * 이동 뒤의 URL.
     */
    private String newUrl = "";

    /**
     * Creates the event with empty URLs.
     * <p>
     * URL이 빈 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public HashChangeEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Returns the URL before the move.
     * <p>
     * 이동 전의 URL을 반환한다.
     *
     * @return the old URL / 이전 URL
     */
    public String getOldUrl() {
        return oldUrl;
    }

    /**
     * Returns the URL after the move.
     * <p>
     * 이동 뒤의 URL을 반환한다.
     *
     * @return the new URL / 새 URL
     */
    public String getNewUrl() {
        return newUrl;
    }

    /**
     * Sets both URLs (before the event is dispatched).
     * <p>
     * 두 URL을 지정한다(이벤트를 전달하기 전에).
     *
     * @param oldValue the URL before / 이전 URL
     * @param newValue the URL after / 이후 URL
     */
    public void setUrls(String oldValue, String newValue) {
        this.oldUrl = oldValue == null ? "" : oldValue;
        this.newUrl = newValue == null ? "" : newValue;
    }
}
