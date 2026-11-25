package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Implements the atomic operations of ECMA-262 §25.4 on integer typed arrays.
 * <p>
 * 정수 형식화 배열에 대한 ECMA-262 §25.4의 원자 연산을 구현한다.
 */
final class AtomicsLib {

    /**
     * The read-modify-write operations and their built-in method names.
     * <p>
     * 읽기-수정-쓰기 연산과 해당 내장 메서드 이름.
     */
    private enum Operation {
        /** Wraparound addition. / 넘친 비트를 버리는 덧셈. */
        ADD("add"),
        /** Wraparound subtraction. / 넘친 비트를 버리는 뺄셈. */
        SUB("sub"),
        /** Bitwise conjunction. / 비트 논리곱. */
        AND("and"),
        /** Bitwise disjunction. / 비트 논리합. */
        OR("or"),
        /** Bitwise exclusive disjunction. / 비트 배타적 논리합. */
        XOR("xor"),
        /** Unconditional replacement. / 조건 없는 교체. */
        EXCHANGE("exchange");

        /** The method name on Atomics. / Atomics에 설치할 메서드 이름. */
        final String methodName;

        /**
         * Stores the associated method name.
         * <p>
         * 대응하는 메서드 이름을 보관한다.
         *
         * @param methodName method name / 메서드 이름
         */
        Operation(String methodName) {
            this.methodName = methodName;
        }

        /**
         * Applies the operation to two integer bit patterns.
         * <p>
         * 두 정수 비트 패턴에 연산을 적용한다.
         *
         * @param previous stored bits / 저장된 비트
         * @param operand input bits / 입력 비트
         * @return replacement bits before width truncation / 너비에 맞춰 자르기 전의 교체 비트
         */
        long apply(long previous, long operand) {
            switch (this) {
            case ADD:
                return previous + operand;
            case SUB:
                return previous - operand;
            case AND:
                return previous & operand;
            case OR:
                return previous | operand;
            case XOR:
                return previous ^ operand;
            case EXCHANGE:
                return operand;
            default:
                throw new AssertionError(this);
            }
        }
    }

    /** Only static methods are used. / 정적 메서드만 사용한다. */
    private AtomicsLib() {
    }

    /**
     * Installs the Atomics namespace object into the given realm.
     * <p>
     * 주어진 realm에 Atomics 네임스페이스 객체를 설치한다.
     *
     * @param realm the realm that receives the built-in / 내장 객체를 설치할 realm
     */
    static void install(JsRealm realm) {
        JsObject atomics = new JsObject(realm.getObjectPrototype());
        atomics.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("Atomics"), false, false, true);
        realm.setIntrinsic("%Atomics%", atomics);
        realm.getGlobalObject().defineBuiltin("Atomics", atomics);
        realm.method(atomics, "isLockFree", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                // 4바이트는 표준 필수 값이다. 그 밖의 크기는 JRE가 내부 잠금 사용 여부를 공개하지 않는다.
                return Js.bool(Ops.toInteger(Js.arg(args, 0)) == 4);
            }
        });
        realm.method(atomics, "load", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject array = validateIntegerArray(Js.arg(args, 0), false);
                int offset = accessOffset(array, Js.arg(args, 1));
                revalidate(array, offset);
                return array.getBuffer().readValue(array.getType(), offset, true);
            }
        });
        realm.method(atomics, "store", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject array = validateIntegerArray(Js.arg(args, 0), false);
                int offset = accessOffset(array, Js.arg(args, 1));
                JsValue value = coerce(array.getType(), Js.arg(args, 2));
                revalidate(array, offset);
                array.getBuffer().writeValue(array.getType(), offset, value, true);
                return value;
            }
        });
        realm.method(atomics, "compareExchange", 4, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                TypedArrayObject array = validateIntegerArray(Js.arg(args, 0), false);
                int offset = accessOffset(array, Js.arg(args, 1));
                ElementType type = array.getType();
                JsValue expected = coerce(type, Js.arg(args, 2));
                JsValue replacement = coerce(type, Js.arg(args, 3));
                revalidate(array, offset);
                return type.valueFromBits(compareExchange(array.getBuffer(), offset, type.getSize(),
                        type.bitsFromValue(expected), type.bitsFromValue(replacement)));
            }
        });
        // 길이와 변환 순서가 같은 읽기-수정-쓰기 메서드를 이름별로 설치한다.
        for (final Operation operation : Operation.values()) {
            realm.method(atomics, operation.methodName, 3, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                    TypedArrayObject array = validateIntegerArray(Js.arg(args, 0), false);
                    int offset = accessOffset(array, Js.arg(args, 1));
                    ElementType type = array.getType();
                    JsValue value = coerce(type, Js.arg(args, 2));
                    revalidate(array, offset);
                    return type.valueFromBits(modify(array.getBuffer(), offset, type.getSize(), type.bitsFromValue(value), operation));
                }
            });
        }
        AtomicsWait.install(realm, atomics);
    }

    /**
     * Checks the typed array brand, bounds and element category.
     * <p>
     * 형식화 배열인지, 범위가 유효한지, 요소 형식이 허용되는지 검사한다.
     *
     * @param value typed array argument / 형식화 배열 인수
     * @param waitable whether only waitable types are allowed / 대기 가능한 배열만 허용할지 여부
     * @return the valid typed array / 검사한 형식화 배열
     */
    static TypedArrayObject validateIntegerArray(JsValue value, boolean waitable) {
        TypedArrayObject array = TypedSupport.validate(value);
        ElementType type = array.getType();
        if (waitable) {
            if (type != ElementType.INT32 && type != ElementType.BIGINT64) {
                throw JsException.typeError("Atomics wait requires an Int32Array or BigInt64Array");
            }
        } else {
            switch (type) {
            case UINT8_CLAMPED:
            case FLOAT16:
            case FLOAT32:
            case FLOAT64:
                throw JsException.typeError("Atomics operations require an unclamped integer typed array");
            default:
                break;
            }
        }
        return array;
    }

    /**
     * Converts the index against the length captured before observable coercion.
     * <p>
     * 관찰 가능한 변환을 수행하기 전에 읽어 둔 길이를 기준으로 인덱스를 변환한다.
     *
     * @param array a validated integer typed array / 검증한 정수 형식화 배열
     * @param index index argument / 인덱스 인수
     * @return byte offset in the underlying buffer / 버퍼 내 바이트 위치
     */
    static int accessOffset(TypedArrayObject array, JsValue index) {
        int length = array.getLength();
        double requested = TypedSupport.toIndex(index, "atomic access index");
        if (requested >= length) {
            throw JsException.rangeError("Atomic access index is out of bounds");
        }
        return array.getRawByteOffset() + (int) requested * array.getType().getSize();
    }

    /**
     * Checks the view and byte range again after observable operand conversions.
     * <p>
     * 관찰 가능한 피연산자 변환 뒤 뷰와 바이트 범위를 다시 검사한다.
     *
     * @param array the typed array / 형식화 배열
     * @param offset byte offset to access / 접근할 바이트 위치
     */
    private static void revalidate(TypedArrayObject array, int offset) {
        TypedSupport.validate(array);
        if (offset > array.getBuffer().getByteLength() - array.getType().getSize()) {
            throw JsException.rangeError("Atomic access byte range is out of bounds");
        }
    }

    /**
     * Obtains the numeric operand before applying any storage-width reduction.
     * <p>
     * 저장 너비에 맞춰 값을 줄이기 전에 숫자 피연산자를 얻는다.
     *
     * @param type element type / 요소 형식
     * @param value JavaScript argument / JavaScript 인수
     * @return BigInt or integer Number with a canonical positive zero / BigInt 또는 양의 0으로 통일한 정수 Number
     */
    private static JsValue coerce(ElementType type, JsValue value) {
        if (type.isBigInt()) {
            return Js.bigint(Ops.toBigInt(value));
        }
        double integer = Ops.toInteger(value);
        return Js.num(integer == 0 ? 0 : integer);
    }

    /**
     * Reads and conditionally replaces the bits at an integer element.
     * <p>
     * 정수 요소의 비트를 읽고 조건에 따라 교체한다.
     *
     * @param buffer buffer with the element / 요소의 버퍼
     * @param offset byte offset / 바이트 위치
     * @param width element width / 요소 너비
     * @param expected comparison bits / 비교할 비트
     * @param value replacement bits / 교체 비트
     * @return previous element bits / 이전 요소 비트
     */
    private static long compareExchange(ArrayBufferObject buffer, int offset, int width, long expected, long value) {
        SharedByteBlock shared = buffer.sharedBlock();
        if (shared != null) {
            return shared.compareExchange(offset, width, expected, value);
        }
        byte[] data = buffer.getData();
        long previous = ElementType.readBits(data, offset, width, true);
        // 비교 전에 피연산자에서 요소 너비를 넘는 비트를 제거한다.
        if (previous == (expected & SharedByteBlock.mask(width))) {
            ElementType.writeBits(data, offset, width, value, true);
        }
        return previous;
    }

    /**
     * Applies a read-modify-write operation, retrying shared writes that lost a race.
     * <p>
     * 읽기-수정-쓰기를 수행한다. 경합으로 공유 쓰기에 실패한 경우 다시 시도한다.
     *
     * @param buffer buffer with the element / 요소의 버퍼
     * @param offset byte offset / 바이트 위치
     * @param width element width / 요소 너비
     * @param value operand bits / 피연산자 비트
     * @param operation operation to apply / 적용할 연산
     * @return previous element bits / 이전 요소 비트
     */
    private static long modify(ArrayBufferObject buffer, int offset, int width, long value, Operation operation) {
        SharedByteBlock shared = buffer.sharedBlock();
        if (shared == null) {
            byte[] data = buffer.getData();
            long previous = ElementType.readBits(data, offset, width, true);
            ElementType.writeBits(data, offset, width, operation.apply(previous, value), true);
            return previous;
        }
        long previous;
        // 다른 에이전트가 요소를 바꾸면 새 값에 연산을 적용해 다시 시도한다.
        do {
            previous = shared.load(offset, width, true);
        } while (shared.compareExchange(offset, width, previous, operation.apply(previous, value)) != previous);
        return previous;
    }
}
