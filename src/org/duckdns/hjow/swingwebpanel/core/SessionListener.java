package org.duckdns.hjow.swingwebpanel.core;

/**
 * Observer of the state of a {@link BrowserSession}. The session calls every method on the Swing event dispatch thread,
 * in the order in which the changes happened, and only when the value really changed.
 * <p>
 * {@link BrowserSession} 의 상태를 관찰하는 객체. 세션은 모든 메서드를 Swing 이벤트 디스패치 스레드에서, 변경이 일어난 순서대로,
 * 값이 실제로 달라졌을 때만 호출한다.
 */
public interface SessionListener {

    /**
     * A new page was confirmed (also for a refresh, even if the URL is unchanged).
     * <p>
     * 새 페이지가 확정되었다(URL 이 같은 새로고침도 포함).
     *
     * @param oldUrl   previous URL / 이전 URL
     * @param snapshot the new confirmed page / 새로 확정된 페이지
     */
    void pageConfirmed(String oldUrl, PageSnapshot snapshot);

    /**
     * A navigation inside the confirmed page changed only the fragment of its URL (the page and its document stay). The listener scrolls
     * to the target if there is one; it is called on the EDT after the URL has changed.
     * <p>
     * 확정된 페이지 안의 이동이 URL의 프래그먼트만 바꾸었다(페이지와 문서는 그대로). 리스너는 대상이 있으면 그곳으로 스크롤한다. URL이 바뀐 뒤에
     * EDT 에서 호출된다.
     *
     * @param oldUrl   previous URL / 이전 URL
     * @param snapshot the page, whose URL is the new one / 새 URL을 가진 페이지
     * @param target   border box of the target in document coordinates as x, y, width, height, or null / 대상의 border box(문서 좌표, x·y·폭·높이). 없으면 null
     */
    void fragmentChanged(String oldUrl, PageSnapshot snapshot, double[] target);

    /**
     * A script asked to scroll the viewport (<code>window.scrollTo</code>, <code>scrollBy</code>, <code>Element.scrollIntoView</code>); it is called
     * on the EDT. The listener scrolls so that the point is at the top left of the viewport, clamped to the content, if the component sits in
     * a viewport.
     * <p>
     * 스크립트가 뷰포트를 스크롤하라고 요청했다(<code>window.scrollTo</code>, <code>scrollBy</code>, <code>Element.scrollIntoView</code>). EDT 에서
     * 호출된다. 컴포넌트가 뷰포트 안에 있으면 리스너가 그 점이 뷰포트 왼쪽 위에 오도록 내용 범위 안에서 스크롤한다.
     *
     * @param x the x in document pixels / 문서 픽셀의 x
     * @param y the y in document pixels / 문서 픽셀의 y
     */
    void scrollRequested(double x, double y);

    /**
     * The URL of the confirmed page changed.
     * <p>
     * 확정된 페이지의 URL 이 바뀌었다.
     *
     * @param oldUrl previous URL / 이전 URL
     * @param newUrl new URL / 새 URL
     */
    void pageChanged(String oldUrl, String newUrl);

    /**
     * The document title changed.
     * <p>
     * 문서 제목이 바뀌었다.
     *
     * @param oldTitle previous title / 이전 제목
     * @param newTitle new title / 새 제목
     */
    void titleChanged(String oldTitle, String newTitle);

    /**
     * The status bar text changed.
     * <p>
     * 상태바 메시지가 바뀌었다.
     *
     * @param oldText previous text / 이전 메시지
     * @param newText new text / 새 메시지
     */
    void statusTextChanged(String oldText, String newText);

    /**
     * The progress value changed.
     * <p>
     * 진행률이 바뀌었다.
     *
     * @param oldValue previous progress / 이전 진행률
     * @param newValue new progress (0 to 100, or -1 when indeterminate) / 새 진행률(0~100, 불확정이면 -1)
     */
    void progressChanged(int oldValue, int newValue);
}
