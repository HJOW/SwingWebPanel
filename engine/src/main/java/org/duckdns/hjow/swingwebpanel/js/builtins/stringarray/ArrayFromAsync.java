package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.AsyncIteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * The suspended state of Array.fromAsync, which reads and stores one value before proceeding to the next.
 * <p>
 * 값을 하나 읽고 저장한 뒤 다음 값으로 진행하는 Array.fromAsync의 일시 중단 상태.
 */
final class ArrayFromAsync {

    /** The realm that owns the operation. / 연산을 소유한 realm. */
    private final JsRealm realm;

    /** The intrinsic result Promise capability. / 결과 내장 Promise의 capability. */
    private final PromiseCapability result;

    /** Mapper or undefined. / 매퍼 또는 undefined. */
    private final JsValue mapper;

    /** The receiver used for mapper calls. / 매퍼 호출에 쓰는 수신자. */
    private final JsValue thisArg;

    /** Async iterator or async-from-sync wrapper if present. / 비동기 이터레이터 또는 동기 이터레이터의 비동기 래퍼. */
    private JsObject iterator;

    /** The iterator's next method read at initialization. / 초기화 때 읽은 이터레이터의 next 메서드. */
    private JsValue nextMethod;

    /** Source for the array-like path. / 유사 배열 경로의 원본. */
    private JsObject arrayLike;

    /** Snapshot of the array-like length. / 미리 읽은 유사 배열의 길이. */
    private double length;

    /** Destination array or constructor result. / 결과 배열 또는 생성자 결과. */
    private JsObject output;

    /** Index of the next element to store. / 다음에 저장할 요소의 인덱스. */
    private double index;

    /** Processes the awaited next result. / 대기한 next 결과를 처리한다. */
    private final NativeFunction onNext;

    /** Processes the awaited array-like value. / 대기한 유사 배열 값을 처리한다. */
    private final NativeFunction onValue;

    /** Processes the awaited mapper result. / 대기한 매퍼 결과를 처리한다. */
    private final NativeFunction onMapped;

    /** Rejects the operation for an input failure. / 입력 오류로 연산을 거부한다. */
    private final NativeFunction onInputError;

    /** Closes an open iterator after a mapper failure. / 매퍼 오류가 나면 열린 이터레이터를 닫는다. */
    private final NativeFunction onMapperError;

    /**
     * Starts consumption and returns the promise even when setup fails.
     * <p>
     * 소비를 시작하고 준비 중 오류가 나도 Promise를 반환한다.
     *
     * @param realm the realm / realm
     * @param constructor the value of this / this 값
     * @param args input, mapper and mapper receiver / 입력, 매퍼와 매퍼 수신자
     * @return Promise for the result / 결과 Promise
     */
    static JsObject start(JsRealm realm, JsValue constructor, JsValue[] args) {
        ArrayFromAsync operation = new ArrayFromAsync(realm, Js.arg(args, 1), Js.arg(args, 2));
        try {
            operation.initialize(constructor, Js.arg(args, 0));
            operation.advance();
        } catch (JsException e) {
            operation.result.reject(e.getValue(realm));
        }
        return operation.result.getPromise();
    }

    /**
     * Creates the result capability and the reactions reused for each element.
     * <p>
     * 결과 capability와 각 요소에서 재사용하는 반응을 만든다.
     *
     * @param realm the realm / realm
     * @param mapper mapper or undefined / 매퍼 또는 undefined
     * @param thisArg mapper receiver / 매퍼 수신자
     */
    private ArrayFromAsync(JsRealm realm, JsValue mapper, JsValue thisArg) {
        this.realm = realm;
        this.result = PromiseInterop.newCapability(realm);
        this.mapper = mapper;
        this.thisArg = thisArg;
        onNext = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                readNext(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        onValue = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                map(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        onMapped = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                store(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        onInputError = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                result.reject(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        onMapperError = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                closeAndReject(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
    }

    /**
     * Selects the input path and creates the destination before the first read.
     * <p>
     * 입력 경로를 선택하고 첫 값을 읽기 전에 결과 객체를 만든다.
     *
     * @param constructor the value of this / this 값
     * @param items input values / 입력 값
     */
    private void initialize(JsValue constructor, JsValue items) {
        if (!mapper.isUndefined()) {
            Support.requireCallable(mapper, "mapper");
        }
        iterator = AsyncIteratorHelper.getAsyncIteratorOrNull(realm, items);
        if (iterator != null) {
            nextMethod = iterator.get("next");
            output = Ops.isConstructor(constructor) ? Ops.construct(constructor, Js.NO_ARGS) : Support.arrayCreate(realm, 0);
        } else {
            arrayLike = Ops.toObject(realm, items);
            length = Ops.lengthOf(arrayLike);
            output = Ops.isConstructor(constructor) ? Ops.construct(constructor, new JsValue[] { Js.num(length) }) : Support.arrayCreate(realm, length);
        }
    }

    /**
     * Requests the next result or awaits the current array-like value.
     * <p>
     * 다음 반복 결과를 요청하거나 현재 유사 배열 값을 기다린다.
     */
    private void advance() {
        try {
            if (iterator != null) {
                if (index >= Support.MAX_SAFE_INTEGER) {
                    closeAndReject(JsException.typeError("Array.fromAsync index exceeds the maximum safe integer").getValue(realm));
                    return;
                }
                PromiseInterop.await(realm, Ops.call(nextMethod, iterator, Js.NO_ARGS), onNext, onInputError);
            } else if (index < length) {
                PromiseInterop.await(realm, Support.getAt(arrayLike, index), onValue, onInputError);
            } else {
                finish();
            }
        } catch (JsException e) {
            result.reject(e.getValue(realm));
        }
    }

    /**
     * Reads the next iterator result after awaiting it.
     * <p>
     * 기다린 다음 이터레이터 결과를 읽는다.
     *
     * @param value the iterator result / 이터레이터 결과
     */
    private void readNext(JsValue value) {
        try {
            if (!(value instanceof JsObject)) {
                throw JsException.typeError("Iterator result is not an object");
            }
            JsObject next = (JsObject) value;
            if (IteratorHelper.isDone(next)) {
                finish();
            } else {
                // 비동기 이터레이터에서 받은 value는 매핑하기 전 별도로 대기하지 않는다.
                map(IteratorHelper.value(next));
            }
        } catch (JsException e) {
            result.reject(e.getValue(realm));
        }
    }

    /**
     * Applies and awaits the mapper, or stores the input as it arrived.
     * <p>
     * 매퍼를 호출해 그 결과를 기다리거나 받은 입력을 바로 저장한다.
     *
     * @param value input element / 입력 요소
     */
    private void map(JsValue value) {
        if (mapper.isUndefined()) {
            store(value);
            return;
        }
        try {
            JsValue mapped = Ops.call(mapper, thisArg, new JsValue[] { value, Js.num(index) });
            PromiseInterop.await(realm, mapped, onMapped, onMapperError);
        } catch (JsException e) {
            closeAndReject(e.getValue(realm));
        }
    }

    /**
     * Defines the output property and advances only after that succeeds.
     * <p>
     * 결과 프로퍼티를 정의한 뒤 성공한 경우에만 다음 요소로 진행한다.
     *
     * @param value element to store / 저장할 요소
     */
    private void store(JsValue value) {
        try {
            Support.createAt(output, index, value);
        } catch (JsException e) {
            closeAndReject(e.getValue(realm));
            return;
        }
        index++;
        advance();
    }

    /**
     * Sets the final length before resolving with the complete output.
     * <p>
     * 완성한 결과로 Promise를 해결하기 전에 최종 길이를 설정한다.
     */
    private void finish() {
        Support.setLength(output, index);
        result.resolve(output);
    }

    /**
     * Waits for iterator return when present while preserving the original failure.
     * <p>
     * 이터레이터에 return이 있으면 기다리면서 원래 오류를 보존한다.
     *
     * @param reason the original rejection reason / 원래 거부 이유
     */
    private void closeAndReject(final JsValue reason) {
        if (iterator != null) {
            try {
                JsValue closing = AsyncIteratorHelper.close(iterator);
                if (closing != null) {
                    NativeFunction settle = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                        public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                            result.reject(reason);
                            return Js.UNDEFINED;
                        }
                    });
                    PromiseInterop.await(realm, closing, settle, settle);
                    return;
                }
            } catch (JsException ignored) {
                // throw 완료에서는 return의 예외보다 원래 오류가 우선한다.
            }
        }
        result.reject(reason);
    }
}
