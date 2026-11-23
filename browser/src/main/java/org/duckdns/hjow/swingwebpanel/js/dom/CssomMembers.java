package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;

/**
 * Installs the members of the CSSOM interfaces that the short member specifications of {@link DomBridge#defineInterface} cannot describe (an
 * attribute that forwards its assignment, an operation with a default that depends on the object, a result that needs the page). Every installed
 * function checks that the receiver is a wrapper of the interface, hands the Java target to the {@link Op}, and turns a {@link DomException} that the
 * operation throws into a script <code>DOMException</code>.
 * <p>
 * {@link DomBridge#defineInterface} 의 짧은 멤버 명세로는 기술할 수 없는 CSSOM 인터페이스의 멤버(대입을 전달하는 속성, 기본값이 객체에 따라 달라지는 연산, 페이지가
 * 필요한 결과)를 설치한다. 설치한 함수는 수신자가 인터페이스의 래퍼인지 확인하고 Java 대상을 {@link Op} 에 넘기며, 연산이 던진 {@link DomException} 을 스크립트
 * <code>DOMException</code> 으로 바꾼다.
 */
public final class CssomMembers {

    /**
     * The behaviour of one member: it gets the Java target and the arguments and returns the script result.
     * <p>
     * 멤버 하나의 동작: Java 대상과 인수를 받아 스크립트 결과를 반환한다.
     */
    public interface Op {
        /**
         * Runs the member on a target.
         * <p>
         * 대상에 멤버를 실행한다.
         *
         * @param bridge the bridge of the page / 페이지의 브리지
         * @param target the Java object behind the receiver / 수신자 뒤의 Java 객체
         * @param args   the arguments / 인수
         * @return the result (undefined for a setter) / 결과(설정 메서드이면 undefined)
         */
        JsValue run(DomBridge bridge, Object target, JsValue[] args);
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CssomMembers() {
    }

    /**
     * Wraps an operation into a native function that checks the receiver.
     * <p>
     * 연산을 수신자를 검사하는 네이티브 함수로 감싼다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface that the receiver must be a wrapper of / 수신자가 래퍼여야 하는 인터페이스
     * @param name   the function name / 함수 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param op     the behaviour / 동작
     * @return the function / 함수
     */
    private static NativeFunction function(final DomBridge bridge, final DomBridge.InterfaceInfo info, String name, int length, final Op op) {
        return bridge.getRealm().newFunction(name, length, new NativeFunction.Call() {
            /**
             * Checks the receiver and runs the operation.
             * <p>
             * 수신자를 검사하고 연산을 실행한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the result / 결과
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Object t = DomBridge.targetOf(self, info.javaClass);
                if (t == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                try {
                    return op.run(bridge, t, args);
                } catch (DomException e) {
                    throw bridge.domError(e);
                }
            }
        });
    }

    /**
     * Installs an operation on the prototype of an interface.
     * <p>
     * 인터페이스의 프로토타입에 연산을 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param name   the method name / 메서드 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param op     the behaviour / 동작
     */
    public static void method(DomBridge bridge, DomBridge.InterfaceInfo info, String name, int length, Op op) {
        info.prototype.defineData(name, function(bridge, info, name, length, op), true, true, true);
    }

    /**
     * Installs an accessor property on the prototype of an interface.
     * <p>
     * 인터페이스의 프로토타입에 접근자 프로퍼티를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param info   the interface / 인터페이스
     * @param name   the property name / 프로퍼티 이름
     * @param getter the read behaviour / 읽기 동작
     * @param setter the write behaviour, or null for a read-only attribute / 쓰기 동작. 읽기 전용이면 null
     */
    public static void accessor(DomBridge bridge, DomBridge.InterfaceInfo info, String name, Op getter, Op setter) {
        JsObject get = function(bridge, info, "get " + name, 0, getter);
        JsObject set = setter == null ? null : function(bridge, info, "set " + name, 1, setter);
        info.prototype.defineAccessor(name, get, set, true, true);
    }
}
