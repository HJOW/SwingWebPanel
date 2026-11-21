package org.duckdns.hjow.swingwebpanel.js.date;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Date to string conversions of <code>Date.prototype</code>: toString, toDateString, toTimeString, toUTCString,
 * toISOString, toJSON helper and English (en-US) fallbacks for toLocaleString, toLocaleDateString and toLocaleTimeString.
 * The time zone name in parentheses is implementation defined and comes from the {@link TimeZone} short display name.
 * Years are printed with at least four digits (a minus sign for negative years). All methods are static and
 * thread-safe.
 * <p>
 * <code>Date.prototype</code> 의 문자열 변환: toString, toDateString, toTimeString, toUTCString, toISOString, toJSON 도우미,
 * 그리고 toLocaleString, toLocaleDateString, toLocaleTimeString 의 영어(en-US) 대체 구현. 괄호 안의 시간대 이름은 구현 정의이며
 * {@link TimeZone} 의 짧은 표시 이름에서 얻는다. 연도는 4 자리 이상으로 쓰고 음수 연도는 마이너스 부호를 붙인다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class DateFormatter {

    /**
     * Text returned for an invalid date.
     * <p>
     * 올바르지 않은 날짜에 반환하는 문자열.
     */
    public static final String INVALID_DATE = "Invalid Date";

    /**
     * Abbreviated weekday names starting with Sunday.
     * <p>
     * 일요일부터 시작하는 요일 약어.
     */
    private static final String[] DAY_NAMES = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

    /**
     * Abbreviated month names starting with January.
     * <p>
     * 1 월부터 시작하는 달 약어.
     */
    private static final String[] MONTH_NAMES = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DateFormatter() {
    }

    /**
     * Pads a non-negative integer with zeros to a minimum width.
     * <p>
     * 음이 아닌 정수를 0 으로 채워 최소 너비를 맞춘다.
     *
     * @param v     value / 값
     * @param width minimum width / 최소 너비
     * @return padded text / 채운 문자열
     */
    private static String pad(double v, int width) {
        StringBuilder sb = new StringBuilder(String.valueOf((long) v));
        // 자릿수가 너비에 모자라는 동안 앞에 0 을 붙인다.
        while (sb.length() < width) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    /**
     * Formats a year for the toString family: at least four digits, minus sign for negative years.
     * <p>
     * toString 계열용 연도 문자열: 4 자리 이상, 음수 연도는 마이너스 부호.
     *
     * @param y year / 연도
     * @return year text / 연도 문자열
     */
    private static String year(double y) {
        return (y < 0 ? "-" : "") + pad(Math.abs(y), 4);
    }

    /**
     * Date.prototype.toString().
     * <p>
     * Date.prototype.toString().
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text such as "Tue Mar 01 2016 10:20:30 GMT+0900 (KST)" or "Invalid Date" / 예: "Tue Mar 01 2016 10:20:30 GMT+0900 (KST)" 또는 "Invalid Date"
     */
    public static String toString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        return toDateString(t, tz) + " " + toTimeString(t, tz);
    }

    /**
     * Date.prototype.toDateString().
     * <p>
     * Date.prototype.toDateString().
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text such as "Tue Mar 01 2016" or "Invalid Date" / 예: "Tue Mar 01 2016" 또는 "Invalid Date"
     */
    public static String toDateString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        double local = DateMath.localTime(t, tz);
        return DAY_NAMES[(int) DateMath.weekDay(local)] + " " + MONTH_NAMES[(int) DateMath.monthFromTime(local)] + " "
                + pad(DateMath.dateFromTime(local), 2) + " " + year(DateMath.yearFromTime(local));
    }

    /**
     * Date.prototype.toTimeString().
     * <p>
     * Date.prototype.toTimeString().
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text such as "10:20:30 GMT+0900 (KST)" or "Invalid Date" / 예: "10:20:30 GMT+0900 (KST)" 또는 "Invalid Date"
     */
    public static String toTimeString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        double local = DateMath.localTime(t, tz);
        double offsetMs = DateMath.localTZA(t, true, tz);
        long offsetMin = Math.abs((long) (offsetMs / DateMath.MS_PER_MINUTE));
        return clock(local) + " GMT" + (offsetMs < 0 ? "-" : "+") + pad(offsetMin / 60, 2) + pad(offsetMin % 60, 2) + " (" + zoneName(t, tz) + ")";
    }

    /**
     * Formats HH:mm:ss of a time value.
     * <p>
     * 시간값의 HH:mm:ss 를 만든다.
     *
     * @param t time value (already local if local time is wanted) / 시간값(지역 시간을 원하면 이미 지역 시간으로 바꾼 값)
     * @return clock text / 시계 문자열
     */
    private static String clock(double t) {
        return pad(DateMath.hourFromTime(t), 2) + ":" + pad(DateMath.minFromTime(t), 2) + ":" + pad(DateMath.secFromTime(t), 2);
    }

    /**
     * Returns the short time zone display name for the instant (implementation defined).
     * <p>
     * 그 시각의 짧은 시간대 표시 이름을 반환한다(구현 정의).
     *
     * @param t  UTC time value / UTC 시간값
     * @param tz time zone / 시간대
     * @return name such as "KST" / "KST" 같은 이름
     */
    private static String zoneName(double t, TimeZone tz) {
        try {
            boolean dst = tz.inDaylightTime(new java.util.Date((long) t));
            return tz.getDisplayName(dst, TimeZone.SHORT, Locale.ENGLISH);
        } catch (RuntimeException e) {
            return tz.getID();
        }
    }

    /**
     * Date.prototype.toUTCString().
     * <p>
     * Date.prototype.toUTCString().
     *
     * @param t time value / 시간값
     * @return text such as "Tue, 01 Mar 2016 10:20:30 GMT" or "Invalid Date" / 예: "Tue, 01 Mar 2016 10:20:30 GMT" 또는 "Invalid Date"
     */
    public static String toUTCString(double t) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        return DAY_NAMES[(int) DateMath.weekDay(t)] + ", " + pad(DateMath.dateFromTime(t), 2) + " "
                + MONTH_NAMES[(int) DateMath.monthFromTime(t)] + " " + year(DateMath.yearFromTime(t)) + " " + clock(t) + " GMT";
    }

    /**
     * Date.prototype.toISOString().
     * <p>
     * Date.prototype.toISOString().
     *
     * @param t time value / 시간값
     * @return text such as "2016-03-01T01:20:30.000Z"; years outside 0..9999 use a sign and six digits / 예: "2016-03-01T01:20:30.000Z". 0..9999 밖의 연도는 부호와 6 자리
     * @throws DateRangeException if t is NaN / t 가 NaN 이면
     */
    public static String toISOString(double t) {
        if (Double.isNaN(t)) {
            throw new DateRangeException("Invalid time value");
        }
        double y = DateMath.yearFromTime(t);
        String ys = (y >= 0 && y <= 9999) ? pad(y, 4) : ((y < 0 ? "-" : "+") + pad(Math.abs(y), 6));
        return ys + "-" + pad(DateMath.monthFromTime(t) + 1, 2) + "-" + pad(DateMath.dateFromTime(t), 2) + "T" + clock(t) + "."
                + pad(DateMath.msFromTime(t), 3) + "Z";
    }

    /**
     * The string part of Date.prototype.toJSON(): null (JSON null) for a non-finite time value, else toISOString.
     * <p>
     * Date.prototype.toJSON() 의 문자열 부분: 유한하지 않은 시간값이면 null(JSON null), 아니면 toISOString.
     *
     * @param t time value / 시간값
     * @return ISO text or null / ISO 문자열 또는 null
     */
    public static String toJsonString(double t) {
        if (Double.isNaN(t) || Double.isInfinite(t)) {
            return null;
        }
        return toISOString(t);
    }

    /**
     * Date.prototype.toLocaleDateString() with the English (en-US) fallback, for example "3/1/2016".
     * <p>
     * 영어(en-US) 대체 구현의 Date.prototype.toLocaleDateString(). 예: "3/1/2016".
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text or "Invalid Date" / 문자열 또는 "Invalid Date"
     */
    public static String toLocaleDateString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        double local = DateMath.localTime(t, tz);
        return ((long) DateMath.monthFromTime(local) + 1) + "/" + (long) DateMath.dateFromTime(local) + "/" + (long) DateMath.yearFromTime(local);
    }

    /**
     * Date.prototype.toLocaleTimeString() with the English (en-US) fallback, for example "10:20:30 AM".
     * <p>
     * 영어(en-US) 대체 구현의 Date.prototype.toLocaleTimeString(). 예: "10:20:30 AM".
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text or "Invalid Date" / 문자열 또는 "Invalid Date"
     */
    public static String toLocaleTimeString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        double local = DateMath.localTime(t, tz);
        int h = (int) DateMath.hourFromTime(local);
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + ":" + pad(DateMath.minFromTime(local), 2) + ":" + pad(DateMath.secFromTime(local), 2) + (h < 12 ? " AM" : " PM");
    }

    /**
     * Date.prototype.toLocaleString() with the English (en-US) fallback, for example "3/1/2016, 10:20:30 AM".
     * <p>
     * 영어(en-US) 대체 구현의 Date.prototype.toLocaleString(). 예: "3/1/2016, 10:20:30 AM".
     *
     * @param t  time value / 시간값
     * @param tz time zone / 시간대
     * @return text or "Invalid Date" / 문자열 또는 "Invalid Date"
     */
    public static String toLocaleString(double t, TimeZone tz) {
        if (Double.isNaN(t)) {
            return INVALID_DATE;
        }
        return toLocaleDateString(t, tz) + ", " + toLocaleTimeString(t, tz);
    }
}
