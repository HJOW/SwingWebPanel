package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.JsWeakKey;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.CollectionBuiltins.MapObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.CollectionBuiltins.WeakMapObject;

/**
 * The Map and WeakMap getOrInsert methods and their computed variants.
 * <p>
 * Map과 WeakMap의 getOrInsert 및 콜백으로 값을 계산하는 메서드.
 */
final class MapUpserts {

    /** Uses static methods only. / 정적 메서드만 사용한다. */
    private MapUpserts() {
    }

    /**
     * Installs Map getOrInsert methods using canonical collection keys.
     * <p>
     * 컬렉션 키를 정규화하는 Map의 getOrInsert 메서드를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Map prototype / Map 프로토타입
     */
    static void installMap(JsRealm realm, JsObject prototype) {
        realm.method(prototype, "getOrInsert", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                MapObject map = CollectionBuiltins.thisMap(receiver, "getOrInsert");
                JsValue key = Js.arg(args, 0);
                OrderedTable.Entry entry = map.table.find(key);
                if (entry != null) {
                    return entry.value;
                }
                JsValue value = Js.arg(args, 1);
                map.table.put(key, value);
                return value;
            }
        });
        realm.method(prototype, "getOrInsertComputed", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                MapObject map = CollectionBuiltins.thisMap(receiver, "getOrInsertComputed");
                JsValue callback = requireCallback(Js.arg(args, 1));
                JsValue key = OrderedTable.canonicalKey(Js.arg(args, 0));
                OrderedTable.Entry entry = map.table.find(key);
                if (entry != null) {
                    return entry.value;
                }
                JsValue value = Ops.call(callback, Js.UNDEFINED, new JsValue[] { key });
                // 콜백이 키를 추가했으면 그 위치를 보존한 채 계산 결과로 덮어쓴다.
                map.table.put(key, value);
                return value;
            }
        });
    }

    /**
     * Installs WeakMap getOrInsert methods with weak key validation.
     * <p>
     * 약한 키를 검사하는 WeakMap의 getOrInsert 메서드를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype WeakMap prototype / WeakMap 프로토타입
     */
    static void installWeakMap(JsRealm realm, JsObject prototype) {
        realm.method(prototype, "getOrInsert", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                WeakMapObject map = CollectionBuiltins.thisWeakMap(receiver, "getOrInsert");
                JsWeakKey key = CollectionBuiltins.weakKey(Js.arg(args, 0), "weak map");
                JsValue entry = map.get(key);
                if (entry != null) {
                    return entry;
                }
                JsValue value = Js.arg(args, 1);
                map.put(key, value);
                return value;
            }
        });
        realm.method(prototype, "getOrInsertComputed", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                WeakMapObject map = CollectionBuiltins.thisWeakMap(receiver, "getOrInsertComputed");
                JsWeakKey key = CollectionBuiltins.weakKey(Js.arg(args, 0), "weak map");
                JsValue callback = requireCallback(Js.arg(args, 1));
                JsValue entry = map.get(key);
                if (entry != null) {
                    return entry;
                }
                JsValue value = Ops.call(callback, Js.UNDEFINED, new JsValue[] { key });
                map.put(key, value);
                return value;
            }
        });
    }

    /**
     * Validates a computed upsert callback before lookup.
     * <p>
     * 조회 전에 값을 계산할 콜백을 확인한다.
     *
     * @param callback callback argument / 콜백 인수
     * @return the callable argument / 호출 가능한 인수
     */
    private static JsValue requireCallback(JsValue callback) {
        if (!callback.isCallable()) {
            throw JsException.typeError("getOrInsertComputed callback is not callable");
        }
        return callback;
    }
}
