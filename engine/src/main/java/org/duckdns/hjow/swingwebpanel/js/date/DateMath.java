package org.duckdns.hjow.swingwebpanel.js.date;

import java.util.TimeZone;

/**
 * ECMAScript 2015 date arithmetic (ECMA-262 section 20.3.1) on time values: a double holding milliseconds since
 * 1970-01-01T00:00:00Z, NaN for an invalid date, valid range +-8.64e15. Day numbers, year/month/day extraction,
 * MakeTime, MakeDay, MakeDate, TimeClip and the local time conversions LocalTime and UTC are provided.
 * <p>
 * Local time uses an injectable {@link TimeZone}. For a local time that occurs twice (clock set back) or not at all
 * (clock set forward) the offset in effect <em>before</em> the transition is used, as ES2015 requires. The lookup assumes
 * that at most one offset transition happens within two days around the requested instant. Years are the proleptic
 * Gregorian years of the specification, independent of the JRE calendar. All methods are static and thread-safe
 * (a {@link TimeZone} instance is read only).
 * <p>
 * 시간값(1970-01-01T00:00:00Z 이후 밀리초를 담은 double, 올바르지 않은 날짜는 NaN, 유효 범위 +-8.64e15) 위의 ECMAScript 2015 날짜 연산
 * (ECMA-262 20.3.1절). 날짜 번호, 연/월/일 추출, MakeTime, MakeDay, MakeDate, TimeClip, 지역 시간 변환 LocalTime 과 UTC 를 제공한다.
 * <p>
 * 지역 시간은 주입할 수 있는 {@link TimeZone} 을 쓴다. 두 번 나타나는 지역 시간(시계를 되돌림)이나 아예 없는 지역 시간(시계를 앞당김)은
 * ES2015 규칙대로 전환 <em>이전</em>의 오프셋을 쓴다. 조회는 요청한 시각 앞뒤 이틀 안에 오프셋 전환이 많아야 한 번이라고 가정한다.
 * 연도는 규격의 그레고리력 연도이며 JRE 달력과 무관하다. 모든 메서드는 정적이고 스레드 안전하다({@link TimeZone} 은 읽기만 한다).
 */
public final class DateMath {

    /**
     * Milliseconds per day.
     * <p>
     * 하루의 밀리초.
     */
    public static final double MS_PER_DAY = 86400000.0;

    /**
     * Milliseconds per hour.
     * <p>
     * 한 시간의 밀리초.
     */
    public static final double MS_PER_HOUR = 3600000.0;

    /**
     * Milliseconds per minute.
     * <p>
     * 일 분의 밀리초.
     */
    public static final double MS_PER_MINUTE = 60000.0;

    /**
     * Milliseconds per second.
     * <p>
     * 일 초의 밀리초.
     */
    public static final double MS_PER_SECOND = 1000.0;

    /**
     * Largest absolute time value: 100,000,000 days on either side of the epoch.
     * <p>
     * 시간값의 절댓값 최댓값: epoch 앞뒤 100,000,000 일.
     */
    public static final double MAX_TIME = 8.64e15;

    /**
     * Number of days before the first day of each month in a non-leap year.
     * <p>
     * 평년에서 각 달 1일 이전의 날수.
     */
    private static final int[] DAYS_BEFORE_MONTH = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DateMath() {
    }

    /**
     * ToInteger for finite doubles (truncation toward zero, never -0).
     * <p>
     * 유한 double 에 대한 ToInteger (0 쪽으로 버림, -0 없음).
     *
     * @param d finite or NaN value / 유한 값 또는 NaN
     * @return integral value / 정수 값
     */
    private static double toInteger(double d) {
        if (d != d) {
            return 0.0;
        }
        return (d < 0 ? Math.ceil(d) : Math.floor(d)) + 0.0;
    }

    /**
     * Tests that a value is neither NaN nor infinite.
     * <p>
     * 값이 NaN 도 무한대도 아닌지 검사한다.
     *
     * @param d value / 값
     * @return true if finite / 유한하면 true
     */
    private static boolean finite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }

    /**
     * Positive modulo (the result has the sign of the divisor).
     * <p>
     * 양의 나머지(결과는 나누는 수의 부호를 따른다).
     *
     * @param a dividend / 나뉘는 수
     * @param b positive divisor / 양의 나누는 수
     * @return value in [0, b) / [0, b) 의 값
     */
    private static double mod(double a, double b) {
        double r = a % b;
        if (r < 0) {
            r += b;
        }
        return r + 0.0;
    }

    /**
     * Day(t): the day number containing the time value.
     * <p>
     * Day(t): 시간값이 속한 날짜 번호.
     *
     * @param t time value / 시간값
     * @return day number / 날짜 번호
     */
    public static double day(double t) {
        return Math.floor(t / MS_PER_DAY);
    }

    /**
     * TimeWithinDay(t): milliseconds since the start of the day.
     * <p>
     * TimeWithinDay(t): 그날 0 시 이후의 밀리초.
     *
     * @param t time value / 시간값
     * @return value in [0, 86400000) / [0, 86400000) 의 값
     */
    public static double timeWithinDay(double t) {
        return mod(t, MS_PER_DAY);
    }

    /**
     * DaysInYear(y).
     * <p>
     * DaysInYear(y).
     *
     * @param y year / 연도
     * @return 365 or 366 / 365 또는 366
     */
    public static double daysInYear(double y) {
        if (y % 4 != 0) {
            return 365;
        }
        if (y % 100 != 0) {
            return 366;
        }
        if (y % 400 != 0) {
            return 365;
        }
        return 366;
    }

    /**
     * DayFromYear(y): day number of January 1 of the year.
     * <p>
     * DayFromYear(y): 그 해 1월 1일의 날짜 번호.
     *
     * @param y year / 연도
     * @return day number / 날짜 번호
     */
    public static double dayFromYear(double y) {
        return 365 * (y - 1970) + Math.floor((y - 1969) / 4) - Math.floor((y - 1901) / 100) + Math.floor((y - 1601) / 400);
    }

    /**
     * TimeFromYear(y): time value of the start of the year.
     * <p>
     * TimeFromYear(y): 그 해 시작의 시간값.
     *
     * @param y year / 연도
     * @return time value / 시간값
     */
    public static double timeFromYear(double y) {
        return MS_PER_DAY * dayFromYear(y);
    }

    /**
     * YearFromTime(t).
     * <p>
     * YearFromTime(t).
     *
     * @param t time value / 시간값
     * @return year / 연도
     */
    public static double yearFromTime(double t) {
        if (!finite(t)) {
            return Double.NaN;
        }
        double y = Math.floor(day(t) / 365.2425) + 1970;
        // 추정한 연도가 어긋나면 한 해씩 보정한다.
        while (timeFromYear(y) > t) {
            y--;
        }
        // 다음 해 시작이 t 이하이면 연도를 한 해 올린다.
        while (timeFromYear(y + 1) <= t) {
            y++;
        }
        return y;
    }

    /**
     * InLeapYear(t).
     * <p>
     * InLeapYear(t).
     *
     * @param t time value / 시간값
     * @return true if the year of t is a leap year / t 가 속한 해가 윤년이면 true
     */
    public static boolean inLeapYear(double t) {
        return daysInYear(yearFromTime(t)) == 366;
    }

    /**
     * Number of days from the start of the year to the first day of a month.
     * <p>
     * 그 해 시작부터 어떤 달 1일까지의 날수.
     *
     * @param month month 0..11 / 달 0..11
     * @param leap  leap year flag / 윤년 여부
     * @return day count / 날수
     */
    private static int daysBeforeMonth(int month, boolean leap) {
        return DAYS_BEFORE_MONTH[month] + (leap && month >= 2 ? 1 : 0);
    }

    /**
     * MonthFromTime(t): 0 for January to 11 for December, NaN for an invalid time value.
     * <p>
     * MonthFromTime(t): 1 월은 0, 12 월은 11. 올바르지 않은 시간값은 NaN.
     *
     * @param t time value / 시간값
     * @return month / 달
     */
    public static double monthFromTime(double t) {
        if (!finite(t)) {
            return Double.NaN;
        }
        double y = yearFromTime(t);
        int dayInYear = (int) (day(t) - dayFromYear(y));
        boolean leap = daysInYear(y) == 366;
        // 12 월부터 거꾸로 내려오며 시작일이 하루 번호 이하인 첫 달을 찾는다.
        for (int m = 11; m >= 0; m--) {
            if (dayInYear >= daysBeforeMonth(m, leap)) {
                return m;
            }
        }
        return 0;
    }

    /**
     * DateFromTime(t): day of the month 1..31, NaN for an invalid time value.
     * <p>
     * DateFromTime(t): 달의 날짜 1..31. 올바르지 않은 시간값은 NaN.
     *
     * @param t time value / 시간값
     * @return day of month / 날짜
     */
    public static double dateFromTime(double t) {
        if (!finite(t)) {
            return Double.NaN;
        }
        double y = yearFromTime(t);
        int dayInYear = (int) (day(t) - dayFromYear(y));
        int m = (int) monthFromTime(t);
        return dayInYear - daysBeforeMonth(m, daysInYear(y) == 366) + 1;
    }

    /**
     * WeekDay(t): 0 for Sunday to 6 for Saturday.
     * <p>
     * WeekDay(t): 일요일은 0, 토요일은 6.
     *
     * @param t time value / 시간값
     * @return weekday / 요일
     */
    public static double weekDay(double t) {
        return mod(day(t) + 4, 7);
    }

    /**
     * HourFromTime(t).
     * <p>
     * HourFromTime(t).
     *
     * @param t time value / 시간값
     * @return hour 0..23 / 시 0..23
     */
    public static double hourFromTime(double t) {
        return mod(Math.floor(t / MS_PER_HOUR), 24);
    }

    /**
     * MinFromTime(t).
     * <p>
     * MinFromTime(t).
     *
     * @param t time value / 시간값
     * @return minute 0..59 / 분 0..59
     */
    public static double minFromTime(double t) {
        return mod(Math.floor(t / MS_PER_MINUTE), 60);
    }

    /**
     * SecFromTime(t).
     * <p>
     * SecFromTime(t).
     *
     * @param t time value / 시간값
     * @return second 0..59 / 초 0..59
     */
    public static double secFromTime(double t) {
        return mod(Math.floor(t / MS_PER_SECOND), 60);
    }

    /**
     * msFromTime(t).
     * <p>
     * msFromTime(t).
     *
     * @param t time value / 시간값
     * @return millisecond 0..999 / 밀리초 0..999
     */
    public static double msFromTime(double t) {
        return mod(t, MS_PER_SECOND);
    }

    /**
     * MakeTime(hour, min, sec, ms): NaN if any argument is not finite.
     * <p>
     * MakeTime(hour, min, sec, ms): 인수 중 유한하지 않은 것이 있으면 NaN.
     *
     * @param hour hour / 시
     * @param min  minute / 분
     * @param sec  second / 초
     * @param ms   millisecond / 밀리초
     * @return milliseconds within a day (may be outside one day) / 하루 안의 밀리초(하루를 벗어날 수 있음)
     */
    public static double makeTime(double hour, double min, double sec, double ms) {
        if (!finite(hour) || !finite(min) || !finite(sec) || !finite(ms)) {
            return Double.NaN;
        }
        return toInteger(hour) * MS_PER_HOUR + toInteger(min) * MS_PER_MINUTE + toInteger(sec) * MS_PER_SECOND + toInteger(ms);
    }

    /**
     * MakeDay(year, month, date): the day number, NaN if an argument is not finite or the result is not representable.
     * <p>
     * MakeDay(year, month, date): 날짜 번호. 인수가 유한하지 않거나 결과를 나타낼 수 없으면 NaN.
     *
     * @param year  year / 연
     * @param month month 0-based (may be outside 0..11) / 달(0 기준, 0..11 밖일 수 있음)
     * @param date  day of month (1-based, may be outside the month) / 날짜(1 기준, 그 달을 벗어날 수 있음)
     * @return day number / 날짜 번호
     */
    public static double makeDay(double year, double month, double date) {
        if (!finite(year) || !finite(month) || !finite(date)) {
            return Double.NaN;
        }
        double y = toInteger(year);
        double m = toInteger(month);
        double dt = toInteger(date);
        double ym = y + Math.floor(m / 12);
        // 연도가 지나치게 크면 표현할 수 없으므로 NaN 으로 한다.
        if (Math.abs(ym) > 1000000) {
            return Double.NaN;
        }
        int mn = (int) mod(m, 12);
        double days = dayFromYear(ym) + daysBeforeMonth(mn, daysInYear(ym) == 366);
        return days + dt - 1;
    }

    /**
     * MakeDate(day, time).
     * <p>
     * MakeDate(day, time).
     *
     * @param day  day number / 날짜 번호
     * @param time milliseconds within the day / 하루 안의 밀리초
     * @return time value, or NaN if not finite / 시간값. 유한하지 않으면 NaN
     */
    public static double makeDate(double day, double time) {
        if (!finite(day) || !finite(time)) {
            return Double.NaN;
        }
        double tv = day * MS_PER_DAY + time;
        return finite(tv) ? tv : Double.NaN;
    }

    /**
     * TimeClip(time): NaN outside +-8.64e15, otherwise the integral value (+0 for -0).
     * <p>
     * TimeClip(time): +-8.64e15 밖이면 NaN, 아니면 정수 값(-0 은 +0).
     *
     * @param time time value / 시간값
     * @return clipped time value / 제한한 시간값
     */
    public static double timeClip(double time) {
        if (!finite(time) || Math.abs(time) > MAX_TIME) {
            return Double.NaN;
        }
        return toInteger(time);
    }

    /**
     * Offset of the time zone at a UTC instant in milliseconds, tolerant of out-of-range values.
     * <p>
     * UTC 시각에서의 시간대 오프셋(밀리초). 범위를 벗어난 값에도 안전하다.
     *
     * @param tz    time zone / 시간대
     * @param utcMs UTC milliseconds / UTC 밀리초
     * @return offset east of UTC in milliseconds / UTC 동쪽 오프셋(밀리초)
     */
    private static double offsetAt(TimeZone tz, double utcMs) {
        double clamped = Math.max(-MAX_TIME, Math.min(MAX_TIME, utcMs));
        try {
            return tz.getOffset((long) clamped);
        } catch (RuntimeException e) {
            return tz.getRawOffset();
        }
    }

    /**
     * LocalTZA(t, isUTC): the offset of local time from UTC in milliseconds.
     * If isUTC is true, t is a UTC time value; otherwise t is a local time value and the offset in effect before any
     * transition is used for repeated or skipped local times.
     * <p>
     * LocalTZA(t, isUTC): 지역 시간의 UTC 와의 차이(밀리초). isUTC 가 true 이면 t 는 UTC 시간값이고, 아니면 t 는 지역 시간값이며
     * 반복되거나 건너뛰는 지역 시간에는 전환 이전의 오프셋을 쓴다.
     *
     * @param t     time value / 시간값
     * @param isUtc whether t is a UTC time value / t 가 UTC 시간값인지 여부
     * @param tz    time zone / 시간대
     * @return offset east of UTC in milliseconds / UTC 동쪽 오프셋(밀리초)
     */
    public static double localTZA(double t, boolean isUtc, TimeZone tz) {
        if (!finite(t)) {
            return Double.NaN;
        }
        if (isUtc) {
            return offsetAt(tz, t);
        }
        double before = offsetAt(tz, t - MS_PER_DAY);
        double after = offsetAt(tz, t + MS_PER_DAY);
        if (before == after) {
            return before;
        }
        // 전환이 있는 경우 두 후보 오프셋 중 지역 시간과 일치하는 것을 찾는다.
        boolean beforeValid = offsetAt(tz, t - before) == before;
        boolean afterValid = offsetAt(tz, t - after) == after;
        if (beforeValid && !afterValid) {
            return before;
        }
        if (afterValid && !beforeValid) {
            return after;
        }
        // 둘 다 맞으면(반복되는 시간) 또는 둘 다 틀리면(건너뛴 시간) 전환 이전의 오프셋을 쓴다.
        return before;
    }

    /**
     * LocalTime(t): converts a UTC time value to local time.
     * <p>
     * LocalTime(t): UTC 시간값을 지역 시간으로 바꾼다.
     *
     * @param t  UTC time value / UTC 시간값
     * @param tz time zone / 시간대
     * @return local time value / 지역 시간값
     */
    public static double localTime(double t, TimeZone tz) {
        return t + localTZA(t, true, tz);
    }

    /**
     * UTC(t): converts a local time value to UTC.
     * <p>
     * UTC(t): 지역 시간값을 UTC 로 바꾼다.
     *
     * @param t  local time value / 지역 시간값
     * @param tz time zone / 시간대
     * @return UTC time value / UTC 시간값
     */
    public static double utc(double t, TimeZone tz) {
        return t - localTZA(t, false, tz);
    }
}
