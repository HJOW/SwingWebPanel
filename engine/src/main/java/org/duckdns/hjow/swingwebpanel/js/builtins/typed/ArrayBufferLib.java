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
 * Installs the ArrayBuffer constructor and its prototype (ECMA-262 6th edition, 24.1). Registers the intrinsics
 * <code>%ArrayBuffer%</code> and <code>%ArrayBufferPrototype%</code>.
 * <p>
 * ArrayBuffer 생성자와 프로토타입을 설치한다(ECMA-262 6판, 24.1). 내장 객체 <code>%ArrayBuffer%</code>, <code>%ArrayBufferPrototype%</code> 를 등록한다.
 */
final class ArrayBufferLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ArrayBufferLib() {
    }

    /**
     * Installs ArrayBuffer into a realm.
     * <p>
     * realm 에 ArrayBuffer 를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = new JsObject(realm.getObjectPrototype());
        realm.setIntrinsic("%ArrayBufferPrototype%", proto);
        NativeFunction ctor = new NativeFunction(realm, "ArrayBuffer", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Constructor ArrayBuffer requires 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                double length = TypedSupport.toIndex(Js.arg(args, 0), "array buffer length");
                JsValue options = Js.arg(args, 1);
                JsValue max = options instanceof JsObject ? ((JsObject) options).get("maxByteLength") : Js.UNDEFINED;
                double maximum = max.isUndefined() ? -1 : TypedSupport.toIndex(max, "maximum array buffer length");
                if (maximum >= 0 && length > maximum) {
                    throw JsException.rangeError("ArrayBuffer length exceeds its maximum");
                }
                JsObject p = Ops.getPrototypeFromConstructor(r, newTarget, proto);
                if (maximum > ArrayBufferObject.MAX_BYTE_LENGTH) {
                    throw JsException.rangeError("Maximum ArrayBuffer length exceeds the implementation limit");
                }
                return new ArrayBufferObject(p, TypedSupport.allocate(length), (int) maximum);
            }
        });
        realm.setIntrinsic("%ArrayBuffer%", ctor);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("ArrayBuffer", ctor);

        realm.method(ctor, "isView", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue a = Js.arg(args, 0);
                return Js.bool(a instanceof TypedArrayObject || a instanceof DataViewObject);
            }
        });
        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });
        realm.getter(proto, "byteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisBuffer(thisValue).getByteLength());
            }
        });
        realm.getter(proto, "maxByteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(thisBuffer(thisValue).getMaxByteLength());
            }
        });
        realm.getter(proto, "resizable", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisBuffer(thisValue).isResizable());
            }
        });
        realm.getter(proto, "detached", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(thisBuffer(thisValue).isDetached());
            }
        });
        realm.method(proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return slice(r, thisBuffer(thisValue), Js.arg(args, 0), Js.arg(args, 1));
            }
        });
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("ArrayBuffer"), false, false, true);
    }

    /**
     * Returns the this value as an ArrayBuffer or throws a TypeError.
     * <p>
     * this 값을 ArrayBuffer 로 반환하거나 TypeError 를 던진다.
     *
     * @param v the this value / this 값
     * @return the buffer / 버퍼
     */
    private static ArrayBufferObject thisBuffer(JsValue v) {
        if (!(v instanceof ArrayBufferObject)) {
            throw JsException.typeError("this is not an ArrayBuffer");
        }
        return (ArrayBufferObject) v;
    }

    /**
     * ArrayBuffer.prototype.slice (24.1.4.3): copies a byte range into a new buffer made by the species constructor.
     * <p>
     * ArrayBuffer.prototype.slice (24.1.4.3): species 생성자가 만든 새 버퍼로 바이트 구간을 복사한다.
     *
     * @param realm the realm / realm
     * @param buf   the source buffer / 원본 버퍼
     * @param start start argument / 시작 인수
     * @param end   end argument / 끝 인수
     * @return the new buffer / 새 버퍼
     */
    private static JsValue slice(JsRealm realm, ArrayBufferObject buf, JsValue start, JsValue end) {
        if (buf.isDetached()) {
            throw JsException.typeError("this ArrayBuffer is detached");
        }
        int len = buf.getByteLength();
        int first = TypedSupport.relative(start, len, 0);
        int fin = TypedSupport.relative(end, len, len);
        int newLen = Math.max(fin - first, 0);
        JsObject ctor = TypedSupport.speciesConstructor(buf, realm.getIntrinsic("%ArrayBuffer%"));
        JsObject created = Ops.construct(ctor, new JsValue[] { Js.num(newLen) });
        if (!(created instanceof ArrayBufferObject)) {
            throw JsException.typeError("the species constructor did not return an ArrayBuffer");
        }
        ArrayBufferObject nb = (ArrayBufferObject) created;
        if (nb.isDetached()) {
            throw JsException.typeError("the new ArrayBuffer is detached");
        }
        if (nb == buf) {
            throw JsException.typeError("the species constructor returned the same ArrayBuffer");
        }
        if (nb.getByteLength() < newLen) {
            throw JsException.typeError("the new ArrayBuffer is too small");
        }
        if (buf.isDetached()) {
            throw JsException.typeError("this ArrayBuffer is detached");
        }
        int copyLength = Math.min(newLen, buf.getByteLength() - first);
        // 변환 또는 species 호출 중 원본이 줄어들었으면 남은 구간만 복사한다.
        if (copyLength > 0) {
            System.arraycopy(buf.getData(), first, nb.getData(), 0, copyLength);
        }
        return nb;
    }
}
