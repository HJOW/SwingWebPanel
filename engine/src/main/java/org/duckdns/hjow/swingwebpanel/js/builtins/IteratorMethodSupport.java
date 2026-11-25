package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.IteratorRecord;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Common validation and callback cleanup for the Iterator prototype methods.
 * <p>
 * Iterator 프로토타입 메서드의 공통 검증 및 콜백 실패 후 정리.
 */
final class IteratorMethodSupport {

    /** Uses static methods only. / 정적 메서드만 사용한다. */
    private IteratorMethodSupport() {
    }

    /**
     * Requires an object receiver before accessing its iterator properties.
     * <p>
     * 이터레이터 프로퍼티에 접근하기 전에 수신자가 객체인지 확인한다.
     *
     * @param receiver the method receiver / 메서드의 수신 객체
     * @return the receiver object / 수신 객체
     */
    static JsObject requireObject(JsValue receiver) {
        if (!(receiver instanceof JsObject)) {
            throw JsException.typeError("Iterator method requires an object receiver");
        }
        return (JsObject) receiver;
    }

    /**
     * Validates the callback and captures the receiver's next method.
     * Closes the receiver before throwing for a non-callable callback.
     * <p>
     * 콜백을 검사한 뒤 수신 객체의 next 메서드를 읽어 둔다.
     * 콜백을 호출할 수 없으면 수신 객체를 닫은 뒤 예외를 던진다.
     *
     * @param receiver the method receiver / 메서드의 수신 객체
     * @param callback the method's callback argument / 메서드의 콜백 인수
     * @return the receiving iterator record / 수신 이터레이터 기록
     */
    static IteratorRecord withCallback(JsValue receiver, JsValue callback) {
        JsObject iterator = requireObject(receiver);
        if (!callback.isCallable()) {
            IteratorHelper.closeQuietly(iterator);
            throw JsException.typeError("Iterator callback is not callable");
        }
        return new IteratorRecord(iterator);
    }

    /**
     * Gets an iterator directly or through Symbol.iterator, with optional support for string primitives.
     * <p>
     * 객체를 직접 이터레이터로 읽거나 Symbol.iterator를 호출한다. 필요에 따라 원시 문자열을 허용한다.
     *
     * @param realm current realm / 현재 realm
     * @param value object to flatten / 펼칠 객체
     * @param allowString whether a string primitive is allowed / 원시 문자열 허용 여부
     * @return an iterator record / 이터레이터 기록
     */
    static IteratorRecord flattenable(JsRealm realm, JsValue value, boolean allowString) {
        if (!(value instanceof JsObject) && !(allowString && value instanceof JsString)) {
            throw JsException.typeError("Expected an iterator or iterable object");
        }
        JsValue method = Ops.getMethod(realm, value, WellKnownSymbols.ITERATOR);
        if (method.isUndefined()) {
            // 문자열에 @@iterator가 없으면 그 문자열 자체를 이터레이터로 사용할 수 없다.
            return new IteratorRecord(requireObject(value));
        }
        return new IteratorRecord(IteratorHelper.getIteratorFromMethod(value, method));
    }

    /**
     * Calls a helper callback and preserves its error while closing the source iterator.
     * <p>
     * 헬퍼 콜백을 호출한다. 오류가 나면 그 오류를 보존하며 원본 이터레이터를 닫는다.
     *
     * @param iterator the source iterator / 원본 이터레이터
     * @param callback the validated callback / 이미 확인한 콜백
     * @param args arguments to the callback / 콜백에 전달할 인수
     * @return the callback result / 콜백 반환값
     */
    static JsValue invoke(IteratorRecord iterator, JsValue callback, JsValue[] args) {
        try {
            return Ops.call(callback, Js.UNDEFINED, args);
        } catch (JsException e) {
            iterator.closeOnError();
            throw e;
        }
    }
}
