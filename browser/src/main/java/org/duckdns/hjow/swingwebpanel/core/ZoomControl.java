package org.duckdns.hjow.swingwebpanel.core;

/**
 * What the input handlers (Ctrl+wheel and Ctrl+plus, Ctrl+minus, Ctrl+0) need from the component to change the page zoom (plan item P11-08).
 * The component implements it on top of its <code>getZoom</code> and <code>setZoom</code>; both are called on the event dispatch thread.
 * <p>
 * 입력 처리기(Ctrl+휠, Ctrl+플러스, Ctrl+마이너스, Ctrl+0)가 페이지 확대를 바꾸기 위해 컴포넌트에 필요로 하는 것(계획 P11-08). 컴포넌트가
 * <code>getZoom</code> 과 <code>setZoom</code> 위에 구현하며, 둘 다 이벤트 디스패치 스레드에서 호출된다.
 */
public interface ZoomControl {

    /**
     * Returns the current zoom factor.
     * <p>
     * 현재 확대 배율을 반환한다.
     *
     * @return the factor, 1 for 100 percent / 배율. 1 이면 100 %
     */
    double getZoom();

    /**
     * Sets the zoom factor (the implementation keeps it inside the allowed range).
     * <p>
     * 확대 배율을 정한다(구현이 허용 범위 안으로 맞춘다).
     *
     * @param zoom the factor / 배율
     */
    void setZoom(double zoom);

    /**
     * Keeps the point of the page that is under a place of the viewport where it is while the zoom changes (the pointer position of
     * Ctrl+wheel). The default of the component uses the top-left corner of the viewport; this is called before {@link #setZoom(double)}.
     * <p>
     * 확대가 바뀌는 동안 뷰포트의 한 곳 아래에 있는 페이지의 점이 그 자리에 머물게 한다(Ctrl+휠의 포인터 위치). 컴포넌트의 기본은 뷰포트의 왼쪽 위 모서리를
     * 쓴다. {@link #setZoom(double)} 앞에 호출된다.
     *
     * @param x x in component coordinates / 컴포넌트 좌표의 x
     * @param y y in component coordinates / 컴포넌트 좌표의 y
     */
    void setZoomAnchor(int x, int y);
}
