package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The methods of <code>Array.prototype</code> (ECMA-262 6th edition 22.1.3). Every method is generic: it works on any object through ToObject and
 * the object's own internal methods (Get, Set, HasProperty, DeletePropertyOrThrow, CreateDataPropertyOrThrow), so array-likes, holes, accessors
 * and prototype chains behave as specified. Methods that return a new array use ArraySpeciesCreate. <code>includes</code> (ES2016) is installed as a
 * documented extension.
 * <p>
 * Implementation notes: <code>join</code>, <code>toString</code> and <code>toLocaleString</code> return the empty string for an array that
 * contains itself (directly or indirectly) as real engines do, instead of overflowing the stack; <code>sort</code> is stable and does not modify the
 * array when the comparison function throws; lengths larger than 2<sup>53</sup>-1 after growing are a TypeError as in later editions of the
 * specification.
 * <p>
 * <code>Array.prototype</code> 의 메서드(ECMA-262 6판 22.1.3). 모든 메서드는 일반적이다: ToObject 와 객체 자신의 내부 메서드(Get, Set, HasProperty,
 * DeletePropertyOrThrow, CreateDataPropertyOrThrow)로 임의의 객체에서 동작하므로 유사 배열, 구멍, 접근자, 프로토타입 체인이 규격대로 동작한다.
 * 새 배열을 돌려주는 메서드는 ArraySpeciesCreate 를 쓴다. <code>includes</code>(ES2016)는 문서화한 확장으로 설치한다.
 * <p>
 * 구현 메모: <code>join</code>, <code>toString</code>, <code>toLocaleString</code> 은 자기 자신을 (직접 또는 간접으로) 포함하는 배열에 대해 실제 엔진처럼
 * 스택을 넘치게 하지 않고 빈 문자열을 돌려준다. <code>sort</code> 는 안정적이며 비교 함수가 예외를 던져도 배열을 바꾸지 않는다. 늘어난 뒤의 길이가
 * 2<sup>53</sup>-1 을 넘으면 규격의 이후 판처럼 TypeError 이다.
 */
final class ArrayMethods {

    /**
     * Arrays whose join is currently running on this thread (cycle detection).
     * <p>
     * 이 스레드에서 join 이 진행 중인 배열(순환 감지용).
     */
    private static final ThreadLocal<Set<JsObject>> JOINING = new ThreadLocal<Set<JsObject>>() {
        protected Set<JsObject> initialValue() {
            return Collections.newSetFromMap(new IdentityHashMap<JsObject, Boolean>());
        }
    };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ArrayMethods() {
    }

    /**
     * Installs all prototype methods.
     * <p>
     * 모든 프로토타입 메서드를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = realm.getArrayPrototype();
        installMutators(realm, proto);
        installBuilders(realm, proto);
        installSearch(realm, proto);
        installIteration(realm, proto);
        installModern(realm, proto);
        installReduce(realm, proto);
        installIterators(realm, proto);
    }

    /**
     * Installs ES2019 and ES2022 Array.prototype methods: flat, flatMap and at.
     * <p>
     * ES2019 와 ES2022 의 Array.prototype 메서드 flat, flatMap, at 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installModern(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "flat", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject source = toObject(r, thisValue);
                double depth = args.length == 0 ? 1 : Ops.toInteger(args[0]);
                return flatten(r, source, Math.max(depth, 0));
            }
        });
        Support.def(realm, proto, "flatMap", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject source = toObject(r, thisValue);
                double length = Ops.lengthOf(source);
                JsValue mapper = Js.arg(args, 0);
                Support.requireCallable(mapper, "callback");
                JsValue thisArg = Js.arg(args, 1);
                JsObject out = ArrayBuiltins.speciesCreate(r, source, 0);
                double target = 0;
                Set<JsObject> active = Collections.newSetFromMap(new IdentityHashMap<JsObject, Boolean>());
                // 원본 길이를 고정해 매핑하고 결과를 한 칸씩 평탄화한다.
                for (double k = 0; k < length; k++) {
                    Support.poll(k);
                    if (Support.hasAt(source, k)) {
                        JsValue mapped = Ops.call(mapper, thisArg, new JsValue[] { Support.getAt(source, k), Js.num(k), source });
                        target = flattenInto(r, out, target, mapped, 1, active, 0);
                    }
                }
                Support.setLength(out, target);
                return out;
            }
        });
        Support.def(realm, proto, "at", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double length = Ops.lengthOf(o);
                double relative = Ops.toInteger(Js.arg(args, 0));
                double index = relative < 0 ? length + relative : relative;
                if (index < 0 || index >= length) {
                    return Js.UNDEFINED;
                }
                return Support.getAt(o, index);
            }
        });
    }

    /**
     * Flattens the source array into a newly created array.
     * <p>
     * 원본 배열을 새 배열에 평탄화한다.
     *
     * @param realm the realm / realm
     * @param source the source object / 원본 객체
     * @param depth maximum nesting depth / 최대 중첩 깊이
     * @return the flattened array / 평탄화한 배열
     */
    private static JsValue flatten(JsRealm realm, JsObject source, double depth) {
        JsObject out = ArrayBuiltins.speciesCreate(realm, source, 0);
        double target = 0;
        double length = Ops.lengthOf(source);
        Set<JsObject> active = Collections.newSetFromMap(new IdentityHashMap<JsObject, Boolean>());
        // 원본에 실제로 있는 요소만 깊이에 따라 평탄화한다.
        for (double k = 0; k < length; k++) {
            Support.poll(k);
            if (Support.hasAt(source, k)) {
                target = flattenInto(realm, out, target, Support.getAt(source, k), depth, active, 0);
            }
        }
        Support.setLength(out, target);
        return out;
    }

    /**
     * Adds one value to a flatten destination, recursing only into actual arrays.
     * <p>
     * 실제 배열에만 재귀하여 평탄화 대상에 값 하나를 추가한다.
     *
     * @param realm the realm / realm
     * @param out destination / 대상
     * @param target next destination index / 다음 대상 인덱스
     * @param value value to add / 추가할 값
     * @param depth remaining depth / 남은 깊이
     * @param active arrays on the active recursion path / 현재 재귀 경로의 배열
     * @param nesting current nesting depth / 현재 중첩 깊이
     * @return next destination index / 다음 대상 인덱스
     */
    private static double flattenInto(JsRealm realm, JsObject out, double target, JsValue value, double depth,
            Set<JsObject> active, int nesting) {
        if (depth > 0 && Ops.isArray(value)) {
            JsObject nested = (JsObject) value;
            if (nesting >= 1024 || !active.add(nested)) {
                throw JsException.rangeError("Maximum call stack size exceeded while flattening an array");
            }
            double length = Ops.lengthOf(nested);
            try {
                // 중첩 배열의 있는 요소만 남은 깊이로 재귀 처리한다.
                for (double n = 0; n < length; n++) {
                    Support.poll(n);
                    if (Support.hasAt(nested, n)) {
                        target = flattenInto(realm, out, target, Support.getAt(nested, n), depth - 1, active, nesting + 1);
                    }
                }
            } finally {
                active.remove(nested);
            }
            return target;
        }
        if (target >= Support.MAX_SAFE_INTEGER) {
            throw JsException.typeError("Invalid array length");
        }
        Support.createAt(out, target, value);
        return target + 1;
    }

    /**
     * ToObject(this value) for Array.prototype methods.
     * <p>
     * Array.prototype 메서드를 위한 ToObject(this 값).
     *
     * @param realm     the realm / realm
     * @param thisValue the this value / this 값
     * @return the object / 객체
     */
    private static JsObject toObject(JsRealm realm, JsValue thisValue) {
        return Ops.toObject(realm, thisValue);
    }

    /**
     * Checks that a growth does not exceed 2<sup>53</sup>-1.
     * <p>
     * 늘어난 길이가 2<sup>53</sup>-1 을 넘지 않는지 확인한다.
     *
     * @param newLength the resulting length / 결과 길이
     */
    private static void checkLength(double newLength) {
        if (newLength > Support.MAX_SAFE_INTEGER) {
            throw JsException.typeError("Invalid array length");
        }
    }

    // ------------------------------------------------------------------ mutators

    /**
     * Installs the methods that change the array: push, pop, shift, unshift, splice, reverse, sort, fill, copyWithin.
     * <p>
     * 배열을 바꾸는 메서드를 설치한다: push, pop, shift, unshift, splice, reverse, sort, fill, copyWithin.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installMutators(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "push", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                checkLength(len + args.length);
                // 인수를 배열 끝에 차례로 덧붙인다.
                for (int i = 0; i < args.length; i++) {
                    Support.setAt(o, len + i, args[i]);
                }
                Support.setLength(o, len + args.length);
                return Js.num(len + args.length);
            }
        });
        Support.def(realm, proto, "pop", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    Support.setLength(o, 0);
                    return Js.UNDEFINED;
                }
                double index = len - 1;
                JsValue element = Support.getAt(o, index);
                Support.deleteAt(o, index);
                Support.setLength(o, index);
                return element;
            }
        });
        Support.def(realm, proto, "shift", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    Support.setLength(o, 0);
                    return Js.UNDEFINED;
                }
                JsValue first = Support.getAt(o, 0);
                // 모든 요소를 한 칸 앞으로 옮긴다. 구멍은 구멍인 채로 옮긴다.
                for (double k = 1; k < len; k++) {
                    if (Support.hasAt(o, k)) {
                        Support.setAt(o, k - 1, Support.getAt(o, k));
                    } else {
                        Support.deleteAt(o, k - 1);
                    }
                    Support.poll(k);
                }
                Support.deleteAt(o, len - 1);
                Support.setLength(o, len - 1);
                return first;
            }
        });
        Support.def(realm, proto, "unshift", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                int argCount = args.length;
                if (argCount > 0) {
                    checkLength(len + argCount);
                    // 기존 요소를 뒤에서부터 argCount 칸 뒤로 옮긴다.
                    for (double k = len; k > 0; k--) {
                        double from = k - 1;
                        double to = k + argCount - 1;
                        if (Support.hasAt(o, from)) {
                            Support.setAt(o, to, Support.getAt(o, from));
                        } else {
                            Support.deleteAt(o, to);
                        }
                        Support.poll(k);
                    }
                    // 새 요소를 앞쪽에 차례로 쓴다.
                    for (int j = 0; j < argCount; j++) {
                        Support.setAt(o, j, args[j]);
                    }
                }
                Support.setLength(o, len + argCount);
                return Js.num(len + argCount);
            }
        });
        Support.def(realm, proto, "splice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return splice(r, toObject(r, thisValue), args);
            }
        });
        Support.def(realm, proto, "reverse", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                double middle = Math.floor(len / 2);
                // 양 끝에서 가운데로 오며 대칭 위치의 요소를 서로 바꾼다. 구멍은 구멍인 채로 옮긴다.
                for (double lower = 0; lower != middle; lower++) {
                    double upper = len - lower - 1;
                    boolean lowerExists = Support.hasAt(o, lower);
                    JsValue lowerValue = lowerExists ? Support.getAt(o, lower) : null;
                    boolean upperExists = Support.hasAt(o, upper);
                    JsValue upperValue = upperExists ? Support.getAt(o, upper) : null;
                    if (lowerExists && upperExists) {
                        Support.setAt(o, lower, upperValue);
                        Support.setAt(o, upper, lowerValue);
                    } else if (upperExists) {
                        Support.setAt(o, lower, upperValue);
                        Support.deleteAt(o, upper);
                    } else if (lowerExists) {
                        Support.deleteAt(o, lower);
                        Support.setAt(o, upper, lowerValue);
                    }
                    Support.poll(lower);
                }
                return o;
            }
        });
        Support.def(realm, proto, "sort", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue compareFn = Js.arg(args, 0);
                if (!compareFn.isUndefined() && !compareFn.isCallable()) {
                    throw JsException.typeError("The comparison function must be either a function or undefined");
                }
                return sort(toObject(r, thisValue), compareFn);
            }
        });
        Support.def(realm, proto, "fill", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                double k = Support.relative(Js.arg(args, 1), len, 0);
                double end = Support.relative(Js.arg(args, 2), len, len);
                JsValue value = Js.arg(args, 0);
                // 시작부터 끝 직전까지 같은 값을 채운다.
                for (; k < end; k++) {
                    Support.setAt(o, k, value);
                    Support.poll(k);
                }
                return o;
            }
        });
        Support.def(realm, proto, "copyWithin", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                double to = Support.relative(Js.arg(args, 0), len, 0);
                double from = Support.relative(Js.arg(args, 1), len, 0);
                double fin = Support.relative(Js.arg(args, 2), len, len);
                double count = Math.min(fin - from, len - to);
                int direction = 1;
                if (from < to && to < from + count) {
                    direction = -1;
                    from = from + count - 1;
                    to = to + count - 1;
                }
                // 겹치는 구간에서 덮어쓰지 않도록 방향을 정해 count 개를 복사한다.
                for (; count > 0; count--) {
                    if (Support.hasAt(o, from)) {
                        Support.setAt(o, to, Support.getAt(o, from));
                    } else {
                        Support.deleteAt(o, to);
                    }
                    from += direction;
                    to += direction;
                    Support.poll(count);
                }
                return o;
            }
        });
    }

    /**
     * Array.prototype.splice (22.1.3.25).
     * <p>
     * Array.prototype.splice(22.1.3.25).
     *
     * @param realm the realm / realm
     * @param o     the object / 객체
     * @param args  start, deleteCount, items... / start, deleteCount, items...
     * @return the array of removed elements / 제거된 요소의 배열
     */
    private static JsValue splice(JsRealm realm, JsObject o, JsValue[] args) {
        double len = Ops.lengthOf(o);
        double start = Support.relative(Js.arg(args, 0), len, 0);
        int itemCount = Math.max(args.length - 2, 0);
        double deleteCount;
        if (args.length == 0) {
            deleteCount = 0;
        } else if (args.length == 1) {
            deleteCount = len - start;
        } else {
            deleteCount = Math.min(Math.max(Ops.toInteger(args[1]), 0), len - start);
        }
        checkLength(len + itemCount - deleteCount);
        JsObject a = ArrayBuiltins.speciesCreate(realm, o, deleteCount);
        // 지워질 요소를 새 배열로 복사한다.
        for (double k = 0; k < deleteCount; k++) {
            double from = start + k;
            if (Support.hasAt(o, from)) {
                Support.createAt(a, k, Support.getAt(o, from));
            }
            Support.poll(k);
        }
        Support.setLength(a, deleteCount);
        if (itemCount < deleteCount) {
            // 지운 칸보다 적게 넣으면 뒤쪽 요소를 앞으로 당긴다.
            for (double k = start; k < len - deleteCount; k++) {
                double from = k + deleteCount;
                double to = k + itemCount;
                if (Support.hasAt(o, from)) {
                    Support.setAt(o, to, Support.getAt(o, from));
                } else {
                    Support.deleteAt(o, to);
                }
                Support.poll(k);
            }
            // 남는 뒤쪽 칸을 지운다.
            for (double k = len; k > len - deleteCount + itemCount; k--) {
                Support.deleteAt(o, k - 1);
                Support.poll(k);
            }
        } else if (itemCount > deleteCount) {
            // 지운 칸보다 많이 넣으면 뒤쪽 요소를 뒤로 민다.
            for (double k = len - deleteCount; k > start; k--) {
                double from = k + deleteCount - 1;
                double to = k + itemCount - 1;
                if (Support.hasAt(o, from)) {
                    Support.setAt(o, to, Support.getAt(o, from));
                } else {
                    Support.deleteAt(o, to);
                }
                Support.poll(k);
            }
        }
        // 새 요소를 start 위치부터 채운다.
        for (int i = 0; i < itemCount; i++) {
            Support.setAt(o, start + i, args[i + 2]);
        }
        Support.setLength(o, len - deleteCount + itemCount);
        return a;
    }

    /**
     * Array.prototype.sort (22.1.3.24): present elements are sorted (undefined last), holes are moved to the end and deleted.
     * <p>
     * Array.prototype.sort(22.1.3.24): 있는 요소를 정렬하고(undefined 는 맨 뒤), 구멍은 끝으로 모아 지운다.
     *
     * @param o         the object / 객체
     * @param compareFn comparison function or undefined / 비교 함수 또는 undefined
     * @return the object / 객체
     */
    private static JsValue sort(JsObject o, JsValue compareFn) {
        double len = Ops.lengthOf(o);
        List<JsValue> defined = new ArrayList<JsValue>();
        int undefinedCount = 0;
        // 있는 요소를 모으며 undefined 는 개수만 센다.
        for (double k = 0; k < len; k++) {
            if (Support.hasAt(o, k)) {
                JsValue v = Support.getAt(o, k);
                if (v.isUndefined()) {
                    undefinedCount++;
                } else {
                    defined.add(v);
                }
            }
            Support.poll(k);
        }
        JsValue[] values = defined.toArray(new JsValue[defined.size()]);
        ArraySorter.sort(values, compareFn);
        double index = 0;
        // 정렬된 값, undefined, 그리고 남은 구멍 순으로 다시 쓴다.
        for (int i = 0; i < values.length; i++) {
            Support.setAt(o, index++, values[i]);
        }
        // 정렬된 값 뒤에 undefined 를 개수만큼 쓴다.
        for (int i = 0; i < undefinedCount; i++) {
            Support.setAt(o, index++, Js.UNDEFINED);
        }
        // 남은 칸 중 실제로 있는 요소를 지워 구멍으로 만든다.
        for (; index < len; index++) {
            if (Support.hasAt(o, index)) {
                Support.deleteAt(o, index);
            }
            Support.poll(index);
        }
        return o;
    }

    // ------------------------------------------------------------------ builders

    /**
     * Installs concat, slice, join, toString and toLocaleString.
     * <p>
     * concat, slice, join, toString, toLocaleString 을 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installBuilders(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "concat", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return concat(r, toObject(r, thisValue), args);
            }
        });
        Support.def(realm, proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                double k = Support.relative(Js.arg(args, 0), len, 0);
                double fin = Support.relative(Js.arg(args, 1), len, len);
                double count = Math.max(fin - k, 0);
                JsObject a = ArrayBuiltins.speciesCreate(r, o, count);
                double n = 0;
                // 범위의 있는 요소만 새 배열로 복사한다. 구멍은 건너뛴다.
                for (; k < fin; k++) {
                    if (Support.hasAt(o, k)) {
                        Support.createAt(a, n, Support.getAt(o, k));
                    }
                    n++;
                    Support.poll(k);
                }
                Support.setLength(a, n);
                return a;
            }
        });
        Support.def(realm, proto, "join", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                JsValue sepArg = Js.arg(args, 0);
                String sep = sepArg.isUndefined() ? "," : Ops.toString(sepArg);
                return Js.str(join(r, o, sep, false));
            }
        });
        Support.def(realm, proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                JsValue func = o.get("join");
                if (func.isCallable()) {
                    return Ops.call(func, o, Js.NO_ARGS);
                }
                JsValue objToString = r.getObjectPrototype().get("toString");
                if (objToString.isCallable()) {
                    return Ops.call(objToString, o, Js.NO_ARGS);
                }
                return Js.str("[object " + o.className() + "]");
            }
        });
        Support.def(realm, proto, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(join(r, toObject(r, thisValue), ",", true));
            }
        });
    }

    /**
     * IsConcatSpreadable (22.1.3.1.1).
     * <p>
     * IsConcatSpreadable(22.1.3.1.1).
     *
     * @param e the value / 값
     * @return true if concat spreads its elements / concat 이 요소를 펼치면 true
     */
    private static boolean isConcatSpreadable(JsValue e) {
        if (!(e instanceof JsObject)) {
            return false;
        }
        JsValue spreadable = ((JsObject) e).get(WellKnownSymbols.IS_CONCAT_SPREADABLE);
        if (!spreadable.isUndefined()) {
            return Ops.toBoolean(spreadable);
        }
        return Ops.isArray(e);
    }

    /**
     * Array.prototype.concat (22.1.3.1).
     * <p>
     * Array.prototype.concat(22.1.3.1).
     *
     * @param realm the realm / realm
     * @param o     the object / 객체
     * @param args  the items to append / 덧붙일 항목
     * @return the new array / 새 배열
     */
    private static JsValue concat(JsRealm realm, JsObject o, JsValue[] args) {
        JsObject a = ArrayBuiltins.speciesCreate(realm, o, 0);
        double n = 0;
        // 대상 자신과 인수를 차례로 처리한다.
        for (int i = -1; i < args.length; i++) {
            JsValue e = i < 0 ? o : args[i];
            if (isConcatSpreadable(e)) {
                JsObject eo = (JsObject) e;
                double len = Ops.lengthOf(eo);
                if (n + len > Support.MAX_SAFE_INTEGER) {
                    throw JsException.typeError("Invalid array length");
                }
                // 펼칠 배열의 요소를 차례로 복사한다. 구멍은 건너뛰되 위치는 센다.
                for (double k = 0; k < len; k++, n++) {
                    if (Support.hasAt(eo, k)) {
                        Support.createAt(a, n, Support.getAt(eo, k));
                    }
                    Support.poll(k);
                }
            } else {
                if (n >= Support.MAX_SAFE_INTEGER) {
                    throw JsException.typeError("Invalid array length");
                }
                Support.createAt(a, n, e);
                n++;
            }
        }
        Support.setLength(a, n);
        return a;
    }

    /**
     * Joins the elements of an object into a string; an object that is already being joined (a cycle) contributes the empty string.
     * <p>
     * 객체의 요소를 문자열로 잇는다. 이미 join 중인 객체(순환)는 빈 문자열이 된다.
     *
     * @param realm  the realm / realm
     * @param o      the object / 객체
     * @param sep    the separator / 구분자
     * @param locale true for toLocaleString (elements are converted with their toLocaleString) / toLocaleString 이면 true
     * @return the joined string / 이은 문자열
     */
    private static String join(JsRealm realm, JsObject o, String sep, boolean locale) {
        Set<JsObject> active = JOINING.get();
        if (!active.add(o)) {
            return "";
        }
        try {
            double len = Ops.lengthOf(o);
            StringBuilder sb = new StringBuilder();
            // 요소를 0 부터 차례로 문자열로 바꿔 구분자와 함께 잇는다.
            for (double k = 0; k < len; k++) {
                if (k > 0) {
                    sb.append(sep);
                }
                JsValue e = Support.getAt(o, k);
                if (!e.isNullOrUndefined()) {
                    if (locale) {
                        sb.append(Ops.toString(Ops.invoke(realm, e, "toLocaleString", Js.NO_ARGS)));
                    } else {
                        sb.append(Ops.toString(e));
                    }
                }
                Support.checkStringLength(sb.length());
                Support.poll(k);
            }
            return sb.toString();
        } finally {
            active.remove(o);
        }
    }

    // ------------------------------------------------------------------ searching

    /**
     * Installs indexOf, lastIndexOf, includes (extension), find and findIndex.
     * <p>
     * indexOf, lastIndexOf, includes(확장), find, findIndex 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installSearch(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "indexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    return Js.num(-1);
                }
                double n = Ops.toInteger(Js.arg(args, 1));
                if (n >= len) {
                    return Js.num(-1);
                }
                double k = n >= 0 ? n : Math.max(len + n, 0);
                JsValue search = Js.arg(args, 0);
                // 앞에서부터 있는 요소를 엄격 동등으로 비교한다.
                for (; k < len; k++) {
                    if (Support.hasAt(o, k) && Ops.strictEquals(search, Support.getAt(o, k))) {
                        return Js.num(k);
                    }
                    Support.poll(k);
                }
                return Js.num(-1);
            }
        });
        Support.def(realm, proto, "lastIndexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    return Js.num(-1);
                }
                double n = args.length > 1 ? Ops.toInteger(args[1]) : len - 1;
                double k = n >= 0 ? Math.min(n, len - 1) : len + n;
                JsValue search = Js.arg(args, 0);
                // 뒤에서부터 있는 요소를 엄격 동등으로 비교한다.
                for (; k >= 0; k--) {
                    if (Support.hasAt(o, k) && Ops.strictEquals(search, Support.getAt(o, k))) {
                        return Js.num(k);
                    }
                    Support.poll(k);
                }
                return Js.num(-1);
            }
        });
        Support.def(realm, proto, "includes", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = toObject(r, thisValue);
                double len = Ops.lengthOf(o);
                if (len == 0) {
                    return Js.FALSE;
                }
                double n = Ops.toInteger(Js.arg(args, 1));
                double k = n >= 0 ? n : Math.max(len + n, 0);
                JsValue search = Js.arg(args, 0);
                // 구멍도 undefined 로 읽으며 SameValueZero 로 비교한다.
                for (; k < len; k++) {
                    if (Ops.sameValueZero(search, Support.getAt(o, k))) {
                        return Js.TRUE;
                    }
                    Support.poll(k);
                }
                return Js.FALSE;
            }
        });
        Support.def(realm, proto, "find", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(r, thisValue, args, false);
            }
        });
        Support.def(realm, proto, "findIndex", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return find(r, thisValue, args, true);
            }
        });
    }

    /**
     * find and findIndex (22.1.3.8, 22.1.3.9): every index is visited, holes read as undefined.
     * <p>
     * find, findIndex(22.1.3.8, 22.1.3.9): 모든 인덱스를 방문하며 구멍은 undefined 로 읽는다.
     *
     * @param realm     the realm / realm
     * @param thisValue the this value / this 값
     * @param args      predicate, thisArg / predicate, thisArg
     * @param index     true for findIndex / findIndex 이면 true
     * @return the element or index / 요소 또는 인덱스
     */
    private static JsValue find(JsRealm realm, JsValue thisValue, JsValue[] args, boolean index) {
        JsObject o = toObject(realm, thisValue);
        double len = Ops.lengthOf(o);
        JsValue predicate = Js.arg(args, 0);
        Support.requireCallable(predicate, "predicate");
        JsValue t = Js.arg(args, 1);
        // 모든 인덱스에서 술어를 호출해 처음 참인 요소를 찾는다.
        for (double k = 0; k < len; k++) {
            JsValue v = Support.getAt(o, k);
            if (Ops.toBoolean(Ops.call(predicate, t, new JsValue[] { v, Js.num(k), o }))) {
                return index ? Js.num(k) : v;
            }
            Support.poll(k);
        }
        return index ? Js.num(-1) : Js.UNDEFINED;
    }

    // ------------------------------------------------------------------ iteration with callbacks

    /**
     * Installs forEach, every, some, map and filter.
     * <p>
     * forEach, every, some, map, filter 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installIteration(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "forEach", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterate(r, thisValue, args, 0);
            }
        });
        Support.def(realm, proto, "every", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterate(r, thisValue, args, 1);
            }
        });
        Support.def(realm, proto, "some", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterate(r, thisValue, args, 2);
            }
        });
        Support.def(realm, proto, "map", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterate(r, thisValue, args, 3);
            }
        });
        Support.def(realm, proto, "filter", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return iterate(r, thisValue, args, 4);
            }
        });
    }

    /**
     * The shared loop of forEach (0), every (1), some (2), map (3) and filter (4): the callback is called for every present element only.
     * <p>
     * forEach(0), every(1), some(2), map(3), filter(4) 의 공통 반복: 콜백은 있는 요소에만 호출한다.
     *
     * @param realm     the realm / realm
     * @param thisValue the this value / this 값
     * @param args      callbackfn, thisArg / callbackfn, thisArg
     * @param mode      which method / 어느 메서드인지
     * @return the method's result / 메서드의 결과
     */
    private static JsValue iterate(JsRealm realm, JsValue thisValue, JsValue[] args, int mode) {
        JsObject o = toObject(realm, thisValue);
        double len = Ops.lengthOf(o);
        JsValue callback = Js.arg(args, 0);
        Support.requireCallable(callback, "callback");
        JsValue t = Js.arg(args, 1);
        JsObject a = null;
        if (mode == 3) {
            a = ArrayBuiltins.speciesCreate(realm, o, len);
        } else if (mode == 4) {
            a = ArrayBuiltins.speciesCreate(realm, o, 0);
        }
        double to = 0;
        // 있는 요소마다 콜백(요소, 인덱스, 대상)을 호출하고 종류에 맞게 결과를 처리한다.
        for (double k = 0; k < len; k++) {
            Support.poll(k);
            if (!Support.hasAt(o, k)) {
                continue;
            }
            JsValue v = Support.getAt(o, k);
            JsValue result = Ops.call(callback, t, new JsValue[] { v, Js.num(k), o });
            if (mode == 1 && !Ops.toBoolean(result)) {
                return Js.FALSE;
            }
            if (mode == 2 && Ops.toBoolean(result)) {
                return Js.TRUE;
            }
            if (mode == 3) {
                Support.createAt(a, k, result);
            }
            if (mode == 4 && Ops.toBoolean(result)) {
                Support.createAt(a, to++, v);
            }
            Support.poll(k);
        }
        if (mode == 1) {
            return Js.TRUE;
        }
        if (mode == 2) {
            return Js.FALSE;
        }
        return mode >= 3 ? a : Js.UNDEFINED;
    }

    // ------------------------------------------------------------------ reduce

    /**
     * Installs reduce and reduceRight.
     * <p>
     * reduce 와 reduceRight 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installReduce(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "reduce", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return reduce(r, thisValue, args, false);
            }
        });
        Support.def(realm, proto, "reduceRight", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return reduce(r, thisValue, args, true);
            }
        });
    }

    /**
     * reduce and reduceRight (22.1.3.18, 22.1.3.19).
     * <p>
     * reduce, reduceRight(22.1.3.18, 22.1.3.19).
     *
     * @param realm     the realm / realm
     * @param thisValue the this value / this 값
     * @param args      callbackfn, initialValue / callbackfn, initialValue
     * @param right     true for reduceRight / reduceRight 이면 true
     * @return the accumulated value / 누적 값
     */
    private static JsValue reduce(JsRealm realm, JsValue thisValue, JsValue[] args, boolean right) {
        JsObject o = toObject(realm, thisValue);
        double len = Ops.lengthOf(o);
        JsValue callback = Js.arg(args, 0);
        Support.requireCallable(callback, "callback");
        if (len == 0 && args.length < 2) {
            throw JsException.typeError("Reduce of empty array with no initial value");
        }
        double k = right ? len - 1 : 0;
        double step = right ? -1 : 1;
        JsValue acc = null;
        if (args.length >= 2) {
            acc = args[1];
        } else {
            boolean present = false;
            // 초기값이 없으면 처음 나오는 있는 요소를 누적 값으로 쓴다.
            while (!present && (right ? k >= 0 : k < len)) {
                present = Support.hasAt(o, k);
                if (present) {
                    acc = Support.getAt(o, k);
                }
                k += step;
            }
            if (!present) {
                throw JsException.typeError("Reduce of empty array with no initial value");
            }
        }
        // 남은 있는 요소마다 콜백(누적, 요소, 인덱스, 대상)을 호출해 누적 값을 갱신한다.
        for (; right ? k >= 0 : k < len; k += step) {
            if (Support.hasAt(o, k)) {
                acc = Ops.call(callback, Js.UNDEFINED, new JsValue[] { acc, Support.getAt(o, k), Js.num(k), o });
            }
            Support.poll(k);
        }
        return acc;
    }

    // ------------------------------------------------------------------ iterators

    /**
     * Installs entries, keys, values, [Symbol.iterator] (the same function object as values) and [Symbol.unscopables].
     * <p>
     * entries, keys, values, [Symbol.iterator](values 와 같은 함수 객체), [Symbol.unscopables] 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto Array.prototype / Array.prototype
     */
    private static void installIterators(JsRealm realm, JsObject proto) {
        ArrayIteratorObject.install(realm);
        Support.def(realm, proto, "entries", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ArrayIteratorObject.create(r, toObject(r, thisValue), ArrayIteratorObject.ENTRIES);
            }
        });
        Support.def(realm, proto, "keys", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ArrayIteratorObject.create(r, toObject(r, thisValue), ArrayIteratorObject.KEYS);
            }
        });
        NativeFunction values = Support.def(realm, proto, "values", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return ArrayIteratorObject.create(r, toObject(r, thisValue), ArrayIteratorObject.VALUES);
            }
        });
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, values);
        realm.setIntrinsic("%ArrayProto_values%", values);
        JsObject unscopables = new JsObject(null);
        String[] names = { "copyWithin", "entries", "fill", "find", "findIndex", "keys", "values" };
        // ES2015 가 정한 일곱 이름을 true 로 표시한다.
        for (int i = 0; i < names.length; i++) {
            unscopables.createDataProperty(names[i], Js.TRUE);
        }
        proto.defineOwnProperty(WellKnownSymbols.UNSCOPABLES, PropertyDescriptor.data(unscopables, false, false, true));
    }
}
