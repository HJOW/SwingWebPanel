package org.duckdns.hjow.swingwebpanel.js.date;

import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * Parses the ECMAScript UTCOffset exchange format (21.4.1.33 and Annex A.7).
 * <p>
 * ECMAScript의 UTCOffset 교환 형식(21.4.1.33 및 부속서 A.7)을 파싱한다.
 */
public final class TimeZoneOffsets {

    /** Nanoseconds per second. / 초당 나노초. */
    private static final long NANOS_PER_SECOND = 1000000000L;

    /** Sentinel outside the valid offset range. / 오프셋 범위 밖의 오류 표지. */
    private static final long INVALID = Long.MIN_VALUE;

    /** Static parsing operations. / 정적 파싱 연산. */
    private TimeZoneOffsets() {
    }

    /**
     * Checks the UTCOffset grammar with IsTimeZoneOffsetString.
     * <p>
     * IsTimeZoneOffsetString의 UTCOffset 문법을 확인한다.
     *
     * @param text candidate string / 검사할 문자열
     * @return true for a well-formed offset / 올바른 오프셋이면 true
     */
    public static boolean isOffsetString(String text) {
        return read(text) != INVALID;
    }

    /**
     * Implements ParseTimeZoneOffsetString for a validated string.
     * <p>
     * 검증한 문자열에 ParseTimeZoneOffsetString을 적용한다.
     *
     * @param text offset string / 오프셋 문자열
     * @return signed nanoseconds east of UTC / UTC 동쪽 오프셋 나노초
     * @throws IllegalArgumentException if the text does not match UTCOffset / UTCOffset 문법에 맞지 않을 때
     */
    public static long parseNanoseconds(String text) {
        long result = read(text);
        if (result == INVALID) {
            throw new IllegalArgumentException("Invalid UTC offset");
        }
        return result;
    }

    /**
     * Reads the sign, hours, and optional minutes, seconds, and subsecond digits.
     * <p>
     * 부호, 시간 및 선택적인 분·초·초 미만 숫자를 읽는다.
     *
     * @param text candidate string / 검사할 문자열
     * @return offset or the invalid sentinel / 오프셋 또는 오류 표지
     */
    private static long read(String text) {
        if (text == null || text.length() < 3 || text.length() > 19) {
            return INVALID;
        }
        char sign = text.charAt(0);
        int hour = component(text, 1, 23);
        if ((sign != '+' && sign != '-') || hour < 0) {
            return INVALID;
        }
        int pos = 3;
        int minute = 0;
        int second = 0;
        int fraction = 0;
        int length = text.length();
        if (pos < length) {
            boolean extended = text.charAt(pos) == ':';
            if (extended) {
                pos++;
            }
            minute = component(text, pos, 59);
            if (minute < 0) {
                return INVALID;
            }
            pos += 2;
            if (pos < length) {
                // 분 앞에 콜론을 썼으면 초 앞에도 같은 구분자를 요구한다.
                if (extended && text.charAt(pos++) != ':') {
                    return INVALID;
                }
                second = component(text, pos, 59);
                if (second < 0) {
                    return INVALID;
                }
                pos += 2;
                if (pos < length) {
                    char separator = text.charAt(pos++);
                    int digits = length - pos;
                    if ((separator != '.' && separator != ',') || digits < 1 || digits > 9) {
                        return INVALID;
                    }
                    // 소수 초를 읽고 오른쪽에 0을 채워 정확한 나노초 값을 만든다.
                    for (int i = 0; i < 9; i++) {
                        int digit = 0;
                        if (i < digits) {
                            char ch = text.charAt(pos + i);
                            if (!CharClass.isDecimalDigit(ch)) {
                                return INVALID;
                            }
                            digit = ch - '0';
                        }
                        fraction = fraction * 10 + digit;
                    }
                }
            }
        }
        long magnitude = ((hour * 60L + minute) * 60L + second) * NANOS_PER_SECOND + fraction;
        return sign == '-' ? -magnitude : magnitude;
    }

    /**
     * Reads a bounded two-digit offset component.
     * <p>
     * 최댓값 이하인 두 자리 오프셋 항목을 읽는다.
     *
     * @param text complete string / 전체 문자열
     * @param pos component start / 항목 시작 위치
     * @param maximum highest allowed value / 허용 최댓값
     * @return component value or -1 / 항목 값 또는 -1
     */
    private static int component(String text, int pos, int maximum) {
        if (pos + 2 > text.length()) {
            return -1;
        }
        char hi = text.charAt(pos);
        char lo = text.charAt(pos + 1);
        if (!CharClass.isDecimalDigit(hi) || !CharClass.isDecimalDigit(lo)) {
            return -1;
        }
        int value = 10 * (hi - '0') + lo - '0';
        return value <= maximum ? value : -1;
    }
}
