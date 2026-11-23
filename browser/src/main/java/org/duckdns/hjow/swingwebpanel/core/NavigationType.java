package org.duckdns.hjow.swingwebpanel.core;

/**
 * Why a navigation was started. It decides how the confirmed page is recorded in the history.
 * <p>
 * 이동이 시작된 이유. 확정된 페이지를 이력에 어떻게 기록할지를 결정한다.
 */
public enum NavigationType {

    /**
     * A new navigation (<code>setPage</code>, link, form): adds a history entry.
     * <p>
     * 새 이동(<code>setPage</code>, 링크, 폼): 이력 항목을 추가한다.
     */
    NAVIGATE,

    /**
     * Content given with <code>setContent</code>: adds a history entry.
     * <p>
     * <code>setContent</code> 로 받은 콘텐츠: 이력 항목을 추가한다.
     */
    SET_CONTENT,

    /**
     * A reload of the current page: replaces the current entry.
     * <p>
     * 현재 페이지 다시 읽기: 현재 항목을 교체한다.
     */
    REFRESH,

    /**
     * Navigation to another URL that replaces the current history entry instead of adding one (<code>location.replace</code>).
     * <p>
     * 이력 항목을 추가하지 않고 현재 항목을 바꾸는 다른 URL로의 이동(<code>location.replace</code>).
     */
    REPLACE,

    /**
     * A move inside the history (back/forward): moves the position and refreshes that entry.
     * <p>
     * 이력 안의 이동(뒤로/앞으로): 위치를 옮기고 그 항목을 갱신한다.
     */
    HISTORY
}
