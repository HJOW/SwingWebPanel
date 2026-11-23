package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>pageshow</code> and <code>pagehide</code> events (<code>PageTransitionEvent</code>, HTML5 2014 REC section 5.5.? "Page show and
 * page hide events"): <code>persisted</code> tells whether the page came from or goes into a back-forward cache. This component has no
 * such cache, so <code>persisted</code> is always false; the event is fired at the window after the <code>load</code> event. It does not
 * bubble and cannot be cancelled.
 * <p>
 * <code>pageshow</code>, <code>pagehide</code> 이벤트(<code>PageTransitionEvent</code>, HTML5 2014 REC "Page show and page hide events"):
 * <code>persisted</code> 는 페이지가 뒤로/앞으로 이동 캐시에서 왔는지, 그곳으로 가는지 알려 준다. 이 컴포넌트에는 그런 캐시가 없으므로
 * <code>persisted</code> 는 항상 false 이며, 이벤트는 <code>load</code> 이벤트 뒤에 window 에 발생한다. 버블링하지 않고 취소할 수 없다.
 */
public class PageTransitionEvent extends Event {

    /**
     * Whether the page comes from a cache.
     * <p>
     * 페이지가 캐시에서 왔는지 여부.
     */
    private final boolean persisted;

    /**
     * Creates the event.
     * <p>
     * 이벤트를 만든다.
     *
     * @param type      the event type: <code>pageshow</code> or <code>pagehide</code> / 이벤트 종류: <code>pageshow</code> 또는 <code>pagehide</code>
     * @param bubbles   true if it bubbles / 버블링하면 true
     * @param persisted true if the page comes from a cache / 페이지가 캐시에서 왔으면 true
     */
    public PageTransitionEvent(String type, boolean bubbles, boolean persisted) {
        super(type, bubbles, false);
        this.persisted = persisted;
    }

    /**
     * Returns whether the page comes from a cache (<code>PageTransitionEvent.persisted</code>).
     * <p>
     * 페이지가 캐시에서 왔는지 반환한다(<code>PageTransitionEvent.persisted</code>).
     *
     * @return true if persisted / 캐시에서 왔으면 true
     */
    public boolean getPersisted() {
        return persisted;
    }
}
