package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A pointer event (W3C Pointer Events Level 3): a mouse event with the identity of the pointer that caused it (<code>pointerId</code>,
 * <code>pointerType</code>, <code>isPrimary</code>) and the properties of its contact (<code>width</code>, <code>height</code>, <code>pressure</code>,
 * <code>tangentialPressure</code>, tilt, twist and the altitude and azimuth angles). The mouse of this component is the only pointer: the id
 * {@link #MOUSE_POINTER_ID}, the type <code>mouse</code>, always primary, a contact of 1 by 1 pixels, a pressure of 0.5 while a button is down and 0
 * otherwise, and the neutral values (altitude {@code PI / 2}) for everything that a mouse cannot report. Events that the component fires are
 * trusted; events that a script constructs take their values from the dictionary (the defaults of the specification), and the lists of coalesced
 * and predicted events are copies of the dictionary lists (empty for events of the component).
 * <p>
 * 포인터 이벤트(W3C Pointer Events Level 3): 이벤트를 일으킨 포인터의 정체(<code>pointerId</code>, <code>pointerType</code>, <code>isPrimary</code>)와 접촉의
 * 속성(<code>width</code>, <code>height</code>, <code>pressure</code>, <code>tangentialPressure</code>, 기울기, 비틀림, 고도각과 방위각)을 가진 마우스
 * 이벤트. 이 컴포넌트의 마우스가 유일한 포인터이다: ID {@link #MOUSE_POINTER_ID}, 종류 <code>mouse</code>, 항상 주 포인터, 1 x 1 픽셀 접촉, 버튼이 눌려 있는
 * 동안 압력 0.5(아니면 0), 마우스가 보고할 수 없는 것은 중립 값(고도각 {@code PI / 2}). 컴포넌트가 발생시키는 이벤트는 신뢰 이벤트이다. 스크립트가 만든 이벤트는
 * 사전의 값(규격의 기본값)을 쓰고, 합쳐진 이벤트와 예측 이벤트의 목록은 사전 목록의 복사본이다(컴포넌트의 이벤트는 비어 있다).
 */
public class PointerEvent extends MouseEvent {

    /**
     * The id of the mouse pointer.
     * <p>
     * 마우스 포인터의 ID.
     */
    public static final int MOUSE_POINTER_ID = 1;

    /**
     * The identifier of the pointer.
     * <p>
     * 포인터의 식별자.
     */
    private int pointerId;

    /**
     * The width of the contact geometry in CSS pixels.
     * <p>
     * 접촉 형상의 너비(CSS 픽셀).
     */
    private double width = 1;

    /**
     * The height of the contact geometry in CSS pixels.
     * <p>
     * 접촉 형상의 높이(CSS 픽셀).
     */
    private double height = 1;

    /**
     * The normalized pressure, 0 to 1.
     * <p>
     * 정규화한 압력(0~1).
     */
    private double pressure;

    /**
     * The normalized tangential pressure, -1 to 1.
     * <p>
     * 정규화한 접선 압력(-1~1).
     */
    private double tangentialPressure;

    /**
     * The tilt around the Y axis in degrees.
     * <p>
     * Y 축 둘레의 기울기(도).
     */
    private int tiltX;

    /**
     * The tilt around the X axis in degrees.
     * <p>
     * X 축 둘레의 기울기(도).
     */
    private int tiltY;

    /**
     * The clockwise rotation of the transducer around its axis in degrees.
     * <p>
     * 장치가 자기 축 둘레로 시계 방향으로 돈 각도(도).
     */
    private int twist;

    /**
     * The altitude angle in radians.
     * <p>
     * 고도각(라디안).
     */
    private double altitudeAngle = Math.PI / 2;

    /**
     * The azimuth angle in radians.
     * <p>
     * 방위각(라디안).
     */
    private double azimuthAngle;

    /**
     * The device type: <code>mouse</code>, <code>pen</code>, <code>touch</code> or the empty string.
     * <p>
     * 장치 종류: <code>mouse</code>, <code>pen</code>, <code>touch</code> 또는 빈 문자열.
     */
    private String pointerType = "";

    /**
     * True for the primary pointer of its type.
     * <p>
     * 그 종류의 주 포인터이면 true.
     */
    private boolean primary;

    /**
     * The coalesced events (a copy of the constructor dictionary).
     * <p>
     * 합쳐진 이벤트(생성자 사전의 복사본).
     */
    private List<PointerEvent> coalesced = Collections.<PointerEvent>emptyList();

    /**
     * The predicted events (a copy of the constructor dictionary).
     * <p>
     * 예측 이벤트(생성자 사전의 복사본).
     */
    private List<PointerEvent> predicted = Collections.<PointerEvent>emptyList();

    /**
     * Creates a pointer event with the defaults of the specification (pointer 0, no type, not primary, 1 x 1 contact).
     * <p>
     * 규격의 기본값(포인터 0, 종류 없음, 주 포인터 아님, 1 x 1 접촉)으로 포인터 이벤트를 만든다.
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether the event bubbles / 버블링 여부
     * @param cancelable whether the event can be cancelled / 취소 가능 여부
     */
    public PointerEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Creates the event of the mouse pointer: the id 1, the type <code>mouse</code>, primary, and the pressure that belongs to the buttons that are
     * down (0.5 while any button is down, 0 otherwise, and always 0 for <code>pointerup</code>).
     * <p>
     * 마우스 포인터의 이벤트를 만든다: ID 1, 종류 <code>mouse</code>, 주 포인터, 눌려 있는 버튼에 맞는 압력(버튼이 눌려 있으면 0.5, 아니면 0.
     * <code>pointerup</code> 은 항상 0).
     *
     * @param type       the event type / 이벤트 종류
     * @param bubbles    whether the event bubbles / 버블링 여부
     * @param cancelable whether the event can be cancelled / 취소 가능 여부
     * @return the event / 이벤트
     */
    public static PointerEvent forMouse(String type, boolean bubbles, boolean cancelable) {
        PointerEvent e = new PointerEvent(type, bubbles, cancelable);
        e.pointerId = MOUSE_POINTER_ID;
        e.pointerType = "mouse";
        e.primary = true;
        return e;
    }

    /**
     * Sets the identity and the contact properties.
     * <p>
     * 정체와 접촉 속성을 정한다.
     *
     * @param id          the pointer id / 포인터 ID
     * @param type        the pointer type / 포인터 종류
     * @param isPrimary   true for the primary pointer / 주 포인터이면 true
     * @param w           the contact width / 접촉 너비
     * @param h           the contact height / 접촉 높이
     * @param pressureNow the pressure / 압력
     * @param tangential  the tangential pressure / 접선 압력
     * @param tx          the tilt around the Y axis / Y 축 기울기
     * @param ty          the tilt around the X axis / X 축 기울기
     * @param twistNow    the twist / 비틀림
     * @param altitude    the altitude angle / 고도각
     * @param azimuth     the azimuth angle / 방위각
     */
    public void setPointer(int id, String type, boolean isPrimary, double w, double h, double pressureNow, double tangential, int tx, int ty,
            int twistNow, double altitude, double azimuth) {
        this.pointerId = id;
        this.pointerType = type == null ? "" : type;
        this.primary = isPrimary;
        this.width = w;
        this.height = h;
        this.pressure = pressureNow;
        this.tangentialPressure = tangential;
        this.tiltX = tx;
        this.tiltY = ty;
        this.twist = twistNow;
        this.altitudeAngle = altitude;
        this.azimuthAngle = azimuth;
    }

    /**
     * Sets the pressure only (a mouse reports 0.5 while a button is down and 0 otherwise).
     * <p>
     * 압력만 정한다(마우스는 버튼이 눌려 있는 동안 0.5, 아니면 0 을 보고한다).
     *
     * @param value the pressure / 압력
     */
    public void setPressure(double value) {
        this.pressure = value;
    }

    /**
     * Sets the coalesced and the predicted events (copies of the lists).
     * <p>
     * 합쳐진 이벤트와 예측 이벤트를 정한다(목록의 복사본).
     *
     * @param coalescedEvents the coalesced events / 합쳐진 이벤트
     * @param predictedEvents the predicted events / 예측 이벤트
     */
    public void setEventLists(List<PointerEvent> coalescedEvents, List<PointerEvent> predictedEvents) {
        this.coalesced = coalescedEvents == null ? Collections.<PointerEvent>emptyList() : new ArrayList<PointerEvent>(coalescedEvents);
        this.predicted = predictedEvents == null ? Collections.<PointerEvent>emptyList() : new ArrayList<PointerEvent>(predictedEvents);
    }

    /**
     * Returns <code>pointerId</code>.
     * <p>
     * <code>pointerId</code> 를 반환한다.
     *
     * @return the id / ID
     */
    public int getPointerId() {
        return pointerId;
    }

    /**
     * Returns <code>width</code>.
     * <p>
     * <code>width</code> 를 반환한다.
     *
     * @return the width / 너비
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns <code>height</code>.
     * <p>
     * <code>height</code> 를 반환한다.
     *
     * @return the height / 높이
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns <code>pressure</code>.
     * <p>
     * <code>pressure</code> 를 반환한다.
     *
     * @return the pressure / 압력
     */
    public double getPressure() {
        return pressure;
    }

    /**
     * Returns <code>tangentialPressure</code>.
     * <p>
     * <code>tangentialPressure</code> 를 반환한다.
     *
     * @return the tangential pressure / 접선 압력
     */
    public double getTangentialPressure() {
        return tangentialPressure;
    }

    /**
     * Returns <code>tiltX</code>.
     * <p>
     * <code>tiltX</code> 를 반환한다.
     *
     * @return the tilt / 기울기
     */
    public int getTiltX() {
        return tiltX;
    }

    /**
     * Returns <code>tiltY</code>.
     * <p>
     * <code>tiltY</code> 를 반환한다.
     *
     * @return the tilt / 기울기
     */
    public int getTiltY() {
        return tiltY;
    }

    /**
     * Returns <code>twist</code>.
     * <p>
     * <code>twist</code> 를 반환한다.
     *
     * @return the twist / 비틀림
     */
    public int getTwist() {
        return twist;
    }

    /**
     * Returns <code>altitudeAngle</code>.
     * <p>
     * <code>altitudeAngle</code> 를 반환한다.
     *
     * @return the angle in radians / 라디안 각도
     */
    public double getAltitudeAngle() {
        return altitudeAngle;
    }

    /**
     * Returns <code>azimuthAngle</code>.
     * <p>
     * <code>azimuthAngle</code> 를 반환한다.
     *
     * @return the angle in radians / 라디안 각도
     */
    public double getAzimuthAngle() {
        return azimuthAngle;
    }

    /**
     * Returns <code>pointerType</code>.
     * <p>
     * <code>pointerType</code> 을 반환한다.
     *
     * @return the type / 종류
     */
    public String getPointerType() {
        return pointerType;
    }

    /**
     * Returns <code>isPrimary</code>.
     * <p>
     * <code>isPrimary</code> 를 반환한다.
     *
     * @return true for the primary pointer / 주 포인터이면 true
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Returns a copy of the coalesced events (<code>getCoalescedEvents()</code>).
     * <p>
     * 합쳐진 이벤트의 복사본을 반환한다(<code>getCoalescedEvents()</code>).
     *
     * @return the events / 이벤트
     */
    public List<PointerEvent> getCoalescedEvents() {
        return new ArrayList<PointerEvent>(coalesced);
    }

    /**
     * Returns a copy of the predicted events (<code>getPredictedEvents()</code>).
     * <p>
     * 예측 이벤트의 복사본을 반환한다(<code>getPredictedEvents()</code>).
     *
     * @return the events / 이벤트
     */
    public List<PointerEvent> getPredictedEvents() {
        return new ArrayList<PointerEvent>(predicted);
    }
}
