package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A mouse event (UI Events): the position in screen, client (the visible part of the page), page (the whole document) and target
 * element coordinates, the button that changed and the buttons that are down, and the element that the pointer came from or goes to.
 * The component fills the coordinates from the AWT event with the scroll position of the viewport.
 * <p>
 * 마우스 이벤트(UI Events): 화면, client(페이지의 보이는 부분), page(문서 전체), 대상 요소 좌표의 위치, 바뀐 버튼과 눌려 있는 버튼, 포인터가 온 곳이나
 * 가는 곳의 요소. 컴포넌트가 AWT 이벤트에서 뷰포트의 스크롤 위치를 반영해 좌표를 채운다.
 */
public class MouseEvent extends UiEvent {

    /**
     * The x coordinate on the screen.
     * <p>
     * 화면의 x 좌표.
     */
    private double screenX;

    /**
     * The y coordinate on the screen.
     * <p>
     * 화면의 y 좌표.
     */
    private double screenY;

    /**
     * The x coordinate in the visible part of the page.
     * <p>
     * 페이지의 보이는 부분 안의 x 좌표.
     */
    private double clientX;

    /**
     * The y coordinate in the visible part of the page.
     * <p>
     * 페이지의 보이는 부분 안의 y 좌표.
     */
    private double clientY;

    /**
     * The x coordinate in the whole document.
     * <p>
     * 문서 전체 안의 x 좌표.
     */
    private double pageX;

    /**
     * The y coordinate in the whole document.
     * <p>
     * 문서 전체 안의 y 좌표.
     */
    private double pageY;

    /**
     * The x coordinate relative to the padding edge of the target element.
     * <p>
     * 대상 요소의 padding 가장자리를 기준으로 한 x 좌표.
     */
    private double offsetX;

    /**
     * The y coordinate relative to the padding edge of the target element.
     * <p>
     * 대상 요소의 padding 가장자리를 기준으로 한 y 좌표.
     */
    private double offsetY;

    /**
     * The button that changed: 0 main, 1 auxiliary (wheel), 2 secondary, 3 back, 4 forward.
     * <p>
     * 바뀐 버튼: 0 주, 1 보조(휠), 2 보조 버튼(오른쪽), 3 뒤로, 4 앞으로.
     */
    private int button;

    /**
     * The buttons that are down as a bit mask: 1 main, 2 secondary, 4 auxiliary, 8 back, 16 forward.
     * <p>
     * 눌려 있는 버튼의 비트 마스크: 1 주, 2 보조 버튼, 4 보조(휠), 8 뒤로, 16 앞으로.
     */
    private int buttons;

    /**
     * The other element of an over, out, enter or leave event.
     * <p>
     * over, out, enter, leave 이벤트의 상대 요소.
     */
    private EventTarget relatedTarget;

    /**
     * Creates a mouse event.
     * <p>
     * 마우스 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블링 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public MouseEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the coordinates.
     * <p>
     * 좌표를 지정한다.
     *
     * @param sx  screen x / 화면 x
     * @param sy  screen y / 화면 y
     * @param cx  client x / client x
     * @param cy  client y / client y
     * @param px  page x / page x
     * @param py  page y / page y
     * @param ox  offset x / offset x
     * @param oy  offset y / offset y
     */
    public void setCoordinates(double sx, double sy, double cx, double cy, double px, double py, double ox, double oy) {
        this.screenX = sx;
        this.screenY = sy;
        this.clientX = cx;
        this.clientY = cy;
        this.pageX = px;
        this.pageY = py;
        this.offsetX = ox;
        this.offsetY = oy;
    }

    /**
     * Sets the button and the buttons that are down.
     * <p>
     * 바뀐 버튼과 눌려 있는 버튼을 지정한다.
     *
     * @param changed the changed button / 바뀐 버튼
     * @param down    the bit mask of the buttons that are down / 눌려 있는 버튼의 비트 마스크
     */
    public void setButtons(int changed, int down) {
        this.button = changed;
        this.buttons = down;
    }

    /**
     * Sets the related target.
     * <p>
     * 상대 대상을 지정한다.
     *
     * @param target the other element or null / 상대 요소. 없으면 null
     */
    public void setRelatedTarget(EventTarget target) {
        this.relatedTarget = target;
    }

    /**
     * Returns the screen x.
     * <p>
     * 화면 x 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getScreenX() {
        return screenX;
    }

    /**
     * Returns the screen y.
     * <p>
     * 화면 y 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getScreenY() {
        return screenY;
    }

    /**
     * Returns the client x.
     * <p>
     * client x 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getClientX() {
        return clientX;
    }

    /**
     * Returns the client y.
     * <p>
     * client y 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getClientY() {
        return clientY;
    }

    /**
     * Returns the page x.
     * <p>
     * page x 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getPageX() {
        return pageX;
    }

    /**
     * Returns the page y.
     * <p>
     * page y 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getPageY() {
        return pageY;
    }

    /**
     * Returns the offset x.
     * <p>
     * offset x 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getOffsetX() {
        return offsetX;
    }

    /**
     * Returns the offset y.
     * <p>
     * offset y 를 반환한다.
     *
     * @return the coordinate / 좌표
     */
    public double getOffsetY() {
        return offsetY;
    }

    /**
     * Returns the button that changed.
     * <p>
     * 바뀐 버튼을 반환한다.
     *
     * @return the button / 버튼
     */
    public int getButton() {
        return button;
    }

    /**
     * Returns the bit mask of the buttons that are down.
     * <p>
     * 눌려 있는 버튼의 비트 마스크를 반환한다.
     *
     * @return the mask / 마스크
     */
    public int getButtons() {
        return buttons;
    }

    /**
     * Returns the related target.
     * <p>
     * 상대 대상을 반환한다.
     *
     * @return the other element or null / 상대 요소. 없으면 null
     */
    public EventTarget getRelatedTarget() {
        return relatedTarget;
    }
}
