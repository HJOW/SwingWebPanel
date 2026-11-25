package org.duckdns.hjow.swingwebpanel.js.date;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks the ECMAScript offset grammar and Date time string boundaries.
 * <p>
 * ECMAScript 오프셋 문법 및 Date 시간 문자열의 경계를 검증한다.
 */
public class Es2026DateOffsetTest {

    /**
     * Verifies signed hours, basic and extended offsets, and exact fractional seconds.
     * <p>
     * 부호 있는 시, 기본·확장 오프셋 및 정확한 소수 초를 검증한다.
     */
    public void testOffsetNanoseconds() {
        assertOffset("+00", 0L);
        assertOffset("-0000", 0L);
        assertOffset("-00:00:00,000000000", 0L);
        assertOffset("+09", 32400000000000L);
        assertOffset("-05:30", -19800000000000L);
        assertOffset("+0530", 19800000000000L);
        assertOffset("+05:30:45.1234567", 19845123456700L);
        assertOffset("-023045,1", -9045100000000L);
        assertOffset("-00:00:00.000000001", -1L);
        assertOffset("+23:59:59.999999999", 86399999999999L);
        assertOffset("-235959.999999999", -86399999999999L);
    }

    /**
     * Rejects malformed separators, excess precision, and non-ASCII or overflowing components.
     * <p>
     * 잘못된 구분자와 과도한 정밀도, ASCII 이외의 문자 및 범위 초과 항목을 거부한다.
     */
    public void testInvalidOffsets() {
        String[] invalid = { null, "", "Z", "09", "+9", "+090", "+09:", "+09:1", "+09.5", "+09:00.1",
            "+09:0000", "+0900:00", "+09:00:", "+090000.", "+09:00:00.1234567890", "+090000,1234567890",
            "+24", "-24:00", "+00:60", "+00:00:60", "-00:00:00..1", "+00:00:00,x",
            " +09:00", "+09:00 ", "+09:00\n", "\u221209:00", "+\u0660\u0669:00", "+00:00:00.\u0661" };
        // 각 입력을 문법 검사와 잘못된 파싱 호출에서 모두 거부하는지 확인한다.
        for (String text : invalid) {
            Check.isFalse(TimeZoneOffsets.isOffsetString(text), "invalid offset " + text);
            try {
                TimeZoneOffsets.parseNanoseconds(text);
                Check.fail("invalid offset parsed: " + text);
            } catch (IllegalArgumentException expected) {
                Check.equal("Invalid UTC offset", expected.getMessage(), "parse failure for " + text);
            }
        }
    }

    /**
     * Checks both operations for a valid input.
     * <p>
     * 유효한 입력에 두 연산을 함께 검증한다.
     *
     * @param text valid offset / 유효한 오프셋
     * @param expected nanosecond displacement / 나노초 오프셋
     */
    private static void assertOffset(String text, long expected) {
        Check.isTrue(TimeZoneOffsets.isOffsetString(text), "valid offset " + text);
        Check.equal(Long.valueOf(expected), Long.valueOf(TimeZoneOffsets.parseNanoseconds(text)), "nanoseconds for " + text);
    }
}
