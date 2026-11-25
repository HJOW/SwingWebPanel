package org.duckdns.hjow.swingwebpanel.js.date;

import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DateBuiltins;
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
     * Checks Date-only UTC defaults, local date and time, and minute-precision explicit offsets.
     * <p>
     * 날짜 전용 UTC 기본값, 지역 날짜·시간과 분 단위 명시 오프셋을 확인한다.
     */
    public void testDateTimeStringOffsets() {
        JsRealm realm = JsRealm.create();
        DateBuiltins.setTimeZone(realm, new SimpleTimeZone(345 * 60000, "UTC+05:45"));
        String script = "const parse=Date.parse,utc=Date.UTC,base=utc(2024,1,29,1,15);"
                + "[parse('2024')===utc(2024,0),parse('2024-02')===utc(2024,1),parse('2024-02-29')===utc(2024,1,29),"
                + "parse('2024T05:45')===utc(2024,0),parse('2024-02T05:45')===utc(2024,1),"
                + "parse('2024-02-29T07:00')===base,parse('2024-02-29T07:00+05:45')===base,"
                + "parse('2024-02-29T07:00+0545')===base,parse('2024-02-29T07:00-00:00')===utc(2024,1,29,7),"
                + "parse('Thu, 29 Feb 2024 07:00:00 +0545')===base,"
                + "new Date('2024-02-29T07:00+0545').getTime()===base,"
                + "['+05','+05:45:30','+05:45:30.1','+054530','+24:00','-00:60'].every(s=>Number.isNaN(parse('2024-02-29T07:00'+s)))].join()";
        Check.equal("true,true,true,true,true,true,true,true,true,true,true,true",
                Ops.toString(realm.evaluateScript(script, "date-time-offsets.js")), "Date strings and compatibility offsets");
    }

    /**
     * Checks extended years and the last millisecond at each TimeClip boundary.
     * <p>
     * 확장 연도와 양쪽 TimeClip 경계의 밀리초를 검증한다.
     */
    public void testDateTimeStringBoundaries() {
        JsRealm realm = JsRealm.create();
        DateBuiltins.setTimeZone(realm, TimeZone.getTimeZone("UTC"));
        String script = "const p=Date.parse;"
                + "[p('+275760-09-13T00:01:00+00:01')===8640000000000000,"
                + "p('-271821-04-19T23:59:00-00:01')===-8640000000000000,"
                + "p('2024-02-29T24:00:00.0000Z')===p('2024-03-01T00:00:00Z'),"
                + "p('2024-02-29T00:00:00.1239Z')===p('2024-02-29T00:00:00.123Z'),"
                + "['-000000-01-01','+275760-09-13T00:00:00.001Z','-271821-04-19T23:59:59.999Z',"
                + "'2024-02-29T24:00:00.0001Z','2024-02-29T24:01Z','2024-02-30'].every(s=>Number.isNaN(p(s)))].join()";
        Check.equal("true,true,true,true,true", Ops.toString(realm.evaluateScript(script, "date-time-boundaries.js")),
                "numeric offsets apply before TimeClip and midnight rejects nonzero fractions");
    }

    /**
     * Confirms that Date samples the realm's current zone after coercing script arguments.
     * <p>
     * 스크립트 인수를 강제 변환한 후 Date가 realm의 현재 시간대를 사용하는지 확인한다.
     */
    public void testHostZoneAfterCoercion() {
        JsRealm realm = JsRealm.create();
        realm.method(realm.getGlobalObject(), "setTestZone", 1, new NativeFunction.Call() {
            /** Changes only this test realm's fixed offset. / 시험 realm의 고정 오프셋만 바꾼다. */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                int minutes = (int) Ops.toNumber(Js.arg(args, 0));
                DateBuiltins.setTimeZone(r, new SimpleTimeZone(minutes * 60000, "test"));
                return Js.UNDEFINED;
            }
        });
        String script = "const time='1970-01-01T00:00';setTestZone(0);"
                + "const fromText=new Date({[Symbol.toPrimitive](){setTestZone(60);return time;}}).getTime();"
                + "const fromParts=new Date({valueOf(){setTestZone(120);return 1970;}},0).getTime();"
                + "const fromParse=Date.parse({toString(){setTestZone(-30);return time;}});"
                + "[fromText,fromParts,fromParse,new Date(time).getTimezoneOffset()].join()";
        Check.equal("-3600000,-7200000,1800000,30", Ops.toString(realm.evaluateScript(script, "date-realm-zone.js")),
                "Date uses the time zone in effect after each conversion");
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
