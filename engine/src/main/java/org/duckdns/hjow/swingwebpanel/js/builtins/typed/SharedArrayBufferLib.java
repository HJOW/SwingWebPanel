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
 * Installs the SharedArrayBuffer constructor and prototype from ECMA-262 §25.2.
 * <p>
 * ECMA-262 §25.2의 SharedArrayBuffer 생성자와 프로토타입을 설치한다.
 */
final class SharedArrayBufferLib {

    /** Uses static methods only. / 정적 메서드만 사용한다. */
    private SharedArrayBufferLib() {
    }

    /**
     * Installs SharedArrayBuffer into one realm.
     * <p>
     * 한 realm에 SharedArrayBuffer를 설치한다.
     *
     * @param realm the realm receiving the built-in / 내장 객체를 설치할 realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = new JsObject(realm.getObjectPrototype());
        realm.setIntrinsic("%SharedArrayBufferPrototype%", proto);
        NativeFunction ctor = new NativeFunction(realm, "SharedArrayBuffer", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                throw JsException.typeError("Constructor SharedArrayBuffer requires 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                double length = TypedSupport.toIndex(Js.arg(args, 0), "shared array buffer length");
                double maximum = TypedSupport.maxByteLengthOption(Js.arg(args, 1));
                // 명시한 최대 길이가 초기 길이보다 작으면 프로토타입 조회 전에 거부한다.
                if (maximum >= 0 && length > maximum) {
                    throw JsException.rangeError("SharedArrayBuffer length exceeds its maximum");
                }
                JsObject prototype = Ops.getPrototypeFromConstructor(r, newTarget, proto);
                if (length > ArrayBufferObject.MAX_BYTE_LENGTH || maximum > ArrayBufferObject.MAX_BYTE_LENGTH) {
                    throw JsException.rangeError("SharedArrayBuffer length exceeds the implementation limit");
                }
                return new SharedArrayBufferObject(prototype, new SharedByteBlock((int) length), (int) maximum);
            }
        });
        realm.setIntrinsic("%SharedArrayBuffer%", ctor);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("SharedArrayBuffer", ctor);

        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return receiver;
            }
        });
        realm.getter(proto, "byteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return Js.num(requireShared(receiver).getByteLength());
            }
        });
        realm.getter(proto, "maxByteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return Js.num(requireShared(receiver).getMaxByteLength());
            }
        });
        realm.getter(proto, "growable", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return Js.bool(requireShared(receiver).isResizable());
            }
        });
        realm.method(proto, "grow", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                SharedArrayBufferObject buffer = requireShared(receiver);
                // 고정 버퍼라면 전달한 길이 객체를 숫자로 변환하기 전에 오류를 낸다.
                if (!buffer.isResizable()) {
                    throw JsException.typeError("SharedArrayBuffer is not growable");
                }
                double length = TypedSupport.toIndex(Js.arg(args, 0), "new shared array buffer length");
                if (length > buffer.getMaxByteLength()) {
                    throw JsException.rangeError("New SharedArrayBuffer length exceeds its maximum");
                }
                buffer.sharedBlock().grow((int) length);
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return slice(r, requireShared(receiver), Js.arg(args, 0), Js.arg(args, 1));
            }
        });
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("SharedArrayBuffer"), false, false, true);
    }

    /**
     * Validates the shared-buffer brand.
     * <p>
     * 공유 버퍼인지 검사한다.
     *
     * @param value value to check / 검사할 값
     * @return the shared buffer / 공유 버퍼
     */
    private static SharedArrayBufferObject requireShared(JsValue value) {
        if (!(value instanceof SharedArrayBufferObject)) {
            throw JsException.typeError("Expected a SharedArrayBuffer");
        }
        return (SharedArrayBufferObject) value;
    }

    /**
     * Copies a range into storage allocated by the species constructor.
     * <p>
     * 지정 범위를 species 생성자가 만든 저장소로 복사한다.
     *
     * @param realm current realm / 현재 realm
     * @param source shared source buffer / 원본 공유 버퍼
     * @param start start argument / 시작 인수
     * @param end end argument / 끝 인수
     * @return the result of the species constructor / species 생성자의 결과
     */
    private static SharedArrayBufferObject slice(JsRealm realm, SharedArrayBufferObject source, JsValue start, JsValue end) {
        int length = source.getByteLength();
        int first = TypedSupport.relative(start, length, 0);
        int last = TypedSupport.relative(end, length, length);
        int count = Math.max(last - first, 0);
        JsObject ctor = TypedSupport.speciesConstructor(source, realm.getIntrinsic("%SharedArrayBuffer%"));
        SharedArrayBufferObject target = requireShared(Ops.construct(ctor, new JsValue[] { Js.num(count) }));
        // 다른 래퍼가 같은 저장소를 가리키는 경우에도 복사 대상으로 쓸 수 없다.
        if (source.sharesStorageWith(target) || target.getByteLength() < count) {
            throw JsException.typeError("Invalid SharedArrayBuffer slice destination");
        }
        source.copyBytesTo(first, target, 0, count);
        return target;
    }
}
