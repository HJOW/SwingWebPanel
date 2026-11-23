package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Small helpers for the interface objects of this package: the behaviour of a constructor that is called without <code>new</code>, and the
 * reading of the members of an options (init) dictionary.
 * <p>
 * 이 패키지의 인터페이스 객체를 위한 작은 도우미: <code>new</code> 없이 부른 생성자의 동작, 옵션(초기화) 사전의 멤버 읽기.
 */
final class ScriptUtil {

    /**
     * Private constructor: static helpers.
     * <p>
     * 비공개 생성자: 정적 도우미.
     */
    private ScriptUtil() {
    }

    /**
     * Creates the behaviour of an interface object that is called without <code>new</code>: it throws a TypeError.
     * <p>
     * <code>new</code> 없이 부른 인터페이스 객체의 동작을 만든다: TypeError 를 던진다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the behaviour / 동작
     */
    static NativeFunction.Call refuse(final String name) {
        return new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        };
    }

    /**
     * Reads a member of an options dictionary (undefined if it is missing or the options are not an object).
     * <p>
     * 옵션 사전의 멤버를 읽는다(없거나 옵션이 객체가 아니면 undefined).
     *
     * @param options the dictionary / 사전
     * @param name    the member / 멤버
     * @return the value / 값
     */
    static JsValue member(JsValue options, String name) {
        return options.isObject() ? ((JsObject) options).get(name) : Js.UNDEFINED;
    }

    /**
     * Reads a string member of an options dictionary (the fallback if it is missing).
     * <p>
     * 옵션 사전의 문자열 멤버를 읽는다(없으면 대체값).
     *
     * @param options  the dictionary / 사전
     * @param name     the member / 멤버
     * @param fallback the value when it is missing / 없을 때의 값
     * @return the text / 문자열
     */
    static String text(JsValue options, String name, String fallback) {
        JsValue v = member(options, name);
        return v.isUndefined() ? fallback : Ops.toString(v);
    }

    /**
     * Requires at least a number of arguments, as WebIDL does.
     * <p>
     * WebIDL 처럼 인수가 최소 몇 개 있어야 함을 요구한다.
     *
     * @param args  the arguments / 인수
     * @param count the required number / 필요한 개수
     * @param what  the operation, for example <code>construct 'Worker'</code> / 동작. 예: <code>construct 'Worker'</code>
     */
    static void requireArgs(JsValue[] args, int count, String what) {
        if (args.length < count) {
            throw JsException.typeError("Failed to " + what + ": " + count + " argument" + (count == 1 ? "" : "s") + " required, but only " + args.length
                    + " present.");
        }
    }
}
