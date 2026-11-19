package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A wheel event (UI Events): a mouse event with the amount that the wheel turned along the three axes and the unit of the amount.
 * <p>
 * 휠 이벤트(UI Events): 휠이 세 축으로 돌아간 양과 그 단위를 가진 마우스 이벤트.
 */
public class WheelEvent extends MouseEvent {

    /**
     * Unit of the deltas: pixels.
     * <p>
     * 변화량의 단위: 픽셀.
     */
    public static final int DOM_DELTA_PIXEL = 0;

    /**
     * Unit of the deltas: lines of text.
     * <p>
     * 변화량의 단위: 텍스트 줄.
     */
    public static final int DOM_DELTA_LINE = 1;

    /**
     * Unit of the deltas: pages.
     * <p>
     * 변화량의 단위: 페이지.
     */
    public static final int DOM_DELTA_PAGE = 2;

    /**
     * The horizontal amount.
     * <p>
     * 가로 양.
     */
    private double deltaX;

    /**
     * The vertical amount (positive scrolls down).
     * <p>
     * 세로 양(양수는 아래로 스크롤).
     */
    private double deltaY;

    /**
     * The amount along the z axis.
     * <p>
     * z 축 양.
     */
    private double deltaZ;

    /**
     * The unit of the amounts.
     * <p>
     * 양의 단위.
     */
    private int deltaMode;

    /**
     * Creates a wheel event.
     * <p>
     * 휠 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public WheelEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the amounts.
     * <p>
     * 양을 지정한다.
     *
     * @param x    horizontal / 가로
     * @param y    vertical / 세로
     * @param z    z axis / z 축
     * @param mode the unit / 단위
     */
    public void setDeltas(double x, double y, double z, int mode) {
        this.deltaX = x;
        this.deltaY = y;
        this.deltaZ = z;
        this.deltaMode = mode;
    }

    /**
     * Returns the horizontal amount.
     * <p>
     * 가로 양을 반환한다.
     *
     * @return the amount / 양
     */
    public double getDeltaX() {
        return deltaX;
    }

    /**
     * Returns the vertical amount.
     * <p>
     * 세로 양을 반환한다.
     *
     * @return the amount / 양
     */
    public double getDeltaY() {
        return deltaY;
    }

    /**
     * Returns the z amount.
     * <p>
     * z 양을 반환한다.
     *
     * @return the amount / 양
     */
    public double getDeltaZ() {
        return deltaZ;
    }

    /**
     * Returns the unit of the amounts.
     * <p>
     * 양의 단위를 반환한다.
     *
     * @return the unit / 단위
     */
    public int getDeltaMode() {
        return deltaMode;
    }
}
