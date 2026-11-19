package org.duckdns.hjow.swingwebpanel.dom.html;

/**
 * The date and time microsyntaxes of form controls (HTML Standard, "dates and times"): strict parsing and shortest-form serialization of
 * date (<code>YYYY-MM-DD</code>), month (<code>YYYY-MM</code>), week (<code>YYYY-Www</code>), time (<code>HH:MM[:SS[.sss]]</code>) and local date and time
 * strings. All values are exact numbers: milliseconds since 1970-01-01T00:00 UTC (a month is counted in months since 1970-01). Years run from 1
 * to 275760, like the range of a JavaScript <code>Date</code>. The proleptic Gregorian calendar is used.
 * <p>
 * 폼 컨트롤의 날짜/시간 마이크로 구문(HTML 표준 "dates and times"): 날짜(<code>YYYY-MM-DD</code>), 월(<code>YYYY-MM</code>), 주(<code>YYYY-Www</code>),
 * 시간(<code>HH:MM[:SS[.sss]]</code>), 지역 날짜와 시간 문자열의 엄격한 해석과 가장 짧은 형식의 직렬화. 모든 값은 정확한 수이며 1970-01-01T00:00 UTC 이후의
 * 밀리초이다(월은 1970-01 이후의 개월 수). 연도는 JavaScript <code>Date</code> 의 범위처럼 1 ~ 275760 이고 그레고리력을 외삽해 쓴다.
 */
final class DateTimeSyntax {

    /**
     * Milliseconds in one day.
     * <p>
     * 하루의 밀리초.
     */
    static final long MS_PER_DAY = 86400000L;

    /**
     * Milliseconds in one week.
     * <p>
     * 한 주의 밀리초.
     */
    static final long MS_PER_WEEK = 7L * MS_PER_DAY;

    /**
     * The largest year that is accepted.
     * <p>
     * 허용하는 가장 큰 연도.
     */
    static final long MAX_YEAR = 275760L;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DateTimeSyntax() {
    }

    /**
     * Floor division.
     * <p>
     * 내림 나눗셈.
     *
     * @param a dividend / 피제수
     * @param b divisor, positive / 제수(양수)
     * @return floor(a / b) / floor(a / b)
     */
    static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && (a < 0)) ? q - 1 : q;
    }

    /**
     * Floor modulo (always 0 or more).
     * <p>
     * 내림 나머지(항상 0 이상).
     *
     * @param a dividend / 피제수
     * @param b divisor, positive / 제수(양수)
     * @return a - floor(a / b) * b / a - floor(a / b) * b
     */
    static long floorMod(long a, long b) {
        long m = a % b;
        return m < 0 ? m + b : m;
    }

    /**
     * Tells whether a year is a leap year of the proleptic Gregorian calendar.
     * <p>
     * 연도가 그레고리력의 윤년인지 알려 준다.
     *
     * @param y the year / 연도
     * @return true for leap years / 윤년이면 true
     */
    static boolean isLeap(long y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }

    /**
     * Returns the number of days of a month.
     * <p>
     * 월의 일수를 반환한다.
     *
     * @param y the year / 연도
     * @param m the month 1-12 / 월(1-12)
     * @return the days / 일수
     */
    static int daysInMonth(long y, int m) {
        switch (m) {
        case 2:
            return isLeap(y) ? 29 : 28;
        case 4:
        case 6:
        case 9:
        case 11:
            return 30;
        default:
            return 31;
        }
    }

    /**
     * Converts a civil date to days since 1970-01-01.
     * <p>
     * 달력 날짜를 1970-01-01 이후의 일수로 바꾼다.
     *
     * @param year  the year / 연도
     * @param month the month 1-12 / 월(1-12)
     * @param day   the day / 일
     * @return days since the epoch / 에포크 이후의 일수
     */
    static long daysFromCivil(long year, int month, int day) {
        long y = month <= 2 ? year - 1 : year;
        long era = floorDiv(y, 400);
        long yoe = y - era * 400;
        long mp = month > 2 ? month - 3 : month + 9;
        long doy = (153 * mp + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /**
     * Converts days since 1970-01-01 to a civil date.
     * <p>
     * 1970-01-01 이후의 일수를 달력 날짜로 바꾼다.
     *
     * @param days days since the epoch / 에포크 이후의 일수
     * @return {year, month, day} / {연도, 월, 일}
     */
    static long[] civilFromDays(long days) {
        long z = days + 719468;
        long era = floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new long[] {m <= 2 ? y + 1 : y, m, d};
    }

    /**
     * Returns the day of the week of a day count, Monday being 0.
     * <p>
     * 일수의 요일을 반환한다(월요일이 0).
     *
     * @param days days since the epoch / 에포크 이후의 일수
     * @return 0 (Monday) to 6 (Sunday) / 0(월)~6(일)
     */
    private static int weekday(long days) {
        return (int) floorMod(days + 3, 7);
    }

    /**
     * Returns the days since the epoch of the Monday that starts week 1 of a year (the week with the first Thursday).
     * <p>
     * 해의 1주가 시작하는 월요일(첫 목요일이 든 주)의 에포크 이후 일수를 반환한다.
     *
     * @param year the year / 연도
     * @return the day count / 일수
     */
    private static long week1Monday(long year) {
        long jan4 = daysFromCivil(year, 1, 4);
        return jan4 - weekday(jan4);
    }

    /**
     * Returns the number of weeks of a year: 53 if January 1 is a Thursday (or a Wednesday in a leap year), otherwise 52.
     * <p>
     * 해의 주 수를 반환한다: 1월 1일이 목요일(윤년은 수요일도)이면 53, 아니면 52.
     *
     * @param year the year / 연도
     * @return 52 or 53 / 52 또는 53
     */
    static int weeksInYear(long year) {
        int dow = weekday(daysFromCivil(year, 1, 1));
        return (dow == 3 || (isLeap(year) && dow == 2)) ? 53 : 52;
    }

    /**
     * Reads exactly two digits at a position.
     * <p>
     * 위치에서 정확히 두 자리 숫자를 읽는다.
     *
     * @param s   the text / 문자열
     * @param pos the position / 위치
     * @return the number, or -1 if there are not two digits / 숫자. 두 자리 숫자가 아니면 -1
     */
    private static int two(String s, int pos) {
        if (pos + 2 > s.length() || !HtmlText.isDigit(s.charAt(pos)) || !HtmlText.isDigit(s.charAt(pos + 1))) {
            return -1;
        }
        return (s.charAt(pos) - '0') * 10 + (s.charAt(pos + 1) - '0');
    }

    /**
     * Reads a year (four to six digits, 1 to {@link #MAX_YEAR}) at a position.
     * <p>
     * 위치에서 연도(4~6자리, 1~{@link #MAX_YEAR})를 읽는다.
     *
     * @param s   the text / 문자열
     * @param pos the position / 위치
     * @return {year, end position} or null / {연도, 끝 위치} 또는 null
     */
    private static long[] year(String s, int pos) {
        int i = pos;
        // 연속한 숫자를 읽는다.
        while (i < s.length() && HtmlText.isDigit(s.charAt(i))) {
            i++;
        }
        int len = i - pos;
        if (len < 4 || len > 6) {
            return null;
        }
        long y = Long.parseLong(s.substring(pos, i));
        return y < 1 || y > MAX_YEAR ? null : new long[] {y, i};
    }

    /**
     * Reads a date component <code>YYYY-MM-DD</code> at a position.
     * <p>
     * 위치에서 날짜 구성요소 <code>YYYY-MM-DD</code> 를 읽는다.
     *
     * @param s   the text / 문자열
     * @param pos the position / 위치
     * @return {days since the epoch, end position} or null / {에포크 이후 일수, 끝 위치} 또는 null
     */
    private static long[] dateComponent(String s, int pos) {
        long[] y = year(s, pos);
        if (y == null) {
            return null;
        }
        int i = (int) y[1];
        if (i >= s.length() || s.charAt(i) != '-') {
            return null;
        }
        int m = two(s, i + 1);
        if (m < 1 || m > 12 || i + 3 >= s.length() || s.charAt(i + 3) != '-') {
            return null;
        }
        int d = two(s, i + 4);
        if (d < 1 || d > daysInMonth(y[0], m)) {
            return null;
        }
        return new long[] {daysFromCivil(y[0], m, d), i + 6};
    }

    /**
     * Reads a time component <code>HH:MM[:SS[.fraction]]</code> at a position (the fraction has one to three digits).
     * <p>
     * 위치에서 시간 구성요소 <code>HH:MM[:SS[.fraction]]</code> 를 읽는다(소수부는 1~3자리).
     *
     * @param s   the text / 문자열
     * @param pos the position / 위치
     * @return {milliseconds of the day, end position} or null / {하루 중 밀리초, 끝 위치} 또는 null
     */
    private static long[] timeComponent(String s, int pos) {
        int h = two(s, pos);
        if (h < 0 || h > 23 || pos + 2 >= s.length() || s.charAt(pos + 2) != ':') {
            return null;
        }
        int mi = two(s, pos + 3);
        if (mi < 0 || mi > 59) {
            return null;
        }
        int i = pos + 5;
        long ms = (h * 60L + mi) * 60000L;
        if (i < s.length() && s.charAt(i) == ':') {
            int sec = two(s, i + 1);
            if (sec < 0 || sec > 59) {
                return null;
            }
            ms += sec * 1000L;
            i += 3;
            if (i < s.length() && s.charAt(i) == '.') {
                int f = i + 1;
                // 소수부의 숫자를 읽는다(최대 세 자리).
                while (f < s.length() && HtmlText.isDigit(s.charAt(f))) {
                    f++;
                }
                int len = f - (i + 1);
                if (len < 1 || len > 3) {
                    return null;
                }
                int frac = Integer.parseInt(s.substring(i + 1, f));
                // 한 자리나 두 자리 소수부는 밀리초 자릿수에 맞춘다.
                for (int k = len; k < 3; k++) {
                    frac *= 10;
                }
                ms += frac;
                i = f;
            }
        }
        return new long[] {ms, i};
    }

    /**
     * Parses a date string.
     * <p>
     * 날짜 문자열을 해석한다.
     *
     * @param s the text / 문자열
     * @return milliseconds at midnight UTC of the date, or null / 그 날짜 UTC 자정의 밀리초. 올바르지 않으면 null
     */
    static Long parseDate(String s) {
        long[] d = dateComponent(s, 0);
        return d == null || d[1] != s.length() ? null : Long.valueOf(d[0] * MS_PER_DAY);
    }

    /**
     * Serializes milliseconds as a date string.
     * <p>
     * 밀리초를 날짜 문자열로 직렬화한다.
     *
     * @param ms milliseconds since the epoch / 에포크 이후 밀리초
     * @return the text / 문자열
     */
    static String formatDate(long ms) {
        long[] c = civilFromDays(floorDiv(ms, MS_PER_DAY));
        return pad(c[0], 4) + "-" + pad(c[1], 2) + "-" + pad(c[2], 2);
    }

    /**
     * Parses a month string.
     * <p>
     * 월 문자열을 해석한다.
     *
     * @param s the text / 문자열
     * @return months since 1970-01, or null / 1970-01 이후의 개월 수. 올바르지 않으면 null
     */
    static Long parseMonth(String s) {
        long[] y = year(s, 0);
        if (y == null || y[1] + 3 != s.length() || s.charAt((int) y[1]) != '-') {
            return null;
        }
        int m = two(s, (int) y[1] + 1);
        return m < 1 || m > 12 ? null : Long.valueOf((y[0] - 1970) * 12 + (m - 1));
    }

    /**
     * Serializes a month count as a month string.
     * <p>
     * 개월 수를 월 문자열로 직렬화한다.
     *
     * @param months months since 1970-01 / 1970-01 이후의 개월 수
     * @return the text / 문자열
     */
    static String formatMonth(long months) {
        long y = 1970 + floorDiv(months, 12);
        long m = floorMod(months, 12) + 1;
        return pad(y, 4) + "-" + pad(m, 2);
    }

    /**
     * Parses a week string.
     * <p>
     * 주 문자열을 해석한다.
     *
     * @param s the text / 문자열
     * @return milliseconds at midnight UTC of the Monday of the week, or null / 그 주 월요일 UTC 자정의 밀리초. 올바르지 않으면 null
     */
    static Long parseWeek(String s) {
        long[] y = year(s, 0);
        if (y == null) {
            return null;
        }
        int i = (int) y[1];
        if (i + 4 != s.length() || s.charAt(i) != '-' || s.charAt(i + 1) != 'W') {
            return null;
        }
        int w = two(s, i + 2);
        if (w < 1 || w > weeksInYear(y[0])) {
            return null;
        }
        return Long.valueOf((week1Monday(y[0]) + (w - 1) * 7L) * MS_PER_DAY);
    }

    /**
     * Serializes milliseconds as the week string of the week that contains them.
     * <p>
     * 밀리초가 속한 주의 주 문자열로 직렬화한다.
     *
     * @param ms milliseconds since the epoch / 에포크 이후 밀리초
     * @return the text / 문자열
     */
    static String formatWeek(long ms) {
        long days = floorDiv(ms, MS_PER_DAY);
        long monday = days - weekday(days);
        long weekYear = civilFromDays(monday + 3)[0];
        long week = (monday - week1Monday(weekYear)) / 7 + 1;
        return pad(weekYear, 4) + "-W" + pad(week, 2);
    }

    /**
     * Parses a time string.
     * <p>
     * 시간 문자열을 해석한다.
     *
     * @param s the text / 문자열
     * @return milliseconds since midnight, or null / 자정 이후 밀리초. 올바르지 않으면 null
     */
    static Long parseTime(String s) {
        long[] t = timeComponent(s, 0);
        return t == null || t[1] != s.length() ? null : Long.valueOf(t[0]);
    }

    /**
     * Serializes milliseconds since midnight as the shortest time string (seconds and fraction only when they are not zero).
     * <p>
     * 자정 이후 밀리초를 가장 짧은 시간 문자열로 직렬화한다(초와 소수부는 0 이 아닐 때만).
     *
     * @param ms milliseconds since midnight / 자정 이후 밀리초
     * @return the text / 문자열
     */
    static String formatTime(long ms) {
        long t = floorMod(ms, MS_PER_DAY);
        long h = t / 3600000L;
        long mi = (t / 60000L) % 60;
        long sec = (t / 1000L) % 60;
        long milli = t % 1000;
        StringBuilder b = new StringBuilder();
        b.append(pad(h, 2)).append(':').append(pad(mi, 2));
        if (sec != 0 || milli != 0) {
            b.append(':').append(pad(sec, 2));
            if (milli != 0) {
                String f = pad(milli, 3);
                int end = f.length();
                // 소수부 끝의 0 을 뗀다.
                while (end > 1 && f.charAt(end - 1) == '0') {
                    end--;
                }
                b.append('.').append(f.substring(0, end));
            }
        }
        return b.toString();
    }

    /**
     * Parses a local date and time string (the separator may be T or a space).
     * <p>
     * 지역 날짜와 시간 문자열을 해석한다(구분자는 T 또는 공백).
     *
     * @param s the text / 문자열
     * @return milliseconds since 1970-01-01T00:00, or null / 1970-01-01T00:00 이후 밀리초. 올바르지 않으면 null
     */
    static Long parseLocal(String s) {
        long[] d = dateComponent(s, 0);
        if (d == null) {
            return null;
        }
        int i = (int) d[1];
        if (i >= s.length() || (s.charAt(i) != 'T' && s.charAt(i) != ' ')) {
            return null;
        }
        long[] t = timeComponent(s, i + 1);
        if (t == null || t[1] != s.length()) {
            return null;
        }
        return Long.valueOf(d[0] * MS_PER_DAY + t[0]);
    }

    /**
     * Serializes milliseconds as the normalized (shortest) local date and time string.
     * <p>
     * 밀리초를 정규화된(가장 짧은) 지역 날짜와 시간 문자열로 직렬화한다.
     *
     * @param ms milliseconds since 1970-01-01T00:00 / 1970-01-01T00:00 이후 밀리초
     * @return the text / 문자열
     */
    static String formatLocal(long ms) {
        return formatDate(ms) + "T" + formatTime(floorMod(ms, MS_PER_DAY));
    }

    /**
     * Left-pads a number with zeros.
     * <p>
     * 숫자 앞을 0 으로 채운다.
     *
     * @param v      the number / 숫자
     * @param digits the minimum number of digits / 최소 자릿수
     * @return the text / 문자열
     */
    private static String pad(long v, int digits) {
        String t = Long.toString(v);
        StringBuilder b = new StringBuilder();
        // 모자란 자릿수만큼 0 을 앞에 붙인다.
        for (int i = t.length(); i < digits; i++) {
            b.append('0');
        }
        return b.append(t).toString();
    }
}
