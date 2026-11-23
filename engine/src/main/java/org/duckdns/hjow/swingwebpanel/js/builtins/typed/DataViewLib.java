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
 * Installs the DataView constructor and its prototype (ECMA-262 6th edition, 24.2): the buffer, byteLength and byteOffset getters and the
 * get/set methods for Int8, Uint8, Int16, Uint16, Int32, Uint32, Float32 and Float64 with an explicit endianness argument. Registers the
 * intrinsics <code>%DataView%</code> and <code>%DataViewPrototype%</code>.
 * <p>
 * DataView 생성자와 프로토타입을 설치한다(ECMA-262 6판, 24.2): buffer, byteLength, byteOffset getter 와 엔디언 인수를 받는 Int8, Uint8, Int16,
 * Uint16, Int32, Uint32, Float32, Float64 의 get/set 메서드. 내장 객체 <code>%DataView%</code>, <code>%DataViewPrototype%</code> 를 등록한다.
 */
final class DataViewLib {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DataViewLib() {
    }

    /**
     * Installs DataView into a realm.
     * <p>
     * realm 에 DataView 를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = new JsObject(realm.getObjectPrototype());
        realm.setIntrinsic("%DataViewPrototype%", proto);
        NativeFunction ctor = new NativeFunction(realm, "DataView", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Constructor DataView requires 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return create(r, args, newTarget, proto);
            }
        });
        realm.setIntrinsic("%DataView%", ctor);
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        realm.getGlobalObject().defineBuiltin("DataView", ctor);

        realm.getter(proto, "buffer", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisView(thisValue).getBuffer();
            }
        });
        realm.getter(proto, "byteLength", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                DataViewObject v = thisView(thisValue);
                return Js.num(v.getBuffer().isDetached() ? 0 : v.getByteLength());
            }
        });
        realm.getter(proto, "byteOffset", new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                DataViewObject v = thisView(thisValue);
                return Js.num(v.getBuffer().isDetached() ? 0 : v.getByteOffset());
            }
        });
        ElementType[] types = { ElementType.INT8, ElementType.UINT8, ElementType.INT16, ElementType.UINT16, ElementType.INT32, ElementType.UINT32,
            ElementType.FLOAT32, ElementType.FLOAT64 };
        // 형식마다 getXxx / setXxx 메서드를 정의한다.
        for (int i = 0; i < types.length; i++) {
            final ElementType type = types[i];
            String suffix = type.getConstructorName().substring(0, type.getConstructorName().length() - "Array".length());
            realm.method(proto, "get" + suffix, 1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    DataViewObject view = thisView(thisValue);
                    double index = TypedSupport.toIndex(Js.arg(args, 0), "DataView offset");
                    boolean little = Ops.toBoolean(Js.arg(args, 1));
                    int at = checkRange(view, index, type);
                    return Js.num(type.read(view.getBuffer().getData(), at, little));
                }
            });
            realm.method(proto, "set" + suffix, 2, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    DataViewObject view = thisView(thisValue);
                    double index = TypedSupport.toIndex(Js.arg(args, 0), "DataView offset");
                    double number = Ops.toNumber(Js.arg(args, 1));
                    boolean little = Ops.toBoolean(Js.arg(args, 2));
                    int at = checkRange(view, index, type);
                    type.write(view.getBuffer().getData(), at, number, little);
                    return Js.UNDEFINED;
                }
            });
        }
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("DataView"), false, false, true);
    }

    /**
     * Returns the this value as a DataView or throws a TypeError.
     * <p>
     * this 값을 DataView 로 반환하거나 TypeError 를 던진다.
     *
     * @param v the this value / this 값
     * @return the view / 뷰
     */
    private static DataViewObject thisView(JsValue v) {
        if (!(v instanceof DataViewObject)) {
            throw JsException.typeError("this is not a DataView");
        }
        return (DataViewObject) v;
    }

    /**
     * Checks that an element fits into the view and returns its absolute position in the buffer.
     * <p>
     * 요소가 뷰 안에 들어가는지 검사하고 버퍼 안의 절대 위치를 반환한다.
     *
     * @param view  the view / 뷰
     * @param index the requested offset inside the view / 뷰 안에서 요청한 위치
     * @param type  the element type / 요소 형식
     * @return the position in the buffer / 버퍼 안의 위치
     */
    private static int checkRange(DataViewObject view, double index, ElementType type) {
        if (view.getBuffer().isDetached()) {
            throw JsException.typeError("the buffer of this DataView is detached");
        }
        if (index + type.getSize() > view.getByteLength()) {
            throw JsException.rangeError("Offset is outside the bounds of the DataView");
        }
        return view.getByteOffset() + (int) index;
    }

    /**
     * The DataView constructor behavior (24.2.2.1).
     * <p>
     * DataView 생성자 동작(24.2.2.1).
     *
     * @param realm     the realm / realm
     * @param args      the arguments / 인수
     * @param newTarget new.target / new.target
     * @param proto     the default prototype / 기본 프로토타입
     * @return the new view / 새 뷰
     */
    private static JsObject create(JsRealm realm, JsValue[] args, JsObject newTarget, JsObject proto) {
        JsValue b = Js.arg(args, 0);
        if (!(b instanceof ArrayBufferObject)) {
            throw JsException.typeError("First argument to DataView constructor must be an ArrayBuffer");
        }
        ArrayBufferObject buffer = (ArrayBufferObject) b;
        double offset = TypedSupport.toIndex(Js.arg(args, 1), "DataView offset");
        if (buffer.isDetached()) {
            throw JsException.typeError("the buffer is detached");
        }
        int bufferLength = buffer.getByteLength();
        if (offset > bufferLength) {
            throw JsException.rangeError("Start offset " + (long) offset + " is outside the bounds of the buffer");
        }
        double viewLength;
        JsValue lengthArg = Js.arg(args, 2);
        if (lengthArg.isUndefined()) {
            viewLength = bufferLength - offset;
        } else {
            viewLength = TypedSupport.toIndex(lengthArg, "DataView length");
            if (offset + viewLength > bufferLength) {
                throw JsException.rangeError("Invalid DataView length " + (long) viewLength);
            }
        }
        JsObject p = Ops.getPrototypeFromConstructor(realm, newTarget, proto);
        if (buffer.isDetached()) {
            throw JsException.typeError("the buffer is detached");
        }
        return new DataViewObject(p, buffer, (int) offset, (int) viewLength);
    }
}
