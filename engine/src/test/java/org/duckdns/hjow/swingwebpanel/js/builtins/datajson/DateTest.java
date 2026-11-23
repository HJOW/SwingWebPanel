package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.TimeZone;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Date built-in (plan item P14-02): constructor forms, static methods, getters and setters in several time zones, NaN rules,
 * argument conversion order, formatting, toJSON, [Symbol.toPrimitive], receiver checks and the Annex B methods.
 * <p>
 * Date 내장 객체 시험(계획 P14-02): 생성자 형태, 정적 메서드, 여러 시간대의 getter/setter, NaN 규칙, 인수 변환 순서, 서식, toJSON,
 * [Symbol.toPrimitive], 수신 객체 검사, 부속서 B 메서드.
 */
public class DateTest {

    /**
     * Environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private Env env;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DateTest() {
    }

    /**
     * Creates a fresh realm fixed to UTC before each case.
     * <p>
     * 사례마다 UTC 로 고정한 새 realm 을 만든다.
     */
    public void setUp() {
        env = new Env();
        DateBuiltins.setTimeZone(env.realm, TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a Date object with a time value.
     * <p>
     * 시간값으로 Date 객체를 만든다.
     *
     * @param t time value / 시간값
     * @return the date / 날짜
     */
    private JsObject date(double t) {
        return env.construct("Date", Env.n(t));
    }

    /**
     * Reads a numeric method result.
     * <p>
     * 숫자를 돌려주는 메서드 결과를 읽는다.
     *
     * @param d      the date / 날짜
     * @param method method name / 메서드 이름
     * @param args   arguments / 인수
     * @return the number / 숫자
     */
    private double num(JsObject d, String method, JsValue... args) {
        return Env.d(env.invoke(d, method, args));
    }

    /**
     * The constructor forms: no argument, one number, a string, a Date, several components, NaN and out-of-range values.
     * <p>
     * 생성자 형태: 인수 없음, 숫자 하나, 문자열, Date, 여러 구성 요소, NaN 과 범위를 벗어난 값.
     */
    public void testConstructorForms() {
        Check.equal(0.0, num(date(0), "getTime"), "new Date(0)");
        Check.isTrue(Double.isNaN(num(env.construct("Date", Env.n(Double.NaN)), "getTime")), "new Date(NaN)");
        Check.isTrue(Double.isNaN(num(date(8.64e15 + 1), "getTime")), "out of range time values are clipped to NaN");
        Check.equal(8.64e15, num(date(8.64e15), "getTime"), "the largest valid time value");
        Check.isTrue(Double.isNaN(num(env.construct("Date", Js.UNDEFINED), "getTime")), "new Date(undefined) is invalid");
        Check.equal(0.0, num(env.construct("Date", Js.NULL), "getTime"), "new Date(null) is time 0");
        double before = (double) System.currentTimeMillis();
        double now = num(env.construct("Date"), "getTime");
        Check.isTrue(now >= before - 5 && now <= before + 60000, "new Date() is about now");
        JsObject full = env.construct("Date", Env.n(2016), Env.n(0), Env.n(31), Env.n(10), Env.n(20), Env.n(30), Env.n(40));
        Check.equal(2016.0, num(full, "getFullYear"), "year");
        Check.equal(0.0, num(full, "getMonth"), "month");
        Check.equal(31.0, num(full, "getDate"), "day");
        Check.equal(10.0, num(full, "getHours"), "hours");
        Check.equal(20.0, num(full, "getMinutes"), "minutes");
        Check.equal(30.0, num(full, "getSeconds"), "seconds");
        Check.equal(40.0, num(full, "getMilliseconds"), "milliseconds");
        Check.equal(1999.0, num(env.construct("Date", Env.n(99), Env.n(0)), "getFullYear"), "two digit years mean 1900..1999");
        Check.equal(100.0, num(env.construct("Date", Env.n(100), Env.n(0)), "getFullYear"), "year 100 stays");
        Check.equal(0.0, num(env.construct("Date", Env.n(2000), Env.n(12)), "getMonth"), "month 12 is January of the next year");
        Check.equal(2001.0, num(env.construct("Date", Env.n(2000), Env.n(12)), "getFullYear"), "month overflow rolls the year");
        Check.isTrue(Double.isNaN(num(env.construct("Date", Env.n(2000), Env.n(Double.NaN)), "getTime")), "NaN component");
        JsObject copy = env.construct("Date", date(12345));
        Check.equal(12345.0, num(copy, "getTime"), "copy of a Date");
        Check.equal(1456795230000.0, num(env.construct("Date", Env.s("2016-03-01T01:20:30Z")), "getTime"), "ISO string");
    }

    /**
     * Date called as a function ignores its arguments and returns text; a subclass prototype is honored through new.target.
     * <p>
     * 함수로 호출한 Date 는 인수를 무시하고 문자열을 반환한다. 하위 클래스 프로토타입은 new.target 을 통해 반영된다.
     */
    public void testCallAndNewTarget() {
        JsValue text = Ops.call(env.get("Date"), Js.UNDEFINED, new JsValue[] { Env.n(1) });
        Check.isTrue(text instanceof JsString, "Date() returns a string");
        Check.isTrue(Env.str(text).length() > 10, "Date() text");
        JsObject sub = env.realm.newObject();
        NativeFunction target = new NativeFunction(env.realm, "Sub", 0, new NativeFunction.Call() {
            /**
             * Does nothing.
             * <p>
             * 아무것도 하지 않는다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        target.defineData("prototype", sub, false, false, false);
        JsObject d = env.obj("Date").construct(new JsValue[] { Env.n(0) }, target);
        Check.same(sub, d.getPrototypeOf(), "the prototype comes from new.target");
    }

    /**
     * Date.UTC, Date.parse and Date.now.
     * <p>
     * Date.UTC, Date.parse, Date.now 정적 메서드 시험.
     */
    public void testStaticMethods() {
        Check.equal(946684800000.0, Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2000), Env.n(0), Env.n(1))), "Date.UTC");
        Check.equal(946684800000.0, Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2000))), "a missing month counts as 0");
        Check.equal(915062400000.0, Env.d(env.call("Date.UTC", env.get("Date"), Env.n(98), Env.n(11), Env.n(31))), "two digit year (1998-12-31)");
        Check.isTrue(Double.isNaN(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(Double.NaN)))), "NaN year");
        Check.equal(1456795230000.0, Env.d(env.call("Date.parse", env.get("Date"), Env.s("2016-03-01T01:20:30.000Z"))), "parse ISO");
        Check.equal(1456795230000.0, Env.d(env.call("Date.parse", env.get("Date"), Env.s("Tue, 01 Mar 2016 01:20:30 GMT"))), "parse UTC string");
        Check.isTrue(Double.isNaN(Env.d(env.call("Date.parse", env.get("Date"), Env.s("garbage")))), "parse garbage");
        Check.isTrue(Double.isNaN(Env.d(env.call("Date.parse", env.get("Date")))), "parse(undefined) is NaN");
        double now = Env.d(env.call("Date.now", env.get("Date")));
        Check.isTrue(Math.abs(now - System.currentTimeMillis()) < 60000, "Date.now");
    }

    /**
     * Getters in a zone without DST (Asia/Seoul, UTC+9) and with DST (America/New_York).
     * <p>
     * DST 가 없는 시간대(Asia/Seoul, UTC+9)와 있는 시간대(America/New_York)의 getter.
     */
    public void testGettersInTimeZones() {
        JsObject d = date(1456795230040.0);
        DateBuiltins.setTimeZone(env.realm, TimeZone.getTimeZone("Asia/Seoul"));
        Check.equal(10.0, num(d, "getHours"), "local hours in Seoul");
        Check.equal(1.0, num(d, "getUTCHours"), "UTC hours");
        Check.equal(-540.0, num(d, "getTimezoneOffset"), "offset in Seoul");
        Check.equal(2.0, num(d, "getDay"), "weekday (Tuesday)");
        Check.equal(40.0, num(d, "getMilliseconds"), "milliseconds");
        Check.equal(40.0, num(d, "getUTCMilliseconds"), "UTC milliseconds");
        Check.equal(116.0, num(d, "getYear"), "getYear is year - 1900");
        DateBuiltins.setTimeZone(env.realm, TimeZone.getTimeZone("America/New_York"));
        Check.equal(20.0, num(d, "getHours"), "local hours in New York (UTC-5 on 2016-03-01)");
        Check.equal(300.0, num(d, "getTimezoneOffset"), "offset in standard time");
        Check.equal(29.0, num(d, "getDate"), "previous day in the evening: local date is 29 Feb");
        Check.equal(1.0, num(d, "getMonth"), "local month is February");
        JsObject summer = date(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2016), Env.n(6), Env.n(1), Env.n(12))));
        Check.equal(240.0, num(summer, "getTimezoneOffset"), "offset in daylight time");
        JsObject invalid = date(Double.NaN);
        Check.isTrue(Double.isNaN(num(invalid, "getHours")), "getter of an invalid date");
        Check.isTrue(Double.isNaN(num(invalid, "getTimezoneOffset")), "offset of an invalid date");
        Check.isTrue(Double.isNaN(num(invalid, "getYear")), "getYear of an invalid date");
    }

    /**
     * Setters, their return values, optional arguments and rolling over fields.
     * <p>
     * setter, 반환값, 선택 인수, 필드 넘침.
     */
    public void testSetters() {
        DateBuiltins.setTimeZone(env.realm, TimeZone.getTimeZone("Asia/Seoul"));
        JsObject d = date(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2016), Env.n(0), Env.n(31), Env.n(1), Env.n(2), Env.n(3), Env.n(4))));
        Check.equal(10.0, num(d, "getHours"), "start: local hour");
        double r = num(d, "setHours", Env.n(23), Env.n(59));
        Check.equal(r, num(d, "getTime"), "setHours returns the new time value");
        Check.equal(23.0, num(d, "getHours"), "hours");
        Check.equal(59.0, num(d, "getMinutes"), "minutes");
        Check.equal(3.0, num(d, "getSeconds"), "seconds are kept when not given");
        Check.equal(4.0, num(d, "getMilliseconds"), "milliseconds are kept when not given");
        num(d, "setMonth", Env.n(1));
        Check.equal(2.0, num(d, "getMonth"), "setMonth(1) on the 31st: Feb 31 overflows into March");
        Check.equal(2.0, num(d, "getDate"), "Feb 31 becomes Mar 2 (2016 is a leap year)");
        num(d, "setDate", Env.n(0));
        Check.equal(29.0, num(d, "getDate"), "day 0 is the last day of the previous month");
        num(d, "setUTCFullYear", Env.n(2000), Env.n(11), Env.n(25));
        Check.equal(2000.0, num(d, "getUTCFullYear"), "UTC full year");
        Check.equal(11.0, num(d, "getUTCMonth"), "UTC month");
        Check.equal(25.0, num(d, "getUTCDate"), "UTC date");
        num(d, "setMinutes", Env.n(61));
        Check.equal(1.0, num(d, "getMinutes"), "minutes overflow");
        num(d, "setSeconds", Env.n(-1));
        Check.equal(59.0, num(d, "getSeconds"), "negative seconds roll back");
        num(d, "setMilliseconds", Env.n(1500));
        Check.equal(500.0, num(d, "getMilliseconds"), "milliseconds overflow");
        Check.equal(5.0, num(d, "setTime", Env.n(5)), "setTime");
        Check.equal(5.0, num(d, "valueOf"), "valueOf");
        Check.isTrue(Double.isNaN(num(d, "setTime", Env.n(Double.NaN))), "setTime(NaN)");
        Check.isTrue(Double.isNaN(num(d, "setTime", Env.n(8.64e15 + 1))), "setTime clips");
    }

    /**
     * NaN rules of the setters: an invalid date stays invalid except for setFullYear, and every argument is converted anyway.
     * <p>
     * setter 의 NaN 규칙: 잘못된 날짜는 setFullYear 를 제외하고 잘못된 채로 있고, 인수는 어쨌든 모두 변환한다.
     */
    public void testSettersOnInvalidDates() {
        JsObject d = date(Double.NaN);
        Check.isTrue(Double.isNaN(num(d, "setHours", Env.n(1))), "setHours on an invalid date");
        Check.isTrue(Double.isNaN(num(d, "setMonth", Env.n(1), Env.n(1))), "setMonth on an invalid date");
        Check.isTrue(Double.isNaN(num(d, "setDate", Env.n(1))), "setDate on an invalid date");
        double r = num(d, "setFullYear", Env.n(2000));
        Check.equal(946684800000.0, r, "setFullYear revives an invalid date as 1 January 00:00 of that year (UTC zone)");
        JsObject d2 = date(Double.NaN);
        final int[] calls = new int[2];
        JsObject counting = env.realm.newObject();
        env.realm.method(counting, "valueOf", 0, new NativeFunction.Call() {
            /**
             * Counts conversions.
             * <p>
             * 변환 횟수를 센다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the number 1 / 숫자 1
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                return Env.n(1);
            }
        });
        num(d2, "setHours", counting, counting, counting, counting);
        Check.equal(4, calls[0], "all four arguments are converted even though the date is invalid");
        Check.isTrue(Double.isNaN(num(d2, "getTime")), "still invalid");
        Check.isTrue(Double.isNaN(num(date(0), "setSeconds", Env.n(Double.NaN))), "NaN argument gives NaN");
    }

    /**
     * Receivers that are not Dates are rejected, and Date.prototype itself is not a Date (ES2015).
     * <p>
     * Date 가 아닌 수신 객체는 거부되며 Date.prototype 자체도 Date 가 아니다(ES2015).
     */
    public void testReceiverChecks() {
        final JsObject proto = env.obj("Date.prototype");
        String[] methods = { "getTime", "valueOf", "getHours", "setHours", "toString", "toISOString", "getYear", "setYear", "getTimezoneOffset" };
        // 메서드마다 Date 가 아닌 수신 객체에서 TypeError 가 나는지 확인한다.
        for (int i = 0; i < methods.length; i++) {
            final String m = methods[i];
            Env.expect(ErrorKind.TYPE_ERROR, new Block() {
                /**
                 * Calls the method on a plain object.
                 * <p>
                 * 일반 객체에서 메서드를 호출한다.
                 */
                public void run() {
                    env.invoke(proto, m, Env.n(1));
                }
            }, m + " on Date.prototype");
            Env.expect(ErrorKind.TYPE_ERROR, new Block() {
                /**
                 * Calls the method on a number.
                 * <p>
                 * 숫자에서 메서드를 호출한다.
                 */
                public void run() {
                    env.call("Date.prototype." + m, Env.n(5), Env.n(1));
                }
            }, m + " on a number");
        }
    }

    /**
     * The formatting methods: ISO, UTC and local forms, Invalid Date, RangeError of toISOString and toGMTString identity.
     * <p>
     * 서식 메서드: ISO, UTC, 지역 형태, Invalid Date, toISOString 의 RangeError, toGMTString 동일성.
     */
    public void testFormatting() {
        JsObject d = date(1456795230040.0);
        Check.equal("2016-03-01T01:20:30.040Z", Env.str(env.invoke(d, "toISOString")), "toISOString");
        Check.equal("Tue, 01 Mar 2016 01:20:30 GMT", Env.str(env.invoke(d, "toUTCString")), "toUTCString");
        Check.equal("Tue Mar 01 2016", Env.str(env.invoke(d, "toDateString")), "toDateString");
        Check.contains(Env.str(env.invoke(d, "toTimeString")), "01:20:30 GMT+0000", "toTimeString");
        Check.contains(Env.str(env.invoke(d, "toString")), "Tue Mar 01 2016 01:20:30 GMT+0000", "toString");
        Check.isTrue(Env.str(env.invoke(d, "toLocaleString")).length() > 5, "toLocaleString");
        Check.isTrue(Env.str(env.invoke(d, "toLocaleDateString")).length() > 3, "toLocaleDateString");
        Check.isTrue(Env.str(env.invoke(d, "toLocaleTimeString")).length() > 3, "toLocaleTimeString");
        Check.same(env.get("Date.prototype.toUTCString"), env.get("Date.prototype.toGMTString"), "toGMTString is the same function object");
        final JsObject bad = date(Double.NaN);
        Check.equal("Invalid Date", Env.str(env.invoke(bad, "toString")), "toString of an invalid date");
        Check.equal("Invalid Date", Env.str(env.invoke(bad, "toUTCString")), "toUTCString of an invalid date");
        Env.expect(ErrorKind.RANGE_ERROR, new Block() {
            /**
             * Formats an invalid date as ISO text.
             * <p>
             * 잘못된 날짜를 ISO 문자열로 서식화한다.
             */
            public void run() {
                env.invoke(bad, "toISOString");
            }
        }, "toISOString of an invalid date");
        Check.equal("-000001-01-01T00:00:00.000Z", Env.str(env.invoke(date(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(-1), Env.n(0), Env.n(1)))), "toISOString")), "negative year with six digits");
    }

    /**
     * toJSON returns null for non-finite dates and is generic for objects with toISOString.
     * <p>
     * toJSON 은 유한하지 않은 날짜에는 null 을 반환하고 toISOString 이 있는 객체에 대해 범용으로 동작한다.
     */
    public void testToJson() {
        Check.equal("2016-03-01T01:20:30.040Z", Env.str(env.invoke(date(1456795230040.0), "toJSON")), "toJSON of a date");
        Check.same(Js.NULL, env.invoke(date(Double.NaN), "toJSON"), "toJSON of an invalid date is null");
        JsObject generic = env.realm.newObject();
        generic.createDataProperty("valueOf", env.realm.newFunction("valueOf", 0, new NativeFunction.Call() {
            /**
             * Returns a finite number.
             * <p>
             * 유한한 숫자를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return 1 / 1
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Env.n(1);
            }
        }));
        generic.createDataProperty("toISOString", env.realm.newFunction("toISOString", 0, new NativeFunction.Call() {
            /**
             * Returns a fixed text.
             * <p>
             * 고정된 문자열을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return text / 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Env.s("generic");
            }
        }));
        Check.equal("generic", Env.str(env.call("Date.prototype.toJSON", generic)), "toJSON works on any object");
    }

    /**
     * [Symbol.toPrimitive]: default and string hints give text, number hint gives the time value, anything else is a TypeError.
     * <p>
     * [Symbol.toPrimitive]: 기본/문자열 힌트는 문자열, 숫자 힌트는 시간값을 주고 그 밖의 힌트는 TypeError 이다.
     */
    public void testToPrimitive() {
        final JsObject d = date(1000);
        Check.isTrue(Ops.toPrimitive(d, Ops.HINT_DEFAULT) instanceof JsString, "default hint prefers toString");
        Check.isTrue(Ops.toPrimitive(d, Ops.HINT_STRING) instanceof JsString, "string hint");
        Check.isTrue(Ops.toPrimitive(d, Ops.HINT_NUMBER) instanceof JsNumber, "number hint");
        Check.equal(1000.0, Env.d(Ops.toPrimitive(d, Ops.HINT_NUMBER)), "number hint value");
        final JsObject proto = env.obj("Date.prototype");
        final NativeFunction fn = (NativeFunction) proto.get(WellKnownSymbols.TO_PRIMITIVE);
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls the method with an invalid hint.
             * <p>
             * 잘못된 힌트로 메서드를 호출한다.
             */
            public void run() {
                fn.call(d, new JsValue[] { Env.s("bogus") });
            }
        }, "invalid hint");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls the method on a primitive receiver.
             * <p>
             * 원시 값 수신 객체로 메서드를 호출한다.
             */
            public void run() {
                fn.call(Env.n(1), new JsValue[] { Env.s("number") });
            }
        }, "primitive receiver");
        Check.equal("[Symbol.toPrimitive]", Env.str(fn.get("name")), "function name");
        Check.equal(1.0, Env.d(fn.get("length")), "function length");
        Check.isFalse(proto.getOwnProperty(WellKnownSymbols.TO_PRIMITIVE).isWritableTrue(), "not writable");
        Check.isTrue(proto.getOwnProperty(WellKnownSymbols.TO_PRIMITIVE).isConfigurableTrue(), "configurable");
        JsValue sum = Ops.add(d, Env.n(1));
        Check.isTrue(sum instanceof JsString, "date + number concatenates text");
    }

    /**
     * Annex B getYear and setYear.
     * <p>
     * 부속서 B getYear 와 setYear.
     */
    public void testAnnexBYear() {
        JsObject d = date(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2000), Env.n(5), Env.n(15))));
        num(d, "setYear", Env.n(99));
        Check.equal(1999.0, num(d, "getFullYear"), "setYear(99) means 1999");
        Check.equal(99.0, num(d, "getYear"), "getYear");
        num(d, "setYear", Env.n(2010));
        Check.equal(2010.0, num(d, "getFullYear"), "setYear(2010)");
        Check.isTrue(Double.isNaN(num(d, "setYear", Env.n(Double.NaN))), "setYear(NaN)");
        Check.isTrue(Double.isNaN(num(d, "getTime")), "date is now invalid");
        JsObject revived = date(Double.NaN);
        Check.isFalse(Double.isNaN(num(revived, "setYear", Env.n(101))), "setYear revives an invalid date");
        Check.equal(101.0, num(revived, "getFullYear"), "year 101 is not mapped to 1900..1999");
    }

    /**
     * Local time rules at a daylight saving gap: an unreachable local time moves forward (UTC conversion uses the offset before the change).
     * <p>
     * 서머타임 틈의 지역 시간 규칙: 존재하지 않는 지역 시간은 앞으로 이동한다(UTC 변환은 전환 이전 오프셋을 쓴다).
     */
    public void testDaylightSavingGap() {
        DateBuiltins.setTimeZone(env.realm, TimeZone.getTimeZone("America/New_York"));
        JsObject d = env.construct("Date", Env.n(2016), Env.n(2), Env.n(13), Env.n(2), Env.n(30));
        Check.equal(3.0, num(d, "getHours"), "02:30 on the spring-forward day does not exist and becomes 03:30");
        Check.equal(30.0, num(d, "getMinutes"), "minutes");
    }

    /**
     * Integer part rules of Date.UTC with fractional and huge arguments.
     * <p>
     * 소수와 매우 큰 인수를 준 Date.UTC 의 정수 부분 규칙.
     */
    public void testFractionalAndHugeArguments() {
        Check.equal(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2000), Env.n(0), Env.n(1))),
                Env.d(env.call("Date.UTC", env.get("Date"), Env.n(2000.9), Env.n(0.9), Env.n(1.9))), "fractions are truncated");
        Check.isTrue(Double.isNaN(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(Double.POSITIVE_INFINITY)))), "infinite year");
        Check.isTrue(Double.isNaN(Env.d(env.call("Date.UTC", env.get("Date"), Env.n(300000), Env.n(0)))), "year beyond the time range");
    }
}
