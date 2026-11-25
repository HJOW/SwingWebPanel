package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.date.DateFormatter;
import org.duckdns.hjow.swingwebpanel.js.date.DateMath;
import org.duckdns.hjow.swingwebpanel.js.date.DateParser;
import org.duckdns.hjow.swingwebpanel.js.date.DateRangeException;

/**
 * The built-in <code>Date</code> constructor and <code>Date.prototype</code> (ECMA-262 6th edition 20.3) including the Annex B methods
 * getYear, setYear and toGMTString. The calendar arithmetic, parsing and formatting come from the package js.date; this class only
 * implements the JavaScript argument handling and receiver checks. The local time zone is <code>TimeZone.getDefault()</code> read at
 * every call unless the host (or a test) fixed one for the realm with {@link #setTimeZone(JsRealm, TimeZone)}.
 * <p>
 * Deviation from the 6th edition text, taken from later editions and all browsers: <code>Date.UTC(year)</code> treats a missing month as 0
 * instead of NaN.
 * <p>
 * 내장 <code>Date</code> 생성자와 <code>Date.prototype</code>(ECMA-262 6판 20.3). 부속서 B 의 getYear, setYear, toGMTString 을 포함한다.
 * 달력 연산, 해석, 서식은 js.date 패키지가 맡고 이 클래스는 JavaScript 인수 처리와 수신 객체 검사만 구현한다. 지역 시간대는 호스트(또는 시험)가
 * {@link #setTimeZone(JsRealm, TimeZone)} 으로 realm 에 고정하지 않는 한 호출마다 읽는 <code>TimeZone.getDefault()</code> 이다.
 * <p>
 * 6판 본문과 다른 점(이후 판과 모든 브라우저를 따름): <code>Date.UTC(year)</code> 는 월이 없으면 NaN 이 아니라 0 으로 본다.
 */
public final class DateBuiltins {

    /**
     * Time zones fixed for realms; entries disappear with their realm.
     * <p>
     * realm 에 고정한 시간대. realm 이 사라지면 항목도 사라진다.
     */
    private static final Map<JsRealm, TimeZone> ZONES = Collections.synchronizedMap(new WeakHashMap<JsRealm, TimeZone>());

    /**
     * Getter field code: day of month.
     * <p>
     * getter 필드 코드: 날짜.
     */
    private static final int F_DATE = 0;

    /**
     * Getter field code: day of week.
     * <p>
     * getter 필드 코드: 요일.
     */
    private static final int F_DAY = 1;

    /**
     * Getter field code: full year.
     * <p>
     * getter 필드 코드: 연도.
     */
    private static final int F_YEAR = 2;

    /**
     * Getter field code: hours.
     * <p>
     * getter 필드 코드: 시.
     */
    private static final int F_HOURS = 3;

    /**
     * Getter field code: milliseconds.
     * <p>
     * getter 필드 코드: 밀리초.
     */
    private static final int F_MS = 4;

    /**
     * Getter field code: minutes.
     * <p>
     * getter 필드 코드: 분.
     */
    private static final int F_MINUTES = 5;

    /**
     * Getter field code: month.
     * <p>
     * getter 필드 코드: 월.
     */
    private static final int F_MONTH = 6;

    /**
     * Getter field code: seconds.
     * <p>
     * getter 필드 코드: 초.
     */
    private static final int F_SECONDS = 7;

    /**
     * Setter kind: milliseconds.
     * <p>
     * setter 종류: 밀리초.
     */
    private static final int S_MS = 0;

    /**
     * Setter kind: seconds.
     * <p>
     * setter 종류: 초.
     */
    private static final int S_SECONDS = 1;

    /**
     * Setter kind: minutes.
     * <p>
     * setter 종류: 분.
     */
    private static final int S_MINUTES = 2;

    /**
     * Setter kind: hours.
     * <p>
     * setter 종류: 시.
     */
    private static final int S_HOURS = 3;

    /**
     * Setter kind: day of month.
     * <p>
     * setter 종류: 날짜.
     */
    private static final int S_DATE = 4;

    /**
     * Setter kind: month.
     * <p>
     * setter 종류: 월.
     */
    private static final int S_MONTH = 5;

    /**
     * Setter kind: full year.
     * <p>
     * setter 종류: 연도.
     */
    private static final int S_YEAR = 6;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DateBuiltins() {
    }

    /**
     * Fixes the local time zone used by the Date objects of a realm; null restores the default (<code>TimeZone.getDefault()</code> at each call).
     * <p>
     * realm 의 Date 객체가 쓰는 지역 시간대를 고정한다. null 이면 기본값(호출마다 <code>TimeZone.getDefault()</code>)으로 되돌린다.
     *
     * @param realm the realm / realm
     * @param tz    time zone or null / 시간대 또는 null
     */
    public static void setTimeZone(JsRealm realm, TimeZone tz) {
        if (tz == null) {
            ZONES.remove(realm);
        } else {
            ZONES.put(realm, tz);
        }
    }

    /**
     * Returns the local time zone of a realm.
     * <p>
     * realm 의 지역 시간대를 반환한다.
     *
     * @param realm the realm / realm
     * @return the time zone / 시간대
     */
    public static TimeZone getTimeZone(JsRealm realm) {
        TimeZone tz = ZONES.get(realm);
        return tz == null ? TimeZone.getDefault() : tz;
    }

    /**
     * Requires a Date receiver.
     * <p>
     * Date 수신 객체임을 확인한다.
     *
     * @param v the this value / this 값
     * @return the date object / 날짜 객체
     */
    private static DateObject thisDate(JsValue v) {
        if (!(v instanceof DateObject)) {
            throw JsException.typeError("this is not a Date object.");
        }
        return (DateObject) v;
    }

    /**
     * Returns the current time in milliseconds as a double.
     * <p>
     * 현재 시각을 밀리초 double 로 반환한다.
     *
     * @return now / 현재 시각
     */
    private static double now() {
        return (double) System.currentTimeMillis();
    }

    /**
     * Installs <code>Date</code> into a realm.
     * <p>
     * realm 에 <code>Date</code> 를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(final JsRealm realm) {
        JsObject proto = realm.newObject();
        NativeFunction ctor = Support.makeConstructor(realm, "Date", 7, proto, new NativeFunction.Call() {
            /**
             * Date() called as a function returns the current time as text.
             * <p>
             * 함수로 호출한 Date() 는 현재 시각을 문자열로 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments (ignored) / 인수(무시)
             * @return text of the current time / 현재 시각 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(DateFormatter.toString(now(), getTimeZone(r)));
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a Date object from the arguments.
             * <p>
             * 인수로 Date 객체를 만든다.
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the new date / 새 날짜
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                double tv = constructValue(r, args);
                JsObject p = Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%DatePrototype%"));
                return new DateObject(p, tv);
            }
        });
        installStatics(realm, ctor);
        installGetters(realm, proto);
        installSetters(realm, proto);
        installFormatters(realm, proto);
    }

    /**
     * Computes the time value for <code>new Date(...args)</code> (ECMA-262 20.3.2.1).
     * <p>
     * <code>new Date(...args)</code> 의 시간값을 계산한다(ECMA-262 20.3.2.1).
     *
     * @param r    realm / realm
     * @param args arguments / 인수
     * @return the time value / 시간값
     */
    private static double constructValue(JsRealm r, JsValue[] args) {
        if (args.length == 0) {
            return now();
        }
        if (args.length == 1) {
            JsValue v = args[0];
            if (v instanceof DateObject) {
                return DateMath.timeClip(((DateObject) v).getTime());
            }
            JsValue prim = Ops.toPrimitive(v, Ops.HINT_DEFAULT);
            if (prim instanceof JsString) {
                return DateMath.timeClip(DateParser.parse(((JsString) prim).getValue(), getTimeZone(r)));
            }
            return DateMath.timeClip(Ops.toNumber(prim));
        }
        double[] f = components(args);
        double finalDate = DateMath.makeDate(DateMath.makeDay(f[0], f[1], f[2]), DateMath.makeTime(f[3], f[4], f[5], f[6]));
        return DateMath.timeClip(DateMath.utc(finalDate, getTimeZone(r)));
    }

    /**
     * Converts the arguments of the multi-argument constructor and of Date.UTC: year (two-digit years map to 1900..1999), month, day (default 1),
     * hours, minutes, seconds, milliseconds. All conversions happen in argument order even when an earlier value is NaN.
     * <p>
     * 인수가 여럿인 생성자와 Date.UTC 의 인수를 변환한다: 연(두 자리 연도는 1900..1999), 월, 일(기본 1), 시, 분, 초, 밀리초.
     * 앞의 값이 NaN 이어도 모든 변환은 인수 순서대로 수행한다.
     *
     * @param args arguments / 인수
     * @return seven numbers: year, month, day, hours, minutes, seconds, milliseconds / 숫자 일곱 개
     */
    private static double[] components(JsValue[] args) {
        double y = Ops.toNumber(Js.arg(args, 0));
        double m = args.length > 1 ? Ops.toNumber(args[1]) : 0;
        double dt = args.length > 2 ? Ops.toNumber(args[2]) : 1;
        double h = args.length > 3 ? Ops.toNumber(args[3]) : 0;
        double min = args.length > 4 ? Ops.toNumber(args[4]) : 0;
        double s = args.length > 5 ? Ops.toNumber(args[5]) : 0;
        double milli = args.length > 6 ? Ops.toNumber(args[6]) : 0;
        double yr = y;
        if (!Double.isNaN(y)) {
            double yi = Ops.toInteger(y);
            if (yi >= 0 && yi <= 99) {
                yr = 1900 + yi;
            }
        }
        return new double[] { yr, m, dt, h, min, s, milli };
    }

    /**
     * Installs Date.now, Date.parse and Date.UTC.
     * <p>
     * Date.now, Date.parse, Date.UTC 를 설치한다.
     *
     * @param realm the realm / realm
     * @param ctor  the constructor / 생성자
     */
    private static void installStatics(JsRealm realm, NativeFunction ctor) {
        realm.method(ctor, "now", 0, new NativeFunction.Call() {
            /**
             * Returns the current time value.
             * <p>
             * 현재 시간값을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return milliseconds since the epoch / 에포크 이후 밀리초
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(now());
            }
        });
        realm.method(ctor, "parse", 1, new NativeFunction.Call() {
            /**
             * Parses a date text.
             * <p>
             * 날짜 문자열을 해석한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the time value or NaN / 시간값 또는 NaN
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(DateParser.parse(Ops.toString(Js.arg(args, 0)), getTimeZone(r)));
            }
        });
        realm.method(ctor, "UTC", 7, new NativeFunction.Call() {
            /**
             * Computes the UTC time value of calendar fields.
             * <p>
             * 달력 필드의 UTC 시간값을 계산한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the time value / 시간값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double[] f = components(args);
                return Js.num(DateMath.timeClip(DateMath.makeDate(DateMath.makeDay(f[0], f[1], f[2]), DateMath.makeTime(f[3], f[4], f[5], f[6]))));
            }
        });
    }

    /**
     * A getter method of Date.prototype.
     * <p>
     * Date.prototype 의 getter 메서드.
     */
    private static final class Getter implements NativeFunction.Call {
        /**
         * Field code (one of the F_ constants).
         * <p>
         * 필드 코드(F_ 상수 중 하나).
         */
        private final int field;

        /**
         * True for local time, false for UTC.
         * <p>
         * 지역 시간이면 true, UTC 이면 false.
         */
        private final boolean local;

        /**
         * Creates a getter.
         * <p>
         * getter 를 만든다.
         *
         * @param field field code / 필드 코드
         * @param local local time or UTC / 지역 시간 또는 UTC
         */
        Getter(int field, boolean local) {
            this.field = field;
            this.local = local;
        }

        /**
         * Reads the field of the receiver's time value.
         * <p>
         * 수신 객체 시간값의 필드를 읽는다.
         *
         * @param r         realm / realm
         * @param thisValue the date / 날짜
         * @param args      arguments (ignored) / 인수(무시)
         * @return the field or NaN / 필드 값 또는 NaN
         */
        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
            double t = thisDate(thisValue).getTime();
            if (Double.isNaN(t)) {
                return JsNumber.NAN;
            }
            if (local) {
                t = DateMath.localTime(t, getTimeZone(r));
            }
            switch (field) {
            case F_DATE:
                return Js.num(DateMath.dateFromTime(t));
            case F_DAY:
                return Js.num(DateMath.weekDay(t));
            case F_YEAR:
                return Js.num(DateMath.yearFromTime(t));
            case F_HOURS:
                return Js.num(DateMath.hourFromTime(t));
            case F_MS:
                return Js.num(DateMath.msFromTime(t));
            case F_MINUTES:
                return Js.num(DateMath.minFromTime(t));
            case F_MONTH:
                return Js.num(DateMath.monthFromTime(t));
            default:
                return Js.num(DateMath.secFromTime(t));
            }
        }
    }

    /**
     * Defines one local and one UTC getter.
     * <p>
     * 지역 getter 하나와 UTC getter 하나를 정의한다.
     *
     * @param realm  the realm / realm
     * @param proto  Date.prototype / Date.prototype
     * @param suffix property suffix such as "Date" / "Date" 같은 이름 접미사
     * @param field  field code / 필드 코드
     */
    private static void getterPair(JsRealm realm, JsObject proto, String suffix, int field) {
        realm.method(proto, "get" + suffix, 0, new Getter(field, true));
        realm.method(proto, "getUTC" + suffix, 0, new Getter(field, false));
    }

    /**
     * Installs the getters, getTime, valueOf, getTimezoneOffset and getYear.
     * <p>
     * getter 들과 getTime, valueOf, getTimezoneOffset, getYear 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Date.prototype / Date.prototype
     */
    private static void installGetters(JsRealm realm, JsObject proto) {
        getterPair(realm, proto, "Date", F_DATE);
        getterPair(realm, proto, "Day", F_DAY);
        getterPair(realm, proto, "FullYear", F_YEAR);
        getterPair(realm, proto, "Hours", F_HOURS);
        getterPair(realm, proto, "Milliseconds", F_MS);
        getterPair(realm, proto, "Minutes", F_MINUTES);
        getterPair(realm, proto, "Month", F_MONTH);
        getterPair(realm, proto, "Seconds", F_SECONDS);
        NativeFunction.Call raw = new NativeFunction.Call() {
            /**
             * Returns the time value.
             * <p>
             * 시간값을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the date / 날짜
             * @param args      arguments (ignored) / 인수(무시)
             * @return the time value / 시간값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisDate(thisValue).getTime());
            }
        };
        realm.method(proto, "getTime", 0, raw);
        realm.method(proto, "valueOf", 0, raw);
        realm.method(proto, "getTimezoneOffset", 0, new NativeFunction.Call() {
            /**
             * Returns the difference between UTC and local time in minutes.
             * <p>
             * UTC 와 지역 시간의 차이를 분 단위로 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the date / 날짜
             * @param args      arguments (ignored) / 인수(무시)
             * @return offset in minutes or NaN / 분 단위 차이 또는 NaN
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double t = thisDate(thisValue).getTime();
                if (Double.isNaN(t)) {
                    return JsNumber.NAN;
                }
                return Js.num((t - DateMath.localTime(t, getTimeZone(r))) / DateMath.MS_PER_MINUTE);
            }
        });
        realm.method(proto, "getYear", 0, new NativeFunction.Call() {
            /**
             * Annex B getYear: the year minus 1900.
             * <p>
             * 부속서 B getYear: 연도에서 1900 을 뺀 값.
             *
             * @param r         realm / realm
             * @param thisValue the date / 날짜
             * @param args      arguments (ignored) / 인수(무시)
             * @return year - 1900 or NaN / 연도 - 1900 또는 NaN
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                double t = thisDate(thisValue).getTime();
                if (Double.isNaN(t)) {
                    return JsNumber.NAN;
                }
                return Js.num(DateMath.yearFromTime(DateMath.localTime(t, getTimeZone(r))) - 1900);
            }
        });
    }

    /**
     * A setter method of Date.prototype (setMilliseconds ... setFullYear, local and UTC).
     * <p>
     * Date.prototype 의 setter 메서드(setMilliseconds ... setFullYear, 지역/UTC).
     */
    private static final class Setter implements NativeFunction.Call {
        /**
         * Setter kind (one of the S_ constants).
         * <p>
         * setter 종류(S_ 상수 중 하나).
         */
        private final int kind;

        /**
         * True for local time, false for UTC.
         * <p>
         * 지역 시간이면 true, UTC 이면 false.
         */
        private final boolean local;

        /**
         * Creates a setter.
         * <p>
         * setter 를 만든다.
         *
         * @param kind  setter kind / setter 종류
         * @param local local time or UTC / 지역 시간 또는 UTC
         */
        Setter(int kind, boolean local) {
            this.kind = kind;
            this.local = local;
        }

        /**
         * Converts an optional argument: its number if present, otherwise the given default.
         * <p>
         * 선택 인수를 변환한다: 있으면 그 숫자, 없으면 주어진 기본값.
         *
         * @param args arguments / 인수
         * @param i    argument index / 인수 위치
         * @param def  value when missing / 없을 때의 값
         * @return the number / 숫자
         */
        private static double optional(JsValue[] args, int i, double def) {
            return args.length > i ? Ops.toNumber(args[i]) : def;
        }

        /**
         * Applies the setter.
         * <p>
         * setter 를 적용한다.
         *
         * @param r         realm / realm
         * @param thisValue the date / 날짜
         * @param args      arguments / 인수
         * @return the new time value / 새 시간값
         */
        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
            DateObject d = thisDate(thisValue);
            TimeZone tz = getTimeZone(r);
            double tv = d.getTime();
            double t = tv;
            if (kind == S_YEAR) {
                t = Double.isNaN(tv) ? 0 : (local ? DateMath.localTime(tv, tz) : tv);
            } else if (local) {
                t = DateMath.localTime(tv, tz);
            }
            double date;
            switch (kind) {
            case S_MS:
                date = DateMath.makeDate(DateMath.day(t), DateMath.makeTime(DateMath.hourFromTime(t), DateMath.minFromTime(t),
                        DateMath.secFromTime(t), Ops.toNumber(Js.arg(args, 0))));
                break;
            case S_SECONDS: {
                double s = Ops.toNumber(Js.arg(args, 0));
                double milli = optional(args, 1, DateMath.msFromTime(t));
                date = DateMath.makeDate(DateMath.day(t), DateMath.makeTime(DateMath.hourFromTime(t), DateMath.minFromTime(t), s, milli));
                break;
            }
            case S_MINUTES: {
                double m = Ops.toNumber(Js.arg(args, 0));
                double s = optional(args, 1, DateMath.secFromTime(t));
                double milli = optional(args, 2, DateMath.msFromTime(t));
                date = DateMath.makeDate(DateMath.day(t), DateMath.makeTime(DateMath.hourFromTime(t), m, s, milli));
                break;
            }
            case S_HOURS: {
                double h = Ops.toNumber(Js.arg(args, 0));
                double m = optional(args, 1, DateMath.minFromTime(t));
                double s = optional(args, 2, DateMath.secFromTime(t));
                double milli = optional(args, 3, DateMath.msFromTime(t));
                date = DateMath.makeDate(DateMath.day(t), DateMath.makeTime(h, m, s, milli));
                break;
            }
            case S_DATE: {
                double dt = Ops.toNumber(Js.arg(args, 0));
                date = DateMath.makeDate(DateMath.makeDay(DateMath.yearFromTime(t), DateMath.monthFromTime(t), dt), DateMath.timeWithinDay(t));
                break;
            }
            case S_MONTH: {
                double m = Ops.toNumber(Js.arg(args, 0));
                double dt = optional(args, 1, DateMath.dateFromTime(t));
                date = DateMath.makeDate(DateMath.makeDay(DateMath.yearFromTime(t), m, dt), DateMath.timeWithinDay(t));
                break;
            }
            default: {
                double y = Ops.toNumber(Js.arg(args, 0));
                double m = optional(args, 1, DateMath.monthFromTime(t));
                double dt = optional(args, 2, DateMath.dateFromTime(t));
                date = DateMath.makeDate(DateMath.makeDay(y, m, dt), DateMath.timeWithinDay(t));
                break;
            }
            }
            double u = DateMath.timeClip(local ? DateMath.utc(date, tz) : date);
            d.setTime(u);
            return Js.num(u);
        }
    }

    /**
     * Defines the local and the UTC setter of a field.
     * <p>
     * 필드의 지역 setter 와 UTC setter 를 정의한다.
     *
     * @param realm  the realm / realm
     * @param proto  Date.prototype / Date.prototype
     * @param suffix name suffix / 이름 접미사
     * @param kind   setter kind / setter 종류
     * @param length number of expected arguments / 기대 인수 개수
     */
    private static void setterPair(JsRealm realm, JsObject proto, String suffix, int kind, int length) {
        realm.method(proto, "set" + suffix, length, new Setter(kind, true));
        realm.method(proto, "setUTC" + suffix, length, new Setter(kind, false));
    }

    /**
     * Installs the setters, setTime and setYear.
     * <p>
     * setter 들과 setTime, setYear 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Date.prototype / Date.prototype
     */
    private static void installSetters(JsRealm realm, JsObject proto) {
        setterPair(realm, proto, "Milliseconds", S_MS, 1);
        setterPair(realm, proto, "Seconds", S_SECONDS, 2);
        setterPair(realm, proto, "Minutes", S_MINUTES, 3);
        setterPair(realm, proto, "Hours", S_HOURS, 4);
        setterPair(realm, proto, "Date", S_DATE, 1);
        setterPair(realm, proto, "Month", S_MONTH, 2);
        setterPair(realm, proto, "FullYear", S_YEAR, 3);
        realm.method(proto, "setTime", 1, new NativeFunction.Call() {
            /**
             * Sets the time value.
             * <p>
             * 시간값을 지정한다.
             *
             * @param r         realm / realm
             * @param thisValue the date / 날짜
             * @param args      arguments / 인수
             * @return the new time value / 새 시간값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                DateObject d = thisDate(thisValue);
                double v = DateMath.timeClip(Ops.toNumber(Js.arg(args, 0)));
                d.setTime(v);
                return Js.num(v);
            }
        });
        realm.method(proto, "setYear", 1, new NativeFunction.Call() {
            /**
             * Annex B setYear: two-digit years mean 1900..1999.
             * <p>
             * 부속서 B setYear: 두 자리 연도는 1900..1999 를 뜻한다.
             *
             * @param r         realm / realm
             * @param thisValue the date / 날짜
             * @param args      arguments / 인수
             * @return the new time value / 새 시간값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                DateObject d = thisDate(thisValue);
                TimeZone tz = getTimeZone(r);
                double tv = d.getTime();
                double t = Double.isNaN(tv) ? 0 : DateMath.localTime(tv, tz);
                double y = Ops.toNumber(Js.arg(args, 0));
                if (Double.isNaN(y)) {
                    d.setTime(Double.NaN);
                    return JsNumber.NAN;
                }
                double yi = Ops.toInteger(y);
                double yyyy = yi >= 0 && yi <= 99 ? yi + 1900 : y;
                double day = DateMath.makeDay(yyyy, DateMath.monthFromTime(t), DateMath.dateFromTime(t));
                double u = DateMath.timeClip(DateMath.utc(DateMath.makeDate(day, DateMath.timeWithinDay(t)), tz));
                d.setTime(u);
                return Js.num(u);
            }
        });
    }

    /**
     * A formatting method of Date.prototype.
     * <p>
     * Date.prototype 의 서식 메서드.
     */
    private static final class Formatter implements NativeFunction.Call {
        /**
         * Which format: 0 toString, 1 toDateString, 2 toTimeString, 3 toUTCString, 4 toISOString, 5 toLocaleString, 6 toLocaleDateString,
         * 7 toLocaleTimeString.
         * <p>
         * 서식 종류: 0 toString, 1 toDateString, 2 toTimeString, 3 toUTCString, 4 toISOString, 5 toLocaleString, 6 toLocaleDateString,
         * 7 toLocaleTimeString.
         */
        private final int kind;

        /**
         * Creates a formatting method.
         * <p>
         * 서식 메서드를 만든다.
         *
         * @param kind format kind / 서식 종류
         */
        Formatter(int kind) {
            this.kind = kind;
        }

        /**
         * Formats the receiver's time value.
         * <p>
         * 수신 객체의 시간값을 서식화한다.
         *
         * @param r         realm / realm
         * @param thisValue the date / 날짜
         * @param args      arguments (ignored) / 인수(무시)
         * @return the text / 문자열
         */
        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
            double t = thisDate(thisValue).getTime();
            TimeZone tz = getTimeZone(r);
            try {
                switch (kind) {
                case 0:
                    return Js.str(DateFormatter.toString(t, tz));
                case 1:
                    return Js.str(DateFormatter.toDateString(t, tz));
                case 2:
                    return Js.str(DateFormatter.toTimeString(t, tz));
                case 3:
                    return Js.str(DateFormatter.toUTCString(t));
                case 4:
                    return Js.str(DateFormatter.toISOString(t));
                case 5:
                    return Js.str(DateFormatter.toLocaleString(t, tz));
                case 6:
                    return Js.str(DateFormatter.toLocaleDateString(t, tz));
                default:
                    return Js.str(DateFormatter.toLocaleTimeString(t, tz));
                }
            } catch (DateRangeException e) {
                throw JsException.rangeError(e.getMessage());
            }
        }
    }

    /**
     * Installs the toString family, toJSON, toGMTString and [Symbol.toPrimitive].
     * <p>
     * toString 계열, toJSON, toGMTString, [Symbol.toPrimitive] 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Date.prototype / Date.prototype
     */
    private static void installFormatters(JsRealm realm, JsObject proto) {
        realm.method(proto, "toString", 0, new Formatter(0));
        realm.method(proto, "toDateString", 0, new Formatter(1));
        realm.method(proto, "toTimeString", 0, new Formatter(2));
        NativeFunction utc = realm.method(proto, "toUTCString", 0, new Formatter(3));
        proto.defineBuiltin("toGMTString", utc);
        realm.method(proto, "toISOString", 0, new Formatter(4));
        realm.method(proto, "toLocaleString", 0, new Formatter(5));
        realm.method(proto, "toLocaleDateString", 0, new Formatter(6));
        realm.method(proto, "toLocaleTimeString", 0, new Formatter(7));
        realm.method(proto, "toJSON", 1, new NativeFunction.Call() {
            /**
             * Returns the ISO text of a finite date, or null.
             * <p>
             * 유한한 날짜의 ISO 문자열을 반환하고 아니면 null 을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the receiver (any object) / 수신 객체(모든 객체)
             * @param args      arguments (ignored) / 인수(무시)
             * @return text or null / 문자열 또는 null
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Ops.toObject(r, thisValue);
                JsValue tv = Ops.toPrimitive(o, Ops.HINT_NUMBER);
                if (tv instanceof JsNumber && (Double.isNaN(((JsNumber) tv).getValue()) || Double.isInfinite(((JsNumber) tv).getValue()))) {
                    return Js.NULL;
                }
                return Ops.invoke(r, o, "toISOString", Js.NO_ARGS);
            }
        });
        NativeFunction toPrim = realm.newFunction("[Symbol.toPrimitive]", 1, new NativeFunction.Call() {
            /**
             * Date.prototype[@@toPrimitive]: "default" and "string" prefer toString, "number" prefers valueOf.
             * <p>
             * Date.prototype[@@toPrimitive]: "default" 와 "string" 은 toString 을, "number" 는 valueOf 를 먼저 쓴다.
             *
             * @param r         realm / realm
             * @param thisValue the receiver / 수신 객체
             * @param args      arguments: the hint / 인수: 힌트
             * @return the primitive / 원시 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Support.requireObject(thisValue, "Date.prototype[Symbol.toPrimitive]");
                JsValue hint = Js.arg(args, 0);
                String h = hint instanceof JsString ? ((JsString) hint).getValue() : null;
                if ("string".equals(h) || "default".equals(h)) {
                    return Ops.ordinaryToPrimitive(o, true);
                }
                if ("number".equals(h)) {
                    return Ops.ordinaryToPrimitive(o, false);
                }
                throw JsException.typeError("Invalid hint: " + (h == null ? "undefined" : h));
            }
        });
        proto.defineData(WellKnownSymbols.TO_PRIMITIVE, toPrim, false, false, true);
    }
}
