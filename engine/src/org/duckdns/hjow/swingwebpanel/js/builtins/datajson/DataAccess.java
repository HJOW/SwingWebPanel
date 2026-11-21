package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * A small public facade over the internal slots of the built-in <code>Date</code>, <code>RegExp</code>, <code>Map</code> and
 * <code>Set</code> objects, for host code that must read or create them without going through (changeable) script-visible methods: the
 * structured clone algorithm of the HTML Standard uses it. Every method works on the object itself, never calls a method that a page
 * script may have replaced.
 * <p>
 * 내장 <code>Date</code>, <code>RegExp</code>, <code>Map</code>, <code>Set</code> 객체의 내부 슬롯에 대한 작은 공개 창구. (바뀔 수 있는) 스크립트 공개
 * 메서드를 거치지 않고 읽거나 만들어야 하는 호스트 코드를 위한 것으로, HTML 표준의 구조화 복제 알고리즘이 쓴다. 모든 메서드는 객체 자체에 작용하며
 * 페이지 스크립트가 바꿨을 수 있는 메서드를 호출하지 않는다.
 */
public final class DataAccess {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DataAccess() {
    }

    /**
     * Tells whether a value is a <code>Date</code> object (it has the [[DateValue]] slot).
     * <p>
     * 값이 <code>Date</code> 객체([[DateValue]] 슬롯을 가진 것)인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a Date / Date 이면 true
     */
    public static boolean isDate(JsValue v) {
        return v instanceof DateObject;
    }

    /**
     * Returns the time value of a Date object (NaN for an invalid date).
     * <p>
     * Date 객체의 시간 값을 반환한다(잘못된 날짜이면 NaN).
     *
     * @param date the Date object / Date 객체
     * @return the time value in milliseconds / 시간 값(밀리초)
     */
    public static double dateValue(JsObject date) {
        return ((DateObject) date).getTime();
    }

    /**
     * Creates a Date object of a realm for a time value.
     * <p>
     * 시간 값으로 realm 의 Date 객체를 만든다.
     *
     * @param realm the realm / realm
     * @param time  the time value (NaN for an invalid date) / 시간 값(잘못된 날짜이면 NaN)
     * @return the Date object / Date 객체
     */
    public static JsObject newDate(JsRealm realm, double time) {
        return new DateObject(realm.getIntrinsic("%DatePrototype%"), time);
    }

    /**
     * Tells whether a value is an initialized <code>RegExp</code> object.
     * <p>
     * 값이 초기화된 <code>RegExp</code> 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a RegExp / RegExp 이면 true
     */
    public static boolean isRegExp(JsValue v) {
        return v instanceof RegExpObject && ((RegExpObject) v).isInitialized();
    }

    /**
     * Returns the pattern text that a RegExp object was created from.
     * <p>
     * RegExp 객체를 만든 패턴 문자열을 반환한다.
     *
     * @param regExp the RegExp object / RegExp 객체
     * @return the source text / 원문
     */
    public static String regExpSource(JsObject regExp) {
        return ((RegExpObject) regExp).getSource();
    }

    /**
     * Returns the flags that a RegExp object was created with.
     * <p>
     * RegExp 객체를 만들 때의 플래그를 반환한다.
     *
     * @param regExp the RegExp object / RegExp 객체
     * @return the flags text / 플래그 문자열
     */
    public static String regExpFlags(JsObject regExp) {
        return ((RegExpObject) regExp).getOriginalFlags();
    }

    /**
     * Creates a RegExp object of a realm with the intrinsic constructor of that realm (so the pattern is checked and compiled again).
     * <p>
     * 해당 realm 의 내장 생성자로 RegExp 객체를 만든다(패턴을 다시 검사하고 컴파일한다).
     *
     * @param realm  the realm / realm
     * @param source the pattern / 패턴
     * @param flags  the flags / 플래그
     * @return the RegExp object / RegExp 객체
     */
    public static JsObject newRegExp(JsRealm realm, String source, String flags) {
        JsObject ctor = realm.getIntrinsic("%RegExp%");
        return ctor.construct(new JsValue[] { org.duckdns.hjow.swingwebpanel.js.Js.str(source), org.duckdns.hjow.swingwebpanel.js.Js.str(flags) }, ctor);
    }

    /**
     * Tells whether a value is a <code>Map</code> object.
     * <p>
     * 값이 <code>Map</code> 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a Map / Map 이면 true
     */
    public static boolean isMap(JsValue v) {
        return v instanceof CollectionBuiltins.MapObject;
    }

    /**
     * Tells whether a value is a <code>Set</code> object.
     * <p>
     * 값이 <code>Set</code> 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a Set / Set 이면 true
     */
    public static boolean isSet(JsValue v) {
        return v instanceof CollectionBuiltins.SetObject;
    }

    /**
     * Returns a snapshot of the entries of a Map in insertion order, as pairs of key and value.
     * <p>
     * Map 의 항목을 삽입 순서대로 (키, 값) 쌍으로 복사해 반환한다.
     *
     * @param map the Map object / Map 객체
     * @return the pairs / 쌍 목록
     */
    public static List<JsValue[]> mapEntries(JsObject map) {
        List<JsValue[]> out = new ArrayList<JsValue[]>();
        OrderedTable.Cursor c = ((CollectionBuiltins.MapObject) map).table.cursor();
        OrderedTable.Entry e = c.next();
        // 살아 있는 항목을 순서대로 모은다.
        while (e != null) {
            out.add(new JsValue[] { e.key, e.value });
            e = c.next();
        }
        return out;
    }

    /**
     * Returns a snapshot of the values of a Set in insertion order.
     * <p>
     * Set 의 값을 삽입 순서대로 복사해 반환한다.
     *
     * @param set the Set object / Set 객체
     * @return the values / 값 목록
     */
    public static List<JsValue> setValues(JsObject set) {
        List<JsValue> out = new ArrayList<JsValue>();
        OrderedTable.Cursor c = ((CollectionBuiltins.SetObject) set).table.cursor();
        OrderedTable.Entry e = c.next();
        // 살아 있는 항목의 키를 순서대로 모은다.
        while (e != null) {
            out.add(e.key);
            e = c.next();
        }
        return out;
    }

    /**
     * Creates an empty Map of a realm.
     * <p>
     * realm 의 빈 Map 을 만든다.
     *
     * @param realm the realm / realm
     * @return the Map object / Map 객체
     */
    public static JsObject newMap(JsRealm realm) {
        return new CollectionBuiltins.MapObject(realm.getIntrinsic("%MapPrototype%"));
    }

    /**
     * Creates an empty Set of a realm.
     * <p>
     * realm 의 빈 Set 을 만든다.
     *
     * @param realm the realm / realm
     * @return the Set object / Set 객체
     */
    public static JsObject newSet(JsRealm realm) {
        return new CollectionBuiltins.SetObject(realm.getIntrinsic("%SetPrototype%"));
    }

    /**
     * Stores an entry in a Map the way <code>Map.prototype.set</code> does (keys compare with SameValueZero).
     * <p>
     * <code>Map.prototype.set</code> 처럼 Map 에 항목을 저장한다(키는 SameValueZero 로 비교한다).
     *
     * @param map   the Map object / Map 객체
     * @param key   the key / 키
     * @param value the value / 값
     */
    public static void mapPut(JsObject map, JsValue key, JsValue value) {
        ((CollectionBuiltins.MapObject) map).table.put(key, value);
    }

    /**
     * Adds a value to a Set the way <code>Set.prototype.add</code> does.
     * <p>
     * <code>Set.prototype.add</code> 처럼 Set 에 값을 추가한다.
     *
     * @param set   the Set object / Set 객체
     * @param value the value / 값
     */
    public static void setAdd(JsObject set, JsValue value) {
        ((CollectionBuiltins.SetObject) set).table.put(value, value);
    }
}
