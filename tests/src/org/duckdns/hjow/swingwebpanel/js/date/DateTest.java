package org.duckdns.hjow.swingwebpanel.js.date;

import java.util.Random;
import java.util.TimeZone;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link DateMath}, {@link DateParser} and {@link DateFormatter} with fixed time zones
 * (UTC, Asia/Seoul and America/New_York) so that results are deterministic.
 * <p>
 * 고정 시간대(UTC, Asia/Seoul, America/New_York)로 결과가 결정되도록 한 {@link DateMath}, {@link DateParser},
 * {@link DateFormatter} 의 시험.
 */
public class DateTest {

    /**
     * UTC time zone.
     * <p>
     * UTC 시간대.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Seoul time zone (UTC+9, no daylight saving time).
     * <p>
     * 서울 시간대(UTC+9, 일광 절약 시간 없음).
     */
    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    /**
     * New York time zone (daylight saving time).
     * <p>
     * 뉴욕 시간대(일광 절약 시간 있음).
     */
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DateTest() {
    }

    /**
     * Builds a UTC time value from calendar fields (month is 1-based here).
     * <p>
     * 달력 필드로 UTC 시간값을 만든다(여기서 월은 1 기준).
     *
     * @param y year / 연
     * @param mo month 1..12 / 월 1..12
     * @param d day / 일
     * @param h hour / 시
     * @param mi minute / 분
     * @param s second / 초
     * @param ms millisecond / 밀리초
     * @return time value / 시간값
     */
    private static double t(int y, int mo, int d, int h, int mi, int s, int ms) {
        return DateMath.makeDate(DateMath.makeDay(y, mo - 1, d), DateMath.makeTime(h, mi, s, ms));
    }

    /**
     * Checks calendar field extraction around well known instants.
     * <p>
     * 잘 알려진 시각 주변에서 달력 필드 추출을 확인한다.
     */
    public void testFields() {
        Check.equal(1970.0, Double.valueOf(DateMath.yearFromTime(0)), "year of epoch");
        Check.equal(0.0, Double.valueOf(DateMath.monthFromTime(0)), "month of epoch");
        Check.equal(1.0, Double.valueOf(DateMath.dateFromTime(0)), "date of epoch");
        Check.equal(4.0, Double.valueOf(DateMath.weekDay(0)), "epoch is a Thursday");
        double mar1 = t(2016, 3, 1, 0, 0, 0, 0);
        Check.equal(1456790400000.0, Double.valueOf(mar1), "2016-03-01T00:00:00Z");
        Check.equal(2.0, Double.valueOf(DateMath.weekDay(mar1)), "2016-03-01 is a Tuesday");
        Check.equal(2016.0, Double.valueOf(DateMath.yearFromTime(mar1)), "year");
        Check.equal(2.0, Double.valueOf(DateMath.monthFromTime(mar1)), "month");
        Check.equal(1.0, Double.valueOf(DateMath.dateFromTime(mar1)), "date");
        Check.equal(29.0, Double.valueOf(DateMath.dateFromTime(mar1 - 1)), "the last millisecond of February 2016 is the 29th");
        Check.equal(1.0, Double.valueOf(DateMath.monthFromTime(mar1 - 1)), "and still February");
        Check.equal(23.0, Double.valueOf(DateMath.hourFromTime(mar1 - 1)), "hour");
        Check.equal(59.0, Double.valueOf(DateMath.minFromTime(mar1 - 1)), "minute");
        Check.equal(59.0, Double.valueOf(DateMath.secFromTime(mar1 - 1)), "second");
        Check.equal(999.0, Double.valueOf(DateMath.msFromTime(mar1 - 1)), "millisecond");
        Check.equal(366.0, Double.valueOf(DateMath.daysInYear(2000)), "2000 is a leap year");
        Check.equal(365.0, Double.valueOf(DateMath.daysInYear(2100)), "2100 is not a leap year");
        Check.equal(366.0, Double.valueOf(DateMath.daysInYear(2016)), "2016 is a leap year");
        Check.equal(365.0, Double.valueOf(DateMath.daysInYear(1900)), "1900 is not a leap year");
        Check.equal(275760.0, Double.valueOf(DateMath.yearFromTime(8.64e15)), "max year");
        Check.equal(8.0, Double.valueOf(DateMath.monthFromTime(8.64e15)), "max month (September)");
        Check.equal(13.0, Double.valueOf(DateMath.dateFromTime(8.64e15)), "max date");
        Check.equal(-271821.0, Double.valueOf(DateMath.yearFromTime(-8.64e15)), "min year");
        Check.equal(3.0, Double.valueOf(DateMath.monthFromTime(-8.64e15)), "min month (April)");
        Check.equal(20.0, Double.valueOf(DateMath.dateFromTime(-8.64e15)), "min date");
        Check.equal(23.0, Double.valueOf(DateMath.hourFromTime(-1)), "negative time values still give hours 0..23");
        Check.equal(1969.0, Double.valueOf(DateMath.yearFromTime(-1)), "year just before the epoch");
        Check.isTrue(Double.isNaN(DateMath.yearFromTime(Double.NaN)), "NaN year");
    }

    /**
     * Checks MakeTime, MakeDay, MakeDate and TimeClip.
     * <p>
     * MakeTime, MakeDay, MakeDate, TimeClip 를 확인한다.
     */
    public void testMakeFunctions() {
        Check.equal(0.0, Double.valueOf(DateMath.makeDay(1970, 0, 1)), "epoch day");
        Check.equal(365.0, Double.valueOf(DateMath.makeDay(1970, 12, 1)), "month overflow moves to the next year");
        Check.equal(DateMath.makeDay(2015, 11, 1), Double.valueOf(DateMath.makeDay(2016, -1, 1)), "negative month");
        Check.equal(DateMath.makeDay(2016, 1, 1), Double.valueOf(DateMath.makeDay(2016, 0, 32)), "date overflow");
        Check.equal(DateMath.makeDay(2016, 1, 29) + 1, Double.valueOf(DateMath.makeDay(2016, 2, 1)), "leap day");
        Check.isTrue(Double.isNaN(DateMath.makeDay(Double.NaN, 0, 1)), "NaN year");
        Check.isTrue(Double.isNaN(DateMath.makeDay(1e20, 0, 1)), "huge year");
        Check.equal(3723004.0, Double.valueOf(DateMath.makeTime(1, 2, 3, 4)), "MakeTime");
        Check.equal(3723004.0, Double.valueOf(DateMath.makeTime(1.9, 2.9, 3.9, 4.9)), "MakeTime truncates");
        Check.isTrue(Double.isNaN(DateMath.makeTime(1, Double.POSITIVE_INFINITY, 0, 0)), "infinite minute");
        Check.isTrue(Double.isNaN(DateMath.makeDate(Double.NaN, 0)), "MakeDate NaN");
        Check.equal(8.64e15, Double.valueOf(DateMath.timeClip(8.64e15)), "max kept");
        Check.isTrue(Double.isNaN(DateMath.timeClip(8.64e15 + 1)), "above max");
        Check.isTrue(Double.isNaN(DateMath.timeClip(-8.64e15 - 1)), "below min");
        Check.equal(Double.POSITIVE_INFINITY, Double.valueOf(1.0 / DateMath.timeClip(-0.0)), "-0 becomes +0");
        Check.equal(1.0, Double.valueOf(DateMath.timeClip(1.9)), "truncation");
        Check.equal(-1.0, Double.valueOf(DateMath.timeClip(-1.9)), "truncation of negatives");
        Check.isTrue(Double.isNaN(DateMath.timeClip(Double.NaN)), "NaN");
        Check.isTrue(Double.isNaN(DateMath.timeClip(Double.POSITIVE_INFINITY)), "Infinity");
    }

    /**
     * Checks local time conversion in zones with fixed and changing offsets.
     * <p>
     * 오프셋이 고정된/바뀌는 시간대에서 지역 시간 변환을 확인한다.
     */
    public void testLocalTime() {
        Check.equal(32400000.0, Double.valueOf(DateMath.localTZA(0, true, SEOUL)), "Seoul offset in 1970");
        Check.equal(32400000.0, Double.valueOf(DateMath.localTime(0, SEOUL)), "Seoul local time of the epoch");
        Check.equal(-32400000.0, Double.valueOf(DateMath.utc(0, SEOUL)), "local epoch in Seoul");
        Check.equal(0.0, Double.valueOf(DateMath.localTZA(1e12, true, UTC)), "UTC offset");
        double spring = t(2016, 3, 13, 7, 0, 0, 0);
        Check.equal(-5 * 3600000.0, Double.valueOf(DateMath.localTZA(spring - 1, true, NEW_YORK)), "EST before the spring transition");
        Check.equal(-4 * 3600000.0, Double.valueOf(DateMath.localTZA(spring, true, NEW_YORK)), "EDT after the spring transition");
        // 건너뛰는 지역 시간(02:30)은 전환 이전 오프셋(EST)으로 해석한다.
        Check.equal(t(2016, 3, 13, 7, 30, 0, 0), Double.valueOf(DateMath.utc(t(2016, 3, 13, 2, 30, 0, 0), NEW_YORK)), "skipped local time uses the old offset");
        Check.equal(t(2016, 3, 13, 6, 59, 59, 999), Double.valueOf(DateMath.utc(t(2016, 3, 13, 1, 59, 59, 999), NEW_YORK)), "just before the gap");
        Check.equal(t(2016, 3, 13, 7, 0, 0, 0), Double.valueOf(DateMath.utc(t(2016, 3, 13, 3, 0, 0, 0), NEW_YORK)), "just after the gap");
        // 반복되는 지역 시간(01:30)은 전환 이전 오프셋(EDT)으로 해석한다.
        Check.equal(t(2016, 11, 6, 5, 30, 0, 0), Double.valueOf(DateMath.utc(t(2016, 11, 6, 1, 30, 0, 0), NEW_YORK)), "repeated local time uses the old offset");
        Check.equal(t(2016, 11, 6, 4, 59, 0, 0), Double.valueOf(DateMath.utc(t(2016, 11, 6, 0, 59, 0, 0), NEW_YORK)), "before the repeated hour");
        Check.equal(t(2016, 11, 6, 7, 0, 0, 0), Double.valueOf(DateMath.utc(t(2016, 11, 6, 2, 0, 0, 0), NEW_YORK)), "after the repeated hour");
        Check.equal(t(2016, 7, 1, 8, 0, 0, 0), Double.valueOf(DateMath.utc(t(2016, 7, 1, 4, 0, 0, 0), NEW_YORK)), "ordinary summer time");
        Check.isTrue(!Double.isNaN(DateMath.localTime(8.64e15, NEW_YORK)), "local time at the maximum does not fail");
        Check.isTrue(!Double.isNaN(DateMath.utc(-8.64e15, NEW_YORK)), "UTC at the minimum does not fail");
        Check.isTrue(Double.isNaN(DateMath.localTime(Double.NaN, NEW_YORK)), "NaN stays NaN");
    }

    /**
     * Checks the ISO date time string parser.
     * <p>
     * ISO 날짜 시간 문자열 해석기를 확인한다.
     */
    public void testParseIso() {
        double mar1 = 1456790400000.0;
        Check.equal(mar1, Double.valueOf(DateParser.parse("2016-03-01", NEW_YORK)), "date only is UTC even in New York");
        Check.equal(mar1, Double.valueOf(DateParser.parse("2016-03-01T00:00:00Z", NEW_YORK)), "with Z");
        Check.equal(mar1, Double.valueOf(DateParser.parse("2016-03-01T09:00:00+09:00", UTC)), "with offset");
        Check.equal(mar1 + 123, Double.valueOf(DateParser.parse("2016-03-01T00:00:00.123Z", UTC)), "milliseconds");
        Check.equal(mar1 + 120, Double.valueOf(DateParser.parse("2016-03-01T00:00:00.12Z", UTC)), "two fraction digits");
        Check.equal(mar1 + 123, Double.valueOf(DateParser.parse("2016-03-01T00:00:00.1239Z", UTC)), "extra fraction digits are truncated");
        Check.equal(mar1, Double.valueOf(DateParser.parse("2016-03-01T00:00", UTC)), "date time without zone in UTC zone");
        Check.equal(mar1 - 9 * 3600000.0, Double.valueOf(DateParser.parse("2016-03-01T00:00", SEOUL)), "date time without zone is local");
        Check.equal(mar1 - 9 * 3600000.0, Double.valueOf(DateParser.parse("2016-03-01T00:00:00", SEOUL)), "with seconds");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("2016-03", UTC)), "year and month");
        Check.equal(t(2016, 1, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("2016", UTC)), "year only");
        Check.equal(t(2016, 1, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("+002016-01-01T00:00:00Z", UTC)), "extended year");
        Check.equal(t(-1, 1, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("-000001-01-01T00:00:00Z", UTC)), "negative year");
        Check.isTrue(Double.isNaN(DateParser.parse("-000000-01-01T00:00:00Z", UTC)), "minus zero year is invalid");
        Check.equal(mar1 + 86400000.0, Double.valueOf(DateParser.parse("2016-03-01T24:00:00Z", UTC)), "24:00 is the end of the day");
        Check.isTrue(Double.isNaN(DateParser.parse("2016-03-01T24:00:01Z", UTC)), "24:00:01");
        Check.isTrue(Double.isNaN(DateParser.parse("2016-02-30", UTC)), "February 30");
        Check.isTrue(Double.isNaN(DateParser.parse("2015-02-29", UTC)), "February 29 in a common year");
        Check.equal(t(2016, 2, 29, 0, 0, 0, 0), Double.valueOf(DateParser.parse("2016-02-29", UTC)), "February 29 in a leap year");
        Check.isTrue(Double.isNaN(DateParser.parse("2016-13-01", UTC)), "month 13");
        Check.isTrue(Double.isNaN(DateParser.parse("2016-03-01T25:00Z", UTC)), "hour 25");
        Check.isTrue(Double.isNaN(DateParser.parse("2016-03-01T10:60Z", UTC)), "minute 60");
        Check.equal(8.64e15, Double.valueOf(DateParser.parse("+275760-09-13T00:00:00.000Z", UTC)), "maximum time value");
        Check.isTrue(Double.isNaN(DateParser.parse("+275760-09-13T00:00:00.001Z", UTC)), "above the maximum");
        Check.equal(-8.64e15, Double.valueOf(DateParser.parse("-271821-04-20T00:00:00.000Z", UTC)), "minimum time value");
    }

    /**
     * Checks the fallback formats.
     * <p>
     * 대체 형식을 확인한다.
     */
    public void testParseFallback() {
        double t0 = t(2016, 3, 1, 1, 20, 30, 0);
        Check.equal(t0, Double.valueOf(DateParser.parse("Tue Mar 01 2016 10:20:30 GMT+0900 (KST)", NEW_YORK)), "toString output");
        Check.equal(t(2016, 3, 1, 10, 20, 30, 0), Double.valueOf(DateParser.parse("Tue, 01 Mar 2016 10:20:30 GMT", NEW_YORK)), "toUTCString output");
        Check.equal(t0, Double.valueOf(DateParser.parse("Tue, 01 Mar 2016 10:20:30 +0900", NEW_YORK)), "RFC 2822");
        Check.equal(t0, Double.valueOf(DateParser.parse("Tue, 01 Mar 2016 10:20:30 +09:00", NEW_YORK)), "offset with colon");
        Check.equal(t(2016, 3, 1, 22, 20, 0, 0), Double.valueOf(DateParser.parse("March 1, 2016 10:20 PM", UTC)), "12 hour clock PM");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("Mar 1 2016 12:00 AM", UTC)), "12 AM is midnight");
        Check.equal(t(2016, 3, 1, 12, 0, 0, 0), Double.valueOf(DateParser.parse("Mar 1 2016 12:00 PM", UTC)), "12 PM is noon");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("1 Mar 2016", UTC)), "day month year");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("01-Mar-2016", UTC)), "dashes with month name");
        Check.equal(t(2016, 3, 1, 10, 20, 30, 0), Double.valueOf(DateParser.parse("2016/03/01 10:20:30", UTC)), "slashes year first");
        Check.equal(t(2016, 3, 1, 10, 20, 30, 0), Double.valueOf(DateParser.parse("2016-03-01 10:20:30", UTC)), "space separated ISO");
        Check.equal(t(2016, 3, 1, 10, 20, 30, 0) - 9 * 3600000.0, Double.valueOf(DateParser.parse("2016-03-01 10:20:30", SEOUL)), "local time in Seoul");
        Check.equal(t(2016, 3, 1, 15, 20, 30, 0), Double.valueOf(DateParser.parse("2016-03-01 10:20:30-0500", UTC)), "ISO style with minus zone");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("03/01/2016", UTC)), "US month first");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("3/1/16", UTC)), "two digit year");
        Check.equal(t(1999, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("3/1/99", UTC)), "two digit year 99");
        Check.equal(t(2016, 3, 1, 5, 0, 0, 0), Double.valueOf(DateParser.parse("Mar 1, 2016 EST", SEOUL)), "named zone");
        Check.equal(t(2016, 3, 1, 10, 20, 30, 500), Double.valueOf(DateParser.parse("Mar 1, 2016 10:20:30.5 UTC", NEW_YORK)), "fraction");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("  mar 1, 2016 gmt  ", NEW_YORK)), "case and spaces");
        Check.equal(t(2016, 3, 1, 0, 0, 0, 0), Double.valueOf(DateParser.parse("March 1st, 2016 UTC", NEW_YORK)), "ordinal");
        Check.isTrue(Double.isNaN(DateParser.parse("Mar 1", UTC)), "missing year");
        Check.isTrue(Double.isNaN(DateParser.parse("garbage", UTC)), "garbage");
        Check.isTrue(Double.isNaN(DateParser.parse("", UTC)), "empty");
        Check.isTrue(Double.isNaN(DateParser.parse(null, UTC)), "null");
        Check.isTrue(Double.isNaN(DateParser.parse("13/01/2016", UTC)), "month 13");
        Check.isTrue(Double.isNaN(DateParser.parse("Feb 30 2016", UTC)), "February 30");
        Check.isTrue(Double.isNaN(DateParser.parse("Mar 1 2016 13:00 PM", UTC)), "hour 13 with PM");
        Check.isTrue(Double.isNaN(DateParser.parse("Mar 1 2016 10:70", UTC)), "minute 70");
    }

    /**
     * Checks the formatting functions.
     * <p>
     * 서식 함수를 확인한다.
     */
    public void testFormat() {
        Check.equal("1970-01-01T00:00:00.000Z", DateFormatter.toISOString(0), "ISO of epoch");
        Check.equal("Thu, 01 Jan 1970 00:00:00 GMT", DateFormatter.toUTCString(0), "UTC string of epoch");
        Check.equal("Thu Jan 01 1970 00:00:00 GMT+0000 (UTC)", DateFormatter.toString(0, UTC), "toString in UTC");
        Check.equal("Thu Jan 01 1970 09:00:00 GMT+0900 (KST)", DateFormatter.toString(0, SEOUL), "toString in Seoul");
        Check.equal("Wed Dec 31 1969 19:00:00 GMT-0500 (EST)", DateFormatter.toString(0, NEW_YORK), "toString in New York");
        Check.equal("Thu Jan 01 1970", DateFormatter.toDateString(0, UTC), "toDateString");
        Check.equal("00:00:00 GMT+0000 (UTC)", DateFormatter.toTimeString(0, UTC), "toTimeString");
        Check.equal("Invalid Date", DateFormatter.toString(Double.NaN, UTC), "invalid date");
        Check.equal("Invalid Date", DateFormatter.toUTCString(Double.NaN), "invalid UTC string");
        Check.equal("+275760-09-13T00:00:00.000Z", DateFormatter.toISOString(8.64e15), "ISO max");
        Check.equal("-271821-04-20T00:00:00.000Z", DateFormatter.toISOString(-8.64e15), "ISO min");
        Check.equal("0000-01-01T00:00:00.000Z", DateFormatter.toISOString(DateMath.timeFromYear(0)), "year 0");
        Check.equal("-000001-01-01T00:00:00.000Z", DateFormatter.toISOString(DateMath.timeFromYear(-1)), "year -1");
        Check.equal("9999-12-31T23:59:59.999Z", DateFormatter.toISOString(DateMath.timeFromYear(10000) - 1), "year 9999");
        Check.equal("+010000-01-01T00:00:00.000Z", DateFormatter.toISOString(DateMath.timeFromYear(10000)), "year 10000");
        Check.contains(DateFormatter.toDateString(DateMath.timeFromYear(-1), UTC), " Jan 01 -0001", "negative year uses a sign");
        Check.equal("1/1/1970, 12:00:00 AM", DateFormatter.toLocaleString(0, UTC), "locale string");
        Check.equal("1/1/1970", DateFormatter.toLocaleDateString(0, UTC), "locale date");
        Check.equal("12:00:00 AM", DateFormatter.toLocaleTimeString(0, UTC), "locale time midnight");
        Check.equal("12:30:00 PM", DateFormatter.toLocaleTimeString(12.5 * 3600000.0, UTC), "locale time noon");
        Check.equal("1:05:09 PM", DateFormatter.toLocaleTimeString(13 * 3600000.0 + 5 * 60000.0 + 9000, UTC), "locale time afternoon");
        Check.isNull(DateFormatter.toJsonString(Double.NaN), "toJSON of an invalid date is null");
        Check.equal("1970-01-01T00:00:00.000Z", DateFormatter.toJsonString(0), "toJSON");
        Check.throwsException(DateRangeException.class, new Block() {
            /**
             * toISOString of an invalid date is a RangeError.
             * <p>
             * 올바르지 않은 날짜의 toISOString 은 RangeError 이다.
             */
            public void run() {
                DateFormatter.toISOString(Double.NaN);
            }
        }, "toISOString(NaN)");
    }

    /**
     * Randomized round trips through the formatters and the parser.
     * <p>
     * 서식 함수와 해석기를 거치는 무작위 왕복 시험.
     */
    public void testRoundTrips() {
        Random rnd = new Random(99L);
        TimeZone[] zones = { UTC, SEOUL, NEW_YORK };
        // 1900년에서 2100년 사이의 무작위 시각으로 세 형식의 왕복을 확인한다.
        for (int i = 0; i < 1500; i++) {
            double tv = Math.floor((rnd.nextDouble() * 200.0 - 70.0) * 365.2425 * 86400000.0);
            double sec = Math.floor(tv / 1000.0) * 1000.0;
            Check.equal(tv, Double.valueOf(DateParser.parse(DateFormatter.toISOString(tv), UTC)), "ISO round trip " + tv);
            Check.equal(sec, Double.valueOf(DateParser.parse(DateFormatter.toUTCString(tv), UTC)), "UTC string round trip " + tv);
            TimeZone zone = zones[i % zones.length];
            Check.equal(sec, Double.valueOf(DateParser.parse(DateFormatter.toString(tv, zone), zone)), "toString round trip " + tv + " " + zone.getID());
        }
    }

    /**
     * Checks that field extraction agrees with java.util.GregorianCalendar for random times.
     * <p>
     * 무작위 시각에서 필드 추출이 java.util.GregorianCalendar 와 일치하는지 확인한다.
     */
    public void testFieldsAgainstCalendar() {
        Random rnd = new Random(5L);
        // 1600년에서 2400년 사이의 무작위 시각을 달력과 비교한다.
        for (int i = 0; i < 3000; i++) {
            double tv = Math.floor((rnd.nextDouble() * 800.0 - 370.0) * 365.2425 * 86400000.0);
            java.util.GregorianCalendar cal = new java.util.GregorianCalendar(UTC);
            cal.setGregorianChange(new java.util.Date(Long.MIN_VALUE));
            cal.setTimeInMillis((long) tv);
            Check.equal(cal.get(java.util.Calendar.YEAR), (long) DateMath.yearFromTime(tv), "year of " + tv);
            Check.equal(cal.get(java.util.Calendar.MONTH), (long) DateMath.monthFromTime(tv), "month of " + tv);
            Check.equal(cal.get(java.util.Calendar.DAY_OF_MONTH), (long) DateMath.dateFromTime(tv), "date of " + tv);
            Check.equal(cal.get(java.util.Calendar.DAY_OF_WEEK) - 1, (long) DateMath.weekDay(tv), "weekday of " + tv);
            Check.equal(cal.get(java.util.Calendar.HOUR_OF_DAY), (long) DateMath.hourFromTime(tv), "hour of " + tv);
        }
    }
}
