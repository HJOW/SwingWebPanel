package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Installs Object.groupBy and Map.groupBy using the common GroupBy operation.
 * <p>
 * 공통 GroupBy 연산을 사용하는 Object.groupBy와 Map.groupBy를 설치한다.
 */
public final class GroupByBuiltins {

    /** Upper bound for the index argument supplied to a callback. / 콜백에 전달하는 인덱스의 상한. */
    private static final long MAX_INDEX = 9007199254740991L;

    /** Only static functions are used. / 정적 함수만 사용한다. */
    private GroupByBuiltins() {
    }

    /**
     * Installs Object.groupBy on the realm's Object constructor.
     * <p>
     * realm의 Object 생성자에 Object.groupBy를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param constructor Object constructor / Object 생성자
     */
    public static void installObject(JsRealm realm, JsObject constructor) {
        realm.method(constructor, "groupBy", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                OrderedTable groups = new OrderedTable();
                group(r, Js.arg(args, 0), Js.arg(args, 1), true, groups);
                JsObject result = new JsObject(null);
                OrderedTable.Cursor cursor = groups.cursor();
                OrderedTable.Entry entry;
                // 처음 만든 그룹의 순서대로 일반 데이터 프로퍼티를 만든다.
                while ((entry = cursor.next()) != null) {
                    result.createDataProperty(Ops.toPropertyKey(entry.key), entry.value);
                }
                return result;
            }
        });
    }

    /**
     * Installs Map.groupBy on the realm's Map constructor.
     * <p>
     * realm의 Map 생성자에 Map.groupBy를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param constructor Map constructor / Map 생성자
     */
    static void installMap(JsRealm realm, JsObject constructor) {
        realm.method(constructor, "groupBy", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                CollectionBuiltins.MapObject result = new CollectionBuiltins.MapObject(r.getIntrinsic("%MapPrototype%"));
                group(r, Js.arg(args, 0), Js.arg(args, 1), false, result.table);
                return result;
            }
        });
    }

    /**
     * Groups an iterable by successive callback results and closes the iterator if the callback or property key conversion throws.
     * <p>
     * 콜백 반환값을 차례로 사용해 반복 가능한 값을 그룹으로 묶는다.
     * 콜백이나 프로퍼티 키 변환에서 오류가 나면 이터레이터를 닫는다.
     *
     * @param realm current realm / 현재 realm
     * @param items source iterable / 원본 반복 가능 값
     * @param callback grouping callback / 그룹을 정하는 콜백
     * @param propertyKeys whether to coerce the callback result to a property key / 콜백 반환값을 프로퍼티 키로 변환할지 여부
     * @param groups output table preserving first occurrence order / 처음 나타난 순서를 유지하는 출력 테이블
     */
    private static void group(JsRealm realm, JsValue items, JsValue callback, boolean propertyKeys, OrderedTable groups) {
        if (items.isNullOrUndefined()) {
            throw JsException.typeError("Cannot group null or undefined");
        }
        if (!callback.isCallable()) {
            throw JsException.typeError("GroupBy callback is not callable");
        }
        IteratorRecord iterator = IteratorRecord.from(realm, items);
        // 이터레이터를 한 번 획득해 값과 인덱스를 차례로 콜백에 넘긴다.
        for (long index = 0;; index++) {
            realm.getLimits().tick();
            if (index == MAX_INDEX) {
                iterator.closeOnError();
                throw JsException.typeError("GroupBy index exceeded the maximum safe integer");
            }
            JsValue value = iterator.stepValue();
            if (value == null) {
                return;
            }
            JsValue key;
            try {
                key = Ops.call(callback, Js.UNDEFINED, new JsValue[] { value, Js.num(index) });
                if (propertyKeys) {
                    Object propertyKey = Ops.toPropertyKey(key);
                    key = propertyKey instanceof JsSymbol ? (JsSymbol) propertyKey : Js.str((String) propertyKey);
                }
            } catch (JsException e) {
                iterator.closeOnError();
                throw e;
            }
            OrderedTable.Entry entry = groups.find(key);
            JsArray values;
            if (entry == null) {
                values = realm.newArray();
                // OrderedTable은 Map 키의 SameValueZero와 음의 0 정규화를 처리한다.
                groups.put(key, values);
            } else {
                values = (JsArray) entry.value;
            }
            values.add(value);
        }
    }
}
