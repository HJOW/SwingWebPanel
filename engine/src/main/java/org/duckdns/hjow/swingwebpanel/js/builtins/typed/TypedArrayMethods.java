package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsBigInt;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The methods and accessors of <code>%TypedArray%.prototype</code> (ECMA-262 6th edition, 22.2.3): buffer, byteLength, byteOffset, length and the
 * toStringTag getter, and copyWithin, entries, every, fill, filter, find, findIndex, forEach, indexOf, join, keys, lastIndexOf, map, reduce,
 * reduceRight, reverse, set, slice, some, sort, subarray, toLocaleString, toString, values and <code>[Symbol.iterator]</code>.
 * Later methods include includes, at, findLast, findLastIndex, toReversed, toSorted and with.
 * <p>
 * <code>%TypedArray%.prototype</code> 의 메서드와 접근자(ECMA-262 6판, 22.2.3): buffer, byteLength, byteOffset, length, toStringTag getter 와
 * copyWithin, entries, every, fill, filter, find, findIndex, forEach, indexOf, join, keys, lastIndexOf, map, reduce, reduceRight, reverse, set,
 * slice, some, sort, subarray, toLocaleString, toString, values, <code>[Symbol.iterator]</code>.
 * 이후 판본에서 추가된 includes, at, findLast, findLastIndex, toReversed, toSorted, with도 설치한다.
 */
final class TypedArrayMethods {

    /**
     * Compares two numbers for sorting.
     * <p>
     * 정렬을 위해 숫자 두 개를 비교한다.
     */
    private interface ValueOrder {
        /**
         * Compares two numbers.
         * <p>
         * 숫자 두 개를 비교한다.
         *
         * @param x first number / 첫 번째 숫자
         * @param y second number / 두 번째 숫자
         * @return negative, zero or positive / 음수, 0, 양수
         */
        int compare(JsValue x, JsValue y);
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TypedArrayMethods() {
    }

    /**
     * Returns the this value as a typed array (without the detached check) or throws a TypeError.
     * <p>
     * this 값을 형식화 배열로 반환하거나(분리 검사 없이) TypeError 를 던진다.
     *
     * @param v the this value / this 값
     * @return the typed array / 형식화 배열
     */
    private static TypedArrayObject thisArray(JsValue v) {
        if (!(v instanceof TypedArrayObject)) {
            throw JsException.typeError("this is not a typed array");
        }
        return (TypedArrayObject) v;
    }

    /**
     * Reads an element for a callback, undefined if the array shrank meanwhile.
     * <p>
     * 콜백에 줄 요소를 읽는다. 그사이 배열이 줄었으면 undefined.
     *
     * @param o the array / 배열
     * @param k the index / 인덱스
     * @return the element / 요소
     */
    private static JsValue elem(TypedArrayObject o, int k) {
        return k < o.getLength() ? o.getElementValue(k) : Js.UNDEFINED;
    }

    /**
     * Builds the argument list (value, index, array) of an iteration callback.
     * <p>
     * 반복 콜백의 인수 목록(값, 인덱스, 배열)을 만든다.
     *
     * @param v the element / 요소
     * @param k the index / 인덱스
     * @param o the array / 배열
     * @return the arguments / 인수
     */
    private static JsValue[] cb(JsValue v, int k, JsObject o) {
        return new JsValue[] { v, Js.num(k), o };
    }

    /**
     * Finds a matching element or index in the requested direction.
     * <p>
     * 지정한 방향에서 조건을 만족하는 요소나 인덱스를 찾는다.
     *
     * @param receiver the typed array / 형식화 배열
     * @param args callback and thisArg / 콜백과 thisArg
     * @param indexResult whether to return the index / 인덱스를 반환할지 여부
     * @param reverse true when searching from the end / 끝에서 검색하면 true
     * @return the matching value or index, or the matching method's sentinel / 일치한 값이나 인덱스, 없으면 메서드별 기본값
     */
    private static JsValue find(JsValue receiver, JsValue[] args, boolean indexResult, boolean reverse) {
        TypedArrayObject source = TypedSupport.validate(receiver);
        int length = source.getLength();
        JsObject predicate = TypedSupport.callable(Js.arg(args, 0));
        JsValue thisArg = Js.arg(args, 1);
        int step = reverse ? -1 : 1;
        // 진입 시 길이를 고정하고 콜백 직전에 각 인덱스의 현재 값을 읽는다.
        for (int i = reverse ? length - 1 : 0; i >= 0 && i < length; i += step) {
            JsValue value = elem(source, i);
            if (Ops.toBoolean(Ops.call(predicate, thisArg, cb(value, i, source)))) {
                return indexResult ? Js.num(i) : value;
            }
        }
        return indexResult ? Js.num(-1) : Js.UNDEFINED;
    }

    /**
     * Installs the prototype methods.
     * <p>
     * 프로토타입 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto <code>%TypedArray%.prototype</code> / <code>%TypedArray%.prototype</code>
     */
    static void install(final JsRealm realm, JsObject proto) {
        installAccessors(realm, proto);
        installIteration(realm, proto);
        installSearching(realm, proto);
        installMutation(realm, proto);
        installCopying(realm, proto);
    }

    /**
     * Installs the accessors buffer, byteLength, byteOffset, length and the toStringTag getter.
     * <p>
     * buffer, byteLength, byteOffset, length 접근자와 toStringTag getter 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto the prototype / 프로토타입
     */
    private static void installAccessors(JsRealm realm, JsObject proto) {
        realm.getter(proto, "buffer", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisArray(thisValue).getBuffer();
            }
        });
        realm.getter(proto, "byteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisArray(thisValue).getByteLength());
            }
        });
        realm.getter(proto, "byteOffset", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisArray(thisValue).getByteOffset());
            }
        });
        realm.getter(proto, "length", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisArray(thisValue).getLength());
            }
        });
        realm.getter(proto, WellKnownSymbols.TO_STRING_TAG, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof TypedArrayObject)) {
                    return Js.UNDEFINED;
                }
                return Js.str(((TypedArrayObject) thisValue).getType().getConstructorName());
            }
        });
    }

    /**
     * Installs entries, keys, values, [Symbol.iterator], forEach, every, some, find, findIndex, map, filter, reduce and reduceRight.
     * <p>
     * entries, keys, values, [Symbol.iterator], forEach, every, some, find, findIndex, map, filter, reduce, reduceRight 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto the prototype / 프로토타입
     */
    private static void installIteration(JsRealm realm, JsObject proto) {
        realm.method(proto, "at", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject array = TypedSupport.validate(thisValue);
                int length = array.getLength();
                double relative = Ops.toInteger(Js.arg(args, 0));
                double index = relative < 0 ? length + relative : relative;
                if (index < 0 || index >= length) {
                    return Js.UNDEFINED;
                }
                return elem(array, (int) index);
            }
        });
        realm.method(proto, "entries", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedArrayIterator.create(r, TypedSupport.validate(thisValue), TypedArrayIterator.ENTRIES);
            }
        });
        realm.method(proto, "keys", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedArrayIterator.create(r, TypedSupport.validate(thisValue), TypedArrayIterator.KEYS);
            }
        });
        NativeFunction values = realm.method(proto, "values", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedArrayIterator.create(r, TypedSupport.validate(thisValue), TypedArrayIterator.VALUES);
            }
        });
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, values);
        realm.method(proto, "forEach", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                JsValue t = Js.arg(args, 1);
                // 요소마다 콜백을 호출한다.
                for (int k = 0; k < len; k++) {
                    Ops.call(fn, t, cb(elem(o, k), k, o));
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "every", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                JsValue t = Js.arg(args, 1);
                // 콜백이 거짓을 돌려주는 첫 요소에서 멈춘다.
                for (int k = 0; k < len; k++) {
                    if (!Ops.toBoolean(Ops.call(fn, t, cb(elem(o, k), k, o)))) {
                        return Js.FALSE;
                    }
                }
                return Js.TRUE;
            }
        });
        realm.method(proto, "some", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                JsValue t = Js.arg(args, 1);
                // 콜백이 참을 돌려주는 첫 요소에서 멈춘다.
                for (int k = 0; k < len; k++) {
                    if (Ops.toBoolean(Ops.call(fn, t, cb(elem(o, k), k, o)))) {
                        return Js.TRUE;
                    }
                }
                return Js.FALSE;
            }
        });
        realm.method(proto, "find", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(thisValue, args, false, false);
            }
        });
        realm.method(proto, "findIndex", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(thisValue, args, true, false);
            }
        });
        realm.method(proto, "findLast", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(thisValue, args, false, true);
            }
        });
        realm.method(proto, "findLastIndex", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(thisValue, args, true, true);
            }
        });
        realm.method(proto, "map", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                JsValue t = Js.arg(args, 1);
                TypedArrayObject a = TypedSupport.speciesCreate(r, o, new JsValue[] { Js.num(len) });
                // 콜백의 결과를 새 배열의 같은 위치에 저장한다.
                for (int k = 0; k < len; k++) {
                    JsValue mapped = Ops.call(fn, t, cb(elem(o, k), k, o));
                    a.setIndexed(k, mapped);
                }
                return a;
            }
        });
        realm.method(proto, "filter", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                JsValue t = Js.arg(args, 1);
                List<JsValue> kept = new ArrayList<JsValue>();
                // 콜백이 참을 돌려준 요소만 모은다.
                for (int k = 0; k < len; k++) {
                    JsValue v = elem(o, k);
                    if (Ops.toBoolean(Ops.call(fn, t, cb(v, k, o)))) {
                        kept.add(v);
                    }
                }
                TypedArrayObject a = TypedSupport.speciesCreate(r, o, new JsValue[] { Js.num(kept.size()) });
                // 모은 요소를 새 배열에 차례로 저장한다.
                for (int n = 0; n < kept.size(); n++) {
                    a.setIndexed(n, kept.get(n));
                }
                return a;
            }
        });
        realm.method(proto, "reduce", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                int k = 0;
                JsValue acc;
                if (args.length >= 2) {
                    acc = args[1];
                } else {
                    if (len == 0) {
                        throw JsException.typeError("Reduce of empty array with no initial value");
                    }
                    acc = elem(o, 0);
                    k = 1;
                }
                // 누적값과 요소를 콜백에 차례로 넘긴다.
                for (; k < len; k++) {
                    acc = Ops.call(fn, Js.UNDEFINED, new JsValue[] { acc, elem(o, k), Js.num(k), o });
                }
                return acc;
            }
        });
        realm.method(proto, "reduceRight", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsObject fn = TypedSupport.callable(Js.arg(args, 0));
                int k = len - 1;
                JsValue acc;
                if (args.length >= 2) {
                    acc = args[1];
                } else {
                    if (len == 0) {
                        throw JsException.typeError("Reduce of empty array with no initial value");
                    }
                    acc = elem(o, k);
                    k--;
                }
                // 뒤에서부터 누적값과 요소를 콜백에 넘긴다.
                for (; k >= 0; k--) {
                    acc = Ops.call(fn, Js.UNDEFINED, new JsValue[] { acc, elem(o, k), Js.num(k), o });
                }
                return acc;
            }
        });
    }

    /**
     * Installs indexOf, lastIndexOf, join, toLocaleString and toString.
     * <p>
     * indexOf, lastIndexOf, join, toLocaleString, toString 을 설치한다.
     *
     * @param realm the realm / realm
     * @param proto the prototype / 프로토타입
     */
    private static void installSearching(JsRealm realm, JsObject proto) {
        realm.method(proto, "indexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                if (len == 0) {
                    return Js.num(-1);
                }
                double n = Ops.toInteger(Js.arg(args, 1));
                if (n >= len) {
                    return Js.num(-1);
                }
                int k = n >= 0 ? (int) n : (int) Math.max(len + n, 0);
                JsValue target = Js.arg(args, 0);
                // 엄격한 동등 비교로 처음 일치하는 위치를 찾는다.
                for (; k < len; k++) {
                    if (k < o.getLength() && Ops.strictEquals(target, o.getElementValue(k))) {
                        return Js.num(k);
                    }
                }
                return Js.num(-1);
            }
        });
        realm.method(proto, "lastIndexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                if (len == 0) {
                    return Js.num(-1);
                }
                double n = args.length > 1 ? Ops.toInteger(args[1]) : len - 1;
                int k = n >= 0 ? (int) Math.min(n, len - 1) : (int) (len + n);
                JsValue target = Js.arg(args, 0);
                // 뒤에서부터 엄격한 동등 비교로 처음 일치하는 위치를 찾는다.
                for (; k >= 0; k--) {
                    if (k < o.getLength() && Ops.strictEquals(target, o.getElementValue(k))) {
                        return Js.num(k);
                    }
                }
                return Js.num(-1);
            }
        });
        realm.method(proto, "join", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsValue s = Js.arg(args, 0);
                String sep = s.isUndefined() ? "," : Ops.toString(s);
                StringBuilder sb = new StringBuilder();
                // 요소를 구분자로 이어 붙인다.
                for (int k = 0; k < len; k++) {
                    if (k > 0) {
                        sb.append(sep);
                    }
                    if (k < o.getLength()) {
                        sb.append(Ops.toString(o.getElementValue(k)));
                    }
                }
                return Js.str(sb.toString());
            }
        });
        realm.method(proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                StringBuilder sb = new StringBuilder();
                // 요소마다 toLocaleString 을 호출해 ',' 로 이어 붙인다.
                for (int k = 0; k < len; k++) {
                    if (k > 0) {
                        sb.append(',');
                    }
                    JsValue e = elem(o, k);
                    if (!e.isNullOrUndefined()) {
                        sb.append(Ops.toString(Ops.invoke(r, e, "toLocaleString", Js.NO_ARGS)));
                    }
                }
                return Js.str(sb.toString());
            }
        });
        JsValue arrayToString = realm.getArrayPrototype().get("toString");
        if (arrayToString instanceof JsObject && arrayToString.isCallable()) {
            proto.defineBuiltin("toString", arrayToString);
        } else {
            realm.method(proto, "toString", 0, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    JsObject o = Ops.toObject(r, thisValue);
                    JsValue join = o.get("join");
                    if (join.isCallable()) {
                        return Ops.call(join, o, Js.NO_ARGS);
                    }
                    throw JsException.typeError("join is not a function");
                }
            });
        }
    }

    /**
     * Installs fill, reverse, sort, set and copyWithin: methods that change the array in place.
     * <p>
     * 배열을 제자리에서 바꾸는 fill, reverse, sort, set, copyWithin 을 설치한다.
     *
     * @param realm the realm / realm
     * @param proto the prototype / 프로토타입
     */
    private static void installMutation(JsRealm realm, JsObject proto) {
        realm.method(proto, "fill", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsValue value = o.convertElement(Js.arg(args, 0));
                int k = TypedSupport.relative(Js.arg(args, 1), len, 0);
                int fin = TypedSupport.relative(Js.arg(args, 2), len, len);
                // 인수 변환 중 줄어들거나 분리된 뷰의 현재 경계를 검사한다.
                TypedSupport.validate(o);
                int end = Math.min(fin, o.getLength());
                // 구간의 모든 요소를 같은 값으로 채운다.
                for (; k < end; k++) {
                    o.setElementValue(k, value);
                }
                return o;
            }
        });
        realm.method(proto, "reverse", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                // 양 끝에서 안쪽으로 요소를 맞바꾼다.
                for (int lower = 0, upper = len - 1; lower < upper; lower++, upper--) {
                    JsValue a = o.getElementValue(lower);
                    o.setElementValue(lower, o.getElementValue(upper));
                    o.setElementValue(upper, a);
                }
                return o;
            }
        });
        realm.method(proto, "sort", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue compare = comparator(args);
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                JsValue[] values = sortedValues(o, len, compare);
                int end = Math.min(len, o.getLength());
                // 정렬한 값을 다시 쓴다. 콜백이 배열을 줄였다면 남은 부분만 쓴다.
                for (int i = 0; i < end; i++) {
                    o.setElementValue(i, values[i]);
                }
                return o;
            }
        });
        realm.method(proto, "set", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject target = thisArray(thisValue);
                double offset = Ops.toInteger(Js.arg(args, 1));
                if (offset < 0) {
                    throw JsException.rangeError("offset is out of bounds");
                }
                TypedSupport.validate(target);
                JsValue source = Js.arg(args, 0);
                if (source instanceof TypedArrayObject) {
                    setFromTypedArray(target, offset, (TypedArrayObject) source);
                } else {
                    setFromArrayLike(r, target, offset, source);
                }
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "copyWithin", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                int to = TypedSupport.relative(Js.arg(args, 0), len, 0);
                int from = TypedSupport.relative(Js.arg(args, 1), len, 0);
                int fin = TypedSupport.relative(Js.arg(args, 2), len, len);
                int count = Math.min(fin - from, len - to);
                if (count > 0) {
                    TypedSupport.validate(o);
                    int size = o.getType().getSize();
                    ArrayBufferObject buffer = o.getBuffer();
                    int base = o.getByteOffset();
                    int available = o.getLength();
                    int n = Math.min(count, Math.min(available - from, available - to));
                    if (n > 0) {
                        buffer.copyBytesTo(base + from * size, buffer, base + to * size, n * size);
                    }
                }
                return o;
            }
        });
    }

    /**
     * Installs slice and subarray: the methods that create a new typed array through the species constructor.
     * <p>
     * species 생성자로 새 형식화 배열을 만드는 slice 와 subarray 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto the prototype / 프로토타입
     */
    private static void installCopying(JsRealm realm, JsObject proto) {
        realm.method(proto, "toReversed", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject source = TypedSupport.validate(thisValue);
                int length = source.getLength();
                TypedArrayObject result = TypedArrayFactory.allocateSameType(r, source, length);
                // 원본을 끝에서부터 읽어 같은 요소 형식의 배열에 쓴다.
                for (int i = 0; i < length; i++) {
                    result.setElementValue(i, source.getElementValue(length - i - 1));
                }
                return result;
            }
        });
        realm.method(proto, "toSorted", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue compare = comparator(args);
                TypedArrayObject source = TypedSupport.validate(thisValue);
                int length = source.getLength();
                TypedArrayObject result = TypedArrayFactory.allocateSameType(r, source, length);
                JsValue[] sorted = sortedValues(source, length, compare);
                // 정렬 비교가 끝난 값을 결과에만 쓴다.
                for (int i = 0; i < length; i++) {
                    result.setElementValue(i, sorted[i]);
                }
                return result;
            }
        });
        realm.method(proto, "with", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject source = TypedSupport.validate(thisValue);
                int length = source.getLength();
                double relative = Ops.toInteger(Js.arg(args, 0));
                double index = relative < 0 ? length + relative : relative;
                JsValue replacement = source.convertElement(Js.arg(args, 1));
                // 변환 도중 버퍼가 분리되거나 길이가 변할 수 있어 현재 길이로 검사한다.
                if (index < 0 || index >= source.getLength()) {
                    throw JsException.rangeError("TypedArray index is out of range");
                }
                TypedArrayObject result = TypedArrayFactory.allocateSameType(r, source, length);
                // 원래 길이를 복사한다. 길이가 줄었다면 남은 인덱스의 읽기 결과는 undefined다.
                for (int i = 0; i < length; i++) {
                    result.setElementValue(i, i == index ? replacement : elem(source, i));
                }
                return result;
            }
        });
        realm.method(proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = TypedSupport.validate(thisValue);
                int len = o.getLength();
                int k = TypedSupport.relative(Js.arg(args, 0), len, 0);
                int fin = TypedSupport.relative(Js.arg(args, 1), len, len);
                int count = Math.max(fin - k, 0);
                TypedArrayObject a = TypedSupport.speciesCreate(r, o, new JsValue[] { Js.num(count) });
                if (count > 0) {
                    TypedSupport.validate(o);
                    int available = Math.min(count, Math.min(o.getLength() - k, a.getLength()));
                    if (available > 0 && a.getType() == o.getType()) {
                        int size = o.getType().getSize();
                        o.getBuffer().copyBytesTo(o.getByteOffset() + k * size, a.getBuffer(), a.getByteOffset(), available * size);
                    } else {
                        // 요소 형식이 다르면 요소마다 숫자로 읽어 변환해서 쓴다.
                        for (int n = 0; n < available; n++) {
                            a.setElementValue(n, o.getElementValue(k + n));
                        }
                    }
                }
                return a;
            }
        });
        realm.method(proto, "subarray", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                TypedArrayObject o = thisArray(thisValue);
                int srcLength = o.getLength();
                int begin = TypedSupport.relative(Js.arg(args, 0), srcLength, 0);
                double beginByteOffset = (double) o.getRawByteOffset() + (double) begin * o.getType().getSize();
                JsValue endArg = Js.arg(args, 1);
                // 원본이 길이를 추적하고 끝 위치가 없으면 결과도 길이를 추적한다.
                if (o.isLengthTracking() && endArg.isUndefined()) {
                    return TypedSupport.speciesCreate(r, o, new JsValue[] { o.getBuffer(), Js.num(beginByteOffset) });
                }
                int end = TypedSupport.relative(endArg, srcLength, srcLength);
                int newLength = Math.max(end - begin, 0);
                return TypedSupport.speciesCreate(r, o, new JsValue[] { o.getBuffer(), Js.num(beginByteOffset), Js.num(newLength) });
            }
        });
    }

    /**
     * SetTypedArrayFromTypedArray (22.2.3.22.1): copies the elements of another typed array, safely when both share a buffer.
     * <p>
     * SetTypedArrayFromTypedArray (22.2.3.22.1): 다른 형식화 배열의 요소를 복사한다. 두 배열이 버퍼를 공유해도 안전하다.
     *
     * @param target the array that receives the elements / 요소를 받는 배열
     * @param offset the first element to write / 쓰기 시작할 요소
     * @param source the source array / 원본 배열
     */
    private static void setFromTypedArray(TypedArrayObject target, double offset, TypedArrayObject source) {
        TypedSupport.validate(source);
        int srcLength = source.getLength();
        if (srcLength + offset > target.getLength()) {
            throw JsException.rangeError("offset is out of bounds");
        }
        if (source.getType().isBigInt() != target.getType().isBigInt()) {
            throw JsException.typeError("Cannot mix BigInt and Number typed arrays");
        }
        int at = (int) offset;
        ArrayBufferObject input = source.getBuffer();
        int sourceOffset = source.getByteOffset();
        int size = source.getType().getSize();
        ArrayBufferObject output = target.getBuffer();
        boolean sameType = source.getType() == target.getType();
        if (input.sharesStorageWith(output) && (input.isShared() || !sameType)) {
            // 공유 저장소가 같거나 같은 버퍼의 요소 크기를 변환하면 쓰기 전에 원본 바이트를 복제한다.
            ArrayBufferObject clone = new ArrayBufferObject(null, TypedSupport.allocate(srcLength * size));
            input.copyBytesTo(sourceOffset, clone, 0, srcLength * size);
            input = clone;
            sourceOffset = 0;
        }
        if (sameType) {
            input.copyBytesTo(sourceOffset, output, target.getByteOffset() + at * size, srcLength * size);
            return;
        }
        // 서로 겹치지 않는 원본을 요소별로 읽어 대상 형식으로 변환해 쓴다.
        for (int i = 0; i < srcLength; i++) {
            target.setElementValue(at + i, input.readValue(source.getType(), sourceOffset + i * size, true));
        }
    }

    /**
     * SetTypedArrayFromArrayLike (22.2.3.22.2): copies the elements of an array or array-like object.
     * <p>
     * SetTypedArrayFromArrayLike (22.2.3.22.2): 배열이나 유사 배열 객체의 요소를 복사한다.
     *
     * @param realm  the realm / realm
     * @param target the array that receives the elements / 요소를 받는 배열
     * @param offset the first element to write / 쓰기 시작할 요소
     * @param source the source value / 원본 값
     */
    private static void setFromArrayLike(JsRealm realm, TypedArrayObject target, double offset, JsValue source) {
        int targetLength = target.getLength();
        JsObject src = Ops.toObject(realm, source);
        double srcLength = Ops.lengthOf(src);
        if (srcLength + offset > targetLength) {
            throw JsException.rangeError("offset is out of bounds");
        }
        int at = (int) offset;
        // 원본 요소를 차례로 읽어 숫자로 변환해 쓴다.
        for (int k = 0; k < (int) srcLength; k++) {
            target.setIndexed(at + k, src.get(PropertyKeys.fromIndex(k)));
        }
    }

    /**
     * Reads and validates the optional comparator before reading the receiver.
     * <p>
     * 수신자를 읽기 전에 선택적 비교 함수를 읽고 검사한다.
     *
     * @param args the arguments of sort or toSorted / sort 또는 toSorted의 인수
     * @return the comparator or undefined / 비교 함수 또는 undefined
     */
    private static JsValue comparator(JsValue[] args) {
        JsValue compare = Js.arg(args, 0);
        if (!compare.isUndefined() && !compare.isCallable()) {
            throw JsException.typeError("The comparison function must be either a function or undefined");
        }
        return compare;
    }

    /**
     * Reads the elements before sorting them with the same order for sort and toSorted.
     * <p>
     * 요소를 읽은 뒤 sort와 toSorted에 같은 비교 규칙을 적용한다.
     *
     * @param source the typed array / 형식화 배열
     * @param length captured length / 미리 읽은 길이
     * @param compare comparator or undefined / 비교 함수 또는 undefined
     * @return sorted values / 정렬한 값
     */
    private static JsValue[] sortedValues(TypedArrayObject source, int length, final JsValue compare) {
        JsValue[] values = new JsValue[length];
        // 첫 비교를 하기 전에 원본의 모든 값을 읽는다.
        for (int i = 0; i < length; i++) {
            values[i] = source.getElementValue(i);
        }
        ValueOrder order;
        if (compare.isUndefined()) {
            order = new ValueOrder() {
                public int compare(JsValue x, JsValue y) {
                    return defaultElementOrder(x, y);
                }
            };
        } else {
            order = new ValueOrder() {
                public int compare(JsValue x, JsValue y) {
                    double v = Ops.toNumber(Ops.call(compare, Js.UNDEFINED, new JsValue[] { x, y }));
                    return v != v ? 0 : (v < 0 ? -1 : (v > 0 ? 1 : 0));
                }
            };
        }
        mergeSort(values, order);
        return values;
    }

    /**
     * The default sort order of typed arrays (22.2.3.26): numeric, NaN last, -0 before +0.
     * <p>
     * 형식화 배열의 기본 정렬 순서(22.2.3.26): 숫자 순서, NaN 은 맨 뒤, -0 이 +0 보다 앞.
     *
     * @param x first number / 첫 번째 숫자
     * @param y second number / 두 번째 숫자
     * @return negative, zero or positive / 음수, 0, 양수
     */
    static int defaultOrder(double x, double y) {
        if (x != x) {
            return y != y ? 0 : 1;
        }
        if (y != y) {
            return -1;
        }
        if (x < y) {
            return -1;
        }
        if (x > y) {
            return 1;
        }
        if (x == 0 && y == 0) {
            boolean xNegative = 1 / x < 0;
            boolean yNegative = 1 / y < 0;
            if (xNegative && !yNegative) {
                return -1;
            }
            if (!xNegative && yNegative) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Applies the typed-array default numeric sort to Number or BigInt values.
     * <p>
     * Number 또는 BigInt 값에 형식화 배열의 기본 수치 정렬을 적용한다.
     *
     * @param x first value / 첫 값
     * @param y second value / 둘째 값
     * @return negative, zero or positive / 음수, 0, 양수
     */
    static int defaultElementOrder(JsValue x, JsValue y) {
        if (x instanceof JsBigInt && y instanceof JsBigInt) {
            return ((JsBigInt) x).getValue().compareTo(((JsBigInt) y).getValue());
        }
        return defaultOrder(((JsNumber) x).getValue(), ((JsNumber) y).getValue());
    }

    /**
     * A stable bottom-up merge sort. It never throws because of an inconsistent comparison (unlike some library sorts), and exceptions of
     * the comparison function simply propagate.
     * <p>
     * 안정적인 상향식 병합 정렬. 일관성 없는 비교 때문에 예외를 던지지 않으며(일부 라이브러리 정렬과 다름) 비교 함수의 예외는 그대로 전파된다.
     *
     * @param a     the values, sorted in place / 값들(제자리 정렬)
     * @param order the comparison / 비교
     */
    private static void mergeSort(JsValue[] a, ValueOrder order) {
        int n = a.length;
        JsValue[] src = a;
        JsValue[] dst = new JsValue[n];
        // 병합 구간 폭을 1, 2, 4... 로 늘려 가며 두 구간씩 합친다.
        for (int width = 1; width < n; width *= 2) {
            for (int lo = 0; lo < n; lo += 2 * width) {
                int mid = Math.min(lo + width, n);
                int hi = Math.min(lo + 2 * width, n);
                int i = lo;
                int j = mid;
                // 두 구간에서 작은 것을 먼저 옮긴다. 같으면 왼쪽을 먼저 옮겨 안정성을 지킨다.
                for (int k = lo; k < hi; k++) {
                    if (i < mid && (j >= hi || order.compare(src[i], src[j]) <= 0)) {
                        dst[k] = src[i++];
                    } else {
                        dst[k] = src[j++];
                    }
                }
            }
            JsValue[] t = src;
            src = dst;
            dst = t;
        }
        if (src != a) {
            System.arraycopy(src, 0, a, 0, n);
        }
    }
}
