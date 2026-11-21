package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Map, Set, WeakMap and WeakSet (plan item P14-05): key semantics, insertion order, constructors with iterables, iterators and
 * their behavior when the collection changes during iteration, species, tags, receiver checks and weak references.
 * <p>
 * Map, Set, WeakMap, WeakSet 시험(계획 P14-05): 키 의미, 삽입 순서, 이터러블을 받는 생성자, 이터레이터와 순회 중 컬렉션이 바뀔 때의 동작,
 * species, 태그, 수신 객체 검사, 약한 참조.
 */
public class CollectionTest {

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
    public CollectionTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        env = new Env();
    }

    /**
     * Describes a value for comparisons: numbers and strings by text, arrays as "a:b", other objects as "obj".
     * <p>
     * 비교용으로 값을 설명한다: 숫자와 문자열은 내용, 배열은 "a:b", 그 밖의 객체는 "obj".
     *
     * @param v the value / 값
     * @return the description / 설명
     */
    private String show(JsValue v) {
        if (v instanceof JsArray) {
            JsObject a = (JsObject) v;
            return show(a.get("0")) + ":" + show(a.get("1"));
        }
        if (v instanceof JsObject) {
            return "obj";
        }
        if (v.isUndefined()) {
            return "undefined";
        }
        if (v.isNull()) {
            return "null";
        }
        if (v instanceof JsSymbol) {
            return "symbol";
        }
        return Ops.toString(v);
    }

    /**
     * Runs an iterator to its end and describes the produced values.
     * <p>
     * 이터레이터를 끝까지 실행하고 만들어진 값을 설명한다.
     *
     * @param iterator the iterator / 이터레이터
     * @return the values / 값들
     */
    private List<String> drain(JsValue iterator) {
        List<String> out = new ArrayList<String>();
        // done 이 true 가 될 때까지 next 를 호출한다.
        while (true) {
            JsObject r = (JsObject) env.invoke(iterator, "next");
            if (Ops.toBoolean(r.get("done"))) {
                return out;
            }
            out.add(show(r.get("value")));
        }
    }

    /**
     * Basic Map operations and SameValueZero key semantics.
     * <p>
     * 기본 Map 연산과 SameValueZero 키 의미.
     */
    public void testMapBasics() {
        JsObject m = env.construct("Map");
        Check.same(m, env.invoke(m, "set", Env.s("a"), Env.n(1)), "set returns the map");
        env.invoke(m, "set", Env.n(0.0), Env.s("zero"));
        Check.same(Js.TRUE, env.invoke(m, "has", Env.n(-0.0)), "-0 and +0 are the same key");
        Check.equal("zero", Env.str(env.invoke(m, "get", Env.n(-0.0))), "lookup with -0");
        env.invoke(m, "set", Env.n(Double.NaN), Env.s("nan"));
        Check.equal("nan", Env.str(env.invoke(m, "get", Env.n(Double.NaN))), "NaN equals NaN");
        env.invoke(m, "set", Env.n(1), Env.s("number"));
        env.invoke(m, "set", Env.s("1"), Env.s("string"));
        Check.equal("number", Env.str(env.invoke(m, "get", Env.n(1))), "number 1");
        Check.equal("string", Env.str(env.invoke(m, "get", Env.s("1"))), "string \"1\" is a different key");
        JsObject k1 = env.realm.newObject();
        JsObject k2 = env.realm.newObject();
        env.invoke(m, "set", k1, Env.s("o1"));
        Check.same(Js.UNDEFINED, env.invoke(m, "get", k2), "objects are compared by identity");
        env.invoke(m, "set", Js.UNDEFINED, Env.s("u"));
        env.invoke(m, "set", Js.NULL, Env.s("n"));
        Check.equal("u", Env.str(env.invoke(m, "get", Js.UNDEFINED)), "undefined key");
        Check.equal("n", Env.str(env.invoke(m, "get", Js.NULL)), "null key");
        Check.equal(8.0, Env.d(m.get("size")), "size");
        Check.same(Js.TRUE, env.invoke(m, "delete", Env.s("a")), "delete existing");
        Check.same(Js.FALSE, env.invoke(m, "delete", Env.s("a")), "delete missing");
        Check.equal(7.0, Env.d(m.get("size")), "size after delete");
        env.invoke(m, "clear");
        Check.equal(0.0, Env.d(m.get("size")), "size after clear");
        Check.same(Js.UNDEFINED, env.invoke(m, "get", Env.n(1)), "empty after clear");
        env.invoke(m, "set", Env.n(-0.0), Env.n(5));
        JsObject firstKey = (JsObject) env.invoke(env.invoke(m, "keys"), "next");
        Check.equal(Double.POSITIVE_INFINITY, 1.0 / Env.d(firstKey.get("value")), "the stored key is +0, not -0");
    }

    /**
     * Updating a key keeps its position; deleting and re-adding moves it to the end.
     * <p>
     * 키를 갱신해도 위치가 유지되고, 삭제 후 다시 추가하면 끝으로 옮겨진다.
     */
    public void testMapOrder() {
        JsObject m = env.construct("Map");
        env.invoke(m, "set", Env.s("a"), Env.n(1));
        env.invoke(m, "set", Env.s("b"), Env.n(2));
        env.invoke(m, "set", Env.s("c"), Env.n(3));
        env.invoke(m, "set", Env.s("a"), Env.n(10));
        Check.equal(Arrays.asList("a", "b", "c"), drain(env.invoke(m, "keys")), "update keeps the position");
        Check.equal(Arrays.asList("10", "2", "3"), drain(env.invoke(m, "values")), "values in order");
        Check.equal(Arrays.asList("a:10", "b:2", "c:3"), drain(env.invoke(m, "entries")), "entries in order");
        env.invoke(m, "delete", Env.s("a"));
        env.invoke(m, "set", Env.s("a"), Env.n(11));
        Check.equal(Arrays.asList("b", "c", "a"), drain(env.invoke(m, "keys")), "delete + set moves the key to the end");
        Check.same(env.obj("Map.prototype").get("entries"), env.obj("Map.prototype").get(WellKnownSymbols.ITERATOR), "Map.prototype[@@iterator] is entries");
        Check.equal(Arrays.asList("b:2", "c:3", "a:11"), drain(env.invokeSymbol(m, WellKnownSymbols.ITERATOR)), "default iteration gives entries");
    }

    /**
     * The Map constructor: iterables, missing new, bad entries, closing of the iterator, overridden set.
     * <p>
     * Map 생성자: 이터러블, new 누락, 잘못된 항목, 이터레이터 닫기, 재정의된 set.
     */
    public void testMapConstructor() {
        JsObject m = env.construct("Map", env.iterable(env.pair(Env.s("a"), Env.n(1)), env.pair(Env.s("b"), Env.n(2)), env.pair(Env.s("a"), Env.n(3))));
        Check.equal(2.0, Env.d(m.get("size")), "duplicate keys in the iterable");
        Check.equal(3.0, Env.d(env.invoke(m, "get", Env.s("a"))), "the later value wins");
        Check.equal(0.0, Env.d(env.construct("Map", Js.UNDEFINED).get("size")), "undefined iterable");
        Check.equal(0.0, Env.d(env.construct("Map", Js.NULL).get("size")), "null iterable");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls Map without new.
             * <p>
             * new 없이 Map 을 호출한다.
             */
            public void run() {
                Ops.call(env.get("Map"), Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "Map() without new");
        final JsObject closed = env.realm.newObject();
        final int[] returns = new int[1];
        env.realm.method(closed, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            /**
             * Returns an iterator that yields one bad entry and counts calls of return().
             * <p>
             * 잘못된 항목 하나를 내고 return() 호출을 세는 이터레이터를 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the iterator / 이터레이터
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject it = r.newObject();
                r.method(it, "next", 0, new NativeFunction.Call() {
                    /**
                     * Yields the primitive 5, which is not an entry object.
                     * <p>
                     * 항목 객체가 아닌 원시 값 5 를 낸다.
                     *
                     * @param r2         realm / realm
                     * @param thisValue2 this / this
                     * @param args2      arguments / 인수
                     * @return the result / 결과
                     */
                    public JsValue call(JsRealm r2, JsValue thisValue2, JsValue[] args2) {
                        JsObject res = r2.newObject();
                        res.createDataProperty("value", Env.n(5));
                        res.createDataProperty("done", Js.FALSE);
                        return res;
                    }
                });
                r.method(it, "return", 0, new NativeFunction.Call() {
                    /**
                     * Counts the call.
                     * <p>
                     * 호출을 센다.
                     *
                     * @param r2         realm / realm
                     * @param thisValue2 this / this
                     * @param args2      arguments / 인수
                     * @return an object / 객체
                     */
                    public JsValue call(JsRealm r2, JsValue thisValue2, JsValue[] args2) {
                        returns[0]++;
                        return r2.newObject();
                    }
                });
                return it;
            }
        });
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Builds a Map from an iterable that yields a non-object entry.
             * <p>
             * 객체가 아닌 항목을 내는 이터러블로 Map 을 만든다.
             */
            public void run() {
                env.construct("Map", closed);
            }
        }, "entries must be objects");
        Check.equal(1, returns[0], "the iterator is closed when an entry is rejected");
        final JsObject proto = env.obj("Map.prototype");
        final JsValue original = proto.get("set");
        final int[] calls = new int[1];
        proto.defineBuiltin("set", env.realm.newFunction("set", 2, new NativeFunction.Call() {
            /**
             * A replacement set that counts calls.
             * <p>
             * 호출을 세는 대체 set.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the map / Map
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                return thisValue;
            }
        }));
        env.construct("Map", env.iterable(env.pair(Env.n(1), Env.n(2))));
        Check.equal(1, calls[0], "the constructor uses the current Map.prototype.set");
        proto.defineBuiltin("set", Env.n(1));
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Builds a Map while set is not a function.
             * <p>
             * set 이 함수가 아닐 때 Map 을 만든다.
             */
            public void run() {
                env.construct("Map", env.iterable(env.pair(Env.n(1), Env.n(2))));
            }
        }, "set must be callable when an iterable is given");
        proto.defineBuiltin("set", original);
        Check.equal(0.0, Env.d(env.construct("Map", Js.UNDEFINED).get("size")), "restored");
    }

    /**
     * Changing a Map while iterating: deletions skip, additions are visited, clear lets the iterator continue with new entries.
     * <p>
     * 순회 중 Map 변경: 삭제하면 건너뛰고, 추가하면 방문하며, clear 후에는 새 항목으로 계속된다.
     */
    public void testMapMutationDuringIteration() {
        JsObject m = env.construct("Map");
        env.invoke(m, "set", Env.s("a"), Env.n(1));
        env.invoke(m, "set", Env.s("b"), Env.n(2));
        env.invoke(m, "set", Env.s("c"), Env.n(3));
        JsValue it = env.invoke(m, "keys");
        Check.equal("a", show(((JsObject) env.invoke(it, "next")).get("value")), "first key");
        env.invoke(m, "delete", Env.s("b"));
        env.invoke(m, "set", Env.s("d"), Env.n(4));
        Check.equal(Arrays.asList("c", "d"), drain(it), "deleted keys are skipped and added keys are visited");
        env.invoke(m, "set", Env.s("late"), Env.n(0));
        Check.equal(Arrays.asList(), drain(it), "a finished iterator stays finished");
        JsValue it2 = env.invoke(m, "keys");
        env.invoke(it2, "next");
        env.invoke(m, "clear");
        env.invoke(m, "set", Env.s("x"), Env.n(9));
        Check.equal(Arrays.asList("x"), drain(it2), "after clear the running iterator sees later entries");
        JsObject m2 = env.construct("Map");
        env.invoke(m2, "set", Env.s("k"), Env.n(1));
        JsValue it3 = env.invoke(m2, "keys");
        env.invoke(m2, "delete", Env.s("k"));
        env.invoke(m2, "set", Env.s("k"), Env.n(2));
        Check.equal(Arrays.asList("k"), drain(it3), "a key deleted and added again is visited once at its new position");
    }

    /**
     * Compaction of the entry list keeps running iterators and forEach correct.
     * <p>
     * 항목 목록 압축이 일어나도 진행 중인 이터레이터와 forEach 가 올바르다.
     */
    public void testMapCompactionKeepsIterators() {
        final JsObject m = env.construct("Map");
        // 항목을 많이 넣은 뒤 이터레이터를 중간까지 진행시킨다.
        for (int i = 0; i < 300; i++) {
            env.invoke(m, "set", Env.n(i), Env.n(i));
        }
        JsValue it = env.invoke(m, "keys");
        // 앞의 100 개를 소비한다.
        for (int i = 0; i < 100; i++) {
            env.invoke(it, "next");
        }
        // 이미 지나간 앞쪽 150 개를 삭제하여 압축이 일어나게 한다.
        for (int i = 0; i < 150; i++) {
            env.invoke(m, "delete", Env.n(i));
        }
        List<String> rest = drain(it);
        Check.equal(150, rest.size(), "the iterator continues with keys 150..299 (100..149 were deleted before being reached)");
        Check.equal("150", rest.get(0), "the next key is the first one that was not deleted");
        Check.equal("299", rest.get(rest.size() - 1), "the last key");
        final List<String> seen = new ArrayList<String>();
        NativeFunction cb = new NativeFunction(env.realm, "cb", 3, new NativeFunction.Call() {
            /**
             * Records keys; deletes the next key and adds a new one during forEach.
             * <p>
             * 키를 기록하고 forEach 도중 다음 키를 삭제하며 새 키를 추가한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      value, key, map / 값, 키, Map
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen.add(show(args[1]));
                if (seen.size() == 1) {
                    env.invoke(m, "delete", Env.n(151));
                    env.invoke(m, "set", Env.s("added"), Env.n(0));
                }
                return Js.UNDEFINED;
            }
        });
        env.invoke(m, "forEach", cb);
        Check.equal("150", seen.get(0), "forEach starts with the first live entry");
        Check.equal("152", seen.get(1), "the deleted entry 151 is skipped");
        Check.equal("added", seen.get(seen.size() - 1), "entries added during forEach are visited");
    }

    /**
     * forEach passes value, key and the map, uses thisArg and validates the callback.
     * <p>
     * forEach 는 값, 키, Map 을 넘기고 thisArg 를 쓰며 콜백을 검증한다.
     */
    public void testMapForEach() {
        final JsObject m = env.construct("Map");
        env.invoke(m, "set", Env.s("k"), Env.s("v"));
        final List<String> log = new ArrayList<String>();
        final JsObject self = env.realm.newObject();
        NativeFunction cb = new NativeFunction(env.realm, "cb", 3, new NativeFunction.Call() {
            /**
             * Records its arguments.
             * <p>
             * 인수를 기록한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      value, key, map / 값, 키, Map
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.add(show(args[0]) + "," + show(args[1]) + "," + (args[2] == m) + "," + (thisValue == self));
                return Js.UNDEFINED;
            }
        });
        env.invoke(m, "forEach", cb, self);
        Check.equal(Arrays.asList("v,k,true,true"), log, "callback arguments and this");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls forEach with a non-function.
             * <p>
             * 함수가 아닌 값으로 forEach 를 호출한다.
             */
            public void run() {
                env.invoke(m, "forEach", Env.n(1));
            }
        }, "callback must be callable");
    }

    /**
     * Basic Set operations, order and shared function objects.
     * <p>
     * 기본 Set 연산, 순서, 공유 함수 객체.
     */
    public void testSet() {
        JsObject s = env.construct("Set", env.iterable(Env.n(3), Env.n(1), Env.n(3), Env.n(2), Env.n(-0.0), Env.n(0)));
        Check.equal(4.0, Env.d(s.get("size")), "duplicates, and +0/-0, collapse");
        Check.equal(Arrays.asList("3", "1", "2", "0"), drain(env.invoke(s, "values")), "values in insertion order");
        Check.same(s, env.invoke(s, "add", Env.n(9)), "add returns the set");
        Check.same(Js.TRUE, env.invoke(s, "has", Env.n(9)), "has");
        Check.same(Js.TRUE, env.invoke(s, "delete", Env.n(9)), "delete");
        Check.same(Js.FALSE, env.invoke(s, "delete", Env.n(9)), "delete missing");
        Check.equal(Arrays.asList("3:3", "1:1", "2:2", "0:0"), drain(env.invoke(s, "entries")), "entries are [value, value] pairs");
        JsObject proto = env.obj("Set.prototype");
        Check.same(proto.get("values"), proto.get("keys"), "Set.prototype.keys is values");
        Check.same(proto.get("values"), proto.get(WellKnownSymbols.ITERATOR), "Set.prototype[@@iterator] is values");
        final List<String> log = new ArrayList<String>();
        env.invoke(s, "forEach", new NativeFunction(env.realm, "cb", 3, new NativeFunction.Call() {
            /**
             * Records value, value and whether the third argument is a Set.
             * <p>
             * 값, 값, 세 번째 인수가 Set 인지를 기록한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.add(show(args[0]) + "=" + show(args[1]) + (args[2] instanceof JsObject));
                return Js.UNDEFINED;
            }
        }));
        Check.equal("3=3true", log.get(0), "forEach arguments");
        JsValue it = env.invoke(s, "values");
        env.invoke(it, "next");
        env.invoke(s, "add", Env.s("new"));
        Check.equal(Arrays.asList("1", "2", "0", "new"), drain(it), "values added during iteration are visited");
        env.invoke(s, "clear");
        Check.equal(0.0, Env.d(s.get("size")), "clear");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls Set without new.
             * <p>
             * new 없이 Set 을 호출한다.
             */
            public void run() {
                Ops.call(env.get("Set"), Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "Set() without new");
    }

    /**
     * WeakMap and WeakSet accept only objects as keys and have no size or iteration.
     * <p>
     * WeakMap 과 WeakSet 은 객체만 키로 받으며 size 도 순회도 없다.
     */
    public void testWeakCollections() {
        final JsObject wm = env.construct("WeakMap");
        final JsObject key = env.realm.newObject();
        Check.same(wm, env.invoke(wm, "set", key, Env.s("v")), "set returns the weak map");
        Check.equal("v", Env.str(env.invoke(wm, "get", key)), "get");
        Check.same(Js.TRUE, env.invoke(wm, "has", key), "has");
        Check.same(Js.FALSE, env.invoke(wm, "has", env.realm.newObject()), "other objects are different keys");
        Check.same(Js.UNDEFINED, env.invoke(wm, "get", Env.n(1)), "a primitive key is simply absent");
        Check.same(Js.FALSE, env.invoke(wm, "has", Env.s("x")), "has with a primitive");
        Check.same(Js.FALSE, env.invoke(wm, "delete", Env.n(1)), "delete with a primitive");
        Check.same(Js.TRUE, env.invoke(wm, "delete", key), "delete");
        Check.same(Js.FALSE, env.invoke(wm, "has", key), "deleted");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Uses a primitive as weak map key.
             * <p>
             * 원시 값을 WeakMap 키로 쓴다.
             */
            public void run() {
                env.invoke(wm, "set", Env.n(1), Env.n(2));
            }
        }, "primitive key in set");
        Check.same(Js.UNDEFINED, wm.get("size"), "no size property");
        Check.same(Js.UNDEFINED, wm.get("forEach"), "no forEach");
        JsObject k2 = env.realm.newObject();
        JsObject wm2 = env.construct("WeakMap", env.iterable(env.pair(k2, Env.n(7))));
        Check.equal(7.0, Env.d(env.invoke(wm2, "get", k2)), "constructed from an iterable");
        final JsObject ws = env.construct("WeakSet", env.iterable(k2));
        Check.same(Js.TRUE, env.invoke(ws, "has", k2), "WeakSet from an iterable");
        Check.same(ws, env.invoke(ws, "add", key), "add returns the weak set");
        Check.same(Js.TRUE, env.invoke(ws, "delete", key), "WeakSet delete");
        Check.same(Js.FALSE, env.invoke(ws, "has", Env.n(1)), "has with a primitive");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Adds a primitive to a weak set.
             * <p>
             * WeakSet 에 원시 값을 추가한다.
             */
            public void run() {
                env.invoke(ws, "add", Env.s("x"));
            }
        }, "primitive in add");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls WeakMap without new.
             * <p>
             * new 없이 WeakMap 을 호출한다.
             */
            public void run() {
                Ops.call(env.get("WeakMap"), Js.UNDEFINED, Js.NO_ARGS);
            }
        }, "WeakMap() without new");
    }

    /**
     * Weak keys are not retained: after the key becomes unreachable and the JVM collects it, the entry disappears (best effort; skipped with a
     * reason if the JVM does not collect in time, since garbage collection timing is not deterministic).
     * <p>
     * 약한 키는 붙잡히지 않는다: 키에 닿을 수 없게 되고 JVM 이 수거하면 항목이 사라진다(최선 노력. 가비지 수거 시점은 결정적이지 않으므로
     * JVM 이 제때 수거하지 않으면 이유와 함께 건너뛴다).
     */
    public void testWeakKeysAreNotRetained() {
        JsObject wm = env.construct("WeakMap");
        JsObject ws = env.construct("WeakSet");
        // 키 객체를 만들어 넣은 뒤 참조를 버린다.
        for (int i = 0; i < 50; i++) {
            JsObject k = env.realm.newObject();
            env.invoke(wm, "set", k, Env.n(i));
            env.invoke(ws, "add", k);
        }
        int mapSize = ((CollectionBuiltins.WeakMapObject) wm).map.size();
        Check.isTrue(mapSize > 0, "entries exist right after insertion");
        long end = System.currentTimeMillis() + 3000;
        // 수거될 때까지 가비지 수거를 반복 요청한다(최대 3 초).
        while (((CollectionBuiltins.WeakMapObject) wm).map.size() > 0 && System.currentTimeMillis() < end) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (((CollectionBuiltins.WeakMapObject) wm).map.size() > 0 || ((CollectionBuiltins.WeakSetObject) ws).map.size() > 0) {
            Check.skip("the JVM did not collect the weak keys within 3 seconds (garbage collection timing is not deterministic)");
        }
        Check.equal(0, ((CollectionBuiltins.WeakMapObject) wm).map.size(), "weak map entries were collected");
        Check.equal(0, ((CollectionBuiltins.WeakSetObject) ws).map.size(), "weak set members were collected");
    }

    /**
     * Species getters, string tags and the iterator prototypes.
     * <p>
     * species getter, 문자열 태그, 이터레이터 프로토타입.
     */
    public void testSpeciesTagsAndIteratorPrototypes() {
        String[] names = { "Map", "Set" };
        // Map 과 Set 의 species 접근자와 태그를 확인한다.
        for (int i = 0; i < names.length; i++) {
            JsObject ctor = env.obj(names[i]);
            Check.same(ctor, ctor.get(WellKnownSymbols.SPECIES), names[i] + "[@@species] returns the constructor");
            Check.isTrue(ctor.getOwnProperty(WellKnownSymbols.SPECIES).isAccessor(), names[i] + "[@@species] is an accessor");
            Check.equal(names[i], Env.str(env.obj(names[i] + ".prototype").get(WellKnownSymbols.TO_STRING_TAG)), names[i] + " tag");
        }
        Check.equal("WeakMap", Env.str(env.obj("WeakMap.prototype").get(WellKnownSymbols.TO_STRING_TAG)), "WeakMap tag");
        Check.equal("WeakSet", Env.str(env.obj("WeakSet.prototype").get(WellKnownSymbols.TO_STRING_TAG)), "WeakSet tag");
        JsObject mapIter = env.realm.getIntrinsic("%MapIteratorPrototype%");
        Check.same(env.realm.getIteratorPrototype(), mapIter.getPrototypeOf(), "%MapIteratorPrototype% inherits %IteratorPrototype%");
        Check.equal("Map Iterator", Env.str(mapIter.get(WellKnownSymbols.TO_STRING_TAG)), "Map Iterator tag");
        Check.equal("Set Iterator", Env.str(env.realm.getIntrinsic("%SetIteratorPrototype%").get(WellKnownSymbols.TO_STRING_TAG)), "Set Iterator tag");
        JsValue it = env.invoke(env.construct("Map"), "keys");
        Check.same(it, env.invokeSymbol((JsObject) it, WellKnownSymbols.ITERATOR), "iterators return themselves from @@iterator");
        Check.equal("next", Env.str(((JsObject) mapIter.get("next")).get("name")), "next name");
        Check.equal(0.0, Env.d(((JsObject) mapIter.get("next")).get("length")), "next length");
        Check.same(env.realm.getIntrinsic("%MapPrototype%"), env.obj("Map.prototype"), "%MapPrototype% intrinsic");
    }

    /**
     * Receivers of the wrong kind are rejected; the prototype objects are not collections.
     * <p>
     * 잘못된 종류의 수신 객체는 거부되며 프로토타입 객체는 컬렉션이 아니다.
     */
    public void testReceiverChecks() {
        final JsObject set = env.construct("Set");
        final JsObject map = env.construct("Map");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls Map.prototype.get on a Set.
             * <p>
             * Set 에서 Map.prototype.get 을 호출한다.
             */
            public void run() {
                env.call("Map.prototype.get", set, Env.n(1));
            }
        }, "Map.prototype.get on a Set");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls Set.prototype.add on a Map.
             * <p>
             * Map 에서 Set.prototype.add 를 호출한다.
             */
            public void run() {
                env.call("Set.prototype.add", map, Env.n(1));
            }
        }, "Set.prototype.add on a Map");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Reads the size getter of the prototype.
             * <p>
             * 프로토타입의 size getter 를 읽는다.
             */
            public void run() {
                env.obj("Map.prototype").get("size");
            }
        }, "Map.prototype.size");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls the Set iterator's next on a Map iterator.
             * <p>
             * Map 이터레이터에서 Set 이터레이터의 next 를 호출한다.
             */
            public void run() {
                Ops.call(env.realm.getIntrinsic("%SetIteratorPrototype%").get("next"), env.invoke(map, "keys"), Js.NO_ARGS);
            }
        }, "next of the wrong iterator kind");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls WeakMap.prototype.set on a Map.
             * <p>
             * Map 에서 WeakMap.prototype.set 을 호출한다.
             */
            public void run() {
                env.call("WeakMap.prototype.set", map, env.realm.newObject(), Env.n(1));
            }
        }, "WeakMap.prototype.set on a Map");
    }
}
