package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.JsObject;

/**
 * A Date instance: an ordinary object with the internal slot [[DateValue]], a time value in milliseconds since the epoch (UTC) or NaN for
 * an invalid date.
 * <p>
 * Date 인스턴스: 내부 슬롯 [[DateValue]](UTC 기준 에포크 이후 밀리초 시간값, 잘못된 날짜는 NaN)를 가진 일반 객체.
 */
final class DateObject extends JsObject {

    /**
     * The time value.
     * <p>
     * 시간값.
     */
    private double time;

    /**
     * Creates a date object.
     * <p>
     * 날짜 객체를 만든다.
     *
     * @param proto the prototype / 프로토타입
     * @param time  the time value / 시간값
     */
    DateObject(JsObject proto, double time) {
        super(proto);
        this.time = time;
    }

    /**
     * Returns the time value.
     * <p>
     * 시간값을 반환한다.
     *
     * @return time value or NaN / 시간값 또는 NaN
     */
    double getTime() {
        return time;
    }

    /**
     * Replaces the time value.
     * <p>
     * 시간값을 바꾼다.
     *
     * @param value new time value / 새 시간값
     */
    void setTime(double value) {
        this.time = value;
    }

    /**
     * Returns the class name used by Object.prototype.toString before the toStringTag lookup.
     * <p>
     * toStringTag 조회 전에 Object.prototype.toString 이 쓰는 클래스 이름을 반환한다.
     *
     * @return "Date" / "Date"
     */
    public String className() {
        return "Date";
    }
}
