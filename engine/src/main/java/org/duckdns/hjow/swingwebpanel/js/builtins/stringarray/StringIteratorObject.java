package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * A String Iterator object (ECMA-262 6th edition 21.1.5): it walks a string by code points, returning a lone surrogate as it is and a
 * valid surrogate pair as one two-unit string. Its prototype is <code>%StringIteratorPrototype%</code>, which inherits
 * <code>%IteratorPrototype%</code>.
 * <p>
 * String Iterator 객체(ECMA-262 6판 21.1.5): 문자열을 코드 포인트 단위로 걷는다. 외톨이 서로게이트는 그대로, 올바른 서로게이트 쌍은 두 단위
 * 문자열 하나로 돌려준다. 프로토타입은 <code>%IteratorPrototype%</code> 을 상속하는 <code>%StringIteratorPrototype%</code> 이다.
 */
public final class StringIteratorObject extends JsObject {

    /**
     * Name of the intrinsic that holds the prototype.
     * <p>
     * 프로토타입을 담는 내장 객체 이름.
     */
    public static final String PROTOTYPE_NAME = "%StringIteratorPrototype%";

    /**
     * The string being walked, or null once the iterator is exhausted.
     * <p>
     * 걷고 있는 문자열. 이터레이터가 끝나면 null.
     */
    private String string;

    /**
     * Next position (UTF-16 index).
     * <p>
     * 다음 위치(UTF-16 인덱스).
     */
    private int position;

    /**
     * Creates an iterator object.
     * <p>
     * 이터레이터 객체를 만든다.
     *
     * @param proto the prototype / 프로토타입
     * @param s     the string / 문자열
     */
    private StringIteratorObject(JsObject proto, String s) {
        super(proto);
        this.string = s;
    }

    /**
     * Returns the class name for Object.prototype.toString.
     * <p>
     * Object.prototype.toString 용 클래스 이름을 돌려준다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "String Iterator";
    }

    /**
     * Creates a String Iterator for a string (CreateStringIterator).
     * <p>
     * 문자열의 String Iterator 를 만든다(CreateStringIterator).
     *
     * @param realm the realm / realm
     * @param s     the string / 문자열
     * @return the iterator object / 이터레이터 객체
     */
    public static JsObject create(JsRealm realm, String s) {
        return new StringIteratorObject(realm.getIntrinsic(PROTOTYPE_NAME), s);
    }

    /**
     * Installs <code>%StringIteratorPrototype%</code> with its <code>next</code> method and <code>@@toStringTag</code>.
     * <p>
     * <code>next</code> 메서드와 <code>@@toStringTag</code> 를 가진 <code>%StringIteratorPrototype%</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = new JsObject(realm.getIteratorPrototype());
        Support.def(realm, proto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof StringIteratorObject)) {
                    throw JsException.typeError("next method called on incompatible receiver (not a String Iterator)");
                }
                return ((StringIteratorObject) thisValue).step(r);
            }
        });
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG, PropertyDescriptor.data(Js.str("String Iterator"), false, false, true));
        realm.setIntrinsic(PROTOTYPE_NAME, proto);
    }

    /**
     * Produces the next iterator result.
     * <p>
     * 다음 이터레이터 결과를 만든다.
     *
     * @param realm the realm / realm
     * @return the result object / 결과 객체
     */
    private JsValue step(JsRealm realm) {
        if (string == null) {
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        if (position >= string.length()) {
            string = null;
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        char first = string.charAt(position);
        int size = 1;
        // 올바른 서로게이트 쌍이면 두 코드 단위를 한 번에 돌려준다.
        if (Character.isHighSurrogate(first) && position + 1 < string.length() && Character.isLowSurrogate(string.charAt(position + 1))) {
            size = 2;
        }
        String piece = string.substring(position, position + size);
        position += size;
        return IteratorHelper.createResult(realm, Js.str(piece), false);
    }
}
