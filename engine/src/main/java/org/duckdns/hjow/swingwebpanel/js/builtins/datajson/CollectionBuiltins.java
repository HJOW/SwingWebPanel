package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.IdentityHashMap;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.JsWeakKey;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The keyed collections <code>Map</code>, <code>Set</code>, <code>WeakMap</code> and <code>WeakSet</code> (ECMA-262 6th edition 23) with
 * their iterators. Map and Set keep their entries in an {@link OrderedTable} (SameValueZero keys, insertion order, correct behavior when
 * entries are added or removed during iteration). WeakMap and WeakSet accept objects and non-registered symbols.
 * WeakMap associations for collectable keys live with their keys, including when an associated value refers back to the key.
 * <p>
 * 키로 구분하는 컬렉션 <code>Map</code>, <code>Set</code>, <code>WeakMap</code>, <code>WeakSet</code>(ECMA-262 6판 23)과 그 이터레이터.
 * Map 과 Set 은 항목을 {@link OrderedTable}(SameValueZero 키, 삽입 순서, 순회 중 항목을 추가/삭제해도 올바른 동작)에 보관한다.
 * WeakMap과 WeakSet은 객체와 비등록 심볼을 받는다. 수거 가능한 WeakMap 키에 값을 붙여 값이 키를 참조하는 경우에도 키를 수거할 수 있다.
 */
final class CollectionBuiltins {

    /**
     * Iterator kind: keys.
     * <p>
     * 이터레이터 종류: 키.
     */
    private static final int KIND_KEYS = 0;

    /**
     * Iterator kind: values.
     * <p>
     * 이터레이터 종류: 값.
     */
    private static final int KIND_VALUES = 1;

    /**
     * Iterator kind: [key, value] pairs.
     * <p>
     * 이터레이터 종류: [키, 값] 쌍.
     */
    private static final int KIND_ENTRIES = 2;

    /**
     * A Map instance.
     * <p>
     * Map 인스턴스.
     */
    static final class MapObject extends JsObject {
        /**
         * The entries.
         * <p>
         * 항목들.
         */
        final OrderedTable table = new OrderedTable();

        /**
         * Creates a map object.
         * <p>
         * Map 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        MapObject(JsObject proto) {
            super(proto);
        }

        /**
         * Returns "Map".
         * <p>
         * "Map" 을 반환한다.
         *
         * @return the class name / 클래스 이름
         */
        public String className() {
            return "Map";
        }
    }

    /**
     * A Set instance.
     * <p>
     * Set 인스턴스.
     */
    static final class SetObject extends JsObject {
        /**
         * The entries (the value of an entry is its key).
         * <p>
         * 항목들(항목의 값은 그 키).
         */
        final OrderedTable table = new OrderedTable();

        /**
         * Creates a set object.
         * <p>
         * Set 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        SetObject(JsObject proto) {
            super(proto);
        }

        /**
         * Returns "Set".
         * <p>
         * "Set" 을 반환한다.
         *
         * @return the class name / 클래스 이름
         */
        public String className() {
            return "Set";
        }
    }

    /**
     * A WeakMap instance.
     * <p>
     * WeakMap 인스턴스.
     */
    static final class WeakMapObject extends JsObject {
        /**
         * Private identity used to look up this map's associations on each key.
         * <p>
         * 각 키에서 이 맵의 항목을 찾을 비공개 식별자.
         */
        private final Object token = new Object();

        /** Entries whose symbol keys are already rooted for the lifetime of the engine. / 엔진 수명 동안 유지되는 심볼 키의 항목. */
        private IdentityHashMap<JsValue, JsValue> permanentEntries;

        /**
         * Creates a weak map object.
         * <p>
         * WeakMap 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        WeakMapObject(JsObject proto) {
            super(proto);
        }

        /**
         * Looks up an entry without retaining its key.
         * <p>
         * 키를 유지하지 않고 연결된 값을 찾는다.
         *
         * @param key lookup key / 조회할 키
         * @return the entry value or Java null if absent or ineligible / 항목 값. 항목이 없거나 키를 쓸 수 없으면 Java null
         */
        JsValue get(JsValue key) {
            if (isPermanentSymbol(key)) {
                return permanentEntries == null ? null : permanentEntries.get(key);
            }
            return Ops.canBeHeldWeakly(key) ? ((JsWeakKey) key).getWeakAssociation(token) : null;
        }

        /**
         * Writes an entry for a previously validated key.
         * <p>
         * 이미 검사한 키의 항목을 기록한다.
         *
         * @param key the validated key / 검사한 키
         * @param value the entry value / 항목 값
         */
        void put(JsWeakKey key, JsValue value) {
            // 엔진 공용 심볼에 값을 붙이면 값이 참조하는 맵과 realm이 살아 있게 된다.
            if (isPermanentSymbol(key)) {
                if (permanentEntries == null) {
                    permanentEntries = new IdentityHashMap<JsValue, JsValue>();
                }
                permanentEntries.put(key, value);
            } else {
                key.putWeakAssociation(token, value);
            }
        }

        /**
         * Removes the entry of the supplied key.
         * <p>
         * 주어진 키의 항목을 제거한다.
         *
         * @param key key to remove / 제거할 키
         * @return true when an entry was removed / 항목을 제거했으면 true
         */
        boolean remove(JsValue key) {
            if (isPermanentSymbol(key)) {
                return permanentEntries != null && permanentEntries.remove(key) != null;
            }
            return Ops.canBeHeldWeakly(key) && ((JsWeakKey) key).removeWeakAssociation(token) != null;
        }

        /**
         * Checks if the key is a globally rooted, non-registered well-known symbol.
         * <p>
         * 키가 엔진 전체에서 유지되며 레지스트리에 등록되지 않은 well-known 심볼인지 확인한다.
         *
         * @param key candidate key / 후보 키
         * @return true for a well-known symbol / well-known 심볼이면 true
         */
        private static boolean isPermanentSymbol(JsValue key) {
            return key instanceof JsSymbol && ((JsSymbol) key).isWellKnown();
        }

        /**
         * Returns "WeakMap".
         * <p>
         * "WeakMap" 을 반환한다.
         *
         * @return the class name / 클래스 이름
         */
        public String className() {
            return "WeakMap";
        }
    }

    /**
     * A WeakSet instance.
     * <p>
     * WeakSet 인스턴스.
     */
    static final class WeakSetObject extends JsObject {
        /**
         * Members held weakly (identity comparison).
         * <p>
         * 약하게 보관하는 구성원(동일성 비교).
         */
        final WeakHashMap<JsValue, Boolean> map = new WeakHashMap<JsValue, Boolean>();

        /**
         * Creates a weak set object.
         * <p>
         * WeakSet 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        WeakSetObject(JsObject proto) {
            super(proto);
        }

        /**
         * Returns "WeakSet".
         * <p>
         * "WeakSet" 을 반환한다.
         *
         * @return the class name / 클래스 이름
         */
        public String className() {
            return "WeakSet";
        }
    }

    /**
     * A Map or Set iterator.
     * <p>
     * Map 또는 Set 이터레이터.
     */
    static final class CollectionIterator extends JsObject {
        /**
         * True for Map iterators, false for Set iterators.
         * <p>
         * Map 이터레이터이면 true, Set 이터레이터이면 false.
         */
        final boolean map;

        /**
         * KIND_KEYS, KIND_VALUES or KIND_ENTRIES.
         * <p>
         * KIND_KEYS, KIND_VALUES, KIND_ENTRIES 중 하나.
         */
        final int kind;

        /**
         * The position in the table, or null once the iterator finished.
         * <p>
         * 테이블 안의 위치. 이터레이터가 끝나면 null.
         */
        OrderedTable.Cursor cursor;

        /**
         * Creates an iterator.
         * <p>
         * 이터레이터를 만든다.
         *
         * @param proto  the prototype / 프로토타입
         * @param map    Map or Set iterator / Map 또는 Set 이터레이터
         * @param kind   iterator kind / 이터레이터 종류
         * @param cursor the start position / 시작 위치
         */
        CollectionIterator(JsObject proto, boolean map, int kind, OrderedTable.Cursor cursor) {
            super(proto);
            this.map = map;
            this.kind = kind;
            this.cursor = cursor;
        }

        /**
         * Returns the iterator class name.
         * <p>
         * 이터레이터 클래스 이름을 반환한다.
         *
         * @return "Map Iterator" or "Set Iterator" / "Map Iterator" 또는 "Set Iterator"
         */
        public String className() {
            return map ? "Map Iterator" : "Set Iterator";
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CollectionBuiltins() {
    }

    /**
     * Installs all four collections.
     * <p>
     * 네 컬렉션을 모두 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        installMap(realm);
        installSet(realm);
        installWeakMap(realm);
        installWeakSet(realm);
    }

    /**
     * Builds the call behaviour of a constructor that cannot be called without new.
     * <p>
     * new 없이 호출할 수 없는 생성자의 호출 동작을 만든다.
     *
     * @param name constructor name / 생성자 이름
     * @return the behaviour that throws TypeError / TypeError 를 던지는 동작
     */
    private static NativeFunction.Call requiresNew(final String name) {
        return new NativeFunction.Call() {
            /**
             * Always throws TypeError.
             * <p>
             * 항상 TypeError 를 던진다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return never returns / 반환하지 않는다
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Constructor " + name + " requires 'new'");
            }
        };
    }

    /**
     * Fills a collection from an iterable with the adder method (Map: set, Set: add, ...), closing the iterator when the adder or the
     * entry processing fails.
     * <p>
     * 이터러블로 컬렉션을 채운다(Map: set, Set: add 등). 추가 메서드나 항목 처리가 실패하면 이터레이터를 닫는다.
     *
     * @param r        the realm / realm
     * @param target   the new collection / 새 컬렉션
     * @param iterable the iterable argument (undefined and null mean empty) / 이터러블 인수(undefined 와 null 은 빈 것)
     * @param adder    name of the adder method / 추가 메서드 이름
     * @param pairs    true if items are [key, value] entries / 항목이 [키, 값] 쌍이면 true
     */
    private static void fill(JsRealm r, JsObject target, JsValue iterable, String adder, boolean pairs) {
        if (iterable.isNullOrUndefined()) {
            return;
        }
        JsValue add = target.get(adder);
        if (!add.isCallable()) {
            throw JsException.typeError("'" + adder + "' returned for property '" + adder + "' of object is not a function");
        }
        JsObject it = IteratorHelper.getIterator(r, iterable);
        // 이터레이터를 끝까지 돌며 항목을 추가한다. 항목 처리가 실패하면 이터레이터를 닫고 오류를 다시 던진다.
        while (true) {
            JsObject next = IteratorHelper.step(it);
            if (next == null) {
                return;
            }
            JsValue item = IteratorHelper.value(next);
            try {
                if (pairs) {
                    if (!(item instanceof JsObject)) {
                        throw JsException.typeError("Iterator value " + JsException.describe(item) + " is not an entry object");
                    }
                    JsObject entry = (JsObject) item;
                    Ops.call(add, target, new JsValue[] { entry.get("0"), entry.get("1") });
                } else {
                    Ops.call(add, target, new JsValue[] { item });
                }
            } catch (RuntimeException e) {
                IteratorHelper.closeQuietly(it);
                throw e;
            }
        }
    }

    /**
     * Requires a Map receiver.
     * <p>
     * Map 수신 객체임을 확인한다.
     *
     * @param v    the this value / this 값
     * @param name method name for the message / 메시지용 메서드 이름
     * @return the map / Map
     */
    static MapObject thisMap(JsValue v, String name) {
        if (!(v instanceof MapObject)) {
            throw JsException.typeError("Method Map.prototype." + name + " called on incompatible receiver " + JsException.describe(v));
        }
        return (MapObject) v;
    }

    /**
     * Requires a Set receiver.
     * <p>
     * Set 수신 객체임을 확인한다.
     *
     * @param v    the this value / this 값
     * @param name method name for the message / 메시지용 메서드 이름
     * @return the set / Set
     */
    static SetObject thisSet(JsValue v, String name) {
        if (!(v instanceof SetObject)) {
            throw JsException.typeError("Method Set.prototype." + name + " called on incompatible receiver " + JsException.describe(v));
        }
        return (SetObject) v;
    }

    /**
     * Creates the iterator prototype of Map or Set iterators with its next method and tag.
     * <p>
     * Map 또는 Set 이터레이터의 프로토타입을 next 메서드와 태그와 함께 만든다.
     *
     * @param realm the realm / realm
     * @param name  "Map" or "Set" / "Map" 또는 "Set"
     * @param map   true for Map / Map 이면 true
     */
    private static void createIteratorPrototype(JsRealm realm, final String name, final boolean map) {
        JsObject proto = realm.newObject();
        proto.setPrototypeOf(realm.getIteratorPrototype());
        realm.setIntrinsic("%" + name + "IteratorPrototype%", proto);
        realm.method(proto, "next", 0, new NativeFunction.Call() {
            /**
             * Returns the next iterator result.
             * <p>
             * 다음 이터레이터 결과를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the iterator / 이터레이터
             * @param args      arguments (ignored) / 인수(무시)
             * @return the result object / 결과 객체
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof CollectionIterator) || ((CollectionIterator) thisValue).map != map) {
                    throw JsException.typeError("next method called on incompatible receiver " + JsException.describe(thisValue));
                }
                CollectionIterator it = (CollectionIterator) thisValue;
                if (it.cursor == null) {
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                OrderedTable.Entry e = it.cursor.next();
                if (e == null) {
                    it.cursor = null;
                    return IteratorHelper.createResult(r, Js.UNDEFINED, true);
                }
                JsValue v;
                if (it.kind == KIND_KEYS) {
                    v = e.key;
                } else if (it.kind == KIND_VALUES) {
                    v = map ? e.value : e.key;
                } else {
                    v = r.newArray(new JsValue[] { e.key, map ? e.value : e.key });
                }
                return IteratorHelper.createResult(r, v, false);
            }
        });
        Support.defineToStringTag(proto, name + " Iterator");
    }

    /**
     * Creates an iterator object over a table.
     * <p>
     * 테이블 위의 이터레이터 객체를 만든다.
     *
     * @param r     the realm / realm
     * @param map   Map or Set iterator / Map 또는 Set 이터레이터
     * @param table the table / 테이블
     * @param kind  iterator kind / 이터레이터 종류
     * @return the iterator / 이터레이터
     */
    private static JsObject iterator(JsRealm r, boolean map, OrderedTable table, int kind) {
        return new CollectionIterator(r.getIntrinsic(map ? "%MapIteratorPrototype%" : "%SetIteratorPrototype%"), map, kind, table.cursor());
    }

    /**
     * Installs <code>Map</code>.
     * <p>
     * <code>Map</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private static void installMap(JsRealm realm) {
        createIteratorPrototype(realm, "Map", true);
        JsObject proto = realm.newObject();
        NativeFunction ctor = Support.makeConstructor(realm, "Map", 0, proto, requiresNew("Map"), new NativeFunction.Construct() {
            /**
             * new Map(iterable).
             * <p>
             * new Map(iterable).
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the new map / 새 Map
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                MapObject m = new MapObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%MapPrototype%")));
                fill(r, m, Js.arg(args, 0), "set", true);
                return m;
            }
        });
        Support.defineSpecies(realm, ctor);
        GroupByBuiltins.installMap(realm, ctor);
        MapUpserts.installMap(realm, proto);
        realm.method(proto, "clear", 0, new NativeFunction.Call() {
            /**
             * Removes all entries.
             * <p>
             * 모든 항목을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments (ignored) / 인수(무시)
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                thisMap(thisValue, "clear").table.clear();
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "delete", 1, new NativeFunction.Call() {
            /**
             * Removes the entry of a key.
             * <p>
             * 키의 항목을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments: the key / 인수: 키
             * @return true if an entry was removed / 항목이 삭제되었으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisMap(thisValue, "delete").table.remove(Js.arg(args, 0)));
            }
        });
        NativeFunction entries = realm.method(proto, "entries", 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over [key, value] pairs.
             * <p>
             * [키, 값] 쌍의 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments (ignored) / 인수(무시)
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterator(r, true, thisMap(thisValue, "entries").table, KIND_ENTRIES);
            }
        });
        realm.method(proto, "forEach", 1, new NativeFunction.Call() {
            /**
             * Calls the callback for every entry in insertion order, including entries added meanwhile.
             * <p>
             * 삽입 순서로 모든 항목(그 사이 추가된 것 포함)에 대해 콜백을 호출한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments: callback and this argument / 인수: 콜백과 this 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                MapObject m = thisMap(thisValue, "forEach");
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError(JsException.describe(cb) + " is not a function");
                }
                JsValue thisArg = Js.arg(args, 1);
                OrderedTable.Cursor c = m.table.cursor();
                // 살아 있는 항목을 순서대로 콜백에 넘긴다. 순회 중 추가/삭제는 커서가 반영한다.
                for (OrderedTable.Entry e = c.next(); e != null; e = c.next()) {
                    Ops.call(cb, thisArg, new JsValue[] { e.value, e.key, m });
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "get", 1, new NativeFunction.Call() {
            /**
             * Returns the value of a key.
             * <p>
             * 키의 값을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments: the key / 인수: 키
             * @return the value or undefined / 값 또는 undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                OrderedTable.Entry e = thisMap(thisValue, "get").table.find(Js.arg(args, 0));
                return e == null ? Js.UNDEFINED : e.value;
            }
        });
        realm.method(proto, "has", 1, new NativeFunction.Call() {
            /**
             * Tells whether a key is present.
             * <p>
             * 키가 있는지 알려 준다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments: the key / 인수: 키
             * @return true if present / 있으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisMap(thisValue, "has").table.find(Js.arg(args, 0)) != null);
            }
        });
        realm.method(proto, "keys", 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over the keys.
             * <p>
             * 키의 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments (ignored) / 인수(무시)
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterator(r, true, thisMap(thisValue, "keys").table, KIND_KEYS);
            }
        });
        realm.method(proto, "set", 2, new NativeFunction.Call() {
            /**
             * Adds or updates an entry.
             * <p>
             * 항목을 추가하거나 갱신한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments: key and value / 인수: 키와 값
             * @return the map / Map
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                thisMap(thisValue, "set").table.put(Js.arg(args, 0), Js.arg(args, 1));
                return thisValue;
            }
        });
        realm.getter(proto, "size", new NativeFunction.Call() {
            /**
             * Returns the number of entries.
             * <p>
             * 항목 수를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments (ignored) / 인수(무시)
             * @return the size / 크기
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisMap(thisValue, "size").table.size());
            }
        });
        realm.method(proto, "values", 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over the values.
             * <p>
             * 값의 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the map / Map
             * @param args      arguments (ignored) / 인수(무시)
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterator(r, true, thisMap(thisValue, "values").table, KIND_VALUES);
            }
        });
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, entries);
        Support.defineToStringTag(proto, "Map");
    }

    /**
     * Installs <code>Set</code>.
     * <p>
     * <code>Set</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private static void installSet(JsRealm realm) {
        createIteratorPrototype(realm, "Set", false);
        JsObject proto = realm.newObject();
        NativeFunction ctor = Support.makeConstructor(realm, "Set", 0, proto, requiresNew("Set"), new NativeFunction.Construct() {
            /**
             * new Set(iterable).
             * <p>
             * new Set(iterable).
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the new set / 새 Set
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                SetObject s = new SetObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%SetPrototype%")));
                fill(r, s, Js.arg(args, 0), "add", false);
                return s;
            }
        });
        Support.defineSpecies(realm, ctor);
        SetOperations.install(realm, proto);
        realm.method(proto, "add", 1, new NativeFunction.Call() {
            /**
             * Adds a value.
             * <p>
             * 값을 추가한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments: the value / 인수: 값
             * @return the set / Set
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue v = Js.arg(args, 0);
                thisSet(thisValue, "add").table.put(v, v);
                return thisValue;
            }
        });
        realm.method(proto, "clear", 0, new NativeFunction.Call() {
            /**
             * Removes all values.
             * <p>
             * 모든 값을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments (ignored) / 인수(무시)
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                thisSet(thisValue, "clear").table.clear();
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "delete", 1, new NativeFunction.Call() {
            /**
             * Removes a value.
             * <p>
             * 값을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments: the value / 인수: 값
             * @return true if it was removed / 삭제되었으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisSet(thisValue, "delete").table.remove(Js.arg(args, 0)));
            }
        });
        realm.method(proto, "entries", 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over [value, value] pairs.
             * <p>
             * [값, 값] 쌍의 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments (ignored) / 인수(무시)
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterator(r, false, thisSet(thisValue, "entries").table, KIND_ENTRIES);
            }
        });
        realm.method(proto, "forEach", 1, new NativeFunction.Call() {
            /**
             * Calls the callback for every value in insertion order, including values added meanwhile.
             * <p>
             * 삽입 순서로 모든 값(그 사이 추가된 것 포함)에 대해 콜백을 호출한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments: callback and this argument / 인수: 콜백과 this 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                SetObject s = thisSet(thisValue, "forEach");
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError(JsException.describe(cb) + " is not a function");
                }
                JsValue thisArg = Js.arg(args, 1);
                OrderedTable.Cursor c = s.table.cursor();
                // 살아 있는 값을 순서대로 콜백에 넘긴다.
                for (OrderedTable.Entry e = c.next(); e != null; e = c.next()) {
                    Ops.call(cb, thisArg, new JsValue[] { e.key, e.key, s });
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "has", 1, new NativeFunction.Call() {
            /**
             * Tells whether a value is present.
             * <p>
             * 값이 있는지 알려 준다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments: the value / 인수: 값
             * @return true if present / 있으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisSet(thisValue, "has").table.find(Js.arg(args, 0)) != null);
            }
        });
        realm.getter(proto, "size", new NativeFunction.Call() {
            /**
             * Returns the number of values.
             * <p>
             * 값의 수를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments (ignored) / 인수(무시)
             * @return the size / 크기
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisSet(thisValue, "size").table.size());
            }
        });
        NativeFunction values = realm.method(proto, "values", 0, new NativeFunction.Call() {
            /**
             * Returns an iterator over the values.
             * <p>
             * 값의 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the set / Set
             * @param args      arguments (ignored) / 인수(무시)
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterator(r, false, thisSet(thisValue, "values").table, KIND_VALUES);
            }
        });
        proto.defineBuiltin("keys", values);
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, values);
        Support.defineToStringTag(proto, "Set");
    }

    /**
     * Requires a value that can be held weakly as a collection key.
     * <p>
     * 약한 컬렉션의 키로 약하게 참조할 수 있는 값을 요구한다.
     *
     * @param v    the key / 키
     * @param what "weak map" or "weak set" for the message / 메시지용 "weak map" 또는 "weak set"
     * @return the eligible weak key / 사용할 수 있는 약한 키
     */
    static JsWeakKey weakKey(JsValue v, String what) {
        if (!Ops.canBeHeldWeakly(v)) {
            throw JsException.typeError("Invalid value used " + (what.equals("weak map") ? "as weak map key" : "in weak set"));
        }
        return (JsWeakKey) v;
    }

    /**
     * Requires a WeakMap receiver.
     * <p>
     * WeakMap 수신 객체임을 확인한다.
     *
     * @param v    the this value / this 값
     * @param name method name for the message / 메시지용 메서드 이름
     * @return the weak map / WeakMap
     */
    static WeakMapObject thisWeakMap(JsValue v, String name) {
        if (!(v instanceof WeakMapObject)) {
            throw JsException.typeError("Method WeakMap.prototype." + name + " called on incompatible receiver " + JsException.describe(v));
        }
        return (WeakMapObject) v;
    }

    /**
     * Requires a WeakSet receiver.
     * <p>
     * WeakSet 수신 객체임을 확인한다.
     *
     * @param v    the this value / this 값
     * @param name method name for the message / 메시지용 메서드 이름
     * @return the weak set / WeakSet
     */
    private static WeakSetObject thisWeakSet(JsValue v, String name) {
        if (!(v instanceof WeakSetObject)) {
            throw JsException.typeError("Method WeakSet.prototype." + name + " called on incompatible receiver " + JsException.describe(v));
        }
        return (WeakSetObject) v;
    }

    /**
     * Installs <code>WeakMap</code>.
     * <p>
     * <code>WeakMap</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private static void installWeakMap(JsRealm realm) {
        JsObject proto = realm.newObject();
        Support.makeConstructor(realm, "WeakMap", 0, proto, requiresNew("WeakMap"), new NativeFunction.Construct() {
            /**
             * new WeakMap(iterable).
             * <p>
             * new WeakMap(iterable).
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the new weak map / 새 WeakMap
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                WeakMapObject m = new WeakMapObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%WeakMapPrototype%")));
                fill(r, m, Js.arg(args, 0), "set", true);
                return m;
            }
        });
        MapUpserts.installWeakMap(realm, proto);
        realm.method(proto, "delete", 1, new NativeFunction.Call() {
            /**
             * Removes the entry of a key object.
             * <p>
             * 키 객체의 항목을 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the weak map / WeakMap
             * @param args      arguments: the key / 인수: 키
             * @return true if an entry was removed / 항목이 삭제되었으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakMapObject m = thisWeakMap(thisValue, "delete");
                JsValue k = Js.arg(args, 0);
                return Js.bool(m.remove(k));
            }
        });
        realm.method(proto, "get", 1, new NativeFunction.Call() {
            /**
             * Returns the value of a key object.
             * <p>
             * 키 객체의 값을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the weak map / WeakMap
             * @param args      arguments: the key / 인수: 키
             * @return the value or undefined / 값 또는 undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakMapObject m = thisWeakMap(thisValue, "get");
                JsValue k = Js.arg(args, 0);
                JsValue v = m.get(k);
                return v == null ? Js.UNDEFINED : v;
            }
        });
        realm.method(proto, "has", 1, new NativeFunction.Call() {
            /**
             * Tells whether a key object is present.
             * <p>
             * 키 객체가 있는지 알려 준다.
             *
             * @param r         realm / realm
             * @param thisValue the weak map / WeakMap
             * @param args      arguments: the key / 인수: 키
             * @return true if present / 있으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakMapObject m = thisWeakMap(thisValue, "has");
                JsValue k = Js.arg(args, 0);
                return Js.bool(m.get(k) != null);
            }
        });
        realm.method(proto, "set", 2, new NativeFunction.Call() {
            /**
             * Adds or updates an entry; the key must be an object.
             * <p>
             * 항목을 추가하거나 갱신한다. 키는 객체여야 한다.
             *
             * @param r         realm / realm
             * @param thisValue the weak map / WeakMap
             * @param args      arguments: key and value / 인수: 키와 값
             * @return the weak map / WeakMap
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakMapObject m = thisWeakMap(thisValue, "set");
                m.put(weakKey(Js.arg(args, 0), "weak map"), Js.arg(args, 1));
                return thisValue;
            }
        });
        Support.defineToStringTag(proto, "WeakMap");
    }

    /**
     * Installs <code>WeakSet</code>.
     * <p>
     * <code>WeakSet</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    private static void installWeakSet(JsRealm realm) {
        JsObject proto = realm.newObject();
        Support.makeConstructor(realm, "WeakSet", 0, proto, requiresNew("WeakSet"), new NativeFunction.Construct() {
            /**
             * new WeakSet(iterable).
             * <p>
             * new WeakSet(iterable).
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the new weak set / 새 WeakSet
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                WeakSetObject s = new WeakSetObject(Ops.getPrototypeFromConstructor(r, newTarget, r.getIntrinsic("%WeakSetPrototype%")));
                fill(r, s, Js.arg(args, 0), "add", false);
                return s;
            }
        });
        realm.method(proto, "add", 1, new NativeFunction.Call() {
            /**
             * Adds an object.
             * <p>
             * 객체를 추가한다.
             *
             * @param r         realm / realm
             * @param thisValue the weak set / WeakSet
             * @param args      arguments: the object / 인수: 객체
             * @return the weak set / WeakSet
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakSetObject s = thisWeakSet(thisValue, "add");
                s.map.put(weakKey(Js.arg(args, 0), "weak set"), Boolean.TRUE);
                return thisValue;
            }
        });
        realm.method(proto, "delete", 1, new NativeFunction.Call() {
            /**
             * Removes an object.
             * <p>
             * 객체를 삭제한다.
             *
             * @param r         realm / realm
             * @param thisValue the weak set / WeakSet
             * @param args      arguments: the object / 인수: 객체
             * @return true if it was removed / 삭제되었으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakSetObject s = thisWeakSet(thisValue, "delete");
                JsValue k = Js.arg(args, 0);
                return Js.bool(Ops.canBeHeldWeakly(k) && s.map.remove(k) != null);
            }
        });
        realm.method(proto, "has", 1, new NativeFunction.Call() {
            /**
             * Tells whether an object is a member.
             * <p>
             * 객체가 구성원인지 알려 준다.
             *
             * @param r         realm / realm
             * @param thisValue the weak set / WeakSet
             * @param args      arguments: the object / 인수: 객체
             * @return true if present / 있으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                WeakSetObject s = thisWeakSet(thisValue, "has");
                JsValue k = Js.arg(args, 0);
                return Js.bool(Ops.canBeHeldWeakly(k) && s.map.containsKey(k));
            }
        });
        Support.defineToStringTag(proto, "WeakSet");
    }
}
