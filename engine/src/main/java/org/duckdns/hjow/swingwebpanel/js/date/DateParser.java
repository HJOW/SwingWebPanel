package org.duckdns.hjow.swingwebpanel.js.date;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * <code>Date.parse</code>: converts text to a time value (or NaN).
 * <p>
 * First the ECMAScript 2015 date time string format is tried (section 20.3.1.15): <code>YYYY</code>,
 * <code>YYYY-MM</code>, <code>YYYY-MM-DD</code> (UTC), optionally followed by <code>THH:mm</code>,
 * <code>THH:mm:ss</code> or <code>THH:mm:ss.sss</code> and a zone <code>Z</code> or <code>+HH:mm</code>
 * (a date-time without a zone is local time); extended years use a sign and six digits (<code>-000000</code> is invalid);
 * <code>T24:00:00</code> is the end of the day; illegal values such as February 30 make the result NaN.
 * If the text does not have that shape, an implementation-defined fallback parser accepts the formats produced by
 * <code>toString</code> and <code>toUTCString</code> and common forms such as RFC 2822
 * (<code>Tue, 01 Mar 2016 10:20:30 +0900</code>), <code>March 1, 2016 10:20 PM</code>, <code>1 Mar 2016</code>,
 * <code>2016/03/01 10:20:30</code>, <code>2016-03-01 10:20:30</code> and <code>03/01/2016</code>. The fallback treats numeric
 * dates as year-month-day when the first number has three or more digits or exceeds 31, else month-day-year; two digit
 * years 00-49 mean 2000-2049 and 50-99 mean 1950-1999; a missing year makes the result NaN; text without a zone is local time;
 * parenthesized text is ignored. The time zone is injectable. All methods are static and thread-safe.
 * <p>
 * <code>Date.parse</code>: 문자열을 시간값(또는 NaN)으로 바꾼다.
 * <p>
 * 먼저 ECMAScript 2015 날짜 시간 문자열 형식(20.3.1.15절)을 시도한다: <code>YYYY</code>, <code>YYYY-MM</code>,
 * <code>YYYY-MM-DD</code>(UTC), 선택적으로 <code>THH:mm</code>, <code>THH:mm:ss</code>, <code>THH:mm:ss.sss</code> 와 시간대
 * <code>Z</code> 또는 <code>+HH:mm</code>(시간대가 없는 날짜 시간은 지역 시간). 확장 연도는 부호와 6 자리를 쓰며(<code>-000000</code> 은 올바르지 않음),
 * <code>T24:00:00</code> 은 하루의 끝이고, 2 월 30 일 같은 올바르지 않은 값은 NaN 이다.
 * 그 형태가 아니면 구현 정의 대체 해석기가 <code>toString</code>/<code>toUTCString</code> 의 출력과 RFC 2822
 * (<code>Tue, 01 Mar 2016 10:20:30 +0900</code>), <code>March 1, 2016 10:20 PM</code>, <code>1 Mar 2016</code>,
 * <code>2016/03/01 10:20:30</code>, <code>2016-03-01 10:20:30</code>, <code>03/01/2016</code> 같은 흔한 형식을 받아들인다.
 * 대체 해석기는 첫 숫자가 3 자리 이상이거나 31 을 넘으면 숫자 날짜를 연-월-일로, 아니면 월-일-연으로 보고, 두 자리 연도 00-49 는 2000-2049,
 * 50-99 는 1950-1999 이며, 연도가 없으면 NaN, 시간대가 없으면 지역 시간이고 괄호 안의 텍스트는 무시한다. 시간대는 주입할 수 있다.
 * 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class DateParser {

    /**
     * Full month names in lower case.
     * <p>
     * 소문자 달 이름 전체.
     */
    private static final String[] MONTHS = { "january", "february", "march", "april", "may", "june", "july", "august", "september",
        "october", "november", "december" };

    /**
     * Full weekday names in lower case.
     * <p>
     * 소문자 요일 이름 전체.
     */
    private static final String[] WEEKDAYS = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };

    /**
     * Zone abbreviations and their offsets in minutes east of UTC.
     * <p>
     * 시간대 약어와 UTC 동쪽 오프셋(분).
     */
    private static final Object[][] ZONES = { { "gmt", 0 }, { "utc", 0 }, { "ut", 0 }, { "z", 0 }, { "est", -300 }, { "edt", -240 },
        { "cst", -360 }, { "cdt", -300 }, { "mst", -420 }, { "mdt", -360 }, { "pst", -480 }, { "pdt", -420 } };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DateParser() {
    }

    /**
     * Date.parse(string).
     * <p>
     * Date.parse(string).
     *
     * @param text text to parse / 해석할 문자열
     * @param tz   time zone used for text without a zone / 시간대가 없는 문자열에 쓰는 시간대
     * @return time value, or NaN if the text is not a date / 시간값. 날짜가 아니면 NaN
     */
    public static double parse(String text, TimeZone tz) {
        if (text == null) {
            return Double.NaN;
        }
        int b = 0;
        int e = text.length();
        // 앞뒤 공백을 걷어낸다.
        while (b < e && CharClass.isWhiteSpaceOrLineTerminator(text.charAt(b))) {
            b++;
        }
        // 뒤쪽 공백도 걷어낸다.
        while (e > b && CharClass.isWhiteSpaceOrLineTerminator(text.charAt(e - 1))) {
            e--;
        }
        String s = text.substring(b, e);
        if (s.length() == 0) {
            return Double.NaN;
        }
        double[] out = new double[1];
        int status = parseIso(s, tz, out);
        if (status == 1) {
            return out[0];
        }
        if (status == 2) {
            return Double.NaN;
        }
        return DateMath.timeClip(parseFallback(s, tz));
    }

    /**
     * Reads exactly count decimal digits at an index.
     * <p>
     * 지정한 위치에서 정확히 count 개의 10진 숫자를 읽는다.
     *
     * @param s     text / 문자열
     * @param from  start index / 시작 위치
     * @param count digit count / 숫자 개수
     * @return the value, or -1 if not that many digits / 값. 그만큼의 숫자가 아니면 -1
     */
    private static int digits(String s, int from, int count) {
        if (from + count > s.length()) {
            return -1;
        }
        int v = 0;
        // 지정한 개수의 문자가 모두 숫자여야 한다.
        for (int i = 0; i < count; i++) {
            char c = s.charAt(from + i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    /**
     * Number of days in a month of a proleptic Gregorian year.
     * <p>
     * 그레고리력 연도의 어떤 달의 날수.
     *
     * @param year  year / 연도
     * @param month month 1..12 / 달 1..12
     * @return days / 날수
     */
    private static int daysInMonth(double year, int month) {
        int[] d = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if (month == 2 && DateMath.daysInYear(year) == 366) {
            return 29;
        }
        return d[month - 1];
    }

    /**
     * Tries the ECMAScript date time string format.
     * <p>
     * ECMAScript 날짜 시간 문자열 형식을 시도한다.
     *
     * @param s   trimmed text / 다듬은 문자열
     * @param tz  time zone / 시간대
     * @param out receives the time value when the result is 1 / 결과가 1 일 때 시간값을 받는다
     * @return 0 if the text does not have the format, 1 if valid, 2 if it has the format but an illegal value / 형식이 아니면 0, 올바르면 1, 형식은 맞지만 값이 올바르지 않으면 2
     */
    private static int parseIso(String s, TimeZone tz, double[] out) {
        int n = s.length();
        int i;
        int year;
        char c0 = s.charAt(0);
        if (c0 == '+' || c0 == '-') {
            year = digits(s, 1, 6);
            if (year < 0) {
                return 0;
            }
            if (c0 == '-') {
                if (year == 0) {
                    return 2;
                }
                year = -year;
            }
            i = 7;
        } else {
            year = digits(s, 0, 4);
            if (year < 0) {
                return 0;
            }
            i = 4;
        }
        int month = 1;
        int day = 1;
        if (i < n && s.charAt(i) == '-') {
            month = digits(s, i + 1, 2);
            if (month < 0) {
                return 0;
            }
            i += 3;
            if (i < n && s.charAt(i) == '-') {
                day = digits(s, i + 1, 2);
                if (day < 0) {
                    return 0;
                }
                i += 3;
            }
        }
        boolean hasTime = false;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int ms = 0;
        boolean nonzeroFraction = false;
        boolean hasOffset = false;
        int offsetMinutes = 0;
        if (i < n && (s.charAt(i) == 'T' || s.charAt(i) == 't')) {
            hasTime = true;
            hour = digits(s, i + 1, 2);
            if (hour < 0 || i + 3 >= n || s.charAt(i + 3) != ':') {
                return 0;
            }
            min = digits(s, i + 4, 2);
            if (min < 0) {
                return 0;
            }
            i += 6;
            if (i < n && s.charAt(i) == ':') {
                sec = digits(s, i + 1, 2);
                if (sec < 0) {
                    return 0;
                }
                i += 3;
                if (i < n && s.charAt(i) == '.') {
                    int j = i + 1;
                    int scale = 100;
                    // 소수 초는 처음 세 자리까지만 밀리초로 쓴다.
                    while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                        int digit = s.charAt(j) - '0';
                        ms += scale * digit;
                        nonzeroFraction |= digit != 0;
                        scale /= 10;
                        j++;
                    }
                    if (j == i + 1) {
                        return 0;
                    }
                    i = j;
                }
            }
            if (i < n && (s.charAt(i) == 'Z' || s.charAt(i) == 'z')) {
                hasOffset = true;
                i++;
            } else if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                // Date 시간 문자열은 분 단위 숫자 오프셋만 허용한다.
                offsetMinutes = TimeZoneOffsets.dateMinutes(s.substring(i));
                if (offsetMinutes == TimeZoneOffsets.INVALID_DATE_MINUTES) {
                    return 2;
                }
                hasOffset = true;
                i = n;
            }
        }
        if (i != n) {
            return 0;
        }
        boolean legal = month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, Math.max(1, Math.min(12, month)))
                && hour <= 24 && min <= 59 && sec <= 59;
        if (legal && hour == 24 && (min != 0 || sec != 0 || nonzeroFraction)) {
            legal = false;
        }
        if (!legal) {
            return 2;
        }
        double tv = DateMath.makeDate(DateMath.makeDay(year, month - 1, day), DateMath.makeTime(hour, min, sec, ms));
        if (hasOffset) {
            tv -= offsetMinutes * DateMath.MS_PER_MINUTE;
        } else if (hasTime) {
            tv = DateMath.utc(tv, tz);
        }
        out[0] = DateMath.timeClip(tv);
        return 1;
    }

    /**
     * Index of the month named by a lower case word (three or more letters of the full name), or -1.
     * <p>
     * 소문자 단어가 가리키는 달의 번호(전체 이름의 앞 3 글자 이상). 없으면 -1.
     *
     * @param w lower case word / 소문자 단어
     * @return 0..11 or -1 / 0..11 또는 -1
     */
    private static int monthIndex(String w) {
        if (w.length() < 3) {
            return -1;
        }
        // 12 개의 달 이름 중 단어가 접두사인 것을 찾는다.
        for (int m = 0; m < 12; m++) {
            if (MONTHS[m].startsWith(w)) {
                return m;
            }
        }
        return -1;
    }

    /**
     * Tests whether a lower case word names a weekday (three or more letters).
     * <p>
     * 소문자 단어가 요일 이름인지 검사한다(3 글자 이상).
     *
     * @param w lower case word / 소문자 단어
     * @return true for a weekday / 요일이면 true
     */
    private static boolean isWeekday(String w) {
        if (w.length() < 3) {
            return false;
        }
        // 7 개의 요일 이름 중 단어가 접두사인 것이 있는지 확인한다.
        for (int d = 0; d < 7; d++) {
            if (WEEKDAYS[d].startsWith(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fallback parser for the implementation-defined formats described in the class comment.
     * <p>
     * 클래스 설명에 적은 구현 정의 형식을 위한 대체 해석기.
     *
     * @param s  trimmed text / 다듬은 문자열
     * @param tz time zone for text without a zone / 시간대가 없는 문자열에 쓰는 시간대
     * @return time value (not clipped) or NaN / 시간값(제한 전) 또는 NaN
     */
    private static double parseFallback(String s, TimeZone tz) {
        int n = s.length();
        int i = 0;
        int month = -1;
        List<Integer> nums = new ArrayList<Integer>();
        List<Integer> lens = new ArrayList<Integer>();
        boolean hasTime = false;
        int hour = 0;
        int min = 0;
        int sec = 0;
        int ms = 0;
        int ampm = 0;
        boolean hasZone = false;
        int zoneMinutes = 0;
        boolean sawGmt = false;
        // 문자열을 앞에서부터 단어, 숫자, 시각, 시간대 부호로 나누어 해석한다.
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r' || c == '/' || c == '.' || CharClass.isWhiteSpace(c)) {
                i++;
            } else if (c == '(') {
                int depth = 0;
                // 괄호 안(중첩 포함)은 모두 건너뛴다.
                while (i < n) {
                    if (s.charAt(i) == '(') {
                        depth++;
                    } else if (s.charAt(i) == ')') {
                        depth--;
                        if (depth == 0) {
                            i++;
                            break;
                        }
                    }
                    i++;
                }
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int j = i;
                StringBuilder w = new StringBuilder();
                // 글자(와 약어의 점)로 이루어진 단어를 읽는다.
                while (j < n && ((s.charAt(j) >= 'a' && s.charAt(j) <= 'z') || (s.charAt(j) >= 'A' && s.charAt(j) <= 'Z')
                        || (s.charAt(j) == '.' && j + 1 < n && Character.isLetter(s.charAt(j + 1)) && w.length() <= 2))) {
                    if (s.charAt(j) != '.') {
                        w.append(Character.toLowerCase(s.charAt(j)));
                    }
                    j++;
                }
                i = j;
                String word = w.toString();
                int mi = monthIndex(word);
                if (mi >= 0) {
                    month = mi;
                } else if (isWeekday(word) || "t".equals(word) || "at".equals(word) || "of".equals(word) || "st".equals(word)
                        || "nd".equals(word) || "rd".equals(word) || "th".equals(word)) {
                    continue;
                } else if ("am".equals(word)) {
                    ampm = 1;
                } else if ("pm".equals(word)) {
                    ampm = 2;
                } else {
                    boolean found = false;
                    // 알려진 시간대 약어인지 표에서 찾는다.
                    for (int z = 0; z < ZONES.length; z++) {
                        if (ZONES[z][0].equals(word)) {
                            hasZone = true;
                            zoneMinutes = ((Integer) ZONES[z][1]).intValue();
                            sawGmt = "gmt".equals(word) || "utc".equals(word) || "ut".equals(word);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return Double.NaN;
                    }
                }
            } else if (c >= '0' && c <= '9') {
                int j = i;
                long v = 0;
                // 숫자열을 읽는다(너무 길면 값은 제한한다).
                while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                    if (v < 100000000L) {
                        v = v * 10 + (s.charAt(j) - '0');
                    }
                    j++;
                }
                int len = j - i;
                if (j < n && s.charAt(j) == ':' && !hasTime) {
                    // 시:분[:초[.밀리초]] 형태의 시각을 읽는다.
                    hasTime = true;
                    hour = (int) v;
                    int[] pos = { j + 1 };
                    int mm = readNumber(s, pos, 2);
                    if (mm < 0) {
                        return Double.NaN;
                    }
                    min = mm;
                    if (pos[0] < n && s.charAt(pos[0]) == ':') {
                        pos[0]++;
                        int ss = readNumber(s, pos, 2);
                        if (ss < 0) {
                            return Double.NaN;
                        }
                        sec = ss;
                        if (pos[0] < n && (s.charAt(pos[0]) == '.' || s.charAt(pos[0]) == ',')) {
                            pos[0]++;
                            int scale = 100;
                            int start = pos[0];
                            // 소수 초는 처음 세 자리까지만 밀리초로 쓴다.
                            while (pos[0] < n && s.charAt(pos[0]) >= '0' && s.charAt(pos[0]) <= '9') {
                                ms += scale * (s.charAt(pos[0]) - '0');
                                scale /= 10;
                                pos[0]++;
                            }
                            if (pos[0] == start) {
                                return Double.NaN;
                            }
                        }
                    }
                    i = pos[0];
                } else {
                    nums.add(Integer.valueOf((int) Math.min(v, Integer.MAX_VALUE)));
                    lens.add(Integer.valueOf(len));
                    i = j;
                }
            } else if (c == '+' || c == '-') {
                boolean nextDigit = i + 1 < n && s.charAt(i + 1) >= '0' && s.charAt(i + 1) <= '9';
                if (nextDigit && (hasTime || sawGmt)) {
                    int j = i + 1;
                    int hh;
                    int mm = 0;
                    int count = 0;
                    // 부호 뒤 숫자의 자릿수로 hhmm, hh:mm, hh 형태를 구분한다.
                    while (j + count < n && s.charAt(j + count) >= '0' && s.charAt(j + count) <= '9') {
                        count++;
                    }
                    if (count == 4) {
                        hh = digits(s, j, 2);
                        mm = digits(s, j + 2, 2);
                        i = j + 4;
                    } else if (count <= 2) {
                        hh = digits(s, j, count);
                        i = j + count;
                        if (i < n && s.charAt(i) == ':' && digits(s, i + 1, 2) >= 0) {
                            mm = digits(s, i + 1, 2);
                            i += 3;
                        }
                    } else {
                        return Double.NaN;
                    }
                    if (hh < 0 || mm < 0 || hh > 23 || mm > 59) {
                        return Double.NaN;
                    }
                    hasZone = true;
                    zoneMinutes = (hh * 60 + mm) * (c == '-' ? -1 : 1);
                    sawGmt = false;
                } else if (c == '-') {
                    i++;
                } else {
                    return Double.NaN;
                }
            } else {
                return Double.NaN;
            }
        }
        return assemble(month, nums, lens, hasTime, hour, min, sec, ms, ampm, hasZone, zoneMinutes, tz);
    }

    /**
     * Reads one or two decimal digits at pos[0] and advances pos[0].
     * <p>
     * pos[0] 에서 10진 숫자 한두 자리를 읽고 pos[0] 을 전진시킨다.
     *
     * @param s      text / 문자열
     * @param pos    in/out index / 입출력 위치
     * @param maxLen maximum digits / 최대 자릿수
     * @return value or -1 if no digit / 값. 숫자가 없으면 -1
     */
    private static int readNumber(String s, int[] pos, int maxLen) {
        int i = pos[0];
        int v = 0;
        int count = 0;
        // 최대 자릿수까지 숫자를 읽는다.
        while (i < s.length() && count < maxLen && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            v = v * 10 + (s.charAt(i) - '0');
            i++;
            count++;
        }
        if (count == 0) {
            return -1;
        }
        pos[0] = i;
        return v;
    }

    /**
     * Combines the parsed pieces into a time value.
     * <p>
     * 해석한 조각들을 시간값으로 합친다.
     *
     * @param month       month index or -1 / 달 번호 또는 -1
     * @param nums        loose numbers in order / 순서대로의 숫자들
     * @param lens        digit counts of the numbers / 숫자의 자릿수
     * @param hasTime     whether a clock time was given / 시각이 주어졌는지
     * @param hour        hour / 시
     * @param min         minute / 분
     * @param sec         second / 초
     * @param ms          millisecond / 밀리초
     * @param ampm        0 none, 1 AM, 2 PM / 0 없음, 1 AM, 2 PM
     * @param hasZone     whether a zone was given / 시간대가 주어졌는지
     * @param zoneMinutes zone offset in minutes east of UTC / UTC 동쪽 오프셋(분)
     * @param tz          time zone for local text / 지역 시간 문자열에 쓰는 시간대
     * @return time value or NaN / 시간값 또는 NaN
     */
    private static double assemble(int month, List<Integer> nums, List<Integer> lens, boolean hasTime, int hour, int min, int sec,
            int ms, int ampm, boolean hasZone, int zoneMinutes, TimeZone tz) {
        int year;
        int mon;
        int day;
        int count = nums.size();
        if (month >= 0) {
            mon = month + 1;
            if (count == 2) {
                boolean firstIsYear = lens.get(0).intValue() >= 3 || nums.get(0).intValue() > 31;
                year = firstIsYear ? nums.get(0).intValue() : nums.get(1).intValue();
                day = firstIsYear ? nums.get(1).intValue() : nums.get(0).intValue();
                int yearLen = firstIsYear ? lens.get(0).intValue() : lens.get(1).intValue();
                year = expandYear(year, yearLen);
            } else if (count == 1 && (lens.get(0).intValue() >= 3 || nums.get(0).intValue() > 31)) {
                year = expandYear(nums.get(0).intValue(), lens.get(0).intValue());
                day = 1;
            } else {
                return Double.NaN;
            }
        } else if (count == 3) {
            boolean yearFirst = lens.get(0).intValue() >= 3 || nums.get(0).intValue() > 31;
            if (yearFirst) {
                year = expandYear(nums.get(0).intValue(), lens.get(0).intValue());
                mon = nums.get(1).intValue();
                day = nums.get(2).intValue();
            } else {
                mon = nums.get(0).intValue();
                day = nums.get(1).intValue();
                year = expandYear(nums.get(2).intValue(), lens.get(2).intValue());
            }
        } else {
            return Double.NaN;
        }
        if (mon < 1 || mon > 12 || day < 1 || day > daysInMonth(year, mon)) {
            return Double.NaN;
        }
        if (ampm != 0) {
            if (!hasTime || hour < 1 || hour > 12) {
                return Double.NaN;
            }
            hour = (hour % 12) + (ampm == 2 ? 12 : 0);
        }
        if (hour > 23 && !(hour == 24 && min == 0 && sec == 0 && ms == 0)) {
            return Double.NaN;
        }
        if (min > 59 || sec > 59) {
            return Double.NaN;
        }
        double tv = DateMath.makeDate(DateMath.makeDay(year, mon - 1, day), DateMath.makeTime(hour, min, sec, ms));
        if (hasZone) {
            return tv - zoneMinutes * DateMath.MS_PER_MINUTE;
        }
        return DateMath.utc(tv, tz);
    }

    /**
     * Expands a one or two digit year (00-49 to 2000-2049, 50-99 to 1950-1999); longer years are used as they are.
     * <p>
     * 한두 자리 연도를 확장한다(00-49 는 2000-2049, 50-99 는 1950-1999). 더 긴 연도는 그대로 쓴다.
     *
     * @param y   year number as written / 쓰여 있는 연도 숫자
     * @param len digit count as written / 쓰여 있는 자릿수
     * @return full year / 전체 연도
     */
    private static int expandYear(int y, int len) {
        if (len <= 2) {
            return y < 50 ? 2000 + y : 1900 + y;
        }
        return y;
    }
}
