package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.CollectionBuiltins.SetObject;

/**
 * The Set operations using the set-like object protocol defined by ECMA-262 §24.2.
 * <p>
 * ECMA-262 §24.2의 유사 집합 객체 규칙을 사용하는 Set 연산.
 */
final class SetOperations {

    /**
     * The members read from a set-like object before an operation begins.
     * <p>
     * 연산 시작 전에 유사 집합 객체에서 읽어 둔 속성.
     */
    private static final class SetRecord {
        /** Receiver for method calls. / 메서드 호출의 수신 객체. */
        private final JsObject object;

        /** The size converted to a non-negative integer or infinity. / 음이 아닌 정수 또는 무한대로 변환한 크기. */
        private final double size;

        /** The captured membership test. / 읽어 둔 포함 여부 검사 함수. */
        private final JsValue has;

        /** The captured method that supplies the keys iterator. / 읽어 둔 키 이터레이터 제공 메서드. */
        private final JsValue keys;

        /**
         * Performs GetSetRecord, reading the three properties in the specified order.
         * <p>
         * 규격에 정해진 순서로 세 프로퍼티를 읽으며 GetSetRecord를 실행한다.
         *
         * @param value the other operand / 다른 피연산자
         */
        SetRecord(JsValue value) {
            if (!(value instanceof JsObject)) {
                throw JsException.typeError("Expected a set-like object");
            }
            object = (JsObject) value;
            double numericSize = Ops.toNumber(object.get("size"));
            if (Double.isNaN(numericSize)) {
                throw JsException.typeError("Set-like size is NaN");
            }
            size = Ops.toInteger(numericSize);
            if (size < 0) {
                throw JsException.rangeError("Set-like size is negative");
            }
            has = object.get("has");
            if (!has.isCallable()) {
                throw JsException.typeError("Set-like has method is not callable");
            }
            keys = object.get("keys");
            if (!keys.isCallable()) {
                throw JsException.typeError("Set-like keys method is not callable");
            }
        }

        /**
         * Invokes the captured membership test.
         * <p>
         * 읽어 둔 포함 여부 검사 함수를 호출한다.
         *
         * @param key value to check / 검사할 값
         * @return boolean coercion of the returned value / 반환값을 불리언으로 변환한 결과
         */
        boolean contains(JsValue key) {
            return Ops.toBoolean(Ops.call(has, object, new JsValue[] { key }));
        }

        /**
         * Gets the iterator directly from the captured keys method.
         * <p>
         * 읽어 둔 keys 메서드에서 직접 이터레이터를 얻는다.
         *
         * @return iterator record over other keys / 상대 집합 키의 이터레이터 기록
         */
        IteratorRecord keyIterator() {
            return new IteratorRecord(IteratorHelper.getIteratorFromMethod(object, keys));
        }
    }

    /** Installs only static methods. / 정적 메서드만 설치한다. */
    private SetOperations() {
    }

    /**
     * Installs group operations on the Set prototype.
     * <p>
     * Set 프로토타입에 집합 연산을 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Set prototype / Set 프로토타입
     */
    static void install(JsRealm realm, JsObject prototype) {
        realm.method(prototype, "union", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "union");
                return union(r, set, new SetRecord(Js.arg(args, 0)), false);
            }
        });
        realm.method(prototype, "symmetricDifference", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "symmetricDifference");
                return union(r, set, new SetRecord(Js.arg(args, 0)), true);
            }
        });
        realm.method(prototype, "intersection", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "intersection");
                return intersection(r, set, new SetRecord(Js.arg(args, 0)));
            }
        });
        realm.method(prototype, "difference", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "difference");
                return difference(r, set, new SetRecord(Js.arg(args, 0)));
            }
        });
        installPredicates(realm, prototype);
    }

    /**
     * Installs the boolean predicates of Set.
     * <p>
     * 불리언을 반환하는 Set 조건 검사를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     * @param prototype Set prototype / Set 프로토타입
     */
    private static void installPredicates(JsRealm realm, JsObject prototype) {
        realm.method(prototype, "isSubsetOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "isSubsetOf");
                SetRecord other = new SetRecord(Js.arg(args, 0));
                return Js.bool(set.table.size() <= other.size && matchReceiver(r, set, other, true));
            }
        });
        realm.method(prototype, "isSupersetOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "isSupersetOf");
                SetRecord other = new SetRecord(Js.arg(args, 0));
                return Js.bool(set.table.size() >= other.size && matchKeys(r, set, other, true));
            }
        });
        realm.method(prototype, "isDisjointFrom", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SetObject set = CollectionBuiltins.thisSet(receiver, "isDisjointFrom");
                SetRecord other = new SetRecord(Js.arg(args, 0));
                return Js.bool(set.table.size() <= other.size ? matchReceiver(r, set, other, false) : matchKeys(r, set, other, false));
            }
        });
    }

    /**
     * Checks the other operand's has result for each live receiver key.
     * <p>
     * 수신 집합의 현재 키마다 상대 피연산자의 has 결과를 검사한다.
     *
     * @param realm current realm / 현재 realm
     * @param set receiver set / 수신 집합
     * @param other set-like argument / 유사 집합 인수
     * @param expected membership expected for every key / 모든 키에서 기대하는 포함 여부
     * @return true when every check matches / 모든 검사 결과가 기대와 같으면 true
     */
    private static boolean matchReceiver(JsRealm realm, SetObject set, SetRecord other, boolean expected) {
        OrderedTable.Cursor cursor = set.table.cursor();
        OrderedTable.Entry entry;
        // has가 수신 집합을 늘릴 수 있으므로 현재 키를 순회하고 다른 결과에서 바로 끝낸다.
        while ((entry = cursor.next()) != null) {
            realm.getLimits().tick();
            if (other.contains(entry.key) != expected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the live receiver's membership for each key of the other operand.
     * <p>
     * 상대 피연산자의 키마다 수신 집합의 현재 포함 여부를 검사한다.
     *
     * @param realm current realm / 현재 realm
     * @param set receiver set / 수신 집합
     * @param other set-like argument / 유사 집합 인수
     * @param expected membership expected for every key / 모든 키에서 기대하는 포함 여부
     * @return true when every check matches / 모든 검사 결과가 기대와 같으면 true
     */
    private static boolean matchKeys(JsRealm realm, SetObject set, SetRecord other, boolean expected) {
        IteratorRecord iterator = other.keyIterator();
        JsValue key;
        // 불일치로 조기에 끝낼 때는 상대 이터레이터를 정상적으로 닫는다.
        while ((key = iterator.stepValue()) != null) {
            realm.getLimits().tick();
            if ((set.table.find(key) != null) != expected) {
                iterator.close();
                return false;
            }
        }
        return true;
    }

    /**
     * Copies entries into a fresh set in their current insertion order.
     * <p>
     * 현재 삽입 순서대로 항목을 새 집합에 복사한다.
     *
     * @param realm realm of the result / 결과의 realm
     * @param source the original set / 원본 집합
     * @return a copy with the intrinsic Set prototype / 내장 Set 프로토타입을 가진 사본
     */
    private static SetObject copySet(JsRealm realm, SetObject source) {
        SetObject result = new SetObject(realm.getIntrinsic("%SetPrototype%"));
        OrderedTable.Cursor cursor = source.table.cursor();
        OrderedTable.Entry entry;
        // 사용자 코드를 실행하지 않고 현재 집합의 항목만 순서대로 복제한다.
        while ((entry = cursor.next()) != null) {
            realm.getLimits().tick();
            result.table.put(entry.key, entry.key);
        }
        return result;
    }

    /**
     * Adds the other keys in iteration order, optionally removing keys present in both sets.
     * <p>
     * 상대의 키를 순회 순서대로 추가한다. 대칭차집합에서는 양쪽에 있는 키를 제거한다.
     *
     * @param realm result realm / 결과의 realm
     * @param set the original set / 원본 집합
     * @param other set-like argument / 유사 집합 인수
     * @param symmetric whether to compute symmetric difference / 대칭차집합을 구할지 여부
     * @return the union or symmetric difference / 합집합 또는 대칭차집합
     */
    private static SetObject union(JsRealm realm, SetObject set, SetRecord other, boolean symmetric) {
        IteratorRecord iterator = other.keyIterator();
        SetObject result = copySet(realm, set);
        JsValue key;
        // keys 또는 next 조회가 원본을 바꾼 뒤의 상태를 복사하고 실제 순회를 시작한다.
        while ((key = iterator.stepValue()) != null) {
            realm.getLimits().tick();
            if (symmetric && set.table.find(key) != null) {
                result.table.remove(key);
            } else {
                result.table.put(key, key);
            }
        }
        return result;
    }

    /**
     * Intersects the operands, retaining the iteration order selected by their relative sizes.
     * <p>
     * 두 피연산자의 상대 크기로 선택한 순회 순서를 유지하며 교집합을 구한다.
     *
     * @param realm result realm / 결과의 realm
     * @param set receiver set / 수신 집합
     * @param other set-like argument / 유사 집합 인수
     * @return the intersection / 교집합
     */
    private static SetObject intersection(JsRealm realm, SetObject set, SetRecord other) {
        SetObject result = new SetObject(realm.getIntrinsic("%SetPrototype%"));
        if (set.table.size() <= other.size) {
            OrderedTable.Cursor cursor = set.table.cursor();
            OrderedTable.Entry entry;
            // has가 원본에 새 키를 삽입하면 같은 커서에서 뒤이어 방문할 수 있다.
            while ((entry = cursor.next()) != null) {
                realm.getLimits().tick();
                if (other.contains(entry.key)) {
                    result.table.put(entry.key, entry.key);
                }
            }
        } else {
            IteratorRecord iterator = other.keyIterator();
            JsValue key;
            // 더 작은 상대의 키 순서를 따르고 원본의 현재 구성원 여부를 확인한다.
            while ((key = iterator.stepValue()) != null) {
                realm.getLimits().tick();
                if (set.table.find(key) != null) {
                    result.table.put(key, key);
                }
            }
        }
        return result;
    }

    /**
     * Removes the other keys from a snapshot of the receiver.
     * <p>
     * 수신 집합의 사본에서 상대의 키를 제거한다.
     *
     * @param realm result realm / 결과의 realm
     * @param set receiver set / 수신 집합
     * @param other set-like argument / 유사 집합 인수
     * @return the receiver's difference from the other set / 수신 집합에서 상대 집합을 뺀 결과
     */
    private static SetObject difference(JsRealm realm, SetObject set, SetRecord other) {
        SetObject result = copySet(realm, set);
        if (set.table.size() <= other.size) {
            OrderedTable.Cursor cursor = result.table.cursor();
            OrderedTable.Entry entry;
            // 상대의 has가 원본을 바꿔도 사본의 후보 키만 검사한다.
            while ((entry = cursor.next()) != null) {
                realm.getLimits().tick();
                if (other.contains(entry.key)) {
                    result.table.remove(entry.key);
                }
            }
        } else {
            IteratorRecord iterator = other.keyIterator();
            JsValue key;
            // 사본을 만든 뒤 상대의 키를 차례로 제거한다.
            while ((key = iterator.stepValue()) != null) {
                realm.getLimits().tick();
                result.table.remove(key);
            }
        }
        return result;
    }
}
